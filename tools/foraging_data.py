"""JSON resources for autumn foraging (fall additions 12), from tools/agriculture.py: the wild mushrooms' cross models,
blockstates, items (each shows its own texture), names and loot (each drops itself), their worldgen (patches on forest
floors), the Foraging Basket's model and words, the fairy ring's message, and the tags that say what is a wild mushroom
and what the basket holds.

Called from agriculture_data.py (assets, loot, tags, worldgen). Formats follow vanilla Minecraft 26.3's own files. The
mushrooms' foods are plain items in ITEMS; their recipes are in COOKING and POT_RECIPES.
"""
from agriculture import FORAGING
from decor_data import MOD, rid, flat_item, self_drop

TEXT = {
    "item.jugcraft.foraging_basket.hint": "Holds mushrooms, berries, nuts and wild fruit. Pick mushrooms with it in hand to fill it",
    "message.jugcraft.fairy_ring.blessed": "You dance in the fairy ring under the full moon. You feel lucky",
}


def assets(root, write, lang):
    for mushroom, info in FORAGING["mushrooms"].items():
        texture = rid(f"block/{mushroom}")
        write(root / "models" / "block" / f"{mushroom}.json", {"parent": "minecraft:block/cross", "textures": {"cross": texture}})
        write(root / "blockstates" / f"{mushroom}.json", {"variants": {"": {"model": rid(f"block/{mushroom}")}}})
        write(root / "models" / "item" / f"{mushroom}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": texture}})
        write(root / "items" / f"{mushroom}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{mushroom}")}})
        lang[f"block.{MOD}.{mushroom}"] = info["display"]
    flat_item(root, write, FORAGING["basket"])
    lang[f"item.{MOD}.{FORAGING['basket']}"] = FORAGING["basket_display"]
    lang.update(TEXT)


def loot(out, write):
    for mushroom in FORAGING["mushrooms"]:
        write(out / f"{mushroom}.json", self_drop(mushroom))


def tags(tags):
    for mushroom in FORAGING["mushrooms"]:
        tags.add("item", "jugcraft:wild_mushrooms", rid(mushroom))
        tags.add("block", "jugcraft:wild_mushrooms", rid(mushroom))
        tags.add("block", "minecraft:enderman_holdable", rid(mushroom))
    for item in FORAGING["forage"]:
        tags.add("item", FORAGING["forage_tag"], item)
    for block in FORAGING["soil"]:
        tags.add("block", FORAGING["soil_tag"], block)


def worldgen(data, write):
    """Each mushroom in patches on soil in its biomes, a patch in about `rarity` chunks there."""
    patch = FORAGING["patch"]
    spread = patch["spread_xz"]
    for mushroom in FORAGING["mushrooms"]:
        write(data / MOD / "worldgen" / "feature" / f"{mushroom}.json",
              {"type": "minecraft:simple_block", "to_place": {"id": rid(mushroom)}})
        write(data / MOD / "worldgen" / "placed_feature" / f"patch_{mushroom}.json", {
            "feature": rid(mushroom),
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": patch["rarity"]},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                {"type": "minecraft:biome"},
                {"type": "minecraft:count", "count": patch["tries"]},
                {"type": "minecraft:offset",
                 "x": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0},
                 "y": {"type": "minecraft:trapezoid", "max": patch["spread_y"], "min": -patch["spread_y"], "plateau": 0},
                 "z": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0}},
                {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                    {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                    {"type": "minecraft:matching_block_tag", "tag": FORAGING["soil_tag"], "offset": [0, -1, 0]},
                ]}},
            ],
        })
