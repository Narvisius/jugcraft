"""JSON resources for wild turkeys (fall additions 18), from tools/agriculture.py: the turkey's name and loot (a raw
turkey and a few feathers); the roast turkey's models (on a silver platter with cranberries and herbs: whole; its
drumsticks gone; its breast carved; the carcass), blockstate and words, and its loot (only while whole); and tags (what
tempts and breeds turkeys; the biomes they come to). The raw turkey and slice are in ITEMS, the cooking in COOKING, the
advancements in HALLOWEEN_ADVANCEMENTS.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files. The turkey is
drawn by the client's TurkeyRenderer and TurkeyModel (textures from tools/turkey_textures.py).
"""
from agriculture import TURKEYS
from decor_data import MOD, rid, box, block_model, self_drop, turned

TEXTURES = {"skin": "roast_turkey_skin", "meat": "roast_turkey_meat", "bone": "roast_turkey_bone", "platter": "turkey_platter",
            "berries": "turkey_cranberries", "herbs": "turkey_herbs"}
# Which model each number of servings eaten shows.
STAGES = ["", "_carved", "_carved", "_breast", "_breast", "_carcass"]


def roast(stage):
    """A roast turkey facing north (drumsticks to the front) on its platter, as far eaten as `stage` says."""
    s, m, b, p = "#skin", "#meat", "#bone", "#platter"
    elements = [box((1, 0, 1.5), (15, 0.5, 14.5), p),
                box((1.5, 0.5, 12), (2.5, 1.25, 13), "#berries"), box((2.5, 0.5, 12.5), (3.25, 1.1, 13.25), "#berries"),
                box((13, 0.5, 2.5), (14.25, 1, 3.75), "#herbs"), box((12.5, 0.5, 12), (13.75, 1, 13.25), "#herbs"),
                box((13.25, 0.5, 11), (14, 1.25, 11.75), "#berries")]
    if stage == "_carcass":
        elements += [box((5, 0.5, 5), (11, 2.5, 12), b), box((6, 2.5, 6), (10, 3.5, 11), b, textures={"up": m})]
        return block_model(TEXTURES, elements, TEXTURES["skin"])
    carved_breast = stage == "_breast"
    top = 3.5 if carved_breast else 5
    elements.append(box((4, 0.5, 4), (12, top, 13), s, textures={"up": m} if carved_breast else {"north": m} if stage == "_carved" else None))
    if not carved_breast:
        elements.append(box((5, 5, 5), (11, 6.5, 12), s))
    # The wings, tucked at the sides.
    elements += [box((3, 1.5, 6), (4, 4, 11), s), box((12, 1.5, 6), (13, 4, 11), s)]
    if stage == "":
        # The drumsticks, their bones' ends sticking out at the front.
        elements += [box((4.5, 1, 1.5), (7, 4, 4.5), s), box((9, 1, 1.5), (11.5, 4, 4.5), s),
                     box((5.25, 2, 0.75), (6.25, 3, 1.5), b), box((9.75, 2, 0.75), (10.75, 3, 1.5), b)]
    return block_model(TEXTURES, elements, TEXTURES["skin"])


def assets(root, write, lang):
    lang[f"entity.{MOD}.{TURKEYS['entity']}"] = TURKEYS["display"]
    name = TURKEYS["roast"]
    models = root / "models" / "block"
    for stage in sorted(set(STAGES)):
        write(models / f"{name}{stage}.json", roast(stage))
    write(root / "blockstates" / f"{name}.json", {"variants": {
        f"bites={b},facing={f}": turned(rid(f"block/{name}{STAGES[b]}"), f)
        for f in ("north", "south", "east", "west") for b in range(TURKEYS["servings"])}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    lang[f"block.{MOD}.{name}"] = TURKEYS["roast_display"]


def loot(out, write):
    """The roast turkey drops itself only while whole; a turkey drops a raw turkey and feathers (out = loot_table/blocks)."""
    name = TURKEYS["roast"]
    write(out / f"{name}.json", self_drop(name, {"type": "minecraft:match_block", "blocks": rid(name), "state": {"bites": "0"}}))
    low, high = TURKEYS["feathers"]
    write(out.parent / f"{TURKEYS['table']}.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(TURKEYS["raw"])}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:feather", "modifier": {
            "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}]}],
        "random_sequence": rid(TURKEYS["table"])})


def tags(tags):
    for food in TURKEYS["food"]:
        tags.add("item", TURKEYS["food_tag"], food)
    for biome in TURKEYS["habitat"]:
        tags.add("worldgen/biome", TURKEYS["habitat_tag"], biome)
