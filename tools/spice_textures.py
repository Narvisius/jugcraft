"""Original 16x16 textures for the spices (tools/spices.py) (requires Pillow): the black pepper and vanilla vines climbing
their trellises, ginger, mustard and the saffron crocus in four stages each, their wild plants, the Cinnamon Tree's
sapling and leaves (its logs are drawn with the woods, by tools/wood_style.py), the Spice Rack, and the spices and spice
dishes as items. The owner chose Jugcraft's own art for these (9 October 2026).

Crop textures are drawn for the "crop" model and the vines' for the trellis crop model, as the vegetables' are
(tools/vegetable_textures.py). Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang
texture is read, traced or recoloured.
"""
import math
import random

from crop_textures import Canvas, rgb, broad_leaf, stalk, outline, seeds_item
from kitchen_textures import bowl_item
from vegetable_textures import trellis_vine, STEM, YELLOW
import spices

PEPPER_LEAF = [rgb("163a14"), rgb("204e1c"), rgb("2c6426"), rgb("3c7c32"), rgb("52963e"), rgb("70b056")]
PEPPERCORN_GREEN = [rgb("3a6a1a"), rgb("4e8422"), rgb("6aa030")]
PEPPERCORN_RED = [rgb("6a0e0e"), rgb("9e1a14"), rgb("d0321e")]
PEPPERCORN_BLACK = [rgb("1a1412"), rgb("2e2420"), rgb("4a3a32")]
VANILLA_LEAF = [rgb("2a4a14"), rgb("3a621c"), rgb("4e7c26"), rgb("669832"), rgb("82b246"), rgb("a2ca66")]
VANILLA_FLOWER = [rgb("c8b860"), rgb("e8dc8a"), rgb("fbf4c0")]
VANILLA_POD = [rgb("3a4a14"), rgb("566a1e"), rgb("7a8e2c")]
VANILLA_CURED = [rgb("2a1a10"), rgb("40281a"), rgb("5a3a26")]
GINGER_LEAF = [rgb("2a5a1a"), rgb("3a7224"), rgb("4e8c30"), rgb("66a63e"), rgb("84be56"), rgb("a8d478")]
GINGER_ROOT = [rgb("8a6a3a"), rgb("b08a50"), rgb("d0aa6a"), rgb("e8c88a")]
MUSTARD_LEAF = [rgb("2a5418"), rgb("3a6c20"), rgb("4e862c"), rgb("68a03a"), rgb("86b852"), rgb("a8d070")]
MUSTARD_SEED = [rgb("8a5a1a"), rgb("b07a26"), rgb("d09e3a"), rgb("e8c060")]
CROCUS = [rgb("4a2470"), rgb("6a3a9a"), rgb("8e5ac0"), rgb("b48ae0"), rgb("d8bcf4")]
SAFFRON = [rgb("8a1a0a"), rgb("c0300e"), rgb("e85a1a")]
CROCUS_LEAF = [rgb("2a4e1e"), rgb("3a6628"), rgb("4e8034"), rgb("68a046")]
BARK = [rgb("3a1e12"), rgb("54301c"), rgb("6e4228"), rgb("8a5636"), rgb("a66e46")]
INNER = [rgb("8a4a1e"), rgb("a8602a"), rgb("c47a3a"), rgb("dc9a54"), rgb("ecb874")]
CINNAMON_LEAF = [rgb("163418"), rgb("1e4620"), rgb("2a5a2a"), rgb("3a7036"), rgb("4e8846"), rgb("6aa25e")]
NEW_LEAF = [rgb("8a3a2a"), rgb("b05a3a"), rgb("d07a52")]
WOOD = [rgb("4a3218"), rgb("6a4826"), rgb("8a6234"), rgb("a87c46"), rgb("c49a60")]
CHILI_DRIED = [rgb("4a0e0a"), rgb("6e1a10"), rgb("8e2a18"), rgb("aa3e22")]
PAPRIKA = [rgb("8a1e0e"), rgb("b02e14"), rgb("d0461e"), rgb("e86a32")]


# ---------------------------------------------------------------- the vines

def heart_leaf(c, x, y, size, palette):
    """A glossy heart-shaped leaf hanging from the vine (black pepper)."""
    for dy in range(0, size + 1):
        half = size - dy * size / (size + 1)
        for dx in range(-int(half), int(half) + 1):
            c.px(x + dx, y + dy, palette[4 if dy == 0 else 3 if dx < 0 else 2])
    c.px(x, y + size + 1, palette[1])


def catkin(c, x, y, length, tones):
    """A hanging spike of peppercorns, berries in pairs down it."""
    c.px(x, y - 1, STEM[2])
    for k in range(length):
        c.px(x, y + k, tones[2] if k % 2 == 0 else tones[1])
        if k % 2 == 0:
            c.px(x + 1, y + k, tones[1])


def peppercorn_texture(name):
    c = Canvas()
    stage = int(name[-1])
    if name.startswith("peppercorn_stage"):
        if stage == 0:
            stalk(c, 7, 13, palette=STEM)
            heart_leaf(c, 6, 11, 1, PEPPER_LEAF)
            heart_leaf(c, 9, 11, 1, PEPPER_LEAF)
        else:
            trellis_vine(c, 15 - (6 if stage == 1 else 10))
            for x, y, size in ((5, 11, 2), (10, 9, 2), (6, 6, 1), (10, 13, 1))[: 2 + stage]:
                heart_leaf(c, x, y, size, PEPPER_LEAF)
        return c.img
    top = "_top_" in name
    trellis_vine(c, 2 if top else 0, 15)
    spots = ((4, 2, 2), (11, 4, 2), (4, 9, 2), (11, 10, 2)) if not top else ((4, 6, 2), (11, 8, 2), (7, 3, 1))
    for x, y, size in spots[: 3 if stage == 0 else len(spots)]:
        heart_leaf(c, x, y, size, PEPPER_LEAF)
    if stage == 2:
        # Flowering: thin pale spikes.
        for x, y in ((8, 6), (3, 13)) if not top else ((10, 4),):
            catkin(c, x, y, 3, [rgb("c8d898"), rgb("e0ecb8"), rgb("f0f8d8")])
    if stage >= 3:
        tones = PEPPERCORN_RED if stage == 4 else PEPPERCORN_GREEN
        for x, y in ((8, 6), (2, 12), (13, 7)) if not top else ((9, 4), (3, 11)):
            catkin(c, x, y, 5 if stage == 4 else 4, tones)
    return c.img


def vanilla_leaf(c, x, y, direction, palette):
    """A thick, fleshy oblong leaf, held out level from the vine (vanilla orchid)."""
    for i in range(4):
        c.px(x + direction * i, y, palette[4])
        c.px(x + direction * i, y + 1, palette[2])
    c.px(x + direction * 4, y, palette[3])


def vanilla_pod(c, x, y, length, cured=False):
    tones = VANILLA_CURED if cured else VANILLA_POD
    for k in range(length):
        c.px(x, y + k, tones[2] if k % 3 == 0 else tones[1])
    c.px(x, y - 1, STEM[2])


def vanilla_texture(name):
    c = Canvas()
    stage = int(name[-1])
    if name.startswith("vanilla_stage"):
        if stage == 0:
            stalk(c, 7, 13, palette=STEM)
            vanilla_leaf(c, 6, 11, -1, VANILLA_LEAF)
        else:
            trellis_vine(c, 15 - (6 if stage == 1 else 10))
            for x, y, d in ((6, 12, -1), (9, 9, 1), (6, 6, -1))[: 1 + stage]:
                vanilla_leaf(c, x, y, d, VANILLA_LEAF)
        return c.img
    top = "_top_" in name
    trellis_vine(c, 2 if top else 0, 15)
    leaves = ((6, 3, -1), (9, 6, 1), (6, 10, -1), (9, 13, 1)) if not top else ((6, 6, -1), (9, 10, 1), (6, 13, -1))
    for x, y, d in leaves[: 2 if stage == 0 else len(leaves)]:
        vanilla_leaf(c, x, y, d, VANILLA_LEAF)
    if stage == 2:
        for x, y in ((12, 4), (2, 9)) if not top else ((12, 8),):
            c.px(x, y, VANILLA_FLOWER[2])
            c.px(x + 1, y, VANILLA_FLOWER[1])
            c.px(x, y + 1, VANILLA_FLOWER[1])
            c.px(x - 1, y, VANILLA_FLOWER[0])
            c.px(x, y - 1, rgb("e8a832"))
    if stage >= 3:
        for x, y in ((12, 4), (3, 9), (13, 9)) if not top else ((12, 8), (3, 2)):
            vanilla_pod(c, x, y, 5 if stage == 4 else 3)
    return c.img


def wild_vine(leaf_painter, fruit):
    """A vine sprawled over the ground under its leaves, a little of its fruit showing."""
    c = Canvas()
    c.line(1, 14, 14, 12, STEM[1])
    c.line(4, 13, 7, 7, STEM[1])
    c.line(10, 12, 11, 8, STEM[1])
    leaf_painter(c)
    fruit(c)
    return c.img


def wild_peppercorns():
    def leaves(c):
        for x, y, size in ((3, 10, 2), (7, 7, 2), (11, 8, 2), (13, 11, 1)):
            heart_leaf(c, x, y, size, PEPPER_LEAF)

    def fruit(c):
        catkin(c, 9, 10, 4, PEPPERCORN_RED)
        catkin(c, 5, 12, 3, PEPPERCORN_GREEN)
    return wild_vine(leaves, fruit)


def wild_vanilla():
    def leaves(c):
        for x, y, d in ((4, 11, -1), (8, 8, 1), (12, 10, 1), (6, 6, -1)):
            vanilla_leaf(c, x, y, d, VANILLA_LEAF)

    def fruit(c):
        vanilla_pod(c, 10, 11, 4)
        c.px(5, 9, VANILLA_FLOWER[2])
        c.px(6, 9, VANILLA_FLOWER[1])
    return wild_vine(leaves, fruit)


# ---------------------------------------------------------------- ginger, mustard and saffron

def ginger_stage(stage):
    """Reedy stems with long narrow leaves held out alternately; the knobbly rhizome shows at the soil when grown."""
    c = Canvas()
    height = (4, 8, 11, 13)[stage]
    for x in ((7,), (6, 9), (5, 8, 11), (5, 8, 11))[stage]:
        for y in range(15 - height, 16):
            c.px(x, y, GINGER_LEAF[2])
        for k, y in enumerate(range(15 - height + 1, 14, 2)):
            d = -1 if k % 2 else 1
            for i in range(1, 4):
                c.px(x + d * i, y + i // 2, GINGER_LEAF[4 if i < 3 else 3])
    if stage == 3:
        for x, y in ((4, 14), (6, 15), (9, 14), (11, 15), (7, 14)):
            c.px(x, y, GINGER_ROOT[2])
            c.px(x + 1, y, GINGER_ROOT[1])
    return c.img


def mustard_stage(stage):
    """A leafy rosette that bolts into tall, branching stalks of small yellow flowers, with slim seed pods below them when
    grown (stage 3)."""
    c = Canvas()
    for x, y, d in ((5, 13, -1), (10, 13, 1), (6, 11, -1), (9, 11, 1))[: 2 + min(stage, 2)]:
        broad_leaf(c, x, y, d, 1 + (stage >= 2), MUSTARD_LEAF)
    if stage >= 2:
        height = 10 if stage == 2 else 13
        for x in (5, 8, 11):
            c.line(7.5, 13, x, 15 - height, STEM[2])
        for x in (5, 8, 11):
            for dx, dy in ((0, 0), (1, 0), (-1, 1), (0, 1), (1, 2)):
                c.px(x + dx, 15 - height + dy, YELLOW[2 + (dx + dy) % 2])
        if stage == 3:
            for x, y in ((4, 6), (12, 7), (7, 8), (9, 5)):
                c.px(x, y, MUSTARD_SEED[1])
                c.px(x, y + 1, MUSTARD_SEED[0])
    return c.img


def saffron_stage(stage):
    """Grass-fine leaves; a purple crocus bud (stage 2) opening to a cup with three red threads hanging out (stage 3)."""
    c = Canvas()
    rng = random.Random(f"saffron{stage}")
    height = (4, 7, 9, 10)[stage]
    for i in range((3, 5, 7, 8)[stage]):
        x0 = 7.5 + (i - 3.5) * 0.5
        lean = (i - 3.5) * 0.6
        h = height - rng.choice((0, 1, 2))
        for k in range(h):
            c.px(x0 + lean * k / h, 15 - k, CROCUS_LEAF[2 + (k > h - 3)])
    if stage == 2:
        for x in (6, 10):
            for dy in range(3):
                c.px(x, 8 + dy, CROCUS[2 + (dy == 0)])
                c.px(x + 1, 8 + dy, CROCUS[1])
    if stage == 3:
        for cx in (5, 10):
            for dy in range(4):
                half = 1 if dy in (0, 3) else 2
                for dx in range(-half, half + 1):
                    c.px(cx + dx, 5 + dy, CROCUS[4 if dx < 0 else 3 if dx == 0 else 2] if dy < 3 else CROCUS[1])
            c.px(cx, 4, SAFFRON[2])
            c.px(cx - 1, 3, SAFFRON[1])
            c.px(cx + 1, 3, SAFFRON[1])
            c.px(cx, 6, YELLOW[2])
    return c.img


# ---------------------------------------------------------------- the cinnamon tree and the spice rack

def cinnamon_leaves():
    """Glossy, dark leaves with a few reddish new ones among them, as a cinnamon tree flushes."""
    c = Canvas()
    rng = random.Random("cinnamon_leaves")
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.82:
                tone = rng.choice((1, 2, 2, 3, 3, 4))
                c.px(x, y, CINNAMON_LEAF[tone])
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        c.px(x, y, CINNAMON_LEAF[5])
        c.px(x + 1, y, CINNAMON_LEAF[4])
    for _ in range(5):
        x, y = rng.randrange(15), rng.randrange(15)
        c.px(x, y, NEW_LEAF[2])
        c.px(x + 1, y, NEW_LEAF[1])
        c.px(x, y + 1, NEW_LEAF[0])
    return c.img


def cinnamon_sapling():
    c = Canvas()
    stalk(c, 7, 4, palette=[BARK[1], BARK[2], BARK[3], BARK[4]], nodes=(6, 10))
    for x, y, d in ((6, 6, -1), (9, 5, 1), (6, 10, -1), (9, 9, 1), (8, 3, 1)):
        broad_leaf(c, x, y, d, 2, CINNAMON_LEAF)
    c.px(8, 2, NEW_LEAF[2])
    c.px(9, 2, NEW_LEAF[1])
    return c.img


def spice_rack():
    """The rack's wood: a plank back with two shelves, read by its model (tools/spice_data.py)."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, WOOD[3 if (y // 4) % 2 else 2] if y % 4 else WOOD[1])
    return c.img


# ---------------------------------------------------------------- items

def spice_jar(contents, cap=WOOD):
    """A little glass spice jar with a cork, holding `contents` (three tones)."""
    c = Canvas()
    glass = [rgb("6a8a8a"), rgb("a8c8c8"), rgb("e0f0f0")]
    for y in range(6, 15):
        for x in range(4, 12):
            edge = x in (4, 11) or y == 14
            c.px(x, y, glass[1] if edge else contents[(x + y) % 3 if y > 7 else 2])
    for y in range(4, 6):
        for x in range(5, 11):
            c.px(x, y, cap[3 if x < 8 else 2])
    c.px(5, 8, glass[2])
    c.px(5, 9, glass[2])
    outline(c, rgb("1e2a2a"))
    return c.img


def peppercorns_item():
    rng = random.Random("peppercorns")
    c = Canvas()
    for _ in range(12):
        x, y = rng.randrange(3, 12), rng.randrange(4, 13)
        tones = PEPPERCORN_BLACK if rng.random() < 0.7 else PEPPERCORN_RED
        c.px(x, y, tones[2])
        c.px(x + 1, y, tones[1])
        c.px(x, y + 1, tones[1])
        c.px(x + 1, y + 1, tones[0])
    outline(c, rgb("0e0a08"))
    return c.img


def vanilla_pods_item():
    c = Canvas()
    for i in range(3):
        c.line(3 + i * 2, 14, 10 + i * 2, 2, VANILLA_CURED[1 + (i == 1)])
    c.line(11, 3, 13, 1, VANILLA_POD[2])
    outline(c, rgb("140c06"))
    return c.img


def ginger_item():
    c = Canvas()
    for x, y, r in ((6, 9, 3), (10, 7, 2), (4, 12, 2), (11, 11, 2)):
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if math.hypot(dx, dy) <= r:
                    c.px(x + dx, y + dy, GINGER_ROOT[3 if dx + dy < -1 else 2 if dx + dy < 2 else 1])
    for x, y in ((6, 8), (10, 6), (4, 11)):
        c.px(x, y, GINGER_ROOT[0])
    outline(c, rgb("4a3418"))
    return c.img


def crocus_bulb_item():
    c = Canvas()
    for dy in range(-4, 4):
        half = 4 - abs(dy + 1) * 0.8
        for dx in range(-int(half), int(half) + 1):
            c.px(8 + dx, 10 + dy, rgb("c8a878") if dx < 0 else rgb("a88858"))
    c.line(8, 5, 9, 2, CROCUS_LEAF[2])
    for x in range(5, 12):
        c.px(x, 14, rgb("e8dcc0"))
    outline(c, rgb("4a3420"))
    return c.img


def saffron_item():
    c = Canvas()
    for i, (x0, y0) in enumerate(((4, 13), (7, 13), (10, 13))):
        c.line(x0, y0, x0 + 2 - i, 3 + i, SAFFRON[1 + i % 2])
        c.px(x0 + 2 - i, 3 + i, SAFFRON[2])
    outline(c, rgb("3a0a04"))
    return c.img


def cinnamon_item():
    """Two curled quills of cinnamon bark, crossed."""
    c = Canvas()
    for x0, y0, x1, y1 in ((3, 12, 12, 3), (5, 13, 13, 6)):
        c.line(x0, y0, x1, y1, INNER[1])
        c.line(x0 + 1, y0, x1 + 1, y1, INNER[3])
        c.px(x1, y1, INNER[4])
        c.px(x1 + 1, y1 - 1, INNER[0])
    outline(c, rgb("3a1a08"))
    return c.img


def dried_chili_item():
    c = Canvas()
    for k in range(10):
        x, y = 4 + k, 4 + k * 0.8
        c.px(x, y, CHILI_DRIED[2 + (k % 3 == 0)])
        c.px(x, y + 1, CHILI_DRIED[1])
        if k > 7:
            c.px(x, y, CHILI_DRIED[1])
    c.px(3, 3, STEM[0])
    c.px(2, 2, rgb("6a5a2a"))
    outline(c, rgb("1e0604"))
    return c.img


def gingerbread_cookie_item():
    """A little gingerbread figure with white icing."""
    c = Canvas()
    dough = [rgb("6a3a14"), rgb("8a5020"), rgb("a8682e"), rgb("c48440")]
    for dy in range(-2, 2):
        for dx in range(-2, 3):
            if math.hypot(dx, dy) <= 2.2:
                c.px(8 + dx, 4 + dy, dough[2 if dx + dy < 0 else 1])
    for y in range(6, 12):
        for x in range(5, 11):
            c.px(x, y, dough[2 if x < 8 else 1])
    for x in range(2, 14):
        if x < 5 or x > 10:
            c.px(x, 7, dough[2])
            c.px(x, 8, dough[1])
    for x, y in ((5, 12), (5, 13), (6, 13), (10, 12), (10, 13), (9, 13)):
        c.px(x, y, dough[1])
    for x, y in ((7, 3), (9, 3), (8, 8), (8, 10), (3, 7), (12, 7)):
        c.px(x, y, rgb("fffef8"))
    outline(c, rgb("2a1406"))
    return c.img


def spice_textures():
    out = {}
    for names in spices.TALL_CROPS["peppercorn"]["textures"]:
        for name in names:
            out[("block", name)] = peppercorn_texture(name)
    for names in spices.TALL_CROPS["vanilla"]["textures"]:
        for name in names:
            out[("block", name)] = vanilla_texture(name)
    for stage in range(4):
        out[("block", f"ginger_stage{stage}")] = ginger_stage(stage)
        out[("block", f"mustard_stage{stage}")] = mustard_stage(stage)
        out[("block", f"saffron_stage{stage}")] = saffron_stage(stage)
    out[("block", "wild_peppercorns")] = wild_peppercorns()
    out[("block", "wild_vanilla")] = wild_vanilla()
    out[("block", spices.CINNAMON["leaves"])] = cinnamon_leaves()
    out[("block", spices.CINNAMON["sapling"])] = cinnamon_sapling()
    out[("block", "spice_rack")] = spice_rack()
    out.update({
        ("item", "peppercorns"): peppercorns_item(),
        ("item", "vanilla_pods"): vanilla_pods_item(),
        ("item", "ginger"): ginger_item(),
        ("item", "mustard_seeds"): seeds_item(MUSTARD_SEED, [(4, 4), (8, 3), (11, 5), (5, 8), (9, 8), (12, 10), (4, 12), (8, 12)],
                                              size=(1, 1)),
        ("item", "crocus_bulb"): crocus_bulb_item(),
        ("item", "saffron"): saffron_item(),
        ("item", "cinnamon"): cinnamon_item(),
        ("item", "dried_chili"): dried_chili_item(),
        ("item", "paprika"): spice_jar(PAPRIKA),
        ("item", "gingerbread_cookie"): gingerbread_cookie_item(),
        ("item", "chicken_curry"): bowl_item([rgb("b8701e"), rgb("d08a2a"), rgb("e8a840")],
                                             bits=[(rgb("f4ecd8"), [(5, 6), (9, 5)]), (rgb("8a3a12"), [(7, 5), (11, 6)]),
                                                   (GINGER_LEAF[3], [(10, 4)])]),
        ("item", "saffron_rice"): bowl_item([rgb("d8a020"), rgb("eec030"), rgb("fadc60")],
                                            bits=[(SAFFRON[1], [(6, 5), (10, 6)]), (rgb("fff6d0"), [(8, 5), (5, 6)])]),
        ("item", "vanilla_custard"): bowl_item([rgb("e8d080"), rgb("f4e2a0"), rgb("fff2c8")],
                                               bits=[(VANILLA_CURED[1], [(6, 5), (10, 6)])]),
    })
    return out
