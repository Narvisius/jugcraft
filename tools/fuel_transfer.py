"""The fuel transfer system, reactor side (reactor building rebuild, machine 4).

Built like the Westinghouse PWR fuel transfer system (US4053067, US4717531): fuel passes between the containment's
refuelling canal and the fuel building's pool through the transfer tube, lying in a fuel container on a conveyor car.

- The upender pit: the deep end of the transfer canal, where the refuelling machine's mast reaches down into the
  container standing upright. The car's rails run from the pit's stop into the tube.
- Conveyor car (the client moves it along the rails): a central beam on three axles of flanged wheels, the trunnion
  plates at its west end that carry the container's trunnions, the cradle and the end retainer plate at its east end,
  the pusher arm and the drive chains running into the tube (the drive is on the fuel building side).
- Fuel container (the client turns it about the trunnions with the upender): a square stainless box with its bottom
  plate, flow holes, bands, a lead-in funnel at its open top, the trunnions and the side pins the upender's hooks take.
- Upender: two lifting arms on pivot pedestals, tied by a cross tie behind the container, open hooks round the
  container's pins, each arm raised by a hydraulic cylinder (body, rod, clevises, hose ports, hoses) from a floor
  anchor; the arms' stop posts; the limit switch bracket (car at reactor, upender up, upender down) with its junction
  box and conduit up the pit wall.
- Transfer tube end: its weld-neck flange and studs on the pit's east wall (the blind flange is off while
  refuelling), the tube's bore, the rails going in, painted wall legends.
- On the operating floor beside the canal: the reactor-side control console (lamps, car position readout, hydraulic
  pressure gauge, upender and car controls, permissive key, E-stop, the phone to the fuel building), the upender's
  hydraulic power unit (reservoir, motor and pump, accumulator, valve manifold, gauge, filter, drip skid) with the
  hydraulic lines down the pit wall, the blind flange on its storage stand.

Frame: the reactor building's (tools/reactor_head.py, as machine 3): the canal along x east of the cavity, its
centre line at z = C. The pivot (the container's trunnions when the car is at the reactor side) is at (XP, P_Y); the
refuelling machine's mast reaches it. y as reactor_head's (the operating floor at OP).

Live (LIVE): car position along the rails (0 at the reactor side), the upender's angle (90 up .. 0 down: the
container and the arms turn together, the cylinders' rods follow), the fuel assembly in the container, the console's
lamps, its car position readout (ft) and the hydraulic pressure gauge (psig: about 1,500 while moving, 0 with the HPU
off).
"""
import math

import control_boards as CB
from control_boards import sw, pic
import reactor_head as RH
import rb_parts as RP
import refuelling_machine as RM
from reactor_head import Head, C, legend, legend_size, _cross, _unit

OP = RM.OP
CF = RH.CAVITY_FLOOR
ZC = C                                   # the canal's centre line
CANAL_HALF = 1.5
CANAL_END = C + 16.5
PIT_X = (C + 9.4, CANAL_END)             # the upender pit: the canal's deep end
PIT_HALF = 1.1
PIT_FLOOR = 0.95
XP = C + 11.0                            # the pivot: the container's trunnions with the car at the reactor side
P_Y = 2.04
TUBE_Y = P_Y - 0.06                      # the transfer tube's axis (20 in: bore radius 0.245)
TUBE_R = 0.245
TUBE_END = C + 24.0                      # where it leaves the drawing (into the fuel building)
RAIL_Y = TUBE_Y - 0.17                   # the car's rails' top (their feet just clear of the bore)
GAUGE = 0.12                             # half the rails' gauge
CONT = 0.125                             # the container's half width (inside 0.112)
CONT_BOT = P_Y - 0.1                     # its bottom end (upright)
CONT_TOP = RM.CORE_BOT + RM.FUEL_L + 0.15
ARM_Z = 0.33                             # the upender arms' z from the centre line
ARM_L = 2.1
CYL_ANCHOR = (XP - 1.2, PIT_FLOOR + 0.2) # each cylinder's floor clevis, west of the pivot
CRANK = 0.6                              # the arm's crank: pointing east from the pivot with the arm upright
CYL_Z = 0.27                             # the cylinders' (and cranks') z from the centre line
CYL_BODY = 1.1
BLUE = "paint_blue"
YELLOW = "paint_yellow"


class Transfer(Head):
    def __init__(self):
        CB.Board.__init__(self, 21, 404)
        self.live = {"car": {"still": []}, "container": {"still": []}, "arms": {"still": []}, "fuel": {"still": []},
                     "cylinders": [], "lamps": {}, "readouts": [], "dial": None}


def _with(b, lst, fn, *a, **k):
    keep = b.still
    b.still = lst
    try:
        fn(*a, **k)
    finally:
        b.still = keep


def tag(b, lst, text, c, n, up=(0, 1, 0)):
    _with(b, lst, b.tag, text, c, n, up=up)


# ---------------------------------------------------------------- rotating and moving operations

def _rot(p, c, ang):
    """p turned about the z axis through c by ang (radians, x toward y)."""
    x, y = p[0] - c[0], p[1] - c[1]
    ca, sa = math.cos(ang), math.sin(ang)
    return (c[0] + x * ca - y * sa, c[1] + x * sa + y * ca, p[2])


def _rotv(v, ang):
    ca, sa = math.cos(ang), math.sin(ang)
    return (v[0] * ca - v[1] * sa, v[0] * sa + v[1] * ca, v[2])


def turned(ops, c, ang, dx=0.0):
    """Operations turned about the z axis through c (then moved dx along x): boxes become oriented boxes."""
    out = []
    for op in ops:
        k = op[0]
        if k == "b":
            x0, y0, z0, x1, y1, z1 = op[1:7]
            m = ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
            mc = _rot(m, c, ang)
            u, v = _rotv((1, 0, 0), ang), _rotv((0, 1, 0), ang)
            out.append(["o", *CB._r(mc[0] + dx, mc[1], mc[2], *u, *v, (x1 - x0) / 2, (y1 - y0) / 2, (z1 - z0) / 2), op[7]])
        elif k == "o":
            cc = _rot(op[1:4], c, ang)
            u, v = _rotv(op[4:7], ang), _rotv(op[7:10], ang)
            out.append(["o", *CB._r(cc[0] + dx, cc[1], cc[2], *u, *v, *op[10:13]), op[13]])
        elif k == "q":
            pts = [_rot(op[i:i + 3], c, ang) for i in (1, 4, 7, 10)]
            out.append(["q", *CB._r(*[v + (dx if j % 3 == 0 else 0) for p in pts for j, v in enumerate(p)]), op[13]])
        elif k == "c":
            a, e = _rot(op[1:4], c, ang), _rot(op[4:7], c, ang)
            out.append(["c", *CB._r(a[0] + dx, a[1], a[2], e[0] + dx, e[1], e[2], op[7]), op[8], op[9], op[10]])
    return out


# ---------------------------------------------------------------- the fixed parts in the pit

def bore(b, lst, x0, x1, y, z, r, mat, n=16):
    """The inside of a horizontal tube along x (faces turned inward), cut into pieces a block long."""
    x = x0
    while x < x1 - 1e-6:
        xe = min(x1, x + 1.0)
        for i in range(n):
            a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            p = lambda a, xx: (xx, y + r * math.cos(a), z + r * math.sin(a))
            b.quad(p(a1, x), p(a1, xe), p(a0, xe), p(a0, x), RH.tile_mat(mat, 0, 0, min(1, xe - x), min(1, 2 * math.pi * r / n * 1.0)), lst)
        x = xe


def rails(b):
    L = b.still
    x0 = XP - 0.55
    for s in (-1, 1):
        z = ZC + s * GAUGE
        b.box(x0, RAIL_Y - 0.03, z - 0.02, TUBE_END, RAIL_Y - 0.012, z + 0.02, "stainless_dark")
        b.box(x0, RAIL_Y - 0.012, z - 0.008, TUBE_END, RAIL_Y, z + 0.008, "stainless")
    # in the pit: stanchions from the floor, a sole bar under each rail; in the tube: its support plate
    x = x0 + 0.15
    while x < PIT_X[1] - 0.1:
        for s in (-1, 1):
            z = ZC + s * GAUGE
            b.box(x - 0.03, PIT_FLOOR + 0.015, z - 0.03, x + 0.03, RAIL_Y - 0.03, z + 0.03, "stainless_dark")
            b.box(x - 0.07, PIT_FLOOR, z - 0.07, x + 0.07, PIT_FLOOR + 0.015, z + 0.07, "stainless_dark")
            for dx in (-0.05, 0.05):
                RP.bolt(b, L, (x + dx, PIT_FLOOR + 0.015, z + 0.05), (0, 1, 0), 0.008, 0.006, False)
        RP.obox(b, L, (x, RAIL_Y - 0.05, ZC), (0, 0, 1), (0, 1, 0), GAUGE - 0.02, 0.012, 0.012, "stainless_dark")
        x += 0.75
    # the car's stop at the pit end of the rails
    for s in (-1, 1):
        z = ZC + s * GAUGE
        b.box(x0 - 0.12, PIT_FLOOR, z - 0.05, x0, RAIL_Y + 0.08, z + 0.05, YELLOW)
        b.box(x0, RAIL_Y + 0.0, z - 0.035, x0 + 0.03, RAIL_Y + 0.07, z + 0.035, "rubber")


def tube_end(b):
    """The transfer tube's end on the pit's east wall: weld-neck flange, its studs (the blind flange is off), the bore,
    a dark end where it runs on into the fuel building."""
    L = b.still
    xw = PIT_X[1]
    RP.cyl(b, L, (xw, TUBE_Y, ZC), (xw - 0.07, TUBE_Y, ZC), 0.4, "stainless", "stainless_dark", 28, both=False)
    RP.cyl(b, L, (xw - 0.07, TUBE_Y, ZC), (xw - 0.1, TUBE_Y, ZC), 0.3, "stainless", "stainless", 24, both=False)
    for k in range(20):
        a = 2 * math.pi * (k + 0.5) / 20
        p = (xw - 0.07, TUBE_Y + 0.35 * math.cos(a), ZC + 0.35 * math.sin(a))
        RP.cyl(b, L, p, (p[0] - 0.09, p[1], p[2]), 0.012, "stainless_dark", "stainless_dark", 6, both=False)
    bore(b, L, xw - 0.1, xw + 2.0, TUBE_Y, ZC, TUBE_R, "stainless")
    b.quad((xw + 2.0, TUBE_Y + TUBE_R, ZC + TUBE_R), (xw + 2.0, TUBE_Y - TUBE_R, ZC + TUBE_R),
           (xw + 2.0, TUBE_Y - TUBE_R, ZC - TUBE_R), (xw + 2.0, TUBE_Y + TUBE_R, ZC - TUBE_R), "black", L)
    # painted legends on the wall's liner
    b.stencil("TRANSFER TUBE", (xw, TUBE_Y + 0.75, ZC), (-1, 0, 0))
    b.stencil("UPENDER PIT", (XP + 2.6, 4.2, ZC - PIT_HALF), (0, 0, 1))


def upender_base(b):
    """The upender's fixed parts: pivot pedestals on base plates, the cylinders' floor anchors, the arms' stop posts,
    the limit switch bracket with its junction box and conduit."""
    L = b.still
    for s in (-1, 1):
        zp = ZC + s * 0.43
        b.box(XP - 0.24, PIT_FLOOR, zp - 0.075, XP + 0.24, PIT_FLOOR + 0.03, zp + 0.075, "stainless_dark")
        for dx in (-0.19, 0.19):
            for dz in (-0.06, 0.06):
                RP.bolt(b, L, (XP + dx, PIT_FLOOR + 0.03, zp + dz), (0, 1, 0), 0.011, 0.01, True)
        b.box(XP - 0.12, PIT_FLOOR + 0.03, zp - 0.05, XP + 0.12, P_Y - 0.08, zp + 0.05, "stainless_dark")
        RP.cyl(b, L, (XP, P_Y, zp - 0.06), (XP, P_Y, zp + 0.06), 0.11, "stainless_dark", "stainless_dark", 16, both=True)
        RP.cyl(b, L, (XP, P_Y, ZC + s * (ARM_Z + 0.02)), (XP, P_Y, zp + s * 0.08), 0.035, "stainless", "stainless", 10, both=True)
        # cylinder anchor (a clevis block on its base plate)
        ax, ay = CYL_ANCHOR
        za = ZC + s * CYL_Z
        b.box(ax - 0.12, PIT_FLOOR, za - 0.1, ax + 0.12, PIT_FLOOR + 0.02, za + 0.1, "stainless_dark")
        for dx in (-0.08, 0.08):
            RP.bolt(b, L, (ax + dx, PIT_FLOOR + 0.02, za + 0.075), (0, 1, 0), 0.009, 0.008, False)
            RP.bolt(b, L, (ax + dx, PIT_FLOOR + 0.02, za - 0.075), (0, 1, 0), 0.009, 0.008, False)
        for dz in (-0.055, 0.055):
            b.box(ax - 0.05, PIT_FLOOR + 0.02, za + dz - 0.012, ax + 0.05, ay + 0.06, za + dz + 0.012, "stainless_dark")
        RP.cyl(b, L, (ax, ay, za - 0.075), (ax, ay, za + 0.075), 0.02, "stainless", "stainless", 8, both=True)
    # limit switch bracket on the pit's -z wall by the pivot: three switches with rollers, their junction box and
    # the conduit up the wall to the operating floor
    zw = ZC - PIT_HALF
    b.box(XP - 0.6, 1.2, zw, XP + 0.9, 1.28, zw + 0.05, "stainless_dark")
    b.box(XP - 0.6, 1.2, zw + 0.05, XP + 0.9, 1.22, zw + 0.35, "stainless_dark")
    for dx in (-0.55, 0.85):
        RP.bolt(b, L, (XP + dx, 1.25, zw + 0.05), (0, 0, 1), 0.01, 0.008, False)
    for k, (x, ang) in enumerate(((XP - 0.3, 0), (XP + 0.15, 0), (XP + 0.6, 0))):
        b.box(x - 0.07, 1.22, zw + 0.12, x + 0.07, 1.36, zw + 0.24, "paint_grey")
        RP.cyl(b, L, (x, 1.36, zw + 0.18), (x, 1.42, zw + 0.18), 0.012, "stainless", "stainless", 6, both=False)
        RP.obox(b, L, (x, 1.47, zw + 0.18), (1, 0, 0), (0, 1, 0), 0.008, 0.06, 0.008, "stainless")
        RP.cyl(b, L, (x, 1.52, zw + 0.16), (x, 1.52, zw + 0.2), 0.022, "rubber", "rubber", 10, both=True)
    tag(b, L, "UPENDER LIMIT SWITCHES", (XP + 0.15, 1.24, zw + 0.05), (0, 0, 1))
    b.box(XP + 1.1, 1.3, zw, XP + 1.4, 1.65, zw + 0.14, "paint_grey")
    tag(b, L, "FTS JB-1", (XP + 1.25, 1.55, zw + 0.141), (0, 0, 1))
    RP.conduit(b, L, [(XP + 0.9, 1.25, zw + 0.03), (XP + 1.1, 1.25, zw + 0.03)], 0.012)
    RP.conduit(b, L, [(XP + 1.25, 1.65, zw + 0.06), (XP + 1.25, CF - 0.05, zw + 0.06), (XP + 1.25, CF + 0.04, zw - 0.2),
                      (XP + 1.25, CF + 0.04, ZC - CANAL_HALF + 0.06), (XP + 1.25, OP + 0.05, ZC - CANAL_HALF + 0.06)], 0.02,
               walls=[((0, 0, 1), zw), None, ((0, 1, 0), CF), ((0, 0, 1), ZC - CANAL_HALF)])


def hydraulic_lines(b):
    """From each cylinder's ports: hoses to a floor manifold, steel lines along the floor to the -z wall and up it,
    across the ledge and up the canal wall to the HPU."""
    L = b.still
    zw = ZC - PIT_HALF
    mx = XP + 0.75
    b.box(mx - 0.1, PIT_FLOOR, zw + 0.25, mx + 0.1, PIT_FLOOR + 0.12, zw + 0.45, "stainless_dark")
    for k, dz in enumerate((0.3, 0.4)):
        for x in (XP + 1.6 + k * 0.08,):
            pts = [(mx + 0.1, PIT_FLOOR + 0.06, zw + dz), (x, PIT_FLOOR + 0.06, zw + dz), (x, PIT_FLOOR + 0.06, zw + 0.04 + k * 0.05),
                   (x, CF - 0.06, zw + 0.04 + k * 0.05), (x, CF + 0.04, zw - 0.15), (x, CF + 0.04, ZC - CANAL_HALF + 0.04 + k * 0.05),
                   (x, OP + 0.05, ZC - CANAL_HALF + 0.04 + k * 0.05)]
            RP.conduit(b, L, pts, 0.016, "stainless", 0.9,
                       walls=[None, None, ((0, 0, 1), zw), None, ((0, 1, 0), CF), ((0, 0, 1), ZC - CANAL_HALF)])
    return (mx, zw + 0.35)


# ---------------------------------------------------------------- the car (x from the reactor-side stop)

def car(b):
    L = b.live["car"]["still"]
    # rear crossbeam (under the container's swing) carrying the trunnion plates, the central I-beam forward of it
    b.box(XP - 0.24, RAIL_Y + 0.01, ZC - 0.085, XP + 0.17, RAIL_Y + 0.06, ZC + 0.085, "stainless_dark", L)
    RP.ibeam(b, L, (XP + 0.17, RAIL_Y + 0.06, ZC), (XP + 4.36, RAIL_Y + 0.06, ZC), (0, 1, 0), 0.08, 0.08, 0.01, 0.01, "stainless_dark")
    for s in (-1, 1):
        za, zb = (ZC + 0.085, ZC + 0.15) if s > 0 else (ZC - 0.15, ZC - 0.085)
        b.box(XP - 0.09, RAIL_Y + 0.085, za, XP + 0.09, RAIL_Y + 0.11, zb, "stainless_dark", L)
        wa, wb = (za, za + 0.02) if s > 0 else (zb - 0.02, zb)
        b.box(XP - 0.03, RAIL_Y + 0.01, wa, XP + 0.03, RAIL_Y + 0.085, wb, "stainless_dark", L)
        pa, pb = (ZC + 0.13, ZC + 0.15) if s > 0 else (ZC - 0.15, ZC - 0.13)
        b.box(XP - 0.09, RAIL_Y + 0.11, pa, XP + 0.09, P_Y + 0.06, pb, "stainless_dark", L)
        RP.cyl(b, L, (XP, P_Y, ZC + s * 0.15), (XP, P_Y, ZC + s * 0.17), 0.055, "stainless_dark", "stainless_dark", 14, both=True)
    # wheels: two axles at the rear, one at the front; each wheel on its axle in a plate inside the rail
    wr = 0.04
    for xw in (XP - 0.16, XP + 0.36, XP + 4.2):
        for s in (-1, 1):
            z = ZC + s * GAUGE
            RP.cyl(b, L, (xw, RAIL_Y + wr, z - 0.01), (xw, RAIL_Y + wr, z + 0.01), wr, "stainless", "stainless_dark", 14, both=True)
            zf = z - s * 0.013
            RP.cyl(b, L, (xw, RAIL_Y + wr, zf - 0.003), (xw, RAIL_Y + wr, zf + 0.003), wr + 0.009, "stainless", "stainless", 14, both=True)
            pa, pb = (z - 0.045, z - 0.03) if s > 0 else (z + 0.03, z + 0.045)
            b.box(xw - 0.055, RAIL_Y + 0.005, pa, xw + 0.055, RAIL_Y + 0.075, pb, "stainless_dark", L)
            RP.cyl(b, L, (xw, RAIL_Y + wr, z - s * 0.045), (xw, RAIL_Y + wr, z - s * 0.01), 0.011, "stainless", "stainless", 8, both=True)
        if xw > XP:
            b.box(xw - 0.05, RAIL_Y + 0.035, ZC - GAUGE + 0.045, xw + 0.05, RAIL_Y + 0.05, ZC + GAUGE - 0.045, "stainless_dark", L)
    # front cradle under the container's top end, side guides, the retainer plate over its open end
    yc = P_Y - CONT - 0.004
    b.box(XP + 3.95, RAIL_Y + 0.1, ZC - 0.1, XP + 4.12, yc, ZC + 0.1, "stainless_dark", L)
    for s in (-1, 1):
        za, zb = (ZC + CONT + 0.006, ZC + CONT + 0.026) if s > 0 else (ZC - CONT - 0.026, ZC - CONT - 0.006)
        b.box(XP + 3.95, RAIL_Y + 0.1, za, XP + 4.12, P_Y, zb, "stainless_dark", L)
    b.box(XP + 4.32, RAIL_Y + 0.06, ZC - 0.16, XP + 4.35, P_Y + 0.16, ZC + 0.16, "stainless_dark", L)
    b.box(XP + 4.12, RAIL_Y + 0.1, ZC - 0.04, XP + 4.32, RAIL_Y + 0.13, ZC + 0.04, "stainless_dark", L)
    # pusher arm hinged on the front, to the drive chains' couplers in the tube
    RP.cyl(b, L, (XP + 4.36, RAIL_Y + 0.11, ZC - 0.05), (XP + 4.36, RAIL_Y + 0.11, ZC + 0.05), 0.02, "stainless", "stainless", 8, both=True)
    RP.obox(b, L, (XP + 4.75, RAIL_Y + 0.07, ZC), (1, -0.1, 0), (0, 1, 0), 0.4, 0.015, 0.03, "stainless_dark")
    for s in (-1, 1):
        RP.obox(b, L, (XP + 5.2, RAIL_Y + 0.025, ZC + s * 0.05), (1, 0, 0), (0, 1, 0), 0.06, 0.02, 0.012, "stainless_dark")
    tag(b, L, "CONVEYOR CAR", (XP + 2.2, RAIL_Y + 0.06, ZC + 0.007), (0, 0, 1))


def chains(b):
    """The two drive chains from the car's couplers along the tube's floor to the fuel building drive."""
    L = b.still
    for s in (-1, 1):
        z = ZC + s * 0.05
        b.box(XP + 5.26, RAIL_Y - 0.008, z - 0.008, TUBE_END, RAIL_Y + 0.004, z + 0.008, "gunmetal")


# ---------------------------------------------------------------- container and upender arms (drawn upright)

def container(b):
    L = b.live["container"]["still"]
    w, t = CONT, 0.013
    y0, y1 = CONT_BOT, CONT_TOP
    x, z = XP, ZC
    b.box(x - w, y0, z - w, x + w, y0 + 0.06, z + w, "stainless", L)                                      # bottom plate
    b.box(x - w, y0 + 0.06, z - w, x + w, y1, z - w + t, "stainless", L)
    b.box(x - w, y0 + 0.06, z + w - t, x + w, y1, z + w, "stainless", L)
    b.box(x - w, y0 + 0.06, z - w + t, x - w + t, y1, z + w - t, "stainless", L)
    b.box(x + w - t, y0 + 0.06, z - w + t, x + w, y1, z + w - t, "stainless", L)
    i = w - t
    # bands and the flow holes near the bottom
    for yb in (y0 + 0.5, y0 + 1.6, y0 + 2.7, y0 + 3.8):
        b.box(x - w - 0.008, yb - 0.03, z - w - 0.008, x + w + 0.008, yb + 0.03, z + w + 0.008, "stainless_dark", L)
    for k in range(3):
        for s in (-1, 1):
            hz = z + s * (w + 0.0015)
            b.quad((x - 0.08 + k * 0.08 - 0.015, y0 + 0.2, hz), (x - 0.08 + k * 0.08 - 0.015, y0 + 0.17, hz),
                   (x - 0.08 + k * 0.08 + 0.015, y0 + 0.17, hz), (x - 0.08 + k * 0.08 + 0.015, y0 + 0.2, hz), "black", L) if s < 0 else \
                b.quad((x - 0.08 + k * 0.08 + 0.015, y0 + 0.2, hz), (x - 0.08 + k * 0.08 + 0.015, y0 + 0.17, hz),
                       (x - 0.08 + k * 0.08 - 0.015, y0 + 0.17, hz), (x - 0.08 + k * 0.08 - 0.015, y0 + 0.2, hz), "black", L)
    # the lead-in funnel at the top
    f, h = 0.17, 0.12
    for (p, q) in (((-1, -1), (1, -1)), ((1, -1), (1, 1)), ((1, 1), (-1, 1)), ((-1, 1), (-1, -1))):
        b.quad((x + q[0] * f, y1 + h, z + q[1] * f), (x + q[0] * w, y1, z + q[1] * w), (x + p[0] * w, y1, z + p[1] * w), (x + p[0] * f, y1 + h, z + p[1] * f), "stainless", L)
        b.quad((x + p[0] * f, y1 + h, z + p[1] * f), (x + p[0] * i, y1, z + p[1] * i), (x + q[0] * i, y1, z + q[1] * i), (x + q[0] * f, y1 + h, z + q[1] * f), "stainless_dark", L)
    # trunnions, the side pins for the upender's hooks
    for s in (-1, 1):
        RP.cyl(b, L, (x, P_Y, z + s * w), (x, P_Y, z + s * 0.17), 0.035, "stainless", "stainless", 10, both=True)
        RP.cyl(b, L, (x, P_Y + 1.8, z + s * w), (x, P_Y + 1.8, z + s * (ARM_Z - 0.03)), 0.03, "stainless", "stainless", 10, both=True)
        RP.cyl(b, L, (x, P_Y + 1.8, z + s * w), (x, P_Y + 1.8, z + s * (w + 0.015)), 0.06, "stainless_dark", "stainless_dark", 12, both=True)
    tag(b, L, "FUEL CONTAINER", (x - w - 0.008, y0 + 1.05, z), (-1, 0, 0), up=(0, 1, 0))


def arms(b):
    L = b.live["arms"]["still"]
    for s in (-1, 1):
        z = ZC + s * ARM_Z
        b.box(XP - 0.06, P_Y + 0.08, z - 0.03, XP + 0.06, P_Y + ARM_L, z + 0.03, "stainless_dark", L)
        RP.cyl(b, L, (XP, P_Y, z - 0.035), (XP, P_Y, z + 0.035), 0.09, "stainless_dark", "stainless_dark", 16, both=True)
        b.box(XP - 0.05, P_Y + 0.0, z - 0.028, XP + 0.05, P_Y + 0.08, z + 0.028, "stainless_dark", L)
        # the open hook round the container's pin (a U open toward the container's top)
        hz0, hz1 = (z - 0.03, z - 0.12) if s > 0 else (z + 0.12, z + 0.03)
        za, zb = min(hz0, hz1), max(hz0, hz1)
        b.box(XP - 0.06, P_Y + 1.7, za, XP + 0.06, P_Y + 1.74, zb, "stainless_dark", L)
        for dx in (-0.05, 0.05):
            b.box(XP + dx - 0.012, P_Y + 1.74, za, XP + dx + 0.012, P_Y + 1.88, zb, "stainless_dark", L)
        # the crank on the arm's inner side (east with the arm upright), its hub on the pivot, the clevis pin at its end
        zc_ = ZC + s * CYL_Z
        RP.cyl(b, L, (XP, P_Y, zc_ - s * 0.02), (XP, P_Y, z), 0.075, "stainless_dark", "stainless_dark", 16, both=True)
        b.box(XP + 0.07, P_Y - 0.045, zc_ - 0.02, XP + CRANK + 0.04, P_Y + 0.045, zc_ + 0.02, "stainless_dark", L)
        RP.cyl(b, L, (XP + CRANK, P_Y, zc_ - 0.05), (XP + CRANK, P_Y, zc_ + 0.05), 0.016, "stainless", "stainless", 8, both=True)
        # the cross tie's brackets (behind the container, on the -x side)
        b.box(XP - 0.2, P_Y + 1.98, z - 0.025, XP - 0.06, P_Y + 2.08, z + 0.025, "stainless_dark", L)
    RP.cyl(b, L, (XP - 0.15, P_Y + 2.03, ZC - ARM_Z + 0.03), (XP - 0.15, P_Y + 2.03, ZC + ARM_Z - 0.03), 0.04, "stainless_dark", "stainless_dark", 12, both=True)


def cylinder_ops(angle_up, s):
    """One upender cylinder for the arms at angle_up (degrees: 90 upright, 0 down): body from the anchor, rod to the
    arm's lug, clevises, the hose ports and two hoses to the floor manifold."""
    out = Head()
    L = out.still
    ang = math.radians(angle_up - 90)          # the arms are drawn upright: turned by this about the pivot
    za = ZC + s * CYL_Z
    att = _rot((XP + CRANK, P_Y, za), (XP, P_Y, za), ang)
    a = (CYL_ANCHOR[0], CYL_ANCHOR[1], za)
    d = RP.unit(tuple(att[i] - a[i] for i in range(3)))
    body_end = RP.add(a, RP.mul(d, CYL_BODY))
    RP.cyl(out, L, RP.add(a, RP.mul(d, 0.05)), RP.add(a, RP.mul(d, 0.12)), 0.05, "stainless_dark", "stainless_dark", 12, both=True)
    RP.cyl(out, L, RP.add(a, RP.mul(d, 0.12)), body_end, 0.06, YELLOW, "stainless_dark", 16, both=True)
    RP.cyl(out, L, body_end, RP.add(body_end, RP.mul(d, 0.04)), 0.068, "stainless_dark", "stainless_dark", 16, both=True)
    RP.cyl(out, L, RP.add(a, RP.mul(d, 0.12)), RP.add(a, RP.mul(d, 0.16)), 0.068, "stainless_dark", "stainless_dark", 16, both=True)
    rod_end = RP.add(att, RP.mul(d, -0.06))
    RP.cyl(out, L, RP.add(body_end, RP.mul(d, 0.04)), rod_end, 0.028, "stainless", "stainless", 12, both=True)
    RP.cyl(out, L, rod_end, att, 0.035, "stainless_dark", "stainless_dark", 10, both=True)
    # ports and hoses (to the manifold at the floor by the -z wall)
    for k, t in enumerate((0.2, CYL_BODY - 0.05)):
        p = RP.add(a, RP.mul(d, t), (0, 0.065, 0))
        RP.cyl(out, L, RP.add(a, RP.mul(d, t)), p, 0.014, "stainless_dark", "stainless_dark", 8, both=False)
        mx, mz = XP + 0.75, ZC - PIT_HALF + 0.35
        q = (mx - 0.1, PIT_FLOOR + 0.06 + k * 0.03, mz)
        m1 = (p[0], p[1] + 0.05, p[2])
        m2 = ((p[0] + q[0]) / 2, PIT_FLOOR + 0.05, (p[2] + q[2]) / 2)
        for u, v in ((p, m1), (m1, m2), (m2, q)):
            RP.cyl(out, L, u, v, 0.013, "rubber", "rubber", 6, both=False)
    return out.still


# ---------------------------------------------------------------- on the operating floor

CON_X = (C + 13.5, C + 14.5)
CON_Z = (ZC - 3.95, ZC - 3.35)          # the console: its panel faces -z (the operator), the canal beyond it
HPU_X = (C + 15.0, C + 16.4)
HPU_Z = (ZC - 3.9, ZC - 2.9)


def console(b):
    L = b.still
    lit = b.lit
    xa, xb = CON_X
    zb_, zf = CON_Z[1], CON_Z[0]               # back (toward the canal) and front (the operator's side)
    yt = OP + 0.85
    b.box(xa, OP + 0.08, zf, xb, yt, zb_, BLUE)
    b.box(xa + 0.03, OP, zf + 0.03, xb - 0.03, OP + 0.08, zb_ - 0.03, "black")
    RP.obox(b, L, ((xa + xb) / 2, (OP + 0.1 + yt - 0.08) / 2, zf - 0.007), (1, 0, 0), (0, 1, 0), (xb - xa) / 2 - 0.05, (yt - OP - 0.18) / 2, 0.007, BLUE)
    RP.cyl(b, L, (xa + 0.08, OP + 0.5, zf - 0.014), (xa + 0.08, OP + 0.5, zf - 0.03), 0.016, "black", "black", 8, both=False)
    b.quad((xb - 0.2, OP + 0.27, zf - 0.0145), (xb - 0.2, OP + 0.17, zf - 0.0145), (xa + 0.2, OP + 0.17, zf - 0.0145), (xa + 0.2, OP + 0.27, zf - 0.0145),
           ["louvre", 0.05, 0.05, 0.95, 0.95])
    yf, yb = yt, yt + 0.3
    b.quad((xa, yb, zb_), (xa, yf, zf), (xb, yf, zf), (xb, yb, zb_), "paint_grey")
    for x, s in ((xa, -1), (xb, 1)):
        if s > 0:
            b.quad((x, yb, zb_), (x, yf, zf), (x, yf, zf), (x, yf, zb_), BLUE)
        else:
            b.quad((x, yb, zb_), (x, yf, zb_), (x, yf, zf), (x, yf, zf), BLUE)
    b.quad((xb, yb, zb_), (xb, yf, zb_), (xa, yf, zb_), (xa, yb, zb_), BLUE)
    sl = _unit((0, yb - yf, zb_ - zf))
    n = _unit(_cross((1, 0, 0), sl))
    if n[1] < 0:
        n = tuple(-c for c in n)
    u = RM._right(n)
    xm = (xa + xb) / 2
    P = lambda a, s_: (xm + u[0] * a, yf + sl[1] * s_, zf + sl[2] * s_)
    slen = math.dist((yf, zf), (yb, zb_))
    for s0 in (0.012, slen - 0.012):
        c = P(0, s0)
        RP.obox(b, L, tuple(c[i] + n[i] * 0.008 for i in range(3)), u, sl, (xb - xa) / 2, 0.012, 0.008, "gunmetal")
    for a in (-(xb - xa) / 2 + 0.012, (xb - xa) / 2 - 0.012):
        c = P(a, slen / 2)
        RP.obox(b, L, tuple(c[i] + n[i] * 0.008 for i in range(3)), u, sl, 0.012, slen / 2 - 0.024, 0.008, "gunmetal")
    tag(b, L, "FUEL TRANSFER SYSTEM", P(-0.15, slen - 0.045), n)
    # car position readout and the hydraulic pressure gauge on the top row
    c = P(0.33, slen - 0.075)
    b.obox(tuple(c[i] + n[i] * 0.006 for i in range(3)), u, sl, 0.08, 0.035, 0.006, ["black", None, "black", "black", "black", "black"], L)
    face = tuple(c[i] + n[i] * 0.012 for i in range(3))
    b.live["readouts"].append({"name": "car_ft", "centre": [round(v, 4) for v in face], "normal": [round(v, 4) for v in n], "char_h": 0.05, "chars": 4})
    tag(b, L, "CAR POS  FT", P(0.33, slen - 0.135), n)
    # lamps: two rows of four, their tags under them
    lamps = [("CAR AT REACTOR", "green", "car_reactor"), ("CAR IN TRANSIT", "amber", "car_transit"), ("CAR AT POOL", "green", "car_pool"),
             ("TUBE VALVE OPEN", "white", "valve_open"), ("UPENDER UP", "green", "up"), ("UPENDER DOWN", "green", "down"),
             ("RM MAST UP", "white", "rm_mast_up"), ("HPU RUNNING", "red", "hpu_running")]
    for k, (lab, col, key) in enumerate(lamps):
        a = -0.375 + 0.25 * (k % 4)
        s_ = 0.33 - 0.12 * (k // 4)
        c = P(a, s_)
        cc = tuple(c[i] + n[i] * 0.018 for i in range(3))
        b.cyl(c, cc, 0.03, "stainless", "gunmetal", 12, lst=L)
        lens = tuple(c[i] + n[i] * 0.026 for i in range(3))
        b.live["lamps"][key] = {"a": [round(v, 4) for v in cc], "b": [round(v, 4) for v in lens], "r": 0.023, "colour": col}
        tag(b, L, lab, P(a, s_ - 0.05), n)
    # gauge (live needle), controls along the front
    gc = P(-0.33, 0.47)
    RP.cyl(b, L, gc, tuple(gc[i] + n[i] * 0.02 for i in range(3)), 0.07, "black", "black", 18, both=False)
    gf = tuple(gc[i] + n[i] * 0.021 for i in range(3))
    picture(b, L, "DIAL_HYD", gf, n)
    b.live["dial"] = {"centre": [round(v, 4) for v in tuple(gf[i] + n[i] * 0.002 for i in range(3))], "normal": [round(v, 4) for v in n], "r": 0.06,
                      "low": 0, "high": 3000}
    tag(b, L, "HYD PRESS  PSIG", P(-0.09, 0.47), n)
    ctrls = [("UPENDER", "lever"), ("CAR", "lever"), ("HPU START", "push_g"), ("HPU STOP", "push_r"),
             ("PERMISSIVE", "key"), ("E-STOP", "stop")]
    for k, (lab, kind) in enumerate(ctrls):
        a = -0.4 + 0.16 * k
        c = P(a, 0.075)
        if kind == "lever":
            b.cyl(c, tuple(c[i] + n[i] * 0.012 for i in range(3)), 0.04, "gunmetal", "gunmetal", 12, lst=L)
            tip = tuple(c[i] + n[i] * 0.06 + u[i] * 0.02 for i in range(3))
            b.cyl(tuple(c[i] + n[i] * 0.012 for i in range(3)), tip, 0.008, "stainless_dark", "stainless_dark", 6, lst=L)
            b.cyl(tip, tuple(tip[i] + n[i] * 0.02 for i in range(3)), 0.016, "black", "black", 8, lst=L)
        elif kind.startswith("push"):
            b.cyl(c, tuple(c[i] + n[i] * 0.012 for i in range(3)), 0.032, "stainless", "stainless", 12, lst=L)
            b.cyl(tuple(c[i] + n[i] * 0.012 for i in range(3)), tuple(c[i] + n[i] * 0.025 for i in range(3)), 0.024,
                  sw("green") if kind == "push_g" else sw("red"), sw("green") if kind == "push_g" else sw("red"), 12, lst=L)
        elif kind == "key":
            b.cyl(c, tuple(c[i] + n[i] * 0.02 for i in range(3)), 0.028, "stainless_dark", "stainless_dark", 10, lst=L)
            b.obox(tuple(c[i] + n[i] * 0.033 for i in range(3)), u, sl, 0.006, 0.018, 0.013, "black", L)
        else:
            RP.cyl(b, L, c, tuple(c[i] + n[i] * 0.01 for i in range(3)), 0.05, sw("yellow"), sw("yellow"), 14, both=False)
            b.cyl(c, tuple(c[i] + n[i] * 0.028 for i in range(3)), 0.026, "black", "black", 10, lst=L)
            b.cyl(tuple(c[i] + n[i] * 0.028 for i in range(3)), tuple(c[i] + n[i] * 0.05 for i in range(3)), 0.04, sw("red"), sw("red"), 12, lst=L)
        tag(b, L, lab, (xm + u[0] * a, yf - 0.05, zf - 0.014), (0, 0, -1))
    tag(b, L, "REACTOR SIDE CONSOLE", (xm, yf - 0.2, zf - 0.014), (0, 0, -1))
    # the phone to the fuel building on the console's +x side
    px = xb
    b.box(px, OP + 0.95, zf + 0.15, px + 0.12, OP + 1.25, zf + 0.4, "paint_grey")
    RP.obox(b, L, (px + 0.135, OP + 1.12, zf + 0.27), (0, 1, 0), (0, 0, 1), 0.1, 0.03, 0.015, "black")
    for s in (-1, 1):
        RP.cyl(b, L, (px + 0.13, OP + 1.12 + s * 0.09, zf + 0.27), (px + 0.15, OP + 1.12 + s * 0.09, zf + 0.27), 0.03, "black", "black", 10, both=True)
    RP.cyl(b, L, (px + 0.12, OP + 1.0, zf + 0.33), (px + 0.14, OP + 0.98, zf + 0.33), 0.006, "black", "black", 6, both=False)
    tag(b, L, "PHONE  FUEL BLDG", (px + 0.121, OP + 1.21, zf + 0.275), (1, 0, 0))
    # its cable conduit to the junction box on the canal's edge
    RP.conduit(b, L, [((xa + xb) / 2, OP + 0.05, zb_), ((xa + xb) / 2, OP + 0.05, ZC - CANAL_HALF - 0.6), (XP + 1.25, OP + 0.05, ZC - CANAL_HALF - 0.6),
                      (XP + 1.25, OP + 0.05, ZC - CANAL_HALF + 0.06)], 0.02)


def picture(b, lst, name, centre, normal, up=(0, 1, 0)):
    keep = b.still
    b.still = lst
    try:
        RM.picture(b, name, centre, normal, lst, up=up, lift=0.0)
    finally:
        b.still = keep


def hpu(b, manifold_end):
    """The upender's hydraulic power unit on its drip skid: reservoir with level gauge, filler breather and return
    filter; the motor and pump on the reservoir's top; the accumulator; the valve manifold with its two solenoid valves,
    relief valve and pressure gauge; the lines to the canal's edge."""
    L = b.still
    xa, xb = HPU_X
    za, zb = HPU_Z
    # drip skid: a tray with a lip, on two channels
    for z in (za + 0.12, zb - 0.12):
        RP.channel(b, L, (xa, OP + 0.05, z), (xb, OP + 0.05, z), (0, 1, 0), 0.1, 0.05, (0, 0, 1) if z < (za + zb) / 2 else (0, 0, -1), mat=BLUE)
    b.box(xa, OP + 0.1, za, xb, OP + 0.115, zb, BLUE)
    for (x0, z0, x1, z1) in ((xa, za, xb, za + 0.015), (xa, zb - 0.015, xb, zb), (xa, za + 0.015, xa + 0.015, zb - 0.015), (xb - 0.015, za + 0.015, xb, zb - 0.015)):
        b.box(x0, OP + 0.115, z0, x1, OP + 0.18, z1, BLUE)
    y0 = OP + 0.115
    # reservoir
    ra, rb_ = xa + 0.08, xa + 0.95
    rz0, rz1 = za + 0.12, zb - 0.12
    b.box(ra, y0, rz0, rb_, y0 + 0.6, rz1, "paint_grey")
    b.box(ra - 0.01, y0 + 0.6, rz0 - 0.01, rb_ + 0.01, y0 + 0.62, rz1 + 0.01, "paint_grey")
    for x in (ra + 0.1, rb_ - 0.1):
        RP.bolt_row(b, L, (x, y0 + 0.62, rz0 + 0.03), (x, y0 + 0.62, rz1 - 0.03), (0, 1, 0), 5, 0.008, 0.006)
    # level gauge on the -z face (the operator's side), filler breather and return filter on top
    b.box(ra + 0.3, y0 + 0.1, rz0 - 0.03, ra + 0.36, y0 + 0.5, rz0, "gunmetal")
    b.quad((ra + 0.345, y0 + 0.47, rz0 - 0.031), (ra + 0.345, y0 + 0.13, rz0 - 0.031), (ra + 0.315, y0 + 0.13, rz0 - 0.031), (ra + 0.315, y0 + 0.47, rz0 - 0.031),
           ["glass", 0.2, 0.2, 0.8, 0.8])
    b.quad((ra + 0.343, y0 + 0.33, rz0 - 0.032), (ra + 0.343, y0 + 0.14, rz0 - 0.032), (ra + 0.317, y0 + 0.14, rz0 - 0.032), (ra + 0.317, y0 + 0.33, rz0 - 0.032), sw("orange"))
    tag(b, L, "RESERVOIR", (ra + 0.62, y0 + 0.45, rz0), (0, 0, -1))
    RP.cyl(b, L, (ra + 0.15, y0 + 0.62, (rz0 + rz1) / 2 - 0.2), (ra + 0.15, y0 + 0.72, (rz0 + rz1) / 2 - 0.2), 0.04, "gunmetal", "gunmetal", 12, both=False)
    RP.cyl(b, L, (ra + 0.15, y0 + 0.72, (rz0 + rz1) / 2 - 0.2), (ra + 0.15, y0 + 0.76, (rz0 + rz1) / 2 - 0.2), 0.055, "gunmetal", "gunmetal", 12, both=True)
    RP.cyl(b, L, (rb_ - 0.12, y0 + 0.62, rz1 - 0.12), (rb_ - 0.12, y0 + 0.86, rz1 - 0.12), 0.07, "paint_blue", "gunmetal", 14, both=False)
    # motor (vertical, flanged on the pump's bell housing on the reservoir's top)
    mx, mz = ra + 0.45, (rz0 + rz1) / 2 + 0.05
    RP.cyl(b, L, (mx, y0 + 0.62, mz), (mx, y0 + 0.8, mz), 0.11, "paint_grey", "paint_grey", 16, both=False)
    RP.motor(b, L, (mx, y0 + 0.8, mz), (0, 1, 0), 0.55, 0.14, BLUE)
    tag(b, L, "HPU MOTOR", (mx, y0 + 0.92, mz - 0.155), (0, 0, -1))
    # accumulator: a bottle on a bracket on the skid's +x end, gas valve on top
    ax = xb - 0.18
    az = (za + zb) / 2 + 0.2
    RP.cyl(b, L, (ax, y0 + 0.02, az), (ax, y0 + 0.75, az), 0.11, "red_iron", "red_iron", 16, both=True)
    RP.cyl(b, L, (ax, y0 + 0.75, az), (ax, y0 + 0.8, az), 0.07, "red_iron", "red_iron", 14, both=True)
    RP.cyl(b, L, (ax, y0 + 0.8, az), (ax, y0 + 0.86, az), 0.018, "copper", "copper", 8, both=True)
    b.box(ax - 0.13, y0 + 0.35, az - 0.13, ax + 0.13, y0 + 0.39, az - 0.112, "gunmetal")
    # valve manifold on a stand at the skid's +x end, two solenoid valves, relief valve, gauge
    vx = xb - 0.2
    vz = za + 0.3
    b.box(vx - 0.03, y0, vz - 0.03, vx + 0.03, y0 + 0.45, vz + 0.03, "gunmetal")
    b.box(vx - 0.13, y0 + 0.45, vz - 0.09, vx + 0.13, y0 + 0.6, vz + 0.09, "stainless_dark")
    for dx in (-0.065, 0.065):
        b.box(vx + dx - 0.05, y0 + 0.6, vz - 0.07, vx + dx + 0.05, y0 + 0.68, vz + 0.07, "paint_blue")
        b.box(vx + dx - 0.035, y0 + 0.68, vz - 0.11, vx + dx + 0.035, y0 + 0.75, vz - 0.04, "black")
        b.box(vx + dx - 0.035, y0 + 0.68, vz + 0.04, vx + dx + 0.035, y0 + 0.75, vz + 0.11, "black")
    RP.cyl(b, L, (vx + 0.13, y0 + 0.53, vz), (vx + 0.2, y0 + 0.53, vz), 0.02, "copper", "copper", 8, both=True)
    gc = (vx, y0 + 0.53, vz - 0.09)
    RP.cyl(b, L, gc, (gc[0], gc[1], gc[2] - 0.05), 0.012, "stainless", "stainless", 8, both=False)
    RP.cyl(b, L, (gc[0], gc[1], gc[2] - 0.05), (gc[0], gc[1], gc[2] - 0.08), 0.06, "black", "black", 16, both=False)
    picture(b, L, "DIAL_HYD", (gc[0], gc[1], gc[2] - 0.081), (0, 0, -1))
    b.live["dial_hpu"] = {"centre": [gc[0], gc[1], round(gc[2] - 0.083, 4)], "normal": [0, 0, -1], "r": 0.06, "low": 0, "high": 3000}
    tag(b, L, "UPENDER HPU", ((xa + xb) / 2 - 0.2, OP + 0.15, za - 0.001), (0, 0, -1))
    # pipes: pump to manifold, manifold to the canal edge (the lines down the pit wall), return to the filter
    RP.conduit(b, L, [(mx + 0.11, y0 + 0.7, mz), (vx - 0.13, y0 + 0.7, mz), (vx - 0.13, y0 + 0.52, vz)], 0.014, "stainless")
    for k in range(2):
        x = XP + 1.6 + k * 0.08
        z_edge = ZC - CANAL_HALF + 0.04 + k * 0.05
        RP.conduit(b, L, [(vx - 0.05 + k * 0.1, y0 + 0.45, vz - 0.09), (vx - 0.05 + k * 0.1, OP + 0.05 + k * 0.04, vz - 0.09),
                          (vx - 0.05 + k * 0.1, OP + 0.05 + k * 0.04, ZC - CANAL_HALF - 0.4 - k * 0.06),
                          (x, OP + 0.05 + k * 0.04, ZC - CANAL_HALF - 0.4 - k * 0.06), (x, OP + 0.05 + k * 0.04, z_edge)], 0.016, "stainless", 0.9)


def blind_flange_stand(b):
    """The transfer tube's blind flange, off for refuelling, on its storage stand on the +z side of the canal."""
    L = b.still
    x, z = C + 14.2, ZC + 3.2
    yc = OP + 0.62
    # the A-frame stand
    for s in (-1, 1):
        zs = z + s * 0.18
        for dx in (-0.4, 0.4):
            RP.cyl(b, L, (x + dx, OP + 0.02, zs), (x + dx * 0.15, yc - 0.1, zs), 0.025, YELLOW, YELLOW, 8, both=False)
        b.box(x - 0.46, OP, zs - 0.04, x + 0.46, OP + 0.02, zs + 0.04, YELLOW)
        b.box(x - 0.09, yc - 0.12, zs - 0.03, x + 0.09, yc - 0.06, zs + 0.03, YELLOW)
    RP.cyl(b, L, (x, yc - 0.09, z - 0.2), (x, yc - 0.09, z + 0.2), 0.02, "gunmetal", "gunmetal", 8, both=True)
    # the flange standing in it: a disc with its bolt holes, gasket face, lifting eye
    RP.cyl(b, L, (x, yc + 0.32, z - 0.05), (x, yc + 0.32, z + 0.05), 0.4, "stainless", "stainless", 28, both=True)
    for s in (-1, 1):
        RP.cyl(b, L, (x, yc + 0.32, z + s * 0.05), (x, yc + 0.32, z + s * 0.051), 0.3, "stainless_dark", "stainless_dark", 24, both=False)
        for k in range(20):
            a = 2 * math.pi * (k + 0.5) / 20
            p = (x + 0.35 * math.cos(a), yc + 0.32 + 0.35 * math.sin(a), z + s * 0.0501)
            RP.cyl(b, L, p, (p[0], p[1], p[2] + s * 0.0005), 0.016, "black", "black", 8, both=False)
    RP.cyl(b, L, (x, yc + 0.72, z), (x, yc + 0.78, z), 0.015, "stainless", "stainless", 8, both=False)
    RP.cyl(b, L, (x, yc + 0.8, z - 0.025), (x, yc + 0.8, z + 0.025), 0.03, "stainless", "stainless", 10, both=True)
    b.box(x - 0.42, OP + 0.22, z - 0.24, x + 0.42, OP + 0.34, z - 0.22, YELLOW)
    tag(b, L, "TRANSFER TUBE BLIND FLANGE", (x, OP + 0.28, z - 0.24), (0, 0, -1))
    # its bolts and nuts in a tray beside it
    b.box(x + 0.55, OP, z - 0.2, x + 0.85, OP + 0.08, z + 0.2, "gunmetal")
    for i in range(4):
        for j in range(5):
            RP.cyl(b, L, (x + 0.6 + i * 0.065, OP + 0.08, z - 0.16 + j * 0.08), (x + 0.6 + i * 0.065, OP + 0.1, z - 0.16 + j * 0.08), 0.018, "stainless_dark", "stainless_dark", 6, both=False)


def build():
    b = Transfer()
    rails(b)
    tube_end(b)
    upender_base(b)
    mf = hydraulic_lines(b)
    chains(b)
    b.pit_end = len(b.still)
    console(b)
    hpu(b, mf)
    blind_flange_stand(b)
    b.core = len(b.still)
    car(b)
    container(b)
    arms(b)
    RM.fuel_assembly(b, b.live["fuel"]["still"])
    return b


def placed(b, car_x=0.0, angle_up=90.0, fuel=True, readings=None, lamps=(), pressure=0.0, deck=True, pit=True):
    """The system for a still picture: the car car_x along the rails from the reactor-side stop, the container and
    arms at angle_up (90 upright), an assembly in the container; the console's readouts, lamps, gauge needles."""
    lv = b.live
    out = {"still": [], "lit": []}
    core = b.core
    if pit:
        out["still"] += b.still[:b.pit_end]
    if deck:
        out["still"] += b.still[b.pit_end:core]
        out["lit"] += list(b.lit)
    ang = math.radians(angle_up - 90)
    piv = (XP, P_Y, ZC)
    out["still"] += RM.moved(lv["car"]["still"], car_x, 0, 0)
    out["still"] += turned(lv["arms"]["still"], piv, ang)
    out["still"] += turned(lv["container"]["still"], piv, ang, car_x if angle_up < 1 else 0.0)
    if fuel:
        f = RM.moved(lv["fuel"]["still"], XP, RM.CORE_BOT, ZC)
        out["still"] += turned(f, piv, ang, car_x if angle_up < 1 else 0.0)
    for s in (-1, 1):
        out["still"] += cylinder_ops(angle_up, s)
    lens = {"green": "lamp_green", "white": "lamp_white", "red": "lamp_red", "amber": "lamp_amber"}
    off = {"green": sw("green"), "white": sw("enamel"), "red": sw("red"), "amber": sw("orange")}
    for key, lp in (lv["lamps"].items() if deck else ()):
        on = key in lamps
        mat = pic(lens[lp["colour"]]) if on else off[lp["colour"]]
        out["lit" if on else "still"].append(["c", *CB._r(*lp["a"], *lp["b"], lp["r"]), 12, "gunmetal", mat])
    for name, text in ((readings or {}).items() if deck else ()):
        for r in lv["readouts"]:
            if r["name"] == name:
                out["lit"] += RM.led_ops(r["centre"], r["normal"], r["char_h"], text.rjust(r["chars"]))
    for key in ("dial", "dial_hpu"):
        dl = lv.get(key)
        if deck and dl:
            out["still"] += needle_ops(dl["centre"], dl["normal"], dl["r"], dl["low"], dl["high"], pressure)
    return out


def needle_ops(centre, n, r, lo, hi, value):
    """A dial's needle as the client draws it."""
    th = math.radians(-135 + 270 * (value - lo) / (hi - lo))
    n = _unit(n)
    up = (0, 1, 0) if abs(n[1]) < 0.9 else (0, 0, 1)
    u = RM._right(n, up)
    v = _unit(_cross(u, n))
    v = tuple(-c for c in v)
    if RH._dot(v, up) < 0:
        v = tuple(-c for c in v)
    d = tuple(math.sin(th) * u[i] + math.cos(th) * v[i] for i in range(3))
    c = tuple(centre[i] + d[i] * r * 0.4 + n[i] * 0.004 for i in range(3))
    h = Head()
    w = _unit(_cross(d, n))
    h.obox(c, d, w, r * 0.45, 0.004, 0.002, "black")
    return h.still


def ops_check():
    b = build()
    bad = len(CB.overlaps(b.ops()))
    for g in ("car", "container", "arms", "fuel"):
        bad += len(CB.overlaps({"still": b.live[g]["still"], "lit": [], "blink": []}))
    for ang, cx in ((90, 0), (0, 0), (0, 3.0)):          # the rest states (in between, it is moving)
        o = placed(b, cx, ang)
        bad += len(CB.overlaps({"still": o["still"], "lit": o["lit"], "blink": []}))
    return bad


if __name__ == "__main__":
    print("overlaps", ops_check())
