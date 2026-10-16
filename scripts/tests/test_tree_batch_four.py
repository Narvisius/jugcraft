"""Timber/foliage separation and exact biome selections, independent of terrain placement success."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import agriculture as ag
import trees_data
import machines
from tree_roster_textures import wonder_textures


class TreeBatchFourTests(unittest.TestCase):
    def test_exact_biome_attempt_shares_and_single_canopy(self):
        expected = {
            "snowpetal_grove": {"jugcraft:snowpetal_cherry_checked": .75, "jugcraft:aspen_checked": .25},
            "frostlight_garden": {"jugcraft:shimmer_birch_checked": .4, "jugcraft:fir_checked": .6},
            "starlit_wood": {"jugcraft:tall_shimmer_birch_checked": .45, "minecraft:super_birch_bees": .30, "jugcraft:tall_fir_checked": .25},
            "glimmer_grove": {"jugcraft:shimmer_birch_checked": .2, "jugcraft:jacaranda_checked": .55,
                              "jugcraft:big_jacaranda_checked": .15, "jugcraft:huge_red_mushroom_on_soil": .1},
        }
        folder = ROOT / "src/main/resources/data/jugcraft/worldgen"
        for biome, wanted in expected.items():
            data = json.loads((folder / "feature" / f"trees_{biome}.json").read_text())
            shares, left = {}, 1.0
            for pick in data["features"]:
                shares[pick["feature"]] = left * pick["chance"]
                left *= 1 - pick["chance"]
            shares[data["default"]] = left
            self.assertEqual(set(shares), set(wanted))
            for shape, share in wanted.items(): self.assertAlmostEqual(shares[shape], share)
            features = [f for step in json.loads((folder / "biome" / f"{biome}.json").read_text())["features"] for f in step]
            self.assertEqual([f for f in features if ":trees_" in f], [f"jugcraft:trees_{biome}"])

    def test_shared_timber_keeps_own_foliage_survival_drops_and_farm_output(self):
        folder = ROOT / "src/main/resources/data/jugcraft"
        recipes = machines._tree_farm()
        for tree, wood in (("snowpetal_cherry", "cherry"), ("shimmer_birch", "birch")):
            self.assertNotIn(tree, ag.WOOD_SETS)
            self.assertEqual(ag.switch_of(ag.sapling(tree)), ["biomes"])
            self.assertEqual(trees_data.tree_blocks(f"minecraft:{wood}", tree),
                             (f"minecraft:{wood}_log", f"jugcraft:{tree}_leaves", None))
            for shape in (tree, "tall_shimmer_birch") if tree == "shimmer_birch" else (tree,):
                feature = json.loads((folder / "worldgen/feature" / f"{shape}.json").read_text())
                self.assertEqual(feature["trunk_provider"]["id"], f"minecraft:{wood}_log")
                self.assertEqual(feature["foliage_provider"]["id"], f"jugcraft:{tree}_leaves")
                placed = json.loads((folder / "worldgen/placed_feature" / f"{shape}_checked.json").read_text())
                self.assertEqual(placed["placement"][0]["predicate"]["state"], f"jugcraft:{tree}_sapling")
            recipe = next(r for r in recipes if r["input"] == f"jugcraft:{tree}_sapling")
            self.assertEqual((recipe["output"], recipe["count"], recipe["ticks"]), (f"minecraft:{wood}_log", 6, 400))
            self.assertIn([f"jugcraft:{tree}_sapling", 1, 1.0, None], recipe["byproducts"])
            loot = (folder / "loot_table/blocks" / f"{tree}_leaves.json").read_text()
            self.assertIn(f"jugcraft:{tree}_sapling", loot)
            self.assertNotIn(f"minecraft:{wood}_sapling", loot)

    def test_foliage_art_has_dense_white_blossoms_and_sparse_aurora(self):
        images = wonder_textures()
        for image in images.values(): self.assertEqual(image.size, (16, 16))
        snow = images[("block", "snowpetal_cherry_leaves")]
        self.assertLessEqual(sum(a == 0 for *_, a in snow.getdata()), 8)
        self.assertGreaterEqual(sum(a == 0 for *_, a in snow.getdata()), 1)
        shimmer = images[("block", "shimmer_birch_leaves")]
        self.assertEqual(sum((r, g, b) in ((122,82,136), (160,112,176), (196,154,208))
                             for r,g,b,a in shimmer.getdata() if a), 12)


if __name__ == "__main__": unittest.main()
