"""Deferred combined validation of source art, atlas UVs and counted industrial batches."""
import importlib.util
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('jimtafacts_industry',ROOT/'tools/jimtafacts_industry.py')
industry=importlib.util.module_from_spec(spec);spec.loader.exec_module(industry)

class IndustryAssets(unittest.TestCase):
 def test_original_art_is_unchanged_and_every_runtime_copy_has_a_source(self):
  copies=json.loads((ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/industry-runtime-copies.json').read_text(encoding='utf-8'))
  self.assertGreaterEqual(len(copies),24)
  for row in copies:self.assertEqual((ROOT/row['source']).read_bytes(),(ROOT/row['runtime']).read_bytes(),row['source'])
 def test_source_atlas_uvs_stay_inside_original_sheet(self):
  for id,row in industry.MACHINES.items():
   # BuildCraft tables use their original element geometry and implicit UVs;
   # this check covers the IndustrialCraft atlas projection only.
   if row[2] is None or row[2].startswith('bc:'):continue
   for suffix in ('','_active'):
    model=json.loads((industry.RES/f'assets/jugcraft/models/block/{id}{suffix}.json').read_text(encoding='utf-8'))
    for face in model['elements'][0]['faces'].values():
     self.assertTrue(all(0<=v<=16 for v in face['uv']))
     self.assertGreater(face['uv'][2],face['uv'][0])
 def test_complete_batches_and_casing_conversions_have_no_duplicate_material_shortcut(self):
  recipes=industry.recipes()
  self.assertEqual(recipes['compress_iron']['input_count'],9)
  self.assertEqual(recipes['centrifuge_iron']['input_count'],9)
  self.assertEqual(recipes['centrifuge_iron']['minimum_heat'],1500)
  self.assertEqual(recipes['extract_latex']['byproducts'][0]['result'],{'id':'minecraft:glass_bottle','count':1})
  for id,row in industry.MACHINES.items():
   if row[3]:
    extras = {'jimta_advanced_crafting_table': ['jugcraft:advanced_circuit'],
              'jimta_integration_table': ['jugcraft:basic_circuit']}
    self.assertEqual(recipes[id+'_housing']['ingredients'],['jugcraft:'+row[3]] + extras.get(id, []))
    self.assertEqual(recipes[id+'_restore']['result'],{'id':'jugcraft:'+row[3],'count':1})

if __name__=='__main__':unittest.main()
