"""Editable 16px foliage and produce; fig/mango timber uses the owner's painted wood palette."""
import random
from crop_textures import Canvas, rgb

PALETTES = {
    'flame_tree': ('4a1c0e', '682814', '89341a', 'a54224', 'c9512c', 'd37255'),
    'sea_grape': ('2a321a', '3b4725', '4e5c30', '5f713d', '748a4a', '8ba35c'),
    'osmanthus': ('162a14', '1e3a1b', '284c24', '325d2e', '3e7138', '4c8646'),
    'banyan': ('173113', '20441a', '2a5922', '356c2c', '408436', '509c45'),
    'mango': ('0f2b11', '153d17', '1b4f1f', '246028', '2b7530', '388a3d'),
}


def harvest(image, tree, ripe):
    c = Canvas(); c.img = image.copy()
    for x, y in ((3, 3), (10, 2), (7, 10)):
        if tree == 'mango':
            if ripe:
                c.rect(x, y, x + 1, y + 2, rgb('d0582a'))
                c.line(x, y, x, y + 1, rgb('e0a428')); c.px(x, y - 1, rgb('5a8a2a'))
            else:
                for dx, dy in ((0, 0), (1, 1), (-1, 1)):
                    c.px(x + dx, y + dy, rgb('e4ccc0' if dx == 0 else 'd8b8a8'))
        elif tree == 'sea_grape':
            colors = ('3e1e42', '5a2d5e', '7a4a7e') if ripe else ('4e632b', '6a8a3a', '8aa34a')
            c.line(x, y - 1, x, y + 1, rgb('4e5c30'))
            for dx, dy, tone in ((-1, 1, 2), (1, 1, 1), (0, 2, 2), (1, 3, 0), (0, 4, 1)):
                c.px(x + dx, y + dy, rgb(colors[tone]))
        else:
            # Tiny gold flowers, not large white blossom blobs.
            for dx, dy in ((0, 0), (2, 1), (-1, 2)) if ripe else ((0, 0),):
                c.px(x + dx, y + dy, rgb('f2e2a8' if ripe else 'e8d08a'))
    return c.img


def textures():
    from wood_style import paint_leaves, pal
    out = {}
    for index, (tree, colors) in enumerate(PALETTES.items()):
        p = pal(*colors)
        leaf = paint_leaves(p, 'blossom' if tree == 'flame_tree' else 'leaves', 11100 + index,
                            gap_threshold=.22 if tree == 'flame_tree' else .14 if tree == 'mango' else .16 if tree == 'banyan' else .18,
                            edge_green=pal('3a6a2a', '4c8236'), flower_coverage=.85)
        c = Canvas(); c.img = leaf
        if tree in ('sea_grape', 'banyan'):
            for x, y in ((2, 2), (8, 1), (12, 6), (4, 8), (9, 12), (1, 13)):
                c.rect(x, y, x + 1, y + 1, p[3]); c.px(x, y, p[5]); c.px(x + 1, y + 2, p[1])
        if tree == 'banyan':
            for x, y, color in ((4, 5, '7d2929'), (11, 10, '9c3e3e'), (12, 3, 'bb5555')): c.px(x, y, rgb(color))
        if tree == 'osmanthus':
            for x, y in ((3, 3), (10, 2), (7, 10)): c.px(x, y, rgb('e8d08a'))
        out['block', tree + '_leaves'] = leaf
        if tree in ('sea_grape', 'mango', 'osmanthus'):
            out['block', tree + '_leaves_unripe'] = harvest(leaf, tree, False)
            out['block', tree + '_leaves_ripe'] = harvest(leaf, tree, True)
        c = Canvas(); bark = rgb('817d79' if tree == 'banyan' else '615a56' if tree == 'mango' else '7b7f66')
        c.line(8, 15, 8, 7, bark)
        rng = random.Random(11200 + index)
        for x, y in ((4, 9), (8, 5), (12, 9)) if tree != 'sea_grape' else ((3, 12), (8, 10), (12, 12)):
            c.line(8, 13, x, y, bark)
            for dx in range(-2, 3):
                for dy in range(-2, 2):
                    if abs(dx) + abs(dy) < 4: c.px(x + dx, y + dy, p[rng.choice((2, 3, 4, 5))])
            if tree == 'osmanthus': c.px(x, y, rgb('f2e2a8'))
        out['block', tree + '_sapling'] = c.img
    c = Canvas()
    c.line(7, 2, 8, 4, rgb('4e5c30')); c.line(7, 2, 10, 1, rgb('748a4a'))
    for x, y in ((4, 5), (9, 5), (6, 8), (9, 10), (6, 12)):
        c.rect(x, y, x + 2, y + 2, rgb('3e1e42')); c.rect(x, y, x + 1, y + 1, rgb('5a2d5e')); c.px(x, y, rgb('9a70a0'))
    out['item', 'sea_grapes'] = c.img
    c = Canvas(); c.line(8, 1, 7, 4, rgb('5a632f'))
    for y, lo, hi in ((4, 5, 9), (5, 4, 10), (6, 3, 11), (7, 3, 11), (8, 3, 11), (9, 3, 10), (10, 4, 10), (11, 4, 9), (12, 5, 8)):
        c.line(lo, y, hi, y, rgb('9c4425'))
        for x in range(lo + 1, hi): c.px(x, y, rgb('e0a428' if x < 7 else 'd0582a'))
    c.line(5, 6, 4, 8, rgb('f4cb5c')); c.px(7, 4, rgb('5a8a2a'))
    out['item', 'mango'] = c.img
    c = Canvas()
    for x, y in ((4, 5), (10, 4), (8, 10), (4, 12)):
        c.line(x, y, 8, 14, rgb('325d2e'))
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)): c.px(x + dx, y + dy, rgb('f2e2a8'))
        c.px(x, y, rgb('c49c45'))
    out['item', 'osmanthus_flowers'] = c.img
    return out
