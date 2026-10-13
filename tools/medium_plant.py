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


def door_state(kind, facing, half, hinge="left"):
    m = M(f"jugcraft:{kind}_door[facing={facing},half={half},hinge={hinge},open=false,powered=false]",
          (200, 200, 200) if kind == "steel" else (150, 190, 200))
    DOORISH.add(m)
    return m


def row_state(name, facing, left, right, variant=None):
    v = "" if variant is None else f",variant={variant}"
    return M(f"jugcraft:{name}[facing={facing},left={'true' if left else 'false'},right={'true' if right else 'false'}{v}]", (120, 100, 80))


def multi_state(name, facing, part, variant=None):
    extra = "" if variant is None else f",variant={variant}"
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


def steel_ladder(facing, cage, top):
    return M(f"jugcraft:caged_ladder[cage={'true' if cage else 'false'},facing={facing},top={'true' if top else 'false'},"
             f"waterlogged=false]", (170, 176, 178))


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
STACKING = {"server_rack", "relay_cabinet", "locker", "pallet_rack", "pallet_rack_drums"}


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
    def door(self, x, y, z, facing, kind="steel", double=False, label=None, sign_side=None):
        """A door (steel, or glass for offices and entrances) in a wall cell, opening by hand. The door leaf sits
        on the side of its block away from facing: give facing as the way you walk in through it to have the leaf
        flush with the face you walk in from. double: a pair, the second to the facing's left. label: a room sign
        beside the door on the side you come from."""
        dx, dz = P.DIRS[facing]
        lx, lz = P.DIRS[CCW[facing]]
        leaves = [(x, z, "right" if double else "left")]
        if double:
            leaves.append((x + lx, z + lz, "left"))
        for (cx, cz, hinge) in leaves:
            self.p(cx, y, cz, door_state(kind, facing, "lower", hinge))
            self.p(cx, y + 1, cz, door_state(kind, facing, "upper", hinge))
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

    def panel(self, name, x, y, z, facing, width):
        """A floor-standing panel two blocks high and width wide (plant_panels.py), its left block (as you face it) at
        x, z."""
        rx, rz = P.DIRS[P.CLOCKWISE[facing]]
        for c in range(width):
            for row in range(2):
                self.p(x + rx * c, y + row, z + rz * c, multi_state(name, facing, row * width + c))
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

    def prop(self, kind, x0, y, z0, facing="north"):
        super().prop(kind, x0, y + self.base, z0, facing)

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
    b.door(x, 1, z, facing, kind=kind, double=True)
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

    Inside, like a four-loop PWR: the vessel in its reactor cavity behind the primary shield wall; each steam
    generator and its coolant pump in a compartment behind the crane wall's radial walls; the pressurizer in its own
    compartment; grating levels round the steam generators; the operating floor at +16 with the refuelling cavity
    over the vessel (the head and drive forest show in it) and the refuelling machine on rails; the polar crane on its
    ring girder high up; the annulus between the crane wall and the liner with ladders up to the operating floor; the
    personnel airlock and the equipment hatch at ground level. Yellow safety railing round every opening."""
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
    b.disc(c, c, inner, 0, 0, EPOXY)
    cv = int(c)
    floor_y = 16
    for y in range(1, floor_y):
        b.ring(c, c, 6.5, y, 1.2, SHIELD)
    crane_r = 19.5
    for y in range(1, floor_y):
        b.ring(c, c, crane_r, y, 1.0, CONCRETE)
    for y in range(1, floor_y):
        b.box(cv, y, int(c + 7), cv, y, int(c + crane_r), CONCRETE)
        b.box(int(c - crane_r), y, cv, int(c - 7), y, cv, CONCRETE)
        b.box(int(c + 7), y, cv, int(c + crane_r), y, cv, CONCRETE)
    # Doors through the crane wall into each loop compartment from the annulus (steel doors, 2 wide).
    for k, a in enumerate((math.pi / 4, 3 * math.pi / 4, 5 * math.pi / 4, 7 * math.pi / 4)):
        x, z = int(c + math.cos(a) * crane_r), int(c + math.sin(a) * crane_r)
        b.clear(x - 1, 1, z - 1, x + 1, 3, z + 1)
        # A ladder inside the compartment up the crane wall to the grating level, through a hole in the grating.
        a2 = a + 0.3
        lx, lz = c + math.cos(a2) * (crane_r - 1.7), c + math.sin(a2) * (crane_r - 1.7)
        ox_, oz_ = math.cos(a2), math.sin(a2)
        if abs(ox_) > abs(oz_):
            face = "west" if ox_ > 0 else "east"
            wx, wz = (1 if ox_ > 0 else -1), 0
        else:
            face = "north" if oz_ > 0 else "south"
            wx, wz = 0, (1 if oz_ > 0 else -1)
        ix, iz = int(lx), int(lz)
        while b.g[ix + wx, 2, iz + wz] == 0:
            ix, iz = ix + wx, iz + wz
        b.ladders = getattr(b, "ladders", []) + [(ix, iz, face, ix + wx, iz + wz)]
    # Grating levels round the steam generators (+8) inside the compartments.
    gy8 = 8
    for x in range(size):
        for z in range(size):
            dx, dz = x + 0.5 - c, z + 0.5 - c
            rr = math.hypot(dx, dz)
            if 8.5 < rr < crane_r - 0.6 and abs(x - cv) > 1 and abs(z - cv) > 1 and b.g[x, gy8, z] == 0:
                b.g[x, gy8, z] = GRATE
    for ix, iz, face, wx, wz in b.ladders:
        for y in range(1, gy8 + 1):
            b.box(wx, y, wz, wx, y, wz, CONCRETE)
            b.g[ix, y, iz] = LADDER[face]
    # Operating floor (+16).
    for x in range(size):
        for z in range(size):
            if math.hypot(x + 0.5 - c, z + 0.5 - c) < inner - 0.2:
                b.g[x, floor_y, z] = EPOXY
    cav = 6
    b.clear(cv - cav, floor_y, cv - cav, cv + cav, floor_y, cv + cav)
    # The refuelling cavity's stainless-lined walls up to the floor; railing round it.
    for y in range(floor_y - 3, floor_y + 1):
        for x in range(cv - cav - 1, cv + cav + 2):
            for z in (cv - cav - 1, cv + cav + 1):
                b.g[x, y, z] = LINER
        for z in range(cv - cav - 1, cv + cav + 2):
            for x in (cv - cav - 1, cv + cav + 1):
                b.g[x, y, z] = LINER
    b.clear(cv - cav, floor_y, cv - cav, cv + cav, floor_y, cv + cav)
    # Steam generator shafts: the operating floor open round each steam generator, railed.
    span = 10
    holes = [(cv - cav, cv - cav, cv + cav, cv + cav)]
    for gx in (cv - span, cv + span):
        for gz in (cv - span, cv + span):
            b.clear(gx - 4, floor_y, gz - 4, gx + 4, floor_y, gz + 4)
            holes.append((gx - 4, gz - 4, gx + 4, gz + 4))
    b.clear(cv - 2, floor_y, cv - 12, cv + 2, floor_y, cv - 8)
    holes.append((cv - 2, cv - 12, cv + 2, cv - 8))
    # The pressurizer's compartment walls.
    for y in range(1, 24):
        b.box(cv - 3, y, cv - 13, cv + 3, y, cv - 13, CONCRETE)
        b.box(cv - 3, y, cv - 13, cv - 3, y, cv - 8, CONCRETE)
        b.box(cv + 3, y, cv - 13, cv + 3, y, cv - 8, CONCRETE)
    b.clear(cv - 3, 1, cv - 10, cv - 3, 2, cv - 10)
    # Refuelling machine rails on the cavity's side walls.
    for z in range(cv - cav - 4, cv + cav + 5):
        for x in (cv - cav - 1, cv + cav + 1):
            b.g[x, floor_y + 1, z] = RAIL_NS
    # The polar crane's ring girder (a concrete corbel round the liner), lights under it.
    gy = 40
    b.disc(c, c, inner, gy, gy + 1, CONCRETE, inner=inner - 2)
    for k in range(24):
        a = 2 * math.pi * k / 24
        x, z = int(c + math.cos(a) * (inner - 1.5)), int(c + math.sin(a) * (inner - 1.5))
        if b.g[x, gy - 1, z] == 0 and b.g[x, gy, z] != 0:
            b.g[x, gy - 1, z] = LIGHT
    # Ladders up the outside of the crane wall in the annulus, west and east, to the operating floor.
    for x, face in ((int(c - crane_r) - 2, "west"), (int(c + crane_r) + 1, "east")):
        for y in range(1, floor_y + 1):
            b.g[x, y, cv + 3] = LADDER[face]
    # Personnel airlock (west): a steel box through the wall with a steel door at each end.
    b.box(0, 1, cv - 2, 4, 4, cv + 2, LINER)
    b.clear(1, 1, cv - 1, 3, 3, cv + 1)
    b.door(0, 1, cv, "east", double=False)
    b.door(4, 1, cv, "east", double=False)
    # Equipment hatch (south): a big steel plug in a concrete collar, warning lights.
    b.box(cv - 4, 1, size - 4, cv + 4, 10, size - 1, CONCRETE)
    b.box(cv - 3, 1, size - 1, cv + 3, 8, size - 1, LINER)
    b.box(cv - 3, 1, size - 4, cv + 3, 8, size - 2, 0)
    # (Its amber beacons hang on the wall either side, outside the part: site decor, below.)
    # Railing round every opening in the operating floor.
    for x0, z0, x1, z1 in holes:
        b.rail_round(x0, z0, x1, z1, floor_y + 1)
    b.prop("polar_crane", cv - 20, gy + 2, cv - 2, "north")
    b.prop("refueling_machine", cv - cav - 1, floor_y + 2, cv - cav, "north")
    # Lights hung from the ring girder round the operating floor.
    for k in range(24):
        a = 2 * math.pi * k / 24
        x, z = int(c + math.cos(a) * (inner - 1.5)), int(c + math.sin(a) * (inner - 1.5))
        if b.g[x, gy - 1, z] == LIGHT:
            for y in range(floor_y + 6, gy):
                b.g[x, y, z] = CHAIN
            b.g[x, floor_y + 5, z] = LIGHT
    b.light_steps = [(4, None, floor_y + 6), (5, floor_y + 7, None)]
    # Amber beacons either side of the equipment hatch, on the face of its collar.
    for x in (cv - 4, cv + 4):
        s.decor[(ox + x, b.site_y(9), oz + size)] = beacon("amber", "south")
    s.add(b, ox, oz)
    return b


def place_primary(s, ox, oz):
    """The working reactor equipment in the containment at (ox, oz): the four loops of tools/nuclear_plants.py."""
    return P.four_loops(s, ox, oz, 25.5, 10, floor=-1)


# ------------------------------------------------------------------------------------------------- turbine hall

def turbine_hall(s, ox, oz):
    """The turbine building, 100 x 44 and 36 tall.

    The turbine-generator stands on its concrete pedestal at the operating floor (+12); under its low-pressure end
    the condenser sits on its foundation in the pedestal's condenser bay (a door from the ground floor). The ground
    floor has the feedwater heater trains, the condensate pumps and the feedwater pump, a pipe rack along the north
    wall, and plant rooms along the south wall (switchgear, lube oil, workshop, turbine control). The laydown bay at
    the west end is open to the crane, closed by a roller door; stairs at both ends; the overhead crane on its runway."""
    W, D, H = 100, 44, 36
    b = Building("pwr_turbine_hall", "Turbine Building", W, D, H, grounded=True)
    b.shell_walls(CONCRETE, step=10)
    for y in range(13, H + 1):
        for x in range(1, W - 1):
            if x % 10:
                b.p(x, y, 0, CLADDING)
                b.p(x, y, D - 1, CLADDING)
        for z in range(1, D - 1):
            if z % 10:
                b.p(0, y, z, CLADDING)
                b.p(W - 1, y, z, CLADDING)
    b.windows(26, 32, "ns", step=10, glass=BLAST)
    b.windows(4, 6, "s", step=10, skip=range(40, 99))
    b.b(1, 0, 1, W - 2, 0, D - 2, EPOXY)
    # Operating floor at +12 east of the laydown bay; railing along its edge over the bay.
    b.b(15, 12, 1, W - 2, 12, D - 2, EPOXY)
    for z in range(1, D - 1):
        b.rail(15, 13, z, "west")
    # The pedestal: a massive concrete table under the turbine with the condenser bay inside.
    b.b(34, 1, 14, 87, 11, 28, CONCRETE_DARK)
    b.c(44, 1, 15, 53, 11, 27)
    b.b(46, 1, 16, 51, 6, 27, CONCRETE)  # the condenser's foundation
    b.door(45, 1, 28, "north", double=True, label="pump_room")
    # Column lines under the operating floor.
    for x in range(20, W - 1, 10):
        for z in (8, D - 9):
            if b.get(x, 1, z) == 0:
                b.b(x, 1, z, x, 11, z, COLUMN)
    # Crane runway beams along both long walls.
    for x in range(1, W - 1):
        b.p(x, 30, 1, FRAME)
        b.p(x, 30, D - 2, FRAME)
    # The laydown bay's roller door (west end) and personnel doors.
    for z in range(17, 27):
        for y in range(1, 10):
            b.p(0, y, z, roller("east"))
    b.b(0, 10, 16, 0, 10, 27, CONCRETE_DARK)
    entrance(b, 0, 31, "w", kind="steel")
    entrance(b, W - 1, 6, "e", kind="steel")
    # Plant rooms along the south wall on the ground floor.
    b.wall_x(D - 12, 40, W - 2, 1, 11, CONCRETE)
    for x in (55, 70, 85):
        b.wall_z(x, D - 11, D - 2, 1, 11, CONCRETE)
    for x, label in ((47, "switchgear_room"), (62, "lube_oil_room"), (77, "workshop"), (91, "turbine_control_room")):
        b.door(x, 1, D - 12, "south", double=True, label=label)
    b.prop("switchgear_lineup", 42, 1, D - 4, "south")
    b.prop("switchgear_lineup", 44, 1, D - 9, "north")
    b.prop("storage_tank", 57, 1, D - 7, "north")
    b.prop("pump_skid", 64, 1, D - 6, "west")
    for x in range(64, 69):
        b.stack("pallet_rack_drums", x, 1, D - 2, "south", n=1)
    for x in range(72, 84, 2):
        b.stack("pallet_rack", x, 1, D - 2, "south", n=2)
    for x in (88, 92):
        b.desk(x, 1, D - 3, "south")
    for x in range(87, 97):
        b.stack("relay_cabinet", x, 1, D - 11, "north", n=2) if x not in (90, 91, 92) else None
    # Stairwells at both ends, ground to the operating floor.
    b.stairwell(1, 1, [0, 12], door_side="s")
    for z in (3, 8, 13):
        b.p(8, 4, z, LANTERN)
    b.stairwell(W - 10, 13, [0, 12], door_side="s")
    # Feedwater heater trains on the ground floor north of the pedestal, condensate pumps by the condenser bay.
    for x0 in (18, 56):
        for z in (2, 6, 10):
            b.prop("feedwater_heater", x0, 1, z, "east")
    for x in (30, 35):
        b.prop("pump_skid", x, 1, 29, "north")
    # The pipe rack along the north wall (pipes on brackets, two tiers).
    for x in range(1, W - 1):
        if b.get(x, 8, 1) == 0:
            b.p(x, 8, 1, PIPE)
        if b.get(x, 10, 1) == 0:
            b.p(x, 10, 1, PIPE)
    # Moisture separator reheaters on the operating floor beside the turbine, the overhead crane.
    b.prop("moisture_separator_reheater", 36, 13, 6, "east")
    b.prop("moisture_separator_reheater", 36, 13, 31, "east")
    b.prop("overhead_crane", 9, 31, 2, "east")
    b.hangers = []
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- control building

def wall_screen(b, name, x, y, z, facing):
    """A wall screen three blocks across and two high (plant_office2: status, plant map, training), its left block as
    you face it at x, z and its bottom at y, on the wall it faces."""
    rx, rz = P.DIRS[P.CLOCKWISE[facing]]
    for r in range(2):
        for c in range(3):
            b.p(x + rx * c, y + r, z + rz * c, multi_state(name, facing, r * 3 + c))


def dc_run(b, battery_ends, board, y, z, facing):
    """The DC power cables from a station battery to its switchboard (plant_panels.RISERS): a riser up the wall from
    the end of each rack (where its cables come out of the top) to a cable tray under the ceiling at y, the tray along
    the wall at z to the switchboard, and a riser down into the switchboard's top. battery_ends: the x of each rack's
    terminal end; board: the x of the switchboard block the cables go down into; the racks and the board stand against
    the same wall (facing it), two blocks high from the floor below y - 4."""
    floor = y - 4
    ends = sorted(set(battery_ends) | {board})
    for x in ends:
        for k in range(floor + 2, y):
            b.p(x, k, z, furniture("dc_cable_riser", facing))
        b.p(x, y, z, furniture("dc_cable_riser_top", facing))
    for x in range(ends[0] + 1, ends[-1]):
        if x not in ends:
            b.p(x, y, z, furniture("dc_cable_tray", facing))


def feeder_conduits(b, ax, ay, az, facing, upper):
    """A load centre's feeder conduits (round 25b; Justin: the conduits on top of the switchgear were "going nowhere"):
    from the hub over each in-use upper feeder's section (tools/control_boards.py LC_CONDUIT_*), one 4-inch conduit
    straight up under the ceiling, through the slab and up through the floor of the cable spreading room above, to its
    lowest tray behind the run, where it turns in under the tray (plant_office2 feeder_conduit_*). ax, ay, az: the
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
        tray = next(y for y in range(upper + 1, upper + 6) if b.get(x, y, z + 1) and P.MATERIALS[int(b.get(x, y, z + 1))][0].startswith("jugcraft:cable_tray"))
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
MCC_RUNS = ([(15, 0, z) for z in range(1, 14)]                                       # 1A CHGR 1 to the charger at x 15
            + [(14, 1, 14), (14, 0, 14), (13, 0, 14)] + [(13, 0, z) for z in range(1, 14)]   # 1A CHGR 2, x 13
            + [(26, 0, z) for z in range(19, 29)]                                    # 1B CHGR 1 to the charger at x 26
            + [(27, 1, 18), (27, 0, 18), (24, 0, 28)])                               # 1B CHGR 2 into the wall, down; up at x 24
MCC_DEEP_RUNS = ([(27, -1, 18), (27, -2, 18), (26, -2, 18), (25, -2, 18), (24, -2, 18)]
                 + [(24, -2, z) for z in range(19, 29)] + [(24, -1, 28)])


def control_building_mcc_runs(s, ox, oz, sink=1):
    """MCC 1B's CHGR 2 run where it passes under the others, below the slab (round 26): in the site's ground, the
    building sunk a block (its slab is the site's y -1)."""
    for x, y, z in MCC_DEEP_RUNS:
        s.decor[(ox + x, y - sink, oz + z)] = CABLE


def control_building_feed(s, ox, oz):
    """The buried feed from the main transformer to the control building's load centres (round 25): down from under
    the transformer, west two blocks under the grass to the building, then under the building a block deeper (clear of
    the charger runs in its slab) to below each load centre's riser."""
    run = [(230, 0, 59), (230, -1, 59)] + [(x, -2, 59) for x in range(158, 231)] + [(158, -2, z) for z in range(60, oz + 2)]
    rx, rz = ox + SLAB_RISERS[1][0], oz + SLAB_RISERS[1][1]
    run += [(158, -3, oz + 1)] + [(x, -3, oz + 1) for x in range(159, rx + 1)] + [(rx, -3, z) for z in range(oz + 2, rz + 1)]
    run += [(rx, -2, rz)]
    assert (ox + SLAB_RISERS[0][0], oz + SLAB_RISERS[0][1]) == (158, oz + 1)
    for cell in run:
        s.decor[cell] = CABLE
    return run


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
    for x in list(range(2, 8)) + list(range(10, 16)):
        for y in (L3 + 2, L3 + 3, L3 + 4):
            b.p(x, y, 0, LOUVRE)
    for z in range(17, 23):
        for y in (L3 + 2, L3 + 3, L3 + 4):
            b.p(W - 1, y, z, LOUVRE)
    ground_floor(b, W, D)
    first_floor(b, W, D, L1)
    console = second_floor(b, W, D, L2)
    top_floor(b, W, D, L3)
    # The switchgear room's load centres' feeder conduits, up into the cable spreading room's trays (round 25b).
    for kind, ax, ay, az, facing in list(b.props):
        if kind == "switchgear_lineup" and (ax, az) in ((22, 2), (29, 9)):
            feeder_conduits(b, ax, ay - b.base, az, facing, L1)
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
    b.panel("station_battery", 1, 1, 1, "north", 4)
    b.panel("station_battery", 5, 1, 1, "north", 4)
    dc_run(b, [4, 8], 11, 5, 1, "north")
    b.door(10, 1, 6, "west", label="battery_room_1a")
    b.furniture("fire_extinguisher", 1, 2, 7, "west")
    # ---- Remote Shutdown Room (x 1..9, z 10..13; round 24): the remote shutdown panel standing along its north wall
    # (Justin: no bookcase; the panel must work; "more realistic with detail"), a rubber mat in front of it, the
    # battery emergency lights over it (the room must stay lit if the plant loses power), against the east wall an
    # operator console, an extinguisher by the door.
    b.prop("remote_shutdown_panel", 2, 1, 10, "north")
    for x in range(2, 8):
        b.p(x, 1, 11, MAT)
    for x in (3, 6):
        b.furniture("emergency_light", x, 4, 10, "north")
    b.row(("operator_console", 3), 9, 1, 10, "east", tops=(2, 3, 0))
    b.p(8, 1, 11, furniture("operator_chair", "east"))
    b.door(5, 1, 14, "north", label="remote_shutdown")
    b.furniture("fire_extinguisher", 1, 2, 13, "west")
    # ---- DC Equipment Room 1A (x 11..19, z 1..13): along the north wall the 125 VDC switchboard (the battery's cables
    # come down into it), the chargers and the vital inverters, a charger never beside a charger (their feeds in the
    # slab would join); along the south wall the 480 V motor control centre feeding the chargers (round 26, 8a; its
    # first section, the incoming, at x 17).
    b.panel("dc_switchboard", 11, 1, 1, "north", 2)
    b.panel("edg_battery_charger", 13, 1, 1, "north", 1)
    b.panel("vital_inverter", 14, 1, 1, "north", 1)
    b.panel("edg_battery_charger", 15, 1, 1, "north", 1)
    b.panel("vital_inverter", 16, 1, 1, "north", 1)
    for z in (4, 5):
        b.stack("relay_cabinet", 19, 1, z, "east", n=2)
    b.prop("mcc_lineup", 14, 1, 13, "south")
    b.door(11, 1, 14, "north", label="dc_equipment_1a")
    b.furniture("fire_extinguisher", 19, 2, 8, "east")
    # ---- Switchgear Room (x 21..29, z 1..13; round 25): the 480 V load centres of both trains, face to face across the
    # aisle, walked round at the west end (Justin: "realistic switchgear ... less simple and cartoony", and it must
    # hold charge and work): rubber mats along their fronts, the battery emergency lights over the aisle, an
    # extinguisher (no locker: Justin, round 25b). Their feeder conduits go up into the cable spreading room
    # (feeder_conduits, after the floors are in).
    b.prop("switchgear_lineup", 22, 1, 1, "north")
    b.prop("switchgear_lineup", 22, 1, 9, "south")
    for x in range(22, 30):
        b.p(x, 1, 3, MAT)
        b.p(x, 1, 8, MAT)
    b.furniture("emergency_light", 21, 4, 4, "west")
    b.furniture("emergency_light", 29, 4, 6, "east")
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
    # The security counter in the hall (z 23..25), facing the way in, the officers' chairs behind it clear of the
    # wall; the camera desk inside the gates.
    b.row(("reception_desk", 3), 10, 1, 25, "west", seat="operator_chair")
    b.furniture("cctv_desk", 12, 1, 20, "east")
    b.p(11, 1, 20, furniture("operator_chair", "east"))
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
    # ---- Locker room (x 14..17): lockers along both walls, a bench down the middle.
    for z in range(20, D - 1):
        b.stack("locker", 17, 1, z, "east", n=2)
        if z not in range(22, 27):
            b.stack("locker", 14, 1, z, "west", n=2)
    b.row(("locker_bench", 5), 15, 1, 22, "east")
    b.door(16, 1, 18, "south", label="locker_room")
    # ---- DC Equipment Room 1B (x 19..28) and Battery Room 1B (x 30..38): as Train A, along the south wall; MCC 1B
    # along the north wall (its incoming section at x 24, over the feed from load centre B).
    b.panel("dc_switchboard", 28, 1, D - 2, "south", 2)
    b.panel("edg_battery_charger", 26, 1, D - 2, "south", 1)
    b.panel("vital_inverter", 25, 1, D - 2, "south", 1)
    b.panel("edg_battery_charger", 24, 1, D - 2, "south", 1)
    b.panel("vital_inverter", 23, 1, D - 2, "south", 1)
    b.prop("mcc_lineup", 24, 1, 19, "north")
    for z in (22, 23):
        b.stack("relay_cabinet", 19, 1, z, "west", n=2)
    b.door(22, 1, 18, "south", double=True, label="dc_equipment_1b")
    b.furniture("fire_extinguisher", 19, 2, 26, "west")
    b.panel("station_battery", 37, 1, D - 2, "south", 4)
    b.panel("station_battery", 33, 1, D - 2, "south", 4)
    dc_run(b, [34, 30], 28, 5, D - 2, "south")
    b.door(34, 1, 18, "south", double=True, label="battery_room_1b")
    b.furniture("fire_extinguisher", W - 2, 2, 21, "east")


def first_floor(b, W, D, y):
    """The first floor (walked at y + 1): the cable spreading room under the control room (tiers of cable tray, risers
    up into the floor above), and south of the corridor the relay room and the plant computer room on its raised floor."""
    y0, y1 = y + 1, y + 5
    b.wall_x(14, 1, W - 10, y0, y1)
    b.wall_x(18, 1, W - 2, y0, y1)
    b.wall_z(30, 1, 13, y0, y1)
    b.wall_z(19, 19, D - 2, y0, y1)
    # ---- Cable spreading room (x 1..29, z 1..13): three tiers of tray along the room, risers up into the control room.
    for z in (2, 5, 8, 11):
        for k, ty in enumerate((y0 + 2, y0 + 3, y0 + 4)):
            if z == 11 and k == 0:
                continue
            b.tray_run(1, z, 29, z, ty, "north")
    for x in (6, 12, 18, 24):
        for k in range(y0, y1 + 1):
            b.p(x, k, 1, furniture("dc_cable_riser", "north"))
    for x in range(3, 30, 4):
        for z in (3, 7, 10, 13):
            b.p(x, y1, z, LIGHT)
    b.door(15, y0, 14, "north", double=True, label="cable_spreading_room")
    b.furniture("fire_extinguisher", 1, y0 + 1, 12, "west")
    # ---- Relay room (x 1..18, z 19..28): relay cabinets back to back in rows.
    for x in range(2, 18):
        if x in (8, 9):
            continue
        for z, f in ((21, "south"), (22, "north"), (25, "south"), (26, "north")):
            b.stack("relay_cabinet", x, y0, z, f, n=2)
    b.door(9, y0, 18, "south", double=True, label="relay_room")
    # ---- Plant computer room (x 20..38, z 19..28): racks in hot and cold aisles on the raised floor.
    b.b(20, y, 19, W - 2, y, D - 2, ACCESS_FLOOR)
    for x in range(22, W - 2):
        if x in (29, 30):
            continue
        for z, f in ((21, "south"), (22, "north"), (25, "south"), (26, "north")):
            b.stack("server_rack", x, y0, z, f, n=2)
    for z in (21, 22, 25, 26):
        b.tray_run(22, z, W - 3, z, y1, "north")
    for x in range(21, W - 1, 3):
        for z in (19, 23, 24, 28):
            b.p(x, y1, z, LIGHT)
    b.door(29, y0, 18, "south", double=True, label="server_room")
    b.furniture("fire_extinguisher", W - 2, y0 + 1, 20, "east")


def second_floor(b, W, D, y):
    """The control room floor (walked at y + 1, a raised floor throughout): the main control room north of the corridor,
    the shift supervisor's glazed office in its corner; south of the corridor the Technical Support Center, the kitchen,
    the washroom and the operations office. Returns where the reactor console stands (x, z)."""
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
    for x, z, tops in ((9, 8, (1, 0, 3)), (19, 8, (3, 0, 2)), (14, 11, (2, 1, 0))):
        b.row(("operator_console", 3), x, y0, z, "north", tops=tops)
        for k in (0, 2):
            b.p(x + k, y0, z + 1, furniture("operator_chair", "north"))
    # The procedures on shelves along the back wall, the plant overview screens on the east wall, and the office
    # copier the administration building has (Justin: "use that printer").
    for x in range(20, 28):
        b.stack("bookcase", x, y0, 16, "south", n=2)
    wall_screen(b, "plant_map_screen", 29, y0 + 1, 11, "east")
    import medium_admin
    assert medium_admin.copier(b, 11, y0, 16, "south")
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
    b.row(("executive_desk", 3), 4, y0, 13, "north", seat="operator_chair", tops=(2, 0, 1))
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
    # ---- South of the corridor, with windows: the Technical Support Center (x 1..13), the kitchen (x 15..24),
    # the washroom (x 26..29), the operations office (x 31..38).
    for x in (14, 25, 30):
        b.wall_z(x, 22, D - 2, y0, top)
    # Technical Support Center: the meeting table, the plant's status on screens, a whiteboard.
    b.b(1, y, 22, 13, y, D - 2, CARPET)
    b.round_table(7, y0, 25)
    wall_screen(b, "status_screen", 10, y0 + 1, 22, "north")
    wall_screen(b, "plant_map_screen", 1, y0 + 1, 26, "west")
    b.varied("whiteboard", 13, y0 + 1, 24, "east", 24)
    b.door(7, y0, 21, "south", double=True, label="support_center")
    # Kitchen: counters under the windows, the fridge, tables.
    b.row(["kitchen_counter", "kitchen_sink", "coffee_counter", "microwave_counter", "kitchen_counter"], 20, y0, D - 2, "south")
    b.stack("fridge", 22, y0, D - 2, "south", n=2)
    for x, z in ((17, 24), (21, 24)):
        b.varied("cafe_table", x, y0, z, "north", 6)
        b.p(x - 1, y0, z, furniture("cafe_chair", "east"))
        b.p(x + 1, y0, z, furniture("cafe_chair", "west"))
    b.door(19, y0, 21, "south", double=True, label="break_room")
    # Washroom.
    b.row(("kitchen_sink", 2), 27, y0, D - 2, "south")
    b.door(27, y0, 21, "south", label="washroom")
    # Operations office: desks, files.
    b.b(31, y, 22, W - 2, y, D - 2, CARPET)
    for x in (32, 36):
        b.desk(x, y0, 25, "south")
    b.furniture("filing_cabinet", W - 2, y0, 22, "east")
    b.furniture("filing_cabinet", W - 2, y0, 23, "east")
    b.door(34, y0, 21, "south", label="operations")
    return console


def top_floor(b, W, D, y):
    """The top floor (walked at y + 1, under the roof): the control room's emergency filtration and air conditioning,
    one plant room round the stair tower. Two air handling units with their filter trains, two chillers and their
    chilled water pumps, the ducts down into the control room, the louvres in the walls."""
    y0 = y + 1
    b.prop("air_handling_unit", 9, y0, 3, "south")
    b.prop("air_handling_unit", 16, y0, 3, "south")
    b.prop("heat_exchanger", 22, y0, 2, "south")
    b.prop("heat_exchanger", 26, y0, 2, "south")
    b.prop("pump_skid", 6, y0, 18, "south")
    b.prop("pump_skid", 12, y0, 18, "south")
    for x in (10, 17):
        b.duct_up(x, 12, x + 1, 12, y0)
    for z in (16, 20, 24):
        b.stack("relay_cabinet", 1, y0, z, "west", n=2)
    b.furniture("fire_extinguisher", W - 2, y0 + 1, 26, "east")


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
    for x in range(10, 22):
        for y in range(1, 10):
            b.p(x, y, D - 1, roller("north"))
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
    """The emergency diesel generator building, 46 x 20: two generator rooms (one per safety train), each with its
    diesel generator set (the engine's exhaust up through the roof to its stack), its fuel oil day tank (a small storage
    tank MBS, filled from the fuel oil storage tank outside), its control and protection panels and battery along the
    south wall, air intake louvres in the north wall.
    Returns the building and, per room, where its fill line comes through the south wall."""
    W, D, H = 46, 20, 11
    b = Building("diesel_building", "Emergency Diesel Building", W, D, H, grounded=True)
    b.shell_walls(CONCRETE, step=8)
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    b.wall_z(W // 2, 1, D - 2, 1, H, CONCRETE)
    fills = []
    for room, x0 in enumerate((2, W // 2 + 2)):
        b.prop("emergency_diesel_generator", x0 + 2, 1, 7, "east")
        # Along the south wall, as you face them from the left: the battery rack and its charger, the load sequencer,
        # the protection relays, the voltage regulator, the generator's control panel, the engine's (nearest the engine's
        # front).
        x = x0 + 8
        for name, width in (("edg_battery_rack", 1), ("edg_battery_charger", 1), ("edg_load_sequencer", 1), ("edg_protection_panel", 1),
                            ("edg_voltage_regulator", 1), ("edg_generator_panel", 2), ("edg_engine_panel", 1)):
            b.panel(name, x, 1, D - 2, "south", width)
            x -= width
        # Louvres high in the north wall; the stack over the engine's turbochargers (its exhaust comes up from them).
        for x in range(x0 + 1, x0 + 17):
            if x % 8:
                for y in (6, 7, 8):
                    b.p(x, y, 0, LOUVRE)
        # (The stack stands above the part's own height: it goes in as site decor.)
        sx = x0 + 2
        for y in range(H + 1, H + 8):
            for dx in (0, 1):
                for z in (9, 10):
                    s.decor[(ox + sx + dx, b.site_y(y), oz + z)] = BRICK
        # (No campfire on top: the running generator makes its own exhaust smoke there, client/NuclearRenderer.)
        entrance(b, x0 + 11, D - 1, "s", kind="steel")
        b.p(x0 + 13, 2, D - 2, sign("diesel_a" if room == 0 else "diesel_b", "north"))
        fills.append(day_tank(b, x0, room, H, D))
    s.add(b, ox, oz)
    return b, fills


def day_tank(b, x0, room, H, D):
    """A generator room's fuel oil day tank in its north-east corner: a small storage tank MBS (tank_in) on a steel
    drip pan with a lip round it, ladder, railing, gate and hatch like the big tanks, and a short vent on its roof like
    theirs (Justin, round 14: no vent line up through the building's roof). It is rated for flammable liquids (the
    pressure/vacuum vent; no foam chambers indoors). The fuel lines keep out of the way, in the floor (pipes inside the slab, Justin's round 13):
    the outlet drops into the floor and comes up under the engine's fuel connection; the line from the fuel oil
    storage tank comes in under the south wall, along the floor and up into the inlet. Returns where that comes in
    under the wall (its x)."""
    cx, cz, r, h = x0 + 16.5, 3.5, 2.6, 6
    out_cell, in_cell, _ = tank_in(b, cx, cz, r, h, "diesel_day_a" if room == 0 else "diesel_day_b", outlet="w",
                                           inlet="s", ladder="e", manway=None, hatch_in=2, vent=(0, -1), foundation=None,
                                           fill_line=False, wall=1.1, rating="flammable", foam=False)
    # The outlet: a stub off its flange, down into the floor, along under it and up under the engine's fuel connection
    # on the generator's near side.
    ox, oz = out_cell
    fx = x0 + 7
    ix, iz = in_cell
    risers = [(ox - 1, 1, oz), (fx, 1, 6), (ix, 1, iz + 1)]
    floor = [(ox - 1, 0, oz)] + [(x, 0, oz) for x in range(ox - 2, fx - 1, -1)] + [(fx, 0, z) for z in range(oz + 1, 7)]
    # The inlet: in under the south wall (through the slab under it), along the floor and up into the inlet's flange.
    floor += [(ix, 0, z) for z in range(D - 1, iz, -1)]
    for x, y, z in risers:
        assert b.get(x, y, z) == 0, ("fuel line", x, y, z)
        b.p(x, y, z, PIPE)
    for x, y, z in floor:
        assert b.get(x, y, z) in (EPOXY, SMOOTH), ("fuel line in the floor", x, y, z)
        b.p(x, y, z, WALL_PIPE)
    # The drip pan under and round the tank, a steel lip at its edge (broken where the pipes come out).
    for x in range(int(cx - r - 3), int(cx + r + 3)):
        for z in range(1, int(cz + r + 3)):
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
    q.prop("switchgear_lineup", 24, 1, D - 10, "south")
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
    """The warehouse, level with the ground: storage racks (real stores, three bays by three levels) back to back in
    rows with aisles between, high-bay lights under the roof, the stores office in the corner (its own block walls on
    every side), roller doors at the loading end that roll up."""
    W, D, H = 40, 26, 12
    b = Building("warehouse", "Warehouse", W, D, H, grounded=True)
    b.high_bay = True
    b.shell_walls(CLADDING, step=8)
    b.windows(9, 10, "ns", step=8)
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    # Rows of racks back to back, each front on an aisle (z 5..8, 11..14, 17..18), eight racks to a row.
    for z in (3, 4, 9, 10, 15, 16):
        front_south = z in (3, 9, 15)
        facing = "north" if front_south else "south"     # the user looks at the front, from the aisle
        for k in range(8):
            x_left, x_right = 3 + 3 * k, 5 + 3 * k
            # The left bay as you face the front: west when looking north, east when looking south.
            storage_rack(b, x_left if facing == "north" else x_right, 1, z, facing)
    # The stores office: its own block walls inside the outer walls (so every wall inside is painted block), a window
    # onto the floor, a door, a desk and cabinets.
    ox0, oz0, ox1, oz1 = W - 11, D - 9, W - 2, D - 2
    b.wall_x(oz0, ox0, ox1, 1, 4)
    b.wall_x(oz1, ox0, ox1, 1, 4)
    b.wall_z(ox0, oz0, oz1, 1, 4)
    b.wall_z(ox1, oz0, oz1, 1, 4)
    for x in range(ox0 + 2, ox0 + 6):
        b.p(x, 2, oz0, PANE)
    b.b(ox0 + 1, 4, oz0 + 1, ox1 - 1, 4, oz1 - 1, CEIL)
    b.door(ox0, 1, oz0 + 3, "east", label="stores")
    b.desk(ox1 - 3, 1, oz1 - 1, "south")
    b.furniture("filing_cabinet", ox1 - 1, 1, oz0 + 2, "east")
    b.furniture("filing_cabinet", ox1 - 1, 1, oz0 + 3, "east")
    b.plant(ox0 + 1, 1, oz1 - 1)     # one small potted tree in the corner
    for z in range(4, 17):
        for y in range(1, 9):
            b.p(W - 1, y, z, roller("west"))
    b.b(W - 1, 9, 3, W - 1, 9, 17, CLADDING)
    entrance(b, 5, D - 1, "s", kind="steel", step=False)
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
            b.p(lx, y, lz, steel_ladder(facing, y >= 3, y == h + 2))
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


def fence(s, x0, z0, x1, z1, gate):
    """A double security fence round the protected area, lit, with a gate gap at x = gate on the south side."""
    for off in (0, 3):
        for x in range(x0 + off, x1 - off + 1):
            for z in (z0 + off, z1 - off):
                if z == z1 - off and abs(x - gate) <= 4:
                    continue
                for y in range(0, 3):
                    s.decor[(x, y, z)] = BARS
        for z in range(z0 + off, z1 - off + 1):
            for x in (x0 + off, x1 - off):
                for y in range(0, 3):
                    s.decor[(x, y, z)] = BARS
    for x in range(x0, x1 + 1, 16):
        for z in (z0 - 2, z1 + 2):
            for y in range(0, 6):
                s.decor[(x, y, z)] = M("minecraft:stone_brick_wall", (122, 122, 120)) if y < 5 else LANTERN


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
    hall = turbine_hall(s, 124, 38)
    # (The buildings' ground floors are level with the grass, at y -1: what stands on them starts at y 0.)
    s.eq("turbine_generator", 160, 54, "east", y=12)
    s.eq("condenser", 170, 54, "east", y=6)
    s.eq("main_transformer", 228, 57, "east")
    s.eq("feedwater_pump", 150, 66, y=0)
    # Main steam: out of the containment in the pipe bridge, across the hall above the operating floor to the turbine's
    # main steam inlet over its high-pressure end; feedwater up from the pump into it.
    s.line(P.STEAM, (sx + 1, 21, sz), (165, 21, sz))
    s.line(P.STEAM, (165, 21, 59), (165, 21, sz))
    s.line(P.STEAM, (165, 20, 59), (165, 20, 59))
    s.line(P.STEAM, (152, 3, 67), (152, 21, 67))
    s.line(P.STEAM, (152, 21, 66), (152, 21, 66))
    s.gallery(111, 123, sz - 2, sz + 2, 19, 23, [(112, sz - 2), (112, sz + 2), (122, sz - 2), (122, sz + 2)])
    # Hangers for the steam line inside the hall, from the roof trusses.
    for x in list(range(126, 166, 6)) + [164]:
        for y in range(23, 37):
            hall.g[x - 124, y, sz - 38] = CHAIN if hall.g[x - 124, y, sz - 38] == 0 else hall.g[x - 124, y, sz - 38]
    # Isolated phase bus from the generator out through the end wall to the main transformer, on steel trestles.
    s.line(P.BUS, (210, 12, 59), (230, 12, 59))
    s.line(P.BUS, (230, 6, 59), (230, 11, 59))
    for x in (226, 229):
        for y in range(0, 12):
            s.decor[(x, y, 60)] = GIRDER
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
    trench = [(200, 0, 119)] + [(200, -1, z) for z in range(110, 120)]
    trench += [(x, -1, 110) for x in range(180 + min(fills), 180 + max(fills) + 1)]
    for x, y, z in trench:
        s.decor[(x, y, z)] = WALL_PIPE
        if y == -1:
            s.decor[(x, -2, z)] = SMOOTH
    # Electrical: the transformer yard by the turbine hall, the switchyard beyond.
    s.add(P.transformer_yard("pwr_transformer_yard", "Main Transformer Yard", 20, 26), 226, 46)
    s.add(P.switchyard("switchyard_medium", "Switchyard", 70, 60, 6), 262, 30)
    # The intake: legs down into each bay behind its screen, a header in the pump floor, a pump over each leg.
    ph = pump_house(s, 150, -8)
    legs = [150 + i * ph.bay_w + 8 for i in range(ph.bays)]
    for x in legs:
        s.line(P.CONDUIT, (x, -4, -4), (x, -1, -4))
        s.line(P.CONDUIT, (x, -1, -3), (x, -1, -2))
        s.line(P.TRENCH_KIND, (x, -1, -1), (x, -1, 1))
        s.eq("circulating_water_pump", x - 1, 0, y=0)
    s.line(P.TRENCH_KIND, (legs[0], -1, -1), (legs[-1], -1, -1))
    # Towers.
    t = P.cooling_tower("ct_natural", "Natural-Draft Cooling Tower", 52, 150, 31, 115, "natural")
    for tx in (120, 250):
        s.tower(t, tx, 215, "natural")
    # Circulating water: lake -> condenser (riser up through the floor and its foundation) -> both towers.
    s.conduit((legs[2], -1), (legs[2], 30), (172, 30), (172, 59))
    s.line(P.CONDUIT, (172, 0, 59), (172, 5, 59))
    s.conduit((172, 59), (172, 205), (120, 205), (120, 270))
    s.conduit((172, 205), (250, 205), (250, 270))
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
    fence(s, 0, 26, 250, 146, 123)
    parking(s, 160, 165, 230, 200)
    s.roads += [(0, 150, 372, 156), (120, 82, 127, 215), (100, 84, 240, 87)]
    # Building name signs.
    site_sign(s, hall, 20, 15, 0, "s", "turbine_building")
    site_sign(s, ctrl_part(s, "control_building"), 20, 5, 0, "s", "control_building")
    site_sign(s, admin, 30, 4, 0, "n", "administration")
    site_sign(s, gate, 5, 3, 0, "s", "security")
    site_sign(s, gate, 5, 3, 0, "n", "security")
    site_sign(s, ware, 20, 6, 0, "s", "warehouse")    # well under the windows (the sign stands taller than its block)
    # The warehouse's door path to the road, and the concrete apron in front of its roller doors.
    A.walkway(s, 266, 136, 268, 149)
    A.walkway(s, 302, 113, 310, 128)
    A.walkway(s, 306, 129, 310, 149)
    site_sign(s, diesel, 23, 8, 0, "s", "diesel_building")
    site_sign(s, ctrl_part(s, "aux_building"), 30, 4, 0, "s", "auxiliary_building")
    site_sign(s, ctrl_part(s, "fuel_building"), 0, 15, 23, "e", "fuel_building")
    site_sign(s, ph, 20, 11, 0, "s", "pump_house")
    finish_lights(s)
    for part, *_ in s.parts:
        finish_railings(part)
        finish_glass(part)
    return s


def ctrl_part(s, key):
    return next(p for p, *_ in s.parts if p.key == key)
