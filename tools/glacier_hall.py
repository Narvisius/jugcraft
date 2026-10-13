"""The Glacier Hall (docs/features/glacier-hall.md): the Yeti King's lair, written as the structure template
data/jugcraft/structure/lair/glacier_hall.nbt, which lair/Lairs places into every instance of the jugcraft:glacier_hall
dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI checks it is current).

A cavern in the heart of a glacier: 80 blocks across (x), 44 high (y) and 88 long (z, from the north wall at 0 to the
south wall at 87), walled and vaulted in packed ice veined with blue. The cavern (cavern()) is a vast hall under a
dome, an apse to the north for the throne, a den in each side wall and an alcove high in the south wall, each half an
ellipsoid on its floor, their walls rippled by a smooth noise; only the ice touching the open air is kept. From south
to north:
- the arrival (ARRIVAL): a ledge high in the south wall, LEDGE blocks above the lake, under a snow-choked crevasse that
  rises through the ice to the daylight (CREVASSE). Beside it, Grey Mist hangs in an arch of blue ice (EXIT): the way
  home;
- the snow ramp (RAMP): trampled snow from the ledge down to the lake's south shore, half a block lower each block, on
  a drift of snow banked against the wall;
- the frozen lake, the arena: LAKE_RADIUS across its centre, crusted with drift snow. The wind has scoured a band of its
  shore and a patch in its middle to bare glare ice. Four ice columns (COLUMNS) rise from it to the vault; round each
  the snow is trampled hard (COLUMN_RING), as it is at the ramp's foot, before the dais and on the paths from the dens:
  the footing the King never bares (trampled());
- the throne: on the north shore, a dais of three blue ice steps topped with trampled snow (TIERS), his throne of ice on
  the top step (THRONE) under an arch of two mammoth tusks (TUSKS), his frozen hoard heaped about it (HOARD);
- the dens (DENS): two caves in the east and west walls at the lake's level, bones and pelts on their snowy floors;
- overhead: giant icicles hanging from the vault, and a crack across its crown (CRACK) open to the pale sky.
The light is hidden light blocks: soaked through the ice everywhere, brightest under the crack and at the arrival.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written (the lair-only blocks' are written with them); vanilla blocks use only properties they have had for
years. Nothing in it melts: no vanilla ice or snow layers, which the hidden lights would melt. Deterministic: the same
file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/glacier_hall"

SIZE = (80, 44, 88)
LAKE = 4                           # the lake's layer: players stand on it at LAKE + 1
LAKE_CENTRE = (39.5, 36.5)
LAKE_RADIUS = 20.5
SHORE_BAND = 1.5                   # the scoured band of glare ice round the lake's edge
GLARE_PATCH = 2.5                  # the scoured patch of glare ice in its middle, under the crack
APRON = 4.0                        # how far the trampled snow reaches onto the lake at the ramp, the dais and the dens
# The four ice columns rising from the lake to the vault: (x, z) of their centres, their radius at the lake (wider at
# their foot, flaring into the vault) and the ring of trampled snow round each.
COLUMNS = ((26.5, 23.5), (52.5, 23.5), (26.5, 49.5), (52.5, 49.5))
COLUMN_RADIUS = 2.6
COLUMN_FOOT = 1.4
COLUMN_RING = 2.0
# The cavern's parts, each half an ellipsoid standing on its floor: (x, y of the floor's blocks, z) and radii (x, y, z).
HALL = ((39.5, LAKE, 40.0), (31.0, 30.0, 39.5))
APSE = ((39.5, LAKE, 9.0), (17.0, 24.0, 9.5))
DENS = {"west": ((6.0, LAKE, 36.5), (7.0, 7.0, 6.5)), "east": ((73.0, LAKE, 36.5), (7.0, 7.0, 6.5))}
LEDGE = 10                         # the arrival ledge's height over the lake
ARRIVAL = (39.5, LAKE + 1 + LEDGE, 81.5, 180.0)     # x, y, z and yaw (north): on the ledge, facing the lake
ALCOVE = ((39.5, LAKE + LEDGE, 80.0), (8.5, 7.5, 8.0))
WOBBLE = 0.3                       # how far the noise pushes the walls in and out (of an ellipsoid's radius squared)
# The crevasse rising from the back of the ledge to the daylight: its middle (x), its rows (z), its half-width at the
# ledge and how much narrower it is a block up. Snow fills its foot, rising a block a row.
CREVASSE = {"x": 39.5, "z": (83, 86), "half_width": 2.5, "narrowing": 0.08}
# The snow ramp: five blocks wide, from the ledge (row "top", at the arrival's height) down to the lake's south shore,
# half a block lower each block north, flat from row "foot".
RAMP = {"x": (37, 41), "top": 78, "foot": 58}
# The Grey Mist in its arch of blue ice on the ledge, east of the arrival and facing it: the arch's column (x) and the
# mist's rows (z), three blocks high.
EXIT = {"x": 44, "z": (80, 81)}
# The dais: three steps, each a rounded square round DAIS_CENTRE of these half-sizes (x, z), a block higher than the
# last; the throne stands on the top one.
DAIS_CENTRE = (39.5, 7.0)
TIERS = ((12.5, 8.5), (10.0, 6.5), (7.5, 4.5))
STEP_TOP = LAKE + 1 + len(TIERS)   # where the top step is stood on
# The throne: its seat (x and z ranges), its back behind it rising to "back_top", with a crown of points along it.
THRONE = {"seat": ((36, 42), (5, 8)), "back": ((35, 43), (3, 4)), "back_top": LAKE + 12}
CROWN = (1, 0, 2, 0, 3, 0, 2, 0, 1)
# The mammoth tusks arching over the throne, bowed forward from the plane z TUSKS["z"]: each a quarter circle round
# "centre" (x, y) of "radius", from its root on the lowest step "sweep" degrees up toward the other, "root" thick at the
# root and "tip" at the point.
TUSKS = {"z": 9.5, "centre": (39.5, LAKE + 3.0), "radius": 10.5, "root": 1.7, "tip": 0.55, "sweep": 83.0, "bow": 1.2}
# The frozen hoard heaped on the steps beside the throne: (x, z) and radius of each heap.
HOARD = (((32.5, 6.5), 2.4), ((46.5, 6.5), 2.4), ((31.5, 11.5), 1.6), ((47.5, 11.5), 1.6))
# The crack across the vault's crown, open to the sky: its columns (x), the row it wanders about (z) and how far.
CRACK = {"x": (31, 48), "z": 36, "wander": 1.5}
ICICLE_CLEARANCE = LAKE + 15       # no icicle hangs lower than this
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
CENTRE = (39.5, 43.5)
BOUNDS = 46
FLOOR = LAKE - 2
# The hidden lights: a lattice through the hall at two heights (y, level, spacing), and these: (x, y, z, level).
LIGHT_LAYERS = ((LAKE + 6, 12, 8), (LAKE + 17, 13, 10))
LIGHTS = [
    (39, LAKE + 4, 36, 15), (39, LAKE + 22, 36, 15),
    (39, ARRIVAL[1] + 3, 81, 13), (39, ARRIVAL[1] + 11, 84, 15),
    (6, LAKE + 3, 36, 10), (73, LAKE + 3, 36, 10),
    (39, STEP_TOP + 7, 12, 13),
]

# ---------------------------------------------------------------- states

DRIFT = block("jugcraft:drift_snow")
TRAMPLED = block("jugcraft:trampled_snow", height=2)
TRAMPLED_HALF = block("jugcraft:trampled_snow", height=1)
GLARE = block("jugcraft:glare_ice")
TUSK = block("jugcraft:mammoth_tusk")
HOARD_ICE = block("jugcraft:frozen_hoard")
EXIT_MIST = block("jugcraft:lair_exit", axis="z")
PACKED = block("minecraft:packed_ice")
BLUE = block("minecraft:blue_ice")
SNOW = block("minecraft:snow_block")
PELT = block("minecraft:white_carpet")


def icicle(part):
    return block("jugcraft:giant_icicle", part=part)


def bone(axis):
    return block("minecraft:bone_block", axis=axis)


def light(level):
    return block("minecraft:light", level=level, waterlogged=False)


# ---------------------------------------------------------------- shapes

def noise(x, y, z, scale, salt):
    """Smooth value noise, 0 to 1: the hash at the corners of a lattice `scale` blocks apart, blended smoothly."""
    fx, fy, fz = x / scale, y / scale, z / scale
    ix, iy, iz = math.floor(fx), math.floor(fy), math.floor(fz)
    tx, ty, tz = (t * t * (3.0 - 2.0 * t) for t in (fx - ix, fy - iy, fz - iz))
    total = 0.0
    for cx in (0, 1):
        for cy in (0, 1):
            for cz in (0, 1):
                weight = (tx if cx else 1.0 - tx) * (ty if cy else 1.0 - ty) * (tz if cz else 1.0 - tz)
                total += weight * _hash(ix + cx, iy + cy, iz + cz, salt)
    return total


def dome(shape, x, y, z, salt):
    """Whether cell (x, y, z) is open under one of the cavern's half-ellipsoids, its surface rippled by the noise."""
    (cx, floor, cz), (rx, ry, rz) = shape
    if y <= floor:
        return False
    e = ((x + 0.5 - cx) / rx) ** 2 + ((y - floor - 0.5) / ry) ** 2 + ((z + 0.5 - cz) / rz) ** 2
    if e < 1.0 - WOBBLE / 2:
        return True
    if e > 1.0 + WOBBLE / 2:
        return False
    return e < 1.0 + WOBBLE * (noise(x, y, z, 7.0, salt) - 0.5)


def crack_row(x):
    """The row (z) of the crack's southern half over column x."""
    return CRACK["z"] + int(round(CRACK["wander"] * math.sin(0.55 * x)))


def cavern():
    """The open cells of the cavern: the hall, the apse, the dens and the alcove; the crevasse and the crack, open to
    the sky. Kept a block inside the template, so its ice fits round it (but for the crevasse's and the crack's tops)."""
    w, h, length = SIZE
    cells = set()
    for salt, shape in enumerate((HALL, APSE, DENS["west"], DENS["east"], ALCOVE)):
        (cx, floor, cz), (rx, ry, rz) = shape
        reach = 1.0 + WOBBLE
        for x in range(max(1, int(cx - rx * reach) - 1), min(w - 1, int(cx + rx * reach) + 2)):
            for z in range(max(1, int(cz - rz * reach) - 1), min(length - 1, int(cz + rz * reach) + 2)):
                for y in range(floor + 1, min(h - 1, int(floor + ry * reach) + 2)):
                    if dome(shape, x, y, z, 11 + salt):
                        cells.add((x, y, z))
    # Headroom over the snow ramp, where it climbs into the south wall: four blocks over its surface, a block either side.
    for z in range(RAMP["foot"], RAMP["top"] + 1):
        surface = int(ramp_height(z))
        for x in range(RAMP["x"][0] - 1, RAMP["x"][1] + 2):
            for y in range(surface, surface + 4):
                cells.add((x, y, z))
    z0, z1 = CREVASSE["z"]
    for y in range(ARRIVAL[1], h):
        half = max(0.5, CREVASSE["half_width"] - CREVASSE["narrowing"] * (y - ARRIVAL[1]))
        for z in range(z0, z1 + 1):
            for x in range(w):
                if abs(x + 0.5 - CREVASSE["x"]) <= half:
                    cells.add((x, y, z))
    for x in range(CRACK["x"][0], CRACK["x"][1] + 1):
        z = crack_row(x)
        under = [y for y in range(h) if (x, y, z) in cells]
        if under:
            for y in range(max(under) + 1, h):
                cells.add((x, y, z))
                cells.add((x, y, z + 1))
    return cells


def lake_radius(x, z):
    return math.hypot(x + 0.5 - LAKE_CENTRE[0], z + 0.5 - LAKE_CENTRE[1])


def trampled(x, z):
    """Whether the snow at (x, z), on the lake or its shore, is trampled hard: round an ice column, at the ramp's foot,
    before the dais and on the paths from the dens. The King never bares it."""
    px, pz = x + 0.5, z + 0.5
    cx, cz = LAKE_CENTRE
    for sx, sz in COLUMNS:
        if math.hypot(px - sx, pz - sz) <= COLUMN_RADIUS + COLUMN_FOOT + COLUMN_RING:
            return True
    if RAMP["x"][0] - 1 <= x <= RAMP["x"][1] + 1 and pz >= cz + LAKE_RADIUS - APRON:
        return True
    if abs(px - cx) <= TIERS[0][0] - 2.0 and pz <= cz - LAKE_RADIUS + APRON:
        return True
    return abs(pz - cz) <= 1.5 and abs(px - cx) >= LAKE_RADIUS - APRON


def lake(x, z):
    """The lake's block at (x, z), or None off the lake: trampled snow where trampled(), glare ice on the scoured shore
    and in the middle, drift snow everywhere else."""
    r = lake_radius(x, z)
    if r > LAKE_RADIUS:
        return None
    if trampled(x, z):
        return TRAMPLED
    if r <= GLARE_PATCH or r > LAKE_RADIUS - SHORE_BAND:
        return GLARE
    return DRIFT


def in_den(x, z):
    for (cx, _, cz), (rx, _, rz) in DENS.values():
        if ((x + 0.5 - cx) / rx) ** 2 + ((z + 0.5 - cz) / rz) ** 2 <= 1.0:
            return True
    return False


def ice(x, y, z):
    """The glacier's ice in the walls and vault: packed ice veined with blue, snow drifted against the walls' foot."""
    if y <= LAKE + 2 and noise(x, y, z, 4.0, 22) > 0.55:
        return SNOW
    return BLUE if noise(x, y, z, 5.0, 21) > 0.64 else PACKED


def ground(x, y, z):
    """The floor under the open cell above (x, y, z): the lake, the shore's and the dens' snow (scoured to packed ice in
    places; trampled on the paths), the ledge's trampled snow."""
    if y == LAKE:
        on_lake = lake(x, z)
        if on_lake is not None:
            return on_lake
        if not in_den(x, z) and trampled(x, z):
            return TRAMPLED
        return PACKED if noise(x, y, z, 4.0, 23) > 0.78 else SNOW
    if y == LAKE + LEDGE:
        return TRAMPLED
    return SNOW


# ---------------------------------------------------------------- building

def build():
    cells = cavern()
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    walls(put, cells)
    columns(put, cells)
    snow_ramp(put, cells, blocks)
    crevasse(put, cells)
    exit_arch(put)
    dais(put, cells)
    hoard(put)
    throne(put)
    tusks(put)
    dens(put, cells, blocks)
    icicles(put, cells, blocks)
    for y, level, spacing in LIGHT_LAYERS:
        for x in range(4, SIZE[0], spacing):
            for z in range(4, SIZE[2], spacing):
                if (x, y, z) in cells and (x, y, z) not in blocks:
                    put(x, y, z, light(level))
    for x, y, z, level in LIGHTS:
        if (x, y, z) not in blocks:
            put(x, y, z, light(level))
    return hollow(blocks)


def walls(put, cells):
    """The ice round the open cells (beside, above, below or across a corner): floors under them, walls and vault."""
    w, h, length = SIZE
    done = set()
    for (x, y, z) in cells:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    n = (x + dx, y + dy, z + dz)
                    if n in cells or n in done or not (0 <= n[0] < w and 0 <= n[1] < h and 0 <= n[2] < length):
                        continue
                    done.add(n)
                    nx, ny, nz = n
                    put(nx, ny, nz, ground(nx, ny, nz) if (nx, ny + 1, nz) in cells else ice(nx, ny, nz))


def column_top(cells, sx, sz):
    x, z = int(sx), int(sz)
    return max(y for y in range(SIZE[1]) if (x, y, z) in cells)


def columns(put, cells):
    """The four ice columns, from the lake up into the vault: banded packed and blue ice, wider at the foot, flaring where
    they meet the vault."""
    for i, (sx, sz) in enumerate(COLUMNS):
        top = column_top(cells, sx, sz)
        for y in range(LAKE, top + 1):
            foot = COLUMN_FOOT * max(0.0, 1.0 - (y - LAKE) / 4.0) ** 2
            crown = 2.4 * max(0.0, 1.0 - (top - y) / 7.0) ** 2
            radius = COLUMN_RADIUS + foot + crown + 0.35 * (noise(sx, y, sz, 3.0, 31 + i) - 0.5)
            reach = int(radius) + 2
            for x in range(int(sx) - reach, int(sx) + reach + 1):
                for z in range(int(sz) - reach, int(sz) + reach + 1):
                    if math.hypot(x + 0.5 - sx, z + 0.5 - sz) <= radius and ((x, y, z) in cells or y == LAKE):
                        band = int((y + 2.0 * noise(x, y, z, 4.0, 33)) / 2.0) % 3 == 0
                        put(x, y, z, BLUE if band else PACKED)


def ramp_height(z):
    """The height players stand at (in half blocks) on the ramp's row z, or None off it."""
    top, foot = RAMP["top"], RAMP["foot"]
    if not foot <= z <= top:
        return None
    return max(LAKE + 1.0, ARRIVAL[1] - (top - z) * 0.5)


def snow_ramp(put, cells, blocks):
    """The ramp: trampled snow on top, a whole or a half block, on a drift of snow; the drift's banks fall away a block
    a block either side. Only where the cavern is open, or over its ice."""
    (x0, x1), middle = RAMP["x"], (RAMP["x"][0] + RAMP["x"][1] + 1) / 2.0
    half = (x1 - x0 + 1) / 2.0
    for z in range(RAMP["foot"], RAMP["top"] + 1):
        h = ramp_height(z)
        for x in range(x0 - 12, x1 + 13):
            beyond = max(0.0, abs(x + 0.5 - middle) - half)
            if beyond == 0.0:
                surface = h
            else:
                surface = math.floor(h - beyond)
            if surface <= LAKE + 1:
                continue
            whole = int(math.floor(surface))
            for y in range(LAKE + 1, whole):
                if (x, y, z) in cells or (x, y, z) in blocks:
                    put(x, y, z, SNOW)
            if beyond == 0.0:
                if surface - whole >= 0.5:
                    put(x, whole, z, TRAMPLED_HALF)   # a half block on the snow below
                else:
                    put(x, whole - 1, z, TRAMPLED)


def crevasse(put, cells):
    """The snow choking the crevasse's foot: a block deep at its mouth, a block deeper each row back."""
    z0, z1 = CREVASSE["z"]
    for z in range(z0, z1 + 1):
        for y in range(ARRIVAL[1], ARRIVAL[1] + 1 + (z - z0)):
            for x in range(SIZE[0]):
                if (x, y, z) in cells:
                    put(x, y, z, SNOW)


def exit_arch(put):
    """The arch of blue ice on the ledge, the Grey Mist hanging in it facing west to the arrival."""
    x, (z0, z1) = EXIT["x"], EXIT["z"]
    y0 = ARRIVAL[1]
    for y in range(y0, y0 + 3):
        for z in range(z0, z1 + 1):
            put(x, y, z, EXIT_MIST)
        put(x, y, z0 - 1, BLUE)
        put(x, y, z1 + 1, BLUE)
    for z in range(z0 - 1, z1 + 2):
        put(x, y0 + 3, z, BLUE)
    for z in range(z0, z1 + 1):
        put(x, y0 + 4, z, BLUE)


def exit_mist():
    """Where the Grey Mist in the arch is (for the audit and the tests)."""
    x, (z0, z1) = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(ARRIVAL[1], ARRIVAL[1] + 3) for z in range(z0, z1 + 1)]


def tier(k, x, z):
    """Whether (x, z) is on step k (0 the lowest) of the dais."""
    a, b = TIERS[k]
    dx, dz = (x + 0.5 - DAIS_CENTRE[0]) / a, (z + 0.5 - DAIS_CENTRE[1]) / b
    return dx ** 4 + dz ** 4 <= 1.0


def step_height(x, z):
    """Where (x, z) on the dais is stood on (LAKE + 1 off it)."""
    return LAKE + 1 + sum(1 for k in range(len(TIERS)) if tier(k, x, z))


def dais(put, cells):
    """The dais: each step blue ice round its edge and trampled snow on its top."""
    for k in range(len(TIERS)):
        y = LAKE + 1 + k
        a, b = TIERS[k]
        for x in range(int(DAIS_CENTRE[0] - a) - 1, int(DAIS_CENTRE[0] + a) + 2):
            for z in range(int(DAIS_CENTRE[1] - b) - 1, int(DAIS_CENTRE[1] + b) + 2):
                if not tier(k, x, z) or (x, y, z) not in cells:
                    continue
                rim = any(not tier(k, x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                put(x, y, z, BLUE if rim else TRAMPLED)


def throne(put):
    """The throne of ice on the top step: a seat with a white pelt on it, arms, and a tall back with a crown of points."""
    (sx0, sx1), (sz0, sz1) = THRONE["seat"]
    (bx0, bx1), (bz0, bz1) = THRONE["back"]
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            put(x, STEP_TOP, z, PACKED)
            if sx0 < x < sx1 and z < sz1:
                put(x, STEP_TOP + 1, z, PELT)
    for x in (bx0, bx1):
        for z in range(sz0, sz1 + 1):
            for y in range(STEP_TOP, STEP_TOP + 2):
                put(x, y, z, BLUE)
        put(x, STEP_TOP + 2, sz1, BLUE)
    for x in range(bx0, bx1 + 1):
        top = THRONE["back_top"] + CROWN[x - bx0]
        for z in range(bz0, bz1 + 1):
            for y in range(STEP_TOP, top + 1):
                panel = bx0 < x < bx1 and z == bz1 and STEP_TOP < y < THRONE["back_top"] - 1
                put(x, y, z, PACKED if panel else BLUE)


def tusk_points(side):
    """The centre line of one tusk (side -1 west, 1 east): (x, y, z, radius) from its root down in the step to its point."""
    cx, cy = TUSKS["centre"]
    root_x = cx + side * TUSKS["radius"]
    points = [(root_x, LAKE + 1.0 + i * 0.25, TUSKS["z"], TUSKS["root"]) for i in range(8)]
    steps = 96
    for i in range(steps + 1):
        t = i / steps
        angle = math.radians(TUSKS["sweep"] * t)
        points.append((cx + side * TUSKS["radius"] * math.cos(angle), cy + TUSKS["radius"] * math.sin(angle),
                       TUSKS["z"] + TUSKS["bow"] * math.sin(math.pi * t), TUSKS["root"] + (TUSKS["tip"] - TUSKS["root"]) * t))
    return points


def tusks(put):
    """The two mammoth tusks, ivory, rising from the lowest step either side and arching in over the throne."""
    for side in (-1, 1):
        points = tusk_points(side)
        xs, ys, zs = [p[0] for p in points], [p[1] for p in points], [p[2] for p in points]
        for x in range(int(min(xs)) - 3, int(max(xs)) + 4):
            for y in range(LAKE + 1, int(max(ys)) + 4):
                for z in range(int(min(zs)) - 3, int(max(zs)) + 4):
                    for px, py, pz, r in points:
                        if (x + 0.5 - px) ** 2 + (y + 0.5 - py) ** 2 + (z + 0.5 - pz) ** 2 <= r * r:
                            put(x, y, z, TUSK)
                            break


def hoard(put):
    """The frozen hoard: heaps of treasure frozen into ice on the steps beside the throne."""
    for (hx, hz), r in HOARD:
        for x in range(int(hx - r) - 1, int(hx + r) + 2):
            for z in range(int(hz - r) - 1, int(hz + r) + 2):
                d = math.hypot(x + 0.5 - hx, z + 0.5 - hz)
                if d > r:
                    continue
                base = step_height(x, z)
                for y in range(base, base + max(1, int(round(1.2 * math.sqrt(r * r - d * d))))):
                    put(x, y, z, HOARD_ICE)


def dens(put, cells, blocks):
    """The dens: old bones and white pelts on their snowy floors (the east den the west's mirror)."""
    west = [((2, 34), bone("y")), ((3, 39), bone("x")), ((4, 33), bone("z")), ((7, 40), bone("x")), ((5, 36), PELT),
            ((6, 37), PELT), ((4, 37), PELT)]
    for (x, z), state in west:
        for px in (x, SIZE[0] - 1 - x):
            y = LAKE + 1
            if (px, y, z) in cells and (px, y, z) not in blocks and (px, y - 1, z) in blocks:
                put(px, y, z, state)


def icicles(put, cells, blocks):
    """Giant icicles hanging from the vault on a loose lattice, none lower than ICICLE_CLEARANCE, none in front of a
    column or the crack; some with a ring of shorter ones round them."""
    for gx in range(6, SIZE[0] - 4, 5):
        for gz in range(4, SIZE[2] - 4, 5):
            x = gx + int(_hash(gx, gz, 51) * 3) - 1
            z = gz + int(_hash(gx, gz, 52) * 3) - 1
            if any(math.hypot(x + 0.5 - sx, z + 0.5 - sz) < 6.5 for sx, sz in COLUMNS):
                continue
            if CRACK["x"][0] - 2 <= x <= CRACK["x"][1] + 2 and abs(z - CRACK["z"]) <= 4:
                continue
            length = 2 + int(_hash(x, z, 53) * 6)
            if not hang(put, cells, blocks, x, z, length):
                continue
            if _hash(x, z, 54) < 0.4:
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    hang(put, cells, blocks, x + dx, z + dz, 2 + int(_hash(x + dx, z + dz, 55) * 2))


def hang(put, cells, blocks, x, z, length):
    """Hangs an icicle of up to `length` blocks under the vault over (x, z): its base, its middle and its tip."""
    column = [y for y in range(SIZE[1]) if (x, y, z) in cells]
    if not column:
        return False
    under = max(column)
    if (x, under + 1, z) not in blocks:
        return False  # open to the sky
    length = min(length, under - ICICLE_CLEARANCE + 1)
    if length < 2 or any((x, under - i, z) in blocks or (x, under - i, z) not in cells for i in range(length)):
        return False
    for i in range(length):
        put(x, under - i, z, icicle("base" if i == 0 else "tip" if i == length - 1 else "middle"))
    return True


def hollow(blocks):
    """Leaves out blocks nobody can see: those whose six neighbours are all whole, opaque blocks of the template."""
    opaque = {DRIFT, TRAMPLED, GLARE, TUSK, HOARD_ICE, PACKED, BLUE, SNOW}
    out = {}
    for (x, y, z), state in blocks.items():
        if state in opaque and all(blocks.get(n) in opaque for n in (
                (x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1))):
            continue
        out[(x, y, z)] = state
    return out


def template():
    return lair_layout.template(build(), SIZE)


def write(path):
    lair_layout.write(path, template())


if __name__ == "__main__":
    t = template()
    print(f"{len(t['blocks'])} blocks, {len(t['palette'])} states")
