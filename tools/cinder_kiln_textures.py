"""Textures for the Cinder Kiln (tools/cinder_kiln.py, docs/features/cinder-kiln.md): its lair-only blocks (the dome's
kiln brick, the bowl's cracked basalt, the molten slag, the troughs' stone and water, and the sluice gates' frame, panels,
gushing water and wheel) and the Kiln Seal's icon (its map, tools/item_icons/kiln_seal.txt). Painted here by code,
16 x 16, in the manner of the vanilla blocks (tools/block_style.py) and the Witching Season's clean style: a few flat
tones a material, and patterns (the brick courses, the basalt's plates, the slag's crust, the flagstones, the rivets)
laid out whole within the block rather than as noise. Called from tools/lair_textures.py. No Mojang texture is read,
traced or copied.
"""
import math
import random

from PIL import Image

import block_style as bs
import item_icons


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


FIREBRICK = pal("3a2a20", "8a6a48", "a8865c", "c4a274", "dabd8e", "ecd6ac")   # buff firebrick, a joint darkest
SOOT = pal("2a221e", "4a3c32", "6a5644")
BASALT = pal("1c1b1f", "28272c", "34333a", "424148", "54535b")
EMBER = pal("4a140a", "7a240e", "b0421a", "e0782c")                          # deep in a crack up to its glow
SLAG = pal("3a120a", "5e1e0e", "8a3212", "c85a18", "f08c22", "ffc04a", "fff0a0")
FLAG = pal("2e2e30", "4c4c50", "5e5e62", "707074", "84848a", "9a9aa0")        # the troughs' flagstones
WATER = [(40, 86, 140, 168), (52, 104, 162, 168), (70, 128, 186, 172), (120, 176, 220, 180), (200, 230, 248, 196)]
IRON = pal("16171a", "26282c", "36393e", "474b51", "5c6067", "767b82")
RUST = pal("4a2412", "6e3818", "8e4a20")
WHEEL = pal("2a0c0a", "5a1612", "8a2418", "b23a26", "d6644a")                  # the wheel's red paint


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def put(img, x, y, c):
    img.putpixel((int(x) % 16, int(y) % 16), c)


def wrapped(ax, ay, bx, by):
    """The distance between two points on the wrapping 16 x 16 tile."""
    dx = min(abs(ax - bx), 16 - abs(ax - bx))
    dy = min(abs(ay - by), 16 - abs(ay - by))
    return math.hypot(dx, dy)


def cells(seed, count, spacing):
    """`count` points scattered over the tile, none nearer another than `spacing` (on the wrapping tile, so the pattern
    has no grid to it), and for each pixel the nearest one, how much nearer it is than the next, and its offset from it:
    plates and floes that tile without seams."""
    rng = random.Random(seed)
    pts = []
    tries = 0
    while len(pts) < count:
        x, y = rng.random() * 16, rng.random() * 16
        if all(wrapped(x, y, px, py) >= spacing for px, py in pts):
            pts.append((x, y))
        tries += 1
        if tries % 2000 == 0:
            spacing *= 0.95  # no room left at this spacing: close up a little
    out = {}
    for y in range(16):
        for x in range(16):
            ds = []
            for k, (px, py) in enumerate(pts):
                dx = min(abs(x + 0.5 - px), 16 - abs(x + 0.5 - px))
                dy = min(abs(y + 0.5 - py), 16 - abs(y + 0.5 - py))
                ds.append((math.hypot(dx, dy), k, (x + 0.5 - px + 8) % 16 - 8, (y + 0.5 - py + 8) % 16 - 8))
            ds.sort()
            out[(x, y)] = (ds[0][1], ds[1][0] - ds[0][0], ds[0][2], ds[0][3])
    return pts, out


# ---------------------------------------------------------------- the dome

def kiln_brick():
    """Firebrick laid in four courses of two, buff with a lit top edge and a dark joint below and to the right, a few
    bricks glazed darker by the heat, and soot breathed up the joints from below."""
    img = bs.img(bs.bricks(FIREBRICK, seed=701, rows=4, cols=2, mortar=SOOT[0]))
    rng = random.Random(702)
    glazed = {(0, 1), (3, 0), (2, 1)}       # (course, brick) darkened by the heat
    for y in range(16):
        course = y // 4
        for x in range(16):
            u = (x + (4 if course % 2 else 0)) % 16
            if (course, u // 8) in glazed and y % 4 != 3 and u % 8 != 7:
                c = img.getpixel((x, y))
                img.putpixel((x, y), tuple(int(v * 0.78) for v in c[:3]) + (255,))
    for _ in range(6):
        x, y = rng.randrange(16), rng.choice((3, 7, 11, 15))
        put(img, x, y - 1, SOOT[1])
    return img


# ---------------------------------------------------------------- the bowl

def cracked_basalt_top():
    """The bowl's floor: plates of dark basalt, each lit along its upper left, set in cracks that glow from deep red at
    their edges to ember orange where they are widest."""
    pts, near = cells(711, 8, 4.4)
    img = new()
    tone = [random.Random(712 + k).choice((1, 2, 2, 3)) for k in range(len(pts))]
    for (x, y), (k, gap, dx, dy) in near.items():
        if gap < 0.9:
            img.putpixel((x, y), EMBER[2] if gap < 0.35 and (x * 3 + y) % 4 else EMBER[1])
        elif gap < 1.6:
            img.putpixel((x, y), BASALT[0])
        else:
            t = tone[k] + (1 if dx + dy < -2.0 else 0) - (1 if dx + dy > 2.4 else 0)
            img.putpixel((x, y), BASALT[max(1, min(4, t))])
    return img


def cracked_basalt_side():
    """Its sides: basalt in upright columns, lit and shaded in turn, crossed by two cracks with a glow deep in them."""
    img = bs.img(bs.streaks(BASALT[1:], seed=721, vertical=True, across=2.0, along=12.0, spread=0.7))
    for x in range(16):
        if x % 4 == 0:
            for y in range(16):
                img.putpixel((x, y), BASALT[0])
    for points in (((0, 5), (4, 6), (9, 4), (16, 5)), ((2, 12), (7, 11), (12, 13), (16, 12))):
        for (ax, ay), (bx, by) in zip(points, points[1:]):
            steps = max(abs(bx - ax), abs(by - ay))
            for i in range(steps):
                x = round(ax + (bx - ax) * i / steps)
                y = round(ay + (by - ay) * i / steps)
                put(img, x, y, EMBER[1])
                put(img, x, y + 1, BASALT[0])
    return img


def molten_slag():
    """Molten slag: floes of dark crust drifting on it, each its own dull red and cooling darker toward its middle, and
    between them the molten seams, white-yellow where they are widest, orange at their edges."""
    pts, near = cells(731, 7, 4.5)
    img = new()
    for (x, y), (k, gap, dx, dy) in near.items():
        d = math.hypot(dx, dy)
        if gap < 0.7:
            img.putpixel((x, y), SLAG[6] if gap < 0.25 else SLAG[5])
        elif gap < 1.5:
            img.putpixel((x, y), SLAG[4])
        elif gap < 2.2:
            img.putpixel((x, y), SLAG[3])
        else:
            img.putpixel((x, y), SLAG[1] if d < 1.2 + (k % 3) * 0.5 else SLAG[2])
    return img


# ---------------------------------------------------------------- the troughs

def trough_stone_top():
    """The troughs' paving: flagstones in two courses, each its own grey, lit at its top edge, worn smooth and pale down
    the middle where the water runs, and set in dark joints."""
    img = new()
    g = bs.grain(16, 16, 741, 2.0, 5.0)
    for y in range(16):
        for x in range(16):
            course = y // 8
            u = (x + (5 if course else 0)) % 16
            joint = y % 8 == 7 or u in (0, 9)
            if joint:
                img.putpixel((x, y), FLAG[0])
                continue
            k = 2 + (1 if g(x, y) > 0.62 else 0) + (1 if 5 <= x <= 10 else 0)
            if y % 8 == 0:
                k += 1
            img.putpixel((x, y), FLAG[min(5, k)])
    return img


def trough_stone_side():
    """Its sides: the paving's thickness at the top, a dark joint, then coursed stone below."""
    img = bs.img(bs.bricks(FLAG[:5], seed=751, rows=2, cols=1, mortar=FLAG[0]))
    for x in range(16):
        img.putpixel((x, 3), FLAG[0])
        for y in range(3):
            img.putpixel((x, y), FLAG[4] if y == 0 else FLAG[3])
    return img


def trough_water():
    """The water of a flooded trough, see-through: blue in soft clumps, ripples running across it in pale arcs."""
    g = bs.grain(16, 16, 761, 2.0, 5.0)
    img = new()
    for y in range(16):
        for x in range(16):
            v = g(x, y)
            img.putpixel((x, y), WATER[0] if v < 0.35 else WATER[2] if v > 0.66 else WATER[1])
    for y0 in (2, 7, 12):
        for x in range(16):
            y = y0 + round(1.0 * math.sin(2 * math.pi * x / 16 + y0))
            if (x + y0) % 6 not in (0, 1):
                put(img, x, y, WATER[3])
    for x, y in ((4, 4), (11, 9), (6, 14)):
        put(img, x, y, WATER[4])
    return img


# ---------------------------------------------------------------- the sluices

def sluice_frame():
    """The gates' iron beams: dark iron plates, lit along the top and left, riveted at the corners, rust weeping from
    under the rivets."""
    img = bs.img(bs.metal(IRON, seed=771, panels=False, rivets=True))
    for x, y0 in ((3, 4), (12, 4), (3, 13)):
        for i in range(3):
            put(img, x, y0 + i, RUST[1] if i else RUST[2])
    return img


def sluice_panel():
    """A gate's panel: an iron plate with two ribs across it, a row of rivets along each, and a rust stain along its foot
    where the water stands against it."""
    img = bs.img(bs.metal(IRON, seed=781, panels=False, rivets=False))
    for y in (4, 11):
        for x in range(16):
            img.putpixel((x, y), IRON[5] if x % 4 else IRON[4])
            img.putpixel((x, y + 1), IRON[1])
        for x in (2, 6, 10, 14):
            img.putpixel((x, y - 1), IRON[5])
            img.putpixel((x, y + 2), IRON[2])
    for x in range(16):
        img.putpixel((x, 15), RUST[0] if x % 3 else RUST[1])
        if x % 5 in (1, 2):
            img.putpixel((x, 14), RUST[1])
    return img


def sluice_slag():
    """Slag choking a gate, cut out over its panel: forced through the seams under its two ribs, beading along them,
    and oozing from under its foot in tongues of different lengths; bright where it is thickest."""
    img = new()
    for y in (6, 13):
        for x in range(16):
            img.putpixel((x, y), SLAG[4] if x % 3 else SLAG[5])
    for x in range(1, 16, 4):
        for y in (7, 14):
            img.putpixel((x, y), SLAG[3])
    for x in range(16):
        tongue = (3, 1, 2, 4, 1, 2, 3, 1, 4, 2, 1, 3, 2, 1, 3, 2)[x]
        for y in range(16 - tongue, 16):
            img.putpixel((x, y), SLAG[5] if y == 15 else SLAG[4] if y > 15 - tongue + 1 else SLAG[3])
    return img


def sluice_water():
    """Water gushing from an open gate, see-through: pale streaks running down it over blue, a white foam band at the
    top where it spills from under the raised panel."""
    img = new()
    rng = random.Random(791)
    columns = [rng.choice((1, 1, 2, 3)) for _ in range(16)]
    for x in range(16):
        for y in range(16):
            k = columns[x]
            if (y + x * 5) % 7 == 0:
                k = min(4, k + 1)
            img.putpixel((x, y), WATER[k])
        for y in (0, 1):
            img.putpixel((x, y), WATER[4] if (x + y) % 3 else WATER[3])
    return img


def sluice_wheel():
    """A gate's wheel, cut out: a rim painted red, lit along its top left, four spokes to an iron hub."""
    img = new()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if 5.6 <= r <= 7.4:
                lit = dx + dy < -2.0
                dark = dx + dy > 3.0
                img.putpixel((x, y), WHEEL[4] if lit else WHEEL[1] if dark else WHEEL[3] if r < 6.6 else WHEEL[2])
            elif r < 5.6 and (abs(dx) < 0.8 or abs(dy) < 0.8):
                img.putpixel((x, y), WHEEL[3] if dx + dy < 0 else WHEEL[2])
    for x in range(6, 10):
        for y in range(6, 10):
            img.putpixel((x, y), IRON[4] if x + y < 14 else IRON[2])
    img.putpixel((7, 7), IRON[5])
    return img


def cinder_kiln_textures():
    return {
        ("block", "kiln_brick"): kiln_brick(),
        ("block", "cracked_basalt_top"): cracked_basalt_top(),
        ("block", "cracked_basalt_side"): cracked_basalt_side(),
        ("block", "molten_slag"): molten_slag(),
        ("block", "trough_stone_top"): trough_stone_top(),
        ("block", "trough_stone_side"): trough_stone_side(),
        ("block", "trough_water"): trough_water(),
        ("block", "sluice_frame"): sluice_frame(),
        ("block", "sluice_panel"): sluice_panel(),
        ("block", "sluice_water"): sluice_water(),
        ("block", "sluice_slag"): sluice_slag(),
        ("block", "sluice_wheel"): sluice_wheel(),
        ("item", "kiln_seal"): item_icons.draw("kiln_seal"),
    }


if __name__ == "__main__":
    import sys
    sheet = cinder_kiln_textures()
    keys = sorted(sheet)
    scale, cols = 8, 4
    rows = (len(keys) + cols - 1) // cols
    image = Image.new("RGBA", (cols * (16 * scale + 8), rows * (16 * scale + 8)), (40, 44, 52, 255))
    for i, key in enumerate(keys):
        tile = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        image.paste(tile, ((i % cols) * (16 * scale + 8), (i // cols) * (16 * scale + 8)), tile)
    image.save(sys.argv[1] if len(sys.argv) > 1 else "cinder_kiln_textures.png")
    print(len(keys), "textures")
