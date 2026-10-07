"""JSON resources for the seventh batch of Halloween decorations, the haunted house inside, from tools/agriculture.py:
the Haunted Chandelier, the Phantom Pipe Organ, the Suit of Armor, the Dust Sheet, the Spirit Mirror, Tattered
Curtains and the Creepy Doll; their names, messages, loot and tags, and the quads the client draws the chandelier, the
suit's helmet and the doll's head from (assets/jugcraft/decor7_quads.json).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files and the earlier
batches' (tools/decor_data.py, decor6_data.py). Block model rotations are right-handed: about y, a positive angle
turns +z toward +x.

The organ (three blocks wide, two tall) and the suit of armor (two tall) are modelled whole, then cut into one model
per block (`split`): each box is clipped to the block, and the faces where it was cut are left out. Their items show
the whole thing scaled down (`scaled`).
"""
import math

from agriculture import HAUNTED_CHANDELIER, PIPE_ORGAN, SUIT_OF_ARMOR, DUST_SHEET, SPIRIT_MIRROR, TATTERED_CURTAINS, CREEPY_DOLL
from decor_data import MOD, HORIZONTAL, SIDES, ALL, rid, turned, box, block_model, flat_item, self_drop
from decor3_data import fitted
from decor6_data import quads
from halloween_data import match_block

AXIS_FACES = {0: ("west", "east"), 1: ("down", "up"), 2: ("north", "south")}


def split(elements, cell):
    """The part of `elements` inside the block at (cx, cy) (in blocks), moved into that block: boxes clipped to it,
    the faces on their cuts left out. Only unrotated boxes."""
    cx, cy = cell
    lo_cell, hi_cell = (cx * 16, cy * 16, -16), (cx * 16 + 16, cy * 16 + 16, 32)
    out = []
    for element in elements:
        assert "rotation" not in element and "rotations" not in element
        frm, to = element["from"], element["to"]
        lo = [max(frm[k], lo_cell[k]) for k in range(3)]
        hi = [min(to[k], hi_cell[k]) for k in range(3)]
        if any(hi[k] - lo[k] <= 1e-6 for k in range(3)):
            continue
        faces = dict(element["faces"])
        for k in range(2):
            low_face, high_face = AXIS_FACES[k]
            if lo[k] > frm[k]:
                faces.pop(low_face, None)
            if hi[k] < to[k]:
                faces.pop(high_face, None)
        if not faces:
            continue
        shift = (cx * 16, cy * 16, 0)
        out.append({**element, "from": [round(lo[k] - shift[k], 4) for k in range(3)],
                    "to": [round(hi[k] - shift[k], 4) for k in range(3)], "faces": faces})
    return out


def scaled(elements, factor, offset):
    """`elements` (unrotated) scaled about the origin by `factor`, then moved by `offset` (for an item model)."""
    out = []
    for element in elements:
        assert "rotations" not in element
        moved = {**element, "from": [round(v * factor + offset[k], 4) for k, v in enumerate(element["from"])],
                 "to": [round(v * factor + offset[k], 4) for k, v in enumerate(element["to"])]}
        if "rotation" in element:
            rotation = dict(element["rotation"])
            rotation["origin"] = [round(v * factor + offset[k], 4) for k, v in enumerate(rotation["origin"])]
            moved["rotation"] = rotation
        out.append(moved)
    return out


def bar(p, q, y0, y1, width, texture):
    """A bar of `width` from (x, z) point p to q between heights y0 and y1, turned about y (by a multiple of 22.5
    degrees, at most 45 either way) to run from one to the other."""
    mx, mz = (p[0] + q[0]) / 2, (p[1] + q[1]) / 2
    length = math.hypot(q[0] - p[0], q[1] - p[1])
    direction = math.degrees(math.atan2(q[1] - p[1], q[0] - p[0]))  # from +x toward +z
    # A bar along x turned by a points along -a; one along z turned by a points along 90 - a.
    for along_x in (True, False):
        angle = ((-direction if along_x else 90 - direction) + 90) % 180 - 90
        if abs(angle) <= 45 and abs(angle / 22.5 - round(angle / 22.5)) < 1e-6:
            break
    else:
        raise ValueError(f"No 22.5-degree bar from {p} to {q}")
    h, w = length / 2, width / 2
    lo = (mx - h, y0, mz - w) if along_x else (mx - w, y0, mz - h)
    hi = (mx + h, y1, mz + w) if along_x else (mx + w, y1, mz + h)
    element = box(lo, hi, texture)
    if abs(angle) > 1e-6:
        element["rotation"] = {"origin": [mx, y0, mz], "axis": "y", "angle": round(angle, 4)}
    return element


# ---------------------------------------------------------------- the haunted chandelier

CHANDELIER_TEXTURES = {"iron": "haunted_chandelier_iron", "wax": "haunted_chandelier_wax"}
RING = 5.5


def candle_spots():
    """Each candle's centre (x, z), every 45 degrees round the ring, as HauntedChandelierBlock.wick has them."""
    return [(8 + RING * math.cos(math.radians(i * 45)), 8 + RING * math.sin(math.radians(i * 45))) for i in range(HAUNTED_CHANDELIER["candles"])]


def chandelier_elements():
    """A ceiling plate and a chain down to a stem, a hub with a finial below, four spokes and an octagonal ring, and a
    candle in a drip pan at each corner of the ring."""
    iron, wax = "#iron", "#wax"
    elements = [box((6, 15.5, 6), (10, 16, 10), iron),
                box((7.25, 13, 7.75), (8.75, 15.5, 8.25), iron), box((7.75, 10.5, 7.25), (8.25, 13.5, 8.75), iron),
                box((7.5, 6, 7.5), (8.5, 10.5, 8.5), iron),
                box((6.5, 3.5, 6.5), (9.5, 6, 9.5), iron),
                box((7.25, 1.5, 7.25), (8.75, 3.5, 8.75), iron), box((7.75, 0.5, 7.75), (8.25, 1.5, 8.25), iron),
                box((8 - RING, 4.25, 7.5), (8 + RING, 5, 8.5), iron), box((7.5, 4.25, 8 - RING), (8.5, 5, 8 + RING), iron)]
    spots = candle_spots()
    for i, p in enumerate(spots):
        elements.append(bar(p, spots[(i + 1) % len(spots)], 4.25, 5, 0.75, iron))
    for x, z in spots:
        elements += [box((x - 1.25, 5, z - 1.25), (x + 1.25, 5.75, z + 1.25), iron),
                     box((x - 0.75, 5.75, z - 0.75), (x + 0.75, 8.25, z + 0.75), wax),
                     box((x - 0.125, 8.25, z - 0.125), (x + 0.125, 8.5, z + 0.125), iron)]
    return elements


# ---------------------------------------------------------------- the phantom pipe organ

ORGAN_TEXTURES = {"case": "phantom_pipe_organ_case", "panel": "phantom_pipe_organ_panel", "pipe": "phantom_pipe_organ_pipe",
                  "mouth": "phantom_pipe_organ_mouth", "pedals": "phantom_pipe_organ_pedals", "music": "phantom_pipe_organ_music"}


def organ_pipe_tops():
    """Thirteen pipes, tallest in the middle, falling away to each side."""
    return [29 - abs(i - 6) * 0.8 for i in range(13)]


def organ_elements():
    """The whole organ facing north (its keyboard toward -z), 48 wide and 32 tall: a carved case on a plinth with a
    pedalboard, the key bed (the keys are drawn by the client), a music desk with a sheet of music, two carved towers
    and thirteen pipes before a back panel under a cornice."""
    elements = [box((0, 0, 6.5), (48, 1, 16), "#case"),
                box((1, 1, 7), (47, 10, 16), "#case", textures={"north": "#panel"}),
                box((8, 0, 1), (40, 0.75, 7), "#case", textures={"up": "#pedals"}),
                box((5, 10, 1.5), (43, 12, 8), "#case"),
                box((4, 10, 1.5), (5, 13.5, 8), "#case"), box((43, 10, 1.5), (44, 13.5, 8), "#case"),
                box((4, 12, 8), (44, 20, 9.5), "#case", textures={"north": "#panel"}),
                box((17, 13.5, 7.5), (31, 18.5, 8), "#case", textures={"north": "#music"}),
                box((4, 20, 12), (44, 29, 16), "#case", textures={"north": "#panel"}),
                box((3.5, 29, 11), (44.5, 30.5, 16), "#case")]
    for x0 in (0, 44):
        elements += [box((x0, 1, 6.5), (x0 + 4, 30, 16), "#case", textures={"north": "#panel"}),
                     box((x0 - 0.0, 30, 6), (x0 + 4, 31, 16), "#case"),
                     box((x0 + 1.25, 31, 10), (x0 + 2.75, 32, 11.5), "#pipe")]
    for i, top in enumerate(organ_pipe_tops()):
        x = 6 + i * 3
        elements += [box((x + 0.5, 19, 10.5), (x + 1.5, 20, 11.5), "#pipe"),
                     box((x, 20, 10), (x + 2, top, 12), "#pipe"),
                     box((x + 0.25, 21, 9.9), (x + 1.75, 22.5, 10), "#mouth", faces=("north",))]
    return elements


# ---------------------------------------------------------------- the suit of armor

ARMOR_TEXTURES = {"steel": "suit_of_armor_steel", "mail": "suit_of_armor_mail", "wood": "suit_of_armor_wood", "visor": "suit_of_armor_visor",
                  "plume": "suit_of_armor_plume", "blade": "suit_of_armor_blade"}


def armor_elements():
    """A suit of plate facing north on a wooden stand, 32 pixels tall to the gorget's top (25), holding a halberd
    upright in its right gauntlet; the helmet is apart (`helmet_elements`)."""
    steel, mail, wood, blade = "#steel", "#mail", "#wood", "#blade"
    elements = [box((2, 0, 2), (14, 1.5, 14), wood), box((7.5, 1.5, 10.5), (8.5, 18, 11.5), wood)]
    for x0 in (4.5, 8.5):                                                            # the legs, left then right
        elements += [box((x0, 1.5, 4.5), (x0 + 3, 3, 10), steel),                     # sabatons
                     box((x0 + 0.25, 3, 6), (x0 + 2.75, 9, 9.5), steel),               # greaves
                     box((x0, 9, 5.5), (x0 + 3, 10.5, 9.5), steel),                    # poleyns
                     box((x0 + 0.25, 10.5, 6), (x0 + 2.75, 15, 9.5), steel)]           # cuisses
    elements += [box((7.25, 13, 6.5), (8.75, 15, 9.5), mail),
                 box((4.25, 15, 5.5), (11.75, 18, 10), steel),                           # tassets
                 box((4.5, 18, 5.5), (11.5, 24.5, 10.5), steel),                         # cuirass
                 box((7.5, 18.5, 5.25), (8.5, 24, 5.5), steel, faces=("north", "up", "down", "east", "west")),
                 box((5.5, 24.5, 6), (10.5, 25.5, 10), mail)]                            # gorget
    for x0 in (2, 11.25):
        elements.append(box((x0, 22, 5), (x0 + 2.75, 25, 11), steel))                  # pauldrons
    elements += [box((2.5, 18.5, 6.5), (4.5, 22, 9.5), steel), box((2.25, 17.5, 6.25), (4.75, 18.5, 9.75), steel),
                 box((2.5, 13, 6.5), (4.5, 17.5, 9.5), steel), box((2.25, 11, 6.25), (4.75, 13, 9.75), steel)]   # left arm
    elements += [box((11.5, 18.5, 6.5), (13.5, 22, 9.5), steel), box((11.25, 17.5, 6.25), (13.75, 18.5, 9.75), steel),
                 box((11.5, 15.5, 4.5), (13.5, 17.5, 9.5), steel), box((11.25, 14.5, 2.25), (13.75, 17.5, 4.5), steel)]  # right arm
    elements += [box((12.25, 1.5, 3), (13, 31.5, 3.75), wood),                           # the halberd's shaft
                 box((12.375, 25, 0.5), (12.875, 29.5, 3), blade),                       # its axe blade
                 box((12.375, 26.5, 3.75), (12.875, 27.5, 5.5), blade),                  # its back spike
                 box((12.4, 31.5, 3.15), (12.85, 32, 3.6), blade)]                       # its point
    return elements


def helmet_elements():
    """The helmet facing north: a great helm with its visor on the front, a comb and a red plume (y from 25)."""
    return [box((4.75, 25, 4.75), (11.25, 31.5, 11.25), "#steel", textures={"north": "#visor"}, uvs={"north": (0, 0, 16, 16)}),
            box((7.5, 31.5, 5), (8.5, 32.5, 11), "#steel"),
            box((7.5, 32.5, 7), (8.5, 35, 11.5), "#plume")]


def moved(elements, dy):
    return [{**e, "from": [e["from"][0], round(e["from"][1] + dy, 4), e["from"][2]],
             "to": [e["to"][0], round(e["to"][1] + dy, 4), e["to"][2]]} for e in elements]


# ---------------------------------------------------------------- the spirit mirror

def mirror_model():
    """An arched gilt frame round silvered glass, hung on the wall to the south, facing north."""
    gilt = "#gilt"
    return block_model({"gilt": "spirit_mirror_gilt", "glass": "spirit_mirror_glass"}, [
        box((2.5, 1.5, 15), (13.5, 13.5, 15.5), "#glass", faces=("north",)),
        box((1.5, 0.5, 14.25), (14.5, 1.5, 16), gilt),
        box((1.5, 1.5, 14.25), (2.5, 13.5, 16), gilt), box((13.5, 1.5, 14.25), (14.5, 13.5, 16), gilt),
        box((1.5, 13.5, 14.25), (14.5, 14.5, 16), gilt),
        box((3.5, 14.5, 14.25), (12.5, 15.25, 16), gilt), box((6.5, 15.25, 14.25), (9.5, 16, 16), gilt),
        box((1, 0, 14), (3, 2, 16), gilt), box((13, 0, 14), (15, 2, 16), gilt),
        box((7, 0, 14.5), (9, 0.5, 16), gilt)], "spirit_mirror_gilt")


# ---------------------------------------------------------------- the creepy doll

DOLL_TEXTURES = {"dress": "creepy_doll_dress", "lace": "creepy_doll_lace", "stocking": "creepy_doll_stocking", "shoe": "creepy_doll_shoe",
                 "porcelain": "creepy_doll_porcelain", "face": "creepy_doll_face", "hair": "creepy_doll_hair", "bow": "creepy_doll_bow"}


def doll_body():
    """The doll sitting facing north, legs out in front: a velvet dress with a lace collar, white stockings and black
    shoes, porcelain hands."""
    elements = [box((4.5, 0, 5.5), (11.5, 3.5, 11.5), "#dress"),
                box((5.5, 3.5, 6.5), (10.5, 7.5, 10), "#dress"),
                box((5.25, 7, 6.25), (10.75, 8, 10.25), "#lace")]
    for x0 in (5.75, 8.75):
        elements += [box((x0, 0, 1.5), (x0 + 1.5, 1.5, 5.5), "#stocking"), box((x0 - 0.25, 0, 0.75), (x0 + 1.75, 2, 2), "#shoe")]
    for x0 in (4, 10.5):
        elements += [box((x0, 4, 7), (x0 + 1.5, 7.5, 8.5), "#dress"), box((x0 + 0.1, 3, 7.1), (x0 + 1.4, 4, 8.4), "#porcelain")]
    return elements


def doll_head():
    """The porcelain head (pivoting at the neck, (8, 8, 8.5)), its face to the north, ringlets of hair and a bow."""
    return [box((5, 8, 5.5), (11, 14, 11.5), "#porcelain", textures={"north": "#face"}, uvs={"north": (0, 0, 16, 16)}),
            box((4.75, 9.5, 6.5), (11.25, 14.5, 11.75), "#hair", faces=("south", "east", "west", "up", "down")),
            box((4.25, 8.5, 7.5), (4.75, 11, 9), "#hair"), box((11.25, 8.5, 7.5), (11.75, 11, 9), "#hair"),
            box((6, 14.5, 8.5), (10, 15.5, 9.5), "#bow")]


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    chandelier = HAUNTED_CHANDELIER["block"]
    # Drawn by the client so it can sway; the block model only gives the particles. The item shows it whole.
    write(models / f"{chandelier}.json", {"textures": {"particle": rid(f"block/{CHANDELIER_TEXTURES['iron']}")}})
    write(models / f"{chandelier}_item.json", fitted(block_model(CHANDELIER_TEXTURES, chandelier_elements(), CHANDELIER_TEXTURES["iron"])))
    write(states / f"{chandelier}.json", {"variants": {f"burning={n},lit={str(lit).lower()}": {"model": rid(f"block/{chandelier}")}
                                                       for lit in (False, True) for n in range(HAUNTED_CHANDELIER["candles"] + 1)}})
    write(root / "items" / f"{chandelier}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{chandelier}_item")}})
    lang[f"block.{MOD}.{chandelier}"] = HAUNTED_CHANDELIER["display"]

    organ = PIPE_ORGAN["block"]
    whole = organ_elements()
    parts = PIPE_ORGAN["width"] * PIPE_ORGAN["height"]
    for part in range(parts):
        write(models / f"{organ}_{part}.json", block_model(ORGAN_TEXTURES, split(whole, (part % 3, part // 3)), ORGAN_TEXTURES["case"]))
    write(states / f"{organ}.json", {"variants": {
        f"facing={f},part={p},playing={str(on).lower()},powered={str(pw).lower()}": turned(rid(f"block/{organ}_{p}"), f)
        for f in HORIZONTAL for p in range(parts) for on in (False, True) for pw in (False, True)}})
    write(models / f"{organ}_item.json", fitted(block_model(ORGAN_TEXTURES, scaled(whole, 1 / 3, (0, 2.5, 5.5)), ORGAN_TEXTURES["case"])))
    write(root / "items" / f"{organ}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{organ}_item")}})
    lang[f"block.{MOD}.{organ}"] = PIPE_ORGAN["display"]

    armor = SUIT_OF_ARMOR["block"]
    suit = armor_elements()
    write(models / f"{armor}_lower.json", block_model(ARMOR_TEXTURES, split(suit, (0, 0)), ARMOR_TEXTURES["steel"]))
    write(models / f"{armor}_upper.json", block_model(ARMOR_TEXTURES, split(suit, (0, 1)), ARMOR_TEXTURES["steel"]))
    write(states / f"{armor}.json", {"variants": {f"facing={f},half={h}": turned(rid(f"block/{armor}_{h}"), f)
                                                  for f in HORIZONTAL for h in ("lower", "upper")}})
    write(models / f"{armor}_item.json", fitted(block_model(ARMOR_TEXTURES, scaled(suit + helmet_elements(), 0.45, (4.4, 0, 4.4)),
                                                            ARMOR_TEXTURES["steel"])))
    write(root / "items" / f"{armor}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{armor}_item")}})
    lang[f"block.{MOD}.{armor}"] = SUIT_OF_ARMOR["display"]

    sheet = DUST_SHEET["block"]
    # The sheet is drawn by the client over whatever it covers.
    write(models / f"{sheet}.json", {"textures": {"particle": rid(f"block/{sheet}")}})
    write(states / f"{sheet}.json", {"variants": {"": {"model": rid(f"block/{sheet}")}}})
    flat_item(root, write, DUST_SHEET["item"])
    lang[f"block.{MOD}.{sheet}"] = DUST_SHEET["display"]
    lang[f"item.{MOD}.{DUST_SHEET['item']}"] = DUST_SHEET["display"]

    mirror = SPIRIT_MIRROR["block"]
    write(models / f"{mirror}.json", mirror_model())
    write(states / f"{mirror}.json", {"variants": {f"facing={f}": turned(rid(f"block/{mirror}"), f) for f in HORIZONTAL}})
    write(root / "items" / f"{mirror}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{mirror}")}})
    lang[f"block.{MOD}.{mirror}"] = SPIRIT_MIRROR["display"]
    lang[f"message.{MOD}.{mirror}.day"] = "Just your reflection. Probably."
    lang[f"message.{MOD}.{mirror}.night"] = "For a moment, someone stands behind you in the glass."

    curtains = TATTERED_CURTAINS["block"]
    write(models / f"{curtains}.json", {"textures": {"particle": rid(f"block/{curtains}_rod")}})
    write(states / f"{curtains}.json", {"variants": {f"facing={f},open={str(o).lower()},part={p}": {"model": rid(f"block/{curtains}")}
                                                     for f in HORIZONTAL for o in (False, True) for p in ("single", "top", "middle", "bottom")}})
    flat_item(root, write, curtains)
    lang[f"block.{MOD}.{curtains}"] = TATTERED_CURTAINS["display"]

    doll = CREEPY_DOLL["block"]
    write(models / f"{doll}.json", block_model(DOLL_TEXTURES, doll_body(), DOLL_TEXTURES["dress"]))
    write(models / f"{doll}_item.json", block_model(DOLL_TEXTURES, doll_body() + doll_head(), DOLL_TEXTURES["dress"]))
    write(states / f"{doll}.json", {"variants": {f"facing={f}": turned(rid(f"block/{doll}"), f) for f in HORIZONTAL}})
    write(root / "items" / f"{doll}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{doll}_item")}})
    lang[f"block.{MOD}.{doll}"] = CREEPY_DOLL["display"]

    write(root / "decor7_quads.json", {"haunted_chandelier": quads(chandelier_elements(), CHANDELIER_TEXTURES),
                                       "suit_of_armor_helmet": quads(moved(helmet_elements(), -16), ARMOR_TEXTURES),
                                       "creepy_doll_head": quads(doll_head(), DOLL_TEXTURES)})


def loot(out, write):
    """Each drops itself once: the organ from its master block, the suit from its lower half. A sheet drops itself (and
    DustSheetBlock adds what it covered)."""
    for block in (HAUNTED_CHANDELIER["block"], DUST_SHEET["block"], SPIRIT_MIRROR["block"], TATTERED_CURTAINS["block"], CREEPY_DOLL["block"]):
        write(out / f"{block}.json", self_drop(block))
    organ = PIPE_ORGAN["block"]
    write(out / f"{organ}.json", self_drop(organ, match_block(organ, part=1)))
    armor = SUIT_OF_ARMOR["block"]
    write(out / f"{armor}.json", self_drop(armor, match_block(armor, half="lower")))


def tags(tags):
    for block in (HAUNTED_CHANDELIER["block"], SUIT_OF_ARMOR["block"], SPIRIT_MIRROR["block"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    tags.add("block", "minecraft:mineable/axe", rid(PIPE_ORGAN["block"]))
    for value in DUST_SHEET["coverable"]:
        tags.add("block", DUST_SHEET["tag"], value)
