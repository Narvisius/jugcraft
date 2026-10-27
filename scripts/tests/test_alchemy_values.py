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
    def test_overworld_quartz_does_not_multiply_essence_by_mining_or_crushing(self):
        for ore in ('quartz_ore','deepslate_quartz_ore'):
            self.assertGreaterEqual(value('jugcraft:'+ore),4*value('minecraft:quartz'))
    def test_storage_crate_routes_cannot_create_essence(self):
        metals = ('bronze', 'steel', 'invar', 'stainless_steel', 'aluminum', 'titanium', 'tungsten')
        previous = 'minecraft:chest'
        for tier, metal in enumerate(metals):
            crate = 'jugcraft:' + metal + '_crate'
            plate = value('jugcraft:' + metal + '_plate')
            self.assertLessEqual(value(crate), value(previous) + 4 * plate)
            if tier:
                kit = 'jugcraft:' + metal + '_storage_crate_upgrade'
                self.assertLessEqual(value(crate), value('minecraft:chest') + 8 * plate)
                self.assertLessEqual(value(kit), value('minecraft:iron_nugget') + 4 * plate)
                self.assertLessEqual(value(crate), value(previous) + value(kit))
            previous = crate

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

    def test_chemistry_remainders_and_molding_do_not_create_essence(self):
        self.assertGreaterEqual(value('jugcraft:glucose_syrup'),
                                3*value('minecraft:sugar')+value('minecraft:glass_bottle'))
        self.assertGreaterEqual(value('jugcraft:borax'),2*value('jugcraft:boric_acid'))
        self.assertGreaterEqual(2*value('jugcraft:silicone'),3*value('jugcraft:gasket'))
        for stock in ('plastic_pellets','pvc_resin','hdpe_pellets'):
            self.assertGreaterEqual(value('jugcraft:'+stock),2*value('jugcraft:plastic_sheet'))

    def test_aged_wood_has_consistent_shaped_values(self):
        for wood in ('pine','maple'):
            plank=value('jugcraft:aged_'+wood+'_planks')
            self.assertEqual(plank,value('jugcraft:'+wood+'_planks'))
            self.assertEqual(6*value('jugcraft:aged_'+wood+'_slab'),3*plank)
            self.assertEqual(4*value('jugcraft:aged_'+wood+'_stairs'),6*plank)
