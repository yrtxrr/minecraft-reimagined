import importlib.util
from pathlib import Path
import unittest
s=importlib.util.spec_from_file_location('research',Path(__file__).with_name('research_addons.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)

class ResearchTests(unittest.TestCase):
 def candidate(self):return dict(id='x',name='Example',status='UNVERIFIED',project_id={'modrinth':'official'})
 def test_filters_platform_and_never_promotes_status(self):
  def get(route):
   if '/version?' not in route:return dict(id='official',title='Example')
   return [dict(id='good',date_published='2026-10-31',game_versions=['1.21.1'],loaders=['neoforge']),dict(id='wrong',date_published='2026-10-31',game_versions=['1.21.1'],loaders=['fabric'])]
  result=m.collect(self.candidate(),get)
  self.assertEqual(1,result['compatible_catalog_count']);self.assertEqual('UNVERIFIED',result['status']);self.assertFalse(result['jar_downloaded'])
 def test_different_reviewed_id_fails(self):
  result=m.collect(self.candidate(),lambda route:dict(id='impostor'))
  self.assertIn('mismatch',result['error']);self.assertNotIn('versions',result)
 def test_ambiguous_identity_stays_unknown(self):
  c=self.candidate();c['project_id']=None
  result=m.collect(c,lambda route:{'hits':[dict(title='Example',project_id='a'),dict(title='Example',project_id='b')]})
  self.assertIn('No unique',result['error'])
if __name__=='__main__':unittest.main()
