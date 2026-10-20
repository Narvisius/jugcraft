"""Rainforest selection budgets, canonical routes and reproducible fern/rubber artwork."""
import json
from pathlib import Path
import sys
import unittest
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import agriculture as ag
import biomes
import machines
import rainforest_biomes
import rainforest_textures


class TreeBatchTwelveTests(unittest.TestCase):
    def test_biome_shares_and_attempt_budgets(self):
        counts = {'rainforest': [24, 28], 'tropics': [4, 5], 'lush_grassland': [2, 3],
                  'eucalyptus_forest': [8, 9], 'temperate_rainforest': [11, 12]}
        for biome, targets in rainforest_biomes.SHARES.items():
            d = json.loads((ROOT / f'src/main/resources/data/jugcraft/worldgen/feature/trees_{biome}.json').read_text())
            remaining, shares = 1., {}
            for p in d['features']:
                shares[p['feature']] = remaining * p['chance']; remaining *= 1 - p['chance']
            shares[d['default']] = remaining
            self.assertEqual(set(shares), {ref for ref, _ in targets})
            for ref, share in targets: self.assertAlmostEqual(shares[ref], share)
            self.assertEqual(biomes.BIOMES[biome]['trees']['count'], counts[biome])

    def test_existing_materials_and_no_fabricated_wood(self):
        self.assertNotIn('tree_fern', ag.WOOD_SETS)
        self.assertNotIn('rubber_tree', ag.WOOD_SETS)
        self.assertNotIn('banana', ag.TREES)
        self.assertEqual(ag.POT_RECIPES['rubber']['inputs'], {'jugcraft:latex_bottle': 1, 'jugcraft:cider_vinegar': 1})
        saw = {r['input']: r for r in machines._sawmill()}
        self.assertEqual(saw['jugcraft:rubber_log']['output'], 'minecraft:jungle_planks')
        self.assertNotIn('jugcraft:tree_fern_trunk', saw)
        for name in ['rubber_log', 'tree_fern_trunk']:
            self.assertFalse((ROOT / f'src/main/resources/data/jugcraft/recipe/{name}_charcoal.json').exists())
        self.assertEqual(ag.trunk_block('rubber_tree'), 'jugcraft:rubber_log')

    def test_latex_processing_cannot_mint_essence_through_returned_bottles(self):
        from fractions import Fraction
        values = json.loads((ROOT / 'src/main/resources/data/jugcraft/alchemy/values.json').read_text())['items']
        def value(item):
            v = values[item]; return Fraction(v['numerator'], v['denominator'])
        self.assertGreaterEqual(value('jugcraft:latex_bottle'), value('minecraft:glass_bottle'))
        self.assertGreaterEqual(value('jugcraft:latex_bottle') + value('jugcraft:cider_vinegar'),
                                value('jugcraft:rubber') + 2 * value('minecraft:glass_bottle'))

    def test_artwork_matches_editable_source(self):
        from PIL import Image
        for (kind, name), img in rainforest_textures.textures().items():
            actual = Image.open(ROOT / f'src/main/resources/assets/jugcraft/textures/{kind}/{name}.png').convert('RGBA')
            self.assertEqual(img.size, (16, 16)); self.assertEqual(img.tobytes(), actual.tobytes())

    def test_tap_fill_models_and_upgrade_recipe_match_generators(self):
        import rainforest_data, model_writer
        assets = ROOT / 'src/main/resources/assets/jugcraft'
        generated = {}
        def capture(path, data):
            if isinstance(data, dict) and data.get('elements'):
                model_writer.finish_elements(data['elements'])
            generated[path] = data
        rainforest_data.assets(assets, capture, {})
        rainforest_data.loot(ROOT / 'src/main/resources/data/jugcraft/loot_table/blocks', capture)
        recipe = json.loads((ROOT / 'src/main/resources/data/jugcraft/recipe/tree_tap_bucket.json').read_text())
        self.assertEqual(recipe['key'], {'T': 'jugcraft:tree_tap', 'B': 'minecraft:bucket'})
        self.assertEqual(recipe['result']['components'], {'minecraft:block_state': {'upgraded': 'true'}})
        for path, expected in generated.items():
            self.assertEqual(json.loads(path.read_text(encoding='utf8')), expected, str(path))
        variants = generated[assets / 'blockstates/tree_tap.json']['variants']
        self.assertEqual(len(variants), 4 * 33 * 2)
        heights = []
        for fill in range(1, 33):
            model = generated[assets / f'models/block/tree_tap_bucket_{fill}.json']
            latex = [e for e in model['elements'] if any(f.get('texture') == '#latex' for f in e['faces'].values())]
            self.assertEqual(len(latex), 2)
            self.assertEqual(latex[0]['to'][1], latex[1]['to'][1], 'Front gauge and contents disagree')
            heights.append(latex[0]['to'][1])
        self.assertTrue(all(a < b for a, b in zip(heights, heights[1:])), 'Latex surface must rise at each fill step')
