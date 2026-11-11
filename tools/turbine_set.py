"""The main turbine-generator and its moisture separator reheaters (turbine hall part 1; Justin, 2026-10-18: "Turbine
hall (Recommended)", the turbine "Local trip + turning gear"), drawn from their own parts the way tools/diesel_set.py
draws the diesel generator set: operations replayed by client/ControlBoards.java in the equipment sheet's materials,
every nameplate, sign and gauge face on the turbine's own legend texture (textures/entity/turbine_legends.png).

A Westinghouse four-loop plant's machine (the PWR technology manual, 7.3): a tandem-compound six-flow unit at 1,800
rpm, one double-flow high-pressure cylinder and three double-flow low-pressure cylinders rigidly coupled to the
generator, its brushless exciter on the end. Steam comes in through two steam chests, one each side of the HP, each
with two throttle (stop) valves and two governor valves; the HP exhausts down through the cross-unders to the moisture
separator reheaters beside the LPs, and the crossovers bring the reheated steam back over to six combined reheat stop
and intercept valves, two on each LP. The manual trip is at the front standard (the governor pedestal); the turning
gear sits on the pedestal between the last LP and the generator.

The turbine-generator (11 wide, 50 deep, 10 tall; frame as the boards': x to the right, y up from the operating
floor, z away from whoever placed it), its front standard toward them, the shaft along the middle at (AX, AY):

- the front standard: the pedestal and its hood, the local gauge board (SPEED digits, bearing oil, auto stop oil and
  EHC fluid gauges, TRIPPED and LATCHED lamps), the manual trip handle under its yellow guard, oil lines into the floor;
- the HP cylinder's lagging and its horizontal joint with the stud nuts, the two steam chests with their throttle
  and governor valves (bodies, yokes, EHC actuators, the stems and their position pointers the game's), the main steam
  leads up out of the floor into them and the governor valves' leads into the casing;
- three LP cylinders: the exhaust hoods with their ribs, the inner casing's cover, the atmospheric relief diaphragms,
  each LP's two combined intermediate valves on its shoulders (their actuators' stems the game's) and the crossover
  legs from them into the casing's inlets;
- the bearing pedestals between the cylinders, their caps, oil drains and thermocouple conduits;
- the turning gear on its pedestal: the gear case, the motor and its fan cowl, the engage lever (the game's), its
  ENGAGED and MOTOR RUNNING lamps;
- the generator: the frame and end shields, its bands, feet, the hydrogen coolers on top and their water lines, the
  line terminal box with the isolated phase bus down into the floor, the nameplate and the hydrogen warning;
- the brushless exciter's housing, its louvres and the window onto the diode wheel (turning: the game's).

The moisture separator reheater (5 wide, 20 deep, 5 tall) stands on the floor beside the LPs, its three crossovers
over to the turbine's intercept valves on its right (so the one on the far side is placed facing the other way)."""
import contextlib
import math

import board_live as L
import control_boards as C
import hvac_boards as H
from control_boards import sw, _r
from diesel_set import rev, run, joint, bolts, on_floor, plate, labelled

W, D, T = 11, 50, 10
AX, AY = 5.5, 1.5               # the shaft line, along z
FS = (0.3, 2.35)                # the front standard
HP = (2.75, 9.0)                # the HP cylinder
HP_R = 1.75
BEARINGS = [(9.0, 9.9), (18.1, 18.9), (27.1, 27.9)]
LPS = [(9.9, 18.1), (18.9, 27.1), (27.9, 36.1)]
TG = (36.1, 38.1)               # the turning gear's pedestal
GEN = (38.1, 46.6)
GEN_R = 2.15
EXC = (46.75, 49.75)
HOOD = (0.35, 10.65, 3.3)       # an LP's exhaust hood: x across, its top
HUMP_C, HUMP_R = 3.0, 2.6       # the inner casing's cover: its middle's height, its radius
CIV_X = (1.5, 9.5)              # the combined intermediate valves, left and right
CIV_R = 0.5
CIV_IN = 4.5                    # the crossover's height where it comes into a CIV's outer side
XO_R = 0.5                      # the crossovers' radius
INLET_X = (3.9, 7.1)            # the LP's crossover inlets
CHESTS = ((0.75, 2.35), (8.65, 10.25))
CHEST_Z = (3.55, 8.25)
TV_Z = (4.1, 7.7)               # the throttle valves along each chest
GV_Z = (5.3, 6.5)               # the governor valves
MS_X = (0.35, 10.65)            # the main steam leads up out of the floor beside each chest

LAG, LP_PAINT, GEN_PAINT = "turbine_lagging", "lp_hood", "gen_frame"
DARK, MID, BLACK, WHITE = sw("dgrey"), sw("mgrey"), sw("black"), sw("white")
STEEL, GUN = "carbon_steel", "gunmetal"
ACT = "gunmetal"                # the EHC actuators
PANEL = "paint_grey"            # the local panels
YELLOW = "paint_yellow"


def fl(b, at, axis, r, t=0.05, mat="flange"):
    """A pipe's flange: a ring a little proud of the pipe, t thick, across axis, its bolt heads round it."""
    x, y, z = at
    d = {"x": (t / 2, 0, 0), "y": (0, t / 2, 0), "z": (0, 0, t / 2)}[axis]
    b.cyl((x - d[0], y - d[1], z - d[2]), (x + d[0], y + d[1], z + d[2]), r + 0.09, mat, sw("mgrey"), 18)
    n = max(8, int(r * 24))
    for k in range(n):
        a = 2 * math.pi * (k + 0.5) / n
        ca, sa = math.cos(a) * (r + 0.05), math.sin(a) * (r + 0.05)
        if axis == "y":
            p = (x + ca, y + t / 2, z + sa)
            q = (p[0], p[1] + 0.03, p[2])
        elif axis == "x":
            p = (x + t / 2, y + ca, z + sa)
            q = (p[0] + 0.03, p[1], p[2])
        else:
            p = (x + ca, y + sa, z + t / 2)
            q = (p[0], p[1], p[2] + 0.03)
        b.cyl(p, q, 0.022, MID, MID, 6)

# The turbine's own legend texture: its plates, signs and gauge faces.
ATLAS = L.Atlas()


@contextlib.contextmanager
def own_atlas():
    keep = L.ATLAS
    L.ATLAS = ATLAS
    try:
        yield
    finally:
        L.ATLAS = keep


def lp_mid(k):
    z0, z1 = LPS[k]
    return (z0 + z1) / 2


def half_cyl(b, cx, cy, z0, z1, r, mat, end=None, sides=24, lst=None):
    """The upper half of a cylinder along z (centre cx, cy), its two half-disc ends in end (None: open)."""
    pts = [(cx + r * math.cos(math.pi * k / sides), cy + r * math.sin(math.pi * k / sides)) for k in range(sides + 1)]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        b.quad((x1, y1, z1), (x1, y1, z0), (x0, y0, z0), (x0, y0, z1), mat, lst)
    if end is not None:
        for k in range(0, sides, 2):
            p0, p1, p2 = pts[k], pts[k + 1], pts[min(k + 2, sides)]
            b.quad((cx, cy, z0), (p0[0], p0[1], z0), (p1[0], p1[1], z0), (p2[0], p2[1], z0), end, lst)
            b.quad((cx, cy, z1), (p2[0], p2[1], z1), (p1[0], p1[1], z1), (p0[0], p0[1], z1), end, lst)


def front_plate(b, zf, x0, x1, y0, y1, mat, edge=WHITE, d=0.008):
    b.box(x0, y0, zf - d, x1, y1, zf, [mat, None, edge, edge, edge, edge])


def back_plate(b, zf, x0, x1, y0, y1, mat, edge=WHITE, d=0.008):
    b.box(x0, y0, zf, x1, y1, zf + d, [None, mat, edge, edge, edge, edge])


def side_plate(b, side, xf, z0, z1, y0, y1, mat, edge=WHITE, d=0.008):
    H.side_box(b, side, xf, d, y0, y1, z0, z1, mat, edge)


def gauge_z(b, live, name, c, r, face):
    """A round gauge facing -z: its black case, the dial behind the glass, the needle the game's."""
    x, y, z = c
    b.cyl((x, y, z), (x, y, z - 0.05), r, BLACK, BLACK, 14)
    b.cyl((x, y, z - 0.05), (x, y, z - 0.058), r * 0.86, DARK, DARK, 14)
    s = r * 0.68
    zf = z - 0.062
    b.quad((x - s, y + s, zf), (x - s, y - s, zf), (x + s, y - s, zf), (x + s, y + s, zf), face)
    live["gauges"].append({"name": name, "c": _r(x, y, zf - 0.003), "len": _r(s * 0.82)[0], "n": [0, 0, -1]})


def gauge_x(b, live, name, c, r, face, side):
    """A round gauge on a side (side -1 facing -x)."""
    x, y, z = c
    xo = x + side * 0.05
    b.cyl((x, y, z), (xo, y, z), r, BLACK, BLACK, 14)
    b.cyl((xo, y, z), (xo + side * 0.008, y, z), r * 0.86, DARK, DARK, 14)
    s = r * 0.68
    xf = xo + side * 0.012
    if side < 0:
        b.quad((xf, y + s, z + s), (xf, y - s, z + s), (xf, y - s, z - s), (xf, y + s, z - s), face)
    else:
        b.quad((xf, y + s, z - s), (xf, y - s, z - s), (xf, y - s, z + s), (xf, y + s, z + s), face)
    live["gauges"].append({"name": name, "c": _r(xf + side * 0.003, y, z), "len": _r(s * 0.82)[0], "n": [side, 0, 0]})


def lamp_z(b, live, name, x, y, z, colour, r=0.045):
    b.cyl((x, y, z), (x, y, z - 0.03), r + 0.02, "stainless", "stainless", 12)
    b.cyl((x, y, z - 0.03), (x, y, z - 0.04), r, BLACK, BLACK, 12)
    live["lamps"].append({"name": name, "c": _r(x, y, z - 0.042), "r": _r(r * 0.85)[0], "n": [0, 0, -1], "colour": colour,
                          "on": C.LENS[colour], "off": C.LENS["off_" + colour]})


def lamp_x(b, live, name, x, y, z, colour, side, r=0.045):
    xo = x + side * 0.03
    b.cyl((x, y, z), (xo, y, z), r + 0.02, "stainless", "stainless", 12)
    b.cyl((xo, y, z), (xo + side * 0.01, y, z), r, BLACK, BLACK, 12)
    live["lamps"].append({"name": name, "c": _r(xo + side * 0.012, y, z), "r": _r(r * 0.85)[0], "n": [side, 0, 0], "colour": colour,
                          "on": C.LENS[colour], "off": C.LENS["off_" + colour]})


def ring(b, cx, cy, z, r, w, mat, sides=24):
    """A band round something along z (a lagging seam, a frame band)."""
    rev(b, (cx, cy, z - w / 2), (cx, cy, z + w / 2), [(0, r), (w, r)], mat, None, None, sides)


# ------------------------------------------------------------------------------------------------ the front standard

def front_standard(b, live):
    z0, z1 = FS
    # The pedestal on its sole plate, its hood (the main oil pump and overspeed trip inside).
    b.box(3.85, 0.0, z0 - 0.05, 7.15, 0.08, z1 + 0.05, STEEL)
    b.box(4.0, 0.08, z0, 7.0, 1.75, z1, LAG)
    half_cyl(b, AX, 1.75, z0, z1, 1.5, LAG, LAG, 20)
    b.box(3.95, 1.7, z0 - 0.03, 7.05, 1.8, z1 + 0.03, GUN)          # the hood's joint
    for z in (z0 + 0.25, (z0 + z1) / 2, z1 - 0.25):
        for x in (3.97, 7.03):
            b.cyl((x, 1.8, z), (x, 1.86, z), 0.03, MID, MID, 6)
    # The local gauge board on its front: SPEED, the three oil gauges, the two lamps.
    zf = z0
    b.box(4.15, 0.95, zf - 0.04, 6.85, 2.05, zf, [PANEL, None, DARK, DARK, DARK, DARK])
    front_plate(b, zf - 0.04, 4.3, 6.7, 1.9, 2.0, plate("MAIN TURBINE  -  FRONT STANDARD", 2.4, 0.1, "black"))
    # The speed indicator: its digits the game's.
    b.box(4.35, 1.6, zf - 0.07, 5.25, 1.85, zf - 0.04, BLACK)
    live["digits"] = [{"name": "speed", "c": _r(4.8, 1.725, zf - 0.072), "w": 0.8, "h": 0.17}]
    front_plate(b, zf - 0.04, 4.35, 5.25, 1.53, 1.59, plate("TURBINE SPEED  RPM", 0.9, 0.06, "white"), d=0.006)
    for k, (name, unit, lo, hi, majors) in enumerate((("BRG OIL", "PSIG", 0, 60, 6), ("AUTO STOP OIL", "PSIG", 0, 150, 4),
                                                       ("EHC FLUID", "PSIG", 0, 2000, 5))):
        gx = 4.55 + k * 0.8
        face = L.dial_face_tex(unit, lo, hi, majors, 0.15 * 1.44, 270)
        gauge_z(b, live, name, (gx, 1.25, zf - 0.04), 0.15, face)
        front_plate(b, zf - 0.04, gx - 0.3, gx + 0.3, 1.0, 1.07, plate(name, 0.6, 0.07, "white"), d=0.006)
    for k, (name, legend, colour) in enumerate((("tripped", "TRIPPED", "red"), ("latched", "LATCHED", "green"))):
        lx = 5.55 + k * 0.62
        lamp_z(b, live, name, lx, 1.72, zf - 0.04, colour)
        front_plate(b, zf - 0.04, lx + 0.08, lx + 0.5, 1.67, 1.77, plate(legend, 0.42, 0.1, "white"), d=0.006)
    # The manual trip under its guard: the handle's housing (the handle and the guard the game's), its legend.
    hx, hy = 5.5, 0.55
    b.box(hx - 0.32, hy - 0.22, zf - 0.08, hx + 0.32, hy + 0.22, zf, [sw("red"), None, sw("red"), sw("red"), sw("red"), sw("red")])
    b.box(hx - 0.24, hy - 0.14, zf - 0.081, hx + 0.24, hy + 0.14, zf - 0.08, BLACK)
    live["trip"] = {"c": _r(hx, hy, zf - 0.081), "guard": _r(hx - 0.3, hy + 0.2, zf - 0.09), "w": 0.6, "h": 0.4,
                    "mats": {"handle": sw("red"), "rod": "stainless", "guard": YELLOW, "hinge": DARK}}
    front_plate(b, zf, hx - 0.55, hx + 0.55, hy - 0.42, hy - 0.26, plate("MANUAL TRIP|LIFT GUARD - PULL HANDLE", 1.1, 0.16, "red"), d=0.006)
    # The oil lines out of its sides into the floor (bearing oil in, drain out, auto stop oil).
    for side, x in ((-1, 4.0), (1, 7.0)):
        for k, (r, z, mat) in enumerate(((0.14, z0 + 0.5, "pipe_brown"), (0.2, z0 + 1.2, "pipe_brown"), (0.06, z1 - 0.3, "pipe_brown"))):
            xo = x + side * (r + 0.12)
            run(b, [(x, 1.0 - k * 0.2, z), (xo, 1.0 - k * 0.2, z), (xo, -0.05, z)], r, mat, joints=DARK, sides=10)
            b.cyl((xo, 0.0, z), (xo, 0.03, z), r + 0.05, MID, MID, 10)
            fl(b, (x + side * 0.03, 1.0 - k * 0.2, z), "x", r, 0.04)
    # The shaft between the front standard and the HP (its turning mark the game's).
    b.cyl((AX, AY, z1), (AX, AY, HP[0]), 0.22, "stainless_dark", None, 14)


# ------------------------------------------------------------------------------------------------ the HP cylinder

def hp_cylinder(b):
    z0, z1 = HP
    rev(b, (AX, AY, z0), (AX, AY, z1), [(0, 1.2), (0.45, HP_R), (z1 - z0 - 0.45, HP_R), (z1 - z0, 1.2)], LAG, LAG, LAG, 32)


def hp_detail(b, live):
    z0, z1 = HP
    # The lagging's seams; the horizontal joint's flange along each side, its stud nuts.
    for z in (z0 + 0.45, z0 + 1.7, z0 + 3.1, z1 - 1.7, z1 - 0.45):
        ring(b, AX, AY, z, HP_R + 0.012, 0.04, sw("mgrey"), 32)
    for side in (-1, 1):
        xf = AX + side * HP_R
        x0, x1 = (xf - 0.18, xf + 0.02) if side < 0 else (xf - 0.02, xf + 0.18)
        b.box(x0, AY - 0.07, z0 + 0.5, x1, AY + 0.07, z1 - 0.5, GUN)
        z = z0 + 0.65
        while z < z1 - 0.55:
            xs = xf + side * 0.1
            b.cyl((xs, AY + 0.07, z), (xs, AY + 0.15, z), 0.04, MID, MID, 6)
            z += 0.32
    # The nameplate on the front end, the lifting lugs on top.
    b.box(AX - 0.5, AY + 0.55, z0 + 0.08, AX + 0.5, AY + 0.95, z0 + 0.1, [None, None, WHITE, WHITE, WHITE, WHITE])
    front_plate(b, z0 + 0.1 + 0.15, AX - 0.5, AX + 0.5, AY + 0.55, AY + 0.95, plate("HIGH PRESSURE|TURBINE", 1.0, 0.4, "white"), d=0.006)
    for z in (z0 + 1.0, z1 - 1.0):
        b.box(AX - 0.08, AY + HP_R - 0.05, z - 0.2, AX + 0.08, AY + HP_R + 0.25, z + 0.2, GUN)
        b.cyl((AX - 0.09, AY + HP_R + 0.12, z), (AX + 0.09, AY + HP_R + 0.12, z), 0.06, BLACK, BLACK, 8)
    # The steam chests.
    for side, (x0, x1) in zip((-1, 1), CHESTS):
        steam_chest(b, live, side, x0, x1)


def steam_chest(b, live, side, x0, x1):
    """A steam chest beside the HP: its lagged body, two throttle valves and two governor valves on top (lagged bodies,
    bonnet flanges and studs, yokes, EHC actuators; the stems and their pointers the game's), the main steam leads up
    out of the floor into its outer side under the throttle valves, the governor valves' leads over into the casing."""
    cz0, cz1 = CHEST_Z
    cx = (x0 + x1) / 2
    b.box(x0, 0.0, cz0, x1, 1.05, cz1, LAG)
    b.box(x0 - 0.03, 1.05, cz0 - 0.03, x1 + 0.03, 1.12, cz1 + 0.03, GUN)
    for z in (cz0 + 1.2, cz1 - 1.2):
        b.box(x0 - 0.012, 0.0, z - 0.02, x1 + 0.012, 1.04, z + 0.02, sw("mgrey"))
    k0 = 2 * (side > 0)
    for k, z in enumerate(TV_Z):
        valve_stack(b, live, cx, z, side, 0.5, 0.24, f"tv{'LR'[side > 0]}{k}", f"TV-{k0 + k + 1}")
    for k, z in enumerate(GV_Z):
        valve_stack(b, live, cx, z, side, 0.4, 0.2, f"gv{'LR'[side > 0]}{k}", f"GV-{k0 + k + 1}")
    # The main steam leads up out of the floor into the chest's outer side, under the throttle valves.
    xo = MS_X[0] if side < 0 else MS_X[1]
    xf = x0 if side < 0 else x1
    for z in TV_Z:
        run(b, [(xo, -0.1, z), (xo, 0.55, z), (xf, 0.55, z)], 0.3, "insulation", joints="insulation", sides=14)
        b.cyl((xo, 0.0, z), (xo, 0.04, z), 0.38, sw("mgrey"), sw("mgrey"), 14)
        fl(b, (xf + (-0.03 if side < 0 else 0.03), 0.55, z), "x", 0.3, 0.05)
    # The governor valves' leads over into the casing (below its joint).
    xi = x1 if side < 0 else x0
    s = -1 if side < 0 else 1
    tip = AX + s * 1.55
    for z in GV_Z:
        run(b, [(xi, 0.6, z), (xi - s * 0.4, 0.6, z), (xi - s * 0.4, 0.95, z), (tip, 0.95, z)], 0.18, "insulation", joints="insulation", sides=12)
        fl(b, (xi - s * 0.03, 0.6, z), "x", 0.18, 0.04)


def valve_stack(b, live, x, z, side, r, ra, name, tag):
    """A throttle or governor valve standing on its chest: the lagged body, the bonnet flange and its studs, the yoke's
    two posts and crosshead, the EHC actuator (its cylinder, end caps and tie rods, the servo valve block and the
    position transmitter); the stem and its pointer the game's; the valve's tag on the chest's outer side."""
    y0 = 1.12
    b.cyl((x, y0, z), (x, y0 + 0.62, z), r, LAG, None, 18)
    b.cyl((x, y0 + 0.62, z), (x, y0 + 0.72, z), r + 0.06, GUN, GUN, 18)
    bolts(b, (x, y0 + 0.72, z), r - 0.03, 10, 0.028, 0.05, MID, "y")
    top = y0 + 1.42                   # the yoke's crosshead
    for dz in (-r * 0.62, r * 0.62):
        b.box(x - 0.045, y0 + 0.72, z + dz - 0.045, x + 0.045, top, z + dz + 0.045, GUN)
    b.box(x - 0.08, top, z - r * 0.62 - 0.08, x + 0.08, top + 0.1, z + r * 0.62 + 0.08, GUN)
    # The actuator: its cylinder between end caps held by four tie rods.
    a0, a1 = top + 0.1, top + 0.9
    b.box(x - ra - 0.04, a0, z - ra - 0.04, x + ra + 0.04, a0 + 0.07, z + ra + 0.04, DARK)
    b.cyl((x, a0 + 0.07, z), (x, a1 - 0.07, z), ra, "lp_hood", None, 16)
    b.box(x - ra - 0.04, a1 - 0.07, z - ra - 0.04, x + ra + 0.04, a1, z + ra + 0.04, DARK)
    for dx in (-ra, ra):
        for dz in (-ra, ra):
            b.cyl((x + dx, a0, z + dz), (x + dx, a1 + 0.03, z + dz), 0.018, "stainless", "stainless", 6)
    # The servo valve block (blue) and its hydraulic lines, the position transmitter (LVDT) on the far side.
    b.box(x + ra + 0.04, a0 + 0.2, z - 0.1, x + ra + 0.2, a0 + 0.42, z + 0.1, "paint_blue")
    b.cyl((x + ra + 0.12, a0 + 0.2, z + 0.05), (x + ra + 0.12, y0 + 0.05, z + 0.05), 0.02, "stainless", "stainless", 6)
    b.cyl((x + ra + 0.17, a0 + 0.2, z - 0.05), (x + ra + 0.17, y0 + 0.05, z - 0.05), 0.02, "stainless", "stainless", 6)
    b.box(x - ra - 0.16, a0 + 0.3, z - 0.06, x - ra - 0.04, a0 + 0.62, z + 0.06, BLACK)
    # The scale on the yoke post the pointer runs up (OPEN at the top), the stem the game's.
    zs = z - r * 0.62 - 0.045
    b.box(x + 0.045, y0 + 0.8, zs, x + 0.06, top - 0.06, zs + 0.09, WHITE)
    live["stems"].append({"name": name, "c": _r(x, y0 + 0.72, z), "top": _r(top)[0], "r": 0.045 if r > 0.45 else 0.038,
                          "travel": 0.22, "pointer": _r(x + 0.06, zs + 0.045)})
    # The tag on a little plate on the chest's outer face, under the valve.
    xf = CHESTS[0][0] if side < 0 else CHESTS[1][1]
    side_plate(b, side, xf, z - 0.22, z + 0.22, 0.75, 0.95, plate(tag, 0.44, 0.2, "yellow"))


# ------------------------------------------------------------------------------------------------ the LP cylinders

def lp_cylinders(b):
    x0, x1, top = HOOD
    for k, (z0, z1) in enumerate(LPS):
        b.box(x0, 0.0, z0 + 0.35, x1, top, z1 - 0.35, LP_PAINT)
        # The hood's ends step in toward the bearings.
        for zz0, zz1 in ((z0 + 0.05, z0 + 0.35), (z1 - 0.35, z1 - 0.05)):
            b.box(x0 + 0.5, 0.0, zz0, x1 - 0.5, top - 0.5, zz1, LP_PAINT)
        rev(b, (AX, HUMP_C, z0 + 0.6), (AX, HUMP_C, z1 - 0.6),
            [(0, 1.7), (0.45, HUMP_R), (z1 - z0 - 1.65, HUMP_R), (z1 - z0 - 1.2, 1.7)], LP_PAINT, LP_PAINT, LP_PAINT, 32)


def lp_detail(b, live):
    x0, x1, top = HOOD
    for k, (z0, z1) in enumerate(LPS):
        zm = (z0 + z1) / 2
        # The hood's side ribs and its belt, the shoulders' edge angles.
        for side, xf in ((-1, x0), (1, x1)):
            z = z0 + 0.85
            while z < z1 - 0.6:
                H.side_box(b, side, xf, 0.06, 0.0, top, z - 0.04, z + 0.04, LP_PAINT, LP_PAINT)
                z += 1.0
            H.side_box(b, side, xf, 0.05, 1.6, 1.7, z0 + 0.4, z1 - 0.4, LP_PAINT, LP_PAINT)
            # The LP's nameplate on its side.
            side_plate(b, side, xf + side * 0.06, zm - 1.25, zm - 0.45, 2.1, 2.5, plate(f"LP TURBINE {'ABC'[k]}", 0.8, 0.4, "white"))
        # The shoulders' edge angles along the top.
        for xa, xb in ((x0 - 0.03, x0 + 0.06), (x1 - 0.06, x1 + 0.03)):
            b.box(xa, top - 0.06, z0 + 0.33, xb, top + 0.03, z1 - 0.33, GUN)
        # The inner casing cover's flange and bands, its lifting lugs.
        for z in (z0 + 1.05, zm - 1.7, zm + 1.7, z1 - 1.05):
            ring(b, AX, HUMP_C, z, HUMP_R + 0.015, 0.05, sw("mgrey"), 32)
        for z in (z0 + 1.6, z1 - 1.6):
            b.box(AX - 0.08, HUMP_C + HUMP_R - 0.05, z - 0.22, AX + 0.08, HUMP_C + HUMP_R + 0.28, z + 0.22, GUN)
            b.cyl((AX - 0.09, HUMP_C + HUMP_R + 0.14, z), (AX + 0.09, HUMP_C + HUMP_R + 0.14, z), 0.065, BLACK, BLACK, 8)
        # The atmospheric relief diaphragms on the shoulders, near the ends.
        for xc in (1.35, 9.65):
            for z in (z0 + 1.25, z1 - 1.25):
                b.cyl((xc, top + 0.06, z), (xc, top + 0.16, z), 0.42, GUN, DARK, 20)
                b.cyl((xc, top + 0.16, z), (xc, top + 0.19, z), 0.34, sw("mgrey"), sw("enamel"), 20)
                bolts(b, (xc, top + 0.16, z), 0.38, 12, 0.022, 0.04, MID, "y")
        # The crossover inlets on top and the intercept valves on the shoulders, their legs over into the inlets.
        for side, cx, ix in ((-1, CIV_X[0], INLET_X[0]), (1, CIV_X[1], INLET_X[1])):
            iy = HUMP_C + math.sqrt(HUMP_R ** 2 - (ix - AX) ** 2)
            b.cyl((ix, iy - 0.2, zm), (ix, 5.75, zm), XO_R, LP_PAINT, None, 18)
            fl(b, (ix, 5.75, zm), "y", XO_R, 0.06)
            civ(b, live, cx, zm, side, f"civ{k}{'LR'[side > 0]}", f"CIV-{2 * k + (side > 0) + 1}")
            run(b, [(cx, top + 1.9, zm), (cx, 6.4, zm), (ix, 6.4, zm), (ix, 5.8, zm)], XO_R, "insulation", joints="insulation", sides=18)


def civ(b, live, x, z, side, name, tag):
    """A combined intermediate valve (reheat stop and intercept) on an LP's shoulder: its body, the crossover's inlet
    flange on its outer side, the two EHC actuators along z with their stems in their yokes (the game's), its tag."""
    y0 = HOOD[2] + 0.06
    b.cyl((x, y0, z), (x, y0 + 1.84, z), CIV_R, "insulation", sw("white"), 18)
    for y in (y0 + 0.4, y0 + 1.4):
        b.cyl((x, y - 0.025, z), (x, y + 0.025, z), CIV_R + 0.015, sw("mgrey"), sw("mgrey"), 18)
    b.cyl((x, y0, z), (x, y0 + 0.1, z), CIV_R + 0.1, GUN, GUN, 18)
    xo = x + side * CIV_R
    fl(b, (xo + side * 0.03, CIV_IN, z), "x", XO_R, 0.06)
    for dz in (-1, 1):
        zb = z + dz * CIV_R
        za, zc = zb + dz * 0.35, zb + dz * 1.0
        for dy in (-0.12, 0.12):
            b.box(x - 0.04, y0 + 1.0 + dy - 0.03, min(zb, za), x + 0.04, y0 + 1.0 + dy + 0.03, max(zb, za), GUN)
        b.cyl((x, y0 + 1.0, za), (x, y0 + 1.0, zc), 0.22, ACT, DARK, 14)
        b.box(x - 0.13, y0 + 1.22, min(za, zc) + 0.15, x + 0.13, y0 + 1.36, max(za, zc) - 0.15, "paint_blue")
        live["stems"].append({"name": f"{name}{'ab'[dz > 0]}", "c": _r(x, y0 + 1.0, zb), "axis": [0, 0, dz], "top": _r(0.35)[0], "r": 0.035,
                              "travel": 0.12})
    side_plate(b, side, x + side * (CIV_R + 0.01), z - 0.25, z + 0.25, y0 + 0.5, y0 + 0.72, plate(tag, 0.5, 0.22, "yellow"))


# ------------------------------------------------------------------------------------------------ bearings, turning gear

def bearings(b, live):
    labels = ("BRG 2 - 3", "BRG 4 - 5", "BRG 6 - 7")
    for (z0, z1), label in zip(BEARINGS, labels):
        b.box(4.45, 0.0, z0 + 0.02, 6.55, 2.0, z1 - 0.02, LAG)
        b.box(4.4, 2.0, z0, 6.6, 2.12, z1, GUN)
        half_cyl(b, AX, 2.12, z0 + 0.06, z1 - 0.06, 0.62, LAG, LAG, 16)
        for x in (4.47, 6.53):
            for z in (z0 + 0.15, z1 - 0.15):
                b.cyl((x, 2.12, z), (x, 2.18, z), 0.03, MID, MID, 6)
        # The oil drain down into the floor on the left, the thermocouple conduit on the right, the label.
        zm = (z0 + z1) / 2
        run(b, [(4.45, 0.75, zm), (4.15, 0.75, zm), (4.15, -0.05, zm)], 0.1, "pipe_brown", joints=DARK, sides=8)
        b.cyl((4.15, 0.0, zm), (4.15, 0.03, zm), 0.15, MID, MID, 8)
        run(b, [(6.55, 1.6, zm), (6.75, 1.6, zm), (6.75, -0.05, zm)], 0.035, "stainless", joints="stainless", sides=6)
        side_plate(b, -1, 4.45, zm - 0.3, zm + 0.3, 1.45, 1.65, plate(label, 0.6, 0.2, "white"))


def turning_gear(b, live):
    """The pedestal between LP C and the generator (bearing 8), the turning gear on it: the gear case on top, the motor
    across to the left with its fins and fan cowl, its terminal box; the engage lever's housing on the pedestal's left
    (the lever the game's: back DISENGAGED, forward ENGAGED); the local station on its stand by the pedestal with the
    ENGAGED and MOTOR RUN lamps (the game's) and its plates."""
    z0, z1 = TG
    zm = (z0 + z1) / 2
    b.box(4.2, 0.0, z0 + 0.05, 6.8, 2.2, z1 - 0.05, LAG)
    b.box(4.15, 2.2, z0, 6.85, 2.3, z1, GUN)
    side_plate(b, 1, 6.8, zm - 0.3, zm + 0.3, 1.45, 1.65, plate("BRG 8", 0.6, 0.2, "white"))
    # The gear case on top, the motor across to the left, its fan cowl, its terminal box.
    b.box(4.55, 2.3, z0 + 0.3, 6.45, 2.95, z1 - 0.3, "paint_green")
    b.box(4.5, 2.95, z0 + 0.25, 6.5, 3.02, z1 - 0.25, GUN)
    my = 2.62
    b.cyl((4.55, my, zm), (3.45, my, zm), 0.34, "paint_green", None, 18)
    for k in range(7):
        x = 4.45 - k * 0.14
        b.cyl((x - 0.02, my, zm), (x + 0.02, my, zm), 0.38, "paint_green", "paint_green", 18)
    b.cyl((3.45, my, zm), (3.1, my, zm), 0.37, DARK, None, 18)
    b.cyl((3.1, my, zm), (3.08, my, zm), 0.33, BLACK, BLACK, 18)
    b.box(3.75, my + 0.3, zm - 0.14, 4.05, my + 0.48, zm + 0.14, DARK)
    # The engage lever's housing on the pedestal's left, its quadrant and plates (the lever the game's).
    xf = 4.2
    lz = z0 + 0.6                                   # the lever's housing, toward LP C
    b.box(3.9, 0.95, lz - 0.45, xf, 1.6, lz + 0.45, "paint_green")
    xq = 3.9
    H.side_box(b, -1, xq, 0.01, 1.05, 1.5, lz - 0.4, lz + 0.4, WHITE, DARK)
    live["lever"] = {"pivot": _r(xq - 0.012, 1.15, lz), "len": 0.55, "mats": {"arm": DARK, "knob": sw("red"), "boss": "stainless"}}
    side_plate(b, -1, xq - 0.01, lz + 0.12, lz + 0.42, 1.38, 1.47, plate("ENGAGE", 0.3, 0.09, "black"))
    side_plate(b, -1, xq - 0.01, lz - 0.42, lz - 0.12, 1.38, 1.47, plate("DISENG", 0.3, 0.09, "black"))
    # The local station on its stand beside the pedestal toward the generator, facing out to the left.
    zm = z1 - 0.55
    sx = 2.6
    b.box(sx + 0.15, 0.0, zm - 0.06, sx + 0.27, 1.0, zm + 0.06, DARK)
    b.box(sx + 0.05, 0.0, zm - 0.2, sx + 0.37, 0.04, zm + 0.2, DARK)
    b.box(sx, 1.0, zm - 0.45, sx + 0.42, 1.75, zm + 0.45, PANEL)
    side_plate(b, -1, sx, zm - 0.4, zm + 0.4, 1.6, 1.71, plate("TURNING GEAR", 0.8, 0.11, "black"))
    for k, (name, legend, colour) in enumerate((("tg_engaged", "ENGAGED", "white"), ("tg_motor", "MOTOR RUN", "red"))):
        lz = zm - 0.22 + k * 0.44
        lamp_x(b, live, name, sx, 1.42, lz, colour, -1, 0.04)
        side_plate(b, -1, sx, lz - 0.2, lz + 0.2, 1.25, 1.33, plate(legend, 0.4, 0.08, "white"))
    side_plate(b, -1, sx, zm - 0.4, zm + 0.4, 1.06, 1.18, plate("ENGAGE AT ZERO SPEED ONLY", 0.8, 0.12, "yellow"))
    run(b, [(sx + 0.42, 1.38, zm), (4.2, 1.38, zm)], 0.025, "stainless", sides=6)


# ------------------------------------------------------------------------------------------------ the generator, exciter

def generator(b):
    z0, z1 = GEN
    L_ = z1 - z0
    rev(b, (AX, AY, z0), (AX, AY, z1), [(0, 0.9), (0.25, 1.55), (0.6, 2.0), (0.8, GEN_R), (L_ - 0.8, GEN_R), (L_ - 0.6, 2.0),
                                        (L_ - 0.25, 1.55), (L_, 0.9)], GEN_PAINT, GEN_PAINT, GEN_PAINT, 36)
    # The feet on their sole plates.
    for x0, x1 in ((3.05, 3.75), (7.25, 7.95)):
        for zz in ((z0 + 0.9, z0 + 1.9), (z1 - 1.9, z1 - 0.9)):
            b.box(x0, 0.0, zz[0], x1, 0.55, zz[1], GEN_PAINT)
            b.box(x0 - 0.05, 0.0, zz[0] - 0.05, x1 + 0.05, 0.06, zz[1] + 0.05, STEEL)
    # The hydrogen coolers' housings on top.
    for cz0, cz1 in ((z0 + 1.2, z0 + 3.9), (z1 - 3.9, z1 - 1.2)):
        b.box(4.0, AY + GEN_R - 0.35, cz0, 7.0, AY + GEN_R + 0.6, cz1, GEN_PAINT)
        b.box(3.95, AY + GEN_R + 0.6, cz0 - 0.05, 7.05, AY + GEN_R + 0.68, cz1 + 0.05, GEN_PAINT)


def generator_detail(b, live):
    z0, z1 = GEN
    for z in (z0 + 1.0, z0 + 2.6, z0 + 4.5, z1 - 2.6, z1 - 1.0):
        ring(b, AX, AY, z, GEN_R + 0.02, 0.08, GEN_PAINT, 36)
    for cz0, cz1 in ((z0 + 1.2, z0 + 3.9), (z1 - 3.9, z1 - 1.2)):
        ty = AY + GEN_R + 0.68
        x = 4.15
        while x < 6.9:
            b.cyl((x, ty, cz0 + 0.1), (x, ty + 0.05, cz0 + 0.1), 0.03, MID, MID, 6)
            b.cyl((x, ty, cz1 - 0.1), (x, ty + 0.05, cz1 - 0.1), 0.03, MID, MID, 6)
            x += 0.35
        # Each cooler's water in and out: stainless lines over the side and down into the floor.
        for side in (-1, 1):
            xs = AX + side * 1.5
            xd = AX + side * (GEN_R + 0.35)
            for k, z in enumerate((cz0 + 0.5, cz1 - 0.5)):
                y = AY + GEN_R + 0.2
                run(b, [(xs, y, z), (xd, y, z), (xd, -0.05, z)], 0.07, "stainless", joints="stainless", sides=8)
                b.cyl((xd, 0.0, z), (xd, 0.03, z), 0.11, MID, MID, 8)
    # The nameplate and the hydrogen warning on the left side, each on two standoff brackets off the frame, between the
    # coolers' water lines.
    xf = AX - GEN_R - 0.06
    for (pz0, pz1, py0, py1, mat) in ((z0 + 3.5, z0 + 5.0, 1.25, 2.05,
                                       plate("GENERATOR|1,280 MVA  22 KV  60 HZ|1800 RPM  0.90 PF|H2 COOLED  75 PSIG", 1.5, 0.8, "white")),
                                      (z1 - 3.15, z1 - 1.95, 1.3, 2.0, plate("DANGER|HYDROGEN|NO SMOKING|OR OPEN FLAME", 1.2, 0.7, "red"))):
        side_plate(b, -1, xf, pz0, pz1, py0, py1, mat)
        for yb in (py0 + 0.1, py1 - 0.14):
            b.box(xf, yb, pz0 + 0.12, xf + 0.18, yb + 0.04, pz1 - 0.12, MID)
    # The seal oil lines at the end shields, down into the floor.
    for z in (z0 + 0.35, z1 - 0.35):
        for side in (-1, 1):
            xd = AX + side * 1.75
            run(b, [(AX + side * 1.2, AY - 0.6, z), (xd, AY - 0.6, z), (xd, -0.05, z)], 0.05, "pipe_brown", joints=DARK, sides=6)
    # The line terminal box under the exciter end, the isolated phase bus down into the floor.
    b.box(3.85, 0.0, z1 - 1.5, 7.15, 0.85, z1 - 0.15, GUN)
    for k in range(3):
        x = 4.4 + k * 1.1
        b.cyl((x, 0.85, z1 - 0.85), (x, 1.05, z1 - 0.85), 0.3, "stainless", "stainless", 16)
    front_plate(b, z1 - 1.5, 4.2, 6.8, 0.25, 0.6, plate("LINE TERMINALS  22 KV|DANGER HIGH VOLTAGE", 2.6, 0.35, "red"))


def exciter(b, live):
    z0, z1 = EXC
    b.box(4.15, 0.0, z0, 6.85, 1.3, z1, PANEL)
    half_cyl(b, AX, 1.3, z0, z1, 1.35, PANEL, PANEL, 20)
    b.box(4.05, 0.0, z0 - 0.05, 6.95, 0.06, z1 + 0.05, STEEL)
    # The louvres on the sides, the window onto the diode wheel (the wheel the game's), the nameplate.
    for side, xf in ((-1, 4.15), (1, 6.85)):
        # A louvred panel: its frame, a dark screen behind, the slats sloping out.
        H.side_box(b, side, xf, 0.012, 0.36, 0.99, z1 - 1.29, z1 - 0.26, BLACK, BLACK)
        for (ya, yb_, za, zb) in ((0.3, 0.36, z1 - 1.35, z1 - 0.2), (0.99, 1.05, z1 - 1.35, z1 - 0.2), (0.36, 0.99, z1 - 1.35, z1 - 1.29), (0.36, 0.99, z1 - 0.26, z1 - 0.2)):
            H.side_box(b, side, xf, 0.04, ya, yb_, za, zb, PANEL, PANEL)
        for k in range(6):
            y = 0.42 + k * 0.095
            b.obox((xf + side * 0.03, y + 0.02, z1 - 0.775), (0, 0, 1), (side * 0.5, 0.86, 0), 0.51, 0.035, 0.008, PANEL)
    xf = 4.15
    side_plate(b, -1, xf, z0 + 0.6, z0 + 1.4, 1.25, 1.55, plate("BRUSHLESS|EXCITER", 0.8, 0.3, "white"))
    # The inspection port on the end cover: the dark housing behind, its bolted bezel (the diode wheel the game's, turning
    # on the shaft), the strobe tachometer's pickup over it.
    wy = 1.0
    b.cyl((AX, wy, z1), (AX, wy, z1 + 0.006), 0.46, BLACK, BLACK, 24)
    rev(b, (AX, wy, z1), (AX, wy, z1 + 0.09), [(0, 0.5), (0.09, 0.5)], GUN, None, None, 24)
    for k in range(8):
        a = 2 * math.pi * (k + 0.5) / 8
        b.cyl((AX + math.cos(a) * 0.5, wy + math.sin(a) * 0.5, z1), (AX + math.cos(a) * 0.5, wy + math.sin(a) * 0.5, z1 + 0.12), 0.03, MID, MID, 6)
    live["wheel"] = {"c": _r(AX, wy, z1 + 0.03), "r": 0.38, "axis": [0, 0, 1],
                     "mats": {"wheel": "stainless_dark", "diode": BLACK, "fuse": sw("white")}}
    b.box(AX + 0.55, wy + 0.35, z1, AX + 0.75, wy + 0.5, z1 + 0.12, BLACK)
    back_plate(b, z1, AX - 0.55, AX + 0.55, 1.62, 1.82, plate("ROTATING RECTIFIER", 1.1, 0.2, "white"))


# ------------------------------------------------------------------------------------------------ the turbine

def turbine(seed=401):
    with own_atlas():
        return _turbine(seed)


def _turbine(seed=401):
    b = C.Board(W, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "stems": []}
    hp_cylinder(b)
    lp_cylinders(b)
    generator(b)
    b.core = len(b.still)
    front_standard(b, live)
    hp_detail(b, live)
    lp_detail(b, live)
    bearings(b, live)
    turning_gear(b, live)
    generator_detail(b, live)
    exciter(b, live)
    live["shaft"] = {"c": [AX, AY], "z0": FS[1], "z1": HP[0], "r": 0.22}
    live["mats"] = {"needle": sw("red"), "mark": sw("white"), "stem": "stainless", "pointer": sw("red")}
    b.live = live
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the MSR

MSR_W, MSR_D, MSR_T = 5, 20, 5
MSR_C, MSR_Y, MSR_R = 2.5, 2.2, 1.8
MSR_OFF = 13.0                   # where the MSR's z 0 is along the turbine (centred on LP B)
GAP = 7.0                        # from the MSR's x 0 to the turbine's x 0 (its right-hand side's)
MSR_TAGS = ("1A", "1B")


def msr(seed=409):
    with own_atlas():
        return _msr(seed)


def _msr(seed=409):
    b = C.Board(MSR_W, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    z0, z1 = 0.3, MSR_D - 0.3
    L_ = z1 - z0
    rev(b, (MSR_C, MSR_Y, z0), (MSR_C, MSR_Y, z1), [(0, 0.6), (0.3, 1.3), (0.7, MSR_R), (L_ - 0.7, MSR_R), (L_ - 0.3, 1.3), (L_, 0.6)],
        "insulation", sw("white"), sw("white"), 32)
    # The saddles.
    for z in (3.2, 10.0, 16.8):
        b.box(0.75, 0.0, z - 0.3, 4.25, 0.95, z + 0.3, STEEL)
        b.box(0.6, 0.0, z - 0.4, 4.4, 0.08, z + 0.4, STEEL)
    b.core = len(b.still)
    for z in [z0 + 0.7 + k * 1.24 for k in range(15)]:
        ring(b, MSR_C, MSR_Y, z, MSR_R + 0.012, 0.035, sw("mgrey"), 32)
    for z in (3.2, 10.0, 16.8):
        for x in (0.8, 4.2):
            b.cyl((x, 0.08, z), (x, 0.14, z), 0.035, MID, MID, 6)
    # The crossunder inlets up out of the floor into its bottom; the crossovers out of its top over to the turbine.
    for z in (5.5, 14.5):
        b.cyl((MSR_C, -0.05, z), (MSR_C, MSR_Y - MSR_R + 0.2, z), 0.6, "insulation", None, 18)
        b.cyl((MSR_C, 0.0, z), (MSR_C, 0.05, z), 0.7, sw("mgrey"), sw("mgrey"), 18)
    tx = GAP + CIV_X[0] - CIV_R - 0.05               # the intercept valve's inlet flange
    for k in range(3):
        z = lp_mid(k) - MSR_OFF
        nx = 3.6
        ny = MSR_Y + math.sqrt(MSR_R ** 2 - (nx - MSR_C) ** 2)
        run(b, [(nx, ny - 0.2, z), (nx, CIV_IN, z), (tx, CIV_IN, z)], XO_R, "insulation", joints="insulation", sides=18)
        fl(b, (nx, ny + 0.15, z), "y", XO_R, 0.06)
        # A hanger from the crossover down to the floor in the walkway's edge, clear of the walkway.
        b.box(5.0, 0.0, z - 0.06, 5.12, CIV_IN - XO_R, z + 0.06, YELLOW)
    # The relief valves on top, their discharge down the far side into the floor.
    for z in (7.6, 12.4):
        b.cyl((MSR_C - 0.4, MSR_Y + MSR_R - 0.1, z), (MSR_C - 0.4, MSR_Y + MSR_R + 0.35, z), 0.16, GUN, GUN, 12)
        b.cyl((MSR_C - 0.4, MSR_Y + MSR_R + 0.35, z), (MSR_C - 0.4, MSR_Y + MSR_R + 0.85, z), 0.11, sw("red"), DARK, 12)
        run(b, [(MSR_C - 0.4, MSR_Y + MSR_R + 0.2, z), (0.25, MSR_Y + MSR_R + 0.2, z), (0.25, -0.05, z)], 0.12, STEEL, joints=DARK, sides=10)
    # The heating steam in at the ends and the drains: the drain tanks on the near side with their level columns.
    for z in (6.8, 13.2):
        b.cyl((0.45, 0.0, z), (0.45, 1.45, z), 0.3, STEEL, DARK, 14)
        run(b, [(1.0, 0.6, z), (0.45, 0.6, z)], 0.07, STEEL, sides=8)
        b.cyl((0.08, 0.3, z), (0.08, 1.2, z), 0.05, "stainless", "stainless", 8)
    # The manways on both heads, their davits.
    for z, end in ((z0, -1), (z1, 1)):
        zz = z + end * 0.02
        b.cyl((MSR_C, MSR_Y, z - end * 0.05), (MSR_C, MSR_Y, zz + end * 0.12), 0.45, GUN, GUN, 18)
        bolts(b, (MSR_C, MSR_Y, zz + end * 0.12), 0.38, 12, 0.025, 0.04, MID, "z" if end > 0 else "-z")
    # The gauge board on its stand on the near side (shell pressure, reheat steam out), the nameplate on brackets.
    gz = 10.0
    b.box(0.2, 0.0, gz - 0.05, 0.3, 1.2, gz + 0.05, DARK)
    b.box(0.1, 0.0, gz - 0.25, 0.4, 0.04, gz + 0.25, DARK)
    b.box(0.18, 1.2, gz - 0.62, 0.3, 1.85, gz + 0.62, PANEL)
    for k, (name, unit, lo, hi, majors) in enumerate((("SHELL PRESS", "PSIG", 0, 250, 6), ("REHEAT OUT", "°F", 0, 600, 7))):
        z = gz - 0.3 + k * 0.6
        gauge_x(b, live, name, (0.18, 1.58, z), 0.18, L.dial_face_tex(unit, lo, hi, majors, 0.18 * 1.44, 270), -1)
        side_plate(b, -1, 0.18, z - 0.27, z + 0.27, 1.27, 1.35, plate(name, 0.54, 0.08, "white"))
    xp = 0.62
    for z in (8.9, 11.1):
        b.box(xp, 2.95, z - 0.05, 0.95, 3.05, z + 0.05, DARK)
    labelled(b, MSR_TAGS, lambda tag: side_plate(b, -1, xp, 8.8, 11.2, 2.7, 3.2,
                                                 L.plate_tex(f"MOISTURE SEPARATOR|REHEATER {tag}", 2.4, 0.5, "white", 96)))
    live["mats"] = {"needle": sw("red")}
    b.live = live
    on_floor(b)
    return b


def legend_image():
    """The turbine's legend texture: every piece built (the turbine hall part 2 feed train's and oil plant's too, which
    draw on it: tools/feed_train.py; part 3's crane and lube oil room: tools/hall_crane.py, tools/oil_room.py), its
    drawings packed."""
    import feed_train
    import hall_crane
    import oil_room
    for v in list(BOARDS.values()) + list(feed_train.BOARDS.values()) + list(hall_crane.BOARDS.values()) + list(oil_room.BOARDS.values()):
        v[4]()
    return ATLAS.pack()


BOARDS = {"turbine_generator": ("Turbine-Generator", W, D, T, turbine),
          "moisture_separator_reheater": ("Moisture Separator Reheater", MSR_W, MSR_D, MSR_T, msr)}
