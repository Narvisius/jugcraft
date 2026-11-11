"""The turbine building's lube oil room (turbine hall part 3): beside the main lube oil reservoir (tools/feed_train.py),
the lube oil conditioner that keeps its oil clean and dry, the clean and dirty lube oil storage tanks and the transfer
pumps between them. Drawn from their own parts as the feed train's pieces are, into the turbine's legend texture, their
live parts drawn by client/TurbineParts.java (readings: nuclear/TurbineHall.java).

A Westinghouse plant's turbine lube oil system (the PWR technology manual, 7.3): the reservoir's oil is cleaned
continuously by a conditioner (a gravity settling tank, a coalescer that takes out the water, particulate filters, a
pump and a heater) taking from the reservoir's bottom and returning to its top; the plant keeps a clean oil storage tank
to make up and refill from, a dirty oil tank to drain the reservoir into, and a pair of transfer pumps to move oil
between them, all piped through the floor.

The pieces (frame as the boards': x to the right, y up from the floor, z away from whoever placed it):

- lo_conditioner (3 wide, 2 deep, 3 tall): the skid and its drip pan, the settling tank with its level glass, breather,
  inspection hatch and immersion heater, the coalescer vessel with its differential pressure gauge, the pump and its
  motor, the local control box (RUN, HEATER and HIGH WATER lamps, START and STOP); its oil in from the reservoir and
  back to it through the floor;
- lo_storage_tank (3 x 3, 8 tall; variant 0 the clean oil tank, 1 the dirty): a vertical tank on its ring pad, its
  anchor chairs and roof, the magnetic level indicator up its front (live), the manway, the fill line down its front
  and the outlet's valve at its foot, both into the floor, the vent up out of its roof (the room carries it on outside);
- lo_transfer_pumps (2 x 2, 2 tall): the two pumps and their motors on their bedplate, the suction and discharge lines
  through the floor, the discharge header's gauge and check valves, the local station (A and B: RUN lamps)."""
import math

import control_boards as C
from control_boards import sw, _r
from diesel_set import rev, run, bolts
import turbine_set as T
from turbine_set import front_plate, side_plate, own_atlas, gauge_z, lamp_z
import feed_train as F
from feed_train import tag_tex, stencil_tex, motor_h, volute, gate_valve, handwheel, gauge, button

GUN, DARK, MID, BLACK, STEEL, PANEL = T.GUN, T.DARK, T.MID, T.BLACK, T.STEEL, T.PANEL
OIL = "pipe_brown"
SHELL = "cond_shell"
CONCRETE = "concrete"
MOTOR = F.MOTOR
PUMP = F.PUMP
INK = (26, 26, 28)

COND_W, COND_D, COND_T = 3, 2, 3
TANK_W, TANK_D, TANK_T = 3, 3, 8
XFER_W, XFER_D, XFER_T = 2, 2, 2
TANK_R = 1.32
TANK_Y = (0.14, 6.6)            # the shell's foot and top (the roof's eaves)
TANK_TAGS = (("CLEAN LUBE OIL", "T-401  CLEAN LO STORAGE"), ("DIRTY LUBE OIL", "T-402  DIRTY LO STORAGE"))


def wrap_stencil(b, cx, cz, r, y0, y1, a_mid, width, tex, n=8):
    """Lettering painted on round a vertical cylinder (centre cx, cz, radius r): the drawing cut into n strips, each a
    flat quad tangent to the shell a little out from it, a_mid the angle (degrees, 0 toward -z: the front) of the
    lettering's middle."""
    arc = width / r
    a0 = math.radians(a_mid) - arc / 2
    rr = r + 0.03
    for k in range(n):
        t0, t1 = a0 + arc * k / n, a0 + arc * (k + 1) / n
        p0 = (cx - math.sin(t0) * rr, cz - math.cos(t0) * rr)
        p1 = (cx - math.sin(t1) * rr, cz - math.cos(t1) * rr)
        # Seen from outside the angle grows toward the viewer's left: this strip (p1 its left edge) takes its share of
        # the drawing counted from the right.
        q = tex[:2] + [1 - (k + 1) / n, 0.0, 1 - k / n, 1.0]
        b.quad((p1[0], y1, p1[1]), (p1[0], y0, p1[1]), (p0[0], y0, p0[1]), (p0[0], y1, p0[1]), q)


# ------------------------------------------------------------------------------------------------ the conditioner

CT_X = (0.15, 1.95)             # its settling tank
CT_Y = (0.27, 1.65)
CT_Z = (0.75, 1.82)
CV = (2.5, 1.25, 0.3)           # the coalescer vessel: x, z, radius
CV_Y = (0.3, 1.62)


def lo_conditioner(seed=441):
    with own_atlas():
        return _lo_conditioner(seed)


def _lo_conditioner(seed=441):
    b = C.Board(COND_W, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "levels": []}
    # The skid: its channels, the drip pan and its lip.
    for z0, z1 in ((0.05, 0.15), (1.85, 1.95)):
        b.box(0.05, 0.0, z0, 2.95, 0.12, z1, DARK)
    for x0, x1 in ((0.05, 0.15), (2.85, 2.95)):
        b.box(x0, 0.0, 0.15, x1, 0.12, 1.85, DARK)
    b.box(0.15, 0.04, 0.15, 2.85, 0.08, 1.85, [None, None, None, None, MID, None])
    for z0, z1 in ((0.15, 0.2), (1.8, 1.85)):
        b.box(0.15, 0.08, z0, 2.85, 0.17, z1, DARK)
    # The settling tank on its legs: the shell, its cover and flange, the bolted inspection hatch, the breather.
    x0, x1 = CT_X
    y0, y1 = CT_Y
    z0, z1 = CT_Z
    for lx in (x0 + 0.08, x1 - 0.08):
        for lz in (z0 + 0.08, z1 - 0.08):
            b.box(lx - 0.05, 0.08, lz - 0.05, lx + 0.05, y0, lz + 0.05, DARK)
    b.box(x0, y0, z0, x1, y1, z1, SHELL)
    b.box(x0 - 0.03, y1, z0 - 0.03, x1 + 0.03, y1 + 0.05, z1 + 0.03, GUN)
    b.box(0.45, y1 + 0.05, 1.0, 1.05, y1 + 0.1, 1.55, GUN)
    for bx in (0.5, 0.75, 1.0):
        for bz in (1.05, 1.5):
            b.cyl((bx, y1 + 0.1, bz), (bx, y1 + 0.125, bz), 0.018, MID, MID, 6)
    b.cyl((1.55, y1 + 0.05, 1.55), (1.55, y1 + 0.3, 1.55), 0.03, GUN, GUN, 8)
    rev(b, (1.55, y1 + 0.3, 1.55), (1.55, y1 + 0.42, 1.55), [(0, 0.08), (0.08, 0.08), (0.12, 0.03)], DARK, DARK, DARK, 12)
    # The stiffeners round its sides.
    for sy in (0.75, 1.2):
        b.box(x0 - 0.025, sy - 0.02, z0 - 0.025, x1 + 0.025, sy + 0.02, z1 + 0.025, [SHELL, SHELL, SHELL, SHELL, SHELL, SHELL])
    # Its level glass on the front (live: the oil in the settling tank), the stencil, the tag.
    gx = 0.32
    b.box(gx - 0.06, 0.45, z0 - 0.06, gx + 0.06, 1.5, z0 - 0.025, GUN)
    b.box(gx - 0.04, 0.5, z0 - 0.065, gx + 0.04, 1.45, z0 - 0.06, [sw("enamel"), None, None, None, None, None])
    for vy in (0.47, 1.48):
        b.cyl((gx, vy, z0 - 0.025), (gx, vy, z0 - 0.09), 0.02, GUN, GUN, 6)
    live["levels"].append({"name": "LO COND", "x0": _r(gx - 0.035)[0], "x1": _r(gx + 0.035)[0], "y0": 0.5, "y1": 1.45, "z": _r(z0 - 0.067)[0]})
    front_plate(b, z0 - 0.025, 0.47, 1.25, 1.42, 1.56, stencil_tex("LUBE OIL CONDITIONER", 0.78, 0.14, INK), edge=None, d=0.003)
    front_plate(b, z0, 0.55, 1.15, 1.31, 1.37, tag_tex("LO COND 1", 0.6, 0.06), d=0.003)
    # The immersion heater through its left end: the flange, the terminal head, its conduit down to the floor.
    b.cyl((x0, 0.55, 1.3), (x0 - 0.04, 0.55, 1.3), 0.11, GUN, GUN, 12)
    b.box(x0 - 0.16, 0.45, 1.18, x0 - 0.04, 0.68, 1.42, sw("dgrey"))
    b.cyl((x0 - 0.1, 0.45, 1.3), (x0 - 0.1, 0.0, 1.3), 0.025, "stainless", None, 6)
    # The drain valve at its foot.
    b.cyl((1.75, y0 + 0.06, z0), (1.75, y0 + 0.06, z0 - 0.12), 0.03, OIL, OIL, 8)
    gate_valve(b, (1.75, y0 + 0.06, z0 - 0.17), "z", 0.035, wheel=False)
    b.cyl((1.75, y0 + 0.06, z0 - 0.21), (1.75, y0 + 0.06, z0 - 0.24), 0.034, OIL, OIL, 8)
    # The coalescer vessel on its legs: shell, body flange and bolts, domed head, lifting lug, the sump drain.
    cx, cz, cr = CV
    for k in range(3):
        a = math.radians(30 + 120 * k)
        b.box(cx + math.cos(a) * (cr - 0.06) - 0.03, 0.08, cz + math.sin(a) * (cr - 0.06) - 0.03,
              cx + math.cos(a) * (cr - 0.06) + 0.03, CV_Y[0] + 0.05, cz + math.sin(a) * (cr - 0.06) + 0.03, DARK)
    rev(b, (cx, CV_Y[0], cz), (cx, CV_Y[0] + 0.12, cz), [(0, 0.05), (0.06, cr * 0.8), (0.12, cr)], SHELL, None, None, 20)
    b.cyl((cx, CV_Y[0] + 0.12, cz), (cx, CV_Y[1], cz), cr, SHELL, None, 20)
    b.cyl((cx, CV_Y[1], cz), (cx, CV_Y[1] + 0.06, cz), cr + 0.06, GUN, GUN, 20)
    bolts(b, (cx, CV_Y[1] + 0.06, cz), cr + 0.03, 12, br=0.016, length=0.02, axis_="y")
    rev(b, (cx, CV_Y[1] + 0.06, cz), (cx, CV_Y[1] + 0.3, cz), [(0, cr), (0.1, cr * 0.85), (0.2, cr * 0.45), (0.24, 0.05)], SHELL, None, None, 20)
    b.box(cx - 0.02, CV_Y[1] + 0.26, cz - 0.07, cx + 0.02, CV_Y[1] + 0.38, cz + 0.07, GUN)
    side_plate(b, 1, cx + cr + 0.002, cz - 0.17, cz + 0.17, 1.2, 1.27, tag_tex("COALESCER", 0.34, 0.07), d=0.003)
    # Its differential pressure gauge on a bracket off the front (live), the sensing lines along the shell.
    fz = cz - cr
    b.box(cx - 0.03, 0.95, fz - 0.06, cx + 0.03, 1.45, fz + 0.02, DARK)
    gauge_z(b, live, "COND FILTER DP", (cx, 1.33, fz - 0.06), 0.1, gauge("COND FILTER DP", "PSID", 0, 30, 6, 0.1))
    front_plate(b, fz - 0.06, cx - 0.12, cx + 0.12, 1.07, 1.14, tag_tex("FILTER DP", 0.24, 0.07), d=0.003)
    for sx in (-0.06, 0.06):
        run(b, [(cx + sx, 1.2, fz - 0.06), (cx + sx, 1.2, fz + 0.01)], 0.01, "stainless")
    # The pump and its motor on their base at the front: suction from the tank's foot, the discharge up and along to
    # the coalescer through its valve and check valve.
    b.box(0.45, 0.17, 0.2, 1.62, 0.25, 0.52, DARK)
    volute(b, (0.72, 0.42, 0.36), "x", 0.13, 0.12, PUMP)
    b.box(0.79, 0.27, 0.27, 0.95, 0.53, 0.45, sw("orange"))
    motor_h(b, (0.96, 0.42, 0.36), "x", 0.5, 0.14, MOTOR)
    b.box(1.2, 0.56, 0.3, 1.32, 0.66, 0.42, MOTOR)
    run(b, [(1.26, 0.66, 0.36), (1.26, 0.66, 0.63), (1.26, 0.0, 0.63)], 0.02, "stainless", sides=6)
    run(b, [(0.4, 0.5, z0), (0.4, 0.5, 0.36), (0.6, 0.5, 0.36)], 0.04, OIL, sides=10)
    b.cyl((0.6, 0.5, 0.36), (0.66, 0.5, 0.36), 0.06, OIL, OIL, 10)
    dy = 0.92
    run(b, [(0.72, 0.55, 0.36), (0.72, dy, 0.36), (2.5, dy, 0.36), (2.5, dy, fz)], 0.04, OIL, sides=10)
    gate_valve(b, (1.05, dy, 0.36), "x", 0.04)
    b.cyl((2.0, dy, 0.36), (2.12, dy, 0.36), 0.06, GUN, GUN, 10)
    b.box(2.03, dy + 0.06, 0.32, 2.09, dy + 0.1, 0.4, GUN)
    # The clean oil out of the coalescer's foot and down into the floor (back to the reservoir); the dirty oil in from
    # the reservoir up out of the floor into the tank's back.
    run(b, [(cx + 0.18, CV_Y[0] + 0.06, cz + 0.12), (cx + 0.18, CV_Y[0] + 0.06, 1.75), (cx + 0.18, 0.0, 1.75)], 0.035, OIL, sides=10)
    run(b, [(1.7, 0.0, 1.9), (1.7, 1.35, 1.9), (1.7, 1.35, z1)], 0.04, OIL, sides=10)
    gate_valve(b, (1.7, 0.95, 1.9), "z", 0.04, wheel=False)
    # The control box on the tank's front: lamps (RUN, HEATER, HIGH WATER), START and STOP, its conduit down.
    bx0, bx1, by0, by1 = 1.32, 1.9, 1.0, 1.36
    zf = z0 - 0.12
    b.box(bx0, by0, zf, bx1, by1, z0, [PANEL, None, PANEL, PANEL, PANEL, PANEL])
    front_plate(b, zf, bx0 + 0.06, bx1 - 0.06, by1 - 0.075, by1 - 0.02, tag_tex("CONDITIONER", bx1 - bx0 - 0.12, 0.055), d=0.003)
    for k, (name, colour, legend) in enumerate((("cond_run", "green", "RUN"), ("cond_heat", "amber", "HTR"), ("cond_water", "red", "H2O HI"))):
        lx = bx0 + 0.1 + k * 0.19
        lamp_z(b, live, name, lx, by0 + 0.2, zf, colour, 0.03)
        front_plate(b, zf, lx - 0.08, lx + 0.08, by0 + 0.12, by0 + 0.16, tag_tex(legend, 0.16, 0.04), d=0.003)
    button(b, bx0 + 0.15, by0 + 0.06, zf, "green", 0.025)
    button(b, bx0 + 0.34, by0 + 0.06, zf, "red", 0.025)
    run(b, [(bx1 - 0.08, by0, z0 - 0.06), (bx1 - 0.08, 0.17, z0 - 0.06)], 0.02, "stainless", sides=6)
    live["mats"] = {"needle": sw("red"), "water": "glaze_brown"}
    b.live = live
    return b


# ------------------------------------------------------------------------------------------------ the storage tanks

def lo_storage_tank(seed=443):
    """Both tanks' parts, each variant's own lettering (v0 clean, v1 dirty)."""
    with own_atlas():
        return _lo_storage_tank(seed)


def _lo_storage_tank(seed=443):
    b = C.Board(TANK_W, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "levels": []}
    cx, cz, r = 1.5, 1.5, TANK_R
    y0, y1 = TANK_Y
    # The ring pad, the anchor chairs at the shell's foot.
    b.box(0.08, 0.0, 0.08, 2.92, y0, 2.92, [CONCRETE, CONCRETE, CONCRETE, CONCRETE, CONCRETE, None])
    for k in range(6):
        a = math.radians(30 + 60 * k)
        px, pz = cx + math.sin(a) * (r + 0.07), cz - math.cos(a) * (r + 0.07)
        b.obox((px, y0 + 0.14, pz), (math.sin(a), 0, -math.cos(a)), (0, 1, 0), 0.07, 0.14, 0.06, GUN)
        b.cyl((px + math.sin(a) * 0.03, y0, pz - math.cos(a) * 0.03), (px + math.sin(a) * 0.03, y0 + 0.05, pz - math.cos(a) * 0.03), 0.022, MID, MID, 6)
    # The shell, its girth seams, the roof (a shallow cone) and its edge angle.
    b.cyl((cx, y0, cz), (cx, y1, cz), r, SHELL, None, 24)
    for sy in (1.9, 3.65, 5.4):
        rev(b, (cx, sy, cz), (cx, sy + 0.025, cz), [(0, r + 0.008), (0.025, r + 0.008)], SHELL, None, None, 24)
    rev(b, (cx, y1, cz), (cx, y1 + 0.42, cz), [(0, r + 0.04), (0.04, r + 0.04), (0.06, r), (0.42, 0.18)], SHELL, None, SHELL, 24)
    # The vent out of the roof's crown (the room carries it on up and out through the wall).
    b.cyl((cx, y1 + 0.38, cz), (cx, TANK_T, cz), 0.07, OIL, None, 10)
    rev(b, (cx, y1 + 0.42, cz), (cx, y1 + 0.5, cz), [(0, 0.12), (0.08, 0.12)], GUN, GUN, GUN, 12)
    # The roof manhole and the gauge hatch.
    for (hx, hz, hr) in ((cx + 0.6, cz + 0.3, 0.2), (cx - 0.55, cz - 0.35, 0.1)):
        hy = y1 + 0.42 * (1 - (math.hypot(hx - cx, hz - cz) - 0.18) / (r - 0.18))
        b.cyl((hx, hy - 0.05, hz), (hx, hy + 0.1, hz), hr, GUN, GUN, 12)
    # The manway near its foot on the front: the neck, the bolted cover, the davit; its tag.
    my = 0.85
    fz = cz - r
    b.cyl((cx, my, fz + 0.05), (cx, my, fz - 0.12), 0.3, SHELL, SHELL, 16)
    b.cyl((cx, my, fz - 0.12), (cx, my, fz - 0.17), 0.36, GUN, GUN, 16)
    bolts(b, (cx, my, fz - 0.17), 0.32, 12, br=0.018, length=0.02, axis_="-z")
    b.cyl((cx + 0.42, my - 0.2, fz - 0.05), (cx + 0.42, my + 0.4, fz - 0.05), 0.025, GUN, GUN, 6)
    run(b, [(cx + 0.42, my + 0.4, fz - 0.05), (cx + 0.42, my + 0.4, fz - 0.14), (cx + 0.1, my + 0.4, fz - 0.14), (cx + 0.1, my + 0.36, fz - 0.14)],
        0.02, GUN, sides=6)
    b.box(cx + 0.4, my - 0.24, fz - 0.03, cx + 0.44, my - 0.16, fz + 0.06, GUN)
    # The magnetic level indicator up its left front (its chamber off a nozzle at each end, the red and white flags
    # live, its scale), the fill line down its right front into the floor, the outlet and its valve at its foot.
    ma = math.radians(-35)
    mx, mz = cx + math.sin(ma) * (r + 0.2), cz - math.cos(ma) * (r + 0.2)
    for ny in (0.45, 6.15):
        sx_, sz_ = cx + math.sin(ma) * (r - 0.02), cz - math.cos(ma) * (r - 0.02)
        b.cyl((sx_, ny, sz_), (mx, ny, mz), 0.04, SHELL, SHELL, 8)
        b.cyl((mx, ny - 0.07, mz), (mx, ny + 0.07, mz), 0.07, GUN, GUN, 10)
    b.cyl((mx, 0.42, mz), (mx, 6.18, mz), 0.05, "stainless", "stainless", 10)
    fx0, fz0 = mx + math.cos(ma) * 0.0, mz - 0.075
    b.box(mx - 0.035, 0.6, mz - 0.075, mx + 0.035, 6.0, mz - 0.05, [sw("enamel"), None, DARK, DARK, None, None])
    live["levels"].append({"name": "LO STORAGE", "x0": _r(mx - 0.03)[0], "x1": _r(mx + 0.03)[0], "y0": 0.6, "y1": 6.0, "z": _r(mz - 0.077)[0]})
    for k in range(11):
        ty = 0.6 + 5.4 * k / 10
        b.box(mx + 0.05, ty - 0.008, mz - 0.07, mx + 0.11 if k % 5 == 0 else mx + 0.08, ty + 0.008, mz - 0.06, BLACK)
    fa = math.radians(32)
    lx, lz = cx + math.sin(fa) * (r + 0.12), cz - math.cos(fa) * (r + 0.12)
    nx, nz = cx + math.sin(fa) * (r - 0.02), cz - math.cos(fa) * (r - 0.02)
    b.cyl((nx, 6.2, nz), (lx, 6.2, lz), 0.06, OIL, OIL, 10)
    run(b, [(lx, 6.2, lz), (lx, 0.0, lz)], 0.055, OIL, sides=10)
    for cy_ in (1.6, 3.4, 5.2):
        b.obox(((lx + nx) / 2, cy_, (lz + nz) / 2), (math.sin(fa), 0, -math.cos(fa)), (0, 1, 0), 0.08, 0.03, 0.025, GUN)
    gate_valve(b, (lx, 2.4, lz), "x", 0.05, wheel=False)
    handwheel(b, (lx, 2.4, lz - 0.12), "z", 0.1)
    b.cyl((lx, 2.4, lz - 0.06), (lx, 2.4, lz - 0.12), 0.015, "stainless", None, 6)
    oa = math.radians(12)
    ox, oz = cx + math.sin(oa) * (r - 0.02), cz - math.cos(oa) * (r - 0.02)
    tx, tz = cx + math.sin(oa) * (r + 0.32), cz - math.cos(oa) * (r + 0.32)
    b.cyl((ox, 0.35, oz), (tx, 0.35, tz), 0.06, OIL, OIL, 10)
    gate_valve(b, ((ox + tx) / 2 + math.sin(oa) * 0.06, 0.35, (oz + tz) / 2 - math.cos(oa) * 0.06), "z", 0.055)
    run(b, [(tx, 0.35, tz), (tx, 0.0, tz)], 0.06, OIL, sides=10)
    # Each variant's lettering painted round the shell's front, and its tag on the manway's neck.
    for v, (big, tag) in enumerate(TANK_TAGS):
        st = b.state(f"v{v}")
        tex = stencil_tex(big, 2.6, 0.3, INK)
        _wrap_into(b, st, cx, cz, r, 4.15, 4.45, 0.0, 2.6, tex)
        cap = stencil_tex("CAPACITY 12,000 GAL", 1.5, 0.15, INK)
        _wrap_into(b, st, cx, cz, r, 3.85, 4.0, 0.0, 1.5, cap)
        st["still"].append(["b", *_r(cx - 0.28, my + 0.44, fz - 0.05, cx + 0.28, my + 0.52, fz - 0.047),
                            [tag_tex(tag, 0.56, 0.08), None, None, None, None, None]])
    live["mats"] = {"needle": sw("red"), "water": sw("red")}
    b.live = live
    return b


def _wrap_into(b, st, cx, cz, r, y0, y1, a_mid, width, tex, n=10):
    keep = b.still
    b.still = st["still"]
    try:
        wrap_stencil(b, cx, cz, r, y0, y1, a_mid, width, tex, n)
    finally:
        b.still = keep


# ------------------------------------------------------------------------------------------------ the transfer pumps

def lo_transfer_pumps(seed=445):
    with own_atlas():
        return _lo_transfer_pumps(seed)


def _lo_transfer_pumps(seed=445):
    b = C.Board(XFER_W, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "levels": []}
    # The bedplate, its drip lip and anchor bolts.
    b.box(0.1, 0.0, 0.15, 1.9, 0.1, 1.55, DARK)
    for x in (0.18, 1.82):
        for z in (0.22, 1.48):
            b.cyl((x, 0.1, z), (x, 0.14, z), 0.022, MID, MID, 6)
    # Each pump (its casing at the front, the coupling guard, the motor along z behind it), its suction up out of the
    # floor into the eye, its discharge up through the check valve and the valve into the header.
    hy, hz = 1.05, 0.3
    for k, px in enumerate((0.55, 1.45)):
        b.box(px - 0.2, 0.1, 0.3, px + 0.2, 0.18, 1.5, DARK)
        volute(b, (px, 0.38, 0.42), "z", 0.13, 0.12, PUMP)
        b.box(px - 0.08, 0.26, 0.5, px + 0.08, 0.5, 0.66, sw("orange"))
        motor_h(b, (px, 0.38, 0.67), "z", 0.6, 0.15, MOTOR)
        b.box(px + 0.12, 0.38, 0.9, px + 0.24, 0.5, 1.04, MOTOR)
        run(b, [(px + 0.18, 0.38, 0.97), (px + 0.18, 0.38, 1.6), (px + 0.18, 0.0, 1.6)], 0.02, "stainless", sides=6)
        run(b, [(px, 0.0, 0.12), (px, 0.38, 0.12), (px, 0.38, 0.36)], 0.045, OIL, sides=10)
        gate_valve(b, (px, 0.22, 0.12), "z", 0.045, wheel=False)
        handwheel(b, (px, 0.22, 0.0), "z", 0.07)
        run(b, [(px, 0.5, 0.42), (px, hy, 0.42), (px, hy, hz)], 0.04, OIL, sides=10)
        b.cyl((px, 0.66, 0.42), (px, 0.78, 0.42), 0.06, GUN, GUN, 10)
        gate_valve(b, (px, 0.88, 0.42), "z", 0.04)
        front_plate(b, 0.36, px - 0.17, px + 0.17, 0.2, 0.26, tag_tex(f"LO XFER PP {'AB'[k]}", 0.34, 0.06), d=0.003)
    # The header along the front and down into the floor at its left end, its pressure gauge.
    run(b, [(1.45, hy, hz), (0.12, hy, hz), (0.12, 0.0, hz)], 0.05, OIL, sides=10)
    b.cyl((1.0, hy, hz), (1.0, hy + 0.18, hz), 0.012, "stainless", None, 6)
    b.box(0.97, hy + 0.12, hz - 0.03, 1.03, hy + 0.17, hz + 0.03, GUN)
    gauge_z(b, live, "LO XFER DISCH", (1.0, hy + 0.3, hz - 0.02), 0.09, gauge("LO XFER DISCH", "PSIG", 0, 100, 5, 0.09))
    # The local station on its post at the back right: each pump's RUN lamp, START and STOP; the conduit down.
    sx0, sx1, sy0, sy1, sz = 1.25, 1.85, 1.15, 1.6, 1.62
    b.box(1.52, 0.1, 1.68, 1.58, sy0, 1.74, DARK)
    b.box(sx0, sy0, sz, sx1, sy1, sz + 0.12, [PANEL, PANEL, PANEL, PANEL, PANEL, PANEL])
    front_plate(b, sz, sx0 + 0.05, sx1 - 0.05, sy1 - 0.08, sy1 - 0.025, tag_tex("LO XFER PUMPS", sx1 - sx0 - 0.1, 0.055), d=0.003)
    for k, name in enumerate(("lox_a", "lox_b")):
        lx = sx0 + 0.15 + k * 0.3
        lamp_z(b, live, name, lx, sy0 + 0.25, sz, "red", 0.03)
        front_plate(b, sz, lx - 0.12, lx + 0.12, sy0 + 0.17, sy0 + 0.21, tag_tex(f"PP {'AB'[k]} RUN", 0.24, 0.04), d=0.003)
        button(b, lx - 0.05, sy0 + 0.08, sz, "green", 0.022)
        button(b, lx + 0.05, sy0 + 0.08, sz, "red", 0.022)
    live["mats"] = {"needle": sw("red")}
    b.live = live
    return b


BOARDS = {"lo_conditioner": ("Lube Oil Conditioner", COND_W, COND_D, COND_T, lo_conditioner),
          "lo_storage_tank": ("Lube Oil Storage Tank", TANK_W, TANK_D, TANK_T, lo_storage_tank),
          "lo_transfer_pumps": ("Lube Oil Transfer Pumps", XFER_W, XFER_D, XFER_T, lo_transfer_pumps)}
