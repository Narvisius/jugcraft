"""Generation budgets, dimensions, renewability and asset contracts for batches 9/9b."""
from pathlib import Path
import sys, unittest, json
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
import agriculture as ag, biomes, biomes_data, trees, fungal_trees_data
from fungal_trees_textures import textures

class FungalEndTreeTests(unittest.TestCase):
    def test_existing_end_budget_and_exact_species_shares(self):
        spec=biomes.BIOMES['ender_wilds']['trees']; self.assertEqual(spec['count'],[2,3])
        left=1.;actual={}
        for name,p in spec['picks']: actual[name]=left*p;left*=1-p
        actual[spec['default']]=left
        expected={'jugcraft:dragonblood_checked':.45,'jugcraft:dragonblood_shrub_checked':.2,'jugcraft:huge_glowcap':.05,'jugcraft:jacaranda_checked':.3}
        self.assertEqual(actual.keys(),expected.keys())
        for name,value in expected.items(): self.assertAlmostEqual(actual[name],value,places=10)
        spec=biomes.BIOMES['mycelial_jungle']['trees'];left=1.;shares={}
        for name,p in spec['picks']:shares[name]=left*p;left*=1-p
        self.assertAlmostEqual(shares['jugcraft:huge_glowcap'],.07)
        self.assertAlmostEqual(shares['jugcraft:mahogany_checked'],.7*.85*.15)

    def test_netheritic_layers_cave_floors_and_rare_stone_only_end_trees(self):
        for name,count in (('thicket_huge_glowcaps',2),('sands_blight_fungi',1),('hollow_blight_fungi',3),('marrow_fungi',1),('netherbrush_bushes',4),('sands_bramble_bushes',2)):
            self.assertEqual(biomes.EXTRAS[name]['placement'][0],{'type':'minecraft:count_on_every_layer','count':count})
        cave=biomes.EXTRAS['grotto_huge_glowcaps']['placement']
        self.assertEqual(cave[0]['count'],{'type':'minecraft:uniform','min_inclusive':2,'max_inclusive':4})
        self.assertEqual(cave[2],{'type':'jugcraft:cave_fungal_floor'})
        self.assertIn('minecraft:environment_scan',[p['type'] for p in cave]);self.assertNotIn('minecraft:heightmap',[p['type'] for p in cave])
        steps=biomes_data.steps('glowcap_grotto')[9]
        self.assertLess(steps.index('jugcraft:grotto_mud'),steps.index('jugcraft:grotto_huge_glowcaps'))
        self.assertLess(steps.index('jugcraft:grotto_huge_glowcaps'),steps.index('jugcraft:grotto_glowcaps'))
        outer=biomes.EXTRAS['outer_dragonblood']['placement']
        self.assertEqual(outer[0],{'type':'minecraft:rarity_filter','chance':10})
        self.assertEqual(outer[-1]['predicate']['blocks'],'minecraft:end_stone')

    def test_loot_cost_and_existing_crafting_systems(self):
        output={};fungal_trees_data.loot(Path('loot'),lambda p,d:output.__setitem__(p.stem,d))
        for block,chance in (('glowcap_block',.03),('blight_wart_block',.05),('bramble_bush_leaves',.06)):
            entry=output[block]['pools'][0]['entries'][0]['children'][1]
            self.assertEqual(entry['condition']['terms'][0]['chance'],chance)
        self.assertLess((109*.03*.65+9+1)/7,4) # includes all collectible stems and composter's free initial layer
        self.assertEqual(len(ag.wood_blocks('dragonblood','Dragonblood')),9)
        self.assertEqual(ag.tree_switches('dragonblood'),['biomes'])
        for block in ag.FUNGAL_BLOCKS:self.assertEqual(ag.switch_of(block),['biomes'])
        self.assertNotIn('marrow_fungus',ag.TREES); self.assertNotIn('marrow_sapling',ag.all_items())
        self.assertEqual(trees.SHAPES['dragonblood_shrub']['trunk']['base_height'],1)

    def test_native_art_and_preserved_owner_bramble(self):
        art=textures()
        for image in art.values():self.assertEqual(image.size,(16,16))
        from PIL import Image
        source=Path(__file__).resolve().parents[2]/'art/owner-library/originals/Blocks/biomes and tree blocks/bramble_leaves.png'
        self.assertEqual(art['block','bramble_bush_leaves'].tobytes(),Image.open(source).convert('RGBA').tobytes())
        self.assertGreater(sum(p[3]>0 for p in art['block','dragonblood_fronds'].getdata()),100)

if __name__=='__main__':unittest.main()
