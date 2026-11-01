#!/usr/bin/env python3
"""Package owned Core + frozen metadata/tools/configs; no external mod binaries."""
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile
ROOT=Path(__file__).resolve().parents[2]
core=ROOT/'build/libs/reimagined-0.1.0-alpha.jar'
snapshot=json.loads((ROOT/'pack/r8/r7-source-snapshot.json').read_text(encoding='utf-8'))
if hashlib.sha256(core.read_bytes()).hexdigest()!=snapshot['versions']['reimagined']['sha256']:raise SystemExit('Core differs from verified R7; refusing silent replacement')
files={'START_HERE_RU.md':ROOT/'docs/R8_PARTY_A_WINDOWS.md','payload/reimagined-0.1.0-alpha.jar':core}
for name in ['party_a_plan.py','install_party_a.py']:
    files['scripts/r8/'+name]=ROOT/'scripts/r8'/name
for name in ['install.py','audit_jars.py','official_ftb.py','prepare-server.py','collect-diagnostics.py']:
    files['scripts/modpack/'+name]=ROOT/'scripts/modpack'/name
files['scripts/validate-modpack.py']=ROOT/'scripts/validate-modpack.py'
for name in ['mods.json']:files['pack/'+name]=ROOT/'pack'/name
for name in ['mod-registry.json','jvm-args.txt']:
    files['modpack/'+name]=ROOT/'modpack'/name
for name in ['r7-source-snapshot.json','baseline/r7-installed.lock.json','party-a/evidence.json','party-a/install-plan.json']:
    files['pack/r8/'+name]=ROOT/'pack/r8'/name
for folder in ['modpack/overrides','modpack/server']:
    for p in sorted((ROOT/folder).rglob('*')):
        if p.is_file() and not p.is_symlink():files[p.relative_to(ROOT).as_posix()]=p
for name in ['R8_PARTY_A_INTEGRATION.md','R8_PARTY_A_TEST_RESULTS.md']:
    files['docs/'+name]=ROOT/'docs'/name
out=ROOT/'build/r8/Reimagined-R8-Party-A-Windows.zip';out.parent.mkdir(parents=True,exist_ok=True)
manifest={'revision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'runtime_verified':False,'external_artifacts':56,'files':{n:hashlib.sha256(p.read_bytes()).hexdigest() for n,p in files.items()}}
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
    for n,p in files.items():z.write(p,n)
    z.writestr('party-a-manifest.json',json.dumps(manifest,indent=2))
(out.parent/'PARTY-A-SHA256SUMS.txt').write_text(hashlib.sha256(out.read_bytes()).hexdigest()+'  '+out.name+'\n')
print(out)
