"""Original textures for face paint (fall additions 10) (requires Pillow): the Face Paint Kit (a tin palette of four
greasepaints and a brush) and each design, as the client's FacePaintLayer lays it over a player's face: 16x16 over the
face's 8x8, see-through wherever the skin shows. Rows 8 and 9 are where a skin's eyes are; designs that leave the eyes
showing keep those windows clear.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang texture is read, traced or
recoloured.
"""
import math

from PIL import Image

from agriculture import FACE_PAINT
from crop_textures import rgb
from decor9_textures import put
from decor13_textures import icon

KIT = [
    "................",
    "............hh..",
    "...........hh...",
    "..........hh....",
    ".........ff.....",
    "........kk......",
    "..rrrrrrkrrr....",
    ".rsssssssssr....",
    "rsWWssKKssssr...",
    "rsWWssKKssOOr...",
    "rssssssssOOsr...",
    "rsGGsssssssr....",
    "rsGGssssssr.....",
    ".rssssssrr......",
    "..rrrrrr........",
    "................",
]
# Where a skin's eyes are, in the design's 16x16 (left and right as seen from the front).
EYES = [(x, y) for y in (8, 9) for x in list(range(2, 6)) + list(range(10, 14))]


def kit():
    return icon(KIT, {"h": rgb("8a5a2a"), "f": rgb("c0c4cc"), "k": rgb("1a1a1a"), "r": rgb("7a7e86"), "s": rgb("b8bcc4"),
                      "W": rgb("f4f4f0"), "K": rgb("1a1a1e"), "O": rgb("f08a1c"), "G": rgb("4ab040")})


def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def fill(img, colour, keep_eyes=False):
    """The whole face in one paint, a little uneven (and, if asked, the eyes left clear)."""
    r, g, b = colour
    for y in range(16):
        for x in range(16):
            if keep_eyes and (x, y) in EYES:
                continue
            shade = ((x * 7 + y * 13) % 5 - 2) * 3
            put(img, x, y, (max(0, min(255, r + shade)), max(0, min(255, g + shade)), max(0, min(255, b + shade))))


def oval(img, cx, cy, rx, ry, colour):
    for y in range(16):
        for x in range(16):
            if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1.0:
                put(img, x, y, colour)


def points(img, coords, colour):
    for x, y in coords:
        put(img, x, y, colour)


def skull():
    img = blank()
    fill(img, rgb("e8e2d4"))
    points(img, [(x, 11) for x in (2, 3, 4, 5, 10, 11, 12, 13)], rgb("c8c0b0"))
    dark = rgb("1a1418")
    oval(img, 4.5, 8.5, 2.7, 2.3, dark)
    oval(img, 11.5, 8.5, 2.7, 2.3, dark)
    points(img, [(6, 10), (7, 10), (8, 10), (9, 10), (7, 11), (8, 11)], dark)
    points(img, [(x, 13) for x in range(3, 13)], dark)
    points(img, [(x, y) for x in (4, 6, 8, 10) for y in (12, 14)] + [(12, 12), (12, 14)], dark)
    return img


def pumpkin():
    img = blank()
    fill(img, rgb("f08a1c"))
    points(img, [(x, y) for x in (3, 12) for y in range(16)], rgb("d8701a"))
    points(img, [(7, 0), (8, 0), (7, 1)], rgb("4a8a2a"))
    dark = rgb("2a1608")
    eyes = [(4, 6), (3, 7), (4, 7), (5, 7), (2, 8), (3, 8), (4, 8), (5, 8), (6, 8)]
    points(img, eyes + [(x + 7, y) for x, y in eyes], dark)
    points(img, [(7, 10), (8, 10), (6, 11), (7, 11), (8, 11), (9, 11)], dark)
    points(img, [(x, 12) for x in range(3, 13) if x not in (5, 10)], dark)
    points(img, [(x, 13) for x in range(4, 12) if x != 8], dark)
    points(img, [(x, 14) for x in range(6, 10)], dark)
    return img


def black_cat():
    img = blank()
    dark = rgb("1a1a1c")
    points(img, [(x, 7) for x in list(range(1, 6)) + list(range(10, 15))] + [(0, 6), (15, 6)], dark)
    points(img, [(6, 10), (7, 10), (8, 10), (9, 10), (7, 11), (8, 11)], dark)
    points(img, [(6, 12), (9, 12), (7, 13), (8, 13)], dark)
    whiskers = [(0, 10), (1, 10), (2, 10), (3, 10), (0, 12), (1, 12), (2, 12), (3, 12), (1, 11), (2, 11), (3, 11), (4, 11)]
    points(img, whiskers + [(15 - x, y) for x, y in whiskers], dark)
    return img


def vampire():
    img = blank()
    fill(img, rgb("e6e2ee"), keep_eyes=True)
    hair = rgb("201820")
    for x in range(16):
        line = 3.0 - abs(x - 7.5) * 0.35
        for y in range(16):
            if y < line:
                put(img, x, y, hair)
    points(img, [(x, 10) for x in (2, 3, 4, 5, 10, 11, 12, 13)], rgb("b8a8c8"))
    red = rgb("8a1020")
    points(img, [(x, 12) for x in range(5, 11)] + [(7, 13), (8, 13), (9, 14)], red)
    points(img, [(6, 13), (9, 13)], rgb("f8f8f8"))
    return img


def witch():
    img = blank()
    fill(img, rgb("6ab04a"), keep_eyes=True)
    points(img, [(7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (8, 11)], rgb("5a9a3e"))
    brow = rgb("2a4a1a")
    points(img, [(x, 6) for x in (2, 5, 10, 13)] + [(x, 5) for x in (3, 4, 11, 12)], brow)
    points(img, [(12, 11)], rgb("4a5a20"))
    points(img, [(12, 10)], rgb("7aa040"))
    lips = rgb("4a1a5a")
    points(img, [(x, 12) for x in range(5, 11)] + [(x, 13) for x in range(6, 10)], lips)
    return img


def scarecrow():
    img = blank()
    cheeks = rgb("e87070")
    oval(img, 3.0, 11.5, 1.7, 1.4, cheeks)
    oval(img, 13.0, 11.5, 1.7, 1.4, cheeks)
    points(img, [(7, 10), (8, 10), (7, 11)], rgb("e8802a"))
    stitch = rgb("2a1a10")
    points(img, [(4, 12), (11, 12)] + [(x, 13) for x in range(5, 11)], stitch)
    points(img, [(x, y) for x in (5, 7, 9) for y in (12, 14)] + [(10, 12), (10, 14)], stitch)
    points(img, [(2, 9), (13, 9), (5, 10), (10, 11)], rgb("9a6a3a"))
    return img


DRAWERS = {"skull": skull, "pumpkin": pumpkin, "black_cat": black_cat, "vampire": vampire, "witch": witch, "scarecrow": scarecrow}


def face_paint_textures():
    out = {("item", FACE_PAINT["kit"]): kit()}
    for design in FACE_PAINT["designs"]:
        out[("entity", f"face_paint/{design}")] = DRAWERS[design]()
    return out
