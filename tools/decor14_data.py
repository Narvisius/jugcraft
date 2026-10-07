"""JSON resources for the fourteenth batch of Halloween decorations, costumes, from tools/agriculture.py: the six outfits
(their item models, names, equipment assets and tags) and the Costume Trunk (models, blockstate, name, messages, loot,
tags), and the boxes the client draws each outfit from (assets/jugcraft/costumes.json, read by CostumeLayer).

Called from agriculture_data.py (assets, loot, tags); decor14_textures.py paints each outfit's texture on the same boxes.
Formats follow vanilla Minecraft 26.3's own files and the earlier batches'.

An outfit is a list of pieces. A piece hangs from one of the wearer's body parts (head, body, right_arm, left_arm,
right_leg, left_leg) and holds boxes in that part's own space, in pixels: y runs down, the wearer faces -z, and +x is
the wearer's left. A piece may first move through joints, each a point (from the last joint, or the part) and an axis
it turns about by a named motion (CostumeLayer.angle): the cape flares and wraps, tails sway, wings flap. A box's faces
are laid out on the texture as a vanilla model box's are (see CostumeLayer).
"""
import math

from agriculture import OUTFITS, COSTUME_TRUNK, COSTUME_TAG, COSTUME_HAT_TAG
from decor_data import MOD, HORIZONTAL, rid, box, block_model, self_drop, flat_item, turned

PARTS = ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")
MOTIONS = ("cape_flare", "cape_side_left", "cape_side_right", "cape_front_left", "cape_front_right", "tail_sway", "tail_lift",
           "tail_curl", "wolf_tail", "wing_left", "wing_right")


def b(x0, y0, z0, x1, y1, z1, paint):
    """A box from (x0, y0, z0) to (x1, y1, z1) painted by `paint` (decor14_textures.PAINTERS)."""
    return {"from": (x0, y0, z0), "to": (x1, y1, z1), "paint": paint}


def piece(part, boxes, joints=()):
    return {"part": part, "joints": [list(j) for j in joints], "boxes": boxes}


def limbs(paint, grow=0.5, legs=True, arms=True):
    """Boxes a little bigger than the wearer's arms and legs (clear of a player skin's outer layer, 0.25 out)."""
    out = []
    if arms:
        out += [piece("right_arm", [b(-3 - grow, -2 - grow, -2 - grow, 1 + grow, 10 + grow, 2 + grow, paint)]),
                piece("left_arm", [b(-1 - grow, -2 - grow, -2 - grow, 3 + grow, 10 + grow, 2 + grow, paint)])]
    if legs:
        out += [piece(leg, [b(-2 - grow, -grow, -2 - grow, 2 + grow, 12 + grow, 2 + grow, paint)]) for leg in ("right_leg", "left_leg")]
    return out


def torso(paint, grow=0.5):
    return piece("body", [b(-4 - grow, -grow, -2 - grow, 4 + grow, 12 + grow, 2 + grow, paint)])


def hood(paint, grow=0.75):
    return piece("head", [b(-4 - grow, -8 - grow, -4 - grow, 4 + grow, grow, 4 + grow, paint)])


def vampire_cape():
    """A high red-lined collar, and a long black cape from the shoulders in five panels: the back, and on each side a
    side panel and a front panel hinged one after the other, so the cape can flare out behind or wrap round."""
    top = (0, 0.5, 2.6, "x", "cape_flare")
    collar = [b(-5, -5, 1.6, 5, 0.5, 2.6, "collar"), b(4.2, -4, -1.5, 5.2, 0.5, 1.6, "collar"), b(-5.2, -4, -1.5, -4.2, 0.5, 1.6, "collar")]
    return [piece("body", collar),
            piece("body", [b(-8, 0, 0, 8, 21, 0.5, "cape")], [top]),
            piece("body", [b(0, 0, 0, 4, 21, 0.5, "cape")], [top, (8, 0, 0, "y", "cape_side_left")]),
            piece("body", [b(0, 0, 0, 6, 21, 0.5, "cape")], [top, (8, 0, 0, "y", "cape_side_left"), (4, 0, 0, "y", "cape_front_left")]),
            piece("body", [b(-4, 0, 0, 0, 21, 0.5, "cape")], [top, (-8, 0, 0, "y", "cape_side_right")]),
            piece("body", [b(-6, 0, 0, 0, 21, 0.5, "cape")], [top, (-8, 0, 0, "y", "cape_side_right"), (-4, 0, 0, "y", "cape_front_right")])]


def mummy_wraps():
    """Linen bandages over everything, a slit for the eyes, and two loose ends trailing."""
    return ([hood("mummy_head"), torso("wraps")] + limbs("wraps")
            + [piece("left_arm", [b(3.5, 4, -1, 3.75, 13, 0.5, "strip")]), piece("body", [b(-4.75, 9, -2, -4.5, 16, -0.5, "strip")])])


def skeleton_suit():
    """A black suit painted with bones (a skull mask, ribs, arm and leg bones), which glow in the dark."""
    return [hood("skull", 0.6), torso("ribs", 0.35)] + limbs("arm_bones", 0.35, legs=False) + limbs("leg_bones", 0.35, arms=False)


def werewolf_mask():
    """A wolf's head with a snout and pricked ears, shaggy fur over the body, arms and legs, claws, and a bushy tail."""
    claws = {"right_arm": (-3, -1.5, 0), "left_arm": (-1, 0.5, 2)}
    out = [piece("head", [b(-4.75, -8.75, -4.75, 4.75, 0.75, 4.75, "wolf_head"), b(-2, -4.5, -8.25, 2, -1, -4.75, "snout"),
                          b(-4, -11.75, -1, -1.5, -8.75, 0.5, "wolf_ear"), b(1.5, -11.75, -1, 4, -8.75, 0.5, "wolf_ear")]),
           torso("fur")] + limbs("fur")
    for arm, xs in claws.items():
        out.append(piece(arm, [b(x, 10.5, -2, x + 0.75, 12, -1, "claw") for x in xs]))
    out.append(piece("body", [b(-1.5, -1.5, 0, 1.5, 1.5, 8, "tail_fur")], [(0, 10, 2.5, "y", "tail_sway"), (0, 0, 0, "x", "wolf_tail")]))
    return out


def cat_ears_and_tail():
    """Cat ears on a headband, a black catsuit with a bell at the neck, and a long tail that sways and curls."""
    tail = [(0, 11, 2.3, "y", "tail_sway"), (0, 0, 0, "x", "tail_lift")]
    return ([piece("head", [b(-4.6, -8.6, -1, 4.6, -7.8, 0.2, "band"), b(-4, -11.2, -1.2, -1, -8.6, 0.2, "cat_ear"),
                            b(1, -11.2, -1.2, 4, -8.6, 0.2, "cat_ear")]),
             piece("body", [b(-4.3, -0.3, -2.3, 4.3, 12.3, 2.3, "catsuit"), b(-1, 0, -3, 1, 2, -2.3, "bell")])]
            + limbs("catsuit", 0.3)
            + [piece("body", [b(-0.75, -0.75, 0, 0.75, 0.75, 6, "tail")], tail),
               piece("body", [b(-0.7, -0.7, 0, 0.7, 0.7, 5, "tail")], tail + [(0, 0, 6, "x", "tail_curl")]),
               piece("body", [b(-0.65, -0.65, 0, 0.65, 0.65, 4, "tail_tip")], tail + [(0, 0, 6, "x", "tail_curl"), (0, 0, 5, "x", "tail_curl")])])


def bat_wings():
    """A hood with tall bat ears, a dark furry suit, and leathery wings on the back that spread and flap off the ground."""
    return ([hood("bat_hood"), piece("head", [b(-4.2, -12.75, -0.5, -1.2, -8.75, 0.5, "bat_ear"), b(1.2, -12.75, -0.5, 4.2, -8.75, 0.5, "bat_ear")]),
             torso("bat_suit", 0.3)] + limbs("bat_suit", 0.3)
            + [piece("body", [b(0, 0, 0, 12, 11, 0.25, "wing")], [(1.5, 1, 2.3, "y", "wing_left")]),
               piece("body", [b(-12, 0, 0, 0, 11, 0.25, "wing")], [(-1.5, 1, 2.3, "y", "wing_right")])])


GEOMETRY = {"vampire_cape": vampire_cape, "mummy_wraps": mummy_wraps, "skeleton_suit": skeleton_suit, "werewolf_mask": werewolf_mask,
            "cat_ears_and_tail": cat_ears_and_tail, "bat_wings": bat_wings}


def texels(lo, hi):
    """A box's width, height and depth on the texture: whole texels, at least one."""
    return tuple(max(1, math.ceil(round(h - l, 3))) for l, h in zip(lo, hi))


def layout(name):
    """The outfit's pieces with every box given its place on the texture (u, v and its texel sizes w, h, d), packed
    in rows 128 texels wide, and the texture's size (128 wide, as tall as it needs, a power of two)."""
    pieces = GEOMETRY[name]()
    x = y = row = 0
    for p in pieces:
        for bx in p["boxes"]:
            w, h, d = texels(bx["from"], bx["to"])
            width, height = 2 * (d + w), d + h
            if x + width > 128:
                x, y, row = 0, y + row, 0
            bx["uv"] = (x, y, w, h, d)
            x += width
            row = max(row, height)
    used = y + row
    size = 32
    while size < used:
        size *= 2
    return pieces, (128, size)


def costume_json():
    out = {}
    for name in OUTFITS:
        pieces, size = layout(name)
        entry = {"texture": rid(f"textures/entity/costume/{name}.png"), "size": list(size), "pieces": [
            {"part": p["part"], "joints": p["joints"],
             "boxes": [list(bx["from"]) + list(bx["to"]) + list(bx["uv"]) for bx in p["boxes"]]} for p in pieces]}
        if OUTFITS[name].get("glow"):
            entry["glow"] = rid(f"textures/entity/costume/{name}_glow.png")
        out[name] = entry
    return out


TRUNK_TEXTURES = {"wood": "costume_trunk_wood", "lid": "costume_trunk_lid", "brass": "costume_trunk_brass",
                  "inside": "costume_trunk_inside", "clothes": "costume_trunk_clothes"}


def trunk(open_lid):
    """A steamer trunk of dark wood bound in brass, with a purple-labelled lid; open, the lid stands back and the
    costumes heaped inside show."""
    w, br = "#wood", "#brass"
    elements = [box((1, 0, 3), (15, 9, 13), w, textures={"up": "#inside"} if open_lid else None),
                box((0.5, 0, 2.5), (2, 9, 4), br), box((14, 0, 2.5), (15.5, 9, 4), br), box((0.5, 0, 12), (2, 9, 13.5), br),
                box((14, 0, 12), (15.5, 9, 13.5), br), box((7, 6, 2.5), (9, 8.5, 3), br)]
    if open_lid:
        elements.append(box((2, 6, 4), (14, 9.5, 12), "#clothes", faces=("up",)))
        elements.append(box((1, 9, 13), (15, 18, 15.5), "#lid", rotation={"origin": [8, 9, 13], "axis": "x", "angle": -22.5}))
    else:
        elements.append(box((1, 9, 3), (15, 12, 13), "#lid"))
        elements.append(box((0.5, 9, 7.25), (15.5, 12.5, 8.75), br))
    textures = dict(TRUNK_TEXTURES)
    return block_model(textures, elements, TRUNK_TEXTURES["wood"])


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"
    for name, info in OUTFITS.items():
        flat_item(root, write, name)
        lang[f"item.{MOD}.{name}"] = info["display"]
        # No layers: the armor layer draws nothing and the item isn't drawn on the head; CostumeLayer draws the outfit.
        write(root / "equipment" / f"{name}.json", {"layers": {}})
    write(root / "costumes.json", costume_json())

    chest = COSTUME_TRUNK["block"]
    write(models / f"{chest}.json", trunk(False))
    write(models / f"{chest}_open.json", trunk(True))
    write(states / f"{chest}.json", {"variants": {
        f"facing={facing},open={str(o).lower()}": turned(rid(f"block/{chest}{'_open' if o else ''}"), facing)
        for facing in HORIZONTAL for o in (False, True)}})
    write(items / f"{chest}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{chest}")}})
    lang[f"block.{MOD}.{chest}"] = COSTUME_TRUNK["display"]
    for key, text in {"stored": "Packed away (%s of %s)", "full": "The trunk is full", "empty": "The trunk is empty",
                      "changed": "You change into the %s", "helmet": "Take your helmet off to change",
                      "taken": "You take out the %s"}.items():
        lang[f"message.{MOD}.{chest}.{key}"] = text


def loot(out, write):
    """The trunk drops itself (its costumes spill out, from the block entity)."""
    write(out / f"{COSTUME_TRUNK['block']}.json", self_drop(COSTUME_TRUNK["block"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(COSTUME_TRUNK["block"]))
    for name in OUTFITS:
        tags.add("item", COSTUME_TAG, rid(name))
        tags.add("item", COSTUME_HAT_TAG, rid(name))
