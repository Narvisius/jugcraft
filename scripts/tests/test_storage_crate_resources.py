"""Targeted crate refresh must agree with the full generator's tag order."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
import storage_crates


class StorageCrateResourceTests(unittest.TestCase):
    def test_targeted_refresh_preserves_full_generator_order(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            tag = root / 'data/minecraft/tags/block/mineable/pickaxe.json'
            bays = ['jugcraft:mecha_workbench', 'jugcraft:tank_workbench']

            def write(path, obj):
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(obj), encoding='utf-8')

            write(tag, {'values': ['minecraft:stone']})
            storage_crates.write_all(write, root / 'assets', root / 'data/jugcraft', {})
            expected = json.loads(tag.read_text())
            expected['values'] += bays  # The full generator writes vehicle bays last.
            write(tag, expected)
            storage_crates.write_all(write, root / 'assets', root / 'data/jugcraft', {})
            self.assertEqual(expected, json.loads(tag.read_text()))

            # Repair the older targeted refresh order as well, without duplicates.
            write(tag, {'values': ['minecraft:stone'] + bays + expected['values'][1:-2]})
            storage_crates.write_all(write, root / 'assets', root / 'data/jugcraft', {})
            self.assertEqual(expected, json.loads(tag.read_text()))
