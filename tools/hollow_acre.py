"""The Hollow Acre (docs/features/hollow-acre.md): Vesperine's lair, written as the structure template
data/jugcraft/structure/lair/hollow_acre.nbt, which lair/Lairs places into every instance of the jugcraft:hollow_acre
dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI checks it is current).

A floating island of black earth in a starless night, about 64 blocks across and 76 long, its underside hanging in
roots over the void. From south to north (x across, 0 to 63; z from the chapel at 0 to the south edge at 75; y up, the
top soil at SURFACE):
- the lych gate (the graveyard's own Lych Gate) on the south edge, its opening filled with Grey Mist, the way home;
  players arrive just inside it, facing north (ARRIVAL);
- the black wheat: a field of blighted wheat and leaning headstones either side of a path lit by witchlight stakes;
- the Mown Circle: the arena, a disc of cut stubble ARENA_RADIUS across its centre, ringed with soul soil and edged with
  soul braziers (the four on its diagonals ward it in the fight);
- the bone chapel: a ruin at the north end, its north wall holding a broken rose window, its sides lined with ossuary
  walls, sarcophagi in its aisles, witchlights hanging from what is left of its roof and the Bone Throne at its open
  south side, facing the circle.
The moon is not in the template: Lairs places it, MOON_RADIUS across, north of the island (MOON), so the fight can turn
it red.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written; vanilla blocks use only properties they have had for years. Deterministic: the same file every run.
"""
import json
import math

import graveyard
import lair_layout
from lair_layout import ROOT, block, hash01 as _hash

BUILDINGS = ROOT / "src" / "main" / "resources" / "jugcraft" / "graveyard_buildings.json"
TEMPLATE = "lair/hollow_acre"

SIZE = (64, 48, 76)
SURFACE = 16                       # the top soil's y; players stand on SURFACE + 1
ISLAND = {"centre": (32.0, 38.0), "radius": (31.0, 37.0), "depth": 16}
ARENA_CENTRE = (32.0, 36.0)        # x, z (block corners: block x spans x to x + 1)
ARENA_RADIUS = 20
RING = 1.5                         # the soul-soil ring inside the arena's edge
PATH = (30, 33)                    # x of the path from the circle to the gate
FIELD = (57, 66)                   # z of the black wheat
CHAPEL = {"x": (20, 43), "z": (3, 15), "wall": 11}
THRONE = (31, SURFACE + 1, 13)     # the Bone Throne's lower half, facing south
GATE = (33, SURFACE + 1, 69)       # the lych gate's part 0, facing south
EXIT = [(31, SURFACE + 1, 69), (32, SURFACE + 1, 69), (31, SURFACE + 2, 69), (32, SURFACE + 2, 69)]
ARRIVAL = (32.0, SURFACE + 1, 66.5, 180.0)   # x, y, z and yaw (north)
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from the island's centre, or below
# FLOOR.
BOUNDS = 44
FLOOR = SURFACE - 24
MOON = (32, SURFACE + 30, -30)     # the moon's centre (it faces south)
MOON_RADIUS = 13
# The soul braziers: the four wards on the circle's diagonals, then the rest round its edge and either side of the throne.
WARDS = [(17, SURFACE + 1, 21), (46, SURFACE + 1, 21), (17, SURFACE + 1, 50), (46, SURFACE + 1, 50)]
BRAZIERS = WARDS + [(10, SURFACE + 1, 36), (53, SURFACE + 1, 36), (27, SURFACE + 1, 15), (36, SURFACE + 1, 15)]


# ---------------------------------------------------------------- states

SOIL = block("jugcraft:blighted_soil")
WHEAT = block("jugcraft:black_wheat")
STUBBLE = block("jugcraft:mown_stubble")
BRAZIER = block("jugcraft:lair_brazier", lit=True)
EXIT_MIST = block("jugcraft:lair_exit", axis="x")
SOUL_SOIL = block("minecraft:soul_soil")
TUFF = block("minecraft:tuff")
DEEPSLATE = block("minecraft:deepslate", axis="y")
ROOTS = block("minecraft:hanging_roots", waterlogged=False)
BRICKS = block("minecraft:polished_blackstone_bricks")
CRACKED = block("minecraft:cracked_polished_blackstone_bricks")
BLACKSTONE = block("minecraft:blackstone")
POLISHED = block("minecraft:polished_blackstone")
BONE = block("minecraft:bone_block", axis="y")
BEAM = block("minecraft:dark_oak_log", axis="x")
GLASS_RED = block("minecraft:red_stained_glass")
GLASS_PURPLE = block("minecraft:purple_stained_glass")
COBWEB = block("minecraft:cobweb")
DEAD_BUSH = block("minecraft:dead_bush")


# ---------------------------------------------------------------- the island

def island_depth(x, z):
    """How many blocks of earth hang under the top soil at (x, z), or 0 off the island."""
    (cx, cz), (rx, rz) = ISLAND["centre"], ISLAND["radius"]
    dx, dz = (x + 0.5 - cx) / rx, (z + 0.5 - cz) / rz
    angle = math.atan2(dz, dx)
    rim = 1.0 + 0.035 * math.sin(angle * 5.0 + 0.7) + 0.025 * math.sin(angle * 11.0 + 2.1)
    r = math.hypot(dx, dz) / rim
    (x0, x1), (z0, z1) = CHAPEL["x"], CHAPEL["z"]
    if x0 - 1 <= x <= x1 + 1 and z0 - 2 <= z <= z1:
        r = min(r, 0.85)  # the chapel stands on a squared-off spur of rock
    if r >= 1.0:
        return 0
    t = 1.0 - r * r
    return max(1, 2 + int(t * ISLAND["depth"] * (0.7 + 0.3 * _hash(x, z, 7))))


def in_arena(x, z, inset=0.0):
    return math.hypot(x + 0.5 - ARENA_CENTRE[0], z + 0.5 - ARENA_CENTRE[1]) <= ARENA_RADIUS - inset


def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    top = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            depth = island_depth(x, z)
            if not depth:
                continue
            top[(x, z)] = True
            for d in range(depth):
                y = SURFACE - d
                if d < 3:
                    state = SOIL
                else:
                    roll = _hash(x, y, z, 1)
                    state = TUFF if roll < 0.55 else DEEPSLATE if roll < 0.85 else SOUL_SOIL
                put(x, y, z, state)
            if depth > 3 and _hash(x, z, 2) < 0.18:
                put(x, SURFACE - depth, z, ROOTS)

    # The Mown Circle: cut stubble on black earth, a soul-soil ring inside its edge.
    for (x, z) in top:
        if in_arena(x, z):
            if not in_arena(x, z, RING):
                put(x, SURFACE, z, SOUL_SOIL)
            elif _hash(x, z, 3) < 0.6:
                put(x, SURFACE + 1, z, STUBBLE)
    # The path from the circle to the lych gate.
    for z in range(SIZE[2]):
        for x in range(PATH[0], PATH[1] + 1):
            if (x, z) in top and not in_arena(x, z) and z > CHAPEL["z"][1]:
                put(x, SURFACE, z, SOUL_SOIL)

    field(put, top)
    chapel(put)
    lych_gate(put)
    for pos in BRAZIERS:
        put(*pos, BRAZIER)
    # Dead bushes here and there round the edge.
    for (x, z) in top:
        if not in_arena(x, z, -2) and (x, SURFACE + 1, z) not in blocks and not (PATH[0] - 1 <= x <= PATH[1] + 1) \
                and z > CHAPEL["z"][1] + 1 and _hash(x, z, 4) < 0.05:
            put(x, SURFACE + 1, z, DEAD_BUSH)
    return blocks


def field(put, top):
    """The black wheat either side of the path, with weathered headstones in rows and witchlight stakes along the path."""
    stones = ["gothic_headstone", "winged_skull_headstone", "willow_urn_headstone", "rustic_scroll_headstone",
              "lamb_headstone", "broken_column", "celtic_cross"]
    graves = set()
    for row, z in enumerate(range(FIELD[0] + 1, FIELD[1], 4)):
        for column, x in enumerate(range(13 + (row % 2) * 2, 51, 5)):
            if PATH[0] - 2 <= x <= PATH[1] + 2 or (x, z) not in top or (x, z + 1) not in top:
                continue
            kind = stones[int(_hash(x, z, 5) * len(stones))]
            facing = "south" if _hash(x, z, 6) < 0.75 else ("east" if x < PATH[0] else "west")
            weathering = 1 + int(_hash(x, z, 8) * 3)
            for part, cell in enumerate(graveyard.HEADSTONES[kind]["cells"]):
                put(x, SURFACE + 1 + cell[1], z, block(f"jugcraft:{kind}", facing=facing, part=part, weathering=weathering,
                                                       waxed=False))
                graves.add((x, z))
            # A mourning flower before some of them.
            if _hash(x, z, 9) < 0.4:
                put(x, SURFACE + 1, z + 1, block("jugcraft:spider_lily"))
                graves.add((x, z + 1))
    for (x, z) in top:
        if FIELD[0] <= z <= FIELD[1] and not (PATH[0] - 1 <= x <= PATH[1] + 1) and (x, z) not in graves \
                and not in_arena(x, z, -1) and _hash(x, z, 10) < 0.85:
            put(x, SURFACE + 1, z, WHEAT)
    for z in range(FIELD[0], FIELD[1] + 1, 3):
        for x in (PATH[0] - 1, PATH[1] + 1):
            put(x, SURFACE + 1, z, block("jugcraft:witchlight_path_stake", awake=True, colour="purple"))


def chapel(put):
    """The bone chapel: a roofless ruin at the north end, open to the circle on its south side."""
    (x0, x1), (z0, z1), wall = CHAPEL["x"], CHAPEL["z"], CHAPEL["wall"]
    cx = (x0 + x1 + 1) / 2.0
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            aisle = 31 <= x <= 32
            put(x, SURFACE, z, BONE if aisle else CRACKED if _hash(x, z, 11) < 0.22 else BRICKS)

    def stone(x, y, z):
        roll = _hash(x, y, z, 12)
        return CRACKED if roll < 0.25 else BLACKSTONE if roll < 0.35 else BRICKS

    # The north wall, ruined along its top, with the broken rose window.
    window = (cx, SURFACE + 7.5, 3.5)
    for x in range(x0, x1 + 1):
        height = wall - int(abs(x + 0.5 - cx) / 4) - int(_hash(x, 13) * 2)
        for y in range(SURFACE + 1, SURFACE + 1 + height):
            r = math.hypot(x + 0.5 - window[0], y + 0.5 - window[1])
            if r <= 3.6:
                roll = _hash(x, y, 14)
                if roll < 0.35:
                    put(x, y, z0, GLASS_RED if _hash(x, y, 15) < 0.5 else GLASS_PURPLE)
                continue  # the rest of the window is broken out
            put(x, y, z0, POLISHED if r <= 4.6 else stone(x, y, z0))
    # The side walls, lower toward the circle, with arched gaps.
    for x in (x0, x1):
        for z in range(z0, z1 + 1):
            height = max(2, wall - 1 - (z - z0) * 2 // 3 - int(_hash(x, z, 16) * 3))
            for y in range(SURFACE + 1, SURFACE + 1 + height):
                if z in (7, 11) and SURFACE + 2 <= y <= SURFACE + 4:
                    continue
                put(x, y, z, stone(x, y, z))
    # Ossuary walls along the inside of the side walls.
    for z in range(z0 + 2, z1 - 1):
        if z in (7, 11):
            continue
        for y in (SURFACE + 1, SURFACE + 2):
            put(x0 + 1, y, z, block("jugcraft:ossuary_wall", facing="east"))
            put(x1 - 1, y, z, block("jugcraft:ossuary_wall", facing="west"))
    # The two broken pillars at its open south side, a gargoyle on each.
    for px in (x0, x1):
        for y in range(SURFACE + 1, SURFACE + 8):
            put(px, y, z1, POLISHED)
        for part, cell in enumerate(graveyard.HEADSTONES["gargoyle"]["cells"]):
            put(px, SURFACE + 8 + cell[1], z1, block("jugcraft:gargoyle", facing="south", part=part, weathering=2, waxed=False))
    # What is left of the roof: two beams across, witchlights hanging from them.
    for bz in (6, 10):
        for x in range(x0 + 1, x1):
            if _hash(x, bz, 17) < 0.8:
                put(x, SURFACE + wall - 2, bz, BEAM)
        for x, colour in ((24, "purple"), (31, "green"), (39, "purple")):
            put(x, SURFACE + wall - 2, bz, BEAM)
            put(x, SURFACE + wall - 3, bz, block("jugcraft:hanging_witchlight", awake=True, colour=colour))
    # The Bone Throne at the head of the aisle, candelabra either side.
    tx, ty, tz = THRONE
    put(tx, ty, tz, block("jugcraft:bone_throne", facing="south", half="lower", lit=True))
    put(tx, ty + 1, tz, block("jugcraft:bone_throne", facing="south", half="upper", lit=True))
    for x in (tx - 3, tx + 4):
        put(x, ty, tz, block("jugcraft:floor_candelabrum", facing="south", half="lower"))
        put(x, ty + 1, tz, block("jugcraft:floor_candelabrum", facing="south", half="upper"))
    # Sarcophagi in the aisles, bone piles and cobwebs in the corners, sconces either side of the window.
    for x, lid in ((24, "skull"), (39, "knight")):
        put(x, SURFACE + 1, 6, block("jugcraft:blackstone_sarcophagus", facing="north", part="head", lid=lid, open=False))
        put(x, SURFACE + 1, 7, block("jugcraft:blackstone_sarcophagus", facing="north", part="foot", lid=lid, open=False))
    for x, z, layers in ((x0 + 2, z0 + 1, 3), (x1 - 2, z0 + 1, 4), (x0 + 3, z1 - 3, 2), (x1 - 3, z1 - 4, 1), (27, 9, 1)):
        put(x, SURFACE + 1, z, block("jugcraft:bone_pile", layers=layers))
    for x in (x0 + 1, x1 - 1):
        put(x, SURFACE + wall - 4, z0 + 1, COBWEB)
    for x in (int(cx) - 6, int(cx) + 5):
        put(x, SURFACE + 5, z0 + 1, block("jugcraft:skeleton_hand_sconce", facing="south", lit=True))


def lych_gate(put):
    """The graveyard's Lych Gate on the south edge, facing out, its opening filled with Grey Mist."""
    layouts = json.loads(BUILDINGS.read_text(encoding="utf-8"))
    cells = next(b["cells"] for b in layouts if b["id"] == "lych_gate")
    gx, gy, gz = GATE
    for part, (right, up, back) in enumerate(cells):
        # As GraveyardBuildingBlock.partPos facing south: "right" is east, "back" is north.
        put(gx + right, gy + up, gz - back, block("jugcraft:lych_gate", facing="south", part=part, weathering=2))
    for pos in EXIT:
        put(*pos, EXIT_MIST)


def template():
    return lair_layout.template(build(), SIZE)


def write(path):
    lair_layout.write(path, template())


if __name__ == "__main__":
    t = template()
    print(f"{len(t['blocks'])} blocks, {len(t['palette'])} states")
