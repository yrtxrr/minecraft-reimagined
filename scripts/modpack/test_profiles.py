import importlib.util
import io,json,sys,tempfile,unittest,zipfile,shutil
from unittest.mock import patch
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
spec=importlib.util.spec_from_file_location('profiles',Path(__file__).with_name('export-pinned-profiles.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

class ProfileTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        self.core=self.root/'core.jar'
        with zipfile.ZipFile(self.core,'w') as z:z.writestr('META-INF/neoforge.mods.toml','[[mods]]\nmodId="reimagined"\n')
    def test_exact_25_pins_have_real_sha512_without_invented_sha1(self):
        client,_=m.export(self.root/'out',self.core,True)
        with zipfile.ZipFile(client) as z:
            index=json.loads(z.read('modrinth.index.json'));self.assertEqual(25,len(index['files']))
            pins=json.loads((m.ROOT/'pack/mods.json').read_text())['mods'];by_name={p['filename']:p for p in pins}
            for f in index['files']:
                pin=by_name[f['path'].split('/')[-1]];self.assertEqual(pin['sha512'],f['hashes']['sha512']);self.assertNotIn('sha1',f['hashes']);self.assertEqual([pin['url']],f['downloads'])
            self.assertEqual('21.1.264',index['dependencies']['neoforge']);self.assertIn('NOT included',index['summary'])
    def test_server_uses_same_manifest_no_world_no_eula_no_client_options(self):
        _,server=m.export(self.root/'out',self.core,True)
        with zipfile.ZipFile(server) as z:
            self.assertEqual((m.ROOT/'pack/mods.json').read_bytes(),z.read('pack/mods.json'))
            self.assertIn('server/config/ftbquests/quests/chapters/reimagined_intro.snbt',z.namelist())
            self.assertNotIn('server/options.txt',z.namelist());self.assertFalse(any('eula.txt' in n or '/saves/' in n for n in z.namelist()))
            self.assertEqual(1,sum(n.endswith('.jar') for n in z.namelist()))
    def test_optional_off_is_24_and_cold_sweat_not_silently_installed(self):
        client,_=m.export(self.root/'out',self.core,False)
        with zipfile.ZipFile(client) as z:
            files=json.loads(z.read('modrinth.index.json'))['files'];self.assertEqual(24,len(files));self.assertFalse(any('coldsweat' in f['path'].lower().replace('-','') for f in files))
    def test_extracted_toolkit_export_does_not_require_git(self):
        source=m.ROOT;offline=self.root/'offline';offline.mkdir()
        for folder in ['pack','modpack/overrides','modpack/server']:
            shutil.copytree(source/folder,offline/folder,ignore=shutil.ignore_patterns('mods'))
        for name in ['fetch-modpack.py','validate-modpack.py','modpack/install.py','modpack/audit_jars.py','modpack/official_ftb.py','modpack/prepare-server.py']:
            dest=offline/'scripts'/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source/'scripts'/name,dest)
        (offline/'installer-manifest.json').write_text(json.dumps({'revision':'a'*40}))
        with patch.object(m,'ROOT',offline),patch.object(m.subprocess,'check_output',side_effect=AssertionError('Git required')):
            client,_=m.export(self.root/'offline-out',self.core,True)
        with zipfile.ZipFile(client) as z:self.assertEqual('world-baseline-aaaaaaa',json.loads(z.read('modrinth.index.json'))['versionId'])
    def test_wrong_core_or_fixture_jar_rejected(self):
        with zipfile.ZipFile(self.core,'a') as z:z.writestr('dev/reimagined/gametest/Fake.class',b'bad')
        with self.assertRaises(m.Blocked):m.export(self.root/'out',self.core,True)

if __name__=='__main__':unittest.main()
