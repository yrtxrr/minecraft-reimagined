#!/usr/bin/env python3
"""Read-only R8 catalog evidence. No JAR downloads, installs or status promotion."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import ssl
import urllib.parse
import urllib.request

ROOT=Path(__file__).resolve().parents[2]


def normal(text):return re.sub('[^a-z0-9]','',text.lower())


def fetch(route):
    url='https://api.modrinth.com/v2/'+route
    request=urllib.request.Request(url,headers={'User-Agent':'Reimagined-R8-research (github.com/yrtxrr/minecraft-reimagined)'})
    with urllib.request.urlopen(request,timeout=20,context=ssl.create_default_context()) as response:
        body=response.read(8*1024*1024+1)
    if len(body)>8*1024*1024:raise ValueError('Catalog response over8MiB')
    return json.loads(body)


def collect(candidate,get=fetch):
    result={'id':candidate['id'],'status':candidate['status'],'runtime_verified':False,'jar_downloaded':False}
    try:
        reviewed=(candidate.get('project_id') or {}).get('modrinth')
        if reviewed:
            project=get('project/'+reviewed)
            if project['id']!=reviewed:raise ValueError('Reviewed project ID mismatch')
        else:
            query=urllib.parse.urlencode({'query':candidate['name'],'limit':20,'facets':'[["project_type:mod"]]'})
            hits=get('search?'+query)['hits']
            exact={h['project_id'] for h in hits if normal(h['title'])==normal(candidate['name'])}
            if len(exact)!=1:raise ValueError('No unique exact title in first20hits; manual identity review required, no alias guessed')
            project=get('project/'+exact.pop())
            if normal(project['title'])!=normal(candidate['name']):raise ValueError('Project title changed; review required')
        result['project']={k:project.get(k) for k in ('id','slug','title','source_url','issues_url','license','client_side','server_side')}
        query=urllib.parse.urlencode({'loaders':'["neoforge"]','game_versions':'["1.21.1"]'})
        versions=get('project/'+project['id']+'/version?'+query)
        matching=[v for v in versions if '1.21.1' in v.get('game_versions',[]) and 'neoforge' in v.get('loaders',[])]
        result['compatible_catalog_count']=len(matching)
        result['versions']=[{k:v.get(k) for k in ('id','version_number','version_type','date_published','game_versions','loaders','dependencies','files')} for v in sorted(matching,key=lambda v:v['date_published'],reverse=True)[:20]]
        result['limitations']='Catalog only; authorship, exact base-mod ranges, actual JAR metadata/SHA256/runtime not certified. No source/download URL is automatically trusted.'
    except Exception as exc:
        result['error']=str(exc)
    return result


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--r7-lock',type=Path)
    args=p.parse_args()
    data=json.loads((ROOT/'pack/r8/addon-research.json').read_text(encoding='utf-8'))
    if args.output.exists():raise SystemExit('Output already exists; choose a new filename')
    report={'runtime_verified':False,'installer_unchanged':True,'candidates':[]}
    if args.r7_lock:
        raw=args.r7_lock.read_bytes();lock=json.loads(raw)
        if (lock.get('minecraft'),lock.get('neoforge'))!=('1.21.1','21.1.264'):raise SystemExit('R7 lock platform mismatch')
        wanted={'create','mekanism','applied-energistics-2','farmers-delight','ftb-quests'}
        mods={m['key']:m for m in lock['mods']}
        report['r7_lock']={'sha256':hashlib.sha256(raw).hexdigest(),'versions':{t['id']:mods[t['key']]['version'] for t in lock['targets'] if t['id'] in wanted and t.get('key') in mods},'core':'Core is not necessarily recorded in external-mod lock; supply actual installed Core hash separately'}
    for candidate in data['candidates']:
        print('Research:',candidate['name'],flush=True)
        report['candidates'].append(collect(candidate))
    with args.output.open('x',encoding='utf-8') as out:json.dump(report,out,ensure_ascii=False,indent=2)
    print('Catalog evidence only:',args.output)

if __name__=='__main__':main()
