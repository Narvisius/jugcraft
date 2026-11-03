"""Imported open seams require exact source geometry and the real companion model composition."""
from copy import deepcopy
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import model_writer
import art_check
import newest_multipart_art as art
import newest_model_finishing as finishing


class MultipartArtTests(unittest.TestCase):
    def elements(self, relative):
        path = art.MODELS / relative
        return path, art.resolve(path, art.ASSETS / 'models', 'jugcraft')

    def test_native_wall_seam_requires_source_geometry_and_complete_multipart_mapping(self):
        path, elements = self.elements('jugtania/block/livingrock_wall_side.json')
        self.assertEqual({(0, 'south')}, art.intentional_open_faces(path, elements, [(0, 'south')]))
        mutated = deepcopy(elements)
        del mutated[0]['faces']['east']
        self.assertFalse(art.intentional_open_faces(path, mutated, [(0, 'south'), (0, 'east')]))
        mutated = deepcopy(elements)
        mutated[0]['to'][2] = 7
        self.assertFalse(art.intentional_open_faces(path, mutated, [(0, 'south')]))
        original_read = art.read
        state = art.ASSETS / 'blockstates/jugtania_livingrock_wall.json'
        def missing_post(target):
            data = original_read(target)
            if target == state:
                data['multipart'] = [p for p in data['multipart'] if p.get('when') != {'up': 'true'}]
            return data
        with patch.object(art, 'read', side_effect=missing_post):
            self.assertFalse(art.intentional_open_faces(path, elements, [(0, 'south')]))

    def test_moved_wall_post_cannot_cover_source_arm_by_assumption(self):
        path, elements = self.elements('jugtania/block/livingrock_wall_side_tall.json')
        self.assertEqual({(0, 'south')}, art.intentional_open_faces(path, elements, [(0, 'south')]))
        original_read = art.read
        post = art.MODELS / 'jugtania/block/shapes/wall_post.json'
        def moved_post(target):
            data = original_read(target)
            if target == post:
                data['elements'][0]['from'][0] = 10
            return data
        with patch.object(art, 'read', side_effect=moved_post):
            self.assertFalse(art.intentional_open_faces(path, elements, [(0, 'south')]))

    def test_all_stump_corner_states_cover_their_two_inner_seams(self):
        path, elements = self.elements('homeworks/block/oak_large_stump_corner_piece.json')
        candidates = {(2, 'north'), (2, 'west')}
        self.assertEqual(candidates, art.intentional_open_faces(path, elements, candidates))
        original_read = art.read
        state = art.ASSETS / 'blockstates/homeworks_oak_large_stump.json'
        def missing_core(target):
            data = original_read(target)
            if target == state:
                data['multipart'] = [p for p in data['multipart']
                                     if p['apply']['model'].endswith('_corner_piece')]
            return data
        with patch.object(art, 'read', side_effect=missing_core):
            self.assertFalse(art.intentional_open_faces(path, elements, candidates))

    def test_inner_corner_is_a_real_hole_and_only_its_two_inward_faces_are_repaired(self):
        path, elements = self.elements('homeworks/block/oak_large_stump_inner_corner_piece.json')
        self.assertFalse(art.intentional_open_faces(path, elements, [(0, 'north'), (0, 'west')]))
        source = art.read(art.SOURCE / 'home/assets/cozyhome/models/block/large_stump/template_large_stump_inner_corner_piece.json')
        before = deepcopy(source)
        repaired = finishing.finalized_elements('homeworks/block/large_stump/template_large_stump_inner_corner_piece', source['elements'])
        self.assertEqual(source, before, 'Source data is not mutated')
        self.assertFalse(model_writer.open_faces(repaired))
        self.assertEqual({'up', 'down', 'north', 'west'}, set(repaired[0]['faces']))
        self.assertEqual('#particle', repaired[0]['faces']['north']['texture'])
        self.assertEqual(before['elements'][0]['faces']['up'], repaired[0]['faces']['up'])

    def test_backpack_component_seams_require_complete_native_tier_composites(self):
        for part in ('backpack_front_pouch', 'backpack_left_pouch'):
            path, elements = self.elements('packworks/block/' + part + '.json')
            _, lookup, _ = art_check._resolve(art.read(path), (art.ASSETS,))
            faces, _ = art_check._model_faces(elements, lookup)
            candidates = set(art_check._open_faces(elements, faces))
            self.assertTrue(candidates)
            self.assertEqual(candidates, art.intentional_open_faces(path, elements, candidates))
            finished = finishing.finalized_elements('packworks/block/' + part)
            self.assertEqual(candidates, art.intentional_open_faces(path, finished, candidates))
        original_read = art.read
        tier = art.MODELS / 'packworks/block/iron_backpack.json'
        def missing_component(target):
            data = original_read(target)
            if target == tier:
                data['elements'] = data['elements'][:-1]
            return data
        with patch.object(art, 'read', side_effect=missing_component):
            self.assertFalse(art.intentional_open_faces(path, elements, candidates))

    def test_worm_compost_side_faces_are_covered_by_its_unconditional_frame(self):
        candidates = {(0, side) for side in ('north', 'south', 'east', 'west')}
        for level in range(1, 8):
            path, elements = self.elements(f'aquatic/block/worm_farm_contents{level}.json')
            self.assertEqual(candidates, art.intentional_open_faces(path, elements, candidates))
        original_read = art.read
        state = art.ASSETS / 'blockstates/aquatic_worm_farm.json'
        def missing_frame(target):
            data = original_read(target)
            if target == state:
                data['multipart'] = [p for p in data['multipart'] if p.get('when')]
            return data
        with patch.object(art, 'read', side_effect=missing_frame):
            self.assertFalse(art.intentional_open_faces(path, elements, candidates))

    def test_wicker_arm_ends_require_the_unconditional_opaque_post(self):
        for name in ('wicker_fence_side', 'wicker_fence_side_tall'):
            path, elements = self.elements('supps/block/' + name + '.json')
            self.assertEqual({(0, 'south')}, art.intentional_open_faces(path, elements, [(0, 'south')]))
        original_read = art.read
        state = art.ASSETS / 'blockstates/supps_wicker_fence.json'
        def missing_post(target):
            data = original_read(target)
            if target == state:
                data['multipart'] = [p for p in data['multipart'] if p.get('when')]
            return data
        with patch.object(art, 'read', side_effect=missing_post):
            self.assertFalse(art.intentional_open_faces(path, elements, [(0, 'south')]))

    def test_blackboard_recess_is_closed_by_its_complete_opaque_backing(self):
        path, elements = self.elements('supps/block/blackboard.json')
        self.assertEqual({(0, 'north')}, art.intentional_open_faces(path, elements, [(0, 'north')]))
        moved = deepcopy(elements)
        moved[1]['from'][0] = 1
        self.assertFalse(art.intentional_open_faces(path, moved, [(0, 'north')]))
        with patch.object(art, 'opaque_texture', return_value=False):
            self.assertFalse(art.intentional_open_faces(path, elements, [(0, 'north')]))

    def test_transparent_runtime_texture_overrides_cannot_cover_imported_seams(self):
        cases = [
            ('supps/block/wicker_fence_side.json', {(0, 'south')},
             {'supps/block/wicker_fence_stick.json'}),
            ('aquatic/block/worm_farm_contents7.json', {(0, side) for side in ('north', 'south', 'east', 'west')},
             {'aquatic/block/worm_farm.json'}),
            ('homeworks/block/oak_large_stump_corner_piece.json', {(2, 'north'), (2, 'west')},
             {'homeworks/block/oak_large_stump' + suffix + '.json'
              for suffix in ('', '_corner', '_double', '_inner_corner_piece', '_middle', '_side')}),
            ('packworks/block/backpack_left_pouch.json', {(4, 'west'), (7, 'west'), (7, 'up'), (7, 'down')},
             {'packworks/block/iron_backpack.json'}),
            ('jugtania/block/livingrock_wall_side.json', {(0, 'south')},
             {'jugtania/block/livingrock_wall_post.json'}),
        ]
        original_read = art.read
        for candidate, seams, covers in cases:
            with self.subTest(model=candidate):
                path, elements = self.elements(candidate)
                self.assertEqual(seams, art.intentional_open_faces(path, elements, seams))
                covering_paths = {art.MODELS / relative for relative in covers}
                def transparent_cover(target):
                    data = original_read(target)
                    if target in covering_paths:
                        data['textures'] = {name: 'minecraft:block/glass' for name in data.get('textures', {})}
                    return data
                with patch.object(art, 'read', side_effect=transparent_cover):
                    self.assertNotEqual(seams, art.intentional_open_faces(path, elements, seams),
                                        'A geometry-identical transparent support must not prove these seams')

    def test_cover_opacity_reads_pixels_and_ignores_empty_elements(self):
        path, elements = self.elements('supps/block/wicker_fence_side.json')
        self.assertEqual({(0, 'south')}, art.intentional_open_faces(path, elements, [(0, 'south')]))
        original_image = art_check._image
        def transparent_pixels(texture):
            pixels = original_image(texture)
            if pixels is not None and 'wicker_fence' in str(texture):
                pixels = pixels.copy()
                pixels[:, :, 3] = 0
            return pixels
        with patch.object(art_check, '_image', side_effect=transparent_pixels):
            self.assertFalse(art.intentional_open_faces(path, elements, [(0, 'south')]))
        with patch.object(art, 'material_faces', return_value=([], set())):
            self.assertFalse(art.opaque_elements(path), 'An element with no drawn surfaces is never an opaque cover')


if __name__ == '__main__':
    unittest.main()
