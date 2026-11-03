"""Fresh CI can resolve pinned shapes before Loom creates a remapped client."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
import newest_shapes

class ShapeArchiveTests(unittest.TestCase):
    def tearDown(self):
        newest_shapes.vanilla_model.cache_clear()

    def test_prepared_archive_without_remapped_loom_cache(self):
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            jar = home / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
            jar.parent.mkdir(parents=True)
            model = {'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16]}]}
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr('assets/minecraft/models/block/cube.json', json.dumps(model))
            with patch.object(Path, 'home', return_value=home), patch.object(Path, 'glob', return_value=iter(())):
                self.assertEqual(model, newest_shapes.vanilla_model('block/cube'))
                with self.assertRaisesRegex(ValueError, 'unavailable'):
                    newest_shapes.vanilla_model('block/nonexistent')
