"""Original 16x16 textures for the Drone Depot (docs/features/drone-depot.md). MIT, like the rest.

The depot uses the plan's sci-fi look: white and grey composite panels, dark navy glass, cyan
status light strips and amber hazard marks. Drawn procedurally and deterministically, like
generate_textures.py, which calls draw_all().
"""
import math
import random

from PIL import Image

from generate_textures import TEX, new, save

WHITE = [(150, 156, 164), (186, 192, 199), (214, 219, 225), (232, 236, 240), (246, 248, 250)]
NAVY = [(10, 16, 30), (16, 26, 46), (24, 38, 64), (34, 54, 86)]
CYAN = [(30, 150, 180), (70, 210, 235), (170, 245, 255)]
# The Drone Tower's dull red glow, used by the pads, pickups and control screens (the tower look).
GLOW = [(112, 30, 24), (170, 48, 36), (226, 84, 62)]
AMBER = [(200, 120, 20), (240, 170, 40), (255, 214, 110)]
ASPHALT = [(40, 42, 46), (48, 50, 55), (56, 58, 63), (64, 66, 71)]
STEEL = [(70, 74, 80), (96, 100, 107), (122, 126, 133), (150, 154, 160)]
COPPER = [(120, 56, 32), (176, 92, 54), (222, 136, 90)]
WOOD = [(96, 66, 36), (130, 92, 52), (166, 122, 74)]
ALU = [(150, 156, 164), (196, 202, 210), (232, 236, 242)]
PCB = [(20, 70, 40), (30, 100, 56), (44, 130, 72)]


def px(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c) + (255,))


def panel_block(seed, palette=WHITE, strip=True):
    """White composite panel with seams, rivets and an optional cyan light strip."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = palette[3] if x + y < 9 and (x + y) % 4 == 1 else palette[2]
            if x in (0, 15) or y in (0, 15):
                c = palette[0]
            elif x == 7 or y == 7:
                c = palette[1]
            px(img, x, y, c)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, x, y, palette[1])
    if strip:
        for x in range(3, 13):
            px(img, x, 11, CYAN[1] if x % 3 else CYAN[2])
    return img


def terminal_screen(seed):
    """Navy glass readout with cyan lines and a small fleet grid."""
    rnd = random.Random(seed)
    img = panel_block(seed, strip=False)
    for y in range(2, 14):
        for x in range(2, 14):
            px(img, x, y, NAVY[1] if (x + y) % 7 else NAVY[2])
    for x in range(3, 13):
        px(img, x, 3, CYAN[1])
    for y in (5, 7, 9):
        length = rnd.randint(4, 9)
        for x in range(3, 3 + length):
            px(img, x, y, CYAN[0])
    for gx in range(3):
        for gy in range(2):
            px(img, 10 + gx, 9 + gy * 2, CYAN[2] if rnd.random() < 0.6 else AMBER[1])
    for x in range(2, 14):
        px(img, x, 13, NAVY[3])
    return img


def terminal_top(seed):
    img = panel_block(seed, strip=False)
    for x in range(5, 11):
        for y in range(5, 11):
            px(img, x, y, NAVY[2])
    px(img, 7, 7, CYAN[2])
    px(img, 8, 8, CYAN[2])
    return img


def platform(seed):
    """Steel deck plate with a diamond tread."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = STEEL[1] if rnd.random() < 0.8 else STEEL[0]
            if (x + y) % 4 == 0 and (x - y) % 4 == 0:
                c = STEEL[3]
            elif (x + y) % 4 == 0 or (x - y) % 4 == 0:
                c = STEEL[2] if rnd.random() < 0.5 else c
            if x == 0 or y == 0:
                c = STEEL[0]
            px(img, x, y, c)
    return img


BLACK = [(12, 13, 16), (17, 18, 22), (22, 23, 28), (30, 32, 38)]
PAD_TILES = 5


def formed_pad(seed):
    """A formed 5x5 landing pad as one 80x80 picture: a black deck with a hazard-striped rim, cyan
    edge lights, a white touchdown circle and the charger port in the middle. Sliced into 25 tiles."""
    rnd = random.Random(seed)
    size = 16 * PAD_TILES
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            col = BLACK[rnd.randrange(3)]
            # Faint panel seams every 16 px so the pad reads as bolted plates.
            if x % 16 == 0 or y % 16 == 0:
                col = BLACK[0]
            img.putpixel((x, y), col + (255,))

    def put(x, y, col):
        if 0 <= x < size and 0 <= y < size:
            img.putpixel((x, y), tuple(col) + (255,))

    # Hazard rim: 3 px amber/black diagonal stripes round the outside.
    for y in range(size):
        for x in range(size):
            if min(x, y, size - 1 - x, size - 1 - y) < 3:
                put(x, y, AMBER[1] if ((x + y) // 4) % 2 == 0 else BLACK[0])
    # Cyan edge light strip just inside the rim, brighter every 8 px.
    for i in range(5, size - 5):
        for x, y in ((i, 5), (i, size - 6), (5, i), (size - 6, i)):
            put(x, y, GLOW[2] if i % 8 == 0 else GLOW[0])
    # White corner brackets.
    for cx, cy, sx, sy in ((9, 9, 1, 1), (size - 10, 9, -1, 1), (9, size - 10, 1, -1), (size - 10, size - 10, -1, -1)):
        for k in range(7):
            put(cx + sx * k, cy, WHITE[3])
            put(cx, cy + sy * k, WHITE[3])
    # Dashed white touchdown circle.
    for step in range(360):
        a = math.radians(step)
        if (step // 10) % 2 == 0:
            for r in (26.0, 26.6):
                put(round(c + r * math.cos(a)), round(c + r * math.sin(a)), WHITE[2])
    # Charger port: steel ring, dark socket, glowing cyan contact ring and four contact pins.
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if d <= 11.5:
                if d > 9.5:
                    put(x, y, STEEL[2] if (x + y) % 3 else STEEL[3])
                elif d > 8.5:
                    put(x, y, STEEL[0])
                elif d > 6.8:
                    put(x, y, NAVY[1])
                elif d > 5.6:
                    put(x, y, GLOW[1])
                elif d > 2.2:
                    put(x, y, NAVY[0])
                else:
                    put(x, y, GLOW[2])
    for a in range(4):
        ang = math.radians(45 + 90 * a)
        for r in (3.6, 4.4):
            put(round(c + r * math.cos(ang)), round(c + r * math.sin(ang)), GLOW[2])
    # Amber arrows pointing in to the port from the four sides.
    for k in range(4):
        for w in range(-k, k + 1):
            put(round(c) + w, 16 + k, AMBER[2])
            put(round(c) + w, size - 17 - k, AMBER[2])
            put(16 + k, round(c) + w, AMBER[2])
            put(size - 17 - k, round(c) + w, AMBER[2])
    return img


def formed_pad_side(seed):
    """Side and underside of pad plates: black with a thin cyan line."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            px(img, x, y, BLACK[rnd.randrange(3)])
    for x in range(16):
        px(img, x, 14, GLOW[0])
    return img


def loose_pad(seed):
    """A loose pad plate (not yet part of a complete 5x5): black with amber corner marks and a dim
    grey cross, so it clearly isn't a working pad yet."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            px(img, x, y, BLACK[rnd.randrange(3)] if x not in (0, 15) and y not in (0, 15) else BLACK[3])
    for k in range(3):
        for x, y in ((1 + k, 1), (1, 1 + k), (14 - k, 1), (14, 1 + k), (1 + k, 14), (1, 14 - k), (14 - k, 14), (14, 14 - k)):
            px(img, x, y, AMBER[1])
    for i in range(5, 11):
        px(img, i, 7, STEEL[1])
        px(img, i, 8, STEEL[1])
        px(img, 7, i, STEEL[1])
        px(img, 8, i, STEEL[1])
    return img


# ---------------------------------------------------------------- supply pickup

PICKUP_TILES = 3
YELLOW = [(180, 140, 20), (226, 182, 40), (250, 214, 90)]


def formed_pickup(seed):
    """A formed 3x3 supply pickup as one 48x48 picture: yellow and black hazard border, dark deck,
    arrows pointing in, and the closed lift hatch (two steel doors, seam down the middle) in the
    centre tile. Sliced into 9 tiles; the client draws the doors opening over it."""
    rnd = random.Random(seed)
    size = 16 * PICKUP_TILES
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))

    def put(x, y, col):
        if 0 <= x < size and 0 <= y < size:
            img.putpixel((x, y), tuple(col) + (255,))

    for y in range(size):
        for x in range(size):
            put(x, y, ASPHALT[rnd.randrange(3)])
            if min(x, y, size - 1 - x, size - 1 - y) < 4:
                put(x, y, YELLOW[1] if ((x + y) // 4) % 2 == 0 else BLACK[0])
    # Arrows pointing in to the hatch from each side.
    c = size // 2
    for k in range(4):
        for w in range(-k, k + 1):
            put(c + w - 1, 9 + k, YELLOW[2])
            put(c + w - 1, size - 10 - k, YELLOW[2])
            put(9 + k, c + w - 1, YELLOW[2])
            put(size - 10 - k, c + w - 1, YELLOW[2])
    # The hatch: a steel frame, two doors meeting in the middle, amber stripes and cyan lamps.
    x0, y0 = 16, 16
    for y in range(16):
        for x in range(16):
            col = STEEL[1] if (x + y) % 5 else STEEL[2]
            if x in (0, 15) or y in (0, 15):
                col = STEEL[0]
            elif x in (1, 14) or y in (1, 14):
                col = BLACK[1]
            elif x in (7, 8):
                col = BLACK[0] if x == 7 else STEEL[3]
            elif 5 <= y <= 10 and ((x + y) // 2) % 2 == 0:
                col = AMBER[1]
            put(x0 + x, y0 + y, col)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        put(x0 + x, y0 + y, STEEL[3])
    for x, y in ((-2, -2), (17, -2), (-2, 17), (17, 17)):
        put(x0 + x, y0 + y, GLOW[2])
    return img


def loose_pickup(seed):
    """A loose supply pickup plate: dark deck with yellow corner marks and a small crate symbol."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            px(img, x, y, ASPHALT[rnd.randrange(3)] if x not in (0, 15) and y not in (0, 15) else BLACK[3])
    for k in range(3):
        for x, y in ((1 + k, 1), (1, 1 + k), (14 - k, 1), (14, 1 + k), (1 + k, 14), (1, 14 - k), (14 - k, 14), (14, 14 - k)):
            px(img, x, y, YELLOW[1])
    for y in range(5, 11):
        for x in range(5, 11):
            px(img, x, y, AMBER[0] if x in (5, 10) or y in (5, 10) else WOOD[1])
    return img


def pickup_side(seed):
    img = new()
    for y in range(16):
        for x in range(16):
            px(img, x, y, YELLOW[1] if ((x + y) // 3) % 2 == 0 else BLACK[0])
    return img


# ---------------------------------------------------------------- control room screen

SCREEN_W, SCREEN_H, SCREEN_FRAMES = 3, 2, 8


def formed_screen_frames(seed):
    """The formed 3x2 control screen, 48x32, as SCREEN_FRAMES animation frames: dark navy glass with a
    faint grid, a bezel with cyan corner brackets, and a scan line sweeping down. The live readout text
    is drawn over it by the client."""
    w, h = 16 * SCREEN_W, 16 * SCREEN_H
    base = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    for y in range(h):
        for x in range(w):
            col = NAVY[0]
            if x % 8 == 0 or y % 8 == 0:
                col = (14, 28, 44)
            if x in (0, w - 1) or y in (0, h - 1):
                col = STEEL[0]
            elif x in (1, w - 2) or y in (1, h - 2):
                col = (8, 12, 20)
            base.putpixel((x, y), col + (255,))
    for cx, cy, sx, sy in ((2, 2, 1, 1), (w - 3, 2, -1, 1), (2, h - 3, 1, -1), (w - 3, h - 3, -1, -1)):
        for k in range(4):
            base.putpixel((cx + sx * k, cy), GLOW[1] + (255,))
            base.putpixel((cx, cy + sy * k), GLOW[1] + (255,))
    frames = []
    for f in range(SCREEN_FRAMES):
        img = base.copy()
        line = 3 + (f * (h - 6)) // SCREEN_FRAMES
        for x in range(2, w - 2):
            img.putpixel((x, line), (26, 70, 90, 255))
        frames.append(img)
    return frames


def loose_screen(seed):
    """A loose control screen panel: dark glass showing a dim standby pattern."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            col = NAVY[0]
            if x in (0, 15) or y in (0, 15):
                col = STEEL[0]
            px(img, x, y, col)
    for y in (4, 7, 10):
        for x in range(3, 3 + rnd.randint(5, 10)):
            px(img, x, y, GLOW[0])
    px(img, 12, 12, AMBER[1])
    return img


def screen_side(seed):
    """Plain steel side, lit along its top edge and shaded along its bottom."""
    img = new()
    for y in range(16):
        for x in range(16):
            px(img, x, y, STEEL[2] if y == 0 else STEEL[0] if y == 15 else STEEL[1])
    return img


# ---------------------------------------------------------------- drones (entity texture)


def drone_sheet(seed):
    """The 128x128 texture for flying drones and the pickup lift, in 16x16 regions (column, row):
    row 0: bodies of tiers 1-8; row 1: tier 9 body, rotor, crate, hatch door, lift platform, arm, duct,
    shaft; row 2: plain white (for tinting), aerostat envelope, wing, coil, fin, glass, gondola, big
    three-bladed rotor."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))

    def region(col, row, fn):
        for y in range(16):
            for x in range(16):
                col_ = fn(x, y)
                if col_ is not None:
                    img.putpixel((col * 16 + x, row * 16 + y), tuple(col_) + (255,))

    def body(main, trim, light):
        def fn(x, y):
            if x in (0, 15) or y in (0, 15):
                return main[0]
            if y in (6, 9):
                return trim
            if 6 < y < 9 and 5 <= x <= 10:
                return light
            return main[2] if rnd.random() < 0.8 else main[3]
        return fn

    GUN = [(46, 50, 58), (62, 66, 74), (78, 82, 90), (92, 96, 104)]
    GRAPHITE = [(22, 22, 28), (34, 34, 42), (46, 46, 56), (58, 58, 70)]
    VIOLET = [(120, 70, 200), (176, 108, 255), (220, 180, 255)]
    region(0, 0, body(WHITE, CYAN[1], CYAN[2]))
    region(1, 0, body([(90, 94, 100), (120, 124, 130), (150, 154, 160), (170, 174, 180)], AMBER[1], AMBER[2]))
    region(2, 0, body([(170, 90, 30), (210, 120, 40), (236, 236, 240), (246, 246, 250)], (220, 110, 30), CYAN[2]))
    region(3, 0, body([NAVY[0], NAVY[1], NAVY[2], NAVY[3]], CYAN[1], CYAN[2]))
    region(4, 0, body([(150, 156, 164), (200, 206, 214), (222, 228, 236), (236, 240, 246)], (40, 110, 200), CYAN[2]))
    region(5, 0, body(GUN, AMBER[1], AMBER[2]))
    region(6, 0, body(NAVY, CYAN[0], CYAN[2]))
    region(7, 0, body(GUN, CYAN[1], CYAN[2]))
    region(0, 1, body(GRAPHITE, VIOLET[1], VIOLET[2]))

    def rotor(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d > 7.6:
            return None
        if abs(x - 7.5) < 1.2 or abs(y - 7.5) < 1.2:
            return STEEL[1] if d > 1.5 else STEEL[0]
        return None
    region(1, 1, rotor)

    def crate(x, y):
        if x in (0, 15) or y in (0, 15):
            return AMBER[0]
        if x in (3, 12) or y in (3, 12):
            return WOOD[0]
        return WOOD[1] if (x + 2 * y) % 7 else WOOD[2]
    region(2, 1, crate)

    def door(x, y):
        if x in (0, 15) or y in (0, 15):
            return STEEL[0]
        if 5 <= y <= 10 and ((x + y) // 2) % 2 == 0:
            return AMBER[1]
        return STEEL[1] if (x + y) % 5 else STEEL[2]
    region(3, 1, door)
    region(4, 1, lambda x, y: STEEL[0] if x in (0, 15) or y in (0, 15) else STEEL[2] if (x // 4 + y // 4) % 2 else STEEL[1])
    region(5, 1, lambda x, y: (40, 44, 50) if (x + y) % 4 else (60, 64, 70))
    region(6, 1, lambda x, y: WHITE[2] if y not in (0, 15) else CYAN[1])
    region(7, 1, lambda x, y: (6, 8, 12) if (x + y) % 5 else (14, 16, 20))

    region(0, 2, lambda x, y: (255, 255, 255))
    region(1, 2, lambda x, y: WHITE[1] if x % 8 == 0 or y % 8 == 0 else WHITE[3] if rnd.random() < 0.85 else WHITE[2])
    region(2, 2, lambda x, y: GUN[0] if x % 5 == 0 else GUN[2] if rnd.random() < 0.85 else GUN[1])
    region(3, 2, lambda x, y: VIOLET[(x + y) % 3] if (x + y) % 2 else COPPER[1])
    region(4, 2, lambda x, y: STEEL[0] if x in (0, 15) or y in (0, 15) else STEEL[2])
    region(5, 2, lambda x, y: (20, 60, 80) if (x + y) % 6 else (60, 150, 180))
    region(6, 2, body(NAVY, AMBER[1], CYAN[2]))

    def big_rotor(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d > 7.8:
            return None
        a = math.atan2(y - 7.5, x - 7.5)
        for k in range(3):
            if abs((a - k * 2 * math.pi / 3 + math.pi) % (2 * math.pi) - math.pi) < 0.22:
                return STEEL[2] if d > 2 else STEEL[0]
        return STEEL[0] if d < 1.6 else None
    region(7, 2, big_rotor)
    return img


# Drone item icons (top-down), in the fleet's look: dark hulls, dark rotors, dull red lights.
HULL4 = [(90, 94, 100), (124, 128, 134), (150, 154, 160)]
HULL5 = [(52, 54, 60), (72, 74, 82), (92, 94, 102)]
HULL6 = [(30, 31, 34), (44, 46, 50), (58, 60, 64)]
HULL7 = [(44, 48, 58), (62, 68, 80), (82, 88, 100)]
HULL8 = [(40, 45, 38), (58, 64, 54), (76, 84, 70)]
HULL9 = [(24, 25, 28), (36, 38, 42), (50, 52, 58)]
RED = [(110, 26, 20), (160, 40, 32), (230, 72, 54)]
BLADE = [(60, 62, 68), (84, 86, 92), (104, 106, 112)]


def dark_rotor(img, cx, cy, radius=2):
    px(img, cx, cy, (24, 24, 26))
    for d in range(1, radius + 1):
        for dx, dy in ((-d, 0), (d, 0), (0, -d), (0, d)):
            px(img, cx + dx, cy + dy, BLADE[1] if d < radius else BLADE[2])


def icon_runner():
    img = new()
    for x, y in QUAD:
        for a in range(16):
            t = a * math.pi / 8
            px(img, round(x + 2.4 * math.cos(t)), round(y + 2.4 * math.sin(t)), HULL4[0])
        dark_rotor(img, x, y, 1)
    for y in range(4, 12):
        for x in range(6, 10):
            px(img, x, y, HULL4[2] if x in (7, 8) else HULL4[1])
    px(img, 7, 11, RED[2])
    px(img, 8, 11, RED[1])
    return img


def icon_tiltrotor():
    img = new()
    for y in range(2, 14):
        for x in range(7, 9):
            px(img, x, y, HULL5[2] if x == 7 else HULL5[1])
    for x in range(1, 15):
        px(img, x, 6, HULL5[0])
        px(img, x, 7, HULL5[1])
    for cx in (2, 13):
        dark_rotor(img, cx, 6, 2)
    px(img, 6, 13, HULL5[0])
    px(img, 9, 13, HULL5[0])
    px(img, 7, 2, RED[2])
    return img


def icon_tandem():
    img = new()
    for y in range(2, 14):
        for x in range(6, 10):
            px(img, x, y, HULL6[0] if x in (6, 9) else HULL6[2])
    for cy in (3, 12):
        for d in range(-4, 5):
            px(img, 8 + d, cy, BLADE[2])
            px(img, 8, cy + d // 2, BLADE[1])
    px(img, 7, 7, RED[1])
    px(img, 8, 13, (214, 170, 38))
    return img


def icon_aerostat():
    img = new()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) / 5.5) ** 2 + ((y - 7.5) / 7.2) ** 2
            if d <= 1:
                px(img, x, y, HULL7[0] if d > 0.75 else HULL7[2] if (y % 5) else HULL7[1])
    for y in range(3, 13):
        px(img, 2, y, RED[0])
        px(img, 13, y, RED[0])
    for x, y in ((1, 4), (14, 4), (1, 11), (14, 11)):
        dark_rotor(img, x, y, 1)
    return img


def icon_glider():
    img = new()
    for i in range(8):
        for x in range(7 - i, 9 + i):
            px(img, x, 4 + i // 2 + (i > 5), HULL8[1])
        px(img, 7 - i, 4 + i // 2 + (i > 5), RED[1])
        px(img, 8 + i, 4 + i // 2 + (i > 5), RED[1])
    for y in range(3, 11):
        px(img, 7, y, HULL8[2])
        px(img, 8, y, HULL8[2])
    for x in (3, 12):
        px(img, x, 7, (20, 20, 22))
    return img


def icon_ring_lifter():
    img = new()
    for a in range(72):
        t = a * math.pi / 36
        for r in (5.4, 6.2):
            px(img, round(7.5 + r * math.cos(t)), round(7.5 + r * math.sin(t)), HULL9[2] if r > 6 else HULL9[1])
        px(img, round(7.5 + 6.9 * math.cos(t)), round(7.5 + 6.9 * math.sin(t)), RED[0])
    for k in range(6):
        t = k * math.pi / 3 + math.pi / 6
        dark_rotor(img, round(7.5 + 5.8 * math.cos(t)), round(7.5 + 5.8 * math.sin(t)), 1)
    for y in range(6, 10):
        for x in range(6, 10):
            px(img, x, y, HULL9[1] if (x + y) % 3 else (214, 170, 38))
    px(img, 7, 7, RED[2])
    return img


# ---------------------------------------------------------------- control room


# The drone fleet's look (tower theme): dark matte military hulls in the material of the tower tier that
# unlocks each drone, dull red lights only. 32x32 regions in the parts of the 256x256 entity texture the
# 16x16 regions above don't use; DroneModel.java names the same slots (checked by check_mod_data.py).
FLEET_PANELS = [
    # name, base colour, seams, noise, stripes
    ("alu", (153, 158, 166), True, 6, None), ("alu_dark", (101, 106, 114), True, 6, None),
    ("tungsten", (85, 88, 96), True, 6, None), ("tungsten_dark", (59, 62, 70), True, 6, None),
    ("carbon", (52, 54, 59), True, 3, None), ("carbon_dark", (36, 37, 41), True, 3, None),
    ("sic", (78, 85, 98), True, 6, None), ("sic_dark", (54, 59, 70), True, 6, None),
    ("du", (70, 78, 67), True, 6, None), ("du_dark", (49, 54, 48), True, 6, None),
    ("graphene", (41, 44, 49), True, 3, None), ("graphene_dark", (28, 29, 33), True, 2, None),
    ("steel", (88, 92, 98), False, 6, None), ("black", (20, 20, 22), False, 2, None),
    ("glass", (36, 30, 32), False, 2, None), ("sensor", (16, 16, 18), False, 1, None),
    ("vent", (34, 36, 40), False, 6, (20, 20, 22)), ("hazard", (214, 170, 38), False, 3, (26, 26, 28)),
    ("glow", (255, 255, 255), False, 0, None), ("conduit", (120, 28, 26), False, 4, None),
    ("gunmetal", (91, 96, 104), True, 6, None), ("gunmetal_dark", (59, 62, 70), True, 6, None),
    ("armor_plate", (78, 83, 91), True, 6, None), ("frame", (34, 35, 38), False, 2, None),
]
FLEET_BLADES = [("blades2", 2, (78, 80, 86)), ("blades3", 3, (78, 80, 86)), ("blades4", 4, (78, 80, 86)),
                ("blades5", 5, (78, 80, 86)), ("blades6", 6, (52, 54, 58))]
FLEET_ORDER = [p[0] for p in FLEET_PANELS] + [b[0] for b in FLEET_BLADES]


def fleet_slot(index):
    """Top-left pixel of fleet region {@code index}: four columns down the right half, then the bottom left."""
    if index < 32:
        return 128 + (index % 4) * 32, (index // 4) * 32
    index -= 32
    return (index % 4) * 32, 128 + (index // 4) * 32


def fleet_regions(img, seed):
    """Draws every fleet region into the 256x256 sheet and returns {name: (x0, y0, x1, y1)}."""
    rnd = random.Random(seed)
    where = {}
    R = 32
    for i, (name, base, seams, noise, stripes) in enumerate(FLEET_PANELS):
        x0, y0 = fleet_slot(i)
        for y in range(R):
            for x in range(R):
                n = rnd.randint(-noise, noise) if noise else 0
                c = [max(0, min(255, v + n)) for v in base]
                if seams:
                    dark = [int(v * 0.62) for v in base]
                    if x == 0 or y == 0:
                        c = dark
                    elif y == R // 2 and 4 <= x < R - 4:
                        c = [int(v * 0.72) for v in base]
                    elif (x, y) in ((3, 3), (R - 4, 3), (3, R - 4), (R - 4, R - 4)):
                        c = [min(255, int(v * 1.25)) for v in base]
                if stripes and ((x + y) // 4) % 2:
                    c = list(stripes)
                img.putpixel((x0 + x, y0 + y), tuple(c) + (255,))
        where[name] = (x0, y0, x0 + R, y0 + R)
    for j, (name, count, colour) in enumerate(FLEET_BLADES):
        x0, y0 = fleet_slot(len(FLEET_PANELS) + j)
        for y in range(R):
            for x in range(R):
                dx, dy = x + 0.5 - R / 2, y + 0.5 - R / 2
                d = math.hypot(dx, dy)
                c = None
                if d <= 3:
                    c = (60, 62, 66)
                elif d <= R / 2 - 0.3:
                    a = math.atan2(dy, dx)
                    for k in range(count):
                        off = (a - k * 2 * math.pi / count + math.pi) % (2 * math.pi) - math.pi
                        if abs(off) * d < 1.9 * (1 - 0.35 * d / (R / 2)):
                            c = colour
                if c:
                    img.putpixel((x0 + x, y0 + y), tuple(c) + (255,))
        where[name] = (x0, y0, x0 + R, y0 + R)
    return where


def drone_depot_sheet(seed):
    """The 256x256 drone depot entity texture: the 16x16 regions of drone_sheet() top left, and the fleet's
    32x32 regions round them."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    img.paste(drone_sheet(seed), (0, 0))
    fleet_regions(img, seed + 1)
    return img


def tech_wall(seed):
    """Tech wall panel: white composite panel with a thin cyan light seam and corner bolts."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            col = WHITE[3] if rnd.random() < 0.8 else WHITE[2]
            if x in (0, 15) or y in (0, 15):
                col = WHITE[0]
            elif y == 11:
                col = CYAN[1] if x % 4 else CYAN[2]
            elif y == 12:
                col = WHITE[1]
            px(img, x, y, col)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, x, y, STEEL[1])
    return img


def tech_floor(seed):
    """Tech floor tile: dark grip plate with a light grid and a cyan dot at each corner."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            col = ASPHALT[rnd.randrange(3)]
            if x in (0, 15) or y in (0, 15):
                col = ASPHALT[3]
            elif (x + y) % 4 == 0 and 2 < x < 13 and 2 < y < 13:
                col = STEEL[0]
            px(img, x, y, col)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px(img, x, y, CYAN[1])
    return img


def tech_light(seed):
    """Tech ceiling light: a bright white-cyan diffuser in a steel frame."""
    img = new()
    for y in range(16):
        for x in range(16):
            col = (236, 250, 255) if 2 <= x <= 13 and 2 <= y <= 13 else STEEL[1]
            if (x in (2, 13) or y in (2, 13)) and 2 <= x <= 13 and 2 <= y <= 13:
                col = (170, 235, 255)
            px(img, x, y, col)
    return img


def tech_window(seed):
    """Tech window: translucent cyan-tinted glass in a steel frame with corner brackets."""
    img = new()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                img.putpixel((x, y), STEEL[1] + (255,))
            elif (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
                img.putpixel((x, y), CYAN[1] + (255,))
            elif x + y in (9, 10) and x < 9:
                img.putpixel((x, y), (200, 240, 255, 110))
            else:
                img.putpixel((x, y), (120, 200, 230, 60))
    return img


HOLO_TILES = 3


def formed_holo(seed):
    """A formed 3x3 hologram table top, 48x48: dark glass with a cyan grid and ring, the projector lens
    in the middle tile, and a steel rim."""
    size = 16 * HOLO_TILES
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            col = NAVY[0]
            if x % 6 == 0 or y % 6 == 0:
                col = (16, 34, 52)
            d = math.hypot(x - c, y - c)
            if 19 <= d <= 20:
                col = GLOW[0]
            if min(x, y, size - 1 - x, size - 1 - y) < 2:
                col = STEEL[1] if min(x, y, size - 1 - x, size - 1 - y) == 0 else STEEL[2]
            if d <= 5.5:
                col = STEEL[2] if d > 4.5 else GLOW[1] if d > 2.5 else GLOW[2]
            img.putpixel((x, y), col + (255,))
    return img


def loose_holo(seed):
    img = new()
    for y in range(16):
        for x in range(16):
            col = NAVY[0]
            if x in (0, 15) or y in (0, 15):
                col = STEEL[1]
            px(img, x, y, col)
    for i in range(4, 12):
        px(img, i, 8, GLOW[0])
    return img


def holo_side(seed):
    """Plain steel side with a dark slot and the table's glow line, lit along its top edge."""
    img = new()
    for y in range(16):
        for x in range(16):
            col = STEEL[2] if y == 0 else STEEL[0] if y == 15 else STEEL[1]
            if y in (4, 5):
                col = (40, 44, 50)
            if y == 5 and 3 <= x <= 12:
                col = GLOW[1]
            px(img, x, y, col)
    return img


def packager_side(seed):
    img = panel_block(seed, strip=False)
    for y in range(4, 12):
        for x in range(4, 12):
            px(img, x, y, AMBER[0] if (x + y) % 2 else AMBER[1])
    for x in range(4, 12):
        px(img, x, 4, STEEL[0])
        px(img, x, 11, STEEL[0])
    for y in range(4, 12):
        px(img, 4, y, STEEL[0])
        px(img, 11, y, STEEL[0])
    for x in range(5, 11):
        px(img, x, 13, GLOW[1])
    return img


def packager_top(seed):
    img = panel_block(seed, strip=False)
    for y in range(3, 13):
        for x in range(3, 13):
            px(img, x, y, NAVY[1])
    for x in range(4, 12):
        px(img, x, 7, AMBER[1])
        px(img, x, 8, AMBER[0])
    return img


# ---------------------------------------------------------------- items


def rotor(img, cx, cy, blade=ALU, hub=NAVY[3], ring=None):
    px(img, cx, cy, hub)
    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        px(img, cx + dx, cy + dy, blade[1])
    for dx, dy in ((-2, 0), (2, 0)):
        px(img, cx + dx, cy + dy, blade[2])
    if ring:
        for a in range(12):
            t = a * math.pi / 6
            px(img, round(cx + 2 * math.cos(t)), round(cy + 2 * math.sin(t)), ring)


def drone_icon(rotors, body_size, duct=False, body=WHITE):
    """Top-down drone: a body with a dull red status light and dark rotors around it."""
    img = new()
    c = 7.5
    for x, y in rotors:
        # Arm from the body to the rotor.
        steps = 8
        for i in range(steps + 1):
            t = i / steps
            px(img, round(c + (x - c) * t), round(c + (y - c) * t), STEEL[1])
    for x, y in rotors:
        rotor(img, x, y, blade=[(60, 62, 68), (84, 86, 92), (104, 106, 112)], hub=(24, 24, 26))
    half = body_size // 2
    for y in range(8 - half, 8 + half):
        for x in range(8 - half, 8 + half):
            edge = x in (8 - half, 8 + half - 1) or y in (8 - half, 8 + half - 1)
            px(img, x, y, body[1] if edge else body[3])
    px(img, 7, 7, (230, 72, 54))
    px(img, 8, 7, (160, 40, 32))
    return img


def motor():
    img = new()
    for y in range(4, 13):
        for x in range(5, 11):
            px(img, x, y, COPPER[(x + y) % 3] if 6 <= x <= 9 else STEEL[1])
    for x in range(5, 11):
        px(img, x, 4, STEEL[2])
        px(img, x, 12, STEEL[0])
    for y in range(1, 4):
        px(img, 8, y, STEEL[3])
    return img


def propellers(palette):
    img = new()
    for i in range(2, 14):
        px(img, i, 7, palette[1])
        px(img, i, 8, palette[0] if i % 2 else palette[2])
    for x, y in ((3, 6), (12, 9), (4, 6), (11, 9)):
        px(img, x, y, palette[2])
    px(img, 7, 7, STEEL[0])
    px(img, 8, 8, STEEL[0])
    return img


def rotor_assembly(palette):
    img = motor()
    for i in range(1, 15):
        px(img, i, 2, palette[1] if i % 3 else palette[2])
    return img


def ducted_fan():
    img = new()
    for a in range(40):
        t = a * math.pi / 20
        for r in (5, 6):
            px(img, round(7.5 + r * math.cos(t)), round(7.5 + r * math.sin(t)), WHITE[1] if r == 6 else STEEL[1])
    rotor(img, 7, 7)
    rotor(img, 8, 8)
    for x in range(5, 11):
        px(img, x, 13, CYAN[1])
    return img


def flight_controller():
    img = new()
    for y in range(3, 13):
        for x in range(3, 13):
            px(img, x, y, PCB[1] if (x + y) % 4 else PCB[2])
    for y in range(6, 10):
        for x in range(6, 10):
            px(img, x, y, NAVY[0])
    px(img, 7, 7, CYAN[2])
    for x in range(3, 13, 2):
        px(img, x, 3, AMBER[2])
        px(img, x, 12, AMBER[2])
    return img


def lead_acid_pack():
    img = new()
    for y in range(4, 14):
        for x in range(2, 14):
            px(img, x, y, STEEL[1] if 3 <= x <= 12 and 5 <= y <= 12 else STEEL[0])
    for x, y in ((4, 3), (5, 3)):
        px(img, x, y, (200, 40, 40))
    for x, y in ((10, 3), (11, 3)):
        px(img, x, y, (30, 30, 30))
    for x in range(4, 12):
        px(img, x, 8, AMBER[1])
    return img


def cargo_winch():
    img = new()
    for y in range(3, 8):
        for x in range(4, 12):
            px(img, x, y, STEEL[1] if (x + y) % 2 else STEEL[2])
    for y in range(8, 12):
        px(img, 8, y, STEEL[0])
    for x, y in ((6, 12), (7, 13), (8, 13), (9, 12), (10, 11), (6, 11)):
        px(img, x, y, AMBER[1])
    return img


QUAD = [(3, 3), (12, 3), (3, 12), (12, 12)]
HEX = [(8, 1), (14, 5), (14, 11), (8, 14), (2, 11), (2, 5)]
OCTO = [(round(7.5 + 6.5 * math.cos(a * math.pi / 4)), round(7.5 + 6.5 * math.sin(a * math.pi / 4))) for a in range(8)]


# ---------------------------------------------------------------- tier 5-9 parts (tower theme: dark, dull red)

GRAPHITE = [(28, 29, 32), (42, 44, 48), (60, 63, 68), (84, 88, 94)]
DULL_RED = [(96, 22, 18), (150, 38, 30), (220, 70, 52)]
MAGNET = [(120, 124, 130), (168, 172, 178), (210, 214, 220)]
TUNGSTEN = [(70, 72, 78), (98, 100, 108), (130, 132, 140)]
SILVER = [(150, 152, 160), (196, 198, 206), (232, 234, 240)]


def neodymium_motor():
    img = motor()
    for y in range(5, 12):
        px(img, 5, y, MAGNET[1] if y % 2 else MAGNET[2])
        px(img, 10, y, MAGNET[1] if y % 2 else MAGNET[2])
    for x in range(6, 10):
        px(img, x, 13, GRAPHITE[2])
    return img


def nacelle():
    img = new()
    for y in range(5, 13):
        for x in range(5, 11):
            px(img, x, y, GRAPHITE[2] if 6 <= x <= 9 else GRAPHITE[1])
    for i in range(1, 15):
        px(img, i, 3, ALU[1] if i % 3 else ALU[2])
    px(img, 7, 4, STEEL[2])
    px(img, 8, 4, STEEL[2])
    px(img, 8, 11, DULL_RED[2])
    return img


def composite_blades():
    img = new()
    for i in range(1, 15):
        px(img, i, 7, GRAPHITE[1])
        px(img, i, 8, GRAPHITE[2] if i % 2 else GRAPHITE[0])
        px(img, 7, i, GRAPHITE[1])
        px(img, 8, i, GRAPHITE[2] if i % 2 else GRAPHITE[0])
    for x, y in ((7, 7), (8, 8), (7, 8), (8, 7)):
        px(img, x, y, TUNGSTEN[2])
    return img


def composite_assembly():
    img = neodymium_motor()
    for i in range(1, 15):
        px(img, i, 2, GRAPHITE[1] if i % 3 else GRAPHITE[3])
    return img


def lift_cell():
    img = new()
    for y in range(3, 14):
        for x in range(3, 13):
            d = ((x - 7.5) / 5) ** 2 + ((y - 8) / 5.5) ** 2
            if d <= 1:
                px(img, x, y, GRAPHITE[2] if d > 0.7 else GRAPHITE[3])
    for x in range(4, 12):
        px(img, x, 8, DULL_RED[0])
    px(img, 7, 2, STEEL[2])
    px(img, 8, 2, STEEL[2])
    return img


def ion_emitter():
    img = new()
    for y in range(6, 12):
        for x in range(2, 14):
            px(img, x, y, GRAPHITE[1] if y > 6 else GRAPHITE[2])
    for x in range(2, 14):
        px(img, x, 4, TUNGSTEN[2])
        px(img, x, 5, DULL_RED[1] if x % 2 else DULL_RED[0])
    for x in range(4, 12, 3):
        px(img, x, 9, SILVER[1])
    return img


def sc_tape():
    img = new()
    for a in range(60):
        t = a * math.pi / 30
        r = 2 + t * 0.9
        px(img, round(7.5 + r * math.cos(t)), round(7.5 + r * math.sin(t)), SILVER[1] if a % 4 else SILVER[2])
    for a in range(20):
        t = a * math.pi / 10
        px(img, round(7.5 + 1.5 * math.cos(t)), round(7.5 + 1.5 * math.sin(t)), GRAPHITE[2])
    return img


def cryocooler():
    img = new()
    for y in range(2, 14):
        for x in range(5, 11):
            px(img, x, y, ALU[1] if x in (5, 10) else ALU[0])
    for y in range(3, 13, 2):
        for x in range(3, 13):
            px(img, x, y, ALU[2] if x in (3, 12) else ALU[1])
    for x in range(6, 10):
        px(img, x, 13, COPPER[1])
    return img


def sc_motor():
    img = new()
    for y in range(3, 14):
        for x in range(4, 12):
            px(img, x, y, GRAPHITE[1] if 5 <= x <= 10 else GRAPHITE[0])
    for x in range(4, 12):
        px(img, x, 6, DULL_RED[1])
        px(img, x, 10, DULL_RED[1])
    for y in range(1, 3):
        px(img, 7, y, STEEL[2])
        px(img, 8, y, STEEL[2])
    return img


def sc_lift_fan():
    img = new()
    for a in range(48):
        t = a * math.pi / 24
        for r in (5, 6):
            px(img, round(7.5 + r * math.cos(t)), round(7.5 + r * math.sin(t)), DULL_RED[1] if r == 6 and a % 6 == 0 else GRAPHITE[2 if r == 6 else 1])
    for a in range(6):
        t = a * math.pi / 3
        for r in range(1, 5):
            px(img, round(7.5 + r * math.cos(t)), round(7.5 + r * math.sin(t)), GRAPHITE[3])
    px(img, 7, 7, STEEL[2])
    px(img, 8, 8, STEEL[2])
    return img


def draw_all():
    save(platform(904), "block", "landing_platform")
    save(loose_pad(905), "block", "landing_pad")
    save(formed_pad_side(909), "block", "landing_pad_side")
    formed = formed_pad(910)
    for dz in range(PAD_TILES):
        for dx in range(PAD_TILES):
            tile = formed.crop((dx * 16, dz * 16, dx * 16 + 16, dz * 16 + 16))
            save(tile, "block", f"landing_pad_formed_{1 + dz * PAD_TILES + dx}")
    save(loose_pickup(911), "block", "supply_pickup")
    save(pickup_side(912), "block", "supply_pickup_side")
    formed = formed_pickup(913)
    for dz in range(PICKUP_TILES):
        for dx in range(PICKUP_TILES):
            save(formed.crop((dx * 16, dz * 16, dx * 16 + 16, dz * 16 + 16)), "block", f"supply_pickup_formed_{1 + dz * PICKUP_TILES + dx}")
    save(loose_screen(914), "block", "control_screen")
    save(screen_side(915), "block", "control_screen_side")
    frames = formed_screen_frames(916)
    for row in range(SCREEN_H):
        for col in range(SCREEN_W):
            strip = Image.new("RGBA", (16, 16 * SCREEN_FRAMES))
            for f, frame in enumerate(frames):
                strip.paste(frame.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16)), (0, f * 16))
            name = f"control_screen_formed_{1 + row * SCREEN_W + col}"
            save(strip, "block", name)
            (TEX / "block" / f"{name}.png.mcmeta").write_text('{"animation": {"frametime": 3}}\n', encoding="utf-8")
    sheet = drone_depot_sheet(917)
    (TEX / "entity").mkdir(parents=True, exist_ok=True)
    sheet.save(TEX / "entity" / "drone_depot.png")
    save(loose_holo(924), "block", "holo_table")
    save(holo_side(925), "block", "holo_table_side")
    holo = formed_holo(926)
    for dz in range(HOLO_TILES):
        for dx in range(HOLO_TILES):
            save(holo.crop((dx * 16, dz * 16, dx * 16 + 16, dz * 16 + 16)), "block", f"holo_table_formed_{1 + dz * HOLO_TILES + dx}")
    save(packager_side(906), "block", "cargo_packager_side")
    save(packager_top(907), "block", "cargo_packager_top")
    save(panel_block(908, strip=False), "block", "cargo_packager_bottom")

    save(drone_icon(QUAD, 4, body=[(59, 62, 70), (70, 74, 80), (91, 96, 104), (91, 96, 104)]), "item", "drone_t1")
    save(drone_icon(QUAD, 4, body=[(150, 112, 20), (214, 170, 38), (240, 206, 90), (240, 206, 90)]), "item", "creative_drone")
    save(drone_icon(HEX, 4, body=[(40, 42, 48), (59, 62, 70), (214, 170, 38), (59, 62, 70)]), "item", "drone_t2")
    save(drone_icon(OCTO, 6, body=[(52, 55, 61), (60, 64, 70), (78, 83, 91), (78, 83, 91)]), "item", "drone_t3")
    save(icon_runner(), "item", "drone_t4")
    save(icon_tiltrotor(), "item", "drone_t5")
    save(icon_tandem(), "item", "drone_t6")
    save(icon_aerostat(), "item", "drone_t7")
    save(icon_glider(), "item", "drone_t8")
    save(icon_ring_lifter(), "item", "drone_t9")

    save(motor(), "item", "drone_motor")
    save(propellers(WOOD), "item", "wooden_propeller_set")
    save(propellers(ALU), "item", "aluminum_propeller_set")
    save(rotor_assembly(WOOD), "item", "rotor_assembly")
    save(rotor_assembly(ALU), "item", "aluminum_rotor_assembly")
    save(ducted_fan(), "item", "ducted_fan")
    save(flight_controller(), "item", "flight_controller")
    save(lead_acid_pack(), "item", "lead_acid_pack")
    save(cargo_winch(), "item", "cargo_winch")
    save(neodymium_motor(), "item", "neodymium_drone_motor")
    save(nacelle(), "item", "tilt_rotor_nacelle")
    save(composite_blades(), "item", "composite_rotor_set")
    save(composite_assembly(), "item", "composite_rotor_assembly")
    save(lift_cell(), "item", "hydrogen_lift_cell")
    save(ion_emitter(), "item", "ion_emitter")
    save(sc_tape(), "item", "superconducting_tape")
    save(cryocooler(), "item", "stirling_cryocooler")
    save(sc_motor(), "item", "superconducting_motor")
    save(sc_lift_fan(), "item", "superconducting_lift_fan")


if __name__ == "__main__":
    draw_all()
