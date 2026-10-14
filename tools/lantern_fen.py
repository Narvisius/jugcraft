"""The Lantern Fen (docs/features/lantern-fen.md): the Mire Hag's lair, written as the structure template
data/jugcraft/structure/lair/lantern_fen.nbt, which lair/Lairs places into every instance of the jugcraft:lantern_fen
dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI checks it is current).

A drowned fen floating in a night of mist: an island of mud about 75 blocks across (x) and 83 long (z, from its north
rim near 0 to its south rim near 85), its underside hanging in roots over the void. Wherever there is no bank, black
water lies knee-deep over the mud (WATER over MUD), held in by a bank all round the rim. Angles below are measured
clockwise from north, round CENTRE.
- the marsh, the arena: MARSH_RADIUS round its middle, open water with a few tussocks (TUSSOCKS) and drowned stumps
  (STUMPS);
- the boardwalks: willow planks two blocks wide laid in the water's own layer (DECK), so they are stood on a block above
  the mud with the water just under them. A ring (RING) round the marsh's middle joins four lamp platforms on its
  diagonals; a spur runs north from it to the hut, and the jetty south to the arrival;
- the lamps (LAMPS): on each platform a great lantern, the lair-only fen lamp, on a willow post, burning;
- the hut (HUT): the Hag's hut on stilts at the north rim, of dead planks under a roof of dead-wood stairs, its porch
  over the water facing the marsh with her cauldron on it, a stair (PORCH_STAIR) climbing to it from the spur;
- the banks: beyond the marsh, tussocks and reed beds, more of them toward the rim, and a bank all round the rim; dead
  willows hung with shroud moss and vines stand on them, and reeds, cattails and ferns grow along them;
- the arrival (ARRIVAL): the south end of the jetty, facing north, a punt moored beside it (PUNT); behind it Grey Mist
  hangs between two dead willows whose crowns have grown together (EXIT), the way home.
The light is hidden light blocks, the lamps, and the lanterns and cauldron of the hut.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written (the lair-only blocks' are written with them); vanilla blocks use only properties they have had for
years. Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/lantern_fen"

SIZE = (80, 40, 88)
CENTRE = (40.0, 43.0)              # the marsh's middle (block corners: cell x spans x to x + 1)
MUD = 8                            # the mud bed's top layer
WATER = 9                          # the water's layer: knee-deep, stood in on the mud at WATER
DECK = WATER                       # the boardwalks' planks lie in the water's layer: stood on at DECK + 1
STAND = DECK + 1                   # where anyone on a boardwalk or a bank stands
# The island: an ellipse round CENTRE (radii x, z), its edge rippled by the noise; its underside hangs DEPTH blocks deep
# at the middle, thinning to the rim.
ISLAND = (37.5, 41.5)
EDGE_WOBBLE = 1.5
DEPTH = 8
RIM = 2.5                          # the bank all round the rim (blocks in from the edge), which holds the water in
MARSH_RADIUS = 22.0                # the arena: open water but for its tussocks and stumps
BANK_SHARE = 0.42                  # how much of the fen beyond the marsh is bank, rising toward the rim
# The boardwalks: the ring (between these radii), the lamp platforms (each a square, PLATFORM blocks either side of
# its lamp) and the spurs (two blocks wide, the columns SPUR) north to the porch's stair and south to the jetty's end.
RING = (13.0, 15.0)
PLATFORM = 2
LAMPS = [(30, 33), (49, 33), (30, 52), (49, 52)]   # (x, z) of each lamp's post: north-west, north-east, south-west, south-east
LAMP_HEIGHT = 2                    # the post's blocks; the lamp stands on it at STAND + LAMP_HEIGHT
SPUR = (39, 40)
JETTY_END = 82                     # the jetty's last row (z)
# A punt moored to the post west of the jetty's end, its stern half sunk: its columns and its rows, bow (north) to stern.
PUNT = {"x": (36, 37), "z": (74, 78)}
# The hut: its walls (x, z ranges), its floor's layer, its porch (z rows, in front of the south wall) and the stair
# down from the porch to the spur, two steps (z rows, from the porch out).
HUT = {"x": (34, 46), "z": (8, 15), "floor": STAND + 1, "wall_top": STAND + 6, "porch": (16, 19)}
PORCH_STAIR = (20, 21)
CAULDRON = (38, HUT["floor"] + 1, 18)        # the Hag's cauldron on the porch
WAIT = (40.5, HUT["floor"] + 1, 17.5, 0.0)   # where she waits (part 2): x, y, z and yaw (facing south, over the marsh)
ARRIVAL = (40.0, STAND, 80.5, 180.0)          # x, y, z and yaw: at the jetty's end, facing north
WOKE = (42.5, 24.5)                           # where she comes down into the water as she wakes (part 2): x, z
MIDDLE_EXIT = {"x": (39, 40), "z": 43}        # where the Grey Mist opens when she falls (part 2): in the water and over it
EXIT = {"x": (39, 40), "z": 83}               # the Grey Mist's columns and row, three blocks high from STAND
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
BOUNDS = 44
FLOOR = MUD - DEPTH - 4
# Tussocks in the marsh: (x, z, radius), and drowned stumps: (x, z, height over the water).
TUSSOCKS = [(36, 39, 1.3), (44, 47, 1.5), (24, 44, 1.8), (56, 41, 1.7), (33, 61, 1.4), (47, 25, 1.4)]
STUMPS = [(43, 38, 1), (37, 49, 2), (52, 47, 1), (27, 37, 2), (50, 60, 1), (31, 27, 1)]
# Dead willows: how many, how far apart at least, and how tall their trunks are.
TREES = {"count": 16, "spacing": 6.0, "height": (5, 8)}
# The hidden lights: a lattice at each of these heights (y, level, spacing, staggered: every other row shifted half a
# spacing along, the rows half a spacing apart; a light whose cell is filled hangs beside it), and these: (x, y, z, level).
LIGHT_LAYERS = ((STAND + 2, 14, 8, True),)
LIGHTS = [
    (40, STAND + 2, 79, 12),                         # over the arrival
    (40, HUT["floor"] + 3, 11, 12),                  # in the hut
]
MARSH_LIGHT = 8                    # the least block light on any cell where anyone stands in the marsh (the audit)


# ---------------------------------------------------------------- states

WATER_BLOCK = block("minecraft:water", level=0)
MUD_BLOCK = block("minecraft:mud")
PACKED_MUD = block("minecraft:packed_mud")
DIRT = block("minecraft:dirt")
ROOTED = block("minecraft:rooted_dirt")
CLAY = block("minecraft:clay")
GRASS = block("minecraft:grass_block", snowy=False)
MOSS = block("minecraft:moss_block")
PODZOL = block("minecraft:podzol", snowy=False)
HANGING_ROOTS = block("minecraft:hanging_roots", waterlogged=False)
PLANKS = block("jugcraft:willow_planks")
POST = block("jugcraft:willow_fence")
WILLOW = {axis: block("jugcraft:willow_log", axis=axis) for axis in "xyz"}
STRIPPED = block("jugcraft:stripped_willow_log", axis="y")
# Bare twigs: dead bushes, which never change once placed (leaves would decay and follow the seasons).
TWIGS = block("minecraft:dead_bush")
DEAD_PLANKS = block("jugcraft:dead_planks")
DEAD_LOG = {axis: block("jugcraft:dead_log", axis=axis) for axis in "xyz"}
DEAD_FENCE = block("jugcraft:dead_fence")
EXIT_MIST = block("jugcraft:lair_exit", axis="x")
LILY = block("minecraft:lily_pad")
DUCKWEED = block("jugcraft:duckweed")
SEAGRASS = block("minecraft:seagrass")
SHORT_GRASS = block("minecraft:short_grass")
FERN = block("minecraft:fern")
GHOST_FERN = block("jugcraft:ghost_fern")
WITHERED = block("jugcraft:withered_grass")
BRAMBLE = block("jugcraft:bramble")
NIGHTSHADE = block("jugcraft:deadly_nightshade")
CATTAIL = {half: block("jugcraft:cattail", half=half) for half in ("lower", "upper")}
MOSS_CARPET = block("minecraft:moss_carpet")
SKULL = block("minecraft:skeleton_skull", rotation=0)
CAULDRON_BLOCK = block("jugcraft:bubbling_cauldron", contents="green")
SHELF = block("jugcraft:apothecary_shelf", arrangement=1, facing="south")
WITCH_LANTERN = block("jugcraft:witchs_lantern", hanging=True)


def fen_lamp(lit=True):
    return block("jugcraft:fen_lamp", lit=lit)


def light(level):
    return block("minecraft:light", level=level, waterlogged=False)


def shroud(tip):
    return block("jugcraft:shroud_moss", tip=tip)


def stairs(facing, half="bottom", shape="straight"):
    return block("jugcraft:dead_stairs", facing=facing, half=half, shape=shape)


def slab(kind):
    return block("jugcraft:dead_slab", type=kind)


def willow_stairs(facing):
    return block("jugcraft:willow_stairs", facing=facing, half="bottom", shape="straight")


def ivy(side):
    """Creeping ivy on the face of the block to its `side` (north, east, south or west). Unlike vanilla's vines it never
    spreads on its own, so the fen stays as it is placed."""
    return block("jugcraft:creeping_ivy", **{side: True})


# ---------------------------------------------------------------- shapes

def noise(x, z, scale, salt):
    """Smooth value noise in the plane, 0 to 1: the hash at the corners of a lattice `scale` blocks apart, blended."""
    fx, fz = x / scale, z / scale
    ix, iz = math.floor(fx), math.floor(fz)
    tx, tz = (t * t * (3.0 - 2.0 * t) for t in (fx - ix, fz - iz))
    total = 0.0
    for cx in (0, 1):
        for cz in (0, 1):
            total += (tx if cx else 1.0 - tx) * (tz if cz else 1.0 - tz) * _hash(ix + cx, iz + cz, salt)
    return total


def polar(x, z):
    """The radius and the angle (degrees clockwise from north) of cell (x, z) round the marsh's middle."""
    dx, dz = x + 0.5 - CENTRE[0], z + 0.5 - CENTRE[1]
    return math.hypot(dx, dz), math.degrees(math.atan2(dx, -dz)) % 360.0


def island_reach(x, z):
    """How far into the island cell (x, z) is: 1 at its middle, 0 at its rippled edge, below 0 outside it."""
    dx, dz = (x + 0.5 - CENTRE[0]) / ISLAND[0], (z + 0.5 - CENTRE[1]) / ISLAND[1]
    e = math.hypot(dx, dz)
    edge = 1.0 + EDGE_WOBBLE * (noise(x, z, 9.0, 11) - 0.5) / min(ISLAND)
    return 1.0 - e / edge


def on_island(x, z):
    return island_reach(x, z) > 0.0


def on_rim(x, z):
    """The bank all round the rim: within RIM blocks of the island's edge."""
    return on_island(x, z) and island_reach(x, z) * min(ISLAND) < RIM


def in_marsh(x, z):
    return polar(x, z)[0] <= MARSH_RADIUS


def on_ring(x, z):
    return RING[0] <= polar(x, z)[0] < RING[1]


def on_platform(x, z):
    return any(max(abs(x - lx), abs(z - lz)) <= PLATFORM for lx, lz in LAMPS)


def on_spur(x, z):
    """The spur north from the ring to the porch's stair, and the jetty south from it to its end."""
    if x not in SPUR:
        return False
    north = PORCH_STAIR[1] + 1 <= z <= CENTRE[1] - RING[0]
    south = CENTRE[1] + RING[0] <= z <= JETTY_END
    return north or south


def on_deck(x, z):
    """Whether cell (x, z) is boardwalk: the ring, a platform, the spur or the jetty."""
    return on_ring(x, z) or on_platform(x, z) or on_spur(x, z)


def under_hut(x, z):
    """The hut, its porch and its stair: over their cells there is no bank, plant or tree."""
    (x0, x1), (z0, _) = HUT["x"], HUT["z"]
    return x0 - 1 <= x <= x1 + 1 and z0 - 1 <= z <= PORCH_STAIR[1]


def in_tussock(x, z):
    return any(math.hypot(x + 0.5 - tx - 0.5, z + 0.5 - tz - 0.5) <= r for tx, tz, r in TUSSOCKS)


def is_bank(x, z):
    """Whether cell (x, z) is bank (land whose top is level with the boardwalks) rather than water."""
    if not on_island(x, z) or on_deck(x, z):
        return False
    if on_rim(x, z):
        return True
    if in_marsh(x, z):
        return in_tussock(x, z)
    r, _ = polar(x, z)
    # Beyond the marsh the banks grow toward the rim: a share of the noise's highest cells, more of them farther out.
    toward_rim = 1.0 - island_reach(x, z)
    return noise(x, z, 5.5, 23) > 1.0 - BANK_SHARE * (0.6 + 0.8 * toward_rim) and r > MARSH_RADIUS + 1.0


def is_water(x, z):
    return on_island(x, z) and not is_bank(x, z) and not on_deck(x, z)


def underside_depth(x, z):
    """How far the island's underside hangs below the mud bed at cell (x, z)."""
    reach = max(0.0, island_reach(x, z))
    return 2 + int(round(DEPTH * reach ** 0.6 * (0.8 + 0.4 * noise(x, z, 6.0, 31))))


# ---------------------------------------------------------------- building

def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    ground(put)
    boardwalks(put)
    punt(put)
    lamps(put)
    hut(put, blocks)
    trees(put, blocks)
    plants(put, blocks)
    exit_arch(put)
    # Grass under a whole block (a trunk, a post) would turn to dirt in the lair: podzol, which never changes, lies there.
    covers = {*WILLOW.values(), *DEAD_LOG.values(), STRIPPED, PLANKS, DEAD_PLANKS}
    for (x, y, z), state in list(blocks.items()):
        if state == GRASS and blocks.get((x, y + 1, z)) in covers:
            put(x, y, z, PODZOL)
    for y, level, spacing, staggered in LIGHT_LAYERS:
        for row, z in enumerate(range(4, SIZE[2], spacing // 2 if staggered else spacing)):
            for x in range(4 + (spacing // 2 if staggered and row % 2 else 0), SIZE[0], spacing):
                # A light whose cell a tree or the hut fills hangs in the first free cell beside it instead.
                for cx, cz in ((x, z), (x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                    if on_island(cx, cz) and (cx, y, cz) not in blocks:
                        put(cx, y, cz, light(level))
                        break
    for x, y, z, level in LIGHTS:
        if (x, y, z) not in blocks:
            put(x, y, z, light(level))
    return hollow(blocks)


def ground(put):
    """The island: the mud bed, the water over it or a bank's top, and its underside, roots hanging under it."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if not on_island(x, z):
                continue
            depth = underside_depth(x, z)
            for y in range(MUD - depth, MUD):
                put(x, y, z, MUD_BLOCK if y >= MUD - 2 else (CLAY if _hash(x, y, z, 41) < 0.15 else DIRT))
            put(x, MUD - depth - 1, z, ROOTED)
            if _hash(x, z, 43) < 0.2:
                put(x, MUD - depth - 2, z, HANGING_ROOTS)
            put(x, MUD, z, PACKED_MUD if _hash(x, z, 47) < 0.08 else MUD_BLOCK)
            if is_bank(x, z):
                h = _hash(x, z, 53)
                put(x, WATER, z, MOSS if h < 0.2 else PODZOL if h < 0.3 else MUD_BLOCK if h < 0.38 else GRASS)
            elif not on_deck(x, z):
                put(x, WATER, z, WATER_BLOCK)


def boardwalks(put):
    """Willow planks in the water's layer: the ring, the platforms, the spur and the jetty; posts along the jetty, and
    stilts under the planks where they cross deeper water (none: the water is knee-deep everywhere)."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if on_island(x, z) and on_deck(x, z) and not under_hut(x, z):
                put(x, DECK, z, PLANKS)
    # Mooring posts along the jetty, every fourth row, on the water either side.
    for z in range(int(CENTRE[1] + RING[1]) + 2, JETTY_END, 4):
        for x in (SPUR[0] - 1, SPUR[1] + 1):
            if not is_bank(x, z):
                put(x, STAND, z, DEAD_FENCE)
                put(x, DECK, z, DEAD_LOG["y"])


def punt(put):
    """A flat-bottomed punt of dead wood riding on the water beside the jetty's end: its bow a stair rising north, its
    floor slabs on the water, its stern a slab sunk half a block into it."""
    (x0, x1), (z0, z1) = PUNT["x"], PUNT["z"]
    for x in range(x0, x1 + 1):
        put(x, STAND, z0, stairs("north"))
        for z in range(z0 + 1, z1):
            put(x, STAND, z, slab("bottom"))
        put(x, WATER, z1, slab("top"))


def punt_cells():
    """The punt's cells, which no plant shares."""
    (x0, x1), (z0, z1) = PUNT["x"], PUNT["z"]
    return {(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)}


def lamps(put):
    """Each lamp: a willow post LAMP_HEIGHT blocks high in the middle of its platform, and the fen lamp on top, lit."""
    for x, z in LAMPS:
        for y in range(STAND, STAND + LAMP_HEIGHT):
            put(x, y, z, POST)
        put(x, STAND + LAMP_HEIGHT, z, fen_lamp(True))


def lamp_blocks():
    """Where each lamp is (for the audit and the tests)."""
    return [(x, STAND + LAMP_HEIGHT, z) for x, z in LAMPS]


def hut(put, blocks):
    """The Hag's hut on stilts: a floor of dead planks, walls with stripped-willow corner posts, a door and windows, a
    gabled roof of dead-wood stairs whose eaves overhang the porch; inside, her shelves and a hanging lantern; on the
    porch, her cauldron, a skull on a post and two lanterns under the eaves; and the stair down to the spur."""
    (x0, x1), (z0, z1) = HUT["x"], HUT["z"]
    floor, top = HUT["floor"], HUT["wall_top"]
    p0, p1 = HUT["porch"]
    # Stilts under the floor and the porch, from the mud up.
    for x in (x0, x0 + 4, x1 - 4, x1):
        for z in (z0, z1, p1):
            for y in range(MUD + 1, floor):
                put(x, y, z, STRIPPED)
    # The floor, the porch's boards.
    for x in range(x0, x1 + 1):
        for z in range(z0, p1 + 1):
            put(x, floor, z, DEAD_PLANKS)
    # The walls: corner posts, plank walls with a door (south) and windows.
    for y in range(floor + 1, top + 1):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                corner = x in (x0, x1)
                door = z == z1 and x in (39, 40, 41) and y <= floor + 3
                window = y in (floor + 2, floor + 3) and x in (x0 + 3, x1 - 3) and not corner
                if door or window:
                    continue
                put(x, y, z, STRIPPED if corner else DEAD_PLANKS)
        for z in range(z0 + 1, z1):
            for x in (x0, x1):
                window = y in (floor + 2, floor + 3) and z in (z0 + 3, z1 - 3)
                if not window:
                    put(x, y, z, DEAD_PLANKS)
    # The roof: a gable, its ridge along x, its eaves overhanging the walls by one block north and over the porch's
    # first row south; the gable ends filled with planks up to it.
    north, south = z0 - 1, z1 + 1
    rows = list(range(north, south + 1))
    half = len(rows) // 2
    for i, z in enumerate(rows):
        rise = i if i < half else len(rows) - 1 - i
        y = top + 1 + rise
        facing = "south" if i < half else "north"
        for x in range(x0 - 1, x1 + 2):
            put(x, y, z, stairs(facing))
        for x in (x0, x1):
            for fill in range(top + 1, y):
                if z0 <= z <= z1:
                    put(x, fill, z, DEAD_PLANKS)
    # Inside: her shelves along the north wall, a lantern hung from the ridge, moss on the boards.
    for x in (x0 + 2, x0 + 3, x1 - 3, x1 - 2):
        put(x, floor + 1, z0 + 1, SHELF)
    put(40, top + half - 1, (z0 + z1) // 2, WITCH_LANTERN)
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if _hash(x, z, 61) < 0.18 and (x, floor + 1, z) not in blocks:
                put(x, floor + 1, z, MOSS_CARPET)
    # The porch: her cauldron, a skull on a post at each front corner, lanterns under the eaves.
    put(*CAULDRON, CAULDRON_BLOCK)
    for x in (x0, x1):
        put(x, floor + 1, p1, DEAD_FENCE)
        put(x, floor + 2, p1, SKULL)
    for x in (x0 + 2, x1 - 2):
        put(x, top, south, WITCH_LANTERN)
    # The stair from the porch down to the spur: a step a block lower each row out.
    for i, z in enumerate((PORCH_STAIR[0], PORCH_STAIR[1])):
        for x in SPUR:
            put(x, floor - i, z, willow_stairs("north"))
            for y in range(MUD + 1, floor - i):
                put(x, y, z, STRIPPED if y < DECK else PLANKS)


def stair_cells():
    """The porch stair's steps: (x, y, z) of each stair block (for the audit)."""
    return [(x, HUT["floor"] - i, z) for i, z in enumerate(PORCH_STAIR) for x in SPUR]


def tree_spots():
    """Where the dead willows stand: bank cells beyond the marsh, never on the hut, the deck or the exit's row, at least
    TREES["spacing"] apart, chosen by the hash."""
    spots = []
    candidates = sorted(((x, z) for x in range(SIZE[0]) for z in range(SIZE[2])
                         if is_bank(x, z) and polar(x, z)[0] > MARSH_RADIUS + 2.0 and not under_hut(x, z)
                         and abs(z - EXIT["z"]) > 3 and island_reach(x, z) * min(ISLAND) > 1.0),
                        key=lambda c: _hash(c[0], c[1], 71))
    for x, z in candidates:
        if all(math.hypot(x - a, z - b) >= TREES["spacing"] for a, b in spots):
            spots.append((x, z))
        if len(spots) == TREES["count"]:
            break
    return spots


def trees(put, blocks):
    """Dead willows: a trunk of willow log leaning a little in its upper half, three or four bare branches from
    different heights reaching out and up (a block higher every two out), bare twigs on their tips and on the crown,
    shroud moss hanging from the branches, and ivy on the trunk's lower half."""
    lo, hi = TREES["height"]
    sides = [(1, 0, "x"), (0, 1, "z"), (-1, 0, "x"), (0, -1, "z")]
    for i, (x, z) in enumerate(tree_spots()):
        height = lo + int(_hash(x, z, 73) * (hi - lo + 1))
        lean = sides[int(_hash(x, z, 75) * 4)]
        top = (x, z)
        for y in range(STAND, STAND + height):
            if y == STAND + height // 2 + 1 and _hash(x, z, 77) < 0.6:
                top = (x + lean[0], z + lean[1])
                put(top[0], y - 1, top[1], WILLOW[lean[2]])
            put(top[0], y, top[1], WILLOW["y"])
        crown = STAND + height
        put(top[0], crown, top[1], TWIGS)
        start = int(_hash(x, z, 79) * 4)
        for k in range(3 + (i % 2)):
            dx, dz, axis = sides[(start + k) % 4]
            y = crown - 1 - (k % 3)
            reach = 2 + int(_hash(x, z, 83 + k) * 3)
            bx, bz, by = top[0], top[1], y
            for step in range(1, reach + 1):
                bx, bz = bx + dx, bz + dz
                if step % 2 == 0:
                    by += 1
                    put(bx, by - 1, bz, WILLOW["y"])
                put(bx, by, bz, WILLOW[axis])
                if step == reach // 2 + 1 and (bx, by - 1, bz) not in blocks:
                    for d in range(1, 3):
                        if (bx, by - d, bz) not in blocks and by - d > STAND:
                            put(bx, by - d, bz, shroud(d == 2))
            put(bx, by + 1, bz, TWIGS)
            drop = 2 + int(_hash(x, z, 89 + k) * 2)
            for d in range(1, drop + 1):
                if (bx, by - d, bz) not in blocks and by - d > STAND:
                    put(bx, by - d, bz, shroud(d == drop))
        # Ivy on the trunk's lower half, on the side away from the marsh.
        dx = 1 if x + 0.5 > CENTRE[0] else -1
        side = "west" if dx > 0 else "east"
        for y in range(STAND + 1, STAND + height // 2 + 1):
            if (x + dx, y, z) not in blocks:
                put(x + dx, y, z, ivy(side))


def plants(put, blocks):
    """Reeds and cattails along the banks' edges, ferns, grass, brambles and nightshade on them; lily pads, duckweed
    and seagrass in the open water (never on or beside the boardwalks), drowned stumps in the marsh."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if not on_island(x, z) or under_hut(x, z) or (x, STAND, z) in blocks or (x, z) in punt_cells():
                continue
            h = _hash(x, z, 101)
            if is_bank(x, z):
                shore = any(is_water(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if shore and h < 0.45 and (x, STAND + 1, z) not in blocks:
                    put(x, STAND, z, CATTAIL["lower"])
                    put(x, STAND + 1, z, CATTAIL["upper"])
                elif h < 0.30:
                    put(x, STAND, z, FERN if h < 0.12 else SHORT_GRASS)
                elif h < 0.38:
                    put(x, STAND, z, GHOST_FERN)
                elif h < 0.44:
                    put(x, STAND, z, WITHERED)
                elif h < 0.47:
                    put(x, STAND, z, BRAMBLE)
                elif h < 0.49:
                    put(x, STAND, z, NIGHTSHADE)
            elif is_water(x, z) and not near_deck(x, z):
                if h < 0.025:
                    put(x, STAND, z, LILY)
                elif h < 0.04:
                    put(x, STAND, z, DUCKWEED)
                elif h < 0.08 and not in_marsh(x, z):
                    put(x, WATER, z, SEAGRASS)
    for x, z, height in STUMPS:
        for y in range(WATER, STAND + height):
            put(x, y, z, DEAD_LOG["y"])


def near_deck(x, z):
    return any(on_deck(x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1))


def exit_arch(put):
    """Two dead willows either side of the jetty's end, their crowns grown together over it and broken off a block
    beyond, twigs along the top, the Grey Mist between them."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    for y in range(STAND, STAND + 5):
        put(x0 - 1, y, z, WILLOW["y"])
        put(x1 + 1, y, z, WILLOW["y"])
    for x in range(x0 - 2, x1 + 3):
        put(x, STAND + 5, z, WILLOW["x"])
        put(x, STAND + 6, z, TWIGS)
    for x in range(x0, x1 + 1):
        put(x, STAND + 4, z, shroud(True))
        for y in range(STAND, STAND + 3):
            put(x, y, z, EXIT_MIST)
        put(x, DECK, z, PLANKS)



def exit_mist():
    """Where the Grey Mist between the willows is (for the audit and the tests)."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(STAND, STAND + 3) for x in range(x0, x1 + 1)]


def hollow(blocks):
    """Leaves out blocks nobody can see: those whose six neighbours are all whole, opaque blocks of the template."""
    opaque = {MUD_BLOCK, PACKED_MUD, DIRT, ROOTED, CLAY, GRASS, MOSS, PODZOL}
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
