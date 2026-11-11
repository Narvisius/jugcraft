"""The refuelling floor's stands, racks and the stud tensioner (reactor building rebuild, machine 5).

Built like a Westinghouse four-loop plant's refuelling equipment:

- The reactor vessel head's storage stand on the operating deck (the head package goes to a dry storage stand while
  refuelling): a ring girder on six columns with their base plates and bracing, alignment posts, a work platform
  round it at the flange's underside (stairs at a real angle, handrail) for the O-ring and flange work.
- The upper internals (lifted out at every refuelling and stored underwater): upper support plate in its top hat with
  the flange and the hold-down spring, the guide tubes at the control rod locations, the support columns, the upper
  core plate, thermocouple columns, lifting lugs; and their lifting rig (three legs with latching tools, the spider,
  the crane's pin, guide sleeves).
- The upper internals storage stand in a deeper bay off the cavity's west side (US4859404: concrete supports with
  steel pedestal assemblies, bearing pads under the upper core plate, chamfered guide plates, guide studs on the walls).
- The 54 head studs out of the vessel: two stud racks (numbered slots) and the nut and washer rack; the stud hole plugs
  in the vessel flange.
- The stud tensioner (Westinghouse TENSOR-style): a carousel monorail round the head on brackets off the head package,
  four tensioners on trolleys and air hoists, the hydraulic hoses to a distribution manifold, and the pump and control
  console (digital pressure readout, stroke lamps, air valves); the tensioners' storage rack on the deck.

Frame: the reactor building's (tools/reactor_head.py). The internals are drawn where they sit in the vessel (axis
(C, C)) and placed on their stand by IN_STAND; the head is placed on its stand by HEAD_AT. Live (LIVE): where the
head and the internals are (vessel, crane, stand), which rack slots hold studs, the tensioners' places on the
carousel, the console's pressure readout (psi, to 14,500) and stroke lamps.
"""
import math

import control_boards as CB
from control_boards import sw, pic
import reactor_head as RH
import rb_parts as RP
import refuelling_machine as RM
from reactor_head import Head, C, P, legend, legend_size, _cross, _unit

OP = RM.OP
CF = RH.CAVITY_FLOOR
H = RH.CAVITY_HALF
YELLOW = "paint_yellow"
BLUE = "paint_blue"

# the upper internals' storage bay: west of the cavity, through an opening in the cavity's west wall, deeper
BAY_X = (C - 12.9, C - H)
BAY_Z = (C + 1.3, C + H)
BAY_FLOOR = 9.5
UI_X, UI_Z = C - 9.7, C + 3.9                 # the stand's centre
# the upper internals, as they sit in the vessel
UCP = (6.12, 6.2)                             # upper core plate
USP = (10.65, 10.9)                           # upper support plate
HAT_TOP = 11.6
FLANGE = (11.6, 11.8)
SPRING = (11.8, 11.92)
R_UI = 2.15
PAD_TOP = 10.45                               # the stand's bearing pads' top (the upper core plate sits on them)
IN_STAND = (UI_X - C, PAD_TOP - UCP[0], UI_Z - C)
# the head's storage stand on the deck
HX, HZ = C - 13.6, C - 10.6          # north-west of the cavity, west of steam generator 3's compartment
STAND_TOP = OP + 1.8                          # the head flange's underside on the stand
HEAD_AT = (HX - C, STAND_TOP - RH.FLANGE_TOP, HZ - C)
PLAT_Y = OP + 0.75


class Floor(Head):
    def __init__(self):
        CB.Board.__init__(self, 21, 505)
        self.live = {"internals": {"still": []}, "rig": {"still": []}, "tensioners": [], "studs": [], "lamps": {}, "readouts": []}


def tag(b, lst, text, c, n, up=(0, 1, 0)):
    keep = b.still
    b.still = lst
    try:
        b.tag(text, c, n, up=up)
    finally:
        b.still = keep


def moved(ops, d):
    return RM.moved(ops, *d)


# ---------------------------------------------------------------- the upper internals (as in the vessel)

def rcca_positions():
    """The 53 control rod locations in the core (core pitch, checkerboard within the core's round)."""
    p = RH.CORE_PITCH
    pts = []
    for i in range(-7, 8):
        for j in range(-7, 8):
            if (i + j) % 2 == 0 and (i % 2 == 0) and math.hypot(i, j) * p < 1.45:
                pts.append((i * p, j * p))
    pts.sort(key=lambda q: (math.hypot(*q), q))
    return pts[:53]


def column_positions():
    p = RH.CORE_PITCH
    rc = set((round(x, 4), round(z, 4)) for x, z in rcca_positions())
    pts = []
    for i in range(-7, 8):
        for j in range(-7, 8):
            if (i % 2 == 1) and (j % 2 == 1) and math.hypot(i, j) * p < 1.55:
                pts.append((i * p, j * p))
    return [q for q in pts if (round(q[0], 4), round(q[1], 4)) not in rc][:32]


def internals(b):
    L = b.live["internals"]["still"]
    keep = b.still
    b.still = L
    try:
        # upper core plate (its underside: the fuel's alignment pins), its rim's bolts
        b.tube(2.1, UCP[0], UCP[1], "stainless_dark")
        b.ring_top(0.0, 2.1, UCP[1], "stainless")
        b.ring_top(0.0, 2.1, UCP[0], "stainless", down=True)
        for x, z in rcca_positions()[::3]:
            RP.cyl(b, L, (C + x + 0.07, UCP[0], C + z), (C + x + 0.07, UCP[0] - 0.06, C + z), 0.012, "stainless", "stainless", 6, both=False)
        # the guide tubes: lower section, mid flange, upper section through the support plate, top flange
        for x, z in rcca_positions():
            X, Z = C + x, C + z
            b.box(X - 0.075, UCP[1] + 0.05, Z - 0.075, X + 0.075, 9.2, Z + 0.075, "stainless")
            b.box(X - 0.1, 9.2, Z - 0.1, X + 0.1, 9.26, Z + 0.1, "stainless_dark")
            b.box(X - 0.07, 9.26, Z - 0.07, X + 0.07, USP[0], Z + 0.07, "stainless")
            b.box(X - 0.065, USP[1], Z - 0.065, X + 0.065, 11.42, Z + 0.065, "stainless")
            b.box(X - 0.085, 11.42, Z - 0.085, X + 0.085, 11.46, Z + 0.085, "stainless_dark")
            b.box(X - 0.1, UCP[1], Z - 0.1, X + 0.1, UCP[1] + 0.05, Z + 0.1, "stainless_dark")
        # support columns with their feet and the 4 thermocouple columns
        for x, z in column_positions():
            X, Z = C + x, C + z
            RP.cyl(b, L, (X, UCP[1], Z), (X, UCP[1] + 0.06, Z), 0.085, "stainless_dark", "stainless_dark", 12, both=False)
            RP.cyl(b, L, (X, UCP[1] + 0.06, Z), (X, USP[0], Z), 0.055, "stainless", "stainless", 10, both=False)
        tc = [(1.25, 0.0), (-1.25, 0.0), (0.0, 1.25), (0.0, -1.25)]
        for x, z in tc:
            X, Z = C + x, C + z
            RP.cyl(b, L, (X, UCP[1], Z), (X, 11.9, Z), 0.045, "stainless", "stainless", 10, both=False)
            RP.cyl(b, L, (X, 11.5, Z), (X, 11.56, Z), 0.07, "stainless_dark", "stainless_dark", 10, both=True)
            RP.cyl(b, L, (X, 11.9, Z), (X, 11.95, Z), 0.06, "stainless_dark", "stainless_dark", 10, both=False)
        # upper support plate in its top hat, the flange and the hold-down spring
        b.tube(1.9, USP[0], USP[1], "stainless_dark")
        b.ring_top(0.0, 1.9, USP[1], "stainless")
        b.ring_top(0.0, 1.9, USP[0], "stainless", down=True)
        b.tube(1.9, USP[1], HAT_TOP, "stainless")
        b.tube(1.86, USP[1], HAT_TOP, "stainless_dark", inward=True)
        b.tube(R_UI, FLANGE[0], FLANGE[1], "stainless_dark")
        b.ring_top(1.86, R_UI, FLANGE[1], "stainless")
        b.ring_top(1.9, R_UI, FLANGE[0], "stainless", down=True)
        b.tube(2.11, SPRING[0], SPRING[1], "stainless_dark")
        b.tube(2.0, SPRING[0], SPRING[1], "stainless_dark", inward=True)
        b.ring_top(2.0, 2.11, SPRING[1], "stainless_dark")
        # lifting lugs on the flange, alignment keys round its edge
        for a in RH.LUGS:
            c = P(1.98, a, FLANGE[1] + 0.12)
            n = (math.cos(a), 0, math.sin(a))
            t = (-math.sin(a), 0, math.cos(a))
            RP.obox(b, L, c, t, (0, 1, 0), 0.1, 0.12, 0.03, "stainless_dark")
            RP.cyl(b, L, RP.add(c, RP.mul(n, -0.032), (0, 0.04, 0)), RP.add(c, RP.mul(n, 0.032), (0, 0.04, 0)), 0.04, "black", "black", 10, both=True)
        for k in range(4):
            a = math.radians(45 + 90 * k)
            c = P(R_UI + 0.03, a, (FLANGE[0] + FLANGE[1]) / 2)
            RP.obox(b, L, c, (-math.sin(a), 0, math.cos(a)), (0, 1, 0), 0.06, 0.09, 0.03, "stainless_dark")
    finally:
        b.still = keep


def lifting_rig(b):
    """The internals' lifting rig (as on the internals in the vessel): three legs from the lugs to the spider, their
    latching tools on the lugs, the hub and the crane's pin, two arms to the guide sleeves that take the guide studs."""
    L = b.live["rig"]["still"]
    hub_y = SPRING[1] + 3.2
    hub = (C, hub_y, C)
    for a in RH.LUGS:
        foot = P(1.98, a, FLANGE[1] + 0.36)
        RP.obox(b, L, P(1.98, a, FLANGE[1] + 0.3), (-math.sin(a), 0, math.cos(a)), (0, 1, 0), 0.07, 0.08, 0.06, YELLOW)
        top = P(0.42, a, hub_y - 0.1)
        RP.cyl(b, L, foot, top, 0.06, YELLOW, YELLOW, 10, both=False)
        mid = RP.add(RP.mul(foot, 0.5), RP.mul(top, 0.5))
    RP.cyl(b, L, (C, hub_y - 0.2, C), (C, hub_y + 0.25, C), 0.45, YELLOW, YELLOW, 18, both=True)
    RP.cyl(b, L, (C, hub_y + 0.25, C), (C, hub_y + 0.5, C), 0.16, "stainless_dark", "stainless_dark", 12, both=True)
    RP.cyl(b, L, (C - 0.12, hub_y + 0.62, C), (C + 0.12, hub_y + 0.62, C), 0.06, "stainless", "stainless", 10, both=True)
    for s in (-1, 1):
        RP.obox(b, L, (C + s * 0.1, hub_y + 0.56, C), (0, 1, 0), (0, 0, 1), 0.08, 0.08, 0.02, "stainless_dark")
    # guide arms and sleeves (to the guide studs on the bay's +z and -x walls)
    for a, r in ((math.radians(90), 2.45), (math.radians(180), 3.05)):
        RP.obox(b, L, P(r / 2 + 0.2, a, hub_y - 0.6), (math.cos(a), 0, math.sin(a)), (0, 1, 0), r / 2 - 0.2, 0.05, 0.05, YELLOW)
        RP.cyl(b, L, P(0.42, a, hub_y - 0.1), P(r - 0.3, a, hub_y - 0.6), 0.03, YELLOW, YELLOW, 8, both=False)
        RP.cyl(b, L, P(r, a, hub_y - 1.0), P(r, a, hub_y - 0.2), 0.09, YELLOW, "stainless_dark", 12, both=True)
    RP.obox(b, L, (C, hub_y + 0.02, C - 0.47), (1, 0, 0), (0, 1, 0), 0.42, 0.06, 0.02, YELLOW)
    tag(b, L, "UPPER INTERNALS LIFTING RIG", (C, hub_y + 0.02, C - 0.49), (0, 0, -1))


# ---------------------------------------------------------------- the stands

def internals_stand(b):
    """Four concrete supports with their steel pedestal assemblies (embedment, middle and adjustable top plates, two
    bearing pads, chamfered guide plates), and the two guide studs on the bay's walls."""
    L = b.still
    for k in range(4):
        a = math.radians(45 + 90 * k)
        cx, cz = UI_X + 1.75 * math.cos(a), UI_Z + 1.75 * math.sin(a)
        u = (math.cos(a), 0, math.sin(a))
        t = (-math.sin(a), 0, math.cos(a))
        RP.obox(b, L, (cx, (BAY_FLOOR + 10.3) / 2, cz), u, (0, 1, 0), 0.32, (10.3 - BAY_FLOOR) / 2, 0.32, "concrete")
        RP.obox(b, L, (cx, 10.3 + 0.02, cz), u, (0, 1, 0), 0.28, 0.02, 0.28, "stainless_dark")
        RP.obox(b, L, (cx, 10.34 + 0.02, cz), u, (0, 1, 0), 0.25, 0.02, 0.25, "stainless_dark")
        for s1 in (-1, 1):
            for s2 in (-1, 1):
                RP.bolt(b, L, (cx + u[0] * s1 * 0.2 + t[0] * s2 * 0.2, 10.38, cz + u[2] * s1 * 0.2 + t[2] * s2 * 0.2), (0, 1, 0), 0.012, 0.01, False)
        for s in (-1, 1):
            pc = (cx + t[0] * s * 0.11 - u[0] * 0.05, PAD_TOP - 0.035, cz + t[2] * s * 0.11 - u[2] * 0.05)
            RP.obox(b, L, pc, u, (0, 1, 0), 0.1, 0.035, 0.07, "stainless")
        # guide plates outside the core plate's rim (radially out from the stand's centre), chamfered tops
        gx, gz = UI_X + 2.17 * math.cos(a), UI_Z + 2.17 * math.sin(a)
        for s in (-1, 1):
            gc = (gx + t[0] * s * 0.12, 10.38 + 0.35, gz + t[2] * s * 0.12)
            RP.obox(b, L, gc, u, (0, 1, 0), 0.025, 0.35, 0.03, "stainless_dark")
        gb = (gx + u[0] * 0.08, 10.36, gz + u[2] * 0.08)
        RP.obox(b, L, (gb[0], 10.35 + 0.02, gb[2]), u, (0, 1, 0), 0.1, 0.02, 0.2, "stainless_dark")
    # guide studs on the walls
    for (x, z, n) in ((UI_X, BAY_Z[1] - 0.15, (0, 0, -1)), (BAY_X[0] + 0.15, UI_Z, (1, 0, 0))):
        RP.cyl(b, L, (x, BAY_FLOOR + 0.3, z), (x, 18.6, z), 0.06, "stainless", "stainless", 12, both=False)
        RP.cyl(b, L, (x, 18.6, z), (x, 18.9, z), 0.04, "stainless", "stainless", 10, both=False)
        for y in (BAY_FLOOR + 1.0, 13.5, 17.5):
            w = (z - n[2] * 0.15, x - n[0] * 0.15)
            RP.obox(b, L, (x - n[0] * 0.075, y, z - n[2] * 0.075), n, (0, 1, 0), 0.075, 0.05, 0.05, "stainless_dark")
        tag(b, L, "GUIDE STUD", (x - n[0] * 0.149 + (0.25 if n[2] else 0), 12.5, z - n[2] * 0.149 + (0.25 if n[0] else 0)), n)
    tag(b, L, "UPPER INTERNALS STAND", (UI_X, 11.0, BAY_Z[1] - 0.001), (0, 0, -1))
    b.stencil("UPPER INTERNALS", (UI_X, 14.5, BAY_Z[1]), (0, 0, -1))


def head_stand(b):
    """The head's storage stand: a box ring girder on six columns (base plates, anchors, bracing), alignment posts,
    the work platform round it with stairs and a handrail, the stand's tag."""
    L = b.still
    yt = STAND_TOP
    yg = yt - 0.4
    # ring girder: outer and inner webs, top and bottom flanges (as bands about the stand's axis)
    def band(r0, y0, r1, y1, mat, inward=False):
        n = 48
        for i in range(n):
            a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            if inward:
                a0, a1 = a1, a0
            q = lambda r, a, y: (HX + r * math.cos(a), y, HZ + r * math.sin(a))
            b.quad(q(r1, a0, y1), q(r0, a0, y0), q(r0, a1, y0), q(r1, a1, y1), RH.tile_mat(mat, 0, 0, min(1, 2 * math.pi * max(r0, r1) / n), min(1, abs(y1 - y0) + abs(r1 - r0))), L)
    def ring(r0, r1, y, mat, down=False):
        n = 48
        for i in range(n):
            a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
            if down:
                a0, a1 = a1, a0
            q = lambda r, a: (HX + r * math.cos(a), y, HZ + r * math.sin(a))
            b.quad(q(r0, a0), q(r1, a0), q(r1, a1), q(r0, a1), RH.tile_mat(mat, 0, 0, min(1, 2 * math.pi * r1 / n), min(1, r1 - r0)), L)
    band(3.0, yg, 3.0, yt, YELLOW)
    band(2.45, yg, 2.45, yt, YELLOW, inward=True)
    ring(2.45, 3.0, yt, YELLOW)
    ring(2.45, 3.0, yg, YELLOW, down=True)
    for k in range(12):
        a = 2 * math.pi * (k + 0.5) / 12
        c = (HX + 3.01 * math.cos(a), (yg + yt) / 2, HZ + 3.01 * math.sin(a))
        RP.obox(b, L, c, (-math.sin(a), 0, math.cos(a)), (0, 1, 0), 0.012, 0.19, 0.01, YELLOW)
    # columns (I-sections) with base plates and anchors, bracing between them
    cols = [math.radians(30 + 60 * k) for k in range(6)]
    for a in cols:
        cx, cz = HX + 2.72 * math.cos(a), HZ + 2.72 * math.sin(a)
        RP.ibeam(b, L, (cx, OP + 0.03, cz), (cx, yg, cz), (math.cos(a), 0, math.sin(a)), 0.25, 0.25, 0.02, 0.016, YELLOW)
        RP.obox(b, L, (cx, OP + 0.015, cz), (math.cos(a), 0, math.sin(a)), (0, 1, 0), 0.22, 0.015, 0.22, "gunmetal")
        for s1 in (-1, 1):
            for s2 in (-1, 1):
                RP.bolt(b, L, (cx + math.cos(a) * s1 * 0.17 - math.sin(a) * s2 * 0.17, OP + 0.03, cz + math.sin(a) * s1 * 0.17 + math.cos(a) * s2 * 0.17), (0, 1, 0), 0.014, 0.014, True)
    for k in range(6):
        if k == 4:
            continue                      # the stair's bay is open
        a0, a1 = cols[k], cols[(k + 1) % 6]
        p0 = (HX + 2.72 * math.cos(a0), OP + 0.25, HZ + 2.72 * math.sin(a0))
        p1 = (HX + 2.72 * math.cos(a1), yg - 0.1, HZ + 2.72 * math.sin(a1))
        q0 = (HX + 2.72 * math.cos(a0), yg - 0.1, HZ + 2.72 * math.sin(a0))
        q1 = (HX + 2.72 * math.cos(a1), OP + 0.25, HZ + 2.72 * math.sin(a1))
        RP.cyl(b, L, p0, p1, 0.025, YELLOW, YELLOW, 8, both=False)
        RP.cyl(b, L, q0, q1, 0.025, YELLOW, YELLOW, 8, both=False)
    # alignment posts outside the head flange
    for a in (math.radians(0), math.radians(120), math.radians(240)):
        x, z = HX + 2.92 * math.cos(a), HZ + 2.92 * math.sin(a)
        RP.cyl(b, L, (x, yt, z), (x, yt + 0.55, z), 0.05, "stainless", "stainless", 10, both=False)
        RP.cyl(b, L, (x, yt + 0.55, z), (x, yt + 0.62, z), 0.035, "stainless", "stainless", 10, both=False)
    # work platform round the stand: a grating ring on brackets from the columns, handrail outside, stairs down
    pr0, pr1 = 3.12, 3.95
    n = 24
    for i in range(n):
        a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        q = lambda r, a, y: (HX + r * math.cos(a), y, HZ + r * math.sin(a))
        b.quad(q(pr0, a0, PLAT_Y), q(pr1, a0, PLAT_Y), q(pr1, a1, PLAT_Y), q(pr0, a1, PLAT_Y), ["grating", 0.02, 0.02, 0.98, 0.98], L)
        b.quad(q(pr0, a1, PLAT_Y - 0.03), q(pr1, a1, PLAT_Y - 0.03), q(pr1, a0, PLAT_Y - 0.03), q(pr0, a0, PLAT_Y - 0.03), ["grating", 0.02, 0.02, 0.98, 0.98], L)
    for a in cols:
        c = (HX + (2.72 + 3.95) / 2 * math.cos(a), PLAT_Y - 0.07, HZ + (2.72 + 3.95) / 2 * math.sin(a))
        RP.obox(b, L, c, (math.cos(a), 0, math.sin(a)), (0, 1, 0), (3.95 - 2.72) / 2, 0.04, 0.03, YELLOW)
        RP.cyl(b, L, (HX + 3.9 * math.cos(a), PLAT_Y - 0.11, HZ + 3.9 * math.sin(a)), (HX + 2.85 * math.cos(a), OP + 0.25, HZ + 2.85 * math.sin(a)), 0.025, YELLOW, YELLOW, 8, both=False)
    stair_a = math.radians(30 + 60 * 4 + 30)        # between columns 4 and 5
    pts = []
    m = 28
    for i in range(m + 1):
        a = 2 * math.pi * i / m
        pts.append((HX + 3.9 * math.cos(a), HZ + 3.9 * math.sin(a)))
    gap_t = (stair_a / (2 * math.pi))
    RP.handrail(b, L, pts[:-1], PLAT_Y, closed=True, gaps=[(int(gap_t * m), 0.0, 1.0)], post_pitch=1.0)
    # the stair: straight out from the platform's edge at stair_a, down to the deck (risers 1/6, treads 1/4)
    u = (math.cos(stair_a), 0, math.sin(stair_a))
    t = (-math.sin(stair_a), 0, math.cos(stair_a))
    start = (HX + 3.95 * math.cos(stair_a), PLAT_Y, HZ + 3.95 * math.sin(stair_a))
    rise = PLAT_Y - OP
    nst = int(round(rise / (1 / 6)))
    rr = rise / nst
    for k in range(1, nst):
        c = RP.add(start, RP.mul(u, 0.25 * k - 0.125), (0, -rr * k, 0))
        RP.obox(b, L, RP.add(c, (0, -0.015, 0)), u, (0, 1, 0), 0.125, 0.015, 0.38, "grating")
        RP.obox(b, L, RP.add(c, RP.mul(u, -0.12), (0, -0.036, 0)), u, (0, 1, 0), 0.01, 0.03, 0.375, "gunmetal")
    run = 0.25 * nst
    for s in (-1, 1):
        a0 = RP.add(start, RP.mul(t, s * 0.4))
        a1 = RP.add(a0, RP.mul(u, run), (0, -rise, 0))
        RP.channel(b, L, RP.add(a0, (0, -0.06, 0)), RP.add(a1, (0, 0.06, 0)), RP.unit(RP.cross(RP.unit(RP.add(a1, RP.mul(a0, -1))), RP.mul(t, s))), 0.15, 0.05, RP.mul(t, -s), mat=YELLOW)
        RP.cyl(b, L, RP.add(a0, (0, 1.0, 0)), RP.add(a1, (0, 1.0, 0)), 0.022, YELLOW, YELLOW, 8, both=False)
        RP.cyl(b, L, RP.add(a1, (0, 0.0, 0)), RP.add(a1, (0, 1.0, 0)), 0.022, YELLOW, YELLOW, 8, both=False)
        RP.cyl(b, L, RP.add(a0, (0, 0.0, 0)), RP.add(a0, (0, 1.0, 0)), 0.022, YELLOW, YELLOW, 8, both=False)
    ta = math.radians(30 + 60 * 3 + 30)
    tag(b, L, "RV HEAD STORAGE STAND", (HX + 3.003 * math.cos(ta), (yg + yt) / 2, HZ + 3.003 * math.sin(ta)), (math.cos(ta), 0, math.sin(ta)))


# ---------------------------------------------------------------- studs and their racks

STUD_L = 1.5
STUD_R = 0.075
RACKS = [(C - 9.6, C - 4.4, "STUD RACK A", 0), (C - 9.6, C - 2.2, "STUD RACK B", 27)]


def stud(b, L, x, y, z):
    """A head stud standing on its end: threads at both ends, the shank, the handling eye on top."""
    RP.cyl(b, L, (x, y, z), (x, y + 0.42, z), STUD_R, "crdm", "gunmetal", 12, both=True)
    RP.cyl(b, L, (x, y + 0.42, z), (x, y + STUD_L - 0.42, z), STUD_R * 0.86, "carbon_steel", "carbon_steel", 12, both=False)
    RP.cyl(b, L, (x, y + STUD_L - 0.42, z), (x, y + STUD_L, z), STUD_R, "crdm", "gunmetal", 12, both=True)
    RP.cyl(b, L, (x, y + STUD_L, z), (x, y + STUD_L + 0.05, z), 0.03, "gunmetal", "gunmetal", 8, both=False)
    RP.cyl(b, L, (x - 0.03, y + STUD_L + 0.08, z), (x + 0.03, y + STUD_L + 0.08, z), 0.035, "gunmetal", "gunmetal", 10, both=True)


def stud_racks(b, filled=True):
    L = b.still
    for (x0, z0, name, first) in RACKS:
        w = 9 * RH.STUD_PITCH
        xa, xb = x0 - w / 2 - 0.08, x0 + w / 2 + 0.08
        za, zb = z0 - 0.55, z0 + 0.55
        # base frame, the floor tray with its sockets, the top rail with U slots, end frames
        RP.channel(b, L, (xa, OP + 0.06, za + 0.03), (xb, OP + 0.06, za + 0.03), (0, 1, 0), 0.12, 0.05, (0, 0, 1), mat=BLUE)
        RP.channel(b, L, (xa, OP + 0.06, zb - 0.03), (xb, OP + 0.06, zb - 0.03), (0, 1, 0), 0.12, 0.05, (0, 0, -1), mat=BLUE)
        b.box(xa, OP + 0.12, za, xb, OP + 0.14, zb, "gunmetal")
        for x in (xa + 0.03, xb - 0.03):
            for z in (za + 0.03, zb - 0.03):
                b.box(x - 0.03, OP + 0.14, z - 0.03, x + 0.03, OP + 1.3, z + 0.03, BLUE)
            b.box(x - 0.03, OP + 1.24, za + 0.06, x + 0.03, OP + 1.3, zb - 0.06, BLUE)
        for row in range(3):
            z = z0 - 0.32 + row * 0.32
            b.box(xa + 0.06, OP + 1.18, z - 0.1, xb - 0.06, OP + 1.24, z + 0.1, BLUE)          # top rail (the studs through it)
            # the slots' numbers on a bar in front of the row, read from the rack's front
            b.box(xa + 0.06, OP + 1.18, z - 0.19, xb - 0.06, OP + 1.21, z - 0.1, "gunmetal")
            picture(b, L, "STUD_NOS_%d" % (first // 9 + row + 1), (x0, OP + 1.21, z - 0.145), (0, 1, 0), up=(0, 0, 1))
            for j in range(9):
                x = x0 - w / 2 + (j + 0.5) * RH.STUD_PITCH
                RP.cyl(b, L, (x, OP + 0.14, z), (x, OP + 0.16, z), 0.1, "rubber", "rubber", 12, both=False)
                if filled:
                    stud(b, L, x, OP + 0.16, z)
        b.box(x0 - 0.22, OP + 1.3, za + 0.0, x0 + 0.22, OP + 1.4, za + 0.03, BLUE)
        tag(b, L, name, (x0, OP + 1.35, za), (0, 0, -1))
    # nut and washer rack: three shelves of pegs
    x0, z0 = C - 12.2, C - 3.3
    xa, xb, za, zb = x0 - 0.3, x0 + 0.3, z0 - 1.5, z0 + 1.5
    for x in (xa, xb - 0.05):
        for z in (za, zb - 0.05):
            b.box(x, OP, z, x + 0.05, OP + 1.35, z + 0.05, BLUE)
    for k, y in enumerate((OP + 0.25, OP + 0.7, OP + 1.15)):
        b.box(xa + 0.05, y - 0.03, za, xb - 0.05, y, zb, "gunmetal")
        for j in range(18):
            z = za + 0.12 + j * (zb - za - 0.24) / 17
            for x in (x0,):
                RP.cyl(b, L, (x, y, z), (x, y + 0.004, z), 0.12, "stainless_dark", "stainless_dark", 12, both=False)
                RP.cyl(b, L, (x, y + 0.004, z), (x, y + 0.2, z), 0.1, "gunmetal", "gunmetal", 6, both=False)
    b.box(x0 + 0.3, OP + 1.25, z0 - 0.25, x0 + 0.33, OP + 1.35, z0 + 0.25, BLUE)
    tag(b, L, "NUTS AND WASHERS", (x0 + 0.33, OP + 1.3, z0), (1, 0, 0))


def picture(b, lst, name, centre, normal, up=(0, 1, 0)):
    keep = b.still
    b.still = lst
    try:
        RM.picture(b, name, centre, normal, lst, up=up, lift=0.002)
    finally:
        b.still = keep


def stud_hole_plugs(b):
    """The plugs in the vessel flange's stud holes while the cavity is flooded (studs out): a cap and a T handle."""
    L = b.still
    for k in range(RH.N_STUDS):
        a = 2 * math.pi * k / RH.N_STUDS
        x, _, z = P(RH.R_STUD, a, 0)
        y = RH.FLANGE_TOP
        RP.cyl(b, L, (x, y, z), (x, y + 0.03, z), 0.09, "stainless", "stainless", 10, both=False)
        RP.cyl(b, L, (x, y + 0.03, z), (x, y + 0.11, z), 0.014, "stainless", "stainless", 6, both=False)
        t = (-math.sin(a), 0, math.cos(a))
        RP.cyl(b, L, (x - t[0] * 0.05, y + 0.11, z - t[2] * 0.05), (x + t[0] * 0.05, y + 0.11, z + t[2] * 0.05), 0.011, "stainless", "stainless", 6, both=True)


# ---------------------------------------------------------------- the stud tensioner

TENS_STUDS = (0, 14, 27, 41)                 # the four studs the tensioners are on (a quarter apart)
R_CAR = 2.98                                 # the carousel monorail's radius
CAR_Y = 13.95


def tensioner_set(b):
    """In use round the head on the vessel (the cavity dry): the carousel monorail on brackets off the head package's
    columns, four tensioners on trolleys and air hoists over their studs, hoses to the manifold, the console."""
    L = b.still
    lit = b.lit
    y1 = RH.HEAD_FLANGE[1]
    # the monorail: a ring of I-beam segments
    n = 36
    for i in range(n):
        a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        yy = CAR_Y + (0.002 if i % 2 else 0.0)      # alternate segments a hair apart where their flanges meet
        RP.ibeam(b, L, P(R_CAR, a0, yy), P(R_CAR, a1, yy), (0, 1, 0), 0.16, 0.1, 0.014, 0.01, YELLOW)
    for k in range(6):
        a = math.radians(30 + 60 * k)
        p0 = P(RH.R_SHROUD + 0.13, a, CAR_Y + 0.11)
        p1 = P(R_CAR, a, CAR_Y + 0.11)
        RP.obox(b, L, RP.mul(RP.add(p0, p1), 0.5), (math.cos(a), 0, math.sin(a)), (0, 1, 0), (R_CAR - RH.R_SHROUD - 0.13) / 2, 0.03, 0.04, YELLOW)
        RP.cyl(b, L, P(R_CAR - 0.05, a, CAR_Y + 0.08), P(R_CAR - 0.05, a, CAR_Y + 0.14), 0.04, "gunmetal", "gunmetal", 8, both=False)
    for i, k in enumerate(TENS_STUDS):
        a = 2 * math.pi * k / RH.N_STUDS
        x, _, z = P(RH.R_STUD, a, 0)
        tensioner(b, L, x, y1, z, a, i + 1)
        # trolley on the monorail, the air hoist and its chain down to the counterbalance
        tc = P(R_CAR, a, CAR_Y - 0.08 - 0.06)
        RP.obox(b, L, tc, (-math.sin(a), 0, math.cos(a)), (0, 1, 0), 0.12, 0.05, 0.09, "gunmetal")
        hc = P(R_CAR, a, CAR_Y - 0.33)
        RP.obox(b, L, hc, (-math.sin(a), 0, math.cos(a)), (0, 1, 0), 0.09, 0.12, 0.08, YELLOW)
        RP.cyl(b, L, P(R_CAR, a, CAR_Y - 0.45), P(RH.R_STUD + 0.04, a, y1 + 0.82), 0.01, "gunmetal", "gunmetal", 6, both=False)
        tag(b, L, "TENSIONER %d" % (i + 1), RP.add(hc, RP.mul((math.cos(a), 0, math.sin(a)), 0.081)), (math.cos(a), 0, math.sin(a)))
    # manifold on the cavity floor, hoses to the tensioners, the console beside it
    mx, mz = C - 3.4, C + 4.6
    b.box(mx - 0.25, CF, mz - 0.12, mx + 0.25, CF + 0.18, mz + 0.12, "stainless_dark")
    tag(b, L, "MANIFOLD", (mx, CF + 0.09, mz - 0.121), (0, 0, -1))
    for i, k in enumerate(TENS_STUDS):
        a = 2 * math.pi * k / RH.N_STUDS
        port = (mx - 0.2 + i * 0.13, CF + 0.18, mz)
        dst = P(RH.R_STUD + 0.2, a, y1 + 0.2)
        flo = P(3.6, a, CF + 0.04)
        mid = ((port[0] + flo[0]) / 2, CF + 0.04, (port[2] + flo[2]) / 2)
        for p, q in ((port, (port[0], CF + 0.28, port[2])), ((port[0], CF + 0.28, port[2]), mid), (mid, flo), (flo, (flo[0], y1 + 0.2, flo[2])), ((flo[0], y1 + 0.2, flo[2]), dst)):
            RP.cyl(b, L, p, q, 0.016, "rubber", "rubber", 6, both=False)
    console(b, C - 4.6, C + 4.9)


def tensioner(b, L, x, y, z, a, i):
    """One TENSOR-style tensioner on its stud: the puller sleeve housing, the floating piston's body, the nut-turning
    gear and handle, the stroke indicator, the hydraulic port, the counterbalance on top."""
    RP.cyl(b, L, (x, y, z), (x, y + 0.08, z), 0.17, "stainless_dark", "stainless_dark", 18, both=False)
    RP.cyl(b, L, (x, y + 0.08, z), (x, y + 0.5, z), 0.155, "stainless", "stainless_dark", 18, both=False)
    RP.cyl(b, L, (x, y + 0.5, z), (x, y + 0.56, z), 0.16, "stainless_dark", "stainless_dark", 18, both=False)
    RP.cyl(b, L, (x, y + 0.56, z), (x, y + 0.82, z), 0.07, "gunmetal", "gunmetal", 12, both=False)
    n = (math.cos(a), 0, math.sin(a))
    t = (-math.sin(a), 0, math.cos(a))
    g = (x + n[0] * 0.155, y + 0.2, z + n[2] * 0.155)
    RP.obox(b, L, RP.add(g, RP.mul(n, 0.04)), t, (0, 1, 0), 0.07, 0.07, 0.04, "gunmetal")
    RP.cyl(b, L, RP.add(g, RP.mul(n, 0.08)), RP.add(g, RP.mul(n, 0.2)), 0.012, "stainless", "stainless", 6, both=False)
    RP.cyl(b, L, RP.add(g, RP.mul(n, 0.2), RP.mul(t, -0.07)), RP.add(g, RP.mul(n, 0.2), RP.mul(t, 0.07)), 0.014, "black", "black", 6, both=True)
    sp = (x - n[0] * 0.155, y + 0.38, z - n[2] * 0.155)
    RP.cyl(b, L, sp, RP.add(sp, RP.mul(n, -0.05)), 0.022, "copper", "copper", 8, both=False)
    st = (x + t[0] * 0.155, y + 0.4, z + t[2] * 0.155)
    RP.obox(b, L, RP.add(st, RP.mul(t, 0.012)), n, (0, 1, 0), 0.02, 0.06, 0.012, "black")


def console(b, x, z):
    """The tensioner's pump and control console on the cavity floor: a cabinet (three air-driven pumps and the
    reservoir behind louvres), the sloped panel facing +x (digital pressure readout, four stroke lamps, the pump and
    vent air valves), the air supply hose."""
    L = b.still
    xa, xb = x - 0.35, x + 0.35
    za, zb = z - 0.5, z + 0.5
    yt = CF + 0.95
    b.box(xa, CF + 0.06, za, xb, yt, zb, BLUE)
    b.box(xa + 0.03, CF, za + 0.03, xb - 0.03, CF + 0.06, zb - 0.03, "black")
    b.quad((xb + 0.001, CF + 0.5, zb - 0.1), (xb + 0.001, CF + 0.2, zb - 0.1), (xb + 0.001, CF + 0.2, za + 0.1), (xb + 0.001, CF + 0.5, za + 0.1), ["louvre", 0.05, 0.05, 0.95, 0.95])
    # sloped panel rising toward -x
    yf, yb = yt, yt + 0.25
    b.quad((x - 0.35, yb, za), (xb, yf, za), (xb, yf, zb), (x - 0.35, yb, zb), "paint_grey")
    b.quad((x - 0.35, yb, za), (x - 0.35, yf, za), (xb, yf, za), (xb, yf, za), BLUE)
    b.quad((xb, yf, zb), (x - 0.35, yf, zb), (x - 0.35, yb, zb), (x - 0.35, yb, zb), BLUE)
    b.quad((x - 0.35, yb, zb), (x - 0.35, yf, zb), (x - 0.35, yf, za), (x - 0.35, yb, za), BLUE)
    sl = _unit((-0.7, yb - yf, 0))
    nrm = _unit(_cross(sl, (0, 0, 1)))
    if nrm[1] < 0:
        nrm = tuple(-c for c in nrm)
    u = RM._right(nrm)
    Pp = lambda a_, s_: (xb + sl[0] * s_, yf + sl[1] * s_, z + u[2] * a_)
    slen = math.dist((xb, yf), (x - 0.35, yb))
    tag(b, L, "STUD TENSIONER CONSOLE", Pp(0, slen - 0.06), nrm)
    c = Pp(0.2, slen - 0.19)
    b.obox(tuple(c[i] + nrm[i] * 0.006 for i in range(3)), u, sl, 0.12, 0.035, 0.006, ["black", None, "black", "black", "black", "black"], L)
    face = tuple(c[i] + nrm[i] * 0.012 for i in range(3))
    b.live["readouts"].append({"name": "psi", "centre": [round(v, 4) for v in face], "normal": [round(v, 4) for v in nrm], "char_h": 0.05, "chars": 5})
    tag(b, L, "PRESS  PSI", Pp(0.2, slen - 0.25), nrm)
    for k in range(4):
        a_ = -0.36 + 0.24 * k
        cc = Pp(a_, 0.22)
        top = tuple(cc[i] + nrm[i] * 0.016 for i in range(3))
        b.cyl(cc, top, 0.026, "stainless", "gunmetal", 12, lst=L)
        b.live["lamps"]["stroke%d" % (k + 1)] = {"a": [round(v, 4) for v in top], "b": [round(v, 4) for v in tuple(cc[i] + nrm[i] * 0.024 for i in range(3))], "r": 0.02, "colour": "amber"}
        tag(b, L, "STROKE %d" % (k + 1), Pp(a_, 0.16), nrm)
    for k, lab in enumerate(("PUMP", "VENT")):
        a_ = -0.3 + 0.18 * k
        cc = Pp(a_, slen - 0.2)
        b.cyl(cc, tuple(cc[i] + nrm[i] * 0.015 for i in range(3)), 0.035, "gunmetal", "gunmetal", 12, lst=L)
        tip = tuple(cc[i] + nrm[i] * 0.05 + u[i] * 0.03 for i in range(3))
        b.cyl(tuple(cc[i] + nrm[i] * 0.015 for i in range(3)), tip, 0.01, "stainless", "stainless", 6, lst=L)
        b.cyl(tip, tuple(tip[i] + nrm[i] * 0.02 for i in range(3)), 0.02, sw("red") if k else sw("green"), "black", 8, lst=L)
        tag(b, L, lab, Pp(a_, slen - 0.27), nrm)
    # the air supply hose off along the floor to the cavity wall
    RP.cyl(b, L, (xa, CF + 0.3, z), (xa - 0.1, CF + 0.04, z), 0.02, "black", "black", 6, both=False)
    RP.cyl(b, L, (xa - 0.1, CF + 0.04, z), (C - H + 0.1, CF + 0.04, z), 0.02, "black", "black", 6, both=False)


def tensioner_storage(b):
    """The tensioners' storage rack on the deck by the stud racks: four cradles on a frame, the units in them."""
    L = b.still
    x0, z0 = C - 9.6, C - 0.4
    b.box(x0 - 1.2, OP, z0 - 0.4, x0 + 1.2, OP + 0.06, z0 + 0.4, BLUE)
    for k in range(4):
        x = x0 - 0.9 + k * 0.6
        b.box(x - 0.22, OP + 0.06, z0 - 0.22, x + 0.22, OP + 0.3, z0 + 0.22, BLUE)
        tensioner(b, L, x, OP + 0.3, z0, math.radians(-90), k + 1)
    b.box(x0 - 0.3, OP + 0.06, z0 - 0.42, x0 + 0.3, OP + 0.16, z0 - 0.4, BLUE)
    tag(b, L, "TENSIONER STORAGE", (x0, OP + 0.11, z0 - 0.42), (0, 0, -1))


def build(state="refuelling"):
    """state: refuelling (head on its stand, internals on theirs, studs in the racks, plugs in, tensioners stored) or
    tensioning (head on the vessel, tensioners on the studs, cavity dry, racks empty)."""
    b = Floor()
    head_stand(b)
    internals_stand(b)
    stud_racks(b, filled=(state == "refuelling"))
    if state == "refuelling":
        stud_hole_plugs(b)
        tensioner_storage(b)
    else:
        tensioner_set(b)
    b.core = len(b.still)
    internals(b)
    lifting_rig(b)
    return b


def ops_check():
    bad = 0
    for st in ("refuelling", "tensioning"):
        b = build(st)
        bad += len(CB.overlaps(b.ops()))
    b = build()
    for g in ("internals", "rig"):
        bad += len(CB.overlaps({"still": b.live[g]["still"], "lit": [], "blink": []}))
    return bad


if __name__ == "__main__":
    print("overlaps", ops_check())
