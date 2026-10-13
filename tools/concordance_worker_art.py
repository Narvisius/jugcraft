"""Textures for roadmap step 17, spirits, familiars and constructs (tools/concordance_worker_models.py): the 64x64
GeckoLib sheets of the Hearthling, the Gathering Shade and the Clockwork Porter, and the Spirit Anchor's two block
faces. The four item icons (the Bonding Charm, the Spirit Anchor, the Clockwork Porter and the Porter Key) are 16x16
maps in tools/item_icons/ (docs/ITEM_ICONS.md), drawn here by item_icons.draw. tools/concordance_art.py includes these
in its textures().

Provenance: every colour on the three sheets and the two block faces is one the owner painted, read (colours only; no
pixel, shape or layout is copied) from these files in their library, art/owner-library/originals/Blocks/:
- the Hearthling: its glowing core from Guns/block/treated_brass_lamp_lit.png and its cap, base band and eyes from the
  unlit Guns/block/treated_brass_lamp.png; its wings from Guns/block/orange_niter_glass_opaque.png; its ember drip from
  Big Cannons and Mounted Guns/textures/item/ember.png (with the lamps' tones);
- the Gathering Shade: its robe, hood, arms and tail from biomes and tree blocks/umbran_log.png with the paler tones of
  Big Cannons and Mounted Guns/textures/block/leadblock.png; the hollow under its hood from biomes and tree
  blocks/umbran_leaves.png; its eyes and the palest wisp of its hands and tail from biomes and tree
  blocks/jacaranda_planks.png;
- the Clockwork Porter: its brass, and the iron of its gauge, keyhole and soles, from the owner's clockwork knight,
  Guns/entity/cog_knight.png; its copper band, head and legs from Big Cannons and Mounted
  Guns/textures/item/copperplate.png; its lens from Guns/block/light_blue_niter_glass_opaque.png; its basket from
  farming and food textures/wooden_basket_side.png;
- the Spirit Anchor: its dark stone from biomes and tree blocks/blackstone_spines.png, with the darkest seams from
  biomes and tree blocks/blackstone_bulb.png and the lightest edge from Guns/block/treated_iron_block.png; its violet
  glow from the bulb of biomes and tree blocks/blackstone_bulb.png.
SOURCES names each file and owner_colours() reads them back, so a check can confirm that every colour drawn here is
one of them. The boxes and faces are painted here from code with fixed seeds; the library files are only read, never
changed. Nothing is read, traced or recoloured from Mojang's files.
"""

import os
import random
import zlib

from PIL import Image

import clean_metal as cm
import concordance_ecology_art as ecology_art
import concordance_worker_models as models
import item_icons

HERE = os.path.dirname(os.path.abspath(__file__))
LIBRARY = os.path.join(HERE, "..", "art", "owner-library", "originals", "Blocks")
# The owner's files whose colours are used, by what they colour (paths under LIBRARY).
SOURCES = {
    "lamp_lit": "Guns/block/treated_brass_lamp_lit.png",
    "lamp": "Guns/block/treated_brass_lamp.png",
    "amber_glass": "Guns/block/orange_niter_glass_opaque.png",
    "ember": "Big Cannons and Mounted Guns/textures/item/ember.png",
    "umbran": "biomes and tree blocks/umbran_log.png",
    "umbran_leaves": "biomes and tree blocks/umbran_leaves.png",
    "lead": "Big Cannons and Mounted Guns/textures/block/leadblock.png",
    "jacaranda": "biomes and tree blocks/jacaranda_planks.png",
    "cog_knight": "Guns/entity/cog_knight.png",
    "copper": "Big Cannons and Mounted Guns/textures/item/copperplate.png",
    "lens_glass": "Guns/block/light_blue_niter_glass_opaque.png",
    "basket": "farming and food textures/wooden_basket_side.png",
    "blackstone": "biomes and tree blocks/blackstone_spines.png",
    "bulb": "biomes and tree blocks/blackstone_bulb.png",
    "treated_iron": "Guns/block/treated_iron_block.png",
}

# Ramps, darkest first (ecology_art._paint_box paints a box's bottom in tone 1, its sides in 2 with a lit top row in 3,
# its top in 3, and moves a few specks one tone either way).
# The Hearthling: a lit lamp under a dark brass cap, amber-glass wings and an ember drip.
GLOW = [(206, 109, 77), (230, 145, 92), (247, 180, 110), (247, 203, 108), (255, 218, 156)]  # lamp_lit
GLOW_HOT = (255, 236, 220)  # lamp_lit: the brightest heart of the glow
CAP = [(71, 29, 21), (95, 41, 31), (126, 61, 52), (165, 89, 74), (206, 109, 77)]  # lamp
WING = [(133, 91, 38), (157, 105, 38), (192, 126, 52), (210, 154, 92), (224, 196, 167)]  # amber_glass
EMBER = [(206, 109, 77), (255, 132, 81), (255, 152, 85), (253, 177, 128), (255, 236, 220)]  # lamp, ember, lamp_lit
# The Gathering Shade: dusky violet-grey cloth, paler towards its wispy ends, a dark hollow and pale lilac eyes.
ROBE = [(50, 48, 62), (62, 59, 77), (77, 73, 95), (96, 94, 124), (109, 106, 140)]  # umbran, lead
WISP = [(77, 73, 95), (96, 94, 124), (109, 106, 140), (129, 126, 166), (176, 155, 190)]  # umbran, lead, jacaranda
HOLLOW = [(10, 10, 20), (22, 22, 36)]  # umbran_leaves
EYE = [(203, 177, 220), (243, 220, 243)]  # jacaranda
# The Clockwork Porter: the cog knight's brass and iron, copper, a pale blue lens and a wicker basket.
BRASS = [(153, 90, 61), (175, 121, 79), (206, 160, 90), (228, 183, 99), (251, 220, 125)]  # cog_knight
COPPER = [(107, 32, 26), (127, 47, 35), (166, 72, 49), (204, 97, 65), (243, 131, 97)]  # copper
IRON = [(47, 49, 49), (62, 62, 63), (79, 79, 79), (103, 97, 97), (129, 128, 126)]  # cog_knight
LENS = [(69, 100, 140), (92, 141, 196), (125, 167, 213), (182, 203, 225)]  # lens_glass
WICKER = [(92, 64, 36), (102, 77, 40), (128, 89, 41), (149, 106, 54), (163, 121, 70), (184, 144, 96)]  # basket
# The Spirit Anchor: dark violet-grey stone (seam, dark, mid, light, lit edge) and a faint violet glow.
STONE = [(32, 19, 28), (49, 44, 54), (60, 57, 71), (78, 75, 84), (90, 87, 97)]  # bulb, blackstone, treated_iron
VIOLET = [(43, 1, 120), (96, 6, 171), (131, 8, 228)]  # bulb: its deeper tones, for a faint glow


def owner_colours():
    """Every colour in the owner's files of SOURCES (for a check that each colour drawn here is one of them)."""
    colours = set()
    for path in SOURCES.values():
        with Image.open(os.path.join(LIBRARY, path)) as image:
            pixels = image.convert("RGBA").getcolors(maxcolors=1 << 16)
        colours |= {pixel[:3] for _count, pixel in pixels if pixel[3] > 0}
    return colours


# ------------------------------------------------------------------------------------------------- box-UV faces

def _face(uv, size, side):
    """(x, y, width, height) of one face of a box's UV region, as Bedrock lays it out. The front faces north. On the
    front, column 0 is the model's right (-x); on the back, column 0 is its left (+x)."""
    (u, v), (w, h, d) = uv, size
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}[side]


SIDES = ("right", "front", "left", "back")


def _put(img, face, x, y, colour):
    """A pixel at (x, y) inside a face (negative x or y count from its right or bottom edge)."""
    x0, y0, w, h = face
    cm.put(img, x0 + (x % w), y0 + (y % h), colour)


def _row(img, face, y, colour):
    for x in range(face[2]):
        _put(img, face, x, y, colour)


def _sheet(key, ramps, speckle):
    """Every box of a model painted in its ramp (ecology_art._paint_box), with a fixed seed per box."""
    uvs, sizes = models.SIZES[key]
    img = Image.new("RGBA", (models.TEXTURE, models.TEXTURE), (0, 0, 0, 0))
    for name, uv in uvs.items():
        ecology_art._paint_box(img, uv, sizes[name], ramps[name], zlib.crc32(f"{key}.{name}".encode()),
                               speckle.get(name, 0.1))
    return img


# ------------------------------------------------------------------------------------------------- the Hearthling

def hearthling_sheet():
    """A lit lamp of a body under a dark brass cap: its glow brightest at the heart of each pane, a brass band round
    its foot, two dark eyes on its front; amber wings with a darker vein and edge; an ember drip, hottest at its tip."""
    uvs, sizes = models.HEARTHLING_UV, models.HEARTHLING_SIZES
    img = _sheet("hearthling", {"core": GLOW, "tail": EMBER, "lid": CAP, "knob": CAP, "wing_left": WING,
                                "wing_right": WING},
                 {"core": 0.0, "tail": 0.0, "lid": 0.12, "knob": 0.0, "wing_left": 0.0, "wing_right": 0.0})
    core = uvs["core"], sizes["core"]
    edge, warm, bright, eye, lid = GLOW[1], GLOW[2], GLOW[3], CAP[0], CAP[1]
    pane = [[edge, warm, warm, warm, warm, edge],  # each pane (6 x 4) glows outwards from its heart,
            [edge, bright, GLOW_HOT, GLOW_HOT, bright, edge],
            [edge, warm, bright, bright, warm, edge],
            [CAP[2]] * 6]  # over the lamp's brass foot
    face_ = [[edge, warm, warm, warm, warm, edge],  # and the front is a face: two dark eyes in the glow
             [edge, eye, bright, bright, eye, edge],
             [edge, lid, warm, warm, lid, edge],
             [CAP[2]] * 6]
    for side in SIDES:
        face = _face(*core, side)
        for y, row in enumerate(face_ if side == "front" else pane):
            for x, colour in enumerate(row):
                _put(img, face, x, y, colour)
    top = _face(*core, "top")
    for x in range(top[2]):
        for y in range(top[3]):
            _put(img, top, x, y, GLOW[3] if 0 < x < top[2] - 1 and 0 < y < top[3] - 1 else GLOW[2])
    bottom = _face(*core, "bottom")
    for x in range(bottom[2]):
        for y in range(bottom[3]):
            _put(img, bottom, x, y, CAP[1] if x in (0, bottom[2] - 1) or y in (0, bottom[3] - 1) else CAP[2])
    # The ember drip: orange sides, its tip (the bottom face) hottest.
    tail = uvs["tail"], sizes["tail"]
    for side in SIDES:
        face = _face(*tail, side)
        _put(img, face, 0, 0, EMBER[2])
        _put(img, face, 1, 0, EMBER[1])
        _put(img, face, 0, 1, EMBER[3])
        _put(img, face, 1, 1, EMBER[2])
    bottom = _face(*tail, "bottom")
    for x in range(2):
        for y in range(2):
            _put(img, bottom, x, y, EMBER[4] if (x + y) % 2 == 0 else EMBER[3])
    # The wings: amber membranes, a darker vein from the root and a dark trailing edge; the root is the inner column.
    for name, root_front in (("wing_left", 0), ("wing_right", -1)):
        uv, size = uvs[name], sizes[name]
        for side, root in (("front", root_front), ("back", -1 - root_front)):
            face = _face(uv, size, side)
            for y in range(face[3]):
                for x in range(face[2]):
                    tone = WING[3] if y == 0 else WING[1] if y == face[3] - 1 else WING[2]
                    _put(img, face, x, y, tone)
            for y in range(1, face[3] - 1):
                _put(img, face, root, y, WING[0])  # the vein along the root
            _put(img, face, root, 0, WING[1])
            tip = -1 if root == 0 else 0
            _put(img, face, tip, 1, WING[4])  # the light catching the wingtip
            _put(img, face, tip, -1, WING[0])
        for side in ("right", "left", "top", "bottom"):
            face = _face(uv, size, side)
            for y in range(face[3]):
                for x in range(face[2]):
                    _put(img, face, x, y, WING[1] if side != "top" else WING[2])
    return img


# ------------------------------------------------------------------------------------------------- the Gathering Shade

def shade_sheet():
    """Dusky violet-grey cloth: a hood whose front is a dark hollow with two pale lilac eyes under a lit rim, a robe
    with two folds, long arms paling to wispy hands, and a tail fading to its palest at the tip."""
    uvs, sizes = models.SHADE_UV, models.SHADE_SIZES
    img = _sheet("gathering_shade", {"body": ROBE, "hood": ROBE, "hood_tip": ROBE, "tail_upper": ROBE,
                                     "tail_tip": WISP, "arm_left": ROBE, "arm_right": ROBE},
                 {"body": 0.05, "hood": 0.04, "hood_tip": 0.0, "tail_upper": 0.05, "tail_tip": 0.0, "arm_left": 0.0,
                  "arm_right": 0.0})
    hood = uvs["hood"], sizes["hood"]
    front = _face(*hood, "front")
    w, h = front[2], front[3]
    for x in range(w):
        _put(img, front, x, 0, ROBE[4])  # the hood's lit rim
        _put(img, front, x, 1, ROBE[3] if 0 < x < w - 1 else ROBE[2])
    for y in range(2, h):
        _put(img, front, 0, y, ROBE[3])
        _put(img, front, w - 1, y, ROBE[2])
        for x in range(1, w - 1):
            _put(img, front, x, y, HOLLOW[1] if y == 2 else HOLLOW[0])
    for x in (1, w - 2):  # the eyes, with a faint glow under each
        _put(img, front, x, 3, EYE[1])
        _put(img, front, x, 4, EYE[0])
    # The robe's two folds, down its front and back; a darker hem.
    body = uvs["body"], sizes["body"]
    for side in ("front", "back"):
        face = _face(*body, side)
        for y in range(1, face[3]):
            _put(img, face, 1, y, ROBE[1])
            _put(img, face, face[2] - 2, y, ROBE[1])
    for side in SIDES:
        _row(img, _face(*body, side), -1, ROBE[1])
    # Arms pale towards wispy hands.
    for name in ("arm_left", "arm_right"):
        arm = uvs[name], sizes[name]
        for side in SIDES:
            face = _face(*arm, side)
            _row(img, face, -3, WISP[2])
            _row(img, face, -2, WISP[3])
            _row(img, face, -1, WISP[3])
            _put(img, face, 0, -1, WISP[4])
        bottom = _face(*arm, "bottom")
        for x in range(bottom[2]):
            for y in range(bottom[3]):
                _put(img, bottom, x, y, WISP[4])
    # The tail: the upper part pales at its hem, where it frays into the tip, which pales to its end.
    upper = uvs["tail_upper"], sizes["tail_upper"]
    for side in SIDES:
        face = _face(*upper, side)
        _row(img, face, -1, WISP[1])
    tip = uvs["tail_tip"], sizes["tail_tip"]
    for side in SIDES:
        face = _face(*tip, side)
        for y, tone in enumerate((WISP[1], WISP[2], WISP[3], WISP[3])):
            _row(img, face, y, tone)
        _put(img, face, 0, -1, WISP[4])
    bottom = _face(*tip, "bottom")
    for x in range(bottom[2]):
        for y in range(bottom[3]):
            _put(img, bottom, x, y, WISP[4])
    return img


# ------------------------------------------------------------------------------------------------- the Clockwork Porter

def porter_sheet():
    """Brass with a riveted copper band round the drum and a winding keyhole in its left side, a copper head whose lens
    of pale blue glass catches the light at its top left, copper legs on iron soles, and a woven wicker basket with a
    dark open top."""
    uvs, sizes = models.PORTER_UV, models.PORTER_SIZES
    img = _sheet("clockwork_porter", {"body_drum": BRASS, "body_round": BRASS, "head": COPPER, "lens": BRASS,
                                      "basket": WICKER, "leg_left": COPPER, "leg_right": COPPER},
                 {"body_drum": 0.08, "body_round": 0.08, "head": 0.06, "lens": 0.0, "basket": 0.0, "leg_left": 0.05,
                  "leg_right": 0.05})
    drum = uvs["body_drum"], sizes["body_drum"]
    for side in SIDES:  # the copper band, lit along its top, riveted with brass
        face = _face(*drum, side)
        _row(img, face, 2, COPPER[3])
        _row(img, face, 3, COPPER[2])
        for x in range(1, face[2], 3):
            _put(img, face, x, 3, BRASS[4])
        _row(img, face, -1, BRASS[1])
    left = _face(*drum, "left")  # the winding keyhole, which the Porter Key turns
    for y, tone in ((0, IRON[1]), (1, IRON[0])):
        _put(img, left, left[2] // 2 - 1, y + 4, IRON[3])
        _put(img, left, left[2] // 2, y + 4, tone)
    front = _face(*drum, "front")  # a small gauge on its chest, under the head
    _put(img, front, 3, 1, IRON[4])
    _put(img, front, 4, 1, IRON[3])
    _put(img, front, 3, 0, IRON[3])
    _put(img, front, 4, 0, IRON[2])
    round_ = uvs["body_round"], sizes["body_round"]
    top = _face(*round_, "top")  # rivets round the shoulders
    for x, y in ((0, 0), (top[2] - 1, 0), (0, top[3] - 1), (top[2] - 1, top[3] - 1)):
        _put(img, top, x, y, BRASS[1])
    # The head: copper, a dark socket round where the lens sits on its front.
    head = uvs["head"], sizes["head"]
    face = _face(*head, "front")
    for x in range(1, 3):
        for y in range(1, 3):
            _put(img, face, x, y, COPPER[0])
    # The lens: pale blue glass with a glint at its top left, in a brass rim.
    lens = uvs["lens"], sizes["lens"]
    face = _face(*lens, "front")
    for (x, y), tone in (((0, 0), LENS[3]), ((1, 0), LENS[2]), ((0, 1), LENS[1]), ((1, 1), LENS[0])):
        _put(img, face, x, y, tone)
    # The legs: copper, lit where they meet the body, on iron soles.
    for name in ("leg_left", "leg_right"):
        leg = uvs[name], sizes[name]
        for side in SIDES:
            face = _face(*leg, side)
            _row(img, face, -1, IRON[1])
            _row(img, face, 0, COPPER[3])
        bottom = _face(*leg, "bottom")
        for x in range(bottom[2]):
            for y in range(bottom[3]):
                _put(img, bottom, x, y, IRON[0])
    # The basket: woven wicker (staggered over-and-under), a lit rim, and a dark open top.
    basket = uvs["basket"], sizes["basket"]
    for side in SIDES:
        face = _face(*basket, side)
        for y in range(face[3]):
            for x in range(face[2]):
                over = ((x // 2) + y) % 2 == 0
                tone = WICKER[4] if over else WICKER[2]
                if y == 0:
                    tone = WICKER[5]
                elif y == face[3] - 1:
                    tone = WICKER[1] if over else WICKER[0]
                _put(img, face, x, y, tone)
    top = _face(*basket, "top")
    for x in range(top[2]):
        for y in range(top[3]):
            rim = x in (0, top[2] - 1) or y in (0, top[3] - 1)
            _put(img, top, x, y, WICKER[5] if rim else WICKER[0])
    _put(img, top, 2, 1, WICKER[1])
    _put(img, top, 3, 2, WICKER[1])
    return img


# ------------------------------------------------------------------------------------------------- the Spirit Anchor

def _stone(seed):
    """Dark dressed stone in the manner of docs/NATURAL_TEXTURES.md: a field of the mid tone with irregular blotches of
    two to four pixels in the dark and light tones and a couple of short cracks, every mark wrapping at the edges."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), STONE[2] + (255,))
    shapes = [((0, 0), (1, 0)), ((0, 0), (0, 1)), ((0, 0), (1, 0), (0, 1)), ((0, 0), (1, 0), (1, 1)),
              ((0, 0), (1, 0), (2, 0)), ((0, 0), (1, 0), (0, 1), (1, 1))]
    for i in range(22):
        x, y = rng.randrange(16), rng.randrange(16)
        tone = STONE[1] if i % 2 else STONE[3]
        for dx, dy in rng.choice(shapes):
            cm.put(img, (x + dx) % 16, (y + dy) % 16, tone)
    for _ in range(2):
        x, y = rng.randrange(16), rng.randrange(16)
        for step in range(rng.choice((2, 3))):
            cm.put(img, (x + step) % 16, (y + step // 2) % 16, STONE[0])
    return img


def anchor_side():
    """A face of the shrine: a capstone course over a carved seam, the lantern window between, its arch shadowed and
    its sill lit, glowing a faint violet that is brightest low in its middle, and a base course under a second seam."""
    img = _stone(zlib.crc32(b"spirit_anchor_side"))
    for x in range(16):
        cm.put(img, x, 0, STONE[4] if x % 7 else STONE[3])  # the capstone's lit top edge
        cm.put(img, x, 3, STONE[0])  # the seam under the capstone
        cm.put(img, x, 12, STONE[0])  # the seam over the base course
        cm.put(img, x, 13, STONE[3] if x % 5 else STONE[4])  # the base course's lit top
        cm.put(img, x, 15, STONE[1])
    for x in (4, 11):  # breaks in the seams, where the courses are worn
        cm.put(img, x, 3, STONE[1])
    cm.put(img, 8, 12, STONE[1])
    # The window: an arch over six rows, dark under its head, the deep violet of the spirit's light filling it and
    # rising a little brighter low in its middle (0 the seam's dark, 1 to 3 the glow's tones).
    glow = {"0": STONE[0], "1": VIOLET[0], "2": VIOLET[1], "3": VIOLET[2]}
    for y, row in enumerate([".0000.", "011111", "111111", "111111", "112211", "123321"], start=5):
        for x, tone in enumerate(row, start=5):
            if tone != ".":
                cm.put(img, x, y, glow[tone])
    for x in range(5, 11):
        cm.put(img, x, 11, STONE[4])  # the sill
    for x, y in ((5, 5), (10, 5)):
        cm.put(img, x, y, STONE[0])  # the arch's carved shoulders
    for x in range(6, 10):
        cm.put(img, x, 4, STONE[1])  # shadow over the arch
    for y in range(6, 11):
        cm.put(img, 4, y, STONE[1])  # the jambs: shadowed on the left, lit on the right
        cm.put(img, 11, y, STONE[3])
    return img


def anchor_top():
    """The capstone from above: dressed stone inside a carved border, and at its middle a round vent where the violet
    light shows, faint at its rim and brightest at its heart."""
    img = _stone(zlib.crc32(b"spirit_anchor_top"))
    for i in range(1, 15):  # a carved border: a groove, deepest along the top and left, the field's edge lit inside it
        for x, y in ((i, 1), (1, i)):
            cm.put(img, x, y, STONE[0])
        for x, y in ((i, 14), (14, i)):
            cm.put(img, x, y, STONE[1])
    for i in range(2, 14):
        for x, y in ((i, 2), (2, i)):
            cm.put(img, x, y, STONE[3])
    # The vent (1 to 3 the glow's tones, faint at the rim and brightest at its heart), its carved lip shadowed above
    # and to the left (s) and lit below and to the right (l).
    tones = {"1": VIOLET[0], "2": VIOLET[1], "3": VIOLET[2], "s": STONE[1], "l": STONE[3]}
    for y, row in enumerate([".s11s.", "s1111l", "113211", "112211", "s1111l", ".l11l."], start=5):
        for x, tone in enumerate(row, start=5):
            if tone != ".":
                cm.put(img, x, y, tones[tone])
    return img


# ------------------------------------------------------------------------------------------------- all of them

ICONS = ["bonding_charm", "spirit_anchor", "clockwork_porter", "porter_key"]
SHEETS = {"hearthling": hearthling_sheet, "gathering_shade": shade_sheet, "clockwork_porter": porter_sheet}


def textures():
    out = {("item", name): item_icons.draw(name) for name in ICONS}
    for name, draw in SHEETS.items():
        out[("entity", name)] = draw()
    out[("block", "spirit_anchor_side")] = anchor_side()
    out[("block", "spirit_anchor_top")] = anchor_top()
    return out
