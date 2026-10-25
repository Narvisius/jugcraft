"""Worn atlases must retain original pixels and all archived original mesh provenance."""
import hashlib
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class JimtafactsWearableAssetsTests(unittest.TestCase):
    def test_original_wearable_atlases_are_complete_and_byte_identical(self):
        source = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/assets/artifacts/textures/entity/wearable'
        manifest = json.loads((ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/wearable-copies.json').read_text(encoding='utf-8'))
        expected = {p.relative_to(ROOT).as_posix() for p in source.rglob('*.png')}
        self.assertEqual(expected, {row['source'] for row in manifest['textures']})
        for row in manifest['textures']:
            with self.subTest(row['source']):
                original = (ROOT / row['source']).read_bytes()
                self.assertEqual((ROOT / row['runtime']).read_bytes(), original)
                self.assertEqual(hashlib.sha256(original).hexdigest(), row['sha256'])

    def test_geometry_recovery_preserves_declarative_source_not_binary_loading(self):
        manifest = json.loads((ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/wearable-copies.json').read_text(encoding='utf-8'))
        self.assertEqual(len(manifest['geometry']), 5)
        for row in manifest['geometry']:
            with self.subTest(row['source']):
                self.assertEqual(hashlib.sha256((ROOT / row['source']).read_bytes()).hexdigest(), row['sha256'])
                text = (ROOT / row['runtime']).read_text(encoding='utf-8')
                self.assertIn('CubeListBuilder', text)
                self.assertNotIn('import artifacts.', text)
                self.assertNotIn('ClassLoader', text)
                self.assertNotIn('Runtime.getRuntime', text)


if __name__ == '__main__':
    unittest.main()
