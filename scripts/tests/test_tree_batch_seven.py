"""Absolute olive placement shares, evergreen fruit separation and reachable processing."""
from pathlib import Path
import sys
import unittest
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import agriculture as ag
import biomes
import trees
import trees_data
import machines
import olive_textures


def shares(name):
    spec = biomes.BIOMES[name]['trees']; left = 1.; result = {}
    for feature, chance in spec['picks']:
        result[feature] = left * chance; left *= 1 - chance
    result[spec['default']] = left
    return result


class OliveTests(unittest.TestCase):
    def test_absolute_mixes_and_existing_citrus_shares(self):
        expected = {
            'mediterranean_forest': ([5,6], {'jugcraft:olive_checked':.25, 'jugcraft:olive_bush_checked':.15,
                'jugcraft:umbrella_pine_checked':.15, 'jugcraft:lemon_checked':.05, 'jugcraft:orange_checked':.05,
                'jugcraft:cypress_checked':.35}),
            'lavender_field': ([1,2], {'jugcraft:olive_bees_checked':.3, 'minecraft:oak_bees_002':.1,
                'jugcraft:cypress_checked':.1, 'jugcraft:jacaranda_checked':.5}),
            'scrubland': ([1,2], {'jugcraft:olive_bush_checked':.6, 'jugcraft:oak_bush_checked':.25, 'jugcraft:juniper_checked':.15}),
        }
        for biome, (count, mix) in expected.items():
            self.assertEqual(biomes.BIOMES[biome]['trees']['count'], count)
            self.assertEqual(shares(biome).keys(), mix.keys())
            for name, value in mix.items(): self.assertAlmostEqual(shares(biome)[name], value, places=10)
        self.assertFalse(biomes.BIOMES['lavender_field']['winter_snow'])
        for biome, spec in biomes.BIOMES.items():
            if not spec.get('trees'): continue
            keys = shares(biome)
            self.assertNotIn('jugcraft:ancient_olive_checked', keys)
            if biome not in expected: self.assertFalse(any('olive' in key for key in keys), biome)

    def test_wild_bush_has_no_fruit_state_loot_or_cultivated_sapling(self):
        self.assertNotIn('fruit', ag.TREES['olive_bush'])
        self.assertIsNone(ag.TREES['olive']['season'])
        self.assertEqual(ag.TREES['olive']['giant'], 'ancient_olive')
        self.assertEqual(trees.SHAPES['olive_bees']['decorators'], [{'type':'minecraft:beehive','probability':.05}])
        out = {}; trees_data.assets(Path('assets'), lambda p,d: out.__setitem__(p.as_posix(), d))
        self.assertEqual(set(out['assets/blockstates/olive_leaves.json']['variants']), {'fruit=0','fruit=1','fruit=2'})
        self.assertEqual(set(out['assets/blockstates/wild_olive_leaves.json']['variants']), {''})
        loot = {}; trees_data.loot(Path('loot'), lambda p,d: loot.__setitem__(p.name, d))
        self.assertNotIn('jugcraft:olives', str(loot['wild_olive_leaves.json']))
        self.assertIn('jugcraft:olive_bush_sapling', str(loot['wild_olive_leaves.json']))
        self.assertNotIn('jugcraft:olive_sapling', str(loot['wild_olive_leaves.json']))

    def test_preserving_and_wood_routes(self):
        recipe = ag.POT_RECIPES['pickled_olives']
        self.assertEqual(recipe, {'inputs':{'jugcraft:mason_jar':1,'jugcraft:olives':4,'jugcraft:cider_vinegar':1},'time':300})
        self.assertEqual(len(ag.wood_blocks('olive','Olive')),9)
        for tree, count in (('olive',6),('olive_bush',1)):
            farm=next(r for r in machines._tree_farm() if r['input']==f'jugcraft:{tree}_sapling')
            self.assertEqual(farm['output'],'jugcraft:olive_log')
            self.assertEqual(farm['count'],count)
            self.assertIn([f'jugcraft:{tree}_sapling',1,1.,None],farm['byproducts'])

    def test_fruit_is_visible_and_evergreen_art_is_dense(self):
        art=olive_textures.textures()
        for img in art.values(): self.assertEqual(img.size,(16,16))
        self.assertGreater(sum(a>0 for a in art['block','olive_leaves'].getchannel('A').tobytes()),220)
        self.assertEqual(len({art['block',n].tobytes() for n in ('olive_leaves','olive_leaves_unripe','olive_leaves_ripe')}),3)

if __name__ == '__main__': unittest.main()
