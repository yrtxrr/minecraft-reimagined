#!/usr/bin/env python3
"""Check local resource links without pretending to run Mojang's registry codecs."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
files = sorted(resources.rglob('*.json')) + [resources / 'pack.mcmeta']
documents = {p: json.loads(p.read_text(encoding='utf-8')) for p in files}
errors = []

def require(path, context):
    if not path.is_file():
        errors.append(f'{context}: missing {path.relative_to(resources)}')

for path, data in documents.items():
    if 'models' in path.parts:
        parent = data.get('parent', '')
        if parent.startswith('reimagined:'):
            require(resources / 'assets/reimagined/models' / (parent.split(':')[1] + '.json'), path.name)
        for texture in data.get('textures', {}).values():
            if texture.startswith('reimagined:'):
                require(resources / 'assets/reimagined/textures' / (texture.split(':')[1] + '.png'), path.name)
    if 'blockstates' in path.parts:
        for variants in data.get('variants', {}).values():
            for variant in variants if isinstance(variants, list) else [variants]:
                model = variant.get('model', '')
                if model.startswith('reimagined:'):
                    require(resources / 'assets/reimagined/models' / (model.split(':')[1] + '.json'), path.name)
    if 'recipe' in path.parts:
        result = data.get('result', {})
        if isinstance(result, dict) and result.get('id', '').startswith('reimagined:'):
            require(resources / 'assets/reimagined/models/item' / (result['id'].split(':')[1] + '.json'), path.name)

# Translation completeness and format placeholders must match across shipped languages.
locales = [json.loads((resources / f'assets/reimagined/lang/{locale}.json').read_text(encoding='utf-8')) for locale in ('en_us', 'ru_ru')]
for key in set(locales[0]) | set(locales[1]):
    if not all(key in locale for locale in locales):
        errors.append(f'Translation missing: {key}')
    elif re.findall(r'%(?:\d+\$)?[sdif]', locales[0][key]) != re.findall(r'%(?:\d+\$)?[sdif]', locales[1][key]):
        errors.append(f'Translation format mismatch: {key}')
for source in (root / 'src/main/java').rglob('*.java'):
    for key in re.findall(r'Component.translatable\("([^"\n]+)"', source.read_text(encoding='utf-8')):
        if key.endswith('.') or not any(key.startswith(p) for p in ('reimagined.', 'message.reimagined.', 'hud.reimagined.', 'tooltip.reimagined.', 'journal.reimagined.')):
            continue
        if key not in locales[0]: errors.append(f'{source.name}: missing translation {key}')

# Custom density function references form an acyclic graph.
density = resources / 'data/reimagined/worldgen/density_function'
graph = {}
def refs(value):
    if isinstance(value, dict):
        for v in value.values(): yield from refs(v)
    elif isinstance(value, list):
        for v in value: yield from refs(v)
    elif isinstance(value, str) and value.startswith('reimagined:'):
        yield value.split(':', 1)[1]
for path in density.glob('*.json'):
    graph[path.stem] = set(refs(documents[path]))
visiting, visited = set(), set()
def visit(node):
    if node in visiting: errors.append(f'Density cycle: {node}'); return
    if node in visited: return
    if node not in graph: errors.append(f'Missing density function: {node}'); return
    visiting.add(node)
    for neighbor in graph[node]: visit(neighbor)
    visiting.remove(node); visited.add(node)
for node in graph: visit(node)
settings = documents[resources / 'data/reimagined/worldgen/noise_settings/continents.json']
for node in refs(settings['noise_router']):
    if node not in graph: errors.append(f'Noise router missing density function: {node}')
assert settings['noise']['min_y'] == -64 and settings['noise']['height'] == 384
preset = documents[resources / 'data/reimagined/worldgen/world_preset/continents.json']
assert set(preset['dimensions']) == {'minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end'}
tag = documents[resources / 'data/minecraft/tags/worldgen/world_preset/normal.json']
assert tag.get('replace', False) is False and 'reimagined:continents' in tag['values']
if errors:
    raise SystemExit('\n'.join(errors))
print(f'Validated {len(files)} JSON resources, local model/texture links, {len(locales[0])} bilingual keys, and acyclic density references.')
print('Minecraft registry codecs, API compatibility and gameplay are NOT validated by this script.')
