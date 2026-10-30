"""Opt-in official FTB Maven candidate: verify publication, checksum AND game JAR.
No snapshots, development classifiers, guessed CDN links or runtime certification.
Coordinates are owned by pack/mods.json; no second lockfile/quest implementation.
"""
import json
import re
import tomllib
import xml.etree.ElementTree as ET
import zipfile

HOSTS = {'maven.ftb.dev'}
BASE = 'https://maven.ftb.dev/releases'


def read(transport, url, limit):
    with transport.open(url, HOSTS) as response:
        data = response.read(limit + 1)
    if len(data) > limit:
        raise ValueError('Oversized official Maven response')
    return data


def confirm_game_jar(path, entry):
    with zipfile.ZipFile(path) as jar:
        name = 'META-INF/neoforge.mods.toml'
        if name not in jar.namelist() or jar.getinfo(name).file_size > 2 * 1024 * 1024:
            raise ValueError('Official Maven artifact lacks bounded NeoForge game descriptor')
        data = tomllib.loads(jar.read(name).decode('utf-8-sig'))
        declared = [m for m in data.get('mods', []) if m.get('modId') == entry['modId']]
        if len(declared) != 1 or declared[0].get('version') != entry['version']:
            raise ValueError('Maven game mod ID/version mismatch; dev/unexpanded artifact refused')
        deps = data.get('dependencies', {}).get(entry['modId'], [])
        platform = {d['modId']: d.get('versionRange') for d in deps if d.get('type') == 'required'}
        # Source-reviewed FTB1.21.1 platform ranges; unfamiliar binaries require review.
        if platform.get('minecraft') not in ('[1.21.1,)', '[1.21.1]', '[1.21.1,1.22)') or platform.get('neoforge') != '[21.1.0,)':
            raise ValueError('Unreviewed Minecraft/NeoForge platform ranges in FTB artifact')
        if not any(n.endswith('.class') for n in jar.namelist()):
            raise ValueError('Maven artifact contains no game classes')
        return [d['modId'] for d in deps if d.get('type') == 'required' and d['modId'] not in ('minecraft', 'neoforge')]


def resolve(resolver, manifest, cache):
    config = json.loads(manifest.read_text(encoding='utf-8'))['officialMavenCandidates']['ftb']
    if config['repository'] != BASE or config['group'] != 'dev.ftb.mods':
        raise ValueError('Unreviewed FTB repository/coordinate namespace')
    entries = config['artifacts']
    expected = {'ftb-library-neoforge':'ftblibrary','ftb-teams-neoforge':'ftbteams','ftb-quests-neoforge':'ftbquests'}
    if len(entries) != 3 or {e['artifact']:e['modId'] for e in entries} != expected:
        raise ValueError('FTB bundle must include exactly the three reviewed NeoForge artifacts')
    pending, requirements = {}, {}
    cache.mkdir(parents=True, exist_ok=True)
    for entry in entries:
        artifact, version = entry['artifact'], entry['version']
        if not re.fullmatch(r'2101\.\d+\.\d+', version):
            raise ValueError('Only reviewed FTB1.21.1 release coordinates, no snapshots/classifiers')
        root = BASE + '/dev/ftb/mods/' + artifact
        raw = read(resolver.t, root + '/maven-metadata.xml', 1024 * 1024)
        if b'<!DOCTYPE' in raw.upper() or b'<!ENTITY' in raw.upper():
            raise ValueError('Unexpected XML entity declaration')
        try:
            metadata = ET.fromstring(raw)
        except ET.ParseError as exc:
            raise ValueError("Invalid official Maven metadata XML") from exc
        if metadata.findtext('groupId') != config['group'] or metadata.findtext('artifactId') != artifact:
            raise ValueError('Official Maven metadata coordinate mismatch')
        if version not in [n.text for n in metadata.findall('./versioning/versions/version')]:
            raise ValueError('Source version is not published on official Maven: ' + artifact + ':' + version)
        filename = artifact + '-' + version + '.jar'
        url = root + '/' + version + '/' + filename
        checksum = read(resolver.t, url + '.sha1', 256).decode('ascii').strip()
        if not re.fullmatch(r'[0-9a-fA-F]{40}', checksum):
            raise ValueError('Missing valid official Maven SHA1; no locally invented trust anchor')
        # GET headers only: actual bounded body/hash validation uses the common transport.
        with resolver.t.open(url, HOSTS) as response:
            size = int(response.headers.get('Content-Length', '0'))
        file = dict(url=url,filename=filename,hashes={'sha1':checksum.lower()},size=size)
        path = resolver.t.download(file, cache)
        requirements[entry['modId']] = confirm_game_jar(path, entry)
        key = 'ftb-maven:' + artifact
        pending[key] = dict(key=key,source='ftb-maven',project_id=artifact,name=entry['name'],version_id=version,version=version,
            minecraft='1.21.1',loader='neoforge',sides=['client','server'],license='All Rights Reserved; official personal download only',
            source_url=config['source'],file=file,dependencies=[],optional_dependencies=[],
            provenance='Official Maven metadata + SHA1 + production NeoForge descriptor; runtime unverified')
    # Actual descriptors, not a guessed Java API or Maven transitive dependency graph.
    ids = {entry['modId']:'ftb-maven:'+entry['artifact'] for entry in entries}
    required = set(d for deps in requirements.values() for d in deps)
    if required - set(ids) - {'architectury'}:
        raise ValueError('Unreviewed additional FTB game dependency: ' + ', '.join(sorted(required-set(ids)-{'architectury'})))
    if 'architectury' in required:
        project = resolver.identify_mr({'name':'Architectury API'}, {'modrinth_slug_hint':'architectury-api','accepted_titles':['Architectury API']})
        ids['architectury'] = resolver.mr(project['id'])
    for entry in entries:
        pending[ids[entry['modId']]]['dependencies'] = [ids[d] for d in requirements[entry['modId']]]
    resolver.selected.update(pending)
    return ids['ftbquests']
