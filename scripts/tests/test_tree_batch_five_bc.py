"""Exact selector shares, generated loot, shared timber and save compatibility for 5b/5c."""
import json
from pathlib import Path
import re
import sys
import unittest
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import agriculture as ag
import biomes
import trees_data
import machines
import hawthorn_persimmon_textures as art

class TreeBatchFiveBCTests(unittest.TestCase):
    def test_selector_shares_keep_open_land_and_existing_density(self):
        expected = {
            "field": ([2,3], {"hawthorn_bush_checked":.55, "hawthorn_checked":.10, "oak_bush_checked":.015}),
            # Batch 6 fills the reserved 30% while preserving both approved batch-5c shares.
            "shrubland": ([4,6], {"hawthorn_bush_checked":.60, "oak_bush_checked":.10, "gorse_checked":.20, "juniper_bush_checked":.10}),
            "woodland": ([9,10], {"hawthorn_bush_checked":.04, "oak_bush_checked":.035, "elder_checked":.04}),
            "seasonal_forest": ([10,11], {"persimmon_checked":.10})}
        for biome, (density, expected_shares) in expected.items():
            info = biomes.BIOMES[biome]["trees"]
            left, shares = 1., {}
            for feature, chance in info["picks"]:
                shares[feature] = left * chance
                left *= 1 - chance
            shares[info["default"]] = left
            self.assertEqual(info["count"], density)
            for name, share in expected_shares.items():
                self.assertAlmostEqual(shares["jugcraft:"+name], share, places=8)
        self.assertIsNone(biomes.BIOMES["highland"]["trees"])
        self.assertEqual(biomes.EXTRAS["lone_hawthorn"]["placement"][0]["chance"],16)
        self.assertEqual(biomes.EXTRAS["lone_rowan"]["placement"][0]["chance"],8)
        self.assertEqual(biomes.EXTRAS["open_scrub"]["configured"], {"type":"minecraft:no_op"})

    def test_haws_drop_only_when_ripe_without_shears_or_silk(self):
        generated = {}
        trees_data.loot(Path("loot"), lambda p,d: generated.__setitem__(p.name,d))
        pools = generated["hawthorn_leaves.json"]["pools"][2:]
        self.assertEqual(len(pools),1)
        pool=pools[0]
        terms=pool["condition"]["terms"]
        self.assertEqual(terms[1]["state"], {"fruit":"2"})
        self.assertEqual(terms[0], {"type":"minecraft:inverted", "term":trees_data.SHEARS_OR_SILK})
        self.assertEqual(pool["entries"][0]["name"],"jugcraft:haws")
        self.assertEqual(pool["entries"][0]["modifier"][0]["count"], {"type":"minecraft:uniform","min":1,"max":2})
        self.assertTrue(ag.TREES["hawthorn"]["fruit"]["exposed_sides"])

    def test_models_art_and_shared_timber_are_complete(self):
        generated={}
        trees_data.assets(Path("assets"),lambda p,d:generated.__setitem__(p.as_posix(),d))
        textures=art.textures()
        for tree, count in (("hawthorn",9),("persimmon",9)):
            variants=generated[f"assets/blockstates/{tree}_leaves.json"]["variants"]
            self.assertEqual(len(variants),count)
            for variant in variants.values():
                name=variant["model"].split("/")[-1]
                self.assertIn(("block",name),textures)
            recipe=next(r for r in machines._tree_farm() if r["input"]==f"jugcraft:{tree}_sapling")
            self.assertEqual(recipe["output"],"jugcraft:"+("fruitwood" if tree=="hawthorn" else tree)+"_log")
            self.assertIn([f"jugcraft:{tree}_sapling",1,1.,None],recipe["byproducts"])
        self.assertNotIn("hawthorn",ag.WOOD_SETS)
        self.assertEqual(len(ag.wood_blocks("persimmon","Persimmon")),9)
        self.assertEqual(len(art.CENTRES["persimmon"]),5)
        for suffix in ("", "_gold", "_bare"):
            base = "hawthorn_leaves" + suffix
            self.assertNotEqual(textures["block",base].tobytes(), textures["block",base+"_ripe"].tobytes())
            self.assertNotEqual(textures["block",base+"_unripe"].tobytes(), textures["block",base+"_ripe"].tobytes())

    def test_old_pie_ordinals_are_preserved(self):
        source=(ROOT/"src/main/java/io/github/jimbozoomer/jugcraft/agriculture/PieFilling.java").read_text(encoding="utf-8")
        names=re.findall(r'^\t([A-Z_]+)\(',source,re.M)
        old="APPLE PUMPKIN_CREAM CRANBERRY SWEET_POTATO CHESTNUT CHOCOLATE SWEET_BERRY PEACH LEMON CARROT_CAKE BIRTHDAY_CAKE ICE_CREAM_CAKE RED_VELVET_CAKE CHEESECAKE COFFEE_CAKE APPLE_CAKE STRAWBERRY_PIE BLUEBERRY_TART PLUM_PIE BANOFFEE_PIE SWEET_BERRY_TART LEMON_TART WHIPPED_PUMPKIN_PIE PORK_PIE STRAWBERRY_TART COFFEE_TART".split()
        self.assertEqual(names,old+["PERSIMMON"])
        self.assertEqual(list(ag.PIES["fillings"])[-1],"persimmon")

if __name__ == "__main__": unittest.main()
