"""Original crop stages, exact plug geometry and reachable explicit breeding. Deferred until combined tests."""
import importlib.util,json,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class IndustrialFarmingAssets(unittest.TestCase):
 def test_original_pixels_are_unchanged(self):
  for name in ('automation','crop'):
   rows=json.loads((ROOT/f'art/owner-library/catalog/edited-2026-10-23-jimtafacts/{name}-runtime-copies.json').read_text(encoding='utf-8'))
   for row in rows:self.assertEqual((ROOT/row['source']).read_bytes(),(ROOT/row['runtime']).read_bytes(),row['source'])
 def test_every_crop_is_reachable_from_base_seed_conversions(self):
  catalog=json.loads((ROOT/'src/main/resources/jugcraft/jimtafacts/crops.json').read_text(encoding='utf-8'));known={'wheat','pumpkin','melon','carrots','potato','reed','cocoa','nether_wart','red_mushroom','brown_mushroom'}
  for _ in range(len(catalog)):
   known|={name for name,row in catalog.items() if set(row['breed'])<=known}
  self.assertEqual(set(catalog),known)
 def test_gate_vertices_and_uvs_retain_source_shape(self):
  source=json.loads((ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcraftsilicon/models/plugs/gate.json').read_text(encoding='utf-8'))
  model=json.loads((ROOT/'src/main/resources/assets/jugcraft/models/block/jimta_gate_iron_and.json').read_text(encoding='utf-8'))
  for old,new in zip(source['elements'][:2],model['elements']):
   self.assertEqual(old['from'],new['from']);self.assertEqual(old['to'],new['to'])
   self.assertEqual({f:v['uv'] for f,v in old['faces'].items()},{f:v['uv'] for f,v in new['faces'].items()})
if __name__=='__main__':unittest.main()
