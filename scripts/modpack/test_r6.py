import json
import io
import zipfile
import hashlib
import tempfile
from pathlib import Path
import unittest
import audit_jars
from test_install import m,Fake,project,version

class R6Tests(unittest.TestCase):
    def test_actual_windows_neoforge_beta_lower_bound(self):
        self.assertTrue(audit_jars.numeric_range_contains('21.1.264','[21.0.118-beta,)'))
    def test_actual_jar_beta_neoforge_bound_passes_production_inspection(self):
        body=io.BytesIO()
        with zipfile.ZipFile(body,'w') as z:
            z.writestr('META-INF/neoforge.mods.toml','[[mods]]\nmodId="jei"\nversion="1"\n[[dependencies.jei]]\nmodId="minecraft"\ntype="required"\nversionRange="[1.21.1]"\n[[dependencies.jei]]\nmodId="neoforge"\ntype="required"\nversionRange="[21.0.118-beta,)"\n')
        raw=body.getvalue();file=version()['files'][0]
        file.update(size=len(raw),hashes={'sha512':hashlib.sha512(raw).hexdigest()})
        with tempfile.TemporaryDirectory() as folder:
            fake=Fake();fake.artifact_cache=Path(folder);fake.payload=raw
            m.Resolver(fake,{}).verify_platform(file,'jei')
    def test_lower_numeric_version_is_not_allowed_by_qualifier(self):
        self.assertFalse(audit_jars.numeric_range_contains('21.0.117','[21.0.118-beta,)'))
    def test_release_follows_beta_at_identical_base(self):
        self.assertTrue(audit_jars.numeric_range_contains('21.0.118','[21.0.118-beta,)'))
        self.assertFalse(audit_jars.numeric_range_contains('21.0.118-beta','[21.0.118,)'))
    def test_upper_bound_inclusivity_and_qualifier_order(self):
        self.assertFalse(audit_jars.numeric_range_contains('21.0.118','[21.0.118-beta,21.0.118)'))
        self.assertFalse(audit_jars.numeric_range_contains('21.0.118-beta1','[21.0.118-beta2,)'))
        self.assertTrue(audit_jars.numeric_range_contains('21.0.118-rc1','[21.0.118-beta2,)'))
    def test_original_minecraft_conflict_still_rejected(self):
        self.assertFalse(audit_jars.numeric_range_contains('1.21.1','[1.21,1.21.1)'))
    def test_unknown_suffix_remains_unverified(self):
        self.assertIsNone(audit_jars.numeric_range_contains('21.1.264','[21.0.118-custom,)'))
    def test_moreculling_beta_scoped_to_official_id(self):
        bindings=json.loads((m.ROOT/'modpack/source-bindings.json').read_text())
        fake=Fake();fake.projects['51shyZVL']=project('51shyZVL','More Culling')
        v=version('cull1','51shyZVL');v['version_type']='beta';fake.versions['cull1']=v
        r=m.Resolver(fake,bindings)
        self.assertEqual('beta',r.selected[r.mr('51shyZVL')]['release_type'])
        fake.versions['v1']['version_type']='beta'
        with self.assertRaises(m.Blocked):r.mr('root')
    def test_moreculling_wrong_loader_still_rejected(self):
        bindings=json.loads((m.ROOT/'modpack/source-bindings.json').read_text())
        fake=Fake();fake.projects['51shyZVL']=project('51shyZVL','More Culling')
        v=version('cull1','51shyZVL',loaders=['fabric']);v['version_type']='beta';fake.versions['cull1']=v
        with self.assertRaises(m.Blocked):m.Resolver(fake,bindings).mr('51shyZVL')

if __name__=='__main__':unittest.main()
