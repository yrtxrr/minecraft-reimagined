import io
from pathlib import Path
import tempfile
import unittest
import zipfile
import audit_jars as a

class MatrixTests(unittest.TestCase):
    def test_exact_target_uses_confirmed_fml_matrix(self):
        self.assertFalse(a.numeric_range_contains('1.21.1','[1.21,1.21.1)'))
        self.assertTrue(a.platform_range_contains('minecraft','1.21.1','[1.21,1.21.1)'))
        self.assertTrue(a.platform_range_contains('neoforge','21.1.264','[21.0.166]'))
    def test_no_global_range_weakening(self):
        for context in [dict(neoforge='21.1.263'),dict(fml='4.0.44'),dict(minecraft='1.21.2')]:
            self.assertFalse(a.platform_range_contains('minecraft','1.21.1','[1.21,1.21.1)',**context))
        self.assertFalse(a.platform_range_contains('minecraft','1.21.1','[1.20,1.21)'))
        self.assertIsNone(a.platform_range_contains('minecraft','1.21.1','nonsense'))

class DependencyTests(unittest.TestCase):
    def check(self, mezz='0.6.3',cloth='15.0.0',sodium=None):
        tmp=tempfile.TemporaryDirectory();self.addCleanup(tmp.cleanup);root=Path(tmp.name)
        paths=[]
        def add(ident,version,deps='',manifest=None):
            p=root/(ident+'.jar')
            with zipfile.ZipFile(p,'w') as z:
                z.writestr('META-INF/neoforge.mods.toml',f'[[mods]]\nmodId="{ident}"\nversion="{version}"\n'+deps)
                if manifest:z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nImplementation-Version: '+manifest+'\n')
            paths.append(p)
        def dep(owner,ident,rng,typ='required'):
            return f'[[dependencies.{owner}]]\nmodId="{ident}"\nversionRange="{rng}"\ntype="{typ}"\nside="CLIENT"\n'
        add('jei','19.70.0.468',dep('jei','minecraft','[1.21,1.21.1)')+dep('jei','mezz_config','[0.6.3,1.0.0)'))
        add('moreculling','1.0.10',dep('moreculling','cloth_config','[15.0.0,)')+dep('moreculling','sodium','[,0.8.0)','incompatible'))
        if mezz:add('mezz_config',mezz)
        if cloth:add('cloth_config','${file.jarVersion}',manifest=cloth)
        if sodium:add('sodium',sodium)
        return a.audit(paths,'client')
    def test_confirmed_descriptor_ranges_and_no_sodium(self):self.assertEqual([],self.check()['errors'])
    def test_mezz_upper_bound_and_minimum_enforced(self):
        self.assertTrue(self.check(mezz='1.0.0')['errors']);self.assertTrue(self.check(mezz='0.6.2')['errors'])
    def test_cloth_minimum_and_missing_library(self):
        self.assertTrue(self.check(cloth='14.0.0')['errors']);self.assertTrue(self.check(cloth=None)['errors'])
    def test_sodium_incompatibility_boundary(self):
        self.assertTrue(self.check(sodium='0.7.3')['errors'])
        self.assertEqual([],self.check(sodium='0.8.0')['errors'])
    def test_unknown_sodium_version_is_not_silently_accepted(self):self.assertTrue(self.check(sodium='unknown')['errors'])

if __name__=='__main__':unittest.main()
