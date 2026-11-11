"""The Tri-Pillar's stations inside (work list item 20, take 1 approved by Justin 26 Oct 2026; layout 37; the cargo area
reworked in layout 40, take 2 approved 27 Oct 2026).

The yard east of the control building goes (its landing platform, spare pad, pickup and packager), and so does the
exchanges' standing outside it. Now:
- The ground floor's east point steps back to a straight intake face (x 45, z -10..10: anthracite wall panels on a
  plinth) under the upper floor, whose point roofs over it as a canopy on one steel column (item 12's girder) over a
  yard concrete apron with downlights in its soffit. The energy and cargo exchanges (the same formed stations) stand
  side by side behind it, both facing out, their fronts in the face, so a cable or pipe from outside plugs straight into
  their ports (z -5 and 5, y 2): a yellow plate over each, hazard plating in front, a badge-reader maintenance door
  between them, the bay over the door and the room's north end dark (no gap shows between the stations). Behind them
  the intake room: its aisle, its end walls, its glass wall onto the cargo hall (taller than a room's, the stations
  stand 5 high; solid behind the case packer) and a steel door in line with the glass.
- The cargo hall, east of pillar B (the security office moved south to the curtain wall, layout 40): the Case Packer
  against the intake room's wall, fed by the cargo exchange (layout 41: its infeed belt from the exchange's back over the
  aisle and through the wall, down a spout into the packer's hopper); a stacker drone palletising its cases onto the
  pallet a pallet drone holds on the pack spot; the drone's lane up to the goods lift's landing (bollards by it); the
  DISPATCH and SUPPLY console rows looking over it. The packer, the drones and the cases are look-only
  (client/warehouse/CargoHallRenderer, on the lift's round). The depot terminal and its cargo packager (the store the
  cargo exchange fills) are out of sight under the floor: the consoles show and work them.
- The goods lift (tools/goods_lift.py): a glass shaft (steel girder corners, a steel band at each slab) from a pit in
  the hall up through the engineering lab to the lift house on the roof; its landing door faces the hall (west); two
  T-rails on its back wall. The car is look-only (client/GoodsLiftRenderer): it runs up loaded while the depot's drones
  are out, to the top of the house.
- The roof: the lift house is the shaft's head, the same on all four sides (anthracite panels on a plinth between steel
  corner posts, a steel band round its top, an obstruction light on each corner); on its top, over the shaft, the 3 x 3
  supply pickup on its landing platform, where the drones load. The roof round it is the roof again (layout 36), the
  walkway running past.

The depot terminal finds its platform and its packager through the tower (TowerData's "platform" and "packager", as
DroneTerminalBlockEntity reads them: a tower's platform needs no pad and may be as small as the pickup); the exchanges
bind at their places (ExchangeStations). fit(TT, b) runs at the end of the control building.
"""
from __future__ import annotations

# ------------------------------------------------------------------ where (tower coordinates, the core at 0, 0, 0)
FACE_X = 45                    # the intake face (ground floor), z -10..10 (the curtain's glass returns beyond)
FACE_Z = (-10, 10)
ENERGY_Z = (-9, -1)            # the energy exchange's front (9 wide), its port at z -5, y 2
CARGO_Z = (1, 9)               # the cargo exchange's, its port at z 5, y 2
DOOR_Z = 0                     # the outside door between the stations
ROOM_DOOR_Z = -8               # the intake room's door from the cargo hall
ROOM = (38, 44, -9, 9)         # the intake room's inside (the stations x 40..45, the aisle x 38..39)
PART_X = 37                    # its glass wall onto the cargo hall
CANOPY_X = 46                  # ground floor cells x >= this are outdoors under the upper floor
COLUMN = (50, 0)
SHAFT = (36, 40, -15, -11)     # the lift shaft's walls; the car x 37..39, z -14..-12
HOUSE_TOP = 25                 # the lift house: the shaft head y 21..24, its steel band y 25 (the pickup's base inside it)
PICKUP = (37, -14)             # the 3 x 3 supply pickup on the house's top, over the car: its north-west corner (plates y 26)
PLATFORM_SEED = (38, HOUSE_TOP, -13)   # the platform block under the pickup's middle: the terminal's seed
# the depot terminal and its cargo packager, out of sight under the hall's floor (the consoles work the terminal; the
# cargo exchange fills the packager, the store the drones load from)
TERMINAL = (31, -1, -8)
PACKAGER = (31, -1, -2)
BOLLARDS = [(35, 1, -15), (35, 1, -10)]
# the cargo hall (layout 40): the Case Packer (a room kit piece, 3 wide, 2 deep, 2 high) with its back to the intake
# room's wall, its conveyor's stop at its south end; the pack spot west of that, the stacker drone south of the pack
# spot; the pallet drone's lane (x 33) from the lift's landing down past the pack spot
PACKER = (36, 1, 2)            # its anchor (part 0), facing west: cells x 35..36, z 2..4, y 1..2
PACKER_WALL = (1, 5)           # the intake room's wall solid behind it (z), anthracite panels
# its infeed (layout 41: "connect the case packager to the cargo exchange"): a belt from the exchange's back over the
# aisle, through the wall at y 4, down a spout into the packer's hopper; the anchor is the spout, facing west
INFEED = (36, 3, 3)
PACK_SPOT = (34, 0, 3)
LANE_X = 33
LANE_Z = (-14, 3)
DESKS = {3: (31, -9), 7: (31, -3)}     # role: (x, the row's first z) facing east, the chair west of the middle desk
PLATES = {"energy": (FACE_X + 1, 6, -5), "cargo": (FACE_X + 1, 6, 5)}
# the exchanges' master cells (ExchangeStations.masterOffset, layout 37), both facing east
MASTERS = {"energy": (45, 1, -9), "cargo": (45, 1, 1)}
SHIFT = {"energy": (25, -5), "cargo": (25, 5)}  # from the industrial layout's cargo station (x 15..20, z -4..4)

PANEL = "jugcraft:wall_panel[facing={f},lit={lit},shape={s},tone=anthracite]"
PLINTH = "jugcraft:wall_plinth[facing={f},row=lower,shape={s}]"
# A corner panel is an L: its skin faces its facing and the face clockwise of it (facing south: south and east), the
# flashing on that edge. A run's two ends turn their returns outwards, mirrored (Justin, 27 October 2026: "you're
# putting the corner ones down in one singular direction"): the facing that puts the skin on the run's face and the
# return on that end.
CORNER_FACING = {("east", -1): "east", ("east", 1): "south", ("west", -1): "north", ("west", 1): "west",
                 ("south", -1): "west", ("south", 1): "south", ("north", -1): "north", ("north", 1): "east"}


def end_facing(face, end):
    """The facing of a corner panel ending a run that faces {face}: end -1 at its low end (x or z), 1 at its high end."""
    return CORNER_FACING[(face, end)]
DOOR_CELLS = [(x, y, 0) for y in range(4) for x in range(3)]     # tools/goods_lift.DOOR_CELLS
PACKER_CELLS = [(-c, r, -k) for r in range(2) for k in range(2) for c in range(3)]   # tools/goods_lift.PACKER_CELLS
INFEED_CELLS = [(0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 1, 2), (0, 1, 3)]                # tools/goods_lift.INFEED_CELLS


def turn(dx, dz, facing):
    """A cell's offset from a piece's anchor (as it faces north) as the piece faces `facing` (tower/RoomKitBlock)."""
    return {"north": (dx, dz), "east": (-dz, dx), "south": (-dx, -dz), "west": (dz, -dx)}[facing]


def car_cells():
    x0, x1, z0, z1 = SHAFT
    return [(x, z) for x in range(x0 + 1, x1) for z in range(z0 + 1, z1)]


def piece(TT, b, name, cells, anchor, facing):
    ax, ay, az = anchor
    for i, (dx, dy, dz) in enumerate(cells):
        tx, tz = turn(dx, dz, facing)
        TT.put(b, ax + tx, ay + dy, az + tz, f"jugcraft:{name}[facing={facing},part={i}]")


def fit(TT, b):
    M, put, glass, HEX = TT.M, TT.put, TT.glass, TT.HEX
    # 1. nothing stands outside the hexagon east of x 44 any more (the old yard and stations)
    for p in [p for p in b if p[0] >= 44 and (p[0], p[2]) not in HEX and -1 <= p[1] <= 9]:
        del b[p]
    # 2. the canopy: the ground storey under the point is outdoors, the apron yard concrete, the soffit metal panels
    for (x, z) in HEX:
        if x < CANOPY_X:
            continue
        for y in range(1, TT.CEIL1):
            b.pop((x, y, z), None)
        put(b, x, 0, z, "jugcraft:yard_concrete")
        if b.get((x, TT.CEIL1, z)) == M["ceiling"]:
            put(b, x, TT.CEIL1, z, "jugcraft:aluminum_cladding")
    for y in range(1, TT.CEIL1):
        put(b, COLUMN[0], y, COLUMN[1], "jugcraft:steel_girder[axis=y]")
    for zc in (-5, 5):                                  # keep clear in front of the ports
        for z in range(zc - 1, zc + 2):
            for x in (46, 47):
                put(b, x, 0, z, "jugcraft:hazard_plating")
    # 3. the intake room: the point x 37..45, z -10..10 cleared on the ground storey (its curtain, its lights)
    for x in range(PART_X, FACE_X + 1):
        for z in range(FACE_Z[0], FACE_Z[1] + 1):
            if (x, z) in HEX:
                for y in range(1, TT.CEIL1):
                    b.pop((x, y, z), None)
    stations, _ = TT.industrial_stations()
    for key, (dx, dz) in SHIFT.items():
        for (x, y, z), state in stations["cargo"].items():
            put(b, x + dx, y, z + dz, state.replace("cargo_exchange", "energy_exchange") if key == "energy" else state)
    # the face: panels over and between the stations and out to the curtain, a door between them
    for z in range(FACE_Z[0], FACE_Z[1] + 1):
        station = ENERGY_Z[0] <= z <= ENERGY_Z[1] or CARGO_Z[0] <= z <= CARGO_Z[1]
        for y in range(1, TT.CEIL1):
            if station and y <= 5 or z == DOOR_Z and y <= 2:
                continue
            end = abs(z) == FACE_Z[1]
            f = end_facing("east", 1 if z > 0 else -1) if end else "east"
            if station and y == 6 or z == DOOR_Z:
                # a dark surround over the stations and between them (their models stand in from their cells' sides, so
                # a panel's pale liner edge would show there)
                put(b, FACE_X, y, z, M["skin"])
            elif y == 1:
                put(b, FACE_X, y, z, PLINTH.format(f=f, s="corner" if end else "plain"))
            else:
                put(b, FACE_X, y, z, PANEL.format(f=f, lit="false", s="corner" if end else "plain"))
    for half, y in (("lower", 1), ("upper", 2)):
        put(b, FACE_X, y, DOOR_Z, f"jugcraft:maintenance_door[exit=false,facing=west,half={half},hinge=left,hood=false,"
                                  f"moving=false,open=false,paired=false,powered=false,reader=true]")
    for kind, (x, y, z) in PLATES.items():
        put(b, x, y, z, f"jugcraft:{kind}_port_plate[facing=east]")
    x0, x1, z0, z1 = ROOM
    for z in (z0 - 1, z1 + 1):                          # its end walls: painted, the band at y 7, the skin over it
        for x in range(PART_X + 1, FACE_X):
            for y in range(1, 7):
                # both ends dark beside the exchanges (their models stand in from their sides: no white shows), the same
                # each end
                put(b, x, y, z, M["skin"] if x >= 40 else "jugcraft:painted_wall")
            put(b, x, 7, z, M["metal"])
            for y in range(8, TT.CEIL1):
                put(b, x, y, z, M["skin"])
    for x in range(40, FACE_X):                         # the bay between the stations, over the door's way in: dark
        for y in range(3, 7):
            put(b, x, y, DOOR_Z, M["skin"])
    for z in range(z0 - 1, z1 + 2):                     # its glass wall onto the cargo hall (solid behind the packer)
        for y in range(1, 7):
            if PACKER_WALL[0] <= z <= PACKER_WALL[1]:
                end = z in PACKER_WALL
                f = end_facing("west", -1 if z == PACKER_WALL[0] else 1) if end else "west"
                put(b, PART_X, y, z, PANEL.format(f=f, lit="false", s="corner" if end else "plain"))
            elif z == z1 + 1:
                put(b, PART_X, y, z, "jugcraft:painted_wall")      # the security office's corner
            else:
                put(b, PART_X, y, z, glass("west"))
        put(b, PART_X, 7, z, M["metal"])
        for y in range(8, TT.CEIL1):
            put(b, PART_X, y, z, M["skin"])
    for half, y in (("lower", 1), ("upper", 2)):        # the door in the glass's plane (as the glass: facing west)
        put(b, PART_X, y, ROOM_DOOR_Z, f"jugcraft:steel_door[exit=false,facing=west,half={half},hinge=right,moving=false,"
                                       f"open=false,paired=false,powered=false,reader=false]")
    for x in range(x0, x1 + 1):                         # its ceiling (y 7), lights over the aisle
        for z in range(z0, z1 + 1):
            put(b, x, 7, z, M["ceiling"])
    for z in (-8, -4, 0, 4, 8):
        put(b, 38, 6, z, M["lamp"])
    for x in range(x0, x1 + 1):                         # over it the hall's lights stay (nothing spawns in there)
        for z in range(z0, z1 + 1):
            if x % 5 == 0 and z % 5 == 0:
                put(b, x, TT.CEIL1 - 1, z, M["lamp"])
    for (x, z) in ((47, -6), (47, 0), (47, 6), (50, -2), (50, 2)):   # the canopy's downlights
        put(b, x, TT.CEIL1 - 1, z, M["lamp"])
    # 4. the shaft through both storeys: girder corners, framed glass, a steel band at each slab
    sx0, sx1, sz0, sz1 = SHAFT
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            for y in range(1, TT.ROOF):
                b.pop((x, y, z), None)
    faces = {"west": [(sx0, z) for z in range(sz0 + 1, sz1)], "east": [(sx1, z) for z in range(sz0 + 1, sz1)],
             "north": [(x, sz0) for x in range(sx0 + 1, sx1)], "south": [(x, sz1) for x in range(sx0 + 1, sx1)]}
    for y in range(1, TT.ROOF):
        for (x, z) in ((sx0, sz0), (sx1, sz0), (sx0, sz1), (sx1, sz1)):
            put(b, x, y, z, "jugcraft:steel_girder[axis=y]")
        for face, cells in faces.items():
            for (x, z) in cells:
                if face == "west" and y <= 4:
                    continue                            # the ground landing door
                put(b, x, y, z, M["metal"] if y in (TT.CEIL1, TT.FLOOR2, TT.CEIL2) else glass(face))
    piece(TT, b, "goods_lift_door", DOOR_CELLS, (sx0, 1, sz1 - 1), "west")
    for (x, z) in car_cells():                          # the pit (the car's floor stands in it at the ground)
        b.pop((x, 0, z), None)
        put(b, x, -1, z, M["conc"])
    for y in range(0, TT.ROOF + 5):                     # the two rails on the back wall (east)
        for z in (sz0 + 1, sz1 - 1):
            put(b, sx1 - 1, y, z, "jugcraft:lift_rail[facing=west]")
    for (x, z) in [(sx0, z) for z in range(sz0 + 1, sz1)]:
        for k in (1, 2):
            put(b, x - k, 0, z, "jugcraft:hazard_plating")   # the landing in front of the ground door
    for x in (sx0 - 1, sx1 + 1):                        # the lab's two ceiling lights the shaft takes, beside it
        put(b, x, 15, -12, M["lamp"])
    # the cargo hall: the terminal and the packager under the floor, the console rows, the packer, the lane, the pack spot
    put(b, *TERMINAL, "jugcraft:drone_depot_terminal[facing=east]")
    put(b, *PACKAGER, M["packager"])
    for role, (x, z0) in DESKS.items():
        for dz in range(3):
            put(b, x, 1, z0 + dz, TT.desk_state("east", dz, role))
        put(b, x - 1, 1, z0 + 1, "jugcraft:operator_chair[facing=east]")
    for p in BOLLARDS:
        put(b, *p, "jugcraft:warehouse_bollard")
    piece(TT, b, "cargo_case_packer", PACKER_CELLS, PACKER, "west")
    piece(TT, b, "cargo_infeed", INFEED_CELLS, INFEED, "west")        # (its sleeve takes the wall panel's place)
    lane = {(LANE_X, z) for z in range(LANE_Z[0], LANE_Z[1] + 1)}
    spot = (PACK_SPOT[0], PACK_SPOT[2])
    for (x, z) in sorted(lane):
        mask = 0
        for bit, (dx, dz) in ((1, (0, -1)), (2, (1, 0)), (4, (0, 1)), (8, (-1, 0))):
            if (x + dx, z + dz) not in lane and (x + dx, z + dz) != spot:
                mask |= bit
        put(b, x, 0, z, f"jugcraft:warehouse_lane[code={(x + z) % 4},mask={mask},soffit=none]")
    put(b, *PACK_SPOT, "jugcraft:warehouse_pack_spot")
    # 5. the roof: the lift house is the shaft's head, the same on all four sides; the supply pickup on its top
    for (x, z) in car_cells():
        if x == sx1 - 1 and z in (sz0 + 1, sz1 - 1):
            continue                                    # (the rails run on up through the slab: no gap in them)
        b.pop((x, TT.ROOF, z), None)                    # open: the car runs up into the house
    corners = [(sx0, sz0), (sx1, sz0), (sx0, sz1), (sx1, sz1)]
    for y in range(TT.ROOF, HOUSE_TOP + 1):
        for x in range(sx0, sx1 + 1):
            for z in range(sz0, sz1 + 1):
                edge = [f for f, cells in faces.items() if (x, z) in cells]
                if not edge and (x, z) not in corners:
                    continue
                if y == TT.ROOF:
                    put(b, x, y, z, M["conc"])
                elif y == HOUSE_TOP or not edge:
                    put(b, x, y, z, M["metal"])         # the steel corner posts and the band round its top
                elif y == TT.ROOF + 1:
                    put(b, x, y, z, PLINTH.format(f=edge[0], s="plain"))
                else:
                    put(b, x, y, z, PANEL.format(f=edge[0], lit="false", s="plain"))
    qx, qz = PICKUP
    for x in range(qx, qx + 3):                         # the pickup on its platform, over the car
        for z in range(qz, qz + 3):
            put(b, x, HOUSE_TOP, z, M["platform"])
            put(b, x, HOUSE_TOP + 1, z, f"{M['pickup']}[part={1 + (z - qz) * 3 + (x - qx)}]")
    for (x, z) in corners:                              # a steady red obstruction light on each corner (the pads fly by)
        put(b, x, HOUSE_TOP + 1, z, "jugcraft:warning_light")
