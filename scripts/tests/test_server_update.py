import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import package_server_update as update


class ServerUpdateTests(unittest.TestCase):
    def bundle(self, path, files, version, java=25, corrupt=False):
        with zipfile.ZipFile(path, 'w') as z:
            rows = []
            for name, data in files.items():
                z.writestr(name, data)
                rows.append({'path': name, 'size': len(data), 'sha512': hashlib.sha512(data).hexdigest()})
            if corrupt:
                rows[0]['sha512'] = '0'*128
            z.writestr('JUGCRAFT-SERVER-FILES.json', json.dumps(rows))
            z.writestr('JUGCRAFT-RELEASE.json', json.dumps({
                'releaseVersion': version, 'minecraft': '26.3', 'fabricLoader': '0.19.5', 'java': java}))

    def test_only_changed_mods_are_uploaded_and_configs_are_reviewed(self):
        with tempfile.TemporaryDirectory() as temp:
            p = Path(temp)
            self.bundle(p/'old.zip', {'mods/jugcraft.jar': b'old', 'mods/same.jar': b'same',
                'mods/removed.jar': b'gone', 'config/owner.toml': b'old'}, 'one')
            self.bundle(p/'new.zip', {'mods/jugcraft.jar': b'new', 'mods/same.jar': b'same',
                'config/owner.toml': b'new'}, 'two')
            result = update.build_update(p/'old.zip', p/'new.zip', p/'out')
            with zipfile.ZipFile(p/'out/JUGCRAFT-changed-mods.zip') as z:
                self.assertEqual(z.namelist(), ['mods/jugcraft.jar'])
                self.assertEqual(z.read('mods/jugcraft.jar'), b'new')
            self.assertEqual(result['removeOldManagedModsManually'], ['mods/removed.jar'])
            self.assertEqual(result['mergeChangedDefaultsManually'], ['config/owner.toml'])
            with self.assertRaisesRegex(ValueError, 'empty'):
                update.build_update(p/'old.zip', p/'new.zip', p/'out')

    def test_corruption_unsafe_paths_and_platform_changes_fail_before_output(self):
        for files, java, corrupt in [({'mods/jugcraft.jar': b'x'}, 25, True),
                                     ({'mods/../world/level.dat': b'x'}, 25, False),
                                     ({'world/level.dat': b'x'}, 25, False),
                                     ({'mods/jugcraft.jar': b'x'}, 26, False)]:
            with self.subTest(files=files, java=java, corrupt=corrupt), tempfile.TemporaryDirectory() as temp:
                p = Path(temp)
                self.bundle(p/'old.zip', {'mods/jugcraft.jar': b'old'}, 'one')
                self.bundle(p/'new.zip', files, 'two', java, corrupt)
                with self.assertRaises(ValueError):
                    update.build_update(p/'old.zip', p/'new.zip', p/'out')
                self.assertFalse((p/'out').exists())
