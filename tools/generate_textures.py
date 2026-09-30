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


def ore(base_palette, seed, streaks=False):
    img = rock(base_palette, seed, streaks)
    rng = random.Random(seed + 1)
    for cx, cy in [(3, 3), (10, 2), (6, 8), (12, 10), (2, 12), (9, 13)]:
        cells = [(cx, cy), (cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1), (cx + rng.choice([-1, 2]), cy + rng.choice([0, 1]))]
        for x, y in cells:
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), rng.choice(CASSITERITE) + (255,))
        img.putpixel((cx, cy), GLINT + (255,))
    return img


def from_mask(mask, palette):
    img = new()
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch.isdigit():
                img.putpixel((x, y), palette[int(ch)] + (255,))
    return img


def raw_chunk(seed):
    rng = random.Random(seed)
    img = new()
    for y, row in enumerate(RAW):
        for x, ch in enumerate(row):
            if ch == "x":
                shade = 2 if (x + y) < 12 else (1 if (x + y) < 20 else 0)
                img.putpixel((x, y), CASSITERITE[max(0, shade - rng.choice([0, 0, 1]))] + (255,))
    for x, y in [(6, 5), (9, 4), (4, 7), (8, 8), (11, 6)]:
        img.putpixel((x, y), GLINT + (255,))
    return img


def raw_block(seed):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), rng.choice(CASSITERITE + [CASSITERITE[1]]) + (255,))
    for _ in range(9):
        img.putpixel((rng.randrange(16), rng.randrange(16)), GLINT + (255,))
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


def blend(seed):
    rng = random.Random(seed)
    img = new()
    grey = [TIN[1], TIN[2], TIN[3]]
    for y, row in enumerate(PILE):
        for x, ch in enumerate(row):
            if ch == "x":
                img.putpixel((x, y), rng.choice(COPPER + COPPER + COPPER + grey) + (255,))
    return img


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


if __name__ == "__main__":
    main()
