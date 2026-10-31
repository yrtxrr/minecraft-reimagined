import hashlib
import copy
import json
import io
from pathlib import Path
import tempfile
import unittest
import zipfile
from test_install import m,Fake,project,version


def jar(rng):
    out=io.BytesIO()
    with zipfile.ZipFile(out,'w') as z:z.writestr('META-INF/neoforge.mods.toml','[[mods]]\nmodId="jei"\nversion="1"\n[[dependencies.jei]]\nmodId="minecraft"\ntype="required"\nversionRange="'+rng+'"\n')
    return out.getvalue()


class SelectionTests(unittest.TestCase):
    def test_chooses_older_official_file_by_real_descriptor(self):
        with tempfile.TemporaryDirectory() as tmp:
            bodies={};fake=Fake();fake.artifact_cache=Path(tmp)
            fake.versions={}
            for name,rng,date in [('bad','[1.21,1.21.1)','2026-10-31'),('good','[1.21.1,1.22)','2026-10-01')]:
                v=version(name,date=date);body=jar(rng);f=v['files'][0];f['hashes']={'sha512':hashlib.sha512(body).hexdigest()};f['size']=len(body)
                bodies[f['url']]=body;fake.versions[name]=v
            fake.open=lambda url,hosts:io.BytesIO(bodies[url])
            r=m.Resolver(fake,{})
            key=r.mr('root',verify=True,mod_id='jei')
            self.assertEqual('good',r.selected[key]['version_id'])
            self.assertEqual(2,len(list(Path(tmp).glob('*.jar'))))
    def test_network_failure_does_not_retry_different_versions(self):
        with tempfile.TemporaryDirectory() as tmp:
            fake=Fake();fake.artifact_cache=Path(tmp);calls=[]
            def denied(url,hosts):calls.append(url);raise m.Blocked('HTTP403')
            fake.open=denied
            with self.assertRaisesRegex(m.Blocked,'HTTP403'):m.Resolver(fake,{}).mr('root',verify=True)
            self.assertEqual(1,len(calls))
    def test_base_plan_repairs_only_two_roots(self):
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text())
        fake=Fake();fake.projects={};fake.versions={};bindings={'targets':{}}
        for t in registry['mods']:
            ident=t['id'];fake.projects[ident]=project(ident,t['name']);fake.versions[ident]=version(ident,ident)
            bindings['targets'][ident]=dict(modrinth_slug_hint=ident,accepted_titles=[t['name']])
        old=m.resolve(registry,bindings,fake)
        for ident in ('jei','more-culling'):bindings['targets'][ident]['verify_game_jar']=True
        for v in list(fake.versions.values()):
            new=copy.deepcopy(v);new['id']+='new';new['date_published']='2027-01-01';fake.versions[new['id']]=new
        new=m.resolve(registry,bindings,fake,base_plan=old)
        before={r['key']:r for r in old['mods']};after={r['key']:r for r in new['mods']}
        for key,record in before.items():
            if key not in ('modrinth:jei','modrinth:more-culling'):self.assertEqual(record,after[key])
        self.assertEqual('jeinew',after['modrinth:jei']['version_id'])
    def test_pinned_transitive_wrong_game_range_cannot_bypass_check(self):
        with tempfile.TemporaryDirectory() as tmp:
            fake=Fake();fake.artifact_cache=Path(tmp)
            fake.projects['library']=project('library','Library')
            root=version();lib=version('library1','library')
            root['dependencies']=[dict(dependency_type='required',project_id='library',version_id='library1')]
            bodies={}
            for v,rng in [(root,'[1.21.1]'),(lib,'[1.21,1.21.1)')]:
                body=jar(rng);f=v['files'][0];f.update(hashes={'sha512':hashlib.sha512(body).hexdigest()},size=len(body));bodies[f['url']]=body
            fake.versions={'v1':root,'library1':lib};fake.open=lambda url,hosts:io.BytesIO(bodies[url])
            with self.assertRaisesRegex(m.Blocked,'excludes'):m.Resolver(fake,{}).mr('root',verify=True)
    def test_reuses_original_cache_without_network(self):
        with tempfile.TemporaryDirectory() as tmp:
            old=Path(tmp)/'old';new=Path(tmp)/'new';old.mkdir();new.mkdir()
            fake=Fake();f=version()['files'][0];p=fake.download(f,old)
            fake.reuse_caches=[old];fake.opens=[]
            reused=fake.download(f,new)
            self.assertEqual(p.read_bytes(),reused.read_bytes());self.assertEqual([],fake.opens)

if __name__=='__main__':unittest.main()
