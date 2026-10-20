"""Protect the intended shares against Minecraft's sequential selector probabilities."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/jugcraft/worldgen"


def shares(name):
    tree = json.loads((DATA / "feature" / f"trees_{name}.json").read_text(encoding="utf-8"))
    remainder, out = 1.0, {}
    for pick in tree["features"]:
        out[pick["feature"]] = remainder * pick["chance"]
        remainder *= 1 - pick["chance"]
    out[tree["default"]] = remainder
    return out


class TreeBiomeMixTests(unittest.TestCase):
    def test_batch_two_shares(self):
        intended = {
            "floodplain": {"cottonwood": .30}, "prairie": {"cottonwood": .10},
            "lush_river": {"cottonwood": .10, "willow": .20},
            "hot_springs": {"pine": .55, "japanese_maple": .30},
            "subtropics": {"live_oak": .30}, "bayou": {"live_oak": .15},
            "shield": {"pine": .25}, "karst_pinnacles": {"pine": .65},
            "jacaranda_glade": {"big_jacaranda": .15}, "glimmer_grove": {"big_jacaranda": .15},
            "outback": {"mallee": .30, "desert_acacia": .50, "baobab": .10, "giant_baobab": .10},
            "eucalyptus_forest": {"eucalyptus": .42, "tree_fern": .05, "big_eucalyptus": .25, "wattle_bush": .20, "giant_eucalyptus": .08},
            "lush_desert": {"desert_acacia": .45, "small_palm": .20, "joshua_tree": .35},
            "flower_isle": {"apple_tree": .15}, "lavender_field": {"cypress": .10},
            "lush_grassland": {"small_palm": .10}, "oasis": {"small_palm": .15},
            "overgrown_beach": {"palm": .20, "small_palm": .20},
        }
        for biome, picks in intended.items():
            actual = shares(biome)
            self.assertAlmostEqual(sum(actual.values()), 1.0)
            for tree, share in picks.items():
                with self.subTest(biome=biome, tree=tree):
                    self.assertAlmostEqual(actual[f"jugcraft:{tree}_checked"], share, places=6)

    def test_existing_citrus_and_shield_companions_keep_their_shares(self):
        subtropics = shares("subtropics")
        self.assertAlmostEqual(subtropics["jugcraft:lemon_checked"], .7 * .9 * .8 * .9 * .15)
        self.assertAlmostEqual(subtropics["jugcraft:orange_checked"], .7 * .9 * .8 * .9 * .85 * .15)
        shield = shares("shield")
        self.assertAlmostEqual(shield["jugcraft:aspen_checked"], .75 * (1 - .1867) * .1311)
        self.assertAlmostEqual(shield["jugcraft:larch_checked"], .75 * (1 - .1867) * (1 - .1311) * .1509)

    def test_desert_grass_islands_have_matching_dirt_below(self):
        rule = json.loads((DATA / "material_rule/overworld/surface.json").read_text(encoding="utf-8"))
        desert = next(r for r in rule["sequence"] if r["if_true"]["biome_is"] == "jugcraft:lush_desert")
        floor, under = desert["then_run"]["sequence"]
        grass = floor["then_run"]["sequence"][0]
        dirt = under["then_run"]["sequence"][0]
        self.assertEqual(grass["if_true"], dirt["if_true"])
        self.assertEqual(grass["then_run"]["result_state"], "minecraft:grass_block")
        self.assertEqual(dirt["then_run"]["result_state"], "minecraft:dirt")


if __name__ == "__main__":
    unittest.main()
