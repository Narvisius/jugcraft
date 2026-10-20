"""Selection budgets, processing costs, timber identity, and exact editable artwork."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import agriculture as ag
import biomes
import machines
import tropical_tree_textures


class TreeBatchElevenTests(unittest.TestCase):
    def test_absolute_shares_and_existing_attempt_budgets(self):
        for name, count, target, expected in [
            ('tropics', [4, 5], 'flame_tree', .10), ('jacaranda_glade', [6, 7], 'flame_tree', .08),
            ('overgrown_beach', [1, 2], 'sea_grape', .60), ('karst_pinnacles', [6, 8], 'osmanthus', .15),
            ('shrine_springs', [2, 3], 'banyan', .14), ('lush_grassland', [2, 3], 'mango', .20),
        ]:
            d = json.loads((ROOT / f'src/main/resources/data/jugcraft/worldgen/feature/trees_{name}.json').read_text())
            remaining, shares = 1., {}
            for p in d['features']:
                shares[p['feature']] = remaining * p['chance']; remaining *= 1 - p['chance']
            shares[d['default']] = remaining
            self.assertAlmostEqual(shares['jugcraft:' + target + '_checked'], expected)
            self.assertEqual(biomes.BIOMES[name]['trees']['count'], count)
            # Banana shares are extended by Batch 12; this batch's flame-tree share remains unchanged.
            if name == 'shrine_springs': self.assertAlmostEqual(shares['jugcraft:great_oak_checked'], .56)
            if name == 'lush_grassland': self.assertAlmostEqual(shares['jugcraft:small_palm_checked'], .10)
        self.assertNotIn('pandanus', ag.TREES)
        for biome, rarity in [('dune_beach', 6), ('gravel_beach', 12)]:
            self.assertIsNone(biomes.BIOMES[biome]['trees'])
            self.assertEqual(biomes.EXTRAS[biome + '_driftwood']['placement'][0]['chance'], rarity)

    def test_processing_costs_and_timber_identity(self):
        self.assertEqual(ag.POT_RECIPES['sea_grape_jelly']['inputs'], {'jugcraft:mason_jar': 1, 'jugcraft:sea_grapes': 6, 'minecraft:sugar': 2})
        self.assertEqual(ag.POT_RECIPES['mango_chutney']['inputs']['jugcraft:cider_vinegar'], 1)
        self.assertEqual(ag.POT_RECIPES['osmanthus_mooncake']['count'], 4)
        self.assertEqual(ag.POT_RECIPES['osmanthus_mooncake']['inputs']['jugcraft:osmanthus_flowers'], 2)
        self.assertEqual(ag.CANDY['flavours']['osmanthus']['effect'], 'LUCK')
        for fruit in ['sea_grapes', 'mango']: self.assertIn('jugcraft:fermentable', ag.ITEMS[fruit]['tags'])
        self.assertIn('minecraft:parrot_food', ag.ITEMS['mango']['tags'])
        for tree, wood in [('flame_tree', 'minecraft:acacia'), ('sea_grape', 'minecraft:oak'), ('osmanthus', 'olive'), ('banyan', 'fig')]:
            self.assertEqual(ag.timber(tree), wood)
            self.assertNotIn(tree, ag.WOOD_SETS)
        self.assertNotIn('fig', ag.ITEMS)
        for wood in ['fig', 'mango']: self.assertEqual(len(ag.wood_blocks(wood, wood.title())), 9)
        recipes = {r['input']: r for r in machines._tree_farm()}
        self.assertEqual(recipes['jugcraft:banyan_sapling']['output'], 'jugcraft:fig_log')
        self.assertEqual(recipes['jugcraft:sea_grape_sapling']['count'], 1)

    def test_generated_art_matches_editable_source(self):
        from PIL import Image
        for (kind, name), image in tropical_tree_textures.textures().items():
            actual = Image.open(ROOT / f'src/main/resources/assets/jugcraft/textures/{kind}/{name}.png').convert('RGBA')
            self.assertEqual(image.size, (16, 16)); self.assertEqual(image.tobytes(), actual.tobytes())


if __name__ == '__main__': unittest.main()
