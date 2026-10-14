"""Reachability, material conservation, regional ore and supported process plumbing."""
from pathlib import Path
import sys,json,unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
import fluorochemistry as f
import fluorochemistry_models as models
from materials import MINERALS

class FluorochemistryTests(unittest.TestCase):
    def test_starter_construction_does_not_need_its_own_ptfe(self):
        for name,info in f.FORMS.items():
            inputs=info['recipe'][1].values()
            if name=='lined_fluorochemical_reactor':
                self.assertTrue({'jugcraft:ptfe_seal','jugcraft:ptfe_fitting','jugcraft:ptfe_liner'} <= set(inputs))
                self.assertEqual(info['profile'],'EXPANDED')
            else:self.assertFalse(any('ptfe' in item or 'fluor' in item for item in inputs))

    def test_volume_and_work_ledgers_never_create_free_liquid(self):
        for entries in [*f.RECIPES.values(),*f.EXTRA_RECIPES.values()]:
            for r in entries:
                self.assertGreater(r['ticks']*r['energy_per_tick'],0)
                self.assertLessEqual(sum(x[1] for x in r['fluid_results']),sum(x[1] for x in r['fluids'])+r.get('source',0),r['name'])

    def test_ptfe_extrusion_allocates_liner_mass_and_scrap(self):
        r=f.EXTRA_RECIPES['polymer_extruder'][0]
        # A liner uses three powder units; the fourth is the separately allocated offcut.
        self.assertEqual(r['items'],[('jugcraft:ptfe_paste_charge',4)])
        self.assertEqual(r['results'],[('jugcraft:ptfe_liner_blank',1),('jugcraft:ptfe_offcut',1)])
        self.assertEqual(f.EXTRA_RECIPES['polymer_reclaimer'][0]['items'],[('jugcraft:ptfe_offcut',4)])
        self.assertEqual(f.EXTRA_RECIPES['polymer_reclaimer'][0]['results'],[('jugcraft:ptfe_powder',1)])
        for r in f.EXTRA_RECIPES['polymer_molding_press']:
            self.assertTrue(r['results'][0][0].endswith('_blank'))

    def test_service_pipes_meet_body_and_match_live_ports(self):
        for name,factory in models.FACTORIES.items():
            body=factory();ports=[p for p in f.FORMS[name]['ports'] if p[1].startswith('FLUID')]
            self.assertEqual({(p[1],p[2]) for p in ports},set(models.PIPE_PATHS[name]))
            for points in models.PIPE_PATHS[name].values():
                for a,b in zip(points,points[1:]):self.assertEqual(sum(x!=y for x,y in zip(a,b)),1)
                self.assertTrue(any(all(e[0][i]<=points[-1][i]<=e[1][i] for i in range(3)) for e in body),(name,points[-1]))

    def test_ore_regions_and_owner_texture_are_explicit(self):
        ore=MINERALS['fluorite'];root=Path(__file__).resolve().parents[2]
        self.assertIn('minecraft:snowy_plains',ore['gen']['surface_biomes'])
        self.assertNotIn('minecraft:plains',ore['gen']['surface_biomes'])
        self.assertGreaterEqual(ore['gen']['size'],16)
        self.assertGreater(ore['gen']['outcrop_chance'],0)
        self.assertEqual((root/'art/owner-library/originals/Blocks/block_fluorite.png').read_bytes(),(root/'src/main/resources/assets/jugcraft/textures/block/fluorite_block.png').read_bytes())

    def test_shared_reactor_recipes_have_one_file_and_two_viewer_categories(self):
        import industrial_forms as forms
        import generate_material_data as data
        view={row['block']:row for row in forms.recipe_view()}
        starter=view['jugcraft:fluorocarbon_reactor']['recipes']
        advanced=view['jugcraft:lined_fluorochemical_reactor']['recipes']
        self.assertEqual(len(starter),2)
        self.assertEqual([r['fluids'] for r in starter],[r['fluids'] for r in advanced])
        self.assertEqual([r['energy_per_tick'] for r in advanced],[58,72])
        ids=[(kind,name) for kind,name,r in forms.form_recipe_files(data.condition)]
        self.assertEqual(len(ids),len(set(ids)))
        # Importing the aggregate must not append fluorochemical recipes to the standalone polymer table.
        import polymers
        self.assertEqual(len(polymers.RECIPES['polymer_extruder']),6)

if __name__=='__main__':unittest.main()
