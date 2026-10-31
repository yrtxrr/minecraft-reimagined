import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

ROOT=Path(__file__).resolve().parents[2]
class PackageTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):subprocess.run([sys.executable,str(ROOT/'scripts/magic-lab/package.py')],check=True)
    def test_public_zip_contains_tools_no_binaries(self):
        with zipfile.ZipFile(ROOT/'build/modpack/Reimagined-Magic-Ports-Windows.zip') as z:
            self.assertFalse(any(n.endswith('.jar') for n in z.namelist()))
            self.assertIn('Build-MagicLab.ps1',z.namelist());self.assertIn('START_HERE_RU.md',z.namelist())
    def test_native_prism_export_and_no_overwrite(self):
        with tempfile.TemporaryDirectory() as folder:
            folder=Path(folder)
            with zipfile.ZipFile(ROOT/'build/modpack/Reimagined-Magic-Ports-Windows.zip') as z:z.extractall(folder/'tools')
            spec=importlib.util.spec_from_file_location('lab',folder/'tools/magic_lab.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
            jar=folder/'fake.jar';jar.write_bytes(b'fixture only')
            out=m.profile(folder/'out','Test',[jar])
            with zipfile.ZipFile(out) as z:
                cfg=json.loads(z.read('mmc-pack.json'))
                self.assertEqual('net.neoforged',cfg['components'][1]['uid']);self.assertEqual('21.1.264',cfg['components'][1]['version'])
                self.assertIn('.minecraft/mods/fake.jar',z.namelist());self.assertNotIn('.minecraft/eula.txt',z.namelist())
            with self.assertRaises(ValueError):m.profile(folder/'out','Test',[jar])
    def test_pins_and_runtime_status_retained(self):
        with zipfile.ZipFile(ROOT/'build/modpack/Reimagined-Magic-Ports-Windows.zip') as z:
            p=json.loads(z.read('profile.json'))
            self.assertFalse(p['runtime_verified']);self.assertFalse(p['install_into_r4'])
            self.assertEqual('dffee19a4f7f82e0538d323d5de403883db1fe0c',p['ports'][0]['revision'])
if __name__=='__main__':unittest.main()
