"""Local-only source build and isolated Prism profile exporter. No port redistribution."""
import argparse
from datetime import datetime
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE))
spec=importlib.util.spec_from_file_location('installer',HERE/'install.py')
installer=importlib.util.module_from_spec(spec);spec.loader.exec_module(installer)
import audit_jars

DEPENDENCIES = {
 'terrablender': ('TerraBlender', ['TerraBlender']),
 'curios': ('Curios API', ['Curios API', 'Curios API (Forge/NeoForge)']),
 'patchouli': ('Patchouli', ['Patchouli']),
}


def run(command,cwd,logs,label):
    log=logs/(label+'.log')
    print('Running '+label+'; log: '+str(log),flush=True)
    with log.open('w',encoding='utf-8') as handle:
        result=subprocess.run(command,cwd=cwd,stdout=handle,stderr=subprocess.STDOUT)
    if result.returncode:raise ValueError(label+' failed; see '+str(log))


def source_build(port,work,logs):
    folder=work/'sources'/port['mod_id']
    if not folder.exists():run(['git','clone',port['repository'],str(folder)],work,logs,port['mod_id']+'-clone')
    remote=subprocess.check_output(['git','remote','get-url','origin'],cwd=folder,text=True).strip().removesuffix('.git')
    if remote!=port['repository']:raise ValueError('Unexpected source repository: '+str(folder))
    dirty=subprocess.check_output(['git','status','--porcelain'],cwd=folder,text=True).strip()
    if dirty:raise ValueError('Source worktree has edits; refusing checkout/build: '+str(folder))
    run(['git','checkout','--detach',port['revision']],folder,logs,port['mod_id']+'-checkout')
    actual=subprocess.check_output(['git','rev-parse','HEAD'],cwd=folder,text=True).strip()
    if actual!=port['revision']:raise ValueError('Source revision mismatch')
    # No toolchain/config patches, disabled tests, credential/proxy/TLS workarounds.
    run([str(folder/'gradlew.bat'),'build','--no-daemon'],folder,logs,port['mod_id']+'-build')
    candidates=[]
    for jar in (folder/'build/libs').glob('*.jar'):
        if any(x in jar.name.lower() for x in ('-sources','-javadoc','-dev','-api')):continue
        mods,_,_=audit_jars.inspect(jar)
        if any(m['id']==port['mod_id'] and not m['embedded'] and m['version']==port['source_version'] for m in mods):candidates.append(jar)
    if len(candidates)!=1:raise ValueError('No unique production game JAR: '+port['name'])
    return candidates[0]


def profile(output,name,files):
    output.mkdir(parents=True,exist_ok=True)
    destination=output/(name+'.zip')
    if destination.exists():raise ValueError('Existing profile export will not be overwritten: '+str(destination))
    # Prism's native MultiMC instance import format. User selects Java21/RAM/account.
    pack={'formatVersion':1,'components':[{'uid':'net.minecraft','version':'1.21.1'}, {'uid':'net.neoforged','version':'21.1.264'}]}
    with zipfile.ZipFile(destination,'x',zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('mmc-pack.json',json.dumps(pack,indent=2))
        archive.writestr('instance.cfg','InstanceType=OneSix\nname='+name+'\n')
        archive.writestr('.minecraft/LAB_STATUS.txt','LOCAL PRIVATE TEST ONLY. Not runtime verified. No world/EULA supplied.\n')
        for path in files:archive.write(path,'.minecraft/mods/'+path.name)
    return destination


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work',type=Path,required=True);parser.add_argument('--core',type=Path,required=True)
    args=parser.parse_args()
    try:
        work=args.work.resolve();core=args.core.resolve()
        if not core.is_file() or 'reimagined' not in installer.java_mod_ids(core):raise ValueError('Supply the existing R4 Core game JAR')
        if core.is_relative_to(work):raise ValueError('Keep original R4 Core outside the lab directory')
        work.mkdir(parents=True,exist_ok=True)
        logs=work/'logs'/datetime.now().strftime('%Y%m%d-%H%M%S-%f');logs.mkdir(parents=True)
        cfg=json.loads((HERE/'profile.json').read_text())
        built={};errors=[]
        for port in cfg['ports']:
            try:built[port['mod_id']]=source_build(port,work,logs)
            except (ValueError,OSError,subprocess.SubprocessError) as exc:errors.append(str(exc));print(str(exc),file=sys.stderr)
        if errors:raise ValueError('Builds incomplete: '+'; '.join(errors))
        transport=installer.Transport();resolver=installer.Resolver(transport,{'dependency_sides':{}})
        keys={}
        for slug,(title,titles) in DEPENDENCIES.items():
            project=resolver.identify_mr({'name':title},{'modrinth_slug_hint':slug,'accepted_titles':titles})
            keys[slug]=resolver.mr(project['id'])
        downloads=work/'downloads';downloads.mkdir(exist_ok=True)
        downloaded={k:transport.download(r['file'],downloads) for k,r in resolver.selected.items()}
        # Preserve official filenames in an isolated staging directory.
        stage=work/'artifacts';stage.mkdir(exist_ok=True)
        staged={}
        for key,path in downloaded.items():
            dest=stage/resolver.selected[key]['file']['filename']
            if dest.exists() and dest.read_bytes()!=path.read_bytes():raise ValueError('Existing staged artifact differs: '+dest.name)
            shutil.copy2(path,dest);staged[key]=dest
        def closure(key):
            found=set();todo=[key]
            while todo:
                k=todo.pop()
                if k not in found:found.add(k);todo.extend(resolver.selected[k]['dependencies'])
            return found
        thaum=[built['thaumcraft']]+[staged[k] for k in sorted(closure(keys['terrablender'])|closure(keys['curios']))]
        bot=[built['botania']]+[staged[k] for k in sorted(closure(keys['patchouli']))]
        groups={'Thaumcraft-Lab':thaum,'Botania-Lab':bot,'Thaumcraft-Core-Lab':thaum+[core], 'Botania-Core-Lab':bot+[core], 'Both-Ports-Core-Lab':list(dict.fromkeys(thaum+bot+[core]))}
        audits={}
        for name,files in groups.items():
            for side in ('client','server'):
                report=audit_jars.audit(files,side);audits[name+'-'+side]=report
                if report['errors']:errors.extend(report['errors'])
        (logs/'dependency-audits.json').write_text(json.dumps(audits,indent=2))
        lock={'runtime_verified':False,'source_revisions':cfg['ports'],'dependencies':list(resolver.selected.values()),'files':[{'name':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in dict.fromkeys(thaum+bot+[core])]}
        (logs/'artifacts.json').write_text(json.dumps(lock,indent=2))
        if errors:raise ValueError('JAR dependency/platform preflight failed: '+'; '.join(errors))
        for name,files in groups.items():print('Import into Prism: '+str(profile(work/'profiles',name,files)))
        print('Local exports only. Do not upload port JARs/profile ZIPs. Follow the supplied runtime checklist.')
        return 0
    except (ValueError,OSError,subprocess.SubprocessError,installer.Blocked,zipfile.BadZipFile) as exc:
        print('STOP: '+str(exc),file=sys.stderr);return 1

if __name__=='__main__':raise SystemExit(main())
