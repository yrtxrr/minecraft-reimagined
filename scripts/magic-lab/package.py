"""Public tools-only ZIP: never includes Core, third-party JARs, sources or worlds."""
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile
root=Path(__file__).resolve().parents[2]
files={}
for name in ['Build-MagicLab.ps1','Collect-MagicLab.ps1','magic_lab.py']:
    files[name]=root/'scripts/magic-lab'/name
for name in ['install.py','audit_jars.py','official_ftb.py','collect-diagnostics.py']:
    files[name]=root/'scripts/modpack'/name
files['profile.json']=root/'modpack/experimental/magic-ports/profile.json'
files['START_HERE_RU.md']=root/'modpack/experimental/magic-ports/WINDOWS_SCRIPTS_RU.md'
files['RUNTIME_CHECKLIST_RU.md']=root/'modpack/experimental/magic-ports/README_RU.md'
manifest={'revision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'runtime_verified':False,'files':{name:hashlib.sha256(path.read_bytes()).hexdigest() for name,path in files.items()}}
out=root/'build/modpack/Reimagined-Magic-Ports-Windows.zip';out.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as archive:
    for name,path in files.items():archive.write(path,name)
    archive.writestr('tools-manifest.json',json.dumps(manifest,indent=2))
(out.parent/'MAGIC-LAB-SHA256SUMS.txt').write_text(hashlib.sha256(out.read_bytes()).hexdigest()+'  '+out.name+'\n')
print(out)
