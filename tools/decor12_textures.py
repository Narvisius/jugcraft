"""Original textures for the twelfth batch of Halloween decorations, night events (requires Pillow): toilet paper (the
roll, the hanging and draped streamers), the Halloween Bonfire's stones, charred logs, embers, skewers and flames,
the Haunted Hayride's planks, hay, wheels, iron and jack o'lantern and its item, and marshmallows (raw, on a stick,
toasted and burnt).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code or from the small pixel-art grids below,
from fixed seeds; no Mojang texture is read, traced or recoloured. Block textures are 16x16; the streamers and the
hayride's wheels are see-through between their strips and spokes; the flame (an entity texture) and the items are
see-through round their shapes.

Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a short palette in small clumps,
never a random colour at every pixel; wood as planks, and straw, bark and hair as streaks.
"""
import math
import random

from PIL import Image

from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import IRON, put

PAPER = [rgb("d8d8d0"), rgb("e8e8e2"), rgb("f6f6f2"), rgb("ffffff")]
STONE = [rgb("4a4a4a"), rgb("5a5a58"), rgb("6c6c68"), rgb("7e7e78")]
CHAR = [rgb("1a1410"), rgb("2a2018"), rgb("3a2c20"), rgb("4a3828")]
PLANK = [rgb("7a5a34"), rgb("8a6a3e"), rgb("9a7848"), rgb("6a4c2a")]
HAY = [rgb("b8962a"), rgb("ccaa34"), rgb("dcbe48"), rgb("9a7a22")]
ORANGE = [rgb("a84a0c"), rgb("c85e16"), rgb("e07422"), rgb("f49038")]
MALLOW = rgb("f4f0e8")


def streamer():
    """Strips of paper hanging down, each with a perforation every few rows, wavering and torn at the ends."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(22101)
    for x0 in (1, 6, 11):
        start = rng.randrange(0, 3)
        end = 16 - rng.randrange(0, 4)
        for y in range(start, end):
            wave = int(round(math.sin(y * 0.7 + x0) * 0.8))
            for dx in range(3):
                x = x0 + dx + wave
                tone = 1 if dx == 0 else 2 if dx == 1 else 3
                put(img, x, y, PAPER[0] if y % 5 == 4 else PAPER[tone])
    return img


def drape():
    """Paper strips lying across, perforated."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y0 in (2, 7, 12):
        for y in range(y0, y0 + 3):
            for x in range(16):
                put(img, x, y, PAPER[0] if x % 5 == 4 else PAPER[1 + (y - y0)])
    return img


def roll():
    """A toilet paper roll seen from the side and end: a white cylinder with a cardboard core."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 13):
        for x in range(2, 11):
            put(img, x, y, PAPER[1 if x in (2, 10) else 2 if y % 3 else 1])
    for y in range(3, 13):
        for x in range(10, 14):
            d = ((x - 10.5) / 3.0) ** 2 + ((y - 7.5) / 5.0) ** 2
            if d < 1:
                put(img, x, y, rgb("9a7a4e") if d < 0.25 else PAPER[3])
    for y in range(12, 15):
        put(img, 4 + y - 12, y, PAPER[3])
    return img


# ---------------------------------------------------------------- the bonfire

def stone():
    """The fire ring's stones, as vanilla cobblestone."""
    c = Canvas()
    bs.cobble(STONE, 22121)(c)
    return c.img


def log():
    """Bark charred black in places, glowing in its cracks."""
    c = Canvas()
    bs.streaks(CHAR[1:], 22122, along=6.0)(c)
    for x in range(0, 16, 4):
        for y in range(16):
            # The cracks between the bark ridges, glowing here and there.
            c.px(x, y, rgb("e0601a") if (y * 3 + x) % 11 == 0 else CHAR[0])
    return c.img


def log_end():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            c.px(x, y, CHAR[0] if d > 7 else [rgb("5a3a20"), rgb("6a4628")][int(d) % 2] if d > 2 else rgb("e0601a"))
    return c.img


def embers(lit):
    c = Canvas()
    palette = [rgb("8a1a06"), rgb("c8380a"), rgb("f07a1a"), rgb("ffc040")] if lit else [rgb("3a3634"), rgb("4a4644"), rgb("5c5854"),
                                                                                       rgb("2a2624")]
    bs.fill(c, 0, 0, 15, 15, palette, 22124 if lit else 22125, [2, 3, 2, 1])
    return c.img


def skewer():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("8a6a3e"), rgb("9a7848"), rgb("aa8654")], 22126)
    return c.img


def flame():
    """A big tongue of fire: yellow-white at its root, orange, then red at its ragged tip; see-through round it."""
    img = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    for y in range(32):
        t = y / 31.0                      # 0 at the tip, 1 at the root
        half = 1.5 + 6.3 * (t ** 0.45)
        for x in range(16):
            d = abs(x - 7.5) / half
            if d > 1:
                continue
            heat = (1 - d) * 0.6 + t * 0.6
            colour = rgb("fff2b0") if heat > 0.95 else rgb("ffc040") if heat > 0.75 else rgb("f07a1a") if heat > 0.5 else rgb("c8380a")
            alpha = int(255 * min(1.0, 0.35 + t * 0.9) * (1 - d * 0.5))
            img.putpixel((x, y), colour + (alpha,))
    return img


# ---------------------------------------------------------------- the haunted hayride

def planks():
    c = Canvas()
    bs.planks([PLANK[3]] + PLANK[:3], 22131)(c)
    for y in range(1, 16, 4):
        c.px(2, y, IRON[1])
        c.px(13, y, IRON[1])
    return c.img


def hay_side():
    """A bale's side: straw in long strands, bound with two twine bands."""
    c = Canvas()
    bs.streaks([HAY[3]] + HAY[:3], 22132, vertical=False, spread=0.9)(c)
    for y in range(16):
        for x in (4, 11):
            c.px(x, y, rgb("6a4a20"))
    return c.img


def hay_top():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, HAY, 22133, [2, 3, 2, 1])
    for x in range(16):
        c.px(x, 4, rgb("6a4a20"))
        c.px(x, 11, rgb("6a4a20"))
    return c.img


def wheel():
    """A wooden cart wheel: an iron-shod rim, eight spokes and a hub; clear between the spokes."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            angle = math.degrees(math.atan2(dy, dx)) % 45
            if 6.6 <= d < 8:
                put(img, x, y, IRON[1])
            elif 5.4 <= d < 6.6:
                put(img, x, y, PLANK[3])
            elif d < 1.8:
                put(img, x, y, IRON[2])
            elif d < 5.4 and (angle < 9 or angle > 36):
                put(img, x, y, PLANK[1])
    return img


def iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 22135)
    return c.img


def pumpkin_rind():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, ORANGE[0] if x % 4 == 0 else ORANGE[2 if x % 4 == 2 else 1])
    return c.img


FACE = [
    "................",
    "................",
    "................",
    "...YY......YY...",
    "...YYY....YYY...",
    "...YYYY..YYYY...",
    "................",
    ".......YY.......",
    "................",
    "..Y..........Y..",
    "..YYY.YYYY.YYY..",
    "...YYYYYYYYYY...",
    "....YYY..YYY....",
    "................",
    "................",
    "................"]


def pumpkin_face():
    """The lantern's grin, lit from inside."""
    c = Canvas()
    base = pumpkin_rind()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("ffe46a") if FACE[y][x] == "Y" else base.getpixel((x, y))[:3])
    return c.img


HAYRIDE_ICON = [
    "................",
    "..........P.....",
    "..........P.OO..",
    "..........PPOO..",
    "..........P.....",
    "..HHHHHHHHP.....",
    ".HHHHHHHHHHH....",
    ".BBBBBBBBBBBB...",
    ".B..........B...",
    ".BBBBBBBBBBBB...",
    "..WWW....WWW....",
    ".W.I.W..W.I.W...",
    ".W.I.W..W.I.W...",
    "..WWW....WWW....",
    "................",
    "................"]


def hayride_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    colours = {"H": HAY[2], "B": PLANK[1], "W": PLANK[3], "I": IRON[2], "P": PLANK[3], "O": ORANGE[2]}
    for y, row in enumerate(HAYRIDE_ICON):
        for x, ch in enumerate(row):
            if ch in colours:
                put(img, x, y, colours[ch])
    return img


# ---------------------------------------------------------------- marshmallows

def mallow(colours, on_stick, seed):
    """A marshmallow (a soft square), toasted or burnt by `colours` (top, side, shadow), on a stick if `on_stick`."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    if on_stick:
        for i in range(9):
            put(img, 3 + i // 2 + i % 2, 14 - i, rgb("8a6a3e") if i % 2 else rgb("6a4c2a"))
        x0, y0 = 6, 2
    else:
        x0, y0 = 4, 4
    for y in range(7):
        for x in range(7):
            if (x, y) in ((0, 0), (6, 0), (0, 6), (6, 6)):
                continue
            tone = colours[0] if y < 2 else colours[2] if x > 4 or y > 4 else colours[1]
            put(img, x0 + x, y0 + y, tone)
    return img


def decor12_textures():
    """(kind, name) -> image for every texture of the twelfth decorations batch."""
    raw = (MALLOW, rgb("e8e2d6"), rgb("d0c8b8"))
    toasted = (rgb("c47a2a"), rgb("e0a050"), rgb("b06820"))
    burnt = (rgb("1e1612"), rgb("3a2a1e"), rgb("140e0a"))
    return {
        ("block", "toilet_paper_streamer"): streamer(),
        ("block", "toilet_paper_drape"): drape(),
        ("item", "toilet_paper_roll"): roll(),
        ("block", "halloween_bonfire_stone"): stone(),
        ("block", "halloween_bonfire_log"): log(),
        ("block", "halloween_bonfire_log_end"): log_end(),
        ("block", "halloween_bonfire_embers"): embers(True),
        ("block", "halloween_bonfire_embers_out"): embers(False),
        ("block", "halloween_bonfire_skewer"): skewer(),
        ("entity", "bonfire_flame"): flame(),
        ("block", "hayride_planks"): planks(),
        ("block", "hayride_hay_side"): hay_side(),
        ("block", "hayride_hay_top"): hay_top(),
        ("block", "hayride_wheel"): wheel(),
        ("block", "hayride_iron"): iron(),
        ("block", "hayride_pumpkin"): pumpkin_rind(),
        ("block", "hayride_pumpkin_face"): pumpkin_face(),
        ("item", "haunted_hayride"): hayride_icon(),
        ("item", "marshmallow"): mallow(raw, False, 22141),
        ("item", "marshmallow_on_a_stick"): mallow(raw, True, 22142),
        ("item", "toasted_marshmallow"): mallow(toasted, True, 22143),
        ("item", "burnt_marshmallow"): mallow(burnt, True, 22144),
    }
