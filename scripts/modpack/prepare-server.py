#!/usr/bin/env python3
"""Prepare the matching Windows dedicated server using the official NeoForge installer.
Does not start Minecraft, open firewall ports or accept/write the Minecraft EULA.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

spec=importlib.util.spec_from_file_location('installer',Path(__file__).with_name('install.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
HOSTS={'maven.neoforged.net'}
BASE='https://maven.neoforged.net/releases/net/neoforged/neoforge/'+m.NF
NAME='neoforge-'+m.NF+'-installer.jar'


def prepare(profile, java, transport):
    profile=profile.resolve()
    if not (profile/'mods/reimagined-0.1.0-alpha.jar').is_file() or not (profile/'server.properties').is_file():
        raise m.Blocked('Choose the generated server profile, not an existing world or empty folder')
    for name in ('world','eula.txt','libraries','run.bat','run.sh'):
        if (profile/name).exists():
            raise m.Blocked('Existing server state must not be overwritten: '+name)
    result=subprocess.run([java,'-version'],capture_output=True,text=True,check=True)
    if not re.search(r'version "21[.\"]',result.stdout+result.stderr):
        raise m.Blocked('Java21 required; select --java with the full executable path')
    url=BASE+'/'+NAME
    with transport.open(url+'.sha1',HOSTS) as response:
        checksum=response.read(257).decode('ascii').strip()
    if not re.fullmatch(r'[a-fA-F0-9]{40}',checksum):
        raise m.Blocked('Invalid official NeoForge installer checksum')
    # Use the shared checked transport, only for this exact official installer URL.
    cache=profile.parent/'loader-downloads';cache.mkdir(exist_ok=True)
    local=cache/NAME
    if local.exists():
        if local.is_symlink() or hashlib.sha1(local.read_bytes()).hexdigest()!=checksum.lower():
            raise m.Blocked('Cached official installer checksum mismatch')
    else:
        temporary=local.with_suffix('.part')
        if temporary.exists():raise m.Blocked('Inspect stale installer partial file first')
        try:
            with transport.open(url,HOSTS) as response,temporary.open('xb') as output:
                size=0
                while chunk:=response.read(1024*1024):
                    size+=len(chunk)
                    if size>64*1024*1024:raise m.Blocked('Installer download budget exceeded')
                    output.write(chunk)
            if hashlib.sha1(temporary.read_bytes()).hexdigest()!=checksum.lower():
                raise m.Blocked('Official installer checksum mismatch')
            temporary.rename(local)
        finally:
            if temporary.exists():temporary.unlink()
    with zipfile.ZipFile(local) as jar:
        if 'install_profile.json' not in jar.namelist():raise m.Blocked('Not a NeoForge installer')
        if jar.getinfo('install_profile.json').file_size>2*1024*1024:raise m.Blocked('Oversized installer metadata')
        metadata=json.loads(jar.read('install_profile.json'))
        if metadata.get('minecraft')!=m.MC or metadata.get('path')!='net.neoforged:neoforge:'+m.NF:
            raise m.Blocked('Wrong official installer Minecraft/NeoForge platform')
    with (profile/'loader-install.log').open('w',encoding='utf-8') as log:
        subprocess.run([java,'-jar',str(local),'--installServer'],cwd=profile,stdout=log,stderr=subprocess.STDOUT,check=True)
    if not (profile/'run.bat').exists():
        raise m.Blocked('Installer did not produce Windows launcher; inspect loader-install.log')
    args=profile/'user_jvm_args.txt'
    if args.exists():shutil.copy2(args,profile/'user_jvm_args.installer-original.txt')
    args.write_text((profile/'jvm-args.txt').read_text(encoding='utf-8')+'\n',encoding='utf-8')
    print('Official server prepared. Read the Minecraft EULA and decide locally before starting run.bat. No EULA accepted; no world launched.')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--profile',type=Path,required=True)
    parser.add_argument('--java',default='java')
    args=parser.parse_args()
    try:prepare(args.profile,args.java,m.Transport())
    except (m.Blocked,OSError,ValueError,subprocess.SubprocessError,zipfile.BadZipFile) as exc:
        print('STOP: '+str(exc),file=sys.stderr);return 1
    return 0

if __name__=='__main__':raise SystemExit(main())
