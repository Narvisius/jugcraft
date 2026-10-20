"""Olive art on the approved natural-texture tools and owner painting 3's green-gold palette."""
from crop_textures import Canvas, rgb

GREEN = ("424a37", "576249", "6d7b5b", "83926f", "9ba88b", "b4bca9")


def clusters(image, ripe):
    c = Canvas(); c.img = image.copy()
    colors = tuple(map(rgb, ("291f2b", "3a2a3a", "5a3f55") if ripe else ("4e5b20", "6a7a2a", "8a9a3a")))
    for x, y in ((3, 4), (10, 2), (7, 11)):
        for dx, dy, tone in ((0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 0), (0, 2, 0), (3, 1, 2), (3, 2, 1), (3, 3, 0)):
            c.px(x + dx, y + dy, colors[tone])
    return c.img


def textures():
    from wood_style import paint_leaves, pal, ramp, WOOD
    leaf = paint_leaves(pal(*GREEN), "leaves", 10701, gap_threshold=.08)
    c = Canvas(); c.img = leaf
    # Narrow two/three-pixel leaves, silver on their upper edge, keep the dense underlying canopy.
    for x, y in ((1, 2), (6, 1), (11, 4), (3, 8), (8, 7), (12, 11), (5, 13)):
        c.line(x, y, x + 2, y - 1, rgb(GREEN[4]))
        c.line(x, y + 1, x + 2, y, rgb(GREEN[2]))
        c.px(x, y, rgb(GREEN[5]))
    out = {("block", "olive_leaves"): leaf, ("block", "wild_olive_leaves"): leaf.copy(),
           ("block", "olive_leaves_unripe"): clusters(leaf, False),
           ("block", "olive_leaves_ripe"): clusters(leaf, True)}
    for tree, short in (("olive", False), ("olive_bush", True)):
        c = Canvas()
        c.line(8, 15, 8, 5 if not short else 9, rgb("616451"))
        for x, y in ((4, 6), (11, 5), (5, 10), (11, 10)) if not short else ((3, 10), (8, 8), (12, 10)):
            c.line(8, 12, x, y, rgb("7b7f66"))
            for dx, dy in ((-2, 0), (-1, -1), (0, -1), (1, 0), (0, 1)):
                c.px(x + dx, y + dy, rgb(GREEN[4 if dy < 0 else 2]))
        out["block", tree + "_sapling"] = c.img
    c = Canvas()
    for x, y in ((4, 6), (10, 8), (8, 3)):
        c.line(x, y - 2, x + 1, y - 3, rgb(GREEN[2]))
        for dx in range(-2, 3):
            for dy in range(-1, 4):
                if abs(dx) == 2 and dy in (-1, 3): continue
                c.px(x + dx, y + dy, rgb("291f2b" if abs(dx) == 2 or dy == 3 else "5a3f55" if dx < 0 and dy < 1 else "3a2a3a"))
    out["item", "olives"] = c.img
    return out


def figure(image):
    """Three restrained, broken grain streaks; tiling and the four-board silhouette remain intact."""
    from wood_style import ramp, WOOD
    c = Canvas(); c.img = image.copy(); colors = ramp(rgb(WOOD["olive"][1]))
    for x, y in ((2, 1), (9, 5), (5, 9)):
        for dx, dy in ((0, 0), (1, 0), (2, 1), (3, 1)):
            c.px(x + dx, y + dy, colors[1 if dx < 2 else 0])
    return c.img
