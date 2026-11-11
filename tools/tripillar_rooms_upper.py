"""Work list item 22, part B (take 1, for approval): the Tri-Pillar control building's upper floor fitted out round the
gallery, from the floor plans (claude/jugcraft-tripillar-floor-plans.md): the briefing room and the manager's office
(north-east, beside the map room), the drone engineering lab (east), the pilot simulators (south-west), the crew lounge
and kitchen (south), offices south-east and south-west, the training room (west), the meeting room (west, by pillar A's
foot), the restrooms (north-west, by the stairs). (The flight planning office, south-east and open to the gallery, was
taken out on 25 October 2026 at Justin's word; its space is open floor.)

The rooms are built like the ground floor's (tools/tripillar_rooms.py, its Rooms counted from the upper floor's slab:
floor 0, walls 1..4, band and ceiling 5): painted walls, framed glass where a room looks onto the gallery, its own
ceiling and lights; the plant's interior kit, the tower's own console desks (their role's live pages), and two new
pieces: the Ops Wall Screen (a wall screen showing a desk role's three live pages in turn; 3 x 2, or 5 x 3 for the
briefing, planning and training rooms) and the Pilot Simulator (a seat on a motion base, stick and throttle, pedals, three
screens round it showing the drone's camera; 2 x 2 blocks).

Coordinates are blocks relative to the Tower Core; the upper floor's slab is y 11 (FLOOR2). Corridors at least three
wide are kept between the rooms and the pillar feet (x -24..-5 and 5..24 at z -17..1; x -9..9 at z 8..26), the stairs'
landing (x -33..-21, z -32..-27) and the way from it to the gallery stay open.
"""
from __future__ import annotations

import random

import admin_rooms as AR
import medium_plant as MP
import tripillar_hall as HALL
import tripillar_rooms as RM
from tripillar_rooms import S, Y0, Y1, TOP, WALL, CARPET, VINYL, TILE_FLOOR, furn, dirs, right_of, turn, floor, _shell_keep

# The new pieces (item 22 part B): their block ids
SCREEN = "jugcraft:ops_wall_screen"            # 3 x 2
SCREEN_L = "jugcraft:ops_wall_screen_large"    # 5 x 3
SIM = "jugcraft:pilot_simulator"               # 2 x 2 x 2
BRIEF_SIGN = "jugcraft:briefing_sign"          # 4 x 1, over the briefing room's doors
TRAINING_DESK = "training_desk"                 # joins up in rows; its top a variant (tools/training_kit.py)
TRAINING_CHAIR = "jugcraft:training_chair"      # a sled-base chair; variants turned or pushed back
LECTERN = "jugcraft:briefing_lectern"           # the presenter's lectern: the tower's mark, a touch panel, a microphone
CEIL_SPEAKER = "jugcraft:ceiling_speaker"       # a ceiling tile with the PA speaker in it
CREDENZA = "jugcraft:av_credenza"               # 3 x 1: the AV credenza under a screen (louvred doors, the AV bay, soundbar)
CLOCK = "jugcraft:digital_wall_clock"           # the digital wall clock (the tower's time, live)


flat_tv = RM.flat_tv                            # (a Flat Screen TV's block: the ground floor's rooms have it)
EXEC_DESK = "jugcraft:exec_desk"                # 3 x 1 (tools/manager_kit.py): the manager's desk
EXEC_CHAIR = "jugcraft:exec_chair"
VISITOR_CHAIR = "jugcraft:visitor_chair"
BOOKCASE = "jugcraft:office_bookcase"           # two high (half), its shelves a variant
SOFA = "jugcraft:leather_sofa"                  # joins up (left/right); one alone is an armchair
COFFEE_TABLE = "jugcraft:lounge_coffee_table"   # 2 x 1
CAFE_TABLE = "jugcraft:bistro_table"
CERT = "jugcraft:framed_certificate"            # on the wall: the certificate, the award plaque, the millionth delivery
COAT_STAND = "jugcraft:coat_stand"              # two high
FLOOR_LAMP = "jugcraft:floor_lamp"              # two high
WASTE_BASKET = "jugcraft:waste_basket"
PLINTH = "jugcraft:display_plinth"              # a walnut plinth for a drone stand, its brass plaque
BENCH = "jugcraft:lab_workbench"                # (tools/lab_kit.py) its things a variant
PEGBOARD = "jugcraft:lab_pegboard"              # on the wall over a bench
TOOL_CAB = "jugcraft:lab_tool_cabinet"          # a roll cab, colour red | grey
TASK_CHAIR = "jugcraft:task_chair"
STOOL = "jugcraft:bench_stool"
CAGE = "jugcraft:test_cage"                     # one block of the flight test cage (its sides, part, floor)
PRINTER_FARM = "jugcraft:printer_farm"          # two high
LIPO = "jugcraft:lipo_cabinet"                  # two high
THRUST = "jugcraft:thrust_test_stand"
RACK = "jugcraft:parts_rack"                    # two high, its shelves a variant
PLOTTER = "jugcraft:lab_plotter"                # 2 x 1
PLAN_CHEST = "jugcraft:plan_chest"              # 2 x 1
WHITEBOARD = "jugcraft:rolling_whiteboard"      # 2 x 1, two high
CASE = "jugcraft:flight_case"
COUCH = "jugcraft:exam_couch"                   # (tools/rest_kit.py) 2 long: part 0 the head
MED_CUPBOARD = "jugcraft:medicine_cupboard"
MED_FRIDGE = "jugcraft:med_fridge"
PEDAL_BIN = "jugcraft:pedal_bin"
CRASH_CART = "jugcraft:crash_cart"
O2_TROLLEY = "jugcraft:oxygen_trolley"
MED_DESK = "jugcraft:med_desk"                  # 2 x 1
AID_BOX = "jugcraft:first_aid_box"
EYEWASH = "jugcraft:eyewash_bottles"
WHEELCHAIR = "jugcraft:wheelchair"
WALL_TV = "jugcraft:wall_tv"                    # 2 x 1, on a wall
CREW_SOFA = "jugcraft:crew_sofa"
CREW_TABLE = "jugcraft:crew_coffee_table"       # 2 x 1
RECLINER = "jugcraft:recliner"
SIDE_TABLE = "jugcraft:crew_side_table"
RISER = "jugcraft:tier_riser"                  # a carpeted riser block, its nosing and step light where it steps down
STEP = "jugcraft:aisle_step"                   # a half step in a tiered room's aisle (facing: the way up)
# desk roles (tower/ConsoleDeskBlock ROLES)
FLT_DIR, AIRSPACE, FLEET, DISPATCH, MISSIONS, TOWER, POWER, SUPPLY, DOCKS, MAINT = range(10)
LESSON = 15        # (the Ops Wall Screen's own role for the training room: the instructor's current slide; after the 15 desk roles)
OAK = "jugcraft:carpet_tile"


class Upper:
    """The rooms' helpers on top of tripillar_rooms.Rooms."""

    def __init__(self, R):
        self.R = R

    def wall_screen(self, x, z, facing, role, large=False, y=None):
        """An Ops Wall Screen on the wall behind cell (x, z) (its bottom middle there), facing out into the room (the
        way it faces: toward the viewer); it shows `role`'s three pages in turn. Parts row by row from the top left as
        the viewer sees it."""
        R = self.R
        w, h = (5, 3) if large else (3, 2)
        y0 = (Y0 + 1) if y is None else y
        lx, lz = dirs(turn(facing, 1))       # the viewer's left (they face the other way)
        for r in range(h):
            for c in range(w):
                cx, cz = x + lx * ((w - 1) // 2 - c), z + lz * ((w - 1) // 2 - c)
                assert R.free(cx, y0 + h - 1 - r, cz), ("screen", cx, y0 + h - 1 - r, cz, R.get(cx, y0 + h - 1 - r, cz))
                R.put(cx, y0 + h - 1 - r, cz, f"{SCREEN_L if large else SCREEN}[facing={facing},part={r * w + c},role={role}]")

    def sim(self, x, z, facing, n):
        """A Pilot Simulator: its screens' row at (x, z) and the cell to the pilot's right, the seat's row behind it; n its
        scenario (0..5: what its screens show)."""
        R = self.R
        rx, rz = right_of(facing)
        bx, bz = dirs(turn(facing, 2))
        for row in range(2):            # 0 the screens, 1 the seat
            for c in range(2):
                for k in range(2):      # its two layers
                    cx, cz = x + rx * c + bx * row, z + rz * c + bz * row
                    assert R.free(cx, Y0 + k, cz), ("sim", cx, Y0 + k, cz, R.get(cx, Y0 + k, cz))
                    R.put(cx, Y0 + k, cz, f"{SIM}[facing={facing},part={k * 4 + row * 2 + c},sim={n}]")

    def desks(self, x, z, facing, role, chair=True):
        """Three of the tower's console desks in a row from (x, z) to the operator's right, working `role`, and the
        operator's chair behind the middle one."""
        R = self.R
        rx, rz = right_of(facing)
        for k in range(3):
            R.put(x + rx * k, Y0, z + rz * k, HALL.desk_state(facing, k, role))
        if chair:
            bx, bz = dirs(turn(facing, 2))
            R.put(x + rx + bx, Y0, z + rz + bz, f"jugcraft:operator_chair[facing={facing}]")

    def long_desk(self, x, z, facing, n, tops, chairs=(), name="office_desk_long", chair="operator_chair"):
        """n joined desks from (x, z) to the user's right (tops: what stands on each), a chair behind those in chairs."""
        R = self.R
        rx, rz = right_of(facing)
        bx, bz = dirs(turn(facing, 2))
        for i in range(n):
            cx, cz = x + rx * i, z + rz * i
            assert R.free(cx, Y0, cz), (name, cx, cz, R.get(cx, Y0, cz))
            R.put(cx, Y0, cz, S(MP.row_state(name, facing, i > 0, i < n - 1, tops[i % len(tops)])))
        for i in chairs:
            R.put(x + rx * i + bx, Y0, z + rz * i + bz, furn(chair, facing))

    def oval(self, x, z, layout=0):
        """The plant's oval meeting table, five blocks along x and three along z, its middle at (x, z), chairs all round."""
        R = self.R
        for p in range(15):
            R.put(x + p % 5 - 2, Y0, z + p // 5 - 1, S(MP.multi_state("oval_table", "north", p, layout)))
        for k in range(-2, 3):
            R.put(x + k, Y0, z - 2, furn("operator_chair", "south"))
            R.put(x + k, Y0, z + 2, furn("operator_chair", "north"))
        R.put(x - 3, Y0, z, furn("operator_chair", "east"))
        R.put(x + 3, Y0, z, furn("operator_chair", "west"))

    def board(self, x0, z0):
        """The plant's boardroom table (8 along x, 3 along z) from (x0, z0), chairs down both sides and at the ends."""
        R = self.R
        for c in range(AR.BT_W):
            for rr in range(AR.BT_D):
                R.put(x0 + c, Y0, z0 + rr, S(MP.multi_state("board_table", "north", rr * AR.BT_W + c)))
        for c in AR.BT_SEATS:
            R.put(x0 + c, Y0, z0 - 1, furn("operator_chair", "south"))
            R.put(x0 + c, Y0, z0 + AR.BT_D, furn("operator_chair", "north"))
        R.put(x0 - 1, Y0, z0 + 1, furn("operator_chair", "east"))
        R.put(x0 + AR.BT_W, Y0, z0 + 1, furn("operator_chair", "west"))

    def lounge_table(self, x0, z0):
        """The oval glass coffee table, three along x and two along z, from (x0, z0)."""
        for z in range(2):
            for x in range(3):
                self.R.put(x0 + x, Y0, z0 + z, S(MP.multi_state("lounge_table", "north", z * 3 + x)))

    def f(self, name, x, y, z, facing):
        if self.R.free(x, y, z):
            self.R.put(x, y, z, furn(name, facing))

    def plant(self, x, z):
        if self.R.free(x, Y0, z):
            self.R.plant(x, z)


# ------------------------------------------------------------------ the rooms

def restrooms(R, U):
    """North-west, by the stairs' landing: the custodian's closet, its door off the gallery at its west end. (The men's
    and women's rooms in front of it, room 12 of part B, were taken out on 26 October 2026 at Justin's word, fix 8: their
    floor is open gallery now; the closet keeps its south wall, and the bottle filler stands against it.)"""
    # the custodian's closet behind them: x -27..-12, z -44..-41, closed off from the gallery with its door at x -26
    cells = R.interior(-25, -44, -12, -41) | {(-26, -43), (-26, -42), (-26, -41), (-27, -41)}
    # (the wall over its door too: Justin, 27 October, "missing the wall above the door")
    walls = {(-27, -40): "wall", (-26, -40): "wall", (-25, -40): "wall"}
    walls.update({(x, -40): "wall" for x in range(-24, -11)})     # (its south wall, the restrooms' back wall till fix 8)
    R.door(-26, -40, "north", label="custodian")
    _shell_keep(R, "closet_up", cells, walls, solid=True)
    floor(R, cells, TILE_FLOOR)
    # its west end: the mop sink in the corner, the mop rack and the dilution station beside it, the cart and the
    # wet floor sign ready by the door
    R.put(-25, Y0, -44, "jugcraft:mop_sink[facing=south,half=lower]")
    R.put(-25, Y0 + 1, -44, "jugcraft:mop_sink[facing=south,half=upper]")
    for x, n in ((-24, "mop_rack"), (-23, "chemical_dispenser")):
        R.put(x, Y0, -44, f"jugcraft:{n}[facing=south,half=lower]")
        R.put(x, Y0 + 1, -44, f"jugcraft:{n}[facing=south,half=upper]")
    # (the three janitors' carts parked side by side: C5, Justin, 9 November 2026, "they should get carts from the
    # janitor room"; each fetches theirs and parks it back at night, tools/tripillar_crew.CARTS)
    for x0 in (-22, -20, -18):
        for p in range(2):
            R.put(x0 - p, Y0, -42, f"jugcraft:janitor_cart[facing=south,part={p}]")
    R.put(-25, Y0, -42, "jugcraft:wet_floor_sign[facing=east]")
    # along the north wall the supplies; the water heater in the east corner, the scrubber on charge by it
    for k, x1 in enumerate((-20, -18, -16)):
        for p in range(2):
            for h, y in (("lower", Y0), ("upper", Y0 + 1)):
                R.put(x1 - p, y, -44, f"jugcraft:supply_shelf[facing=south,half={h},part={p},variant={k % 2}]")
    R.put(-14, Y0, -44, "jugcraft:water_heater[facing=south,half=lower]")
    R.put(-14, Y0 + 1, -44, "jugcraft:water_heater[facing=south,half=upper]")
    R.put(-12, Y0, -43, "jugcraft:floor_scrubber[facing=west]")
    U.f("spill_kit", -12, Y0, -41, "east")
    U.f("fire_extinguisher", -13, Y0 + 1, -41, "south")
    # on the gallery side of the closet's south wall: the bottle filler
    R.put(-22, Y0, -39, "jugcraft:bottle_filler[facing=south,half=lower]")
    R.put(-22, Y0 + 1, -39, "jugcraft:bottle_filler[facing=south,half=upper]")


def briefing(R, U):
    """North-east, beside the map room: the briefing room (room 1 of part B, take 2: tiered seating).

    Full height like the map room beside it (no ceiling of its own: the hall's, y 19), its glass front on the gallery
    in line with the map room's (glass y 12..16, the dark band y 17..18), the glass doors at the front (low) end, the lit
    BRIEFING sign over them on the band. The front (the map room's dark wall, x 13): the video wall, MISSIONS 5 x 3 in
    the middle with AIRSPACE (north) and FLEET (south) either side, a master clock over each; the AV credenza under
    the big screen; the lectern; a T1 Courier Quad on its display plinth in the corner. The seating in three tiers
    rising to the back, like a lecture hall: a row of desks on the floor, then one on each tier (a full block up each,
    carpeted risers with an aluminium nosing and an amber step light on their front edge; new pieces), two banks either
    side of a stepped middle aisle (half steps, lit the same), stepped walkways at the ends; a seat at every other
    desk block (roomier: 24 places). The back wall painted with a band of acoustic panels; the mission board and a
    framed picture over the back row; the exit sign, the fire extinguisher, the water cooler, plants."""
    cells = R.interior(14, -44, 24, -23)
    R.rooms["briefing"] = set(cells)
    top = 7                                      # (relative: y 18, under the hall's ceiling at 19)
    for z in range(-44, -21):
        for y in range(Y0, top + 1):
            if (25, z) in R.hex and (25, z) not in R.edge and R.free(25, y, z):
                R.put(25, y, z, "jugcraft:acoustic_wall_panel" if y in (2, 3) else WALL)
    for x in range(14, 25):
        for y in range(Y0, top + 1):
            if y >= 6:
                R.put(x, y, -22, RM.SKIN)
            elif x in (15, 16) and y <= 2:
                continue
            else:
                R.put(x, y, -22, R.glass("north"))
    # its doors: an automatic sliding pair (tools/sliding_doors.py), walking in north, the master the east cell (the
    # second to the walker's left), the leaves sliding along the glass on the gallery side
    import sliding_doors as SD
    SD.place(lambda px, py, pz, st: R._put(R.b, px, py, pz, st), 16, Y0 + R.dy, -22, "double_glass", "south")
    floor(R, cells, CARPET)
    # the tiers: risers (tier 1: x 19..21 one block up; tier 2: x 22..24 two up), half steps in the aisles
    aisles = set(range(-44, -42)) | set(range(-35, -32)) | set(range(-25, -22))
    riser = set()
    for z in range(-44, -22):
        for x in range(19, 25):
            levels = 1 if x <= 21 else 2
            for k in range(levels):
                y = Y0 + k
                step = z in aisles and ((x == 19 and k == 0) or (x == 22 and k == 1))
                if step:
                    R.put(x, y, z, f"{STEP}[facing=east]")
                else:
                    riser.add((x, y, z))
    for (x, y, z) in riser:
        edges = {d: (x + dx, y, z + dz) not in riser and not R.get(x + dx, y, z + dz)
                 for d, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0)))}
        R.put(x, y, z, f"{RISER}[" + ",".join(f"edge_{d}={'true' if edges[d] else 'false'}" for d in ("east", "north", "south", "west")) + "]")
    # the video wall and the clocks, high enough for the back row to see over the front ones
    U.wall_screen(14, -33, "east", MISSIONS, large=True, y=3)
    U.wall_screen(14, -39, "east", AIRSPACE, y=3)
    U.wall_screen(14, -27, "east", FLEET, y=3)
    for z in (-39, -27):
        R.put(14, 5, z, f"{CLOCK}[facing=east]")
    for c in range(3):
        R.put(14, Y0, -32 - c, f"{CREDENZA}[facing=west,part={c}]")
    R.put(15, Y0, -29, f"{LECTERN}[facing=east]")
    R.put(14, Y0, -43, "jugcraft:drone_display_stand[facing=east,tier=1,style=display]")
    # the desks: a row on the floor and one on each tier, two banks of seven (z -42..-36, -32..-26), a seat at every
    # other block. No two places alike: every seat's place has its own things on it (training_kit.SEATS: each of the
    # twelve twice, shuffled), the places between them the coffee jug and cups, briefing packs or nothing, the chairs
    # turned this way and that or pushed back.
    rng = random.Random(2203)
    seats = list(range(12)) * 2
    rng.shuffle(seats)
    between = [12, 14, 13, 14, 12, 15, 13, 14, 12, 15, 14, 13, 12, 14, 15, 13, 14, 12]
    for r, (x, y) in enumerate(((17, Y0), (20, Y0 + 1), (23, Y0 + 2))):
        for k, z0 in enumerate((-26, -36)):
            bank = r * 2 + k
            for i in range(7):
                v = seats[bank * 4 + i // 2] if i % 2 == 0 else between[bank * 3 + i // 2]
                R.put(x, y, z0 - i, S(MP.row_state(TRAINING_DESK, "west", i > 0, i < 6, v)))
            for i in (0, 2, 4, 6):
                R.put(x + 1, y, z0 - i, f"{TRAINING_CHAIR}[facing=west,variant={rng.randrange(6)}]")
    # the back wall, over the back row
    for c in range(2):
        R.put(24, 5, -38 + c, S(MP.multi_state("large_print", "east", c, 3)))
    U.f("whiteboard", 24, 5, -29, "east")
    # the front: the exit sign on the band over the doors, the extinguisher, the cooler, a bin, plants
    U.f("exit_sign", 15, 6, -23, "south")
    U.f("fire_extinguisher", 14, Y0 + 1, -24, "west")
    U.f("water_cooler", 16, Y0, -44, "north")
    R.put(14, Y0, -23, "jugcraft:restroom_trash_can[facing=west,fill=1]")
    U.plant(14, -44) if R.free(14, Y0, -44) else None
    U.plant(18, -23)
    # the hall's ceiling over it: supply diffusers and return grilles among its tiles, the smoke detector
    for (x, z) in ((17, -38), (17, -28), (22, -38), (22, -28)):
        R.put(x, 8, z, furn("hv_supply_diffuser", "north"))
    for (x, z) in ((19, -42), (19, -24)):
        R.put(x, 8, z, furn("hv_return_grille", "north"))
    U.f("smoke_detector", 19, top, -33, "north")
    for (x, z) in ((17, -33), (22, -33), (18, -41), (18, -25)):
        R.put(x, 8, z, CEIL_SPEAKER)
    # the lit sign over the doors, on the band on the gallery's side, as the map room's (part 0 on the reader's left)
    for p in range(4):
        R.put(14 + p, 6, -21, f"{BRIEF_SIGN}[facing=north,part={p}]")


def manager(R, U):
    """North-east, east of the briefing room: the tower manager's office (room 2 of part B; its pieces are
    tools/manager_kit.py's). The room runs up into the point the curtain wall makes with the west wall, its windows
    along that diagonal; the glass front on the gallery with its door. The desk zone in the middle: the walnut
    executive desk facing the door (the manager's high-back chair behind it, his waste basket by it, two visitors'
    chairs before it); the live screen (FLIGHT DIRECTOR) on the west wall
    beside the desk, the digital clock. In the point: the bookcases on the west wall, a model drone on its stand by
    the windows, plants. By the glass front: the leather sofa under the framed certificates and the award, the
    coffee table, an armchair, the floor lamp; a little round table with two chairs by the windows; the coat stand
    in the corner inside the door; the smoke detector."""
    cells = R.interior(27, -40, 40, -23)
    walls = {(26, z): "wall" for z in range(-45, -21)}
    walls.update({(x, -22): "north" for x in range(27, 40)})
    R.door(31, -22, "north", kind="glass", label="manager")
    _shell_keep(R, "manager", cells, walls)
    floor(R, cells, CARPET)
    for p in range(3):
        R.put(30 - p, Y0, -31, f"{EXEC_DESK}[facing=south,part={p}]")        # the manager at the north, facing the door
    R.put(29, Y0, -32, f"{EXEC_CHAIR}[facing=south,variant=1]")
    R.put(28, Y0, -29, f"{VISITOR_CHAIR}[facing=north,variant=2]")
    R.put(30, Y0, -29, f"{VISITOR_CHAIR}[facing=north,variant=1]")
    R.put(30, Y0, -32, WASTE_BASKET)                                        # by his chair, under the desk's end
    U.wall_screen(27, -30, "east", FLT_DIR)
    R.put(27, Y0 + 2, -27, f"{CLOCK}[facing=east]")
    # the point: the bookcases, the model drone by the windows, plants
    for k, z in enumerate((-37, -36, -35)):
        R.put(27, Y0, z, f"{BOOKCASE}[facing=west,half=lower,variant={k}]")
        R.put(27, Y0 + 1, z, f"{BOOKCASE}[facing=west,half=upper,variant={k + 3}]")
    R.put(29, Y0, -37, f"{PLINTH}[facing=south]")
    R.put(29, Y0 + 1, -37, "jugcraft:drone_display_stand[facing=south,tier=1,style=display]")
    U.plant(27, -40)
    U.plant(28, -39)
    # the sitting area by the glass front
    for k, z in enumerate((-26, -25, -24)):
        R.put(27, Y0, z, f"{SOFA}[facing=east,left={'true' if k else 'false'},right={'true' if k < 2 else 'false'}]")
        R.put(27, Y0 + 1, z, f"{CERT}[facing=east,variant={k}]")
    for p in range(2):
        R.put(28, Y0, -26 + p, f"{COFFEE_TABLE}[facing=east,part={p}]")
    R.put(30, Y0, -25, f"{SOFA}[facing=west,left=false,right=false]")
    R.put(27, Y0, -27, f"{FLOOR_LAMP}[half=lower]")          # (at the sofa's end, under the clock)
    R.put(27, Y0 + 1, -27, f"{FLOOR_LAMP}[half=upper]")
    U.plant(27, -28)
    # the round table by the windows, the coat stand inside the door
    R.put(34, Y0, -27, CAFE_TABLE)
    R.put(33, Y0, -27, f"{VISITOR_CHAIR}[facing=east,variant=0]")
    R.put(34, Y0, -26, f"{VISITOR_CHAIR}[facing=north,variant=2]")
    # the coat stand in the corner by the door, against the west wall and the glass front; the plant in the far corner,
    # against the glass and the windows (fix 6, Justin 26 October: both had stood out on the floor)
    R.put(27, Y0, -23, f"{COAT_STAND}[half=lower]")
    R.put(27, Y0 + 1, -23, f"{COAT_STAND}[half=upper]")
    U.plant(37, -23)
    U.f("smoke_detector", 32, Y1, -27, "north")


def lab(R, U):
    """East: the drone engineering lab (room 3 of part B; its pieces are tools/lab_kit.py's), glass onto the gallery
    (x 28) with a pair of doors; the curtain wall's diagonal to the east. Along the north wall the workbenches under
    their pegboards, the MAINTENANCE screen live over the tool cabinets, the parts racks, the clock; the two service
    stands with drones on their cradles. By the glass the plotter and the plan chest. In the middle two banks of CAD
    workstations (task chairs) and two banks of electronics benches (stools), the rolling whiteboard before them. By the
    diagonal the 3D printer farm, the LiPo charging cabinet and the thrust test stand; the flight test cage, a T1 parked
    on its pad; the drones' hard cases. Along the south wall the parts racks and the assembly benches under their
    pegboards; the AED, the extinguishers."""
    cells = R.interior(29, -17, 52, 5)
    walls = {(28, z): "east" for z in range(-18, 7)}             # (into the room, as its doors: glass and doors in one plane)
    walls.update({(x, -18): "wall" for x in range(28, 46)})
    walls.update({(x, 6): "wall" for x in range(28, 52)})
    R.door(28, -7, "east", kind="glass", double=True, label="engineering_lab")
    _shell_keep(R, "lab", cells, walls)
    floor(R, cells, "jugcraft:epoxy_floor")

    def row(name, x0, z, n, facing, variants, step=1):
        rx, rz = dirs(turn(facing, 1))
        for k in range(n):
            R.put(x0 + rx * k, Y0, z + rz * k, f"jugcraft:{name}[facing={facing},left={'true' if k else 'false'},"
                                                f"right={'true' if k < n - 1 else 'false'},variant={variants[k % len(variants)]}]")
    # the north wall: the benches and their pegboards, the screen over the cabinets, the racks, the clock
    for k, x in enumerate(range(29, 36)):
        R.put(x, Y0, -17, f"{BENCH}[facing=north,variant={(0, 3, 1, 4, 2, 5, 3)[k]}]")
        R.put(x, Y0 + 1, -17, f"{PEGBOARD}[facing=south,variant={k % 4}]")
    U.f("sign_eye_protection", 36, Y0 + 1, -17, "north")
    U.wall_screen(38, -17, "south", MAINT)
    R.put(37, Y0, -17, f"{TOOL_CAB}[facing=north,colour=red]")
    R.put(39, Y0, -17, f"{TOOL_CAB}[facing=north,colour=grey]")
    for k, x in enumerate((40, 41)):
        R.put(x, Y0, -17, f"{RACK}[facing=north,half=lower,variant={k}]")
        R.put(x, Y0 + 1, -17, f"{RACK}[facing=north,half=upper,variant={k + 2}]")
    R.put(40, Y0 + 2, -17, f"{CLOCK}[facing=south]")
    # the service stands and their cabinets
    R.put(34, Y0, -12, "jugcraft:drone_display_stand[facing=north,tier=4,style=service]")
    R.put(40, Y0, -12, "jugcraft:drone_display_stand[facing=north,tier=2,style=service]")
    R.put(32, Y0, -14, f"{TOOL_CAB}[facing=east,colour=red]")
    R.put(42, Y0, -14, f"{TOOL_CAB}[facing=west,colour=grey]")
    # by the glass: the plotter, the plan chest (their users look west, out through the glass)
    for p in range(2):
        R.put(29, Y0, -15 - p, f"{PLOTTER}[facing=west,part={p}]")
        R.put(29, Y0, -12 - p, f"{PLAN_CHEST}[facing=west,part={p}]")
    # the middle: two banks of CAD workstations, two of electronics benches, the whiteboard before them
    for x0 in (31, 38):
        row("cad_desk", x0, -8, 4, "north", (0, 1, 2, 3) if x0 == 31 else (2, 3, 0, 1))
        row("electronics_bench", x0, -3, 4, "north", (0, 1, 2, 3) if x0 == 31 else (3, 0, 1, 2))
        for k in range(4):
            R.put(x0 + k, Y0, -7, f"{TASK_CHAIR}[facing=north,variant={(0, 1, 2, 1)[(k + x0) % 4]}]")
            R.put(x0 + k, Y0, -2, STOOL)
    for p in range(2):
        R.put(36 + p, Y0, -10, f"{WHITEBOARD}[facing=north,half=lower,part={p}]")
        R.put(36 + p, Y0 + 1, -10, f"{WHITEBOARD}[facing=north,half=upper,part={p}]")
    # by the diagonal: the printer farm, the LiPo cabinet, the thrust stand (their users look east)
    for z in (-10, -9):
        R.put(44, Y0, z, f"{PRINTER_FARM}[facing=east,half=lower]")
        R.put(44, Y0 + 1, z, f"{PRINTER_FARM}[facing=east,half=upper]")
    R.put(45, Y0, -7, f"{LIPO}[facing=east,half=lower]")
    R.put(45, Y0 + 1, -7, f"{LIPO}[facing=east,half=upper]")
    R.put(45, Y0, -6, f"{THRUST}[facing=east]")
    # the flight test cage (x 44..47, z -5..-2), its door on the west, a T1 parked on its pad
    x0, z0, n = 44, -5, 4
    for x in range(x0, x0 + n):
        for z in range(z0, z0 + n):
            for k in range(3):
                sides = {"north": "net" if z == z0 else "none", "south": "net" if z == z0 + n - 1 else "none",
                         "west": "net" if x == x0 else "none", "east": "net" if x == x0 + n - 1 else "none"}
                if x == x0 and z == z0 + 2 and k < 2:
                    sides["west"] = "door"
                fl = ("drone" if (x, z) == (x0 + 2, z0 + 1) else "foam") if k == 0 else "none"
                part = ("bottom", "middle", "top")[k]
                if any(v != "none" for v in sides.values()) or part == "top" or fl != "none":
                    R.put(x, Y0 + k, z, f"{CAGE}[" + ",".join(f"{sd}={v}" for sd, v in sides.items()) + f",part={part},floor={fl}]")
    # the drones' hard cases by the cage
    for (x, z, v) in ((48, -1, 1), (49, 0, 0), (48, 1, 2), (47, 2, 0)):
        R.put(x, Y0, z, f"{CASE}[facing=west,variant={v}]")
    # the south wall: the parts racks, the assembly benches under their pegboards, the AED, extinguishers
    for k, x in enumerate(range(30, 36)):
        R.put(x, Y0, 5, f"{RACK}[facing=south,half=lower,variant={k % 4}]")
        R.put(x, Y0 + 1, 5, f"{RACK}[facing=south,half=upper,variant={(k + 1) % 4}]")
    for k, x in enumerate(range(38, 44)):
        R.put(x, Y0, 5, f"{BENCH}[facing=south,variant={(4, 2, 5, 1, 3, 0)[k]}]")
        R.put(x, Y0 + 1, 5, f"{PEGBOARD}[facing=north,variant={(k + 2) % 4}]")
        R.put(x, Y0, 4, STOOL) if k % 2 == 0 else None
    U.f("aed_cabinet", 36, Y0 + 1, 5, "south") if R.free(36, Y0 + 1, 5) else None
    U.f("fire_extinguisher", 37, Y0 + 1, 5, "south")
    for (x, z) in ((29, 0), (41, -12)):           # (against the front glass and the lift shaft's: fix 3, 26 Oct)
        U.plant(x, z) if R.free(x, Y0, z) else None
    U.f("smoke_detector", 38, Y1, -5, "north")
    U.f("smoke_detector", 46, Y1, 0, "north")


def first_aid(R, U):
    """South of the lab, their doors on the open floor where the planning office was (room 4 of part B; its pieces
    tools/rest_kit.py's).

    The first aid room: two examination couches against the north wall (one with its head raised), the IV pole by
    them, a folding screen between, the medicines cupboards; the hand-wash sink (its dispenser wall,
    the glove boxes, the sharps bin), the medicines fridge, the clinical and general waste bins; the crash cart and the
    oxygen trolley along the diagonal; the first aider's desk (the log live on its screen) and chair; on the west wall
    the first aid box, the eye wash station, the AED, the extinguisher and the spine board; the wheelchair by the door;
    the clock; a plant.

    Behind it the crew rest room for the pilots between shifts: the water cooler, the kitchenette (its espresso machine,
    kettle, microwave, sink, wall cabinets) and the fridge along the north wall; the sofa facing the TV on the west wall
    (the tower's weather, live) across the coffee table; the crew lockers, a bookcase and a bunk for a proper rest along the south wall; plants, the clock."""
    cells = R.interior(31, 7, 52, 13)
    walls = {(30, z): "wall" for z in range(6, 15)}
    walls.update({(x, 14): "wall" for x in range(30, 48)})
    R.door(30, 10, "east", label="first_aid")
    _shell_keep(R, "first_aid", cells, walls)
    floor(R, cells, VINYL)

    def tall(name, x, z, **props):
        st = ",".join(f"{k}={v}" for k, v in props.items())
        R.put(x, Y0, z, f"jugcraft:{name}[{st},half=lower]")
        R.put(x, Y0 + 1, z, f"jugcraft:{name}[{st},half=upper]")
    for x, v in ((32, 0), (36, 1)):
        for k in range(2):
            R.put(x, Y0, 7 + k, f"{COUCH}[facing=south,part={k},variant={v}]")
    tall("privacy_screen", 34, 8, facing="east")
    tall("iv_pole", 35, 7, facing="south")
    R.put(34, Y0 + 1, 7, f"{MED_CUPBOARD}[facing=south]")
    for p in range(2):
        tall("scrub_sink", 39 + p, 7, facing="north", part=p)
    R.put(41, Y0, 7, f"{MED_FRIDGE}[facing=south]")
    R.put(41, Y0 + 1, 7, f"{MED_CUPBOARD}[facing=south]")
    R.put(42, Y0, 7, f"{PEDAL_BIN}[facing=south,colour=yellow]")
    R.put(43, Y0, 7, f"{PEDAL_BIN}[facing=south,colour=black]")
    R.put(44, Y0, 9, f"{CRASH_CART}[facing=west]")
    R.put(44, Y0, 11, f"{O2_TROLLEY}[facing=west]")
    for p in range(2):
        R.put(38 + p, Y0, 12, f"{MED_DESK}[facing=north,part={p}]")
    R.put(38, Y0, 13, f"{TASK_CHAIR}[facing=north,variant=1]")
    R.put(31, Y0 + 1, 8, f"{AID_BOX}[facing=east]")
    R.put(31, Y0 + 1, 9, f"{EYEWASH}[facing=east]")
    U.f("aed_cabinet", 31, Y0 + 1, 12, "west")
    U.f("fire_extinguisher", 31, Y0 + 1, 11, "west")
    tall("spine_board", 31, 13, facing="east")
    R.put(33, Y0, 12, f"{WHEELCHAIR}[facing=east]")
    R.put(31, Y0, 8, "jugcraft:step_stool[facing=east]")
    R.put(37, Y0, 8, "jugcraft:dressing_trolley[facing=west]")
    R.put(37, Y0 + 1, 7, "jugcraft:diagnostic_set[facing=south]")
    tall("column_scale", 42, 13, facing="north")
    R.put(35, Y0 + 1, 13, "jugcraft:med_poster[facing=north,variant=1]")
    R.put(41, Y0 + 1, 13, "jugcraft:med_poster[facing=north,variant=0]")
    R.put(39, Y0 + 2, 13, f"{CLOCK}[facing=north]")
    U.plant(43, 13) if R.free(43, Y0, 13) else None
    U.f("smoke_detector", 38, Y1, 10, "north")
    # the crew rest room
    cells = R.interior(31, 15, 52, 21)
    walls = {(30, z): "wall" for z in range(14, 23)}
    walls.update({(x, 22): "wall" for x in range(30, 45)})
    R.door(30, 19, "east", label="crew_rest")
    _shell_keep(R, "crew_rest", cells, walls)
    floor(R, cells, CARPET)
    tall("water_dispenser", 31, 15, facing="south")
    for p in range(3):
        tall("kitchenette", 35 + p, 15, facing="north", part=p)
    tall("crew_fridge", 38, 15, facing="south")
    for p in range(2):
        R.put(31, Y0 + 1, 16 + p, f"{WALL_TV}[facing=east,part={p}]")
    for i, z in enumerate((18, 17, 16)):
        R.put(34, Y0, z, f"{CREW_SOFA}[facing=west,left={'true' if i else 'false'},right={'true' if i < 2 else 'false'}]")
    for p in range(2):
        R.put(32, Y0, 17 - p, f"{CREW_TABLE}[facing=west,part={p}]")
    # (no recliners at the windows: Justin, 26 October, had the pair and their side table taken out, fix 5)
    R.put(31, Y0 + 2, 18, f"{CLOCK}[facing=east]")
    for k, x in enumerate(range(31, 35)):
        tall("crew_locker", x, 21, facing="north", variant=(0, 1, 2, 3)[k])
    R.put(35, Y0, 21, f"{BOOKCASE}[facing=south,half=lower,variant=2]")
    R.put(35, Y0 + 1, 21, f"{BOOKCASE}[facing=south,half=upper,variant=5]")
    for p in range(2):
        tall("crew_bunk", 36 + p, 21, facing="north", part=p)
    U.plant(38, 21) if R.free(38, Y0, 21) else None
    U.plant(41, 16) if R.free(41, Y0, 16) else None
    U.f("smoke_detector", 36, Y1, 18, "north")


def simulators(R, U):
    """South-west: the pilot simulators (room 9 of part B; its pieces tools/sim_kit.py's), a room with glass onto the
    gallery (north, its doors; and east, one straight run from the north-east corner to the back wall).

    Six Pilot Simulators in two rows of three facing north, the gallery watching through the glass; behind them the
    instructor's station (a repeater of each simulator, live; the control panel: freeze, faults, weather, the emergency
    stop). The debrief table facing the wall screen (FLEET) on the back wall, the clock over it, the acoustic panels
    along it. The pilots' lockers and the headset rack on the west wall, the extinguisher; the water cooler and the
    files by the east glass; plants. (The waiting bench inside the doors went with the corner, 26 October.)"""
    # The east glass one straight run at x -18 (fix 2 of Justin's 26 October list cut the north-east corner back off the
    # atrium, the glass stepping back round it to x -13 from z 12; on 27 October the step "out in the middle of the open"
    # went too: the room ends at x -18 all the way, the gallery open from there to the pillar).
    cells = R.interior(-34, 6, -19, 21)
    walls = {(x, 5): "south" for x in range(-35, -18)}
    walls.update({(-18, z): "east" for z in range(5, 23)})
    # (its back wall ends at its glass: until 5 November 2026 it ran on five blocks east into the gallery, the back wall
    # of the bay the room left; Justin: "this extra length of wall ... has no purpose")
    walls.update({(x, 22): "wall" for x in range(-40, -17)})
    walls.update({(-35, z): "wall" for z in range(5, 23)})
    cells -= set(walls)          # (the new glass stands on the gallery's floor, as the rest of the room's glass does)
    R.door(-24, 5, "south", kind="glass", double=True, label="simulators")
    _shell_keep(R, "simulators", cells, walls)
    floor(R, cells, CARPET)

    def put(name, x, y, z, **props):
        st = ",".join(f"{k}={v}" for k, v in props.items())
        assert (x, z) in cells and R.free(x, y, z), (name, x, y, z, R.get(x, y, z))
        R.put(x, y, z, f"jugcraft:{name}[{st}]")

    def tall(name, x, z, **props):
        put(name, x, Y0, z, half="lower", **props)
        put(name, x, Y0 + 1, z, half="upper", **props)
    for i, z in enumerate((9, 14)):                       # the far row's scenarios 1 to 3 from the west, the near row's 4 to 6
        for j, x in enumerate((-32, -27, -22)):
            U.sim(x, z, "north", i * 3 + j)
    for p in range(3):
        put("instructor_station", -28 + p, Y0, 18, facing="north", part=p)
    put("task_chair", -27, Y0, 19, facing="north", variant=1)
    # the debrief: the table facing the wall screen, the clock, the acoustic panels
    U.wall_screen(-20, 21, "north", FLEET)
    for p in range(3):
        put("team_table", -21 + p, Y0, 19, facing="north", part=p)
        put("task_chair", -21 + p, Y0, 18, facing="south", variant=p % 3)
    R.put(-24, Y0 + 2, 21, f"{CLOCK}[facing=north]")
    for k, x in enumerate((-33, -31, -29, -27, -25)):
        for dy in (1, 2):
            put("acoustic_panel", x, Y0 + dy, 21, facing="north", variant=(k + dy) % 3)
    # the west wall: lockers, the headset rack, the extinguisher
    for k, z in enumerate((15, 16, 17, 18)):
        tall("crew_locker", -34, z, facing="east", variant=(1, 2, 3, 0)[k])
    for p in range(2):
        put("headset_rack", -34, Y0 + 1, 11 + p, facing="east", part=p)
    U.f("fire_extinguisher", -34, Y0 + 1, 9, "west")
    # (no waiting bench inside the doors: Justin, 26 October, had it and its side table taken out with the corner)
    # by the east glass the water cooler and the files
    tall("water_dispenser", -19, 12, facing="west")
    tall("steel_filing_cabinet", -19, 13, facing="west", variant=0)
    for (x, z) in ((-19, 17), (-34, 6), (-19, 6)):
        U.plant(x, z)
    U.f("smoke_detector", -24, Y1, 12, "north")


def lounge(R, U):
    """South: the crew lounge and kitchen (room 7 of part B; its pieces tools/lounge_kit.py's), glass onto the gallery
    with two pairs of doors.

    The kitchen along the west wall: the drinks cooler and the snack machine by the doors, the fridge freezer, the run
    of cupboards (drawers, the oven under the hob and its hood, the sink, the dishwasher, the coffee corner with its two
    machines and the open shelves of mugs, the microwave), the larder; the island with its bar stools across from it.
    Tables for four by the south windows and down the middle. The lounge at the east end: the sofas round the coffee
    table on the rug, facing the Flat Screen TV (anyone's videos) over the media console; two recliners by the windows; the
    foosball table; bookcases of books and board games; the water cooler, the coat stand, plants, the clock."""
    cells = R.interior(-18, 31, 18, 44)
    walls = {(x, 30): "south" for x in range(-18, 19)}           # (into the room, as its doors: glass and doors in one plane)
    walls.update({(-19, z): "wall" for z in range(30, 46)})
    walls.update({(19, z): "wall" for z in range(30, 46)})
    for dx in (-11, 10):
        R.door(dx, 30, "south", kind="glass", double=True, label="crew_lounge")
    _shell_keep(R, "lounge", cells, walls)
    floor(R, cells, VINYL)

    def put(name, x, y, z, **props):
        st = ",".join(f"{k}={v}" for k, v in props.items())
        assert (x, z) in cells and R.free(x, y, z), (name, x, y, z, R.get(x, y, z))
        R.put(x, y, z, f"jugcraft:{name}[{st}]")

    def tall(name, x, z, **props):
        put(name, x, Y0, z, half="lower", **props)
        put(name, x, Y0 + 1, z, half="upper", **props)
    # the kitchen along the west wall (its run built with the wall to its north: facing west puts it on this wall)
    tall("drinks_fridge", -18, 31, facing="east")
    tall("vending_machine", -18, 32, facing="east")
    tall("fridge_freezer", -18, 33, facing="east")
    for p in range(10):
        tall("kitchen_run", -18, 43 - p, facing="west", part=p)
    tall("storage_cupboard", -18, 44, facing="east")
    R.put(-18, Y0 + 2, 38, f"{CLOCK}[facing=east]")
    for p in range(4):
        put("kitchen_island", -16, Y0, 35 + p, facing="east", part=p)
        put("bar_stool", -15, Y0, 35 + p, variant=p % 2)
    # the dining tables, their chairs (some pulled out, some turned)
    rng = random.Random(4207)
    for row, z in enumerate((37, 42)):
        for k, x0 in enumerate((-11, -7, -3, 1)):
            for p in range(2):
                put("dining_table", x0 + p, Y0, z, facing="north", part=p, variant=(k + row) % 3)
                put("dining_chair", x0 + p, Y0, z - 1, facing="south", variant=rng.choice((0, 0, 1, 2)))
                put("dining_chair", x0 + p, Y0, z + 1, facing="north", variant=rng.choice((0, 0, 1, 2)))
    # the lounge: the rug (as the floor under it), the sofas round the coffee table, the screen over the console; the
    # whole sitting group centred on the TV (Justin, 10 November 2026: "move these couches and stuff over to be centered
    # with the TV on the wall"; layout 51: one block south of where it stood, the TV's z 36..38)
    for xi in range(4):
        for zi in range(3):
            R.put(12 + xi, 0, 36 + zi, f"jugcraft:lounge_rug[part={zi * 4 + xi}]")
    # the TV over the media console: a Flat Screen TV three wide and two high, hung on the east wall (its back east),
    # anyone's to put a video on (Justin, 27 October 2026: the TOWER TV here "needs to be replaced with one of the flat
    # screens you can put the YouTube videos on"; before that, note 20 of his walk through had made the ops screen the
    # Lounge TV)
    for c, z in enumerate((36, 37, 38)):               # (its viewer faces east: their left is north)
        for r in range(2):
            R.put(18, Y0 + 1 + r, z, flat_tv("east", c, r, 3, 2))
    for p in range(3):
        put("media_console", 18, Y0, 38 - p, facing="west", part=p)
    for i, z in enumerate((36, 37, 38)):
        R.put(11, Y0, z, f"{CREW_SOFA}[facing=east,left={'true' if i else 'false'},right={'true' if i < 2 else 'false'}]")
    for i, x in enumerate((14, 13)):
        R.put(x, Y0, 35, f"{CREW_SOFA}[facing=south,left={'true' if i else 'false'},right={'true' if i < 1 else 'false'}]")
    for i, x in enumerate((13, 14)):
        R.put(x, Y0, 39, f"{CREW_SOFA}[facing=north,left={'true' if i else 'false'},right={'true' if i < 1 else 'false'}]")
    for p in range(2):
        R.put(13 + p, Y0, 37, f"{CREW_TABLE}[facing=north,part={p}]")
    R.put(11, Y0, 39, f"{FLOOR_LAMP}[half=lower]")
    R.put(11, Y0 + 1, 39, f"{FLOOR_LAMP}[half=upper]")
    # (both before the window between x 9 and 11: the mullions are at x 8 and 12)
    R.put(9, Y0, 43, f"{RECLINER}[facing=south,variant=1]")
    R.put(10, Y0, 43, f"{SIDE_TABLE}[facing=south]")
    R.put(11, Y0, 43, f"{RECLINER}[facing=south,variant=0]")
    for p in range(2):
        put("foosball_table", 7 + p, Y0, 33, facing="north", part=p)
    for z, k in ((32, 1), (42, 4)):
        # (a bookcase's facing is the wall at its back: its shelves open the other way, into the room)
        R.put(18, Y0, z, f"{BOOKCASE}[facing=east,half=lower,variant={k}]")
        R.put(18, Y0 + 1, z, f"{BOOKCASE}[facing=east,half=upper,variant={k + 1}]")
    # by the glass: the water cooler, the coat stand, plants
    tall("water_dispenser", -2, 31, facing="south")
    R.put(13, Y0, 31, f"{COAT_STAND}[half=lower]")
    R.put(13, Y0 + 1, 31, f"{COAT_STAND}[half=upper]")
    for (x, z) in ((-5, 31), (4, 31), (17, 44), (-6, 44), (17, 31)):
        U.plant(x, z)


def offices_sw(R, U):
    """South-west (room 8 of part B; its pieces tools/office_kit.py's, as room 6's): the operations support office in the
    corner the curtain wall makes, its door on the corridor down to the lounge (x -23, z 31).

    Two benches of desks in the wide north end (their things their own), the team table for the morning stand-up with
    its conference phone; along the inner wall two benches of two desks facing it, the whiteboard and the notice board
    over them by the door; the phone booth in the narrow tip. Along the north wall the filing cabinets, the copier under
    the clock, the recycling bins, the stationery cupboard, the post pigeonholes and the year planner; the coat stand by
    the door; plants by the windows."""
    cells = {(x, z) for (x, z) in R.interior(-39, 23, -24, 44)}
    walls = {(-23, z): "wall" for z in range(22, 46)}
    walls.update({(x, 22): "wall" for x in range(-39, -22)})
    R.door(-23, 31, "west", label="offices")
    _shell_keep(R, "offices_sw", cells, walls)
    floor(R, cells, CARPET)

    def put(name, x, y, z, **props):
        st = ",".join(f"{k}={v}" for k, v in props.items())
        assert (x, z) in cells and R.free(x, y, z), (name, x, y, z, R.get(x, y, z))
        R.put(x, y, z, f"jugcraft:{name}[{st}]" if st else f"jugcraft:{name}")

    def tall(name, x, z, **props):
        put(name, x, Y0, z, half="lower", **props)
        put(name, x, Y0 + 1, z, half="upper", **props)
    for k, (x0, z0) in enumerate(((-28, 26), (-32, 26))):
        for xi in range(3):
            for zi in range(2):
                put("office_bench", x0 + xi, Y0, z0 + zi, facing="north", part=zi * 3 + xi, variant=3 + k)
        for j, x in enumerate((x0, x0 + 2)):
            put("task_chair", x, Y0, z0 - 1, facing="south", variant=(k + j + 1) % 3)
            put("task_chair", x, Y0, z0 + 2, facing="north", variant=(k + j) % 3)
    for p in range(3):
        put("team_table", -28 + p, Y0, 31, facing="north", part=p)
        put("task_chair", -28 + p, Y0, 30, facing="south", variant=p % 3)
        put("task_chair", -28 + p, Y0, 32, facing="north", variant=(p + 2) % 3)
    for k, z0 in enumerate((34, 38)):
        for p in range(3):
            put("office_bench_single", -24, Y0, z0 + 2 - p, facing="west", part=p, variant=2 + k)
        put("task_chair", -25, Y0, z0, facing="east", variant=2)
        put("task_chair", -25, Y0, z0 + 2, facing="east", variant=0)
    tall("phone_booth", -24, 42, facing="west")
    tall("steel_filing_cabinet", -24, 23, facing="south", variant=1)
    tall("steel_filing_cabinet", -25, 23, facing="south", variant=0)
    tall("mfp_copier", -27, 23, facing="south")
    R.put(-27, Y0 + 2, 23, f"{CLOCK}[facing=south]")
    put("recycling_station", -28, Y0, 23, facing="south")
    tall("storage_cupboard", -29, 23, facing="south")
    put("mail_sorter", -30, Y0, 23, facing="south")
    for p in range(2):
        put("year_planner", -32 - p, Y0 + 1, 23, facing="south", part=p)
        put("wall_whiteboard", -24, Y0 + 1, 25 - p, facing="west", part=p)
        put("office_noticeboard", -24, Y0 + 1, 28 - p, facing="west", part=p)
    R.put(-24, Y0, 32, f"{COAT_STAND}[half=lower]")
    R.put(-24, Y0 + 1, 32, f"{COAT_STAND}[half=upper]")
    # the plants against the walls, the tip's in its corner behind the phone booth (fix 3, Justin 26 Oct: not between the
    # booth and the desks, nor loose on the floor)
    for (x, z) in ((-36, 23), (-36, 25), (-24, 44)):
        U.plant(x, z) if (x, z) in cells else None
    U.f("smoke_detector", -30, Y1, 29, "north")


def offices_se(R, U):
    """South-east (room 6 of part B; its pieces tools/office_kit.py's): the open office in the corner the curtain wall
    makes, its door on the corridor down to the lounge (x 23, z 31).

    In its wide north end three benches of desks (two a side back to back, the acoustic screen between, each desk its
    two monitors live and its own things) and a small round table for a quick word; along the west wall two benches
    of two desks facing the wall; in the narrow south tip a phone booth. Along the north wall the steel filing
    cabinets, the copier under the clock, the recycling bins, the stationery cupboard and the whiteboard with the
    week's plan; the notice board on the west wall by the door, the coat stand inside it; plants by the windows."""
    s = 1
    cells = {(x, z) for (x, z) in R.interior(24, 23, 39, 44)}
    walls = {(23, z): "wall" for z in range(22, 46)}
    walls.update({(x, 22): "wall" for x in range(23, 40)})
    R.door(23, 31, "east", label="offices")
    _shell_keep(R, "offices_se", cells, walls)
    floor(R, cells, CARPET)

    def put(name, x, y, z, **props):
        st = ",".join(f"{k}={v}" for k, v in props.items())
        assert (x, z) in cells and R.free(x, y, z), (name, x, y, z, R.get(x, y, z))
        R.put(x, y, z, f"jugcraft:{name}[{st}]")

    def tall(name, x, z, **props):
        put(name, x, Y0, z, half="lower", **props)
        put(name, x, Y0 + 1, z, half="upper", **props)
    # the benches in the wide end, their chairs either side
    for k, (x0, z0) in enumerate(((26, 26), (30, 26), (26, 30))):
        for xi in range(3):
            for zi in range(2):
                put("office_bench", x0 + xi, Y0, z0 + zi, facing="north", part=zi * 3 + xi, variant=k)
        for j, x in enumerate((x0, x0 + 2)):
            put("task_chair", x, Y0, z0 - 1, facing="south", variant=(k + j) % 3)
            put("task_chair", x, Y0, z0 + 2, facing="north", variant=(k + j + 1) % 3)
    R.put(31, Y0, 31, CAFE_TABLE)
    R.put(30, Y0, 31, f"{VISITOR_CHAIR}[facing=east,variant=1]")
    R.put(32, Y0, 31, f"{VISITOR_CHAIR}[facing=west,variant=0]")
    # the benches along the west wall, their users facing it
    for z0 in (34, 38):
        for p in range(3):
            put("office_bench_single", 24, Y0, z0 + p, facing="east", part=p, variant=(z0 // 4) % 2)
        put("task_chair", 25, Y0, z0, facing="west", variant=1)
        put("task_chair", 25, Y0, z0 + 2, facing="west", variant=2)
    tall("phone_booth", 24, 42, facing="east")
    # the north wall: the files, the copier and the clock over it, the bins, the cupboard, the whiteboard
    tall("steel_filing_cabinet", 24, 23, facing="south", variant=0)
    tall("steel_filing_cabinet", 25, 23, facing="south", variant=2)
    tall("mfp_copier", 27, 23, facing="south")
    R.put(27, Y0 + 2, 23, f"{CLOCK}[facing=south]")
    put("recycling_station", 28, Y0, 23, facing="south")
    tall("storage_cupboard", 29, 23, facing="south")
    for p in range(2):
        put("wall_whiteboard", 32 - p, Y0 + 1, 23, facing="south", part=p)
        put("office_noticeboard", 24, Y0 + 1, 27 + p, facing="east", part=p)
    R.put(24, Y0, 32, f"{COAT_STAND}[half=lower]")
    R.put(24, Y0 + 1, 32, f"{COAT_STAND}[half=upper]")
    for (x, z) in ((36, 23), (36, 25), (24, 44)):  # (against the walls: fix 3, as the south-west offices')
        U.plant(x, z) if (x, z) in cells else None
    U.f("smoke_detector", 30, Y1, 29, "north")


def training(R, U):
    """West (room 10 of part B; its pieces tools/class_kit.py's): the flight school's classroom, a wedge between the
    straight front wall (x -38, the glass doors at its south end) and the curtain wall's two slopes meeting at its
    west point.

    The teaching wall is the front wall's middle: the big wall screen between two whiteboards, the clock; the
    instructor's desk at its south side, the display cart at its north. Facing it two rows of four training tables, a
    student's things on each place and a training chair at each. The narrow north end is the workshop: the parts board
    on the wall, three build benches with their stools, the training radios charging on the wall, the LiPo cabinet and
    flight cases in the tip, posters. By the doors in the south end: four carrels for the computer-based exams, the
    water cooler, the extinguisher, the cubbies for bags under the notice board, the coat stand, the recycling."""
    # Its tips closed to the curtain wall (fix 1 of Justin's 26 Oct list: "cut off on both sides"): at the north the
    # return wall goes on in line with the meeting room's north wall (z -17) to the curtain wall; at the south the
    # front wall runs on to the south-west offices' wall (z 22), the room taking in the bay by the windows there.
    cells = R.interior(-52, -16, -39, 21)
    walls = {(-38, z): "wall" for z in range(-17, 22)}
    walls.update({(x, -17): "wall" for x in range(-41, -38)})
    R.door(-38, 10, "west", kind="glass", double=True, label="training_room")
    _shell_keep(R, "training", cells, walls)
    # the passage between it and the simulators, walled in to its end now: a light in the hall's ceiling over that end
    R._put(R.b, -37, R.lamp_y, 19, RM.LAMP)
    floor(R, cells, CARPET)
    rng = random.Random(1010)

    def put(name, x, y, z, **props):
        st = ",".join(f"{k}={v}" for k, v in props.items())
        assert (x, z) in cells and R.free(x, y, z), (name, x, y, z, R.get(x, y, z))
        R.put(x, y, z, f"jugcraft:{name}[{st}]")

    def tall(name, x, z, **props):
        put(name, x, Y0, z, half="lower", **props)
        put(name, x, Y0 + 1, z, half="upper", **props)
    # the teaching wall: the screen between the whiteboards, the clock; the desk, the cart
    U.wall_screen(-39, 0, "west", LESSON, large=True)
    for p in range(2):
        put("class_whiteboard", -39, Y0 + 1, -3 - p, facing="west", part=p, variant=0)
        put("class_whiteboard", -39, Y0 + 1, 4 - p, facing="west", part=p, variant=1)
        put("instructor_desk", -41, Y0, 5 - p, facing="west", part=p)
        for h, y in (("lower", Y0), ("upper", Y0 + 1)):
            put("touch_display_cart", -40, y, -6 - p, facing="west", part=p, half=h)
    R.put(-39, Y0 + 2, 4, f"{CLOCK}[facing=west]")
    put("task_chair", -40, Y0, 5, facing="west", variant=0)
    # the class: two rows of four tables facing the screen, a chair at each place
    for row, (x, tops) in enumerate(((-43, (0, 1, 2, 3)), (-46, (4, 5, 2, 0)))):
        for k, z0 in enumerate((-4, -2, 0, 2)):
            for p in range(2):
                put("training_table", x, Y0, z0 + p, facing="east", part=p, variant=tops[k])
                R.put(x - 1, Y0, z0 + p, f"{TRAINING_CHAIR}[facing=east,variant={rng.randrange(6)}]")
    # the workshop in the north end
    for h, y in (("lower", Y0 + 1), ("upper", Y0 + 2)):
        for p in range(3):
            put("parts_board", -39, y, -10 - p, facing="west", half=h, part=p)
    for p in range(2):
        put("build_table", -42, Y0, -11 + p, facing="east", part=p, variant=0)
        R.put(-43, Y0, -11 + p, STOOL)
        put("build_table", -45 + p, Y0, -9, facing="north", part=p, variant=1)
        R.put(-45 + p, Y0, -8, STOOL)
        put("build_table", -42 + p, Y0, -14, facing="north", part=p, variant=2)
        R.put(-42 + p, Y0, -13, STOOL)
        put("controller_rack", -39, Y0 + 1, -14 - p, facing="west", part=p)
    R.put(-39, Y0, -16, f"{LIPO}[facing=west,half=lower]")
    R.put(-39, Y0 + 1, -16, f"{LIPO}[facing=west,half=upper]")
    for x, v in ((-40, 1), (-41, 0)):
        R.put(x, Y0, -16, f"{CASE}[facing=west,variant={v}]")
    for z, v in ((-13, 3), (-8, 4), (-5, 0), (6, 2), (7, 1)):
        put("classroom_poster", -39, Y0 + 1, z, facing="west", variant=v)
    # by the doors: the carrels, the water, the cubbies under the notices, the coats, the bins
    for k, z in enumerate((7, 8, 9, 10)):
        put("study_carrel", -44, Y0, z, facing="west", variant=k)
        put("task_chair", -43, Y0, z, facing="west", variant=(k + 1) % 3)
    tall("water_dispenser", -39, 8, facing="west")
    U.f("fire_extinguisher", -39, Y0 + 1, 9, "east")
    for p in range(2):
        put("bag_cubbies", -39, Y0, 13 - p, facing="west", part=p)
        put("office_noticeboard", -39, Y0 + 1, 13 - p, facing="west", part=p)
    R.put(-39, Y0, 14, f"{COAT_STAND}[half=lower]")
    R.put(-39, Y0 + 1, 14, f"{COAT_STAND}[half=upper]")
    put("recycling_station", -39, Y0, 15, facing="west")
    for (x, z) in ((-48, -5), (-48, 5), (-44, 12)):
        U.plant(x, z)
    U.f("smoke_detector", -44, Y1, 0, "north")


def meeting(R, U):
    """West, by pillar A's foot (room 11 of part B; its pieces tools/meeting_kit.py's): the meeting room, glass onto
    the corridor (x -28) with its door.

    The walnut boardroom table runs down the room toward the big screen on the north wall (the AV credenza under it,
    the video call camera on its bracket beside it), ten leather chairs down its sides, the linear pendant over it.
    On the south wall the walnut slats with the tower's mark and name in brass over the sideboard with the coffee,
    water, biscuits and fruit; on the west wall the whiteboard, acoustic panels and the clock; plants in the corners,
    the coat stand by the door."""
    cells = R.interior(-37, -16, -29, -6)
    walls = {(-28, z): "west" for z in range(-17, -4)}           # (into the room, as its door: glass and door in one plane)
    walls.update({(x, -17): "wall" for x in range(-38, -27)})
    walls.update({(x, -5): "wall" for x in range(-38, -27)})
    R.door(-28, -10, "west", kind="glass", label="meeting_room")
    _shell_keep(R, "meeting", cells, walls)
    floor(R, cells, CARPET)

    def put(name, x, y, z, **props):
        st = ",".join(f"{k}={v}" for k, v in props.items())
        assert (x, z) in cells and R.free(x, y, z), (name, x, y, z, R.get(x, y, z))
        R.put(x, y, z, f"jugcraft:{name}[{st}]")
    # the table down the room, the chairs, the pendant over it
    for zi in range(5):
        for xi in range(2):
            put("boardroom_table", -34 + xi, Y0, -14 + zi, facing="north", part=zi * 2 + xi)
        R.put(-35, Y0, -14 + zi, f"{EXEC_CHAIR}[facing=east,variant={zi % 3}]")
        R.put(-32, Y0, -14 + zi, f"{EXEC_CHAIR}[facing=west,variant={(zi + 1) % 3}]")
        put("pendant_light", -34, Y0 + 2, -14 + zi, facing="north", part=zi)
    # the north wall: the screen, the AV credenza under it, the camera
    U.wall_screen(-33, -16, "south", TOWER)
    for p in range(3):
        R.put(-32 - p, Y0, -16, f"{CREDENZA}[facing=south,part={p}]")
    put("vc_camera", -35, Y0 + 2, -16, facing="south")
    # the south wall: the sideboard and the logo wall over it
    for p in range(3):
        put("refreshment_credenza", -34 + p, Y0, -6, facing="north", part=p)
        put("logo_wall", -34 + p, Y0 + 1, -6, facing="north", half="lower", part=p)
        put("logo_wall", -34 + p, Y0 + 2, -6, facing="north", half="upper", part=p)
    # the west wall: the whiteboard, the acoustic panels, the clock
    for p in range(2):
        put("wall_whiteboard", -37, Y0 + 1, -8 + p, facing="east", part=p)
    for k, z in enumerate((-15, -14, -13, -12)):
        put("acoustic_panel", -37, Y0 + 1, z, facing="east", variant=(k + 1) % 3)
        put("acoustic_panel", -37, Y0 + 2, z, facing="east", variant=(k + 2) % 3)
    R.put(-37, Y0 + 2, -9, f"{CLOCK}[facing=east]")
    R.put(-29, Y0, -7, f"{COAT_STAND}[half=lower]")
    R.put(-29, Y0 + 1, -7, f"{COAT_STAND}[half=upper]")
    for (x, z) in ((-37, -16), (-29, -16), (-37, -6)):
        U.plant(x, z)
    U.f("smoke_detector", -30, Y1, -11, "north")


def map_room(R, U):
    """The map room's furnishing round the Big Map Table (fix 7 of Justin's 26 October list: "add a little decoration
    to the map room"): a large wall screen on each side wall, the airspace (west) and the missions (east), live, with the
    clock over the west one, acoustic panels either side of each; a leather sofa either side of the doors facing the
    map; a plant in each corner. (The room
    itself, its walls, front and table: tower_tripillar.map_room.)"""
    U.wall_screen(-10, -34, "east", AIRSPACE, large=True)
    U.wall_screen(12, -34, "west", MISSIONS, large=True)
    R.put(-10, Y0 + 4, -34, f"{CLOCK}[facing=east]")
    # acoustic panels either side of each screen (the meeting room's), two columns of two
    for x, facing in ((-10, "east"), (12, "west")):
        for k, z in enumerate((-40, -39, -29, -28)):
            for dy in (1, 2):
                R.put(x, Y0 + dy, z, f"jugcraft:acoustic_panel[facing={facing},variant={(k + dy) % 3}]")
    for x0 in (-8, 7):
        for k in range(3):
            R.put(x0 + k, Y0, -23, f"{SOFA}[facing=north,left={'true' if k else 'false'},right={'true' if k < 2 else 'false'}]")
    for (x, z) in ((-10, -44), (12, -44), (-10, -23), (12, -23)):
        U.plant(x, z)


# the gallery's air is filled from these (the upper floor patrol's spots: tools/tripillar_crew.py), at the floor's walking level
GALLERY_SEEDS = [(-20, -20), (20, -20), (16, 28), (-16, 28)]


def gallery_side(b, hex_cells, edge, floor_y, top, pillars, rooms):
    """What the gallery sees of the upper floor's rooms (Justin, 10 November 2026, after the first-floor look-over: "Upper
    floor walls", the sheet's finish with the pillars carrying the line too, B): as tripillar_rooms.hall_side does
    downstairs, every painted room wall that looks onto the gallery or a corridor faced in concrete
    (tools/faced_walls.py: the room behind keeps its paint), the light line three blocks up as on the ground floor; and
    the same line on the pillars' own faces up here, which had none at that height, so it reads as one line round the
    floor. Once everything on the floor stands. Returns what it did."""
    import faced_walls as FW
    inner = set(hex_cells) - set(edge)
    y0 = floor_y + Y0
    air = FW.hall_air(b, [(x, y0, z) for x, z in GALLERY_SEEDS], inner, y0, top)
    RM.rooms_shut(air, rooms, y0)
    line_y = floor_y + RM.LINE_Y
    did = FW.face(b, air, RM.FACE, y0, floor_y + Y1, line_y, edge)
    did["pillar lines"] = FW.line(b, air, pillars, line_y, RM.FACE)
    return did


def upper_floor(b, put, glass, hex_cells, edge, lamp_y, floor_y):
    """The upper floor's rooms into the tower's block dict b; returns the Rooms."""
    R = RM.Rooms(b, put, glass, hex_cells, edge, lamp_y, dy=floor_y)
    U = Upper(R)
    restrooms(R, U)
    briefing(R, U)
    manager(R, U)
    lab(R, U)
    first_aid(R, U)
    # (no flight planning: Justin, 25 Oct 2026, "just get rid of flight planning"; its space is left open)
    simulators(R, U)
    lounge(R, U)
    offices_se(R, U)
    offices_sw(R, U)
    training(R, U)
    meeting(R, U)
    map_room(R, U)
    R.plenum_lights()
    return R
