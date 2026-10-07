"""JSON resources for the preserves pantry (fall additions 3), from tools/agriculture.py: the Canning Kettle and the
Pantry Shelf (models, blockstates, items), the Mason Jar, cider vinegar and every preserve's item model (a cloth-capped
jar once it is sealed: the jugcraft:sealed component), names, tooltips, messages, loot and tags.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files. The water and
jars in the kettle and the jars on the shelf are drawn by the client's CanningKettleRenderer and PantryShelfRenderer.
"""
from agriculture import PANTRY
from decor_data import MOD, rid, box, block_model, turned, self_drop, flat_item

SIDES = ["north", "south", "east", "west"]


def kettle_model():
    """A speckled blue enamel kettle: thin walls with a rolled rim, two loop handles, and an iron jar rack on its floor."""
    e, i, r = "#enamel", "#inside", "#rack"
    elements = [
        box((2, 0, 2), (14, 1, 14), e, textures={"up": i}),
        box((2, 1, 2), (14, 10, 3), e, textures={"south": i}), box((2, 1, 13), (14, 10, 14), e, textures={"north": i}),
        box((2, 1, 3), (3, 10, 13), e, textures={"east": i}), box((13, 1, 3), (14, 10, 13), e, textures={"west": i}),
        box((1.5, 9.5, 1.5), (14.5, 10.5, 2.5), e), box((1.5, 9.5, 13.5), (14.5, 10.5, 14.5), e),
        box((1.5, 9.5, 2.5), (2.5, 10.5, 13.5), e), box((13.5, 9.5, 2.5), (14.5, 10.5, 13.5), e),
        box((0.5, 7, 7), (2, 8, 9), r), box((0.5, 8, 7), (1.25, 9.5, 9), r),
        box((14, 7, 7), (15.5, 8, 9), r), box((14.75, 8, 7), (15.5, 9.5, 9), r),
        box((3, 1, 4), (13, 1.5, 5), r), box((3, 1, 7.5), (13, 1.5, 8.5), r), box((3, 1, 11), (13, 1.5, 12), r),
    ]
    return block_model({"enamel": "canning_kettle", "inside": "canning_kettle_inside", "rack": "canning_kettle_rack"}, elements,
                       "canning_kettle")


def shelf_model():
    """An open oak cupboard facing north: a beadboard back, sides, top and bottom, and a middle shelf, each shelf with a lip."""
    w, b = "#wood", "#back"
    elements = [
        box((0, 0, 14), (16, 16, 16), w, textures={"north": b}),
        box((0, 0, 1), (1, 16, 14), w), box((15, 0, 1), (16, 16, 14), w),
        box((1, 0, 1), (15, 1, 14), w), box((1, 15, 1), (15, 16, 14), w),
        box((1, 7.5, 1), (15, 8.5, 14), w), box((1, 8.5, 1), (15, 9, 1.5), w), box((1, 1, 1), (15, 1.5, 1.5), w),
    ]
    return block_model({"wood": "pantry_shelf_wood", "back": "pantry_shelf_back"}, elements, "pantry_shelf_wood")


TEXT = {
    "tooltip.jugcraft.preserves.sealed": "Sealed: keeps until opened",
    "tooltip.jugcraft.preserves.unsealed": "Not sealed: spoils three days after it was cooked. Seal it in a Canning Kettle",
    "tooltip.jugcraft.preserves.opened": "Opened: spoils three days after it was opened",
    "tooltip.jugcraft.preserves.servings": "%s of %s servings",
    "tooltip.jugcraft.preserves.spoiled_eaten": "Ugh! That jar had gone off",
    "message.jugcraft.canning_kettle.no_water": "Fill the kettle with water first",
    "message.jugcraft.canning_kettle.jars_in": "Take the jars out before you pour the water away",
    "message.jugcraft.canning_kettle.already_sealed": "That jar is already sealed",
    "message.jugcraft.canning_kettle.opened": "Only a full, fresh jar can be sealed",
    "message.jugcraft.canning_kettle.full": "The rack is full",
    "message.jugcraft.canning_kettle.heating_empty": "The water is heating",
    "message.jugcraft.canning_kettle.boiling_empty": "The water is at a rolling boil",
    "message.jugcraft.canning_kettle.heating": "%s jars in; the water isn't boiling yet (the next needs %s s at the boil)",
    "message.jugcraft.canning_kettle.processing": "%s jars in the boiling water; the next seals in %s s",
    "message.jugcraft.canning_kettle.lifted": "You lift out %s jars, %s of them sealed",
    "message.jugcraft.pantry_shelf.full": "The shelf is full",
    "message.jugcraft.pantry_shelf.empty": "The shelf is bare",
}


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"
    kettle, shelf = PANTRY["kettle"], PANTRY["shelf"]
    write(models / f"{kettle}.json", kettle_model())
    write(states / f"{kettle}.json", {"variants": {"": {"model": rid(f"block/{kettle}")}}})
    write(items / f"{kettle}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{kettle}")}})
    lang[f"block.{MOD}.{kettle}"] = PANTRY["kettle_display"]
    write(models / f"{shelf}.json", shelf_model())
    write(states / f"{shelf}.json", {"variants": {f"facing={f}": turned(rid(f"block/{shelf}"), f) for f in ("north", "south", "east", "west")}})
    write(items / f"{shelf}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{shelf}")}})
    lang[f"block.{MOD}.{shelf}"] = PANTRY["shelf_display"]
    for item, display in ((PANTRY["jar"], PANTRY["jar_display"]), (PANTRY["vinegar"], PANTRY["vinegar_display"])):
        flat_item(root, write, item)
        lang[f"item.{MOD}.{item}"] = display
    # A preserve's model: the plain jar, or once sealed the jar with its cloth cap.
    for preserve, info in PANTRY["preserves"].items():
        for name in (preserve, f"{preserve}_sealed"):
            write(root / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
        write(items / f"{preserve}.json", {"model": {
            "type": "minecraft:condition", "property": "minecraft:has_component", "component": rid("sealed"),
            "on_true": {"type": "minecraft:model", "model": rid(f"item/{preserve}_sealed")},
            "on_false": {"type": "minecraft:model", "model": rid(f"item/{preserve}")}}})
        lang[f"item.{MOD}.{preserve}"] = info["display"]
    lang.update(TEXT)


def loot(out, write):
    for block in (PANTRY["kettle"], PANTRY["shelf"]):
        write(out / f"{block}.json", self_drop(block))


def tags(tags):
    tags.add("block", "minecraft:mineable/pickaxe", rid(PANTRY["kettle"]))
    tags.add("block", "minecraft:mineable/axe", rid(PANTRY["shelf"]))
    for preserve in PANTRY["preserves"]:
        tags.add("item", "c:foods", rid(preserve))
