"""The reactor coolant loop piping and its supports (reactor building rebuild, machine 10).

Built like a Westinghouse four-loop plant's reactor coolant system piping (Westinghouse Technology Systems Manual 3.2:
austenitic stainless; hot leg 29 in ID x 2.84 in, crossover leg 31 in x 2.99 in, cold leg 27.5 in x 2.69 in; the
surge line 14 in x 1.40 in from loop 2's hot leg; spray from two cold legs through scoops; hot leg RTDs in three
wells 120 degrees apart, a cold leg RTD downstream of each pump; elbow flow taps on the crossover leg's elbow; loop
drains; the residual heat removal suction off a hot leg; accumulator injection into each cold leg; charging into a
cold leg, letdown off a crossover leg), one block a metre:

- Each loop: the hot leg from the vessel's outlet nozzle straight out through the primary shield and up its 50 degree
  elbow into the steam generator; the crossover leg down off the generator's outlet nozzle, under it, out through its
  enclosure and along the loop seal to under the pump and up into its suction; the cold leg from the pump's discharge
  straight back to the vessel's inlet nozzle. All in reflective metal insulation (jacket bands, gore bands on the
  bends) with the welds bare at the nozzles.
- Their fittings: hot and cold leg RTD wells with their heads and conduits to a junction box on a stand; the elbow
  taps with root valves and tubing to the enclosure's wall; a loop drain off the loop seal's bottom with its valve; the
  branch nozzles and their lines down to floor penetrations (accumulator injection with its check valve on each cold
  leg, charging on loop 1, the RHR suction with its motor-operated valve on loop 4, letdown on loop 3).
- Supports and restraints: a pipe whip restraint on each hot and cold leg where it leaves the primary shield (a steel
  frame round the pipe, crushable honeycomb pads, brackets back to the wall); two saddle supports under each
  crossover leg's loop seal; spring hangers under the surge line.
- The surge line from the top of loop 2's hot leg up at a fall to the pressurizer's surge nozzle; the two spray lines
  from scoops in loops 1 and 2's cold legs along the south side to the pressurizer's enclosure.

Live: none (the piping has no local readouts).
"""
import math

import control_boards as CB
from control_boards import sw
import reactor_head as RH
import rb_parts as RP
import refuelling_machine as RM
import steam_generator as S
import coolant_pump as P
import pressurizer as Z
from reactor_head import Head, C, legend_size, _cross, _unit
from steam_generator import tag, pipe

OP = RM.OP
FLOOR = S.FLOOR
NY = RH.NOZZLE_Y
HOT = (0.44, 0.51)
XO = (0.47, 0.54)
COLD = (0.42, 0.46)
SURGE = (0.21, 0.27)
SPRAY = (0.057, 0.1)
ACC = (0.137, 0.19)
RHR = (0.162, 0.22)
SMALL = (0.045, 0.08)
DRAIN = (0.03, 0.06)
SHIELD_R = 7.1


def add(a, b):
    return tuple(a[i] + b[i] for i in range(3))


def mul(a, s):
    return tuple(v * s for v in a)


def along(p, q, t):
    """The point t along p -> q (t in metres)."""
    d = _unit(tuple(q[i] - p[i] for i in range(3)))
    return tuple(p[i] + d[i] * t for i in range(3))


def frame_of(d):
    """Two unit vectors square to a pipe's direction d: up-ish and side."""
    d = _unit(d)
    up = (0.0, 1.0, 0.0)
    side = _unit(_cross(d, up)) if abs(d[1]) < 0.99 else (1.0, 0.0, 0.0)
    upv = _unit(_cross(side, d))
    return upv, side


def insulated(b, pts, size, R, bare_a=True, bare_b=True, sides=20):
    """A run of insulated pipe through pts (pipe radius, insulation radius); its ends bare for 0.3 at a weld (a weld
    band, the insulation's end collar) where bare_a / bare_b."""
    r, ri = size
    L = b.still
    pts = list(pts)
    ends = []
    if bare_a:
        e = along(pts[0], pts[1], 0.3)
        ends.append((pts[0], e))
        pts[0] = e
    if bare_b:
        e = along(pts[-1], pts[-2], 0.3)
        ends.append((pts[-1], e))
        pts[-1] = e
    pipe(b, pts, ri, "stainless", R, sides=sides)
    for w, e in ends:
        RP.cyl(b, L, w, e, r, "stainless", "stainless", sides, both=False)
        d = _unit(tuple(e[i] - w[i] for i in range(3)))
        RP.cyl(b, L, along(w, e, 0.04), along(w, e, 0.08), r + 0.012, "weld_band", "weld_band", sides, both=True)
        RP.cyl(b, L, e, tuple(e[i] + d[i] * 0.02 for i in range(3)), ri + 0.006, "stainless_dark", "stainless_dark", sides, both=True)


def floor_collar(b, p, r):
    RP.cyl(b, b.still, (p[0], FLOOR, p[2]), (p[0], FLOOR + 0.12, p[2]), r + 0.04, "gunmetal", "gunmetal", 12, both=False)
    RP.cyl(b, b.still, (p[0], FLOOR, p[2]), (p[0], FLOOR + 0.015, p[2]), r + 0.1, "gunmetal", "gunmetal", 12, both=False)


def boss(b, c, n, r, r_out, length):
    """A branch nozzle (a sweepolet) on a pipe's surface at c, pointing n."""
    RP.cyl(b, b.still, tuple(c[i] - n[i] * 0.05 for i in range(3)), tuple(c[i] + n[i] * length for i in range(3)), r_out,
           "stainless", "stainless", 14, both=False)
    RP.cyl(b, b.still, tuple(c[i] + n[i] * length for i in range(3)), tuple(c[i] + n[i] * (length + 0.04) for i in range(3)), r + 0.01,
           "weld_band", "weld_band", 12, both=False)


def plate_tag(b, text, c, n):
    P.plate_tag(b, text, c, n)


def check_valve(b, c, d, r):
    """A swing check valve on a line along d: its body, the bolted cover on top, the hinge pin's boss."""
    L = b.still
    d = _unit(d)
    RP.cyl(b, L, tuple(c[i] - d[i] * r * 2.4 for i in range(3)), tuple(c[i] + d[i] * r * 2.4 for i in range(3)), r * 1.6, "gunmetal", "gunmetal", 14, both=True)
    upv, side = frame_of(d)
    top = tuple(c[i] + upv[i] * r * 1.5 for i in range(3))
    RP.cyl(b, L, c, top, r * 1.25, "gunmetal", "gunmetal", 14, both=False)
    RP.cyl(b, L, top, tuple(top[i] + upv[i] * r * 0.4 for i in range(3)), r * 1.5, "gunmetal", "gunmetal", 14, both=True)
    RP.bolt_circle(b, L, tuple(top[i] + upv[i] * r * 0.4 for i in range(3)), upv, r * 1.25, 8, r=max(0.008, r * 0.12), h=0.015)
    for e in (-1, 1):
        RP.cyl(b, L, tuple(c[i] + side[i] * e * r * 1.5 for i in range(3)), tuple(c[i] + side[i] * e * r * 1.8 for i in range(3)), r * 0.4, "gunmetal", "gunmetal", 8, both=True)
    for e in (-1, 1):
        fl = tuple(c[i] + d[i] * e * r * 2.5 for i in range(3))
        RP.cyl(b, L, tuple(fl[i] - d[i] * 0.02 for i in range(3)), tuple(fl[i] + d[i] * 0.02 for i in range(3)), r * 1.9, "gunmetal", "gunmetal", 14, both=True)


# ---------------------------------------------------------------- the loops

class Piping(Head):
    def __init__(self):
        CB.Board.__init__(self, 9, 1000)
        self.live = {}
        self.spray_tap = {}


def routes(loop):
    """The three legs' centre lines (world points) for a loop: hot (generator to vessel), crossover (generator to
    pump), cold (pump to vessel)."""
    sg = S.SG(loop)
    pu = P.Pump(loop)
    hot = S.hot_leg_route(sg)
    xo, _ = S.crossover_route(sg)
    a = pu.L(0, 0, 0)
    xo = xo[:-1] + [(a[0], S.X_Y, a[2]), (a[0], P.SUC_BOT - 0.05, a[2])]
    far = math.hypot(pu.ox - C, pu.oz - C) - 4.1
    cold = [pu.L(1.95, NY, 0), pu.L(far, NY, 0)]
    return hot, xo, cold, sg, pu


def point_at_r(p, q, r):
    """The point on segment p -> q (horizontal, at its y) whose distance from the vessel's axis is r."""
    dx, dz = q[0] - p[0], q[2] - p[2]
    ln = math.hypot(dx, dz)
    ux, uz = dx / ln, dz / ln
    ox, oz = p[0] - C, p[2] - C
    bq = 2 * (ox * ux + oz * uz)
    cq = ox * ox + oz * oz - r * r
    disc = bq * bq - 4 * cq
    ts = [(-bq + s_ * math.sqrt(max(0.0, disc))) / 2 for s_ in (-1, 1)]
    t = min(t for t in ts if t >= 0) if any(t >= 0 for t in ts) else 0
    return (p[0] + ux * t, p[1], p[2] + uz * t)


def whip_restraint(b, c, d, r_ins, wall_side, name):
    """A pipe whip restraint round a pipe at c (direction d): a square frame of thick plates, four honeycomb crush pads
    against the insulation, two brackets back along the pipe to plates on the primary shield's face."""
    L = b.still
    upv, side = frame_of(d)
    a = r_ins + 0.11
    t = 0.12
    for v, u, half in ((upv, side, a + t), (side, upv, a)):
        for e in (-1, 1):
            RP.obox(b, L, tuple(c[i] + v[i] * e * (a + t / 2) for i in range(3)), u, d, half, 0.17, t / 2, "gunmetal")
            RP.obox(b, L, tuple(c[i] + v[i] * e * (r_ins + 0.055) for i in range(3)), u, d, 0.22, 0.14, 0.05, "stainless_dark")
    # brackets from the frame's sides back to the wall
    for e in (-1, 1):
        s0 = tuple(c[i] + side[i] * e * (a + t / 2) for i in range(3))
        back = wall_side
        RP.obox(b, L, tuple(s0[i] - d[i] * (back / 2 + 0.17) for i in range(3)), d, upv, back / 2, 0.16, t / 2 + 0.01, "gunmetal")
        wp = tuple(s0[i] - d[i] * (back + 0.17 + 0.02) for i in range(3))
        RP.obox(b, L, wp, d, upv, 0.02, 0.32, 0.22, "gunmetal")
        for v in (-1, 1):
            for w in (-1, 1):
                q = tuple(wp[i] + d[i] * 0.02 + upv[i] * v * 0.24 + side[i] * w * 0.15 for i in range(3))
                RP.bolt(b, L, q, d, 0.02, 0.02, True)
    plate_tag(b, "PIPE WHIP RESTRAINT", tuple(c[i] + d[i] * 0.175 + upv[i] * (a + t / 2) for i in range(3)), d)
    plate_tag(b, name, tuple(c[i] + d[i] * 0.175 - upv[i] * (a + t / 2) for i in range(3)), d)


def rtd_wells(b, c, d, r_pipe, r_ins, clocks, jb_side):
    """RTD wells round a pipe at c (direction d) at the given clock angles (0 the top): boss, RTD head, its flexible
    conduit to a junction box on a stand beside the pipe."""
    L = b.still
    upv, side = frame_of(d)
    jb = tuple(c[i] + side[i] * jb_side * (r_ins + 0.9) for i in range(3))
    jb = (jb[0], FLOOR + 1.25, jb[2])
    RP.cyl(b, L, (jb[0], FLOOR, jb[2]), (jb[0], jb[1] - 0.2, jb[2]), 0.04, "gunmetal", "gunmetal", 8, both=False)
    RP.cyl(b, L, (jb[0], FLOOR, jb[2]), (jb[0], FLOOR + 0.012, jb[2]), 0.1, "gunmetal", "gunmetal", 10, both=False)
    RP.obox(b, L, jb, side, (0, 1, 0), 0.06, 0.2, 0.16, "gunmetal")
    for k, ang in enumerate(clocks):
        n = tuple(math.cos(ang) * upv[i] + math.sin(ang) * side[i] for i in range(3))
        base = tuple(c[i] + n[i] * r_pipe for i in range(3))
        RP.cyl(b, L, base, tuple(base[i] + n[i] * (r_ins - r_pipe + 0.1) for i in range(3)), 0.045, "stainless", "stainless", 10, both=False)
        hd = tuple(base[i] + n[i] * (r_ins - r_pipe + 0.1) for i in range(3))
        RP.cyl(b, L, hd, tuple(hd[i] + n[i] * 0.12 for i in range(3)), 0.04, "stainless_dark", "stainless_dark", 10, both=True)
        tip = tuple(hd[i] + n[i] * 0.14 for i in range(3))
        mid = tuple(tip[i] + n[i] * 0.15 for i in range(3))
        drop = (jb[0] - side[0] * jb_side * 0.07 + d[0] * (k - 1) * 0.08, max(jb[1] + 0.05, min(mid[1], jb[1] + 0.12)), jb[2] - side[2] * jb_side * 0.07 + d[2] * (k - 1) * 0.08)
        pts = [tip, mid]
        if mid[1] > drop[1] + 0.35:
            pts.append((mid[0], drop[1] + 0.3, mid[2]))
        pts.append(drop)
        pipe(b, pts, 0.011, "stainless_dark", 0.1, sides=6, jacket=False)


def elbow_taps(b, pts, r_pipe, r_ins, sg):
    """The crossover leg's elbow flow taps on the outside of its first 90 degree bend (under the generator): three
    taps with root valves, their tubing to sleeves in the enclosure's wall."""
    L = b.still
    v1, v2 = pts[1], pts[2]
    c = (v2[0], v2[1] + 0.95, v2[2])
    out = _unit((v2[0] - sg.ox, 0, v2[2] - sg.oz))
    side = _unit(_cross(out, (0, 1, 0)))
    for k, (s_, dy) in enumerate(((0.0, 0.0), (-0.22, -0.3), (0.22, -0.3))):
        base = tuple(c[i] + side[i] * s_ + out[i] * r_pipe for i in range(3))
        base = (base[0], c[1] + dy, base[2])
        tipp = tuple(base[i] + out[i] * (r_ins - r_pipe + 0.12) for i in range(3))
        RP.cyl(b, L, base, tipp, 0.025, "stainless", "stainless", 8, both=False)
        S.valve(b, tuple(tipp[i] + out[i] * 0.1 for i in range(3)), out, r=0.016, wheel=0.045)
        f = math.atan2((tipp[0] - sg.ox) * sg.es[0] + (tipp[2] - sg.oz) * sg.es[2], (tipp[0] - sg.ox) * sg.ein[0] + (tipp[2] - sg.oz) * sg.ein[2])
        wall = sg.p(S.W_IN - 0.02, f, tipp[1])
        RP.cyl(b, L, tipp, wall, 0.012, "stainless", "stainless", 6, both=False)
        RP.cyl(b, L, sg.p(S.W_IN - 0.09, f, tipp[1]), sg.p(S.W_IN + 0.03, f, tipp[1]), 0.05, "gunmetal", "gunmetal", 12, both=False)
    return c


def branch_down(b, c, n, size, out_len, valve=None, low=None):
    """A branch line: off the pipe's surface at c along n for out_len, then down to a floor penetration (low: the
    height of a horizontal run before it goes down, for a motor-operated valve)."""
    r, ri = size
    p0 = c
    p1 = tuple(c[i] + n[i] * out_len for i in range(3))
    if low is None:
        pts = [p0, p1, (p1[0], FLOOR + 0.12, p1[2])]
    else:
        p2 = (p1[0], low, p1[2])
        h = _unit((n[0], 0, n[2]))
        p3 = tuple(p2[i] + h[i] * 1.3 for i in range(3))
        pts = [p0, p1, p2, p3, (p3[0], FLOOR + 0.12, p3[2])]
    insulated(b, pts, size, max(0.25, ri * 2.2), bare_a=True, bare_b=False, sides=12)
    floor_collar(b, pts[-1], ri)
    return pts


def loop(b, k):
    L = b.still
    hot, xo, cold, sg, pu = routes(k)
    # the legs
    insulated(b, hot, HOT, 1.2, bare_a=True, bare_b=False)
    insulated(b, xo, XO, 0.9, bare_a=True, bare_b=True)
    insulated(b, cold, COLD, 1.0, bare_a=True, bare_b=False)
    vh, vc = hot[2], cold[1]                                    # the vessel's ends
    out_h = _unit(tuple(hot[1][i] - vh[i] for i in range(3)))
    out_c = _unit(tuple(cold[0][i] - vc[i] for i in range(3)))
    # whip restraints where they leave the primary shield
    for v, out, size, name in ((vh, out_h, HOT, "RCS LOOP %d HOT LEG" % k), (vc, out_c, COLD, "RCS LOOP %d COLD LEG" % k)):
        rc = SHIELD_R + 0.62
        if math.dist((v[0], v[2]), (cold[0][0], cold[0][2])) < 4.5 and size == COLD:
            continue                     # loop 2's short cold leg: the pump's tie bars hold it
        c = point_at_r(v, tuple(v[i] + out[i] * 10 for i in range(3)), rc)
        whip_restraint(b, c, out, size[1], rc - 0.17 - SHIELD_R - 0.02, name)
    # hot leg RTDs (three wells), cold leg RTD downstream of the pump
    ch = point_at_r(vh, tuple(vh[i] + out_h[i] * 10 for i in range(3)), SHIELD_R + 1.55)
    rtd_wells(b, ch, out_h, HOT[0], HOT[1], (0.0, math.radians(120), math.radians(-120)), 1)
    cc_r = min(SHIELD_R + 1.4, math.hypot(cold[0][0] - C, cold[0][2] - C) - 0.6)
    cc = point_at_r(vc, tuple(vc[i] + out_c[i] * 10 for i in range(3)), cc_r)
    rtd_wells(b, cc, out_c, COLD[0], COLD[1], (0.0,), -1)
    plate_tag(b, "HOT LEG RTD", tuple(ch[i] + frame_of(out_h)[1][i] * (HOT[1] + 0.84) + (0, FLOOR + 1.55 - ch[1], 0)[i] for i in range(3)), frame_of(out_h)[1])
    # accumulator injection on the cold leg's side, its check valve, down to the floor
    upv, side = frame_of(out_c)
    acc_side = {1: -1, 2: -1, 3: 1, 4: 1}[k]
    n = tuple(side[i] * acc_side for i in range(3))
    acc_r = cc_r - 0.45 if cc_r - 0.45 > SHIELD_R + 0.95 else cc_r
    cacc = point_at_r(vc, tuple(vc[i] + out_c[i] * 10 for i in range(3)), acc_r)
    c0 = tuple(cacc[i] + n[i] * COLD[0] for i in range(3))
    boss(b, c0, n, ACC[0], ACC[0] + 0.05, 0.1)
    pts = branch_down(b, tuple(c0[i] + n[i] * 0.14 for i in range(3)), n, ACC, 0.9)
    check_valve(b, (pts[1][0], 8.0, pts[1][2]), (0, 1, 0), ACC[0])
    plate_tag(b, "ACCUMULATOR INJECTION", (pts[1][0] + n[0] * (ACC[1] + 0.01), 7.3, pts[1][2] + n[2] * (ACC[1] + 0.01)), n)
    # the loop drain at the loop seal's run under the pump's side
    p2, p3 = xo[3], xo[4]
    free = [t / 40 for t in range(41) if math.hypot(*(along(p2, p3, t / 40 * math.dist(p2, p3))[i] - (sg.ox, 0, sg.oz)[i] for i in (0, 2))) > S.W_OUT + 0.7
            and math.hypot(*(along(p2, p3, t / 40 * math.dist(p2, p3))[i] - (pu.ox, 0, pu.oz)[i] for i in (0, 2))) > 3.4]
    if free:
        dc = along(p2, p3, (free[0] + free[-1]) / 2 * math.dist(p2, p3))
    else:                                        # loop 2: no room outside, the drain is inside the enclosure
        dc = along(xo[2], xo[3], 0.55 * math.dist(xo[2], xo[3]))
        p2, p3 = xo[2], xo[3]
    bot = (dc[0], dc[1] - XO[0], dc[2])
    RP.cyl(b, L, bot, (dc[0], FLOOR + 0.3, dc[2]), DRAIN[0], "stainless", "stainless", 8, both=False)
    hx = _unit(_cross(_unit(tuple(p3[i] - p2[i] for i in range(3))), (0, 1, 0)))
    dend = tuple((dc[0], FLOOR + 0.3, dc[2])[i] + hx[i] * 0.45 for i in range(3))
    pipe(b, [(dc[0], FLOOR + 0.42, dc[2]), (dc[0], FLOOR + 0.3, dc[2]), dend, (dend[0], FLOOR + 0.12, dend[2])], DRAIN[0], "stainless", 0.08, sides=8, jacket=False)
    S.valve(b, tuple((dc[0], FLOOR + 0.3, dc[2])[i] + hx[i] * 0.22 for i in range(3)), hx, r=DRAIN[0], wheel=0.08)
    floor_collar(b, dend, DRAIN[0])
    plate_tag(b, "LOOP DRAIN", (dend[0] + hx[0] * 0.02, FLOOR + 0.75, dend[2] + hx[2] * 0.02), hx)
    # saddle supports under the loop seal's run
    for t in (0.22, 0.7):
        sc = along(p2, p3, t * math.dist(p2, p3))
        if math.hypot(sc[0] - pu.ox, sc[2] - pu.oz) < 3.25 or math.hypot(sc[0] - sg.ox, sc[2] - sg.oz) < S.W_OUT + 0.6:
            continue
        saddle(b, sc, _unit(tuple(p3[i] - p2[i] for i in range(3))), XO[1])
    # elbow taps under the generator
    elbow_taps(b, xo, XO[0], XO[1], sg)
    # the loop's own branches
    b.spray_tap[k] = (cacc, tuple(-v for v in n))
    if k == 4:
        nn = tuple(-v for v in n)
        c0 = tuple(cacc[i] + nn[i] * COLD[0] for i in range(3))
        boss(b, c0, nn, SMALL[0], SMALL[0] + 0.03, 0.08)
        pts = branch_down(b, tuple(c0[i] + nn[i] * 0.1 for i in range(3)), nn, SMALL, 0.6)
        check_valve(b, (pts[1][0], 8.2, pts[1][2]), (0, 1, 0), SMALL[0])
        plate_tag(b, "CHARGING", (pts[1][0] + nn[0] * 0.09, 7.4, pts[1][2] + nn[2] * 0.09), nn)
    if k == 4:
        cr = point_at_r(vh, tuple(vh[i] + out_h[i] * 10 for i in range(3)), SHIELD_R + 2.5)
        down = (0.0, -1.0, 0.0)
        c0 = (cr[0], cr[1] - HOT[0], cr[2])
        boss(b, c0, down, RHR[0], RHR[0] + 0.05, 0.12)
        p1 = (c0[0], 7.7, c0[2])
        hside = frame_of(out_h)[1]
        p2_ = tuple(p1[i] + hside[i] * 1.6 for i in range(3))
        pts = [(c0[0], c0[1] - 0.16, c0[2]), p1, p2_, (p2_[0], FLOOR + 0.12, p2_[2])]
        insulated(b, pts, RHR, 0.5, bare_a=True, bare_b=False, sides=14)
        floor_collar(b, pts[-1], RHR[1])
        import pressurizer as _Z
        _Z.gate_mov(b, tuple(p1[i] + hside[i] * 0.8 for i in range(3)), hside, r=RHR[0])
        plate_tag(b, "RHR SUCTION", (p2_[0] + hside[0] * (RHR[1] + 0.01), 7.0, p2_[2] + hside[2] * (RHR[1] + 0.01)), hside)
    if k == 3:
        lc = along(p2, p3, (free[0] + 0.3 * (free[-1] - free[0])) * math.dist(p2, p3) if free else 0.3 * math.dist(p2, p3))
        nn = hx
        c0 = tuple(lc[i] + nn[i] * XO[0] for i in range(3))
        boss(b, c0, nn, SMALL[0], SMALL[0] + 0.03, 0.08)
        pts = branch_down(b, tuple(c0[i] + nn[i] * 0.1 for i in range(3)), nn, SMALL, 0.5)
        S.valve(b, (pts[1][0], 6.6, pts[1][2]), (0, 1, 0), r=SMALL[0], wheel=0.1)
        plate_tag(b, "LETDOWN", (pts[1][0] + nn[0] * 0.09, 7.1, pts[1][2] + nn[2] * 0.09), nn)


def saddle(b, c, d, r_ins):
    """A saddle support under a horizontal pipe at c: two stanchions on base plates, the cradle and a U strap."""
    L = b.still
    upv, side = frame_of(d)
    yb = c[1] - r_ins - 0.06
    for e in (-1, 1):
        q = tuple(c[i] + side[i] * e * 0.32 for i in range(3))
        RP.ibeam(b, L, (q[0], FLOOR + 0.03, q[2]), (q[0], yb - 0.01, q[2]), d, 0.16, 0.16, mat=S.SUPPORT)
        RP.obox(b, L, (q[0], FLOOR + 0.015, q[2]), side, (0, 1, 0), 0.16, 0.015, 0.16, "gunmetal")
        for v in (-1, 1):
            for w in (-1, 1):
                RP.bolt(b, L, (q[0] + side[0] * v * 0.11 + d[0] * w * 0.11, FLOOR + 0.03, q[2] + side[2] * v * 0.11 + d[2] * w * 0.11), (0, 1, 0), 0.014, 0.014, True)
    RP.obox(b, L, (c[0], yb + 0.02, c[2]), side, (0, 1, 0), 0.48, 0.03, 0.2, S.SUPPORT)
    # the cradle's curved shoe and the strap over the pipe, in short straight pieces
    n = 10
    for j in range(n):
        a0, a1 = math.pi * j / n, math.pi * (j + 1) / n
        for rr, w, mat in ((r_ins + 0.012, 0.08, "gunmetal"),):
            p0 = tuple(c[i] + side[i] * rr * math.cos(a0) + upv[i] * rr * math.sin(a0) for i in range(3))
            p1 = tuple(c[i] + side[i] * rr * math.cos(a1) + upv[i] * rr * math.sin(a1) for i in range(3))
            RP.cyl(b, L, p0, p1, 0.012, mat, mat, 6, both=False)
        p0 = tuple(c[i] - side[i] * (r_ins + 0.03) * math.cos(a0) - upv[i] * (r_ins + 0.03) * math.sin(a0) for i in range(3))
        p1 = tuple(c[i] - side[i] * (r_ins + 0.03) * math.cos(a1) - upv[i] * (r_ins + 0.03) * math.sin(a1) for i in range(3))
        RP.cyl(b, L, p0, p1, 0.03, "gunmetal", "gunmetal", 6, both=False)
    plate_tag(b, "CROSSOVER SUPPORT", tuple((c[0], yb - 0.4, c[2])[i] + side[i] * 0.32 + d[i] * 0.09 for i in range(3)), d)


# ---------------------------------------------------------------- surge and spray

def spring_hanger(b, c, d, r_ins):
    """A spring support under a pipe at c: a stand from the floor, the spring can on it (its load scale), the load
    column up to a clamp round the pipe."""
    L = b.still
    yb = c[1] - r_ins - 0.05
    top = yb - 0.75
    for e in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        q = (c[0] + e[0] * 0.18, 0, c[2] + e[1] * 0.18)
        RP.cyl(b, L, (q[0], FLOOR, q[2]), (q[0], top - 0.42, q[2]), 0.035, S.SUPPORT, S.SUPPORT, 8, both=False)
    RP.cyl(b, L, (c[0], top - 0.45, c[2]), (c[0], top - 0.42, c[2]), 0.26, S.SUPPORT, S.SUPPORT, 14, both=True)
    RP.cyl(b, L, (c[0], top - 0.42, c[2]), (c[0], top, c[2]), 0.14, sw("yellow"), sw("yellow"), 16, both=True)
    RP.obox(b, L, (c[0] + 0.142, top - 0.21, c[2]), (0, 0, 1), (0, 1, 0), 0.02, 0.15, 0.003, "stainless")
    RP.cyl(b, L, (c[0], top, c[2]), (c[0], yb, c[2]), 0.03, "stainless_dark", "stainless_dark", 8, both=False)
    RP.cyl(b, L, (c[0], yb - 0.03, c[2]), (c[0], yb + 0.04, c[2]), 0.07, "gunmetal", "gunmetal", 10, both=True)
    upv, side = frame_of(d)
    rr = r_ins + 0.02
    for k in range(12):
        a0, a1 = 2 * math.pi * k / 12, 2 * math.pi * (k + 1) / 12
        p0 = tuple(c[i] + side[i] * rr * math.cos(a0) + upv[i] * rr * math.sin(a0) for i in range(3))
        p1 = tuple(c[i] + side[i] * rr * math.cos(a1) + upv[i] * rr * math.sin(a1) for i in range(3))
        RP.cyl(b, L, p0, p1, 0.025, "gunmetal", "gunmetal", 6, both=False)
    plate_tag(b, "SPRING HANGER", (c[0], top - 0.62, c[2] + 0.222), (0, 0, 1))


def surge_line(b):
    pz = Z.PZR()
    pts = Z.surge_route(pz)
    tap = pts[-1]
    c0 = (tap[0], NY + HOT[0], tap[2])
    boss(b, c0, (0, 1, 0), SURGE[0], SURGE[0] + 0.06, 0.16)
    pts[-1] = (tap[0], NY + HOT[0] + 0.2, tap[2])
    insulated(b, pts, SURGE, 0.7, bare_a=True, bare_b=True, sides=16)
    p1, p2 = pts[1], pts[2]
    d = _unit(tuple(p2[i] - p1[i] for i in range(3)))
    for t in (0.36, 0.72):
        spring_hanger(b, along(p1, p2, t * math.dist(p1, p2)), d, SURGE[1])
    plate_tag(b, "PZR SURGE LINE", tuple(along(p1, p2, 0.5 * math.dist(p1, p2))[i] + (0, -SURGE[1] - 0.09, 0)[i] for i in range(3)), frame_of(d)[1])


SPRAY_ROUTES = {
    # loop: (y of the run, z of the run along the south side, x where it turns to the pressurizer, the pressurizer's exit)
    1: (8.2, C + 8.55, -1),
    2: (8.6, C + 8.2, 1),
}


def spray_lines(b):
    pz = Z.PZR()
    for k, (yr, zr, e) in SPRAY_ROUTES.items():
        c, n = b.spray_tap[k]
        c0 = tuple(c[i] + n[i] * COLD[0] for i in range(3))
        boss(b, c0, n, SPRAY[0], SPRAY[0] + 0.03, 0.08)
        p0 = tuple(c0[i] + n[i] * 0.12 for i in range(3))
        p1 = tuple(c0[i] + n[i] * 0.5 for i in range(3))
        p2 = (p1[0], yr, p1[2])
        h = _unit((n[0], 0, n[2]))
        if abs(h[2]) > 1e-6:
            t = (zr - p2[2]) / h[2]
            p3 = (p2[0] + h[0] * t, yr, zr)
        else:
            p3 = (p2[0], yr, zr)
        fe, ye = Z.SPRAY_EXIT[e]
        ex = pz.p(Z.W_OUT, pz.f(fe), ye)
        out = pz.dirh(pz.f(fe))
        ex2 = tuple(ex[i] + out[i] * 0.25 for i in range(3))
        xt = ex2[0]
        pts = [p0, p1, p2, p3, (xt, yr, zr), (xt, yr, ex2[2]), (xt, ex2[1], ex2[2]), ex]
        insulated(b, pts, SPRAY, 0.3, bare_a=True, bare_b=False, sides=10)
        RP.cyl(b, b.still, tuple(ex[i] - out[i] * 0.01 for i in range(3)), tuple(ex[i] + out[i] * 0.06 for i in range(3)), SPRAY[1] + 0.03, "gunmetal", "gunmetal", 12, both=False)
        plate_tag(b, "PZR SPRAY  LOOP %d" % k, (xt, yr + 0.6, ex2[2] + SPRAY[1] + 0.012), (0, 0, 1))


def build():
    b = Piping()
    for k in (1, 2, 3, 4):
        loop(b, k)
    surge_line(b)
    spray_lines(b)
    return b


def ops_check():
    b = build()
    return len(CB.overlaps(b.ops() | {"blink": []}))


if __name__ == "__main__":
    print("overlaps", ops_check())
