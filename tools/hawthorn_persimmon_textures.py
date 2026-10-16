"""Roster palettes on the shared deterministic foliage, joined twigs and item drawing tools."""
from crop_textures import Canvas, rgb

PALETTES = {
    "hawthorn": (("16301a", "1f4022", "2a522c", "366638", "447a44", "5c925a"),
                 ("3c2e1a", "544124", "6e552f", "86683c", "a48049", "b69768")),
    "persimmon": (("133016", "1b431e", "245828", "2e6b33", "38823e", "47994e"),
                  ("3a1018", "561a22", "74262c", "8e3634", "a84a3e", "c06450")),
}
CENTRES = {"hawthorn": ((3, 4), (11, 3), (5, 11), (12, 12)),
           "persimmon": ((3, 3), (11, 2), (7, 7), (2, 12), (12, 12))}


def fruit_overlay(image, bare, tree, ripe):
    """Anchor each fruit to the same joined twig in every look, so winter fruit cannot float."""
    c = Canvas()
    c.img = image.copy()
    anchors = [(x, y) for x in range(16) for y in range(16) if bare.getpixel((x, y))[3]]
    for x, y in CENTRES[tree]:
        ax, ay = min(anchors, key=lambda p: (p[0]-x)**2 + (p[1]-y+1)**2)
        c.line(ax, ay, x, y - 1, rgb("554e46" if tree == "hawthorn" else "36383c"))
        colours = ("8e1c18", "d04a36") if tree == "hawthorn" else ("b8561a", "de7a2a", "f0a050")
        if not ripe:
            colours = ("526323", "6a7a2a") if tree == "hawthorn" else ("596a2b", "7d8d3e", "a5ad58")
        if tree == "hawthorn":
            c.px(x, y, rgb(colours[1])); c.px(x, y+1, rgb(colours[0]))
        else:
            c.px(x, y, rgb(colours[2])); c.px(x+1, y, rgb(colours[1]))
            c.px(x, y+1, rgb(colours[1])); c.px(x+1, y+1, rgb(colours[0]))
    return c.img


def persimmon_item():
    c = Canvas()
    p = [rgb(v) for v in ("743918", "b8561a", "de7a2a", "f0a050", "ffc16c")]
    for y in range(4, 14):
        for x in range(2, 14):
            d = ((x-7.5)/6)**2 + ((y-8)/5.5)**2
            if d < 1:
                c.px(x, y, p[0 if d > .84 else 1 if y > 10 or x > 10 else 2])
    c.line(4, 6, 6, 5, p[4]); c.line(3, 7, 3, 8, p[3])
    for x, y in ((7, 3), (8, 3), (6, 4), (9, 4), (5, 3), (10, 3), (7, 5), (8, 5)):
        c.px(x, y, rgb("526332"))
    c.line(8, 1, 7, 3, rgb("554e46"))
    return c.img


def textures():
    from wood_style import paint_leaves, cypress_bare, BARK, pal
    out = {}
    for index, (tree, (green, gold)) in enumerate(PALETTES.items()):
        seed = 9551 + index
        bark = BARK["fruitwood" if tree == "hawthorn" else tree][0]
        bare = cypress_bare(seed, bark)
        looks = {"": paint_leaves(pal(*green), "leaves", seed, gap_threshold=.03 if tree == "hawthorn" else .08),
                 "_gold": paint_leaves(pal(*gold), "leaves", seed, gap_threshold=.03 if tree == "hawthorn" else .08),
                 "_bare": bare}
        for suffix, image in looks.items():
            name = tree + "_leaves" + suffix
            out[("block", name)] = image
            for ripe, fruit_suffix in ((False, "_unripe"), (True, "_ripe")):
                out[("block", name + fruit_suffix)] = fruit_overlay(image, bare, tree, ripe)
        c = Canvas()
        c.line(8, 15, 8, 4, bark[3]); c.line(7, 15, 7, 11, bark[1])
        p = pal(*green)
        for x, y in ((4, 8), (11, 7), (8, 3)):
            c.line(8, y+4, x, y, bark[3])
            for dx, dy in ((-1,0),(0,-1),(0,0),(1,0),(0,1),(-1,-1),(1,1)):
                c.px(x+dx,y+dy,p[5 if dy < 0 else 3])
        out[("block", tree + "_sapling")] = c.img
    c = Canvas()
    c.line(8, 2, 7, 11, rgb("554e46"))
    for x, y in ((4, 6), (10, 6), (6, 11), (11, 11)):
        c.line(8, 4, x, y, rgb("554e46"))
        for dx, dy, colour in ((0,-1,"d04a36"),(-1,0,"b8302a"),(0,0,"b8302a"),(1,0,"8e1c18"),(0,1,"8e1c18")):
            c.px(x+dx,y+dy,rgb(colour))
    out[("item", "haws")] = c.img
    out[("item", "persimmon")] = persimmon_item()
    return out
