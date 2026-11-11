"""The Tri-Pillar's crew posts (Justin approved the take-2 crew sheet on 5 November 2026; about 20-30 crew, real mobs with
simple AI; then the new-jobs sheet, ten more in four new jobs): where each of the tower's 37 crew works and what they do
there, written into the layout's "crew" list (tower_tripillar.export) for tower/crew/TowerCrewCare to staff.

A post is one of:
  seat     the crew member sits on the chair at "seat" (a Console Chair, an Operator Chair, a seat furniture block, an
           office chair of the room kits, a Pilot Simulator or any rest seat: a dining chair, a bar stool, a sofa) doing
           "act" (type, mug, sit, pilot); "stand" is the free cell beside the chair they stand on when it is taken. The
           room kits' pieces and the rest seats aren't seats for players, so their seat's point and the way it faces are
           given here ("seat_at", relative to the core, and "seat_yaw")
  station  they stand at one of "spots" (x, y, z, yaw) doing "act" (read, radio, cook, point, idle), moving to another
           spot now and then
  patrol   they walk "spots" in turn carrying "prop", pausing at each
  perimeter  a patrol outside the tower: its spots (x, z) are on the world's ground round the control building, wherever
           that is (the tower doesn't build it); they are checked only to be clear of the tower

Each post has its routine too (routines; the night shift and breaks sheet): "night" for the twelve kept on at night,
else a "sleep" spot on their own floor (a sofa, a cafe chair, the recliner, the lower bunk) with when they go off and get
up; and "breaks" at the nearest cafe, dining or lounge seat free then, on a three-day rota: two or three breaks over
the three days, never two in a day, at other times each day (CYCLE, BREAKS).

Departments (the sheets): flight ops 8, drone pilots 4, security 5, technicians 8 (two of them on their break, one in
the cafe and one at the crew kitchen's island upstairs), engineers 4, a medic, the manager, cafe and facilities 6 (one
of the two servers upstairs). Every spot is checked here: a walkable floor cell (two cells of headroom, three over the raised floor), and
each post's spots reachable from one another without going through a door; "step" is the build step that brings the
post in (0: the foundation)."""
from __future__ import annotations

import json
from collections import defaultdict, deque
from pathlib import Path
import sliding_doors

LOW_FLOORS = ("jugcraft:raised_floor",)
PASSABLE = ("minecraft:light", "minecraft:air")
SEATS = ("jugcraft:console_chair", "jugcraft:operator_chair", "jugcraft:cafe_chair", "jugcraft:task_chair", "jugcraft:exec_chair",
         "jugcraft:training_chair", "jugcraft:pilot_simulator")
# the room kits' chairs: each one's cushion top (model pixels: tools/lab_kit.task_chair, manager_kit.exec_chair,
# training_kit.chair); the sitter's hips a touch back from the middle, towards the chair's back
KIT_SEATS = {"jugcraft:task_chair": 8.2, "jugcraft:exec_chair": 9.2, "jugcraft:training_chair": 9.0}
KIT_BACK = 0.06
# the Pilot Simulator (tools/room_extra_kit.py, 2 x 2, facing north): its cushion's top 9.5/16 up, the pilot's hips
# 1.375 back from the master cell's north edge, in the middle across
SIM, SIM_SEAT = "jugcraft:pilot_simulator", (1.0, 9.5 / 16, 1.375)
FACING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}

N, S, E, W = 180.0, 0.0, -90.0, 90.0      # yaw facing north, south, east, west (the game's degrees)

# (id, job, kind, act, prop, seat, spots)
POSTS = [
    # flight ops: the master console's three chairs, its tablet controller, reception, dispatch, supply, the director's walk
    ("console_director", "flight_director", "seat", "type", None, (0, 1, -11), []),
    ("console_west", "controller", "seat", "type", None, (-4, 1, -10), []),
    ("console_east", "controller", "seat", "type", None, (4, 1, -10), []),
    ("console_tablet", "controller", "station", "read", "tablet", None, [(-2, 1, -11, S), (2, 1, -11, S), (-1, 1, -12, S), (1, 1, -12, S)]),
    ("reception", "controller", "seat", "type", None, (-5, 1, -35), []),
    ("dispatch", "controller", "seat", "type", None, (30, 1, -8), []),
    ("supply", "controller", "seat", "type", None, (30, 1, -2), []),
    ("hall_director", "flight_director", "patrol", "read", "tablet", None, [(0, 1, -22), (-12, 1, -22), (-16, 1, 6), (0, 1, 10), (16, 1, 6), (12, 1, -22)]),
    # security: the alarm console's two chairs, the security desk at reception, the gate guard, the hall patrol
    ("alarm_west", "security", "seat", "type", None, (34, 1, 12), []),
    ("alarm_east", "security", "seat", "type", None, (37, 1, 12), []),
    ("security_desk", "security", "seat", "type", None, (5, 1, -35), []),
    ("gates", "gate_guard", "station", "radio", None, None, [(-6, 1, -38, N), (6, 1, -38, N), (0, 1, -37, N)]),
    # (its third spot two blocks north of where it was, z 20: a bench stands along the locker room's wall there since
    # layout 49, the first-floor look-over)
    ("hall_patrol", "security", "patrol", "idle", None, None, [(-26, 1, -6), (-32, 1, 4), (-26, 1, 18), (-12, 1, 28), (12, 1, 28), (18, 1, 18), (32, 1, 6), (30, 1, -14)]),
    # the security detail (Justin, 6 November 2026: "I want to see a security detail or patrolling outside the perimeter,
    # as well as the top floor of the tier 1 area ... add in a couple more"; reworked 9 November 2026, the crew list's C2,
    # the sheet approved with "make sure one security with a plasma gun is patrolling the first floor, one on the second,
    # and another on the roof"): the two who walked round outside now stand guard either side of the entrance, facing
    # out (their posts keep their ids, so a tower keeps its guards); two guard the energy and storage inputs on the east
    # side, facing out; three walk circuits round the outside at three distances (the middle one the other way round:
    # perimeter posts, their spots on the world's ground, pushed out from the tower till they're clear of it); one
    # patrols the upper floor (the ground floor's hall patrol and the two on the roof as before). They keep the watch day
    # and night, no breaks (standing "idle": armed, they hold the rifle at the low ready, client TowerCrewRenderer)
    ("perimeter_west", "security", "station", "idle", None, None, [(-6, 1, -47, N)]),
    ("perimeter_east", "security", "station", "idle", None, None, [(5, 1, -47, N)]),
    ("inputs_north", "security", "station", "idle", None, None, [(48, 1, -5, E)]),
    ("inputs_south", "security", "station", "idle", None, None, [(48, 1, 5, E)]),
    ("circuit_inner", "security", "perimeter", "idle", None, None, "circuit:5"),
    ("circuit_middle", "security", "perimeter", "idle", None, None, "circuit:-13"),
    ("circuit_outer", "security", "perimeter", "idle", None, None, "circuit:24"),
    ("upper_patrol", "security", "patrol", "idle", None, None, [(-20, 12, -20), (20, 12, -20), (16, 12, 28), (-16, 12, 28)]),
    ("roof_north", "security", "patrol", "idle", None, None, [(-20, 21, -40), (20, 21, -40), (36, 21, -24), (42, 21, -6), (16, 21, -24), (-16, 21, -24), (-42, 21, -6), (-36, 21, -24)]),
    ("roof_south", "security", "patrol", "idle", None, None, [(20, 21, 40), (-20, 21, 40), (-32, 21, 26), (-44, 21, 6), (-14, 21, 30), (14, 21, 30), (44, 21, 6), (32, 21, 26)]),
    # technicians: the build desk, the data hall, the drone service pad, two on their break in the cafe
    ("build_desk", "tech", "seat", "type", None, (-24, 1, -20), []),
    ("data_hall_rounds", "tech", "patrol", "carry", "toolbox", None, [(-47, 1, -4), (-40, 1, -4), (-39, 1, 4), (-47, 1, 4)]),
    ("data_hall_checks", "tech", "station", "read", "tablet", None, [(-44, 1, -4, S), (-43, 1, 4, N), (-48, 1, 3, N)]),
    ("pad_rounds", "mechanic", "patrol", "carry", "toolbox", None, [(20, 1, -35), (30, 1, -35), (30, 1, -25), (20, 1, -25)]),
    ("pad_checks", "mechanic", "station", "read", "clipboard", None, [(25, 1, -26, N), (21, 1, -30, E), (29, 1, -30, W)]),
    # (the second spot was in under the lifted cradle's deck, beside its stand: now off its south west corner)
    ("pad_inspect", "mechanic", "station", "point", None, None, [(25, 1, -34, S), (23, 1, -28, E)]),
    ("cafe_break_a", "mechanic", "seat", "mug", "mug", (-16, 1, 37), []),
    # (Justin, 10 November 2026: "there seems to be a bunch of people in the kitchen down there, but not ... in the kitchen
    # up here"; his pick: "Move staff up from the cafe", one server and one of the two who sit, no new crew. This one now
    # sits with his mug at the crew kitchen's island upstairs, the second bar stool; the post keeps its id, so a tower
    # keeps its technician, who walks up)
    ("cafe_break_b", "tech", "seat", "mug", "mug", (-15, 12, 36), []),
    # drone pilots: two in the simulators, one briefing at the lectern, one listening at his desk
    ("sim_a", "pilot", "seat", "pilot", None, (-27, 12, 9), []),
    ("sim_b", "pilot", "seat", "pilot", None, (-22, 12, 9), []),
    ("briefing_lectern", "pilot", "station", "point", None, None, [(14, 12, -29, E)]),
    # (the back row's chair: the middle chairs' cells beside them are shut in by the next chairs and the riser)
    ("briefing_desk", "pilot", "seat", "sit", None, (18, 12, -26), []),
    # engineers: two at the CAD desks, one at the lab bench, one at the flight test cage
    ("cad_west", "engineer", "seat", "type", None, (32, 12, -7), []),
    ("cad_east", "engineer", "seat", "type", None, (39, 12, -7), []),
    ("lab_bench", "engineer", "station", "bench", None, None, [(30, 12, -16, N), (32, 12, -16, N), (34, 12, -16, N)]),
    ("test_cage", "engineer", "station", "point", None, None, [(43, 12, -4, E), (43, 12, -3, E)]),
    # the medic on rounds in the first aid room, the manager at his desk
    ("first_aid", "medic", "station", "read", "clipboard", None, [(33, 12, 9, W), (35, 12, 9, E), (42, 12, 9, E)]),
    ("manager", "manager", "seat", "type", None, (29, 12, -32), []),
    # cafe and facilities: the cook on the line, two servers, three janitors with their carts
    ("kitchen", "cook", "station", "cook", None, None, [(-22, 1, 35, W), (-22, 1, 38, W), (-22, 1, 41, W)]),
    ("cafe_west", "server", "patrol", "tray", "tray", None, [(-17, 1, 35), (-17, 1, 42), (-7, 1, 42), (-7, 1, 35)]),
    # (the second server waits on the crew lounge's dining tables upstairs since 10 November 2026: round the first row of
    # tables, down the aisle by the island and back between the rows; the post keeps its id, as the technician's above)
    ("cafe_east", "server", "patrol", "tray", "tray", None, [(-13, 12, 34), (-13, 12, 40), (4, 12, 40), (4, 12, 34)]),
    ("janitor_west", "janitor", "patrol", "push", "cart", None, [(-30, 1, -10), (-34, 1, 2), (-28, 1, 16), (-18, 1, 18)]),
    ("janitor_east", "janitor", "patrol", "push", "cart", None, [(30, 1, -12), (34, 1, 2), (28, 1, 16), (18, 1, 18)]),
    ("janitor_north", "janitor", "patrol", "push", "cart", None, [(-14, 1, -31), (14, 1, -32), (12, 1, -41), (-12, 1, -41)]),
]


def name(state):
    return state.split("[")[0] if state else None


def solid(state):
    n = name(state)
    return n is not None and n not in PASSABLE


def walkable(b, x, y, z):
    """A floor cell a crew member can stand in: open (or a raised floor tile) with headroom, on something solid."""
    here = b.get((x, y, z))
    low = name(here) in LOW_FLOORS
    if solid(here) and not low:
        return False
    if solid(b.get((x, y + 1, z))) or (low and solid(b.get((x, y + 2, z)))):
        return False
    return low or solid(b.get((x, y - 1, z)))


def nearest(b, x, y, z, reach=3):
    """The walkable cell nearest (x, y, z) on its floor, or None."""
    best = None
    for dx in range(-reach, reach + 1):
        for dz in range(-reach, reach + 1):
            if walkable(b, x + dx, y, z + dz):
                d = dx * dx + dz * dz
                if best is None or d < best[0]:
                    best = (d, (x + dx, y, z + dz))
    return best[1] if best else None


def clear_of(b, x, z, margin=3):
    """True when no part of the tower stands within margin cells of (x, z), from its floor up."""
    return not any((x + dx, y, z + dz) in b for dx in range(-margin, margin + 1) for dz in range(-margin, margin + 1)
                   for y in range(-3, 30))


def near_cradle(b, x, y, z):
    """True beside a drone stand's lifted service cradle (it reaches out over the cells round its stand)."""
    return any(name(b.get((x + dx, y, z + dz))) == "jugcraft:drone_display_stand" and "style=service" in b[(x + dx, y, z + dz)]
               for dx in (-1, 0, 1) for dz in (-1, 0, 1))


def reachable(b, a, goal, limit=6000):
    """True when goal can be walked to from a on their floor (no doors: they don't count as walkable)."""
    if a == goal:
        return True
    seen, q = {a}, deque([a])
    while q and len(seen) < limit:
        x, y, z = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y, z + dz)
            if n in seen or not walkable(b, *n):
                continue
            if n == goal:
                return True
            seen.add(n)
            q.append(n)
    return False


def facing(state):
    for kv in state.split("[", 1)[1].rstrip("]").split(","):
        k, v = kv.split("=")
        if k == "facing":
            return v
    raise AssertionError(f"no facing in {state}")


def kit_seat(b, seat):
    """A room kit seat's or a rest seat's chair cell, seat point and yaw ((x, y, z) cell, [x, y, z] point, yaw), or None
    for the seats the game knows itself (the Console Chair, the Operator Chair, seat furniture)."""
    state = b.get(seat)
    n = name(state)
    if n in REST and n not in SEATS:
        point, yaw = rest_point(b, seat)
        return seat, point, yaw
    if n in KIT_SEATS:
        fx, fz = FACING[facing(state)]
        point = [seat[0] + 0.5 - fx * KIT_BACK, seat[1] + KIT_SEATS[n] / 16, seat[2] + 0.5 - fz * KIT_BACK]
        return seat, point, YAW[facing(state)]
    if n == SIM:
        assert facing(state) == "north" and "part=0" in state, f"a simulator's master cell, facing north: {state}"
        x, y, z = seat
        point = [x + SIM_SEAT[0], y + SIM_SEAT[1], z + SIM_SEAT[2]]
        cell = (x + 1, y, z + 1)                    # the cell the seat is in (part 3): a second sitter is kept out by it
        assert name(b.get(cell)) == SIM, f"the simulator's seat cell {cell}"
        return cell, point, YAW["north"]
    return None


def stand_cell(b, seat):
    """The cell beside a chair its sitter steps out to: the walkable neighbour behind or beside it that the rest of the
    floor can be walked to from (the first in that order of those that reach the most)."""
    x, y, z = seat
    best = None
    for dx, dz in ((0, -1), (0, 1), (-1, 0), (1, 0), (-1, -1), (1, -1), (-1, 1), (1, 1)):
        c = (x + dx, y, z + dz)
        if walkable(b, *c):
            n = len(paths(b, c))
            if best is None or n > best[0]:
                best = (n, c)
    assert best is not None and best[0] > 50, f"no free cell beside the chair at {seat} the floor can be walked to from"
    return best[1]

# ------------------------------------------------------------------ the routines (Justin's night shift and breaks sheet)
# Approved 5 November 2026: about a third keep the night watch at the key posts; the rest go off shift on their own floor
# (the crew can't climb the riser ladders) to the sofas, the cafe's chairs, the recliner and the crew bunk and sleep; by
# day each takes a break now and then (one a day, a second two days in three, at other times each day of a three-day
# rota) at the nearest cafe, dining or lounge seat.
# The crew open the glass and steel doors, so every walk here goes through them.

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DOORS = ("jugcraft:glass_door", "jugcraft:steel_door")
NIGHT = ("console_west", "console_east", "dispatch", "alarm_west", "security_desk", "gates", "hall_patrol",
         "data_hall_rounds", "pad_rounds", "janitor_north", "sim_a", "first_aid",
         "perimeter_east", "perimeter_west", "roof_north", "roof_south") + ("inputs_north", "inputs_south", "circuit_inner",
         "circuit_middle", "circuit_outer", "upper_patrol")
# on their break all day already (the two with their mugs: one in the cafe, one at the crew kitchen's island upstairs);
# the security detail (on guard day and night, C2)
NO_BREAK = ("cafe_break_a", "cafe_break_b", "perimeter_east", "perimeter_west", "roof_north", "roof_south", "inputs_north", "inputs_south",
            "circuit_inner", "circuit_middle", "circuit_outer", "upper_patrol")
OUTSIDE = ("perimeter",)                             # kinds of post outside the tower (their spots on the world's ground)
# they doze in the cafe's chairs (the server and the technician who moved upstairs sleep up there as the upper floor's
# crew do, on its sofas, recliners and bunk)
CAFE_STAFF = ("kitchen", "cafe_west", "cafe_break_a")
# never away together: the master console, the alarm console, the front desk and the gates, the simulators, the CAD
# desks, the cafe, the pad crew, the data hall, dispatch and supply, the briefing room
APART = [("console_director", "console_west", "console_east"), ("alarm_west", "alarm_east"), ("security_desk", "gates"),
         ("sim_a", "sim_b"), ("cad_west", "cad_east"), ("kitchen", "cafe_west"),
         ("pad_rounds", "pad_checks", "pad_inspect"), ("data_hall_rounds", "data_hall_checks"), ("dispatch", "supply"),
         ("briefing_lectern", "briefing_desk")]
# the manager's office is his: its seats are nobody else's, and he sleeps on its sofa (the end by the door)
OFFICE = {"manager": ((26, 12, -36), (37, 12, -23))}
OFFICE_SOFA = (27, 12, -24)
# the rest furniture: (its cushion's top in its model, the seat's top as the game has it, how far back of the cushion's
# middle the hips go, all px; how they sleep on it: doze sitting up, recline)
REST = {"jugcraft:sofa": (6.5, 0.42 * 16, 1.0, "doze"), "jugcraft:cafe_chair": (7.0, 0.44 * 16, 0.5, "doze"),
        "jugcraft:crew_sofa": (7.6, 7.6, 1.0, "doze"), "jugcraft:leather_sofa": (7.6, 7.6, 1.0, "doze"),
        "jugcraft:recliner": (7.8, 7.8, 1.0, "recline"), "jugcraft:dining_chair": (7.6, 7.6, 0.5, None),
        "jugcraft:bar_stool": (10.8, 10.8, 0.0, None)}
SLEEP_ON = ("jugcraft:sofa", "jugcraft:crew_sofa", "jugcraft:leather_sofa", "jugcraft:recliner")
BUNK = "jugcraft:crew_bunk"
# tools/rest_kit.crew_bunk: the lower bunk's blanket top (px; the upper bunk's folded blanket lies where a sleeper's legs
# would, so it stays made up); the head board's inside 1.4 px into the master cell, the foot board's 14.6 px into the
# other; the mattress 8.1 px in from the bunk's front
BUNK_TOP, BUNK_HEAD, BUNK_FOOT, BUNK_MID = 8.4, 1.4, 14.6, 8.1
LIE_GAP = 0.1                                        # px between a sleeper's back and what they lie on
SCALE = 0.9375                                       # the crew's size (the player's)
# a sleeper lying down (client/TowerCrewModel ACT_LIE): from the crown (model y -8.5, the hair) to the blanket's foot
# (y 21), the hips (y 12) 20.5 px from the crown
LIE_CROWN, LIE_FOOT = 8.5, 21.0
DUSK, DAWN = 12500, 23000                            # off shift 18:30, up 05:00 (ticks of the day, 06:00 = 0)
OFF_EVERY, UP_EVERY = 60, 40                         # ticks between one and the next going off, getting up
BREAK_LO, BREAK_HI = 500, DUSK - 500                 # breaks between 06:30 and 18:00
SIT = 800                                            # ticks a break is sat for (40 s); the walk there and back on top
WALK = 8                                             # ticks a block at the crew's walk
GAP = 2400                                           # ticks at least between one's two breaks
CYCLE = 3                                            # the routine's days: each day of three its own break times
# How many breaks a post takes on each day of the rota, by its place in the list of those who take any (i) and the day.
# Justin, 10 November 2026 (people "sitting down there doing a whole bunch of nothing"; his pick: "Keep sofas, fewer
# breaks", half as many): one a day, and every other post has one day of the three without; 88 breaks over the three
# days where there were 175 (each had one a day and a second on two days of the three), three away at a time where
# there were six.
BREAKS = lambda i, day: 0 if i % 2 == 1 and (i // 2 + day) % CYCLE == 0 else 1
_models = {}


def door_cell(b, x, y, z):
    s = b.get((x, y, z))
    if s is not None and sliding_doors.passable(s):
        return True             # an automatic sliding door's doorway (it opens for them: tower/SlidingDoorBlockEntity)
    return name(s) in DOORS and "half=lower" in s


def paths(b, start):
    """Every floor cell that can be walked to from start on its floor, through doors, and the walk to each (blocks)."""
    d = {start: 0}
    q = deque([start])
    while q:
        p = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (p[0] + dx, p[1], p[2] + dz)
            if n in d or not (walkable(b, *n) or door_cell(b, *n)):
                continue
            d[n] = d[p] + 1
            q.append(n)
    return d


def beside(cell, d):
    """The free cell beside a seat with the shortest walk in d (None: none can be walked to)."""
    best = None
    for dx, dz in ((0, -1), (0, 1), (-1, 0), (1, 0)):
        n = (cell[0] + dx, cell[1], cell[2] + dz)
        if n in d and (best is None or d[n] < d[best]):
            best = n
    return best


def in_box(c, box):
    (x0, y0, z0), (x1, y1, z1) = box
    return x0 <= c[0] <= x1 and y0 <= c[1] <= y1 and z0 <= c[2] <= z1


def _state(state):
    if "[" not in state:
        return state, {}
    n, props = state[:-1].split("[", 1)
    return n, dict(kv.split("=") for kv in props.split(","))


def _block_models(state):
    """The models a block state shows and their turn (blockstates: variants or multipart)."""
    n, props = _state(state)
    bs = json.loads((ASSETS / "blockstates" / f"{n.split(':')[1]}.json").read_text())
    out = []
    if "variants" in bs:
        for key, v in bs["variants"].items():
            want = dict(kv.split("=") for kv in key.split(",")) if key else {}
            if all(props.get(k) == x for k, x in want.items()):
                v = v[0] if isinstance(v, list) else v
                out.append((v["model"], v.get("y", 0)))
                break
    else:
        for part in bs["multipart"]:
            if all(props.get(k) in str(x).split("|") for k, x in part.get("when", {}).items()):
                a = part["apply"][0] if isinstance(part["apply"], list) else part["apply"]
                out.append((a["model"], a.get("y", 0)))
    return out


def _elements(model):
    if model not in _models:
        m = json.loads((ASSETS / "models" / f"{model.split(':')[1]}.json").read_text())
        els = list(m.get("elements", []))
        if not els and m.get("parent", "").startswith("jugcraft:"):
            els = _elements(m["parent"])
        _models[model] = els
    return _models[model]


def _turn(x, z, y):
    return {0: (x, z), 90: (16 - z, x), 180: (16 - x, 16 - z), 270: (z, 16 - x)}[y % 360]


def hips(state, top, back):
    """(dx, dz) px from a seat cell's middle to its sitter's hips: the middle of the cushion (the model's elements whose
    top is top) clear of any armrest, back px further towards the backrest."""
    ms = _block_models(state)
    turn = ms[0][1]
    cushion, tall = [], []
    for model, y in ms:
        assert y == turn, state
        for e in _elements(model):
            if e.get("rotation", {}).get("angle", 0):
                continue
            f, t = e["from"], e["to"]
            if abs(t[1] - top) < 0.15 and (t[0] - f[0]) * (t[2] - f[2]) >= 20:
                cushion.append((f, t))
            elif t[1] > top + 1.5 and f[1] < top + 0.5:
                tall.append((f, t))
    assert cushion, f"no cushion {top} px high in {state}"
    x0, x1 = min(f[0] for f, t in cushion), max(t[0] for f, t in cushion)
    z0, z1 = min(f[2] for f, t in cushion), max(t[2] for f, t in cushion)
    for f, t in tall:
        if t[2] - f[2] > 6 and t[0] - f[0] < 5:      # an armrest: front to back at one side
            if f[0] + t[0] < 16:
                x0 = max(x0, t[0])
            else:
                x1 = min(x1, f[0])
    wx, wz = _turn((x0 + x1) / 2, (z0 + z1) / 2 + back, turn)
    return wx - 8, wz - 8


def rest_point(b, cell):
    """Where a sitter's hips go on a rest seat (point) and the way they face (yaw)."""
    s = b[cell]
    top, height, back, _ = REST[name(s)]
    x, y, z = cell
    if "facing=" in s:
        dx, dz = hips(s, top, back)
        yaw = YAW[facing(s)]
    else:                                            # a bar stool: facing the kitchen island, west of the stools
        dx, dz, yaw = 0.0, 0.0, W
    return [round(x + 0.5 + dx / 16, 4), round(y + height / 16, 4), round(z + 0.5 + dz / 16, 4)], yaw


def bunk_point(b, cell):
    """The lower bunk's sleeper (cell: the bunk's master): the hips' back on the blanket, the head on the pillow at the
    master cell's end, the body centred between the boards; the yaw the way the feet point."""
    fx, fz = FACING[facing(b[cell])]
    rx, rz = -fz, fx                                 # along the bunk, towards its other cell
    x, y, z = cell
    hx, hz = x + 0.5 - rx * (0.5 - BUNK_HEAD / 16), z + 0.5 - rz * (0.5 - BUNK_HEAD / 16)
    inner = (16 - BUNK_HEAD + BUNK_FOOT) / 16
    length = (LIE_CROWN + LIE_FOOT) * SCALE / 16
    along = (inner - length) / 2 + (LIE_CROWN + 12) * SCALE / 16
    across = 0.5 - BUNK_MID / 16                     # from the cell's middle towards its front
    point = [round(hx + rx * along + fx * across, 4), round(y + (BUNK_TOP + LIE_GAP) / 16, 4), round(hz + rz * along + fz * across, 4)]
    return point, {(1, 0): E, (-1, 0): W, (0, 1): S, (0, -1): N}[(rx, rz)]


def schedule(b, takers, d, seats):
    """Each post's breaks over the routine's three days (CYCLE; Justin, 6 November 2026: a "schedule over like 3 days"):
    as many each day as BREAKS says (none, one or two), at times of their own each day (so nobody's day is the same as
    the day before); never with an APART partner (a group's members a share of the day apart), as few away at once as
    can be; then a seat for each break, the nearest one nobody else is on then (the manager's office his alone).
    Each break says its day ("days": 0, 1 or 2, the game's day count over three)."""
    est = {}
    for pid in takers:
        walks = [d[pid][st] for n, c in seats for st in [beside(c, d[pid])] if st is not None]
        est[pid] = SIT + 2 * WALK * min(walks) + 100
    order = sorted(takers)
    out = defaultdict(list)
    for day in range(CYCLE):
        today = [p for i, p in enumerate(order) if BREAKS(i, day) > 0]
        seconds = {p for i, p in enumerate(order) if BREAKS(i, day) > 1}
        times = plan_day(today, est, day, seconds)
        booked = defaultdict(list)
        mine = defaultdict(list)
        for t0, pid in sorted((t0, p) for p in today for t0 in times[p]):
            pick = None
            for n, c in seats:
                if any(in_box(c, box) for box in OFFICE.values()) != (pid in OFFICE):
                    continue
                if any(a < t0 + est[pid] + 400 and t0 < e for a, e in booked[c]):
                    continue
                st = beside(c, d[pid])
                if st is not None and (pick is None or d[pid][st] < pick[2]):
                    pick = (c, st, d[pid][st])
            assert pick, f"{pid}: no seat for a break at {t0} on day {day}"
            c, st, walk = pick
            end = t0 + SIT + 2 * WALK * walk
            booked[c].append((t0, end))
            at, yaw = rest_point(b, c)
            mine[pid].append({"from": t0, "to": end, "seat": list(c), "seat_at": at, "seat_yaw": yaw, "stand": list(st), "days": day})
        for g in APART:
            for i, p in enumerate(g):
                for q in g[i + 1:]:
                    for x in mine.get(p, []):
                        for y in mine.get(q, []):
                            assert not (x["from"] < y["to"] and y["from"] < x["to"]), f"{p} and {q} away together on day {day}"
        for pid, brks in mine.items():
            out[pid] += sorted(brks, key=lambda r: r["from"])
    return out


def plan_day(takers, est, day, seconds):
    """The break start times of one day of the three (pid -> one or two times, the second for those in seconds): the
    APART groups first, spread through the day; then everyone else where the fewest are away, and, of the times as
    good as that, the one nearer a point of each post's own that moves round the day from one day to the next (so the
    days differ)."""
    import numpy as np
    half = (BREAK_HI - BREAK_LO) // 2
    step = 25
    span = BREAK_HI - BREAK_LO
    load = np.zeros(DUSK // step + 200)

    def aim(pid):
        return BREAK_LO + (sum(map(ord, pid)) * 97 + day * span // CYCLE) % span

    def add(lo, pid, t1, t2):
        lo[t1 // step:(t1 + est[pid]) // step + 1] += 1
        if t2 is not None:
            lo[t2 // step:(t2 + est[pid]) // step + 1] += 1

    times = {}
    units = [[p for p in g if p in takers] for g in APART]
    grouped = {p for u in units for p in u}
    units = [u for u in units if u] + [[p] for p in sorted(takers) if p not in grouped]
    units.sort(key=lambda u: (-len(u), -max(est[p] for p in u)))
    for unit in units:
        best = None
        if len(unit) > 1:
            g = len(unit)
            for off in range(0, span - (g - 1) * (half // g), step):
                trial, plan = load.copy(), []
                for j, pid in enumerate(unit):
                    t1 = BREAK_LO + off + j * (half // g)
                    t1 -= t1 % step
                    t2 = t1 + half if pid in seconds else None
                    if (t2 if t2 is not None else t1) + est[pid] > BREAK_HI:
                        plan = None
                        break
                    add(trial, pid, t1, t2)
                    plan.append((pid, t1, t2))
                if plan is None:
                    continue
                score = (trial.max(), float((trial ** 2).sum()) + 0.1 * abs(plan[0][1] - aim(unit[0])) / step)
                if best is None or score < best[0]:
                    best = (score, plan, trial)
        else:
            pid = unit[0]
            for t1 in range(BREAK_LO, BREAK_HI - est[pid] - (GAP if pid in seconds else 0), step):
                for t2 in (range(t1 + GAP, BREAK_HI - est[pid], 2 * step) if pid in seconds else [None]):
                    trial = load.copy()
                    add(trial, pid, t1, t2)
                    score = (trial.max(), float((trial ** 2).sum()) + 0.1 * abs(t1 - aim(pid)) / step)
                    if best is None or score < best[0]:
                        best = (score, [(pid, t1, t2)], trial)
        load = best[2]
        for pid, t1, t2 in best[1]:
            times[pid] = [t1] if t2 is None else [t1, t2]
    return times


def routines(b, homes):
    """Each post's routine (homes: post id -> the cell they start from): "night" (kept on at night) or a "sleep" spot (a
    seat or the bunk: its cell, the point and yaw they settle at, how they sleep, the cell beside it to walk to, when
    they go off and get up); and "breaks" (each: from, to, the seat as a sleep spot's, its day of the rota)."""
    rest = defaultdict(list)
    for p, s in b.items():
        n = name(s)
        if n in REST or n == BUNK and "part=0" in s and "half=lower" in s:
            rest[n].append(p)
    d = {pid: paths(b, h) for pid, h in homes.items()}
    post_seats = {tuple(p[5]) for p in POSTS if p[5]}
    spots = [(n, c) for n, cells in rest.items() for c in cells if n in SLEEP_ON or n in (BUNK, "jugcraft:cafe_chair")]
    sleepers = [p[0] for p in POSTS if p[0] not in NIGHT]
    pairs = []
    for pid in sleepers:
        own = next(p[5] for p in POSTS if p[0] == pid)
        for n, c in spots:
            if pid in OFFICE or any(in_box(c, box) for box in OFFICE.values()):
                continue
            if (n == "jugcraft:cafe_chair") != (pid in CAFE_STAFF) or n == "jugcraft:cafe_chair" and c in post_seats and c != own:
                continue
            st = beside(c, d[pid])
            if st is None:
                continue
            # a bed or the recliner is worth a longer walk; their own chair first
            bonus = -1000 if c == own else {BUNK: -40, "jugcraft:recliner": -25}.get(n, 0)
            pairs.append((d[pid][st] + bonus, pid, n, c, st))
    for pid in OFFICE:
        pairs.append((-2000, pid, "jugcraft:leather_sofa", OFFICE_SOFA, beside(OFFICE_SOFA, d[pid])))
    pairs.sort()
    sleep, used = {}, set()
    for _, pid, n, c, st in pairs:
        if pid in sleep or c in used:
            continue
        if n == BUNK:
            at, yaw = bunk_point(b, c)
            act = "lie"
        else:
            at, yaw = rest_point(b, c)
            act = REST[n][3]
        sleep[pid] = {"seat": list(c), "seat_at": at, "seat_yaw": yaw, "act": act, "stand": list(st), "walk": d[pid][st]}
        used.add(c)
    assert set(sleep) == set(sleepers), set(sleepers) - set(sleep)
    # off shift a few at a time, the furthest to walk last; up the other way round
    for i, pid in enumerate(sorted(sleep, key=lambda p: (sleep[p]["walk"], p))):
        sleep[pid]["from"] = DUSK + i * OFF_EVERY
        sleep[pid]["to"] = DAWN + (len(sleep) - 1 - i) * UP_EVERY
        del sleep[pid]["walk"]
    seats = [(n, c) for n, cells in rest.items() if n in REST for c in cells if c not in post_seats]
    breaks = schedule(b, [p[0] for p in POSTS if p[0] not in NO_BREAK], d, seats)
    out = {}
    for p in POSTS:
        pid = p[0]
        out[pid] = {"night": pid in NIGHT}
        if pid in sleep:
            out[pid]["sleep"] = sleep[pid]
        if pid in breaks:
            out[pid]["breaks"] = breaks[pid]
    return out


# the janitors' carts in the custodian's closet upstairs (tripillar_rooms_upper.restrooms: three, side by side) and
# the cell south of each they stand in to take it (C5, Justin, 9 November 2026: "they should get carts from the
# janitor room")
CARTS = {"janitor_west": ((-22, 12, -42), (-22, 12, -41)), "janitor_east": ((-20, 12, -42), (-20, 12, -41)),
         "janitor_north": ((-18, 12, -42), (-18, 12, -41))}


def circuit(b, off):
    """An outside circuit `off` blocks out from the control building's six walls (negative: the other way round): its
    corners and mid-sides, each pushed straight out from the middle till it's clear of the tower (3 blocks)."""
    k = (52 + abs(off)) / 52
    corners = [(-26, -45), (26, -45), (52, 0), (26, 45), (-26, 45), (-52, 0)]
    pts = []
    for i in range(6):
        a, c = corners[i], corners[(i + 1) % 6]
        for x, z in (a, ((a[0] + c[0]) / 2, (a[1] + c[1]) / 2)):
            px, pz = x * k, z * k
            r = (px * px + pz * pz) ** 0.5
            while not clear_of(b, round(px), round(pz)):
                px, pz = px * (r + 1) / r, pz * (r + 1) / r
                r += 1
            pts.append((round(px), 1, round(pz)))
    return pts[::-1] if off < 0 else pts


def posts(b):
    """The crew posts as the layout's "crew" list (b: the finished tower's blocks), every spot checked, each with its
    routine (routines)."""
    out, homes = [], {}
    for pid, job, kind, act, prop, seat, spots in POSTS:
        entry = {"id": pid, "job": job, "kind": kind, "act": act, "prop": prop}
        if kind == "seat":
            assert name(b.get(seat)) in SEATS or name(b.get(seat)) in REST, f"{pid}: no chair at {seat} ({b.get(seat)})"
            kit = kit_seat(b, seat)
            if kit is not None:
                seat, point, yaw = kit
                entry["seat_at"] = [round(v, 4) for v in point]
                entry["seat_yaw"] = yaw
            entry["seat"] = list(seat)
            entry["stand"] = list(stand_cell(b, seat))
            entry["spots"] = []
            homes[pid] = tuple(entry["stand"])
        elif kind in OUTSIDE:
            # on the world's ground round the tower: only checked to be well clear of it (a circuit's spots worked out
            # here, pushed out till they are)
            if isinstance(spots, str):
                spots = circuit(b, int(spots.split(":")[1]))
            for s in spots:
                assert clear_of(b, s[0], s[2]), f"{pid}: {s} is too near the tower"
            entry["spots"] = [list(s[:3]) for s in spots]
        else:
            cells = []
            for s in spots:
                cell = tuple(s[:3])
                assert walkable(b, *cell), f"{pid}: {cell} is not a free floor cell"
                assert not near_cradle(b, *cell), f"{pid}: {cell} is in under a lifted cradle"
                cells.append(cell)
            for c in cells[1:]:
                assert reachable(b, cells[0], c), f"{pid}: {c} can't be walked to from {cells[0]}"
            entry["spots"] = [list(s[:3]) + ([s[3]] if len(s) > 3 else []) for s in spots]
            homes[pid] = cells[0]
        if pid in CARTS:
            # a janitor's cart, parked in the custodian's closet upstairs: its cell (the cart's first part) and where they
            # stand to take it and park it (C5)
            cart, stand = CARTS[pid]
            assert name(b.get(cart)) == "jugcraft:janitor_cart" and "part=0" in b[cart], f"{pid}: no cart at {cart} ({b.get(cart)})"
            assert walkable(b, *stand), f"{pid}: {stand} (by the cart) is not a free floor cell"
            entry["cart"] = list(cart)
            entry["cart_stand"] = list(stand)
        out.append(entry)
    assert len({p["id"] for p in out}) == len(out)
    routine = routines(b, homes)
    for entry in out:
        entry.update(routine[entry["id"]])
    return out


def steps(build):
    """The build step each post comes in with (build(step): the tower's blocks at that step)."""
    found = {}
    for step in range(0, 10):
        b = build(step)
        for pid, job, kind, act, prop, seat, spots in POSTS:
            if pid in found:
                continue
            if kind in OUTSIDE:
                found[pid] = 0                       # round the control building, there with the foundation
                continue
            cells = [seat] if seat else [tuple(s[:3]) for s in spots]
            if all((name(b.get(c)) in SEATS or name(b.get(c)) in REST) if seat else walkable(b, *c) for c in cells):
                found[pid] = step
        if len(found) == len(POSTS):
            break
    return found
