"""Direct release inventory is bounded to mods and immutable source versions."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import package_player_update as updater
import promote_player_update as promotion


class PlayerUpdateTests(unittest.TestCase):
    def test_promotion_requires_exact_server_version_and_release_url(self):
        candidate={'schemaVersion':1,'releaseVersion':'test.1',
                   'manifestUrl':updater.BASE+'pack-test.1/player-update.json','manifestSha512':'a'*128}
        self.assertEqual(promotion.validate(candidate,'test.1'),'test.1')
        with self.assertRaises(ValueError):
            promotion.validate(candidate,'test.2')
        for update in ({'manifestUrl':'https://example.com/other.json'},
                       {'manifestSha512':'bad'},{'releaseVersion':'../bad'},{'schemaVersion':2}):
            with self.assertRaises(ValueError):
                promotion.validate({**candidate,**update},'test.1')

    def test_inventory_preserves_settings_and_uses_fixed_release_urls(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / 'sample.mrpack'
            with zipfile.ZipFile(source, 'w') as z:
                z.writestr('modrinth.index.json', json.dumps({'versionId': 'test.1',
                    'files': [], 'dependencies': {'minecraft': '26.3', 'fabric-loader': '0.19.5'}}))
                z.writestr('overrides/mods/jugcraft.jar', b'jar')
                z.writestr('overrides/options.txt', b'not managed')
                z.writestr('overrides/config/client.json', b'not managed')
            result = updater.build(source, root/'out', 'a'*40)
            self.assertEqual([f['path'] for f in result['files']], ['mods/jugcraft.jar'])
            self.assertEqual(result['files'][0]['url'], updater.BASE+'pack-test.1/jugcraft-client.jar')
            self.assertEqual((root/'out/jugcraft-client.jar').read_bytes(), b'jar')
            channel = json.loads((root/'out/stable.json').read_text())
            self.assertEqual(channel['manifestUrl'], updater.BASE+'pack-test.1/player-update.json')
            with self.assertRaises(ValueError):
                updater.build(source, root/'out', 'a'*40)

    def test_unexpected_payload_paths_rejected_before_writing(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            for path in ('../outside.jar', 'options.txt', 'mods/a/b.jar'):
                source=root/'bad.mrpack'
                with zipfile.ZipFile(source,'w') as z:
                    z.writestr('modrinth.index.json', json.dumps({'versionId':'test.1','files':[{'path':path}]}))
                with self.assertRaises(ValueError):
                    updater.build(source,root/'out','a'*40)
                self.assertFalse((root/'out').exists())
