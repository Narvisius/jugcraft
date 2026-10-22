"""The shared join link must stay on Modrinth and must not become a shell command."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import package_server_join as join


class ServerJoinTests(unittest.TestCase):
    def test_join_bundle_uses_linked_server_not_loose_import(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / 'distribution').mkdir()
            config = root / 'distribution/server-project.json'
            config.write_text(json.dumps({'projectUrl': 'https://modrinth.com/server/jugcraft-server',
                                          'serverAddress': 'jugcraft.modrinth.gg'}))
            result = join.build_join(root / 'out', root)
            with zipfile.ZipFile(result) as archive:
                self.assertEqual(set(archive.namelist()), {'Join-JUGCRAFT.url', 'READ-ME.txt'})
                shortcut = archive.read('Join-JUGCRAFT.url').decode()
                self.assertIn('URL=https://modrinth.com/server/jugcraft-server\r\n', shortcut)
                self.assertNotIn('IconFile', shortcut)
                instructions = archive.read('READ-ME.txt').decode()
                self.assertIn('Keep the instance linked', instructions)
                self.assertIn('must be approved', instructions)
            with self.assertRaisesRegex(ValueError, 'Never replace'):
                join.build_join(root / 'out', root)

    def test_settings_reject_non_modrinth_and_injected_urls(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / 'distribution').mkdir()
            for url in ('https://evil.example/server/jugcraft', 'https://modrinth.com/server/jugcraft\nIconFile=bad',
                        'https://modrinth.com/modpack/jugcraft'):
                (root / 'distribution/server-project.json').write_text(json.dumps({
                    'projectUrl': url, 'serverAddress': 'jugcraft.modrinth.gg'}))
                with self.assertRaises(ValueError):
                    join.build_join(root / 'out', root)
                self.assertFalse((root / 'out').exists())


if __name__ == '__main__':
    unittest.main()
