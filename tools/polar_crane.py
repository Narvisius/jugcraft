"""The containment's polar crane (reactor building rebuild, machine 2).

Built like a real PWR polar crane (a single-failure-proof double box-girder bridge crane running on a circular rail on
the containment's ring girder, turning a full 360 degrees; Haiyang's: about 38 m long and 13.5 m wide, 300 t main and
25 t auxiliary hoists), as board operations (tools/control_boards.py) in the equipment sheet's materials, with
engraved tags, painted stencils and an LED load display on its own legend texture (reactor_head's: the reactor
building's pieces share one).

- Bridge: two box girders tapered at their ends, stiffening ribs, a walkway with handrails along each girder's outside,
  the trolley rails on the girders' tops; two end trucks on the circular rail, each with two wheel bogies, a bridge
  drive (motor, gearbox, brake), anti-derailment hooks round the rail head and a current collector arm.
- Trolley (the client moves it along the bridge): frame, four wheels, the main hoist (big grooved drum, motor, gearbox,
  two brakes: single-failure proof), the auxiliary hoist, the trolley drive, the LED load display (live) and a beacon.
- Main hook block (the client lowers and raises it; the ropes are drawn from the drum to it): 8 falls, a duplex
  (ramshorn) hook. Auxiliary hook block: 2 falls, a single hook.
- Operator's cab under the first girder at one end: windows on three sides and in the floor, the seat and both master
  controllers inside, its door onto a platform and a ladder up to the walkway.

Frame: x along the bridge (0 to SPAN), y up from the rail's top, z across (0 to WIDTH); the turning axis is at
(SPAN / 2, WIDTH / 2). The client turns the whole crane about the containment's axis by the bridge's angle.

Live (LIVE in the operations): the trolley's travel and the two hook heights, the load display (the hook's load in
short tons, e.g. the head package 147.6), the beacons (flashing while anything moves).
"""
import math
from pathlib import Path

import control_boards as CB
from control_boards import Board, sw
import reactor_head as RH
from reactor_head import Head, legend

RAIL_R = 20.6             # the rail's radius (the ring girder's)
SPAN = 2 * RAIL_R
WIDTH = 8.0
ZC = WIDTH / 2
GIRDERS = (ZC - 2.6, ZC + 2.6)
GW = 0.45                 # half a girder's width
G_BOT, G_TOP = 0.9, 3.3   # a girder's bottom and top (middle part)
G_END_BOT = 1.7           # its bottom at the ends
RAIL_Y = G_TOP + 0.14     # the trolley rails' top
YELLOW = "paint_yellow"


class Crane(Head):
    def __init__(self):
        Board.__init__(self, int(SPAN) + 1, 202)
        self.live = {"trolley": {"still": [], "lit": []}, "main_block": {"still": []}, "aux_block": {"still": []},
                     "beacons": [], "displays": [], "ropes": {}}


def _sub(b, group):
    """A list of operations for a part the client moves (the trolley, a hook block)."""
    return b.live[group]["still"]


# ---------------------------------------------------------------- the bridge

def girder(b, zc):
    """A box girder: its side plates (the bottom tapering up at both ends), top and bottom flanges, stiffening ribs on
    the outside, the trolley rail on top."""
    x0, x1 = 1.3, SPAN - 1.3
    taper = 3.4
    # outline along x (bottom): ends at G_END_BOT, middle at G_BOT
    pts = [(x0, G_END_BOT), (x0 + taper, G_BOT), (x1 - taper, G_BOT), (x1, G_END_BOT)]
    for zf, sgn in ((zc - GW, -1), (zc + GW, 1)):
        for (xa, ya), (xb, yb) in zip(pts, pts[1:]):
            # a side quad from the bottom outline up to the top (seen from outside: -z side is the front)
            if sgn < 0:
                b.quad((xa, G_TOP, zf), (xa, ya, zf), (xb, yb, zf), (xb, G_TOP, zf), YELLOW)
            else:
                b.quad((xb, G_TOP, zf), (xb, yb, zf), (xa, ya, zf), (xa, G_TOP, zf), YELLOW)
    # bottom flange pieces (seen from below), the top, the end plates
    for (xa, ya), (xb, yb) in zip(pts, pts[1:]):
        b.quad((xa, ya, zc - GW), (xa, ya, zc + GW), (xb, yb, zc + GW), (xb, yb, zc - GW), YELLOW)
    b.box(x0, G_TOP, zc - GW - 0.06, x1, G_TOP + 0.05, zc + GW + 0.06, [YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW])
    for x, s in ((x0, -1), (x1, 1)):
        if s < 0:
            b.quad((x, G_TOP, zc + GW), (x, G_END_BOT, zc + GW), (x, G_END_BOT, zc - GW), (x, G_TOP, zc - GW), YELLOW)
        else:
            b.quad((x, G_TOP, zc - GW), (x, G_END_BOT, zc - GW), (x, G_END_BOT, zc + GW), (x, G_TOP, zc + GW), YELLOW)
    # trolley rail (crane rail on its pad)
    b.box(x0 + 0.6, G_TOP + 0.05, zc - 0.12, x1 - 0.6, G_TOP + 0.08, zc + 0.12, "gunmetal")
    b.box(x0 + 0.6, G_TOP + 0.08, zc - 0.05, x1 - 0.6, RAIL_Y, zc + 0.05, "stainless_dark")
    # stiffening ribs on the outside face, leaving the stencils' panels clear
    out = -1 if zc < ZC else 1
    zf = zc + out * GW
    clear = [(SPAN / 2 - 2.0, SPAN / 2 + 4.8), (4.6, 7.4), (SPAN - 7.4, SPAN - 4.6)]
    x = x0 + taper + 0.3
    while x < x1 - taper:
        if not any(a <= x <= c for a, c in clear):
            if out < 0:
                b.box(x - 0.025, G_BOT + 0.06, zf - 0.04, x + 0.025, G_TOP - 0.06, zf, [YELLOW, None, YELLOW, YELLOW, YELLOW, YELLOW])
            else:
                b.box(x - 0.025, G_BOT + 0.06, zf, x + 0.025, G_TOP - 0.06, zf + 0.04, [None, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW])
        x += 1.6
    # stencils on the outside face: the rated capacity in the middle, the crane's number toward each end
    n = (0, 0, out)
    b.stencil("300 / 25 TON", (SPAN / 2, (G_BOT + G_TOP) / 2, zf), n)
    for xs in (6.0, SPAN - 6.0):
        b.stencil("PC-1", (xs, (G_BOT + G_TOP) / 2 + 0.2, zf), n)
    # the load display: an LED panel in a black case on the girder's outside, the hook's load in tons (live)
    xd = SPAN / 2 + 3.4
    yd = G_BOT + 0.75
    if out < 0:
        b.box(xd - 1.0, yd - 0.32, zf - 0.12, xd + 1.0, yd + 0.32, zf, ["black", None, "black", "black", "black", "black"])
        b.live.setdefault("displays", []).append([round(xd - 0.92, 4), round(yd - 0.25, 4), round(xd + 0.92, 4), round(yd + 0.25, 4), round(zf - 0.125, 4), -1])
    else:
        b.box(xd - 1.0, yd - 0.32, zf, xd + 1.0, yd + 0.32, zf + 0.12, [None, "black", "black", "black", "black", "black"])
        b.live.setdefault("displays", []).append([round(xd - 0.92, 4), round(yd - 0.25, 4), round(xd + 0.92, 4), round(yd + 0.25, 4), round(zf + 0.125, 4), 1])


def walkway(b, zc):
    """Grating walkway on brackets along the girder's outside at its top, toe plate and a two-rail handrail."""
    out = -1 if zc < ZC else 1
    z_in = zc + out * (GW + 0.06)
    z_out = z_in + out * 0.8
    za, zb = min(z_in, z_out), max(z_in, z_out)
    x0, x1 = 2.2, SPAN - 2.2
    b.box(x0, G_TOP - 0.03, za, x1, G_TOP + 0.02, zb, ["grating"] * 6)
    for x in [x0 + 0.4 + k * 2.0 for k in range(int((x1 - x0) / 2.0))]:
        # bracket under the deck (a knee brace back to the girder's side)
        b.box(x - 0.04, G_TOP - 0.15, za, x + 0.04, G_TOP - 0.03, zb, "gunmetal")
        ze = z_out - out * 0.04
        b.cyl((x, G_TOP - 0.15, ze), (x, G_TOP - 0.15 - 0.45, z_in + out * 0.02), 0.025, "gunmetal", "gunmetal", 6)
    # toe plate and handrail on the outer edge
    ze = z_out
    b.box(x0, G_TOP + 0.02, min(ze, ze - out * 0.02), x1, G_TOP + 0.12, max(ze, ze - out * 0.02), YELLOW)
    k = int((x1 - x0) / 1.6)
    for j in range(k + 1):
        x = x0 + (x1 - x0) * j / k
        b.cyl((x, G_TOP + 0.02, ze - out * 0.03), (x, G_TOP + 1.09, ze - out * 0.03), 0.022, YELLOW, YELLOW, 6)
    for hy, r in ((1.09, 0.026), (0.57, 0.02)):
        b.cyl((x0, G_TOP + hy, ze - out * 0.03), (x1, G_TOP + hy, ze - out * 0.03), r, YELLOW, YELLOW, 6)


def end_truck(b, xc, i):
    """An end truck across the girders' end: its box, two wheel bogies on the rail (the wheels set on the rail's
    circle), the bridge drive, anti-derailment hooks, buffers and the current collector arm."""
    xin = 1 if xc < SPAN / 2 else -1
    b.box(xc - 0.65, 0.35, -0.3, xc + 0.65, 1.75, WIDTH + 0.3, YELLOW)
    # hazard stripes on its ends (the corners that sweep round past the containment wall)
    b.box(xc - 0.64, 0.45, -0.32, xc + 0.64, 1.0, -0.3, ["hazard", None, None, None, None, None])
    b.box(xc - 0.64, 0.45, WIDTH + 0.3, xc + 0.64, 1.0, WIDTH + 0.32, [None, "hazard", None, None, None, None])
    for zb in (0.9, WIDTH - 0.9):
        # where the rail is under this bogie: the rail's circle about the axis
        dz = zb - ZC
        dx = RAIL_R - math.sqrt(RAIL_R ** 2 - dz ** 2)
        xr = dx if xc < SPAN / 2 else SPAN - dx
        # bogie frame and its two wheels (axles along x, rolling along z)
        b.box(xr - 0.42, 0.18, zb - 0.95, xr + 0.42, 0.42, zb + 0.95, YELLOW)
        for zw in (zb - 0.55, zb + 0.55):
            b.cyl((xr - 0.18, 0.0 + 0.36, zw), (xr + 0.18, 0.36, zw), 0.36, "carbon_steel", "gunmetal", 14)
            b.cyl((xr + 0.18, 0.36, zw), (xr + 0.24, 0.36, zw), 0.12, "gunmetal", "gunmetal", 8)
        # anti-derailment hooks hanging round the rail head
        for zh in (zb - 0.9, zb + 0.9):
            b.box(xr - 0.3, -0.35, zh - 0.05, xr - 0.22, 0.18, zh + 0.05, "gunmetal")
            b.box(xr + 0.22, -0.35, zh - 0.05, xr + 0.3, 0.18, zh + 0.05, "gunmetal")
    # end platform across the bridge's end at the walkways' height, its handrail on the outer edge; the bridge drive on
    # it over the first bogie (motor, gearbox, disc brake) with its shaft down through the end truck to the wheels
    xo = -1 if xc < SPAN / 2 else 1
    pa, pb = (0.12, 1.28) if xo < 0 else (SPAN - 1.28, SPAN - 0.12)
    za, zb = GIRDERS[0] - GW - 0.86, GIRDERS[1] + GW + 0.86
    b.box(pa, G_TOP - 0.03, za, pb, G_TOP + 0.02, zb, ["grating"] * 6)
    for k in range(4):
        b.box(pa, G_TOP - 0.25, za + 0.4 + k * (zb - za - 0.8) / 3 - 0.05, pb, G_TOP - 0.03, za + 0.4 + k * (zb - za - 0.8) / 3 + 0.05, "gunmetal")
        zz = za + 0.4 + k * (zb - za - 0.8) / 3
    xe = pa + 0.04 if xo < 0 else pb - 0.04
    n = int((zb - za) / 1.6)
    for k in range(n + 1):
        z = za + (zb - za) * k / n
        b.cyl((xe, G_TOP + 0.02, z), (xe, G_TOP + 1.09, z), 0.022, YELLOW, YELLOW, 6)
    for hy, r in ((1.09, 0.026), (0.57, 0.02)):
        b.cyl((xe, G_TOP + hy, za), (xe, G_TOP + hy, zb), r, YELLOW, YELLOW, 6)
    zd = 0.9
    xm = (pa + pb) / 2
    yb = G_TOP + 0.02
    b.box(xm - 0.3, yb, zd - 0.35, xm + 0.3, yb + 0.55, zd + 0.35, "paint_grey")             # gearbox
    b.cyl((xm, yb + 0.3, zd + 0.35), (xm, yb + 0.3, zd + 1.3), 0.27, "paint_blue", "gunmetal", 14)  # motor
    b.cyl((xm, yb + 0.3, zd + 1.3), (xm, yb + 0.3, zd + 1.42), 0.29, "gunmetal", "gunmetal", 14)
    b.cyl((xm, yb + 0.3, zd - 0.35), (xm, yb + 0.3, zd - 0.6), 0.26, "stainless_dark", "red_iron", 14)  # disc brake
    b.cyl((xm, yb, zd), (xm, 1.75, zd), 0.07, "gunmetal", "gunmetal", 8)                     # drive shaft down
    b.tag("BRIDGE DRIVE %d" % i, (xm - xo * 0.301, yb + 0.3, zd), (-xo, 0, 0))
    # collector arm reaching out to the conductor bar on the ring girder's face (outward)
    xo = -1 if xc < SPAN / 2 else 1
    xa = xc + xo * 0.65
    b.box(min(xa, xa + xo * 0.9), 0.8, WIDTH - 2.1, max(xa, xa + xo * 0.9), 0.9, WIDTH - 2.0, "gunmetal")
    b.box(min(xa + xo * 0.9, xa + xo * 0.95), 0.6, WIDTH - 2.15, max(xa + xo * 0.9, xa + xo * 0.95), 1.1, WIDTH - 1.95, "black")


def bridge(b):
    for zc in GIRDERS:
        girder(b, zc)
        walkway(b, zc)
    end_truck(b, 0.65, 1)
    end_truck(b, SPAN - 0.65, 2)
    # cross ties between the girders near the ends (over the end trucks), festoon support beam along one girder
    for xc in (2.2, SPAN - 2.2):
        b.box(xc - 0.2, G_TOP - 0.6, GIRDERS[0] + GW, xc + 0.2, G_TOP - 0.05, GIRDERS[1] - GW, YELLOW)
    # festoon track for the trolley's cables, along the inside of girder 2 under its top
    z = GIRDERS[1] - GW - 0.15
    b.box(2.5, G_TOP - 0.35, z - 0.06, SPAN - 2.5, G_TOP - 0.25, z + 0.06, "gunmetal")
    for k in range(8):
        x = 4.0 + k * 0.5
        b.cyl((x, G_TOP - 0.36, z), (x, G_TOP - 1.05, z), 0.03, "black", "black", 6)
        b.box(x - 0.08, G_TOP - 0.36, z - 0.08, x + 0.08, G_TOP - 0.3, z + 0.08, "gunmetal")
    cab(b)


def cab(b):
    """The operator's cab under girder 1 near the x 0 end: steel frame and panels, windows on three sides and in the
    floor, the seat and both master controllers inside, a door onto a platform, a ladder up to the walkway."""
    x0, x1 = 3.4, 5.8
    zc = GIRDERS[0]
    z0, z1 = zc - 1.1, zc + 1.1
    y0, y1 = -1.6, G_BOT
    # frame posts and the roof hanging from the girder
    for x in (x0, x1):
        for z in (z0, z1):
            b.box(x - 0.05, y0, z - 0.05, x + 0.05, y1 - 0.12, z + 0.05, YELLOW)
    b.box(x0 - 0.05, y1 - 0.12, z0 - 0.05, x1 + 0.05, y1, z1 + 0.05, YELLOW)
    # floor: a steel border round a floor window
    b.box(x0 - 0.05, y0 - 0.12, z0 - 0.05, x1 + 0.05, y0, z1 + 0.05, [YELLOW, YELLOW, YELLOW, YELLOW, None, YELLOW])
    b.box(x0 + 0.05, y0 - 0.001, z0 + 0.05, x1 - 0.05, y0, z0 + 0.6, "paint_grey")
    b.box(x0 + 0.05, y0 - 0.001, z1 - 0.6, x1 - 0.05, y0, z1 - 0.05, "paint_grey")
    b.quad((x0 + 0.6, y0 + 0.002, z1 - 0.6), (x0 + 0.6, y0 + 0.002, z0 + 0.6), (x1 - 0.6, y0 + 0.002, z0 + 0.6), (x1 - 0.6, y0 + 0.002, z1 - 0.6),
           ["glass", 0.05, 0.05, 0.95, 0.95])
    # lower panels (to waist height) and the windows above them: front (-x end), the long side away from the girder's
    # middle (-z), the back (+x) has the door
    wy = y0 + 0.9
    for face, (a, c) in (("-x", (z0, z1)), ("-z", (x0, x1)), ("+z", (x0, x1))):
        if face == "-x":
            b.box(x0 - 0.04, y0, a, x0, wy, c, [None, None, YELLOW, None, YELLOW, None])
            b.quad((x0 - 0.02, y1 - 0.12, c - 0.05), (x0 - 0.02, wy, c - 0.05), (x0 - 0.02, wy, a + 0.05), (x0 - 0.02, y1 - 0.12, a + 0.05),
                   ["glass", 0.05, 0.05, 0.95, 0.95])
        elif face == "-z":
            b.box(a, y0, z0 - 0.04, c, wy, z0, [YELLOW, None, None, None, YELLOW, None])
            b.quad((a + 0.05, y1 - 0.12, z0 - 0.02), (a + 0.05, wy, z0 - 0.02), (c - 0.05, wy, z0 - 0.02), (c - 0.05, y1 - 0.12, z0 - 0.02),
                   ["glass", 0.05, 0.05, 0.95, 0.95])
        else:
            b.box(a, y0, z1, c, wy, z1 + 0.04, [None, YELLOW, None, None, YELLOW, None])
            b.quad((c - 0.05, y1 - 0.12, z1 + 0.02), (c - 0.05, wy, z1 + 0.02), (a + 0.05, wy, z1 + 0.02), (a + 0.05, y1 - 0.12, z1 + 0.02),
                   ["glass", 0.05, 0.05, 0.95, 0.95])
    # back wall with the door
    b.box(x1, y0, z0, x1 + 0.04, y1 - 0.12, z1, [None, None, None, YELLOW, None, None])
    b.box(x1 + 0.04, y0 + 0.05, zc - 0.35, x1 + 0.08, y0 + 2.0, zc + 0.35, "paint_grey")
    b.box(x1 + 0.08, y0 + 1.0, zc + 0.2, x1 + 0.12, y0 + 1.05, zc + 0.3, "black")
    # inside: the seat, both master controllers on their consoles, a small panel
    sx, sz = x0 + 1.4, zc
    b.box(sx - 0.25, y0, sz - 0.25, sx + 0.25, y0 + 0.45, sz + 0.25, "black")
    b.box(sx + 0.15, y0 + 0.45, sz - 0.25, sx + 0.25, y0 + 1.05, sz + 0.25, "black")
    for s in (-1, 1):
        cz = sz + s * 0.45
        b.box(sx - 0.35, y0, cz - 0.12, sx + 0.1, y0 + 0.75, cz + 0.12, "paint_grey")
        b.cyl((sx - 0.15, y0 + 0.75, cz), (sx - 0.15, y0 + 0.95, cz), 0.025, "black", "black", 6)
        b.cyl((sx - 0.15, y0 + 0.95, cz), (sx - 0.15, y0 + 1.0, cz), 0.05, "black", "black", 8)
    b.box(x0 + 0.05, y0 + 0.9, zc - 0.5, x0 + 0.25, y0 + 1.25, zc + 0.5, "paint_grey")
    b.tag("POLAR CRANE PC-1 CAB", (x1 + 0.04, y1 - 0.35, zc), (1, 0, 0))
    # platform at the door, its railing, the ladder up to walkway 1's deck
    px0, px1 = x1 + 0.12, x1 + 1.3
    b.box(px0, y0 - 0.06, zc - 0.7, px1, y0, zc + 0.7, ["grating"] * 6)
    for z in (zc - 0.7, zc + 0.7):
        b.cyl((px1, y0, z), (px1, y0 + 1.07, z), 0.022, YELLOW, YELLOW, 6)
        b.cyl((px0 + 0.05, y0 + 1.07, z), (px1, y0 + 1.07, z), 0.022, YELLOW, YELLOW, 6)
        b.cyl((px0 + 0.05, y0 + 0.55, z), (px1, y0 + 0.55, z), 0.018, YELLOW, YELLOW, 6)
    # ladder up the platform's outer edge, over the walkway's toe plate onto its deck
    lz = zc - 0.75 - 0.6     # outside girder 1, at walkway 1
    lx = px1 - 0.3
    for zr in (lz - 0.21, lz + 0.21):
        b.cyl((lx, y0, zr), (lx, G_TOP + 1.09, zr), 0.025, YELLOW, YELLOW, 6)
    y = y0 + 0.3
    while y < G_TOP:
        b.cyl((lx, y, lz - 0.21), (lx, y, lz + 0.21), 0.017, "gunmetal", "gunmetal", 6)
        y += 0.3
    b.box(px0, y0 - 0.06, lz - 0.3, px1, y0, zc - 0.7, ["grating"] * 6)


# ---------------------------------------------------------------- the trolley and hooks (moved by the client)

TROLLEY_L = 4.4
MAIN_DRUM = (0.0, 1.35)       # the main drum's x (from the trolley's middle) and radius... see trolley()


def trolley(b):
    """The trolley's operations in its own frame: x 0 at its middle (the client places it along the bridge), y from the
    rail's top, z as the bridge's. Returns where the ropes leave the drums."""
    L = b.live["trolley"]["still"]
    lit = b.live["trolley"]["lit"]
    hx = TROLLEY_L / 2
    y0 = RAIL_Y
    # wheels on both rails and the end carriages (saddles) over them
    for zr in GIRDERS:
        b.box(-hx, y0 + 0.25, zr - 0.3, hx, y0 + 0.75, zr + 0.3, YELLOW, L)
        for xw in (-hx + 0.5, hx - 0.5):
            b.cyl((xw, y0 + 0.24, zr - 0.12), (xw, y0 + 0.24, zr + 0.12), 0.24, "carbon_steel", "gunmetal", 12, lst=L)
    # deck between the saddles
    b.box(-hx, y0 + 0.75, GIRDERS[0] - 0.3, hx, y0 + 0.95, GIRDERS[1] + 0.3, YELLOW, L)
    # main hoist: grooved drum (axis along z) on two bearing pedestals, its gearbox and motor, two brakes
    dx, dr = -0.5, 0.75
    dy = y0 + 0.95 + dr + 0.12
    b.cyl((dx, dy, GIRDERS[0] + 0.55), (dx, dy, GIRDERS[1] - 0.55), dr, "crdm", "gunmetal", 20, lst=L)
    for zb in (GIRDERS[0] + 0.45, GIRDERS[1] - 0.45):
        b.box(dx - 0.3, y0 + 0.95, zb - 0.1, dx + 0.3, dy - 0.15, zb + 0.1, YELLOW, L)          # bearing pedestal
        b.cyl((dx, dy, zb - 0.12), (dx, dy, zb + 0.12), 0.24, "gunmetal", "gunmetal", 12, lst=L)  # bearing housing
    gz = GIRDERS[1] - 0.1
    b.box(dx - 0.6, y0 + 0.95, gz - 0.25, dx + 0.6, dy + 0.55, gz + 0.35, "paint_grey", L)     # gearbox
    b.cyl((dx + 0.6, dy + 0.1, gz + 0.05), (dx + 1.7, dy + 0.1, gz + 0.05), 0.33, "paint_blue", "gunmetal", 14, lst=L)  # motor
    for bx in (dx + 0.75, dx + 0.95):
        b.cyl((bx, dy + 0.1, gz + 0.05), (bx + 0.08, dy + 0.1, gz + 0.05), 0.42, "stainless_dark", "red_iron", 16, lst=L)  # brakes
    # auxiliary hoist at the trolley's +x end
    ax, ar = 1.55, 0.32
    ay = y0 + 0.95 + ar + 0.1
    b.cyl((ax, ay, ZC - 1.0), (ax, ay, ZC + 0.6), ar, "crdm", "gunmetal", 14, lst=L)
    for zb in (ZC - 1.1, ZC + 0.66):
        b.box(ax - 0.2, y0 + 0.95, zb - 0.06, ax + 0.2, ay + 0.1, zb + 0.06, YELLOW, L)
    b.box(ax - 0.3, y0 + 0.95, ZC + 0.75, ax + 0.3, ay + 0.35, ZC + 1.25, "paint_grey", L)
    b.cyl((ax, ay + 0.05, ZC + 1.25), (ax, ay + 0.05, ZC + 2.0), 0.2, "paint_blue", "gunmetal", 12, lst=L)
    # trolley drive at the -x end: motor, gearbox, a cross shaft to both wheel sets
    tx = -hx + 0.35
    b.box(tx - 0.25, y0 + 0.95, ZC - 0.3, tx + 0.25, y0 + 1.45, ZC + 0.3, "paint_grey", L)
    b.cyl((tx, y0 + 1.2, ZC - 0.3), (tx, y0 + 1.2, ZC - 1.0), 0.2, "paint_blue", "gunmetal", 12, lst=L)
    b.cyl((tx, y0 + 0.5, GIRDERS[0] + 0.3), (tx, y0 + 0.5, GIRDERS[1] - 0.3), 0.06, "gunmetal", "gunmetal", 8, lst=L)
    # tags on the hoists' gear cases and the drive (facing out to +z/-z sides)
    _tag_on(b, L, "MAIN HOIST 300 T", (dx, dy + 0.35, gz + 0.351), (0, 0, 1))
    _tag_on(b, L, "AUX HOIST 25 T", (ax, ay + 0.18, ZC + 1.251), (0, 0, 1))
    _tag_on(b, L, "TROLLEY DRIVE", (tx, y0 + 1.3, ZC + 0.301), (0, 0, 1))
    # beacon on the trolley
    b.cyl((hx - 0.3, y0 + 0.95, ZC - 1.6), (hx - 0.3, y0 + 1.1, ZC - 1.6), 0.12, "gunmetal", "gunmetal", 10, lst=L)
    b.cyl((hx - 0.3, y0 + 1.1, ZC - 1.6), (hx - 0.3, y0 + 1.35, ZC - 1.6), 0.1, "lamp_amber", "lamp_amber", 10, lst=lit)
    b.live["beacons"].append(["trolley", hx - 0.3, y0 + 1.22, ZC - 1.6])
    # where the ropes leave: the main drum's underside at 4 places (8 falls run from them to the block), the aux drum
    main = [(dx + s * 0.05, dy - dr, ZC + k * 0.45) for k in (-1.5, -0.5, 0.5, 1.5) for s in (-1, 1)]
    aux = [(ax, ay - ar, ZC - 0.6), (ax, ay - ar, ZC + 0.2)]
    b.live["ropes"] = {"main": [list(map(lambda v: round(v, 4), p)) for p in main], "aux": [list(map(lambda v: round(v, 4), p)) for p in aux]}
    return main, aux


def _tag_on(b, lst, text, centre, normal):
    keep = b.still
    b.still = lst
    try:
        b.tag(text, centre, normal)
    finally:
        b.still = keep


def main_block(b):
    """The main hook block in its own frame (y 0 at the hook's bite): the sheave block (8 falls: 4 sheaves), the
    crosshead, the duplex hook. Returns where the ropes reach it."""
    L = b.live["main_block"]["still"]
    # sheave housing
    b.box(-0.55, 1.4, ZC - 1.0, 0.55, 2.5, ZC + 1.0, YELLOW, L)
    # the sheaves' axle caps on both sides
    for k in (-1.5, -0.5, 0.5, 1.5):
        for s in (-1, 1):
            b.cyl((s * 0.55, 2.05, ZC + k * 0.45), (s * 0.6, 2.05, ZC + k * 0.45), 0.09, "gunmetal", "gunmetal", 8, lst=L)
    # sheave guards' bolts, its tag stencil
    b.box(-0.6, 1.3, ZC - 0.6, 0.6, 1.4, ZC + 0.6, "gunmetal", L)          # crosshead
    b.cyl((0, 1.3, ZC), (0, 0.95, ZC), 0.16, "gunmetal", "gunmetal", 10, lst=L)  # hook shank (thrust bearing nut above)
    b.box(-0.22, 1.15, ZC - 0.22, 0.22, 1.3, ZC + 0.22, "gunmetal", L)
    # duplex (ramshorn) hook: a body and two horns curving out and up (blocky arcs)
    b.box(-0.18, 0.55, ZC - 0.22, 0.18, 0.95, ZC + 0.22, "carbon_steel", L)
    for s in (-1, 1):
        pts = [(0.0, 0.62), (0.28, 0.5), (0.5, 0.3), (0.6, 0.05), (0.58, -0.15), (0.45, -0.25), (0.35, -0.12)]
        for (xa, ya), (xb, yb) in zip(pts, pts[1:]):
            b.cyl((s * xa, ya, ZC), (s * xb, yb, ZC), 0.14 if ya > 0 else 0.11, "carbon_steel", "carbon_steel", 10, lst=L)
    # ropes reach the block's top at 8 places
    return [(s * 0.05, 2.5, ZC + k * 0.45) for k in (-1.5, -0.5, 0.5, 1.5) for s in (-1, 1)]


def aux_block(b):
    L = b.live["aux_block"]["still"]
    b.box(-0.2, 0.55, ZC - 0.25, 0.2, 1.0, ZC + 0.25, YELLOW, L)
    b.cyl((0, 0.55, ZC - 0.2), (0, 0.3, ZC - 0.2), 0.06, "gunmetal", "gunmetal", 8, lst=L)
    pts = [(0.0, 0.3), (0.12, 0.2), (0.16, 0.02), (0.1, -0.08), (0.0, -0.06), (-0.05, 0.04)]
    for (xa, ya), (xb, yb) in zip(pts, pts[1:]):
        b.cyl((xa, ya, ZC - 0.2), (xb, yb, ZC - 0.2), 0.045, "carbon_steel", "carbon_steel", 8, lst=L)
    return [(0.0, 1.0, ZC - 0.6 + 0.4), (0.0, 1.0, ZC + 0.2 - 0.4)]


def build():
    b = Crane()
    bridge(b)
    b.core = len(b.still)
    trolley(b)
    b.live["main_top"] = [list(p) for p in main_block(b)]
    b.live["aux_top"] = [list(p) for p in aux_block(b)]
    # beacons on the end trucks (lit lenses in the bridge's lit list)
    for xc in (0.65, SPAN - 0.65):
        b.cyl((xc, 1.75, WIDTH - 0.6), (xc, 1.9, WIDTH - 0.6), 0.12, "gunmetal", "gunmetal", 10)
        b.cyl((xc, 1.9, WIDTH - 0.6), (xc, 2.15, WIDTH - 0.6), 0.1, "lamp_amber", "lamp_amber", 10, lst=b.lit)
        b.live["beacons"].append(["bridge", xc, 2.02, WIDTH - 0.6])
    return b


def placed(b, trolley_x, main_y, aux_y):
    """The whole crane's operations for a still picture (the mockup): the trolley at trolley_x along the bridge, the
    hooks' bites at main_y and aux_y, the ropes drawn between drum and block."""
    out = {"still": list(b.still), "lit": list(b.lit)}

    def moved(ops, dx, dy):
        res = []
        for op in ops:
            k = op[0]
            o = list(op)
            if k == "b":
                o[1] += dx; o[4] += dx; o[2] += dy; o[5] += dy
            elif k == "o":
                o[1] += dx; o[2] += dy
            elif k == "q":
                for i in (1, 4, 7, 10):
                    o[i] += dx; o[i + 1] += dy
            elif k == "c":
                o[1] += dx; o[4] += dx; o[2] += dy; o[5] += dy
            res.append(o)
        return res
    out["still"] += moved(b.live["trolley"]["still"], trolley_x, 0)
    out["lit"] += moved(b.live["trolley"]["lit"], trolley_x, 0)
    out["still"] += moved(b.live["main_block"]["still"], trolley_x - 0.5, main_y)
    aux_x = trolley_x + 1.55
    out["still"] += moved(b.live["aux_block"]["still"], aux_x, aux_y)
    # ropes
    for p, q in zip(b.live["ropes"]["main"], b.live["main_top"]):
        a = (p[0] + trolley_x, p[1], p[2])
        c = (q[0] + trolley_x - 0.5, q[1] + main_y, q[2])
        out["still"].append(["c", *CB._r(*a, *c, 0.025), 6, "stainless_dark", "stainless_dark"])
    for p, q in zip(b.live["ropes"]["aux"], b.live["aux_top"]):
        a = (p[0] + trolley_x, p[1], p[2])
        c = (q[0] + aux_x, q[1] + aux_y, q[2])
        out["still"].append(["c", *CB._r(*a, *c, 0.02), 6, "stainless_dark", "stainless_dark"])
    return out


if __name__ == "__main__":
    b = build()
    o = placed(b, SPAN / 2, -8.0, -3.0)
    print("still", len(o["still"]), "overlaps", len(CB.overlaps(b.ops())), len(CB.overlaps({"still": o["still"], "lit": o["lit"], "blink": []})))
