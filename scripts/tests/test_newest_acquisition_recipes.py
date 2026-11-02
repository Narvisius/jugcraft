"""Keep restored acquisition recipes reproducible from the reviewed generator."""
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from newest_acquisition_recipes import build, build_rituals, build_rewards


class AcquisitionRecipesTests(unittest.TestCase):
    def test_singular_ribbit_chest_folder_is_imported(self):
        from newest_ribbits import chest_loot
        tables=chest_loot()
        self.assertEqual(6,len(tables))
        for name,data in tables.items():
            self.assertNotIn('"minecraft:air"',json.dumps(data))
            path=ROOT/'src/main/resources/data/jugcraft/loot_table'/(name+'.json')
            self.assertEqual(data,json.loads(path.read_text()))

    def test_rituals_fit_native_forge_limits_and_shipped_data(self):
        for name, data in build_rituals().items():
            with self.subTest(ritual=name):
                self.assertLessEqual(sum(i['amount'] for i in data['inputs']), 8)
                self.assertGreaterEqual(data['forge_tier'], 2)
                self.assertEqual({'id':'jugcraft:forbidden_'+name,'count':1},data['result']['result_item'])
                path=ROOT/'src/main/resources/data/jugcraft/arcane_ritual'/('acquisition_'+name+'.json')
                self.assertEqual(data,json.loads(path.read_text()))

    def test_reward_pools_are_idempotent_and_not_guaranteed(self):
        for name, data in build_rewards().items():
            pools=[p for p in data['pools'] if p.get('name','').startswith('acquisition_nov1_')]
            self.assertEqual(len(pools),len({p['name'] for p in pools}))
            path=ROOT/'src/main/resources/data/jugcraft/loot_table'/(name+'.json')
            self.assertEqual(data,json.loads(path.read_text()))
            for pool in pools:
                condition=pool['condition']
                if 'terms' in condition:condition=condition['terms'][0]
                self.assertGreater(condition['chance'],0)
                self.assertLess(condition['chance'],1)
    def test_shipped_recipes_match_reviewed_generator(self):
        for name, expected in build().items():
            with self.subTest(recipe=name):
                path = ROOT / 'src/main/resources/data/jugcraft/recipe' / (name + '.json')
                self.assertEqual(expected, json.loads(path.read_text(encoding='utf-8')))

    def test_restored_special_recipes_keep_their_runtime_constraints(self):
        rows = build()
        self.assertEqual('jugcraft:botanical_water_shaped', rows['jugtania/water_rod']['type'])
        for part in ('helmet', 'chestplate', 'leggings', 'boots'):
            self.assertEqual('jugcraft:botanical_armor_upgrade', rows['jugtania/terrasteel_' + part]['type'])
        self.assertEqual(['jugcraft:supps_trapped_present', 'minecraft:red_dye'],
                         rows['acquisition/supps_trapped_present_red']['ingredients'])


if __name__ == '__main__':
    unittest.main()
