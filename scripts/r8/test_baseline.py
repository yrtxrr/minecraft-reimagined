import hashlib
import json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class BaselineTests(unittest.TestCase):
 def test_original_uploaded_lock_bytes_are_frozen(self):
  raw=(ROOT/'pack/r8/baseline/r7-installed.lock.json').read_bytes()
  self.assertEqual('88bfabac42f3c52675cd9690487efa4e9ee692e319feccc3261c3d6e9a279729',hashlib.sha256(raw).hexdigest())
  data=json.loads(raw);self.assertEqual(53,len(data['mods']))
  self.assertFalse(data['runtime_verified'])
 def test_core_match_does_not_claim_world_runtime(self):
  s=json.loads((ROOT/'pack/r8/r7-source-snapshot.json').read_text(encoding='utf-8'))
  self.assertEqual(s['local_archive_evidence']['owned_core_sha256'],s['versions']['reimagined']['sha256'])
  self.assertTrue(s['versions']['reimagined']['hash_matches_r7_packaged_core'])
  self.assertIn('NOT_TESTED',s['runtime_evidence'])
if __name__=='__main__':unittest.main()
