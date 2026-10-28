#!/usr/bin/env python3
"""Create an opt-in native QA datapack in an explicitly disposable dev world.

Run only with that world stopped or follow with /reload and /datapack enable.
Functions supply ordinary AI-enabled entities; they never mark a test passed.
"""
import argparse
import json
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('world', type=Path)
args = parser.parse_args()
world = args.world.resolve()
if not (world / '.alpha-approved-disposable').is_file():
    parser.error('World must have an explicit .alpha-approved-disposable marker')
pack = world / 'datapacks' / 'fauna_qa'
pack.mkdir(parents=True, exist_ok=True)
(pack / 'pack.mcmeta').write_text(json.dumps({'pack': {'pack_format': 48, 'description': 'Disposable fauna QA fixtures; not player content'}}, indent=2)+'\n')
functions = pack / 'data' / 'reimagined_qa' / 'function'
functions.mkdir(parents=True, exist_ok=True)
locations = [(13, 180.6, 13), (24, 181, 7), (23, 181, 24), (7, 181, 24)]
for name, species in [('baseline', ['minecraft:cow']*4), ('candidate', ['reimagined:otter', 'reimagined:badger', 'reimagined:marmot', 'reimagined:deer'])]:
    commands = ['# Assumes a prepared, loaded, enclosed grass arena at0..30/y180 and water8..18/y180.',
                'kill @e[tag=fauna_scale]', 'kill @e[tag=fauna_qa]',
                'kill @e[type=minecraft:item,distance=..40]', 'kill @e[type=minecraft:experience_orb,distance=..40]',
                'tp @s 15 181 22 180 15', 'weather rain']
    for mob, (x, y, z) in zip(species, locations):
        for index in range(8):
            commands.append(f'summon {mob} {x + (index % 3)*0.8} {y} {z + (index // 3)*0.8} {{Tags:["fauna_scale"],PersistenceRequired:1b}}')
    commands += ['execute store result score total fauna_count run execute if entity @e[tag=fauna_scale]',
                 'scoreboard players get total fauna_count']
    (functions / f'fauna_{name}.mcfunction').write_text('\n'.join(commands)+'\n')
print(f'Created {pack}; use /datapack enable "file/fauna_qa", /function reimagined_qa:fauna_baseline or fauna_candidate. Expect count32.')
