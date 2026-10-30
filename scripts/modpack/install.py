#!/usr/bin/env python3
"""Official-source personal NeoForge resolver/installer. Python 3.11+, no third-party packages."""
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import ssl
import sys
import subprocess
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import zipfile

# Also works when imported by tests from an arbitrary current directory.
sys.path.insert(0, str(Path(__file__).resolve().parent))
import audit_jars
import official_ftb

ROOT = Path(__file__).resolve().parents[2]
MC = '1.21.1'
NF = '21.1.264'
UA = 'Reimagined-personal-coop/0.2 (github.com/yrtxrr/minecraft-reimagined)'
API_HOSTS = {'api.modrinth.com', 'api.curseforge.com'}
FILE_HOSTS = {'cdn.modrinth.com', 'edge.forgecdn.net', 'mediafilez.forgecdn.net', 'media.forgecdn.net', 'maven.ftb.dev'}
MAX_FILE = 512 * 1024 * 1024


class Blocked(RuntimeError):
    pass


def safe_url(url, hosts):
    p = urllib.parse.urlsplit(url)
    if p.scheme != 'https' or p.hostname not in hosts or p.username or p.password or p.port not in (None, 443):
        raise Blocked('Non-official or unsafe HTTPS URL refused')
    return url


def safe_filename(name):
    if not isinstance(name,str) or not re.fullmatch(r"[A-Za-z0-9_.+() '-]+[.]jar",name):
        raise Blocked('Unsafe or non-JAR artifact filename')
    if name.split('.')[0].upper() in {'CON','PRN','AUX','NUL',*(f'COM{i}' for i in range(1,10)),*(f'LPT{i}' for i in range(1,10))}:
        raise Blocked('Windows reserved artifact filename')
    return name


class Redirects(urllib.request.HTTPRedirectHandler):
    def __init__(self, hosts):
        self.hosts = hosts

    def redirect_request(self, request, fp, code, msg, headers, url):
        safe_url(url, self.hosts)
        if urllib.parse.urlsplit(request.full_url).hostname == 'api.curseforge.com' and urllib.parse.urlsplit(url).hostname != 'api.curseforge.com':
            raise Blocked('CurseForge API-key cross-host redirect refused')
        return super().redirect_request(request, fp, code, msg, headers, url)


class Transport:
    def __init__(self, api_key=None, local_files=None):
        self.api_key = api_key
        self.local_files = Path(local_files) if local_files else None

    def open(self, url, hosts):
        safe_url(url, hosts)
        headers = {'User-Agent': UA}
        if urllib.parse.urlsplit(url).hostname == 'api.curseforge.com':
            if not self.api_key:
                raise Blocked('CurseForge official API needs a local CURSEFORGE_API_KEY; use official app/manual export otherwise')
            headers['x-api-key'] = self.api_key
        # Default proxy and certificate validation are deliberately retained.
        opener = urllib.request.build_opener(Redirects(hosts), urllib.request.HTTPSHandler(context=ssl.create_default_context()))
        try:
            return opener.open(urllib.request.Request(url, headers=headers), timeout=30)
        except urllib.error.HTTPError as exc:
            raise Blocked(f'{urllib.parse.urlsplit(url).hostname}: HTTP {exc.code}') from None
        except urllib.error.URLError as exc:
            raise Blocked(f'{urllib.parse.urlsplit(url).hostname}: {exc.reason}') from None

    def json(self, url):
        with self.open(url, API_HOSTS) as response:
            raw = response.read(8 * 1024 * 1024 + 1)
        if len(raw) > 8 * 1024 * 1024:
            raise Blocked('Oversized catalog response')
        return json.loads(raw)

    def download(self, file, directory):
        safe_url(file['url'], FILE_HOSTS)
        name = safe_filename(file['filename'])
        hashes = file['hashes']
        algorithm = next((x for x in ('sha512', 'sha256', 'sha1') if x in hashes), None)
        if not algorithm or not re.fullmatch(r'[0-9a-fA-F]+', hashes[algorithm]) or len(hashes[algorithm]) != hashlib.new(algorithm).digest_size * 2:
            raise Blocked('Missing/invalid official checksum (MD5-only files refused)')
        size = file.get('size')
        if not isinstance(size, int) or not 0 < size <= MAX_FILE:
            raise Blocked('Missing or oversized official file size')
        path = directory / (hashlib.sha256(hashes[algorithm].lower().encode('ascii')).hexdigest() + '.jar')
        def verify(candidate):
            return candidate.stat().st_size == size and hashlib.new(algorithm, candidate.read_bytes()).hexdigest() == hashes[algorithm].lower()
        if path.exists():
            if path.is_symlink() or not path.is_file() or not verify(path):
                raise Blocked('Cached artifact checksum mismatch')
        else:
            temporary = path.with_suffix('.part')
            if temporary.exists() or temporary.is_symlink():
                raise Blocked('Stale partial download; inspect and remove it manually')
            try:
                local_dir = getattr(self, 'local_files', None)
                local = local_dir / name if local_dir else None
                if local and local.exists():
                    if local.is_symlink() or not local.is_file() or not verify(local):
                        raise Blocked('Manual official artifact checksum/size mismatch: ' + name)
                    source = local.open('rb')
                else:
                    source = self.open(file['url'], FILE_HOSTS)
                with source as response, temporary.open('xb') as output:
                    count = 0
                    while chunk := response.read(1024 * 1024):
                        count += len(chunk)
                        if count > size or count > MAX_FILE:
                            raise Blocked('Download exceeded declared size')
                        output.write(chunk)
                if not verify(temporary):
                    raise Blocked('Official artifact checksum/size mismatch')
                temporary.rename(path)
            finally:
                if temporary.exists():
                    temporary.unlink()
        if not zipfile.is_zipfile(path):
            raise Blocked('Downloaded file is not a JAR/ZIP')
        return path


def normal(title):
    return re.sub(r'[^a-z0-9]', '', title.lower())


def sides(project, override=None):
    if override is not None:
        if override not in ('client', 'server', 'both'):
            raise Blocked('Invalid manually reviewed side override')
        return ['client', 'server'] if override == 'both' else [override]
    values = [project.get('client_side'), project.get('server_side')]
    if any(x not in ('required', 'optional', 'unsupported') for x in values):
        raise Blocked('Unknown side metadata: review exact artifact and supply side_override')
    result = [side for side, value in zip(('client', 'server'), values) if value != 'unsupported']
    if not result:
        raise Blocked('Project supports neither client nor server')
    return result


def compatible(version):
    return MC in version.get('game_versions', []) and 'neoforge' in version.get('loaders', [])


def choose(versions, allow_beta=False):
    choices = [v for v in versions if compatible(v) and v.get('version_type') in (('release','beta') if allow_beta else ('release',))]
    if not choices:
        raise Blocked('No verified NeoForge 1.21.1 release; wrong-loader/Fabric/Forge/datapack files are not substituted')
    return max(choices, key=lambda v: v['date_published'])


class Resolver:
    def __init__(self, transport, bindings, allow_beta=False):
        self.t = transport
        self.bindings = bindings
        self.allow_beta = allow_beta
        self.selected = {}
        self.constraints = []
        self.projects = {}

    def mr_project(self, ident):
        if ident not in self.projects:
            p = self.t.json('https://api.modrinth.com/v2/project/' + urllib.parse.quote(ident, safe=''))
            self.projects[ident] = p
            self.projects[p['id']] = p
        return self.projects[ident]

    def identify_mr(self, target, binding):
        ident = binding.get('modrinth_project_id') or binding.get('modrinth_slug_hint')
        allowed = {normal(x) for x in binding['accepted_titles']}
        if ident:
            p = self.mr_project(ident)
            if normal(p['title']) not in allowed:
                raise Blocked(f'Project identity mismatch: official title {p["title"]!r}; review bindings, do not substitute names')
            return p
        query = urllib.parse.urlencode({'query': target['name'], 'limit': 20, 'facets': json.dumps([['project_type:mod']])})
        hits = self.t.json('https://api.modrinth.com/v2/search?' + query)['hits']
        exact = {p['project_id'] for p in hits if normal(p['title']) in allowed}
        if len(exact) != 1:
            raise Blocked('No unique exact official project identity; review source-bindings.json (candidate retained)')
        return self.mr_project(exact.pop())

    def mr(self, ident, pinned=None, override=None, depth=0):
        if depth > 32 or len(self.selected) >= 160:
            raise Blocked('Dependency budget exceeded')
        project = self.mr_project(ident)
        key = 'modrinth:' + project['id']
        if key in self.selected:
            if pinned and self.selected[key]['version_id'] != pinned:
                raise Blocked('Conflicting exact dependency version: ' + key)
            return key
        if pinned:
            version = self.t.json('https://api.modrinth.com/v2/version/' + urllib.parse.quote(pinned, safe=''))
            if version['project_id'] != project['id'] or not compatible(version):
                raise Blocked('Pinned dependency has wrong project/loader/Minecraft')
        else:
            query = urllib.parse.urlencode({'loaders': '["neoforge"]', 'game_versions': '["1.21.1"]'})
            version = choose(self.t.json('https://api.modrinth.com/v2/project/' + project['id'] + '/version?' + query), self.allow_beta)
        jar_files = [f for f in version['files'] if f['filename'].endswith('.jar')]
        primary = [f for f in jar_files if f.get('primary')]
        if len(primary) == 1:
            file = primary[0]
        elif len(jar_files) == 1:
            file = jar_files[0]
        else:
            raise Blocked('No unique primary JAR; datapack/manual handling required')
        safe_url(file['url'], FILE_HOSTS)
        record = dict(key=key, source='modrinth', project_id=project['id'], name=project['title'],
                      version_id=version['id'], version=version['version_number'], minecraft=MC, loader='neoforge',
                      sides=sides(project, override or self.bindings.get('dependency_sides', {}).get(key)),
                      license=project.get('license'), source_url='https://modrinth.com/mod/' + project['slug'],
                      file={k: file[k] for k in ('url', 'filename', 'hashes', 'size')}, dependencies=[], optional_dependencies=[])
        self.selected[key] = record
        for dep in version.get('dependencies', []):
            if dep['dependency_type'] == 'required':
                dep_id = dep.get('project_id')
                if not dep_id and dep.get('version_id'):
                    dep_id = self.t.json('https://api.modrinth.com/v2/version/' + dep['version_id'])['project_id']
                if not dep_id:
                    raise Blocked('Required dependency lacks project/version identity')
                record['dependencies'].append(self.mr(dep_id, dep.get('version_id'), depth=depth+1))
            elif dep['dependency_type'] == 'incompatible':
                self.constraints.append(dict(source=key, project=dep.get('project_id'), version=dep.get('version_id')))
            elif dep['dependency_type'] == 'optional':
                record['optional_dependencies'].append(dep)
            elif dep['dependency_type'] not in ('embedded',):
                raise Blocked('Unknown dependency relation')
        return key

    def cf(self, ident, pinned=None, override=None, depth=0):
        if depth > 32 or len(self.selected) >= 160:
            raise Blocked('Dependency budget exceeded')
        ident = str(int(ident)); key = 'curseforge:' + ident
        if key in self.selected:
            if pinned and str(self.selected[key]['version_id']) != str(pinned):
                raise Blocked('Conflicting exact CurseForge dependency file')
            return key
        project = self.t.json('https://api.curseforge.com/v1/mods/' + ident)['data']
        if project.get('allowModDistribution') is False:
            raise Blocked('Author disallows API distribution: use official CurseForge app, not a synthesized URL')
        if override is None:
            override = self.bindings.get('dependency_sides', {}).get(key)
        side_list = sides({}, override)  # CF API has no reliable client/server requirement field.
        if pinned:
            file = self.t.json(f'https://api.curseforge.com/v1/mods/{ident}/files/{int(pinned)}')['data']
        else:
            files = self.t.json(f'https://api.curseforge.com/v1/mods/{ident}/files?gameVersion={MC}&modLoaderType=6&pageSize=50')['data']
            matches = [f for f in files if MC in f.get('gameVersions', []) and 'NeoForge' in f.get('gameVersions', []) and (self.allow_beta or f['releaseType'] == 1)]
            if not matches:
                raise Blocked('No matching official CurseForge NeoForge release')
            file = max(matches, key=lambda f: f['fileDate'])
        if str(file['modId']) != ident or MC not in file.get('gameVersions', []) or 'NeoForge' not in file.get('gameVersions', []):
            raise Blocked('CurseForge file project/loader mismatch')
        if not file.get('downloadUrl'):
            raise Blocked('Author disallows API distribution or no official downloadUrl: use official CurseForge app; do not synthesize CDN URLs')
        safe_url(file['downloadUrl'], FILE_HOSTS)
        hashes = {('sha1' if h['algo'] == 1 else 'md5'): h['value'] for h in file.get('hashes', []) if h['algo'] in (1, 2)}
        if 'sha1' not in hashes:
            raise Blocked('CurseForge file has no official SHA1')
        record = dict(key=key,source='curseforge',project_id=ident,name=project['name'],version_id=str(file['id']),
                      version=file['displayName'],minecraft=MC,loader='neoforge',sides=side_list,
                      license=None,license_review='Review official project license; personal download is not redistribution permission',
                      source_url=project['links']['websiteUrl'],file=dict(url=file['downloadUrl'],filename=file['fileName'],hashes=hashes,size=file['fileLength']),
                      dependencies=[],optional_dependencies=[])
        self.selected[key] = record
        for dep in file.get('dependencies', []):
            if dep['relationType'] == 3:
                record['dependencies'].append(self.cf(dep['modId'], depth=depth+1))
            elif dep['relationType'] == 5:
                self.constraints.append(dict(source=key, cf_project=str(dep['modId'])))
            elif dep['relationType'] == 2:
                record['optional_dependencies'].append(dep)
        return key

    def target(self, target, binding):
        snapshot = copy.deepcopy(self.selected), copy.deepcopy(self.constraints)
        errors = []
        try:
            p = self.identify_mr(target, binding)
            return self.mr(p['id'], binding.get('modrinth_version_id'), binding.get('side_override'))
        except (Blocked, KeyError, ValueError) as exc:
            self.selected, self.constraints = copy.deepcopy(snapshot)
            errors.append('Modrinth: ' + str(exc))
        if binding.get('curseforge_project_id') or getattr(self.t, 'api_key', None):
            try:
                if binding.get('curseforge_project_id'):
                    project = self.t.json('https://api.curseforge.com/v1/mods/' + str(int(binding['curseforge_project_id'])))['data']
                else:
                    query=urllib.parse.urlencode({'gameId':432,'classId':6,'searchFilter':target['name'],'gameVersion':MC,'modLoaderType':6,'pageSize':50})
                    matches=self.t.json('https://api.curseforge.com/v1/mods/search?'+query)['data']
                    matches=[p for p in matches if normal(p['name']) in {normal(x) for x in binding['accepted_titles']}]
                    if len(matches)!=1:
                        raise Blocked('No unique exact CurseForge project; bind reviewed numeric ID')
                    project=matches[0]
                if normal(project['name']) not in {normal(x) for x in binding['accepted_titles']}:
                    raise Blocked('CurseForge project title mismatch')
                return self.cf(project['id'], binding.get('curseforge_file_id'), binding.get('side_override'))
            except (Blocked, KeyError, ValueError) as exc:
                self.selected, self.constraints = snapshot
                errors.append('CurseForge: ' + str(exc))
        else:
            errors.append('CurseForge: project ID not reviewed/bound; official app/manual project selection remains available')
        raise Blocked('; '.join(errors))

    def conflicts(self):
        result = []
        for c in self.constraints:
            for record in self.selected.values():
                if ((record['source'] == 'modrinth' and (c.get('project') == record['project_id'] or c.get('version') == record['version_id'])
                     and (not c.get('version') or c['version'] == record['version_id']))
                    or (record['source'] == 'curseforge' and c.get('cf_project') == record['project_id'])):
                    result.append(c['source'] + ' incompatible with ' + record['key'])
        for key, record in self.selected.items():
            for dep in record['dependencies']:
                if not set(record['sides']).issubset(self.selected[dep]['sides']):
                    result.append(key + ' has unresolved side-specific dependency ' + dep)
        return result



def pinned_records(path, include_optional=False):
    manifest=json.loads(path.read_text(encoding='utf-8'))
    if (manifest['minecraft'],manifest['neoforge'])!=(MC,NF):raise Blocked('Pinned manifest platform mismatch')
    records={};targets={};all_targets={}
    for entry in manifest['mods']:
        if entry.get('candidateId'):all_targets[entry['candidateId']]=entry
        if not entry['required'] and not include_optional:continue
        key='modrinth:'+entry['projectId']
        if key in records:raise Blocked('Duplicate pinned project')
        safe_url(entry['url'],FILE_HOSTS)
        safe_filename(entry['filename'])
        if not re.fullmatch('[0-9a-f]{128}',entry['sha512']):raise Blocked('Invalid pinned SHA512')
        records[key]=dict(key=key,source='modrinth',project_id=entry['projectId'],name=entry['name'],version_id=entry['versionId'],version=entry['version'],minecraft=MC,loader='neoforge',sides=entry['sides'],license=entry['license'],source_url='https://modrinth.com/mod/'+entry['slug'],file=dict(url=entry['url'],filename=entry['filename'],hashes={'sha512':entry['sha512']},size=entry['size']),dependencies=['modrinth:'+x for x in entry['dependencies']],optional_dependencies=[],pin_source='pack/mods.json')
        if entry.get('candidateId'):targets[entry['candidateId']]=key
    for record in records.values():
        if any(dep not in records for dep in record['dependencies']):raise Blocked('Pinned dependency closure incomplete')
    return records,targets,all_targets


def resolve(registry, bindings, transport, end='nullscape', allow_beta=False, pinned=None, pinned_only=False, include_optional=False, ftb_maven_cache=None):
    expected = {m['id'] for m in registry['mods']}
    if len(expected) != 40 or expected != set(bindings['targets']):
        raise Blocked('Registry/bindings must cover all 40 unique candidates')
    resolver = Resolver(transport, bindings, allow_beta)
    pinned_targets={};all_pinned={}
    if pinned is not None:
        resolver.selected,pinned_targets,all_pinned=pinned_records(pinned,include_optional)
    for candidate,key in list(pinned_targets.items()):
        if (candidate=='nullscape' and end!='nullscape') or (candidate=='betterend-new-dawn' and end=='nullscape'):
            resolver.selected.pop(key,None);pinned_targets.pop(candidate)
    report = []
    for target in registry['mods']:
        selected = not (target['id'] == 'nullscape' and end != 'nullscape' or target['id'] == 'betterend-new-dawn' and end == 'nullscape')
        before = copy.deepcopy(resolver.selected), copy.deepcopy(resolver.constraints)
        if not selected and target['id'] in all_pinned:
            entry=all_pinned[target['id']]
            report.append(dict(id=target['id'],name=target['name'],selected=False,status='available-alternative-not-selected',version=entry['version'],version_id=entry['versionId']))
            continue
        if target['id'] in all_pinned and not all_pinned[target['id']]['required'] and target['id'] not in pinned_targets:
            report.append(dict(id=target['id'],name=target['name'],selected=False,status='optional-not-selected',reason='Pinned optional mod; enable include_optional explicitly'))
            continue
        try:
            if target['id'] in pinned_targets:
                key=pinned_targets[target['id']]
            elif target['id'] == 'ftb-quests' and ftb_maven_cache is not None:
                key = official_ftb.resolve(resolver, pinned, ftb_maven_cache)
            elif pinned_only:
                raise Blocked('Not in verified pack/mods.json; retained candidate, no invented pin')
            else:
                key = resolver.target(target, bindings['targets'][target['id']])
            record = resolver.selected[key]
            report.append(dict(id=target['id'], name=target['name'], selected=selected,
                               status='resolved' if selected else 'available-alternative-not-selected', key=key,
                               version=record['version'], version_id=record['version_id'], source_url=record['source_url']))
        except (Blocked, ValueError, KeyError, OSError) as exc:
            resolver.selected, resolver.constraints = before
            report.append(dict(id=target['id'],name=target['name'],selected=selected,status='unresolved',reason=str(exc)))
        if not selected:
            resolver.selected, resolver.constraints = before
    # Independently curated providers can publish the same mod on both catalogs.
    # Cross-provider duplicate Java mod IDs are checked after downloading JARs, not guessed from names.
    return dict(schema=1,minecraft=MC,neoforge=NF,state='candidate-not-runtime-tested',end_choice=end,
                targets=report,mods=list(resolver.selected.values()),constraints=resolver.constraints,
                conflicts=resolver.conflicts(),runtime_verified=False)


def write_reports(plan, directory):
    directory.mkdir(parents=True, exist_ok=True)
    (directory / 'install-plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    lines = ['# Reimagined official-source resolution', '',
             'Candidate only: metadata and hashes do not establish runtime compatibility.', '',
             '| Candidate | Selected | Result | Version / reason |','|---|---|---|---|']
    for t in plan['targets']:
        detail = str(t.get('version') or t.get('reason', '')).replace('|','/').replace('\n',' ')
        lines.append(f'|{t["name"]}|{t["selected"]}|{t["status"]}|{detail}|')
    lines += ['', '## Conflicts', *('- '+c for c in plan['conflicts']), '',
              'No Fabric/Forge fallback, mirror, synthesized CDN URL, automatic EULA acceptance or authentication bypass.']
    (directory / 'INSTALL_REPORT.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')


def validate_plan(plan, allow_incomplete=False):
    if plan.get('schema') != 1 or (plan.get('minecraft'),plan.get('neoforge')) != (MC,NF):
        raise Blocked('Wrong/unknown plan platform')
    expected = {m['id'] for m in json.loads((ROOT/'modpack/mod-registry.json').read_text(encoding='utf-8'))['mods']}
    ids = [t['id'] for t in plan['targets']]
    if len(ids) != 40 or set(ids) != expected:
        raise Blocked('Plan omits or duplicates a target candidate')
    missing = [t['id'] for t in plan['targets'] if t['selected'] and t['status'] != 'resolved']
    if missing and not allow_incomplete:
        raise Blocked('Unresolved targets: '+', '.join(missing)+'. Review report; --allow-incomplete is explicit, never silently full.')
    if not plan['mods']:
        raise Blocked('No external mods resolved: refusing to create another Core-only archive')
    if plan.get('conflicts'):
        raise Blocked('Known incompatible/side constraints: '+'; '.join(plan['conflicts']))
    if len(plan['mods']) > 160:
        raise Blocked('Locked dependency budget exceeded')
    records = {m['key']:m for m in plan['mods']}
    if len(records) != len(plan['mods']):
        raise Blocked('Duplicate locked project')
    names = {'reimagined-0.1.0-alpha.jar'}
    for record in records.values():
        if record['minecraft'] != MC or record['loader'] != 'neoforge' or not record['version_id']:
            raise Blocked('Wrong/unknown locked loader/version')
        if not record['sides'] or not set(record['sides']).issubset({'client','server'}):
            raise Blocked('Unknown locked side')
        safe_url(record['file']['url'],FILE_HOSTS)
        name = record['file']['filename']
        if name.casefold() in names:
            raise Blocked('Duplicate/reserved JAR filename')
        names.add(name.casefold())
        for dependency in record['dependencies']:
            if dependency not in records or not set(record['sides']).issubset(records[dependency]['sides']):
                raise Blocked('Missing or wrong-side locked dependency')
    resolver = Resolver(None,{})
    resolver.selected = records
    resolver.constraints = plan.get('constraints',[])
    if resolver.conflicts():
        raise Blocked('Locked dependency incompatibility')
    chosen = {t['id'] for t in plan['targets'] if t['selected'] and t['status']=='resolved'}
    if {'nullscape','betterend-new-dawn'}.issubset(chosen):
        raise Blocked('Two End providers cannot be activated together')
    for t in plan['targets']:
        if t['selected'] and t['status']=='resolved' and t.get('key') not in records:
            raise Blocked('Resolved target missing its artifact')
    return records, missing


def java_mod_ids(path):
    import tomllib
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        if len(names)>100000:
            raise Blocked('Excessive JAR entry budget')
        for name in ('META-INF/neoforge.mods.toml','META-INF/mods.toml'):
            if name in names:
                if jar.getinfo(name).file_size > 2*1024*1024:
                    raise Blocked('Oversized mod descriptor')
                metadata = tomllib.loads(jar.read(name).decode('utf-8-sig'))
                return [m['modId'] for m in metadata.get('mods',[]) if '${' not in m['modId']]
        if 'fabric.mod.json' in names:
            raise Blocked('Fabric-only JAR slipped through NeoForge catalog metadata')
    return []  # Library/datapack wrapper with explicit NeoForge metadata selection, not guessed support.



def validate_downloaded_pins(records,downloaded,output):
    pins=[r for r in records.values() if r.get('pin_source')=='pack/mods.json']
    if not pins:return
    with tempfile.TemporaryDirectory(prefix='pin-check-',dir=output) as directory:
        directory=Path(directory)
        for record in pins:shutil.copyfile(downloaded[record['key']],directory/record['file']['filename'])
        result=subprocess.run([sys.executable,str(ROOT/'scripts/validate-modpack.py'),'--mods',str(directory)],capture_output=True,text=True,encoding='utf-8',env={**os.environ,'PYTHONUTF8':'1'})
        if result.returncode:raise Blocked('Pinned JAR hash/dependency inspection failed: '+result.stdout+result.stderr)
        (output/'PIN_VALIDATION.txt').write_text(result.stdout,encoding='utf-8')


def install(plan, output, transport, core, allow_incomplete=False):
    records, missing = validate_plan(plan,allow_incomplete)
    if not core.is_file() or not zipfile.is_zipfile(core):
        raise Blocked('Provide the previously built Reimagined Core JAR with --core-jar')
    if 'reimagined' not in java_mod_ids(core):
        raise Blocked('Provided Core JAR does not declare reimagined')
    if any((output / side).exists() for side in ('client','server')):
        raise Blocked('Fresh output required: existing profiles/worlds/mods will never be overwritten')
    cache = output / 'downloads'; cache.mkdir(parents=True,exist_ok=True)
    downloaded = {}
    seen_ids = {'reimagined':'local-core'}
    for key, record in records.items():
        path = transport.download(record['file'],cache)
        for ident in java_mod_ids(path):
            if ident in seen_ids:
                raise Blocked('Duplicate Java mod ID '+ident+' from '+key+' and '+seen_ids[ident])
            seen_ids[ident]=key
        downloaded[key]=path
    validate_downloaded_pins(records,downloaded,output)
    audits = []
    for side in ('client', 'server'):
        paths = [core] + [downloaded[key] for key, record in records.items() if side in record['sides']]
        audits.append(audit_jars.audit(paths, side))
    (output/'DEPENDENCY_AUDIT.json').write_text(json.dumps(audits, indent=2)+'\n', encoding='utf-8')
    errors = [error for audit in audits for error in audit['errors']]
    if errors:
        raise Blocked('Downloaded JAR dependencies: ' + '; '.join(errors))
    for side in ('client','server'):
        dest=output/side; (dest/'mods').mkdir(parents=True)
        shutil.copy2(core,dest/'mods/reimagined-0.1.0-alpha.jar')
        for key,record in records.items():
            if side in record['sides']:
                shutil.copy2(downloaded[key],dest/'mods'/record['file']['filename'])
        overrides=ROOT/'modpack/overrides'
        if side=='client':
            shutil.copytree(overrides,dest,dirs_exist_ok=True)
        else:
            shutil.copy2(ROOT/'modpack/server/server.properties',dest/'server.properties')
            ftb=overrides/'config/ftbquests'
            if ftb.is_dir():shutil.copytree(ftb,dest/'config/ftbquests')
            shutil.copytree(overrides/'kubejs',dest/'kubejs',ignore=shutil.ignore_patterns('client_scripts'))
        shutil.copy2(ROOT/('modpack/jvm-args.txt' if side=='client' else 'modpack/server/jvm-args.txt'),dest/'jvm-args.txt')
        (dest/'PROFILE_STATUS.txt').write_text('CANDIDATE — NOT RUNTIME VERIFIED\nMissing targets: '+', '.join(missing)+'\nMinecraft1.21.1 NeoForge21.1.264 Java21\nNo EULA accepted. No worlds provided.\n',encoding='utf-8')
    # Standard Modrinth pack can be imported by Prism; Minecraft/NeoForge installed by launcher.
    index=dict(formatVersion=1,game='minecraft',versionId='candidate-not-runtime-tested',
               name='Reimagined candidate'+(' - INCOMPLETE' if missing else ''),
               summary='Personal two-player profile; see INSTALL_REPORT.md before playing',files=[],
               dependencies={'minecraft':MC,'neoforge':NF})
    for key,record in records.items():
        if 'client' not in record['sides']:
            continue
        raw=downloaded[key].read_bytes()
        index['files'].append(dict(path='mods/'+record['file']['filename'],fileSize=len(raw),
                                   hashes={h:hashlib.new(h,raw).hexdigest() for h in ('sha1','sha512')},
                                   downloads=[record['file']['url']],
                                   env={'client':'required','server':'required' if 'server' in record['sides'] else 'unsupported'}))
    with zipfile.ZipFile(output/'Reimagined-candidate.mrpack','w',zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('modrinth.index.json',json.dumps(index,indent=2)+'\n')
        archive.write(core,'overrides/mods/reimagined-0.1.0-alpha.jar')
        for p in sorted((ROOT/'modpack/overrides').rglob('*')):
            if p.is_file() and not p.is_symlink():
                archive.write(p,'overrides/'+p.relative_to(ROOT/'modpack/overrides').as_posix())
    installed=copy.deepcopy(plan)
    installed['state']='downloaded-hash-verified-not-runtime-tested'
    for record in installed['mods']:
        record['sha256']=hashlib.sha256(downloaded[record['key']].read_bytes()).hexdigest()
    (output/'installed.lock.json').write_text(json.dumps(installed,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'Prepared {len(records)} official mods/dependencies. Missing targets: {len(missing)}. Profiles are NOT runtime certified.')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command',choices=['resolve','install'])
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--bindings',type=Path,default=ROOT/'modpack/source-bindings.json')
    parser.add_argument('--plan',type=Path)
    parser.add_argument('--core-jar',type=Path,default=ROOT/'payload/reimagined-0.1.0-alpha.jar')
    parser.add_argument('--end',choices=['betterend','nullscape'],default='nullscape')
    parser.add_argument('--allow-beta',action='store_true')
    parser.add_argument('--allow-incomplete',action='store_true')
    parser.add_argument('--pinned-only',action='store_true')
    parser.add_argument('--include-optional',action='store_true')
    parser.add_argument('--ftb-maven',action='store_true',help='Verify reviewed FTB production candidates through official Maven')
    parser.add_argument('--local-files',type=Path,help='Manually downloaded official JARs; exact plan checksums required')
    args=parser.parse_args()
    transport=Transport(os.environ.get('CURSEFORGE_API_KEY'), args.local_files)
    try:
        if args.command=='resolve':
            registry=json.loads((ROOT/'modpack/mod-registry.json').read_text(encoding='utf-8'))
            bindings=json.loads(args.bindings.read_text(encoding='utf-8'))
            plan=resolve(registry,bindings,transport,args.end,args.allow_beta,ROOT/'pack/mods.json' if (ROOT/'pack/mods.json').exists() else None,args.pinned_only,args.include_optional,args.output/'downloads' if args.ftb_maven else None)
            write_reports(plan,args.output)
            missing=sum(t['selected'] and t['status']!='resolved' for t in plan['targets'])
            print(f'Checked all {len(plan["targets"])} candidates; resolved {len(plan["mods"])} artifacts, unresolved selected targets {missing}. See INSTALL_REPORT.md')
            return 2 if missing or plan['conflicts'] else 0
        plan_path=args.plan or args.output/'install-plan.json'
        plan=json.loads(plan_path.read_text(encoding='utf-8'))
        install(plan,args.output,transport,args.core_jar,args.allow_incomplete)
        return 0
    except (Blocked,OSError,ValueError,KeyError,TypeError,zipfile.BadZipFile) as exc:
        print('STOP: '+str(exc),file=sys.stderr)
        return 1


if __name__=='__main__':
    raise SystemExit(main())
