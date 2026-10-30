#!/usr/bin/env python3
"""Export the recorded world baseline for Prism (SHA512) and a matching server bootstrap, offline."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile
from install import ROOT, MC, NF, Blocked, pinned_records, java_mod_ids, safe_filename


def export(output,core,include_optional=False):
    records,targets,_=pinned_records(ROOT/'pack/mods.json',include_optional)
    if {'nullscape','betterend-new-dawn'}.issubset(targets):raise Blocked('Conflicting End providers')
    if 'reimagined' not in java_mod_ids(core):raise Blocked('Wrong Core JAR')
    with zipfile.ZipFile(core) as jar:
        if any('/gametest/' in p or '/profile/' in p for p in jar.namelist()):raise Blocked('Dev fixtures in player JAR')
    for record in records.values():
        file=record['file']
        safe_filename(file['filename'])
        if not 0<file['size']<=512*1024*1024:raise Blocked('Unsafe pinned artifact')
    bundled=ROOT/'installer-manifest.json'
    if bundled.is_file():revision=json.loads(bundled.read_text(encoding='utf-8'))['revision']
    else:
        try:revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True,stderr=subprocess.DEVNULL).strip()
        except (OSError,subprocess.CalledProcessError):revision='unversioned-source'
    status=(f'Reimagined world baseline: {len(records)} external file pins, code {revision}.\n'
            'NOT the complete40-target pack; technology/FTB/optimization binaries are absent.\n'
            'Freebuff server evidence25/99tests does not certify this integrated client/server.\n'
            'Client/new Default world/team/save/restart/performance acceptance remains pending.\n')
    index=dict(formatVersion=1,game='minecraft',versionId='world-baseline-'+revision[:7],
               name=f'Reimagined world-{len(records)} candidate',summary='Recorded world baseline; tech and FTB binaries are NOT included',
               dependencies={'minecraft':MC,'neoforge':NF},files=[])
    for record in records.values():
        file=record['file'];index['files'].append(dict(path='mods/'+file['filename'],fileSize=file['size'],
            hashes={'sha512':file['hashes']['sha512']},downloads=[file['url']],
            env={s:'required' if s in record['sides'] else 'unsupported' for s in ('client','server')}))
    output.mkdir(parents=True,exist_ok=True)
    client=output/f'Reimagined-world-{len(records)}-candidate.mrpack'
    server=output/f'Reimagined-world-{len(records)}-server.zip'
    listing='\n'.join(f'- {r["name"]}: {r["version"]}' for r in records.values())+'\n'
    with zipfile.ZipFile(client,'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('modrinth.index.json',json.dumps(index,ensure_ascii=False,indent=2)+'\n')
        z.write(core,'overrides/mods/reimagined-0.1.0-alpha.jar')
        for p in sorted((ROOT/'modpack/overrides').rglob('*')):
            if p.is_file() and not p.is_symlink():z.write(p,'overrides/'+p.relative_to(ROOT/'modpack/overrides').as_posix())
        z.writestr('overrides/PROFILE_STATUS.txt',status)
        z.writestr('overrides/PINNED_MODS.md',listing)
        z.write(ROOT/'pack/mods.json','overrides/REIMAGINED_PINS.json')
    with zipfile.ZipFile(server,'w',zipfile.ZIP_DEFLATED) as z:
        z.write(core,'server/mods/reimagined-0.1.0-alpha.jar')
        for p in sorted((ROOT/'modpack/server').rglob('*')):
            if p.is_file():z.write(p,'server/'+p.relative_to(ROOT/'modpack/server').as_posix())
        for folder in ['config/ftbquests','kubejs']:
            for p in sorted((ROOT/'modpack/overrides'/folder).rglob('*')):
                if p.is_file() and 'client_scripts' not in p.parts:z.write(p,'server/'+folder+'/'+p.relative_to(ROOT/'modpack/overrides'/folder).as_posix())
        for name in ['fetch-modpack.py','validate-modpack.py']:
            z.write(ROOT/'scripts'/name,'scripts/'+name)
        z.write(ROOT/'scripts/modpack/install.py','scripts/modpack/install.py')
        z.write(ROOT/'pack/mods.json','pack/mods.json')
        z.writestr('PROFILE_STATUS.txt',status)
        z.writestr('PINNED_MODS.md',listing)
        z.writestr('START_HERE_RU.md',f'''# Тот же набор, что в клиентском world-{len(records)} mrpack

Это bootstrap сервера, внешние JAR скачиваются официально на вашем ПК. Не полноценный40-модовый target: FTB/технологии/оптимизаторы пока отсутствуют.

Python3.11+: в распакованной папке выполните:

    py -3 scripts/fetch-modpack.py --destination server/mods{' --optional' if include_optional else ''}
    py -3 scripts/validate-modpack.py --mods server/mods

Поставьте официальный NeoForge{NF}/Minecraft{MC} server в каталог server (Java21), самостоятельно прочитайте/примите EULA и используйте штатный run.bat. Настройки памяти в server/jvm-args.txt, maxplayers2. Клиент и сервер должны использовать один и тот же набор (ColdSweat {'включён' if include_optional else 'не включён'}).

Не открывайте единственное старое сохранение: остановите игру/сервер, скопируйте весь мир. Version2 миграция дополнительно сохраняет legacy settlement .dat, не заменяя полный backup. Для отката — полная резервная копия, не запуск старого JAR на новом v2мире.

Общее состояние цивилизаций/серверная торговля/личные навыки сохранены в Core. Native первая глава начнёт загружаться только после установки реального FTB Quests+зависимостей; копия конфига сама не добавляет мод. Полный Windows installer позволяет разрешить остальные40целей, не меняя эти пины.
''')
    manifest=dict(revision=revision,purpose='Prism SHA512 recorded baseline; not integrated runtime certification',
                  artifacts={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (client,server)})
    (output/'pinned-profiles-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    return client,server


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,default=ROOT/'build/modpack')
    p.add_argument('--include-optional',action='store_true');p.add_argument('--core-jar',type=Path)
    args=p.parse_args();core=args.core_jar or (ROOT/'payload/reimagined-0.1.0-alpha.jar' if (ROOT/'payload/reimagined-0.1.0-alpha.jar').exists() else ROOT/'build/libs/reimagined-0.1.0-alpha.jar')
    for artifact in export(args.output,core,args.include_optional):print(artifact)
