"""Original drive slot meshes/UVs and press texture; executed only in the final combined batch."""
import hashlib
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/applied energentics reborn by jimbo/assets/ae2'
ASSETS = ROOT / 'src/main/resources/assets/jugcraft'


class NetworkRenderingAssetsTests(unittest.TestCase):
    def test_all_ten_source_cell_chassis_keep_exact_geometry_and_uvs(self):
        for kind in ('item', 'fluid'):
            for size in (1, 4, 16, 64, 256):
                name = f'drive_{size}k_{kind}_cell'
                source = json.loads((SOURCE / f'models/block/{name}.json').read_text(encoding='utf-8'))
                runtime = json.loads((ASSETS / f'models/jimtafacts/ae2/block/{name}.json').read_text(encoding='utf-8'))
                self.assertEqual(source['elements'], runtime['elements'], name)
                visual = json.loads((ASSETS / f'items/jimtafacts/ae2/{name}.json').read_text(encoding='utf-8'))
                self.assertEqual(visual['model']['model'], f'jugcraft:jimtafacts/ae2/block/{name}')

    def test_original_inside_press_texture_is_exact_copy(self):
        original = (SOURCE / 'textures/block/inscriber_inside.png').read_bytes()
        runtime = (ASSETS / 'textures/jimtafacts/ae2/block/inscriber_inside.png').read_bytes()
        self.assertEqual(original, runtime)
        records = json.loads((ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/network-render-copies.json').read_text(encoding='utf-8'))
        self.assertEqual(records[0]['sha256'], hashlib.sha256(original).hexdigest())


if __name__ == '__main__':
    unittest.main()
