"""Original textures for the Día de Muertos ofrenda (fall additions 20) (requires Pillow): the altar's white linen cloth;
its riser fronts (the bottom five rows): a band embroidered with little flowers in pink, orange, purple and green over
a scalloped lace edge; marigold petals scattered on the ground; papel picado, three cut-paper flags (pink, orange,
purple) on a string, each cut through with a flower and scallops; the sugar skull's iced face (eyes ringed with petals, a
heart nose, a stitched smile, a flower on the brow) and its sugar sides; and pan de muerto and its dough as items. The
marigold flower itself is drawn with the mums.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured.
"""
from PIL import Image

from crop_textures import Canvas, rgb, outline
from halloween_textures import shade
import block_style as bs

LINEN = [rgb("e8e4da"), rgb("f0ece2"), rgb("f6f2ea")]
PINK = rgb("e0508a")
ORANGE = rgb("f08a1c")
PURPLE = rgb("8a46b8")
GREEN = rgb("3a9a4a")
BLUE = rgb("3aa0d8")
YELLOW = rgb("f4cc30")
MARIGOLD = [rgb("c85a0c"), rgb("e87a12"), rgb("f89a20"), rgb("ffbe40")]
SUGAR = [rgb("ece8e2"), rgb("f4f0ea"), rgb("faf8f4")]
BREAD = [rgb("a8601e"), rgb("c47a2a"), rgb("d8943a"), rgb("e8ae52")]


def cloth():
    """White linen, a fine weave."""
    img = Image.new("RGBA", (16, 16))
    linen = bs.surface(LINEN, 20001, [1, 3, 2], spread=0.6)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), linen(x, y) + (255,))
    return img


def cloth_front():
    """Linen above; in the bottom five rows a band of embroidered flowers and a scalloped lace edge."""
    img = cloth()
    colours = [PINK, ORANGE, PURPLE, GREEN]
    for i, cx in enumerate(range(1, 16, 4)):
        colour = colours[i % 4]
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            x, y = cx + dx, 12 + dy
            if 0 <= x < 16:
                img.putpixel((x, y), (YELLOW if (dx, dy) == (0, 0) else colour) + (255,))
    for x in range(16):
        img.putpixel((x, 14), shade(LINEN[0], 0.92) + (255,))
        if x % 2 == 0:
            img.putpixel((x, 15), shade(LINEN[0], 0.85) + (255,))
        else:
            img.putpixel((x, 15), shade(LINEN[0], 0.7) + (255,))
    return img


def petals():
    """Marigold petals strewn thick on the ground in little clumps, each lit at its upper left, the soil showing
    through between them."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for gy in range(4):
        for gx in range(4):
            x, y = gx * 4 + (gy % 2) * 2, gy * 4 + (gx % 2)
            for dx, dy, tone in ((0, 0, -1), (1, 0, -2), (0, 1, -2), (1, 1, -3)):
                img.putpixel(((x + dx) % 16, (y + dy) % 16), MARIGOLD[tone] + (255,))
    return img


def papel_picado():
    """Three cut-paper flags on a string: each cut with a flower in the middle and scallops along its foot."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        img.putpixel((x, 1), rgb("d8d0c0") + (255,))
    for i, colour in enumerate((PINK, ORANGE, PURPLE)):
        x0 = 1 + i * 5
        for x in range(x0, x0 + 4):
            for y in range(2, 13):
                cut = False
                # The flower cut through the middle, and the scalloped foot.
                if y in (6, 8) and x in (x0 + 1, x0 + 2) or y == 7 and x in (x0, x0 + 3):
                    cut = True
                if y == 12 and (x - x0) % 2 == 1:
                    cut = True
                if y == 10 and x in (x0 + 1, x0 + 2):
                    cut = True
                if not cut:
                    img.putpixel((x, y), colour + (255,))
    return img


def skull_face():
    """A sugar skull's iced face (on the region the model's face reads: x 5 to 10, rows 4 to 9)."""
    img = Image.new("RGBA", (16, 16))
    sugar = bs.surface(SUGAR, 20020, spread=0.6)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), sugar(x, y) + (255,))
    # The eyes: dark sockets ringed with petals.
    for ex, ring in ((6, BLUE), (9, PINK)):
        img.putpixel((ex, 6), rgb("2a2028") + (255,))
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            img.putpixel((ex + dx, 6 + dy), ring + (255,))
    # A flower on the brow, a heart for a nose, and a stitched smile.
    img.putpixel((7, 4), ORANGE + (255,))
    img.putpixel((8, 4), YELLOW + (255,))
    img.putpixel((7, 7), PINK + (255,))
    img.putpixel((8, 7), PINK + (255,))
    for x in range(6, 10):
        img.putpixel((x, 9), rgb("2a2028") + (255,) if x % 2 == 0 else GREEN + (255,))
    return img


def skull_side():
    """Moulded sugar, sparkling, dotted with icing."""
    img = Image.new("RGBA", (16, 16))
    sugar = bs.surface(SUGAR, 20030, spread=0.6)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), sugar(x, y) + (255,))
    for (x, y), colour in zip(((3, 3), (11, 4), (7, 8), (2, 12), (12, 12), (8, 14)), (PINK, BLUE, ORANGE, PURPLE, GREEN, PINK)):
        img.putpixel((x, y), colour + (255,))  # dots of icing, set evenly
    return img


def bread_item(dough):
    """Pan de muerto: a round golden loaf crossed with dough bones and a knob on top, sugared; or its pale dough."""
    c = Canvas()
    palette = BREAD if not dough else [rgb("d8c090"), rgb("e4cea0"), rgb("eedab0"), rgb("f6e6c4")]
    for y in range(4, 14):
        for x in range(2, 14):
            if (x - 7.5) ** 2 / 36 + (y - 9) ** 2 / 25 <= 1:
                c.px(x, y, palette[3 if x + y < 13 else 2 if x + y < 19 else 1])  # lit at its upper left
    for i in range(3, 13):
        c.px(i, 4 + (i - 3) * 9 // 10, shade(palette[3], 1.05))
        c.px(i, 13 - (i - 3) * 9 // 10, shade(palette[3], 1.05))
    for x, y in ((7, 8), (8, 8), (7, 9), (8, 9)):
        c.px(x, y, shade(palette[3], 1.1))
    if not dough:
        for x, y in ((4, 7), (10, 6), (6, 11), (11, 10), (9, 12)):
            c.px(x, y, (250, 246, 236))
    outline(c, shade(palette[0], 0.7))
    return c.img


def ofrenda_textures():
    return {("block", "ofrenda_cloth"): cloth(), ("block", "ofrenda_cloth_front"): cloth_front(), ("block", "marigold_petals"): petals(),
            ("block", "papel_picado"): papel_picado(), ("block", "sugar_skull_face"): skull_face(), ("block", "sugar_skull_side"): skull_side(),
            ("item", "marigold_petals"): petals(), ("item", "papel_picado"): papel_picado(), ("item", "pan_de_muerto"): bread_item(False),
            ("item", "pan_de_muerto_dough"): bread_item(True)}
