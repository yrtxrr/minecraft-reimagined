#!/usr/bin/env python3
"""Read-only JEI/MoreCulling cache inventory; no network or compatibility overrides."""
import argparse
import hashlib
import json
from pathlib import Path
import tomllib
import zipfile

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--cache',type=Path,action='append',required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args()
report={'runtime_verified':False,'files':[],'errors':[]}
seen=set()
for folder in a.cache:
    for path in sorted(folder.glob('*.jar')):
        if path.is_symlink() or not path.is_file():continue
        resolved=path.resolve()
        if resolved in seen:continue
        seen.add(resolved)
        try:
            with zipfile.ZipFile(path) as jar:
                name=next((n for n in ('META-INF/neoforge.mods.toml','META-INF/mods.toml') if n in jar.namelist()),None)
                if not name:continue
                if jar.getinfo(name).file_size>2*1024*1024:raise ValueError('metadata over2MiB')
                data=tomllib.loads(jar.read(name).decode('utf-8-sig'))
                mods=[m for m in data.get('mods',[]) if m.get('modId') in ('jei','moreculling')]
                if not mods:continue
                sha=hashlib.sha512()
                with path.open('rb') as stream:
                    for chunk in iter(lambda:stream.read(1024*1024),b''):sha.update(chunk)
                report['files'].append({'path':str(resolved),'size':path.stat().st_size,'sha512':sha.hexdigest(),'descriptor':name,'mods':mods,'dependencies':data.get('dependencies',{}),'note':'Raw evidence only; range must be evaluated with actual NeoForge/FML support matrix, not bare Maven alone.'})
        except (OSError,ValueError,zipfile.BadZipFile) as exc:report['errors'].append({'path':str(path),'error':str(exc)})
with a.output.open('x',encoding='utf-8') as out:json.dump(report,out,ensure_ascii=False,indent=2)
print('Recorded',len(report['files']),'JEI/MoreCulling files. Cache untouched. Report:',a.output)
