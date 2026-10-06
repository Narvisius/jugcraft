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
GIRDER = M("jugcraft:steel_girder", (120, 124, 128))
FRAME = M("jugcraft:tungsten_steel_frame", (110, 114, 120))
COLUMN = M("jugcraft:concrete_column", (170, 170, 166))
ASPHALT = M("jugcraft:asphalt", (56, 58, 60))
ROAD_LINE = M("jugcraft:asphalt_road_line", (200, 200, 200))
LANTERN = M("minecraft:sea_lantern", (210, 230, 226))
WARN_LIGHT = M("jugcraft:warning_light", (220, 60, 50))
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
LOUVRE = M("jugcraft:intake_louvre", (150, 154, 152))
LIGHT = M("jugcraft:ceiling_light_panel", (240, 240, 230))
BATTERY = M("jugcraft:battery_box", (80, 84, 90))
HAZARD = M("jugcraft:hazard_plating", (210, 170, 40))

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


def roller(facing):
    return M(f"jugcraft:roller_door[facing={facing},open=false]", (150, 156, 160))


CCW = {v: k for k, v in P.CLOCKWISE.items()}
GLAZING = {PANE, GLASS, BLAST}
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

    def rail(self, x, y, z, facing):
        """A section of yellow safety railing standing at y on the facing edge of its cell."""
        self.p(x, y, z, furniture("guard_rail", facing))

    def rail_round(self, x0, z0, x1, z1, y):
        """Railing round an opening x0..x1, z0..z1 in a floor whose top is walked at y (the rails stand just outside)."""
        for x in range(x0, x1 + 1):
            for z, f in ((z0 - 1, "south"), (z1 + 1, "north")):
                if self.get(x, y, z) == 0 and self.get(x, y - 1, z):
                    self.rail(x, y, z, f)
        for z in range(z0, z1 + 1):
            for x, f in ((x0 - 1, "east"), (x1 + 1, "west")):
                if self.get(x, y, z) == 0 and self.get(x, y - 1, z):
                    self.rail(x, y, z, f)

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
        tall spaces they hang on chains to about hang blocks over the floor below. blocked: grid cells a light or
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
                        if drop is not None:
                            for j in range(drop):
                                self.g[x + ox, gy - j, z + oz] = CHAIN
                            self.g[x + ox, gy - drop, z + oz] = LIGHT
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
    if step:
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
    b = Building("pwr_containment", "Reactor Containment (PWR)", size, size, wall_h + dome_h - 2)
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
    b.g[cv - 4, 9, size - 1] = WARN_LIGHT
    b.g[cv + 4, 9, size - 1] = WARN_LIGHT
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
    s.add(b, ox, oz)
    return b


def place_primary(s, ox, oz):
    """The working reactor equipment in the containment at (ox, oz): the four loops of tools/nuclear_plants.py."""
    return P.four_loops(s, ox, oz, 25.5, 10)


# ------------------------------------------------------------------------------------------------- turbine hall

def turbine_hall(s, ox, oz):
    """The turbine building, 100 x 44 and 36 tall.

    The turbine-generator stands on its concrete pedestal at the operating floor (+12); under its low-pressure end
    the condenser sits on its foundation in the pedestal's condenser bay (a door from the ground floor). The ground
    floor has the feedwater heater trains, the condensate pumps and the feedwater pump, a pipe rack along the north
    wall, and plant rooms along the south wall (switchgear, lube oil, workshop, turbine control). The laydown bay at
    the west end is open to the crane, closed by a roller door; stairs at both ends; the overhead crane on its runway."""
    W, D, H = 100, 44, 36
    b = Building("pwr_turbine_hall", "Turbine Building", W, D, H)
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

def control_building(s, ox, oz):
    """The control building, 40 x 30, three storeys round a corridor (z 13..15) with the stairs at the east end.

    Ground floor: the entrance lobby with reception and the turnstiles, the battery room, the HVAC room (air handling
    units with their ducts up through the ceiling) and the electrical room. First floor: the relay room (relay
    cabinets back to back in rows), the cable spreading room (tiers of cable tray under the ceiling) and the plant
    computer room (server racks in hot and cold aisles on a raised floor). Top floor: the main control room, with
    the horseshoe of control boards under the plant display wall, operator desks, the reactor console, the shift
    supervisor's office (part glazed) and a break room by the stairs. Returns the reactor console's site position."""
    W, D, H = 40, 30, 22
    b = Building("control_building", "Control Building", W, D, H)
    b.shell_walls(CONCRETE, step=8)
    b.windows(2, 4, "s", step=8, skip=(18, 19, 20, 21))
    b.windows(9, 11, "ns", step=8)
    b.storey(0, 1, 1, W - 2, D - 2, VINYL)
    b.storey(7, 1, 1, W - 2, D - 2, VINYL, ceiling_at=6)
    b.storey(14, 1, 1, W - 2, D - 2, ACCESS_FLOOR, ceiling_at=13)
    b.b(1, 21, 1, W - 2, 21, D - 2, CEIL)
    entrance(b, 20, D - 1, "s", label=None)
    b.stairwell(W - 9, 16, [0, 7, 14], door_side="n")
    # The corridor walls on each floor.
    for lv in (0, 7):
        y0, y1 = lv + 1, lv + 5
        b.wall_x(12, 1, W - 2, y0, y1)
        b.wall_x(16, 1, W - 10, y0, y1)
    # Ground floor.
    b.wall_z(14, 1, 11, 1, 5)
    b.wall_z(30, 1, 11, 1, 5)
    b.door(7, 1, 12, "north", double=True, label="battery_room")
    b.door(22, 1, 12, "north", double=True, label="hvac_room")
    b.door(35, 1, 12, "north", label="electrical_room")
    for x in range(2, 13, 2):
        for z in (2, 4, 7, 9):
            b.p(x, 1, z, BATTERY)
    b.prop("air_handling_unit", 16, 1, 2, "east")
    b.prop("air_handling_unit", 16, 1, 7, "east")
    b.duct_up(22, 3, 23, 4, 4)
    b.duct_up(22, 8, 23, 9, 4)
    b.prop("switchgear_lineup", 31, 1, 2, "south")
    b.p(37, 1, 6, furniture("relay_cabinet", "west", up=False, down=False))
    # Lobby: reception desk, turnstiles through the corridor wall, lockers.
    b.c(10, 1, 16, 21, 3, 16)
    for x in (11, 14, 17, 20):
        b.furniture("turnstile", x, 1, 16, "north")
    b.desk(25, 1, 22, "south")
    b.desk(27, 1, 22, "south")
    for x in range(2, 9):
        b.stack("locker", x, 1, 17, "north", n=2)
    b.furniture("water_cooler", 1, 1, 27, "west")
    # First floor: relay room (west), cable spreading room (east), computer room (south).
    b.wall_z(18, 1, 11, 8, 12)
    b.door(9, 8, 12, "north", double=True, label="relay_room")
    b.door(27, 8, 12, "north", double=True, label="cable_spreading_room")
    b.door(15, 8, 16, "south", double=True, label="server_room")
    for x in range(2, 17):
        if x == 9:
            continue
        b.stack("relay_cabinet", x, 8, 3, "south", n=2)
        b.stack("relay_cabinet", x, 8, 4, "north", n=2)
        b.stack("relay_cabinet", x, 8, 8, "south", n=2)
        b.stack("relay_cabinet", x, 8, 9, "north", n=2)
    for z in (3, 5, 7, 9):
        b.tray_run(19, z, W - 3, z, 12, "east")
    for z in range(2, 11, 4):
        b.stack("relay_cabinet", W - 2, 8, z, "east", n=2)
    b.b(1, 7, 17, W - 2, 7, D - 2, ACCESS_FLOOR)
    for x in range(3, 28):
        if x in (15, 16):
            continue
        for z, f in ((19, "south"), (20, "north"), (24, "south"), (25, "north")):
            b.stack("server_rack", x, 8, z, f, n=2)
    for z in (19, 25):
        b.tray_run(3, z, 27, z, 12, "east")
    for z in range(18, 28, 3):
        b.p(1, 8, z, BATTERY)
    # Top floor: the main control room (west of x 30), break room and stair lobby east.
    b.wall_z(30, 1, D - 2, 15, 20, ACOUSTIC)
    b.wall_x(12, 31, W - 2, 15, 20)
    b.door(30, 15, 14, "west", double=True, label="main_control_room")
    b.door(34, 15, 12, "north", label="cafeteria")
    for x in range(1, 30):
        for y in range(15, 21):
            for z in (1, D - 2):
                if b.get(x, y, z) == 0:
                    b.p(x, y, z, ACOUSTIC)
    for z in range(1, D - 1):
        for y in range(15, 21):
            if b.get(1, y, z) == 0:
                b.p(1, y, z, ACOUSTIC)
    b.prop("plant_display_wall", 10, 17, 2, "north")
    for x in (4, 10, 16, 22):
        b.prop("main_control_board", x, 15, 4, "north")
    b.prop("main_control_board", 2, 15, 7, "west")
    b.prop("main_control_board", 2, 15, 13, "west")
    b.prop("main_control_board", 27, 15, 7, "east")
    for x in (8, 12, 20, 24):
        b.desk(x, 15, 14, "north", name="console_desk")
    # The shift supervisor's office: a half wall with glazing over it, a door, a desk, cabinets, a whiteboard.
    for x in range(2, 13):
        b.p(x, 15, 20, ACOUSTIC)
        for y in (16, 17):
            b.p(x, y, 20, PANE)
        for y in range(18, 21):
            b.p(x, y, 20, ACOUSTIC)
    for z in range(21, D - 1):
        for y in range(15, 21):
            b.p(13, y, z, ACOUSTIC)
    b.door(8, 15, 20, "south", label="shift_supervisor")
    b.b(2, 14, 21, 12, 14, D - 2, CARPET)
    b.desk(6, 15, 25, "south")
    b.furniture("filing_cabinet", 2, 15, 27, "west")
    b.furniture("filing_cabinet", 2, 15, 26, "west")
    b.furniture("whiteboard", 12, 15, 24, "east")
    # The control room's lighting: a close grid of panels in the ceiling.
    for x in range(3, 30, 3):
        for z in range(3, D - 2, 3):
            if b.get(x, 20, z) == 0 and b.get(x, 21, z) == CEIL:
                b.p(x, 20, z, LIGHT)
    # Break room: kitchen counter, table, water cooler.
    b.furniture("water_cooler", 38, 15, 2, "east")
    for x in (33, 35):
        b.desk(x, 15, 6, "north")
    s.add(b, ox, oz)
    return (ox + 15, oz + 10, 15)


# ------------------------------------------------------------------------------------------------- auxiliary building

def aux_building(s, ox, oz):
    """The nuclear auxiliary building, 60 x 40, three storeys of plant rooms either side of a central corridor:
    charging, residual heat removal and safety injection pumps, component cooling heat exchangers, the boric acid
    tanks, the ventilation plant. Each room is entered round a shielding wall; the health physics checkpoint with its
    lockers is at the entrance; pipes run along the corridor ceilings."""
    W, D, H = 60, 40, 23
    b = Building("aux_building", "Nuclear Auxiliary Building", W, D, H)
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
    b.p(6, 3, 22, WARN_LIGHT)
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
    b = Building("fuel_building", "Fuel Building", W, D, H)
    b.shell_walls(SHIELD, step=8)
    b.windows(22, 24, "ew", step=8)
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    deck = 12
    b.b(1, deck, 1, W - 2, deck, 30, EPOXY)
    px0, px1, pz0, pz1 = 7, 18, 4, 25
    b.b(px0, 1, pz0, px1, deck, pz1, LINER)
    b.c(px0 + 1, 2, pz0 + 1, px1 - 1, deck, pz1 - 1)
    for x in range(px0 + 1, px1):
        for z in range(pz0 + 1, pz1):
            for y in range(2, deck + 1):
                b.p(x, y, z, WATER)
    for x in range(px0 + 1, px1 - 3, 4):
        for z in range(pz0 + 1, pz1 - 3, 4):
            b.prop("spent_fuel_rack", x, 2, z, "north")
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
    diesel generator set, day tank and control panels, air intake louvres in the north wall and the exhaust stack
    through the roof."""
    W, D, H = 46, 20, 11
    b = Building("diesel_building", "Emergency Diesel Building", W, D, H)
    b.shell_walls(CONCRETE, step=8)
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    b.wall_z(W // 2, 1, D - 2, 1, H, CONCRETE)
    for room, x0 in enumerate((2, W // 2 + 2)):
        b.prop("emergency_diesel_generator", x0 + 2, 1, 7, "east")
        b.prop("storage_tank", x0 + 15, 1, 2, "north")
        for x in range(x0 + 1, x0 + 9):
            b.stack("relay_cabinet", x, 1, D - 2, "south", n=2)
        # Louvres high in the north wall, the stack over the engine.
        for x in range(x0 + 1, x0 + 17):
            if x % 8:
                for y in (6, 7, 8):
                    b.p(x, y, 0, LOUVRE)
        sx = x0 + 9
        b.b(sx, H + 1, 9, sx + 1, H + 7, 10, BRICK)
        b.p(sx, H + 7, 9, M("minecraft:campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]", (120, 80, 40)))
        entrance(b, x0 + 11, D - 1, "s", kind="steel")
        b.p(x0 + 13, 2, D - 2, sign("diesel_a" if room == 0 else "diesel_b", "north"))
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- pump house

def pump_house(s, ox, oz):
    """The circulating water intake and pump house, 40 x 30: four intake bays reaching out over the lake (concrete
    walls down into the water, a travelling screen in each), the pump floor with a circulating water pump over each
    bay's suction, the electrical room behind, and a roller door for the crane bay."""
    W, D, H, sink = 40, 30, 14, 4
    b = Building("pump_house", "Intake Pump House", W, D, H, sink=sink)
    bays, bw = 4, 10
    b.b(0, 0, 0, W - 1, 0, 6, CONCRETE)
    for i in range(bays + 1):
        x = min(i * bw, W - 1)
        b.b(x, -sink, 0, x, 0, 6, CONCRETE)
    b.b(0, -sink, 6, W - 1, 0, 6, CONCRETE)
    for i in range(bays):
        x0 = i * bw + 1
        b.c(x0, 0, 0, x0 + bw - 2, 0, 5)
        b.prop("travelling_water_screen", x0 + 1, -3, 2, "north")
    # Railing along the intake deck's edge over the bays.
    for x in range(W):
        if b.get(x, 0, 6) and b.get(x, 1, 6) == 0:
            b.rail(x, 1, 6, "north")
    q = Building("x", "x", W, D - 7, H)
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

def warehouse(s, ox, oz):
    """The warehouse: rows of pallet racking three high with aisles between, the stores office in the corner,
    roller doors at the loading end."""
    W, D, H = 40, 26, 12
    b = Building("warehouse", "Warehouse", W, D, H)
    b.shell_walls(CLADDING, step=8)
    b.windows(9, 10, "ns", step=8)
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    # Rows of racking back to back, each front on an aisle (z 5..8, 11..14), three pallets high.
    for z in (3, 4, 9, 10, 15, 16):
        f = "south" if z in (3, 9, 15) else "north"
        for x in range(3, W - 12):
            kind = "pallet_rack_drums" if (x // 3) % 4 == 1 else "pallet_rack"
            b.stack(kind, x, 1, z, f, n=3)
    # The stores office: block walls with a window, a door, a desk and cabinets.
    b.wall_x(D - 8, W - 10, W - 2, 1, 4)
    b.wall_z(W - 10, D - 8, D - 2, 1, 4)
    for x in range(W - 8, W - 4):
        b.p(x, 2, D - 8, PANE)
    b.b(W - 9, 4, D - 7, W - 2, 4, D - 2, CEIL)
    b.door(W - 10, 1, D - 4, "east", label="stores")
    b.desk(W - 5, 1, D - 3, "south")
    b.furniture("filing_cabinet", W - 2, 1, D - 6, "east")
    for z in range(4, 18):
        for y in range(1, 9):
            b.p(W - 1, y, z, roller("west"))
    b.b(W - 1, 9, 3, W - 1, 9, 18, CLADDING)
    entrance(b, 5, D - 1, "s", kind="steel")
    s.add(b, ox, oz)
    return b


def outdoor_tank(s, cx, cz, r, h, name, key):
    """A big outdoor storage tank (refuelling water, condensate): painted steel plate on a concrete ring, a caged
    ladder, a railing round the roof."""
    size = int(2 * r) + 5
    c = size / 2
    b = Building(key, name, size, size, h + 1)
    b.disc(c, c, r + 1, 0, 0, SMOOTH)
    b.disc(c, c, r, 1, h, TANK, inner=r - 1.2)
    b.disc(c, c, r, h + 1, h + 1, TANK)
    # Railing round the roof edge (on the roof plate, facing out).
    for x in range(size):
        for z in range(size):
            if b.g[x, h + 1, z] and not b.g[x, h + 2, z]:
                dx, dz = x + 0.5 - c, z + 0.5 - c
                if r - 1.2 < math.hypot(dx, dz) <= r:
                    f = ("east" if dx > 0 else "west") if abs(dx) > abs(dz) else ("south" if dz > 0 else "north")
                    b.g[x, h + 2, z] = furniture("guard_rail", f)
    lx = int(c)
    for y in range(1, h + 2):
        z = int(c + r)
        while z < size - 1 and b.g[lx, y, z] != 0:
            z += 1
        if z < size and b.g[lx, y, z - 1] != 0:
            b.g[lx, y, z] = LADDER["south"]
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
    sign block stands just outside the wall."""
    _, px, pz, _ = next(e for e in s.parts if e[0] is part)
    w, _, d = part.size
    fx, fz = P.DIRS[SIDE[side]]
    wx = px + (x if side in "ns" else (-1 if side == "w" else w))
    wz = pz + (z if side in "ew" else (-1 if side == "n" else d))
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
    s.eq("turbine_generator", 160, 54, "east", y=13)
    s.eq("condenser", 170, 54, "east", y=7)
    s.eq("main_transformer", 228, 57, "east")
    s.eq("feedwater_pump", 150, 66)
    # Main steam: out of the containment in the pipe bridge, across the hall above the operating floor to the turbine's
    # main steam inlet over its high-pressure end; feedwater up from the pump into it.
    s.line(P.STEAM, (sx + 1, 22, sz), (165, 22, sz))
    s.line(P.STEAM, (165, 22, 59), (165, 22, sz))
    s.line(P.STEAM, (165, 21, 59), (165, 21, 59))
    s.line(P.STEAM, (152, 4, 67), (152, 22, 67))
    s.line(P.STEAM, (152, 22, 66), (152, 22, 66))
    s.gallery(111, 123, sz - 2, sz + 2, 20, 24, [(112, sz - 2), (112, sz + 2), (122, sz - 2), (122, sz + 2)])
    # Hangers for the steam line inside the hall, from the roof trusses.
    for x in list(range(126, 166, 6)) + [164]:
        for y in range(23, 37):
            hall.g[x - 124, y, sz - 38] = CHAIN if hall.g[x - 124, y, sz - 38] == 0 else hall.g[x - 124, y, sz - 38]
    # Isolated phase bus from the generator out through the end wall to the main transformer, on steel trestles.
    s.line(P.BUS, (210, 13, 59), (230, 13, 59))
    s.line(P.BUS, (230, 6, 59), (230, 12, 59))
    for x in (226, 229):
        for y in range(0, 13):
            s.decor[(x, y, 60)] = GIRDER
    # Around the reactor.
    aux_building(s, 30, 95)
    fuel_building(s, 100, 95)
    ctrl = control_building(s, 136, 88)
    cx, cz, cy = ctrl
    s.eq("reactor_control_console", cx, cz, "north", y=cy)
    diesel = diesel_building(s, 180, 90)
    outdoor_tank(s, 16, 70, 6.5, 14, "Refuelling Water Storage Tank", "rwst_tank")
    outdoor_tank(s, 232, 125, 5.5, 12, "Condensate Storage Tank", "condensate_tank")
    # Electrical: the transformer yard by the turbine hall, the switchyard beyond.
    s.add(P.transformer_yard("pwr_transformer_yard", "Main Transformer Yard", 20, 26), 226, 46)
    s.add(P.switchyard("switchyard_medium", "Switchyard", 70, 60, 6), 262, 30)
    # The intake: legs down into each bay behind its screen, a header under the pump floor, a pump over each leg.
    ph = pump_house(s, 150, -8)
    legs = [150 + i * ph.bay_w + 8 for i in range(ph.bays)]
    for x in legs:
        s.line(P.CONDUIT, (x, -4, -4), (x, -1, -4))
        s.line(P.CONDUIT, (x, -1, -3), (x, -1, -2))
        s.line(P.TRENCH_KIND, (x, -1, -1), (x, -1, 1))
        s.line(P.CONDUIT, (x, 0, 1), (x, 0, 1))
        s.eq("circulating_water_pump", x - 1, 0)
    s.line(P.TRENCH_KIND, (legs[0], -1, -1), (legs[-1], -1, -1))
    # Towers.
    t = P.cooling_tower("ct_natural", "Natural-Draft Cooling Tower", 52, 150, 31, 115, "natural")
    for tx in (120, 250):
        s.tower(t, tx, 215, "natural")
    # Circulating water: lake -> condenser (riser up through the floor and its foundation) -> both towers.
    s.conduit((legs[2], -1), (legs[2], 30), (172, 30), (172, 59))
    s.line(P.CONDUIT, (172, 0, 59), (172, 6, 59))
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
    site_sign(s, hall, 20, 16, 0, "s", "turbine_building")
    site_sign(s, ctrl_part(s, "control_building"), 20, 6, 0, "s", "control_building")
    site_sign(s, admin, 30, 4, 0, "n", "administration")
    site_sign(s, gate, 5, 3, 0, "s", "security")
    site_sign(s, gate, 5, 3, 0, "n", "security")
    site_sign(s, ware, 20, 9, 0, "s", "warehouse")
    site_sign(s, diesel, 23, 9, 0, "s", "diesel_building")
    site_sign(s, ctrl_part(s, "aux_building"), 30, 5, 0, "s", "auxiliary_building")
    site_sign(s, ctrl_part(s, "fuel_building"), 0, 16, 23, "e", "fuel_building")
    site_sign(s, ph, 20, 12, 0, "s", "pump_house")
    finish_lights(s)
    return s


def ctrl_part(s, key):
    return next(p for p, *_ in s.parts if p.key == key)
