"""Original textures for pie baking (fall additions 16) (requires Pillow): the Hearth Oven's red bricks with pale mortar,
its sooty inside, glowing embers and stone hearth; the pies' lattice tops (golden pastry strips over each filling; the
orchards' lemon meringue pie wears toasted meringue instead), the fillings where a pie is cut, the fluted crust edge and
the tin; the near-white crust the oven's renderer tints as a pie bakes; and the raw pies, slices and pastry dough as
items.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured.
"""
from PIL import Image

from agriculture import PIES, pie_name
from crop_textures import Canvas, rgb, outline
from halloween_textures import shade
import block_style as bs

BRICK = [rgb("8a3a26"), rgb("9c4430"), rgb("ae5038"), rgb("7a3220")]
MORTAR = rgb("b8ad9c")
SOOT = [rgb("1a1614"), rgb("241e1a"), rgb("2e2620"), rgb("3a2e26")]
EMBER = [rgb("c83a0e"), rgb("e8601a"), rgb("f8902a"), rgb("ffc04a")]
STONE = [rgb("6e6c68"), rgb("7e7c78"), rgb("8e8c86"), rgb("9c9a94")]
CRUST = [rgb("c8862e"), rgb("d89a40"), rgb("e8b058"), rgb("f0c470")]
RAW = [rgb("e6d0a0"), rgb("eedcb0"), rgb("f4e6c0"), rgb("faeed0")]
TIN = [rgb("8a8c90"), rgb("a0a2a6"), rgb("b4b6ba")]
BURNT = [rgb("1c120c"), rgb("2a1a10"), rgb("3a2616"), rgb("4a3020")]
# Meringue (the orchards' lemon meringue pie, tools/orchard.py): cream to white, and its tips toasted gold.
MERINGUE = [rgb("d6c6a4"), rgb("ebe0c8"), rgb("f7f0e0"), rgb("fffbf2")]
TOAST = [rgb("b27a36"), rgb("d4a45e")]
PEAKS = [(3, 3), (8, 2), (12, 4), (5, 7), (10, 8), (2, 11), (7, 12), (12, 11)]


def noise(palette, seed, weights=None):
    """A plain 16x16 of the palette in small clumps, in the manner of the vanilla blocks (tools/block_style.py)."""
    img = Image.new("RGBA", (16, 16))
    surface = bs.surface(palette, seed, weights)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), surface(x, y) + (255,))
    return img


def painted(painter):
    """A 16x16 image painted by one of tools/block_style.py's painters."""
    return bs.img(painter)


def bricks():
    """Red bricks in courses of four pixels, staggered, with pale mortar, as vanilla bricks: each brick lit along its
    top and left."""
    return painted(bs.bricks([BRICK[3]] + BRICK[:3], 16101, rows=4, cols=2, mortar=MORTAR))


def embers():
    """Glowing coals heaped like vanilla gravel, each lump lit at its upper left."""
    lumps = [[EMBER[0], EMBER[1], EMBER[2]], [EMBER[1], EMBER[2], EMBER[3]]]
    return painted(bs.heap(lumps, 16120, count=12, joint=rgb("3a1406"), weights=[3, 2]))


def filling_palette(color):
    base = ((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF)
    return [shade(base, 0.75), shade(base, 0.9), base, shade(base, 1.12)]


def lattice_top(filling, crust=CRUST, seed=16200):
    """A pie's top: the filling, with a lattice of pastry strips over it (two pixels wide, every five) and a crust rim."""
    img = noise(filling, seed, [1, 3, 3, 1])
    for y in range(16):
        for x in range(16):
            down, across = x % 5 in (1, 2), y % 5 in (1, 2)
            rim = x in (0, 15) or y in (0, 15)
            if rim:
                img.putpixel((x, y), crust[1] + (255,))
            elif down or across:
                # Woven: at each crossing the strip running down and the one running across take turns on top; each
                # strip is lit along its first pixel.
                on_top_down = down and (not across or (x // 5 + y // 5) % 2 == 0)
                lit = x % 5 == 1 if on_top_down else y % 5 == 1
                img.putpixel((x, y), crust[3 if lit else 2] + (255,))
    return img


def meringue_top(seed, crust=CRUST):
    """A meringue pie's top: swirled meringue in peaks, each toasted gold at its tip and shadowed below, in a crust rim."""
    img = noise(MERINGUE, seed, [1, 2, 4, 3])
    for x, y in PEAKS:
        for px, py, colour in ((x, y, TOAST[1]), (x - 1, y + 1, MERINGUE[3]), (x, y + 1, TOAST[0]), (x + 1, y + 1, MERINGUE[2]),
                               (x + 1, y + 2, MERINGUE[0]), (x, y + 2, MERINGUE[1])):
            if 0 < px < 15 and 0 < py < 15:
                img.putpixel((px, py), colour + (255,))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                img.putpixel((x, y), crust[1] + (255,))
    return img


def inside(filling, seed, meringue=False):
    """The filling where a pie is cut, under a line of top crust (or a deep layer of meringue) and over the bottom crust."""
    img = noise(filling, seed, [1, 3, 3, 1])
    top = ((0, MERINGUE[3]), (1, MERINGUE[2]), (2, MERINGUE[2]), (3, MERINGUE[1]), (4, MERINGUE[0])) if meringue else \
        ((0, CRUST[3]), (1, CRUST[2]), (2, CRUST[2]), (3, CRUST[1]))
    for x in range(16):
        for y, colour in top + ((12, CRUST[2]), (13, CRUST[1]), (14, CRUST[1]), (15, CRUST[0])):
            img.putpixel((x, y), colour + (255,))
    return img


def side():
    """The pie's crust edge: fluted pastry in rounded scallops."""
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            tone = CRUST[3] if (x % 3 == 1 and y < 14) else CRUST[2] if y < 14 else CRUST[0]
            img.putpixel((x, y), tone + (255,))
    return img


def crust_white():
    """Near-white crust for the oven's renderer to tint as the pie bakes."""
    img = Image.new("RGBA", (16, 16))
    grain = bs.grain(16, 16, 16300)
    for x in range(16):
        for y in range(16):
            g = grain(x, y)
            v = (240 if g > 0.62 else 214 if g < 0.38 else 228) if (x % 5) not in (1, 2) and (y % 5) not in (1, 2) else 255
            img.putpixel((x, y), (v, v, v, 255))
    return img


def raw_pie_item(filling, meringue=False):
    """A raw pie seen from above: pale pastry lattice over the filling (or dollops of white meringue), in a tin."""
    c = Canvas()
    for y in range(2, 14):
        for x in range(2, 14):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 36:
                if meringue:
                    dollop = (x % 4 in (1, 2)) and (y % 4 in (1, 2))
                    c.px(x, y, MERINGUE[3] if dollop and x + y < 14 else MERINGUE[2] if dollop
                         else filling[3] if x + y < 12 else filling[2])
                    continue
                strip = x % 4 == 1 or y % 4 == 1
                c.px(x, y, RAW[2] if strip else filling[3] if x + y < 12 else filling[2])
    for y in range(2, 14):
        for x in range(2, 14):
            if 30 < (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 42:
                c.px(x, y, RAW[3])
    outline(c, TIN[0])
    return c.img


def slice_item(filling, meringue=False):
    """A wedge of pie: golden lattice (or toasted meringue) on top, the filling and the bottom crust on its cut side."""
    c = Canvas()
    for y in range(3, 13):
        width = int((y - 3) * 1.1) + 2
        for x in range(8 - width // 2, 8 + width // 2 + 1):
            if y < 6 and meringue:
                c.px(x, y, TOAST[1] if y == 3 else MERINGUE[3] if x % 3 else MERINGUE[2])
            elif y < 6:
                c.px(x, y, CRUST[2] if x % 3 else CRUST[3])
            elif y < 11:
                c.px(x, y, filling[3] if y == 6 else filling[2] if y < 9 else filling[1])
            else:
                c.px(x, y, CRUST[1])
    outline(c, shade(CRUST[0], 0.6))
    return c.img


def dough_item():
    """A ball of pastry dough, floured."""
    c = Canvas()
    for y in range(4, 14):
        for x in range(3, 14):
            if (x - 8) ** 2 / 25 + (y - 9) ** 2 / 20 <= 1:
                c.px(x, y, RAW[3] if x + y < 13 else RAW[2] if x + y < 18 else RAW[1])  # lit at its upper left
    for x, y in ((6, 6), (9, 7), (7, 9), (10, 10)):
        c.px(x, y, (250, 248, 240))
    outline(c, shade(RAW[0], 0.7))
    return c.img


def pie_textures():
    out = {("block", "oven_brick"): bricks(), ("block", "oven_soot"): noise(SOOT, 16110, [2, 3, 2, 1]),
           ("block", "oven_embers"): embers(), ("block", "oven_stone"): painted(bs.stone(STONE, 16130, cracks=2)),
           ("block", "pie_side"): side(), ("block", "pie_tin"): noise(TIN, 16140), ("block", "pie_crust"): crust_white(),
           ("block", "burnt_pie_top"): lattice_top(BURNT, BURNT, 16250), ("block", "burnt_pie_inside"): inside(BURNT, 16251),
           ("item", PIES["dough"]): dough_item()}
    for i, (filling, info) in enumerate(PIES["fillings"].items()):
        palette = filling_palette(info["color"])
        name = pie_name(filling)
        meringue = info.get("top") == "meringue"
        out[("item", f"raw_{name}")] = raw_pie_item(palette, meringue)
        if info.get("owner"):
            continue  # its top, filling and slice are the owner's own art (tools/feasts.py, tools/owner_art.py)
        out[("block", f"{name}_top")] = meringue_top(16200 + 10 * i) if meringue else lattice_top(palette, seed=16200 + 10 * i)
        out[("block", f"{name}_inside")] = inside(palette, 16201 + 10 * i, meringue)
        out[("item", f"{name}_slice")] = slice_item(palette, meringue)
    return out
