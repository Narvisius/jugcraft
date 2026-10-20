"""Cross-PR regressions for orchard inputs, candy compatibility and processing routes."""
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
import agriculture
import petro

class ChemistryTreeIntegrationTests(unittest.TestCase):
    def test_all_three_new_or_retained_candy_flavours_survive(self):
        flavours=agriculture.CANDY['flavours']
        for name,item in [('orange','jugcraft:orange'),('sour','jugcraft:citric_acid'),
                          ('osmanthus','jugcraft:osmanthus_flowers')]:
            self.assertIn(name,flavours)
            self.assertIn(item,flavours[name]['items'])
        self.assertIn('jugcraft:orange',flavours['orange']['items'])
        self.assertIn('jugcraft:citric_acid',flavours['sour']['items'])

    def test_orchard_wood_and_walnut_switches_coexist(self):
        self.assertIn('agriculture',agriculture.WOOD_SWITCHES['fruitwood'])
        self.assertIn('biomes',agriculture.WOOD_SWITCHES['fruitwood'])
        self.assertIn('walnut',agriculture.WOOD_SETS)
        self.assertIn('biomes',agriculture.WOOD_SWITCHES['walnut'])

    def test_reactor_routes_have_unique_names_and_new_chemistry(self):
        recipes=petro.FLUID_RECIPES['chemical_reactor']
        names=[r['name'] for r in recipes]
        self.assertEqual(len(names),len(set(names)))
        for name in ['aspirin','salicylic_acid_birch','salicylic_acid_willow',
                     'boric_acid','citric_acid_lemon','citric_acid_orange','glucose_syrup',
                     'vinyl_acetate','aged_pine_planks','aged_maple_planks']:
            self.assertIn(name,names)

    def test_every_added_server_test_is_registered_once(self):
        data=json.loads((ROOT/'src/gametest/resources/fabric.mod.json').read_text())
        entries=data['entrypoints']['fabric-gametest']
        for name in ['AspirinGameTests','BoricAcidGameTests','SourCandyGameTests',
                     'PvacGlueGameTests','SiliconeGameTests','AgedWoodGameTests',
                     'WalnutTreeGameTests','WalnutWoodGameTests','OrchardWorldgenGameTests']:
            self.assertEqual(entries.count('io.github.jimbozoomer.jugcraft.test.'+name),1,name)
