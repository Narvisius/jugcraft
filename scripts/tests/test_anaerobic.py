"""Cross-process material and power constraints for the renewable farm branch."""
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
import anaerobic as a

class AnaerobicBalanceTests(unittest.TestCase):
    def test_every_station_has_a_discoverable_guide_with_its_crafting_bill(self):
        import handbook
        pages = [page for chapter in handbook.build()['chapters'] for page in chapter['pages']]
        for name in a.FORMS:
            matches = [page for page in pages if page.get('icon') == 'jugcraft:'+name]
            self.assertEqual(len(matches),1,name)
            self.assertTrue(matches[0].get('craft'),name)
            self.assertTrue(matches[0].get('text'),name)

    def test_complete_plant_closes_its_residue_but_needs_external_feed_and_water(self):
        dig=a.RECIPES['anaerobic_digester'][0];clean=a.RECIPES['biogas_cleaner'][0];press=a.RECIPES['digestate_separator'][0]
        self.assertEqual(dig['fluid_results'][1][1]+clean['fluid_results'][1][1],press['fluids'][0][1])
        self.assertLess(press['fluid_results'][0][1],dig['fluids'][0][1]+clean['fluids'][1][1])
        self.assertTrue(set(a.FEEDSTOCK).isdisjoint({'jugcraft:fertilizer','jugcraft:digestate_fiber','minecraft:bone_meal'}))
        self.assertEqual(dig['fluid_results'][0][1],clean['fluids'][0][1])
        self.assertEqual(clean['fluid_results'][0][1],clean['fluids'][0][1])

    def test_upgrading_preserves_methane_and_converts_only_co2(self):
        r=a.UPGRADING;gas=r['fluids'][0][1];hydrogen=r['fluids'][1][1]
        self.assertEqual(hydrogen,gas*.4*4)
        self.assertEqual(r['fluid_results'][0][1],gas*.6+gas*.4)
        # Match the existing Infuser's game CO2 conversion convention, not physical volume claims.
        self.assertEqual(r['fluid_results'][1][1],gas*.4*4)
        self.assertEqual(r['tool'],'jugcraft:nickel_catalyst_bed')

    def test_no_machine_requires_its_own_biological_outputs_to_build(self):
        forbidden={'jugcraft:'+x for x in [*a.FORMS,*a.ITEMS,*a.GASES,*a.FLUIDS]}
        for info in a.FORMS.values():
            self.assertTrue(set(info['recipe'][1].values()).isdisjoint(forbidden))
        fuels=a.FORMS['bio_generator']['fuels']
        self.assertEqual({x[0] for x in fuels},{'jugcraft:clean_biogas','jugcraft:bioethanol'})
        self.assertTrue(all(rate<128 for _,_,rate in fuels))

if __name__=='__main__':unittest.main()
