"""JSON resources for ghost hunting (fall additions 9), from tools/agriculture.py: the Spirit Lantern (its item model,
tooltip and recipe), Ectoplasm (its item model; the Ghostly candle scent's tag comes from CHANDLERY), the restless
spirit's name, and the words.

Called from agriculture_data.py (assets, recipes). Formats follow vanilla Minecraft 26.3's own files. A spirit is drawn by
the client's RestlessSpiritRenderer (texture from tools/ghost_textures.py); it has no loot.
"""
from agriculture import GHOSTS
from decor_data import MOD, rid, flat_item

TEXT = {
    "item.jugcraft.spirit_lantern.tooltip": "Reveals restless spirits within %s blocks",
}


def assets(root, write, lang):
    for item, display in ((GHOSTS["lantern"], GHOSTS["lantern_display"]), (GHOSTS["ectoplasm"], GHOSTS["ectoplasm_display"])):
        flat_item(root, write, item)
        lang[f"item.{MOD}.{item}"] = display
    lang[f"entity.{MOD}.{GHOSTS['entity']}"] = GHOSTS["display"]
    lang.update(TEXT)


def recipes(out, write, conditions):
    # Three gold nuggets for the brass, two glass panes, an amethyst shard to see through, and a candle for the flame.
    write(out / f"{GHOSTS['lantern']}.json", {
        "fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": [" N ", "PAP", "NCN"],
        "key": {"N": "minecraft:gold_nugget", "P": "minecraft:glass_pane", "A": "minecraft:amethyst_shard", "C": "minecraft:candle"},
        "result": {"id": rid(GHOSTS["lantern"]), "count": 1}})
