"""Client selection must discover every registered package and preserve all shards' coverage."""
import importlib.util
import json
import re
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
        jobs = selection.share(set(self.tests), self.tests, 16)
        assigned = [name for _, names in jobs for name in names]
        self.assertCountEqual(assigned, self.tests)
        self.assertEqual(len(assigned), len(set(assigned)))
        # The all-gun gallery takes about 30 minutes on its own. Retain setup/slow-runner
        # headroom under the unchanged 60-minute CI limit without understating that weight.
        durations = [seconds for seconds, _ in jobs]
        self.assertLess(max(durations), 40 * 60)
        self.assertLessEqual(max(durations), max(sum(durations) / len(durations) + 30, max(selection.WEIGHTS.values())))
        gun_job = next(names for _, names in jobs if "GunsClientGameTests" in names)
        self.assertEqual(gun_job, ["GunsClientGameTests"])
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

    def test_workflow_runs_every_selected_shard(self):
        workflow = (ROOT / ".github/workflows/build.yml").read_text()
        matrix = re.search(r"shard: \[([\d, ]+)\]", workflow)
        self.assertIsNotNone(matrix)
        indices = [int(value) for value in matrix.group(1).split(",")]
        self.assertEqual(indices, list(range(16)))
        outputs = {int(value) for value in re.findall(r"^\s+shard(\d+):", workflow, re.MULTILINE)}
        self.assertEqual(outputs, set(indices))
        self.assertEqual(re.findall(r"--shards (\d+)", workflow), [str(len(indices))] * 2)

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
