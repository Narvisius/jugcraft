"""The Crowfield (docs/features/crowfield.md): the Pumpkin King's lair, written as the structure template
data/jugcraft/structure/lair/crowfield.nbt, which lair/Lairs places into every instance of the jugcraft:crowfield
dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI checks it is current).

A pumpkin field at dusk, walled all round with standing corn: a slab of farmland about 69 blocks across (x) and 76 long
(z) hanging in the void. North is low z.
- the field, the arena (FIELD): a square of tilled earth 45 blocks across round the centre (CENTRE), its rows running
  north and south, furrows of coarse earth between ridges of rooted earth (furrow); pumpkins along the ridges, heirlooms
  among them and a few jack-o'-lanterns (pumpkin), never on the cross through the centre or on the ring of hills, so a
  run between neighbouring hills is never blocked; low cover from two stacks of hay bales (HAY_STACKS) and a tipped-over
  cart (CART);
- the corn: Crowfield corn (the lair-only crowfield_corn), CORN_HIGH blocks tall and too thick to pass, fills everything
  round the field, the farmyard and the lane, its tassels along the top (top=true);
- the King's hills (HILLS): eight hills of dark loam in a ring round the centre, HILL_SPACING apart in a three-by-three
  grid whose middle is his post, in ring order (tools/lairs.py HILL_NAMES); on each a King's Gourd (the lair-only
  kings_gourd, a fixture: green until the King ripens it, part 2) facing the post, moss carpet round it for its vine;
- the King's Post (POST): at the centre, a mound of hay bales (MOUND) with a tall post of stripped dark oak on it and a
  crossbar near its top, where the Pumpkin King hangs waiting (WAIT, part 2), facing the farmyard;
- the farmyard (YARD): at the field's south edge, behind a split-rail fence (FENCE_Z) with a wide farm gate open into the
  field (GATE): a yard of trodden earth with a hay cart, a lamp post, a water trough, a bench of pumpkins and a raven
  perch; the arrival (ARRIVAL), and at the end of the lane behind it Grey Mist (EXIT): the way home.
The moon is not in the template: Lairs places it, MOON_RADIUS across, north beyond the corn (MOON), low, its foot behind
the tassels. The light is hidden light blocks over the field and the yard, the jack-o'-lanterns, the lamp post's lantern
and the Grey Mist.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written (the lair-only blocks' are written with them); vanilla blocks use only properties they have had for
years. Nothing in it changes by itself once placed: no farmland (it would dry), no crops or stems (they would grow or
pop off), no grass blocks; the corn and the gourds are lair blocks. Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/crowfield"

SIZE = (69, 30, 76)
SOIL = 4                           # the floor's top layer
STAND = SOIL + 1                   # where anyone on the floor stands
FIELD = {"x": (12, 56), "z": (10, 54)}   # the field's cells, inclusive: 45 across
CX, CZ = 34, 32                    # the field's middle cell, where the post stands
CENTRE = (34.5, 32.5)              # its middle
# The rows run north and south: a cell is a furrow when (x - FIELD x0) % 3 == FURROW, a ridge otherwise.
FURROW = 1
# The King's hills: the grid's cells HILL_SPACING apart round the post, in ring order (clockwise from the north-west, seen
# from above), each a patch of dark loam HILL_RADIUS round its middle with the gourd on it, moss carpet round the gourd.
HILL_SPACING = 13
HILLS = {
    "north_west": (-1, -1), "north": (0, -1), "north_east": (1, -1), "east": (1, 0),
    "south_east": (1, 1), "south": (0, 1), "south_west": (-1, 1), "west": (-1, 0),
}
HILL_RADIUS = 1.6
# Clear of pumpkins: within CLEAR of the cross through the centre (rows and columns of the post) and of the ring's sides
# (the lines through the hills), within HILL_CLEAR of a hill's middle and within POST_CLEAR of the centre.
CLEAR = 1
HILL_CLEAR = 3.0
POST_CLEAR = 4.5
# The King's Post: hay bales within MOUND of the centre (one high), the post on them from STAND + 1 up to POST_TOP, and a
# crossbar at CROSSBAR_Y reaching CROSSBAR_HALF either side of it (along x). Where the King hangs waiting on it, facing
# south to the farmyard (part 2): x, y, z, yaw.
MOUND = 2.3
POST_TOP = STAND + 7
CROSSBAR_Y = STAND + 6
CROSSBAR_HALF = 2
WAIT = (34.5, STAND + 1, 33.5, 0.0)
# Where the Grey Mist opens at the post when the King falls (part 2): three columns across the floor south of the mound,
# two high.
POST_EXIT = {"x": (33, 35), "z": 35, "y": STAND}
# Low cover in the field: stacks of hay bales (x, z of their north-west cell; two by two, two high), and the tipped-over
# cart (its bed's cells, and its shaft's).
HAY_STACKS = [(27, 25), (40, 38)]
CART = {"bed": [(40, 24), (41, 24), (42, 24), (40, 25), (41, 25), (42, 25)], "shaft": (43, 25)}
# How often a ridge bears a pumpkin, and which: plain pumpkins, the heirlooms, carved ones and jack-o'-lanterns.
PUMPKIN_CHANCE = 0.085
HEIRLOOMS = ("white_pumpkin", "jarrahdale_pumpkin", "cinderella_pumpkin", "red_kuri_pumpkin", "kabocha_pumpkin")
# The fence between the field and the farmyard (z, and its x from west to east) and the farm gate in it, open (its x).
FENCE_Z = 55
FENCE_X = (26, 42)
GATE = (33, 35)
# The farmyard's open cells (inclusive), the arrival in it (x, y, z, yaw: facing north through the gate into the field),
# the lane behind it to the Grey Mist (EXIT: its columns and row, three high).
YARD = {"x": (26, 42), "z": (56, 64)}
ARRIVAL = (34.5, float(STAND), 61.5, 180.0)
LANE = {"x": (33, 35), "z": (65, 68)}
EXIT = {"x": (33, 35), "z": 68}
HAY_CART = [(28, 58), (29, 58), (30, 58), (28, 59), (29, 59), (30, 59)]   # its bed, a hay bale on its middle
LAMP_POST = (40, 57)
TROUGH = [(39, 62), (40, 62)]
BENCH = [(27, 62), (28, 62)]                   # top slabs, a pumpkin on each
PERCH = (42, 60, "west")                       # a raven perch: x, z, facing
# The corn: CORN_HIGH blocks from the floor, the top one in tassel.
CORN_HIGH = 3
# The moon's centre (it faces south) and radius: low over the north-west corn, off the line from the yard to the post.
MOON = (CX - 14, STAND + 16, -26)
MOON_RADIUS = 15
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
BOUNDS = 40
FLOOR = SOIL - 3
# The hidden lights: a lattice LIGHT_SPACING apart (every other row shifted half), LIGHT_Y high, at LIGHT_LEVEL, over the
# field; and these besides (x, y, z, level): in the farmyard and the lane.
LIGHT_Y = STAND + 2
LIGHT_LEVEL = 15
LIGHT_SPACING = 7
LIGHTS = [(30, STAND + 2, 60, 12), (37, STAND + 2, 63, 12), (34, STAND + 2, 66, 10)]
FIELD_LIGHT = 7                    # the least block light on any cell where anyone stands in the field (the audit, and
                                   # the client test against the game's own light)
# The harvest's walk (the plan, docs/branches/BOSSES.md, the Pumpkin King's rule): he calls three gourds in a row round the
# ring from the one nearest his foe, and they ripen in RIPEN_SECONDS. From every open cell of the field, a sprinting
# player (SPRINT blocks a second) walking round the cover comes within REACH of all three, either way round the ring,
# inside HARVEST_SHARE of that time; the audit walks it.
RIPEN_SECONDS = 10
SPRINT = 5.6
REACH = 4.0
CALLED = 3
HARVEST_SHARE = 0.8


# ---------------------------------------------------------------- states

DIRT = block("minecraft:dirt")
COARSE = block("minecraft:coarse_dirt")
PODZOL = block("minecraft:podzol", snowy=False)
ROOTED = block("minecraft:rooted_dirt")
PATH_BLOCK = block("minecraft:dirt_path")
PLANKS = block("minecraft:spruce_planks")
MOSS_CARPET = block("minecraft:moss_carpet")
SHORT_GRASS = block("minecraft:short_grass")
DEAD_BUSH = block("minecraft:dead_bush")
PUMPKIN = block("minecraft:pumpkin")
EXIT_MIST = block("jugcraft:lair_exit", axis="x")


def corn(top):
    return block("jugcraft:crowfield_corn", top=str(top).lower())


def gourd(facing):
    return block("jugcraft:kings_gourd", facing=facing, stage="0")


def log(name, axis):
    return block(f"minecraft:{name}", axis=axis)


def hay():
    return block("minecraft:hay_block", axis="y")


def facing_block(name, facing):
    return block(name, facing=facing)


def slab(name, kind):
    return block(f"minecraft:{name}_slab", type=kind, waterlogged=False)


def fence(east=False, west=False, north=False, south=False):
    return block("minecraft:spruce_fence", north=north, south=south, east=east, west=west, waterlogged=False)


def gate():
    return block("minecraft:spruce_fence_gate", facing="north", open=True, in_wall=False, powered=False)


def lantern():
    return block("minecraft:lantern", hanging=False, waterlogged=False)


def cauldron():
    return block("minecraft:water_cauldron", level=3)


def light(level):
    return block("minecraft:light", level=level, waterlogged=False)


# ---------------------------------------------------------------- shapes

def in_field(x, z):
    return FIELD["x"][0] <= x <= FIELD["x"][1] and FIELD["z"][0] <= z <= FIELD["z"][1]


def in_yard(x, z):
    return YARD["x"][0] <= x <= YARD["x"][1] and YARD["z"][0] <= z <= YARD["z"][1]


def in_lane(x, z):
    return LANE["x"][0] <= x <= LANE["x"][1] and LANE["z"][0] <= z <= LANE["z"][1]


def on_fence(x, z):
    return z == FENCE_Z and FENCE_X[0] <= x <= FENCE_X[1]


def in_gate(x, z):
    return z == FENCE_Z and GATE[0] <= x <= GATE[1]


def open_floor(x, z):
    """Every cell anyone can walk: the field, the gate, the farmyard and the lane."""
    return in_field(x, z) or in_gate(x, z) or in_yard(x, z) or in_lane(x, z)


def distance(x, z):
    """How far the middle of cell (x, z) is from the centre."""
    return math.hypot(x + 0.5 - CENTRE[0], z + 0.5 - CENTRE[1])


def furrow(x):
    return (x - FIELD["x"][0]) % 3 == FURROW


def hill_cell(name):
    """The cell a hill's gourd grows in: (x, y, z)."""
    dx, dz = HILLS[name]
    return CX + dx * HILL_SPACING, STAND, CZ + dz * HILL_SPACING


def hill_cells():
    return {name: hill_cell(name) for name in HILLS}


def hill_facing(name):
    """The way a hill's gourd faces: toward the post, along z where the hill is a corner of the grid."""
    dx, dz = HILLS[name]
    if dz:
        return "south" if dz < 0 else "north"
    return "east" if dx < 0 else "west"


def on_hill(x, z):
    for hx, _, hz in hill_cells().values():
        if math.hypot(x - hx, z - hz) <= HILL_RADIUS:
            return True
    return False


def on_mound(x, z):
    return distance(x, z) <= MOUND


def hay_stack_cells():
    return {(x + dx, z + dz) for x, z in HAY_STACKS for dx in (0, 1) for dz in (0, 1)}


def cover_cells():
    return hay_stack_cells() | set(CART["bed"]) | {CART["shaft"]}


def ring_lines():
    """The lines of the ring's sides and the cross through the centre: x of the grid's columns, z of its rows."""
    return ({CX + d * HILL_SPACING for d in (-1, 0, 1)}, {CZ + d * HILL_SPACING for d in (-1, 0, 1)})


def kept_clear(x, z):
    """Where no pumpkin lies: the cross through the centre and the ring's sides (CLEAR either side, between the hills),
    round each hill and round the post."""
    xs, zs = ring_lines()
    lo_x, hi_x = CX - HILL_SPACING, CX + HILL_SPACING
    lo_z, hi_z = CZ - HILL_SPACING, CZ + HILL_SPACING
    if any(abs(x - lx) <= CLEAR for lx in xs) and lo_z - CLEAR <= z <= hi_z + CLEAR:
        return True
    if any(abs(z - lz) <= CLEAR for lz in zs) and lo_x - CLEAR <= x <= hi_x + CLEAR:
        return True
    # The cross runs on to the field's edges, and to the gate.
    if abs(x - CX) <= CLEAR or abs(z - CZ) <= CLEAR:
        return True
    if any(math.hypot(x - hx, z - hz) <= HILL_CLEAR for hx, _, hz in hill_cells().values()):
        return True
    return distance(x, z) <= POST_CLEAR


def pumpkin(x, z):
    """The pumpkin on ridge cell (x, z), or None."""
    if furrow(x) or kept_clear(x, z) or (x, z) in cover_cells() or not in_field(x, z):
        return None
    if _hash(x, z, 41) >= PUMPKIN_CHANCE:
        return None
    pick, facing = _hash(x, z, 43), ("north", "east", "south", "west")[int(_hash(x, z, 47) * 4) % 4]
    if pick < 0.5:
        return PUMPKIN
    if pick < 0.85:
        return facing_block("jugcraft:" + HEIRLOOMS[int(_hash(x, z, 53) * len(HEIRLOOMS)) % len(HEIRLOOMS)], facing)
    if pick < 0.93:
        return facing_block("minecraft:carved_pumpkin", facing)
    return facing_block("minecraft:jack_o_lantern", facing)


# ---------------------------------------------------------------- building

def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    floor(put)
    corn_wall(put)
    field(put)
    hills(put)
    post(put)
    cover(put)
    farmyard(put)
    exit_mist(put)
    lights(put, blocks)
    return hollow(blocks)


def floor(put):
    """The floor: dirt under a top of coarse earth in the furrows and rooted earth on the ridges in the field, trodden earth
    through the gate, across the farmyard to the lane and down it, coarse earth and podzol in the yard and under the corn."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            for y in range(SOIL):
                put(x, y, z, DIRT)
            h = _hash(x, z, 3)
            if in_field(x, z):
                top = COARSE if furrow(x) else ROOTED if h < 0.7 else DIRT
            elif in_lane(x, z) or in_gate(x, z) or in_yard(x, z) and GATE[0] <= x <= GATE[1]:
                top = PATH_BLOCK
            elif in_yard(x, z):
                top = COARSE if h < 0.6 else PODZOL
            else:
                top = COARSE if h < 0.5 else PODZOL
            put(x, SOIL, z, top)


def corn_wall(put):
    """The corn: every cell off the open floor and the fence, CORN_HIGH tall, its top in tassel."""
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if open_floor(x, z) or on_fence(x, z):
                continue
            for y in range(STAND, STAND + CORN_HIGH):
                put(x, y, z, corn(y == STAND + CORN_HIGH - 1))


def field(put):
    """The pumpkins along the ridges, and weeds: grass on a few ridges, a dead bush now and then in a furrow."""
    for x in range(FIELD["x"][0], FIELD["x"][1] + 1):
        for z in range(FIELD["z"][0], FIELD["z"][1] + 1):
            if on_hill(x, z) or on_mound(x, z) or (x, z) in cover_cells():
                continue
            state = pumpkin(x, z)
            if state:
                put(x, STAND, z, state)
            elif kept_clear(x, z):
                continue
            elif not furrow(x) and _hash(x, z, 59) < 0.06:
                put(x, STAND, z, SHORT_GRASS)
            elif furrow(x) and _hash(x, z, 61) < 0.025:
                put(x, STAND, z, DEAD_BUSH)


def hills(put):
    """The King's hills: dark loam round each gourd, moss carpet on it for the vine, the gourd facing the post."""
    for name, (hx, hy, hz) in hill_cells().items():
        for x in range(hx - 2, hx + 3):
            for z in range(hz - 2, hz + 3):
                if math.hypot(x - hx, z - hz) <= HILL_RADIUS:
                    put(x, SOIL, z, PODZOL)
                    if (x, z) != (hx, hz):
                        put(x, STAND, z, MOSS_CARPET)
        put(hx, hy, hz, gourd(hill_facing(name)))


def post(put):
    """The King's Post: the mound of hay bales, the post of stripped dark oak on it, its crossbar."""
    for x in range(CX - 3, CX + 4):
        for z in range(CZ - 3, CZ + 4):
            if on_mound(x, z):
                put(x, STAND, z, hay())
    for y in range(STAND + 1, POST_TOP + 1):
        put(CX, y, CZ, log("stripped_dark_oak_log", "y"))
    for dx in range(-CROSSBAR_HALF, CROSSBAR_HALF + 1):
        if dx:
            put(CX + dx, CROSSBAR_Y, CZ, log("stripped_dark_oak_log", "x"))


def cover(put):
    """The hay stacks, two high, and the tipped-over cart: its bed of planks with a slab of its side tipped up on it, and its
    shaft, a fence post."""
    for x, z in hay_stack_cells():
        for y in (STAND, STAND + 1):
            put(x, y, z, hay())
    for x, z in CART["bed"]:
        put(x, STAND, z, PLANKS)
    for x, z in CART["bed"][:3]:
        put(x, STAND + 1, z, slab("spruce", "bottom"))
    sx, sz = CART["shaft"]
    put(sx, STAND, sz, fence(west=True))


def farmyard(put):
    """The fence and its gate, the hay cart, the lamp post, the trough, the bench of pumpkins and the raven perch."""
    for x in range(FENCE_X[0], FENCE_X[1] + 1):
        put(x, STAND, FENCE_Z, gate() if GATE[0] <= x <= GATE[1] else fence(east=x < FENCE_X[1], west=x > FENCE_X[0]))
    for x, z in HAY_CART:
        put(x, STAND, z, PLANKS)
    put(HAY_CART[1][0], STAND + 1, HAY_CART[1][1], hay())
    lx, lz = LAMP_POST
    put(lx, STAND, lz, fence())
    put(lx, STAND + 1, lz, fence())
    put(lx, STAND + 2, lz, lantern())
    for x, z in TROUGH:
        put(x, STAND, z, cauldron())
    for i, (x, z) in enumerate(BENCH):
        put(x, STAND, z, slab("spruce", "top"))
        put(x, STAND + 1, z, facing_block("jugcraft:white_pumpkin", "south") if i else PUMPKIN)
    px, pz, facing = PERCH
    put(px, STAND, pz, facing_block("jugcraft:raven_perch", facing))


def exit_mist(put):
    """The Grey Mist at the end of the lane behind the farmyard, between the corn."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    for x in range(x0, x1 + 1):
        for y in range(STAND, STAND + 3):
            put(x, y, z, EXIT_MIST)


def post_exit_cells():
    """Where the Grey Mist opens at the post when the King falls."""
    (x0, x1), z, y = POST_EXIT["x"], POST_EXIT["z"], POST_EXIT["y"]
    return [(x, yy, z) for yy in (y, y + 1) for x in range(x0, x1 + 1)]


def exit_cells():
    """Where the Grey Mist is (for the audit and the tests)."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(STAND, STAND + 3) for x in range(x0, x1 + 1)]


def lights(put, blocks):
    """Hidden lights over the field (a staggered lattice, a light moved to the next free cell when its own is taken), and
    in the farmyard and the lane."""
    (x0, x1), (z0, z1) = FIELD["x"], FIELD["z"]
    for row, z in enumerate(range(z0 + 2, z1 + 1, LIGHT_SPACING // 2 + 1)):
        for x in range(x0 + 2 + (LIGHT_SPACING // 2 if row % 2 else 0), x1 + 1, LIGHT_SPACING):
            for cx, cz in ((x, z), (x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                if in_field(cx, cz) and (cx, LIGHT_Y, cz) not in blocks:
                    put(cx, LIGHT_Y, cz, light(LIGHT_LEVEL))
                    break
    for x, y, z, level in LIGHTS:
        if (x, y, z) not in blocks:
            put(x, y, z, light(level))


def hollow(blocks):
    """Leaves out blocks nobody can see: those whose six neighbours are all whole, opaque blocks of the template; but none
    over the field's floor, so no pocket of air is left inside a hay stack, where the game would see dark floor."""
    opaque = {DIRT, COARSE, PODZOL, ROOTED}
    out = {}
    for (x, y, z), state in blocks.items():
        if y >= STAND and in_field(x, z):
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
    blocks = build()
    field_pumpkins = [s for (x, y, z), s in blocks.items() if y == STAND and in_field(x, z) and ("pumpkin" in s[0] or "lantern" in s[0])]
    print(SIZE, len(blocks), "blocks,", sum(1 for s in blocks.values() if s[0] == "jugcraft:kings_gourd"), "gourds,",
          len(field_pumpkins), "pumpkins in the field")
