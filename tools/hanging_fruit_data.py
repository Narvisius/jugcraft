"""Batch 10 hanging pods/clusters; age is reset when preserved as an item."""
from trees_data import SHEARS_OR_SILK

FRUIT = {"baobab_pod": ("baobab_fruit", 1, 2), "date_cluster": ("dates", 2, 4)}


def assets(root, write, lang):
    for block in FRUIT:
        lang[f"block.jugcraft.{block}"] = block.replace("_", " ").title()
        variants = {}
        for age in range(3):
            name = f"{block}_{age}"
            write(root / "models/block" / f"{name}.json", {"parent": "minecraft:block/cross",
                  "textures": {"cross": f"jugcraft:block/{name}"}})
            variants[f"age={age}"] = {"model": f"jugcraft:block/{name}"}
        write(root / "blockstates" / f"{block}.json", {"variants": variants})
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{block}"}})
        write(root / "models/item" / f"{block}.json", {"parent": "minecraft:item/generated",
              "textures": {"layer0": f"jugcraft:block/{block}_2"}})


def loot(out, write):
    for block, (fruit, low, high) in FRUIT.items():
        write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
              "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:alternatives", "children": [
                  {"type": "minecraft:item", "name": f"jugcraft:{block}", "condition": SHEARS_OR_SILK},
                  {"type": "minecraft:item", "name": f"jugcraft:{fruit}",
                   "condition": {"type": "minecraft:match_block", "blocks": f"jugcraft:{block}", "state": {"age": "2"}},
                   "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}},
                                {"type": "minecraft:explosion_decay"}]}]}]}]})


def tags(tags):
    for block in FRUIT:
        tags.add("block", "minecraft:mineable/hoe", f"jugcraft:{block}")
