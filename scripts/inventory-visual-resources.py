#!/usr/bin/env python3
"""Inventory existing visual assets, without editing artwork. Standard library only."""
import argparse
import csv
import hashlib
import io
import json
import struct
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--check', action='store_true', help='Fail if the committed inventory is stale')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
assets = root / 'src/main/resources/assets/reimagined'
rows = []
for path in sorted(assets.rglob('*')):
    if not path.is_file():
        continue
    relative = path.relative_to(assets)
    if relative.parts[0] not in {'textures', 'models', 'blockstates'}:
        continue
    data = path.read_bytes()
    width = height = ''
    links = ''
    if path.suffix == '.png':
        if data[:8] != b'\x89PNG\r\n\x1a\n' or data[12:16] != b'IHDR':
            raise SystemExit(f'Invalid PNG header: {relative}')
        width, height = struct.unpack('>II', data[16:24])
    elif path.suffix == '.json':
        obj = json.loads(data)
        refs = [obj['parent']] if 'parent' in obj else []
        refs += [v for v in obj.get('textures', {}).values() if not v.startswith('#')]
        if relative.parts[0] == 'blockstates':
            def models(node):
                if isinstance(node, dict):
                    if 'model' in node:
                        refs.append(node['model'])
                    for child in node.values():
                        models(child)
                elif isinstance(node, list):
                    for child in node:
                        models(child)
            models(obj)
        links = ';'.join(sorted(set(refs)))
    rows.append((relative.parts[0], str(relative), width, height, hashlib.sha256(data).hexdigest(), links))
stream = io.StringIO(newline='')
writer = csv.writer(stream, lineterminator='\n')
writer.writerow(['kind', 'path', 'width', 'height', 'sha256', 'parent_texture_or_model_refs'])
writer.writerows(rows)
result = stream.getvalue()
target = root / 'docs/VISUAL_RESOURCE_INVENTORY.csv'
if args.check:
    if not target.is_file() or target.read_text() != result:
        raise SystemExit('Visual resource inventory is stale; regenerate and review asset changes.')
else:
    target.write_text(result)
counts = {kind: sum(r[0] == kind for r in rows) for kind in ['textures', 'models', 'blockstates']}
print(f'Visual assets: {counts}; no texture/model modified.')
