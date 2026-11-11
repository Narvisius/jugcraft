"""The pressurizer (reactor building rebuild, machine 9).

Built like a Westinghouse four-loop plant's pressurizer (Westinghouse Technology Systems Manual 3.2, tables 3.2-2, 4
and 5: a vertical cylinder with hemispherical heads, 84 in inside diameter, 1800 cu ft, 2485 psig / 680 F; 78
immersion heaters through the bottom head (18 proportional, 60 backup, 1794 kW); the surge line from loop 2's hot leg
into the bottom; spray from loops 2 and 3 cold legs through two spray valves into the top; three code safety valves
with loop seals, two air-operated PORVs with motor-operated block valves, all discharging to the relief tank; a
manway in the top head; a support skirt with openings for the heaters' terminals), one block a metre:

- The vessel in reflective metal insulation (its courses, standing seams and latches built in 3D), the bare bottom
  head with the 78 heaters' sleeves, terminals and cables to six junction boxes inside the skirt; the surge nozzle; the
  level taps and temperature wells on the shell; the seismic lugs and their bumper brackets in the wall.
- The top head: the spray nozzle at the crown, the three safety valve nozzles with their loop seals and valves
  (spring housings, caps, lifting levers), the relief nozzle with its two block valves (motor operators with
  handwheels) and two PORVs (diaphragm actuators), the relief header round the top and out through the wall to the
  relief tank, the manway with its studs and davit; the spray line down to its two spray valves and on down the wall.
- Its compartment: a round concrete wall from the loop floor to above the operating floor, the heater room's slab on
  which the skirt stands, ring platforms at mid height, at the operating floor and round the top, ladders, a locked
  door at the loop floor and at the operating floor (HIGH RADIATION AREA), the PZR stencil.

Frame: the compartment stands west of steam generator 2, south-west of the cavity (PZ_XZ); local a toward the reactor.
Live (LIVE): the pressure gauge at the top platform (psig; 2235 normal).
"""
import math

import control_boards as CB
from control_boards import sw
import reactor_head as RH
import rb_parts as RP
import refuelling_machine as RM
import steam_generator as S
from reactor_head import C, legend_size, _cross, _unit
from steam_generator import tag, plate, picture, pipe, arc_rail, ring_wall, seam, latch

OP = RM.OP
FLOOR = S.FLOOR
PZ_XZ = (C - 13.6, C + 10.9)
R_V = 1.22                          # the shell's outside (84 in inside, about 4 in wall... with cladding)
RV = R_V + S.INS
BOT_TAN, TOP_TAN = 12.4, 25.6
SLAB = (9.0, 9.6)                   # the heater room's slab
SKIRT = (9.6, 12.25)
W_IN, W_OUT = 2.7, 3.3
W_TOP = 28.6
P_MID, P_DECK, P_TOP = 15.6, OP, 26.0
HEADER_Y = 28.3
HEADER_R = 1.96
SUPPORT = S.SUPPORT
WALL = S.WALL


class PZR(S.SG):
    def __init__(self):
        CB.Board.__init__(self, 9, 900)
        self.loop = 0
        self.h = 1
        self.ox, self.oz = PZ_XZ
        d = _unit((C - self.ox, 0.0, C - self.oz))
        self.ein = d
        self.es = (-d[2], 0.0, d[0])
        self.stand = False
        self.live = {"dial": None}


def head_pt(b, r, f, top=True, ins=False):
    """A point on a head's outside at horizontal radius r, azimuth f."""
    R = RV if ins else R_V
    dy = math.sqrt(max(0.0, R * R - r * r))
    return b.p(r, f, TOP_TAN + dy if top else BOT_TAN - dy)


# ---------------------------------------------------------------- the vessel

def vessel(b):
    L = b.still
    b.rtube(RV, BOT_TAN, TOP_TAN, "stainless")
    # the top head in its insulation, up to the spray nozzle's boss
    steps = 8
    t_end = math.acos(0.25 / RV)
    for k in range(steps):
        t0, t1 = t_end * k / steps, t_end * (k + 1) / steps
        b.rband(RV * math.cos(t0), TOP_TAN + RV * math.sin(t0), RV * math.cos(t1), TOP_TAN + RV * math.sin(t1), "stainless", n=36)
    # the bottom head bare (heaters through it): a hemisphere in steel, down to the surge nozzle
    t_end = math.acos(0.22 / R_V)
    for k in range(steps):
        t0, t1 = t_end * k / steps, t_end * (k + 1) / steps
        b.rband(R_V * math.cos(t1), BOT_TAN - R_V * math.sin(t1), R_V * math.cos(t0), BOT_TAN - R_V * math.sin(t0), "gunmetal", n=36)
    b.rring(R_V, RV, BOT_TAN, "stainless", down=True, n=40)
    # insulation courses, standing seams, latches; meridian seams on the top head
    ys = [BOT_TAN + 0.02 + 1.2 * k for k in range(11)] + [TOP_TAN - 0.02]
    for y in ys:
        seam(b, RV, y)
    for k in range(10):
        f = 2 * math.pi * (k + 0.5) / 10
        for ya, yb in zip(ys, ys[1:]):
            if not _clear(b, f, ya, yb):
                continue
            RP.cyl(b, L, b.p(RV + 0.004, f, ya + 0.02), b.p(RV + 0.004, f, yb - 0.02), 0.011, "stainless", "stainless", 6, both=False)
            latch(b, RV, f, yb - 0.02)
        prev = None
        for j in range(8):
            t = math.acos(0.4 / RV) * j / 7
            q = b.p((RV + 0.004) * math.cos(t), f, TOP_TAN + (RV + 0.004) * math.sin(t))
            if prev and _clear_head(b, f):
                RP.cyl(b, L, prev, q, 0.011, "stainless", "stainless", 6, both=False)
            prev = q
    # the surge nozzle under the bottom head, its safe end
    a = b.L(0, 0, 0)
    RP.cyl(b, L, (a[0], BOT_TAN - R_V - 0.3, a[2]), (a[0], BOT_TAN - R_V + 0.15, a[2]), 0.24, "gunmetal", "gunmetal", 18, both=False)
    RP.cyl(b, L, (a[0], BOT_TAN - R_V - 0.45, a[2]), (a[0], BOT_TAN - R_V - 0.3, a[2]), 0.2, "stainless", "stainless", 16, both=False)
    # the name tag and the data plate facing the operating floor's door
    f = b.f(0)
    n = b.dirh(f)
    tag(b, "PRESSURIZER", b.p(RV, f, P_DECK + 1.6), n, d=0.02)
    for su in (-1, 1):
        q = tuple(b.p(RV, f, P_DECK + 1.1)[i] + b.tang(f)[i] * su * 0.14 for i in range(3))
        RP.obox(b, L, tuple(q[i] + n[i] * 0.02 for i in range(3)), n, (0, 1, 0), 0.025, 0.04, 0.015, "stainless_dark")
    plate(b, "PZR_PLATE", tuple(b.p(RV, f, P_DECK + 1.1)[i] + n[i] * 0.045 for i in range(3)), n)


KEEP = [(0, 14, P_DECK + 0.7, P_DECK + 1.8), (300, 8, 12.8, 13.3), (300, 8, 24.5, 25.1), (240, 8, 13.3, 13.9),
        (240, 8, 24.1, 24.7), (45, 9, 23.3, 23.95), (135, 9, 23.3, 23.95), (225, 9, 23.3, 23.95), (315, 9, 23.3, 23.95)]


def _clear(b, f, y0, y1):
    for deg, half, ka, kb in KEEP:
        if abs(S._norm(f - b.f(deg))) < math.radians(half) and y1 > ka and y0 < kb:
            return False
    return True


def _clear_head(b, f):
    for deg in (30, 90, 150, 210, 270, 330):
        if abs(S._norm(f - b.f(deg))) < math.radians(14):
            return False
    return True


def heaters(b):
    """78 heaters on a square pitch through the bottom head: sleeve, terminal, a cable down and out to its junction
    box on the skirt (six boxes, 13 cables each)."""
    L = b.still
    pts = []
    pitch = 0.17
    for i in range(-6, 7):
        for j in range(-6, 7):
            x, z = i * pitch, j * pitch
            r = math.hypot(x, z)
            if 0.3 < r < 0.98:
                pts.append((x, z))
    pts.sort(key=lambda p: (math.hypot(*p), math.atan2(p[1], p[0])))
    pts = pts[:78]
    jbs = [2 * math.pi * (k + 0.5) / 4 for k in range(4)]
    for x, z in pts:
        r = math.hypot(x, z)
        y0 = BOT_TAN - math.sqrt(R_V * R_V - r * r)
        top = b.L(x, y0 + 0.05, z)
        RP.cyl(b, L, top, b.L(x, y0 - 0.12, z), 0.022, "stainless", "stainless", 8, both=False)
        RP.cyl(b, L, b.L(x, y0 - 0.12, z), b.L(x, y0 - 0.26, z), 0.032, "stainless_dark", "stainless_dark", 8, both=True)
        f = math.atan2(z, x)
        jb = min(jbs, key=lambda q: abs(S._norm(f - q)))
        low = b.L(x, y0 - 0.26, z)
        mid = b.L(x, 10.55 + (r - 0.3) * 0.25, z)
        end = b.p(0.98, jb, 10.45)
        RP.cyl(b, L, low, mid, 0.009, "black", "black", 5, both=False)
        RP.cyl(b, L, mid, end, 0.009, "black", "black", 5, both=False)
    for jb in jbs:
        n = b.dirh(jb)
        RP.obox(b, L, b.p(1.05, jb, 10.45), b.tang(jb), (0, 1, 0), 0.16, 0.2, 0.07, "gunmetal")
        RP.obox(b, L, b.p(0.975, jb, 10.45), b.tang(jb), (0, 1, 0), 0.15, 0.19, 0.005, "gunmetal")
        # its conduit out through the skirt's opening to the wall
        end = 1.6 if b.stand else W_IN + 0.02
        RP.cyl(b, L, b.p(1.12, jb, 10.3), b.p(end, jb, 10.3), 0.04, "stainless_dark", "stainless_dark", 8, both=False)
        if b.stand:
            RP.cyl(b, L, b.p(end, jb, 10.3), b.p(end + 0.04, jb, 10.3), 0.06, "gunmetal", "gunmetal", 8, both=True)
        else:
            S.sleeve(b, jb, 10.3, 0.04, w_in=W_IN)
    wall_tag(b, "HEATER JUNCTION BOX", b.f(45), 11.0)


def skirt(b):
    """The support skirt with four openings for the heaters, its base ring and anchor bolts on the slab."""
    gaps = []
    for k in range(4):
        f = 2 * math.pi * k / 4
        gaps.append((f - 0.42, f + 0.42, SKIRT[0] + 0.3, SKIRT[1] - 0.45))
    ring_wall(b, 1.17, 1.24, SKIRT[0], SKIRT[1], gaps, mat=SUPPORT)
    b.rband(1.0, SKIRT[0], 1.0, SKIRT[0] + 0.08, SUPPORT, inward=True, n=40)
    b.rband(1.45, SKIRT[0], 1.45, SKIRT[0] + 0.08, SUPPORT, n=40)
    b.rring(1.0, 1.17, SKIRT[0] + 0.08, SUPPORT, n=40)
    b.rring(1.24, 1.45, SKIRT[0] + 0.08, SUPPORT, n=40)
    for k in range(24):
        f = 2 * math.pi * (k + 0.5) / 24
        c = b.p(1.36, f, SKIRT[0] + 0.08)
        RP.cyl(b, b.still, c, (c[0], c[1] + 0.14, c[2]), 0.022, "stainless_dark", "stainless_dark", 6, both=False)
        RP.cyl(b, b.still, c, (c[0], c[1] + 0.06, c[2]), 0.045, "gunmetal", "gunmetal", 6, both=False)


def shell_fittings(b):
    L = b.still
    for deg, y, text in ((300, 13.05, "PZR LEVEL TAP"), (300, 24.8, "PZR LEVEL TAP"), (240, 13.6, "PZR TEMP"), (240, 24.4, "PZR TEMP")):
        f = b.f(deg)
        RP.cyl(b, L, b.p(R_V - 0.02, f, y), b.p(RV + 0.14, f, y), 0.035, "gunmetal", "gunmetal", 8, both=False)
        end = RV + 0.6 if b.stand else W_IN - 0.02
        if text == "PZR TEMP":
            RP.cyl(b, L, b.p(RV + 0.14, f, y), b.p(RV + 0.22, f, y), 0.05, "stainless_dark", "stainless_dark", 10, both=True)
            RP.cyl(b, L, b.p(RV + 0.22, f, y), b.p(end, f, y), 0.01, "stainless_dark", "stainless_dark", 6, both=False)
        else:
            RP.cyl(b, L, b.p(RV + 0.14, f, y), b.p(end, f, y), 0.012, "stainless", "stainless", 6, both=False)
            S.valve(b, b.p(RV + 0.3, f, y), b.dirh(f), r=0.016, wheel=0.045)
        if b.stand:
            RP.cyl(b, L, b.p(end, f, y), b.p(end + 0.03, f, y), 0.022, "stainless_dark", "stainless_dark", 6, both=False)
        else:
            S.sleeve(b, f, y, 0.035, w_in=W_IN)
        tag(b, text, b.p(RV, f, y + 0.24), b.dirh(f), d=0.014)
    # seismic lugs and their bumper brackets in the wall
    for deg in (45, 135, 225, 315):
        f = b.f(deg)
        n = b.dirh(f)
        b.rbox(f, R_V - 0.05, RV + 0.3, 23.35, 23.9, 0.1, "gunmetal")
        if not b.stand:
            RP.obox(b, L, b.p(W_IN - 0.2, f, 23.62), n, (0, 1, 0), 0.2, 0.3, 0.25, "gunmetal")
            RP.obox(b, L, b.p(W_IN - 0.405, f, 23.62), n, (0, 1, 0), 0.005, 0.2, 0.15, "stainless_dark")


# ---------------------------------------------------------------- the top head: valves and lines

def safety_valve(b, c, out_dir):
    """A spring-loaded code safety valve standing on its inlet flange at c: body with its side outlet toward
    out_dir, bonnet, spring housing (open yoke with the spring), cap and lifting lever."""
    L = b.still
    x, y, z = c
    RP.cyl(b, L, (x, y, z), (x, y + 0.07, z), 0.21, "gunmetal", "gunmetal", 16, both=True)
    RP.bolt_circle(b, L, (x, y + 0.07, z), (0, 1, 0), 0.17, 12, r=0.016, h=0.025)
    RP.cyl(b, L, (x, y + 0.07, z), (x, y + 0.45, z), 0.17, "gunmetal", "gunmetal", 16, both=False)
    o = _unit(out_dir)
    oc = (x, y + 0.27, z)
    RP.cyl(b, L, oc, tuple(oc[i] + o[i] * 0.35 for i in range(3)), 0.13, "gunmetal", "gunmetal", 14, both=False)
    RP.cyl(b, L, tuple(oc[i] + o[i] * 0.3 for i in range(3)), tuple(oc[i] + o[i] * 0.37 for i in range(3)), 0.19, "gunmetal", "gunmetal", 14, both=True)
    RP.cyl(b, L, (x, y + 0.45, z), (x, y + 0.52, z), 0.15, "gunmetal", "gunmetal", 14, both=True)
    side = _unit(_cross(o, (0, 1, 0)))
    for e in (-1, 1):
        q = tuple((x, y, z)[i] + side[i] * e * 0.1 for i in range(3))
        RP.obox(b, L, (q[0], y + 0.8, q[2]), side, (0, 1, 0), 0.02, 0.28, 0.04, "gunmetal")
    for k in range(9):
        yy = y + 0.56 + k * 0.055
        # the spring's coils
        RP.cyl(b, L, (x, yy, z), (x, yy + 0.025, z), 0.075, sw("dgrey"), sw("dgrey"), 12, both=True)
    RP.cyl(b, L, (x, y + 1.08, z), (x, y + 1.18, z), 0.14, "gunmetal", "gunmetal", 14, both=True)
    RP.cyl(b, L, (x, y + 1.18, z), (x, y + 1.32, z), 0.06, "gunmetal", "gunmetal", 10, both=True)
    lv0 = (x, y + 1.27, z)
    RP.cyl(b, L, lv0, tuple(lv0[i] - o[i] * 0.3 + (0, -0.08, 0)[i] for i in range(3)), 0.015, "stainless_dark", "stainless_dark", 6, both=True)
    return tuple(oc[i] + o[i] * 0.37 for i in range(3))


def gate_mov(b, c, d, r=0.12):
    """A motor-operated gate valve on a line along d at c: body, bonnet, yoke, the motor operator (gearbox, motor,
    handwheel) on top."""
    L = b.still
    d = _unit(d)
    RP.cyl(b, L, tuple(c[i] - d[i] * 0.22 for i in range(3)), tuple(c[i] + d[i] * 0.22 for i in range(3)), r * 1.5, "gunmetal", "gunmetal", 14, both=True)
    for e in (-1, 1):
        fl = tuple(c[i] + d[i] * e * 0.24 for i in range(3))
        RP.cyl(b, L, tuple(fl[i] - d[i] * 0.025 for i in range(3)), tuple(fl[i] + d[i] * 0.025 for i in range(3)), r * 1.8, "gunmetal", "gunmetal", 14, both=True)
    x, y, z = c
    RP.cyl(b, L, (x, y, z), (x, y + 0.4, z), r * 1.1, "gunmetal", "gunmetal", 12, both=False)
    RP.cyl(b, L, (x, y + 0.4, z), (x, y + 0.45, z), r * 1.6, "gunmetal", "gunmetal", 12, both=True)
    side = _unit(_cross(d, (0, 1, 0)))
    for e in (-1, 1):
        q = tuple(c[i] + side[i] * e * 0.09 for i in range(3))
        RP.obox(b, L, (q[0], y + 0.62, q[2]), side, (0, 1, 0), 0.015, 0.17, 0.03, "gunmetal")
    RP.cyl(b, L, (x, y + 0.45, z), (x, y + 0.8, z), 0.03, "stainless", "stainless", 8, both=False)
    RP.obox(b, L, (x, y + 0.92, z), d, (0, 1, 0), 0.18, 0.13, 0.16, sw("blue"))
    RP.motor(b, L, tuple((x, y + 0.92, z)[i] + side[i] * 0.16 for i in range(3)), side, 0.32, 0.11, colour="paint_blue", fins=10)
    hw = tuple((x, y + 0.92, z)[i] - side[i] * 0.16 for i in range(3))
    RP.cyl(b, L, hw, tuple(hw[i] - side[i] * 0.03 for i in range(3)), 0.16, sw("red"), sw("red"), 16, both=True)


def globe_aov(b, c, d, r=0.1):
    """An air-operated globe valve: body on the line along d, bonnet and yoke up, the diaphragm actuator on top with
    its air line and a stem position indicator."""
    L = b.still
    d = _unit(d)
    RP.cyl(b, L, tuple(c[i] - d[i] * 0.2 for i in range(3)), tuple(c[i] + d[i] * 0.2 for i in range(3)), r * 1.5, "gunmetal", "gunmetal", 14, both=True)
    RP.cyl(b, L, c, tuple(c[i] + (0, 1, 0)[i] * r * 1.7 for i in range(3)), r * 1.55, "gunmetal", "gunmetal", 14, both=True)
    for e in (-1, 1):
        fl = tuple(c[i] + d[i] * e * 0.22 for i in range(3))
        RP.cyl(b, L, tuple(fl[i] - d[i] * 0.025 for i in range(3)), tuple(fl[i] + d[i] * 0.025 for i in range(3)), r * 1.8, "gunmetal", "gunmetal", 14, both=True)
    x, y, z = c
    RP.cyl(b, L, (x, y, z), (x, y + 0.32, z), r * 0.9, "gunmetal", "gunmetal", 12, both=False)
    side = _unit(_cross(d, (0, 1, 0)))
    for e in (-1, 1):
        q = tuple(c[i] + side[i] * e * 0.08 for i in range(3))
        RP.obox(b, L, (q[0], y + 0.5, q[2]), side, (0, 1, 0), 0.012, 0.18, 0.025, "gunmetal")
    RP.cyl(b, L, (x, y + 0.32, z), (x, y + 0.68, z), 0.022, "stainless", "stainless", 8, both=False)
    RP.cyl(b, L, (x, y + 0.68, z), (x, y + 0.76, z), 0.24, sw("yellow"), sw("yellow"), 20, both=True)
    RP.cyl(b, L, (x, y + 0.76, z), (x, y + 0.84, z), 0.24, sw("yellow"), sw("yellow"), 20, both=True)
    RP.bolt_circle(b, L, (x, y + 0.84, z), (0, 1, 0), 0.21, 12, r=0.012, h=0.015)
    RP.cyl(b, L, (x, y + 0.84, z), (x, y + 0.9, z), 0.05, "stainless_dark", "stainless_dark", 8, both=True)
    pi = tuple((x, y + 0.5, z)[i] + side[i] * 0.11 for i in range(3))
    RP.obox(b, L, pi, side, (0, 1, 0), 0.004, 0.08, 0.02, "stainless")


def wall_tag(b, text, f, y):
    """An engraved tag on the compartment wall's inside, facing in (a valve's name over it); none on a stand."""
    if b.stand:
        return
    w, h = legend_size(text)
    n = b.dirh(f + math.pi)
    c = b.p(W_IN - 0.006, f, y)
    RP.obox(b, b.still, c, RM._right(n), (0, 1, 0), w / 2 + 0.015, h / 2 + 0.012, 0.006, "stainless")
    tag(b, text, b.p(W_IN - 0.012, f, y), n)


# where the two spray lines leave the compartment (degrees on the hand's side, y): east, toward loops 1 and 2
SPRAY_EXIT = {1: (40, 11.4), -1: (52, 11.75)}


def spray_downcomer(b, j, bj, fd, e):
    """A spray line from its valve's branch down the wall, then round the wall's inside to its sleeve (the two at
    different heights and radii so they pass each other)."""
    fe, ye = SPRAY_EXIT[e]
    r_arc = 2.3 if e > 0 else 2.1
    f1 = b.f(fe)
    while f1 < fd:
        f1 += 2 * math.pi
    while f1 - fd > 2 * math.pi:
        f1 -= 2 * math.pi
    pts = [j, bj, (bj[0], 26.4, bj[2]), b.p(2.3, fd, 26.2), b.p(2.3, fd, ye), b.p(r_arc, fd, ye)]
    n = max(2, int((f1 - fd) / math.radians(12)))
    for k in range(1, n + 1):
        pts.append(b.p(r_arc, fd + (f1 - fd) * k / n, ye))
    pts.append(b.p(W_IN + 0.02, f1, ye))
    return pts


def top_valves(b):
    L = b.still
    # the three safety valves: nozzle, loop seal, the valve; its discharge up into the relief header
    for deg, name in ((30, "PSV-8010A"), (150, "PSV-8010B"), (270, "PSV-8010C")):
        f = b.f(deg)
        t = b.tang(f)
        hp = head_pt(b, 0.75, f)
        RP.cyl(b, L, (hp[0], hp[1] - 0.1, hp[2]), (hp[0], 26.85, hp[2]), 0.16, "gunmetal", "gunmetal", 14, both=False)
        pts = [(hp[0], 26.85, hp[2]), (hp[0], 27.05, hp[2]), b.p(1.4, f, 27.05), b.p(1.4, f, 26.25), b.p(1.85, f, 26.25),
               b.p(1.85, f, 26.62)]
        pipe(b, pts, 0.09, "stainless", 0.18, sides=12)
        outlet = safety_valve(b, b.p(1.85, f, 26.62), t)
        riser = tuple(outlet[i] + t[i] * 0.2 for i in range(3))
        pipe(b, [outlet, riser, (riser[0], HEADER_Y, riser[2])], 0.09, "stainless", 0.18, sides=12)
        wall_tag(b, name, f + 0.12, 27.45)
    # the relief nozzle, a tee, each branch: block valve, PORV, up into the header
    f = b.f(90)
    t = b.tang(f)
    n = b.dirh(f)
    hp = head_pt(b, 0.75, f)
    RP.cyl(b, L, (hp[0], hp[1] - 0.1, hp[2]), (hp[0], 26.75, hp[2]), 0.12, "gunmetal", "gunmetal", 12, both=False)
    tee = b.p(1.25, f, 26.95)
    pipe(b, [(hp[0], 26.75, hp[2]), (hp[0], 26.95, hp[2]), tee], 0.07, "stainless", 0.15, sides=12)
    RP.cyl(b, L, tuple(tee[i] - n[i] * 0.1 for i in range(3)), tuple(tee[i] + n[i] * 0.1 for i in range(3)), 0.085, "stainless", "stainless", 12, both=True)
    for e, blk, porv in ((-1, "BLOCK MOV-8000A", "PORV PCV-455A"), (1, "BLOCK MOV-8000B", "PORV PCV-456")):
        a2 = tuple(tee[i] + t[i] * e * 0.75 for i in range(3))
        a3 = tuple(a2[i] + n[i] * 0.57 for i in range(3))
        pipe(b, [tee, a2, a3, (a3[0], HEADER_Y, a3[2])], 0.07, "stainless", 0.12, sides=12)
        gate_mov(b, tuple(tee[i] + t[i] * e * 0.42 for i in range(3)), t, r=0.08)
        globe_aov(b, tuple(a2[i] + n[i] * 0.28 for i in range(3)), n, r=0.07)
        fa = math.atan2(*[(a2[0] - b.ox) * b.es[0] + (a2[2] - b.oz) * b.es[2], (a2[0] - b.ox) * b.ein[0] + (a2[2] - b.oz) * b.ein[2]])
        wall_tag(b, blk, fa - e * 0.1, 27.62)
        wall_tag(b, porv, fa + e * 0.06, 27.45)
    # the relief header round the top (its flanged joints), out through the wall and down outside to the deck
    m = 36
    for k in range(m):
        fa, fb = 2 * math.pi * k / m, 2 * math.pi * (k + 1) / m
        RP.cyl(b, L, b.p(HEADER_R, fa, HEADER_Y), b.p(HEADER_R, fb, HEADER_Y), 0.15, "stainless", "stainless", 14, both=False)
    for k in range(6):
        fa = 2 * math.pi * k / 6 + 0.26
        RP.cyl(b, L, tuple(b.p(HEADER_R, fa, HEADER_Y)[i] - b.tang(fa)[i] * 0.03 for i in range(3)),
               tuple(b.p(HEADER_R, fa, HEADER_Y)[i] + b.tang(fa)[i] * 0.03 for i in range(3)), 0.21, "stainless_dark", "stainless_dark", 14, both=True)
    fo = b.f(200)
    if not b.stand:
        pipe(b, [b.p(HEADER_R, fo, HEADER_Y), b.p(3.9, fo, HEADER_Y), b.p(3.9, fo, OP - 0.8)], 0.15, "stainless", 0.5, sides=14)
        x, _, z = b.p(3.9, fo, 0)
        S.deck_collar(b, x, z, 0.15)
        tag(b, "RELIEF TO PRT", b.p(W_OUT, fo + 0.18, OP + 1.5), b.dirh(fo + 0.18), d=0.012)
    # the manway in the top head
    f = b.f(210)
    r = 0.75
    hp = head_pt(b, r, f, ins=False)
    nn = _unit(tuple((hp[i] - b.L(0, TOP_TAN, 0)[i]) for i in range(3)))
    S.manway(b, hp, nn, 0.34, 16, "PZR MANWAY", davit=True)
    # the spray nozzle at the crown, the spray line out and down to its two spray valves, on down the wall
    a = b.L(0, 0, 0)
    RP.cyl(b, L, (a[0], TOP_TAN + R_V - 0.05, a[2]), (a[0], TOP_TAN + RV + 0.22, a[2]), 0.13, "gunmetal", "gunmetal", 14, both=False)
    fs = b.f(330)
    top = (a[0], 27.45, a[2])
    j = b.p(2.35, fs, 26.75)
    pipe(b, [(a[0], TOP_TAN + RV + 0.22, a[2]), top, b.p(2.35, fs, 27.45), j], 0.06, "stainless", 0.2, sides=10)
    t = b.tang(fs)
    for e, name in ((-1, "SPRAY PCV-455B"), (1, "SPRAY PCV-455C")):
        bj = tuple(j[i] + t[i] * e * 0.6 for i in range(3))
        fd = math.atan2((bj[0] - b.ox) * b.es[0] + (bj[2] - b.oz) * b.es[2], (bj[0] - b.ox) * b.ein[0] + (bj[2] - b.oz) * b.ein[2])
        if b.stand:
            pipe(b, [j, bj, (bj[0], 26.1, bj[2])], 0.06, "stainless", 0.15, sides=10)
            RP.cyl(b, L, (bj[0], 26.1, bj[2]), (bj[0], 26.04, bj[2]), 0.09, "gunmetal", "gunmetal", 10, both=True)
        else:
            pipe(b, spray_downcomer(b, j, bj, fd, e), 0.06, "stainless", 0.15, sides=10)
            fe, ye = SPRAY_EXIT[e]
            S.sleeve(b, b.f(fe), ye, 0.06, w_in=W_IN)
        globe_aov(b, tuple(j[i] + t[i] * e * 0.33 for i in range(3)), t, r=0.06)
        wall_tag(b, name, fs + e * 0.14, 27.5)
    wall_tag(b, "SPRAY LINE", fs, 25.4)


# ---------------------------------------------------------------- the compartment

def surge_route(b):
    """The surge line's centre line (world points): down off the nozzle, out at a slight fall to over its tap on loop
    2's hot leg, down into it."""
    sg = S.SG(2)
    hot = S.hot_leg_route(sg)
    v, n = hot[2], hot[1]
    u = _unit(tuple(n[i] - v[i] for i in range(3)))
    tap = tuple(v[i] + u[i] * 3.65 for i in range(3))
    a = b.L(0, 0, 0)
    y0 = BOT_TAN - R_V - 0.45
    return [(a[0], y0, a[2]), (a[0], 10.42, a[2]), (tap[0], 10.32, tap[2]), (tap[0], RH.NOZZLE_Y + S.HOT_R, tap[2])]


def surge_angle(b):
    pts = surge_route(b)
    q = (pts[2][0] - b.ox, pts[2][2] - b.oz)
    return math.atan2(q[0] * b.es[0] + q[1] * b.es[2], q[0] * b.ein[0] + q[1] * b.ein[2])


def enclosure(b):
    L = b.still
    a = b.L(0, 0, 0)
    # the plinth (solid concrete up to the heater room's floor) and the wall above it
    RP.cyl(b, L, (a[0], FLOOR, a[2]), (a[0], SLAB[1], a[2]), W_OUT, WALL, WALL, 48, both=False)
    fs = surge_angle(b)
    gaps = [(b.f(0) - S.DOOR_HALF * 1.2, b.f(0) + S.DOOR_HALF * 1.2, OP, OP + 2.2),
            (fs - 0.2, fs + 0.2, 9.85, 10.95),
            (b.f(200) - 0.09, b.f(200) + 0.09, HEADER_Y - 0.28, HEADER_Y + 0.28)]
    ring_wall(b, W_IN, W_OUT, SLAB[1], W_TOP, gaps)
    for f, y in ((b.f(0), OP + 3.0), (b.f(180), 25.0)):
        b.curved_stencil("PZR", W_OUT, f, y)


def wall_ring(b, r0, y, ranges, r1=W_IN):
    """A grating ring platform out of the wall, its brackets and knee braces, a toe plate at its inner edge."""
    L = b.still
    t = 0.04
    for f0, f1 in ranges:
        b.rring(r0, r1, y, "grating", f0, f1)
        b.rring(r0, r1, y - t, "grating", f0, f1, down=True)
        b.rband(r0, y - t, r0, y, "gunmetal", f0, f1, inward=True)
        if f1 - f0 < 2 * math.pi - 1e-6:
            b.jamb(f0, r0, r1, y - t, y, -1, "gunmetal")
            b.jamb(f1, r0, r1, y - t, y, +1, "gunmetal")
        b.rband(r0 + 0.006, y, r0 + 0.006, y + 0.1, sw("yellow"), f0, f1, inward=True)
        b.rband(r0 + 0.012, y, r0 + 0.012, y + 0.1, sw("yellow"), f0, f1)
        b.rring(r0 + 0.006, r0 + 0.012, y + 0.1, sw("yellow"), f0, f1)
        n = max(1, int(round((f1 - f0) / math.radians(36))))
        for k in range(n):
            f = f0 + (f1 - f0) * (k + 0.5) / n
            RP.ibeam(b, L, b.p(W_IN + 0.02, f, y - t - 0.07), b.p(r0 + 0.04, f, y - t - 0.07), (0, 1, 0), 0.14, 0.08, tf=0.012, tw=0.01, mat="gunmetal")
            RP.cyl(b, L, b.p(W_IN, f, y - t - 0.65), b.p((W_IN + r0) / 2, f, y - t - 0.14), 0.03, "gunmetal", "gunmetal", 8, both=False)
            RP.obox(b, L, b.p(W_IN - 0.01, f, y - t - 0.35), b.dirh(f), (0, 1, 0), 0.01, 0.4, 0.1, "gunmetal")


LADDERS = ((120, SLAB[1], P_MID), (60, P_MID, P_DECK), (100, P_DECK, P_TOP))


def platforms(b):
    lad = math.radians(10)
    # each ring is open where the ladder from the level below comes up through it
    for y, r0, deg in ((P_MID, RV + 0.15, 120), (P_DECK, RV + 0.15, 60), (P_TOP, 1.45, 100)):
        g = b.f(deg)
        wall_ring(b, r0, y, [(g + lad, g - lad + 2 * math.pi)])
    # the heater room's floor (the plinth's top) needs no grating; ladders between the levels
    for deg, y0, y1 in LADDERS:
        f = b.f(deg)
        x, _, z = b.p(2.42, f, 0)
        n = max(1, int((y1 - y0) / 1.5))
        RP.ladder(b, b.still, x, z, y0, y1, along=b.tang(f), standoffs=[y0 + (y1 - y0) * (k + 0.5) / n for k in range(n)],
                  standoff_to=W_IN - 2.42 + 0.006, wall_dir=b.dirh(f))


def door(b):
    """The locked door at the operating floor, its frame, the HIGH RADIATION AREA sign and the enclosure's tag."""
    L = b.still
    f = b.f(0)
    n = b.dirh(f)
    t = b.tang(f)
    half_a = S.DOOR_HALF * 1.2
    rm = W_IN + 0.3
    half = rm * math.tan(half_a)
    c = b.p(rm, f, OP + 1.1)
    RP.obox(b, L, c, t, (0, 1, 0), half, 1.1, 0.03, "paint_grey")
    face = tuple(c[i] + n[i] * 0.03 for i in range(3))
    for s in (-1, 1):
        q = b.p(W_OUT + 0.03, f + s * (half_a + 0.02), OP + 1.15)
        RP.obox(b, L, q, b.dirh(f + s * half_a), (0, 1, 0), 0.03, 1.15, 0.05, "gunmetal")
    RP.obox(b, L, b.p(W_OUT + 0.03, f, OP + 2.26), n, (0, 1, 0), 0.03, 0.05, W_OUT * math.sin(half_a) + 0.1, "gunmetal")
    for k in (0.3, 1.1, 1.9):
        hc = tuple(face[i] + t[i] * (-half + 0.05) for i in range(3))
        RP.cyl(b, L, (hc[0], OP + k - 0.08, hc[2]), (hc[0], OP + k + 0.08, hc[2]), 0.02, "gunmetal", "gunmetal", 8, both=True)
    hd = tuple(face[i] + t[i] * (half - 0.12) for i in range(3))
    hb = tuple((hd[0], OP + 1.0, hd[2])[i] + n[i] * 0.05 for i in range(3))
    RP.cyl(b, L, (hd[0], OP + 1.0, hd[2]), hb, 0.012, "stainless", "stainless", 6, both=False)
    RP.cyl(b, L, (hb[0], hb[1] - 0.12, hb[2]), (hb[0], hb[1] + 0.02, hb[2]), 0.016, "stainless", "stainless", 6, both=True)
    hp = tuple((hd[0], OP + 1.25, hd[2])[i] + n[i] * 0.02 for i in range(3))
    RP.obox(b, L, hp, t, (0, 1, 0), 0.06, 0.02, 0.01, "stainless_dark")
    pl = tuple(hp[i] + n[i] * 0.03 for i in range(3))
    RP.obox(b, L, (pl[0], pl[1] - 0.05, pl[2]), t, (0, 1, 0), 0.025, 0.03, 0.012, sw("red"))
    fs = f - (half_a + 0.16)
    plate(b, "SIGN_HRA", tuple(b.p(W_OUT, fs, OP + 1.5)[i] + b.dirh(fs)[i] * 0.012 for i in range(3)), b.dirh(fs))
    ft = f + (half_a + 0.12)
    tag(b, "PRESSURIZER ENCLOSURE", b.p(W_OUT, ft, OP + 1.6), b.dirh(ft), d=0.012)


def gauge(b):
    """The pressure gauge on a post at the top platform, its line down to the upper level tap's line."""
    L = b.still
    f = b.f(300)
    n = b.dirh(f + math.pi)
    post = b.p(2.55, f, 0)
    gy = P_TOP + 1.25
    RP.cyl(b, L, (post[0], P_TOP, post[2]), (post[0], gy + 0.22, post[2]), 0.03, "gunmetal", "gunmetal", 8, both=False)
    RP.cyl(b, L, (post[0], P_TOP, post[2]), (post[0], P_TOP + 0.012, post[2]), 0.07, "gunmetal", "gunmetal", 10, both=False)
    q = b.p(2.42, f, 0)
    RP.cyl(b, L, (q[0], 24.8, q[2]), (q[0], gy - 0.075, q[2]), 0.012, "stainless", "stainless", 6, both=False)
    gc = (q[0], gy, q[2])
    RP.cyl(b, L, gc, tuple(gc[i] + n[i] * 0.04 for i in range(3)), 0.075, "black", "black", 18, both=True)
    RP.obox(b, L, tuple((gc[i] + post[i]) / 2 if i != 1 else gy for i in range(3)), b.dirh(f), (0, 1, 0), 0.065, 0.02, 0.01, "gunmetal")
    gf = tuple(gc[i] + n[i] * 0.041 for i in range(3))
    picture(b, "DIAL_PZR", gf, n)
    b.live["dial"] = {"centre": [round(v, 4) for v in tuple(gf[i] + n[i] * 0.002 for i in range(3))],
                      "normal": [round(v, 4) for v in n], "r": 0.06, "low": 0, "high": 3000}
    tc = (gc[0], gy + 0.16, gc[2])
    RP.obox(b, L, tuple(tc[i] - n[i] * 0.004 for i in range(3)), RM._right(n), (0, 1, 0), 0.145, 0.03, 0.004, "stainless")
    tag(b, "PZR PRESS  PSIG", tc, n)
    arm = tuple((tc[i] - n[i] * 0.008 + post[i]) / 2 for i in range(3))
    RP.obox(b, L, (arm[0], gy + 0.16, arm[2]), b.dirh(f), (0, 1, 0), math.dist((tc[0], tc[2]), (post[0], post[2])) / 2 - 0.004, 0.015, 0.008, "gunmetal")


def build(stand=False):
    b = PZR()
    b.stand = stand
    vessel(b)
    heaters(b)
    skirt(b)
    shell_fittings(b)
    top_valves(b)
    if not stand:
        enclosure(b)
        platforms(b)
        door(b)
        gauge(b)
    return b


def placed(b, pressure=2235):
    import fuel_transfer as F
    o = b.ops()
    still = list(o["still"])
    d = b.live["dial"]
    if d:
        still += F.needle_ops(d["centre"], d["normal"], d["r"], d["low"], d["high"], pressure)
    return {"still": still, "lit": o["lit"]}


def piping_context(b):
    """The surge line from loop 2's hot leg (machine 10 builds it properly)."""
    h = RH.Head()
    pipe(h, surge_route(b), 0.25, "stainless", 0.7, sides=16)
    return h.ops()


def ops_check():
    b = build()
    return len(CB.overlaps(placed(b) | {"blink": []}))


if __name__ == "__main__":
    print("overlaps", ops_check())
