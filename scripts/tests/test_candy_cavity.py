"""Source geometry fidelity and repeatable resource generation, not a substitute for Minecraft tests."""
import hashlib
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import nouveau_nbt
import candy_cavity_content

DATA = ROOT / 'src/main/resources/data/jugcraft'


def template(name):
    return nouveau_nbt.read((DATA / 'structure' / (name + '.nbt')).read_bytes())[2]


class CandyCavityResources(unittest.TestCase):
    def test_lollipop_geometry_keeps_every_supplied_block_and_original_dimensions(self):
        for size, count in [('big', 5), ('small', 4)]:
            for variant in range(count):
                source = template(f'cavern_{size}_lollipop_top_{variant}')
                for color in range(6):
                    target = template(f'cavern/candy/{size}_lollipop_{variant}_{color}')
                    self.assertEqual(source['size'], target['size'])
                    self.assertEqual(source['blocks'], target['blocks'])
                    palette = [s['id'][1] for s in target['palette'][1][1]]
                    self.assertNotIn('jugcraft:cavern_rock_candy_black', palette)

    def test_sideways_bottle_markers_become_real_soda_without_remodeling(self):
        source, target = template('cavern_soda_bottle_side'), template('cavern/candy/soda_bottle')
        self.assertEqual(source['size'], target['size'])
        self.assertEqual(source['blocks'], target['blocks'])
        palette = [s['id'][1] for s in target['palette'][1][1]]
        self.assertIn('jugcraft:cavern_purple_soda', palette)
        self.assertNotIn('minecraft:purple_concrete', palette)
        self.assertNotIn('minecraft:orange_concrete', palette)
        mouths = [b for b in target['blocks'][1][1] if b['pos'][1][1][2] == 25 and palette[b['state'][1]] == 'jugcraft:cavern_purple_soda']
        self.assertEqual(3, len(mouths))

    def test_generator_is_repeatable_and_keeps_saved_structure_definitions(self):
        paths = list((DATA / 'structure/cavern/candy').glob('*.nbt'))
        paths += list((DATA / 'worldgen').rglob('cavern_*.json'))
        before = {p: hashlib.sha256(p.read_bytes()).digest() for p in paths}
        candy_cavity_content.main()
        self.assertEqual(before, {p: hashlib.sha256(p.read_bytes()).digest() for p in paths})
        for name in ['cake_cave', 'donut_arch', 'soda_bottle']:
            self.assertTrue((DATA / f'worldgen/structure/cavern_{name}.json').exists())
            self.assertTrue((DATA / f'worldgen/template_pool/cavern_{name}.json').exists())
            self.assertFalse((DATA / f'worldgen/structure_set/cavern_{name}.json').exists())

    def test_tower_decorated_pots_use_current_map_schema(self):
        pots = [b['nbt'][1] for b in template('cavern_licowitch_tower')['blocks'][1][1]
                if 'nbt' in b and 'sherds' in b['nbt'][1]]
        self.assertGreater(len(pots), 0)
        for pot in pots:
            self.assertEqual(10, pot['sherds'][0])
            self.assertTrue(set(pot['sherds'][1]).issubset({'back', 'left', 'right', 'front'}))


if __name__ == '__main__':
    unittest.main()
