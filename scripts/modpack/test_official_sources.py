import copy
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import official_ftb
from test_install import m, jar, Fake, version

spec=importlib.util.spec_from_file_location('prepare_server',Path(__file__).with_name('prepare-server.py'))
server=importlib.util.module_from_spec(spec);spec.loader.exec_module(server)


class OfficialSourcesTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
    def game(self, ident='ftbquests', ver='2101.1.37', mc='[1.21.1,)', nf='[21.1.0,)', classes=True):
        p=self.root/'ftb.jar'
        with zipfile.ZipFile(p,'w') as z:
            z.writestr('META-INF/neoforge.mods.toml',f'[[mods]]\nmodId="{ident}"\nversion="{ver}"\n[[dependencies.{ident}]]\nmodId="minecraft"\ntype="required"\nversionRange="{mc}"\n[[dependencies.{ident}]]\nmodId="neoforge"\ntype="required"\nversionRange="{nf}"\n')
            if classes:z.writestr('dev/ftb/Example.class',b'test fixture only')
        return p
    def confirm(self,p):return official_ftb.confirm_game_jar(p,dict(modId='ftbquests',version='2101.1.37'))
    def test_expected_production_descriptor_accepted_without_runtime_claim(self):self.assertEqual([],self.confirm(self.game()))
    def test_source_or_empty_maven_artifact_rejected(self):
        with self.assertRaisesRegex(ValueError,'no game classes'):self.confirm(self.game(classes=False))
    def test_wrong_minecraft_rejected(self):
        with self.assertRaisesRegex(ValueError,'platform'):self.confirm(self.game(mc='[1.21.4,)'))
    def test_wrong_neoforge_rejected(self):
        with self.assertRaisesRegex(ValueError,'platform'):self.confirm(self.game(nf='[21.1.300,)'))
    def test_wrong_mod_id_rejected(self):
        with self.assertRaisesRegex(ValueError,'ID/version'):self.confirm(self.game(ident='ftbteams'))
    def test_dev_unexpanded_version_rejected(self):
        with self.assertRaisesRegex(ValueError,'ID/version'):self.confirm(self.game(ver='${version}'))
    def test_manual_download_validated_without_network(self):
        transport=Fake();transport.local_files=self.root
        file=version()['files'][0];(self.root/file['filename']).write_bytes(transport.payload)
        cache=self.root/'cache';cache.mkdir()
        self.assertTrue(transport.download(file,cache).exists());self.assertEqual([],transport.opens)
    def test_manual_wrong_file_does_not_fall_back_or_install(self):
        transport=Fake();transport.local_files=self.root
        file=version()['files'][0];(self.root/file['filename']).write_bytes(b'wrong')
        cache=self.root/'cache';cache.mkdir()
        with self.assertRaisesRegex(m.Blocked,'Manual official'):transport.download(file,cache)
        self.assertEqual([],transport.opens)
    def test_complete_official_bundle_uses_actual_descriptors_and_catalog_library(self):
        config=json.loads((m.ROOT/'pack/mods.json').read_text())['officialMavenCandidates']['ftb']
        bodies={}
        for entry in config['artifacts']:
            raw=io.BytesIO();ident=entry['modId']
            with zipfile.ZipFile(raw,'w') as z:
                deps=['architectury'] + (['ftblibrary'] if ident!='ftblibrary' else []) + (['ftbteams'] if ident=='ftbquests' else [])
                text=f'[[mods]]\nmodId="{ident}"\nversion="{entry["version"]}"\n'
                for dependency,rng in [('minecraft','[1.21.1,)'),('neoforge','[21.1.0,)')] + [(d,'[1,)') for d in deps]:
                    text+=f'[[dependencies.{ident}]]\nmodId="{dependency}"\ntype="required"\nversionRange="{rng}"\n'
                z.writestr('META-INF/neoforge.mods.toml',text);z.writestr('dev/ftb/Test.class',b'fixture')
            bodies[entry['artifact']]=raw.getvalue()
        class Transport(Fake):
            def open(inner,url,hosts):
                artifact=next(e for e in config['artifacts'] if '/'+e['artifact']+'/' in url)
                body=bodies[artifact['artifact']]
                if url.endswith('.xml'):
                    body=f'<metadata><groupId>dev.ftb.mods</groupId><artifactId>{artifact["artifact"]}</artifactId><versioning><versions><version>{artifact["version"]}</version></versions></versioning></metadata>'.encode()
                elif url.endswith('.sha1'):body=hashlib.sha1(body).hexdigest().encode()
                response=io.BytesIO(body);response.headers={'Content-Length':str(len(body))};return response
        transport=Transport();transport.projects['architectury-api']=dict(id='arch',title='Architectury API',slug='architectury-api',client_side='required',server_side='required')
        transport.projects['arch']=transport.projects['architectury-api'];transport.versions['arch']=version('arch','arch')
        resolver=m.Resolver(transport,{})
        key=official_ftb.resolve(resolver,m.ROOT/'pack/mods.json',self.root/'cache')
        self.assertEqual('ftb-maven:ftb-quests-neoforge',key)
        self.assertEqual(4,len(resolver.selected));self.assertEqual([],resolver.conflicts())
        self.assertEqual(3,len(resolver.selected[key]['dependencies']))

    def test_official_maven_release_must_exist_not_inferred_from_source(self):
        class Transport:
            def open(self,url,hosts):
                return io.BytesIO(b'<metadata><groupId>dev.ftb.mods</groupId><artifactId>ftb-library-neoforge</artifactId><versioning><versions/></versioning></metadata>')
        resolver=m.Resolver(Transport(),{})
        with self.assertRaisesRegex(ValueError,'not published'):
            official_ftb.resolve(resolver,m.ROOT/'pack/mods.json',self.root)
        self.assertEqual({},resolver.selected)
    def test_unrelated_metadata_not_accepted(self):
        class Transport:
            def open(self,url,hosts):return io.BytesIO(b'<metadata><artifactId>wrong</artifactId></metadata>')
        with self.assertRaisesRegex(ValueError,'coordinate mismatch'):
            official_ftb.resolve(m.Resolver(Transport(),{}),m.ROOT/'pack/mods.json',self.root)
    def test_official_maven_download_host_supported_no_mirrors(self):
        m.safe_url('https://maven.ftb.dev/releases/example.jar',m.FILE_HOSTS)
        with self.assertRaises(m.Blocked):m.safe_url('https://ftb-mirror.invalid/example.jar',m.FILE_HOSTS)


class ServerTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        self.profile=self.root/'server';(self.profile/'mods').mkdir(parents=True)
        (self.profile/'mods/reimagined-0.1.0-alpha.jar').write_bytes(jar('reimagined'))
        (self.profile/'server.properties').write_text('max-players=2\n')
        (self.profile/'jvm-args.txt').write_text('-Xms2G -Xmx4G\n')
        b=io.BytesIO()
        with zipfile.ZipFile(b,'w') as z:z.writestr('install_profile.json',json.dumps(dict(minecraft=m.MC,path='net.neoforged:neoforge:'+m.NF)))
        self.body=b.getvalue()
    def transport(self,corrupt=False):
        outer=self
        class T:
            def open(self,url,hosts):
                return io.BytesIO((('0'*40 if corrupt else hashlib.sha1(outer.body).hexdigest()).encode()) if url.endswith('.sha1') else outer.body)
        return T()
    def java(self,args,**kwargs):
        if '-version' in args:return server.subprocess.CompletedProcess(args,0,'','openjdk version "21.0.8"')
        (self.profile/'run.bat').write_text('@echo off\n')
        (self.profile/'user_jvm_args.txt').write_text('# upstream defaults\n')
        return server.subprocess.CompletedProcess(args,0)
    def test_preparation_keeps_eula_and_world_absent_and_sets_actual_jvm_file(self):
        with patch.object(server.subprocess,'run',side_effect=self.java):server.prepare(self.profile,'java',self.transport())
        self.assertFalse((self.profile/'eula.txt').exists());self.assertFalse((self.profile/'world').exists())
        self.assertIn('-Xmx4G',(self.profile/'user_jvm_args.txt').read_text())
        self.assertTrue((self.profile/'user_jvm_args.installer-original.txt').is_file())
    def test_existing_server_never_modified(self):
        (self.profile/'eula.txt').write_text('eula=false\n')
        with self.assertRaisesRegex(server.m.Blocked,'Existing server'):server.prepare(self.profile,'java',self.transport())
        self.assertEqual('eula=false\n',(self.profile/'eula.txt').read_text())
    def test_bad_checksum_prevents_executing_jar(self):
        with patch.object(server.subprocess,'run',side_effect=self.java) as run:
            with self.assertRaisesRegex(server.m.Blocked,'checksum'):server.prepare(self.profile,'java',self.transport(True))
            self.assertEqual(1,run.call_count)
        self.assertFalse((self.profile/'run.bat').exists())
    def test_wrong_java_prevents_download_or_install(self):
        with patch.object(server.subprocess,'run',return_value=server.subprocess.CompletedProcess([],0,'','version "17.0.1"')):
            with self.assertRaisesRegex(server.m.Blocked,'Java21'):server.prepare(self.profile,'java',self.transport())
        self.assertFalse((self.root/'loader-downloads').exists())

if __name__=='__main__':unittest.main()
