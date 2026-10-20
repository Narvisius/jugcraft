"""Holly's approved palette, drawn on Jugcraft's existing deterministic 16x16 tools."""
from crop_textures import Canvas, rgb

GREEN = ("17340f", "204915", "29601c", "347425", "408e2d", "50a63b")
RED = ("8a1414", "c21f1f", "e04a3a")


def berries(image):
    c = Canvas()
    c.img = image.copy()
    for x, y in ((3, 4), (11, 3), (6, 11), (13, 12)):
        for dx, dy, tone in ((0, 0, 2), (1, 0, 1), (0, 1, 0), (2, 1, 1), (2, 2, 0)):
            c.px(x + dx, y + dy, rgb(RED[tone]))
    return c.img


def sprig(sapling=False):
    c = Canvas()
    c.line(7, 14, 8, 3, rgb("5e655a"))
    c.line(8, 13, 8, 5, rgb("858e7f"))
    for x, y, side in ((5, 5, -1), (11, 7, 1), (4, 10, -1), (10, 11, 1)):
        c.line(8, y + 2, x, y, rgb("717a6c"))
        # Pointed, slightly toothed leaves with a light upper edge and dark lower edge.
        for dx, dy in ((0, -2), (-1, -1), (0, -1), (1, -1), (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (-1, 1), (0, 1), (1, 1), (0, 2)):
            c.px(x + dx, y + dy, rgb(GREEN[4 if dy < 0 else 2 if dy == 0 else 0]))
        c.px(x, y, rgb(GREEN[5]))
    if not sapling:
        for x, y in ((7, 7), (9, 5), (8, 10)):
            c.px(x, y, rgb(RED[2])); c.px(x + 1, y, rgb(RED[1])); c.px(x, y + 1, rgb(RED[0]))
    return c.img


def textures():
    from wood_style import paint_leaves, pal
    leaves = paint_leaves(pal(*GREEN), "leaves", 10661, gap_threshold=.08)
    c = Canvas(); c.img = leaves
    for x, y in ((2, 2), (6, 5), (11, 8), (4, 12), (13, 3), (9, 14)):
        c.px(x, y, rgb(GREEN[5]))
    wreath = paint_leaves(pal(*GREEN), "leaves", 10662, gap_threshold=0)
    red = Canvas()
    for y in range(16):
        for x in range(16):
            red.px(x, y, rgb(RED[2 if x < 5 and y < 5 else 0 if x > 11 or y > 11 else 1]))
    return {("block", "holly_leaves"): leaves, ("block", "holly_leaves_berries"): berries(leaves),
            ("block", "holly_sapling"): sprig(True), ("item", "holly_sprig"): sprig(),
            ("block", "holly_wreath_leaves"): wreath, ("block", "holly_wreath_berries"): red.img}
