"""JSON resources for the Hay Golem (fall additions 14), from tools/agriculture.py: its name, the heads it may wear (item
tag jugcraft:hay_golem_heads) and its loot (a few wheat; its head and pouch drop from code).

Called from agriculture_data.py (assets, loot, tags). The golem is drawn by the client's HayGolemRenderer and
HayGolemModel (texture from tools/hay_golem_textures.py); it is built from vanilla hay bales and needs no recipe.
"""
from agriculture import HAY_GOLEM
from decor_data import MOD, rid


def assets(root, write, lang):
    lang[f"entity.{MOD}.{HAY_GOLEM['entity']}"] = HAY_GOLEM["display"]


def loot(out, write):
    """A Hay Golem drops a few wheat (out = loot_table/blocks; the table goes beside it under entities)."""
    low, high = HAY_GOLEM["wheat"]
    write(out.parent / f"{HAY_GOLEM['table']}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "minecraft:wheat",
        "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}]}],
        "random_sequence": rid(HAY_GOLEM["table"])})


def tags(tags):
    for head in HAY_GOLEM["heads"]:
        tags.add("item", HAY_GOLEM["heads_tag"], head)
