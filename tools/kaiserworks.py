"""Kaiserworks (batch 48, docs/features/kaiserworks.md): imperial building blocks to dress the dieselpunk set.

Black lacquered iron with gilt trim and gold rivets, polished brass, a gilt key-pattern frieze, a crest of our own
made-up empire (a winged cog on a crimson shield, no real nation's arms), fluted marble and banded black iron columns,
polished marble and black-and-cream station tiles, a see-through wrought-iron lattice, leaded glass and a brass gas
lamp with a warm white flame. Every texture is drawn here.

Java: building/Kaiserworks.java registers the blocks; tools/check_mod_data.py keeps the list and strengths the same.
The models, blockstates, loot and recipes are written by tools/dieselworks.write_blocks. Every recipe makes blocks from
plates, ingots, nuggets, stone or glass; nothing turns a block back into metal.
"""
import math
import random

from PIL import Image

import block_style as bs

MOD = "jugcraft"

# id: (display name, kind, hardness, blast resistance); the kinds are those of tools/dieselworks.py, plus "crest", a
# cube with a front that faces the player who places it.
BLOCKS = {
    "black_lacquer_plate": ("Black Lacquer Plate", "family", 5.0, 6.0),
    "riveted_black_plate": ("Riveted Black Plate", "family", 5.0, 6.0),
    "gilt_trimmed_plate": ("Gilt-Trimmed Plate", "family", 5.0, 6.0),
    "polished_brass_plate": ("Polished Brass Plate", "family", 5.0, 6.0),
    "gilt_frieze": ("Gilt Frieze", "full", 5.0, 6.0),
    "imperial_crest": ("Imperial Crest", "crest", 5.0, 6.0),
    "fluted_marble_column": ("Fluted Marble Column", "pillar", 2.0, 6.0),
    "black_iron_column": ("Black Iron Column", "pillar", 5.0, 6.0),
    "polished_marble": ("Polished Marble", "family", 2.0, 6.0),
    "station_tiles": ("Station Tiles", "family", 2.0, 6.0),
    "wrought_iron_lattice": ("Wrought Iron Lattice", "grating", 3.0, 6.0),
    "leaded_glass": ("Leaded Glass", "glass", 1.0, 3.0),
    "imperial_gas_lamp": ("Imperial Gas Lamp", "lamp", 1.5, 3.0),
}
TEXTURES = {
    "black_lacquer_plate": "ik_lacquer",
    "riveted_black_plate": "ik_lacquer_riveted",
    "gilt_trimmed_plate": "ik_gilt_trim",
    "polished_brass_plate": "ik_brass",
    "gilt_frieze": "ik_frieze",
    "fluted_marble_column": ("ik_marble_fluted", "ik_marble_end"),
    "black_iron_column": ("ik_iron_column", "ik_lacquer_riveted"),
    "polished_marble": "ik_marble",
    "station_tiles": "ik_tiles",
    "wrought_iron_lattice": "ik_lattice",
    "leaded_glass": "ik_leaded",
}
CREST_TEXTURES = {"imperial_crest": ("ik_crest", "ik_gilt_trim")}
LAMP_TEXTURES = {"imperial_gas_lamp": {"metal": "ik_brass", "bar": "ik_lacquer", "glass": "ik_gaslight"}}
LAMP_LIGHT = 15
TOOLTIPS = {
    "imperial_crest": "The arms of no real empire: a winged cog on a crimson shield. Faces you when placed.",
    "wrought_iron_lattice": "See-through: railings, screens and grand station roofs.",
    "imperial_gas_lamp": "Burns with a bright, warm white flame.",
}

BL = f"{MOD}:black_lacquer_plate"
PM = f"{MOD}:polished_marble"
# (recipe name, result, pattern, key, count). One plate of metal makes one block; trims add nuggets or a dye.
RECIPES = [
    ("black_lacquer_plate", "black_lacquer_plate", ["PPP", "PDP", "PPP"], {"P": "#c:plates/iron", "D": "minecraft:black_dye"}, 8),
    ("riveted_black_plate", "riveted_black_plate", [" B ", "BNB", " B "], {"B": BL, "N": "minecraft:gold_nugget"}, 4),
    ("gilt_trimmed_plate", "gilt_trimmed_plate", ["NBN", "B B", "NBN"], {"B": BL, "N": "minecraft:gold_nugget"}, 4),
    ("polished_brass_plate", "polished_brass_plate", ["PP", "PP"], {"P": "#c:plates/brass"}, 4),
    ("gilt_frieze", "gilt_frieze", ["NNN", "BBB", "NNN"], {"B": BL, "N": "minecraft:gold_nugget"}, 3),
    ("imperial_crest", "imperial_crest", ["NGN", "BRB", "NBN"],
     {"B": BL, "N": "minecraft:gold_nugget", "G": "minecraft:gold_ingot", "R": "minecraft:red_dye"}, 2),
    ("polished_marble", "polished_marble", ["CC", "CC"], {"C": "minecraft:calcite"}, 4),
    ("fluted_marble_column", "fluted_marble_column", ["M", "N", "M"], {"M": PM, "N": "#c:nuggets/brass"}, 2),
    ("black_iron_column", "black_iron_column", ["B", "N", "B"], {"B": BL, "N": "#c:nuggets/brass"}, 2),
    ("station_tiles", "station_tiles", ["MB", "BM"], {"M": PM, "B": BL}, 4),
    ("wrought_iron_lattice", "wrought_iron_lattice", ["BDB", "B B"], {"B": "minecraft:iron_bars", "D": "minecraft:black_dye"}, 4),
    ("leaded_glass", "leaded_glass", ["GLG", "LGL", "GLG"], {"G": "minecraft:glass", "L": "#c:plates/lead"}, 5),
    ("imperial_gas_lamp", "imperial_gas_lamp", ["NBN", "BGB", "NPN"],
     {"N": "#c:nuggets/brass", "B": "minecraft:iron_bars", "G": "minecraft:glowstone", "P": "#c:plates/brass"}, 2),
]


def blocks():
    out = []
    for block, (_, kind, _, _) in BLOCKS.items():
        out.append(block)
        if kind == "family":
            out += [f"{block}_slab", f"{block}_stairs"]
        elif kind == "grating":
            out.append(f"{block}_slab")
    return out


def write_all(write, assets, data, lang, condition, self_drop):
    import sys
    from dieselworks import write_blocks
    write_blocks(write, assets, data, lang, condition, self_drop, sys.modules[__name__])


# ------------------------------------------------------------------ art

BLACK = [(12, 12, 16), (20, 20, 26), (30, 30, 38), (44, 44, 54), (66, 66, 80)]
GOLD = [(96, 66, 18), (146, 104, 30), (196, 150, 48), (232, 194, 88), (252, 230, 150)]
BRASS = [(104, 78, 34), (146, 112, 52), (184, 148, 74), (214, 182, 104), (240, 216, 150)]
MARBLE = [(176, 168, 152), (204, 198, 182), (222, 216, 202), (236, 232, 220), (246, 244, 236)]
CRIMSON = [(70, 10, 14), (108, 18, 24), (142, 28, 32), (176, 44, 44)]


def _img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c + (255,))


def lacquer(seed=4801):
    """Black lacquered iron: deep black with a soft diagonal sheen and faint panel seams. (`seed` is unused since the
    lacquer became a clean, noise-free fill.)"""
    img = _img()
    for y in range(16):
        for x in range(16):
            band = (x + y) % 16
            c = BLACK[2] if 3 <= band <= 5 else BLACK[1]
            if band == 4:
                c = BLACK[3]
            if x in (0, 15) or y in (0, 15):
                c = BLACK[0]
            _put(img, x, y, c)
    return img


def lacquer_riveted():
    """Black plate with gold rivets at the corners and edge middles."""
    img = lacquer(4802)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 2), (7, 13), (2, 7), (13, 7)):
        _put(img, x, y, GOLD[3])
        _put(img, x + 1, y, GOLD[2])
        _put(img, x, y + 1, GOLD[1])
        _put(img, x + 1, y + 1, GOLD[1])
    return img


def gilt_trim():
    """Black plate inside a gilt border with a dark inner line."""
    img = lacquer(4803)
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            _put(img, x, y, GOLD[1])
        for x, y in ((i, 1), (i, 14), (1, i), (14, i)):
            if 1 <= i <= 14:
                _put(img, x, y, GOLD[3] if i % 4 else GOLD[4])
        for x, y in ((i, 2), (i, 13), (2, i), (13, i)):
            if 2 <= i <= 13:
                _put(img, x, y, GOLD[0])
    return img


def brass():
    """Polished brass: warm, banded with a bright highlight, and fine dark seams between panels."""
    img = _img()
    for y in range(16):
        for x in range(16):
            shade = 2 + round(math.sin((y + x * 0.25) / 16 * math.pi * 2) * 1.2)
            c = BRASS[max(0, min(4, shade))]
            if y in (0, 8) or x == 0:
                c = BRASS[0]
            _put(img, x, y, c)
    return img


def frieze():
    """A gilt Greek-key band across black, with gold edge lines."""
    img = lacquer(4805)
    for x in range(16):
        for y in (1, 14):
            _put(img, x, y, GOLD[2])
    key = ["xxxxxxx.",
           "x.....x.",
           "x.xxx.x.",
           "x.x.x.x.",
           "x.x...x.",
           "x.xxxxx.",
           "x.......",
           "xxxxxxxx"]
    for gy, row in enumerate(key):
        for gx, ch in enumerate(row):
            if ch == "x":
                for repeat in (0, 8):
                    _put(img, gx + repeat, gy + 4, GOLD[3] if (gx + gy) % 3 else GOLD[4])
    return img


CREST = ["gggggggggggggggg",
         "g..............g",
         "gY............Yg",
         "gGY.GGGGGGGG.YGg",
         "g.GYGrrrrrrGYG.g",
         "gY.GGrRYYRrGG.Yg",
         "gGYGGrYrrYrGGYGg",
         "g.GYGrYrrYrGYG.g",
         "g..GGrRYYRrGG..g",
         "g...GrrrrrrG...g",
         "g....GrRRrG....g",
         "g.....GrrG.....g",
         "g......GG......g",
         "g..............g",
         "g..Y.Y.YY.Y.Y..g",
         "gggggggggggggggg"]


def crest():
    """The crest of our own empire, on black inside a gilt border: a crimson shield bearing a gold cog, with gold
    wings spread on either side."""
    colours = {"g": GOLD[1], "G": GOLD[2], "Y": GOLD[4], "r": CRIMSON[1], "R": CRIMSON[3]}
    img = lacquer(4811)
    for y, row in enumerate(CREST):
        for x, ch in enumerate(row):
            if ch in colours:
                _put(img, x, y, colours[ch])
    return img


def marble(seed=4806):
    """Polished cream marble in the manner of vanilla calcite: one ground tone with soft clumps a tone either side and
    a single smooth grey vein wandering down it."""
    rng = random.Random(seed)
    img = _img()
    s = bs.surface(MARBLE[2:5], seed, weights=[1, 3, 1], spread=0.7)
    for y in range(16):
        for x in range(16):
            _put(img, x, y, s(x, y))
    x = rng.randint(0, 15)
    for y in range(16):
        if y % 3 == 0:
            x = (x + rng.choice([-1, 1])) % 16
        _put(img, x, y, MARBLE[1])
        if y % 5 == 2:
            _put(img, x, y, MARBLE[0])
    return img


def marble_fluted():
    """A fluted marble column side: shadowed vertical grooves between bright ribs."""
    img = marble(4807)
    for y in range(16):
        for x in range(16):
            phase = x % 4
            if phase == 0:
                _put(img, x, y, MARBLE[0])
            elif phase == 1:
                _put(img, x, y, MARBLE[1])
            elif phase == 3:
                _put(img, x, y, MARBLE[4])
    return img


def marble_end():
    """The column's top: marble inside a brass ring."""
    img = marble(4808)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 6.0 <= r <= 7.4:
                _put(img, x, y, BRASS[2 if r < 6.7 else 1])
    return img


def iron_column():
    """A black iron column: lacquered staves with gold-banded brass collars at the top and bottom."""
    img = lacquer(4809)
    for y in range(16):
        for x in range(0, 16, 4):
            _put(img, x, y, BLACK[0])
            _put(img, x + 2, y, BLACK[3])
    for y in (1, 2, 13, 14):
        for x in range(16):
            _put(img, x, y, BRASS[3 if y in (1, 13) else 1])
    return img


def tiles():
    """Black and cream station tiles: a 2x2 checker with fine brass grout lines, the black tiles flat with a lit top
    left edge, the cream ones clumped marble."""
    img = _img()
    cream = bs.surface(MARBLE[2:5], 4810, weights=[1, 3, 1], spread=0.6)
    for y in range(16):
        for x in range(16):
            dark = (x // 8 + y // 8) % 2 == 1
            if x % 8 == 0 or y % 8 == 0:
                c = BRASS[1]
            elif dark:
                c = BLACK[3] if x % 8 == 1 or y % 8 == 1 else BLACK[2]
            else:
                c = cream(x, y)
            _put(img, x, y, c)
    return img


def lattice():
    """Wrought-iron lattice: a black frame with diamond bars and scroll curls, see-through between."""
    img = _img()
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            _put(img, x, y, BLACK[2])
    for i in range(16):
        for x, y in ((i, i), (i, 15 - i)):
            _put(img, x, y, BLACK[3])
    for cx, cy in ((7.5, 3), (7.5, 12), (3, 7.5), (12, 7.5)):
        for angle in range(0, 360, 30):
            x = round(cx + 1.6 * math.cos(math.radians(angle)))
            y = round(cy + 1.6 * math.sin(math.radians(angle)))
            _put(img, x, y, BLACK[3])
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        _put(img, x, y, GOLD[2])
    return img


def leaded():
    """Leaded glass: clear diamonds held by black leading, gold-rimmed, a faint tint on alternate panes."""
    img = _img()
    for y in range(16):
        for x in range(16):
            u, v = (x + y) % 8, (x - y) % 8
            if u == 0 or v == 0:
                _put(img, x, y, BLACK[2])
            elif ((x + y) // 8 + (x - y) // 8) % 2:
                img.putpixel((x, y), (214, 196, 140, 70))
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            _put(img, x, y, GOLD[1])
    for x, y in ((4, 3), (3, 4), (11, 10)):
        img.putpixel((x, y), (250, 250, 240, 200))
    return img


def gaslight():
    """The gas lamp's glass: a warm white glow brightest at the mantle."""
    img = _img()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 8.5)
            t = max(0.0, 1.0 - r / 9.0)
            _put(img, x, y, (round(236 + 19 * t), round(208 + 44 * t), round(150 + 90 * t)))
    return img


def draw_all(save):
    for name, img in (("ik_lacquer", lacquer()), ("ik_lacquer_riveted", lacquer_riveted()), ("ik_gilt_trim", gilt_trim()),
                      ("ik_brass", brass()), ("ik_frieze", frieze()), ("ik_crest", crest()), ("ik_marble", marble()),
                      ("ik_marble_fluted", marble_fluted()), ("ik_marble_end", marble_end()),
                      ("ik_iron_column", iron_column()), ("ik_tiles", tiles()), ("ik_lattice", lattice()),
                      ("ik_leaded", leaded()), ("ik_gaslight", gaslight())):
        save(img, "block", name)
