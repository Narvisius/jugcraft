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
                if self.b.get(cx, cy, cz) != 0 or not behind or behind in GLAZING or not self.inside(*self.uv(cx, cz)):
                    return False
                cells.append((cx, cy, cz, r * 3 + c))
        for cx, cy, cz, part in cells:
            self.b.p(cx, cy, cz, MP.multi_state(name, fw, part))
        return True

    def oval(self, u, v, chairs=True):
        """The oval meeting table (five blocks long across u, three deep) with its middle at (u, v), chairs all round
        it and room to walk round the chairs."""
        ring = [(u + a, v + c) for a in range(-4, 5) for c in range(-3, 4)]
        if not all(self.free(a, c) or (abs(a - u) == 4 or abs(c - v) == 3) and self.open(a, c) for a, c in ring):
            return False
        # The table is laid facing north in the world: columns run east, rows south.
        for p in range(15):
            cc, rr = p % 5, p // 5
            x, z = self.xz(u, v)
            self.b.p(x + cc - 2, self.y, z + rr - 1, MP.multi_state("oval_table", "north", p))
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
    r.on_wall("framed_print", 0, 5, "west", 12)
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
    r.on_wall("framed_print", r.w - 1, 2, "east", 12)
    r.on_wall("framed_print", 0, 6, "west", 12)
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
    r.put("printer", r.w - 1, r.d - 3, "east")
    r.plant(0, 0)
    r.plant(r.w - 1, 0)


def shared_office(r):
    """A shared office (maintenance planning): two-block desks along the window, cabinets and a copier at the back,
    whiteboards and a print."""
    for u in range(1, r.w - 2, 4):
        r.long_desk([(u, 0), (u + 1, 0)], "win", [0, (u // 4) % 4 + 1], [0])
    for u in range(0, r.w, 2):
        r.put("filing_cabinet", u, r.d - 1, "door")
    r.put("printer", r.w - 1, r.d - 3, "east")
    r.plant(0, r.d - 1)
    r.on_wall("whiteboard", 0, 3, "west", 24)
    r.on_wall("whiteboard", r.w - 1, 3, "east", 24)
    r.on_wall("framed_print", r.w - 1, 1, "east", 12)


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
    r.put("printer", 0, r.d - 2, "west")
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
    r.put("printer", 0, r.d - 1, "west")
    r.put("water_cooler", r.w - 1, r.d - 1, "east")
    r.on_wall("whiteboard", 0, 4, "west", 24)
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)


def meeting_room(r):
    """A meeting room: the oval table with chairs all round it and room to walk round them, the screen on the end
    wall, whiteboards on the other, plants in the corners."""
    for cu in (r.w // 2 - 1, r.w // 2, r.w // 2 - 2, r.w // 2 + 1):
        if r.oval(cu, 3) or r.oval(cu, 4):
            break
    r.screen("training_screen", 0, 3, "west")
    r.on_wall("whiteboard", r.w - 1, 2, "east", 24)
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)
    for u, v in ((0, 0), (r.w - 1, 0), (0, r.d - 1)):
        r.plant(u, v)
    r.on_wall("framed_print", 0, 6, "west", 12)


def board_room(r):
    """The board room: the oval table, the plant overview on the end wall, a lounge corner of sofas over coffee
    tables, a coffee counter, bookcases, prints and plants."""
    r.oval(7, 5)
    r.screen("plant_map_screen", 0, 5, "west")
    r.on_wall("framed_print", 0, 2, "west", 12)
    r.on_wall("framed_print", 0, 8, "west", 12)
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
    """The water chemistry lab: benches with sinks and analysers along the window and the end wall, two islands,
    fume hoods, a write-up desk, a fridge for samples, coats by the door."""
    bench = ["lab_bench", "lab_analyzer", "lab_bench", "lab_sink", "lab_bench", "lab_analyzer", "lab_bench"]
    r.row_u(bench[:min(len(bench), r.w - 3)], 2, 0, "win")
    isl = ["lab_bench", "lab_sink", "lab_analyzer", "lab_bench"]
    r.row_u(isl, 3, 3, "door")
    r.row_u(isl[::-1], 3, 4, "win")
    r.row_u(["lab_bench", "lab_analyzer", "lab_bench", "lab_bench"], 3, 7, "door")
    for v in (1, 2, 3):
        r.stack("fume_hood", 0, v, "west")
    r.row_v(["lab_bench", "lab_analyzer", "lab_bench"], r.w - 1, 2, "east")
    r.long_desk([(r.w - 1, 6), (r.w - 1, 7)], "east", [1, 0], [0])
    r.stack("fridge", 0, 6, "west")
    r.on_wall("coat_rack", r.w - 1, r.d - 3, "east")
    r.on_wall("fire_extinguisher", 0, r.d - 2, "west")
    r.on_wall("whiteboard", 0, 5, "west", 24)


def documentation(r):
    """Documentation storage: back-to-back bookcases full of manuals and binders, filing cabinets down the walls
    (some drawers pulled out, the files showing), a reading desk by the window and a copier."""
    for u in range(2, 10):
        r.stack("bookcase", u, 2, "door")
        r.stack("bookcase", u, 3, "win")
    for v in range(1, r.d - 2):
        r.put("filing_cabinet_open" if v % 3 == 1 else "filing_cabinet", 0, v, "west")
        r.put("filing_cabinet_open" if v % 3 == 2 else "filing_cabinet", r.w - 1, v, "east")
    r.long_desk([(12, 0), (13, 0)], "win", [1, 2], [0])
    r.put("printer", 12, r.d - 1, "door")
    r.plant(r.w - 2, 0)


def training_room(r):
    """A training room: the big training screen on the end wall, rows of desks facing it, the trainer's desk."""
    r.screen("training_screen", 0, r.d // 2, "west")
    r.desk(2, 1, "east")
    for u in range(4, r.w - 2, 3):
        for v in range(1, r.d - 2):
            r.desk(u, v, "west")
    r.plant(r.w - 1, 0)
    r.plant(0, r.d - 1)
    r.on_wall("fire_extinguisher", r.w - 1, r.d - 2, "east")


def break_room(r, big=False):
    """A break room: a kitchenette along the west wall (counters, sink, coffee, fridges), tables with chairs."""
    n = min(6, r.d - 3)
    names = ["kitchen_counter", "kitchen_sink", "kitchen_counter", "coffee_counter", "kitchen_counter", "kitchen_counter"][:n]
    r.row_v(names, 0, 0, "west")
    for k in range(2):
        r.stack("fridge", 0, n + k, "west")
    for u in range(3, r.w - 1, 4):
        for v in range(1, r.d - 2, 3):
            if r.free(u, v):
                x, z = r.xz(u, v)
                r.b.furniture("cafe_table", x, r.y, z, "north")
                r.chair(u - 1, v, "east", "cafe_chair")
                r.chair(u + 1, v, "west", "cafe_chair")
    r.put("water_cooler", r.w - 1, r.d - 1, "east")
    r.plant(r.w - 1, 0)
    r.on_wall("framed_print", r.w - 1, 2, "east", 12)
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)
    r.on_wall("fire_extinguisher", r.w - 1, r.d - 2, "east")
    if big:
        r.row_u(("sofa", 3), r.w - 5, r.d - 3, "door")


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
    for u in range(1, 5):
        r.stack("server_rack", u, 0, "win")
    r.long_desk([(2, 4), (3, 4), (4, 4)], "win", [0, 1, 2], [0, 2], name="glass_desk")
    r.long_desk([(8, 4), (9, 4), (10, 4)], "win", [3, 0, 1], [1], name="glass_desk")
    r.long_desk([(2, 8), (3, 8), (4, 8)], "win", [1, 3, 0], [2], name="glass_desk")
    for v in (1, 2):
        r.stack("pallet_rack", r.w - 1, v, "east", 2)
    r.put("printer", r.w - 1, r.d - 3, "east")
    r.on_wall("whiteboard", 0, 3, "west", 24)
    r.plant(0, r.d - 1)


def manager_office(r):
    """The plant manager's office: an executive desk with the window behind it, the live plant status on one wall
    and a big flat screen on the other, the plant overview by the door, a sofa over a glass coffee table (with a
    coffee on it), a coffee counter, bookcases, prints, plants."""
    cu = r.w // 2
    r.long_desk([(cu - 1, 2), (cu, 2), (cu + 1, 2)], "door", [2, 0, 1], [1], name="executive_desk")
    r.chair(cu - 1, 4, "win", "cafe_chair")
    r.chair(cu + 1, 4, "win", "cafe_chair")
    r.screen("status_screen", 0, 5, "west")
    r.screen("flat_tv", r.w - 1, 5, "east")
    r.screen("plant_map_screen", 4, r.d - 1, "door")
    r.row_u(("sofa", 3), 1, 8, "win")
    r.put("glass_coffee_table", 2, 6, "west")
    r.row_v(["kitchen_counter", "coffee_counter"], r.w - 1, 8, "east")
    for u, v in ((0, 0), (r.w - 1, 0), (0, 8), (r.w - 1, 2), (r.w - 1, 7)):
        r.plant(u, v)
    r.on_wall("framed_print", 0, 2, "west", 12)
    r.on_wall("framed_print", r.w - 1, 2, "east", 12)


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
    r.on_wall("framed_print", r.w - 1, 4, "east", 12)


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
    r.put("printer", r.w - 1, r.d - 3, "east")
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
    for lv in (0, 5, 10):
        b.windows(lv + 2, lv + 3, "nse", step=6)
        for z in range(17, D - 1):
            if z % 6:
                for y in (lv + 2, lv + 3):
                    b.p(0, y, z, PANE)
    b.storey(0, 1, 1, W - 2, D - 2, VINYL)
    b.storey(5, 1, 1, W - 2, D - 2, CARPET, ceiling_at=4)
    b.storey(10, 1, 1, W - 2, D - 2, CARPET, ceiling_at=9)
    b.b(1, 14, 1, W - 2, 14, D - 2, CEIL)
    b.stairwell(1, 1, [0, 5, 10], door_side="s")
    cz0, cz1 = 13, 15  # the corridor (walls at 12 and 16)
    north = [(9, 23), (25, 35), (37, 58)]
    north_top = [(9, 23), (25, 35), (37, 47), (49, 58)]
    south = [(1, 11), (13, 29), (31, 41), (43, 58)]
    plan = {
        0: {"n": [("lockers", "locker_room"), None, ("cafeteria", "cafeteria")],
            "s": [("first_aid", "first_aid"), ("meeting", "meeting_room"), ("operations", "operations"), ("hr", "human_resources")]},
        5: {"n": [("lab", "chemistry_lab"), ("open", "open_office"), ("board", "board_room")],
            "s": [("engineering", "engineering"), ("docs", "document_control"), ("shared", "maintenance_planning"), ("break", "break_room")]},
        10: {"n": [("it", "it_support"), ("training", "training_room"), ("manager", "plant_manager"), ("private", "office")],
             "s": [("private", "office"), ("meeting", "meeting_room"), ("private", "office"), ("interns", "intern_area")]},
    }
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
                if job is None:
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
        for x in range(14, W - 3, 12):
            b.varied("framed_print", x, y0 + 1, cz0, "north", 12)
            b.seats.append(("framed_print", x, y0 + 1, cz0, "north"))
        for x in (20, 44):
            b.furniture("fire_extinguisher", x, y0 + 1, cz1, "south")
        b.plant(W - 2, y0, cz0)
        b.plant(W - 2, y0, cz1)
        b.furniture("water_cooler", 10, y0, cz1, "south")
    lobby(b, 25, 1, 35, cz0 - 2, cz0 - 1)
    for theme, r in rooms:
        THEMES[theme](r)
    s.add(b, ox, oz)
    return b


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
    b.furniture("printer", x0 + 1, 1, z1, "south")
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
    b.furniture("wall_display", x0, 2, 6, "west")
    b.varied("framed_print", x1, 2, 6, "east", 12)
    b.seats.append(("framed_print", x1, 2, 6, "east"))
    b.p(30, 2, z1, sign("lobby", "north"))
    b.furniture("fire_extinguisher", x0, 2, z1 - 2, "west")


# --------------------------------------------------------------------------------------------------- gatehouse

def gatehouse(s, ox, oz):
    """The security gatehouse: in from the car park at the south door to the visitors' hall, the guard window, and
    the one screening door through the checkpoint wall to the plant side; out at the north door toward the gate. The
    guard room has the camera desks, the counter at the window (one officer, one computer), security lockers and a
    kitchenette."""
    W, D, H = 18, 12, 4
    b = Building("security_gatehouse", "Security Gatehouse", W, D, H, grounded=True)
    b.shell_walls(WHITE, step=6)
    b.windows(2, 3, "nsew", step=6, skip=(1, 2, 3, 7, 8, 9, 10))
    b.storey(0, 1, 1, W - 2, D - 2, VINYL)
    b.b(1, 4, 1, W - 2, 4, D - 2, CEIL)
    # The guard room (west), its window onto the visitors' hall and its door onto the plant side.
    b.wall_z(8, 1, D - 2, 1, 3)
    for z in (7, 8, 9):
        b.p(8, 2, z, PANE)
    b.door(8, 1, 3, "west", label="guard_room")
    b.row(("reception_desk", 3), 7, 1, 7, "east", tops=[1, 0, 2])
    b.p(6, 1, 8, furniture("operator_chair", "east"))
    b.furniture("cctv_desk", 1, 1, 6, "west")
    b.p(2, 1, 6, furniture("operator_chair", "west"))
    b.furniture("cctv_desk", 1, 1, 5, "west")
    b.p(2, 1, 5, furniture("operator_chair", "west"))
    for x in range(1, 5):
        b.stack("locker_security" if x in (2, 4) else "locker", x, 1, 1, "north", n=2)
    b.row(["kitchen_counter", "coffee_counter", "kitchen_sink"], 5, 1, D - 2, "south")
    b.stack("fridge", 2, 1, D - 2, "south", n=2)
    b.furniture("filing_cabinet", 7, 1, 1, "north")
    b.furniture("water_cooler", 1, 1, 8, "west")
    b.varied("whiteboard", 1, 2, 3, "west", 24)
    b.seats.append(("whiteboard", 1, 2, 3, "west"))
    b.furniture("coat_rack", 6, 2, 1, "north")
    b.furniture("fire_extinguisher", 7, 2, 4, "east")
    # The checkpoint wall across the hall, glazed above, with the one screening door.
    b.wall_x(6, 9, W - 2, 1, 3)
    for x in range(9, W - 1):
        if x not in (12, 13):
            b.p(x, 3, 6, PANE)
    b.door(12, 1, 6, "north", kind="steel", label="screening")
    # The visitors' side: in at the south doors, a bench to sit on, a plant, the guard window.
    entrance(b, 13, D - 1, "s", kind="glass", step=False)
    b.row(("sofa", 2), W - 2, 1, 9, "west")
    b.plant(W - 2, 1, 10)
    b.plant(9, 1, 10)
    b.varied("framed_print", W - 2, 2, 7, "east", 12)
    b.seats.append(("framed_print", W - 2, 2, 7, "east"))
    # The plant side: out at the north doors.
    entrance(b, 13, 0, "n", kind="glass", step=False)
    b.plant(W - 2, 1, 1)
    b.furniture("fire_extinguisher", W - 2, 2, 2, "east")
    s.add(b, ox, oz)
    return b


def walkway(s, x0, z0, x1, z1, m=SMOOTH):
    """A path laid in the ground (level with the grass) from x0, z0 to x1, z1 inclusive."""
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            s.decor[(x, -1, z)] = m
