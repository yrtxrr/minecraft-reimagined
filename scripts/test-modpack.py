import copy
import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('pack', Path(__file__).with_name('assemble-modpack.py'))
pack = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pack)

class PackTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.old = pack.PACK
        self.addCleanup(setattr, pack, 'PACK', self.old)
        pack.PACK = Path(self.temp.name)
        (pack.PACK / 'downloads').mkdir()
        self.registry = {'schema': 1, 'mods': [{'id': 'test', 'role': 'target'}]}
        self.lock = {'schema': 1, 'minecraft': '1.21.1', 'neoforge': '21.1.264', 'mods': []}
        self.artifact = pack.PACK / 'downloads/test.jar'
        self.artifact.write_bytes(b'test artifact')
        self.mod = dict(id='test', version='1', minecraft='1.21.1', loader='neoforge', side='both',
                        verification='client-and-save-restart', license='test-only', personal_use_allowed=True,
                        source_url='https://example.invalid', dependencies=[], file='test.jar',
                        sha256=hashlib.sha256(self.artifact.read_bytes()).hexdigest())

    def test_core_only_cannot_masquerade_as_full(self):
        self.assertEqual({}, pack.validated_mods(self.registry, self.lock, 'core-only'))
        with self.assertRaisesRegex(ValueError, 'missing verified mods'):
            pack.validated_mods(self.registry, self.lock, 'full')

    def test_verified_local_artifact_and_checksum(self):
        self.lock['mods'] = [self.mod]
        self.assertIn('test', pack.validated_mods(self.registry, self.lock, 'full'))
        self.artifact.write_bytes(b'tampered')
        with self.assertRaisesRegex(ValueError, 'SHA256 mismatch'):
            pack.validated_mods(self.registry, self.lock, 'full')

    def test_unsafe_artifact_path_rejected(self):
        self.mod['file'] = '../test.jar'
        self.lock['mods'] = [self.mod]
        with self.assertRaisesRegex(ValueError, 'Unsafe artifact'):
            pack.validated_mods(self.registry, self.lock, 'full')

    def test_missing_dependency_rejected(self):
        self.mod['dependencies'] = ['required_library']
        self.lock['mods'] = [self.mod]
        with self.assertRaisesRegex(ValueError, 'Unresolved required dependency'):
            pack.validated_mods(self.registry, self.lock, 'full')

    def test_unverified_or_wrong_loader_not_bundled(self):
        for key, value in [('loader', 'fabric'), ('verification', 'metadata-only'), ('side', 'unverified')]:
            mod = copy.deepcopy(self.mod); mod[key] = value; self.lock['mods'] = [mod]
            with self.assertRaises(ValueError):
                pack.validated_mods(self.registry, self.lock, 'full')

if __name__ == '__main__':
    unittest.main()
