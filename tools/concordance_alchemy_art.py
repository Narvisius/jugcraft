"""Textures for roadmap step 13, alchemy (tools/concordance_alchemy.py): the Alembic Crucible's GeckoLib sheet (an
animated strip whose liquid ripples), its item icon, and the alchemy items. Original pixel art drawn from code;
tools/concordance_art.py includes these in its textures(). The crucible's .png.mcmeta is written by the data generator.
"""

import math
import random

from PIL import Image

import clean_metal as cm
import concordance_alchemy as alchemy

OUTLINE = (20, 14, 24)
IRON = [(38, 38, 44), (58, 58, 66), (80, 80, 90), (104, 104, 116), (132, 132, 144), (166, 166, 178)]
COPPER = [(74, 32, 18), (116, 54, 30), (160, 84, 48), (200, 118, 72), (230, 158, 110), (250, 206, 168)]
BRASS = [(76, 50, 18), (120, 84, 32), (166, 124, 50), (206, 166, 76), (236, 206, 122), (250, 236, 180)]
STONE = [(70, 70, 74), (96, 96, 100), (122, 122, 126), (150, 150, 154), (178, 178, 182)]
WOOD = [(60, 38, 20), (86, 56, 30), (112, 76, 42), (140, 100, 58), (166, 124, 76)]
LIQUID = [(20, 70, 76), (28, 98, 104), (40, 128, 132), (70, 160, 160), (130, 206, 200), (200, 240, 230)]
# The crucible's liquid is drawn pale, without hue: the client tints it with what a spoon would find (roadmap step 27,
# client/CrucibleRenderer: plain water is the teal above; each property its own colour), so the tint shows true.
LIQUID_PALE = [(96, 96, 96), (136, 136, 136), (176, 176, 176), (212, 212, 212), (240, 240, 240), (255, 255, 255)]
GREEN = [(40, 76, 30), (60, 104, 42), (84, 134, 58), (116, 166, 80), (160, 200, 112)]
PAPER = [(150, 130, 96), (206, 190, 150), (232, 220, 186), (246, 238, 214)]
INK = [(60, 40, 104), (96, 70, 150)]
GLASS = [(150, 190, 210), (196, 226, 240), (236, 248, 255)]
AMETHYST = [(84, 58, 128), (154, 126, 204), (218, 202, 248)]


def _new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def _line(img, a, b, colour):
    steps = max(abs(b[0] - a[0]), abs(b[1] - a[1])) * 2 + 1
    for i in range(steps + 1):
        t = i / steps
        cm.put(img, round(a[0] + (b[0] - a[0]) * t), round(a[1] + (b[1] - a[1]) * t), colour)


def _outline(img):
    src = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < img.width and 0 <= ny < img.height and src.getpixel((nx, ny))[3] == 255:
                    img.putpixel((x, y), OUTLINE + (255,))
                    break
    return img


def mortar():
    """A grey stone bowl, lit on its rim, with a wooden pestle leaning out of it."""
    img = _new()
    _line(img, (11, 2), (8, 8), WOOD[3])
    _line(img, (12, 2), (9, 8), WOOD[2])
    cm.put(img, 11, 1, WOOD[4])
    for y in range(8, 14):
        half = 6 - max(0, y - 10)
        for x in range(8 - half, 8 + half):
            cm.put(img, x, y, STONE[3] if y == 8 else STONE[2] if x < 8 else STONE[1])
    for x in range(3, 13):
        cm.put(img, x, 9, STONE[0] if 4 < x < 11 else STONE[2])
    for x in range(5, 11):
        cm.put(img, x, 14, STONE[1])
    return _outline(img)


def _rod(img, colours, tip, tip_colours):
    for i, (x, y) in enumerate(zip(range(2, 13), range(13, 2, -1))):
        cm.put(img, x, y, colours[1] if i % 3 else colours[2])
        cm.put(img, x + 1, y, colours[0])
    tip(img, tip_colours)


def stirring_rod():
    """A long rod of dark wood with a copper ferrule and a paddle end."""
    img = _new()
    _rod(img, WOOD, lambda im, c: [cm.put(im, x, y, c[3]) for x, y in ((12, 2), (13, 2), (13, 3), (12, 3), (14, 1))], COPPER)
    for x, y in ((2, 13), (3, 13), (2, 12)):
        cm.put(img, x, y, COPPER[3])
    return _outline(img)


def sampling_spoon():
    """A small copper spoon: a long handle and a round bowl."""
    img = _new()
    _rod(img, COPPER, lambda im, c: None, COPPER)
    for y in range(1, 6):
        for x in range(10, 15):
            if math.hypot(x - 12, y - 3) <= 2.3:
                cm.put(img, x, y, COPPER[4] if x + y < 15 else COPPER[2])
    cm.put(img, 11, 2, COPPER[5])
    return _outline(img)


def assay_glass():
    """A brass-rimmed lens on a short handle, an amethyst set where the rim meets it."""
    img = _new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 9.5, y + 0.5 - 6.5)
            if d <= 5.2:
                cm.put(img, x, y, BRASS[3] if (x + y) < 16 else BRASS[2])
            if d <= 3.9:
                cm.put(img, x, y, GLASS[2] if (x - y) > 2 else GLASS[1] if x + y < 16 else GLASS[0])
    for i in range(5):
        cm.put(img, 5 - i, 10 + i, WOOD[2])
        cm.put(img, 6 - i, 10 + i, WOOD[1])
    cm.put(img, 6, 10, AMETHYST[2])
    cm.put(img, 7, 10, AMETHYST[1])
    return _outline(img)


def formula(written):
    """A sheet of paper with a folded corner; written, ruled in violet with a small flask drawn at its head."""
    img = _new()
    for y in range(1, 15):
        for x in range(3, 13):
            cm.put(img, x, y, PAPER[2] if x < 12 else PAPER[1])
    img.putpixel((12, 1), (0, 0, 0, 0))
    cm.put(img, 11, 1, PAPER[1])
    cm.put(img, 12, 2, PAPER[0])
    for y in (5, 7, 9, 11, 13):
        for x in range(4, 12):
            cm.put(img, x, y, INK[1] if written and (x + y) % 5 else PAPER[1])
    if written:
        for x, y in ((6, 2), (6, 3), (5, 4), (7, 4), (8, 2), (8, 3), (9, 4)):
            cm.put(img, x, y, INK[0])
    return _outline(img)


def reagent():
    """A twist of paper holding a heap of ground powder."""
    img = _new()
    for y in range(6, 14):
        for x in range(8 - (y - 5), 8 + (y - 5)):
            if 2 <= x <= 13:
                cm.put(img, x, y, PAPER[2] if x < 8 else PAPER[1])
    for y in range(4, 9):
        for x in range(5, 11):
            if math.hypot(x - 7.5, (y - 8) * 1.4) <= 3.2:
                cm.put(img, x, y, (226, 214, 176) if x < 8 else (196, 182, 142))
    cm.put(img, 7, 5, (246, 238, 214))
    return _outline(img)


def draught():
    """A round glass flask of teal liquid with a cork."""
    img = _new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 10)
            if d <= 4.8:
                cm.put(img, x, y, GLASS[0])
            if d <= 4.0 and y >= 8:
                cm.put(img, x, y, LIQUID[4] if x < 7 and y < 10 else LIQUID[3] if x < 9 else LIQUID[2])
    for y in range(3, 6):
        cm.put(img, 7, y, GLASS[1])
        cm.put(img, 8, y, GLASS[0])
    for x, y in ((7, 2), (8, 2), (7, 1), (8, 1)):
        cm.put(img, x, y, WOOD[3])
    cm.put(img, 6, 8, GLASS[2])
    return _outline(img)


def salve():
    """A wooden bowl heaped with green salve."""
    img = _new()
    for y in range(9, 14):
        half = 7 - max(0, y - 11)
        for x in range(8 - half, 8 + half):
            cm.put(img, x, y, WOOD[3] if y == 9 else WOOD[2] if x < 8 else WOOD[1])
    for y in range(6, 10):
        for x in range(3, 13):
            if math.hypot(x - 7.5, (y - 9.5) * 1.6) <= 4.8:
                cm.put(img, x, y, GREEN[3] if x < 8 and y < 8 else GREEN[2])
    cm.put(img, 6, 7, GREEN[4])
    return _outline(img)


def crucible_item():
    """The crucible as an item: the iron pot with its copper band, teal liquid at the brim and the ladle's handle."""
    img = _new()
    for y in range(5, 15):
        for x in range(2, 14):
            cm.put(img, x, y, IRON[3] if x < 6 else IRON[2] if x < 11 else IRON[1])
    for x in range(1, 15):
        cm.put(img, x, 9, COPPER[3])
        cm.put(img, x, 10, COPPER[2])
    for x in range(3, 13):
        cm.put(img, x, 5, LIQUID[3])
    for y in range(1, 6):
        cm.put(img, 11, y, BRASS[3])
    for x in (3, 12):
        cm.put(img, x, 15, IRON[1])
    return _outline(img)


def crucible_sheet():
    """The GeckoLib sheet: CRUCIBLE_FRAMES frames of 64x64 stacked downwards. Iron for the pot, copper for its band,
    brass for the ladle, and pale liquid whose highlights drift a little each frame (the surface animation); the
    renderer colours the liquid by its contents."""
    frames = alchemy.CRUCIBLE_FRAMES
    sheet = Image.new("RGBA", (64, 64 * frames), (0, 0, 0, 0))
    materials = {"floor": IRON, "wall_ns": IRON, "wall_ew": IRON, "band": COPPER, "foot": IRON, "shaft": BRASS,
                 "scoop": BRASS, "liquid": LIQUID_PALE}
    for frame in range(frames):
        oy = frame * 64
        for name, (u, v) in alchemy.CRUCIBLE_UV.items():
            w, h, d = alchemy.CRUCIBLE_SIZES[name]
            ramp = materials[name]
            rng = random.Random(1300 + len(name))
            for y in range(v, v + d + h):
                for x in range(u, u + 2 * (w + d)):
                    top = y < v + d
                    level = 3 if top else 2
                    if name == "liquid":
                        # Ripples: soft bright bands that move with the frame.
                        wave = math.sin((x * 0.7 + y * 0.45) + frame * math.pi / 2)
                        level = 4 if wave > 0.75 else 3 if wave > -0.2 else 2
                    elif rng.random() < 0.15:
                        level -= 1
                    if name in ("wall_ns", "wall_ew") and y == v + d + 1:
                        level = 4  # the lit rim
                    cm.put(sheet, x, y + oy, ramp[min(level, len(ramp) - 1)])
    return sheet


def textures():
    return {("block", "crucible"): crucible_sheet(), ("item", "crucible"): crucible_item(), ("item", "mortar"): mortar(),
            ("item", "stirring_rod"): stirring_rod(), ("item", "sampling_spoon"): sampling_spoon(),
            ("item", "assay_glass"): assay_glass(), ("item", "formula"): formula(False),
            ("item", "formula_written"): formula(True), ("item", "reagent"): reagent(), ("item", "draught"): draught(),
            ("item", "salve"): salve()}
