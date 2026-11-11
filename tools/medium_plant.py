"""The Medium Nuclear Plant (4-loop PWR), designed like a real station, building by building.

Every building is drawn at real size (one block = one metre) with what a real one holds and how people get round it:
floors at real heights, stairwells, corridors, doors, offices and plant rooms, lit inside. Outside the buildings keep
to plain Minecraft materials (concrete, stone bricks, andesite, smooth stone, glass); inside they turn realistic, with
the plant kit (tools/plant_interior.py): suspended ceilings, vinyl and epoxy floors, painted block walls, steel and
glass doors that open by hand, server racks, relay cabinets, pallet racking, office furniture, safety railing, ducts,
signs on the buildings and by the doors. Decor equipment (cranes, heaters, tanks, the diesel generators...) is drawn
by the client like the reactor equipment and placed whole.

tools/medium_check.py walks the finished site and fails on what a visitor would notice: furniture in walls or
windows, blocked doors, floating blocks, lights not under a ceiling, stairs you can't climb, dark rooms.

Site coordinates as in tools/nuclear_plants.py: x east, z south (the lake is north, z < 0), y up from the ground.
"""
import math

import numpy as np

import nuclear_plants as P
from nuclear_plants import Part, M

# ------------------------------------------------------------------------------------------------- the palette
# Outside: plain Minecraft.
CONCRETE = M("minecraft:light_gray_concrete", (176, 176, 170))
CONCRETE_DARK = M("minecraft:gray_concrete", (104, 107, 110))
WHITE = M("minecraft:white_concrete", (222, 222, 216))
SMOOTH = M("minecraft:smooth_stone", (158, 158, 156))
SMOOTH_SLAB = M("minecraft:smooth_stone_slab[type=bottom]", (158, 158, 156))
BRICK = M("minecraft:stone_bricks", (122, 122, 120))
ANDESITE = M("minecraft:polished_andesite", (132, 135, 132))
TILES = M("minecraft:deepslate_tiles", (60, 60, 64))
SHIELD = M("jugcraft:shielding_concrete", (186, 182, 172))
LINER = M("jugcraft:steel_armor_plate", (170, 174, 178))
CLADDING = M("jugcraft:aluminum_cladding", (190, 196, 200))
GLASS = M("minecraft:glass", (180, 210, 220))
PANE = M("minecraft:glass_pane", (180, 210, 220))
FROSTED = M("minecraft:white_stained_glass_pane", (232, 236, 238))     # the restroom's windows (round 26, #14)
RR_PAPER = 8                                                           # a toilet's rolls as built (tools/restroom.py)
BLAST = M("jugcraft:blast_glass", (150, 180, 196))
BARS = M("minecraft:iron_bars", (92, 94, 98))
CHAIN = M("minecraft:iron_chain[axis=y]", (70, 70, 74))
# What lights hang from: a chain with the power cable run down it.
HANGER = M("jugcraft:light_hanger[facing=north]", (70, 70, 74))
# A high-bay light, hung close under a high roof (when placed it sets the game's invisible light block lower down, to
# light the floor as brightly as a real one does: block light only reaches 15 blocks).
HIGH_BAY = M("jugcraft:high_bay_light[facing=north]", (236, 236, 228))
GIRDER = M("jugcraft:steel_girder", (120, 124, 128))
FRAME = M("jugcraft:tungsten_steel_frame", (110, 114, 120))
COLUMN = M("jugcraft:concrete_column", (170, 170, 166))
ASPHALT = M("jugcraft:asphalt", (56, 58, 60))
ROAD_LINE = M("jugcraft:asphalt_road_line", (200, 200, 200))
LANTERN = M("minecraft:sea_lantern", (210, 230, 226))
def beacon(colour, facing):
    """A warning beacon (nuclear_storage.beacon_model) on the wall behind it: facing is the way it looks out."""
    return M(f"jugcraft:warning_beacon[colour={colour},facing={facing}]", {"amber": (250, 170, 40), "red": (230, 50, 40), "green": (70, 210, 90)}[colour])
PIPE = M("jugcraft:steel_fluid_pipe", (120, 128, 136))
WATER = M("minecraft:water", (52, 92, 158))
RAIL_NS = M("minecraft:rail[shape=north_south]", (110, 100, 90))
LADDER = {f: M(f"minecraft:ladder[facing={f}]", (150, 120, 80)) for f in ("north", "south", "east", "west")}
GRATE = M("minecraft:iron_trapdoor[half=top,facing=north]", (160, 162, 166))
# Inside: the plant kit.
ACCESS_FLOOR = M("jugcraft:access_floor_tile", (150, 154, 158))
ACOUSTIC = M("jugcraft:acoustic_wall_panel", (200, 200, 196))
CARPET = M("jugcraft:carpet_tile", (90, 96, 110))
VINYL = M("jugcraft:vinyl_floor", (198, 200, 196))
EPOXY = M("jugcraft:epoxy_floor", (176, 180, 178))
CEIL = M("jugcraft:ceiling_tile", (232, 232, 226))
PAINTED = M("jugcraft:painted_wall", (226, 224, 214))
TANK = M("jugcraft:tank_plate", (226, 228, 224))
# The storage tank MBS (tools/storage_tank.py): grey primer plates until the outlet forms the tank.
TANK_PLATE = M("jugcraft:storage_tank_plate[band=wall,formed=false]", (150, 156, 150))
# Framed glass (tools/framed_glass.py): laid facing the way a door in it would face, its frame worked out at the end
# (finish_glass) from what is round it, like the game does.
FGLASS = {f: M(f"jugcraft:framed_glass[facing={f}]", (186, 212, 218)) for f in ("north", "south", "east", "west")}
# Railing is laid as this placeholder and joined up at the end (finish_railings), like the game joins it.
RAIL = M("jugcraft:railing", (232, 190, 30))
# A speed gate's lane (shut) and the pedestals either side of it, along z (tools/speed_gate.py).
SPEED_GATE = {f: M(f"jugcraft:speed_gate[facing={f},open=false,part=lane]", (90, 96, 104)) for f in ("north", "south")}
GATE_PEDESTAL = {h: M(f"jugcraft:speed_gate_pedestal[axis=z,half={h}]", (176, 180, 184)) for h in ("a", "b")}
LOUVRE = M("jugcraft:intake_louvre", (150, 154, 152))
# A steel fluid pipe run inside a wall (wallrun/WallRuns.java): it takes the wall's look when it is built.
WALL_PIPE = M("jugcraft:wall_steel_fluid_pipe[down=false,east=false,north=false,south=false,up=false,west=false]", (120, 128, 136))
# Power cable (round 25): buried in the ground as it is, and run in a floor slab (wallrun/WallRuns.java: it takes the
# slab's look when it is built).
CABLE = M("jugcraft:aluminum_cable[down=false,east=false,north=false,south=false,up=false,west=false]", (150, 152, 158))
WALL_CABLE = M("jugcraft:wall_aluminum_cable[down=false,east=false,north=false,south=false,up=false,west=false]", (150, 152, 158))
# A drip pan's lip: a steel plate a few pixels high.
LIP = M("minecraft:iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]", (160, 162, 166))
LIGHT = M("jugcraft:ceiling_light_panel", (240, 240, 230))
BATTERY = M("jugcraft:battery_box", (80, 84, 90))
HAZARD = M("jugcraft:hazard_plating", (210, 170, 40))
# A black rubber mat on the floor in front of a panel.
MAT = M("minecraft:black_carpet", (30, 30, 32))

# Blocks a ceiling light may hang under (the undersides of slabs, ceilings and roofs).
CEILINGS = {CEIL, CONCRETE, CONCRETE_DARK, SMOOTH, SHIELD, VINYL, EPOXY, ACCESS_FLOOR, CARPET, TILES, LINER, CLADDING, GRATE, FRAME}


def stair(material, facing, half="bottom"):
    return M(f"minecraft:{material}_stairs[facing={facing},half={half},shape=straight]", (130, 130, 130))


def door_state(kind, facing, half, hinge="left", paired=False, exit_=False, reader=False):
    """A plant door (doors round: nuclear/PlantDoorBlock.java): paired a double door's leaf, exit its panic bar,
    reader the badge reader beside it."""
    m = M(f"jugcraft:{kind}_door[exit={_flag(exit_)},facing={facing},half={half},hinge={hinge},moving=false,open=false,"
          f"paired={_flag(paired)},powered=false,reader={_flag(reader)}]",
          (200, 200, 200) if kind == "steel" else (150, 190, 200))
    DOORISH.add(m)
    return m


# The vital rooms: their doors have badge readers (doors round; Justin: the reader lets the plant's owner and their
# party in).
SECURED = {"main_control_room", "server_room", "switchgear_room", "relay_room", "cable_spreading_room", "remote_shutdown",
           "battery_room_1a", "battery_room_1b", "dc_equipment_1a", "dc_equipment_1b", "support_center"}


def roller_door(b, cells, y0, y1, facing):
    """A roller door over an opening's cells (x, z) in a wall, y0..y1 high, facing the way in (its coil inside): each
    block's place in the door (nuclear/RollerDoorBlock.java POS, LEFT, RIGHT), and its push-button station on the
    inside of the wall beside the opening's right end, a block up (doors round)."""
    cells = set(cells)
    lx, lz = P.DIRS[CCW[facing]]
    rx, rz = P.DIRS[P.CLOCKWISE[facing]]
    for x, z in sorted(cells):
        left, right = (x + lx, z + lz) not in cells, (x + rx, z + rz) not in cells
        for y in range(y0, y1 + 1):
            pos = "single" if y0 == y1 else "bottom" if y == y0 else "top" if y == y1 else "middle"
            b.p(x, y, z, M(f"jugcraft:roller_door[facing={facing},left={_flag(left)},moving=false,open=false,pos={pos},"
                           f"right={_flag(right)}]", (150, 156, 160)))
    ex, ez = next(c for c in sorted(cells) if (c[0] + rx, c[1] + rz) not in cells)
    fx, fz = P.DIRS[facing]
    sx, sz = ex + rx + fx, ez + rz + fz
    if b.get(sx, y0 + 1, sz) == 0:
        b.p(sx, y0 + 1, sz, M(f"jugcraft:roller_door_station[facing={OPPOSITE[facing]}]", (128, 132, 134)))


def row_state(name, facing, left, right, variant=None):
    v = "" if variant is None else f",variant={variant}"
    return M(f"jugcraft:{name}[facing={facing},left={'true' if left else 'false'},right={'true' if right else 'false'}{v}]", (120, 100, 80))


def multi_state(name, facing, part, variant=None):
    extra = "" if variant is None else f",variant={variant}"
    import plant_panels
    if plant_panels.PANELS.get(name, {}).get("swing"):
        # Its door shut (older rooms round 2: a rack's front door, nuclear/ServerRoomPanelBlock OPEN; the admin building
        # part 2: the chemistry lab's safety cabinets).
        extra = ",open=false" + extra
    return M(f"jugcraft:{name}[facing={facing},part={part}{extra}]", (60, 60, 64))


def variant_state(name, facing, v):
    return M(f"jugcraft:{name}[facing={facing},variant={v}]", (120, 140, 110))


def table_state(part):
    return M(f"jugcraft:conference_table[part={part}]", (96, 64, 42))


def furniture(name, facing, up=None, down=None):
    extra = "" if up is None else f",up={'true' if up else 'false'},down={'true' if down else 'false'}"
    return M(f"jugcraft:{name}[facing={facing}{extra}]", (70, 74, 80))


def duct(axis):
    return M(f"jugcraft:hvac_duct[axis={axis}]", (176, 180, 182))


def sign(label, facing, big=False):
    return M(f"jugcraft:{'building' if big else 'room'}_sign[facing={facing},label={label}]", (44, 48, 54))


def tank_port(role, facing, service="none", rating="general"):
    extra = f",rating={rating},service={service}" if role == "outlet" else ""
    return M(f"jugcraft:storage_tank_{role}[facing={facing},formed=false,open=true,powered=false{extra}]", (70, 74, 80))


def tank_piece(name, facing, **props):
    extra = "".join(f",{k}={v}" for k, v in sorted(props.items()))
    return M(f"jugcraft:{name}[facing={facing}{extra}]", (150, 156, 160))


def steel_ladder(facing, cage, top, bottom=False):
    """The caged steel ladder (nuclear/SteelLadderBlock): ``bottom`` the cage's flared lowest length."""
    return M(f"jugcraft:caged_ladder[bottom={'true' if bottom else 'false'},cage={'true' if cage else 'false'},facing={facing},"
             f"top={'true' if top else 'false'},waterlogged=false]", (170, 176, 178))


def _flag(v):
    return "true" if v else "false"


def finish_railings(part):
    """Joins every railing in a part the way the game does (nuclear/RailingBlock.java): an arm to each railing or gate
    beside it (a gate along its line), a post at corners, ends and junctions and every other block of a straight run."""
    g = part.g
    rails = {k for k, (st, _) in P.MATERIALS.items() if st == "jugcraft:railing" or st.startswith("jugcraft:railing[")}
    gates = {k: st for k, (st, _) in P.MATERIALS.items() if st.startswith("jugcraft:railing_gate[")}
    w, h, d = g.shape
    # Joined against the railing as laid (a converted neighbour's new state is not in rails yet).
    laid = g.copy()

    def joins(x, y, z, axis):
        if not (0 <= x < w and 0 <= z < d):
            return False
        m = int(laid[x, y, z])
        if m in rails:
            return True
        if m in gates:
            facing = gates[m].split("facing=")[1].split(",")[0].rstrip("]")
            return (facing in ("north", "south")) != (axis == "z")
        return False
    cells = [tuple(c) for c in np.argwhere(np.isin(g, list(rails)))]
    for x, y, z in cells:
        n, s_, e, wst = joins(x, y, z - 1, "z"), joins(x, y, z + 1, "z"), joins(x + 1, y, z, "x"), joins(x - 1, y, z, "x")
        straight = n and s_ and not e and not wst or e and wst and not n and not s_
        post = not straight or (x + z) % 2 == 0
        g[x, y, z] = M(f"jugcraft:railing[east={_flag(e)},north={_flag(n)},post={_flag(post)},south={_flag(s_)},waterlogged=false,"
                       f"west={_flag(wst)}]", (232, 190, 30))


def finish_glass(part):
    """Frames every framed glass in a part the way the game does (nuclear/FramedGlassBlock.java): the glass runs on
    into the framed glass beside, above and below it, with a jamb, a corner post, a sill, a head or a door's frame
    wherever it needs support."""
    import framed_glass as FG
    g = part.g
    laid = g.copy()
    w, h, d = g.shape
    placed = {v: f for f, v in FGLASS.items()}
    doors = {k: st.split("facing=")[1].split(",")[0].rstrip("]") for k, (st, _) in P.MATERIALS.items()
             if "_door[" in st and "half=" in st}

    def base(x, y, z):
        if not (0 <= x < w and 0 <= y < h and 0 <= z < d):
            return None
        m = int(laid[x, y, z])
        if m in placed:
            return ("glass", placed[m], None)
        if m in doors:
            return ("door", doors[m])
        return None

    def at(x, y, z):
        here = base(x, y, z)
        if here is not None and here[0] == "glass":
            return ("glass", here[1], FG.bottom_of(base, x, y, z))
        return here
    for x, y, z in [tuple(c) for c in np.argwhere(np.isin(g, list(placed)))]:
        g[x, y, z] = M(FG.state(at, x, y, z), (186, 212, 218))


def roller(facing):
    return M(f"jugcraft:roller_door[facing={facing},open=false]", (150, 156, 160))


CCW = {v: k for k, v in P.CLOCKWISE.items()}
GLAZING = {PANE, GLASS, BLAST} | set(FGLASS.values())
DOORISH = set()  # door block ids, filled as doors are made
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
SIDE = {"n": "north", "s": "south", "w": "west", "e": "east"}
STACKING = {"relay_cabinet", "locker", "pallet_rack", "pallet_rack_drums"}


# ------------------------------------------------------------------------------------------------- building kit

class Building(Part):
    """A building with floors, rooms and fittings. Coordinates are the part's own (x across, z front-to-back toward
    +z, y up from the ground floor slab at 0). The parts' notes (doors, furniture, signs, lights) are kept for
    tools/medium_check.py."""

    def __init__(self, key, name, w, d, h, sink=0, grounded=False):
        """grounded: the ground floor slab is laid in the ground (level with the grass round it), so you walk
        straight in at the doors; otherwise it sits on the ground, one block up."""
        super().__init__(key, name, w, h + 3 + sink, d)
        self.sink = sink + (1 if grounded else 0)
        self.base = sink  # y of the ground floor slab in the grid
        self.grounded = grounded
        self.h = h
        self.doors = []  # (x, y, z, facing) lower halves
        self.seats = []  # (x, y, z, facing): furniture, which must stand inside with room in front
        self.stairs = []  # stairwell footprints, for the checker
        self.high_bay = False  # tall open spaces lit by high-bay lights under the roof instead of lights on chains
        # Rooms whose ceiling lights are another fixture (older rooms round 4: the battery rooms' vapor-tight lights):
        # (x0, y0, z0, x1, y1, z1, material), building coordinates.
        self.light_swap = []

    def site_y(self, y):
        """The site y of this building's y (from its ground floor slab): one lower when the building is grounded."""
        return y + self.base - self.sink

    # Every method below takes y from the ground floor slab (the grid's y is y + base).
    def b(self, x0, y0, z0, x1, y1, z1, m):
        self.box(x0, y0 + self.base, z0, x1, y1 + self.base, z1, m)

    def c(self, x0, y0, z0, x1, y1, z1):
        self.clear(x0, y0 + self.base, z0, x1, y1 + self.base, z1)

    def p(self, x, y, z, m):
        self.put(x, y + self.base, z, m)

    def get(self, x, y, z):
        w, h, d = self.g.shape
        y += self.base
        return int(self.g[x, y, z]) if 0 <= x < w and 0 <= y < h and 0 <= z < d else 0

    # -- the shell
    def shell_walls(self, wall=CONCRETE, base=BRICK, base_h=2, pilaster=ANDESITE, roof=CONCRETE_DARK, parapet=SMOOTH, step=8):
        """Outer walls (stone-brick plinth, wall, pilasters every step), the ground slab and a flat roof with parapet
        (the roof slab at h + 1)."""
        w, _, d = self.size
        h = self.h
        self.b(0, 0, 0, w - 1, 0, d - 1, SMOOTH)
        for y in range(1, h + 1):
            m = base if y <= base_h else wall
            self.b(0, y, 0, w - 1, y, 0, m)
            self.b(0, y, d - 1, w - 1, y, d - 1, m)
            self.b(0, y, 0, 0, y, d - 1, m)
            self.b(w - 1, y, 0, w - 1, y, d - 1, m)
        for x in list(range(0, w, step)) + [w - 1]:
            self.b(x, 1, 0, x, h, 0, pilaster)
            self.b(x, 1, d - 1, x, h, d - 1, pilaster)
        for z in list(range(0, d, step)) + [d - 1]:
            self.b(0, 1, z, 0, h, z, pilaster)
            self.b(w - 1, 1, z, w - 1, h, z, pilaster)
        self.b(0, h + 1, 0, w - 1, h + 1, d - 1, roof)
        for x in range(w):
            self.p(x, h + 2, 0, parapet)
            self.p(x, h + 2, d - 1, parapet)
        for z in range(d):
            self.p(0, h + 2, z, parapet)
            self.p(w - 1, h + 2, z, parapet)

    def windows(self, y0, y1, sides="nsew", step=8, glass=PANE, skip=()):
        """Window strips between the pilasters on the chosen sides (n = z 0, s = z max, w = x 0, e = x max)."""
        w, _, d = self.size
        for x in range(1, w - 1):
            if x % step == 0 or x in skip:
                continue
            for y in range(y0, y1 + 1):
                if "n" in sides:
                    self.p(x, y, 0, glass)
                if "s" in sides:
                    self.p(x, y, d - 1, glass)
        for z in range(1, d - 1):
            if z % step == 0 or z in skip:
                continue
            for y in range(y0, y1 + 1):
                if "w" in sides:
                    self.p(0, y, z, glass)
                if "e" in sides:
                    self.p(w - 1, y, z, glass)

    def storey(self, y, x0, z0, x1, z1, floor=VINYL, ceiling_at=None, ceiling=CEIL):
        """A floor slab at y (walked on at y + 1) and, for the storey below, a suspended ceiling one block under it
        (ceiling_at overrides where)."""
        self.b(x0, y, z0, x1, y, z1, floor)
        if ceiling_at is not None and ceiling is not None:
            self.b(x0, ceiling_at, z0, x1, ceiling_at, z1, ceiling)

    def wall_x(self, z, x0, x1, y0, y1, m=PAINTED):
        """An interior wall along x at z."""
        self.b(x0, y0, z, x1, y1, z, m)

    def wall_z(self, x, z0, z1, y0, y1, m=PAINTED):
        self.b(x, y0, z0, x, y1, z1, m)

    # -- doors and signs
    def door(self, x, y, z, facing, kind="steel", double=False, label=None, sign_side=None, exit_=None, reader=None):
        """A door (steel, or glass for offices and entrances) in a wall cell, opening by hand. The door leaf sits
        on the side of its block away from facing: give facing as the way you walk in through it to have the leaf
        flush with the face you walk in from. double: a pair, the second to the facing's left. label: a room sign
        beside the door on the side you come from. exit_: panic bars (a pair's, by default); reader: a badge reader
        (a vital room's, SECURED, by default) (doors round)."""
        dx, dz = P.DIRS[facing]
        lx, lz = P.DIRS[CCW[facing]]
        exit_ = double if exit_ is None else exit_
        reader = (label in SECURED) if reader is None else reader
        leaves = [(x, z, "right" if double else "left")]
        if double:
            leaves.append((x + lx, z + lz, "left"))
        for (cx, cz, hinge) in leaves:
            self.p(cx, y, cz, door_state(kind, facing, "lower", hinge, double, exit_, reader))
            self.p(cx, y + 1, cz, door_state(kind, facing, "upper", hinge, double, exit_, reader))
            self.doors.append((cx, y, cz, facing))
        if label:
            # Beside the door, at eye height, on the wall you approach from: on the right if that is plain wall
            # (not glass, not another door), else on the left.
            rx, rz = P.DIRS[P.CLOCKWISE[facing]]
            width = 2 if double else 1
            for side, off in ((1, 1), (-1, width)):
                wx, wz = x + rx * side * off, z + rz * side * off
                if side < 0:
                    wx, wz = x - rx * width, z - rz * width
                sx, sz = wx - dx, wz - dz
                behind = self.get(wx, y + 1, wz)
                if self.get(sx, y + 1, sz) == 0 and behind and behind not in GLAZING and behind not in DOORISH:
                    self.p(sx, y + 1, sz, sign(label, OPPOSITE[facing]))
                    break

    def building_sign(self, x, y, z, side, label):
        """The building's name on an outer wall (side n/s/e/w), hung in front of it at height y."""
        facing = SIDE[side]
        self.p(x, y, z, sign(label, facing, big=True))

    # -- fittings
    def furniture(self, name, x, y, z, facing):
        """A piece of furniture; facing is the way its user looks (its front faces the other way)."""
        self.p(x, y, z, furniture(name, facing))
        self.seats.append((name, x, y, z, facing))

    def stack(self, name, x, y, z, facing, n=2):
        """A stacking piece (server rack, relay cabinet, locker, pallet racking) n high, joined up."""
        for k in range(n):
            self.p(x, y + k, z, furniture(name, facing, up=k < n - 1, down=k > 0))
        self.seats.append((name, x, y, z, facing))

    def panel(self, name, x, y, z, facing, width, label=None):
        """A floor-standing panel two blocks high and width wide (plant_panels.py), its left block (as you face it) at
        x, z; label: which of its train labels it wears (round 26, 8c: plant_panels PANELS "labels")."""
        import plant_panels
        labels = plant_panels.PANELS.get(name, {}).get("labels")
        variant = labels.index(label) if labels and label else None
        rx, rz = P.DIRS[P.CLOCKWISE[facing]]
        rows = plant_panels.PANELS.get(name, {}).get("rows", 2)      # (round 26, #12: a console is one row)
        for c in range(width):
            for row in range(rows):
                self.p(x + rx * c, y + row, z + rz * c, multi_state(name, facing, row * width + c, variant or None))
            self.seats.append((name, x + rx * c, y, z + rz * c, facing))

    def desk(self, x, y, z, facing, name="office_desk"):
        """A desk with its chair on the user's side."""
        self.furniture(name, x, y, z, facing)
        dx, dz = P.DIRS[OPPOSITE[facing]]
        self.p(x + dx, y, z + dz, furniture("operator_chair", facing))

    def row(self, names, x, y, z, facing, seat=None, tops=None):
        """Pieces that join side by side (counters, desks, benches, sofas, lab benches), laid from x, z to the user's
        right; names is a list (one per block) or a name and a count. seat: chairs on the user's side. tops: which
        variant (what stands on it) each piece is, for the pieces that have them."""
        if isinstance(names, tuple):
            names = [names[0]] * names[1]
        rx, rz = P.DIRS[P.CLOCKWISE[facing]]
        n = len(names)
        for i, name in enumerate(names):
            cx, cz = x + rx * i, z + rz * i
            self.p(cx, y, cz, row_state(name, facing, i > 0, i < n - 1, None if tops is None else tops[i]))
            self.seats.append((name, cx, y, cz, facing))
            if seat:
                dx, dz = P.DIRS[OPPOSITE[facing]]
                self.p(cx + dx, y, cz + dz, furniture(seat, facing))

    def varied(self, name, x, y, z, facing, n):
        """A piece that comes in variants (whiteboards, prints, plants): which one, from where it stands."""
        v = (x * 7 + z * 13 + y * 5 + len(self.key)) % n
        self.p(x, y, z, variant_state(name, facing, v))
        self.seats.append((name, x, y, z, facing))

    def plant(self, x, y, z):
        self.varied("office_plant", x, y, z, "north", 8)

    def round_table(self, x, y, z, chairs=True):
        """The round meeting table, its middle at x, z, with a chair at each of the twelve places round it."""
        for part in range(9):
            self.p(x + part % 3 - 1, y, z + part // 3 - 1, table_state(part))
        if chairs:
            for k in (-1, 0, 1):
                self.p(x + k, y, z - 2, furniture("operator_chair", "south"))
                self.p(x + k, y, z + 2, furniture("operator_chair", "north"))
                self.p(x - 2, y, z + k, furniture("operator_chair", "east"))
                self.p(x + 2, y, z + k, furniture("operator_chair", "west"))

    def prop(self, kind, x0, y, z0, facing="north", variant=None):
        super().prop(kind, x0, y + self.base, z0, facing, variant)

    def rail(self, x, y, z, facing=None):
        """A length of yellow safety railing standing at y (it joins the railing beside it like a fence; facing is
        where the drop is, kept for reading the layouts)."""
        self.p(x, y, z, RAIL)

    def rail_round(self, x0, z0, x1, z1, y):
        """Railing round an opening x0..x1, z0..z1 in a floor whose top is walked at y (the rails stand just outside,
        corners included, so it runs round unbroken)."""
        for x in range(x0 - 1, x1 + 2):
            for z in (z0 - 1, z1 + 1):
                if self.get(x, y, z) == 0 and self.get(x, y - 1, z):
                    self.rail(x, y, z)
        for z in range(z0, z1 + 1):
            for x in (x0 - 1, x1 + 1):
                if self.get(x, y, z) == 0 and self.get(x, y - 1, z):
                    self.rail(x, y, z)

    def duct_up(self, x0, z0, x1, z1, y0):
        """A rectangular duct riser from y0 up to the ceiling above (stops under the first solid block)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                y = y0
                while self.get(x, y, z) == 0 and y < self.h + 2:
                    self.p(x, y, z, duct("y"))
                    y += 1

    def tray_run(self, x0, z0, x1, z1, y, facing="east"):
        """A continuous cable tray hung under the ceiling at y (a straight run)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if self.get(x, y, z) == 0:
                    self.p(x, y, z, furniture("cable_tray", facing))

    # -- stairs
    def stairwell(self, x0, z0, levels, wall=CONCRETE, material="stone_brick", door_side="s", kind="steel"):
        """An enclosed switchback stair (6 wide, two flights a storey with a half landing) between floor slabs at the
        levels given, its doors on door_side (s: the +z wall, n: the -z wall). Footprint 8 x (inner + 2)."""
        rises = [hi - lo for lo, hi in zip(levels, levels[1:])]
        a_max = max((r + 1) // 2 for r in rises)
        inner = max(10, a_max + 7)
        w, d = 8, inner + 2
        top = levels[-1] + 4
        self.b(x0, levels[0] + 1, z0, x0 + w - 1, top, z0 + d - 1, wall)
        self.c(x0 + 1, levels[0] + 1, z0 + 1, x0 + w - 2, top - 1, z0 + d - 2)
        # Rows counted from the door wall: r = 1 is next to it. Map to z.
        if door_side == "s":
            Z = lambda r: z0 + d - 1 - r
            up, down = "north", "south"
        else:
            Z = lambda r: z0 + r
            up, down = "south", "north"
        west = (x0 + 1, x0 + 2)
        east = (x0 + 5, x0 + 6)
        for lo, hi in zip(levels, levels[1:]):
            r_ = hi - lo
            a = (r_ + 1) // 2
            b2 = r_ - a
            # Flight A up the west side away from the door, from row 5 (rows 1..4 are the floor landing), each step
            # on a stringer block so the flight is one piece.
            for i in range(1, a + 1):
                for x in west:
                    self.p(x, lo + i, Z(4 + i), stair(material, up))
                    if i > 1:
                        self.p(x, lo + i - 1, Z(4 + i), SMOOTH)
            # Half landing at lo + a beyond flight A, full width, to the far wall.
            for r in range(5 + a, inner + 1):
                self.b(x0 + 1, lo + a, Z(r), x0 + 6, lo + a, Z(r), SMOOTH)
            # Flight B back down the east side toward the door, climbing to hi, on its stringer.
            for j in range(1, b2 + 1):
                for x in east:
                    self.p(x, lo + a + j, Z(5 + a - j), stair(material, down))
                    self.p(x, lo + a + j - 1, Z(5 + a - j), SMOOTH)
        for lv in levels:
            for r in range(1, 6):
                for x in range(x0 + 1, x0 + 7):
                    if self.get(x, lv, Z(r)) == 0 or lv == levels[0]:
                        self.p(x, lv, Z(r), SMOOTH)
            self.door(x0 + 3, lv + 1, Z(0), up, kind=kind, double=True, label="stairs")
        self.stairs.append((x0, z0, w, d, tuple(levels)))
        return w, d

    # -- lights
    def light_ceilings(self, step=4, hang=4, x0=1, z0=1, x1=None, z1=None, y_max=None, y_min=None, blocked=frozenset()):
        """Ceiling lights flush under every ceiling (slab, suspended ceiling, roof) over open floor, on a grid; in
        tall spaces (more than hang + 1 over the floor) high-bay lights hung close under the roof instead. blocked: grid cells a light or
        its chain must not go through (the plant's equipment and pipes)."""
        w, H, d = self.size
        x1 = w - 2 if x1 is None else x1
        z1 = d - 2 if z1 is None else z1

        def column(x, z, gy):
            """Where a light under the ceiling over gy (air) at x, z would go, or None."""
            if not (0 <= x < w and 0 <= z < d) or self.g[x, gy, z] != 0 or int(self.g[x, gy + 1, z]) not in CEILINGS:
                return None
            k = 0
            while gy - k - 1 >= 0 and self.g[x, gy - k - 1, z] == 0:
                k += 1
            if k < 2:
                return None
            drop = k - hang if k > hang + 1 else 0
            if any((x, gy - j, z) in blocked for j in range(drop + 1)):
                return None
            return drop

        for x in range(x0, x1 + 1):
            if (x - x0) % step != 1:
                continue
            for z in range(z0, z1 + 1):
                if (z - z0) % step != 1:
                    continue
                for gy in range(H - 2, 1, -1):
                    if y_max is not None and gy > y_max + self.base:
                        continue
                    if y_min is not None and gy < y_min + self.base:
                        continue
                    if self.g[x, gy, z] != 0 or int(self.g[x, gy + 1, z]) not in CEILINGS:
                        continue
                    # A light here, or next to here if equipment is in the way.
                    for ox, oz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2)):
                        drop = column(x + ox, z + oz, gy)
                        if drop is None:
                            continue
                        if drop > 0:
                            # A high space: a high-bay light close under the roof, on a block of chain at most where
                            # the room is very tall; it lights the floor itself (nuclear/HighBayLightBlock.java).
                            hang_by = 1 if drop >= 4 and not self.high_bay else 0
                            for j in range(hang_by):
                                self.g[x + ox, gy - j, z + oz] = HANGER
                            self.g[x + ox, gy - hang_by, z + oz] = HIGH_BAY
                            break
                        self.g[x + ox, gy, z + oz] = LIGHT
                        break
        for sx0, sy0, sz0, sx1, sy1, sz1, m in self.light_swap:
            for x in range(sx0, sx1 + 1):
                for z in range(sz0, sz1 + 1):
                    for gy in range(sy0 + self.base, sy1 + self.base + 1):
                        if self.g[x, gy, z] == LIGHT:
                            self.g[x, gy, z] = m


def entrance(b, x, z, side, kind="glass", label=None, canopy=True, step=True, frame=True):
    """A double front door in an outer wall (side n/s/e/w; x, z the first leaf), flush with the outside, with a slab
    canopy over it, a step up from the ground and a room sign inside if label. frame: a stone surround (a column
    each side, the full height of the wall, and a lintel over the doors) so the window glass either side ends
    cleanly against stone rather than against the door."""
    out = SIDE[side]
    facing = OPPOSITE[out]  # you walk in
    rx, rz = P.DIRS[P.CLOCKWISE[facing]]  # the second leaf is to the facing's left: -r
    if frame:
        for k in (1, -2):
            b.b(x + rx * k, 1, z + rz * k, x + rx * k, b.h, z + rz * k, ANDESITE)
        for k in (0, -1):
            b.b(x + rx * k, 3, z + rz * k, x + rx * k, 3, z + rz * k, ANDESITE)
    # The doors swing out, their panic bars inside (doors round: real exit doors open the way you leave): the pair
    # laid from the other leaf, as the second leaf goes to the facing's left.
    b.door(x - rx, 1, z - rz, out, kind=kind, double=True, exit_=True)
    fx, fz = P.DIRS[out]
    cells = [(x, z), (x - rx, z - rz)]
    if canopy:
        for k in range(-2, 2):
            b.p(x - rx * k + fx, 4, z - rz * k + fz, SMOOTH_SLAB)
    if step and not b.grounded:
        for cx, cz in cells:
            b.p(cx + fx, 0, cz + fz, SMOOTH_SLAB)


# ------------------------------------------------------------------------------------------------- reactor building

def containment(s, ox, oz):
    """The reactor building (containment): a cylinder 51 across and 66 tall with a domed roof, a steel liner inside.

    Inside (22 October 2026: the approved machines in game, Justin: "i want all the new parts loaded in ... the
    reactor room"), the building is a compiled site (tools/reactor_site.py, its structure tools/rb_building.py): the
    operating floor at +15 with the refuelling cavity, the internals' bay and the transfer canal, the vessel and its
    integrated head package, the polar crane on its ring girder, the refuelling machine, the fuel transfer system, the
    head and internals stands, the SRO's station, the four steam generators and coolant pumps, the pressurizer, the
    loop piping, the primary shield, ladders and lights. These blocks are what stays round it: the base slab, the
    shell and its dome, the liner, the loop compartments' floor, the personnel airlock (west) and the equipment
    hatch (south) at the loop floor (level with the grass)."""
    r, wall_h, dome_h = 24.5, 46, 20
    size = 51
    c = size / 2
    b = Building("pwr_containment", "Reactor Containment (PWR)", size, size, wall_h + dome_h - 2, grounded=True)
    b.disc(c, c, r, 0, 0, SMOOTH)
    b.disc(c, c, r, 0, wall_h, SHIELD, inner=r - 2)
    b.disc(c, c, r - 2, 1, wall_h, LINER, inner=r - 2.7)
    b.disc(c, c, r + 0.6, wall_h - 2, wall_h, CONCRETE, inner=r - 2)
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        bx, bz = c + math.cos(a) * (r + 0.2), c + math.sin(a) * (r + 0.2)
        b.box(int(bx) - 1, 0, int(bz) - 1, int(bx) + 1, wall_h, int(bz) + 1, CONCRETE)
    for y in range(1, dome_h + 1):
        t = y / dome_h
        rr = r * math.sqrt(max(0.0, 1 - t * t))
        above = r * math.sqrt(max(0.0, 1 - ((y + 1) / dome_h) ** 2))
        b.disc(c, c, rr, wall_h + y, wall_h + y, SHIELD, inner=max(-1, min(rr - 2.6, above - 1.2)))
    b.disc(c, c, 3, wall_h + dome_h, wall_h + dome_h, CONCRETE_DARK)
    inner = r - 2.7  # inside the liner
    # The loop compartments' floor, level with the grass: the site opens it round the vessel's pit and the upender pit
    # and draws it there itself.
    b.disc(c, c, inner, 0, 0, EPOXY)
    cv = int(c)
    # Personnel airlock (west): a steel box through the wall with a steel door at each end.
    b.box(0, 1, cv - 2, 4, 4, cv + 2, LINER)
    b.clear(1, 1, cv - 1, 3, 3, cv + 1)
    b.door(0, 1, cv, "east", double=False)
    b.door(4, 1, cv, "east", double=False)
    # Equipment hatch (south): a big steel plug in a concrete collar, warning lights.
    b.box(cv - 4, 1, size - 4, cv + 4, 10, size - 1, CONCRETE)
    b.box(cv - 3, 1, size - 1, cv + 3, 8, size - 1, LINER)
    b.box(cv - 3, 1, size - 4, cv + 3, 8, size - 2, 0)
    # Amber beacons either side of the equipment hatch, on the face of its collar.
    for x in (cv - 4, cv + 4):
        s.decor[(ox + x, b.site_y(9), oz + size)] = beacon("amber", "south")
    s.add(b, ox, oz)
    return b


def place_primary(s, ox, oz):
    """The working reactor equipment (the same unit as before: the vessel, four steam generators, four coolant pumps,
    the pressurizer), hidden under the reactor building's loop floor (its look is the compiled site's): their anchors in
    a row at y -3 north of the vessel's pit on a buried coolant pipe; the main steam from the first steam generator
    along a buried line and up inside the containment's wall to the pipe bridge at y 21. Returns where the steam
    leaves the wall (x, z): the bridge's line goes on east from there."""
    y, zr = -3, 51
    kinds = (["reactor_pressure_vessel"] + ["reactor_steam_generator"] * 4 + ["reactor_coolant_pump"] * 4 + ["pressurizer"])
    for i, kind in enumerate(kinds):
        x = 74 + 2 * i
        P.HIDDEN.add((kind, x, y, zr))
        s.equipment.append((kind, x, y, zr, "north"))
    s.line(P.COOLANT, (74, y, zr + 1), (74 + 2 * (len(kinds) - 1), y, zr + 1))
    # main steam: off the first steam generator's anchor (x 76), north, east under the ground, south under the
    # containment to its east wall, up inside the wall (enclosed by it and the liner) and over to the bridge's line
    s.line(P.STEAM, (76, y, zr - 2), (76, y, zr - 1))
    s.line(P.STEAM, (77, y, zr - 2), (108, y, zr - 2))
    s.line(P.STEAM, (108, y, zr - 1), (108, y, 64))
    s.line(P.STEAM, (108, y + 1, 64), (108, 21, 64))
    s.line(P.STEAM, (108, 21, 65), (108, 21, 65))
    return 108, 65


# ------------------------------------------------------------------------------------------------- turbine hall

def turbine_hall(s, ox, oz):
    """The turbine building (turbine hall part 1: tools/turbine_hall.py)."""
    import turbine_hall as TH
    return TH.turbine_hall(s, ox, oz)


# ------------------------------------------------------------------------------------------------- control building

def wall_screen(b, name, x, y, z, facing):
    """A wall screen three blocks across and two high (plant_office2: status, plant map, training), its left block as
    you face it at x, z and its bottom at y, on the wall it faces."""
    rx, rz = P.DIRS[P.CLOCKWISE[facing]]
    for r in range(2):
        for c in range(3):
            b.p(x + rx * c, y + r, z + rz * c, multi_state(name, facing, r * 3 + c))
    screen_raceway(b, name, x + rx, y - 1, z + rz, facing)


def screen_raceway(b, name, x, y, z, facing):
    """Under a plant overview screen (round 34), where the wall below its middle is bare: the raceway bringing its power
    down the wall to a receptacle over the floor."""
    if name == "plant_map_screen" and b.get(x, y, z) == 0:
        b.p(x, y, z, furniture("screen_raceway", facing))


def dc_run(b, battery_ends, board, y, z, facing, tee=None):
    """The DC power cables from a station battery to its switchboard (plant_panels.RISERS): a riser up the wall from
    the end of each rack (where its cables come out of the top) to a cable tray under the ceiling at y, the tray along
    the wall at z to the switchboard, and a riser down into the switchboard's top. battery_ends: the x of each rack's
    terminal end; board: the x of the switchboard block the cables go down into; the racks and the board stand against
    the same wall (facing it), two blocks high from the floor below y - 4. The racks' risers carry the battery
    monitor's sense cable too (round 26, #9); tee: (x, facing), where a second string's run joins this one."""
    floor = y - 4
    ends = sorted(set(battery_ends) | {board})
    for x in ends:
        sense = "_sense" if x in battery_ends else ""
        for k in range(floor + 2, y):
            b.p(x, k, z, furniture("dc_cable_riser" + sense, facing))
        b.p(x, y, z, furniture("dc_cable_riser_top" + sense, facing))
    for x in range(ends[0] + 1, ends[-1]):
        if tee and x == tee[0]:
            b.p(x, y, z, furniture("dc_cable_tray_tee", tee[1]))
        elif x not in ends:
            b.p(x, y, z, furniture("dc_cable_tray", facing))


def dc_branch(b, battery_ends, y, z, facing, bend, cross, cross_facing):
    """A second string's DC cables (round 26, #9; Justin: "Second string in parallel", "Up the wall into the DC tray"):
    risers up its wall from its racks' terminal ends (with the sense cable) into a tray along that wall at z, the
    tray's bend at bend (x, facing), and on across (cross: the cells, cross_facing) to the tee in the first string's
    run (dc_run tee)."""
    floor = y - 4
    for x in battery_ends:
        for k in range(floor + 2, y):
            b.p(x, k, z, furniture("dc_cable_riser_sense", facing))
        b.p(x, y, z, furniture("dc_cable_riser_top_sense", facing))
    lo, hi = min(battery_ends + [bend[0]]), max(battery_ends + [bend[0]])
    for x in range(lo + 1, hi):
        if x not in battery_ends:
            b.p(x, y, z, furniture("dc_cable_tray", facing))
    b.p(bend[0], y, z, furniture("dc_cable_tray_bend", bend[1]))
    for cx, cz in cross:
        b.p(cx, y, cz, furniture("dc_cable_tray", cross_facing))


# The battery rooms' fittings (round 26, #9; Justin: "Eyewash and spill kit", "Hydrogen monitor and fan", "Warning
# signs"): (piece, x, y, z, facing, label) — the exhaust fan over a hydrogen exhaust louvre, the hydrogen monitor at eye
# level with its conduit up to the sensor head by the ceiling, the eyewash with its sign over it, the acid spill kit, the
# signs (NO SMOKING outside the door).
BATTERY_ROOM_1A = [("exhaust_fan", 1, 3, 3, "west", "1A"), ("h2_monitor", 1, 2, 5, "west", "1A"), ("wall_conduit", 1, 4, 5, "west", None),
                   ("h2_sensor", 1, 5, 5, "west", None), ("sign_hydrogen", 1, 2, 4, "west", None),
                   ("eyewash_station", 9, 1, 3, "east", None), ("sign_eyewash", 9, 2, 3, "east", None),
                   ("spill_kit", 9, 1, 2, "east", None), ("sign_battery_acid", 9, 2, 2, "east", None),
                   ("sign_no_smoking", 11, 2, 7, "west", None)]
BATTERY_ROOM_1B = [("exhaust_fan", 38, 3, 23, "east", "1B"), ("h2_monitor", 38, 2, 25, "east", "1B"), ("wall_conduit", 38, 4, 25, "east", None),
                   ("h2_sensor", 38, 5, 25, "east", None), ("sign_hydrogen", 38, 2, 24, "east", None),
                   ("eyewash_station", 30, 1, 23, "west", None), ("sign_eyewash", 30, 2, 23, "west", None),
                   ("spill_kit", 30, 1, 22, "west", None), ("sign_battery_acid", 30, 2, 22, "west", None),
                   ("sign_no_smoking", 36, 2, 17, "south", None)]


# Brought up to the detail standard (older rooms round 4: tools/battery_rooms.py): each string's disconnect switch on its
# strut frame under the DC tray, its conduit pair up into the tray (string 1 the racks labelled STR 1, the northern line
# in both rooms: nuclear/DcRoom.java onBus); the emergency light and the smoke detector; the PPE cabinet by the door,
# the room's sign over it, the cell log; the acid neutralization sump by the eyewash, a floor drain set in the floor; the ceiling lights
# vapor-tight fixtures (BATTERY_ROOMS).
BATTERY_ROOM_1A += [("battery_disconnect", 9, 1, 4, "east", "1A-1"), ("battery_disconnect", 9, 1, 5, "east", "1A-2"),
                    ("battery_disconnect_riser", 9, 3, 4, "east", None), ("battery_disconnect_riser", 9, 3, 5, "east", None),
                    ("emergency_light", 1, 4, 7, "west", None), ("smoke_detector", 5, 5, 4, "north", None),
                    ("battery_ppe_cabinet", 9, 2, 8, "south", None), ("sign_battery_1a", 9, 3, 8, "south", None),
                    ("battery_log_board", 1, 2, 6, "west", None), ("battery_room_sump", 8, 1, 3, "north", None),
                    ("restroom_floor_drain", 4, 0, 5, "north", None)]
BATTERY_ROOM_1B += [("battery_disconnect", 32, 1, 21, "west", "1B-1"), ("battery_disconnect", 32, 1, 22, "west", "1B-2"),
                    ("battery_disconnect_riser", 32, 3, 21, "west", None), ("battery_disconnect_riser", 32, 3, 22, "west", None),
                    ("emergency_light", 38, 4, 27, "east", None), ("smoke_detector", 35, 5, 24, "north", None),
                    ("battery_ppe_cabinet", 38, 2, 22, "east", None), ("sign_battery_1b", 38, 3, 22, "east", None),
                    ("battery_log_board", 38, 2, 26, "east", None), ("battery_room_sump", 31, 1, 23, "north", None),
                    ("restroom_floor_drain", 35, 0, 25, "north", None)]
BATTERY_ROOMS = [(1, 1, 9, 8), (30, 19, 38, 28)]
MULTI_FITTINGS = {"battery_disconnect_riser": 2}


def battery_room_fittings(b, fittings):
    for name, x, y, z, facing, label in fittings:
        if label:
            b.panel(name, x, y, z, facing, 1, label)
        elif name in MULTI_FITTINGS:
            for row in range(MULTI_FITTINGS[name]):
                assert b.get(x, y + row, z) == 0, (name, x, y + row, z)
                b.p(x, y + row, z, multi_state(name, facing, row))
        elif y == 0:
            # A floor drain is set in the floor (a whole block with the grate in its top, as in the diesel rooms).
            assert b.get(x, y, z) == EPOXY, (name, x, y, z)
            b.p(x, y, z, furniture(name, facing))
        else:
            assert b.get(x, y, z) == 0, (name, x, y, z)
            b.furniture(name, x, y, z, facing)


def feeder_conduits(b, ax, ay, az, facing, upper):
    """A load centre's feeder conduits (round 25b; Justin: the conduits on top of the switchgear were "going nowhere"):
    from the hub over each in-use upper feeder's section (tools/control_boards.py LC_CONDUIT_*), one 4-inch conduit
    straight up under the ceiling, through the slab and up through the floor of the cable spreading room above, to its
    power tray (the top tier: punch list #15) behind the run, where it turns in under the tray (plant_office2
    feeder_conduit_*). ax, ay, az: the
    lineup's anchor (building coordinates, ay walked on); upper: the slab of the floor above (its floor is upper)."""
    import control_boards as CB
    fx, fz = P.DIRS[facing]
    rx, rz = P.DIRS[P.CLOCKWISE[facing]]
    _, _, _, h = P.EQUIPMENT_SIZES["switchgear_lineup"]
    for k in range(5):
        if CB.LC_FEEDERS[2 * k] == "SPARE":
            continue
        lx, lz = 3 + k, int(CB.LC_CONDUIT_Z)
        x, z = ax + rx * lx + fx * lz, az + rz * lx + fz * lz
        tray = next(y for y in range(upper + 1, upper + 6) if b.get(x, y, z + 1) and P.MATERIALS[int(b.get(x, y, z + 1))][0].startswith("jugcraft:cable_tray_power"))
        column = [(ay + h, "feeder_conduit_base"), (upper - 2, "feeder_conduit_ceiling"), (upper + 1, "feeder_conduit_floor")]
        column += [(y, "feeder_conduit") for y in range(upper + 2, tray)] + [(tray, "feeder_conduit_tray")]
        column += [(y, "feeder_conduit") for y in range(ay + h + 1, upper - 2)]
        for y, piece in column:
            assert b.get(x, y, z) == 0, (piece, x, y, z, P.MATERIALS[int(b.get(x, y, z))][0])
            b.p(x, y, z, furniture(piece, "north"))


# The control building's electrical runs (building coordinates; the slab is y 0): where the main transformer's buried
# feed comes up under each load centre's transformer section; the DC MCC breakers' runs to the DC rooms' motor control
# centres (round 26, 8a: under MCC 1A's incoming section at (17, 13), MCC 1B's at (24, 19)); and the MCCs' charger
# runs. MCC 1A faces south (its sections x 17, 16, 15, 14), MCC 1B north (x 24..27). CHGR 1 is the third section's
# bottom terminal, straight down; CHGR 2 the fourth's, out of the back into the wall behind it. In 1B, CHGR 2's run
# goes down under the others (two and three blocks below the slab: MCC_DEEP_RUNS, laid by the site).
SLAB_RISERS = [(22, 1), (28, 10)]
CHARGER_RUNS = ([(27, 0, 1), (27, 0, 2)] + [(x, 0, 3) for x in range(19, 28)] + [(19, 0, z) for z in range(4, 14)] + [(18, 0, 13), (17, 0, 13)]
                + [(24, 0, z) for z in range(10, 20)])
MCC_RUNS = ([(15, 0, z) for z in range(2, 14)] + [(16, 0, 2), (16, 0, 1)]           # 1A CHGR 1 to the charger at x 16
            + [(14, 1, 14), (14, 0, 14), (13, 0, 14)] + [(13, 0, z) for z in range(1, 14)]   # 1A CHGR 2, x 13
            + [(26, 0, z) for z in range(19, 29)]                                    # 1B CHGR 1 to the charger at x 26
            + [(27, 1, 18), (27, 0, 18), (23, 0, 28)])                               # 1B CHGR 2 into the wall, down; up at x 23
MCC_DEEP_RUNS = ([(27, -1, 18), (27, -2, 18), (26, -2, 18), (25, -2, 18), (24, -2, 18)]
                 + [(24, -2, z) for z in range(19, 29)] + [(23, -2, 28), (23, -1, 28)])

# The DC rooms' overhead ladder tray (round 26, 8d; Justin: "Overhead tray now"), under the ceiling (y 5): over the
# equipment row, along the side wall over the relay cabinets, and over the MCC, each piece (x, z, kind, facing). The
# regulating transformers' feeders leave it by the drops: one over each transformer, its conduit down into the
# transformer's top (TRAY_CONDUITS), and two side by side over the MCC's REG XF section (its second section, x 16 in
# 1A, x 25 in 1B), down into its top. A trapeze hanger every other block and at each run's end.
TRAY_Y = 5


def _tray_line(cells, facing, drops=(), pairs=(), walls=()):
    """A straight run of tray: a drop where a conduit comes up into it, a trapeze hanger every other block and at the
    run's end (a drop's on its hanger kind there: round 26, 8f, the equipment rows being all drops)."""
    out = []
    for i, (x, z) in enumerate(cells):
        key = (x, z)
        hung = i % 2 == 0 or i == len(cells) - 1
        kind = ("ladder_tray_drop" if key in drops else "ladder_tray_drop_pair" if key in pairs else "ladder_tray_drop_wall" if key in walls
                else "ladder_tray")
        if hung and kind in ("ladder_tray", "ladder_tray_drop", "ladder_tray_drop_pair"):
            kind = "ladder_tray_hanger" if kind == "ladder_tray" else kind + "_hanger"
        out.append((x, z, kind, (walls.get(key, facing) if isinstance(walls, dict) else facing)))
    return out


# The 120 VAC vital panels (round 26, 8e; Justin: "Side wall, under the tray"): each room's two on its relay cabinets'
# wall, under the tray's run along it, (x, z, facing, label); their conduits go straight up the wall into the tray's
# wall drops over them, and each inverter's output conduit up from its top into a drop over the row.
VITAL_PANELS_1A = [(19, 10, "east", "1A-1"), (19, 11, "east", "1A-2")]
VITAL_PANELS_1B = [(19, 24, "west", "1B-1"), (19, 25, "west", "1B-2")]
# The 125 VDC distribution panels (older rooms round 3; Justin: "DC distribution panels"), where the relay cabinets
# stood on the same wall, built as the vital panels (their conduits up the wall into the tray's wall drops).
DC_PANELS_1A = [(19, 4, "east", "1A-1"), (19, 5, "east", "1A-2")]
DC_PANELS_1B = [(19, 22, "west", "1B-1"), (19, 23, "west", "1B-2")]
DC_TRAY_1A = (_tray_line([(x, 1) for x in range(12, 19)], "north", drops={(12, 1), (13, 1), (15, 1), (16, 1), (18, 1)}, pairs={(14, 1), (17, 1)})
              + [(19, 1, "ladder_tray_bend_drop", "north")]
              + _tray_line([(19, z) for z in range(2, 13)], "east", walls={(x, z): f for x, z, f, _ in VITAL_PANELS_1A + DC_PANELS_1A})
              + [(19, 13, "ladder_tray_bend", "east")]
              + _tray_line([(x, 13) for x in range(18, 13, -1)], "north", pairs={(16, 13)}))
DC_TRAY_1B = (_tray_line([(x, 28) for x in range(27, 19, -1)], "north", drops={(27, 28), (26, 28), (24, 28), (23, 28), (21, 28), (20, 28)},
                         pairs={(25, 28), (22, 28)})
              + [(19, 28, "ladder_tray_bend", "south")]
              + _tray_line([(19, z) for z in range(27, 19, -1)], "east", walls={(x, z): f for x, z, f, _ in VITAL_PANELS_1B + DC_PANELS_1B})
              + [(19, 19, "ladder_tray_bend", "west")]
              + _tray_line([(x, 19) for x in range(20, 28)], "north", pairs={(25, 19)}))
# The feeders' conduits under the drops: (x, z, from y, to y, piece, facing): from the top of every cabinet in the row
# (round 26, 8f; Justin: "Chargers, switchboards, inverter DC"): the switchboard's (its second section, x 12 in 1A, x 27
# in 1B), each charger's DC output, each inverter's two (its DC feed and its AC output), each transformer's, the ground
# detector's (in 1A into the drop in the bend over it); the MCC's pair from its top; the vital panels' up the wall.
TRAY_CONDUITS = ([(x, 1, 3, 4, "feeder_conduit", "north") for x in (12, 13, 15, 16, 18, 19)] + [(x, 1, 3, 4, "conduit_pair_run", "north") for x in (14, 17)]
                 + [(16, 13, 4, 4, "conduit_pair", "north")]
                 + [(x, 28, 3, 4, "feeder_conduit", "north") for x in (27, 26, 24, 23, 21, 20)] + [(x, 28, 3, 4, "conduit_pair_run", "north") for x in (25, 22)]
                 + [(25, 19, 4, 4, "conduit_pair", "north")]
                 + [(x, z, 3, 4, "wall_conduit", f) for x, z, f, _ in VITAL_PANELS_1A + VITAL_PANELS_1B + DC_PANELS_1A + DC_PANELS_1B])
# Each room's DC ground detector and battery monitor (round 26, 8f; Justin: "End of the equipment row"): beside the
# row's last transformer (in 1A in the corner, under the tray's bend), (x, z, facing, label).
GROUND_DETECTORS = [(19, 1, "north", "1A"), (20, 28, "south", "1B")]


# Each DC room's fittings (older rooms round 3; Justin: "Lights and safety", "One-line and signs", "Desk, phone,
# drawings", "Mats and work gear"): the log desk (phone, logbook, the live laptop) against the wall opposite the door's
# side with the framed one-line over it, the printer and the drawing rack beside it, the battery service board by the
# battery room's way, the train's sign and the DANGER sign at the row's end, the test cart, rubber mats along the
# switchboard's row and the MCC's front, two battery emergency lights on the desk's wall, two smoke detectors.
DC_FITTINGS_1A = {"wall": "west", "x": 11, "desk": [10, 9, 8], "printer": 11, "rack": 12, "board": [4, 3], "sign": 2,
                  "cart": (18, 7, "east"), "mats": [(x, 2) for x in range(11, 19)] + [(x, 12) for x in range(14, 18)],
                  "lights": [3, 11], "smoke": [(14, 5), (14, 10)], "train": "a"}
DC_FITTINGS_1B = {"wall": "east", "x": 28, "desk": [22, 23, 24], "printer": 21, "rack": 20, "board": [25, 26], "sign": 27,
                  "cart": (20, 20, "west"), "mats": [(x, 27) for x in range(20, 29)] + [(x, 20) for x in range(24, 28)],
                  "lights": [21, 26], "smoke": [(24, 22), (24, 25)], "train": "b"}


def dc_room_fittings(b, fit):
    w, x, y = fit["wall"], fit["x"], 1

    def put(xx, yy, z, m):
        assert b.get(xx, yy, z) == 0, ("DC room", xx, yy, z, P.MATERIALS[int(b.get(xx, yy, z))][0])
        b.p(xx, yy, z, m)
    for k, z in enumerate(fit["desk"]):
        put(x, y, z, variant_state("sgr_log_desk", w, k) if k < 2 else multi_state("dc_desk_laptop", w, 0))
        for row in range(2):
            put(x, y + 1 + row, z, multi_state("dc_one_line", w, row * 3 + k))
    put(x, y, fit["printer"], furniture("soe_printer", w))
    put(x, y, fit["rack"], furniture("drawing_rack", w))
    for c, z in enumerate(fit["board"]):
        put(x, y + 1, z, multi_state("battery_service_board", w, c))
    put(x, y + 2, fit["sign"], furniture(f"sign_dc_train_{fit['train']}", w))
    put(x, y + 1, fit["sign"], furniture("sign_dc_danger", w))
    cx, cz, cf = fit["cart"]
    put(cx, y, cz, furniture("battery_test_cart", cf))
    for mx, mz in fit["mats"]:
        put(mx, y, mz, MAT)
    for z in fit["lights"]:
        b.furniture("emergency_light", x, 4, z, w)
    for sx, sz in fit["smoke"]:
        put(sx, TRAY_Y, sz, furniture("smoke_detector", "north"))


def dc_room_tray(b, tray):
    """A DC room's ladder tray and its feeders' conduits down to the transformers, the inverters, the MCC and the
    vital panels."""
    for x, z, kind, facing in tray:
        assert b.get(x, TRAY_Y, z) == 0, (kind, x, z)
        b.p(x, TRAY_Y, z, furniture(kind, facing))
    cells = {(x, z) for x, z, _, _ in tray}
    for x, z, y0, y1, piece, facing in TRAY_CONDUITS:
        if (x, z) in cells:
            for y in range(y0, y1 + 1):
                assert b.get(x, y, z) == 0, (piece, x, y, z)
                b.p(x, y, z, furniture(piece, facing))


def control_building_mcc_runs(s, ox, oz, sink=1):
    """MCC 1B's CHGR 2 run where it passes under the others, below the slab (round 26): in the site's ground, the
    building sunk a block (its slab is the site's y -1)."""
    for x, y, z in MCC_DEEP_RUNS:
        s.decor[(ox + x, y - sink, oz + z)] = CABLE


def control_building_feed(s, ox, oz):
    """The buried feed from the unit auxiliary transformers to the control building's load centres (round 25; since the
    transformer yard build, from under UAT-1A's 4160 V bus duct where it goes down into the ground, the yard's house
    feed: tools/yard_sites.py): west two blocks under the grass to the building, then under the building a block deeper
    (clear of the charger runs in its slab) to below each load centre's riser."""
    run = [(240, -1, 59)] + [(x, -2, 59) for x in range(158, 241)] + [(158, -2, z) for z in range(60, oz + 2)]
    rx, rz = ox + SLAB_RISERS[1][0], oz + SLAB_RISERS[1][1]
    run += [(158, -3, oz + 1)] + [(x, -3, oz + 1) for x in range(159, rx + 1)] + [(rx, -3, z) for z in range(oz + 2, rz + 1)]
    run += [(rx, -2, rz)]
    assert (ox + SLAB_RISERS[0][0], oz + SLAB_RISERS[0][1]) == (158, oz + 1)
    for cell in run:
        s.decor[cell] = CABLE
    return run


# The entrance hall's fittings (older rooms round 6; tools/entrance_hall.py): the hand geometry reader at the entry lane; the
# VITAL AREA and prohibited items signs over the vestibule's inner doors; inside the gates the evacuation plan and the
# master clock on the corridor wall, the AED; smoke detectors and emergency lights.
ENTRANCE_SMOKE = [(4, 23), (10, 20), (5, 27)]


# The remote shutdown room's smoke detector (older rooms round 7).
RSR_SMOKE = [(5, 12)]


def remote_shutdown_fittings(b):
    """The remote shutdown room's fittings (older rooms round 7; tools/rsr_room.py): along the south wall, west of the
    door, the AOP procedure rack and an SCBA cabinet (the control room's); east of it the transfer key cabinet by the
    door, the communications station and the radio charging rack; the master clock over the door; the room's sign on
    the west wall; a smoke detector."""
    import cr_room as CR

    def put(x, y, z, m):
        assert b.get(x, y, z) == 0, ("remote shutdown", x, y, z, P.MATERIALS[int(b.get(x, y, z))][0])
        b.p(x, y, z, m)
    # (x 2 left clear: the extinguisher on the west wall faces along it.)
    b.panel("cr_procedure_rack", 3, 1, 13, "south", 1, label="AOP")
    for row in range(2):
        put(4, 1 + row, 13, multi_state("cr_scba_cabinet", "south", row))
    put(6, 2, 13, furniture("rsr_key_cabinet", "south"))
    put(7, 2, 13, furniture("rsr_phone_station", "south"))
    b.panel("ops_radio_rack", 8, 2, 13, "south", 1)
    b.panel("ops_wall_clock", 5, 3, 13, "south", 1)
    put(1, 3, 11, furniture("sign_rsr_area", "west"))
    for x, z in RSR_SMOKE:
        put(x, 5, z, furniture("smoke_detector", "north"))
    assert CR.PR_LABELS.index("AOP") >= 0


def entrance_fittings(b):
    def put(x, y, z, m):
        assert b.get(x, y, z) == 0, ("entrance", x, y, z, P.MATERIALS[int(b.get(x, y, z))][0])
        b.p(x, y, z, m)
    put(4, 1, 23, furniture("ac_hand_reader", "west"))
    put(4, 4, 26, furniture("sign_vital_area", "north"))
    put(7, 4, 26, furniture("sign_prohibited", "north"))
    rx, rz = P.DIRS[P.CLOCKWISE["north"]]
    for c in range(2):
        put(2 + rx * c, 2, 19, multi_state("ac_evac_plan", "north", c))
    b.panel("ops_wall_clock", 9, 3, 19, "north", 1)
    put(12, 2, 20, furniture("aed_cabinet", "east"))
    for x, z in ENTRANCE_SMOKE:
        put(x, 5, z, furniture("smoke_detector", "north"))
    b.furniture("emergency_light", 12, 4, 22, "east")
    b.furniture("emergency_light", 1, 4, 24, "west")


def control_building(s, ox, oz):
    """The control building, 40 x 30 and four floors, laid out like a real plant's (round 18; Justin: "the stairs are
    cut off ... I don't know if the batteries are even connected ... the front lobby looks kind of tacky ... make it
    look realistic"). Nuclear plants put the control room high in a building of its own, over the cable spreading room,
    with the safety-related DC power at grade, each train in its own fire-separated rooms (NRC fire hazards analyses of
    TMI, Susquehanna and Peach Bottom; IEEE 484 battery rooms).

    Grade: a small entrance (an airlock-style vestibule, the security officer's desk, speed gates) in place of the
    old empty lobby; Battery Rooms 1A and 1B, each a 60-cell 125 VDC station battery on two step racks (plant_panels'
    station_battery) with its cables up a riser into a tray and across to the DC Equipment Room next door, down into the
    125 VDC switchboard beside the chargers and vital inverters; the 480 V switchgear; the remote shutdown room; the
    operators' locker room. First floor: the cable spreading room under the control room (tiers of tray, risers up into
    the floor above), the relay room and the plant computer room on its raised floor. Second floor: the main control
    room (the horseshoe of control boards under the plant display wall, the reactor console, operators' consoles, the
    shift supervisor's glazed office), across the corridor the Technical Support Center, the kitchen, washroom and the
    operations office. Top floor: the control room's emergency filtration and air conditioning (air handling units,
    chillers, pumps), with louvres in the walls. A stair tower serves every floor, laid last so no floor cuts it.
    Returns the reactor console's site position."""
    W, D, H = 40, 30, 29
    L0, L1, L2, L3 = 0, 7, 14, 23
    b = Building("control_building", "Control Building", W, D, H, grounded=True)
    b.shell_walls(CONCRETE, step=8)
    b.storey(L0, 1, 1, W - 2, D - 2, EPOXY)
    b.storey(L1, 1, 1, W - 2, D - 2, VINYL, ceiling_at=L1 - 1)
    b.storey(L2, 1, 1, W - 2, D - 2, ACCESS_FLOOR, ceiling_at=L2 - 1)
    b.storey(L3, 1, 1, W - 2, D - 2, EPOXY, ceiling_at=L3 - 1)
    # Windows only where people work by day: the entrance hall, the second floor's offices; the plant floors have none.
    for x in range(1, 13):
        if x % 8 and x not in (3, 4, 5, 6, 7, 8):
            for y in (2, 3, 4):
                b.p(x, y, D - 1, PANE)
    for x in range(1, W - 1):
        if x % 8:
            for y in range(L2 + 2, L2 + 6):
                b.p(x, y, D - 1, PANE)
    entrance(b, 6, D - 1, "s")
    # Louvres: the battery rooms' hydrogen exhaust, high in the outside wall; the top floor's air intakes and exhausts.
    for z in (3, 6):
        b.p(0, 4, z, LOUVRE)
    for z in (23, 26):
        b.p(W - 1, 4, z, LOUVRE)
    for x in [x for r in HVAC_LOUVRES for x in r]:
        for y in (L3 + 2, L3 + 3, L3 + 4):
            b.p(x, y, 0, LOUVRE)
    for z in range(17, 23):
        for y in (L3 + 2, L3 + 3, L3 + 4):
            b.p(W - 1, y, z, LOUVRE)
    ground_floor(b, W, D)
    first_floor(b, W, D, L1)
    console = second_floor(b, W, D, L2)
    spreading_room_slab(b, L2)
    top_floor(b, W, D, L3)
    # The switchgear room's fit-out (older rooms round 1): the load centres' trays and their way up into the cable
    # spreading room, the cooling, the handling gear, the log desk, the signs.
    switchgear_room(b, L1)
    # The corridors' lights (the ceiling grid misses them), and the entrance hall's.
    for x in range(3, W - 1, 4):
        b.p(x, L1 - 2, 16, LIGHT)
        b.p(x, L2 - 2, 16, LIGHT)
        b.p(x, L3 - 2, 19, LIGHT)
    for x, z in ((5, 27), (11, 26), (1, 26)):
        b.p(x, L1 - 2, z, LIGHT)
    # The stair tower, last: walls, landings, flights and a door on every floor, nothing laid after it cuts into it.
    # A light over every floor's landing (under the landing above, or the tower's lid).
    levels = [L0, L1, L2, L3]
    b.stairwell(W - 9, 1, levels, door_side="s")
    for above in levels[1:] + [L3 + 4]:
        b.p(W - 6, above - 1, 11, LIGHT)
    top_floor_sign(b, L3)
    s.add(b, ox, oz)
    cx, cz = console
    return (ox + cx, oz + cz, b.site_y(L2 + 1))


def ground_floor(b, W, D):
    """Grade (y 1..5): north of the corridor Battery Room 1A, the Remote Shutdown Room, DC Equipment Room 1A and the
    switchgear, the stairs; south of it the entrance hall, the locker room, DC Equipment Room 1B and Battery Room 1B."""
    y0, y1 = 1, 5
    b.wall_x(14, 1, W - 10, y0, y1)
    b.wall_x(18, 1, W - 2, y0, y1)
    b.wall_z(10, 1, 13, y0, y1)
    b.wall_x(9, 1, 9, y0, y1)
    b.wall_z(20, 1, 13, y0, y1)
    b.wall_z(30, 1, 13, y0, y1)
    for x in (13, 18, 29):
        b.wall_z(x, 19, D - 2, y0, y1)

    # ---- Battery Room 1A (x 1..9, z 1..8): the station battery's two racks end to end along the north wall, their
    # cables up and along the wall under the ceiling, through into the DC Equipment Room; the hydrogen exhaust louvres.
    b.panel("station_battery", 1, 1, 1, "north", 4, "1A STR 1 CELLS 31-60")
    b.panel("station_battery", 5, 1, 1, "north", 4, "1A STR 1 CELLS 1-30")
    dc_run(b, [4, 8], 11, 5, 1, "north", tee=(9, "north"))
    # Its second string (round 26, #9; Justin: "Second string in parallel"): two more racks along the south wall, their
    # cables up into a tray along it, round the corner and along the east wall into the first string's run.
    b.panel("station_battery", 8, 1, 8, "south", 4, "1A STR 2 CELLS 1-30")
    b.panel("station_battery", 4, 1, 8, "south", 4, "1A STR 2 CELLS 31-60")
    dc_branch(b, [5, 1], 5, 8, "south", (9, "east"), [(9, z) for z in range(2, 8)], "east")
    b.door(10, 1, 6, "west", label="battery_room_1a")
    b.furniture("fire_extinguisher", 1, 2, 7, "west")
    battery_room_fittings(b, BATTERY_ROOM_1A)
    # ---- Remote Shutdown Room (x 1..9, z 10..13; round 24): the remote shutdown panel standing along its north wall
    # (Justin: no bookcase; the panel must work; "more realistic with detail"), a rubber mat in front of it, the
    # battery emergency lights over it (the room must stay lit if the plant loses power), against the east wall an
    # operator console, an extinguisher by the door.
    b.prop("remote_shutdown_panel", 2, 1, 10, "north")
    for x in range(2, 8):
        b.p(x, 1, 11, MAT)
    for x in (3, 6):
        b.furniture("emergency_light", x, 4, 10, "north")
    # (Older rooms round 7, tools/rsr_room.py: the operator console became the remote shutdown station, its screens
    # live; the room's fittings in remote_shutdown_fittings.)
    b.panel("rsr_desk", 9, 1, 10, "east", 3)
    b.p(8, 1, 11, furniture("operator_chair", "east"))
    b.door(5, 1, 14, "north", label="remote_shutdown")
    b.furniture("fire_extinguisher", 1, 2, 13, "west")
    remote_shutdown_fittings(b)
    # ---- DC Equipment Room 1A (x 11..19, z 1..13): along the north wall the 125 VDC switchboard (the battery's cables
    # come down into it), the chargers and the vital inverters, a charger never beside a charger (their feeds in the
    # slab would join); along the south wall the 480 V motor control centre feeding the chargers (round 26, 8a; its
    # first section, the incoming, at x 17).
    # Their nameplates by train and number (round 26, 8c): charger 1A-1 is the one MCC 1A's CHGR 1 feeds, inverter
    # 1A-1 the one its REG XF 1 backs. Each inverter has its regulating transformer beside it (8d; Justin: "Beside
    # their inverters"), so the row is in groups, charger, inverter, transformer, by number.
    b.panel("dc_switchboard", 11, 1, 1, "north", 2, "1A")
    for x, label in ((13, "1A-2"), (16, "1A-1")):
        b.panel("station_charger", x, 1, 1, "north", 1, label)
        b.panel("vital_inverter", x + 1, 1, 1, "north", 1, label)
        b.panel("reg_transformer", x + 2, 1, 1, "north", 1, label)
    b.prop("mcc_lineup", 14, 1, 13, "south")
    for x, z, facing, label in VITAL_PANELS_1A:
        b.panel("vital_panel", x, 1, z, facing, 1, label)
    for x, z, facing, label in DC_PANELS_1A:
        b.panel("dc_dist_panel", x, 1, z, facing, 1, label)
    x, z, facing, label = GROUND_DETECTORS[0]
    b.panel("dc_ground_detector", x, 1, z, facing, 1, label)
    dc_room_tray(b, DC_TRAY_1A)
    b.door(11, 1, 14, "north", label="dc_equipment_1a")
    b.furniture("fire_extinguisher", 19, 2, 8, "east")
    dc_room_fittings(b, DC_FITTINGS_1A)
    # ---- Switchgear Room (x 21..29, z 1..13; round 25): the 480 V load centres of both trains, face to face across the
    # aisle, walked round at the west end (Justin: "realistic switchgear ... less simple and cartoony", and it must
    # hold charge and work): rubber mats along their fronts, the battery emergency lights over the aisle, an
    # extinguisher (no locker: Justin, round 25b). Their feeder conduits go up into the cable spreading room
    # (feeder_conduits, after the floors are in).
    b.prop("switchgear_lineup", 22, 1, 1, "north", variant=0)
    b.prop("switchgear_lineup", 22, 1, 9, "south", variant=1)
    for x in range(22, 30):
        b.p(x, 1, 3, MAT)
        b.p(x, 1, 8, MAT)
    b.furniture("emergency_light", 21, 4, 4, "west")
    b.furniture("emergency_light", 29, 4, 4, "east")      # (north of the fan coil unit's duct, its conduit clear)
    b.door(26, 1, 14, "north", double=True, label="switchgear_room")
    b.furniture("fire_extinguisher", 21, 2, 6, "west")
    # Their supply comes up through the slab into the back of each transformer section (from the main transformer,
    # buried: control_building_feed), and each one's DC MCC breaker (the lower breaker of its third section) feeds its
    # train's motor control centre by a cable in the slab: train A west along the aisle's mat, through the wall into DC
    # Equipment Room 1A and along its east side to under the MCC's incoming section; train B south under the corridor
    # to under MCC 1B's (nuclear/Switchgear.java, MotorControlCenter.java). Each MCC's CHGR 1 and CHGR 2 breakers feed
    # one charger each (MCC_RUNS; the deeper parts, control_building_mcc_runs). The runs keep a block clear of every
    # other terminal and of each other, so the feeds never meet.
    for x, z in SLAB_RISERS:
        b.p(x, 0, z, WALL_CABLE)
    for x, y, z in CHARGER_RUNS + MCC_RUNS:
        b.p(x, y, z, WALL_CABLE)

    # ---- The entrance hall (x 1..12, z 19..28): in through the glass doors to a vestibule, a second pair of glass doors,
    # the security officer at his desk behind the counter, the speed gates, and through them the doors to the
    # corridor. A status screen, a notice board, a bench.
    b.b(1, 0, 19, 12, 0, D - 2, VINYL)
    b.b(3, 0, 26, 8, 0, D - 2, CARPET)
    # The vestibule's glass: framed glass flush with its doors (the doors' plane, the south edge of z 25), the side
    # walls on the vestibule's outer faces, so the corners meet; a transom of glass over the doors.
    for y in range(y0, y1 + 1):
        for z in range(26, D - 1):
            b.p(2, y, z, FGLASS["east"] if y <= 3 else PAINTED)
            b.p(9, y, z, FGLASS["west"] if y <= 3 else PAINTED)
        for x in range(2, 10):
            b.p(x, y, 25, FGLASS["north"] if y <= 3 else PAINTED)
    b.door(6, 1, 25, "north", kind="glass", double=True)
    # The security post in the hall (z 23..25; older rooms round 6, tools/entrance_hall.py: its bullet-resisting counter and
    # glass to the hall, the officer's desk behind with the security computer's screens, the gate status panel and
    # the duress button), the officer's chair behind it clear of the wall.
    b.panel("ac_station", 10, 1, 25, "west", 3)
    b.p(11, 1, 24, furniture("operator_chair", "west"))
    # The speed gates across the hall (Justin: "the ones with the two doors ... it'll just open one way"): the entry
    # lane on the west, opening for whoever walks up from the hall, the exit lane beside it opening for whoever comes
    # out; three pedestals, the middle one shared, each lane's doors sliding back into their own slots. The door line
    # is the south edge of z 21, the framed glass either side on the same line (nuclear/SpeedGateBlock.java).
    b.p(5, 1, 22, SPEED_GATE["north"])
    b.p(7, 1, 21, SPEED_GATE["south"])
    for x in (4, 6, 8):
        b.p(x, 1, 21, GATE_PEDESTAL["a"])
        b.p(x, 1, 22, GATE_PEDESTAL["b"])
    # The glass runs on from the outer pedestals to both walls: the counter is on the hall side, so nobody gets round.
    for y in (1, 2):
        for x in (1, 2, 3, 9, 10, 11, 12):
            b.p(x, y, 21, FGLASS["north"])
    b.door(6, 1, 18, "north", double=True)
    # The plant's status on a screen, a bench by the window, a notice board; the potted plants against the walls.
    wall_screen(b, "status_screen", 1, 2, 24, "west")
    b.row(("sofa", 2), 12, 1, 27, "west")
    b.varied("whiteboard", 10, 2, 28, "south", 24)
    b.plant(1, 1, 20)
    b.plant(1, 1, 22)
    b.furniture("water_cooler", 1, 1, 23, "west")
    b.furniture("fire_extinguisher", 12, 2, 25, "east")
    entrance_fittings(b)
    # ---- Locker room (x 14..17): lockers along both walls, a bench down the middle.
    for z in range(20, D - 1):
        b.stack("locker", 17, 1, z, "east", n=2)
        if z not in range(22, 27):
            b.stack("locker", 14, 1, z, "west", n=2)
    b.row(("locker_bench", 5), 15, 1, 22, "east")
    b.door(16, 1, 18, "south", label="locker_room")
    # ---- DC Equipment Room 1B (x 19..28) and Battery Room 1B (x 30..38): as Train A, along the south wall; MCC 1B
    # along the north wall (its incoming section at x 24, over the feed from load centre B).
    b.panel("dc_switchboard", 28, 1, D - 2, "south", 2, "1B")
    for x, label in ((26, "1B-1"), (23, "1B-2")):
        b.panel("station_charger", x, 1, D - 2, "south", 1, label)
        b.panel("vital_inverter", x - 1, 1, D - 2, "south", 1, label)
        b.panel("reg_transformer", x - 2, 1, D - 2, "south", 1, label)
    b.prop("mcc_lineup", 24, 1, 19, "north", variant=1)
    for x, z, facing, label in VITAL_PANELS_1B:
        b.panel("vital_panel", x, 1, z, facing, 1, label)
    x, z, facing, label = GROUND_DETECTORS[1]
    b.panel("dc_ground_detector", x, 1, z, facing, 1, label)
    for x, z, facing, label in DC_PANELS_1B:
        b.panel("dc_dist_panel", x, 1, z, facing, 1, label)
    dc_room_tray(b, DC_TRAY_1B)
    b.door(22, 1, 18, "south", double=True, label="dc_equipment_1b")
    b.furniture("fire_extinguisher", 19, 2, 26, "west")
    dc_room_fittings(b, DC_FITTINGS_1B)
    b.panel("station_battery", 37, 1, D - 2, "south", 4, "1B STR 2 CELLS 1-30")
    b.panel("station_battery", 33, 1, D - 2, "south", 4, "1B STR 2 CELLS 31-60")
    dc_run(b, [34, 30], 28, 5, D - 2, "south", tee=(32, "south"))
    # Its second string (round 26, #9) along the north wall; the door moved to the room's east end to make room for it.
    b.panel("station_battery", 30, 1, 19, "north", 4, "1B STR 1 CELLS 31-60")
    b.panel("station_battery", 34, 1, 19, "north", 4, "1B STR 1 CELLS 1-30")
    dc_branch(b, [33, 37], 5, 19, "north", (32, "west"), [(32, z) for z in range(20, D - 2)], "west")
    b.door(W - 2, 1, 18, "south", label="battery_room_1b")
    b.furniture("fire_extinguisher", W - 2, 2, 21, "east")
    battery_room_fittings(b, BATTERY_ROOM_1B)
    # (Older rooms round 4: the battery rooms lit by vapor-tight fixtures where the ceiling grid puts its lights.)
    b.light_swap += [(x0, 1, z0, x1, 6, z1, furniture("vapor_tight_light", "north")) for x0, z0, x1, z1 in BATTERY_ROOMS]


# The relay room (round 26, #10; Justin: "Real cabinet types", "Unique labels", "Overhead trays and cable entry", "Room
# extras"): four rows back to back either side of the aisle under the door (x 8 and 9), each cabinet (tools/
# relay_room.py) by its label, as you read the row facing it. Row A faces the north aisle, row B and C the middle
# aisle, row D the south one; rows C and D stop short of the east wall for the sequence of events recorder's desk.
RELAY_ROWS = {
    # row: (z, facing, its cabinets from the reader's left: (piece, label))
    "a": (21, "south", [("protection_rack", f"CH {c} RACK {r}") for c in ("I", "II") for r in (1, 2, 3)]
          + [("relay_panel", "RCP 1A 1B MOTORS"), ("relay_panel", "RCP 1C 1D MOTORS")]
          + [("protection_rack", f"CH {c} RACK {r}") for c in ("III", "IV") for r in (1, 2, 3)]),
    "b": (22, "north", [("ssps_cabinet", f"TRAIN {t} {bay} BAY") for t in "AB" for bay in ("INPUT", "LOGIC", "OUTPUT")]
          + [("nis_cabinet", n) for n in ("N31 N32", "N35 N36", "N41 N42", "N43 N44", "NC41 NM41")]
          + [("relay_panel", n) for n in ("MAIN GENERATOR", "MAIN TRANSFORMER", "UNIT AUX XFMR")]),
    "c": (25, "south", [("relay_panel", n) for n in ("4160V BUS 1A", "4160V BUS 1B", "4160V BUS 1C", "4160V BUS 1D", "EDG 1A", "EDG 1B",
                                                    "480V LC 1A", "480V LC 1B", "STARTUP XFMR", "345KV BREAKERS", "MAIN STEAM ISOL",
                                                    "FEEDWATER ISOL")]),
    "d": (26, "north", [("relay_panel", n) for n in ("SI PUMPS", "RHR PUMPS", "CONT SPRAY PUMPS", "CHARGING PUMPS", "CCW PUMPS",
                                                    "SERVICE WATER", "AFW PUMPS", "CONDENSATE PUMPS", "FEEDWATER PUMPS",
                                                    "CIRC WATER PUMPS", "DIESEL SEQ A", "DIESEL SEQ B")]),
}
RELAY_AISLE = (8, 9)          # the aisle under the door: the trays' header runs over x 8
RELAY_HEADER_X = 8
RELAY_HANGERS = (3, 6, 9, 12, 15)
RELAY_SMOKE = ((4, 20), (13, 20), (4, 24), (13, 24), (4, 28), (12, 28))


def relay_row_xs(row):
    """The x of each of a row's cabinets in the order RELAY_ROWS lists them (from the reader's left: west for a row
    read facing north, east for one read facing south); every row starts at the west wall's aisle."""
    z, facing, cabinets = RELAY_ROWS[row]
    xs = [x for x in range(2, 18) if x not in RELAY_AISLE][:len(cabinets)]
    return xs[::-1] if facing == "south" else xs


def relay_room(b, y0, y1):
    """The relay room's fit-out: the cabinets; over each row its control cable tray at the ceiling, a drop over every
    cabinet and its bundle down into the cabinet's top; the header along the aisle from the cable spreading room's top
    tray (a tee in it), through both walls of the corridor in fire-stopped penetrations, crossing the rows' runs; the
    row signs, smoke detectors, the drawing rack, the SOE recorder's desk and printer, an extinguisher."""
    tray = y1
    for row, (z, facing, cabinets) in RELAY_ROWS.items():
        xs = relay_row_xs(row)
        for x, (piece, label) in zip(xs, cabinets):
            b.panel(piece, x, y0, z, facing, 1, label)
            b.p(x, y0 + 2, z, furniture("control_cable_entry", "north"))
            for k in range(y0 + 3, tray):
                b.p(x, k, z, furniture("control_cable_drop", "north"))
        for x in range(min(xs), max(xs) + 1):
            if x == RELAY_HEADER_X:
                piece = "control_tray_cross"
            elif x in xs:
                piece = "control_tray_waterfall_hanger" if x in RELAY_HANGERS else "control_tray_waterfall"
            else:
                piece = "control_tray_hanger" if x in RELAY_HANGERS else "control_tray"
            b.p(x, tray, z, furniture(piece, "north"))
    # The header: out of the spreading room's top tray at z 11 (a tee), south over the corridor, the rows' crosses, its
    # end a tee into row D's run.
    x = RELAY_HEADER_X
    last = max(z for z, _, _ in RELAY_ROWS.values())
    # (The tee itself is the spreading room's: Train A's top tray there, spreading_room CSR_RELAY_TEE.)
    assert P.MATERIALS[int(b.get(x, tray, 11))][0].startswith("jugcraft:cable_tray_control_tee"), "the relay header's tee"
    for z in range(12, last):
        if b.get(x, tray, z) and z not in (14, 18):
            continue
        piece = "control_tray_firestop" if z in (14, 18) else "control_tray_hanger" if z in (16, 23) else "control_tray"
        b.p(x, tray, z, furniture(piece, "east"))
    b.p(x, tray, last, furniture("control_tray_tee", "south"))
    # The row signs on the west wall at eye level, at the end of the aisle each row's cabinets face (read down it).
    for row, (z, facing, _) in RELAY_ROWS.items():
        b.furniture(f"sign_relay_row_{row}", 1, y0 + 1, z - 1 if facing == "south" else z + 1, "west")
    # The lights down the aisles (the ceiling grid's places are under the trays: finish_lights finds few), the smoke
    # detectors between them.
    for z in (20, 23, 27):
        for x in (2, 6, 11, 15):
            b.p(x, tray, z, LIGHT)
    for x, z in RELAY_SMOKE:
        b.p(x, tray, z, furniture("smoke_detector", "north"))
    b.furniture("drawing_rack", 1, y0, 24, "west")
    # The sequence of events recorder: its console three wide against the east wall past rows C and D, a chair, the
    # printer beside it.
    b.row(("operator_console", 3), 18, y0, 25, "east", tops=[0, 2, 3])
    b.p(17, y0, 26, furniture("operator_chair", "east"))
    b.furniture("soe_printer", 18, y0, 28, "east")
    b.furniture("fire_extinguisher", 18, y0 + 1, 20, "east")


# The cable spreading room (punch list #15; Justin: "Full cable runs", "Colour-coded, separated", "Working sprinklers";
# tools/spreading_room.py): x 1..29, z 1..13 of the first floor (walked at y0), under the control room's boards (the
# slab over it is the control room's floor). Four rows of trays along x, each three tiers (instrument, control, power
# up from y0 + 2), in three areas: Train A (x 1..9), non-safety (11..19), Train B (21..29), the gaps between them at
# x 10 and 20. Bundles go up out of the trays into the boards over the room: (row, x, tier, way), way "s" or "n" out
# over the tray's rail into the next block that side and up, "up" straight up out of the top tier. Along the north
# wall (row 2) the boards' whole length (control bundles at even x, power at odd x), but the safety systems board's
# Train B half (x 5..7), whose cables come from Train B's area in the wrapped raceway; the electrical board (x 2..3)
# and the radiation board (x 28..29) from rows 5 and 8; the consoles (rows 8 and 11) power from the top tier.
CSR_ROWS = (2, 5, 8, 11)
CSR_TIERS = (("instr", 2), ("control", 3), ("power", 4))
CSR_ZONES = (("a", 1, 9), ("n", 11, 19), ("b", 21, 29))
CSR_GAPS = (10, 20)
CSR_DROPS = ([(2, x, "control" if x % 2 == 0 else "power", "s" if x % 2 == 0 else "up") for x in range(2, 29) if x not in (5, 6, 7, 10, 20)]
             + [(5, 2, "control", "n"), (5, 3, "control", "s"), (5, 2, "power", "up"), (5, 29, "control", "n"), (5, 28, "control", "s"),
                (5, 29, "power", "up"), (8, 2, "control", "n"), (8, 3, "control", "s"), (8, 3, "power", "up"), (8, 29, "control", "n"),
                (8, 28, "control", "s"), (8, 28, "power", "up")]
             + [(8, x, "power", "up") for x in (9, 11, 19, 21)] + [(11, x, "power", "up") for x in (14, 16)]
             + [(2, 21, "instr", "s")])                      # Train B's wrapped raceway out of its instrument tray
CSR_WRAP = (21, 5, (5, 6, 7))                                # its run along z 3 (east end, west end, its rises)
CSR_RISERS = (6, 12, 18, 24)                                 # up the north wall out of the floor into row 2's top tier
CSR_RELAY_TEE = (8, 11)                                      # the relay room's header out of Train A's top tray (control)
CSR_SPRINKLER_X = 13                                         # the riser by the door, the cross main north from it
CSR_BRANCHES = {4: (3, 28), 7: (3, 28), 10: (1, 29), 13: (1, 29)}   # the branch lines along x (z: west end, east end)


def csr_train(x):
    return next(t for t, x0, x1 in CSR_ZONES if x0 <= x <= x1)


def spreading_room(b, y0, y1):
    """The cable spreading room's fit-out (CSR_* above): the trays, their hangers and tags, the gaps' barriers; the
    bundles up into the boards; the risers; Train B's fire-wrapped raceway; the pre-action sprinkler system (the valve
    station on the riser by the door, the releasing panel, the pull station, the cross main and the branch lines); the
    lights, smoke and heat detectors on the slab; the areas' signs."""
    import spreading_room as SR
    top = y1 + 1                       # under the slab: the old suspended ceiling is gone
    b.c(1, top, 1, 29, top, 13)

    def put(x, y, z, m):
        assert b.get(x, y, z) == 0, ("spreading room", x, y, z, P.MATERIALS[int(b.get(x, y, z))][0])
        b.p(x, y, z, m)

    tv = lambda t: SR.TRAINS.index(t)
    drops = {(row, x, tier): way for row, x, tier, way in CSR_DROPS}
    ups = {(row, x) for row, x, tier, way in CSR_DROPS if way == "up"}
    for row in CSR_ROWS:
        # The trapezes about every three blocks in each area, off the columns a bundle goes straight up in (its sleeve
        # is where the rods would go) and the sprinkler main's.
        hangers = set()
        for t, x0, x1 in CSR_ZONES:
            for x in (x0 + 1, x0 + 4, x0 + 7):
                for c in (x, x - 1, x + 1):
                    if x0 <= c <= x1 and (row, c) not in ups and c != CSR_SPRINKLER_X and (row, c) != (CSR_RELAY_TEE[1], CSR_RELAY_TEE[0]):
                        hangers.add(c)
                        break
        for t, x0, x1 in CSR_ZONES:
            for x in range(x0, x1 + 1):
                h = 3 if x in hangers else 0
                for tier, k in CSR_TIERS:
                    y = y0 + k
                    kind = "control" if (row == CSR_RELAY_TEE[1] and t == "a" and tier == "power") else tier
                    if (row, x) == (CSR_RELAY_TEE[1], CSR_RELAY_TEE[0]) and tier == "power":
                        put(x, y, row, furniture("cable_tray_control_tee", "north"))
                        continue
                    way = drops.get((row, x, tier))
                    if way == "up":
                        put(x, y, row, variant_state("cable_tray_power_up", "north", tv(t)))
                    elif way:
                        put(x, y, row, variant_state(f"cable_tray_{kind}_drop", "north" if way == "s" else "south", tv(t) + h))
                    else:
                        put(x, y, row, variant_state(f"cable_tray_{kind}", "north", tv(t) + h))
                if h:
                    put(x, top, row, furniture("cable_tray_rods", "north"))
        for x in CSR_GAPS:
            for tier, k in CSR_TIERS:
                put(x, y0 + k, row, variant_state("cable_tray_gap", "north", 1 if tier == "instr" else 0))
    # The bundles: out over the rail, the elbow up, on up to the sleeve in the slab (or straight up out of the top tier).
    for (row, x, tier), way in drops.items():
        t = csr_train(x)
        y = y0 + dict(CSR_TIERS)[tier]
        if way == "up":
            put(x, top, row, variant_state("cable_bundle_sleeve", "north", tv(t)))
            continue
        if (row, x, tier) == (2, CSR_WRAP[0], "instr"):
            continue
        z = row + (1 if way == "s" else -1)
        put(x, y, z, variant_state("cable_bundle_elbow", "north" if way == "s" else "south", tv(t)))
        for k in range(y + 1, top):
            put(x, k, z, variant_state("cable_bundle", "north", tv(t)))
        put(x, top, z, variant_state("cable_bundle_sleeve", "north", tv(t)))
    # Train B's raceway to the safety systems board's Train B half, through the other areas wrapped (along z 3 at the
    # instrument tier's height), its wrapped rises up into the board.
    east, west, rises = CSR_WRAP
    wy = y0 + dict(CSR_TIERS)["instr"]
    put(east, wy, 3, furniture("fire_wrap_start", "north"))
    for x in range(west, east):
        piece = "fire_wrap_up" if x == west else "fire_wrap_tee_up" if x in rises else "fire_wrap"
        put(x, wy, 3, furniture(piece, "north"))
    for x in rises:
        for k in range(wy + 1, top):
            put(x, k, 3, furniture("fire_wrap_rise", "north"))
        put(x, top, 3, furniture("fire_wrap_sleeve", "north"))
    # The risers up the north wall out of the floor (from the switchgear and DC rooms under it) into row 2's top tier.
    for x in CSR_RISERS:
        t = tv(csr_train(x))
        put(x, y0, 1, variant_state("cable_tray_riser_floor", "north", t))
        for k in range(y0 + 1, y0 + 4):
            put(x, k, 1, variant_state("cable_tray_riser", "north", t))
        put(x, y0 + 4, 1, variant_state("cable_tray_riser_top", "north", t))
    # The pre-action sprinkler system: the valve station on its riser by the door (on the south wall), the releasing
    # panel beside it, the pull station on the door's other side; the riser up to the slab, the cross main north from
    # it, the branch lines along x (a pendent head every third block, the last at each end).
    sx = CSR_SPRINKLER_X
    b.panel(SR.VALVE, sx, y0, 13, "south", 1)
    b.panel(SR.RELEASE, sx - 1, y0, 13, "south", 1)
    put(16, y0 + 1, 13, furniture("fire_pull_station", "south"))
    put(sx - 2, y0 + 1, 13, furniture("sign_csr_preaction", "south"))
    for k in range(y0 + 2, top):
        put(sx, k, 13, furniture("sprinkler_riser", "north"))
    put(sx, top, 13, furniture("sprinkler_riser_top", "north"))
    north = min(CSR_BRANCHES)
    for z in range(north, 13):
        piece = "sprinkler_main_end" if z == north else "sprinkler_main_cross" if z in CSR_BRANCHES else "sprinkler_main"
        put(sx, top, z, furniture(piece, "north"))
    for z, (xw, xe) in CSR_BRANCHES.items():
        for x in range(xw, xe + 1):
            if x == sx:
                continue
            if x in (xw, xe):
                piece, f = "sprinkler_pipe_end", "north" if x == xe else "south"
            else:
                piece, f = ("sprinkler_pipe_head" if (x - sx) % 3 == 0 else "sprinkler_pipe"), "north"
            put(x, top, z, furniture(piece, f))
    # The lights on the slab over the aisles; the smoke and heat detectors between them.
    for z, xs in ((3, (3, 9, 15, 21, 27)), (6, (4, 8, 12, 16, 20, 24, 27)), (9, (4, 8, 12, 16, 20, 24, 27)), (12, (4, 8, 12, 16, 20, 24, 28))):
        for x in xs:
            put(x, top, z, LIGHT)
    for z, xs in ((6, (6, 14, 22)), (12, (6, 14, 22))):
        for x in xs:
            put(x, top, z, furniture("smoke_detector", "north"))
    for z, xs in ((3, (11, 17, 25)), (9, (6, 14, 22))):
        for x in xs:
            put(x, top, z, furniture("heat_detector", "north"))
    # The areas' signs, on both long walls at eye level.
    for (x, z, f), t in zip(((3, 1, "north"), (15, 1, "north"), (21, 1, "north"), (4, 13, "south"), (18, 13, "south"), (26, 13, "south")),
                            ("a", "n", "b", "a", "n", "b")):
        put(x, y0 + 1, z, furniture(f"sign_csr_train_{t}", f))


# The switchgear room (older rooms round 1, tools/switchgear_room.py; Justin: "Real 480 V switchgear", "Overhead power
# tray", "One-line and signs", "Breaker handling gear", "Lights, detectors, cooling", "Log desk"). Building
# coordinates: the room x 21..29, z 1..13 on the ground floor (y 1 walked on, the ceiling at y 6); LC 1A (Train A)
# along the north wall (x 22..29, z 1..2, its front south), LC 1B (Train B) across the aisle (z 9..10, its front north).
SGR_TRAY_Y = 5                                                # the trays and the duct under the ceiling
SGR_TRAYS = {"a": (1, (28, 27, 26, 25, 24, 23), {25, 26, 27, 28}, {28, 25}),       # z, the run from its east end, drops, hangers
             "b": (10, (26, 25, 24, 23), {23, 24, 25, 26}, {26, 23})}                # (hangers only at drops: single tier)
SGR_RISE_X = 22                                               # both runs' ends: up through the ceiling
SGR_WRAP_X = 7                                                # LC 1A's wrapped run west to Train A's row 2 power tray
SGR_LIGHTS = [(23, 9), (27, 9), (23, 12), (26, 12)]           # (the ceiling grid lights the rest)
SGR_SMOKE = [(24, 7), (25, 12), (25, 3)]
SGR_FCU = (28, 12)                                            # the fan coil unit's west half (its discharge north)
SGR_DUCT_X, SGR_DUCT_Z = 28, 5                                # the supply duct north from it, then west along the aisle
SGR_REGISTERS = (26, 23)
SGR_DANGER_X = 23                                             # the DANGER sign on the corridor wall, west of the doors


def switchgear_room(b, L1):
    """The switchgear room's fit-out (SGR_* above) and its trays' way up through the cable spreading room (L1 its
    floor slab: the spreading room walked on at L1 + 1)."""
    y, ty, c0 = 1, SGR_TRAY_Y, L1 + 1

    def put(x, yy, z, m):
        assert b.get(x, yy, z) == 0, ("switchgear room", x, yy, z, P.MATERIALS[int(b.get(x, yy, z))][0])
        b.p(x, yy, z, m)
    # The trays over each lineup's back (variant: the train, + 3 with a hanger), a drop down into each feeder
    # section's gland plate, the run's end up through the ceiling.
    for t, (z, run, drops, hangers) in SGR_TRAYS.items():
        v = "ab".index(t)
        for x in run:
            piece = "cable_tray_power_down" if x in drops else "cable_tray_power"
            put(x, ty, z, variant_state(piece, "north", v + (3 if x in hangers else 0)))
            if x in drops:
                put(x, ty - 1, z, variant_state("lc_cable_drop", "north", v))
        put(SGR_RISE_X, ty, z, variant_state("cable_tray_power_ceiling", "north", v))
    # Over it, in the spreading room: LC 1B's cables up a riser (as the room's own, its ladder on the run's north
    # side) into its row's Train B power tray; LC 1A's up wrapped, west under the slab out of the Train B area, and down
    # into Train A's row 2 power tray.
    zb = SGR_TRAYS["b"][0]
    put(SGR_RISE_X, c0, zb, variant_state("cable_tray_riser_floor", "north", 1))
    for k in range(c0 + 1, c0 + 4):
        put(SGR_RISE_X, k, zb, variant_state("cable_tray_riser", "north", 1))
    put(SGR_RISE_X, c0 + 4, zb, variant_state("cable_tray_riser_top", "north", 1))
    za, top = SGR_TRAYS["a"][0], c0 + 5
    put(SGR_RISE_X, c0, za, furniture("fire_wrap_floor", "north"))
    for k in range(c0 + 1, top):
        put(SGR_RISE_X, k, za, furniture("fire_wrap_rise", "north"))
    put(SGR_RISE_X, top, za, furniture("fire_wrap_elbow", "north"))
    for x in range(SGR_RISE_X - 1, SGR_WRAP_X, -1):
        put(x, top, za, furniture("fire_wrap_hanger" if (SGR_RISE_X - x) % 3 == 0 else "fire_wrap", "north"))
    put(SGR_WRAP_X, top, za, furniture("fire_wrap_drop", "north"))
    # The cooling: the fan coil unit over the rear aisle, its supply duct north beside LC 1B's transformer and west
    # along the aisle (two registers), closed at its west end.
    fx, fz = SGR_FCU
    put(fx, ty, fz, furniture("sgr_fan_coil", "north"))
    put(fx + 1, ty, fz, furniture("sgr_fan_coil_end", "north"))
    for z in range(fz - 1, SGR_DUCT_Z, -1):
        put(SGR_DUCT_X, ty, z, furniture("sgr_duct_hanger" if (fz - z) % 3 == 2 else "sgr_duct", "north"))
    put(SGR_DUCT_X, ty, SGR_DUCT_Z, furniture("hv_duct_elbow", "south"))
    for x in range(SGR_DUCT_X - 1, 22, -1):
        piece = "sgr_duct_register" if x in SGR_REGISTERS else "sgr_duct_hanger" if x == 25 else "sgr_duct"
        put(x, ty, SGR_DUCT_Z, furniture(piece, "east"))
    put(22, ty, SGR_DUCT_Z, furniture("sgr_duct_end", "west"))
    # The lights over LC 1B's front and the rear aisle, the smoke detectors.
    for x, z in SGR_LIGHTS:
        put(x, ty, z, LIGHT)
    for x, z in SGR_SMOKE:
        put(x, ty, z, furniture("smoke_detector", "north"))
    # The handling gear at the aisle's east end: the remote racking device, the spare breaker on its lift truck, the
    # test set.
    put(29, y, 4, furniture("remote_racking_device", "east"))
    put(29, y, 5, furniture("breaker_lifter", "east"))
    put(29, y + 1, 5, furniture("breaker_lifter_top", "east"))
    put(29, y, 7, furniture("breaker_test_set", "east"))
    # The log desk against the south wall west of the door (phone, logbook, laptop from its left), the one-line diagram
    # over it; the lineups' signs at the aisle's west end; the DANGER sign outside by the door.
    for k, x in enumerate((24, 23)):
        put(x, y, 13, variant_state("sgr_log_desk", "south", k))
    put(22, y, 13, multi_state("sgr_desk_laptop", "south", 0))
    for c in range(3):
        for row in range(2):
            put(24 - c, y + 1 + row, 13, multi_state("sgr_one_line", "south", row * 3 + c))
    put(21, y + 2, 3, furniture("sign_lc_a", "west"))
    put(21, y + 2, 8, furniture("sign_lc_b", "west"))
    put(SGR_DANGER_X, y + 1, 15, furniture("sign_switchgear_danger", "north"))


def spreading_room_slab(b, y):
    """Over the cable spreading room the control room's floor (y) shows its underside: the bare slab, painted (its top
    the control room's carpet or access floor as laid)."""
    slab = {CARPET: M("jugcraft:carpet_floor_slab", (200, 200, 196)), ACCESS_FLOOR: M("jugcraft:access_floor_slab", (200, 200, 196))}
    for x in range(1, 30):
        for z in range(1, 14):
            k = b.get(x, y, z)
            assert k in slab, ("spreading room slab", x, y, z, P.MATERIALS[int(k)][0])
            b.p(x, y, z, slab[k])


def first_floor(b, W, D, y):
    """The first floor (walked at y + 1): the cable spreading room under the control room (tiers of cable tray, risers
    up into the floor above), and south of the corridor the relay room and the plant computer room on its raised floor."""
    y0, y1 = y + 1, y + 5
    b.wall_x(14, 1, W - 10, y0, y1)
    b.wall_x(18, 1, W - 2, y0, y1)
    b.wall_z(30, 1, 13, y0, y1)
    b.wall_z(19, 19, D - 2, y0, y1)
    # ---- Cable spreading room (x 1..29, z 1..13; punch list #15: spreading_room).
    spreading_room(b, y0, y1)
    b.door(15, y0, 14, "north", double=True, label="cable_spreading_room")
    b.furniture("fire_extinguisher", 1, y0 + 1, 12, "west")
    # ---- Relay room (x 1..18, z 19..28): its cabinets back to back in rows (round 26, #10: relay_room).
    relay_room(b, y0, y1)
    b.door(9, y0, 18, "south", double=True, label="relay_room")
    # ---- Plant computer room (x 20..38, z 19..28): older rooms round 2 (computer_room, tools/server_room.py).
    computer_room(b, W, D, y, y0, y1)
    b.door(29, y0, 18, "south", double=True, label="server_room")
    b.furniture("fire_extinguisher", W - 2, y0 + 1, 24, "east")


# The plant computer room (older rooms round 2; Justin, 15 October 2026: "Real 3D IT racks", "Rack lights show
# traffic", "Plant computer console", "UPS and power", "Room cooling", "Clean-agent fire system", "Overhead cable
# ladder", "Cooled floor and CRACs", "Signs and labels"). x 20..38, z 19..28 on the raised floor; the door at x 29..30
# in the north wall. Four rows of twelve racks (A and B back to back, C and D back to back) with a cross aisle at x
# 29..30, their fronts on the cold aisles (perforated tiles); the ladder and fiber runway over each row, the cross
# ladder at their west end with the plant's cables rising into it; the CRACs at the west wall at the cold aisles'
# ends, the console in the north-west corner; the UPS lineup on the east wall, the agent cylinders beside it, their
# pipe under the ceiling over the north and south aisles.
SRV_ROWS = (("A", 21, "south"), ("B", 22, "north"), ("C", 25, "south"), ("D", 26, "north"))
SRV_X = tuple(range(23, 29)) + tuple(range(31, 37))
SRV_KINDS = {"A": "PPNSSG SSNSGS", "B": "SSNSGS PNSSSG", "C": "GSSNSS SGNSSS", "D": "SSGNSS SSNGSP"}
SRV_KEY = {"P": "ppc_rack", "N": "network_rack", "S": "server_rack", "G": "san_rack"}
SRV_COLD = (20, 23, 24, 27)                     # the perforated tiles' rows (in front of the racks)
SRV_HANGERS = (23, 26, 29, 32, 35)              # the ladders' rods
SRV_LADDER_X = 22                               # the cross ladder; the riser up into it against row B's west end
SRV_RISER = (22, 22)
SRV_UPS = (("ups_battery", 19), ("ups_battery", 20), ("ups_module", 21), ("ups_bypass", 22), ("pdu_cabinet", 23))
SRV_CYL_Z = (26, 27, 28)
SRV_NOZZLES = (34, 26)                          # x of the nozzles on each run; the runs end at x 25
SRV_PIPE_Z = (20, 27)
SRV_SMOKE = ((23, 23), (26, 24), (29, 23), (32, 24), (35, 23))
PERF_TILE = M("jugcraft:access_floor_perforated", (176, 180, 182))


def computer_room(b, W, D, y, y0, y1):
    put = b.p
    b.b(20, y, 19, W - 2, y, D - 2, ACCESS_FLOOR)
    for z in SRV_COLD:
        for x in SRV_X:
            put(x, y, z, PERF_TILE)
    # The racks.
    for row, z, facing in SRV_ROWS:
        kinds = SRV_KINDS[row].replace(" ", "")
        for x, k in zip(SRV_X, kinds):
            b.panel(SRV_KEY[k], x, y0, z, facing, 1)
        # The row's sign on its east end.
        b.furniture(f"sign_srv_row_{row.lower()}", SRV_X[-1] + 1, y0 + 1, z, "west")
    # Over each row its ladder and fiber runway (y1 - 1): a drop into every rack, a fiber spill-out into the network and
    # process computer racks, rods every three; plain over the cross aisle.
    ly = y1 - 1
    for row, z, facing in SRV_ROWS:
        kinds = dict(zip(SRV_X, SRV_KINDS[row].replace(" ", "")))
        for x in range(SRV_LADDER_X + 1, SRV_X[-1] + 1):
            v = (1 if x in SRV_HANGERS else 0)
            if x in kinds:
                v |= 2 | (4 if kinds[x] in "PN" else 0)
            put(x, ly, z, variant_state("srv_ladder", facing, v))
    # The cross ladder at their west end (its ladder on its east side, against the rows' ends), the riser up into it.
    for z in range(21, 27):
        put(SRV_LADDER_X, ly, z, variant_state("srv_ladder_cross", "east", 1 if z == SRV_RISER[1] else 0))
    rx, rz = SRV_RISER
    for k in range(y0, ly):
        put(rx, k, rz, variant_state("srv_ladder_riser", "east", 1 if k == y0 else 0))
    # The CRACs at the west wall (CRAC-1 at the middle cold aisle, CRAC-2 at the south one), the console in the corner.
    b.panel("crac_unit", 20, y0, 24, "west", 2, "CRAC-1")
    b.panel("crac_unit", 20, y0, 28, "west", 2, "CRAC-2")
    b.panel("ppc_console", 20, y0, 19, "north", 3)
    # The UPS lineup on the east wall; the agent cylinders beside it, their manifold and riser, the pipe under the
    # ceiling: north along x 37 and west along each run to its nozzles.
    for key, z in SRV_UPS:
        b.panel(key, W - 2, y0, z, "east", 1)
    for z in SRV_CYL_Z:
        b.panel("agent_cylinder", W - 2, y0, z, "east", 1)
        put(W - 2, y0 + 2, z, variant_state("agent_manifold", "east", 1 if z == SRV_CYL_Z[1] else 2 if z == SRV_CYL_Z[-1] else 0))
    put(W - 2, y0 + 3, SRV_CYL_Z[1], furniture("agent_pipe_riser", "east"))
    put(W - 2, y1, SRV_CYL_Z[1], furniture("agent_pipe_riser_top", "east"))
    zs, zn = SRV_PIPE_Z[1], SRV_PIPE_Z[0]
    put(W - 3, y1, zs, furniture("agent_pipe_tee", "south"))
    for z in range(zn + 1, zs):
        put(W - 3, y1, z, furniture("agent_pipe", "east"))
    put(W - 3, y1, zn, furniture("agent_pipe_elbow", "north"))
    for z in SRV_PIPE_Z:
        for x in range(SRV_NOZZLES[-1] - 1, W - 3):
            if x == SRV_NOZZLES[-1] - 1:
                piece = variant_state("agent_pipe_nozzle", "north", 1)
            elif x in SRV_NOZZLES:
                piece = variant_state("agent_pipe_nozzle", "north", 0)
            else:
                piece = furniture("agent_pipe", "north")
            put(x, y1, z, piece)
    for x, z in SRV_SMOKE:
        put(x, y1, z, furniture("smoke_detector", "north"))
    # The releasing panel, the abort and manual release stations, the horn and strobe by the door; the signs.
    b.panel("agent_release_panel", 32, y0, 19, "north", 1)
    b.furniture("agent_abort_station", 31, y0 + 1, 19, "north")
    b.furniture("agent_manual_release", 28, y0 + 1, 19, "north")
    b.panel("agent_horn_strobe", 31, y1 - 1, 19, "north", 1)
    b.furniture("sign_agent_vacate", 27, y0 + 1, 19, "north")
    b.furniture("sign_no_food", 34, y0 + 1, 19, "north")
    b.furniture("sign_clean_agent", 31, y0 + 1, 17, "south")
    for x in range(21, W - 1, 3):
        for z in (19, 23, 24, 28):
            if b.get(x, y1, z) == 0:
                put(x, y1, z, LIGHT)


# The control room's fittings (older rooms round 5; tools/cr_room.py): the At The Controls area's line across the floor
# between the operators' consoles and the supervisor's, its sign on a post at each end; the master clocks over the
# boards' ends; the SCBA cabinet and an extinguisher by the door, the drawing rack beside the procedures, the alarm printer by
# the supervisor's console; smoke detectors and emergency lights; in the shift manager's office the EAL chart over his
# files and the key locker.
CR_ATC_Z = 10
CR_SMOKE = [(7, 7), (16, 7), (25, 7), (12, 14), (24, 14), (5, 14)]


def control_room_fittings(b, y0, top):
    def put(x, y, z, m):
        assert b.get(x, y, z) == 0, ("control room", x, y, z, P.MATERIALS[int(b.get(x, y, z))][0])
        b.p(x, y, z, m)
    for x in range(5, 27):
        put(x, y0, CR_ATC_Z, furniture("cr_atc_line", "north"))
    for x in (4, 27):
        put(x, y0, CR_ATC_Z, furniture("cr_atc_sign", "north"))
    b.panel("ops_wall_clock", 7, y0 + 4, 2, "north", 1)
    b.panel("ops_wall_clock", 24, y0 + 4, 2, "north", 1)
    for row in range(2):
        put(17, y0 + row, 16, multi_state("cr_scba_cabinet", "south", row))
    put(18, y0 + 1, 16, furniture("fire_extinguisher", "south"))
    put(28, y0, 16, furniture("drawing_rack", "south"))
    put(17, y0, 11, furniture("soe_printer", "north"))
    for x, z in CR_SMOKE:
        put(x, top, z, furniture("smoke_detector", "north"))
    b.furniture("emergency_light", 13, y0 + 4, 16, "south")
    b.furniture("emergency_light", 29, y0 + 4, 15, "east")
    rx, rz = P.DIRS[P.CLOCKWISE["south"]]
    for c in range(2):
        put(3 + rx * c, y0 + 2, 16, multi_state("cr_eal_chart", "south", c))
    put(6, y0 + 1, 16, furniture("ops_key_locker", "south"))


def second_floor(b, W, D, y):
    """The control room floor (walked at y + 1, a raised floor throughout): the main control room north of the corridor,
    the shift supervisor's glazed office in its corner; south of the corridor the Technical Support Center, the kitchen,
    the restroom and the operations office. Returns where the reactor console stands (x, z)."""
    y0 = y + 1
    top = y + 7
    # The control room's walls: acoustic panels inside; the corridor (z 18..20) along its south side.
    b.wall_x(17, 1, 30, y0, top, ACOUSTIC)
    b.wall_z(30, 1, 16, y0, top, ACOUSTIC)
    b.wall_x(21, 1, W - 2, y0, top)
    for x in range(1, 30):
        for k in range(y0, top + 1):
            if b.get(x, k, 1) == 0:
                b.p(x, k, 1, ACOUSTIC)
    for z in range(1, 17):
        for k in range(y0, top + 1):
            if b.get(1, k, z) == 0:
                b.p(1, k, z, ACOUSTIC)
    b.b(2, y, 2, 29, y, 16, CARPET)
    # The horseshoe (round 23; Justin: the reactor console "in line with the boards, as a real control board", the
    # boards varied "like a real control room"), laid out as a Westinghouse four-loop plant's: along the north wall the
    # safety systems, the reactor coolant system, the reactor control console in the middle (the working one), steam
    # and feedwater, the turbine and generator; down the west side the electrical board, down the east side radiation
    # monitoring and ventilation. The plant display wall over the middle; the operators' consoles in front.
    for kind, x in (("esf_control_board", 2), ("rcs_control_board", 8), ("secondary_control_board", 17),
                    ("turbine_control_board", 23)):
        b.prop(kind, x, y0, 2, "north")
    console = (14, 2)
    b.prop("electrical_control_board", 2, y0, 4, "west")
    b.prop("radiation_control_board", 28, y0, 4, "east")
    b.prop("plant_display_wall", 11, y0 + 3, 2, "north")
    # The operators' consoles (Justin: black ones, "at least three blocks each ... like little setups"): the reactor
    # operator's in front of the reactor side of the boards, the balance of plant operator's in front of the steam and
    # turbine side, the senior reactor operator's behind them in the middle, looking over the whole horseshoe.
    # (Older rooms round 5, tools/cr_room.py: each a control room console of its own, one position each, its three
    # screens the plant process computer's, read live from the display wall; the chair in the middle.)
    import cr_room as CR
    for (x, z), label in zip(((9, 8), (19, 8), (14, 11)), CR.POSITIONS):
        b.panel("cr_console", x, y0, z, "north", 3, label=label)
        b.p(x + 1, y0, z + 1, furniture("operator_chair", "north"))
    # The procedures along the back wall (older rooms round 5: the control room's procedure racks, a section each), the
    # plant overview screens on the east wall, and the office copier the administration building has (Justin: "use
    # that printer").
    for k, x in enumerate(range(20, 28)):
        b.panel("cr_procedure_rack", x, y0, 16, "south", 1, label=CR.PR_LABELS[k])
    wall_screen(b, "plant_map_screen", 29, y0 + 1, 11, "east")
    import medium_admin
    assert medium_admin.copier(b, 11, y0, 16, "south")
    control_room_fittings(b, y0, top)
    # The shift supervisor's office in the south-west corner: a half wall glazed over, so he sees the boards (framed
    # glass, one pane from wall to wall), round the corner to his office's east wall.
    for x in range(2, 10):
        b.p(x, y0, 12, ACOUSTIC)
        for k in (y0 + 1, y0 + 2):
            b.p(x, k, 12, FGLASS["north"])
        for k in range(y0 + 3, top + 1):
            b.p(x, k, 12, ACOUSTIC)
    for z in range(13, 17):
        for k in range(y0, top + 1):
            b.p(9, k, z, ACOUSTIC)
    b.door(9, y0, 14, "west", label="shift_supervisor")
    # The shift supervisor's executive desks along his glass, looking out over the boards (moved in from the control
    # room floor, Justin's round 23): a phone and lamp, his two screens, the blotter.
    # (Older rooms round 5: his own desk, tools/cr_room.py ss_desk: the plant's status and its alarms on his screens, the
    # ENS phone, the emergency plan's classification binder, the shift log.)
    b.panel("ss_desk", 4, y0, 13, "north", 3)
    b.p(5, y0, 14, furniture("operator_chair", "north"))
    b.furniture("filing_cabinet", 2, y0, 16, "west")
    b.furniture("filing_cabinet", 2, y0, 15, "west")
    b.plant(8, y0, 16)
    # The control room's lighting: a close grid of panels in its ceiling.
    for x in range(3, 30, 3):
        for z in range(3, 17, 3):
            if b.get(x, top, z) == 0 and b.get(x, top + 1, z) == CEIL:
                b.p(x, top, z, LIGHT)
    b.door(15, y0, 17, "north", double=True, label="main_control_room")
    # ---- The corridor (z 18..20) and the stairs' landing (x 31..38, z 15..17).
    b.furniture("fire_extinguisher", 1, y0 + 1, 19, "west")
    b.furniture("water_cooler", W - 2, y0, 19, "east")
    # ---- South of the corridor, with windows: the Technical Support Center (x 1..18), the kitchen (x 20..24),
    # the restroom (x 26..29), the operations office (x 31..38).
    for x in (19, 25, 30):
        b.wall_z(x, 22, D - 2, y0, top)
    tsc(b, y, y0, top, D)
    # Kitchen (round 26, #12: the TSC took its west half): the counters under the windows, the fridge, a table.
    b.row(["kitchen_counter", "kitchen_sink", "coffee_counter", "microwave_counter", "kitchen_counter"], 24, y0, D - 2, "south")
    b.stack("fridge", 24, y0, 22, "north", n=2)
    b.varied("cafe_table", 22, y0, 25, "north", 6)
    b.p(21, y0, 25, furniture("cafe_chair", "east"))
    b.p(23, y0, 25, furniture("cafe_chair", "west"))
    b.door(22, y0, 21, "south", double=True, label="break_room")
    restroom(b, y, y0, D)
    operations_office(b, y, y0, W, D)
    return console


# The restroom (round 26, #14; tools/restroom.py; Justin: "2 stalls, urinal, 2 sinks", ceramic tile): x 26..29 of the
# second floor's south side, its door from the corridor at x 27. Tiled walls to 2 m (the lower course with its cove
# base, the upper with the accent band and bullnose cap), a grey tile floor with a drain in front of the urinal,
# frosted windows. Along the west wall the trash can under the towel dispenser, the two sinks with their mirrors, the
# hand dryer, the baby changing station (Justin: no mop bucket or wet floor sign); on the east wall the urinal and its
# screen, then the stalls (toilets against the wall): the standard stall, the accessible stall in the corner, their
# fronts on the aisle's east edge (the doors swing out into the aisle) under the headrail.
def restroom(b, y, y0, D):
    def st(text):
        return M("jugcraft:" + text, (230, 230, 226))
    b.b(26, y, 22, 29, y, D - 2, st("restroom_floor_tile"))
    b.p(28, y, 24, furniture("restroom_floor_drain", "north"))
    for z in range(22, D - 1):
        for x, face in ((25, "east"), (30, "west")):
            b.p(x, y0, z, st(f"restroom_wall_tile[back=painted,facing={face}]"))
            b.p(x, y0 + 1, z, st(f"restroom_wall_tile_cap[back=painted,facing={face}]"))
    for x in (26, 28, 29):
        b.p(x, y0, 21, st("restroom_wall_tile[back=painted,facing=south]"))
        b.p(x, y0 + 1, 21, st("restroom_wall_tile_cap[back=painted,facing=south]"))
    for x in range(26, 30):
        b.p(x, y0, D - 1, st("restroom_wall_tile[back=concrete,facing=north]"))
        for k in range(y0 + 1, y0 + 5):
            if b.get(x, k, D - 1) == PANE:
                b.p(x, k, D - 1, FROSTED)
    # The west wall.
    b.p(26, y0, 22, st("restroom_trash_can[facing=west,fill=1]"))
    b.p(26, y0 + 1, 22, furniture("restroom_towel_dispenser", "west"))
    for z in (23, 24):
        b.p(26, y0, z, st("restroom_sink[facing=west,running=false]"))
        b.p(26, y0 + 1, z, furniture("restroom_mirror", "west"))
    b.p(26, y0 + 1, 25, st("restroom_hand_dryer[facing=west,running=false]"))
    b.p(26, y0, 26, furniture("restroom_changing_station", "west"))
    # The urinal and its screen; the signs on the north wall.
    b.p(29, y0, 23, st("restroom_urinal[facing=east,flushing=false]"))
    b.p(29, y0, 24, furniture("restroom_urinal_screen", "east"))
    b.p(28, y0 + 1, 22, furniture("sign_wash_hands", "north"))
    b.p(29, y0 + 1, 22, furniture("sign_no_smoking", "north"))
    # The stalls: partitions (the standard stall's north side, between the stalls), the toilets, the fronts.
    for z, face, vs in ((25, "south", (4, 1)), (27, "north", (3, 2))):
        for x, v in zip((28, 29), vs):
            b.p(x, y0, z, variant_state("restroom_stall_partition", face, v))
    b.p(29, y0, 26, st(f"restroom_toilet[facing=east,flushing=false,paper={RR_PAPER},variant=0]"))
    b.p(29, y0, D - 2, st(f"restroom_toilet[facing=east,flushing=false,paper={RR_PAPER},variant=1]"))
    for z, door in ((26, "restroom_stall_door"), (27, "restroom_stall_door_accessible")):
        for k, half in ((0, "lower"), (1, "upper")):
            b.p(27, y0 + k, z, st(f"{door}[facing=west,half={half},hinge=right,latched=false,open=false,powered=false]"))
    b.p(27, y0, D - 2, furniture("restroom_stall_panel", "east"))
    for z, v in ((26, 1), (27, 0), (D - 2, 2)):
        b.p(27, y0 + 2, z, variant_state("restroom_stall_headrail", "east", v))
    # The exhaust grille in the ceiling over the urinal's floor.
    b.p(28, y0 + 7, 24, furniture("restroom_exhaust_grille", "north"))
    b.door(27, y0, 21, "south", label="restroom")


# The operations office (round 26, #13; tools/ops_office.py; Justin: "Work control + tagout"): x 31..38 of the second
# floor's south side, its door from the corridor at x 34. The work control centre: on the east wall the equipment
# out-of-service board and the plant status screen, the two work control consoles facing them (the work control SRO's,
# the tagout coordinator's); on the north wall the shift schedule whiteboard over the copier and files, the master
# clock over the door, the radio charging rack and the PPE shelf; on the west wall the locked valve key locker, the
# clearance tag rack and the group lockbox over the files, the procedure shelves.
def operations_office(b, y, y0, W, D):
    import ops_office as O
    b.b(31, y, 22, W - 2, y, D - 2, CARPET)
    # The east wall: the out-of-service board, the status screen; the consoles facing them, their chairs.
    b.panel("ops_oos_board", 38, y0 + 2, 23, "east", 3)
    wall_screen(b, "status_screen", 38, y0 + 2, 26, "east")
    for z, v in ((23, 0), (26, 1)):
        b.panel("ops_console", 36, y0, z, "east", 3, label=O.PANELS["ops_console"]["labels"][v])
        for dz in (0, 2):
            b.p(35, y0, z + dz, furniture("operator_chair", "east"))
    # The north wall: the schedule board over the files and the copier, the clock over the door, the radios and the PPE.
    b.panel("ops_schedule_board", 31, y0 + 1, 22, "north", 3)
    for x in (31, 32):
        b.furniture("filing_cabinet", x, y0, 22, "north")
    b.furniture("printer", 33, y0, 22, "north")
    b.panel("ops_wall_clock", 34, y0 + 2, 22, "north", 1)
    b.panel("ops_radio_rack", 35, y0 + 1, 22, "north", 1)
    b.panel("ops_ppe_shelf", 36, y0 + 1, 22, "north", 2)
    b.furniture("filing_cabinet", W - 2, y0, 22, "north")
    b.furniture("fire_extinguisher", W - 2, y0 + 1, 22, "north")
    # The west wall: the key locker, the tag rack and the lockbox over the files; the procedure shelves; a plant.
    for z, name in ((24, "ops_key_locker"), (25, "ops_tag_rack"), (26, "ops_lockbox")):
        b.p(31, y0 + 1, z, furniture(name, "west"))
    for z in (24, 25):
        b.furniture("filing_cabinet", 31, y0, z, "west")
    for z in (27, 28):
        for k in range(2):
            b.furniture("ops_procedure_shelf", 31, y0 + k, z, "west")
    b.plant(33, y0, D - 2)
    b.p(33, y0 + 6, 25, furniture("smoke_detector", "north"))
    b.door(34, y0, 21, "south", label="operations")


# The Technical Support Center (round 26, #12; tools/tsc_room.py): x 1..18 of the second floor's south side, its door
# from the corridor at x 12..13. The video wall and the emergency classification board on the west wall; three rows of
# consoles facing them, each two consoles (z 26..28 and 22..24) either side of the aisle at z 25; the command desk behind
# them; the NRC's room in the north-east corner behind a glazed partition; the communications console under the
# windows in the south-east corner; the status boards over the consoles' ends on the north wall; the procedure shelves
# and the flat file of drawings by the front wall; the frisker and the area radiation monitor inside the door, the card
# reader outside it.
TSC_ROWS = [(4, 28, 0), (4, 24, 1), (7, 28, 2), (7, 24, 3), (10, 28, 4), (10, 24, 5), (13, 28, 6)]


def tsc(b, y, y0, top, D):
    import tsc_room as T
    b.b(1, y, 22, 18, y, D - 2, CARPET)
    # The video wall (its displays two blocks square, clear of the procedure shelves), the classification board over it.
    for z, label in ((28, "SPDS"), (26, "RAD MET"), (24, "ELEC EVENTS")):
        b.panel("tsc_video_wall", 1, y0 + 1, z, "west", 2, label=label)
    b.panel("tsc_class_board", 1, y0 + 3, 26, "west", 3)
    # The consoles, their positions' chairs.
    for x, z, v in TSC_ROWS:
        b.panel("tsc_console", x, y0, z, "west", 3, label=T.PANELS["tsc_console"]["labels"][v])
        for dz in (0, -2):
            b.p(x + 1, y0, z + dz, furniture("operator_chair", "west"))
    # The NRC's room: a solid pier with its sign, its glass door, the glazed partition round it; the site team's desk
    # (its monitors and phones), chairs, and their board on the wall.
    for k in range(y0, top + 1):
        b.p(15, k, 22, PAINTED)
        b.p(15, k, 23, PAINTED) if k > y0 + 1 else None
        for x, z, f in ((15, 24, "east"), (15, 25, "east"), (16, 25, "north"), (17, 25, "north"), (18, 25, "north")):
            b.p(x, k, z, FGLASS[f] if k <= y0 + 2 else PAINTED)
    b.door(15, y0, 23, "east", kind="glass")
    b.p(14, y0 + 2, 22, sign("nrc_room", "west"))
    b.panel("tsc_nrc_desk", 18, y0, 22, "east", 3)
    for z in (22, 23, 24):
        b.p(17, y0, z, furniture("operator_chair", "east"))
    b.panel("tsc_status_board", 16, y0 + 2, 22, "north", 3, label="NRC")
    # The communications console under the windows.
    b.panel("tsc_comms_console", 18, y0, 28, "south", 3)
    for x in (16, 17, 18):
        b.p(x, y0, 27, furniture("operator_chair", "south"))
    b.furniture("printer", 15, y0, 28, "south")
    # The status boards over the consoles' ends.
    for x, label in ((2, "STAFFING"), (5, "PRIORITIES"), (8, "EVENTS")):
        b.panel("tsc_status_board", x, y0 + 2, 22, "north", 3, label=label)
    # Records: the procedure shelves by the front wall, the flat file of drawings under the windows.
    for x in (2, 3):
        for k in range(2):
            b.furniture("tsc_procedure_shelf", x, y0 + k, 22, "north")
        b.furniture("tsc_plan_file", x, y0, D - 2, "south")
    # At the door: the frisker, the area radiation monitor; outside it, the accountability card reader.
    b.panel("tsc_frisker", 14, y0, 22, "north", 1)
    b.panel("tsc_area_monitor", 11, y0 + 1, 22, "north", 1)
    b.p(14, y0 + 1, 20, furniture("tsc_card_reader", "south"))
    b.furniture("fire_extinguisher", 18, y0 + 1, 26, "east")
    b.door(12, y0, 21, "south", double=True, label="support_center")


# The Control Room HVAC Equipment Room (round 26, #11; Justin: "Control Room HVAC Equipment Room", all four kinds of
# machine, "Switch to emergency mode", "Real ducts with dampers"): the top floor (walked at y 24, the roof's underside at
# y 30). Each train's outside air comes in through its louvres in the north wall into its intake plenum; normally
# through its normal outside air damper (CR-D-1) along the overhead duct down into its air handler's mixing box; in
# emergency (the HVAC panel's AUTO on a reactor trip or high temperature) that damper shuts, the emergency intake
# damper (CR-D-2) opens into the filtration unit, its booster fan starts and its outlet damper (CR-D-3) opens, the
# filtered air going on up and along into the same mixing box. The control room's return air comes up through the
# floor into the air handler's front; its supply goes out of its far end, up, along and down through the floor at two
# ceiling diffusers. Chilled water: the return main from each air handler's coil back to its pump, through its chiller,
# the supply main back to the coil. Train A west, B east (B's machines turned the same way, their operating sides east).
HVAC_TRAINS = {
    # efu / ahu: the footprint's corner (x0, z0) facing south; plenum: its x span; the louvres', the dampers', the runs'
    # cells are worked out from these.
    "A": {"efu": (2, 3), "ahu": (8, 3), "plenum": (2, 7), "detector": 6, "door": 4, "rad": 8, "tox": 1, "pump": (10, 16),
          "chiller": (6, 18), "variant": 0},
    "B": {"efu": (25, 3), "ahu": (20, 3), "plenum": (24, 29), "detector": 25, "door": 27, "rad": 23, "tox": 30, "pump": (18, 16),
          "chiller": (20, 18), "variant": 1},
}
HVAC_LOUVRES = [range(2, 8), range(24, 30)]
HVAC_PANEL = (14, 1)


def hvac_runs(t):
    """Train t's ducts and pipes: (family, runs, special) for hvac_room.lay: each run a list of cells with the faces
    it opens on at either end (into a machine, the floor), special the blocks that are a particular piece."""
    import hvac_room as H
    c = HVAC_TRAINS[t]
    y0 = 24
    ex, ez = c["efu"]
    ax_, az = c["ahu"]
    px0, px1 = c["plenum"]
    efu_in = ex + 1                        # the filtration unit's inlet and outlet column (its middle)
    ahu_c = ax_ + 1                        # the air handler's middle column
    chw_x = ax_                            # its coil's chilled water risers (its west column, z 6 supply, 7 return)
    oa_x = px1 if ahu_c > px1 else px0     # the plenum's top outlet: at its end toward the air handler
    side = 1 if ahu_c > oa_x else -1       # which way the mixed air duct runs from the tee to the air handler
    damp = {"A": (0, 2, 4), "B": (1, 3, 5)}[t]
    duct, pipe = [], []
    special = {}
    # Normal outside air: up out of the plenum's top, its damper, the tee, along to the air handler and down into it.
    oa = [(oa_x, 28, 1), (oa_x, 28, 2), (oa_x, 28, 3)] + [(x, 28, 3) for x in range(oa_x + side, ahu_c + side, side)] + [(ahu_c, 27, 3)]
    duct.append((oa, "d", "d"))
    # (Open on train A, which runs; shut on train B, on standby: as the HVAC panel sets them, nuclear/HvacRoom.java.)
    special[(oa_x, 28, 2)] = ("hv_damper", None, 2 * damp[0] + (1 if t == "A" else 0))
    special[(oa_x + side, 28, 3)] = ("hv_duct_label_oa", "e" if side > 0 else "w", None)
    # The filtered air: up out of the filtration unit's fan, its outlet damper, north and along into the tee.
    fz = ez + 5
    fa = [(efu_in, 27, fz)] + [(efu_in, 28, z) for z in range(fz, 2, -1)]
    step = 1 if oa_x > efu_in else -1
    fa += [(x, 28, 3) for x in range(efu_in + step, oa_x, step)]
    duct.append((fa + [(oa_x, 28, 3)], "d", None))
    special[(efu_in, 28, fz - 1)] = ("hv_damper", None, 2 * damp[2])
    special[(efu_in, 28, fz - 2)] = ("hv_duct_label_filtered", "n", None)
    special[(efu_in, 28, fz - 3)] = ("hv_duct_hanger", None, None)
    special[(efu_in + step, 28, 3)] = ("hv_duct_label_filtered", "e" if step > 0 else "w", None)
    if abs(oa_x - efu_in) > 2:
        special[(efu_in + 2 * step, 28, 3)] = ("hv_duct_hanger", None, None)
    # The emergency intake: out of the plenum's lower outlet, its damper, into the filtration unit's inlet.
    duct.append(([(efu_in, 25, 2)], "n", "s"))
    special[(efu_in, 25, 2)] = ("hv_damper", None, 2 * damp[1])
    # The control room's return: up through the floor (grille, sleeve, the fire damper) and into the air handler's front.
    ret = [(ahu_c, 22, 2), (ahu_c, 23, 2), (ahu_c, 24, 2), (ahu_c, 25, 2)]
    duct.append((ret, None, "s"))
    special[(ahu_c, 22, 2)] = ("hv_return_grille", None, None)
    special[(ahu_c, 23, 2)] = ("hv_duct_sleeve", None, None)
    special[(ahu_c, 24, 2)] = ("hv_duct_riser_fd", None, None)
    # The supply: out of the far end, up, a tee either way, down through the floor at two diffusers.
    sz = az + 7
    sup = [(ahu_c, 25, sz), (ahu_c, 26, sz), (ahu_c, 27, sz)]
    duct.append((sup, "n", None))
    for dx in (-3, 1) if t == "A" else (-3, 1):
        x1 = ahu_c + dx
        stp = 1 if dx > 0 else -1
        run = [(ahu_c, 27, sz)] + [(x, 27, sz) for x in range(ahu_c + stp, x1 + stp, stp)] + [(x1, y, sz) for y in (26, 25, 24, 23, 22)]
        duct.append((run, None, None))
        special[(x1, 22, sz)] = ("hv_supply_diffuser", None, None)
        special[(x1, 23, sz)] = ("hv_duct_sleeve", None, None)
        special[(x1, 24, sz)] = ("hv_duct_riser_fd", None, None)
        special[(x1, 25, sz)] = ("hv_duct_riser_label_supply", None, None)
    special[(ahu_c, 24, 2)] = ("hv_duct_riser_fd", None, None)
    # Chilled water. The return main: up out of the coil, south at y 28 to over the pump, down into its suction.
    pxp, pz = c["pump"]
    chx, chz = c["chiller"]
    cx_ret, cx_sup = chx + 2, chx + 1      # the chiller's return (in) and supply (out) nozzles' columns
    stp = 1 if pxp > chw_x else -1
    chwr = [(chw_x, 27, 7)] + [(chw_x, 28, z) for z in range(7, pz + 1)] + [(x, 28, pz) for x in range(chw_x + stp, pxp + stp, stp)]
    chwr += [(pxp, 27, pz), (pxp, 26, pz)]
    pipe.append((chwr, "d", "d"))
    # The pump's discharge: up out of its top, along at y 27 to over the chiller's return nozzle, down into it.
    stp2 = 1 if cx_ret > pxp else -1
    dis = [(pxp, 26, pz + 1), (pxp, 27, pz + 1)] + [(x, 27, pz + 1) for x in range(pxp + stp2, cx_ret + stp2, stp2)] + [(cx_ret, 27, chz)]
    pipe.append((dis, "d", "d"))
    # The supply main: up out of the chiller's supply nozzle to y 29, along over the return main, north to the coil.
    stp3 = 1 if chw_x > cx_sup else -1
    chws = [(cx_sup, 27, chz), (cx_sup, 28, chz), (cx_sup, 29, chz)] + [(x, 29, chz) for x in range(cx_sup + stp3, chw_x + stp3, stp3)]
    chws += [(chw_x, 29, z) for z in range(chz - 1, 5, -1)] + [(chw_x, 28, 6), (chw_x, 27, 6)]
    pipe.append((chws, "d", "d"))
    for z in (9, 15):
        special[(chw_x, 29, z)] = ("hv_chw_pipe_hanger", None, None)
    for z in (10, 14):
        special[(chw_x, 28, z)] = ("hv_chw_pipe_trapeze", None, None)
    special[(chw_x, 29, 12)] = ("hv_chw_label_chws", "n", None)
    special[(chw_x, 28, 12)] = ("hv_chw_label_chwr", "s", None)
    # The expansion tank's line comes up into the return main over it (between the coil's column and the pump's).
    special[((chw_x + pxp) // 2, 28, pz)] = ("hv_chw_pipe_tap", None, None)
    return duct, pipe, special


def hvac_south_bay(t):
    """Train t's loop fittings in the south bay: (piece, x, y, z, facing) for its expansion tank (beside its pump, its
    line up into the return main's tap), its chemical feed pot across the pump, its chiller's refrigerant sensor."""
    c = HVAC_TRAINS[t]
    pxp, pz = c["pump"]
    chx, chz = c["chiller"]
    tank_x = (c["ahu"][0] + pxp) // 2      # between the coil's column and the pump's: under the return main
    return [("hv_expansion_tank", tank_x, 24, pz, "north"), ("hv_expansion_tank_top", tank_x, 25, pz, "north"),
            ("hv_small_pipe", tank_x, 26, pz, "north"), ("hv_small_pipe", tank_x, 27, pz, "north"),
            ("hv_chem_feeder", pxp + 1, 24, pz + 1, "north"), ("hv_refrig_sensor", chx + 3, 24, chz + 4, "west")]


def top_floor(b, W, D, y):
    """The Control Room HVAC Equipment Room (walked at y + 1, under the roof): each train's intake plenum against its
    louvres with its radiation monitor and toxic gas analyser, its emergency filtration unit and air handler, the ducts
    between them and down through the floor into the control room, its chilled water pump and chiller in the south
    bay and their mains overhead; the HVAC control panel on the north wall between the trains."""
    import hvac_room as H
    y0 = y + 1
    for t, c in HVAC_TRAINS.items():
        v = c["variant"] or None
        b.prop("cr_filtration_unit", c["efu"][0], y0, c["efu"][1], "south", variant=v)
        b.prop("cr_air_handling_unit", c["ahu"][0], y0, c["ahu"][1], "south", variant=v)
        b.prop("chw_pump", c["pump"][0], y0, c["pump"][1], "south", variant=v)
        b.prop("cr_chiller", c["chiller"][0], y0, c["chiller"][1], "south", variant=v)
        # The intake plenum, four high against the louvres: its base, the emergency outlet's row, the access door and
        # the monitors' sample point, its top with the normal outside air's outlet.
        px0, px1 = c["plenum"]
        efu_in = c["efu"][0] + 1
        oa_x = px1 if c["ahu"][0] + 1 > px1 else px0
        for row, yy in enumerate(range(y0, y0 + 4)):
            names = []
            for x in range(px0, px1 + 1):
                if row == 0:
                    names.append("hv_plenum_base")
                elif row == 1:
                    names.append("hv_plenum_outlet" if x == efu_in else "hv_plenum")
                elif row == 2:
                    names.append(f"hv_plenum_door_{t.lower()}" if x == c["door"] else "hv_plenum_detector" if x == c["detector"] else "hv_plenum")
                else:
                    names.append("hv_plenum_top_outlet" if x == oa_x else "hv_plenum_top")
            b.row(names, px0, yy, 1, "north")
        b.panel("hv_rad_monitor", c["rad"], y0 + 1, 1, "north", 1, label=t)
        b.panel("hv_tox_monitor", c["tox"], y0 + 1, 1, "north", 1, label=t)
        duct, pipe, special = hvac_runs(t)
        for runs, family in ((duct, H.DUCT_FAMILY), (pipe, H.PIPE_FAMILY)):
            for (x, yy, z), (name, facing, variant) in H.pieces_for(H.connections(runs), family, special).items():
                if yy == 22:
                    assert b.get(x, yy, z) == CEIL, (t, x, yy, z)
                elif yy == 23:
                    assert b.get(x, yy, z) == EPOXY, (t, x, yy, z)
                else:
                    assert b.get(x, yy, z) == 0, (t, name, x, yy, z)
                b.p(x, yy, z, variant_state(name, facing, variant) if variant is not None else furniture(name, facing))
    b.panel("hv_control_panel", HVAC_PANEL[0], y0, HVAC_PANEL[1], "north", 3)
    # The south bay: each loop's expansion tank, feed pot and refrigerant sensor; the refrigerant monitor on the west
    # wall by the chillers; the spare filters on their racks along the south wall.
    for t in HVAC_TRAINS:
        for name, x, yy, z, facing in hvac_south_bay(t):
            assert b.get(x, yy, z) == 0, (name, x, yy, z)
            b.p(x, yy, z, furniture(name, facing))
    b.panel("hv_refrig_monitor", 1, y0 + 1, 20, "west", 1)
    for k, x in enumerate(range(18, 28)):
        for row in range(2):
            b.p(x, y0 + row, D - 2, variant_state("hv_filter_rack", "south", (k + 2 * row) % 4))
    # The room's fittings: smoke detectors on the roof's underside clear of the ducts and pipes, the extinguishers.
    for x, z in ((5, 13), (15, 7), (25, 13), (14, 20), (26, 25), (4, 25), (34, 21)):
        if b.get(x, y0 + 5, z) == 0:
            b.p(x, y0 + 5, z, furniture("smoke_detector", "north"))
    # The spare filters on racks along the south wall (HEPA cells, charcoal trays, prefilters), the room's drawings.
    b.furniture("drawing_rack", 14, y0, D - 2, "south")
    b.furniture("fire_extinguisher", 1, y0 + 1, 12, "west")
    b.furniture("fire_extinguisher", W - 2, y0 + 1, 26, "east")
    b.furniture("fire_extinguisher", 12, y0 + 1, 28, "south")


def top_floor_sign(b, y):
    """The room's name inside the stair tower beside its top floor door (laid after the tower), facing whoever comes
    up the stairs."""
    y0 = y + 1
    for x in (35, 36, 32, 31):
        if b.get(x, y0 + 1, 14) not in (0,) and b.get(x, y0 + 1, 13) == 0 and b.get(x, y0, 13) == 0:
            b.p(x, y0 + 1, 13, sign("cr_hvac_room", "north"))
            return
    raise AssertionError("no wall for the HVAC room's sign")


# ------------------------------------------------------------------------------------------------- auxiliary building

def aux_building(s, ox, oz):
    """The nuclear auxiliary building, 60 x 40, three storeys of plant rooms either side of a central corridor:
    charging, residual heat removal and safety injection pumps, component cooling heat exchangers, the boric acid
    tanks, the ventilation plant. Each room is entered round a shielding wall; the health physics checkpoint with its
    lockers is at the entrance; pipes run along the corridor ceilings."""
    W, D, H = 60, 40, 23
    b = Building("aux_building", "Nuclear Auxiliary Building", W, D, H, grounded=True)
    b.shell_walls(CONCRETE, step=10)
    b.windows(3, 4, "s", step=10, skip=range(1, 12))
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    b.storey(8, 1, 1, W - 2, D - 2, EPOXY)
    b.storey(16, 1, 1, W - 2, D - 2, EPOXY)
    entrance(b, 4, D - 1, "s", kind="steel")
    b.stairwell(W - 9, D - 15, [0, 8, 16], door_side="n")
    corridor = (18, 21)
    labels = {0: ("charging_pumps", "heat_exchangers"), 8: ("tank_room", "heat_exchangers"), 16: ("ventilation", "rhr_pumps")}
    for lv in (0, 8, 16):
        y0, y1 = lv + 1, lv + 7
        b.wall_x(corridor[0] - 1, 13, W - 2, y0, y1, CONCRETE)
        b.wall_x(corridor[1] + 1, 1, W - 11, y0, y1, CONCRETE)
        for x in range(23, W - 2, 10):
            b.wall_z(x, 1, corridor[0] - 2, y0, y1, CONCRETE)
            if x < W - 11:
                b.wall_z(x, corridor[1] + 2, D - 2, y0, y1, CONCRETE)
        # Room doors from the corridor, each behind a shielding wall inside the room.
        for x in range(27, W - 2, 10):
            b.door(x, y0, corridor[0] - 1, "north", label=labels[lv][0])
            b.wall_x(corridor[0] - 4, x - 2, x + 2, y0, y0 + 2, CONCRETE_DARK)
            if x < W - 11:
                b.door(x, y0, corridor[1] + 1, "south", label=labels[lv][1])
                b.wall_x(corridor[1] + 4, x - 2, x + 2, y0, y0 + 2, CONCRETE_DARK)
        for x in range(14, W - 2, 4):
            for z in (corridor[0] + 1, corridor[1] - 1):
                if b.get(x, y1, z) == 0:
                    b.p(x, y1, z, LIGHT)
        if lv:
            b.door(6, y0, corridor[1] + 1, "south", double=True, label="ventilation" if lv == 16 else "tank_room")
        # Pipes along the corridor ceiling, on both walls.
        for x in range(13, W - 1):
            for z in (corridor[0], corridor[1]):
                if b.get(x, y1, z) == 0:
                    b.p(x, y1, z, PIPE)
    # The west end: health physics checkpoint by the entrance (lockers, a desk), the stair to the upper floors.
    b.wall_z(12, corridor[0], D - 2, 1, 7, PAINTED)
    b.door(12, 1, 30, "east", double=True, label="health_physics")
    for z in range(23, 36):
        b.stack("locker", 1, 1, z, "west", n=2)
    b.desk(8, 1, 27, "west")
    b.p(6, 3, 23, beacon("red", "south"))   # the controlled area's warning beacon, on the corridor wall
    b.stairwell(1, 1, [0, 8, 16], door_side="s")
    # Ground floor: pumps north, heat exchangers south.
    for x in range(24, W - 4, 10):
        b.prop("pump_skid", x + 1, 1, 3, "south")
        if x + 7 < W - 1:
            b.prop("pump_skid", x + 5, 1, 3, "south")
        if x < W - 12:
            b.prop("heat_exchanger", x + 3, 1, 27, "south")
    # Level 2: tanks, heat exchangers.
    for x in (24, 34, 44):
        b.prop("storage_tank", x + 2, 9, 3, "south")
    for x in range(24, W - 12, 10):
        b.prop("heat_exchanger", x + 3, 9, 27, "south")
    # Level 3: ventilation plant, pumps.
    for x in (24, 34):
        b.prop("air_handling_unit", x + 2, 17, 4, "south")
    for x in range(24, W - 12, 10):
        b.prop("pump_skid", x + 3, 17, 27, "south")
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- fuel building

def fuel_building(s, ox, oz):
    """The fuel building, 32 x 46: the spent fuel pool (lined, blue with Cherenkov light from the racks), the fuel
    transfer canal toward the containment, the cask loading pit, the fuel handling machine on rails over the pool,
    and the truck bay with dry storage casks and new fuel storage at the south end behind a roller door."""
    W, D, H = 32, 46, 27
    b = Building("fuel_building", "Fuel Building", W, D, H, grounded=True)
    b.shell_walls(SHIELD, step=8)
    b.windows(22, 24, "ew", step=8)
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    deck = 12
    b.b(1, deck, 1, W - 2, deck, 30, EPOXY)
    px0, px1, pz0, pz1 = 7, 18, 4, 25
    # The pool's structure: shielding concrete round its steel liner (in a real plant, walls a couple of metres
    # thick), so the racks' radiation stays in the water (round 14: it reached the open through the single liner
    # plate and the building's brick plinth).
    b.b(px0 - 1, 1, pz0 - 1, px1 + 1, deck, pz1 + 1, SHIELD)
    b.b(px0, 1, pz0, px1, deck, pz1, LINER)
    b.c(px0 + 1, 2, pz0 + 1, px1 - 1, deck, pz1 - 1)
    for x in range(px0 + 1, px1):
        for z in range(pz0 + 1, pz1):
            for y in range(2, deck + 1):
                b.p(x, y, z, WATER)
    for x in range(px0 + 1, px1 - 3, 4):
        for z in range(pz0 + 1, pz1 - 3, 4):
            b.prop("spent_fuel_rack", x, 2, z, "north")
    b.b(0, 4, 12, px0 - 1, deck, 18, SHIELD)
    b.b(0, 5, 13, px0, deck, 17, LINER)
    b.c(1, 6, 14, px0, deck, 16)
    for x in range(1, px0 + 1):
        for z in range(14, 17):
            for y in range(6, deck + 1):
                b.p(x, y, z, WATER)
    b.b(px1 + 1, 1, 5, px1 + 6, deck, 11, LINER)
    b.c(px1 + 2, 2, 6, px1 + 5, deck, 10)
    for x in range(px1 + 2, px1 + 6):
        for z in range(6, 11):
            for y in range(8, deck + 1):
                b.p(x, y, z, WATER)
    b.prop("dry_storage_cask", px1 + 2, 2, 6, "north")
    # Railing round the pool, the canal and the cask pit; rails for the handling machine.
    b.rail_round(px0 + 1, pz0 + 1, px1 - 1, pz1 - 1, deck + 1)
    b.rail_round(px1 + 2, 6, px1 + 5, 10, deck + 1)
    for z in range(pz0 - 2, pz1 + 3):
        for x in (px0 - 1, px1 + 1):
            if b.get(x, deck + 1, z) == 0 or b.get(x, deck + 1, z) == 0:
                b.p(x, deck + 1, z, RAIL_NS)
    b.prop("fuel_handling_machine", px0 - 1, deck + 2, 12, "north")
    # The truck bay: roller door south, dry casks, new fuel on racking.
    roller_door(b, [(x, D - 1) for x in range(10, 22)], 1, 9, "north")
    b.b(9, 10, D - 1, 22, 10, D - 1, SHIELD)
    for z in (32, 36):
        b.prop("dry_storage_cask", 24, 1, z, "north")
    for z in list(range(31, 36)) + list(range(41, 45)):
        b.stack("pallet_rack", 30, 1, z, "east", n=3)
    b.stairwell(1, 30, [0, deck], door_side="n")
    entrance(b, W - 1, 38, "e", kind="steel")
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- diesel building

def diesel_building(s, ox, oz):
    """The emergency diesel generator building, 67 x 26 (round 31; Justin: "rebuild the whole diesel room, generator
    included", "Bigger than real"): two generator rooms (one per safety train), each 32 x 24 inside and 13 high, laid
    out the same (the set can't be mirrored: its gauge board, fuel filter and terminal box are on fixed sides). In each:
    the V20 diesel generator set along the middle, its free end east (the exhaust up through the roof to its stack),
    the generator west; along the north wall its starting air system (two compressors, the dryer, two receivers) with
    their lines in the floor to the engine's two starting air headers; its fuel oil day tank in the south-east corner,
    its line in the floor to the engine's duplex filter; its panels and battery along the south wall; air intake louvres
    high in the north wall. Returns the building and, per room, where its fill line comes through the south wall."""
    W, D, H = 67, 26, 13
    b = Building("diesel_building", "Emergency Diesel Building", W, D, H, grounded=True)
    b.shell_walls(CONCRETE, step=8)
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    b.wall_z(W // 2, 1, D - 2, 1, H, CONCRETE)
    fills = []
    for room, x0 in enumerate((0, W // 2)):
        floor = diesel_room(b, s, ox, oz, x0, room, H, D)
        fills.append(day_tank(b, x0, room, H, D, floor))
        diesel_room_plant(b, x0, room, D)
    s.add(b, ox, oz)
    return b, fills


# Where the set and its starting air system stand in a diesel room (x from the room's west wall, z from the north
# wall): the set's footprint corner (it faces west: its free end at the east), and each air piece's (they face north,
# their fronts to the room).
DIESEL_SET = (5, 9)
DIESEL_AIR = [("edg_air_compressor", 3, 2, 0), ("edg_air_compressor", 7, 2, 1), ("edg_air_dryer", 11, 3, 0),
              ("edg_air_receiver", 14, 2, 0), ("edg_air_receiver", 17, 2, 1)]
# The starting air's lines in the floor (cells, from the room's corner): the compressors' discharges along behind them
# to the dryer's inlet; the dryer's outlet to both receivers' inlets; each receiver's outlet to one of the engine's two
# starting air headers (receiver 1 the left bank's, coming up at the engine's front left; receiver 2 the right's).
AIR_LINES = [
    [(5, 2), (5, 1)] + [(x, 1) for x in range(6, 11)] + [(10, 2), (10, 3), (11, 3)] + [(9, 2), (9, 1)],
    [(12, 3), (13, 3), (14, 3)] + [(13, 2), (13, 1)] + [(x, 1) for x in range(14, 18)] + [(17, 2), (17, 3)],
    [(15, 3)] + [(15, z) for z in range(4, 7)] + [(x, 6) for x in range(16, 22)] + [(21, z) for z in range(7, 17)] + [(22, 16), (23, 16), (24, 16)],
    [(18, 3), (18, 4)] + [(x, 4) for x in range(19, 24)] + [(23, z) for z in range(5, 12)] + [(24, 11)],
]


def diesel_room(b, s, ox, oz, x0, room, H, D):
    """One generator room (x0: its west wall), its set, starting air system and panels; returns where the day tank's
    line ends under the engine's fuel connection (its floor cell)."""
    sx, sz = DIESEL_SET
    b.prop("emergency_diesel_generator", x0 + sx, 1, sz, "west", variant=room or None)
    for kind, x, z, k in DIESEL_AIR:
        variant = 2 * room + k if kind != "edg_air_dryer" else room
        b.prop(kind, x0 + x, 1, z, "north", variant=variant or None)
    for line in AIR_LINES:
        for x, z in line:
            assert b.get(x0 + x, 0, z) in (EPOXY, WALL_PIPE), ("starting air line in the floor", x0 + x, z)
            b.p(x0 + x, 0, z, WALL_PIPE)
    # Along the south wall, west to east: the diesel's battery (two wide: round 31 part 2) and its charger, the load
    # sequencer, the protection relays, the voltage regulator, the generator's control panel, the engine's (nearest the
    # engine's front); each labelled with the room's train (EDG 1A, EDG 1B: tools/diesel_panels.py).
    x = x0 + 15
    for name, width in reversed((("edg_battery_rack", 2), ("edg_battery_charger", 1), ("edg_load_sequencer", 1), ("edg_protection_panel", 1),
                                 ("edg_voltage_regulator", 1), ("edg_generator_panel", 2), ("edg_engine_panel", 1))):
        b.panel(name, x, 1, D - 2, "south", width, ("EDG 1A", "EDG 1B")[room])
        x -= width
    # Louvres high in the north wall (between its pilasters).
    for x in range(x0 + 1, x0 + 33):
        if x % 8:
            for y in (7, 8, 9):
                b.p(x, y, 0, LOUVRE)
    # The stack over the exhaust's roof thimble (the set's exhaust comes up through the roof there: tools/diesel_set.py
    # STACK). (It stands above the part's own height: it goes in as site decor.)
    for y in range(H + 1, H + 8):
        for dx in (0, 1):
            for z in (13, 14):
                s.decor[(ox + x0 + 24 + dx, b.site_y(y), oz + z)] = BRICK
    entrance(b, x0 + 23, D - 1, "s", kind="steel")
    b.p(x0 + 21, 2, D - 2, sign("diesel_a" if room == 0 else "diesel_b", "north"))
    # The day tank's line comes up under the engine's duplex fuel filter (the set's local (8, 0): a block right of
    # its starting air riser).
    return (x0 + sx + 20, sz + 1)


# The diesel room's own plant and fittings (round 31 part 3; Justin: "Diesel Part 3"; tools/diesel_room.py,
# tools/diesel_fittings.py), room coordinates as DIESEL_AIR (x from the room's west wall, z from the north wall):
# along the north wall east of the starting air, the jacket water and lube oil keep-warm skids and the jacket water
# expansion tank; along the east wall the jacket water cooler (its front to the set's free end); along the south wall
# west of the panels the generator's neutral grounding resistor and its terminal and CT cubicle, east of them the CO2
# cylinder bank and the release station by the door.
DIESEL_PLANT = [("edg_jw_keepwarm", 20, 1, 1, "north"), ("edg_lo_keepwarm", 24, 1, 1, "north"), ("edg_expansion_tank", 28, 1, 1, "north"),
                ("edg_jw_cooler", 31, 1, 3, "east"), ("edg_ngr", 1, 1, 23, "south"), ("edg_gen_cubicle", 3, 1, 23, "south"),
                ("edg_co2_bank", 17, 1, 24, "south"), ("edg_monorail", 2, 12, 13, "north")]
# Where each piece's lines go down into the floor (room cells, from its footprint), and where they go to: the engine's
# connections at its free end, under the set (the jacket water's and lube oil's), the generator's terminal box
# (round 31 part 1: its conduits down into the floor) to the cubicle and the cubicle to the resistor; the service
# water lines down into the floor under the cooler. Laid in the floor (WALL_PIPE, as the starting air's), each the
# shortest way; where it meets a line already in the floor it shares its duct bank (the floor's one block deep).
DIESEL_PLANT_LINES = [((20, 2), (24, 12)), ((22, 2), (24, 13)), ((24, 2), (24, 14)), ((26, 2), (24, 15)), ((28, 2), (25, 12)),
                      ((32, 9), (25, 13)), ((32, 4), (25, 14)), ((32, 3), (32, 3)), ((9, 16), (4, 23)), ((8, 16), (5, 23)),
                      ((3, 23), (2, 24))]
# The CO2 discharge piping, in the block under the roof (y 13): the riser up the wall from the cylinder bank (x 19), the
# main along the south wall (z 23) from x 9 to 29, and four branches north from it (x 9, 15, 21, 29) to z 3; a nozzle
# every five blocks, the last capped with one, hangers between.
CO2_Y = 13
CO2_RISER = 19
CO2_MAIN_Z = 23
CO2_BRANCHES = (9, 15, 21, 29)
CO2_NOZZLES_Z = (19, 14, 9)
CO2_NOZZLES_X = (12, 25)
CO2_HANGERS_Z = (21, 16, 11, 6)
CO2_HANGERS_X = (11, 17, 23, 27)
HEAT_DETECTORS = [(5, 7), (12, 7), (18, 7), (25, 7), (5, 18), (12, 18), (18, 18), (25, 18)]


def floor_path(b, start, goal, x0):
    """The shortest way through the floor (its epoxy, and the lines already in it) from start to goal (room cells), or
    None."""
    from collections import deque
    sx, sz = start[0] + x0, start[1]
    gx, gz = goal[0] + x0, goal[1]
    prev = {(sx, sz): None}
    q = deque([(sx, sz)])
    while q:
        c = q.popleft()
        if c == (gx, gz):
            out = []
            while c is not None:
                out.append(c)
                c = prev[c]
            return out[::-1]
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (c[0] + dx, c[1] + dz)
            if n not in prev and x0 + 1 <= n[0] <= x0 + 32 and 1 <= n[1] <= 24 and b.get(n[0], 0, n[1]) in (EPOXY, WALL_PIPE):
                prev[n] = c
                q.append(n)
    return None


def diesel_room_plant(b, x0, room, D):
    import diesel_fittings as DF
    import hvac_room as HR
    label = ("EDG 1A", "EDG 1B")[room]
    for kind, x, y, z, facing in DIESEL_PLANT:
        b.prop(kind, x0 + x, y, z, facing, variant=room or None)
    for start, goal in DIESEL_PLANT_LINES:
        path = floor_path(b, start, goal, x0)
        assert path, ("diesel room line in the floor", room, start, goal)
        for x, z in path:
            if b.get(x, 0, z) == EPOXY:
                b.p(x, 0, z, WALL_PIPE)
    # The release station by the door (west of its frame), a horn and strobe over it and another on the north wall,
    # the exhaust fans high in the south wall over their louvres, the hoist's pendant station on the west wall under
    # the monorail's end.
    b.panel(DF.STATION, x0 + 20, 1, D - 2, "south", 1, label)
    b.panel(DF.HORN, x0 + 20, 4, D - 2, "south", 1)
    b.panel(DF.HORN, x0 + 12, 5, 1, "north", 1)
    for fx in (10, 26):
        b.panel(DF.FAN, x0 + fx + 1, 8, D - 2, "south", 2, label)
        for x in (fx, fx + 1):
            for y in (8, 9):
                assert b.get(x0 + x, y, D - 1) == CONCRETE, ("fan louvre", room, x, y)
                b.p(x0 + x, y, D - 1, LOUVRE)
    b.panel(DF.PENDANT, x0 + 1, 1, 13, "west", 1, label)
    # The signs at the door: CO2 and hearing protection; the extinguisher; the eyewash in the north-west corner; floor
    # drains by the skids and the cooler.
    b.p(x0 + 24, 2, D - 2, furniture("sign_co2", "south"))
    b.p(x0 + 24, 3, D - 2, furniture("sign_hearing", "south"))
    b.furniture("fire_extinguisher", x0 + 25, 2, D - 2, "south")
    b.furniture("eyewash_station", x0 + 1, 1, 1, "north")
    b.p(x0 + 1, 2, 1, furniture("sign_eyewash", "north"))
    for x, z in ((23, 3), (27, 3), (30, 6)):
        assert b.get(x0 + x, 0, z) == EPOXY, ("floor drain", room, x, z)
        b.p(x0 + x, 0, z, furniture("restroom_floor_drain", "north"))
    for x, z in HEAT_DETECTORS:
        assert b.get(x0 + x, CO2_Y, z) == 0, ("heat detector", room, x, z)
        b.p(x0 + x, CO2_Y, z, furniture("smoke_detector", "north"))
    # The CO2 piping.
    y = CO2_Y
    runs = [([(x0 + CO2_RISER, yy, D - 2) for yy in range(3, y + 1)] + [(x0 + CO2_RISER, y, CO2_MAIN_Z)], "d", None),
            ([(x0 + x, y, CO2_MAIN_Z) for x in range(CO2_RISER, CO2_BRANCHES[0] - 1, -1)] + [(x0 + CO2_BRANCHES[0], y, z) for z in range(CO2_MAIN_Z - 1, 2, -1)], None, None),
            ([(x0 + x, y, CO2_MAIN_Z) for x in range(CO2_RISER, CO2_BRANCHES[-1] + 1)] + [(x0 + CO2_BRANCHES[-1], y, z) for z in range(CO2_MAIN_Z - 1, 2, -1)], None, None)]
    for bx in CO2_BRANCHES[1:-1]:
        runs.append(([(x0 + bx, y, z) for z in range(CO2_MAIN_Z, 2, -1)], None, None))
    conn = HR.connections(runs)
    for (x, yy, z), faces in sorted(conn.items()):
        want = "".join(sorted(faces))
        rx = x - x0
        if want in ("ns",) and rx in CO2_BRANCHES and z in CO2_NOZZLES_Z or want == "ew" and z == CO2_MAIN_Z and rx in CO2_NOZZLES_X:
            name = "co2_nozzle"
        elif want in ("ns",) and rx in CO2_BRANCHES and z in CO2_HANGERS_Z or want == "ew" and z == CO2_MAIN_Z and rx in CO2_HANGERS_X:
            name = "co2_pipe_hanger"
        else:
            name = None
        for base, fam in DF.FAMILY.items():
            if name and fam != "co2_pipe":
                continue
            f = HR.facing_for(base, want)
            if f:
                assert b.get(x, yy, z) == 0, ("CO2 pipe", room, rx, yy, z)
                b.p(x, yy, z, furniture(name or fam, f))
                break
        else:
            raise ValueError(f"no CO2 piece opens {want} at {(rx, yy, z)}")


def day_tank(b, x0, room, H, D, fuel_at):
    """A generator room's fuel oil day tank in its south-east corner: a small storage tank MBS (tank_in) on a steel
    drip pan with a lip round it, ladder, railing, gate and hatch like the big tanks, and a short vent on its roof like
    theirs (Justin, round 14: no vent line up through the building's roof). It is rated for flammable liquids (the
    pressure/vacuum vent; no foam chambers indoors). The fuel lines keep out of the way, in the floor (pipes inside the slab, Justin's round 13):
    the outlet drops into the floor and runs under it to come up in the engine's fuel connection (fuel_at); the line
    from the fuel oil storage tank comes in under the south wall and up into the inlet. Returns where that comes in
    under the wall (its x)."""
    cx, cz, r, h = x0 + 29.5, 19.5, 2.6, 6
    out_cell, in_cell, _ = tank_in(b, cx, cz, r, h, "diesel_day_a" if room == 0 else "diesel_day_b", outlet="w",
                                           inlet="s", ladder="n", manway=None, hatch_in=2, vent=(0, -1), foundation=None,
                                           fill_line=False, wall=1.1, rating="flammable", foam=False)
    # The outlet: a stub off its flange, down into the floor, along under it to the set's fuel connection.
    ox, oz = out_cell
    fx, fz = fuel_at
    ix, iz = in_cell
    risers = [(ox - 1, 1, oz), (ix, 1, iz + 1)]
    floor = [(ox - 1, 0, oz), (ox, 0, oz)] + [(ox, 0, z) for z in range(oz - 1, fz - 1, -1)] + [(x, 0, fz) for x in range(ox - 1, fx - 1, -1)]
    # The inlet: in under the south wall (through the slab under it) and up into the inlet's flange.
    floor += [(ix, 0, z) for z in range(D - 1, iz, -1)]
    for x, y, z in risers:
        assert b.get(x, y, z) == 0, ("fuel line", x, y, z)
        b.p(x, y, z, PIPE)
    for x, y, z in floor:
        assert b.get(x, y, z) in (EPOXY, SMOOTH), ("fuel line in the floor", x, y, z)
        b.p(x, y, z, WALL_PIPE)
    # The drip pan under and round the tank, a steel lip at its edge (broken where the pipes come out).
    for x in range(int(cx - r - 3), int(cx + r + 3)):
        for z in range(1, min(D - 1, int(cz + r + 3))):
            d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
            if d <= r + 1.5 and b.get(x, 0, z) == EPOXY:
                b.p(x, 0, z, LINER)
            if r + 0.9 < d <= r + 1.5 and b.get(x, 1, z) == 0:
                b.p(x, 1, z, LIP)
    return ix


# ------------------------------------------------------------------------------------------------- pump house

def pump_house(s, ox, oz):
    """The circulating water intake and pump house, 40 x 30: four intake bays reaching out over the lake (concrete
    walls down into the water, a travelling screen in each), the pump floor with a circulating water pump over each
    bay's suction, the electrical room behind, and a roller door for the crane bay."""
    W, D, H, sink = 40, 30, 14, 3
    b = Building("pump_house", "Intake Pump House", W, D, H, sink=sink, grounded=True)
    # The building's floor is level with the ground; the intake deck out over the water stands a block higher (up),
    # clear of the water. The bay walls go down to y -4 (the grid's bottom).
    bays, bw, up = 4, 10, 1
    b.b(0, up, 0, W - 1, up, 6, CONCRETE)
    for i in range(bays + 1):
        x = min(i * bw, W - 1)
        b.b(x, -sink, 0, x, up, 6, CONCRETE)
    b.b(0, -sink, 6, W - 1, up, 6, CONCRETE)
    for i in range(bays):
        x0 = i * bw + 1
        b.c(x0, up, 0, x0 + bw - 2, up, 5)
        b.prop("travelling_water_screen", x0 + 1, up - 3, 2, "north")
    # Railing along the intake deck's edge over the bays.
    for x in range(W):
        if b.get(x, up, 6) and b.get(x, up + 1, 6) == 0:
            b.rail(x, up + 1, 6, "north")
    q = Building("x", "x", W, D - 7, H, grounded=True)
    q.shell_walls(CONCRETE, step=10)
    q.windows(8, 11, "nsew", step=10)
    q.storey(0, 1, 1, W - 2, D - 9, EPOXY)
    for x in range(1, W - 1):
        q.p(x, 10, 1, FRAME)
        q.p(x, 10, D - 9, FRAME)
    q.wall_x(D - 15, 1, W - 2, 1, 7, CONCRETE)
    q.b(1, 8, D - 14, W - 2, 8, D - 9, CONCRETE)
    q.door(20, 1, D - 15, "south", double=True, label="electrical_room")
    entrance(q, 6, D - 8, "s", kind="steel")
    for x in range(3, 15):
        q.stack("relay_cabinet", x, 1, D - 9, "south", n=2)
    q.prop("switchgear_lineup", 24, 1, D - 10, "south", variant=4)
    b.g[:, sink:sink + q.size[1], 7:] = q.g[:, :b.size[1] - sink, :]
    b.extras.update({(x, y + sink, z + 7): st for (x, y, z), st in q.extras.items()})
    b.props += [(k, x, y + sink, z + 7, f) for k, x, y, z, f in q.props]
    b.doors += [(x, y, z + 7, f) for x, y, z, f in q.doors]
    b.seats += [(n, x, y, z + 7, f) for n, x, y, z, f in q.seats]
    b.bays, b.bay_w = bays, bw
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- site buildings

def storage_rack(b, x, y, z, facing):
    """A storage rack (three bays long, three levels high, one deep) whose left bay as you face its front is at
    (x, y, z); facing is the way its user looks (its front faces the other way). Its parts: bay c to the user's right,
    level r up, part r * 3 + c."""
    rx, rz = P.DIRS[P.CLOCKWISE[facing]]
    for r in range(3):
        for c in range(3):
            b.p(x + rx * c, y + r, z + rz * c, multi_state("storage_rack", facing, r * 3 + c))


def warehouse(s, ox, oz):
    """The warehouse, level with the ground: the automated warehouse's Medium Warehouse with two floors (part W3,
    tools/warehouse_bp.py plant_warehouse; it replaced the old one-floor stores with storage racks): Goods In and Goods
    Out in its front wall, racks five places high on both floors, the Pallet Lift, the stair tower on its east side, the
    control office with the terminal, which can add floors to it."""
    import warehouse_bp
    b = warehouse_bp.plant_warehouse()
    s.add(b, ox, oz)
    return b


SIDES = {"n": ("north", 0, -1), "s": ("south", 0, 1), "e": ("east", 1, 0), "w": ("west", -1, 0)}


# The roof vent of each tank rating (nuclear/TankFittings.java): a gooseneck, a pressure/vacuum vent on a flame
# arrester, a scrubber vent, the cryogenic tank's relief valves.
RATING_VENTS = {"general": "storage_tank_vent[facing=north,riser=false]", "flammable": "storage_tank_pv_vent[facing=north]",
                "corrosive": "storage_tank_scrubber_vent[facing=north]", "cryogenic": "tank_relief_valves[facing=north]"}
BUND = M("minecraft:smooth_stone_slab[type=bottom,waterlogged=false]", (158, 158, 156))


def tank_in(b, cx, cz, r, h, service="none", outlet="w", inlet="n", ladder="e", manway="s", hatch_in=2, vent=(0, 0),
            foundation=SMOOTH, fill_line=True, wall=1.2, rating="general", foam=None, bund=False, foundation_r=None):
    """A storage tank (the MBS, tools/storage_tank.py) drawn into part b round (cx, cz) (block-centre coordinates):
    a shell of tank plates from y 1 to h on a foundation, a roof of plates at h + 1; low on the shell the outlet (the
    tank's controller, its float gauge run up the shell above it) and the inlet (with its fill line climbing to the
    top, fill_line), a bolted manway (manway None: none); a steel ladder with its safety cage up one side to a
    self-closing gate in the railing that runs round the roof edge unbroken; on the roof the hatch (part of the roof,
    hatch_in blocks in from the gate, its lid hinged on the far side) and the vent (vent: its offset from the middle,
    None for none). The plates are primer in the blueprint; the outlet paints them when it forms the tank.

    rating (nuclear/TankRating.java) sets the outlet's rating and the fittings: the roof vent (RATING_VENTS); a
    flammable tank's two foam chambers on the top course with their lines down the shell and an earthing boss at the
    foot (foam, on unless said otherwise); a corrosive tank's spill bund round the foundation (bund); a cryogenic tank has no ladder, railing or hatch (ladder None) and relief valves on
    its roof (tools/tank_kits.py stands it on legs and adds its vaporizer). Returns the cells of the outlet, the inlet
    and the vent."""
    if foam is None:
        foam = rating == "flammable"
    if foundation is not None:
        b.disc(cx, cz, foundation_r or r + 1, 0, 0, foundation)
    b.disc(cx, cz, r, 1, h, TANK_PLATE, inner=r - wall)
    b.disc(cx, cz, r, h + 1, h + 1, TANK_PLATE)
    mx, mz = int(cx), int(cz)
    gw, _, gd = b.g.shape

    def outside(side, y):
        """The cell just outside the shell in the middle of a side, and the last plate before it."""
        facing, dx, dz = SIDES[side]
        last = None
        for k in range(max(gw, gd)):
            px, pz = mx + dx * k, mz + dz * k
            plate = 0 <= px < gw and 0 <= pz < gd and b.get(px, y, pz) == TANK_PLATE
            if plate:
                last = (px, pz)
            elif last is not None:
                break  # out through this tank's shell (another tank may stand further on)
        return facing, (last[0] + dx, last[1] + dz), last

    # Outlet (the controller; the gauge runs up the shell above it) and the inlet with its fill line.
    facing, out_cell, _ = outside(outlet, 1)
    b.p(out_cell[0], 1, out_cell[1], tank_port("outlet", facing, service, rating))
    facing, in_cell, _ = outside(inlet, 1)
    b.p(in_cell[0], 1, in_cell[1], tank_port("inlet", facing))
    if fill_line:
        for y in range(2, h + 1):
            b.p(in_cell[0], y, in_cell[1], tank_piece("storage_tank_fill_pipe", facing, top=_flag(y == h)))
    if manway:
        facing, (x, z), _ = outside(manway, 1)
        b.p(x, 1, z, tank_piece("storage_tank_manway", facing))
    roof = {(x, z) for x in range(gw) for z in range(gd)
            if b.get(x, h + 1, z) == TANK_PLATE and math.hypot(x + 0.5 - cx, z + 0.5 - cz) <= r + 0.5}
    if ladder is not None:
        # The ladder, from the foundation to a length above the roof's edge, caged from the third length up.
        facing, (lx, lz), (ex, ez) = outside(ladder, 1)
        for y in range(1, h + 3):
            b.p(lx, y, lz, steel_ladder(facing, y >= 3, y == h + 2, y == 3))
        # Railing round the roof's edge: the roof cells with a gap beside them (corners too), so it runs unbroken;
        # the self-closing gate where the ladder comes up, swinging in over the roof.
        for x, z in roof:
            if any((x + dx, z + dz) not in roof for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
                b.p(x, h + 2, z, RAIL)
        b.p(ex, h + 2, ez, M(f"jugcraft:railing_gate[facing={OPPOSITE[facing]},open=false]", (232, 190, 30)))
        # The hatch, a cell of the roof a little in from the gate (its lid opens away from whoever comes up).
        _, gdx, gdz = SIDES[ladder]
        hx, hz = ex - hatch_in * gdx, ez - hatch_in * gdz
        assert (hx, hz) in roof and b.get(hx, h + 2, hz) == 0, ("hatch", hx, hz)
        b.p(hx, h + 1, hz, M(f"jugcraft:storage_tank_hatch[band=wall,facing={OPPOSITE[facing]},formed=false,open=false]", (150, 156, 150)))
    vent_cell = None
    if vent is not None:
        vent_cell = (mx + vent[0], mz + vent[1])
        b.p(vent_cell[0], h + 2, vent_cell[1], M("jugcraft:" + RATING_VENTS[rating], (150, 156, 160)))
    if foam:
        tank_foam(b, cx, cz, h)
    if bund:
        # The spill bund: a low concrete kerb round the foundation's edge.
        fr = foundation_r or r + 1
        for x in range(gw):
            for z in range(gd):
                d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
                if fr - 0.9 < d <= fr and b.get(x, 1, z) == 0 and b.get(x, 0, z) != 0:
                    b.p(x, 1, z, BUND)
    return out_cell, in_cell, vent_cell


def tank_foam(b, cx, cz, h):
    """A flammable tank's foam system (as nuclear/TankFittings.java lays it on a re-rated tank): two foam chambers on
    the top course as far apart as they go, from the north-east round, each with its foam line down the shell to the
    foot, and the earthing boss at the foot a quarter turn round from the first."""
    gw, _, gd = b.g.shape
    plate = TANK_PLATE

    def spots(y, need_clear_down):
        out = []
        for x in range(gw):
            for z in range(gd):
                if b.get(x, y, z) != plate:
                    continue
                for facing, dx, dz in SIDES.values():
                    ox, oz = x + dx, z + dz
                    if not (0 <= ox < gw and 0 <= oz < gd) or b.get(ox, y, oz) != 0:
                        continue
                    if (ox + 0.5 - cx) * dx + (oz + 0.5 - cz) * dz <= 0:
                        continue
                    if need_clear_down and any(b.get(ox, yy, oz) != 0 for yy in range(1, y)):
                        continue
                    out.append(((ox, oz), facing, math.atan2(oz + 0.5 - cz, ox + 0.5 - cx)))
        return out

    def best(cands, angle, not_near=None):
        found, off = None, 1e9
        for c in cands:
            if not_near is not None and abs(c[0][0] - not_near[0][0]) + abs(c[0][1] - not_near[0][1]) < 3:
                continue
            o = abs(math.atan2(math.sin(c[2] - angle), math.cos(c[2] - angle)))
            if o < off:
                found, off = c, o
        return found

    top = spots(h, True)
    first = best(top, math.pi / 4)
    assert first is not None, "foam chamber"
    second = best(top, first[2] + math.pi, first)
    for (x, z), facing, _ in [s for s in (first, second) if s is not None]:
        b.p(x, h, z, M(f"jugcraft:tank_foam_chamber[facing={facing}]", (200, 44, 44)))
        for y in range(1, h):
            b.p(x, y, z, M(f"jugcraft:tank_foam_line[facing={facing}]", (190, 40, 40)))
    boss = best(spots(1, False), first[2] + math.pi / 2)
    if boss is not None:
        (x, z), facing, _ = boss
        b.p(x, 1, z, M(f"jugcraft:tank_earthing_boss[facing={facing}]", (150, 110, 40)))


def outdoor_tank(s, cx, cz, r, h, name, key, service="none", outlet="w", inlet="n", ladder="e", manway="s", bund=False,
                 rating="general"):
    """A big outdoor storage tank as the storage tank MBS (tank_in) on its concrete foundation ring. A fuel tank
    (bund) stands on a concrete spill pad."""
    margin = 7 if bund else 5
    size = int(2 * r) + margin
    c = size / 2
    b = Building(key, name, size, size, h + 2)
    if bund:
        # A concrete spill pad round a fuel tank, its edge a darker kerb.
        b.disc(c, c, r + 3.3, 0, 0, CONCRETE)
        b.disc(c, c, r + 3.3, 0, 0, CONCRETE_DARK, inner=r + 2.5)
    tank_in(b, c, c, r, h, service, outlet, inlet, ladder, manway, rating=rating)
    finish_railings(b)
    s.add(b, int(cx - c), int(cz - c))
    return b


def fence(s, x0, z0, x1, z1, gate, step=None):
    """A double security fence round the protected area, lit, with a gate gap at x = gate on the south side. step
    (sx, sz): the east side stands out at x sx from the north side down to z sz, then steps back to x1 (an L: the
    transformer yard inside)."""
    if step is None:
        outer = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
    else:
        sx, sz = step
        outer = [(x0, z0), (sx, z0), (sx, sz), (x1, sz), (x1, z1), (x0, z1)]
    n = len(outer)
    # each edge's inward normal (the polygon runs clockwise seen from above, x east and z south: inside on the right)
    normals = []
    for i in range(n):
        (ax, az), (bx, bz) = outer[i], outer[(i + 1) % n]
        dx, dz = (bx > ax) - (bx < ax), (bz > az) - (bz < az)
        normals.append((-dz, dx))
    for off in (0, 3):
        # the edges moved in by off; each corner where its two edges' lines meet
        pts = []
        for i in range(n):
            e0, e1 = i - 1, i
            (ax, az) = outer[i]
            nx0, nz0 = normals[e0]
            nx1, nz1 = normals[e1]
            pts.append((ax + off * (nx0 or nx1), az + off * (nz0 or nz1)))
        for i, (ax, az) in enumerate(pts):
            bx, bz = pts[(i + 1) % n]
            for x in range(min(ax, bx), max(ax, bx) + 1):
                for z in range(min(az, bz), max(az, bz) + 1):
                    if z == z1 - off and az == bz and abs(x - gate) <= 4:
                        continue
                    for y in range(0, 3):
                        s.decor[(x, y, z)] = BARS
    for i, (ax, az) in enumerate(outer):
        bx, bz = outer[(i + 1) % n]
        if az == bz and ax != bx:
            for x in range(min(ax, bx), max(ax, bx) + 1, 16):
                for y in range(0, 6):
                    s.decor[(x, y, az - 2 * normals[i][1])] = M("minecraft:stone_brick_wall", (122, 122, 120)) if y < 5 else LANTERN


def parking(s, x0, z0, x1, z1):
    """A car park, set into the ground: asphalt with painted bay lines."""
    for x in range(x0, x1):
        for z in range(z0, z1):
            line = (x - x0) % 4 == 0 and (z - z0) % 14 not in (6, 7)
            s.decor[(x, -1, z)] = ROAD_LINE if line else ASPHALT


def site_sign(s, part, x, y, z, side, label):
    """A building's name sign hung on its outer wall: (x, z) along the wall in the part's frame, side n/s/e/w; the
    sign block stands just outside the wall. y is the height wanted: it moves down to the first plain wall (never on
    glass, a door or a roller door)."""
    _, px, pz, _ = next(e for e in s.parts if e[0] is part)
    w, _, d = part.size
    fx, fz = P.DIRS[SIDE[side]]
    wx = px + (x if side in "ns" else (-1 if side == "w" else w))
    wz = pz + (z if side in "ew" else (-1 if side == "n" else d))
    # The wall behind the sign, in the part's grid (its y counts from the bottom of the part, sunk sink below 0).
    gx = x if side in "ns" else (0 if side == "w" else w - 1)
    gz = z if side in "ew" else (0 if side == "n" else d - 1)
    while y > 1:
        gy = y + part.sink
        behind = int(part.g[gx, gy, gz]) if 0 <= gy < part.g.shape[1] else 0
        name = P.MATERIALS[behind][0] if behind else ""
        if behind and behind not in GLAZING and behind not in DOORISH and "roller_door" not in name and "glass" not in name:
            break
        y -= 1
    s.decor[(wx, y, wz)] = sign(label, SIDE[side], big=True)


def finish_lights(s):
    """Lights in every building, now that the plant's own equipment and pipes are placed (lights and their chains
    keep clear of them)."""
    occupied = set()
    for kind, ax, ay, az, facing in s.equipment:
        occupied |= P.equipment_cells(kind, ax, ay, az, facing)
    occupied |= set(s.lines)
    for part, px, pz, rot in s.parts:
        if not isinstance(part, Building):
            continue
        blocked = {(x - px, y + part.sink, z - pz) for (x, y, z) in occupied}
        for kind, ax, ay, az, facing in part.props:
            blocked |= P.equipment_cells(kind, ax, ay, az, facing)
        for step, y_min, y_max in getattr(part, "light_steps", [(4, None, None)]):
            part.light_ceilings(step=step, blocked=blocked, y_min=y_min, y_max=y_max)
        emergency_light_feeds(part)


def emergency_light_feeds(b):
    """Each emergency light's conduit (round 25b; Justin: cables must go to something): down the wall from the ceiling
    into the top of its cabinet. Laid after the ceiling lights, which would otherwise not go where it stands."""
    for name, x, y, z, facing in list(b.seats):
        if name != "emergency_light":
            continue
        k = y + 1
        while b.get(x, k, z) == 0 and int(b.get(x, k + 1, z)) not in CEILINGS:
            b.furniture("emergency_light_conduit", x, k, z, facing)
            k += 1
        assert b.get(x, k, z) == 0 and int(b.get(x, k + 1, z)) in CEILINGS, ("emergency light conduit", b.key, x, y, z)
        b.furniture("emergency_light_conduit", x, k, z, facing)


# ------------------------------------------------------------------------------------------------- the site

def medium_site():
    """The whole Medium Nuclear Plant, laid out like a real single-unit station: the power block by the lake (pump
    house, reactor building, turbine hall), the auxiliary, fuel and control buildings round the reactor, the diesel
    building, the transformer yard and switchyard to the east, all inside a double security fence; outside it the
    administration building, the gatehouse, parking, the warehouse and the two cooling towers."""
    s = P.Site("medium_pwr", "Medium Plant (4-loop PWR)", 372, 330, water=(0, -40, 372, 0))
    # Power block.
    cont = containment(s, 60, 40)
    sx, sz = place_primary(s, 60, 40)
    # The reactor building's inside (22 October 2026): the approved machines as compiled sites (tools/reactor_site.py),
    # over the containment's blocks.
    import reactor_site as RS
    rb = RS.reactor_part()
    s.add(rb, *rb.at)
    hall = turbine_hall(s, 124, 38)
    # (The buildings' ground floors are level with the grass, at y -1: what stands on them starts at y 0.)
    s.eq("turbine_generator", 160, 54, "east", y=12)
    s.eq("condenser", 171, 54, "east", y=6)          # condenser shell 1A, under LP 1 (turbine hall part 2)
    s.eq("feedwater_pump", 150, 66, y=0)
    # Main steam (turbine hall part 1): out of the containment in the pipe bridge, in through the hall's west wall
    # high up, along it to clear the roller door, down the wall to under the operating floor, east under it and
    # through the pedestal to the steam chests, up into the right-hand chest's main steam lead; feedwater up from the
    # pump into it.
    s.line(P.STEAM, (sx + 1, 21, sz), (125, 21, sz))
    s.line(P.STEAM, (125, 21, sz + 1), (125, 21, sz + 1))
    s.line(P.STEAM, (125, 9, sz + 1), (125, 20, sz + 1))
    s.line(P.STEAM, (126, 9, sz + 1), (164, 9, sz + 1))
    s.line(P.STEAM, (164, 9, 64), (164, 9, sz))
    s.line(P.STEAM, (164, 10, 64), (164, 11, 64))
    s.line(P.STEAM, (152, 3, 67), (152, 9, 67))
    s.gallery(111, 123, sz - 2, sz + 2, 19, 23, [(112, sz - 2), (112, sz + 2), (122, sz - 2), (122, sz + 2)])
    # Isolated phase bus from under the generator east under the operating floor to the end wall, where the transformer
    # yard's bus takes it on (its bus port: the yard's controller finds the generator through it, tools/yard_sites.py).
    s.line(P.BUS, (203, 9, 59), (203, 11, 59))
    s.line(P.BUS, (204, 9, 59), (223, 9, 59))
    # Around the reactor.
    aux_building(s, 30, 95)
    # The radwaste building (round 14) west of the auxiliary building, by its health physics entrance.
    import radwaste_store
    radwaste_store.place(s, 5, 98)
    fuel_building(s, 100, 95)
    ctrl = control_building(s, 136, 88)
    control_building_feed(s, 136, 88)
    control_building_mcc_runs(s, 136, 88)
    cx, cz, cy = ctrl
    s.eq("reactor_control_console", cx, cz, "north", y=cy)
    diesel, fills = diesel_building(s, 180, 90)
    outdoor_tank(s, 16, 70, 6.5, 14, "Refuelling Water Storage Tank", "rwst_tank", service="refuelling_water")
    outdoor_tank(s, 232, 125, 5.5, 12, "Condensate Storage Tank", "condensate_tank", service="condensate")
    # The diesel generators' fuel oil storage tank on its spill pad south of the diesel building (where the tanker
    # fills it). Its line keeps out of the way (Justin's round 13): off the outlet, straight down through the pad, then
    # buried in a trench flush with the grass (pipe in concrete) north to the building and along its south wall to
    # where each room's line goes in under the wall. A smooth stone bed under the trench gives it its look.
    outdoor_tank(s, 200, 125, 4.5, 8, "Diesel Fuel Oil Storage Tank", "diesel_fuel_tank", service="diesel_fuel", outlet="n", inlet="e",
                 ladder="w", manway="s", bund=True, rating="flammable")
    s.decor[(200, 1, 119)] = PIPE
    south = 90 + diesel.size[2]
    trench = [(200, 0, 119)] + [(200, -1, z) for z in range(south, 120)]
    trench += [(x, -1, south) for x in range(min(180 + min(fills), 200), max(180 + max(fills), 200) + 1)]
    for x, y, z in trench:
        s.decor[(x, y, z)] = WALL_PIPE
        if y == -1:
            s.decor[(x, -2, z)] = SMOOTH
    # Electrical (transformer yard and switchyard build, 20 October 2026; compiled sites, tools/yard_sites.py): the
    # main transformer yard along the turbine hall's east wall (its bus from the hall's), the 345 kV switchyard east of
    # it, outside the protected area.
    import yard_sites as YS
    for part in (YS.transformer_yard_part(), YS.switchyard_part()):
        s.add(part, *part.at)
    s.yard_ports = [(YS.HALL_FACE, 9, 59)]
    # The water works to the detail standard (towers and pump houses build, 19 October 2026; compiled sites,
    # tools/water_works.py). The lake intake (make-up and service water, tools/ww_intake.py) where the old pump house
    # stood: its shore (d 2) on the plant's shore, its hall's middle at x 170.
    import water_works as WW
    import ww_circ
    import ww_intake
    import ww_tower
    place_site(s, ww_intake.intake_part(WW.cells_of("lake_intake")), "intake", 9, 2, 170, 0)
    # The towers, their axes where the old ones stood, their outlets toward the plant.
    t = ww_tower.tower_part(WW.cells_of("ct_medium"))
    ctrls = [place_tower(s, t, tx, 270) for tx in (120, 250)]
    # The circulating water pump house (tools/ww_circ.py) between the towers, on the trench from them to the condenser:
    # its controller touches the trench, its pumps move the towers' water.
    place_site(s, ww_circ.circ_part(WW.cells_of("cw_pump_house")), "circ", *CW_HOUSE)
    # Circulating water: the towers' outlets -> the trench -> the condenser (riser up through the floor and its foundation).
    s.line(P.CONDUIT, (172, 0, 59), (172, 5, 59))
    s.conduit((172, 59), (172, 196), (ctrls[0][0], 196), (ctrls[0][0], ctrls[0][2] - 1))
    s.conduit((172, 196), (ctrls[1][0], 196), (ctrls[1][0], ctrls[1][2] - 1))
    # Site: fence round the protected area, admin, gatehouse, parking, warehouse; roads.
    import medium_admin as A
    admin = A.admin_building(s, 40, 160)
    gate = A.gatehouse(s, 128, 157)
    # Paths, level with the grass, from the road to every entrance and from the gatehouse to the car park.
    A.walkway(s, 69, 156, 72, 159)
    A.walkway(s, 140, 156, 143, 156)
    A.walkway(s, 139, 169, 142, 171)
    A.walkway(s, 143, 169, 159, 171)
    ware = warehouse(s, 262, 110)
    # (the protected area takes in the transformer yard: its east side steps out round it, north of the diesel building)
    fence(s, 0, 26, 250, 146, 123, step=(286, 96))
    parking(s, 160, 165, 230, 196)
    s.roads += [(0, 150, 372, 156), (120, 82, 127, 215), (100, 84, 240, 87)]
    # Building name signs.
    site_sign(s, hall, 20, 15, 0, "s", "turbine_building")
    site_sign(s, ctrl_part(s, "control_building"), 20, 5, 0, "s", "control_building")
    site_sign(s, admin, 30, 4, 0, "n", "administration")
    site_sign(s, gate, 5, 3, 0, "s", "security")
    site_sign(s, gate, 5, 3, 0, "n", "security")
    # The warehouse's apron along its front (the Goods In and Goods Out windows), and paths from the office door and the
    # stair tower's exit to the road.
    A.walkway(s, 262, 137, 306, 140)
    A.walkway(s, 281, 141, 283, 149)
    A.walkway(s, 302, 141, 305, 149)
    site_sign(s, diesel, 23, 8, 0, "s", "diesel_building")
    site_sign(s, ctrl_part(s, "aux_building"), 30, 4, 0, "s", "auxiliary_building")
    site_sign(s, ctrl_part(s, "fuel_building"), 0, 15, 23, "e", "fuel_building")
    finish_lights(s)
    for part, *_ in s.parts:
        finish_railings(part)
        finish_glass(part)
    return s


# Where the circulating water pump house stands: its site point (x, z) at the plant's (x, z) (its controller's middle at
# the trench along z 196).
CW_HOUSE = (12.5, 10.5, 185.5, 197.5)


def place_site(s, part, works, sx, sz, px, pz):
    """A compiled pump house (ww_intake.intake_part, ww_circ.circ_part) with its site point (sx, sz) at the plant's (px,
    pz) (the part stands turned half round in its file: site x and z run the other way). Returns its controller's place."""
    x0, z0 = int(round(px - part.origin[0] + sx)), int(round(pz - part.origin[2] + sz))
    s.add(part, x0, z0)
    cx, cy, cz = part.controller
    at = (x0 + cx, cy - part.sink, z0 + cz)
    s.works.append((*at, works))
    return at


def place_tower(s, part, ax, az):
    """A compiled cooling tower (ww_tower.tower_part) with its axis at (ax, az): returns its controller's place."""
    x0, z0 = ax - int(round(part.axis[0])), az - int(round(part.axis[2]))
    s.add(part, x0, z0)
    cx, cy, cz = part.controller
    at = (x0 + cx, cy - part.sink, z0 + cz)
    s.towers.append((*at, "medium"))
    return at


def ctrl_part(s, key):
    return next(p for p, *_ in s.parts if p.key == key)
