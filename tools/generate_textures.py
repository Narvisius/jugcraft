"""Draw Jugcraft's original 16x16 material textures (requires Pillow).

Run from the repository root:  python3 tools/generate_textures.py
Every pixel is generated here from fixed seeds; no Mojang texture is read, traced or
recolored. Colors follow the real minerals: cassiterite is glossy brown-black, tin is
pale cool silver, bronze is warm golden-brown.
"""
import random
from pathlib import Path

from PIL import Image

import electric_textures

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


def save(img, kind, name, scale=1):
    path = TEX / kind / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    if scale != 1:
        img = img.resize((16 * scale, 16 * scale), Image.NEAREST)
    img.save(path, optimize=True)


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
    rng = random.Random(seed)
    img = new()
    for y, row in enumerate(PILE):
        for x, ch in enumerate(row):
            if ch == "x":
                img.putpixel((x, y), rng.choice(palette) + (255,))
    return img


def blend(seed):
    rng = random.Random(seed)
    img = new()
    grey = [TIN[1], TIN[2], TIN[3]]
    for y, row in enumerate(PILE):
        for x, ch in enumerate(row):
            if ch == "x":
                img.putpixel((x, y), rng.choice(COPPER + COPPER + COPPER + grey) + (255,))
    return img


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

BAUXITE = [(126, 58, 36), (150, 72, 44), (170, 88, 54), (188, 108, 68), (112, 50, 32)]
OIL_SAND_BASE = [(176, 152, 108), (190, 166, 120), (160, 136, 94), (146, 124, 86), (132, 110, 76)]
BITUMEN = [(14, 12, 12), (28, 24, 22), (44, 40, 36)]


def speckled(base, seed, specks, count=18):
    img = rock(base, seed)
    rng = random.Random(seed + 7)
    for _ in range(count):
        img.putpixel((rng.randrange(16), rng.randrange(16)), rng.choice(specks) + (255,))
    return img


def main_extra():
    seed = 100
    for metal, (specks, glint, palette) in METAL_COLORS.items():
        seed += 10
        if specks:
            save(ore(STONE, seed, specks=specks, glint=glint), "block", f"{metal}_ore")
            save(ore(DEEPSLATE, seed + 1, streaks=True, specks=specks, glint=glint), "block", f"deepslate_{metal}_ore")
            save(raw_block(seed + 2, specks, glint), "block", f"raw_{metal}_block")
            save(raw_chunk(seed + 3, specks, glint), "item", f"raw_{metal}")
        save(metal_block(palette, seed + 4), "block", f"{metal}_block")
        save(from_mask(INGOT, palette), "item", f"{metal}_ingot")
        save(from_mask(NUGGET, palette), "item", f"{metal}_nugget")

    for mineral, (specks, glint, palette) in MINERAL_COLORS.items():
        seed += 10
        save(ore(STONE, seed, specks=specks, glint=glint), "block", f"{mineral}_ore")
        save(ore(DEEPSLATE, seed + 1, streaks=True, specks=specks, glint=glint), "block", f"deepslate_{mineral}_ore")
        save(rock(palette, seed + 2), "block", f"{mineral}_block")
        save(pile(seed + 3, palette), "item", mineral)

    for index, (metal, (_, _, palette)) in enumerate(ALLOY_COLORS.items()):
        save(metal_block(palette, 700 + index), "block", f"{metal}_block")
        save(from_mask(INGOT, palette), "item", f"{metal}_ingot")
        save(from_mask(NUGGET, palette), "item", f"{metal}_nugget")

    save(speckled(BAUXITE, 300, [(214, 170, 130), (226, 190, 150)]), "block", "bauxite")
    save(speckled(OIL_SAND_BASE, 301, BITUMEN, count=40), "block", "oil_sand")
    save(raw_chunk(302, BITUMEN, (120, 116, 110)), "item", "bitumen")
    save(pile(303, [(222, 200, 40), (240, 222, 70), (250, 238, 120)]), "item", "sulfur_dust")
    save(raw_chunk(304, [(46, 54, 72), (70, 80, 102), (100, 112, 138)], (190, 206, 236)), "item", "silicon")
    save(pile(305, [(236, 236, 240), (248, 248, 250), (222, 224, 230)]), "item", "lithium_carbonate")
    save(pile(306, [(232, 196, 210), (196, 224, 196), (240, 228, 196), (214, 206, 232)]), "item", "rare_earth_oxide")


STEEL = [(62, 66, 72), (86, 90, 97), (110, 114, 121), (134, 138, 145), (160, 164, 170)]


def panel(seed, palette=STEEL, trim=BRONZE):
    """A riveted machine panel: bronze frame around a steel plate."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = trim[1]
            elif x == 1 or y == 1:
                c = trim[3]
            elif x == 14 or y == 14:
                c = trim[0]
            else:
                c = palette[rng.choice([1, 2, 2, 3])]
            img.putpixel((x, y), c + (255,))
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        img.putpixel((x, y), trim[4] + (255,))
    return img


def window(seed, inner, glow=None):
    """A machine front with a recessed dark window; glow colors light it when running."""
    img = panel(seed)
    rng = random.Random(seed + 1)
    for y in range(4, 12):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (4, 11)
            if edge:
                img.putpixel((x, y), STEEL[0] + (255,))
            else:
                palette = glow if glow else inner
                img.putpixel((x, y), rng.choice(palette) + (255,))
    return img


def grate(seed, glow=None):
    img = window(seed, [(24, 22, 22), (34, 30, 28)], glow)
    for y in (6, 8, 10):
        for x in range(5, 11):
            img.putpixel((x, y), STEEL[1] + (255,))
    return img


def jaws(seed, active):
    img = window(seed, [(40, 40, 44), (52, 52, 58)])
    tooth = STEEL[4] if active else STEEL[3]
    for x in range(5, 11):
        top = 5 + (x % 2)
        bottom = 10 - (x % 2)
        img.putpixel((x, top), tooth + (255,))
        img.putpixel((x, bottom), tooth + (255,))
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
    """A fine mesh; grains fall through when running."""
    img = window(seed, [(34, 30, 28), (42, 38, 34)])
    for y in range(5, 11):
        for x in range(5, 11):
            if (x + y) % 2 == 0:
                img.putpixel((x, y), STEEL[2] + (255,))
    if active:
        for x, y in [(6, 7), (9, 9), (7, 10)]:
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
    rng = random.Random(seed)
    img = new()
    brick = [(196, 176, 150), (210, 192, 166), (182, 160, 134)]
    mortar = (120, 110, 100)
    for y in range(16):
        for x in range(16):
            row = y // 4
            offset = 4 if row % 2 else 0
            if y % 4 == 3 or (x + offset) % 8 == 7:
                c = mortar
            else:
                c = rng.choice(brick)
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
    import json
    strip = Image.new("RGBA", (16, 16 * len(frames)), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        strip.paste(frame, (0, 16 * index))
    path = TEX / "block" / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    strip.save(path, optimize=True)
    (TEX / "block" / f"{name}.png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": frametime}}, indent=2) + "\n", encoding="utf-8")


def belt_texture():
    """A dark leather belt: brown with a lighter stitch line along each edge (length runs down the texture)."""
    rng = random.Random(970)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (74, 46, 28) if rng.random() < 0.7 else (86, 54, 32)
            if x in (1, 14):
                c = (168, 136, 96) if y % 3 else (74, 46, 28)
            if x in (0, 15):
                c = (52, 32, 20)
            img.putpixel((x, y), c + (255,))
    return img


def conveyor_frame(shift):
    """Rubberised conveyor belt, seen from above with the front (where items go) at the top: dark rubber with
    chevron ribs pointing forwards. Shifting the ribs two pixels a frame makes them run at the items' speed."""
    rng = random.Random(975)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (38, 36, 34) if rng.random() < 0.75 else (46, 43, 40)
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
    """Dark steel footing with a yellow-black hazard edge."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            if y in (0, 15) or x in (0, 15):
                c = (214, 170, 40) if (x + y) // 2 % 2 else (30, 28, 26)
            else:
                c = STEEL[rng.choice([0, 1, 1])]
            img.putpixel((x, y), c + (255,))
    return img


def geothermal_front(seed, lit):
    """Vented front with a glowing heat exchanger when running."""
    img = panel(seed)
    glow = [(255, 120, 20), (255, 170, 40), (230, 80, 10)] if lit else [(60, 30, 24), (74, 36, 28)]
    rng = random.Random(seed + 1)
    for y in range(3, 13):
        for x in range(3, 13):
            if y % 2 == 0:
                img.putpixel((x, y), STEEL[0] + (255,))
            else:
                img.putpixel((x, y), rng.choice(glow) + (255,))
    return img


def geothermal_tank(seed):
    """Tinted steel shell with a narrow window of lava."""
    rng = random.Random(seed)
    img = new()
    lava = [(230, 90, 20), (250, 140, 30), (200, 60, 10)]
    for y in range(16):
        for x in range(16):
            if 6 <= x <= 9 and 2 <= y <= 13:
                c = STEEL[0] if x in (6, 9) else rng.choice(lava)
            else:
                c = (STEEL[1] if (x + y) % 5 else STEEL[2])
            img.putpixel((x, y), c + (255,))
    return img


def stack(seed):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = STEEL[0] if y % 4 == 0 else STEEL[rng.choice([1, 2])]
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
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (224, 226, 228) if y > 1 else (180, 184, 190)
            if rng.random() < 0.08:
                c = (206, 208, 212)
            img.putpixel((x, y), c + (255,))
    return img


def blade(seed):
    img = new()
    for y in range(16):
        for x in range(16):
            c = (238, 240, 242) if (x + y) % 4 else (216, 220, 226)
            img.putpixel((x, y), c + (255,))
    return img


def blade_tip():
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), ((206, 56, 46) if (x + y) % 4 else (180, 44, 38)) + (255,))
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
    """Small red-brown refractory bricks."""
    rng = random.Random(seed)
    img = new()
    brick = [(142, 62, 40), (158, 72, 46), (126, 54, 36)]
    for y in range(16):
        for x in range(16):
            offset = 2 if (y // 3) % 2 else 0
            if y % 3 == 2 or (x + offset) % 4 == 3:
                c = (84, 72, 64)
            else:
                c = rng.choice(brick)
            img.putpixel((x, y), c + (255,))
    return img


def crucible(seed):
    """Dark cast iron with a glowing seam, for the alloy crucible."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = rng.choice([(46, 44, 46), (56, 54, 56), (38, 36, 38)])
            if y in (4, 11) and 2 <= x <= 13:
                c = (240, 130, 40) if x % 3 else (255, 190, 80)
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


def solar_top():
    img = new()
    cell = [(22, 34, 78), (28, 44, 96), (34, 54, 112)]
    rng = random.Random(600)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = BRONZE[1]
            elif x in (5, 10) or y in (5, 10):
                c = (176, 182, 190)
            else:
                c = rng.choice(cell)
                if rng.random() < 0.06:
                    c = (120, 150, 220)
            img.putpixel((x, y), c + (255,))
    return img


def boiler(seed, lit):
    img = panel(seed)
    for y in range(3, 13):
        for x in range(3, 13):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 20:
                img.putpixel((x, y), BRONZE[2 if (x + y) % 3 else 3] + (255,))
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
                    c = [(250, 150, 40), (255, 200, 80), (230, 110, 30)][(x + y) % 3]
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
    return ALLOY_COLORS[metal][2]


def dust(palette):
    """A heap of metal powder: mid tones with dark grains, lighter on the lit top-left."""
    rng = random.Random(sum(palette[2]))
    img = new()
    for y, row in enumerate(PILE):
        for x, ch in enumerate(row):
            if ch == "x":
                shade = rng.choice([1, 2, 2, 3]) + (1 if x + y < 14 else 0)
                if rng.random() < 0.12:
                    shade = 0
                img.putpixel((x, y), palette[min(4, shade)] + (255,))
    return img


def washed_ore(metal, seed):
    """A clean chunk of ore in the metal's own colours, still wet: a few blue highlights."""
    img = raw_chunk(seed, part_palette(metal)[1:4], (236, 246, 255))
    for x, y in [(5, 9), (10, 7), (7, 11)]:
        img.putpixel((x, y), (120, 190, 240, 255))
    return img


def plate(palette):
    img = new()
    for y in range(3, 13):
        for x in range(2, 14):
            if x == 2 or y == 3:
                c = palette[4]
            elif x == 13 or y == 12:
                c = palette[0]
            else:
                c = palette[2 if (x + y) % 5 else 3]
            img.putpixel((x, y), c + (255,))
    return img


def gear(palette):
    img = new()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r2 = dx * dx + dy * dy
            tooth = (abs(dx) < 1.6 or abs(dy) < 1.6 or abs(abs(dx) - abs(dy)) < 1.2)
            if r2 <= 2.2:
                continue  # axle hole
            if r2 <= 22 or (r2 <= 49 and tooth):
                shade = 4 if dx + dy < -4 else (0 if dx + dy > 5 else 2)
                img.putpixel((x, y), palette[shade] + (255,))
    return img


def wire(palette):
    img = new()
    for i in range(4):
        cy = 4 + i * 3
        for x in range(3, 13):
            y = cy + (1 if (x // 2) % 2 else 0)
            img.putpixel((x, y), palette[3 if x % 3 else 4] + (255,))
            img.putpixel((x, y + 1), palette[1] + (255,))
    for y in range(3, 15):
        img.putpixel((2, y), (96, 70, 44, 255))
        img.putpixel((13, y), (96, 70, 44, 255))
    return img


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
    img = window(seed, [(24, 60, 40), (28, 70, 46)])
    for x in range(5, 11):
        for y in range(5, 11):
            if (x + y) % 3 == 0:
                img.putpixel((x, y), ((230, 200, 90) if lit else (120, 110, 70)) + (255,))
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
    for form, draw in (("plate", plate), ("gear", gear), ("wire", wire), ("dust", dust)):
        for metal in COMPONENTS[form]:
            save(draw(part_palette(metal)), "item", f"{metal}_{form}")
    from materials import WASHED_ORES
    for index, metal in enumerate(WASHED_ORES):
        save(washed_ore(metal, 900 + index), "item", f"washed_{metal}_ore")
    save(pile(950, [(196, 160, 108), (214, 180, 126), (176, 140, 92), (230, 200, 150)]), "item", "sawdust")
    # Coke: porous gray-black lumps with a dull silver sheen.
    save(raw_chunk(951, [(28, 28, 30), (48, 48, 52), (74, 74, 80)], (150, 150, 158)), "item", "coke")
    ember = [(250, 140, 30), (255, 190, 60), (220, 80, 20)]
    save(grate(952), "block", "coke_oven_front")
    save(grate(952, glow=ember), "block", "coke_oven_front_on")
    save(window(953, [(30, 26, 26), (44, 36, 34)]), "block", "steel_foundry_front")
    save(battery_front(954), "block", "capacitor_bank_front")
    save(battery_front(978), "block", "lithium_battery_bank_front")
    save(window(979, [(40, 46, 50), (52, 60, 64)]), "block", "crystal_grower_front")
    save(window(979, [(40, 46, 50)], glow=[(80, 210, 230), (150, 240, 250), (60, 170, 200)]), "block", "crystal_grower_front_on")
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
    save(tank_side(959), "block", "water_wheel_front")
    save(boiler(961, False), "block", "large_steam_engine_front")
    save(boiler(961, True), "block", "large_steam_engine_front_on")
    save(assembler_front(960, False), "block", "auto_crafter_front")
    save(assembler_front(960, True), "block", "auto_crafter_front_on")
    save(window(962, [(20, 16, 12), (30, 24, 18)]), "block", "pumpjack_front")
    save(window(962, [(20, 16, 12)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "pumpjack_front_on")
    save(window(963, [(40, 34, 26), (52, 44, 32)]), "block", "oil_sand_extractor_front")
    save(window(963, [(40, 34, 26)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "oil_sand_extractor_front_on")
    save(window(964, [(30, 26, 26), (44, 36, 34)]), "block", "distillation_tower_front")
    save(window(964, [(30, 26, 26)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "distillation_tower_front_on")
    save(window(965, [(30, 26, 26), (44, 36, 34)]), "block", "catalytic_cracker_front")
    save(window(965, [(30, 26, 26)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "catalytic_cracker_front_on")
    save(window(966, [(30, 26, 26), (44, 36, 34)]), "block", "vacuum_distillation_unit_front")
    save(window(966, [(30, 26, 26)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "vacuum_distillation_unit_front_on")
    save(window(967, [(30, 26, 26), (44, 36, 34)]), "block", "catalytic_reformer_front")
    save(window(967, [(30, 26, 26)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "catalytic_reformer_front_on")
    save(window(968, [(40, 46, 50), (52, 60, 64)]), "block", "chemical_mixer_front")
    save(window(968, [(40, 46, 50)], glow=[(255, 170, 40), (255, 214, 110), (230, 140, 30)]), "block", "chemical_mixer_front_on")
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


def main():
    save(ore(STONE, 11), "block", "tin_ore")
    save(ore(DEEPSLATE, 12, streaks=True), "block", "deepslate_tin_ore")
    save(raw_block(13), "block", "raw_tin_block")
    save(metal_block(TIN, 14), "block", "tin_block")
    save(metal_block(BRONZE, 15), "block", "bronze_block")
    save(raw_chunk(16), "item", "raw_tin")
    save(from_mask(INGOT, TIN), "item", "tin_ingot")
    save(from_mask(NUGGET, TIN), "item", "tin_nugget")
    save(from_mask(INGOT, BRONZE), "item", "bronze_ingot")
    save(from_mask(NUGGET, BRONZE), "item", "bronze_nugget")
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
    electric_textures.draw_all()
    import petro_textures
    petro_textures.draw_all(save, save_animation)
    import crop_textures
    crop_textures.draw_all(save)
    import deposits
    deposits.draw_all(save)


if __name__ == "__main__":
    main()
