"""Original textures for Jugcraft's crops (batch 9): the four growth stages of cotton, its seeds and the cotton boll.
Crop stages have transparent backgrounds (the game draws them cut out, as a cross of two planes). Deterministic.
"""
import random

from PIL import Image

STEM = [(58, 96, 40), (78, 124, 52), (104, 150, 66)]
LEAF = [(52, 110, 46), (72, 140, 58), (98, 168, 74)]
BOLL = [(214, 214, 206), (236, 236, 230), (252, 252, 248)]
BRACT = [(96, 70, 40), (124, 92, 54)]


def _put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c + (255,))


def cotton_stage(stage):
    """Stage 0: sprouts; 1: young plants with leaves; 2: tall plants with green bolls; 3: open white bolls."""
    rng = random.Random(990 + stage)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    height = (4, 8, 12, 13)[stage]
    for x0 in (2, 7, 12):
        x = x0 + rng.randrange(0, 2)
        for y in range(15, 15 - height, -1):
            _put(img, x, y, STEM[(y + x) % 3])
        # Leaves along the stem.
        for y in range(15 - height + 2, 15, 3):
            side = 1 if (y + x0) % 2 else -1
            _put(img, x + side, y, LEAF[1])
            _put(img, x + 2 * side, y - 1, LEAF[2 if stage else 0])
        top = 15 - height
        if stage == 2:
            for dx, dy in ((0, 0), (1, 2), (-1, 3)):
                _put(img, x + dx, top + dy, (120, 160, 80))
                _put(img, x + dx, top + dy + 1, (96, 136, 64))
        elif stage == 3:
            for dx, dy in ((0, 0), (1, 3), (-1, 4)):
                for bx, by in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
                    _put(img, x + dx + bx, top + dy + by, BOLL[rng.randrange(3)])
                _put(img, x + dx, top + dy + 2, BRACT[0])
    return img


def cotton_seeds():
    """A small pile of fuzzy grey-white cotton seeds."""
    rng = random.Random(995)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for sx, sy in ((5, 7), (9, 6), (7, 10), (10, 10), (4, 11)):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            _put(img, sx + dx, sy + dy, (70, 56, 44) if (dx + dy) % 2 else (96, 80, 62))
        _put(img, sx - 1, sy, (220, 220, 214))
        _put(img, sx + rng.randrange(0, 2), sy - 1, (236, 236, 232))
    return img


def cotton():
    """A cotton boll: a round white tuft in brown bracts on a short stem."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) / 5.5) ** 2 + ((y - 6.5) / 5) ** 2
            if d <= 1:
                c = BOLL[2] if d < 0.35 and x < 8 else BOLL[1] if d < 0.75 else BOLL[0]
                if (x == 7 or x == 8) and y > 4:
                    c = BOLL[0]
                _put(img, x, y, c)
    for x in range(3, 13):
        _put(img, x, 11, BRACT[1] if x % 3 else BRACT[0])
    for x, y in ((4, 12), (11, 12), (7, 12), (8, 12), (7, 13), (8, 14)):
        _put(img, x, y, BRACT[0])
    return img


def draw_all(save):
    for stage in range(4):
        save(cotton_stage(stage), "block", f"cotton_crop_stage{stage}")
    save(cotton_seeds(), "item", "cotton_seeds")
    save(cotton(), "item", "cotton")
