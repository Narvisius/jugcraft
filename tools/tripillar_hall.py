"""The Tri-Pillar control building's hall fittings (approved R8 version A and R9/R10 version B, October 2026;
claude/jugcraft-drone-tower-redesign.md). Called by tower_tripillar.py while it builds the foundation.

Version A: floor light lines from the holo table's amber ring to the entrance and to both operator areas; five touch
consoles round the holo table (not on the master console's side); white light strips up the pillar feet's outer
corners; (six glass-door equipment racks behind each operator area until layout 16: work list item 13, layout 17, moved
the servers into the data hall in the west corner, the nuclear plant's racks in a glass room with a contained aisle); two Drone Display Cases past the
gates (work list item 1, layout 12; they replaced the first lit plinths) with a tier-4 and a tier-1 drone; an info totem at the entrance showing the tower live; one
lounge west of the lobby, two rows of six sofas over the Long Coffee Table (work list item 3, layout 14; it replaced two
lounges by the entrance, three sofas a side over three small glass tables); the Crystal Tree in its planter bench (work
list item 2, layout 13; it replaced four small planters with birch trees); the flight-ops trees where the operator areas'
desk rows were, the Ember Oak west and the Drift Isle east (Justin, 5 November 2026, layout 45: ops_trees).

Version B: (floating holo panels (FLEET, TOWER, SUPPLY) over each operator area on floor light emitters until layout 18:
work list item 15, layout 19, replaced them with one Holo Projector in the lobby past the gates, the big watching head
(tools/holo_projector.py)); a second holo
table, the zooming topographic map of the build radius, with its own desk; a drone service bay (pad, lifted cradle with
a tier-4 drone, a 6-axis robot arm, a parts wall); four-lane security speed gates inside the entrance, in a glass
vestibule; two rings of light bars in the low ceiling round the opening; glass floor strips over lit cable trays from
the holo table to the three pillar feet. The status ticker round the opening's slab edge, the solid tower hologram and
the projector over it are drawn by the central holo table (client/HoloMapRenderer.java).

The live pieces (touch consoles, holo emitters, totem, the service bay's drone stand, service arm) are tower blocks with a depot
display block entity: the core pins them to the tower's terminal when it builds the foundation, and their renderers
read the depot (client/HallFixtureRenderer.java).

Coordinates are relative to the Tower Core; the hall floor is y 0, so everything standing on it is at y 1.
"""
import math

import tripillar_geometry as G

AMBER = "jugcraft:amber_light_strip"
METAL = "jugcraft:steel_armor_plate"
METAL_SLAB = "jugcraft:steel_armor_plate_slab[type=bottom,waterlogged=false]"
SKIN = "jugcraft:carbon_composite_panel"
GLASS_FLOOR = "jugcraft:blast_glass"
CONC = "jugcraft:reinforced_concrete"
ROD = "minecraft:end_rod[facing=up]"

# The hall's furniture, for the tests and the renderers (positions of the live pieces).
TOUCH_CONSOLES = []      # (x, z, facing)
EMITTERS = []            # (x, z, facing, panel): none since layout 19 (the Holo Projector replaced them)
PROJECTOR = (0, -33)     # the Holo Projector's master cell (x, z): past the gates, on the amber line, facing the entrance
PROJECTOR_REACH = 1      # tower/HoloProjectorBlock
MAP_TABLE = (-24, -25)   # centre cell of the small 3x3 hologram table (the tower diorama on it: client/TowerDiorama)
SERVICE_PAD = (22, -33)  # north-west cell of the service bay's 7x7 pad
SERVICE_STAND = (25, -30)
SERVICE_ARM = (27, -30)  # beside the cradle: its tool reaches the near side's two fans, 1.2-1.6 blocks out
CASES = {"west": (-14, -38, 4), "east": (13, -38, 1)}   # centre (x, z) of each 5x5 display case, and the drone tier shown
TREE = (14, -27)                                          # the Crystal Tree's master cell (x, z): the planter's middle
TREE_REACH, TREE_SEAT_R, TREE_TRUNK = 4, 4.12, 3          # tower/CrystalTreeBlock
CASE_REACH, CASE_HEIGHT = 2, 4                            # tower/DroneDisplayCaseBlock
# the flight-ops trees (Justin, 5 November 2026; layout 45): each one's master cell (x, z), the tree's middle over it.
# Layout 46 (6 November: "center that tree in that room more. It's like sitting way too close to one wall"): each one in
# the middle of its floor, as far from everything round it as it can be (the Drift Isle had 2 blocks to the security
# room's glass; now 6), the Ember Oak brought out from the windows and the data hall (from -26, 11 and 23, 13)
OPS_TREES = {"ember_oak": (-23, 12), "drift_isle": (19, 12)}
OPS_REACH, OPS_ISLE_DY = 4, 2                              # tower/OpsTreeBlock
OPS_RADIUS = {"ember_oak": 4.3, "drift_isle": 4.12}        # how far out each one's bed reaches (OpsTreeBlock.Kind.radius)
OPS_ISLE_R = 1.85                                          # the Drift Isle's island (its cells, 2 up)
LOUNGE = (-14, -27)                                       # the Long Coffee Table's master cell (its NW corner the table's middle)
LOUNGE_REACH = 2                                          # tower/LongCoffeeTableBlock
LOUNGE_ROWS = ((-12, -30, "south"), (-17, -25, "north"))  # each row of six sofas: its first cell, its facing (a
#   block's walk between each row and the table, Justin 24 Oct 2026: "walk ... in between the table and the couch")
TOTEM = (-7, -42)
GATE_LANES = {-3: "south", -1: "south", 1: "north", 3: "north"}
GATE_ROWS = (-41, -40)   # the line's north (half a) and south (half b) rows


def furniture(name, facing, **extra):
    props = "".join(f",{k}={v}" for k, v in extra.items())
    return f"jugcraft:{name}[facing={facing}{props}]"


MAP_DESK_ROLE = 5    # tower/ConsoleDeskBlock ROLES: TOWER BUILD, beside the tower's model (tower_tripillar.DESK_ROLES has the others)


def desk_state(facing, k, role):
    """A console desk of a row of three (work list item 14), k from the operator's left (0, 1, 2): joined to the desks
    beside it, working its row's role."""
    return (f"jugcraft:console_desk[facing={facing},left={'true' if k > 0 else 'false'},right={'true' if k < 2 else 'false'},"
            f"role={role}]")


def row_state(name, facing, left, right):
    return f"jugcraft:{name}[facing={facing},left={'true' if left else 'false'},right={'true' if right else 'false'}]"


CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}
DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}


def row(b, put, name, n, x, y, z, facing):
    """Pieces that join side by side (sofas), laid from x, z to the user's right (as tools/medium_plant.py)."""
    rx, rz = DIRS[CLOCKWISE[facing]]
    for i in range(n):
        put(b, x + rx * i, y, z + rz * i, row_state(name, facing, i > 0, i < n - 1))


def facing_towards(x, z, tx, tz):
    """The facing of a piece at (x, z) whose user looks towards (tx, tz)."""
    dx, dz = tx - x, tz - z
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def fit(b, put, glass, pillars, inside_hex, atrium, pillar_cells):
    """Adds the fittings to the building ``b`` (cells -> material), using the caller's helpers."""
    TOUCH_CONSOLES.clear()
    EMITTERS.clear()
    floor_lines(b, put, pillars)
    glass_strips(b, put, pillar_cells)
    corner_strips(b, put, pillar_cells)
    ceiling_rings(b, put, pillars, atrium)
    # (the five touch consoles round the old 3x3 table went with it: the Holo Command Table's touch stations replace them)
    operator_areas(b, put)
    holo_projector(b, put)
    entrance(b, put, glass)
    display_cases(b, put)
    lounge(b, put)
    crystal_tree(b, put)
    ops_trees(b, put)
    # the small 3x3 table, its desk and chair: taken out with layout 25 (Justin: "Trash it"; the Big Map Table in the map
    # room upstairs maps the land), back with layout 29 bearing the tower diorama (redesign 3)
    map_table(b, put)
    service_bay(b, put)
    data_hall(b, put, glass)


# ------------------------------------------------------------------ floor

def floor_lines(b, put, pillars):
    """Amber lines set in the floor: from the ring round the holo table north to the security gates, and east and west
    along z 5 to the operator areas (between the pillar feet)."""
    for z in range(-38, -5):
        put(b, 0, 0, z, AMBER)
    for x in range(6, 17):
        for s in (1, -1):
            if (s * x, 5) not in pillars:
                put(b, s * x, 0, 5, AMBER)


def glass_strips(b, put, pillar_cells):
    """Two-wide glass floor strips from the holo table's ring to each pillar foot, over a lit cable tray in a trench:
    blast glass in the floor, the tray under it (along the strip), an amber light under the tray, concrete round it.
    Each strip runs beside its pillar's axis (2.5 blocks clockwise of it), leaving the axis for the touch console in
    front of the pillar A foot; the three stay alike turned about the centre."""
    for k in "ABC":
        cx, cz = G.pillar_centres(2)[k]
        cells = pillar_cells(2, k)
        d = math.hypot(cx, cz)
        ux, uz = cx / d, cz / d
        px, pz = -uz, ux
        along_x = abs(ux) >= abs(uz)
        strip = set()
        for i in range(60):
            r = 6 + i * 0.25
            for w in (2, 3):
                x, z = round(ux * r + px * w), round(uz * r + pz * w)
                if (x, z) in cells:
                    continue
                strip.add((x, z))
            if (round(ux * r), round(uz * r)) in cells:
                break
        for (x, z) in strip:
            put(b, x, 0, z, GLASS_FLOOR)
            put(b, x, -1, z, f"jugcraft:cable_tray[facing={'north' if along_x else 'east'}]")
            put(b, x, -2, z, AMBER)
            put(b, x, -3, z, CONC)
        for (x, z) in strip:
            for dx, dz in DIRS.values():
                n = (x + dx, z + dz)
                if n not in strip:
                    for y in (-1, -2):
                        if (n[0], y, n[1]) not in b:
                            put(b, n[0], y, n[1], CONC)


def corner_strips(b, put, pillar_cells):
    """White light strips (end rods) up the pillar feet's outer corners, where each face meets its chamfer, on both
    storeys of the hall."""
    hw = G.half_width(2)
    for k, (cx, cz) in G.pillar_centres(2).items():
        cells = pillar_cells(2, k)
        for fx, fz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for sgn in (-1, 1):
                a = sgn * (hw - G.CHAMFER)
                ox, oz = (cx + fx * (hw + 1), cz + a) if fx else (cx + a, cz + fz * (hw + 1))
                if (ox, oz) in cells or math.hypot(ox, oz) < G.ATRIUM_R + 1:
                    continue
                for y in list(range(1, 10)) + list(range(12, 19)):
                    if (ox, y, oz) not in b:
                        put(b, ox, y, oz, ROD)


def ceiling_rings(b, put, pillars, atrium):
    """Two rings of linear light bars in the hall's low ceiling round the opening (r 17 and 20), turned along the ring."""
    for r0 in (17, 20):
        for x in range(-r0 - 1, r0 + 2):
            for z in range(-r0 - 1, r0 + 2):
                r = math.hypot(x, z)
                if not (r0 - 0.5 <= r < r0 + 0.5) or (x, z) in pillars or atrium(x, z):
                    continue
                if b.get((x, 10, z)) != "jugcraft:ceiling_tile":
                    continue
                tx, tz = -z, x   # tangent
                facing = "north" if abs(tx) >= abs(tz) else "east"
                put(b, x, 9, z, furniture("linear_ceiling_light", facing))


# ------------------------------------------------------------------ round the holo table

def touch_consoles(b, put):
    """Five touch consoles round the holo table on its south half (the master console is on the north side), in the
    two gaps between the pillar feet and in front of the pillar A foot, each facing its user, who stands outside the
    ring looking at the hologram. Angles are clockwise from north."""
    for deg, r in ((108, 8), (144, 8), (180, 6), (216, 8), (252, 8)):
        a = math.radians(deg)
        x, z = round(r * math.sin(a)), round(-r * math.cos(a))
        facing = facing_towards(x, z, 0, 0)
        put(b, x, 1, z, furniture("touch_console", facing))
        TOUCH_CONSOLES.append((x, z, facing))


# ------------------------------------------------------------------ operator areas

def operator_areas(b, put):
    """Each operator area (the three rows of desks between the pillar feet, east and west). Layouts 8-18 had three holo
    emitters in front of each carrying the floating FLEET, TOWER and SUPPLY panels: work list item 15 (layout 19) took
    them out for the Holo Projector in the lobby (holo_projector). (Layouts 8-16 had six glass-door racks behind each;
    the servers moved into the data hall in layout 17.)"""
    EMITTERS.clear()


def holo_projector(b, put):
    """The Holo Projector (work list item 15): 3 x 3 on the lobby floor past the security gates, on the amber line to the
    holo table, between the two security desks; the head it throws up faces the entrance (north) when nobody is near."""
    px, pz = PROJECTOR
    for dz in range(-PROJECTOR_REACH, PROJECTOR_REACH + 1):
        for dx in range(-PROJECTOR_REACH, PROJECTOR_REACH + 1):
            if (dx, dz) == (0, 0):
                state = "jugcraft:holo_projector[facing=north]"
            else:
                state = f"jugcraft:holo_projector_part[ox={dx + PROJECTOR_REACH},oz={dz + PROJECTOR_REACH}]"
            put(b, px + dx, 1, pz + dz, state)


OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


# ------------------------------------------------------------------ entrance

def entrance(b, put, glass):
    """The security vestibule inside the doors: framed glass side walls (x -9 and 8, z -44..-42), the info totem, and
    the line of four speed gates (two in, two out) at z -41/-40 with glass on from the outer pedestals to the walls."""
    zn, zs = GATE_ROWS
    for z in range(-44, zn):
        for y in (1, 2, 3):
            put(b, -9, y, z, glass("east"))
            put(b, 8, y, z, glass("west"))
    for x, facing in GATE_LANES.items():
        put(b, x, 1, zn if facing == "south" else zs, f"jugcraft:speed_gate[facing={facing},open=false,part=lane]")
        b.pop((x, 1, zs if facing == "south" else zn), None)   # the lane's second block stays clear
    for x in (-4, -2, 0, 2, 4):
        put(b, x, 1, zn, "jugcraft:speed_gate_pedestal[axis=z,half=a]")
        put(b, x, 1, zs, "jugcraft:speed_gate_pedestal[axis=z,half=b]")
    for x in list(range(-9, -4)) + list(range(5, 9)):
        for y in (1, 2):
            put(b, x, y, zn, glass("south"))
    put(b, TOTEM[0], 1, TOTEM[1], furniture("tower_info_totem", "west"))


def display_cases(b, put):
    """The two Drone Display Cases past the gates (tools/drone_display_case.py), each in the lit amber floor ring the
    first plinths stood in: a museum vitrine 5x5 and four high, its master the middle cell on the hall floor, its front
    towards the entrance (north), the drone's name running round its light hood."""
    for side, (cx, cz, tier) in CASES.items():
        for x in range(cx - 3, cx + 4):
            for z in range(cz - 3, cz + 4):
                if abs(x - cx) == 3 or abs(z - cz) == 3:
                    put(b, x, 0, z, AMBER)
        for dy in range(CASE_HEIGHT):
            for dz in range(-CASE_REACH, CASE_REACH + 1):
                for dx in range(-CASE_REACH, CASE_REACH + 1):
                    if (dx, dy, dz) == (0, 0, 0):
                        state = f"jugcraft:drone_display_case[facing=north,tier={tier}]"
                    else:
                        state = f"jugcraft:drone_display_case_part[ox={dx + CASE_REACH},oy={dy},oz={dz + CASE_REACH}]"
                    put(b, cx + dx, 1 + dy, cz + dz, state)


def lounge_cells():
    """The Long Coffee Table's cells from its master, facing north (tower/LongCoffeeTableBlock.footprint): 4 along, 2 across."""
    return [(dx, dz) for dz in (-1, 0) for dx in range(-2, 2)]


def lounge(b, put):
    """One lounge west of the lobby, where the floor plan puts it: two rows of six sofas facing each other, the Lounge
    Table between them (its middle on the corner of four cells), with room to walk between it and each row."""
    for x, z, facing in LOUNGE_ROWS:
        row(b, put, "sofa", 6, x, 1, z, facing)   # laid to the user's right
    lx, lz = LOUNGE
    for dx, dz in lounge_cells():
        if (dx, dz) == (0, 0):
            state = "jugcraft:long_coffee_table[facing=north]"
        else:
            state = f"jugcraft:long_coffee_table_part[ox={dx + LOUNGE_REACH},oz={dz + LOUNGE_REACH}]"
        put(b, lx + dx, 1, lz + dz, state)


def tree_cells():
    """The Crystal Tree's cells from its master, as tower/CrystalTreeBlock.FOOTPRINT has them: the master, the planter's
    and bench's cells round it (any cell its seat's round edge reaches into), the trunk's cells over it."""
    out = [(0, 0, 0)]
    for dz in range(-TREE_REACH, TREE_REACH + 1):
        for dx in range(-TREE_REACH, TREE_REACH + 1):
            nx, nz = min(max(0.5, dx), dx + 1), min(max(0.5, dz), dz + 1)
            if (dx, dz) != (0, 0) and math.hypot(nx - 0.5, nz - 0.5) < TREE_SEAT_R:
                out.append((dx, 0, dz))
    out += [(0, dy, 0) for dy in range(1, TREE_TRUNK + 1)]
    return out


def crystal_tree(b, put):
    """The Crystal Tree south-east of the T1 display case: a round planter with a timber bench round it, the tree grown
    from its master's position (client/CrystalTreeGeometry)."""
    tx, tz = TREE
    for dx, dy, dz in tree_cells():
        if (dx, dy, dz) == (0, 0, 0):
            state = "jugcraft:crystal_tree"
        else:
            state = f"jugcraft:crystal_tree_part[ox={dx + TREE_REACH},oy={dy},oz={dz + TREE_REACH}]"
        put(b, tx + dx, 1 + dy, tz + dz, state)


def ops_tree_cells(kind):
    """A flight-ops tree's cells from its master, as tower/OpsTreeBlock.Kind.footprint has them: the master, the bed's
    cells round it (any its round edge reaches into), then over it the Ember Oak's trunk (2 cells) or the Drift Isle's
    island (2 up) and trunk (3 and 4 up)."""
    def near(dx, dz):
        nx, nz = min(max(0.5, dx), dx + 1), min(max(0.5, dz), dz + 1)
        return math.hypot(nx - 0.5, nz - 0.5)
    span = range(-OPS_REACH, OPS_REACH + 1)
    out = [(0, 0, 0)]
    out += [(dx, 0, dz) for dz in span for dx in span if (dx, dz) != (0, 0) and near(dx, dz) < OPS_RADIUS[kind]]
    if kind == "ember_oak":
        out += [(0, 1, 0), (0, 2, 0)]
    else:
        out += [(dx, OPS_ISLE_DY, dz) for dz in span for dx in span if near(dx, dz) < OPS_ISLE_R]
        out += [(0, 3, 0), (0, 4, 0)]
    return out


def ops_trees(b, put):
    """The flight-ops trees where the operator rows were (layout 45; Justin chose options A and D of the sheet): the Ember
    Oak on the west floor, the Drift Isle on the east (tools/ops_trees.py; client/OpsTreeRenderer)."""
    for kind, (tx, tz) in OPS_TREES.items():
        for dx, dy, dz in ops_tree_cells(kind):
            if (dx, dy, dz) == (0, 0, 0):
                state = f"jugcraft:{kind}"
            else:
                state = f"jugcraft:{kind}_part[ox={dx + OPS_REACH},oy={dy},oz={dz + OPS_REACH}]"
            put(b, tx + dx, 1 + dy, tz + dz, state)


# ------------------------------------------------------------------ version B: map table and service bay

def map_table(b, put):
    """The tower diorama's case (the small 3x3 holo table until layout 44), north-west of the pillar feet, with its desk
    (three wide, working the tower's build) and chair on its south side. Taken out with layout 25, back with layout 29 (Justin's walk through of 24 October 2026,
    redesign 3, approved take 2): it shows the tower diorama, a still model of the finished tower in plain Minecraft land
    (tools/tower_diorama.py, client/TowerDiorama); the tower pins it to its terminal like its other displays."""
    mx, mz = MAP_TABLE
    # the Tower Diorama Case (Justin, 27 October 2026, "its own little multi-block structure"; option A, layout 44): its
    # 36 cells over the table's 3 x 3, its anchor the middle (tools/diorama_case.py; the model drawn on its tray)
    import diorama_case as DC
    for i, (dx, dy, dz) in enumerate(DC.CASE_CELLS):
        put(b, mx + dx, 1 + dy, mz + dz, f"jugcraft:{DC.NAME}[facing=north,part={i}]")
    for x in range(mx - 1, mx + 2):
        put(b, x, 1, mz + 4, desk_state("north", x - (mx - 1), MAP_DESK_ROLE))
    put(b, mx, 1, mz + 5, furniture("operator_chair", "north"))


def service_bay(b, put):
    """The drone service bay, north-east: a 7x7 pad in the floor, the lifted cradle with a tier-4 drone in the middle,
    the 6-axis robot arm on its east side (client/ServiceArm), and the parts wall (two storage racks) along its north
    side."""
    import tower_art
    x0, z0 = SERVICE_PAD
    for dx in range(7):
        for dz in range(7):
            put(b, x0 + dx, 0, z0 + dz, f"jugcraft:hangar_pad[part={tower_art.hangar_pad_part('large', 7, 7, dx, dz)}]")
    sx, sz = SERVICE_STAND
    # facing north, the drone lies along its cradle, nose to the hall, its right side to the arm: the arm works its two
    # near fans and passes its camera along it without ever reaching over the tail fins (layout 15, work list item 8;
    # tail-on, as layout 14 had it, the arm's links would cross the fins)
    put(b, sx, 1, sz, furniture("drone_display_stand", "north", tier=4, style="service"))
    ax, az = SERVICE_ARM
    put(b, ax, 1, az, furniture("drone_service_arm", "west"))
    # the parts wall: two units of the Drone Parts Shelving (work list item 9, layout 16) facing the pad, stocked
    # differently (a: frames, batteries, motors, hardware, props, cases, the extinguisher; b: booms, ducts, gimbals, big
    # motors, long props); each is its master in the middle at the bottom and 8 parts (tower/DronePartsShelvingBlock)
    for x, stock in ((x0 + 1, "a"), (x0 + 5, "b")):
        for dy, dx in shelving_cells():
            if (dx, dy) == (0, 0):
                state = f"jugcraft:drone_parts_shelving[facing=south,stock={stock}]"
            else:
                state = f"jugcraft:drone_parts_shelving_part[ox={dx + 1},oy={dy},oz=1]"
            put(b, x + dx, 1 + dy, z0 - 2, state)


def shelving_cells():
    """The Drone Parts Shelving's cells from its master facing south (tower/DronePartsShelvingBlock.footprint): 3 across, 3 high."""
    return [(dy, dx) for dy in range(3) for dx in (-1, 0, 1)]


# ------------------------------------------------------------------ the data hall (work list item 13, layout 17)

# The room in the hall's west corner (claude/jugcraft-tripillar-data-hall.md, the approved mockup): from the hexagon's two
# west walls to x -38, z -9..8, a raised floor, clear height y 1..4 under its own ceiling (y 5). Framed glass y 1..4 on
# its three inner sides between steel posts at the corners and the door's jambs, a steel band at y 5; the reader doors
# on the cold aisle's axis, the DATA HALL light box over them.
DH_EAST, DH_NORTH, DH_SOUTH, DH_CEIL = -37, -10, 9, 5
HALL_CEIL = 10                                            # tower_tripillar.CEIL1: the hall's ceiling over the data hall
SKIN = "jugcraft:carbon_composite_panel"
DH_ROWS = (("A", -2, "north"), ("B", 1, "south"))        # row, z, facing (A's fronts south, B's north: both to the aisle)
DH_RACK_X = tuple(range(-48, -40))
DH_KINDS = {"A": "SSGNSSGS", "B": "SGSSNGSS"}
DH_KEY = {"S": "server_rack", "N": "network_rack", "G": "san_rack"}
DH_HANGERS = (-47, -44, -41)
DH_POSTS = (-10, -2, 1, 9)
DH_DOORS = ((-40, -1, "east"), (-49, 0, "west"))         # the containment doors' masters (x, z) and outsides
DH_CRACS = (("CRAC-1", 0, -46, (-7, -8)), ("CRAC-2", 1, -46, (8, 7)))
DH_UPS = (("ups_battery", -44), ("ups_battery", -43), ("ups_module", -42), ("ups_bypass", -41), ("pdu_cabinet", -40))
DH_UPS_Z = 7
DH_CYL_X, DH_CYL_Z = (-44, -43, -42), -8
DH_PIPE_Z, DH_PIPE_X = (-5, 4), (-47, -39)
DH_NOZZLES = (-46, -42)
DH_SMOKE = ((-46, -1), (-42, -1), (-46, -6), (-41, -6), (-46, 5), (-41, 5))
DH_LIGHTS = tuple((x, z) for z in (-3, 2) for x in (-47, -44, -41)) + tuple((x, z) for z in (-7, 6) for x in (-45, -40)) + ((-38, -1),)


def panel(name, facing, part, **extra):
    props = "".join(f",{k}={v}" for k, v in extra.items())
    return f"jugcraft:{name}[facing={facing},part={part}{props}]"


def variant(name, facing, v):
    return f"jugcraft:{name}[facing={facing},variant={v}]"


def door_turn(dx, dz, facing):
    """tower/AisleContainmentDoorBlock.turn: a cell of the door drawn facing north turned to ``facing``."""
    return {"east": (-dz, dx), "south": (-dx, -dz), "west": (dz, -dx)}.get(facing, (dx, dz))


def data_hall_cells():
    inside_hex = G.inside_hex
    return [(x, z) for x in range(-60, DH_EAST) for z in range(DH_NORTH + 1, DH_SOUTH)
            if inside_hex(x, z) and inside_hex(x - 1, z)]


def data_hall(b, put, glass):
    """The data hall (work list item 13): the nuclear plant's server-room pieces (tools/server_room.py) in a glass room,
    two rows of eight racks facing each other across a contained cold aisle (tools/aisle_containment.py: its doors at
    both ends, its roof), the cable ladder over them, two CRACs, the UPS lineup, the FM-200 system, the door's fire
    panels and signs. Its racks number themselves A-01.. and B-01.. (nuclear/ServerRoom), its UPS module is its hub."""
    inside_hex = G.inside_hex
    for x, z in data_hall_cells():
        perforated = x in DH_RACK_X and z in (-1, 0)
        b[(x, 0, z)] = "jugcraft:access_floor_perforated" if perforated else "jugcraft:access_floor_tile[phase=0]"
        for y in range(1, DH_CEIL):
            b.pop((x, y, z), None)
        put(b, x, DH_CEIL, z, "jugcraft:ceiling_tile")
    # the walls: glass y 1..4 between the posts, the band at y 5, the skin over it up to the hall's ceiling (as the rooms,
    # tools/tripillar_rooms.py); the doors in the east wall
    def bulkhead(x, z):
        for y in range(DH_CEIL + 1, HALL_CEIL):
            b.pop((x, y, z), None)
            put(b, x, y, z, SKIN)
    for z in range(DH_NORTH, DH_SOUTH + 1):
        for y in range(1, DH_CEIL):
            if z in (-1, 0) and y <= 2:
                continue
            put(b, DH_EAST, y, z, METAL if z in DH_POSTS else glass("west"))
        put(b, DH_EAST, DH_CEIL, z, METAL)
        bulkhead(DH_EAST, z)
    for z, face in ((DH_NORTH, "south"), (DH_SOUTH, "north")):
        x = DH_EAST - 1
        while inside_hex(x - 1, z):
            for y in range(1, DH_CEIL):
                put(b, x, y, z, glass(face))
            put(b, x, DH_CEIL, z, METAL)
            bulkhead(x, z)
            x -= 1
    # the closed space over its ceiling lit as the rooms' are (tripillar_rooms.Rooms.plenum_lights)
    lamp = "jugcraft:ceiling_light_panel"
    reach = 15 - 3 - (HALL_CEIL - 1 - (DH_CEIL + 1))
    lamps = [(x, z) for x, z in data_hall_cells() if b.get((x, HALL_CEIL - 1, z)) == lamp]
    for x, z in sorted(data_hall_cells()):
        if not any(abs(x - lx) + abs(z - lz) <= reach for lx, lz in lamps) and (x, HALL_CEIL - 1, z) not in b:
            put(b, x, HALL_CEIL - 1, z, lamp)
            lamps.append((x, z))
    # a pair of glass doors with closers (nuclear/PlantDoorBlock), facing west (pushed from the hall; "left" is south: the
    # pair hinge on their outer sides), the badge reader on the hall's side
    for z, hinge in ((-1, "right"), (0, "left")):
        for y, half in ((1, "lower"), (2, "upper")):
            put(b, DH_EAST, y, z, f"jugcraft:glass_door[exit=false,facing=west,half={half},hinge={hinge},moving=false,open=false,"
                                  f"paired=true,powered=false,reader=true]")
    # the racks, the ladder and fiber runway over each row
    for row, z, facing in DH_ROWS:
        for x, k in zip(DH_RACK_X, DH_KINDS[row]):
            for part in (0, 1):
                put(b, x, 1 + part, z, panel(DH_KEY[k], facing, part, open="false"))
            v = (1 if x in DH_HANGERS else 0) | 2 | (4 if k == "N" else 0)
            put(b, x, 3, z, variant("srv_ladder", facing, v))
    # the cross ladder over the west door, its riser up into it at hot aisle A's end
    for z in range(DH_ROWS[0][1] - 1, DH_ROWS[1][1] + 1):
        put(b, -49, 3, z, variant("srv_ladder_cross", "east", 1 if z == DH_ROWS[0][1] - 1 else 0))
    for y in (1, 2):
        put(b, -49, y, DH_ROWS[0][1] - 1, variant("srv_ladder_riser", "east", 1 if y == 1 else 0))
    # the containment: a door at each end of the cold aisle, the roof over it
    for mx, mz, facing in DH_DOORS:
        for dy in (0, 1):
            for dx in (0, 1):
                ox, oz = door_turn(dx, 0, facing)
                if (dx, dy) == (0, 0):
                    state = f"jugcraft:aisle_containment_door[facing={facing},open=false,part=none]"
                else:
                    state = f"jugcraft:aisle_containment_door_part[ox={ox + 1},oy={dy},oz={oz + 1}]"
                put(b, mx + ox, 1 + dy, mz + oz, state)
    for x in DH_RACK_X:
        put(b, x, 3, -1, furniture("aisle_containment_roof", "north"))
        put(b, x, 3, 0, furniture("aisle_containment_roof", "south"))
    # CRACs against the west walls (their fronts east: their user looks west)
    for label, v, x, (zl, zr) in DH_CRACS:
        for c, z in enumerate((zl, zr)):
            for row in (0, 1):
                put(b, x, 1 + row, z, f"jugcraft:crac_unit[facing=west,part={row * 2 + c},variant={v}]")
    # the UPS lineup (fronts north)
    for name, x in DH_UPS:
        for row in (0, 1):
            put(b, x, 1 + row, DH_UPS_Z, panel(name, "south", row))
    # FM-200: three cylinders (fronts south), the manifold, the riser, the runs under the ceiling and their nozzles
    for i, x in enumerate(DH_CYL_X):
        for row in (0, 1):
            put(b, x, 1 + row, DH_CYL_Z, panel("agent_cylinder", "north", row))
        put(b, x, 3, DH_CYL_Z, variant("agent_manifold", "north", i))
    top = DH_CEIL - 1
    put(b, DH_CYL_X[1], top, DH_CYL_Z, furniture("agent_pipe_riser_top", "north"))
    for z in range(DH_CYL_Z + 1, DH_PIPE_Z[0]):
        put(b, DH_CYL_X[1], top, z, furniture("agent_pipe", "east"))
    for z in DH_PIPE_Z:
        for x in range(DH_PIPE_X[0], DH_PIPE_X[1]):
            if x == DH_PIPE_X[0]:
                piece = variant("agent_pipe_nozzle", "north", 1)
            elif x in DH_NOZZLES:
                piece = variant("agent_pipe_nozzle", "north", 0)
            elif x == DH_CYL_X[1] and z == DH_PIPE_Z[0]:
                piece = furniture("agent_pipe_tee", "south")
            else:
                piece = furniture("agent_pipe", "north")
            put(b, x, top, z, piece)
    put(b, DH_PIPE_X[1], top, DH_PIPE_Z[0], furniture("agent_pipe_elbow", "north"))
    for z in range(DH_PIPE_Z[0] + 1, DH_PIPE_Z[1]):
        put(b, DH_PIPE_X[1], top, z, furniture("agent_pipe", "east"))
    put(b, DH_PIPE_X[1], top, DH_PIPE_Z[1], furniture("agent_pipe_elbow", "east"))
    for x, z in DH_SMOKE:
        put(b, x, top, z, furniture("smoke_detector", "north"))
    for x, z in DH_LIGHTS:
        put(b, x, top, z, furniture("linear_ceiling_light", "north"))
    # at the door: inside, the releasing panel and the horn/strobe on the north jamb; the abort station, the vacate sign,
    # the emergency light and an extinguisher on the south jamb; outside, the manual release and the signs, the light box
    for row in (0, 1):
        put(b, DH_EAST - 1, 1 + row, -2, panel("agent_release_panel", "east", row))
    put(b, DH_EAST - 1, 4, -2, panel("agent_horn_strobe", "east", 0))
    put(b, DH_EAST - 1, 1, 1, furniture("fire_extinguisher", "east"))
    put(b, DH_EAST - 1, 2, 1, furniture("agent_abort_station", "east"))
    put(b, DH_EAST - 1, 3, 1, furniture("sign_agent_vacate", "east"))
    put(b, DH_EAST - 1, 4, 1, furniture("emergency_light", "east"))
    put(b, DH_EAST + 1, 2, 1, furniture("agent_manual_release", "west"))
    put(b, DH_EAST + 1, 2, -2, furniture("sign_clean_agent", "west"))
    put(b, DH_EAST + 1, 3, -2, furniture("sign_no_food", "west"))
    for p, z in enumerate((1, 0, -1, -2)):
        put(b, DH_EAST + 1, DH_CEIL, z, f"jugcraft:data_hall_sign[facing=west,part={p}]")
