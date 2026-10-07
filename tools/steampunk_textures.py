"""Original 16x16 textures for the steampunk machine style (see tools/steampunk_models.py).

Drawn in the clean style the owner asked for on 4 October 2026 (tools/clean_metal.py, docs/ART_DIRECTION.md): flat
fills from short palettes, bevelled plates and rivets, banded sheens, and wear only as a few placed marks, never a
random shade per pixel. Deterministic: regenerating never changes a file unless its drawing code changes. (The punch
card's holes and the counter's engraved figures still come from a seeded random source: they are a pattern, not
noise.) Called from generate_textures.py.
All textures are opaque: machine blocks render in the solid layer, where alpha is ignored.
"""
import math
import random
from pathlib import Path

from PIL import Image

import clean_metal

TEX = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures" / "block"

IRON = [(34, 32, 34), (50, 48, 50), (68, 65, 66), (86, 82, 81), (108, 103, 99), (146, 138, 128)]
BRASS = [(76, 50, 18), (120, 84, 32), (166, 124, 50), (206, 166, 76), (236, 206, 122), (250, 236, 180)]
COPPER = [(96, 44, 26), (140, 68, 40), (184, 98, 58), (216, 132, 86), (240, 176, 132)]
PATINA = [(62, 124, 104), (88, 156, 132), (120, 184, 156)]
WOOD = [(46, 28, 18), (62, 40, 24), (80, 52, 32), (98, 66, 40), (118, 82, 52)]
BRICK = [(104, 44, 30), (122, 54, 36), (140, 64, 42)]
MORTAR = (58, 50, 46)
FIRE = [(255, 110, 20), (255, 150, 30), (255, 196, 70), (255, 232, 150), (236, 80, 16)]
CREAM = [(236, 226, 196), (220, 208, 176)]
RED = [(104, 20, 18), (138, 30, 24), (170, 44, 32), (196, 70, 50)]


def new(fill=(0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill + (255,))


def put(img, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(color[:3]) + (255,))


def save(img, name):
    TEX.mkdir(parents=True, exist_ok=True)
    img.save(TEX / f"{name}.png", optimize=True)


def rivet(img, x, y, palette):
    """A rivet: highlight up-left, head, shadow down-right."""
    put(img, x - 1, y - 1, palette[-1])
    put(img, x, y, palette[-2])
    put(img, x + 1, y + 1, palette[0])


def plate(seed, palette, rivets=True, border=True):
    """Bevelled metal plate (a dark seam, lit top and left, shaded bottom and right) with rivets near the corners."""
    img = new()
    if border:
        clean_metal.plate(img, palette)
        clean_metal.scuffs(img, palette[3], [(4, 5, 3), (9, 10, 2)], only=palette[2])
    else:
        clean_metal.rect(img, 0, 0, 15, 15, palette[2])
    if rivets:
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            rivet(img, x, y, palette)
    return img


# Where verdigris gathers on brushed copper: small hand-placed blooms (offsets from their corner), so it reads as
# weathering in a few places rather than speckle.
BLOOMS = ((2, 9, [(0, 0), (1, 0), (0, 1)]), (11, 3, [(0, 0), (1, 0)]), (7, 13, [(0, 0), (1, 0), (1, 1)]),
          (13, 11, [(0, 0), (0, 1)]))


def brushed(seed, palette, specks=None, speck_count=0):
    """Polished metal: a soft horizontal sheen (a light band near the top, darker towards the bottom, so it reads
    as a curved, shiny surface), two fine bright scratches and, with `specks`, a few small verdigris blooms."""
    img = new()
    for y in range(16):
        band = 4 if 3 <= y <= 5 else (1 if y >= 12 else (3 if y in (2, 6, 7) else 2))
        for x in range(16):
            put(img, x, y, palette[band])
    clean_metal.scuffs(img, palette[min(len(palette) - 1, 4)], [(2, 9, 4), (8, 11, 3)])
    if specks:
        for i, (x, y, pixels) in enumerate(BLOOMS[:max(1, speck_count // 2)]):
            clean_metal.patch(img, pixels, specks[i % len(specks)], x, y)
    return img


def wrought_iron(seed):
    """Wrought iron: flat dark iron in vertical bars, each lit along its left edge with a dark joint beside it."""
    img = new(IRON[2])
    for y in range(16):
        for x in range(16):
            if x % 5 == 0:
                put(img, x, y, IRON[1])
            elif x % 5 == 1:
                put(img, x, y, IRON[3])
    clean_metal.scuffs(img, IRON[3], [(2, 4, 2), (12, 9, 2), (7, 13, 2)], only=IRON[2])
    return img


def planks(seed):
    """Dark stained planks, 4 pixels tall: each lit along its top, a staggered joint and a few grain lines."""
    img = new()
    for plank, (base, joint) in enumerate(((2, 5), (1, 11), (2, 3), (1, 9))):
        y0 = plank * 4
        for y in range(y0, y0 + 4):
            for x in range(16):
                c = WOOD[base + 1] if y == y0 else WOOD[0] if y == y0 + 3 or x == joint else WOOD[base]
                put(img, x, y, c)
        for x in range(16):
            if (x + plank * 5) % 9 in (1, 2, 3) and x != joint:
                put(img, x, y0 + 1 + plank % 2, WOOD[base + 1] if base == 1 else WOOD[base - 1])
    return img


def bricks(seed, sooty=True):
    """Firebrick in courses: each brick one flat shade, lit along its top edge, in mortar; a sooty brick or two."""
    img = new()
    for y in range(16):
        course = y // 4
        offset = 2 if course % 2 else 0
        for x in range(16):
            if y % 4 == 3 or (x + offset) % 8 == 7:
                put(img, x, y, MORTAR)
                continue
            brick = (course * 3 + (x + offset) // 8) % 3
            c = BRICK[(1, 0, 2)[brick]]
            if sooty and (course, (x + offset) // 8) in ((1, 1), (3, 0)):
                c = (78, 40, 30)
            if y % 4 == 0:
                c = tuple(min(255, v + 18) for v in c)
            put(img, x, y, c)
    return img


def disc(img, cx, cy, r_in, r_out, color_fn):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r_in <= d < r_out:
                put(img, x, y, color_fn(x, y, d))


def gauge():
    """Pressure gauge: brass bezel, cream dial, ticks and a red needle. Stretched over a small face."""
    img = new(IRON[1])
    cx = cy = 8.0
    disc(img, cx, cy, 0, 6.2, lambda x, y, d: CREAM[0] if y > 3 else CREAM[1])
    disc(img, cx, cy, 6.2, 8.0, lambda x, y, d: BRASS[4] if (x + y) < 14 else BRASS[2])
    for angle in range(-135, 136, 45):
        rad = math.radians(angle - 90)
        put(img, round(cx - 0.5 + 4.8 * math.cos(rad)), round(cy - 0.5 + 4.8 * math.sin(rad)), (30, 26, 24))
    for step in range(5):  # needle toward the upper right
        put(img, 8 + step // 2 + (1 if step > 2 else 0), 7 - step, RED[2])
    put(img, 7, 8, (40, 34, 30))
    put(img, 8, 8, (40, 34, 30))
    return img


def porthole(seed, inner_fn, rim=BRASS):
    """Round window in an iron plate with a brass rim; inner_fn colors the glass."""
    rng = random.Random(seed)
    img = plate(seed, IRON, rivets=False)
    cx = cy = 8.0
    disc(img, cx, cy, 0, 5.4, lambda x, y, d: inner_fn(x, y, d, rng))
    disc(img, cx, cy, 5.4, 7.2, lambda x, y, d: rim[4] if (x + y) < 14 else rim[2])
    for x, y in ((8, 1), (8, 14), (1, 8), (14, 8)):
        put(img, x, y, rim[3])
    return img


def firebox(seed, lit):
    """Firebox door: riveted frame, four draft slots (glowing in bands when lit), brass latch."""
    img = plate(seed, IRON)
    for row in (4, 6, 8, 10):
        for x in range(4, 12):
            if lit:
                c = FIRE[(1, 2, 3, 2)[(x + row) % 4]]
            else:
                c = (38, 22, 18) if (x + row) % 4 == 0 else (22, 16, 14)
            put(img, x, row, c)
            put(img, x, row + 1, IRON[4])
    for y in (7, 8):
        put(img, 12, y, BRASS[4])
        put(img, 13, y, BRASS[2])
    return img


def coil(seed):
    """Copper windings: tight bands, each lit on top, with a dark gap every fourth row."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = COPPER[0] if y % 4 == 3 else COPPER[3] if y % 4 == 1 else COPPER[2]
            if y % 4 == 1 and (x + y * 3) % 7 == 0:
                c = COPPER[4]
            put(img, x, y, c)
    return img


def lamp(lit):
    img = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 6, y - 6)
            if lit:
                c = (255, 244, 200) if d < 3 else ((255, 206, 96) if d < 7 else (240, 150, 40))
            else:
                c = (130, 92, 40) if d < 3 else ((92, 60, 24) if d < 7 else (70, 44, 18))
            put(img, x, y, c)
    return img


def glass(seed):
    """Opaque bluish glass: a flat tint with two diagonal highlight streaks."""
    img = new((132, 170, 184))
    for y in range(16):
        for x in range(16):
            if (x + y) % 11 in (0, 1) and x < 12:
                put(img, x, y, (214, 236, 242))
            elif (x + y) % 11 == 2 and x < 12:
                put(img, x, y, (160, 196, 208))
    return img


def sight_glass():
    img = new()
    for y in range(16):
        for x in range(16):
            if x in (0, 15):
                c = BRASS[2]
            elif y < 5:
                c = (170, 204, 214)
            elif y == 5:
                c = (120, 170, 220)
            else:
                c = (44, 92, 170) if (x + y) % 5 else (60, 116, 196)
            if x == 4 and 1 <= y <= 13:
                c = (228, 244, 250)
            put(img, x, y, c)
    return img


def leyden_jar():
    """Glass jar: clear top third, copper foil below, brass rim."""
    img = new()
    for y in range(16):
        for x in range(16):
            if y == 0:
                c = BRASS[4]
            elif y < 6:
                c = (150, 190, 200) if x % 5 else (220, 240, 246)
            else:
                c = COPPER[3] if x in (3, 4) else (COPPER[2] if (x + y) % 6 else COPPER[1])
            put(img, x, y, c)
    return img


def solar_cells():
    img = new()
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0:
                c = BRASS[3] if (x + y) % 2 else BRASS[2]
            else:
                c = (26, 40, 86) if (x + y) % 3 else (40, 62, 120)
                if (x % 4, y % 4) == (1, 1):
                    c = (120, 150, 210)
            put(img, x, y, c)
    return img


def punchcard(seed):
    rng = random.Random(seed)
    img = new(BRASS[2])
    for y in range(1, 15):
        for x in range(1, 15):
            put(img, x, y, CREAM[0] if y % 2 else CREAM[1])
    for y in range(3, 14, 2):
        for x in range(3, 13):
            if rng.random() < 0.35:
                put(img, x, y, (40, 34, 30))
    return img


def digit_wheels(seed):
    """Stacked number wheels: brass rims with engraved figures between them."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            if y % 4 == 0:
                c = BRASS[1]
            elif y % 4 == 3:
                c = BRASS[4]
            else:
                c = BRASS[3]
                if x % 3 == 1 and rng.random() < 0.7:
                    c = (46, 34, 20)
            put(img, x, y, c)
    return img


def screw():
    img = new()
    for y in range(16):
        for x in range(16):
            phase = (x + y) % 4
            c = BRASS[4] if phase == 0 else (BRASS[3] if phase == 1 else (BRASS[2] if phase == 2 else BRASS[0]))
            put(img, x, y, c)
    return img


def vane(seed):
    """Galvanized windmill vane: flat zinc grey, darker edges and a riveted spine."""
    img = new((196, 200, 202))
    for y in range(16):
        for x in range(16):
            if y in (0, 15):
                put(img, x, y, (140, 144, 148))
            elif x == 7:
                put(img, x, y, (120, 124, 128) if y % 3 else (220, 224, 226))
            elif x == 8:
                put(img, x, y, (176, 180, 184))
    return img


def tank(seed):
    """Riveted copper tank plates with a verdigris bloom here and there."""
    img = brushed(seed, COPPER + [COPPER[4]], PATINA, 7)
    for y in range(16):
        put(img, 0, y, COPPER[0])
        put(img, 8, y, COPPER[0])
        if y % 3 == 1:
            put(img, 1, y, COPPER[4])
            put(img, 9, y, COPPER[4])
    for x in range(16):
        put(img, x, 8, COPPER[0])
        if x % 3 == 2:
            put(img, x, 9, COPPER[4])
    return img


def crusher_jaws(seed):
    """Front of the crusher: two toothed rollers in a dark mouth."""
    img = plate(seed, IRON)
    for y in range(3, 13):
        for x in range(3, 13):
            put(img, x, y, (18, 16, 16))
    for x in range(3, 13):
        top = IRON[5] if x % 2 else IRON[3]
        bottom = IRON[3] if x % 2 else IRON[5]
        put(img, x, 5, top)
        put(img, x, 6, IRON[2])
        put(img, x, 9, IRON[2])
        put(img, x, 10, bottom)
    return img


def water(seed):
    """Water surface seen from above: deep blue with long, regular pale ripple lines."""
    img = new((38, 84, 168))
    for y in range(16):
        for x in range(16):
            wave = (x + 2 * y) % 16
            if wave in (0, 1, 2) and y % 4 == 1:
                put(img, x, y, (126, 176, 226))
            elif wave in (8, 9, 10, 11) and y % 4 == 3:
                put(img, x, y, (46, 98, 186))
    return img


def mesh(seed):
    """Woven brass sieve mesh over a dark tray."""
    img = new((22, 20, 18))
    for y in range(16):
        for x in range(16):
            if x % 3 == 0 or y % 3 == 0:
                put(img, x, y, BRASS[3] if (x + y) % 2 else BRASS[2])
    for i in range(16):
        put(img, i, 0, BRASS[1])
        put(img, i, 15, BRASS[1])
        put(img, 0, i, BRASS[1])
        put(img, 15, i, BRASS[1])
    return img


def saw(seed):
    """Polished saw steel with circular grinding marks."""
    img = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            put(img, x, y, (196, 200, 206) if int(d) % 2 else (168, 172, 180))
    for x, y in ((4, 3), (5, 3), (11, 6), (3, 10)):
        put(img, x, y, (226, 230, 236))
    return img


def casing(seed):
    """Machine casing: iron plate inside a riveted brass frame."""
    img = plate(seed, IRON, rivets=False)
    for i in range(16):
        for j in (0, 1, 14, 15):
            put(img, i, j, BRASS[3] if j in (0, 14) else BRASS[1])
            put(img, j, i, BRASS[3] if j in (0, 14) else BRASS[1])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14), (7, 1), (7, 14), (1, 7), (14, 7)):
        put(img, x, y, BRASS[5])
    return img


def strapped_bricks(seed):
    """Refractory brick bound with riveted iron straps (arc furnace casing)."""
    img = bricks(seed)
    for i in range(16):
        for j in (0, 15):
            put(img, j, i, IRON[3] if j == 0 else IRON[1])
        put(img, i, 7, IRON[3])
        put(img, i, 8, IRON[1])
    for y in (3, 12):
        put(img, 0, y, IRON[5])
        put(img, 15, y, IRON[5])
    for x in (3, 12):
        put(img, x, 7, IRON[5])
    return img


def belt():
    img = new()
    for y in range(16):
        for x in range(16):
            c = (92, 54, 32) if (x + y) % 7 else (78, 44, 26)
            if y in (1, 14) and x % 3 != 2:
                c = (196, 176, 132)
            put(img, x, y, c)
    return img


def grate():
    img = new()
    for y in range(16):
        for x in range(16):
            c = IRON[4] if x % 3 == 0 else IRON[0]
            if y in (0, 15):
                c = IRON[3]
            put(img, x, y, c)
    return img


def painted(seed, palette):
    """Painted iron (valve wheels): a flat coat, lit along the top and left, chipped to bare metal at two corners."""
    img = new()
    clean_metal.bevel(img, 0, 0, 15, 15, palette[3], palette[1], palette[2])
    clean_metal.patch(img, clean_metal.CHIP, IRON[3], 1, 13)
    clean_metal.patch(img, clean_metal.CHIP, IRON[3], 13, 1)
    return img


def ceramic():
    img = new()
    for y in range(16):
        for x in range(16):
            c = (232, 228, 216) if y % 3 else (200, 196, 184)
            put(img, x, y, c)
    return img


def draw_die():
    """Wire-drawing die: iron face with three graded holes ringed in brass."""
    img = plate(611, IRON, rivets=False)
    for cx, r in ((4, 2.0), (8, 1.6), (12, 1.2)):
        disc(img, cx, 8, 0, r, lambda x, y, d: (10, 8, 8))
        disc(img, cx, 8, r, r + 1.0, lambda x, y, d: BRASS[4])
    return img


def hopper_inside(seed):
    """The dark inside of a hopper or chute: an iron rim stepping down to a near-black throat."""
    img = new((26, 22, 22))
    for i, c in enumerate((IRON[3], IRON[1], (40, 34, 32))):
        for x in range(i, 16 - i):
            for y in (i, 15 - i):
                put(img, x, y, c)
                put(img, y, x, c)
    return img


def fire_glow(x, y, d, rng):
    return FIRE[3] if d < 2 else (FIRE[2] if d < 3.6 else (FIRE[0], FIRE[1], FIRE[4])[(x + 2 * y) % 3])


def dark_glass(x, y, d, rng):
    return (200, 214, 220) if (x - y) in (0, 1) and d < 4.5 else ((30, 28, 34) if d > 3 else (44, 40, 48))


def arc_glow(x, y, d, rng):
    if abs((x - 8) - round(2 * math.sin(y))) < 1:
        return (250, 252, 255)
    return (150, 200, 255) if d < 3 else (70, 120, 230)


def lava_glow(x, y, d, rng):
    return (255, 200, 80) if d < 2 else (255, 150, 30) if d < 3.6 else ((255, 110, 20), (240, 80, 10))[(x + y) % 2]


def lava_crust(x, y, d, rng):
    return (90, 34, 16) if (x + 2 * y) % 5 == 0 else (60, 22, 14) if d < 3.6 else (40, 18, 14)


def leaves(seed):
    """Dense leaf canopy: overlapping leaves, each lit along its top, over dark gaps."""
    greens = [(34, 78, 30), (48, 104, 38), (62, 128, 46), (92, 158, 62)]
    img = new(greens[0])
    for lx, ly in ((0, 0), (8, 1), (4, 5), (12, 6), (0, 9), (8, 10), (4, 13), (12, 14)):
        for dy, row in enumerate((" ### ", "#####", "#####", " ### ")):
            for dx, ch in enumerate(row):
                if ch == "#":
                    c = greens[3] if dy == 0 else greens[2] if dy == 1 else greens[1]
                    img.putpixel(((lx + dx) % 16, (ly + dy) % 16), c + (255,))
    return img


def bark(seed):
    """Vertical bark ridges in browns: each ridge lit on its left, with dark furrows between."""
    browns = [(56, 38, 22), (78, 54, 32), (98, 70, 42)]
    img = new(browns[1])
    for y in range(16):
        for x in range(16):
            phase = (x + (y // 6) % 2) % 4
            put(img, x, y, browns[0] if phase == 0 else browns[2] if phase == 1 else browns[1])
    return img


def soil(seed):
    """Dark tilled soil in furrows, with a few lighter clods along each ridge."""
    img = new((58, 40, 26))
    for y in range(16):
        for x in range(16):
            if y % 4 == 0:
                put(img, x, y, (44, 30, 20))
            elif y % 4 == 1 and (x + y) % 5 in (0, 1):
                put(img, x, y, (74, 52, 34))
    return img


def lava(seed):
    """Molten surface: orange with bright yellow currents and a few dark crust plates."""
    img = new(FIRE[1])
    for y in range(16):
        for x in range(16):
            wave = (x + 2 * y + (y // 4) * 3) % 12
            if wave in (0, 1):
                put(img, x, y, FIRE[3])
            elif wave in (2, 3, 11):
                put(img, x, y, FIRE[2])
    for x, y in ((3, 4), (4, 4), (11, 10), (12, 10), (12, 11)):
        put(img, x, y, (120, 40, 12))
    return img


def draw_all():
    save(plate(601, IRON), "sp_iron_plate")
    save(wrought_iron(602), "sp_iron")
    save(brushed(603, BRASS), "sp_brass")
    save(plate(604, BRASS), "sp_brass_plate")
    save(brushed(605, COPPER + [COPPER[4]], PATINA, 5), "sp_copper")
    save(planks(606), "sp_wood")
    save(bricks(607), "sp_firebrick")
    save(gauge(), "sp_gauge")
    save(firebox(608, False), "sp_firebox")
    save(firebox(608, True), "sp_firebox_on")
    save(porthole(609, dark_glass), "sp_window")
    save(porthole(609, fire_glow), "sp_window_on")
    save(porthole(610, dark_glass), "sp_arc_window")
    save(porthole(610, arc_glow), "sp_arc_window_on")
    save(porthole(612, lava_crust), "sp_lava_window")
    save(porthole(612, lava_glow), "sp_lava_window_on")
    save(coil(613), "sp_coil")
    save(lamp(False), "sp_lamp")
    save(lamp(True), "sp_lamp_on")
    save(glass(614), "sp_glass")
    save(sight_glass(), "sp_sight_glass")
    save(leyden_jar(), "sp_leyden_jar")
    save(solar_cells(), "sp_solar")
    save(punchcard(615), "sp_punchcard")
    save(digit_wheels(616), "sp_digit_wheels")
    save(screw(), "sp_screw")
    save(vane(617), "sp_vane")
    save(tank(618), "sp_tank")
    save(crusher_jaws(619), "sp_crusher_jaws")
    save(casing(620), "sp_machine_casing")
    save(water(624), "sp_water")
    save(leaves(640), "sp_leaves")
    save(bark(641), "sp_bark")
    save(soil(642), "sp_soil")
    save(lava(643), "sp_lava")
    save(mesh(625), "sp_mesh")
    save(saw(626), "sp_saw")
    save(strapped_bricks(621), "sp_arc_casing")
    save(belt(), "sp_belt")
    save(grate(), "sp_grate")
    save(painted(622, RED), "sp_red_iron")
    save(ceramic(), "sp_ceramic")
    save(draw_die(), "sp_die")
    save(hopper_inside(623), "sp_hopper_inside")
