"""PNG encoding differences must not dirty unchanged source pixels."""
import sys
import tempfile
import unittest
from pathlib import Path
from PIL import Image
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
from asset_png import save_rgba

class PngStabilityTests(unittest.TestCase):
    def test_preserves_equivalent_encoding_and_replaces_changed_pixels(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "tile.png"
            image = Image.new("RGBA", (16,16), (12,34,56,255))
            image.save(path, compress_level=0)
            original = path.read_bytes()
            save_rgba(image,path)
            self.assertEqual(original,path.read_bytes())
            image.putpixel((0,0),(90,80,70,0))
            save_rgba(image,path)
            with Image.open(path) as actual:
                self.assertEqual(image.tobytes(),actual.convert("RGBA").tobytes())
            path.write_bytes(b"invalid png")
            save_rgba(image,path)
            with Image.open(path) as actual:
                self.assertEqual(image.tobytes(),actual.convert("RGBA").tobytes())
