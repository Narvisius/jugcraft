"""Small authored tree details, using the same Canvas and palettes as the established wood generator."""
import random

from crop_textures import Canvas, rgb


def cluster_centres(seed, count=3, spacing=4):
    """Distinct seeded clusters, separated even across a repeating texture's edges."""
    rng, points = random.Random(seed), []
    for _ in range(256):
        point = rng.randrange(16), rng.randrange(16)
        if all(sum(min(abs(a - b), 16 - abs(a - b)) ** 2 for a, b in zip(point, other)) >= spacing ** 2 for other in points):
            points.append(point)
            if len(points) == count:
                return points
    raise ValueError("Cannot fit fruit clusters at the requested spacing")


def yew_fruit(base, ripe):
    canvas = Canvas()
    canvas.img = base.copy()
    colours = [rgb(c) for c in ("901a29", "b22e3e", "d04859", "3a1418")] if ripe else [rgb(c) for c in ("39472a", "53633a", "71854d", "25301b")]
    for x, y in cluster_centres(9303):
        for dx, dy, shade in ((-1, 0, 0), (0, -1, 2), (1, 0, 1), (0, 1, 0), (0, 0, 3)):
            canvas.px((x + dx) % 16, (y + dy) % 16, colours[shade])
    return canvas.img


def yew_sapling():
    from wood_style import BARK, LEAVES
    canvas = Canvas()
    bark, leaves = BARK["yew"][0], LEAVES["yew_needles"][0]
    canvas.line(8, 15, 8, 5, bark[2])
    for y, half in ((4, 2), (7, 4), (10, 5)):
        canvas.line(8 - half, y, 8 + half, y, bark[2])
        for dx in range(-half, half + 1):
            canvas.px(8 + dx, y - 1, leaves[4 if dx % 2 else 3])
            canvas.px(8 + dx, y + 1, leaves[2])
    canvas.px(8, 2, leaves[5])
    return canvas.img


def yew_arils():
    canvas = Canvas()
    palette = [rgb(c) for c in ("3a1418", "681421", "901a29", "b22e3e", "d04859", "e3747e")]
    for x, y in ((5, 5), (11, 7), (7, 11)):
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if abs(dx) + abs(dy) <= 3:
                    canvas.px(x + dx, y + dy, palette[1 if dy == 2 else 2 if dx == 2 else 3])
        canvas.px(x - 1, y - 1, palette[4])
        canvas.px(x, y - 2, palette[5])
        canvas.px(x, y, palette[0])
    return canvas.img


def textures(base):
    needles = base[("block", "yew_needles")]
    return {("block", "yew_needles_unripe"): yew_fruit(needles, False),
            ("block", "yew_needles_ripe"): yew_fruit(needles, True),
            ("block", "yew_sapling"): yew_sapling(), ("item", "yew_arils"): yew_arils(), **wonder_textures(), **wetland_textures(base)}


def wetland_textures(base):
    """Batches 4b/4c: sparse cream flowers and saplings in the approved species palettes."""
    from wood_style import BARK, LEAVES, pal
    flowers = pal("b8962e", "c9b578", "ddd2b2", "e8e1cc")
    magnolia = Canvas()
    magnolia.img = base[("block", "magnolia_leaves")].copy()
    for x, y in cluster_centres(9443, count=3, spacing=5):
        for dx, dy, tone in ((-1, 0, 2), (0, -1, 3), (1, 0, 1), (0, 1, 2), (0, 0, 0)):
            magnolia.px((x + dx) % 16, (y + dy) % 16, flowers[tone])
    out = {("block", "magnolia_leaves"): magnolia.img}
    for tree, leaf in (("bald_cypress", "bald_cypress_needles"), ("magnolia", "magnolia_leaves")):
        canvas = Canvas()
        bark, foliage = BARK[tree][0], LEAVES[leaf][0]
        canvas.line(8, 15, 8, 3, bark[2])
        canvas.line(7, 15, 7, 10, bark[1])
        rng = random.Random(9442 if tree == "bald_cypress" else 9443)
        for x, y, radius in ((5, 7, 2), (11, 6, 2), (8, 3, 2)) if tree == "bald_cypress" else ((4, 7, 3), (11, 7, 3), (8, 3, 3)):
            canvas.line(8, y + 4, x, y, bark[2])
            for dx in range(-radius, radius + 1):
                for dy in range(-2, 3):
                    if dx * dx + dy * dy <= radius * radius and rng.random() > .1:
                        canvas.px(x + dx, y + dy, foliage[rng.choice((2, 3, 4))])
            canvas.px(x - 1, y - 1, foliage[5])
        if tree == "magnolia":
            for x, y in ((4, 6), (10, 6)):
                canvas.px(x, y, flowers[3])
                canvas.px(x + 1, y, flowers[1])
        out[("block", tree + "_sapling")] = canvas.img
    return out


def wonder_textures():
    """Batch 4: roster palettes and the established seeded leaf painter; no copied timber pixels."""
    from wood_style import paint_leaves, pal
    snow = pal("72687e", "8a7f94", "a79db0", "c2bbc8", "d6d1da", "e4e1e7")
    mint = pal("1b4433", "265f47", "327c5d", "419773", "52b58b", "7ac2a4")
    snow_leaves = paint_leaves(snow, "blossom", 9401, gap_threshold=.18,
                              edge_green=pal("2e5426", "3c6a2e"), under_gap=rgb("2a3328"), flower_coverage=.90)
    shimmer = Canvas()
    shimmer.img = paint_leaves(mint, "leaves", 9402)
    aurora = pal("7a5288", "a070b0", "c49ad0")
    for x, y in cluster_centres(9402):
        for dx, dy, tone in ((0, 0, 1), (0, -1, 2), (1, 0, 0), (-1, 0, 0)):
            shimmer.px((x + dx) % 16, (y + dy) % 16, aurora[tone])
    petal = Canvas()
    for x, y, tone in ((7, 6, 5), (8, 6, 4), (6, 7, 4), (7, 7, 5), (8, 7, 3),
                       (7, 8, 4), (8, 8, 2), (8, 9, 3)):
        petal.px(x, y, snow[tone])
    return {("block", "snowpetal_cherry_leaves"): snow_leaves,
            ("block", "shimmer_birch_leaves"): shimmer.img,
            ("block", "snowpetal_cherry_sapling"): wonder_sapling(snow, False),
            ("block", "shimmer_birch_sapling"): wonder_sapling(mint, True),
            ("particle", "snowpetal"): petal.img}


def wonder_sapling(leaves, birch):
    canvas = Canvas()
    bark = [rgb(c) for c in (("58545b", "c4c4b4", "e1dec9") if birch else ("35222d", "5e3944", "84525e"))]
    canvas.line(8, 15, 8, 3, bark[1])
    canvas.line(7, 15, 7, 8, bark[0])
    rng = random.Random(9403 if birch else 9401)
    for x, y, radius in ((5, 8, 2), (11, 6, 2), (8, 3, 2)) if birch else ((4, 7, 3), (11, 6, 3), (8, 3, 3)):
        canvas.line(8, y + 4, x, y, bark[1])
        for dy in range(-2, 3):
            for dx in range(-radius, radius + 1):
                if dx * dx + dy * dy <= radius * radius and rng.random() > .12:
                    tone = rng.choice((2, 3, 4)) if dy <= 0 else rng.choice((1, 2, 3))
                    canvas.px(x + dx, y + dy, leaves[tone])
        canvas.px(x - 1, y - 1, leaves[5])
    if birch:
        canvas.px(8, 12, bark[0])
        for x, y in ((4, 7), (10, 5), (8, 2)):
            canvas.px(x, y, rgb("c49ad0"))
    return canvas.img
