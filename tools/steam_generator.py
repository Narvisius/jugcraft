"""The steam generators (reactor building rebuild, machine 7).

Built like a real Westinghouse four-loop plant's vertical U-tube steam generators (Westinghouse Technology Systems
Manual 3.2, table 3.2-7: 67 ft 9 in tall, upper shell 175 3/4 in OD, 3388 U-tubes 7/8 in OD; support arrangement as in
US 5,152,253's prior art and US 5,651,334), one block a metre:

- The vessel: the hemispherical channel head (the primary head) with its divider plate inside, the hot leg's inlet
  nozzle facing the reactor and the outlet nozzle (to the crossover leg) opposite, both 50 degrees below horizontal;
  two 16 in primary manways (one per plenum) with 20 studs; the tubesheet; the lower shell round the tube bundle; the
  transition cone; the upper shell (the steam drum: swirl vane separators and chevron dryers inside); the elliptical
  top head with the main steam nozzle (its flow restrictor inside) and two secondary manways with davits. Two
  handholes at the tubesheet (on the no-tube lane), the two blowdown connections just above it, the feedwater nozzle
  on the upper shell (the feed ring inside), narrow and wide range level taps with root valves (condensate pots on
  the upper ones), two lifting trunnions. Reflective metal insulation (stainless panels with their banded seams) over
  the lot, the nozzles' and manways' bosses standing through it.
- Its supports: four pin-ended columns under pads on the channel head (spherical bearings, pins and clevises; base
  pads on grout with anchor bolts and levelling nuts); the lower lateral support, a frame of built-up beams round the
  lower shell between the compartment's walls with bumpers (shim packs, a cold gap behind for the thermal growth away
  from the reactor); the upper lateral support, a bolted ring band round the upper shell with four hydraulic
  snubbers (two trains front and back, along the hot leg, the way the generator moves as the loop heats) and two
  seismic keys against bumper brackets either side.
- Its enclosure: the concrete compartment wall round it from the loop floor to above the operating floor (the part
  above the floor is the missile shield), with the hot leg's and crossover leg's openings, a locked door at the loop
  floor and one at the operating floor (CAUTION HIGH RADIATION AREA), and inside: the manway platform under the
  channel head, a grating ring at the operating floor, the platform at the upper lateral support and the top
  platform round the head (on the wall's top, with its handrail), with ladders between them.
- Its lines: the main steam line up off the top, over the wall and down outside it to its floor sleeve; the
  feedwater line out through the wall and down to its sleeve; the blowdown lines with their valves and the level
  sensing lines to wall sleeves; the steam pressure gauge at the top platform (live).

Layout (Justin, Oct 19: "Yes, corner compartments"): one generator at each corner of the cavity, north and south of
it (reactor_head.SG_XZ), each hot leg straight from its vessel nozzle (reactor_head.LOOP_AZ). The frame of each
generator is local: a toward the reactor, s across (s = a x up), y up; the access side (doors, manways, the manway
platform, the data plate) is the side away from the loop's pump (HAND).

Live (LIVE): the steam pressure gauge's needle (psig).
"""
import math

import control_boards as CB
from control_boards import sw
import reactor_head as RH
import rb_parts as RP
import refuelling_machine as RM
from reactor_head import Head, C, legend, legend_size, tile_mat, _cross, _unit

OP = RM.OP
FLOOR = 6.0                      # the loop compartments' floor
APEX = 10.9                      # the channel head's bottom (steel)
R_LOW = 1.715                    # lower shell, 135 in OD
R_UP = 2.232                     # upper shell, 175 3/4 in OD
INS = 0.09                       # the reflective metal insulation's thickness
TS = (APEX + R_LOW, APEX + R_LOW + 0.53)          # the tubesheet (21 in)
LOW_TOP = TS[1] + 9.9                             # the lower shell's top (the cone's bottom)
CONE_TOP = LOW_TOP + 2.0
UP_TOP = CONE_TOP + 5.4                           # the upper shell's top (the head's equator)
HEAD_H = 1.1
TOP = UP_TOP + HEAD_H                             # 67 ft 9 in above the apex
RL, RU = R_LOW + INS, R_UP + INS                  # the insulated radii
W_IN, W_OUT = 3.4, 4.0                            # the compartment wall
W_TOP = 29.8
P_MANWAY = 10.3                                   # platform heights (their tops)
P_DECK = OP
P_UPPER = 24.8
P_TOP = W_TOP
NOZ_POLAR = math.radians(40)                      # the primary nozzles' axes from straight down
HOT_R, HOT_INS = 0.44, 0.51                       # hot leg 29 in ID x 2.84 in wall, its insulation
X_R, X_INS = 0.47, 0.54                           # crossover leg 31 in ID x 2.99 in wall
MS_R, MS_INS = 0.41, 0.5                          # main steam 32 in
FW_R, FW_INS = 0.203, 0.27                        # feedwater 16 in
FW_Y = 26.6
BAND = (25.28, 25.78)                             # the upper lateral support's ring band
LLS = (14.2, 14.65)                               # the lower lateral support's frame
X_Y = 7.0                                         # the crossover leg's run under the generator (its loop seal)
MS_OUT = 5.0                                      # where the main steam and feedwater lines come down outside
HAND = (-1, -1, -1, 1)                            # +1: the access side is +s (loop 4); the pumps are opposite
GREY = "paint_grey"
WALL = sw("enamel")                               # the coated concrete
SUPPORT = "paint_green"                           # the supports' paint
YELLOW = "paint_yellow"


def pump_xz(loop):
    """Where the loop's reactor coolant pump stands (machine 8): on its cold leg's line out from the vessel."""
    az = RH.LOOP_AZ[loop - 1][1]
    d = 10.0 if loop == 2 else 11.0
    return C + d * math.cos(az), C + d * math.sin(az)


class SG(Head):
    def __init__(self, loop=1):
        CB.Board.__init__(self, 9, 700 + loop)
        self.loop = loop
        self.h = HAND[loop - 1]
        self.ox, self.oz = RH.SG_XZ[loop - 1]
        d = _unit((C - self.ox, 0.0, C - self.oz))
        self.ein = d
        self.es = (-d[2], 0.0, d[0])                  # a x up
        self.live = {"dial": None}
        self.stand = False

    # ------------------------------------------------ the local frame
    def L(self, a, y, s):
        return (self.ox + a * self.ein[0] + s * self.es[0], y, self.oz + a * self.ein[2] + s * self.es[2])

    def Lh(self, a, y, s):
        """A point with s on the hand's side (s > 0: the access side)."""
        return self.L(a, y, self.h * s)

    def D(self, a, y, s):
        return (a * self.ein[0] + s * self.es[0], y, a * self.ein[2] + s * self.es[2])

    def f(self, deg):
        """An azimuth (radians, local) from degrees on the hand's side: 0 toward the reactor, 90 the access side."""
        return self.h * math.radians(deg)

    def p(self, r, f, y):
        return self.L(r * math.cos(f), y, r * math.sin(f))

    def dirh(self, f):
        return self.D(math.cos(f), 0.0, math.sin(f))

    def tang(self, f):
        return self.D(-math.sin(f), 0.0, math.cos(f))

    def ndir(self, f, polar):
        """The channel head's outward normal at azimuth f, polar angle from straight down."""
        h = self.dirh(f)
        return (h[0] * math.sin(polar), -math.cos(polar), h[2] * math.sin(polar))

    def world_xz(self, a, s):
        q = self.L(a, 0, s)
        return (q[0], q[2])

    # ------------------------------------------------ boxes and surfaces of revolution in the local frame
    def lbox(self, a0, y0, s0, a1, y1, s1, mat, lst=None):
        a0, a1 = sorted((a0, a1))
        s0, s1 = sorted((s0, s1))
        c = self.L((a0 + a1) / 2, (y0 + y1) / 2, (s0 + s1) / 2)
        RP.obox(self, self.still if lst is None else lst, c, self.ein, (0, 1, 0), (a1 - a0) / 2, (y1 - y0) / 2, (s1 - s0) / 2, mat)

    def rbox(self, f, r0, r1, y0, y1, half_t, mat, lst=None):
        """A box along the radius at azimuth f (r0..r1), half_t either side of it."""
        u = self.dirh(f)
        c = self.p((r0 + r1) / 2, f, (y0 + y1) / 2)
        RP.obox(self, self.still if lst is None else lst, c, u, (0, 1, 0), (r1 - r0) / 2, (y1 - y0) / 2, half_t, mat)

    def rband(self, r0, y0, r1, y1, mat, f0=0.0, f1=2 * math.pi, per_block=1.0, inward=False, lst=None, n=None):
        rm = max(r0, r1, 0.05)
        length = math.hypot(r1 - r0, y1 - y0)
        n = n or max(8, int(round(abs(f1 - f0) * rm * 3 / per_block)))
        seg = abs(f1 - f0) * rm / n
        for i in range(n):
            fa, fb = f0 + (f1 - f0) * i / n, f0 + (f1 - f0) * (i + 1) / n
            if inward:
                fa, fb = fb, fa
            tl, tr = self.p(r1, fa, y1), self.p(r1, fb, y1)
            bl, br = self.p(r0, fa, y0), self.p(r0, fb, y0)
            if isinstance(mat, str):
                u0 = (i * seg / per_block) % 1.0
                m = tile_mat(mat, u0, 0.0, min(1.0, u0 + seg / per_block), min(1.0, max(0.03, length / per_block)))
            else:
                m = mat
            self.quad(tl, bl, br, tr, m, lst)

    def rtube(self, r, y0, y1, mat, f0=0.0, f1=2 * math.pi, inward=False, lst=None):
        y = y0
        while y < y1 - 1e-6:
            yt = min(y1, math.floor(y + 1e-6) + 1.0)
            self.rband(r, y, r, yt, mat, f0, f1, inward=inward, lst=lst)
            y = yt

    def rring(self, r_in, r_out, y, mat, f0=0.0, f1=2 * math.pi, down=False, lst=None, n=None):
        n = n or max(12, int(round(abs(f1 - f0) * r_out * 3)))
        depth = r_out - r_in
        rows = max(1, math.ceil(depth - 1e-6))
        for j in range(rows):
            ri, ro = r_in + depth * j / rows, r_in + depth * (j + 1) / rows
            arc = abs(f1 - f0) * ro / n
            for i in range(n):
                fa, fb = f0 + (f1 - f0) * i / n, f0 + (f1 - f0) * (i + 1) / n
                if down:
                    fa, fb = fb, fa
                o_a, o_b, i_a, i_b = self.p(ro, fa, y), self.p(ro, fb, y), self.p(ri, fa, y), self.p(ri, fb, y)
                if isinstance(mat, str):
                    u0 = (i * arc) % 1.0
                    m = tile_mat(mat, u0, 0.0, min(1.0, u0 + arc), min(1.0, ro - ri))
                else:
                    m = mat
                if ri < 1e-6:
                    self.quad(i_a, o_a, o_b, o_b, m, lst)
                else:
                    self.quad(i_a, o_a, o_b, i_b, m, lst)

    def jamb(self, f, r_in, r_out, y0, y1, facing, mat, lst=None):
        """A radial face at azimuth f (a cut wall's or platform's end), facing +1 (toward increasing f) or -1."""
        a, b_, c, d = self.p(r_in, f, y1), self.p(r_in, f, y0), self.p(r_out, f, y0), self.p(r_out, f, y1)
        if facing < 0:
            self.quad(a, b_, c, d, mat, lst)
        else:
            self.quad(d, c, b_, a, mat, lst)

    def curved_stencil(self, text, r, f, y, lift=0.006):
        """Painted letters on the round wall's outside, in strips following it, reading left to right from outside."""
        w, h = legend_size(text)
        m = legend(text)
        n = max(4, int(w / 0.12))
        df = w / r
        for k in range(n):
            fa, fb = f - df / 2 + df * k / n, f - df / 2 + df * (k + 1) / n
            ua, ub = m[1] + (m[3] - m[1]) * k / n, m[1] + (m[3] - m[1]) * (k + 1) / n
            self.quad(self.p(r + lift, fa, y + h / 2), self.p(r + lift, fa, y - h / 2), self.p(r + lift, fb, y - h / 2),
                      self.p(r + lift, fb, y + h / 2), [m[0], ua, m[2], ub, m[4]])


def _norm(f):
    while f <= -math.pi:
        f += 2 * math.pi
    while f > math.pi:
        f -= 2 * math.pi
    return f


def _solid(gaps):
    """The angular intervals in (-pi, pi] left solid between gaps (each (f0, f1), f0 < f1, any range)."""
    cuts = []
    for g0, g1 in gaps:
        a, b_ = _norm(g0), _norm(g0) + (g1 - g0)
        if b_ > math.pi:
            cuts += [(a, math.pi), (-math.pi, b_ - 2 * math.pi)]
        else:
            cuts.append((a, b_))
    cuts.sort()
    out, cur = [], -math.pi
    for a, b_ in cuts:
        if a > cur + 1e-6:
            out.append((cur, a))
        cur = max(cur, b_)
    if cur < math.pi - 1e-6:
        out.append((cur, math.pi))
    # join the interval across +-pi
    if len(out) > 1 and abs(out[0][0] + math.pi) < 1e-6 and abs(out[-1][1] - math.pi) < 1e-6:
        out = [(out[-1][0], out[0][1] + 2 * math.pi)] + out[1:-1]
    return out


def ring_wall(b, r_in, r_out, y0, y1, gaps, mat=None, lst=None):
    """A round concrete wall about the generator's axis from y0 to y1, with openings gaps: (f0, f1, ya, yb)."""
    mat = WALL if mat is None else mat
    ys = sorted({y0, y1} | {g[2] for g in gaps if y0 < g[2] < y1} | {g[3] for g in gaps if y0 < g[3] < y1})
    for ya, yb in zip(ys, ys[1:]):
        active = [(g[0], g[1]) for g in gaps if g[2] <= ya + 1e-6 and g[3] >= yb - 1e-6]
        for fa, fb in _solid(active):
            b.rtube(r_out, ya, yb, mat, fa, fb, lst=lst)
            b.rtube(r_in, ya, yb, mat, fa, fb, inward=True, lst=lst)
        for g0, g1 in active:
            b.jamb(g0, r_in, r_out, ya, yb, +1, mat, lst)
            b.jamb(g1, r_in, r_out, ya, yb, -1, mat, lst)
    for g in gaps:
        if g[2] > y0 + 1e-6:
            b.rring(r_in, r_out, g[2], mat, g[0], g[1], lst=lst)
        if g[3] < y1 - 1e-6:
            b.rring(r_in, r_out, g[3], mat, g[0], g[1], down=True, lst=lst)
    top = [(g[0], g[1]) for g in gaps if g[3] >= y1 - 1e-6]
    for fa, fb in _solid(top):
        b.rring(r_in, r_out, y1, mat, fa, fb, lst=lst)


def _bands(L, b, p, q, r, pitch=0.9):
    """The insulation jacket's bands along a straight length of pipe."""
    ln = math.dist(p, q)
    if ln < 0.3:
        return
    d = _unit(tuple(q[i] - p[i] for i in range(3)))
    n = int(ln / pitch)
    for j in range(1, n + 1):
        t = ln * j / (n + 1)
        c = tuple(p[i] + d[i] * t for i in range(3))
        RP.cyl(b, L, tuple(c[i] - d[i] * 0.018 for i in range(3)), tuple(c[i] + d[i] * 0.018 for i in range(3)), r + 0.008, "stainless_dark", "stainless_dark", 18, both=True)


def pipe(b, pts, r, mat, R, lst=None, sides=16, seg_deg=12, jacket=True):
    """A pipe along the polyline pts, each corner bent round radius R (less where the legs are short); jacket: its
    insulation's bands on the straights and a gore band at each joint of the bends."""
    L = b.still if lst is None else lst
    cur = pts[0]
    for i in range(1, len(pts) - 1):
        a, c, d = pts[i - 1], pts[i], pts[i + 1]
        u1 = _unit(tuple(c[k] - a[k] for k in range(3)))
        u2 = _unit(tuple(d[k] - c[k] for k in range(3)))
        cs = max(-1.0, min(1.0, sum(u1[k] * u2[k] for k in range(3))))
        th = math.acos(cs)
        if th < 1e-3:
            continue
        t = min(R * math.tan(th / 2), 0.48 * math.dist(a, c), 0.48 * math.dist(c, d))
        Rr = t / math.tan(th / 2)
        p_in = tuple(c[k] - u1[k] * t for k in range(3))
        p_out = tuple(c[k] + u2[k] * t for k in range(3))
        RP.cyl(b, L, cur, p_in, r, mat, mat, sides, both=False)
        if jacket:
            _bands(L, b, cur, p_in, r)
        m = _unit(tuple(u2[k] - u1[k] * cs for k in range(3)))
        cen = tuple(p_in[k] + m[k] * Rr for k in range(3))
        n = max(2, int(math.degrees(th) / seg_deg) + 1)
        prev = p_in
        for j in range(1, n + 1):
            an = th * j / n
            q = tuple(cen[k] - m[k] * Rr * math.cos(an) + u1[k] * Rr * math.sin(an) for k in range(3))
            dd = _unit(tuple(q[k] - prev[k] for k in range(3)))
            RP.cyl(b, L, tuple(prev[k] - dd[k] * 0.01 for k in range(3)), tuple(q[k] + dd[k] * 0.01 for k in range(3)), r, mat, mat, sides, both=False)
            if jacket and j < n:
                RP.cyl(b, L, tuple(q[k] - dd[k] * 0.015 for k in range(3)), tuple(q[k] + dd[k] * 0.015 for k in range(3)), r + 0.008, "stainless_dark", "stainless_dark", sides, both=True)
            prev = q
        cur = p_out
    RP.cyl(b, L, cur, pts[-1], r, mat, mat, sides, both=False)
    if jacket:
        _bands(L, b, cur, pts[-1], r)


def tag(b, text, c, n, up=(0, 1, 0), d=0.006):
    b.tag(text, c, n, up=up, d=d)


def picture(b, name, centre, normal, lst=None, up=(0, 1, 0), lift=0.002):
    RM.picture(b, name, centre, normal, b.still if lst is None else lst, up=up, lift=lift)


def plate(b, name, centre, n, t=0.006):
    """A flat aluminium sign plate (a bolt at each corner) on a face, its picture on it."""
    w, h = legend_size(name)
    n = _unit(n)
    u = RM._right(n)
    v = (0, 1, 0)
    c = tuple(centre[i] + n[i] * t / 2 for i in range(3))
    RP.obox(b, b.still, c, u, v, w / 2 + 0.01, h / 2 + 0.01, t / 2, "stainless")
    picture(b, name, tuple(centre[i] + n[i] * t for i in range(3)), n, lift=0.003)
    for su in (-1, 1):
        for sv in (-1, 1):
            p = tuple(centre[i] + u[i] * su * (w / 2 - 0.015) + v[i] * sv * (h / 2 - 0.015) + n[i] * (t + 0.002) for i in range(3))
            RP.cyl(b, b.still, p, tuple(p[i] + n[i] * 0.004 for i in range(3)), 0.007, "stainless_dark", "stainless_dark", 6, both=False)


# ---------------------------------------------------------------- the pressure vessel in its insulation

def seam(b, r, y, w=0.035):
    """A banded seam between two insulation panels: a stainless band round the shell."""
    b.rband(r + 0.007, y - w / 2, r + 0.007, y + w / 2, "stainless_dark", n=max(24, int(r * 18)))
    b.rring(r, r + 0.007, y + w / 2, "stainless_dark", n=max(24, int(r * 18)))
    b.rring(r, r + 0.007, y - w / 2, "stainless_dark", down=True, n=max(24, int(r * 18)))


def shell(b):
    # channel head: a hemisphere under the tubesheet, insulated, cut where the support pads come through
    steps = 8
    for k in range(steps):
        t0, t1 = k / steps * math.pi / 2, (k + 1) / steps * math.pi / 2
        b.rband(RL * math.sin(t0), TS[0] - RL * math.cos(t0), RL * math.sin(t1), TS[0] - RL * math.cos(t1), "stainless",
                n=36 if k else 18)
    b.rtube(RL, TS[0], LOW_TOP, "stainless")
    # the cone: two bands
    ym = (LOW_TOP + CONE_TOP) / 2
    rm = (RL + RU) / 2
    b.rband(RL, LOW_TOP, rm, ym, "stainless")
    b.rband(rm, ym, RU, CONE_TOP, "stainless")
    b.rtube(RU, CONE_TOP, UP_TOP, "stainless")
    # the top head (2:1 ellipse in its insulation) up to the steam nozzle's boss
    A, B = RU, HEAD_H + INS
    r_hole = 0.6
    t_end = math.acos(r_hole / A)
    steps = 7
    for k in range(steps):
        t0, t1 = t_end * k / steps, t_end * (k + 1) / steps
        b.rband(A * math.cos(t0), UP_TOP + B * math.sin(t0), A * math.cos(t1), UP_TOP + B * math.sin(t1), "stainless", n=40)
    b.head_hole_y = UP_TOP + B * math.sin(t_end)
    rmi_details(b)


# Where the insulation's panel seams keep clear of what comes through it or is fixed on it: (azimuth on the hand's
# side in degrees, half width in degrees, y0, y1).
KEEP_CLEAR = [(90, 13, P_DECK + 0.7, P_DECK + 1.75), (90, 9, TS[1], TS[1] + 0.75), (-90, 9, TS[1], TS[1] + 0.75),
              (120, 6, TS[1], TS[1] + 0.6), (-120, 6, TS[1], TS[1] + 0.6), (-140, 7, TS[1], TS[1] + 0.8),
              (90, 7, LLS[0] - 0.1, LLS[1] + 0.1), (-90, 7, LLS[0] - 0.1, LLS[1] + 0.1), (180, 7, LLS[0] - 0.1, LLS[1] + 0.1),
              (155, 10, FW_Y - 0.4, FW_Y + 0.8), (140, 7, 25.7, 26.3), (140, 7, 29.1, 29.8), (-140, 7, 29.1, 29.8),
              (90, 10, 27.7, 28.7), (-90, 10, 27.7, 28.7)]


def _clear(b, f, y0, y1):
    for deg, half, ka, kb in KEEP_CLEAR:
        d = abs(_norm(f - b.f(deg)))
        if d < math.radians(half) and y1 > ka and y0 < kb:
            return False
    return True


def latch(b, r, f, y):
    """A panel latch just under a band: its body on the lower panel and the hook over the band."""
    n = b.dirh(f)
    t = b.tang(f)
    RP.obox(b, b.still, b.p(r + 0.012, f, y - 0.075), n, (0, 1, 0), 0.012, 0.035, 0.024, "stainless_dark")
    RP.obox(b, b.still, b.p(r + 0.02, f, y - 0.03), n, (0, 1, 0), 0.006, 0.018, 0.012, "stainless_dark")


def rmi_details(b):
    """The reflective metal insulation's panels made out: the bands between the courses, a standing seam between the
    panels of each course, a latch on each seam under each band; meridian seams on the top head."""
    low = [TS[0] + 0.02, TS[1] + 0.9, TS[1] + 2.1, TS[1] + 3.3, TS[1] + 4.5, TS[1] + 5.7, TS[1] + 6.9, TS[1] + 8.1,
           TS[1] + 9.3, LOW_TOP - 0.02]
    low = [y for y in low if not (LLS[0] - 0.1 < y < LLS[1] + 0.1) and not (P_DECK - 0.15 < y < P_DECK + 0.15)]
    up = [CONE_TOP + 0.02, CONE_TOP + 1.4, CONE_TOP + 2.75, CONE_TOP + 4.1, UP_TOP - 0.02]
    up = [y for y in up if not (BAND[0] - 0.1 < y < BAND[1] + 0.1)]
    for y in low:
        seam(b, RL, y)
    for y in up:
        seam(b, RU, y)
    # the channel head's course seam
    pc = math.radians(80)
    seam(b, RL * math.sin(pc), TS[0] - RL * math.cos(pc), 0.03)
    # standing seams and latches
    for rr, ys, n in ((RL, [TS[0]] + low + [LOW_TOP], 12), (RU, [CONE_TOP] + up + [BAND[0], BAND[1], UP_TOP], 16)):
        ys = sorted(set(ys))
        for k in range(n):
            f = 2 * math.pi * (k + 0.5) / n
            for ya, yb in zip(ys, ys[1:]):
                if yb - ya < 0.2 or (BAND[0] - 0.01 <= ya < BAND[1]) or not _clear(b, f, ya, yb):
                    continue
                RP.cyl(b, b.still, b.p(rr + 0.004, f, ya + 0.02), b.p(rr + 0.004, f, yb - 0.02), 0.011, "stainless", "stainless", 6, both=False)
                if yb in low or yb in up:
                    latch(b, rr, f, yb - 0.02)
    # the cone's seams up the slant
    for k in range(12):
        f = 2 * math.pi * (k + 0.5) / 12
        RP.cyl(b, b.still, b.p(RL + 0.004, f, LOW_TOP + 0.03), b.p(RU + 0.004, f, CONE_TOP - 0.03), 0.011, "stainless", "stainless", 6, both=False)
    # the top head's meridian seams (clear of the manways and the nozzle)
    A, B = RU, HEAD_H + INS
    for k in range(16):
        f = 2 * math.pi * (k + 0.5) / 16
        if min(abs(_norm(f - b.f(90))), abs(_norm(f - b.f(-90)))) < math.radians(16):
            continue
        prev = None
        for j in range(9):
            t = math.acos(0.72 / A) * j / 8
            q = b.p(A * math.cos(t) + 0.004 * math.cos(t), f, UP_TOP + 0.02 + B * math.sin(t) + 0.004 * math.sin(t))
            if prev:
                RP.cyl(b, b.still, prev, q, 0.011, "stainless", "stainless", 6, both=False)
            prev = q


def head_y(r):
    """The insulated top head's surface height at radius r."""
    A, B = RU, HEAD_H + INS
    return UP_TOP + B * math.sqrt(max(0.0, 1 - (r / A) ** 2))


def head_normal(b, r, f):
    A, B = RU, HEAD_H + INS
    y = head_y(r) - UP_TOP
    h = b.dirh(f)
    nr, ny = r / A ** 2, y / B ** 2
    l = math.hypot(nr, ny)
    return (h[0] * nr / l, ny / l, h[2] * nr / l)


def nozzle_ends(b):
    """The primary nozzles' safe ends (centre, axis) for the piping: hot (the inlet) and cold (the outlet)."""
    c0 = b.L(0, TS[0], 0)
    out = {}
    for kind, f in (("hot", 0.0), ("cold", math.pi)):
        d = b.ndir(f, NOZ_POLAR)
        out[kind] = (tuple(c0[i] + d[i] * (R_LOW + 0.5) for i in range(3)), d)
    return out


def primary_nozzles(b):
    L = b.still
    c0 = b.L(0, TS[0], 0)
    for kind, f, r_pipe, r_boss in (("hot", 0.0, HOT_R, 0.62), ("cold", math.pi, X_R, 0.66)):
        d = b.ndir(f, NOZ_POLAR)
        P = lambda t: tuple(c0[i] + d[i] * t for i in range(3))
        RP.cyl(b, L, P(R_LOW - 0.2), P(R_LOW + 0.28), r_boss, "gunmetal", "gunmetal", 20, both=False)
        RP.cyl(b, L, P(RL + 0.02), P(RL + 0.045), r_boss + 0.09, "stainless", "stainless_dark", 22, both=True)     # the RMI's collar
        RP.cyl(b, L, P(R_LOW + 0.28), P(R_LOW + 0.31), r_boss - 0.06, "gunmetal", "gunmetal", 20, both=False)
        RP.cyl(b, L, P(R_LOW + 0.31), P(R_LOW + 0.45), r_pipe + 0.025, "stainless", "stainless", 20, both=False)     # safe end
        RP.cyl(b, L, P(R_LOW + 0.45), P(R_LOW + 0.5), r_pipe + 0.035, "weld_band", "stainless_dark", 20, both=False)


def manway(b, centre, n, r_cover, studs, tag_text=None, davit=False):
    """A bolted manway: the boss (bare steel through the insulation), the cover and its studs and nuts, a tag on the
    cover; a davit for the secondary ones."""
    L = b.still
    n = _unit(n)
    P = lambda t: tuple(centre[i] + n[i] * t for i in range(3))
    RP.cyl(b, L, P(-0.15), P(0.14), r_cover - 0.03, "gunmetal", "gunmetal", 20, both=False)
    RP.cyl(b, L, P(0.14), P(0.2), r_cover + 0.02, "gunmetal", "gunmetal", 20, both=False)     # boss flange
    RP.cyl(b, L, P(0.2), P(0.27), r_cover, "gunmetal", "gunmetal", 20, both=False)            # the cover
    face = P(0.27)
    RP.bolt_circle(b, L, face, n, r_cover - 0.045, studs, r=0.016, h=0.03, a0=math.pi / studs)
    # the cover's two lifting lugs (eyes) either side of the tag
    up = (0, 1, 0)
    side = _unit(_cross(n, up)) if abs(n[1]) < 0.99 else (1, 0, 0)
    w = _unit(_cross(side, n))
    for e in (-1, 1):
        lc = tuple(face[i] + w[i] * e * 0.17 + n[i] * 0.03 for i in range(3))
        RP.obox(b, L, lc, side, n, 0.05, 0.03, 0.012, "gunmetal")
        RP.cyl(b, L, tuple(lc[i] - side[i] * 0.015 + n[i] * 0.005 for i in range(3)), tuple(lc[i] + side[i] * 0.015 + n[i] * 0.005 for i in range(3)), 0.016, "black", "black", 8, both=True)
    if tag_text:
        tag(b, tag_text, face, n)
    if davit:
        up = (0, 1, 0)
        side = _unit(_cross(n, up)) if abs(n[1]) < 0.99 else (1, 0, 0)
        root = tuple(centre[i] + side[i] * (r_cover + 0.07) + n[i] * 0.05 for i in range(3))
        top = (root[0], root[1] + 0.55, root[2])
        RP.cyl(b, L, root, top, 0.03, "gunmetal", "gunmetal", 8, both=False)
        tip = tuple(face[i] - side[i] * 0.0 + n[i] * 0.1 for i in range(3))
        arm_end = (tip[0], top[1], tip[2])
        RP.cyl(b, L, top, arm_end, 0.025, "gunmetal", "gunmetal", 8, both=True)
        RP.cyl(b, L, arm_end, (tip[0], face[1] + 0.03, tip[2]) if arm_end[1] > face[1] + 0.03 else arm_end, 0.012,
               "gunmetal", "gunmetal", 6, both=False)
        RP.cyl(b, L, (root[0], root[1] - 0.03, root[2]), root, 0.05, "gunmetal", "gunmetal", 8, both=False)


def openings(b):
    """The primary manways (one per plenum, on the access side), the secondary manways on the top head, the
    handholes, the lifting trunnions."""
    c0 = b.L(0, TS[0], 0)
    for deg, text in ((70, "PRIMARY MANWAY  HOT LEG"), (110, "PRIMARY MANWAY  COLD LEG")):
        f = b.f(deg)
        n = b.ndir(f, math.radians(60))
        manway(b, tuple(c0[i] + n[i] * R_LOW for i in range(3)), n, 0.36, 20, text)
    for deg, text in ((90, "SECONDARY MANWAY A"), (-90, "SECONDARY MANWAY B")):
        f = b.f(deg)
        r = 1.5
        n = head_normal(b, r, f)
        c = b.p(r, f, head_y(r))
        c = tuple(c[i] - n[i] * INS for i in range(3))
        manway(b, c, n, 0.36, 16, text, davit=True)
    for deg, text in ((90, "HANDHOLE A"), (-90, "HANDHOLE B")):
        f = b.f(deg)
        y = TS[1] + 0.3
        n = b.dirh(f)
        RP.cyl(b, b.still, b.p(R_LOW - 0.05, f, y), b.p(RL + 0.1, f, y), 0.15, "gunmetal", "gunmetal", 14, both=False)
        RP.cyl(b, b.still, b.p(RL + 0.1, f, y), b.p(RL + 0.16, f, y), 0.19, "gunmetal", "gunmetal", 16, both=False)
        RP.bolt_circle(b, b.still, b.p(RL + 0.16, f, y), n, 0.155, 8, r=0.013, h=0.025, a0=math.pi / 8)
        tag(b, text, b.p(RL, f, y + 0.36), n, d=0.014)
    for deg in (90, -90):
        f = b.f(deg)
        y = 28.3
        RP.cyl(b, b.still, b.p(R_UP - 0.05, f, y), b.p(RU + 0.42, f, y), 0.22, "gunmetal", "gunmetal", 16, both=False)
        RP.cyl(b, b.still, b.p(RU + 0.42, f, y), b.p(RU + 0.48, f, y), 0.3, "gunmetal", "gunmetal", 18, both=False)
        tag(b, "LIFTING TRUNNION", b.p(RU, f, y - 0.42), b.dirh(f), d=0.016)
    # the data plate and the generator's tag on the lower shell, facing the operating floor's door
    f = b.f(90)
    n = b.dirh(f)
    tag(b, "STEAM GENERATOR SG-%d" % b.loop, b.p(RL, f, P_DECK + 1.55), n, d=0.02)
    for su in (-1, 1):       # the plate's two standoff brackets
        t = b.tang(f)
        q = b.p(RL, f, P_DECK + 1.08)
        q = tuple(q[i] + t[i] * su * 0.14 for i in range(3))
        RP.obox(b, b.still, tuple(q[i] + n[i] * 0.02 for i in range(3)), n, (0, 1, 0), 0.025, 0.04, 0.015, "stainless_dark")
    plate(b, "SG_PLATE", tuple(b.p(RL, f, P_DECK + 1.08)[i] + n[i] * 0.045 for i in range(3)), n)


def steam_nozzle(b):
    L = b.still
    y0 = b.head_hole_y - 0.08
    RP.cyl(b, L, b.L(0, y0, 0), b.L(0, TOP + 0.3, 0), 0.62, "gunmetal", "gunmetal", 22, both=False)
    RP.cyl(b, L, b.L(0, TOP + 0.3, 0), b.L(0, TOP + 0.36, 0), 0.55, "gunmetal", "gunmetal", 22, both=False)
    RP.cyl(b, L, b.L(0, TOP + 0.36, 0), b.L(0, TOP + 0.42, 0), MS_R + 0.03, "weld_band", "weld_band", 20, both=False)


def feedwater_nozzle(b):
    f = b.f(155)
    RP.cyl(b, b.still, b.p(R_UP - 0.1, f, FW_Y), b.p(RU + 0.26, f, FW_Y), 0.3, "gunmetal", "gunmetal", 18, both=False)
    RP.cyl(b, b.still, b.p(RU + 0.26, f, FW_Y), b.p(RU + 0.3, f, FW_Y), FW_R + 0.03, "weld_band", "weld_band", 16, both=False)
    tag(b, "FEEDWATER", b.p(RU, b.f(155 - 13), FW_Y + 0.42), b.dirh(b.f(155 - 13)), d=0.014)


# ---------------------------------------------------------------- small connections: blowdown, level taps

def valve(b, c, d, r=0.03, wheel=0.08):
    """A small globe valve on a line along d: its body, bonnet and handwheel (up)."""
    L = b.still
    d = _unit(d)
    RP.cyl(b, L, tuple(c[i] - d[i] * 0.08 for i in range(3)), tuple(c[i] + d[i] * 0.08 for i in range(3)), r * 2.0, "stainless_dark", "stainless_dark", 10, both=True)
    RP.cyl(b, L, c, (c[0], c[1] + 0.2, c[2]), r * 1.1, "stainless", "stainless", 8, both=False)
    RP.cyl(b, L, (c[0], c[1] + 0.2, c[2]), (c[0], c[1] + 0.215, c[2]), wheel, "gunmetal", "gunmetal", 14, both=True)


def sleeve(b, f, y, r=0.05, w_in=W_IN):
    """A wall sleeve on the compartment's inside face: a line's way through (a collar standing off the wall)."""
    RP.cyl(b, b.still, b.p(w_in - 0.09, f, y), b.p(w_in + 0.03, f, y), r + 0.02, "gunmetal", "gunmetal", 12, both=False)


def small_lines(b):
    L = b.still
    # blowdown: a nozzle just over the tubesheet each side, a valve, the line to its wall sleeve
    for deg, text in ((120, "BLOWDOWN A"), (-120, "BLOWDOWN B")):
        f = b.f(deg)
        y = TS[1] + 0.2
        RP.cyl(b, L, b.p(R_LOW - 0.02, f, y), b.p(RL + 0.18, f, y), 0.05, "gunmetal", "gunmetal", 10, both=False)
        end = 2.75 if b.stand else W_IN - 0.02
        RP.cyl(b, L, b.p(RL + 0.18, f, y), b.p(end, f, y), 0.032, "stainless", "stainless", 8, both=False)
        valve(b, b.p(2.45, f, y), b.dirh(f))
        if b.stand:
            RP.cyl(b, L, b.p(end, f, y), b.p(end + 0.05, f, y), 0.045, "stainless_dark", "stainless_dark", 8, both=False)
        else:
            sleeve(b, f, y)
        tag(b, text, b.p(RL, f, y + 0.3), b.dirh(f), d=0.014)
    # level taps: narrow range (steam space to the downcomer above the cone), wide range (to the tubesheet)
    taps = [(140, 29.4, "NR LEVEL TAP", True), (140, 25.95, None, False), (-140, 29.4, "WR LEVEL TAP", True),
            (-140, TS[1] + 0.42, "WR LEVEL TAP", False)]
    for deg, y, text, pot in taps:
        f = b.f(deg)
        r0 = R_UP if y > CONE_TOP else R_LOW
        rr = RU if y > CONE_TOP else RL
        RP.cyl(b, L, b.p(r0 - 0.02, f, y), b.p(rr + 0.12, f, y), 0.03, "gunmetal", "gunmetal", 8, both=False)
        end = rr + 0.7 if b.stand else W_IN - 0.02
        RP.cyl(b, L, b.p(rr + 0.12, f, y), b.p(end, f, y), 0.012, "stainless", "stainless", 6, both=False)
        valve(b, b.p(rr + 0.22, f, y), b.dirh(f), r=0.016, wheel=0.045)
        if b.stand:
            RP.cyl(b, L, b.p(end, f, y), b.p(end + 0.03, f, y), 0.022, "stainless_dark", "stainless_dark", 6, both=False)
        if pot:
            pc = b.p(rr + 0.5, f, y)
            RP.cyl(b, L, (pc[0], y - 0.04, pc[2]), (pc[0], y + 0.24, pc[2]), 0.055, "stainless", "stainless", 10, both=True)
            if not b.stand:
                tag(b, "CONDENSATE POT", b.p(W_IN, f, y + 0.12), b.dirh(f + math.pi), d=0.008)
        if not b.stand:
            sleeve(b, f, y, 0.035)
        if text:
            tag(b, text, b.p(rr, f, y + 0.26), b.dirh(f), d=0.014)
    if b.stand:
        return
    # the steam pressure gauge at the top platform on a post, off the narrow range upper tap's line
    f = b.f(140)
    q = b.p(3.2, f, 29.4)
    gy = P_TOP + 1.25
    post = b.p(3.34, f, 0)
    RP.cyl(b, L, (post[0], P_TOP, post[2]), (post[0], gy + 0.1, post[2]), 0.03, GREY, GREY, 8, both=False)
    RP.cyl(b, L, (post[0], P_TOP, post[2]), (post[0], P_TOP + 0.012, post[2]), 0.07, GREY, GREY, 10, both=False)
    n = b.dirh(f + math.pi)                               # the gauge faces in, toward the head
    gc = (q[0], gy, q[2])
    RP.cyl(b, L, q, (q[0], gy - 0.08, q[2]), 0.012, "stainless", "stainless", 6, both=False)
    RP.cyl(b, L, (q[0], gy - 0.1, q[2]), (q[0], gy - 0.075, q[2]), 0.02, "stainless_dark", "stainless_dark", 6, both=False)
    # the gauge's case (its back on the tube's end), the bracket back to the post
    RP.cyl(b, L, gc, tuple(gc[i] + n[i] * 0.04 for i in range(3)), 0.075, "black", "black", 18, both=True)
    t = b.dirh(f)
    arm_c = tuple((gc[i] + post[i]) / 2 for i in range(3))
    arm_c = (arm_c[0], gy, arm_c[2])
    RP.obox(b, L, arm_c, t, (0, 1, 0), math.dist((gc[0], gc[2]), (post[0], post[2])) / 2, 0.02, 0.01, "gunmetal")
    gf = tuple(gc[i] + n[i] * 0.041 for i in range(3))
    picture(b, "DIAL_STEAM", gf, n)
    b.live["dial"] = {"centre": [round(v, 4) for v in tuple(gf[i] + n[i] * 0.002 for i in range(3))],
                      "normal": [round(v, 4) for v in n], "r": 0.06, "low": 0, "high": 1200}
    # its tag on a plate under it, on a second arm to the post
    tc = (gc[0], gy - 0.2, gc[2])
    RP.obox(b, L, tuple(tc[i] - n[i] * 0.004 for i in range(3)), RM._right(n), (0, 1, 0), 0.135, 0.03, 0.004, "stainless")
    tag(b, "STM PRESS  PSIG", tc, n)
    arm2 = tuple((tc[i] - n[i] * 0.008 + post[i]) / 2 for i in range(3))
    arm2 = (arm2[0], gy - 0.2, arm2[2])
    RP.obox(b, L, arm2, t, (0, 1, 0), math.dist((tc[0], tc[2]), (post[0], post[2])) / 2 - 0.004, 0.015, 0.008, "gunmetal")


# ---------------------------------------------------------------- supports

COLUMNS = ((45, "A"), (135, "B"), (-135, "C"), (-45, "D"))
PAD_POLAR = math.radians(55)
R_COL = R_LOW * math.sin(PAD_POLAR)                    # the columns' circle
PAD_Y = TS[0] - R_LOW * math.cos(PAD_POLAR)             # where the pads meet the head
PIN_TOP = PAD_Y - 0.5
PIN_BOT = FLOOR + 0.5


def clevis(b, f, y_pin, down, tongue_len=0.35):
    """A clevis about a pin across the column's radial plane: the two cheek plates, the pin with its nuts, and the
    tongue of the column's end between the cheeks. down: +1 cheeks above the pin (hanging), -1 below (standing)."""
    L = b.still
    t = b.tang(f)
    c = b.p(R_COL, f, y_pin)
    for s in (-1, 1):
        cc = tuple(c[i] + t[i] * s * 0.12 for i in range(3))
        cc = (cc[0], y_pin + (0.13 if down > 0 else -0.18), cc[2])
        RP.obox(b, L, cc, b.dirh(f), (0, 1, 0), 0.17, 0.2 if down > 0 else 0.21, 0.025, "gunmetal")
    a_, b2 = tuple(c[i] - t[i] * 0.2 for i in range(3)), tuple(c[i] + t[i] * 0.2 for i in range(3))
    RP.cyl(b, L, a_, b2, 0.055, "stainless_dark", "stainless_dark", 12, both=True)
    for s in (-1, 1):
        nc = tuple(c[i] + t[i] * s * 0.17 for i in range(3))
        RP.cyl(b, L, nc, tuple(nc[i] + t[i] * s * 0.03 for i in range(3)), 0.08, "gunmetal", "gunmetal", 6, both=True)
    # the tongue (with its spherical bearing) between the cheeks
    ty0, ty1 = (y_pin - tongue_len + 0.1, y_pin + 0.1) if down > 0 else (y_pin - 0.1, y_pin + tongue_len - 0.1)
    RP.obox(b, L, (c[0], (ty0 + ty1) / 2, c[2]), b.dirh(f), (0, 1, 0), 0.13, (ty1 - ty0) / 2, 0.09, SUPPORT)
    for s in (-1, 1):
        bc = tuple(c[i] + t[i] * s * 0.09 for i in range(3))
        RP.cyl(b, L, bc, tuple(bc[i] + t[i] * s * 0.004 for i in range(3)), 0.085, "stainless", "stainless", 14, both=False)


def columns(b):
    L = b.still
    for deg, name in COLUMNS:
        f = b.f(deg)
        n = b.dirh(f)
        # the pad: a forged boss on the head, square to the column
        RP.obox(b, L, b.p(R_COL, f, PAD_Y + 0.015), n, (0, 1, 0), 0.16, 0.335, 0.22, "gunmetal")
        clevis(b, f, PIN_TOP, +1)
        # the column: a heavy pipe section with its end plates
        top = PIN_TOP - 0.25
        bot = PIN_BOT + 0.25
        c_top, c_bot = b.p(R_COL, f, top), b.p(R_COL, f, bot)
        RP.cyl(b, L, c_bot, c_top, 0.19, SUPPORT, SUPPORT, 16, both=False)
        for y in (top, bot):
            RP.cyl(b, L, b.p(R_COL, f, y - 0.025), b.p(R_COL, f, y + 0.025), 0.22, SUPPORT, SUPPORT, 16, both=True)
        clevis(b, f, PIN_BOT, -1)
        # the base pad: grout bed, base plate, its stiffeners, the four anchor bolts with levelling nuts
        g = b.p(R_COL, f, FLOOR)
        RP.obox(b, L, (g[0], FLOOR + 0.03, g[2]), n, (0, 1, 0), 0.56, 0.03, 0.56, WALL)
        RP.obox(b, L, (g[0], FLOOR + 0.1, g[2]), n, (0, 1, 0), 0.5, 0.04, 0.5, "gunmetal")
        t = b.tang(f)
        for s in (-1, 1):
            for e in (-1, 1):
                q = tuple(g[i] + n[i] * e * 0.4 + t[i] * s * 0.4 for i in range(3))
                RP.cyl(b, L, (q[0], FLOOR + 0.14, q[2]), (q[0], FLOOR + 0.26, q[2]), 0.025, "stainless_dark", "stainless_dark", 8, both=False)
                RP.cyl(b, L, (q[0], FLOOR + 0.14, q[2]), (q[0], FLOOR + 0.19, q[2]), 0.05, "gunmetal", "gunmetal", 6, both=False)
        for e in (-1, 1):
            RP.obox(b, L, tuple(g[i] + n[i] * e * 0.3 if i != 1 else FLOOR + 0.22 for i in range(3)), n, (0, 1, 0), 0.12, 0.08, 0.02, "gunmetal")
        tag(b, "SG SUPPORT COLUMN " + name, (g[0] + n[0] * 0.5, FLOOR + 0.1, g[2] + n[2] * 0.5), n, d=0.004)


def lower_lateral(b):
    """The lower lateral support: two built-up girders either side (along the hot leg) and two cross beams, their
    ends in the wall on embedded plates; bumpers with shim packs on lugs on the shell either side and behind."""
    L = b.still
    y0, y1 = LLS
    yc = (y0 + y1) / 2
    hgt = y1 - y0
    S = 2.15
    a_end = math.sqrt((W_IN + 0.05) ** 2 - (S - 0.18) ** 2)
    for s in (-S, S):
        RP.ibeam(b, L, b.L(-a_end, yc, s), b.L(a_end, yc, s), (0, 1, 0), hgt, 0.36, tf=0.035, tw=0.03, mat=SUPPORT)
        for e in (-1, 1):
            f = math.atan2(s, e * a_end)
            ep = b.p(W_IN - 0.015, f, yc)
            RP.obox(b, L, ep, b.tang(f), (0, 1, 0), 0.4, 0.42, 0.015, "gunmetal")
        # web stiffeners
        for a in (-1.2, -0.4, 0.4, 1.2):
            for e in (-1, 1):
                RP.obox(b, L, b.L(a, yc, s + e * 0.09), b.ein, (0, 1, 0), 0.012, hgt / 2 - 0.036, 0.074, SUPPORT)
    for a in (-S, S):
        RP.ibeam(b, L, b.L(a, yc, -S + 0.18), b.L(a, yc, S - 0.18), (0, 1, 0), hgt - 0.08, 0.3, tf=0.03, tw=0.025, mat=SUPPORT)
    # bumpers: either side (on the girders) and behind (on the back beam), each on a lug through the insulation
    for deg, beam_r, gap in ((90, S - 0.18, 0.004), (-90, S - 0.18, 0.004), (180, S - 0.15, 0.045)):
        f = b.f(deg)
        n = b.dirh(f)
        RP.obox(b, L, b.p((R_LOW - 0.05 + RL + 0.06) / 2, f, yc), n, (0, 1, 0), (RL + 0.06 - R_LOW + 0.05) / 2, 0.16, 0.14, "gunmetal")
        r0 = RL + 0.06 + gap
        RP.obox(b, L, b.p((r0 + 0.04 + beam_r) / 2, f, yc), n, (0, 1, 0), (beam_r - r0 - 0.04) / 2, 0.17, 0.15, "gunmetal")
        for k in range(3):
            rr = r0 + 0.004 + k * 0.012
            RP.obox(b, L, b.p(rr + 0.005, f, yc), n, (0, 1, 0), 0.005, 0.15 - k * 0.01, 0.13 - k * 0.01, "stainless_dark")
        for e in (-1, 1):
            q = b.p(beam_r - 0.04, f, yc + e * 0.1)
            RP.cyl(b, L, q, tuple(q[i] - n[i] * 0.03 for i in range(3)), 0.016, "gunmetal", "gunmetal", 6, both=False)
    tag(b, "LOWER LATERAL SUPPORT", b.L(0.0, yc, b.h * (S + 0.015)), b.D(0, 0, b.h))


def upper_lateral(b):
    """The upper lateral support: the ring band round the upper shell (four segments, bolted splices), four hydraulic
    snubbers front and back on clevis lugs to brackets in the wall, a seismic key each side against its bracket."""
    L = b.still
    y0, y1 = BAND
    yc = (y0 + y1) / 2
    R0, R1 = RU + 0.01, RU + 0.24
    b.rband(R1, y0, R1, y1, SUPPORT, n=64)
    b.rring(R0, R1, y1, SUPPORT, n=64)
    b.rring(R0, R1, y0, SUPPORT, down=True, n=64)
    for deg in (45, 135, -135, -45):
        f = b.f(deg)
        RP.obox(b, L, b.p(R1 + 0.012, f, yc), b.dirh(f), (0, 1, 0), 0.012, 0.22, 0.16, SUPPORT)
        for e in (-1, 1):
            for v in (-1, 1):
                q = tuple(b.p(R1 + 0.024, f, yc + v * 0.12)[i] + b.tang(f)[i] * e * 0.1 for i in range(3))
                RP.bolt(b, L, q, b.dirh(f), 0.016, 0.014, True)
    names = iter("ABCD")
    for a_sign in (1, -1):
        for s in (-0.45, 0.45):
            name = next(names)
            s_ = b.h * s
            a_lug = a_sign * math.sqrt(R1 ** 2 - s_ ** 2)
            a_wall = a_sign * math.sqrt(W_IN ** 2 - s_ ** 2)
            d = (a_sign, 0, 0)
            # lugs on the band, the bracket on its embedded plate in the wall
            b.lbox(a_lug - a_sign * 0.02, yc - 0.17, s_ - 0.06, a_lug + a_sign * 0.2, yc + 0.17, s_ - 0.03, SUPPORT)
            b.lbox(a_lug - a_sign * 0.02, yc - 0.17, s_ + 0.03, a_lug + a_sign * 0.2, yc + 0.17, s_ + 0.06, SUPPORT)
            if b.stand:
                continue
            b.lbox(a_wall - a_sign * 0.22, yc - 0.17, s_ - 0.06, a_wall - a_sign * 0.02, yc + 0.17, s_ - 0.03, "gunmetal")
            b.lbox(a_wall - a_sign * 0.22, yc - 0.17, s_ + 0.03, a_wall - a_sign * 0.02, yc + 0.17, s_ + 0.06, "gunmetal")
            b.lbox(a_wall - a_sign * 0.03, yc - 0.3, s_ - 0.2, a_wall + a_sign * 0.006, yc + 0.3, s_ + 0.2, "gunmetal")
            for v in (-1, 1):
                for e in (-1, 1):
                    q = b.L(a_wall - a_sign * 0.03, yc + v * 0.24, s_ + e * 0.15)
                    RP.bolt(b, L, q, b.D(-a_sign, 0, 0), 0.016, 0.014, True)
            if b.stand:
                continue
            # the snubber: rod end on the lug's pin, the cylinder, its reservoir, the far end on the bracket's pin
            p_lug = b.L(a_lug + a_sign * 0.12, yc, s_)
            p_wall = b.L(a_wall - a_sign * 0.14, yc, s_)
            for p in (p_lug, p_wall):
                RP.cyl(b, L, tuple(p[i] - b.es[i] * 0.075 for i in range(3)),
                       tuple(p[i] + b.es[i] * 0.075 for i in range(3)), 0.025, "stainless_dark", "stainless_dark", 8, both=True)
            u = _unit(tuple(p_wall[i] - p_lug[i] for i in range(3)))
            Lsn = math.dist(p_lug, p_wall)
            Q = lambda t: tuple(p_lug[i] + u[i] * t for i in range(3))
            RP.cyl(b, L, Q(0.0), Q(0.06), 0.035, "gunmetal", "gunmetal", 10, both=True)           # rod eye
            RP.cyl(b, L, Q(0.06), Q(0.2), 0.022, "stainless", "stainless", 10, both=False)        # the rod
            RP.cyl(b, L, Q(0.2), Q(Lsn - 0.08), 0.095, sw("yellow"), sw("yellow"), 16, both=True)  # the cylinder
            RP.cyl(b, L, Q(Lsn - 0.08), Q(Lsn), 0.04, "gunmetal", "gunmetal", 10, both=True)       # the end eye
            r0 = tuple(Q(0.26)[i] + (0, 1, 0)[i] * 0.15 for i in range(3))
            r1 = tuple(Q(Lsn - 0.14)[i] + (0, 1, 0)[i] * 0.15 for i in range(3))
            RP.cyl(b, L, r0, r1, 0.045, sw("yellow"), sw("yellow"), 10, both=True)                # the reservoir
            for t_ in (0.32, Lsn - 0.2):
                q = Q(t_)
                RP.cyl(b, L, (q[0], q[1] + 0.09, q[2]), (q[0], q[1] + 0.11, q[2]), 0.014, "stainless_dark", "stainless_dark", 6, both=False)
            tag(b, "SNUBBER " + name, b.L(a_wall - a_sign * 0.03, yc + 0.4, s_), b.D(-a_sign, 0, 0))
    # the seismic keys either side and their brackets (a gap for the slide along the hot leg)
    for deg in (90, -90):
        f = b.f(deg)
        n = b.dirh(f)
        b.rbox(f, R1, W_IN - 0.43, yc - 0.15, yc + 0.15, 0.11, "gunmetal")
        if b.stand:
            continue
        RP.obox(b, L, b.p(W_IN - 0.21, f, yc), n, (0, 1, 0), 0.2, 0.3, 0.3, "gunmetal")
        RP.obox(b, L, b.p(W_IN - 0.416, f, yc), n, (0, 1, 0), 0.006, 0.16, 0.15, "stainless_dark")    # the bumper's shims
        tag(b, "SEISMIC KEY", b.p(W_IN - 0.006, f, yc + 0.4), tuple(-v for v in n))
    tag(b, "UPPER LATERAL SUPPORT", b.p(R1, b.f(90 - 22), yc), b.dirh(b.f(90 - 22)), d=0.012)


# ---------------------------------------------------------------- the enclosure: wall, platforms, ladders, doors

DOOR_HALF = 0.147                 # a door's half opening (radians at the wall's inside)


def crossover_route(b, beyond=1.2):
    """The crossover leg's centre line (local points, then world): down off the outlet nozzle, under the generator to
    the pump's side, then toward the pump, through the wall; and its opening's azimuth."""
    sa, ca = math.sin(NOZ_POLAR), math.cos(NOZ_POLAR)
    S = (-(R_LOW + 0.5) * sa, TS[0] - (R_LOW + 0.5) * ca)
    t = (1.95 + S[0]) / sa
    v0 = (-1.95, S[1] - ca * t)
    px, pz = pump_xz(b.loop)
    q = (px - b.ox, pz - b.oz)
    pa = q[0] * b.ein[0] + q[1] * b.ein[2]
    ps = q[0] * b.es[0] + q[1] * b.es[2]
    sp = 1 if ps > 0 else -1
    v2 = (-1.95, sp * 2.3)
    d = (pa - v2[0], ps - v2[1])
    ln = math.hypot(*d)
    d = (d[0] / ln, d[1] / ln)
    # where it leaves: W_OUT + beyond from the axis
    R = W_OUT + beyond
    bq = 2 * (v2[0] * d[0] + v2[1] * d[1])
    cq = v2[0] ** 2 + v2[1] ** 2 - R ** 2
    tt = (-bq + math.sqrt(bq * bq - 4 * cq)) / 2
    end = (v2[0] + d[0] * tt, v2[1] + d[1] * tt)
    Rm = (W_IN + W_OUT) / 2
    cq = v2[0] ** 2 + v2[1] ** 2 - Rm ** 2
    tm = (-bq + math.sqrt(bq * bq - 4 * cq)) / 2
    mid = (v2[0] + d[0] * tm, v2[1] + d[1] * tm)
    pts = [(S[0], S[1], 0.0), (v0[0], v0[1], 0.0), (-1.95, X_Y, 0.0), (v2[0], X_Y, v2[1]), (end[0], X_Y, end[1])]
    return [b.L(*p) for p in pts], math.atan2(mid[1], mid[0])


def hot_leg_route(b):
    sa, ca = math.sin(NOZ_POLAR), math.cos(NOZ_POLAR)
    S = ((R_LOW + 0.5) * sa, TS[0] - (R_LOW + 0.5) * ca)
    t = (S[1] - RH.NOZZLE_Y) / ca
    h0 = (S[0] + sa * t, RH.NOZZLE_Y)
    far = math.hypot(b.ox - C, b.oz - C) - 4.1
    return [b.L(S[0], S[1], 0), b.L(h0[0], h0[1], 0), b.L(far, RH.NOZZLE_Y, 0)]


def wall_gaps(b):
    _, fx = crossover_route(b)
    xw = 0.62 / W_IN + 0.04
    g = [(-0.23, 0.23, RH.NOZZLE_Y - 0.8, RH.NOZZLE_Y + 0.8),                        # the hot leg
         (fx - xw, fx + xw, X_Y - 0.75, X_Y + 0.75),                                    # the crossover leg
         (b.f(90) - DOOR_HALF, b.f(90) + DOOR_HALF, FLOOR, FLOOR + 2.2),                  # the loop floor's door
         (b.f(90) - DOOR_HALF, b.f(90) + DOOR_HALF, OP, OP + 2.2),                        # the operating floor's door
         (b.f(155) - 0.12, b.f(155) + 0.12, FW_Y - 0.4, FW_Y + 0.4)]                       # the feedwater line
    return g


def _h_range(b, d0, d1):
    a, c = b.f(d0), b.f(d1)
    return (min(a, c), max(a, c))


def enclosure(b):
    """The compartment wall with its openings, the stencils on it."""
    ring_wall(b, W_IN, W_OUT, FLOOR, W_TOP, wall_gaps(b))
    # stencils: over the operating floor's door and toward the cavity
    for f, y in ((b.f(90), OP + 3.0), (0.0, 26.9)):
        b.curved_stencil("SG-%d" % b.loop, W_OUT, f, y)


def grating_ring(b, r_in, r_out, y, ranges, toe=True):
    L = b.still
    t = 0.04
    for f0, f1 in ranges:
        b.rring(r_in, r_out, y, "grating", f0, f1)
        b.rring(r_in, r_out, y - t, "grating", f0, f1, down=True)
        b.rband(r_in, y - t, r_in, y, "gunmetal", f0, f1, inward=True)
        b.jamb(f0, r_in, r_out, y - t, y, -1, "gunmetal")
        b.jamb(f1, r_in, r_out, y - t, y, +1, "gunmetal")
        if toe:
            b.rband(r_in + 0.006, y, r_in + 0.006, y + 0.1, YELLOW, f0, f1, inward=True)
            b.rband(r_in + 0.012, y, r_in + 0.012, y + 0.1, YELLOW, f0, f1)
            b.rring(r_in + 0.006, r_in + 0.012, y + 0.1, YELLOW, f0, f1)
        # its brackets: a beam out of the wall under it every 30 degrees or so, with a knee brace
        n = max(1, int(round((f1 - f0) / math.radians(30))))
        for k in range(n + 1):
            f = f0 + (f1 - f0) * (k + 0.5) / (n + 1)
            p0, p1 = b.p(W_IN + 0.02, f, y - t - 0.07), b.p(r_in + 0.04, f, y - t - 0.07)
            RP.ibeam(b, L, p0, p1, (0, 1, 0), 0.14, 0.08, tf=0.012, tw=0.01, mat="gunmetal")
            kb0 = b.p(W_IN, f, y - t - 0.75)
            kb1 = b.p((W_IN + r_in) / 2, f, y - t - 0.14)
            RP.cyl(b, L, kb0, kb1, 0.03, "gunmetal", "gunmetal", 8, both=False)
            RP.obox(b, L, b.p(W_IN - 0.01, f, y - t - 0.4), b.dirh(f), (0, 1, 0), 0.01, 0.45, 0.1, "gunmetal")


def _arc(b, r, f0, f1, step=math.radians(8)):
    n = max(1, int(abs(f1 - f0) / step))
    return [b.world_xz(r * math.cos(f0 + (f1 - f0) * k / n), r * math.sin(f0 + (f1 - f0) * k / n)) for k in range(n + 1)]


def arc_rail(b, r, f0, f1, y, closed=False):
    """A handrail round an arc of radius r (f0..f1): posts on base plates about every 1.2 m, top and knee rails in
    short straight lengths following the curve."""
    L = b.still
    span = abs(f1 - f0) * r
    n = max(1, math.ceil(span / 1.2))
    posts = range(n) if closed else range(n + 1)
    for k in posts:
        f = f0 + (f1 - f0) * k / n
        x, _, z = b.p(r, f, 0)
        RP.cyl(b, L, (x, y + 0.012, z), (x, y + 1.07, z), 0.021, YELLOW, YELLOW, 8, both=False)
        RP.cyl(b, L, (x, y, z), (x, y + 0.012, z), 0.045, YELLOW, YELLOW, 8, both=False)
    m = max(2, int(abs(f1 - f0) / math.radians(8)))
    for k in range(m):
        fa, fb = f0 + (f1 - f0) * k / m, f0 + (f1 - f0) * (k + 1) / m
        for hy, rr in ((1.07, 0.024), (0.53, 0.019)):
            pa, pb = b.p(r, fa, y + hy), b.p(r, fb, y + hy)
            d = _unit(tuple(pb[i] - pa[i] for i in range(3)))
            RP.cyl(b, L, tuple(pa[i] - d[i] * 0.004 for i in range(3)), tuple(pb[i] + d[i] * 0.004 for i in range(3)), rr, YELLOW, YELLOW, 8, both=False)


def platforms(b):
    L = b.still
    lad = math.radians(9)
    # the manway platform under the channel head, on the access side
    m0, m1 = _h_range(b, 62, 118)
    grating_ring(b, 1.6, W_IN, P_MANWAY, [(m0, m1)], toe=True)
    end_open = b.f(118)
    other = b.f(62)
    arc_rail(b, 1.66, other, end_open, P_MANWAY)
    RP.handrail(b, L, [b.world_xz(1.66 * math.cos(other), 1.66 * math.sin(other)),
                       b.world_xz(3.34 * math.cos(other), 3.34 * math.sin(other))], P_MANWAY)
    RP.handrail(b, L, [b.world_xz(1.66 * math.cos(end_open), 1.66 * math.sin(end_open)),
                       b.world_xz(2.55 * math.cos(end_open), 2.55 * math.sin(end_open))], P_MANWAY)
    # the ring at the operating floor, the platform at the upper lateral support, the top platform
    grating_ring(b, RL + 0.15, W_IN, P_DECK, [(-math.pi, math.pi)])
    g0, g1 = b.f(130) - lad, b.f(130) + lad
    grating_ring(b, 2.45, W_IN, P_UPPER, [(g1, g0 + 2 * math.pi)])
    t0, t1 = b.f(50) - lad, b.f(50) + lad
    grating_ring(b, 2.45, W_IN - 0.01, P_TOP, [(t1, t0 + 2 * math.pi)])
    arc_rail(b, 3.9, 0.0, 2 * math.pi, P_TOP, closed=True)


def ladders(b):
    L = b.still
    for deg, y0, y1, r in ((125, FLOOR, P_MANWAY, 3.1), (130, P_DECK, P_UPPER, 3.12), (50, P_UPPER, P_TOP, 3.12)):
        f = b.f(deg)
        x, _, z = b.p(r, f, 0)
        n = max(1, int((y1 - y0) / 1.5))
        RP.ladder(b, L, x, z, y0, y1, along=b.tang(f), standoffs=[y0 + (y1 - y0) * (k + 0.5) / n for k in range(n)],
                  standoff_to=W_IN - r + 0.006, wall_dir=b.dirh(f))


def door(b, y0):
    """A locked steel door in the wall's opening at the hand's 90 degrees: the leaf, its frame, three hinges, the
    handle and the padlocked hasp; the HIGH RADIATION AREA sign and the enclosure's tag beside it."""
    L = b.still
    f = b.f(90)
    n = b.dirh(f)
    t = b.tang(f)
    rm = W_IN + 0.38
    half = rm * math.tan(DOOR_HALF)
    c = b.p(rm, f, y0 + 1.1)
    RP.obox(b, L, c, t, (0, 1, 0), half, 1.1, 0.03, "paint_grey")
    face = tuple(c[i] + n[i] * 0.03 for i in range(3))
    # the frame on the wall's outside round the opening
    for s in (-1, 1):
        q = b.p(W_OUT, f + s * (DOOR_HALF + 0.012), y0 + 1.15)
        q = tuple(q[i] + b.dirh(f + s * DOOR_HALF)[i] * 0.03 for i in range(3))
        RP.obox(b, L, q, b.dirh(f + s * DOOR_HALF), (0, 1, 0), 0.03, 1.15, 0.05, "gunmetal")
    RP.obox(b, L, b.p(W_OUT + 0.03, f, y0 + 2.26), n, (0, 1, 0), 0.03, 0.05, W_OUT * math.sin(DOOR_HALF) + 0.1, "gunmetal")
    for k in (0.3, 1.1, 1.9):
        hc = tuple(face[i] + t[i] * (-half + 0.05) for i in range(3))
        RP.cyl(b, L, (hc[0], y0 + k - 0.08, hc[2]), (hc[0], y0 + k + 0.08, hc[2]), 0.02, "gunmetal", "gunmetal", 8, both=True)
    hd = tuple(face[i] + t[i] * (half - 0.12) for i in range(3))
    RP.cyl(b, L, (hd[0], y0 + 1.0, hd[2]), tuple((hd[0], y0 + 1.0, hd[2])[i] + n[i] * 0.05 for i in range(3)), 0.012, "stainless", "stainless", 6, both=False)
    hb = tuple((hd[0], y0 + 1.0, hd[2])[i] + n[i] * 0.05 for i in range(3))
    RP.cyl(b, L, (hb[0], hb[1] - 0.12, hb[2]), (hb[0], hb[1] + 0.02, hb[2]), 0.016, "stainless", "stainless", 6, both=True)
    hp = tuple((hd[0], y0 + 1.25, hd[2])[i] + n[i] * 0.02 for i in range(3))
    RP.obox(b, L, hp, t, (0, 1, 0), 0.06, 0.02, 0.01, "stainless_dark")
    pl = tuple(hp[i] + n[i] * 0.03 for i in range(3))
    RP.obox(b, L, (pl[0], pl[1] - 0.05, pl[2]), t, (0, 1, 0), 0.025, 0.03, 0.012, sw("red"))
    RP.cyl(b, L, (pl[0], pl[1] - 0.02, pl[2]), (pl[0], pl[1] + 0.02, pl[2]), 0.012, "stainless", "stainless", 6, both=False)
    # the sign and the tag beside the door (flat plates standing off the round wall)
    fs = f - (DOOR_HALF + 0.16)
    plate(b, "SIGN_HRA", tuple(b.p(W_OUT, fs, y0 + 1.5)[i] + b.dirh(fs)[i] * 0.01 for i in range(3)), b.dirh(fs))
    ft = f + (DOOR_HALF + 0.12)
    tag(b, "SG-%d ENCLOSURE" % b.loop, b.p(W_OUT, ft, y0 + 1.6), b.dirh(ft), d=0.012)


def doors(b):
    door(b, FLOOR)
    door(b, OP)


def _strut(b, pipe_c, f_wall, r_pipe, y):
    """A pipe's guide: a clamp round it and a strut back to a plate on the wall's outside."""
    L = b.still
    RP.cyl(b, L, (pipe_c[0], y - 0.08, pipe_c[2]), (pipe_c[0], y + 0.08, pipe_c[2]), r_pipe + 0.03, "gunmetal", "gunmetal", 16, both=True)
    w = b.p(W_OUT, f_wall, y)
    n = b.dirh(f_wall)
    d = _unit((pipe_c[0] - w[0], 0, pipe_c[2] - w[2]))
    ln = math.hypot(pipe_c[0] - w[0], pipe_c[2] - w[2]) - r_pipe - 0.03
    RP.obox(b, L, tuple(w[i] + d[i] * ln / 2 if i != 1 else y for i in range(3)), d, (0, 1, 0), ln / 2 + 0.01, 0.05, 0.04, "gunmetal")
    RP.obox(b, L, tuple(w[i] + n[i] * 0.015 if i != 1 else y for i in range(3)), n, (0, 1, 0), 0.015, 0.2, 0.2, "gunmetal")


def deck_collar(b, x, z, r):
    L = b.still
    RP.cyl(b, L, (x, OP, z), (x, OP + 0.012, z), r + 0.2, "gunmetal", "gunmetal", 18, both=False)
    RP.cyl(b, L, (x, OP + 0.012, z), (x, OP + 0.15, z), r + 0.08, "gunmetal", "gunmetal", 18, both=False)


def steam_feed_lines(b):
    """The main steam line off the top, over the wall and down outside it through the floor; the feedwater line out
    through the wall, its check valve, down through the floor."""
    L = b.still
    yms = 33.3
    m0 = b.L(0, TOP + 0.42, 0)
    m1 = b.L(0, yms, 0)
    m2 = b.p(MS_OUT, math.pi, yms)
    m3 = b.p(MS_OUT, math.pi, OP - 0.8)
    pipe(b, [m0, m1, m2, m3], MS_INS, "stainless", 1.1, sides=20)
    # its stanchion on the top platform and the guides down the wall
    st = b.p(3.7, math.pi, 0)
    RP.ibeam(b, L, (st[0], P_TOP, st[2]), (st[0], yms - MS_INS - 0.06, st[2]), b.dirh(math.pi), 0.2, 0.2, mat="gunmetal")
    RP.obox(b, L, (st[0], yms - MS_INS - 0.04, st[2]), b.dirh(math.pi), (0, 1, 0), 0.25, 0.02, 0.2, "gunmetal")
    for e in (-1, 1):
        q = tuple(st[i] + b.tang(math.pi)[i] * e * (MS_INS + 0.02) for i in range(3))
        RP.cyl(b, L, (q[0], yms - MS_INS - 0.02, q[2]), (q[0], yms, q[2]), 0.02, "stainless_dark", "stainless_dark", 6, both=False)
    U = []
    for k in range(9):
        an = math.pi * k / 8
        tq = b.tang(math.pi)
        U.append(tuple(st[i] + tq[i] * (MS_INS + 0.02) * math.cos(an) if i != 1 else yms + (MS_INS + 0.02) * math.sin(an) for i in range(3)))
    for p_, q_ in zip(U, U[1:]):
        RP.cyl(b, L, p_, q_, 0.018, "stainless_dark", "stainless_dark", 6, both=False)
    for y in (27.2, 23.6):
        _strut(b, b.p(MS_OUT, math.pi, y), math.pi, MS_INS, y)
    x, _, z = b.p(MS_OUT, math.pi, 0)
    deck_collar(b, x, z, MS_INS)
    tag(b, "MAIN STEAM", b.p(W_OUT, math.pi + 0.2, OP + 1.5), b.dirh(math.pi + 0.2), d=0.012)
    # feedwater
    f = b.f(155)
    f0 = b.p(RU + 0.3, f, FW_Y)
    f1 = b.p(MS_OUT, f, FW_Y)
    f2 = b.p(MS_OUT, f, OP - 0.8)
    pipe(b, [f0, f1, f2], FW_INS, "stainless", 0.6, sides=16)
    vc = b.p(4.45, f, FW_Y)
    n = b.dirh(f)
    RP.cyl(b, L, tuple(vc[i] - n[i] * 0.28 for i in range(3)), tuple(vc[i] + n[i] * 0.28 for i in range(3)), 0.34, "gunmetal", "gunmetal", 16, both=True)
    for e in (-1, 1):
        fl = tuple(vc[i] + n[i] * e * 0.3 for i in range(3))
        RP.cyl(b, L, tuple(fl[i] - n[i] * 0.03 for i in range(3)), tuple(fl[i] + n[i] * 0.03 for i in range(3)), 0.4, "gunmetal", "gunmetal", 18, both=True)
        RP.bolt_circle(b, L, tuple(fl[i] + n[i] * 0.03 * e for i in range(3)), tuple(v * e for v in n), 0.36, 12, r=0.015, h=0.02)
    RP.cyl(b, L, (vc[0], vc[1] + 0.3, vc[2]), (vc[0], vc[1] + 0.42, vc[2]), 0.24, "gunmetal", "gunmetal", 14, both=True)
    RP.bolt_circle(b, L, (vc[0], vc[1] + 0.42, vc[2]), (0, 1, 0), 0.2, 10, r=0.013, h=0.02)
    _strut(b, b.p(MS_OUT, f, 23.6), f, FW_INS, 23.6)
    x, _, z = b.p(MS_OUT, f, 0)
    deck_collar(b, x, z, FW_INS)


def deck_holes(b):
    """Where the operating floor is open for this generator: (x, z, r) of the wall and each line's collar."""
    out = [(b.ox, b.oz, W_OUT)]
    for f, r in ((math.pi, MS_INS), (b.f(155), FW_INS)):
        x, _, z = b.p(MS_OUT, f, 0)
        out.append((x, z, r + 0.2))
    return out


def piping_context(b):
    """The hot leg to the vessel and the crossover leg off to the pump (machine 10 builds them properly)."""
    h = Head()
    hot = hot_leg_route(b)
    pipe(h, hot, HOT_INS, "stainless", 1.2, sides=20)
    xo, _ = crossover_route(b)
    pipe(h, xo, X_INS, "stainless", 0.9, sides=20)
    return h.ops()


def build(loop=1, stand=False):
    """The generator in its enclosure; stand=True: the generator alone on its columns (for a stand picture): no wall
    and nothing that hangs on it, the small lines capped after their root valves, the big ones at their nozzles."""
    b = SG(loop)
    b.stand = stand
    shell(b)
    primary_nozzles(b)
    openings(b)
    steam_nozzle(b)
    feedwater_nozzle(b)
    small_lines(b)
    columns(b)
    if not stand:
        lower_lateral(b)
    upper_lateral(b)
    if not stand:
        enclosure(b)
        platforms(b)
        ladders(b)
        doors(b)
        steam_feed_lines(b)
    return b


def placed(b, pressure=895):
    """The generator's operations in a state: the gauge's needle at pressure (psig; 895 at full power, about 1005 at
    no load)."""
    import fuel_transfer as F
    o = b.ops()
    d = b.live["dial"]
    if d is None:
        return {"still": list(o["still"]), "lit": o["lit"]}
    needle = F.needle_ops(d["centre"], d["normal"], d["r"], d["low"], d["high"], pressure)
    return {"still": o["still"] + needle, "lit": o["lit"]}


def ops_check():
    bad = 0
    for loop in (1, 2, 3, 4):
        b = build(loop)
        bad += len(CB.overlaps(placed(b) | {"blink": []}))
    return bad


if __name__ == "__main__":
    print("overlaps", ops_check())
