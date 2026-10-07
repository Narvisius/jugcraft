"""Steampunk Armor and Kaiser Armor (docs/features/steampunk-and-kaiser-armor.md): the stylized looks first drawn for
bronze and steel armor (2 October 2026), kept as their own sets on 5 October 2026. These are the worn 64x32 layers and
the inventory icons. tools/gear_textures.py draws them, and for now draws the bronze and steel armor with them too.
tools/check_mod_data.py (check_armor_looks) pins the Steampunk and Kaiser PNGs to these pixels.

Everything is hand-drawn pixel art, written out below as rows of characters.
- '.' is empty.
- '0' to '4' are the style's five metal shades, darkest to lightest (PALETTES).
- Letters are each style's own colours (COLORS).
- Faces not listed stay empty.

Steampunk (from bronze) has the following.
- Helmet: a bronze-crowned aviator cap with leather ear flaps and valve ear cups. Teal-glassed goggles sit on the brow, on
  the outer hat layer so they stand proud of the cap.
- Chestplate: a bronze breastplate with a pressure gauge, a copper boiler on the back, leather straps and a buckled
  belt. Bronze pauldrons with copper bosses, and leather bracers.
- Leggings: riveted thigh plates with copper knee cops.
- Boots: buckled leather with bronze cuffs and toe caps.

Kaiser (kaiserpunk, from steel) has the following.
- Helmet: a black leather Pickelhaube with a gold star plate, a gold cruciform spike base (raised on the hat layer)
  and a steel brim. Chin scales run down the cheeks.
- Chestplate: a field-grey tunic with a red-and-gold collar, under a steel cuirass. A double row of gold buttons, a
  medal on its ribbon, and a black belt with a gold buckle. Gold-fringed epaulettes, and red-piped cuffs.
- Leggings: field-grey breeches with red side piping and steel knee plates.
- Boots: tall polished black jackboots with steel toe caps.
"""
from PIL import Image

COLORS = {
    "steampunk": {
        "L": (70, 44, 24), "l": (108, 70, 38),          # leather
        "C": (132, 62, 34), "c": (186, 96, 52), "p": (226, 146, 92),  # copper
        "G": (40, 140, 132), "g": (190, 246, 232),      # teal goggle glass and its glint
        "W": (234, 224, 192), "n": (176, 32, 26),       # gauge dial and needle
        "k": (30, 24, 20), "S": (112, 116, 120),        # near black, steam pipe
    },
    "kaiser": {
        "K": (24, 22, 26), "k": (54, 52, 60), "w": (132, 134, 146),  # black leather and its gloss
        "F": (88, 96, 82), "f": (116, 124, 106),        # field grey
        "Y": (204, 158, 48), "y": (248, 214, 120),      # gold
        "R": (162, 32, 36), "r": (106, 20, 24),         # red piping
    },
}

# Face rectangles on the 64x32 sheet: box (u, v, w, h, d) -> top, bottom, right, front, left, back.
BOXES = {"head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 4, 12, 4),
         "leg": (0, 16, 4, 12, 4)}
FACE_NAMES = ("top", "bottom", "right", "front", "left", "back")


def faces(box):
    u, v, w, h, d = BOXES[box]
    rects = [(u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h),
             (u + 2 * d + w, v + d, w, h)]
    return dict(zip(FACE_NAMES, rects))


def _sides(rows):
    """The same rows on the right, left and back faces (a box's sides and back often match)."""
    return {"right": rows, "left": rows, "back": rows}


# ---------------------------------------------------------------- Steampunk (first drawn for bronze)
B_HEAD_SIDE = ["13444431", "23333332", "LLLLLLLL", "LLLLLLLL", "LLL34LLL", "LL3cp3LL", "LLL23LLL", "lLLLLLLl"]
B_ARM = ["3443", "3cp3", "2332", "1221", "LLLL", "....", "....", "....", "L4LL", "LLLL", "2332", "1221"]
STEAMPUNK = {
    "layer1": {
        "head": {
            "top": ["12233221", "23344332", "2343c432", "234cpc32", "2343c432", "23344332", "12333321", "11222211"],
            "front": ["13444431", "23333332", "LLLLLLLL", "LLLLLLLL", "4......4", "L......L", "L......L",
                      "lk....kl"],
            "right": B_HEAD_SIDE, "left": B_HEAD_SIDE,
            "back": ["13444431", "23333332", "LLLLLLLL", "LLL44LLL", "LLLLLLLL", "LlLLLLlL", "LLLLLLLL", "lLLLLLLl"],
        },
        "hat": {
            "front": ["........", ".34..43.", "3Gg33gG3", "3GG33GG3", ".34..43.", "........", "........",
                      "........"],
            **_sides(["........", "........", "kkkkkkkk", "kkkkkkkk", "........", "........", "........",
                      "........"]),
        },
        "body": {
            "top": ["L344443L", "L333333L", "L333333L", "L222222L"],
            "bottom": ["11111111"] * 4,
            "front": ["LL3443LL", "L234432L", "k233332k", "21kkkk12", "2kWWnWk2", "2kWnWWk2", "21kkkk12",
                      "LLL44LLL", "2CccccC2", "2c3333c2", "23333332", "k122221k"],
            "back": ["L122221L", "LppccCCL", "1ppccCC1", "14444441", "1ppccCC1", "1ppccCC1", "14444441",
                     "LLLLLLLL", "1ppccCC1", "S122221S", "S222222S", "11111111"],
            "right": ["L34L", "L33L", "2SS2", "2332", "2332", "2SS2", "2332", "LLLL", "2332", "2332", "2332", "1221"],
            "left": ["L34L", "L33L", "2SS2", "2332", "2332", "2SS2", "2332", "LLLL", "2332", "2332", "2332", "1221"],
        },
        "arm": {"top": ["3443", "4cp4", "4pc4", "3443"], "front": B_ARM, **_sides(B_ARM)},
        "leg": {
            "bottom": ["kkkk"] * 4,
            "front": ["...."] * 7 + ["3443", "LLLL", "L4LL", "2cp2", "kkkk"],
            **_sides(["...."] * 7 + ["3443", "LLLL", "L4LL", "2332", "kkkk"]),
        },
    },
    "layer2": {
        "body": {
            "front": ["........"] * 8 + ["LLL44LLL", "23333332", "2c3333c2", "12222221"],
            "back": ["........"] * 8 + ["LLLLLLLL", "23333332", "23333332", "12222221"],
            "right": ["...."] * 8 + ["LLLL", "2332", "2332", "1221"],
            "left": ["...."] * 8 + ["LLLL", "2332", "2332", "1221"],
        },
        "leg": {
            "front": ["2332", "3443", "3333", "2332", "L4LL", "2332", "3cp3", "2pc2", "1221"] + ["...."] * 3,
            **_sides(["2332", "3443", "3333", "2332", "LLLL", "2332", "3cp3", "2pc2", "1221"] + ["...."] * 3),
        },
    },
}

# ---------------------------------------------------------------- Kaiser (first drawn for steel)
S_HEAD_SIDE = ["KkKKKKkK", "KKKKKKKK", "KkKKKKkK", "34444443", "...Yy...", "...yY...", "...Yy...", "...yY..."]
S_ARM = ["YyYy", "yYyY", "FFFF", "FfFF", "FFFF", "FFfF", "FFFF", "FfFF", "FFFF", "RRRR", "RYRR", "rRRr"]
S_LEG = ["FFFF", "FfFF", "FFFF", "FFfF", "FFFF", "3443", "2332", "FFFF", "FfFF", "....", "....", "...."]
S_LEG_SIDE = ["FRFF", "FRfF", "FRFF", "FRFF", "FRFF", "3443", "2332", "FRFF", "FRfF", "....", "....", "...."]
S_BOOT = ["...."] * 4 + ["kKKk", "KwKK", "KwKK", "KKKK", "KwKK", "KKKK", "3443", "1111"]
KAISER = {
    "layer1": {
        "head": {
            "top": ["KKKYYKKK", "KkKYYKkK", "KKKYyKKK", "YYYyyYYY", "YYYwyYYY", "KKKYYKKK", "KkKYYKkK",
                    "KKKYYKKK"],
            "front": ["KkKYYKkK", "KKYyyYKK", "KYyYYyYK", "34444443", "Y......Y", "y......y", "Y......Y",
                      "y......y"],
            "right": S_HEAD_SIDE, "left": S_HEAD_SIDE,
            "back": ["KkKKKKkK", "KKKKKKKK", "KKKKKKKK", "34444443", "KKKKKKKK", "kKKKKKKk", "........",
                     "........"],
        },
        # The spike's base, raised a little above the helmet's crown.
        "hat": {"top": ["........", "........", "...YY...", "..YyyY..", "..YyyY..", "...YY...",
                        "........", "........"]},
        "body": {
            "top": ["FRffffRF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF"],
            "bottom": ["rRRRRRRr"] * 4,
            "front": ["FRYffYRF", "FFFYYFFF", "23444432", "2R3YY432", "2Y344332", "233YY332", "23344332",
                      "KKKYYKKK", "FFFFFFFF", "FffFFffF", "FfYFFYfF", "rRRRRRRr"],
            "back": ["FRffffRF", "FFFFFFFF", "23333332", "23444432", "23333332", "23444432", "23333332",
                     "KKKKKKKK", "FFFFFFFF", "FYFFFFYF", "FFFFFFFF", "rRRRRRRr"],
            "right": ["FFFF", "FfFF", "2332", "2442", "2332", "2442", "2332", "KKKK", "FFFF", "FFFF", "FfFF", "rRRr"],
            "left": ["FFFF", "FfFF", "2332", "2442", "2332", "2442", "2332", "KKKK", "FFFF", "FFFF", "FfFF", "rRRr"],
        },
        "arm": {"top": ["YyYy", "yYyY", "YyYy", "yYyY"], "front": S_ARM, **_sides(S_ARM)},
        "leg": {"bottom": ["1111"] * 4, "front": S_BOOT, **_sides(S_BOOT)},
    },
    "layer2": {
        "body": {
            "front": ["........"] * 8 + ["FFFFFFFF", "FfFFFFfF", "FFFFFFFF", "rRRRRRRr"],
            "back": ["........"] * 8 + ["FFFFFFFF", "FFFFFFFF", "FfFFFFfF", "rRRRRRRr"],
            "right": ["...."] * 8 + ["FFFF", "FRFF", "FRFF", "rRRr"],
            "left": ["...."] * 8 + ["FFFF", "FFRF", "FFRF", "rRRr"],
        },
        "leg": {"front": S_LEG, "back": S_LEG, "right": S_LEG_SIDE, "left": S_LEG_SIDE},
    },
}

STYLES = {"steampunk": STEAMPUNK, "kaiser": KAISER}
# Each style's five metal shades, pinned here so a repaint of the ingots never shifts these looks. Steampunk's is a
# deeper, polished bronze, so it reads against the leather and copper; Kaiser's is the steel ingot's palette of
# 2 October 2026.
PALETTES = {"steampunk": [(46, 28, 12), (94, 58, 22), (146, 96, 36), (192, 142, 58), (232, 190, 100)],
            "kaiser": [(44, 48, 56), (72, 78, 88), (102, 108, 120), (136, 142, 154), (176, 182, 194)]}

# ---------------------------------------------------------------- inventory icons (16x16)
ICONS = {
    "steampunk": {
        "helmet": [
            "................",
            "................",
            ".....233332.....",
            "....23444432....",
            "...2344cp4432...",
            "...2344444432...",
            "..k3Gg3333gG3k..",
            "..k3GG3443GG3k..",
            "...L23444432L...",
            "...LL......LL...",
            "...L3......3L...",
            "...Lc......cL...",
            "...lL......Ll...",
            "................",
            "................",
            "................",
        ],
        "chestplate": [
            "................",
            "..LL23....32LL..",
            ".L3344L..L4433L.",
            ".23444333334432.",
            ".1c3221kkkk12c1.",
            "..1.2kWWnWk2.1..",
            "....2kWnWWk2....",
            "....21kkkk12....",
            "....LLLL44LL....",
            "....23cccc32....",
            "....2c3333c2....",
            "....12222221....",
            "................",
            "................",
            "................",
            "................",
        ],
        "leggings": [
            "................",
            "................",
            "....LLL44LLL....",
            "....23333332....",
            "....2c3..3c2....",
            "....2332.332....",
            "....3443.443....",
            "....L4LL.L4L....",
            "....2332.332....",
            "....3cp3.3cp....",
            "....2pc2.2pc....",
            "....1221.122....",
            "................",
            "................",
            "................",
            "................",
        ],
        "boots": [
            "................",
            "................",
            "................",
            "................",
            "...3443..3443...",
            "...LLLL..LLLL...",
            "...L4LL..LL4L...",
            "...LLLL..LLLL...",
            "...LLLl..lLLL...",
            "..2cpLl..lLpc2..",
            ".23cpc2..2cpc32.",
            ".kkkkkk..kkkkkk.",
            "................",
            "................",
            "................",
            "................",
        ],
    },
    "kaiser": {
        "helmet": [
            ".......yy.......",
            ".......Yy.......",
            ".......Yy.......",
            "......YyyY......",
            ".....KKYYKK.....",
            "....KkKYyKkK....",
            "...KKKYyyYKKK...",
            "...KkYyYYyYkK...",
            "...KKKYyyYKKK...",
            "..234444444432..",
            "...Y.........Y..",
            "...y.........y..",
            "....Y.......Y...",
            ".....yYyYyYy....",
            "................",
            "................",
        ],
        "chestplate": [
            "................",
            ".yYyFRYffYRFyYy.",
            ".YyFFFFYYFFFFyY.",
            ".FF2334444332FF.",
            ".FF2R3YY4332FF..",
            ".RR2Y3443332RR..",
            "....233YY332....",
            "....23344332....",
            "....KKKYYKKK....",
            "....FFFFFFFF....",
            "....FfYFFYfF....",
            "....rRRRRRRr....",
            "................",
            "................",
            "................",
            "................",
        ],
        "leggings": [
            "................",
            "................",
            "....FFFFFFFF....",
            "....FfFFFFfF....",
            "....FRFF.FFR....",
            "....FRFF.FFR....",
            "....FRfF.FfR....",
            "....3443.344....",
            "....2332.233....",
            "....FRFF.FFR....",
            "....FRFF.FFR....",
            "....rRRr.rRR....",
            "................",
            "................",
            "................",
            "................",
        ],
        "boots": [
            "................",
            "................",
            "...kKKk..kKKk...",
            "...KwKK..KKwK...",
            "...KwKK..KKwK...",
            "...KwKK..KKwK...",
            "...KKKK..KKKK...",
            "...KwKK..KKwK...",
            "...KKKK..KKKK...",
            "..KKKKK..KKKKK..",
            ".23443K..K34432.",
            ".111111..111111.",
            "................",
            "................",
            "................",
            "................",
        ],
    },
}


def _color(ch, palette, colors):
    if ch.isdigit():
        return tuple(palette[int(ch)]) + (255,)
    return colors[ch] + (255,)


def _paint(img, rect, rows, palette, colors):
    left, top, width, height = rect
    assert len(rows) == height, (rect, rows)
    for y, row in enumerate(rows):
        assert len(row) == width, (rect, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((left + x, top + y), _color(ch, palette, colors))


def layer(style, palette, leggings):
    """One worn layer (64x32): layer 1 for the helmet, chestplate and boots, layer 2 for the leggings."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for box, face_rows in STYLES[style]["layer2" if leggings else "layer1"].items():
        rects = faces(box)
        for face, rows in face_rows.items():
            _paint(img, rects[face], rows, palette, COLORS[style])
    return img


def icon(style, piece, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _paint(img, (0, 0, 16, 16), ICONS[style][piece], palette, COLORS[style])
    return img
