"""Holly placement, year-round appearance, acquisition and bounded batch scope."""
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import agriculture as ag
import biomes
import trees
import trees_data
import holly_textures
import machines


def shares(spec):
    result, left = {}, 1.0
    for name, chance in spec["picks"]:
        result[name] = left * chance
        left *= 1 - chance
    result[spec["default"]] = left
    return result


class HollyTests(unittest.TestCase):
    def test_biome_shares_and_existing_species_preserved(self):
        expected = {
            "woodland": ([9, 10], {"minecraft:fancy_oak_leaf_litter": .25, "jugcraft:hawthorn_bush_checked": .04,
                "jugcraft:oak_bush_checked": .035, "minecraft:fallen_oak_tree": .675 * .03,
                "jugcraft:elder_checked": .04, "jugcraft:holly_checked": .04,
                "jugcraft:walnut_checked": .032, "jugcraft:large_walnut_checked": .008,
                "minecraft:oak_leaf_litter": .675 * .97 - .08 - .04}),
            "dense_forest": ([14, 16], {"minecraft:oak_checked": .3, "minecraft:dark_oak_checked": .07,
                "jugcraft:oak_bush_checked": .63 * .1111 - .05, "jugcraft:holly_checked": .05,
                "jugcraft:great_oak_checked": .63 * (1 - .1111) * .0357,
                "jugcraft:walnut_checked": .08, "jugcraft:large_walnut_checked": .02,
                "minecraft:fancy_oak_checked": .63 * (1 - .1111) * (1 - .0357) - .10}),
        }
        for biome, (count, wanted) in expected.items():
            self.assertEqual(biomes.BIOMES[biome]["trees"]["count"], count)
            actual = shares(biomes.BIOMES[biome]["trees"])
            self.assertEqual(actual.keys(), wanted.keys())
            for name, share in wanted.items(): self.assertAlmostEqual(actual[name], share, places=10)
        self.assertEqual({name for name, spec in biomes.BIOMES.items()
                          if spec.get("trees") and "jugcraft:holly_checked" in shares(spec["trees"])},
                         {"woodland", "dense_forest"})

    def test_evergreen_berry_variants_and_clipped_appearance(self):
        generated = {}
        trees_data.assets(Path("assets"), lambda p,d: generated.__setitem__(p.as_posix(), d))
        variants = generated["assets/blockstates/holly_leaves.json"]["variants"]
        self.assertEqual(len(variants), 6)
        green = variants["clipped=false,season=green"]
        self.assertEqual({v["model"]: v["weight"] for v in green}, {"jugcraft:block/holly_leaves": 2, "jugcraft:block/holly_leaves_berries": 1})
        for season in ("green", "gold", "bare"):
            self.assertEqual(variants[f"clipped=true,season={season}"], {"model": "jugcraft:block/holly_leaves"})
        for season in ("gold", "bare"):
            self.assertEqual(variants[f"clipped=false,season={season}"], {"model": "jugcraft:block/holly_leaves_berries"})
        art = holly_textures.textures()
        for img in art.values(): self.assertEqual(img.size, (16,16))
        plain, berries = (art["block", name] for name in ("holly_leaves", "holly_leaves_berries"))
        self.assertNotEqual(plain.tobytes(), berries.tobytes())
        self.assertGreater(sum(value > 0 for value in plain.getchannel("A").tobytes()), 220)

    def test_sprigs_feed_only_decoration_and_wood_is_renewable(self):
        self.assertNotIn("food", ag.ITEMS["holly_sprig"])
        recipe = next(r for r in ag.SHAPED if r["id"] == "holly_wreath")
        self.assertEqual(recipe["pattern"], ["HHH", "HSH", "HHH"])
        self.assertEqual(recipe["key"], {"H": "jugcraft:holly_sprig", "S": "minecraft:string"})
        self.assertEqual(len(ag.wood_blocks("holly", "Holly")), 9)
        farm = next(r for r in machines._tree_farm() if r["input"] == "jugcraft:holly_sapling")
        self.assertEqual(farm["count"], 6)
        self.assertIn(["jugcraft:holly_sapling", 1, 1.0, None], farm["byproducts"])
        self.assertEqual(ag.WOOD_SWITCHES["holly"], ["biomes"])


if __name__ == "__main__": unittest.main()
