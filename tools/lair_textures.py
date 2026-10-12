"""Textures for the lairs (tools/lairs.py, docs/features/hollow-acre.md): the Hollow Acre's lair-only blocks (blighted
soil, black wheat, mown stubble, the soul brazier, the harvest moon and the Grey Mist), the Mourning Wreath and the Death
Knell; from tools/spindle_loft_textures.py, the Spindle Loft's blocks and the Cursed Spindle; from
tools/glacier_hall_textures.py, the Glacier Hall's blocks and the Frost Horn; and from tools/cinder_kiln_textures.py, the
Cinder Kiln's blocks and the Kiln Seal. Painted here by code, 16 x 16, in the clean
style of the Witching Season: a few flat tones per material, crisp edges, no noise for its own sake. Called from
tools/crop_textures.py. No Mojang texture is read, traced or copied.
"""
import math
import random

from PIL import Image


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


EARTH = pal("17121a", "211a24", "2b222e", "362b38", "453646")
ASH = pal("5a5060", "7a7080", "9c94a0")
WHEAT = pal("0d0b0f", "18141b", "241e28", "332a37", "463a4a")
EAR = pal("17131c", "231d2b", "302839", "4a3d5a")  # near-black, a violet sheen at the tip
STUBBLE = pal("3a2a1c", "5a4229", "7a5c38", "9a7a4c", "b89a66")
STONE = pal("1c1a20", "28252d", "353139", "444049", "57525c")
IRON = pal("101012", "1c1c20", "2a2a30", "3a3a42", "50505a")
COALS = pal("0c0d10", "16242a", "1f4a52", "2d7f88", "57c6cc")
SOUL = pal("0f5a66", "1d8c99", "3fc3cc", "86ebef", "d4fbfb")
MOON = pal("b88a3a", "d4a650", "e8c06a", "f4d690", "fbe9be")
BLOOD_MOON = pal("4e0d0d", "711616", "962020", "b8322a", "d65a46")
MIST = [(150, 146, 156, 70), (176, 172, 182, 100), (200, 196, 206, 130), (226, 222, 232, 165)]
LEAF = pal("12201a", "1c3226", "284632", "385c40")
LILY = pal("b9b4bf", "dcd8e2", "f6f4f8")
VIOLET = pal("3a1f4c", "5a3274", "7c4c9c")
RIBBON = pal("0a080c", "18131c", "2a2230")
GOLD = pal("5a3a10", "8a5e1c", "b8862c", "e0b048", "f6d880")
BONE = pal("6e6656", "958c78", "bcb39c", "ddd6c2")


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def put(img, x, y, c):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c)


# ---------------------------------------------------------------- blighted soil

def soil(seed, top=False):
    """Black earth in a few clean tones: clods of the lighter tones on the dark ground, and on top pale ash flecks."""
    rng = random.Random(seed)
    img = new(EARTH[1])
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        w = rng.choice((2, 2, 3))
        tone = rng.choice((2, 2, 3))
        for dx in range(w):
            put(img, x + dx, y, EARTH[tone])
        put(img, x, y + 1, EARTH[0])
        if w == 3:
            put(img, x + 1, y - 1, EARTH[tone + 1] if tone < 4 else EARTH[4])
    for _ in range(8):
        put(img, rng.randrange(16), rng.randrange(16), EARTH[0])
    if top:
        for _ in range(7):
            x, y = rng.randrange(16), rng.randrange(16)
            put(img, x, y, rng.choice(ASH))
    return img


def soil_side():
    img = soil(31)
    rng = random.Random(32)
    # The crust: the top three rows darker, broken unevenly into the earth below, with ash at its lip.
    for x in range(16):
        depth = 2 + (1 if rng.random() < 0.5 else 0)
        for y in range(depth):
            img.putpixel((x, y), EARTH[0] if y < depth - 1 else EARTH[2])
        if rng.random() < 0.3:
            img.putpixel((x, 0), ASH[0])
    return img


# ---------------------------------------------------------------- the blighted crops

def black_wheat():
    """Blighted wheat, ripe and dead: dark stalks crossing upward, heavy grey ears bowed at their tops."""
    img = new()
    rng = random.Random(41)
    stalks = [1, 3, 5, 7, 9, 11, 13, 14]
    for i, x0 in enumerate(stalks):
        height = 10 + rng.randrange(5)
        lean = rng.choice((-1, 0, 0, 1))
        x = float(x0)
        top = 16 - height
        for y in range(15, top - 1, -1):
            t = (15 - y) / max(1, height)
            put(img, x, y, WHEAT[2 if t < 0.5 else 3])
            if t > 0.4 and rng.random() < 0.25:
                x += lean * 0.5
        # The ear: a bowed head of grain over the stalk's top.
        ex = x + lean
        for j in range(4):
            put(img, ex, top - 1 + j, EAR[3 if j == 0 else 2])
            put(img, ex + (1 if (i % 2) else -1), top + j, EAR[1])
        put(img, ex, top + 4, EAR[0])
        # A blade or two of dead leaf.
        if rng.random() < 0.6:
            ly = 15 - rng.randrange(3, 7)
            for k in range(3):
                put(img, x0 + (k + 1) * (1 if i % 2 else -1), ly - k // 2, WHEAT[1 + (k > 0)])
    return img


def stubble():
    """Cut stubble: short dun stalks standing in rows, their cut ends pale."""
    img = new()
    rng = random.Random(51)
    for x in range(0, 16):
        if rng.random() < 0.55:
            h = 2 + rng.randrange(3)
            for y in range(15, 15 - h, -1):
                put(img, x, y, STUBBLE[1 if y > 15 - h + 1 else 2])
            put(img, x, 15 - h, STUBBLE[4])
            if rng.random() < 0.2:
                put(img, x, 14 - h, STUBBLE[3])
    return img


# ---------------------------------------------------------------- the soul brazier

def brazier_stone():
    """Black stone in courses, each course's top edge lit and its joint dark."""
    img = new(STONE[2])
    for y in range(16):
        course = y // 4
        if y % 4 == 0:
            for x in range(16):
                img.putpixel((x, y), STONE[0])
        elif y % 4 == 1:
            for x in range(16):
                img.putpixel((x, y), STONE[3])
        offset = 0 if course % 2 == 0 else 4
        for x in range(offset, 16, 8):
            if y % 4 != 0:
                img.putpixel((x, y), STONE[1])
    return img


def brazier_iron():
    """Blackened iron: a rim highlight, rivets, a darker lower half."""
    img = new(IRON[2])
    for x in range(16):
        img.putpixel((x, 0), IRON[4])
        img.putpixel((x, 1), IRON[3])
        img.putpixel((x, 15), IRON[0])
        for y in range(10, 15):
            img.putpixel((x, y), IRON[1])
    for x in range(2, 16, 5):
        img.putpixel((x, 4), IRON[4])
        img.putpixel((x, 5), IRON[0])
    return img


def brazier_coals():
    """Coals glowing soul-blue in their cracks."""
    img = new(COALS[0])
    rng = random.Random(61)
    for _ in range(40):
        x, y = rng.randrange(16), rng.randrange(16)
        img.putpixel((x, y), COALS[rng.choice((1, 1, 2, 3))])
    for _ in range(8):
        img.putpixel((rng.randrange(16), rng.randrange(16)), COALS[4])
    return img


def soul_flame():
    """A soul flame: a teardrop of blue-white fire, hottest at its heart, its tip licking up in two tongues."""
    img = new()
    for y in range(16):
        for x in range(16):
            # Distance inside a teardrop rising from the bottom.
            t = (15 - y) / 15.0
            half = 6.5 * math.sin(math.pi * min(1.0, 0.15 + t * 0.95)) * (1.0 - 0.55 * t)
            dx = abs(x + 0.5 - 8 + math.sin(t * 6.0) * 0.8 * t)
            if dx > half:
                continue
            k = 1.0 - dx / max(half, 0.01)
            heat = k * 0.7 + (1.0 - t) * 0.5
            idx = 0 if heat < 0.35 else 1 if heat < 0.55 else 2 if heat < 0.75 else 3 if heat < 0.92 else 4
            img.putpixel((x, y), SOUL[idx])
    # The two tongues at the tip.
    for y, x in ((1, 6), (2, 6), (0, 9), (1, 9), (2, 9)):
        img.putpixel((x, y), SOUL[1])
    return img


# ---------------------------------------------------------------- the moon and the mist

def moon(colours, seed):
    """A tile of the harvest moon's face: the body in two tones, soft maria and a few sharp craters."""
    rng = random.Random(seed)
    img = new(colours[2])
    for _ in range(3):
        cx, cy, r = rng.uniform(2, 14), rng.uniform(2, 14), rng.uniform(2.5, 4.5)
        for y in range(16):
            for x in range(16):
                for ox in (-16, 0, 16):
                    for oy in (-16, 0, 16):
                        if math.hypot(x + ox - cx, y + oy - cy) <= r:
                            img.putpixel((x, y), colours[1])
    for _ in range(4):
        cx, cy = rng.randrange(1, 15), rng.randrange(1, 15)
        put(img, cx, cy, colours[0])
        put(img, cx + 1, cy, colours[1])
        put(img, cx, cy - 1, colours[4])
        put(img, cx - 1, cy - 1, colours[3])
    for _ in range(10):
        put(img, rng.randrange(16), rng.randrange(16), colours[3])
    return img


def mist():
    """Grey Mist: soft see-through bands drifting across, densest at the middle."""
    img = new()
    rng = random.Random(71)
    for y in range(16):
        band = 0.5 + 0.5 * math.sin(y * 0.9 + 0.4)
        for x in range(16):
            wave = 0.5 + 0.5 * math.sin(x * 0.55 + y * 0.35 + rng.random() * 0.4)
            v = band * 0.55 + wave * 0.45
            idx = 0 if v < 0.35 else 1 if v < 0.6 else 2 if v < 0.8 else 3
            img.putpixel((x, y), MIST[idx])
    return img


# ---------------------------------------------------------------- the wreath and the knell

def wreath(item=False):
    """A Mourning Wreath from above: a ring of dark leaves, pale lilies and a violet bloom round it, a black ribbon bow
    at the bottom with its tails trailing."""
    img = new()
    cx, cy = 7.5, 7.0
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, (y - cy) * 1.05)
            if 4.2 <= d <= 6.9:
                a = math.atan2(y - cy, x - cx)
                k = (math.sin(a * 9.0) + 1) / 2
                img.putpixel((x, y), LEAF[1 + int(k * 2.9)] if d < 6.2 else LEAF[0])
    # The flowers round the ring: lilies, with a violet bloom at the top.
    for i in range(6):
        a = -math.pi / 2 + i * (2 * math.pi / 6) + 0.25
        fx, fy = cx + math.cos(a) * 5.5, cy + math.sin(a) * 5.5
        petals = VIOLET if i == 0 else LILY
        for dx, dy, k in ((0, 0, 2), (1, 0, 1), (-1, 0, 1), (0, 1, 1), (0, -1, 1)):
            put(img, fx + dx, fy + dy, petals[k])
        put(img, fx, fy, petals[2] if i else LILY[2])
    # The ribbon bow and its tails.
    bx, by = 7, 12
    for dx, dy in ((-2, 0), (-1, 0), (1, 0), (2, 0), (-2, -1), (2, -1), (0, 0)):
        put(img, bx + dx, by + dy, RIBBON[1])
    put(img, bx, by, RIBBON[2])
    for k in range(3):
        put(img, bx - 1 - k // 2, by + 1 + k, RIBBON[0 if k == 2 else 1])
        put(img, bx + 1 + k // 2, by + 1 + k, RIBBON[0 if k == 2 else 1])
    return img


def death_knell():
    """The Death Knell: a gold handbell on a bone handle, its iron clapper showing under the lip."""
    img = new()
    # The handle, a bone shaft with a knob.
    for y in range(1, 6):
        put(img, 8, y, BONE[2])
        put(img, 7, y, BONE[1])
    put(img, 7, 0, BONE[3])
    put(img, 8, 0, BONE[2])
    put(img, 6, 1, BONE[1])
    put(img, 9, 1, BONE[1])
    # The bell: widening downward to a flared lip.
    rows = [(6, 9), (5, 10), (5, 10), (4, 11), (4, 11), (3, 12), (3, 12), (2, 13), (1, 14)]
    for i, (x0, x1) in enumerate(rows):
        y = 6 + i
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1)
            lit = x - x0 <= 1
            put(img, x, y, GOLD[1] if edge else GOLD[4] if lit and i < 7 else GOLD[3] if x - x0 <= (x1 - x0) // 2 else GOLD[2])
    for x in range(1, 15):
        put(img, x, 14, GOLD[0])
    # The clapper.
    put(img, 7, 15, IRON[3])
    put(img, 8, 15, IRON[2])
    return img


def lair_textures():
    from cinder_kiln_textures import cinder_kiln_textures
    from glacier_hall_textures import glacier_hall_textures
    from spindle_loft_textures import spindle_loft_textures
    return {
        **spindle_loft_textures(),
        **glacier_hall_textures(),
        **cinder_kiln_textures(),
        ("block", "blighted_soil_top"): soil(21, top=True),
        ("block", "blighted_soil_side"): soil_side(),
        ("block", "blighted_soil"): soil(23),
        ("block", "black_wheat"): black_wheat(),
        ("block", "mown_stubble"): stubble(),
        ("block", "lair_brazier_stone"): brazier_stone(),
        ("block", "lair_brazier_iron"): brazier_iron(),
        ("block", "lair_brazier_coals"): brazier_coals(),
        ("block", "lair_brazier_flame"): soul_flame(),
        ("block", "lair_moon"): moon(MOON, 81),
        ("block", "lair_moon_red"): moon(BLOOD_MOON, 81),
        ("block", "lair_exit"): mist(),
        ("block", "mourning_wreath"): wreath(),
        ("item", "mourning_wreath"): wreath(item=True),
        ("item", "death_knell"): death_knell(),
    }
