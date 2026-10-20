"""Batch 13 extends the shared deterministic foliage and food painters."""
from crop_textures import Canvas, rgb


def textures():
    from wood_style import paint_leaves, cypress_bare, BARK, pal
    green = pal("2b3c1a", "3c5424", "4f6e2f", "61863c", "77a449", "8fb668")
    gold = pal("504412", "705f19", "937c21", "b2972c", "cfb240", "d6c271")
    bark = BARK["walnut"][0]
    bare = cypress_bare(13241, bark)
    looks = {"": paint_leaves(green, "leaves", 13241, gap_threshold=.05),
             "_gold": paint_leaves(gold, "leaves", 13241, gap_threshold=.05), "_bare": bare}
    out = {}
    for suffix, image in looks.items():
        for stage in ("", "_unripe", "_ripe"):
            c = Canvas(); c.img = image.copy()
            if stage and suffix != "_bare":
                p = pal("2e2418", "4a3a28", "66533b") if stage == "_ripe" else pal("8aa040", "a8c050", "c4d870")
                for x, y in ((3, 4), (11, 5), (6, 11)):
                    c.line(x, y-2, x, y-1, bark[2])
                    for dx, dy, shade in ((0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 0)):
                        c.px(x+dx, y+dy, p[shade])
            out["block", "walnut_leaves"+suffix+stage] = c.img
    c = Canvas()
    c.line(7, 15, 7, 4, bark[2]); c.line(8, 15, 8, 5, bark[3])
    for x, y in ((3, 7), (11, 6), (7, 2)):
        c.line(7, y+5, x, y, bark[3])
        for dx, dy in ((-1, 0), (0, -1), (0, 0), (1, 0), (0, 1), (-1, -1), (1, 1)):
            c.px(x+dx, y+dy, green[4 if dy < 0 else 2])
    out["block", "walnut_sapling"] = c.img
    c = Canvas()
    for x, y in ((3, 7), (9, 9)):
        for dx in range(5):
            for dy in range(4):
                if dx in (0, 4) and dy == 0: continue
                c.px(x+dx, y+dy, rgb(("302619", "4a3a28", "70613a")[(dx+dy) % 3]))
    out["item", "walnut_husk"] = c.img
    c = Canvas()
    # Two toasted, lobed kernels with the characteristic dark folds.
    p = pal("65402a", "97613b", "bc8650", "d9aa70")
    for cx, cy in ((5, 6), (10, 10)):
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                if dx*dx + dy*dy > 11: continue
                shade = 0 if dx == 0 and abs(dy) < 3 else 1 if dy > 1 else 3 if dx+dy < -1 else 2
                c.px(cx+dx, cy+dy, p[shade])
        for dx, dy in ((-2,-1),(1,-2),(-1,2),(2,1)):
            c.px(cx+dx,cy+dy,p[1])
    out["item", "roasted_walnuts"] = c.img
    return out
