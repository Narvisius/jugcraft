"""Regional placement, saved-state coverage and seasonal artwork compatibility for batch 5."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import agriculture as ag
import biomes
import biomes_data
import machines
import seasonal_fruit_textures
import trees_data


class TreeBatchFiveTests(unittest.TestCase):
    def test_biome_shares_and_bounded_highland_extra(self):
        expected = {"snowy_forest": ("jugcraft:rowan_checked", .36, [7, 8]),
                    "lake_district": ("jugcraft:rowan_checked", .10, [6, 7]),
                    "woodland": ("jugcraft:elder_checked", .04, [9, 10]),
                    "wetland": ("jugcraft:elder_checked", .10, [2, 3])}
        for biome, (tree, share, density) in expected.items():
            info = biomes.BIOMES[biome]["trees"]
            left, shares = 1., {}
            for feature, chance in info["picks"]:
                shares[feature] = left * chance
                left *= 1 - chance
            shares[info["default"]] = left
            self.assertAlmostEqual(shares[tree], share, places=4)
            self.assertEqual(info["count"], density)
            self.assertEqual([f for step in biomes_data.steps(biome) for f in step if ":trees_" in f], ["jugcraft:trees_" + biome])
        self.assertIsNone(biomes.BIOMES["highland"]["trees"])
        self.assertNotIn("lone_rowan", biomes.BIOMES["elder_vale"].get("extras", []))
        placement = biomes_data.extra_placement("lone_rowan")
        self.assertEqual(placement[0], {"type": "minecraft:rarity_filter", "chance": 8})
        self.assertEqual(placement[-1]["predicate"], {"type": "minecraft:would_survive", "state": "jugcraft:rowan_sapling"})
        self.assertNotIn("minecraft:count", [p["type"] for p in placement])

    def test_all_season_fruit_models_and_same_fruit_positions(self):
        generated = {}
        trees_data.assets(Path("assets"), lambda path, data: generated.__setitem__(path.as_posix(), data))
        art = seasonal_fruit_textures.textures()
        for tree in ("rowan", "elder"):
            variants = generated[f"assets/blockstates/{tree}_leaves.json"]["variants"]
            self.assertEqual(set(variants), {f"fruit={fruit},season={season}" for fruit in range(3) for season in ag.SEASON_STATES})
            for stage in ("unripe", "ripe"):
                masks = []
                for suffix in ("", "_gold", "_bare"):
                    key = tree + "_leaves" + suffix
                    base, fruit = art[("block", key)], art[("block", key + "_" + stage)]
                    # The twig overlay can coincide with an existing bare twig; compare flowers/berries themselves.
                    masks.append({(x, y): fruit.getpixel((x, y)) for x in range(16) for y in range(16)
                                  if fruit.getpixel((x, y)) != base.getpixel((x, y)) and fruit.getpixel((x, y)) != (110, 101, 91, 255)})
                self.assertEqual(masks[0], masks[1])
                self.assertEqual(masks[0], masks[2])
            self.assertEqual(art[("block", tree + "_leaves")].getchannel("A").tobytes(), art[("block", tree + "_leaves_gold")].getchannel("A").tobytes())
            for variant in variants.values():
                name = variant["model"].split("/")[-1]
                self.assertIn(("block", name), art)
                self.assertTrue((ROOT / "src/main/resources/assets/jugcraft/textures/block" / (name + ".png")).is_file())

    def test_tree_farm_and_crafting_use_shared_timber(self):
        recipes = machines._tree_farm()
        for tree, wood in (("rowan", "jugcraft:fruitwood_log"), ("elder", "minecraft:oak_log")):
            farm = next(r for r in recipes if r["input"] == f"jugcraft:{tree}_sapling")
            self.assertEqual(farm["output"], wood)
            self.assertIn([f"jugcraft:{tree}_sapling", 1, 1., None], farm["byproducts"])
            self.assertEqual(ag.tree_switches(tree), ["biomes"])
        self.assertNotIn("rowan", ag.WOOD_SETS)
        self.assertNotIn("elder", ag.WOOD_SETS)
        self.assertEqual(len(ag.wood_blocks("fruitwood", "Fruitwood")), 9)

    def test_new_tree_placement_initializes_the_season(self):
        generated = {}
        trees_data.worldgen(Path("data"), lambda path, data: generated.__setitem__(path.as_posix(), data))
        for tree, wood in (("rowan", "jugcraft:fruitwood_log"), ("elder", "minecraft:oak_log")):
            feature = generated[f"data/jugcraft/worldgen/feature/{tree}.json"]
            self.assertEqual(feature["trunk_provider"]["id"], wood)
            self.assertEqual(feature["foliage_provider"]["id"], f"jugcraft:{tree}_leaves")
            self.assertIn({"type": "jugcraft:seasonal_leaves"}, feature["decorators"])
        self.assertEqual(generated["data/jugcraft/worldgen/feature/elder.json"]["trunk_placer"]["base_height"], 1)


if __name__ == "__main__": unittest.main()
