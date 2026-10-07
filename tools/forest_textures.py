"""Original 16x16 textures for the biomes branch's seasonal-forest trees (requires Pillow): the saplings of maple,
aspen and fir (agriculture.TREES).

Called from crop_textures.crop_textures(), after the larch's. Every pixel is drawn here by code from fixed seeds; no
Mojang or other mod's texture is read, traced or recolored. The woods and leaves of these trees (and of the dead
wood) are drawn with every other tree's by wood_style.py.
"""
import random

from crop_textures import Canvas, rgb


def pal(*hexes):
    return [rgb(h) for h in hexes]


WOODS = {
    "maple": {"bark": pal("3a3330", "4d4540", "615751", "776b63", "8c8077"),
              "plank": pal("8a5a3c", "a3704d", "bb8661", "cf9c76", "e0b38d"),
              "heart": pal("8a4f30", "a2623c", "b9774c", "cf8f60"),
              "sap": pal("c9a27c", "d8b48f", "e4c4a1", "eed3b3")},
    "aspen": {"bark": pal("4a443d", "a9a597", "c9c5b8", "dedace", "f0eee6"),
              "plank": pal("b69f74", "c9b48a", "d8c59c", "e5d4ae", "efe1c0"),
              "heart": pal("b19566", "c2a777", "d1b98a", "dec99d"),
              "sap": pal("dccfae", "e6dbbe", "eee5cc", "f5eedb")},
    "fir": {"bark": pal("2e2621", "40352d", "54473c", "685849", "7c6a57"),
            "plank": pal("9a7646", "b08a55", "c39d66", "d3af79", "e0c08c"),
            "heart": pal("9d6d3c", "b07f49", "c19158", "d0a269"),
            "sap": pal("d2b688", "dcc298", "e5cea8", "ecd9b7")},
    "dead": {"bark": pal("33302d", "4a4642", "615c56", "79736b", "908a81"),
             "plank": pal("6f695f", "857e72", "9a9285", "aea698", "c0b9ab"),
             "heart": pal("6a645b", "7b746a", "8b8479", "9b9488"),
             "sap": pal("a49d90", "b1aa9d", "bdb7aa", "c8c2b6")},
}

LEAVES = {
    "maple": pal("223f12", "2e5418", "3e6c20", "52872b", "6ba13a", "8cbc55"),
    "maple_red": pal("4e0d0a", "721510", "992018", "be3220", "dc512b", "ef7a3c"),
    "maple_orange": pal("6e2a07", "96400b", "ba5911", "d8761b", "ec9530", "f6b656"),
    "maple_gold": pal("6e4c07", "94690e", "b98c17", "d6ad27", "ebc943", "f6e07a"),
    "aspen": pal("27501a", "356a21", "47852b", "5ea137", "7dbc4e", "a2d571"),
    "aspen_gold": pal("7a5d0a", "a27d10", "c69d1a", "e2bd2e", "f2d752", "faea8c"),
    "fir": pal("0d261c", "143525", "1d4530", "27563c", "33684a", "457d5c"),
}


def leaf_shape(kind, c, x0, y0, palette, rng):
    """One leaf (or spray) centred at x0, y0, wrapping round the tile."""
    def put(dx, dy, shade):
        c.px((x0 + dx) % 16, (y0 + dy) % 16, palette[shade])
    if kind == "maple":
        # A five-pointed maple leaf: a centre with three upper lobes and two side lobes.
        for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, 1), (0, -1)):
            put(dx, dy, 3)
        for dx, dy in ((0, -2), (-2, -1), (2, -1), (-2, 1), (2, 1)):
            put(dx, dy, 4)
        put(0, 2, 1)
        put(-1, -1, 4 if rng.random() < 0.5 else 3)
        put(1, -1, 5 if rng.random() < 0.3 else 4)
    elif kind == "aspen":
        # Small round leaves that flutter: a lit side and a shaded side.
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            put(dx, dy, 4 if dx == 0 and dy == 0 else 3)
        put(1, 1, 2)
        put(-1, 0, 2 if rng.random() < 0.5 else 3)
    else:
        # A flat fir spray: a stem with short needles to both sides.
        for i in range(-2, 3):
            put(i, 0, 2)
            put(i, -1, 3 if i % 2 else 4)
            put(i, 1, 1 if i % 2 else 3)


def sapling(tree):
    c = Canvas()
    stem = WOODS[tree]["bark"][2 if tree != "aspen" else 3]
    c.line(8, 15, 8, 5, stem)
    if tree == "fir":
        green = LEAVES["fir"]
        for y, half in ((5, 1), (7, 2), (9, 3), (11, 4), (13, 4)):
            for dx in range(-half, half + 1):
                c.px(8 + dx, y, green[4] if dx % 2 else green[3])
                c.px(8 + dx, y + 1, green[2])
        c.px(8, 3, green[5])
        c.px(8, 4, green[4])
        return c.img
    green = LEAVES[tree]
    c.line(8, 10, 5, 7, stem)
    c.line(8, 8, 11, 5, stem)
    rng = random.Random(79)
    for x0, y0 in ((5, 5), (11, 4), (8, 3), (4, 9), (12, 8)):
        leaf_shape(tree, c, x0, y0, green, rng)
    return c.img


def forest_textures():
    """(kind, name) -> image for every seasonal-forest tree texture."""
    out = {}
    for tree in ("maple", "aspen", "fir"):
        out[("block", f"{tree}_sapling")] = sapling(tree)
    return out
