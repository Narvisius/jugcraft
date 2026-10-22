"""Release bundle contracts, including side filtering and immutable output."""
import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import package_release as release
import package_modrinth as pack


class ReleasePackagingTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.cache = self.root / 'cache'
        self.cache.mkdir()
        self.lock = copy.deepcopy(pack.load_lock())
        # Tiny fixtures retain the real dependency identities/sides and metadata.
        for row in self.lock['dependencies']:
            data = row['key'].encode()
            row['size'] = len(data)
            row['hashes'] = {a: hashlib.new(a, data).hexdigest() for a in ('sha1', 'sha512')}
            (self.cache / row['filename']).write_bytes(data)
        metadata = {'id': 'jugcraft', 'version': '0.1.0-test', 'depends': {
            'minecraft': '~' + self.lock['minecraft'], 'fabricloader': '>=' + self.lock['fabricLoader']}}
        metadata['depends'].update({r['modId']: '>=' + r['modVersion'] for r in self.lock['dependencies'] if r['requiredByJugcraft']})
        self.jar = self.root / 'jugcraft.jar'
        with zipfile.ZipFile(self.jar, 'w') as archive:
            archive.writestr('fabric.mod.json', json.dumps(metadata))
        for path in ('overrides/config/shared.toml', 'server-overrides/config/shared.toml',
                     'overrides/defaultconfigs/server.toml', 'overrides/options.txt',
                     'client-overrides/config/client.json', 'overrides/saves/private.txt'):
            target = self.root / 'distribution' / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(path)

    def build(self, output='out'):
        return release.build_release(self.lock, self.jar, self.cache, self.root / output,
                                     '0.1.0-test.1', 'a' * 40, self.root)

    def test_server_bundle_excludes_client_mods_and_private_files(self):
        result = self.build()
        server = next(x for x in result['artifacts'] if '-server-' in x['name'])
        with zipfile.ZipFile(self.root / 'out' / server['name']) as archive:
            names = archive.namelist()
            for row in self.lock['dependencies']:
                self.assertEqual('mods/' + row['filename'] in names, row['side'] == 'both')
            self.assertEqual(archive.read('config/shared.toml'), b'server-overrides/config/shared.toml')
            self.assertIn('defaultconfigs/server.toml', names)
            for forbidden in ('options.txt', 'config/client.json', 'saves/private.txt', 'eula.txt'):
                self.assertNotIn(forbidden, names)
            inventory = json.loads(archive.read('JUGCRAFT-SERVER-FILES.json'))
            for entry in inventory:
                self.assertEqual(hashlib.sha512(archive.read(entry['path'])).hexdigest(), entry['sha512'])
        for entry in result['artifacts']:
            self.assertEqual(release.sha512(self.root / 'out' / entry['name']), entry['sha512'])

    def test_corrupt_dependency_rejected_before_release_creation(self):
        row = next(x for x in self.lock['dependencies'] if x['side'] == 'both')
        (self.cache / row['filename']).write_bytes(b'corrupt')
        with self.assertRaisesRegex(ValueError, 'mismatch'):
            self.build()
        self.assertFalse((self.root / 'out').exists())

    def test_player_handout_contains_pack_and_instructions_without_executable(self):
        result = self.build()
        player = next(x for x in result['artifacts'] if '-player-' in x['name'])
        with zipfile.ZipFile(self.root / 'out' / player['name']) as archive:
            name = next(n for n in archive.namelist() if n.endswith('.mrpack'))
            self.assertEqual(set(archive.namelist()), {name, 'READ-ME.txt'})
            self.assertIn('Apply update & quit', archive.read('READ-ME.txt').decode())
        self.assertFalse(any(x['name'].endswith(('.cmd', '.exe', '.ps1')) for x in result['artifacts']))
        with zipfile.ZipFile(self.root / 'out' / name) as archive:
            self.assertEqual(json.loads(archive.read('modrinth.index.json'))['name'], 'JUGCRAFT')

    def test_release_is_reproducible_and_cannot_replace_existing_output(self):
        first = self.build()
        second = self.build('second')
        self.assertEqual(first, second)
        with self.assertRaisesRegex(ValueError, 'never overwrite'):
            self.build()

    def test_unidentified_commit_and_unsafe_version_rejected(self):
        for version, commit in (('../bad', 'a' * 40), ('test', 'main')):
            with self.assertRaises(ValueError):
                release.build_release(self.lock, self.jar, self.cache, self.root / 'out', version, commit, self.root)

    def test_online_installer_is_pinned_and_verifies_before_opening(self):
        # Legacy utility remains readable/tested, but new releases do not ship it.
        pack_record = {'name': 'jugcraft-complete-0.1.0-test.1.mrpack', 'sha512': 'a' * 128}
        script = release.online_installer('0.1.0-test.1', pack_record['name'], pack_record['sha512']).decode()
        self.assertIn('/releases/download/pack-0.1.0-test.1/'+pack_record['name'], script)
        self.assertIn(pack_record['sha512'], script)
        self.assertLess(script.index('Get-FileHash'), script.index('Start-Process'))
        self.assertIn("DisplayName -eq 'Modrinth App'", script)
        self.assertNotIn('ExecutionPolicy', script)
        self.assertNotIn('/latest/', script)
        self.assertLess(max(map(len, script.splitlines())), 8191)

    def test_configured_server_is_recorded_and_join_bundle_checksummed(self):
        settings = {'projectUrl': 'https://modrinth.com/server/jugcraft-server',
                    'serverAddress': 'jugcraft.modrinth.gg'}
        (self.root / 'distribution/server-project.json').write_text(json.dumps(settings))
        result = self.build()
        self.assertEqual(result['serverAddress'], settings['serverAddress'])
        self.assertEqual(result['modrinthServerProject'], settings['projectUrl'])
        entry = next(x for x in result['artifacts'] if x['name'] == 'JUGCRAFT-Join.zip')
        self.assertEqual(entry['sha512'], release.sha512(self.root / 'out' / entry['name']))

    def test_disabled_listing_uses_direct_inventory_without_join_shortcut(self):
        (self.root/'distribution/server-project.json').write_text(json.dumps({
            'enabled':False,'projectUrl':'https://modrinth.com/server/jugcraft-server',
            'serverAddress':'jugcraft.modrinth.gg'}))
        result=self.build()
        self.assertIsNone(result['modrinthServerProject'])
        self.assertNotIn('JUGCRAFT-Join.zip',[a['name'] for a in result['artifacts']])
        manifest=json.loads((self.root/'out/player-update.json').read_text())
        self.assertEqual(manifest['sourceCommit'],'a'*40)
        self.assertEqual(len(manifest['files']),len(self.lock['dependencies'])+1)
        self.assertEqual((self.root/'out/jugcraft-client.jar').read_bytes(),self.jar.read_bytes())


if __name__ == '__main__':
    unittest.main()
