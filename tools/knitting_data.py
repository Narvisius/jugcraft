"""JSON resources for knitting (fall additions 15), from tools/agriculture.py: the Spinning Wheel's model (bench, legs,
treadle, the wheel's posts and axle, the distaff and the spindle's maidens; the wheel, the wool and the yarn are drawn by
the client's SpinningWheelRenderer) and blockstate, the yarn's, needles' and garments' item models (each tinted by the
vanilla dyed_color component, a sweater's motif over it), the garments' equipment assets (the knit, dyeable, and a
sweater's motif), names, messages, tooltips, loot, tags (knitwear; vanilla's freeze-immune wearables, and the items a
cauldron washes the dye out of) and the dyeing recipes (one for the yarn and each garment, as 26.3 dyes leather armour).
The other recipes are in SHAPED; the advancements in HALLOWEEN_ADVANCEMENTS.

Called from agriculture_data.py (assets, loot, tags, recipes). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import KNITTING
from candy_data import argb
from decor_data import MOD, rid, box, block_model, self_drop, turned

TEXTURES = {"wood": "spinning_wheel_wood", "dark": "spinning_wheel_dark"}

TEXT = {
    "message.jugcraft.spinning_wheel.empty": "Put a skein of wool on the distaff first",
    "message.jugcraft.spinning_wheel.busy": "There is wool on the distaff already: work the treadle to spin it",
    "message.jugcraft.spinning_wheel.unravelled": "Unravelled into %s balls of yarn",
    "message.jugcraft.knitting.no_yarn": "Hold a ball of yarn in your other hand to knit",
    "message.jugcraft.knitting.row": "Row %s of %s: %s",
    "message.jugcraft.knitting.done": "Finished: %s",
    "message.jugcraft.knitting.project": "Now knitting: %s (%s rows)",
    "message.jugcraft.knitting.unpicked": "Unpicked %s rows; the yarn is back",
    "tooltip.jugcraft.knitting_needles.project": "Knitting: %s, %s of %s rows",
}


def wheel():
    """A spinning wheel facing north: a plank bench on three legs, a treadle under the front, two posts holding the
    wheel's axle across the back, the distaff post at the front left, and the spindle between its two maidens at the front
    right."""
    w, d = "#wood", "#dark"
    elements = [box((3, 3, 3), (13, 4.25, 14), w),
                box((3.5, 0, 3.5), (5, 3, 5), d), box((11, 0, 3.5), (12.5, 3, 5), d), box((7.25, 0, 12), (8.75, 3, 13.5), d),
                box((5, 0.5, 0.5), (11, 1.25, 3.5), w), box((7.5, 0, 2.5), (8.5, 0.5, 3.5), d),
                box((7.4, 4.25, 7.6), (8.6, 10.6, 8.4), d), box((7.4, 4.25, 10.6), (8.6, 10.6, 11.4), d),
                box((7.6, 9.6, 7.6), (8.4, 10.4, 11.4), d),
                box((3, 4.25, 3), (4, 11, 4), d),
                box((11.5, 4.25, 1.5), (12.5, 9.5, 2.25), d), box((11.5, 4.25, 6), (12.5, 9.5, 6.75), d),
                box((11.75, 8.5, 2.25), (12.25, 9, 6), d)]
    return block_model(TEXTURES, elements, TEXTURES["wood"])


def tinted(root, write, name, layers, color):
    """An item model whose first layer is tinted by its dyed colour (default `color`), any others drawn as they are."""
    write(root / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {
        f"layer{i}": rid(f"item/{layer}") for i, layer in enumerate(layers)}})
    tints = [{"type": "minecraft:dye", "default": argb(color)}] + [{"type": "minecraft:constant", "value": -1}] * (len(layers) - 1)
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}"), "tints": tints}})


def assets(root, write, lang):
    name = KNITTING["wheel"]
    write(root / "models" / "block" / f"{name}.json", wheel())
    write(root / "blockstates" / f"{name}.json", {"variants": {
        f"facing={f},powered={p}": turned(rid(f"block/{name}"), f) for f in ("north", "south", "east", "west") for p in ("false", "true")}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    lang[f"block.{MOD}.{name}"] = KNITTING["wheel_display"]
    undyed = KNITTING["undyed"]
    tinted(root, write, KNITTING["yarn"], [KNITTING["yarn"]], undyed)
    lang[f"item.{MOD}.{KNITTING['yarn']}"] = KNITTING["yarn_display"]
    needles = KNITTING["needles"]
    write(root / "models" / "item" / f"{needles}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{needles}")}})
    write(root / "items" / f"{needles}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{needles}")}})
    lang[f"item.{MOD}.{needles}"] = KNITTING["needles_display"]
    for garment, info in KNITTING["garments"].items():
        base = "knit_sweater" if info["slot"] == "CHEST" else garment
        layers = [base] + ([f"{info['motif']}_motif"] if "motif" in info else [])
        tinted(root, write, garment, layers, undyed)
        lang[f"item.{MOD}.{garment}"] = info["display"]
    for asset in sorted({info["asset"] for info in KNITTING["garments"].values()}):
        layers = [{"texture": rid("knit"), "dyeable": {"color_when_undyed": argb(undyed)}}]
        if asset != "knit":
            layers.append({"texture": rid(asset)})
        write(root / "equipment" / f"{asset}.json", {"layers": {"humanoid": layers}})
    lang.update(TEXT)


def loot(out, write):
    write(out / f"{KNITTING['wheel']}.json", self_drop(KNITTING["wheel"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(KNITTING["wheel"]))
    tags.add("item", KNITTING["wash_tag"], rid(KNITTING["yarn"]))
    for garment in KNITTING["garments"]:
        tags.add("item", KNITTING["knitwear_tag"], rid(garment))
        tags.add("item", "minecraft:freeze_immune_wearables", rid(garment))
        tags.add("item", KNITTING["wash_tag"], rid(garment))


def dyeable():
    """The items that take dye: the yarn and every garment."""
    return [KNITTING["yarn"]] + list(KNITTING["garments"])


def recipes(out, write, conditions):
    """A dyeing recipe for each, in the form of vanilla's leather_helmet_dyed: the item and any dye make it that colour."""
    for item in dyeable():
        write(out / f"{item}_dyed.json", {"fabric:load_conditions": conditions(), "type": KNITTING["dye_recipe"],
                                          "group": KNITTING["dye_group"], "target": rid(item), "dye": "#minecraft:dyes",
                                          "result": {"id": rid(item)}})
