from pathlib import Path
import json
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import menu
import menu_data
import feast_data


class FeastPresentationTests(unittest.TestCase):
    def test_meal_faces_use_opaque_texels_and_fit_their_collision(self):
        for name, spec in menu.all_placed().items():
            if spec[0] not in ('bowl', 'plate'):
                continue
            with self.subTest(meal=name):
                model = menu_data.dish_model(name)
                image = menu_data.icon(name)
                height = 6.5 if spec[0] == 'bowl' else 5.5
                for element in model['elements']:
                    self.assertTrue(all(a < b for a, b in zip(element['from'], element['to'])))
                    self.assertGreaterEqual(min(element['from']), 0)
                    self.assertLessEqual(element['to'][1], height)
                    self.assertLessEqual(max(element['to'][0], element['to'][2]), 16)
                    self.assertEqual(set(element['faces']), {'up','down','north','south','east','west'})
                    for face in element['faces'].values():
                        u0,v0,u1,v1 = face['uv']
                        self.assertEqual(int(u0), int(u1))
                        self.assertEqual(int(v0), int(v1))
                        self.assertEqual(image.getpixel((int(u0),int(v0)))[3], 255)
                saved = ROOT / 'src/main/resources/assets/jugcraft/models/block' / (name+'.json')
                self.assertEqual(json.loads(saved.read_text(encoding='utf-8')), model)

    def test_linen_cloth_only_has_end_drapes_at_exposed_ends(self):
        for part, expected_ends in [('single',2),('start',1),('middle',0),('end',1)]:
            model=feast_data.table_model(part,'linen')
            cloth=[e for e in model['elements'] if any(f['texture']=='#linen' for f in e['faces'].values())]
            self.assertEqual(len(cloth), 3+expected_ends)
            self.assertEqual(model['textures']['linen'], 'minecraft:block/white_wool')
            top=cloth[0]
            self.assertEqual(top['from'][0], 0)
            self.assertEqual(top['to'][0], 16)
            for element in cloth:
                self.assertGreaterEqual(min(element['from']), 0)
                self.assertLessEqual(max(element['to']),16)

    def test_every_table_axis_and_part_has_linen_model(self):
        root=ROOT/'src/main/resources/assets/jugcraft'
        states=json.loads((root/'blockstates/feast_table.json').read_text(encoding='utf-8'))['variants']
        for axis in ('x','z'):
            for part in feast_data.PARTS:
                target=states[f'axis={axis},part={part},runner=linen']['model'].split(':')[1]
                self.assertTrue((root/'models'/(target+'.json')).is_file())


if __name__ == '__main__':
    unittest.main()
