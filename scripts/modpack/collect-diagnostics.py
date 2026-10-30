#!/usr/bin/env python3
"""Collect a bounded first-run report. Does not start Minecraft or modify worlds.
Only named logs/reports are copied; launcher accounts, worlds and JARs are excluded.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
import zipfile

LIMIT = 16 * 1024 * 1024
TOTAL_LIMIT = 80 * 1024 * 1024
REPORTS = ('install-plan.json', 'installed.lock.json', 'INSTALL_REPORT.md',
           'DEPENDENCY_AUDIT.json', 'PIN_VALIDATION.txt')


def redact(text):
    text = re.sub(r'(?i)(--(?:accessToken|clientToken|session)\s+)(\S+)', r'\1<redacted>', text)
    text = re.sub(r'(?i)(Bearer\s+)[A-Za-z0-9._~+/=-]+', r'\1<redacted>', text)
    text = re.sub(r'(?i)(["\']?(?:access_token|accessToken|clientToken|refresh_token|password|x-api-key|authorization)["\']?\s*[:=]\s*["\']?)[^\s"\',}]+', r'\1<redacted>', text)
    return text


def collect(instance, installation, server, output, result=None):
    sources, notes = [], []
    if not any((instance, installation, server)):
        raise ValueError('Supply --instance (Minecraft folder), --install-output or --server')
    def add(root, relative, archive_name):
        root = root.resolve()
        path = root / relative
        if not path.exists():
            return
        if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(root):
            notes.append('Skipped non-regular/outside-root file: ' + archive_name)
            return
        sources.append((path, archive_name))
    for label, root in [('client', instance), ('server', server)]:
        if root is None:
            continue
        for log in ('logs/latest.log', 'logs/debug.log', 'loader-install.log'):
            add(root, log, label + '/' + log)
        crashes = root / 'crash-reports'
        if crashes.is_dir() and crashes.resolve().is_relative_to(root.resolve()):
            candidates = sorted((p for p in crashes.glob('*.txt') if p.is_file() and not p.is_symlink()), key=lambda p:p.stat().st_mtime, reverse=True)[:3]
            for path in candidates:
                add(root, 'crash-reports/' + path.name, label + '/crash-reports/' + path.name)
        # Hash inventory reveals manual changes without uploading third-party binaries.
        inventory = []
        mod_dir = root / 'mods'
        if mod_dir.is_dir() and mod_dir.resolve().is_relative_to(root.resolve()):
            for path in sorted(mod_dir.glob('*.jar'))[:200]:
                if path.is_symlink() or not path.is_file():continue
                if path.stat().st_size > 512 * 1024 * 1024:
                    notes.append('Skipped oversized JAR hash: ' + path.name);continue
                digest = hashlib.sha256()
                with path.open('rb') as handle:
                    for chunk in iter(lambda:handle.read(1024*1024), b''):digest.update(chunk)
                inventory.append(dict(file=path.name, size=path.stat().st_size, sha256=digest.hexdigest()))
        sources.append((json.dumps(inventory, indent=2).encode(), label + '/mods-sha256.json'))
    if installation:
        for name in REPORTS:add(installation, name, 'installation/' + name)
    if result:
        add(result.parent, result.name, 'FIRST_RUN_RESULTS.md')
    output.parent.mkdir(parents=True, exist_ok=True)
    total = 0; included = []
    with zipfile.ZipFile(output, 'x', zipfile.ZIP_DEFLATED) as archive:
        for source, name in sources:
            size = len(source) if isinstance(source, bytes) else source.stat().st_size
            if size > LIMIT or total + size > TOTAL_LIMIT:
                notes.append('Skipped size budget: ' + name);continue
            if isinstance(source, bytes):
                data = source
            else:
                with source.open('rb') as handle:
                    data = handle.read(LIMIT + 1)
            if len(data) > LIMIT or total + len(data) > TOTAL_LIMIT:
                notes.append('Skipped file that grew beyond size budget: ' + name)
                continue
            total += len(data)
            cleaned = redact(data.decode('utf-8', errors='replace')).encode('utf-8')
            archive.writestr(name, cleaned);included.append(name)
        archive.writestr('COLLECTION.json', json.dumps(dict(files=included, notes=notes,
            runtime_verified=False, explanation='Collection only. Review logs for nicknames/IP/private chat before sharing; automatic redaction is limited. No accounts/worlds/JARs included.'), indent=2))
    return included, notes


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--instance',type=Path,help='Prism Minecraft folder containing logs/ and mods/, NOT launcher data root')
    parser.add_argument('--install-output',type=Path)
    parser.add_argument('--server',type=Path)
    parser.add_argument('--results',type=Path)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    try:
        files,notes=collect(args.instance,args.install_output,args.server,args.output,args.results)
    except (OSError,ValueError) as exc:
        print('STOP: '+str(exc),file=sys.stderr);return 1
    print(f'Collected {len(files)} entries: {args.output}. No game check is inferred. Review before sharing.')
    for note in notes:print(note)
    return 0

if __name__=='__main__':raise SystemExit(main())
