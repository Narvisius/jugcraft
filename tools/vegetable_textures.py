"""Original 16x16 textures for the vegetables (tools/vegetables.py) (requires Pillow): lettuce, spinach, radishes and peas in
four stages each; the cucumber climbing its trellis; the eggplant and zucchini bushes in six stages; the wild cucumber; and
the vegetables, their seeds and dishes as items. The owner chose Jugcraft's own art for these (9 October 2026).

Crop textures are drawn for the "crop" model (four upright planes, row 15 at the soil) and the cucumber's for the trellis
crop model, as the pepper's and the old tomato's were (tools/kitchen_textures.py). Called from crop_textures.crop_textures().
Every pixel is drawn here by code; no Mojang texture is read, traced or recoloured.
"""
import math

from crop_textures import Canvas, rgb, broad_leaf, stalk, outline, seeds_item
from kitchen_textures import bowl_item, round_fruit
import vegetables

STEM = [rgb("2f5a1d"), rgb("447a28"), rgb("5e9936"), rgb("7fb24e")]
LETTUCE = [rgb("3e6e1e"), rgb("5a8e2a"), rgb("7aae3a"), rgb("9ccc52"), rgb("c0e27a"), rgb("e0f2aa")]
SPINACH = [rgb("143a14"), rgb("1e4e1c"), rgb("2a6426"), rgb("3a7c32"), rgb("4e9444"), rgb("6aae5a")]
RADISH = [rgb("6a0a26"), rgb("a0143a"), rgb("d02a4e"), rgb("ee5a74"), rgb("ff9aaa")]
RADISH_LEAF = [rgb("2a4e18"), rgb("3a6a20"), rgb("4e862c"), rgb("66a03a"), rgb("86ba54"), rgb("a8d070")]
PEA = [rgb("2e5a16"), rgb("44781e"), rgb("5e9a2c"), rgb("80b84a"), rgb("a8d470"), rgb("cce89a")]
CUCUMBER = [rgb("163a14"), rgb("235220"), rgb("336c2c"), rgb("4c8a3e"), rgb("78b060"), rgb("a8d08a")]
CUCUMBER_LEAF = [rgb("1e4418"), rgb("2a5a20"), rgb("3a7228"), rgb("4e8c34"), rgb("68a444"), rgb("8cbe5e")]
EGGPLANT = [rgb("1e0c26"), rgb("341444"), rgb("4e1e62"), rgb("6e2e84"), rgb("9a5ab0"), rgb("caa0dc")]
EGG_LEAF = [rgb("2a3e26"), rgb("3a5434"), rgb("4c6a44"), rgb("62845a"), rgb("7c9c72"), rgb("a0bc94")]
ZUCCHINI = [rgb("12301a"), rgb("1c4424"), rgb("285a30"), rgb("3c7442"), rgb("5e9460"), rgb("8ab88a")]
SQUASH_LEAF = [rgb("1e4016"), rgb("2a561e"), rgb("3a6e28"), rgb("4e8834"), rgb("68a246"), rgb("8ebc66")]
YELLOW = [rgb("c88a10"), rgb("eab01a"), rgb("fad040"), rgb("fff08a")]
WHITE = [rgb("d8d4c6"), rgb("f4f2ea"), rgb("fffef8")]
PURPLE_FLOWER = [rgb("5a2a7a"), rgb("8a52b0"), rgb("b48ad6")]
SEED_PALE = [rgb("b8a878"), rgb("d8caa0"), rgb("f2e8c8")]
SEED_DARK = [rgb("3a2a18"), rgb("5a4228"), rgb("806040")]
SOIL_SHADOW = rgb("3a2a18")


def leaf_blob(c, x, y, radius, palette, frill=False):
    """A round leaf seen from the side, lit at its top: lettuce and spinach leaves, a radish's."""
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            d = math.hypot(dx, dy * 1.2)
            if d > radius + 0.2:
                continue
            edge = d > radius - 0.8
            if frill and edge and (dx + dy) % 2 == 0:
                continue
            tone = 1 if edge and dy > 0 else 4 if dy < -radius / 3 else 3 if dy <= radius / 3 else 2
            c.px(x + dx, y + dy, palette[tone])


# ---------------------------------------------------------------- lettuce and spinach

def lettuce_stage(stage):
    """A loose rosette of pale, frilled leaves, cupping into a head with a light heart (stage 3)."""
    c = Canvas()
    if stage == 0:
        stalk(c, 7, 14, palette=STEM)
        leaf_blob(c, 6, 13, 1, LETTUCE)
        leaf_blob(c, 9, 13, 1, LETTUCE)
        return c.img
    if stage == 1:
        for x, y, r in ((5, 12, 2), (10, 12, 2), (7, 11, 2)):
            leaf_blob(c, x, y, r, LETTUCE, frill=True)
        return c.img
    if stage == 2:
        for x, y, r in ((3, 12, 2), (12, 12, 2), (5, 10, 3), (10, 10, 3), (8, 9, 2)):
            leaf_blob(c, x, y, r, LETTUCE, frill=True)
        return c.img
    # A full head: outer leaves spread low, the heart cupped and pale.
    for x, y, r in ((2, 13, 2), (13, 13, 2), (4, 11, 3), (11, 11, 3)):
        leaf_blob(c, x, y, r, LETTUCE, frill=True)
    for dy in range(-4, 3):
        for dx in range(-4, 5):
            if math.hypot(dx, dy * 1.15) <= 4.2:
                tone = 5 if dx + dy < -3 else 4 if dx + dy < 1 else 3 if dy < 2 else 2
                c.px(7.5 + dx, 10 + dy, LETTUCE[tone])
    for x, y in ((6, 8), (9, 9), (7, 11), (10, 7)):
        c.px(x, y, LETTUCE[2])
    return c.img


def spade_leaf(c, x, y, direction, length, palette):
    """A spinach leaf: a dark, glossy spade on its own stalk, held up and out."""
    for i in range(length):
        width = 1 if i in (0, length - 1) else 2 if i < length - 2 else 1
        cx = x + direction * i
        cy = y - i * 0.6
        for w in range(-width + 1, width):
            c.px(cx, cy + w, palette[4 if w < 0 else 3 if w == 0 else 2])
    c.px(x + direction * (length - 1), y - (length - 1) * 0.6, palette[5])


def spinach_stage(stage):
    """Dark, glossy spade-shaped leaves on short stalks, upright, filling out to a bunch."""
    c = Canvas()
    if stage == 0:
        stalk(c, 7, 13, palette=STEM)
        c.px(6, 12, SPINACH[4])
        c.px(9, 12, SPINACH[4])
        return c.img
    count = (0, 3, 5, 7)[stage]
    leaves = [(7, 13, -1, 4), (8, 13, 1, 4), (7, 11, -1, 5), (8, 11, 1, 5), (7, 9, -1, 4), (8, 9, 1, 4), (7, 8, 1, 3)]
    for x in (7, 8):
        c.line(x, 15, x, 15 - 2 - stage * 2, STEM[1 + (x == 8)])
    for x, y, d, length in leaves[:count]:
        spade_leaf(c, x, y - (3 - stage), d, length + (stage == 3), SPINACH)
    return c.img


# ---------------------------------------------------------------- radishes and peas

def radish_stage(stage):
    """A small rosette of rough leaves; the red root's shoulder shows at the soil once it swells (stages 2 and 3)."""
    c = Canvas()
    height = (3, 6, 8, 9)[stage]
    for i, (x, d) in enumerate(((6, -1), (9, 1), (7, -1), (8, 1), (5, -1), (10, 1))[: 2 + stage]):
        c.line(7.5, 14, x + d * (1 + stage // 2), 14 - height + i % 2, STEM[2])
        leaf_blob(c, x + d * (1 + stage // 2), 14 - height + i % 2, 1 + (stage >= 2), RADISH_LEAF)
    if stage >= 2:
        r = 1 if stage == 2 else 2
        for dy in range(-r, 1):
            for dx in range(-r - 1, r + 2):
                if math.hypot(dx * 0.8, dy) <= r + 0.4:
                    c.px(7.5 + dx, 15 + dy, RADISH[3 if dx < 0 else 2])
        c.px(6, 14, RADISH[4])
    return c.img


def pea_stage(stage):
    """A climbing pea: thin stems with paired round leaves and curling tendrils; white flowers (stage 2), then fat green pods
    hanging (stage 3)."""
    c = Canvas()
    height = (4, 9, 13, 14)[stage]
    for x0, sway in ((6, 1.0), (9, -1.0)):
        for y in range(15 - height, 16):
            c.px(x0 + math.sin(y * 0.6) * sway * 0.6, y, PEA[1])
    for i in range(1 + height // 3):
        y = 14 - i * 3
        for x, d in ((5, -1), (10, 1)):
            c.px(x + d, y, PEA[4])
            c.px(x + d * 2, y, PEA[3])
            c.px(x + d, y + 1, PEA[2])
        if i % 2 and stage >= 1:
            c.px(4, y - 1, PEA[5])
            c.px(3, y - 2, PEA[5])
    if stage == 2:
        for x, y in ((4, 5), (11, 7), (7, 4)):
            c.px(x, y, WHITE[2])
            c.px(x + 1, y, WHITE[1])
            c.px(x, y + 1, WHITE[0])
    if stage == 3:
        # Fat pods hanging: pale yellow-green, the peas bulging along them, a dark seam below.
        for x, y in ((3, 6), (11, 8), (7, 10)):
            for k in range(4):
                c.px(x + (k > 1), y + k, PEA[5] if k % 2 == 0 else PEA[4])
                c.px(x + 1 + (k > 1), y + k, PEA[3])
                c.px(x + 2 + (k > 1), y + k, PEA[0])
    return c.img


# ---------------------------------------------------------------- cucumber (climbing)

def hand_leaf(c, x, y, size, palette):
    """A broad lobed leaf seen from the side (cucumber, zucchini): lobes as three bumps, lit above."""
    for dy in range(-size, size + 1):
        for dx in range(-size - 1, size + 2):
            d = math.hypot(dx * 0.85, dy)
            lobe = dy < -size / 2 and abs(dx) in (size // 2 + 1,) and size > 1
            if d > size + 0.3 or lobe:
                continue
            tone = 4 if dy < -size / 3 else 3 if dy < size / 3 else 2
            if d > size - 0.6 and dy > 0:
                tone = 1
            c.px(x + dx, y + dy, palette[tone])
    c.px(x, y, palette[5])


def cucumber_fruit(c, x, y, length, ripe=True):
    """A cucumber hanging from the vine: long, dark, with pale stripes and bumps, lit down one side."""
    tones = CUCUMBER
    for k in range(length):
        c.px(x, y + k, tones[4] if ripe else tones[5])
        c.px(x + 1, y + k, tones[2] if k % 3 else tones[3])
        if ripe and k % 2:
            c.px(x, y + k, tones[5])
    c.px(x, y - 1, STEM[2])


def trellis_vine(c, top, bottom=15):
    for y in range(top, bottom + 1):
        x = 7.5 + math.sin(y * 0.7)
        c.px(x, y, STEM[1])
        c.px(x + 1, y, STEM[2])


def cucumber_texture(name):
    c = Canvas()
    if name.startswith("cucumber_stage"):
        stage = int(name[-1])
        if stage == 0:
            stalk(c, 7, 13, palette=STEM)
            broad_leaf(c, 7, 12, -1, 1, CUCUMBER_LEAF)
            broad_leaf(c, 8, 12, 1, 1, CUCUMBER_LEAF)
        else:
            trellis_vine(c, 15 - (6 if stage == 1 else 10))
            spots = ((5, 11, 2), (10, 9, 2), (6, 7, 1), (10, 13, 1))
            for x, y, size in spots[: 2 + stage]:
                hand_leaf(c, x, y, size, CUCUMBER_LEAF)
        return c.img
    stage = int(name[-1])
    top = "_top_" in name
    trellis_vine(c, 2 if top else 0, 15)
    spots = ((4, 4, 2), (11, 6, 2), (5, 10, 2), (11, 12, 2), (8, 2, 1)) if not top else ((5, 7, 2), (10, 10, 2), (4, 13, 1), (9, 4, 1))
    leaves = spots[: (3 if stage == 0 else len(spots))]
    for x, y, size in leaves:
        hand_leaf(c, x, y, size + (stage >= 2 and size == 1), CUCUMBER_LEAF)
    # Tendrils curling off the vine.
    for x, y in ((12, 3), (3, 8)) if top else ((13, 9),):
        c.px(x, y, STEM[3])
        c.px(x + 1, y - 1, STEM[3])
    if stage == 2:
        for x, y in ((3, 7), (12, 9), (7, 13)) if not top else ((12, 6), (3, 11)):
            c.px(x, y, YELLOW[2])
            c.px(x + 1, y, YELLOW[1])
            c.px(x, y - 1, YELLOW[3])
    if stage >= 3:
        fruits = ((3, 9, 4), (12, 7, 5)) if not top else ((12, 11, 4),)
        for x, y, length in fruits:
            cucumber_fruit(c, x, y, length - (stage == 3) * 2, ripe=stage == 4)
    return c.img


def wild_cucumbers():
    """A cucumber vine sprawled on the ground, leaves up, a ripe cucumber lying under them."""
    c = Canvas()
    c.line(1, 14, 14, 13, STEM[1])
    for x, y, size in ((3, 11, 2), (8, 10, 2), (12, 11, 2), (6, 8, 1)):
        hand_leaf(c, x, y, size, CUCUMBER_LEAF)
    for k in range(5):
        c.px(5 + k, 14, CUCUMBER[3] if k % 2 else CUCUMBER[4])
        c.px(5 + k, 15, CUCUMBER[2])
    c.px(10, 12, YELLOW[2])
    c.px(11, 12, YELLOW[1])
    return c.img


# ---------------------------------------------------------------- eggplant and zucchini (bushes)

def soft_leaf(c, x, y, direction, length, palette):
    """A long, soft oval leaf held out and drooping (eggplant)."""
    for i in range(length):
        t = i / max(1, length - 1)
        cy = y + t * t * 2
        width = 1 + (0.2 < t < 0.8)
        for w in range(-width + 1, width + 1):
            c.px(x + direction * i, cy + w, palette[4 if w < 0 else 3 if w == 0 else 2])
    c.px(x + direction * (length // 2), y + 0.5, palette[5])


def eggplant_fruit(c, x, y, size, ripe):
    tones = EGGPLANT if ripe else [rgb("8a6aa0"), rgb("a888ba"), rgb("c4a8d2"), rgb("dccae6"), rgb("eee0f4"), rgb("ffffff")]
    c.px(x, y - 1, rgb("4a6a30"))
    c.px(x + 1, y - 1, rgb("5e8238"))
    for k in range(size):
        half = 0 if k < size // 3 else 1
        for dx in range(-half, half + 2):
            tone = 4 if dx <= -half else 3 if dx == 0 else 2 if dx == 1 else 1
            c.px(x + dx, y + k, tones[tone])
    if ripe:
        c.px(x, y + 1, tones[5])


def eggplant_stage(stage):
    """A sturdy bush of soft grey-green leaves; purple star flowers (stage 3), small pale fruit (4), glossy deep purple
    eggplants hanging (5)."""
    c = Canvas()
    if stage == 0:
        stalk(c, 7, 13, palette=STEM)
        soft_leaf(c, 7, 12, -1, 3, EGG_LEAF)
        soft_leaf(c, 8, 12, 1, 3, EGG_LEAF)
        return c.img
    height = (0, 8, 11, 12, 12, 12)[stage]
    top = 15 - height
    for x in (5, 8, 11):
        c.line(7.5, 15, x, top + 3, STEM[1])
    leaves = [(5, top + 3, -1, 4), (10, top + 3, 1, 4), (6, top + 7, -1, 5), (9, top + 6, 1, 5), (7, top + 1, -1, 3)]
    for x, y, d, length in leaves[: 2 + stage]:
        soft_leaf(c, x, y, d, length, EGG_LEAF)
    if stage == 3:
        for x, y in ((4, top + 6), (11, top + 8), (8, top + 4)):
            c.px(x, y, PURPLE_FLOWER[2])
            c.px(x - 1, y, PURPLE_FLOWER[1])
            c.px(x + 1, y, PURPLE_FLOWER[1])
            c.px(x, y + 1, PURPLE_FLOWER[0])
            c.px(x, y - 1, YELLOW[2])
    if stage >= 4:
        for x, y in ((4, top + 7), (10, top + 6)):
            eggplant_fruit(c, x, y, 4 if stage == 4 else 6, stage == 5)
    return c.img


def zucchini_fruit(c, x, y, length, ripe, flower=False):
    """A zucchini lying under the leaves: glossy dark green speckled pale (a young one lighter), a shadow along its
    underside, its stem end at the left and, while young, the yellow flower still at its tip."""
    tones = ZUCCHINI if ripe else [ZUCCHINI[3], ZUCCHINI[4], ZUCCHINI[4], ZUCCHINI[5], ZUCCHINI[5], rgb("c8e0b0")]
    for k in range(length):
        c.px(x + k, y, tones[5] if k % 3 == 1 else tones[4])
        c.px(x + k, y + 1, tones[2])
        c.px(x + k, y + 2, ZUCCHINI[0])
    c.px(x - 1, y, rgb("8a7a3a"))
    if flower:
        c.px(x + length, y, YELLOW[3])
        c.px(x + length, y + 1, YELLOW[1])


def zucchini_stage(stage):
    """Big lobed leaves sprawling from a low crown; yellow trumpet flowers (stage 3), small zucchini (4), long dark green
    zucchini lying under the leaves (5)."""
    c = Canvas()
    if stage == 0:
        stalk(c, 7, 13, palette=STEM)
        hand_leaf(c, 6, 12, 1, SQUASH_LEAF)
        hand_leaf(c, 10, 12, 1, SQUASH_LEAF)
        return c.img
    spots = [(4, 11, 2), (11, 11, 2), (7, 8, 3), (2, 9, 2), (13, 8, 2)]
    for x, y, size in spots[: 1 + stage]:
        c.line(7.5, 15, x, y, STEM[2])
        hand_leaf(c, x, y + (3 - min(stage, 3)), size, SQUASH_LEAF)
    if stage == 3:
        for x, y in ((5, 13), (11, 13)):
            c.px(x, y, YELLOW[3])
            c.px(x + 1, y, YELLOW[2])
            c.px(x, y + 1, YELLOW[1])
            c.px(x + 1, y + 1, YELLOW[2])
    if stage >= 4:
        zucchini_fruit(c, 2, 12, 3 if stage == 4 else 6, stage == 5, flower=stage == 4)
        zucchini_fruit(c, 9, 13, 3 if stage == 4 else 5, stage == 5, flower=stage == 4)
    return c.img


PAINTERS = {"lettuce": lettuce_stage, "spinach": spinach_stage, "radish": radish_stage, "pea": pea_stage,
            "eggplant": eggplant_stage, "zucchini": zucchini_stage}


# ---------------------------------------------------------------- items

def lettuce_item():
    c = Canvas()
    for dy in range(-5, 5):
        for dx in range(-6, 7):
            d = math.hypot(dx, dy * 1.2)
            if d <= 6.2:
                tone = 5 if dx + dy < -5 else 4 if dx + dy < 0 else 3 if dx + dy < 5 else 2
                if d > 5.3 and (dx + dy) % 2:
                    tone = 1
                c.px(8 + dx, 9 + dy, LETTUCE[tone])
    for x, y in ((6, 7), (9, 9), (7, 11), (10, 6), (5, 10)):
        c.px(x, y, LETTUCE[2])
    outline(c, rgb("1e3a10"))
    return c.img


def spinach_item():
    c = Canvas()
    for x, y, d in ((5, 12, 1), (8, 13, 1), (6, 10, -1)):
        c.line(x, y, x + 2 * d, y - 2, STEM[2])
    for x, y, r in ((6, 6, 3), (10, 8, 3), (5, 10, 2)):
        leaf_blob(c, x, y, r, SPINACH)
        c.px(x, y, SPINACH[5])
    outline(c, rgb("0a1e0a"))
    return c.img


def radish_item():
    c = Canvas()
    for x, y in ((6, 4), (8, 2), (10, 4)):
        c.line(8, 7, x, y, STEM[2])
        leaf_blob(c, x, y, 1, RADISH_LEAF)
    round_fruit(c, 8, 10, 3, RADISH + [RADISH[4]])
    c.px(8, 14, RADISH[0])
    c.px(8, 15, rgb("e8d8c8"))
    outline(c, rgb("3a0414"))
    return c.img


def peas_item():
    c = Canvas()
    # An open pod with its peas in a row.
    for k in range(10):
        c.px(3 + k, 10 - k // 3, PEA[2])
        c.px(3 + k, 11 - k // 3, PEA[1])
    for k in range(4):
        x, y = 5 + 2 * k, 8 - (2 * k) // 3
        c.px(x, y, PEA[5])
        c.px(x + 1, y, PEA[4])
        c.px(x, y + 1, PEA[3])
        c.px(x + 1, y + 1, PEA[2])
    outline(c, rgb("1a3a0a"))
    return c.img


def cucumber_item():
    c = Canvas()
    for k in range(11):
        x, y = 3 + k, 12 - k
        for w in range(-1, 2):
            tone = 4 if w < 0 else 3 if w == 0 else 2
            if k in (0, 10) and w:
                continue
            c.px(x + w * 0.5, y + w * 0.5 + 0.5, CUCUMBER[tone])
        if k % 2:
            c.px(x, y, CUCUMBER[5])
    c.px(14, 1, STEM[2])
    outline(c, rgb("0a1e0a"))
    return c.img


def eggplant_item():
    c = Canvas()
    for dy in range(-3, 6):
        half = 2 if dy < 0 else 3
        for dx in range(-half, half + 1):
            if math.hypot(dx, dy - 1) <= half + 1.3:
                tone = 5 if dx + dy < -3 else 4 if dx + dy < 0 else 3 if dx + dy < 4 else 2
                c.px(8 + dx, 9 + dy, EGGPLANT[tone])
    for x, y in ((7, 4), (8, 4), (9, 4), (6, 5), (10, 5), (8, 3), (9, 2)):
        c.px(x, y, rgb("5e8238") if y > 3 else rgb("4a6a30"))
    outline(c, rgb("12061a"))
    return c.img


def zucchini_item():
    c = Canvas()
    for k in range(11):
        x, y = 2 + k, 13 - k
        for w in range(-1, 2):
            if k in (0, 10) and w:
                continue
            c.px(x + w * 0.5, y + w * 0.5 + 0.5, ZUCCHINI[4 if w < 0 else 3 if w == 0 else 2])
        if k % 3 == 1:
            c.px(x, y, ZUCCHINI[5])
    c.px(13, 2, rgb("8a7a3a"))
    c.px(14, 1, rgb("a8964a"))
    c.px(2, 13, YELLOW[2])
    outline(c, rgb("08180c"))
    return c.img


def cooked(img, tint=(140, 90, 40), strength=0.35, char=((5, 7), (9, 9), (7, 11))):
    """A roasted or grilled vegetable: warmed toward brown, with dark grill marks."""
    out = img.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = out.getpixel((x, y))
            if a and (r, g, b) != (0, 0, 0):
                out.putpixel((x, y), (round(r + (tint[0] - r) * strength), round(g + (tint[1] - g) * strength),
                                      round(b + (tint[2] - b) * strength), a))
    for x, y in char:
        if out.getpixel((x, y))[3]:
            out.putpixel((x, y), (46, 30, 18, 255))
            if out.getpixel((x + 1, y - 1))[3]:
                out.putpixel((x + 1, y - 1), (46, 30, 18, 255))
    return out


def vegetable_textures():
    out = {}
    for crop, painter in PAINTERS.items():
        stages = 6 if crop in vegetables.TALL_CROPS else 4
        for stage in range(stages):
            out[("block", f"{crop}_stage{stage}")] = painter(stage)
    for names in vegetables.TALL_CROPS["cucumber"]["textures"]:
        for name in names:
            out[("block", name)] = cucumber_texture(name)
    out[("block", "wild_cucumbers")] = wild_cucumbers()
    out.update({
        ("item", "lettuce"): lettuce_item(),
        ("item", "lettuce_seeds"): seeds_item(SEED_PALE, [(4, 5), (9, 4), (6, 9), (11, 8), (5, 12), (9, 12)]),
        ("item", "spinach"): spinach_item(),
        ("item", "spinach_seeds"): seeds_item(SEED_DARK, [(5, 4), (10, 5), (4, 9), (8, 8), (11, 11), (6, 12)]),
        ("item", "radish"): radish_item(),
        ("item", "peas"): peas_item(),
        ("item", "cucumber"): cucumber_item(),
        ("item", "cucumber_seeds"): seeds_item(SEED_PALE, [(4, 4), (8, 5), (11, 3), (5, 9), (10, 9), (7, 12)], size=(2, 1)),
        ("item", "eggplant"): eggplant_item(),
        ("item", "eggplant_seeds"): seeds_item([rgb("c8b080"), rgb("e0cca0"), rgb("f4e4c0")],
                                               [(5, 5), (9, 4), (4, 9), (8, 9), (11, 8), (7, 12)], size=(1, 1)),
        ("item", "zucchini"): zucchini_item(),
        ("item", "zucchini_seeds"): seeds_item([rgb("d8c898"), rgb("ece0b8"), rgb("fbf4dc")],
                                               [(4, 4), (9, 3), (6, 8), (11, 8), (4, 12), (9, 12)], size=(2, 1)),
        ("item", "roasted_eggplant"): cooked(eggplant_item()),
        ("item", "grilled_zucchini"): cooked(zucchini_item(), char=((5, 10), (8, 7), (11, 4))),
        ("item", "green_salad"): bowl_item([LETTUCE[2], LETTUCE[3], LETTUCE[4]],
                                           bits=[(CUCUMBER[4], [(5, 5), (10, 6)]), (RADISH[2], [(8, 5), (4, 6)]),
                                                 (SPINACH[3], [(7, 4), (11, 5)])]),
        ("item", "pea_soup"): bowl_item([rgb("6a9a2a"), rgb("86b23a"), rgb("a2c85a")],
                                        bits=[(PEA[4], [(5, 6), (9, 6), (7, 7)]), (rgb("e88a2a"), [(11, 6)])]),
    })
    return out
