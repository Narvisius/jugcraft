"""Source fidelity/coverage checks; authored for the final combined pass, not run during intake."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import jimtafacts_network_equipment as equipment
import jimtafacts_network_fixtures as fixtures
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/applied energentics reborn by jimbo/assets/ae2'
ASSETS = ROOT / 'src/main/resources/assets/jugcraft'


class EquipmentSourceFidelity(unittest.TestCase):
    def test_source_models_keep_all_original_geometry_and_uvs(self):
        for name in equipment.ITEMS:
            if name in ('memory_card', 'color_applicator'): continue
            descriptor = json.loads((SOURCE / f'items/{name}.json').read_text(encoding='utf-8'))['model']
            ref = descriptor.get('model')
            if not ref: continue
            original = json.loads((SOURCE / ('models/' + ref[4:] + '.json')).read_text(encoding='utf-8'))
            actual = json.loads((ASSETS / ('models/jimtafacts/ae2/' + ref[4:] + '.json')).read_text(encoding='utf-8'))
            for a, b in zip(original.get('elements', []), actual.get('elements', [])):
                self.assertEqual(a['from'], b['from'], name); self.assertEqual(a['to'], b['to'], name)
                self.assertEqual(a.get('rotation'), b.get('rotation'), name)
                for face in a.get('faces', {}): self.assertEqual(a['faces'][face].get('uv'), b['faces'][face].get('uv'), name)
            self.assertEqual(len(original.get('elements', [])), len(actual.get('elements', [])), name)

    def test_no_neoforge_only_item_selectors_remain(self):
        for name in equipment.ITEMS + fixtures.BLOCKS:
            text = (ASSETS / f'items/jimta_{name}.json').read_text(encoding='utf-8')
            self.assertNotIn('ae2:', text, name); self.assertNotIn('neoforge:', text, name)

    def test_source_registry_records_every_source_definition(self):
        record = json.loads((ROOT / 'art/owner-library/catalog/jimtafacts-ae-registry.json').read_text(encoding='utf-8'))
        self.assertEqual({e['source_id'] for e in record['entries']}, {'ae2:' + p.stem for p in (SOURCE / 'items').glob('*.json')})
        self.assertTrue(all(e['implementation'] for e in record['entries']))
        smart = [e for e in record['entries'] if 'smart' in e['source_id'] and 'cable' in e['source_id']]
        self.assertTrue(smart)
        for entry in smart:
            self.assertEqual(entry['status'], 'functional_native', entry['source_id'])
            name = entry['runtime_id'].split(':', 1)[1]
            states = json.loads((ASSETS / f'blockstates/{name}.json').read_text(encoding='utf-8'))['multipart']
            for channels in range(9):
                parts = [p for p in states if p.get('when', {}).get('channels') == str(channels)]
                self.assertEqual(len(parts), 7, (name, channels))
                for part in parts:
                    path = part['apply']['model'].split(':', 1)[1]
                    model = json.loads((ASSETS / f'models/{path}.json').read_text(encoding='utf-8'))
                    self.assertTrue(model['textures']['channels'].endswith(f'channels_0{min(4, channels)}'))
                    self.assertTrue(model['textures']['channels_even'].endswith(f'channels_1{max(0, channels - 4)}'))

    def test_quartz_glass_original_texture_bytes(self):
        for source in (SOURCE / 'textures/block/glass').glob('*.png'):
            target = ASSETS / ('textures/jimtafacts/ae2/block/glass/' + source.name)
            if target.exists(): self.assertEqual(source.read_bytes(), target.read_bytes(), source.name)


if __name__ == '__main__': unittest.main()
