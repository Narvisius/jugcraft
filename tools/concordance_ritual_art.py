"""Textures for roadmap step 12, rituals (tools/concordance_rituals.py): the Adept's Wand (a 16x16 map in
tools/item_icons/, docs/ITEM_ICONS.md), the Circle Anchor (its GeckoLib sheet and its item icon), the Ley Pylon and the
Warding Stone (plain, and the 4x4 connected sheet the Fusion built-in pack uses). Original pixel art drawn from code;
tools/concordance_art.py includes these in its textures().
"""

import random
import zlib

from PIL import Image

import clean_metal as cm
import hd_art as hd
import item_icons
from construction_art import Tool

GOLD = hd.Material([(86, 56, 10), (140, 98, 22), (196, 148, 40), (232, 192, 74), (250, 226, 132), (255, 246, 206)],
                   0.7, 24)
DARK_WOOD = hd.Material([(30, 18, 12), (48, 30, 19), (68, 43, 27), (90, 59, 37), (114, 78, 50), (140, 100, 66)], 0.25, 10)
LAPIS = hd.Material([(18, 30, 82), (30, 52, 128), (46, 80, 172), (78, 120, 212), (136, 172, 240), (214, 230, 255)],
                    0.75, 30, 0.35)
GLINT = [(214, 164, 70), (246, 210, 120), (255, 240, 190)]
WHITE = (255, 255, 250)

# 16x16 palettes, darkest first.
STONE = [(52, 50, 58), (72, 70, 80), (94, 92, 102), (116, 114, 124), (140, 138, 148), (166, 164, 172)]
PALE = [(118, 112, 106), (146, 140, 132), (172, 166, 158), (196, 190, 182), (216, 212, 204), (236, 232, 226)]
COPPER = [(74, 32, 18), (116, 54, 30), (160, 84, 48), (200, 118, 72), (230, 158, 110), (250, 206, 168)]
VERDIGRIS = [(44, 96, 84), (66, 132, 114), (98, 168, 146)]
BLUE = [(14, 22, 64), (24, 42, 110), (40, 72, 160), (70, 112, 204), (128, 166, 236), (206, 224, 255)]
VIOLET = [(40, 22, 64), (66, 40, 104), (98, 68, 150), (140, 112, 196), (188, 166, 232)]


# ------------------------------------------------------------------------------------------------- the wand (64)

# ------------------------------------------------------------------------------------------------- blocks (16)

def _noise_fill(img, palette, levels, seed, x0=0, y0=0, x1=15, y1=15):
    rng = random.Random(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cm.put(img, x, y, palette[levels[rng.randrange(len(levels))]])


def pylon_stone():
    """The pylon's plinth: dark dressed stone in two courses, with a carved channel along the middle."""
    img = cm.canvas(STONE[2])
    _noise_fill(img, STONE, [2, 2, 2, 3, 1], 1201)
    for x in range(16):
        cm.put(img, x, 7, STONE[0])
        cm.put(img, x, 8, STONE[4] if x % 5 else STONE[3])
        cm.put(img, x, 15, STONE[0])
    for y in (0, 1, 2, 3, 4, 5, 6):
        cm.put(img, 7 if y < 7 else 3, y, STONE[0])
    for y in range(9, 15):
        cm.put(img, 3, y, STONE[0])
        cm.put(img, 11, y, STONE[0])
    cm.bevel(img, 0, 0, 15, 15, STONE[4], STONE[0])
    return img


def pylon_copper():
    """Worked copper for the pylon's frame: polished bands with a little verdigris in the joints."""
    img = cm.canvas(COPPER[3])
    bands = [4, 4, 3, 3, 4, 3, 3, 2, 3, 3, 2, 2, 3, 2, 2, 1]
    for y in range(16):
        for x in range(16):
            cm.put(img, x, y, COPPER[bands[y]])
    cm.bevel(img, 0, 0, 15, 15, COPPER[5], COPPER[0])
    for x, y in ((3, 14), (4, 14), (11, 13), (12, 14), (7, 1)):
        cm.put(img, x, y, VERDIGRIS[1])
    cm.put(img, 4, 13, VERDIGRIS[2])
    return img


def pylon_crystal():
    """The pylon's crystal: deep lapis facets lit from the top left, with a pale vein through them."""
    img = cm.canvas(BLUE[2])
    for y in range(16):
        for x in range(16):
            k = (x + y) / 2 + (3 if (x // 4 + y // 4) % 2 else 0)
            colour = BLUE[4] if k < 5 else BLUE[3] if k < 9 else BLUE[2] if k < 13 else BLUE[1]
            cm.put(img, x, y, colour)
    for i in range(16):
        cm.put(img, i, 15 - i if i % 3 else 14 - i if i < 14 else 0, BLUE[5])
    cm.bevel(img, 0, 0, 15, 15, BLUE[5], BLUE[0])
    return img


def _warding_tile(top, right, bottom, left, seed=1301):
    """One Warding Stone face: pale dressed stone, a carved border on every side not joined to another stone, and a
    violet rune inlaid where a border runs. Joined sides run straight on into the neighbour."""
    img = Image.new("RGBA", (16, 16), PALE[3] + (255,))
    _noise_fill(img, PALE, [3, 3, 3, 2, 4], seed)
    if not top:
        for x in range(16):
            cm.put(img, x, 0, PALE[5])
            cm.put(img, x, 1, PALE[1])
    if not bottom:
        for x in range(16):
            cm.put(img, x, 15, PALE[0])
            cm.put(img, x, 14, PALE[2])
    if not left:
        for y in range(16):
            cm.put(img, 0, y, PALE[5])
            cm.put(img, 1, y, PALE[1])
    if not right:
        for y in range(16):
            cm.put(img, 15, y, PALE[0])
            cm.put(img, 14, y, PALE[2])
    # The carved band across the middle: always there, so joined stones read as one long band.
    for x in range(16):
        cm.put(img, x, 7, PALE[1])
        cm.put(img, x, 8, VIOLET[2] if x % 4 in (1, 2) else PALE[1])
        cm.put(img, x, 9, PALE[4])
    # A rune at the centre of a stone that stands alone left and right.
    if not left and not right:
        for x, y, colour in ((7, 4, VIOLET[3]), (8, 4, VIOLET[3]), (7, 5, VIOLET[2]), (8, 6, VIOLET[2]),
                             (6, 11, VIOLET[2]), (9, 11, VIOLET[2]), (7, 12, VIOLET[3]), (8, 12, VIOLET[3])):
            cm.put(img, x, y, colour)
    return img


def warding_stone():
    return _warding_tile(False, False, False, False)


def _simple_tile(top, right, bottom, left):
    """Where Fusion's "simple" layout keeps the tile for these connections (Fusion SimpleLayoutHandler)."""
    if not (left or top or right or bottom):
        return 0, 0
    one = {(True, False, False, False): (3, 0), (False, True, False, False): (3, 1),
           (False, False, True, False): (2, 1), (False, False, False, True): (2, 0)}
    key = (left, top, right, bottom)
    if key in one:
        return one[key]
    two = {(True, False, True, False): (0, 1), (False, True, False, True): (1, 1), (True, True, False, False): (3, 3),
           (False, True, True, False): (2, 3), (False, False, True, True): (2, 2), (True, False, False, True): (3, 2)}
    if key in two:
        return two[key]
    if not left:
        return 0, 2
    if not top:
        return 1, 2
    if not right:
        return 1, 3
    if not bottom:
        return 0, 3
    return 1, 0


def warding_stone_connected():
    """The 4x4 sheet Fusion's connecting texture reads, every tile drawn for the connections it stands for."""
    sheet = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for bits in range(16):
        top, right, bottom, left = bool(bits & 1), bool(bits & 2), bool(bits & 4), bool(bits & 8)
        col, row = _simple_tile(top, right, bottom, left)
        sheet.paste(_warding_tile(top, right, bottom, left), (col * 16, row * 16))
    return sheet


# ------------------------------------------------------------------------------------------------- the anchor

# The anchor's box-UV regions in its 64x64 sheet (tools/concordance_rituals.py anchor_geo): (u, v, width, height).
ANCHOR_REGIONS = {
    "base": (0, 0, 56, 18), "collar": (0, 18, 40, 12), "ring_post_x": (40, 18, 6, 4), "ring_post_z": (40, 23, 6, 3),
    "post": (48, 18, 4, 4), "bar_x": (0, 30, 22, 2), "bar_z": (0, 32, 18, 9), "crystal": (24, 32, 16, 10),
    "tip": (40, 32, 8, 4),
}


def circle_anchor_sheet():
    """The anchor's GeckoLib sheet: dressed stone for the plinth, copper for the collar, brass for the ring and its
    posts, and lapis for the floating crystal, each region shaded so its top is lit and its sides are darker."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    materials = {"base": STONE, "collar": COPPER, "ring_post_x": hd.BRASS.ramp, "ring_post_z": hd.BRASS.ramp,
                 "post": hd.BRASS.ramp, "bar_x": hd.BRASS.ramp, "bar_z": hd.BRASS.ramp, "crystal": BLUE, "tip": BLUE}
    for name, (u, v, w, h) in ANCHOR_REGIONS.items():
        ramp = materials[name]
        rng = random.Random(zlib.crc32(name.encode()))
        for y in range(v, v + h):
            for x in range(u, u + w):
                level = 3 if (y - v) < max(1, h // 3) else 2
                if rng.random() < 0.18:
                    level = max(1, level - 1)
                if name in ("crystal", "tip") and (x + y) % 5 == 0:
                    level = 4
                cm.put(img, x, y, ramp[min(level, len(ramp) - 1)])
        # A carved line round the plinth's sides.
        if name == "base":
            for x in range(u, u + w):
                cm.put(img, x, v + 14 + 2, STONE[0])
    return img


def circle_anchor_item():
    """The anchor as an item: the stone plinth, the brass ring and the lapis crystal floating above it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(11, 16):
        for x in range(2, 14):
            cm.put(img, x, y, STONE[3] if y == 11 else STONE[2] if y < 14 else STONE[1])
    for x in range(4, 12):
        cm.put(img, x, 10, COPPER[3])
    for x in range(3, 13):
        cm.put(img, x, 8, hd.BRASS.ramp[4] if x < 8 else hd.BRASS.ramp[3])
    for y, (a, b) in enumerate(((7, 8), (6, 9), (6, 9), (6, 9), (7, 8)), start=1):
        for x in range(a, b + 1):
            cm.put(img, x, y, BLUE[4] if x < 8 else BLUE[2])
    cm.put(img, 7, 2, BLUE[5])
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3] == 255:
                        out.putpixel((x, y), (14, 10, 22, 255))
                        break
    return out


def textures():
    return {("item", "adept_wand"): item_icons.draw("adept_wand"), ("item", "circle_anchor"): circle_anchor_item(),
            ("block", "circle_anchor"): circle_anchor_sheet(), ("block", "ley_pylon_stone"): pylon_stone(),
            ("block", "ley_pylon_copper"): pylon_copper(), ("block", "ley_pylon_crystal"): pylon_crystal(),
            ("block", "warding_stone"): warding_stone(), ("block", "warding_stone_connected"): warding_stone_connected()}


# Fusion reads this section of the connected sheet's .png.mcmeta (vanilla ignores it).
FUSION_METADATA = {"fusion": {"type": "connecting", "layout": "simple"}}
