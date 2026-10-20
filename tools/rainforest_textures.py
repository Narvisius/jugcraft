"""Editable 16px fibrous fern trunk and fine fronds in the approved roster palette."""
import random
from crop_textures import Canvas, rgb


def textures():
    from wood_style import paint_leaves, pal
    p = pal('271d16', '392b20', '4b382a', '5d4634', '6f533e')
    green = pal('17331f', '20472c', '2a5d39', '357147', '418a57', '52a36a')
    rng = random.Random(12001)
    c = Canvas()
    for x in range(16):
        for y in range(16): c.px(x, y, p[1 if x % 5 == 0 else 2])
    for _ in range(65):
        x, y, length, tone = rng.randrange(16), rng.randrange(16), rng.randint(2, 7), rng.choice((0, 1, 3, 4))
        for dy in range(length): c.px((x + (dy // 3)) % 16, (y + dy) % 16, p[tone])
    for x, y in ((2, 4), (10, 2), (6, 12), (13, 9)):
        c.px(x, y, rgb('7a6a4a')); c.px((x + 1) % 16, (y + 1) % 16, p[3])
    out = {('block', 'tree_fern_trunk'): c.img}
    c = Canvas()
    # Fibrous cross-section, deliberately no timber growth rings.
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15-x, 15-y)
            c.px(x, y, p[rng.choice((0, 1)) if edge < 2 else rng.choice((1, 2, 2, 3))])
    out['block', 'tree_fern_trunk_top'] = c.img
    c = Canvas(); c.img = paint_leaves(green, 'leaves', 12002, gap_threshold=.23)
    for x0, y0 in ((1, 2), (7, 9), (11, 1)):
        for step in range(9):
            x, y = (x0 + step) % 16, (y0 + step // 2) % 16
            c.px(x, y, green[1])
            if step % 2 == 0:
                for side in (-1, 1):
                    for d in range(1, 4): c.px((x-d//2) % 16, (y+side*d) % 16, green[4 if side < 0 else 2])
    out['block', 'tree_fern_fronds'] = c.img
    c = Canvas(); c.line(8, 15, 8, 6, p[2]); c.line(7, 15, 7, 10, p[1])
    for endx, endy in ((2, 9), (4, 5), (8, 2), (12, 5), (14, 9)):
        c.line(8, 9, endx, endy, green[1])
        for n in range(1, 6):
            x, y = round(8+(endx-8)*n/6), round(9+(endy-9)*n/6)
            c.px(x-1, y, green[4]); c.px(x+1, y+1, green[2])
    out['block', 'tree_fern_sapling'] = c.img
    out.update(rubber_textures())
    return out


def rubber_textures():
    from wood_style import paint_leaves, pal
    p = pal('434137', '585548', '6d695a', '827e6b', '95917e')
    green = pal('1e3914', '29501b', '366824', '447f2f', '539b39', '66b54a')
    rng = random.Random(12003); c = Canvas()
    c.rect(0, 0, 15, 15, p[2])
    for x in (2, 7, 12):
        for y in range(16):
            xx = (x + (1 if 5 <= (y+x) % 16 <= 9 else 0)) % 16
            c.px(xx, y, p[0 if y % 5 else 1]); c.px((xx+1) % 16, y, p[3])
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        for n in range(3):
            if c.img.getpixel((x, (y+n) % 16))[:3] == p[2]: c.px(x, (y+n) % 16, p[rng.choice((1, 3))])
    for x, y in ((5, 3), (10, 10), (15, 6)):
        for n in range(2): c.px(x, y+n, rgb('e6dcc4'))
    out = {('block', 'rubber_log'): c.img}
    c = Canvas(); ring = pal('9b8072', 'b59b88', 'c7ae98', 'd7c1a9')
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15-x, 15-y)
            c.px(x, y, p[1] if d == 0 else ring[1 if d % 3 == 1 else 2 if x+y > 14 else 3])
    out['block', 'rubber_log_top'] = c.img
    out['block', 'rubber_leaves'] = paint_leaves(green, 'leaves', 12004, gap_threshold=.17)
    c = Canvas(); c.line(8, 15, 8, 3, p[2])
    for x, y in ((4, 9), (11, 6), (7, 3)):
        c.line(8, 13, x, y, p[1]); c.rect(x-2, y-1, x+2, y+1, green[2]); c.line(x-1, y-1, x+1, y-1, green[4])
    out['block', 'rubber_tree_sapling'] = c.img
    c = Canvas(); c.rect(0, 0, 15, 15, rgb('805e3d'))
    for x in (0, 4, 9, 14):
        c.line(x, 0, x, 15, rgb('513a27')); c.line((x+1)%16, 0, (x+1)%16, 15, rgb('a17b50'))
    out['block', 'tree_tap'] = c.img
    c = Canvas(); c.rect(0, 0, 15, 15, rgb('e6dcc4'))
    for x, y in ((2, 3), (8, 11), (13, 6)): c.px(x, y, rgb('f3ead7'))
    out['block', 'latex'] = c.img
    c = Canvas(); dark, glass = rgb('476062'), rgb('8ca5a0')
    c.rect(6, 1, 9, 3, rgb('6d5338')); c.rect(6, 4, 9, 6, dark)
    c.rect(4, 7, 11, 13, dark); c.line(5, 14, 10, 14, dark)
    c.rect(5, 7, 10, 12, glass); c.rect(5, 9, 10, 13, rgb('c2b79f'))
    c.rect(6, 9, 9, 12, rgb('e6dcc4')); c.line(5, 8, 5, 11, rgb('e2f0e8'))
    c.line(6, 1, 8, 1, rgb('a58b62'))
    out['item', 'latex_bottle'] = c.img
    return out
