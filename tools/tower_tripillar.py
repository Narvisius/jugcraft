"""The Tri-Pillar Drone Tower (layout 8): the October 2026 redesign, as blocks.

Geometry comes from tripillar_geometry.py (the approved mockup's numbers): a hexagonal two-storey control building
(tier 1) with a glass hall, an atrium under a glass lantern and six roof pads for the 24 tier-1 drones; then three
chamfered pillars (tiers 2-9) with hangar bays on their outer faces, a tie deck at every tier line, a lift core
with a service ladder from the tier-2 deck up, supply cradles at tiers 2, 4, 6 and 8 and a mast on top.

The bays have no door blocks: their iris doors are drawn by the depot renderer from each dock's door (flag 2).
The energy and cargo exchanges are the formed stations of the industrial layout; since layout 37 (work list item 20,
tools/tripillar_stations.py) they stand in the building's east point with their ports in its intake face, the depot
terminal and its packager in dispatch on the ground floor, the supply pickup on the roof's landing platform beside the
goods lift's house (an exchange binds only within 48 blocks of the terminal; ExchangeStations.masterOffset knows each
layout). Layouts 8 to 36 had them outside in a supply yard on the east side.

Run it to write data/jugcraft/drone_tower/tower_tripillar.json.gz and the status-screen pictures
(textures/gui/tower_tripillar/tier_N.png). Earlier layouts are untouched; saved towers keep their own.
"""
from __future__ import annotations

import gzip
import sys
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

import framed_glass as FG
import sliding_doors as SD
import pillar_rooms as PROOMS
import roof_plant_layout as ROOFP
import holo_command_table as HCT
import curved_video_wall as CVW
import tower_control_console as TCC
import tower_art
import tower_costs
import tripillar_geometry as G
import tripillar_hall as HALL
import tripillar_lights as LIGHTS
import neon_lines as NEON
import cradle_struts as STRUTS
import joined_girders as JG
import big_map_table as BMT
import tower_diorama as TD
import tripillar_rooms as ROOMS
import tripillar_rooms_upper as UPPER
import tripillar_stations as STATIONS
import tripillar_crew as CREW
import tower_night_lights as NIGHT
import atrium_dome as ATRIUM
import keep_same as KEEP

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src/main/resources"
DATA = RESOURCES / "data/jugcraft/drone_tower/tower_tripillar.json.gz"
INDUSTRIAL = RESOURCES / "data/jugcraft/drone_tower/tower_industrial.json.gz"
GUI = RESOURCES / "assets/jugcraft/textures/gui/tower_tripillar"
VERSION = 51   # 51: flat screens only (the downstairs cafe's Lounge TV a Flat Screen TV over a media console; the upper lounge's sitting group centred on its TV); 50: the upper floor's walls (the rooms' walls onto the gallery faced in concrete with the light line, the pillars up there carrying it too: tripillar_rooms_upper.gallery_side); 49: the first-floor look-over (the rooms' walls onto the hall faced in concrete with the pillars' light line, the slot by the battery room and the wedge by the restrooms closed, the battery room's doors turned to the hall, the goods lift's nook closed, benches and plants: tools/faced_walls.py, tripillar_rooms.hall_side); 48: the crew list (C2: the security detail reworked, an upper floor patrol; C5: three carts in the custodian's closet); 47: automatic sliding doors (the entrance, the rooms, the map and briefing rooms: tools/sliding_doors.py); 46: the flight-ops trees centred in their floors; 45: the flight-ops trees (the Ember Oak and the Drift Isle in place of the operator rows) and the crew's posts (tripillar_crew); 44: the diorama's own case; 43: the atrium dome (the feet over the rotunda cut back to one sphere), the riser ladders up the outer corners; 42: the wall panel runs' end corners mirrored, the intake room's ends alike, the diorama table placed formed; 41: the packer's infeed conveyor; 40: the cargo area reworked (the cargo hall, the pickup on the lift house); 39: fix list 3 (custodian wall, simulators glass straight, lounge flat screen TV); 38: the training room closed at both ends (fix 1); 37: the stations inside (item 20); 36: the roof plant and walkways; 35: the pillar rooms fitted; 34: the lift core's door centred; 33: no flight planning; 32: the cafe's kitchen kit; 49 ... 8 frozen as _v49 ... _v8
PLINTH = 7
# R12 (foundation pricing): the control building (with its exchanges, dispatch and the roof's pickup since layout 37) is
# the foundation, placed by the core with its own cost; tier 1 is the six roof pads; tiers 2-9 the pillar sections.
FOUNDATION_NAME = "Control Building"
TIER_NAMES = ["Roof Pads", "Pillar Feet", "Small Hangar Rows", "Medium Hangars", "Logistics Levels",
              "Composite Levels", "Large Hangars", "Heavy Levels", "Crown"]

# design material -> block state
M = {
    "conc": "jugcraft:reinforced_concrete",
    "skin": "jugcraft:carbon_composite_panel",
    "metal": "jugcraft:steel_armor_plate",
    "lining": "jugcraft:dock_plating",
    "hall_floor": "jugcraft:epoxy_floor",
    "ceiling": "jugcraft:ceiling_tile",
    "floor": "jugcraft:access_floor_tile",
    "wall_in": "jugcraft:acoustic_wall_panel",
    "amber": "jugcraft:amber_light_strip",
    "skylight": "jugcraft:blast_glass",
    "lamp": "jugcraft:ceiling_light_panel",
    "rod": "minecraft:end_rod[facing=up]",
    "beacon": "jugcraft:warning_light",
    "girder_y": "jugcraft:steel_girder[axis=y]",
    "girder_x": "jugcraft:steel_girder[axis=x]",
    "girder_z": "jugcraft:steel_girder[axis=z]",
    "platform": "jugcraft:landing_platform",
    "pad_plate": "jugcraft:landing_pad",
    "pickup": "jugcraft:supply_pickup",
    "packager": "jugcraft:cargo_packager",
    "holo": "jugcraft:holo_table",
}
DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
FACE_NAME = {(0, -1): "north", (0, 1): "south", (-1, 0): "west", (1, 0): "east"}

# ------------------------------------------------------------------ small helpers


def put(b, x, y, z, m):
    b[(x, y, z)] = m


def fill(b, x0, y0, z0, x1, y1, z1, m):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                b[(x, y, z)] = m


def glass(facing):
    """Framed glass, its panel on the side away from ``facing`` (facing points into the room)."""
    return "fglass:" + facing


def hex_cells():
    r = G.HEX_R
    return {(x, z) for x in range(-r - 1, r + 2) for z in range(-r - 1, r + 2) if G.inside_hex(x, z)}


HEX = hex_cells()


def edge_of(cells):
    """Cells of ``cells`` with a side neighbour outside, and for each the outside directions."""
    out = {}
    for (x, z) in cells:
        sides = [d for d, (dx, dz) in DIRS.items() if (x + dx, z + dz) not in cells]
        if sides:
            out[(x, z)] = sides
    return out


HEX_EDGE = edge_of(HEX)
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


def pillar_cells(t, k):
    cx, cz = G.pillar_centres(t)[k]
    hw = G.half_width(t)
    return {(x, z) for x in range(cx - hw, cx + hw + 1) for z in range(cz - hw, cz + hw + 1)
            if G.inside_pillar(x, z, cx, cz, hw)}


def all_pillars(t):
    out = set()
    for k in "ABC":
        out |= pillar_cells(t, k)
    return out


def atrium(x, z):
    return x * x + z * z < G.ATRIUM_R * G.ATRIUM_R


# Layout 11 (work list item 19): the rotunda. On the ground storey the pillar feet are cut back to R_ROT round the
# hall's axis (the core cell's middle), so nothing of them stands inside the circle; the cells next to the cut, out to
# FACE_R, make the new concrete face the Curved Video Walls stand on. Over the cut the soffit is a storey's slab higher
# (y 11), so the slab edge at y 10 runs on round the circle over the walls, amber like the rest, and the status ticker
# with it.
R_ROT = 15.0
FACE_R = 16.2
# the glass floor strips run out to the walls' plinths
STRIP_R = 14.0


def near_r(x, z):
    """The nearest point of cell (x, z) to the hall's axis (0.5, 0.5)."""
    return math.hypot(min(max(0.0, x - 0.5), x + 0.5), min(max(0.0, z - 0.5), z + 0.5))


def triangle(t):
    """The tie deck's plan: the triangle through the three pillar directions (mockup: R = |A| + 1.1 hw)."""
    R = G.pillar_distance(t) + G.half_width(t) * 1.1
    v = [(R * math.sin(a), R * math.cos(a)) for a in (0, 2 * math.pi / 3, 4 * math.pi / 3)]

    def inside(x, z):
        s = []
        for i in range(3):
            (ax, az), (bx, bz) = v[i], v[(i + 1) % 3]
            s.append((bx - ax) * (z - az) - (bz - az) * (x - ax))
        return all(c >= 0 for c in s) or all(c <= 0 for c in s)
    r = int(R) + 2
    return {(x, z) for x in range(-r, r + 1) for z in range(-r, r + 1) if inside(x, z)}


def lift_cells():
    """The lift core: an octagon of radius 4 round the centre."""
    return {(x, z) for x in range(-4, 5) for z in range(-4, 5) if abs(x) + abs(z) <= 6}


LIFT = lift_cells()
LIFT_EDGE = edge_of(LIFT)

# ------------------------------------------------------------------ tier 1: the control building

ROOF = G.ROOF_Y            # roof slab (walk on y 21)
HALL_TOP = 9               # ground floor y 1..9
CEIL1, FLOOR2 = 10, 11     # hall ceiling, upper floor
UP_TOP = 18                # upper storey y 12..18
CEIL2 = 19
# The exchanges, the depot terminal, its packager and the supply pickup are part of the building (layout 37, work list
# item 20: tools/tripillar_stations.py); an exchange binds only within 48 blocks of the terminal (ExchangePortBlock.RANGE).
TERMINAL = STATIONS.TERMINAL
PACKAGER = STATIONS.PACKAGER
def roof_pad_corners():
    """Six 7x7 roof pads at radius 37, 17 degrees either side of the three bays between the pillars."""
    out = []
    for sector in (60, 180, 300):
        for s in (-1, 1):
            a = math.radians(sector + s * 17)
            cx, cz = round(G.PAD_RING * math.sin(a)), round(G.PAD_RING * math.cos(a))
            out.append((cx - 3, cz - 3))
    return out


ROOF_PADS = roof_pad_corners()


def industrial_stations():
    """The formed energy and cargo exchange blocks of the industrial layout's tier 1, by station."""
    with gzip.open(INDUSTRIAL, "rt", encoding="utf-8") as f:
        d = json.load(f)
    pal = d["palette"]
    out = {"energy": {}, "cargo": {}}
    for x, y, z, i in d["tiers"][0]["place"]:
        s = pal[i]
        if s.startswith("jugcraft:energy_exchange["):
            out["energy"][(x, y, z)] = s
        elif s.startswith("jugcraft:cargo_exchange["):
            out["cargo"][(x, y, z)] = s
    return out, d


def control_building(b):
    SD.PLACED.clear()
    pillars = all_pillars(2)
    inner = HEX - set(HEX_EDGE)
    # foundation under the walls and the hall floor
    for (x, z) in HEX:
        put(b, x, 0, z, M["hall_floor"])
    for (x, z), sides in HEX_EDGE.items():
        put(b, x, -1, z, M["conc"])
        put(b, x, 0, z, M["metal"])
    # the pillars' feet: concrete cores through both storeys, an amber band at y 3
    core_feet(b)
    # ground floor curtain wall: framed glass between mullions, the entrance on the north face
    curtain(b, 1, HALL_TOP, door_face=True)
    for (x, z) in HEX_EDGE:
        put(b, x, CEIL1, z, M["metal"])
        put(b, x, FLOOR2, z, M["metal"])
    # ceilings, floors, roof (with the atrium open through them)
    for (x, z) in inner:
        if (x, z) in pillars:
            continue
        if not atrium(x, z):
            put(b, x, CEIL1, z, M["ceiling"])
            put(b, x, FLOOR2, z, M["floor"])
            put(b, x, CEIL2, z, M["ceiling"])
        put(b, x, ROOF, z, M["skylight"] if atrium(x, z) else M["conc"])
    # the atrium's slab edges: an amber line under a metal fascia, a glass balustrade on the gallery
    ring = {(x, z) for (x, z) in inner if not atrium(x, z) and (x, z) not in pillars
            and any(atrium(x + dx, z + dz) for dx, dz in DIRS.values())}
    for (x, z) in ring:
        put(b, x, CEIL1, z, M["amber"])
        put(b, x, FLOOR2, z, M["metal"])
        put(b, x, CEIL2, z, M["metal"])
        toward = [d for d, (dx, dz) in DIRS.items() if atrium(x + dx, z + dz)]
        put(b, x, FLOOR2 + 1, z, glass(OPP[toward[0]]))
        if len(toward) == 2 and OPP[toward[0]] != toward[1]:
            RAIL_CORNERS[(x, FLOOR2 + 1, z)] = set(toward)
    # the slab edge goes on amber where the hall's ceiling meets the rotunda cut beside a foot (item 19)
    cut = {(x, z) for (x, z) in pillars if near_r(x, z) < R_ROT}
    for (x, z) in inner - pillars - ring:
        if not atrium(x, z) and any((x + dx, z + dz) in cut for dx, dz in DIRS.values()):
            put(b, x, CEIL1, z, M["amber"])
    # upper storey: dark skin with a glass band y 13..16, metal fascia, amber roof line, glass rail on the roof
    for (x, z), sides in HEX_EDGE.items():
        for y in range(FLOOR2 + 1, UP_TOP + 1):
            put(b, x, y, z, M["skin"])
        put(b, x, CEIL2, z, M["metal"])
        put(b, x, ROOF, z, M["amber"])
    curtain(b, 13, 16, door_face=False)
    for (x, z), sides in HEX_EDGE.items():
        if len(sides) == 1:
            put(b, x, ROOF + 1, z, glass(OPP[sides[0]]))
        else:
            put(b, x, ROOF + 1, z, M["metal"])
    # lights: ceiling panels on a 5-block grid under both ceilings
    for (x, z) in inner:
        if (x, z) in pillars or atrium(x, z) or (x, z) in ring:
            continue
        if x % 5 == 0 and z % 5 == 0:
            put(b, x, CEIL1 - 1, z, M["lamp"])
            put(b, x, CEIL2 - 1, z, M["lamp"])
    furnish(b, pillars)
    stairs(b)
    entrance(b)
    # (the glass floor strips to the feet run on to the walls' plinths now the feet are cut back)
    HALL.fit(b, put, glass, pillars, None, atrium,
             lambda t, k: {c for c in pillar_cells(t, k) if t != 2 or near_r(*c) >= STRIP_R})
    map_room(b)
    # the ground floor's rooms fitted out (work list item 22, part A: tools/tripillar_rooms.py)
    ground = ROOMS.ground_floor(b, put, glass, HEX, HEX_EDGE, CEIL1 - 1)
    # the upper floor's twelve rooms (work list item 22, part B: tools/tripillar_rooms_upper.py, the room kits)
    upper = UPPER.upper_floor(b, put, glass, HEX, HEX_EDGE, CEIL2 - 1, FLOOR2)
    # the stations inside: the intake face and room, dispatch, the goods lift, the roof's pickup (work list item 20)
    STATIONS.fit(sys.modules[__name__], b)
    # the atrium dome: the feet over the rotunda cut back to one sphere (Justin's inside-pillars option A, layout 43)
    ATRIUM.fit(b, put, M)
    # the rooms' hall side (Justin's first-floor look-over, layout 49): their walls onto the hall faced in concrete with the
    # pillars' light line, the goods lift's nook closed, benches and plants (tools/tripillar_rooms.py hall_side)
    _META_HALL_SIDE.update(ROOMS.hall_side(b, put, glass, HEX, HEX_EDGE, CEIL1 - 1, HALL_TOP, ground.rooms))
    # and the upper floor's rooms' gallery side (layout 50): the same, the pillars up there carrying the line too
    _META_HALL_SIDE["upper"] = UPPER.gallery_side(b, HEX, HEX_EDGE, FLOOR2, UP_TOP, pillars, upper.rooms)
    # every sliding door's leaves free to slide along its wall (tools/sliding_doors.py)
    SD.check(lambda x, y, z: b.get((x, y, z)))


# The map room (work list item 16, approved by Justin on 23 October 2026 after three takes): the upper floor's north side
# walled off behind glass, the Big Map Table in it. Inside x -10..12, z -44..-23 (the building's north wall is z -45).
MAP_ROOM = (-10, -44, 12, -23)
MAP_WALLS_X = (-11, 13)
MAP_FRONT_Z = -22                    # its framed-glass front on the gallery
MAP_DOORS_X = (0, 1)                 # glass double doors, hinges outer
MAP_BAND = (17, 18)                  # a solid band over the glass: the MAP ROOM sign on it, never on the glass
MAP_TABLE = (1, -34)                 # the table's middle cell, on the upper floor's top (y 12)
MAP_LAMPS_OUT = ((0, -35), (5, -35), (0, -30), (5, -30))     # no ceiling panels over the map


def map_room(b):
    """The map room: carbon walls on its west and east, the glass front with its doors and the band over it, the sign on
    the band over the doors, the Big Map Table in the middle, the ceiling panels over the map taken out."""
    y0, y1 = FLOOR2 + 1, UP_TOP
    x0, z0, x1, z1 = MAP_ROOM
    for z in range(z0, z1 + 1):
        for x in MAP_WALLS_X:
            for y in range(y0, y1 + 1):
                assert (x, y, z) not in b, (x, y, z, b[(x, y, z)])
                put(b, x, y, z, M["skin"])
    for x in range(MAP_WALLS_X[0], MAP_WALLS_X[1] + 1):
        for y in range(y0, y1 + 1):
            assert (x, y, MAP_FRONT_Z) not in b, (x, y, MAP_FRONT_Z, b[(x, y, MAP_FRONT_Z)])
            if y in MAP_BAND or x in MAP_WALLS_X:
                put(b, x, y, MAP_FRONT_Z, M["skin"])
            elif x in MAP_DOORS_X and y in (y0, y0 + 1):
                continue                     # the doors (below)
            else:
                put(b, x, y, MAP_FRONT_Z, glass("north"))
    # the doors: an automatic sliding pair (tools/sliding_doors.py), walking in north, the master the east cell (the
    # second to the walker's left), the leaves sliding along the glass on the gallery side
    SD.place(lambda x, y, z, s: put(b, x, y, z, s), MAP_DOORS_X[1], y0, MAP_FRONT_Z, "double_glass", "south")
    # the light box over the doors, on the band's gallery side (part 0 on the reader's left, the west)
    for p in range(4):
        put(b, MAP_DOORS_X[0] - 1 + p, MAP_BAND[0], MAP_FRONT_Z + 1, f"jugcraft:{BMT.SIGN}[facing=north,part={p}]")
    for (x, z) in MAP_LAMPS_OUT:
        assert b.get((x, CEIL2 - 1, z)) == M["lamp"], (x, z, b.get((x, CEIL2 - 1, z)))
        b.pop((x, CEIL2 - 1, z))
    tx, tz = MAP_TABLE
    for i, ((dx, dy, dz), _) in enumerate(BMT.footprint()):
        at = (tx + dx, y0, tz + dz)
        assert at not in b and x0 <= at[0] <= x1 and z0 <= at[2] <= z1, (at, b.get(at))
        put(b, *at, f"jugcraft:{BMT.ID}" if i == 0 else f"jugcraft:{BMT.PART_ID}[index={i}]")


def roof_pads(b):
    """Tier 1: the six roof pads, each one hangar pad of the large layout (hazard rim, edge lights, touchdown marks,
    charger) set into the roof slab."""
    for (x0, z0) in ROOF_PADS:
        for dx in range(7):
            for dz in range(7):
                part = tower_art.hangar_pad_part("large", 7, 7, dx, dz)
                put(b, x0 + dx, ROOF, z0 + dz, f"jugcraft:hangar_pad[part={part}]")


def core_feet(b):
    """The pillars' feet through the building: concrete cores, each a riser room with a mid floor, lights and a
    service ladder in the corner the tier-2 pillar's ladder uses, up through a hatch in its top; a doorway from the
    hall on its outer face, and an amber band round it at y 3."""
    hw = G.half_width(2)
    for k, (cx, cz) in G.pillar_centres(2).items():
        cells = pillar_cells(2, k)
        edge = edge_of(cells)
        for (x, z) in cells:
            put(b, x, -1, z, M["conc"])
            put(b, x, 0, z, M["conc"])
            put(b, x, ROOF, z, M["conc"])
            if (x, z) in edge:
                for y in range(1, ROOF):
                    put(b, x, y, z, M["conc"])
                put(b, x, 3, z, M["amber"])
            else:
                put(b, x, CEIL1, z, M["conc"])
                if (x - cx) % 4 == 2 and (z - cz) % 4 == 2:
                    put(b, x, CEIL1 - 1, z, M["lamp"])
                    put(b, x, ROOF - 1, z, M["lamp"])
        sx, sz = (1 if cx <= 0 else -1), (1 if cz <= 0 else -1)
        rotunda_cut(b, cells)
        # the riser ladder: up the foot's outer corner from the ground to the roof, on its post (the rotunda cut took the
        # inner corner's foot, layout 11; the atrium dome takes the rest of it, layout 43); the pillar's tiers carry it on
        # up from the same corner (pillar_ladder)
        lad = (cx - sx * (hw - 3), cz - sz * (hw - 3))
        post = (cx - sx * (hw - 2), cz - sz * (hw - 3))
        assert near_r(*lad) >= FACE_R and near_r(*post) >= FACE_R
        for y in range(1, ROOF + 1):
            put(b, post[0], y, post[1], M["metal"])
            put(b, lad[0], y, lad[1], f"jugcraft:tower_ladder[facing={'east' if sx > 0 else 'west'}]")
        # doorway on the face away from the centre, its Maintenance Door (work list item 10; open until layout 19)
        d = math.hypot(cx, cz)
        fx, fz = (int(math.copysign(1, cx / d)), 0) if abs(cx) > abs(cz) else (0, int(math.copysign(1, cz / d)))
        cells = [((cx + fx * hw, cz + a) if fx else (cx + a, cz + fz * hw)) for a in (0, 1)]
        maintenance_door(b, cells, (fx, fz), 0, hood=False)


def rotunda_cut(b, cells):
    """Item 19: the foot's cells inside R_ROT go on the ground storey (and the slab over them: their soffit is the
    concrete at y 11), the hall floor runs on under them; the next cells out make the cut's concrete face, amber at the
    slab edge (y 10) where they look onto the cut; lamps in the soffit."""
    cut = {(x, z) for (x, z) in cells if near_r(x, z) < R_ROT}
    for (x, z) in cells:
        r = near_r(x, z)
        if r < R_ROT:
            for y in range(1, CEIL1 + 1):
                b.pop((x, y, z), None)
            put(b, x, 0, z, M["hall_floor"])
            put(b, x, FLOOR2, z, M["conc"])
            if x % 5 == 0 and z % 5 == 0 and r < R_ROT - 2:
                put(b, x, CEIL1, z, M["lamp"])
        elif r < FACE_R:
            for y in range(1, CEIL1):
                put(b, x, y, z, M["conc"])
            if any((x + dx, z + dz) in cut for dx, dz in DIRS.values()):
                put(b, x, CEIL1, z, M["amber"])


def curtain(b, y0, y1, door_face):
    """Framed glass on the hex wall for y0..y1, mullions on every step corner and every fourth cell of a straight run."""
    for (x, z), sides in HEX_EDGE.items():
        mullion = len(sides) > 1 or (sides[0] in ("north", "south") and x % 4 == 0)
        for y in range(y0, y1 + 1):
            if mullion:
                put(b, x, y, z, M["metal"])
            else:
                put(b, x, y, z, glass(OPP[sides[0]]))


def entrance(b):
    """The Automatic Entrance Door in the north face (z -45; Justin, 8 November 2026, in place of three pairs of glass
    doors: tools/sliding_doors.py), six wide and three high (its header the row over the doorway), framed glass over
    it, and a canopy on two posts."""
    z = -45
    SD.place(lambda x, y, z, s: put(b, x, y, z, s), -3, 1, z, "entrance", "north")
    for x in range(-3, 3):
        for y in range(4, HALL_TOP + 1):
            put(b, x, y, z, glass("south"))
    for x in (-4, 3):
        for y in range(1, HALL_TOP + 1):
            put(b, x, y, z, M["metal"])
    # canopy over the doors: a dark slab with an amber front edge, on two girder posts
    fill(b, -8, CEIL1, -53, 7, CEIL1, -46, M["skin"])
    fill(b, -8, CEIL1, -53, 7, CEIL1, -53, M["amber"])
    for x in (-8, 7):
        fill(b, x, 0, -53, x, CEIL1 - 1, -53, M["girder_y"])
    fill(b, -8, 0, -53, 7, 0, -46, M["metal"])


CONSOLE_MASTER = (0, 1, -9)
# the console chairs: TOWER CONTROL's straight behind its desk, the side positions' turned a sixteenth in to theirs
CONSOLE_CHAIRS = [((0, 1, -11), 0), ((-4, 1, -10), 15), ((4, 1, -10), 1)]
RAISED_FLOOR_BACK = -12      # the raised floor's back step (the nave goes on north to the outer hall)


def control_console(b):
    """The Tower Control Console (work list item 6) at the head of the north nave, facing north (its operators sit
    north of it and look south over their monitors to the Holo Command Table), its parts on the floor band whose curved
    step follows the table; the Raised Floor fills the rest of the nave wall to wall back to z -12, its back edge
    stepping down; the three Console Chairs stand on it (each taking its floor tile)."""
    mx, my, mz = CONSOLE_MASTER
    band = set()
    for part, (dx, dz) in enumerate(TCC.footprint()):
        put(b, mx + dx, my, mz + dz, f"jugcraft:tower_control_console[facing=north,part={part}]")
        band.add((mx + dx, mz + dz))
    floor = set()
    for z in range(RAISED_FLOOR_BACK, 0):
        for x in range(-5, 6):
            near, _ = TCC.cell_r(x - mx, z - mz)
            if (x, z) not in band and near >= TCC.BAND_R - 1e-6:
                floor.add((x, z))
    chairs = {(x, z): rot for (x, _, z), rot in CONSOLE_CHAIRS}
    assert set(chairs) <= floor, "the console chairs must stand on the raised floor"
    carried = band | floor
    for (x, z) in sorted(floor):
        if (x, z) in chairs:
            put(b, x, my, z, f"jugcraft:console_chair[platform=true,rotation={chairs[(x, z)]}]")
            continue
        edges = {}
        for side, (sx, sz) in (("north", (0, -1)), ("east", (1, 0)), ("south", (0, 1)), ("west", (-1, 0))):
            n = (x + sx, z + sz)
            # steps down unless the floor goes on or a wall stands there (since the rotunda cut, nothing at the sides)
            edges[side] = "false" if n in carried or b.get((n[0], my, n[1])) in (M["conc"], M["metal"]) else "true"
        put(b, x, my, z, "jugcraft:raised_floor[" + ",".join(f"edge_{k}={v}" for k, v in edges.items()) + "]")


def furnish(b, pillars):
    """The Holo Command Table, master console, video walls on the cores, operator rows, reception."""
    # the Holo Command Table (item 5): one item, its parts on the cells under its round top, the middle over the core
    for part, (dx, dz) in enumerate(HCT.footprint()):
        put(b, dx, 1, dz, f"jugcraft:holo_command_table[part={part}]")
    control_console(b)
    video_walls(b)
    # (the operator rows in the two bays between the cores, each desk 3 wide with its chair, until layout 44: since layout
    # 45 the flight-ops trees stand there, the Ember Oak west and the Drift Isle east: tripillar_hall.ops_trees)
    # reception inside the entrance, staff facing the doors: the welcome desk on the west, security on the east
    for x0 in (-6, 4):
        for dx in range(3):
            put(b, x0 + dx, 1, -36, desk_state("north", dx, DESK_ROLES[("reception", x0)]))
        put(b, x0 + 1, 1, -35, "jugcraft:operator_chair[facing=north]")


# Each desk's role (work list item 14; Justin approved the take-2 mockup sheet): its row's three monitors show that role's
# three pages (client/DeskPages). The operator rows (west the flights and the build, east the plant; roles 0-11) went with
# layout 45 for the flight-ops trees, their roles kept for desks players set; the small table's desk
# (tripillar_hall.map_table) works the tower's build; the last role is for desks players set.
DESK_ROLE_NAMES = ["FLIGHT DIRECTOR", "AIRSPACE", "FLEET", "DISPATCH", "MISSIONS", "TOWER BUILD", "POWER", "SUPPLY", "DOCKS",
                   "MAINTENANCE", "DATA HALL", "COMMS", "RECEPTION", "SECURITY", "MAP TABLE"]
# (the operator rows' were keyed (side, x, the row's first z) until layout 44: west x 20, 25, 30 and east x 20, 25 at z 7
# and 12, the east's outer pair a third row at z 17)
DESK_ROLES = {("reception", -6): 12, ("reception", 4): 13}


def desk_state(facing, k, role):
    """A console desk of a row of three, k from the operator's left (0, 1, 2): joined on the sides it has a desk."""
    return HALL.desk_state(facing, k, role)


# the Curved Video Walls (item 7) on the cut faces: (master cell, aim vector to the axis cell, board)
VIDEO_WALLS = [((0, 1, 15), (0, -15), "operations"), ((13, 1, -8), (-13, 8), "fleet"), ((-13, 1, -8), (13, 8), "logistics")]


def video_walls(b):
    """One Curved Video Wall on each cut face, round the hall's axis: A (south, facing the console) the operations
    picture, B the fleet board, C logistics. Every cell of each is a cell of the cut (inside R_ROT)."""
    for (mx, my, mz), (vx, vz), board in VIDEO_WALLS:
        assert (mx + vx, mz + vz) == (0, 0), "the walls stand round the core's cell"
        aim = CVW.aim_of(vx, vz)
        for (dx, dy, dz) in CVW.footprint(vx, vz):
            x, y, z = mx + dx, my + dy, mz + dz
            assert near_r(x, z) < R_ROT and (x, y, z) not in b, (x, y, z, b.get((x, y, z)))
            if (dx, dy, dz) == (0, 0, 0):
                put(b, x, y, z, f"jugcraft:{CVW.ID}[aim={aim},board={board}]")
            else:
                put(b, x, y, z, f"jugcraft:{CVW.PART_ID}[ox={dx + CVW.SPAN},oy={dy},oz={dz + CVW.SPAN}]")


def stairs(b):
    """Concrete stairs from the hall up to the upper floor (y 1 to 11) and straight on to the roof (y 12 to 20), coming
    out in a hatch house with a glass door onto the roof. Two wide, along x at z -30..-29."""
    z0 = -30
    stair = "jugcraft:reinforced_concrete_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]"
    run = [(-31 + i, 1 + i) for i in range(11)] + [(-20 + i, 12 + i) for i in range(9)]
    for x, y in run:
        for z in (z0, z0 + 1):
            for yy in range(0 if y <= 11 else FLOOR2 + 1, y):
                put(b, x, yy, z, M["conc"])
            put(b, x, y, z, stair)
            for yy in range(y + 1, y + 4):
                if b.get((x, yy, z)) in (M["ceiling"], M["floor"], M["lamp"], M["conc"]) and yy in (CEIL1, FLOOR2, CEIL2, ROOF, CEIL1 - 1, CEIL2 - 1):
                    b.pop((x, yy, z), None)
    # glass rails along the hole in the upper floor (its stair-top end stays open: step off sideways at x -21)
    hole = sorted(x for x in range(-31, -20) if (x, FLOOR2, z0) not in b)
    for x in hole:
        put(b, x, FLOOR2 + 1, z0 - 1, glass("north"))
        put(b, x, FLOOR2 + 1, z0 + 2, glass("south"))
    for z in (z0, z0 + 1):
        put(b, hole[0] - 1, FLOOR2 + 1, z, glass("west"))
    # hatch house on the roof round the top of the flight, door on its east end
    for x in range(-17, -9):
        for z in (z0 - 1, z0 + 2):
            for y in range(ROOF + 1, ROOF + 4):
                put(b, x, y, z, M["skin"])
    for z in range(z0 - 1, z0 + 3):
        for y in range(ROOF + 1, ROOF + 4):
            put(b, -17, y, z, M["skin"])
            put(b, -10, y, z, M["skin"])
    fill(b, -17, ROOF + 4, z0 - 1, -10, ROOF + 4, z0 + 2, M["metal"])
    put(b, -11, ROOF, z0, M["conc"])
    put(b, -11, ROOF, z0 + 1, M["conc"])
    for z in (z0, z0 + 1):
        hinge = "right" if z == z0 else "left"   # facing west, "left" is south
        put(b, -10, ROOF + 1, z, f"jugcraft:glass_door[facing=west,half=lower,hinge={hinge},open=false,powered=false]")
        put(b, -10, ROOF + 2, z, f"jugcraft:glass_door[facing=west,half=upper,hinge={hinge},open=false,powered=false]")
    put(b, -13, ROOF + 3, z0, M["lamp"])


def roof_docks():
    out = []
    for (x0, z0) in ROOF_PADS:
        for dx in (-1.5, 1.5):
            for dz in (-1.5, 1.5):
                x, z = x0 + 3.5 + dx, z0 + 3.5 + dz
                out.append({"tier": 1, "size": "small", "x": x, "y": ROOF + 1, "z": z, "ex": x, "ez": z})
    return out

# ------------------------------------------------------------------ tiers 2-9: pillars, bays, decks


def pillar_tier(b, t, docks, pickups):
    y0, y1 = G.TB[t - 1], G.TB[t]
    hw = G.half_width(t)
    size, _ = G.PLAN[t]
    w, h, _depth, pitch = G.BAY[size]
    pad_depth = {"small": 3, "medium": 4, "large": 7}[size]
    floors = G.row_floors(t)
    bays = G.lay_bays(t)
    centres = G.pillar_centres(t)
    keep = set()  # cells of bays and their frames: the skin and the rooms leave them alone
    for k in "ABC":
        cells = pillar_cells(t, k)
        edge = edge_of(cells)
        cx, cz = centres[k]
        # the shell
        for (x, z), sides in edge.items():
            for y in range(y0 + 1, y1):
                put(b, x, y, z, M["skin"])
            put(b, x, y1 - 1, z, M["metal"])
        # row floors and lights inside
        inner = cells - set(edge)
        for ys in floors[1:]:
            for (x, z) in inner:
                put(b, x, ys, z, M["floor"])
        for ys in floors[1:] + [y1]:
            for (x, z) in inner:
                if (x - cx) % 4 == 2 and (z - cz) % 4 == 2:
                    put(b, x, ys - 1, z, M["lamp"])
        # a glass band at every row's lintel level
        for ys in floors:
            for (x, z), sides in edge.items():
                if len(sides) == 1:
                    put(b, x, ys + h + 1, z, glass(OPP[sides[0]]))
    # bays are numbered up each pillar from tier 2 (A01, A02 ...): the bay number tags on the jambs (DroneDepotRenderer)
    number = {k: sum(1 for tt in range(2, t) for bay in G.lay_bays(tt) if bay["pillar"] == k) for k in "ABC"}
    for bay in bays:
        k, (fx, fz), s, row = bay["pillar"], bay["face"], bay["s"], bay["row"]
        cx, cz = centres[k]
        ys = floors[row]
        number[k] += 1

        def cell(v, a):
            """(x, z) of the cell v in from the face, a along it."""
            return (cx + fx * (hw - v), cz + a) if fx else (cx + a, cz + fz * (hw - v))
        a0 = s - w // 2
        across = list(range(a0, a0 + w))
        # room: air over the pad, lined walls, back wall and ceiling
        for v in range(0, pad_depth + 2):
            for a in range(a0 - 1, a0 + w + 1):
                x, z = cell(v, a)
                side = a in (a0 - 1, a0 + w)
                for y in range(ys, ys + h + 2):
                    keep.add((x, y, z))
                    if y == ys:
                        m = M["metal"] if (v == 0 or side or v == pad_depth + 1) else None
                    elif y == ys + h + 1:
                        m = M["metal"] if v == 0 else M["lining"]
                    elif v == 0:
                        m = M["metal"] if side else "AIR"
                    elif side or v == pad_depth + 1:
                        m = M["lining"]
                    else:
                        m = "AIR"
                    if m == "AIR":
                        b.pop((x, y, z), None)
                    elif m is not None:
                        put(b, x, y, z, m)
        # the pad: one formed hangar pad over the whole floor
        cells_ = [cell(v, a) for v in range(1, pad_depth + 1) for a in across]
        bx0, bz0 = min(c[0] for c in cells_), min(c[1] for c in cells_)
        wx = max(c[0] for c in cells_) - bx0 + 1
        dz_ = max(c[1] for c in cells_) - bz0 + 1
        for (x, z) in cells_:
            put(b, x, ys, z, f"jugcraft:hangar_pad[part={tower_art.hangar_pad_part(size, wx, dz_, x - bx0, z - bz0)}]")
        # a ceiling light just inside the door
        lx, lz = cell(1, s if w % 2 else s - 1)
        put(b, lx, ys + h, lz, M["lamp"])
        # dock and iris door
        e0, e1 = cell(0, across[0]), cell(0, across[-1])
        mid_x = (bx0 + bx0 + wx - 1) / 2 + 0.5
        mid_z = (bz0 + bz0 + dz_ - 1) / 2 + 0.5
        ox, oz = (e0[0] + e1[0]) / 2 + 0.5, (e0[1] + e1[1]) / 2 + 0.5
        docks.append({"tier": t, "size": size, "x": mid_x, "y": ys + 1, "z": mid_z,
                      "ex": ox + fx * 4, "ez": oz + fz * 4,
                      "door": [min(e0[0], e1[0]), min(e0[1], e1[1]), max(e0[0], e1[0]), max(e0[1], e1[1]),
                               ys + 1, h, fx, fz, "ABC".index(k), number[k]]})
    for k in "ABC":
        cx, cz = centres[k]
        cells = pillar_cells(t, k)
        # white light lines up the corners where the faces meet the chamfers, standing on the deck or roof below
        for fx, fz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for sgn in (-1, 1):
                a = sgn * (hw - G.CHAMFER)
                ox, oz = (cx + fx * (hw + 1), cz + a) if fx else (cx + a, cz + fz * (hw + 1))
                if (ox, y0, oz) not in b or (t == 2 and atrium(ox, oz)) or any((ox, y, oz) in keep for y in range(y0, y1)):
                    continue
                for y in range(y0 + 1, y1 - 1):
                    if (ox, y, oz) not in b:
                        put(b, ox, y, oz, M["rod"])
        pillar_door(b, t, k, cells, keep)
        pillar_ladder(b, t, k, cells, keep, floors)
    # empty rooms inside: leave bays and doors; everything else inside the shell between floors is air
    # (tie deck on top: the triangle joining the pillars, an amber edge under it and a glass rail on it)
    tri = triangle(t)
    top = tri | all_pillars(t)
    tri_edge = edge_of(top)
    for (x, z) in top:
        if (x, y1, z) not in keep:
            put(b, x, y1, z, M["conc"])
    for (x, z) in tri - all_pillars(t):
        put(b, x, y1 - 1, z, M["metal"])
    for (x, z), sides in tri_edge.items():
        if (x, z) in all_pillars(t):
            continue
        put(b, x, y1 - 1, z, M["amber"])
        if t < 9:
            put(b, x, y1 + 1, z, glass(OPP[sides[0]]) if len(sides) == 1 else M["metal"])
    lift(b, t)
    if t in (2, 4, 6, 8):
        cradle(b, t, pickups)
    if t == 2:
        lantern(b)
    if t == 9:
        crown(b)
    # the rooms behind the maintenance doors as drone service floors (work list item 21's last part, tools/pillar_rooms.py)
    PROOMS.fit(sys.modules[__name__], b, t)


# the way to the left of a door's facing (the way you walk in), as you come up to it
LEFT_OF = {"east": (0, -1), "south": (1, 0), "west": (0, 1), "north": (-1, 0)}
FACE = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}


def maintenance_door(b, cells, out, y0, hood):
    """Work list item 10: a Maintenance Door pair (tower/MaintenanceDoorBlock, tools/maintenance_door.py) in a 2-wide
    pillar doorway (its two cells, the way out of the pillar, the floor's y): pushed from outside into the pillar, the
    hinge-left (active) leaf on the left as you come up to it, the badge reader on (the core gives the doors its owner),
    a rain hood over it out on the roof and the decks."""
    facing = FACE[(-out[0], -out[1])]
    lx, lz = LEFT_OF[facing]
    left, right = sorted(cells, key=lambda c: c[0] * lx + c[1] * lz, reverse=True)
    for (x, z), hinge in ((left, "left"), (right, "right")):
        for dy, half in ((1, "lower"), (2, "upper")):
            put(b, x, y0 + dy, z, f"jugcraft:maintenance_door[exit=false,facing={facing},half={half},hinge={hinge},"
                                  f"hood={str(hood).lower()},moving=false,open=false,paired=true,powered=false,reader=true]")


def pillar_door(b, t, k, cells, keep):
    """A two-wide doorway from the deck (or roof) below into the pillar, on the straight face nearest the centre whose
    threshold is on solid deck, clear of the bays; its Maintenance Door, hooded (work list item 10; open until layout
    19)."""
    cx, cz = G.pillar_centres(t)[k]
    hw = G.half_width(t)
    y0 = G.TB[t - 1]
    best = None
    for fx, fz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for a in range(-(hw - G.CHAMFER), hw - G.CHAMFER):
            slot = []
            for aa in (a, a + 1):
                shell = (cx + fx * hw, cz + aa) if fx else (cx + aa, cz + fz * hw)
                out = (shell[0] + fx, shell[1] + fz)
                inside = (shell[0] - fx, shell[1] - fz)
                if (out[0], y0, out[1]) not in b or (t == 2 and atrium(*out)):
                    break
                if any((p[0], y, p[1]) in keep for p in (shell, inside, out) for y in (y0 + 1, y0 + 2)):
                    break
                slot.append(shell)
            else:
                d = math.hypot(slot[0][0] + fx, slot[0][1] + fz)
                if best is None or d < best[0]:
                    best = (d, slot, (fx, fz))
    if best is None:
        return
    for (x, z) in best[1]:
        for y in (y0 + 1, y0 + 2):
            b.pop((x, y, z), None)
            keep.add((x, y, z))
    maintenance_door(b, best[1], best[2], y0, hood=True)


def pillar_ladder(b, t, k, cells, keep, floors):
    """A service ladder in a corner of the pillar, from its floor up through every row floor and out through the deck,
    on a steel post."""
    cx, cz = G.pillar_centres(t)[k]
    hw = G.half_width(t)
    y0, y1 = G.TB[t - 1], G.TB[t]
    sx, sz = (1 if cx <= 0 else -1), (1 if cz <= 0 else -1)
    # the outer corner first (the feet's riser ladder comes up there, layout 43), then the others
    for lx, lz in ((-sx, -sz), (sx, -sz), (-sx, sz), (sx, sz)):
        lad = (cx + lx * (hw - 3), cz + lz * (hw - 3))
        post = (cx + lx * (hw - 2), cz + lz * (hw - 3))
        if any((p[0], y, p[1]) in keep for p in (lad, post) for y in range(y0, y1 + 1)):
            continue
        facing = "west" if lx > 0 else "east"
        for y in range(y0 + 1, y1 + 1):
            put(b, post[0], y, post[1], M["metal"])
            put(b, lad[0], y, lad[1], f"jugcraft:tower_ladder[facing={facing}]")
            keep.add((lad[0], y, lad[1]))
        return


def lift(b, t):
    """The lift core from the tier-2 deck up (built with tier 3 on): a glass octagon with corner posts, a service ladder
    on a steel post inside it, a landing and a door onto every deck. Work list item 21 (take 1 approved 25 October
    2026): the door is one Glass Door in the middle of the core's south face, framed glass either side under the
    transom (it was a two-block gap at one side), and the core's glass stands on a steel sill all round at every deck
    (there was a one-block slot round its foot, under the doorway too)."""
    if t < 3:
        return
    y0, y1 = G.TB[t - 1], G.TB[t] + (6 if t == 9 else 0)
    for (x, z) in LIFT:
        for y in range(y0, y1 + 1):
            b.pop((x, y, z), None)
    for (x, z), sides in LIFT_EDGE.items():
        for y in range(y0 + 1, y1 + 1):
            put(b, x, y, z, glass(OPP[sides[0]]) if len(sides) == 1 else M["metal"])
    decks = [y0] + ([G.TB[9]] if t == 9 else [])
    for yd in decks:
        for (x, z) in LIFT:
            if (x, z) not in LIFT_EDGE and z >= -2:
                put(b, x, yd, z, M["metal"])
        for (x, z) in LIFT_EDGE:
            put(b, x, yd, z, M["metal"])           # the glass's sill
        for half, y in (("lower", yd + 1), ("upper", yd + 2)):
            put(b, 0, y, 4, f"jugcraft:glass_door[facing=north,half={half},hinge=left,open=false,powered=false]")
            b.pop((0, y, 5), None)  # through the deck's glass rail, if it runs there
    for y in range(y0, y1 + 1):
        put(b, 0, y, -4, M["metal"])
        put(b, 0, y, -3, "jugcraft:tower_ladder[facing=south]")
    if t == 9:
        for (x, z) in LIFT:
            put(b, x, y1 + 1, z, M["metal"])
        put(b, 0, y1 + 1, -3, "jugcraft:tower_ladder[facing=south]")


def cradle(b, t, pickups):
    """A supply cradle cantilevered off a pillar's outer chamfer at the tier line: a 7x7 platform with the 3x3 pickup,
    a hazard rim, red corner lights, a walkway to the pillar and the Cradle Struts under it."""
    k, (dx, dz) = {2: ("A", (-1, 1)), 4: ("B", (1, -1)), 6: ("A", (1, 1)), 8: ("C", (-1, -1))}[t]
    cx, cz = G.pillar_centres(t)[k]
    hw = G.half_width(t)
    y1 = G.TB[t]
    start = (cx + dx * (hw - 1), cz + dz * (hw - 1))   # the first cell outside the chamfer, on the diagonal
    px, pz = start[0] + dx * 5, start[1] + dz * 5
    for x in range(px - 3, px + 4):
        for z in range(pz - 3, pz + 4):
            edge = abs(x - px) == 3 or abs(z - pz) == 3
            put(b, x, y1, z, "jugcraft:hazard_plating" if edge else M["metal"])
    for i in range(0, 3):
        x, z = start[0] + dx * i, start[1] + dz * i
        for (ox, oz) in ((0, 0), (-dx, 0), (0, -dz), (dx, 0), (0, dz)):
            if (x + ox, y1, z + oz) not in b:
                put(b, x + ox, y1, z + oz, M["metal"])
    fill(b, px - 1, y1 + 1, pz - 1, px + 1, y1 + 1, pz + 1, M["pickup"])
    pickups.append({"tier": t, "x": px - 1, "y": y1 + 1, "z": pz - 1})
    # the raked struts under it (work list item 12, tools/cradle_struts.py): one pair from the chamfer up under the deck,
    # its master the foot of the strut on the left seen from the pillar (a cell across the diagonal from its first cell)
    facing = STRUTS.FACINGS[(dx, dz)]
    a = (start[0] - dz, y1 - STRUTS.RISE, start[1] + dx)
    for i, c in enumerate(STRUTS.cells()):
        o = STRUTS.turn(c, facing)
        p = (a[0] + o[0], a[1] + o[1], a[2] + o[2])
        if p in b:
            raise ValueError(f"the tier {t} cradle's struts run into {b[p]} at {p}")
        put(b, *p, f"jugcraft:cradle_struts[facing={facing}]" if i == 0 else f"jugcraft:cradle_struts_part[facing={facing},index={i}]")
    for sx in (-3, 3):
        for sz in (-3, 3):
            put(b, px + sx, y1 + 1, pz + sz, M["beacon"])


def lantern(b):
    """Tier 2 closes the atrium: the roof's skylight gives way to a glass lantern between the pillar feet, from the roof
    up to just under the tier-2 deck, with a flat glass top and three girder trusses."""
    pillars = all_pillars(2)
    lt = G.LANTERN_TOP
    disc = {(x, z) for x in range(-16, 17) for z in range(-16, 17) if atrium(x, z)}
    for (x, z) in disc:
        if (x, z) in pillars:
            continue
        b.pop((x, G.ROOF_Y, z), None)
        put(b, x, lt + 1, z, M["skylight"])
    ring = edge_of(disc)
    for (x, z), sides in ring.items():
        if (x, z) in pillars:
            continue
        for y in range(G.ROOF_Y + 1, lt + 1):
            put(b, x, y, z, glass(OPP[sides[0]]) if len(sides) == 1 else M["metal"])
        put(b, x, G.ROOF_Y, z, M["metal"])
        put(b, x, lt + 1, z, M["metal"])
    # the rim of the old opening becomes a metal kerb
    for (x, z) in disc:
        if (x, z) in pillars:
            continue
        if any((x + dx, z + dz) not in disc for dx, dz in DIRS.values()):
            put(b, x, G.ROOF_Y, z, M["metal"])
    for (x, z) in disc:
        if (x, z) not in pillars and (x, z) not in ring:
            if (x * x + z * z) in range(60, 70) or (x * x + z * z) in range(140, 150):
                put(b, x, lt, z, M["lamp"])


def crown(b):
    """Beacons on the three pillar tops, the mast over the lift core, a red light on top."""
    y1 = G.TB[9]
    for k, (cx, cz) in G.pillar_centres(9).items():
        put(b, cx, y1 + 1, cz, M["beacon"])
    top = y1 + 7
    for y in range(top + 1, top + 16):
        put(b, 0, y, 0, M["girder_y"])
    for (x, z) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for y in range(top + 1, top + 5):
            put(b, x, y, z, M["girder_y"])
    put(b, 0, top + 16, 0, M["beacon"])


# ------------------------------------------------------------------ assembling the tiers

_BUILT = {}
_META = {}
_META_HALL_SIDE = {}        # what tripillar_rooms.hall_side did (for the tool's report)


def build(tier):
    """The whole tower as it stands at ``tier`` (0: the foundation alone): {(x, y, z): material}."""
    if tier in _BUILT:
        return dict(_BUILT[tier])
    if tier == 0:
        b = {}
        control_building(b)
        _META[0] = {"docks": [], "pickups": []}
    elif tier == 1:
        b = build(0)
        roof_pads(b)
        ROOFP.fit(sys.modules[__name__], b)
        _META[1] = {"docks": roof_docks(), "pickups": []}
    else:
        b = build(tier - 1)
        docks, pickups = [], []
        pillar_tier(b, tier, docks, pickups)
        _META[tier] = {"docks": docks, "pickups": pickups}
    b.pop((0, 0, 0), None)  # the Tower Core
    # no dark floors: flush amber lights where the tower's own lamps don't reach (tools/tripillar_lights.py)
    _META[tier]["lights"] = LIGHTS.light_pass(b, f"step {tier}")
    # the amber boxes and end rods (this step's: the steps before are converted already) as neon lines (work list item
    # 11, tools/neon_lines.py; the same light, so the lights above stand as they were)
    _META[tier]["neon"] = NEON.convert(b)
    _BUILT[tier] = b
    return dict(b)


# The gallery's rail cells where the atrium's stepped edge turns (the atrium on two sides): each is an L, its glass
# round both sides, so the rail runs on unbroken from step to step (fix 4 of Justin's 26 October list: "some of the
# glass isn't connecting"). Filled in by the slab edge's loop; resolve() gives them their corner.
RAIL_CORNERS = {}


def _rail_corner(state, sides):
    """A rail cell's framed glass state with a corner (an L) on its second atrium side, unless it already turns one."""
    props = dict(kv.split("=") for kv in state[state.index("[") + 1:-1].split(","))
    if "corner" in (props["left"], props["right"]) or props["up"] == "true":
        return state            # (already turning; or a room's glass wall stands on it)
    f = props["facing"]
    other = [s for s in sides if s != OPP[f]]
    if len(other) != 1:
        return state
    if other[0] == FG.CW[f]:
        props["right"] = "corner"
    elif other[0] == FG.CCW[f]:
        props["left"] = "corner"
    else:
        return state
    return state[:state.index("[")] + "[" + ",".join(f"{k}={props[k]}" for k in ("bottom", "facing", "left", "right", "up")) + "]"


def resolve(b):
    """Material names -> block states (framed glass worked out from its neighbours, as tools/framed_glass.py)."""
    def at(x, y, z):
        m = b.get((x, y, z))
        if m is None:
            return None
        if m.startswith("fglass:"):
            return ("glass", m[7:], FG.bottom_of(lambda *p: at0(*p), x, y, z))
        if "glass_door" in m:
            return ("door", m.split("facing=")[1].split(",")[0])
        if m.split("[")[0] in SD.NAMES:
            return ("door", "")
        return None

    def at0(x, y, z):
        m = b.get((x, y, z))
        if m is None:
            return None
        if m.startswith("fglass:"):
            return ("glass", m[7:], "sill")
        if "glass_door" in m:
            return ("door", m.split("facing=")[1].split(",")[0])
        if m.split("[")[0] in SD.NAMES:
            return ("door", "")
        return None
    out = {}
    for p, m in b.items():
        if m.startswith("fglass:"):
            out[p] = FG.state(at, *p)
            if p in RAIL_CORNERS:
                out[p] = _rail_corner(out[p], RAIL_CORNERS[p])
        else:
            out[p] = m
    # a run of girders is one member, plates where it ends (work list item 12, tools/joined_girders.py)
    return caged(JG.joined(out))


# What a climber can't stand on or in, to SteelLadderBlock.landing (its floor sturdy on top, its room not a full block).
NOT_SOLID = ("ladder", "light", "sign", "torch", "rail", "glass_pane", "button", "carpet", "neon_tube", "end_rod", "door")
LADDER_FRONT = {"east": (1, 0), "west": (-1, 0), "north": (0, -1), "south": (0, 1)}


def caged(blocks):
    """Every tower ladder as the plant's Caged Steel Ladder (work list item 17, Justin 24 Oct 2026: "Looks good, build
    it"): caged from the third length over every landing, flared at the cage's foot, handholds at the top, as
    nuclear/SteelLadderBlock.shaped works them out in game."""
    cells = {p: s.split("facing=")[1].split(",")[0].rstrip("]") for p, s in blocks.items() if s.startswith("jugcraft:tower_ladder")}

    def solid(p):
        s = blocks.get(p)
        return s is not None and not any(k in s for k in NOT_SOLID)

    def landing(p):
        dx, dz = LADDER_FRONT[cells[p]]
        x, y, z = p[0] + dx, p[1], p[2] + dz
        return solid((x, y - 1, z)) and not solid((x, y, z))

    def cage(p):
        x, y, z = p
        below, below2 = (x, y - 1, z), (x, y - 2, z)
        return below in cells and below2 in cells and not landing(p) and not landing(below)
    out = dict(blocks)
    for p, facing in cells.items():
        x, y, z = p
        c = cage(p)
        bottom = c and not cage((x, y - 1, z))
        top = (x, y + 1, z) not in cells
        out[p] = (f"jugcraft:caged_ladder[bottom={str(bottom).lower()},cage={str(c).lower()},facing={facing},"
                  f"top={str(top).lower()},waterlogged=false]")
    return out


def interior(blocks):
    """Cells (y >= 1) the tower needs empty: air between a tower block below and one above in the same column."""
    cols = {}
    for (x, y, z) in blocks:
        cols.setdefault((x, z), []).append(y)
    out = set()
    for (x, z), ys in cols.items():
        ys.sort()
        for lo, hi in zip(ys, ys[1:]):
            for y in range(max(lo + 1, 1), hi):
                out.add((x, y, z))
    return out


def write_gzip(path, data):
    KEEP.gz(json.dumps(data, separators=(",", ":")).encode("utf-8"), path)


def export(path=DATA):
    """The layout: the foundation (tier 0, placed by the core) and tiers 1-9, each with only what it adds."""
    palette, ids, tiers = [], {}, []
    previous, cleared, capacity = {}, set(), 0
    foundation = None
    resolved = {}
    for tier in range(0, 10):
        blocks = resolve(build(tier))
        resolved[tier] = blocks
        place = []
        for (x, y, z), state in sorted(blocks.items(), key=lambda r: (r[0][1], r[0][2], r[0][0])):
            if previous.get((x, y, z)) == state:
                continue
            if state not in ids:
                ids[state] = len(palette)
                palette.append(state)
            place.append([x, y, z, ids[state]])
        clear = [[x, y, z] for (x, y, z) in sorted(previous) if (x, y, z) not in blocks]
        need = interior(blocks)
        air = [[x, y, z] for (x, y, z) in sorted(need - cleared - set(previous) - set(blocks), key=lambda p: (p[1], p[2], p[0]))]
        cleared |= need
        meta = _META[tier]
        capacity += len(meta["docks"])
        # What the step costs is the blocks it places (tower/TowerCosts.java; tower_costs.check keeps tiers rising).
        entry = {"tier": tier, "name": FOUNDATION_NAME if tier == 0 else TIER_NAMES[tier - 1],
                 "top": G.TB[max(1, tier)] + 4 if tier <= 1 else G.TB[tier] + (23 if tier == 9 else 0),
                 "place": place, "clear": clear, "air": air, "docks": meta["docks"], "pickups": meta["pickups"],
                 "capacity": capacity}
        if tier == 0:
            foundation = entry
        else:
            tiers.append(entry)
        previous = blocks
    SD.check(lambda x, y, z: resolved[9].get((x, y, z)))      # (and in the finished tower)
    # the crew's posts (tools/tripillar_crew.py), each with the step that brings it in
    crew = CREW.posts(resolved[9])
    found = CREW.steps(lambda step: resolved[step])
    for post in crew:
        post["step"] = found[post["id"]]
    # the ceiling lights the core switches at night (tools/tower_night_lights.py)
    night_lights = NIGHT.plan(resolved[9], crew)
    data = {"layout_version": VERSION, "palette": palette, "plinth": PLINTH, "terminal": list(TERMINAL),
            "packager": list(PACKAGER), "platform": list(STATIONS.PLATFORM_SEED),
            "pads": [], "cosmetic_apron": [], "foundation": foundation, "tiers": tiers, "crew": crew,
            "night_lights": night_lights}
    write_gzip(path, data)
    return data

# ------------------------------------------------------------------ status screen pictures

SCREEN = (142, 434)
SCREEN_BLOCKS = 217          # TowerScreen.IMAGE_BLOCKS: the picture's height in "blocks" of 2 px
TOWER_TOP = G.TB[9] + 24     # the mast's top
COLOURS = {
    "reinforced_concrete": (122, 121, 118), "carbon_composite_panel": (52, 58, 62), "steel_armor_plate": (92, 100, 106),
    "framed_glass": (120, 150, 160), "blast_glass": (130, 160, 170), "shroomlight": (232, 150, 60),
    "hangar_pad": (70, 66, 60), "dock_plating": (64, 64, 65), "end_rod": (230, 230, 220), "warning_light": (230, 60, 44),
    "access_floor_tile": (90, 94, 94), "epoxy_floor": (170, 174, 172), "ceiling_tile": (200, 200, 196),
}


def screen_image(tier):
    image = Image.new("RGBA", SCREEN, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    scale_y = SCREEN_BLOCKS / TOWER_TOP
    half = 64  # the building is 104 across plus its exchanges: fit x -64..64 into the width

    def front(blocks):
        cols = {}
        for (x, y, z), m in blocks.items():
            if (x, y) not in cols or z < cols[(x, y)][0]:
                cols[(x, y)] = (z, m)
        return cols

    def rect(x, y, colour):
        px0 = math.floor((x + half) * SCREEN[0] / (2 * half + 1))
        px1 = max(px0, math.floor((x + half + 1) * SCREEN[0] / (2 * half + 1)) - 1)
        py0 = math.floor((SCREEN_BLOCKS - (y + 1) * scale_y) * 2)
        py1 = max(py0, math.floor((SCREEN_BLOCKS - y * scale_y) * 2) - 1)
        draw.rectangle((px0, py0, px1, py1), fill=colour)
    # tier -1: the bare plinth (the foundation next); 0: the foundation (tier 1 next); 1-9: the tower at that tier
    built = resolve(build(tier)) if tier >= 0 else {(x, 0, z): "minecraft:chiseled_stone_bricks"
                                                    for x in range(-7, 8) for z in range(-7, 8)}
    nxt = resolve(build(tier + 1)) if tier < 9 else {}
    current = front(built)
    ghost = set(front(nxt)) - set(current)
    for (x, y) in ghost:
        edge = any((x + dx, y + dy) not in ghost for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        rect(x, y, (200, 60, 46, 230) if edge and (x + y) % 3 else (120, 34, 28, 46))
    for (x, y), (_, m) in current.items():
        name = m.split("[")[0].split(":")[1]
        rect(x, y, COLOURS.get(name, (93, 103, 113)) + (255,))
    return image


def write_all():
    data = export()
    ATRIUM.write()         # the atrium dome's shell cells: their looks and shapes (atrium_dome.bin.gz)
    TD.write()             # the hall's tower diorama: a still model of the finished tower as this data has it
    GUI.mkdir(parents=True, exist_ok=True)
    # TowerScreen: tier_0 before anything is built, foundation.png once the foundation stands, tier_N from tier 1 on
    # (KEEP: a picture, like every file written here, is rewritten only when what it shows has changed)
    KEEP.png(screen_image(-1), GUI / "tier_0.png", optimize=True)
    KEEP.png(screen_image(0), GUI / "foundation.png", optimize=True)
    for tier in range(1, 10):
        KEEP.png(screen_image(tier), GUI / f"tier_{tier}.png", optimize=True)
    return data


if __name__ == "__main__":
    data = write_all()
    total = 0
    for t in [data["foundation"]] + data["tiers"]:
        total += len(t["place"])
        print(f"tier {t['tier']} {t['name']}: {len(t['place'])} placed, {len(t['clear'])} cleared, {len(t['air'])} air, "
              f"{len(t['docks'])} docks, {len(t['pickups'])} pickups, capacity {t['capacity']}, top {t['top']}, "
              f"{sum(tower_costs.step_items(t['place'], data['palette']).values())} items")
    print("palette", len(data["palette"]), "blocks", total, "bytes", DATA.stat().st_size)
    print("the hall side:", _META_HALL_SIDE)
