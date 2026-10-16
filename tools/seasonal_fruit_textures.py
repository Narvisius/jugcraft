"""Batch 5 art: stable foliage masks and joined twigs with the same fruit positions in every season."""
from crop_textures import Canvas, rgb
import random

PALETTES = {
    "rowan": (("384619", "4f6123", "67802e", "7e9b3c", "99ba4d", "aec577"),
              ("4f3810", "6e4e17", "90661e", "ae7d29", "d09836", "d5ad67")),
    "elder": (("26351e", "364a2a", "466137", "577645", "6a9055", "82a66e"),
              ("3d3d1b", "565626", "707032", "898940", "a7a74f", "b7b770")),
}
FRUIT = {"rowan": ("8a1a0a", "c4300f", "e8521c", "f47a3a"),
         "elder": ("2d1932", "39233f", "482c4e", "6a4e70")}
SEEDS = {"rowan": 9501, "elder": 9502}


def fruit_centres(tree):
    """Species-specific clusters with separation across the repeating texture edges."""
    rng = random.Random(SEEDS[tree])
    centres = []
    while len(centres) < 3:
        x, y = rng.randrange(16), rng.randrange(16)
        if all(min(abs(x-a), 16-abs(x-a)) ** 2 + min(abs(y-b), 16-abs(y-b)) ** 2 >= 36 for a, b in centres):
            centres.append((x, y))
    return centres


def fruit_overlay(image, tree, stage):
    c = Canvas()
    c.img = image.copy()
    ripe = [rgb(x) for x in FRUIT[tree]]
    green = [rgb(x) for x in ("47532a", "637239", "82944d", "a4af69")]
    def dot(x, y, colour): c.px(x % 16, y % 16, colour)
    for x, y in fruit_centres(tree):
        for dy in (-2, -1, 0): dot(x, y + dy, rgb("6e655b"))
        if tree == "elder" and stage == 1:
            for dx in range(-2, 3): dot(x + dx, y, rgb("caba6a"))
            for dx in (-2, 0, 2): dot(x + dx, y - 1, rgb("e0d9b6"))
        else:
            p = ripe if stage == 2 else green
            for dx, dy, tone in ((-1, 0, 0), (0, 0, 2), (1, 0, 1), (-1, 1, 1), (0, 1, 3), (1, 1, 0)):
                dot(x + dx, y + dy, p[tone])
    return c.img


def berry_item(tree):
    c = Canvas()
    p = [rgb(x) for x in FRUIT[tree]]
    c.line(7, 2, 8, 9, rgb("6e655b"))
    c.line(7, 4, 3, 3, rgb("577645"))
    c.line(8, 5, 12, 3, rgb("82a66e"))
    for x, y in ((5, 8), (10, 7), (8, 12)):
        for dx, dy, tone in ((-1,-1,2),(0,-1,3),(1,-1,1),(-2,0,0),(-1,0,1),(0,0,2),(1,0,1),
                             (2,0,0),(-1,1,1),(0,1,1),(1,1,0),(0,2,0)):
            c.px(x+dx,y+dy,p[tone])
    return c.img


def textures():
    from wood_style import paint_leaves, cypress_bare, BARK, pal
    out = {}
    for tree, (green, gold) in PALETTES.items():
        seed = SEEDS[tree]
        bark = BARK["fruitwood"][0] if tree == "rowan" else pal("3d3832", "554e46", "6e655b", "898075", "a59a8b")
        looks = {"": paint_leaves(pal(*green), "leaves", seed, gap_threshold=.18 if tree == "rowan" else .04),
                 "_gold": paint_leaves(pal(*gold), "leaves", seed, gap_threshold=.18 if tree == "rowan" else .04),
                 "_bare": cypress_bare(seed, bark)}
        for suffix, image in looks.items():
            name = tree + "_leaves" + suffix
            out[("block", name)] = image
            for stage, fruit_suffix in ((1, "_unripe"), (2, "_ripe")):
                out[("block", name + fruit_suffix)] = fruit_overlay(image, tree, stage)
        c = Canvas()
        c.line(8, 15, 8, 4, bark[2])
        c.line(7, 15, 7, 10, bark[1])
        p = pal(*green)
        for x, y in ((4, 7), (10, 5), (8, 3)):
            c.line(8, y+4, x, y, bark[2])
            for dx, dy in ((-1,0),(0,-1),(0,0),(1,0),(0,1)):
                c.px(x+dx,y+dy,p[4 if dy <= 0 else 2])
            c.px(x-1,y-1,p[5])
        out[("block", tree + "_sapling")] = c.img
        out[("item", "rowan_berries" if tree == "rowan" else "elderberries")] = berry_item(tree)
    return out
