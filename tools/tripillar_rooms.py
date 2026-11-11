"""Work list item 22, part A (approved by Justin on 23 October 2026 at take 1; layout 24): the Tri-Pillar control
building's empty ground-floor rooms fitted out, from the floor plans (claude/jugcraft-tripillar-floor-plans.md: crew
cafe south, battery + UPS south-east, lockers + showers south-west, security office west of pillar C, restrooms
north-west), as real control buildings have them, with the nuclear plant's interior kit (the same blocks and the same
room compositions Justin approved there: the restroom, the break room, the locker room, the guard room, the battery
rooms).

Each room is a box inside the hall like the data hall: walls y 1..4 (painted; framed glass where the room looks onto
the hall), a steel header band at y 5, its own ceiling tiles at y 5 with ceiling lights under them, and over the band
the dark carbon panel of the building's skin on up to the hall's ceiling (Justin, 24 Oct 2026: walls up to the
ceiling, option B), closing the space over the room; the hall's lights over a room stay in that space, lighting it. Where a room backs onto the building's curtain wall its glass there (y 1..4) is the dark
skin of the upper storey instead (no windows into the restrooms, lockers or battery room); the cafe keeps its windows.
Doors: glass for the cafe and the security office, steel for the rest, with the plant's room plaques beside them (over
a glass door the plaque goes on the band, never on the glass). One new piece: the locker room's shower stalls
(tools/shower_stall.py).

The block states come from the plant's own helpers (tools/medium_plant.py), so they are the ones its rooms use.
"""
from __future__ import annotations

import re

import nuclear_plants as P
import medium_plant as MP

FACINGS = ["north", "east", "south", "west"]
WALL = "jugcraft:painted_wall"
SKIN = "jugcraft:carbon_composite_panel"
BAND = "jugcraft:steel_armor_plate"
CEIL = "jugcraft:ceiling_tile"
LAMP = "jugcraft:ceiling_light_panel"
TILE_FLOOR = "jugcraft:restroom_floor_tile"
VINYL = "jugcraft:vinyl_floor"
CARPET = "jugcraft:carpet_tile"
Y0, Y1, TOP = 1, 4, 5          # walls y 1..4, header band and ceiling y 5


def S(m):
    """A plant material id's block state."""
    return P.MATERIALS[int(m)][0]


def furn(name, facing):
    return S(MP.furniture(name, facing))


def turn(facing, k):
    return FACINGS[(FACINGS.index(facing) + k) % 4]


def turn_state(state, k):
    """A block state turned k quarters clockwise (seen from above)."""
    if k % 4 == 0:
        return state
    state = re.sub(r"facing=(north|east|south|west)", lambda m: "facing=" + turn(m.group(1), k), state)
    if k % 2:
        state = re.sub(r"axis=(x|z)", lambda m: "axis=" + ("z" if m.group(1) == "x" else "x"), state)
    return state


def dirs(facing):
    return {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}[facing]


def right_of(facing):
    return dirs(turn(facing, 1))


class Rooms:
    """Places rooms into the tower's block dict b (put(b, x, y, z, state)); hex: the building's cells, edge: its curtain
    wall's cells. dy: the storey's floor slab (0 the ground floor's; the upper floor's FLOOR2): every y the rooms use is
    counted from it (the floor at 0, walls 1..4, the band and ceiling 5); lamp_y (the hall's lights over the rooms,
    taken out) is the tower's own y."""

    def __init__(self, b, put, glass, hex_cells, edge, lamp_y, dy=0):
        self.b, self._put, self.glass = b, put, glass
        self.hex, self.edge = hex_cells, edge
        self.lamp_y = lamp_y          # the hall's ceiling lights (taken out over the rooms)
        self.dy = dy
        self.rooms = {}               # name -> interior cells

    def put(self, x, y, z, state):
        self._put(self.b, x, y + self.dy, z, state)

    def get(self, x, y, z):
        return self.b.get((x, y + self.dy, z))

    def free(self, x, y, z):
        return (x, y + self.dy, z) not in self.b

    # -------------------------------------------------------------- the shell
    def interior(self, x0, z0, x1, z1):
        return {(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) if (x, z) in self.hex and (x, z) not in self.edge}

    def shell(self, name, cells, walls, curtain_solid=True):
        """A room: its interior cells, its walls {(x, z): "wall" | glass facing}; the ceiling, band and lights; the
        curtain wall beside it solid (y 1..4) unless curtain_solid is False."""
        self.rooms[name] = set(cells)
        for (x, z), kind in walls.items():
            if (x, z) in self.edge:
                continue
            for y in range(Y0, Y1 + 1):
                assert self.free(x, y, z) or self.get(x, y, z) in (WALL,), (name, x, y, z, self.get(x, y, z))
                self.put(x, y, z, WALL if kind == "wall" else self.glass(kind))
            self.put(x, TOP, z, BAND)
            self.bulkhead(x, z)
        for (x, z) in cells:
            self.put(x, TOP, z, CEIL)
            self.b.pop((x, self.lamp_y, z), None)
            if x % 5 == 0 and z % 5 == 0:
                self.put(x, Y1, z, LAMP)
        if curtain_solid:
            for (x, z) in cells:
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
                    e = (x + dx, z + dz)
                    if e in self.edge:
                        for y in range(Y0, Y1 + 1):
                            if self.get(e[0], y, e[1]) is not None and self.get(e[0], y, e[1]).startswith("fglass:"):
                                self.put(e[0], y, e[1], SKIN)

    def bulkhead(self, x, z):
        """Over a wall's band, the skin on up to the hall's ceiling (a hall light in the wall's line comes out)."""
        for y in range(TOP + 1, self.lamp_y - self.dy + 1):
            assert self.free(x, y, z) or self.get(x, y, z) in (LAMP, SKIN), (x, y, z, self.get(x, y, z))
            self.put(x, y, z, SKIN)

    def plenum_lights(self):
        """Over a room with its own ceiling (y 5) its walls run on up to the hall's ceiling, closing the space between:
        the hall's ceiling lights stay there (taken out with the rooms), and where none of them reaches a corner of that
        space another goes in over it, so all of it is lit and nothing spawns in it."""
        drop = self.lamp_y - (TOP + 1 + self.dy)        # from a light down to the space's floor
        reach = 15 - 3 - drop                          # a ceiling light's 15, less tripillar_lights.NEED and one to spare
        for cells in self.rooms.values():
            over = sorted((x, z) for (x, z) in cells if self.get(x, TOP, z) is not None)
            lamps = []
            for (x, z) in over:
                if x % 5 == 0 and z % 5 == 0 and self.get(x, TOP, z) == CEIL and (x, self.lamp_y, z) not in self.b:
                    self._put(self.b, x, self.lamp_y, z, LAMP)
                if self.b.get((x, self.lamp_y, z)) == LAMP:
                    lamps.append((x, z))
            for (x, z) in over:
                if not any(abs(x - lx) + abs(z - lz) <= reach for lx, lz in lamps) and (x, self.lamp_y, z) not in self.b:
                    self._put(self.b, x, self.lamp_y, z, LAMP)
                    lamps.append((x, z))

    def door(self, x, z, facing, kind="steel", double=False, label=None, plaque_side=1):
        """An automatic sliding door (tools/sliding_doors.py; Justin, 8 November 2026) in a wall cell (x, z), facing the way
        you walk in; double: two leaves, the second cell to the walker's left. Its leaves hang on the wall's outside face
        (the side you come from) and slide along it a block; a single's slides to the walker's left unless the wall there
        isn't clear (then right). The room's name on a plaque over it on the header band, facing you as you come: never
        on the glass, and clear of the leaves. (plaque_side: where the hinged doors' plaques went, beside them.)"""
        import sliding_doors as SD
        outside = turn(facing, 2)
        style = ("double_" if double else "single_") + ("glass" if kind == "glass" else "steel")
        mirrored = False
        if not double:
            def free(px, py, pz):
                return (px, py, pz) not in self.b
            y = Y0 + self.dy
            mirrored = not SD.clear(free, x, y, z, style, outside) and SD.clear(free, x, y, z, style, outside, True)
        SD.place(lambda px, py, pz, st: self._put(self.b, px, py, pz, st), x, Y0 + self.dy, z, style, outside, mirrored)
        if label:
            fx, fz = dirs(facing)
            self.put(x - fx, TOP, z - fz, S(MP.sign(label, turn(facing, 2))))

    # -------------------------------------------------------------- placing
    def f(self, name, x, y, z, facing):
        assert self.free(x, y, z), (name, x, y, z, self.get(x, y, z))
        self.put(x, y, z, furn(name, facing))

    def stack(self, name, x, z, facing, n=2):
        for k in range(n):
            assert self.free(x, Y0 + k, z), (name, x, z)
            self.put(x, Y0 + k, z, S(MP.furniture(name, facing, up=k < n - 1, down=k > 0)))

    def panel(self, name, x, z, facing, width, label=None, y=Y0):
        """A plant panel width wide (its left block as you face it at x, z), two rows high (plant_panels)."""
        import plant_panels
        p = plant_panels.PANELS.get(name, {})
        labels = p.get("labels")
        variant = labels.index(label) if labels and label else None
        rows = p.get("rows", 2)
        rx, rz = right_of(facing)
        for c in range(width):
            for row in range(rows):
                cx, cz = x + rx * c, z + rz * c
                assert self.free(cx, y + row, cz), (name, cx, y + row, cz, self.get(cx, y + row, cz))
                self.put(cx, y + row, cz, S(MP.multi_state(name, facing, row * width + c, variant or None)))

    def row(self, names, x, z, facing, tops=None, y=Y0):
        """Pieces that join side by side, from x, z to the user's right."""
        if isinstance(names, tuple):
            names = [names[0]] * names[1]
        rx, rz = right_of(facing)
        n = len(names)
        for i, name in enumerate(names):
            cx, cz = x + rx * i, z + rz * i
            assert self.free(cx, y, cz), (name, cx, cz, self.get(cx, y, cz))
            self.put(cx, y, cz, S(MP.row_state(name, facing, i > 0, i < n - 1, None if tops is None else tops[i])))

    def varied(self, name, x, y, z, facing, n):
        v = (x * 7 + z * 13 + y * 5) % n
        assert self.free(x, y, z), (name, x, y, z)
        self.put(x, y, z, S(MP.variant_state(name, facing, v)))

    def plant(self, x, z):
        self.varied("office_plant", x, Y0, z, "north", 8)

    def screen(self, name, x, z, facing):
        """A wall screen three across and two high, its bottom middle beside cell (x, z), at y 2..3."""
        rx, rz = right_of(facing)
        for r in range(2):
            for c in range(3):
                cx, cz = x + rx * (c - 1), z + rz * (c - 1)
                self.put(cx, Y0 + 1 + r, cz, S(MP.multi_state(name, facing, r * 3 + c)))

    def control_screen(self, x, z, facing, lift=0):
        """The drone depot's Control Screen, six panels 3 x 2 (y 2..3 over cell (x, z)'s side of the wall, centred on
        x, z; `lift` blocks higher), facing out from the wall: pinned with the tower's displays, it shows the depot live
        (fleet, flights, power, supply). Parts 1..6 row by row from the top left as seen from the front."""
        lx, lz = dirs(turn(facing, 1))          # the viewer's left, looking at it (they face the other way)
        for r in range(2):
            for c in range(3):
                cx, cz = x + lx * (1 - c), z + lz * (1 - c)
                self.put(cx, Y0 + 2 - r + lift, cz, f"jugcraft:control_screen[facing={facing},part={r * 3 + c + 1}]")

    def table(self, x, z):
        """The plant's round meeting table (3 x 3) with its middle at x, z and a chair at each of its twelve places."""
        for part in range(9):
            self.put(x + part % 3 - 1, Y0, z + part // 3 - 1, S(MP.table_state(part)))
        for k in (-1, 0, 1):
            self.put(x + k, Y0, z - 2, furn("operator_chair", "south"))
            self.put(x + k, Y0, z + 2, furn("operator_chair", "north"))
            self.put(x - 2, Y0, z + k, furn("operator_chair", "east"))
            self.put(x + 2, Y0, z + k, furn("operator_chair", "west"))

    def stamp(self, blocks, k, ox, oz, x0, z0):
        """Blocks laid out in another frame ({(x, y, z): state}, a plant building's), turned k quarters clockwise about
        (x0, z0) and moved there to (ox, oz)."""
        for (x, y, z), s in blocks.items():
            dx, dz = x - x0, z - z0
            for _ in range(k % 4):
                dx, dz = -dz, dx
            self.put(ox + dx, y, oz + dz, turn_state(s, k))


# ------------------------------------------------------------------ the rooms

def plant_restroom():
    """The plant's control building restroom (round 26, #14: 2 stalls, a urinal, 2 sinks; tools/medium_plant.restroom),
    laid out on a scratch building: its blocks in its own frame (x 25..30, z 20..29; the door wall z 21, the door at x
    27 walking south; the window wall z 29), the ceiling's exhaust grille brought down to the room's ceiling (y 5)."""
    b = MP.Building("tp_restroom", "restroom", 34, 31, 8)
    b.b(0, 0, 0, 33, 0, 30, MP.SMOOTH)
    MP.restroom(b, 0, 1, 30)
    out = {}
    for x in range(25, 31):
        for z in range(20, 30):
            for y in range(0, 9):
                m = int(b.g[x, y + b.base, z])
                if not m:
                    continue
                s = S(m)
                if s.startswith("minecraft:"):
                    continue
                if "exhaust_grille" in s:
                    y = TOP
                out[(x, y, z)] = s
    return out


def restrooms(R):
    """North-west, off the lobby: the plant's restroom (two stalls, one accessible; the urinal; two sinks) turned to face
    the lobby, its door at x -21; and north of it a single accessible washroom (toilet, sink, dryer, baby changer)."""
    # the restroom: x -29..-21, z -36..-31 with its walls; its door wall (the plant's z 21) on the lobby at x -21
    R.stamp(plant_restroom(), 1, -21, -36, 25, 21)
    cells = R.interior(-28, -35, -22, -32)
    walls = {}
    for z in range(-36, -30):
        walls[(-21, z)] = "wall"
        walls[(-29, z)] = "wall"
    for x in range(-29, -20):
        walls[(x, -36)] = "wall"
        walls[(x, -31)] = "wall"
    _shell_keep(R, "restroom", cells, walls, solid=True)
    for (x, z) in cells:
        if "floor_drain" not in (R.get(x, 0, z) or ""):
            R.put(x, 0, z, TILE_FLOOR)
    # the washroom north of it: the wedge x -28..-22, z -44..-38 (lined with tiles inside the curtain wall), its door at
    # x -21, z -40
    cells = R.interior(-28, -44, -22, -38)
    walls = {(-21, z): "wall" for z in range(-45, -36)}
    walls.update({(x, -37): "wall" for x in range(-30, -20)})
    R.door(-21, -40, "west", label="washroom")
    _shell_keep(R, "washroom", cells, walls, solid=True)
    floor(R, cells, TILE_FLOOR)
    cells = tile_walls(R, cells, line=True)
    R.rooms["washroom"] = cells
    # the accessible toilet in the north corner, its back to the lining (its grab bars are its own); the sink, mirror,
    # towels and dryer on the east wall
    R.put(-23, Y0, -43, "jugcraft:restroom_toilet[facing=north,flushing=false,paper=8,variant=1]")
    R.put(-22, Y0, -43, "jugcraft:restroom_trash_can[facing=east,fill=1]")
    R.f("restroom_towel_dispenser", -22, Y0 + 1, -43, "east")
    R.put(-22, Y0, -42, "jugcraft:restroom_sink[facing=east,running=false]")
    R.f("restroom_mirror", -22, Y0 + 1, -42, "east")
    R.put(-22, Y0 + 1, -41, "jugcraft:restroom_hand_dryer[facing=east,running=false]")
    R.f("sign_wash_hands", -24, Y0 + 1, -38, "south")
    R.put(-24, 0, -40, furn("restroom_floor_drain", "north"))
    R.put(-24, TOP, -40, furn("restroom_exhaust_grille", "north"))


def floor(R, cells, state):
    for (x, z) in cells:
        if "floor_drain" not in (R.get(x, 0, z) or ""):
            R.put(x, 0, z, state)


def tile_walls(R, cells, line=False):
    """The room's walls tiled to y 2 on their faces into the room (the plant restroom's tiles: the lower course with
    its cove base, the upper with the accent band and the cap); line: where the room meets the curtain wall, a lining
    of tiles inside it instead (the room loses those cells)."""
    cells = set(cells)
    if line:
        lining = {c for c in cells if any((c[0] + dx, c[1] + dz) in R.edge for dx in (-1, 0, 1) for dz in (-1, 0, 1))}
        for (x, z) in lining:
            for y in range(Y0, Y1 + 1):
                if R.free(x, y, z) or R.get(x, y, z) in (TILE_FLOOR,):
                    R.put(x, y, z, WALL)
        cells -= lining
    for (x, z) in cells:
        for f in FACINGS:
            dx, dz = dirs(f)
            w = (x + dx, z + dz)
            if w in cells:
                continue
            for y, piece in ((Y0, "restroom_wall_tile"), (Y0 + 1, "restroom_wall_tile_cap")):
                if R.get(w[0], y, w[1]) in (WALL, SKIN):
                    R.put(w[0], y, w[1], f"jugcraft:{piece}[back=painted,facing={turn(f, 2)}]")
    return cells


def _shell_keep(R, name, cells, walls, solid=False):
    """R.shell, keeping what is already in a wall cell (a door, the module's tiles) and filling the rest (over a door:
    the wall, or glass over a glass door, and the band); solid: the curtain wall's glass beside the room made the dark
    skin (no windows into it)."""
    R.rooms[name] = set(cells)
    for (x, z), kind in walls.items():
        if (x, z) in R.edge or (x, z) not in R.hex:
            continue            # (the curtain wall is the room's wall there; never a wall outside the building)
        for y in range(Y0, Y1 + 1):
            if R.free(x, y, z):
                R.put(x, y, z, WALL if kind == "wall" else R.glass(kind))
        R.put(x, TOP, z, BAND)
        R.bulkhead(x, z)
    for (x, z) in cells:
        if R.free(x, TOP, z):
            R.put(x, TOP, z, CEIL)
        R.b.pop((x, R.lamp_y, z), None)
    for (x, z) in cells if solid else ():
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
            e = (x + dx, z + dz)
            if e in R.edge:
                for y in range(Y0, Y1 + 1):
                    s = R.get(e[0], y, e[1])
                    if s is not None and s.startswith("fglass:"):
                        R.put(e[0], y, e[1], SKIN)
    lamps = [c for c in sorted(cells) if R.free(c[0], Y1, c[1]) and c[0] % 4 == 0 and c[1] % 4 == 0]
    for (x, z) in lamps:
        R.put(x, Y1, z, LAMP)


# The security office's offset from where fix 9 had it (x 28..37, z -5..4): over into the hall's south-east corner by
# the curtain wall (cargo area rework take 2, Justin 27 October 2026: "move it over more. It needs to be closer to the
# main wall of the structure", giving dispatch the room for the cargo hall).
SEC_DX, SEC_DZ = 2, 15


def security(R):
    """The hall's south-east corner, against the intake room's south end and by the curtain wall (moved here on 27
    October 2026 for the cargo hall; fix 9 the same day had it east of pillar B, before that west of pillar C in front
    of the data hall's glass, "it looks really ugly"): the security office, glass onto the hall on its west with its
    doors, its other walls solid. The secondary alarm station's console (live: the zones round the tower,
    the access log, who is inside) under the tower's live screen on the north wall with the security desks beside it
    (live), the weapons locker in the corner; along the south wall the files, the officers' lockers, the kitchenette under the clock and
    the radios, the fridge in the corner; the AED, the extinguisher, a plant. (The slot along its south side, z 20, is
    closed since layout 49: close_gaps; the battery room's doors are in that room's west wall.)"""
    dx, dz = SEC_DX, SEC_DZ
    cells = R.interior(29 + dx, -4 + dz, 36 + dx, 3 + dz)
    walls = {}
    for x in range(28 + dx, 38 + dx):
        walls[(x, -5 + dz)] = "wall"
        walls[(x, 4 + dz)] = "wall"
    for z in range(-4 + dz, 4 + dz):
        walls[(28 + dx, z)] = "east"        # framed glass onto the hall (the room is east of it)
        walls[(37 + dx, z)] = "wall"        # its east wall, solid
    # the doors first (a pair, walking in east)
    R.door(28 + dx, 0 + dz, "east", kind="glass", double=True, label="security")
    _shell_keep(R, "security", cells, walls)
    floor(R, cells, CARPET)
    # the north wall: the weapons locker in the corner; the alarm station console, its chair and the live screen over
    # it; the security desks (the hall's console desks working SECURITY: the gates and doors, who is in, the alarms;
    # live), in place of the plant's CCTV desks with their little painted monitors (Justin, 27 October: ugly)
    import tripillar_hall as HALL
    for row in range(2):
        R.put(29 + dx, Y0 + row, -4 + dz, S(MP.multi_state("sec_weapons_locker", "north", row)))
    R.f("aed_cabinet", 30 + dx, Y0 + 1, -4 + dz, "north")
    R.panel("sec_alarm_console", 31 + dx, -4 + dz, "north", 3)
    R.f("operator_chair", 32 + dx, Y0, -3 + dz, "north")
    # (the screen a block up, its lower edge clear of the console's monitors: Justin, 5 November 2026, "merging with the
    # monitors")
    R.control_screen(32 + dx, -4 + dz, "south", lift=1)
    for k in range(3):
        R.put(34 + dx + k, Y0, -4 + dz, HALL.desk_state("north", k, SECURITY_ROLE))
    R.f("operator_chair", 35 + dx, Y0, -3 + dz, "north")
    R.plant(36 + dx, -1 + dz)               # (against its solid east wall)
    # (the report desk for two in the middle and the supervisor's desk by the doors went on 5 November 2026: Justin, "i
    # dont like single block desks, get rid of them"; the console and the desk row on the north wall are the desks)
    # the south wall: the files (the extinguisher over them), the lockers, the kitchenette under the clock and the radio
    # rack, the fridge in the corner
    R.f("filing_cabinet", 29 + dx, Y0, 3 + dz, "south")
    R.f("fire_extinguisher", 29 + dx, Y0 + 1, 3 + dz, "south")
    for x, n in ((30, "locker"), (31, "locker_security"), (32, "locker")):
        R.stack(n, x + dx, 3 + dz, "south")
    R.row(["kitchen_counter", "coffee_counter", "kitchen_sink"], 35 + dx, 3 + dz, "south")
    R.panel("ops_wall_clock", 34 + dx, 3 + dz, "south", 1, y=Y0 + 2)
    R.panel("ops_radio_rack", 35 + dx, 3 + dz, "south", 1, y=Y0 + 1)
    R.stack("fridge", 36 + dx, 3 + dz, "south")
    R.f("smoke_detector", 34 + dx, Y1, -2 + dz, "north")


SECURITY_ROLE = 13          # tower/ConsoleDeskBlock.ROLES: "SECURITY"
CAFE = (-23, 31, 23, 44)


def flat_tv(facing, c, r, w, h):
    """A block of a w x h Flat Screen TV hung on a wall (tv/FlatTvBlock), its back to {facing}: column c from its
    viewer's left, row r from the bottom (each block says which ways the screen carries on). The only TV the buildings
    have (Justin, 10 November 2026: "I don't want to use that shitty TV at all, anywhere. It should be actual TVs"):
    anyone's videos and streams play on it."""
    b = lambda v: "true" if v else "false"
    return (f"jugcraft:flat_tv[down={b(r > 0)},facing={facing},left={b(c > 0)},right={b(c < w - 1)},stand=false,"
            f"up={b(r < h - 1)}]")


def cafe(R):
    """South: the crew cafe, its windows the building's own on the south, framed glass onto the hall with two pairs of
    glass doors. The kitchen down the west wall (the upstairs kitchen kit: the Kitchen Run and the Fridge Freezer), the
    water cooler; tables for two in rows across the room (lunch on some), plants; the lounge corner on the east wall:
    the sofa facing a Flat Screen TV over a media console (since layout 51: it was the Lounge TV, its TOWER TV channel
    only), the long glass coffee table between."""
    x0, z0, x1, z1 = CAFE
    cells = R.interior(x0, z0, x1, z1)
    walls = {(x, z0 - 1): "south" for x in range(x0 - 1, x1 + 2)}
    for z in range(21, z1 + 1):
        walls[(x0 - 1, z)] = "wall"
        walls[(x1 + 1, z)] = "wall"
    for x in (x0 - 1, x1 + 1):
        walls[(x, z0 - 1)] = "wall"
    for dx in (-13, 12):
        R.door(dx, z0 - 1, "south", kind="glass", double=True, label="cafeteria")
    _shell_keep(R, "cafe", cells, walls)
    floor(R, cells, VINYL)
    # the kitchen down the west wall: the upper lounge's kitchen kit (tools/lounge_kit.py; Justin's walk through, note
    # 18: the downstairs kitchen made of the upstairs kitchen's cabinets and appliances): the Kitchen Run (10 x 1, two
    # high) as it stands upstairs (facing west puts it on a west wall), the Fridge Freezer at its south end by the window
    for p in range(10):
        for k, half in enumerate(("lower", "upper")):
            assert R.free(x0, Y0 + k, 43 - p), (x0, Y0 + k, 43 - p)
            R.put(x0, Y0 + k, 43 - p, f"jugcraft:kitchen_run[facing=west,half={half},part={p}]")
    for k, half in enumerate(("lower", "upper")):
        R.put(x0, Y0 + k, 44, f"jugcraft:fridge_freezer[facing=east,half={half}]")
    R.f("water_cooler", x0, Y0, 32, "west")
    R.plant(x0, 31)
    # tables for two, chairs either side
    for x in (-15, -10, -5, 0, 5, 10):
        for z in (34, 37, 40, 43):
            if (x, z) in cells:
                R.varied("cafe_table", x, Y0, z, "north", 6)
                R.f("cafe_chair", x - 1, Y0, z, "east")
                R.f("cafe_chair", x + 1, Y0, z, "west")
    # the lounge corner on the east wall: the TV, the three-seat sofa facing it, the long glass coffee table between
    # (Justin's walk through, note 16: the small table became a long glass one and the control screen a flat screen TV;
    # a block left between the sofa and the table to walk through). The TV: a Flat Screen TV three wide and two high on
    # the wall (its back east) over the media console, as upstairs, centred on the sofa (Justin, 10 November 2026:
    # "change the TV that's down here to be one of the actual flat screens that work"; layout 51)
    for c, z in enumerate((36, 37, 38)):               # (its viewer faces east: their left is north)
        for r in range(2):
            assert R.free(x1, Y0 + 1 + r, z), (x1, Y0 + 1 + r, z)
            R.put(x1, Y0 + 1 + r, z, flat_tv("east", c, r, 3, 2))
    for p in range(3):
        assert R.free(x1, Y0, 38 - p), (x1, Y0, 38 - p)
        R.put(x1, Y0, 38 - p, f"jugcraft:media_console[facing=west,part={p}]")
    R.row(("sofa", 3), x1 - 4, 36, "east")
    rx, rz = right_of("east")
    for c in range(3):
        assert R.free(x1 - 2 + rx * (c - 1), Y0, 37 + rz * (c - 1))
        R.put(x1 - 2 + rx * (c - 1), Y0, 37 + rz * (c - 1), S(MP.multi_state("long_glass_table", "east", c)))
    for p in ((x1, 32), (x1, 42), (x1 - 4, 32), (-20, 44), (20, 44)):
        if p in cells and R.free(p[0], Y0, p[1]):
            R.plant(*p)
    R.f("fire_extinguisher", x0, Y0 + 1, 33, "west")


def lockers(R):
    """South-west: the locker room. Full-height crew lockers along the north and east walls (some standing open: the
    crew's jacket, headset and kit bag), benches down the middle; three shower stalls in the south end against the east
    wall (tiled floor, drains). (No coat rack: Justin, redesign 4: "get rid of the thing that has the orange vest and
    scientist coat beside that sink".)"""
    cells = R.interior(-40, 22, -25, 44)
    walls = {(x, 21): "wall" for x in range(-40, -23)}
    walls.update({(-24, z): "wall" for z in range(21, 45)})
    R.door(-31, 21, "south", double=True, label="locker_room")
    _shell_keep(R, "lockers", cells, walls)
    floor(R, cells, VINYL)
    for x in range(-37, -25):
        if x in (-32, -31, -30, -29):
            continue
        if (x, 22) in cells:
            R.stack("crew_locker_open" if x % 4 == 1 else "locker", x, 22, "north")
    for z in range(23, 33):
        R.stack("crew_locker_open" if z % 5 == 2 else "locker", -25, z, "east")
    R.row(("locker_bench", 3), -34, 26, "north")
    R.row(("locker_bench", 3), -30, 26, "north")
    R.row(("locker_bench", 3), -32, 29, "north")
    # the showers (new piece: shower_stall) in a bank of three on the east wall, the user facing east; the sinks and
    # mirrors south of them, the dryer; tiled floor from the showers south
    for z in (34, 35, 36):
        R.put(-25, Y0, z, "jugcraft:shower_stall[facing=east]")
    for z in range(33, 45):
        for x in range(-38, -24):
            if (x, z) in cells:
                R.put(x, 0, z, TILE_FLOOR)
    # (each stall's tray has its drain: no floor drain out in front of them, Justin asked for it gone)
    tile_walls(R, {(x, z) for (x, z) in cells if z >= 33 and x >= -27})
    R.row(("locker_bench", 3), -28, 34, "west")
    for z in (38, 39):
        R.put(-25, Y0, z, "jugcraft:restroom_sink[facing=east,running=false]")
        R.f("restroom_mirror", -25, Y0 + 1, z, "east")
    R.put(-25, Y0 + 1, 40, "jugcraft:restroom_hand_dryer[facing=east,running=false]")
    R.put(-25, Y0, 41, "jugcraft:restroom_trash_can[facing=east,fill=1]")
    R.f("restroom_towel_dispenser", -25, Y0 + 1, 41, "east")
    R.f("water_cooler", -26, Y0, 23, "east") if R.free(-26, Y0, 23) else None
    R.f("fire_extinguisher", -28, Y0 + 1, 22, "north") if R.free(-28, Y0 + 1, 22) else None
    R.plant(-34, 23) if R.free(-34, Y0, 23) else None
    R.put(-30, TOP, 36, furn("restroom_exhaust_grille", "north"))


def battery(R):
    """South-east: the battery and UPS room. The UPS lineup along the north wall (batteries, the module, the bypass,
    the PDUs); two strings of station batteries along the west wall and the south-east wall's foot; the hydrogen
    monitor and the exhaust fan, the eyewash and its sign, the spill kit, the acid and NO SMOKING signs, the PPE
    cabinet, the cell log, the sump; steel doors with the room's plaque. The doors are in its west wall, on the hall
    (Justin's first-floor look-over, 10 November 2026: they were in its north wall at x 36..37, reached only along a
    slot one block wide between this room and the security office, which close_gaps fills)."""
    cells = R.interior(25, 22, 40, 44)
    walls = {(x, 21): "wall" for x in range(24, 41)}
    walls.update({(24, z): "wall" for z in range(21, 45)})
    R.door(24, 24, "east", double=True, label="battery_room")
    _shell_keep(R, "battery", cells, walls)
    # (the walker's left facing east is north: the pair is z 23..24, where the room is clear inside)
    for x, name in zip(range(25, 32), ["ups_battery", "ups_battery", "ups_battery", "ups_module", "ups_bypass", "pdu_cabinet", "pdu_cabinet"]):
        R.panel(name, x, 22, "north", 1)
    # string 1 along the west wall (the user faces west; each rack four long, to the user's right: north), string 2 back
    # to back down the middle facing the aisle between them
    R.panel("station_battery", 25, 32, "west", 4, "1A STR 1 CELLS 1-30")
    R.panel("station_battery", 25, 38, "west", 4, "1A STR 1 CELLS 31-60")
    R.panel("station_battery", 29, 29, "east", 4, "1A STR 2 CELLS 1-30")
    R.panel("station_battery", 29, 34 - 0, "east", 4, "1A STR 2 CELLS 31-60") if all(
        (29, z) in cells for z in range(34, 38)) else None
    R.panel("battery_disconnect", 25, 27, "west", 1, "1A-1")
    R.panel("battery_disconnect", 25, 26, "west", 1, "1A-2")
    # by the door on the north wall: the eyewash and its sign, the sump in front of it, the spill kit, the PPE cabinet and
    # the cell log over it; the hydrogen monitor and the exhaust fan over the UPS
    R.f("eyewash_station", 32, Y0, 22, "north")
    R.f("sign_eyewash", 32, Y0 + 1, 22, "north")
    R.f("spill_kit", 33, Y0, 22, "north")
    R.f("battery_ppe_cabinet", 34, Y0 + 1, 22, "north")
    R.f("battery_log_board", 33, Y0 + 1, 22, "north")
    R.f("battery_room_sump", 34, Y0, 22, "north")
    R.panel("h2_monitor", 28, 22, "north", 1, "1A", y=Y0 + 2)
    R.panel("exhaust_fan", 26, 22, "north", 1, "1A", y=Y0 + 2)
    R.f("sign_battery_acid", 30, Y0 + 2, 22, "north")
    R.f("sign_no_smoking", 23, Y0 + 1, 21, "east")      # (on the hall side: you look east at it; clear of the doors' leaves)
    R.f("smoke_detector", 30, Y1, 30, "north") if R.free(30, Y1, 30) else None
    R.f("fire_extinguisher", 38, Y0 + 1, 22, "north")


# ------------------------------------------------------------------ the first-floor look-over (layout 49)
# Justin, 10 November 2026: "Currently there are gaps between rooms and some of the walls don't really look that great
# with the theme of the building. I am not sure how to make everything flow together and look less messy." From the
# look-over's sheets he picked finish A (concrete, like the pillars, with the pillars' light line carried round the rooms
# at the pillars' height), both gaps closed, and the three extras: the line on the stair's wall, the nook behind the goods
# lift closed, benches and plants along the long blank walls.
CONC = "jugcraft:reinforced_concrete"
FACE = "concrete"               # the rooms' walls onto the hall (tools/faced_walls.py)
LINE_Y = 3                      # the pillars' light line (tower_tripillar.core_feet)
# the slot between the security office (its south wall z 19) and the battery room (its north wall z 21): one block wide,
# ten long, the only way to the battery room's doors while they were in that wall
ALLEY = [(x, 20) for x in range(30, 40)]
# the hall's cells beside the goods lift's glass house, between it and the curtain wall (a slot two wide, five deep):
# its mouth, on the line of the house's north wall, each cell with the cell whose column it carries on: the house's
# glass, then a steel post like the curtain wall's mullion beside it (glass there would join the curtain wall's own pane
# in front of it and take that pane's frame away)
LIFT_NOOK = {(41, -15): (39, -15), (42, -15): (43, -15)}
# the stair up from the hall (tower_tripillar.stairs: two wide, along x at z -30..-29, its foot at x -31)
STAIR = [(x, z) for x in range(-31, -20) for z in (-30, -29)]
CLOSED = []                     # the cells close_gaps filled


def nook(R):
    """The dead-end wedge beside the restrooms: the hall's cells between their west wall (x -29) and the curtain wall,
    north of the stair's foot (z -30..-29, which stays clear)."""
    return sorted((x, z) for x in range(-36, -29) for z in range(-36, -30)
                  if (x, z) in R.hex and (x, z) not in R.edge and R.free(x, Y0, z) and R.free(x, Y0 + 1, z))


def close_gaps(R):
    """The slot and the wedge filled as the rooms' walls stand: solid to the band, the band, the skin on up to the hall's
    ceiling (the security office and the battery room one block; the restrooms flush with the stair). Concrete: what
    shows of them is on the hall."""
    cells = ALLEY + nook(R)
    CLOSED[:] = cells
    for (x, z) in cells:
        for y in range(Y0, Y1 + 1):
            assert R.free(x, y, z), ("a gap to close isn't empty", x, y, z, R.get(x, y, z))
            R.put(x, y, z, CONC)
        R.put(x, TOP, z, BAND)
        R.bulkhead(x, z)
    return cells


def rooms_shut(air, rooms, y):
    """The hall's air must not reach into a room (a wall, glass or a door stands in every way in): else the facing rule
    would take the room's own side of its walls for the hall's. rooms: {name: interior cells}."""
    for name, cells in rooms.items():
        inside = sorted(c for c in cells if (c[0], y, c[1]) in air)
        assert not inside, (f"the hall's air reaches into the {name}", inside[:6])


def hall_side(b, put, glass, hex_cells, edge, lamp_y, top, rooms):
    """What the hall sees of the ground floor's rooms, once everything on the floor stands (the last thing the control
    building does): every room wall that looks onto the hall faced in concrete (tools/faced_walls.py: the room behind
    keeps its paint and its tiles), the pillars' light line along them and along the stair's side and the closed gaps
    where they are high enough, at the pillars' height; the nook behind the goods lift
    closed with the glass of its house; benches and plants along the long blank walls. Returns what it did."""
    import faced_walls as FW
    R = Rooms(b, put, glass, hex_cells, edge, lamp_y)
    inner = set(hex_cells) - set(edge)
    # the goods lift's nook: its house's north wall carried on to the curtain wall
    for (x, z), (lx, lz) in LIFT_NOOK.items():
        for y in range(Y0, top + 1):
            like = b.get((lx, y, lz))
            assert like is not None and (x, y, z) not in b, ("the goods lift's nook", x, y, z, like, b.get((x, y, z)))
            b[(x, y, z)] = like
    air = FW.hall_air(b, [(0, Y0, -38)], inner, Y0, top)
    rooms_shut(air, rooms, Y0)
    did = FW.face(b, air, FACE, Y0, Y1, LINE_Y, edge)
    # (only these: the pillars' own faces onto the rotunda carry the video walls, not a line)
    did["concrete lines"] = FW.line(b, air, STAIR + CLOSED, LINE_Y, FACE)
    # benches (the lounge's sofas, their backs to the wall) and plants along the long blank walls, clear of every door's
    # leaves: the locker room's north wall either side of its doors and its east wall; the battery room's north wall and
    # its west wall south of its doors
    R.plant(-33, 20)
    R.row(("sofa", 3), -36, 20, "north")
    R.plant(-28, 20)
    R.row(("sofa", 3), -27, 20, "north")
    R.plant(-23, 22)
    R.row(("sofa", 4), -23, 23, "east")
    R.plant(-23, 27)
    R.plant(24, 20)
    R.row(("sofa", 4), 25, 20, "north")
    R.plant(29, 20)
    R.plant(23, 26)
    R.row(("sofa", 3), 23, 29, "west")
    return did


def ground_floor(b, put, glass, hex_cells, edge, lamp_y):
    """Part A: all five rooms, and the gaps between them closed. Returns the Rooms (their interiors, for the checks)."""
    R = Rooms(b, put, glass, hex_cells, edge, lamp_y)
    restrooms(R)
    security(R)
    cafe(R)
    lockers(R)
    battery(R)
    close_gaps(R)
    R.plenum_lights()
    return R
