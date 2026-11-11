"""The reactor coolant pumps (reactor building rebuild, machine 8).

Built like a Westinghouse four-loop plant's reactor coolant pumps (Westinghouse Technology Systems Manual 3.2, tables
3.2-8 and 3.2-9: vertical single-stage centrifugal pumps, 88,500 gpm at 277 ft and 1200 rpm, 28.5 ft tall, 188,900
lb; suction 31 in, discharge 27.5 in; 6000 hp 12.5 kV six-pole drip-proof air-cooled induction motors; supports as
in US 5,152,253's prior art: three columns and three lateral tie bars), one block a metre:

- Hydraulic section: the stainless casing (suction nozzle straight down from the crossover leg, discharge nozzle
  sideways into the cold leg toward the reactor), its support pads and tie-bar lugs; the main flange with its 24
  studs and nuts.
- Shaft seal section: the thermal barrier under the main flange (its component cooling water connections), the No 1,
  2 and 3 seal housings stacked on it with their bolting, seal injection coming in at the flange, the leakoff lines
  (No 2 to the seal standpipe with its level switches), the coupling spool between the pump and motor shafts (taken
  out for seal work without moving the motor), two vibration probes on the shaft.
- The motor stand (four legs with windows between) and the motor: lower bracket (the lower oil pot, its cooling
  coil's lines), the frame with its bars and louvred air inlets and outlets, the upper bracket, the upper bearing's oil
  pot (double Kingsbury thrust bearing; oil level sight glass, oil temperature dial, live), the flywheel cover (13,200
  lb flywheel and the anti-reverse ratchet inside; inspection covers) and lifting lugs; the 12.5 kV terminal box with
  its conduit; the upper oil cooler with its oil and CCW lines; the oil lift pump with its gauge (live); drip pans and
  drains of the oil collection system.
- Platforms: the seal platform at the main flange and the motor platform above it, on posts, with ladders and rails.
- Supports: three pin-ended columns on base pads, three tie bars (two to the primary shield wall, one to the steam
  generator's enclosure) with turnbuckles.

Frame: the pump stands on its loop's cold leg line (steam_generator.pump_xz); local a toward the reactor (the
discharge), s across; the service side (lines, ladder, terminal box) is away from the steam generator (HAND).
Live (LIVE): the upper bearing oil temperature (F), the oil lift pressure (psig), the shafts and coupling spinning.
"""
import math

import control_boards as CB
from control_boards import sw
import reactor_head as RH
import rb_parts as RP
import refuelling_machine as RM
import steam_generator as S
from reactor_head import C, legend_size, _cross, _unit
from steam_generator import tag, plate, picture, pipe, arc_rail

FLOOR = S.FLOOR
SUC_BOT = 8.25
CAS = [(0.55, 8.6), (0.95, 8.72), (1.18, 8.95), (1.25, 9.3), (1.25, 9.95), (1.16, 10.3), (1.0, 10.6)]
FL = (10.6, 10.95)                       # the main flange
SEALS = [(0.62, 10.95, 11.35), (0.55, 11.35, 11.6), (0.48, 11.6, 11.85)]
STAND = (10.95, 13.05)
LB = (13.05, 13.35)                      # the motor's lower bracket
FRAME = (13.35, 15.75)
UB = (15.75, 16.05)
UPOT = (16.05, 16.55)
FLY = (16.55, 16.85)
TOP = 17.0
R_FRAME = 1.3
DIS_Y = RH.NOZZLE_Y
P1 = 10.55                               # the seal platform's top
P2 = 13.2                                # the motor platform's top
COLD_R, COLD_INS = 0.42, 0.46
MOTOR = "paint_blue"
SUPPORT = S.SUPPORT
COLS = (60, 180, -60)
POSTS = (55, 130, -160, -55)


def _crossover_angle(b):
    sg = S.SG(b.loop)
    pts, _ = S.crossover_route(sg)
    u = _unit(tuple(pts[-1][i] - pts[-2][i] for i in range(3)))
    a = -u[0] * b.ein[0] - u[2] * b.ein[2]
    s = -u[0] * b.es[0] - u[2] * b.es[2]
    return math.atan2(s, a)


class Pump(S.SG):
    def __init__(self, loop=1):
        CB.Board.__init__(self, 9, 800 + loop)
        self.loop = loop
        self.ox, self.oz = S.pump_xz(loop)
        d = _unit((C - self.ox, 0.0, C - self.oz))
        self.ein = d
        self.es = (-d[2], 0.0, d[0])
        self.h = 1
        xa = _crossover_angle(self)
        self.h = -1 if xa > 0 else 1                  # the service side is away from the crossover and the generator
        self.xa = xa
        sg = S.SG(loop)
        q = (sg.ox - self.ox, sg.oz - self.oz)
        self.sg_f = math.atan2(q[0] * self.es[0] + q[1] * self.es[2], q[0] * self.ein[0] + q[1] * self.ein[2])
        self.sg_d = math.hypot(*q)
        self.stand = False
        self.live = {"dial": None, "lift": None, "spin": {"still": [], "axis": None}}


def lathe(b, prof, mat, n=40):
    for (r0, y0), (r1, y1) in zip(prof, prof[1:]):
        b.rband(r0, y0, r1, y1, mat, n=n)


# ---------------------------------------------------------------- the hydraulic section

def casing(b):
    L = b.still
    a = b.L(0, 0, 0)
    RP.cyl(b, L, (a[0], SUC_BOT, a[2]), (a[0], 8.62, a[2]), 0.56, "stainless", "stainless", 24, both=False)
    RP.cyl(b, L, (a[0], SUC_BOT - 0.05, a[2]), (a[0], SUC_BOT, a[2]), 0.5, "weld_band", "weld_band", 24, both=False)
    lathe(b, CAS, "stainless", 48)
    # cast ribs down the bowl
    for k in range(8):
        f = 2 * math.pi * (k + 0.5) / 8
        RP.obox(b, L, b.p(1.0, f, 8.85), b.dirh(f), (0, 1, 0), 0.2, 0.18, 0.025, "stainless")
    # the discharge nozzle toward the reactor, its safe end
    RP.cyl(b, L, b.L(0.6, DIS_Y, 0), b.L(1.75, DIS_Y, 0), 0.56, "stainless", "stainless", 24, both=False)
    RP.cyl(b, L, b.L(1.75, DIS_Y, 0), b.L(1.9, DIS_Y, 0), COLD_R + 0.03, "stainless", "stainless", 20, both=False)
    RP.cyl(b, L, b.L(1.9, DIS_Y, 0), b.L(1.95, DIS_Y, 0), COLD_R + 0.04, "weld_band", "stainless_dark", 20, both=False)
    # the main flange and its 24 studs
    RP.cyl(b, L, (a[0], FL[0], a[2]), (a[0], FL[1], a[2]), 1.38, "stainless_dark", "stainless_dark", 48, both=True)
    for k in range(24):
        f = 2 * math.pi * (k + 0.5) / 24
        c = b.p(1.2, f, FL[1])
        RP.cyl(b, L, c, (c[0], FL[1] + 0.2, c[2]), 0.045, "stainless", "stainless", 8, both=False)
        RP.cyl(b, L, c, (c[0], FL[1] + 0.12, c[2]), 0.085, "gunmetal", "gunmetal", 6, both=False)
    # support pads (under) and tie-bar lugs
    for deg in COLS:
        f = b.f(deg)
        RP.obox(b, L, b.p(1.38, f, 9.0), b.dirh(f), (0, 1, 0), 0.3, 0.15, 0.22, "stainless")
    for f in tie_angles(b):
        RP.obox(b, L, b.p(1.45, f, 9.75), b.dirh(f), (0, 1, 0), 0.26, 0.12, 0.045, "stainless")
        for e in (-1, 1):
            RP.obox(b, L, tuple(b.p(1.55, f, 9.75)[i] + b.tang(f)[i] * e * 0.07 for i in range(3)), b.dirh(f), (0, 1, 0), 0.16, 0.12, 0.012, "stainless")


def tie_angles(b):
    return [math.atan2(b.h * 0.86 * e, 1.23) for e in (1, -1)] + [b.sg_f]


def seal_section(b):
    L = b.still
    a = b.L(0, 0, 0)
    for r, y0, y1 in SEALS:
        RP.cyl(b, L, (a[0], y0, a[2]), (a[0], y1, a[2]), r, "stainless", "stainless", 32, both=False)
        RP.cyl(b, L, (a[0], y1 - 0.06, a[2]), (a[0], y1, a[2]), r + 0.05, "stainless_dark", "stainless_dark", 32, both=False)
        RP.bolt_circle(b, L, (a[0], y1, a[2]), (0, 1, 0), r - 0.04, 12, r=0.02, h=0.03)
    # thermal barrier CCW connections on the flange's side and the seal injection port
    for deg, y in ((95, 10.7), (105, 10.85), (70, 10.78)):
        f = b.f(deg)
        RP.cyl(b, L, b.p(1.3, f, y), b.p(1.48, f, y), 0.06, "stainless_dark", "stainless_dark", 10, both=False)
        RP.cyl(b, L, b.p(1.46, f, y), b.p(1.5, f, y), 0.09, "stainless_dark", "stainless_dark", 12, both=True)


def coupling(b):
    """The shafts and the coupling spool (the client spins them)."""
    L = b.live["spin"]["still"]
    a = b.L(0, 0, 0)
    X = lambda y: (a[0], y, a[2])
    RP.cyl(b, L, X(11.85), X(12.2), 0.12, "stainless", "stainless", 16, both=False)
    for y0 in (12.2, 12.52):
        RP.cyl(b, L, X(y0), X(y0 + 0.08), 0.32, "gunmetal", "gunmetal", 24, both=True)
        for k in range(8):
            f = 2 * math.pi * k / 8
            c = b.p(0.25, f, y0)
            RP.cyl(b, L, (c[0], y0 - 0.04, c[2]), (c[0], y0 + 0.12, c[2]), 0.022, "stainless_dark", "stainless_dark", 6, both=True)
    RP.cyl(b, L, X(12.28), X(12.52), 0.18, "stainless", "stainless", 16, both=False)
    RP.cyl(b, L, X(12.6), X(LB[0] + 0.02), 0.14, "stainless", "stainless", 16, both=False)
    # a painted match mark on the spool shows it turning: a raised key bar
    RP.obox(b, L, b.p(0.19, 0.0, 12.4), b.dirh(0.0), (0, 1, 0), 0.012, 0.1, 0.02, sw("yellow"))
    b.live["spin"]["axis"] = [round(a[0], 4), round(a[2], 4)]


def motor_stand(b):
    L = b.still
    b.rband(1.3, STAND[0], 1.3, STAND[0] + 0.15, "gunmetal", n=40)
    b.rring(0.8, 1.3, STAND[0] + 0.15, "gunmetal", n=40)
    for k in range(4):
        f = math.pi / 4 + k * math.pi / 2
        RP.obox(b, L, b.p(1.1, f, (STAND[0] + 0.15 + STAND[1] - 0.15) / 2), b.dirh(f), (0, 1, 0), 0.2, (STAND[1] - STAND[0] - 0.3) / 2, 0.15, "gunmetal")
        for e in (-1, 1):            # gussets on the legs' feet and heads
            for y, s_ in ((STAND[0] + 0.15, 1), (STAND[1] - 0.15, -1)):
                g = tuple(b.p(1.1, f, y + s_ * 0.12)[i] + b.tang(f)[i] * e * 0.165 for i in range(3))
                RP.obox(b, L, g, b.dirh(f), (0, 1, 0), 0.18, 0.12, 0.012, "gunmetal")
    b.rband(1.3, STAND[1] - 0.15, 1.3, STAND[1], "gunmetal", n=40)
    b.rring(0.8, 1.3, STAND[1] - 0.15, "gunmetal", down=True, n=40)
    b.rring(1.2, 1.3, STAND[1], "gunmetal", n=40)
    b.rband(0.8, STAND[1] - 0.15, 0.8, STAND[1], "gunmetal", inward=True, n=24)
    b.rband(0.8, STAND[0], 0.8, STAND[0] + 0.15, "gunmetal", inward=True, n=24)
    for k in range(16):
        f = 2 * math.pi * (k + 0.5) / 16
        RP.bolt(b, L, b.p(1.22, f, STAND[0] + 0.15), (0, 1, 0), 0.02, 0.02, True)


def vibration_probes(b):
    L = b.still
    for deg in (0, 90):
        f = b.f(deg)
        base = b.p(0.86, f, STAND[0] + 0.15)
        RP.cyl(b, L, base, (base[0], 12.05, base[2]), 0.025, "gunmetal", "gunmetal", 8, both=False)
        arm0 = (base[0], 12.05, base[2])
        tip = b.p(0.2, f, 12.05)
        RP.cyl(b, L, arm0, b.p(0.38, f, 12.05), 0.02, "gunmetal", "gunmetal", 8, both=False)
        RP.cyl(b, L, b.p(0.38, f, 12.05), tip, 0.016, "stainless", "black", 8, both=False)
        RP.cyl(b, L, b.p(0.42, f, 12.05), b.p(0.36, f, 12.05), 0.03, "stainless_dark", "stainless_dark", 6, both=True)
        # the probe's armoured cable along the arm and down the post to the junction box
        RP.cyl(b, L, b.p(0.36, f, 12.09), b.p(0.86, f, 12.09), 0.008, "stainless_dark", "stainless_dark", 6, both=False)
    jf = b.f(45)
    jb = b.p(1.38, jf, 12.3)
    RP.obox(b, L, jb, b.dirh(jf), (0, 1, 0), 0.08, 0.14, 0.12, "gunmetal")
    tag(b, "VIBRATION PROBES", b.p(1.3, b.f(45), 12.62), b.dirh(b.f(45)))
    RP.obox(b, L, b.p(1.3, b.f(45), 12.62), b.dirh(b.f(45)), (0, 1, 0), 0.002, 0.03, 0.19, "stainless")


def leakoffs(b):
    """No 1 and No 3 leakoff lines down to the floor, No 2 to the seal standpipe; seal injection up from the floor,
    the thermal barrier's CCW lines down to the floor."""
    L = b.still
    for (r, y0, y1), deg in zip(SEALS, (80, 120, 100)):
        f = b.f(deg)
        y = (y0 + y1) / 2 - 0.03
        RP.cyl(b, L, b.p(r - 0.02, f, y), b.p(r + 0.06, f, y), 0.03, "stainless_dark", "stainless_dark", 8, both=False)
    pts = lambda deg, y, r: [b.p(0.6, b.f(deg), y), b.p(r, b.f(deg), y), b.p(r, b.f(deg), FLOOR + 0.12)]
    for deg, y, text in ((80, 11.12, "NO 1 SEAL LEAKOFF"), (100, 11.69, "NO 3 SEAL LEAKOFF")):
        pipe(b, pts(deg, y, 1.44), 0.022, "stainless", 0.15, sides=8, jacket=False)
        floor_collar(b, b.p(1.44, b.f(deg), 0), 0.022)
    # the seal standpipe on leg 135 and the No 2 leakoff to it
    sf = b.f(135)
    sp = b.p(1.47, sf, 0)
    RP.cyl(b, L, (sp[0], 11.35, sp[2]), (sp[0], 12.75, sp[2]), 0.075, "stainless", "stainless", 12, both=True)
    RP.cyl(b, L, (sp[0], 12.75, sp[2]), (sp[0], 12.85, sp[2]), 0.03, "stainless_dark", "stainless_dark", 8, both=False)
    for y in (11.55, 12.5):
        RP.obox(b, L, b.p(1.47, sf, y), b.dirh(sf), (0, 1, 0), 0.1, 0.02, 0.02, "gunmetal")        # its clamps
        ls = tuple(b.p(1.47, sf, y + 0.12)[i] + b.tang(sf)[i] * 0.1 for i in range(3))
        RP.obox(b, L, ls, b.dirh(sf), (0, 1, 0), 0.04, 0.05, 0.03, sw("red"))                 # level switches
        RP.cyl(b, L, tuple(ls[i] - b.tang(sf)[i] * 0.03 for i in range(3)), tuple(ls[i] - b.tang(sf)[i] * 0.025 for i in range(3)), 0.012, "stainless_dark", "stainless_dark", 6, both=False)
    pipe(b, [b.p(0.6, b.f(120), 11.44), b.p(1.3, b.f(120), 11.44), b.p(1.3, b.f(120), 11.25),
             tuple(b.p(1.47, sf, 11.25)[i] for i in range(3)), b.p(1.47, sf, 11.35)], 0.02, "stainless", 0.08, sides=8, jacket=False)
    pipe(b, [b.p(1.47, sf, 12.85), b.p(1.47, sf, 12.95), b.p(1.62, b.f(140), 12.95), b.p(1.62, b.f(140), FLOOR + 0.12)],
         0.02, "stainless", 0.1, sides=8, jacket=False)
    floor_collar(b, b.p(1.62, b.f(140), 0), 0.02)
    plate_tag(b, "SEAL STANDPIPE", b.p(1.55, sf, 12.2), b.dirh(sf))
    plate_tag(b, "NO 1 SEAL LEAKOFF", b.p(1.47, b.f(80), 10.0), b.dirh(b.f(80)))
    # seal injection: up from the floor, a check valve, in to the flange
    f = b.f(70)
    pipe(b, [b.p(1.66, f, FLOOR + 0.12), b.p(1.66, f, 10.78), b.p(1.5, f, 10.78)], 0.03, "stainless", 0.1, sides=8, jacket=False)
    floor_collar(b, b.p(1.66, f, 0), 0.03)
    S.valve(b, b.p(1.66, f, 9.8), (0, 1, 0), r=0.03, wheel=0.0)
    plate_tag(b, "SEAL INJECTION", b.p(1.74, f, 10.25), b.dirh(f))
    # thermal barrier CCW, supply and return
    for deg, y, text in ((95, 10.7, "CCW SUPPLY"), (105, 10.85, "CCW RETURN")):
        f = b.f(deg)
        pipe(b, [b.p(1.5, f, y), b.p(1.8, f, y), b.p(1.8, f, FLOOR + 0.12)], 0.045, "stainless", 0.15, sides=10, jacket=False)
        floor_collar(b, b.p(1.8, f, 0), 0.045)
        S.valve(b, b.p(1.8, f, 8.4), (0, 1, 0), r=0.045, wheel=0.12)


def floor_collar(b, p, r):
    RP.cyl(b, b.still, (p[0], FLOOR, p[2]), (p[0], FLOOR + 0.12, p[2]), r + 0.04, "gunmetal", "gunmetal", 10, both=False)
    RP.cyl(b, b.still, (p[0], FLOOR, p[2]), (p[0], FLOOR + 0.015, p[2]), r + 0.09, "gunmetal", "gunmetal", 10, both=False)


def plate_tag(b, text, c, n):
    """An engraved tag on its own small stainless plate, wired to the line it names (the plate hangs off it)."""
    w, h = legend_size(text)
    n = _unit(n)
    u = RM._right(n)
    RP.obox(b, b.still, tuple(c[i] - n[i] * 0.003 for i in range(3)), u, (0, 1, 0), w / 2 + 0.015, h / 2 + 0.012, 0.003, "stainless")
    tag(b, text, c, n)


# ---------------------------------------------------------------- the motor

def louvre(b, f, y0, y1):
    """A louvred air opening in the frame's bay at azimuth f: a dark screen behind, slats in a frame."""
    L = b.still
    n = b.dirh(f)
    t = b.tang(f)
    w = 0.36
    c = b.p(R_FRAME + 0.01, f, (y0 + y1) / 2)
    RP.obox(b, L, c, t, (0, 1, 0), w, (y1 - y0) / 2, 0.01, sw("black"))
    fc = tuple(c[i] + n[i] * 0.04 for i in range(3))
    for e in (-1, 1):
        RP.obox(b, L, tuple(fc[i] + t[i] * e * (w + 0.015) for i in range(3)), t, (0, 1, 0), 0.015, (y1 - y0) / 2 + 0.03, 0.04, MOTOR)
        RP.obox(b, L, (fc[0], (y0 + y1) / 2 + e * ((y1 - y0) / 2 + 0.015), fc[2]), t, (0, 1, 0), w - 0.005, 0.015, 0.04, MOTOR)
    k = int((y1 - y0) / 0.09)
    sl = _unit((n[0] * 0.5, 1.0, n[2] * 0.5))
    for j in range(k):
        y = y0 + (y1 - y0) * (j + 0.5) / k
        RP.obox(b, L, (fc[0], y, fc[2]), t, sl, w - 0.005, 0.004, 0.035, MOTOR)


def motor(b):
    L = b.still
    a = b.L(0, 0, 0)
    X = lambda y: (a[0], y, a[2])
    RP.cyl(b, L, X(LB[0]), X(LB[1]), 1.2, MOTOR, MOTOR, 40, both=True)
    RP.cyl(b, L, X(FRAME[0]), X(FRAME[1]), R_FRAME, MOTOR, MOTOR, 48, both=False)
    for y in (FRAME[0], FRAME[1] - 0.07):
        RP.cyl(b, L, X(y), X(y + 0.07), R_FRAME + 0.09, MOTOR, MOTOR, 48, both=True)
    for k in range(8):
        f = 2 * math.pi * k / 8
        RP.obox(b, L, b.p(R_FRAME + 0.045, f, (FRAME[0] + FRAME[1]) / 2), b.dirh(f), (0, 1, 0), 0.045, (FRAME[1] - FRAME[0]) / 2 - 0.07, 0.05, MOTOR)
    box_bay = b.f(112.5)
    for k in range(8):
        f = 2 * math.pi * (k + 0.5) / 8
        louvre(b, f, 13.5, 14.12)
        if abs(S._norm(f - box_bay)) > 0.1:
            louvre(b, f, 15.0, 15.62)
    RP.cyl(b, L, X(UB[0]), X(UB[1]), 1.25, MOTOR, MOTOR, 40, both=True)
    RP.cyl(b, L, X(UPOT[0]), X(UPOT[1]), 0.95, MOTOR, MOTOR, 36, both=False)
    RP.cyl(b, L, X(FLY[0]), X(FLY[1]), 1.15, MOTOR, MOTOR, 44, both=True)
    b.rband(1.15, FLY[1], 0.4, TOP - 0.02, MOTOR, n=44)
    RP.cyl(b, L, X(TOP - 0.03), X(TOP + 0.02), 0.42, MOTOR, MOTOR, 24, both=False)
    RP.bolt_circle(b, L, X(TOP + 0.02), (0, 1, 0), 0.34, 12, r=0.02, h=0.025)
    # the flywheel cover's inspection covers, lifting lugs on top
    for deg in (22.5, -157.5):
        f = b.f(deg)
        c = b.p(1.15, f, (FLY[0] + FLY[1]) / 2)
        n = b.dirh(f)
        RP.obox(b, L, tuple(c[i] + n[i] * 0.015 for i in range(3)), b.tang(f), (0, 1, 0), 0.22, 0.11, 0.015, MOTOR)
        for e in (-1, 1):
            for v in (-1, 1):
                q = tuple(c[i] + n[i] * 0.03 + b.tang(f)[i] * e * 0.18 for i in range(3))
                RP.bolt(b, L, (q[0], q[1] + v * 0.07, q[2]), n, 0.012, 0.012, False)
        tag(b, "FLYWHEEL INSPECTION", tuple(c[i] + n[i] * 0.03 for i in range(3)), n)
    for k in range(3):
        f = 2 * math.pi * k / 3 + 0.3
        c = b.p(0.75, f, 0)
        yb = FLY[1] + (TOP - FLY[1]) * (1.15 - 0.75) / (1.15 - 0.4)
        RP.obox(b, L, (c[0], yb + 0.1, c[2]), b.tang(f), (0, 1, 0), 0.12, 0.13, 0.03, MOTOR)
        eye = (c[0], yb + 0.15, c[2])
        n = b.dirh(f)
        RP.cyl(b, L, tuple(eye[i] - n[i] * 0.04 for i in range(3)), tuple(eye[i] + n[i] * 0.04 for i in range(3)), 0.05, "black", "black", 10, both=True)


def motor_fittings(b):
    L = b.still
    # the terminal box and its conduit down to the floor
    f = b.f(112.5)
    n = b.dirh(f)
    t = b.tang(f)
    bc = b.p(R_FRAME + 0.3, f, 14.575)
    RP.obox(b, L, bc, t, (0, 1, 0), 0.36, 0.375, 0.21, "gunmetal")
    cover = tuple(bc[i] + n[i] * 0.22 for i in range(3))
    RP.obox(b, L, cover, t, (0, 1, 0), 0.33, 0.345, 0.012, "gunmetal")
    for e in (-1, 0, 1):
        q = tuple(cover[i] + t[i] * e * 0.29 + n[i] * 0.012 for i in range(3))
        for v in (-1, 1):
            RP.bolt(b, L, (q[0], q[1] + v * 0.3, q[2]), n, 0.014, 0.014, True)
        if e:
            RP.bolt(b, L, q, n, 0.014, 0.014, True)
    tag(b, "MOTOR TERMINAL BOX  12.5 KV", (cover[0] + n[0] * 0.012, cover[1] + 0.15, cover[2] + n[2] * 0.012), n)
    cd = b.p(R_FRAME + 0.3, f, 14.2)
    RP.cyl(b, L, cd, (cd[0], FLOOR + 0.1, cd[2]), 0.07, "stainless_dark", "stainless_dark", 10, both=False)
    for y in (14.15, 12.0, 9.5, 7.0):
        RP.cyl(b, L, (cd[0], y - 0.05, cd[2]), (cd[0], y + 0.05, cd[2]), 0.09, "gunmetal", "gunmetal", 10, both=True)
    floor_collar(b, b.p(R_FRAME + 0.3, f, 0), 0.07)
    # the stencil and the data plate on the frame's bays
    b.curved_stencil("RCP-%d" % b.loop, R_FRAME, b.f(22.5), 14.57)
    pf = b.f(67.5)
    pn = b.dirh(pf)
    for su in (-1, 1):
        q = tuple(b.p(R_FRAME, pf, 14.57)[i] + b.tang(pf)[i] * su * 0.14 for i in range(3))
        RP.obox(b, L, tuple(q[i] + pn[i] * 0.025 for i in range(3)), pn, (0, 1, 0), 0.03, 0.04, 0.015, "stainless_dark")
    plate(b, "RCP_PLATE", tuple(b.p(R_FRAME, pf, 14.57)[i] + pn[i] * 0.055 for i in range(3)), pn)
    # the upper oil cooler, its brackets, oil lines to the pot, CCW lines down, its drip pan and drain
    f = b.f(157.5)
    n = b.dirh(f)
    t = b.tang(f)
    cc = b.p(1.75, f, 15.35)
    RP.cyl(b, L, tuple(cc[i] - t[i] * 0.55 for i in range(3)), tuple(cc[i] + t[i] * 0.55 for i in range(3)), 0.2, "stainless", "stainless", 18, both=True)
    for e in (-1, 1):
        hc = tuple(cc[i] + t[i] * e * 0.55 for i in range(3))
        RP.cyl(b, L, hc, tuple(hc[i] + t[i] * e * 0.08 for i in range(3)), 0.235, "stainless_dark", "stainless_dark", 18, both=True)
        RP.bolt_circle(b, L, tuple(hc[i] + t[i] * e * 0.08 for i in range(3)), tuple(v * e for v in t), 0.2, 10, r=0.014, h=0.02)
        bq = tuple(cc[i] + t[i] * e * 0.38 for i in range(3))
        RP.obox(b, L, (bq[0] - n[0] * 0.23, 15.12, bq[2] - n[2] * 0.23), n, (0, 1, 0), 0.24, 0.035, 0.05, "gunmetal")
        RP.obox(b, L, (bq[0], 15.17, bq[2]), n, (0, 1, 0), 0.16, 0.025, 0.06, "gunmetal")
        top = tuple(hc[i] + t[i] * e * 0.04 for i in range(3))
        pipe(b, [(top[0], 15.55, top[2]), (top[0], 16.3, top[2]), tuple(b.p(0.95, b.f(157.5 + e * 12), 16.3)[i] for i in range(3))],
             0.035, "stainless_dark", 0.12, sides=8, jacket=False)
        RP.cyl(b, L, (top[0], 15.52, top[2]), (top[0], 15.58, top[2]), 0.05, "stainless_dark", "stainless_dark", 8, both=True)
        bot = tuple(cc[i] + t[i] * e * 0.48 for i in range(3))
        pipe(b, [(bot[0], 15.15, bot[2]), (bot[0], FLOOR + 0.12, bot[2])], 0.045, "stainless", 0.1, sides=10, jacket=False)
        S.valve(b, (bot[0], 13.95, bot[2]), (0, 1, 0), r=0.045, wheel=0.11)
        floor_collar(b, (bot[0], 0, bot[2]), 0.045)
    tag(b, "UPPER OIL COOLER", tuple(cc[i] + n[i] * 0.2 + (0, 0.21, 0)[i] for i in range(3)), n, d=0.01)
    pan = b.p(1.75, f, 14.98)
    RP.obox(b, L, pan, t, (0, 1, 0), 0.68, 0.01, 0.27, "stainless")
    for e in (-1, 1):
        RP.obox(b, L, tuple(pan[i] + n[i] * e * 0.265 + (0, 0.05, 0)[i] for i in range(3)), t, (0, 1, 0), 0.668, 0.05, 0.006, "stainless")
        RP.obox(b, L, tuple(pan[i] + t[i] * e * 0.675 + (0, 0.05, 0)[i] for i in range(3)), n, (0, 1, 0), 0.26, 0.05, 0.006, "stainless")
    dr = b.p(1.75, b.f(150), 0)
    pipe(b, [(dr[0], 14.97, dr[2]), (dr[0], FLOOR + 0.12, dr[2])], 0.025, "stainless_dark", 0.1, sides=8, jacket=False)
    floor_collar(b, (dr[0], 0, dr[2]), 0.025)
    # the oil lift pump on its tray, its gauge, its line up to the thrust bearing
    f = b.f(-157.5)
    n = b.dirh(f)
    t = b.tang(f)
    tc = b.p(1.67, f, 13.5)
    RP.obox(b, L, tc, n, (0, 1, 0), 0.28, 0.01, 0.4, "stainless")
    for e in (-1, 1):
        RP.obox(b, L, tuple(tc[i] + t[i] * e * 0.395 + (0, 0.04, 0)[i] for i in range(3)), n, (0, 1, 0), 0.27, 0.04, 0.006, "stainless")
        RP.obox(b, L, tuple(tc[i] + n[i] * e * 0.275 + (0, 0.04, 0)[i] for i in range(3)), t, (0, 1, 0), 0.388, 0.04, 0.006, "stainless")
        RP.obox(b, L, tuple(b.p(1.55, f, 13.42)[i] + t[i] * e * 0.3 for i in range(3)), n, (0, 1, 0), 0.17, 0.07, 0.02, "gunmetal")
    mc = tuple(tc[i] - t[i] * 0.12 for i in range(3))
    RP.motor(b, L, (mc[0], 13.62, mc[2]), t, 0.3, 0.09, colour=MOTOR, feet_to=13.51, fins=10)
    pb = tuple(tc[i] + t[i] * 0.22 for i in range(3))
    RP.obox(b, L, (pb[0], 13.62, pb[2]), t, (0, 1, 0), 0.08, 0.11, 0.08, "gunmetal")
    tag(b, "OIL LIFT PUMP", tuple(tc[i] + n[i] * 0.281 + (0, 0.02, 0)[i] for i in range(3)), n, d=0.004)
    gs = (pb[0], 13.73, pb[2])
    RP.cyl(b, L, gs, (gs[0], 13.9, gs[2]), 0.012, "stainless", "stainless", 6, both=False)
    gc = (gs[0], 13.97, gs[2])
    RP.cyl(b, L, gc, tuple(gc[i] + n[i] * 0.04 for i in range(3)), 0.075, "black", "black", 18, both=True)
    gf = tuple(gc[i] + n[i] * 0.041 for i in range(3))
    picture(b, "DIAL_LIFT", gf, n)
    b.live["lift"] = {"centre": [round(v, 4) for v in tuple(gf[i] + n[i] * 0.002 for i in range(3))],
                      "normal": [round(v, 4) for v in n], "r": 0.06, "low": 0, "high": 1000}
    plate_tag(b, "OIL LIFT PRESS  PSIG", (gc[0] + n[0] * 0.012, 13.8, gc[2] + n[2] * 0.012), n)
    up = tuple(pb[i] + t[i] * 0.05 for i in range(3))
    pipe(b, [(up[0], 13.73, up[2]), (up[0], 16.2, up[2]), tuple(b.p(0.95, b.f(-150), 16.2)[i] for i in range(3))],
         0.015, "stainless_dark", 0.06, sides=6, jacket=False)
    dr = b.p(1.67, b.f(-150), 0)
    pipe(b, [(dr[0], 13.49, dr[2]), (dr[0], FLOOR + 0.12, dr[2])], 0.02, "stainless_dark", 0.1, sides=8, jacket=False)
    floor_collar(b, (dr[0], 0, dr[2]), 0.02)
    plate_tag(b, "OIL DRAIN", (dr[0] + n[0] * 0.03, 12.3, dr[2] + n[2] * 0.03), n)
    # the upper pot's sight glass and oil temperature dial (live)
    f = b.f(67.5)
    n = b.dirh(f)
    for y in (16.12, 16.48):
        RP.cyl(b, L, b.p(0.94, f, y), b.p(1.06, f, y), 0.025, "stainless_dark", "stainless_dark", 8, both=True)
    gl = b.p(1.06, f, 0)
    RP.cyl(b, L, (gl[0], 16.1, gl[2]), (gl[0], 16.32, gl[2]), 0.018, sw("orange"), sw("orange"), 10, both=False)
    RP.cyl(b, L, (gl[0], 16.32, gl[2]), (gl[0], 16.5, gl[2]), 0.018, sw("black"), sw("black"), 10, both=False)
    for e in (-1, 1):
        q = tuple(gl[i] + b.tang(f)[i] * e * 0.03 + n[i] * 0.015 for i in range(3))
        RP.cyl(b, L, (q[0], 16.1, q[2]), (q[0], 16.5, q[2]), 0.005, "stainless", "stainless", 6, both=False)
    tag(b, "OIL LEVEL", b.p(1.25, f, 15.9), n, d=0.01)
    f = b.f(112.5)
    n = b.dirh(f)
    RP.cyl(b, L, b.p(0.94, f, 16.3), b.p(1.1, f, 16.3), 0.02, "stainless_dark", "stainless_dark", 8, both=False)
    dc = b.p(1.1, f, 16.3)
    RP.cyl(b, L, dc, tuple(dc[i] + n[i] * 0.04 for i in range(3)), 0.075, "black", "black", 18, both=True)
    df = tuple(dc[i] + n[i] * 0.041 for i in range(3))
    picture(b, "DIAL_OILT", df, n)
    b.live["dial"] = {"centre": [round(v, 4) for v in tuple(df[i] + n[i] * 0.002 for i in range(3))],
                      "normal": [round(v, 4) for v in n], "r": 0.06, "low": 0, "high": 250}
    tag(b, "UPPER BRG OIL  °F", b.p(1.25, f, 15.9), n, d=0.01)
    # the lower oil pot's cooling coil lines
    for deg in (-105, -120):
        f = b.f(deg)
        pipe(b, [b.p(1.15, f, 13.1), b.p(1.55, f, 13.1), b.p(1.55, f, FLOOR + 0.12)], 0.03, "stainless", 0.1, sides=8, jacket=False)
        floor_collar(b, b.p(1.55, f, 0), 0.03)


# ---------------------------------------------------------------- platforms and supports

def ring_grating(b, r0, r1, y, ranges):
    t = 0.04
    for f0, f1 in ranges:
        b.rring(r0, r1, y, "grating", f0, f1)
        b.rring(r0, r1, y - t, "grating", f0, f1, down=True)
        b.rband(r0, y - t, r0, y, "gunmetal", f0, f1, inward=True)
        b.rband(r1, y - t, r1, y, "gunmetal", f0, f1)
        b.rband(r0 + 0.006, y, r0 + 0.006, y + 0.1, sw("yellow"), f0, f1, inward=True)
        b.rband(r0 + 0.012, y, r0 + 0.012, y + 0.1, sw("yellow"), f0, f1)
        b.rring(r0 + 0.006, r0 + 0.012, y + 0.1, sw("yellow"), f0, f1)
        if f1 - f0 < 2 * math.pi - 1e-6:
            b.jamb(f0, r0, r1, y - t, y, -1, "gunmetal")
            b.jamb(f1, r0, r1, y - t, y, +1, "gunmetal")


def platforms(b):
    L = b.still
    R0, R1, RB = 1.5, 3.0, 2.9
    for y in (P1, P2):
        ys = y - 0.04 - 0.08
        n = 24
        for k in range(n):
            fa, fb = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
            yk = ys - 0.003 * (k % 2)
            RP.ibeam(b, L, b.p(RB, fa, yk), b.p(RB, fb, yk), (0, 1, 0), 0.16, 0.1, tf=0.012, tw=0.01, mat="gunmetal")
        for deg in POSTS:
            f = b.f(deg)
            RP.ibeam(b, L, b.p(RB - 0.06, f, ys - 0.0015), b.p(R0 + 0.05, f, ys - 0.0015), (0, 1, 0), 0.16, 0.1, tf=0.012, tw=0.01, mat="gunmetal")
    ring_grating(b, R0, R1, P1, [(-math.pi, math.pi)])
    g = b.f(160)
    gw = math.radians(13)
    ring_grating(b, R0, R1, P2, [(g + gw, g - gw + 2 * math.pi)])
    for deg in POSTS:
        f = b.f(deg)
        q = b.p(RB, f, 0)
        RP.cyl(b, L, (q[0], FLOOR, q[2]), (q[0], P2 - 0.2, q[2]), 0.06, "gunmetal", "gunmetal", 10, both=False)
        RP.obox(b, L, (q[0], FLOOR + 0.015, q[2]), b.dirh(f), (0, 1, 0), 0.16, 0.015, 0.16, "gunmetal")
        for e in (-1, 1):
            for v in (-1, 1):
                RP.bolt(b, L, tuple((q[0], FLOOR + 0.03, q[2])[i] + b.dirh(f)[i] * e * 0.11 + b.tang(f)[i] * v * 0.11 for i in range(3)), (0, 1, 0), 0.014, 0.014, True)
    # rails: the seal platform's open at its ladder, the motor platform's all round
    lg = b.f(95)
    lw = math.radians(11)
    a0, a1 = (lg + lw, lg - lw + 2 * math.pi)
    arc_rail(b, 2.97, a0, a1, P1)
    arc_rail(b, 2.97, 0.0, 2 * math.pi, P2, closed=True)
    # ladders: floor to the seal platform outside the ring, the seal platform to the motor platform through its opening
    x, _, z = b.p(3.25, lg, 0)
    RP.ladder(b, L, x, z, FLOOR, P1, along=b.tang(lg), standoffs=[P1 - 0.12], standoff_to=0.25, wall_dir=tuple(-v for v in b.dirh(lg)))
    x, _, z = b.p(2.62, g, 0)
    RP.ladder(b, L, x, z, P1, P2, along=b.tang(g))
    # the pump's tag on the seal platform's rail
    f = b.f(30)
    n = b.dirh(f)
    pc = b.p(3.0, f, P1 + 0.92)
    RP.obox(b, L, pc, RM._right(n), (0, 1, 0), 0.43, 0.05, 0.004, "stainless")
    tag(b, "REACTOR COOLANT PUMP RCP-%d" % b.loop, tuple(pc[i] + n[i] * 0.004 for i in range(3)), n)
    for e in (-1, 1):
        q = tuple(pc[i] + RM._right(n)[i] * e * 0.36 for i in range(3))
        RP.cyl(b, L, (q[0], P1 + 0.97, q[2]), (q[0], P1 + 1.07, q[2]), 0.006, "stainless_dark", "stainless_dark", 6, both=False)


def _clevis(b, r, f, y_pin, down):
    L = b.still
    t = b.tang(f)
    c = b.p(r, f, y_pin)
    for s in (-1, 1):
        cc = tuple(c[i] + t[i] * s * 0.1 for i in range(3))
        cc = (cc[0], y_pin + (0.1 if down > 0 else -0.14), cc[2])
        RP.obox(b, L, cc, b.dirh(f), (0, 1, 0), 0.13, 0.16 if down > 0 else 0.17, 0.02, "gunmetal")
    RP.cyl(b, L, tuple(c[i] - t[i] * 0.16 for i in range(3)), tuple(c[i] + t[i] * 0.16 for i in range(3)), 0.045, "stainless_dark", "stainless_dark", 12, both=True)
    for s in (-1, 1):
        nc = tuple(c[i] + t[i] * s * 0.14 for i in range(3))
        RP.cyl(b, L, nc, tuple(nc[i] + t[i] * s * 0.025 for i in range(3)), 0.065, "gunmetal", "gunmetal", 6, both=True)
    ty0, ty1 = (y_pin - 0.22, y_pin + 0.08) if down > 0 else (y_pin - 0.08, y_pin + 0.22)
    RP.obox(b, L, (c[0], (ty0 + ty1) / 2, c[2]), b.dirh(f), (0, 1, 0), 0.1, (ty1 - ty0) / 2, 0.075, SUPPORT)


def supports(b):
    L = b.still
    names = iter("ABC")
    for deg in COLS:
        f = b.f(deg)
        n = b.dirh(f)
        r = 1.38
        pin_top, pin_bot = 8.85 - 0.28, FLOOR + 0.45
        _clevis(b, r, f, pin_top, +1)
        RP.cyl(b, L, b.p(r, f, pin_bot + 0.22), b.p(r, f, pin_top - 0.22), 0.15, SUPPORT, SUPPORT, 14, both=False)
        for y in (pin_bot + 0.22, pin_top - 0.22):
            RP.cyl(b, L, b.p(r, f, y - 0.02), b.p(r, f, y + 0.02), 0.18, SUPPORT, SUPPORT, 14, both=True)
        _clevis(b, r, f, pin_bot, -1)
        g = b.p(r, f, FLOOR)
        RP.obox(b, L, (g[0], FLOOR + 0.025, g[2]), n, (0, 1, 0), 0.46, 0.025, 0.46, S.WALL)
        RP.obox(b, L, (g[0], FLOOR + 0.085, g[2]), n, (0, 1, 0), 0.4, 0.035, 0.4, "gunmetal")
        for e in (-1, 1):
            for v in (-1, 1):
                q = tuple(g[i] + n[i] * e * 0.32 + b.tang(f)[i] * v * 0.32 for i in range(3))
                RP.cyl(b, L, (q[0], FLOOR + 0.12, q[2]), (q[0], FLOOR + 0.22, q[2]), 0.02, "stainless_dark", "stainless_dark", 8, both=False)
                RP.cyl(b, L, (q[0], FLOOR + 0.12, q[2]), (q[0], FLOOR + 0.16, q[2]), 0.04, "gunmetal", "gunmetal", 6, both=False)
        tag(b, "RCP SUPPORT COLUMN " + next(names), (g[0] + n[0] * 0.4, FLOOR + 0.085, g[2] + n[2] * 0.4), n, d=0.004)


def tie_bars(b):
    """Three lateral tie bars from the casing's lugs: two parallel to the cold leg to the primary shield wall, one to the
    steam generator's enclosure; each with clevis ends, a turnbuckle, its embedded plate in the wall."""
    L = b.still
    sg = S.SG(b.loop)
    for k, f in enumerate(tie_angles(b)):
        p0 = b.p(1.66, f, 9.75)
        if k < 2:
            d = b.ein
            # to the primary shield's outside (r 7.1 about the vessel)
            dx, dz = p0[0] - C, p0[2] - C
            bq = 2 * (dx * d[0] + dz * d[2])
            cq = dx * dx + dz * dz - 7.12 ** 2
            tt = (-bq - math.sqrt(bq * bq - 4 * cq)) / 2
            wall_n = _unit((p0[0] + d[0] * tt - C, 0, p0[2] + d[2] * tt - C))
        else:
            d = b.dirh(f)
            dx, dz = p0[0] - sg.ox, p0[2] - sg.oz
            bq = 2 * (dx * d[0] + dz * d[2])
            cq = dx * dx + dz * dz - (S.W_OUT + 0.02) ** 2
            tt = (-bq - math.sqrt(bq * bq - 4 * cq)) / 2
            wall_n = _unit((p0[0] + d[0] * tt - sg.ox, 0, p0[2] + d[2] * tt - sg.oz))
        p1 = tuple(p0[i] + d[i] * tt for i in range(3))
        side = _unit(_cross(d, (0, 1, 0)))
        Q = lambda s_: tuple(p0[i] + d[i] * s_ for i in range(3))
        # pins at the lug and at the wall bracket
        RP.cyl(b, L, tuple(p0[i] - (0, 1, 0)[i] * 0.14 for i in range(3)), tuple(p0[i] + (0, 1, 0)[i] * 0.14 for i in range(3)), 0.035, "stainless_dark", "stainless_dark", 8, both=True)
        RP.cyl(b, L, Q(0.0), Q(0.16), 0.06, "gunmetal", "gunmetal", 10, both=True)
        RP.cyl(b, L, Q(0.16), Q(tt / 2 - 0.3), 0.065, SUPPORT, SUPPORT, 10, both=False)
        RP.cyl(b, L, Q(tt / 2 - 0.3), Q(tt / 2 + 0.3), 0.1, "gunmetal", "gunmetal", 8, both=True)     # turnbuckle
        RP.cyl(b, L, Q(tt / 2 + 0.3), Q(tt - 0.36), 0.065, SUPPORT, SUPPORT, 10, both=False)
        RP.cyl(b, L, Q(tt - 0.36), Q(tt - 0.2), 0.06, "gunmetal", "gunmetal", 10, both=True)
        pw = Q(tt - 0.13)
        RP.cyl(b, L, (pw[0], 9.6, pw[2]), (pw[0], 9.9, pw[2]), 0.035, "stainless_dark", "stainless_dark", 8, both=True)
        for e in (-1, 1):
            RP.obox(b, L, tuple(Q(tt - 0.12)[i] + (0, e * 0.09, 0)[i] for i in range(3)), d, side, 0.13, 0.015, 0.09, "gunmetal")
        wp = tuple(p1[i] + wall_n[i] * 0.025 for i in range(3))
        RP.obox(b, L, wp, wall_n, (0, 1, 0), 0.025, 0.3, 0.3, "gunmetal")
        for e in (-1, 1):
            for v in (-1, 1):
                q = tuple(wp[i] + wall_n[i] * 0.025 + _unit(_cross(wall_n, (0, 1, 0)))[i] * e * 0.22 for i in range(3))
                RP.bolt(b, L, (q[0], q[1] + v * 0.22, q[2]), wall_n, 0.018, 0.018, True)
        RP.obox(b, L, tuple(wp[i] + (0, 0.4, 0)[i] for i in range(3)), wall_n, (0, 1, 0), 0.025, 0.04, 0.16, "gunmetal")
        tag(b, "TIE BAR %d" % (k + 1), tuple(wp[i] + wall_n[i] * 0.025 + (0, 0.4, 0)[i] for i in range(3)), wall_n)


def build(loop=1, stand=False):
    b = Pump(loop)
    b.stand = stand
    casing(b)
    seal_section(b)
    coupling(b)
    motor_stand(b)
    vibration_probes(b)
    leakoffs(b)
    motor(b)
    motor_fittings(b)
    platforms(b)
    supports(b)
    if not stand:
        tie_bars(b)
    return b


def placed(b, oil_t=140, lift=0):
    """The pump's operations in a state: the upper bearing oil at oil_t F (about 140 running), the oil lift pressure
    (0 running; about 600 psig while it lifts the thrust shoes for a start)."""
    import fuel_transfer as F
    o = b.ops()
    still = list(o["still"]) + list(b.live["spin"]["still"])
    for key, value in (("dial", oil_t), ("lift", lift)):
        d = b.live[key]
        still += F.needle_ops(d["centre"], d["normal"], d["r"], d["low"], d["high"], value)
    return {"still": still, "lit": o["lit"]}


def piping_context(b):
    """The cold leg to the vessel and the crossover leg from the steam generator into the suction (machine 10 builds
    them properly)."""
    h = RH.Head()
    a = b.L(0, 0, 0)
    far = math.hypot(b.ox - C, b.oz - C) - 4.1
    pipe(h, [b.L(1.95, DIS_Y, 0), b.L(far, DIS_Y, 0)], COLD_INS, "stainless", 1.0, sides=20)
    sg = S.SG(b.loop)
    pts, _ = S.crossover_route(sg)
    pipe(h, pts[:-1] + [(a[0], S.X_Y, a[2]), (a[0], SUC_BOT - 0.05, a[2])], S.X_INS, "stainless", 0.9, sides=20)
    return h.ops()


def ops_check():
    bad = 0
    for loop in (1, 2, 3, 4):
        b = build(loop)
        bad += len(CB.overlaps(placed(b) | {"blink": []}))
    return bad


if __name__ == "__main__":
    print("overlaps", ops_check())
