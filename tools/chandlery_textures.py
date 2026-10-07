"""Original textures for the chandlery (fall additions 1) (requires Pillow): the Wax Melting Pot's hammered copper and
its dark inside, the candle's brass dish, and for the client's renderers the wax (pale and grey, tinted by its colour
as it is drawn), the wax's surface in the pot and the candle flame (white at its heart, tinted by its scent); and the
candle's item in two layers, its body (tinted by its dyed colour) and its wick and dish.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. The block textures are 16x16 and opaque; the flame and the items are see-through round
their shapes.
"""
import math
import random

from PIL import Image

from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import put

COPPER = [rgb("8a4a26"), rgb("a85c32"), rgb("c4743e"), rgb("e0925a")]
DARK_COPPER = [rgb("3a2014"), rgb("4a2a1a"), rgb("5a3420")]
BRASS = [rgb("8a6a22"), rgb("b0882e"), rgb("d0a840"), rgb("f0d070")]
WAX = [rgb("c8c8c4"), rgb("dcdcd8"), rgb("ececea"), rgb("f8f8f6")]


def pot():
    """Hammered copper: dimpled, with a band of rivets."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, COPPER[:3], 25101, [2, 3, 2])
    for x, y in ((2, 5), (7, 4), (12, 6), (4, 9), (10, 10), (14, 9), (1, 11), (7, 12)):
        c.px(x, y, COPPER[3])  # a hammer dimple, lit at its upper left
        c.px(x + 1, y + 1, COPPER[0])
    for x in range(1, 16, 4):
        c.px(x, 2, COPPER[3])
        c.px(x, 13, COPPER[3])
    return c.img


def pot_inside():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, DARK_COPPER, 25103, [2, 3, 2])
    return c.img


def dish():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BRASS[:3], 25104, [1, 3, 2])
    for i in range(4, 12):
        c.px(i, 5, BRASS[3])
    return c.img


def wax_surface():
    """Wax seen from above: pale, with soft swirls (tinted by its colour as it is drawn)."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            swirl = math.sin(x * 0.6 + math.sin(y * 0.4) * 2.2)
            c.px(x, y, WAX[3] if swirl > 0.7 else WAX[2] if swirl > -0.4 else WAX[1])
    return c.img


def candle_wax():
    """A candle's side: the faint rings of its dipped layers and a few runs (tinted by its colour as it is drawn)."""
    c = Canvas()
    rng = random.Random(25105)
    wax = bs.surface(WAX[2:4], 25106, [2, 1], spread=0.6)
    for y in range(16):
        ring = WAX[1] if y % 4 == 0 else None
        for x in range(16):
            c.px(x, y, ring or wax(x, y))
    for x in (3, 9, 13):
        for y in range(rng.randrange(0, 4), rng.randrange(8, 14)):
            c.px(x, y, WAX[3])
    return c.img


def flame():
    """A candle flame: white at its heart, paler towards its edges (tinted by the scent as it is drawn); see-through
    round it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        t = y / 15.0                                   # 0 at the tip, 1 at the base
        half = 0.4 + 5.4 * math.sin(math.pi * min(0.97, t) ** 1.6)
        for x in range(16):
            d = abs(x - 7.5) / half
            if d > 1:
                continue
            core = d < 0.45 and t > 0.45
            shade = 255 if core else int(200 + 55 * (1 - d))
            put(img, x, y, (shade, shade, shade), 255 if d < 0.8 else 200)
    return img


CANDLE_ICON = [
    "................",
    "................",
    "................",
    "................",
    "......WWWW......",
    "......LWWWD.....",
    "......LWWWD.....",
    "......LWWWDD....",
    "......LWWWD.....",
    "......LWWWD.....",
    "......LWWWD.....",
    "......LWWWD.....",
    "......LWWWD.....",
    "................",
    "................",
    "................"]

WICK_ICON = [
    "................",
    "........F.......",
    ".......FYF......",
    ".......FYF......",
    "........K.......",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "...BBBBBBBBBBR..",
    "....bbbbbbbb.R..",
    "................"]


def candle_item():
    """The candle's body for its item (tinted by its dyed colour): pale, shaded at its sides."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(CANDLE_ICON):
        for x, ch in enumerate(row):
            colour = {"W": WAX[3], "L": WAX[1], "D": WAX[0]}.get(ch)
            if colour:
                put(img, x, y, colour)
    return img


def wick_item():
    """The candle's wick, its flame and its brass dish, untinted."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    colours = {"F": rgb("ff9a2a"), "Y": rgb("ffe890"), "K": rgb("2a2018"), "B": BRASS[2], "b": BRASS[0], "R": BRASS[1]}
    for y, row in enumerate(WICK_ICON):
        for x, ch in enumerate(row):
            if ch in colours:
                put(img, x, y, colours[ch])
    return img


def chandlery_textures():
    """(kind, name) -> image for every texture of the chandlery."""
    return {
        ("block", "wax_pot"): pot(),
        ("block", "wax_pot_inside"): pot_inside(),
        ("block", "candle_dish"): dish(),
        ("entity", "wax_surface"): wax_surface(),
        ("entity", "candle_wax"): candle_wax(),
        ("entity", "candle_flame"): flame(),
        ("item", "aura_candle"): candle_item(),
        ("item", "aura_candle_wick"): wick_item(),
    }
