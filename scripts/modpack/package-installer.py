#!/usr/bin/env python3
"""Package source installer and owned Core only; never bundle third-party JARs or worlds."""
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile

root=Path(__file__).resolve().parents[2]
jar=root/'build/libs/reimagined-0.1.0-alpha.jar'
if not jar.is_file():
    raise SystemExit('Build Core JAR first')
with zipfile.ZipFile(jar) as archive:
    if any('/gametest/' in n or '/profile/' in n for n in archive.namelist()):
        raise SystemExit('Refusing dev fixture/profile classes in player JAR')
files={
    'START_HERE_RU.md':root/'modpack/INSTALL_WINDOWS.md',
    'payload/reimagined-0.1.0-alpha.jar':jar,
}
for name in ['install.py','Install-Reimagined.ps1','test_install.py']:
    files['scripts/modpack/'+name]=root/'scripts/modpack'/name
for name in ['mod-registry.json','source-bindings.json','INSTALL_WINDOWS.md','VERIFY_WINDOWS.md','jvm-args.txt']:
    files['modpack/'+name]=root/'modpack'/name
for directory in ['overrides','server']:
    for path in sorted((root/'modpack'/directory).rglob('*')):
        if path.is_file() and not path.is_symlink():
            files[path.relative_to(root).as_posix()]=path
files['pack/mods.json']=root/'pack/mods.json'
for name in ['fetch-modpack.py','validate-modpack.py','validate-quests.py','check-gametest-report.py']:
    files['scripts/'+name]=root/'scripts'/name
for name in ['MOD_DOWNLOAD_DIAGNOSTICS.md','KILO_PROGRESSION_REVIEW.md','QUEST_RUNTIME_INTEGRATION.md','MODPACK_AGENT_INTEGRATION.md']:
    files['docs/'+name]=root/'docs'/name
manifest=dict(revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),
              purpose='Official-source Windows installer, NOT an installed/tested full modpack',
              files={name:hashlib.sha256(path.read_bytes()).hexdigest() for name,path in files.items()})
output=root/'build/modpack/Reimagined-Windows-Setup.zip';output.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
    for name,path in files.items():archive.write(path,name)
    archive.writestr('installer-manifest.json',json.dumps(manifest,indent=2)+'\n')
print(output)
