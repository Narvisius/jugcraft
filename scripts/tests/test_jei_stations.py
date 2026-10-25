"""Every bundled station recipe must reach the viewer without losing amounts or outputs."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import station_recipe_view as stations


class StationRecipes(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.view = {g['type']: g for g in stations.build()}

    def test_all_custom_station_recipes_preserve_data(self):
        root = stations.RES / 'data/jugcraft/recipe'
        for path in root.rglob('*.json'):
            data = json.loads(path.read_text(encoding='utf-8'))
            kind = data['type'].removeprefix('jugcraft:')
            if kind not in stations.STATIONS:
                continue
            key = path.relative_to(root).with_suffix('').as_posix()
            rows = [r for r in self.view[kind]['recipes'] if r['id'] == key]
            self.assertEqual(len(rows), 1, key)
            row = rows[0]
            self.assertEqual(row['outputs'], data.get('results', [data.get('result')]), key)
            self.assertEqual(row['inputs'], data.get('ingredients', [dict(ingredient=data.get('ingredient'), count=1)]), key)
            self.assertEqual(row['ticks'], data.get('time', data.get('cookingtime', 0)), key)
            self.assertEqual(row['tools'], [data['tool']] if 'tool' in data else [], key)

    def test_all_catalysts_are_registered_items(self):
        for group in self.view.values():
            for item in group['catalysts']:
                self.assertTrue((stations.RES / 'assets/jugcraft/items' / (item.split(':')[1] + '.json')).exists(), item)

    def test_generated_catalog_is_current(self):
        catalog = json.loads((stations.RES / 'assets/jugcraft/recipe_view.json').read_text(encoding='utf-8'))
        self.assertEqual(catalog['stations'], stations.build())
        types = {g['type'] for g in catalog['machines']}
        self.assertTrue({'enrichment', 'fuel_fabrication'} <= types)

    def test_imbuement_retains_pedestal_materials(self):
        row = next(r for r in self.view['nouveau_imbuement_chamber']['recipes'] if r['id'].endswith('imbuement_abjuration_essence'))
        self.assertEqual(len(row['inputs']), 1)
        self.assertEqual(len(row['tools']), 3)
        self.assertEqual(row['outputs'], [{'id': 'jugcraft:nouveau_abjuration_essence', 'count': 1}])

    def test_industrial_view_preserves_counted_batches_heat_and_byproducts(self):
        catalog = json.loads((stations.RES / 'assets/jugcraft/recipe_view.json').read_text(encoding='utf-8'))
        groups = {g['type']: g['recipes'] for g in catalog['machines']}
        for path in (stations.RES / 'data/jugcraft/recipe/jimtafacts_industry').glob('*.json'):
            recipe = json.loads(path.read_text(encoding='utf-8'))
            kind = recipe['type'].removeprefix('jugcraft:')
            if not kind.startswith('jimta_'): continue
            matches = [r for r in groups[kind]
                       if r['in'] == [[recipe['ingredient'], recipe.get('input_count', 1)]]
                       and r['out'] == [recipe['result']['id'], recipe['result'].get('count', 1)]]
            self.assertTrue(matches, path.name)
            self.assertTrue(any(r['minimum_heat'] == recipe.get('minimum_heat', 0)
                and r['ticks'] == recipe['time']
                and r['extra'] == [[b['result']['id'], b['result'].get('count', 1), b['chance']]
                                  for b in recipe.get('byproducts', [])] for r in matches), path.name)

    def test_no_unaccounted_custom_processor_type(self):
        catalog = json.loads((stations.RES / 'assets/jugcraft/recipe_view.json').read_text(encoding='utf-8'))
        covered = {'jugcraft:' + g['type'] for group in ('machines', 'fluid_machines', 'stations') for g in catalog[group]}
        # Non-processor crafting serializers and the workshop's per-station categories.
        covered |= {'jugcraft:' + x for x in ('starter_working', 'gun_attachment', 'gun_attachment_removal',
            'key_copying', 'beast_bison_upgrade', 'beast_mimicream_repair', 'household_door_mat_copy',
            'jimta_cable_anchors', 'jimta_facade', 'jimta_compact_part')}
        for path in (stations.RES / 'data/jugcraft/recipe').rglob('*.json'):
            kind = json.loads(path.read_text(encoding='utf-8'))['type']
            if kind.startswith('jugcraft:'):
                self.assertIn(kind, covered, str(path))


if __name__ == '__main__':
    unittest.main()
