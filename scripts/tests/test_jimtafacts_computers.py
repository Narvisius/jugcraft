"""Preserve original computer art and source-authored model coordinates across namespace adaptation."""
import hashlib
import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ComputerAssetsTests(unittest.TestCase):
    def test_every_runtime_copy_matches_recorded_source(self):
        rows = json.loads((ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/computer-copies.json').read_text(encoding='utf-8'))
        self.assertGreater(len(rows), 200)
        spec = importlib.util.spec_from_file_location('jimtafacts_computers', ROOT / 'tools/jimtafacts_computers.py')
        generator = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(generator)
        for row in rows:
            with self.subTest(row['source']):
                original = (ROOT / row['source']).read_bytes()
                self.assertEqual(hashlib.sha256(original).hexdigest(), row['sha256'])
                runtime = (ROOT / row['runtime']).read_bytes()
                if row['source'].endswith('.json'):
                    self.assertEqual(json.loads(runtime), generator.remap(json.loads(original)))
                else:
                    self.assertEqual(runtime, original)

    def test_connected_monitor_variants_and_turtle_upgrade_models_are_supplied_shapes(self):
        assets = ROOT / 'src/main/resources/assets/jugcraft'
        for family in ('normal', 'advanced'):
            state = json.loads((assets / f'blockstates/jimta_computer_monitor_{family}.json').read_text(encoding='utf-8'))
            self.assertEqual(len(state['variants']), 128)
            for value in state['variants'].values():
                model = value['model'].split(':', 1)[1]
                self.assertTrue((assets / f'models/{model}.json').is_file(), model)
        for part in ('crafting_table', 'speaker', 'modem_normal_off', 'modem_normal_on', 'modem_advanced_off', 'modem_advanced_on'):
            for side in ('left', 'right'):
                item = json.loads((assets / f'items/jimtafacts/turtle_{part}_{side}.json').read_text(encoding='utf-8'))
                self.assertEqual(item['model']['model'], f'jugcraft:jimtafacts/computers/block/turtle_{part}_{side}')


if __name__ == '__main__':
    unittest.main()
