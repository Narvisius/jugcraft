"""The Archive of Last Names (docs/features/crypt-archive.md): the Crypt Lich's lair, written as the structure template
data/jugcraft/structure/lair/crypt_archive.nbt, which lair/Lairs places into every instance of the jugcraft:crypt_archive
dimension. Called from tools/lair_data.py (so tools/generate_material_data.py writes it and CI checks it is current).

A vaulted crypt deep under the graves, closed all round: a box of deepslate about 59 blocks across (x) and 66 long (z)
hanging in the void, nothing of the void to be seen from inside. North is low z.
- the hall, the arena (HALL): 41 blocks square, floored with old flagstones, under a barrel vault (VAULT) that springs
  from the walls and is crossed by ribs (RIBS); four pairs of thick pillars (PILLARS) stand in its quarters, and ledger
  stones (FLOOR_GRAVES) lie in its floor, where the Lich's dead will rise;
- the four lanes (LANES): walks three blocks wide paved with polished deepslate, a line of bone down the middle, from the
  dais to the three alcoves and south to the lectern; nothing stands on them;
- the dais (DAIS): a round step of blackstone at the hall's heart, the phylactery on a plinth in its middle;
- the three alcoves (ALCOVES), one for each of the Lich's names: the Sexton's to the west, the Crowned's to the north and
  the Scribe's to the east. In each a deepslate sarcophagus lies with its head to the back wall, and on that wall a
  grave seal (the lair-only block) with the name's rune, and over it the name-stone. The north alcove is raised two
  blocks with the throne's apse, the Lich's bone throne (THRONE) before it;
- the Lectern of Last Names (LECTERN): at the foot of the stair, at the south lane's end;
- the stair (STAIR) climbing south out of the hall to the landing (LANDING), the arrival (ARRIVAL), and behind it Grey
  Mist (EXIT) filling the stair's way back up;
- the walls: the archive itself, bays of bookshelves and of ossuary niches between pilasters, soul lanterns hung on
  chains from the vault, candles in skulls, bones in the corners.
The light is hidden light blocks and the soul lanterns.

Format: tools/lair_layout.py's (26.3's). Jugcraft blocks' states are checked against their blockstate files as the
template is written (the lair-only blocks' are written with them); vanilla blocks use only properties they have had for
years. Deterministic: the same file every run.
"""
import math

import lair_layout
from lair_layout import block, hash01 as _hash

TEMPLATE = "lair/crypt_archive"

SIZE = (64, 26, 70)
PAVE = 4                           # the hall's paving layer (its flagstones)
STAND = PAVE + 1                   # where anyone in the hall stands
HALL = {"x": (12, 52), "z": (14, 54)}   # the hall's open cells (inclusive)
CENTRE = (32.5, 34.5)              # the dais's middle: the middle of cell (32, 34)
# The box: its outer cells (inclusive), solid but where the rooms are carved out of it.
BOX = {"x": (3, 61), "y": (0, 24), "z": (3, 68)}
# The vault: it springs from the hall's side walls at SPRING and rises to CROWN over the middle, a segment of a circle.
SPRING = STAND + 8
CROWN = 21
# The ribs across the vault (z rows), and the pilasters under them in the side walls.
RIBS = (17, 23, 29, 39, 45, 51)
# The lanes: three cells wide (the middle and one either side), from the dais's edge out. Each is (axis, fixed, lo, hi):
# along x at z = fixed, or along z at x = fixed, from lo to hi inclusive.
LANES = {
    "west": ("x", 34, 12, 28),
    "north": ("z", 32, 14, 30),
    "east": ("x", 34, 36, 52),
    "south": ("z", 32, 38, 52),
}
DAIS = 3.5                         # the dais's radius round CENTRE (cells whose middle is within it)
PLINTH = (32, STAND, 34)           # the plinth in the dais's middle, and the phylactery on it
PHYLACTERY = (32, STAND + 1, 34)
# Thick pillars, two by two, in pairs down the hall's quarters: (x, z) of each one's north-west cell.
PILLARS = [(19, 20), (19, 26), (44, 20), (44, 26), (19, 41), (19, 47), (44, 41), (44, 47)]
# The ledger stones in the floor, where the dead rise (part 2): (x, z) of each one's north cell; each is two long (z).
FLOOR_GRAVES = [(16, 17), (25, 17), (16, 24), (25, 24), (39, 17), (48, 17), (39, 24), (48, 24),
                (16, 42), (25, 43), (16, 49), (25, 49), (39, 43), (48, 42), (39, 49), (48, 49)]
# The alcoves. Each: its open cells (x, z ranges), the stand height of its floor, the sarcophagus (head and foot cells,
# the way it faces: foot to head), the back wall's cells it hangs on (the seal at stand + 1, the name-stone over it at
# stand + 2, both facing out), the name and the rune.
APSE_STAND = STAND + 2             # the north alcove and the throne's apse are raised two blocks
ALCOVES = {
    "sexton": {"rune": "spade", "x": (5, 11), "z": (30, 38), "stand": STAND,
               "head": (6, 34), "foot": (7, 34), "facing": "west", "wall": (5, 34), "face": "east"},
    "crowned": {"rune": "crown", "x": (26, 38), "z": (5, 13), "stand": APSE_STAND,
                "head": (32, 6), "foot": (32, 7), "facing": "north", "wall": (32, 5), "face": "south"},
    "scribe": {"rune": "quill", "x": (53, 59), "z": (30, 38), "stand": STAND,
               "head": (58, 34), "foot": (57, 34), "facing": "east", "wall": (59, 34), "face": "west"},
}
ALCOVE_CEILING = 5                 # an alcove's open height over its floor (the north alcove's is the apse's)
# The throne's apse: its steps up from the hall (z rows, lowest first), and the bone throne on it, facing the hall.
APSE_STEPS = (13, 12)
THRONE = (32, APSE_STAND, 9)
WAIT = (32.5, APSE_STAND, 9.5, 0.0)          # where the Lich waits (part 2), on his throne: x, y, z and yaw (facing south)
LECTERN = (32, STAND, 53)                     # the Lectern of Last Names, facing south (its reader stands on the stair's foot)
# The stair: its columns, and its rows rising south from the hall (each one block higher than the last), then the
# landing's columns and rows.
STAIR = {"x": (30, 34), "z": (55, 60)}
LANDING = {"x": (29, 35), "z": (61, 65)}
LANDING_STAND = STAND + (STAIR["z"][1] - STAIR["z"][0] + 1)
ARRIVAL = (32.5, LANDING_STAND, 63.5, 180.0)  # x, y, z and yaw: on the landing, facing north down the stair
# Where the Grey Mist opens on the dais when the Lich falls: two high from STAND + 1, where the phylactery stood.
DAIS_EXIT = {"x": (31, 33), "z": 34}
EXIT = {"x": (31, 33), "z": 66}               # the Grey Mist's columns and row, three blocks high from LANDING_STAND
# Where the mist throws back whoever strays: farther than BOUNDS (horizontally) from CENTRE, or below FLOOR.
BOUNDS = 40
FLOOR = PAVE - 3
# Soul lanterns hanging on chains from the vault: (x, z), each LANTERN_DROP blocks over the floor.
LANTERNS = [(x, z) for x in (16, 24, 40, 48) for z in (19, 30, 38, 49)]
LANTERN_DROP = 6
# The hidden lights: a lattice at each of these heights (y, level, spacing, staggered: every other row shifted half a
# spacing along, the rows half a spacing apart, from wall to wall), over the hall's open cells, and these: (x, y, z, level).
LIGHT_LAYERS = ((STAND + 2, 14, 8, True),)
LIGHTS = [
    (8, STAND + 3, 34, 12), (56, STAND + 3, 34, 12),             # in the side alcoves
    (30, APSE_STAND + 3, 7, 12), (34, APSE_STAND + 3, 7, 12),    # in the apse
    (32, LANDING_STAND + 2, 63, 12),                             # over the arrival
    (32, STAND + 5, 57, 11),                                     # over the stair
]
HALL_LIGHT = 7                     # the least block light on any cell where anyone stands in the hall (the audit)


# ---------------------------------------------------------------- states

DEEPSLATE = block("minecraft:deepslate_tiles")
BRICKS = block("minecraft:deepslate_bricks")
CRACKED_BRICKS = block("minecraft:cracked_deepslate_bricks")
TILES = block("minecraft:deepslate_tiles")
CRACKED_TILES = block("minecraft:cracked_deepslate_tiles")
POLISHED = block("minecraft:polished_deepslate")
CHISELED = block("minecraft:chiseled_deepslate")
RIB = block("minecraft:polished_blackstone_bricks")
BONE = {axis: block("minecraft:bone_block", axis=axis) for axis in "xyz"}
CRYPT = block("jugcraft:crypt_stone")
CRYPT_CHISELED = block("jugcraft:chiseled_crypt_stone")
PILLAR = {axis: block("jugcraft:crypt_stone_pillar", axis=axis) for axis in "xyz"}
BOOKSHELF = block("minecraft:bookshelf")
DAIS_STEP = block("minecraft:polished_blackstone_slab", type="bottom", waterlogged=False)
COBWEB = block("minecraft:cobweb")
EXIT_MIST = block("jugcraft:lair_exit", axis="x")


def stairs(facing, half="bottom"):
    return block("minecraft:deepslate_brick_stairs", facing=facing, half=half, shape="straight", waterlogged=False)


def ossuary(facing):
    return block("jugcraft:ossuary_wall", facing=facing)


def sarcophagus(facing, part):
    return block("jugcraft:deepslate_sarcophagus", facing=facing, part=part, open=False, lid="skull")


def candle_skull(facing):
    return block("jugcraft:candle_skull", facing=facing, lit=True)


def sconce(facing):
    return block("jugcraft:skeleton_hand_sconce", facing=facing, lit=True)


def bone_pile(layers):
    return block("jugcraft:bone_pile", layers=layers)


def soul_lantern():
    return block("minecraft:soul_lantern", hanging=True, waterlogged=False)


def chain():
    return block("minecraft:iron_chain", axis="y", waterlogged=False)


def throne(half):
    return block("jugcraft:bone_throne", facing="south", half=half, lit=False)


def name_stone(rune, face, glow="dark"):
    return block("jugcraft:name_stone", rune=rune, facing=face, glow=glow)


def grave_seal(rune, face, broken=False):
    return block("jugcraft:grave_seal", rune=rune, facing=face, broken=broken)


def lectern():
    return block("jugcraft:archive_lectern", facing="south")


def phylactery(cracked=False):
    return block("jugcraft:phylactery", cracked=cracked)


def light(level):
    return block("minecraft:light", level=level, waterlogged=False)


# ---------------------------------------------------------------- shapes

def in_hall(x, z):
    return HALL["x"][0] <= x <= HALL["x"][1] and HALL["z"][0] <= z <= HALL["z"][1]


def vault(x):
    """The lowest vault cell over hall column x: the vault is solid from here up."""
    half = (HALL["x"][1] - HALL["x"][0] + 2) / 2.0
    t = (x - CENTRE[0] + 0.5) / half
    return SPRING + int(round((CROWN - SPRING) * math.sqrt(max(0.0, 1.0 - t * t))))


def lane_cells(name):
    axis, fixed, lo, hi = LANES[name]
    if axis == "x":
        return {(x, z) for x in range(lo, hi + 1) for z in range(fixed - 1, fixed + 2)}
    return {(x, z) for x in range(fixed - 1, fixed + 2) for z in range(lo, hi + 1)}


def all_lane_cells():
    return set().union(*(lane_cells(name) for name in LANES))


def on_lane_line(x, z):
    """The middle line of a lane, inlaid with bone."""
    for axis, fixed, lo, hi in LANES.values():
        if axis == "x" and z == fixed and lo <= x <= hi:
            return "x"
        if axis == "z" and x == fixed and lo <= z <= hi:
            return "z"
    return None


def on_dais(x, z):
    return math.hypot(x + 0.5 - CENTRE[0], z + 0.5 - CENTRE[1]) <= DAIS


def pillar_cells():
    return {(x + dx, z + dz) for x, z in PILLARS for dx in (0, 1) for dz in (0, 1)}


def grave_cells():
    return {(x, z + dz) for x, z in FLOOR_GRAVES for dz in (0, 1)}


def in_alcove(name, x, z):
    info = ALCOVES[name]
    return info["x"][0] <= x <= info["x"][1] and info["z"][0] <= z <= info["z"][1]


def in_stair(x, z):
    return STAIR["x"][0] <= x <= STAIR["x"][1] and STAIR["z"][0] <= z <= STAIR["z"][1]


def in_landing(x, z):
    return LANDING["x"][0] <= x <= LANDING["x"][1] and LANDING["z"][0] <= z <= LANDING["z"][1]


# ---------------------------------------------------------------- building

def build():
    blocks = {}

    def put(x, y, z, state):
        if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
            blocks[(x, y, z)] = state

    def clear(x, y, z):
        blocks.pop((x, y, z), None)

    shell(put)
    hall(put, clear)
    apse(put, clear)
    alcoves(put, clear)
    stair(put, clear)
    pillars(put)
    walls(put, blocks)
    dais(put)
    lanterns(put)
    furnish(put)
    exit_mist(put)
    for y, level, spacing, staggered in LIGHT_LAYERS:
        # Rows from wall to wall: every other row (staggered) shifted half a spacing, so the lights reach both walls.
        for row, z in enumerate(range(HALL["z"][0], HALL["z"][1] + 1, spacing // 2 if staggered else spacing)):
            for x in range(HALL["x"][0] + (spacing // 2 if staggered and row % 2 == 0 else 0), HALL["x"][1] + 1, spacing):
                # A light whose cell is filled (a pillar) hangs in the first free cell beside it instead.
                for cx, cz in ((x, z), (x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                    if in_hall(cx, cz) and (cx, y, cz) not in blocks:
                        put(cx, y, cz, light(level))
                        break
    for x, y, z, level in LIGHTS:
        if (x, y, z) not in blocks:
            put(x, y, z, light(level))
    return hollow(blocks)


def shell(put):
    """The box: solid deepslate tiles, out of which the rooms are carved."""
    for x in range(BOX["x"][0], BOX["x"][1] + 1):
        for y in range(BOX["y"][0], BOX["y"][1] + 1):
            for z in range(BOX["z"][0], BOX["z"][1] + 1):
                put(x, y, z, DEEPSLATE)


def hall(put, clear):
    """The hall: its floor of flagstones (cracked here and there), the lanes and the ledger stones in it, open up to the
    vault, and the ribs under the vault."""
    lanes = all_lane_cells()
    graves = grave_cells()
    for x in range(HALL["x"][0], HALL["x"][1] + 1):
        top = vault(x)
        for z in range(HALL["z"][0], HALL["z"][1] + 1):
            line = on_lane_line(x, z)
            if line:
                put(x, PAVE, z, BONE[line])
            elif (x, z) in lanes:
                put(x, PAVE, z, POLISHED)
            elif (x, z) in graves:
                put(x, PAVE, z, CRYPT_CHISELED)
            else:
                put(x, PAVE, z, CRACKED_TILES if _hash(x, z, 11) < 0.18 else TILES)
            for y in range(STAND, top):
                clear(x, y, z)
            put(x, top, z, TILES)
            if z in RIBS:
                put(x, top - 1, z, RIB)


def alcoves(put, clear):
    """The side alcoves, carved out of the hall's west and east walls: a floor of crypt stone, the sarcophagus with its
    head to the back wall, and on the back wall the grave seal and the name-stone over it."""
    for name, info in ALCOVES.items():
        stand = info["stand"]
        for x in range(info["x"][0], info["x"][1] + 1):
            for z in range(info["z"][0], info["z"][1] + 1):
                if name != "crowned":
                    put(x, stand - 1, z, CRYPT)
                    for y in range(stand, stand + ALCOVE_CEILING):
                        clear(x, y, z)
                    put(x, stand + ALCOVE_CEILING, z, CRYPT)
        (hx, hz), (fx, fz) = info["head"], info["foot"]
        put(hx, stand, hz, sarcophagus(info["facing"], "head"))
        put(fx, stand, fz, sarcophagus(info["facing"], "foot"))
        wx, wz = info["wall"]
        put(wx, stand + 1, wz, grave_seal(info["rune"], info["face"]))
        put(wx, stand + 2, wz, name_stone(info["rune"], info["face"]))


def apse(put, clear):
    """The throne's apse at the hall's north end, raised two blocks: its steps up from the hall, its floor, the bone throne
    on it facing the hall, and behind the throne the Crowned's alcove (its sarcophagus, seal and stone are laid with the
    other alcoves', after the apse is carved)."""
    info = ALCOVES["crowned"]
    (x0, x1), (z0, z1) = info["x"], info["z"]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if z == APSE_STEPS[0]:
                put(x, STAND, z, stairs("north"))
            elif z == APSE_STEPS[1]:
                put(x, STAND, z, BRICKS)
                put(x, STAND + 1, z, stairs("north"))
            else:
                put(x, STAND, z, BRICKS)
                put(x, STAND + 1, z, CRYPT)
            start = STAND + 1 if z == APSE_STEPS[0] else STAND + 2
            for y in range(start, APSE_STAND + ALCOVE_CEILING + 2):
                clear(x, y, z)
            put(x, APSE_STAND + ALCOVE_CEILING + 2, z, CRYPT)
    tx, ty, tz = THRONE
    put(tx, ty, tz, throne("lower"))
    put(tx, ty + 1, tz, throne("upper"))


def stair(put, clear):
    """The stair climbing south from the hall to the landing, five wide, each row a step higher; the landing; their walls
    and ceilings stepped with them."""
    (x0, x1), (z0, z1) = STAIR["x"], STAIR["z"]
    for i, z in enumerate(range(z0, z1 + 1)):
        for x in range(x0, x1 + 1):
            for y in range(PAVE, STAND + i):
                put(x, y, z, BRICKS)
            put(x, STAND + i, z, stairs("south"))
            for y in range(STAND + i + 1, STAND + i + 6):
                clear(x, y, z)
    (lx0, lx1), (lz0, lz1) = LANDING["x"], LANDING["z"]
    for x in range(lx0, lx1 + 1):
        for z in range(lz0, lz1 + 1):
            put(x, LANDING_STAND - 1, z, TILES)
            for y in range(LANDING_STAND, LANDING_STAND + 5):
                clear(x, y, z)


def pillars(put):
    """The pillars, from the floor to the vault, a band of chiselled stone at their foot and their head."""
    for x, z in PILLARS:
        for dx in (0, 1):
            for dz in (0, 1):
                top = vault(x + dx)
                for y in range(STAND, top):
                    put(x + dx, y, z + dz, CHISELED if y in (STAND, top - 1) else PILLAR["y"])


def walls(put, blocks):
    """The hall's side walls as the archive: bays three high between pilasters, of bookshelves and of ossuary niches by
    turns, cracked brick over them; at each rib a pilaster up to the vault."""
    for z in range(HALL["z"][0], HALL["z"][1] + 1):
        for x, face in ((HALL["x"][0] - 1, "east"), (HALL["x"][1] + 1, "west")):
            if (x, STAND, z) not in blocks:
                continue          # an alcove's mouth
            top = vault(x + (1 if face == "east" else -1))
            rib = z in RIBS
            bay = (z - HALL["z"][0]) // 6
            for y in range(STAND, top):
                if rib:
                    put(x, y, z, PILLAR["y"])
                elif y < STAND + 3:
                    put(x, y, z, BOOKSHELF if bay % 2 == 0 else ossuary(face))
                else:
                    put(x, y, z, CRACKED_BRICKS if _hash(x, y, z, 13) < 0.2 else BRICKS)
    # The end walls: bookshelves, and ossuary niches either side of the stair's mouth and of the apse's.
    for x in range(HALL["x"][0], HALL["x"][1] + 1):
        for z, face, mouth in ((HALL["z"][1] + 1, "north", STAIR["x"]), (HALL["z"][0] - 1, "south", ALCOVES["crowned"]["x"])):
            if mouth[0] <= x <= mouth[1]:
                continue
            for y in range(STAND, STAND + 3):
                put(x, y, z, ossuary(face) if abs(x - CENTRE[0] + 0.5) < 10 else BOOKSHELF)


def dais(put):
    """The dais: a round step of blackstone slabs, the plinth in its middle and the phylactery on it."""
    for x in range(HALL["x"][0], HALL["x"][1] + 1):
        for z in range(HALL["z"][0], HALL["z"][1] + 1):
            if on_dais(x, z):
                put(x, STAND, z, DAIS_STEP)
    put(*PLINTH, CRYPT_CHISELED)
    put(*PHYLACTERY, phylactery())


def lanterns(put):
    """Soul lanterns hung on chains from the vault, LANTERN_DROP blocks over the floor."""
    for x, z in LANTERNS:
        y = STAND + LANTERN_DROP
        put(x, y, z, soul_lantern())
        for cy in range(y + 1, vault(x)):
            put(x, cy, z, chain())


def furnish(put):
    """The throne's sconces, candles in skulls and bones in the alcoves and the hall's corners, the lectern at the stair's
    foot, cobwebs high in the vault's corners."""
    put(*LECTERN, lectern())
    # Sconces either side of the throne, on the apse's back corners, and either side of each side alcove's mouth.
    for x in (27, 37):
        put(x, APSE_STAND + 2, 5, sconce("south"))
    for name in ("sexton", "scribe"):
        info = ALCOVES[name]
        mouth = info["x"][1] + 1 if name == "sexton" else info["x"][0] - 1
        for z in (info["z"][0] - 1, info["z"][1] + 1):
            put(mouth, STAND + 3, z, sconce("east" if name == "sexton" else "west"))
        # Candles in skulls on the alcove's floor either side of the sarcophagus, bones in its corners.
        hx, hz = info["head"]
        for dz in (-2, 2):
            put(hx, STAND, hz + dz, candle_skull(info["face"]))
        back, inward = (info["x"][0], 1) if name == "sexton" else (info["x"][1], -1)
        for z in (info["z"][0], info["z"][1]):
            put(back, STAND, z, bone_pile(3))
            put(back + inward, STAND, z, bone_pile(1))
    for x in (28, 36):
        put(x, APSE_STAND, 6, candle_skull("south"))
    # Bones and candles in skulls in the hall's corners.
    for x, z, face in ((HALL["x"][0], HALL["z"][0], "east"), (HALL["x"][1], HALL["z"][0], "west"),
                       (HALL["x"][0], HALL["z"][1], "east"), (HALL["x"][1], HALL["z"][1], "west")):
        put(x, STAND, z, bone_pile(4))
        dz = 1 if z == HALL["z"][0] else -1
        put(x, STAND, z + dz, bone_pile(2))
        put(x + (1 if face == "east" else -1), STAND, z, candle_skull(face))
    # Candles in skulls either side of the stair's foot.
    for x in (STAIR["x"][0] - 1, STAIR["x"][1] + 1):
        put(x, STAND, HALL["z"][1], candle_skull("north"))
    # Cobwebs where the vault meets the end walls.
    for x in range(HALL["x"][0], HALL["x"][1] + 1):
        top = vault(x)
        for z in (HALL["z"][0], HALL["z"][1]):
            if _hash(x, z, 17) < 0.35:
                put(x, top - 1, z, COBWEB)
    # Sconces on the stair's walls, every other row.
    for i, z in enumerate(range(STAIR["z"][0], STAIR["z"][1] + 1)):
        if i % 2 == 1:
            put(STAIR["x"][0], STAND + i + 3, z, sconce("east"))
            put(STAIR["x"][1], STAND + i + 3, z, sconce("west"))


def exit_mist(put):
    """The Grey Mist at the landing's south end, filling the doorway of the stair's way back up."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    for x in range(x0, x1 + 1):
        put(x, LANDING_STAND - 1, z, TILES)
        for y in range(LANDING_STAND, LANDING_STAND + 3):
            put(x, y, z, EXIT_MIST)


def exit_cells():
    """Where the Grey Mist is (for the audit and the tests)."""
    (x0, x1), z = EXIT["x"], EXIT["z"]
    return [(x, y, z) for y in range(LANDING_STAND, LANDING_STAND + 3) for x in range(x0, x1 + 1)]


def seal_cells():
    """Each name's grave seal: {name: (x, y, z)} (for the audit, the tests and the Java)."""
    return {name: (info["wall"][0], info["stand"] + 1, info["wall"][1]) for name, info in ALCOVES.items()}


def stone_cells():
    """Each name's name-stone: {name: (x, y, z)}."""
    return {name: (info["wall"][0], info["stand"] + 2, info["wall"][1]) for name, info in ALCOVES.items()}


def hollow(blocks):
    """Leaves out blocks nobody can see: those whose six neighbours are all whole, opaque blocks of the template."""
    opaque = {DEEPSLATE, BRICKS, CRACKED_BRICKS, TILES, CRACKED_TILES, POLISHED, CHISELED, CRYPT, CRYPT_CHISELED, RIB,
              *BONE.values(), *PILLAR.values(), BOOKSHELF}
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
