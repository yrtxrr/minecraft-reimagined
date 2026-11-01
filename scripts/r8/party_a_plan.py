"""Pinned composition from frozen R7 + supplied module metadata; no resolution/upgrades."""
import copy
import hashlib
import json
from pathlib import Path
import re
import urllib.parse

MODULES={'mekanism-generators':('OFVYKsAk','mekanismgenerators'),
         'mekanism-tools':('tqQpq1lt','mekanismtools'),
         'mekanism-additions':('a6F3uASn','mekanismadditions')}


def make_plan(base,evidence,base_sha):
    if evidence.get('r7_lock_sha256')!=base_sha or not evidence.get('all_metadata_verified'):
        raise ValueError('Evidence baseline/hash/status mismatch')
    mods={m['key']:m for m in base['mods']}
    base_key=next(t['key'] for t in base['targets'] if t['id']=='mekanism')
    mek=mods[base_key];game=evidence['base_game_descriptor']
    if evidence['base_content_sha256']!=mek['sha256'] or game['id']!='mekanism':raise ValueError('Wrong base artifact')
    rows=evidence['results']
    if len(rows)!=3 or {r['id'] for r in rows}!=set(MODULES):raise ValueError('Exactly three reviewed modules required')
    plan=copy.deepcopy(base);new=[]
    for r in rows:
        project,modid=MODULES[r['id']]
        if r['project_id']!=project or r['status']!='VERIFIED_METADATA' or r['runtime_verified'] or r['installed']:
            raise ValueError('Wrong project or unsupported evidence state')
        if r['version']!=mek['version'] or r['base_game_version']!=game['version'] or r['recorded_base_sha256']!=mek['sha256']:
            raise ValueError('Module/base build mismatch')
        if r['license']['id']!='MIT' or set(r['sides'])!={'client','server'}:raise ValueError('License/side mismatch')
        if not re.fullmatch('[0-9a-f]{64}',r['sha256']):raise ValueError('Invalid SHA256')
        own=[m for m in r['descriptor']['mods'] if m['id']==modid and not m['embedded']]
        if len(own)!=1 or own[0]['version']!=game['version']:raise ValueError('Wrong game descriptor')
        required=[d for d in r['descriptor']['dependencies'] if d.get('type','required').lower()=='required']
        if len(required)!=1 or required[0]['id']!='mekanism' or required[0]['range']!='['+game['version']+']':
            raise ValueError('Unexpected required dependency closure')
        if r['descriptor'].get('descriptor_license')!='MIT':raise ValueError('Game license differs from reviewed MIT')
        if len(r['catalog_dependencies'])!=1 or r['catalog_dependencies'][0]['dependency_type']!='required':raise ValueError('Expected exactly one catalog base dependency')
        for dep in r['catalog_dependencies']:
            if dep['dependency_type']=='required' and (dep.get('project_id')!=mek['project_id'] or dep.get('version_id')!=mek['version_id']):
                raise ValueError('Catalog base dependency differs from frozen R7')
        file=copy.deepcopy(r['file']);u=urllib.parse.urlsplit(file['url'])
        if u.scheme!='https' or u.netloc!='cdn.modrinth.com' or not u.path.startswith('/data/'+project+'/versions/'+r['version_id']+'/'):
            raise ValueError('Unexpected official artifact URL')
        if not re.fullmatch('[0-9a-f]{128}',file['hashes']['sha512']):raise ValueError('Invalid official SHA512')
        key='modrinth:'+project
        if key in mods:raise ValueError('Addon already present in baseline')
        plan['mods'].append(dict(key=key,source='modrinth',project_id=project,name=r['id'],version_id=r['version_id'],
            version=r['version'],release_type=mek.get('release_type','release'),minecraft='1.21.1',loader='neoforge',
            sides=['client','server'],license=r['license'],source_url='https://modrinth.com/mod/'+project,
            file=file,sha256=r['sha256'],dependencies=[base_key],optional_dependencies=[],metadata_status='VERIFIED_METADATA'))
        new.append(key)
    plan.update(state='r8-party-a-metadata-verified-runtime-not-tested',runtime_verified=False,
                r8_party='A',r7_lock_sha256=base_sha,addon_keys=new)
    return plan


def load_plan(root):
    raw=(root/'pack/r8/baseline/r7-installed.lock.json').read_bytes()
    snapshot=json.loads((root/'pack/r8/r7-source-snapshot.json').read_text(encoding='utf-8'))
    sha=hashlib.sha256(raw).hexdigest()
    if sha!=snapshot['full_installed_lock']['sha256']:raise ValueError('Frozen baseline changed')
    return make_plan(json.loads(raw),json.loads((root/'pack/r8/party-a/evidence.json').read_bytes()),sha)

if __name__=='__main__':
    root=Path(__file__).resolve().parents[2]
    (root/'pack/r8/party-a/install-plan.json').write_text(json.dumps(load_plan(root),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
