"""Deferred source fidelity and conserved fuel recipe validation."""
import json,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class ProductionReactorAssets(unittest.TestCase):
 def test_all_original_reactor_and_cell_pixels_are_unchanged(self):
  for name in ('production','reactor'):
   rows=json.loads((ROOT/f'art/owner-library/catalog/edited-2026-10-23-jimtafacts/{name}-runtime-copies.json').read_text(encoding='utf-8'))
   for row in rows:self.assertEqual((ROOT/row['source']).read_bytes(),(ROOT/row['runtime']).read_bytes(),row['source'])
 def test_active_fuel_recipes_cannot_reset_partly_spent_fuel(self):
  path=ROOT/'src/main/resources/data/jugcraft/recipe/jimtafacts_reactor'
  for name in ('uranium','mox'):
   for tier,count in [('simple',1),('dual',2),('quad',4)]:
    recipe=json.loads((path/f'{name}_{tier}.json').read_text(encoding='utf-8'))
    self.assertEqual(recipe['ingredients'].count('jugcraft:leu_fuel_assembly'),count)
    self.assertFalse(any('_rod_simple' in item or '_rod_dual' in item or '_rod_quad' in item for item in recipe['ingredients']))
 def test_recycling_is_restricted_to_explicit_unvalued_junk(self):
  tag=json.loads((ROOT/'src/main/resources/data/jugcraft/tags/item/jimta_recyclable.json').read_text(encoding='utf-8'))
  self.assertNotIn('jugcraft:jimta_scrap',tag['values']);self.assertNotIn('jugcraft:jimta_scrapbox',tag['values'])
  self.assertTrue(all(not value.startswith('#') for value in tag['values']))
if __name__=='__main__':unittest.main()
