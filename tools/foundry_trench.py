"""The Foundry Trench (docs/features/foundry-trench.md): the Iron Dreadnought's lair, written as the structure template
data/jugcraft/structure/lair/foundry_trench.nbt, which lair/Lairs places into every instance of the
jugcraft:foundry_trench dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI
checks it is current).

A ruined test pit sunk behind the front line, open to a sky of drifting smoke: a slab of churned ground about 66 blocks
across (x) and 70 long (z) hanging in the void, the pit sunk in its middle. North is low z.
- the pit, the arena (PIT): 47 blocks by 41 of blast-scorched test-bed concrete, eight blocks below the ground, walled
  with reinforced concrete (girder pilasters, blast doors);
- the three pylons (PYLONS): grounded discharge towers at the pit's west, east and south, each a lattice of steel on a
  concrete footing with a tesla crown at its top (the lair-only pylon_lattice and pylon_crown). At the foot of each a
  breaker (the lair-only discharge_breaker, a knife switch on a steel cabinet) faces the pit, and before it a copper
  ground terminal (the lair-only ground_terminal) is set in the floor. A lane of bare concrete, a hazard stripe down its
  middle, runs from the pit's centre to each terminal (LANES); nothing stands on them;
- the cover (BASTIONS, WRECK): three concrete bastions two high in three of the pit's quarters, and in the fourth the
  burnt-out hull of a landship on a rail spur: permanent cover, which stops the Dreadnought's rivets and stuns him if he
  charges into it;
- the two service gantries (CATWALKS): steel catwalks along the west and east walls, five blocks up, with ladders at
  both ends;
- the assembly gantry (GANTRY): a recess in the pit's north wall with a steel cradle and a crane beam over it, where
  the Dreadnought stands dormant (WAIT);
- the fire trench (TRENCH): along the pit's south rim, sandbags and timber revetments, duckboards underfoot; a ramp of
  sandbag steps (RAMP) goes down from its west end into the pit's south-west corner;
- the dugout (DUGOUT): off the trench's south side, two steps down, shored with timber under a corrugated iron roof,
  bunks, a lamp, a map table and the telephone the orders came by; the arrival (ARRIVAL), and in its back doorway Grey
  Mist (EXIT): the way home;
- round it: barbed wire along the pit's rim, searchlights at its corners, smoke stacks leaning over the far side.
The light is hidden light blocks, the searchlights and the bunker lamps.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written (the lair-only blocks' are written with them); vanilla blocks use only properties they have had for
years. Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/foundry_trench"

SIZE = (72, 24, 76)
PAVE = 4                           # the pit's paving layer (its concrete)
STAND = PAVE + 1                   # where anyone in the pit stands
GROUND = STAND + 7                 # the ground's top layer round the pit (the rim): anyone on it stands at GROUND + 1
PIT = {"x": (12, 58), "z": (16, 56)}    # the pit's open cells (inclusive)
CENTRE = (35.5, 36.5)              # the pit's middle: the middle of cell (35, 36)
# The slab of ground the pit is sunk in: its outer cells (inclusive), solid but where the pit, the trench and the
# dugout are carved out of it.
EARTH = {"x": (4, 67), "y": (0, GROUND), "z": (3, 73)}
# The three pylons, each by the centre cell of its 3 x 3 footing; its breaker stands against the footing's pit side and
# faces the pit, its ground terminal is set in the floor in front of the breaker, and its lane runs from the terminal to
# the pit's centre. Each: footing centre (x, z), breaker cell (x, z), the way the breaker faces, terminal cell (x, z).
PYLONS = {
    "west": {"at": (16, 36), "breaker": (18, 36), "face": "east", "terminal": (19, 36)},
    "east": {"at": (55, 36), "breaker": (53, 36), "face": "west", "terminal": (52, 36)},
    "south": {"at": (35, 53), "breaker": (35, 51), "face": "north", "terminal": (35, 50)},
}
LATTICE = 9                        # the lattice's height over the footing; the crown sits on its top
# The lanes: three cells wide (the middle and one either side), from the pit's centre out to each terminal. Each is
# (axis, fixed, lo, hi): along x at z = fixed, or along z at x = fixed, from lo to hi inclusive.
LANES = {
    "west": ("x", 36, 20, 34),
    "east": ("x", 36, 36, 51),
    "south": ("z", 35, 37, 49),
}
# The cover: concrete bastions, two high and three by three (x, z of each one's north-west cell), and the landship's hull
# (its cells, inclusive, three high) on the rail spur that runs from it to the east wall (RAIL: z, x from, x to).
BASTIONS = [(22, 23), (46, 23), (22, 46)]
BASTION_HEIGHT = 2
WRECK = {"x": (44, 50), "z": (44, 48), "height": 3}
RAIL = (47, 51, 58)
# The service gantries: catwalk floors (top slabs) five blocks up along the side walls, from z_lo to z_hi; a railing on
# their pit side, posts under them, and ladders against the wall at either end.
CATWALK_Y = STAND + 4              # the catwalk's slab layer (top half): anyone on it stands at CATWALK_Y + 1
CATWALKS = {"west": {"x": (12, 13), "rail": 14, "ladder": 12, "face": "east"},
            "east": {"x": (57, 58), "rail": 56, "ladder": 58, "face": "west"}}
CATWALK_Z = (22, 50)
LADDER_Z = (21, 51)
POSTS_Z = (22, 28, 34, 40, 46, 50)
# The assembly gantry: a recess in the north wall (its open cells), the cradle's two columns at its mouth and a crane
# beam across the top, and where the Dreadnought waits (part 2): x, y, z and yaw (facing south).
GANTRY = {"x": (26, 44), "z": (5, 15)}
GANTRY_BEAM = GROUND + 3
WAIT = (35.5, STAND, 9.5, 0.0)
# Where the Grey Mist opens in the gantry when the Dreadnought falls: two high from STAND.
GANTRY_EXIT = {"x": (34, 36), "z": 6}
# The fire trench along the pit's south rim, its floor two below the ground; the ramp of sandbag stairs from its west
# end down into the pit's south-west corner, a block lower each row north from the gap in the pit's south wall (z 57),
# whose step is level with the trench.
TRENCH = {"x": (12, 33), "z": (58, 60)}
TRENCH_STAND = GROUND - 1          # the trench's floor: duckboards laid on the earth at this layer
DUCK = 2.5 / 16                    # a duckboard's height: anyone on one stands this much over its layer
RAMP = {"x": (12, 14), "z": (52, 57)}
# The dugout off the trench's south side: its steps down (STEPS rows, one lower each), its room and its roof.
DUGOUT_STEPS = {"x": (21, 23), "z": (61, 62)}
DUGOUT = {"x": (17, 27), "z": (63, 69)}
DUGOUT_STAND = TRENCH_STAND - 2   # the dugout's floor: duckboards, like the trench's
DUGOUT_ROOF = DUGOUT_STAND + 4
ARRIVAL = (22.5, DUGOUT_STAND + DUCK, 65.5, 180.0)  # x, y, z and yaw: on the dugout's duckboards, facing north up the steps
EXIT = {"x": (21, 23), "z": 70}               # the Grey Mist's columns and row (the back doorway), three high
TELEPHONE = (25, DUGOUT_STAND + 1, 64)        # the dugout's telephone, on the map table
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
BOUNDS = 40
FLOOR = PAVE - 3
# Searchlights at the pit's four corners on the rim, each on a pile of sandbags, and smoke stacks beyond the north rim.
SEARCHLIGHTS = [(10, 13, 6), (61, 13, 10), (10, 59, 2), (61, 59, 14)]   # x, z, yaw (sixteenths of a turn)
STACKS = [(8, 8), (63, 7), (52, 5)]
STACK_TOP = GROUND + 8
# The hidden lights: a lattice at each of these heights (y, level, spacing, staggered) over the pit's open cells, and
# these: (x, y, z, level).
LIGHT_LAYERS = ((STAND + 2, 14, 8, True),)
LIGHTS = [
    (20, DUGOUT_STAND + 2, 66, 12), (25, DUGOUT_STAND + 2, 66, 12),  # in the dugout
    (16, TRENCH_STAND + 2, 59, 11), (27, TRENCH_STAND + 2, 59, 11),  # in the fire trench
    (30, STAND + 3, 9, 12), (40, STAND + 3, 9, 12),                  # in the gantry
]
PIT_LIGHT = 7                      # the least block light on any cell where anyone stands in the pit (the audit)
# Old blast marks on the test bed, where shells and charges were proved: (x, z, radius). Clear of the lanes and cover.
SCORCHES = [(21.5, 30.5, 2.4), (41.5, 28.5, 2.8), (28.5, 47.5, 2.0), (50.5, 40.5, 2.2), (28.5, 19.5, 1.7),
            (43.5, 53.5, 1.8)]


# ---------------------------------------------------------------- states

DIRT = block("minecraft:dirt")
COARSE = block("minecraft:coarse_dirt")
MUD = block("minecraft:mud")
GRAVEL = block("minecraft:gravel")
CONCRETE = block("minecraft:gray_concrete")
LIGHT_CONCRETE = block("minecraft:light_gray_concrete")
SCORCH = block("minecraft:black_concrete")
BASTION = block("jugcraft:bastion_concrete")
HAZARD = block("jugcraft:hazard_plating")
PLATE = block("jugcraft:riveted_black_plate")
ARMOUR = block("jugcraft:steel_armor_plate")
GRATING = block("jugcraft:rust_grating_slab", type="top")
SANDBAGS = block("jugcraft:sandbags")
REVETMENT = block("jugcraft:timber_revetment")
DUCKBOARD = block("jugcraft:duckboard")
CORRUGATED = block("jugcraft:corrugated_iron")
WIRE = block("jugcraft:barbed_wire")
BRICKS = block("minecraft:bricks")
EXIT_MIST = block("jugcraft:lair_exit", axis="x")


def girder(axis):
    return block("jugcraft:steel_girder", axis=axis)


def beam(axis):
    return block("jugcraft:steel_i_beam", axis=axis)


def shoring(axis):
    return block("jugcraft:timber_shoring", axis=axis)


def sandbag_stairs(facing):
    return block("jugcraft:sandbags_stairs", facing=facing, half="bottom", shape="straight")


def ladder(facing):
    return block("jugcraft:steel_ladder", facing=facing)


def railing(**sides):
    return block("jugcraft:railing", **{side: "true" for side in sides})


def blast_door(facing, half):
    return block("jugcraft:blast_door", facing=facing, half=half, hinge="left", open="false")


def bunk(facing, half):
    return block("jugcraft:bunker_bunk", facing=facing, half=half)


def periscope(facing, half):
    return block("jugcraft:trench_periscope", facing=facing, half=half)


def lamp(hanging):
    return block("jugcraft:bunker_lamp", hanging=str(hanging).lower())


def map_table(facing):
    return block("jugcraft:map_table", facing=facing)


def telephone(facing):
    return block("jugcraft:field_telephone", facing=facing)


def searchlight(yaw):
    return block("jugcraft:searchlight", yaw=str(yaw), tilt="1", lit="true")


def rail(shape):
    return block("minecraft:rail", shape=shape, waterlogged=False)


def chain():
    return block("minecraft:iron_chain", axis="y", waterlogged=False)


def campfire():
    return block("minecraft:campfire", facing="north", lit=True, signal_fire=True, waterlogged=False)


def lattice():
    return block("jugcraft:pylon_lattice")


def crown(live=False):
    return block("jugcraft:pylon_crown", live=str(live).lower())


def breaker(face, thrown=False, live=False, fouled=False):
    return block("jugcraft:discharge_breaker", facing=face, thrown=str(thrown).lower(), live=str(live).lower(),
                 fouled=str(fouled).lower())


def terminal(live=False):
    return block("jugcraft:ground_terminal", live=str(live).lower())


def light(level):
    return block("minecraft:light", level=level, waterlogged=False)


# ---------------------------------------------------------------- shapes

def in_pit(x, z):
    return PIT["x"][0] <= x <= PIT["x"][1] and PIT["z"][0] <= z <= PIT["z"][1]


def in_ramp(x, z):
    return RAMP["x"][0] <= x <= RAMP["x"][1] and RAMP["z"][0] <= z <= RAMP["z"][1]


def ramp_stand(z):
    """The top of the ramp's stair in row z (its high, south half): TRENCH_STAND in the gap (z 57), a block lower each row
    north, STAND + 1 in its first row, whose low half is half a block over the pit's floor."""
    return STAND + 1 + (z - RAMP["z"][0])


def lane_cells(name):
    axis, fixed, lo, hi = LANES[name]
    if axis == "x":
        return {(x, z) for x in range(lo, hi + 1) for z in range(fixed - 1, fixed + 2)}
    return {(x, z) for x in range(fixed - 1, fixed + 2) for z in range(lo, hi + 1)}


def all_lane_cells():
    return set().union(*(lane_cells(name) for name in LANES))


def on_lane_line(x, z):
    """The middle line of a lane, laid with hazard plating."""
    for axis, fixed, lo, hi in LANES.values():
        if axis == "x" and z == fixed and lo <= x <= hi:
            return True
        if axis == "z" and x == fixed and lo <= z <= hi:
            return True
    return False


def footing_cells(name):
    x, z = PYLONS[name]["at"]
    return {(x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)}


def bastion_cells():
    return {(x + dx, z + dz) for x, z in BASTIONS for dx in range(3) for dz in range(3)}


def wreck_cells():
    return {(x, z) for x in range(WRECK["x"][0], WRECK["x"][1] + 1) for z in range(WRECK["z"][0], WRECK["z"][1] + 1)}


def cover_cells():
    """Every pit cell taken by cover: the bastions and the wreck."""
    return bastion_cells() | wreck_cells()


def in_trench(x, z):
    return TRENCH["x"][0] <= x <= TRENCH["x"][1] and TRENCH["z"][0] <= z <= TRENCH["z"][1]


def in_gantry(x, z):
    return GANTRY["x"][0] <= x <= GANTRY["x"][1] and GANTRY["z"][0] <= z <= GANTRY["z"][1]


def in_dugout(x, z):
    return DUGOUT["x"][0] <= x <= DUGOUT["x"][1] and DUGOUT["z"][0] <= z <= DUGOUT["z"][1]


# ---------------------------------------------------------------- building

def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    def clear(x, y, z):
        blocks.pop((x, y, z), None)

    earth(put)
    pit(put, clear)
    walls(put, blocks)
    gantry(put, clear)
    pylons(put)
    cover(put)
    catwalks(put)
    trench(put, clear)
    dugout(put, clear)
    rim(put)
    exit_mist(put)
    for y, level, spacing, staggered in LIGHT_LAYERS:
        # Rows from wall to wall: every other row (staggered) shifted half a spacing, so the lights reach both walls.
        # A row whose last light falls more than half a spacing short of the far wall gets one more against it.
        for row, z in enumerate(range(PIT["z"][0], PIT["z"][1] + 1, spacing // 2 if staggered else spacing)):
            xs = list(range(PIT["x"][0] + (spacing // 2 if staggered and row % 2 == 0 else 0), PIT["x"][1] + 1, spacing))
            if PIT["x"][1] - xs[-1] > spacing // 2:
                xs.append(PIT["x"][1])
            for x in xs:
                # A light whose cell is filled (a pylon, cover) hangs in the first free cell beside it instead.
                for cx, cz in ((x, z), (x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                    if in_pit(cx, cz) and (cx, y, cz) not in blocks:
                        put(cx, y, cz, light(level))
                        break
    for x, y, z, level in LIGHTS:
        if (x, y, z) not in blocks:
            put(x, y, z, light(level))
    return hollow(blocks)


def earth(put):
    """The slab of ground: dirt under a churned top of coarse dirt, mud and gravel."""
    for x in range(EARTH["x"][0], EARTH["x"][1] + 1):
        for z in range(EARTH["z"][0], EARTH["z"][1] + 1):
            for y in range(EARTH["y"][0], GROUND):
                put(x, y, z, DIRT)
            h = _hash(x, z, 3)
            put(x, GROUND, z, MUD if h < 0.16 else GRAVEL if h < 0.24 else COARSE)


def pit(put, clear):
    """The pit: its floor of scorched test-bed concrete, the lanes and the terminals in it, open to the sky."""
    lanes = all_lane_cells()
    terminals = {p["terminal"] for p in PYLONS.values()}
    for x in range(PIT["x"][0], PIT["x"][1] + 1):
        for z in range(PIT["z"][0], PIT["z"][1] + 1):
            if (x, z) in terminals:
                put(x, PAVE, z, terminal())
            elif on_lane_line(x, z):
                put(x, PAVE, z, HAZARD)
            elif (x, z) in lanes:
                put(x, PAVE, z, LIGHT_CONCRETE)
            else:
                h = _hash(x, z, 5)
                put(x, PAVE, z, SCORCH if scorched(x, z) else LIGHT_CONCRETE if h < 0.12 else CONCRETE)
            for y in range(STAND, GROUND + 1):
                clear(x, y, z)


def scorched(x, z):
    """Inside a blast mark: its edge ragged by up to half a block either way."""
    return any(math.hypot(x + 0.5 - cx, z + 0.5 - cz) < r + (_hash(x, z, 9) - 0.5) for cx, cz, r in SCORCHES)


def walls(put, blocks):
    """The pit's walls of reinforced concrete, from the floor to the rim: a girder pilaster every sixth cell, and a blast
    door (shut) in the middle of each side wall."""
    for z in range(PIT["z"][0], PIT["z"][1] + 1):
        for x, face in ((PIT["x"][0] - 1, "east"), (PIT["x"][1] + 1, "west")):
            for y in range(STAND, GROUND + 1):
                put(x, y, z, girder("y") if (z - PIT["z"][0]) % 6 == 3 and y < GROUND else BASTION)
    for x in range(PIT["x"][0] - 1, PIT["x"][1] + 2):
        for z in (PIT["z"][0] - 1, PIT["z"][1] + 1):
            if (x, STAND, z) in blocks and not in_gantry(x, z):
                for y in range(STAND, GROUND + 1):
                    put(x, y, z, girder("y") if (x - PIT["x"][0]) % 6 == 3 and y < GROUND else BASTION)
    for x, face in ((PIT["x"][0] - 1, "east"), (PIT["x"][1] + 1, "west")):
        put(x, STAND, 30, blast_door(face, "lower"))
        put(x, STAND + 1, 30, blast_door(face, "upper"))
        put(x, STAND, 44, blast_door(face, "lower"))
        put(x, STAND + 1, 44, blast_door(face, "upper"))


def gantry(put, clear):
    """The assembly gantry: a recess in the north wall floored with concrete, open to the sky, walled with riveted plate;
    two girder columns at its mouth and a crane beam across them, chains hanging from it; the cradle's back frame."""
    (x0, x1), (z0, z1) = GANTRY["x"], GANTRY["z"]
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 1):
            inside = in_gantry(x, z)
            if inside:
                put(x, PAVE, z, HAZARD if z == z1 else CONCRETE)
                for y in range(STAND, GROUND + 1):
                    clear(x, y, z)
            else:
                for y in range(STAND, GROUND + 1):
                    put(x, y, z, PLATE)
    for x in (x0, x1):
        for y in range(STAND, GANTRY_BEAM):
            put(x, y, z1, girder("y"))
    for x in range(x0, x1 + 1):
        put(x, GANTRY_BEAM, z1, beam("x"))
    for z in range(z0, z1):
        for x in (x0, x1):
            put(x, GANTRY_BEAM, z, beam("z"))
    for x in (30, 40):
        for y in range(GROUND - 2, GANTRY_BEAM):
            put(x, y, z1, chain())
    # The cradle's back frame: girder posts against the back wall, a cross beam at the rim.
    for x in (29, 35, 41):
        for y in range(STAND, GROUND):
            put(x, y, z0, girder("y"))
    for x in range(x0 + 1, x1):
        put(x, GROUND, z0, beam("x"))


def pylons(put):
    """Each pylon: a 3 x 3 footing of concrete, the lattice from it LATTICE high, the crown on top; its breaker against the
    footing's pit side, facing the pit (the terminal is laid with the floor)."""
    for name, info in PYLONS.items():
        x, z = info["at"]
        for cx, cz in footing_cells(name):
            put(cx, STAND, cz, BASTION)
        for y in range(STAND + 1, STAND + 1 + LATTICE):
            put(x, y, z, lattice())
        put(x, STAND + 1 + LATTICE, z, crown())
        bx, bz = info["breaker"]
        put(bx, STAND, bz, breaker(info["face"]))


def cover(put):
    """The bastions (concrete, a parapet of plate along their tops' rims) and the landship's hull: plate, its armoured
    sponsons and turret, on the rail spur that runs east from it to the wall."""
    for x, z in BASTIONS:
        for dx in range(3):
            for dz in range(3):
                for y in range(STAND, STAND + BASTION_HEIGHT):
                    put(x + dx, y, z + dz, BASTION)
    (x0, x1), (z0, z1) = WRECK["x"], WRECK["z"]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(STAND, STAND + WRECK["height"]):
                edge = z in (z0, z1) or x in (x0, x1)
                put(x, y, z, ARMOUR if edge and y == STAND + 1 else PLATE)
    # The turret, burnt open: a ring of armour plate on the hull's top.
    for dx, dz in ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)):
        put((x0 + x1) // 2 + dx, STAND + WRECK["height"], (z0 + z1) // 2 + dz, ARMOUR)
    z, xa, xb = RAIL
    for x in range(xa, xb + 1):
        put(x, STAND, z, rail("east_west"))


def catwalks(put):
    """The service gantries: catwalk slabs CATWALK_Y high along the side walls, railings on their pit side, girder posts
    under them, and ladders against the wall at either end."""
    for side in CATWALKS.values():
        for z in range(CATWALK_Z[0], CATWALK_Z[1] + 1):
            for x in range(side["x"][0], side["x"][1] + 1):
                put(x, CATWALK_Y, z, GRATING)
            put(side["rail"], CATWALK_Y + 1, z, railing(north=True, south=True))
        for z in POSTS_Z:
            for y in range(STAND, CATWALK_Y):
                put(side["rail"] - (1 if side["face"] == "east" else -1), y, z, girder("y"))
        for z in LADDER_Z:
            for y in range(STAND, CATWALK_Y + 1):
                put(side["ladder"], y, z, ladder(side["face"]))


def trench(put, clear):
    """The fire trench along the south rim: duckboards on the earth a block below the ground, revetments on its south side,
    sandbags on its parapet over the pit; the gap at its west end and the ramp of sandbag steps down into the pit."""
    (x0, x1), (z0, z1) = TRENCH["x"], TRENCH["z"]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            put(x, TRENCH_STAND, z, DUCKBOARD)
            for y in range(TRENCH_STAND + 1, GROUND + 3):
                clear(x, y, z)
        for y in range(TRENCH_STAND, GROUND + 1):
            put(x, y, z1 + 1, REVETMENT)
        put(x, GROUND + 1, z0 - 1, SANDBAGS)
    # A periscope over the parapet, midway along.
    put(24, TRENCH_STAND, z0, periscope("north", "lower"))
    put(24, TRENCH_STAND + 1, z0, periscope("north", "upper"))
    # The ramp: a row of sandbag stairs rising south each row, from the pit's floor to the gap in its south wall (z 57),
    # whose stair's top is level with the trench.
    (rx0, rx1), (rz0, rz1) = RAMP["x"], RAMP["z"]
    for z in range(rz0, rz1 + 1):
        step = ramp_stand(z) - 1
        for x in range(rx0, rx1 + 1):
            for y in range(STAND, step):
                put(x, y, z, SANDBAGS)
            put(x, step, z, sandbag_stairs("south"))
            for y in range(step + 1, GROUND + 3):
                clear(x, y, z)


def dugout(put, clear):
    """The dugout: its steps down from the trench, the room shored with timber under a corrugated iron roof and sandbags,
    bunks, a lamp, the map table and the telephone on it."""
    (sx0, sx1), (sz0, sz1) = DUGOUT_STEPS["x"], DUGOUT_STEPS["z"]
    for i, z in enumerate(range(sz0, sz1 + 1)):
        step = TRENCH_STAND - 1 - i       # a stair rising north: its high half level with the row before it
        for x in range(sx0, sx1 + 1):
            for y in range(DUGOUT_STAND - 1, step):
                put(x, y, z, SANDBAGS)
            put(x, step, z, sandbag_stairs("north"))
            for y in range(step + 1, DUGOUT_ROOF):
                clear(x, y, z)
            put(x, DUGOUT_ROOF, z, CORRUGATED)
    (x0, x1), (z0, z1) = DUGOUT["x"], DUGOUT["z"]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            put(x, DUGOUT_STAND, z, DUCKBOARD)
            for y in range(DUGOUT_STAND + 1, DUGOUT_ROOF):
                clear(x, y, z)
            put(x, DUGOUT_ROOF, z, CORRUGATED)
            put(x, DUGOUT_ROOF + 1, z, SANDBAGS)
    # Shoring: posts at the corners (the middle of each end wall is a doorway), a cap beam over them.
    for x in (x0, x1):
        for z in (z0, z1):
            for y in range(DUGOUT_STAND, DUGOUT_ROOF):
                put(x, y, z, shoring("y"))
    for x in range(x0, x1 + 1):
        put(x, DUGOUT_ROOF - 1, z0, shoring("x"))
    # Bunks along the west wall, the map table and the telephone on it by the east wall, lamps hanging from the roof.
    for z in (65, 67):
        put(x0, DUGOUT_STAND, z, bunk("east", "lower"))
        put(x0, DUGOUT_STAND + 1, z, bunk("east", "upper"))
    put(25, DUGOUT_STAND, 64, map_table("west"))
    tx, ty, tz = TELEPHONE
    put(tx, ty, tz, telephone("west"))
    for x in (19, 25):
        put(x, DUGOUT_ROOF - 1, 66, lamp(True))


def rim(put):
    """Round the pit at ground level: barbed wire a block back from its edge (but over the trench and the gantry), the
    searchlights on sandbags at its corners, and the smoke stacks beyond the north rim."""
    top = GROUND + 1
    for x in range(PIT["x"][0] - 2, PIT["x"][1] + 3):
        for z in (PIT["z"][0] - 3,):
            if not (GANTRY["x"][0] - 2 <= x <= GANTRY["x"][1] + 2):
                put(x, top, z, WIRE)
    for z in range(PIT["z"][0] - 3, PIT["z"][1] + 2):
        for x in (PIT["x"][0] - 3, PIT["x"][1] + 3):
            put(x, top, z, WIRE)
    for x in range(TRENCH["x"][1] + 2, PIT["x"][1] + 3):
        put(x, top, PIT["z"][1] + 3, WIRE)
    for x, z, yaw in SEARCHLIGHTS:
        put(x, top, z, SANDBAGS)
        put(x, top + 1, z, searchlight(yaw))
    for x, z in STACKS:
        for y in range(top, STACK_TOP):
            put(x, y, z, BRICKS)
        put(x, STACK_TOP, z, campfire())


def exit_mist(put):
    """The Grey Mist in the dugout's back doorway, on the bare earth, under the roof's edge."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    for x in range(x0, x1 + 1):
        for y in range(DUGOUT_STAND, DUGOUT_STAND + 3):
            put(x, y, z, EXIT_MIST)
        put(x, DUGOUT_STAND + 3, z, CORRUGATED)


def exit_cells():
    """Where the Grey Mist is (for the audit and the tests)."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(DUGOUT_STAND, DUGOUT_STAND + 3) for x in range(x0, x1 + 1)]


def breaker_cells():
    """Each pylon's breaker: {name: (x, y, z)} (for the audit, the tests and the Java)."""
    return {name: (info["breaker"][0], STAND, info["breaker"][1]) for name, info in PYLONS.items()}


def crown_cells():
    """Each pylon's crown: {name: (x, y, z)}."""
    return {name: (info["at"][0], STAND + 1 + LATTICE, info["at"][1]) for name, info in PYLONS.items()}


def terminal_cells():
    """Each pylon's ground terminal: {name: (x, y, z)} (in the paving)."""
    return {name: (info["terminal"][0], PAVE, info["terminal"][1]) for name, info in PYLONS.items()}


def hollow(blocks):
    """Leaves out blocks nobody can see: those whose six neighbours are all whole, opaque blocks of the template; but none
    in the pit's footprint, so no pocket of air is left inside its cover or its ramp, where the game would see dark floor."""
    opaque = {DIRT, COARSE, MUD, GRAVEL, CONCRETE, LIGHT_CONCRETE, SCORCH, BASTION, HAZARD, PLATE, ARMOUR, SANDBAGS,
              REVETMENT, BRICKS}
    out = {}
    for (x, y, z), state in blocks.items():
        if state in opaque and not in_pit(x, z) and all(blocks.get(n) in opaque for n in (
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
    print(SIZE, len(build()), "blocks")
