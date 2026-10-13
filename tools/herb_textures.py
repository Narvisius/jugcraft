"""Original 16x16 textures for the kitchen herbs (tools/herbs.py) (requires Pillow): the eight herbs in four stages each
(sprouting, young, bushy, grown), their sprigs, their bundles hung to dry (fresh and dried), Dried Herbs, the Planter Box,
and the herb dishes. The owner chose Jugcraft's own art for these (9 October 2026).

Herb textures are drawn for the "crop" model (four upright planes, row 15 at the soil), as the pepper's are; the potted
herbs show their grown stage in vanilla's flower pot. Called from crop_textures.crop_textures(). Every pixel is drawn here
by code; no Mojang texture is read, traced or recoloured.
"""
import math
import random

from crop_textures import Canvas, rgb, outline
from kitchen_textures import bowl_item
import herbs

STEM = [rgb("2f5a1d"), rgb("447a28"), rgb("5e9936"), rgb("7fb24e")]
WOODY = [rgb("4a3020"), rgb("6a4630"), rgb("8a6040"), rgb("a87a54")]
STRING = [rgb("a8946a"), rgb("ccb88a"), rgb("e8dcb4")]
# Each herb's leaf greens (darkest to lightest) and stem.
GREENS = {
    "basil": [rgb("1e5016"), rgb("2a6a1c"), rgb("3a8626"), rgb("50a234"), rgb("70be4a"), rgb("9ad86c")],
    "mint": [rgb("1e4a26"), rgb("2a6232"), rgb("3a7c42"), rgb("4e9656"), rgb("6eb072"), rgb("98cc98")],
    "rosemary": [rgb("1e3a2a"), rgb("2a4c36"), rgb("3a6046"), rgb("4e7658"), rgb("6a9070"), rgb("8eac90")],
    "thyme": [rgb("2a3e20"), rgb("3a522a"), rgb("4c6836"), rgb("627e46"), rgb("7e985c"), rgb("a2b67c")],
    "parsley": [rgb("1a4a14"), rgb("26621c"), rgb("347c26"), rgb("469832"), rgb("60b244"), rgb("86ca62")],
    "sage": [rgb("3a4a3a"), rgb("4c5e4a"), rgb("62765e"), rgb("7a8e74"), rgb("96a88e"), rgb("b6c4ac")],
    "dill": [rgb("2e4e1a"), rgb("3e6622"), rgb("52802e"), rgb("6a9a3c"), rgb("88b452"), rgb("aace74")],
    "chives": [rgb("1e4a1e"), rgb("2a6228"), rgb("387c34"), rgb("4a9644"), rgb("64ae58"), rgb("88c87a")],
}
FLOWERS = {"thyme": [rgb("9a5a9a"), rgb("c88ac8"), rgb("ecc0ec")], "chives": [rgb("7a3a8a"), rgb("a85ab8"), rgb("d090dc")],
           "dill": [rgb("b89a18"), rgb("dcc02a"), rgb("f4e060")], "basil": [rgb("c8c0d8"), rgb("ece6f6"), rgb("ffffff")],
           "rosemary": [rgb("5a6ab8"), rgb("8a9ad8"), rgb("bcc8f0")], "sage": [rgb("6a5ab0"), rgb("9a8ad0"), rgb("c4b8ec")],
           "mint": [rgb("b0a0d0"), rgb("d4c8ec"), rgb("f0eafc")], "parsley": [rgb("c8d8a0"), rgb("e0ecc0"), rgb("f4fae4")]}


def dry(palette):
    """A herb's greens dried: faded toward grey-brown, as hung herbs go."""
    out = []
    for r, g, b in palette:
        grey = (r + g + b) // 3
        out.append((round(r * 0.45 + grey * 0.3 + 70 * 0.25), round(g * 0.5 + grey * 0.3 + 58 * 0.2),
                    round(b * 0.35 + grey * 0.3 + 34 * 0.35)))
    return out


# ---------------------------------------------------------------- leaves, by herb

def pair_leaves(c, x, y, size, palette, pointed=False):
    """A pair of leaves held out either side of the stem at (x, y): basil's broad ones, mint's and sage's."""
    for d in (-1, 1):
        for i in range(size):
            width = 1 if i in (0, size - 1) and size > 2 else 1 + (size > 2)
            for w in range(width):
                tone = 4 if w == 0 else 2
                c.px(x + d * (1 + i), y - w + (i == size - 1 and pointed), palette[tone])
        c.px(x + d * (1 + size // 2), y - (size > 2), palette[5])


def needle_sprig(c, x, top, bottom, palette, lean=0.0):
    """A woody stem set all along with short needles (rosemary)."""
    for y in range(top, bottom + 1):
        cx = x + lean * (bottom - y) / max(1, bottom - top)
        c.px(cx, y, WOODY[2])
        if (y - top) % 2 == 0 and y < bottom:
            c.px(cx - 1, y, palette[3])
            c.px(cx + 1, y - 1, palette[4])
            c.px(cx - 2, y + 1, palette[2])
            c.px(cx + 2, y, palette[2])


def tiny_mound(c, cx, top, width, palette, rng):
    """A low mound of tiny leaves on wiry stems (thyme)."""
    for y in range(top, 16):
        half = width * (y - top + 1) / (16 - top)
        for x in range(int(cx - half), int(cx + half) + 1):
            if rng.random() < 0.7:
                c.px(x, y, palette[rng.choice((2, 3, 3, 4, 5))])


def frilly(c, x, y, radius, palette, rng):
    """A frilled cluster of small leaflets (parsley)."""
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            if math.hypot(dx, dy) <= radius + 0.2 and rng.random() < 0.8:
                c.px(x + dx, y + dy, palette[5 if dx + dy < -radius else 4 if dx + dy < 0 else 3 if dx + dy < radius else 2])


def feathery(c, x, top, bottom, palette, rng):
    """A tall, hollow stem with thread-fine leaves feathering out of it (dill)."""
    for y in range(top, bottom + 1):
        c.px(x, y, palette[2])
        if (y + x) % 2 and y < bottom - 1:
            for d in (-1, 1):
                length = rng.choice((1, 2, 2, 3))
                for k in range(1, length + 1):
                    c.px(x + d * k, y - (k > 1), palette[3 + (k == length)])


def tubes(c, count, height, palette, rng):
    """Chives: thin, hollow, upright leaves fanning a little."""
    for i in range(count):
        lean = (i - (count - 1) / 2) * 0.7
        h = height - rng.choice((0, 1, 1, 2))
        for k in range(h):
            t = k / max(1, h)
            c.px(7.5 + lean * t * 2, 15 - k, palette[2 + (k % 2)] if t < 0.85 else palette[4])


# ---------------------------------------------------------------- the herbs' stages

def herb_stage(herb, stage):
    """Sprouting (0), young (1), bushy (2) and grown (3); grown, several flower as they would in a garden."""
    c = Canvas()
    g = GREENS[herb]
    rng = random.Random(f"{herb}{stage}")
    height = (4, 7, 10, 12)[stage]
    top = 15 - height
    flowers = FLOWERS[herb] if stage == 3 else None
    if herb in ("basil", "mint", "sage"):
        stems = (7,) if stage < 2 else (5, 8, 11) if stage == 3 else (6, 9)
        for x in stems:
            c.line(7.5, 15, x, top + 1, STEM[1] if herb != "sage" else WOODY[2])
            size = {"basil": 3, "mint": 2, "sage": 3}[herb] - (stage == 0)
            for y in range(top + 2, 15, 3):
                pair_leaves(c, x + (y - top) * (7.5 - x) / max(1, 14 - top), y, max(1, size), g, pointed=herb == "mint")
            if flowers:
                for k in range(2):
                    c.px(x, top - k, flowers[1 + k % 2])
                    c.px(x + 1, top - k + 1, flowers[0])
    elif herb == "rosemary":
        for x, lean in ((6, -1.5), (9, 1.5), (7.5, 0.0))[: 1 + stage]:
            needle_sprig(c, x, top, 15, g, lean)
        if flowers:
            for x, y in ((5, top + 2), (10, top + 3), (7, top + 1)):
                c.px(x, y, flowers[1])
                c.px(x + 1, y, flowers[2])
    elif herb == "thyme":
        tiny_mound(c, 7.5, top + 1, 2 + stage * 2, g, rng)
        if flowers:
            for x in range(3, 13, 2):
                c.px(x, top + rng.choice((0, 1, 2)), flowers[rng.choice((1, 2))])
    elif herb == "parsley":
        for x in (5, 8, 11)[: 1 + stage]:
            c.line(7.5, 15, x, top + 3, STEM[2])
        for x, y, r in ((7, top + 2, 2), (4, top + 4, 2), (11, top + 4, 2), (6, top + 7, 2), (10, top + 7, 2))[: 1 + stage * 2]:
            frilly(c, x, y, r if stage > 0 else 1, g, rng)
        if flowers:
            for x in (5, 9):
                c.px(x, top, flowers[2])
                c.px(x + 1, top, flowers[1])
    elif herb == "dill":
        for x in (6, 9, 7)[: 1 + stage]:
            feathery(c, x, top + (x == 7), 15, g, rng)
        if flowers:
            # Yellow umbels, flat-topped, on the tallest stems.
            for x in (6, 9):
                for dx in range(-2, 3):
                    c.px(x + dx, top - 1 + (abs(dx) == 2), flowers[2 if dx % 2 else 1])
                c.px(x, top, flowers[0])
    elif herb == "chives":
        tubes(c, (3, 5, 7, 8)[stage], height, g, rng)
        if flowers:
            for x, y in ((5, top), (10, top + 1), (7, top - 1)):
                for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
                    c.px(x + dx, y + dy, flowers[1 + (dx + dy < 0)])
    return c.img


# ---------------------------------------------------------------- sprigs, bundles and the planter box

def sprig_item(herb):
    """A cut sprig of the herb, lying across the slot."""
    c = Canvas()
    g = GREENS[herb]
    rng = random.Random(herb)
    if herb == "chives":
        # A handful of thin hollow leaves, cut, one still carrying its purple flower.
        for i in range(4):
            c.line(3 + 2 * i, 14, 6 + 2 * i, 3 + i % 2, g[2 + i % 3])
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
            c.px(12 + dx, 3 + dy, FLOWERS["chives"][1 + (dx + dy < 0)])
        outline(c, g[0])
        return c.img
    if herb == "rosemary":
        needle_sprig(c, 8, 2, 14, g, -3.0)
        outline(c, g[0])
        return c.img
    if herb == "thyme":
        for x0, y0 in ((4, 13), (7, 14), (10, 13)):
            c.line(x0, y0, x0 + 3, y0 - 10, WOODY[2])
            for k in range(0, 10, 2):
                c.px(x0 + k * 0.3 - 1, y0 - k, g[3])
                c.px(x0 + k * 0.3 + 1, y0 - k - 1, g[4])
        outline(c, g[0])
        return c.img
    if herb == "dill":
        feathery(c, 8, 2, 14, g, rng)
        outline(c, g[0])
        return c.img
    if herb == "parsley":
        c.line(8, 14, 8, 7, STEM[2])
        for x, y in ((6, 5), (10, 5), (8, 3), (5, 8), (11, 8)):
            frilly(c, x, y, 2, g, rng)
        outline(c, g[0])
        return c.img
    # Basil, mint and sage: a stem with paired leaves, the top pair largest.
    c.line(8, 14, 8, 3, STEM[2] if herb != "sage" else WOODY[2])
    for y, size in ((4, 3), (8, 3), (12, 2)):
        pair_leaves(c, 8, y, size + (herb == "basil"), g, pointed=herb == "mint")
    outline(c, g[0])
    return c.img


def bundle_texture(herb, dried):
    """A bunch of the herb tied with string, hung stems up from a loop at the top of the block."""
    c = Canvas()
    g = dry(GREENS[herb]) if dried else GREENS[herb]
    rng = random.Random(f"bundle{herb}{dried}")
    # The string loop and the tie.
    for y in range(0, 3):
        c.px(7, y, STRING[1])
        c.px(8, y, STRING[0])
    for x in range(6, 10):
        c.px(x, 3, STRING[2] if x % 2 else STRING[1])
    # Stems above the tie are hidden by it; the bunch spreads below, leaves pointing down.
    for y in range(4, 15):
        half = 1 + (y - 4) * 0.45
        for x in range(int(7.5 - half), int(8.5 + half) + 1):
            if rng.random() < (0.85 if y < 12 else 0.6):
                tone = rng.choice((1, 2, 3, 3, 4)) if not dried else rng.choice((1, 2, 2, 3, 4))
                c.px(x, y, g[tone])
    if not dried and herb in FLOWERS and herb in ("thyme", "chives", "dill"):
        for x, y in ((5, 12), (10, 13), (8, 14)):
            c.px(x, y, FLOWERS[herb][1])
    return c.img


def dried_herbs_item():
    """A little heap of crumbled dried herbs on a scrap of paper."""
    c = Canvas()
    rng = random.Random("dried_herbs")
    for y in range(10, 14):
        for x in range(3, 14):
            c.px(x, y, rgb("e8dcc0") if y > 11 else rgb("f4ecd8"))
    for y in range(5, 12):
        half = (y - 4) * 0.9
        for x in range(int(8 - half), int(8 + half) + 1):
            if rng.random() < 0.9:
                c.px(x, y, dry(GREENS[rng.choice(list(GREENS))])[rng.choice((1, 2, 3, 4))])
    outline(c, rgb("3a2e1a"))
    return c.img


PLANK = [rgb("4a3218"), rgb("6a4826"), rgb("8a6234"), rgb("a87c46"), rgb("c49a60")]
SOIL = [rgb("2a1c10"), rgb("3e2a18"), rgb("523822"), rgb("6a4a2e")]


def planter_side():
    """A wooden planter box's side: three planks nailed across, the soil's dark line at the top."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            band = y // 5
            tone = 3 if (y % 5) else 1
            if x in (0, 15):
                tone = 2
            c.px(x, y, PLANK[tone] if band < 3 else PLANK[2])
    for x in range(16):
        c.px(x, 0, SOIL[1])
    for x, y in ((2, 2), (13, 2), (2, 7), (13, 7), (2, 12), (13, 12)):
        c.px(x, y, rgb("5a5a60"))
    return c.img


def planter_top():
    """The planter's top: dark, damp soil inside a wooden rim."""
    c = Canvas()
    rng = random.Random("planter_top")
    for y in range(16):
        for x in range(16):
            rim = x in (0, 15) or y in (0, 15)
            c.px(x, y, PLANK[3 if (x + y) % 3 else 2] if rim else SOIL[rng.choice((0, 1, 1, 2, 2, 3))])
    return c.img


def planter_bottom():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, PLANK[2 if y % 4 else 1])
    return c.img


def mint_tea_item():
    """A glass bottle of pale green tea with a mint leaf floating in it."""
    c = Canvas()
    glass = [rgb("6a8a8a"), rgb("a8c8c8"), rgb("e0f0f0")]
    tea = [rgb("6a8a2a"), rgb("8aaa3a"), rgb("aac85a")]
    for y in range(6, 15):
        half = 3 if y > 7 else 2
        for x in range(8 - half, 8 + half + 1):
            c.px(x, y, tea[1 + (x < 8)] if y > 8 else glass[1])
    for y in range(2, 6):
        c.px(7, y, glass[1])
        c.px(8, y, glass[2])
    c.px(7, 1, rgb("8a6a40"))
    c.px(8, 1, rgb("8a6a40"))
    c.px(6, 10, GREENS["mint"][5])
    c.px(7, 9, GREENS["mint"][4])
    outline(c, glass[0])
    return c.img


def herb_textures():
    out = {}
    for herb in herbs.HERBS:
        for stage in range(herbs.STAGES):
            out[("block", herbs.stage_texture(herb, stage))] = herb_stage(herb, stage)
        out[("item", herb)] = sprig_item(herb)
        out[("block", herbs.bundle(herb))] = bundle_texture(herb, False)
        out[("block", f"{herbs.bundle(herb)}_dried")] = bundle_texture(herb, True)
    textures = herbs.PLANTER["textures"]
    out[("block", textures["side"])] = planter_side()
    out[("block", textures["top"])] = planter_top()
    out[("block", textures["bottom"])] = planter_bottom()
    out[("item", herbs.BUNDLE["dried"])] = dried_herbs_item()
    out.update({
        ("item", "pesto_pasta"): bowl_item([rgb("c8a85a"), rgb("dcc070"), rgb("eed890")],
                                           bits=[(GREENS["basil"][3], [(5, 5), (8, 6), (10, 5), (6, 7)]),
                                                 (GREENS["basil"][4], [(9, 4), (4, 6)])]),
        ("item", "sage_and_onion_stuffing"): bowl_item([rgb("8a5a2a"), rgb("a8743a"), rgb("c4904c")],
                                                       bits=[(GREENS["sage"][4], [(5, 5), (9, 6)]), (rgb("f0e8d0"), [(7, 5), (11, 6)])]),
        ("item", "herb_roasted_mutton"): bowl_item([rgb("6a3a1e"), rgb("8a4e2a"), rgb("a8683a")],
                                                   bits=[(GREENS["rosemary"][4], [(5, 5), (6, 4), (10, 5)]),
                                                         (rgb("e8c070"), [(8, 6), (11, 6)])]),
        ("item", "garden_herb_soup"): bowl_item([rgb("8aa040"), rgb("a2b85a"), rgb("bccc78")],
                                                bits=[(GREENS["parsley"][4], [(5, 6), (9, 5)]), (rgb("e88a2a"), [(7, 6)]),
                                                      (GREENS["chives"][3], [(11, 6)])]),
        ("item", "herb_roasted_potatoes"): bowl_item([rgb("b88a3a"), rgb("d4a64e"), rgb("ecc26a")],
                                                     bits=[(GREENS["thyme"][2], [(5, 5), (9, 4), (11, 6)]),
                                                           (rgb("8a5a1e"), [(7, 6), (6, 4)])]),
        ("item", "mint_tea"): mint_tea_item(),
    })
    return out
