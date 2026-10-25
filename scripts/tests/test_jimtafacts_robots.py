"""Deferred original-model/resource and survival acquisition checks for native robots."""
import importlib.util
import json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('jimtafacts_robots',ROOT/'tools/jimtafacts_robots.py')
robots=importlib.util.module_from_spec(spec);spec.loader.exec_module(robots)

class RobotAssets(unittest.TestCase):
 def test_geometry_and_source_pixels_are_retained(self):
  manifest=json.loads((ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/robot-runtime-copies.json').read_text(encoding='utf-8'))
  self.assertTrue(any(row['source'].endswith('/models/robot.json') for row in manifest))
  for row in manifest:self.assertEqual((ROOT/row['source']).read_bytes(),(ROOT/row['runtime']).read_bytes(),row['source'])
 def test_every_program_has_an_obtainable_card_and_functional_robot(self):
  recipes=robots.recipes();self.assertEqual(len(robots.PROGRAMS),20)
  for p in robots.PROGRAMS:
   self.assertEqual(recipes['program_'+p]['result']['id'],'jugcraft:jimta_robot_board_'+p)
   self.assertEqual(recipes['robot_'+p]['ingredients'],['jugcraft:jimta_workshop_robot','jugcraft:jimta_robot_board_'+p])
   model=json.loads((robots.RES/f'assets/jugcraft/models/item/jimta_robot_{p}.json').read_text(encoding='utf-8'))
   self.assertEqual(model['parent'],'jugcraft:item/jimtafacts/robot_geometry')
 def test_all_items_have_model_definitions_and_names(self):
  lang=json.loads((robots.RES/'assets/jugcraft/lang/en_us.json').read_text(encoding='utf-8'))
  for id in robots.items():
   self.assertTrue((robots.RES/f'assets/jugcraft/items/{id}.json').exists(),id)
   self.assertIn('item.jugcraft.'+id,lang)

if __name__=='__main__':unittest.main()
