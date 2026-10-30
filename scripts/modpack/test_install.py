import copy
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

spec=importlib.util.spec_from_file_location('installer',Path(__file__).with_name('install.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


def jar(modid='example'):
    b=io.BytesIO()
    with zipfile.ZipFile(b,'w') as z:z.writestr(zipfile.ZipInfo('META-INF/neoforge.mods.toml',date_time=(2026,1,1,0,0,0)),f'[[mods]]\nmodId="{modid}"\n')
    return b.getvalue()


def project(ident='root',title='Root'):
    return dict(id=ident,title=title,slug=ident,client_side='required',server_side='required',license={'id':'MIT'})


def version(ident='v1',pid='root',deps=None,loaders=None,date='2026-01-01'):
    data=jar(pid)
    return dict(id=ident,project_id=pid,version_number=ident,version_type='release',date_published=date,
                game_versions=['1.21.1'],loaders=loaders or ['neoforge'],dependencies=deps or [],
                files=[dict(url='https://cdn.modrinth.com/data/'+pid+'/'+ident+'.jar',filename=pid+'-'+ident+'.jar',
                            hashes={'sha512':hashlib.sha512(data).hexdigest()},size=len(data),primary=True)])


class Fake:
    def __init__(self):
        self.api_key=None;self.projects={'root':project()};self.versions={'v1':version()};self.payload=jar('root');self.opens=[]
    def json(self,url):
        if '/search?' in url:return {'hits':[]}
        if '/v2/version/' in url:return copy.deepcopy(self.versions[url.rsplit('/',1)[1]])
        suffix=url.split('/v2/project/')[1]
        if '/version?' in suffix:
            pid=suffix.split('/')[0];return [copy.deepcopy(v) for v in self.versions.values() if v['project_id']==pid]
        return copy.deepcopy(self.projects[suffix])
    def open(self,url,hosts):
        self.opens.append(url);return io.BytesIO(self.payload)
    download=m.Transport.download


class ResolverTests(unittest.TestCase):
    def setUp(self):
        self.t=Fake();self.r=m.Resolver(self.t,{'dependency_sides':{}})
    def test_required_dependency_recurses_and_deduplicates(self):
        self.t.projects['lib']=project('lib','Library');self.t.versions['lib1']=version('lib1','lib')
        self.t.versions['v1']['dependencies']=[dict(dependency_type='required',project_id='lib',version_id='lib1')]*2
        self.r.mr('root');self.assertEqual(2,len(self.r.selected));self.assertEqual([],self.r.conflicts())
    def test_wrong_loader_is_not_substituted(self):
        self.t.versions['v1']['loaders']=['fabric','forge']
        with self.assertRaises(m.Blocked):self.r.mr('root')
    def test_exact_pin_conflict_is_not_silently_overwritten(self):
        self.r.mr('root','v1')
        with self.assertRaisesRegex(m.Blocked,'Conflicting'):self.r.mr('root','v2')
    def test_project_identity_mismatch_blocks_impostor(self):
        with self.assertRaisesRegex(m.Blocked,'identity mismatch'):
            self.r.identify_mr({'name':'Different'},dict(modrinth_slug_hint='root',accepted_titles=['Different']))
    def test_unknown_side_requires_review(self):
        self.t.projects['root']['server_side']='unknown'
        with self.assertRaisesRegex(m.Blocked,'Unknown side'):self.r.mr('root')
    def test_explicit_side_override(self):
        self.t.projects['root']['server_side']='unknown'
        self.r.mr('root',override='client');self.assertEqual(['client'],self.r.selected['modrinth:root']['sides'])
    def test_beta_not_selected_without_explicit_flag(self):
        self.t.versions['v1']['version_type']='beta'
        with self.assertRaises(m.Blocked):self.r.mr('root')
    def test_pinned_wrong_project_rejected(self):
        self.t.versions['v2']=version('v2','other')
        with self.assertRaisesRegex(m.Blocked,'wrong project'):self.r.mr('root','v2')
    def test_required_cycle_is_finite(self):
        self.t.versions['v1']['dependencies']=[dict(dependency_type='required',project_id='root',version_id='v1')]
        self.r.mr('root');self.assertEqual(1,len(self.r.selected))
    def test_incompatible_dependency_blocks_profile(self):
        self.t.versions['v1']['dependencies']=[dict(dependency_type='incompatible',project_id='root',version_id=None)]
        self.r.mr('root');self.assertTrue(self.r.conflicts())
    def test_optional_dependency_not_automatically_installed(self):
        self.t.versions['v1']['dependencies']=[dict(dependency_type='optional',project_id='missing',version_id=None)]
        self.r.mr('root');self.assertEqual(1,len(self.r.selected))
    def test_side_specific_required_library_mismatch(self):
        self.t.projects['lib']=project('lib','Library');self.t.projects['lib']['server_side']='unsupported'
        self.t.versions['lib1']=version('lib1','lib');self.t.versions['v1']['dependencies']=[dict(dependency_type='required',project_id='lib',version_id='lib1')]
        self.r.mr('root');self.assertTrue(self.r.conflicts())
    def test_failed_target_rolls_back_dependencies(self):
        self.t.versions['v1']['dependencies']=[dict(dependency_type='required',project_id=None,version_id=None)]
        with self.assertRaises(m.Blocked):self.r.target({'name':'Root'},dict(modrinth_slug_hint='root',accepted_titles=['Root']))
        self.assertEqual({},self.r.selected)
    def test_https_and_official_hosts_only(self):
        for u in ['http://cdn.modrinth.com/x.jar','https://evil.invalid/x.jar','https://cdn.modrinth.com@evil.invalid/x.jar']:
            with self.assertRaises(m.Blocked):m.safe_url(u,m.FILE_HOSTS)
    def test_no_api_key_leaked_or_guessed(self):
        with self.assertRaisesRegex(m.Blocked,'local CURSEFORGE_API_KEY'):
            m.Transport().open('https://api.curseforge.com/v1/mods/1',m.API_HOSTS)


class DownloadTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.path=Path(self.temp.name);self.t=Fake();self.file=version()['files'][0]
        self.t.payload=jar('root')
    def test_official_checksum_and_size_then_cache_reuse(self):
        p=self.t.download(self.file,self.path);self.assertTrue(p.is_file());self.t.download(self.file,self.path);self.assertEqual(1,len(self.t.opens))
    def test_tampered_body_not_installed(self):
        self.t.payload=b'bad'
        with self.assertRaises(m.Blocked):self.t.download(self.file,self.path)
        self.assertFalse(list(self.path.iterdir()))
    def test_size_budget_stops_oversized_response(self):
        self.file['size']=1
        with self.assertRaisesRegex(m.Blocked,'exceeded'):self.t.download(self.file,self.path)
    def test_unsafe_filename(self):
        for name in ['../outside.jar','bad.exe','CON.jar']:
            self.file['filename']=name
            with self.assertRaises(m.Blocked):self.t.download(self.file,self.path)
    def test_md5_only_not_accepted(self):
        self.file['hashes']={'md5':'a'*32}
        with self.assertRaises(m.Blocked):self.t.download(self.file,self.path)
    def test_cached_corruption_does_not_silently_pass(self):
        p=self.t.download(self.file,self.path);p.write_bytes(b'tampered')
        with self.assertRaises(m.Blocked):self.t.download(self.file,self.path)
    def test_fabric_only_jar_rejected(self):
        p=self.path/'fabric.jar'
        with zipfile.ZipFile(p,'w') as z:z.writestr('fabric.mod.json','{}')
        with self.assertRaises(m.Blocked):m.java_mod_ids(p)


class ProfilesTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.path=Path(self.temp.name)
        self.t=Fake();self.r=m.Resolver(self.t,{});self.r.mr('root')
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text())
        targets=[dict(id=t['id'],name=t['name'],selected=t['id']!='nullscape',status='unresolved',reason='fixture missing') for t in registry['mods']]
        targets[0].update(status='resolved',key='modrinth:root')
        self.plan=dict(schema=1,minecraft=m.MC,neoforge=m.NF,targets=targets,mods=list(self.r.selected.values()),constraints=[],conflicts=[],runtime_verified=False)
        self.core=self.path/'core.jar';self.core.write_bytes(jar('reimagined'))
    def test_incomplete_profile_requires_explicit_opt_in(self):
        with self.assertRaisesRegex(m.Blocked,'Unresolved'):m.validate_plan(self.plan)
    def test_no_core_only_output(self):
        self.plan['mods']=[]
        with self.assertRaisesRegex(m.Blocked,'another Core-only'):m.validate_plan(self.plan,True)
    def test_all_40_candidates_required_in_plan(self):
        self.plan['targets'].pop()
        with self.assertRaisesRegex(m.Blocked,'omits'):m.validate_plan(self.plan,True)
    def test_client_server_separation_and_mrpack_pins(self):
        self.plan['mods'][0]['sides']=['client'];out=self.path/'out'
        m.install(self.plan,out,self.t,self.core,True)
        self.assertTrue((out/'client/mods/root-v1.jar').exists());self.assertFalse((out/'server/mods/root-v1.jar').exists())
        self.assertFalse((out/'server/options.txt').exists());self.assertFalse((out/'server/eula.txt').exists())
        with zipfile.ZipFile(out/'Reimagined-candidate.mrpack') as z:
            index=json.loads(z.read('modrinth.index.json'));self.assertEqual(m.NF,index['dependencies']['neoforge']);self.assertEqual('unsupported',index['files'][0]['env']['server'])
        self.assertFalse(json.loads((out/'installed.lock.json').read_text())['runtime_verified'])
    def test_existing_profile_never_overwritten(self):
        out=self.path/'out';(out/'client').mkdir(parents=True)
        with self.assertRaisesRegex(m.Blocked,'Fresh output'):m.install(self.plan,out,self.t,self.core,True)
    def test_duplicate_java_id_blocked_before_profile_creation(self):
        self.t.payload=jar('reimagined');f=self.plan['mods'][0]['file'];f['hashes']['sha512']=hashlib.sha512(self.t.payload).hexdigest();f['size']=len(self.t.payload)
        with self.assertRaisesRegex(m.Blocked,'Duplicate Java'):m.install(self.plan,self.path/'out',self.t,self.core,True)
        self.assertFalse((self.path/'out/client').exists())
    def test_both_end_providers_are_rejected(self):
        for t in self.plan['targets']:
            if t['id'] in ('betterend-new-dawn','nullscape'):t.update(selected=True,status='resolved',key='modrinth:root')
        with self.assertRaisesRegex(m.Blocked,'Two End'):m.validate_plan(self.plan,True)
    def test_provider_checksum_pin_frozen_not_latest(self):
        m.install(self.plan,self.path/'out',self.t,self.core,True)
        installed=json.loads((self.path/'out/installed.lock.json').read_text());self.assertEqual('v1',installed['mods'][0]['version_id'])
        self.assertEqual(hashlib.sha256(self.t.payload).hexdigest(),installed['mods'][0]['sha256'])



class CurseForgeTests(unittest.TestCase):
    def setUp(self):
        self.file=dict(id=100,modId=10,displayName='Example1',releaseType=1,fileDate='2026-01-01',gameVersions=['1.21.1','NeoForge'],downloadUrl='https://edge.forgecdn.net/files/example.jar',fileName='example.jar',fileLength=1,hashes=[dict(algo=1,value='a'*40)],dependencies=[])
        outer=self
        class CF:
            api_key='fixture-only'
            def json(self,url):
                if '/v2/' in url:raise m.Blocked('Unavailable on Modrinth')
                if '/files?' in url:return {'data':[copy.deepcopy(outer.file)]}
                if '/files/' in url:return {'data':copy.deepcopy(outer.file)}
                return {'data':dict(id=10,name='Root',allowModDistribution=getattr(outer,'allow',None),links={'websiteUrl':'https://www.curseforge.com/minecraft/mc-mods/example'})}
        self.r=m.Resolver(CF(),{})
    def test_official_curseforge_file_and_hash_pin(self):
        key=self.r.cf(10,override='both');record=self.r.selected[key]
        self.assertEqual('100',record['version_id']);self.assertEqual('a'*40,record['file']['hashes']['sha1'])
    def test_null_download_url_never_synthesized(self):
        self.file['downloadUrl']=None
        with self.assertRaisesRegex(m.Blocked,'do not synthesize'):self.r.cf(10,override='both')
    def test_author_api_distribution_restriction_respected(self):
        self.allow=False
        with self.assertRaisesRegex(m.Blocked,'Author disallows'):
            self.r.cf(10,override='both')
    def test_curseforge_side_cannot_be_guessed(self):
        with self.assertRaisesRegex(m.Blocked,'Unknown side'):self.r.cf(10)
    def test_curseforge_wrong_loader_refused(self):
        self.file['gameVersions']=['1.21.1','Forge']
        with self.assertRaises(m.Blocked):self.r.cf(10,100,override='both')
    def test_cross_host_api_key_redirect_blocked(self):
        request=m.urllib.request.Request('https://api.curseforge.com/v1/mods/10',headers={'x-api-key':'fixture-only'})
        with self.assertRaisesRegex(m.Blocked,'cross-host'):
            m.Redirects(m.API_HOSTS).redirect_request(request,None,302,'',{},'https://api.modrinth.com/v2/project/test')
    def test_curseforge_fallback_keeps_exact_title_check(self):
        binding=dict(modrinth_slug_hint='missing',accepted_titles=['Different'],curseforge_project_id=10,side_override='both')
        with self.assertRaisesRegex(m.Blocked,'title mismatch'):self.r.target({'name':'Different'},binding)

class FullCatalogTests(unittest.TestCase):
    def test_every_candidate_checked_and_end_alternative_not_installed_together(self):
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text())
        fake=Fake();fake.projects={};fake.versions={};bindings={'targets':{}}
        for target in registry['mods']:
            ident=target['id'];fake.projects[ident]=project(ident,target['name']);fake.versions[ident]=version(ident,ident)
            bindings['targets'][ident]=dict(modrinth_slug_hint=ident,accepted_titles=[target['name']])
        for end in ['betterend','nullscape']:
            plan=m.resolve(registry,bindings,fake,end)
            self.assertEqual(40,len(plan['targets']));self.assertEqual(39,len(plan['mods']))
            self.assertEqual(1,sum(t['status']=='available-alternative-not-selected' for t in plan['targets']))
            self.assertFalse(plan['runtime_verified']);m.validate_plan(plan)
    def test_blocked_catalog_retains_all_candidates_and_reasons(self):
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text())
        bindings=json.loads((m.ROOT/'modpack/source-bindings.json').read_text())
        class Denied:
            api_key=None
            def json(self,url):raise m.Blocked('CONNECT403')
        plan=m.resolve(registry,bindings,Denied());self.assertEqual(40,len(plan['targets']));self.assertEqual([],plan['mods'])
        self.assertTrue(all('CONNECT403' in t['reason'] for t in plan['targets']))


class PinnedManifestTests(unittest.TestCase):
    def test_verified_manifest_dependency_closure_and_hashes(self):
        records,targets,all_targets=m.pinned_records(m.ROOT/'pack/mods.json',True)
        self.assertEqual(25,len(records))
        self.assertTrue(all(len(r['file']['hashes']['sha512'])==128 for r in records.values()))
        self.assertTrue(all(d in records for r in records.values() for d in r['dependencies']))
    def test_pinned_only_plan_never_requests_catalog(self):
        class Offline:
            def json(self,url):raise AssertionError('Pinned-only must not request API')
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text())
        bindings=json.loads((m.ROOT/'modpack/source-bindings.json').read_text())
        plan=m.resolve(registry,bindings,Offline(),pinned=m.ROOT/'pack/mods.json',pinned_only=True,include_optional=True)
        self.assertEqual(40,len(plan['targets']));self.assertEqual(25,len(m.validate_plan(plan,True)[0]))
    def test_optional_cold_sweat_is_explicit_and_end_choice_preserved(self):
        class Offline:
            def json(self,url):raise AssertionError()
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text());bindings=json.loads((m.ROOT/'modpack/source-bindings.json').read_text())
        plan=m.resolve(registry,bindings,Offline(),end='betterend',pinned=m.ROOT/'pack/mods.json',pinned_only=True)
        self.assertTrue(any(t['id']=='cold-sweat' and t['status']=='optional-not-selected' for t in plan['targets']))
        self.assertFalse(any(r['name']=='Nullscape' for r in plan['mods']))
        m.validate_plan(plan,True)

if __name__=='__main__':unittest.main()

