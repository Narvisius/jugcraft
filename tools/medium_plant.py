"""The Medium Nuclear Plant (4-loop PWR), designed like a real station, building by building.

Every building is drawn at real size (one block = one metre) with what a real one holds and how people get round it:
floors at real heights, stairwells, corridors, doors, offices and plant rooms, lit inside. Outside the buildings keep
to plain Minecraft materials (concrete, stone bricks, smooth stone, glass, iron); inside they turn realistic and
technical, with the mod's equipment and furniture. Decor equipment (cranes, heaters, tanks, the diesel generators...)
is drawn by the client like the reactor equipment and placed whole.

Site coordinates as in tools/nuclear_plants.py: x east, z south (the lake is north, z < 0), y up from the ground.
"""
import math

import nuclear_plants as P
from nuclear_plants import Part, M

# ------------------------------------------------------------------------------------------------- the palette
# Outside: plain Minecraft. Inside: the mod's technical surfaces.
CONCRETE = M("minecraft:light_gray_concrete", (176, 176, 170))
CONCRETE_DARK = M("minecraft:gray_concrete", (104, 107, 110))
WHITE = M("minecraft:white_concrete", (222, 222, 216))
SMOOTH = M("minecraft:smooth_stone", (158, 158, 156))
SMOOTH_SLAB = M("minecraft:smooth_stone_slab[type=bottom]", (158, 158, 156))
BRICK = M("minecraft:stone_bricks", (122, 122, 120))
ANDESITE = M("minecraft:polished_andesite", (132, 135, 132))
DEEPSLATE = M("minecraft:polished_deepslate", (72, 72, 76))
TILES = M("minecraft:deepslate_tiles", (60, 60, 64))
SHIELD = M("jugcraft:shielding_concrete", (186, 182, 172))
REINFORCED = M("jugcraft:reinforced_concrete", (150, 150, 146))
LINER = M("jugcraft:steel_armor_plate", (170, 174, 178))
CLADDING = M("jugcraft:aluminum_cladding", (190, 196, 200))
GLASS = M("minecraft:glass", (180, 210, 220))
PANE = M("minecraft:glass_pane", (180, 210, 220))
BLAST = M("jugcraft:blast_glass", (150, 180, 196))
IRON = M("minecraft:iron_block", (204, 208, 212))
BARS = M("minecraft:iron_bars", (92, 94, 98))
CHAIN = M("minecraft:iron_chain[axis=y]", (70, 70, 74))
GIRDER = M("jugcraft:steel_girder", (120, 124, 128))
FRAME = M("jugcraft:tungsten_steel_frame", (110, 114, 120))
HAZARD = M("jugcraft:hazard_plating", (210, 170, 40))
ACCESS_FLOOR = M("jugcraft:access_floor_tile", (150, 154, 158))
ACOUSTIC = M("jugcraft:acoustic_wall_panel", (200, 200, 196))
CARPET = M("jugcraft:carpet_tile", (90, 96, 110))
COLUMN = M("jugcraft:concrete_column", (170, 170, 166))
ASPHALT = M("jugcraft:asphalt", (56, 58, 60))
ROAD_LINE = M("jugcraft:asphalt_road_line", (200, 200, 200))
GRAVEL = M("minecraft:gravel", (132, 126, 120))
LIGHT = M("jugcraft:ceiling_light_panel", (240, 240, 230))
LANTERN = M("minecraft:sea_lantern", (210, 230, 226))
WARN_LIGHT = M("jugcraft:warning_light", (220, 60, 50))
PIPE = M("jugcraft:steel_fluid_pipe", (120, 128, 136))
TRAY = M("jugcraft:cable_tray", (120, 120, 124))
RACK = M("jugcraft:equipment_rack", (60, 64, 70))
WATER = M("minecraft:water", (52, 92, 158))
RAIL = M("minecraft:rail[shape=east_west]", (110, 100, 90))
RAIL_NS = M("minecraft:rail[shape=north_south]", (110, 100, 90))
LADDER = {f: M(f"minecraft:ladder[facing={f}]", (150, 120, 80)) for f in ("north", "south", "east", "west")}
GRATE = M("minecraft:iron_trapdoor[half=top,facing=north]", (160, 162, 166))
BARREL = M("minecraft:barrel[facing=up]", (120, 90, 60))
CHEST = M("minecraft:chest[facing=north]", (150, 110, 60))


def stair(material, facing, half="bottom"):
    return M(f"minecraft:{material}_stairs[facing={facing},half={half},shape=straight]", (130, 130, 130))


def door(facing, half, hinge="left"):
    return M(f"minecraft:iron_door[facing={facing},half={half},hinge={hinge},open=false]", (200, 200, 200))


def furniture(name, facing):
    return M(f"jugcraft:{name}[facing={facing}]", (70, 74, 80))


def trapdoor_grate(facing="north"):
    return M(f"minecraft:iron_trapdoor[half=top,facing={facing}]", (160, 162, 166))


OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}


# ------------------------------------------------------------------------------------------------- building kit

class Building(Part):
    """A building with floors, rooms and fittings. Coordinates are the part's own (x across, z front-to-back toward
    the front +z, y up from the ground floor slab at 0)."""

    def __init__(self, key, name, w, d, h, sink=0):
        super().__init__(key, name, w, h + 2 + sink, d)
        self.sink = sink
        self.base = sink  # y of the ground floor slab in the grid

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

    def shell_walls(self, h, wall, base=BRICK, base_h=2, pilaster=ANDESITE, roof=CONCRETE_DARK, parapet=SMOOTH, step=8):
        """Outer walls (stone-brick plinth, wall, pilasters every step), the ground slab and a flat roof with parapet."""
        w, _, d = self.size
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

    def floor(self, y, x0, z0, x1, z1, m=SMOOTH, ceiling_lights=True, light_step=6, light_y=None):
        """A floor slab at y (its top is walked on at y + 1), with ceiling lights under it for the storey below."""
        self.b(x0, y, z0, x1, y, z1, m)
        if ceiling_lights and y > 1:
            ly = y - 1 if light_y is None else light_y
            for x in range(x0 + 2, x1 - 1, light_step):
                for z in range(z0 + 2, z1 - 1, light_step):
                    if self.get(x, ly, z) == 0:
                        self.p(x, ly, z, LIGHT)

    def lights(self, y, x0, z0, x1, z1, step=6, m=LIGHT):
        for x in range(x0, x1 + 1, step):
            for z in range(z0, z1 + 1, step):
                if self.get(x, y, z) == 0:
                    self.p(x, y, z, m)

    def wall_x(self, z, x0, x1, y0, y1, m=WHITE):
        """An interior wall along x at z."""
        self.b(x0, y0, z, x1, y1, z, m)

    def wall_z(self, x, z0, z1, y0, y1, m=WHITE):
        self.b(x, y0, z0, x, y1, z1, m)

    def door(self, x, y, z, facing, wall=None):
        """An iron door in a wall, 2 high, with the wall above it kept (a door frame)."""
        self.p(x, y, z, door(facing, "lower"))
        self.p(x, y + 1, z, door(facing, "upper"))

    def opening(self, x0, y0, z0, x1, y1, z1):
        self.c(x0, y0, z0, x1, y1, z1)

    def stairs_run(self, x, z, y0, y1, facing, width=2, material="stone_brick", along=None):
        """A straight flight from the floor at y0 (first step at y0 + 1) up to y1, climbing toward facing, width wide
        to the right of facing. Clears the headroom above it."""
        dx, dz = P.DIRS[facing]
        rx, rz = P.DIRS[P.CLOCKWISE[facing]]
        for i in range(y1 - y0):
            for k in range(width):
                sx, sz = x + dx * i + rx * k, z + dz * i + rz * k
                self.p(sx, y0 + 1 + i, sz, stair(material, facing))
                for up in range(1, 4):
                    if self.get(sx, y0 + 1 + i + up, sz) and y0 + 1 + i + up <= y1:
                        self.p(sx, y0 + 1 + i + up, sz, 0)
                for below in range(y0 + 1, y0 + 1 + i):
                    if self.get(sx, below, sz) == 0:
                        pass

    def stairwell(self, x0, z0, levels, facing="north", wall=CONCRETE, material="stone_brick"):
        """A switchback stair shaft (5 wide, 2 flights per storey) between the floor levels given (each a slab y):
        walls round it, a door on each level on the side facing back, landings, a light on each landing."""
        # Shaft: 6 x 10 inside walls (footprint x0..x0+6, z0..z0+11 for facing north).
        w, d = 7, 12
        top = levels[-1] + 4
        self.b(x0, levels[0] + 1, z0, x0 + w - 1, top, z0 + d - 1, wall)
        self.c(x0 + 1, levels[0] + 1, z0 + 1, x0 + w - 2, top - 1, z0 + d - 2)
        for i in range(len(levels) - 1):
            lo, hi = levels[i], levels[i + 1]
            mid = lo + (hi - lo) // 2
            # Flight 1 up the west half toward -z, landing at the far end, flight 2 back up the east half.
            self.b(x0 + 1, mid, z0 + 1, x0 + w - 2, mid, z0 + 2, SMOOTH)
            for k in range(mid - lo):
                for xx in (x0 + 1, x0 + 2):
                    zz = z0 + d - 3 - k
                    self.p(xx, lo + 1 + k, zz, stair(material, "north"))
            for k in range(hi - mid):
                for xx in (x0 + 4, x0 + 5):
                    zz = z0 + 3 + k
                    self.p(xx, mid + 1 + k, zz, stair(material, "south"))
            self.b(x0 + 1, hi, z0 + d - 3, x0 + w - 2, hi, z0 + d - 2, SMOOTH)
            self.p(x0 + 3, mid + 3, z0 + 1, LIGHT) if self.get(x0 + 3, mid + 3, z0 + 1) == 0 else None
        for lv in levels:
            self.b(x0 + 1, lv, z0 + d - 3, x0 + w - 2, lv, z0 + d - 2, SMOOTH)
            self.door(x0 + 3, lv + 1, z0 + d - 1, "south")
            if self.get(x0 + 2, lv + 4, z0 + d - 2) == 0:
                self.p(x0 + 2, lv + 4, z0 + d - 2, LIGHT)

    def prop(self, kind, x0, y, z0, facing="north"):
        super().prop(kind, x0, y + self.base, z0, facing)

    def furniture(self, name, x, y, z, facing):
        self.p(x, y, z, furniture(name, facing))

    def desk(self, x, y, z, facing):
        """An operator or office desk with its chair (the chair on the user's side)."""
        self.furniture("console_desk", x, y, z, facing)
        dx, dz = P.DIRS[OPPOSITE[facing]]
        self.furniture("operator_chair", x + dx, y, z + dz, facing)

    def sign_row(self, x0, x1, y, z, m=HAZARD):
        self.b(x0, y, z, x1, y, z, m)


def entrance(b, x, z, side, width=2, canopy=True):
    """A front door with a stone frame and a slab canopy on an outer wall (side n/s/e/w)."""
    facing = {"n": "north", "s": "south", "w": "west", "e": "east"}[side]
    rx, rz = P.DIRS[P.CLOCKWISE[facing]]
    for k in range(width):
        b.door(x + rx * k, 1, z + rz * k, facing, )
    fx, fz = P.DIRS[facing]
    if canopy:
        for k in range(-1, width + 1):
            b.p(x + rx * k + fx, 4, z + rz * k + fz, SMOOTH_SLAB)
        b.p(x - rx + fx, 3, z - rz + fz, LANTERN) if False else None


# ------------------------------------------------------------------------------------------------- reactor building

def containment(s, ox, oz):
    """The reactor building (containment): a cylinder 51 across and 66 tall with a domed roof, a steel liner inside.

    Inside, like a four-loop PWR: the vessel in its reactor cavity behind the primary shield wall; each steam
    generator and its coolant pump in a compartment behind the crane wall's radial walls; the pressurizer in its own
    compartment; grating levels round the steam generators; the operating floor at +16 with the refuelling cavity
    over the vessel (the head and drive forest show in it) and the refuelling machine on rails; the polar crane on its
    ring girder high up; the annulus between the crane wall and the liner with stairs up to the operating floor; the
    personnel airlock and the equipment hatch at ground level. Returns the steam header's exit (x, z)."""
    r, wall_h, dome_h = 24.5, 46, 20
    size = 51
    c = size / 2
    b = Building("pwr_containment", "Reactor Containment (PWR)", size, size, wall_h + dome_h)
    b.disc(c, c, r + 0.8, 0, 0, SMOOTH)
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
        b.disc(c, c, rr, wall_h + y, wall_h + y, SHIELD, inner=max(-1, rr - 2.6))
    b.disc(c, c, 3, wall_h + dome_h, wall_h + dome_h, CONCRETE_DARK)
    inner = r - 2.7  # inside the liner
    # Basemat floor inside.
    b.disc(c, c, inner, 0, 0, TILES)
    cv = int(c)  # 25: the vessel's middle row and column
    # Primary shield wall: the reactor cavity round the vessel, up to the operating floor.
    floor_y = 16
    for y in range(1, floor_y):
        b.ring(c, c, 6.5, y, 1.2, SHIELD)
    # The crane wall (secondary shield) round the whole primary, with radial walls between the loops.
    crane_r = 19.5
    for y in range(1, 30):
        b.ring(c, c, crane_r, y, 1.0, CONCRETE)
    for y in range(1, floor_y):
        # (No wall north: the pressurizer has its own compartment there.)
        b.box(cv, y, int(c + 7), cv, y, int(c + crane_r), CONCRETE)
        b.box(int(c - crane_r), y, cv, int(c - 7), y, cv, CONCRETE)
        b.box(int(c + 7), y, cv, int(c + crane_r), y, cv, CONCRETE)
    # Doors through the crane wall into each loop compartment from the annulus.
    for a in (math.pi / 4, 3 * math.pi / 4, 5 * math.pi / 4, 7 * math.pi / 4):
        x, z = int(c + math.cos(a) * crane_r), int(c + math.sin(a) * crane_r)
        b.clear(x - 1, 1, z - 1, x + 1, 3, z + 1)
    # Grating levels round the steam generators (+8) inside the compartments.
    for gy in (8,):
        for x in range(size):
            for z in range(size):
                dx, dz = x + 0.5 - c, z + 0.5 - c
                rr = math.hypot(dx, dz)
                if 8.5 < rr < crane_r - 0.6 and abs(x - cv) > 1 and abs(z - cv) > 1 and b.g[x, gy, z] == 0:
                    b.g[x, gy, z] = GRATE
    # Operating floor (+16) over everything inside the liner, open over the reactor cavity (the refuelling cavity).
    for x in range(size):
        for z in range(size):
            dx, dz = x + 0.5 - c, z + 0.5 - c
            if math.hypot(dx, dz) < inner - 0.2:
                b.g[x, floor_y, z] = SMOOTH
    cav = 6
    b.clear(cv - cav, floor_y, cv - cav, cv + cav, floor_y, cv + cav)
    # The refuelling cavity's stainless-lined walls rise a little above the floor, with a handrail.
    for y in range(floor_y - 3, floor_y + 2):
        for x in range(cv - cav - 1, cv + cav + 2):
            for z in (cv - cav - 1, cv + cav + 1):
                b.g[x, y, z] = LINER if y <= floor_y else BARS
        for z in range(cv - cav - 1, cv + cav + 2):
            for x in (cv - cav - 1, cv + cav + 1):
                b.g[x, y, z] = LINER if y <= floor_y else BARS
    # Steam generator shafts: the operating floor is open round each steam generator (they rise through it) with a
    # concrete curb and railing; the pressurizer too.
    span = 10
    for gx in (cv - span, cv + span):
        for gz in (cv - span, cv + span):
            b.clear(gx - 4, floor_y, gz - 4, gx + 4, floor_y, gz + 4)
            for x in range(gx - 5, gx + 6):
                for z in (gz - 5, gz + 5):
                    b.g[x, floor_y + 1, z] = BARS
            for z in range(gz - 5, gz + 6):
                for x in (gx - 5, gx + 5):
                    b.g[x, floor_y + 1, z] = BARS
    b.clear(cv - 2, floor_y, cv - 12, cv + 2, floor_y, cv - 8)
    # The pressurizer's compartment walls.
    for y in range(1, 24):
        b.box(cv - 3, y, cv - 13, cv + 3, y, cv - 13, CONCRETE)
        b.box(cv - 3, y, cv - 13, cv - 3, y, cv - 8, CONCRETE)
        b.box(cv + 3, y, cv - 13, cv + 3, y, cv - 8, CONCRETE)
    b.clear(cv - 3, 1, cv - 10, cv - 3, 2, cv - 10)
    # Refuelling machine rails on the cavity's side walls (it runs along the cavity, north-south).
    for z in range(cv - cav - 4, cv + cav + 5):
        for x in (cv - cav - 1, cv + cav + 1):
            b.g[x, floor_y + 1, z] = RAIL_NS
    # The polar crane's ring girder (a concrete corbel round the liner) and lights under it.
    gy = 40
    b.disc(c, c, inner, gy, gy + 1, CONCRETE, inner=inner - 2)
    for k in range(24):
        a = 2 * math.pi * k / 24
        x, z = int(c + math.cos(a) * (inner - 2.5)), int(c + math.sin(a) * (inner - 2.5))
        b.g[x, gy - 1, z] = LIGHT
        x, z = int(c + math.cos(a) * (inner - 1.5)), int(c + math.sin(a) * (inner - 1.5))
        b.g[x, 30, z] = LANTERN
    # Lights under the operating floor and above the grating.
    for x in range(4, size - 4, 6):
        for z in range(4, size - 4, 6):
            dx, dz = x + 0.5 - c, z + 0.5 - c
            rr = math.hypot(dx, dz)
            if rr < inner - 1 and (rr > 8 or rr < 0) and b.g[x, floor_y - 1, z] == 0:
                b.g[x, floor_y - 1, z] = LIGHT
            if rr < inner - 1 and rr > 8 and b.g[x, 7, z] == 0:
                b.g[x, 7, z] = LIGHT
    # Ladders up the outside of the crane wall in the annulus, west and east, to the operating floor (with a hatch).
    for x, face in ((int(c - crane_r) - 2, "west"), (int(c + crane_r) + 1, "east")):
        for y in range(1, floor_y + 1):
            b.g[x, y, cv + 3] = LADDER[face]
        b.g[x, floor_y, cv + 3] = LADDER[face]
    # Personnel airlock (west) and equipment hatch (south): openings through the wall with steel doors.
    b.box(0, 1, cv - 1, 4, 4, cv + 1, LINER)
    b.clear(0, 1, cv, 4, 2, cv)
    b.g[0, 1, cv] = door("west", "lower")
    b.g[0, 2, cv] = door("west", "upper")
    b.g[4, 1, cv] = door("west", "lower")
    b.g[4, 2, cv] = door("west", "upper")
    b.box(cv - 4, 1, size - 4, cv + 4, 10, size - 1, CONCRETE)
    b.box(cv - 3, 1, size - 1, cv + 3, 8, size - 1, LINER)
    b.box(cv - 3, 1, size - 4, cv + 3, 8, size - 2, 0)
    # Warning lights at the hatch, radiation signs.
    b.g[cv - 4, 9, size - 1] = WARN_LIGHT
    b.g[cv + 4, 9, size - 1] = WARN_LIGHT
    # Decor: the polar crane and the refuelling machine.
    b.prop("polar_crane", cv - 20, gy + 2, cv - 2, "north")
    b.prop("refueling_machine", cv - cav - 1, floor_y + 2, cv - cav, "north")
    s.add(b, ox, oz)
    return b


def place_primary(s, ox, oz):
    """The working reactor equipment in the containment at (ox, oz): the four loops of tools/nuclear_plants.py."""
    return P.four_loops(s, ox, oz, 25.5, 10)


# ------------------------------------------------------------------------------------------------- turbine hall

def turbine_hall(s, ox, oz):
    """The turbine building, 100 x 44 and 36 tall: the turbine-generator on a raised concrete pedestal (the operating
    floor at +12), the condenser in its pit under the turbine, the moisture separator reheaters either side, feedwater
    heaters and the feedwater pump on the ground floor, the laydown bay at the west end open to the crane, an overhead
    crane on runway beams, plant rooms along the south wall, stairs at both ends."""
    W, D, H = 100, 44, 36
    b = Building("pwr_turbine_hall", "Turbine Hall", W, D, H)
    b.shell_walls(H, CONCRETE, step=10)
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
    b.windows(4, 6, "s", step=10)
    b.floor(0, 1, 1, W - 2, D - 2, TILES, ceiling_lights=False)
    # Operating floor at +12 (open over the laydown bay at the west end).
    b.floor(12, 15, 1, W - 2, D - 2, SMOOTH)
    for x in range(15, W - 1, 10):
        for z in (1, D - 2):
            pass
    # The turbine pedestal: a concrete table under the turbine, on columns.
    for x in range(34, 88, 6):
        for z in (14, 15, 27, 28):
            b.b(x, 1, z, x + 1, 11, z, CONCRETE_DARK)
    b.b(34, 12, 14, 87, 12, 28, CONCRETE_DARK)
    # Column lines under the operating floor, and the floor's edge railing at the laydown bay.
    for x in range(20, W - 1, 10):
        for z in (8, D - 9):
            b.b(x, 1, z, x, 11, z, COLUMN)
    for z in range(1, D - 1):
        b.p(15, 13, z, BARS)
    # Crane runway beams along both long walls, rails on them.
    for x in range(1, W - 1):
        b.p(x, 30, 1, FRAME)
        b.p(x, 30, D - 2, FRAME)
    # High-bay lights under the roof and lanterns in the pilasters.
    b.lights(H - 1, 4, 4, W - 5, D - 5, step=8)
    for x in range(10, W - 1, 10):
        b.p(x, 24, 1, LANTERN)
        b.p(x, 24, D - 2, LANTERN)
    # The laydown bay's roller door (west end) and personnel doors.
    b.b(0, 1, 17, 0, 9, 26, M("jugcraft:hangar_bay_door", (120, 124, 128)))
    entrance(b, 0, 30, "w")
    entrance(b, W - 1, 6, "e")
    # Plant rooms along the south wall on the ground floor: switchgear, lube oil, workshop, the turbine control room.
    b.wall_x(D - 12, 40, W - 2, 1, 11, CONCRETE)
    for x in (55, 70, 85):
        b.wall_z(x, D - 11, D - 2, 1, 11, CONCRETE)
    for x, name in ((47, "switchgear"), (62, "lube"), (77, "workshop"), (91, "control")):
        b.door(x, 1, D - 12, "north")
    b.prop("switchgear_lineup", 42, 1, D - 4, "south")
    b.prop("switchgear_lineup", 44, 1, D - 10, "north")
    b.prop("storage_tank", 60, 1, D - 9, "north")
    for x in range(72, 84, 3):
        b.furniture("equipment_rack", x, 1, D - 3, "north")
    b.desk(88, 1, D - 6, "south")
    b.desk(92, 1, D - 6, "south")
    b.furniture("control_screen", 90, 3, D - 2, "north") if False else None
    # Stairwells at both ends, ground to the operating floor and up to the crane walkway.
    b.stairwell(2, 1, [0, 12, 24])
    b.stairwell(W - 9, D - 25, [0, 12, 24])
    # Feedwater heaters on the ground floor (north side), the moisture separator reheaters on the operating floor.
    for z in (3, 7):
        b.prop("feedwater_heater", 22, 1, z, "east")
    b.prop("moisture_separator_reheater", 36, 13, 6, "east")
    b.prop("moisture_separator_reheater", 36, 13, 31, "east")
    b.prop("overhead_crane", 18, 31, 2, "east")
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- control building

def control_building(s, ox, oz):
    """The control building, 40 x 30, three storeys: the entrance, battery and HVAC rooms on the ground floor; the
    relay room, cable spreading room and plant computer room above; the main control room on top: a horseshoe of
    control boards under the plant display wall, operator desks, the reactor console in front, the shift supervisor's
    glass office, a kitchen. Returns the reactor console's site position (x, z, y)."""
    W, D, H = 40, 30, 22
    b = Building("control_building", "Control Building", W, D, H)
    b.shell_walls(H, CONCRETE, step=8)
    b.windows(3, 5, "s", step=8)
    b.windows(9, 11, "ns", step=8)
    b.floor(0, 1, 1, W - 2, D - 2, TILES, ceiling_lights=False)
    b.floor(7, 1, 1, W - 2, D - 2, SMOOTH)
    b.floor(14, 1, 1, W - 2, D - 2, ACCESS_FLOOR)
    b.lights(H - 1, 3, 3, W - 4, D - 4, step=5)
    entrance(b, 18, D - 1, "s")
    b.stairwell(W - 9, 1, [0, 7, 14])
    # Ground floor: lobby with a security turnstile line, battery rooms, HVAC.
    b.wall_x(12, 1, W - 10, 1, 6)
    b.door(18, 1, 12, "south")
    for x in range(10, 28, 2):
        b.p(x, 1, 18, BARS)
    b.p(18, 1, 18, 0)
    b.p(19, 1, 18, 0)
    b.wall_z(14, 1, 11, 1, 6)
    b.door(14, 1, 6, "east")
    for x in range(2, 13, 2):
        for z in (2, 5, 8):
            b.p(x, 1, z, M("jugcraft:battery_box", (80, 84, 90)))
    b.prop("air_handling_unit", 17, 1, 2, "east")
    b.prop("air_handling_unit", 17, 1, 7, "east") if False else None
    # Level 2: relay room (rows of racks), cable spreading room (cable trays), the plant computer room.
    b.wall_z(16, 1, D - 2, 8, 13)
    b.wall_z(26, 1, D - 2, 8, 13)
    b.door(16, 8, 15, "east")
    b.door(26, 8, 15, "east")
    for x in range(2, 15, 3):
        for z in range(2, D - 2):
            if z % 6 != 0:
                b.furniture("equipment_rack", x, 8, z, "east")
    for x in range(17, 26):
        for z in range(2, D - 2, 3):
            b.furniture("cable_tray", x, 12, z, "east")
    for z in range(3, D - 3, 4):
        b.furniture("equipment_rack", 27, 8, z, "east")
        b.desk(30, 8, z, "west")
    # Main control room (level 3): acoustic walls, the boards in a horseshoe, the display wall, desks, the console.
    for x in range(1, W - 1):
        for y in range(15, H):
            for z in (1, D - 2):
                if b.get(x, y, z) == 0:
                    b.p(x, y, z, ACOUSTIC)
    for z in range(1, D - 1):
        for y in range(15, H):
            for x in (1,):
                if b.get(x, y, z) == 0:
                    b.p(x, y, z, ACOUSTIC)
    b.prop("plant_display_wall", 9, 17, 2, "north")
    for x in (3, 9, 15, 21):
        b.prop("main_control_board", x, 15, 3, "north")
    b.prop("main_control_board", 2, 15, 6, "west")
    b.prop("main_control_board", 2, 15, 12, "west")
    b.prop("main_control_board", 26, 15, 6, "east")
    for x in (7, 11, 15, 19):
        b.desk(x, 15, 14, "north")
    # The shift supervisor's glass office and a kitchen in the south-west corner.
    for x in range(2, 13):
        b.p(x, 15, 20, PANE)
        b.p(x, 16, 20, PANE)
        b.p(x, 17, 20, PANE)
    b.door(7, 15, 20, "north")
    b.b(2, 14, 21, 12, 14, D - 2, CARPET)
    b.desk(5, 15, 24, "north")
    b.desk(9, 15, 24, "north")
    b.wall_z(20, 21, D - 2, 15, H - 1)
    b.door(20, 15, 24, "east")
    s.add(b, ox, oz)
    return (ox + 18, oz + 9, 15)


# ------------------------------------------------------------------------------------------------- auxiliary building

def aux_building(s, ox, oz):
    """The nuclear auxiliary building, 60 x 40, three storeys of plant rooms on either side of a central corridor:
    charging, residual heat removal and safety injection pumps, component cooling heat exchangers, the boric acid and
    volume control tanks, filters, the ventilation plant. Rooms with radioactive systems are entered through shielding
    mazes; the health physics checkpoint is at the entrance; valve galleries run pipework along the corridors."""
    W, D, H = 60, 40, 24
    b = Building("aux_building", "Nuclear Auxiliary Building", W, D, H)
    b.shell_walls(H, CONCRETE, step=10)
    b.windows(4, 5, "s", step=10)
    b.floor(0, 1, 1, W - 2, D - 2, TILES, ceiling_lights=False)
    b.floor(8, 1, 1, W - 2, D - 2, SMOOTH)
    b.floor(16, 1, 1, W - 2, D - 2, SMOOTH)
    b.lights(H - 1, 3, 3, W - 4, D - 4, step=6)
    entrance(b, 3, D - 1, "s")
    b.stairwell(W - 9, D - 13, [0, 8, 16])
    b.stairwell(9, 1, [0, 8, 16])
    corridor = (18, 21)
    for lv in (0, 8, 16):
        y0, y1 = lv + 1, lv + 7
        b.wall_x(corridor[0] - 1, 17, W - 2, y0, y1, CONCRETE)
        b.wall_x(corridor[1] + 1, 1, W - 11, y0, y1, CONCRETE)
        for x in range(20, W - 2, 10):
            b.wall_z(x, 1, corridor[0] - 2, y0, y1, CONCRETE)
            b.wall_z(x, corridor[1] + 2, D - 2, y0, y1, CONCRETE)
        # Room doors from the corridor, each behind a shielding maze wall.
        for x in range(25, W - 2, 10):
            b.door(x, y0, corridor[0] - 1, "south")
            b.wall_x(corridor[0] - 3, x - 1, x + 2, y0, y0 + 2, CONCRETE_DARK)
            b.door(x, y0, corridor[1] + 1, "north")
            b.wall_x(corridor[1] + 3, x - 2, x + 1, y0, y0 + 2, CONCRETE_DARK)
        # Pipe runs along the corridor ceiling and hazard striping on the floor edges.
        for x in range(1, W - 1):
            b.p(x, y1, corridor[0], PIPE)
            b.p(x, y1, corridor[1], PIPE)
        b.lights(y1, 4, corridor[0] + 1, W - 4, corridor[0] + 1, step=6)
    # Ground floor: pumps and heat exchangers.
    for i, x in enumerate(range(22, W - 4, 10)):
        b.prop("pump_skid", x + 1, 1, 4, "south")
        b.prop("pump_skid", x + 5, 1, 4, "south") if x + 8 < W - 2 else None
        b.prop("heat_exchanger", x + 2, 1, 25, "south")
    # Health physics checkpoint at the entrance (west end): radiation monitors and lockers.
    for z in range(23, D - 2, 2):
        b.p(2, 1, z, CHEST)
    b.p(6, 3, 22, WARN_LIGHT)
    # Level 2: tanks (double-height rooms through level 3).
    for x in (23, 33, 43):
        b.c(x, 16, 3, x + 6, 16, 9)
        b.prop("storage_tank", x + 1, 9, 3, "south")
    for x in range(22, W - 4, 10):
        b.prop("heat_exchanger", x + 2, 9, 25, "south")
    # Level 3: ventilation plant.
    for x in (22, 32):
        b.prop("air_handling_unit", x + 1, 17, 26, "south")
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- fuel building

def fuel_building(s, ox, oz):
    """The fuel building, 32 x 46: the spent fuel pool (12 deep, lined, blue with Cherenkov light from the racks), the
    fuel transfer canal toward the containment, the cask loading pit, the fuel handling machine on rails over the
    pool, and the truck bay with dry storage casks and new fuel storage at the south end."""
    W, D, H = 32, 46, 28
    b = Building("fuel_building", "Fuel Building", W, D, H)
    b.shell_walls(H, SHIELD, base=BRICK, step=8)
    b.windows(22, 25, "ew", step=8)
    b.floor(0, 1, 1, W - 2, D - 2, TILES, ceiling_lights=False)
    deck = 12
    b.floor(deck, 1, 1, W - 2, 30, SMOOTH)
    b.lights(H - 1, 3, 3, W - 4, D - 4, step=5)
    # The pool: lined walls and floor, racks on the floor, water up to the deck.
    px0, px1, pz0, pz1 = 6, 23, 4, 25
    b.b(px0, 1, pz0, px1, deck, pz1, LINER)
    b.c(px0 + 1, 2, pz0 + 1, px1 - 1, deck, pz1 - 1)
    for x in range(px0 + 1, px1):
        for z in range(pz0 + 1, pz1):
            for y in range(2, deck + 1):
                b.p(x, y, z, WATER)
    for x in range(px0 + 1, px1 - 3, 4):
        for z in range(pz0 + 1, pz1 - 3, 4):
            b.prop("spent_fuel_rack", x, 2, z, "north")
    # Transfer canal west toward the containment, and the cask pit east.
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
    # Handrails round the pool, rails for the handling machine along its sides.
    for x in range(px0 - 1, px1 + 2):
        for z in (pz0 - 1, pz1 + 1):
            b.p(x, deck + 1, z, BARS)
    for z in range(pz0 - 2, pz1 + 3):
        b.p(px0 - 1, deck + 1, z, RAIL_NS)
        b.p(px1 + 1, deck + 1, z, RAIL_NS)
    b.prop("fuel_handling_machine", px0 - 1, deck + 2, 12, "north")
    # The truck bay: roller door south, dry casks and new fuel racks.
    b.b(10, 1, D - 1, 21, 9, D - 1, M("jugcraft:hangar_bay_door", (120, 124, 128)))
    for x in (3, 7, 24):
        b.prop("dry_storage_cask", x, 1, 36, "north")
    for z in range(33, 42, 2):
        b.furniture("equipment_rack", 28, 1, z, "west")
    b.stairwell(1, 31, [0, deck])
    entrance(b, W - 1, 36, "e")
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- diesel building

def diesel_building(s, ox, oz):
    """The emergency diesel generator building, 46 x 20: two generator rooms (one per safety train) with their
    control panels and day tanks, air intake louvres, and exhaust stacks above the roof that smoke."""
    W, D, H = 46, 20, 12
    b = Building("diesel_building", "Emergency Diesel Building", W, D, H)
    b.shell_walls(H, CONCRETE, step=8)
    b.floor(0, 1, 1, W - 2, D - 2, TILES, ceiling_lights=False)
    b.wall_z(W // 2, 1, D - 2, 1, H)
    b.lights(H - 1, 3, 3, W - 4, D - 4, step=5)
    for room, x0 in enumerate((2, W // 2 + 2)):
        b.prop("emergency_diesel_generator", x0 + 2, 1, 7, "east")
        b.prop("storage_tank", x0 + 15, 1, 2, "north")
        for x in range(x0, x0 + 8, 2):
            b.furniture("equipment_rack", x, 1, D - 3, "north")
        # Louvres on the north wall, the stack over the engine.
        for x in range(x0 + 2, x0 + 16):
            if x % 8:
                b.p(x, 6, 0, BARS)
                b.p(x, 7, 0, BARS)
        sx = x0 + 9
        b.b(sx, H + 1, 9, sx + 1, H + 7, 10, BRICK)
        b.p(sx, H + 7, 9, M("minecraft:campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]", (120, 80, 40)))
        entrance(b, x0 + 10, D - 1, "s")
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- pump house

def pump_house(s, ox, oz):
    """The circulating water intake and pump house, 40 x 30: four intake bays reaching out over the lake (concrete
    walls down into the water, a travelling screen in each), the pump floor with a circulating water pump over each
    bay's suction, an electrical room, stairs and an overhead crane."""
    W, D, H, sink = 40, 30, 14, 4
    b = Building("pump_house", "Intake Pump House", W, D, H, sink=sink)
    bays, bw = 4, 10
    # The intake structure: a concrete deck over the bays, bay walls down into the lake.
    b.b(0, 0, 0, W - 1, 0, 6, CONCRETE)
    for i in range(bays + 1):
        x = min(i * bw, W - 1)
        b.b(x, -sink, 0, x, 0, 6, CONCRETE)
    b.b(0, -sink, 6, W - 1, 0, 6, CONCRETE)
    for i in range(bays):
        x0 = i * bw + 1
        b.c(x0, 0, 0, x0 + bw - 2, 0, 5)
        b.b(x0, 1, 0, x0 + bw - 2, 1, 0, BARS)
        b.prop("travelling_water_screen", x0 + 1, -3, 2, "north")
    # The pump building behind.
    q = Building("x", "x", W, D - 7, H)
    q.shell_walls(H, CONCRETE, step=10)
    q.windows(8, 11, "nsew", step=10)
    q.floor(0, 1, 1, W - 2, D - 9, TILES, ceiling_lights=False)
    q.lights(H - 1, 3, 3, W - 4, D - 11, step=5)
    for x in range(1, W - 1):
        q.p(x, 10, 1, FRAME)
        q.p(x, 10, D - 9, FRAME)
    q.wall_x(D - 15, 1, W - 2, 1, 7, CONCRETE)
    q.door(20, 1, D - 15, "north")
    entrance(q, 5, D - 8, "s")
    for x in range(3, 15, 2):
        q.furniture("equipment_rack", x, 1, D - 10, "north")
    q.prop("switchgear_lineup", 24, 1, D - 10, "south")
    b.g[:, sink:sink + q.size[1], 7:] = q.g[:, :b.size[1] - sink, :]
    b.extras.update({(x, y + sink, z + 7): st for (x, y, z), st in q.extras.items()})
    b.props += [(k, x, y + sink, z + 7, f) for k, x, y, z, f in q.props]
    b.bays, b.bay_w = bays, bw
    s.add(b, ox, oz)
    return b


# ------------------------------------------------------------------------------------------------- site buildings

def admin_building(s, ox, oz):
    """The administration building, 60 x 22, three storeys of offices off a central corridor, the lobby and a
    cafeteria on the ground floor, a meeting room on each floor."""
    W, D, H = 60, 22, 14
    b = Building("admin_building", "Administration Building", W, D, H)
    b.shell_walls(H, WHITE, step=6)
    for y0 in (2, 7, 11):
        b.windows(y0, y0 + 1, "nsew", step=6)
    b.floor(0, 1, 1, W - 2, D - 2, TILES, ceiling_lights=False)
    b.floor(5, 1, 1, W - 2, D - 2, SMOOTH)
    b.floor(10, 1, 1, W - 2, D - 2, SMOOTH)
    b.lights(H - 1, 3, 3, W - 4, D - 4, step=4)
    entrance(b, 29, D - 1, "s")
    b.stairwell(1, 5, [0, 5, 10])
    for lv in (0, 5, 10):
        y0, y1 = lv + 1, lv + 4
        b.wall_x(9, 9, W - 2, y0, y1)
        b.wall_x(12, 9, W - 2, y0, y1)
        b.b(9, lv, 10, W - 2, lv, 11, CARPET if lv else TILES)
        for x in range(9, W - 2, 6):
            b.wall_z(x, 1, 8, y0, y1)
            b.wall_z(x, 13, D - 2, y0, y1)
            b.door(x + 3, y0, 9, "north")
            b.door(x + 3, y0, 12, "south")
            if lv or x > 30:
                b.desk(x + 2, y0, 3, "north")
                b.desk(x + 4, y0, 16, "south")
    s.add(b, ox, oz)
    return b


def gatehouse(s, ox, oz):
    """The security gatehouse: a guard room with desks and screens, the personnel turnstiles, a vehicle barrier."""
    W, D, H = 16, 12, 5
    b = Building("security_gatehouse", "Security Gatehouse", W, D, H)
    b.shell_walls(H, WHITE, step=5)
    b.windows(2, 3, "nsew", step=5)
    b.floor(0, 1, 1, W - 2, D - 2, TILES, ceiling_lights=False)
    b.lights(H - 1, 3, 3, W - 4, D - 4, step=4)
    for x in (3, 7):
        b.desk(x, 1, 3, "north")
    for x in range(10, 15):
        b.furniture("equipment_rack", x, 1, 1, "south")
    for z in range(4, 10, 2):
        b.b(11, 1, z, 14, 1, z, BARS)
    entrance(b, 6, D - 1, "s")
    entrance(b, 6, 0, "n")
    s.add(b, ox, oz)
    return b


def warehouse(s, ox, oz):
    """The warehouse: steel shelving with stores, roller doors, an office in the corner."""
    W, D, H = 40, 26, 12
    b = Building("warehouse", "Warehouse", W, D, H)
    b.shell_walls(H, CLADDING, step=8)
    b.windows(9, 10, "ns", step=8)
    b.floor(0, 1, 1, W - 2, D - 2, TILES, ceiling_lights=False)
    b.lights(H - 1, 3, 3, W - 4, D - 4, step=5)
    for z in range(4, D - 4, 4):
        for x in range(4, W - 10):
            for y in (1, 4, 7):
                b.p(x, y, z, FRAME if y > 1 else SMOOTH_SLAB)
                b.p(x, y + 1, z, BARREL if (x + y) % 3 else CHEST)
    b.b(W - 1, 1, 8, W - 1, 6, 15, M("jugcraft:hangar_bay_door", (120, 124, 128)))
    entrance(b, 4, D - 1, "s")
    s.add(b, ox, oz)
    return b


def outdoor_tank(s, cx, cz, r, h, name, key):
    """A big outdoor storage tank (refuelling water, condensate) with a ladder and a railing round the top."""
    size = int(2 * r) + 3
    c = size / 2
    b = Building(key, name, size, size, h + 2)
    b.disc(c, c, r + 1, 0, 0, SMOOTH)
    b.disc(c, c, r, 1, h, IRON, inner=r - 1.2)
    b.disc(c, c, r, h + 1, h + 1, IRON)
    b.ring(c, c, r + 0.4, h + 2, 1.0, BARS)
    for y in range(1, h + 2):
        b.put(int(c), y, size - 1, LADDER["south"])
    s.add(b, int(cx - c), int(cz - c))


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
    for x in range(x0, x1):
        for z in range(z0, z1):
            line = (x - x0) % 4 == 0 and (z - z0) % 14 not in (6, 7)
            s.decor[(x, -1, z)] = ROAD_LINE if line else ASPHALT


# ------------------------------------------------------------------------------------------------- the site

def medium_site():
    """The whole Medium Nuclear Plant, laid out like a real single-unit station: the power block by the lake (pump
    house, reactor building, turbine hall), the auxiliary, fuel and control buildings round the reactor, the diesel
    building, the transformer yard and switchyard to the east, all inside a double security fence; outside it the
    administration building, the gatehouse, parking, the warehouse and the two cooling towers."""
    s = P.Site("medium_pwr", "Medium Plant (4-loop PWR)", 372, 330, water=(0, -40, 372, 0))
    S = lambda *v: v
    # Power block.
    containment(s, 60, 40)
    sx, sz = place_primary(s, 60, 40)
    turbine_hall(s, 124, 38)
    s.eq("turbine_generator", 160, 54, "east", y=13)
    s.eq("condenser", 170, 54, "east", y=7)
    s.eq("main_transformer", 228, 57, "east")
    s.eq("feedwater_pump", 130, 66)
    # Main steam: out of the containment in the pipe bridge, across the hall above the operating floor, down onto
    # the turbine; the feedwater pump's line down from it.
    s.line(P.STEAM, (sx + 1, 22, sz), (185, 22, sz))
    s.line(P.STEAM, (185, 22, 60), (185, 22, sz))
    s.line(P.STEAM, (185, 21, 60), (185, 21, 60))
    s.line(P.STEAM, (131, 1, sz), (131, 21, sz))
    s.gallery(111, 123, sz - 2, sz + 2, 20, 24, [(112, sz - 2), (112, sz + 2), (122, sz - 2), (122, sz + 2)])
    # Isolated phase bus from the generator out through the end wall to the main transformer.
    s.line(P.BUS, (210, 14, 59), (230, 14, 59))
    s.line(P.BUS, (230, 6, 59), (230, 13, 59))
    # Around the reactor.
    aux_building(s, 30, 95)
    fuel_building(s, 100, 95)
    cx, cz, cy = control_building(s, 136, 88)
    s.eq("reactor_control_console", cx, cz, "north", y=cy)
    diesel_building(s, 180, 90)
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
    # Circulating water: lake -> condenser (riser up through the floor into its pit) -> both towers.
    s.conduit((legs[2], -1), (legs[2], 30), (172, 30), (172, 59))
    s.line(P.CONDUIT, (172, 0, 59), (172, 6, 59))
    s.conduit((172, 59), (172, 205), (120, 205), (120, 270))
    s.conduit((172, 205), (250, 205), (250, 270))
    # Site: fence round the protected area, admin, gatehouse, parking, warehouse; roads.
    admin_building(s, 40, 160)
    gatehouse(s, 140, 182)
    warehouse(s, 262, 110)
    fence(s, 0, 26, 250, 146, 123)
    parking(s, 160, 165, 230, 200)
    s.roads += [(0, 150, 372, 156), (120, 82, 127, 215), (100, 84, 240, 87)]
    return s
