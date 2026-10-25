"""Original curio appearance, dynamic affinity art and native wearable integration."""
import importlib.util
import json
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'src/main/resources'
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/assets/irons_spellbooks'
spec = importlib.util.spec_from_file_location('curios', ROOT / 'tools/jimtafacts_spellbook_curios.py')
curios = importlib.util.module_from_spec(spec)
spec.loader.exec_module(curios)

class SpellbookCurioAssets(unittest.TestCase):
    def test_all_source_layers_and_animation_sidecars_remain_unchanged(self):
        models = [name for name in curios.CURIOS if name != 'affinity_ring'] + ['affinity_ring_' + school for school in curios.SCHOOLS]
        for name in models:
            source = json.loads((SOURCE / f'models/item/{name}.json').read_text(encoding='utf-8'))
            native = json.loads((RES / f'assets/jugcraft/models/jimtafacts/spellbooks/item/{name}.json').read_text(encoding='utf-8'))
            self.assertEqual(set(source.get('textures', {})), set(native.get('textures', {})))
            for ref in source.get('textures', {}).values():
                if not ref.startswith('irons_spellbooks:'):
                    continue
                relative = ref.split(':', 1)[1] + '.png'
                original = SOURCE / 'textures' / relative
                runtime = RES / 'assets/jugcraft/textures/jimtafacts/spellbooks' / relative
                self.assertEqual(original.read_bytes(), runtime.read_bytes(), name)
                if original.with_suffix('.png.mcmeta').exists():
                    self.assertEqual(json.loads(original.with_suffix('.png.mcmeta').read_text(encoding='utf-8')),
                                     json.loads(runtime.with_suffix('.png.mcmeta').read_text(encoding='utf-8')))

    def test_affinity_switch_retains_all_nine_authored_appearances(self):
        model = json.loads((RES / 'assets/jugcraft/items/jimta_spellbooks_affinity_ring.json').read_text(encoding='utf-8'))['model']
        self.assertEqual(model['type'], 'minecraft:range_dispatch')
        self.assertEqual(len(model['entries']), 9)
        for index, school in enumerate(curios.SCHOOLS):
            self.assertEqual(model['entries'][index]['threshold'], index)
            self.assertTrue(model['entries'][index]['model']['model'].endswith('affinity_ring_' + school))

    def test_all_twenty_one_curios_use_additive_existing_slots(self):
        self.assertEqual(len(curios.CURIOS), 21)
        for name, (slot, _) in curios.CURIOS.items():
            tag = json.loads((RES / f'data/trinkets/tags/item/{slot}.json').read_text(encoding='utf-8'))
            self.assertFalse(tag.get('replace', False))
            self.assertIn('jugcraft:jimta_spellbooks_' + name, tag['values'])
        catalog = json.loads((RES / 'jugcraft/jimtafacts_spellbooks/affinities.json').read_text(encoding='utf-8'))
        self.assertIn('heal', catalog)
        self.assertIn('fire', set(catalog.values()))

if __name__ == '__main__':
    unittest.main()
