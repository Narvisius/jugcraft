import json
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
class BoardResourcesTest(unittest.TestCase):
    def test_board_resources_resolve_without_missing_materials(self):
        for name in ("checkers_table", "chess_table"):
            state=json.loads((RES/f"assets/jugcraft/blockstates/{name}.json").read_text(encoding="utf-8"))
            ref=state["variants"][""]["model"].split(":",1)[1]
            model=json.loads((RES/f"assets/jugcraft/models/{ref}.json").read_text(encoding="utf-8"))
            self.assertGreater(len(model["elements"]),64)
            for element in model["elements"]:
                for axis in range(3): self.assertLess(element["from"][axis],element["to"][axis])
                for face in element["faces"].values():
                    self.assertIn(face["texture"][1:],model["textures"])
            for texture in model["textures"].values():
                self.assertIn(texture, {"minecraft:block/dark_oak_planks", "minecraft:block/spruce_planks", "minecraft:block/birch_planks", "minecraft:block/polished_deepslate", "minecraft:block/yellow_terracotta"})
            item=json.loads((RES/f"assets/jugcraft/items/{name}.json").read_text(encoding="utf-8"))
            self.assertEqual(item["model"]["model"],f"jugcraft:block/{name}")
    def test_survival_recipes_and_single_table_drops(self):
        for name in ("checkers_table", "chess_table"):
            recipe=json.loads((RES/f"data/jugcraft/recipe/{name}.json").read_text(encoding="utf-8"))
            self.assertEqual(recipe["result"]["id"],f"jugcraft:{name}")
            self.assertEqual(set("".join(recipe["pattern"]).replace(" ","")),set(recipe["key"]))
            loot=json.loads((RES/f"data/jugcraft/loot_table/blocks/{name}.json").read_text(encoding="utf-8"))
            self.assertEqual(len(loot["pools"]),1)
            self.assertEqual(loot["pools"][0]["entries"],[{"type":"minecraft:item","name":f"jugcraft:{name}"}])
            self.assertEqual(loot["pools"][0]["condition"],{"type":"minecraft:survives_explosion"})
            self.assertNotIn("conditions",loot["pools"][0])
if __name__ == "__main__": unittest.main()
