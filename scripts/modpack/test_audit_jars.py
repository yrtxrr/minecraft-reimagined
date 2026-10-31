import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
import audit_jars


def jar(ident, dependencies='', nested=None):
    out = io.BytesIO()
    with zipfile.ZipFile(out, 'w') as z:
        z.writestr('META-INF/neoforge.mods.toml', f'[[mods]]\nmodId="{ident}"\nversion="1"\n'+dependencies)
        if nested:
            z.writestr('META-INF/jarjar/metadata.json', json.dumps({'jars':[{'path':'lib.jar'}]}))
            z.writestr('lib.jar', nested)
    return out.getvalue()


class AuditTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        self.root=Path(self.tmp.name)
    def write(self, name, data):
        p=self.root/name; p.write_bytes(data); return p
    def dep(self, ident='library', side='BOTH'):
        return f'[[dependencies.root]]\nmodId="{ident}"\ntype="required"\nversionRange="[1,)"\nside="{side}"\n'
    def test_missing_catalog_dependency_detected_in_actual_jar(self):
        p=self.write('root.jar',jar('root',self.dep()))
        report=audit_jars.audit([p],'server')
        self.assertIn('requires missing library',report['errors'][0])
        self.assertFalse(report['runtime_verified'])
    def test_client_dependency_not_required_on_server(self):
        p=self.write('root.jar',jar('root',self.dep(side='CLIENT')))
        self.assertEqual([],audit_jars.audit([p],'server')['errors'])
        self.assertTrue(audit_jars.audit([p],'client')['errors'])
    def test_embedded_library_satisfies_dependency_without_extracting(self):
        p=self.write('root.jar',jar('root',self.dep(),jar('library')))
        report=audit_jars.audit([p],'client')
        self.assertEqual([],report['errors']);self.assertEqual(2,len(report['mods']))
        self.assertEqual([p],list(self.root.iterdir()))
    def test_shared_embedded_library_is_left_to_neoforge(self):
        paths=[self.write('root.jar',jar('root',self.dep(),jar('library'))),self.write('other.jar',jar('other',nested=jar('library')))]
        report=audit_jars.audit(paths,'server')
        self.assertEqual([],report['errors']);self.assertTrue(report['notes'])
        self.assertFalse(report['version_ranges_checked'])
    def test_top_level_duplicate_rejected(self):
        paths=[self.write('a.jar',jar('root')),self.write('b.jar',jar('root'))]
        self.assertTrue(audit_jars.audit(paths,'server')['errors'])
    def test_forge_dependency_not_treated_as_neoforge(self):
        p=self.write('root.jar',jar('root',self.dep('forge')))
        self.assertTrue(audit_jars.audit([p],'server')['errors'])
    def test_wrong_numeric_platform_range_blocks_real_jar_profile(self):
        dep=self.dep('minecraft').replace('[1,)','[1.21, 1.21.1)')
        p=self.write('root.jar',jar('root',dep))
        report=audit_jars.audit([p],'client')
        self.assertIn('excludes minecraft 1.21.1',report['errors'][0])
        self.assertFalse(report['platform_checks'][0]['matches'])
    def test_unknown_side_rejected(self):
        p=self.write('root.jar',jar('root',self.dep(side='UNKNOWN')))
        self.assertTrue(audit_jars.audit([p],'server')['errors'])
    def test_embedded_depth_is_bounded(self):
        data=jar('library')
        for i in range(6):data=jar('nested'+str(i),nested=data)
        with self.assertRaisesRegex(ValueError,'budget'):audit_jars.inspect(self.write('root.jar',data))

if __name__=='__main__':unittest.main()
