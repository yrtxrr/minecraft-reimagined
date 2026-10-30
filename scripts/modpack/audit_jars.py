"""Bounded, offline NeoForge dependency presence audit for BOTH generated profiles.

NeoForge remains authoritative for Maven version ranges, mixins and JarJar version
selection. Reports these limitations explicitly; never labels a profile playable.
"""
import io
import json
from pathlib import Path
import tomllib
import zipfile

MAX_METADATA = 2 * 1024 * 1024
MAX_NESTED = 64 * 1024 * 1024


def inspect(path):
    mods, dependencies, notes = [], [], []
    budget = [0, 0]

    def visit(source, label, depth):
        budget[0] += 1
        if depth > 4 or budget[0] > 128:
            raise ValueError('JarJar recursion/entry budget exceeded')
        with zipfile.ZipFile(source) as jar:
            if len(jar.infolist()) > 100000:
                raise ValueError('JAR entry budget exceeded')
            def read(name, limit=MAX_METADATA):
                info = jar.getinfo(name)
                if info.file_size > limit:
                    raise ValueError('Oversized JAR metadata or embedded library: ' + name)
                return jar.read(name)
            names = set(jar.namelist())
            descriptor = next((n for n in ('META-INF/neoforge.mods.toml', 'META-INF/mods.toml') if n in names), None)
            if descriptor:
                data = tomllib.loads(read(descriptor).decode('utf-8-sig'))
                for mod in data.get('mods', []):
                    ident = mod['modId']
                    if not ident or '${' in ident:
                        raise ValueError('Unresolved Java mod ID in ' + label)
                    mods.append(dict(id=ident, version=mod.get('version', ''), artifact=label, embedded=depth > 0))
                    for dep in data.get('dependencies', {}).get(ident, []):
                        if dep.get('type') == 'required' or dep.get('mandatory') is True:
                            dependencies.append(dict(owner=ident, id=dep['modId'],
                                side=dep.get('side', 'BOTH').lower(), range=dep.get('versionRange', '')))
            elif depth == 0 and 'fabric.mod.json' in names:
                raise ValueError('Fabric-only top-level JAR: ' + label)
            elif depth == 0:
                notes.append('No Java descriptor (library/datapack wrapper): ' + label)
            if 'META-INF/jarjar/metadata.json' in names:
                metadata = json.loads(read('META-INF/jarjar/metadata.json'))
                for entry in metadata.get('jars', []):
                    nested = entry['path']
                    # Never extract paths; bounded in-memory inspection only.
                    size = jar.getinfo(nested).file_size
                    budget[1] += size
                    if budget[1] > MAX_NESTED:
                        raise ValueError('Total embedded library byte budget exceeded')
                    visit(io.BytesIO(read(nested, MAX_NESTED)), label + '!' + nested, depth + 1)
    visit(path, Path(path).name, 0)
    return mods, dependencies, notes


def audit(paths, side):
    if side not in ('client', 'server'):
        raise ValueError('Unknown profile side')
    declared, required, notes, errors = {}, [], [], []
    top_level = set()
    for path in paths:
        mods, dependencies, warnings = inspect(path)
        notes.extend(warnings)
        required.extend(dependencies)
        for mod in mods:
            if not mod['embedded']:
                if mod['id'] in top_level:
                    errors.append('Duplicate top-level mod ID: ' + mod['id'])
                top_level.add(mod['id'])
            old = declared.get(mod['id'])
            if old:
                if old['embedded'] or mod['embedded']:
                    notes.append('NeoForge must resolve shared JarJar versions: ' + mod['id'])
            else:
                declared[mod['id']] = mod
    for dep in required:
        if dep['side'] not in ('both', 'client', 'server'):
            errors.append('Unknown dependency side: ' + str(dep))
        elif dep['side'] in ('both', side) and dep['id'] not in {'minecraft', 'neoforge'} | declared.keys():
            errors.append(dep['owner'] + ' requires missing ' + dep['id'] + ' on ' + side)
    return dict(side=side, mods=list(declared.values()), required=required, errors=errors,
                notes=notes, version_ranges_checked=False, runtime_verified=False,
                limitation='NeoForge must verify version ranges, embedded library selection, mixins and actual world loading.')
