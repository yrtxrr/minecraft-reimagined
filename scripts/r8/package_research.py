#!/usr/bin/env python3
"""Bundle only research tools and frozen metadata, never game JARs/worlds/credentials."""
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile
ROOT=Path(__file__).resolve().parents[2]
files={'START_HERE_RU.md':ROOT/'docs/R8_RESEARCH_TOOLS_WINDOWS.md'}
for name in ['research_addons.py','probe_party_a.py','test_research.py','test_party_a.py','test_baseline.py']:
    files['scripts/r8/'+name]=ROOT/'scripts/r8'/name
for name in ['install.py','audit_jars.py','official_ftb.py']:
    files['scripts/modpack/'+name]=ROOT/'scripts/modpack'/name
for name in ['addon-research.json','r7-source-snapshot.json','baseline/r7-installed.lock.json']:
    files['pack/r8/'+name]=ROOT/'pack/r8'/name
out=ROOT/'build/r8/Reimagined-R8-Research-Tools.zip';out.parent.mkdir(parents=True,exist_ok=True)
manifest={'revision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
          'purpose':'Research only, not an installable R8 modpack','runtime_verified':False,
          'files':{n:hashlib.sha256(p.read_bytes()).hexdigest() for n,p in files.items()}}
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
    for name,path in files.items():z.write(path,name)
    z.writestr('research-tools-manifest.json',json.dumps(manifest,indent=2))
(out.parent/'SHA256SUMS.txt').write_text(hashlib.sha256(out.read_bytes()).hexdigest()+'  '+out.name+'\n')
print(out)
