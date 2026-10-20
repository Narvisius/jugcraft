"""Regression coverage for the seven-package shared recipe graph and fuel audit."""
from pathlib import Path
import sys, unittest, json
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
import check_mod_data as audit
import petro, industrial_forms, machines, generate_material_data as data

class TechSevenIntegrationTests(unittest.TestCase):
    def test_phase_cycle_is_not_a_free_fuel_source(self):
        def recipe(source,target,amount=1000):
            return dict(fluids=[(source,1000)],fluid_results=[(target,amount)],ticks=10)
        graph={'test':[recipe('minecraft:water','jugcraft:test_gas'),
            recipe('jugcraft:test_gas','jugcraft:test_liquid',250),
            dict(fluids=[('jugcraft:test_liquid',250)],fluid_results=[('jugcraft:test_gas',1000)],ticks=10)]}
        with patch.dict(petro.FLUID_RECIPES,graph,clear=True),patch.dict(industrial_forms.FORM_RECIPES,{},clear=True),patch.dict(machines.STATS,{'test':{'use_per_tick':100}},clear=True):
            self.assertEqual(audit.cheapest_electrolysis('jugcraft:test_gas'),1)
            self.assertEqual(audit.cheapest_electrolysis('jugcraft:test_liquid'),8)
            graph['test'].pop(0)
            self.assertIsNone(audit.cheapest_electrolysis('jugcraft:test_gas'))

    def test_shared_recipes_never_overwrite_another_id(self):
        files=list(industrial_forms.form_recipe_files(data.condition))
        root=Path(__file__).resolve().parents[2]/'src/main/resources/data/jugcraft/recipe'
        for kind,name,recipe in files:
            self.assertEqual(json.loads((root/kind/(name+'.json')).read_text()),recipe,(kind,name))
        ids=[(kind,name) for kind,name,r in files]
        self.assertEqual(len(ids),len(set(ids)))
        self.assertTrue(any(name=='prepare_zirconia' for kind,name in ids))
        self.assertTrue(any(name=='prepare_zirconia_precursor' for kind,name in ids))

    def test_phosphate_pulverizing_matches_single_input_codec(self):
        import json
        root=Path(__file__).resolve().parents[2]/'src/main/resources/data/jugcraft/recipe'
        rows=[json.loads(p.read_text()) for p in root.rglob('*phosphate*.json')]
        crushing=[r for r in rows if r.get('type')=='jugcraft:pulverizing']
        self.assertTrue(crushing)
        for row in crushing:
            self.assertEqual(row['ingredient'], 'jugcraft:phosphate')
            self.assertNotIn('inputs',row)

if __name__=='__main__':unittest.main()
