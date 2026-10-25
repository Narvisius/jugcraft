"""Owner texture fidelity, modern model references and nondestructive resource regeneration."""
import hashlib
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import jimtafacts


class JimtafactsAssetsTests(unittest.TestCase):
    def test_native_static_gecko_assets_have_a_parseable_idle_definition(self):
        base = ROOT / 'src/main/resources/assets/jugcraft/geckolib/animations'
        for name in ('armor/jimtafacts/empty', 'block/jimtafacts/tyros_statue'):
            row = json.loads((base / (name + '.animation.json')).read_text(encoding='utf-8'))
            # GeckoLib rejects a file with no animation definitions even for static meshes.
            self.assertEqual(row['animations']['idle'], {'loop': True, 'bones': {}})

    def test_copied_icons_match_the_supplied_files(self):
        copies = json.loads((ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/runtime-copies.json').read_text(encoding='utf-8'))
        icons=[row for row in copies if '/textures/item/' in row['runtime']]
        self.assertEqual(len(icons), len(jimtafacts.items()) - 1)
        self.assertEqual(len(copies)-len(icons),1)  # Supplied mimic entity skin.
        for row in copies:
            with self.subTest(row['runtime']):
                original = (ROOT / row['source']).read_bytes()
                self.assertEqual((ROOT / row['runtime']).read_bytes(), original)
                self.assertEqual(hashlib.sha256(original).hexdigest(), row['sha256'])

    def test_every_modern_item_model_resolves_to_a_real_texture(self):
        res = ROOT / 'src/main/resources'
        for name in set(jimtafacts.ACCESSORIES) | set(jimtafacts.FOOD):
            with self.subTest(name):
                item = json.loads((res / f'assets/jugcraft/items/jimta_{name}.json').read_text(encoding='utf-8'))
                model_id = item['model']['model'].split(':')[1]
                model = json.loads((res / f'assets/jugcraft/models/{model_id}.json').read_text(encoding='utf-8'))
                texture = model['textures']['layer0'].split(':')[1]
                self.assertTrue((res / f'assets/jugcraft/textures/{texture}.png').is_file())

    def test_original_umbrella_geometry_and_mimic_texture_are_preserved(self):
        res = ROOT / 'src/main/resources'
        source = jimtafacts.SOURCE / 'assets/artifacts'
        for name in ('umbrella_held', 'umbrella_held_blocking'):
            original=json.loads((source/f'models/item/{name}.json').read_text(encoding='utf-8'))
            imported=json.loads((res/f'assets/jugcraft/models/item/jimtafacts/{name}.json').read_text(encoding='utf-8'))
            self.assertEqual(original.get('elements'),imported.get('elements'))
            self.assertEqual(original.get('display'),imported.get('display'))
        self.assertEqual((source/'textures/entity/mimic.png').read_bytes(),(res/'assets/jugcraft/textures/entity/jimtafacts/mimic.png').read_bytes())
        item=json.loads((res/'assets/jugcraft/items/jimta_umbrella.json').read_text(encoding='utf-8'))
        self.assertIn('minecraft:using_item',json.dumps(item))
        self.assertNotIn('artifacts:',json.dumps(item))

    def test_regeneration_preserves_existing_shared_tags_and_language(self):
        res = ROOT / 'src/main/resources'
        lang = res / 'assets/jugcraft/lang/en_us.json'
        paths = [lang] + sorted((res / 'data/trinkets/tags/item').rglob('*.json'))
        before = {p:json.loads(p.read_text(encoding='utf-8')) for p in paths}
        written={}
        with patch.object(jimtafacts,'dump',side_effect=lambda p,v:written.__setitem__(p,v)), patch.object(jimtafacts.shutil,'copyfile'):
            jimtafacts.generate()
            for p, previous in before.items():
                self.assertEqual(written.get(p,previous), previous, str(p))

    def test_equipped_tags_link_without_replacing_existing_equipment(self):
        res = ROOT / 'src/main/resources'
        for name, (slot, _, _) in jimtafacts.ACCESSORIES.items():
            shared = json.loads((res / f'data/trinkets/tags/item/{slot}.json').read_text(encoding='utf-8'))
            own = json.loads((res / f'data/jugcraft/tags/item/jimtafacts/{slot}.json').read_text(encoding='utf-8'))
            self.assertFalse(shared['replace'])
            self.assertIn('#jugcraft:jimtafacts/' + slot, shared['values'])
            self.assertIn('jugcraft:jimta_' + name, own['values'])

if __name__ == '__main__': unittest.main()
