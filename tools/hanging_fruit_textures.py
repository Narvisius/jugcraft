"""Baobab foliage and hanging fruit, using the existing editable 16px painters."""
import random
from crop_textures import Canvas, rgb


def textures():
    from wood_style import paint_leaves, pal
    greens = pal("243e18", "335721", "43722c", "548a38", "66a845", "80ba63")
    out = {("block", "baobab_leaves"): paint_leaves(greens, "leaves", 11001, gap_threshold=.24)}
    c = Canvas(); c.rect(7, 8, 9, 15, rgb("937e77")); c.line(7, 8, 4, 6, rgb("a69691")); c.line(9, 8, 12, 5, rgb("7b6861"))
    rng = random.Random(11002)
    for cx, cy in ((4, 5), (8, 3), (12, 5)):
        for dx in range(-2, 3):
            for dy in range(-1, 2):
                if abs(dx) + abs(dy) < 3: c.px(cx + dx, cy + dy, greens[rng.choice((2, 3, 4, 5))])
    out["block", "baobab_sapling"] = c.img
    for age in range(3):
        c = Canvas(); c.line(7, 0, 7, 3, rgb("6e7056"))
        p = pal("4b5138", "6e7056", "8a8c6c", "a6a886") if age < 2 else pal("514c36", "7a7052", "9d9170", "bdb18c")
        length = 3 + age
        c.rect(7, 3, 8, 3 + length, p[1]); c.line(7, 3, 7, 2 + length, p[2]); c.px(7, 3, p[3]); c.px(8, 3 + length, p[0])
        out["block", f"baobab_pod_{age}"] = c.img
        c = Canvas(); c.line(7, 0, 7, 5, rgb("d0a040"))
        p = pal("806120", "c89a2a", "e0bc48") if age < 2 else pal("7a3e18", "a45a24", "c87a38")
        for x, y in ((4, 4), (9, 4), (6, 7), (10, 7), (4, 8))[:3 if age == 0 else 5]:
            c.line(7, 2, x, y, rgb("a38733")); c.rect(x, y, x + 1, y + 2, p[0]); c.line(x, y, x, y + 1, p[1]); c.px(x, y, p[2])
        out["block", f"date_cluster_{age}"] = c.img
    c = Canvas(); c.line(8, 1, 7, 4, rgb("514c36"))
    c.rect(5, 5, 10, 12, rgb("514c36")); c.rect(6, 4, 9, 13, rgb("514c36"))
    c.rect(6, 5, 9, 12, rgb("8a8c6c")); c.line(6, 6, 6, 10, rgb("bdb18c")); c.line(9, 7, 9, 11, rgb("6e7056"))
    out["item", "baobab_fruit"] = c.img
    c = Canvas()
    for x, y in ((4, 7), (9, 10), (9, 4)):
        c.rect(x, y, x + 2, y + 3, rgb("3d3023")); c.line(x + 1, y, x + 1, y + 2, rgb("a8966f")); c.px(x + 1, y, rgb("d6c59d"))
    out["item", "baobab_seeds"] = c.img
    c = Canvas()
    for x, y in ((3, 6), (8, 4), (9, 10)):
        c.rect(x, y, x + 3, y + 4, rgb("54301d")); c.rect(x + 1, y, x + 2, y + 4, rgb("a45a24")); c.line(x + 1, y + 1, x + 1, y + 2, rgb("d28d51")); c.px(x + 2, y + 3, rgb("7a3e18"))
    out["item", "dates"] = c.img
    return out
