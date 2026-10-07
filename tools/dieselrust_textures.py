"""Original 16x16 textures for the dieselpunk giants of batch 44 (tools/giant_models.py) and everything built from them.

The giants' parts: weathered riveted steel plate, green verdigris patina on copper housings, perforated covers with
round holes, copper and steel pipes with hex-nut fittings, banded domes and coil stacks, chipped red paint, skid feet
and glowing amber conduits.

Drawn in the clean style the owner asked for on 4 October 2026 (tools/clean_metal.py, docs/ART_DIRECTION.md): flat
fills from short palettes, bevelled panels and bolts, and wear only as a few small marks at corners and seams. Rust
is an accent (a stain under a bolt, a run from a seam), never the whole surface. Deterministic: the same code always
draws the same pixels. Names start with "dr_". Called from generate_textures.py.
"""
import math

from clean_metal import CHIP, CHIP_WIDE, bevel, bolt, corner_bolts, inset, patch, plate, ramp, rect, scuffs, stain
from steampunk_textures import new, put, save

RUST = [(46, 26, 18), (70, 38, 22), (98, 52, 28), (128, 68, 34), (156, 88, 44), (184, 112, 58), (206, 140, 80)]
PATINA = [(30, 62, 54), (46, 88, 74), (66, 116, 96), (90, 146, 120), (122, 176, 146), (160, 204, 176)]
COPPER = [(84, 40, 24), (122, 62, 36), (162, 88, 50), (196, 116, 70), (224, 152, 100), (244, 196, 150)]
IRON = [(22, 20, 20), (36, 33, 32), (52, 48, 46), (70, 65, 62), (92, 86, 82), (120, 112, 106)]
RED = [(70, 18, 14), (104, 26, 20), (138, 36, 26), (168, 52, 36), (192, 78, 54)]
AMBER = [(70, 34, 6), (130, 70, 10), (196, 118, 20), (240, 168, 40), (255, 210, 96), (255, 240, 180)]
BLUE = [(18, 40, 70), (28, 62, 104), (44, 88, 140), (78, 124, 176)]
STEEL = [(64, 66, 68), (92, 94, 96), (124, 126, 126), (160, 160, 156), (196, 194, 188)]


# The giants' weathered steel: a warm mid grey, like tuff, so the red paint, copper and patina stand out against it.
PLATE = [(44, 42, 44), (66, 63, 63), (86, 82, 80), (106, 101, 97), (128, 122, 116), (152, 146, 138)]
# Copper as vanilla draws it: warm orange with a pink-brown shadow and a pale highlight.
BRIGHT_COPPER = [(118, 56, 40), (156, 80, 54), (190, 106, 74), (216, 134, 96), (236, 166, 128)]
VERDIGRIS = [(40, 92, 82), (58, 120, 104), (78, 148, 126), (104, 174, 148), (140, 198, 172)]


def rust_stain(img, x, y, drip=0):
    """A small rust stain (a seam or bolt weeping), with an optional run below it."""
    stain(img, x, y, RUST[2], RUST[3], RUST[3], drip)


def rust_plate(seed, rivets=True):
    """Weathered steel plate: a bevelled panel split by a seam, bolted at the corners, rust weeping from two spots."""
    img = new()
    plate(img, PLATE)
    scuffs(img, PLATE[3], only=PLATE[2])
    if rivets:
        for x in range(1, 15):
            put(img, x, 7, PLATE[1])
            put(img, x, 8, PLATE[4])
        corner_bolts(img, PLATE)
        rust_stain(img, 12, 4, drip=2)
        patch(img, CHIP_WIDE, RUST[3], 2, 13)
    else:
        rust_stain(img, 10 if seed % 2 else 4, 9, drip=1)
    return img


def patina_plate(seed, holes=False):
    """Copper gone green: a bevelled verdigris panel with a recessed inset, bolted corners and bare copper showing
    at two worn corners; optionally a perforated cover with nine round holes."""
    img = new()
    plate(img, VERDIGRIS)
    scuffs(img, VERDIGRIS[3], only=VERDIGRIS[2])
    patch(img, CHIP, BRIGHT_COPPER[2], 2, 2)
    patch(img, [(0, 0), (-1, 0), (0, -1)], BRIGHT_COPPER[1], 13, 13)
    if holes:
        for cy in (3, 7, 11):
            for cx in (3, 7, 11):
                rect(img, cx, cy, cx + 1, cy + 1, IRON[0])
                put(img, cx, cy, IRON[1])
                put(img, cx + 2, cy + 1, VERDIGRIS[3])
                put(img, cx + 1, cy + 2, VERDIGRIS[3])
    else:
        inset(img, 4, 4, 11, 11, VERDIGRIS[3], VERDIGRIS[1])
        bolt(img, 12, 2, VERDIGRIS)
        bolt(img, 2, 12, VERDIGRIS)
    return img


def ribbed(seed, palette, accent=None):
    """Horizontal ribs every four rows (coil stacks, dome bands): each rib lit on top, shaded underneath."""
    img = new()
    ramp(img, (len(palette) - 2, 2, 2, 0), palette, 4)
    if accent:
        for x, y in ((3, 1), (4, 1), (11, 9), (12, 9), (12, 10)):
            put(img, x, y, accent)
    return img


def banded_dome(seed):
    """The dome: two courses of copper panels laid like brickwork, each lit along its top edge, a few rivets on the
    seams and a touch of green where the rain sits."""
    img = new()
    for band, (y0, seams) in enumerate(((0, (0, 8)), (8, (4, 12)))):
        bevel(img, 0, y0, 15, y0 + 7, BRIGHT_COPPER[3], BRIGHT_COPPER[0], BRIGHT_COPPER[2])
        for x in seams:
            for y in range(y0, y0 + 8):
                put(img, x, y, BRIGHT_COPPER[0])
                put(img, x + 1, y, BRIGHT_COPPER[3] if y > y0 else BRIGHT_COPPER[3])
        for x in seams:
            put(img, x + 1, y0 + 3, BRIGHT_COPPER[4])
    scuffs(img, VERDIGRIS[2], [(6, 6, 2), (13, 14, 2)])
    return img


def pipe_metal(seed, palette, spots):
    """Pipe stock: shaded like a cylinder (a bright highlight row and a dark underside), with one band of wear."""
    img = new()
    ramp(img, (1, 2, 3, 4, 3, 2, 2, 1), palette, 8)
    for x in (5, 6, 11):
        put(img, x, 10, spots[0])
    return img


def hex_nut():
    """A hex-nut fitting seen face on: a six-sided steel nut round a dark bore, on a bevelled steel plate."""
    img = new()
    plate(img, PLATE, outline=False)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            # Hexagon with flats top and bottom.
            inside = abs(dy) <= 6.5 and abs(dx) * 0.866 + abs(dy) * 0.5 <= 6.5
            if inside:
                c = STEEL[3] if dy < -2 else STEEL[2] if dy < 3 else STEEL[1]
                if math.hypot(dx, dy) < 2.6:
                    c = IRON[0]
                elif math.hypot(dx, dy) < 3.6:
                    c = STEEL[4]
                put(img, x, y, c)
    return img


def red_paint(seed):
    """Red paint on steel: a bevelled panel with a recessed inset, bolts, and paint chipped to bare steel at two
    corners."""
    img = new()
    plate(img, RED)
    scuffs(img, RED[3], only=RED[2])
    inset(img, 4, 4, 11, 11, RED[3], RED[1])
    corner_bolts(img, PLATE)
    patch(img, CHIP_WIDE, PLATE[3], 1, 12)
    put(img, 2, 13, PLATE[2])
    patch(img, CHIP, PLATE[3], 12, 1)
    return img


def rivet_band(seed):
    """A dark steel band with a row of bolts along its middle: flanges and hoops."""
    img = new()
    bevel(img, 0, 0, 15, 15, PLATE[3], PLATE[0], PLATE[1])
    for x in range(1, 15):
        put(img, x, 4, PLATE[0])
        put(img, x, 11, PLATE[2])
    for x in range(2, 15, 4):
        bolt(img, x, 7, PLATE)
    return img


def skid(seed):
    """Dark skid steel with two weld beads across it and one grease mark."""
    img = new(IRON[2])
    for y in (3, 12):
        for x in range(16):
            put(img, x, y, IRON[4] if x % 2 else IRON[3])
            put(img, x, y + 1, IRON[1])
    scuffs(img, IRON[1], [(5, 7, 3), (10, 9, 2)])
    return img


def grate(seed):
    """Floor grating: a square grid of steel bars, lit on top, over dark gaps."""
    img = new(IRON[0])
    for y in range(16):
        for x in range(16):
            if y % 4 == 0:
                put(img, x, y, PLATE[3])
            elif x % 4 == 0:
                put(img, x, y, PLATE[2] if y % 4 != 3 else PLATE[1])
    return img


def amber(on):
    """A glowing amber conduit window behind cage bars; dim when the machine is idle."""
    img = new()
    base = 3 if on else 1
    for y in range(16):
        for x in range(16):
            glow = 1 - min(1.0, abs(y - 7.5) / 8)
            c = AMBER[min(5, base + int(glow * (3 if on else 1.5)))]
            if x % 5 == 0:
                c = IRON[1]
            if y in (0, 15):
                c = IRON[2]
            put(img, x, y, c)
    return img


def fire_door(on):
    """A bolted steel firebox door with three draft slots that glow while it burns, and a latch."""
    img = new()
    plate(img, PLATE)
    for y in (5, 8, 11):
        for x in range(3, 13):
            put(img, x, y - 1, IRON[0])
            put(img, x, y, AMBER[4 if on else 1] if x % 3 else AMBER[5 if on else 2])
    corner_bolts(img, PLATE)
    put(img, 13, 7, STEEL[4])
    put(img, 13, 8, STEEL[2])
    return img


def fan():
    """A turbine fan in its steel housing, seen face on: a lit rim, steel blades and a blue hub."""
    img = new(PLATE[1])
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r > 7.6:
                continue
            if r > 6.5:
                put(img, x, y, PLATE[4] if dy < 0 else PLATE[2])
                continue
            angle = math.atan2(dy, dx)
            blade = (angle * 7 / (2 * math.pi) + r * 0.08) % 1.0
            c = STEEL[3] if blade < 0.45 else IRON[1]
            if r < 1.8:
                c = BLUE[3]
            elif r < 2.6:
                c = BLUE[1]
            put(img, x, y, c)
    return img


def coil(seed):
    """Copper windings: tight horizontal turns, each lit along its top."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = BRIGHT_COPPER[3] if y % 2 == 0 else BRIGHT_COPPER[1]
            if y % 2 == 0 and (x + y * 3) % 11 == 0:
                c = BRIGHT_COPPER[4]
            put(img, x, y, c)
    return img


def blue_cap(seed):
    """Blue-painted end caps: a bevelled blue panel worn to bare steel at one corner."""
    img = new()
    plate(img, BLUE + [(110, 150, 196)])
    patch(img, CHIP, STEEL[2], 12, 12)
    return img


def blade(seed):
    """Brushed saw steel in rings, with a bright edge where the teeth run."""
    img = new()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            c = STEEL[2 + int(r) % 2]
            if r > 6.6:
                c = STEEL[4] if (x + y) % 2 else IRON[2]
            if r < 1.5:
                c = IRON[1]
            put(img, x, y, c)
    return img


def crt(on):
    """A round-cornered amber phosphor screen with scan lines and a trace."""
    img = new(IRON[1])
    for y in range(2, 14):
        for x in range(2, 14):
            if (x in (2, 13)) and (y in (2, 13)):
                continue
            c = AMBER[1 if on else 0] if y % 2 else AMBER[2 if on else 0]
            put(img, x, y, c)
    if on:
        for x in range(3, 13):
            y = 8 + round(2.5 * math.sin(x * 0.9))
            put(img, x, y, AMBER[5])
        for x in range(3, 8):
            put(img, x, 4, AMBER[4])
    return img


LEAF_GREENS = [(30, 70, 26), (44, 98, 34), (62, 128, 44), (90, 160, 60), (130, 194, 86)]
# Each leaf: its top-left corner. They overlap like a canopy and the pattern wraps, so the texture tiles.
LEAVES = ((0, 0), (8, 1), (4, 5), (12, 6), (0, 9), (8, 10), (4, 13), (12, 14))


def leaves(seed):
    """Hydroponic greens: a canopy of overlapping leaves, each lit along its top with a pale midrib."""
    img = new(LEAF_GREENS[0])
    for lx, ly in LEAVES:
        for dy, row in enumerate(("  ##  ", " #### ", "######", " #### ", "  ##  ")):
            for dx, ch in enumerate(row):
                if ch == "#":
                    c = LEAF_GREENS[3] if dy <= 1 else LEAF_GREENS[2] if dy <= 2 else LEAF_GREENS[1]
                    img.putpixel(((lx + dx) % 16, (ly + dy) % 16), c + (255,))
        for dx in range(1, 5):
            img.putpixel(((lx + dx) % 16, (ly + 2) % 16), LEAF_GREENS[4] + (255,))
    return img


def solution(seed, palette):
    """A tank's liquid surface: a deep colour with long, regular ripple lines."""
    img = new(palette[1])
    for y in range(16):
        for x in range(16):
            wave = (x + 2 * y) % 16
            if wave in (0, 1) and y % 4 == 1:
                put(img, x, y, palette[3])
            elif wave in (8, 9, 10) and y % 4 == 3:
                put(img, x, y, palette[2])
    return img


def soot():
    """The sooty inside of a smokestack or engine opening: a dark steel rim round a near-black throat, one shade
    darker at each step in."""
    img = new(IRON[0])
    for i, c in enumerate((PLATE[1], IRON[2], IRON[1])):
        for x in range(i, 16 - i):
            for y in (i, 15 - i):
                put(img, x, y, c)
                put(img, y, x, c)
    return img


ICE = [(150, 186, 196), (186, 214, 222), (220, 236, 240), (246, 250, 252)]


def frost(seed):
    """Frosted pipework: a pale ice crust in soft bands, a few bright crystals and green patina showing through."""
    img = new()
    ramp(img, (1, 2, 2, 2, 1, 1, 2, 2), ICE, 8)
    for x, y in ((3, 2), (11, 5), (6, 10), (13, 13), (1, 12)):
        put(img, x, y, ICE[3])
        put(img, x + 1, y, ICE[3])
        put(img, x, y + 1, ICE[2])
    patch(img, [(0, 0), (1, 0), (1, 1)], VERDIGRIS[3], 8, 7)
    patch(img, [(0, 0), (1, 0)], VERDIGRIS[3], 2, 4)
    return img


def draw_all():
    save(rust_plate(4400), "dr_rust")
    save(rust_plate(4401, rivets=False), "dr_rust_bare")
    save(patina_plate(4402), "dr_patina")
    save(patina_plate(4403, holes=True), "dr_perforated")
    save(ribbed(4404, VERDIGRIS), "dr_ribbed_patina")
    save(ribbed(4405, PLATE), "dr_ribbed_rust")
    save(banded_dome(4406), "dr_dome")
    save(pipe_metal(4407, [BRIGHT_COPPER[0]] + BRIGHT_COPPER, [VERDIGRIS[3]]), "dr_copper_pipe")
    save(pipe_metal(4408, PLATE, [RUST[3]]), "dr_rust_pipe")
    save(hex_nut(), "dr_nut")
    save(red_paint(4409), "dr_red")
    save(rivet_band(4410), "dr_band")
    save(skid(4411), "dr_skid")
    save(grate(4412), "dr_grate")
    save(amber(False), "dr_amber")
    save(amber(True), "dr_amber_on")
    save(fire_door(False), "dr_fire_door")
    save(fire_door(True), "dr_fire_door_on")
    save(fan(), "dr_fan")
    save(coil(4413), "dr_coil")
    save(blue_cap(4414), "dr_blue")
    save(blade(4415), "dr_blade")
    save(crt(False), "dr_crt")
    save(crt(True), "dr_crt_on")
    save(leaves(4416), "dr_leaves")
    save(solution(4417, [(10, 40, 46), (20, 70, 80), (40, 110, 120), (120, 190, 196)]), "dr_wash_water")
    save(solution(4418, [(20, 60, 30), (40, 110, 60), (70, 150, 90), (160, 220, 150)]), "dr_plating_bath")
    save(frost(4419), "dr_frost")
    save(soot(), "dr_soot")
