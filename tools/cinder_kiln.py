"""The Cinder Kiln (docs/features/cinder-kiln.md): the Cinder Tyrant's lair, written as the structure template
data/jugcraft/structure/lair/cinder_kiln.nbt, which lair/Lairs places into every instance of the jugcraft:cinder_kiln
dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI checks it is current).

A kiln hollowed out of a volcano's roots: 72 blocks across (x), 46 high (y) and 80 long (z, from the north wall at 0 to
the south wall at 79). The kiln (cavern()) is a dome standing on its floor, its wall rippled by a smooth noise: basalt
below WALL_TOP, the bowl, and kiln brick above, the dome; only the stone touching the open air is kept. Its crown is
open to the sky, the vent where the smoke rises. Angles below are measured clockwise from north, round BOWL_CENTRE.
- the bowl, the arena: BOWL_RADIUS across its centre, floored with cracked basalt; round it, to the wall, a ring of
  rough basalt;
- the forge mouth (FORGE): an arched recess in the north wall. A lip of kiln brick runs across its front, LIP high, and
  behind the lip a pool of molten slag fills the forge. In the middle the slag runs over the lip and falls into the heat
  channel (CHANNEL), a trench of slag across the bowl's floor to the crucible (CRUCIBLE): a round pool of slag in the
  bowl's north third, CRUCIBLE["depth"] deep, rimmed with kiln brick but where the channel runs in. The Tyrant waits
  sunk in it (part 2);
- the sluices (SLUICES): three great iron sluice gates in the west, east and south walls, each in a recess, its wheel
  on its right as you face it. A paved quench trough of trough stone runs from under each gate toward the bowl's
  middle, flooded while its sluice is open;
- the shelves (SHELVES): four raised shelves of smooth basalt round the bowl (north-west, north-east, south-west and
  south-east), up a step, where the slag never reaches;
- the arrival (ARRIVAL): a ledge high in the south wall (LEDGE_ARC), LEDGE blocks over the bowl, under an alcove at the
  mouth of a lava tube long since cooled. Grey Mist hangs in an arch of kiln brick across the tube (EXIT): the way home.
  From the ledge a stair of blackstone (STAIR) curves down the south-east wall to the bowl, half a block lower every
  STAIR["step"] degrees, so no neighbouring blocks differ by more than half a block and it is walked both ways without
  jumping.
The light is hidden light blocks, and the slag's own glow.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written (the lair-only blocks' are written with them); vanilla blocks use only properties they have had for
years. Nothing in it burns: no wood, wool or plants. Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/cinder_kiln"

SIZE = (72, 46, 80)
BOWL = 5                           # the floor's layer: players stand on it at BOWL + 1
BOWL_CENTRE = (35.5, 37.5)
BOWL_RADIUS = 22.0                 # the arena of cracked basalt: 44 across
# The kiln: half an ellipsoid standing on the floor, (x, y of the floor's blocks, z) and radii (x, y, z); its wall is
# basalt up to WALL_TOP, a band of blackstone bricks there, and kiln brick above. The vent at its crown is open to the
# sky.
DOME = ((35.5, BOWL, 37.5), (27.5, 31.0, 27.5))
WALL_TOP = BOWL + 9
RIBS = 8                           # ribs of blackstone bricks up the dome, evenly round it from north
RIB_HALF = 0.75                    # each rib's half-width (blocks, along the dome)
VENT_RADIUS = 3.5
WOBBLE = 0.22                      # how far the noise pushes the wall in and out (of the ellipsoid's radius squared)
# The forge mouth in the north wall: its columns (x), the arch's crown, and its rows (z) from the back wall: the forge's
# pool of slag, then the lip of kiln brick, whose front stands out into the kiln. The lip's top is LIP; the slag runs
# over it in the middle (RUN, columns) and falls into the channel in the row in front of it.
FORGE = {"x": (30, 40), "back": 2, "pool": (3, 7), "lip": (8, 13), "crown": BOWL + 13}
LIP = BOWL + 3                     # the lip's top: stood on at LIP + 1
RUN = (34, 36)                     # the columns of the slag run, the slag fall and the heat channel
# The crucible: its centre (x, z), the pool's radius and depth (blocks of slag), and its rim of kiln brick, a block high.
CRUCIBLE = {"centre": (35.5, 25.5), "radius": 4.5, "depth": 3, "rim": 1.0}
# The heat channel: the rows (z) from the slag fall to the crucible, in the RUN columns.
CHANNEL = (FORGE["lip"][1] + 1, 21)
# The sluices: the plane of the gate (its column for a west or east gate, its row for a south one), the middle of its
# trough across (z, or x), which way the trough runs into the kiln, and the trough's last block; the gate faces into the
# kiln. Each gate is GATE wide (its posts either side of its panels) and GATE_HEIGHT high under a lintel; its wheel stands
# in the recess in front of its right-hand post, at WHEEL_HEIGHT.
SLUICES = {
    "west": {"axis": "x", "plane": 6, "across": 37, "toward": 1, "end": 26, "facing": "east"},
    "east": {"axis": "x", "plane": 64, "across": 37, "toward": -1, "end": 44, "facing": "west"},
    "south": {"axis": "z", "plane": 66, "across": 35, "toward": -1, "end": 46, "facing": "north"},
}
TROUGH_HALF = 1                    # each trough is three blocks wide
GATE_HEIGHT = 4                    # the panels and posts; the lintel is above them
WHEEL_HEIGHT = BOWL + 2
RECESS = 3                         # how deep each gate's recess is cut, in front of the gate
# The shelves: (x, z) of each one's middle, and its half-size: a rounded square a block high.
SHELVES = ((24.5, 26.5), (46.5, 26.5), (24.5, 48.5), (46.5, 48.5))
SHELF_HALF = 3.2
LEDGE = 7                          # the arrival ledge's height over the bowl
ARRIVAL = (41.5, BOWL + 1 + LEDGE, 63.5, 165.0)     # x, y, z and yaw: on the ledge, facing the crucible
# The ledge: between these angles (degrees clockwise from north) and radii; the stair: between its radii, from the ledge
# at "top" degrees down toward the east, half a block lower every "step" degrees, to the bowl's floor.
LEDGE_ARC = {"angles": (160.0, 172.0), "r": (24.0, 30.5)}
STAIR = {"r": (24.0, 28.5), "top": 160.0, "step": 3.5}
# The alcove over the ledge (half an ellipsoid on the ledge's top) and the lava tube's mouth behind it, closed by the
# Grey Mist in its arch of kiln brick: the mist's columns (x) and row (z), three blocks high.
ALCOVE = ((41.5, BOWL + LEDGE, 65.0), (6.0, 6.5, 5.0))
EXIT = {"x": (41, 42), "z": 68}
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
CENTRE = (35.5, 38.5)
BOUNDS = 40
FLOOR = BOWL - 4
# The hidden lights: a lattice through the kiln at two heights (y, level, spacing), and these: (x, y, z, level).
LIGHT_LAYERS = ((BOWL + 5, 11, 8), (BOWL + 16, 12, 10))
LIGHTS = [
    (35, BOWL + 3, 37, 13), (35, BOWL + 20, 37, 14),
    (41, ARRIVAL[1] + 2, 63, 13), (41, ARRIVAL[1] + 3, 66, 12),
    (8, BOWL + 3, 37, 11), (62, BOWL + 3, 37, 11), (35, BOWL + 3, 64, 11),
    (35, LIP + 4, 6, 12),
]

# ---------------------------------------------------------------- states

KILN_BRICK = block("jugcraft:kiln_brick")
CRACKED = block("jugcraft:cracked_basalt")
SLAG = block("jugcraft:molten_slag")
EXIT_MIST = block("jugcraft:lair_exit", axis="x")
BASALT = block("minecraft:basalt", axis="y")
BLACKSTONE = block("minecraft:blackstone")
MAGMA = block("minecraft:magma_block")
SMOOTH = block("minecraft:smooth_basalt")
POLISHED = block("minecraft:polished_basalt", axis="y")
STEP = block("minecraft:polished_blackstone")
STEP_HALF = block("minecraft:polished_blackstone_slab", type="bottom", waterlogged=False)
STEP_SIDE = block("minecraft:polished_blackstone_bricks")


def trough(flooded=False):
    return block("jugcraft:trough_stone", flooded=flooded)


def sluice(part, facing, flow="ready"):
    return block("jugcraft:sluice_gate", part=part, facing=facing, flow=flow)


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
    """Whether cell (x, y, z) is open under one of the kiln's half-ellipsoids, its surface rippled by the noise."""
    (cx, floor, cz), (rx, ry, rz) = shape
    if y <= floor:
        return False
    e = ((x + 0.5 - cx) / rx) ** 2 + ((y - floor - 0.5) / ry) ** 2 + ((z + 0.5 - cz) / rz) ** 2
    if e < 1.0 - WOBBLE / 2:
        return True
    if e > 1.0 + WOBBLE / 2:
        return False
    return e < 1.0 + WOBBLE * (noise(x, y, z, 7.0, salt) - 0.5)


def polar(x, z):
    """The radius and the angle (degrees clockwise from north) of cell (x, z) round the bowl's centre."""
    dx, dz = x + 0.5 - BOWL_CENTRE[0], z + 0.5 - BOWL_CENTRE[1]
    return math.hypot(dx, dz), math.degrees(math.atan2(dx, -dz)) % 360.0


def stair_height(x, z):
    """Where cell (x, z) of the ledge or the stair is stood on (in half blocks), or None off them."""
    r, angle = polar(x, z)
    (a0, a1), (l0, l1) = LEDGE_ARC["angles"], LEDGE_ARC["r"]
    if a0 <= angle <= a1 and l0 <= r <= l1:
        return float(ARRIVAL[1])
    s0, s1 = STAIR["r"]
    if not s0 <= r <= s1 or angle > a0:
        return None
    level = math.ceil((STAIR["top"] - angle) / STAIR["step"])
    height = ARRIVAL[1] - 0.5 * level
    return height if height > BOWL + 1 else None


def forge_arch(x, y):
    """Whether (x, y) is inside the forge mouth's arch, seen from the front: full width below the lip's top, and above
    it a half-ellipse up to the crown."""
    x0, x1 = FORGE["x"]
    half = (x1 - x0 + 1) / 2.0
    middle = (x0 + x1 + 1) / 2.0
    if not x0 <= x <= x1:
        return False
    if y <= LIP:
        return True
    return ((x + 0.5 - middle) / half) ** 2 + ((y - LIP) / (FORGE["crown"] - LIP)) ** 2 <= 1.0


def recess(name):
    """The open cells cut in front of a sluice's gate: GATE wide, from the floor to the lintel, RECESS deep."""
    s = SLUICES[name]
    cells = set()
    for depth in range(1, RECESS + 1):
        along = s["plane"] + s["toward"] * depth
        for across in range(s["across"] - TROUGH_HALF - 1, s["across"] + TROUGH_HALF + 2):
            for y in range(BOWL + 1, BOWL + GATE_HEIGHT + 2):
                cells.add(at(s, along, across, y))
    return cells


def at(s, along, across, y):
    """The cell `along` the sluice's trough and `across` it, at height y."""
    return (along, y, across) if s["axis"] == "x" else (across, y, along)


def cavern():
    """The open cells of the kiln: the dome, the forge mouth, the sluices' recesses, the alcove over the ledge and the
    lava tube's mouth, headroom over the stair, and the vent at the crown, open to the sky. Kept a block inside the
    template, so its stone fits round it (but for the vent's top)."""
    w, h, length = SIZE
    cells = set()
    (cx, floor, cz), (rx, ry, rz) = DOME
    reach = 1.0 + WOBBLE
    for x in range(1, w - 1):
        for z in range(1, length - 1):
            for y in range(floor + 1, min(h - 1, int(floor + ry * reach) + 2)):
                if dome(DOME, x, y, z, 11):
                    cells.add((x, y, z))
    (ax, afloor, az), (arx, ary, arz) = ALCOVE
    for x in range(int(ax - arx) - 2, int(ax + arx) + 3):
        for z in range(int(az - arz) - 2, int(az + arz) + 3):
            for y in range(afloor + 1, int(afloor + ary) + 3):
                if dome(ALCOVE, x, y, z, 12):
                    cells.add((x, y, z))
    # The tube's mouth behind the alcove, up to the mist.
    for x in range(EXIT["x"][0] - 1, EXIT["x"][1] + 2):
        for z in range(int(az), EXIT["z"] + 1):
            for y in range(ARRIVAL[1], ARRIVAL[1] + 4):
                cells.add((x, y, z))
    for x in range(FORGE["x"][0], FORGE["x"][1] + 1):
        for z in range(FORGE["back"] + 1, FORGE["lip"][1] + 1):
            for y in range(LIP + 1, FORGE["crown"] + 1):
                if forge_arch(x, y):
                    cells.add((x, y, z))
    for name in SLUICES:
        cells |= recess(name)
    # Headroom over the ledge and the stair, where they climb into the wall: four blocks over their surface.
    for x in range(w):
        for z in range(length):
            surface = stair_height(x, z)
            if surface is not None:
                for y in range(int(surface), int(surface) + 4):
                    cells.add((x, y, z))
    top = max(y for (x, y, z) in cells if math.hypot(x + 0.5 - cx, z + 0.5 - cz) <= VENT_RADIUS)
    for x in range(int(cx - VENT_RADIUS) - 1, int(cx + VENT_RADIUS) + 2):
        for z in range(int(cz - VENT_RADIUS) - 1, int(cz + VENT_RADIUS) + 2):
            if math.hypot(x + 0.5 - cx, z + 0.5 - cz) <= VENT_RADIUS:
                for y in range(top, h):
                    cells.add((x, y, z))
    return cells


def in_bowl(x, z):
    return polar(x, z)[0] <= BOWL_RADIUS


def crucible_distance(x, z):
    return math.hypot(x + 0.5 - CRUCIBLE["centre"][0], z + 0.5 - CRUCIBLE["centre"][1])


def in_crucible(x, z):
    return crucible_distance(x, z) <= CRUCIBLE["radius"]


def on_rim(x, z):
    """The crucible's rim of kiln brick, open where the channel runs in."""
    d = crucible_distance(x, z)
    if not CRUCIBLE["radius"] < d <= CRUCIBLE["radius"] + CRUCIBLE["rim"]:
        return False
    return not (RUN[0] <= x <= RUN[1] and z + 0.5 < CRUCIBLE["centre"][1])


def in_channel(x, z):
    return RUN[0] <= x <= RUN[1] and CHANNEL[0] <= z <= CHANNEL[1]


def in_trough(x, z):
    """The name of the sluice whose trough covers (x, z), or None."""
    for name, s in SLUICES.items():
        along, across = (x, z) if s["axis"] == "x" else (z, x)
        lo, hi = sorted((s["plane"], s["end"]))
        if lo <= along <= hi and abs(across - s["across"]) <= TROUGH_HALF:
            return name
    return None


def shelf(x, z):
    """Whether (x, z) is on a shelf: a rounded square round its middle."""
    for sx, sz in SHELVES:
        dx, dz = (x + 0.5 - sx) / SHELF_HALF, (z + 0.5 - sz) / SHELF_HALF
        if dx ** 4 + dz ** 4 <= 1.0:
            return True
    return False


def wall(x, y, z):
    """The kiln's stone: basalt in the bowl's wall, veined with magma and patched with blackstone; above WALL_TOP, a band
    of blackstone bricks and then the dome's kiln brick, with RIBS of blackstone bricks running up it to the vent."""
    if y == WALL_TOP:
        return STEP_SIDE
    if y > WALL_TOP:
        r, angle = polar(x, z)
        nearest = round(angle / (360.0 / RIBS)) * (360.0 / RIBS)
        if r > 0.0 and abs(angle - nearest) * math.pi / 180.0 * r <= RIB_HALF:
            return STEP_SIDE
        return KILN_BRICK
    if noise(x, y, z, 3.0, 21) > 0.78:
        return MAGMA
    return BLACKSTONE if noise(x, y, z, 5.0, 22) > 0.62 else BASALT


def ground(x, y, z):
    """The floor under the open cell above (x, y, z): the bowl's cracked basalt, the ring of rough basalt to the wall;
    elsewhere (the alcove, the recesses) the wall's stone, but never magma, which would burn whoever stood on it."""
    if y == BOWL:
        return CRACKED if in_bowl(x, z) else BASALT
    stone = wall(x, y, z)
    return BASALT if stone == MAGMA else stone


# ---------------------------------------------------------------- building

def build():
    cells = cavern()
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    walls(put, cells)
    forge(put)
    crucible(put)
    channel(put)
    sluices(put)
    shelves(put, cells)
    stair(put, cells, blocks)
    exit_arch(put)
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
    """The stone round the open cells (beside, above, below or across a corner): floors under them, walls and dome."""
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
                    put(nx, ny, nz, ground(nx, ny, nz) if (nx, ny + 1, nz) in cells else wall(nx, ny, nz))


def forge(put):
    """The forge mouth: its back wall glowing with magma, the forge's pool of slag on kiln brick, the lip of kiln brick
    across its front, the slag running over the lip's middle and falling into the channel."""
    (x0, x1), back = FORGE["x"], FORGE["back"]
    p0, p1 = FORGE["pool"]
    l0, l1 = FORGE["lip"]
    for x in range(x0 - 1, x1 + 2):
        for y in range(BOWL, FORGE["crown"] + 2):
            if forge_arch(x, y) or forge_arch(x, y - 1) or forge_arch(x - 1, y) or forge_arch(x + 1, y):
                put(x, y, back, MAGMA if LIP < y <= LIP + 2 and (x + y) % 3 else KILN_BRICK)
    for x in range(x0, x1 + 1):
        for z in range(p0, l1 + 1):
            for y in range(BOWL, LIP):
                put(x, y, z, KILN_BRICK)
            pool = p0 <= z <= p1 and x0 < x < x1
            run = RUN[0] <= x <= RUN[1]
            put(x, LIP, z, SLAG if pool or run else KILN_BRICK)
        # The arch's sides, kiln brick, standing on the lip.
        for y in range(LIP + 1, FORGE["crown"] + 2):
            for z in range(back, l0):
                if not forge_arch(x, y) and (forge_arch(x - 1, y) or forge_arch(x + 1, y) or forge_arch(x, y - 1)):
                    put(x, y, z, KILN_BRICK)
    for x in range(RUN[0], RUN[1] + 1):
        for y in range(BOWL + 1, LIP + 1):
            put(x, y, l1 + 1, SLAG)
    # The lip's ends: kiln brick down the sides of its front, from the bowl's floor up.
    for z in range(l0, l1 + 1):
        for x in (x0 - 1, x1 + 1):
            for y in range(BOWL + 1, LIP + 1):
                put(x, y, z, KILN_BRICK)


def crucible(put):
    """The crucible: a round pool of slag CRUCIBLE["depth"] deep, on basalt, and its rim of kiln brick."""
    (cx, cz), r = CRUCIBLE["centre"], CRUCIBLE["radius"] + CRUCIBLE["rim"]
    for x in range(int(cx - r) - 2, int(cx + r) + 3):
        for z in range(int(cz - r) - 2, int(cz + r) + 3):
            if in_crucible(x, z):
                for y in range(BOWL - CRUCIBLE["depth"] + 1, BOWL + 1):
                    put(x, y, z, SLAG)
                put(x, BOWL - CRUCIBLE["depth"], z, BASALT)
            elif crucible_distance(x, z) <= r + 1.0:
                for y in range(BOWL - CRUCIBLE["depth"], BOWL):
                    put(x, y, z, BASALT)
            if on_rim(x, z):
                put(x, BOWL + 1, z, KILN_BRICK)


def channel(put):
    """The heat channel: a trench of slag across the floor from the slag fall to the crucible, on basalt."""
    for x in range(RUN[0], RUN[1] + 1):
        for z in range(CHANNEL[0], CHANNEL[1] + 1):
            if not in_crucible(x, z):
                put(x, BOWL, z, SLAG)
                put(x, BOWL - 1, z, BASALT)


def sluice_blocks():
    """Every sluice's gate: {(x, y, z): (sluice, part)}, its posts and lintel the frame, its panels, and its wheel."""
    out = {}
    for name, s in SLUICES.items():
        a = s["across"]
        for y in range(BOWL + 1, BOWL + GATE_HEIGHT + 1):
            for across in range(a - TROUGH_HALF - 1, a + TROUGH_HALF + 2):
                post = abs(across - a) > TROUGH_HALF
                out[at(s, s["plane"], across, y)] = (name, "frame" if post else "panel")
        for across in range(a - TROUGH_HALF - 1, a + TROUGH_HALF + 2):
            out[at(s, s["plane"], across, BOWL + GATE_HEIGHT + 1)] = (name, "frame")
        out[at(s, s["plane"] + s["toward"], right_post(s), WHEEL_HEIGHT)] = (name, "wheel")
    return out


def right_post(s):
    """The post on the right of a gate, as you face it from the kiln."""
    # Facing east (the west gate) you look west: your right is north (z - 1). Facing west, south (z + 1). Facing north
    # (the south gate) you look south: your right is west (x - 1).
    side = {"east": -1, "west": 1, "north": -1}[s["facing"]]
    return s["across"] + side * (TROUGH_HALF + 1)


def trough_cells():
    """Every trough's cells: {(x, z): sluice}."""
    out = {}
    for name, s in SLUICES.items():
        lo, hi = sorted((s["plane"], s["end"]))
        for along in range(lo, hi + 1):
            for across in range(s["across"] - TROUGH_HALF, s["across"] + TROUGH_HALF + 1):
                x, _, z = at(s, along, across, 0)
                out[(x, z)] = name
    return out


def sluices(put):
    """The sluice gates in their recesses, and their troughs from under each gate toward the bowl's middle."""
    for (x, y, z), (name, part) in sluice_blocks().items():
        put(x, y, z, sluice(part, SLUICES[name]["facing"]))
    for (x, z) in trough_cells():
        put(x, BOWL, z, trough())
        put(x, BOWL - 1, z, BASALT)


def shelves(put, cells):
    """The four shelves: a step of smooth basalt, rimmed with polished basalt."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if shelf(x, z) and (x, BOWL + 1, z) in cells:
                rim = any(not shelf(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                put(x, BOWL + 1, z, POLISHED if rim else SMOOTH)


def stair(put, cells, blocks):
    """The ledge and the stair: polished blackstone on top, a whole block or a half one, on a mass of blackstone bricks
    from the floor. Only where the kiln is open, or over its stone."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            h = stair_height(x, z)
            if h is None:
                continue
            whole = int(math.floor(h))
            for y in range(BOWL + 1, whole - (0 if h > whole else 1)):
                put(x, y, z, STEP_SIDE)
            if h > whole:
                put(x, whole, z, STEP_HALF)   # a half block on the bricks below
            else:
                put(x, whole - 1, z, STEP)


def exit_arch(put):
    """The arch of kiln brick across the lava tube's mouth, behind the ledge, the Grey Mist hanging in it."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    y0 = ARRIVAL[1]
    for y in range(y0, y0 + 3):
        for x in range(x0, x1 + 1):
            put(x, y, z, EXIT_MIST)
            put(x, y, z + 1, BASALT)
        put(x0 - 1, y, z, KILN_BRICK)
        put(x1 + 1, y, z, KILN_BRICK)
    for x in range(x0 - 1, x1 + 2):
        put(x, y0 + 3, z, KILN_BRICK)
    for x in range(x0, x1 + 1):
        put(x, y0 + 4, z, KILN_BRICK)


def exit_mist():
    """Where the Grey Mist in the arch is (for the audit and the tests)."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(ARRIVAL[1], ARRIVAL[1] + 3) for x in range(x0, x1 + 1)]


def hollow(blocks):
    """Leaves out blocks nobody can see: those whose six neighbours are all whole, opaque blocks of the template. The
    slag is never left out: the crucible keeps its depth."""
    opaque = {KILN_BRICK, CRACKED, BASALT, BLACKSTONE, MAGMA, SMOOTH, POLISHED, STEP, STEP_SIDE}
    out = {}
    for (x, y, z), state in blocks.items():
        if state in opaque and all(blocks.get(n) in opaque or blocks.get(n) == SLAG for n in (
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
