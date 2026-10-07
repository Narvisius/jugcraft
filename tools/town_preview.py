"""Pictures of the walled town for checking its look while it is designed (not shipped): isometric views and a plan
from above, coloured by rough block colours. Run `python3 tools/town.py --preview`; images go to build/town/."""
from PIL import Image, ImageDraw

from town_voxels import block_name

COLOURS = [
    ("water", (60, 100, 200)), ("lava", (230, 110, 20)), ("glass_pane", (170, 200, 220)), ("glass", (170, 200, 220)),
    ("blue_stained", (60, 70, 190)), ("light_blue_stained", (110, 160, 220)), ("yellow_stained", (230, 210, 60)),
    ("red_stained", (180, 40, 40)), ("magenta_stained", (190, 70, 180)), ("cyan_stained", (60, 150, 160)),
    ("orange_stained", (220, 130, 40)), ("purple_stained", (120, 50, 170)),
    ("deepslate_tile", (55, 55, 60)), ("deepslate", (70, 70, 75)), ("resin_brick", (190, 90, 40)),
    ("mud_brick", (160, 120, 90)), ("mossy_stone", (110, 120, 100)), ("cracked_stone", (120, 120, 120)),
    ("stone_brick", (125, 125, 125)), ("tuff_brick", (105, 108, 100)), ("nether_brick", (60, 30, 35)),
    ("brick", (150, 75, 60)), ("mangrove", (120, 50, 45)),
    ("dark_oak", (70, 48, 28)), ("spruce", (110, 80, 50)), ("jungle", (150, 110, 70)), ("birch", (200, 185, 130)),
    ("pale_oak", (225, 215, 210)), ("oak", (165, 130, 80)), ("acacia", (170, 90, 50)),
    ("calcite", (225, 225, 220)), ("white_terracotta", (210, 180, 165)), ("smooth_sandstone", (220, 210, 165)),
    ("mushroom_stem", (215, 210, 200)), ("quartz", (235, 230, 225)),
    ("mossy_stone", (110, 120, 100)), ("cracked_stone", (120, 120, 120)), ("stone_brick", (125, 125, 125)),
    ("chiseled_stone", (130, 130, 130)), ("mossy_cobble", (100, 115, 95)), ("cobble", (115, 115, 115)),
    ("polished_andesite", (140, 145, 140)), ("andesite", (135, 135, 135)), ("smooth_stone", (160, 160, 160)),
    ("tuff", (105, 108, 100)), ("stone", (125, 125, 125)), ("gravel", (130, 125, 120)),
    ("grass_block", (95, 150, 60)), ("dirt_path", (150, 125, 75)), ("coarse_dirt", (120, 90, 60)), ("dirt", (130, 95, 65)),
    ("leaves", (70, 130, 50)), ("hay", (200, 170, 50)), ("pumpkin", (220, 130, 30)), ("jack_o", (230, 150, 40)),
    ("melon", (110, 160, 50)), ("lantern", (250, 210, 120)), ("campfire", (230, 140, 50)), ("lightning_rod", (200, 120, 80)),
    ("iron_bars", (160, 160, 165)), ("iron_door", (190, 190, 190)), ("bell", (230, 190, 60)), ("gold", (240, 200, 60)),
    ("barrel", (130, 95, 55)), ("chest", (150, 110, 50)), ("bookshelf", (120, 90, 60)), ("crafting", (140, 100, 60)),
    ("furnace", (110, 110, 110)), ("smoker", (100, 90, 80)), ("bed", (170, 40, 40)), ("carpet", (150, 50, 50)),
    ("ladder", (150, 120, 70)), ("chain", (80, 80, 90)), ("candle", (230, 220, 190)), ("potted", (170, 90, 60)),
    ("netherrack", (110, 40, 40)), ("snow", (240, 245, 250)), ("ice", (160, 190, 240)),
    ("red_wool", (170, 40, 40)), ("white_wool", (235, 235, 235)), ("yellow_wool", (240, 200, 50)),
    ("orange_wool", (230, 120, 30)), ("black_wool", (30, 30, 35)), ("brown_wool", (110, 75, 45)),
    ("green_wool", (85, 110, 30)), ("lime_wool", (110, 180, 40)), ("pink_wool", (230, 140, 170)),
    ("light_blue_wool", (100, 170, 220)), ("purple_wool", (120, 50, 170)), ("blue_wool", (50, 60, 160)),
    ("banner", (180, 50, 50)), ("wool", (220, 220, 220)), ("short_grass", (95, 150, 60)), ("poppy", (200, 40, 40)),
    ("dandelion", (230, 210, 40)), ("cornflower", (80, 100, 220)), ("daisy", (240, 240, 230)), ("bluet", (220, 230, 240)),
    ("cherry", (240, 170, 200)), ("fence", (70, 48, 28)), ("ladder", (150, 120, 70)), ("wall", (125, 125, 125)),
]


def colour(state):
    name = block_name(state)
    for key, c in COLOURS:
        if key in name:
            return c
    return (200, 0, 200)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def iso(vox, scale=2, back=False):
    """Isometric picture of a voxel map, seen from the south-east (or the north-west)."""
    blocks = {p: s for p, s in vox.b.items() if block_name(s) not in ("air", "cave_air", "void_air")}
    if not blocks:
        return Image.new("RGB", (8, 8))
    x0, y0, z0 = (min(p[i] for p in blocks) for i in range(3))
    x1, y1, z1 = (max(p[i] for p in blocks) for i in range(3))
    if back:
        blocks = {(x0 + x1 - x, y, z0 + z1 - z): s for (x, y, z), s in blocks.items()}
    a, b, h = 2 * scale, scale, 2 * scale
    W = (x1 - x0 + z1 - z0 + 2) * a + 8
    H = (x1 - x0 + z1 - z0 + 2) * b + (y1 - y0 + 2) * h + 8
    img = Image.new("RGB", (W, H), (235, 240, 245))
    draw = ImageDraw.Draw(img)
    ox = (z1 - z0 + 1) * a + 4
    oy = (y1 - y0 + 1) * h + 4

    def solid(p):
        s = blocks.get(p)
        return s is not None and "pane" not in s and "fence" not in s

    for (x, y, z) in sorted(blocks, key=lambda p: (p[0] + p[2], p[1], p[0])):
        if solid((x + 1, y, z)) and solid((x, y, z + 1)) and solid((x, y + 1, z)):
            continue
        c = colour(blocks[(x, y, z)])
        sx = ox + ((x - x0) - (z - z0)) * a
        sy = oy + ((x - x0) + (z - z0)) * b - (y - y0) * h
        top = [(sx, sy - b), (sx + a, sy), (sx, sy + b), (sx - a, sy)]
        left = [(sx - a, sy), (sx, sy + b), (sx, sy + b + h), (sx - a, sy + h)]
        right = [(sx, sy + b), (sx + a, sy), (sx + a, sy + h), (sx, sy + b + h)]
        draw.polygon(left, fill=shade(c, 0.75))
        draw.polygon(right, fill=shade(c, 0.58))
        draw.polygon(top, fill=c)
    return img


def plan(vox, size, scale=4):
    """The town from above: each column's highest block."""
    top = {}
    for (x, y, z), s in vox.b.items():
        if block_name(s) in ("air",):
            continue
        if (x, z) not in top or y > top[(x, z)][0]:
            top[(x, z)] = (y, s)
    img = Image.new("RGB", (size * scale, size * scale), (60, 90, 40))
    draw = ImageDraw.Draw(img)
    for (x, z), (y, s) in top.items():
        if 0 <= x < size and 0 <= z < size:
            c = shade(colour(s), 0.75 + min(0.5, max(0, y) / 60))
            draw.rectangle([x * scale, z * scale, x * scale + scale - 1, z * scale + scale - 1], fill=c)
    return img


def elevation(vox, side="north", scale=6):
    """A flat picture of one side: for each column and height, the nearest block seen from `side`."""
    blocks = {p: s for p, s in vox.b.items() if block_name(s) not in ("air",)}
    x0, y0, z0 = (min(p[i] for p in blocks) for i in range(3))
    x1, y1, z1 = (max(p[i] for p in blocks) for i in range(3))
    near = {}
    for (x, y, z), s in blocks.items():
        if side == "north":
            key, depth = (x - x0, y), z
        elif side == "south":
            key, depth = (x1 - x, y), -z
        elif side == "west":
            key, depth = (z1 - z, y), x
        else:
            key, depth = (z - z0, y), -x
        if key not in near or depth < near[key][0]:
            near[key] = (depth, s)
    width = (x1 - x0 + 1) if side in ("north", "south") else (z1 - z0 + 1)
    img = Image.new("RGB", (width * scale, (y1 - y0 + 1) * scale), (235, 240, 245))
    draw = ImageDraw.Draw(img)
    dmin = min(d for d, _ in near.values())
    for (u, y), (d, s) in near.items():
        f = 1.0 - min(0.5, (d - dmin) * 0.04)
        c = shade(colour(s), f)
        draw.rectangle([u * scale, (y1 - y) * scale, u * scale + scale - 1, (y1 - y) * scale + scale - 1], fill=c)
    return img
