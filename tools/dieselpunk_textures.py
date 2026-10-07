"""Original 16x16 textures for the dieselpunk look of Jugcraft's higher tiers (see docs/ART_DIRECTION.md).

Where the steampunk tiers are brass, copper and riveted iron, the dieselpunk tiers are gunmetal and
olive-drab paint worn through at the edges, chrome trim, hazard stripes, black rubber hoses,
bakelite grips, louvred grilles and green phosphor gauges. Drawn in the clean style (tools/clean_metal.py): flat
fills, bevelled panels and bolts, and wear only as a few marks at the edges. Deterministic: the same code always draws
the same pixels. Names start with "dp_". Called from generate_textures.py.
"""
import math

from clean_metal import CHIP, CHIP_WIDE, corner_bolts, inset, patch, plate, scuffs
from steampunk_textures import new, put, save

GUNMETAL = [(24, 26, 28), (38, 41, 44), (54, 58, 62), (72, 77, 82), (96, 102, 108), (132, 138, 144)]
OLIVE = [(40, 46, 26), (56, 64, 36), (74, 84, 48), (92, 104, 60), (116, 128, 78)]
CHROME = [(72, 76, 80), (118, 124, 130), (168, 174, 180), (214, 218, 222), (244, 246, 248)]
HAZARD = [(222, 172, 28), (246, 204, 60), (28, 26, 24)]
RUBBER = [(16, 16, 16), (28, 28, 27), (40, 40, 38)]
BAKELITE = [(58, 24, 12), (82, 36, 18), (108, 52, 26), (132, 70, 38)]
PHOSPHOR = [(10, 24, 12), (30, 70, 34), (90, 190, 90), (170, 250, 150)]
SOOT = [(18, 16, 15), (30, 27, 25), (44, 40, 36)]
STEEL = [(60, 64, 70), (88, 94, 100), (120, 126, 134), (160, 166, 172), (200, 206, 210)]


def gunmetal_plate(seed):
    """Dark gunmetal: a bevelled panel with a recessed groove inset and a screw in each corner."""
    img = new()
    plate(img, GUNMETAL)
    inset(img, 4, 4, 11, 11, GUNMETAL[3], GUNMETAL[1])
    scuffs(img, GUNMETAL[3], [(6, 7, 3), (8, 9, 2)], only=GUNMETAL[2])
    corner_bolts(img, GUNMETAL)
    return img


def painted(seed, paint, chip_rate=0.22):
    """Army paint on gunmetal: a bevelled painted panel with bolts, worn through to bare metal at two corners."""
    img = new()
    plate(img, paint)
    scuffs(img, paint[3], only=paint[2])
    corner_bolts(img, paint)
    patch(img, CHIP_WIDE, GUNMETAL[3], 1, 12)
    put(img, 2, 13, GUNMETAL[2])
    patch(img, CHIP, GUNMETAL[3], 12, 1)
    return img


def chrome(seed):
    """Polished chrome: bright horizontal highlight bands over a darker reflection."""
    img = new()
    for y in range(16):
        band = CHROME[4] if y in (3, 4) else CHROME[3] if y in (2, 5, 11) else CHROME[1] if y in (8, 9, 14) else CHROME[2]
        for x in range(16):
            put(img, x, y, band)
    return img


def hazard(seed):
    """Yellow and black hazard stripes, the yellow lit along each stripe's upper edge."""
    img = new()
    for y in range(16):
        for x in range(16):
            phase = (x + y) % 8
            c = HAZARD[2] if phase >= 4 else HAZARD[1] if phase == 0 else HAZARD[0]
            put(img, x, y, c)
    return img


def rubber(seed, ribbed=False):
    """Black rubber (hoses, grips, tyres): a flat fill with a faint diagonal grain; ribbed has a raised rib every
    third row."""
    img = new(RUBBER[1])
    for y in range(16):
        for x in range(16):
            if ribbed:
                if y % 3 == 0:
                    put(img, x, y, RUBBER[2])
                elif y % 3 == 2:
                    put(img, x, y, RUBBER[0])
            elif (x + 2 * y) % 11 == 0:
                put(img, x, y, RUBBER[2])
    return img


def bakelite(seed):
    """Brown bakelite with a glossy swirl."""
    img = new()
    for y in range(16):
        for x in range(16):
            swirl = math.sin(x * 0.7 + y * 0.35 + math.sin(y * 0.9) * 0.6)
            c = BAKELITE[2] if swirl > 0.55 else BAKELITE[1] if swirl > -0.4 else BAKELITE[0]
            put(img, x, y, c)
    for x in range(3, 7):
        put(img, x, 2, BAKELITE[3])
    return img


def grille(seed):
    """Louvred vent grille in gunmetal: slanted slats with dark gaps."""
    img = gunmetal_plate(seed)
    for y in range(2, 14):
        for x in range(2, 14):
            row = (y - 2) % 3
            put(img, x, y, GUNMETAL[0] if row == 2 else GUNMETAL[4] if row == 0 else GUNMETAL[2])
    return img


def gauge(value=0.65):
    """Round gauge in a chrome bezel: green phosphor dial, tick marks and a red needle."""
    img = new(GUNMETAL[1])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 7.5:
                put(img, x, y, CHROME[3] if d > 6.5 else CHROME[1] if d > 5.8 else PHOSPHOR[1])
    for tick in range(9):
        a = math.radians(-225 + tick * 33.75)
        put(img, round(7.5 + math.cos(a) * 4.8), round(7.5 + math.sin(a) * 4.8), PHOSPHOR[3])
    a = math.radians(-225 + value * 270)
    for step in range(5):
        put(img, round(7.5 + math.cos(a) * step), round(7.5 + math.sin(a) * step), (220, 40, 30))
    put(img, 7, 7, CHROME[2])
    put(img, 8, 8, CHROME[1])
    return img


def exhaust(seed):
    """Exhaust pipe metal: gunmetal fading into soot towards the hot end, darker at the edges."""
    img = new()
    for y in range(16):
        base = GUNMETAL[2] if y < 6 else SOOT[2] if y < 11 else SOOT[1]
        for x in range(16):
            put(img, x, y, SOOT[0] if x in (0, 15) else base)
    return img


def drill_bit(seed):
    """Fluted steel drill bit: diagonal spiral flutes with bright cutting edges."""
    img = new()
    for y in range(16):
        for x in range(16):
            phase = (x + 2 * y) % 8
            c = STEEL[4] if phase == 0 else STEEL[3] if phase in (1, 2) else STEEL[1] if phase in (5, 6) else STEEL[2]
            put(img, x, y, c)
    return img


def saw_chain(seed):
    """A saw chain seen side-on: chrome cutter teeth on dark links, running along the texture's length."""
    img = new(GUNMETAL[1])
    for y in range(16):
        for x in range(16):
            c = GUNMETAL[2] if y % 4 == 0 else GUNMETAL[1]
            if x in (0, 15):
                c = CHROME[2] if y % 4 in (0, 1) else GUNMETAL[0]
            elif x in (1, 14):
                c = CHROME[1] if y % 4 == 0 else c
            if y % 4 == 3:
                c = GUNMETAL[0]
            put(img, x, y, c)
    return img


def warning_lamp(on):
    """Caged amber lamp: wire cage bars over glass."""
    img = new(GUNMETAL[1])
    glass = [(255, 170, 40), (255, 214, 110), (255, 240, 190)] if on else [(90, 56, 18), (120, 76, 26), (150, 100, 40)]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 7:
                put(img, x, y, glass[2] if d < 2.5 else glass[1] if d < 5 else glass[0])
            if d < 7.5 and (x in (4, 11) or y in (4, 11)):
                put(img, x, y, GUNMETAL[4])
    return img


def stencil(seed):
    """Olive paint with a stencilled white serial band and a star: the painted panels of machines."""
    img = painted(seed, OLIVE)
    for x in range(3, 13):
        if x % 3:
            put(img, x, 11, (214, 210, 196))
    for x, y in ((7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6), (6, 7), (9, 7)):
        put(img, x, y, (214, 210, 196))
    return img


def draw_all():
    save(gunmetal_plate(700), "dp_gunmetal")
    save(painted(701, OLIVE), "dp_olive")
    save(stencil(702), "dp_olive_stencil")
    save(chrome(703), "dp_chrome")
    save(hazard(704), "dp_hazard")
    save(rubber(705), "dp_rubber")
    save(rubber(706, ribbed=True), "dp_rubber_ribbed")
    save(bakelite(707), "dp_bakelite")
    save(grille(708), "dp_grille")
    save(gauge(), "dp_gauge")
    save(exhaust(709), "dp_exhaust")
    save(drill_bit(710), "dp_drill_bit")
    save(saw_chain(711), "dp_saw_chain")
    save(warning_lamp(False), "dp_lamp")
    save(warning_lamp(True), "dp_lamp_on")
