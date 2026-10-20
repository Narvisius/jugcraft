"""Batch 6 foliage on the shared 16x16 deterministic texture tools; no existing seed changes."""
from crop_textures import Canvas, rgb

PALETTES = {
    "juniper": ("1e2a2e", "27373b", "324548", "3f5558", "4f676a", "688487"),
    "gorse": ("14200e", "1c2c14", "253a1a", "2e4721", "385629", "446633"),
    "sagebrush": ("3e463a", "535c4e", "697262", "80897a", "9aa392", "b4bca8"),
}


def berries(image, ripe):
    c = Canvas()
    c.img = image.copy()
    tones = ("2e3a66", "3a4a6e", "9aaac4") if ripe else ("344a3a", "506044", "7e8b68")
    for x, y in ((3, 4), (11, 3), (5, 11), (12, 12)):
        c.px(x, y, rgb(tones[2]))
        c.px(x, y+1, rgb(tones[0]))
        c.px(x+1, y+1, rgb(tones[1]))
    return c.img


def textures():
    from wood_style import paint_leaves, pal, BARK
    out = {}
    for index, (tree, colours) in enumerate(PALETTES.items()):
        p = pal(*colours)
        image = paint_leaves(p, "needles" if tree != "sagebrush" else "leaves", 10601 + index, gap_threshold=.10)
        c = Canvas()
        c.img = image
        if tree == "gorse":
            flowers = pal("927e1c", "b09927", "d2b834", "d7c465")
            # Small, spaced flower clusters retain a dark green field, rather than a yellow cube.
            for x, y in ((2,2),(8,1),(13,3),(5,5),(10,7),(1,9),(5,12),(12,12),(9,15)):
                for dx, dy in ((0,-1),(-1,0),(0,0),(1,0),(-1,1),(0,1),(1,1),(0,2)):
                    c.px((x+dx)%16, (y+dy)%16, flowers[3 if dy < 0 else 2 if dx <= 0 else 1])
        name = "juniper_needles" if tree == "juniper" else tree + "_leaves"
        out["block", name] = c.img
        if tree == "juniper":
            out["block", name + "_unripe"] = berries(c.img, False)
            out["block", name + "_ripe"] = berries(c.img, True)
        young = Canvas()
        bark = BARK["juniper" if tree == "juniper" else "dead" if tree == "sagebrush" else "chestnut"][0]
        young.line(8, 15, 8, 5 if tree == "juniper" else 9, bark[3])
        young.line(7, 15, 7, 12, bark[1])
        for x, y in ((4,8),(11,8),(8,4)) if tree == "juniper" else ((4,10),(11,10),(8,8)):
            young.line(8, 14, x, y, bark[2])
            for dx, dy in ((-1,0),(0,-1),(0,0),(1,0),(0,1),(-1,-1),(1,1)):
                young.px(x+dx,y+dy,p[5 if dy < 0 else 3])
            if tree == "gorse": young.px(x,y, rgb("d2b834"))
        out["block", tree + "_sapling"] = young.img
    c = Canvas()
    c.line(8, 2, 7, 11, rgb("4c4537"))
    for x,y in ((4,6),(10,6),(6,11),(11,11)):
        c.line(8,4,x,y,rgb("4c4537"))
        for dx,dy,tone in ((0,-1,"9aaac4"),(-1,0,"2e3a66"),(0,0,"586887"),(1,0,"2e3a66"),(0,1,"273253")):
            c.px(x+dx,y+dy,rgb(tone))
    out["item", "juniper_berries"] = c.img
    return out
