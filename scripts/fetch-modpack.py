#!/usr/bin/env python3
"""Fetch pack/mods.json using the same verified transport as the Windows installer."""
import argparse
import hashlib
from pathlib import Path
import shutil
import sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'scripts/modpack'))
from install import pinned_records, Transport, Blocked


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--destination',required=True,type=Path)
    p.add_argument('--optional',action='store_true')
    p.add_argument('--dry-run',action='store_true')
    args=p.parse_args()
    records,_,_=pinned_records(ROOT/'pack/mods.json',args.optional)
    dest=args.destination.expanduser()
    if args.dry_run:
        for m in records.values():print('Pinned:',m['name'],m['version'],m['file']['filename'])
        print(f'Dry run: {len(records)} pinned files, no downloads')
        return 0
    dest.mkdir(parents=True,exist_ok=True)
    cache=dest.parent/'.reimagined-download-cache';cache.mkdir(exist_ok=True)
    transport=Transport()
    for record in records.values():
        file=record['file'];target=dest/file['filename']
        if target.exists():
            if target.is_symlink() or hashlib.sha512(target.read_bytes()).hexdigest()!=file['hashes']['sha512']:
                raise Blocked('Existing mismatched artifact preserved; inspect before retry: '+target.name)
            continue
        source=transport.download(file,cache)
        shutil.copy2(source,target)
        print('Verified:',target.name)
    print(f'{len(records)} pinned files present. Run validate-modpack.py; runtime is a separate gate.')
    return 0

if __name__=='__main__':
    try:raise SystemExit(main())
    except (Blocked,OSError,ValueError,KeyError) as e:print('STOP:',e,file=sys.stderr);raise SystemExit(1)
