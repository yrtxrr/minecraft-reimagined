#!/usr/bin/env python3
"""Validate the native FTB JSON-subset SNBT graph and server-advancement references offline."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
folder=ROOT/'modpack/overrides/config/ftbquests/quests'
read=lambda p:json.loads(p.read_text(encoding='utf-8'))
chapter=read(folder/'chapters/reimagined_intro.snbt');data=read(folder/'data.snbt')
assert data['version']==13 and data['default_reward_team'] is False
assert data['progression_mode']=='flexible' and data['default_autoclaim_rewards']=='disabled'
ids=set()
def ident(value):
    assert re.fullmatch('[0-9A-F]{16}',value) and value not in ids,value
    ids.add(value)
ident(chapter['id']);quests=chapter['quests'];assert len(quests)==8
lookup={q['id']:q for q in quests};assert len(lookup)==8
for q in quests:
    ident(q['id']);assert all(d in lookup for d in q['dependencies'])
    for task in q['tasks']:
        ident(task['id']);assert task['type'] in ('advancement','checkmark')
        if task['type']=='advancement':
            namespace,path=task['advancement'].split(':')
            tag=read(ROOT/f'src/main/resources/data/{namespace}/advancement/{path}.json')
            assert tag['criteria']['milestone']['trigger']=='minecraft:impossible'
    for reward in q['rewards']:
        ident(reward['id']);assert reward['type']=='xp' and 0<reward['xp']<=100 and reward['team_reward'] is False
visiting=set();done=set()
def visit(key):
    assert key not in visiting,'Dependency cycle'
    if key in done:return
    visiting.add(key)
    for dep in lookup[key]['dependencies']:visit(dep)
    visiting.remove(key);done.add(key)
for key in lookup:visit(key)
langs=[read(folder/'lang'/f'{lang}.snbt') for lang in ('en_us','ru_ru')]
assert langs[0].keys()==langs[1].keys()
for lang in langs:
    assert lang[f'chapter.{chapter["id"]}.title']
    for q in quests:assert lang[f'quest.{q["id"]}.title'] and lang[f'quest.{q["id"]}.quest_desc']
print(f'Validated 8 native quests, {len(ids)} unique IDs, bilingual text, dependency DAG and loaded advancement resource references. FTB runtime/GUI is a separate gate.')
