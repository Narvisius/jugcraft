"""Drone Tower design (proposal stage, v7): the block layout of each of the 9 tower tiers, plus previews.

The tower holds 100 drones and every drone has its own hangar, sized to fit it:
  small  (tiers 1-3) 3 wide x 3 deep x 3 tall inside
  medium (tiers 4-6) 6 wide x 4 deep x 4 tall
  large  (tiers 7-9) 7 wide x 7 deep x 5 tall (a large drone is as big as one 5x5 pad)
The 8 tier-1 drones keep the 8 ground pads (one each). Every other drone lives in a hangar with a bay
door in the tower's outer wall: it flies straight out of the door, clear of the tower, and then climbs.
Drones leave empty and grab their crates at supply pickups that stick out of the tower on several sides.

Layout (x east, z south, y up), on a 39x39 landing field:
- Tier 1, Command Post: the 15x15 chiseled-stone plinth with the fortified command post (the control
  room: terminal, hologram table, screen wall) in the middle; 8 ground pads round it, 2 per side, and the
  ground supply pickup in the south-east corner.
- Tier 2, Hangar Deck: a wide deck over the whole field, the pads and the command post, standing on raked
  corner legs and slim girders that players walk between to reach the command post. Its rim is a ring of
  24 small hangars, 6 per side, doors facing out. Ground drones fly out sideways under it.
- Tiers 3-9: hangar floors stacked into one continuous octagonal core (26x26, chamfered corners with
  glowing seams). Each floor is a ring of hangars in a pinwheel (each side's row starts at a different
  corner, so every door faces out and none is blocked) round a hollow atrium with the lift.
- Four great sloping buttresses, after the reference image's central tower: thick blades standing on
  footings outside the field, sweeping up in a concave curve to meet the tower at different heights. Each
  one is different, so it doesn't read as a symmetric four-legged frame. Supply pickups stand out
  from the buttresses on different sides, with open sky above them.
- Tier 9 adds a curved shoulder into a slender, nearly straight shaft and a needle with the beacon. No
  external pads at the top: all 8 tier-9 drones are inside.

Materials per tier (real or real-theoretical):
  1 chiseled stone, stone bricks, iron        6 carbon-fibre composite (pitch fibre from bitumen)
  2 reinforced concrete, steel girders         7 silicon carbide armour (silicon + coke, Acheson process)
  3 steel armour plate, blast glass            8 depleted uranium armour (uranium enrichment tailings)
  4 aluminium alloy cladding                   9 graphene lattice, YBCO superconducting conduit
  5 tungsten-steel frame

This file only describes and previews the design. `python tools/drone_tower.py` writes preview images to
build/drone_tower/ (not part of the mod yet).
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 39
C = SIZE // 2  # centre column, 19
TIER_NAMES = ["Command Post", "Hangar Deck", "Armoured Block", "Logistics Spire", "Frame Tower",
              "Composite Tower", "Ceramic Bastion", "Uranium Citadel", "Graphene Spire"]
SHELL = ["stone_bricks", "reinforced_concrete", "steel_armor", "aluminum_cladding", "tungsten_frame",
         "carbon_composite", "sic_armor", "du_armor", "graphene_lattice"]

# Inside size of a hangar: (door width, depth, height).
HANGAR = {"small": (3, 3, 3), "medium": (6, 4, 4), "large": (7, 7, 5)}
TIER_SIZE = {1: "small", 2: "small", 3: "small", 4: "medium", 5: "medium", 6: "medium",
             7: "large", 8: "large", 9: "large"}
# Hangar floors above the deck: (tier, hangars on the N, E, S, W sides). Each tier's floors add up to
# its drone count.
FLOORS = [(3, (3, 3, 3, 3)),
          (4, (2, 1, 2, 1)), (4, (1, 2, 1, 2)),
          (5, (2, 1, 1, 1)), (5, (1, 1, 2, 1)),
          (6, (1, 2, 1, 1)), (6, (1, 1, 1, 2)),
          (7, (1, 1, 1, 1)), (7, (1, 1, 1, 1)),
          (8, (1, 1, 1, 1)), (8, (1, 1, 1, 1)),
          (9, (1, 1, 1, 1)), (9, (1, 1, 1, 1))]
CHAMFER = 5         # the core's corners are cut off on the diagonal (glowing seams run up them)
CORE = 26           # the hangar core is one continuous 24x24 shaft (every floor the same size)
DECK_BOTTOM = 12    # underside of the tier 2 hangar deck
DECK_TOP = 16       # roof of the deck
SPIRE_TOP = 214
# The great curved buttresses (after the reference's central tower): one per corner, from a foot well
# outside the field, sweeping up in a concave arc to meet the tower. (corner, height they join at).
# One per corner, each different (where it joins, how far out its foot stands), so it doesn't read as a
# symmetric four-legged frame: (corner, join height, foot distance on the diagonal).
BUTTRESSES = [((1, 1), 150, 26), ((-1, 1), 118, 22), ((-1, -1), 176, 28), ((1, -1), 134, 24)]
SHAFT_TOP = 186     # the slender shaft rises nearly straight to here, then the needle
# Supply pickups standing out from the buttresses on different sides: (tier, buttress index, height).
RIB_PICKUPS = [(4, 0, 30), (6, 2, 56), (8, 1, 82), (9, 3, 110), (9, 0, 138)]

# Drones each tier adds (100 in total), and where they live.
FLOOR_CAPACITY = {1: (32, "4 per ground pad, 8 pads"), 2: (24, "24 small hangars, deck rim"),
                  3: (12, "12 small hangars"), 4: (12, "12 medium hangars"), 5: (10, "10 medium hangars"),
                  6: (10, "10 medium hangars"), 7: (8, "8 large hangars"), 8: (8, "8 large hangars"),
                  9: (8, "8 large hangars (all inside)")}
for _t in range(3, 10):
    assert sum(sum(arms) for t, arms in FLOORS if t == _t) == FLOOR_CAPACITY[_t][0]
# Large hangars stacked up the spire (tier 9), one per level, doors turning N, E, S, W as they rise, a block
# apart: (level floor y, side). Filled in once the spire's start is known (spire_hangar_levels below).
SPIRE_HANGAR_GAP = 1

# Approximate colour of each material (top face) for the previews; side faces are shaded.
COLORS = {
    "chiseled_stone": (128, 128, 128), "stone_bricks": (116, 116, 118), "iron": (200, 200, 205),
    "tech_window": (70, 60, 62), "light_cyan": (150, 38, 30), "light_red": (235, 62, 44),
    "reinforced_concrete": (150, 150, 146), "steel_girder": (84, 88, 96), "hazard_plating": (226, 182, 40),
    "hangar_door": (60, 66, 76), "steel_armor": (72, 78, 88), "blast_glass": (46, 40, 44),
    "aluminum_cladding": (170, 178, 188), "tungsten_frame": (58, 60, 66), "carbon_composite": (34, 36, 42),
    "sic_armor": (52, 58, 70), "du_armor": (44, 50, 44), "graphene_lattice": (28, 30, 36),
    "sc_conduit": (120, 28, 26), "landing_platform": (100, 104, 110), "landing_pad": (24, 24, 28),
    "supply_pickup": (200, 160, 40), "cargo_packager": (220, 220, 225), "drone": (150, 150, 158),
    "hangar_floor": (30, 26, 28), "cargo_chute": (84, 88, 96), "access_floor_tile": (88, 90, 94),
    "carpet_tile": (40, 42, 46), "acoustic_wall_panel": (62, 64, 68), "concrete_column": (132, 132, 128),
    "holo_table": (60, 62, 66), "hangar_pad": (70, 50, 48), "ceiling_light_panel": (236, 236, 228), "control_screen": (26, 32, 34),
    "drone_depot_terminal": (70, 72, 76), "console_desk": (70, 72, 76), "operator_chair": (30, 30, 33),
    "equipment_rack": (28, 28, 30), "cable_tray": (150, 154, 160),
}


def colour(material):
    return COLORS.get(material.split("[")[0], (255, 0, 255))
GLOWING = {"light_cyan", "light_red", "sc_conduit"}


# Set by export(): collects the docks and pickups of each tier for the mod's tower data.
META = None
# Previews park a drone in some hangars; the exported structure doesn't.
PREVIEW = True


def schedule():
    """Heights: [(tier, kind, y0, y1, info)] for the command post, the deck, each collar and floor, the crown."""
    parts = [(1, "post", 0, 11, None), (2, "deck", DECK_BOTTOM, DECK_TOP + 1, None)]
    y = DECK_TOP + 1
    last = 2
    for i, (tier, arms) in enumerate(FLOORS):
        h = HANGAR[TIER_SIZE[tier]][2]
        parts.append((tier, "floor", y, y + h + 2, i))
        y += h + 2
    parts.append((9, "spire", y, SPIRE_TOP + 1, None))
    return parts


def tier_tops():
    tops = [0] * 10
    for tier, kind, y0, y1, _ in schedule():
        if kind != "spire":
            tops[tier] = max(tops[tier], y1)
    tops[9] = SPIRE_TOP + 1
    return tops


TIER_TOPS = tier_tops()


def cross_section(r, squash=1.0, n=2.6):
    """Rounded-square cross-section."""
    cells = set()
    span = int(math.ceil(r)) + 1
    for dx in range(-span, span + 1):
        for dz in range(-span, span + 1):
            if abs(dx / r) ** n + abs(dz / (r * squash)) ** n <= 1:
                cells.add((dx, dz))
    return cells


def put(blocks, x, y, z, material, keep_lights=True):
    if keep_lights and blocks.get((x, y, z)) in ("light_cyan", "light_red"):
        return
    blocks[(x, y, z)] = material


# ---------------------------------------------------------------- hangar rings

def floor_square(index):
    """(x0, z0, S) of hangar floor {@code index}: every floor is the same CORE x CORE square."""
    tier, arms = FLOORS[index]
    w, d, _ = HANGAR[TIER_SIZE[tier]]
    assert CHAMFER + max(arms) * (w + 1) + 1 + d + 2 <= CORE
    return C - CORE // 2, C - CORE // 2, CORE


def local(x0, z0, size, side, u, v, mirror):
    """Block position of point (u along the side, v in from the face) on side 0 N, 1 E, 2 S, 3 W of a
    size x size square; {@code mirror} flips the pinwheel's direction."""
    x, z = [(u, v), (size - 1 - v, u), (size - 1 - u, size - 1 - v), (v, size - 1 - u)][side]
    if mirror:
        x = size - 1 - x
    return x0 + x, z0 + z


# The open space inside every hangar built so far (its floor up to its ceiling, bay included). Whatever a later
# part of the tower puts there (a floor above, a buttress, a corner blade) is carved out again at the end of
# build_raw, so nothing ever stands on a hangar pad or in a drone's way.
HANGAR_SPACE = set()
# Each hangar's pad plates, {pos: material}: put back at the end of build_raw over anything laid on them since.
HANGAR_PADS = {}


def hangar_row(blocks, y0, x0, z0, size, side, count, kind, material, mirror, start=0, seed=0, tier=None):
    """A row of {@code count} hangars along one side, starting {@code start} blocks from its corner: dividing
    walls, back wall, an open bay with a hazard frame and a light strip over it. Previews park a drone in
    some of them. Each hangar is recorded as a dock (its floor centre and the point outside its bay)."""
    w, d, h = HANGAR[kind]
    for k in range(count):
        u0 = start + k * (w + 1)
        is_open = True
        if META is not None:
            a, b = local(x0, z0, size, side, u0 + 1, 1, mirror), local(x0, z0, size, side, u0 + w, d, mirror)
            e1, e2 = local(x0, z0, size, side, u0 + 1, -2, mirror), local(x0, z0, size, side, u0 + w, -2, mirror)
            # The bay opening (for the roll-up door): its two end cells, its bottom and height, and which way is out.
            d1, d2 = local(x0, z0, size, side, u0 + 1, 0, mirror), local(x0, z0, size, side, u0 + w, 0, mirror)
            o = local(x0, z0, size, side, u0 + 1, -1, mirror)
            META["docks"].append({"tier": tier, "size": kind, "x": (a[0] + b[0]) / 2 + 0.5, "y": y0 + 1,
                                  "z": (a[1] + b[1]) / 2 + 0.5, "ex": (e1[0] + e2[0]) / 2 + 0.5, "ez": (e1[1] + e2[1]) / 2 + 0.5,
                                  "door": [min(d1[0], d2[0]), min(d1[1], d2[1]), max(d1[0], d2[0]), max(d1[1], d2[1]), y0 + 1, h,
                                           o[0] - d1[0], o[1] - d1[1]]})
        cells = [local(x0, z0, size, side, u, v, mirror) for u in range(u0 + 1, u0 + w + 1) for v in range(1, d + 1)]
        for u in range(u0 + 1, u0 + w + 1):
            for v in range(0, d + 1):
                cx, cz = local(x0, z0, size, side, u, v, mirror)
                for y in range(y0 + 1, y0 + h + 1):
                    HANGAR_SPACE.add((cx, y, cz))
        pad_box = (min(c[0] for c in cells), min(c[1] for c in cells), max(c[0] for c in cells), max(c[1] for c in cells))
        for u in range(u0, u0 + w + 2):
            wall = u in (u0, u0 + w + 1)
            for v in range(0, d + 2):
                for y in range(y0 + 1, y0 + h + 1):
                    x, z = local(x0, z0, size, side, u, v, mirror)
                    if wall or v == d + 1:
                        put(blocks, x, y, z, material)
                    elif v == 0:
                        if is_open:
                            blocks.pop((x, y, z), None)
                        else:
                            put(blocks, x, y, z, "hangar_door", keep_lights=False)
                    else:
                        blocks.pop((x, y, z), None)
                x, z = local(x0, z0, size, side, u, v, mirror)
                if not wall and 1 <= v <= d:
                    # The landing pad: the whole hangar floor is one pad, each plate showing its own part of it
                    # (hazard rim, edge light, touchdown marks, charger), like the ground pads (tower_art.py).
                    pad = f"hangar_pad[part={pad_part(kind, pad_box, x, z)}]"
                    put(blocks, x, y0, z, pad, keep_lights=False)
                    HANGAR_PADS[(x, y0, z)] = pad
            x, z = local(x0, z0, size, side, u, 0, mirror)
            put(blocks, x, y0 + h + 1, z, "hazard_plating" if wall else "light_cyan", keep_lights=False)
        if PREVIEW and (k + seed) % 3 == 0:  # the drone parked inside
            dw, dd, dh = {"small": (2, 2, 1), "medium": (5, 3, 2), "large": (5, 5, 3)}[kind]
            for u in range(u0 + 1 + (w - dw) // 2, u0 + 1 + (w - dw) // 2 + dw):
                for v in range(1, 1 + dd):
                    for y in range(y0 + 1, y0 + 1 + dh):
                        x, z = local(x0, z0, size, side, u, v, mirror)
                        blocks[(x, y, z)] = "drone"
            x, z = local(x0, z0, size, side, u0 + 1 + w // 2, 1, mirror)
            blocks[(x, y0 + dh, z)] = "light_red"


def pad_part(kind, box, x, z):
    """The hangar pad part (tower/HangarPadBlock.PART) for the plate at (x, z) of a hangar floor covering ``box``."""
    import tower_art
    wx, dz = box[2] - box[0] + 1, box[3] - box[1] + 1
    return tower_art.hangar_pad_part(kind, wx, dz, x - box[0], z - box[1])


def hangar_floor(blocks, index, y0):
    """One hangar floor: floor slab and roof over the whole square, an outer wall with glass bands where
    there are no hangars, the pinwheel of hangars, a roof brow, and sloped corner blades on two corners
    (they turn round the tower from floor to floor)."""
    tier, arms = FLOORS[index]
    kind = TIER_SIZE[tier]
    w, d, h = HANGAR[kind]
    material = SHELL[tier - 1]
    x0, z0, size = floor_square(index)
    mirror = index % 2 == 1
    def corner_dist(x, z):
        return min(x - x0, x0 + size - 1 - x) + min(z - z0, z0 + size - 1 - z)

    for x in range(x0, x0 + size):
        for z in range(z0, z0 + size):
            cd = corner_dist(x, z)
            if cd <= CHAMFER:
                continue
            put(blocks, x, y0, z, material)
            put(blocks, x, y0 + h + 1, z, material)
            edge = x in (x0, x0 + size - 1) or z in (z0, z0 + size - 1) or cd == CHAMFER + 1
            if edge:
                for y in range(y0 + 1, y0 + h + 1):
                    put(blocks, x, y, z, "blast_glass" if y == y0 + 2 and (x + z) % 3 else material)
    for side, count in enumerate(arms):
        hangar_row(blocks, y0, x0, z0, size, side, count, kind, material, mirror, start=CHAMFER, seed=index + side, tier=tier)
    if index == 0 or FLOORS[index - 1][0] != tier:  # a thin flush glowing band where each tier starts
        for x in range(x0, x0 + size):
            for z in range(z0, z0 + size):
                if corner_dist(x, z) > CHAMFER and (x in (x0, x0 + size - 1) or z in (z0, z0 + size - 1)
                                                    or corner_dist(x, z) == CHAMFER + 1):
                    put(blocks, x, y0, z, "sc_conduit")
    # Glowing seams run straight up the middle of the four chamfered corners.
    for x in range(x0, x0 + size):
        for z in range(z0, z0 + size):
            if corner_dist(x, z) == CHAMFER + 1 and abs((x - x0 if x - x0 < size / 2 else x0 + size - 1 - x)
                                                       - (z - z0 if z - z0 < size / 2 else z0 + size - 1 - z)) <= 1:
                for y in range(y0, y0 + h + 2):
                    put(blocks, x, y, z, "light_cyan", keep_lights=False)

# ---------------------------------------------------------------- tier 1 and 2

def base_field(blocks):
    """The 39x39 landing field: platform, 8 pads (2 per side), ground pickup and packager (south-east)."""
    for x in range(SIZE):
        for z in range(SIZE):
            if max(abs(x - C), abs(z - C)) > 7:
                blocks[(x, 0, z)] = "landing_platform"
    for side in range(4):
        for offset in (-6, 2):
            for a in range(5):
                for b in range(6, 11):
                    u, v = C + offset + a, b
                    x, z = [(u, v), (SIZE - 1 - v, u), (SIZE - 1 - u, SIZE - 1 - v), (v, SIZE - 1 - u)][side]
                    blocks[(x, 1, z)] = "landing_pad"
    for x in range(29, 32):
        for z in range(29, 32):
            blocks[(x, 1, z)] = "supply_pickup"
    blocks[(32, 1, 30)] = "cargo_packager"


def command_post(blocks):
    """Tier 1: the fortified command post on the 15x15 plinth, and the command room inside it (11 x 11,
    9 high): a two-layer wall (stone brick outside, acoustic panels between concrete columns inside, slit
    windows), an open blast-door bay in the south wall, the plotting table (hologram table) over the Tower
    Core, the video wall (3x2 screen) on the north wall, the command desk with the Drone Depot Terminal on
    the west side facing the table, a row of three operator stations facing the video wall, equipment racks,
    a lift shaft (decor), ceiling light panels and cable trays. Real furniture blocks only."""
    for dx in range(-7, 8):
        for dz in range(-7, 8):
            blocks[(C + dx, 0, C + dz)] = "chiseled_stone"
    outer = {(dx, dz) for dx in range(-7, 8) for dz in range(-7, 8) if (abs(dx) / 7.45) ** 4 + (abs(dz) / 7.45) ** 4 <= 1}
    inner = {(dx, dz) for dx in range(-6, 7) for dz in range(-6, 7) if (abs(dx) / 6.45) ** 4 + (abs(dz) / 6.45) ** 4 <= 1}
    room = {(dx, dz) for dx in range(-5, 6) for dz in range(-5, 6)}
    for dx, dz in room:
        work = (dx <= -3 and abs(dz) <= 2) or (dx >= 2 and 3 <= dz <= 4)
        blocks[(C + dx, 0, C + dz)] = "carpet_tile" if work else "access_floor_tile"
    for y in range(1, 11):
        for dx, dz in outer:
            if y == 10:
                blocks[(C + dx, y, C + dz)] = "stone_bricks" if (dx, dz) not in inner else "steel_armor"
            elif (dx, dz) not in inner:
                window = y in (4, 5) and dz != -7 and dx != -7 and (dx + dz) % 4 == 0
                blocks[(C + dx, y, C + dz)] = "blast_glass" if window else "stone_bricks"
            elif (dx, dz) not in room:
                column = (abs(dx) == 6 and dz in (-6, -3, 0, 3, 6)) or (abs(dz) == 6 and dx in (-6, -3, 0, 3, 6))
                window = y in (4, 5) and dz != -6 and dx != -6 and (dx + dz) % 4 == 0 and not column
                band = y == 6 and not column and (dx + dz) % 2 == 0
                blocks[(C + dx, y, C + dz)] = ("concrete_column" if column else "blast_glass" if window
                                               else "light_cyan" if band else "steel_armor" if y == 6 else "acoustic_wall_panel")
    # Sloped corner buttresses outside.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        for y in range(1, 5):
            for k in range(0, 5 - y):
                put(blocks, C + sx * (7 - k // 2), y, C + sz * (5 + (k + 1) // 2), "chiseled_stone")
    # Blast-door bay (open, 3 wide and 3 high) in the south wall, with a hazard frame.
    for y in range(1, 4):
        for dx in (-1, 0, 1):
            for dz in (6, 7):
                blocks.pop((C + dx, y, C + dz), None)
    for dz in (6, 7):
        for y in range(1, 5):
            blocks[(C - 2, y, C + dz)] = "hazard_plating"
            blocks[(C + 2, y, C + dz)] = "hazard_plating"
        for dx in (-1, 0, 1):
            blocks[(C + dx, 4, C + dz)] = "hazard_plating"
    blocks[(C, 5, C + 7)] = "light_red"
    # No slit windows right beside the door bay's warning light.
    for dx in range(-3, 4):
        for y in (4, 5):
            if blocks.get((C + dx, y, C + 7)) == "blast_glass":
                blocks[(C + dx, y, C + 7)] = "stone_bricks"
            if blocks.get((C + dx, y, C + 6)) == "blast_glass":
                blocks[(C + dx, y, C + 6)] = "acoustic_wall_panel"
    # Plotting table over the core, video wall, command desk with the terminal, operator row, racks.
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            blocks[(C + dx, 1, C + dz)] = "holo_table"
    for dx in (-1, 0, 1):
        for y in (3, 4):
            blocks[(C + dx, y, C - 5)] = "control_screen[facing=south]"
    blocks[(C - 3, 1, C)] = "drone_depot_terminal[facing=east]"
    for dz in (-1, 1):
        blocks[(C - 3, 1, C + dz)] = "console_desk[facing=east]"
        blocks[(C - 4, 1, C + dz)] = "operator_chair[facing=east]"
    for dx in (2, 3, 4):
        blocks[(C + dx, 1, C + 3)] = "console_desk[facing=north]"
        blocks[(C + dx, 1, C + 4)] = "operator_chair[facing=north]"
    for dz in (3, 4):
        for y in (1, 2):
            blocks[(C - 5, y, C + dz)] = "equipment_rack[facing=west]"
    # Lift shaft in the north-east corner (glass and steel; the lift itself comes with the hangar floors).
    for y in range(1, 10):
        blocks[(C + 4, y, C - 4)] = "blast_glass"
        blocks[(C + 5, y, C - 4)] = "steel_girder"
        blocks[(C + 4, y, C - 5)] = "steel_girder"
    # Ceiling: light panels and cable trays under the roof.
    for dx, dz in ((-3, -3), (0, -3), (3, -3), (-3, 0), (0, 0), (3, 0), (-3, 3), (0, 3), (3, 3),
                   (-5, -5), (5, -5), (-5, 5), (5, 5), (0, 5), (-5, 0)):
        blocks[(C + dx, 9, C + dz)] = "ceiling_light_panel"
    for dx in range(-5, 4):
        blocks[(C + dx, 9, C - 1)] = "cable_tray[facing=north]"


def hangar_deck(blocks):
    """Tier 2: a wide deck over the whole field, the 8 pads and the command post (slab, roof, chamfered
    corners), whose rim is a ring of 24 small hangars, 6 per side, doors facing out. It stands on raked
    corner legs, legs under the rim and slim girders between the pad pairs; players walk in between them,
    and ground drones fly out sideways under it. A pickup sticks out of the south-west corner."""
    kind, material = "small", "reinforced_concrete"
    y0 = DECK_BOTTOM
    for x in range(SIZE):
        for z in range(SIZE):
            dx, dz = abs(x - C), abs(z - C)
            if dx + dz > 34:
                continue
            put(blocks, x, y0, z, material)
            put(blocks, x, DECK_TOP, z, material)
            edge = max(dx, dz) == C or dx + dz >= 33
            if edge:
                for y in range(y0 + 1, DECK_TOP):
                    put(blocks, x, y, z, "light_cyan" if y == y0 + 1 else material)
                put(blocks, x, DECK_TOP + 1, z, "hazard_plating" if (x + z) % 2 else "steel_girder")
            # Girder ribs under the deck, between the pad pairs.
            if (dx == 0 or dz == 0) and max(dx, dz) <= 17:
                put(blocks, x, y0 - 1, z, "steel_girder")
    for side in range(4):
        hangar_row(blocks, y0, 0, 0, SIZE, side, 6, kind, material, False, start=7, seed=side, tier=2)
    # Raked corner legs (2x2 girders leaning in as they rise), legs under the rim, girders between pads.
    for sx, sz in ((-1, -1), (-1, 1), (1, 1), (1, -1)):
        for y in range(1, y0):
            lean = 16.5 - 3 * y / y0
            x, z = C + sx * round(lean), C + sz * round(lean)
            for ax in (0, -sx):
                for az in (0, -sz):
                    put(blocks, x + ax, y, z + az, "steel_girder", keep_lights=False)
            if y % 4 == 3:
                put(blocks, x + sx, y, z + sz, "light_red", keep_lights=False)
    for side in range(4):
        for u, v in ((9, 2), (29, 2), (C, 8)):
            x, z = [(u, v), (SIZE - 1 - v, u), (SIZE - 1 - u, SIZE - 1 - v), (v, SIZE - 1 - u)][side]
            for y in range(1, y0):
                put(blocks, x, y, z, "steel_girder", keep_lights=False)
    pickup_deck(blocks, DECK_TOP, (-1, 1), 16, "reinforced_concrete")


# ---------------------------------------------------------------- extras

def pickup_deck(blocks, y, corner, reach, material):
    """A supply pickup sticking out of the tower on a diagonal: a 3x3 pickup on a braced 5x5 platform with
    a hazard rim, warning lights and a cargo chute up from the tower's storage. Drones that left their
    hangars empty collect their crates here."""
    sx, sz = corner
    px, pz = C + sx * reach, C + sz * reach
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            rim = max(abs(dx), abs(dz)) == 2
            put(blocks, px + dx, y, pz + dz, "hazard_plating" if rim else material, keep_lights=False)
            if not rim:
                put(blocks, px + dx, y + 1, pz + dz, "supply_pickup", keep_lights=False)
    for k in (-2, 2):
        put(blocks, px + sx * 2, y + 1, pz + k, "light_red", keep_lights=False)
        put(blocks, px + k, y + 1, pz + sz * 2, "light_red", keep_lights=False)
    for h in range(1, 4):
        put(blocks, px - sx * 2, y + h, pz - sz * 2, "cargo_chute", keep_lights=False)
    if META is not None:
        META["pickups"].append({"tier": 2, "x": px - 1, "y": y + 1, "z": pz - 1})
    # A boom back to the tower, and stepped braces under the platform.
    for k in range(CORE // 2, reach - 1):
        put(blocks, C + sx * k, y, C + sz * k, material)
        put(blocks, C + sx * k, y - 1, C + sz * k, "steel_girder")
    for step in range(1, 4):
        put(blocks, px - sx * step, y - step, pz - sz * step, "steel_girder")


def spire_half(y, y0):
    """Half-width of the spire at height y (it starts at the top of the hangar core, y0)."""
    # A curved shoulder from the wide hangar core into a slender shaft that rises nearly straight, then
    # the needle to the beacon.
    if y < SHAFT_TOP:
        t = (y - y0) / (SHAFT_TOP - y0)
        return 4.3 + 2.4 * (1 - t) + (CORE / 2 - 6.7) * math.exp(-(y - y0) / 6.0)
    t = (y - SHAFT_TOP) / (SPIRE_TOP - SHAFT_TOP)
    return 0.5 + 3.8 * (1 - t) ** 1.3


def core_reach(y, y0):
    """Diagonal distance from the centre to the core's chamfered corner at height y."""
    if y < y0:
        return CORE / 2 - CHAMFER / 2
    return spire_half(y, y0) * 0.78


def rib_k(y, join, foot, y0):
    """Diagonal distance of a buttress' centre line at height y: it flares out only near the ground and
    otherwise runs close up the tower (a tight concave sweep), like the reference's ribs."""
    end = core_reach(join, y0) + 1.6
    return end + (foot - end) * max(0.0, 1 - y / join) ** 2.5


def buttresses(blocks, top, y0):
    """The four great buttresses: thick at the foot, slimmer as they sweep up and in to meet the tower,
    with a dim red seam along their outer edge and a footing outside the field. Each one is built as far
    up as the tower has been."""
    for (sx, sz), join, foot in BUTTRESSES:
        for dx in range(-6, 7):
            for dz in range(-6, 7):
                if abs(dx) + abs(dz) <= 8:
                    blocks[(C + sx * foot + dx, 0, C + sz * foot + dz)] = "reinforced_concrete"
        for y in range(1, min(top, join + 1)):
            k = rib_k(y, join, foot, y0)
            # A blade: deep along the diagonal, thin across it, slimming as it rises.
            depth = 6.0 - 3.6 * y / join
            half = 2.6 - 1.4 * y / join
            material = SHELL[min(8, tier_at(y) - 1)]
            cells = set()
            for a in range(-13, 14):
                for b in range(-6, 7):
                    along, across = a * 0.5, b * 0.5
                    if abs(along) <= depth and abs(across) <= half:
                        kk = k + along
                        cells.add((round(C + sx * (kk + across)), round(C + sz * (kk - across))))
            # Hollow inside (a skin one block thick), so it costs what it shows.
            for x, z in cells:
                if any((x + ex, z + ez) not in cells for ex, ez in ((1, 0), (-1, 0), (0, 1), (0, -1))) or y % 8 == 0:
                    put(blocks, x, y, z, material)
            put(blocks, round(C + sx * (k + depth)), y, round(C + sz * (k + depth)), "light_cyan", keep_lights=False)


def rib_pickup(blocks, index, y, y0):
    """A supply pickup standing out from a buttress: open sky above it, as the buttress leans in."""
    (sx, sz), join, foot = BUTTRESSES[index]
    k = rib_k(y, join, foot, y0) + 6
    px, pz = C + sx * round(k), C + sz * round(k)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            rim = max(abs(dx), abs(dz)) == 2
            put(blocks, px + dx, y, pz + dz, "hazard_plating" if rim else "steel_armor", keep_lights=False)
            if not rim:
                put(blocks, px + dx, y + 1, pz + dz, "supply_pickup", keep_lights=False)
    for kk in (-2, 2):
        put(blocks, px + sx * 2, y + 1, pz + kk, "light_red", keep_lights=False)
        put(blocks, px + kk, y + 1, pz + sz * 2, "light_red", keep_lights=False)
    for h in range(1, 3):
        put(blocks, px - sx * 2, y + h, pz - sz * 2, "cargo_chute", keep_lights=False)
    if META is not None:
        META["pickups"].append({"tier": next(t for t, i, yy in RIB_PICKUPS if i == index and yy == y), "x": px - 1,
                                "y": y + 1, "z": pz - 1})
    # A solid tapering bracket under the pad, back into the buttress, so it reads as held up, not hanging.
    shell = SHELL[min(8, tier_at(y) - 1)]
    for step in range(1, 9):
        for a in range(-8, 3):
            for b in range(-8, 3):
                t = a + b  # along the diagonal, outward positive
                if t <= 4 - 2 * step and t >= -10 - step and abs(a - b) <= 4 - step // 3:
                    x, z = px + sx * a, pz + sz * b
                    if (x, y - step, z) not in blocks:
                        blocks[(x, y - step, z)] = "hazard_plating" if step == 1 and t >= 1 else shell
    for step in range(1, 7):  # a steel strut along the bracket's underside
        put(blocks, px - sx * (step // 2), y - step - 1, pz - sz * (step // 2), "steel_girder")


def tier_at(y):
    for t in range(1, 10):
        if y < TIER_TOPS[t]:
            return t
    return 9


def spire_hangar_levels():
    """Every level of the spire with room for a large hangar: (floor y, side, half-width of the spire there)."""
    y0 = [y for t, kind, y, y1, _ in schedule() if kind == "spire"][0]
    w, d, h = HANGAR["large"]
    levels = []
    y = y0 + 1
    while y + h + 1 < SHAFT_TOP:
        r = min(spire_half(yy, y0) for yy in range(y, y + h + 2))
        if r < 4.6:
            break
        levels.append((y, len(levels) % 4, int(r)))
        y += h + 2 + SPIRE_HANGAR_GAP
    return levels


def spire_hangars(blocks):
    """The spire's hangars: each a large hangar (7 wide, 7 deep, 5 high) set into the spire with its door in the
    spire's face, a floor and a roof of its own, and a landing pad like every other hangar."""
    w, d, h = HANGAR["large"]
    spire_y0 = [y for t, kind, y, y1, _ in schedule() if kind == "spire"][0]
    for y, side, half in spire_hangar_levels():
        size = 2 * half + 1
        x0 = z0 = C - half
        u0 = (size - (w + 2)) // 2
        hangar_row(blocks, y, x0, z0, size, side, 1, "large", "graphene_lattice", False, start=u0, seed=y, tier=9)
        # Lower on the level the spire is wider than the hangar's face, so its skin would stand in front of the
        # door (and a buttress can reach past it): cut a short framed mouth through it, and move the drones' exit point out past it.
        inside = {yy: cross_section(spire_half(yy, spire_y0), n=3.2) for yy in range(y, y + h + 2)}

        def in_spire(x, yy, z):
            return (x - C, z - C) in inside[yy]

        def blocked(v):
            for u in range(u0 + 1, u0 + w + 1):
                x, z = local(x0, z0, size, side, u, v, False)
                if any(in_spire(x, yy, z) or (x, yy, z) in blocks for yy in range(y + 1, y + h + 1)):
                    return True
            return False
        # Through the spire's skin, or a buttress that reaches past the door (up to 6 blocks out).
        depth = max([k for k in range(1, 7) if blocked(-k)], default=0)
        for v in range(-depth, 0):
            for u in range(u0, u0 + w + 2):
                x, z = local(x0, z0, size, side, u, v, False)
                for yy in range(y, y + h + 2):
                    frame = u in (u0, u0 + w + 1) or yy in (y, y + h + 1)
                    if not frame:
                        HANGAR_SPACE.add((x, yy, z))
                    elif in_spire(x, yy, z):
                        put(blocks, x, yy, z, "hazard_plating" if yy == y + h + 1 else "graphene_lattice")
        if depth and META is not None:
            dock = META["docks"][-1]
            e1 = local(x0, z0, size, side, u0 + 1, -depth - 2, False)
            e2 = local(x0, z0, size, side, u0 + w, -depth - 2, False)
            dock["ex"], dock["ez"] = (e1[0] + e2[0]) / 2 + 0.5, (e1[1] + e2[1]) / 2 + 0.5
        for u in range(u0, u0 + w + 2):
            for v in range(0, d + 2):
                x, z = local(x0, z0, size, side, u, v, False)
                if (x, y, z) not in blocks:
                    blocks[(x, y, z)] = "graphene_lattice"
                if v > 0:
                    put(blocks, x, y + h + 1, z, "graphene_lattice")


def spire(blocks, y0):
    """Tier 9's top: one long, smooth, tapering spire on the hangar core (no eaves or rings sticking out),
    with dim red seams up its corners, flush glowing bands, and the beacon."""
    for y in range(y0, SPIRE_TOP):
        r = spire_half(y, y0)
        cells = cross_section(r, n=3.2)
        skin = {c for c in cells if any((c[0] + ex, c[1] + ez) not in cells for ex, ez in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
        for dx, dz in (cells if y % 12 == 0 or r < 2 else skin):
            m = "graphene_lattice"
            if y % 12 == 0:
                m = "sc_conduit"
            elif abs(abs(dx) - abs(dz)) <= 0 and (dx, dz) not in cross_section(max(0.3, r - 1.2), n=3.2):
                m = "light_cyan"
            put(blocks, C + dx, y, C + dz, m)
    blocks[(C, SPIRE_TOP, C)] = "light_red"


def roof(blocks, index, y):
    """An antenna on the current top floor (until the next tier is built)."""
    x0, z0, size = floor_square(index)
    for h in range(1, 6):
        put(blocks, x0 + size // 2, y + h, z0 + size // 2, "iron")
    blocks[(x0 + size // 2, y + 6, z0 + size // 2)] = "light_red"


_LIGHTS = {}
_BUILT = {}


def build(tier):
    """Every block of the tower up to and including {@code tier}, as {(x, y, z): material}, lit so no mob can
    spawn on it (tools/tower_lights.py). Each tier keeps the earlier tiers' lights whose floor is still there."""
    global PREVIEW
    key = (tier, PREVIEW)
    if key not in _BUILT:
        import tower_lights
        preview = PREVIEW
        for t in range(1, tier + 1):
            if t in _LIGHTS:
                continue
            PREVIEW = False
            raw = build_raw(t)
            PREVIEW = preview
            for k in range(1, t):
                for pos, under in _LIGHTS[k].items():
                    if raw.get(pos) == under:
                        raw[pos] = tower_lights.FIXTURE
            _LIGHTS[t] = tower_lights.plan(raw, C)
        blocks = build_raw(tier)
        for k in range(1, tier + 1):
            for pos, under in _LIGHTS[k].items():
                if blocks.get(pos) == under:
                    blocks[pos] = tower_lights.FIXTURE
        _BUILT[key] = blocks
    return dict(_BUILT[key])


def build_raw(tier):
    """The tower's blocks up to {@code tier} before lighting."""
    blocks = {}
    HANGAR_SPACE.clear()
    HANGAR_PADS.clear()
    base_field(blocks)
    command_post(blocks)
    import exchanges  # the Energy and Storage Exchanges on the field's west and east edges (tier 1)
    COLORS.update(exchanges.EXTRA_COLORS)
    GLOWING.add("power_port")
    exchanges.build(blocks)
    if tier >= 2:
        hangar_deck(blocks)
    last_floor = None
    core_top = [y1 for t, kind, y0, y1, _ in schedule() if kind == "floor"][-1]
    for t, kind, y0, y1, info in schedule():
        if t > tier or kind in ("post", "deck"):
            continue
        if kind == "floor":
            hangar_floor(blocks, info, y0)
            last_floor = (info, y1 - 1)
        elif kind == "spire":
            spire(blocks, y0)
    if tier >= 3:
        buttresses(blocks, TIER_TOPS[tier], core_top)
        for t, index, y in RIB_PICKUPS:
            if t <= tier:
                rib_pickup(blocks, index, y, core_top)
    if tier >= 9:
        spire_hangars(blocks)  # after the buttresses, so nothing ends up inside a hangar
    if tier < 9 and last_floor:
        roof(blocks, *last_floor)
    for pos in HANGAR_SPACE:
        if blocks.get(pos) not in (None, "drone", "light_red"):
            del blocks[pos]
    blocks.update(HANGAR_PADS)
    return blocks


# ---------------------------------------------------------------- previews


def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def iso(blocks, scale=3, back=False):
    """Isometric render of the blocks, from the south-east (or from the north-west with {@code back})."""
    if back:
        blocks = {(SIZE - 1 - x, y, SIZE - 1 - z): m for (x, y, z), m in blocks.items()}
    a, b, h = 2 * scale, scale, 2 * scale
    height = TIER_TOPS[-1] + 2
    lo, hi = -18, SIZE + 18
    img = Image.new("RGBA", ((hi - lo) * 2 * a + 8, height * h + (hi - lo) * 2 * b + 8), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    ox = (hi - lo) * a + 4 + (lo + hi - SIZE) * a
    oy = height * h + 4 - 2 * lo * b

    def solid(pos):
        return pos in blocks

    for (x, y, z) in sorted(blocks, key=lambda p: (p[0] + p[2], p[1])):
        if solid((x + 1, y, z)) and solid((x, y, z + 1)) and solid((x, y + 1, z)):
            continue
        material = blocks[(x, y, z)]
        color = colour(material)
        glow = material in GLOWING
        sx = ox + (x - z) * a
        sy = oy + (x + z) * b - y * h
        top = [(sx, sy - b), (sx + a, sy), (sx, sy + b), (sx - a, sy)]
        left = [(sx - a, sy), (sx, sy + b), (sx, sy + b + h), (sx - a, sy + h)]
        right = [(sx, sy + b), (sx + a, sy), (sx + a, sy + h), (sx, sy + b + h)]
        draw.polygon(left, fill=shade(color, 1.0 if glow else 0.72))
        draw.polygon(right, fill=shade(color, 0.95 if glow else 0.55))
        draw.polygon(top, fill=color)
    return img


def elevation(tier, scale=2):
    """The 2D picture for the tower status screen: the built tower solid, the next tier as a faint ghost."""
    built = build(tier)
    nxt = build(tier + 1) if tier < 9 else {}
    height = TIER_TOPS[-1] + 2
    img = Image.new("RGBA", (SIZE * scale, height * scale), (14, 10, 12, 255))
    draw = ImageDraw.Draw(img)

    def front(blocks):
        cols = {}
        for (x, y, z), material in blocks.items():
            if not 0 <= x < SIZE:
                continue
            if (x, y) not in cols or z > cols[(x, y)][0]:
                cols[(x, y)] = (z, material)
        return cols

    for (x, y), (z, material) in front(nxt).items():
        px, py = x * scale, (height - 1 - y) * scale
        draw.rectangle([px, py, px + scale - 1, py + scale - 1], fill=(70, 26, 24, 255))
    for (x, y), (z, material) in front(built).items():
        color = colour(material)
        depth = 0.55 + 0.45 * max(0.0, min(1.0, z / (SIZE - 1)))
        px, py = x * scale, (height - 1 - y) * scale
        draw.rectangle([px, py, px + scale - 1, py + scale - 1], fill=shade(color, 1.0 if material in GLOWING else depth))
    return img


def screen_image(tier, scale=2):
    """The tower status screen's picture for a tower at {@code tier} (0: only the plinth): the built tower in
    its colours seen from the south, the next tier as a dull red ghost. Transparent background."""
    built = build(tier) if tier >= 1 else {}
    nxt = build(tier + 1) if tier < 9 else {}
    lo, hi = -10, SIZE + 10
    height = TIER_TOPS[-1] + 2
    img = Image.new("RGBA", ((hi - lo) * scale, height * scale), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    def front(blocks):
        cols = {}
        for (x, y, z), material in blocks.items():
            if material == "drone" or not lo <= x < hi:
                continue
            if (x, y) not in cols or z > cols[(x, y)][0]:
                cols[(x, y)] = (z, material)
        return cols

    # The next tier is not built yet: a faint fill with a dashed red outline, so it never reads as built.
    ghost = {key for key in front(nxt) if key not in front(built)}
    for (x, y) in ghost:
        px, py = (x - lo) * scale, (height - 1 - y) * scale
        edge = any((x + dx, y + dy) not in ghost for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        fill = (200, 60, 46, 230) if edge and (x + y) % 3 else (120, 34, 28, 46)
        draw.rectangle([px, py, px + scale - 1, py + scale - 1], fill=fill)
    for (x, y), (z, material) in front(built).items():
        color = colour(material)
        depth = 0.6 + 0.4 * max(0.0, min(1.0, (z + 10) / (SIZE + 20)))
        px, py = (x - lo) * scale, (height - 1 - y) * scale
        draw.rectangle([px, py, px + scale - 1, py + scale - 1], fill=shade(color, 1.0 if material in GLOWING else depth) + (255,))
    if tier == 0:  # the plinth and the core
        for x in range(C - 7, C + 8):
            px, py = (x - lo) * scale, (height - 1) * scale
            draw.rectangle([px, py, px + scale - 1, py + scale - 1], fill=colour("chiseled_stone") + (255,))
    return img


def plan():
    """Top-down plan of the landing field and the tier 1 footprint."""
    blocks = build(1)
    scale = 10
    img = Image.new("RGBA", (SIZE * scale, SIZE * scale), (20, 24, 30, 255))
    draw = ImageDraw.Draw(img)
    tops = {}
    for (x, y, z), material in blocks.items():
        if (x, z) not in tops or y > tops[(x, z)][0]:
            tops[(x, z)] = (y, material)
    for (x, z), (y, material) in tops.items():
        draw.rectangle([x * scale, z * scale, x * scale + scale - 1, z * scale + scale - 1], fill=colour(material))
    return img


# The mod's block for each design material (the previews use the material names and colours above).
BLOCK_IDS = {
    "chiseled_stone": "minecraft:chiseled_stone_bricks", "stone_bricks": "jugcraft:reinforced_concrete",
    "iron": "jugcraft:steel_girder", "tech_window": "jugcraft:blast_glass", "light_cyan": "jugcraft:red_light_strip",
    "light_red": "jugcraft:warning_light", "reinforced_concrete": "jugcraft:reinforced_concrete",
    "steel_girder": "jugcraft:steel_girder", "hazard_plating": "jugcraft:hazard_plating",
    "hangar_door": "jugcraft:hangar_bay_door", "steel_armor": "jugcraft:steel_armor_plate",
    "blast_glass": "jugcraft:blast_glass", "aluminum_cladding": "jugcraft:aluminum_cladding",
    "tungsten_frame": "jugcraft:tungsten_steel_frame", "carbon_composite": "jugcraft:carbon_composite_panel",
    "sic_armor": "jugcraft:silicon_carbide_armor", "du_armor": "jugcraft:depleted_uranium_armor",
    "graphene_lattice": "jugcraft:graphene_lattice", "sc_conduit": "jugcraft:superconducting_conduit",
    "landing_platform": "jugcraft:landing_platform", "landing_pad": "jugcraft:landing_pad",
    "supply_pickup": "jugcraft:supply_pickup", "cargo_packager": "jugcraft:cargo_packager",
    "cargo_chute": "jugcraft:steel_girder", "hangar_floor": "jugcraft:access_floor_tile",
    "access_floor_tile": "jugcraft:access_floor_tile", "carpet_tile": "jugcraft:carpet_tile",
    "acoustic_wall_panel": "jugcraft:acoustic_wall_panel", "concrete_column": "jugcraft:concrete_column",
    "holo_table": "jugcraft:holo_table", "ceiling_light_panel": "jugcraft:ceiling_light_panel",
    # The exchanges (tools/exchanges.py).
    "power_port": "jugcraft:energy_exchange_port", "cargo_port": "jugcraft:cargo_exchange_port",
    "transformer": "jugcraft:transformer_casing", "fin": "jugcraft:cooling_fin", "insulator": "jugcraft:ceramic_insulator",
    "busbar": "jugcraft:copper_busbar[axis=z]", "busbar_x": "jugcraft:copper_busbar[axis=x]", "conduit": "jugcraft:armored_conduit", "conduit_x": "jugcraft:armored_conduit[axis=x]",
    "dock_floor": "jugcraft:dock_plating", "hangar_pad": "jugcraft:hangar_pad", "hopper": "jugcraft:intake_funnel", "conveyor": "jugcraft:dock_plating",
    "bay_door": "jugcraft:hangar_bay_door", "girder_x": "jugcraft:steel_girder[axis=x]", "girder_z": "jugcraft:steel_girder[axis=z]",
}


def block_id(material):
    """The block state string for a design material (furniture carries its facing in brackets)."""
    if "[" in material:
        name, props = material.split("[", 1)
        return f"jugcraft:{name}[{props}"
    if material in BLOCK_IDS:
        return BLOCK_IDS[material]
    raise KeyError(f"no block for design material {material}")


def module_cost(tier, place, palette):
    """Modules an upgrade to {@code tier} costs: worth what its blocks would cost to craft (tools/tower_costs.py)."""
    import tower_costs
    names = [palette[p[3]].split("[")[0].split(":")[1] for p in place]
    return tower_costs.module_cost(tier, names)


def interior(blocks):
    """Cells (y >= 1) the tower needs empty: air between a tower block below and one above in the same column
    (rooms, hangars, under the deck), or air walled in on all sides within its layer (the atrium). Trees, dirt
    and stray blocks there are cleared before the tier goes in."""
    cols = {}
    for (x, y, z) in blocks:
        cols.setdefault((x, z), []).append(y)
    out = set()
    for (x, z), ys in cols.items():
        ys.sort()
        for lo, hi in zip(ys, ys[1:]):
            for y in range(max(lo + 1, 1), hi):
                out.add((x, y, z))
    # Enclosed in a layer: flood from the layer's bounding box edge; air not reached is walled in.
    layers = {}
    for (x, y, z) in blocks:
        if y >= 1:
            layers.setdefault(y, set()).add((x, z))
    for y, cells in layers.items():
        xs = [c[0] for c in cells]
        zs = [c[1] for c in cells]
        x0, x1, z0, z1 = min(xs) - 1, max(xs) + 1, min(zs) - 1, max(zs) + 1
        seen = set()
        stack = [(x0, z0)]
        while stack:
            c = stack.pop()
            if c in seen or c in cells or not (x0 <= c[0] <= x1 and z0 <= c[1] <= z1):
                continue
            seen.add(c)
            stack.extend(((c[0] + 1, c[1]), (c[0] - 1, c[1]), (c[0], c[1] + 1), (c[0], c[1] - 1)))
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if (x, z) not in seen and (x, z) not in cells:
                    out.add((x, y, z))
    return {p for p in out if p not in blocks}


def export(path):
    """Writes the tower data the mod builds from (gzipped JSON): for each tier, the blocks to place and to
    clear (relative to the Tower Core, which sits in the middle of the plinth), its docks (hangars), supply
    pickups, drone capacity and module cost. Ground pads and the ground pickup are the depot's own."""
    import gzip
    import json
    global META, PREVIEW
    PREVIEW = False
    palette, index = [], {}
    tiers = []
    previous = {}
    cleared = set()
    for tier in range(1, 10):
        META = {"docks": [], "pickups": []}
        # The Tower Core stays where it is (the plinth's middle): nothing is ever placed over it.
        blocks = {pos: m for pos, m in build(tier).items() if m != "drone" and pos != (C, 0, C)}
        place, clear = [], []
        for (x, y, z), m in sorted(blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
            if previous.get((x, y, z)) == m:
                continue
            state = block_id(m)
            if state not in index:
                index[state] = len(palette)
                palette.append(state)
            place.append([x - C, y, z - C, index[state]])
        for pos in previous:
            if pos not in blocks:
                clear.append([pos[0] - C, pos[1], pos[2] - C])
        # Cells this tier needs empty that no earlier tier cleared (drones clear them tile by tile).
        need = interior(blocks)
        air = sorted(need - cleared - set(previous), key=lambda p: (p[1], p[2], p[0]))
        cleared |= need
        docks = [d for d in META["docks"] if d["tier"] == tier]
        pickups = [p for p in META["pickups"] if p["tier"] == tier]
        for d in docks:
            for k in ("x", "ex"):
                d[k] -= C
            for k in ("z", "ez"):
                d[k] -= C
            if "door" in d:
                d["door"][0] -= C
                d["door"][1] -= C
                d["door"][2] -= C
                d["door"][3] -= C
        for p in pickups:
            p["x"] -= C
            p["z"] -= C
        tiers.append({"tier": tier, "name": TIER_NAMES[tier - 1], "top": TIER_TOPS[tier], "place": place, "clear": clear, "air": [[x - C, y, z - C] for x, y, z in air],
                      "docks": docks, "pickups": pickups,
                      "capacity": sum(FLOOR_CAPACITY[t][0] for t in range(1, tier + 1)),
                      "modules": module_cost(tier, place, palette)})
        previous = blocks
    META = None
    PREVIEW = True
    # The 8 ground pads (north-west corner of each) and where their drones fly out once the deck covers them.
    pads = []
    for side in range(4):
        for offset in (-6, 2):
            u, v = C + offset, 6
            corners = [(u, v), (SIZE - 1 - v - 4, u), (SIZE - 1 - u - 4, SIZE - 1 - v - 4), (v, SIZE - 1 - u - 4)]
            x, z = corners[side]
            out = [(0, -1), (1, 0), (0, 1), (-1, 0)][side]
            cx, cz = x + 2.5, z + 2.5
            ex, ez = (cx + out[0] * (abs(cx - C) + 3 if out[0] else 0)), (cz + out[1] * (abs(cz - C) + 3 if out[1] else 0))
            if out[0]:
                ex = C + out[0] * (C + 3) + 0.5
            if out[1]:
                ez = C + out[1] * (C + 3) + 0.5
            pads.append({"x": x - C, "z": z - C, "ex": ex - C, "ez": ez - C})
    data = {"palette": palette, "plinth": 7, "terminal": [-3, 1, 0], "pads": pads, "tiers": tiers}
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(json.dumps(data, separators=(",", ":")).encode("utf-8"))
    return data


def blueprint_stages(path):
    """What the Drone Tower Foundation blueprint can preview with shift+scroll: the space the tower takes at each
    tier, as columns {x, z, bottom, top} relative to the Tower Core (the tower always faces the same way)."""
    import gzip
    import json
    global PREVIEW
    PREVIEW = False
    stages = []
    for tier in range(1, 10):
        cols = {}
        for (x, y, z), m in build(tier).items():
            lo, hi = cols.get((x, z), (y, y))
            cols[(x, z)] = (min(lo, y), max(hi, y))
        stages.append({"name": f"Tier {tier}: {TIER_NAMES[tier - 1]}", "top": TIER_TOPS[tier],
                       "columns": [[x - C, z - C, lo, hi] for (x, z), (lo, hi) in sorted(cols.items())]})
    PREVIEW = True
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(json.dumps({"anchor": "jugcraft:drone_tower_core", "stages": stages}, separators=(",", ":")).encode("utf-8"))


FLOOR_CAPACITY[9] = (8 + len(spire_hangar_levels()), "8 large hangars in the core and %d up the spire" % len(spire_hangar_levels()))


def main():
    out = Path(__file__).resolve().parent.parent / "build" / "drone_tower"
    out.mkdir(parents=True, exist_ok=True)
    for tier in range(1, 10):
        blocks = build(tier)
        iso(blocks).save(out / f"tier_{tier}_iso.png")
        elevation(tier).save(out / f"tier_{tier}_screen.png")
        print(tier, TIER_NAMES[tier - 1], "height", TIER_TOPS[tier], "blocks", len(blocks))
    iso(build(9), back=True).save(out / "tier_9_iso_back.png")
    plan().save(out / "plan.png")
    data = export(Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data" / "jugcraft"
                  / "drone_tower" / "tower.json.gz")
    blueprint_stages(Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "jugcraft"
                     / "blueprint_stages" / "drone_tower_foundation.json.gz")
    gui = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures" / "gui" / "drone_tower"
    gui.mkdir(parents=True, exist_ok=True)
    for tier in range(0, 10):
        screen_image(tier).save(gui / f"tier_{tier}.png", optimize=True)
    for t in data["tiers"]:
        print("tier", t["tier"], "places", len(t["place"]), "clears", len(t["clear"]), "air", len(t["air"]), "docks", len(t["docks"]),
              "pickups", len(t["pickups"]), "capacity", t["capacity"], "modules", t["modules"])


if __name__ == "__main__":
    main()
