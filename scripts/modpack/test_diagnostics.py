import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

spec=importlib.util.spec_from_file_location('diagnostics',Path(__file__).with_name('collect-diagnostics.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


class DiagnosticsTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name)
        self.instance=self.root/'minecraft';(self.instance/'logs').mkdir(parents=True)
        self.out=self.root/'report.zip'
    def test_named_logs_only_not_accounts_worlds_or_jars(self):
        (self.instance/'logs/latest.log').write_text('loaded fixture\n')
        (self.instance/'accounts.json').write_text('secret')
        (self.instance/'saves').mkdir();(self.instance/'saves/level.dat').write_bytes(b'private world')
        (self.instance/'mods').mkdir();(self.instance/'mods/test.jar').write_bytes(b'fixture binary')
        m.collect(self.instance,None,None,self.out)
        with zipfile.ZipFile(self.out) as z:
            self.assertEqual({'client/logs/latest.log','client/mods-sha256.json','COLLECTION.json'},set(z.namelist()))
            self.assertFalse(json.loads(z.read('COLLECTION.json'))['runtime_verified'])
            self.assertEqual('test.jar',json.loads(z.read('client/mods-sha256.json'))[0]['file'])
    def test_obvious_tokens_are_redacted(self):
        text='--accessToken secret_token\nAuthorization: Bearer abc.def\n"refresh_token":"private"\nx-api-key=key_value\n'
        cleaned=m.redact(text)
        for secret in ('secret_token','abc.def','private','key_value'):self.assertNotIn(secret,cleaned)
    def test_existing_output_is_preserved(self):
        self.out.write_bytes(b'keep')
        with self.assertRaises(FileExistsError):m.collect(self.instance,None,None,self.out)
        self.assertEqual(b'keep',self.out.read_bytes())
    def test_installation_failure_collected_without_client(self):
        (self.root/'INSTALL_REPORT.md').write_text('unresolved FTB')
        m.collect(None,self.root,None,self.out)
        with zipfile.ZipFile(self.out) as z:self.assertIn('installation/INSTALL_REPORT.md',z.namelist())
    def test_oversized_logs_reported_without_claiming_complete_collection(self):
        with (self.instance/'logs/debug.log').open('wb') as f:f.truncate(m.LIMIT+1)
        _,notes=m.collect(self.instance,None,None,self.out)
        self.assertTrue(any('size budget' in n for n in notes))
    def test_symlink_log_outside_instance_is_not_read(self):
        target=self.root/'secret';target.write_text('private')
        try:(self.instance/'logs/latest.log').symlink_to(target)
        except OSError:self.skipTest('symlinks unavailable on this platform')
        files,notes=m.collect(self.instance,None,None,self.out)
        self.assertNotIn('client/logs/latest.log',files);self.assertTrue(notes)

if __name__=='__main__':unittest.main()
