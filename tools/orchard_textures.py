"""Original textures for the orchards (tools/orchard.py) (requires Pillow): the pear, peach, lemon and orange trees' leaves
(plain, in blossom, hung with ripe fruit) and saplings; the four fruits, their seeds (the peach's pit) and the two juices.
The owner chose this slice in Jugcraft's own art (7 October 2026), their library having no fruit trees.

The leaves are painted as every Jugcraft tree's are (tools/wood_style.py paint_leaves: vanilla's speckle of small leaves
in four tones, see-through between them), each tree in its own green, then blossom or fruit is drawn over them: pear
blossom white with dark red anthers, peach blossom pink, lemon and orange blossom white; pears yellow-green and pear-shaped,
peaches orange with a red blush, lemons yellow with a nib, oranges round and deep orange. The juices are the shared bottle
(tools/decor13_textures.py) every Jugcraft drink is drawn in; the set-down juices' models wear the same icons
(block/menu/<juice>, tools/menu_data.py).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured.
"""
from crop_textures import Canvas, rgb, outline
from decor9_textures import put
from decor13_textures import bottle
from wood_style import paint_leaves, pal
import orchard

# Each tree's leaves, six tones dark to light (tools/wood_style.py LEAVES): the pear a fresh mid green, the peach a
# yellower green, the lemon and orange a dark glossy evergreen (the lemon a touch yellower).
LEAVES = {
    "pear": pal("1c3a16", "27501d", "356a26", "458431", "5a9e3e", "79b856"),
    "peach": pal("26401a", "345a22", "46752c", "5a8f36", "74a846", "96c262"),
    "lemon": pal("12301a", "1a4222", "24552b", "2f6936", "3e7e42", "549856"),
    "orange": pal("0f2c18", "163c20", "1f4e2a", "2a6236", "377644", "4c8e5a"),
    # The fruit crops (tools/fruit_crops.py): the plum a cool, deep green; the banana's fronds a bright yellow-green.
    "plum": pal("162e1a", "1f4024", "2a552e", "386c3a", "4a844a", "649e5e"),
    "banana": pal("2a4a12", "3a6418", "4e801e", "649c26", "7cb432", "a0cc4a"),
}
SEEDS = {tree: 900 + i for i, tree in enumerate(orchard.TREES)}

# Blossom: (petal light, petal shade, centre).
BLOSSOM = {
    "pear": (rgb("faf8f2"), rgb("e4e0d6"), rgb("8a2a3a")),
    "peach": (rgb("f8b4c8"), rgb("e07a9c"), rgb("b23a66")),
    "lemon": (rgb("f8f4fa"), rgb("dccae6"), rgb("e8d040")),
    "orange": (rgb("fdfaf2"), rgb("e8e2d2"), rgb("f0cc3a")),
    "plum": (rgb("fbf6f8"), rgb("e4d6de"), rgb("d8b03a")),
    # The banana's flower: deep maroon bracts round a pale tip.
    "banana": (rgb("a83a5e"), rgb("6e2440"), rgb("f0d890")),
}
FLOWERS = [(3, 2), (10, 3), (6, 7), (13, 8), (2, 11), (9, 12)]

# Fruit tones: dark, mid, light, highlight.
FRUIT = {
    "pear": [rgb("6e7a1a"), rgb("a8b42e"), rgb("d0d452"), rgb("eeeca0")],
    "peach": [rgb("a8442a"), rgb("e27a3a"), rgb("f6a45c"), rgb("ffd4a2")],
    "lemon": [rgb("c09a0e"), rgb("ecca1c"), rgb("fbe44a"), rgb("fff8b0")],
    "orange": [rgb("b4520a"), rgb("e8821a"), rgb("fca43a"), rgb("ffd08a")],
    "plum": [rgb("2e0e30"), rgb("561c52"), rgb("7e2e76"), rgb("b484ae")],
    "banana": [rgb("a07a0c"), rgb("e0b81a"), rgb("f6d846"), rgb("fff0a0")],
}
BLUSH = rgb("cc3a30")
STEM = rgb("4a2a12")
CALYX = rgb("3a6a20")
SPOTS = [(4, 3), (11, 4), (7, 9), (13, 12), (3, 12)]

# Each fruit hanging in the leaves, as (dx, dy, tone) about its spot: tone 0-3 into FRUIT, "s" the stem, "c" a calyx, "b"
# the peach's blush.
HANGING = {
    "pear": [(0, -1, "s"), (0, 0, 2), (-1, 1, 2), (0, 1, 1), (1, 1, 1), (-1, 2, 3), (0, 2, 1), (1, 2, 0), (0, 3, 0)],
    "peach": [(0, -1, "s"), (-1, 0, 2), (0, 0, 2), (1, 0, 1), (-1, 1, 3), (0, 1, 1), (1, 1, "b"), (-1, 2, 1), (0, 2, 0),
              (1, 2, "b")],
    "lemon": [(0, -1, "s"), (-1, 0, 2), (0, 0, 2), (1, 0, 1), (-1, 1, 3), (0, 1, 1), (1, 1, 0), (0, 2, 0)],
    "orange": [(0, -1, "c"), (-1, 0, 2), (0, 0, 3), (1, 0, 1), (-1, 1, 2), (0, 1, 1), (1, 1, 0), (-1, 2, 1), (0, 2, 0),
               (1, 2, 0)],
    "plum": [(0, -1, "s"), (-1, 0, 2), (0, 0, 3), (1, 0, 1), (-1, 1, 2), (0, 1, 1), (1, 1, 0), (0, 2, 0)],
    # A hand of bananas: a stalk, then fingers fanning out, lit on top, green at their tips.
    "banana": [(0, -1, "s"), (-1, 0, 2), (0, 0, 3), (1, 0, 2), (-2, 1, 2), (-1, 1, 1), (0, 1, 2), (1, 1, 1), (2, 1, 2),
               (-2, 2, 1), (-1, 2, 0), (1, 2, 0), (2, 2, 1), (-2, 3, "c"), (2, 3, "c")],
}


def base(tree):
    """The tree's leaves, as every Jugcraft tree's are painted; the banana's as fronds, the palms' way."""
    return paint_leaves(LEAVES[tree], "fronds" if tree == "banana" else "leaves", SEEDS[tree])


def blossom(tree):
    """The leaves in flower: five-petalled blossoms, lit on their upper petals, round a coloured centre."""
    c = Canvas()
    c.img = base(tree)
    light, shade, centre = BLOSSOM[tree]
    for x, y in FLOWERS:
        for dx, dy in ((0, -1), (-1, 0)):
            c.px(x + dx, y + dy, light)
        for dx, dy in ((1, 0), (0, 1)):
            c.px(x + dx, y + dy, shade)
        c.px(x, y, centre)
    return c.img


def ripe(tree):
    """The leaves hung with ripe fruit."""
    c = Canvas()
    c.img = base(tree)
    tones = FRUIT[tree]
    for x, y in SPOTS:
        for dx, dy, tone in HANGING[tree]:
            colour = STEM if tone == "s" else CALYX if tone == "c" else BLUSH if tone == "b" else tones[tone]
            c.px(x + dx, y + dy, colour)
    return c.img


def banana_sapling():
    """A young banana plant: a short green stem and four broad leaves, two arching out to each side, ribbed down the
    middle."""
    p = LEAVES["banana"]
    stem = rgb("6a7a2a")
    c = Canvas()
    c.line(7, 15, 7, 7, stem)
    c.line(8, 15, 8, 8, rgb("4e5a1e"))
    for sign, top in ((-1, 4), (1, 3), (-1, 8), (1, 7)):
        for i in range(6):
            x = 8 + sign * (1 + i) if sign > 0 else 7 + sign * (1 + i)
            y = top + (i * i) // 6
            c.px(x, y - 1, p[5] if i < 3 else p[4])
            c.px(x, y, p[3])
            c.px(x, y + 1, p[2] if i < 5 else p[1])
    for y in range(2, 6):
        c.px(7 + (y % 2), y, p[4])
    return c.img


def sapling(tree):
    """A slim sapling: a stem with a side shoot and a few small leaves in the tree's green; the pear, peach and plum in
    blossom, the lemon and orange with a first small fruit; the banana a young plant of its own."""
    if tree == "banana":
        return banana_sapling()
    p = LEAVES[tree]
    c = Canvas()
    c.line(8, 15, 8, 6, rgb("5a3a1e"))
    c.line(8, 11, 11, 8, rgb("5a3a1e"))
    c.line(8, 9, 5, 7, rgb("5a3a1e"))
    for cx, cy in ((8, 4), (5, 6), (11, 7), (6, 10), (10, 11), (12, 5)):
        for dx in range(-1, 2):
            c.px(cx + dx, cy - 1, p[5])
            c.px(cx + dx, cy, p[4] if dx < 1 else p[3])
        c.px(cx, cy + 1, p[2])
    if tree in ("pear", "peach", "plum"):
        light, shade, centre = BLOSSOM[tree]
        for x, y in ((9, 2), (4, 4)):
            c.px(x, y - 1, light)
            c.px(x - 1, y, light)
            c.px(x + 1, y, shade)
            c.px(x, y + 1, shade)
            c.px(x, y, centre)
    else:
        tones = FRUIT[tree]
        c.px(11, 9, tones[2])
        c.px(12, 9, tones[1])
        c.px(11, 10, tones[1])
        c.px(12, 10, tones[0])
    return c.img


# ---------------------------------------------------------------- items

def fruit_item(tree):
    """The fruit as an item: a pear (narrow above, round below), a peach (blushed, with its crease), a lemon (an oval with a
    nib at each end), an orange (round, dimpled) or a plum (an oval with its crease and a bloomy light), each with a leaf;
    a banana (a curved yellow finger)."""
    tones = FRUIT[tree]
    leaf = LEAVES[tree]
    c = Canvas()

    def disc(cx, cy, rx, ry):
        for y in range(16):
            for x in range(16):
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
                if d <= 1:
                    lit = (x - cx) + (y - cy)
                    c.px(x, y, tones[2] if lit < -2 else tones[1] if lit < 2 else tones[0])

    if tree == "pear":
        disc(7.5, 10, 4.2, 3.8)
        disc(7.5, 5.5, 2.4, 2.8)
        c.line(7, 2, 8, 0, STEM)
        c.px(6, 8, tones[3])
        c.px(5, 9, tones[3])
    elif tree == "peach":
        disc(7.5, 8.5, 5.2, 5.0)
        for y in range(5, 14):
            for x in range(9, 13):
                if c.get(x, y) and (x - 11) ** 2 + (y - 9) ** 2 <= 6:
                    c.px(x, y, BLUSH)
        c.line(7, 4, 6, 12, tones[0])
        c.px(5, 6, tones[3])
        c.px(4, 7, tones[3])
        c.px(8, 3, STEM)
    elif tree == "lemon":
        disc(7.5, 8.5, 5.6, 4.0)
        c.px(2, 8, tones[1])
        c.px(1, 9, tones[0])
        c.px(13, 8, tones[0])
        c.px(14, 7, tones[0])
        c.px(5, 6, tones[3])
        c.px(6, 6, tones[3])
    elif tree == "plum":
        disc(7.5, 8.5, 4.4, 5.0)
        c.line(9, 4, 10, 12, tones[0])
        for x, y in ((5, 6), (4, 7), (5, 8)):
            c.px(x, y, tones[3])
        c.px(8, 3, STEM)
    elif tree == "banana":
        # A curved finger of banana, its brown stalk up at the right and its dark tip down at the left.
        for i in range(12):
            t = i / 11
            cx, cy = 2.5 + 10 * t, 12.5 - 9 * t * t
            for dy in range(-1, 2):
                c.px(cx, cy + dy, tones[3] if dy < 0 and i < 9 else tones[2] if dy < 1 else tones[1])
            c.px(cx, cy + 2, tones[0])
        c.px(13, 2, STEM)
        c.px(13, 1, STEM)
        c.px(2, 12, rgb("3a2a12"))
        c.px(2, 13, rgb("3a2a12"))
        outline(c, rgb("2a1a0e"))
        return c.img
    else:
        disc(7.5, 8.5, 5.0, 5.0)
        for x, y in ((6, 9), (9, 7), (10, 10), (5, 11), (8, 12)):
            c.px(x, y, tones[0])
        c.px(5, 6, tones[3])
        c.px(4, 7, tones[3])
        c.px(7, 3, CALYX)
        c.px(8, 3, CALYX)
    # A leaf at the stem, up and to the right.
    for x, y, tone in ((9, 2, 4), (10, 2, 4), (10, 1, 5), (11, 1, 4), (12, 1, 3), (11, 2, 2)):
        if c.empty(x, y):
            c.px(x, y, leaf[tone])
    outline(c, rgb("2a1a0e"))
    return c.img


def banana_pup():
    """A banana pup: the young shoot cut from beside a banana plant, its pale corm and roots below and two rolled leaves
    above."""
    p = LEAVES["banana"]
    c = Canvas()
    corm, root = rgb("c8b47a"), rgb("8a6a3a")
    for y in range(10, 15):
        for x in range(5, 11):
            if ((x - 7.5) / 3.0) ** 2 + ((y - 12) / 2.6) ** 2 <= 1:
                c.px(x, y, corm if x + y < 21 else rgb("a8945a"))
    for x, y in ((5, 15), (7, 15), (10, 15), (4, 14), (11, 14)):
        c.px(x, y, root)
    for y in range(2, 10):
        c.px(7, y, p[4] if y < 6 else p[3])
        c.px(8, y, p[2])
    for x, y in ((6, 3), (5, 2), (9, 4), (10, 3), (11, 2)):
        c.px(x, y, p[5])
    outline(c, rgb("2a1a0e"))
    return c.img


def seeds_item(tree):
    """The seeds: three pear pips (dark brown teardrops), a peach or plum pit (a wrinkled brown stone, the plum's smaller and
    smoother), three citrus pips (the lemon's pale cream, the orange's warmer), or a banana pup."""
    if tree == "banana":
        return banana_pup()
    c = Canvas()
    if tree in ("peach", "plum"):
        dark, mid, light = (rgb("5a2e16"), rgb("8a4a24"), rgb("b06a36")) if tree == "peach" else (rgb("6a4a2a"), rgb("9a7244"), rgb("c49a64"))
        rx, ry = (4.0, 5.5) if tree == "peach" else (3.2, 4.6)
        for y in range(16):
            for x in range(16):
                if ((x - 7.5) / rx) ** 2 + ((y - 8) / ry) ** 2 <= 1:
                    c.px(x, y, light if x + y < 13 else mid if x + y < 18 else dark)
        for x, y in ((6, 5), (8, 6), (7, 8), (9, 9), (6, 10), (8, 11)) if tree == "peach" else ((7, 6), (8, 9), (7, 11)):
            c.px(x, y, dark)
        c.px(7, 2 if tree == "peach" else 3, mid)
        outline(c, rgb("2a160a"))
        return c.img
    dark, light = {"pear": (rgb("2a160a"), rgb("6a3c1a")), "lemon": (rgb("b8a878"), rgb("f0e6c4")),
                   "orange": (rgb("c49a5a"), rgb("f2d89a"))}[tree]
    pips = ((5, 4), (10, 7), (4, 10)) if tree == "orange" else ((4, 3), (10, 5), (5, 10))
    for ox, oy in pips:
        c.px(ox, oy, dark)
        c.px(ox - 1, oy + 1, dark)
        c.px(ox, oy + 1, light)
        c.px(ox + 1, oy + 1, dark)
        c.px(ox - 1, oy + 2, dark)
        c.px(ox, oy + 2, light)
        c.px(ox + 1, oy + 2, light)
        c.px(ox, oy + 3, dark)
    return c.img


def juice(name):
    """A bottle of juice: orange juice, cloudy orange with a slice on the rim; lemonade, pale with a lemon slice."""
    if name == "orange_juice":
        img = bottle(rgb("f0921e"), rgb("c8a26a"), rgb("ffc46a"))
        slice_tones = FRUIT["orange"]
    else:
        img = bottle(rgb("f4e48a"), rgb("c8a26a"), rgb("fffad0"))
        slice_tones = FRUIT["lemon"]
    for x, y, tone in ((11, 2, 1), (12, 2, 2), (13, 2, 1), (11, 3, 2), (12, 3, 3), (13, 3, 2), (12, 4, 1)):
        put(img, x, y, slice_tones[tone])
    return img


def dish_icon(name):
    """The icon a set-down juice's model is fitted to (tools/menu_data.py icon())."""
    return juice(name)


def banana_stem(top):
    """The banana's stem: its side soft green-brown sheaths in long vertical streaks, scarred where old leaves fell; its end
    rings of pale green sheath round a creamy heart."""
    c = Canvas()
    tones = [rgb("4a4a1e"), rgb("5e6024"), rgb("74782e"), rgb("8a8c3a"), rgb("a0a24a")]
    if top:
        for y in range(16):
            for x in range(16):
                r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                ring = int(r)
                c.px(x, y, rgb("e8e0b0") if r < 2 else rgb("c8c890") if r < 3 else tones[4 - ring % 3] if r < 7.2 else tones[0])
        return c.img
    for x in range(16):
        streak = [2, 3, 2, 1, 2, 3, 4, 3, 2, 1, 2, 3, 2, 3, 1, 2][x]
        for y in range(16):
            tone = streak
            if (y + 3 * (x // 4)) % 11 == 0:
                tone = 0
            elif (x * 7 + y * 3) % 13 == 0:
                tone = min(4, tone + 1)
            c.px(x, y, tones[tone])
    return c.img


def orchard_textures():
    """(kind, name) -> image for every texture of the orchards."""
    out = {}
    out[("block", orchard.BANANA_STEM["block"])] = banana_stem(False)
    out[("block", f"{orchard.BANANA_STEM['block']}_top")] = banana_stem(True)
    for tree in orchard.TREES:
        out[("block", orchard.leaves(tree))] = base(tree)
        out[("block", f"{orchard.leaves(tree)}_blossom")] = blossom(tree)
        out[("block", f"{orchard.leaves(tree)}_ripe")] = ripe(tree)
        out[("block", orchard.sapling(tree))] = sapling(tree)
        out[("item", tree)] = fruit_item(tree)
        out[("item", orchard.TREES[tree]["seed"])] = seeds_item(tree)
    for name in orchard.DISHES:
        out[("item", name)] = juice(name)
        out[("block", f"menu/{name}")] = juice(name)
    return out
