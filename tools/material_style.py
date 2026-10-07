"""Metal parts and powders in the manner of the vanilla items (docs/ART_DIRECTION.md, "Each material looks like its
vanilla counterpart"): dusts, plates, gears, wire, washed ore and raw lumps. The ores, storage blocks, raw ores, ingots
and nuggets are the material sets (#218, docs/MATERIAL_SETS.md), not drawn here.

Every pixel comes from code here: no Mojang texture is read, traced or recoloured. What follows the vanilla items is
the manner only: drawn on the diagonal or face on, lit from the top left, outlined in each part's own dark tone
(docs/ITEM_ICONS.md), with a few flat tones and no random speckle.

Palettes run darkest first: metal palettes have five tones, lump tones four (dark, mid, light, highlight).
"""
import math

from PIL import Image


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c[:3]) + (255,))


# ---------------------------------------------------------------- items

def lumps(circles, palette, outline=True):
    """Rounded lumps (cx, cy, r), later ones in front: each shaded by its own centre (lit at the upper left), the
    joins between lumps one tone down, the whole outlined in the darkest tone. `palette` has five tones."""
    owner = {}
    for index, (cx, cy, r) in enumerate(circles):
        for y in range(16):
            for x in range(16):
                if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                    owner[(x, y)] = index
    img = new()
    for (x, y), index in owner.items():
        cx, cy, r = circles[index]
        d = (x + 0.5 - cx) + (y + 0.5 - cy)
        k = 3 if d < -r * 0.55 else 1 if d > r * 0.6 else 2
        if any(owner.get(n, index) > index for n in ((x + 1, y), (x, y + 1))):
            k = 1
        if outline and any(n not in owner for n in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))):
            k = 0
        _put(img, x, y, palette[k])
    for cx, cy, r in circles:
        if r >= 1.9:
            hx, hy = int(cx - r * 0.45), int(cy - r * 0.45)
            if owner.get((hx, hy)) is not None and owner.get((hx - 1, hy)) is not None and owner.get((hx, hy - 1)) is not None:
                _put(img, hx, hy, palette[4])
    return img


RAW_LUMPS = [(5.8, 10.2, 3.3), (10.6, 6.6, 3.1), (6.6, 5.6, 2.5), (10.6, 11.0, 2.6), (3.4, 12.2, 1.6)]


def raw_item(tones, outline=None):
    """Raw ore, as the vanilla raw metals: a lumpy chunk of four rounded lumps."""
    dark, mid, light, high = tones
    edge = outline or tuple(max(0, int(v * 0.6)) for v in dark)
    return lumps(RAW_LUMPS, [edge, dark, mid, light, high])


PILE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......####......",
    "....########....",
    "...##########...",
    "..############..",
    "..############..",
    ".##############.",
    ".##############.",
    "..############..",
    "................",
    "................",
]
# Grains a tone lighter (+) or darker (-) on a heap, at fixed places so every dust shares one clean pattern.
GRAINS = {(7, 8): 1, (5, 9): 1, (10, 9): -1, (4, 11): 1, (8, 11): -1, (12, 11): -1, (6, 12): -1, (11, 12): -1,
          (9, 7): 1}


def dust(palette):
    """A heap of powder: lit on its upper left, shaded towards its lower right, a few grains a tone apart, outlined
    one tone down from the shade."""
    cells = {(x, y) for y, row in enumerate(PILE) for x, ch in enumerate(row) if ch == "#"}
    img = new()
    for x, y in cells:
        if any(n not in cells for n in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))):
            _put(img, x, y, palette[1] if (x, y - 1) in cells or x > 7 else palette[2])
            continue
        k = 3 if x + y < 15 else 1 if x - y > 0 or y >= 12 else 2
        k = max(1, min(4, k + GRAINS.get((x, y), 0)))
        _put(img, x, y, palette[k])
    return img


def plate(palette):
    """A flat plate seen face on: outlined, lit along its top and left, shaded along its bottom and right, with two
    soft diagonal sheens."""
    img = new()
    for y in range(3, 13):
        for x in range(2, 14):
            if x in (2, 13) or y in (3, 12):
                c = palette[0]
            elif x == 3 or y == 4:
                c = palette[4]
            elif x == 12 or y == 11:
                c = palette[1]
            elif (x - y) in (2, 3) or (x - y) == 7:
                c = palette[3]
            else:
                c = palette[2]
            _put(img, x, y, c)
    return img


def _gear_cells():
    cells = set()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy)
            a = math.degrees(math.atan2(dy, dx)) % 45
            tooth = a < 13 or a > 32
            if 1.8 < r <= 5.4 or (r <= 7.4 and tooth and r > 1.8):
                cells.add((x, y))
    return cells


def gear(palette):
    """A gear of eight square teeth round an axle hole: outlined, lit on its upper left, shaded on its lower right."""
    cells = _gear_cells()
    img = new()
    for x, y in cells:
        d = (x + 0.5 - 8) + (y + 0.5 - 8)
        if any(n not in cells for n in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))):
            c = palette[0]
        else:
            c = palette[4] if d < -4 else palette[1] if d > 3 else palette[3] if d < 0 else palette[2]
        _put(img, x, y, c)
    return img


SPOOL = [(150, 112, 70), (112, 80, 48), (78, 54, 32)]


def wire(palette):
    """A spool of wire: two wooden flanges with the wire wound neatly between them and its end trailing off."""
    img = new()
    for y in (2, 3, 12, 13):
        for x in range(3, 13):
            c = SPOOL[0] if y in (2, 12) else SPOOL[1]
            if x in (3, 12):
                c = SPOOL[2]
            _put(img, x, y, c)
    for y in range(4, 12):
        for x in range(4, 12):
            c = palette[3] if y % 2 == 0 else palette[2]
            if x == 4:
                c = palette[4] if y % 2 == 0 else palette[3]
            elif x == 11:
                c = palette[1]
            _put(img, x, y, c)
        _put(img, 3, y, palette[0])
        _put(img, 12, y, palette[0])
    for x, y in ((12, 10), (13, 11), (14, 11), (15, 12)):
        _put(img, x, y, palette[2])
    return img


WATER = [(96, 160, 214), (170, 218, 246)]


def washed(palette):
    """Washed ore: the clean chunk in the metal's own tones, still wet, with three drops of water on it."""
    img = raw_item(palette[1:5], palette[0])
    for x, y in ((6, 6), (10, 9), (5, 11)):
        _put(img, x, y, WATER[0])
        _put(img, x, y - 1, WATER[1])
    return img


def blend(copper, tin):
    """Bronze blend: a heap of copper powder with small clumps of tin powder through it."""
    img = dust(copper)
    for x, y, k in ((6, 8, 3), (7, 8, 2), (10, 10, 2), (4, 11, 3), (5, 11, 2), (9, 12, 1), (12, 11, 2)):
        _put(img, x, y, tin[k])
    return img
