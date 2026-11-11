"""The medium plant's turbine building (turbine hall part 1; Justin, 2026-10-18: "Turbine hall (Recommended)", built in
three parts, this one "the building (realistic walls, roof, steel stairs) and the turbine-generator on its deck").

100 x 44 and 36 tall, at the same place as before (site x 124, z 38), its ground floor level with the grass:

- the shell to the detail standard's realism max (tools/exterior_kit.py, as the warehouses'): precast plinth, insulated
  metal panels with the steel columns inside at the column lines every 10, a band of framed glass high along every
  wall (the clerestory), the parapet and its coping, downpipes, wall packs by the doors, the sprinkler inlet; the
  membrane roof on its deck and girders, rooflights, smoke vents, the roof hatch over the ladder;
- the operating floor at +12 east of the laydown bay (the bay open from the ground to the crane, the roller door in
  the west end), its green walkways round the machine and across both ends, its railing along the bay;
- two enclosed stair towers with real steel stairs (riser 1/6, tread 1/4) from the ground floor to the operating floor:
  the west one in the laydown bay's corner, the east one in the north-east corner (the east exit door at its foot);
  a caged ladder up the east wall from the operating floor to the roof hatch;
- the turbine-generator (tools/turbine_set.py) on the operating floor, its two moisture separator reheaters either
  side of the LPs, their crossovers over the walkways;
- under the operating floor (part 2 rebuilds it): the pedestal with the condenser bay, the feedwater heaters, the
  pumps, the pipe rack, the plant rooms along the south wall (part 3).

The main steam comes in through the west wall high up from the pipe bridge, down the wall beside the roller door and
east under the operating floor through the pedestal to the steam chests (tools/medium_plant.py medium_site); the
isolated phase bus leaves under the generator east under the floor and out through the east wall."""
import medium_plant as MP
import nuclear_plants as P
import warehouse_bp as BP
from warehouse_bp import st

W, D, H = 100, 44, 36
FLOOR = 12                     # the operating floor's slab
COL = 10                       # the column lines along the walls
LAYDOWN = 15                   # the operating floor starts here (the laydown bay west of it, open to the crane)
TURBINE = (36, 16)             # the turbine-generator's anchor (x, z), facing east, on the operating floor
MSR_A = (49, 9)                # the reheaters' anchors: A on the north side facing east, B on the south facing west
MSR_B = (68, 33)
GROUND_ROWS = (1, 5, 9, 13, 19, 25, 29, 33, 37, 41)    # under the operating floor: the heaters' aisles, between the feed pumps
ROOF_ROWS = (3, 7, 11, 15, 21, 27, 31, 35, 40)  # the roof lights' rows: the walkways, beside and over the reheaters, the turbine
WEST_TOWER = (11, 1)           # the stair towers' insides (x0, z0): four across (two lanes), ten deep
EAST_TOWER = (95, 1)
LADDER = (98, 13)              # the caged ladder up the east wall from the operating floor to the roof hatch

TOWER_WALL = MP.PAINTED


def col_lines(n):
    keep = BP.COL
    BP.COL = COL
    try:
        return BP.col_lines(n)
    finally:
        BP.COL = keep


def shell(b):
    """The walls and the roof (BP.enclose / BP.roof with the turbine building's column lines)."""
    b.b(0, 0, 0, W - 1, 0, D - 1, MP.CONCRETE)
    b.b(1, 0, 1, W - 2, 0, D - 2, MP.EPOXY)
    cx, cz = col_lines(W), col_lines(D)
    pipes = {(x, 0) for x in (20, 40, 60, 80)} | {(x, D - 1) for x in (20, 40, 60, 80)}
    lights = {(x, 4, 0) for x in (5, 35, 65, 95)} | {(x, 4, D - 1) for x in (5, 35, 65, 95)} | \
        {(0, 4, z) for z in (5, 13, 35)} | {(W - 1, 4, z) for z in (8, 25, 35)}
    keep = BP.COL
    BP.COL = COL
    try:
        BP.enclose(b, 0, 0, W - 1, D - 1, H, pipes=pipes, lights=lights, inlet=(0, 34))
    finally:
        BP.COL = keep
    # The clerestory: framed glass high along every wall between the columns.
    # (Not where the east stair tower stands against the north and east walls.)
    tx0, tz1 = EAST_TOWER[0] - 1, EAST_TOWER[1] + 10
    for y in range(27, 32):
        for x in range(1, W - 1):
            if x not in cx:
                if x < tx0:
                    b.p(x, y, 0, MP.FGLASS["north"])
                b.p(x, y, D - 1, MP.FGLASS["south"])
        for z in range(1, D - 1):
            if z not in cz:
                b.p(0, y, z, MP.FGLASS["west"])
                if z > tz1:
                    b.p(W - 1, y, z, MP.FGLASS["east"])
    mids = [(cx[i] + cx[i + 1]) // 2 for i in range(len(cx) - 1)]
    rl = {(x, z) for x in mids for z in (7, 15, 28, 36)}
    vents = {(mids[2], 21), (mids[5], 21), (mids[8], 21)}
    BP.roof(b, 1, 1, W - 2, D - 2, H + 1, girders=set(cx[1:-1]), lights=rl - vents, vents=vents, hatch=LADDER)


def stair_tower(b, x0, z0, top_y, doors, ladder=None):
    """A stair tower inside the hall (walls round x0 - 1..x0 + 4, z0 - 1..z0 + 10 from the slab to top_y): the first
    flight up the west lane from the front (north) to the half landing at the back, the second back up the east lane
    to the landing at the front at 8, the third up the west lane again to the top landing at the back on the
    operating floor's level. doors: [(x, y, z, facing)]; ladder: (x, z) of a caged ladder from the top landing up."""
    x1, z1 = x0 + 3, z0 + 9
    for y in range(1, top_y + 1):
        for x in range(x0 - 1, x1 + 2):
            for z in (z0 - 1, z1 + 1):
                if 0 < z < D - 1 and 0 < x < W - 1:     # (the building's own wall where the tower stands against it)
                    b.p(x, y, z, TOWER_WALL)
        for z in range(z0, z1 + 1):
            for x in (x0 - 1, x1 + 1):
                if 0 < x < W - 1:
                    b.p(x, y, z, TOWER_WALL)
    # Clear the inside (the operating floor's slab crosses it) but for the slab on the ground.
    b.c(x0, 1, z0, x1, top_y, z1)
    row = lambda k: z0 + k

    def flight(xl, foot, y0, toward):
        step = 1 if toward == "south" else -1
        left, right = (xl, xl + 1) if toward == "north" else (xl + 1, xl)
        for c in range(6):
            rep, cc = divmod(c, 3)
            z = foot + step * c
            parts = {0: [(0, 0)], 1: [(1, 0), (2, 1)], 2: [(3, 1)]}[cc]
            for part, lv in parts:
                for x, side in ((left, "left"), (right, "right")):
                    b.p(x, y0 + 2 * rep + lv, z, st("steel_stair", (70, 76, 84), facing=toward, part=part, side=side))

    def landing(y, rows, edge_row, facing):
        for z in rows:
            for x in range(x0, x1 + 1):
                b.p(x, y, z, st("stair_landing", (150, 154, 156), facing=facing, edge=z == edge_row))

    flight(x0, row(2), 1, "south")
    landing(4, (row(8), row(9)), row(8), "north")
    flight(x0 + 2, row(7), 5, "north")
    landing(8, (row(0), row(1)), row(1), "south")
    flight(x0, row(2), 9, "south")
    landing(FLOOR, (row(8), row(9)), row(8), "north")
    for x, y, z, facing in doors:
        b.door(x, y, z, facing, kind="steel")
    # Lights: a vapour-tight light under each landing, an emergency light at each level.
    b.p(x0 + 3, 3, row(9), MP.furniture("vapor_tight_light", "west"))
    b.p(x0 + 1, 7, row(0), MP.furniture("vapor_tight_light", "east"))
    b.p(x1, 11, row(9), MP.furniture("vapor_tight_light", "west"))
    for y, z in ((3, row(0)), (7, row(9)), (FLOOR + 3, row(9))):
        b.p(x1 if z == row(0) else x0, y, z, st("emergency_light", (236, 236, 228), facing="west" if z == row(0) else "east"))
    if top_y > 20:
        b.p(x0 + 1, top_y, row(4), MP.furniture("vapor_tight_light", "east"))
    if ladder:
        lx, lz = ladder
        for y in range(FLOOR + 1, top_y + 1):
            b.p(lx, y, lz, st("steel_ladder", (150, 154, 156), facing="north", waterlogged=False))
    b.stairs.append((x0, z0, x1, z1))


def walkways(b):
    """The green walkways on the operating floor: along both long walls, along both sides of the machine, across both
    ends (warehouse walkway tiles, their yellow edges where the walkway stops)."""
    walk = set()
    for x in range(LAYDOWN + 1, W - 6):
        for z in (5, 6, 36, 37):
            walk.add((x, z))
    for x in range(27, 91):
        for z in (14, 15, 27, 28):
            walk.add((x, z))
    for z in range(5, 38):
        for x in (27, 28, 89, 90):
            walk.add((x, z))
    for (x, z) in walk:
        if b.get(x, FLOOR, z) != int(MP.EPOXY):
            continue
        m = sum(bit for bit, (dx, dz) in BP.LANE_SIDES if (dx + x, dz + z) not in walk)
        b.p(x, FLOOR, z, st("warehouse_walkway", (60, 130, 80), mask=m, stencil=(x * 5 + z * 3) % 11 == 0, soffit="none"))


SHELLS = (47, 56, 65)            # the condenser shells' west sides (under LP 1, 2, 3; 1A is the plant's own condenser)
COLUMNS = (34, 44, 53, 62, 71, 77, 86)   # the table's column pairs' west sides


def pedestal(b):
    """The turbine's table: a concrete deck under the operating floor from the front standard to the exciter, on two
    rows of columns between the condenser shells (the main steam and the generator's bus run under the deck)."""
    b.b(34, FLOOR - 1, 14, 87, FLOOR - 1, 28, MP.CONCRETE)
    for x in COLUMNS:
        for z in (15, 26):
            b.b(x, 1, z, x + 1, FLOOR - 2, z + 1, MP.CONCRETE)


def feed_train(b):
    """Part 2's pieces (tools/feed_train.py): the condenser shells 1B and 1C beside the plant's own condenser (1A, the
    site's), the three hotwells under them, the condensate pumps south of them, the main feed pumps in the west bay
    under the operating floor, the EHC unit by the front standard, the seal oil unit and the gas control panel by the
    generator; the lube oil reservoir in its room. The condensate pumps stand under the table's edge in front of their
    hotwells' outlets, clear of the corridor along the plant rooms."""
    for k, x in enumerate(SHELLS):
        if k:
            b.prop("condenser_shell", x, FLOOR - 5, 16, "east", variant=k)
        b.prop("condenser_hotwell", x, 1, 16, "east", variant=k)
        b.prop("condensate_pump", x + 2, 1, 28, "north", variant=k)       # in line with its hotwell's outlet
    for k, z in enumerate((14, 21)):
        b.prop("main_feed_pump", 19, 1, z, "east", variant=k)
    b.prop("ehc_unit", 30, 1, 9, "north")
    b.prop("seal_oil_unit", 89, 1, 13, "east")
    b.prop("h2_gas_panel", 89, 1, 24, "east")
    # (Part 3: along the lube oil room's south side, clear of the column at (60, 35), its pump panel facing the room.)
    b.prop("lube_oil_reservoir", 57, 1, LOR_Z, "east")
    # Its vapour extractor's vent: up out of the demister on its cover and out through the hall's south wall.
    for y in range(5, 11):
        b.p(63, y, LOR_Z + 1, MP.PIPE)
    for z in range(LOR_Z + 2, D):
        b.p(63, 10, z, MP.PIPE)
    lube_oil_room(b)


def lights(b):
    """The hall's high-bay lights, laid out here rather than by Building.light_ceilings (which only lights under slabs
    and suspended ceilings, and skips the walkway tiles): under the roof deck over the laydown bay and the operating
    floor, in rows along the walkways and beside the reheaters (a light over one only lights its top), in columns either
    side of the parked crane (x 9 to 13); under the operating floor on a 4-block grid whose rows run down the aisles
    between the feedwater heaters and the main feed pumps, and under the turbine's table between the condenser shells.
    The towers' own lights, and the site's lighting of any ceilings higher up, stay."""
    for x in range(2, W - 1, 6):
        for z in ROOF_ROWS:
            if b.get(x, H, z) == 0 and b.get(x, H + 1, z) != 0:
                b.p(x, H, z, MP.HIGH_BAY)
    for x in range(LAYDOWN + 1, W - 1, 4):
        for z in GROUND_ROWS:
            if b.get(x, FLOOR - 1, z) == 0 and b.get(x, FLOOR, z) != 0 and b.get(x, FLOOR - 2, z) == 0:
                b.p(x, FLOOR - 1, z, MP.HIGH_BAY)
    # Under the table's deck: between the condenser shells, clear of the main steam and the bus under it.
    for x in (37, 41, 54, 63, 74, 78, 82, 86):
        for z in (18, 24):
            if b.get(x, FLOOR - 2, z) == 0 and b.get(x, FLOOR - 1, z) != 0:
                b.p(x, FLOOR - 2, z, MP.HIGH_BAY)
    b.light_steps = [(4, FLOOR + 1, None)]


SGR_DOOR = 41                  # the switchgear room's double door (x, x + 1) in its north wall, at its west end
SGR_TRAY_Y = 5                 # its power trays (the load centres' tops at 3.5, the drops' block between)
SGR_RISER_X = 51               # the trays' risers up out of the floor (the cables in the duct bank under it)
SGR_TRAYS = {"d": (34, range(42, 52), range(45, 49), (43, 46, 49), 33, "north"),      # z, run, drops, hangers, riser z, its wall
             "c": (41, range(42, 52), range(43, 47), (43, 46, 50), 42, "south")}


def switchgear_room(b):
    """The turbine building's switchgear room (turbine hall part 3; x 41..54, z 33..42 under the operating floor, the
    column at (50, 35) standing in it), fitted out as the control building's (tools/switchgear_room.py): over each load
    centre's back its power tray (non-1E, the plant's grey and black), a drop down into every feeder section's gland
    plate, the trays hung from the operating floor's slab on rods; at their east ends the cables come up out of the duct
    bank under the floor in a riser against the wall and over onto the tray. The fan coil unit under the slab in the
    south-east corner, its supply duct along the front aisle (registers over it); the log desk on the east wall with the
    laptop that reads the load centres' trip units and the turbine building's one-line diagram over it, the breaker
    handling gear beside it; the LC signs on the west wall by the way in, the extinguisher, the DANGER sign outside the
    door; smoke detectors under the slab."""
    y, ty, N = 1, SGR_TRAY_Y, 2

    def put(x, yy, z, m):
        assert b.get(x, yy, z) == 0, ("turbine building switchgear room", x, yy, z, P.MATERIALS[int(b.get(x, yy, z))][0])
        b.p(x, yy, z, m)
    for z, run, drops, hangers, rz, wall in SGR_TRAYS.values():
        for x in run:
            hung = x in hangers
            piece = "cable_tray_power_down" if x in drops else "cable_tray_power"
            put(x, ty, z, MP.variant_state(piece, "north", N + (3 if hung else 0)))
            if x in drops:
                put(x, ty - 1, z, MP.variant_state("lc_cable_drop", "north", N))
            if hung:
                for k in range(ty + 1, FLOOR):
                    put(x, k, z, MP.furniture("cable_tray_rods" if k == FLOOR - 1 else "cable_tray_rod_run", "north"))
        put(SGR_RISER_X, y, rz, MP.variant_state("cable_tray_riser_floor", wall, N))
        for k in range(y + 1, ty):
            put(SGR_RISER_X, k, rz, MP.variant_state("cable_tray_riser", wall, N))
        put(SGR_RISER_X, ty, rz, MP.variant_state("cable_tray_riser_top", wall, N))
    # The cooling: the fan coil unit against the east wall over LC 1C's rear aisle, its duct north and west along the
    # front aisle (two registers), closed at the west wall.
    cy = FLOOR - 1
    put(53, cy, 41, MP.furniture("sgr_fan_coil", "north"))
    put(54, cy, 41, MP.furniture("sgr_fan_coil_end", "north"))
    put(53, cy, 40, MP.furniture("sgr_duct_hanger", "north"))
    put(53, cy, 39, MP.furniture("sgr_duct", "north"))
    put(53, cy, 38, MP.furniture("hv_duct_elbow", "south"))
    for x in range(52, 41, -1):
        piece = "sgr_duct_register" if x in (48, 44) else "sgr_duct_hanger" if x in (51, 46, 42) else "sgr_duct"
        put(x, cy, 38, MP.furniture(piece, "east"))
    put(41, cy, 38, MP.furniture("sgr_duct_end", "west"))
    for x, z in ((44, 35), (50, 40), (47, 42)):
        put(x, cy, z, MP.furniture("smoke_detector", "north"))
    # The log desk on the east wall (the phone, the logbook, the laptop on its right), the one-line diagram over it;
    # beside it the handling gear: the remote racking device, the spare breaker on its lift truck, the test set.
    for k, z in enumerate((34, 35)):
        put(54, y, z, MP.variant_state("sgr_log_desk", "east", k))
    put(54, y, 36, MP.multi_state("sgr_desk_laptop", "east", 0))
    for c in range(3):
        for row in range(2):
            put(54, y + 1 + row, 34 + c, MP.multi_state("sgr_one_line_tb", "east", row * 3 + c))
    put(54, y, 38, MP.furniture("remote_racking_device", "east"))
    put(54, y, 39, MP.furniture("breaker_lifter", "east"))
    put(54, y + 1, 39, MP.furniture("breaker_lifter_top", "east"))
    put(54, y, 40, MP.furniture("breaker_test_set", "east"))
    # The lineups' signs on the west wall by the way in; the extinguisher at the front aisle's west end; the DANGER sign
    # outside the door.
    put(41, y + 2, 35, MP.furniture("sign_lc_d", "west"))
    put(41, y + 2, 40, MP.furniture("sign_lc_c", "west"))
    b.furniture("fire_extinguisher", 41, y + 1, 38, "west")
    put(SGR_DOOR + 2, y + 1, D - 13, MP.furniture("sign_switchgear_danger", "south"))


CRANE_ANCHOR = (9, 31, 1)      # the overhead crane's anchor (tools/hall_crane.py): parked over the laydown bay, on the runway
LOR_Z = 38                     # the lube oil reservoir's row (its anchor's z: it covers 38..41)
LOR_DOOR = 61                  # the lube oil room's double door (x, x + 1)
LOR_SPK = 56                   # its sprinkler riser's column (x, on the west wall at z LOR_SPK_Z)
LOR_SPK_Z = 36
LOR_BRANCHES = (59, 63, 67)    # its branch lines (x), along z off the cross main at LOR_SPK_Z


def lube_oil_room(b):
    """The lube oil room (turbine hall part 3; x 56..69, z 33..42 under the operating floor; tools/oil_room.py): the
    main lube oil reservoir along its south side, the conditioner in the north-west corner, the clean and dirty storage
    tanks along the north wall east of the door, the transfer pumps in front of them, the drum racks along the south
    wall; the tanks' vents up and out through the south wall; the room's pre-action sprinkler system (as the cable
    spreading room's: the valve station and its releasing panel on the west wall, the riser to the slab, the cross main
    east along the middle, the branch lines with their heads over the plant), its smoke and heat detectors and the pull
    station by the door; the floor drains, the spill kit, the extinguisher; the NO SMOKING sign by the door outside."""
    y, top = 1, FLOOR - 1

    def put(x, yy, z, m):
        assert b.get(x, yy, z) == 0, ("lube oil room", x, yy, z, P.MATERIALS[int(b.get(x, yy, z))][0])
        b.p(x, yy, z, m)
    b.prop("lo_conditioner", 57, y, 33, "north")
    b.prop("lo_storage_tank", 64, y, 33, "north")
    b.prop("lo_storage_tank", 67, y, 33, "north", variant=1)
    b.prop("lo_transfer_pumps", 66, y, 36, "north")
    for x in range(65, 70):
        b.stack("pallet_rack_drums", x, y, D - 2, "south", n=1)
    # The tanks' vents on up out of their roofs and south under the slab out through the wall.
    for x in (65, 68):
        put(x, 9, 34, MP.PIPE)
        for z in range(35, D):
            if z < D - 1:
                put(x, 9, z, MP.PIPE)
            else:
                b.p(x, 9, z, MP.PIPE)
    # The sprinklers: the valve station and the releasing panel on the west wall, the riser up beside them to the slab,
    # the cross main east along the room's middle (tees at the branch lines), the branch lines north and south from it
    # (a pendent head every third block, the last at each end), one along the west wall through the riser's top.
    import spreading_room as SR
    sx, sz = LOR_SPK, LOR_SPK_Z
    b.panel(SR.VALVE, sx, y, sz, "west", 1, "LOR")
    b.panel(SR.RELEASE, sx, y, sz + 1, "west", 1, "LOR")
    put(sx, y + 2, sz + 2, MP.furniture("sign_csr_preaction", "west"))
    for k in range(y + 2, top):
        put(sx, k, sz, MP.furniture("sprinkler_riser", "west"))
    put(sx, top, sz, MP.furniture("sprinkler_riser_top", "west"))
    east = 69
    for x in range(sx + 1, east + 1):
        piece = "sprinkler_main_end" if x == east else "sprinkler_main_cross" if x in LOR_BRANCHES else "sprinkler_main"
        put(x, top, sz, MP.furniture(piece, "west"))
    for bx in (sx,) + LOR_BRANCHES:
        for z in range(33, D - 1):
            if z == sz:
                continue
            if z in (33, D - 2):
                piece, f = "sprinkler_pipe_end", "east" if z == D - 2 else "west"
            else:
                piece, f = ("sprinkler_pipe_head" if (z - sz) % 3 == 0 else "sprinkler_pipe"), "east"
            put(bx, top, z, MP.furniture(piece, f))
    for x, z in ((61, 34), (61, 40), (66, 39)):
        put(x, top, z, MP.furniture("smoke_detector", "north"))
    for x, z in ((58, 38), (65, 33), (62, 41)):
        put(x, top, z, MP.furniture("heat_detector", "north"))
    put(LOR_DOOR + 2, y + 1, 33, MP.furniture("fire_pull_station", "north"))
    # The floor drains (to the oily waste sump), the spill kit, the extinguisher; the NO SMOKING sign outside.
    for x, z in ((60, 37), (65, 37), (68, 41)):
        b.p(x, 0, z, MP.furniture("restroom_floor_drain", "north"))
    put(69, y, 37, MP.furniture("spill_kit_oil", "east"))
    b.furniture("fire_extinguisher", LOR_DOOR - 1, y + 1, 33, "north")
    put(LOR_DOOR + 2, y + 1, D - 13, MP.furniture("sign_no_smoking", "south"))


SHOP_DOOR = 77                 # the workshop's double door (x, x + 1)
SHOP_BOOTH = (81, 38)          # the welding booth's north-west corner (its screens along z 38 east of it, along x 81 south)


def workshop(b):
    """The maintenance workshop (turbine hall part 3; x 71..84, z 33..42 under the operating floor; tools/shop_room.py):
    the two workbenches along the north wall either side of the door, their pegboards over them, the tool chests, the
    flammables cabinet and the lockers; the engine lathe, the drill press and the parts washer along the west wall, the
    eyewash by them; the spare parts racks and the 20-ton press along the south wall; the welding booth in the south-east
    corner (its screens, the welding table, the MIG welder, the oxy-acetylene cart); the heat detectors (a shop's welding
    would set smoke detectors off), the extinguisher and the pull station by the door; the eye and hearing protection
    signs outside."""
    y, top = 1, FLOOR - 1

    def put(x, yy, z, m):
        assert b.get(x, yy, z) == 0, ("workshop", x, yy, z, P.MATERIALS[int(b.get(x, yy, z))][0])
        b.p(x, yy, z, m)

    def piece(name, x, yy, z, facing, v=None):
        put(x, yy, z, MP.variant_state(name, facing, v) if v is not None else MP.furniture(name, facing))
    z0 = D - 11
    for x0 in (71, 82):
        for k in range(3):
            piece("shop_bench", x0 + k, y, z0, "north", k)
            piece("shop_pegboard", x0 + k, y + 1, z0, "north", k)
    piece("shop_tool_chest", 74, y, z0, "north")
    b.panel("chem_flam_cabinet", 75, y, z0, "north", 1)
    b.furniture("fire_extinguisher", SHOP_DOOR - 1, y + 1, z0, "north")
    put(SHOP_DOOR + 2, y + 1, z0, MP.furniture("fire_alarm_pull", "north"))
    for x in (80, 81):
        b.stack("locker", x, y, z0, "north", n=2)
    # The west wall: the lathe (its headstock at the south end, on the left as its operator faces it), the drill press,
    # the parts washer; the eyewash on the south wall beside it.
    for k, z in enumerate((z0 + 5, z0 + 4, z0 + 3)):
        piece("shop_lathe", 71, y, z, "west", k)
    piece("shop_drill_press", 71, y, z0 + 7, "west")
    piece("shop_parts_washer", 71, y, z0 + 8, "west")
    b.furniture("eyewash_station", 72, y, D - 2, "south")
    put(72, y + 1, D - 2, MP.furniture("sign_eyewash", "south"))
    for x in (73, 74, 75, 76, 78, 79):
        b.stack("pallet_rack", x, y, D - 2, "south", n=2)
    piece("shop_press", 77, y, D - 2, "south")
    # The welding booth: its screens (the gap at its west side's north end its way in), the table, the welder against
    # the south wall, the torch cart against the east wall.
    bx, bz = SHOP_BOOTH
    for x in range(bx, 85):
        piece("shop_weld_screen", x, y, bz, "south")
    for z in range(bz + 2, D - 1):
        piece("shop_weld_screen", bx, y, z, "west")
    piece("shop_weld_table", 83, y, bz + 2, "north")
    piece("shop_welder", 82, y, D - 2, "south")
    piece("shop_torch_cart", 84, y, bz + 3, "east")
    # The portable gantry in the middle, a motor on its hook over a pallet.
    for k in range(3):
        piece("shop_gantry", 75 + k, y, z0 + 5, "north", k)
    for x, z in ((74, 37), (79, 40), (83, 40)):
        put(x, top, z, MP.furniture("heat_detector", "north"))
    # Outside: eye and hearing protection.
    put(SHOP_DOOR - 2, y + 1, D - 13, MP.furniture("sign_eye_protection", "south"))
    put(SHOP_DOOR + 2, y + 1, D - 13, MP.furniture("sign_hearing", "south"))


TCR_DOOR = 91                  # the turbine control room's double door (x, x + 1)
# Its line-up along the south wall, east to west (each piece's left block as you face it, facing south): the control
# system's power, controllers and I/O, the supervisory instruments, the excitation, the generator protection.
TCR_LINEUP = [("tb_tcs_power", 96, 1, None), ("tb_tcs_control", 95, 1, None), ("tb_tcs_io", 94, 1, "I/O CAB 1"),
              ("tb_tcs_io", 93, 1, "I/O CAB 2"), ("tb_tsi_rack", 92, 1, None), ("tb_excitation", 91, 2, None),
              ("tb_gen_protection", 89, 1, "SYSTEM A"), ("tb_gen_protection", 88, 1, "SYSTEM B")]
TCR_TRAY_Y = 4                 # their control cable tray (the cabinets' tops at 3, a bundle down into each)
TCR_HANGERS = (87, 90, 93, 96)


def turbine_control_room(b):
    """The turbine building's control room (turbine hall part 3; x 86..98, z 33..42 under the operating floor, the
    column at (90, 35) in it; tools/tcr_room.py): the turbine-generator's electronics along the south wall (the control
    system's power, controllers and I/O, the supervisory instruments, the excitation, the generator's two protection
    systems), their control cable tray over them on rods from the slab (a bundle down into each cabinet's top), up out of
    the duct bank in a riser in the south-west corner; the turbine control console facing them in the middle; the plant
    overview screen on the west wall; the log desk and the event printer on the north wall by the door, the master clock
    over it; the procedure shelf and the whiteboard on the east wall; the fan coil and its duct under the slab, smoke
    detectors, the extinguisher and the pull station by the door; the access floor."""
    y, top, z = 1, FLOOR - 1, D - 2

    def put(x, yy, zz, m):
        assert b.get(x, yy, zz) == 0, ("turbine control room", x, yy, zz, P.MATERIALS[int(b.get(x, yy, zz))][0])
        b.p(x, yy, zz, m)
    # Its floor: access floor tiles (an electronics room's).
    b.b(86, 0, D - 11, W - 2, 0, D - 2, MP.ACCESS_FLOOR)
    blocks = []
    for key, x, w, label in TCR_LINEUP:
        b.panel(key, x, y, z, "south", w, label)
        blocks += [x - c for c in range(w)]
    ty = TCR_TRAY_Y
    for x in blocks:
        put(x, y + 2, z, MP.furniture("control_cable_entry", "north"))
    for x in range(min(blocks) - 1, max(blocks) + 1):
        hung = x in TCR_HANGERS
        piece = ("control_tray_waterfall" if x in blocks else "control_tray") + ("_hanger" if hung else "")
        put(x, ty, z, MP.furniture(piece, "north"))
        if hung:
            for k in range(ty + 1, FLOOR):
                put(x, k, z, MP.furniture("cable_tray_rods" if k == FLOOR - 1 else "cable_tray_rod_run", "north"))
    rx = min(blocks) - 2
    put(rx, y, z, MP.variant_state("cable_tray_riser_floor", "west", 2))
    for k in range(y + 1, ty):
        put(rx, k, z, MP.variant_state("cable_tray_riser", "west", 2))
    put(rx, ty, z, MP.variant_state("cable_tray_riser_top", "west", 2))
    # The console facing the line-up, its chair; the overview screen on the west wall.
    b.panel("cr_console", 93, y, z - 4, "south", 3, "TURBINE CONTROL")
    put(92, y, z - 5, MP.furniture("operator_chair", "south"))
    MP.wall_screen(b, "plant_map_screen", 86, y + 1, z - 3, "west")
    # The north wall: the extinguisher and the pull station either side of the door, the master clock over it; the log
    # desk (its phone and log) and the event printer east of the door, the desk's chair.
    b.furniture("fire_extinguisher", TCR_DOOR - 1, y + 1, D - 11, "north")
    put(TCR_DOOR + 2, y + 1, D - 11, MP.furniture("fire_alarm_pull", "north"))
    b.panel("ops_wall_clock", TCR_DOOR + 1, y + 2, D - 11, "north", 1)
    for k, x in enumerate((94, 95)):
        put(x, y, D - 11, MP.variant_state("sgr_log_desk", "north", k))
    put(96, y, D - 11, MP.furniture("soe_printer", "north"))
    put(95, y, D - 10, MP.furniture("operator_chair", "north"))
    put(86, y, D - 11, MP.furniture("drawing_rack", "north"))
    # The east wall: the procedure shelf, the whiteboard.
    put(98, y, 37, MP.furniture("ops_procedure_shelf", "east"))
    b.varied("whiteboard", 98, y + 1, 35, "east", 24)
    # Under the slab: the fan coil in the north-east corner, its duct west along z 36 (two registers), smoke detectors.
    put(98, top, 33, MP.furniture("sgr_fan_coil", "south"))
    put(97, top, 33, MP.furniture("sgr_fan_coil_end", "south"))
    put(98, top, 34, MP.furniture("sgr_duct_hanger", "south"))
    put(98, top, 35, MP.furniture("sgr_duct", "south"))
    put(98, top, 36, MP.furniture("hv_duct_elbow", "west"))
    for x in range(97, 86, -1):
        piece = "sgr_duct_register" if x in (94, 88) else "sgr_duct_hanger" if x in (95, 91, 87) else "sgr_duct"
        put(x, top, 36, MP.furniture(piece, "east"))
    put(86, top, 36, MP.furniture("sgr_duct_end", "west"))
    for x, zz in ((88, 39), (94, 39), (96, 34)):
        put(x, top, zz, MP.furniture("smoke_detector", "north"))


# The hall's fittings (turbine hall part 3; tools/hall_fittings.py).
# Fire points: a hose reel and beside it (to its right as you face it) a page station with its horn two blocks over it.
FIRE_POINTS = [(47, 1, D - 13, "south"), (67, 1, D - 13, "south"), (83, 1, D - 13, "south"), (1, 1, 34, "west"),
               (43, FLOOR + 1, 1, "north"), (73, FLOOR + 1, 1, "north"), (44, FLOOR + 1, D - 2, "south"), (74, FLOOR + 1, D - 2, "south")]
COMPASS_Y = 15                 # the crane travel signs' bottom (two blocks by two, on the walls)
# (Between the walls' columns: every 10 along the long walls, 20 and 30 along the ends.)
COMPASS_SIGNS = [(24, 1, "north", "N"), (64, 1, "north", "N"), (25, D - 2, "south", "S"), (65, D - 2, "south", "S"),
                 (1, 22, "west", "W"), (W - 2, 21, "east", "E")]
LAYDOWN_AREA = (2, 13, 13, 29)  # the laydown bay's painted area (x0, z0, x1, z1)
HEARING_SIGNS = [(1, 2, 32, "west"), (14, 2, 12, "north"), (93, 2, 10, "east"), (16, FLOOR + 2, 10, "west"), (93, FLOOR + 2, 10, "east")]


def hall_fittings(b):
    """The hall's fittings: the crane travel direction signs high on the walls (the pendant's NORTH, SOUTH, WEST, EAST);
    the fire points (a hose reel, the plant page station beside it and its horn over them) along the plant rooms'
    corridor, by the west door and along both long walls of the operating floor; the laydown bay's floor paint (its
    yellow edge, LAYDOWN AREA across it, the keep-clear hatching in front of the west door and the stair tower's door);
    the hearing protection signs at every way into the hall."""
    def put(x, y, z, m):
        assert b.get(x, y, z) == 0, ("turbine hall fittings", x, y, z, P.MATERIALS[int(b.get(x, y, z))][0])
        b.p(x, y, z, m)
    for x, z, facing, label in COMPASS_SIGNS:
        b.panel("tb_crane_compass", x, COMPASS_Y, z, facing, 2, label)
    for x, y, z, facing in FIRE_POINTS:
        put(x, y, z, MP.furniture("tb_hose_reel", facing))
        rx, rz = P.DIRS[P.CLOCKWISE[facing]]
        put(x + rx, y, z + rz, MP.furniture("tb_page_station", facing))
        put(x + rx, y + 2, z + rz, MP.furniture("tb_page_horn", facing))
    x0, z0, x1, z1 = LAYDOWN_AREA
    mark = lambda x, z, v, facing: put(x, 1, z, MP.variant_state("tb_floor_mark", facing, v))
    for x in range(x0 + 1, x1):
        mark(x, z0, 0, "north")
        mark(x, z1, 0, "south")
    for z in range(z0 + 1, z1):
        mark(x0, z, 0, "west")
        mark(x1, z, 0, "east")
    for x, z, facing in ((x0, z0, "north"), (x1, z0, "east"), (x1, z1, "south"), (x0, z1, "west")):
        mark(x, z, 1, facing)
    # LAYDOWN AREA reads from the roller door (looking east): its lines' tops to the east, along +z.
    tx, tz = (x0 + x1) // 2 - 1, (z0 + z1) // 2 - 3
    for r in range(2):
        for c in range(6):
            mark(tx + 1 - r, tz + c, 3 + r * 6 + c, "east")
    for x, z in ((1, 30), (1, 31), (2, 30), (2, 31), (13, 12)):
        mark(x, z, 2, "north")
    for x, y, z, facing in HEARING_SIGNS:
        put(x, y, z, MP.furniture("sign_hearing", facing))


def turbine_hall(s, ox, oz):
    b = MP.Building("pwr_turbine_hall", "Turbine Building", W, D, H, grounded=True)
    b.high_bay = True
    shell(b)
    # The operating floor at +12 east of the laydown bay; the railing along its edge over the bay.
    b.b(LAYDOWN, FLOOR, 1, W - 2, FLOOR, D - 2, MP.EPOXY)
    # The turbine's table (part 2): its deck under the operating floor on two rows of columns, the condenser shells
    # up through it under the LPs.
    pedestal(b)
    # Column lines under the operating floor.
    for x in range(20, W - 1, 10):
        for z in (8, D - 9):
            if b.get(x, 1, z) == 0:
                b.b(x, 1, z, x, 11, z, MP.COLUMN)
    # Crane runway beams along both long walls (the crane and its runway rebuilt in part 3).
    for x in range(1, W - 1):
        b.p(x, 30, 1, MP.FRAME)
        b.p(x, 30, D - 2, MP.FRAME)
    # The laydown bay's roller door (west end), the personnel doors with their canopies.
    MP.roller_door(b, [(0, z) for z in range(17, 27)], 1, 9, "east")
    b.door(0, 1, 31, "east", kind="steel", double=True, exit_=True)
    # Plant rooms along the south wall on the ground floor (part 3): the switchgear room (x 41..54), the lube oil room,
    # the workshop, the turbine control room.
    b.wall_x(D - 12, 40, W - 2, 1, 11, MP.CONCRETE)
    for x in (40, 55, 70, 85):
        b.wall_z(x, D - 11, D - 2, 1, 11, MP.CONCRETE)
    for x, label in ((SGR_DOOR, "switchgear_room"), (LOR_DOOR, "lube_oil_room"), (77, "workshop"), (91, "turbine_control_room")):
        b.door(x, 1, D - 12, "south", double=True, label=label)
    # The switchgear room's load centres (tools/load_centre.py): LC 1C along the south wall facing north, LC 1D along the
    # north wall facing south, each with a rear aisle a block wide, the front aisle four wide between them.
    b.prop("switchgear_lineup", 42, 1, D - 4, "south", variant=2)
    b.prop("switchgear_lineup", 42, 1, D - 10, "north", variant=3)
    switchgear_room(b)
    workshop(b)
    turbine_control_room(b)
    # The stair towers.
    wx, wz = WEST_TOWER
    stair_tower(b, wx, wz, 15, [(wx + 2, 1, wz + 10, "north"), (wx + 4, FLOOR + 1, wz + 8, "west")])
    b.b(wx - 1, 16, wz - 1, wx + 4, 16, wz + 10, MP.CONCRETE_DARK)
    ex, ez = EAST_TOWER
    stair_tower(b, ex, ez, 15, [(ex - 1, 1, ez + 8, "east"), (ex - 1, FLOOR + 1, ez + 8, "east"), (W - 1, 1, ez + 5, "west")])
    b.b(ex - 1, 16, ez - 1, W - 2, 16, ez + 10, MP.CONCRETE_DARK)
    # The caged ladder up the east wall from the operating floor to the roof hatch.
    lx, lz = LADDER
    for y in range(FLOOR + 1, H + 1):
        b.p(lx, y, lz, st("steel_ladder", (150, 154, 156), facing="west", waterlogged=False))
    # The railing along the operating floor's edge over the laydown bay (south of the west tower).
    for z in range(wz + 11, D - 1):
        b.rail(LAYDOWN, FLOOR + 1, z, "west")
    walkways(b)
    # Feedwater heater trains on the ground floor north of the table; the feed train and the oil plant (part 2).
    for x0 in (18, 56):
        for z in (2, 6, 10):
            b.prop("feedwater_heater", x0, 1, z, "east")
    for x in (30, 35):
        b.prop("pump_skid", x, 1, 29, "north")
    feed_train(b)
    # The pipe rack along the north wall (pipes on brackets, two tiers), clear of the towers.
    for x in range(1, W - 1):
        for y in (8, 10):
            if b.get(x, y, 1) == 0 and not (wx - 1 <= x <= wx + 4) and not (ex - 1 <= x):
                b.p(x, y, 1, MP.PIPE)
    # The reheaters beside the LPs (their crossovers over the walkways to the turbine's intercept valves), the crane.
    b.prop("moisture_separator_reheater", MSR_A[0], FLOOR + 1, MSR_A[1], "east", variant=0)
    # (MSR_B is its anchor, as the client turns it; Building.prop takes the footprint's corner nearest the origin.)
    _, mw, md, _ = P.EQUIPMENT_SIZES["moisture_separator_reheater"]
    b.prop("moisture_separator_reheater", MSR_B[0] - md + 1, FLOOR + 1, MSR_B[1] - mw + 1, "west", variant=1)
    # The crane (part 3 rebuilds it): its bridge's end trucks against the runway beams.
    b.prop("overhead_crane", CRANE_ANCHOR[0], CRANE_ANCHOR[1], CRANE_ANCHOR[2], "east")
    hall_fittings(b)
    lights(b)
    b.hangers = []
    s.add(b, ox, oz)
    return b
