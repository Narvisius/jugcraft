"""Original textures for autumn foraging (fall additions 12) (requires Pillow): the five wild mushrooms as cross plants
(the chanterelle's golden trumpets, the porcini's brown bun caps on fat stems, a round white puffball, the fly agaric's
white-spotted red cap, and a cluster of orange jack o'lantern mushrooms, which glow), the Foraging Basket, and the foods:
sautéed chanterelles, roasted porcini, fried puffball and Forager's Stew (in the Kitchen Garden's bowl).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. The mushrooms are see-through round their shapes, as vanilla plants are.
"""
import random

from crop_textures import Canvas, rgb, outline
from kitchen_textures import bowl_item

STEM = [rgb("b8a888"), rgb("d8ccb0"), rgb("f0e8d4")]
CHANTERELLE = [rgb("a86410"), rgb("d08a1c"), rgb("eaa832"), rgb("f8c858")]
PORCINI = [rgb("4a2a14"), rgb("6a3e1e"), rgb("8a5830"), rgb("a8744a")]
PUFF = [rgb("b8b0a0"), rgb("d8d2c4"), rgb("ece8de"), rgb("fbf9f4")]
AGARIC = [rgb("8a1010"), rgb("b81c18"), rgb("e03020"), rgb("f05a3a")]
LANTERN = [rgb("a84808"), rgb("d86a10"), rgb("f08a1c"), rgb("ffb84a")]
WICKER = [rgb("6a4a20"), rgb("8a6630"), rgb("ae8644"), rgb("ccaa66")]


def disc(c, cx, cy, rx, ry, shade):
    """Fills an ellipse, `shade(dx, dy)` giving each pixel's colour (dx, dy from -1 to 1)."""
    for y in range(16):
        for x in range(16):
            dx, dy = (x - cx) / rx, (y - cy) / ry
            if dx * dx + dy * dy <= 1:
                c.px(x, y, shade(dx, dy))


def stem(c, x0, x1, y0, y1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c.px(x, y, STEM[2] if x == x0 else STEM[1] if x < x1 else STEM[0])


def chanterelle():
    """Two golden chanterelles: trumpets flaring from slim stems into wavy, upturned rims, a darker hollow at the top and
    ridges running down the sides."""
    c = Canvas()
    for cx, top, widths in ((6, 3, [6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1]), (12, 8, [3.5, 3, 2, 1, 1, 1, 1])):
        for i, half in enumerate(widths):
            for x in range(int(round(cx - half)), int(round(cx + half)) + 1):
                edge = abs(x - cx) >= half - 0.5
                if i == 0 and (x - cx) % 3 == 1:
                    continue  # the wavy rim
                if i == 1 and not edge:
                    color = CHANTERELLE[1]  # the hollow
                elif half <= 1:
                    color = CHANTERELLE[2] if x <= cx else CHANTERELLE[1]  # the stem
                else:
                    color = CHANTERELLE[3] if edge or (x - cx) % 2 == 0 else CHANTERELLE[2]
                c.px(x, top + i, color)
    outline(c, CHANTERELLE[0])
    return c.img


def porcini():
    """Two porcini: brown bun caps, pale at the rim, on fat white stems."""
    c = Canvas()
    stem(c, 3, 6, 10, 15)
    stem(c, 9, 13, 8, 15)
    disc(c, 4.5, 8.5, 3.6, 2.4, lambda dx, dy: PORCINI[3] if dy < -0.5 else PORCINI[2] if dx < 0.3 else PORCINI[1])
    disc(c, 11, 6, 4.4, 2.8, lambda dx, dy: PORCINI[3] if dy < -0.5 else PORCINI[2] if dx < 0.3 else PORCINI[1])
    for x in range(7, 16):
        c.px(x, 8, STEM[0])
    outline(c, PORCINI[0])
    return c.img


def puffball():
    """A round white puffball sitting in the grass, freckled."""
    c = Canvas()
    disc(c, 8, 10.5, 5.2, 4.6, lambda dx, dy: PUFF[3] if dx + dy < -0.6 else PUFF[2] if dx + dy < 0.4 else PUFF[1])
    for x, y in ((6, 9), (9, 8), (11, 10), (7, 12), (10, 12)):
        c.px(x, y, PUFF[0])  # freckles, set evenly
    outline(c, rgb("8a8070"))
    return c.img


def fly_agaric():
    """The fairy-tale toadstool: a red cap with white warts on a white stem with a ring."""
    c = Canvas()
    stem(c, 7, 9, 7, 15)
    for x in range(6, 11):
        c.px(x, 10, STEM[1])
    disc(c, 8, 5.5, 6.5, 3.6, lambda dx, dy: AGARIC[3] if dy < -0.4 and dx < 0 else AGARIC[2] if dy < 0.3 else AGARIC[1])
    for x, y in ((5, 4), (8, 3), (11, 4), (6, 6), (10, 6), (13, 6), (3, 6)):
        c.px(x, y, rgb("fbf6ea"))
    outline(c, AGARIC[0])
    return c.img


def jack_o_lantern_mushroom():
    """A cluster of orange caps on curving stems, bright as embers (it glows)."""
    c = Canvas()
    for cx, cy, r in ((4, 7, 2.6), (9, 4, 3.2), (12, 9, 2.4), (7, 10, 2.2)):
        for y in range(int(cy) + 1, 15):
            c.px(cx + (y - cy) * 0.2, y, LANTERN[1])
        disc(c, cx, cy, r, r * 0.6, lambda dx, dy: LANTERN[3] if dy < -0.3 else LANTERN[2])
    outline(c, LANTERN[0])
    return c.img


def basket():
    """A wicker basket with a high handle, a red cloth tucked in and a chanterelle peeking out."""
    c = Canvas()
    for y in range(8, 15):
        half = 6 - max(0, y - 12)
        for x in range(8 - half, 8 + half):
            c.px(x, y, WICKER[2] if (x + y) % 2 else WICKER[1])
    for x in range(2, 14):
        c.px(x, 8, WICKER[3])
    for i in range(-5, 6):
        x, y = 8 + i, 8 - int((25 - i * i) ** 0.5 * 0.9)
        c.px(x, y, WICKER[2])
    for x in range(4, 8):
        c.px(x, 7, rgb("c02820"))
    c.px(10, 6, CHANTERELLE[3])
    c.px(10, 7, CHANTERELLE[2])
    c.px(9, 6, CHANTERELLE[2])
    outline(c, WICKER[0])
    return c.img


def pan_food(colors, seed):
    """Golden pieces heaped on a small plate."""
    c = Canvas()
    for y in range(10, 14):
        for x in range(2, 14):
            if (x - 7.5) ** 2 / 36 + (y - 11.5) ** 2 / 4 <= 1:
                c.px(x, y, rgb("e8e4dc") if y < 12 else rgb("c8c0b0"))
    rng = random.Random(seed)
    for x, y in ((4, 8), (7, 7), (10, 8), (5, 10), (8, 9), (11, 10), (6, 6), (9, 6)):
        # A golden piece, one colour, lit at its upper left.
        tones = sorted(colors, key=lambda c: sum(c))
        base = rng.randrange(len(tones))
        c.px(x, y, tones[min(len(tones) - 1, base + 1)])
        c.px(x + 1, y, tones[base])
        c.px(x, y + 1, tones[max(0, base - 1)])
    outline(c, rgb("3a2a14"))
    return c.img


def porcini_slices():
    """Thick porcini slices, browned on a skewer."""
    c = Canvas()
    for i, (x0, y0) in enumerate(((3, 9), (6, 6), (9, 3))):
        disc(c, x0 + 2, y0 + 2, 2.6, 2.6, lambda dx, dy: PORCINI[2] if dx + dy < 0 else PORCINI[1])
        c.px(x0 + 2, y0 + 2, STEM[1])
    for i in range(14):
        c.px(1 + i, 14 - i, rgb("8a6a3a"))
    outline(c, PORCINI[0])
    return c.img


def foraging_textures():
    return {
        ("block", "chanterelle"): chanterelle(),
        ("block", "porcini"): porcini(),
        ("block", "puffball"): puffball(),
        ("block", "fly_agaric"): fly_agaric(),
        ("block", "jack_o_lantern_mushroom"): jack_o_lantern_mushroom(),
        ("item", "foraging_basket"): basket(),
        ("item", "sauteed_chanterelles"): pan_food(CHANTERELLE[1:], 27102),
        ("item", "roasted_porcini"): porcini_slices(),
        ("item", "fried_puffball"): pan_food([rgb("e0b060"), rgb("c88a3a"), rgb("f0d090")], 27103),
        ("item", "foragers_stew"): bowl_item([rgb("6a4a2a"), rgb("8a6440"), rgb("a8805a"), rgb("c49c74")],
                                             [(CHANTERELLE[2], [(4, 6), (9, 7)]), (PORCINI[2], [(6, 6), (11, 6)]),
                                              (PUFF[2], [(8, 7), (5, 7)])]),
    }
