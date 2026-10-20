"""Guard the inward-face migration against mirrored skins and sealed vessel mouths."""
import sys
import unittest
from itertools import product
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
from owner_visual_polish import face_panel, repair_mesh
from menagerie_content import ASSET,read
from model_writer import FACES

class OwnerVisualPolishTests(unittest.TestCase):
    def test_signed_faces_preserve_uv_corners_and_become_bounded_panels(self):
        # face_panel verifies all four UV corners while selecting the new orientation.
        for flips,face,rotation in product(product((False,True),repeat=3),FACES,(0,90,180,270)):
            e={'from':[9 if f else 2 for f in flips],'to':[2 if f else 9 for f in flips],
               'faces':{face:{'texture':'#skin','uv':[1,3,8,14],'rotation':rotation}}}
            result=face_panel(e,face)
            self.assertTrue(all(a<b for a,b in zip(result['from'],result['to'])))
            self.assertEqual(set(FACES),set(result['faces']))

    def test_basin_and_sap_bucket_still_have_recessed_open_mouths(self):
        for model,x,z,rim,floor in [('household/block/basin',8,8,16,13),('menagerie/ecology/block/spile_bucket',8,12,12,5)]:
            d=read(ASSET/f'models/{model}.json')
            surfaces=[e['to'][1] for e in d['elements'] if not e.get('rotation') and 'up' in e.get('faces',{}) and e['from'][0]<x<e['to'][0] and e['from'][2]<z<e['to'][2]]
            self.assertTrue(surfaces,model)
            self.assertLess(max(surfaces),rim-1,model+' mouth was capped')
            self.assertAlmostEqual(max(surfaces),floor,delta=.3)

    def test_polish_is_idempotent_on_hollow_bucket(self):
        import copy
        model=read(ASSET/'models/menagerie/ecology/block/spile_bucket.json')
        expected=copy.deepcopy(model)
        repair_mesh(model,'menagerie/ecology/block/spile_bucket')
        self.assertEqual(model,expected)

    def test_legacy_item_models_resolve_to_visible_geometry(self):
        for group,names in [('beast','end_pirate_ship_wheel end_pirate_anchor end_pirate_anchor_winch shield_of_the_deep transmutation_table'),('cavern','shot_gum raygun galena_gauntlet resistor_shield beholder copper_valve gobthumper siren_light')]:
            for name in names.split():
                ref=f'jugcraft:menagerie/{group}/item/{name}';seen=set()
                while ref.startswith('jugcraft:'):
                    self.assertNotIn(ref,seen);seen.add(ref)
                    d=read(ASSET/'models'/(ref.split(':',1)[1]+'.json'))
                    if d.get('elements'):break
                    ref=d.get('parent','')
                else:self.fail(name+' has no native geometry')

if __name__=='__main__':unittest.main()
