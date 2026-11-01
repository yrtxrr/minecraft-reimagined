#!/usr/bin/env python3
"""Install only frozen R7+partyA into fresh profiles, using the existing installer."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import zipfile
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'scripts/modpack'))
from party_a_plan import load_plan
spec=importlib.util.spec_from_file_location('installer',ROOT/'scripts/modpack/install.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

class CheckedTransport(m.Transport):
    def __init__(self,plan,caches):
        super().__init__();self.reuse_caches=caches
        self.expected={r['file']['url']:r['sha256'] for r in plan['mods']}
    def download(self,file,directory):
        path=super().download(file,directory)
        if hashlib.sha256(path.read_bytes()).hexdigest()!=self.expected[file['url']]:
            raise m.Blocked('Frozen content SHA256 mismatch: '+file['filename'])
        return path

def label_mrpack(output):
    original=output/'Reimagined-candidate.mrpack'
    target=output/'Reimagined-R8-Party-A.mrpack'
    with zipfile.ZipFile(original) as source, zipfile.ZipFile(target,'x',zipfile.ZIP_DEFLATED) as dest:
        for item in source.infolist():
            data=source.read(item.filename)
            if item.filename=='modrinth.index.json':
                index=json.loads(data);index.update(name='Reimagined R8 Party A - TEST',versionId='r8-party-a-metadata-only',summary='Frozen R7 plus three matched Mekanism modules; runtime acceptance pending')
                data=json.dumps(index,ensure_ascii=False,indent=2).encode('utf-8')
            dest.writestr(item,data)
    original.unlink()  # Only the temporary pack just generated inside our fresh output.
    return target

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--reuse-downloads',type=Path,action='append',default=[])
    p.add_argument('--core-jar',type=Path,default=ROOT/'payload/reimagined-0.1.0-alpha.jar')
    args=p.parse_args()
    try:
        plan=load_plan(ROOT)
        shipped=json.loads((ROOT/'pack/r8/party-a/install-plan.json').read_text(encoding='utf-8'))
        if plan!=shipped:raise ValueError('Party A plan differs from baseline/evidence')
        snapshot=json.loads((ROOT/'pack/r8/r7-source-snapshot.json').read_text(encoding='utf-8'))
        if hashlib.sha256(args.core_jar.read_bytes()).hexdigest()!=snapshot['versions']['reimagined']['sha256']:
            raise ValueError('Core differs from the verified R7 artifact')
        if args.output.exists() and any(args.output.iterdir()):raise ValueError('Choose a fresh empty output; no worlds/profiles overwritten')
        m.write_reports(plan,args.output)
        m.install(plan,args.output,CheckedTransport(plan,args.reuse_downloads),args.core_jar)
        print('Import new test instance:',label_mrpack(args.output))
        print('Party A profiles prepared; client/server runtime NOT tested. No existing instance/world changed.')
        return 0
    except (m.Blocked,OSError,ValueError,KeyError) as exc:
        print('STOP: '+str(exc),file=sys.stderr);return 1
if __name__=='__main__':raise SystemExit(main())
