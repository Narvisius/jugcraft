"""Deferred source armor/icon fidelity and distinct survival crafting checks."""
import importlib.util,json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('jimtafacts_gear',ROOT/'tools/jimtafacts_gear.py');gear=importlib.util.module_from_spec(spec);spec.loader.exec_module(gear)
class IndustrialGearAssets(unittest.TestCase):
 def test_icons_armor_and_animation_are_byte_identical(self):
  rows=json.loads((ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/gear-runtime-copies.json').read_text(encoding='utf-8'))
  self.assertTrue(any(x['source'].endswith('itemNanoSaber.active.png.mcmeta') for x in rows))
  self.assertTrue(any('/textures/armor/quantum_2.png' in x['source'] for x in rows))
  for row in rows:self.assertEqual((ROOT/row['source']).read_bytes(),(ROOT/row['runtime']).read_bytes(),row['source'])
 def test_same_shapeless_recipe_never_ambiguously_yields_different_gear(self):
  seen={}
  for id,r in gear.recipes().items():
   if r['type']!='minecraft:crafting_shapeless':continue
   key=tuple(sorted(r['ingredients']));self.assertNotIn(key,seen,(id,seen.get(key)));seen[key]=id
 def test_every_equipment_layer_is_shipped(self):
  for row in gear.GEAR.values():
   if 'asset' not in row:continue
   model=json.loads((gear.RES/f"assets/jugcraft/equipment/jimtafacts/{row['asset']}.json").read_text(encoding='utf-8'))
   layer='humanoid_leggings' if row['slot']=='LEGGINGS' else 'humanoid';self.assertIn(layer,model['layers'])
   for texture in model['layers'][layer]:self.assertTrue((gear.RES/f"assets/jugcraft/textures/entity/equipment/{layer}/{texture['texture'].split(':',1)[1]}.png").exists())
if __name__=='__main__':unittest.main()
