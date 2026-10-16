"""Regression checks for tree drawing defaults and the Gloomweald canopy replacement."""
import hashlib
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import wood_style
from tree_roster_textures import cluster_centres

GOLDEN = {'block/maple_leaves': 'd4803bb596f337d4fe79f5539b6a4a45aa723b8d5ccd694554ca88789d8a8139', 'block/jacaranda_leaves': '8329432f74645e578d50d2952b6ccdd8613dc48cd4b85f1ac6eeb3126a563e57', 'block/palm_fronds': '1d10ca8d8a6484365fafaa955a7dd5423e98bd9de3b879af4257f0ed687f8c0f', 'block/cedar_leaves': 'e5854052ff0a064d0fd2f11c8c5fec5902801cf20ae6d7b0b570c5309dff5509'}


class TreeRosterTests(unittest.TestCase):
    def test_existing_leaf_art_keeps_its_pixels(self):
        images = wood_style.wood_textures()
        for key, digest in GOLDEN.items():
            self.assertEqual(hashlib.sha256(images[tuple(key.split("/"))].tobytes()).hexdigest(), digest, key)

    def test_leaf_options_are_seeded_and_clusters_wrap_without_colliding(self):
        palette = wood_style.LEAVES["yew_needles"][0]
        a = wood_style.paint_leaves(palette, "needles", 137, gap_threshold=.1)
        b = wood_style.paint_leaves(palette, "needles", 137, gap_threshold=.1)
        self.assertEqual(a.tobytes(), b.tobytes())
        self.assertNotEqual(a.tobytes(), wood_style.paint_leaves(palette, "needles", 137, gap_threshold=.4).tobytes())
        points = cluster_centres(9303)
        for i, p in enumerate(points):
            for q in points[i + 1:]:
                self.assertGreaterEqual(sum(min(abs(x-y), 16-abs(x-y))**2 for x,y in zip(p,q)), 16)
        self.assertNotEqual(points, cluster_centres(9304))

    def test_gloomweald_has_one_canopy_with_the_intended_mix(self):
        folder = ROOT / "src/main/resources/data/jugcraft/worldgen"
        biome = json.loads((folder / "biome/gloomweald.json").read_text(encoding="utf-8"))
        features = [f for step in biome["features"] for f in step]
        self.assertNotIn("minecraft:dark_forest_vegetation", features)
        self.assertEqual(features.count("jugcraft:trees_gloomweald"), 1)
        data = json.loads((folder / "feature/trees_gloomweald.json").read_text(encoding="utf-8"))
        remainder, shares = 1.0, {}
        for pick in data["features"]:
            shares[pick["feature"]] = remainder * pick["chance"]
            remainder *= 1-pick["chance"]
        shares[data["default"]] = remainder
        for key, expected in {"minecraft:dark_oak_checked":.45, "jugcraft:yew_checked":.20,
                "jugcraft:dead_tree_checked":.10, "jugcraft:dead_snag_checked":.10,
                "jugcraft:huge_red_mushroom_on_soil":.08, "jugcraft:huge_brown_mushroom_on_soil":.07}.items():
            self.assertAlmostEqual(shares[key], expected)


if __name__ == "__main__":
    unittest.main()
