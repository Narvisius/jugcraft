"""Bulk stations keep current production routes and pay each recipe's actual energy cost."""
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
import industrial_forms as forms
import check_mod_data as audit
import machines
import petro
import gui_textures


class BulkFactoryIntegrationTests(unittest.TestCase):
    def test_bulk_stations_keep_all_current_recipes_and_named_tanks(self):
        for bulk, small in forms.BULK_VARIANTS.items():
            with self.subTest(bulk=bulk):
                self.assertEqual(forms.recipes_for_form(bulk), forms.recipes_for_form(small))
                self.assertEqual(forms.FORMS[bulk]['aliases'], forms.FORMS[small].get('aliases', []))
                self.assertIn(bulk, gui_textures.FORM_TAGLINES)
        self.assertIn('jugcraft:electronics_peroxide', forms.FORMS['bulk_electrolytic_separator']['capabilities'])
        self.assertIn('jugcraft:cobalt_synthesis', forms.FORMS['bulk_chemical_infuser']['capabilities'])
        self.assertFalse(forms.FORMS['bulk_electrolytic_separator']['upgrades'])

    def test_fuel_audits_apply_recipe_override_before_bulk_saving(self):
        info = dict(family='test', profile='BULK', upgrades=False, capabilities=['jugcraft:test'])
        recipe = dict(name='test', capability='jugcraft:test', fluids=[('minecraft:water', 1000)],
                      fluid_results=[('jugcraft:hydrogen', 100)], ticks=100, energy_per_tick=400)
        with patch.dict(forms.FORMS, {'test': info}, clear=True), \
             patch.dict(forms.FORM_RECIPES, {'test': [recipe]}, clear=True), \
             patch.dict(machines.STATS, {'test': {'use_per_tick': 100}}, clear=True), \
             patch.dict(petro.FLUID_RECIPES, {}, clear=True):
            self.assertEqual(audit.cheapest_electrolysis('jugcraft:hydrogen'), 320)
            self.assertEqual(audit.electrolysis_floor_breaches(), [])
            recipe['energy_per_tick'] = 200
            self.assertEqual(audit.cheapest_electrolysis('jugcraft:hydrogen'), 160)
            self.assertEqual(audit.electrolysis_floor_breaches(), [('test', 'test', 160)])

    def test_combined_graph_has_no_bulk_hydrogen_floor_breach(self):
        self.assertEqual(audit.electrolysis_floor_breaches(), [])

    def test_large_bio_generator_accepts_every_existing_bio_fuel(self):
        small = {fluid for fluid, _, _ in forms.FORMS['bio_generator']['fuels']}
        large = {fluid for fluid, _, _ in forms.FORMS['large_bio_generator']['fuels']}
        self.assertEqual(small, large)

    def test_all_generator_bonuses_remain_below_methanation_cost(self):
        with patch.object(audit, 'errors', []):
            audit.check_industrial_forms()
            self.assertEqual(audit.errors, [])
