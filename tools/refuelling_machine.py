"""The refuelling machine (reactor building rebuild, machine 3).

Built like a real Westinghouse PWR refuelling machine (patents US4311557 and US5687207): a bridge spanning the
refuelling cavity on rails along both its sides, a trolley running across the bridge, and on the trolley the mast:

- Bridge: two box girders on two end trucks, four wheels on the rails, spring bumpers at the trucks' ends; the bridge
  drive (motor, gearbox, brake) on the first end truck with a line shaft along the second girder to the far truck's
  wheels; a grating walkway with a handrail along the first girder, a ladder down to the operating floor; the trolley's
  rails on the girders' tops; a position strip (core rows A to R) on the first girder and a pointer on the trolley; a
  pointer and its camera over the bridge's position strip (core columns 1 to 15) on the cavity wall.
- Trolley: frame on four wheels, the trolley drive, a platform with a handrail (a gate to the walkway), the operator's
  console (the mast camera's monitor, the bridge, trolley, mast height and load readouts, gripper and fault lamps, the
  hoist, bridge, trolley and gripper controls, the emergency stop).
- Mast: the stationary mast housing on the trolley (a framed window onto the inner mast's top, the electrical cabinet,
  a ladder), the rotation ring with its locking pin at the deck, on the housing's top plate the mast hoist (drum, motor,
  brake) on its load cell and the air hose and cable reels; the outer mast (a square steel tube) down into the water to
  just above the cavity floor; inside it the inner mast (the client raises and lowers it on the hoist cables) with its
  lifting frame, swivel and air cylinder on top and the gripper at its foot (four fingers, two guide pins).
- A fuel assembly (17x17: bottom nozzle, rods and grids, top nozzle with hold-down springs) the gripper carries, and
  the core's top as the refuelling machine sees it (193 top nozzles in the vessel, the baffle round them).

The inner mast's length plus an assembly's is the outer mast's, so an assembly lifted all the way is inside the outer
mast, under water, while the bridge and trolley carry it (real: about 8 ft of water over it).

Frame: the reactor building's (tools/reactor_head.py): the vessel's axis at (C, C), y as reactor_head's (the cavity
floor at 12, the operating floor at OP). The rails run along x on the operating floor on both sides of the cavity
(z = RAIL_Z), from the cavity's far end to the transfer canal (machine 4). Groups the client moves (LIVE): the bridge
(along x), the trolley (along z, with the bridge), the inner mast (up and down, with the trolley), the gripper's two
states, the carried assembly; the console's readouts and lamps.

Live values (the client draws them from the machine's state): bridge column and trolley row (where the mast is over
the core), mast height in inches down from fully up (0 to 413 at the core), the load cell in pounds (an assembly
weighs 1,467 lb in air, about 1,290 lb in water), GRIPPER ENGAGED / RELEASED, OVERLOAD and SLACK CABLE lamps.
"""
import math

import control_boards as CB
from control_boards import sw, pic
import reactor_head as RH
import rb_parts as RP
from reactor_head import Head, C, legend, legend_size, CORE_PITCH, CORE_ROWS, _cross, _unit

OP = RH.CAVITY_FLOOR + 9.0            # the operating floor
H = RH.CAVITY_HALF
RAIL_Z = (C - 7.4, C + 7.4)
RAIL_TOP = OP + 0.15
RAIL_X = (C - 6.2, C + 13.0)          # the rails' run: the cavity's far end to the transfer canal (machine 4)
WHEEL_R = 0.3
TRUCK = (OP + 0.6, OP + 1.15)         # an end truck's box (the wheels under it)
G_BOT, G_TOP = TRUCK[1], TRUCK[1] + 0.8
GIRDER_X = (-0.85, 0.85)              # the girders' middles, from the bridge's middle (the mast's line)
GW = 0.2
T_RAIL = G_TOP + 0.1                  # the trolley rails' top
DECK = T_RAIL + 0.4                   # the trolley's deck
T_Z = (-1.6, 3.6)                     # the trolley's ends, from the mast's line
HOUSING = 0.36                        # the mast housing's half width
HOUSING_TOP = DECK + 3.4
OUTER = 0.27                          # the outer mast's half width
MAST_BOT = RH.CAVITY_FLOOR + 0.4      # the outer mast's foot
INNER = 0.17                          # the inner mast's half width
FUEL_L = 4.06                         # an assembly's length (160 in)
FUEL_HW = 0.107                       # half its width (8.43 in square)
G_UP = MAST_BOT + 0.1 + FUEL_L        # the gripper's bite fully up (an assembly's foot just inside the outer mast)
INNER_L = DECK + 1.75 - G_UP          # the inner mast's length, bite to top (its top fully up seen in the window)
CORE_TOP = 6.06                       # the core's top nozzles' top
CORE_BOT = CORE_TOP - FUEL_L
WATER_Y = 20.0                        # the cavity's water while refuelling
INCH = 0.0254
BLUE = "paint_blue"
YELLOW = "paint_yellow"

# The core: 193 assemblies, the rows' column ranges (Westinghouse four-loop pattern).
CORE_MAP = {r: rng for r, rng in zip(CORE_ROWS, [(5, 11), (3, 13), (2, 14), (2, 14), (1, 15), (1, 15), (1, 15), (1, 15), (1, 15),
                                                   (1, 15), (1, 15), (2, 14), (2, 14), (3, 13), (5, 11)])}
# Which way the core's columns and rows run in the frame: the position strips are read left to right from where the
# operator looks at them (they are drawn the same way as tags), so the columns and rows follow the strips.
COL_DIR = None
ROW_DIR = None


def _right(normal, up=(0, 1, 0)):
    """'Right as seen from outside' on a face (tags' convention: the frame is mirrored)."""
    n = _unit(normal)
    u = _unit(_cross(up, n))
    return (-u[0], -u[1], -u[2])


COL_DIR = _right((0, 0, 1))[0]        # the column strip faces +z (on the -z cavity wall)
ROW_DIR = _right((-1, 0, 0))[2]       # the row strip faces -x (on girder 1's outside)


def cell_xz(row, col):
    """Where a core position (row letter, column 1-15) is."""
    i = CORE_ROWS.index(row) + 1
    return (C + COL_DIR * (col - 8) * CORE_PITCH, C + ROW_DIR * (i - 8) * CORE_PITCH)


class Machine(Head):
    def __init__(self):
        CB.Board.__init__(self, 21, 303)
        self.live = {"bridge": {"still": [], "lit": []}, "trolley": {"still": [], "lit": []}, "mast": {"still": [], "lit": []},
                     "gripper": {"engaged": [], "released": []}, "fuel": {"still": []}, "readouts": [], "lamps": {}, "beacon": None}


def _with(b, lst, fn, *a, **k):
    """Run a Head drawing method (tag, stencil) into another list."""
    keep = b.still
    b.still = lst
    try:
        fn(*a, **k)
    finally:
        b.still = keep


def picture(b, name, centre, normal, lst, up=(0, 1, 0), lift=0.003):
    """A legend picture (a screen, a strip) as one quad on a face, the right way round from outside."""
    w, h = legend_size(name)
    n = _unit(normal)
    u = _right(n, up)
    v = _unit(_cross(u, n))
    v = (-v[0], -v[1], -v[2])
    if RH._dot(v, up) < 0:
        v = (-v[0], -v[1], -v[2])
    c = tuple(centre[i] + n[i] * lift for i in range(3))
    P = lambda a, bb: tuple(c[i] + u[i] * a * w / 2 + v[i] * bb * h / 2 for i in range(3))
    b.quad(P(-1, 1), P(-1, -1), P(1, -1), P(1, 1), legend(name), lst)


def led_ops(centre, normal, char_h, text, up=(0, 1, 0)):
    """A live readout's characters as the client draws them: centred on centre, reading left to right from outside."""
    n = _unit(normal)
    u = _right(n, up)
    v = _unit(_cross(u, n))
    v = (-v[0], -v[1], -v[2])
    if RH._dot(v, up) < 0:
        v = (-v[0], -v[1], -v[2])
    w = char_h * RH.LED_CHAR[0] / RH.LED_CHAR[1]
    c = tuple(centre[i] + n[i] * 0.002 for i in range(3))
    out = []
    x0 = -len(text) * w / 2
    for k, ch in enumerate(text):
        if ch == " ":
            continue
        a0, a1 = x0 + k * w, x0 + (k + 1) * w
        P = lambda a, bb: tuple(c[i] + u[i] * a + v[i] * bb for i in range(3))
        out.append(["q", *CB._r(*P(a0, char_h / 2), *P(a0, -char_h / 2), *P(a1, -char_h / 2), *P(a1, char_h / 2)), RH.led_char(ch)])
    return out


# ---------------------------------------------------------------- the rails (the building's: they stay)

TROUGH_Z = RAIL_Z[1] + 0.56           # the bridge's trailing power cable's trough in the floor, outside the +z rail
FEED_X = C + 3.5                      # the floor junction box the trailing cable comes from
WEB = 0.18                            # a girder's half width at its webs (its flanges: GW + 0.04)


def rails(b):
    L = b.still
    for zr in RAIL_Z:
        RP.crane_rail(b, L, RAIL_X[0] + 0.35, RAIL_X[1] - 0.35, OP, zr)
        for xe, s in ((RAIL_X[0], 1), (RAIL_X[1], -1)):
            end_stop(b, L, xe, zr, s)
    # the trailing cable's trough: edge angles flush with the floor round a dark slot (the cable lies in it: placed())
    za, zb = TROUGH_Z - 0.1, TROUGH_Z + 0.1
    b.box(RAIL_X[0], OP, za - 0.03, RAIL_X[1], OP + 0.006, za, "gunmetal")
    b.box(RAIL_X[0], OP, zb, RAIL_X[1], OP + 0.006, zb + 0.03, "gunmetal")
    b.box(RAIL_X[0], OP, za, RAIL_X[1], OP + 0.002, zb, "black")
    # the floor junction box feeding it, its conduit down into the floor
    jx = FEED_X
    b.box(jx - 0.22, OP, zb + 0.08, jx + 0.22, OP + 0.5, zb + 0.32, "paint_grey")
    b.box(jx - 0.2, OP + 0.04, zb + 0.32, jx + 0.2, OP + 0.48, zb + 0.335, "paint_grey")
    for dx in (-0.17, 0.17):
        for yy in (OP + 0.08, OP + 0.44):
            RP.bolt(b, L, (jx + dx, yy, zb + 0.335), (0, 0, 1), 0.009, 0.006, False)
    _with(b, L, b.tag, "RM-1 POWER FEED", (jx, OP + 0.36, zb + 0.336), (0, 0, 1))
    RP.cyl(b, L, (jx, OP + 0.1, zb + 0.08), (jx, OP + 0.1, zb + 0.0), 0.03, "rubber", "rubber", 8, both=False)
    # the bridge's position strip on the -z cavity wall's inside, just over the water (core columns 1 to 15), on a
    # mounting bar with its fixings
    b.box(C - 1.72, WATER_Y + 0.52, C - H, C + 1.72, WATER_Y + 0.72, C - H + 0.012, "gunmetal")
    for x in (C - 1.68, C + 1.68):
        RP.bolt(b, L, (x, WATER_Y + 0.62, C - H + 0.012), (0, 0, 1), 0.009, 0.006, False)
    picture(b, "SCALE_COL", (C, WATER_Y + 0.62, C - H + 0.012), (0, 0, 1), None)


def end_stop(b, L, xe, zr, s):
    """A rail end stop: its base plate anchored to the floor, the stop block (hazard face toward the bridge, the
    striker plate the bridge's buffers meet), two gussets behind it."""
    x_in = xe + s * 0.3                    # the stop's face toward the bridge
    xa, xb = min(xe, x_in), max(xe, x_in)
    pa, pb = min(xe - s * 0.05, xe + s * 0.33), max(xe - s * 0.05, xe + s * 0.33)
    b.box(pa, OP, zr - 0.33, pb, OP + 0.025, zr + 0.33, "gunmetal")
    for xx in (pa + 0.05, pb - 0.05):
        for zz in (zr - 0.29, zr + 0.29):
            RP.bolt(b, L, (xx, OP + 0.025, zz), (0, 1, 0), 0.013, 0.014, True)
    faces = [YELLOW] * 6
    faces[3 if s > 0 else 2] = "hazard"
    b.box(xa, OP + 0.025, zr - 0.25, xb, OP + 1.05, zr + 0.25, faces)
    fa, fb = (x_in, x_in + 0.03) if s > 0 else (x_in - 0.03, x_in)
    b.box(fa, OP + 0.74, zr - 0.2, fb, OP + 1.02, zr + 0.2, "gunmetal")
    for zz in (zr - 0.16, zr + 0.16):
        for yy in (OP + 0.78, OP + 0.98):
            RP.bolt(b, L, (fb if s > 0 else fa, yy, zz), (s, 0, 0), 0.011, 0.01, False)
    for zz in (zr - 0.18, zr + 0.18):
        c = (xe - s * 0.14, OP + 0.3, zz)
        RP.obox(b, L, c, (s, 0, 0), (0, 1, 0), 0.14, 0.275, 0.012, YELLOW)


# ---------------------------------------------------------------- the bridge (moves along x; its x from the mast's line)

def end_truck(b, L, zr, k):
    """An end truck: a welded box (side webs, top plate, end plates with hazard stripes) with its two double-flanged
    wheels in slots, their axle bearing cartridges on the webs, rail sweeps, the spring buffers on its ends."""
    yb, yt = OP + 0.3, TRUCK[1]
    zi, zo = zr - 0.24, zr + 0.24
    bay = 0.38                          # the wheel bays: the web cut away round each wheel below BAY_TOP
    bay_top = OP + 0.8
    for zw, s in ((zi, -1), (zo, 1)):
        za, zb = (zw - 0.02, zw) if s < 0 else (zw, zw + 0.02)
        for x0, x1 in ((-1.3, -0.85 - bay), (-0.85 + bay, 0.85 - bay), (0.85 + bay, 1.3)):
            b.box(x0, yb, za, x1, yt - 0.03, zb, BLUE, L)
            b.box(x0, yb - 0.02, za, x1, yb, zb, BLUE, L)             # the web's bottom bar
        for xw in (-0.85, 0.85):
            b.box(xw - bay, bay_top, za, xw + bay, yt - 0.03, zb, BLUE, L)
            b.box(xw - bay, bay_top - 0.02, za, xw + bay, bay_top, zb, BLUE, L)
            # the axle box's horn plate down from the bay's top to below the axle
            ha, hb = (za - 0.015, za) if s < 0 else (zb, zb + 0.015)
            b.box(xw - 0.18, RAIL_TOP + 0.16, ha, xw + 0.18, bay_top - 0.02, hb, BLUE, L)
        for x in (-1.265, -0.43, 0.43, 1.265):                       # web stiffeners
            sa, sb = (za - 0.04, za) if s < 0 else (zb, zb + 0.04)
            b.box(x - 0.01, yb + 0.03, sa, x + 0.01, yt - 0.04, sb, BLUE, L)
    b.box(-1.33, yt - 0.03, zi - 0.05, 1.33, yt, zo + 0.05, BLUE, L)
    b.box(-0.55, yb, zi, 0.55, yb + 0.02, zo, BLUE, L)
    for x, s in ((-1.3, -1), (1.3, 1)):
        xa, xb = (x, x + 0.02) if s < 0 else (x - 0.02, x)
        faces = [BLUE] * 6
        faces[2 if s < 0 else 3] = "hazard"
        b.box(xa, yb, zi, xb, yt - 0.03, zo, faces, L)
        RP.spring_buffer(b, L, (x, OP + 0.88, zr), (s, 0, 0), 0.3, 0.085)
        # rail sweep ahead of the outer wheel
        RP.obox(b, L, (x - s * 0.1, RAIL_TOP + 0.1, zr), (1, 0, 0), (0, 1, 0), 0.008, 0.09, 0.07, "gunmetal")
    for xw in (-0.85, 0.85):
        wy = RAIL_TOP + WHEEL_R
        RP.cyl(b, L, (xw, wy, zr - 0.05), (xw, wy, zr + 0.05), WHEEL_R, "carbon_steel", "gunmetal", 20, both=True)
        for zf in (zr - 0.065, zr + 0.05):
            RP.cyl(b, L, (xw, wy, zf), (xw, wy, zf + 0.015), WHEEL_R + 0.035, "carbon_steel", "carbon_steel", 20, both=True)
        RP.cyl(b, L, (xw, wy, zi - 0.09), (xw, wy, zo + 0.09), 0.05, "stainless", "stainless", 10, both=True)
        for zw, s in ((zi - 0.035, -1), (zo + 0.035, 1)):
            RP.cyl(b, L, (xw, wy, zw), (xw, wy, zw + s * 0.06), 0.13, "gunmetal", "gunmetal", 14, both=True)
            RP.bolt_circle(b, L, (xw, wy, zw + s * 0.06), (0, 0, s), 0.1, 6, 0.011, 0.008)
            RP.grease_nipple(b, L, (xw, wy + 0.13, zw + s * 0.03), (0, 1, 0))


def girder(b, L, xg, k):
    """A box girder: top and bottom flanges standing out past its webs, vertical stiffeners both sides, bolted web
    splices, the trolley rail on its clips, the stencils."""
    za, zb = RAIL_Z[0] - 0.24, RAIL_Z[1] + 0.24
    fw = WEB + 0.06
    b.box(xg - fw, G_TOP - 0.03, za, xg + fw, G_TOP, zb, BLUE, L)
    b.box(xg - fw, G_BOT, za, xg + fw, G_BOT + 0.03, zb, BLUE, L)
    b.box(xg - WEB, G_BOT + 0.03, za + 0.01, xg + WEB, G_TOP - 0.03, zb - 0.01, BLUE, L)
    out = -1 if xg < 0 else 1
    splices = (C - 2.25, C + 2.25)
    clear_out = [(C - 6.4, C - 2.5), (C + 2.5, C + 6.4)] + ([(C - 1.8, C + 1.8)] if out < 0 else [])
    for side in (-1, 1):
        xf = xg + side * WEB
        z = za + 0.4
        while z < zb - 0.3:
            skip = any(a <= z <= c for a, c in (clear_out if side == out else [])) or any(abs(z - s) < 0.3 for s in splices)
            if not skip:
                xa, xb = (xf - 0.05, xf) if side < 0 else (xf, xf + 0.05)
                b.box(xa, G_BOT + 0.03, z - 0.006, xb, G_TOP - 0.03, z + 0.006, BLUE, L)
            z += 0.75
    # web splices: a cover plate either side, bolts in four columns
    for zs in splices:
        for side in (-1, 1):
            xf = xg + side * WEB
            xa, xb = (xf - 0.012, xf) if side < 0 else (xf, xf + 0.012)
            b.box(xa, G_BOT + 0.07, zs - 0.2, xb, G_TOP - 0.07, zs + 0.2, BLUE, L)
            for dz in (-0.15, -0.07, 0.07, 0.15):
                for j in range(5):
                    y = G_BOT + 0.12 + j * (G_TOP - G_BOT - 0.24) / 4
                    RP.bolt(b, L, (xb if side > 0 else xa, y, zs + dz), (side, 0, 0), 0.011, 0.008, False)
        b.box(xg - fw + 0.02, G_BOT - 0.012, zs - 0.2, xg + fw - 0.02, G_BOT, zs + 0.2, BLUE, L)
        for dz in (-0.15, -0.07, 0.07, 0.15):
            for dx in (-0.16, 0.16):
                RP.bolt(b, L, (xg + dx, G_BOT - 0.012, zs + dz), (0, -1, 0), 0.011, 0.008, False)
    # the trolley's rail on the top flange: sole bar, rail, clips
    b.box(xg - 0.07, G_TOP, za + 0.25, xg + 0.07, G_TOP + 0.015, zb - 0.25, "gunmetal", L)
    b.box(xg - 0.025, G_TOP + 0.015, za + 0.25, xg + 0.025, T_RAIL - 0.025, zb - 0.25, "stainless_dark", L)
    b.box(xg - 0.04, T_RAIL - 0.025, za + 0.25, xg + 0.04, T_RAIL, zb - 0.25, "stainless_dark", L)
    z = za + 0.5
    while z < zb - 0.4:
        for s in (-1, 1):
            xa, xb = (xg + 0.028, xg + 0.065) if s > 0 else (xg - 0.065, xg - 0.028)
            b.box(xa, G_TOP + 0.015, z - 0.03, xb, G_TOP + 0.03, z + 0.03, "gunmetal", L)
            RP.bolt(b, L, ((xa + xb) / 2, G_TOP + 0.03, z), (0, 1, 0), 0.008, 0.008, False)
        z += 0.5
    # stencils on the outside web
    xf = xg + out * WEB
    n = (out, 0, 0)
    ym = (G_BOT + G_TOP) / 2
    first, second = ("REFUELING MACHINE", "RM-1") if out < 0 else ("RM-1", "REFUELING MACHINE")
    _with(b, L, b.stencil, first, (xf, ym, C - 4.45), n)
    _with(b, L, b.stencil, second, (xf, ym, C + 4.45), n)
    if out < 0:
        b.box(xf - 0.012, G_TOP - 0.2, C - 1.72, xf, G_TOP - 0.04, C + 1.72, "gunmetal", L)       # the row strip's bar
        picture(b, "SCALE_ROW", (xf - 0.012, G_TOP - 0.12, C), n, L)
    # the girder's ends bolted down to the end trucks
    for zr in RAIL_Z:
        for dz in (-0.17, 0.17):
            for dx in (-fw + 0.025, fw - 0.025):
                RP.bolt(b, L, (xg + dx, G_BOT + 0.03, zr + dz), (0, 1, 0), 0.012, 0.01, True)


def bridge_drive(b, L, zr, s, name):
    """A shaft-mounted gear motor on the end truck's outer wheel axle (x 0.85): the reducer on the axle, its torque
    arm to the web, the brake motor along x, the disc brake on the reducer's input shaft, the tag."""
    zo = zr + s * 0.295
    gz = zo + s * 0.13
    gc = (0.85, OP + 0.52, gz)
    RP.gearbox(b, L, gc, (0, 0, 1), 0.11, 0.2, 0.17, bosses=((-0.07, 0.0, -s, 0.08),))
    RP.obox(b, L, (0.85, OP + 0.3, (zo + gz) / 2), (0, 0, 1), (0, 1, 0), abs(gz - zo) / 2 + 0.02, 0.025, 0.04, "gunmetal")
    RP.motor(b, L, (0.85 + 0.17 + 0.0, OP + 0.62, gz), (1, 0, 0), 0.5, 0.12, BLUE, brake=True)
    RP.disc_brake(b, L, (0.85 - 0.2, OP + 0.62, gz), (1, 0, 0), 0.11, side=s)
    RP.cyl(b, L, (0.85 - 0.17, OP + 0.62, gz), (0.85 - 0.24, OP + 0.62, gz), 0.03, "stainless", "stainless", 8, both=True)
    _with(b, L, b.tag, name, (0.85, OP + 0.46, gz + s * 0.111), (0, 0, s))


def bridge(b):
    L = b.live["bridge"]["still"]
    z0, z1 = RAIL_Z
    for k, zr in enumerate(RAIL_Z):
        end_truck(b, L, zr, k)
    for k, xg in enumerate(GIRDER_X):
        girder(b, L, xg, k)
    # cross ties between the girders over the trucks
    for zr in RAIL_Z:
        RP.ibeam(b, L, (GIRDER_X[0] + WEB, G_TOP - 0.25, zr), (GIRDER_X[1] - WEB, G_TOP - 0.25, zr), (0, 1, 0), 0.36, 0.2)
    bridge_drive(b, L, z0, -1, "BR DRIVE 1")
    bridge_drive(b, L, z1, 1, "BR DRIVE 2")
    walkway(b, L)
    cable_reel(b, L)
    festoon_track(b, L)
    # the column pointer and its camera: an arm from truck 1's inner web out over the cavity wall to the strip
    zi = z0 + 0.26
    zp = C - H + 0.045
    ya = OP + 0.38
    RP.obox(b, L, (0, ya, (zi + zp) / 2), (0, 0, 1), (0, 1, 0), (zp - zi) / 2, 0.025, 0.03, "gunmetal")
    b.box(-0.02, WATER_Y + 0.74, zp - 0.02, 0.02, ya - 0.025, zp + 0.02, "gunmetal", L)
    RP.obox(b, L, (0, WATER_Y + 0.74, zp - 0.015), (1, 0, 0), (0, 1, 0), 0.012, 0.035, 0.004, sw("red"))
    b.box(-0.07, ya + 0.025, zp - 0.3, 0.07, ya + 0.16, zp - 0.08, "black", L)                       # camera
    RP.cyl(b, L, (0, ya + 0.09, zp - 0.08), (0, ya + 0.05, zp + 0.0), 0.04, "black", sw("lblue"), 10, both=False)
    RP.conduit(b, L, [(0, ya + 0.16, zp - 0.2), (0, ya + 0.3, zp - 0.2), (0, ya + 0.3, zi + 0.02)], 0.012)


def walkway(b, L):
    """Along girder 1's outside: grating panels in their frames on two channel stringers, brackets with knee braces
    from the girder's web, the handrail with toe plates, the ladder at the -z end, the drive panel at the +z end."""
    wa, wb = -2.1, -1.3
    z0, z1 = RAIL_Z
    za, zb = z0 - 0.75, z1 + 0.75
    n = int(round((zb - za) / 1.25))
    for k in range(n):
        RP.grating_deck(b, L, wa, za + (zb - za) * k / n, wb, za + (zb - za) * (k + 1) / n, G_TOP)
    for x, t in ((wa + 0.02, (1, 0, 0)), (wb - 0.02, (-1, 0, 0))):
        RP.channel(b, L, (x, G_TOP - 0.11, za), (x, G_TOP - 0.11, zb), (0, 1, 0), 0.12, 0.05, t)
    web = GIRDER_X[0] - WEB
    z = za + 0.4
    while z < zb:
        if abs(z - C) < 1.8:            # leave the row strip clear
            z += 1.6
            continue
        RP.obox(b, L, ((wa + web) / 2, G_TOP - 0.2, z), (1, 0, 0), (0, 1, 0), (web - wa) / 2, 0.03, 0.02, "gunmetal")
        if not (2.55 < abs(z - C) < 6.35):          # no knee brace across the stencils: a deep bracket there
            RP.cyl(b, L, (wa + 0.08, G_TOP - 0.23, z), (web - 0.01, G_BOT + 0.12, z), 0.02, "gunmetal", "gunmetal", 6, both=False)
        else:
            RP.gusset(b, L, (web, G_TOP - 0.17, z), (0, -1, 0), (-1, 0, 0), 0.16, 0.016, "gunmetal")
        z += 1.6
    xr = wa + 0.035
    RP.handrail(b, L, [(wb - 0.02, za + 0.03), (xr, za + 0.03), (xr, zb - 0.03), (wb - 0.02, zb - 0.03)], G_TOP,
                gaps=((0, 0.2, 0.75),))
    # ladder off the walkway's -z end (it hangs clear of the floor: the bridge moves)
    RP.ladder(b, L, (wa + wb) / 2, za - 0.1, OP + 0.12, G_TOP, along=(1, 0, 0), standoffs=(G_TOP - 0.06,), standoff_to=0.1,
              wall_dir=(0, 0, 1))
    # the bridge drive panel at the +z end, facing along the walkway; its conduits down to the drives
    cz = zb - 0.06
    RP.cabinet(b, L, ((wa + wb) / 2 - 0.02, G_TOP + 0.13 + 0.6, cz), (0, 0, -1), 0.62, 1.2, 0.3,
               tag=lambda c, nn: _with(b, L, b.tag, "RM-1 BRIDGE DRIVE PANEL", c, nn))
    for x in ((wa + wb) / 2 - 0.25, (wa + wb) / 2 + 0.21):
        b.box(x - 0.025, G_TOP, cz - 0.28, x + 0.025, G_TOP + 0.11, cz - 0.02, "gunmetal", L)
    xc = (wa + wb) / 2 - 0.02
    yw = G_TOP - 0.25                  # under the walkway's brackets
    zt2 = z1 + 0.26 + 0.07             # in front of truck 2's outer web, strapped back to it
    RP.conduit(b, L, [(xc, G_TOP + 0.12, cz - 0.15), (xc, yw, cz - 0.15), (xc, yw, zt2), (xc, TRUCK[1] - 0.1, zt2),
                      (1.18, TRUCK[1] - 0.1, zt2), (1.18, OP + 0.85, z1 + 0.425)], 0.02,
               walls=[None, None, None, ((0, 0, 1), z1 + 0.30), None])
    zt1 = z0 - 0.26 - 0.07
    RP.conduit(b, L, [(xc + 0.08, G_TOP + 0.12, cz - 0.15), (xc + 0.08, yw, cz - 0.15), (xc + 0.08, yw, z1 - 0.4),
                      (GIRDER_X[0], G_BOT - 0.05, z1 - 0.4), (GIRDER_X[0], G_BOT - 0.05, z0 + 0.4),
                      (GIRDER_X[0], TRUCK[1] - 0.1, zt1), (1.18, TRUCK[1] - 0.1, zt1), (1.18, OP + 0.85, z0 - 0.425)], 0.02,
               walls=[None, None, None, ((0, -1, 0), -G_BOT), None, ((0, 0, -1), -(z0 - 0.30)), None])


def cable_reel(b, L):
    """The motor-driven reel on truck 2's outer web that pays the bridge's trailing power cable out into the floor
    trough: drum, flanges, its brackets, gear motor, collector housing, tag."""
    zr = RAIL_Z[1]
    zo = zr + 0.26
    zc = TROUGH_Z
    yc = OP + 0.62
    RP.reel(b, L, (-0.45, yc, zc), (0.05, yc, zc), 0.12, 0.31, 0.24, "rubber", "paint_grey", spring_side=-1)
    for x in (-0.51, 0.11):
        RP.obox(b, L, (x, yc + 0.09, (zo + zc + 0.07) / 2), (0, 0, 1), (0, 1, 0), (zc + 0.07 - zo) / 2, 0.12, 0.012, "paint_grey")
    RP.motor(b, L, (0.13, yc, zc), (1, 0, 0), 0.3, 0.07, BLUE)
    b.box(-0.44, yc + 0.2, zo + 0.04, 0.04, yc + 0.3, zo + 0.06, "paint_grey", L)
    _with(b, L, b.tag, "BRIDGE CABLE REEL", (-0.2, yc + 0.25, zo + 0.06), (0, 0, 1))


def festoon_track(b, L):
    """The trolley's power festoon: a C-track on brackets along girder 2's inside under its top flange (the carriers
    and cable loops are drawn where the trolley is: placed())."""
    xg = GIRDER_X[1]
    xt = xg - WEB - 0.1
    za, zb = RAIL_Z[0] + 0.2, RAIL_Z[1] - 0.1
    b.box(xt - 0.025, G_TOP - 0.16, za, xt + 0.025, G_TOP - 0.1, zb, "gunmetal", L)
    z = RAIL_Z[0] - 0.24 + 0.4 + 0.375        # between the girder's stiffeners
    while z < zb:
        if z > za and all(abs(z - zs) > 0.3 for zs in (C - 2.25, C + 2.25)):
            RP.obox(b, L, ((xg - WEB + xt + 0.025) / 2, G_TOP - 0.08, z), (1, 0, 0), (0, 1, 0), (xg - WEB - xt - 0.025) / 2, 0.02, 0.02, "gunmetal")
        z += 1.5
    RP.conduit(b, L, [(xt, G_TOP - 0.2, zb - 0.1), (xg - WEB + 0.005, G_TOP - 0.2, zb - 0.1)], 0.018)
    RP.cyl(b, L, (xg - WEB, G_TOP - 0.2, zb - 0.1), (xg - WEB - 0.03, G_TOP - 0.2, zb - 0.1), 0.03, "gunmetal", "gunmetal", 8, both=False)


# ---------------------------------------------------------------- the trolley (moves along z; z from the mast's line)

def trolley(b):
    L = b.live["trolley"]["still"]
    lit = b.live["trolley"]["lit"]
    za, zb = T_Z
    hx = 1.25
    # wheel frames: a plate either side of each girder's rail, the wheels between, axle cartridges outside
    for xg in GIRDER_X:
        for s in (-1, 1):
            xp = xg + s * 0.11
            b.box(min(xp, xp + s * 0.016), T_RAIL + 0.05, za + 0.06, max(xp, xp + s * 0.016), DECK - 0.045, zb - 0.06, BLUE, L)
        for zw in (za + 0.45, zb - 0.45):
            wy = T_RAIL + 0.15
            RP.cyl(b, L, (xg - 0.04, wy, zw), (xg + 0.04, wy, zw), 0.15, "carbon_steel", "gunmetal", 16, both=True)
            for xf in (xg - 0.055, xg + 0.04):
                RP.cyl(b, L, (xf, wy, zw), (xf + 0.015, wy, zw), 0.172, "carbon_steel", "carbon_steel", 16, both=True)
            RP.cyl(b, L, (xg - 0.16, wy, zw), (xg + 0.16, wy, zw), 0.03, "stainless", "stainless", 8, both=True)
            for s in (-1, 1):
                xo = xg + s * 0.126
                RP.cyl(b, L, (xo, wy, zw), (xo + s * 0.04, wy, zw), 0.07, "gunmetal", "gunmetal", 12, both=True)
                RP.bolt_circle(b, L, (xo + s * 0.04, wy, zw), (s, 0, 0), 0.05, 4, 0.009, 0.006)
    # end channels and the beams round the mast opening, the deck's skirt
    for z, t in ((za + 0.03, (0, 0, 1)), (zb - 0.03, (0, 0, -1))):
        RP.channel(b, L, (-hx + 0.013, DECK - 0.15, z), (hx - 0.013, DECK - 0.15, z), (0, 1, 0), 0.2, 0.06, t, mat=BLUE)
    for z in (-HOUSING - 0.12, HOUSING + 0.12):
        RP.ibeam(b, L, (-hx + 0.02, DECK - 0.14, z), (hx - 0.02, DECK - 0.14, z), (0, 1, 0), 0.18, 0.12, mat=BLUE)
    for x, s in ((-hx, -1), (hx, 1)):
        xa, xb = (x, x + 0.012) if s < 0 else (x - 0.012, x)
        b.box(xa, DECK - 0.22, za + 0.06, xb, DECK - 0.05, zb - 0.06, BLUE, L)
    # grating deck round the mast's opening
    o = HOUSING + 0.15
    for (x0, z0, x1, z1) in ((-hx, za, hx, -o), (-hx, o, hx, zb), (-hx, -o, -o, o), (o, -o, hx, o)):
        RP.grating_deck(b, L, x0, z0, x1, z1, DECK, edge=BLUE)
    # handrail round the deck, a gate onto the walkway on the -x side (swung open)
    e = hx - 0.04
    RP.handrail(b, L, [(-e, 1.3), (-e, za + 0.04), (e, za + 0.04), (e, zb - 0.04), (-e, zb - 0.04), (-e, 2.25)], DECK)
    for hy in (0.53, 1.07):
        RP.cyl(b, L, (-e, DECK + hy, 2.25), (-e + 0.5, DECK + hy, 2.25), 0.019, YELLOW, YELLOW, 8, both=False)
    RP.cyl(b, L, (-e + 0.5, DECK + 0.45, 2.25), (-e + 0.5, DECK + 1.1, 2.25), 0.019, YELLOW, YELLOW, 8, both=False)
    for hy in (0.4, 0.95):
        RP.cyl(b, L, (-e, DECK + hy, 2.25), (-e, DECK + hy + 0.08, 2.25), 0.03, "gunmetal", "gunmetal", 8, both=True)
    # the step down to the walkway, hung off the -x skirt
    b.box(-1.5, G_TOP + 0.21, 1.35, -hx - 0.012, G_TOP + 0.25, 2.2, ["grating"] * 6, L)
    for z in (1.38, 2.17):
        b.box(-hx - 0.032, G_TOP + 0.25, z - 0.02, -hx - 0.012, DECK - 0.05, z + 0.02, "gunmetal", L)
        b.box(-1.5, G_TOP + 0.19, z - 0.02, -hx - 0.012, G_TOP + 0.21, z + 0.02, "gunmetal", L)
    # trolley drive at the -z end: a brake motor and reducer on the deck, the chain case down to the cross shaft
    tz = za + 0.45
    b.box(0.21, T_RAIL + 0.08, tz - 0.12, 0.29, DECK + 0.38, tz + 0.12, "paint_grey", L)
    RP.gearbox(b, L, (0.41, DECK + 0.21, tz), (1, 0, 0), 0.12, 0.17, 0.12, bosses=((0.04, 0.0, 1, 0.06),))
    b.box(0.29, DECK, tz - 0.14, 0.62, DECK + 0.03, tz + 0.14, "gunmetal", L)
    RP.motor(b, L, (0.53 + 0.035, DECK + 0.25, tz), (1, 0, 0), 0.4, 0.095, BLUE, feet_to=DECK + 0.03, brake=True)
    _with(b, L, b.tag, "TR DRIVE", (0.41, DECK + 0.25, tz + 0.12 + 0.019), (0, 0, 1))
    wy = T_RAIL + 0.15
    RP.cyl(b, L, (GIRDER_X[0] + 0.15, wy, tz), (GIRDER_X[1] - 0.15, wy, tz), 0.03, "stainless", "stainless", 8, both=True)
    for x in (GIRDER_X[0] + 0.2, GIRDER_X[1] - 0.2):
        RP.coupling_guard(b, L, (x - 0.06, wy, tz), (x + 0.06, wy, tz), 0.06)
    # the row pointer reaching down past girder 1's top flange to the strip
    xf = GIRDER_X[0] - WEB
    b.box(-hx + 0.012, DECK - 0.2, -0.02, xf - 0.1, DECK - 0.12, 0.02, "gunmetal", L)
    b.box(xf - 0.13, G_TOP - 0.13, -0.012, xf - 0.1, DECK - 0.2, 0.012, "gunmetal", L)
    b.box(xf - 0.1, G_TOP - 0.13, -0.006, xf - 0.025, G_TOP - 0.11, 0.006, sw("red"), L)
    # the festoon's tow arm, from girder 2's inner wheel plate down beside the track to the lead carrier
    xpl = GIRDER_X[1] - 0.11 - 0.016
    b.box(0.5, T_RAIL + 0.08, -0.97, xpl, T_RAIL + 0.12, -0.93, "gunmetal", L)
    b.box(0.5, G_TOP - 0.2, -0.97, 0.53, T_RAIL + 0.08, -0.93, "gunmetal", L)
    b.box(0.53, G_TOP - 0.2, -0.965, 0.545, G_TOP - 0.17, -0.935, "gunmetal", L)
    air_package(b, L)
    console(b)
    mast_housing(b)


def air_package(b, L):
    """The gripper's air supply on the deck's +x side: a tank-mounted compressor (pump, motor, belt guard) on its
    receiver (saddles, pressure gauge, safety valve, drain), the line to the mast housing."""
    x = 0.86
    z0, z1 = -1.38, -0.58
    y = DECK + 0.24
    r = 0.16
    RP.cyl(b, L, (x, y, z0 + 0.05), (x, y, z1 - 0.05), r, "tank_grey", "tank_grey", 18, both=True)
    for ze, s in ((z0 + 0.05, -1), (z1 - 0.05, 1)):
        RP.cyl(b, L, (x, y, ze), (x, y, ze + s * 0.03), r * 0.86, "tank_grey", "tank_grey", 18, both=True)
        RP.cyl(b, L, (x, y, ze + s * 0.03), (x, y, ze + s * 0.045), r * 0.6, "tank_grey", "tank_grey", 16, both=True)
    for zs in (z0 + 0.17, z1 - 0.17):
        RP.obox(b, L, (x, DECK + 0.054, zs), (1, 0, 0), (0, 1, 0), 0.13, 0.046, 0.025, "gunmetal")
        RP.obox(b, L, (x, DECK + 0.004, zs), (1, 0, 0), (0, 1, 0), 0.15, 0.004, 0.05, "gunmetal")
    # pump and motor on a plate on top
    yp = y + r + 0.01
    b.box(x - 0.13, yp - 0.012, z0 + 0.12, x + 0.13, yp, z1 - 0.12, "gunmetal", L)
    pc = (x, yp + 0.08, z0 + 0.24)
    RP.obox(b, L, pc, (0, 0, 1), (0, 1, 0), 0.08, 0.08, 0.08, "paint_grey")
    for k in range(5):
        RP.obox(b, L, (x, yp + 0.17 + k * 0.018, z0 + 0.24), (0, 0, 1), (0, 1, 0), 0.07, 0.005, 0.07, "paint_grey")
    RP.obox(b, L, (x, yp + 0.27, z0 + 0.24), (0, 0, 1), (0, 1, 0), 0.075, 0.012, 0.075, "gunmetal")
    RP.motor(b, L, (x - 0.02, yp + 0.08, z1 - 0.38), (0, 0, 1), 0.24, 0.065, BLUE, feet_to=yp, fins=8)
    RP.obox(b, L, (x + 0.1, yp + 0.09, (z0 + z1) / 2 - 0.05), (0, 0, 1), (0, 1, 0), 0.2, 0.08, 0.025, YELLOW)   # belt guard
    _with(b, L, b.tag, "AIR COMPRESSOR", (x + 0.126, yp + 0.11, (z0 + z1) / 2 - 0.05), (1, 0, 0))
    # gauge, safety valve, drain, the line to the housing
    g = (x - 0.1, y + 0.12, z1 - 0.1)
    RP.cyl(b, L, (x - 0.06, y + 0.12, z1 - 0.1), g, 0.012, "copper", "copper", 6, both=False)
    RP.cyl(b, L, g, (g[0] - 0.02, g[1], g[2]), 0.045, "gunmetal", ["dial", 0.02, 0.02, 0.98, 0.98], 14, both=False)
    RP.cyl(b, L, (x, y + r, z1 - 0.2), (x, y + r + 0.1, z1 - 0.2), 0.016, "copper", "copper", 8, both=True)
    RP.cyl(b, L, (x, y - r, z0 + 0.4), (x, DECK + 0.02, z0 + 0.4), 0.012, "copper", "copper", 6, both=True)
    RP.conduit(b, L, [(x - 0.12, y + 0.05, z1 - 0.05), (x - 0.12, y + 0.05, -0.2), (HOUSING + 0.0, y + 0.05, -0.2)], 0.014, "copper")


def console(b):
    """The operator's console on the trolley's +z end, facing +z: the operator stands at its front with the mast
    beyond it. Pedestal with its door, the sloped panel in a bezel (readouts, lamps, controls), the camera monitor
    (hooded) on a post behind, the conduit to the mast housing."""
    L = b.live["trolley"]["still"]
    lit = b.live["trolley"]["lit"]
    xa, xb = -0.55, 0.55
    zb0, zf = 1.4, 1.95
    yt = DECK + 0.85
    b.box(xa, DECK + 0.08, zb0, xb, yt, zf, BLUE, L)
    b.box(xa + 0.03, DECK, zb0 + 0.03, xb - 0.03, DECK + 0.08, zf - 0.03, "black", L)                 # kick plinth
    # pedestal door on the operator's side: proud panel, hinges, handle, louvres
    RP.obox(b, L, (0, (DECK + 0.1 + yt - 0.08) / 2, zf + 0.007), (1, 0, 0), (0, 1, 0), 0.5, (yt - DECK - 0.18) / 2, 0.007, BLUE)
    for yy in (DECK + 0.25, yt - 0.2):
        RP.cyl(b, L, (xa + 0.05, yy - 0.04, zf + 0.016), (xa + 0.05, yy + 0.04, zf + 0.016), 0.01, "gunmetal", "gunmetal", 8, both=True)
    RP.cyl(b, L, (xb - 0.08, DECK + 0.5, zf + 0.014), (xb - 0.08, DECK + 0.5, zf + 0.03), 0.016, "black", "black", 8, both=False)
    RP.obox(b, L, (xb - 0.08, DECK + 0.46, zf + 0.034), (1, 0, 0), (0, 1, 0), 0.008, 0.04, 0.005, "black")
    lc = (0.0, DECK + 0.22, zf + 0.0145)
    b.quad((-0.3, lc[1] + 0.05, lc[2]), (-0.3, lc[1] - 0.05, lc[2]), (0.3, lc[1] - 0.05, lc[2]), (0.3, lc[1] + 0.05, lc[2]), ["louvre", 0.05, 0.05, 0.95, 0.95], L)
    # the sloped panel: front edge low, back edge high
    yf, yb = yt, yt + 0.32
    b.quad((xb, yb, zb0), (xb, yf, zf), (xa, yf, zf), (xa, yb, zb0), "paint_grey", L)
    for x, s in ((xa, -1), (xb, 1)):
        if s < 0:
            b.quad((x, yb, zb0), (x, yf, zb0), (x, yf, zf), (x, yf, zf), BLUE, L)
        else:
            b.quad((x, yb, zb0), (x, yf, zf), (x, yf, zf), (x, yf, zb0), BLUE, L)
    b.quad((xa, yb, zb0), (xa, yf, zb0), (xb, yf, zb0), (xb, yb, zb0), BLUE, L)
    sl = _unit((0, yb - yf, zb0 - zf))
    n = _unit(_cross((1, 0, 0), sl))
    if n[1] < 0:
        n = tuple(-c for c in n)
    u = _right(n)
    P = lambda a, s: (u[0] * a, yf + sl[1] * s, zf + sl[2] * s)      # a across, s up the slope from the front edge
    slope_len = math.dist((yf, zf), (yb, zb0))
    # the bezel round the panel: a raised rim on its four edges
    for s0 in (0.012, slope_len - 0.012):
        c = P(0, s0)
        RP.obox(b, L, tuple(c[i] + n[i] * 0.008 for i in range(3)), u, sl, 0.55, 0.012, 0.008, "gunmetal")
    for a in (-0.538, 0.538):
        c = P(a, slope_len / 2)
        RP.obox(b, L, tuple(c[i] + n[i] * 0.008 for i in range(3)), u, sl, 0.012, slope_len / 2 - 0.024, 0.008, "gunmetal")
    _with(b, L, b.tag, "REFUELING MACHINE RM-1", P(0, slope_len - 0.07), n, up=(0, 1, 0))
    reads = [("BRIDGE  COL", "bridge", 2), ("TROLLEY  ROW", "trolley", 1), ("MAST  Z  IN", "mast_z", 4), ("LOAD  LB", "load", 4)]
    for k, (lab, key, nch) in enumerate(reads):
        a = (-0.39 + 0.26 * k)
        c = P(a, slope_len - 0.2)
        b.obox(tuple(c[i] + n[i] * 0.006 for i in range(3)), u, sl, 0.11, 0.04, 0.006, ["black", None, "black", "black", "black", "black"], L)
        face = tuple(c[i] + n[i] * 0.012 for i in range(3))
        b.live["readouts"].append({"name": key, "centre": [round(v, 4) for v in face], "normal": [round(v, 4) for v in n], "char_h": 0.055, "chars": nch})
        _with(b, L, b.tag, lab, P(a, slope_len - 0.135), n)
    for k, (lab, col, key) in enumerate([("GRIPPER ENGAGED", "green", "engaged"), ("GRIPPER RELEASED", "white", "released"),
                                         ("OVERLOAD", "red", "overload"), ("SLACK CABLE", "amber", "slack")]):
        a = -0.39 + 0.26 * k
        c = P(a, 0.33)
        cc = tuple(c[i] + n[i] * 0.02 for i in range(3))
        b.cyl(c, cc, 0.034, "stainless", "gunmetal", 12, lst=L)                          # the lamp's chrome ring
        lens_at = tuple(c[i] + n[i] * 0.03 for i in range(3))
        b.live["lamps"][key] = {"a": [round(v, 4) for v in cc], "b": [round(v, 4) for v in lens_at], "r": 0.026, "colour": col}
        _with(b, L, b.tag, lab, P(a, 0.265), n)
    for k, (lab, kind) in enumerate([("HOIST", "lever"), ("BRIDGE", "stick"), ("TROLLEY", "stick"), ("GRIPPER", "key"), ("E-STOP", "stop")]):
        a = -0.42 + 0.21 * k
        c = P(a, 0.1)
        if kind == "stop":
            RP.cyl(b, L, c, tuple(c[i] + n[i] * 0.012 for i in range(3)), 0.055, sw("yellow"), sw("yellow"), 14, both=False)
            b.cyl(c, tuple(c[i] + n[i] * 0.03 for i in range(3)), 0.03, "black", "black", 10, lst=L)
            b.cyl(tuple(c[i] + n[i] * 0.03 for i in range(3)), tuple(c[i] + n[i] * 0.06 for i in range(3)), 0.045, sw("red"), sw("red"), 12, lst=L)
        elif kind == "key":
            b.cyl(c, tuple(c[i] + n[i] * 0.02 for i in range(3)), 0.03, "stainless_dark", "stainless_dark", 10, lst=L)
            b.obox(tuple(c[i] + n[i] * 0.035 for i in range(3)), u, sl, 0.006, 0.02, 0.015, "black", L)
        else:
            RP.cyl(b, L, c, tuple(c[i] + n[i] * 0.006 for i in range(3)), 0.05, "gunmetal", "gunmetal", 12, both=False)
            b.cyl(c, tuple(c[i] + n[i] * 0.02 for i in range(3)), 0.038, "black", "black", 10, lst=L)          # rubber gaiter
            tip = tuple(c[i] + n[i] * 0.075 + (sl[i] * 0.015 if kind == "lever" else 0) for i in range(3))
            b.cyl(tuple(c[i] + n[i] * 0.02 for i in range(3)), tip, 0.009, "stainless_dark", "stainless_dark", 6, lst=L)
            b.cyl(tip, tuple(tip[i] + n[i] * 0.035 for i in range(3)), 0.018, "black", "black", 8, lst=L)
    for k, lab in enumerate(["HOIST", "BRIDGE", "TROLLEY", "GRIPPER", "E-STOP"]):
        a = -0.42 + 0.21 * k
        _with(b, L, b.tag, lab, (u[0] * a, yf - 0.05, zf + 0.014), (0, 0, 1))
    # the monitor: a case with a bezel and sun hood on a post behind the panel; its screen faces the operator
    mz0, mz1 = 1.12, 1.38
    b.box(-0.07, DECK + 0.015, 1.2, 0.07, yb + 0.02, 1.3, "gunmetal", L)
    b.box(-0.13, DECK, 1.13, 0.13, DECK + 0.015, 1.37, "gunmetal", L)
    for dx in (-0.1, 0.1):
        for dz in (1.16, 1.34):
            RP.bolt(b, L, (dx, DECK + 0.015, dz), (0, 1, 0), 0.008, 0.006, False)
    b.box(-0.29, yb + 0.02, mz0, 0.29, yb + 0.47, mz1, "black", L)
    ys0, ys1 = yb + 0.245 - 0.165, yb + 0.245 + 0.165
    for (x0, y0, x1, y1) in ((-0.27, ys1, 0.27, ys1 + 0.025), (-0.27, ys0 - 0.025, 0.27, ys0), (-0.27, ys0, -0.22, ys1), (0.22, ys0, 0.27, ys1)):
        b.box(x0, y0, mz1, x1, y1, mz1 + 0.01, "gunmetal", L)
    b.box(-0.305, yb + 0.47, mz1 - 0.02, 0.305, yb + 0.485, mz1 + 0.1, "black", L)                   # hood top
    for x in (-0.305, 0.29):
        b.box(x, yb + 0.25, mz1 - 0.02, x + 0.015, yb + 0.47, mz1 + 0.1, "black", L)
    picture(b, "TV_CORE", (0, yb + 0.245, mz1), (0, 0, 1), lit, lift=0.002)
    _with(b, L, b.tag, "MAST CAMERA", (0, yb + 0.045, mz1 + 0.0), (0, 0, 1))
    RP.conduit(b, L, [(0.25, DECK + 0.05, zb0), (0.25, DECK + 0.05, HOUSING + 0.14)], 0.022)


def mast_housing(b):
    """The stationary mast housing on the trolley: its walls with corner angles and seams, the framed window, the
    access door, the electrical cabinet and its cable tray, the ladder; the rotation ring and lock at the deck; the
    top platform with its handrail, the mast hoist (drum with its ropes, reducer, brake motor, second brake, limit
    switch) on its frame on a pivot and a load cell, the hose and cable reels with their guide rollers, the beacon;
    the outer mast (flanged sections, guide-roller housings, the camera and lights at its foot) into the water."""
    L = b.live["trolley"]["still"]
    lit = b.live["trolley"]["lit"]
    h = HOUSING
    y0, y1 = DECK + 0.14, HOUSING_TOP
    win = (DECK + 1.5, DECK + 2.95)
    # walls (the +z wall with the window)
    b.box(-h, y0, -h, h, y1, -h + 0.04, BLUE, L)
    b.box(-h, y0, -h + 0.04, -h + 0.04, y1, h - 0.04, BLUE, L)
    b.box(h - 0.04, y0, -h + 0.04, h, y1, h - 0.04, BLUE, L)
    b.box(-h, y0, h - 0.04, h, win[0], h, BLUE, L)
    b.box(-h, win[1], h - 0.04, h, y1, h, BLUE, L)
    b.box(-h, win[0], h - 0.04, -h + 0.08, win[1], h, BLUE, L)
    b.box(h - 0.08, win[0], h - 0.04, h, win[1], h, BLUE, L)
    b.box(-h + 0.08, win[0], h - 0.02, h - 0.08, win[0] + 0.04, h + 0.02, "gunmetal", L)
    b.box(-h + 0.08, win[1] - 0.04, h - 0.02, h - 0.08, win[1], h + 0.02, "gunmetal", L)
    b.box(-0.02, win[0] + 0.04, h - 0.02, 0.02, win[1] - 0.04, h + 0.02, "gunmetal", L)
    for xa, xb in ((-h + 0.08, -0.02), (0.02, h - 0.08)):
        b.quad((xb, win[1] - 0.04, h - 0.01), (xb, win[0] + 0.04, h - 0.01), (xa, win[0] + 0.04, h - 0.01), (xa, win[1] - 0.04, h - 0.01),
               ["glass", 0.05, 0.05, 0.95, 0.95], L)
    b.quad((-h + 0.04, y1, -h + 0.04), (-h + 0.04, y0, -h + 0.04), (h - 0.04, y0, -h + 0.04), (h - 0.04, y1, -h + 0.04), sw("dgrey"), L)
    # corner angles and the seams between the wall panels
    for sx in (-1, 1):
        for sz in (-1, 1):
            b.box(min(sx * (h - 0.01), sx * (h + 0.022)), y0 + 0.03, min(sz * (h - 0.01), sz * (h + 0.022)),
                  max(sx * (h - 0.01), sx * (h + 0.022)), y1 - 0.005, max(sz * (h - 0.01), sz * (h + 0.022)), BLUE, L)
    for ys in (DECK + 0.6, DECK + 3.15):
        b.box(-h - 0.01, ys - 0.02, -h - 0.01, h + 0.01, ys + 0.02, h + 0.01, BLUE, L)
    # the access door on the -z wall
    dz = -h - 0.012
    RP.obox(b, L, (0, DECK + 1.1, -h - 0.006), (1, 0, 0), (0, 1, 0), 0.24, 0.42, 0.006, BLUE)
    for yy in (DECK + 0.8, DECK + 1.4):
        RP.cyl(b, L, (-0.24, yy - 0.05, dz), (-0.24, yy + 0.05, dz), 0.012, "gunmetal", "gunmetal", 8, both=True)
    RP.cyl(b, L, (0.18, DECK + 1.1, dz), (0.18, DECK + 1.1, dz - 0.02), 0.016, "black", "black", 8, both=False)
    RP.obox(b, L, (0.18, DECK + 1.06, dz - 0.024), (1, 0, 0), (0, 1, 0), 0.008, 0.045, 0.005, "black")
    _with(b, L, b.tag, "ACCESS DOOR", (0, DECK + 1.4, dz), (0, 0, -1))
    # electrical cabinet on the +x wall and the cable tray from it up to the top platform
    RP.cabinet(b, L, (h + 0.022, DECK + 1.4, 0), (1, 0, 0), 0.56, 1.0, 0.22,
               tag=lambda c, nn: _with(b, L, b.tag, "RM-1 MAST ELEC CAB", c, nn), hinge_left=False)
    tx = h + 0.022
    b.box(tx, DECK + 1.9, -0.07, tx + 0.06, y1, -0.06, "gunmetal", L)
    b.box(tx, DECK + 1.9, 0.06, tx + 0.06, y1, 0.07, "gunmetal", L)
    b.box(tx, DECK + 1.9, -0.06, tx + 0.006, y1, 0.06, "gunmetal", L)
    for k in range(3):
        RP.cyl(b, L, (tx + 0.025, DECK + 1.92, -0.03 + k * 0.03), (tx + 0.025, y1, -0.03 + k * 0.03), 0.012, "black", "black", 6, both=False)
    # ladder up the -x wall to the top platform
    RP.ladder(b, L, -0.92, 0.0, DECK, y1 + 0.1, along=(0, 0, 1), half=0.22, top_ext=1.1,
              standoffs=(DECK + 1.2, y1 - 0.25), standoff_to=0.56 - 0.022, wall_dir=(1, 0, 0))
    # rotation ring at the deck (bearing ring, indexing ring with its holes), the housing's base flange, the lock pin
    RP.cyl(b, L, (0, DECK, 0), (0, DECK + 0.07, 0), 0.62, "gunmetal", "gunmetal", 28, both=False)
    RP.cyl(b, L, (0, DECK + 0.07, 0), (0, y0, 0), 0.58, "stainless_dark", "stainless_dark", 28, both=False)
    for k in range(16):
        a = 2 * math.pi * (k + 0.5) / 16
        p = (0.545 * math.cos(a), y0, 0.545 * math.sin(a))
        if abs(p[0]) < h + 0.07 and abs(p[2]) < h + 0.07:
            continue
        RP.cyl(b, L, p, (p[0], y0 + 0.002, p[2]), 0.018, "black", "black", 8, both=False)
    b.box(-h - 0.06, y0 - 0.006, -h - 0.06, h + 0.06, y0 + 0.03, h + 0.06, BLUE, L)
    for k in range(4):
        for t in (-0.3, 0.0, 0.3):
            ang = [(1, 0), (0, 1), (-1, 0), (0, -1)][k]
            p = (ang[0] * (h + 0.04) + (t if ang[0] == 0 else 0), y0 + 0.03, ang[1] * (h + 0.04) + (t if ang[1] == 0 else 0))
            RP.bolt(b, L, p, (0, 1, 0), 0.011, 0.008, False)
    lp = (0.545 * math.cos(math.radians(22.5)), y0, 0.545 * math.sin(math.radians(22.5)))
    RP.cyl(b, L, lp, (lp[0], y0 + 0.24, lp[2]), 0.016, "stainless", "stainless", 8, both=False)
    RP.cyl(b, L, (lp[0] - 0.07, y0 + 0.24, lp[2]), (lp[0] + 0.07, y0 + 0.24, lp[2]), 0.014, sw("red"), sw("red"), 8, both=True)
    _with(b, L, b.tag, "ROTATION LOCK", (h + 0.022, DECK + 0.38, 0.0), (1, 0, 0))
    # lifting lugs at the top corners
    for sx in (-1, 1):
        RP.obox(b, L, (sx * (h - 0.12), y1 - 0.12, -h - 0.02), (1, 0, 0), (0, 1, 0), 0.05, 0.07, 0.02, BLUE)
        RP.cyl(b, L, (sx * (h - 0.12), y1 - 0.1, -h - 0.041), (sx * (h - 0.12), y1 - 0.1, -h - 0.042), 0.02, "black", "black", 8, both=False)
    top_platform(b, L, lit)
    outer_mast(b, L, lit)


def top_platform(b, L, lit):
    h = HOUSING
    y1 = HOUSING_TOP
    px, pz = 0.8, 1.25
    yp = y1 + 0.1
    o = 0.22
    for (x0, z0, x1, z1) in ((-px, -pz, px, -o), (-px, o, px, pz), (-px, -o, -o, o), (o, -o, px, o)):
        b.box(x0, y1, z0, x1, yp, z1, "gunmetal", L)
    e = 0.04
    RP.handrail(b, L, [(-px + e, 0.32), (-px + e, pz - e), (px - e, pz - e), (px - e, -pz + e), (-px + e, -pz + e), (-px + e, -0.32)], yp)
    # the hoist's frame: two channels on the pivot (-x end) and the load cell (+x end)
    yf0 = yp + 0.06
    yf = yf0 + 0.1
    for zc, t in ((-0.26, (0, 0, 1)), (0.26, (0, 0, -1))):
        RP.channel(b, L, (-0.62, yf0 + 0.05, zc), (0.78, yf0 + 0.05, zc), (0, 1, 0), 0.1, 0.05, t, mat=YELLOW)
    for xc in (-0.6, 0.76):
        RP.channel(b, L, (xc, yf0 + 0.05, -0.25), (xc, yf0 + 0.05, 0.25), (0, 1, 0), 0.08, 0.04, (1, 0, 0) if xc < 0 else (-1, 0, 0), mat=YELLOW)
    for zc in (-0.26, 0.26):
        b.box(-0.6, yp, zc - 0.05, -0.5, yf0, zc + 0.05, "gunmetal", L)                                  # pivot pedestal
        RP.cyl(b, L, (-0.55, yf0 + 0.02, zc - 0.07), (-0.55, yf0 + 0.02, zc + 0.07), 0.025, "stainless", "stainless", 8, both=True)
    RP.cyl(b, L, (0.7, yp, 0), (0.7, yf0, 0), 0.05, "stainless", "stainless", 14, both=False)            # load cell
    b.box(0.58, yp, 0.36, 0.74, yp + 0.12, 0.5, "black", L)
    RP.cyl(b, L, (0.7, yp + 0.03, 0.05), (0.7, yp + 0.03, 0.36), 0.008, "black", "black", 6, both=False)
    _with(b, L, b.tag, "LOAD CELL", (0.66, yp + 0.12, 0.43), (0, 1, 0), up=(1, 0, 0))
    # drum on its pillow blocks, the ropes wound on it
    dy = yf + 0.26
    for x in (-0.38, 0.38):
        b.box(x - 0.07, yf, -0.25, x + 0.07, yf + 0.02, 0.25, "gunmetal", L)
        RP.pillow_block(b, L, (x, dy, 0), (1, 0, 0), 0.045, base_y=yf + 0.02)
    RP.cyl(b, L, (-0.56, dy, 0), (0.48, dy, 0), 0.045, "stainless", "stainless", 10, both=True)
    RP.rope_drum(b, L, (-0.3, dy, 0), (0.3, dy, 0), 0.17, 0.011, (0.06, 0.4))
    t = 0.6 * 0.6
    while t < 0.6 * 0.94:
        RP.cyl(b, L, (-0.3 + t - 0.011, dy, 0), (-0.3 + t + 0.011, dy, 0), 0.181, "stainless_dark", "stainless_dark", 16, both=False)
        t += 0.0253
    # the reducer on the drum shaft's +x end, the brake motor back along the drum, the second brake and the limit switch
    gc = (0.6, dy, 0.12)
    RP.gearbox(b, L, gc, (1, 0, 0), 0.12, 0.24, 0.28, bosses=((0.0, -0.12, -1, 0.08), (0.0, 0.26, -1, 0.07)))
    b.box(0.46, yf, -0.2, 0.74, yf + 0.02, 0.44, "gunmetal", L)
    RP.motor(b, L, (0.6 - 0.12 - 0.035, dy, 0.38), (-1, 0, 0), 0.62, 0.12, BLUE, feet_to=yf, brake=True, terminal_side=1)
    _with(b, L, b.tag, "MAST HOIST", (0.6, dy + 0.24, 0.12), (0, 1, 0), up=(1, 0, 0))
    RP.disc_brake(b, L, (-0.47, dy, 0), (1, 0, 0), 0.15, side=-1)
    lsx = -0.6
    RP.obox(b, L, (lsx, dy, 0), (1, 0, 0), (0, 1, 0), 0.04, 0.07, 0.06, "paint_grey")
    RP.bolt_row(b, L, (lsx - 0.04, dy + 0.05, -0.04), (lsx - 0.04, dy + 0.05, 0.04), (-1, 0, 0), 2, 0.007, 0.004)
    RP.bolt_row(b, L, (lsx - 0.04, dy - 0.05, -0.04), (lsx - 0.04, dy - 0.05, 0.04), (-1, 0, 0), 2, 0.007, 0.004)
    b.box(lsx - 0.03, yf, -0.05, lsx + 0.03, dy - 0.07, 0.05, "gunmetal", L)
    # reels with their stands, guide rollers at the opening
    for zc, mat, name in ((-0.86, "rubber", "AIR HOSE REEL"), (0.86, "black", "CABLE REEL")):
        yc = yp + 0.33
        RP.reel(b, L, (-0.28, yc, zc), (0.28, yc, zc), 0.09, 0.26, 0.2, mat, "paint_grey", spring_side=1)
        for xs in (-0.33, 0.33):
            RP.obox(b, L, (xs, (yp + yc) / 2 + 0.03, zc), (0, 0, 1), (0, 1, 0), 0.14, (yc - yp) / 2 + 0.03, 0.012, "paint_grey")
            RP.cyl(b, L, (xs - 0.02, yc, zc), (xs + 0.02, yc, zc), 0.05, "gunmetal", "gunmetal", 10, both=True)
        _with(b, L, b.tag, name, (0.56, yp, zc), (0, 1, 0), up=(1, 0, 0))
        zr_ = -o - 0.04 if zc < 0 else o + 0.04
        for dy_ in (0.03, 0.09):
            RP.cyl(b, L, (-0.1, yp + dy_, zr_), (0.1, yp + dy_, zr_), 0.022, "stainless", "stainless", 10, both=True)
        for xs in (-0.12, 0.12):
            b.box(xs - 0.015, yp, zr_ - 0.03, xs + 0.015, yp + 0.12, zr_ + 0.03, "gunmetal", L)
    # beacon
    bx, bz = -0.62, 1.08
    b.box(bx - 0.08, yp, bz - 0.08, bx + 0.08, yp + 0.02, bz + 0.08, "gunmetal", L)
    RP.cyl(b, L, (bx, yp + 0.02, bz), (bx, yp + 0.12, bz), 0.07, "gunmetal", "gunmetal", 10, both=False)
    b.cyl((bx, yp + 0.12, bz), (bx, yp + 0.3, bz), 0.06, "lamp_amber", "lamp_amber", 10, lst=lit)
    b.cyl((bx, yp + 0.3, bz), (bx, yp + 0.32, bz), 0.065, "gunmetal", "gunmetal", 10, lst=L)
    b.live["beacon"] = [bx, yp + 0.21, bz]
    b.live["cables"] = {"hoist": [[-0.3 + 0.6 * 0.23, dy - 0.181, 0.0], [-0.3 + 0.6 * 0.77, dy - 0.181, 0.0]],
                        "hose": [[0.0, yp + 0.33 - 0.2, -0.86], [0.0, yp + 0.06, -o - 0.04 + 0.022]],
                        "cable": [[0.0, yp + 0.33 - 0.2, 0.86], [0.0, yp + 0.06, o + 0.04 - 0.022]]}


def outer_mast(b, L, lit):
    o = OUTER
    b.box(-o, MAST_BOT, -o, o, DECK, o, ["stainless", "stainless", "stainless", "stainless", None, None], L)
    # bolted flanges between its sections
    y = DECK - 0.5
    while y > MAST_BOT + 1.2:
        b.box(-o - 0.035, y - 0.03, -o - 0.035, o + 0.035, y + 0.03, o + 0.035, "stainless_dark", L)
        for k in range(4):
            nx, nz = [(1, 0), (0, 1), (-1, 0), (0, -1)][k]
            for t in (-0.2, -0.07, 0.07, 0.2):
                p = (nx * (o + 0.035) + (t if nx == 0 else 0), y, nz * (o + 0.035) + (t if nz == 0 else 0))
                RP.bolt(b, L, p, (nx, 0, nz), 0.01, 0.008, False)
        y -= 2.6
    # guide-roller housings near the foot, the skirt
    for k in range(4):
        nx, nz = [(1, 0), (0, 1), (-1, 0), (0, -1)][k]
        c = (nx * (o + 0.03), MAST_BOT + 0.62, nz * (o + 0.03))
        RP.obox(b, L, c, (nz, 0, -nx) if nz or nx else (1, 0, 0), (0, 1, 0), 0.12, 0.13, 0.03, "stainless_dark")
        for sy in (-1, 1):
            for sx in (-1, 1):
                p = (c[0] + nx * 0.03 + nz * sx * 0.09, c[1] + sy * 0.1, c[2] + nz * 0.03 - nx * sx * 0.09)
                RP.bolt(b, L, p, (nx, 0, nz), 0.009, 0.006, False)
    b.box(-o - 0.04, MAST_BOT, -o - 0.04, o + 0.04, MAST_BOT + 0.22, o + 0.04, ["stainless_dark"] * 4 + [None, None], L)
    i = o - 0.02
    yb0, yb1 = MAST_BOT, MAST_BOT + 1.0
    for c0, c1 in (((i, -i), (-i, -i)), ((i, i), (i, -i)), ((-i, i), (i, i)), ((-i, -i), (-i, i))):
        b.quad((c0[0], yb1, c0[1]), (c0[0], yb0, c0[1]), (c1[0], yb0, c1[1]), (c1[0], yb1, c1[1]), sw("dgrey"), L)
    # underwater lights on the +x and -x faces, the camera on the -z face, their cables up the mast
    yl = MAST_BOT + 1.05
    for s in (-1, 1):
        x = s * (o + 0.13)
        RP.obox(b, L, (s * (o + 0.06), yl + 0.1, 0), (1, 0, 0), (0, 1, 0), 0.06, 0.02, 0.03, "stainless_dark")
        RP.cyl(b, L, (x, yl + 0.16, 0), (x, yl - 0.08, 0), 0.075, "stainless", "gunmetal", 14, both=True)
        b.cyl((x, yl - 0.08, 0), (x, yl - 0.1, 0), 0.06, "lamp_white", pic("lamp_white"), 14, lst=lit)
        RP.cyl(b, L, (s * (o + 0.012), yl + 0.2, 0.12), (s * (o + 0.012), DECK - 0.1, 0.12), 0.012, "black", "black", 6, both=False)
    zc = -(o + 0.08)
    RP.obox(b, L, (0, yl + 0.1, -(o + 0.03)), (1, 0, 0), (0, 1, 0), 0.04, 0.02, 0.03, "stainless_dark")
    RP.cyl(b, L, (0, yl + 0.18, zc), (0, yl - 0.1, zc), 0.05, "stainless", ["glass", 0.2, 0.2, 0.8, 0.8], 12, both=True)
    RP.cyl(b, L, (0.1, yl + 0.2, -(o + 0.012)), (0.1, DECK - 0.1, -(o + 0.012)), 0.012, "black", "black", 6, both=False)


# ---------------------------------------------------------------- the inner mast and gripper (y from the gripper's bite)

def inner_mast(b):
    L = b.live["mast"]["still"]
    lit = b.live["mast"]["lit"]
    i = INNER
    # gripper housing on the bite, then the mast tube
    b.box(-0.06, 0.0, -0.06, 0.06, 0.15, 0.06, "stainless_dark", L)
    b.box(-0.09, 0.15, -0.09, 0.09, 0.3, 0.09, "stainless_dark", L)
    b.box(-0.13, 0.3, -0.13, 0.13, 0.42, 0.13, "stainless_dark", L)
    b.box(-i, 0.42, -i, i, INNER_L, i, "stainless", L)
    # guide rollers' axles on the mast's corners every couple of metres (they run in the outer mast)
    y = 1.2
    while y < INNER_L - 0.4:
        for sx, sz in ((1, 1), (-1, -1)):
            b.cyl((sx * i, y, sz * (i - 0.06)), (sx * (i + 0.035), y, sz * (i - 0.06)), 0.045, "gunmetal", "gunmetal", 8, lst=L)
        y += 2.0
    # top: the lifting frame (a plate on support bars), two eyebolts, the swivel bar; the air cylinder; limit switches
    yt = INNER_L
    for x in (-0.12, 0.12):
        b.box(x - 0.02, yt, -0.12, x + 0.02, yt + 0.22, -0.08, "gunmetal", L)
        b.box(x - 0.02, yt, 0.08, x + 0.02, yt + 0.22, 0.12, "gunmetal", L)
    b.box(-0.16, yt + 0.22, -0.16, 0.16, yt + 0.27, 0.16, "gunmetal", L)
    b.box(-0.15, yt + 0.27, -0.03, 0.15, yt + 0.31, 0.03, "stainless_dark", L)                      # swivel bar
    for x in (-0.12, 0.12):
        b.cyl((x, yt + 0.31, 0), (x, yt + 0.36, 0), 0.012, "stainless_dark", "stainless_dark", 6, lst=L)
        b.cyl((x - 0.03, yt + 0.39, 0), (x + 0.03, yt + 0.39, 0), 0.03, "stainless_dark", "stainless_dark", 8, lst=L)  # eye
    b.cyl((0, yt, 0), (0, yt + 0.2, 0), 0.07, "stainless", "gunmetal", 12, lst=L)                     # air cylinder
    b.cyl((0, yt + 0.2, 0), (0, yt + 0.21, 0), 0.075, "gunmetal", "gunmetal", 12, lst=L)
    for z in (-0.15, 0.15):       # the gripper's limit switches
        b.box(-0.03, yt, z - 0.02, 0.03, yt + 0.08, z + 0.02, "paint_grey", L)
    b.live["mast_top"] = {"eyes": [[-0.12, yt + 0.42, 0.0], [0.12, yt + 0.42, 0.0]], "hose": [[0.0, yt + 0.21, -0.05]],
                          "cable": [[0.0, yt + 0.21, 0.05]]}
    # gripper: four fingers (engaged: down into the nozzle; released: drawn up), two guide pins
    for state, dy in (("engaged", 0.0), ("released", 0.07)):
        G = b.live["gripper"][state]
        for sx, sz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            c = (sx * 0.06, -0.035 + dy, sz * 0.06)
            ux = (1, 0, 0) if sx else (0, 0, 1)
            b.obox(c, ux, (0, 1, 0), 0.02, 0.045, 0.02, "gunmetal", G)
    for sx, sz in ((1, 1), (-1, -1)):
        b.cyl((sx * 0.068, 0.15, sz * 0.068), (sx * 0.068, -0.06, sz * 0.068), 0.012, "stainless_dark", "stainless_dark", 6, lst=L)


# ---------------------------------------------------------------- a fuel assembly (y from its foot) and the core

def fuel_assembly(b, L):
    """A 17x17 assembly: bottom nozzle with its legs, the rod bundle (a metre of rods and grids a texture tile), the
    top nozzle (its top: flow holes, alignment holes, hold-down spring clamps) and its two hold-down leaf springs."""
    w = FUEL_HW
    for sx, sz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        b.box(sx * w - (0.03 if sx > 0 else 0), 0.0, sz * w - (0.03 if sz > 0 else 0),
              sx * w + (0.03 if sx < 0 else 0), 0.05, sz * w + (0.03 if sz < 0 else 0), "stainless", L)
    b.box(-w, 0.05, -w, w, 0.12, w, "stainless", L)
    y = 0.12
    top = FUEL_L - 0.1
    k = 0
    while y < top - 1e-6:
        y2 = min(top, y + 1.0)
        f = (y2 - y)
        m = [RH.LEGEND] + legend("FUEL_SIDE")[1:]
        m = [m[0], m[1], m[2], m[3], m[2] + (m[4] - m[2]) * f]
        for (p, q) in (((-w, -w), (w, -w)), ((w, -w), (w, w)), ((w, w), (-w, w)), ((-w, w), (-w, -w))):
            # a side quad from p to q; its normal (q - p) x down must point out, else go from q to p
            n = (q[1] - p[1], 0, -(q[0] - p[0]))
            if n[0] * (p[0] + q[0]) + n[2] * (p[1] + q[1]) < 0:
                p, q = q, p
            b.quad((p[0], y2, p[1]), (p[0], y, p[1]), (q[0], y, q[1]), (q[0], y2, q[1]), m, L)
        y = y2
        k += 1
    b.box(-w, top, -w, w, FUEL_L, w, ["stainless", "stainless", "stainless", "stainless", None, "stainless"], L)
    b.quad((-w, FUEL_L, w), (-w, FUEL_L, -w), (w, FUEL_L, -w), (w, FUEL_L, w), legend("NOZZLE_TOP"), L)
    for sx, sz in ((1, -1), (-1, 1)):
        x0, z0 = sx * (w - 0.04), sz * (w - 0.04)
        b.box(min(x0, x0 + sx * 0.035), FUEL_L, min(z0, z0 + sz * 0.035), max(x0, x0 + sx * 0.035), FUEL_L + 0.025,
              max(z0, z0 + sz * 0.035), "stainless_dark", L)


def core(b, missing=()):
    """The core's top inside the vessel as it is while refuelling (head and upper internals off): the 193 assemblies'
    top nozzles, a metre of each one's side (seen down a gap), the baffle round them, the lit water under (Cherenkov
    glow of the spent core), the vessel's inner wall and flange face."""
    p = CORE_PITCH
    w = FUEL_HW
    y0 = CORE_TOP - 1.0
    cells = set()
    for row, (c0, c1) in CORE_MAP.items():
        for col in range(c0, c1 + 1):
            cells.add((row, col))
    for i, row in enumerate(CORE_ROWS):
        for col in range(1, 16):
            x, z = cell_xz(row, col)
            if (row, col) in cells:
                if (row, col) in missing:
                    continue
                m = legend("FUEL_SIDE")
                b.box(x - w, y0, z - w, x + w, CORE_TOP, z + w, [m, m, m, m, legend("NOZZLE_TOP"), None])
            elif math.hypot(x - C, z - C) < 2.15:
                b.box(x - p / 2, y0 + 0.9, z - p / 2, x + p / 2, CORE_TOP - 0.06, z + p / 2,
                      ["stainless_dark"] * 4 + ["stainless_dark", None])           # baffle filler
    b.ring_top(7.5 * p, 2.2, CORE_TOP - 0.07, "stainless_dark")                      # baffle and barrel top
    # the glow under: a lit floor below the nozzles (only seen down the gaps and an empty cell)
    e = 7.5 * p
    b.quad((C - e, y0 + 0.02, C + e), (C - e, y0 + 0.02, C - e), (C + e, y0 + 0.02, C - e), (C + e, y0 + 0.02, C + e), "cherenkov", b.lit)
    b.tube(2.2, CORE_TOP - 0.07, RH.FLANGE_TOP, "stainless", inward=True)
    b.ring_top(2.2, RH.R_FLANGE, RH.FLANGE_TOP, "flange")


def build():
    b = Machine()
    rails(b)
    b.core = len(b.still)
    bridge(b)
    trolley(b)
    inner_mast(b)
    fuel_assembly(b, b.live["fuel"]["still"])
    return b


def moved(ops, dx, dy, dz):
    out = []
    for op in ops:
        o = list(op)
        k = o[0]
        if k in ("b", "c"):
            o[1] += dx; o[4] += dx; o[2] += dy; o[5] += dy; o[3] += dz; o[6] += dz
        elif k == "o":
            o[1] += dx; o[2] += dy; o[3] += dz
        elif k == "q":
            for i in (1, 4, 7, 10):
                o[i] += dx; o[i + 1] += dy; o[i + 2] += dz
        out.append(o)
    return out


def placed(b, bridge_x, trolley_z, bite_y, carrying=True, engaged=True, readings=None, lamps=("engaged",), with_rails=True):
    """The machine for a still picture: the bridge at bridge_x (the mast's line), the trolley at trolley_z, the gripper's
    bite at bite_y, an assembly in the gripper; the readouts (name -> text) and the lit lamps as the client draws them."""
    lv = b.live
    out = {"still": list(b.still) if with_rails else [], "lit": list(b.lit) if with_rails else []}
    out["still"] += moved(lv["bridge"]["still"], bridge_x, 0, 0)
    out["lit"] += moved(lv["bridge"]["lit"], bridge_x, 0, 0)
    out["still"] += moved(lv["trolley"]["still"], bridge_x, 0, trolley_z)
    out["lit"] += moved(lv["trolley"]["lit"], bridge_x, 0, trolley_z)
    out["still"] += moved(lv["mast"]["still"], bridge_x, bite_y, trolley_z)
    out["still"] += moved(lv["gripper"]["engaged" if engaged else "released"], bridge_x, bite_y, trolley_z)
    if carrying:
        out["still"] += moved(lv["fuel"]["still"], bridge_x, bite_y - FUEL_L, trolley_z)
    # cables and hoses from the top plate down to the inner mast's top
    T = lambda p, dy=0: (p[0] + bridge_x, p[1] + dy, p[2] + trolley_z)
    for a, c in zip(lv["cables"]["hoist"], lv["mast_top"]["eyes"]):
        out["still"].append(["c", *CB._r(*T(a), *T(c, bite_y), 0.012), 6, "stainless_dark", "stainless_dark"])
    for key, mat in (("hose", "rubber"), ("cable", "black")):
        pts = [T(p) for p in lv["cables"][key]] + [T(lv["mast_top"][key][0], bite_y)]
        for p, q in zip(pts, pts[1:]):
            out["still"].append(["c", *CB._r(*p, *q, 0.022), 6, mat, mat])
    # the bridge's trailing cable: from its reel down into the floor trough and along it to the feed box
    if with_rails:
        xr = bridge_x - 0.2
        pts = [(xr, OP + 0.62 - 0.24, TROUGH_Z), (xr, OP + 0.03, TROUGH_Z), (FEED_X, OP + 0.03, TROUGH_Z), (FEED_X, OP + 0.1, TROUGH_Z + 0.18)]
        for p, q in zip(pts, pts[1:]):
            out["still"].append(["c", *CB._r(*p, *q, 0.028), 8, "rubber", "rubber"])
    # the trolley's festoon: carriers on the track between the tow arm and the track's +z end, the cable's loops
    xt = bridge_x + GIRDER_X[1] - WEB - 0.1
    yt = G_TOP - 0.18
    z_a, z_b = trolley_z - 0.95, RAIL_Z[1] - 0.1
    n = 7
    zs = [z_a + (z_b - z_a) * k / (n - 1) for k in range(n)]
    for z in zs:
        out["still"].append(["b", *CB._r(xt - 0.03, yt - 0.04, z - 0.04, xt + 0.03, yt + 0.02, z + 0.04), "gunmetal"])
    sag = min(0.42, 0.1 + abs(z_b - z_a) / (n - 1) * 0.25)
    for za_, zb_ in zip(zs, zs[1:]):
        m = (za_ + zb_) / 2
        for p, q in (((xt, yt - 0.04, za_), (xt, yt - sag, m)), ((xt, yt - sag, m), (xt, yt - 0.04, zb_))):
            out["still"].append(["c", *CB._r(*p, *q, 0.016), 6, "black", "black"])
    for name, text in (readings or {}).items():
        for r in lv["readouts"]:
            if r["name"] == name:
                c = T(r["centre"])
                out["lit"] += led_ops(c, r["normal"], r["char_h"], text.rjust(r["chars"]))
    lens = {"green": "lamp_green", "white": "lamp_white", "red": "lamp_red", "amber": "lamp_amber"}
    off = {"green": sw("green"), "white": sw("enamel"), "red": sw("red"), "amber": sw("orange")}
    for key, lp in lv["lamps"].items():
        on = key in lamps
        mat = pic(lens[lp["colour"]]) if on else off[lp["colour"]]
        out["lit" if on else "still"].append(["c", *CB._r(*T(lp["a"]), *T(lp["b"]), lp["r"]), 12, "gunmetal", mat])
    return out


def ops_check():
    b = build()
    bad = len(CB.overlaps(b.ops()))
    for g in ("bridge", "trolley", "mast"):
        bad += len(CB.overlaps({"still": b.live[g]["still"], "lit": b.live[g]["lit"], "blink": []}))
    c = RH.Head()
    core(c, missing=(("K", 11),))
    bad += len(CB.overlaps(c.ops()))
    o = placed(b, 0.6, C, G_UP)
    bad += len(CB.overlaps({"still": o["still"], "lit": o["lit"], "blink": []}))
    return bad


if __name__ == "__main__":
    print("overlaps", ops_check())
