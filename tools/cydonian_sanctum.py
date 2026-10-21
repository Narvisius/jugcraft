"""The Cydonian Sanctum (docs/features/cydonian-sanctum.md): the Cydonian Sentinel's lair on Mars, written as the
structure template data/jugcraft/structure/lair/cydonian_sanctum.nbt, which lair/Lairs places into every instance of the
jugcraft:cydonian_sanctum dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI
checks it is current).

Ruins under the butterscotch sky of Cydonia, cut into red rock and hanging in the void. North is low z.
- the crater, the arena (CRATER): a floor of red sand and rust-red rock 44 blocks across, ringed by its rim wall (RIM);
  four standing stones (STONES), carved with the Ancient Altar's red star, stand round it; the Sentinel's plinth
  (PLINTH) is on the north rim, steps down from it to the floor; small craters (PITS) pock the floor, where his sentries
  will rise (part 3);
- the passage (PASSAGE) cut through the south rim from the crater to the Star Door (DOOR), the north end of the Dust
  Gallery: carved with three constellations (DOOR_FIGURES), which light as the dials are turned to them;
- the Dust Gallery (GALLERY): a corridor 40 blocks long cut into a ridge of red rock (RIDGE), lines of dust vents (VENTS)
  across its floor, and three star dials (DIALS) on its walls;
- the gate chamber (CHAMBER): a hall cut into the ridge at the gallery's south end, the arrival (ARRIVAL), and behind it,
  in an arch of the south wall, the Red Mist (EXIT): the way home;
- the two moons over the north rim: Phobos (PHOBOS), and Deimos (DEIMOS) higher and further off.
The light under the rock is hidden light blocks; the crater lies open to the sky.

Format: tools/lair_layout.py's (26.3's). Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/cydonian_sanctum"

SIZE = (73, 58, 131)
PAVE = 6                           # the floor's top layer (the crater's sand, the gallery's paving)
STAND = PAVE + 1                   # where anyone on the floor stands
CENTRE = (36.5, 46.5)              # the crater's middle: the middle of cell (36, 46)
CRATER = 22.0                      # the floor's radius: cells whose middle is within it (44 across)
# The rim round the floor, by distance from CENTRE: the inner wall climbs from the floor to the crest (CREST high over
# PAVE), the outer slope falls from the crest to the shelf, and the shelf runs out to the edge, where the void begins.
RIM = {"wall": 25.0, "crest": 8, "slope": 28.0, "shelf": 31.0, "shelf_height": 2}
# The four standing stones: (x, z) of each one's middle cell; each is STONE_SIZE cells square and STONE_HEIGHT high, with
# a star stone in the middle of each face STONE_STAR blocks over the floor.
STONES = [(44, 38), (28, 38), (28, 54), (44, 54)]
STONE_SIZE = 3
STONE_HEIGHT = 5
STONE_STAR = 2
# The Sentinel's plinth on the north rim: its cells (inclusive), its top PLINTH_RISE blocks over the floor, and a row of
# steps down from it onto the floor (z), facing north. WAIT is where the Sentinel will stand on it (x, y, z, yaw).
PLINTH = {"x": (33, 39), "z": (17, 23)}
PLINTH_RISE = 1
PLINTH_STEPS = 24
WAIT = (36.5, STAND + PLINTH_RISE, 20.5, 0.0)
# The small craters: (x, z) of each one's middle, and their radius: the floor sinks a block in them.
PITS = [(26, 46), (46, 46), (31, 31), (41, 31), (31, 61), (41, 61)]
PIT_RADIUS = 1.6
# The passage through the south rim to the door: its open cells (inclusive), open to the sky.
PASSAGE = {"x": (34, 38), "z": (66, 74)}
# The ridge the gallery and the chamber are cut into: its cells (inclusive), and its top over the floor.
RIDGE = {"x": (25, 47), "z": (75, 130)}
RIDGE_TOP = STAND + 9
# The Dust Gallery: its open cells (inclusive), STAND to GALLERY_HEIGHT - 1 over it (the top row only in the middle three).
GALLERY = {"x": (34, 38), "z": (76, 115)}
GALLERY_HEIGHT = 5
# The Star Door: the gallery's cross-section at DOOR_Z, filled. Its three constellations, carved in its middle row
# (DOOR_FIGURE_Y) at DOOR_FIGURES' columns, west to east.
DOOR_Z = 75
DOOR_FIGURE_Y = STAND + 2
DOOR_FIGURES = {"hunter": 35, "swan": 36, "crown": 37}
# When the door opens it crumbles a row at a time from the top: the top row DOOR_FIRST_ROW_TICKS after the last dial
# is turned, each row DOOR_ROW_TICKS after the one above. A lit figure gives DOOR_LIGHT.
DOOR_FIRST_ROW_TICKS = 10
DOOR_ROW_TICKS = 6
DOOR_LIGHT = 7
# The constellations a star dial shows, in the order a turn takes them: the door's three, and three more.
CONSTELLATIONS = ("plough", "hunter", "lyre", "swan", "scorpion", "crown")
# The star dials on the gallery's walls: (x, y, z), the way each faces (into the gallery), and what each shows at first
# (none of the door's).
DIALS = [
    {"pos": (33, STAND + 1, 111), "facing": "east", "stars": "plough"},
    {"pos": (39, STAND + 1, 99), "facing": "west", "stars": "lyre"},
    {"pos": (33, STAND + 1, 87), "facing": "east", "stars": "scorpion"},
]
# The dust vents: lines across the gallery's floor (z), each its full width, and the tick (of VENT_PERIOD) each line's
# puff comes at: neighbours alternate, so there is always a safe line of floor between two.
VENT_LINES = {108: 0, 102: 30, 96: 0, 90: 30, 84: 0}
# A vent's rhythm, in ticks: every VENT_PERIOD it hisses and sends up a wisp for VENT_HISS ticks (the tell), then puffs
# for VENT_PUFF ticks; a puff strikes whoever stands on it for VENT_DAMAGE and throws them back (VENT_PUSH south, and
# VENT_LIFT up), once a puff.
VENT = {"period": 60, "hiss": 20, "puff": 6, "damage": 3.0, "push": 0.9, "lift": 0.35}
# The thin air: everyone in the Sanctum has Jump Boost (this amplifier, so Jump Boost I) and takes this much of a fall's
# damage.
AIR = {"jump_amplifier": 0, "fall_damage": 0.5}
# The gate chamber: its open cells (inclusive), STAND to CHAMBER_HEIGHT - 1 over it (an arched ceiling).
CHAMBER = {"x": (30, 42), "z": (116, 128)}
CHAMBER_HEIGHT = 7
CHAMBER_PILLARS = [(32, 119), (40, 119), (32, 125), (40, 125)]
ARRIVAL = (36.5, STAND, 125.5, 180.0)         # x, y, z and yaw: in the chamber, facing north up the gallery
EXIT = {"x": (35, 37), "z": 129}              # the Red Mist's columns and row, three blocks high from STAND
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
BOUNDS = 88
FLOOR = PAVE - 4
# The moons: middle (x, y, z) and radius. Phobos is the nearer and bigger; Deimos is higher and smaller.
PHOBOS = ((24, STAND + 38, 5), 3.2)
DEIMOS = ((52, STAND + 46, 9), 2.2)
# The hidden lights: in the gallery (every GALLERY_LIGHT_STEP rows, its middle, at STAND + 3), the chamber's lattice,
# and over the crater (the lairs' sky gives no light): a lattice CRATER_LIGHT_STEP apart, CRATER_LIGHT_HEIGHT over the
# floor, staggered row to row, and lights along the passage and the shelf.
GALLERY_LIGHT = 15
GALLERY_LIGHT_STEP = 4
CHAMBER_LIGHT = 15
CRATER_LIGHT = 15
CRATER_LIGHT_STEP = 8
CRATER_LIGHT_HEIGHT = 4
GALLERY_LIGHT_LEAST = 7            # the least block light on any cell where anyone stands in the gallery (the audit)


# ---------------------------------------------------------------- states

RED_SAND = block("minecraft:red_sand")
ROCKS = (block("minecraft:red_terracotta"), block("minecraft:terracotta"), block("minecraft:orange_terracotta"),
         block("minecraft:red_sandstone"))
SANDSTONE = block("minecraft:red_sandstone")
CUT = block("minecraft:cut_red_sandstone")
CHISELED = block("minecraft:chiseled_red_sandstone")
SMOOTH = block("minecraft:smooth_red_sandstone")
DUSTY = block("minecraft:red_terracotta")
MOON_ROCKS = (block("minecraft:tuff"), block("minecraft:packed_mud"))
STAR_STONE = block("jugcraft:star_stone")
VENT_BLOCK = block("jugcraft:dust_vent")
EXIT_MIST = block("jugcraft:red_mist", axis="x")


def door(figure="plain", lit=False):
    return block("jugcraft:star_door", figure=figure, lit=lit, opening=False)


def dial(facing, stars):
    return block("jugcraft:star_dial", facing=facing, stars=stars)


def steps(facing):
    return block("minecraft:red_sandstone_stairs", facing=facing, half="bottom", shape="straight", waterlogged=False)


def light(level):
    return block("minecraft:light", level=level, waterlogged=False)


# ---------------------------------------------------------------- shapes

def distance(x, z):
    """How far the middle of cell (x, z) is from the crater's middle."""
    return math.hypot(x + 0.5 - CENTRE[0], z + 0.5 - CENTRE[1])


def in_crater(x, z):
    return distance(x, z) <= CRATER


def in_box(box, x, z):
    return box["x"][0] <= x <= box["x"][1] and box["z"][0] <= z <= box["z"][1]


def stone_cells(stone):
    """The cells (x, z) of a standing stone."""
    cx, cz = stone
    half = STONE_SIZE // 2
    return [(x, z) for x in range(cx - half, cx + half + 1) for z in range(cz - half, cz + half + 1)]


def in_stone(x, z):
    return any((x, z) in stone_cells(stone) for stone in STONES)


def in_pit(x, z):
    return any(math.hypot(x - px, z - pz) <= PIT_RADIUS for px, pz in PITS)


def rim_top(x, z):
    """The top block's height of the rim's column (x, z), or None beyond the shelf: the void."""
    d = distance(x, z)
    wobble = round(_hash(x, z, 31) * 1.4)
    if d <= RIM["wall"]:
        return PAVE + min(RIM["crest"], round((d - CRATER) / (RIM["wall"] - CRATER) * RIM["crest"])) + wobble // 2
    if d <= RIM["slope"]:
        fall = (d - RIM["wall"]) / (RIM["slope"] - RIM["wall"]) * (RIM["crest"] - RIM["shelf_height"])
        return PAVE + RIM["crest"] - round(fall) + wobble
    if d <= RIM["shelf"]:
        return PAVE + RIM["shelf_height"] - (1 if _hash(x, z, 37) < 0.3 else 0)
    return None


def rock(x, y, z):
    """The rust-red rock: bands of terracotta by height, broken up."""
    band = (y // 2 + (1 if _hash(x, z, y // 2, 41) < 0.25 else 0)) % len(ROCKS)
    return ROCKS[band]


def in_gallery(x, y, z):
    if not in_box(GALLERY, x, z) or not STAND <= y < STAND + GALLERY_HEIGHT:
        return False
    return y < STAND + GALLERY_HEIGHT - 1 or GALLERY["x"][0] < x < GALLERY["x"][1]


def in_chamber(x, y, z):
    if not in_box(CHAMBER, x, z) or not STAND <= y < STAND + CHAMBER_HEIGHT:
        return False
    # The arched ceiling: the top two rows narrow in from the side walls.
    inset = max(0, y - (STAND + CHAMBER_HEIGHT - 3))
    return CHAMBER["x"][0] + inset <= x <= CHAMBER["x"][1] - inset


def door_cells():
    """The Star Door's cells (x, y, z): the gallery's cross-section at DOOR_Z."""
    return [(x, y, DOOR_Z) for y in range(STAND, STAND + GALLERY_HEIGHT) for x in range(GALLERY["x"][0], GALLERY["x"][1] + 1)
            if in_gallery(x, y, GALLERY["z"][0])]


def figure_cells():
    """Each of the door's constellations: {figure: (x, y, z)}."""
    return {figure: (x, DOOR_FIGURE_Y, DOOR_Z) for figure, x in DOOR_FIGURES.items()}


def vent_cells():
    """The dust vents: [(x, PAVE, z)], each line's cells west to east, the lines north to south."""
    return [(x, PAVE, z) for z in sorted(VENT_LINES) for x in range(GALLERY["x"][0], GALLERY["x"][1] + 1)]


def dial_cells():
    return [tuple(info["pos"]) for info in DIALS]


def exit_cells():
    """Where the Red Mist is (for the audit and the tests)."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(STAND, STAND + 3) for x in range(x0, x1 + 1)]


def moon_cells(moon):
    """A moon: a lumpy ball of rock round its middle."""
    (mx, my, mz), radius = moon
    reach = math.ceil(radius) + 1
    out = []
    for x in range(mx - reach, mx + reach + 1):
        for y in range(my - reach, my + reach + 1):
            for z in range(mz - reach, mz + reach + 1):
                lump = radius + (_hash(x, y, z, 53) - 0.5) * 0.9
                if math.dist((x, y, z), (mx, my, mz)) <= lump:
                    out.append((x, y, z))
    return out


# ---------------------------------------------------------------- the build

def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    def clear(x, y, z):
        blocks.pop((x, y, z), None)

    land(put)
    ridge(put, clear)
    plinth(put, clear)
    stones(put)
    door_and_dials(put)
    vents(put)
    exit_mist(put)
    moons(put)
    lights(put, blocks)
    return hollow(blocks)


def land(put):
    """The crater's floor and its rim, and the shelf round it: rock from the bottom up, sand on the floor."""
    for x in range(SIZE[0]):
        for z in range(min(RIDGE["z"][0], SIZE[2])):
            if in_crater(x, z) or in_box(PASSAGE, x, z):
                top = PAVE - (1 if in_pit(x, z) else 0)
                for y in range(top):
                    put(x, y, z, rock(x, y, z))
                outcrop = _hash(x, z, 43)
                put(x, top, z, DUSTY if in_pit(x, z) else SANDSTONE if outcrop < 0.08 else RED_SAND)
                continue
            top = rim_top(x, z)
            if top is None:
                continue
            for y in range(top + 1):
                put(x, y, z, rock(x, y, z))
            if distance(x, z) > RIM["slope"]:
                put(x, top, z, RED_SAND)


def ridge(put, clear):
    """The ridge of rock south of the crater, and the gallery and the chamber cut into it."""
    for x in range(RIDGE["x"][0], RIDGE["x"][1] + 1):
        for z in range(RIDGE["z"][0], RIDGE["z"][1] + 1):
            edge = min(x - RIDGE["x"][0], RIDGE["x"][1] - x, RIDGE["z"][1] - z)
            top = RIDGE_TOP - (2 if edge == 0 else 1 if edge == 1 else 0) - (1 if _hash(x, z, 47) < 0.2 else 0)
            for y in range(top + 1):
                put(x, y, z, rock(x, y, z))
    for x in range(RIDGE["x"][0], RIDGE["x"][1] + 1):
        for z in range(RIDGE["z"][0], RIDGE["z"][1] + 1):
            for y in range(STAND, STAND + CHAMBER_HEIGHT):
                if in_gallery(x, y, z) or in_chamber(x, y, z):
                    clear(x, y, z)
            if in_box(GALLERY, x, z) or in_box(CHAMBER, x, z):
                put(x, PAVE, z, SMOOTH if (x + z) % 2 else CUT)
    # The gallery's walls: a carved band every six rows; the chamber's: cut sandstone, chiseled at eye height.
    for z in range(GALLERY["z"][0], GALLERY["z"][1] + 1):
        for x in (GALLERY["x"][0] - 1, GALLERY["x"][1] + 1):
            for y in range(STAND, STAND + GALLERY_HEIGHT - 1):
                put(x, y, z, CHISELED if z % 6 == 0 and y == STAND + 1 else CUT)
    for z in range(CHAMBER["z"][0], CHAMBER["z"][1] + 1):
        for x in (CHAMBER["x"][0] - 1, CHAMBER["x"][1] + 1):
            for y in range(STAND, STAND + CHAMBER_HEIGHT - 2):
                put(x, y, z, CHISELED if y == STAND + 2 else CUT)
    for x, z in CHAMBER_PILLARS:
        for y in range(STAND, STAND + CHAMBER_HEIGHT):
            put(x, y, z, CHISELED if y in (STAND, STAND + CHAMBER_HEIGHT - 1) else CUT)


def plinth(put, clear):
    """The Sentinel's plinth, cut into the north rim, and its steps down to the floor."""
    top = PAVE + PLINTH_RISE
    for x in range(PLINTH["x"][0], PLINTH["x"][1] + 1):
        for z in range(PLINTH["z"][0], PLINTH["z"][1] + 1):
            for y in range(top + 1, SIZE[1]):
                clear(x, y, z)
            edge = x in PLINTH["x"] or z in PLINTH["z"]
            put(x, top, z, CHISELED if edge else SMOOTH)
            for y in range(top):
                put(x, y, z, rock(x, y, z))
        put(x, PAVE, PLINTH_STEPS, steps("north"))
    put((PLINTH["x"][0] + PLINTH["x"][1]) // 2, top, PLINTH["z"][0], STAR_STONE)


def stones(put):
    """The four standing stones: red sandstone, banded, a star stone in the middle of each face."""
    for stone in STONES:
        cx, cz = stone
        for x, z in stone_cells(stone):
            for y in range(STAND, STAND + STONE_HEIGHT):
                middle_of_face = (x == cx) != (z == cz)
                if y == STAND + STONE_STAR and middle_of_face:
                    put(x, y, z, STAR_STONE)
                elif y == STAND + STONE_HEIGHT - 1:
                    put(x, y, z, CHISELED if (x == cx and z == cz) else SMOOTH)
                else:
                    put(x, y, z, CUT if y % 2 else SANDSTONE)


def door_and_dials(put):
    figures = {pos: figure for figure, pos in figure_cells().items()}
    for x, y, z in door_cells():
        put(x, y, z, door(figures.get((x, y, z), "plain")))
    for info in DIALS:
        put(*info["pos"], dial(info["facing"], info["stars"]))


def vents(put):
    for x, y, z in vent_cells():
        put(x, y, z, VENT_BLOCK)


def exit_mist(put):
    """The Red Mist in an arch of the chamber's south wall: the way home."""
    for x, y, z in exit_cells():
        put(x, y, z, EXIT_MIST)
    (x0, x1), z = EXIT["x"], EXIT["z"]
    for x in range(x0 - 1, x1 + 2):
        put(x, STAND + 3, z, CHISELED)
    for y in range(STAND, STAND + 3):
        put(x0 - 1, y, z, CHISELED)
        put(x1 + 1, y, z, CHISELED)


def moons(put):
    for moon in (PHOBOS, DEIMOS):
        for x, y, z in moon_cells(moon):
            put(x, y, z, MOON_ROCKS[0] if _hash(x, y, z, 59) < 0.7 else MOON_ROCKS[1])


def lights(put, blocks):
    """The hidden lights: down the gallery's middle, and a lattice in the chamber."""
    middle = (GALLERY["x"][0] + GALLERY["x"][1]) // 2
    for z in range(GALLERY["z"][0] + 2, GALLERY["z"][1] + 1, GALLERY_LIGHT_STEP):
        if (middle, STAND + 3, z) not in blocks:
            put(middle, STAND + 3, z, light(GALLERY_LIGHT))
    for x in range(CHAMBER["x"][0] + 2, CHAMBER["x"][1] + 1, 4):
        for z in range(CHAMBER["z"][0] + 2, CHAMBER["z"][1] + 1, 4):
            if (x, STAND + 3, z) not in blocks:
                put(x, STAND + 3, z, light(CHAMBER_LIGHT))
    y = STAND + CRATER_LIGHT_HEIGHT
    step = CRATER_LIGHT_STEP
    for row, z in enumerate(range(int(CENTRE[1]) - 24, int(CENTRE[1]) + 25, step // 2)):
        for x in range(int(CENTRE[0]) - 24 + (step // 2 if row % 2 else 0), int(CENTRE[0]) + 25, step):
            if distance(x, z) <= RIM["slope"] and (x, y, z) not in blocks:
                put(x, y, z, light(CRATER_LIGHT))
    for z in range(PASSAGE["z"][0], PASSAGE["z"][1] + 1, 4):
        if (int(CENTRE[0]), y, z) not in blocks:
            put(int(CENTRE[0]), y, z, light(CRATER_LIGHT))


def hollow(blocks):
    """Leaves out blocks nobody can see: rock whose six neighbours are all rock. Sand is never counted, so whatever holds
    it up is kept."""
    opaque = {*ROCKS, CUT, CHISELED, SMOOTH, *MOON_ROCKS}
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
