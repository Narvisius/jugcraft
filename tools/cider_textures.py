"""Original textures for the cider mill (fall additions 2) (requires Pillow): the apple tree's leaves (plain, in blossom,
hung with ripe apples) and sapling; the Cider Press's dark oak, slatted basket and iron; the Cider Barrel's staves, its
head with a chalk mark for each stage, and its brass tap; for the press's renderer, the ground apple pulp, the juice and
an apple's skin; and the items: apple seeds, pomace, four ciders, mulling spices and an apple cider donut.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. The block textures are 16x16; the leaves are see-through between the leaves (as vanilla's
are), the rest opaque.
"""
import math

from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import put
from decor13_textures import bottle, icon

LEAF = [rgb("1e3a14"), rgb("2a5018"), rgb("366620"), rgb("447c28"), rgb("5a9634"), rgb("74ae44")]
BLOSSOM = [rgb("f8f4f0"), rgb("f2d6de"), rgb("e8a6bc"), rgb("e8d040")]
APPLE = [rgb("6a0e0e"), rgb("9a1a14"), rgb("c42a1e"), rgb("e65a3a"), rgb("ffb08a")]
STEM = rgb("5a3a1a")
OAK = [rgb("3e2814"), rgb("54361c"), rgb("6a4624"), rgb("82582e")]
SLAT = [rgb("2a1a0c"), rgb("6e4a26"), rgb("8a5e30"), rgb("a2733c")]
IRON = [rgb("26262a"), rgb("38383e"), rgb("4a4a52"), rgb("6a6a74")]
STAVE = [rgb("4a2e14"), rgb("6a4220"), rgb("7e5028"), rgb("946032")]
BRASS = [rgb("8a6a22"), rgb("b0882e"), rgb("d0a840"), rgb("f0d070")]
CHALK = rgb("ece8e0")
PULP = [rgb("b89a5a"), rgb("d0b470"), rgb("e2ca8a"), rgb("a8442a")]
JUICE = [rgb("b0741c"), rgb("c88a26"), rgb("dca23a"), rgb("f0c46a")]


def leaves(fruit=0):
    """Apple leaves in clumps, as vanilla leaves: each clump lit along its top and shaded along its foot, with gaps
    between; fruit 1 adds white-and-pink blossom, 2 ripe red apples."""
    c = Canvas()
    grain = bs.grain(16, 16, 26101, 1.6, 4.0)
    for y in range(16):
        for x in range(16):
            g = grain(x, y)
            if g < 0.3:
                c.img.putpixel((x, y), LEAF[0] + (0,))  # a gap
            elif grain(x, (y - 1) % 16) < 0.3:
                c.px(x, y, LEAF[4])                      # lit along the top of a clump
            elif grain(x, (y + 1) % 16) < 0.3:
                c.px(x, y, LEAF[1])                      # shaded along its foot
            else:
                c.px(x, y, LEAF[3] if g > 0.6 else LEAF[2])
    if fruit == 1:
        for cx, cy in ((3, 4), (11, 3), (7, 9), (13, 12), (3, 13)):
            for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                c.px(cx + dx, cy + dy, BLOSSOM[0] if dy <= 0 else BLOSSOM[1])
            c.px(cx - 1, cy - 1, BLOSSOM[1])
            c.px(cx + 1, cy + 1, BLOSSOM[2])
            c.px(cx, cy, BLOSSOM[3])
    elif fruit == 2:
        for cx, cy in ((4, 5), (11, 3), (9, 11), (2, 12)):
            c.px(cx, cy - 2, STEM)
            for dx in range(-1, 2):
                for dy in range(-1, 2):
                    c.px(cx + dx, cy + dy, APPLE[2] if dx + dy < 1 else APPLE[1])
            c.px(cx - 1, cy - 1, APPLE[4] if (cx + cy) % 2 else APPLE[3])
            c.px(cx + 1, cy + 1, APPLE[0])
    return c.img


def sapling():
    """A slim sapling with a few round leaves and a white blossom."""
    c = Canvas()
    c.line(8, 15, 8, 6, STAVE[1])
    c.line(8, 10, 11, 7, STAVE[1])
    for cx, cy in ((8, 4), (5, 6), (11, 6), (6, 9), (10, 10), (12, 4)):
        for dx in range(-1, 2):
            for dy in range(-1, 1):
                c.px(cx + dx, cy + dy, LEAF[4] if dy < 0 else LEAF[3])
        c.px(cx, cy + 1, LEAF[2])
    c.px(9, 3, BLOSSOM[0])
    c.px(10, 3, BLOSSOM[1])
    c.px(9, 2, BLOSSOM[0])
    c.px(10, 2, BLOSSOM[3])
    return c.img


def press_wood():
    """Dark oak boards with a grain, two to a face."""
    c = Canvas()
    bs.planks(OAK, 26102, boards=2)(c)
    return c.img


def press_slats():
    """Upright oak slats with dark gaps between them, where the juice runs out."""
    c = Canvas()
    bs.planks(SLAT, 26104, vertical=True, joint=False)(c)
    return c.img


def press_iron():
    """Dark iron with a few rivets."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 26105, [1, 3, 2])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (8, 8)):
        c.px(x, y, IRON[3])
        c.px(x + 1, y + 1, IRON[0])
    return c.img


def staves(across):
    """Barrel staves running along the barrel: down the texture for its top and bottom (`across` False), along it for its
    sides."""
    c = Canvas()
    bs.planks(STAVE, 26106 + across, vertical=not across, joint=False)(c)
    return c.img


def head(mark):
    """The barrel's round head: boards inside a dark rim, and a chalk mark: none, one stroke (sweet), two (sparkling) or
    three (aged)."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.6:
                c.px(x, y, STAVE[0])
            elif d > 5.6:
                c.px(x, y, STAVE[1])
            else:
                c.px(x, y, STAVE[2] if (y // 4) % 2 else STAVE[3])
    for y in (4, 8, 12):
        for x in range(3, 13):
            if math.hypot(x - 7.5, y - 7.5) < 5.6:
                c.px(x, y, STAVE[1])
    for i in range(mark):
        for y in range(5, 10):
            c.px(8 - mark + 2 * i, y, CHALK)
    return c.img


def brass():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BRASS[1:4], 26107, [2, 3, 1])
    return c.img


def pulp():
    """Ground apple: cream and tan, flecked with red skin."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, PULP[:3], 26108, [2, 3, 2])
    for y in range(1, 16, 4):
        for x in range((y // 4) % 2 * 2 + 1, 16, 4):
            c.px(x, y, PULP[3])  # flecks of red skin, set evenly
            c.px(x + 1, y, PULP[3])
    return c.img


def juice():
    """Fresh apple juice: cloudy amber with a few highlights."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, JUICE[:3], 26110, [2, 3, 2])
    for x, y in ((3, 4), (4, 4), (11, 9), (12, 9), (7, 13)):
        c.px(x, y, JUICE[3])
    return c.img


def apple_skin():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, APPLE[1:4], 26111, [2, 3, 1])
    for x, y in ((3, 3), (4, 3), (3, 4), (11, 10)):
        c.px(x, y, APPLE[4])
    return c.img


SEEDS = [
    "................",
    "................",
    "................",
    "......DD........",
    ".....DSSD.......",
    ".....DSSD...DD..",
    "......DSD..DSSD.",
    "...........DSSD.",
    "....DD......DSD.",
    "...DSSD.........",
    "...DSSD...DD....",
    "....DSD..DSSD...",
    ".........DSSD...",
    "..........DSD...",
    "................",
    "................"]

POMACE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......PPRP......",
    "....PPLPPPPR....",
    "...PLPPRPPLPP...",
    "..PPPLPPPPRPPP..",
    "..RPPPPLPPPPLP..",
    "..PPLPPPRPPPPP..",
    "...PPPPPPLPPR...",
    "....DDDDDDDDD...",
    "................",
    "................",
    "................"]

SPICES = [
    "................",
    "................",
    "..CC............",
    "..CcC...........",
    "...CcC.....S....",
    "....CcC...SsS...",
    ".....CcC.SsSsS..",
    "......CcC.SsS...",
    ".......CcC.S....",
    "........CC......",
    "...O.......BB...",
    "..OoO.....BbbB..",
    "...O......BbbB..",
    "...........BB...",
    "................",
    "................"]

DONUT = [
    "................",
    "................",
    "................",
    ".....WDDDDW.....",
    "....DDDWDDDD....",
    "...DDWDDDDWDD...",
    "..DDDDD..DDDDD..",
    "..DWDD....DDWD..",
    "..DDDD....DDDD..",
    "..DDWDD..DDDWD..",
    "...DDDDDWDDDD...",
    "....LDDDDDDL....",
    ".....LLLLLL.....",
    "................",
    "................",
    "................"]


def seeds_item():
    return icon(SEEDS, {"D": rgb("2a160a"), "S": rgb("6a3c1a")})


def pomace_item():
    return icon(POMACE, {"P": PULP[1], "L": PULP[2], "R": PULP[3], "D": PULP[0]})


def spices_item():
    """Cinnamon sticks, a star anise, a clove and a nutmeg."""
    return icon(SPICES, {"C": rgb("6a3a1a"), "c": rgb("a0603a"), "S": rgb("4a2a14"), "s": rgb("8a5a2a"), "O": rgb("3a1e0e"),
                         "o": rgb("6a3a1a"), "B": rgb("7a4a24"), "b": rgb("a8784a")})


def donut_item():
    """A cake donut rolled in cinnamon sugar."""
    return icon(DONUT, {"D": rgb("b87a3e"), "W": rgb("f4e8d0"), "L": rgb("8a5426")})


def cider_item(stage):
    """A bottle of cider: pale gold (sweet), golden with bubbles (sparkling), deep amber (aged), or dark mulled cider with a
    cinnamon stick."""
    liquid, top, shine = {"sweet": (rgb("e0b450"), rgb("c8a26a"), rgb("f4d690")),
                          "sparkling": (rgb("e8b832"), rgb("c8a26a"), rgb("fff0a0")),
                          "aged": (rgb("b06a1a"), rgb("6a4424"), rgb("d8943a")),
                          "mulled": (rgb("7a2a14"), rgb("6a4424"), rgb("b4502a"))}[stage]
    img = bottle(liquid, top, shine)
    if stage == "sparkling":
        for x, y in ((6, 9), (8, 7), (9, 11), (10, 9), (7, 12)):
            put(img, x, y, rgb("fff8d8"))
    elif stage == "mulled":
        for i in range(5):
            put(img, 10 + i // 2, 1 + i, rgb("a0603a"))
    return img


def cider_textures():
    """(kind, name) -> image for every texture of the cider mill."""
    out = {
        ("block", "apple_leaves"): leaves(0),
        ("block", "apple_leaves_blossom"): leaves(1),
        ("block", "apple_leaves_ripe"): leaves(2),
        ("block", "apple_sapling"): sapling(),
        ("block", "cider_press_wood"): press_wood(),
        ("block", "cider_press_slats"): press_slats(),
        ("block", "cider_press_iron"): press_iron(),
        ("block", "cider_barrel_staves"): staves(False),
        ("block", "cider_barrel_staves_side"): staves(True),
        ("block", "cider_barrel_brass"): brass(),
        ("entity", "cider_pulp"): pulp(),
        ("entity", "cider_juice"): juice(),
        ("entity", "cider_apple"): apple_skin(),
        ("item", "apple_seeds"): seeds_item(),
        ("item", "apple_pomace"): pomace_item(),
        ("item", "mulling_spices"): spices_item(),
        ("item", "apple_cider_donut"): donut_item(),
    }
    for mark, name in enumerate(("cider_barrel_head", "cider_barrel_head_sweet", "cider_barrel_head_sparkling", "cider_barrel_head_aged")):
        out[("block", name)] = head(mark)
    for stage in ("sweet", "sparkling", "aged", "mulled"):
        out[("item", f"{stage}_cider")] = cider_item(stage)
    return out
