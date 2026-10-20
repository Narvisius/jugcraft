"""The paid tap upgrade must not hide duplication or multi-recipe cycles."""
from copy import deepcopy
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import check_mod_data as checker


class AgricultureRecipeLoopTests(unittest.TestCase):
    def setUp(self):
        self.upgrade = deepcopy(next(r for r in checker.ag.SHAPED if r["id"] == "tree_tap_bucket"))

    def problems(self, shaped=(), shapeless=(), pots=None, cooking=None, cutting=None):
        with patch.multiple(checker.ag, SHAPED=list(shaped), SHAPELESS=list(shapeless),
                            POT_RECIPES=pots or {}, COOKING=cooking or {}, UNPACKING=()), \
             patch.object(checker.kitchen, "CUTTING", cutting or {}), \
             patch.object(checker, "err") as err:
            checker.check_agriculture_recipe_loops()
        return [call.args[0] for call in err.call_args_list]

    def test_current_agriculture_graph_passes(self):
        with patch.object(checker, "err") as err:
            checker.check_agriculture_recipe_loops()
        err.assert_not_called()

    def test_exact_paid_upgrade_passes(self):
        self.assertEqual(self.problems(shaped=[self.upgrade]), [])

    def test_modified_upgrade_cannot_bypass_loop_detection(self):
        mutations = {
            "duplicates_taps": {"count": 2},
            "missing_bucket": {"pattern": ["T"], "key": {"T": "jugcraft:tree_tap"}},
            "unused_bucket_key": {"pattern": ["T"]},
            "cheaper_payment": {"key": {"T": "jugcraft:tree_tap", "B": "minecraft:dirt"}},
            "missing_components": {"components": {}},
            "not_upgraded": {"components": {"minecraft:block_state": {"upgraded": "false"}}},
            "unrelated_components": {"components": {"minecraft:custom_name": "Upgraded"}},
        }
        for label, changes in mutations.items():
            with self.subTest(label=label):
                recipe = deepcopy(self.upgrade)
                recipe.update(changes)
                self.assertIn("Agriculture recipes form a loop through tree_tap",
                              self.problems(shaped=[recipe]))

    def test_other_tap_self_loop_is_not_hidden(self):
        recipe = {"id": "duplicate_taps", "inputs": ["jugcraft:tree_tap"],
                  "result": "jugcraft:tree_tap", "count": 2}
        self.assertTrue(self.problems(shaped=[self.upgrade], shapeless=[recipe]))

    def test_unrelated_component_recipe_is_not_exempt(self):
        recipe = deepcopy(self.upgrade)
        recipe.update(id="other_upgrade", result="jugcraft:other")
        recipe["key"]["T"] = "jugcraft:other"
        self.assertIn("Agriculture recipes form a loop through other", self.problems(shaped=[recipe]))

    def test_upgrade_payment_edge_still_detects_a_return_route(self):
        reverse = {"id": "recover_bucket", "inputs": ["jugcraft:tree_tap"],
                   "result": "minecraft:bucket", "count": 1}
        problems = self.problems(shaped=[self.upgrade], shapeless=[reverse])
        self.assertIn("Agriculture recipes form a loop through bucket", problems)
        self.assertIn("Agriculture recipes form a loop through tree_tap", problems)

    def test_cycles_across_all_conversion_types_remain_detected(self):
        problems = self.problems(
            shaped=[{"key": {"A": "jugcraft:a"}, "result": "jugcraft:b"}],
            shapeless=[{"inputs": ["jugcraft:b"], "result": "jugcraft:c"}],
            pots={"d": {"inputs": {"jugcraft:c": 1}}},
            cooking={"e": {"input": "d"}},
            cutting={"cut_e": {"input": "jugcraft:e", "results": [("jugcraft:a", 1)]}},
        )
        self.assertEqual(set(problems), {f"Agriculture recipes form a loop through {x}" for x in "abcde"})

    def test_full_agriculture_check_uses_loop_check(self):
        with patch.object(checker, "err") as err, \
             patch.object(checker, "check_agriculture_recipe_loops", wraps=checker.check_agriculture_recipe_loops) as loops:
            checker.check_agriculture()
        loops.assert_called_once_with()
        err.assert_not_called()
