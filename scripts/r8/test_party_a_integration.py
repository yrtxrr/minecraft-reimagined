import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(Path(__file__).parent))
from party_a_plan import load_plan,make_plan
s=importlib.util.spec_from_file_location('party_install',Path(__file__).with_name('install_party_a.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)

class IntegrationTests(unittest.TestCase):
 def setUp(self):
  self.raw=(ROOT/'pack/r8/baseline/r7-installed.lock.json').read_bytes();self.base=json.loads(self.raw)
  self.evidence=json.loads((ROOT/'pack/r8/party-a/evidence.json').read_bytes())
  self.sha=hashlib.sha256(self.raw).hexdigest()
 def test_exact_three_additions_no_base_changes(self):
  p=make_plan(self.base,self.evidence,self.sha)
  self.assertEqual(self.base['mods'],p['mods'][:53]);self.assertEqual(self.base['targets'],p['targets'])
  self.assertEqual(56,len(p['mods']));self.assertFalse(p['runtime_verified'])
  self.assertEqual(56,len(m.m.validate_plan(p)[0]))
  self.assertEqual(51,sum('server' in r['sides'] for r in p['mods']))
 def test_mixed_build_or_unexpected_project_rejected(self):
  for field,value in [('version','10.7.19.84'),('project_id','impostor'),('base_game_version','10.7.18')]:
   e=copy.deepcopy(self.evidence);e['results'][0][field]=value
   with self.assertRaises(ValueError):make_plan(self.base,e,self.sha)
 def test_shipped_plan_equals_generated(self):
  self.assertEqual(load_plan(ROOT),json.loads((ROOT/'pack/r8/party-a/install-plan.json').read_text(encoding='utf-8')))
 def test_client_only_mods_excluded_from_server(self):
  p=load_plan(ROOT);client={r['file']['filename'] for r in p['mods'] if r['sides']==['client']}
  self.assertEqual(5,len(client));self.assertTrue(all(not ('server' in r['sides']) for r in p['mods'] if r['file']['filename'] in client))
 def test_prism_test_identity_and_target_platform(self):
  with tempfile.TemporaryDirectory() as folder:
   folder=Path(folder)
   with zipfile.ZipFile(folder/'Reimagined-candidate.mrpack','w') as z:z.writestr('modrinth.index.json',json.dumps({'dependencies':{'minecraft':'1.21.1','neoforge':'21.1.264'}}))
   p=m.label_mrpack(folder)
   with zipfile.ZipFile(p) as z:
    index=json.loads(z.read('modrinth.index.json'));self.assertIn('R8 Party A',index['name']);self.assertEqual('21.1.264',index['dependencies']['neoforge'])
if __name__=='__main__':unittest.main()
