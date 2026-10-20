"""Placement shares, finite footprints and compatibility with the previously approved trees."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import agriculture as ag
import biomes
import trees_data
import machines
import wood_style


class TreeBatchFourBCTests(unittest.TestCase):
    def test_exact_tree_shares_density_and_no_second_canopy(self):
        expected = {
            "bayou": ({"bald_cypress": .60, "willow": .25, "live_oak": .15}, [5, 6], 3),
            "lush_swamp": ({"bald_cypress": .25, "tall_vine_oak": .35, "willow": .30, "maple": .10}, [3, 4], 2),
            "swamp_woods": ({"bald_cypress": .25, "willow": .45, "tall_vine_oak": .20, "azalea_tree": .10}, [7, 9], 2),
            "webwood": ({"bald_cypress": .15, "willow": .57, "dead_tree": .20, "tall_vine_oak": .08}, [6, 7], 0),
            "flower_isle": ({"magnolia": .55, "oak_bush": .30, "apple_tree": .15}, [1, 2], 0),
        }
        root = ROOT / "src/main/resources/data/jugcraft/worldgen"
        for biome, (wanted, count, depth) in expected.items():
            data = json.loads((root / "feature" / f"trees_{biome}.json").read_text())
            left, shares = 1., {}
            for pick in data["features"]:
                shares[pick["feature"]] = left * pick["chance"]
                left *= 1 - pick["chance"]
            shares[data["default"]] = left
            self.assertEqual(set(shares), {f"jugcraft:{tree}_checked" for tree in wanted})
            for tree, share in wanted.items(): self.assertAlmostEqual(shares[f"jugcraft:{tree}_checked"], share)
            trees = biomes.BIOMES[biome]["trees"]
            self.assertEqual(trees["count"], count)
            self.assertEqual(trees.get("water_depth", 0), depth)
            steps = json.loads((root / "biome" / f"{biome}.json").read_text())["features"]
            self.assertEqual([f for step in steps for f in step if ":trees_" in f], [f"jugcraft:trees_{biome}"])

    def test_new_roots_and_branching_pass_through_without_corrupting_old_features(self):
        generated = {}
        trees_data.worldgen(Path("data"), lambda path, data: generated.__setitem__(path.as_posix(), data))
        cypress = generated["data/jugcraft/worldgen/feature/bald_cypress.json"]
        root = cypress["root_placer"]
        self.assertEqual(root["trunk_offset_y"], 1)
        self.assertEqual(root["mangrove_root_placement"]["max_root_width"], 2)
        self.assertEqual(root["mangrove_root_placement"]["max_root_length"], 3)
        self.assertEqual(root["mangrove_root_placement"]["can_grow_through"], [])
        self.assertEqual(root["mangrove_root_placement"]["muddy_roots_in"], [])
        self.assertEqual(cypress["trunk_placer"]["can_grow_through"], "#minecraft:mangrove_logs_can_grow_through")
        self.assertEqual(cypress["trunk_placer"]["place_branch_per_log_probability"], .15)
        for path, data in generated.items():
            if "/feature/" in path and path.endswith(".json") and Path(path).stem not in ("bald_cypress", "banyan"):
                self.assertNotIn("root_placer", data)

    def test_each_species_has_renewable_wood_and_matching_seasonal_art(self):
        textures = wood_style.wood_textures()
        for tree in ("bald_cypress", "magnolia"):
            self.assertIn(tree, ag.WOOD_SETS)
            self.assertEqual(ag.WOOD_SWITCHES[tree], ["biomes"])
            self.assertEqual(ag.tree_switches(tree), ["biomes"])
            recipe = next(r for r in machines._tree_farm() if r["input"] == f"jugcraft:{tree}_sapling")
            self.assertEqual((recipe["output"], recipe["count"]), (f"jugcraft:{tree}_log", 6))
            self.assertIn([f"jugcraft:{tree}_sapling", 1, 1.0, None], recipe["byproducts"])
            for kind, name in textures:
                if tree in name: self.assertEqual(textures[kind, name].size, (16, 16))
        green = textures["block", "bald_cypress_needles"]
        autumn = textures["block", "bald_cypress_needles_gold"]
        self.assertEqual(list(green.getchannel("A").getdata()), list(autumn.getchannel("A").getdata()))
        flowers = {(184,150,46), (201,181,120), (221,210,178), (232,225,204)}
        self.assertEqual(sum(pixel[:3] in flowers for pixel in textures["block", "magnolia_leaves"].getdata()), 15)


if __name__ == "__main__": unittest.main()
