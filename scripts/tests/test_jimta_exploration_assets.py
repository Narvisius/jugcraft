"""Authored for the final combined Jimtafacts test run; no source programs are loaded."""
from pathlib import Path
import hashlib
import json
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT/'art/owner-library/originals/Oct23/Jimtafacts/applied energentics reborn by jimbo'
RES = ROOT/'src/main/resources'


class ExplorationAssets(unittest.TestCase):
    def test_original_exploration_pixels_are_identical(self):
        for path in ['item/meteorite_compass', 'block/tiny_tnt', 'block/mysterious_cube_top', 'block/mysterious_cube_side', 'block/mysterious_cube_bottom', 'block/mysterious_cube_core']:
            for suffix in ['.png', '.png.mcmeta']:
                src = SOURCE/f'assets/ae2/textures/{path}{suffix}'
                if not src.exists(): continue
                target = RES/f'assets/jugcraft/textures/jimtafacts/ae2/{path}{suffix}'
                self.assertEqual(hashlib.sha256(src.read_bytes()).digest(), hashlib.sha256(target.read_bytes()).digest(), str(src))

    def test_tiny_tnt_retains_source_shape_and_uvs(self):
        source = json.loads((SOURCE/'assets/ae2/models/block/tiny_tnt.json').read_text())
        runtime = json.loads((RES/'assets/jugcraft/models/jimtafacts/ae2/block/tiny_tnt.json').read_text())
        self.assertEqual(source['elements'], runtime['elements'])

    def test_compass_has_direction_and_fast_slow_spin_frames(self):
        model = json.loads((RES/'assets/jugcraft/items/jimta_meteorite_compass.json').read_text())['model']['models'][1]
        self.assertEqual('jugcraft:jimta_meteorite_direction', model['property'])
        self.assertEqual(128, len(model['entries']))
        for i, frame in enumerate(model['entries']):
            self.assertAlmostEqual(i/128, frame['threshold'])
            mesh = json.loads((RES/'assets/jugcraft/models'/f"{frame['model']['model'].split(':')[1]}.json").read_text())
            self.assertEqual(6, len(mesh['quads']))
            for quad in mesh['quads']:
                self.assertEqual(4, len(quad['v']))
                for vertex in quad['v']: self.assertEqual(8, len(vertex))

    def test_meteorites_keep_source_spacing_and_biome_scope(self):
        source = json.loads((SOURCE/'data/ae2/worldgen/structure_set/meteorite.json').read_text())
        native = json.loads((RES/'data/jugcraft/worldgen/structure_set/jimtafacts/meteorite.json').read_text())
        self.assertEqual(source['placement'], native['placement'])
        biome = json.loads((RES/'data/jugcraft/tags/worldgen/biome/jimtafacts/has_meteorites.json').read_text())
        self.assertEqual('#minecraft:is_overworld', biome['values'][0]['id'])
