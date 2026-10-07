"""JSON resources for the Día de Muertos ofrenda (fall additions 20), from tools/agriculture.py: the ofrenda's model
(three tiers stepping up from the front under a white cloth with an embroidered band and a lace edge) and blockstate;
marigold petals strewn flat on the ground; papel picado, a string of cut-paper flags hung on a wall; the sugar skull;
names; loot; and tags (the five kinds of offering, and everything that may be offered). The marigold itself is one of
MUMS; pan de muerto and its dough are in ITEMS and COOKING; the recipes in SHAPED and SHAPELESS; the advancement in
HALLOWEEN_ADVANCEMENTS.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files. The offerings
are drawn by the client's OfrendaRenderer.
"""
from agriculture import OFRENDA
from decor_data import MOD, rid, box, block_model, self_drop, turned, flat_item

DIRECTIONS = ("north", "south", "east", "west")


def ofrenda():
    """An ofrenda facing north: its lowest tier at the front, the cloth's embroidered band and lace edge on each riser."""
    c, f = "#cloth", "#front"
    tiers = [((0, 0, 0), (16, 5, 16)), ((0, 5, 5), (16, 10, 16)), ((0, 10, 10), (16, 15, 16))]
    elements = [box(lo, hi, c, textures={"north": f}, uvs={"north": (0, 11, 16, 16)}) for lo, hi in tiers]
    return block_model({"cloth": "ofrenda_cloth", "front": "ofrenda_cloth_front"}, elements, "ofrenda_cloth")


def petals():
    """Marigold petals strewn on the ground, a sixteenth of a block deep."""
    return block_model({"petals": "marigold_petals"}, [box((0, 0, 0), (16, 0.25, 16), "#petals", faces=("up", "down"))], "marigold_petals")


def papel_picado():
    """A string of papel picado on the wall behind it (to the south of a north-facing one), the flags hanging free."""
    return block_model({"paper": "papel_picado"}, [box((0, 1, 15.25), (16, 15, 15.25), "#paper", faces=("north", "south"))], "papel_picado")


def sugar_skull():
    """A sugar skull facing north: the round cranium and a narrower jaw below its face."""
    return block_model({"face": "sugar_skull_face", "sugar": "sugar_skull_side"}, [
        box((5, 1, 5), (11, 6, 11), "#sugar", textures={"north": "#face"}, uvs={"north": (5, 4, 11, 9)}),
        box((6, 0, 5.5), (10, 1, 10), "#sugar", textures={"north": "#face"}, uvs={"north": (6, 9, 10, 10)})], "sugar_skull_side")


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    name = OFRENDA["block"]
    write(models / f"{name}.json", ofrenda())
    write(states / f"{name}.json", {"variants": {f"complete={c},facing={f}": turned(rid(f"block/{name}"), f)
                                                 for f in DIRECTIONS for c in ("false", "true")}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    lang[f"block.{MOD}.{name}"] = OFRENDA["display"]
    write(models / "marigold_petals.json", petals())
    write(states / "marigold_petals.json", {"variants": {"": {"model": rid("block/marigold_petals")}}})
    flat_item(root, write, "marigold_petals")
    write(models / "papel_picado.json", papel_picado())
    write(states / "papel_picado.json", {"variants": {f"facing={f}": turned(rid("block/papel_picado"), f) for f in DIRECTIONS}})
    flat_item(root, write, "papel_picado")
    write(models / "sugar_skull.json", sugar_skull())
    write(states / "sugar_skull.json", {"variants": {f"facing={f}": turned(rid("block/sugar_skull"), f) for f in DIRECTIONS}})
    write(root / "items" / "sugar_skull.json", {"model": {"type": "minecraft:model", "model": rid("block/sugar_skull")}})
    for block, display in OFRENDA["decor"].items():
        lang[f"block.{MOD}.{block}"] = display


def loot(out, write):
    for block in [OFRENDA["block"]] + list(OFRENDA["decor"]):
        write(out / f"{block}.json", self_drop(block))


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(OFRENDA["block"]))
    for kind, items in OFRENDA["kinds"].items():
        for item in items:
            tags.add("item", f"jugcraft:ofrenda/{kind}", item)
        tags.add("item", OFRENDA["offerings_tag"], f"#jugcraft:ofrenda/{kind}")
    for item in OFRENDA["keepsakes"]:
        tags.add("item", OFRENDA["offerings_tag"], item)
