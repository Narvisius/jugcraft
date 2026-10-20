"""Batch 6 placement balance, renewable routes, real fruit models and bounded scope."""
import json
from pathlib import Path
import sys
import unittest
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import agriculture as ag
import biomes
import biomes_data
import trees
import trees_data
import machines
import evergreen_scrub_textures as art


def shares(name):
    spec = biomes.BIOMES[name]["trees"]
    result, left = {}, 1.
    for feature, chance in spec["picks"]:
        result[feature] = left * chance
        left *= 1 - chance
    result[spec["default"]] = left
    return result


class TreeBatchSixTests(unittest.TestCase):
    def test_absolute_shares_and_density(self):
        expected = {
            "canyon": ([2,3], {"jugcraft:juniper_checked":.35,"jugcraft:juniper_bush_checked":.25,"jugcraft:pinyon_checked":.4}),
            "dryland": ([2,3], {"jugcraft:juniper_checked":.3,"jugcraft:juniper_bush_checked":.25,"jugcraft:pinyon_checked":.45}),
            "xeric_shrubland": ([1,2], {"jugcraft:juniper_checked":.25,"jugcraft:desert_acacia_checked":.45,"jugcraft:joshua_tree_checked":.3}),
            "scrubland": ([1,2], {"jugcraft:juniper_checked":.15,"jugcraft:oak_bush_checked":.25,"jugcraft:olive_bush_checked":.6}),
            "field": ([2,3], {"jugcraft:juniper_column_checked":.3,"minecraft:oak_bees_002":.035,"jugcraft:hawthorn_checked":.1,"jugcraft:oak_bush_checked":.015,"jugcraft:hawthorn_bush_checked":.55}),
            "prairie": ([1,2], {"jugcraft:juniper_column_checked":.15,"jugcraft:cottonwood_checked":.1,"minecraft:fancy_oak_checked":.05,"jugcraft:oak_bush_checked":.7}),
            "heathland": ([2,3], {"jugcraft:gorse_checked":.6,"jugcraft:juniper_bush_checked":.15,"jugcraft:umbrella_pine_checked":.25}),
            "shrubland": ([4,6], {"jugcraft:gorse_checked":.2,"jugcraft:juniper_bush_checked":.1,"jugcraft:oak_bush_checked":.1,"jugcraft:hawthorn_bush_checked":.6}),
            "steppe": ([1,2], {"jugcraft:sagebrush_checked":.8,"jugcraft:juniper_bush_checked":.2}),
        }
        for name,(count,wanted) in expected.items():
            self.assertEqual(biomes.BIOMES[name]["trees"]["count"],count)
            actual=shares(name)
            self.assertEqual(actual.keys(),wanted.keys())
            for feature,share in wanted.items(): self.assertAlmostEqual(actual[feature],share,places=8)
        self.assertAlmostEqual(shares("dead_forest")["jugcraft:juniper_checked"],.09999)
        self.assertNotIn("minecraft:oak_checked",shares("dead_forest"))
        self.assertAlmostEqual(shares("tundra")["jugcraft:juniper_bush_checked"],.1)
        self.assertAlmostEqual(shares("tundra")["jugcraft:willow_bush_checked"],.3)
        self.assertAlmostEqual(shares("tundra")["jugcraft:fir_bush_checked"],.19999)

    def test_cold_desert_is_coarse_dirt_only_and_steppe_keeps_plains_crops(self):
        placement=biomes.EXTRAS["lone_juniper"]["placement"]
        self.assertEqual(placement[0]["chance"],6)
        self.assertIn({"type":"minecraft:matching_blocks","blocks":"minecraft:coarse_dirt","offset":[0,-1,0]},placement[-1]["predicate"]["predicates"])
        self.assertEqual([name for name,b in biomes.BIOMES.items() if "lone_juniper" in b.get("extras",[])],["cold_desert"])
        self.assertIn("c:is_plains",biomes.BIOMES["steppe"]["tags"])
        self.assertIn("jugcraft:trees_steppe",biomes_data.steps("steppe")[9])

    def test_foliage_has_real_fruit_and_no_phantom_woods(self):
        generated={}
        trees_data.assets(Path("assets"),lambda p,d:generated.__setitem__(p.as_posix(),d))
        variants=generated["assets/blockstates/juniper_needles.json"]["variants"]
        self.assertEqual(set(variants),{"fruit=0","fruit=1","fruit=2"})
        textures=art.textures()
        for variant in variants.values():
            self.assertIn(("block",variant["model"].split("/")[-1]),textures)
        self.assertEqual(len(ag.wood_blocks("juniper","Juniper")),9)
        for tree,wood in (("gorse","minecraft:oak"),("sagebrush","dead")):
            self.assertNotIn(tree,ag.WOOD_SETS)
            self.assertEqual(ag.timber(tree),wood)
            farm=next(r for r in machines._tree_farm() if r["input"]==f"jugcraft:{tree}_sapling")
            self.assertEqual(farm["count"],1)
            self.assertIn([f"jugcraft:{tree}_sapling",1,1.,None],farm["byproducts"])
        self.assertIn("minecraft:flowers",ag.TREES["gorse"]["leaf_tags"])
        self.assertIn("minecraft:bee_attractive",ag.TREES["gorse"]["leaf_block_tags"])
        for image in textures.values(): self.assertEqual(image.size,(16,16))
        for stage in ("_unripe","_ripe"):
            self.assertNotEqual(textures["block","juniper_needles"].tobytes(),textures["block","juniper_needles"+stage].tobytes())

    def test_ripe_only_loot_and_existing_spice_recipe_retained(self):
        generated={}
        trees_data.loot(Path("loot"),lambda p,d:generated.__setitem__(p.name,d))
        pool=generated["juniper_needles.json"]["pools"][-1]
        self.assertEqual(pool["condition"]["terms"][1]["state"],{"fruit":"2"})
        self.assertEqual(pool["condition"]["terms"][0],{"type":"minecraft:inverted","term":trees_data.SHEARS_OR_SILK})
        recipes={r["id"]:r for r in ag.SHAPELESS}
        self.assertEqual(recipes["mulling_spices"]["inputs"],["minecraft:sugar","minecraft:sweet_berries","minecraft:cocoa_beans"])
        self.assertEqual(recipes["mulling_spices_from_juniper"]["inputs"],["minecraft:sugar","jugcraft:juniper_berries","minecraft:cocoa_beans"])
        self.assertEqual(recipes["mulling_spices_from_juniper"]["count"],2)

    def test_juniper_supports_alpine(self):
        self.assertEqual(ag.WOOD_SWITCHES["juniper"],["biomes", "alpine_spawn"])

if __name__ == "__main__": unittest.main()
