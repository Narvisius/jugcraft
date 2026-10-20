"""Batch 13's probability budget and saved-walnut compatibility contracts."""
import json
import sys
import unittest
from fractions import Fraction
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import biomes
import walnut_data

DATA = ROOT / "src/main/resources/data/jugcraft"


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def shares(picks):
    remaining = 1
    result = {}
    for name, chance in picks:
        result[name] = remaining * chance
        remaining *= 1 - chance
    result["default"] = remaining
    return result


class WalnutWorldgenTests(unittest.TestCase):
    def test_walnut_uses_only_default_slots_and_preserves_attempts(self):
        previous = {
            "dense_forest": (10, [14, 16], [
                ("minecraft:oak_checked", .3), ("minecraft:dark_oak_checked", .1),
                ("jugcraft:holly_checked", .05 / .63),
                ("jugcraft:oak_bush_checked", (.63 * .1111 - .05) / .58),
                ("jugcraft:great_oak_checked", .0357)]),
            "woodland": (4, [9, 10], [
                ("minecraft:fancy_oak_leaf_litter", .25), ("jugcraft:hawthorn_bush_checked", .04 / .75),
                ("jugcraft:oak_bush_checked", .035 / .71), ("minecraft:fallen_oak_tree", .03),
                ("jugcraft:elder_checked", .04 / (.675 * .97)),
                ("jugcraft:holly_checked", .04 / (.675 * .97 - .04))]),
        }
        for biome, (percent, count, old) in previous.items():
            current = biomes.BIOMES[biome]["trees"]
            self.assertEqual(current["count"], count)
            before, after = shares(old), shares(current["picks"])
            for name in before.keys() - {"default"}:
                self.assertAlmostEqual(before[name], after[name])
            self.assertAlmostEqual(after["jugcraft:walnut_checked"], percent / 100 * .8)
            self.assertAlmostEqual(after["jugcraft:large_walnut_checked"], percent / 100 * .2)
            self.assertAlmostEqual(before["default"] - after["default"], percent / 100)
            generated = read(DATA / f"worldgen/feature/trees_{biome}.json")
            self.assertEqual([(p["feature"], p["chance"]) for p in generated["features"]],
                             [tuple(p) for p in current["picks"]])

    def test_bristlecone_is_a_rare_basin_extra(self):
        self.assertIsNone(biomes.BIOMES["basin"]["trees"])
        placements = [name for name, info in biomes.BIOMES.items()
                      if "lone_bristlecone" in info.get("extras", [])]
        self.assertEqual(placements, ["basin"])
        extras = next(value for value in vars(biomes).values()
                      if isinstance(value, dict) and "lone_bristlecone" in value)
        definition = extras["lone_bristlecone"]
        self.assertEqual(definition["placement"][0], {"type": "minecraft:rarity_filter", "chance": 4})
        self.assertEqual(definition["feature"], "jugcraft:bristlecone")
        self.assertNotIn("minecraft:count", str(definition))

    def test_custom_feature_data_and_saved_alias(self):
        for shape in ("walnut", "large_walnut", "bristlecone"):
            self.assertEqual(read(DATA / f"worldgen/feature/{shape}.json"),
                             {"type": "jugcraft:walnut_tree", "shape": shape})
        ecology = (ROOT / "src/main/java/io/github/jimbozoomer/jugcraft/menagerie/Ecology.java").read_text()
        self.assertNotIn('case "walnut"->c.hasTag(BiomeTags.IS_TAIGA)', ecology)
        self.assertIn('name.equals("walnut_sapling") ? JugcraftAgriculture.WALNUT_GROWER', ecology)
        alias = (ROOT / "src/main/java/io/github/jimbozoomer/jugcraft/menagerie/EcologyTree.java").read_text()
        self.assertIn('if(wood.equals("walnut"))', alias)
        self.assertIn('WalnutTree(r.nextInt(5)==0?"large_walnut":"walnut")', alias)

    def test_recovered_recipes_converge_without_reverse_conversion(self):
        for form in walnut_data.FORMS:
            self.assertEqual(set(read(DATA / f"tags/item/walnut_compat/{form}.json")["values"]),
                             {"jugcraft:"+form, "jugcraft:ecology_"+form})
        for path in (DATA / "recipe/ecology").rglob("*walnut*.json"):
            recipe = read(path)
            self.assertNotIn("ecology_walnut_planks", json.dumps(recipe.get("result")))
            for ingredient in recipe.get("ingredients", []):
                if isinstance(ingredient, list):
                    self.assertTrue(all(not item.startswith("#") for item in ingredient),
                                    f"Native alternatives cannot contain nested tags: {path}")
        self.assertEqual(read(DATA / "recipe/walnut_husk_dye.json")["ingredients"], ["jugcraft:walnut_husk"])
        self.assertFalse((DATA / "recipe/walnut_compat").exists(), "Ambiguous one-item conversion recipes")
        source = read(ROOT / "src/main/resources/jugcraft/menagerie/ecology.json")
        for form in walnut_data.FORMS:
            self.assertIn("ecology_"+form, source["blocks"], "Removed saved registry ID")

    def test_both_leaf_ids_use_the_same_appearance(self):
        assets = ROOT / "src/main/resources/assets/jugcraft"
        self.assertEqual(read(assets/"blockstates/walnut_leaves.json"),
                         read(assets/"blockstates/ecology_walnut_leaves.json"))
        self.assertEqual(len(read(assets/"blockstates/walnut_leaves.json")["variants"]), 9)

    def test_legacy_conversion_and_husk_dye_cannot_create_essence(self):
        records = read(DATA/"alchemy/values.json")["items"]
        def price(item):
            value = records[item]
            return Fraction(value["numerator"], value["denominator"])
        for old, new in walnut_data.ALIASES.items():
            self.assertEqual(price(old), price(new))
        self.assertEqual(price("jugcraft:walnut_husk"), price("minecraft:brown_dye"))
        self.assertEqual(price("jugcraft:walnut_planks"), 8)


if __name__ == "__main__":
    unittest.main()
