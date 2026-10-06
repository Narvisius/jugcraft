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

    def face(self, f):
        if f == "win":
            return "north" if self.win == "n" else "south"
        if f == "door":
            return "south" if self.win == "n" else "north"
        return f

    def free(self, u, v, y=None):
        y = self.y if y is None else y
        if not (0 <= u < self.w and 0 <= v < self.d) or (u, v) in self.keep:
            return False
        x, z = self.xz(u, v)
        return self.b.get(x, y, z) == 0 and self.b.get(x, y - 1, z) != 0

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

    def desk(self, u, v, f, name="office_desk"):
        """A desk with its chair on the user's side (the chair's cell must be free too)."""
        fw = self.face(f)
        dx, dz = P.DIRS[OPPOSITE[fw]]
        x, z = self.xz(u, v)
        cu, cv = self.uv(x + dx, z + dz)
        if self.free(u, v) and self.free(cu, cv):
            self.b.desk(x, self.y, z, fw, name)
            return True
        return False

    def uv(self, x, z):
        return x - self.x0, (z - self.z0 if self.win == "n" else self.z1 - z)

    def chair(self, u, v, f, name="operator_chair"):
        if self.free(u, v):
            x, z = self.xz(u, v)
            self.b.p(x, self.y, z, furniture(name, self.face(f)))

    def on_wall(self, name, u, v, f, n=None):
        """Something hung on the wall beside cell (u, v) on its f side, at eye height (a variant piece if n)."""
        y = self.y + 1
        fw = self.face(f)
        x, z = self.xz(u, v)
        dx, dz = P.DIRS[fw]
        behind = self.b.get(x + dx, y, z + dz)
        if not (0 <= u < self.w and 0 <= v < self.d) or self.b.get(x, y, z) != 0 or not behind or behind in GLAZING:
            return False
        if n:
            self.b.varied(name, x, y, z, fw, n)
        else:
            self.b.furniture(name, x, y, z, fw)
        return True

    def plant(self, u, v):
        if self.free(u, v):
            x, z = self.xz(u, v)
            self.b.plant(x, self.y, z)

    def row_u(self, names, u0, v, f):
        """A row of joining pieces along u from u0 (eastward), facing f (win or door)."""
        if isinstance(names, tuple):
            names = [names[0]] * names[1]
        cells = [(u0 + i, v) for i in range(len(names))]
        if not all(self.free(u, w) for u, w in cells):
            return False
        fw = self.face(f)
        # b.row lays to the user's right; start from whichever end that makes eastward.
        rx, _ = P.DIRS[P.CLOCKWISE[fw]]
        start = cells[0] if rx > 0 else cells[-1]
        x, z = self.xz(*start)
        self.b.row(names if rx > 0 else names[::-1], x, self.y, z, fw)
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
        # Order the cells from the user's left to right.
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

def private_office(r):
    """One person's office: a desk facing the door with the window behind, visitors' chairs, a bookcase, a
    filing cabinet, a whiteboard, a print and a plant."""
    cu = r.w // 2
    r.desk(cu, 1, "door")
    r.chair(cu - 1, 3, "win", "cafe_chair")
    r.chair(cu + 1, 3, "win", "cafe_chair")
    for v in (1, 2):
        r.stack("bookcase", 0, v, "west")
    r.put("filing_cabinet", r.w - 1, 1, "east")
    r.plant(r.w - 1, 0)
    r.plant(0, r.d - 1)
    r.on_wall("whiteboard", r.w - 1, 3, "east", 24)
    r.on_wall("framed_print", 0, 4, "west", 12)
    r.on_wall("framed_print", r.w - 1, 1, "east", 12)
    r.put("filing_cabinet", r.w - 1, 2, "east")
    r.plant(cu + 2, 0)
    r.stack("bookcase", cu - 2, 0, "win")
    r.stack("bookcase", cu - 3, 0, "win")
    if r.d >= 7 and r.w >= 9:
        r.row_v(("sofa", 2), r.w - 1, r.d - 4, "west")
        r.put("coffee_table", r.w - 2, r.d - 4, "west")


def shared_office(r):
    """A shared office: desks along the window, cabinets and a copier along the back, a whiteboard and prints."""
    for u in range(1, r.w - 1, 3):
        r.desk(u, 0, "win")
    if r.d >= 8:
        for u in range(1, r.w - 1, 3):
            r.desk(u, 3, "door")
    for u in range(0, r.w, 2):
        r.put("filing_cabinet", u, r.d - 1, "door")
    r.put("printer", r.w - 1, r.d - 3, "east")
    r.plant(0, r.d - 1)
    r.plant(r.w - 1, 0)
    r.on_wall("whiteboard", 0, 2, "west", 24)
    r.on_wall("framed_print", r.w - 1, 1, "east", 12)


def open_office(r):
    """An open office: pods of desks face to face, a copier, a water cooler, plants between the pods."""
    u = 1
    while u + 2 < r.w - 1:
        for k in range(3):
            r.desk(u + k, 2, "door")
            r.desk(u + k, 3, "win")
            if r.d >= 10:
                r.desk(u + k, 6, "door")
                r.desk(u + k, 7, "win")
        r.plant(u + 3, 0)
        u += 5
    r.put("printer", 0, r.d - 1, "west")
    r.put("water_cooler", r.w - 1, r.d - 1, "east")
    r.on_wall("whiteboard", 0, 3, "west", 24)
    r.on_wall("whiteboard", r.w - 1, 3, "east", 24)
    r.on_wall("framed_print", 0, 6, "west", 12)


def meeting_room(r, lounge=False):
    """A meeting room: the round table in the middle with chairs round it, the screen on one end wall,
    whiteboards on the other, plants in the corners; a sofa corner in a big one."""
    cv = r.d // 2
    placed = False
    for cu in ([r.w // 2 - 3, r.w // 2 - 2] if lounge else [r.w // 2, r.door_u - 5, r.door_u - 4, r.door_u + 5]):
        for v in (cv, cv - 1, 2):
            if not placed and cu >= 2:
                placed = r.table(cu, v)
    r.on_wall("wall_display", 0, cv, "west")
    r.on_wall("whiteboard", r.w - 1, cv - 1, "east", 24)
    r.on_wall("whiteboard", r.w - 1, cv + 1, "east", 24)
    for u, v in ((0, 0), (r.w - 1, 0), (0, r.d - 1), (r.w - 1, r.d - 1)):
        r.plant(u, v)
    r.on_wall("framed_print", 0, 1, "west", 12)
    if lounge:
        # A second seating group at the west end: two sofas facing over a coffee table.
        r.row_u(("sofa", 2), 2, 1, "door")
        r.row_u(("sofa", 2), 2, 4, "win")
        r.put("coffee_table", 2, 2, "west")
        r.put("coffee_table", 3, 2, "west")
        r.row_v(("sofa", 3), r.w - 1, 2, "west")
        r.put("coffee_table", r.w - 2, 3, "west")
        r.row_u(("sofa", 2), r.w - 4, 5, "win")
        r.stack("bookcase", r.w - 3, 0, "win")
        r.stack("bookcase", r.w - 4, 0, "win")
        r.put("water_cooler", r.w - 1, r.d - 3, "east")


def chemistry_lab(r):
    """The water chemistry lab: benches with sinks and analysers along the window, an island of benches, fume
    hoods on the end wall, coats on the door wall."""
    bench = ["lab_bench", "lab_analyzer", "lab_bench", "lab_sink", "lab_bench", "lab_analyzer"]
    n = min(len(bench), r.w - 2)
    r.row_u(bench[:n], 1, 0, "win")
    if r.d >= 8:
        isl = ["lab_bench", "lab_sink", "lab_analyzer", "lab_bench"][:max(1, r.w - 6)]
        r.row_u(isl, 3, 3, "door")
        r.row_u(isl[::-1], 3, 4, "win")
    for v in (1, 2):
        r.stack("fume_hood", 0, v + 1, "west")
    r.row_v(["lab_bench", "lab_analyzer", "lab_bench"], r.w - 1, 5, "east")
    if r.d >= 10:
        r.row_u(["lab_bench", "lab_analyzer", "lab_sink", "lab_bench"], 3, 7, "door")
    r.on_wall("coat_rack", r.w - 1, r.d - 3, "east")
    r.put("filing_cabinet", r.w - 1, 2, "east")
    r.on_wall("whiteboard", r.w - 1, 4, "east", 24)
    r.stack("fridge", 0, r.d - 3, "west")


def document_control(r):
    """Document control: rows of bookcases full of binders and manuals, a reading desk by the window."""
    for v in range(2, r.d - 2, 3):
        for u in range(1, r.w - 1):
            if u != r.w // 2:
                r.stack("bookcase", u, v, "door")
    r.desk(r.w // 2 - 2, 0, "win")
    r.desk(r.w // 2 + 2, 0, "win")
    r.put("printer", r.w - 1, 0, "east")
    r.plant(0, 0)


def training_room(r):
    """A training room: rows of desks facing the screen and whiteboard on the west wall, the trainer's desk."""
    r.on_wall("wall_display", 0, r.d // 2, "west")
    r.on_wall("whiteboard", 0, r.d // 2 - 2, "west", 24)
    r.on_wall("whiteboard", 0, r.d // 2 + 2, "west", 24)
    r.desk(1, 1, "east")
    for u in range(4, r.w - 1, 3):
        for v in range(1, r.d - 2):
            r.desk(u, v, "west")
    r.plant(r.w - 1, 0)
    r.plant(0, r.d - 1)


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


def it_support(r):
    """IT support: a short row of server racks, benches of desks, spare kit on the racking."""
    for u in range(1, min(5, r.w - 1)):
        r.stack("server_rack", u, 0, "win")
    for u in range(1, r.w - 1, 3):
        r.desk(u, 3, "win")
        r.desk(u, 6, "win")
    r.stack("pallet_rack", r.w - 1, 1, "east", 2)
    r.put("printer", r.w - 1, r.d - 3, "east")
    r.on_wall("whiteboard", 0, 3, "west", 24)
    r.plant(0, r.d - 1)


def manager_office(r):
    """The plant manager's office: a big desk by the window, a meeting table, a sofa corner, bookcases."""
    r.desk(4, 1, "door")
    r.chair(3, 3, "win", "cafe_chair")
    r.chair(5, 3, "win", "cafe_chair")
    for u in range(0, 3):
        r.stack("bookcase", u, 0, "win")
    r.table(r.w - 6, r.d // 2)
    r.row_v(("sofa", 3), r.w - 1, 1, "west")
    r.put("coffee_table", r.w - 2, 2, "west")
    for u, v in ((r.w - 1, 0), (0, r.d - 1), (r.w - 1, r.d - 1), (8, 0)):
        r.plant(u, v)
    r.on_wall("framed_print", 0, 3, "west", 12)
    r.on_wall("framed_print", 0, 6, "west", 12)
    r.on_wall("wall_display", r.w - 1, 6, "east")


def first_aid(r):
    """First aid: a sink and cabinets, a couch to lie on, the nurse's desk."""
    r.row_v(["kitchen_counter", "kitchen_sink", "kitchen_counter"], 0, 0, "west")
    r.desk(r.w - 2, 2, "west")
    r.row_u(("sofa", 2), 3, 0, "door")
    r.put("water_cooler", r.w - 1, r.d - 1, "east")
    r.put("filing_cabinet", r.w - 1, 0, "east")
    r.on_wall("framed_print", 0, 1, "west", 12)
    r.plant(0, r.d - 1)


THEMES = {"private": private_office, "shared": shared_office, "open": open_office, "meeting": meeting_room,
          "board": lambda r: meeting_room(r, lounge=True), "lab": chemistry_lab, "docs": document_control,
          "training": training_room, "break": break_room, "cafeteria": lambda r: break_room(r, big=True),
          "lockers": locker_room, "it": it_support, "manager": manager_office, "first_aid": first_aid}
FLOORS = {"lab": VINYL, "break": VINYL, "cafeteria": VINYL, "lockers": VINYL, "it": VINYL, "first_aid": VINYL, "docs": CARPET}


# --------------------------------------------------------------------------------------------------- admin

def admin_building(s, ox, oz):
    """The administration building, 60 x 26, three storeys of 5 m, level with the ground. In at the front (north,
    the road side) to the reception lobby; a corridor runs the length of each floor with the stairs at its west end;
    rooms north and south of it, each with its job."""
    W, D, H = 60, 26, 14
    b = Building("admin_building", "Administration Building", W, D, H, grounded=True)
    b.shell_walls(WHITE, step=6)
    for lv in (0, 5, 10):
        b.windows(lv + 2, lv + 3, "nse", step=6)
        for z in range(19, D - 1):
            if z % 6:
                for y in (lv + 2, lv + 3):
                    b.p(0, y, z, PANE)
    # Floors: vinyl in the corridors, carpet in the offices (rooms re-lay their own); ceilings under each slab.
    b.storey(0, 1, 1, W - 2, D - 2, VINYL)
    b.storey(5, 1, 1, W - 2, D - 2, CARPET, ceiling_at=4)
    b.storey(10, 1, 1, W - 2, D - 2, CARPET, ceiling_at=9)
    b.b(1, 14, 1, W - 2, 14, D - 2, CEIL)
    b.stairwell(1, 1, [0, 5, 10], door_side="s")
    cz0, cz1 = 13, 17  # the corridor (walls at 12 and 18, on the pilaster lines)
    north = [(9, 23), (25, 35), (37, 58)]
    south = [(1, 11), (13, 29), (31, 41), (43, 58)]
    plan = {
        0: {"n": [("lockers", "locker_room"), None, ("cafeteria", "cafeteria")],
            "s": [("first_aid", "first_aid"), ("meeting", "meeting_room"), ("shared", "operations"), ("private", "human_resources")]},
        5: {"n": [("lab", "chemistry_lab"), ("open", "open_office"), ("board", "board_room")],
            "s": [("shared", "engineering"), ("docs", "document_control"), ("shared", "maintenance_planning"), ("break", "break_room")]},
        10: {"n": [("it", "it_support"), ("training", "training_room"), ("manager", "plant_manager")],
             "s": [("private", "office"), ("private", "office"), ("meeting", "meeting_room"), ("shared", "office")]},
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
        for x0, x1 in north[1:]:
            b.wall_z(x0 - 1, 1, cz0 - 2, y0, y1)
        for x0, x1 in south[1:]:
            b.wall_z(x0 - 1, cz1 + 2, D - 2, y0, y1)
        for side, spans, zz in (("n", north, (1, cz0 - 2)), ("s", south, (cz1 + 2, D - 2))):
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
        # The corridor: a water cooler, plants and prints along it.
        for x in range(14, W - 3, 12):
            b.varied("framed_print", x, y0 + 1, cz0, "north", 12)
            b.seats.append(("framed_print", x, y0 + 1, cz0, "north"))
        b.plant(W - 2, y0, cz0)
        b.plant(W - 2, y0, cz1)
        b.furniture("water_cooler", 10, y0, cz1, "south")
    # Reception (ground floor, north middle): in at the front doors from the road.
    lobby(b, 25, 1, 35, cz0 - 2, cz0 - 1)
    for theme, r in rooms:
        THEMES[theme](r)
    s.add(b, ox, oz)
    return b


def lobby(b, x0, z0, x1, z1, wall_z):
    """The front lobby: the entrance doors from the road, a waiting area (sofas round a coffee table, plants, a
    water cooler, prints and the plant's screen), the reception desk across the back with the receptionists, and the
    one staff door into the building behind it, next to the desk."""
    b.b(x0, 0, z0, x1, 0, z1, VINYL)
    entrance(b, 30, 0, "n", kind="glass", canopy=True, step=False)
    # The reception desk: across the back, receptionists facing the doors; the staff door at its east end.
    b.row(("reception_desk", 6), 26, 1, z1 - 2, "north", seat="operator_chair")
    b.door(33, 1, wall_z, "south", kind="steel", label="staff_only")
    b.furniture("filing_cabinet", 26, 1, z1, "south")
    b.furniture("printer", 28, 1, z1, "south")
    b.plant(x1, 1, z1)
    # Waiting area: sofas along the west wall and facing it, a coffee table, plants either side of the doors.
    b.row(("sofa", 3), x0, 1, 1, "east")
    b.furniture("coffee_table", x0 + 1, 1, 2, "east")
    b.plant(x0, 1, z0)
    b.plant(x0, 1, z1)
    b.plant(28, 1, z0)
    b.plant(32, 1, z0)
    b.furniture("water_cooler", x1, 1, z0, "east")
    b.row(("sofa", 3), x1, 1, 4, "west")
    b.furniture("coffee_table", x1 - 1, 1, 3, "west")
    b.furniture("wall_display", x0, 2, 6, "west")
    b.varied("framed_print", x1, 2, 6, "east", 12)
    b.seats.append(("framed_print", x1, 2, 6, "east"))
    b.p(30, 2, z1, sign("lobby", "north"))


# --------------------------------------------------------------------------------------------------- gatehouse

def gatehouse(s, ox, oz):
    """The security gatehouse: in from the car park at the south door to the visitors' hall, the guard window, and
    the one screening door through the checkpoint wall to the plant side; out at the north door toward the gate. The
    guard room has the camera desks, the counter at the window, lockers and a kitchenette."""
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
    b.row(("reception_desk", 3), 7, 1, 7, "east")
    for z in (7, 8, 9):
        b.p(6, 1, z, furniture("operator_chair", "east"))
    b.furniture("cctv_desk", 1, 1, 6, "west")
    b.p(2, 1, 6, furniture("operator_chair", "west"))
    b.furniture("cctv_desk", 1, 1, 5, "west")
    b.p(2, 1, 5, furniture("operator_chair", "west"))
    for x in range(1, 5):
        b.stack("locker_open" if x == 3 else "locker", x, 1, 1, "north", n=2)
    b.row(["kitchen_counter", "coffee_counter", "kitchen_sink"], 5, 1, D - 2, "south")
    b.stack("fridge", 2, 1, D - 2, "south", n=2)
    b.furniture("filing_cabinet", 7, 1, 1, "north")
    b.furniture("water_cooler", 1, 1, 8, "west")
    b.varied("whiteboard", 1, 2, 3, "west", 24)
    b.seats.append(("whiteboard", 1, 2, 3, "west"))
    b.furniture("coat_rack", 6, 2, 1, "north")
    # The checkpoint wall across the hall, glazed above, with the one screening door.
    b.wall_x(6, 9, W - 2, 1, 3)
    for x in range(9, W - 1):
        if x not in (12, 13):
            b.p(x, 3, 6, PANE)
    b.door(12, 1, 6, "north", kind="steel", label="screening")
    # The visitors' side: in at the south doors, a bench, a plant, the guard window.
    entrance(b, 13, D - 1, "s", kind="glass", step=False)
    b.row(("sofa", 2), W - 2, 1, 9, "west")
    b.plant(W - 2, 1, 10)
    b.plant(9, 1, 10)
    b.varied("framed_print", W - 2, 2, 7, "east", 12)
    b.seats.append(("framed_print", W - 2, 2, 7, "east"))
    # The plant side: out at the north doors.
    entrance(b, 13, 0, "n", kind="glass", step=False)
    b.plant(W - 2, 1, 1)
    s.add(b, ox, oz)
    return b


def walkway(s, x0, z0, x1, z1, m=SMOOTH):
    """A path laid in the ground (level with the grass) from x0, z0 to x1, z1 inclusive."""
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            s.decor[(x, -1, z)] = m
