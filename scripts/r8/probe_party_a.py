#!/usr/bin/env python3
"""Research-only official Mekanism module probe; downloads into a NEW cache, never installs."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import tomllib
import zipfile

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'scripts/modpack'))
import audit_jars
spec=importlib.util.spec_from_file_location('r8_installer',ROOT/'scripts/modpack/install.py')
installer=importlib.util.module_from_spec(spec);spec.loader.exec_module(installer)
from research_addons import fetch

MODULES={
 'mekanism-generators':('OFVYKsAk','mekanismgenerators'),
 'mekanism-tools':('tqQpq1lt','mekanismtools'),
 'mekanism-additions':('a6F3uASn','mekanismadditions'),
}


def module_candidates(versions,base):
    return [v for v in versions if v.get('version_number')==base['version']
        and v.get('project_id') in {p for p,_ in MODULES.values()}
        and v.get('version_type')==base.get('release_type','release')
        and '1.21.1' in v.get('game_versions',[]) and 'neoforge' in v.get('loaders',[])]


def inspect_module(path,expected_id,base_version):
    mods,deps,notes=audit_jars.inspect(path)
    own=[m for m in mods if m['id']==expected_id and not m['embedded']]
    if len(own)!=1 or own[0]['version']!=base_version:
        raise ValueError('Production mod ID/version differs from the recorded R7 Mekanism')
    # R7's general inspector does not yet normalize uppercase dependency types.
    # Research probe reads the root descriptor explicitly and refuses unknown closures.
    with zipfile.ZipFile(path) as jar:
        if not any(n.endswith('.class') for n in jar.namelist()):
            raise ValueError('No compiled game classes; not a production game JAR')
        if 'META-INF/jarjar/metadata.json' in jar.namelist() and json.loads(jar.read('META-INF/jarjar/metadata.json')).get('jars'):
            raise ValueError('Embedded library closure needs manual review before metadata acceptance')
        descriptor=next(n for n in ('META-INF/neoforge.mods.toml','META-INF/mods.toml') if n in jar.namelist())
        raw=tomllib.loads(jar.read(descriptor).decode('utf-8-sig'))
    if raw.get('modLoader')!='javafml' or audit_jars.numeric_range_contains('4.0.45',raw.get('loaderVersion','')) is not True:
        raise ValueError('Unverified FML loader metadata; review before acceptance')
    if not raw.get('license'):raise ValueError('Game descriptor license missing; review before metadata acceptance')
    root_deps=raw.get('dependencies',{}).get(expected_id,[])
    if any(str(d.get('type','required' if d.get('mandatory',True) else 'optional')).lower() not in ('required','optional','incompatible','discouraged') for d in root_deps):
        raise ValueError('Unknown dependency relation; review before acceptance')
    required=[{'id':d['modId'],'range':d.get('versionRange','')} for d in root_deps
              if str(d.get('type','required' if d.get('mandatory',True) else 'optional')).lower()=='required']
    base=[d for d in required if d['id']=='mekanism']
    if len(base)!=1 or base[0]['range'].replace('${file.jarVersion}',own[0]['version'])!='['+base_version+']':
        raise ValueError('Module does not require exactly the recorded base version; review needed')
    for dep in required:
        if dep['id'] in ('minecraft','neoforge'):
            target={'minecraft':'1.21.1','neoforge':'21.1.264'}[dep['id']]
            if audit_jars.platform_range_contains(dep['id'],target,dep['range']) is not True:
                raise ValueError('Unverified platform constraint: '+str(dep))
        elif dep['id']!='mekanism':raise ValueError('Additional mandatory dependency needs review: '+str(dep))
    return {'mods':mods,'dependencies':deps,'notes':notes,'descriptor_license':raw['license']}


def probe(ident,base,cache,get=fetch,transport=None,base_game_version=None):
    project_id,mod_id=MODULES[ident]
    project=get('project/'+project_id)
    if project.get('id')!=project_id:raise ValueError('Official publisher project ID mismatch')
    if set(installer.sides(project))!={'client','server'}:raise ValueError('Module side metadata differs from reviewed common distribution')
    if (project.get('license') or {}).get('id')!='MIT':raise ValueError('License differs from reviewed Mekanism MIT; review before download')
    versions=get('project/'+project_id+'/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D')
    matching=[v for v in module_candidates(versions,base) if v['project_id']==project_id]
    if len(matching)!=1:raise ValueError('No unique official exact base-version/channel match: '+base['version'])
    v=matching[0]
    for dep in v.get('dependencies',[]):
        if dep.get('dependency_type')=='incompatible':raise ValueError('Catalog incompatibility requires review before metadata acceptance')
        if dep.get('dependency_type')=='required':
            pid,vid=dep.get('project_id'),dep.get('version_id')
            if pid not in (None,base.get('project_id')) or vid not in (None,base.get('version_id')) or not (pid or vid):
                raise ValueError('Catalog requires another project/exact base file; frozen R7 must not be changed')
    file=installer.Resolver(None,{}).primary_file(v)
    path=(transport or installer.Transport()).download(file,cache)
    if not base_game_version:raise ValueError('Actual frozen base JAR game version is required; do not assume catalog version equals descriptor')
    descriptor=inspect_module(path,mod_id,base_game_version)
    return {'id':ident,'status':'VERIFIED_METADATA','runtime_verified':False,'installed':False,
            'project_id':project_id,'source_repository':'https://github.com/mekanism/Mekanism',
            'version_id':v['id'],'version':v['version_number'],'release_date':v['date_published'],
            'file':file,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
            'license':project['license'],'sides':installer.sides(project),'catalog_dependencies':v.get('dependencies',[]),
            'descriptor':descriptor,'base_version':base['version'],'base_game_version':base_game_version,'recorded_base_sha256':base.get('sha256'),
            'limitation':'Official checksums/JAR metadata only. No client, server, two-player, save or performance test.'}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--r7-downloads',type=Path,help='Read-only reuse of the existing baseline Mekanism download')
    args=parser.parse_args()
    lock_path=ROOT/'pack/r8/baseline/r7-installed.lock.json'
    raw=lock_path.read_bytes();snapshot=json.loads((ROOT/'pack/r8/r7-source-snapshot.json').read_text(encoding='utf-8'))
    if hashlib.sha256(raw).hexdigest()!=snapshot['full_installed_lock']['sha256']:raise SystemExit('Frozen R7 lock changed')
    lock=json.loads(raw)
    reviewed=json.loads((ROOT/'pack/r8/addon-research.json').read_text(encoding='utf-8'))['candidates']
    ids={c['id']:(c.get('project_id') or {}).get('modrinth') for c in reviewed}
    if any(ids.get(ident)!=project_id for ident,(project_id,_) in MODULES.items()):raise SystemExit('Module project IDs differ from reviewed source inventory')
    if (lock['minecraft'],lock['neoforge'])!=('1.21.1','21.1.264'):raise SystemExit('Wrong baseline platform')
    base_key=next(t['key'] for t in lock['targets'] if t['id']=='mekanism')
    base=next(m for m in lock['mods'] if m['key']==base_key)
    if args.output.exists():raise SystemExit('Output exists: choose a new filename')
    args.output.parent.mkdir(parents=True,exist_ok=True)
    cache=args.output.parent/(args.output.stem+'-artifact-cache')
    if cache.exists():raise SystemExit('Use a new output name: research cache must be fresh, never the R7 cache')
    cache.mkdir()
    report={'r7_lock_sha256':snapshot['full_installed_lock']['sha256'],'runtime_verified':False,'installed':False,'results':[]}
    transport=installer.Transport()
    transport.reuse_caches=[args.r7_downloads] if args.r7_downloads else []
    try:
        base_path=transport.download(base['file'],cache)
        if hashlib.sha256(base_path.read_bytes()).hexdigest()!=base['sha256']:raise ValueError('Frozen base content SHA256 mismatch')
        base_mods,_,_=audit_jars.inspect(base_path)
        owners=[m for m in base_mods if m['id']=='mekanism' and not m['embedded']]
        if len(owners)!=1 or not owners[0]['version'] or '${' in owners[0]['version']:raise ValueError('Actual base game version cannot be read')
        game_version=owners[0]['version']
        report['base_game_descriptor']=owners[0]
        report['base_content_sha256']=base['sha256']
    except Exception as exc:
        report['baseline_error']=str(exc)
        with args.output.open('x',encoding='utf-8') as f:json.dump(report,f,ensure_ascii=False,indent=2)
        raise SystemExit('Frozen base inspection failed; report saved: '+str(args.output))
    for ident in MODULES:
        print('Inspecting official exact-version artifact:',ident,flush=True)
        try:report['results'].append(probe(ident,base,cache,transport=transport,base_game_version=game_version))
        except Exception as exc:report['results'].append({'id':ident,'status':'UNVERIFIED','error':str(exc),'runtime_verified':False,'installed':False})
    report['all_metadata_verified']=all(r['status']=='VERIFIED_METADATA' for r in report['results'])
    with args.output.open('x',encoding='utf-8') as f:json.dump(report,f,ensure_ascii=False,indent=2)
    print('Research report only. No profiles/worlds/R7 files changed:',args.output)
    print('Metadata verified:',sum(r['status']=='VERIFIED_METADATA' for r in report['results']),'/ 3; runtime NOT tested')
    return 0 if report['all_metadata_verified'] else 2

if __name__=='__main__':raise SystemExit(main())
