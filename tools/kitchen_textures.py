"""Original 16x16 textures for the Agriculture branch's Kitchen Garden (requires Pillow).

Called from crop_textures.crop_textures(). Like the Fall Harvest art, every pixel is drawn here by
code from fixed seeds; no Mojang texture is read, traced or recolored. Crop textures are drawn for
the "crop" model (four upright planes, row 15 at the soil); climbing tomatoes are drawn the same
way and shown inside the trellis model.
"""
import math
import random

from crop_textures import (Canvas, rgb, broad_leaf, stalk, outline, seeds_item, LEAF, DRY, WOOD,
                           BEAN, KERNEL)
import block_style as bs

TOMATO_LEAF = [rgb("1d3d17"), rgb("27511d"), rgb("336a25"), rgb("43822e"), rgb("5a9b3c"), rgb("7db55a")]
BLOSSOM = [rgb("c89a16"), rgb("f0cc34"), rgb("ffec8e")]
VINE_STEM = [rgb("2f5a1d"), rgb("447a28"), rgb("5e9936"), rgb("7fb24e")]
PEPPER = [rgb("640c0a"), rgb("951610"), rgb("c4261a"), rgb("e84c34"), rgb("f78a70")]
PEPPER_GREEN = [rgb("2a5214"), rgb("3e721d"), rgb("58922a"), rgb("80b848")]
ONION = [rgb("6c3c14"), rgb("9a5e24"), rgb("c68a3c"), rgb("e4b468"), rgb("f4d9a0")]
GARLIC = [rgb("7e6f82"), rgb("ab9fb1"), rgb("d6cfd9"), rgb("f1edf2"), rgb("ffffff")]
CABBAGE = [rgb("3b6a2e"), rgb("578a44"), rgb("78a95e"), rgb("9cc47e"), rgb("c3dea5"), rgb("e1f0cc")]
OAT = [rgb("7d6a33"), rgb("a38d48"), rgb("c6b066"), rgb("e1cf92"), rgb("f2e7c0")]
BARLEY = [rgb("8a6522"), rgb("b38832"), rgb("d6ad4c"), rgb("ecc976"), rgb("f8e3a6")]
GRAIN_GREEN = [rgb("3f6326"), rgb("567f31"), rgb("719c42"), rgb("93b95e")]
IRON = [rgb("222226"), rgb("333339"), rgb("4a4a52"), rgb("64646e"), rgb("848490"), rgb("a8a8b4")]
SOUP_TOMATO = [rgb("7a1c10"), rgb("a82e18"), rgb("cc4a22"), rgb("e26a36")]
BREAD = [rgb("6a3c16"), rgb("8f5622"), rgb("b27634"), rgb("d09a52"), rgb("e7c07e")]
BOWL = WOOD


# ---------------------------------------------------------------- tomato (climbing)

def vine(c, top, bottom=15, sway=1.0):
    """The main climbing stem, winding gently up the middle."""
    for y in range(top, bottom + 1):
        x = 7.5 + math.sin(y * 0.7) * sway
        c.px(x, y, VINE_STEM[1])
        c.px(x + 1, y, VINE_STEM[2])


# ---------------------------------------------------------------- pepper (bush)

def pepper_bush(c, height, flowers=0, fruit=None):
    base = 15
    for x in (5, 8, 11):
        c.line(7.5, base, x, base - height + 3, VINE_STEM[1])
    leaves = [(4, base - height + 4, -1, 2), (11, base - height + 4, 1, 2), (7, base - height + 2, -1, 2),
              (9, base - height + 7, 1, 2), (5, base - height + 8, -1, 2), (10, base - height + 10, 1, 1)]
    for x, y, d, size in leaves[: 2 + height // 3]:
        broad_leaf(c, x, y, d, size, TOMATO_LEAF)
    for i in range(flowers):
        fx, fy = ((5, base - height + 6), (10, base - height + 5), (8, base - height + 9))[i]
        c.px(fx, fy, rgb("f4f2ea"))
        c.px(fx + 1, fy, rgb("d8d4c6"))
    if fruit:
        palette = PEPPER if fruit == "ripe" else PEPPER_GREEN
        for fx, fy in ((4, base - 7), (8, base - 5), (11, base - 8), (6, base - 3)):
            # A pointed chili hanging down from its stem.
            c.px(fx, fy - 1, PEPPER_GREEN[1])
            for k in range(4):
                c.px(fx + (k > 2), fy + k, palette[3 - k // 2])
                if k < 3:
                    c.px(fx + 1, fy + k, palette[1])


def pepper_stage(stage):
    c = Canvas()
    if stage == 0:
        stalk(c, 7, 12, palette=VINE_STEM)
        broad_leaf(c, 7, 11, -1, 1, TOMATO_LEAF)
        broad_leaf(c, 8, 11, 1, 1, TOMATO_LEAF)
    else:
        height = (0, 8, 11, 12, 12, 12)[stage]
        pepper_bush(c, height, flowers=3 if stage == 3 else 0,
                    fruit={4: "green", 5: "ripe"}.get(stage))
    return c.img


# ---------------------------------------------------------------- onion and garlic

def tube_leaves(c, count, height, palette, bend=1.5, seed=0):
    """Hollow, upright leaves fanning out from one point (onions, garlic, chives)."""
    rng = random.Random(seed)
    for i in range(count):
        lean = (i - (count - 1) / 2) * bend
        h = height - rng.choice([0, 1, 1, 2])
        for k in range(h * 2):
            t = k / (h * 2)
            x = 7.5 + lean * t * t
            y = 14 - t * h
            c.px(x, y, palette[1 + (k % 2)] if t < 0.85 else palette[3])


def bulb(c, cx, cy, width, height, palette):
    for dy in range(-height, 1):
        half = width * math.sqrt(max(0.0, 1 - (dy / (height + 0.5)) ** 2))
        for dx in range(-int(half), int(half) + 1):
            shade = 3 if dx < 0 else 2
            c.px(cx + dx, cy + dy, palette[shade])
    c.px(cx, cy - height - 1, palette[1])


def garlic_stage(stage):
    c = Canvas()
    tube_leaves(c, (3, 5, 6, 6)[stage], (5, 9, 12, 12)[stage], LEAF, bend=2.4, seed=20 + stage)
    if stage == 3:
        # A curling flower stalk (scape) and the white bulb shoulder at the soil.
        for i in range(10):
            a = i / 9 * math.pi * 1.3
            c.px(7.5 + math.sin(a) * 2.5, 4 - i * 0.2 + math.cos(a) * 1.5, LEAF[4])
        c.px(10, 3, GARLIC[3])
        bulb(c, 7.5, 15, 2, 1, GARLIC)
    return c.img


# ---------------------------------------------------------------- cabbage

# ---------------------------------------------------------------- oats and barley

def grain_stand(c, stage, palette, awns, seed):
    """A stand of grass-like grain stems. Oats hang in loose nodding spikelets; barley carries a
    dense head with long whisker-like awns."""
    rng = random.Random(seed + stage)
    stems = [1.5, 3.5, 5.5, 7.5, 9.5, 11.5, 13.5] if stage else [2.5, 5, 7.5, 10, 12.5]
    height = (4, 8, 12, 12)[stage]
    colors = DRY if stage == 3 else GRAIN_GREEN
    for i, x in enumerate(stems):
        h = height - rng.choice([0, 1, 2])
        top = 15 - h
        lean = rng.choice([-1, -0.5, 0.5, 1])
        for y in range(top, 16):
            t = (15 - y) / max(1, h)
            c.px(x + lean * t * t, y, colors[1 + (y % 2)])
        tx = x + lean
        if stage == 1:
            c.px(tx + 1, top + 3, colors[3])
        if stage >= 2:
            head = palette if stage == 3 else GRAIN_GREEN
            if awns:
                for k in range(4):
                    c.px(tx, top + k, head[2 + (k % 2)])
                    c.px(tx + 1, top + k, head[1])
                for k in range(1, 4):
                    c.px(tx - 1 - k * 0.3, top - k, head[3])
                    c.px(tx + 1 + k * 0.3, top - k, head[3])
            else:
                for k, (dx, dy) in enumerate(((-1, 1), (1, 2), (-1, 3), (1, 4), (0, 0))):
                    c.px(tx + dx, top + dy, head[2 + (k % 2)])
                    c.px(tx + dx, top + dy + 1, head[1])


def oat_stage(stage):
    c = Canvas()
    grain_stand(c, stage, OAT, awns=False, seed=60)
    return c.img


def barley_stage(stage):
    c = Canvas()
    grain_stand(c, stage, BARLEY, awns=True, seed=70)
    return c.img


# ---------------------------------------------------------------- equipment

def trellis():
    """A lattice of thin wooden laths crossing diagonally, with wide gaps to see the plant through."""
    c = Canvas()
    for i in range(-16, 32, 8):
        for t in range(16):
            c.px(i + t, t, WOOD[3])
            c.px(i + 15 - t, t, WOOD[2])
    for x in range(16):
        c.px(x, 15, WOOD[1])
    return c.img


def trellis_post():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, WOOD[1 + ((x * 7 + y // 3) % 3 == 0)])
    return c.img


def pot_soup():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, SOUP_TOMATO[1:3], 77, [3, 1], spread=0.6)
    for x, y in ((4, 5), (10, 4), (7, 9), (12, 11), (3, 12), (9, 13)):
        c.px(x, y, KERNEL[3])
        c.px(x + 1, y, rgb("7aa34a"))
    for x, y in ((2, 2), (13, 7), (6, 14)):
        c.px(x, y, SOUP_TOMATO[3])  # a glint on the surface
    return c.img


# ---------------------------------------------------------------- items

def round_fruit(c, cx, cy, radius, palette, squash=1.0):
    for dy in range(-radius - 1, radius + 2):
        for dx in range(-radius - 1, radius + 2):
            d = math.hypot(dx, dy / squash)
            if d <= radius + 0.3:
                light = (dx + dy) / (radius * 2.0)
                shade = 3 if light < -0.35 else 2 if light < 0.2 else 1
                c.px(cx + dx, cy + dy, palette[shade])
    c.px(cx - radius // 2 - 1, cy - radius // 2, palette[4] if len(palette) > 4 else palette[3])


def pepper_item():
    c = Canvas()
    # A curved chili from upper left to lower right, stem at the top.
    for i in range(11):
        t = i / 10
        cx, cy = 4 + i * 0.85, 4 + i * 0.95 + math.sin(t * math.pi) * 1.2
        r = 2.1 * (1 - t) + 0.4
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if math.hypot(dx, dy) <= r:
                    c.px(cx + dx, cy + dy, PEPPER[3 if dx + dy < 0 else 2])
    c.px(5, 4, PEPPER[4])
    for x, y in ((3, 3), (3, 2), (2, 1), (4, 2)):
        c.px(x, y, PEPPER_GREEN[2])
    outline(c, rgb("360605"))
    return c.img


def garlic_item():
    c = Canvas()
    round_fruit(c, 8, 10, 4, GARLIC)
    for x in (6, 8, 10):
        for y in range(7, 14):
            c.px(x, y, GARLIC[1])
    for y in range(2, 6):
        c.px(8, y, GARLIC[2])
    c.px(7, 11, rgb("b48bb6"))
    c.px(9, 12, rgb("b48bb6"))
    outline(c, rgb("4a3f4d"))
    return c.img


def grain_sprig(palette, awns):
    c = Canvas()
    for i in range(14):
        c.px(3 + i * 0.6, 15 - i, palette[1] if i < 7 else palette[2])
    for k in range(6):
        x, y = 7 + k * 0.6, 8 - k * 1.3
        if awns:
            c.px(x - 1, y, palette[3])
            c.px(x + 1, y, palette[2])
            c.px(x - 2 - k * 0.3, y - 2, palette[4])
            c.px(x + 2 + k * 0.3, y - 2, palette[4])
        else:
            c.px(x - 2, y + 1, palette[3])
            c.px(x - 2, y + 2, palette[2])
            c.px(x + 2, y + 1, palette[3])
            c.px(x + 2, y + 2, palette[2])
    outline(c, palette[0])
    return c.img


def bowl_item(broth, bits=(), crust=None):
    """A wooden bowl like vanilla's stews, filled with `broth` and dotted with `bits`: [(color, [(x, y)...])]."""
    c = Canvas()
    for y in range(8, 14):
        half = 6 - max(0, y - 10)
        for x in range(8 - half, 8 + half):
            c.px(x, y, BOWL[2] if x < 8 else BOWL[1])
    for x in range(2, 14):
        c.px(x, 8, BOWL[3])
    for x in range(3, 13):
        c.px(x, 7, broth[1 + (x % 2)])
        c.px(x, 6, broth[0 + (x % 3 == 0)] if 4 <= x <= 11 else broth[1])
    for color, spots in bits:
        for x, y in spots:
            c.px(x, y, color)
    if crust:
        for x in range(5, 11):
            c.px(x, 5, crust[3])
            c.px(x, 6, crust[2])
    outline(c, rgb("2e1d0d"))
    return c.img


def bread_item():
    """A round, dark barley loaf with a scored cross on top."""
    c = Canvas()
    for y in range(3, 14):
        for x in range(2, 15):
            d = (x - 8) ** 2 / 36 + (y - 9) ** 2 / 25
            if d <= 1:
                c.px(x, y, BREAD[3] if y < 7 else BREAD[2] if y < 11 else BREAD[1])
    for i in range(-2, 3):
        c.px(8 + i, 6 + i * 0.5, BREAD[0])
        c.px(8 + i, 6 - i * 0.5, BREAD[0])
    c.px(5, 5, BREAD[4])
    c.px(6, 4, BREAD[4])
    outline(c, rgb("2e1a08"))
    return c.img


def sauerkraut_item():
    c = Canvas()
    # A glass jar of pale shredded cabbage with a wooden lid.
    for y in range(4, 15):
        for x in range(4, 12):
            c.px(x, y, rgb("bccd96") if (x + y) % 4 == 0 else rgb("d9e4c0"))  # shreds in even slanting lines
    for y in range(4, 15):
        c.px(4, y, rgb("eef4f6"))
    for x in range(4, 12):
        c.px(x, 2, WOOD[2])
        c.px(x, 3, WOOD[1])
    outline(c, rgb("3a4a38"))
    return c.img


def cabbage_rolls_item():
    c = Canvas()
    for cx, cy in ((5.5, 10), (10.5, 8)):
        for dy in range(-2, 3):
            for dx in range(-3, 4):
                if dx * dx / 10 + dy * dy / 5 <= 1:
                    c.px(cx + dx, cy + dy, CABBAGE[3] if dy < 0 else CABBAGE[2])
        c.px(cx - 3, cy, rgb("8a4a2a"))
        c.px(cx + 3, cy, rgb("8a4a2a"))
        c.line(cx - 2, cy - 1, cx + 2, cy + 1, CABBAGE[5])
    outline(c, rgb("1f3a18"))
    return c.img


def kitchen_textures():
    """(kind, name) -> image for every Kitchen Garden texture."""
    out = {
        ("block", "trellis"): trellis(),
        ("block", "trellis_post"): trellis_post(),
        # The pot itself is the owner's (tools/menu.py COOKING_POT_TEXTURES); the soup that shows in it while it cooks is ours.
        ("block", "cooking_pot_soup"): pot_soup(),
        ("item", "pepper"): pepper_item(),
        ("item", "pepper_seeds"): seeds_item([rgb("d8c08a"), rgb("ecd9a8"), rgb("faf0d2")],
                                             [(3, 4), (8, 3), (11, 7), (5, 9), (9, 11), (4, 13)]),
        ("item", "garlic"): garlic_item(),
        ("item", "oats"): grain_sprig(OAT, awns=False),
        ("item", "oat_seeds"): seeds_item(OAT[1:], [(4, 5), (9, 4), (6, 9), (11, 9), (3, 11), (8, 12)], size=(2, 1)),
        ("item", "barley"): grain_sprig(BARLEY, awns=True),
        ("item", "barley_seeds"): seeds_item(BARLEY[1:], [(3, 4), (8, 5), (11, 8), (5, 9), (9, 12), (4, 12)], size=(2, 1)),
        ("item", "barley_bread"): bread_item(),
        ("item", "sauerkraut"): sauerkraut_item(),
        ("item", "cabbage_rolls"): cabbage_rolls_item(),
        ("item", "tomato_soup"): bowl_item(SOUP_TOMATO, [(rgb("f4efe6"), [(6, 6), (9, 7)])]),
        ("item", "onion_soup"): bowl_item([rgb("6a3a18"), rgb("8a5224"), rgb("a86c34"), rgb("c48a4c")],
                                          [(ONION[3], [(4, 7), (10, 7)])], crust=BREAD),
        ("item", "vegetable_soup"): bowl_item([rgb("7a6a2a"), rgb("9a8a3a"), rgb("b8a650"), rgb("d0c070")],
                                              [(rgb("e07a2a"), [(4, 6), (9, 7)]), (CABBAGE[3], [(6, 7), (11, 6)]),
                                               (rgb("e8d8a8"), [(8, 6), (5, 7)])]),
        ("item", "mushroom_barley_soup"): bowl_item([rgb("8a6a4a"), rgb("a8886a"), rgb("c4a888"), rgb("dcc6a8")],
                                                    [(rgb("5a3a24"), [(5, 6), (10, 7)]), (BARLEY[3], [(7, 7), (8, 6), (11, 6)])]),
        ("item", "oat_porridge"): bowl_item([rgb("c8b890"), rgb("dccca4"), rgb("ece0c0"), rgb("f6eedc")],
                                            [(OAT[1], [(5, 6), (8, 7), (10, 6)])]),
        ("item", "chili"): bowl_item([rgb("5a1a0e"), rgb("7e2614"), rgb("9e3a1c"), rgb("be5028")],
                                     [(BEAN[1], [(5, 6), (9, 7), (11, 6)]), (PEPPER[3], [(7, 6)]), (rgb("f4efe6"), [(8, 5)])]),
    }
    for stage in range(6):
        out[("block", f"pepper_stage{stage}")] = pepper_stage(stage)
    for stage in range(4):
        out[("block", f"garlic_stage{stage}")] = garlic_stage(stage)
        out[("block", f"oat_stage{stage}")] = oat_stage(stage)
        out[("block", f"barley_stage{stage}")] = barley_stage(stage)
    return out
