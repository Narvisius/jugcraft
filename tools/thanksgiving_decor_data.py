"""Generated assets for the Thanksgiving decorations (tools/thanksgiving_decor.py; docs/features/thanksgiving-decor.md).

Built from boxes at real size, one texel to a model pixel, in the Harvest Festival Grounds' manner
(tools/harvest_festival_data.py), sharing its willow weave and braid, its produce, gourd, corn and wheat painters, and
the Pumpkin Patch Market's planks, straps and turned wood (tools/pumpkin_patch_data.py), so the table cornucopia is
the Giant Cornucopia's willow and the baskets sit beside the Orchard's crates in one hand. Each prop is one packed
texture (block/<prop>) of pieces painted for the faces they cover (pumpkin_patch_data.panel), its leaves cut-out
sprites drawn from pixel maps here, flat in two or three tones, lit from the top left. Warm autumn palettes, no noise.

Everything is drawn here by code; no Mojang texture is read, traced or copied, and nothing from the owner library is
used (its catalog has no harvest table, basket or garland drawings at this scale: the record says what was searched).

Writes, from agriculture_data.py (assets, loot, tags) and crop_textures.py (textures): each prop's block and item
models, blockstate, name and self-dropping loot; its axe or pickaxe tag; and its texture. The Autumn Leaf Table Runner's
Feast Table models are tools/feast_data.py's (runner `leaves`), drawn on its texture from here (runner_texture).
"""
import math

import decor17_data as d17d
import decor20_data as d20d
import flora_art as fa
import harvest_festival_data as hfd
import pumpkin_patch_data as ppd
import thanksgiving_decor as td
from flora_art import Sculpt, pal, shade
from haunted_midway_data import PUMPKIN, STEM
from harvest_festival_data import (ACORN, APPLE, ASPEN, BRASS, BUTTERNUT, CORN, CREAM, GRAPE, HESSIAN, HUSK, LEAF, MAPLE, TWINE,
                                   WHEAT, WILLOW, acorn_squash, braid, carrot_skin, crossed_x, crossed_y, crossed_z, ears, frond, gourd,
                                   grapes, kernels, produce, rind, turnip_skin, wicker)
from pumpkin_patch_data import IRON, WOOD, flat, hay, lengthwise, panel, planks

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

# Palettes, darkest first.
WHITE_PUMPKIN = pal("7e7666", "a49c88", "c8c0aa", "e2dcc8")
WARTY = pal("8a560e", "b07616", "d49826", "eab842", "f6d26c")
PEAR = pal("586416", "7c8a1e", "a2ae2e", "c4c84a", "e0e07a")
PEAR_BLUSH = pal("7a2e14", "a2441e")
POTATO = pal("5e4020", "84602e", "a88044", "c8a060")
BEET = pal("320a18", "521226", "741c34", "962a46")
BEET_STALK = pal("6a1424", "9a2438")
LEATHER = pal("2e180a", "4a2812", "663a1c", "7e4c28")
ONION = pal("6e3412", "98521e", "c07432", "dc9a4e")
SHADOW = pal("1c1209", "2a1c0e", "3a2814")
OAK_LEAF = pal("4e2c0c", "704014", "925a20", "b07834")
ACORN_NUT = pal("5a3a14", "7c5220", "9e6c2e", "ba8842")
ACORN_CAP = pal("33240f", "4c3818", "665024")
GREEN_APPLE = pal("2e5410", "4a7418", "6a9422", "8cb236")
GOLD_APPLE = pal("8a5e0c", "b88614", "dcaa26", "f0cc52")
RED_LEAF = MAPLE
GOLD_LEAF = ASPEN
ORANGE_LEAF = pal("6e2c08", "a2460e", "cc6416", "e88a2a", "f6ac4e")
LEAF_STEM = pal("3a2410", "553618")
CHINA = pal("948a72", "b8ae94", "d6cdb4", "eae4d0", "f4f0e2")
CHINA_BAND = pal("7a3c12", "a6561c", "c8762c")
SILVER = pal("4a4e56", "6c727a", "969ca4", "bec2c8", "dcdee2")
NAPKIN = pal("7e2c0c", "a84414", "cc6220", "e48436")
LINEN = pal("a89e88", "c8bea6", "e0d8c2", "f0eadc")
GINGHAM = pal("6e1414", "9a2020", "be3228")
CRUST = pal("7a4a1a", "a06a2a", "c28c40", "dcac5e", "eecb86")
ROLL = pal("6a3810", "8e5018", "b06c26", "cc8a3c", "e0a85a")
PUMPKIN_FILLING = pal("7a3008", "a24812", "c4621e", "da7e30")
APPLE_FILLING = pal("8a5a12", "ae7a1e", "cc9a30", "e2ba52")
GRAVY = pal("2e180a", "462612", "5e361c", "74482a")
BUTTER_PAT = pal("a88a2e", "cca844", "e4c662", "f2e092")
WALNUT = pal("24160c", "382414", "4e341e", "64462a", "7a5836")
COPPER = pal("4a2412", "70381c", "965028", "b8683a", "d4864e", "eaaa74")
AMBER_GLASS = pal("7a3606", "a8520c", "d07a18", "eca234", "f8c860", "fde6a0")
TIN = pal("4c4e52", "6e7076", "92949a", "b6b8bc")
WAX = pal("a89c80", "c8bea2", "e0d8c0", "f0ead8")
FLAME = pal("c84a10", "f08a1e", "fcc044", "fee890")
SIGN_GREEN = pal("1a3416", "264a1e", "34602a", "467a38")
SIGN_CREAM = pal("bcae86", "d8cca6", "ece2c2")
BRONZE_FEATHER = pal("2a1a0e", "442a14", "5e3c1c", "7a5028", "946638")
TURKEY_HEAD = pal("5a6c86", "7a8ea8", "a2b4c8")
WATTLE = pal("7a1014", "a81c1c", "cc3428")
BEAK = pal("8a6418", "c09a2c", "e2c04c")
EYE = pal("141014", "f2eee6")
FAN = {"bronze": BRONZE_FEATHER, "orange": ORANGE_LEAF, "red": MAPLE}
SQUIRREL = pal("4e200c", "722e12", "96421a", "b45a26", "cc7438")
SQUIRREL_BELLY = pal("b49a74", "d4bc94", "eadabc")
BARK = pal("2a1c10", "3e2a18", "543a22")
RINGS = pal("8a6838", "a8844a", "c4a060", "d8b878")
TWIG = pal("2e2014", "443020", "5a402c", "70523a")
CIDER = pal("6a2e06", "944410", "b85e1c", "d47c2e")


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


# ---------------------------------------------------------------- painters

def tagged(paint, *tag):
    paint.tag = repr(tag)
    return paint


def ribbed(palette):
    """A pumpkin's or squash's rind in its own colours (harvest_festival_data.rind's ribs): a column lit, its own tone,
    a groove in shadow, its own tone again; its top row lit."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(palette, 3 if y == 0 and p.h > 1 else (2, 3, 2, 1)[x % 4]))
    return tagged(paint, "ribbed", palette)


def crown(palette):
    """A pumpkin's top seen from above: ribs running in to its middle, in rings a texel wide lit and shaded by turns."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                ring = int(max(abs(x - cx), abs(y - cy)))
                p.put(x, y, shade(palette, (2, 3, 2, 1)[ring % 4]))
    return tagged(paint, "crown", palette)


def warty():
    """A warty gourd: orange-gold, its top row lit, raised warts a tone lighter on a staggered lattice (every third texel,
    each row's a texel along from the last's), each with its shadow under it."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                if (x + y) % 3 == 0 and y % 2 == 0:
                    k = 4
                elif (x + y - 1) % 3 == 0 and y % 2 == 1:
                    k = 1
                else:
                    k = 3 if y == 0 and p.h > 1 else 2
                p.put(x, y, shade(WARTY, k))
    return tagged(paint, "warty")


def smooth(palette, glint=False, foot=True):
    """Smooth produce or glaze: its own tone, its top row lit, its bottom row a tone darker (`foot`), and a glint in its
    top left texel (`glint`)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                if glint and (x, y) == (0, 0):
                    k = len(palette) - 1
                elif y == 0 and p.h > 1:
                    k = 2
                elif foot and y == p.h - 1 and p.h > 2:
                    k = 0
                else:
                    k = 1
                p.put(x, y, shade(palette, k))
    return tagged(paint, "smooth", palette, glint, foot)


def blushed(palette, blush):
    """A pear's skin: its own tone, its top row lit, a russet blush on the left of its lower half."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                if y >= p.h // 2 and x < max(1, p.w // 2) and (x + y) % 2 == 0:
                    c = shade(blush, 1)
                else:
                    c = shade(palette, 3 if y == 0 and p.h > 1 else 2)
                p.put(x, y, c)
    return tagged(paint, "blushed", palette, blush)


def solid(palette, k):
    """One flat tone (a cut face, a shadowed filling)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(palette, k))
    return tagged(paint, "solid", palette, k)


def sprite(rows, colours):
    """A cut-out picture from a pixel map: each symbol's colour, '.' left clear. Drawn at its own size (a texel to a
    model pixel), so the map is the leaf, acorn or emblem as it shows."""
    def paint(p):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != "." and x < p.w and y < p.h:
                    p.put(x, y, colours[ch])
    return tagged(paint, "sprite", rows, tuple(sorted((k, tuple(v)) for k, v in colours.items())))


# Leaves, their stalks at the foot of the map: a maple leaf (five points), a smaller maple, an oak leaf.
MAPLE_7 = ("...L...",
           ".L.L.M.",
           "LLLvMMM",
           ".LLvMM.",
           "LLLvMMM",
           "..LvM..",
           "...s...")
MAPLE_5 = ("..L..",
           "L.L.M",
           "LLvMM",
           ".LvM.",
           "..s..")
OAK_5 = (".L.",
         "LvM",
         ".vM",
         "LvM",
         ".s.")


def leaf_colours(palette):
    return {"L": shade(palette, 3), "M": shade(palette, 2), "v": shade(palette, 4), "s": shade(LEAF_STEM, 1)}


def leaf_painter(kind, palette):
    return sprite({"maple": MAPLE_7, "small": MAPLE_5, "oak": OAK_5}[kind], leaf_colours(palette))


def leaf_size(kind):
    rows = {"maple": MAPLE_7, "small": MAPLE_5, "oak": OAK_5}[kind]
    return len(rows[0]), len(rows)


def flat_leaf(sc, kind, palette, x, z, y, turn=0.0):
    """A leaf lying flat at height y (its picture's top to the north), centred on (x, z), turned `turn` degrees about
    its middle (22.5 or 45, either way)."""
    w, h = leaf_size(kind)
    uv = sc.piece(f"leaf_{kind}_{palette[2]}", w, h, leaf_painter(kind, palette))
    rot = fa.rotation((x, y, z), "y", turn) if turn else None
    return fa.plane_xz(x - w / 2, x + w / 2, z - h / 2, z + h / 2, y, uv, rot)


def upright_leaf(sc, kind, palette, x, y, z, turn=0.0, axis="z"):
    """A leaf standing up, facing north and south, its middle at (x, y, z), leaning `turn` degrees about `axis`."""
    w, h = leaf_size(kind)
    uv = sc.piece(f"leaf_{kind}_{palette[2]}", w, h, leaf_painter(kind, palette))
    rot = fa.rotation((x, y, z), axis, turn) if turn else None
    return fa.plane_xy(x - w / 2, x + w / 2, y - h / 2, y + h / 2, z, uv, rot)


# ---------------------------------------------------------------- shared parts

def wicker_walls(sc, name, x0, z0, x1, z1, y0, y1, t=1.0, floor=True):
    """A woven willow vessel's walls round x0..x1 by z0..z1 from y0 to y1, `t` thick, woven outside and in shadow inside,
    on a woven floor a pixel thick; the north and south walls run its full width."""
    weave, inside = wicker(), wicker(shadowed=True)
    el = [panel(sc, name, weave, (x0, y0, z0), (x1, y1, z0 + t), south=inside),
          panel(sc, name, weave, (x0, y0, z1 - t), (x1, y1, z1), north=inside),
          panel(sc, name, weave, (x0, y0, z0 + t), (x0 + t, y1, z1 - t), east=inside),
          panel(sc, name, weave, (x1 - t, y0, z0 + t), (x1, y1, z1 - t), west=inside)]
    if floor:
        el.append(panel(sc, name, weave, (x0 + t, y0, z0 + t), (x1 - t, y0 + 1.0, z1 - t), up=inside))
    return el


def braided_rim(sc, x0, z0, x1, z1, y, t=1.25, over=0.25):
    """A braided rim round the vessel's top edge from y to y + 1, standing `over` proud of its walls outside and in."""
    rim = braid()
    a0, b0, a1, b1 = x0 - over, z0 - over, x1 + over, z1 + over
    return [panel(sc, "rim", rim, (a0, y, b0), (a1, y + 1.0, b0 + t)),
            panel(sc, "rim", rim, (a0, y, b1 - t), (a1, y + 1.0, b1)),
            panel(sc, "rim", rim, (a0, y, b0 + t), (a0 + t, y + 1.0, b1 - t)),
            panel(sc, "rim", rim, (a1 - t, y, b0 + t), (a1, y + 1.0, b1 - t))]


def arch_handle(sc, xa, xb, z, y0, top, w=1.0, t=1.0, axis="x"):
    """A bent willow handle over a basket, rising at xa and xb from y0, its bar across at `top`, its shoulders cut on the
    slant; along x (or along z with axis "z", then xa and xb are z and `z` is x)."""
    rim = braid()
    mid = (xa + xb) / 2
    el = []

    def box(lo, hi, rot=None):
        if axis == "x":
            return panel(sc, "handle", rim, lo, hi, rot=rot)
        (x0, y0_, z0), (x1, y1, z1) = lo, hi
        if rot:
            rot = dict(rot, axis="x", angle=-rot["angle"], origin=[rot["origin"][2], rot["origin"][1], rot["origin"][0]])
        return panel(sc, "handle", rim, (z0, y0_, x0), (z1, y1, x1), rot=rot)
    shoulder = 2.0
    for x, sign in ((xa, 1), (xb, -1)):
        lo = x if sign == 1 else x - w
        el.append(box((lo, y0, z - t / 2), (lo + w, top - shoulder, z + t / 2)))
        # The shoulder: a short piece turned 45 degrees, from the upright's top in towards the bar.
        sx = lo + w / 2
        el.append(box((sx - w / 2, top - shoulder - 0.5, z - t / 2), (sx + w / 2, top + 0.2, z + t / 2),
                      rot=fa.rotation((sx, top - shoulder, z), "z", -45.0 * sign)))
    el.append(box((xa + shoulder, top - w, z - t / 2), (xb - shoulder, top, z + t / 2)))
    return el


def pumpkin(sc, name, cx, cz, d, y0, h, palette=PUMPKIN, stem=True, lean=None):
    """A pumpkin `d` across and `h` high standing at y0 about (cx, cz): ribbed round its sides, a narrower ring at its
    foot and shoulder so it reads round, its top ringed into its middle and its stalk at its top."""
    rib, top = ribbed(palette), crown(palette)
    el = crossed_y(sc, name, rib, cx, cz, d, y0 + 0.5, y0 + h - 0.5, up=top)
    el += crossed_y(sc, name, rib, cx, cz, max(1.0, d - 2.0), y0, y0 + 0.5)
    el += crossed_y(sc, name, rib, cx, cz, max(1.0, d - 2.0), y0 + h - 0.5, y0 + h, up=top)
    if stem:
        s = 1.0 if d < 7 else 1.5
        el.append(panel(sc, "stem", lengthwise(STEM), (cx - s / 2, y0 + h, cz - s / 2), (cx + s / 2, y0 + h + (1.0 if d < 5 else 1.5), cz + s / 2),
                        rot=lean))
    return el


def apple(sc, x, y, z, size=3.0, palette=APPLE, leafy=False):
    """An apple `size` across at (x, y, z) (its low corner): round enough at this size as one box, lit along its top with a
    glint, its stalk a pixel at its top, now and then a leaf."""
    el = [panel(sc, "apple", smooth(palette, glint=True), (x, y, z), (x + size, y + size, z + size))]
    m = size / 2
    el.append(panel(sc, "stalk", lengthwise(STEM), (x + m - 0.5, y + size, z + m - 0.5), (x + m + 0.5, y + size + 1.0, z + m + 0.5)))
    if leafy:
        el.append(panel(sc, "apple_leaf", flat(LEAF), (x + m + 0.5, y + size, z + m - 0.5), (x + m + 2.0, y + size + 0.5, z + m + 0.5)))
    return el


def pear(sc, x, y, z, lying=False):
    """A pear with its foot at (x, y, z) (its low corner): a round body three pixels across, a narrower neck over it and
    its stalk; lying on its side with its neck to the east (`lying`)."""
    skin, top = blushed(PEAR, PEAR_BLUSH), smooth(PEAR)
    if not lying:
        return [panel(sc, "pear", skin, (x, y, z), (x + 3.0, y + 2.5, z + 3.0)),
                panel(sc, "pear_neck", top, (x + 0.5, y + 2.5, z + 0.5), (x + 2.5, y + 4.5, z + 2.5)),
                panel(sc, "stalk", lengthwise(STEM), (x + 1.0, y + 4.5, z + 1.0), (x + 2.0, y + 5.5, z + 2.0))]
    return [panel(sc, "pear", skin, (x, y, z), (x + 2.5, y + 3.0, z + 3.0)),
            panel(sc, "pear_neck", top, (x + 2.5, y + 0.5, z + 0.5), (x + 4.5, y + 2.5, z + 2.5)),
            panel(sc, "stalk", lengthwise(STEM), (x + 4.5, y + 1.0, z + 1.0), (x + 5.5, y + 2.0, z + 2.0))]


# ---------------------------------------------------------------- D. the harvest market and garden

def pumpkin_and_gourd_stack():
    """The Pumpkin and Gourd Stack, facing north: a market display of the farm's own pumpkins and gourds heaped as a
    grocer stacks them. A big orange pumpkin at the back with a white pumpkin sat on it, an acorn squash and a warty gourd
    in front of them, a butternut squash lying at the front right, two little striped gourds and an apple at their feet,
    and fallen maple leaves round them."""
    sc = Sculpt("pumpkin_and_gourd_stack", 4101, 128)
    el = pumpkin(sc, "pumpkin", 6.5, 10.0, 9.0, 0.0, 7.0)
    el += pumpkin(sc, "white_pumpkin", 6.5, 10.0, 6.0, 7.0, 4.0, WHITE_PUMPKIN)
    el += pumpkin(sc, "small_pumpkin", 12.0, 12.0, 5.0, 0.0, 4.0)
    el += crossed_y(sc, "acorn_squash", acorn_squash(), 10.5, 6.5, 5.0, 0.0, 4.5)
    el.append(panel(sc, "squash_stem", lengthwise(STEM), (10.0, 4.5, 6.0), (11.0, 5.5, 7.0)))
    # The warty gourd: a round body, its neck curving up out of it.
    el += crossed_y(sc, "warty_gourd", warty(), 4.0, 4.0, 5.0, 0.0, 4.0)
    el.append(panel(sc, "warty_neck", warty(), (3.0, 4.0, 3.0), (5.0, 6.5, 5.0), rot=fa.rotation((4.0, 4.0, 4.0), "z", 22.5)))
    # The butternut lying at the front right, its bulb at the east end.
    el.append(panel(sc, "butternut", smooth(BUTTERNUT), (8.0, 0.0, 1.5), (11.5, 2.0, 3.5)))
    el.append(panel(sc, "butternut", smooth(BUTTERNUT), (11.5, 0.0, 1.0), (14.5, 3.0, 4.0)))
    el.append(panel(sc, "squash_stem", lengthwise(STEM), (7.0, 0.5, 2.0), (8.0, 1.5, 3.0)))
    # Two little striped gourds and an apple at their feet.
    el.append(panel(sc, "gourd", gourd(), (1.5, 0.0, 7.5), (3.5, 2.0, 9.5)))
    el.append(panel(sc, "gourd", gourd(), (6.5, 0.0, 2.5), (8.0, 1.5, 4.0)))
    el += apple(sc, 1.5, 0.0, 11.5, 2.5)
    # Fallen leaves round them.
    el.append(flat_leaf(sc, "small", RED_LEAF, 3.5, 12.5, 0.1, 22.5))
    el.append(flat_leaf(sc, "oak", GOLD_LEAF, 14.5, 8.0, 0.1))
    el.append(flat_leaf(sc, "small", ORANGE_LEAF, 6.0, 2.75, 0.1))
    sc.models["whole"] = el
    return sc


def wicker_apple_basket():
    """The Wicker Apple Basket, facing north: a deep willow basket 12 by 10 pixels with a braided rim, heaped with red
    apples above its rim, its bent willow handle arched over them from side to side."""
    sc = Sculpt("wicker_apple_basket", 4102, 128)
    x0, z0, x1, z1, top = 2.5, 3.5, 13.5, 12.5, 6.0
    el = wicker_walls(sc, "basket", x0, z0, x1, z1, 0.0, top)
    el += braided_rim(sc, x0, z0, x1, z1, top)
    # What the apples lie on, in shadow between them.
    el.append(panel(sc, "fill", solid(SHADOW, 1), (x0 + 1.0, 1.0, z0 + 1.0), (x1 - 1.0, 4.5, z1 - 1.0)))
    # Two rows of three, a little apart and not quite level, mostly red with a green and a gold among them; two more
    # heaped on them.
    for ax, ay, az, palette, leafy in ((3.75, 4.5, 4.75, APPLE, False), (6.75, 5.0, 4.5, GREEN_APPLE, True),
                                       (9.75, 4.5, 4.75, APPLE, False), (3.75, 5.0, 8.25, APPLE, False),
                                       (6.75, 4.5, 8.5, GOLD_APPLE, False), (9.75, 5.0, 8.25, APPLE, True)):
        el += apple(sc, ax, ay, az, 2.5, palette, leafy)
    for ax, az, palette in ((5.25, 6.5, APPLE), (8.25, 7.0, APPLE)):
        el += apple(sc, ax, 7.0, az, 2.5, palette)
    el += arch_handle(sc, x0 + 0.25, x1 - 0.25, 8.0, top, 13.0)
    sc.models["whole"] = el
    return sc


def pear_basket():
    """The Pear Basket, facing north: a shallow willow trug, long and low, its bentwood handle along its length, full of
    green-gold pears blushed russet, some standing, some lying; one pear rolled out at its front."""
    sc = Sculpt("pear_basket", 4103, 128)
    x0, z0, x1, z1, top = 1.5, 4.0, 14.5, 12.0, 3.5
    el = wicker_walls(sc, "trug", x0, z0, x1, z1, 0.0, top)
    el += braided_rim(sc, x0, z0, x1, z1, top, t=1.0)
    el.append(panel(sc, "fill", solid(SHADOW, 1), (x0 + 1.0, 1.0, z0 + 1.0), (x1 - 1.0, 2.0, z1 - 1.0)))
    # Two rows: a pear standing and one lying beside it in each, and one more rolled out on the ground at the front.
    el += pear(sc, 2.75, 2.0, 5.25) + pear(sc, 6.25, 2.0, 5.25, lying=True)
    el += pear(sc, 2.75, 2.0, 8.25, lying=True) + pear(sc, 9.0, 2.0, 8.25)
    el += pear(sc, 9.5, 0.0, 0.5, lying=True)
    # The handle along its length, from end to end over the pears.
    el += arch_handle(sc, x0 + 0.5, x1 - 0.5, 8.0, top, 10.0)
    sc.models["whole"] = el
    return sc


def root_vegetable_hamper():
    """The Root Vegetable Hamper, facing north: a lidded willow hamper, its lid swung up and leaning back on its hinges,
    two leather straps with brass buckles down its front, heaped with the farm's roots above its rim: potatoes and an
    onion at the front, a turnip and beetroots on their red stalks behind, and two carrots laid across the top."""
    sc = Sculpt("root_vegetable_hamper", 4104, 128)
    x0, z0, x1, z1, top = 2.0, 3.5, 14.0, 11.5, 7.0
    el = wicker_walls(sc, "hamper", x0, z0, x1, z1, 0.0, top)
    el += braided_rim(sc, x0, z0, x1, z1, top, t=1.0)
    strap, buckle = flat(LEATHER), flat(BRASS)
    for sx in (4.5, 10.5):
        el.append(panel(sc, "strap", strap, (sx, 1.0, z0 - 0.5), (sx + 1.0, top, z0)))
        el.append(panel(sc, "buckle", buckle, (sx - 0.25, 3.0, z0 - 0.75), (sx + 1.25, 4.5, z0 - 0.25)))
    # The lid, as long as the hamper is deep, hinged along its back and leaning back 22.5 degrees.
    el.append(panel(sc, "lid", wicker(), (x0 - 0.25, top + 1.0, z1 - 1.0), (x1 + 0.25, top + 1.0 + (z1 - z0), z1),
                    north=wicker(shadowed=True), rot=fa.rotation((8.0, top + 1.0, z1), "x", 22.5)))
    el.append(panel(sc, "fill", solid(SHADOW, 1), (x0 + 1.0, 1.0, z0 + 1.0), (x1 - 1.0, 7.0, z1 - 1.0)))
    potato = smooth(POTATO)
    el += [panel(sc, "potato", potato, (3.25, 7.0, 4.25), (6.25, 9.0, 6.75)), panel(sc, "potato", potato, (6.75, 7.0, 4.5), (9.25, 8.5, 6.5))]
    el += crossed_y(sc, "onion", smooth(ONION), 11.5, 6.0, 3.0, 7.0, 9.5)
    el.append(panel(sc, "onion_top", lengthwise(ONION), (11.0, 9.5, 5.5), (12.0, 10.5, 6.5)))
    el += crossed_y(sc, "turnip", turnip_skin(), 4.75, 9.0, 3.0, 7.0, 10.0, up=flat(hfd.TURNIP_PURPLE))
    el.append(panel(sc, "turnip_leaf", hfd.turnip_leaf(), (4.25, 10.0, 8.75), (5.25, 13.0, 9.25)))
    beet = smooth(BEET)
    for bx, bz in ((7.5, 8.0), (10.0, 8.5)):
        el.append(panel(sc, "beet", beet, (bx, 7.0, bz), (bx + 2.5, 9.5, bz + 2.5)))
        el.append(panel(sc, "beet_stalk", lengthwise(BEET_STALK), (bx + 1.0, 9.5, bz + 1.0), (bx + 1.5, 12.0, bz + 1.5)))
        el.append(panel(sc, "beet_leaf", flat(LEAF), (bx + 0.25, 12.0, bz + 1.0), (bx + 2.25, 14.0, bz + 1.5)))
    # Two carrots laid across the heap, turned on the slant, their fronds over the side.
    skin = carrot_skin()
    for cx, cy, cz, turn in ((4.0, 9.5, 6.0, 22.5), (6.5, 9.0, 4.25, -22.5)):
        rot = fa.rotation((cx + 3.75, cy, cz + 0.75), "y", turn)
        el.append(panel(sc, "carrot", skin, (cx, cy, cz), (cx + 5.0, cy + 1.5, cz + 1.5), rot=rot))
        el.append(panel(sc, "frond", frond(), (cx + 5.0, cy + 0.25, cz + 0.25), (cx + 7.5, cy + 1.25, cz + 1.25), rot=rot))
    sc.models["whole"] = el
    return sc


# ---------------------------------------------------------------- E. the feast table: the Autumn Leaf Table Runner

# Where each piece of the runner is on its hessian texture (block/feast_table_leaf_runner, u0, v0, u1, v1): the runner
# along a length, its edge, the end hanging over the table's end and a patch of binding; and on its leaves' cut-out
# texture (block/feast_table_leaf_runner_leaves): each leaf, and an acorn's cap and nut.
RUNNER_UV = {"top": [0, 0, 16, 6], "edge": [0, 6, 16, 6.25], "end": [0, 7, 6, 12], "binding": [8, 7, 9, 8]}
LEAF_UV = {"red": [0, 0, 7, 7], "gold": [8, 0, 15, 7], "orange": [0, 8, 5, 13], "oak": [6, 8, 9, 13], "cap": [11, 8, 13, 10],
           "nut": [11, 11, 13, 13]}
# The leaves on a length: (kind, middle x, middle z, height over the table, turn about the upright): each length the same,
# so a table of any length reads as one runner strewn with leaves.
RUNNER_LEAVES = (("red", 4.5, 7.5, 0.35, 22.5), ("gold", 11.5, 8.5, 0.4, -22.5), ("orange", 8.0, 6.0, 0.45, 45.0),
                 ("oak", 14.0, 6.5, 0.5, 0.0))


def leaf_runner(west, east):
    """The Autumn Leaf Table Runner on a length of Feast Table (feast_data.table_model, runner `leaves`): a hessian runner
    `width` wide laid along the table in place of its orange runner, its edges fringed, strewn with fallen leaves (a red
    and a gold maple, a small orange maple and an oak leaf, lying flat at different heights so none shares a plane)
    and a pair of acorns; at the table's `west` or `east` end (where its legs are) it hangs `drape` pixels down over
    the end, fringed at its foot."""
    from decor_data import box
    import party_table as pt
    w, drape = td.RUNNER["width"], td.RUNNER["drape"]
    table = pt.TABLE["table_top"]
    z0, z1, top = 8.0 - w / 2, 8.0 + w / 2, table + 0.25
    out = [box((0, table, z0), (16, top, z1), "#hessian", faces=["up", "north", "south"],
               uvs={"up": RUNNER_UV["top"], "north": RUNNER_UV["edge"], "south": RUNNER_UV["edge"]})]
    for end, (x0, x1), face in ((west, (-0.25, 0.0), "west"), (east, (16.0, 16.25), "east")):
        if end:
            out.append(box((x0, top - drape, z0), (x1, top, z1), "#hessian",
                           uvs={side: (RUNNER_UV["end"] if side == face else RUNNER_UV["binding"])
                                for side in ("north", "south", "east", "west", "up", "down")}))
    for kind, x, z, lift, turn in RUNNER_LEAVES:
        u0, v0, u1, v1 = LEAF_UV[kind]
        y = top + lift - 0.25
        leaf = fa.plane_xz(x - (u1 - u0) / 2, x + (u1 - u0) / 2, z - (v1 - v0) / 2, z + (v1 - v0) / 2, y, (u0, v0, u1, v1),
                           fa.rotation((x, y, z), "y", turn) if turn else None)
        for spec in leaf["faces"].values():
            spec["texture"] = "#leaves"
        out.append(leaf)
    # Two acorns lying on their sides near the runner's middle, each a nut and its cap at one end, pointing apart.
    every = ("north", "south", "east", "west", "up", "down")
    for nut, cap in ((((5.5, top, 9.75), (7.0, top + 1.25, 11.0)), ((7.0, top, 9.625), (7.75, top + 1.5, 11.125))),
                     (((9.75, top, 9.75), (11.25, top + 1.25, 11.0)), ((9.0, top, 9.625), (9.75, top + 1.5, 11.125)))):
        out.append(box(nut[0], nut[1], "#leaves", uvs={side: LEAF_UV["nut"] for side in every}))
        out.append(box(cap[0], cap[1], "#leaves", uvs={side: LEAF_UV["cap"] for side in every}))
    return out


def runner_texture():
    """The runner's hessian, laid out for its pieces (RUNNER_UV): a plain weave of two tones in steps of two texels
    along each row, each row's steps one along from the last's, between a fringed edge row each side (light and dark by
    turns); its edge; the end hanging over the table, its foot fringed; a patch of binding."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def put(x, y, k):
        img.putpixel((x, y), (*shade(HESSIAN, k), 255))

    for x in range(16):
        for y in range(6):
            put(x, y, (3 if x % 2 == 0 else 1) if y in (0, 5) else (2 if (x // 2 + y) % 2 == 0 else 3))
        put(x, 6, 1)
    for x in range(6):
        for y in range(7, 12):
            put(x, y, (3 if x % 2 == 0 else 1) if y == 11 else 1 if x in (0, 5) else (2 if (x // 2 + y) % 2 == 0 else 3))
    put(8, 7, 1)
    return img


def runner_leaves_texture():
    """The leaves strewn on the runner, cut out (LEAF_UV): a red and a gold maple leaf, a small orange maple, an oak leaf,
    and an acorn's cap and nut (opaque, for the acorns' boxes)."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for kind, rows, palette in (("red", MAPLE_7, RED_LEAF), ("gold", MAPLE_7, GOLD_LEAF), ("orange", MAPLE_5, ORANGE_LEAF),
                                ("oak", OAK_5, OAK_LEAF)):
        u0, v0 = LEAF_UV[kind][:2]
        colours = leaf_colours(palette)
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((u0 + x, v0 + y), (*colours[ch], 255))
    for kind, palette in (("cap", ACORN_CAP), ("nut", ACORN_NUT)):
        u0, v0, u1, v1 = LEAF_UV[kind]
        for y in range(v0, v1):
            for x in range(u0, u1):
                img.putpixel((x, y), (*shade(palette, 2 if y == v0 else 1), 255))
    return img


# ---------------------------------------------------------------- E. the feast table: its pieces

def banded(palette, band):
    """Glazed china's side: its own tone, lit along its top row, a painted band of `band` along its middle row(s)."""
    def paint(p):
        mid = p.h // 2
        for y in range(p.h):
            for x in range(p.w):
                if p.h >= 3 and y == mid:
                    c = shade(band, 1)
                else:
                    c = shade(palette, 3 if y == 0 else 1 if y == p.h - 1 and p.h > 2 else 2)
                p.put(x, y, c)
    return tagged(paint, "banded", palette, band)


def check(a, b, size=2):
    """A woven check of two cloths, squares `size` texels across, the crossings a tone between (gingham, a napkin)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                cx, cy = (x // size) % 2, (y // size) % 2
                c = shade(a, 2) if cx and cy else shade(b, 3) if not cx and not cy else shade(a, 1) if cx else shade(b, 2)
                p.put(x, y, c)
    return tagged(paint, "check", a, b, size)


def tines():
    """A fork's head seen from above: its prongs, a texel of silver and a texel of shadow by turns across it, joined at
    the neck (the piece's last row)."""
    def paint(p):
        along_x = p.w >= p.h
        span, across = (p.w, p.h) if along_x else (p.h, p.w)
        for y in range(p.h):
            for x in range(p.w):
                s_, a = (x, y) if along_x else (y, x)
                gap = a % 2 == 1 and s_ < span - 1
                p.put(x, y, shade(SILVER, 0 if gap else 3))
    return tagged(paint, "tines")


def crust_rim():
    """A pie's crimped crust, round its edge: a crimp lit and a crimp in shadow by turns, its top row golden."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(CRUST, 4 if y == 0 else (3, 1)[x % 2]))
    return tagged(paint, "crust_rim")


def lattice():
    """An apple pie's lattice: crust strips two texels wide every four, over and under, the filling between them."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                over_x, over_y = x % 4 < 2, y % 4 < 2
                if over_x and over_y:
                    c = shade(CRUST, 4 if (x // 4 + y // 4) % 2 else 2)
                elif over_x or over_y:
                    c = shade(CRUST, 3)
                else:
                    c = shade(APPLE_FILLING, 1)
                p.put(x, y, c)
    return tagged(paint, "lattice")


def crust_top(palette):
    """A baked roll or loaf's crust: its own tone, lit along its top, a scored line a tone darker across its middle."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                k = 3 if y == 0 and p.h > 1 else 1 if (p.h > 3 and y == p.h // 2 and 0 < x < p.w - 1) else 2
                p.put(x, y, shade(palette, k))
    return tagged(paint, "crust_top", palette)


def disc(sc, name, painter, cx, cz, d, y0, y1, up=None, down=None):
    """A round flat thing (a plate, a charger, a tier) `d` across: two boxes crossed, its top `up`."""
    kw = {}
    if up:
        kw["up"] = up
    if down:
        kw["down"] = down
    return crossed_y(sc, name, painter, cx, cz, d, y0, y1, **kw)


def harvest_place_setting():
    """The Harvest Place Setting, laid for a diner to the north: a woven rattan charger under a cream dinner plate banded
    in amber, an orange napkin folded on it through a brass ring with a maple leaf tucked in; the fork to the diner's
    left, the knife to the right; a brass goblet of cider at the top right and a little pumpkin for a place card at the
    top left."""
    sc = Sculpt("harvest_place_setting", 4201, 128)
    cx, cz = 8.0, 7.5
    el = disc(sc, "charger", braid(), cx, cz, 10.0, 0.0, 0.5)
    el += disc(sc, "plate", banded(CHINA, CHINA_BAND), cx, cz, 8.0, 0.5, 1.5, up=solid(CHINA, 3))
    el.append(panel(sc, "napkin", check(NAPKIN, LINEN), (5.5, 1.5, 5.5), (10.5, 2.0, 9.5)))
    el.append(panel(sc, "ring", flat(BRASS), (7.25, 1.25, 5.25), (8.75, 2.25, 9.75)))
    el.append(flat_leaf(sc, "small", RED_LEAF, 9.5, 7.5, 2.35, 22.5))
    # The diner sits to the north, facing south: their left hand is to the east.
    el += [panel(sc, "fork", lengthwise(SILVER), (13.5, 0.0, 7.0), (14.5, 0.5, 12.0)),
           panel(sc, "fork", tines(), (13.25, 0.0, 3.0), (14.75, 0.5, 7.0))]
    el += [panel(sc, "knife_handle", lengthwise(WOOD), (1.5, 0.0, 8.0), (2.5, 0.75, 12.0)),
           panel(sc, "knife", flat(SILVER), (1.5, 0.0, 3.0), (2.5, 0.5, 8.0))]
    gx, gz = 3.0, 13.0
    el += disc(sc, "goblet", flat(BRASS), gx, gz, 3.0, 0.0, 0.5)
    el.append(panel(sc, "goblet", lengthwise(BRASS), (gx - 0.5, 0.5, gz - 0.5), (gx + 0.5, 2.5, gz + 0.5)))
    el += [panel(sc, "goblet", flat(BRASS), (gx - 1.5, 2.5, gz - 1.5), (gx + 1.5, 5.5, gz - 1.0)),
           panel(sc, "goblet", flat(BRASS), (gx - 1.5, 2.5, gz + 1.0), (gx + 1.5, 5.5, gz + 1.5)),
           panel(sc, "goblet", flat(BRASS), (gx - 1.5, 2.5, gz - 1.0), (gx - 1.0, 5.5, gz + 1.0)),
           panel(sc, "goblet", flat(BRASS), (gx + 1.0, 2.5, gz - 1.0), (gx + 1.5, 5.5, gz + 1.0)),
           panel(sc, "cider", solid(CIDER, 2), (gx - 1.0, 2.5, gz - 1.0), (gx + 1.0, 4.75, gz + 1.0))]
    el += pumpkin(sc, "place_card", 12.5, 13.0, 3.0, 0.0, 2.5)
    sc.models["whole"] = el
    return sc


def napkin_and_cutlery_caddy():
    """The Napkin and Cutlery Caddy, facing north: a planked wooden caddy of two compartments either side of a tall
    divider with a hand-hold cut through its top; forks and knives standing in the left one (the diner's left, to the
    west as you face it from the north... to the east), rolled orange napkins in their brass rings in the other."""
    sc = Sculpt("napkin_and_cutlery_caddy", 4202, 128)
    boards = planks(WOOD, board=2, nails=False)
    x0, z0, x1, z1, top = 3.5, 5.5, 12.5, 10.5, 4.0
    el = ppd.walls(sc, "caddy", boards, x0, z0, x1, z1, 0.75, 0.0, top)
    el.append(panel(sc, "caddy_floor", boards, (x0 + 0.75, 0.0, z0 + 0.75), (x1 - 0.75, 0.75, z1 - 0.75)))
    # The divider, and its hand-hold: two posts and a bar over a gap.
    el.append(panel(sc, "divider", boards, (7.5, 0.75, z0 + 0.75), (8.5, 6.5, z1 - 0.75)))
    el += [panel(sc, "divider", boards, (7.5, 6.5, z0 + 0.75), (8.5, 8.0, 6.75)),
           panel(sc, "divider", boards, (7.5, 6.5, 9.25), (8.5, 8.0, z1 - 0.75)),
           panel(sc, "divider", boards, (7.5, 8.0, z0 + 0.75), (8.5, 9.0, z1 - 0.75))]
    el.append(panel(sc, "caddy_fill", solid(SHADOW, 1), (x0 + 0.75, 0.75, z0 + 0.75), (7.5, 2.5, z1 - 0.75)))
    el.append(panel(sc, "caddy_fill", solid(SHADOW, 1), (8.5, 0.75, z0 + 0.75), (x1 - 0.75, 2.5, z1 - 0.75)))
    # Cutlery in the west compartment: fork and spoon handles of silver, knives with wooden handles, leaning a little.
    for hx, hz, handle, lean in ((4.75, 6.75, SILVER, 0.0), (5.75, 8.25, SILVER, 22.5), (6.5, 6.75, WOOD, -22.5),
                                 (4.75, 8.75, WOOD, 0.0)):
        rot = fa.rotation((hx + 0.5, 2.5, hz + 0.5), "z", lean) if lean else None
        el.append(panel(sc, "cutlery", lengthwise(handle), (hx, 2.5, hz), (hx + 1.0, 7.5, hz + 1.0), rot=rot))
    # Rolled napkins in the east compartment, each round two boxes crossed, in a brass ring.
    for nx, nz, top_ in ((9.75, 7.0, 7.0), (11.25, 7.0, 6.5), (10.5, 9.0, 7.5)):
        el += crossed_y(sc, "napkin_roll", check(NAPKIN, LINEN, 1), nx, nz, 1.5, 2.5, top_, up=solid(LINEN, 2))
        el.append(panel(sc, "ring", flat(BRASS), (nx - 0.875, 4.5, nz - 0.875), (nx + 0.875, 5.25, nz + 0.875)))
    sc.models["whole"] = el
    return sc


def bread_basket():
    """The Bread Basket, facing north: a low willow basket lined with a red gingham cloth whose corners hang over its rim
    front and back and at its ends, heaped with golden dinner rolls and a long loaf laid across them on the slant."""
    sc = Sculpt("bread_basket", 4203, 128)
    x0, z0, x1, z1, top = 2.5, 4.0, 13.5, 12.0, 3.0
    el = wicker_walls(sc, "basket", x0, z0, x1, z1, 0.0, top)
    el += braided_rim(sc, x0, z0, x1, z1, top, t=1.0)
    cloth = check(GINGHAM, LINEN)
    el.append(panel(sc, "cloth", cloth, (x0 + 1.0, 2.5, z0 + 1.0), (x1 - 1.0, 3.0, z1 - 1.0)))
    # The cloth's corners over the rim: a flap down the outside at the middle of each side.
    for lo, hi in (((6.0, 1.5, z0 - 0.75), (10.0, 4.25, z0 - 0.25)), ((6.0, 1.5, z1 + 0.25), (10.0, 4.25, z1 + 0.75)),
                   ((x0 - 0.75, 1.5, 6.5), (x0 - 0.25, 4.25, 9.5)), ((x1 + 0.25, 1.5, 6.5), (x1 + 0.75, 4.25, 9.5))):
        el.append(panel(sc, "flap", cloth, lo, hi))
    roll = crust_top(ROLL)
    for rx, rz, ry in ((4.75, 6.25, 3.0), (8.0, 6.0, 3.0), (11.25, 6.5, 3.0), (5.0, 9.75, 3.0), (11.0, 9.75, 3.0)):
        el += crossed_y(sc, "roll", roll, rx, rz, 3.0, ry, ry + 2.0, up=smooth(ROLL, foot=False))
    # The loaf, laid across the rolls on the slant.
    el.append(panel(sc, "loaf", crust_top(CRUST), (4.5, 4.5, 7.5), (12.5, 6.5, 9.5), rot=fa.rotation((8.5, 4.5, 8.5), "y", -22.5)))
    sc.models["whole"] = el
    return sc


def boat_side(palette, band):
    """A gravy boat's side under its rim: the painted band of `band` along its top row, its own tone below, its foot row
    a tone darker (three rows or more)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                if y == 0:
                    c = shade(band, 1)
                else:
                    c = shade(palette, 1 if y == p.h - 1 and p.h > 2 else 2)
                p.put(x, y, c)
    return tagged(paint, "boat_side", palette, band)


# The gravy boat's hull in plan, bow to stern (north to south): (x0, z0, x1, z1), so it is long and low, its bow and
# stern narrower than its middle.
BOAT_PLAN = ((6.5, 4.0, 9.5, 5.5), (5.5, 5.5, 10.5, 10.0), (6.0, 10.0, 10.0, 11.5))
# Its rim, a half pixel thick round that plan, open at the bow for the spout; its stern raised toward the handle.
BOAT_RIM = (((6.5, 4.0), (7.0, 5.5)), ((9.0, 4.0), (9.5, 5.5)),
            ((5.5, 5.5), (7.0, 6.0)), ((9.0, 5.5), (10.5, 6.0)),
            ((5.5, 6.0), (6.0, 10.0)), ((10.0, 6.0), (10.5, 10.0)),
            ((5.5, 9.5), (6.5, 10.0)), ((9.5, 9.5), (10.5, 10.0)),
            ((6.0, 10.0), (6.5, 11.0)), ((9.5, 10.0), (10.0, 11.0)))


def gravy_boat():
    """The Gravy Boat, its spout to the north: a long, low cream china boat banded in amber on an oval saucer, its bow
    and stern narrower than its middle, brimming with brown gravy just under its rim; at the bow its lip is drawn out
    into a pointed spout tilted up, with a drip run down the bow and a little spill on the saucer, and at the stern its
    rim rises into a looped handle curling up and back."""
    sc = Sculpt("gravy_boat", 4204, 128)
    china, top, gravy = solid(CHINA, 2), solid(CHINA, 3), solid(GRAVY, 2)
    side, under = boat_side(CHINA, CHINA_BAND), solid(CHINA, 1)
    saucer = banded(CHINA, CHINA_BAND)
    # The saucer: an oval three boxes crossed, long under the boat.
    el = [panel(sc, "saucer", saucer, (5.5, 0.0, 3.0), (10.5, 0.5, 13.0), up=top),
          panel(sc, "saucer", saucer, (4.5, 0.0, 4.5), (11.5, 0.5, 11.5), up=top),
          panel(sc, "saucer", saucer, (4.0, 0.0, 6.0), (12.0, 0.5, 10.0), up=top)]
    # Its foot, and the boat's belly drawn in under the hull, so it sits low and rounded.
    el.append(panel(sc, "foot", china, (6.75, 0.5, 6.0), (9.25, 1.0, 9.5)))
    el.append(panel(sc, "belly", under, (6.25, 1.0, 5.0), (9.75, 1.75, 10.75)))
    # The hull, its gravy level a half pixel under the rim.
    for x0, z0, x1, z1 in BOAT_PLAN:
        el.append(panel(sc, "hull", side, (x0, 1.75, z0), (x1, 3.5, z1), up=gravy))
    for (x0, z0), (x1, z1) in BOAT_RIM:
        el.append(panel(sc, "rim", top, (x0, 3.5, z0), (x1, 4.0, z1)))
    el.append(panel(sc, "rim", top, (6.0, 3.5, 11.0), (10.0, 4.5, 11.5)))
    # The spout: a channel drawn out of the bow, tilted up to a point, gravy along its floor.
    tilt = fa.rotation((8.0, 3.5, 4.5), "x", 22.5)
    el += [panel(sc, "spout", china, (7.0, 2.75, 2.75), (9.0, 3.5, 4.5), up=gravy, rot=tilt),
           panel(sc, "spout_lip", top, (7.0, 3.5, 2.75), (7.5, 4.0, 4.5), rot=tilt),
           panel(sc, "spout_lip", top, (8.5, 3.5, 2.75), (9.0, 4.0, 4.5), rot=tilt),
           panel(sc, "spout_tip", top, (7.5, 3.0, 2.25), (8.5, 3.75, 2.75), rot=tilt)]
    # The drip, run down the bow under the spout to its keel, and a little spill on the saucer below.
    el.append(panel(sc, "drip", solid(GRAVY, 1), (7.5, 1.75, 3.75), (8.5, 3.0, 4.0)))
    el.append(panel(sc, "pool", solid(GRAVY, 1), (7.25, 0.5, 3.5), (8.75, 0.625, 4.5)))
    # The handle: out of the stern's raised rim, curling up and back, down and in under the hull.
    curl = fa.rotation((8.0, 3.75, 11.5), "x", -22.5)
    el += [panel(sc, "handle", top, (7.5, 3.75, 11.5), (8.5, 4.5, 13.0), rot=curl),
           panel(sc, "handle", top, (7.5, 2.0, 12.5), (8.5, 4.5, 13.25)),
           panel(sc, "handle", top, (7.5, 1.75, 11.5), (8.5, 2.5, 12.75))]
    sc.models["whole"] = el
    return sc


def covered_butter_dish():
    """The Covered Butter Dish, facing north: a cream china dish with its high cover on, banded in amber, a knob on
    top; a butter knife laid in front of it."""
    sc = Sculpt("covered_butter_dish", 4205, 128)
    china, band, top = solid(CHINA, 2), banded(CHINA, CHINA_BAND), solid(CHINA, 3)
    el = [panel(sc, "dish", band, (3.5, 0.0, 5.0), (12.5, 0.75, 11.0), up=top),
          panel(sc, "dish", band, (4.0, 0.0, 4.5), (12.0, 0.75, 11.5), up=top)]
    el.append(panel(sc, "cover", band, (4.5, 0.75, 6.0), (11.5, 4.0, 10.0), up=top))
    el.append(panel(sc, "cover_top", china, (5.0, 4.0, 6.5), (11.0, 4.5, 9.5), up=top))
    el += disc(sc, "knob", flat(CHINA), 8.0, 8.0, 2.0, 4.5, 5.5, up=top)
    el += [panel(sc, "knife_handle", lengthwise(WOOD), (9.0, 0.0, 2.75), (12.0, 0.75, 3.75)),
           panel(sc, "knife", flat(SILVER), (4.0, 0.0, 2.75), (9.0, 0.5, 3.75))]
    sc.models["whole"] = el
    return sc


def pie(sc, name, cx, cz, d, y0, filling):
    """A pie in its dish `d` across at y0: the dish, the crimped crust round its edge and its top (`filling`, a painter)."""
    el = disc(sc, f"{name}_dish", flat(IRON), cx, cz, d - 1.0, y0, y0 + 0.5)
    el += disc(sc, f"{name}_crust", crust_rim(), cx, cz, d, y0 + 0.5, y0 + 1.5, up=filling)
    return el


def pie_stand():
    """The Pie Stand, facing north: a turned walnut stand of two tiers on a round foot, its post through their middles to
    a knob on top; a pumpkin pie with dollops of cream on the lower tier, a latticed apple pie on the upper."""
    sc = Sculpt("pie_stand", 4206, 128)
    wood = lengthwise(WALNUT)
    face = solid(WALNUT, 3)
    el = disc(sc, "foot", wood, 8.0, 8.0, 6.0, 0.0, 1.0, up=face)
    el.append(panel(sc, "post", wood, (7.25, 1.0, 7.25), (8.75, 13.0, 8.75)))
    el += disc(sc, "tier", wood, 8.0, 8.0, 13.0, 4.0, 4.75, up=face)
    el += pie(sc, "pumpkin_pie", 8.0, 8.0, 11.0, 4.75, solid(PUMPKIN_FILLING, 2))
    for dx, dz in ((4.5, 5.0), (10.5, 9.5), (5.5, 10.5), (10.0, 4.5)):
        el.append(panel(sc, "cream", smooth(hfd.CREAM), (dx, 6.25, dz), (dx + 1.5, 7.0, dz + 1.5)))
    el += disc(sc, "tier", wood, 8.0, 8.0, 10.0, 9.75, 10.5, up=face)
    el += pie(sc, "apple_pie", 8.0, 8.0, 9.0, 10.5, lattice())
    el += disc(sc, "knob", wood, 8.0, 8.0, 2.5, 13.0, 14.0, up=face)
    sc.models["whole"] = el
    return sc


# ---------------------------------------------------------------- F. the warm town and its interiors

def leafy():
    """A garland's body: autumn leaves packed along a twine, red, orange and gold by turns in blocks of two texels, each
    block lit along its top row, a darker vein texel at its foot."""
    colours = (RED_LEAF, ORANGE_LEAF, GOLD_LEAF, ORANGE_LEAF)

    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                palette = colours[(x // 2 + y // 2) % 4]
                p.put(x, y, shade(palette, 4 if y % 2 == 0 and x % 2 == 0 else 3 if y % 2 == 0 else 2))
    return tagged(paint, "leafy")


def lit(elements, light=15):
    for e in elements:
        e["light_emission"] = light
    return elements


# The garland's swag in a block, in its model's frame (facing north, the wall to the south): its ends pinned at
# (0, PIN_Y) and (16, PIN_Y), sagging between them in three lengths, its body CORE across and BACK from the wall.
PIN_Y, CORE, BACK = 12.0, 2.0, 15.5


def garland_swag(sc):
    """The swag: a length falling from each pin at 22.5 degrees and a level length between them, leaves laid over its
    front (at two depths, so overlapping leaves never share a plane) and two acorns hanging from its lowest point."""
    body = leafy()
    z0, z1 = BACK - CORE, BACK
    drop = 5.5 * math.sin(math.radians(22.5))
    low = PIN_Y - drop
    el = [panel(sc, "garland", body, (0.0, PIN_Y - CORE / 2, z0), (5.75, PIN_Y + CORE / 2, z1), rot=fa.rotation((0.0, PIN_Y, 15.0), "z", -22.5)),
          panel(sc, "garland", body, (10.25, PIN_Y - CORE / 2, z0), (16.0, PIN_Y + CORE / 2, z1), rot=fa.rotation((16.0, PIN_Y, 15.0), "z", 22.5)),
          panel(sc, "garland", body, (5.0, low - CORE / 2, z0), (11.0, low + CORE / 2, z1))]
    front = z0 - 0.1
    for kind, palette, x, y, z, turn in (("small", RED_LEAF, 3.25, 11.0, front, -22.5), ("oak", GOLD_LEAF, 5.0, 10.25, front - 0.15, -45.0),
                                         ("maple", ORANGE_LEAF, 7.25, low, front - 0.3, 0.0), ("small", GOLD_LEAF, 9.75, low + 0.25, front, 22.5),
                                         ("oak", RED_LEAF, 11.25, 10.25, front - 0.15, 45.0), ("small", ORANGE_LEAF, 12.75, 11.0, front - 0.3, 22.5)):
        el.append(upright_leaf(sc, kind, palette, x, y, z, turn))
    for ax, top in ((7.25, low - CORE / 2), (9.0, low - CORE / 2)):
        hang = 1.0 if ax < 8 else 2.0
        el.append(panel(sc, "acorn_string", lengthwise(TWINE), (ax + 0.25, top - hang, 14.0), (ax + 0.75, top, 14.5)))
        el.append(panel(sc, "acorn_cap", smooth(ACORN_CAP), (ax - 0.25, top - hang - 0.75, 13.5), (ax + 1.25, top - hang, 15.0)))
        el.append(panel(sc, "acorn_nut", smooth(ACORN_NUT), (ax, top - hang - 2.25, 13.75), (ax + 1.0, top - hang - 0.75, 14.75)))
    return el


def garland_tail(sc, x):
    """A tied end at x (0 or 16): a red ribbon bow at the pin and a short length of leaves hanging from it."""
    inward = 1.0 if x == 0 else -1.0
    cx = x + inward * 2.0
    z0, z1 = BACK - CORE, BACK
    ribbon = flat(GINGHAM)
    el = [panel(sc, "tail", leafy(), (cx - 1.0, PIN_Y - 6.0, z0 + 0.25), (cx + 1.0, PIN_Y, z1 - 0.25)),
          upright_leaf(sc, "oak", RED_LEAF if x == 0 else GOLD_LEAF, cx, PIN_Y - 6.0, z0 - 0.2),
          panel(sc, "knot", ribbon, (cx - 0.75, PIN_Y - 0.25, z0 - 0.75), (cx + 0.75, PIN_Y + 1.25, z0 + 0.5))]
    for side in (-1.0, 1.0):
        el.append(panel(sc, "bow", ribbon, (cx + side * 0.75 - (1.25 if side < 0 else 0.0), PIN_Y, z0 - 0.5),
                        (cx + side * 0.75 + (0.0 if side < 0 else 1.25), PIN_Y + 1.0, z0 + 0.25),
                        rot=fa.rotation((cx + side * 0.75, PIN_Y + 0.5, z0), "z", 22.5 * side)))
    return el


def autumn_leaf_wall_garland():
    """The Autumn Leaf Wall Garland, hung on a wall facing north: leaves packed along a twine in a swag across its block,
    pinned at its top corners; a row of them on one wall joins into one garland (LeafGarlandBlock's `left` and
    `right`), and only its two ends are tied off with a red bow and a hanging tail of leaves. Its models: `single`,
    `left` (joined to its left only, tied at its right), `right` and `both`."""
    sc = Sculpt("autumn_leaf_wall_garland", 4301, 128)
    swag = garland_swag(sc)
    sc.models["single"] = swag + garland_tail(sc, 0.0) + garland_tail(sc, 16.0)
    sc.models["left"] = swag + garland_tail(sc, 16.0)
    sc.models["right"] = garland_tail(sc, 0.0) + swag
    sc.models["both"] = list(swag)
    sc.models["whole"] = sc.models["single"]
    return sc


def amber_jar_lantern():
    """The Amber Jar Lantern: an amber glass jar glowing with the candle inside it (the glass full-bright, brightest in a
    band round its middle), a tin screw band round its neck under its lid, twine wound round under the band and a wire
    bail over its top. Standing (`whole`), or hanging from its bail on a twine loop (`hanging`), as vanilla's lantern."""
    sc = Sculpt("amber_jar_lantern", 4302, 64)

    def jar(y):
        glow = glass_glow()
        el = crossed_y(sc, "foot", solid(AMBER_GLASS, 1), 8.0, 8.0, 5.0, y, y + 0.5)
        el += lit(crossed_y(sc, "glass", glow, 8.0, 8.0, 6.0, y + 0.5, y + 6.0, down=solid(AMBER_GLASS, 1)))
        el += lit(crossed_y(sc, "shoulder", solid(AMBER_GLASS, 3), 8.0, 8.0, 5.0, y + 6.0, y + 6.5))
        el += crossed_y(sc, "twine", lengthwise(TWINE), 8.0, 8.0, 4.5, y + 6.5, y + 7.0)
        el += crossed_y(sc, "band", flat(TIN), 8.0, 8.0, 4.0, y + 7.0, y + 8.0, up=solid(TIN, 2))
        wire = flat(TIN)
        el += [panel(sc, "bail", wire, (5.5, y + 7.25, 7.75), (6.0, y + 9.5, 8.25)),
               panel(sc, "bail", wire, (10.0, y + 7.25, 7.75), (10.5, y + 9.5, 8.25)),
               panel(sc, "bail", wire, (5.5, y + 9.5, 7.75), (10.5, y + 10.0, 8.25))]
        return el
    sc.models["whole"] = jar(0.0)
    sc.models["hanging"] = jar(1.0) + [panel(sc, "loop", lengthwise(TWINE), (7.5, 11.0, 7.5), (8.5, 16.0, 8.5))]
    return sc


def glass_glow():
    """Amber glass lit from within: its brightest band round the middle (where the flame is), dimming in bands above and
    below it, its foot darkest and its rim lit; a candle's dark wick line up the middle of the lower band."""
    def paint(p):
        for y in range(p.h):
            t = y / max(1, p.h - 1)
            k = 4 if 0.3 <= t <= 0.6 else 3 if 0.15 <= t <= 0.75 else 2 if t < 0.9 else 1
            if y == 0:
                k = 3
            for x in range(p.w):
                c = shade(AMBER_GLASS, 5 if k == 4 and x == p.w // 2 else k)
                p.put(x, y, c)
    return tagged(paint, "glass_glow")


def flame_sprite():
    """A candle flame, cut out: white-yellow at its heart, yellow round it, orange at its foot, three texels square."""
    rows = (".Y.", "YWY", ".O.")
    return sprite(rows, {"Y": shade(FLAME, 2), "W": shade(FLAME, 3), "O": shade(FLAME, 1)})


def candle(sc, x, z, y0, h):
    """A pillar candle two pixels across standing at y0 `h` high, its wick on top; and its flame, for the lit model."""
    body = [panel(sc, "candle", smooth(WAX, foot=False), (x - 1.0, y0, z - 1.0), (x + 1.0, y0 + h, z + 1.0), up=solid(WAX, 3)),
            panel(sc, "wick", solid(SHADOW, 0), (x - 0.25, y0 + h, z - 0.25), (x + 0.25, y0 + h + 0.75, z + 0.25))]
    uv = sc.piece("flame", 3, 3, flame_sprite())
    top = y0 + h + 0.25
    flame = lit([fa.plane_xy(x - 1.0, x + 1.0, top, top + 2.0, z, uv), fa.plane_zy(z - 1.0, z + 1.0, top, top + 2.0, x, uv)])
    return body, flame


def pumpkin_candle_centerpiece():
    """The Pumpkin Candle Centerpiece, facing north: a long walnut dough bowl filled with autumn leaves, three cream
    pillar candles of different heights standing in it among little pumpkins, an orange and a white, and acorns, with
    leaves spilling over its ends. Lit (`lit`), each candle burns with a small flame."""
    sc = Sculpt("pumpkin_candle_centerpiece", 4303, 128)
    bowl = lengthwise(WALNUT)
    x0, z0, x1, z1, top = 1.5, 4.5, 14.5, 11.5, 2.5
    el = ppd.walls(sc, "bowl", bowl, x0, z0, x1, z1, 1.0, 0.0, top, top=solid(WALNUT, 3))
    el.append(panel(sc, "bowl_floor", bowl, (x0 + 1.0, 0.0, z0 + 1.0), (x1 - 1.0, 0.5, z1 - 1.0)))
    el.append(panel(sc, "bed", leafy(), (x0 + 1.0, 0.5, z0 + 1.0), (x1 - 1.0, 2.0, z1 - 1.0)))
    flames = []
    for x, z, h in ((4.5, 8.0, 5.0), (8.0, 8.5, 7.0), (11.5, 7.5, 4.0)):
        body, flame = candle(sc, x, z, 2.0, h)
        el += body
        flames += flame
    el += pumpkin(sc, "pumpkin", 6.25, 6.25, 3.0, 2.0, 2.5)
    el += pumpkin(sc, "white_pumpkin", 10.0, 10.0, 3.0, 2.0, 2.0, WHITE_PUMPKIN)
    for ax, az in ((9.5, 6.0), (6.0, 10.25)):
        el.append(panel(sc, "acorn_nut", smooth(ACORN_NUT), (ax, 2.0, az), (ax + 1.0, 3.25, az + 1.0)))
        el.append(panel(sc, "acorn_cap", smooth(ACORN_CAP), (ax - 0.25, 3.25, az - 0.25), (ax + 1.25, 4.0, az + 1.25)))
    el += [upright_leaf(sc, "small", RED_LEAF, 3.5, 4.0, 6.0, 22.5, axis="x"),
           upright_leaf(sc, "small", GOLD_LEAF, 12.5, 4.0, 10.0, -22.5, axis="x"),
           flat_leaf(sc, "small", ORANGE_LEAF, 11.25, 9.75, 2.1, 45.0)]
    sc.models["whole"] = el
    sc.models["lit"] = el + flames
    return sc


SIGN_MAP = ("GGGGGGGGGGGGG",
            "GcccccccccccG",
            "Gccsccwcccl.G",
            "GcoooccwwcrrG",
            "GoooooywwwrrG",
            "GooooocwwrrrG",
            "GcooocwwwcrcG",
            "GcccccccccccG",
            "GGGGGGGGGGGGG")


def sign_face():
    """The market sign's painted face, from SIGN_MAP: a pumpkin, a sheaf of wheat and a red apple on cream, framed in
    green."""
    colours = {"G": shade(SIGN_GREEN, 2), "c": shade(SIGN_CREAM, 2), "o": shade(PUMPKIN, 2), "s": shade(STEM, 1), "w": shade(WHEAT, 2),
               "y": shade(WHEAT, 1), "r": shade(APPLE, 2), "l": shade(LEAF, 1), ".": shade(SIGN_CREAM, 2)}

    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                row = SIGN_MAP[min(y, len(SIGN_MAP) - 1)]
                p.put(x, y, colours[row[min(x, len(row) - 1)]])
    return tagged(paint, "sign_face")


def harvest_market_sign():
    """The Harvest Market Sign, hung on a wall facing north: two wrought-iron brackets out from the wall, a painted
    signboard hung under them on iron rings, its face a pumpkin, a sheaf of wheat and an apple framed in green, a
    little planked cap along its top."""
    sc = Sculpt("harvest_market_sign", 4304, 64)
    boards = planks(WOOD, board=3, nails=False)
    iron = flat(IRON)
    el = []
    for bx in (2.5, 12.5):
        el.append(panel(sc, "bracket", iron, (bx, 13.5, 11.0), (bx + 1.0, 14.5, 16.0)))
        el.append(panel(sc, "bracket", iron, (bx, 11.5, 15.0), (bx + 1.0, 13.5, 16.0)))
        el.append(panel(sc, "ring", iron, (bx + 0.25, 12.0, 11.25), (bx + 0.75, 13.5, 11.75)))
    el.append(panel(sc, "cap", boards, (1.0, 11.5, 10.5), (15.0, 12.0, 12.5)))
    el.append(panel(sc, "board", boards, (1.5, 2.5, 11.0), (14.5, 11.5, 12.0), north=sign_face()))
    sc.models["whole"] = el
    return sc


def pan(sc, name, cx, cy, d, z0, z1, handle_top):
    """A copper pan hung by its handle on a hook, its bottom to the room: round in two boxes crossed, its bottom a tone
    lighter inside its rim, its handle up from its top edge to `handle_top`."""
    copper, face = flat(COPPER), solid(COPPER, 3)
    el = crossed_z(sc, name, copper, cx, cy, d, z0, z1, north=solid(COPPER, 2))
    # Its bottom, a tone lighter inside its rim.
    el += crossed_z(sc, f"{name}_bottom", copper, cx, cy, d - 2.0, z0 - 0.25, z0, north=face)
    el.append(panel(sc, f"{name}_handle", lengthwise(COPPER), (cx - 0.5, cy + d / 2, z1 - 1.0), (cx + 0.5, handle_top, z1)))
    return el


def copper_pan_rack():
    """The Copper Pan Rack, hung on a wall facing north: a walnut rail on two iron brackets with three iron hooks, a
    copper frying pan, a deep saucepan and a ladle hanging from them by their handles, their bottoms to the room."""
    sc = Sculpt("copper_pan_rack", 4305, 64)
    rail, iron = lengthwise(WALNUT), flat(IRON)
    el = [panel(sc, "rail", rail, (1.0, 12.0, 14.0), (15.0, 14.0, 16.0))]
    for bx in (2.0, 13.0):
        el.append(panel(sc, "bracket", iron, (bx, 11.0, 15.0), (bx + 1.0, 14.5, 16.0)))
    for hx in (4.0, 8.0, 12.0):
        el.append(panel(sc, "hook", iron, (hx - 0.25, 10.5, 14.25), (hx + 0.25, 12.0, 14.75)))
        el.append(panel(sc, "hook", iron, (hx - 0.25, 10.5, 13.75), (hx + 0.25, 11.0, 14.25)))
    el += pan(sc, "frying_pan", 4.0, 4.0, 6.0, 13.5, 15.0, 11.0)
    el += pan(sc, "saucepan", 8.0, 5.5, 4.5, 12.5, 15.0, 11.0)
    # The ladle: a small deep bowl on a long handle.
    el += crossed_z(sc, "ladle", flat(COPPER), 12.0, 3.0, 3.0, 13.0, 15.0, north=solid(COPPER, 3))
    el.append(panel(sc, "ladle_handle", lengthwise(COPPER), (11.5, 4.5, 14.0), (12.5, 11.0, 15.0)))
    sc.models["whole"] = el
    return sc


# ---------------------------------------------------------------- G. the playful statement pieces

def rings():
    """A sawn log's end: growth rings a texel wide round its middle, lit and shaded by turns."""
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                ring = int(max(abs(x - cx), abs(y - cy)))
                p.put(x, y, shade(RINGS, (3, 2, 2, 1)[ring % 4]))
    return tagged(paint, "rings")


def log_slice(sc, cx, cz, d, h):
    """A slice of log for a carving to stand on: bark round its side, its rings on top."""
    return crossed_y(sc, "log", lengthwise(BARK), cx, cz, d, 0.0, h, up=rings(), down=solid(BARK, 1))


def barred(palette, light):
    """Barred feathers: rows of its own tone with a row of `light` every third, staggered in pairs of texels."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                bar = (y + (x // 2) % 2) % 3 == 0
                p.put(x, y, shade(light, 2) if bar else shade(palette, 3 if y == 0 else 2))
    return tagged(paint, "barred", palette, light)


def feathers(palette):
    """Body feathers: overlapping scallops, a texel lit at each one's tip and a texel of shadow under it, in rows two
    texels high staggered by two."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                row = y // 2
                tip = (x + 2 * (row % 2)) % 4
                k = 4 if y % 2 == 0 and tip == 1 else 1 if y % 2 == 1 and tip == 1 else 3 if y % 2 == 0 else 2
                p.put(x, y, shade(palette, k))
    return tagged(paint, "feathers", palette)


def tail_feather(palette):
    """A tail feather fanned up: its own colour, a dark band near its end and a cream tip (its top two rows)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                if y == 0:
                    c = shade(SIGN_CREAM, 2)
                elif y == 1:
                    c = shade(BRONZE_FEATHER, 0)
                else:
                    c = shade(palette, 3 if x == 0 else 2)
                p.put(x, y, c)
    return tagged(paint, "tail_feather", palette)


def face(skin, eyes, glint=False):
    """A carved face, painted: `skin`, with a dark eye at each of `eyes` (texel positions) and a white glint above-left
    of each where there is room (`glint`)."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, shade(skin, 2 if y == 0 else 1))
        for ex, ey in eyes:
            ex, ey = min(ex, p.w - 1), min(ey, p.h - 1)
            p.put(ex, ey, shade(EYE, 0))
            if glint and ex > 0 and ey > 0:
                p.put(ex - 1, ey - 1, shade(EYE, 1))
    return tagged(paint, "face", skin, tuple(eyes), glint)


def turkey_statuette():
    """The Turkey Statuette, facing north: a friendly carved and painted turkey standing on a slice of log, its bronze
    body barred at the wings, its head pale blue with a round dark eye each side, a yellow beak, a red snood and wattle,
    and its tail fanned up behind it in bronze, orange and red feathers tipped with cream."""
    sc = Sculpt("turkey_statuette", 4401, 128)
    el = log_slice(sc, 8.0, 8.5, 11.0, 1.5)
    beak = flat(BEAK)
    el += [panel(sc, "foot", beak, (6.25, 1.5, 6.0), (7.25, 3.0, 7.5)), panel(sc, "foot", beak, (8.75, 1.5, 6.0), (9.75, 3.0, 7.5))]
    body = feathers(BRONZE_FEATHER)
    el += crossed_y(sc, "body", body, 8.0, 8.5, 7.0, 3.0, 8.5)
    el.append(panel(sc, "breast", body, (6.0, 3.5, 4.5), (10.0, 8.0, 5.25)))
    wing = barred(BRONZE_FEATHER, SIGN_CREAM)
    el += [panel(sc, "wing", wing, (3.75, 4.0, 6.5), (4.5, 7.5, 11.0)), panel(sc, "wing", wing, (11.5, 4.0, 6.5), (12.25, 7.5, 11.0))]
    # The neck and head, a little forward of the body.
    el.append(panel(sc, "neck", flat(TURKEY_HEAD), (7.0, 8.0, 5.5), (9.0, 10.5, 7.5)))
    head = flat(TURKEY_HEAD)
    el.append(panel(sc, "head", head, (6.5, 10.5, 4.5), (9.5, 13.0, 7.5), east=face(TURKEY_HEAD, [(1, 1)], glint=True),
                    west=face(TURKEY_HEAD, [(1, 1)], glint=True), north=face(TURKEY_HEAD, [(0, 1), (2, 1)])))
    el.append(panel(sc, "beak", beak, (7.5, 11.0, 3.5), (8.5, 12.0, 4.5)))
    wattle = flat(WATTLE)
    el += [panel(sc, "snood", wattle, (7.75, 10.25, 3.75), (8.25, 11.0, 4.25)),
           panel(sc, "wattle", wattle, (7.25, 9.0, 4.75), (8.75, 10.5, 5.5))]
    # The tail fanned up behind: five feathers from one point, each at its own depth so none shares a plane.
    pivot_y, z = 6.5, 11.5
    for i, (angle, colour) in enumerate(((0.0, "bronze"), (-22.5, "orange"), (22.5, "orange"), (-45.0, "red"), (45.0, "red"))):
        zz = z + 0.75 * (i + 1) // 2 * 1.0 + (0.0 if i == 0 else 0.0)
        zz = z + (0.0, 0.6, 0.6, 1.2, 1.2)[i]
        rot = fa.rotation((8.0, pivot_y, zz), "z", angle) if angle else None
        el.append(panel(sc, f"fan_{colour}", tail_feather(FAN[colour]), (7.0, pivot_y, zz), (9.0, pivot_y + 8.5, zz + 0.6), rot=rot))
    sc.models["whole"] = el
    return sc


def squirrel_acorn_figurine():
    """The Squirrel with Acorn Figurine, facing north: a little carved and painted red squirrel sitting up on a slice of
    log, a cream belly, holding an acorn to its chest in both paws, its ears pricked and its bushy tail curled up over
    its back."""
    sc = Sculpt("squirrel_acorn_figurine", 4402, 128)
    fur = feathers(SQUIRREL)
    el = log_slice(sc, 8.0, 8.5, 7.0, 1.0)
    el.append(panel(sc, "body", flat(SQUIRREL), (6.0, 1.0, 7.0), (10.0, 6.0, 10.0)))
    el.append(panel(sc, "belly", flat(SQUIRREL_BELLY), (6.75, 1.5, 6.75), (9.25, 5.0, 7.0)))
    el += [panel(sc, "haunch", flat(SQUIRREL), (5.25, 1.0, 7.5), (6.0, 3.0, 10.0)), panel(sc, "haunch", flat(SQUIRREL), (10.0, 1.0, 7.5), (10.75, 3.0, 10.0))]
    el.append(panel(sc, "head", flat(SQUIRREL), (6.25, 6.0, 6.5), (9.75, 9.0, 9.5),
                    north=face(SQUIRREL, [(0, 1), (3, 1)]), east=face(SQUIRREL, [(1, 1)]), west=face(SQUIRREL, [(1, 1)])))
    el.append(panel(sc, "nose", flat(SQUIRREL_BELLY), (7.5, 6.5, 6.0), (8.5, 7.5, 6.5)))
    el += [panel(sc, "ear", flat(SQUIRREL), (6.5, 9.0, 8.0), (7.25, 10.25, 8.75)), panel(sc, "ear", flat(SQUIRREL), (8.75, 9.0, 8.0), (9.5, 10.25, 8.75))]
    # The acorn held to its chest, and the paws round it.
    el.append(panel(sc, "acorn_nut", smooth(ACORN_NUT), (7.25, 3.5, 5.25), (8.75, 5.0, 6.75)))
    el.append(panel(sc, "acorn_cap", smooth(ACORN_CAP), (7.0, 5.0, 5.0), (9.0, 5.75, 7.0)))
    el += [panel(sc, "paw", flat(SQUIRREL), (6.25, 4.0, 5.75), (7.25, 5.0, 7.0)), panel(sc, "paw", flat(SQUIRREL), (8.75, 4.0, 5.75), (9.75, 5.0, 7.0))]
    # The tail: up the back from the log, then curling over forward.
    el.append(panel(sc, "tail", fur, (6.5, 1.0, 10.0), (9.5, 5.0, 12.5)))
    el.append(panel(sc, "tail", fur, (6.5, 4.5, 10.5), (9.5, 10.0, 13.0), rot=fa.rotation((8.0, 4.5, 11.75), "x", 22.5)))
    el.append(panel(sc, "tail", fur, (6.75, 9.0, 9.0), (9.25, 11.0, 12.0)))
    sc.models["whole"] = el
    return sc


def acorn_leaf_mobile():
    """The Acorn and Leaf Mobile, hung from a ceiling: two twigs crossed on a twine loop, and from their ends and middle
    strings of acorns and autumn leaves at different lengths, red, gold and orange."""
    sc = Sculpt("acorn_leaf_mobile", 4403, 64)
    twig, string = lengthwise(TWIG), lengthwise(TWINE)
    el = [panel(sc, "loop", string, (7.75, 14.0, 7.75), (8.25, 16.0, 8.25)),
          panel(sc, "twig", twig, (2.0, 13.0, 7.5), (14.0, 14.0, 8.5)),
          panel(sc, "twig", twig, (7.5, 12.0, 2.0), (8.5, 13.0, 14.0))]
    def acorn(x, y, z):
        return [panel(sc, "acorn_nut", smooth(ACORN_NUT), (x - 0.75, y - 2.0, z - 0.75), (x + 0.75, y - 0.5, z + 0.75)),
                panel(sc, "acorn_cap", smooth(ACORN_CAP), (x - 1.0, y - 0.5, z - 1.0), (x + 1.0, y + 0.25, z + 1.0))]
    # Each string: where it hangs from, how far down, and what hangs at its end.
    for x, top, z, length, end in ((2.75, 13.0, 8.0, 5.0, "acorn"), (13.25, 13.0, 8.0, 3.0, "red"), (8.0, 12.0, 2.75, 4.0, "gold"),
                                   (8.0, 12.0, 13.25, 6.0, "acorn"), (8.0, 12.0, 8.0, 5.0, "orange")):
        el.append(panel(sc, "string", string, (x - 0.25, top - length, z - 0.25), (x + 0.25, top, z + 0.25)))
        bottom = top - length
        if end == "acorn":
            el += acorn(x, bottom, z)
        else:
            palette = {"red": RED_LEAF, "gold": GOLD_LEAF, "orange": ORANGE_LEAF}[end]
            kind = "small" if end == "red" else "maple"
            half = leaf_size(kind)[1] / 2
            el.append(upright_leaf(sc, kind, palette, x, bottom - half, z - 0.5 if x == 8.0 else z))
    sc.models["whole"] = el
    return sc


def table_cornucopia():
    """The Table Cornucopia, facing north: the Giant Cornucopia's horn of plenty at a table's size, woven of the same
    willow with the same braided rim, lying on its side, its mouth to the west (to the right of whoever placed it) and
    its tail curling up at the east; spilling a little pumpkin, apples, a bunch of grapes, an ear of corn and a sheaf of
    wheat ears from its mouth."""
    sc = Sculpt("table_cornucopia", 4404, 128)
    weave, inside, rim = wicker(), wicker(shadowed=True), braid()
    cy, cz, r = 3.75, 8.5, 3.5
    el = []
    # The mouth: four willow walls round its opening behind a braided rim, a pixel thick.
    for x0, x1, paint, name in ((2.25, 4.0, weave, "mouth"), (1.25, 2.25, rim, "rim")):
        rr = r + (0.25 if name == "rim" else 0.0)
        el += [panel(sc, name, paint, (x0, cy + rr - 1.0, cz - rr + 1.0), (x1, cy + rr, cz + rr - 1.0), down=inside if name == "mouth" else paint),
               panel(sc, name, paint, (x0, cy - rr, cz - rr + 1.0), (x1, cy - rr + 1.0, cz + rr - 1.0), up=inside if name == "mouth" else paint),
               panel(sc, name, paint, (x0, cy - rr, cz - rr), (x1, cy + rr, cz - rr + 1.0), south=inside if name == "mouth" else paint),
               panel(sc, name, paint, (x0, cy - rr, cz + rr - 1.0), (x1, cy + rr, cz + rr), north=inside if name == "mouth" else paint)]
    # The horn narrowing to the east, its first length's west face the dark inside seen through the mouth.
    for i, (x0, x1, d) in enumerate(((4.0, 6.5, 6.0), (6.5, 9.0, 5.0), (9.0, 11.0, 4.0), (11.0, 12.5, 3.0))):
        over = {"west": inside} if i == 0 else {}
        el += crossed_x(sc, "horn", weave, x0, x1, d / 2 + 0.25, cz, d, **over)
    # The tail curling up and back over the horn.
    el.append(panel(sc, "tail", weave, (12.0, 1.0, cz - 1.0), (14.0, 3.0, cz + 1.0), rot=fa.rotation((12.0, 1.5, cz), "z", 45.0)))
    el.append(panel(sc, "tail", weave, (13.0, 3.0, cz - 0.75), (14.5, 7.0, cz + 0.75)))
    el.append(panel(sc, "tail", weave, (11.5, 6.5, cz - 0.5), (13.5, 7.5, cz + 0.5), rot=fa.rotation((13.5, 7.0, cz), "z", -22.5)))
    # The harvest spilling from the mouth.
    el += pumpkin(sc, "pumpkin", 2.25, 4.0, 4.0, 0.0, 3.0)
    el += apple(sc, 4.5, 0.0, 2.25, 2.0) + apple(sc, 0.5, 0.0, 11.5, 2.0, GREEN_APPLE)
    el += [panel(sc, "grapes", grapes(), (0.5, 0.0, 9.0), (2.5, 2.5, 11.0)),
           panel(sc, "grape_stalk", lengthwise(STEM), (1.25, 2.5, 9.75), (1.75, 4.0, 10.25))]
    turn = fa.rotation((3.5, 5.5, 7.0), "z", -22.5)
    el.append(panel(sc, "corn", kernels(), (0.5, 5.0, 6.5), (4.0, 6.0, 7.5), rot=turn))
    el.append(panel(sc, "husk", lengthwise(HUSK), (1.0, 4.5, 6.0), (4.0, 5.0, 8.0), rot=turn))
    el.append(panel(sc, "ears", ears(), (2.5, 5.5, 8.5), (4.5, 7.5, 10.5), rot=fa.rotation((3.5, 5.5, 9.5), "z", 22.5)))
    el.append(panel(sc, "gourd", gourd(), (5.0, 0.0, 12.0), (6.5, 1.5, 13.5)))
    sc.models["whole"] = el
    return sc


# ---------------------------------------------------------------- building and files

BUILDERS = {"pumpkin_and_gourd_stack": pumpkin_and_gourd_stack, "wicker_apple_basket": wicker_apple_basket,
            "pear_basket": pear_basket, "root_vegetable_hamper": root_vegetable_hamper,
            "harvest_place_setting": harvest_place_setting, "napkin_and_cutlery_caddy": napkin_and_cutlery_caddy,
            "bread_basket": bread_basket, "gravy_boat": gravy_boat, "covered_butter_dish": covered_butter_dish,
            "pie_stand": pie_stand, "autumn_leaf_wall_garland": autumn_leaf_wall_garland,
            "amber_jar_lantern": amber_jar_lantern, "pumpkin_candle_centerpiece": pumpkin_candle_centerpiece,
            "harvest_market_sign": harvest_market_sign, "copper_pan_rack": copper_pan_rack,
            "turkey_statuette": turkey_statuette, "acorn_leaf_mobile": acorn_leaf_mobile,
            "squirrel_acorn_figurine": squirrel_acorn_figurine, "table_cornucopia": table_cornucopia}
_built = {}


def build(block):
    """The prop's Sculpt, its pieces packed into the smallest texture of 64 or 128 texels square they fit."""
    if block not in _built:
        try:
            _built[block] = ppd.planned(BUILDERS[block], 64)
        except ValueError:
            _built[block] = ppd.planned(BUILDERS[block], 128)
    return _built[block]


def _image_of(name):
    return build(name).atlas.img if name in BUILDERS else None


DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
           "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
           "head": {"rotation": [0, 0, 0], "translation": [0, 10, 0], "scale": [0.6, 0.6, 0.6]},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
           "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}
WALL_DISPLAY = dict(DISPLAY, gui={"rotation": [30, 160, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                    fixed={"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]})


def bounds(elements):
    """The box round every unturned element (x0, y0, z0, x1, y1, z1), and every turned one's corners turned."""
    pts = []
    for e in elements:
        corners = [(x, y, z) for x in (e["from"][0], e["to"][0]) for y in (e["from"][1], e["to"][1]) for z in (e["from"][2], e["to"][2])]
        r = e.get("rotation")
        if r:
            a = math.radians(r["angle"])
            ox, oy, oz = r["origin"]
            turned = []
            for x, y, z in corners:
                dx, dy, dz = x - ox, y - oy, z - oz
                if r["axis"] == "x":
                    dy, dz = dy * math.cos(a) - dz * math.sin(a), dy * math.sin(a) + dz * math.cos(a)
                elif r["axis"] == "y":
                    dx, dz = dx * math.cos(a) + dz * math.sin(a), -dx * math.sin(a) + dz * math.cos(a)
                else:
                    dx, dy = dx * math.cos(a) - dy * math.sin(a), dx * math.sin(a) + dy * math.cos(a)
                turned.append((ox + dx, oy + dy, oz + dz))
            corners = turned
        pts += corners
    return [min(p[k] for p in pts) for k in range(3)] + [max(p[k] for p in pts) for k in range(3)]


def turned(model, facing, **extra):
    return {"model": rid(f"block/{model}"), **({"y": FACINGS[facing]} if FACINGS[facing] else {}), **extra}


def assets(root, write, lang):
    """Each prop's block models (drawn whole, separated), its blockstate by its kind's states, item model and item."""
    write = fa.closing_writer(write, _image_of)
    models = root / "models" / "block"
    lang.update(td.names())
    for block, spec in td.PROPS.items():
        if block not in BUILDERS:
            continue
        sc = build(block)
        kind = spec["kind"]
        whole = d20d.separated(sc.models["whole"])
        states = root / "blockstates" / f"{block}.json"
        if kind == "garland":
            for name in ("single", "left", "right", "both"):
                write(models / f"{block}_{name}.json", fa.model(block, d20d.separated(sc.models[name])))
            joined = {(False, False): "single", (True, False): "left", (False, True): "right", (True, True): "both"}
            write(states, {"variants": {f"facing={f},left={str(l).lower()},right={str(r).lower()}": turned(f"{block}_{joined[(l, r)]}", f)
                                        for f in FACINGS for l in (False, True) for r in (False, True)}})
        elif kind == "lantern":
            write(models / f"{block}.json", fa.model(block, whole))
            write(models / f"{block}_hanging.json", fa.model(block, d20d.separated(sc.models["hanging"])))
            write(states, {"variants": {"hanging=false": {"model": rid(f"block/{block}")},
                                        "hanging=true": {"model": rid(f"block/{block}_hanging")}}})
        elif kind == "candles":
            write(models / f"{block}.json", fa.model(block, whole))
            write(models / f"{block}_lit.json", fa.model(block, d20d.separated(sc.models["lit"])))
            write(states, {"variants": {f"facing={f},lit={l}": turned(block + ("_lit" if l == "true" else ""), f)
                                        for f in FACINGS for l in ("false", "true")}})
        else:
            write(models / f"{block}.json", fa.model(block, whole))
            write(states, {"variants": {f"facing={f}": turned(block, f) for f in FACINGS}})
        display = WALL_DISPLAY if kind in ("wall", "garland") else DISPLAY
        write(root / "models" / "item" / f"{block}.json", fa.model(block, whole, display))
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})
    # The runner is a flat item, laid on a Feast Table (feast_data draws it there).
    item = td.RUNNER["item"]
    write(root / "models" / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
    write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})


def loot(out, write):
    """Each prop drops itself (once: none is more than one block)."""
    from decor_data import self_drop
    for block in td.PROPS:
        write(out / f"{block}.json", self_drop(block))


def tags(tags):
    """Copper, tin and china come off quickest with a pickaxe (thanksgiving_decor.PICKAXE); wood, willow and leaves
    with an axe."""
    for block in td.PROPS:
        tags.add("block", "minecraft:mineable/pickaxe" if block in td.PICKAXE else "minecraft:mineable/axe", rid(block))


def textures():
    import item_icons
    out = {("block", block): build(block).atlas.img for block in td.PROPS if block in BUILDERS}
    out[("block", "feast_table_leaf_runner")] = runner_texture()
    out[("block", "feast_table_leaf_runner_leaves")] = runner_leaves_texture()
    out[("item", td.RUNNER["item"])] = item_icons.draw(td.RUNNER["item"])
    return out
