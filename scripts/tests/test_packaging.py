"""Installer contract tests; they do not launch Minecraft or certify mod compatibility."""

import copy
import hashlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import package_modrinth as packaging


class PackagingTests(unittest.TestCase):
    def setUp(self):
        self.lock = packaging.load_lock()
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def jar(self, minecraft=None, nested=False):
        metadata = {
            "id": "jugcraft", "version": "0.1.0-test",
            "depends": {"minecraft": minecraft or "~" + self.lock["minecraft"],
                        "fabricloader": ">=" + self.lock["fabricLoader"]},
        }
        metadata["depends"].update({entry["modId"]: ">=" + entry["modVersion"]
                                    for entry in self.lock["dependencies"] if entry["requiredByJugcraft"]})
        jar = self.root / "jugcraft.jar"
        with zipfile.ZipFile(jar, "w") as archive:
            archive.writestr("fabric.mod.json", json.dumps(metadata))
            if nested:
                archive.writestr("META-INF/jars/unauthorized.jar", b"not allowed")
        return jar

    def test_pack_installs_all_selected_files_with_correct_sides(self):
        jar = self.jar()
        pack = packaging.build_pack(self.lock, jar, self.root / "output", root=self.root)
        with zipfile.ZipFile(pack) as archive:
            index = json.loads(archive.read("modrinth.index.json"))
            self.assertEqual(index["dependencies"]["fabric-loader"], self.lock["fabricLoader"])
            self.assertEqual(len(index["files"]), len(self.lock["dependencies"]))
            self.assertEqual(archive.read("overrides/mods/jugcraft.jar"), jar.read_bytes())
            self.assertEqual([p for p in archive.namelist() if p.endswith(".jar")], ["overrides/mods/jugcraft.jar"])
            by_path = {entry["path"]: entry for entry in index["files"]}
            for library in self.lock["dependencies"]:
                entry = by_path["mods/" + library["filename"]]
                self.assertEqual(entry["env"]["client"], "required")
                self.assertEqual(entry["env"]["server"], "required" if library["side"] == "both" else "unsupported")
                self.assertEqual(entry["hashes"]["sha512"], library["hashes"]["sha512"])

    def test_pack_is_reproducible_for_the_same_inputs(self):
        jar = self.jar()
        first = packaging.build_pack(self.lock, jar, self.root / "one", root=self.root)
        second = packaging.build_pack(self.lock, jar, self.root / "two", root=self.root)
        self.assertEqual(first.read_bytes(), second.read_bytes())

    def test_complete_pack_ships_backpack_defaults_and_both_server_dependencies(self):
        import tomllib
        repository = Path(__file__).resolve().parents[2]
        pack = packaging.build_pack(self.lock, self.jar(), self.root / "complete", root=repository)
        with zipfile.ZipFile(pack) as archive:
            server = tomllib.loads(archive.read("overrides/defaultconfigs/travelersbackpack-server.toml").decode())
            common = tomllib.loads(archive.read("overrides/config/travelersbackpack-common.toml").decode())
            self.assertTrue(server["server"]["backpackSettings"]["backSlotIntegration"])
            self.assertFalse(common["common"]["enableLoot"])
            self.assertFalse(common["common"]["enableVillagerTrade"])
            self.assertIn(b"key_key.travelersbackpack.inventory:key.keyboard.n", archive.read("overrides/options.txt"))
            index = json.loads(archive.read("modrinth.index.json"))
            for key in ("travelers-backpack", "forge-config-api-port"):
                dependency = next(x for x in self.lock["dependencies"] if x["key"] == key)
                self.assertFalse(dependency["requiredByJugcraft"])
                entry = next(x for x in index["files"] if x["path"] == "mods/" + dependency["filename"])
                self.assertEqual(entry["env"], {"client": "required", "server": "required"})

    def test_missing_transitive_dependency_is_rejected(self):
        self.lock["dependencies"] = [x for x in self.lock["dependencies"] if x["key"] != "spell-power"]
        with self.assertRaisesRegex(ValueError, "needs missing project"):
            packaging.validate_lock(self.lock)

    def test_browser_and_quiet_map_are_client_only_pack_defaults(self):
        repository = Path(__file__).resolve().parents[2]
        pack = packaging.build_pack(self.lock, self.jar(), self.root / "browser", root=repository)
        with zipfile.ZipFile(pack) as archive:
            self.assertEqual(json.loads(archive.read("client-overrides/diorama/config.json")),
                             {"minimap": False})
            self.assertNotIn("overrides/diorama/config.json", archive.namelist())
            library = next(x for x in self.lock["dependencies"] if x["key"] == "resourcify")
            entry = next(x for x in json.loads(archive.read("modrinth.index.json"))["files"]
                         if x["path"] == "mods/" + library["filename"])
            self.assertEqual(entry["env"], {"client": "required", "server": "unsupported"})
            self.assertFalse(library["requiredByJugcraft"])
            self.assertEqual(entry["downloads"], library["downloads"])
        self.lock["dependencies"] = [x for x in self.lock["dependencies"] if x["key"] != "fabric-language-kotlin"]
        with self.assertRaisesRegex(ValueError, "needs missing project"):
            packaging.validate_lock(self.lock)

    def test_wrong_exact_dependency_is_rejected(self):
        sodium = next(x for x in self.lock["dependencies"] if x["key"] == "sodium")
        sodium["modrinthVersion"] = "different"
        sodium["maven"] = f"maven.modrinth:{sodium['modrinthProject']}:different"
        with self.assertRaisesRegex(ValueError, "needs exact dependency version"):
            packaging.validate_lock(self.lock)

    def test_download_integrity_rejects_modified_bytes(self):
        library = {"key": "fixture", "size": 4,
                   "hashes": {algorithm: hashlib.new(algorithm, b"good").hexdigest()
                              for algorithm in ("sha1", "sha512")}}
        packaging.verify_bytes(library, b"good")
        with self.assertRaisesRegex(ValueError, "mismatch"):
            packaging.verify_bytes(library, b"evil")

    def test_path_escape_and_non_authorized_download_are_rejected(self):
        for field, value in (("filename", "../escape.jar"), ("downloads", ["https://example.com/unreviewed.jar"])):
            lock = copy.deepcopy(self.lock)
            lock["dependencies"][0][field] = value
            with self.assertRaises(ValueError):
                packaging.validate_lock(lock)

    def test_wrong_minecraft_jar_cannot_be_packaged(self):
        with self.assertRaisesRegex(ValueError, "Wrong Minecraft"):
            packaging.build_pack(self.lock, self.jar(minecraft="~1.21.1"), self.root / "output", root=self.root)

    def test_nested_third_party_jars_cannot_be_packaged(self):
        with self.assertRaisesRegex(ValueError, "must not bundle"):
            packaging.build_pack(self.lock, self.jar(nested=True), self.root / "output", root=self.root)

    def test_game_updater_baseline_matches_initial_pack_without_circular_hash(self):
        jar = self.jar()
        helper = io.BytesIO()
        with zipfile.ZipFile(helper, 'w') as z:
            z.writestr('META-INF/MANIFEST.MF', 'Main-Class: io.github.jimbozoomer.jugcraft.update.UpdateApplier\n')
            z.writestr('io/github/jimbozoomer/jugcraft/update/UpdateApplier.class', b'fixture')
            z.writestr('io/github/jimbozoomer/jugcraft/update/UpdateApplier$StartupLease.class', b'fixture')
        with zipfile.ZipFile(jar, 'a') as z:
            z.writestr('jugcraft/updater-helper.jar', helper.getvalue())
        result = packaging.build_pack(self.lock, jar, self.root / 'game-update', root=self.root)
        with zipfile.ZipFile(result) as z:
            baseline = json.loads(z.read('client-overrides/JUGCRAFT-UPDATE.json'))
            self.assertEqual(baseline['releaseVersion'], '0.1.0-test')
            own = next(f for f in baseline['files'] if f['path'] == 'mods/jugcraft.jar')
            self.assertEqual(own['sha512'], hashlib.sha512(jar.read_bytes()).hexdigest())
            self.assertEqual(len(baseline['files']), len(self.lock['dependencies']) + 1)
            self.assertNotIn('pack', baseline)

    def test_unexpected_helper_classes_are_still_rejected(self):
        jar = self.jar()
        helper = io.BytesIO()
        with zipfile.ZipFile(helper, 'w') as z:
            z.writestr('io/github/jimbozoomer/jugcraft/update/UpdateApplier.class', b'fixture')
            z.writestr('io/github/jimbozoomer/jugcraft/update/UpdateApplier$Unexpected.class', b'fixture')
        with zipfile.ZipFile(jar, 'a') as z:
            z.writestr('jugcraft/updater-helper.jar', helper.getvalue())
        with self.assertRaisesRegex(ValueError, 'Unexpected code'):
            packaging.inspect_jugcraft_jar(jar, self.lock)

    def test_jade_remains_optional_in_standalone_publication(self):
        jade = next(x for x in self.lock["dependencies"] if x["key"] == "jade")
        metadata = packaging.publication_metadata(self.lock, "0.1.0-test")
        entry = next(x for x in metadata["dependencies"] if x["project_id"] == jade["modrinthProject"])
        self.assertEqual(entry["dependency_type"], "optional")
        self.assertEqual(entry["version_id"], jade["modrinthVersion"])


if __name__ == "__main__":
    unittest.main()
