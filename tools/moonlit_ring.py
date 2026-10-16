"""The Moonlit Ring (docs/features/moonlit-ring.md): the Alpha Werewolf's lair, written as the structure template
data/jugcraft/structure/lair/moonlit_ring.nbt, which lair/Lairs places into every instance of the jugcraft:moonlit_ring
dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI checks it is current).

A clearing in an old wood under a huge full moon: a slab of forest floor about 75 blocks across (x) and 78 long (z)
hanging in the void. North is low z.
- the ring, the arena (RING): a clearing RING blocks round the centre (CENTRE) of moonlit moss, with low ferns and grass;
  a path of trodden earth round it just inside the trees (PATH), and three-wide spokes from the Howling Stone out to the
  three bell clearings and the camp (SPOKES); a few fallen logs and boulders for low cover (LOGS, BOULDERS);
- the trees: the trunks of giant oaks (dark oak logs) in two rings round the clearing (TRUNK_RINGS), a thicket of
  Moonwood Leaves (the lair-only moonwood_leaves) between and beyond them, too thick to pass, and their canopy (CANOPY)
  closing overhead round the edge and opening in the middle onto the moon;
- the three bell clearings (BELLS): at the ring's west, north and east, each a small glade let into the trees at the end
  of a passage from the ring; in each a Warden Bell (the lair-only warden_bell, a fixture any player may ring) hangs from
  a spruce frame with a little gabled roof;
- the Howling Stone (STONE): a grey outcrop at the centre, three blocks high in its middle and stepped down round it,
  where the Alpha crouches waiting (WAIT, part 2);
- the Hunters' Camp (CAMP): a hollow at the ring's south edge under the trees, at the end of the south spoke: a lean-to
  of hides, a cold firepit, werewolf rugs and a stump with a lantern; the arrival (ARRIVAL), and at the end of the trail
  behind it Grey Mist (EXIT): the way home.
The moon is not in the template: Lairs places it, MOON_RADIUS across, north beyond the trees (MOON), high enough that the
Alpha sees it over the northern canopy from the Howling Stone. The light is hidden light blocks over the clearing, the
glades and the camp, the camp's lantern and the Grey Mist.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written (the lair-only blocks' are written with them); vanilla blocks use only properties they have had for
years. Nothing in it changes by itself once placed: no vanilla leaves (they would decay and follow the seasons), no grass
blocks (they would die under the thicket), moss for the clearing's turf. Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/moonlit_ring"

SIZE = (75, 40, 78)
SOIL = 4                           # the forest floor's top layer
STAND = SOIL + 1                   # where anyone on the floor stands
CENTRE = (37.5, 36.5)              # the middle of the clearing: the middle of cell (37, 36)
CX, CZ = 37, 36                    # that cell
RING = 22.0                        # the clearing: every cell whose middle is within this of CENTRE
PATH = (19.0, 21.0)                # the path round the clearing: cells whose middle is this far from CENTRE
# The spokes: three cells wide, from the Howling Stone's foot (SPOKE_FROM) out along each axis to the ring's edge, and on
# through a passage (five wide) into the bell clearings and the camp.
SPOKE_FROM = 4.5
PASSAGE_HALF = 2                   # a passage is the spoke's middle cell and this many either side
# The three bell clearings, each by the way out from the centre (x, z), the distance of its glade's middle and its
# radius, and the distance of its bell from the centre; the bell hangs from a frame (posts either side of it across the
# way, a beam over it and a gabled roof over that) and faces the ring.
BELLS = {
    "west": {"way": (-1, 0), "face": "east"},
    "north": {"way": (0, -1), "face": "south"},
    "east": {"way": (1, 0), "face": "west"},
}
GLADE_AT = 26.0
GLADE_RADIUS = 3.6
BELL_AT = 29
BELL_Y = STAND + 2                 # the bell hangs from the beam over it
BEAM_Y = STAND + 3
ROOF_Y = STAND + 4
# The Hunters' Camp: the south passage opens into it; its open cells (inclusive), the arrival in it (x, y, z, yaw:
# facing north up the spoke into the ring) and the trail behind it to the Grey Mist (EXIT: its columns and row, three
# high).
CAMP = {"x": (31, 43), "z": (61, 70)}
ARRIVAL = (37.5, float(STAND), 67.5, 180.0)
TRAIL = {"x": (36, 38), "z": (71, 72)}
EXIT = {"x": (36, 38), "z": 72}
LEAN_TO = {"x": (32, 34), "z": (63, 66)}       # the lean-to of hides: back wall at x 32, open to the east
FIREPIT = (40, 65)
STUMP = (42, 62)                               # a stump with a lantern on it
RUGS = [(33, 64, "east"), (33, 65, "east"), (39, 67, "north")]   # werewolf rugs: x, z, facing
# The Howling Stone: cells within each of these of CENTRE stand this high (the core three, a step of two round it, a
# step of one round that), and where the Alpha waits on its top (part 2): x, y, z and yaw (facing north, to the moon).
STONE = ((1.6, 3), (2.8, 2), (3.8, 1))
WAIT = (37.5, STAND + 3, 36.5, 180.0)
# Where the Grey Mist opens on the Howling Stone's top when the Alpha falls (part 2): three columns across its middle, two
# high.
STONE_EXIT = {"x": (36, 38), "z": 36, "y": STAND + 3}
# The trees: trunk rings (radius from the centre, how many trunks, the angle of the first, the trunks' radius); a trunk
# that would stand in the clearing, a passage, a glade or the camp is left out. The thicket of Moonwood Leaves fills the
# floor of the wood up to THICKET_TOP, and the canopy hangs from CANOPY_LOW to CANOPY_HIGH over every cell farther than
# CANOPY_INNER from the centre (ragged by up to CANOPY_RAG).
TRUNK_RINGS = ((25.0, 30, 0.0, 1.7), (31.0, 34, 5.3, 1.9))
TRUNK_TOP = STAND + 13
THICKET_TOP = STAND + 3
CANOPY_LOW = STAND + 10
CANOPY_HIGH = STAND + 14
CANOPY_INNER = 18.5
CANOPY_RAG = 2.5
# Low cover in the clearing: fallen oak logs (x, z, axis, length) with moss on their tops, and boulders (x, z, the cells
# of each, height).
LOGS = [(23, 27, "x", 5), (50, 45, "z", 5)]
BOULDERS = [((49, 25), ((0, 0), (1, 0), (0, 1), (1, 1)), 2), ((24, 46), ((0, 0), (1, 0), (0, 1)), 2),
            ((44, 52), ((0, 0),), 1), ((29, 20), ((0, 0), (0, 1)), 1)]
# The moon's centre (it faces south) and radius.
MOON = (CX, STAND + 44, -24)
MOON_RADIUS = 13
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
BOUNDS = 40
FLOOR = SOIL - 3
# The hidden lights: a lattice LIGHT_SPACING apart (every other row shifted half), LIGHT_Y high, at LIGHT_LEVEL, over the
# clearing; and these besides (x, y, z, level): in the camp and the glades.
LIGHT_Y = STAND + 2
LIGHT_LEVEL = 15
LIGHT_SPACING = 7
LIGHTS = [(35, STAND + 2, 63, 11), (40, STAND + 2, 68, 11)]
RING_LIGHT = 7                     # the least block light on any cell where anyone stands in the clearing or a bell
                                   # clearing (the audit, and the client test against the game's own light)


# ---------------------------------------------------------------- states

DIRT = block("minecraft:dirt")
COARSE = block("minecraft:coarse_dirt")
PODZOL = block("minecraft:podzol", snowy=False)
ROOTED = block("minecraft:rooted_dirt")
MOSS = block("minecraft:moss_block")
PATH_BLOCK = block("minecraft:dirt_path")
STONE_BLOCK = block("minecraft:stone")
ANDESITE = block("minecraft:andesite")
MOSSY = block("minecraft:mossy_cobblestone")
COBBLE = block("minecraft:cobblestone")
LEAVES = block("jugcraft:moonwood_leaves")
WOOL = block("minecraft:brown_wool")
PLANKS = block("minecraft:spruce_planks")
EXIT_MIST = block("jugcraft:lair_exit", axis="x")
SHORT_GRASS = block("minecraft:short_grass")
FERN = block("minecraft:fern")
MOSS_CARPET = block("minecraft:moss_carpet")


def log(name, axis):
    return block(f"minecraft:{name}", axis=axis)


def stairs(name, facing, half="bottom"):
    return block(f"minecraft:{name}_stairs", facing=facing, half=half, shape="straight", waterlogged=False)


def slab(name, kind):
    return block(f"minecraft:{name}_slab", type=kind, waterlogged=False)


def fence():
    return block("minecraft:spruce_fence", north=False, south=False, east=False, west=False, waterlogged=False)


def campfire():
    return block("minecraft:campfire", facing="north", lit=False, signal_fire=False, waterlogged=False)


def lantern():
    return block("minecraft:lantern", hanging=False, waterlogged=False)


def rug(facing):
    return block("jugcraft:werewolf_rug", facing=facing)


def bell(facing, ringing=False):
    return block("jugcraft:warden_bell", facing=facing, ringing=str(ringing).lower())


def light(level):
    return block("minecraft:light", level=level, waterlogged=False)


# ---------------------------------------------------------------- shapes

def distance(x, z):
    """How far the middle of cell (x, z) is from the centre."""
    return math.hypot(x + 0.5 - CENTRE[0], z + 0.5 - CENTRE[1])


def in_ring(x, z):
    return distance(x, z) <= RING


def on_path(x, z):
    return PATH[0] <= distance(x, z) <= PATH[1]


def stone_height(x, z):
    """How high the Howling Stone stands in cell (x, z): 0 off it."""
    d = distance(x, z)
    for reach, height in STONE:
        if d <= reach:
            return height
    return 0


def on_spoke(x, z):
    """Whether (x, z) lies on one of the four spokes: within a cell of an axis through the centre cell, from SPOKE_FROM
    out to the ring's edge."""
    return (abs(x - CX) <= 1 or abs(z - CZ) <= 1) and SPOKE_FROM <= distance(x, z) <= RING


def ways():
    """The ways out from the centre: each bell's, and the camp's (south)."""
    return {**{name: info["way"] for name, info in BELLS.items()}, "camp": (0, 1)}


def in_passage(x, z):
    """In a passage: within PASSAGE_HALF of a way's middle line, from the ring's edge out to its glade (or the camp)."""
    for name, (wx, wz) in ways().items():
        along = (x - CX) * wx + (z - CZ) * wz
        across = abs((x - CX) * wz - (z - CZ) * wx)
        if across <= PASSAGE_HALF and RING - 2 <= along <= (GLADE_AT if name != "camp" else CAMP["z"][0] - CZ):
            return True
    return False


def glade_middle(name):
    wx, wz = BELLS[name]["way"]
    return CENTRE[0] + wx * GLADE_AT, CENTRE[1] + wz * GLADE_AT


def in_glade(x, z):
    for name in BELLS:
        gx, gz = glade_middle(name)
        if math.hypot(x + 0.5 - gx, z + 0.5 - gz) <= GLADE_RADIUS:
            return True
    return False


def in_camp(x, z):
    return CAMP["x"][0] <= x <= CAMP["x"][1] and CAMP["z"][0] <= z <= CAMP["z"][1]


def in_trail(x, z):
    return TRAIL["x"][0] <= x <= TRAIL["x"][1] and TRAIL["z"][0] <= z <= TRAIL["z"][1]


def open_floor(x, z):
    """Every cell of the floor anyone can walk: the clearing, the passages, the glades, the camp and its trail."""
    return in_ring(x, z) or in_passage(x, z) or in_glade(x, z) or in_camp(x, z) or in_trail(x, z)


def bell_cell(name):
    """The cell the bell hangs in: (x, y, z)."""
    wx, wz = BELLS[name]["way"]
    return CX + wx * BELL_AT, BELL_Y, CZ + wz * BELL_AT


def bell_cells():
    return {name: bell_cell(name) for name in BELLS}


def frame_posts(name):
    """The frame's two posts, either side of the bell across the way: (x, z) of each."""
    x, _, z = bell_cell(name)
    wx, wz = BELLS[name]["way"]
    return [(x + wz, z + wx), (x - wz, z - wx)]


def log_cells():
    cells = {}
    for x, z, axis, length in LOGS:
        for i in range(length):
            cells[(x + i, z) if axis == "x" else (x, z + i)] = axis
    return cells


def boulder_cells():
    cells = {}
    for (x, z), shape, height in BOULDERS:
        for dx, dz in shape:
            cells[(x + dx, z + dz)] = height
    return cells


def cover_cells():
    return set(log_cells()) | set(boulder_cells())


def trunks():
    """The trunks' middles and radii: (x, z, radius), each clear of the open floor."""
    found = []
    for radius, count, first, size in TRUNK_RINGS:
        for i in range(count):
            angle = math.radians(first + i * 360.0 / count)
            tx, tz = CENTRE[0] + math.sin(angle) * radius, CENTRE[1] - math.cos(angle) * radius
            cells = [(x, z) for x in range(int(tx - size) - 1, int(tx + size) + 2)
                     for z in range(int(tz - size) - 1, int(tz + size) + 2) if math.hypot(x + 0.5 - tx, z + 0.5 - tz) <= size]
            if cells and not any(open_floor(x, z) for x, z in cells) and all(0 <= x < SIZE[0] and 0 <= z < SIZE[2] for x, z in cells):
                found.append((tx, tz, size))
    return found


def trunk_cells():
    cells = set()
    for tx, tz, size in trunks():
        for x in range(int(tx - size) - 1, int(tx + size) + 2):
            for z in range(int(tz - size) - 1, int(tz + size) + 2):
                if math.hypot(x + 0.5 - tx, z + 0.5 - tz) <= size:
                    cells.add((x, z))
    return cells


# ---------------------------------------------------------------- building

def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    floor(put)
    wood(put)
    stone(put)
    cover(put)
    plants(put, blocks)
    for name in BELLS:
        bell_frame(put, name)
    camp(put)
    exit_mist(put)
    lights(put, blocks)
    return hollow(blocks)


def floor(put):
    """The forest floor: dirt under a top of moss in the clearing, trodden earth on the paths and spokes, and podzol,
    coarse and rooted dirt under the trees, in the passages, glades and camp."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            for y in range(SOIL):
                put(x, y, z, DIRT)
            h = _hash(x, z, 3)
            if in_ring(x, z):
                top = PATH_BLOCK if on_path(x, z) or on_spoke(x, z) else PODZOL if h < 0.06 else MOSS
            elif in_trail(x, z) or in_passage(x, z) and any(abs(c) <= 1 for c in ((x - CX), (z - CZ))):
                top = PATH_BLOCK
            else:
                top = COARSE if h < 0.3 else ROOTED if h < 0.45 else PODZOL
            put(x, SOIL, z, top)


def wood(put):
    """The trees: trunks from the floor up into the canopy; the thicket of Moonwood Leaves between them, off the open
    floor; and the canopy over everything farther than CANOPY_INNER (ragged) from the centre."""
    stems = trunk_cells()
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if (x, z) in stems:
                for y in range(STAND, TRUNK_TOP + 1):
                    put(x, y, z, log("dark_oak_log", "y"))
            elif not open_floor(x, z):
                for y in range(STAND, THICKET_TOP + 1):
                    put(x, y, z, LEAVES)
            d = distance(x, z)
            if d >= CANOPY_INNER + CANOPY_RAG * _hash(x, z, 11):
                low = CANOPY_LOW + (1 if _hash(x, z, 13) < 0.3 else 0)
                high = CANOPY_HIGH - (1 if _hash(x, z, 17) < 0.35 else 0)
                for y in range(low, high + 1):
                    if (x, z) not in stems or y > TRUNK_TOP:
                        put(x, y, z, LEAVES)


def stone(put):
    """The Howling Stone: stone, andesite and mossy cobblestone, stepped up to its core."""
    for x in range(CX - 5, CX + 6):
        for z in range(CZ - 5, CZ + 6):
            height = stone_height(x, z)
            for y in range(STAND, STAND + height):
                h = _hash(x, y, z)
                put(x, y, z, MOSSY if y == STAND + height - 1 and h < 0.5 else ANDESITE if h < 0.3 else STONE_BLOCK)


def cover(put):
    """The fallen logs (oak, moss on their tops) and the boulders."""
    for (x, z), axis in log_cells().items():
        put(x, STAND, z, log("oak_log", axis))
    for (x, z), height in boulder_cells().items():
        for y in range(STAND, STAND + height):
            put(x, y, z, MOSSY if _hash(x, y, z) < 0.4 else ANDESITE)


def plants(put, blocks):
    """Ferns and grass on the clearing's moss (off the paths, the spokes, the stone and the cover), and in the glades."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if blocks.get((x, SOIL, z)) != MOSS and not (in_glade(x, z) and blocks.get((x, SOIL, z)) in (PODZOL, COARSE)):
                continue
            if (x, STAND, z) in blocks or stone_height(x, z) or distance(x, z) < SPOKE_FROM + 1:
                continue
            h = _hash(x, z, 23)
            if h < 0.13:
                put(x, STAND, z, SHORT_GRASS)
            elif h < 0.19:
                put(x, STAND, z, FERN)
    for (x, z), axis in log_cells().items():
        if _hash(x, z, 29) < 0.6:
            put(x, STAND + 1, z, MOSS_CARPET)


def bell_frame(put, name):
    """A Warden Bell's frame: spruce log posts either side of the bell, a beam across them over it, a gabled roof of
    spruce stairs over that with a slab along its ridge; the bell hangs from the beam, facing the ring."""
    x, y, z = bell_cell(name)
    wx, wz = BELLS[name]["way"]
    across = "z" if wx else "x"
    for px, pz in frame_posts(name):
        for py in range(STAND, BEAM_Y):
            put(px, py, pz, log("spruce_log", "y"))
    for i in (-1, 0, 1):
        put(x + wz * i, BEAM_Y, z + wx * i, log("spruce_log", across))
        # The roof: a stair either side along the way, rising to the ridge over the beam.
        for side in (-1, 1):
            facing = {(1, 0): "west", (-1, 0): "east", (0, 1): "north", (0, -1): "south"}[(wx * side, wz * side)]
            put(x + wz * i + wx * side, ROOF_Y, z + wx * i + wz * side, stairs("spruce", facing))
        put(x + wz * i, ROOF_Y, z + wx * i, slab("spruce", "bottom"))
    put(x, y, z, bell(BELLS[name]["face"]))


def camp(put):
    """The Hunters' Camp: the lean-to of hides (a wall of brown wool at its back, spruce posts at its front corners, a roof
    of spruce slabs), the cold firepit ringed with cobblestone slabs, the werewolf rugs, and a stump with a lantern."""
    (x0, x1), (z0, z1) = LEAN_TO["x"], LEAN_TO["z"]
    for z in range(z0, z1 + 1):
        for y in (STAND, STAND + 1):
            put(x0, y, z, WOOL)
        for x in range(x0, x1 + 1):
            put(x, STAND + 2, z, slab("spruce", "bottom"))
    for z in (z0, z1):
        for y in (STAND, STAND + 1):
            put(x1, y, z, fence())
    fx, fz = FIREPIT
    put(fx, STAND, fz, campfire())
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        put(fx + dx, STAND, fz + dz, slab("cobblestone", "bottom"))
    for x, z, facing in RUGS:
        put(x, STAND, z, rug(facing))
    sx, sz = STUMP
    put(sx, STAND, sz, log("stripped_spruce_log", "y"))
    put(sx, STAND + 1, sz, lantern())


def exit_mist(put):
    """The Grey Mist at the end of the trail behind the camp, under the trees."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    for x in range(x0, x1 + 1):
        for y in range(STAND, STAND + 3):
            put(x, y, z, EXIT_MIST)


def stone_exit_cells():
    """Where the Grey Mist opens on the Howling Stone when the Alpha falls."""
    (x0, x1), z, y = STONE_EXIT["x"], STONE_EXIT["z"], STONE_EXIT["y"]
    return [(x, yy, z) for yy in (y, y + 1) for x in range(x0, x1 + 1)]


def exit_cells():
    """Where the Grey Mist is (for the audit and the tests)."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(STAND, STAND + 3) for x in range(x0, x1 + 1)]


def lights(put, blocks):
    """Hidden lights over the clearing (a staggered lattice, a light moved to the next free cell when its own is taken),
    in the glades' middles and in the camp."""
    for row, z in enumerate(range(CZ - int(RING), CZ + int(RING) + 1, LIGHT_SPACING // 2 + 1)):
        for x in range(CX - int(RING) + (LIGHT_SPACING // 2 if row % 2 else 0), CX + int(RING) + 1, LIGHT_SPACING):
            for cx, cz in ((x, z), (x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                if in_ring(cx, cz) and (cx, LIGHT_Y, cz) not in blocks:
                    put(cx, LIGHT_Y, cz, light(LIGHT_LEVEL))
                    break
    for name in BELLS:
        gx, gz = glade_middle(name)
        put(int(gx), LIGHT_Y, int(gz), light(LIGHT_LEVEL))
    for x, y, z, level in LIGHTS:
        if (x, y, z) not in blocks:
            put(x, y, z, light(level))


def hollow(blocks):
    """Leaves out blocks nobody can see: those whose six neighbours are all whole, opaque blocks of the template; but none
    over the floor of the clearing or a glade, so no pocket of air is left inside the Howling Stone or a boulder, where the
    game would see dark floor."""
    opaque = {DIRT, COARSE, PODZOL, ROOTED, MOSS, STONE_BLOCK, ANDESITE, MOSSY, COBBLE}
    out = {}
    for (x, y, z), state in blocks.items():
        if y >= STAND and (in_ring(x, z) or in_glade(x, z)):
            out[(x, y, z)] = state
            continue
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
    print(SIZE, len(build()), "blocks,", len(trunks()), "trunks")
