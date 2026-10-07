"""JSON resources for squirrels and acorns (fall addition 24), from tools/agriculture.py: the acorn's item model and
names; tags (what squirrels eat, where they live, the acorn as a seed). Roasted acorns are in ITEMS and COOKING, the
advancement in HALLOWEEN_ADVANCEMENTS; the squirrel drops nothing.

Called from agriculture_data.py. Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import SQUIRRELS
from decor_data import MOD, rid, flat_item


def assets(root, write, lang):
    flat_item(root, write, SQUIRRELS["acorn"])
    lang[f"item.{MOD}.{SQUIRRELS['acorn']}"] = SQUIRRELS["acorn_display"]
    lang[f"entity.{MOD}.{SQUIRRELS['entity']}"] = SQUIRRELS["display"]


def tags(tags):
    for food in SQUIRRELS["food"]:
        tags.add("item", f"{MOD}:squirrel_food", food)
    for biome in SQUIRRELS["habitat"]:
        tags.add("worldgen/biome", f"{MOD}:squirrel_habitat", biome)
    tags.add("item", "c:seeds", rid(SQUIRRELS["acorn"]))
