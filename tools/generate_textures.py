"""Draw Jugcraft's original 16x16 material textures (requires Pillow).

Run from the repository root:  python3 tools/generate_textures.py
Every pixel is generated here from fixed seeds; no Mojang texture is read, traced or
recolored. Colors follow the real minerals: cassiterite is glossy brown-black, tin is
pale cool silver, bronze is warm golden-brown.
"""
import random
from pathlib import Path

from PIL import Image

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
    "aluminum": (None, None,
                 [(118, 124, 130), (166, 172, 178), (198, 202, 208), (220, 224, 228), (238, 240, 244)]),
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

    save(speckled(BAUXITE, 300, [(214, 170, 130), (226, 190, 150)]), "block", "bauxite")
    save(speckled(OIL_SAND_BASE, 301, BITUMEN, count=40), "block", "oil_sand")
    save(raw_chunk(302, BITUMEN, (120, 116, 110)), "item", "bitumen")
    save(pile(303, [(222, 200, 40), (240, 222, 70), (250, 238, 120)]), "item", "sulfur_dust")
    save(raw_chunk(304, [(46, 54, 72), (70, 80, 102), (100, 112, 138)], (190, 206, 236)), "item", "silicon")
    save(pile(305, [(236, 236, 240), (248, 248, 250), (222, 224, 230)]), "item", "lithium_carbonate")
    save(pile(306, [(232, 196, 210), (196, 224, 196), (240, 228, 196), (214, 206, 232)]), "item", "rare_earth_oxide")


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


if __name__ == "__main__":
    main()
