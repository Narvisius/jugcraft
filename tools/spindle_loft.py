"""The Spindle Loft (docs/features/spindle-loft.md): Madame Tatterlace's lair, written as the structure template
data/jugcraft/structure/lair/spindle_loft.nbt, which lair/Lairs places into every instance of the jugcraft:spindle_loft
dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI checks it is current).

The attic of a colossal sewing room, seen at a spider's size: 81 blocks across (x), 64 high (y) and 100 long (z, from
the north gable at 0 to the south gable at 99), under a steep roof whose two slopes meet at a ridge along the middle.
Nothing stands on a floor: the floorboards are far below in the dark, and the mist throws back anyone who falls before
they reach them (below FLOOR). From south to north:
- the pincushion (ARRIVAL): a giant red tomato pincushion, seamed, capped with a green felt leaf and stuck with pins
  taller than a house, on the top of a spool of white thread that rises out of the dark. Players wake on its leaf.
  Beside them a needle pushed deep into the cushion holds Grey Mist in its eye, the way home (EXIT);
- the measuring tape (TAPE): a tailor's tape stretched from the cushion's top down to the doily's edge, the bridge;
- the doily (the arena): a circle of lace DOILY_RADIUS across its centre, lying at LACE between four giant thread
  spools (SPOOLS) in green, blue, beige and red, whose barrels pierce its rim. Its rings are lace of four patterns, and
  the ring round each spool is the dense band. A thimble lies on its side across the doily's east edge, open to the
  arena, and the open blades of a pair of shears rise out of the dark beyond its north edge;
- overhead: taut threads from the spools' tops up to the rafters and across the arena, and a grimy skylight in the east
  slope. The light is hidden light blocks: brightest on the doily, dimmer up the tape and on the cushion.
The Spider's Larder hangs about it: cocoons under the spools' flanges and the rafters, egg sacs on the spools, silk spool
stacks on their tops, and cobwebs in the roof.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written (the lair-only blocks' are written with them); vanilla blocks use only properties they have had for
years. Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash
from lairs import LACE_PATTERNS

TEMPLATE = "lair/spindle_loft"

SIZE = (81, 64, 100)
LACE = 28                          # the doily's layer: its lace lies at the bottom of these blocks
DOILY = (40.5, 36.5)               # its centre (x, z)
DOILY_RADIUS = 20.5
# The four spools whose barrels pierce the doily's rim, by colour: (x, z) of their axles.
SPOOLS = {"green": (27.5, 23.5), "blue": (53.5, 23.5), "beige": (27.5, 49.5), "red": (53.5, 49.5)}
BARREL = 3.5                       # the thread wound on a spool
FLANGE = 5.0                       # the wooden flange on its top
SPOOL_TOP = LACE + 10              # the top flange's upper layer: its rim is stood on at SPOOL_TOP + 1
SPOOL_BOTTOM = LACE - 14           # where the barrels go down into the dark
SAFE_RING = 2.0                    # the dense lace round a spool's barrel
# The pincushion on its spool: centre and radii (x, y, z), the white spool's barrel and flange.
CUSHION = {"centre": (40.5, LACE + 2.0, 82.5), "radius": (9.0, 8.0, 9.0)}
PEDESTAL = {"barrel": 8.0, "flange": 11.0}
ARRIVAL = (40.5, LACE + 10, 82.5, 180.0)     # x, y, z and yaw (north): on the leaf at the cushion's top
# The tape: three blocks wide, from the cushion's top (z TAPE["top"], at the cushion's height) down to the doily's edge,
# half a block lower each block north, then flat onto the lace.
TAPE = {"x": (39, 41), "top": 79, "foot": 57}
# The needle in the cushion beside the arrival: its eye holds the Grey Mist (EXIT), facing west to the arrival.
NEEDLE = {"x": 45, "z": (81, 83)}
# The thimble on its side across the doily's east edge: its open end (x), closed end, radius and the axis's z.
THIMBLE = {"open": 56, "closed": 64, "radius": 4.0, "z": 36.5}
# The shears rising out of the dark beyond the doily's north edge, in the plane z = SHEARS["z"]: the pivot (x, y), the
# blades' length and spread (degrees from upright).
SHEARS = {"z": 12, "pivot": (40.5, LACE + 3.5), "blade": 18.0, "spread": 14.0, "handle": 9.0}
# The roof: two slopes, one up for each across, meeting at the ridge at x RIDGE on top.
RIDGE = 40
ROOF_TOP = SIZE[1] - 1
SKYLIGHT = {"x": (45, 51), "z": (31, 41)}
RAFTERS = range(3, SIZE[2], 8)     # z of each pair of rafters
GABLE_BOTTOM = SPOOL_BOTTOM        # the gable walls go down into the dark this far
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
CENTRE = (40.5, 56.5)
BOUNDS = 48
FLOOR = LACE - 6
# The hidden lights: (x, y, z, level).
LIGHTS = [
    (40, LACE + 5, 36, 15),
    (30, LACE + 3, 36, 11), (51, LACE + 3, 36, 11), (40, LACE + 3, 26, 11), (40, LACE + 3, 47, 11),
    (40, LACE + 12, 82, 12), (40, LACE + 6, 68, 9), (60, LACE + 3, 36, 7), (40, LACE + 13, 13, 9),
    (48, LACE + 20, 36, 12), (45, LACE + 12, 36, 12),
]


def roof_height(x):
    """The y of the roof over column x (its underside is half a block lower there, but for the ridge)."""
    return ROOF_TOP - abs(x - RIDGE)


# The doily's rings from its centre out, each its outer radius and pattern: a flower medallion, a band, mesh, a band, a
# ring of flowers, a band and mesh; past the last, the scalloped edge (0 band, 1 mesh, 2 flower, 3 edge).
RINGS = ((2.5, 2), (4.5, 0), (9.5, 1), (11.5, 0), (15.5, 2), (17.5, 0), (19.5, 1))


def lace_pattern(r):
    """The pattern of the doily's ring r blocks from its centre (RINGS)."""
    for limit, pattern in RINGS:
        if r < limit:
            return pattern
    return 3


# ---------------------------------------------------------------- states

LACE_STATES = [block("jugcraft:doily_lace", pattern=p) for p in range(LACE_PATTERNS)]
SPOOL_WOOD = block("jugcraft:spool_wood")
CUSHION_FABRIC = block("jugcraft:pincushion")
SEAM = block("jugcraft:pincushion_seam")
LEAF = block("jugcraft:pincushion_leaf")
STEEL = block("jugcraft:needle_steel")
PIN = block("jugcraft:pin_shaft")
BRASS = block("jugcraft:thimble_metal")
SKYLIGHT_GLASS = block("jugcraft:grimy_skylight")
EXIT_MIST = block("jugcraft:lair_exit", axis="z")
BOARDS = block("minecraft:spruce_planks")
STUD = block("minecraft:dark_oak_log", axis="y")
WOOD = block("minecraft:dark_oak_wood", axis="y")
COBWEB = block("minecraft:cobweb")
HANDLE = block("minecraft:polished_blackstone")
PIN_HEADS = ["red", "yellow", "light_blue", "white", "magenta", "lime"]


def thread(colour):
    return block("jugcraft:spool_thread", colour=colour)


def taut(axis, colour):
    return block("jugcraft:taut_thread", axis=axis, colour=colour)


def tape(mark, half):
    return block("jugcraft:measuring_tape", mark=mark, half=half)


def roof_stair(facing):
    return block("minecraft:spruce_stairs", facing=facing, half="top", shape="straight", waterlogged=False)


def beam(axis):
    return block("minecraft:dark_oak_log", axis=axis)


# ---------------------------------------------------------------- building

def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    roof(put)
    gables(put)
    for colour, (sx, sz) in SPOOLS.items():
        spool(put, sx, sz, colour)
    doily(put, blocks)
    pincushion(put, blocks)
    measuring_tape(put)
    thimble(put)
    shears(put)
    threads(put, blocks)
    larder(put, blocks)
    for x, y, z, level in LIGHTS:
        if (x, y, z) not in blocks:
            put(x, y, z, block("minecraft:light", level=level, waterlogged=False))
    return hollow(blocks)


def roof(put):
    """The two slopes of upside-down spruce stairs (a smooth underside), the ridge, the rafters, collar ties, purlins and
    ridge beam of dark oak, and the grimy skylight in the east slope over the doily."""
    for z in range(SIZE[2]):
        for x in range(SIZE[0]):
            y = roof_height(x)
            skylight = SKYLIGHT["x"][0] <= x <= SKYLIGHT["x"][1] and SKYLIGHT["z"][0] <= z <= SKYLIGHT["z"][1]
            if skylight:
                put(x, y, z, SKYLIGHT_GLASS)
            elif x == RIDGE:
                put(x, y, z, BOARDS)
            else:
                put(x, y, z, roof_stair("west" if x < RIDGE else "east"))
    for z in RAFTERS:
        for x in range(SIZE[0]):
            if x != RIDGE:
                put(x, roof_height(x) - 1, z, WOOD)
        tie = ROOF_TOP - 11
        for x in range(SIZE[0]):
            if roof_height(x) - 1 >= tie:
                put(x, tie, z, beam("x"))
    for z in range(SIZE[2]):
        for x in (RIDGE - 26, RIDGE + 26):
            put(x, roof_height(x) - 1, z, beam("z"))
        put(RIDGE, ROOF_TOP - 1, z, beam("z"))
    # The skylight's frame: beams round it under the roof.
    (x0, x1), (z0, z1) = SKYLIGHT["x"], SKYLIGHT["z"]
    for x in range(x0 - 1, x1 + 2):
        for z in (z0 - 1, z1 + 1):
            put(x, roof_height(x) - 1, z, beam("x"))


def gables(put):
    """The gable walls at each end, boards on studs, from the dark up under the roof; a round vent of grimy glass high
    in each."""
    for z in (0, SIZE[2] - 1):
        for x in range(SIZE[0]):
            for y in range(GABLE_BOTTOM, roof_height(x)):
                vent = math.hypot(x - RIDGE, y + 0.5 - (ROOF_TOP - 9)) <= 3.6
                if vent:
                    put(x, y, z, SKYLIGHT_GLASS)
                elif math.hypot(x - RIDGE, y + 0.5 - (ROOF_TOP - 9)) <= 4.6 or x % 8 == 0:
                    put(x, y, z, STUD)
                else:
                    put(x, y, z, BOARDS)


def spool(put, sx, sz, colour, barrel=BARREL, flange=FLANGE, top=SPOOL_TOP, bottom=SPOOL_BOTTOM):
    """A spool standing on end: its barrel of wound thread from the dark up to its top flange (two layers of wood, the
    axle hole dark in the middle)."""
    reach = int(math.ceil(flange))
    for dx in range(-reach - 1, reach + 1):
        for dz in range(-reach - 1, reach + 1):
            x, z = int(math.floor(sx)) + dx, int(math.floor(sz)) + dz
            r = math.hypot(x + 0.5 - sx, z + 0.5 - sz)
            if r <= barrel:
                for y in range(bottom, top - 1):
                    put(x, y, z, thread(colour))
            if r <= flange:
                for y in (top - 1, top):
                    put(x, y, z, beam("y") if r < 0.8 and y == top else SPOOL_WOOD)


def doily(put, blocks):
    """The doily at LACE: lace in rings by its pattern, the dense band round each spool's barrel."""
    cx, cz = DOILY
    reach = int(DOILY_RADIUS) + 1
    for x in range(int(cx) - reach, int(cx) + reach + 1):
        for z in range(int(cz) - reach, int(cz) + reach + 1):
            r = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
            if r > DOILY_RADIUS or (x, LACE, z) in blocks:
                continue  # beyond the rim, or a spool's barrel through it
            pattern = lace_pattern(r)
            near = min(math.hypot(x + 0.5 - sx, z + 0.5 - sz) for sx, sz in SPOOLS.values())
            if near <= BARREL + SAFE_RING:
                pattern = 0
            put(x, LACE, z, LACE_STATES[pattern])


def cushion_cells():
    """The pincushion's cells: an ellipsoid on the pedestal's flange, cut off at LACE."""
    (cx, cy, cz), (rx, ry, rz) = CUSHION["centre"], CUSHION["radius"]
    cells = set()
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            for y in range(LACE, int(cy + ry) + 2):
                if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 + ((z + 0.5 - cz) / rz) ** 2 <= 1.0:
                    cells.add((x, y, z))
    return cells


def surface(cells, x, z):
    """The y players stand on at the top of column (x, z) of the cushion, or None off it."""
    ys = [y for (cx, y, cz) in cells if cx == x and cz == z]
    return max(ys) + 1 if ys else None


def pincushion(put, blocks):
    """The tomato pincushion on its spool: red fabric in eight segments parted by seams, a five-pointed felt leaf on
    top, six pins with glass heads, and the needle whose eye is the way home."""
    cx, _, cz = CUSHION["centre"]
    spool(put, cx, cz, "white", barrel=PEDESTAL["barrel"], flange=PEDESTAL["flange"], top=LACE - 1)
    cells = cushion_cells()
    for (x, y, z) in cells:
        dx, dz = x + 0.5 - cx, z + 0.5 - cz
        r = math.hypot(dx, dz)
        angle = math.atan2(dz, dx)
        # The seams: eight meridians from the leaf down, about a block wide.
        offset = (angle / (math.pi / 4.0)) % 1.0
        seam = r > 1.5 and min(offset, 1.0 - offset) * (math.pi / 4.0) * r < 0.55
        put(x, y, z, SEAM if seam else CUSHION_FABRIC)
    # The leaf: a five-pointed felt star over the top.
    for x in range(int(cx) - 6, int(cx) + 7):
        for z in range(int(cz) - 6, int(cz) + 7):
            top = surface(cells, x, z)
            if top is None:
                continue
            dx, dz = x + 0.5 - cx, z + 0.5 - cz
            r, angle = math.hypot(dx, dz), math.atan2(dz, dx)
            point = max(0.0, math.cos(2.5 * (angle + math.pi / 2.0))) ** 2  # five points, one to the north
            if r <= 2.3 + 3.2 * point:
                put(x, top - 1, z, LEAF)
    # Six pins, each a shaft from the cushion as tall as a house and a glass head.
    for i, (px, pz) in enumerate(((34, 77), (47, 78), (33, 84), (36, 89), (44, 89), (48, 85))):
        base = surface(cells, px, pz)
        height = 12 + int(_hash(px, pz, 41) * 3)
        for y in range(base, base + height):
            put(px, y, pz, PIN)
        head = block(f"minecraft:{PIN_HEADS[i]}_stained_glass")
        hy = base + height + 1
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                for dz in range(-2, 3):
                    if dx * dx + dy * dy + dz * dz <= 2.6 * 2.6 / 1.2:
                        put(px + dx, hy + dy, pz + dz, head)
    # The needle beside the arrival, pushed deep into the cushion: its flat eye faces west, the Grey Mist in it.
    nx, (z0, z1) = NEEDLE["x"], NEEDLE["z"]
    base = min(surface(cells, nx, z) for z in range(z0, z1 + 1))
    for z in range(z0, z1 + 1):
        for y in range(base - 1, base + 7):
            put(nx, y, z, STEEL)
    put(nx, base + 7, (z0 + z1) // 2, STEEL)  # its rounded end
    for y in range(base + 2, base + 5):
        put(nx, y, (z0 + z1) // 2, EXIT_MIST)


def needle_eye():
    """Where the Grey Mist in the needle's eye is (for HollowAcre-like lookups and the audit)."""
    cells = cushion_cells()
    nx, (z0, z1) = NEEDLE["x"], NEEDLE["z"]
    base = min(surface(cells, nx, z) for z in range(z0, z1 + 1))
    return [(nx, y, (z0 + z1) // 2) for y in range(base + 2, base + 5)]


def tape_height(z):
    """The height (in half blocks, as y + 0.5 steps) of the tape's surface over row z, or None off it."""
    top, foot = TAPE["top"], TAPE["foot"]
    if not foot <= z <= top:
        return None
    return max(LACE, ARRIVAL[1] - (top - z) * 0.5)


def measuring_tape(put):
    """The tape: yellow, ticked along both edges, a red mark every fourth block down its middle; half a block lower each
    block from the cushion to the doily."""
    x0, x1 = TAPE["x"]
    for z in range(TAPE["foot"], TAPE["top"] + 1):
        h = tape_height(z)
        y = int(math.floor(h))
        half = "upper" if h - y >= 0.5 else "lower"
        for x in range(x0, x1 + 1):
            mark = 0 if x == x0 else 2 if x == x1 else (3 if z % 4 == 0 else 1)
            put(x, y, z, tape(mark, half))


def thimble(put):
    """The brass thimble on its side, open to the arena: a shell of brass round its axis from the open end to the closed
    one, domed there, its rolled rim a little thicker."""
    x0, x1, radius, cz = THIMBLE["open"], THIMBLE["closed"], THIMBLE["radius"], THIMBLE["z"]
    cy = LACE + 0.5 + radius
    for x in range(x0, x1 + 1):
        # The dome: the last three blocks close in.
        closing = max(0, x - (x1 - 3))
        outer = radius if closing == 0 else radius * math.sqrt(max(0.0, 1.0 - (closing / 3.6) ** 2))
        inner = outer - 1.0 if x < x1 - 1 else -1.0
        rim = 0.35 if x == x0 else 0.0
        for y in range(LACE, LACE + int(2 * radius) + 2):
            for z in range(int(cz - radius) - 1, int(cz + radius) + 2):
                r = math.hypot(y + 0.5 - cy, z + 0.5 - cz)
                if inner < r <= outer + rim:
                    put(x, y, z, BRASS)


def shears(put):
    """The open blades of a giant pair of shears rising out of the dark, crossed at a brass pivot, their black handles
    below in the dark: steel in the plane z SHEARS["z"]."""
    px, py = SHEARS["pivot"]
    z = SHEARS["z"]
    for side in (-1, 1):
        a = math.radians(SHEARS["spread"]) * side
        ux, uy = -math.sin(a), math.cos(a)            # along the blade, up to its tip
        vx, vy = uy, -ux                              # across it
        for x in range(SIZE[0]):
            for y in range(SIZE[1]):
                dx, dy = x + 0.5 - px, y + 0.5 - py
                t, s = dx * ux + dy * uy, dx * vx + dy * vy
                if 0.0 <= t <= SHEARS["blade"]:
                    # The blade: broad at the pivot, its back straight, its edge sweeping in to the point.
                    width = 0.55 + 2.4 * (1.0 - t / SHEARS["blade"]) ** 0.8
                    if -0.5 <= s * side <= width - 0.5:
                        put(x, y, z, STEEL)
                elif -SHEARS["handle"] <= t < 0.0:
                    # The shank down to the handle's loop.
                    ring = math.hypot(t + SHEARS["handle"] - 2.4, s * side - 0.6)
                    if abs(s * side) <= 0.75 and t > -SHEARS["handle"] + 4.5:
                        put(x, y, z, STEEL)
                    elif 1.4 <= ring <= 2.6:
                        put(x, y, z, HANDLE)
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            put(int(px) + dx, int(py) + dy, z - 1, BRASS)


def threads(put, blocks):
    """The taut threads: each spool's own thread from its axle up to the roof, and a white square of her silk between
    them above their tops, crossed over the doily's centre and hung from the ridge, where she hangs."""
    for colour, (sx, sz) in SPOOLS.items():
        x, z = int(sx), int(sz)
        for y in range(SPOOL_TOP + 1, roof_height(x)):
            if (x, y, z) not in blocks:
                put(x, y, z, taut("y", colour))
    web = SPOOL_TOP + 4
    (gx, gz), (rx, rz) = (int(v) for v in SPOOLS["green"]), (int(v) for v in SPOOLS["red"])
    cx, cz = int(DOILY[0]), int(DOILY[1])
    for x in range(gx + 1, rx):
        for z in (gz, cz, rz):
            put(x, web, z, taut("x", "white"))
    for z in range(gz + 1, rz):
        for x in (gx, cx, rx):
            if z != cz or x != cx:
                put(x, web, z, taut("z", "white"))
    for y in range(web + 1, ROOF_TOP - 1):
        put(cx, y, cz, taut("y", "white"))


def larder(put, blocks):
    """The Spider's Larder: cocoons hung under the spools' flanges and the collar ties, egg sacs on the barrels, a silk
    spool stack on each spool's top, cobwebs in the roof."""
    for colour, (sx, sz) in SPOOLS.items():
        x, z = int(sx), int(sz)
        # A cocoon under the flange's rim on the side away from the doily, and one toward it.
        out_x = 1 if sx > DOILY[0] else -1
        out_z = 1 if sz > DOILY[1] else -1
        put(x + 4 * out_x, SPOOL_TOP - 2, z, block("jugcraft:silk_cocoon"))
        put(x, SPOOL_TOP - 2, z - 4 * out_z, block("jugcraft:silk_cocoon"))
        # Egg sacs on the barrel's outer side.
        for dy in (3, 4, 7):
            ex = x + 4 * out_x
            put(ex, LACE + dy, z + (1 if dy == 4 else 0), block("jugcraft:egg_sac_cluster", **{"west" if out_x > 0 else "east": True}))
        put(x - 2 * out_x, SPOOL_TOP + 1, z - 2 * out_z, block("jugcraft:silk_spool_stack", facing="south"))
    tie = ROOF_TOP - 11
    for z in (19, 51):
        for x in (33, 47):
            put(x, tie - 1, z, block("jugcraft:silk_cocoon"))
    for z in (1, 2, SIZE[2] - 3, SIZE[2] - 2):
        for x in range(2, SIZE[0] - 2, 9):
            y = roof_height(x) - 2
            if (x, y, z) not in blocks and _hash(x, z, 43) < 0.7:
                put(x, y, z, COBWEB)


def hollow(blocks):
    """Leaves out blocks nobody can see: those whose six neighbours are all whole, opaque blocks of the template."""
    opaque = {SPOOL_WOOD, CUSHION_FABRIC, SEAM, LEAF, STEEL, BRASS, BOARDS, STUD, WOOD, HANDLE}
    opaque.update(thread(c) for c in SPOOLS)
    opaque.add(thread("white"))
    opaque.update(beam(a) for a in "xyz")
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
