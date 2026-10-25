"""Source fidelity assertions prepared for the final combined Jimtafacts test batch."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import jimtafacts_network as network


class JimtafactsNetworkAssetsTests(unittest.TestCase):
    def test_original_pixels_and_animation_sidecars_are_identical(self):
        manifest = json.loads((ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/network-runtime-copies.json').read_text(encoding='utf-8'))
        images = [r for r in manifest if r['runtime'].endswith(('.png', '.mcmeta'))]
        self.assertGreater(len(images), 50)
        self.assertTrue(any(r['runtime'].endswith('.mcmeta') for r in images))
        for row in images:
            with self.subTest(row['runtime']): self.assertEqual((ROOT / row['source']).read_bytes(), (ROOT / row['runtime']).read_bytes())

    def test_original_mesh_coordinates_rotations_and_uvs_survive_platform_adapter(self):
        manifest = json.loads((ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/network-runtime-copies.json').read_text(encoding='utf-8'))
        for row in manifest:
            if '/models/' not in row['runtime']: continue
            if '/ae2/parts/' in row['source']:
                # These descriptors request a procedural PlanePartModel, not JSON element geometry.
                # The separate test below checks its recovered geometry and source texture choices.
                self.assertIn('/native/plane_', row['runtime'])
                continue
            original = json.loads((ROOT / row['source']).read_text(encoding='utf-8'))
            runtime = json.loads((ROOT / row['runtime']).read_text(encoding='utf-8'))
            self.assertEqual(len(original.get('elements', [])), len(runtime.get('elements', [])))
            for before, after in zip(original.get('elements', []), runtime.get('elements', [])):
                from jimtafacts_model_repairs import REPAIRED, CLOSED
                repaired=row['runtime'].split('/models/',1)[1].removesuffix('.json') in REPAIRED | CLOSED
                for key in ['from', 'to']:
                    if repaired:
                        self.assertTrue(all(abs(a-b)<=0.601 for a,b in zip(before[key],after[key])), row['runtime'])
                    else:self.assertEqual(before.get(key),after.get(key),row['runtime'])
                self.assertEqual(before.get('rotation'),after.get('rotation'),row['runtime'])
                for side, face in before.get('faces', {}).items():
                    if not repaired:self.assertEqual(face.get('uv'), after['faces'][side].get('uv'), row['runtime'])
                    elif 'uv' in face:self.assertTrue(all(abs(a-b)<=0.601 for a,b in zip(face['uv'],after['faces'][side]['uv'])),row['runtime'])

    def test_source_plane_panels_and_all_edge_permutations_have_native_models(self):
        assets = ROOT / 'src/main/resources/assets/jugcraft'
        for kind in ('formation_plane', 'annihilation_plane'):
            descriptor = json.loads((network.SOURCE / f'ae2/parts/{kind}.json').read_text(encoding='utf-8'))['model']['models'][1]
            for mode in ('on', 'off'):
                for mask in range(16):
                    model = json.loads((assets / f'models/jimtafacts/ae2/native/plane_{kind}_{mode}_{mask}.json').read_text(encoding='utf-8'))
                    self.assertEqual(len(model['elements']), 1)
                    panel = model['elements'][0]
                    self.assertEqual(panel['from'][2], 0)
                    self.assertEqual(panel['to'][2], 1)
                    self.assertEqual(model['textures']['front'], descriptor[f'front_{mode}'].replace('ae2:', 'jugcraft:jimtafacts/ae2/'))
                    self.assertEqual(panel['from'][:2], [0 if mask & 1 else 1, 0 if mask & 4 else 1])
                    self.assertEqual(panel['to'][:2], [16 if mask & 2 else 15, 16 if mask & 8 else 15])
            state = json.loads((assets / f'blockstates/jimta_{kind}.json').read_text(encoding='utf-8'))
            panels = [part for part in state['multipart'] if '/native/plane_' in part['apply']['model']]
            self.assertEqual(len(panels), 16 * 2 * 6)
            self.assertEqual({part['when']['facing'] for part in panels}, {'up','down','north','south','east','west'})

    def test_typed_p2p_model_does_not_render_its_inherited_chassis_twice(self):
        for kind in ('me','item','fluid','fe','redstone','light'):
            path = ROOT / f'src/main/resources/assets/jugcraft/blockstates/jimta_{kind}_p2p_tunnel.json'
            models = [part['apply']['model'] for part in json.loads(path.read_text(encoding='utf-8'))['multipart']]
            self.assertNotIn('jugcraft:jimtafacts/ae2/part/p2p_tunnel_base', models)
            self.assertIn(f'jugcraft:jimtafacts/ae2/part/p2p_tunnel_{kind}', models)

    def test_registered_items_have_definitions_and_building_shapes_have_blockstates(self):
        for name in network.ids():
            path = ROOT / f'src/main/resources/assets/jugcraft/items/{name}.json'
            self.assertTrue(path.is_file(), name)
            self.assertNotIn('ae2:', path.read_text(encoding='utf-8'), name)
        for name in network.BLOCKS:
            path = ROOT / f'src/main/resources/assets/jugcraft/blockstates/jimta_{name}.json'
            self.assertTrue(path.is_file(), name)
            self.assertNotIn('ae2:', path.read_text(encoding='utf-8'), name)

if __name__ == '__main__': unittest.main()
