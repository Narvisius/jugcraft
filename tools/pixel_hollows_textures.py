"""Original textures for the Pixel Hollows and the Retro Trader (called from generate_textures.py).

Deterministic: each texture draws from its own seed. Nothing is read, traced or recoloured from Mojang
textures. Block textures are 16x16 and opaque; the shard and the map marker have transparent backgrounds.

The Retro Trader's look follows the owner's reference picture: a bearded villager with brown hair, black
rectangular glasses and green eyes, a red-and-black flannel shirt open over a white tee with a silver cross
on a chain, backpack straps, navy plaid trousers and black sneakers with white soles. It is a villager
profession overlay (64x64, drawn on the villager model's UV layout), so the villager's own face and nose
stay underneath.
"""
import json
import random
from pathlib import Path

from PIL import Image

import block_style as bs

TEX = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures"

SLATE = [(28, 26, 36), (36, 34, 46), (44, 42, 56), (52, 50, 66), (62, 60, 78)]
COPPER = [(110, 62, 36), (156, 92, 52), (200, 130, 76), (236, 176, 112)]
VIA = (250, 222, 160)
CRYSTAL = [(18, 70, 84), (28, 120, 128), (44, 172, 170), (96, 224, 210), (196, 255, 244)]
VIOLET = [(70, 34, 110), (130, 66, 190), (196, 120, 240)]
LEDS = [(60, 230, 210), (240, 80, 200), (250, 220, 80), (110, 240, 110), (90, 140, 255), (240, 240, 250)]


def new(size=16, fill=(0, 0, 0, 255)):
    return Image.new("RGBA", (size, size), fill)


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(color[:3]) + ((color[3],) if len(color) > 3 else (255,)))


def save(img, path):
    target = TEX / f"{path}.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    img.save(target, optimize=True)


def shade(color, amount):
    return tuple(max(0, min(255, c + amount)) for c in color[:3])


# ---------------------------------------------------------------- circuitstone

def slate(seed):
    """Dark slate in the manner of vanilla deepslate: its tones in thin streaks running across the block."""
    return bs.img(bs.streaks(SLATE, seed, vertical=False, spread=0.7))


def trace(img, rng, start, steps):
    """A one-pixel copper trace that runs straight, turns at right angles and ends in a pad and a via."""
    x, y = start
    dx, dy = rng.choice([(1, 0), (-1, 0), (0, 1), (0, -1)])
    for _ in range(steps):
        put(img, x, y, COPPER[2])
        if 0 <= x - dy < 16 and 0 <= y + dx < 16 and img.getpixel((x - dy, y + dx))[:3] in SLATE:
            put(img, x - dy, y + dx, COPPER[0])  # shadow beside the trace
        nx, ny = x + dx, y + dy
        if not (1 <= nx <= 14 and 1 <= ny <= 14) or rng.random() < 0.18:
            dx, dy = (dy, -dx) if rng.random() < 0.5 else (-dy, dx)
            nx, ny = x + dx, y + dy
            if not (1 <= nx <= 14 and 1 <= ny <= 14):
                break
        x, y = nx, ny
    for px, py in ((x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)):
        put(img, px, py, COPPER[3] if (px, py) == (x, y) else COPPER[1])
    put(img, x, y, VIA)


def circuitstone(seed=900):
    """Dark slate veined with copper traces, pads and vias."""
    rng = random.Random(seed)
    img = slate(seed)
    for _ in range(4):
        trace(img, rng, (rng.randint(2, 13), rng.randint(2, 13)), rng.randint(8, 14))
    return img


def polished_circuitstone(seed=901):
    """Smooth slate with a bevel and a little chip on two traces."""
    smooth = bs.surface(SLATE[1:4], seed, weights=[1, 5, 2], spread=0.5)
    img = new()
    for y in range(16):
        for x in range(16):
            c = smooth(x, y)
            if x == 0 or y == 0:
                c = SLATE[4]
            elif x == 15 or y == 15:
                c = SLATE[0]
            put(img, x, y, c)
    for x in range(2, 14):
        put(img, x, 4, COPPER[2])
        put(img, x, 11, COPPER[2])
    for y in range(4, 12):
        put(img, 2, y, COPPER[2])
    for x, y in ((13, 4), (13, 11), (2, 4), (2, 11)):
        put(img, x, y, VIA)
    # The chip: a black package with silver pins.
    for y in range(6, 10):
        for x in range(6, 11):
            put(img, x, y, (20, 20, 24))
    for x in (6, 8, 10):
        put(img, x, 5, (180, 182, 188))
        put(img, x, 10, (180, 182, 188))
    put(img, 6, 6, (60, 60, 70))
    return img


def circuitstone_bricks(seed=902):
    """Slate bricks with dark joints, in the manner of vanilla deepslate bricks; a few bricks carry a trace and a via."""
    rng = random.Random(seed)
    img = bs.img(bs.bricks(SLATE, seed, rows=4, cols=2))
    for row in range(4):
        if rng.random() < 0.6:
            y = row * 4 + 1
            x0 = rng.randint(0, 9)
            for x in range(x0, x0 + 4):
                if (x + (4 if row % 2 else 0)) % 8 != 7:
                    put(img, x, y, COPPER[2])
            put(img, x0, y, VIA)
    return img


def pixel_lamp(seed=903):
    """A grid of 2x2 coloured light-emitting pixels in a dark frame."""
    rng = random.Random(seed)
    img = new(fill=(48, 46, 58, 255))
    for i in range(16):
        put(img, i, 0, (70, 68, 82))
        put(img, 0, i, (70, 68, 82))
        put(img, i, 15, (30, 28, 38))
        put(img, 15, i, (30, 28, 38))
    for gy in range(7):
        for gx in range(7):
            color = rng.choice(LEDS)
            x, y = 1 + gx * 2, 1 + gy * 2
            put(img, x, y, shade(color, 30))
            put(img, x + 1, y, color)
            put(img, x, y + 1, color)
            put(img, x + 1, y + 1, shade(color, -40))
    return img


def crystal(seed, bright=False):
    """Square facets: each 4x4 tile is a lit top-left and a shaded bottom-right, mostly teal, some violet."""
    rng = random.Random(seed)
    img = new()
    for ty in range(4):
        for tx in range(4):
            palette = VIOLET + [VIOLET[2]] if rng.random() < 0.25 else CRYSTAL[1:]
            base = palette[2 if bright else 1]
            light = palette[3] if bright else palette[2]
            dark = palette[0]
            for y in range(4):
                for x in range(4):
                    c = base
                    if x == 0 or y == 0:
                        c = light
                    if x == 3 or y == 3:
                        c = dark
                    put(img, tx * 4 + x, ty * 4 + y, c)
            if rng.random() < (0.6 if bright else 0.35):
                put(img, tx * 4 + 1, ty * 4 + 1, CRYSTAL[4])
    return img


SHARD = [
    "................",
    "................",
    "........00......",
    ".......0440.....",
    "......044330....",
    ".....04433220...",
    "....0443322210..",
    "...04332222110..",
    "..0433222m1110..",
    "..03222mm11100..",
    "...0221m1100....",
    "....0211100.....",
    ".....01100......",
    "......000.......",
    "................",
    "................",
]


def pixel_shard():
    img = new(fill=(0, 0, 0, 0))
    colors = {"0": (14, 52, 60), "1": CRYSTAL[1], "2": CRYSTAL[2], "3": CRYSTAL[3], "4": CRYSTAL[4], "m": VIOLET[2]}
    for y, row in enumerate(SHARD):
        for x, ch in enumerate(row):
            if ch in colors:
                put(img, x, y, colors[ch])
    return img


def pixel_hollows_map(seed=906):
    """An unmarked parchment map, folded in three, stamped with a teal crystal."""
    img = new(fill=(0, 0, 0, 0))
    paper = bs.surface([(196, 170, 120), (214, 190, 140), (228, 206, 160)], seed, weights=[1, 4, 1], spread=0.5)
    for y in range(2, 14):
        for x in range(1, 15):
            c = paper(x, y)
            if x in (5, 10):
                c = shade(c, -26)  # the folds
            put(img, x, y, c)
    for x in range(1, 15):
        put(img, x, 2, (150, 124, 82))
        put(img, x, 13, (122, 98, 62))
    for y in range(2, 14):
        put(img, 1, y, (150, 124, 82))
        put(img, 14, y, (122, 98, 62))
    for x, y, c in ((7, 5, CRYSTAL[3]), (8, 5, CRYSTAL[3]), (6, 6, CRYSTAL[2]), (7, 6, CRYSTAL[4]), (8, 6, CRYSTAL[3]),
                    (9, 6, CRYSTAL[2]), (6, 7, CRYSTAL[1]), (7, 7, CRYSTAL[2]), (8, 7, VIOLET[2]), (9, 7, CRYSTAL[1]),
                    (7, 8, CRYSTAL[1]), (8, 8, CRYSTAL[1])):
        put(img, x, y, c)
    for x in (4, 6, 9, 11):
        put(img, x, 11, (120, 72, 40))  # a dotted trail
    return img


def map_marker():
    """8x8 map icon: a little teal crystal with a dark outline."""
    rows = ["...00...",
            "..0430..",
            ".043320.",
            "04332210",
            "0m32211.",
            ".0m2110.",
            "..0110..",
            "...00..."]
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    colors = {"0": (10, 30, 36), "1": CRYSTAL[1], "2": CRYSTAL[2], "3": CRYSTAL[3], "4": CRYSTAL[4], "m": VIOLET[2]}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                put(img, x, y, colors[ch])
    return img


# ---------------------------------------------------------------- the arcade cabinet

def flat(seed, color, noise=6):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, shade(color, rng.randint(-noise, noise)))
    return img


def side_art(seed=910):
    """Black laminate with 1980s-style diagonal neon stripes."""
    img = flat(seed, (22, 20, 28), 4)
    for y in range(16):
        for x in range(16):
            d = (x + y) % 16
            if d in (5, 6):
                put(img, x, y, (232, 60, 180))
            elif d == 8:
                put(img, x, y, (60, 220, 232))
            elif d == 10:
                put(img, x, y, (250, 210, 70))
    return img


def control_deck(seed=911):
    """Deep purple deck art with two pinstripes and a few stars."""
    rng = random.Random(seed)
    img = flat(seed, (54, 30, 84), 5)
    for x in range(16):
        put(img, x, 1, (232, 60, 180))
        put(img, x, 14, (60, 220, 232))
    for _ in range(6):
        put(img, rng.randint(1, 14), rng.randint(3, 12), (250, 240, 200))
    return img


def coin_door(seed=912):
    """A steel coin door on the black body: two lit coin slots, two return buttons and a lock."""
    img = flat(seed, (22, 20, 28), 4)
    for y in range(3, 13):
        for x in range(3, 13):
            c = (132, 136, 144)
            if x == 3 or y == 3:
                c = (176, 180, 188)
            elif x == 12 or y == 12:
                c = (84, 88, 96)
            put(img, x, y, c)
    for x in (6, 9):
        for y in (5, 6, 7):
            put(img, x, y, (255, 150, 40) if y != 5 else (255, 210, 120))
        put(img, x, 9, (200, 40, 30))
    put(img, 8, 11, (40, 40, 44))
    return img


def screen(seed=913):
    """A CRT showing an original little game: rows of blocky invaders-from-nowhere, a ship, ground and a score."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (10, 12, 34) if y % 2 else (16, 18, 46)
            put(img, x, y, c)
    for x in (1, 2, 4, 5, 6):
        put(img, x, 1, (240, 240, 250))
    colors = [(240, 80, 120), (250, 200, 70), (90, 220, 120)]
    for row in range(3):
        for col in range(4):
            if rng.random() < 0.85:
                x, y = 2 + col * 3, 3 + row * 2
                put(img, x, y, colors[row])
                put(img, x + 1, y, colors[row])
    put(img, 7, 9, (250, 250, 120))
    for x, y in ((6, 12), (7, 12), (8, 12), (7, 11)):
        put(img, x, y, (90, 200, 250))
    for x in range(16):
        put(img, x, 14, (50, 190, 70))
    for i in range(16):
        put(img, i, 0, (36, 36, 44))
        put(img, i, 15, (36, 36, 44))
        put(img, 0, i, (36, 36, 44))
        put(img, 15, i, (36, 36, 44))
    return img


def marquee(seed=914):
    """A lit marquee: orange-to-yellow glow, a border of bulbs and a magenta zigzag with stars."""
    img = new()
    for y in range(16):
        for x in range(16):
            t = y / 15
            put(img, x, y, (255, int(140 + 90 * t), int(40 + 60 * t)))
    for x in range(0, 16, 2):
        put(img, x, 0, (255, 255, 230))
        put(img, x + 1, 15, (255, 255, 230))
    for x in range(1, 15):
        y = 7 + (1 if x % 4 in (1, 2) else -1)
        put(img, x, y, (200, 30, 150))
        put(img, x, y + 1, (130, 20, 110))
    for x, y in ((3, 3), (12, 4), (7, 11), (13, 12)):
        put(img, x, y, (255, 255, 255))
    return img


def button(color, seed):
    img = flat(seed, color, 3)
    for y in range(8):
        for x in range(8):
            put(img, x, y, shade(color, 40))
    return img


# ---------------------------------------------------------------- the Retro Trader (64x64 villager overlay)

CLEAR = (0, 0, 0, 0)
HAIR = [(70, 42, 24), (96, 60, 34), (122, 80, 46)]
BEARD = [(78, 48, 28), (100, 64, 38)]
GLASSES = (18, 18, 20)
EYE_WHITE, EYE_GREEN = (236, 236, 228), (52, 150, 60)
SKIN = (196, 146, 116)
FLANNEL = [(178, 34, 34), (112, 22, 24), (28, 22, 22)]
TEE = [(236, 234, 228), (214, 212, 206)]
CHAIN, CROSS = (112, 114, 122), (148, 152, 162)
STRAP = [(70, 76, 48), (92, 100, 64)]
PACK = [(60, 66, 42), (84, 92, 58), (104, 112, 72)]
DENIM = [(42, 54, 96), (28, 34, 62), (74, 90, 136)]
SHOE, SOLE = (34, 34, 38), (228, 228, 228)


def strand(tones, x, y, period=5):
    """Hair or beard in neat strands: the first tone, the second on a regular slant."""
    return tones[1] if (x + 2 * y) % period == 0 else tones[0]


def flannel(x, y):
    """Buffalo check: red, black where the dark bands cross, dark red where they do not."""
    a, b = (x // 2) % 2, (y // 2) % 2
    return FLANNEL[2] if a and b else FLANNEL[0] if not a and not b else FLANNEL[1]


def denim(x, y):
    """Navy plaid trousers."""
    if x % 4 == 3 and y % 4 == 3:
        return DENIM[2]
    if x % 4 == 3 or y % 4 == 3:
        return DENIM[1]
    return DENIM[0]


def box_faces(u, v, w, h, d):
    """Texture rectangles (x, y, width, height) of a model box at texture offset (u, v)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def fill(img, rect, color_at):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            color = color_at(x, y)
            if color is not None:
                put(img, x0 + x, y0 + y, color if len(color) > 3 else tuple(color) + (255,))


def retro_trader():
    img = Image.new("RGBA", (64, 64), CLEAR)

    # Head (texture offset 0,0; 8x10x8): hair on top and the back, brows, glasses with green eyes, beard.
    head = box_faces(0, 0, 8, 10, 8)
    fill(img, head["top"], lambda x, y: strand(HAIR[1:], x, y))
    fill(img, head["bottom"], lambda x, y: strand(BEARD, x, y, 4))
    fill(img, head["back"], lambda x, y: HAIR[1 + (x % 3 == 0)] if y < 7 else None)

    def face(x, y):
        if y == 0:
            return HAIR[1]
        if y == 2 and x in (1, 2, 5, 6):
            return HAIR[0]
        if y in (3, 5):
            return GLASSES
        if y == 4:
            return {0: GLASSES, 1: EYE_WHITE, 2: EYE_GREEN, 3: GLASSES, 4: GLASSES, 5: EYE_WHITE, 6: EYE_GREEN,
                    7: GLASSES}[x]
        if y >= 7 or (y == 6 and x in (0, 7)):
            return strand(BEARD, x, y, 4)
        return None
    fill(img, head["front"], face)

    def side(front_at_high_u):
        def paint(x, y):
            toward_front = x if front_at_high_u else 7 - x
            if y < 3 or (toward_front < 3 and y < 7):
                return strand(HAIR[1:], x, y)
            if y == 4 and toward_front >= 3:
                return GLASSES  # the arm of the glasses
            if y >= 5 and toward_front >= 4:
                return strand(BEARD, x, y, 4)  # sideburns into the beard
            return None
        return paint
    fill(img, head["right"], side(True))
    fill(img, head["left"], side(False))

    # Hat layer (32,0): fuller hair on top, a fringe at the front, longer at the back.
    hat = box_faces(32, 0, 8, 10, 8)
    fill(img, hat["top"], lambda x, y: HAIR[2] if (x + 2 * y) % 5 == 0 else HAIR[1])
    fill(img, hat["front"], lambda x, y: strand(HAIR[1:], x, y) if y == 0 or (y == 1 and x not in (3, 4)) else None)
    fill(img, hat["right"], lambda x, y: HAIR[1] if y < 2 else None)
    fill(img, hat["left"], lambda x, y: HAIR[1] if y < 2 else None)
    fill(img, hat["back"], lambda x, y: HAIR[1 + (x % 3 == 0)] if y < 6 else None)

    # Nose (24,0; 2x4x2): a moustache along its bottom.
    nose = box_faces(24, 0, 2, 4, 2)
    for name in ("front", "right", "left"):
        fill(img, nose[name], lambda x, y: BEARD[0] if y == 3 else None)
    fill(img, nose["bottom"], lambda x, y: BEARD[0])

    def torso_front(x, y):
        """Flannel open over a white tee, backpack straps, a silver cross on a chain; trousers below the belt."""
        if y >= 12:
            return denim(x, y)
        if x in (1, 6) and y <= 10:
            return STRAP[y % 2]
        if x in (0, 7):
            return flannel(x, y)
        chain = {(2, 0), (2, 1), (3, 2), (5, 0), (5, 1), (4, 2)}
        cross = {(3, 3), (4, 3), (3, 4), (4, 4), (3, 5), (4, 5), (3, 6), (4, 6), (2, 4), (5, 4)}
        if (x, y) in cross:
            return CROSS
        if (x, y) in chain:
            return CHAIN
        return TEE[1] if y == 11 else TEE[0]

    def torso_back(x, y):
        if y >= 12:
            return denim(x, y)
        if 1 <= x <= 6 and 1 <= y <= 10:
            if y == 1 or x in (1, 6):
                return PACK[2] if y == 1 else PACK[0]
            if y == 6:
                return PACK[0]  # the pocket's seam
            return PACK[1]
        return flannel(x, y)

    def torso_side(x, y):
        if y >= 12:
            return denim(x, y)
        return flannel(x, y)

    # Body (16,20; 8x12x6) and the robe layer over it (0,38; 8x20x6): the robe becomes shirt and trousers.
    for faces, height in ((box_faces(16, 20, 8, 12, 6), 12), (box_faces(0, 38, 8, 20, 6), 20)):
        fill(img, faces["front"], torso_front)
        fill(img, faces["back"], torso_back)
        fill(img, faces["right"], torso_side)
        fill(img, faces["left"], torso_side)
        fill(img, faces["top"], lambda x, y: STRAP[0] if x in (1, 6) else flannel(x, y))
        fill(img, faces["bottom"], lambda x, y: denim(x, y))

    # Arms, folded (44,22; 4x8x4 each, the left mirrored) and the forearms across the chest (40,38; 8x4x4):
    # flannel sleeves, hands clasped in the middle.
    arm = box_faces(44, 22, 4, 8, 4)
    for name in ("top", "front", "back", "right", "left"):
        fill(img, arm[name], flannel)
    fill(img, arm["bottom"], lambda x, y: SKIN)
    forearms = box_faces(40, 38, 8, 4, 4)
    for name in ("top", "bottom", "back", "right", "left"):
        fill(img, forearms[name], flannel)
    fill(img, forearms["front"], lambda x, y: SKIN if x in (3, 4) else flannel(x, y))

    # Legs (0,22; 4x12x4): trousers, then black sneakers with white soles and toe caps.
    leg = box_faces(0, 22, 4, 12, 4)

    def leg_side(toe):
        def paint(x, y):
            if y < 8:
                return denim(x, y)
            if y == 11:
                return SOLE
            if toe and y == 10:
                return SOLE
            return shade(SHOE, 18) if y == 8 else SHOE
        return paint
    fill(img, leg["front"], leg_side(True))
    for name in ("right", "left", "back"):
        fill(img, leg[name], leg_side(False))
    fill(img, leg["top"], denim)
    fill(img, leg["bottom"], lambda x, y: SOLE)
    return img


VILLAGER_META = {"villager": {"hat": "full"}}


def draw_all():
    save(circuitstone(), "block/circuitstone")
    save(polished_circuitstone(), "block/polished_circuitstone")
    save(circuitstone_bricks(), "block/circuitstone_bricks")
    save(pixel_lamp(), "block/pixel_lamp")
    save(crystal(904), "block/ph_crystal")
    save(crystal(905, bright=True), "block/ph_crystal_tip")
    save(pixel_shard(), "item/pixel_shard")
    save(pixel_hollows_map(), "item/pixel_hollows_map")
    save(map_marker(), "map/decorations/pixel_hollows")
    save(flat(915, (22, 20, 28), 4), "block/rt_black")
    save(flat(916, (42, 42, 50), 3), "block/rt_bezel")
    save(side_art(), "block/rt_side_art")
    save(control_deck(), "block/rt_control_deck")
    save(coin_door(), "block/rt_coin_door")
    save(screen(), "block/rt_screen")
    save(marquee(), "block/rt_marquee")
    save(button((200, 36, 36), 917), "block/rt_button_red")
    save(button((236, 196, 40), 918), "block/rt_button_yellow")
    save(button((44, 96, 220), 919), "block/rt_button_blue")
    trader = retro_trader()
    for kind in ("villager", "zombie_villager"):
        save(trader, f"entity/{kind}/profession/retro_trader")
        (TEX / "entity" / kind / "profession" / "retro_trader.png.mcmeta").write_text(
            json.dumps(VILLAGER_META, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    draw_all()
