"""Textures for the remodelled placed meals (tools/menu_meal_models.py), drawn here by code at vanilla's 16 x 16
density in the owner's own palettes: every food colour below is taken from their library drawing of the same food
(art/owner-library/originals/Blocks/farming and food textures: the meals' icons and cooked_bacon.png, fried_egg.png,
cornbread.png, raw_pasta.png, cooked_mutton_chops.png, beef_patty.png, tomato.png, cabbage_leaf.png; the woods from
bowl.png and platter.png). No library file is read or changed here; the colours are written out as hex.

Each meal wears two textures, as the owner's shepherd's pie does (shepherds_pie_top, shepherds_pie_side):
- <dish>_top: the dish seen from above, one texel a model pixel, read by position by every upward face. It is drawn
  from the model's own pieces (menu_meal_models.pieces), lowest first: each piece's material (its tile, or one tone),
  its edges lit on the north and west and shaded on the south and east where nothing of its kind and height adjoins
  (light from the top left, as the owner draws), then the meal's DETAILS grid on top (grill marks, glints, herbs).
  So the drawing always fits the geometry it is read by.
- <dish>_side: each material's rows seen from the side (MATERIALS[...]["side"], top row first), stacked in the order the
  materials are listed; side faces read them top-aligned.
The dishware is shared: the wooden plate and bowl in the owner's woods, the glazed plate in the cream and blue of
Jugcraft's own Plate (tools/feasts_textures.py, its whites kept under 235). The skillet under the stuffing is the
owner's Skillet (block/skillet_*, imported by tools/owner_art.py) and is not drawn here.

Called from tools/generate_textures.py; `python3 tools/menu_meal_textures.py` writes these textures alone.
All are 16 x 16 and opaque.
"""
import math
import os

from PIL import Image

# ---------------------------------------------------------------- dishware

# Wood (bowl.png / platter.png), darkest to lightest, and the light rim of the dish icons.
WOOD = {"D": "3f2b0b", "1": "49310b", "2": "533909", "3": "5f420e", "4": "69480d", "5": "714f0f", "6": "7f582c",
        "7": "8b602f", "8": "986d4e"}
# Glaze: cream and blue (tools/feasts_textures.py's plate, its whites darkened to stay under 235).
GLAZE = {"f": "8e8676", "F": "9c9482", "e": "a89e8a", "E": "bab09e", "r": "d4cab6", "R": "e0d8c6", "g": "e4ddcd",
         "G": "ebe5d7", "b": "56749f", "B": "6a88b8"}

DISHWARE = {}

# The plate from above (menu_meal_models.plate): only its well shows (x and z 3..13, its corners stepped), a shaded ring
# inside the rim (darker on the north and west, under the lit rim) round a faint grain.
DISHWARE["meal_wood_plate_top"] = (WOOD, [
    "5555555555555555",
    "5555555555555555",
    "5555555555555555",
    "5553333333344555",
    "5553455556544555",
    "5553545555554555",
    "5553555565554555",
    "5553555455554555",
    "5553565555454555",
    "5553555555554555",
    "5553554555654555",
    "5553455555554555",
    "5553444444444555",
    "5555555555555555",
    "5555555555555555",
    "5555555555555555",
])
# The plate and bowl from the side, in rows (menu_meal_models RIM_TOP, LIP, LINING, FOOT, UNDER): 0 the rim's top,
# 1 the lip, 2-5 a wall from under the lip down to its foot, 6-9 the lining inside a wall from its top down, 10 the
# foot, 11 its lower edge, 12-15 the underside's grain.
DISHWARE["meal_wood_side"] = (WOOD, [
    "7787778777877787",
    "6676667666766676",
    "5545554555455545",
    "4434443444344434",
    "4344434443444344",
    "3233323332333233",
    "4445444544454445",
    "3343334333433343",
    "3332333233323332",
    "2232223222322232",
    "2212221222122212",
    "1D111D111D111D11",
    "2223222322232223",
    "3222322232223222",
    "2232223222322232",
    "2322232223222322",
])
DISHWARE["meal_glazed_plate_top"] = (GLAZE, [
    "GGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGG",
    "GGGBbbbbbbbbbGGG",
    "GGGbGGGGGgGGbGGG",
    "GGGbGgGGGGGGbGGG",
    "GGGbGGGGGGGGbGGG",
    "GGGbGGGGGGgGbGGG",
    "GGGbGGgGGGGGbGGG",
    "GGGbGGGGGGGGbGGG",
    "GGGbGGGGgGGGbGGG",
    "GGGbGGGGGGGGbGGG",
    "GGGbbbbbbbbbbGGG",
    "GGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGG",
])
DISHWARE["meal_glazed_side"] = (GLAZE, [
    "RRgRRRRgRRRRgRRR",
    "RRRRgRRRRRRRgRRR",
    "rrrrrrrrrrrrrrrr",
    "EEEEEEEEEEEEEEEE",
    "EEEEEEEEEEEEEEEE",
    "EEEEEEEEEEEEEEEE",
    "BBBBBBBBBBBBBBBB",
    "rrrrrrrrrrrrrrrr",
    "rrrrrrrrrrrrrrrr",
    "EEEEEEEEEEEEEEEE",
    "FFfFFFfFFFfFFFfF",
    "ffefffefffefffef",
    "EEEEEEEEEEEEEEEE",
    "EErEEEEEErEEEEEE",
    "EEEEEEEEEEEEEEEE",
    "EEEEErEEEEEEErEE",
])

# ---------------------------------------------------------------- the meals

MEALS = {}


def rows_tile(rows):
    """A tile from 16 strings (x across, z down)."""
    return lambda x, z: rows[z % 16][x % 16]


def spiral(tones, turns=1.0, centre=(8, 8), width=1.0):
    """A tile of strands wound round `centre` (a twirl of pasta or a stirred sauce): each texel takes the tone of the
    strand it falls on, counting out from the middle (deterministic, no randomness)."""
    def tile(x, z):
        dx, dz = x + 0.5 - centre[0], z + 0.5 - centre[1]
        r, a = math.hypot(dx, dz), math.atan2(dz, dx)
        return tones[int(math.floor((r - turns * a / math.pi) / width)) % len(tones)]
    return tile


def waves(tones, period=8):
    """A tile of wavy strands: diagonal runs that turn back every four rows (noodles)."""
    def tile(x, z):
        shift = (z % 8) if z % 8 < 4 else 8 - (z % 8)
        return tones[((x + 2 * shift) % period) * len(tones) // period]
    return tile


# Bacon and Eggs: cooked_bacon.png and fried_egg.png.
MEALS["bacon_and_eggs"] = {
    "palette": {"_": "714f0f", "K": "482222", "L": "794141", "R": "bf3b3b", "P": "ca6161", "F": "e08888",
                "b": "b3936c", "c": "d4bea4", "w": "e9e2d9", "W": "f8f1e9", "o": "dc8413", "y": "e9b115", "Y": "f1d243"},
    "materials": {
        # A lean row and a fat row along each strip, the lean marbled with fat; darker where the strip dips.
        "bacon": {"tile": lambda x, z: ("LLRLLLRL"[x % 8] if z % 2 else "PPFPPPPF"[x % 8]),
                  "side": ["LRLKLRLLKLRLLRLK"]},
        "bacon_high": {"tile": lambda x, z: ("RRPRRRPR"[x % 8] if z % 2 else "FFPFFFFP"[x % 8]),
                       "side": ["RPRLRPRRLRPRRPRL"]},
        "white": {"base": "w", "lit": "W", "shade": "c", "side": ["cccbcccbcccbcccb"]},
        "yolk": {"base": "y", "lit": "Y", "shade": "o", "side": ["oyooyoooyooyoooy"]},
    },
    "details": [
        "",
        "",
        "",
        "    K      K",
        "    L      L",
        "   K         K",
        "   P         P",
        "    b      b",
        "   c         c",
        "",
        "   b         c",
        "    c     b",
    ],
}

# Steak and Potatoes: the steak in beef_patty.png's browns, its pink from cooked_mutton_chops.png, the potato, sour
# cream and greens from the dish's icon.
MEALS["steak_and_potatoes"] = {
    "palette": {"_": "714f0f", "K": "2a160d", "k": "4e2719", "m": "713f2d", "M": "7c4835", "L": "8e523c", "l": "985c43",
                "p": "c75e57", "q": "a3241e", "f": "e9c396", "F": "cfc6bb", "s": "9a5500", "S": "9d772e", "y": "c8973a",
                "i": "f0cd5a", "Y": "f8e265", "c": "e9e2d9", "C": "cfc6bb", "b": "b7ab9b", "g": "4d8c3b", "G": "3c713e", "h": "7ab244"},
    "materials": {
        "steak": {"base": "M", "lit": "l", "shade": "k", "side": ["mkMmkmMkMmkMmkmM", "KkKKkKKkKKkKKkKK"]},
        "steak_cut": {"side": ["qpppqppqppqpppqp", "kmkkmkkmkkmkkmkk"]},
        # grill marks across the crown, every third diagonal
        "steak_crown": {"tile": lambda x, z: "k" if (x - z) % 3 == 0 else "L", "lit": None, "shade": None,
                        "side": ["mkmmkmmkmmkmmkmm"]},
        "slice": {"tile": lambda x, z: "p" if z % 2 else "m", "side": ["kmkmkmkmkmkmkmkm"]},
        "mash": {"tile": lambda x, z: "cCcc"[(x + 2 * z) % 4], "lit": "c", "shade": "b", "side": ["CcCCbCCcCCbCCcCC"]},
        "butter": {"base": "Y", "side": ["iYiiYiiiYiiYiiiY"]},
        "potato": {"base": "i", "lit": "Y", "shade": "s", "side": ["yiyyiyyyiyyiyyyi", "sSsssSsssSsssSss"]},
        "parsley": {"base": "g", "lit": "h", "shade": "G", "side": ["GgGGgGGgGGgGGgGG"]},
    },
    "details": [
        "",
        "",
        "",
        "      fFffF",
        "     l",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
    ],
}

# Roasted Mutton Chops: the meat and bone of cooked_mutton_chops.png, the potatoes, red onion and rosemary of the
# dish's icon.
MEALS["roasted_mutton_chops"] = {
    "palette": {"_": "714f0f", "K": "2a160d", "k": "522f1f", "m": "6b3a2a", "M": "7c402f", "L": "884f40", "l": "a3705a",
                "n": "9d6147", "B": "e9e2d9", "b": "cfc6bb", "v": "b7ab9b", "V": "afaa89", "p": "a16530", "P": "c4985e",
                "Y": "f0b95a", "o": "521034", "O": "802457", "q": "c6316e", "g": "3c713e", "G": "4d8c3b", "h": "7ab244"},
    "materials": {
        "chop": {"tile": lambda x, z: "MMnMMMLM"[(x + 3 * z) % 8], "lit": "l", "shade": "k",
                 "side": ["mKmmkmmKmmkmmKmm", "kKkkKkkkKkkKkkkK"]},
        "chop_tail": {"base": "P", "lit": "Y", "shade": "l", "side": ["lPllPlllPllPlllP"]},
        "bone": {"side": ["BbBbbBbBBbBbbBbB"]},
        "knuckle": {"side": ["bvbVbvbbvbVbvbbv"]},
        "roast_potato": {"base": "P", "lit": "Y", "shade": "p", "side": ["PpPPpPPpPPpPPpPP", "pppkppppkppppkpp"]},
        "onion": {"tile": lambda x, z: "qO"[(x + z) % 2], "lit": "q", "shade": "o", "side": ["qOqoqOqoqOqoqOqo"]},
        "rosemary": {"tile": lambda x, z: "hgGg"[x % 4], "lit": None, "shade": None, "side": ["gGgggGgggGgggGgg"]},
    },
    "details": [
        "",
        "",
        "",
        "",
        "",
        "        h",
        "    h",
        "           h",
        "       h",
        "",
        "    Y",
        "",
    ],
}

# Pasta with Meatballs: the spaghetti, sauce and meatballs of the dish's icon (and beef_patty.png), basil from
# tomato.png's leaves.
MEALS["pasta_with_meatballs"] = {
    "palette": {"_": "e8c464", "Y": "ffeabc", "y": "e8c464", "c": "c59333", "a": "a08046", "R": "c0341f", "r": "941e33",
                "d": "63203a", "T": "cf4d3b", "K": "4e2719", "m": "713f2d", "M": "7c4835", "l": "985c43",
                "g": "097209", "G": "005000", "h": "2e922b", "w": "e9e2d9"},
    "materials": {
        "spaghetti": {"tile": spiral("YyycyYyc", turns=1.5), "lit": "Y", "shade": "c",
                      "side": ["yYcyyYycyYcyyYyc", "cyacycacyacycyca"]},
        "strand": {"base": "y", "lit": "Y", "shade": "c", "side": ["cycccycccyccccyc"],
                   "swatch": ["YYyYYYyYYYYyYYYy"]},
        "sauce": {"tile": lambda x, z: "RRTRrRRd"[(x * 3 + z * 5) % 8], "lit": "T", "shade": "r",
                  "side": ["rRrdrRrrdrRrrRrd"]},
        "meatball": {"tile": lambda x, z: "MMmMMMMm"[(x * 5 + z * 3) % 8], "lit": "l", "shade": "K",
                     "side": ["mMmlmMmmlmMmmMml", "KmKKmKKKmKKmKKKm"]},
        "basil": {"base": "g", "lit": "h", "shade": "G", "side": ["GgGGGgGGGgGGGgGG"]},
    },
    "details": [
        "",
        "",
        "",
        "",
        "",
        "",
        "     R   R",
        "    w",
        "           w",
        "",
        "       R",
        "",
    ],
}

# Squid Ink Pasta: the ink-dark noodles, squid and tomato of the dish's icon (tomato.png's lights), parsley from
# cabbage_leaf.png.
MEALS["squid_ink_pasta"] = {
    "palette": {"_": "373261", "a": "29254a", "A": "373261", "b": "514c80", "B": "756fa2", "T": "cf4d3b", "t": "941e33",
                "u": "63203a", "v": "ff8073", "s": "d6c5ad", "S": "c6a271", "z": "e9e2d9", "g": "4d8c3b", "G": "3c713e",
                "h": "7ab244"},
    "materials": {
        "ink": {"tile": spiral("BbAAbAaA", turns=1.5), "lit": "b", "shade": "a", "side": ["AbAaABAaAbAaAABa", "aAaaAaaaAaaAaaaA"]},
        "ink_strand": {"base": "b", "side": ["aAaaAaaaAaaAaaaA"], "swatch": ["BbbbBbbbBbbbBbbb"]},
        "tomato": {"base": "T", "lit": "v", "shade": "t", "side": ["tTttTtttTttTtttT"]},
        "squid": {"base": "s", "lit": "z", "shade": "S", "side": ["SsSSsSSsSSsSSsSS"]},
        "parsley": {"base": "g", "lit": "h", "shade": "G", "side": ["GgGGgGGgGGgGGgGG"]},
    },
    "details": [
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "     v",
        "",
        "",
        "",
    ],
}

# Vegetable Noodles: the pale noodles, carrot and greens of the dish's icon; pepper from tomato.png's reds, cabbage
# from cabbage_leaf.png.
MEALS["vegetable_noodles"] = {
    "palette": {"_": "b2a699", "N": "d8c59c", "n": "bca88e", "q": "b2a699", "Q": "938475", "C": "ff8e09", "c": "d36a0d",
                "x": "ffaa44", "R": "c0341f", "r": "941e33", "p": "cf4d3b", "G": "4d8c3b", "g": "7ab244", "H": "aadb74",
                "k": "3c713e", "o": "e9e2d9", "O": "cfc6bb"},
    "materials": {
        "noodles": {"tile": waves("NNnQ", period=4), "lit": None, "shade": None, "side": ["NnNQNnNQNnNQNnNQ", "nQqQnQqQnQqQnQqQ"]},
        "noodle_strand": {"base": "N", "side": ["nQnnQnnnQnnQnnnQ"], "swatch": ["NNnNNNNnNNNnNNNn"]},
        "carrot": {"base": "C", "lit": "x", "shade": "c", "side": ["cCcCCcCccCcCCcCc"], "swatch": ["xCCCxCCCxCCCxCCC"]},
        "pepper": {"base": "R", "lit": "p", "shade": "r", "side": ["rRrrRrrRrRrrRrrR"], "swatch": ["pRRRpRRRpRRRpRRR"]},
        "cabbage": {"tile": lambda x, z: "gH"[(x + z) % 2], "lit": "H", "shade": "G", "side": ["GkGGkGGkGGkGGkGG"]},
        "onion": {"base": "o", "lit": "o", "shade": "O", "side": ["OoOOoOOoOOoOOoOO"]},
        "scallion": {"base": "g", "lit": "H", "shade": "k", "side": ["kgkkgkkgkkgkkgkk"]},
    },
    "details": [],
}

# Tomato Sauce: the sauce and basil of the dish's icon; the spoon in the owner's light wood (the roast potato's tones).
MEALS["tomato_sauce"] = {
    "palette": {"_": "c0341f", "R": "c0341f", "r": "97141b", "d": "63203a", "L": "de5441", "g": "097209", "G": "005000",
                "h": "2e922b", "S": "c4985e", "s": "a16530", "x": "d8b07a"},
    "materials": {
        "sauce": {"tile": spiral("LRRRrRRd", turns=1.0, width=1.0), "lit": None, "shade": None, "side": ["rRrrRrrdrRrrRrrd"]},
        "basil": {"base": "g", "lit": "h", "shade": "G", "side": ["GgGGgGGgGGgGGgGG"]},
        "spoon": {"base": "S", "lit": "x", "shade": "s", "side": ["sSssSsssSssSsssS"]},
    },
    "details": [],
}

# Cornbread Stuffing: cornbread.png's golds and toasted browns, the herbs, cranberries and onion of the dish's icon.
MEALS["cornbread_stuffing"] = {
    "palette": {"_": "ad6918", "a": "824a00", "b": "a25c00", "c": "ad6918", "d": "d47900", "e": "ec9718", "f": "febb00",
                "g": "ffca38", "h": "f8e265", "G": "3b703d", "H": "4c8a3a", "J": "79b043", "r": "800b05", "R": "a30700",
                "p": "dc457d", "o": "e4c96f", "m": "896727", "M": "966d34", "n": "e2bf5b"},
    "materials": {
        "crumb": {"tile": rows_tile([
            "cdbcdcGdcbdcdcbd",
            "dcdbcodcdcbHdcdc",
            "bcHdcdcbdcdcodcb",
            "dcdcbdcdHcbdcdcd",
            "cbodcdbcdcdcbHdc",
            "dcdcHcdcdobcdcdb",
            "cdbcdcdbcdcHdcbd",
            "dHcdcbodcdbcdcdc",
            "bcdcdcdcbHcdobcd",
            "cdcobcHdcdcdcdcb",
            "dcbdcdcdcbdocHcd",
            "cdcHdcbcdcdcbdcd",
            "bdcdcodcHcbdcdob",
            "dcbcdcdbcdcHcdcd",
            "cHdcbdcdcodcdbcd",
            "dcdcdbHcdcdcbcdc",
        ]), "lit": None, "shade": None, "side": ["cdcbcdcdcbcdcdcb", "bcbabcbbabcbbcba"]},
        "cube": {"base": "e", "lit": "g", "shade": "c", "side": ["cdccdcccdccdcccd", "edeedeedeeddeede"]},
        "crouton": {"side": ["cbccbcccbccbcccb", "edeedeedeeddeede"], "swatch": ["ge", "ec"]},
        "berry": {"base": "R", "lit": "p", "shade": "r", "side": ["rRrrRrrRrrRrrrRr"]},
        "mushroom": {"base": "M", "lit": "n", "shade": "m", "side": ["mnnnmnnnmnnnmnnn"]},
        "sage": {"base": "H", "lit": "J", "shade": "G", "side": ["GHGGHGGHGGHGGHGG"]},
    },
    "details": [
        "",
        "",
        "",
        "",
        "",
        "    h",
        "     b",
        "",
        "     h    J",
        "",
        "         h",
        "    h",
        "",
    ],
}


# ---------------------------------------------------------------- drawing

def rgba(hex_colour):
    return tuple(int(hex_colour[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def grid_image(palette, rows):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows)
    img = Image.new("RGBA", (16, 16))
    for z, row in enumerate(rows):
        for x, key in enumerate(row):
            img.putpixel((x, z), rgba(palette[key]))
    return img


def side_rows(name):
    """The first row of each material in <name>_side, in the order MATERIALS lists them."""
    out, row = {}, 0
    for mat, info in MEALS[name]["materials"].items():
        out[mat] = row
        row += len(info["side"])
    assert row <= 16, f"{name}: {row} side rows"
    return out


def swatches(name):
    """Where each material's swatch (the top of a turned piece, drawn whole) lies in <name>_side: material -> ((u, v),
    (width, depth)). Swatches follow the material rows, left to right."""
    meal = MEALS[name]
    v = sum(len(info["side"]) for info in meal["materials"].values())
    u, line, out = 0, 0, {}
    for mat, info in meal["materials"].items():
        swatch = info.get("swatch")
        if not swatch:
            continue
        w, d = len(swatch[0]), len(swatch)
        if u + w > 16:
            u, v, line = 0, v + line, 0
        out[mat] = ((u, v), (w, d))
        u, line = u + w, max(line, d)
    assert v + line <= 16, f"{name}: side texture overfull"
    return out


def side_image(name):
    meal = MEALS[name]
    ground = next(iter(meal["palette"]))
    rows = [row for info in meal["materials"].values() for row in info["side"]]
    rows = [list(row) for row in rows + [ground * 16] * (16 - len(rows))]
    for mat, ((u, v), _) in swatches(name).items():
        for dz, line in enumerate(meal["materials"][mat]["swatch"]):
            for dx, key in enumerate(line):
                rows[v + dz][u + dx] = key
    return grid_image(meal["palette"], ["".join(row) for row in rows])


def cells(piece):
    """The texels a piece's top covers at least half of."""
    (x0, _, z0), (x1, _, z1) = piece["lo"], piece["hi"]
    out = set()
    for z in range(math.floor(z0), math.ceil(z1)):
        for x in range(math.floor(x0), math.ceil(x1)):
            if min(x + 1, x1) - max(x, x0) >= 0.5 and min(z + 1, z1) - max(z, z0) >= 0.5:
                out.add((x, z))
    return out


def top_rows(name):
    """The meal from above, as 16 rows of palette keys (see the module's docstring)."""
    import menu_meal_models
    meal = MEALS[name]
    ground = next(iter(meal["palette"]))
    grid = [[ground] * 16 for _ in range(16)]
    groups = {}
    for piece in menu_meal_models.pieces(name):
        if "rot" not in piece:
            groups.setdefault((round(piece["hi"][1], 3), piece["mat"]), set()).update(cells(piece))
    for (_, mat), covered in sorted(groups.items(), key=lambda item: item[0][0]):
        info = meal["materials"][mat]
        for x, z in covered:
            lit = ((x, z - 1) not in covered) + ((x - 1, z) not in covered)
            shade = ((x, z + 1) not in covered) + ((x + 1, z) not in covered)
            key = info["tile"](x, z) if "tile" in info else info["base"]
            if lit > shade and info.get("lit"):
                key = info["lit"]
            elif shade > lit and info.get("shade"):
                key = info["shade"]
            if 0 <= x < 16 and 0 <= z < 16:
                grid[z][x] = key
    for z, row in enumerate(meal.get("details", [])):
        for x, key in enumerate(row):
            if key != " ":
                grid[z][x] = key
    return ["".join(row) for row in grid]


def top_image(name):
    return grid_image(MEALS[name]["palette"], top_rows(name))


def images():
    """Every texture this module draws: name -> image."""
    out = {name: grid_image(*DISHWARE[name]) for name in DISHWARE}
    for name in MEALS:
        out[f"{name}_top"] = top_image(name)
        out[f"{name}_side"] = side_image(name)
    return out


def draw_all(save):
    for name, image in images().items():
        save(image, "block", name)


if __name__ == "__main__":
    folder = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "resources", "assets",
                          "jugcraft", "textures", "block")
    for name, image in images().items():
        image.save(os.path.join(folder, f"{name}.png"), optimize=True)
