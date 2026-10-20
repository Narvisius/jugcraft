"""Approved regional mixes, renewable timber/food routes and Alpine's independent switch."""
from pathlib import Path
import sys,unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
import agriculture as ag
import alpine,alpine_data,biomes,trees,trees_data,machines
from dry_country_textures import textures

def shares(spec):
    left=1.;out={}
    for name,p in spec['picks']: out[name]=left*p;left*=1-p
    out[spec['default']]=left
    return out

class DryCountryTests(unittest.TestCase):
    def test_exact_mixes_and_preserved_attempt_budgets(self):
        cases={
            'mediterranean_forest':([5,6],{'cypress':.35,'olive':.25,'olive_bush':.15,'umbrella_pine':.15,'lemon':.05,'orange':.05}),
            'heathland':([2,3],{'gorse':.6,'juniper_bush':.15,'umbrella_pine':.25}),
            'dryland':([2,3],{'pinyon':.45,'juniper':.3,'juniper_bush':.25}),
            'canyon':([2,3],{'pinyon':.4,'juniper':.35,'juniper_bush':.25}),
            'eucalyptus_forest':([8,9],{'eucalyptus':.42,'tree_fern':.05,'big_eucalyptus':.25,'giant_eucalyptus':.08,'wattle_bush':.2}),
            'gilded_shrubland':([3,4],{'wattle_bush':.7,'wattle':.3}),
            'wasteland':([1,1],{'dead_tree':.45,'dead_snag':.3,'dead_snag_bent':.1,'tamarisk':.15}),
            'oasis':([2,3],{'palm':.4,'small_palm':.15,'tamarisk':.1,'date_palm':.35}),
            'lush_desert':([1,2],{'desert_acacia':.45,'small_palm':.2,'joshua_tree':.35}),
            'xeric_shrubland':([1,2],{'desert_acacia':.45,'juniper':.25,'joshua_tree':.3})}
        for name,(count,mix) in cases.items():
            actual=shares(biomes.BIOMES[name]['trees']);expected={f'jugcraft:{k}_checked':v for k,v in mix.items()}
            self.assertEqual(biomes.BIOMES[name]['trees']['count'],count)
            self.assertEqual(actual.keys(),expected.keys(),name)
            for k,v in expected.items():self.assertAlmostEqual(actual[k],v,places=10,msg=name)
        self.assertIsNone(biomes.BIOMES['dune_beach']['trees'])
        self.assertIsNone(biomes.BIOMES['grassland']['trees'])

    def test_dry_river_keeps_tamarisk_beside_date_palms_without_underwater_trees(self):
        spec=biomes.BIOMES['dry_river']['trees'];self.assertEqual(spec['count'],[1,1])
        self.assertEqual(shares(spec),{'jugcraft:tamarisk_checked':.4,'jugcraft:date_palm_checked':.6})
        out={};trees_data.worldgen(Path('data'),lambda p,d:out.__setitem__(p.name,d))
        self.assertEqual(out['dry_river_empty.json']['placement'],[])
        self.assertEqual(trees.SHAPES['tamarisk']['survives_as'],'minecraft:dead_bush')

    def test_alpine_selector_and_independent_acquisition(self):
        self.assertEqual(alpine.TREES['count'],1)
        expected={'jugcraft:larch_checked':.5,'jugcraft:stone_pine_checked':.2,'minecraft:spruce_checked':.2,'jugcraft:juniper_bush_checked':.1}
        for k,v in expected.items():self.assertAlmostEqual(shares(alpine.TREES)[k],v,places=10)
        for tree in ('stone_pine','juniper','larch'):self.assertIn('alpine_spawn',ag.tree_switches(tree))
        self.assertEqual(ag.COOKING['roasted_pine_nuts']['switch'],['biomes','alpine_spawn'])
        self.assertEqual(ag.switch_of('roasted_pine_nuts'),['biomes','alpine_spawn'])
        for item in ('pine_nuts','roasted_pine_nuts'):self.assertIn('jugcraft:'+item,ag.SQUIRRELS['food'])
        out={};alpine_data.worldgen(Path('data'),lambda p,d:out.__setitem__(p.as_posix(),d))
        selector=out['data/jugcraft/worldgen/feature/alpine_spawn_trees.json']
        self.assertEqual(len(selector['features']),3)

    def test_wood_identity_renewability_and_food(self):
        for tree,wood,count in (('stone_pine','pine',6),('wattle','minecraft:acacia',1),('tamarisk','tamarisk',6),('joshua_tree','yucca',6)):
            self.assertEqual(ag.timber(tree),wood)
            farm=next(r for r in machines._tree_farm() if r['input']==f'jugcraft:{tree}_sapling')
            self.assertEqual(farm['output'],(wood if ':' in wood else 'jugcraft:'+wood)+'_log')
            self.assertEqual(farm['count'],count)
            self.assertIn([f'jugcraft:{tree}_sapling',1,1.,None],farm['byproducts'])
        for wood in ('tamarisk','yucca'):self.assertEqual(len(ag.wood_blocks(wood,wood.title())),9)
        for tree in ('stone_pine','wattle','joshua_tree'):self.assertNotIn(tree,ag.WOOD_SETS)
        self.assertEqual(ag.COOKING['roasted_pine_nuts']['input'],'pine_nuts')
        for item in ('pine_nuts','roasted_pine_nuts'):self.assertIn('jugcraft:squirrel_food',ag.ITEMS[item]['tags'])

    def test_art_is_native_size_with_distinct_fruit_stages_and_leaf_backing(self):
        art=textures()
        for img in art.values():self.assertEqual(img.size,(16,16))
        self.assertEqual(len({art['block','stone_pine_needles'+s].tobytes() for s in ('','_unripe','_ripe')}),3)
        wattle=art['block','wattle_leaves'];yellow={tuple(bytes.fromhex(s)) for s in ('997520','b98f2b','d3a844','dabc77')}
        self.assertLess(sum(p[:3] in yellow for p in wattle.getdata()),256*.65)
        tamarisk=art['block','tamarisk_leaves'];pink={tuple(bytes.fromhex(s)) for s in ('7e5a6a','a07888','c09aa8')}
        self.assertLessEqual(sum(p[:3] in pink for p in tamarisk.getdata()),256*.25)

if __name__=='__main__':unittest.main()
