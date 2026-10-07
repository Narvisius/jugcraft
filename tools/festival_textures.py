"""Original 16x16 textures for the Agriculture branch's festival crops, slice 3 (requires Pillow).

Called from crop_textures.crop_textures(). As with the rest of the branch's art, every pixel is drawn
here by code from fixed seeds; no Mojang texture is read, traced or recolored. Gourd, lantern and wood
textures are mapped onto their models by position, so each is drawn where its model shows it. The chestnut
tree's wood and leaves are drawn with every other tree's by wood_style.py.
"""
import math
import random

from crop_textures import Canvas, rgb, outline, seeds_item, heart, KERNEL
from kitchen_textures import bowl_item, round_fruit, ONION, CABBAGE
import block_style as bs

TURNIP_LEAF = [rgb("24481c"), rgb("2f5d22"), rgb("3d7429"), rgb("4f8c33"), rgb("6aa645"), rgb("8cc062")]
TURNIP_PURPLE = [rgb("4a1f4e"), rgb("6c2f70"), rgb("8e4592"), rgb("b067b0"), rgb("cf92cc")]
TURNIP_WHITE = [rgb("b9ad9a"), rgb("d8cfbd"), rgb("ece6d8"), rgb("faf7ef")]
BUTTERNUT = [rgb("8a5a26"), rgb("b07a3a"), rgb("cf9a55"), rgb("e2b774"), rgb("f0d39c")]
ACORN = [rgb("142a16"), rgb("1e3b1f"), rgb("2a4f29"), rgb("3a6636"), rgb("4e7f47")]
ACORN_ORANGE = [rgb("b0601a"), rgb("d68428"), rgb("eba846")]
WARTY = [rgb("8c4a0c"), rgb("c06a14"), rgb("e08e22"), rgb("f2b440"), rgb("fbd678")]
WARTY_GREEN = [rgb("3e5a1c"), rgb("5a7a28"), rgb("7c9a3a")]
STEM_GREEN = [rgb("2e5518"), rgb("3f7021"), rgb("548c2c"), rgb("70a83e")]
STALK = [rgb("4a3a1c"), rgb("66522a"), rgb("857038"), rgb("a38c4c")]
CRAN_LEAF = [rgb("1c3517"), rgb("27481d"), rgb("365f26"), rgb("4a7a30"), rgb("6b8f3c")]
CRAN_STEM = [rgb("5a2a1c"), rgb("7a3a24")]
CRANBERRY = [rgb("4a0810"), rgb("7a0f1c"), rgb("a8182a"), rgb("d0344a"), rgb("f07a88")]
CRAN_FLOWER = [rgb("c8708c"), rgb("e8a2b8"), rgb("fbd8e4")]
BARK = [rgb("2b2119"), rgb("3b2e22"), rgb("4e3d2d"), rgb("63503b"), rgb("7a6449")]
CHESTNUT_LEAF = [rgb("1b3514"), rgb("254a1b"), rgb("306024"), rgb("3e772d"), rgb("508f38"), rgb("6aa84a")]
BUR_DRY = [rgb("6a4e22"), rgb("8c6a30"), rgb("b08a44"), rgb("d2b064")]
NUT = [rgb("3a1c0c"), rgb("5a2c12"), rgb("7c3e1a"), rgb("a05a2a"), rgb("c88048")]
NUT_BASE = [rgb("b8a07a"), rgb("d8c49c")]
GLOW = [rgb("c85a10"), rgb("f08c1e"), rgb("ffc23c"), rgb("fff08a")]
CRUST = [rgb("7a4a1c"), rgb("a06a2c"), rgb("c48c44"), rgb("e0b06a")]
CANDY = [rgb("f4f0e6"), rgb("f08a1c"), rgb("f6c81e")]
BOWL_BROWN = [rgb("5a3418"), rgb("74461f"), rgb("8e5a2a"), rgb("a8703a")]


def _clip(c, x0, y0, x1, y1):
    """Removes everything outside the rectangle (inclusive), for textures a model shows only in part."""
    for y in range(16):
        for x in range(16):
            if not (x0 <= x <= x1 and y0 <= y <= y1):
                c.img.putpixel((x, y), (0, 0, 0, 0))


# ---------------------------------------------------------------- turnips

def lobed_leaf(c, x0, y0, angle, length, width, palette=TURNIP_LEAF):
    """A lobed leaf from (x0, y0) along `angle` (0 = up, negative = left), with a pale midrib."""
    dx, dy = math.sin(angle), -math.cos(angle)
    px, py = -dy, dx
    for i in range(length * 2 + 1):
        t = i / (length * 2)
        cx, cy = x0 + dx * t * length, y0 + dy * t * length
        # The blade swells towards the tip and has notches between its lobes.
        w = width * math.sin(math.pi * min(1.0, t * 1.15)) * (0.55 if (i // 3) % 2 and t < 0.8 else 1.0)
        steps = int(w * 2) + 1
        for s in range(-steps, steps + 1):
            off = s / 2
            shade = 4 if off < -w / 3 else 2 if off > w / 3 else 3
            c.px(cx + px * off, cy + py * off, palette[shade])
        c.px(cx, cy, palette[5] if t > 0.15 else palette[1])


def turnip_stage(stage):
    c = Canvas()
    if stage == 0:
        for angle in (-0.9, 0.9):
            lobed_leaf(c, 7.5, 15, angle, 3, 1)
        return c.img
    leaves = {1: [(-0.7, 5), (0.6, 5), (0.0, 6)],
              2: [(-0.9, 8), (0.8, 8), (-0.3, 10), (0.35, 10)],
              3: [(-1.0, 9), (0.95, 9), (-0.35, 11), (0.4, 12), (0.05, 10)]}[stage]
    for angle, length in leaves:
        lobed_leaf(c, 7.5 + angle, 14, angle, length, 1.4 if stage == 1 else 1.9)
    if stage == 3:
        # The purple shoulder of the root showing above the soil.
        for y in range(12, 16):
            half = (3.6, 4.2, 4.4, 4.4)[y - 12]
            for x in range(int(8 - half), int(8 + half) + 1):
                purple = y < 14
                palette = TURNIP_PURPLE if purple else TURNIP_WHITE
                c.px(x, y, palette[3 if x < 8 else 2] if purple else palette[2 if x < 8 else 1])
        c.px(6, 12, TURNIP_PURPLE[4])
    return c.img


def turnip_item():
    c = Canvas()
    for y in range(5, 14):
        for x in range(3, 14):
            d = math.hypot(x - 8, (y - 9.5) * 1.1)
            if d <= 4.6:
                top = y < 9
                shade = 3 if x + y < 15 else 2 if x + y < 19 else 1
                c.px(x, y, (TURNIP_PURPLE if top else TURNIP_WHITE)[shade])
    for y in range(14, 16):
        c.px(8, y, TURNIP_WHITE[1])
    for x0, x1 in ((7, 5), (8, 8), (9, 11)):
        c.line(x0, 5, x1, 1, TURNIP_LEAF[3])
    c.px(5, 1, TURNIP_LEAF[5])
    c.px(11, 1, TURNIP_LEAF[5])
    c.px(6, 7, TURNIP_PURPLE[4])
    outline(c, rgb("2a1030"))
    return c.img


# ---------------------------------------------------------------- gourds and stems

def gourd_side(palette, ribs, seed, warts=0, stripes=None, spot=None, clean=True):
    """A gourd skin: shading from light (top) to dark (bottom), ribs, warts and stripes. `clean` (the default) varies
    the skin in small clumps (tools/block_style.py) and draws the stripes evenly, instead of at random pixels."""
    rng = random.Random(seed)
    grain = bs.grain(16, 16, seed) if clean else None
    c = Canvas()
    for y in range(16):
        for x in range(16):
            shade = 3 if y < 5 else 2 if y < 11 else 1
            if ribs and x % ribs == 0:
                shade -= 1
            if clean:
                v = grain(x, y)
                shade = max(0, min(len(palette) - 1, shade + (1 if v > 0.7 else -1 if v < 0.3 else 0)))
            elif rng.random() < 0.08:
                shade = max(0, min(len(palette) - 1, shade + rng.choice((-1, 1))))
            c.px(x, y, palette[shade])
    if stripes:
        for x in range(1, 16, 4):
            for y in range(16):
                if clean:
                    c.px(x, y, stripes[(y // 4) % len(stripes)])
                elif rng.random() < 0.7:
                    c.px(x, y, stripes[rng.randrange(len(stripes))])
    if spot:
        for y in range(11, 16):
            for x in range(4, 11):
                if (x - 7) ** 2 / 9 + (y - 14) ** 2 / 6 <= 1:
                    c.px(x, y, spot[1 if (x + y) % 3 else 2])
    for _ in range(warts):
        x, y = rng.randrange(1, 15), rng.randrange(1, 15)
        c.px(x, y, palette[4])
        c.px(x + 1, y, palette[3])
        c.px(x, y + 1, palette[1])
    return c.img


def gourd_top(palette, ribs, center):
    """The top of a gourd: ribs running into the stem end in the middle."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            angle = math.atan2(y - 7.5, x - 7.5)
            dist = math.hypot(x - 7.5, y - 7.5)
            shade = 3 if dist < 4 else 2
            if ribs and int((angle + math.pi) / (2 * math.pi) * ribs * 2) % 2 == 0 and dist > 1.5:
                shade -= 1
            c.px(x, y, palette[shade])
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        c.px(7 + dx, 7 + dy, center)
    return c.img


def gourd_stem():
    """The growing stem: a stalk with small leaves, shown from the top row down as it grows."""
    c = Canvas()
    for y in range(16):
        c.px(7, y, STEM_GREEN[1])
        c.px(8, y, STEM_GREEN[2])
    for y, direction in ((2, 1), (7, -1), (12, 1)):
        for i in range(1, 5):
            c.px(8 + direction * i if direction > 0 else 7 + direction * i, y + (i > 2), STEM_GREEN[3 if i < 4 else 2])
            c.px(8 + direction * i if direction > 0 else 7 + direction * i, y - 1 + (i > 2), STEM_GREEN[2])
    c.px(8, 0, STEM_GREEN[3])
    return c.img


def gourd_stem_attached():
    """The bent stem reaching sideways to its gourd: column 0 is the gourd's side, column 8 the stem."""
    c = Canvas()
    for y in range(9, 16):
        c.px(8, y, STALK[1])
        c.px(9, y, STALK[2])
    for i in range(9):
        x = 8 - i
        y = 8 - round(math.sin(i / 8 * math.pi / 2) * 2)
        c.px(x, y, STALK[2])
        c.px(x, y + 1, STALK[1])
    # A curling tendril and a leaf on the bend.
    for x, y in ((5, 5), (4, 4), (5, 3), (6, 4)):
        c.px(x, y, STEM_GREEN[3])
    for x, y in ((10, 10), (11, 9), (11, 10), (12, 10), (12, 9), (13, 10)):
        c.px(x, y, STEM_GREEN[2])
    return c.img


def gourd_stalk():
    """The woody stub where a gourd was cut from its vine (opaque; shown on the gourd models)."""
    c = Canvas()
    rng = random.Random(77)
    for y in range(16):
        for x in range(16):
            c.px(x, y, STALK[2 if x % 3 else 1] if rng.random() > 0.15 else STALK[3])
    return c.img


# ---------------------------------------------------------------- cranberries

def cranberry_stage(stage):
    """A cranberry bush for the raised bog model: rows 0-7 stand out of the water, the rest is under it."""
    c = Canvas()
    rng = random.Random(40 + stage)
    top = (8, 3, 1, 1)[stage]
    # Wiry red-brown runners and upright shoots with small glossy leaves.
    shoots = (2, 5, 6, 6)[stage]
    for i in range(shoots):
        x = 2 + (i * 12) / max(1, shoots - 1) if shoots > 1 else 7.5
        lean = rng.uniform(-1.5, 1.5)
        height = 15 - top + rng.randint(-1, 0)
        for j in range(height):
            y = 15 - j
            sx = x + lean * j / max(1, height)
            c.px(sx, y, CRAN_STEM[j % 2])
            if j % 2 == 1:
                side = 1 if (j // 2 + i) % 2 else -1
                c.px(sx + side, y, CRAN_LEAF[3])
                c.px(sx + side * 2, y, CRAN_LEAF[2])
                c.px(sx + side, y - 1, CRAN_LEAF[4])
        c.px(x + lean, 15 - height, CRAN_LEAF[4])
    # A dense mat of leaves at the top, where the bush stands above the water.
    if stage > 0:
        for y in range(top + 1, top + 6):
            for x in range(1, 15):
                if rng.random() < 0.45:
                    c.px(x, y, CRAN_LEAF[rng.choice((2, 3, 4))])
    if stage == 2:
        for x, y in ((3, 3), (7, 2), (11, 3), (5, 5), (10, 5), (13, 4), (2, 6)):
            c.px(x, y, CRAN_FLOWER[1])
            c.px(x, y + 1, CRAN_FLOWER[0])
            c.px(x + 1, y, CRAN_FLOWER[2])
    if stage == 3:
        for x, y in ((3, 2), (7, 2), (11, 3), (5, 5), (10, 5), (13, 6), (2, 6), (8, 7), (12, 8), (6, 8)):
            c.px(x, y, CRANBERRY[4] if (x + y) % 3 == 0 else CRANBERRY[3])
            c.px(x + 1, y, CRANBERRY[2])
            c.px(x, y + 1, CRANBERRY[2])
            c.px(x + 1, y + 1, CRANBERRY[1])
    return c.img


def cranberries_item():
    c = Canvas()
    for x, y, r in ((6, 10, 3), (11, 8, 2), (9, 12, 2), (5, 5, 2)):
        round_fruit(c, x, y, r, CRANBERRY)
    c.line(6, 7, 8, 2, CRAN_STEM[1])
    c.px(9, 2, CRAN_LEAF[3])
    c.px(10, 1, CRAN_LEAF[4])
    outline(c, rgb("2a0408"))
    return c.img


# ---------------------------------------------------------------- the chestnut tree

def sapling():
    c = Canvas()
    c.line(8, 15, 8, 7, NUT[1])
    c.line(8, 11, 5, 8, NUT[1])
    for x0, y0, angle, length in ((8, 7, -0.3, 5), (8, 7, 0.5, 5), (5, 8, -1.0, 4), (8, 10, 1.1, 4), (8, 5, 0.0, 4)):
        for i in range(length + 1):
            x, y = x0 + math.sin(angle) * i, y0 - math.cos(angle) * i
            c.px(x, y, CHESTNUT_LEAF[4] if i < length - 1 else CHESTNUT_LEAF[5])
            c.px(x + math.cos(angle), y + math.sin(angle), CHESTNUT_LEAF[3])
    # An empty bur husk at the foot, where the nut sprouted.
    for x, y in ((6, 15), (7, 14), (10, 15), (9, 14), (6, 13), (10, 13)):
        c.px(x, y, BUR_DRY[2])
    return c.img


def nut(c, cx, top, size, roasted=False):
    """A chestnut: a glossy, rounded nut with a pointed tuft on top and a pale, flat base."""
    widths = {3: [0.5, 1.5, 2.5, 3, 3, 3, 2.5], 5: [0.5, 1.5, 2.5, 3.5, 4, 4.5, 4.5, 4.5, 4.5, 4]}[size]
    for i, w in enumerate(widths):
        y = top + i
        base = i >= len(widths) - (1 if size == 3 else 2)
        for x in range(math.floor(cx - w), math.ceil(cx + w) + 1):
            if abs(x - cx) > w:
                continue
            shade = 3 if x < cx - w / 3 and i < len(widths) - 2 else 2 if x < cx + w / 3 else 1
            c.px(x, y, NUT_BASE[1 if x < cx else 0] if base else NUT[shade])
    c.px(cx, top - 1, NUT_BASE[1])
    c.px(cx - widths[2] + 1, top + 2, NUT[4])
    if roasted:
        # The X cut before roasting has split open, showing the golden kernel.
        middle = top + len(widths) // 2 - 1
        for d in (-1, 0, 1):
            c.px(cx + d, middle + d, KERNEL[3])
            c.px(cx - d, middle + d, KERNEL[2])


def chestnut_item(roasted=False):
    c = Canvas()
    if roasted:
        nut(c, 4.5, 8, 3, roasted=True)
        nut(c, 11, 9, 3, roasted=True)
        nut(c, 8, 3, 3, roasted=True)
    else:
        nut(c, 10, 7, 3)
        nut(c, 6.5, 4, 5)
    outline(c, rgb("1e0e06"))
    return c.img


# ---------------------------------------------------------------- the turnip lantern

def lantern_side(face):
    """A turnip skin (purple above, cream below) where the model's sides show it, columns 4-11 and rows 8-15,
    and the root end for its bottom in rows 0-7. The rest is filled so the texture stays opaque."""
    c = Canvas()
    c.rect(0, 0, 15, 15, TURNIP_WHITE[1])
    for y in range(0, 8):
        for x in range(4, 12):
            ring = max(abs(x - 7.5), abs(y - 3.5))
            c.px(x, y, TURNIP_WHITE[0 if ring > 3 else 1 if ring > 2 else 2])
    for x, y in ((7, 3), (8, 4), (8, 3)):
        c.px(x, y, rgb("8a7a62"))
    for y in range(8, 16):
        for x in range(4, 12):
            purple = y < 11 or (y == 11 and x % 3 == 0)
            palette = TURNIP_PURPLE if purple else TURNIP_WHITE
            c.px(x, y, palette[3 if x < 7 else 2] if purple else palette[2 if x < 8 else 1])
    c.px(5, 9, TURNIP_PURPLE[4])
    if face:
        # A crudely carved face with candlelight inside: slanted eyes and a jagged grin.
        for x, y in ((5, 10), (6, 10), (6, 11), (9, 10), (10, 10), (9, 11)):
            c.px(x, y, GLOW[3])
        for x in range(5, 11):
            c.px(x, 13, GLOW[2] if x % 2 else GLOW[1])
        for x in (6, 8, 10):
            c.px(x, 12, GLOW[2])
        c.px(7, 14, GLOW[1])
        c.px(9, 14, GLOW[1])
    return c.img


def lantern_top():
    """The turnip's crown (columns and rows 4-11) with the leaf-stalk stubs in the middle, as a lid.
    The rest is filled so the texture stays opaque."""
    c = Canvas()
    c.rect(0, 0, 15, 15, TURNIP_PURPLE[3])
    for y in range(4, 12):
        for x in range(4, 12):
            ring = max(abs(x - 7.5), abs(y - 7.5))
            c.px(x, y, TURNIP_PURPLE[3 if ring > 3 else 2 if ring > 2 else 1])
    for y in range(5, 10):
        for x in range(6, 10):
            c.px(x, y, TURNIP_LEAF[4] if y == 5 else TURNIP_LEAF[2] if x == 9 or y == 9 else TURNIP_LEAF[3])  # lit at the top
    return c.img


# ---------------------------------------------------------------- food items

def acorn_half_item():
    """Half an acorn squash baked cut side up: dark ribbed rind, orange flesh, a browned hollow."""
    c = Canvas()
    for y in range(5, 14):
        for x in range(2, 15):
            d = (x - 8) ** 2 / 36 + (y - 9) ** 2 / 16
            if d <= 1:
                inner = (x - 8) ** 2 / 25 + (y - 8.5) ** 2 / 9
                hollow = (x - 8) ** 2 / 6 + (y - 8.5) ** 2 / 3
                if hollow <= 1:
                    c.px(x, y, ACORN_ORANGE[0] if y > 8 else rgb("8a4a14"))
                elif inner <= 1:
                    c.px(x, y, ACORN_ORANGE[2 if y < 9 else 1])
                else:
                    c.px(x, y, ACORN[2 if x % 3 else 1])
    c.px(7, 8, rgb("f6e08a"))
    outline(c, rgb("0c1a0c"))
    return c.img


def pie_item():
    """A squash pie seen from above at an angle: golden crust rim and a smooth orange filling."""
    filling = [rgb("b8621c"), rgb("d27e26"), rgb("e39a3a")]
    c = Canvas()
    for y in range(4, 14):
        for x in range(1, 16):
            d = (x - 8) ** 2 / 49 + (y - 9) ** 2 / 20
            if d <= 1:
                rim = d > 0.6
                c.px(x, y, CRUST[3 if y < 9 else 2] if rim else filling[2 if (x * 3 + y) % 7 else 1])
    for x in range(2, 15):
        c.px(x, 13, CRUST[1])
    for x, y in ((6, 7), (10, 8), (8, 10)):
        c.px(x, y, filling[0])
    c.px(7, 8, rgb("fbf2dc"))
    c.px(8, 8, rgb("fbf2dc"))
    outline(c, rgb("3a2008"))
    return c.img


def candy_corn_item():
    """Three pieces of candy corn: a white tip, an orange middle and a wide yellow base."""
    c = Canvas()
    rows = [(0, 0), (-1, 0), (-1, 1), (-2, 1), (-2, 2), (-2, 2)]
    for cx, top in ((4, 2), (10, 5), (6, 9)):
        for i, (left, right) in enumerate(rows):
            color = CANDY[0] if i < 2 else CANDY[1] if i < 4 else CANDY[2]
            for x in range(cx + left, cx + right + 1):
                c.px(x, top + i, color)
    outline(c, rgb("5a2a06"))
    return c.img


def festival_textures():
    """(kind, name) -> image for every festival-crop texture."""
    out = {
        ("block", "butternut_squash_side"): gourd_side(BUTTERNUT, 0, 1, stripes=[BUTTERNUT[3], BUTTERNUT[4]]),
        ("block", "butternut_squash_top"): gourd_top(BUTTERNUT, 0, STALK[1]),
        ("block", "acorn_squash_side"): gourd_side(ACORN, 3, 2, spot=ACORN_ORANGE),
        ("block", "acorn_squash_top"): gourd_top(ACORN, 8, STALK[1]),
        ("block", "warty_gourd_side"): gourd_side(WARTY, 0, 3, warts=22, stripes=WARTY_GREEN),
        ("block", "warty_gourd_top"): gourd_top(WARTY, 6, WARTY_GREEN[0]),
        ("block", "gourd_stem"): gourd_stem(),
        ("block", "gourd_stem_attached"): gourd_stem_attached(),
        ("block", "gourd_stalk"): gourd_stalk(),
        ("block", "chestnut_sapling"): sapling(),
        ("block", "turnip_lantern_side"): lantern_side(False),
        ("block", "turnip_lantern_face"): lantern_side(True),
        ("block", "turnip_lantern_top"): lantern_top(),
        ("item", "butternut_squash_seeds"): seeds_item([rgb("d8c49a"), rgb("ece0c0"), rgb("fbf4e2")],
                                                       [(3, 4), (8, 3), (11, 7), (5, 9), (10, 12), (3, 13)], size=(3, 1)),
        ("item", "acorn_squash_seeds"): seeds_item([rgb("cdb886"), rgb("e4d4a6"), rgb("f6eccc")],
                                                   [(4, 4), (9, 5), (6, 9), (11, 10), (3, 12), (8, 13)], size=(2, 1)),
        ("item", "warty_gourd_seeds"): seeds_item([rgb("a88a4a"), rgb("c8aa66"), rgb("e2c88a")],
                                                  [(3, 5), (8, 4), (12, 7), (5, 10), (10, 11), (6, 13)], size=(2, 1)),
        ("item", "turnip"): turnip_item(),
        ("item", "cranberries"): cranberries_item(),
        ("item", "chestnut"): chestnut_item(),
        ("item", "roasted_chestnuts"): chestnut_item(roasted=True),
        ("item", "baked_acorn_squash"): acorn_half_item(),
        ("item", "squash_pie"): pie_item(),
        ("item", "candy_corn"): candy_corn_item(),
        ("item", "butternut_squash_soup"): bowl_item([rgb("b8641a"), rgb("d8842a"), rgb("e8a040"), rgb("f2c060")],
                                                     [(rgb("f6ecd6"), [(6, 6), (7, 6), (9, 7)]), (CABBAGE[3], [(10, 6)])]),
        ("item", "harvest_stew"): bowl_item(BOWL_BROWN, [(TURNIP_WHITE[3], [(5, 6), (10, 7)]), (TURNIP_PURPLE[3], [(6, 7)]),
                                                         (rgb("e07a2a"), [(8, 6), (11, 6)]), (ONION[3], [(4, 7)])]),
        ("item", "cranberry_sauce"): bowl_item(CRANBERRY[:4], [(CRANBERRY[4], [(5, 6), (9, 6)]), (CRANBERRY[3], [(7, 7), (11, 7)])]),
    }
    for stage in range(4):
        out[("block", f"turnip_stage{stage}")] = turnip_stage(stage)
        out[("block", f"cranberry_stage{stage}")] = cranberry_stage(stage)
    return out
