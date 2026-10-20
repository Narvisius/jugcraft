"""Regeneration must remove stale generated packs while retaining authored integration data."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
from generated_resource_cleanup import prune


class GeneratedResourceCleanupTests(unittest.TestCase):
    def test_cleanup_preserves_authored_backpack_pack(self):
        with tempfile.TemporaryDirectory() as temporary:
            resources = Path(temporary) / "resources"
            fixtures = {
                "resourcepacks/travelers_backpack/pack.mcmeta": b'{"pack": {}}',
                "resourcepacks/travelers_backpack/data/travelersbackpack/recipe/standard.json": b'{"type": "test"}',
                "fabric.mod.json": b'{"id": "jugcraft"}',
                "resourcepacks/alternate_machines/authored.json": b'{"owner": true}',
                "jugcraft/menagerie/catalog.json": b'{"creatures": []}',
            }
            for relative in [*fixtures, "resourcepacks/alternate_machines/stale.json", "resourcepacks/fusion_textures/stale.json"]:
                path = resources / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(fixtures.get(relative, b"stale"))
            stale = {"resourcepacks/alternate_machines/stale.json", "resourcepacks/fusion_textures/stale.json"}
            prune(resources, stale | {"fabric.mod.json"}, {"fabric.mod.json"})
            for relative, content in fixtures.items():
                self.assertEqual((resources / relative).read_bytes(), content)
            for relative in stale:
                self.assertFalse((resources / relative).exists())

    def test_manifest_cannot_delete_outside_resources(self):
        with tempfile.TemporaryDirectory() as temporary:
            resources = Path(temporary) / "resources"
            resources.mkdir()
            outside = Path(temporary) / "keep.txt"
            outside.write_text("keep")
            with self.assertRaises(ValueError):
                prune(resources, {"../keep.txt"}, set())
            self.assertEqual(outside.read_text(), "keep")
