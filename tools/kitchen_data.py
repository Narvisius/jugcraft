"""JSON resources for the Farmhouse Kitchen (the kitchen and cooking expansion's slice 1), from tools/kitchen.py: the
Kitchen Stove (a brick range, its firebox and grill aglow when lit), the Skillet (an iron pan with a wooden handle), the
Cutting Board, the eleven kitchen cabinets (doors open while anyone is in one), the knives, and the cutting recipes.
Every texture is the owner's own, imported from the shared library by tools/owner_art.py. The foods' item models, names
and tags come from tools/agriculture.py ITEMS like every other food's; their cooking recipes from its COOKING table; the
crafting from SHAPED.

Called from agriculture_data.py (assets, loot, recipes, tags). Formats follow vanilla Minecraft 26.3's own files
(the smithing_transform recipe is shaped like vanilla's netherite tool upgrades).
"""
from decor_data import MOD, rid, block_model, self_drop, turned, HORIZONTAL
from kitchen import STOVE, SKILLET, BOARD, KNIVES, KNIFE_TAG, CUTTING_TYPE, CUTTING, CABINET_WOODS, TEXT, cabinet

ALL = ("north", "south", "east", "west", "up", "down")


def face(texture, uv=None, cull=None):
    out = {"texture": texture}
    if uv:
        out["uv"] = list(uv)
    if cull:
        out["cullface"] = cull
    return out


def cube(textures):
    """A whole block, each face its own texture (by side), culled against its neighbours."""
    return {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {side: face(textures[side], cull=side) for side in ALL}}


def stove_model(lit):
    """The Kitchen Stove facing north: brick sides, the firebox's arch in its front and the grill on top; lit, the fire
    burns in the firebox and the grill glows (both animated)."""
    on = "_on" if lit else ""
    textures = {"front": f"kitchen_stove_front{on}", "top": f"kitchen_stove_top{on}", "side": "kitchen_stove_side",
                "bottom": "kitchen_stove_bottom"}
    return block_model(textures, [cube({"north": "#front", "up": "#top", "down": "#bottom", "south": "#side", "east": "#side",
                                         "west": "#side"})], "kitchen_stove_side")


def skillet_model():
    """The Skillet, its handle to the north: a cast-iron pan 10 by 10 with a rim two pixels high, and a wooden handle."""
    t, s, b, h = "#top", "#side", "#bottom", "#handle"
    pan = {"from": [3, 0, 4], "to": [13, 1, 14], "faces": {
        "up": face(t, (1, 1, 15, 15)), "down": face(b, (1, 1, 15, 15)),
        "north": face(s, (0, 2, 10, 3)), "south": face(s, (0, 2, 10, 3)), "east": face(s, (0, 2, 10, 3)), "west": face(s, (0, 2, 10, 3))}}
    walls = []
    for lo, hi, sides in (((3, 1, 4), (13, 3, 5), (10, 1)), ((3, 1, 13), (13, 3, 14), (10, 1)),
                          ((3, 1, 5), (4, 3, 13), (1, 8)), ((12, 1, 5), (13, 3, 13), (1, 8))):
        wide, deep = sides
        walls.append({"from": list(lo), "to": list(hi), "faces": {
            "north": face(s, (0, 0, wide, 2)), "south": face(s, (0, 0, wide, 2)),
            "east": face(s, (0, 0, deep, 2)), "west": face(s, (0, 0, deep, 2)),
            "up": face(s, (0, 0, 1, 1))}})
    handle = {"from": [7, 1.5, 0], "to": [9, 2.5, 4], "faces": {
        "north": face(h, (7, 7, 9, 8)), "south": face(h, (7, 7, 9, 8)), "east": face(h, (6, 7, 10, 8)),
        "west": face(h, (6, 7, 10, 8)), "up": face(h, (7, 6, 9, 10)), "down": face(h, (7, 6, 9, 10))}}
    return block_model({"top": "skillet_top", "side": "skillet_side", "bottom": "skillet_bottom", "handle": "skillet_side"},
                       [pan] + walls + [handle], "skillet_top")


def board_model():
    """The Cutting Board: a wooden board 14 by 15, one pixel thick, the hole to hang it by towards its east end."""
    edge = (1, 15, 15, 16)
    return block_model({"board": "cutting_board"}, [{"from": [1, 0, 0.5], "to": [15, 1, 15.5], "faces": {
        "up": face("#board", (1, 1, 15, 16)), "down": face("#board", (1, 1, 15, 16), cull="down"),
        "north": face("#board", edge), "south": face("#board", edge), "east": face("#board", edge), "west": face("#board", edge)}}],
        "cutting_board")


def cabinet_model(wood, open_doors):
    """A kitchen cabinet facing north: its doors (open, the shelf's shadow behind them), sides and top."""
    front = f"{wood}_cabinet_front_open" if open_doors else f"{wood}_cabinet_front"
    return block_model({"front": front, "side": f"{wood}_cabinet_side", "top": f"{wood}_cabinet_top"},
                       [cube({"north": "#front", "south": "#side", "east": "#side", "west": "#side", "up": "#top", "down": "#top"})],
                       f"{wood}_cabinet_side")


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    stove = STOVE["block"]
    write(models / f"{stove}.json", stove_model(False))
    write(models / f"{stove}_on.json", stove_model(True))
    write(states / f"{stove}.json", {"variants": {
        f"facing={f},lit={str(lit).lower()}": turned(rid(f"block/{stove}" + ("_on" if lit else "")), f)
        for f in HORIZONTAL for lit in (False, True)}})
    skillet = SKILLET["block"]
    write(models / f"{skillet}.json", skillet_model())
    write(states / f"{skillet}.json", {"variants": {f"facing={f}": turned(rid(f"block/{skillet}"), f) for f in HORIZONTAL}})
    board = BOARD["block"]
    write(models / f"{board}.json", board_model())
    write(states / f"{board}.json", {"variants": {f"facing={f}": turned(rid(f"block/{board}"), f) for f in HORIZONTAL}})
    for block, display in ((stove, STOVE["display"]), (skillet, SKILLET["display"]), (board, BOARD["display"])):
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
        lang[f"block.{MOD}.{block}"] = display
    for wood, display in CABINET_WOODS.items():
        name = cabinet(wood)
        write(models / f"{name}.json", cabinet_model(wood, False))
        write(models / f"{name}_open.json", cabinet_model(wood, True))
        write(states / f"{name}.json", {"variants": {
            f"facing={f},open={str(o).lower()}": turned(rid(f"block/{name}" + ("_open" if o else "")), f)
            for f in HORIZONTAL for o in (False, True)}})
        write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
        lang[f"block.{MOD}.{name}"] = f"{display} Cabinet"
    for knife, info in KNIVES.items():
        write(root / "models" / "item" / f"{knife}.json", {"parent": "minecraft:item/handheld",
                                                         "textures": {"layer0": rid(f"item/{knife}")}})
        write(root / "items" / f"{knife}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{knife}")}})
        lang[f"item.{MOD}.{knife}"] = info["display"]
    lang.update(TEXT)


def loot(out, write):
    for block in [STOVE["block"], SKILLET["block"], BOARD["block"]] + [cabinet(wood) for wood in CABINET_WOODS]:
        write(out / f"{block}.json", self_drop(block))


def recipes(out, write, conditions):
    for name, info in CUTTING.items():
        results = [{"id": rid(item), "count": count} if count != 1 else {"id": rid(item)} for item, count in info["results"]]
        write(out / "cutting" / f"{name}.json", {"fabric:load_conditions": conditions(), "type": CUTTING_TYPE,
                                                "ingredient": info["input"], "tool": f"#{KNIFE_TAG}", "results": results})
    for knife, info in KNIVES.items():
        if "smithing" in info:
            write(out / f"{knife}_smithing.json", {"fabric:load_conditions": conditions(), "type": "minecraft:smithing_transform",
                                                  "template": "minecraft:netherite_upgrade_smithing_template",
                                                  "base": rid(info["smithing"]), "addition": "minecraft:netherite_ingot",
                                                  "result": {"id": rid(knife)}})


def tags(tags):
    tags.add("block", "jugcraft:heat_sources", rid(STOVE["block"]))
    for block in (STOVE["block"], SKILLET["block"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    tags.add("block", "minecraft:mineable/axe", rid(BOARD["block"]))
    for wood in CABINET_WOODS:
        tags.add("block", "minecraft:mineable/axe", rid(cabinet(wood)))
    tags.add("item", KNIFE_TAG, rid("carving_knife"))
    for knife in KNIVES:
        tags.add("item", KNIFE_TAG, rid(knife))
        tags.add("item", "c:tools/knife", rid(knife))
        # Vanilla enchantment tags are composed with all equipment in gear.write_all.
