"""Draw Jugcraft's original 16x16 material textures (requires Pillow).

Run from the repository root:  python3 tools/generate_textures.py
Every pixel is generated here from fixed seeds; no Mojang texture is read, traced or
recolored. Colors follow the real minerals: cassiterite is glossy brown-black, tin is
pale cool silver, bronze is warm golden-brown. Metal parts and powders (plates, gears,
wire, dusts, washed ore) are drawn by tools/material_style.py.
"""
import json
import random
from pathlib import Path

from PIL import Image

import block_style as bs
import electric_textures
import material_style as ms

ROOT = Path(__file__).resolve().parents[1]
TEX = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures"

STONE = [(104, 110, 116), (116, 122, 128), (128, 133, 138), (138, 142, 146), (96, 101, 107)]
DEEPSLATE = [(58, 60, 66), (66, 68, 75), (74, 76, 83), (50, 52, 58), (82, 84, 90)]
CASSITERITE = [(34, 22, 18), (54, 34, 24), (78, 52, 36)]
GLINT = (214, 202, 184)
TIN = [(92, 100, 114), (140, 148, 162), (186, 193, 204), (222, 227, 234), (246, 248, 252)]
BRONZE = [(88, 54, 24), (138, 92, 42), (184, 130, 64), (222, 172, 98), (246, 214, 150)]
COPPER = [(150, 74, 44), (196, 108, 66), (226, 142, 96)]

INGOT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "....0000000000..",
    "...044444444430.",
    "..04444444443320",
    ".033333333332210",
    ".022222222221100",
    ".0111111111110..",
    "..00000000000...",
    "................",
    "................",
    "................",
    "................",
]
NUGGET = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......000.......",
    ".....04430......",
    "....0443320.....",
    "....0332210.....",
    ".....01110......",
    "......000.......",
    "................",
    "................",
    "................",
    "................",
]
RAW = [
    "................",
    "................",
    "................",
    "......xxx.......",
    "....xxxxxxx.....",
    "...xxxxxxxxx....",
    "..xxxxxxxxxxx...",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "...xxxxxxxxxxx..",
    "...xxxxxxxxxx...",
    "....xxxxxxxx....",
    "......xxxx......",
    "................",
    "................",
    "................",
]
PILE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......xx.......",
    ".....xxxxxx.....",
    "....xxxxxxxx....",
    "...xxxxxxxxxx...",
    "..xxxxxxxxxxxx..",
    ".xxxxxxxxxxxxxx.",
    ".xxxxxxxxxxxxxx.",
    "..xxxxxxxxxxxx..",
    "................",
    "................",
]


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def save(img, kind, name, scale=1, animation=None):
    """Saves a texture; with animation (the .mcmeta "animation" object), img is a vertical strip of frames."""
    path = TEX / kind / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    if scale != 1:
        img = img.resize((16 * scale, 16 * scale), Image.NEAREST)
    img.save(path, optimize=True)
    if animation is not None:
        path.with_name(path.name + ".mcmeta").write_text(json.dumps({"animation": animation}, indent=2) + "\n",
                                                         encoding="utf-8")


def rock(palette, seed, streaks=False):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        row_bias = rng.choice([0, 0, 1, -1]) if streaks else 0
        for x in range(16):
            i = min(len(palette) - 1, max(0, rng.choice([0, 1, 1, 2, 2, 3]) + row_bias))
            img.putpixel((x, y), palette[i] + (255,))
    return img


def ore(base_palette, seed, streaks=False, specks=None, glint=GLINT):
    specks = specks or CASSITERITE
    img = rock(base_palette, seed, streaks)
    rng = random.Random(seed + 1)
    for cx, cy in [(3, 3), (10, 2), (6, 8), (12, 10), (2, 12), (9, 13)]:
        cells = [(cx, cy), (cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1), (cx + rng.choice([-1, 2]), cy + rng.choice([0, 1]))]
        for x, y in cells:
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), rng.choice(specks) + (255,))
        img.putpixel((cx, cy), glint + (255,))
    return img


def from_mask(mask, palette):
    img = new()
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch.isdigit():
                img.putpixel((x, y), palette[int(ch)] + (255,))
    return img


def raw_chunk(seed, palette=None, glint=GLINT):
    palette = palette or CASSITERITE
    rng = random.Random(seed)
    img = new()
    for y, row in enumerate(RAW):
        for x, ch in enumerate(row):
            if ch == "x":
                shade = 2 if (x + y) < 12 else (1 if (x + y) < 20 else 0)
                img.putpixel((x, y), palette[max(0, shade - rng.choice([0, 0, 1]))] + (255,))
    for x, y in [(6, 5), (9, 4), (4, 7), (8, 8), (11, 6)]:
        img.putpixel((x, y), glint + (255,))
    return img


def lump(palette, glint):
    """A raw lump (bitumen, silicon, coke, borax, ferroboron): the lumpy chunk of tools/material_style.py in the
    palette's tones, darkest first, with `glint` as its highlight."""
    tones = sorted(palette, key=bs._luma)
    return ms.raw_item((tones + tones[-1:] * 3)[:3] + [glint])


def ramp5(palette):
    """Five tones, darkest first, from a palette of three or more: its own tones sorted by lightness (the two closest
    in lightness merged when there are more than four) under one darker tone for outlines."""
    tones = sorted(palette, key=bs._luma)
    while len(tones) > 4:
        gaps = [bs._luma(tones[i + 1]) - bs._luma(tones[i]) for i in range(len(tones) - 1)]
        i = gaps.index(min(gaps))
        tones[i:i + 2] = [bs._lerp(tones[i], tones[i + 1], 0.5)]
    if len(tones) == 3:
        tones.insert(1, bs._lerp(tones[0], tones[1], 0.5))
    return [tuple(int(v * 0.72) for v in tones[0])] + tones


def raw_block(seed, palette=None, glint=GLINT):
    palette = palette or CASSITERITE
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), rng.choice(palette + [palette[1]]) + (255,))
    for _ in range(9):
        img.putpixel((rng.randrange(16), rng.randrange(16)), glint + (255,))
    return img


def metal_block(palette, seed):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            bevel_light = x == 1 or y == 1
            bevel_dark = x == 14 or y == 14
            if edge:
                c = palette[0]
            elif bevel_light:
                c = palette[4]
            elif bevel_dark:
                c = palette[1]
            else:
                c = palette[rng.choice([2, 3, 3, 3])]
            img.putpixel((x, y), c + (255,))
    for x, y in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        img.putpixel((x, y), palette[1] + (255,))
    return img


def pile(seed, palette):
    """A heap of powder or grains in `palette` (tools/material_style.py's clean heap). `seed` is kept for the
    callers."""
    return ms.dust(ramp5(palette))


def blend(seed):
    """Bronze blend: a heap of copper powder with small clumps of tin powder through it. `seed` is kept for the
    caller."""
    return ms.blend(COPPER_METAL, TIN)


# name: (ore specks, glint, metal palette dark->light). Colors follow the real ore minerals.
METAL_COLORS = {
    "zinc": ([(120, 74, 28), (168, 108, 40), (200, 150, 70)], (240, 210, 140),
             [(70, 80, 94), (110, 122, 138), (156, 168, 184), (198, 208, 222), (230, 236, 246)]),
    "lead": ([(92, 96, 108), (130, 134, 146), (170, 174, 186)], (222, 226, 238),
             [(40, 44, 58), (64, 70, 86), (90, 96, 114), (116, 122, 140), (146, 152, 170)]),
    "silver": ([(196, 200, 206), (226, 228, 232), (250, 250, 252)], (255, 255, 255),
               [(112, 116, 126), (170, 174, 184), (210, 214, 222), (236, 238, 244), (255, 255, 255)]),
    "nickel": ([(130, 110, 50), (170, 150, 76), (206, 188, 110)], (242, 230, 172),
               [(94, 92, 84), (148, 144, 130), (190, 186, 170), (220, 216, 200), (244, 242, 230)]),
    "tungsten": ([(30, 28, 30), (52, 48, 46), (78, 72, 66)], (164, 154, 144),
                 [(38, 40, 44), (64, 68, 74), (92, 96, 104), (120, 124, 132), (156, 160, 168)]),
    "uranium": ([(24, 24, 20), (40, 40, 30), (186, 208, 58)], (232, 242, 122),
                [(66, 70, 60), (106, 112, 96), (146, 152, 132), (184, 190, 168), (214, 220, 196)]),
    # Rutile: reddish-brown to black needles in the stone; the metal a cool blue-grey.
    "titanium": ([(70, 30, 20), (120, 54, 30), (168, 86, 44)], (226, 150, 92),
                 [(58, 62, 72), (96, 102, 116), (138, 144, 158), (178, 184, 196), (214, 218, 228)]),
    "aluminum": (None, None,
                 [(118, 124, 130), (166, 172, 178), (198, 202, 208), (220, 224, 228), (238, 240, 244)]),
}

# Alloys have their own seed range so adding them never changes existing textures.
# Brass is bright yellow-gold, invar a cool pale gray, solder a dull lead-tin gray.
ALLOY_COLORS = {
    "brass": (None, None,
              [(120, 88, 24), (176, 138, 40), (214, 180, 64), (236, 210, 108), (250, 236, 170)]),
    "invar": (None, None,
              [(84, 88, 92), (128, 132, 136), (166, 170, 172), (196, 198, 200), (222, 224, 226)]),
    "solder": (None, None,
               [(70, 72, 80), (104, 106, 114), (138, 140, 148), (166, 168, 176), (196, 198, 206)]),
    # Steel: a dark blue-gray, darker and cooler than iron.
    "steel": (None, None,
              [(44, 48, 56), (72, 78, 88), (102, 108, 120), (136, 142, 154), (176, 182, 194)]),
}

# name: (ore specks, glint, item/block palette)
MINERAL_COLORS = {
    "salt": ([(236, 228, 228), (246, 238, 240), (226, 196, 200)], (255, 255, 255),
             [(214, 206, 208), (234, 228, 230), (248, 244, 246), (232, 200, 204)]),
    "phosphate": ([(88, 78, 64), (110, 98, 80), (72, 150, 140)], (170, 220, 206),
                  [(84, 74, 60), (108, 96, 78), (132, 120, 100), (96, 150, 140)]),
    "lepidolite": ([(150, 110, 170), (186, 146, 204), (212, 182, 226)], (244, 228, 250),
                   [(140, 100, 160), (176, 136, 196), (206, 174, 222), (230, 210, 240)]),
    "monazite": ([(150, 90, 40), (186, 120, 56), (212, 156, 82)], (246, 214, 150),
                 [(140, 84, 36), (178, 116, 52), (206, 150, 78), (226, 180, 110)]),
}

SULFUR = [(150, 126, 18), (200, 176, 30), (230, 208, 58), (244, 228, 104), (252, 244, 168)]

BAUXITE = [(126, 58, 36), (150, 72, 44), (170, 88, 54), (188, 108, 68), (112, 50, 32)]
OIL_SAND_BASE = [(176, 152, 108), (190, 166, 120), (160, 136, 94), (146, 124, 86), (132, 110, 76)]
BITUMEN = [(14, 12, 12), (28, 24, 22), (44, 40, 36)]


def speckled(base, seed, specks, count=18):
    """Speckled rock in the manner of vanilla granite or diorite: a clumped base of `base` with about `count` small
    specks of `specks` spread evenly, never touching."""
    return bs.img(bs.speckled(sorted(base, key=bs._luma), specks, seed, density=count / 256))


def main_extra():
    # The metals' ingots, nuggets and storage blocks, their ores, raw ores and raw blocks, and the minerals' ores are the
    # material sets (tools/material_icons.py, docs/MATERIAL_SETS.md). The metals kept seeds 110-190, so the minerals'
    # blocks and items below keep theirs.
    import material_icons
    material_icons.draw_materials(save)
    seed = 100 + 10 * len(METAL_COLORS)
    for mineral, (specks, glint, palette) in MINERAL_COLORS.items():
        seed += 10
        save(rock(palette, seed + 2), "block", f"{mineral}_block")
        save(pile(seed + 3, palette), "item", mineral)

    save(speckled(BAUXITE, 300, [(214, 170, 130), (226, 190, 150)]), "block", "bauxite")
    save(speckled(OIL_SAND_BASE, 301, BITUMEN, count=40), "block", "oil_sand")
    save(lump(BITUMEN, (120, 116, 110)), "item", "bitumen")
    save(ms.dust(SULFUR), "item", "sulfur_dust")
    save(lump([(46, 54, 72), (70, 80, 102), (100, 112, 138)], (190, 206, 236)), "item", "silicon")
    save(pile(305, [(236, 236, 240), (248, 248, 250), (222, 224, 230)]), "item", "lithium_carbonate")
    # Rare earth oxides: a cream heap with grains of the pastel oxides through it.
    oxide = ms.dust(ramp5([(214, 204, 186), (232, 222, 204), (244, 236, 220), (252, 248, 238)]))
    for (x, y), c in zip(sorted(ms.GRAINS), [(232, 196, 210), (196, 224, 196), (214, 206, 232)] * 3):
        oxide.putpixel((x, y), c + (255,))
    save(oxide, "item", "rare_earth_oxide")


STEEL = [(62, 66, 72), (86, 90, 97), (110, 114, 121), (134, 138, 145), (160, 164, 170)]


def panel(seed, palette=STEEL, trim=BRONZE):
    """A riveted machine panel: a bronze frame, lit along its top and left and shaded along its bottom and right,
    round a recessed steel plate. The plate is one ground tone with soft clumps a tone either side, in shadow under
    the frame's top and left lip and catching the light along its bottom and right (docs/ART_DIRECTION.md)."""
    img = new()
    g = bs.grain(16, 16, seed, 3.0, 6.0, 0.5)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = trim[1]
            elif x == 1 or y == 1:
                c = trim[3]
            elif x == 14 or y == 14:
                c = trim[0]
            elif x == 2 or y == 2:
                c = palette[1]
            elif x == 13 or y == 13:
                c = palette[3]
            else:
                v = g(x, y)
                c = palette[3] if v > 0.75 else palette[1] if v < 0.25 else palette[2]
            img.putpixel((x, y), c + (255,))
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        img.putpixel((x, y), trim[4] + (255,))
    return img


def window(seed, inner, glow=None):
    """A machine front with a recessed window, its top and left inner walls in shadow and its bottom and right ones
    catching the light. Behind it the inner colours lie in soft clumps; when running, the glow colours rise in smooth
    bands to the brightest at the bottom, like the fire in a furnace."""
    img = panel(seed)
    for y in range(4, 12):
        for x in range(4, 12):
            if y == 4 or x == 4:
                img.putpixel((x, y), STEEL[0] + (255,))
            elif y == 11 or x == 11:
                img.putpixel((x, y), STEEL[3] + (255,))
    if glow:
        tones = sorted(glow, key=bs._luma)
        g = bs.grain(16, 16, seed + 1, 2.0, 4.0, 0.5)
        for y in range(5, 11):
            for x in range(5, 11):
                k = int(round((y - 5) / 5 * (len(tones) - 1) + (g(x, y) - 0.5) * 1.6))
                img.putpixel((x, y), tones[max(0, min(len(tones) - 1, k))] + (255,))
    else:
        s = bs.surface(inner, seed + 1, spread=0.7)
        for y in range(5, 11):
            for x in range(5, 11):
                img.putpixel((x, y), s(x, y) + (255,))
    return img


def grate(seed, glow=None):
    img = window(seed, [(24, 22, 22), (34, 30, 28)], glow)
    for y in (6, 8, 10):
        for x in range(5, 11):
            img.putpixel((x, y), STEEL[1] + (255,))
    return img


def jaws(seed, active):
    """Crusher jaws behind the window: a row of teeth above meeting a row below, crumbs between them when running."""
    img = window(seed, [(40, 40, 44), (52, 52, 58)])
    tooth = STEEL[4] if active else STEEL[3]
    for x in range(5, 11):
        img.putpixel((x, 5), tooth + (255,))
        img.putpixel((x, 10), STEEL[2] + (255,))
    for x in (5, 7, 9):
        img.putpixel((x, 6), tooth + (255,))
    for x in (6, 8, 10):
        img.putpixel((x, 9), STEEL[2] + (255,))
    if active:
        for x, y in [(6, 7), (8, 8), (9, 7)]:
            img.putpixel((x, y), (190, 180, 160, 255))
    return img


def pulverizer_front(seed, active):
    """Two grinding wheels side by side, dusted when running."""
    img = window(seed, [(40, 40, 44), (50, 50, 56)])
    for cx in (6, 9):
        for y in range(5, 11):
            img.putpixel((cx, y), (STEEL[4] if (y + (active and 1)) % 2 else STEEL[2]) + (255,))
    if active:
        for x, y in [(7, 9), (8, 10), (7, 6), (8, 5)]:
            img.putpixel((x, y), (200, 190, 170, 255))
    return img


def washer_front(seed, active):
    """A water drum window; the water ripples when running."""
    water = [(40, 90, 170), (52, 110, 196)] if active else [(30, 60, 110), (36, 70, 124)]
    img = window(seed, water)
    for x in range(5, 11):
        img.putpixel((x, 6 + (x % 2 if active else 0)), (150, 200, 240, 255))
    return img


def sieve_front(seed, active):
    """A fine mesh of crossing wires; grains fall through when running."""
    img = window(seed, [(34, 30, 28), (42, 38, 34)])
    for y in range(5, 11):
        for x in range(5, 11):
            if x in (6, 8, 10) or y in (6, 8, 10):
                img.putpixel((x, y), (STEEL[3] if x + y < 15 else STEEL[2]) + (255,))
    if active:
        for x, y in [(7, 7), (9, 9), (7, 9)]:
            img.putpixel((x, y), (190, 170, 120, 255))
    return img


def sawmill_front(seed, active):
    """A circular saw blade behind the window."""
    img = window(seed, [(40, 36, 32), (48, 44, 40)])
    teeth = STEEL[4] if active else STEEL[3]
    for x, y in [(7, 5), (8, 5), (10, 7), (10, 8), (7, 10), (8, 10), (5, 7), (5, 8), (6, 6), (9, 6), (6, 9), (9, 9)]:
        img.putpixel((x, y), teeth + (255,))
    for x, y in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        img.putpixel((x, y), BRONZE[3] + (255,))
    return img


def battery_front(seed):
    img = panel(seed, palette=[(40, 44, 58), (64, 70, 86), (90, 96, 114), (116, 122, 140), (146, 152, 170)])
    red, black = (200, 40, 40), (30, 30, 34)
    for x in range(4, 7):
        img.putpixel((x, 7), red + (255,))
    img.putpixel((5, 6), red + (255,))
    img.putpixel((5, 8), red + (255,))
    for x in range(9, 12):
        img.putpixel((x, 7), black + (255,))
    return img


def bricks(seed):
    """Pale firebricks, one tone a brick, lit along each brick's top edge, with grey mortar."""
    rng = random.Random(seed)
    img = new()
    brick = [(196, 176, 150), (210, 192, 166), (182, 160, 134)]
    mortar = (120, 110, 100)
    tones = {}
    for y in range(16):
        for x in range(16):
            row = y // 4
            offset = 4 if row % 2 else 0
            if y % 4 == 3 or (x + offset) % 8 == 7:
                c = mortar
            else:
                key = (row, (x + offset) // 8)
                c = tones.setdefault(key, brick[rng.randrange(3)])
                if y % 4 == 0:
                    c = tuple(min(255, v + 12) for v in c)
            img.putpixel((x, y), c + (255,))
    return img


def _transmitter(palette, node, ring):
    """Texture for a 4-px transmitter. Its arms read the bands at rows/columns 6-9 (the cable
    model's UVs), shaded lengthwise like a round cross-section; the 6-9 center square is the
    junction node seen on the core; ring colors the outer ends where the arm meets a neighbor."""
    img = new()
    shade = {6: palette[3], 7: palette[2], 8: palette[1], 9: palette[0]}
    for y in range(16):
        for x in range(16):
            in_x, in_y = 6 <= x <= 9, 6 <= y <= 9
            if in_x and in_y:
                c = node(x, y)
            elif in_y:
                c = ring[y - 6] if x in (0, 15) else shade[y]
            elif in_x:
                c = ring[x - 6] if y in (0, 15) else shade[x]
            else:
                c = palette[1]
            img.putpixel((x, y), c + (255,))
    return img


RUBBER = [(22, 20, 20), (36, 34, 33), (52, 50, 48), (74, 72, 70)]


def item_tube_texture():
    """6-pixel brass pneumatic tube: bands at rows/columns 5-10 with a glass window down the middle,
    a riveted junction node in the centre square and darker flanges at the ends."""
    from steampunk_textures import BRASS as SP_BRASS
    img = new()
    band = {5: SP_BRASS[0], 6: SP_BRASS[3], 7: (170, 214, 226), 8: (120, 176, 204), 9: SP_BRASS[2], 10: SP_BRASS[0]}
    for y in range(16):
        for x in range(16):
            in_x, in_y = 5 <= x <= 10, 5 <= y <= 10
            if in_x and in_y:
                edge = x in (5, 10) or y in (5, 10)
                c = SP_BRASS[1] if edge else (SP_BRASS[4] if (x, y) in ((6, 6), (9, 6), (6, 9), (9, 9)) else SP_BRASS[3])
            elif in_y:
                c = SP_BRASS[0] if x in (0, 15) else band[y]
            elif in_x:
                c = SP_BRASS[0] if y in (0, 15) else band[x]
            else:
                c = SP_BRASS[2]
            img.putpixel((x, y), c + (255,))
    return img


def wrench_item():
    """Brass wrench lying diagonally, open jaw at the top right."""
    from steampunk_textures import BRASS as SP_BRASS
    img = new()
    for i in range(3, 12):
        x, y = i, 15 - i
        for dx, dy, c in ((0, 0, SP_BRASS[3]), (1, 0, SP_BRASS[1]), (0, -1, SP_BRASS[4])):
            img.putpixel((x + dx, y + dy), c + (255,))
    jaw = [(11, 1), (12, 1), (13, 1), (14, 2), (14, 3), (14, 4), (13, 5), (12, 5), (10, 2), (10, 3), (11, 4)]
    for x, y in jaw:
        img.putpixel((x, y), SP_BRASS[2] + (255,))
    for x, y in ((12, 2), (12, 3), (13, 3)):
        img.putpixel((x, y), (0, 0, 0, 0))
    img.putpixel((3, 12), SP_BRASS[0] + (255,))
    img.putpixel((2, 13), SP_BRASS[1] + (255,))
    return img


def handbook_item():
    """A leather-bound book with brass corners and a small gear on the cover."""
    from steampunk_textures import BRASS as SP_BRASS
    leather = [(70, 36, 22), (92, 50, 30), (112, 64, 38)]
    img = new()
    for y in range(2, 15):
        for x in range(3, 14):
            c = leather[1] if (x + 2 * y) % 7 else leather[2]
            if x == 3:
                c = leather[0]
            img.putpixel((x, y), c + (255,))
    for y in range(3, 14):
        img.putpixel((13, y), (230, 220, 190, 255))
    for x, y in ((3, 2), (4, 2), (3, 3), (12, 2), (3, 14), (4, 14), (3, 13), (12, 14)):
        img.putpixel((x, y), SP_BRASS[3] + (255,))
    for x, y in ((8, 6), (7, 7), (9, 7), (6, 8), (10, 8), (7, 9), (9, 9), (8, 10), (8, 8)):
        img.putpixel((x, y), (SP_BRASS[4] if (x, y) != (8, 8) else leather[0]) + (255,))
    return img


def prospector_item():
    """A brass hand instrument: amber screen with a signal trace, a knob and a copper aerial."""
    from steampunk_textures import BRASS as SP_BRASS
    img = new()
    for y in range(5, 15):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (5, 14)
            img.putpixel((x, y), (SP_BRASS[1] if edge else SP_BRASS[3]) + (255,))
    for y in range(7, 11):
        for x in range(5, 11):
            img.putpixel((x, y), (28, 20, 8, 255))
    for x, y in ((5, 10), (6, 9), (7, 8), (8, 9), (9, 7), (10, 8)):
        img.putpixel((x, y), (255, 179, 64, 255))
    for x, y in ((5, 12), (6, 12), (10, 12)):
        img.putpixel((x, y), SP_BRASS[0] + (255,))
    img.putpixel((9, 12), (176, 32, 24, 255))
    for i in range(0, 5):
        img.putpixel((11 + i // 2, 4 - i), (184, 104, 60, 255))
    img.putpixel((13, 0), (255, 179, 64, 255))
    img.putpixel((3, 5), SP_BRASS[4] + (255,))
    return img


def shaft_frame(shift):
    """Iron shaft with diagonal brass-and-dark bands; shifting the bands one pixel per frame makes it turn."""
    from steampunk_textures import BRASS as SP_BRASS
    img = new()
    for y in range(16):
        for x in range(16):
            phase = (x + y + shift) % 8
            c = SP_BRASS[3] if phase == 0 else SP_BRASS[2] if phase == 1 else IRON_METAL[1] if phase < 5 else IRON_METAL[2]
            img.putpixel((x, y), c + (255,))
    return img


def gearbox_frame(angle):
    """Brass plate with a riveted rim and an eight-toothed iron gear turned by `angle` degrees."""
    import math
    from steampunk_textures import BRASS as SP_BRASS
    img = new()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), (SP_BRASS[1] if edge else SP_BRASS[3]) + (255,))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        img.putpixel((x, y), SP_BRASS[0] + (255,))
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy)
            a = (math.degrees(math.atan2(dy, dx)) - angle) % 45
            tooth = a < 18
            if r <= 4.2 or (r <= 6.3 and tooth):
                c = IRON_METAL[1] if r > 5 else IRON_METAL[2]
                if r <= 1.5:
                    c = SP_BRASS[4]
                img.putpixel((x, y), c + (255,))
    return img


def save_animation(frames, name, frametime=2):
    """A vertical strip of 16x16 frames plus its .mcmeta, so the texture animates in game."""
    strip = Image.new("RGBA", (16, 16 * len(frames)), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        strip.paste(frame, (0, 16 * index))
    path = TEX / "block" / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    strip.save(path, optimize=True)
    (TEX / "block" / f"{name}.png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": frametime}}, indent=2) + "\n", encoding="utf-8")


def belt_texture():
    """A dark leather belt: brown in soft clumps with a lighter stitch line along each edge (length runs down the
    texture)."""
    img = new()
    g = bs.grain(16, 16, 970, 2.0, 5.0, 0.5)
    for y in range(16):
        for x in range(16):
            c = (86, 54, 32) if g(x, y) > 0.66 else (74, 46, 28)
            if x in (1, 14):
                c = (168, 136, 96) if y % 3 else (74, 46, 28)
            if x in (0, 15):
                c = (52, 32, 20)
            img.putpixel((x, y), c + (255,))
    return img


def conveyor_frame(shift):
    """Rubberised conveyor belt, seen from above with the front (where items go) at the top: dark rubber with
    chevron ribs pointing forwards. Shifting the ribs two pixels a frame makes them run at the items' speed."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = (38, 36, 34)
            phase = (y - int(abs(x - 7.5) / 2) + shift) % 8
            if phase == 0:
                c = (78, 74, 68)
            elif phase == 1:
                c = (58, 55, 51)
            if x in (0, 15):
                c = (26, 24, 22)
            img.putpixel((x, y), c + (255,))
    return img


def rocket_pack_armor():
    """The worn rocket pack's harness, on the 64x32 humanoid armor layout: rubber straps and a chrome buckle on the
    front, back, sides and top (the tanks are a 3D model on the back, drawn by client/RocketPackLayer)."""
    from dieselpunk_textures import CHROME, RUBBER
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def px(x, y, c):
        img.putpixel((x, y), tuple(c) + (255,))
    # Straps down the back (the pack itself is drawn in 3D by client/RocketPackLayer).
    for y in range(20, 32):
        for x in (33, 38):
            px(x, y, RUBBER[2] if y % 2 else RUBBER[1])
    # Straps: down the front (x 20..27), over the shoulders (top, x 20..27, y 16..19) and down the sides.
    for y in range(20, 32):
        for x in (21, 26):
            px(x, y, RUBBER[2] if y % 2 else RUBBER[1])
    for x in range(21, 27):
        px(x, 24, CHROME[2])
    px(23, 24, CHROME[4])
    px(24, 24, CHROME[4])
    for y in range(16, 20):
        for x in (21, 26):
            px(x, y, RUBBER[1])
    for y in range(20, 32):
        px(17, y, RUBBER[1])
        px(30, y, RUBBER[1])
    return img


def belt_item():
    """A coiled leather belt with a brass buckle."""
    from steampunk_textures import BRASS as SP_BRASS
    import math
    img = new()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 8.5)
            if 3 <= r <= 6.5:
                shade = (86, 54, 32) if int(r) % 2 else (70, 44, 26)
                img.putpixel((x, y), shade + (255,))
    for x, y in ((12, 3), (13, 3), (14, 3), (12, 4), (14, 4), (12, 5), (13, 5), (14, 5)):
        img.putpixel((x, y), SP_BRASS[3] + (255,))
    return img


def upgrade_card(accent, symbol):
    """A brass-framed punch card with a colored accent and a small symbol (speed: arrow, efficiency: leaf)."""
    from steampunk_textures import BRASS as SP_BRASS
    img = new()
    for y in range(3, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (3, 13)
            c = SP_BRASS[1] if edge else (SP_BRASS[3] if (x + y) % 5 else SP_BRASS[4])
            img.putpixel((x, y), c + (255,))
    for x in range(4, 12):
        img.putpixel((x, 5), accent[0] + (255,))
    for x, y in symbol:
        img.putpixel((x, y), accent[1] + (255,))
    for x, y in ((4, 11), (6, 11), (8, 11), (10, 11)):
        img.putpixel((x, y), (40, 30, 20, 255))
    return img


def module_card(lamp, glyph):
    """A dieselpunk upgrade module: a gunmetal cartridge with a chrome rim and a hazard-striped grip, a green
    circuit window showing a glyph for what it does, and an indicator lamp."""
    from dieselpunk_textures import CHROME, GUNMETAL, HAZARD, PHOSPHOR
    img = new()
    for y in range(2, 15):
        for x in range(3, 13):
            rim = x in (3, 12) or y in (2, 14)
            c = CHROME[2] if rim else GUNMETAL[2] if (x + y) % 4 else GUNMETAL[3]
            if y in (12, 13) and not rim:
                c = HAZARD[0] if (x + y) % 4 < 2 else HAZARD[2]
            img.putpixel((x, y), c + (255,))
    for y in range(4, 11):
        for x in range(5, 11):
            img.putpixel((x, y), PHOSPHOR[0] + (255,))
    for x, y in glyph:
        img.putpixel((x, y), PHOSPHOR[3] + (255,))
    img.putpixel((11, 3), lamp + (255,))
    img.putpixel((4, 3), CHROME[4] + (255,))
    return img


def pipe_texture():
    """Bronze pipe with a riveted junction node and darker flanges at connections."""
    def node(x, y):
        if (x, y) in ((6, 6), (9, 6), (6, 9), (9, 9)):
            return BRONZE[4]
        edge = x in (6, 9) or y in (6, 9)
        return BRONZE[1] if edge else BRONZE[3]
    return _transmitter([BRONZE[0], BRONZE[1], BRONZE[2], BRONZE[4]], node,
                        [BRONZE[0], BRONZE[0], BRONZE[0], BRONZE[0]])


def steel_pipe_texture():
    """Gunmetal pipe with a hazard-striped junction collar and chrome flanges at connections (dieselpunk)."""
    from dieselpunk_textures import CHROME, GUNMETAL, HAZARD
    def node(x, y):
        if x in (6, 9) and y in (6, 9):
            return CHROME[3]
        return HAZARD[0] if x in (6, 9) or y in (6, 9) else GUNMETAL[2]
    return _transmitter([GUNMETAL[1], GUNMETAL[2], GUNMETAL[3], GUNMETAL[4]], node,
                        [HAZARD[0], HAZARD[2], HAZARD[0], HAZARD[2]])


def tank_side(seed):
    """Tinplate walls around a vertical glass gauge."""
    img = panel(seed, palette=TIN, trim=TIN)
    for y in range(2, 14):
        for x in range(6, 10):
            edge = x in (6, 9)
            img.putpixel((x, y), (TIN[0] if edge else (170, 205, 225) if y < 5 else (70, 110, 170)) + (255,))
    return img


def tank_cap(seed, hatch):
    img = panel(seed, palette=TIN, trim=TIN)
    if hatch:
        for y in range(5, 11):
            for x in range(5, 11):
                ring = x in (5, 10) or y in (5, 10)
                img.putpixel((x, y), (TIN[0] if ring else TIN[4]) + (255,))
    return img


def pump_side(seed):
    """Machine panel with a bronze impeller housing."""
    img = panel(seed)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r2 = dx * dx + dy * dy
            if 9 <= r2 <= 20:
                img.putpixel((x, y), BRONZE[1 if dx + dy > 0 else 3] + (255,))
            elif r2 < 9:
                img.putpixel((x, y), (BRONZE[2] if abs(dx - dy) < 1.2 or abs(dx + dy) < 1.2 else STEEL[0]) + (255,))
    return img


def pump_port(seed, intake):
    img = panel(seed)
    for y in range(4, 12):
        for x in range(4, 12):
            ring = x in (4, 11) or y in (4, 11)
            img.putpixel((x, y), (BRONZE[1] if ring else (20, 22, 26) if intake else STEEL[0]) + (255,))
    return img


def hazard_plinth(seed):
    """Dark steel footing in soft clumps with a yellow-black hazard edge."""
    img = new()
    s = bs.surface(STEEL[:3], seed, weights=[1, 3, 1], spread=0.7)
    for y in range(16):
        for x in range(16):
            if y in (0, 15) or x in (0, 15):
                c = (214, 170, 40) if (x + y) // 2 % 2 else (30, 28, 26)
            else:
                c = s(x, y)
            img.putpixel((x, y), c + (255,))
    return img


def geothermal_front(seed, lit):
    """Vented front: steel slats over a heat exchanger that glows in soft clumps when running."""
    img = panel(seed)
    glow = [(255, 120, 20), (255, 170, 40), (230, 80, 10)] if lit else [(60, 30, 24), (74, 36, 28)]
    s = bs.surface(glow, seed + 1, spread=0.7)
    for y in range(3, 13):
        for x in range(3, 13):
            img.putpixel((x, y), (STEEL[0] if y % 2 == 0 else s(x, y)) + (255,))
    return img


def geothermal_tank(seed):
    """Tinted steel shell with a narrow window of lava."""
    img = new()
    shell = bs.surface(STEEL[:3], seed, weights=[1, 3, 1], spread=0.6)
    lava = bs.surface([(230, 90, 20), (250, 140, 30), (200, 60, 10)], seed + 1, spread=0.8)
    for y in range(16):
        for x in range(16):
            if 6 <= x <= 9 and 2 <= y <= 13:
                c = STEEL[0] if x in (6, 9) else lava(x, y)
            else:
                c = shell(x, y)
            img.putpixel((x, y), c + (255,))
    return img


def stack(seed):
    """Chimney stack: steel rings with a dark seam every four pixels."""
    img = new()
    s = bs.surface([STEEL[1], STEEL[2]], seed, spread=0.6)
    for y in range(16):
        for x in range(16):
            c = STEEL[0] if y % 4 == 0 else STEEL[3] if y % 4 == 1 else s(x, y)
            img.putpixel((x, y), c + (255,))
    return img


def lattice(seed):
    """Mast: steel with a climbing-rung pattern."""
    img = new()
    for y in range(16):
        for x in range(16):
            rung = y % 4 == 0
            edge = x % 8 in (0, 7)
            c = STEEL[3] if rung else (STEEL[1] if edge else STEEL[2])
            img.putpixel((x, y), c + (255,))
    return img


def nacelle(seed):
    """White fibreglass housing, a shade darker along its top seam, with a few soft clumps."""
    img = new()
    g = bs.grain(16, 16, seed, 3.0, 6.0, 0.5)
    for y in range(16):
        for x in range(16):
            c = (180, 184, 190) if y <= 1 else (206, 208, 212) if g(x, y) < 0.3 else (224, 226, 228)
            img.putpixel((x, y), c + (255,))
    return img


def blade(seed):
    """White blade skin with one soft sheen running along it."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = (246, 247, 248) if abs(x - y) <= 1 else (238, 240, 242) if x + y < 22 else (226, 230, 234)
            img.putpixel((x, y), c + (255,))
    return img


def blade_tip():
    """Red warning paint on the blade tip, lit along one sheen."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = (226, 74, 62) if abs(x - y) <= 1 else (206, 56, 46) if x + y < 22 else (184, 46, 38)
            img.putpixel((x, y), c + (255,))
    return img


def turbine_front(seed):
    """Base housing door with a small grille."""
    img = panel(seed)
    for y in range(3, 15):
        for x in range(5, 11):
            edge = x in (5, 10) or y == 3
            img.putpixel((x, y), (STEEL[0] if edge else STEEL[3]) + (255,))
    for y in (5, 7, 9):
        for x in range(6, 10):
            img.putpixel((x, y), (20, 22, 26, 255))
    img.putpixel((9, 11), BRONZE[4] + (255,))
    return img


def firebrick(seed):
    """Small red-brown refractory bricks, one tone a brick, lit along each brick's top edge."""
    rng = random.Random(seed)
    img = new()
    brick = [(142, 62, 40), (158, 72, 46), (126, 54, 36)]
    tones = {}
    for y in range(16):
        for x in range(16):
            offset = 2 if (y // 3) % 2 else 0
            if y % 3 == 2 or (x + offset) % 4 == 3:
                c = (84, 72, 64)
            else:
                c = tones.setdefault((y // 3, (x + offset) // 4), brick[rng.randrange(3)])
                if y % 3 == 0:
                    c = tuple(min(255, v + 14) for v in c)
            img.putpixel((x, y), c + (255,))
    return img


def crucible(seed):
    """Dark cast iron in soft clumps with two glowing seams, for the alloy crucible."""
    img = new()
    s = bs.surface([(38, 36, 38), (46, 44, 46), (56, 54, 56)], seed, spread=0.7)
    for y in range(16):
        for x in range(16):
            c = s(x, y)
            if y in (4, 11) and 2 <= x <= 13:
                c = (255, 190, 80) if 5 <= x <= 10 else (240, 130, 40)
            img.putpixel((x, y), c + (255,))
    return img


def hopper_side(seed):
    img = panel(seed)
    for y in range(4, 12):
        for x in range(4, 12):
            img.putpixel((x, y), STEEL[0] + (255,))
    return img


def hopper_top():
    img = new()
    for y in range(16):
        for x in range(16):
            rim = x in (0, 1, 14, 15) or y in (0, 1, 14, 15)
            img.putpixel((x, y), (STEEL[2] if rim else (22, 22, 26)) + (255,))
    return img


def power_port():
    """Copper socket face: three pins on dark insulation, so the cable entry is obvious."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = (30, 28, 28)
            if x in (0, 15) or y in (0, 15):
                c = COPPER[0]
            elif x in (4, 8, 11) and 4 <= y <= 11:
                c = COPPER[2]
            img.putpixel((x, y), c + (255,))
    return img


def power_port_frame():
    """Yellow-and-black frame around the socket, like a warning plate."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = (214, 170, 40) if (x + y) // 3 % 2 else (30, 28, 26)
            if 3 <= x <= 12 and 3 <= y <= 12:
                c = COPPER[1] if (x + y) % 2 else COPPER[0]
            img.putpixel((x, y), c + (255,))
    return img


def gui():
    """Generic 176x166 machine screen: beveled panel and player inventory slots."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    light, face, dark, slot_bg = (255, 255, 255), (198, 198, 198), (85, 85, 85), (139, 139, 139)
    for y in range(166):
        for x in range(176):
            c = face
            if x < 2 or y < 2:
                c = light
            elif x > 173 or y > 163:
                c = dark
            img.putpixel((x, y), c + (255,))

    def slot(sx, sy):
        for y in range(18):
            for x in range(18):
                c = slot_bg
                if x == 0 or y == 0:
                    c = dark
                elif x == 17 or y == 17:
                    c = light
                img.putpixel((sx + x, sy + y), c + (255,))

    for row in range(3):
        for col in range(9):
            slot(7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(7 + col * 18, 141)
    img.save(TEX / "gui" / "machine.png", optimize=True)
    # The themed machine screens (batch 22).
    import gui_textures
    gui_textures.draw_all(lambda image, name: image.save(TEX / "gui" / f"{name}.png", optimize=True))


def solar_top():
    """Four blue photovoltaic cells in a bronze frame, split by silver bus bars, each lit at its top left."""
    img = new()
    cell = [(22, 34, 78), (28, 44, 96), (34, 54, 112)]
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = BRONZE[1]
            elif x in (5, 10) or y in (5, 10):
                c = (176, 182, 190)
            else:
                cx, cy = (x - 1) % 5, (y - 1) % 5
                c = cell[2] if cx + cy <= 1 else cell[0] if cx + cy >= 6 else cell[1]
            img.putpixel((x, y), c + (255,))
    return img


def boiler(seed, lit):
    """A round bronze boiler end, lit at its upper left, with a pressure gauge and a firebox slot below."""
    img = panel(seed)
    for y in range(3, 13):
        for x in range(3, 13):
            dx, dy = x - 7.5, y - 7.5
            if dx * dx + dy * dy <= 20:
                c = BRONZE[3] if dx + dy < -2.5 else BRONZE[1] if dx + dy > 3 else BRONZE[2]
                img.putpixel((x, y), c + (255,))
    gauge = (238, 238, 230)
    for x, y in [(6, 5), (7, 5), (8, 5), (9, 5), (6, 6), (9, 6)]:
        img.putpixel((x, y), gauge + (255,))
    needle = (200, 40, 30) if lit else (60, 60, 60)
    img.putpixel((8, 6) if lit else (7, 6), needle + (255,))
    for x in range(5, 11):
        img.putpixel((x, 11), ((250, 140, 30) if lit else (40, 34, 30)) + (255,))
    return img


def crucibles(seed, lit):
    img = panel(seed)
    for cx in (4, 9):
        for y in range(5, 12):
            for x in range(cx, cx + 4):
                edge = x in (cx, cx + 3) or y == 11
                if edge:
                    c = STEEL[0]
                elif lit and y >= 7:
                    c = (230, 110, 30) if y <= 8 else (250, 150, 40) if y == 9 else (255, 200, 80)
                else:
                    c = (40, 36, 34)
                img.putpixel((x, y), c + (255,))
    return img


# Original palettes for the vanilla metals that get Jugcraft parts (not taken from vanilla textures).
COPPER_METAL = [(110, 52, 30), (156, 78, 46), (196, 108, 66), (226, 142, 96), (244, 184, 140)]
IRON_METAL = [(88, 88, 92), (130, 130, 136), (170, 170, 176), (204, 204, 208), (232, 232, 236)]
GOLD_METAL = [(120, 84, 14), (186, 140, 28), (230, 190, 50), (248, 222, 100), (255, 246, 180)]


def part_palette(metal):
    fixed = {"copper": COPPER_METAL, "iron": IRON_METAL, "gold": GOLD_METAL, "tin": TIN, "bronze": BRONZE}
    if metal in fixed:
        return fixed[metal]
    if metal in METAL_COLORS:
        return METAL_COLORS[metal][2]
    if metal in ALLOY_COLORS:
        return ALLOY_COLORS[metal][2]
    # Metals added since the material sets (thallite) draw their plates, dusts and washed ores in their set's own ramp
    # (tools/material_icons.py), outline to highlight.
    import material_icons
    return list(material_icons.METAL_RAMPS[metal])


def circuit(advanced):
    img = new()
    board = [(24, 96, 56), (30, 112, 64)] if not advanced else [(28, 60, 110), (34, 72, 128)]
    trace = (212, 180, 80)
    for y in range(2, 14):
        for x in range(1, 15):
            img.putpixel((x, y), board[(x + y) % 2] + (255,))
    for x in range(2, 14):
        img.putpixel((x, 4), trace + (255,))
        img.putpixel((x, 11), trace + (255,))
    for y in range(4, 12):
        img.putpixel((3, y), trace + (255,))
        img.putpixel((12, y), trace + (255,))
    chips = [(5, 6), (9, 6)] if advanced else [(6, 6)]
    for cx, cy in chips:
        for y in range(cy, cy + 4):
            for x in range(cx, cx + 3 if advanced else cx + 4):
                img.putpixel((x, y), (30, 30, 34, 255))
    return img


def processor():
    """A processor: a square black package with a cyan die window, on a dark green board with gold pins all round."""
    img = new()
    gold = (226, 188, 72)
    for y in range(1, 15):
        for x in range(1, 15):
            img.putpixel((x, y), ((22, 60, 46) if (x + y) % 2 else (26, 70, 52)) + (255,))
    for i in range(3, 13, 2):
        for x, y in ((i, 2), (i, 13), (2, i), (13, i)):
            img.putpixel((x, y), gold + (255,))
    for y in range(4, 12):
        for x in range(4, 12):
            c = (34, 36, 42) if x in (4, 11) or y in (4, 11) else (24, 26, 30)
            if 6 <= x <= 9 and 6 <= y <= 9:
                c = (56, 200, 218) if (x, y) in ((6, 6), (7, 6), (6, 7)) else (28, 128, 148)
            img.putpixel((x, y), c + (255,))
    return img


def press_front(seed, lit):
    img = window(seed, [(40, 40, 44), (50, 50, 56)])
    head = STEEL[4] if lit else STEEL[3]
    top = 6 if lit else 5
    for x in range(5, 11):
        for y in range(top, top + 2):
            img.putpixel((x, y), head + (255,))
    for x in range(5, 11):
        img.putpixel((x, 10), (BRONZE[3] if lit else STEEL[2]) + (255,))
    return img


def drawer_front(seed, lit):
    img = window(seed, [(40, 40, 44), (50, 50, 56)])
    for cx in (6, 9):
        for y in range(5, 11):
            img.putpixel((cx, y), (STEEL[4] if (y % 2) else STEEL[2]) + (255,))
    wire_color = COPPER_METAL[3] if lit else COPPER_METAL[1]
    for x in range(4, 12):
        img.putpixel((x, 8), wire_color + (255,))
    return img


def assembler_front(seed, lit):
    """A circuit board behind the window: gold traces that brighten when running."""
    img = window(seed, [(24, 60, 40), (28, 70, 46)])
    trace = (230, 200, 90) if lit else (120, 110, 70)
    for x, y in [(x, 6) for x in range(5, 11)] + [(x, 9) for x in range(5, 11)] + [(7, 7), (7, 8), (9, 7), (9, 8)]:
        img.putpixel((x, y), trace + (255,))
    return img


def machines():
    (TEX / "gui").mkdir(parents=True, exist_ok=True)
    save(panel(500), "block", "machine_side")
    save(panel(501, trim=STEEL), "block", "machine_top")
    save(grate(502), "block", "coal_generator_front")
    save(grate(502, glow=[(250, 140, 30), (255, 190, 60), (220, 80, 20)]), "block", "coal_generator_front_on")
    save(window(503, [(30, 26, 26), (44, 36, 34)]), "block", "electric_furnace_front")
    save(window(503, [(30, 26, 26)], glow=[(230, 70, 40), (250, 120, 50), (200, 40, 30)]), "block", "electric_furnace_front_on")
    save(jaws(504, False), "block", "crusher_front")
    save(jaws(504, True), "block", "crusher_front_on")
    save(battery_front(505), "block", "battery_box_front")
    save(window(506, [(28, 30, 40), (36, 40, 52)]), "block", "arc_furnace_controller_front")
    save(window(506, [(28, 30, 40)], glow=[(170, 210, 255), (230, 240, 255), (120, 170, 255)]), "block", "arc_furnace_controller_front_on")
    save(panel(507, palette=BRONZE, trim=STEEL), "block", "machine_casing")
    save(bricks(508), "block", "arc_furnace_casing")
    save(solar_top(), "block", "solar_panel_top")
    save(boiler(509, False), "block", "steam_generator_front")
    save(boiler(509, True), "block", "steam_generator_front_on")
    save(crucibles(510, False), "block", "alloy_smelter_front")
    save(crucibles(510, True), "block", "alloy_smelter_front_on")
    save(press_front(511, False), "block", "metal_press_front")
    save(press_front(511, True), "block", "metal_press_front_on")
    save(drawer_front(512, False), "block", "wire_drawer_front")
    save(drawer_front(512, True), "block", "wire_drawer_front_on")
    save(assembler_front(513, False), "block", "circuit_assembler_front")
    save(assembler_front(513, True), "block", "circuit_assembler_front_on")
    for index, (machine, draw) in enumerate((("pulverizer", pulverizer_front), ("ore_washer", washer_front),
                                             ("sieve", sieve_front), ("sawmill", sawmill_front))):
        save(draw(560 + index, False), "block", f"{machine}_front")
        save(draw(560 + index, True), "block", f"{machine}_front_on")
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from materials import COMPONENTS
    for form, draw in (("plate", ms.plate), ("gear", ms.gear), ("wire", ms.wire), ("dust", ms.dust)):
        for metal in COMPONENTS[form]:
            save(draw(part_palette(metal)), "item", f"{metal}_{form}")
    from materials import WASHED_ORES
    for metal in WASHED_ORES:
        save(ms.washed(part_palette(metal)), "item", f"washed_{metal}_ore")
    save(pile(950, [(196, 160, 108), (214, 180, 126), (176, 140, 92), (230, 200, 150)]), "item", "sawdust")
    # Coke: porous gray-black lumps with a dull silver sheen.
    save(lump([(28, 28, 30), (48, 48, 52), (74, 74, 80)], (150, 150, 158)), "item", "coke")
    glass_textures()
    ember = [(250, 140, 30), (255, 190, 60), (220, 80, 20)]
    save(grate(952), "block", "coke_oven_front")
    save(grate(952, glow=ember), "block", "coke_oven_front_on")
    save(window(953, [(30, 26, 26), (44, 36, 34)]), "block", "steel_foundry_front")
    save(battery_front(954), "block", "capacitor_bank_front")
    save(battery_front(978), "block", "lithium_battery_bank_front")
    save(battery_front(985), "block", "flow_battery_front")
    save(window(980, [(40, 46, 50), (52, 60, 64)]), "block", "lithography_station_front")
    save(window(980, [(40, 46, 50)], glow=[(80, 210, 230), (150, 240, 250), (60, 170, 200)]), "block", "lithography_station_front_on")
    save(tank_side(955), "block", "steel_tank_front")
    save(tank_side(981), "block", "gas_holder_front")
    save(battery_front(983), "block", "advanced_solar_panel_front")
    save(grate(984), "block", "advanced_engine_front")
    save(grate(984, glow=[(250, 140, 30), (255, 190, 60), (220, 80, 20)]), "block", "advanced_engine_front_on")
    save(grate(982), "block", "crop_harvester_front")
    save(grate(982, glow=[(150, 220, 80), (200, 250, 130), (110, 180, 60)]), "block", "crop_harvester_front_on")
    save(grate(957), "block", "cobblestone_generator_front")
    save(grate(957, glow=[(250, 140, 30), (255, 190, 60), (220, 80, 20)]), "block", "cobblestone_generator_front_on")
    save(window(958, [(28, 44, 30), (36, 56, 38)]), "block", "tree_farm_front")
    save(window(958, [(28, 44, 30)], glow=[(120, 200, 90), (170, 230, 120), (90, 170, 70)]), "block", "tree_farm_front_on")
    save(window(3301, [(30, 40, 34), (38, 50, 40)]), "block", "hydroponic_bay_front")
    save(window(3301, [(30, 40, 34)], glow=[(200, 110, 230), (236, 150, 250), (150, 210, 110)]), "block",
         "hydroponic_bay_front_on")
    save(window(3401, [(40, 46, 60), (50, 58, 74)]), "block", "electroplating_bath_front")
    save(window(3401, [(40, 46, 60)], glow=[(90, 170, 230), (150, 210, 250), (60, 130, 200)]), "block",
         "electroplating_bath_front_on")
    save(window(3501, [(46, 56, 66), (60, 74, 86)]), "block", "ammonia_chiller_front")
    save(window(4301, [(44, 52, 64), (58, 70, 86)]), "block", "cryogenic_liquefier_front")
    save(window(4301, [(44, 52, 64)], glow=[(140, 200, 250), (200, 236, 255), (100, 170, 240)]), "block",
         "cryogenic_liquefier_front_on")
    save(window(3501, [(46, 56, 66)], glow=[(170, 220, 250), (220, 244, 255), (120, 190, 240)]), "block",
         "ammonia_chiller_front_on")
    save(window(3801, [(54, 50, 44), (70, 64, 56)]), "block", "rocket_workshop_front")
    save(window(3801, [(54, 50, 44)], glow=[(250, 160, 60), (255, 210, 120), (220, 110, 40)]), "block",
         "rocket_workshop_front_on")
    save(tank_side(959), "block", "water_wheel_front")
    save(boiler(961, False), "block", "large_steam_engine_front")
    save(boiler(961, True), "block", "large_steam_engine_front_on")
    save(assembler_front(960, False), "block", "auto_crafter_front")
    save(assembler_front(960, True), "block", "auto_crafter_front_on")
    save(window(962, [(20, 16, 12), (30, 24, 18)]), "block", "pumpjack_front")
    save(window(962, [(20, 16, 12)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "pumpjack_front_on")
    save(window(964, [(30, 26, 26), (44, 36, 34)]), "block", "distillation_tower_front")
    save(window(964, [(30, 26, 26)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "distillation_tower_front_on")
    save(window(965, [(30, 26, 26), (44, 36, 34)]), "block", "catalytic_cracker_front")
    save(window(965, [(30, 26, 26)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "catalytic_cracker_front_on")
    save(jaws(969, False), "block", "fracking_rig_front")
    save(jaws(969, True), "block", "fracking_rig_front_on")
    save(window(970, [(60, 56, 44), (76, 70, 56)]), "block", "flowback_treatment_unit_front")
    save(window(970, [(60, 56, 44)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "flowback_treatment_unit_front_on")
    save(window(971, [(36, 40, 30), (48, 54, 40)]), "block", "diesel_generator_front")
    save(window(971, [(36, 40, 30)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "diesel_generator_front_on")
    save(window(972, [(34, 38, 44), (46, 52, 60)]), "block", "gas_turbine_front")
    save(window(972, [(34, 38, 44)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "gas_turbine_front_on")
    save(window(973, [(44, 46, 40), (58, 60, 52)]), "block", "polymerization_reactor_front")
    save(window(973, [(44, 46, 40)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "polymerization_reactor_front_on")
    # Batch 29: the hydrotreater's sight glass (golden fuel) and the heat recovery unit's firebox glow.
    save(window(2901, [(44, 46, 40), (58, 60, 52)]), "block", "hydrotreater_front")
    save(window(2901, [(44, 46, 40)], glow=[(230, 190, 80), (250, 222, 140), (200, 160, 60)]), "block", "hydrotreater_front_on")
    save(window(2902, [(44, 46, 40), (58, 60, 52)]), "block", "heat_recovery_unit_front")
    save(window(2902, [(44, 46, 40)], glow=[(255, 120, 40), (255, 180, 90), (220, 90, 30)]), "block", "heat_recovery_unit_front_on")
    save(window(974, [(36, 40, 30), (48, 54, 40)]), "block", "diesel_engine_front")
    save(window(974, [(36, 40, 30)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "diesel_engine_front_on")
    save(window(975, [(40, 46, 50), (52, 60, 64)]), "block", "electrolytic_cell_front")
    save(window(975, [(40, 46, 50)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "electrolytic_cell_front_on")
    save(window(985, [(36, 44, 56), (48, 58, 72)]), "block", "air_separation_unit_front")
    save(window(985, [(36, 44, 56)], glow=[(120, 200, 255), (180, 230, 255), (90, 160, 230)]), "block", "air_separation_unit_front_on")
    save(window(986, [(52, 46, 40), (66, 58, 50)]), "block", "synthesis_converter_front")
    save(window(986, [(52, 46, 40)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "synthesis_converter_front_on")
    save(window(976, [(52, 50, 30), (66, 62, 40)]), "block", "chemical_reactor_front")
    save(window(976, [(52, 50, 30)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "chemical_reactor_front_on")
    save(window(977, [(40, 46, 50), (52, 60, 64)]), "block", "fuel_cell_front")
    save(window(977, [(40, 46, 50)], glow=[(90, 230, 140), (160, 255, 190), (60, 200, 120)]), "block", "fuel_cell_front_on")
    save(jaws(978, False), "block", "deposit_drill_front")
    save(jaws(978, True), "block", "deposit_drill_front_on")
    save(jaws(956, False), "block", "ore_drill_front")
    save(jaws(956, True), "block", "ore_drill_front_on")
    save(window(953, [(30, 26, 26)], glow=[(255, 200, 80), (255, 236, 150), (250, 150, 40)]), "block", "steel_foundry_front_on")
    # The refinery's later machines: their fronts were first committed as pictures in the old style; drawn here now so
    # they match the rest.
    fire = [(255, 170, 40), (255, 214, 110), (230, 140, 30)]
    for index, (machine, inner, glow) in enumerate((
            ("catalytic_reformer", [(30, 26, 26), (44, 36, 34)], fire),
            ("chemical_mixer", [(40, 46, 50), (52, 60, 64)], fire),
            ("crystal_grower", [(40, 46, 50), (52, 60, 64)], [(80, 210, 230), (150, 240, 250), (60, 170, 200)]),
            ("oil_sand_extractor", [(40, 34, 26), (52, 44, 32)], fire),
            ("vacuum_distillation_unit", [(30, 26, 26), (44, 36, 34)], fire))):
        save(window(990 + index, inner), "block", f"{machine}_front")
        save(window(990 + index, inner[:1], glow=glow), "block", f"{machine}_front_on")
    save(circuit(False), "item", "basic_circuit")
    save(circuit(True), "item", "advanced_circuit")
    save(processor(), "item", "processor")
    save(electric_textures.cable(electric_textures.COPPER), "block", "copper_cable")
    save(electric_textures.cable(electric_textures.SILVER), "block", "silver_cable")
    save(electric_textures.cable(electric_textures.ALUMINUM), "block", "aluminum_cable")
    save(pipe_texture(), "block", "bronze_fluid_pipe")
    save(steel_pipe_texture(), "block", "steel_fluid_pipe")
    save(item_tube_texture(), "block", "brass_item_pipe")
    save(wrench_item(), "item", "brass_wrench")
    save(handbook_item(), "item", "engineers_handbook")
    save(prospector_item(), "item", "prospector")
    save(belt_texture(), "block", "belt")
    save(belt_item(), "item", "belt")
    armor = TEX / "entity" / "equipment" / "humanoid"
    armor.mkdir(parents=True, exist_ok=True)
    rocket_pack_armor().save(armor / "rocket_pack.png", optimize=True)

    def save_armor(img, layer, name):
        folder = TEX / "entity" / "equipment" / layer
        folder.mkdir(parents=True, exist_ok=True)
        img.save(folder / f"{name}.png", optimize=True)
    import gear_textures
    gear_textures.draw_all(save, save_armor, part_palette)
    import exosuit_art
    exosuit_art.draw_all(save, save_armor)
    import grapple
    grapple.draw_all(save)
    import guns
    guns.draw_all(save)
    import arms
    arms.draw_all(save)
    import arms_variants
    arms_variants.draw_all(save)
    import field_chemistry
    field_chemistry.draw_all(save, save_armor)
    import construction
    construction.draw_all(save)
    import gas_storage
    gas_storage.draw_all(save)
    import concordance_art
    concordance_art.draw_all(save)
    import control_electronics
    control_electronics.draw_all(save)
    import rocketry
    rocketry.draw_all(save)
    import dieselworks
    dieselworks.draw_all(save)
    import kaiserworks
    kaiserworks.draw_all(save)
    import trenchworks
    trenchworks.draw_all(save)
    import zeppelin
    zeppelin.draw_all(save)
    import mech
    mech.draw_all(save)
    import landship
    landship.draw_all(save)
    import artillery
    artillery.draw_all(save)
    import tower_guns
    tower_guns.draw_all(save)
    import fortifications
    fortifications.draw_all(save)
    import bunkerworks
    bunkerworks.draw_all(save)
    import fire_control
    fire_control.draw_all(save)
    import raiders
    import armoured_walker
    armoured_walker.draw_all(save)
    raiders.draw_all(save)
    import plastic
    plastic.draw_all(save)
    import armor_paint   # the atlases of the 3D armor sets (tools/armor_models.py)
    armor_paint.draw_all(TEX)
    save(conveyor_frame(0), "block", "conveyor_belt")
    save_animation([conveyor_frame(2 * i) for i in range(4)], "conveyor_belt_moving", frametime=1)
    save(shaft_frame(0), "block", "iron_shaft")
    save(gearbox_frame(0), "block", "brass_gearbox")
    save_animation([gearbox_frame(i * 11.25) for i in range(4)], "brass_gearbox_turning")
    arrow = [(5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (9, 7), (8, 6), (9, 9), (8, 10)]
    leaf = [(7, 7), (8, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9), (6, 10), (5, 10)]
    glyphs = {
        "overclock_module": ((255, 90, 40), [(8, 4), (7, 5), (6, 6), (7, 6), (8, 6), (9, 6), (8, 7), (7, 8), (6, 9)]),
        "range_module": ((80, 170, 255), [(x, y) for x in (6, 8, 10) for y in (5, 7, 9) if (x, y) != (8, 7)] + [(8, 7)]),
        "capacity_module": ((255, 200, 60), [(7, 4), (8, 4)] + [(x, y) for y in range(5, 11) for x in (6, 9)]
                            + [(7, 10), (8, 10), (7, 8), (8, 8), (7, 9), (8, 9)]),
        "silk_touch_module": ((120, 240, 200), [(8, 4), (7, 5), (9, 5), (6, 6), (10, 6), (6, 7), (10, 7), (7, 8), (9, 8), (8, 9)]),
        "fortune_module": ((80, 220, 90), [(7, 5), (8, 5), (6, 6), (9, 6), (7, 7), (8, 7), (5, 8), (10, 8), (8, 8), (8, 9), (8, 10)]),
    }
    for module, (lamp, glyph) in glyphs.items():
        save(module_card(lamp, glyph), "item", module)
    save(upgrade_card([(160, 40, 30), (220, 70, 40)], arrow), "item", "speed_upgrade")
    save(upgrade_card([(40, 120, 60), (70, 180, 90)], leaf), "item", "efficiency_upgrade")
    save(hazard_plinth(530), "block", "geothermal_plinth")
    save(geothermal_front(531, False), "block", "geothermal_generator_front")
    save(geothermal_front(531, True), "block", "geothermal_generator_front_on")
    save(geothermal_tank(532), "block", "geothermal_tank")
    save(stack(533), "block", "geothermal_stack")
    save(hazard_plinth(534), "block", "wind_turbine_base")
    save(turbine_front(535), "block", "wind_turbine_front")
    save(lattice(536), "block", "wind_turbine_mast")
    save(nacelle(537), "block", "wind_turbine_nacelle")
    save(blade(538), "block", "wind_turbine_blade")
    save(blade_tip(), "block", "wind_turbine_tip")
    save(hazard_plinth(540), "block", "heavy_plinth")
    save(firebrick(541), "block", "alloy_smelter_brick")
    save(crucible(542), "block", "alloy_crucible")
    save(hopper_side(543), "block", "alloy_hopper")
    save(hopper_top(), "block", "alloy_hopper_top")
    save(power_port(), "block", "power_port")
    save(power_port_frame(), "block", "power_port_frame")
    save(tank_side(520), "block", "fluid_tank_side")
    save(tank_cap(521, True), "block", "fluid_tank_top")
    save(tank_cap(522, False), "block", "fluid_tank_bottom")
    save(pump_side(523), "block", "electric_pump_side")
    save(pump_port(524, False), "block", "electric_pump_top")
    save(pump_port(525, True), "block", "electric_pump_bottom")
    save(panel(526, palette=STEEL, trim=STEEL), "block", "heavy_pump_side")
    save(pump_port(527, False), "block", "heavy_pump_top")
    save(pump_port(528, True), "block", "heavy_pump_bottom")
    gui()


def glass_textures():
    """Glass chemistry (batch 16): tincal crust, borax crystals, borosilicate glass and a coil of optical fibre."""
    save(speckled([(214, 206, 186), (224, 216, 196), (204, 196, 176), (232, 226, 210), (196, 188, 168)], 1601,
                  [(246, 246, 240), (236, 240, 244), (180, 176, 164)], count=36), "block", "tincal")
    save(lump([(196, 200, 204), (226, 230, 234), (246, 248, 250)], (255, 255, 255)), "item", "borax")
    glass = new()
    for y in range(2, 14):
        shift = (13 - y) // 4
        for x in range(3 + shift, 13 + shift):
            edge = y in (2, 13) or x in (3 + shift, 12 + shift)
            c = (120, 170, 176, 255) if edge else (200, 232, 236, 150) if (x + y) % 7 else (240, 252, 252, 200)
            glass.putpixel((x, y), c)
    save(glass, "item", "borosilicate_glass")
    fibre = new()
    import math
    for t in range(0, 360, 6):
        for r, c in ((5.5, (120, 220, 240)), (3.5, (90, 190, 220))):
            x = 8 + r * math.cos(math.radians(t))
            y = 8 + r * 0.6 * math.sin(math.radians(t))
            fibre.putpixel((int(round(x)), int(round(y))), c + (255,))
    for x, y in ((13, 7), (14, 6), (15, 5)):
        fibre.putpixel((x, y), (200, 250, 255, 255))
    save(fibre, "item", "optical_fibre")
    save(lump([(70, 72, 78), (104, 106, 114), (140, 142, 150)], (210, 214, 222)), "item", "ferroboron")


def main():
    save(blend(17), "item", "bronze_blend")
    icon = ore(STONE, 11)
    icon.paste(from_mask(INGOT, BRONZE), (0, 3), from_mask(INGOT, BRONZE))
    icon.resize((128, 128), Image.NEAREST).save(TEX.parent / "icon.png", optimize=True)
    main_extra()
    machines()
    import steampunk_textures
    steampunk_textures.draw_all()
    import dieselpunk_textures
    dieselpunk_textures.draw_all()
    import dieselrust_textures
    dieselrust_textures.draw_all()
    import mill_textures
    mill_textures.draw_all()
    electric_textures.draw_all()
    import crop_textures
    for (kind, name), image in crop_textures.crop_textures().items():
        save(image, kind, name)
    import petro_textures
    petro_textures.draw_all(save, save_animation)
    import cotton_textures
    cotton_textures.draw_all(save)
    import deposits
    deposits.draw_all(save)
    import drone_textures
    drone_textures.draw_all()
    import blueprints
    for name, img in blueprints.draw_textures().items():
        save(img, "block", name)
    for name, img in blueprints.draw_item_textures().items():
        save(img, "item", name)
    ghost_dir = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures" / "misc"
    ghost_dir.mkdir(parents=True, exist_ok=True)
    blueprints.ghost_texture().save(ghost_dir / "blueprint_ghost.png")
    import tower
    for name, img in tower.draw_textures().items():
        save(img, "block", name)
    for name, img in tower.draw_item_textures().items():
        save(img, "item", name)
    # The tower's building blocks, pads and pickups at 64 px (tools/tower_art.py), over the 16 px versions above.
    import tower_art
    tower_art.write_all(str(ROOT / "src" / "main" / "resources" / "assets" / "jugcraft"))

    import power_lines
    for name, img in power_lines.draw_item_textures().items():
        save(img, "item", name)
    entity_dir = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures" / "entity"
    entity_dir.mkdir(parents=True, exist_ok=True)
    power_lines.draw_sheet().save(entity_dir / "power_lines.png", optimize=True)

    import tank_display
    tank_display.draw_all(save)

    import pixel_hollows_textures
    pixel_hollows_textures.draw_all()
    import styx
    styx.textures()

    # The feasts and food displays' plate, which the owner's library has no drawing for (tools/feasts_textures.py).
    import feasts_textures
    feasts_textures.draw_all(save)

    import transport_crates
    transport_crates.main()

    # Last: the owner's own textures, copied from the shared library as drawn (tools/owner_art.py), so nothing above
    # draws over them.
    import owner_art
    owner_art.write_all()


if __name__ == "__main__":
    main()
