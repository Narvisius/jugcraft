"""Data-level conservation checks for the universal catalogue (runtime transactions have game tests)."""
from fractions import Fraction
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
VALUES=json.loads((ROOT/'src/main/resources/data/jugcraft/alchemy/values.json').read_text(encoding='utf-8'))['items']

def value(item):
    v=VALUES[item];return Fraction(v['numerator'],v['denominator'])

class AlchemyValueTests(unittest.TestCase):
    def test_historical_roots_are_not_the_old_reduced_values(self):
        for item,expected in {'diamond':8192,'gold_ingot':2048,'iron_ingot':256,'coal':128,'oak_log':32,'oak_planks':8}.items():
            self.assertEqual(value('minecraft:'+item),expected,item)

    def test_appended_mod_tags_preserve_vanilla_wood(self):
        for name in ('oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry'):
            self.assertEqual(value('minecraft:'+name+'_log'),32)
            self.assertEqual(value('minecraft:'+name+'_planks'),8)

    def test_block_and_nugget_round_trips(self):
        for metal in ('iron','gold','copper'):
            unit=value('minecraft:'+metal+'_ingot')
            self.assertEqual(value('minecraft:'+metal+'_block'),9*unit)
            if 'minecraft:'+metal+'_nugget' in VALUES:self.assertEqual(9*value('minecraft:'+metal+'_nugget'),unit)

    def test_filled_buckets_cannot_create_a_cheaper_empty_shell(self):
        for item in ('water_bucket','lava_bucket','milk_bucket','cod_bucket','powder_snow_bucket'):
            self.assertGreaterEqual(value('minecraft:'+item),value('minecraft:bucket'))

    def test_all_authored_item_types_have_explicit_records(self):
        for p in (ROOT/'src/main/resources/assets/jugcraft/items').glob('*.json'):
            self.assertIn('jugcraft:'+p.stem,VALUES,p.name)

    def test_prices_are_positive_bounded_rationals_and_provenanced(self):
        for item,v in VALUES.items():
            self.assertGreater(value(item),0,item);self.assertLessEqual(value(item),1_000_000_000,item)
            self.assertTrue(v['source'],item)

    def test_matter_uses_actual_local_recipe_inputs(self):
        self.assertEqual(value('jugcraft:dark_matter'),8*value('jugcraft:aeternalis_fuel')+value('minecraft:diamond_block'))
        self.assertEqual(value('jugcraft:klein_star_1'),value('jugcraft:dark_matter')+4*value('minecraft:gold_ingot')+4*value('minecraft:lapis_lazuli'))

    def test_smithing_upgrades_pay_for_base_addition_and_template(self):
        for result,base,addition in [('gem_helmet','red_matter_helmet','minecraft:diamond_block'),
                                     ('condenser_mk2','condenser_mk1','jugcraft:dark_matter'),
                                     ('klein_star_6','klein_star_5','jugcraft:dark_matter')]:
            self.assertEqual(value('jugcraft:'+result),value('jugcraft:'+base)+value(addition)+value('jugcraft:high_covalence_dust'))
            self.assertTrue(VALUES['jugcraft:'+result]['source'].startswith('recipe '))
