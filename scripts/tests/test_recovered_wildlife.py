"""Regression coverage for the original-anatomy/UV import, not palette-based reconstruction."""
import json
from pathlib import Path
import sys
import unittest
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from recover_wildlife_models import geometry, number

ASSET = ROOT / 'src/main/resources/assets/jugcraft'
MODELS = ASSET / 'wildlife_models'


class RecoveredWildlifeTests(unittest.TestCase):
    def test_all_registered_wildlife_have_compatible_base_and_overlay_atlases(self):
        catalog = json.loads((ROOT / 'src/main/resources/jugcraft/menagerie/catalog.json').read_text())
        bindings = json.loads((MODELS / 'bindings.json').read_text())
        species = {s['id']: s for s in catalog['species'] if s['id'].startswith('wildlife_')}
        self.assertEqual(set(species), set(bindings))
        self.assertEqual(47, len(bindings))
        for name, binding in bindings.items():
            self.assertEqual({v['name'] for v in species[name]['variants']}, set(binding['variants']))
            for variant in binding['variants'].values():
                for pairing in variant.values():
                    model = json.loads((MODELS / (pairing['model'] + '.json')).read_text())
                    for texture in [pairing['skin'], pairing['dye']]:
                        if texture:
                            with Image.open(ASSET / 'textures' / (texture + '.png')) as image:
                                self.assertEqual(tuple(model['textureSize']), image.size, (name, pairing))
                    self.assertNotIn('/dye/', pairing['skin'], 'A partial dye overlay must never replace the base skin')

    def test_snail_retains_original_shell_eye_and_mirrored_uv_geometry(self):
        model = json.loads((MODELS / 'snail.json').read_text())
        self.assertEqual([64, 64], model['textureSize'])
        bones = {b['name']: b for b in model['parts']}
        self.assertEqual([4, 7, 7], bones['shell']['cubes'][0]['box'][3:])
        self.assertEqual([0, 0], bones['shell']['cubes'][0]['uv'])
        self.assertEqual([0.5, 0.5, 0.5], bones['shell']['cubes'][1]['inflate'])
        self.assertTrue(bones['leftEye']['cubes'][0]['mirror'])
        self.assertFalse(bones['rightEye']['cubes'][0]['mirror'])
        self.assertIn('walk', model['actions'])
        self.assertIn('idle', model['actions'])

    def test_all_part_hierarchies_and_animation_values_are_well_formed(self):
        for path in MODELS.glob('*.json'):
            if path.name == 'bindings.json': continue
            model = json.loads(path.read_text()); names = {'$mesh'}
            for bone in model['parts']:
                self.assertIn(bone['parent'], names, path.name)
                self.assertNotIn(bone['name'], names, path.name)
                names.add(bone['name'])
            for clip in model['actions'].values():
                self.assertGreaterEqual(clip['length'], 0, path.name)
                for channel in clip['channels']:
                    times = [f['time'] for f in channel['frames']]
                    self.assertEqual(sorted(times), times, path.name)
                    if clip['length'] == 0: self.assertTrue(all(t == 0 for t in times), path.name)
                    self.assertIn(channel['target'], ['POSITION', 'ROTATION', 'SCALE'])

    def test_data_parser_does_not_evaluate_unexpected_code(self):
        with self.assertRaises(ValueError): number('System.exit(0)')
        with self.assertRaises(ValueError):
            geometry('public static LayerDefinition createBodyLayer() { Runtime.getRuntime().exec("bad"); }')


if __name__ == '__main__': unittest.main()
