import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import art_check
import imported_geometry_repairs as repairs
import newest_original_art


class ImportedGeometryTests(unittest.TestCase):
    def test_wall_join_exception_does_not_hide_exterior_holes(self):
        path=repairs.MODELS/'newest/jugtania/block/livingwood_wall_side.json'
        box={'from':[5,0,0],'to':[11,14,8],
             'faces':{f:{'texture':'#wall'} for f in ('north','east','west','up','down')}}
        self.assertTrue(newest_original_art.intentional_wall_join(path,[box],[(0,'south')]))
        self.assertFalse(newest_original_art.intentional_wall_join(path,[box],[(0,'south'),(0,'west')]))
        self.assertFalse(newest_original_art.intentional_wall_join(repairs.MODELS/'other.json',[box],[(0,'south')]))
        box['to'][2]=7
        self.assertFalse(newest_original_art.intentional_wall_join(path,[box],[(0,'south')]))
    def test_patches_are_bounded_and_keep_original_materials(self):
        for ref, record in json.loads(repairs.MANIFEST.read_text()).items():
            model = json.loads((repairs.MODELS / ref).read_text())
            self.assertEqual(record['after_sha256'], repairs.digest(model['elements']), ref)
            restored = copy.deepcopy(model['elements'])
            for index, change in record['elements'].items():
                old, new = change['before'], change['after']
                restored[int(index)] = old
                self.assertEqual(old.get('rotation'), new.get('rotation'), ref)
                for corner in ('from', 'to'):
                    self.assertTrue(all(abs(a-b) <= .301 for a,b in zip(old[corner],new[corner])),ref)
                for face, material in old['faces'].items():
                    for key, value in material.items():
                        self.assertEqual(value, new['faces'][face][key], (ref,face,key))
            self.assertEqual(record['before_sha256'],repairs.digest(restored),ref)

    def test_exact_repair_rejects_drift_and_is_idempotent(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            records=json.loads(repairs.MANIFEST.read_text())
            for ref in records:
                path=root/ref;path.parent.mkdir(parents=True,exist_ok=True)
                path.write_text((repairs.MODELS/ref).read_text())
            self.assertEqual(0,repairs.apply(root))
            path=root/next(iter(records));model=json.loads(path.read_text())
            model['elements'][0]['from'][0] += 1
            path.write_text(json.dumps(model))
            with self.assertRaises(ValueError):repairs.apply(root)

    def test_rotated_overlay_requires_identical_world_quads(self):
        box={'from':[2,2,2],'to':[6,6,6], 'rotation':{'axis':'y','angle':45,'origin':[4,4,4]},
             'faces':{'north':{'texture':'a'}}}
        other=copy.deepcopy(box)
        faces,_=art_check._model_faces([box,other],lambda ref:None)
        self.assertTrue(art_check._layered([box,other],*faces))
        other['rotation']['angle']=-45
        faces,_=art_check._model_faces([box,other],lambda ref:None)
        self.assertFalse(art_check._layered([box,other],*faces))

    def test_rotated_occluder_does_not_cover_its_empty_aabb_corners(self):
        all_faces={name:{'texture':'a'} for name in art_check._NORMAL}
        cover={'from':[3,0,3],'to':[5,6,5],'faces':all_faces,
               'rotation':{'axis':'y','angle':45,'origin':[4,3,4]}}
        inside={'from':[3.8,1,3.8],'to':[4.2,2,4.2],
                'faces':{k:v for k,v in all_faces.items() if k!='up'}}
        outside={'from':[5,1,5],'to':[5.3,2,5.3], 'faces':inside['faces']}
        with patch.object(art_check,'_opaque',return_value=True):
            for box,expected in ((inside,False),(outside,True)):
                elements=[box,cover];faces,_=art_check._model_faces(elements,lambda ref:None)
                self.assertEqual(expected,(0,'up') in art_check._open_faces(elements,faces))

    def test_ordinary_item_models_do_not_depend_on_legacy_entity_renderers(self):
        from functools import lru_cache
        root=Path(__file__).resolve().parents[2]/'src/main/resources/assets/jugcraft'
        @lru_cache(maxsize=None)
        def legacy(ref):
            if ref in ('builtin/entity','minecraft:builtin/entity'):return True
            if not ref.startswith('jugcraft:'):return False
            path=root/'models'/(ref.split(':',1)[1]+'.json')
            if not path.exists():return False
            return legacy(json.loads(path.read_text(encoding='utf-8')).get('parent',''))
        def ordinary(value):
            if isinstance(value,dict):
                if value.get('type')=='minecraft:model':yield value['model']
                for child in value.values():yield from ordinary(child)
            elif isinstance(value,list):
                for child in value:yield from ordinary(child)
        bad=[]
        for path in (root/'items').glob('*.json'):
            for ref in ordinary(json.loads(path.read_text(encoding='utf-8'))):
                if legacy(ref):bad.append((path.name,ref))
        self.assertEqual([],bad,'Ordinary items cannot use a removed entity renderer')

    def test_runtime_material_fixture_matches_resolved_models(self):
        import model_atlas
        fixture=Path(__file__).resolve().parents[2]/'src/gametest/resources/imported-atlas-required.json'
        self.assertEqual(json.loads(fixture.read_text()), sorted(model_atlas.drawn_references()))

    def test_flat_cap_covers_only_its_actual_opaque_surface(self):
        face={'texture':'a'}
        box={'from':[4,0,4],'to':[8,2,8],
             'faces':{k:face for k in art_check._NORMAL if k!='up'}}
        cap={'from':[4,2,4],'to':[8,2,8],'faces':{'up':face}}
        with patch.object(art_check,'_opaque',return_value=True), patch.object(art_check.Face,'texel',return_value=(0,0,0,255)):
            elements=[box,cap];faces,_=art_check._model_faces(elements,lambda ref:None)
            self.assertNotIn((0,'up'),art_check._open_faces(elements,faces))
            with patch.object(art_check.Face,'texel',return_value=(0,0,0,0)):
                self.assertIn((0,'up'),art_check._open_faces(elements,faces))
            cap['to'][0]=6
            faces,_=art_check._model_faces(elements,lambda ref:None)
            self.assertIn((0,'up'),art_check._open_faces(elements,faces))
            cap['to'][0]=8;cap['faces']={'down':face}
            faces,_=art_check._model_faces(elements,lambda ref:None)
            self.assertIn((0,'up'),art_check._open_faces(elements,faces))


if __name__ == '__main__':unittest.main()
