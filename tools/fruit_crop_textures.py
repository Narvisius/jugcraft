"""Original 16x16 textures for the fruit crops (tools/fruit_crops.py) (requires Pillow): the strawberry plant, the blueberry
bush and the coffee plant in six stages each (sprouting, leafing out, flowering, fruit green, then ripe), and their fruit,
seeds and the roasted coffee beans. The owner chose Jugcraft's own art for these (8 October 2026). The plum and banana trees
are drawn with the orchards' (tools/orchard_textures.py); the jams by the pantry's painter.

Crop textures are drawn for the "crop" model (four upright planes, row 15 at the soil), as the pepper's are
(tools/kitchen_textures.py). Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no Mojang
texture is read, traced or recoloured.
"""
from crop_textures import Canvas, rgb, broad_leaf, stalk, outline
import fruit_crops

LEAF = [rgb("1d3d17"), rgb("27511d"), rgb("336a25"), rgb("43822e"), rgb("5a9b3c"), rgb("7db55a")]
BLUE_LEAF = [rgb("1a3622"), rgb("244a2c"), rgb("2f5e36"), rgb("3c7442"), rgb("518a52"), rgb("6aa268")]
GLOSSY = [rgb("0e2a14"), rgb("163a1a"), rgb("1f4c22"), rgb("2a602c"), rgb("3a7838"), rgb("5a9a50")]
STEM = [rgb("2f5a1d"), rgb("447a28"), rgb("5e9936"), rgb("7fb24e")]
WOODY = [rgb("4a3020"), rgb("6a4630"), rgb("8a6040"), rgb("a87a54")]
WHITE = [rgb("d8d4c6"), rgb("f4f2ea"), rgb("fffef8")]

STRAWBERRY = [rgb("6a0c12"), rgb("a8141c"), rgb("d82a2a"), rgb("f25a4a"), rgb("ffa08a")]
STRAWBERRY_GREEN = [rgb("9aa848"), rgb("c8d070"), rgb("eef0b0")]
BLUEBERRY = [rgb("1a1a46"), rgb("2a2e6e"), rgb("3e4a94"), rgb("6a7cbc"), rgb("a4b4dc")]
BERRY_GREEN = [rgb("5a7a2a"), rgb("7e9a3c"), rgb("a8bc5a")]
CHERRY = [rgb("5a0a10"), rgb("8e1418"), rgb("c42a22"), rgb("e85a3c"), rgb("ff9a7a")]
CHERRY_GREEN = [rgb("3e6a1c"), rgb("5a8a26"), rgb("80aa3a")]
BEAN = [rgb("2a160c"), rgb("4a2a16"), rgb("6e4024"), rgb("925a32"), rgb("b47a48")]
GREEN_BEAN = [rgb("6a7a3a"), rgb("94a258"), rgb("bcc684"), rgb("dfe2b2")]


def berry(c, x, y, tones, size=1):
    """A round berry: lit at its upper left, shaded below; `size` 1 is two pixels across, 2 three."""
    if size == 1:
        c.px(x, y, tones[3])
        c.px(x + 1, y, tones[2])
        c.px(x, y + 1, tones[2])
        c.px(x + 1, y + 1, tones[1])
        return
    for dy in range(3):
        for dx in range(3):
            if (dx, dy) in ((0, 0), (2, 0), (0, 2), (2, 2)):
                continue
            c.px(x + dx, y + dy, tones[3] if dx + dy < 2 else tones[2] if dx + dy < 3 else tones[1])


# ---------------------------------------------------------------- the strawberry plant

def trefoil(c, x, y, palette):
    """A strawberry leaf seen from the side: three toothed leaflets on a short stalk."""
    for dx, dy, tone in ((-1, 0, 4), (0, -1, 5), (1, 0, 3), (-2, 1, 3), (0, 0, 4), (2, 1, 2), (-1, 1, 2), (1, 1, 2), (0, 1, 3)):
        c.px(x + dx, y + dy, palette[tone])


def strawberry_stage(stage):
    """A low rosette of three-part leaves; it flowers white (stage 3), sets small pale berries (4) and hangs red ones over
    the edge of its leaves (5)."""
    c = Canvas()
    if stage == 0:
        stalk(c, 7, 13, palette=STEM)
        trefoil(c, 7, 12, LEAF)
        return c.img
    leaves = [(7, 9), (4, 11), (10, 11), (5, 8), (10, 8), (7, 12), (3, 13), (12, 13)]
    count = (0, 4, 6, 8, 8, 8)[stage]
    for x in (4, 7, 10, 12):
        c.line(7.5, 15, x, 15 - (3 + stage // 2), STEM[1])
    for x, y in leaves[:count]:
        trefoil(c, x, y + (2 if stage == 1 else 0), LEAF)
    if stage == 3:
        for x, y in ((5, 6), (9, 6), (12, 9)):
            c.px(x, y, WHITE[2])
            c.px(x - 1, y, WHITE[1])
            c.px(x + 1, y, WHITE[1])
            c.px(x, y - 1, WHITE[1])
            c.px(x, y + 1, WHITE[0])
            c.px(x, y, rgb("f0cc34"))
    if stage >= 4:
        tones = STRAWBERRY if stage == 5 else STRAWBERRY_GREEN
        for x, y in ((3, 12), (8, 13), (12, 11), (5, 14)):
            # A berry hanging under its calyx: wider above, pointed below, seeded.
            c.px(x, y - 1, STEM[2])
            c.px(x + 1, y - 1, STEM[1])
            c.px(x, y, tones[-2] if stage == 5 else tones[2])
            c.px(x + 1, y, tones[2] if stage == 5 else tones[1])
            if stage == 5:
                c.px(x, y + 1, tones[2])
                c.px(x + 1, y + 1, tones[1])
                c.px(x, y + 2, tones[1])
            else:
                c.px(x, y + 1, tones[1])
    return c.img


# ---------------------------------------------------------------- the blueberry bush

def blueberry_stage(stage):
    """A woody little bush of small oval leaves; it hangs white bells (stage 3), then green berries (4), then dusky blue
    clusters (5)."""
    c = Canvas()
    if stage == 0:
        stalk(c, 7, 12, palette=WOODY)
        for x, y in ((6, 11), (9, 11), (7, 10)):
            c.px(x, y, BLUE_LEAF[4])
        return c.img
    height = (0, 8, 11, 13, 13, 13)[stage]
    top = 15 - height
    for x in (4, 7, 9, 12):
        c.line(7.5, 15, x, top + 3, WOODY[1] if x < 8 else WOODY[2])
    spots = [(4, top + 3), (7, top + 1), (10, top + 2), (12, top + 4), (3, top + 6), (6, top + 5), (9, top + 6),
             (12, top + 8), (5, top + 9), (10, top + 10), (3, top + 11), (7, top + 8)]
    for x, y in spots[: 4 + 2 * stage]:
        for dx, dy, tone in ((0, 0, 4), (1, 0, 3), (0, 1, 2), (-1, 0, 3), (1, 1, 1)):
            c.px(x + dx, y + dy, BLUE_LEAF[tone])
    clusters = [(4, top + 6), (9, top + 4), (11, top + 9), (6, top + 10), (3, top + 9)]
    if stage == 3:
        for x, y in clusters:
            c.px(x, y, WHITE[1])
            c.px(x, y + 1, WHITE[0])
            c.px(x + 1, y, rgb("f0d4dc"))
    elif stage >= 4:
        tones = BLUEBERRY if stage == 5 else [BERRY_GREEN[0]] + BERRY_GREEN + [BERRY_GREEN[2]]
        for x, y in clusters:
            berry(c, x, y, tones)
            c.px(x + 2, y + 1, tones[2])
    return c.img


# ---------------------------------------------------------------- the coffee plant

def coffee_leaf(c, x, y, direction, length):
    """A glossy coffee leaf: a long, pointed oval held out level from its stem, lit along its upper edge, its tip dipping."""
    for i in range(length):
        px = x + direction * i
        dip = 1 if i >= length - 1 else 0
        c.px(px, y - 1 + dip, GLOSSY[5] if i < length - 1 else GLOSSY[4])
        c.px(px, y + dip, GLOSSY[3] if i % 2 else GLOSSY[4])
        if 0 < i < length - 1:
            c.px(px, y + 1, GLOSSY[2])


def coffee_stage(stage):
    """An upright shrub: a few slim stems, each with pairs of glossy, dark, pointed leaves held out level, so the plant
    stands round-topped and open; it flowers in white clusters at the leaf joints (stage 3), sets green cherries there (4)
    and ripens them red (5)."""
    c = Canvas()
    if stage == 0:
        stalk(c, 7, 12, palette=STEM)
        coffee_leaf(c, 6, 11, -1, 3)
        coffee_leaf(c, 9, 11, 1, 3)
        return c.img
    height = (0, 9, 12, 14, 14, 14)[stage]
    top = 15 - height
    stems = [(7, top), (4, top + 3), (11, top + 2)] if stage > 1 else [(7, top)]
    for x, t in stems:
        c.line(7.5, 15, x, t, WOODY[1] if x < 7 else WOODY[2])
    for n, (x, t) in enumerate(stems):
        for i, y in enumerate(range(t + 1, 14, 3)):
            length = 3 if i == 0 else 4
            coffee_leaf(c, x - 1, y, -1, length)
            coffee_leaf(c, x + 1, y, 1, length)
            if i == 0:
                continue
            if stage == 3:
                for dx in (-1, 1):
                    c.px(x + dx, y + 1, WHITE[2])
                    c.px(x + dx, y + 2, WHITE[0])
            elif stage >= 4:
                tones = CHERRY if stage == 5 else [CHERRY_GREEN[0]] + CHERRY_GREEN + [CHERRY_GREEN[2]]
                c.px(x - 1, y + 1, tones[3])
                c.px(x + 1, y + 1, tones[2])
                c.px(x, y + 2, tones[1])
    return c.img


PAINTERS = {"strawberry": strawberry_stage, "blueberry": blueberry_stage, "coffee": coffee_stage}


# ---------------------------------------------------------------- items

def strawberry_item():
    """A strawberry: a fat red heart, seeded, under a star of green sepals."""
    c = Canvas()
    for y in range(4, 15):
        half = (4.6 if y < 8 else 4.6 - (y - 8) * 0.72)
        for x in range(16):
            if abs(x - 7.5) <= half:
                lit = (x - 7.5) + (y - 7)
                c.px(x, y, STRAWBERRY[3] if lit < -3 else STRAWBERRY[2] if lit < 2 else STRAWBERRY[1])
    for x, y in ((5, 7), (9, 6), (7, 9), (11, 9), (5, 11), (9, 11), (7, 13)):
        c.px(x, y, rgb("f0d060"))
    for x, y, tone in ((5, 3, 4), (6, 4, 3), (7, 3, 5), (8, 2, 4), (9, 3, 4), (10, 4, 3), (8, 4, 2), (7, 4, 3)):
        c.px(x, y, LEAF[tone])
    c.px(8, 1, STEM[1])
    outline(c, rgb("2a0a0a"))
    return c.img


def blueberries_item():
    """A handful of blueberries: four dusky berries, each with its little five-pointed crown."""
    c = Canvas()
    for x, y in ((3, 6), (8, 4), (9, 9), (4, 10)):
        for dy in range(4):
            for dx in range(4):
                if (dx, dy) in ((0, 0), (3, 0), (0, 3), (3, 3)):
                    continue
                lit = dx + dy
                c.px(x + dx, y + dy, BLUEBERRY[4] if lit == 1 else BLUEBERRY[3] if lit < 3 else BLUEBERRY[2] if lit < 5 else BLUEBERRY[1])
        c.px(x + 1, y + 1, BLUEBERRY[0])
        c.px(x + 2, y + 1, BLUEBERRY[1])
    outline(c, rgb("10102a"))
    return c.img


def coffee_cherries_item():
    """A sprig of ripe coffee cherries: three round red fruits on a short green twig with a leaf."""
    c = Canvas()
    c.line(4, 3, 8, 7, CHERRY_GREEN[0])
    for x, y, tone in ((3, 2, 2), (2, 3, 1), (4, 2, 1), (3, 1, 2)):
        c.px(x, y, GLOSSY[tone + 2])
    for x, y in ((5, 8), (9, 6), (8, 10)):
        for dy in range(4):
            for dx in range(4):
                if (dx, dy) in ((0, 0), (3, 0), (0, 3), (3, 3)):
                    continue
                lit = dx + dy
                c.px(x + dx, y + dy, CHERRY[4] if lit == 1 else CHERRY[3] if lit < 3 else CHERRY[2] if lit < 5 else CHERRY[1])
        c.px(x + 2, y + 3, CHERRY[0])
    outline(c, rgb("2a0808"))
    return c.img


def bean(c, x, y, tones):
    """A coffee bean seen from its flat side: an oval with the crease down the middle."""
    for dy in range(5):
        for dx in range(4):
            if (dx, dy) in ((0, 0), (3, 0), (0, 4), (3, 4)):
                continue
            c.px(x + dx, y + dy, tones[3] if dx == 0 or dy == 0 else tones[2] if dx < 3 else tones[1])
    for dy in range(1, 4):
        c.px(x + 1 + (dy == 2), y + dy, tones[0])


def coffee_beans_item():
    """Three roasted coffee beans, dark and glossy, each with its crease."""
    c = Canvas()
    for x, y in ((2, 3), (8, 2), (5, 9)):
        bean(c, x, y, BEAN[1:])
        c.px(x, y + 1, BEAN[4])
    outline(c, rgb("140a04"))
    return c.img


def coffee_seeds_item():
    """Green coffee seeds: three pale, unroasted beans."""
    c = Canvas()
    for x, y in ((2, 3), (8, 2), (5, 9)):
        bean(c, x, y, GREEN_BEAN)
    outline(c, rgb("3a4220"))
    return c.img


def tiny_seeds(dark, light, spots):
    """A scatter of small seeds, each two pixels, lit on one."""
    c = Canvas()
    for x, y in spots:
        c.px(x, y, light)
        c.px(x + 1, y, dark)
        c.px(x, y + 1, dark)
    return c.img


SEED_SPOTS = ((4, 3), (9, 4), (6, 7), (11, 8), (3, 10), (8, 11), (12, 12), (5, 13))


def fruit_crop_textures():
    """(kind, name) -> image for every texture of the fruit crops."""
    out = {}
    for bush in fruit_crops.BUSHES:
        for stage in range(fruit_crops.STAGES):
            out[("block", fruit_crops.stage_texture(bush, stage))] = PAINTERS[bush](stage)
    out[("item", "strawberry")] = strawberry_item()
    out[("item", "strawberry_seeds")] = tiny_seeds(rgb("a8862a"), rgb("f0d060"), SEED_SPOTS)
    out[("item", "blueberries")] = blueberries_item()
    out[("item", "blueberry_seeds")] = tiny_seeds(rgb("4a2e1a"), rgb("8a6a44"), SEED_SPOTS[1::2] + SEED_SPOTS[::3])
    out[("item", "coffee_cherries")] = coffee_cherries_item()
    out[("item", "coffee_seeds")] = coffee_seeds_item()
    out[("item", "coffee_beans")] = coffee_beans_item()
    return out
