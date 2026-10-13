"""JSON resources for the kitchen herbs (tools/herbs.py, garden crops, herbs and spices part b): the potted herbs, the drying
bundles and the Planter Box (their models, blockstates, items, loot, tags and names). The herbs themselves (crops, wild
plants, sprigs, dishes) are written from tools/agriculture.py's tables by tools/agriculture_data.py.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
from decor_data import MOD, rid, self_drop
from herbs import HERBS, PLANTER, BUNDLE, BUNDLES, bundle, potted, stage_texture

SIDES = ("north", "south", "east", "west")


def face(uv, texture, cull=None):
    out = {"texture": texture, "uv": uv}
    if cull:
        out["cullface"] = cull
    return out


def planter():
    """The box: 15 pixels tall, as farmland, so a crop stands in its soil as on a field; plank sides, soil top, plank bottom."""
    tex = PLANTER["textures"]
    faces = {"down": face([0, 0, 16, 16], "#bottom", "down"), "up": face([0, 0, 16, 16], "#top")}
    faces.update({s: face([0, 1, 16, 16], "#side", s) for s in SIDES})
    return {"parent": "minecraft:block/block",
            "textures": {"particle": rid(f"block/{tex['side']}"), **{k: rid(f"block/{v}") for k, v in tex.items()}},
            "elements": [{"from": [0, 0, 0], "to": [16, 15, 16], "faces": faces}]}


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    for herb, info in HERBS.items():
        # A flower pot with the grown herb in it (vanilla's potted plants' model).
        name = potted(herb)
        write(models / f"{name}.json", {"parent": "minecraft:block/flower_pot_cross",
                                        "textures": {"plant": rid(f"block/{stage_texture(herb, 3)}")}})
        write(states / f"{name}.json", {"variants": {"": {"model": rid(f"block/{name}")}}})
        lang[f"block.{MOD}.{name}"] = f"Potted {info['display']}"
        # The bundle hung to dry: fresh, then dried.
        name = bundle(herb)
        for suffix in ("", "_dried"):
            write(models / f"{name}{suffix}.json", {"parent": "minecraft:block/cross",
                                                    "textures": {"cross": rid(f"block/{name}{suffix}")}})
        write(states / f"{name}.json", {"variants": {f"dried={dried}": {"model": rid(f"block/{name}{suffix}")}
                                                     for dried, suffix in (("false", ""), ("true", "_dried"))}})
        write(root / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated",
                                                          "textures": {"layer0": rid(f"block/{name}")}})
        write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
        lang[f"block.{MOD}.{name}"] = BUNDLES[name]
    name = PLANTER["block"]
    write(models / f"{name}.json", planter())
    write(states / f"{name}.json", {"variants": {f"moisture={m}": {"model": rid(f"block/{name}")} for m in range(8)}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    lang[f"block.{MOD}.{name}"] = PLANTER["display"]


def loot(out, write):
    """A potted herb gives its pot and a sprig; a bundle gives itself fresh, or its sprigs as Dried Herbs once dried; the
    Planter Box gives itself."""
    for herb in HERBS:
        name = potted(herb)
        write(out / f"{name}.json", {"type": "minecraft:block", "random_sequence": rid(f"blocks/{name}"), "pools": [
            {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}]},
            {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": rid(herb)}]}]})
        name = bundle(herb)
        dried = {"type": "minecraft:match_block", "blocks": rid(name), "state": {"dried": "true"}}
        write(out / f"{name}.json", {"type": "minecraft:block", "random_sequence": rid(f"blocks/{name}"), "pools": [
            {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": rid(BUNDLE["dried"]), "condition": dried,
                 "modifier": {"type": "minecraft:set_count", "count": BUNDLE["sprigs"], "add": False}},
                {"type": "minecraft:item", "name": rid(name)}]}]}]})
    write(out / f"{PLANTER['block']}.json", self_drop(PLANTER["block"]))


def tags(tags):
    # The box takes crops as farmland does (crops are planted on it and grow as on moist farmland).
    for tag in ("minecraft:supports_crops", "minecraft:grows_crops"):
        tags.add("block", tag, rid(PLANTER["block"]))
    tags.add("block", "minecraft:mineable/axe", rid(PLANTER["block"]))
    for herb in HERBS:
        tags.add("block", "minecraft:flower_pots", rid(potted(herb)))
