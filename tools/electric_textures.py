"""Original 16x16 textures for the electric look of Jugcraft's power gear (see docs/ART_DIRECTION.md).

Cables, batteries, the capacitor bank, the charging station, solar panels, the electric pump, motor and
dynamo: dark graphite casings with bevelled panels and recessed seams, lit by glowing mint-green strips,
with green-on-black screens and status lamps. Cyan is kept for the higher-tech tiers to come.
Deterministic like the other texture modules: each texture draws from its own seed. Names start with
"el_". Called from generate_textures.py.
"""
import random

from steampunk_textures import new, put, save

GRAPHITE = [(30, 33, 37), (46, 50, 55), (64, 69, 75), (82, 88, 95), (104, 111, 119), (138, 145, 153)]
GLOW = [(20, 82, 52), (34, 138, 84), (62, 204, 124), (128, 244, 172), (206, 255, 224)]
CYAN = [(18, 72, 86), (28, 128, 148), (56, 200, 218), (144, 242, 250), (218, 255, 255)]
SCREEN = [(6, 16, 14), (10, 30, 24), (18, 52, 40)]
COPPER = [(110, 54, 30), (168, 92, 52), (214, 140, 88)]
SILVER = [(120, 126, 136), (176, 182, 190), (230, 234, 240)]
ALUMINUM = [(132, 138, 146), (190, 196, 202), (236, 238, 242)]
HAZARD = [(222, 176, 34), (24, 24, 24)]
# The high-tech (electronics) tier: near-black casings, cyan glass and screens, violet conduits.
DARK = [(14, 16, 20), (22, 25, 31), (32, 36, 44), (44, 49, 59), (62, 68, 80), (92, 100, 116)]
# The flow battery's vanadium electrolyte, seen through its sight glasses.
VANADIUM = [(22, 30, 92), (36, 54, 150), (64, 96, 214), (140, 170, 250), (214, 226, 255)]
VIOLET = [(58, 24, 96), (98, 44, 160), (146, 82, 222), (196, 150, 250), (236, 218, 255)]
GLASS = [(8, 34, 42), (12, 52, 62), (20, 80, 92), (60, 150, 164), (170, 236, 244)]
SILICON = [(70, 78, 92), (104, 114, 130), (140, 150, 166), (190, 198, 212), (232, 236, 244)]
# A beige retro computer (one of the owner's references).
BEIGE = [(132, 120, 96), (168, 156, 128), (196, 186, 158), (218, 210, 184), (236, 230, 208)]


def _noise(rng, a, b, rate=0.18):
    return b if rng.random() < rate else a


def casing(seed, border=True):
    """A graphite panel: fine brushed noise, a raised 1-px bevel (light top-left, dark bottom-right), a recessed
    groove two pixels in, and a small screw in each corner. Without border it tiles seamlessly."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, _noise(rng, GRAPHITE[2], GRAPHITE[3]))
    if border:
        for i in range(16):
            put(img, i, 0, GRAPHITE[4])
            put(img, 0, i, GRAPHITE[4])
            put(img, i, 15, GRAPHITE[1])
            put(img, 15, i, GRAPHITE[1])
        for i in range(2, 14):
            put(img, i, 2, GRAPHITE[1])
            put(img, 2, i, GRAPHITE[1])
            put(img, i, 13, GRAPHITE[4])
            put(img, 13, i, GRAPHITE[4])
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            put(img, x, y, GRAPHITE[5])
    return img


def seams(seed):
    """Tileable graphite with panel seams every eight pixels, for large surfaces."""
    rng = random.Random(seed)
    img = casing(seed, border=False)
    for i in range(16):
        for line in (0, 8):
            put(img, i, line, GRAPHITE[1])
            put(img, i, line + 1, GRAPHITE[4])
            put(img, line, i, GRAPHITE[1])
            put(img, line + 1, i, GRAPHITE[4])
    for _ in range(4):
        x, y = rng.randrange(2, 7) + rng.choice((0, 8)), rng.randrange(2, 7) + rng.choice((0, 8))
        put(img, x, y, GRAPHITE[5])
    return img


def frame(seed):
    """Darker trim graphite for edges, posts and bezels, with a thin highlight line along it."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, _noise(rng, GRAPHITE[1], GRAPHITE[2], 0.25))
    for i in range(16):
        put(img, i, 7, GRAPHITE[4])
        put(img, 7, i, GRAPHITE[3])
    return img


def glow(palette):
    """A glowing light bar: bright core, softer edges, a few hot pixels. Stretched over thin elements."""
    img = new()
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            c = palette[1] if d == 0 else palette[2] if d <= 2 else palette[3]
            if d >= 4 and (x * 3 + y * 5) % 7 == 0:
                c = palette[4]
            put(img, x, y, c)
    return img


def light_bars(seed, palette, count=3):
    """A graphite face with recessed glowing bars across it, like the fronts of high-tech machines."""
    img = casing(seed)
    height = 14 // count
    for bar in range(count):
        top = 1 + bar * height + 1
        for y in range(top, top + height - 2):
            for x in range(3, 13):
                c = palette[3] if y == top + (height - 2) // 2 else palette[2]
                if x in (3, 12):
                    c = palette[1]
                put(img, x, y, c)
        for x in range(2, 14):
            put(img, x, top - 1, GRAPHITE[0])
            put(img, x, top + height - 2, GRAPHITE[4])
    return img


def vent(seed):
    """Graphite with dark horizontal cooling slots, lit faintly green from inside."""
    img = casing(seed)
    for y in (4, 7, 10):
        for x in range(4, 12):
            put(img, x, y, GRAPHITE[0])
            put(img, x, y + 1, GLOW[0] if 5 <= x <= 10 else GRAPHITE[0])
    return img


def screen(lit):
    """A monitor: thin graphite bezel around a black-green glass with phosphor text lines and a cursor."""
    rng = random.Random(720)
    img = new()
    text = [GLOW[2], GLOW[3]] if lit else [GLOW[0], GLOW[1]]
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = GRAPHITE[1]
            else:
                c = SCREEN[1] if lit else SCREEN[0]
                if y in (3, 6, 9, 12) and 2 <= x <= 2 + rng.randrange(6, 12):
                    c = text[1] if rng.random() < 0.3 else text[0]
            put(img, x, y, c)
    if lit:
        put(img, 3, 12, GLOW[4])
        put(img, 4, 12, GLOW[4])
    return img


def keyboard(seed):
    """A keyboard: rows of grey keys with a green-lit key row, on a graphite tray."""
    rng = random.Random(seed)
    img = new(GRAPHITE[1])
    for y in range(2, 14, 3):
        for x in range(1, 15):
            if x % 2:
                key = GRAPHITE[5] if rng.random() < 0.8 else GRAPHITE[4]
                if y == 11:
                    key = GLOW[2] if x in (3, 5, 7) else key
                put(img, x, y, key)
                put(img, x, y + 1, GRAPHITE[3])
    return img


def lamp(on):
    """A status lamp: a round lens in a graphite bezel, dark green when off, bright mint when on."""
    img = new(GRAPHITE[1])
    lens = [GLOW[2], GLOW[3], GLOW[4]] if on else [GLOW[0], GLOW[0], GLOW[1]]
    for y in range(16):
        for x in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d < 42:
                c = lens[2] if d < 6 else lens[1] if d < 20 else lens[0]
                if (x, y) in ((5, 5), (6, 5), (5, 6)):
                    c = (240, 255, 244) if on else GLOW[1]
                put(img, x, y, c)
            elif d < 56:
                put(img, x, y, GRAPHITE[4])
    return img


def port(seed):
    """A power port: a graphite plate with a glowing green ring around a three-pin socket."""
    img = casing(seed)
    for y in range(16):
        for x in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if 14 <= d < 26:
                put(img, x, y, GLOW[2])
            elif d < 14:
                put(img, x, y, GRAPHITE[0])
    for x, y in ((6, 6), (9, 6), (7, 9)):
        put(img, x, y, COPPER[2])
        put(img, x + 1, y, COPPER[1])
    return img


def solar(seed):
    """Solar cells: dark blue-black cells with fine silver busbars and a graphite rim."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (16, 26, 46) if rng.random() < 0.8 else (24, 38, 66)
            if x % 4 == 0 or y % 4 == 0:
                c = (60, 70, 92)
            if x in (0, 15) or y in (0, 15):
                c = GRAPHITE[1]
            put(img, x, y, c)
    for x in range(1, 15):
        put(img, x, 2, (110, 120, 140))
    return img


def cell(seed, palette):
    """A battery cell wrapper: graphite with a vertical charge window lit in the given colour."""
    img = casing(seed)
    for y in range(3, 13):
        for x in (6, 7, 8, 9):
            c = palette[2] if y > 5 else palette[1]
            if x in (6, 9):
                c = palette[1] if y > 5 else GRAPHITE[1]
            put(img, x, y, c)
    return img


def magnet(seed):
    """Nickel-plated magnet segments: bright plating with vertical seams between segments every four pixels and a
    faint cyan reflection, for the rare-earth magnet tier."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = SILVER[1] if rng.random() < 0.8 else SILVER[2]
            if y in (0, 15):
                c = SILVER[0]
            elif x % 4 == 0:
                c = GRAPHITE[1]
            elif x % 4 == 1:
                c = SILVER[2]
            elif y in (4, 5) and x % 4 == 2:
                c = CYAN[3]
            put(img, x, y, c)
    return img


def dark(seed):
    """A near-black sci-fi casing panel: a fine bevel, a recessed groove and a thin cyan trace along the groove's top
    and left edges, like the lit seams of a high-tech machine."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, _noise(rng, DARK[2], DARK[3], 0.15))
    for i in range(16):
        put(img, i, 0, DARK[4])
        put(img, 0, i, DARK[4])
        put(img, i, 15, DARK[0])
        put(img, 15, i, DARK[0])
    for i in range(2, 14):
        put(img, i, 2, CYAN[1])
        put(img, 2, i, CYAN[1])
        put(img, i, 13, DARK[4])
        put(img, 13, i, DARK[4])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, DARK[5])
    return img


def glass(lit):
    """Cyan-tinted glass, drawn opaque: dark teal with two diagonal highlight streaks and a thin rim. Lit, the
    inside glows with a brighter cyan haze."""
    img = new()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = DARK[1]
            else:
                c = GLASS[2] if lit else GLASS[1]
                if lit and 4 <= x <= 11 and 4 <= y <= 11:
                    c = GLASS[3]
                d = (x + y) % 16
                if d in (5, 6) or d == 11:
                    c = GLASS[4] if d == 5 else GLASS[3] if not lit else GLASS[4]
            put(img, x, y, c)
    return img


def conduit(seed):
    """A violet conduit: a dark sheath with a glowing violet line along it (rows 7-8), like the cables' core."""
    img = new(DARK[2])
    band = {4: DARK[4], 5: DARK[3], 6: VIOLET[1], 7: VIOLET[3], 8: VIOLET[2], 9: VIOLET[1], 10: DARK[1], 11: DARK[0]}
    for y in range(16):
        for x in range(16):
            c = band.get(y, DARK[2])
            if x % 8 == 0 and y in (4, 5, 10, 11):
                c = DARK[5]
            put(img, x, y, c)
    return img


def boule(seed):
    """A silicon crystal: bright grey-blue with long vertical facets and a few glints."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            facet = (x // 3) % 3
            c = SILICON[1 + facet]
            if rng.random() < 0.06:
                c = SILICON[4]
            if x % 3 == 0:
                c = SILICON[0]
            put(img, x, y, c)
    return img


def cyan_screen(lit):
    """A cyan monitor: a dark bezel around black glass with cyan graph lines and readouts."""
    rng = random.Random(721)
    img = new()
    lines = [CYAN[2], CYAN[3]] if lit else [CYAN[0], CYAN[1]]
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = DARK[1]
            else:
                c = (6, 18, 24) if lit else (4, 10, 14)
                if y in (3, 5) and 2 <= x <= 2 + rng.randrange(5, 12):
                    c = lines[1] if rng.random() < 0.3 else lines[0]
                if y >= 8 and y == 13 - ((x * 7) % 5):
                    c = lines[1]
            put(img, x, y, c)
    return img


def beige(seed):
    """Beige plastic of a retro computer case: smooth with a faint grain and a soft moulded edge."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = _noise(rng, BEIGE[3], BEIGE[2], 0.12)
            if x == 0 or y == 0:
                c = BEIGE[4]
            elif x == 15 or y == 15:
                c = BEIGE[1]
            put(img, x, y, c)
    return img


def floppy(seed):
    """The front of a retro computer case: beige with two drive bays (dark slots with eject buttons), a vent grille
    and a green power lamp."""
    img = beige(seed)
    for top in (2, 6):
        for y in range(top, top + 3):
            for x in range(2, 14):
                put(img, x, y, BEIGE[1] if y == top else BEIGE[2])
        for x in range(4, 11):
            put(img, x, top + 1, (30, 30, 32))
        put(img, 12, top + 1, BEIGE[0])
    for y in (11, 13):
        for x in range(2, 9):
            put(img, x, y, BEIGE[0])
    put(img, 12, 12, GLOW[3])
    put(img, 13, 12, GLOW[2])
    return img


def retro_keys(seed):
    """A beige keyboard: rows of cream keys with darker function keys along the top."""
    rng = random.Random(seed)
    img = new(BEIGE[1])
    for y in range(1, 16, 3):
        for x in range(1, 15):
            if x % 2:
                key = BEIGE[4] if rng.random() < 0.85 else BEIGE[3]
                if y == 1:
                    key = (120, 112, 100)
                put(img, x, y, key)
                put(img, x, y + 1, BEIGE[2])
    return img


WHITE = [(150, 156, 164), (186, 192, 198), (214, 218, 222), (232, 235, 238), (246, 248, 250)]


def white(seed, border=True):
    """White enamel panels, as on the owner's reference advanced solar panel: a faint brushed grain, a soft bevel and a
    recessed groove two pixels in. Without border it tiles."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, _noise(rng, WHITE[3], WHITE[2], 0.15))
    if border:
        for i in range(16):
            put(img, i, 0, WHITE[4])
            put(img, 0, i, WHITE[4])
            put(img, i, 15, WHITE[1])
            put(img, 15, i, WHITE[1])
        for i in range(2, 14):
            put(img, i, 2, WHITE[1])
            put(img, 2, i, WHITE[1])
            put(img, i, 13, WHITE[4])
            put(img, 13, i, WHITE[4])
    return img


def ribbed(seed):
    """Light brushed steel with vertical ribs, as on the owner's reference engine's cylinder bank."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = WHITE[1] if rng.random() < 0.7 else WHITE[2]
            if x % 4 == 0:
                c = WHITE[0]
            elif x % 4 == 1:
                c = WHITE[3]
            if y in (0, 15):
                c = GRAPHITE[3]
            put(img, x, y, c)
    return img


# Tanks (batch 10), after the owner's reference: light grey panels, yellow-and-black checker bands, dark rims.
TANK = [(118, 122, 128), (150, 154, 160), (176, 180, 186), (198, 202, 207), (222, 225, 229)]


def tank_body(seed):
    """Light grey tank plating: two upright panels with a seam between them, a soft vertical sheen, and rivets."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = _noise(rng, TANK[2], TANK[3], 0.2)
            if x in (2, 3) or x in (10, 11):
                c = TANK[3] if x in (3, 11) else TANK[4]
            if x in (0, 7, 8, 15):
                c = TANK[1] if x in (7, 0) else TANK[0]
            put(img, x, y, c)
    for x, y in ((1, 2), (6, 2), (9, 2), (14, 2), (1, 13), (6, 13), (9, 13), (14, 13)):
        put(img, x, y, TANK[4])
    return img


def tank_checker(seed):
    """A checkered hazard band: two rows of yellow and black squares between thin dark edges."""
    img = new()
    for y in range(16):
        for x in range(16):
            if y in (0, 15):
                c = GRAPHITE[1]
            else:
                c = HAZARD[0] if ((x // 4) + (y // 8)) % 2 == 0 else HAZARD[1]
                if c == HAZARD[0] and y in (1, 8):
                    c = (240, 202, 70)
            put(img, x, y, c)
    return img


def tank_rim(seed):
    """Dark graphite trim for rims, necks and bases, with a light top edge."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = _noise(rng, GRAPHITE[1], GRAPHITE[2], 0.25)
            if y == 0:
                c = GRAPHITE[4]
            put(img, x, y, c)
    return img


def tank_top(seed):
    """The top of a tank: a dark hatch with a light rim and a cross of bolts."""
    img = tank_rim(seed)
    for i in range(16):
        for j in (0, 15):
            put(img, i, j, TANK[3])
            put(img, j, i, TANK[3])
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        put(img, x, y, TANK[4])
    for i in range(5, 11):
        put(img, i, 7, GRAPHITE[0])
        put(img, 7, i, GRAPHITE[0])
    return img


def solar_large(seed):
    """Large-format solar cells: deep blue cells in a fine silver grid with a few sky glints."""
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            c = (22, 48, 104) if rng.random() < 0.75 else (30, 62, 128)
            if x % 4 == 3 or y % 4 == 3:
                c = (78, 104, 150)
            if (x + 2 * y) % 13 == 0 and x % 4 != 3 and y % 4 != 3:
                c = (64, 110, 178)
            put(img, x, y, c)
    return img


def hazard(seed):
    """Yellow-and-black warning stripes for high-voltage edges."""
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, HAZARD[0] if ((x + y) // 4) % 2 == 0 else HAZARD[1])
    return img


def cable(collar):
    """A 6-pixel cable (bands at rows/columns 5-10, read by the cable model's UVs): a graphite sheath with a
    glowing green core seen through a slot along each side, and collars of the tier's metal at the ends where it
    meets a neighbour. The 5-10 centre square is the core, crossed by the glowing line both ways."""
    img = new(GRAPHITE[2])
    band = {5: GRAPHITE[4], 6: GRAPHITE[2], 7: GLOW[3], 8: GLOW[2], 9: GRAPHITE[1], 10: GRAPHITE[0]}
    for y in range(16):
        for x in range(16):
            in_x, in_y = 5 <= x <= 10, 5 <= y <= 10
            if in_x and in_y:
                # The core: graphite with the green line crossing it both ways, so a straight run reads as one
                # continuous glowing line and a junction as a cross.
                c = band[y] if x in (5, 10) else band[x] if y in (5, 10) else GRAPHITE[2]
                if 6 <= x <= 9 and y in (7, 8):
                    c = band[y]
                elif 6 <= y <= 9 and x in (7, 8):
                    c = band[x]
            elif in_y:
                c = collar[1 if y in (6, 7, 8) else 0] if x in (0, 15) else band[y]
                if x in (1, 14) and y in (7, 8):
                    c = GRAPHITE[1]
            elif in_x:
                c = collar[1 if x in (6, 7, 8) else 0] if y in (0, 15) else band[x]
                if y in (1, 14) and x in (7, 8):
                    c = GRAPHITE[1]
            else:
                c = GRAPHITE[2]
            put(img, x, y, c)
    return img


def draw_all():
    save(casing(730), "el_casing")
    save(seams(731), "el_seams")
    save(frame(732), "el_frame")
    save(glow(GLOW), "el_glow")
    save(glow(CYAN), "el_glow_cyan")
    save(light_bars(733, GLOW), "el_light_bars")
    save(light_bars(734, GLOW, count=2), "el_light_bars_wide")
    save(vent(735), "el_vent")
    save(screen(False), "el_screen")
    save(screen(True), "el_screen_on")
    save(keyboard(736), "el_keyboard")
    save(lamp(False), "el_lamp")
    save(lamp(True), "el_lamp_on")
    save(port(737), "el_port")
    save(solar(738), "el_solar")
    save(cell(739, GLOW), "el_cell")
    save(hazard(740), "el_hazard")
    save(magnet(741), "el_magnet")
    # The electronics tier (cyan look) and the retro computer.
    save(white(748), "el_white")
    save(solar_large(749), "el_solar_large")
    save(ribbed(750), "el_ribbed")
    save(tank_body(751), "tk_body")
    save(tank_checker(752), "tk_checker")
    save(tank_rim(753), "tk_rim")
    save(tank_top(754), "tk_top")
    save(dark(742), "el_dark")
    save(glass(False), "el_glass")
    save(glass(True), "el_glass_on")
    save(glow(VIOLET), "el_glow_violet")
    save(glow(VANADIUM), "el_glow_blue")
    save(conduit(743), "el_conduit")
    save(boule(744), "el_boule")
    save(cyan_screen(False), "el_screen_cyan")
    save(cyan_screen(True), "el_screen_cyan_on")
    save(beige(745), "rt_beige")
    save(floppy(746), "rt_floppy")
    save(retro_keys(747), "rt_keys")
