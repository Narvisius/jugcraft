"""Original 16x16 textures for the steampunk machine style (see tools/steampunk_models.py).

Deterministic: every texture draws from its own seeded random source, so regenerating never
changes a file unless its drawing code changes. Called from generate_textures.py.
All textures are opaque: machine blocks render in the solid layer, where alpha is ignored.
"""
import math
import random
from pathlib import Path

from PIL import Image

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
    """Beveled metal plate with rivets near the corners."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = palette[3] if rng.random() < 0.18 else palette[2]
            if border:
                if x == 0 or y == 0:
                    c = palette[3]
                elif x == 15 or y == 15:
                    c = palette[1]
            put(img, x, y, c)
    if rivets:
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            rivet(img, x, y, palette)
    return img


def brushed(seed, palette, specks=None, speck_count=0):
    """Polished metal with a soft horizontal sheen and fine scratches."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        # Light band near the top third, darker toward the bottom: reads as a curved, shiny surface.
        band = 4 if 3 <= y <= 5 else (1 if y >= 12 else (3 if y in (2, 6, 7) else 2))
        for x in range(16):
            c = palette[band]
            if rng.random() < 0.12:
                c = palette[max(0, band - 1)]
            put(img, x, y, c)
    for _ in range(3):
        y = rng.randrange(16)
        x0 = rng.randrange(10)
        for x in range(x0, x0 + rng.randrange(3, 7)):
            put(img, x, y, palette[min(len(palette) - 1, 4)])
    if specks:
        for _ in range(speck_count):
            put(img, rng.randrange(16), rng.randrange(16), rng.choice(specks))
    return img


def wrought_iron(seed):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = IRON[rng.choice([2, 2, 3])]
            if x % 5 == 0 and rng.random() < 0.6:
                c = IRON[1]
            put(img, x, y, c)
    return img


def planks(seed):
    """Dark stained planks, 4 pixels tall, with staggered joints and grain."""
    rng = random.Random(seed)
    img = new()
    for plank in range(4):
        base = rng.choice([1, 2])
        joint = rng.randrange(3, 13)
        for y in range(plank * 4, plank * 4 + 4):
            for x in range(16):
                c = WOOD[base]
                if y % 4 == 3:
                    c = WOOD[0]
                elif x == joint:
                    c = WOOD[0]
                elif rng.random() < 0.18:
                    c = WOOD[base + 1]
                put(img, x, y, c)
    return img


def bricks(seed, sooty=True):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            offset = 2 if (y // 4) % 2 else 0
            if y % 4 == 3 or (x + offset) % 8 == 7:
                c = MORTAR
            else:
                c = rng.choice(BRICK)
                if sooty and rng.random() < 0.08:
                    c = (60, 34, 28)
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
    """Firebox door: riveted frame, four draft slots, brass latch."""
    rng = random.Random(seed)
    img = plate(seed, IRON)
    for row in (4, 6, 8, 10):
        for x in range(4, 12):
            c = rng.choice(FIRE) if lit else rng.choice([(22, 16, 14), (38, 22, 18)])
            put(img, x, row, c)
            put(img, x, row + 1, IRON[4])
    for y in (7, 8):
        put(img, 12, y, BRASS[4])
        put(img, 13, y, BRASS[2])
    return img


def coil(seed):
    """Copper windings: tight bands with a dark gap every fourth row."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            if y % 4 == 3:
                c = COPPER[0]
            else:
                c = COPPER[3] if y % 4 == 1 else COPPER[2]
                if rng.random() < 0.08:
                    c = COPPER[1]
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
    """Opaque bluish glass with highlight streaks."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (132, 170, 184) if rng.random() < 0.8 else (118, 156, 170)
            if (x + y) % 11 in (0, 1) and x < 12:
                c = (214, 236, 242)
            put(img, x, y, c)
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
    """Galvanized windmill vane with a riveted spine."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (196, 200, 202) if rng.random() < 0.85 else (176, 180, 184)
            if y in (0, 15):
                c = (140, 144, 148)
            if x == 7:
                c = (120, 124, 128) if y % 3 else (220, 224, 226)
            put(img, x, y, c)
    return img


def tank(seed):
    """Riveted copper tank plates with a verdigris bloom."""
    rng = random.Random(seed)
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
    put(img, 12, 4, PATINA[rng.choice([0, 1])])
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
    """Water surface seen from above: deep blue with pale ripple lines."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (38, 84, 168) if rng.random() < 0.75 else (46, 98, 186)
            if (y * 3 + x // 4) % 7 == 0 and rng.random() < 0.7:
                c = (126, 176, 226)
            put(img, x, y, c)
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
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = (196, 200, 206) if int(d) % 2 else (168, 172, 180)
            if rng.random() < 0.08:
                c = (226, 230, 236)
            put(img, x, y, c)
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
    """Painted iron with a few chips showing the metal (valve wheels)."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = palette[rng.choice([1, 2, 2])]
            if rng.random() < 0.05:
                c = IRON[3]
            put(img, x, y, c)
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
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (26, 22, 22) if rng.random() < 0.8 else (54, 46, 40)
            if x in (0, 15) or y in (0, 15):
                c = IRON[3]
            put(img, x, y, c)
    return img


def fire_glow(x, y, d, rng):
    return FIRE[3] if d < 2 else (FIRE[2] if d < 3.6 else rng.choice([FIRE[0], FIRE[1], FIRE[4]]))


def dark_glass(x, y, d, rng):
    return (200, 214, 220) if (x - y) in (0, 1) and d < 4.5 else ((30, 28, 34) if d > 3 else (44, 40, 48))


def arc_glow(x, y, d, rng):
    if abs((x - 8) - round(2 * math.sin(y))) < 1:
        return (250, 252, 255)
    return (150, 200, 255) if d < 3 else (70, 120, 230)


def lava_glow(x, y, d, rng):
    return rng.choice([(255, 150, 30), (255, 110, 20), (240, 80, 10), (255, 200, 80)])


def lava_crust(x, y, d, rng):
    return rng.choice([(60, 22, 14), (40, 18, 14), (90, 34, 16)])


def leaves(seed):
    """Dense leaf canopy: mid greens with dark gaps and a few sunlit tips."""
    rng = random.Random(seed)
    img = new()
    greens = [(34, 78, 30), (48, 104, 38), (62, 128, 46), (92, 158, 62)]
    for y in range(16):
        for x in range(16):
            r = rng.random()
            put(img, x, y, greens[0] if r < 0.18 else greens[1] if r < 0.55 else greens[2] if r < 0.9 else greens[3])
    return img


def bark(seed):
    """Vertical bark ridges in browns."""
    rng = random.Random(seed)
    img = new()
    browns = [(56, 38, 22), (78, 54, 32), (98, 70, 42)]
    ridges = [rng.choice([0, 1, 2]) for _ in range(16)]
    for y in range(16):
        for x in range(16):
            c = browns[ridges[x]]
            if rng.random() < 0.12:
                c = browns[max(0, ridges[x] - 1)]
            put(img, x, y, c)
    return img


def soil(seed):
    """Dark tilled soil with small lighter clods."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (58, 40, 26) if rng.random() < 0.7 else (74, 52, 34)
            if y % 4 == 0 and rng.random() < 0.5:
                c = (44, 30, 20)
            put(img, x, y, c)
    return img


def lava(seed):
    """Molten surface: orange-yellow with dark crust flecks."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            r = rng.random()
            c = FIRE[2] if r < 0.35 else FIRE[1] if r < 0.75 else FIRE[3] if r < 0.88 else (120, 40, 12)
            put(img, x, y, c)
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
