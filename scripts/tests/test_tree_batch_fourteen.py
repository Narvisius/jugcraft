"""Batch 14 regression: absolute selector shares, stable crop routes and reproducible foliage."""
import json
from pathlib import Path
import sys
import unittest
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import agriculture as ag
import biomes
import orchard

class TreeBatchFourteenTests(unittest.TestCase):
    def data(self, path):
        return json.loads((ROOT / 'src/main/resources/data' / path).read_text())

    def test_absolute_species_shares_preserve_other_trees_and_density(self):
        targets = {
            'orchard': {'apple_tree_bees': .20, 'azalea_tree': .08, 'pear': .15, 'peach': .10, 'plum': .06912, 'chestnut': .40088},
            'subtropics': {'live_oak': .30, 'small_palm': .126, 'tall_vine_oak': .0504, 'lemon': .06804, 'orange': .10, 'azalea_tree': .35556},
        }
        for biome, expected in targets.items():
            config = self.data(f'jugcraft/worldgen/feature/trees_{biome}.json')
            remaining, shares = 1., {}
            for entry in config['features']:
                shares[entry['feature']] = remaining * entry['chance']
                remaining *= 1 - entry['chance']
            shares[config['default']] = remaining
            self.assertEqual(set(shares), {'jugcraft:' + tree + '_checked' for tree in expected})
            for tree, share in expected.items(): self.assertAlmostEqual(shares['jugcraft:' + tree + '_checked'], share)
            self.assertEqual(biomes.BIOMES[biome]['trees']['count'], [2, 3])
        config = self.data('jugcraft/worldgen/feature/trees_mediterranean_forest.json')
        remaining=1.
        for entry in config['features']:
            if entry['feature'] in ('jugcraft:lemon_checked', 'jugcraft:orange_checked'):
                self.assertAlmostEqual(remaining * entry['chance'], .05)
            remaining *= 1 - entry['chance']

    def test_bee_decorator_does_not_change_existing_apple_sapling(self):
        apple=self.data('jugcraft/worldgen/feature/apple_tree.json')
        bees=self.data('jugcraft/worldgen/feature/apple_tree_bees.json')
        self.assertEqual(apple['decorators'], [])
        self.assertEqual(bees.pop('decorators'), [{'type': 'minecraft:beehive', 'probability': .02}])
        apple.pop('decorators'); self.assertEqual(apple, bees)

    def test_reuse_existing_wood_and_safe_processing_routes(self):
        self.assertEqual(orchard.log('pear'), 'jugcraft:fruitwood_log')
        self.assertEqual(orchard.log('peach'), 'minecraft:cherry_log')
        self.assertEqual(set(ag.WOOD_SWITCHES['fruitwood']), {'biomes', 'agriculture'})
        for fruit in ['pear', 'peach', 'lemon', 'orange']:
            self.assertNotIn(fruit, ag.WOOD_SETS)
            self.assertIn('jugcraft:' + fruit, self.data('jugcraft/tags/item/fermentable.json')['values'])
        self.assertEqual(self.data('jugcraft/tags/item/cider_apples.json')['values'], ['minecraft:apple', 'jugcraft:pear'])
        self.assertEqual(self.data('jugcraft/tags/item/candy_flavours/orange.json')['values'], ['jugcraft:orange'])
        self.assertEqual(ag.CANDY['flavours']['orange']['seconds'], 15)
        for tree in ['pear','peach','lemon','orange']: self.assertEqual(orchard.TREES[tree]['rarity'],16)

    def test_generated_foliage_matches_source_and_keeps_transparency(self):
        from PIL import Image
        import cider_textures, orchard_textures
        images = {('block', 'apple_leaves' + suffix): cider_textures.leaves(stage)
                  for stage,suffix in enumerate(['','_blossom','_ripe'])}
        images.update({key: image for key,image in orchard_textures.orchard_textures().items()
                       if (key[0]=='block' and any(key[1].startswith(tree+'_') for tree in ['pear','peach','lemon','orange'])) or key[0]=='item'})
        for (kind,name), expected in images.items():
            actual=Image.open(ROOT/f'src/main/resources/assets/jugcraft/textures/{kind}/{name}.png').convert('RGBA')
            self.assertEqual(actual.tobytes(),expected.tobytes(),name)
            self.assertEqual(actual.size,(16,16))
            if 'leaves' in name:
                alpha=actual.getchannel('A').tobytes()
                self.assertIn(0,alpha);self.assertIn(255,alpha)

if __name__=='__main__': unittest.main()
