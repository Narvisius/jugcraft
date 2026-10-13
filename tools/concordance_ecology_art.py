"""Textures for roadmap step 14, the Greenwardens' garden (tools/concordance_ecology.py): the Verdant Bed (adapted
from the owner's rich soil, with a wooden rim that Fusion opens between neighbouring beds), the four crops' growth
steps, the Habitat Gauge, and the GeckoLib sheets of the Verdant Heart, Mulch Maw and Gleaner. The item icons are
16x16 maps in tools/item_icons/ (docs/ITEM_ICONS.md). tools/concordance_art.py includes these in its textures().

Provenance (docs/features/arcane-concordance-ecology.md, "Art"): the bed's soil is the owner's library
art/owner-library/originals/Blocks/farming and food textures/rich_soil*.png, shifted towards moss green with a few
moss flecks; the Gloamcap's steps are that folder's brown_mushroom_colony_stage0..3.png with their caps recoloured
violet. Everything else is drawn here from code with fixed seeds. The library files are only read, never changed.
"""

import colorsys
import os
import random
import zlib

from PIL import Image

import clean_metal as cm
import concordance_ecology as ecology
import item_icons

HERE = os.path.dirname(os.path.abspath(__file__))
LIBRARY = os.path.join(HERE, "..", "art", "owner-library", "originals", "Blocks", "farming and food textures")

# Plant ramps for the crops (natural textures: no outline, a few tones of one colour each, lit from above).
LEAF = [(34, 66, 26), (48, 90, 34), (66, 118, 44), (88, 146, 56), (118, 174, 72)]
MOSS = [(26, 66, 58), (38, 92, 78), (54, 120, 98), (78, 150, 120), (120, 186, 156)]
PETAL = [(176, 110, 20), (214, 152, 32), (236, 190, 56), (248, 222, 112)]
VIOLET = [(62, 36, 92), (86, 56, 124), (112, 80, 156), (146, 112, 190), (184, 156, 222)]
DEW = [(150, 210, 236), (214, 244, 255)]
POD = [(70, 104, 40), (98, 134, 52), (132, 166, 70)]
# The devices' sheets.
CLAY = [(92, 42, 24), (130, 62, 38), (166, 86, 52), (196, 116, 74), (222, 154, 108)]
SOIL = [(44, 30, 20), (62, 44, 28), (82, 60, 38)]
FLESH = [(110, 30, 40), (150, 46, 56), (186, 72, 78), (214, 112, 112)]
BONE = [(170, 160, 136), (210, 202, 178), (238, 232, 214)]
WOOD = [(58, 38, 20), (84, 56, 30), (110, 76, 42), (136, 98, 58)]
COPPER = [(110, 52, 30), (160, 84, 48), (200, 118, 72), (230, 158, 110)]


def _new(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def _library(name):
    with Image.open(os.path.join(LIBRARY, name)) as img:
        return img.convert("RGBA").crop((0, 0, 16, 16))


def _shift(img, hue, sat=1.0, val=1.0):
    """The image with every opaque pixel's hue moved by `hue` (0 to 1) and its saturation and value scaled."""
    out = img.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hsv_to_rgb((h + hue) % 1.0, min(1.0, s * sat), min(1.0, v * val))
            out.putpixel((x, y), (round(r2 * 255), round(g2 * 255), round(b2 * 255), a))
    return out


# ------------------------------------------------------------------------------------------------- the bed

def _moss_flecks(img, seed, count):
    """A few small moss clusters (two or three pixels), placed from a fixed seed, so the soil reads as living."""
    rng = random.Random(seed)
    for _ in range(count):
        x, y = rng.randrange(16), rng.randrange(16)
        tone = MOSS[rng.randrange(1, 4)]
        for dx, dy in ((0, 0), (1, 0), (0, 1))[:rng.randrange(2, 4)]:
            cm.put(img, (x + dx) % 16, (y + dy) % 16, tone)
    return img


def bed_top(wet):
    soil = _library("rich_soil_farmland_moist.png" if wet else "rich_soil_farmland.png")
    return _moss_flecks(_shift(soil, 0.035, 0.9), zlib.crc32(b"verdant_bed_top"), 5)


def bed_side(wet):
    soil = _library("rich_soil_farmland_moist_side.png" if wet else "rich_soil.png")
    img = _moss_flecks(_shift(soil, 0.035, 0.9), zlib.crc32(b"verdant_bed_side"), 3)
    # A strip of moss along the top edge, where the bed meets the air.
    rng = random.Random(zlib.crc32(b"verdant_bed_rim"))
    for x in range(16):
        cm.put(img, x, 0, MOSS[2 + rng.randrange(2)])
        if rng.random() < 0.45:
            cm.put(img, x, 1, MOSS[1 + rng.randrange(2)])
    return img


def _rimmed(top, rim):
    """The top with a wooden rim on the sides it does not share with another bed: (top, right, bottom, left)."""
    img = top.copy()
    up, right, down, left = rim
    for i in range(16):
        if up:
            cm.put(img, i, 0, WOOD[2])
        if down:
            cm.put(img, i, 15, WOOD[0])
        if left:
            cm.put(img, 0, i, WOOD[3] if i < 15 else WOOD[1])
        if right:
            cm.put(img, 15, i, WOOD[1])
    return img


def bed_top_rimmed(wet):
    return _rimmed(bed_top(wet), (True, True, True, True))


def bed_connected(wet):
    """Fusion's 4x4 sheet: each tile the rimmed top with the rim left off where a neighbour bed joins."""
    import concordance_ritual_art
    top = bed_top(wet)
    sheet = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for bits in range(16):
        up, right, down, left = bool(bits & 1), bool(bits & 2), bool(bits & 4), bool(bits & 8)
        col, row = concordance_ritual_art._simple_tile(up, right, down, left)
        sheet.paste(_rimmed(top, (not up, not right, not down, not left)), (col * 16, row * 16))
    return sheet


# ------------------------------------------------------------------------------------------------- the crops

def _stem(img, x, y0, y1, ramp):
    for y in range(y0, y1 + 1):
        cm.put(img, x, y, ramp[2])


def _leaf(img, x, y, direction, length, ramp):
    """A leaf from (x, y) outwards and a little up, lit on its upper edge."""
    for i in range(length):
        px_, py = x + direction * (i + 1), y - (i // 2)
        cm.put(img, px_, py, ramp[3] if i < length - 1 else ramp[4])
        cm.put(img, px_, py + 1, ramp[1])


def _sprout(ramp, height, leaves):
    img = _new()
    for x in (4, 11):
        _stem(img, x, 16 - height, 15, ramp)
        for k, y in enumerate(leaves):
            _leaf(img, x, 15 - y, -1 if (k + x) % 2 else 1, 2 + k % 2, ramp)
    return img


def sunpetal(stage):
    img = _sprout(LEAF, (4, 7, 10, 11)[stage], [(2,), (2, 5), (3, 6), (3, 6)][stage])
    if stage >= 2:
        for cx in (4, 11):
            top = 16 - (10, 11)[stage - 2]
            if stage == 2:  # a closed bud, green turning gold
                for dx, dy, tone in ((0, -1, PETAL[1]), (-1, 0, LEAF[3]), (0, 0, PETAL[2]), (1, 0, LEAF[2]), (0, -2, PETAL[2])):
                    cm.put(img, cx + dx, top + dy, tone)
            else:  # open: five petals round a pale centre
                for dx, dy in ((0, -2), (-2, -1), (2, -1), (-1, 1), (1, 1), (-1, -2), (1, -2), (-2, 0), (2, 0)):
                    cm.put(img, cx + dx, top + dy, PETAL[2] if dy < 0 else PETAL[1])
                for dx, dy in ((-1, -1), (1, -1), (0, 0), (-1, 0), (1, 0)):
                    cm.put(img, cx + dx, top + dy, PETAL[3])
                cm.put(img, cx, top - 1, DEW[1])
    return img


def dewmoss(stage):
    """Low tufts of blue-green moss that spread and thicken; ripe, they bead with dew."""
    img = _new()
    rng = random.Random(zlib.crc32(f"dewmoss{stage}".encode()))
    height = (2, 3, 5, 6)[stage]
    for x in range(1, 15):
        tuft = height - (abs(x - 4) // 3 if x < 8 else abs(x - 11) // 3) - rng.randrange(2)
        for y in range(16 - max(1, tuft), 16):
            depth = 15 - y
            cm.put(img, x, y, MOSS[min(4, 1 + depth // 2 + (1 if rng.random() < 0.25 else 0))] if depth < tuft - 1 else MOSS[3])
        if tuft > 1:
            cm.put(img, x, 16 - tuft, MOSS[4] if rng.random() < 0.4 else MOSS[3])
    if stage == 3:
        for x, y in ((3, 11), (7, 10), (12, 11), (10, 12), (5, 12)):
            cm.put(img, x, y, DEW[1])
            cm.put(img, x, y + 1, DEW[0])
    return img


def gloamcap(stage):
    """The owner's mushroom colony, its caps recoloured to the Gloamcap's dusky violet (stems left pale)."""
    src = _library(f"brown_mushroom_colony_stage{stage}.png")
    out = src.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s > 0.18:  # the caps (the pale stems have little colour)
                r2, g2, b2 = colorsys.hsv_to_rgb(0.75, min(1.0, s * 1.1), v * 0.85)
                out.putpixel((x, y), (round(r2 * 255), round(g2 * 255), round(b2 * 255), a))
    return out


def mendvetch(stage):
    """A climbing vetch: tendrils with paired leaflets, violet flowers, then green pods."""
    img = _new()
    height = (4, 7, 10, 12)[stage]
    for x, lean in ((4, 1), (11, -1)):
        for i in range(height):
            y = 15 - i
            cx = x + (lean if i % 4 == 3 else 0)
            cm.put(img, cx, y, LEAF[2])
            if i % 3 == 1 and i < height - 1:
                cm.put(img, cx - 1, y, LEAF[3])
                cm.put(img, cx + 1, y, LEAF[3])
        top = 16 - height
        if stage >= 2:
            cm.put(img, x, top, VIOLET[3])
            cm.put(img, x + lean, top + 1, VIOLET[2])
        if stage == 3:
            for i, (dx, dy) in enumerate(((-1, 3), (1, 6))):
                for k in range(3):
                    cm.put(img, x + dx, top + dy + k, POD[1 + (k == 0)])
    return img


# ------------------------------------------------------------------------------------------------- the gauge

def gauge_stalk():
    img = _new()
    for y in range(16):
        for x in range(16):
            ramp = COPPER if y % 8 < 2 else LEAF
            cm.put(img, x, y, ramp[1 + ((x + y * 3) % 5 == 0)] if x % 4 else ramp[2])
    return img


def gauge_bulb(open_):
    img = _new()
    rng = random.Random(zlib.crc32(b"gauge" + bytes([open_])))
    for y in range(16):
        for x in range(16):
            if open_ and 4 <= x <= 11 and 4 <= y <= 11:
                tone = (120, 220, 120) if (x + y) % 3 else (190, 250, 170)
            else:
                tone = VIOLET[1 + rng.randrange(3)]
            cm.put(img, x, y, tone)
    return img


# ------------------------------------------------------------------------------------------------- GeckoLib sheets

def _paint_box(img, uv, size, ramp, seed, speckle=0.2):
    """Paints one box-UV cube region: the top light, the sides mid, the bottom dark, with a few placed specks."""
    (u, v), (w, h, d) = uv, size
    rng = random.Random(seed)
    faces = [((u + d, v, w, d), 3), ((u + d + w, v, w, d), 1),  # top, bottom
             ((u, v + d, d, h), 2), ((u + d, v + d, w, h), 2), ((u + d + w, v + d, d, h), 2), ((u + 2 * d + w, v + d, w, h), 2)]
    for (x0, y0, fw, fh), level in faces:
        for y in range(y0, y0 + fh):
            for x in range(x0, x0 + fw):
                tone = level
                if fh > 2 and y == y0 and level == 2:
                    tone = 3  # the top row of a side catches the light
                if rng.random() < speckle:
                    tone = max(0, min(len(ramp) - 1, tone + rng.choice((-1, 1))))
                cm.put(img, x, y, ramp[min(tone, len(ramp) - 1)])


def _sheet(key, materials):
    uvs, sizes = ecology.SIZES[key]
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for name, uv in uvs.items():
        _paint_box(img, uv, sizes[name], materials[name], zlib.crc32(f"{key}.{name}".encode()))
    return img


def heart_sheet():
    img = _sheet("verdant_heart", {"root": SOIL, "stem": LEAF, "leaf": LEAF, "heart": LEAF, "crown": FLESH})
    # Garnet veins down the heart's faces.
    u, v = ecology.HEART_UV["heart"]
    w, h, d = ecology.HEART_SIZES["heart"]
    for face_x in (u + d, u + 2 * d + w):
        for y in range(v + d + 1, v + d + h):
            cm.put(img, face_x + w // 2, y, FLESH[1])
            if (y - v) % 3 == 0:
                cm.put(img, face_x + w // 2 - 1, y, FLESH[2])
                cm.put(img, face_x + w // 2 + 1, y, FLESH[2])
    return img


def maw_sheet():
    return _sheet("mulch_maw", {"pot": CLAY, "jaw": LEAF, "tooth": BONE, "tongue": FLESH})


def gleaner_sheet():
    return _sheet("gleaner", {"pot": CLAY, "soil": SOIL, "stem": LEAF, "arm": LEAF, "hand": LEAF})


# ------------------------------------------------------------------------------------------------- all of them

ICONS = ["sunpetal", "dewmoss", "gloamcap", "mendvetch", "verdant_chaff", "verdant_heart", "mulch_maw", "gleaner",
         "habitat_gauge"]
CROPS = {"sunpetal": sunpetal, "dewmoss": dewmoss, "gloamcap": gloamcap, "mendvetch": mendvetch}


def textures():
    out = {}
    for name in ICONS:
        out[("item", name)] = item_icons.draw(name)
    out[("block", "verdant_bed_top")] = bed_top_rimmed(False)
    out[("block", "verdant_bed_wet_top")] = bed_top_rimmed(True)
    out[("block", "verdant_bed_side")] = bed_side(False)
    out[("block", "verdant_bed_wet_side")] = bed_side(True)
    out[("block", "verdant_bed_top_connected")] = bed_connected(False)
    out[("block", "verdant_bed_wet_top_connected")] = bed_connected(True)
    for key, draw in CROPS.items():
        for stage in range(ecology.STAGES + 1):
            out[("block", f"{key}_stage{stage}")] = draw(stage)
    out[("block", "habitat_gauge_stalk")] = gauge_stalk()
    out[("block", "habitat_gauge_bulb")] = gauge_bulb(False)
    out[("block", "habitat_gauge_bulb_open")] = gauge_bulb(True)
    out[("block", "verdant_heart")] = heart_sheet()
    out[("block", "mulch_maw")] = maw_sheet()
    out[("block", "gleaner")] = gleaner_sheet()
    return out
