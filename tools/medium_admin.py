"""The Medium Plant's administration building and security gatehouse, fitted out room by room.

Both stand level with the ground (walk straight in at the doors) with paths up to every entrance. The admin
building's reception is a real front lobby: a reception desk, a waiting area with sofas and plants, and one staff
door into the offices behind it. Offices are wide rather than long and each room has a job (private offices, shared
offices, an open office, labs, document control, training, break rooms, a board room, the locker room), furnished
for it. Whiteboards, framed prints and plants come in variants so no two rooms look alike.

Room furnishing works in the room's own frame: u across (0 at the west wall), v from the window wall (0) to the
corridor wall where the door is; "win" and "door" are the facings toward those walls.
"""
import nuclear_plants as P
import medium_plant as MP
from medium_plant import (Building, entrance, furniture, sign, OPPOSITE, GLAZING, VINYL, CARPET, EPOXY, CEIL, WHITE, PANE,
                          PAINTED, SMOOTH, BRICK, ANDESITE)



def tv_state(facing, c, r, w, h):
    """A block of a w x h flat screen TV at column c (to its viewer's right) and row r, hung on a wall (tv/FlatTvBlock.java:
    the blocks join into one screen, each saying which ways the screen carries on)."""
    b = lambda v: "true" if v else "false"
    return MP.M(f"jugcraft:flat_tv[down={b(r > 0)},facing={facing},left={b(c > 0)},right={b(c < w - 1)},stand=false,up={b(r < h - 1)}]", (60, 60, 64))

class Room:
    def __init__(self, b, x0, z0, x1, z1, y, win, door_u):
        self.b, self.x0, self.z0, self.x1, self.z1, self.y, self.win = b, x0, z0, x1, z1, y, win
        self.w = x1 - x0 + 1
        self.d = z1 - z0 + 1
        self.keep = set()
        # Keep the way in from the door clear: three wide, two deep.
        for du in (-1, 0, 1):
            for dv in (1, 2):
                self.keep.add((door_u + du, self.d - dv))
        self.door_u = door_u

    def xz(self, u, v):
        return self.x0 + u, (self.z0 + v if self.win == "n" else self.z1 - v)

    def uv(self, x, z):
        return x - self.x0, (z - self.z0 if self.win == "n" else self.z1 - z)

    def face(self, f):
        if f == "win":
            return "north" if self.win == "n" else "south"
        if f == "door":
            return "south" if self.win == "n" else "north"
        return f

    def inside(self, u, v):
        return 0 <= u < self.w and 0 <= v < self.d

    def free(self, u, v, y=None):
        """Nothing there yet and not kept clear."""
        y = self.y if y is None else y
        if not self.inside(u, v) or (u, v) in self.keep:
            return False
        x, z = self.xz(u, v)
        return self.b.get(x, y, z) == 0 and self.b.get(x, y - 1, z) != 0

    def open(self, u, v):
        """Floor you can walk on (it may be kept clear for someone)."""
        if not self.inside(u, v):
            return False
        x, z = self.xz(u, v)
        return self.b.get(x, self.y, z) == 0 and self.b.get(x, self.y + 1, z) == 0

    def put(self, name, u, v, f):
        if self.free(u, v):
            x, z = self.xz(u, v)
            self.b.furniture(name, x, self.y, z, self.face(f))
            return True
        return False

    def rack(self, name, u, v, f):
        """A two-high live panel (a rack: tools/server_room.py) at (u, v)."""
        if self.free(u, v) and self.free(u, v, self.y + 1) is not None:
            x, z = self.xz(u, v)
            self.b.panel(name, x, self.y, z, self.face(f), 1)
            return True
        return False

    def stack(self, name, u, v, f, n=2):
        if self.free(u, v):
            x, z = self.xz(u, v)
            self.b.stack(name, x, self.y, z, self.face(f), n)
            return True
        return False

    def _seat_ok(self, cu, cv, fw):
        """A chair at (cu, cv) for someone facing fw: the chair's cell free and room behind it to get in and out,
        which is then kept clear."""
        bx, bz = P.DIRS[OPPOSITE[fw]]
        x, z = self.xz(cu, cv)
        bu, bv = self.uv(x + bx, z + bz)
        return self.free(cu, cv) and self.open(bu, bv), (bu, bv)

    def desk(self, u, v, f, name="office_desk"):
        """A one-block desk with its chair, and room behind the chair."""
        fw = self.face(f)
        dx, dz = P.DIRS[OPPOSITE[fw]]
        x, z = self.xz(u, v)
        cu, cv = self.uv(x + dx, z + dz)
        ok, behind = self._seat_ok(cu, cv, fw)
        if self.free(u, v) and ok:
            self.b.desk(x, self.y, z, fw, name)
            self.keep.add(behind)
            return True
        return False

    def long_desk(self, cells, f, tops, seats, name="office_desk_long", chair="operator_chair"):
        """A desk of several joined blocks at cells (along u or v), the user facing f; tops says what stands on
        each block; a chair (with room behind it) at the blocks listed in seats."""
        fw = self.face(f)
        if not all(self.free(u, v) for u, v in cells):
            return False
        dx, dz = P.DIRS[OPPOSITE[fw]]
        chairs = []
        for i in seats:
            x, z = self.xz(*cells[i])
            cu, cv = self.uv(x + dx, z + dz)
            ok, behind = self._seat_ok(cu, cv, fw)
            if not ok:
                return False
            chairs.append(((x + dx, z + dz), behind))
        xs = [self.xz(*c) for c in cells]
        rx, rz = P.DIRS[P.CLOCKWISE[fw]]
        order = sorted(range(len(cells)), key=lambda i: xs[i][0] * rx + xs[i][1] * rz)
        x, z = xs[order[0]]
        self.b.row([name] * len(cells), x, self.y, z, fw, tops=[tops[i] for i in order])
        for (cx, cz), behind in chairs:
            self.b.p(cx, self.y, cz, furniture(chair, fw))
            self.keep.add(behind)
            self.keep.add(self.uv(cx, cz))
        return True

    def chair(self, u, v, f, name="operator_chair"):
        if self.free(u, v):
            x, z = self.xz(u, v)
            self.b.p(x, self.y, z, furniture(name, self.face(f)))

    def on_wall(self, name, u, v, f, n=None, dy=1):
        """Something hung on the wall beside cell (u, v) on its f side, at eye height (a variant piece if n)."""
        y = self.y + dy
        fw = self.face(f)
        x, z = self.xz(u, v)
        dx, dz = P.DIRS[fw]
        behind = self.b.get(x + dx, y, z + dz)
        if not self.inside(u, v) or self.b.get(x, y, z) != 0 or not behind or behind in GLAZING or behind in MP.DOORISH:
            return False
        if dy == 0 and (u, v) in self.keep:
            return False
        if n:
            self.b.varied(name, x, y, z, fw, n)
        else:
            self.b.furniture(name, x, y, z, fw)
        return True

    def screen(self, name, u, v, f):
        """A wall screen three blocks across and two high, its bottom middle beside cell (u, v), on the wall on its f
        side (the bottom at eye height), if the wall behind is solid all the way."""
        fw = self.face(f)
        x, z = self.xz(u, v)
        rx, rz = P.DIRS[P.CLOCKWISE[fw]]
        dx, dz = P.DIRS[fw]
        cells = []
        for r in range(2):
            for c in range(3):
                cx, cz, cy = x + rx * (c - 1), z + rz * (c - 1), self.y + 1 + r
                behind = self.b.get(cx + dx, cy, cz + dz)
                if self.b.get(cx, cy, cz) != 0 or not behind or behind in GLAZING or behind in MP.DOORISH \
                        or not self.inside(*self.uv(cx, cz)):
                    return False
                cells.append((cx, cy, cz, r * 3 + c))
        for cx, cy, cz, part in cells:
            self.b.p(cx, cy, cz, tv_state(fw, part % 3, part // 3, 3, 2) if name == "flat_tv" else MP.multi_state(name, fw, part))
        MP.screen_raceway(self.b, name, x, self.y, z, fw)
        return True

    def copier(self, u, v, f):
        """The office copier at (u, v) and the cell to its left as you face it (not on a cell kept clear)."""
        fw = self.face(f)
        x, z = self.xz(u, v)
        rx, rz = P.DIRS[P.CLOCKWISE[fw]]
        lu, lv = self.uv(x - rx, z - rz)
        if not (self.free(u, v) and self.free(lu, lv)):
            return False
        # Room in front of it to stand and use it, kept clear afterwards.
        fx, fz = P.DIRS[OPPOSITE[fw]]
        fronts = [self.uv(x + fx, z + fz), self.uv(x - rx + fx, z - rz + fz)]
        if not all(self.open(a, c) and not (self.inside(a, c) and self.b.get(*self.xz(a, c)[:1], self.y, self.xz(a, c)[1]))
                   for a, c in fronts):
            return False
        if copier(self.b, x, self.y, z, fw):
            self.keep.update(fronts)
            return True
        return False

    def big_print(self, u, v, f):
        """A big framed picture two blocks across on the wall on the f side of cells (u, v) and the one to its
        left as you face it (a random one of the twelve, chosen from where it hangs)."""
        fw = self.face(f)
        x, z = self.xz(u, v)
        return big_print(self.b, x, self.y + 1, z, fw)

    def lounge(self, u, v):
        """The oval glass coffee table, three blocks across u (from u) and two deep (v and v + 1), if free."""
        cells = [(u + c, v + r) for c in range(3) for r in range(2)]
        if not all(self.free(a, b) for a, b in cells):
            return False
        xs = [self.xz(a, b) for a, b in cells]
        x0, z0 = min(p[0] for p in xs), min(p[1] for p in xs)
        # Laid facing north in the world: columns run east, rows south.
        for x, z in xs:
            self.b.p(x, self.y, z, MP.multi_state("lounge_table", "north", (z - z0) * 3 + (x - x0)))
        return True

    def oval(self, u, v, chairs=True):
        """The oval meeting table (five blocks long across u, three deep) with its middle at (u, v), chairs all round
        it and room to walk round the chairs."""
        ring = [(u + a, v + c) for a in range(-4, 5) for c in range(-3, 4)]
        if not all(self.free(a, c) or (abs(a - u) == 4 or abs(c - v) == 3) and self.open(a, c) for a, c in ring):
            return False
        # The table is laid facing north in the world: columns run east, rows south.
        x, z = self.xz(u, v)
        layout = (x * 5 + z * 3 + self.y) % 3          # one of three ways the table is set out for a meeting
        for p in range(15):
            cc, rr = p % 5, p // 5
            self.b.p(x + cc - 2, self.y, z + rr - 1, MP.multi_state("oval_table", "north", p, layout))
        x, z = self.xz(u, v)
        if chairs:
            for k in range(-2, 3):
                self.b.p(x + k, self.y, z - 2, furniture("operator_chair", "south"))
                self.b.p(x + k, self.y, z + 2, furniture("operator_chair", "north"))
            self.b.p(x - 3, self.y, z, furniture("operator_chair", "east"))
            self.b.p(x + 3, self.y, z, furniture("operator_chair", "west"))
        for a, c in ring:
            if abs(a - u) == 4 or abs(c - v) == 3:
                self.keep.add((a, c))
        return True

    def plant(self, u, v):
        if self.free(u, v):
            x, z = self.xz(u, v)
            self.b.plant(x, self.y, z)

    def row_u(self, names, u0, v, f, tops=None):
        """A row of joining pieces along u from u0 (eastward), facing f (win or door)."""
        if isinstance(names, tuple):
            names = [names[0]] * names[1]
        cells = [(u0 + i, v) for i in range(len(names))]
        if not all(self.free(u, w) for u, w in cells):
            return False
        fw = self.face(f)
        rx, _ = P.DIRS[P.CLOCKWISE[fw]]
        start = cells[0] if rx > 0 else cells[-1]
        x, z = self.xz(*start)
        self.b.row(names if rx > 0 else names[::-1], x, self.y, z, fw, tops=None if tops is None else (tops if rx > 0 else tops[::-1]))
        return True

    def row_v(self, names, u, v0, f):
        """A row of joining pieces along a side wall (f west at u = 0, east at u = w - 1), from v0 toward the door."""
        if isinstance(names, tuple):
            names = [names[0]] * names[1]
        cells = [(u, v0 + i) for i in range(len(names))]
        if not all(self.free(uu, w) for uu, w in cells):
            return False
        fw = self.face(f)
        xs = [self.xz(*c) for c in cells]
        rx, rz = P.DIRS[P.CLOCKWISE[fw]]
        order = sorted(range(len(cells)), key=lambda i: xs[i][0] * rx + xs[i][1] * rz)
        x, z = xs[order[0]]
        self.b.row([names[i] for i in order], x, self.y, z, fw)
        return True

    def table(self, u, v):
        """The round table with its middle at (u, v) and chairs round it, if the 5 x 5 is free."""
        if all(self.free(u + a, v + c) for a in range(-2, 3) for c in range(-2, 3)):
            x, z = self.xz(u, v)
            self.b.round_table(x, self.y, z)
            return True
        return False


def copier(b, x, y, z, fw):
    """The big office copier, two blocks across (the block at x, z and the one to its left as you face it) and a
    little into the row above; only where all of that is free floor."""
    rx, rz = P.DIRS[P.CLOCKWISE[fw]]
    cells = [(x - rx, z - rz, 0), (x, z, 1)]
    for cx, cz, _ in cells:
        for yy in (y, y + 1):
            if b.get(cx, yy, cz) != 0:
                return False
        if b.get(cx, y - 1, cz) == 0:
            return False
    for cx, cz, c in cells:
        for row in range(2):
            b.p(cx, y + row, cz, MP.M(f"jugcraft:office_copier[facing={fw},part={row * 2 + c}]", (44, 46, 50)))
    b.seats.append(("office_copier", x, y, z, fw))
    b.seats.append(("office_copier", x - rx, y, z - rz, fw))
    return True


def big_print(b, x, y, z, fw):
    """A big framed picture on the wall in front of cell (x, z) (facing fw = toward the wall) and the cell to its
    left; only on solid wall (no glass, no doors)."""
    rx, rz = P.DIRS[P.CLOCKWISE[fw]]
    dx, dz = P.DIRS[fw]
    cells = [(x - rx, z - rz, 0), (x, z, 1)]
    for cx, cz, _ in cells:
        behind = b.get(cx + dx, y, cz + dz)
        if b.get(cx, y, cz) != 0 or not behind or behind in GLAZING or behind in MP.DOORISH:
            return False
    v = (x * 7 + z * 11 + y * 3) % 12
    for cx, cz, part in cells:
        b.p(cx, y, cz, MP.M(f"jugcraft:large_print[facing={fw},part={part},variant={v}]", (120, 100, 80)))
    b.seats.append(("large_print", x, y, z, fw))
    return True


# --------------------------------------------------------------------------------------------------- the rooms
# Desk tops (office_desk_long): 0 computer, 1 paperwork, 2 binders and a mug, 3 lamp and phone, 4 laptop, 5 rolled
# drawings. Executive desk: 0 two screens, 1 blotter and nameplate, 2 lamp and phone. Glass desk: 0 computer, 1 a
# computer opened up for repair, 2 documents and a tablet, 3 tools and circuit boards. Reception: 0 computer, 1
# phone and visitors' book, 2 a small plant.

def private_office(r):
    """One person's office: a three-block desk facing the door with the window behind (room to get round the chair),
    two visitors' chairs, bookcases and a filing cabinet on the side walls, a whiteboard, prints, plants."""
    cu = r.w // 2
    r.long_desk([(cu - 1, 2), (cu, 2), (cu + 1, 2)], "door", [3, 0, 1], [1])
    r.chair(cu - 1, 4, "win", "cafe_chair")
    r.chair(cu + 1, 4, "win", "cafe_chair")
    for v in (1, 2, 3):
        r.stack("bookcase", 0, v, "west")
    r.put("filing_cabinet", r.w - 1, 1, "east")
    r.put("filing_cabinet", r.w - 1, 2, "east")
    r.plant(r.w - 1, 0)
    r.plant(0, r.d - 1)
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)
    r.on_wall("framed_print", 0, 5, "west", 16)
    r.on_wall("fire_extinguisher", r.w - 1, r.d - 1, "east")


def hr_office(r):
    """Human resources: an executive desk with its visitors' chairs, a little meeting corner (a table with chairs),
    bookcases, filing cabinets, prints and plants."""
    r.long_desk([(3, 2), (4, 2), (5, 2)], "door", [2, 0, 1], [1], name="executive_desk")
    r.chair(3, 4, "win", "cafe_chair")
    r.chair(5, 4, "win", "cafe_chair")
    for u in (0,):
        for v in (1, 2, 3, 4):
            r.stack("bookcase", u, v, "west")
    tu = r.w - 4
    r.put("cafe_table", tu, 2, "north")
    r.chair(tu - 1, 2, "east", "cafe_chair")
    r.chair(tu + 1, 2, "west", "cafe_chair")
    r.chair(tu, 3, "win", "cafe_chair")
    for u in range(r.w - 1, r.w - 3, -1):
        r.put("filing_cabinet", u, r.d - 1, "door")
    r.plant(r.w - 1, 0)
    r.plant(8, 0)
    r.on_wall("framed_print", r.w - 1, 2, "east", 16)
    r.on_wall("framed_print", 0, 6, "west", 16)
    r.on_wall("whiteboard", r.w - 1, 5, "east", 24)


def operations(r):
    """Operations: the plant status screen on the end wall with two rows of desks facing it, a whiteboard of the
    day's work, a copier."""
    cv = r.d // 2 - 1
    r.screen("status_screen", 0, cv, "west")
    for u in (3, 7):
        r.long_desk([(u, v) for v in range(cv - 1, cv + 3)], "west", [0, 1, 0, 2], [0, 2])
    r.on_wall("whiteboard", r.w - 1, 2, "east", 24)
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)
    r.copier(r.w - 1, r.d - 3, "east")
    r.plant(0, 0)
    r.plant(r.w - 1, 0)


def shared_office(r):
    """A shared office (maintenance planning): two-block desks along the window, cabinets and a copier at the back,
    whiteboards and a print."""
    for u in range(1, r.w - 2, 4):
        r.long_desk([(u, 0), (u + 1, 0)], "win", [0, (u // 4) % 4 + 1], [0])
    for u in range(0, r.w, 2):
        r.put("filing_cabinet", u, r.d - 1, "door")
    r.copier(r.w - 1, r.d - 4, "east")
    r.plant(0, r.d - 1)
    r.on_wall("whiteboard", 0, 3, "west", 24)
    r.on_wall("whiteboard", r.w - 1, 3, "east", 24)
    r.on_wall("framed_print", r.w - 1, 1, "east", 16)


def engineering(r):
    """Engineering: drafting tables by the window, a long desk of drawings and a computer, blueprints pinned up on
    the walls, plan chests (filing cabinets) and a copier."""
    for u in (1, 3, 5, 7):
        r.put("drafting_table", u, 0, "win")
    r.long_desk([(2, 4), (3, 4), (4, 4), (5, 4)], "door", [5, 0, 5, 1], [1, 3])
    for v in range(1, 6):
        r.on_wall("blueprint_sheet", 0, v, "west", 6)
        r.on_wall("blueprint_sheet", r.w - 1, v, "east", 6)
    r.put("filing_cabinet_open", r.w - 1, r.d - 2, "east")
    r.put("filing_cabinet", r.w - 1, r.d - 3, "east")
    r.copier(0, r.d - 2, "west")
    r.plant(r.w - 1, 0)


def open_office(r):
    """An open office: pods of desks face to face with room behind every chair, a copier, a water cooler."""
    u = 1
    while u + 2 < r.w - 1:
        cells = [(u + k, 2) for k in range(3)]
        r.long_desk(cells, "door", [0, 0, 0], [0, 1, 2])
        r.long_desk([(u + k, 3) for k in range(3)], "win", [0, 1, 0], [0, 1, 2])
        if r.d >= 10:
            r.long_desk([(u + k, 7) for k in range(3)], "door", [0, 2, 0], [0, 1, 2])
            r.long_desk([(u + k, 8) for k in range(3)], "win", [4, 0, 0], [0, 1, 2])
        r.plant(u + 3, 0)
        u += 5
    r.copier(0, r.d - 1, "west")
    r.put("water_cooler", r.w - 1, r.d - 1, "east")
    r.on_wall("whiteboard", 0, 4, "west", 24)
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)


def meeting_room(r):
    """A meeting room: the oval table with chairs all round it and room to walk round them, the screen on the end
    wall, whiteboards on the other, plants in the corners."""
    for cu in (r.w // 2 - 1, r.w // 2, r.w // 2 - 2, r.w // 2 + 1):
        if r.oval(cu, 3) or r.oval(cu, 4):
            break
    r.screen("status_screen", 0, 3, "west")
    r.on_wall("whiteboard", r.w - 1, 2, "east", 24)
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)
    for u, v in ((0, 0), (r.w - 1, 0), (0, r.d - 1)):
        r.plant(u, v)
    r.on_wall("framed_print", 0, 6, "west", 16)


def board_room(r):
    """The board room (the administration building part 2: tools/admin_rooms.py): the boardroom table (eight blocks
    long, three wide) on the line of the plant overview at the end wall, its fourteen chairs; the AV credenza under the
    overview with the video conference bar; the lectern beside it; the unit performance screen on the corridor wall
    facing the table; the master clock; a lounge corner of sofas over coffee tables, a coffee counter, bookcases,
    prints and plants."""
    import admin_rooms as AR
    b, y = r.b, r.y
    # The AV credenza against the end wall, the overview over it (its power from the credenza: no raceway).
    x, z = r.xz(0, 6)
    b.panel(AR.AV_KEY, x, y, z, r.face("west"), 3)
    for v in (4, 5, 6):
        r.keep.add((0, v))
    r.screen("plant_map_screen", 0, 5, "west")
    r.big_print(0, 2, "west")
    r.big_print(0, 9, "west")
    # The table: blocks u 4..11, v 4..6, laid facing north (columns east, rows south); chairs along both sides and at
    # the ends, room kept to walk round them.
    tu, tv = 4, 4
    assert r.win == "n"          # (u east, v south: the table's columns and rows as it is laid)
    for c in range(AR.BT_W):
        for rr in range(AR.BT_D):
            x, z = r.xz(tu + c, tv + rr)
            b.p(x, y, z, MP.multi_state("board_table", "north", rr * AR.BT_W + c))
    for c in AR.BT_SEATS:
        r.chair(tu + c, tv - 1, "door", "operator_chair")
        r.chair(tu + c, tv + AR.BT_D, "win", "operator_chair")
    r.chair(tu - 1, tv + 1, "east", "operator_chair")
    r.chair(tu + AR.BT_W, tv + 1, "west", "operator_chair")
    for u in range(tu - 2, tu + AR.BT_W + 2):
        for v in (tv - 2, tv + AR.BT_D + 1):
            if r.inside(u, v):
                r.keep.add((u, v))
    for v in range(tv - 1, tv + AR.BT_D + 1):
        r.keep.add((tu - 2, v))
        r.keep.add((tu + AR.BT_W + 1, v))
    # The lectern beside the screen, the presenter behind it looking down the table.
    r.put("board_lectern", 1, 8, "east")
    # The unit performance screen on the corridor wall, facing the table; the clock high over the lounge.
    r.screen("perf_screen", 8, r.d - 1, "door")
    x, z = r.xz(14, r.d - 1)
    b.panel("ops_wall_clock", x, y + 2, z, r.face("door"), 1)
    lu = r.w - 6
    # Two sofas facing each other over glass coffee tables, a step's room either side of the tables.
    r.row_u(("sofa", 3), lu, 1, "door")
    r.row_u(("sofa", 3), lu, 5, "win")
    r.put("glass_coffee_table", lu, 3, "west")
    r.put("glass_coffee_table", lu + 2, 3, "west")
    r.row_v(["kitchen_counter", "coffee_counter", "kitchen_counter"], r.w - 1, 6, "east")
    r.stack("bookcase", r.w - 1, 1, "east")
    r.stack("bookcase", r.w - 1, 2, "east")
    for u, v in ((0, 0), (r.w - 1, 0), (0, r.d - 1), (lu - 1, 0), (r.w - 1, 4)):
        r.plant(u, v)


def chemistry_lab(r):
    """The water chemistry lab (the administration building part 2: tools/admin_rooms.py): benches with sinks and
    analysers along the window, two islands, the fume hoods and the plant chemistry status board on the west wall,
    the gamma spectroscopy station and the flammables and corrosives cabinets on the east wall, the write-up desk by
    the window, the emergency shower and eyewash by the door, a sample fridge, coats, the lab's sign."""
    import admin_rooms as AR
    b, y = r.b, r.y
    bench = ["lab_bench", "lab_analyzer", "lab_bench", "lab_sink", "lab_bench", "lab_analyzer", "lab_bench"]
    r.row_u(bench, 2, 0, "win")
    isl = ["lab_bench", "lab_sink", "lab_analyzer", "lab_bench"]
    r.row_u(isl, 3, 3, "door")
    r.row_u(isl[::-1], 3, 4, "win")
    r.row_u(["lab_bench", "lab_analyzer", "lab_bench", "lab_bench"], 3, 7, "door")
    for v in (1, 2, 3):
        r.stack("fume_hood", 0, v, "west")
    # The status board on the west wall at eye height (nothing under it but the floor: the board stands off the wall).
    x, z = r.xz(0, 7)
    b.panel(AR.CSB_KEY, x, y + 1, z, r.face("west"), 3)
    # The write-up desk by the window: the LIMS computer, paperwork, binders.
    r.long_desk([(10, 0), (11, 0), (12, 0)], "win", [0, 1, 2], [0, 2])
    # The east wall: the gamma spectroscopy station (its cave at the window end), the safety cabinets, the fridge.
    x, z = r.xz(r.w - 1, 1)
    b.panel(AR.GS_KEY, x, y, z, r.face("east"), 3)
    for v in (1, 2, 3):
        r.keep.add((r.w - 2, v))
    for v, key in ((5, "chem_flam_cabinet"), (6, "chem_acid_cabinet")):
        x, z = r.xz(r.w - 1, v)
        b.panel(key, x, y, z, r.face("east"), 1)
        r.keep.add((r.w - 2, v))
    r.stack("fridge", r.w - 1, 9, "east")
    r.on_wall("coat_rack", r.w - 1, 8, "east")
    # By the door: the emergency shower and eyewash (its approach kept clear), the extinguisher, the lab's sign.
    x, z = r.xz(0, 9)
    for row in range(2):
        b.p(x, y + row, z, MP.multi_state("lab_safety_shower", r.face("west"), row))
    b.seats.append(("lab_safety_shower", x, y, z, r.face("west")))
    r.keep.add((1, 9))
    r.on_wall("fire_extinguisher", 0, 8, "west")
    r.on_wall("sign_chem_lab", 9, r.d - 1, "door")


def documentation(r):
    """Document control (the administration building part 2: tools/admin_rooms.py): high-density mobile shelving (eight
    carriages on floor rails between two fixed units, each carriage's handwheel and label to the cross aisle; one aisle
    open), the issue counter across the aisle's end with the clerk behind it, the plan files for the drawings, the
    reading desk by the window, the copier, the room's sign."""
    import admin_rooms as AR
    b, y = r.b, r.y
    # The shelving: fixed units at u 0 and u 10, carriages between (u 1..9, the aisle at u 5), each four blocks long
    # (v 1..4, its handwheel end at v 4), on the floor rail.
    face = r.face("win")                 # the carriages' backs to the window: their handwheel ends to the door side
    carriage = 0
    for u in range(0, 11):
        for v in range(1, 1 + AR.MS_SEGS):
            x, z = r.xz(u, v)
            if 1 <= u <= 9:
                b.p(x, y - 1, z, MP.M("jugcraft:doc_rail_floor[axis=x]", (90, 96, 110)))
            if u in (0, 10):
                seg = AR.MS_SEGS - v
                for half, name in ((0, "lower"), (1, "upper")):
                    b.p(x, y + half, z, MP.M(f"jugcraft:doc_fixed_shelving[facing={face},half={name},seg={seg}]", (200, 196, 186)))
            elif u != 5:
                seg = AR.MS_SEGS - v
                for half, name in ((0, "lower"), (1, "upper")):
                    b.p(x, y + half, z, MP.M(f"jugcraft:doc_mobile_shelving[facing={face},half={name},seg={seg},slide={AR.MS_SLIDE},variant={carriage}]",
                                             (200, 196, 186)))
        if 1 <= u <= 9 and u != 5:
            carriage += 1
        for v in range(1, 1 + AR.MS_SEGS):
            r.keep.add((u, v))
    for u in range(0, 11):
        r.keep.add((u, 5))
    # The issue counter: three blocks along v at u 12 (the visitor at u 11, the clerk at u 13 looking at them).
    fw = r.face("west")
    rx, rz = P.DIRS[P.CLOCKWISE[fw]]
    cells = [r.xz(12, v) for v in (4, 5, 6)]
    cells.sort(key=lambda p: p[0] * rx + p[1] * rz)
    b.row(["doc_issue_counter"] * 3, cells[0][0], y, cells[0][1], fw, tops=[1, 0, 2])
    r.chair(13, 5, "west", "operator_chair")
    for v in (4, 5, 6):
        r.keep.add((11, v))
    # The plan files against the east wall (their drawers to the room), the sign over the clerk's shoulder.
    for v0 in (1, 3):
        cells = sorted((r.xz(r.w - 1, v) for v in (v0, v0 + 1)), key=lambda p: p[1])
        fw = r.face("east")
        rx, rz = P.DIRS[P.CLOCKWISE[fw]]
        left = cells[0] if rz > 0 else cells[-1]
        for part in range(2):
            b.p(left[0] + rx * part, y, left[1] + rz * part, MP.M(f"jugcraft:doc_plan_file[drawer=0,facing={fw},part={part}]", (196, 198, 196)))
        b.seats.append(("doc_plan_file", left[0], y, left[1], fw))
        for v in (v0, v0 + 1):
            r.keep.add((r.w - 2, v))
    r.on_wall("sign_doc_control", r.w - 1, 5, "east")
    # The reading desk by the window, the copier by the door, plants.
    r.long_desk([(12, 0), (13, 0), (14, 0)], "win", [0, 3, 2], [0, 2])
    r.copier(r.w - 1, r.d - 1, "door")
    r.plant(r.w - 1, 0)
    r.plant(11, 0)


def training_room(r):
    """A training room: the plant status screen on the end wall (the course's live example), rows of three-seat
    training tables facing it (a laptop at each seat), the trainer's desk (the administration building part 1: no
    desk one block wide)."""
    r.screen("status_screen", 0, r.d // 2, "west")
    r.long_desk([(2, 0), (2, 1), (2, 2)], "east", [0, 1, 3], [1])
    for u in range(4, r.w - 2, 3):
        for v0 in range(1, r.d - 3, 4):
            r.long_desk([(u, v0), (u, v0 + 1), (u, v0 + 2)], "west", [4, 4, 4], [0, 1, 2])
    r.plant(r.w - 1, 0)
    r.plant(0, r.d - 1)
    r.on_wall("fire_extinguisher", r.w - 1, r.d - 2, "east")


def break_room(r, big=False):
    """A break room: a kitchenette along the west wall (counters, sink, coffee, fridges), tables with chairs."""
    n = min(6, r.d - 3)
    names = ["kitchen_counter", "kitchen_sink", "kitchen_counter", "coffee_counter", "microwave_counter", "kitchen_counter"][:n]
    r.row_v(names, 0, 0, "west")
    for k in range(2):
        r.stack("fridge", 0, n + k, "west")
    for u in range(3, r.w - 1, 4):
        for v in range(1, r.d - 2, 3):
            if r.free(u, v):
                x, z = r.xz(u, v)
                # Lunch on some of the tables: noodles, a gyro plate, a burger, a salad, coffee and a croissant.
                r.b.varied("cafe_table", x, r.y, z, "north", 6)
                r.chair(u - 1, v, "east", "cafe_chair")
                r.chair(u + 1, v, "west", "cafe_chair")
    r.put("water_cooler", r.w - 1, r.d - 1, "east")
    r.plant(r.w - 1, 0)
    r.on_wall("framed_print", r.w - 1, 2, "east", 16)
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)
    r.on_wall("fire_extinguisher", r.w - 1, r.d - 2, "east")
    if big:
        r.row_u(("sofa", 3), r.w - 5, r.d - 3, "door")
        r.screen("flat_tv", r.w - 4, r.d - 1, "door")


def locker_room(r):
    """The locker room: full-height lockers down both side walls (some standing open, a lab coat and hard hat
    inside), benches down the middle, coat racks with coats and hi-vis vests."""
    for v in range(0, r.d - 1):
        r.stack("locker_open" if v % 3 == 1 else "locker", 0, v, "west")
        r.stack("locker_open" if v % 4 == 2 else "locker", r.w - 1, v, "east")
    for u0 in range(3, r.w - 4, 5):
        r.row_u(("locker_bench", 3), u0, 2, "win")
        r.row_u(("locker_bench", 3), u0, 5, "win")
    for u in range(1, r.w - 1):
        r.on_wall("coat_rack", u, 0, "win")
    r.put("water_cooler", 2, r.d - 1, "door")
    r.on_wall("fire_extinguisher", r.w - 2, r.d - 1, "door")


def it_support(r):
    """IT support: a short row of server racks, glass desks (computers, one opened up for repair, documents,
    tools and boards), shelving of spare kit, a copier."""
    # (Older rooms round 2: the plant computer room's racks, each two blocks high, its equipment real.)
    for u, key in zip(range(1, 5), ("network_rack", "server_rack", "server_rack", "san_rack")):
        r.rack(key, u, 0, "win")
    r.long_desk([(2, 4), (3, 4), (4, 4)], "win", [0, 1, 2], [0, 2], name="glass_desk")
    r.long_desk([(8, 4), (9, 4), (10, 4)], "win", [3, 0, 1], [1], name="glass_desk")
    r.long_desk([(2, 8), (3, 8), (4, 8)], "win", [1, 3, 0], [2], name="glass_desk")
    for v in (1, 2):
        r.stack("pallet_rack", r.w - 1, v, "east", 2)
    r.copier(r.w - 1, r.d - 3, "east")
    r.on_wall("whiteboard", 0, 3, "west", 24)
    r.plant(0, r.d - 1)


def manager_office(r):
    """The plant manager's office (the administration building part 2: tools/admin_rooms.py): an executive desk with the
    window behind it, the direct line to the control room on the wall beside it, the framed operating license and the
    awards wall either side, the live plant status on one wall and the unit performance screen on the other, the
    plant overview by the door, his hard hat and vest on their hooks, a sofa over a glass coffee table, a coffee
    counter, plants."""
    import admin_rooms as AR
    b, y = r.b, r.y
    cu = r.w // 2
    r.long_desk([(cu - 1, 2), (cu, 2), (cu + 1, 2)], "door", [2, 0, 1], [1], name="executive_desk")
    r.chair(cu - 1, 4, "win", "cafe_chair")
    r.chair(cu + 1, 4, "win", "cafe_chair")
    r.screen("status_screen", 0, 5, "west")
    r.screen("perf_screen", r.w - 1, 5, "east")
    r.screen("plant_map_screen", 4, r.d - 1, "door")
    # The license (west wall) and the awards (east wall) at eye height by the window; the direct line by the desk.
    x, z = r.xz(0, 2)
    b.panel(AR.LIC_KEY, x, y + 1, z, r.face("west"), 2)
    x, z = r.xz(r.w - 1, 1)
    b.panel(AR.AW_KEY, x, y + 1, z, r.face("east"), 3)
    x, z = r.xz(0, 3)
    b.panel(AR.HL_KEY, x, y + 1, z, r.face("west"), 1)
    r.on_wall("mgr_ppe_hooks", 7, r.d - 1, "door")
    r.row_u(("sofa", 3), 1, 8, "win")
    r.lounge(1, 5) or r.lounge(1, 6) or r.put("glass_coffee_table", 2, 6, "west")
    r.row_v(["kitchen_counter", "coffee_counter"], r.w - 1, 7, "east")
    for u, v in ((0, 0), (r.w - 1, 0), (0, 8)):
        r.plant(u, v)


def first_aid(r):
    """First aid: a stretcher bed the first aid
    cabinet and the AED on the wall, a sink and cupboards, the first aider's desk, a fire extinguisher by the door."""
    r.row_u(("stretcher", 2), 1, 1, "door")
    r.on_wall("medicine_cabinet", 0, 3, "west")
    r.on_wall("aed_cabinet", 0, 4, "west")
    r.row_v(["kitchen_counter", "kitchen_sink", "kitchen_counter"], r.w - 1, 0, "east")
    r.long_desk([(5, 4), (6, 4)], "door", [1, 0], [1])
    r.on_wall("fire_extinguisher", r.w - 1, r.d - 2, "east")
    r.put("water_cooler", 0, r.d - 1, "west")
    r.plant(4, 0)
    r.on_wall("framed_print", r.w - 1, 4, "east", 16)


def intern_area(r):
    """The intern area: rows of cubicles (a two-block desk in each, partitions between them and down the middle)."""
    # The spine down the middle, desks against it on both sides, side partitions between the cubicles.
    for u0 in range(1, r.w - 3, 4):
        r.row_u(("cubicle_panel", 3), u0, 3, "door")
        for v in (1, 2):
            r.put("cubicle_panel", u0 + 3, v, "east") if u0 + 3 < r.w - 1 else None
            r.put("cubicle_panel", u0 + 3, v + 3, "east") if u0 + 3 < r.w - 1 else None
        r.long_desk([(u0, 2), (u0 + 1, 2)], "door", [0, (u0 // 4) % 3 + 1], [0])
        r.long_desk([(u0, 4), (u0 + 1, 4)], "win", [(u0 // 4) % 3 + 1, 0], [1])
    r.copier(r.w - 1, r.d - 3, "east")
    r.put("water_cooler", 0, r.d - 1, "west")
    r.on_wall("whiteboard", r.w - 1, 1, "east", 24)


THEMES = {"private": private_office, "hr": hr_office, "operations": operations, "shared": shared_office,
          "engineering": engineering, "open": open_office, "meeting": meeting_room, "board": board_room,
          "lab": chemistry_lab, "docs": documentation, "training": training_room, "break": break_room,
          "cafeteria": lambda r: break_room(r, big=True), "lockers": locker_room, "it": it_support,
          "manager": manager_office, "first_aid": first_aid, "interns": intern_area}
FLOORS = {"lab": VINYL, "break": VINYL, "cafeteria": VINYL, "lockers": VINYL, "it": VINYL, "first_aid": VINYL, "docs": CARPET}


# --------------------------------------------------------------------------------------------------- admin

def admin_building(s, ox, oz):
    """The administration building, 60 x 26, three storeys of 5 m, level with the ground. In at the front (north,
    the road side) to the reception lobby; a corridor runs the length of each floor with the stairs at its west end;
    rooms north (11 deep) and south (8 deep) of it, each with its job."""
    W, D, H = 60, 26, 14
    b = Building("admin_building", "Administration Building", W, D, H, grounded=True)
    b.shell_walls(WHITE, step=6)
    # The windows, framed glass set to the walls' outer faces (Justin, 2026-10-16: every window from now on).
    for lv in (0, 5, 10):
        b.windows(lv + 2, lv + 3, "n", step=6, glass=MP.FGLASS["south"])
        b.windows(lv + 2, lv + 3, "s", step=6, glass=MP.FGLASS["north"])
        b.windows(lv + 2, lv + 3, "e", step=6, glass=MP.FGLASS["west"])
        for z in range(17, D - 1):
            if z % 6:
                for y in (lv + 2, lv + 3):
                    b.p(0, y, z, MP.FGLASS["east"])
    b.storey(0, 1, 1, W - 2, D - 2, VINYL)
    b.storey(5, 1, 1, W - 2, D - 2, CARPET, ceiling_at=4)
    b.storey(10, 1, 1, W - 2, D - 2, CARPET, ceiling_at=9)
    b.b(1, 14, 1, W - 2, 14, D - 2, CEIL)
    b.stairwell(1, 1, [0, 5, 10], door_side="s")
    # The plan (tools/admin_fittings.py: the evacuation plans draw the same building).
    import admin_fittings as AF
    cz0, cz1 = AF.CORRIDOR  # the corridor (walls at 12 and 16)
    north, north_top, south, plan = AF.NORTH, AF.NORTH_TOP, AF.SOUTH, AF.PLAN
    rooms = []
    for lv in (0, 5, 10):
        y0, y1 = lv + 1, lv + 3
        b.b(1, lv, cz0, W - 2, lv, cz1, VINYL)
        for x in range(1, 9):
            for z in range(1, 13):
                if b.get(x, lv, z) == MP.CARPET:
                    b.p(x, lv, z, VINYL)
        b.wall_x(cz0 - 1, 9, W - 2, y0, y1)
        b.wall_x(cz1 + 1, 1, W - 2, y0, y1)
        spans_n = north_top if lv == 10 else north
        for x0, x1 in spans_n[1:]:
            b.wall_z(x0 - 1, 1, cz0 - 2, y0, y1)
        for x0, x1 in south[1:]:
            b.wall_z(x0 - 1, cz1 + 2, D - 2, y0, y1)
        for side, spans, zz in (("n", spans_n, (1, cz0 - 2)), ("s", south, (cz1 + 2, D - 2))):
            for (x0, x1), job in zip(spans, plan[lv][side]):
                if job is None or job[0] == "lobby":
                    continue
                theme, label = job
                floor = FLOORS.get(theme, CARPET)
                b.b(x0, lv, zz[0], x1, lv, zz[1], floor)
                door_x = x1 - 1 if theme in ("meeting", "board", "manager") else (x0 + x1) // 2
                if side == "n":
                    b.door(door_x, y0, cz0 - 1, "north", double=theme in ("cafeteria", "board", "lockers"), label=label)
                    r = Room(b, x0, zz[0], x1, zz[1], y0, "n", door_x - x0)
                else:
                    b.door(door_x, y0, cz1 + 1, "south", double=theme in ("meeting",), label=label)
                    r = Room(b, x0, zz[0], x1, zz[1], y0, "s", door_x - x0)
                rooms.append((theme, r))
        # The corridor: prints, plants, a water cooler, fire extinguishers.
        for x in range(15, W - 3, 10):
            big_print(b, x, y0 + 1, cz0, "north")
            big_print(b, x + 4, y0 + 1, cz1, "south")
        for x in (20, 44):
            # On plain wall only: not in front of a door, a door's sign or a window, the cells either side clear.
            for xx in (x, x + 1, x - 1, x + 2, x - 2):
                wall = [b.get(xx + k, y0 + 1, cz1 + 1) for k in (-1, 0, 1)] + [b.get(xx + k, y0, cz1 + 1) for k in (-1, 0, 1)]
                if all(m and m not in MP.DOORISH and m not in GLAZING for m in wall) \
                        and all(b.get(xx + k, y0 + 1, cz1) == 0 for k in (-1, 0, 1)):
                    b.furniture("fire_extinguisher", xx, y0 + 1, cz1, "south")
                    break
        b.plant(W - 2, y0, cz0)
        b.plant(W - 2, y0, cz1)
        b.furniture("water_cooler", 10, y0, cz1, "south")
    lobby(b, 25, 1, 35, cz0 - 2, cz0 - 1)
    for theme, r in rooms:
        THEMES[theme](r)
    admin_fittings(b)
    s.add(b, ox, oz)
    return b


def admin_fittings(b):
    """The building's fire and life safety fittings (the administration building part 1; tools/admin_fittings.py):
    on each floor a lit EXIT sign over the stair doors with a pull station beside them, the floor's evacuation plan
    on the corridor wall by the stairs, smoke detectors in every room and along the corridor (between the ceiling
    lights' grid), two emergency lights in the corridor, an AED (the ground floor's is in first aid); over the front
    doors an EXIT sign, a pull station beside them."""
    import admin_fittings as AF
    cz0, cz1 = AF.CORRIDOR

    def free(x, y, z):
        return b.get(x, y, z) == 0

    def solid(x, y, z):
        m = b.get(x, y, z)
        return bool(m) and m not in GLAZING and m not in MP.DOORISH

    def put(x, y, z, m):
        assert free(x, y, z), ("admin fittings", x, y, z, P.MATERIALS[int(b.get(x, y, z))][0])
        b.p(x, y, z, m)

    def wall_spot(xs, y, z, wall_z, facing, height=1):
        """The first x of xs with plain wall behind (at y .. y + height - 1) and the cells in front free."""
        for x in xs:
            if all(solid(x, y + k, wall_z) and free(x, y + k, z) for k in range(height)):
                return x
        raise AssertionError(("no wall for", facing, xs, y, z))
    for k, lv in enumerate((0, 5, 10)):
        y0 = lv + 1
        put(4, lv + 3, cz0, furniture("exit_sign", "north"))
        put(6, y0 + 1, cz0, furniture("fire_alarm_pull", "north"))
        b.panel("admin_evac_plan", 3, y0 + 1, cz1, "south", 2, label=AF.FLOORS[k])
        if lv:
            put(9, y0 + 1, cz1, furniture("aed_cabinet", "south"))
        # Smoke detectors: a cell between the lights (x and z multiples of 4) near each room's middle, and along the
        # corridor.
        spaces = AF.rooms(lv) + ([(25, 1, 35, cz0 - 2, "lobby")] if lv == 0 else [])
        for x0, z0, x1, z1, _ in spaces:
            cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
            cells = sorted(((x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) if x % 4 == 0 and z % 4 == 0 and free(x, lv + 3, z)),
                           key=lambda c: (c[0] - cx) ** 2 + (c[1] - cz) ** 2)
            assert cells, ("no ceiling for a smoke detector", lv, x0, z0)
            put(cells[0][0], lv + 3, cells[0][1], furniture("smoke_detector", "north"))
        for x in (12, 28, 44):
            put(x, lv + 3, 14, furniture("smoke_detector", "north"))
        # Emergency lights on the corridor walls, their conduits up to the ceiling over them.
        x = wall_spot(range(20, 40), y0 + 1, cz0, cz0 - 1, "north", 2)
        b.furniture("emergency_light", x, y0 + 1, cz0, "north")
        x = wall_spot(range(40, 58), y0 + 1, cz1, cz1 + 1, "south", 2)
        b.furniture("emergency_light", x, y0 + 1, cz1, "south")
    # The front doors: the EXIT sign on the lintel over them, a pull station on the column beside them.
    put(30, 3, 1, furniture("exit_sign", "north"))
    put(29, 2, 1, furniture("fire_alarm_pull", "north"))


def lobby(b, x0, z0, x1, z1, wall_z):
    """The front lobby: the entrance doors from the road, a waiting area (sofas round coffee tables, plants, a
    water cooler, a print and the plant's screen), the reception desk across the back with two receptionists
    (room to get round their chairs), and the one staff door into the building beside it."""
    b.b(x0, 0, z0, x1, 0, z1, VINYL)
    entrance(b, 30, 0, "n", kind="glass", canopy=True, step=False)
    # The reception desk: four blocks across the back, two computers in the middle, the phone and visitors' book
    # at one end and a small plant at the other; two chairs behind it.
    b.row(("reception_desk", 4), 28, 1, z1 - 2, "north", tops=[1, 0, 0, 2])
    for x in (29, 30):
        b.p(x, 1, z1 - 1, furniture("operator_chair", "north"))
    b.door(33, 1, wall_z, "south", kind="steel", label="staff_only")
    b.furniture("filing_cabinet", x0, 1, z1, "south")
    copier(b, x0 + 2, 1, z1, "south")
    b.plant(x1, 1, z1)
    # Waiting area: sofas along both side walls facing in over coffee tables, plants by the doors.
    b.row(("sofa", 3), x0, 1, 1, "east")
    b.furniture("glass_coffee_table", x0 + 2, 1, 2, "east")
    b.plant(x0, 1, 5)
    b.plant(28, 1, z0)
    b.plant(33, 1, z0)
    b.furniture("water_cooler", x1, 1, z0, "east")
    b.row(("sofa", 3), x1, 1, 4, "west")
    b.furniture("glass_coffee_table", x1 - 2, 1, 3, "west")
    # The plant's status on the wall over the waiting area (live; it was a painted chart).
    MP.wall_screen(b, "status_screen", x0, 2, 7, "west")
    big_print(b, x1, 2, 7, "east")
    b.p(30, 2, z1, sign("lobby", "north"))
    b.furniture("fire_extinguisher", x0, 2, z1 - 2, "west")


# --------------------------------------------------------------------------------------------------- gatehouse

def gatehouse(s, ox, oz):
    """The security gatehouse, the plant's primary access point (Justin: "rework the security post next, the whole
    building everything inside of it up to new standards"; tools/sec_gatehouse.py): in from the car park at the south
    doors to the visitors' hall; through the search train (the X-ray baggage scanner beside the walk-through metal
    detector, the hand geometry reader) and the speed gates into the protected area, framed glass from the gates to
    the guard room's wall; out at the north doors toward the plant. The guard room is the bullet-resisting enclosure
    and secondary alarm station: the security post at its window onto the hall, the alarm station console, the
    weapons locker, the officers' lockers, the kitchenette, the radios. Every window framed glass (Justin, 2026-10-16:
    all windows from now on)."""
    W, D, H = 18, 12, 4
    b = Building("security_gatehouse", "Security Gatehouse", W, D, H, grounded=True)
    b.shell_walls(WHITE, step=6)
    # The windows, framed glass set to the walls' outer faces; none in the guard room's south wall (its console and
    # kitchenette stand against it).
    skip = (1, 2, 3, 7, 8, 9, 10)
    b.windows(2, 3, "n", step=6, skip=skip, glass=MP.FGLASS["south"])
    b.windows(2, 3, "s", step=6, skip=skip + (4, 5), glass=MP.FGLASS["north"])
    b.windows(2, 3, "w", step=6, skip=skip, glass=MP.FGLASS["east"])
    b.windows(2, 3, "e", step=6, skip=skip, glass=MP.FGLASS["west"])
    b.storey(0, 1, 1, W - 2, D - 2, VINYL)
    b.b(1, 4, 1, W - 2, 4, D - 2, CEIL)
    # The guard room (west): its wall to the hall, open at the window (z 7..9) where the security post's counter and
    # glass are the barrier, its header over it; its door onto the plant side.
    b.wall_z(8, 1, 6, 1, 3)
    b.wall_z(8, 10, D - 2, 1, 3)
    b.wall_z(8, 7, 9, 3, 3)
    b.door(8, 1, 3, "west", label="guard_room")
    # The security post (tools/entrance_hall.py: the bullet-resisting counter and glass to the hall, the officer's
    # desk behind with the access log, the gate panel and the duress button), the officer's chair behind it.
    b.panel("ac_station", 7, 1, 7, "east", 3)
    b.p(6, 1, 8, furniture("operator_chair", "east"))
    # The secondary alarm station's console against the south wall (its three screens: the perimeter's zones, the
    # access log, who is inside), its chair; the kitchenette and fridge beside it.
    b.panel("sec_alarm_console", 3, 1, D - 2, "south", 3)
    b.p(2, 1, D - 3, furniture("operator_chair", "south"))
    # (Clear of the post: its south end is the kitchenette's corner.)
    b.row(["kitchen_counter", "coffee_counter", "kitchen_sink"], 6, 1, D - 2, "south")
    b.stack("fridge", 1, 1, 6, "west", n=2)
    # Along the north wall the officers' lockers, a bench under the windows, the weapons locker, the files.
    for x in range(1, 4):
        b.stack("locker_security" if x == 2 else "locker", x, 1, 1, "north", n=2)
    b.row(("locker_bench", 2), 4, 1, 2, "north")
    for row in range(2):
        b.p(6, 1 + row, 1, MP.multi_state("sec_weapons_locker", "north", row))
    b.furniture("filing_cabinet", 7, 1, 1, "north")
    # The west wall: the post orders on the whiteboard, the radios in their charger, the water cooler.
    b.varied("whiteboard", 1, 2, 3, "west", 24)
    b.seats.append(("whiteboard", 1, 2, 3, "west"))
    b.panel("ops_radio_rack", 1, 2, 7, "west", 1)
    b.furniture("water_cooler", 1, 1, 8, "west")
    b.panel("ops_wall_clock", 2, 3, 1, "north", 1)
    b.furniture("fire_extinguisher", 7, 2, 4, "east")
    # The checkpoint across the hall: the speed gates (the entry lane opening north into the protected area, the exit
    # lane south out of it; three pedestals, the door line the south edge of z 5) and framed glass from them to the
    # guard room's wall, to the ceiling.
    b.p(13, 1, 6, MP.SPEED_GATE["north"])
    b.p(15, 1, 5, MP.SPEED_GATE["south"])
    for x in (12, 14, 16):
        b.p(x, 1, 5, MP.GATE_PEDESTAL["a"])
        b.p(x, 1, 6, MP.GATE_PEDESTAL["b"])
    for y in (1, 2, 3):
        for x in (9, 10, 11):
            b.p(x, y, 5, MP.FGLASS["north"])
    # The search train: the X-ray scanner along the entry lane (its belt in from the south, its operator on the west),
    # the walk-through metal detector in the lane, the hand reader between the lanes by the gates.
    b.panel("sec_xray", 12, 1, 7, "east", 3)
    b.panel("sec_metal_detector", 13, 1, 8, "north", 1)
    b.p(14, 1, 7, furniture("ac_hand_reader", "east"))
    # The visitors' side: in at the south doors, the window to the post, a bench to sit on, the plants, the signs.
    entrance(b, 13, D - 1, "s", kind="glass", step=False)
    b.row(("sofa", 2), W - 2, 1, 9, "west")
    b.plant(W - 2, 1, 10)
    b.plant(9, 1, 10)
    b.p(W - 2, 2, 7, furniture("sign_protected_area", "east"))
    b.p(9, 2, D - 2, furniture("sign_prohibited", "west"))
    b.furniture("fire_extinguisher", 10, 2, D - 2, "south")
    # The plant side: out at the north doors.
    entrance(b, 13, 0, "n", kind="glass", step=False)
    b.plant(W - 2, 1, 1)
    b.furniture("fire_extinguisher", W - 2, 2, 2, "east")
    b.p(9, 2, 1, furniture("aed_cabinet", "north"))
    gatehouse_fittings(b)
    s.add(b, ox, oz)
    return b


# The gatehouse's dome cameras and smoke detectors (x, z under the ceiling, y 3) and emergency lights (their conduits up
# to the ceiling over them).
GATE_CAMERAS = [(11, 8), (14, 2)]
GATE_SMOKE = [(4, 5), (11, 10), (10, 3)]
GATE_EMERGENCY = [(7, 2, 6, "east"), (16, 2, 8, "east"), (16, 2, 3, "east")]


def gatehouse_fittings(b):
    """The gatehouse's ceiling fittings: dome cameras over the search train and the plant side, smoke detectors in
    each part, emergency lights."""
    for x, z in GATE_CAMERAS:
        b.p(x, 3, z, furniture("sec_dome_camera", "north"))
    for x, z in GATE_SMOKE:
        b.p(x, 3, z, furniture("smoke_detector", "north"))
    for x, y, z, f in GATE_EMERGENCY:
        b.furniture("emergency_light", x, y, z, f)


def walkway(s, x0, z0, x1, z1, m=SMOOTH):
    """A path laid in the ground (level with the grass) from x0, z0 to x1, z1 inclusive."""
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            s.decor[(x, -1, z)] = m
