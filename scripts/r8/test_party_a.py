import importlib.util
import io
import hashlib
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile
sys.path.insert(0,str(Path(__file__).parent))
s=importlib.util.spec_from_file_location('probe',Path(__file__).with_name('probe_party_a.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)

class PartyATests(unittest.TestCase):
 def test_only_exact_base_build_channel_loader(self):
  good=dict(project_id='OFVYKsAk',version_number='10.7.19.85',version_type='release',game_versions=['1.21.1'],loaders=['neoforge'])
  bads=[{**good,'version_number':'10.7.19.84'},{**good,'loaders':['forge']},{**good,'game_versions':['1.21']},{**good,'version_type':'beta'},{**good,'project_id':'other'}]
  self.assertEqual([good],m.module_candidates([good]+bads,{'version':'10.7.19.85','release_type':'release'}))
 def artifact(self,version='10.7.19.85',dependency='[${file.jarVersion}]'):
  tmp=tempfile.TemporaryDirectory();self.addCleanup(tmp.cleanup);path=Path(tmp.name)/'module.jar'
  with zipfile.ZipFile(path,'w') as z:
   z.writestr('META-INF/MANIFEST.MF','Implementation-Version: '+version+'\n')
   z.writestr('example.class',b'fixture only')
   z.writestr('META-INF/neoforge.mods.toml','modLoader="javafml"\nloaderVersion="[4,)"\nlicense="MIT"\n[[mods]]\nmodId="mekanismgenerators"\nversion="${file.jarVersion}"\n[[dependencies.mekanismgenerators]]\nmodId="mekanism"\ntype="required"\nversionRange="'+dependency+'"\n')
  return path
 def test_real_descriptor_manifest_and_exact_dependency(self):
  self.assertEqual('10.7.19.85',m.inspect_module(self.artifact(),'mekanismgenerators','10.7.19.85')['mods'][0]['version'])
 def test_wrong_build_and_loose_base_range_rejected(self):
  for path in [self.artifact(version='10.7.19.84'),self.artifact(dependency='[10.7.19,)')]:
   with self.assertRaises(ValueError):m.inspect_module(path,'mekanismgenerators','10.7.19.85')
 def test_uppercase_required_extra_dependency_is_not_silently_ignored(self):
  path=self.artifact()
  with zipfile.ZipFile(path,'a') as z:
   original=z.read('META-INF/neoforge.mods.toml').decode()
  # Rewrite fixture once, avoiding duplicate ZIP entries.
  with zipfile.ZipFile(path,'w') as z:
   z.writestr('META-INF/MANIFEST.MF','Implementation-Version: 10.7.19.85\n')
   z.writestr('example.class',b'fixture only')
   z.writestr('META-INF/neoforge.mods.toml',original+'[[dependencies.mekanismgenerators]]\nmodId="unreviewed"\ntype="REQUIRED"\nversionRange="[1,)"\n')
  with self.assertRaisesRegex(ValueError,'Additional mandatory'):m.inspect_module(path,'mekanismgenerators','10.7.19.85')
 def test_hash_verified_metadata_report_is_not_runtime_or_install_success(self):
  body=self.artifact(version='1.21.1-10.7.19.85').read_bytes()
  file={'url':'https://cdn.modrinth.com/data/OFVYKsAk/versions/fixture/module.jar','filename':'module.jar','primary':True,'size':len(body),'hashes':{'sha512':hashlib.sha512(body).hexdigest()}}
  version=dict(project_id='OFVYKsAk',version_number='10.7.19.85',version_type='release',game_versions=['1.21.1'],loaders=['neoforge'],id='fixture',date_published='2026-11-01',files=[file])
  def get(route):
   if '/version?' in route:return [version]
   return {'id':'OFVYKsAk','license':{'id':'MIT'},'client_side':'required','server_side':'required'}
  class Transport:
   download=m.installer.Transport.download
   def open(self,url,hosts):return io.BytesIO(body)
  with tempfile.TemporaryDirectory() as folder:
   r=m.probe('mekanism-generators',{'version':'10.7.19.85','release_type':'release'},Path(folder),get,Transport(),base_game_version='1.21.1-10.7.19.85')
  self.assertEqual(hashlib.sha256(body).hexdigest(),r['sha256'])
  self.assertEqual('1.21.1-10.7.19.85',r['base_game_version']);self.assertEqual('10.7.19.85',r['version']);self.assertEqual('VERIFIED_METADATA',r['status']);self.assertFalse(r['runtime_verified']);self.assertFalse(r['installed'])
 def test_impostor_project_is_rejected_before_download(self):
  with self.assertRaisesRegex(ValueError,'project ID mismatch'):
   m.probe('mekanism-generators',{'version':'10.7.19.85'},Path('/unused'),get=lambda route:{'id':'wrong'})

if __name__=='__main__':unittest.main()
