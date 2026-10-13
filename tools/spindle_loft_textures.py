"""Textures for the Spindle Loft (tools/spindle_loft.py, docs/features/spindle-loft.md): its lair-only blocks (the doily's
four laces, the spools' wood and their five threads, the pincushion's fabric, seam and felt leaf, needle steel, the pins,
the measuring tape, the thimble's brass and the grimy skylight) and the Cursed Spindle's icon (its map,
tools/item_icons/cursed_spindle.txt). Painted here by code, 16 x 16, in the manner of the vanilla blocks
(tools/block_style.py) and the Witching Season's clean style: a few flat tones a material, and patterns (the lace, the
wound thread, the tape's ticks, the thimble's dimples) on an even lattice rather than noise. Called from
tools/lair_textures.py. No Mojang texture is read, traced or copied.
"""
import math

from PIL import Image

import block_style as bs
import item_icons
from lairs import LACE_PATTERNS, THREAD_COLOURS


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


LACE = pal("c7ba98", "e0d5b8", "f0e8d4", "fbf6ea")         # its shade, the thread, lit, highlight
WOOD = pal("94744a", "ad8b5a", "c4a26c", "d6b882")
THREADS = {
    "green": pal("1f4a2a", "2c6639", "3c8249", "57a062"),
    "blue": pal("1d2d66", "2a428a", "3a5aac", "5878c8"),
    "beige": pal("8a7656", "a6916a", "c0ab82", "d8c6a0"),
    "red": pal("5a1418", "7e1f26", "a03036", "c04c4e"),
    "white": pal("a8a49c", "c6c2ba", "dedbd4", "f2f0ec"),
}
VELVET = pal("8a1a20", "a12228", "b62c30", "c8403e")
FELT = pal("2a5a2a", "376f34", "458540", "569a4e")
STITCH = rgb("e6dac4") + (255,)
STEEL = pal("6e7884", "86909c", "a0a9b4", "bac2cc", "d6dce2")
TAPE = pal("c99a1e", "e2b22c", "eec442", "f6d464")
INK = rgb("3a3022") + (255,)
RED = pal("8a2620", "b8382c", "d4503e")
BRASS = pal("6e4c16", "966a24", "b88832", "d6a848", "eccb70")
GLASS = [(96, 102, 86, 190), (128, 136, 116, 150), (154, 164, 142, 118), (186, 196, 172, 104)]


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


# ---------------------------------------------------------------- the doily's lace

def lace(pattern):
    """The doily's lace, cut out (its holes show the dark below):
    0, the band: close cloth stitch, threads on a four-pixel lattice with a pinhole in each square;
    1, mesh: a diamond net of single threads, knots where they cross;
    2, flower: a six-petalled rosette, each petal pierced, on the net;
    3, the edge: rings of tatting linked into a chain by short bars.
    Lit from the top left: the thread's upper-left parts a tone lighter. Every pattern repeats within the block."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = None
            if pattern == 0:
                if not (x % 4 == 2 and y % 4 == 2):
                    c = LACE[0] if (x % 4 == 3 or y % 4 == 3) else LACE[2] if (x % 4 == 0 and y % 4 == 0) else LACE[1]
            elif pattern == 1:
                a, b = (x + y) % 4 == 0, (x - y) % 4 == 0
                if a and b:
                    c = LACE[3]
                elif a or b:
                    c = LACE[2] if a else LACE[1]
            elif pattern == 2:
                c = rosette(x, y)
            else:
                c = tatting(x, y)
            if c is not None:
                img.putpixel((x, y), c)
    return img


def rosette(x, y):
    px, py = x + 0.5 - 8.0, y + 0.5 - 8.0
    r = math.hypot(px, py)
    if r <= 1.9:
        return None if r < 0.8 else LACE[2] if px + py < 0 else LACE[1]
    for k in range(6):
        a = math.radians(30 + 60 * k)
        cx, cy = 4.6 * math.cos(a), 4.6 * math.sin(a)
        d = math.hypot(px - cx, py - cy)
        if d <= 2.1:
            if d < 0.8:
                return None  # the petal's picot hole
            return LACE[2] if (px - cx) + (py - cy) < -0.4 else LACE[0] if d > 1.6 and (px - cx) + (py - cy) > 0.9 else LACE[1]
        # A bar from the centre out to each petal.
        along = px * math.cos(a) + py * math.sin(a)
        across = -px * math.sin(a) + py * math.cos(a)
        if 1.9 <= along <= 2.7 and abs(across) <= 0.5:
            return LACE[1]
    if r >= 7.0 and ((x + y) % 4 == 0 or (x - y) % 4 == 0):
        return LACE[1]
    return None


def tatting(x, y):
    # Ring centres every eight pixels (3.5 + 8i): the pattern repeats twice across the block.
    rx, ry = (x % 8) + 0.5 - 4.0, (y % 8) + 0.5 - 4.0
    d = math.hypot(rx, ry)
    if 2.4 <= d <= 3.5:
        return LACE[2] if rx + ry < -0.5 else LACE[0] if rx + ry > 2.5 else LACE[1]
    # The bars linking each ring to the next, across and down.
    if (x % 8 in (0, 7) and y % 8 in (3, 4)) or (y % 8 in (0, 7) and x % 8 in (3, 4)):
        return LACE[1]
    return None


# ---------------------------------------------------------------- the spools

def spool_wood():
    """The spools' turned wood: pale, its grain running round in long streaks."""
    return bs.img(bs.streaks(WOOD, seed=401, vertical=False, across=1.3, along=8.0, spread=0.8))


def thread(colour):
    """Thread wound on a spool: a row of thread every pixel, lit and shaded in turn, a darker line every fourth row where
    the layers lie, and a glint running round the winding."""
    p = THREADS[colour]
    img = new()
    for y in range(16):
        for x in range(16):
            k = 0 if y % 4 == 3 else 2 if y % 2 == 0 else 1
            if (x + 3 * y) % 16 == 0 and y % 4 != 3:
                k = 3
            img.putpixel((x, y), p[k])
    return img


# ---------------------------------------------------------------- the pincushion

def velvet():
    """The pincushion's red velvet, soft like wool."""
    return bs.img(bs.cloth(VELVET, seed=411))


def seam():
    """A seam between two of the pincushion's segments: the velvet a tone darker, a groove down the middle and stitches
    crossing it every fourth row."""
    img = bs.img(bs.cloth(VELVET[:3], seed=412))
    for y in range(16):
        img.putpixel((7, y), VELVET[0])
        img.putpixel((8, y), VELVET[0])
        if y % 4 == 1:
            img.putpixel((6, y), STITCH)
            img.putpixel((9, y), STITCH)
    return img


def felt():
    """The green felt leaf on the pincushion's top."""
    return bs.img(bs.cloth(FELT, seed=413))


# ---------------------------------------------------------------- steel, the tape, brass and glass

def needle_steel():
    """Polished needle steel: cool blue-grey, brushed in long streaks down its length."""
    return bs.img(bs.streaks(STEEL[1:], seed=421, vertical=True, across=1.2, along=10.0, spread=0.7))


def pin_shaft():
    """A pin's shaft: the model shows four pixels of it (x 6 to 9), lit on the left."""
    img = new(STEEL[1])
    for y in range(16):
        for x, k in ((6, 4), (7, 3), (8, 2), (9, 1)):
            img.putpixel((x, y), STEEL[k])
    return img


def tape(mark):
    """The tailor's tape, running down the texture: 0 its west edge and 2 its east, ticked every second pixel (longer
    every fourth, longest every eighth); 1 its plain middle, and 3 the middle with a red diamond, every fourth block."""
    img = new(TAPE[1])
    for y in range(16):
        for x in range(16):
            if (mark == 0 and x == 0) or (mark == 2 and x == 15):
                img.putpixel((x, y), TAPE[0])
            elif y % 8 == 7:
                img.putpixel((x, y), TAPE[2])  # a faint fold between the units
    if mark in (0, 2):
        for y in range(0, 16, 2):
            length = 6 if y % 8 == 0 else 4 if y % 4 == 0 else 2
            for i in range(length):
                img.putpixel((1 + i if mark == 0 else 14 - i, y), INK)
    if mark == 3:
        for y in range(16):
            for x in range(16):
                d = abs(x + 0.5 - 8) + abs(y + 0.5 - 8)
                if d <= 4.0:
                    img.putpixel((x, y), RED[0] if d > 3.0 else RED[2] if x + y < 15 else RED[1])
    return img


def tape_back():
    """The tape's back: plain, a tone darker."""
    img = new(TAPE[1])
    for y in range(16):
        img.putpixel((0, y), TAPE[0])
        img.putpixel((15, y), TAPE[0])
    return img


def thimble():
    """The thimble's brass, dimpled on a staggered lattice: each dimple's upper-left inside in shade, its lower-right
    catching the light; the brass lit along its top and left."""
    img = bs.img(bs.metal(BRASS, seed=431, panels=False))
    for row, y in enumerate(range(1, 16, 4)):
        for x in range(1 + 2 * (row % 2), 16, 4):
            img.putpixel((x, y), BRASS[1])
            img.putpixel(((x + 1) % 16, y), BRASS[2])
            img.putpixel((x, y + 1), BRASS[2])
            img.putpixel(((x + 1) % 16, y + 1), BRASS[4])
    return img


def skylight():
    """Grimy skylight glass, see-through: a pale glaze lit at the top left, grime streaking down and gathered along
    its bottom."""
    img = new()
    for y in range(16):
        for x in range(16):
            k = 2
            if y == 0 or x == 0:
                k = 3
            elif y >= 13 or (x % 5 == 2 and y >= 6 + (x * 3) % 5):
                k = 1
            if y == 15:
                k = 0
            img.putpixel((x, y), GLASS[k])
    return img


def spindle_loft_textures():
    out = {("block", f"doily_lace_{p}"): lace(p) for p in range(LACE_PATTERNS)}
    out[("block", "spool_wood")] = spool_wood()
    for colour in THREAD_COLOURS:
        out[("block", f"spool_thread_{colour}")] = thread(colour)
    out[("block", "pincushion")] = velvet()
    out[("block", "pincushion_seam")] = seam()
    out[("block", "pincushion_leaf")] = felt()
    out[("block", "needle_steel")] = needle_steel()
    out[("block", "pin_shaft")] = pin_shaft()
    for mark in range(4):
        out[("block", f"measuring_tape_{mark}")] = tape(mark)
    out[("block", "measuring_tape_back")] = tape_back()
    out[("block", "thimble_metal")] = thimble()
    out[("block", "grimy_skylight")] = skylight()
    out[("item", "cursed_spindle")] = item_icons.draw("cursed_spindle")
    return out


if __name__ == "__main__":
    import sys
    sheet = spindle_loft_textures()
    keys = sorted(sheet)
    scale, cols = 6, 6
    rows = (len(keys) + cols - 1) // cols
    image = Image.new("RGBA", (cols * (16 * scale + 8), rows * (16 * scale + 8)), (40, 34, 30, 255))
    for i, key in enumerate(keys):
        tile = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        image.paste(tile, ((i % cols) * (16 * scale + 8), (i // cols) * (16 * scale + 8)), tile)
    image.save(sys.argv[1] if len(sys.argv) > 1 else "spindle_loft_textures.png")
    print(len(keys), "textures")
