#!/usr/bin/env python3
"""Offline personal pack assembler: no guessed versions, downloads or bundled secrets/worlds."""
import argparse
import hashlib
import json
import re
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'modpack'


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def validated_mods(registry, lock, profile):
    if registry['schema'] != 1 or lock['schema'] != 1:
        raise ValueError('Unknown registry/lock schema')
    if (lock['minecraft'], lock['neoforge']) != ('1.21.1', '21.1.264'):
        raise ValueError('Wrong Minecraft/NeoForge platform')
    candidates = {mod['id']: mod for mod in registry['mods']}
    if len(candidates) != len(registry['mods']):
        raise ValueError('Duplicate candidate ID')
    resolved = {}
    filenames = {"reimagined-0.1.0-alpha.jar"}
    for mod in lock['mods']:
        ident = mod['id']
        if ident not in candidates or ident in resolved:
            raise ValueError(f'Unknown/duplicate locked mod: {ident}')
        if not mod.get('version') or mod.get('minecraft') != '1.21.1' or mod.get('loader') != 'neoforge':
            raise ValueError(f'Unverified platform/version: {ident}')
        if mod.get('side') not in ('client', 'server', 'both'):
            raise ValueError(f'Missing verified side: {ident}')
        if mod.get('verification') != 'client-and-save-restart' or not mod.get('license') or mod.get('personal_use_allowed') is not True:
            raise ValueError(f'Missing verification/license permission: {ident}')
        if not str(mod.get('source_url', '')).startswith('https://') or not isinstance(mod.get('dependencies'), list):
            raise ValueError(f'Missing official source/dependencies: {ident}')
        name = mod.get('file', '')
        if not re.fullmatch(r'[A-Za-z0-9_.+\-]+\.jar', name):
            raise ValueError(f'Unsafe artifact filename: {ident}')
        if name in filenames:
            raise ValueError(f'Duplicate/reserved artifact filename: {name}')
        filenames.add(name)
        if not re.fullmatch(r'[a-f0-9]{64}', mod.get('sha256', '')):
            raise ValueError(f'Invalid SHA256: {ident}')
        artifact = PACK / 'downloads' / name
        if artifact.is_symlink() or not artifact.is_file() or artifact.resolve().parent != (PACK / 'downloads').resolve():
            raise ValueError(f'Missing/unsafe local official artifact: {ident}')
        if hashlib.sha256(artifact.read_bytes()).hexdigest() != mod['sha256']:
            raise ValueError(f'SHA256 mismatch: {ident}')
        resolved[ident] = (mod, artifact)
    for ident, (mod, _) in resolved.items():
        for dependency in mod['dependencies']:
            if dependency not in resolved:
                raise ValueError(f'Unresolved required dependency {dependency} for {ident}')
    if 'betterend-new-dawn' in resolved and 'nullscape' in resolved:
        raise ValueError('BetterEnd + Nullscape has no accepted combined validation')
    if profile == 'full':
        missing = sorted(ident for ident, mod in candidates.items() if mod['role'] == 'target' and ident not in resolved)
        if missing:
            raise ValueError('Full profile is blocked; missing verified mods: ' + ', '.join(missing))
    elif resolved:
        raise ValueError('Core-only profile must have an empty third-party lock')
    return resolved


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--profile', choices=['core-only', 'full'], required=True)
    parser.add_argument('--side', choices=['client', 'server'], default='client')
    args = parser.parse_args()
    registry, lock = read(PACK / 'mod-registry.json'), read(PACK / 'mods.lock.json')
    try:
        mods = validated_mods(registry, lock, args.profile)
    except (ValueError, KeyError, TypeError) as exc:
        raise SystemExit(str(exc))
    jar = ROOT / 'build/libs/reimagined-0.1.0-alpha.jar'
    if not jar.is_file():
        raise SystemExit('Build the current Core JAR with ./gradlew build first')
    prefix = 'instance' if args.side == 'client' else 'server'
    output = ROOT / f'build/modpack/reimagined-{args.profile}-{args.side}.zip'
    output.parent.mkdir(parents=True, exist_ok=True)
    payload = {f'{prefix}/mods/{jar.name}': jar}
    if args.side == 'client':
        for path in sorted((PACK / 'overrides').rglob('*')):
            if path.is_file() and not path.is_symlink():
                payload[f'{prefix}/{path.relative_to(PACK / "overrides").as_posix()}'] = path
    else:
        payload[f'{prefix}/server.properties'] = PACK / 'server/server.properties'
        for path in sorted((PACK / 'overrides/config/ftbquests').rglob('*')):
            if path.is_file() and not path.is_symlink():
                payload[f'{prefix}/config/ftbquests/{path.relative_to(PACK / "overrides/config/ftbquests").as_posix()}'] = path
        for path in sorted((PACK / 'overrides/kubejs').rglob('*')):
            if path.is_file() and 'client_scripts' not in path.parts and not path.is_symlink():
                payload[f'{prefix}/kubejs/{path.relative_to(PACK / "overrides/kubejs").as_posix()}'] = path
    for mod, path in mods.values():
        if mod['side'] in (args.side, 'both'):
            for dependency in mod['dependencies']:
                if mods[dependency][0]['side'] not in (args.side, 'both'):
                    raise SystemExit(f'Wrong-side dependency {dependency}')
            payload[f'{prefix}/mods/{path.name}'] = path
    payload['README_WINDOWS.md'] = PACK / ('README_WINDOWS.md' if args.side == 'client' else 'server/README_WINDOWS.md')
    payload['jvm-args.txt'] = PACK / ('jvm-args.txt' if args.side == 'client' else 'server/jvm-args.txt')
    payload['mod-registry.json'] = PACK / 'mod-registry.json'
    payload['mods.lock.json'] = PACK / 'mods.lock.json'
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    manifest = dict(profile=args.profile, side=args.side, revision=revision, minecraft='1.21.1', neoforge='21.1.264',
                    warning='Core-only is not the complete target modpack' if args.profile == 'core-only' else 'Personal verified profile',
                    files={name: hashlib.sha256(path.read_bytes()).hexdigest() for name, path in payload.items()})
    with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
        for name, path in payload.items():
            archive.writestr(name, path.read_bytes())
        archive.writestr('manifest.json', json.dumps(manifest, indent=2) + '\n')
    print(f'Assembled {output}: {len(payload)} files, {len(mods)} external mods locked')


if __name__ == '__main__':
    main()
