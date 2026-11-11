"""The refuelling SRO's station and the refuelling floor's radiation protection fittings (reactor building rebuild,
machine 6).

As on a real PWR refuelling floor:

- The refuelling SRO's station on the deck where the cavity and the refuelling machine can be seen: a steel standing
  desk with the fuel move log open on it, the fuel handling procedure binders, the sound-powered phone headset on its
  jack box, a radio in its charger, the repeat monitor of the refuelling machine's mast camera, the MOVE NO and STEP
  readouts; the core map board beside it (193 positions, each lit live: assembly in place, empty, moving).
- The area radiation monitor RM-ARM-10: its detector on a post over the pool, the local readout and alarm panel (live
  mR/hr, ALERT and HIGH ALARM lamps, the alarm beacon and horn), CAUTION RADIATION AREA signs.
- The contamination control point at the gate through the railing into the cavity's contaminated area: the gate's
  posts with CAUTION CONTAMINATED AREA signs and the yellow-and-magenta chain, the PC, laundry and rad waste bins
  inside, the step-off pad, then the frisker on its stand (count rate LCD, live; pancake probe in its cradle; its
  instruction sign).
- The FME control: the FME ZONE sign on the railing and the FME log stand; a life ring on the railing; the page and
  party phone station; a fire extinguisher on its stand.

Frame: the reactor building's (tools/reactor_head.py), on the operating floor (OP) north of the refuelling machine's
north rail and its railing (z = C - 9.3). The railing's gate is at GATE_X (the building leaves an opening there).
Live (LIVE): the core map's 193 cells, MOVE NO and STEP, the ARM's reading (mR/hr) and alarm lamps and beacon, the
frisker's count rate (cpm) and alarm.
"""
import math

import control_boards as CB
from control_boards import sw, pic
import reactor_head as RH
import rb_parts as RP
import refuelling_machine as RM
from reactor_head import Head, C, legend, legend_size, _cross, _unit

OP = RM.OP
RAIL_LINE = RM.RAIL_Z[0] - 1.9         # the building's railing north of the refuelling machine's north rail (-z is north)
GATE_X = (C + 10.0, C + 11.0)       # east of steam generator 4's compartment
SRO = (C + 0.4, C - 10.9)              # the desk's centre; it faces south (+z), toward the cavity
MAP = (C - 1.2, C - 10.7)              # the core map board's centre, facing south-east toward the SRO's place
ARM = (C + 14.0, RAIL_LINE - 0.25)
FRISK = (C + 12.2, C - 10.8)
YELLOW = "paint_yellow"
BLUE = "paint_blue"
GREY = "paint_grey"


class Desk(Head):
    def __init__(self):
        CB.Board.__init__(self, 21, 606)
        self.live = {"cells": {}, "readouts": [], "lamps": {}, "beacon": None, "lcd": None}


def tag(b, text, c, n, up=(0, 1, 0)):
    b.tag(text, c, n, up=up)


def picture(b, name, centre, normal, lst=None, up=(0, 1, 0), lift=0.002):
    keep = b.still
    lst = b.still if lst is None else lst
    RM.picture(b, name, centre, normal, lst, up=up, lift=lift)


def sign_on_post(b, name, x, y, z, n):
    """A sign plate (aluminium, a bolt at each corner) facing n, its picture on its face."""
    w, h = legend_size(name)
    n = _unit(n)
    u = RM._right(n)
    RP.obox(b, b.still, (x, y, z), u, (0, 1, 0), w / 2 + 0.01, h / 2 + 0.01, 0.004, "stainless")
    picture(b, name, (x + n[0] * 0.004, y, z + n[2] * 0.004), n)
    for su in (-1, 1):
        for sv in (-1, 1):
            p = (x + u[0] * su * (w / 2 - 0.02) + n[0] * 0.006, y + sv * (h / 2 - 0.02), z + u[2] * su * (w / 2 - 0.02) + n[2] * 0.006)
            RP.cyl(b, b.still, p, (p[0] + n[0] * 0.004, p[1], p[2] + n[2] * 0.004), 0.008, "stainless_dark", "stainless_dark", 6, both=False)


# ---------------------------------------------------------------- the SRO's station

def sro_desk(b):
    L = b.still
    lit = b.lit
    x, z = SRO
    w, dpt = 1.6, 0.75
    xa, xb = x - w / 2, x + w / 2
    zf, zb = z - dpt / 2, z + dpt / 2          # the SRO stands at the south (zf) side, facing north
    yt = OP + 0.98
    # four legs (box tube) with feet, a shelf low, the modesty panel at the front, the sloped top
    for lx in (xa + 0.04, xb - 0.04):
        for lz in (zf + 0.04, zb - 0.04):
            b.box(lx - 0.025, OP + 0.012, lz - 0.025, lx + 0.025, yt - 0.07, lz + 0.025, GREY)
            RP.cyl(b, L, (lx, OP, lz), (lx, OP + 0.012, lz), 0.04, "rubber", "rubber", 8, both=False)
    b.box(xa + 0.07, OP + 0.2, zf + 0.07, xb - 0.07, OP + 0.22, zb - 0.07, GREY)
    b.box(xa + 0.065, OP + 0.3, zb - 0.06, xb - 0.065, yt - 0.06, zb - 0.045, GREY)
    # the top: a frame and a top sloped 10 deg up toward the cavity side, a lip at its low edge
    ya, yb = yt - 0.04, yt + 0.09
    b.quad((xa, yb, zb), (xa, ya, zf), (xb, ya, zf), (xb, yb, zb), sw("dgrey"))          # the linoleum top
    b.box(xa, yt - 0.07, zf, xb, ya, zb, GREY)
    b.quad((xb, yb, zb), (xb, ya, zb), (xa, ya, zb), (xa, yb, zb), GREY)
    for xs, s in ((xa, -1), (xb, 1)):
        if s < 0:
            b.quad((xs, yb, zb), (xs, ya, zb), (xs, ya, zf), (xs, ya, zf), GREY)
        else:
            b.quad((xs, ya, zf), (xs, ya, zb), (xs, yb, zb), (xs, yb, zb), GREY)
    b.box(xa, ya, zf - 0.03, xb, ya + 0.03, zf, GREY)                               # the pencil lip
    sl = _unit((0, yb - ya, zb - zf))
    n = _unit(_cross((1, 0, 0), sl))
    if n[1] < 0:
        n = tuple(-c for c in n)
    u = RM._right(n)
    T = lambda a, s_, h=0.0: (x + u[0] * a, ya + sl[1] * s_ + n[1] * h, zf + sl[2] * s_ + n[2] * h)
    slen = math.dist((ya, zf), (yb, zb))
    # the move log open on the desk, a pen
    c = T(-0.35, 0.3, 0.008)
    b.obox(c, u, sl, 0.19, 0.14, 0.008, ["black", None, "black", "black", "black", "black"], L)
    picture(b, "LOG_PAGES", T(-0.35, 0.3, 0.016), n, up=sl)
    RP.cyl(b, L, T(-0.1, 0.2, 0.02), T(-0.03, 0.32, 0.02), 0.006, "black", "black", 6, both=True)
    # MOVE NO and STEP readouts in a case at the back right, the repeat monitor at the back middle
    c = T(0.5, slen - 0.15, 0.05)
    b.obox(c, u, sl, 0.22, 0.09, 0.05, BLUE, L)
    for k, (lab, key, nch, a) in enumerate((("MOVE NO", "move", 3, 0.43), ("STEP", "step", 2, 0.6))):
        wc = T(a, slen - 0.13, 0.101)
        b.obox(wc, u, sl, 0.065, 0.03, 0.002, "black", L)
        b.live["readouts"].append({"name": key, "centre": [round(v, 4) for v in RP.add(wc, RP.mul(n, 0.003))], "normal": [round(v, 4) for v in n],
                                   "char_h": 0.04, "chars": nch})
        tag(b, lab, T(a, slen - 0.2, 0.101), n)
    # the repeat monitor of the mast camera on a short stand
    mc = (x, yb + 0.26, zb - 0.12)
    b.box(x - 0.04, yb - 0.02, zb - 0.16, x + 0.04, yb + 0.06, zb - 0.08, "gunmetal")
    b.box(x - 0.24, yb + 0.06, zb - 0.16, x + 0.24, yb + 0.44, zb - 0.08, "black")
    picture(b, "TV_CORE", (x, yb + 0.25, zb - 0.16), (0, 0, -1), lst=lit)
    tag(b, "MAST CAMERA RM-1", (x, yb + 0.085, zb - 0.161), (0, 0, -1))
    # procedure binders on the shelf under the top
    for k in range(6):
        bx = xa + 0.25 + k * 0.08
        b.box(bx - 0.035, OP + 0.22, zb - 0.4, bx + 0.035, OP + 0.55, zb - 0.1, pic("binders"))
    # sound-powered phone jack box on the desk's left leg, the headset on its hook; a radio in its charger
    jx = xa + 0.04
    b.box(jx - 0.025 - 0.1, OP + 0.7, zf + 0.15, jx - 0.025, OP + 0.86, zf + 0.33, YELLOW)
    RP.cyl(b, L, (jx - 0.125, OP + 0.8, zf + 0.2), (jx - 0.14, OP + 0.8, zf + 0.2), 0.02, "black", "black", 8, both=False)
    RP.cyl(b, L, (jx - 0.14, OP + 0.8, zf + 0.2), (jx - 0.2, OP + 0.56, zf + 0.24), 0.006, "black", "black", 6, both=False)
    RP.cyl(b, L, (jx - 0.21, OP + 0.56, zf + 0.18), (jx - 0.21, OP + 0.56, zf + 0.3), 0.045, "black", "black", 10, both=True)
    rc = T(0.15, 0.22, 0.03)
    b.obox(rc, u, sl, 0.06, 0.05, 0.03, "black", L)
    b.obox(T(0.15, 0.25, 0.1), u, sl, 0.025, 0.03, 0.04, "black", L)
    RP.cyl(b, L, T(0.165, 0.27, 0.14), T(0.165, 0.27, 0.24), 0.006, "black", "black", 6, both=False)
    # the name plate on the front
    b.box(x - 0.28, yt - 0.18, zf - 0.008, x + 0.28, yt - 0.08, zf, "stainless")
    tag(b, "REFUELING SRO", (x, yt - 0.13, zf - 0.008), (0, 0, -1))


def core_map(b):
    """The core map board on two legs, its face toward the SRO's place; a lamp in every core position (live)."""
    L = b.still
    x, z = MAP
    ang = math.radians(-30)                        # it faces south-east-ish (toward the SRO standing at the desk)
    n = (math.sin(ang), 0, -math.cos(ang))
    n = _unit((0.45, 0, -1))
    u = RM._right(n)
    yc = OP + 1.35
    W = 1.6
    RP.obox(b, L, (x - n[0] * 0.02, yc, z - n[2] * 0.02), u, (0, 1, 0), W / 2, W / 2, 0.02, BLUE)
    face = (x, yc, z)
    picture(b, "CORE_MAP", face, n, lift=0.002)
    for s in (-1, 1):
        lx, lz = x + u[0] * s * (W / 2 - 0.1) - n[0] * 0.06, z + u[2] * s * (W / 2 - 0.1) - n[2] * 0.06
        b.box(lx - 0.03, OP + 0.02, lz - 0.03, lx + 0.03, yc - W / 2 + 0.02, lz + 0.03, BLUE)
        RP.obox(b, L, (lx, OP + 0.01, lz), n, (0, 1, 0), 0.25, 0.01, 0.05, BLUE)
    # each core position's lamp: a small square just off the board's face (the client lights it: green in place,
    # dark empty, amber moving)
    S = 1.5
    p = S * 0.86 / 15
    ox, oy = -S / 2 + S * 0.09, S / 2 - S * 0.09
    v = (0, 1, 0)
    for i, row in enumerate(RH.CORE_ROWS):
        c0, c1 = RM.CORE_MAP[row]
        for col in range(c0, c1 + 1):
            a = ox + (col - 0.5) * p
            bb = oy - (i + 0.5) * p
            c = tuple(face[k] + u[k] * a + v[k] * bb + n[k] * 0.005 for k in range(3))
            b.live["cells"][row + "-%02d" % col] = {"c": [round(q, 4) for q in c], "u": [round(q, 4) for q in u], "n": [round(q, 4) for q in n], "half": round(p * 0.33, 4)}


def cell_ops(b, states):
    """The core map's cell lamps as the client draws them: states maps 'K-11' to 'in', 'empty' or 'moving'."""
    out = {"still": [], "lit": []}
    for key, cl in b.live["cells"].items():
        st = states.get(key, "in")
        c, u, n, h = cl["c"], cl["u"], cl["n"], cl["half"]
        v = (0, 1, 0)
        P = lambda a, bb: tuple(c[k] + u[k] * a + v[k] * bb for k in range(3))
        q = [*CB._r(*P(-h, h), *P(-h, -h), *P(h, -h), *P(h, h))]
        if st == "in":
            out["lit"].append(["q", *q, pic("lamp_green")])
        elif st == "moving":
            out["lit"].append(["q", *q, pic("lamp_amber")])
        else:
            out["still"].append(["q", *q, sw("dgrey")])
    return out


# ---------------------------------------------------------------- radiation protection

def arm(b):
    """RM-ARM-10: its detector on an arm off a post over the railing toward the pool; the local readout and alarm panel
    on the post (live reading, alarm lamps), the beacon and horn on top, the RADIATION AREA sign."""
    L = b.still
    lit = b.lit
    x, z = ARM
    b.box(x - 0.2, OP, z - 0.2, x + 0.2, OP + 0.02, z + 0.2, "gunmetal")
    for dx in (-0.15, 0.15):
        for dz in (-0.15, 0.15):
            RP.bolt(b, L, (x + dx, OP + 0.02, z + dz), (0, 1, 0), 0.01, 0.008, False)
    b.box(x - 0.05, OP + 0.02, z - 0.05, x + 0.05, OP + 2.4, z + 0.05, YELLOW)
    # the arm out over the railing and the detector (a cylinder, its cable back to the panel)
    b.box(x - 0.03, OP + 2.2, z - 0.03, x + 0.03, OP + 2.26, z + 0.95, YELLOW)
    RP.cyl(b, L, (x, OP + 2.2, z + 0.9), (x, OP + 1.85, z + 0.9), 0.07, "stainless", "stainless_dark", 14, both=True)
    RP.cyl(b, L, (x, OP + 1.85, z + 0.9), (x, OP + 1.8, z + 0.9), 0.05, "stainless_dark", "stainless_dark", 12, both=False)
    RP.conduit(b, L, [(x, OP + 2.2, z + 0.9), (x, OP + 2.3, z + 0.9), (x + 0.07, OP + 2.3, z + 0.05), (x + 0.07, OP + 1.55, z + 0.05)], 0.012, "black")
    tag(b, "RM-ARM-10 DETECTOR", (x + 0.051, OP + 1.95, z), (1, 0, 0))
    # the local panel facing south (the deck), its face, the reading window, lamps
    pz = z - 0.05
    b.box(x - 0.24, OP + 1.2, pz - 0.14, x + 0.24, OP + 1.58, pz, GREY)
    picture(b, "ARM_FACE", (x, OP + 1.39, pz - 0.14), (0, 0, -1))
    w, h = legend_size("ARM_FACE")
    u = RM._right((0, 0, -1))
    win = (x + u[0] * ((0.12 + 0.76) / 2 - 0.5) * w, OP + 1.39 + (0.5 - 0.51) * h, pz - 0.143)
    b.live["readouts"].append({"name": "mr_hr", "centre": [round(v, 4) for v in win], "normal": [0, 0, -1], "char_h": 0.065, "chars": 4})
    for k, (key, col, ax, ay) in enumerate((("alarm", "red", 0.07, 0.72), ("alert", "amber", 0.57, 0.72), ("fail", "white", 0.07, 0.85))):
        c = (x + u[0] * (ax - 0.5) * w, OP + 1.39 + (0.5 - ay - 0.025) * h, pz - 0.14)
        b.cyl(c, (c[0], c[1], c[2] - 0.012), 0.012, "stainless", "gunmetal", 10, lst=L)
        b.live["lamps"][key] = {"a": [round(v, 4) for v in (c[0], c[1], c[2] - 0.012)], "b": [round(v, 4) for v in (c[0], c[1], c[2] - 0.018)], "r": 0.009, "colour": col}
    c = (x + u[0] * (0.57 - 0.5) * w, OP + 1.39 + (0.5 - 0.85 - 0.025) * h, pz - 0.14)
    b.cyl(c, (c[0], c[1], c[2] - 0.014), 0.011, "black", "black", 10, lst=L)                 # the RESET push button
    # beacon and horn on the post's top
    RP.cyl(b, L, (x, OP + 2.4, z), (x, OP + 2.46, z), 0.08, "gunmetal", "gunmetal", 12, both=False)
    RP.cyl(b, L, (x, OP + 2.46, z), (x, OP + 2.66, z), 0.07, ["lamp_red", 0.2, 0.2, 0.8, 0.8], ["lamp_red", 0.2, 0.2, 0.8, 0.8], 12, both=False)
    RP.cyl(b, L, (x, OP + 2.66, z), (x, OP + 2.69, z), 0.075, "gunmetal", "gunmetal", 12, both=False)
    b.live["beacon"] = [x, OP + 2.56, z]
    hz = z - 0.05
    b.box(x - 0.06, OP + 2.12, hz - 0.12, x + 0.06, OP + 2.2, hz - 0.0, "gunmetal")
    RP.cyl(b, L, (x, OP + 2.16, hz - 0.12), (x, OP + 2.16, hz - 0.22), 0.05, GREY, "black", 12, both=True)
    sign_on_post(b, "SIGN_RAD", x, OP + 0.85, z - 0.055, (0, 0, -1))


def control_point(b):
    """The contamination control point at the railing's gate: gate posts with signs and the chain, the bins inside, the
    step-off pad outside, the frisker on its stand, the FME sign and log stand."""
    L = b.still
    g0, g1 = GATE_X
    zr = RAIL_LINE
    # the gate's posts (the building's railing stops at them) and the chain hung across, slack
    for gx in (g0, g1):
        b.box(gx - 0.04, OP, zr - 0.04, gx + 0.04, OP + 1.15, zr + 0.04, YELLOW)
        RP.cyl(b, L, (gx, OP + 1.15, zr), (gx, OP + 1.19, zr), 0.05, YELLOW, YELLOW, 10, both=False)
    pts = [(g0 + 0.04, OP + 0.95, zr)]
    for k in range(1, 12):
        t = k / 12
        pts.append((g0 + 0.04 + (g1 - g0 - 0.08) * t, OP + 0.95 - 0.18 * math.sin(math.pi * t), zr))
    pts.append((g1 - 0.04, OP + 0.95, zr))
    for k, (p, q) in enumerate(zip(pts, pts[1:])):
        RP.cyl(b, L, p, q, 0.012, sw("purple") if k % 2 else sw("yellow"), sw("yellow"), 6, both=False)
    # the boundary's signs: on the gate post (outside) and hung on the chain
    sign_on_post(b, "SIGN_CONTAM", (g0 + g1) / 2, OP + 0.6, zr - 0.01, (0, 0, -1))
    RP.cyl(b, L, ((g0 + g1) / 2 - 0.18, OP + 0.78, zr - 0.006), ((g0 + g1) / 2 - 0.18, OP + 0.86, zr), 0.004, "stainless_dark", "stainless_dark", 4, both=False)
    RP.cyl(b, L, ((g0 + g1) / 2 + 0.18, OP + 0.78, zr - 0.006), ((g0 + g1) / 2 + 0.18, OP + 0.86, zr), 0.004, "stainless_dark", "stainless_dark", 4, both=False)
    sign_on_post(b, "SIGN_FME", g0 - 0.6, OP + 0.85, zr - 0.06, (0, 0, -1))
    # bins inside the boundary (between the railing and the rail): PCs, laundry, rad waste, each a drum with a liner
    for k, (lab, col) in enumerate((("PC BIN", "yellow"), ("LAUNDRY", "lblue"), ("RAD WASTE", "yellow"))):
        bx, bz = g0 - 0.5 + k * 0.62, zr + 0.6
        RP.cyl(b, L, (bx, OP, bz), (bx, OP + 0.85, bz), 0.27, GREY, "black", 16, both=False)
        RP.cyl(b, L, (bx, OP + 0.85, bz), (bx, OP + 0.9, bz), 0.29, sw(col), sw(col), 16, both=False)
        for yy in (OP + 0.25, OP + 0.6):
            RP.cyl(b, L, (bx, yy - 0.015, bz), (bx, yy + 0.015, bz), 0.275, GREY, GREY, 16, both=False)
        RP.obox(b, L, (bx, OP + 0.45, bz - 0.278), (1, 0, 0), (0, 1, 0), 0.13, 0.04, 0.003, "stainless")
        tag(b, lab, (bx, OP + 0.45, bz - 0.281), (0, 0, -1))
    # the step-off pad on the deck just outside the gate
    pc = ((g0 + g1) / 2, OP + 0.012, zr - 0.5)
    b.box(pc[0] - 0.47, OP, pc[2] - 0.32, pc[0] + 0.47, OP + 0.01, pc[2] + 0.32, "rubber")
    picture(b, "STEP_OFF", (pc[0], OP + 0.01, pc[2]), (0, 1, 0), up=(0, 0, 1))
    # the frisker on its stand: post, a shelf with the meter (LCD live), the probe in its cradle and its cable,
    # the instruction sign
    fx, fz = FRISK
    b.box(fx - 0.18, OP, fz - 0.18, fx + 0.18, OP + 0.02, fz + 0.18, "gunmetal")
    b.box(fx - 0.03, OP + 0.02, fz + 0.06, fx + 0.03, OP + 1.7, fz + 0.125, GREY)
    b.box(fx - 0.2, OP + 1.02, fz - 0.12, fx + 0.2, OP + 1.04, fz + 0.12, GREY)
    m = (fx, OP + 1.04, fz)
    b.box(m[0] - 0.068, m[1], m[2] - 0.095, m[0] + 0.068, m[1] + 0.06, m[2] + 0.095, YELLOW)
    lc = (m[0], m[1] + 0.061, m[2] - 0.03)
    b.live["lcd"] = {"centre": [lc[0], round(lc[1] + 0.001, 4), lc[2]], "normal": [0, 1, 0], "up": [0, 0, 1], "char_h": 0.03, "chars": 3}
    RP.obox(b, L, (lc[0], lc[1] - 0.0005, lc[2]), (1, 0, 0), (0, 0, 1), 0.052, 0.024, 0.0006, sw("mgrey"))
    for k, bx in enumerate((-0.04, 0.0, 0.04)):
        RP.cyl(b, L, (m[0] + bx, m[1] + 0.06, m[2] + 0.05), (m[0] + bx, m[1] + 0.07, m[2] + 0.05), 0.01, "black", "black", 8, both=False)
    tag(b, "FRISKER", (m[0], m[1] + 0.03, m[2] - 0.096), (0, 0, -1))
    # the probe (a pancake GM detector on its handle) in a cradle on the shelf's right, its coiled cable
    pr = (fx + 0.14, OP + 1.04, fz)
    b.box(pr[0] - 0.045, pr[1], pr[2] - 0.08, pr[0] + 0.045, pr[1] + 0.03, pr[2] + 0.08, "gunmetal")
    RP.obox(b, L, (pr[0], pr[1] + 0.05, pr[2] + 0.02), (1, 0, 0), (0, 0, 1), 0.035, 0.06, 0.018, "black")
    RP.cyl(b, L, (pr[0], pr[1] + 0.05, pr[2] - 0.04), (pr[0], pr[1] + 0.05, pr[2] - 0.12), 0.015, "black", "black", 8, both=True)
    RP.cyl(b, L, (pr[0], pr[1] + 0.05, pr[2] - 0.12), (m[0] + 0.068, m[1] + 0.03, m[2] - 0.06), 0.005, "black", "black", 6, both=False)
    sign_on_post(b, "SIGN_FRISK", fx, OP + 1.4, fz + 0.055, (0, 0, -1))
    # the FME log stand (a lectern with the log on it) left of the pad
    lx, lz = g0 - 0.65, zr - 0.8
    b.box(lx - 0.03, OP + 0.015, lz - 0.03, lx + 0.03, OP + 1.0, lz + 0.03, GREY)
    b.box(lx - 0.2, OP, lz - 0.2, lx + 0.2, OP + 0.015, lz + 0.2, GREY)
    sl = _unit((0, 0.25, 1))
    nn = _unit(_cross((1, 0, 0), sl))
    if nn[1] < 0:
        nn = tuple(-c for c in nn)
    top = (lx, OP + 1.05, lz)
    RP.obox(b, L, top, (1, 0, 0), sl, 0.22, 0.17, 0.015, GREY)
    lp = tuple(top[i] + nn[i] * 0.017 for i in range(3))
    b.obox(lp, (1, 0, 0), sl, 0.19, 0.14, 0.004, ["black", None, "black", "black", "black", "black"], L)
    picture(b, "LOG_PAGES", tuple(lp[i] + nn[i] * 0.005 for i in range(3)), nn, up=sl)
    b.box(lx - 0.15, OP + 0.85, lz - 0.031, lx + 0.15, OP + 0.93, lz - 0.03, "stainless")
    tag(b, "FME LOG", (lx, OP + 0.89, lz - 0.031), (0, 0, -1))


def fittings(b):
    """A life ring on the railing by the gate, the page and party phone station, a fire extinguisher on its stand."""
    L = b.still
    zr = RAIL_LINE
    # life ring hung on the railing's inner side (toward the pool), its line coiled on a hook
    rx = C - 2.0
    ring_c = (rx, OP + 0.62, zr + 0.06)
    for k in range(16):
        a0, a1 = 2 * math.pi * k / 16, 2 * math.pi * (k + 1) / 16
        p = (rx + 0.26 * math.cos(a0), ring_c[1] + 0.26 * math.sin(a0), ring_c[2])
        q = (rx + 0.26 * math.cos(a1), ring_c[1] + 0.26 * math.sin(a1), ring_c[2])
        RP.cyl(b, L, p, q, 0.06, sw("orange") if (k // 2) % 2 else sw("white"), sw("white"), 8, both=False)
    RP.cyl(b, L, (rx, OP + 0.9, zr + 0.02), (rx, OP + 0.9, zr + 0.1), 0.012, "stainless", "stainless", 6, both=False)
    b.box(rx - 0.15, OP + 1.12, zr + 0.03, rx + 0.15, OP + 1.2, zr + 0.032, "stainless")
    tag(b, "LIFE RING", (rx, OP + 1.16, zr + 0.032), (0, 0, 1))
    # page and party phone station (a yellow box with its handset) on a post by the desk
    px, pz = SRO[0] + 1.5, SRO[1] + 0.2
    b.box(px - 0.04, OP, pz - 0.04, px + 0.04, OP + 1.7, pz + 0.04, GREY)
    b.box(px - 0.15, OP + 1.1, pz - 0.2, px + 0.15, OP + 1.5, pz - 0.04, YELLOW)
    RP.obox(b, L, (px - 0.07, OP + 1.32, pz - 0.215), (0, 1, 0), (1, 0, 0), 0.12, 0.03, 0.015, "black")
    RP.cyl(b, L, (px - 0.07, OP + 1.2, pz - 0.215), (px - 0.03, OP + 0.95, pz - 0.23), 0.006, "black", "black", 6, both=False)
    for k in range(3):
        RP.cyl(b, L, (px + 0.07, OP + 1.42 - k * 0.07, pz - 0.2), (px + 0.07, OP + 1.42 - k * 0.07, pz - 0.215), 0.015, "black", "black", 8, both=False)
    tag(b, "PAGE  PARTY", (px, OP + 1.46, pz - 0.2), (0, 0, -1))
    # fire extinguisher on its stand
    ex, ez = SRO[0] - 1.2, SRO[1] - 0.6
    b.box(ex - 0.15, OP, ez - 0.15, ex + 0.15, OP + 0.02, ez + 0.15, sw("red"))
    b.box(ex - 0.02, OP + 0.02, ez + 0.08, ex + 0.02, OP + 1.1, ez + 0.12, sw("red"))
    RP.cyl(b, L, (ex, OP + 0.04, ez), (ex, OP + 0.62, ez), 0.08, sw("red"), sw("red"), 14, both=True)
    RP.cyl(b, L, (ex, OP + 0.62, ez), (ex, OP + 0.7, ez), 0.03, "black", "black", 8, both=False)
    RP.obox(b, L, (ex, OP + 0.73, ez), (1, 0, 0), (0, 1, 0), 0.05, 0.02, 0.015, "black")
    RP.cyl(b, L, (ex + 0.02, OP + 0.7, ez), (ex + 0.08, OP + 0.4, ez), 0.01, "black", "black", 6, both=False)
    RP.obox(b, L, (ex, OP + 0.35, ez - 0.081), (1, 0, 0), (0, 1, 0), 0.04, 0.06, 0.001, "nameplate")


def build():
    b = Desk()
    sro_desk(b)
    core_map(b)
    arm(b)
    control_point(b)
    fittings(b)
    return b


def placed(b, cells=None, move="047", step="3", mr="2.4", cpm="58", lamps=()):
    """The station for a still picture with its live parts as the client draws them."""
    out = {"still": list(b.still), "lit": list(b.lit)}
    c = cell_ops(b, cells or {})
    out["still"] += c["still"]
    out["lit"] += c["lit"]
    vals = {"move": move, "step": step, "mr_hr": mr}
    for r in b.live["readouts"]:
        out["lit"] += RM.led_ops(r["centre"], r["normal"], r["char_h"], vals[r["name"]].rjust(r["chars"]))
    lc = b.live["lcd"]
    out["still"] += lcd_ops(lc["centre"], lc["normal"], lc["char_h"], cpm.rjust(lc["chars"]), tuple(lc["up"]))
    lens = {"green": "lamp_green", "white": "lamp_white", "red": "lamp_red", "amber": "lamp_amber"}
    off = {"green": sw("green"), "white": sw("enamel"), "red": sw("red"), "amber": sw("orange")}
    for key, lp in b.live["lamps"].items():
        on = key in lamps
        out["lit" if on else "still"].append(["c", *CB._r(*lp["a"], *lp["b"], lp["r"]), 10, "gunmetal", pic(lens[lp["colour"]]) if on else off[lp["colour"]]])
    return out


def lcd_ops(centre, normal, char_h, text, up):
    ops = RM.led_ops(centre, normal, char_h, text, up=up)
    out = []
    k = 0
    for ch in text:
        if ch == " ":
            continue
        o = ops[k]
        out.append(o[:13] + [RH.lcd_char(ch)])
        k += 1
    return out


def ops_check():
    b = build()
    bad = len(CB.overlaps(b.ops()))
    o = placed(b, {"K-11": "empty", "H-08": "moving"})
    bad += len(CB.overlaps({"still": o["still"], "lit": o["lit"], "blink": []}))
    return bad


if __name__ == "__main__":
    print("overlaps", ops_check())
