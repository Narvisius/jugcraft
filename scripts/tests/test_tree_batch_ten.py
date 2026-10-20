"""Real generated placement weights, renewable routes and hanging-fruit loot semantics."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import agriculture as ag
import biomes
import trees
import hanging_fruit_data as fruit
import hanging_fruit_textures


class TreeBatchTenTests(unittest.TestCase):
    def test_existing_biome_budgets_and_tamarisk_are_preserved(self):
        for biome, expected, count in (
            ("outback", {"desert_acacia": .5, "mallee": .3, "baobab": .1, "giant_baobab": .1}, [1, 2]),
            ("oasis", {"palm": .4, "small_palm": .15, "tamarisk": .1, "date_palm": .35}, [2, 3]),
            ("dry_river", {"tamarisk": .4, "date_palm": .6}, [1, 1]),
        ):
            data = json.loads((ROOT / f"src/main/resources/data/jugcraft/worldgen/feature/trees_{biome}.json").read_text())
            left, shares = 1., {}
            for pick in data["features"]:
                shares[pick["feature"]] = left * pick["chance"]
                left *= 1 - pick["chance"]
            shares[data["default"]] = left
            self.assertEqual(set(shares), {f"jugcraft:{tree}_checked" for tree in expected})
            for tree, chance in expected.items(): self.assertAlmostEqual(shares[f"jugcraft:{tree}_checked"], chance)
            self.assertEqual(biomes.BIOMES[biome]["trees"]["count"], count)

    def test_dates_share_palm_and_fruit_is_not_a_free_ripe_block(self):
        self.assertNotIn("date_palm", ag.WOOD_SETS)
        self.assertNotIn("date_palm", ag.TREES)
        self.assertEqual(ag.TREES["palm"]["sapling_shapes"], {"palm": 3, "date_palm": 2})
        self.assertEqual(ag.TREES["baobab"]["giant"], "giant_baobab")
        self.assertIn("jugcraft:fermentable", ag.ITEMS["dates"]["tags"])
        self.assertEqual(ag.ITEMS["baobab_seeds"]["plants"], "baobab_sapling")
        tables = {}; fruit.loot(Path("loot"), lambda p, v: tables.__setitem__(p.stem, v))
        for name, table in tables.items():
            children = table["pools"][0]["entries"][0]["children"]
            self.assertNotIn("modifier", children[0])  # Do not copy the ripe AGE into the preserved item.
            self.assertEqual(children[1]["condition"]["type"], "minecraft:match_block")
            self.assertEqual(children[1]["condition"]["state"], {"age": "2"})
        for shape in ("baobab", "giant_baobab", "date_palm"):
            decorator = trees.SHAPES[shape]["decorators"][0]
            self.assertEqual(decorator["block_provider"]["properties"], {"age": "0"})
            self.assertEqual(decorator["directions"], ["down"])

    def test_generated_art_is_exact_and_fruit_stalk_reaches_the_canopy(self):
        from PIL import Image
        for (kind, name), expected in hanging_fruit_textures.textures().items():
            actual = Image.open(ROOT / f"src/main/resources/assets/jugcraft/textures/{kind}/{name}.png").convert("RGBA")
            self.assertEqual(expected.size, (16, 16))
            self.assertEqual(actual.tobytes(), expected.tobytes())
            if name.startswith(("baobab_pod_", "date_cluster_")):
                self.assertTrue(any(expected.getpixel((x, 0))[3] for x in range(16)), "Disconnected stalk")


if __name__ == "__main__": unittest.main()
