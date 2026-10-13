"""JSON resources for pie baking (fall additions 16), from tools/agriculture.py: the Hearth Oven's models (a domed brick
oven on a stone hearth, its arched mouth to the front, a chimney; soot inside, glowing embers at the back when lit) and
blockstate; each pie's models (in quarters, one less for each slice gone: a lattice top, a fluted crust edge, the tin
underneath and the filling where it is cut) and blockstate; the items (the pies as their blocks, flat raw pies, slices and
pastry); names and messages; loot (the oven itself; a pie only while whole); and tags. The recipes are in SHAPED and
SHAPELESS; the advancement in HALLOWEEN_ADVANCEMENTS.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import PIES, pie_name, pie_display
import feasts
from decor_data import MOD, rid, box, block_model, self_drop, turned, flat_item

OVEN_TEXTURES = {"brick": "oven_brick", "soot": "oven_soot", "stone": "oven_stone", "embers": "oven_embers"}
# The pie's quarters, in the order slices are taken (PieBlock.QUARTERS).
QUARTERS = [((8, 0, 2), (14, 4, 8)), ((8, 0, 8), (14, 4, 14)), ((2, 0, 8), (8, 4, 14)), ((2, 0, 2), (8, 4, 8))]

TEXT = {
    "message.jugcraft.hearth_oven.status": "Hearth Oven: %s degrees, %s seconds of fire left",
    "message.jugcraft.hearth_oven.full": "The fire is banked as high as it goes",
    "message.jugcraft.hearth_oven.occupied": "There is a pie in the oven already",
    "message.jugcraft.pie.burnt": "That was very burnt",
}


def oven(lit):
    """A Hearth Oven facing north: a stone hearth, brick walls round a soot-black inside open at the front, a domed top and a
    chimney behind and to the right; lit, the back wall and a bed of embers glow."""
    b, s, t, e = "#brick", "#soot", "#stone", "#embers"
    back = e if lit else s
    elements = [box((1, 0, 1), (15, 2, 15), t),
                box((2, 2, 3), (5, 10, 14), b, textures={"east": s}), box((11, 2, 3), (14, 10, 14), b, textures={"west": s}),
                box((5, 2, 12), (11, 10, 14), b, textures={"north": back}, light=13 if lit else None),
                box((5, 7, 3), (11, 10, 12), b, textures={"down": s}),
                box((3, 10, 4), (13, 12, 13), b), box((5, 12, 6), (11, 13, 11), b),
                box((9, 12, 9), (11, 16, 11), b, textures={"up": s}),
                box((4.5, 2, 2.5), (11.5, 2.5, 3), t)]
    if lit:
        elements.append(box((5.5, 2, 10.5), (10.5, 2.75, 12), e, light=15))
    return block_model(OVEN_TEXTURES, elements, OVEN_TEXTURES["brick"])


def pie(name, bites, owner=False):
    """A pie in its tin with the first `bites` quarters gone: the lattice top, the crust's edge, the tin, and the filling on
    the faces where it is cut. A pie in the owner's art (`owner`) wears their shared crust side and bottom."""
    crust, bottom = (feasts.PIE_SIDE, feasts.PIE_BOTTOM) if owner else ("pie_side", "pie_tin")
    textures = {"top": f"{name}_top", "side": crust, "bottom": bottom, "inside": f"{name}_inside"}
    elements = []
    for quarter in range(bites, len(QUARTERS)):
        lo, hi = QUARTERS[quarter]
        faces = {}
        # A face is a cut face where it looks into the middle, towards a quarter that is gone.
        cx = 8
        for side, inner in (("west", lo[0] == cx), ("east", hi[0] == cx), ("north", lo[2] == cx), ("south", hi[2] == cx)):
            if inner:
                faces[side] = "#inside"
        elements.append(box(lo, hi, "#side", textures={"up": "#top", "down": "#bottom", **faces}))
    return block_model(textures, elements, crust)


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    name = PIES["oven"]
    write(models / f"{name}.json", oven(False))
    write(models / f"{name}_lit.json", oven(True))
    write(states / f"{name}.json", {"variants": {
        f"facing={f},lit={l}": turned(rid(f"block/{name}" + ("_lit" if l == "true" else "")), f)
        for f in ("north", "south", "east", "west") for l in ("false", "true")}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    lang[f"block.{MOD}.{name}"] = PIES["oven_display"]
    flat_item(root, write, PIES["dough"])
    lang[f"item.{MOD}.{PIES['dough']}"] = PIES["dough_display"]
    owner = set(feasts.owner_pies())
    pies = [(pie_name(f), pie_display(f)) for f in PIES["fillings"]] + [(PIES["burnt"], PIES["burnt_display"])]
    for block, display in pies:
        for bites in range(PIES["slices"]):
            write(models / (f"{block}.json" if bites == 0 else f"{block}_slice{bites}.json"), pie(block, bites, block in owner))
        write(states / f"{block}.json", {"variants": {
            f"bites={b}": {"model": rid(f"block/{block}" + ("" if b == 0 else f"_slice{b}"))} for b in range(PIES["slices"])}})
        if block in owner:
            flat_item(root, write, block)  # the owner's whole-pie icon
        else:
            write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
        lang[f"block.{MOD}.{block}"] = display
    for filling in PIES["fillings"]:
        name = pie_name(filling)
        flat_item(root, write, f"raw_{name}")
        lang[f"item.{MOD}.raw_{name}"] = f"Raw {pie_display(filling)}"
        flat_item(root, write, f"{name}_slice")
        lang[f"item.{MOD}.{name}_slice"] = f"Slice of {pie_display(filling)}"
    lang.update(TEXT)


def loot(out, write):
    write(out / f"{PIES['oven']}.json", self_drop(PIES["oven"]))
    for block in [pie_name(f) for f in PIES["fillings"]] + [PIES["burnt"]]:
        write(out / f"{block}.json", self_drop(block, {"type": "minecraft:match_block", "blocks": rid(block), "state": {"bites": "0"}}))


def tags(tags):
    tags.add("block", "minecraft:mineable/pickaxe", rid(PIES["oven"]))
    for wood in PIES["wood"]:
        tags.add("item", PIES["wood_tag"], wood)
    for filling in PIES["fillings"]:
        tags.add("item", "c:foods", rid(f"{pie_name(filling)}_slice"))
        tags.add("item", "c:foods/pie", rid(f"{pie_name(filling)}_slice"))
