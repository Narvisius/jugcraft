"""The automated warehouse's pre-made buildings (part W3; claude/jugcraft-warehouse-plan.md): the Small Warehouse (two
floors, the minimum working set and room to grow), the Large Warehouse (four floors, two lifts, two Ready Storages)
and the medium nuclear plant's warehouse, built with the plant kit's Building (tools/medium_plant.py) and the
warehouse's own blocks. Outside they are real buildings (realism max, tools/exterior_kit.py): insulated metal panels on
a precast plinth, ribbon windows of framed glass, the parapet and its coping, downpipes, wall packs, the membrane
roof with rooflights and smoke vents, the stair tower with real steel stairs; inside they are working warehouses: lane floors and aisles, racks five places high, the intakes and
palletizing stations and Ready Storage behind the Goods In and Goods Out panels in the front wall, the control office
with its terminal at a window over the floor, the Pallet Lifts joining the floors, docks on every floor, pedestrian
walkways railed off from the drones with a crossing where they meet, aisle signs, rack end barriers, high-bay lights,
and the sprinklers (the cable spreading room's system, with in-rack lines fitted to the racks).

Building coordinates: x across the front, z from the back (0) to the front wall (d - 1), y up from the ground floor
slab (walked at 1). Floor n's slab is at (n - 1) * FH."""
import medium_plant as MP
import nuclear_plants as P
from nuclear_plants import M

FH = 8                        # floor to floor: the slab, six clear for racks five places high and the drones' masts, a
                              # block for the lights and sprinklers under the next slab
RACK_LEVELS = 5
LANE_SIDES = ((1, (0, -1)), (2, (1, 0)), (4, (0, 1)), (8, (-1, 0)))      # mask bits: N, E, S, W

PIECE_SIZE = {"warehouse_intake": (3, 2, 2), "warehouse_station": (1, 2, 2), "warehouse_terminal": (3, 2, 1),
              "warehouse_goods_in": (3, 2, 1), "warehouse_ready_storage": (3, 4, 2), "warehouse_ready_storage_ext": (3, 4, 2),
              "warehouse_goods_out": (3, 2, 1), "warehouse_lift_landing": (3, 3, 2), "warehouse_lift_shaft": (3, 1, 2),
              "warehouse_lift_drive": (3, 2, 2), "warehouse_drum_filler": (3, 3, 2), "warehouse_drum_emptier": (3, 3, 2),
              "warehouse_liquids_in": (3, 2, 1), "warehouse_liquids_out": (3, 2, 1),
              "warehouse_switchboard": (3, 2, 2), "warehouse_status_board": (3, 2, 1), "warehouse_signal_box": (1, 1, 1)}

EPOXY = M("jugcraft:epoxy_floor", (176, 180, 178))
PACK_SPOT = M("jugcraft:warehouse_pack_spot", (176, 180, 178))


def st(name, rgb=(120, 124, 128), **props):
    inner = ",".join(f"{k}={str(v).lower() if isinstance(v, bool) else v}" for k, v in sorted(props.items()))
    return M(f"jugcraft:{name}" + (f"[{inner}]" if inner else ""), rgb)


def piece(b, name, x, y, z, facing):
    """A warehouse piece placed whole: x, y, z its origin (the front row's middle column, at the bottom), facing the way
    its user looks (WarehousePiece.offset)."""
    w, h, d = PIECE_SIZE[name]
    fx, fz = P.DIRS[facing]
    rx, rz = P.DIRS[P.CLOCKWISE[facing]]
    for r in range(h):
        for k in range(d):
            for c in range(w):
                p = (r * d + k) * w + c
                dc, dk = c - w // 2, (d - 1) - k
                b.p(x + rx * dc + fx * dk, y + r, z + rz * dc + fz * dk, st(name, (90, 96, 104), facing=facing, part=p))


class Floor:
    """One floor's plan: its lane cells, walkway cells, crossings, rack places, pack spots, as (x, z)."""

    def __init__(self, n):
        self.n = n
        self.y = (n - 1) * FH
        self.lanes, self.walk, self.spots = set(), set(), set()
        self.cross = {}
        self.racks = {}


def cells(x0, z0, x1, z1):
    return {(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)}


def lay_floor(b, fl, x0, z0, x1, z1, holes=(), other=EPOXY, skip=(), soffit=None):
    """The floor's slab: lane tiles (each with its edge mask and code), walkway tiles, crossings, pack spots, and other
    under the rest; holes: cells left open (the stair, the lift shafts); skip: cells left as they are. soffit (a
    floor over another): soffit(x, z) is "deck_x" or "girder_z" (warehouse/Soffit.java), what the slab shows underneath; the rest of it is the
    Warehouse Slab."""
    holes = set(holes)
    put = b.p

    class _B:
        pass
    sb = _B()

    def p(x, y, z, m):
        sf = soffit(x, z) if soffit else "none"
        if m == other and soffit:
            m = st("warehouse_slab", (176, 180, 178), soffit=sf)
        elif m:
            stt = P.MATERIALS[m][0]
            if stt.startswith("jugcraft:warehouse_") and "[" in stt:
                m = P.M(stt[:-1] + f",soffit={sf}]", P.MATERIALS[m][1])
        put(x, y, z, m)
    sb.p = p
    b = sb
    skip = set(skip)
    lanes = fl.lanes | set(fl.cross)
    walks = fl.walk | set(fl.cross)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) in skip or (x, z) in holes:
                # A hole (a lift's shaft) is left as it is: the shaft section that runs through the slab.
                continue
            if (x, z) in fl.spots:
                b.p(x, fl.y, z, PACK_SPOT)
            elif (x, z) in fl.cross:
                b.p(x, fl.y, z, st("warehouse_crossing", (200, 200, 200), axis="x" if fl.cross[(x, z)] else "z"))
            elif (x, z) in fl.lanes:
                m = sum(bit for bit, (dx, dz) in LANE_SIDES if (x + dx, z + dz) not in lanes and (x + dx, z + dz) not in fl.spots)
                b.p(x, fl.y, z, st("warehouse_lane", (176, 180, 178), code=(x * 7 + z * 13) % 4, mask=m))
            elif (x, z) in fl.walk:
                m = sum(bit for bit, (dx, dz) in LANE_SIDES if (x + dx, z + dz) not in walks)
                b.p(x, fl.y, z, st("warehouse_walkway", (60, 130, 80), mask=m, stencil=(x * 5 + z * 3) % 7 == 0))
            else:
                b.p(x, fl.y, z, other)


def rack_rows(b, fl, x0, x1, rows, sprinklers=(2, 4)):
    """Rows of racks along x (rows [(z, facing)]), each place joined as the game joins them (bays of two by where they
    lie), the levels in sprinklers with in-rack lines fitted."""
    for z, facing in rows:
        rs = P.DIRS[P.CLOCKWISE[facing]][0]
        for x in range(x0, x1 + 1):
            fl.racks[(x, z)] = facing
            for lv in range(RACK_LEVELS):
                level = "ground" if lv == 0 else "first" if lv == 1 else "upper"
                b.p(x, fl.y + 1 + lv, z, st("warehouse_rack", (30, 76, 150), facing=facing, left=(not x0 <= x - rs <= x1 or (x * rs) % 2 == 0),
                                            right=not x0 <= x + rs <= x1, level=level, top=lv == RACK_LEVELS - 1, load=0,
                                            sprinkler=lv in sprinklers))
    fl.lanes -= set(fl.racks)


def lift(b, x, z, facing, floors):
    """A Pallet Lift whose column stands at x (the middle), z (the front row) on each floor: a landing on each, shaft
    sections between, the drive over the top landing."""
    ys = sorted(fl.y + 1 for fl in floors)
    for i, y in enumerate(ys):
        piece(b, "warehouse_lift_landing", x, y, z, facing)
        if i + 1 < len(ys):
            for s in range(y + 3, ys[i + 1]):
                piece(b, "warehouse_lift_shaft", x, s, z, facing)
    piece(b, "warehouse_lift_drive", x, ys[-1] + 3, z, facing)


def docks(b, fl, at, facing):
    """Charging docks (a drone parks on the dock block, the lane tile under it), each with its conduit up the wall."""
    for (x, z) in at:
        b.p(x, fl.y + 1, z, st("warehouse_dock", (70, 72, 76), facing=facing))
        fl.lanes.add((x, z))
        for y in range(fl.y + 2, fl.y + FH):
            b.p(x, y, z, st("warehouse_conduit", (176, 180, 182), facing=facing))


def ceiling_fittings(b, fl, x0, x1, z0, z1, main_x):
    """Under the slab over a floor: the sprinkler cross main along z at main_x with its branch lines along x over the
    aisles (a pendent head every third block), high-bay lights over the aisles, smoke detectors between."""
    y = fl.y + FH - 1
    rows = {z for (_, z) in fl.racks}
    aisles = [z for z in range(z0, z1 + 1) if z not in rows]
    branches = [z for z in aisles if z - 1 in rows or z + 1 in rows or z == z0 or z == z1]
    branches = sorted(set(branches[::2]) | {aisles[0], aisles[-1]})
    for z in range(branches[0], branches[-1] + 1):
        if b.get(main_x, y, z) == 0:
            name = "sprinkler_main_end" if z in (branches[0], branches[-1]) else "sprinkler_main_cross" if z in branches else "sprinkler_main"
            b.p(main_x, y, z, MP.furniture(name, "north" if z == branches[0] else "south"))
    for z in branches:
        for x in range(x0, x1 + 1):
            if x == main_x or b.get(x, y, z):
                continue
            if x in (x0, x1):
                name, f = "sprinkler_pipe_end", "north" if x == x1 else "south"
            else:
                name, f = ("sprinkler_pipe_head" if (x - main_x) % 3 == 0 else "sprinkler_pipe"), "north"
            b.p(x, y, z, MP.furniture(name, f))
    # The lights: over every aisle between rack rows (its first cell), and every fourth row of the open floor.
    lit = [z for z in aisles if (z + 1 in rows and z - 1 not in rows) or (z - 1 not in rows and z + 1 not in rows and (z - z0) % 4 == 1)]
    for z in lit:
        for x in range(x0 + 2, x1, 4):
            for zz in (z, z + 1 if z + 1 in aisles else z):
                if b.get(x, y, zz) == 0:
                    b.p(x, y, zz, MP.HIGH_BAY)
                    break
    for z in aisles[1::4]:
        for x in range(x0 + 4, x1, 8):
            if b.get(x, y, z) == 0:
                b.p(x, y, z, MP.furniture("smoke_detector", "north"))


def aisle_fittings(b, fl, x0, x1):
    """An aisle sign hung at both ends of every aisle between rack rows (over its first cell), and the rack end barriers
    across the rows' ends."""
    rows = sorted({z for (x, z) in fl.racks})
    for z in range(min(rows) - 2, max(rows) + 2):
        if z in rows:
            for x, f in ((x0 - 1, "east"), (x1 + 1, "west")):
                if b.get(x, fl.y + 1, z) == 0:
                    b.p(x, fl.y + 1, z, st("warehouse_end_barrier", (236, 184, 20), facing=f))
            continue
        if (z + 1 in rows and z - 1 not in rows) and z >= 3:
            for x, f in ((x0 - 1, "east"), (x1 + 1, "west")):
                if b.get(x, fl.y + FH - 2, z) == 0:
                    b.p(x, fl.y + FH - 2, z, st("warehouse_aisle_sign", (24, 60, 120), facing=f))


def framed_band(b, y0, y1, sides, step=8, skip=(), D=None, W=None):
    """A band of framed glass in the outer walls between the pilasters (Justin: every window framed glass)."""
    w, _, d = b.size
    d = D if D is not None else d
    w = W if W is not None else w
    for x in range(1, w - 1):
        if x % step == 0 or x in skip:
            continue
        for y in range(y0, y1 + 1):
            if "n" in sides:
                b.p(x, y, 0, MP.FGLASS["north"])
            if "s" in sides and b.get(x, y, d - 1) != 0 and "warehouse" not in P.MATERIALS[b.get(x, y, d - 1)][0]:
                b.p(x, y, d - 1, MP.FGLASS["south"])
    for z in range(1, d - 1):
        if z % step == 0 or z in skip:
            continue
        for y in range(y0, y1 + 1):
            if "w" in sides:
                b.p(0, y, z, MP.FGLASS["west"])
            if "e" in sides:
                b.p(w - 1, y, z, MP.FGLASS["east"])


# ------------------------------------------------------------------------------------------------ the shell
# Realism max (Justin, 2026-10-17): the walls are insulated metal panels over a precast plinth, an anthracite band and
# parapet with its coping at the top, steel columns inside at the column lines, downpipes and wall packs outside;
# the roof a membrane on the metal deck with its girders, rooflights and smoke vents (tools/exterior_kit.py).
COL = 8                       # the column grid along the walls
TOWER = 5                     # the stair tower beside the east wall: its inside four across and its east wall (the
                              # building's east wall is its west wall)
TOWER_D = 10                  # its inside front to back: the floor landing (2), a flight (6), the half landing (2)
CORNER_FACING = {(0, 0): "north", (1, 0): "east", (0, 1): "west", (1, 1): "south"}
YARD = M("jugcraft:yard_concrete", (168, 166, 160))


def col_lines(n):
    """The column lines along a wall n blocks long: both ends and every COL between (one closer than 4 to the far
    end is left out)."""
    return sorted({0, n - 1} | {i for i in range(COL, n - 1, COL) if i <= n - 5})


def wall_state(facing, tone="silver", shape="plain", lit=False):
    return st("wall_panel", (196, 200, 202) if tone == "silver" else (66, 70, 74), facing=facing, lit=lit, shape=shape,
              tone=tone)


def plinth_state(facing, row, shape="plain"):
    return st("wall_plinth", (184, 182, 174), facing=facing, row=row, shape=shape)


def parapet_state(facing, shape="plain"):
    return st("parapet", (60, 64, 68), facing=facing, shape=shape)


def enclose(b, x0, z0, x1, z1, top, tone="silver", band=True, sides="nsew", columns=True, pipes=(), lights=(),
            inlet=None):
    """The walls round x0..x1, z0..z1 (sides: which of them to build), from the plinth to the parapet (the roof slab
    at top + 1, the parapet over it): precast plinth (two rows), panels (tone; the top row anthracite if band), the
    column lines (joint covers outside, columns inside if columns), corners. pipes: [(x, z)] downpipes; lights:
    [(x, y, z)] wall packs; inlet: (x, z) the sprinkler inlet."""
    cols_x, cols_z = set(col_lines(x1 - x0 + 1)), set(col_lines(z1 - z0 + 1))
    pipes, lights = set(pipes), set(lights)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge_x, edge_z = x in (x0, x1), z in (z0, z1)
            if not (edge_x or edge_z):
                continue
            side = "n" if z == z0 else "s" if z == z1 else "w" if x == x0 else "e"
            if edge_x and edge_z:
                if "w" not in sides and x == x0 or "e" not in sides and x == x1:
                    continue
                facing = CORNER_FACING[(int(x == x1), int(z == z1))]
                kind = "corner"
            else:
                if side not in sides:
                    continue
                facing = {"n": "north", "s": "south", "w": "west", "e": "east"}[side]
                along = x - x0 if side in "ns" else z - z0
                line = along in (cols_x if side in "ns" else cols_z)
                kind = "pipe" if (x, z) in pipes else ("column" if columns else "joint") if line else "plain"
            for y in range(1, top + 3):
                if y <= 2:
                    row = "lower" if y == 1 else "upper"
                    shape = kind if kind in ("corner", "pipe") else "joint" if kind in ("column", "joint") else "plain"
                    if (x, z) == inlet and row == "lower":
                        shape = "inlet"
                    b.p(x, y, z, plinth_state(facing, row, shape))
                elif y <= top + 1:
                    t = "anthracite" if band and y == top + 1 and tone == "silver" else tone
                    shape = kind
                    if kind == "plain" and (x, y, z) in lights:
                        shape = "light"
                    b.p(x, y, z, wall_state(facing, t, shape))
                else:
                    shape = "corner" if kind == "corner" else "scupper" if kind == "pipe" else "plain"
                    b.p(x, y, z, parapet_state(facing, shape))


def roof(b, x0, z0, x1, z1, y, girders=(), lights=(), vents=(), hatch=None):
    """The roof inside the walls at y: membrane on the deck (its girders along z at the x in girders), rooflights,
    smoke vents, a hatch."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) in lights:
                b.p(x, y, z, st("rooflight", (226, 232, 230)))
            elif (x, z) in vents:
                b.p(x, y, z, st("smoke_vent", (60, 64, 68), open=False))
            elif (x, z) == hatch:
                b.p(x, y, z, st("roof_hatch", (188, 192, 194), open=False))
            else:
                b.p(x, y, z, st("roof_deck", (176, 180, 180), axis="x", girder=x in girders))


def shell(b, W, D, h, zb):
    """The hall's walls and roof (and the ground slab under them); the stair tower's walls (z zb - 1..D - 1 beside
    the east wall) come with the tower."""
    b.b(0, 0, 0, W - 1, 0, D - 1, YARD)
    cx = col_lines(W)
    cz = col_lines(D)
    pipes = {(x, 0) for x in cx[1:-1][::2]} | {(0, z) for z in cz[1:-1][:1]}
    lights = {(x, 4, 0) for x in range(4, W - 2, 12)} | {(0, 4, z) for z in range(4, D - 2, 12)} | \
        {(W - 1, 4, z) for z in range(4, zb - 3, 12)} | {(x, 4, D - 1) for x in (1, W - 2)}
    enclose(b, 0, 0, W - 1, D - 1, h, pipes=pipes, lights=lights, inlet=(2, D - 1))
    mids = [(cx[i] + cx[i + 1]) // 2 for i in range(len(cx) - 1)]
    zs = [z for z in range(4, D - 3, 6)]
    rl = {(x, z) for x in mids for z in zs}
    vents = {(mids[len(mids) // 2], zs[len(zs) // 2])}
    rl -= vents
    if len(zs) > 2:
        vents.add((mids[len(mids) // 2 - 1] if len(mids) > 1 else mids[0], zs[0]))
        rl -= vents
    roof(b, 1, 1, W - 2, D - 2, h + 1, girders=set(cx[1:-1]), lights=rl, vents=vents)


def canopy(b, x0, x1, y, z, facing):
    for x in range(x0, x1 + 1):
        part = "single" if x0 == x1 else "left" if x == x0 else "right" if x == x1 else "mid"
        b.p(x, y, z, st("door_canopy", (60, 64, 68), facing=facing, part=part))


# ------------------------------------------------------------------------------------------------ the stair tower
def stair_tower(b, W, D, fls, top):
    """The enclosed stair tower on the building's east side (trapezoidal cladding, a glazed strip up its front), one
    switchback a floor so it stacks floor on floor like the lift: real steel stairs (riser 1/6, tread 1/4). On each
    floor its landing at the front (its door through the building's east wall onto the walkway; on floor 1 a double
    exit door out front), the first flight up the west lane to the half landing at the back, the second back along
    the east lane to the next floor's landing. On the top floor a ladder up to the roof hatch. Inside x W..W + 3,
    z D - 11..D - 2."""
    x0, x1 = W, W + 3
    zf = D - 2
    zb = zf - (TOWER_D - 1)
    v = lambda k: zf - k                            # rows from the front
    enclose(b, W - 1, zb - 1, x1 + 1, zf + 1, top, tone="trapezoid", band=False, sides="nse", columns=False)
    roof(b, x0, zb, x1, zf, top + 1, hatch=(x0, v(1)))
    b.b(x0, 0, zb, x1, 0, zf, EPOXY)

    def flight(xl, foot, y0, toward):
        """A flight up one lane (xl and xl + 1) from the foot row (the first column's row) at y0 (its foot's walking
        level), climbing toward north or south."""
        step = -1 if toward == "north" else 1
        left, right = (xl, xl + 1) if toward == "north" else (xl + 1, xl)
        for c in range(6):
            rep, cc = divmod(c, 3)
            z = foot + step * c
            parts = {0: [(0, 0)], 1: [(1, 0), (2, 1)], 2: [(3, 1)]}[cc]
            for part, lv in parts:
                for x, side in ((left, "left"), (right, "right")):
                    b.p(x, y0 + 2 * rep + lv, z, st("steel_stair", (70, 76, 84), facing=toward, part=part, side=side))

    for i, fl in enumerate(fls):
        Y = fl.y
        if i:
            for z in (v(0), v(1)):
                for x in range(x0, x1 + 1):
                    b.p(x, Y, z, st("stair_landing", (150, 154, 156), facing="north", edge=z == v(1)))
        if i + 1 < len(fls):
            flight(x0, v(2), Y + 1, "north")
            for z in (v(8), v(9)):
                for x in range(x0, x1 + 1):
                    b.p(x, Y + 4, z, st("stair_landing", (150, 154, 156), facing="south", edge=z == v(8)))
            flight(x1 - 1, v(7), Y + 5, "south")
            b.p(x1, Y + 7, v(9), st("emergency_light", (236, 236, 228), facing="west"))
            # A vapour-tight light under the half landing.
            b.p(x0 + 1, Y + 3, v(9), MP.furniture("vapor_tight_light", "east"))
        else:
            for y in range(Y + 1, top + 1):
                b.p(x0, y, v(1), st("steel_ladder", (150, 154, 156), facing="east", waterlogged=False))
        if fl.n == 1:
            b.door(x0 + 1, 1, zf + 1, "north", kind="steel", double=True, exit_=True)
        else:
            b.door(W - 1, Y + 1, zf, "east", kind="steel")
        b.p(x1, Y + 3, v(0), st("emergency_light", (236, 236, 228), facing="west"))
        # A vapour-tight light over the floor's landing (under the landing above, or the tower's roof).
        b.p(x0 + 2, Y + 7, v(0), MP.furniture("vapor_tight_light", "east"))
    # The glazed strip up the front, over the door's canopy to under the band.
    for x in (x0 + 1, x0 + 2):
        for y in range(4, top + 1):
            b.p(x, y, zf + 1, MP.FGLASS["north"])


# ------------------------------------------------------------------------------------------------ the generator
def warehouse(key, name, W, D, floors, front, lifts, racks_x, rows1, rows_up, docks1, docks_up, office, out=4, yard=True):
    """A warehouse building of any number of floors (each floor above the first the same).

    front: the pieces along the front wall [(kind, x)]: "in" (Goods In panel, its intake behind), "station", "out"
    (Goods Out panel and Ready Storage), "liquids" (part W4: the liquid line, six across from x - 1: the Drum Emptying
    Station with the Liquids Out panel behind it at x, the Drum Filling Station with the Liquids In panel at x + 3;
    "liquids_rev" the other way round, the filler at x). lifts: the lifts' middle columns along the north wall.
    racks_x: the racks' first and last x. rows1, rows_up: floor 1's and the upper floors' rack rows [(z, facing)]. docks1, docks_up: their
    docks on the west wall (z). office: (x0, x1) of the control office at the front. The stair tower stands beside
    the east wall."""
    OUT = out                                       # the ground out front (the yard, bollards, the sign)
    b = MP.Building(key, name, W + TOWER, D + OUT, floors * FH - 1, grounded=True)
    b.hall = (W, D)
    b.high_bay = True
    b.floors = floors
    top = floors * FH - 1
    zt = D - 2 - (TOWER_D - 1) - 1                  # the stair tower's north wall
    shell(b, W, D, top, zt)
    fls = [Floor(n) for n in range(1, floors + 1)]
    f1 = fls[0]
    zf = D - 2                                      # the machines' back row (against the front wall at D - 1)
    ox0, ox1 = office
    oz0 = zf - 5                                    # the office's north wall (its window over the floor)
    rx0, rx1 = racks_x
    girders = set(col_lines(W)[1:-1])
    soffit = lambda x, z: "girder_z" if x in girders else "deck_x"
    # ---- floor 1: lanes in the hall but the office; the racks, machines and lifts come out of it.
    f1.lanes = cells(1, 3, W - 2, zf - 2) - cells(ox0, oz0, ox1, zf)
    for kind, x in front:
        if kind == "in":
            piece(b, "warehouse_goods_in", x, 1, D - 1, "north")
            piece(b, "warehouse_intake", x, 1, zf - 1, "south")
        elif kind == "station":
            piece(b, "warehouse_station", x, 1, zf - 1, "south")
            f1.spots.add((x, zf - 1))
        elif kind == "out":
            piece(b, "warehouse_goods_out", x, 1, D - 1, "north")
            piece(b, "warehouse_ready_storage", x, 1, zf - 1, "south")
        elif kind in ("liquids", "liquids_rev"):
            ex, fx = (x, x + 3) if kind == "liquids" else (x + 3, x)
            piece(b, "warehouse_liquids_out", ex, 1, D - 1, "north")
            piece(b, "warehouse_drum_emptier", ex, 1, zf - 1, "south")
            piece(b, "warehouse_liquids_in", fx, 1, D - 1, "north")
            piece(b, "warehouse_drum_filler", fx, 1, zf - 1, "south")
    rack_rows(b, f1, rx0, rx1, rows1)
    for x in lifts:
        f1.lanes |= {(x, 3), (x, 4)}
        lift(b, x, 2, "north", fls)
    docks(b, f1, [(1, z) for z in docks1], "west")
    lay_floor(b, f1, 1, 1, W - 2, D - 2)
    # The office: block walls to the slab, framed glass over the floor, the terminal at the window (the floor under it
    # and under the window wall lane tiles: its link to the warehouse), its own ceiling, the sprinklers' valve station,
    # a desk.
    tx = (ox0 + ox1) // 2
    for x in range(ox0 + 1, ox1):
        for z in range(oz0 + 1, zf + 1):
            b.p(x, 0, z, MP.VINYL)
    for x in range(tx - 1, tx + 2):
        for z in (oz0, oz0 + 1):
            b.p(x, 0, z, st("warehouse_lane", (176, 180, 178), code=0, mask=15, soffit="none"))
    b.wall_x(oz0, ox0, ox1, 1, FH - 1)
    b.wall_z(ox0, oz0, zf, 1, FH - 1)
    b.wall_z(ox1, oz0, zf, 1, FH - 1)
    for x in range(ox0 + 1, ox1 - 1):
        for y in (2, 3):
            b.p(x, y, oz0, MP.FGLASS["north"])
    piece(b, "warehouse_terminal", tx, 1, oz0 + 1, "north")
    b.p(tx, 1, oz0 + 2, MP.furniture("operator_chair", "north"))
    b.door(ox1 - 1, 1, oz0, "north", kind="steel")
    b.b(ox0 + 1, 4, oz0 + 1, ox1 - 1, 4, zf, MP.CEIL)
    for x in range(ox0 + 2, ox1 - 1, 4):
        for z in (oz0 + 2, zf - 1):
            b.p(x, 3, z, MP.LIGHT)
    b.panel("csr_valve_station", ox0 + 1, 1, oz0 + 3, "west", 1, "WH")
    b.panel("csr_release_panel", ox0 + 1, 1, oz0 + 2, "west", 1, "WH")
    b.p(ox0 + 1, 1, oz0 + 1, MP.furniture("filing_cabinet", "west"))
    # Part W5: the switchboard in the office's front wall (its service entrance outside: the cable comes to its CABLE IN
    # box), the status board on the office's east wall (a block back from its door: not in the way of anyone coming in).
    piece(b, "warehouse_switchboard", ox0 + 3, 1, zf, "south")
    piece(b, "warehouse_status_board", ox1 - 1, 2, oz0 + 3, "east")
    for y in range(5, FH):
        b.p(ox0 + 1, y, oz0 + 3, MP.furniture("sprinkler_riser", "north"))
    MP.entrance(b, ox1 - 1, D - 1, "s", kind="steel", canopy=False, step=False, frame=False)
    # ---- the upper floors: racks, the walkway along the front from the stair tower's door and up the east wall to
    # the lifts' call stations, railed off from the drones; the slab's soffit (deck, girders) over the floor below.
    for fl in fls[1:]:
        fl.lanes = cells(1, 3, W - 4, zf - 3) | {(x, z) for x in lifts for z in (3, 4)}
        rack_rows(b, fl, rx0, rx1, rows_up)
        fl.walk = cells(rx0, zf - 1, W - 2, zf) | cells(W - 3, 3, W - 2, zf)
        for x in lifts:
            fl.walk |= cells(x + 2, 3, W - 2, 3) - fl.lanes
        shafts = {(x + dx, z) for x in lifts for dx in (-1, 0, 1) for z in (1, 2)}
        docks(b, fl, [(1, z) for z in docks_up], "west")
        lay_floor(b, fl, 1, 1, W - 2, D - 2, holes=shafts, soffit=soffit)
        for x in range(rx0, W - 3):
            if (x, zf - 2) not in fl.lanes and b.get(x, fl.y + 1, zf - 2) == 0:
                b.rail(x, fl.y + 1, zf - 2)
        for z in range(zf - 1, zf + 1):
            b.rail(rx0 - 1, fl.y + 1, z)
        for z in range(5, zf - 1):
            if b.get(W - 4, fl.y + 1, z) == 0:
                b.rail(W - 4, fl.y + 1, z)
        b.p(W - 3, fl.y + 1, 4, st("warehouse_crossing_beacon", (236, 184, 20), facing="west", lit=False))
    stair_tower(b, W, D, fls, top)
    # ---- fittings on every floor
    for fl in fls:
        z1 = zf - 2 if fl.n == 1 else zf - 3
        ceiling_fittings(b, fl, 2, W - 3, 3, z1, rx1 + 1)
        aisle_fittings(b, fl, rx0, rx1)
    # Floor 1's front, along the intakes and Ready Storage (outside the office): high-bay lights over it too.
    for x in range(2, W - 2, 6):
        if not ox0 <= x <= ox1 and b.get(x, FH - 1, zf - 1) == 0:
            b.p(x, FH - 1, zf - 1, MP.HIGH_BAY)
    # ---- outside: ribbon windows of framed glass (its glass to the outer face: a north wall's glass faces south) high
    # on each floor between the column lines (none into the stair
    # tower), the name under them; the yard out front with its bollards; canopies over the doors.
    cx, cz = set(col_lines(W)), set(col_lines(D))
    for fl in fls:
        for y in (fl.y + 5, fl.y + 6):
            for x in range(1, W - 1):
                if x not in cx:
                    b.p(x, y, 0, MP.FGLASS["south"])
                    if "warehouse" not in P.MATERIALS[b.get(x, y, D - 1)][0]:
                        b.p(x, y, D - 1, MP.FGLASS["north"])
            for z in range(1, D - 1):
                if z not in cz:
                    b.p(0, y, z, MP.FGLASS["east"])
                    if z < zt:
                        b.p(W - 1, y, z, MP.FGLASS["west"])
    if OUT == 0:
        return b
    b.p((ox0 + ox1) // 2 - 2, 3, D, MP.sign("warehouse", "south", big=True))
    if yard:
        b.b(0, 0, D, W + TOWER - 1, 0, D + OUT - 1, YARD)
        for kind, x in front:
            if kind in ("in", "out"):
                for bx in (x - 2, x + 2):
                    b.p(bx, 1, D + OUT - 1, st("warehouse_bollard", (236, 184, 20)))
            elif kind in ("liquids", "liquids_rev"):
                # The tanker's stand before the Liquids In panel's coupling, between the bollards.
                for bx in (x - 2, x + 5):
                    b.p(bx, 1, D + OUT - 1, st("warehouse_bollard", (236, 184, 20)))
    canopy(b, ox1 - 3, ox1, 3, D, "south")
    canopy(b, W, W + 3, 3, D, "south")
    return b


def small_warehouse(floors=2):
    """The Small Warehouse (32 x 20 and its stair tower): the minimum working set and room to grow. Front (south):
    Goods In (its intake and two palletizing stations behind it), the control office, Goods Out (Ready Storage behind
    it), the liquid line (part W4: the Drum Emptying Station and the Drum Filling Station behind the Liquids Out and
    Liquids In panels; the building six wider for it). Floor 1: racks A..D (two back-to-back pairs, 21 long, five
    high), docks for a stacker and two pallet drones, Pallet Lift 1 in the north-east corner. Each floor above: six
    rack rows, a dock for a pallet drone, the lift's landing, the walkway from the stair tower along the front and up
    the east wall to the lift."""
    return warehouse("small_warehouse", "Small Warehouse", 32, 20, floors,
                     front=[("in", 4), ("station", 7), ("station", 9), ("out", 22), ("liquids", 26)], lifts=[28],
                     racks_x=(5, 25),
                     rows1=[(6, "south"), (7, "north"), (10, "south"), (11, "north")],
                     rows_up=[(5, "south"), (6, "north"), (9, "south"), (10, "north"), (13, "south"), (14, "north")],
                     docks1=(9, 10, 11), docks_up=(9, 10), office=(10, 19))


def medium_warehouse(floors=2, key="medium_warehouse", name="Medium Warehouse", out=4, yard=True):
    """The Medium Warehouse (40 x 26 and its stair tower; the medium nuclear plant's warehouse is this one with two
    floors): Goods In with two stations, the office, Goods Out, the liquid line (part W4) east of Ready Storage; six
    rack rows 25 long on floor 1, eight on each floor above; one lift."""
    return warehouse(key, name, 40, 26, floors,
                     front=[("in", 4), ("station", 7), ("station", 9), ("out", 28), ("liquids", 34)], lifts=[33],
                     racks_x=(5, 29),
                     rows1=[(6, "south"), (7, "north"), (10, "south"), (11, "north"), (14, "south"), (15, "north")],
                     rows_up=[(5, "south"), (6, "north"), (9, "south"), (10, "north"), (13, "south"), (14, "north"),
                              (17, "south"), (18, "north")],
                     docks1=(9, 10, 11), docks_up=(9, 10), office=(12, 21), out=out, yard=yard)


def large_warehouse(floors=4):
    """The Large Warehouse (48 x 32 and its stair tower): the office, two Goods In lines (intakes and four stations),
    two Ready Storages, the liquid line (part W4) between the second Goods In line and Ready Storage, two Pallet Lifts;
    racks A..H on floor 1, twelve rows on each floor above."""
    return warehouse("large_warehouse", "Large Warehouse", 48, 32, floors,
                     front=[("in", 4), ("station", 7), ("station", 9), ("in", 22), ("station", 25), ("station", 27),
                            ("liquids_rev", 30), ("out", 36), ("out", 40)],
                     lifts=[13, 37], racks_x=(5, 42),
                     rows1=[(z, f) for k in range(4) for z, f in ((6 + 4 * k, "south"), (7 + 4 * k, "north"))],
                     rows_up=[(z, f) for k in range(6) for z, f in ((5 + 4 * k, "south"), (6 + 4 * k, "north"))],
                     docks1=(9, 10, 11, 12, 13), docks_up=(9, 10, 11), office=(11, 20))


def plant_warehouse():
    """The medium nuclear plant's warehouse: the Medium Warehouse with two floors (a part of the plant: its sign and the
    ground round it are the site's; the canopies over its doors are its own). Its floors above the first are the Medium
    Warehouse's, so its terminal adds floors to it the same way (BUILDING, ADD A FLOOR)."""
    b = medium_warehouse(2, key="warehouse", name="Warehouse", out=1, yard=False)
    finish(b)
    return b


# ------------------------------------------------------------------------------------------------ stacked blueprints
# The pre-made warehouses are stacked from three part blueprints each (Justin, 2026-10-17: pick the floors when you
# place it, and ADD A FLOOR at the terminal): the base (floor 1, the yard out front), a middle floor (as many as wanted)
# and the top (the last floor and the roof over it). blueprint/BlueprintStacks.java reads data/jugcraft/blueprint/
# stacks.json and builds "<kind>@<floors>" (and "<kind>@add": a middle floor and the top, staked on the old top floor)
# from them. Each part is a band of a real build, so a stack is exactly the building built with that many floors
# (check_stacks proves it).
STACKS = {"small_warehouse": ("Small Warehouse", small_warehouse, 2),
          "medium_warehouse": ("Medium Warehouse", medium_warehouse, 2),
          "large_warehouse": ("Large Warehouse", large_warehouse, 4)}
MIN_FLOORS = 2


def finish(b):
    """The finishing passes every building gets (railings joined, framed glass framed); no ceiling lights added (the
    warehouse has its own high-bay lights)."""
    MP.finish_railings(b)
    MP.finish_glass(b)
    b.light_steps = []
    return b


def _props(state):
    return dict(kv.split("=") for kv in state[state.index("[") + 1:-1].split(",")) if "[" in state else {}


def placed_whole(state):
    """False for a cell its block fills in itself when another is placed, so a blueprint leaves it out (it would cost
    an item a cell): a steel stair section is placed from its first tread's left half (exterior/SteelStairBlock.java),
    a warehouse piece from its origin (warehouse/PieceBlock.java onPlace)."""
    if state.startswith("jugcraft:steel_stair["):
        p = _props(state)
        return p.get("part") == "0" and p.get("side") == "left"
    name = state.split("[")[0].split(":")[-1]
    if name in PIECE_SIZE and "[" in state:
        w, h, d = PIECE_SIZE[name]
        return int(_props(state).get("part", -1)) == (d - 1) * w + w // 2
    return True


def grid_cells(b):
    """A building's blocks {(x, y, z): state} in its grid (y from the ground slab's layer)."""
    return P.part_cells(b)


def modules(kind):
    """(base, middle, top) of a stacked warehouse, {(x, y, z): state} each in its own band (y from 0), and the
    two-floor build they came from."""
    _, make, _ = STACKS[kind]
    b2, b3 = finish(make(2)), finish(make(3))
    c2, c3 = grid_cells(b2), grid_cells(b3)
    base = {k: v for k, v in c3.items() if k[1] < FH}
    mid = {(x, y - FH, z): v for (x, y, z), v in c3.items() if FH <= y < 2 * FH}
    top = {(x, y - FH, z): v for (x, y, z), v in c2.items() if y >= FH}
    return base, mid, top, b2


def stack(base, mid, top, floors):
    """The cells of a stack of floors (base, floors - 2 middles, the top)."""
    out = dict(base)
    for k in range(1, floors - 1):
        out.update({(x, y + k * FH, z): v for (x, y, z), v in mid.items()})
    out.update({(x, y + (floors - 1) * FH, z): v for (x, y, z), v in top.items()})
    return out


def check_stacks(kinds=None, up_to=4):
    """Proves a stack of the parts is the building built with that many floors (every block the same)."""
    for kind in kinds or STACKS:
        base, mid, top, _ = modules(kind)
        _, make, _ = STACKS[kind]
        for n in range(MIN_FLOORS, up_to + 1):
            want = grid_cells(finish(make(n)))
            got = stack(base, mid, top, n)
            diff = {k for k in set(want) | set(got) if want.get(k) != got.get(k)}
            assert not diff, f"{kind} x{n}: {len(diff)} cells differ, e.g. {sorted(diff)[:5]}"
    return True


def _module_blueprint(name, cells, size, kind="part"):
    w, h, d = size
    palette, layers = P.rle_layers({k: v for k, v in cells.items() if placed_whole(v)}, (w, h, d))
    return {"format": 2, "name": name, "author": "Jugcraft (built in)", "kind": kind,
            "description": f"One part of the pre-made {name.split(':')[0]} (stacked with the others by floors).",
            "size": [w, h, d], "anchor": [w // 2, 0, d + 1], "palette": palette, "layers": layers}


def write_stacks(out):
    """Writes each pre-made warehouse's base, middle and top part blueprints and stacks.json (what BlueprintStacks
    reads). Returns the part ids."""
    import json
    out.mkdir(parents=True, exist_ok=True)
    defs, ids = {}, []
    for kind, (name, make, default) in STACKS.items():
        base, mid, top, b2 = modules(kind)
        w, _, d = b2.size
        for part, cells, h in (("base", base, FH), ("mid", mid, FH), ("top", top, FH + 2)):
            pid = f"{kind}_{part}"
            data = _module_blueprint(f"{name}: {part}", cells, (w, h, d))
            (out / f"{pid}.jugbp.json").write_text(json.dumps(data, separators=(",", ":")) + "\n", encoding="utf-8")
            ids.append(pid)
        # The terminal's origin (where the BUILDING page measures the floors from) and the add blueprint's anchor: on
        # the new top floor's walkway (y FH + 1 over the old top floor's slab), over the old roof.
        term = next((x, y, z) for (x, y, z), v in base.items() if v.startswith("jugcraft:warehouse_terminal[") and placed_whole(v))
        Wi = w - TOWER
        add = next((x, FH + 1, z) for x in range(Wi - 2, 0, -1) for z in range(d - 3, 0, -1)
                   if "warehouse_walkway" in top.get((x, 0, z), "") and (x, 1, z) not in top and (x, 2, z) not in top)
        defs[kind] = {"name": name, "base": f"{kind}_base", "middle": f"{kind}_mid", "top": f"{kind}_top", "storey": FH,
                      "min": MIN_FLOORS, "default": default, "size": [w, d], "hall": list(b2.hall), "anchor": [w // 2, b2.sink, d + 1],
                      "terminal": list(term), "add_anchor": list(add)}
    (out / "stacks.json").write_text(json.dumps(defs, indent=1) + "\n", encoding="utf-8")
    return ids
