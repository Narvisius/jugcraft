"""Textures for the Glacier Hall (tools/glacier_hall.py, docs/features/glacier-hall.md): its lair-only blocks (the lake's
drift snow and glare ice, the trampled snow and its half block, the giant icicles, the mammoth tusks' ivory and the
frozen hoard) and the Frost Horn's icon (its map, tools/item_icons/frost_horn.txt). Painted here by code, 16 x 16, in
the manner of the vanilla blocks (tools/block_style.py) and the Witching Season's clean style: a few flat tones a
material, and patterns (the wind's ripples, the ice's cracks, the ivory's growth lines, the hoard's coins) laid out
whole within the block rather than as noise. Called from tools/lair_textures.py. No Mojang texture is read, traced or
copied.
"""
import math

from PIL import Image

import block_style as bs
import item_icons


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


CRUST = pal("b4c8dc", "c6d7e7", "d8e4ef", "e7eff6", "f5f9fc")      # drift snow: a lee's blue shadow up to a crest
TRAMPLED = pal("a7b3bf", "b7c2cd", "c6cfd8", "d4dbe2", "e1e7ec")   # snow packed hard, greyed by feet
SNOW = pal("d2dee8", "e0e9f1", "ecf2f7", "f7fafc")
GLARE = pal("1d4680", "285a98", "356ead", "4683c2", "6aa4d8", "bcdcf2")
ICICLE = pal("8fbadb", "a8cbe7", "bfdaf0", "d6e9f7", "eef7fd")
IVORY = pal("a08b66", "bba67e", "cfbd96", "e0d2b1", "eee5cc")
HOARD_ICE = pal("86abd0", "9cbcdc", "b2cde7", "c8ddf0", "dceaf6")
GOLD = pal("7a5212", "a87820", "d4a234", "eecb62", "f9e7a8")
RUBY = pal("7a1424", "b42a3a", "e0626a")
EMERALD = pal("135a38", "23885a", "5cc48e")


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def put(img, x, y, c):
    img.putpixel((int(x) % 16, int(y) % 16), c)


def plain(palette, seed, spread):
    """A plain surface of `palette` in small clumps (tools/block_style.py surface)."""
    colour = bs.surface(palette, seed=seed, spread=spread)
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), tuple(colour(x, y)[:3]) + (255,))
    return img


# ---------------------------------------------------------------- snow

def drift_snow():
    """The lake's crust of drift snow: wind ripples across it every eight pixels, each a bright crest, a lit slope and a
    blue shadow in its lee, wandering a little as they run; the ripples tile across and down the lake."""
    grain = bs.grain(16, 16, 601)
    img = new()
    for y in range(16):
        for x in range(16):
            wave = 1.5 * math.sin(2 * math.pi * x / 16) + 0.7 * math.sin(4 * math.pi * x / 16 + 1.3)
            frac = ((y + wave) / 8.0 + (grain(x, y) - 0.5) * 0.18) % 1.0
            k = 4 if frac < 0.12 else 3 if frac < 0.5 else 2 if frac < 0.74 else 1 if frac < 0.92 else 0
            img.putpixel((x, y), CRUST[k])
    return img


def trampled_top():
    """Snow trampled hard: packed and a little grey, in clumps, scuffed by feet in a few short strokes."""
    img = plain(TRAMPLED, 611, 0.6)
    for x0, y0, dx, dy, n in ((2, 4, 1, 0, 3), (10, 2, 1, 1, 3), (9, 11, 1, 0, 3), (3, 12, 1, -1, 2)):
        for i in range(n):
            put(img, x0 + dx * i, y0 + dy * i, TRAMPLED[1])
            put(img, x0 + dx * i, y0 + dy * i - 1, TRAMPLED[3])
    return img


def trampled_side():
    """The side of trampled snow: its packed crust along the top, white snow below in faint layers."""
    img = plain(SNOW, 612, 0.6)
    for x in range(16):
        crust = 2 + (1 if (x * 7) % 5 == 0 else 0)
        for y in range(crust):
            img.putpixel((x, y), TRAMPLED[2] if y < crust - 1 else TRAMPLED[1])
        for y in (7, 12):
            if (x + y) % 6 != 0:
                img.putpixel((x, y), SNOW[0])
    return img


# ---------------------------------------------------------------- ice

def glare_ice():
    """Bare lake ice, scoured and polished: deep blue in soft clumps, a few pale streaks of polish running across it, two
    long white cracks frozen in it and small bubbles, each a white speck with a dark fleck below. Everything repeats
    within the block, so the ice runs on unbroken."""
    grain = bs.grain(16, 16, 621, fine=3.0, coarse=8.0)
    img = new()
    for y in range(16):
        for x in range(16):
            v = grain(x, y)
            img.putpixel((x, y), GLARE[1] if v < 0.42 else GLARE[3] if v > 0.62 else GLARE[2])
    for x in range(16):
        for y0 in (3, 11):
            y = y0 + round(1.2 * math.sin(2 * math.pi * x / 16 + y0))
            if (x + y0) % 5 != 0:
                put(img, x, y, GLARE[4] if (x + y0) % 3 else GLARE[3])
    for points in (((2, 6), (5, 7), (8, 9), (10, 12)), ((11, 2), (12, 4), (14, 5))):
        for (ax, ay), (bx, by) in zip(points, points[1:]):
            steps = max(abs(bx - ax), abs(by - ay))
            for i in range(steps + 1):
                x = round(ax + (bx - ax) * i / steps)
                y = round(ay + (by - ay) * i / steps)
                put(img, x, y, GLARE[5])
    for x, y in ((6, 2), (13, 9), (3, 13), (9, 5)):
        put(img, x, y, GLARE[5])
        put(img, x, y + 1, GLARE[0])
    return img


def icicle():
    """A giant icicle's ice: pale and streaked down its length, its left edge catching the light."""
    img = bs.img(bs.streaks(ICICLE[:4], seed=631, vertical=True, across=1.2, along=10.0, spread=0.8))
    for y in range(16):
        for x in (0, 1):
            img.putpixel((x, y), ICICLE[4] if x == 0 or y % 5 else ICICLE[3])
        img.putpixel((15, y), ICICLE[0])
    return img


# ---------------------------------------------------------------- ivory and the hoard

def tusk():
    """Mammoth ivory: cream in soft clumps, three fine growth lines winding across it (each a shade with a light edge
    above), a hairline crack."""
    img = plain(IVORY[1:], 641, 0.6)
    for y0 in (3, 8, 13):
        for x in range(16):
            y = y0 + round(0.8 * math.sin(2 * math.pi * x / 16 + y0))
            put(img, x, y, IVORY[1])
            put(img, x, y - 1, IVORY[4])
    for x, y in ((5, 5), (6, 6), (6, 7)):
        put(img, x, y, IVORY[0])
    return img


def frozen_hoard():
    """Treasure frozen into the ice: gold coins lying every way, a goblet and two gems seen through pale blue ice, whose
    frost dulls them a little; all within the block, so the ice meets the next block's."""
    img = plain(HOARD_ICE[:4], 651, 0.7)

    def frosted(c):
        ice = HOARD_ICE[3]
        return tuple(int(round(c[i] * 0.8 + ice[i] * 0.2)) for i in range(3)) + (255,)

    # Coins: three pixels across, two down, rimmed and lit.
    for cx, cy in ((2, 2), (9, 1), (12, 6), (3, 9), (8, 12), (12, 13)):
        for dx in range(3):
            put(img, cx + dx, cy, frosted(GOLD[3] if dx == 0 else GOLD[2]))
            put(img, cx + dx, cy + 1, frosted(GOLD[1] if dx == 2 else GOLD[2]))
        put(img, cx, cy, frosted(GOLD[4]))
    # A goblet: its cup, stem and foot.
    for y, (x0, x1) in ((4, (5, 8)), (5, (5, 8)), (6, (6, 7)), (7, (6, 7)), (8, (5, 8))):
        for x in range(x0, x1 + 1):
            tone = 3 if x == x0 else 1 if x == x1 else 2
            put(img, x, y, frosted(GOLD[tone]))
    put(img, 6, 4, frosted(GOLD[4]))
    # Two gems.
    for (gx, gy), gem in (((10, 9), RUBY), ((1, 13), EMERALD)):
        put(img, gx, gy, frosted(gem[2]))
        put(img, gx + 1, gy, frosted(gem[1]))
        put(img, gx, gy + 1, frosted(gem[1]))
        put(img, gx + 1, gy + 1, frosted(gem[0]))
    return img


def glacier_hall_textures():
    return {
        ("block", "drift_snow"): drift_snow(),
        ("block", "trampled_snow"): trampled_top(),
        ("block", "trampled_snow_side"): trampled_side(),
        ("block", "glare_ice"): glare_ice(),
        ("block", "giant_icicle"): icicle(),
        ("block", "mammoth_tusk"): tusk(),
        ("block", "frozen_hoard"): frozen_hoard(),
        ("item", "frost_horn"): item_icons.draw("frost_horn"),
    }


if __name__ == "__main__":
    import sys
    sheet = glacier_hall_textures()
    keys = sorted(sheet)
    scale, cols = 8, 4
    rows = (len(keys) + cols - 1) // cols
    image = Image.new("RGBA", (cols * (16 * scale + 8), rows * (16 * scale + 8)), (40, 44, 52, 255))
    for i, key in enumerate(keys):
        tile = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        image.paste(tile, ((i % cols) * (16 * scale + 8), (i // cols) * (16 * scale + 8)), tile)
    image.save(sys.argv[1] if len(sys.argv) > 1 else "glacier_hall_textures.png")
    print(len(keys), "textures")
