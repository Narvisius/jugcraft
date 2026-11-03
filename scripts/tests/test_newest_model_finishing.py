"""Real imported surfaces stay source-backed, closed where needed and free of clipping."""
from copy import deepcopy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import art_check
import newest_model_finishing as finishing


class ModelFinishingTests(unittest.TestCase):
    def test_actual_target_models_have_no_flickering_faces(self):
        outputs = {}
        finishing.generate(lambda p, v: outputs.setdefault(p, v))
        self.assertEqual(16, len(outputs))  # Scoped cap/handle repairs; existing separation patches stay intact.
        for path, model in outputs.items():
            with self.subTest(model=path.name):
                elements, lookup, _ = art_check._resolve(model, (art_check.ASSETS,))
                faces, raw_uvs = art_check._model_faces(elements, lookup)
                self.assertFalse(any(any(a > b for a, b in zip(e['from'], e['to'])) for e in elements))
                self.assertTrue(all(min(uv) >= -1e-3 and max(uv) <= 16 + 1e-3 for uv in raw_uvs))
                for i, j, gap, overlap, u, v, normal, distance in art_check._pairs(faces, art_check.N1_GAP):
                    if gap == 0 and art_check._layered(elements, faces[i], faces[j]):
                        continue
                    self.assertFalse(art_check._differ(faces[i], faces[j], overlap, u, v, normal, distance),
                                     str((path, faces[i].label, faces[j].label, gap)))

    def test_finishing_is_idempotent_and_rejects_changed_source_geometry(self):
        with tempfile.TemporaryDirectory() as directory:
            resources = Path(directory)
            paths = sorted({finishing.owner_relative(p) for p in finishing.TARGETS})
            for relative in paths:
                source = finishing.RES / 'assets/jugcraft/models/newest' / (relative + '.json')
                target = resources / source.relative_to(finishing.RES)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(source.read_bytes())
            written = finishing.generate(resources=resources)
            first = {p: p.read_bytes() for p in written}
            finishing.generate(resources=resources)
            self.assertEqual(first, {p: p.read_bytes() for p in written})
            changed = written[0]
            model = json.loads(changed.read_text())
            model['elements'][0]['from'][0] += .35
            changed.write_text(json.dumps(model))
            writes = []
            with self.assertRaisesRegex(ValueError, 'diverged from its preserved source'):
                finishing.generate(lambda p, v: writes.append(p), resources=resources)
            self.assertFalse(writes, 'Reject unexpected geometry before writing any model')

    def test_basket_handle_plane_conversion_preserves_every_surface_and_uv(self):
        for relative in sorted(finishing.BASKETS):
            for element in finishing.original_elements(relative)[-2:]:
                before, _ = art_check._model_faces([element], lambda texture: texture)
                after, _ = art_check._model_faces(finishing.surface_planes(element), lambda texture: texture)
                self.assertEqual(len(before), len(after))
                for original, native in zip(before, after):
                    self.assertEqual(sorted(original.pts), sorted(native.pts))
                    self.assertEqual(original.normal, native.normal)
                    self.assertEqual(original.tex, native.tex)
                    for point in original.pts:
                        for u, v in zip(original.uv_at(point), native.uv_at(point)):
                            self.assertAlmostEqual(u, v)

    def test_cap_detection_requires_complete_same_facing_opaque_surface(self):
        body = {'from': [4, 1, 4], 'to': [12, 4, 12],
                'faces': {face: {'texture': '#solid'} for face in ('north', 'south', 'east', 'west', 'down')}}
        cap = {'from': [4, 4, 4], 'to': [12, 4, 12], 'faces': {'up': {'texture': '#solid'}}}
        solid = np.full((16, 16, 4), 255, dtype=np.uint8)

        def holes(surface, pixels=solid):
            elements = [body, surface]
            with patch.object(art_check, '_image', return_value=pixels):
                faces, _ = art_check._model_faces(elements, lambda texture: texture)
                return art_check._open_faces(elements, faces)

        self.assertNotIn((0, 'up'), holes(cap), 'A complete actual cap closes the body even with zero thickness')
        reversed_cap = deepcopy(cap)
        reversed_cap['faces'] = {'down': {'texture': '#solid'}}
        self.assertIn((0, 'up'), holes(reversed_cap))
        partial = deepcopy(cap)
        partial['to'][0] = 8
        self.assertIn((0, 'up'), holes(partial))
        offset = deepcopy(cap)
        offset['from'][1] = offset['to'][1] = 4.1
        self.assertIn((0, 'up'), holes(offset))
        transparent = solid.copy()
        transparent[:, :, 3] = 0
        # Keep the body opaque while making only the offered cap transparent.
        with patch.object(art_check, '_image', side_effect=lambda texture: transparent if texture == '#clear' else solid):
            clear = deepcopy(cap)
            clear['faces']['up']['texture'] = '#clear'
            faces, _ = art_check._model_faces([body, clear], lambda texture: texture)
            self.assertIn((0, 'up'), art_check._open_faces([body, clear], faces))


if __name__ == '__main__':
    unittest.main()
