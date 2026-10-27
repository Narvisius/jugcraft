"""The Crown of Thunder (docs/features/crown-of-thunder.md): the Storm Roc's lair, written as the structure template
data/jugcraft/structure/lair/crown_of_thunder.nbt, which lair/Lairs places into every instance of the
jugcraft:crown_of_thunder dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI
checks it is current).

The summit of a pinnacle above the clouds, under a storm that never breaks. North is low z.
- the summit, the arena (SUMMIT): a ring of weathered stone, calcite, gravel and moss 36 blocks across, rimmed by boulders
  (BOULDERS) with gaps between them, which shelter from the wind;
- the four Storm Rods (RODS): tall copper rods on stone plinths at the summit's north, east, south and west;
- the ledges (LEDGES): below the summit's east and west edges, LEDGE_DROP blocks down, which catch a player thrown off,
  and from each a flight of steps (STEPS) back up the cliff to the summit: the recovery routes;
- the nest (NEST): a crag beyond the summit's north edge, NEST_RISE blocks higher, a great nest of woven branches on it,
  where the Roc waits (WAIT);
- the terrace (TERRACE): below the summit's south edge, a few steps down from it, the arrival (ARRIVAL), and on its south
  edge, in an arch of grey stone, the Grey Mist (EXIT): the way home;
- the clouds (CLOUDS): a sea of storm cloud far below, round the pinnacle's foot, that nobody falls far enough to reach.
The light is hidden light blocks: the lairs' sky gives none.

Format: tools/lair_layout.py's (26.3's). Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/crown_of_thunder"

SIZE = (64, 44, 64)
PAVE = 30                          # the summit's top layer
STAND = PAVE + 1                   # where anyone on the summit stands
CENTRE = (32.5, 32.5)              # the summit's middle: the middle of cell (32, 32)
SUMMIT = 18.0                      # the summit's radius: cells whose middle is within it (36 across)
# The cliff under the summit: straight down CLIFF blocks from the summit's top, then the pinnacle narrows toward its foot
# by TAPER a block, but never below FOOT across.
CLIFF = 5
TAPER = 0.45
FOOT = 6.0
# The boulders round the summit's rim: one every BOULDER_STEP degrees (from north, clockwise), between BOULDER_INNER and
# SUMMIT from the middle, BOULDER_HEIGHT blocks high (one where the hash says so), with gaps between them. None where
# the terrace's steps, the ledges' steps or the nest's crag meet the rim.
BOULDER_STEP = 24
BOULDER_INNER = 16.5
BOULDER_HALF_WIDTH = 4             # degrees either side of a boulder's bearing
BOULDER_HEIGHT = 2
# The four Storm Rods: each rod's cell, on a plinth PLINTH cells square and one block high; the rod stands ROD_HEIGHT
# blocks on the plinth (base, middle and tip), ROD_RADIUS from the middle.
ROD_RADIUS = 13
RODS = {"north": (32, 19), "east": (45, 32), "south": (32, 45), "west": (19, 32)}
PLINTH = 3
ROD_HEIGHT = 3
ROD_PARTS = ("base", "middle", "tip")
# The ledges: below the summit's east and west edges, between SUMMIT and LEDGE_OUTER from the middle, within
# LEDGE_HALF_ANGLE degrees of due east or due west; their top is LEDGE_DROP below the summit's.
LEDGES = {"east": 90, "west": 270}       # bearing from north, clockwise
LEDGE_OUTER = 23.0
LEDGE_HALF_ANGLE = 34
LEDGE_DROP = 6
LEDGE_TOP = PAVE - LEDGE_DROP
# The steps from each ledge back up to the summit: a flight two cells wide (rows STEP_ROWS), one stair a block, cut
# into the summit's edge, from the ledge's level (the outermost) up to the summit's (the innermost).
STEP_ROWS = (36, 37)
STEPS = {"east": [(49 - i, LEDGE_TOP + 1 + i) for i in range(LEDGE_DROP)],
         "west": [(15 + i, LEDGE_TOP + 1 + i) for i in range(LEDGE_DROP)]}
# The nest's crag: cells within NEST_CRAG (x and z radii) of NEST_CENTRE that are not the summit's; its top NEST_RISE
# over the summit's, sloping down to the summit at its edge. The nest: woven branches on its top, a floor within
# NEST_FLOOR of the nest's middle and a rim from NEST_FLOOR to NEST_RIM, NEST_WALL high.
NEST_CENTRE = (32.5, 8.5)
NEST_CRAG = (6.5, 6.0)
NEST_RISE = 4
NEST_TOP = PAVE + NEST_RISE
NEST_FLOOR = 2.2
NEST_RIM = 3.4
NEST_WALL = 2
WAIT = (32.5, NEST_TOP + 1, 8.5, 0.0)        # x, y, z and yaw: in the nest, facing south over the summit
# The terrace: its cells (inclusive), its top TERRACE_DROP below the summit's; two rows of stairs (STAIR_ROWS, the cells
# STAIR_X) climb from it to the summit.
TERRACE = {"x": (27, 38), "z": (51, 58)}
TERRACE_DROP = 2
TERRACE_TOP = PAVE - TERRACE_DROP
STAIR_X = (30, 35)
STAIR_ROWS = {51: TERRACE_TOP + 1, 50: TERRACE_TOP + 2}
ARRIVAL = (32.5, TERRACE_TOP + 1, 54.5, 180.0)  # x, y, z and yaw: on the terrace, facing north up the steps
EXIT = {"x": (31, 33), "z": 58}               # the Grey Mist's columns and row, three blocks high from the terrace
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR (well below
# the ledges, well above the clouds).
BOUNDS = 40
FLOOR = PAVE - 12
# The clouds: between CLOUD_INNER and CLOUD_OUTER from the middle, CLOUD_DEPTH blocks deep from CLOUD_TOP down, lumpy.
CLOUD_INNER = 9.0
CLOUD_OUTER = 31.5
CLOUD_TOP = 6
CLOUD_DEPTH = 3
# The hidden lights: a lattice LIGHT_STEP apart LIGHT_HEIGHT over the summit, staggered row to row, a ring of them RIM_LIGHTS
# round its rim, and a row over the terrace and each ledge. SUMMIT_LIGHT is the least block light they leave on any cell
# where anyone stands on the summit, a ledge or the terrace (the audit floods it, and the client test reads it).
LIGHT = 15
LIGHT_STEP = 8
LIGHT_HEIGHT = 4
RIM_LIGHTS = (15.5, 20)            # their radius, and the degrees between them
LEDGE_LIGHTS = (-30, -15, 0, 15, 30)  # degrees either side of each ledge's bearing
TERRACE_LIGHTS = (53, 57)          # the rows over the terrace
SUMMIT_LIGHT = 7


# ---------------------------------------------------------------- states

FLOOR_BLOCKS = ((0.36, block("minecraft:moss_block")), (0.60, block("minecraft:stone")),
                (0.78, block("minecraft:calcite")), (0.92, block("minecraft:andesite")), (1.0, block("minecraft:gravel")))
ROCKS = (block("minecraft:stone"), block("minecraft:andesite"), block("minecraft:tuff"), block("minecraft:calcite"))
BOULDER_BLOCKS = (block("minecraft:stone"), block("minecraft:andesite"), block("minecraft:mossy_cobblestone"))
POLISHED = block("minecraft:polished_andesite")
CHISELED = block("minecraft:chiseled_stone_bricks")
BRICKS = block("minecraft:stone_bricks")
CRACKED = block("minecraft:cracked_stone_bricks")
NEST_BLOCK = block("jugcraft:roc_nest")
CLOUD = block("jugcraft:storm_cloud")
EXIT_MIST = block("jugcraft:lair_exit", axis="x")


def rod(part):
    return block("jugcraft:storm_rod", part=part, charged=False)


def stair(facing):
    return block("minecraft:stone_brick_stairs", facing=facing, half="bottom", shape="straight", waterlogged=False)


def light(level):
    return block("minecraft:light", level=level, waterlogged=False)


# ---------------------------------------------------------------- shapes

def distance(x, z):
    """How far the middle of cell (x, z) is from the summit's middle."""
    return math.hypot(x + 0.5 - CENTRE[0], z + 0.5 - CENTRE[1])


def bearing(x, z):
    """The bearing of cell (x, z)'s middle from the summit's middle, in degrees clockwise from north (low z)."""
    return math.degrees(math.atan2(x + 0.5 - CENTRE[0], -(z + 0.5 - CENTRE[1]))) % 360.0


def turn(a, b):
    """The angle between two bearings, 0 to 180."""
    return abs((a - b + 180.0) % 360.0 - 180.0)


def in_summit(x, z):
    return distance(x, z) <= SUMMIT


def in_box(box, x, z):
    return box["x"][0] <= x <= box["x"][1] and box["z"][0] <= z <= box["z"][1]


def in_ledge(x, z):
    """Which ledge cell (x, z) is on, or None."""
    d = distance(x, z)
    if not SUMMIT < d <= LEDGE_OUTER:
        return None
    for name, way in LEDGES.items():
        if turn(bearing(x, z), way) <= LEDGE_HALF_ANGLE:
            return name
    return None


def in_crag(x, z):
    if in_summit(x, z):
        return False
    dx = (x + 0.5 - NEST_CENTRE[0]) / NEST_CRAG[0]
    dz = (z + 0.5 - NEST_CENTRE[1]) / NEST_CRAG[1]
    return dx * dx + dz * dz <= 1.0


def nest_distance(x, z):
    return math.hypot(x + 0.5 - NEST_CENTRE[0], z + 0.5 - NEST_CENTRE[1])


def crag_top(x, z):
    """The crag's top at (x, z): NEST_TOP round the nest, falling to the summit's level at its edge."""
    dx = (x + 0.5 - NEST_CENTRE[0]) / NEST_CRAG[0]
    dz = (z + 0.5 - NEST_CENTRE[1]) / NEST_CRAG[1]
    edge = math.sqrt(dx * dx + dz * dz)
    if nest_distance(x, z) <= NEST_RIM + 0.5:
        return NEST_TOP
    return PAVE + max(0, round((1.0 - edge) / 0.45 * NEST_RISE))


def step_cells():
    """The ledges' steps: {(x, z): (stair's y, facing)}, every row of each flight."""
    out = {}
    for name, flight in STEPS.items():
        facing = "west" if name == "east" else "east"
        for x, y in flight:
            for z in STEP_ROWS:
                out[(x, z)] = (y, facing)
    return out


def stair_cells():
    """The terrace's stairs up to the summit: {(x, z): stair's y}."""
    return {(x, z): y for z, y in STAIR_ROWS.items() for x in range(STAIR_X[0], STAIR_X[1] + 1)}


def plinth_cells(name):
    cx, cz = RODS[name]
    half = PLINTH // 2
    return [(x, z) for x in range(cx - half, cx + half + 1) for z in range(cz - half, cz + half + 1)]


def rod_cells():
    """Each Storm Rod's blocks: {name: [(x, y, z, part)]}, base to tip."""
    return {name: [(x, STAND + 1 + i, z, part) for i, part in enumerate(ROD_PARTS)] for name, (x, z) in RODS.items()}


def boulder_bearings():
    """The bearings the boulders stand at: every BOULDER_STEP degrees, but not where the terrace's steps (south), the
    ledges' steps or the nest's crag (north) meet the rim, nor over a rod."""
    kept = []
    for b in range(0, 360, BOULDER_STEP):
        if turn(b, 0) < 20 or turn(b, 180) < 22 or turn(b, 90) <= 18 or turn(b, 270) <= 18 \
                or turn(b, 103) < 12 or turn(b, 257) < 12:
            continue
        kept.append(b)
    return kept


def in_boulder(x, z):
    d = distance(x, z)
    if not BOULDER_INNER < d <= SUMMIT:
        return False
    return any(turn(bearing(x, z), b) <= BOULDER_HALF_WIDTH for b in boulder_bearings())


def exit_cells():
    """Where the Grey Mist is (for the audit and the tests)."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(TERRACE_TOP + 1, TERRACE_TOP + 4) for x in range(x0, x1 + 1)]


def patches(x, z, scale, salt):
    """Smooth 0..1 noise: hashed corners SCALE cells apart, blended, so the floor lies in patches, not speckles."""
    gx, gz = x / scale, z / scale
    x0, z0 = math.floor(gx), math.floor(gz)
    fx, fz = gx - x0, gz - z0
    fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    a, b = _hash(x0, z0, salt), _hash(x0 + 1, z0, salt)
    c, d = _hash(x0, z0 + 1, salt), _hash(x0 + 1, z0 + 1, salt)
    return (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz


def floor_block(x, z):
    h = 0.7 * patches(x, z, 5, 61) + 0.3 * _hash(x, z, 62)
    for limit, state in FLOOR_BLOCKS:
        if h < limit:
            return state
    return FLOOR_BLOCKS[-1][1]


def rock(x, y, z):
    """The pinnacle's rock: bands of stone, andesite, tuff and calcite by height, broken up."""
    band = (y // 3 + (1 if _hash(x, z, y // 3, 67) < 0.3 else 0)) % len(ROCKS)
    return ROCKS[band]


def pinnacle_radius(y):
    """The pinnacle's radius at height y: the summit's down the cliff, then narrowing toward its foot."""
    if y >= PAVE - CLIFF:
        return SUMMIT + 0.5
    return max(FOOT, SUMMIT + 0.5 - (PAVE - CLIFF - y) * TAPER)


# ---------------------------------------------------------------- the build

def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    def clear(x, y, z):
        blocks.pop((x, y, z), None)

    pinnacle(put)
    summit(put)
    ledges(put)
    steps(put, clear)
    crag_and_nest(put)
    terrace(put)
    plinths_and_rods(put)
    boulders(put)
    exit_mist(put)
    clouds(put, blocks)
    lights(put, blocks)
    return hollow(blocks)


def pinnacle(put):
    """The pinnacle under the summit: rock from its foot up to the summit's underside."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            for y in range(PAVE):
                if y > LEDGE_TOP and in_ledge(x, z):
                    continue                    # the cliff over a ledge is sheer: nothing juts out over it
                wobble = (_hash(x, z, y // 2, 71) - 0.5) * 1.6
                if distance(x, z) <= pinnacle_radius(y) + wobble:
                    put(x, y, z, rock(x, y, z))


def summit(put):
    """The summit's floor: weathered stone, calcite, gravel and moss, in patches."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if in_summit(x, z):
                put(x, PAVE, z, floor_block(x, z))


def ledges(put):
    """The ledges: rock shelves LEDGE_DROP below the summit, a few blocks deep."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if in_ledge(x, z):
                depth = 3 + round(_hash(x, z, 73) * 2)
                for y in range(LEDGE_TOP - depth, LEDGE_TOP):
                    put(x, y, z, rock(x, y, z))
                put(x, LEDGE_TOP, z, floor_block(x, z))


def steps(put, clear):
    """The flights from the ledges up to the summit: stairs cut into the summit's edge, the cut open above them."""
    for (x, z), (y, facing) in step_cells().items():
        for above in range(y + 1, STAND + 1):
            clear(x, above, z)
        for below in range(LEDGE_TOP - 3, y):
            put(x, below, z, rock(x, below, z))
        put(x, y, z, stair(facing))


def crag_and_nest(put):
    """The nest's crag beyond the summit's north edge, and the nest of woven branches on its top."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if not in_crag(x, z):
                continue
            top = crag_top(x, z)
            dx = (x + 0.5 - NEST_CENTRE[0]) / NEST_CRAG[0]
            dz = (z + 0.5 - NEST_CENTRE[1]) / NEST_CRAG[1]
            bottom = PAVE - 2 - round((1.0 - math.sqrt(dx * dx + dz * dz)) * 12) - round(_hash(x, z, 107) * 2)
            for y in range(bottom, top + 1):
                put(x, y, z, rock(x, y, z))
            if top > PAVE and nest_distance(x, z) > NEST_RIM + 0.5:
                put(x, top, z, floor_block(x, z))
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            d = nest_distance(x, z)
            if d <= NEST_FLOOR:
                put(x, NEST_TOP, z, NEST_BLOCK)
            elif d <= NEST_RIM:
                for y in range(NEST_TOP, NEST_TOP + NEST_WALL + 1):
                    put(x, y, z, NEST_BLOCK)


def terrace(put):
    """The terrace below the summit's south edge, paved, its stairs up to the summit."""
    for x in range(TERRACE["x"][0], TERRACE["x"][1] + 1):
        for z in range(TERRACE["z"][0], TERRACE["z"][1] + 1):
            if in_summit(x, z):
                continue
            depth = 6 + round(_hash(x, z, 79) * 3)
            for y in range(TERRACE_TOP - depth, TERRACE_TOP):
                put(x, y, z, rock(x, y, z))
            put(x, TERRACE_TOP, z, CRACKED if _hash(x, z, 83) < 0.25 else BRICKS)
    for (x, z), y in stair_cells().items():
        for below in range(TERRACE_TOP - 2, y):
            put(x, below, z, BRICKS)
        put(x, y, z, stair("north"))


def plinths_and_rods(put):
    """The Storm Rods on their plinths: polished andesite round a chiseled middle, the rod on it."""
    for name in RODS:
        cx, cz = RODS[name]
        for x, z in plinth_cells(name):
            put(x, STAND, z, CHISELED if (x, z) == (cx, cz) else POLISHED)
    for name, cells in rod_cells().items():
        for x, y, z, part in cells:
            put(x, y, z, rod(part))


def boulders(put):
    """The boulders on the rim, a block or two high."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if not in_boulder(x, z):
                continue
            height = BOULDER_HEIGHT - (1 if _hash(x, z, 89) < 0.35 else 0)
            for y in range(STAND, STAND + height):
                put(x, y, z, BOULDER_BLOCKS[int(_hash(x, y, z, 97) * len(BOULDER_BLOCKS))])


def exit_mist(put):
    """The Grey Mist in an arch of grey stone on the terrace's south edge: the way home."""
    for x, y, z in exit_cells():
        put(x, y, z, EXIT_MIST)
    (x0, x1), z = EXIT["x"], EXIT["z"]
    for x in range(x0 - 1, x1 + 2):
        put(x, TERRACE_TOP + 4, z, CHISELED if x in (x0 - 1, x1 + 1) else BRICKS)
    for y in range(TERRACE_TOP + 1, TERRACE_TOP + 4):
        put(x0 - 1, y, z, BRICKS)
        put(x1 + 1, y, z, BRICKS)


def clouds(put, blocks):
    """The sea of storm cloud round the pinnacle's foot: lumpy, thinning toward its edge, never in its rock."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            d = distance(x, z)
            if not CLOUD_INNER <= d <= CLOUD_OUTER:
                continue
            thin = (d - CLOUD_INNER) / (CLOUD_OUTER - CLOUD_INNER)
            depth = CLOUD_DEPTH - (1 if _hash(x, z, 101) < 0.4 + thin * 0.4 else 0) - (1 if thin > 0.85 else 0)
            lift = 1 if _hash(x // 3, z // 3, 103) < 0.3 else 0
            for y in range(CLOUD_TOP - CLOUD_DEPTH + 1 + lift, CLOUD_TOP - CLOUD_DEPTH + 1 + lift + depth):
                if (x, y, z) not in blocks:
                    put(x, y, z, CLOUD)


def lights(put, blocks):
    """The hidden lights: a staggered lattice over the summit, a ring round its rim, and a row over the terrace and each
    ledge."""
    y = STAND + LIGHT_HEIGHT
    step = LIGHT_STEP
    for row, z in enumerate(range(int(CENTRE[1]) - 16, int(CENTRE[1]) + 17, step // 2)):
        for x in range(int(CENTRE[0]) - 16 + (step // 2 if row % 2 else 0), int(CENTRE[0]) + 17, step):
            if in_summit(x, z) and (x, y, z) not in blocks:
                put(x, y, z, light(LIGHT))
    radius, every = RIM_LIGHTS
    for b in range(0, 360, every):
        x = int(CENTRE[0] + math.sin(math.radians(b)) * radius)
        z = int(CENTRE[1] - math.cos(math.radians(b)) * radius)
        if (x, STAND + 2, z) not in blocks:
            put(x, STAND + 2, z, light(LIGHT))
    for z in TERRACE_LIGHTS:
        for x in range(TERRACE["x"][0] + 2, TERRACE["x"][1] + 1, 4):
            if (x, TERRACE_TOP + 4, z) not in blocks:
                put(x, TERRACE_TOP + 4, z, light(LIGHT))
    for name, way in LEDGES.items():
        for spread in LEDGE_LIGHTS:
            angle = math.radians(way + spread)
            x = int(CENTRE[0] + math.sin(angle) * 20.5)
            z = int(CENTRE[1] - math.cos(angle) * 20.5)
            if (x, LEDGE_TOP + 4, z) not in blocks:
                put(x, LEDGE_TOP + 4, z, light(LIGHT))


def hollow(blocks):
    """Leaves out blocks nobody can see: rock whose six neighbours are all rock."""
    opaque = {*ROCKS, *(state for _, state in FLOOR_BLOCKS if state != FLOOR_BLOCKS[-1][1]), BRICKS, CRACKED, POLISHED,
              CHISELED, *BOULDER_BLOCKS}
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
