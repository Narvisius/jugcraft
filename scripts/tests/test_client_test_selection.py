"""Client selection must discover every registered package and preserve all shards' coverage."""
import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("select_client_tests", ROOT / "tools/select_client_tests.py")
selection = importlib.util.module_from_spec(spec)
spec.loader.exec_module(selection)


class ClientTestSelectionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.entries = json.loads((ROOT / selection.MOD_JSON).read_text())["entrypoints"]["fabric-client-gametest"]
        cls.tests = selection.test_classes(cls.entries)

    def test_every_registered_source_is_found(self):
        for entry in self.entries:
            with self.subTest(entry=entry):
                self.assertTrue(self.tests[entry.rsplit(".", 1)[-1]]["source"].is_file())

    def test_companion_code_selects_its_runtime_regressions(self):
        shown = selection.showing("PeepoEntity", self.tests)
        self.assertIn("PeepoWorkSessionClientTests", shown)
        self.assertIn("PeepoCompanionClientTests", shown)
        self.assertIn("PeepoDeliveryClientTests", shown)

    def test_full_run_assigns_every_class_exactly_once(self):
        jobs = selection.share(set(self.tests), self.tests, 5)
        assigned = [name for _, names in jobs for name in names]
        self.assertCountEqual(assigned, self.tests)
        self.assertEqual(len(assigned), len(set(assigned)))
        # Keep estimated execution below 25 minutes; the 60-minute job limit also covers setup and slow runners.
        self.assertLess(max(seconds for seconds, _ in jobs), 25 * 60)
        self.assertNotEqual(
            next(i for i, (_, names) in enumerate(jobs) if "PeepoCompanionClientTests" in names),
            next(i for i, (_, names) in enumerate(jobs) if "BiomeClientGameTests" in names),
        )

    def test_unweighted_companion_uses_its_actual_source_length(self):
        name = "PeepoWorkSessionClientTests"
        old = selection.WEIGHTS.pop(name)
        try:
            seconds = selection.share({name}, self.tests, 1)[0][0]
            self.assertGreater(seconds, 8)
        finally:
            selection.WEIGHTS[name] = old

    def test_shared_helper_change_without_feature_names_retains_consumers(self):
        path = "src/main/java/io/github/jimbozoomer/jugcraft/agriculture/JugcraftAgriculture.java"
        tests = {name: {"classes": {"JugcraftAgriculture"}, "ids": set()} for name in ("Farm", "Kitchen", "Flowers", "Crops")}
        with patch.object(selection, "git", return_value=path), \
                patch.object(selection, "added_only", return_value=None), \
                patch.object(selection, "changed_code", return_value=["return value + 1;"]), \
                patch.object(selection, "users_of", return_value={}):
            chosen, _ = selection.select([path], tests, set(), "base", "head")
        self.assertEqual(chosen, set(tests))


if __name__ == "__main__":
    unittest.main()
