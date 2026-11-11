"""The emergency diesel generator set and its starting air system (round 31; Justin: "rebuild the whole diesel room,
generator included", "Bigger than real": a V20 set about 21 x 10, "Starting air system ... Yes, it works"), drawn from
their own parts the way tools/hvac_boards.py draws the control room's ventilation plant: operations replayed by
client/ControlBoards.java in the equipment sheet's materials, every nameplate, sign and gauge face on the live boards'
legend texture (tools/board_live.py, 512 texels a block) so it reads close up.

The set (10 wide, 21 deep, 7 tall; frame as the boards': x to the right, y up, z away from whoever placed it), its free
end toward them:

- the foundation: the concrete plinth, two I-beam base rails on their anti-vibration mounts, holding-down bolts;
- the engine, a twenty-cylinder vee: the bedplate and oil pan, the crankcase with ten doors a side (each with its
  explosion relief valve and its bolts), the camshaft galleries, the two banks at 45 degrees, twenty heads with their
  rocker covers and cap nuts, the injection pumps on the shoulders with their high-pressure lines up to the heads, the
  fuel supply and return headers and the governor's control shafts, the charge air manifolds and their branches into
  the banks, the jacket water outlet headers over the heads, the starting air headers and the air start valves, the
  lagged exhaust manifold in the vee;
- the free end: the gear case, the engine-driven jacket water and lube oil pumps, the governor and its linkage, the
  overspeed trip, the engine's plate; two turbochargers on the charge air coolers, the exhaust across to them and out
  of the top through the expansion bellows into the silencer, hung from the roof, and on up through the roof thimble;
  the duplex fuel filter; the engine gauge board (its six gauges and two lamps the game's);
- the drive end: the flywheel housing and the barring gear, the coupling's guard (the coupling the game's, turning);
- the generator: its frame and ribs, feet, the cooling air hood, the terminal box and its conduits down into the
  floor, the nameplate and the set's sign by train, the outboard bearing, the exciter and the fan's cowl and guard
  (the fan the game's, turning);
- the walkways along both sides at the heads (grating decks on their posts, toe boards, handrails) and the steps down
  from each beside the generator (half a block each, as the half-block frames under them).

What the game draws (live): the coupling and the fan turning at the shaft (AX, AY) at the set's speed, the gauge
needles and the two lamps, the exhaust smoke out of the stack over the roof thimble."""
import contextlib
import math

import board_live as L
import control_boards as C
import hvac_boards as H
from control_boards import sw, pic, _r

W, D, T = 10, 21, 7
AX, AY = 5.0, 2.5               # the crankshaft and the generator's shaft, along z
BANK = math.radians(22.5)       # each bank's lean from upright (a 45 degree vee)
PITCH = 0.98                    # the cylinders' spacing along each bank
CYL0 = 2.85                     # the first cylinder's middle (z)
CYLS = [CYL0 + k * PITCH for k in range(10)]
ENGINE_Z = (2.0, 12.6)          # the crankcase, front to back
DECK = 2.0                      # the walkways' decks (Justin: "Decks at 2.0"; frames under them at 2.0)
DECK_Z = (2.0, 12.0)
STEPS = [(12.0, 1.5), (13.0, 1.0), (14.0, 0.5)]   # each tread's z and height, down to the floor at 15
GEN_Z = (14.4, 18.6)
GEN_R = 2.1
STACK = (5.0, 1.45)             # the exhaust's x, z: the silencer, the pipe up through the roof thimble
ROOF = 13.0                     # the diesel room's ceiling over the set (tools/medium_plant.py diesel_building)

GREEN, PAINT, GEN = "engine_green", "paint_green", "generator"
YELLOW = "paint_yellow"
DARK, MID, BLACK, WHITE = sw("dgrey"), sw("mgrey"), sw("black"), sw("white")
STEEL = "carbon_steel"

_S, _C = math.sin(BANK), math.cos(BANK)

# The diesel room's own legend texture (textures/entity/diesel_legends.png): its plates, signs and gauge faces, packed
# as the live boards' are, apart from theirs.
ATLAS = L.Atlas()


@contextlib.contextmanager
def own_atlas():
    keep = L.ATLAS
    L.ATLAS = ATLAS
    try:
        yield
    finally:
        L.ATLAS = keep


def bank(side, d, o=0.0):
    """A point in a bank's cross-section: d along its axis out of the crank, o across it (outward positive); side -1 the
    left bank, +1 the right."""
    return (AX + side * _S * d + side * _C * o, AY + _C * d - _S * o)


def axis(side):
    return (side * _S, _C, 0.0)


def ob(b, centre, u, hu, hz, hw, faces, lst=None):
    """An oriented box: its middle, its long side along u (in the cross-section, half hu), hz along z, hw across."""
    b.obox(centre, u, (0, 0, 1), hu, hz, hw, faces, lst)


def bank_box(b, side, d0, d1, o, hw, z0, z1, faces, lst=None):
    """A box standing on a bank: from d0 to d1 along its axis, centred o across it, hw half its width across."""
    x, y = bank(side, (d0 + d1) / 2, o)
    ob(b, (x, y, (z0 + z1) / 2), axis(side), (d1 - d0) / 2, (z1 - z0) / 2, hw, faces, lst)


def rev(b, a, c, profile, mat, cap_a=None, cap_b=None, sides=16, lst=None):
    """A turned part from a to c: its radius along the way ([distance, radius] pairs), capped at either end in
    cap_a / cap_b (none: open). Client: Frame.revolve and its discs."""
    (b.still if lst is None else lst).append(["r", *_r(*a, *c), [_r(t, r) for t, r in profile], sides, mat, cap_a, cap_b])


def cyl(b, a, c, r, mat, cap=None, sides=10):
    b.cyl(a, c, r, mat, cap if cap is not None else mat, sides)


def joint(b, c, r, mat=DARK):
    """A pipe's rounded elbow at c (as the renderer's sphere joint)."""
    x, y, z = c
    rev(b, (x, y - r, z), (x, y + r, z), [(0, 0.001), (r * 0.3, r * 0.8), (r, r), (r * 1.7, r * 0.8), (2 * r, 0.001)], mat, sides=10)


def run(b, pts, r, mat, joints=None, sides=10):
    """A pipe along pts (each leg capped, the bends rounded)."""
    pts = [p for k, p in enumerate(pts) if k == 0 or math.dist(p, pts[k - 1]) > 1e-6]
    for p, q in zip(pts, pts[1:]):
        H.pipe(b, p, q, r, mat, sides=sides)
    for p in pts[1:-1]:
        joint(b, p, r * 1.04, joints or mat)


def bolts(b, centre, r, n, br=0.022, length=0.03, mat=MID, axis_="-z", phase=0.0):
    """n bolt heads round a circle of radius r about centre, standing length out along axis_ ("x", "y", "z", signed)."""
    sgn = -1 if axis_.startswith("-") else 1
    ax = axis_[-1]
    cx, cy, cz = centre
    for k in range(n):
        a = 2 * math.pi * k / n + phase
        ca, sa = math.cos(a) * r, math.sin(a) * r
        if ax == "z":
            p = (cx + ca, cy + sa, cz)
            q = (p[0], p[1], cz + sgn * length)
        elif ax == "x":
            p = (cx, cy + ca, cz + sa)
            q = (cx + sgn * length, p[1], p[2])
        else:
            p = (cx + ca, cy, cz + sa)
            q = (p[0], cy + sgn * length, p[2])
        b.cyl(p, q, br, mat, mat, 6)


def on_floor(b):
    """Drops the bottom face of every box standing on the floor (y 0): never seen, and in the floor's own plane."""
    lists = [b.still] + [st["still"] for st in b.states.values()]
    for lst in lists:
        for op in lst:
            if op[0] == "b" and op[2] <= 0.0:
                faces = op[7]
                if isinstance(faces, str) or (isinstance(faces, list) and len(faces) == 5 and isinstance(faces[1], (int, float))):
                    faces = [faces] * 6
                op[7] = list(faces[:5]) + [None]


def plate(text, w, h, style="white", size=None):
    return L.plate_tex(text, w, h, style, size)


def sign(b, side, xf, z0, z1, y0, y1, mat, edge=WHITE, d=0.012):
    """A plate on a side (side -1 facing -x from xf; +1 facing +x)."""
    H.side_box(b, side, xf, d, y0, y1, z0, z1, mat, edge)


def front_sign(b, zf, x0, x1, y0, y1, mat, edge=WHITE, d=0.012):
    """A plate on a front (facing -z from zf)."""
    b.box(x0, y0, zf - d, x1, y1, zf, [mat, None, edge, edge, edge, edge])


def gauge(b, live, name, c, r, face, facing=(0, 0, -1)):
    """A round gauge facing -z (its black case standing out of whatever it is on toward the viewer, the dial behind
    its glass, the needle the game's: live, swept from 225 degrees round to -45 from empty to full)."""
    x, y, z = c
    b.cyl((x, y, z), (x, y, z - 0.05), r, BLACK, BLACK, 14)
    s = r * 0.68
    zf = z - 0.054
    b.quad((x - s, y + s, zf), (x - s, y - s, zf), (x + s, y - s, zf), (x + s, y + s, zf), face)
    live["gauges"].append({"name": name, "c": _r(x, y, zf - 0.003), "len": _r(s * 0.82)[0], "n": [0, 0, -1]})


# ------------------------------------------------------------------------------------------------ the foundation

def foundation(b):
    b.box(2.2, 0, 0.75, 7.8, 0.12, 20.5, "concrete")
    for x0 in (2.55, 6.95):
        x1 = x0 + 0.5
        # The I-beam: its flanges and web on rubber mounts (black pads every two blocks).
        b.box(x0, 0.2, 0.85, x1, 0.26, 20.3, STEEL)
        b.box(x0 + 0.21, 0.26, 0.85, x1 - 0.21, 0.64, 20.3, STEEL)
        b.box(x0, 0.64, 0.85, x1, 0.7, 20.3, STEEL)
        for z in (1.0, 3.2, 5.4, 7.6, 9.8, 12.0, 14.2, 16.4, 18.6):
            b.box(x0 + 0.03, 0.12, z, x1 - 0.03, 0.2, z + 0.5, "rubber")


def foundation_detail(b):
    for x0 in (2.55, 6.95):
        x1 = x0 + 0.5
        for z in (1.0, 3.2, 5.4, 7.6, 9.8, 12.0, 14.2, 16.4, 18.6):
            for bx in (x0 + 0.08, x1 - 0.08):
                for bz in (z + 0.1, z + 0.4):
                    b.cyl((bx, 0.26, bz), (bx, 0.31, bz), 0.025, MID, MID, 6)
        # Stiffeners in the web every block.
        for z in range(2, 20):
            for sx in (x0 + 0.02, x1 - 0.21):
                b.box(sx, 0.26, z - 0.015, sx + 0.19, 0.64, z + 0.015, STEEL)


# ------------------------------------------------------------------------------------------------ the engine

def engine(b):
    z0, z1 = ENGINE_Z
    b.box(2.6, 0.7, z0 - 0.05, 7.4, 1.45, z1 + 0.05, GREEN)              # bedplate
    b.box(3.1, 0.25, 2.3, 6.9, 0.7, 12.3, GREEN)                          # oil pan
    b.box(2.95, 1.45, z0, 7.05, 3.05, z1, GREEN)                          # crankcase
    for side in (-1, 1):
        # The bank's cylinder block, its heads and rocker covers.
        inset = 0.06 if side < 0 else 0.07
        bank_box(b, side, 0.5, 1.55, 0.0, 0.5, z0 + inset, z1 - inset, GREEN)
        for zc in CYLS:
            bank_box(b, side, 1.55, 1.9, 0.0, 0.47, zc - 0.42, zc + 0.42, PAINT)
            bank_box(b, side, 1.9, 2.18, 0.0, 0.36, zc - 0.34, zc + 0.34, "rocker")
        # The camshaft gallery along the side, under the shoulder.
        gx = 2.78 if side < 0 else 7.05
        b.box(gx, 2.62, z0 + 0.1, gx + 0.17, 3.05, z1 - 0.1, GREEN)
    # The exhaust manifold in the vee (lagged), forward to the turbochargers.
    H.pipe(b, (AX, 4.85, 1.5), (AX, 4.85, 12.15), 0.27, "insulation", sw("white"), 14)


def engine_detail(b):
    z0, z1 = ENGINE_Z
    for side in (-1, 1):
        xf = 2.95 if side < 0 else 7.05
        for k, zc in enumerate(CYLS):
            # The crankcase door: its plate, the bolts round it, the explosion relief valve's cover.
            H.side_box(b, side, xf, 0.025, 2.06, 2.56, zc - 0.33, zc + 0.33, PAINT, DARK)
            xo = xf + side * 0.025
            for t in range(6):
                bz = zc - 0.27 + t * 0.108
                for by in (2.11, 2.51):
                    b.cyl((xo, by, bz), (xo + side * 0.022, by, bz), 0.016, MID, MID, 6)
            for by in (2.21, 2.31, 2.41):
                for bz in (zc - 0.29, zc + 0.29):
                    b.cyl((xo, by, bz), (xo + side * 0.022, by, bz), 0.016, MID, MID, 6)
            b.cyl((xo, 2.31, zc), (xo + side * 0.07, 2.31, zc), 0.15, PAINT, DARK, 14)
            b.cyl((xo + side * 0.07, 2.31, zc), (xo + side * 0.1, 2.31, zc), 0.11, DARK, MID, 12)
            # The gallery's inspection cover for this cylinder.
            gx = 2.78 if side < 0 else 7.22
            H.side_box(b, side, gx, 0.012, 2.68, 2.98, zc - 0.3, zc + 0.3, PAINT, DARK)
            # The head's cap nuts and indicator cock; the rocker cover's hold-down handwheel.
            for o in (-0.36, 0.36):
                for dz in (-0.33, 0.33):
                    x, y = bank(side, 1.9, o)
                    xb, yb = bank(side, 1.97, o)
                    b.cyl((x, y, zc + dz), (xb, yb, zc + dz), 0.035, MID, MID, 6)
            x, y = bank(side, 2.18)
            xb, yb = bank(side, 2.24)
            b.cyl((x, y, zc), (xb, yb, zc), 0.05, DARK, DARK, 8)
            xc, yc = bank(side, 2.24)
            xd, yd = bank(side, 2.27)
            b.cyl((xc, yc, zc), (xd, yd, zc), 0.1, sw("red"), sw("red"), 10)
            x, y = bank(side, 1.72, 0.47)
            xb, yb = bank(side, 1.72, 0.56)
            b.cyl((x, y, zc + 0.3), (xb, yb, zc + 0.3), 0.025, "stainless", "stainless", 6)
            b.cyl((xb, yb, zc + 0.3), (xb, yb + 0.06, zc + 0.3), 0.012, sw("red"), sw("red"), 6)
            injection(b, side, zc)
            branches(b, side, zc)
        headers(b, side)
    # The lagging's bands on the exhaust manifold, its supports off the heads' inner faces.
    for z in [1.9 + k * 0.98 for k in range(11)]:
        b.cyl((AX, 4.85, z), (AX, 4.85, z + 0.05), 0.285, sw("enamel"), sw("enamel"), 14)
    for zc in CYLS[::3]:
        b.box(AX - 0.06, 4.35, zc + 0.43, AX + 0.06, 4.6, zc + 0.53, DARK)


def injection(b, side, zc):
    """A cylinder's injection pump on the shoulder, its rack lever off the control shaft, its high-pressure line up
    and in to the head; its fuel supply and return stubs from the headers."""
    s = side
    px = 3.11 if s < 0 else 6.89
    b.box(px - 0.13, 3.05, zc - 0.13, px + 0.13, 3.55, zc + 0.13, "gunmetal")
    b.box(px - 0.15, 3.55, zc - 0.15, px + 0.15, 3.62, zc + 0.15, DARK)
    b.cyl((px, 3.62, zc - 0.03), (px, 3.72, zc - 0.03), 0.045, "stainless", "stainless", 8)
    # The high-pressure line: up, across over the manifold, into the head's outer face.
    hx, hy = bank(s, 1.764, 0.47)
    zl = zc - 0.03
    run(b, [(px, 3.72, zl), (px, 3.95, zl), (hx, 3.95, zl)], 0.02, "stainless", joints="stainless", sides=6)
    # The rack's lever from the control shaft (at 2.86 / 7.14, y 3.7).
    sx = 2.86 if s < 0 else 7.14
    b.box(min(sx, px + s * 0.13), 3.66, zc + 0.08, max(sx, px + s * 0.13), 3.69, zc + 0.11, MID)
    # The supply and return stubs out of the pump's side to the headers below it.
    for hy_ in (3.15, 3.32):
        b.cyl((px + s * 0.13, hy_, zc + 0.06), (sx, hy_, zc + 0.06), 0.02, "pipe_yellow", "pipe_yellow", 6)


def branches(b, side, zc):
    """A cylinder's charge air branch into its bank, its jacket water outlet up to the header, its air start valve and
    line, its exhaust branch up into the manifold in the vee."""
    s = side
    mx = 3.55 if s < 0 else 6.45
    ix, iy = bank(s, 1.3, 0.5)
    H.pipe(b, (mx, 3.33, zc), (ix, iy, zc), 0.12, GREEN, both=False)
    H.flange(b, ((mx + ix) / 2 + s * 0.06, (3.33 + iy) / 2 - 0.0, zc), "x", 0.1, 0.03)
    # Jacket water out of the head's top outer corner up to the header (green).
    jx, jy = bank(s, 1.85, 0.4)
    hx = 3.62 if s < 0 else 6.38
    run(b, [(jx, jy, zc + 0.18), (hx, jy + 0.05, zc + 0.18), (hx, 4.28, zc + 0.18)], 0.045, "pipe_green", joints=DARK, sides=8)
    # The air start valve on the head's outer face and its line from the header (light blue).
    ax_, ay_ = bank(s, 1.82, 0.47)
    vx, vy = bank(s, 1.82, 0.6)
    b.cyl((ax_, ay_, zc + 0.3), (vx, vy, zc + 0.3), 0.05, DARK, DARK, 8)
    sx = 3.3 if s < 0 else 6.7
    run(b, [(sx, 4.12, zc + 0.3), (vx, 4.12, zc + 0.3), (vx, vy, zc + 0.3)], 0.025, "pipe_lblue", joints="pipe_lblue", sides=6)
    # The exhaust branch out of the head's inner face up into the manifold.
    ex, ey = bank(s, 1.75, -0.47)
    H.pipe(b, (ex, ey, zc), (AX - s * 0.1, 4.65, zc), 0.11, "insulation", both=False)
    b.cyl((ex - s * 0.02, ey - 0.03, zc), (ex + s * 0.03, ey + 0.08, zc), 0.15, "flange", "flange", 10)


def headers(b, side):
    """The headers along a bank: the charge air manifold on the shoulder, the fuel supply and return and the governor's
    control shaft outboard of the pumps, the jacket water outlet header over the heads, the starting air header."""
    s = side
    z0, z1 = 0.45, 12.3
    mx = 3.55 if s < 0 else 6.45
    H.pipe(b, (mx, 3.33, z0), (mx, 3.33, z1), 0.27, GREEN, sides=14)
    for z in (2.35, 5.3, 8.25, 11.2):
        b.box(mx - 0.2, 3.05, z, mx + 0.2, 3.12, z + 0.12, DARK)
    sx = 2.86 if s < 0 else 7.14
    for y, mat in ((3.15, "pipe_yellow"), (3.32, "pipe_yellow")):
        H.pipe(b, (sx, y, 1.6), (sx, y, 12.2), 0.045, mat, both=True)
    b.cyl((sx, 3.7, 1.45), (sx, 3.7, 12.2), 0.025, "stainless", "stainless", 6)
    for z in (2.4, 6.3, 10.2):
        b.box(sx - 0.05, 3.05, z, sx + 0.05, 3.72, z + 0.06, DARK)
    hx = 3.62 if s < 0 else 6.38
    H.pipe(b, (hx, 4.28, 1.9), (hx, 4.28, 12.1), 0.09, "pipe_green", sides=10)
    ax_ = 3.3 if s < 0 else 6.7
    H.pipe(b, (ax_, 4.12, 2.2), (ax_, 4.12, 12.4), 0.06, "pipe_lblue", sides=8)
    for z in (3.4, 7.3, 11.2):
        b.box(ax_ - 0.03, 3.62, z, ax_ + 0.03, 4.06, z + 0.05, DARK)


# ------------------------------------------------------------------------------------------------ the free end

COOLERS = ((2.6, 4.1), (5.9, 7.4))       # the charge air coolers' x (each under its turbocharger)
COOLER_Y, COOLER_Z = (3.7, 5.0), (0.25, 1.15)
TURBO_Y = 5.6
TURBOS = (3.35, 6.65)


def free_end(b):
    b.box(3.0, 0.72, 1.7, 7.0, 3.05, 2.0, GREEN)                          # gear case
    for (x0, x1) in COOLERS:
        b.box(x0, COOLER_Y[0], COOLER_Z[0], x1, COOLER_Y[1], COOLER_Z[1], GREEN)
    for x in TURBOS:
        # Filter-silencer, compressor, bearing housing, turbine.
        rev(b, (x, TURBO_Y, 0.04), (x, TURBO_Y, 0.5), [(0, 0.5), (0.05, 0.56), (0.41, 0.56), (0.46, 0.5)], "stainless", sw("enamel"), None, 16)
        rev(b, (x, TURBO_Y, 0.5), (x, TURBO_Y, 0.97), [(0, 0.4), (0.1, 0.52), (0.36, 0.52), (0.47, 0.3)], "turbo", None, None, 16)
        H.pipe(b, (x, TURBO_Y, 0.97), (x, TURBO_Y, 1.16), 0.28, "gunmetal", sides=12)
        rev(b, (x, TURBO_Y, 1.16), (x, TURBO_Y, 1.76), [(0, 0.32), (0.1, 0.5), (0.5, 0.5), (0.6, 0.36)], "turbo", None, DARK, 16)
    # The exhaust: the manifold's front across to both turbines and up into them; out of their tops, together over
    # the middle, up through the bellows into the silencer, and on up through the roof thimble.
    sx, sz = STACK
    run(b, [(TURBOS[0], 5.15, 1.5), (TURBOS[0], 4.85, 1.5), (TURBOS[1], 4.85, 1.5), (TURBOS[1], 5.15, 1.5)], 0.24, "insulation", joints=sw("enamel"), sides=12)
    for x in TURBOS:
        run(b, [(x, 6.05, 1.46), (x, 6.75, 1.46), (sx, 6.75, 1.46)], 0.26, "insulation", joints=sw("enamel"), sides=12)
    H.pipe(b, (sx, 6.75, sz), (sx, 7.02, sz), 0.26, "insulation", sides=12)
    H.pipe(b, (sx, 7.43, sz), (sx, 7.6, sz), 0.26, "insulation", sides=12)
    rev(b, (sx, 7.55, sz), (sx, 10.45, sz), [(0, 0.3), (0.35, 1.0), (2.55, 1.0), (2.9, 0.3)], "insulation", sw("enamel"), sw("enamel"), 20)
    H.pipe(b, (sx, 10.4, sz), (sx, ROOF - 0.02, sz), 0.3, "insulation", sides=12)


def free_end_detail(b):
    # The gear case's bolts; the engine's plate on it.
    for x in [3.15 + k * 0.37 for k in range(11)]:
        for y in (0.82, 2.93):
            b.cyl((x, y, 1.7), (x, y, 1.675), 0.02, MID, MID, 6)
    front_sign(b, 1.7, 4.35, 5.65, 2.2, 2.8, plate("20-CYLINDER 4-CYCLE DIESEL ENGINE|9400 KW AT 514 RPM|SERIAL NO. 77-2041", 1.3, 0.6, "white", 30))
    # The jacket water pump (green) and the lube oil pump (brown), their suctions down into the floor.
    for x, y, r, mat, cap in ((3.9, 1.25, 0.36, "pipe_green", sw("green")), (6.1, 1.2, 0.3, "pipe_brown", sw("dgrey"))):
        rev(b, (x, y, 1.7), (x, y, 1.0), [(0, r * 0.7), (0.08, r), (0.6, r), (0.7, r * 0.6)], GREEN, None, GREEN, 16)
        bolts(b, (x, y, 1.08), r * 0.88, 10, 0.02, 0.03, MID)
        run(b, [(x, y, 1.0), (x, y, 0.55), (x, 0.0, 0.55)], 0.13, mat, joints=DARK, sides=10)
        H.flange(b, (x, y, 0.85), "z", 0.13)
        H.flange(b, (x, 0.35, 0.55), "y", 0.13)
        b.cyl((x, 0.0, 0.55), (x, 0.04, 0.55), 0.24, sw("red"), sw("red"), 12)
        # The discharge up into the gear case.
        dx = 0.2 if x < AX else -0.2
        run(b, [(x + dx, y + r * 0.8, 1.35), (x + dx, y + r + 0.32, 1.35), (x + dx, y + r + 0.32, 1.7)], 0.11, mat, joints=DARK, sides=10)
    # The governor on the gear case (its drum, the speed-setting motor), its linkage across to both control shafts.
    b.box(4.6, 3.05, 1.2, 5.4, 3.75, 1.7, "gunmetal")
    b.cyl((5.0, 3.75, 1.45), (5.0, 4.08, 1.45), 0.2, "gunmetal", DARK, 14)
    b.cyl((5.0, 4.08, 1.45), (5.0, 4.12, 1.45), 0.12, BLACK, BLACK, 12)
    b.cyl((5.4, 3.45, 1.4), (5.62, 3.45, 1.4), 0.09, BLACK, DARK, 10)
    front_sign(b, 1.2, 4.68, 5.32, 3.48, 3.68, plate("GOVERNOR", 0.64, 0.2, "black"))
    b.cyl((2.86, 3.7, 1.45), (4.6, 3.7, 1.45), 0.025, "stainless", "stainless", 6)
    b.cyl((5.4, 3.7, 1.45), (7.14, 3.7, 1.45), 0.025, "stainless", "stainless", 6)
    # The overspeed trip and its red reset lever.
    b.box(5.62, 3.05, 1.3, 6.02, 3.42, 1.7, "gunmetal")
    b.box(5.78, 3.42, 1.4, 5.86, 3.62, 1.46, sw("red"))
    front_sign(b, 1.3, 5.64, 6.0, 3.12, 3.32, plate("OVERSPEED|TRIP", 0.36, 0.2, "red"))
    # The charge air coolers: their water boxes, the cooling water in and out, the necks from the compressors and down
    # into the manifolds, their legs.
    for (x0, x1), tx, mx in zip(COOLERS, TURBOS, (3.55, 6.45)):
        side = -1 if x0 < 5 else 1
        xo = x0 if side < 0 else x1
        H.side_box(b, side, xo, 0.1, COOLER_Y[0] + 0.05, COOLER_Y[1] - 0.05, COOLER_Z[0] + 0.05, COOLER_Z[1] - 0.05, PAINT, DARK)
        for zb in (0.4, 1.0):
            for yb in (3.8, 4.9):
                b.cyl((xo + side * 0.1, yb, zb), (xo + side * 0.125, yb, zb), 0.02, MID, MID, 6)
        for y, z in ((3.95, 0.4), (4.75, 0.75)):
            px = xo + side * 0.1
            run(b, [(px, y, z), (xo + side * 0.32, y, z), (xo + side * 0.32, 0.0, z)], 0.07, "pipe_green", joints=DARK, sides=8)
            b.cyl((xo + side * 0.32, 0.0, z), (xo + side * 0.32, 0.03, z), 0.13, sw("red"), sw("red"), 10)
        H.pipe(b, (tx, COOLER_Y[1] - 0.02, 0.72), (tx, TURBO_Y - 0.42, 0.72), 0.18, "turbo", sides=12)
        H.pipe(b, (mx, 3.45, 0.7), (mx, COOLER_Y[0] + 0.02, 0.7), 0.2, GREEN, sides=12)
        H.flange(b, (mx, COOLER_Y[0] - 0.05, 0.7), "y", 0.2)
        for lx in (x0 + 0.05, x1 - 0.13):
            for lz in (0.3, 1.02):
                if abs(lx + 0.04 - 3.9) < 0.2 or abs(lx + 0.04 - 6.1) < 0.2:
                    continue
                b.box(lx, 0.0, lz, lx + 0.08, COOLER_Y[0], lz + 0.08, DARK)
        b.box(x0 + 0.06, 2.4, 0.38, x0 + 0.12, 2.48, 1.02, DARK)
        b.box(x1 - 0.12, 2.4, 0.38, x1 - 0.06, 2.48, 1.02, DARK)
        front_sign(b, COOLER_Z[0], x0 + 0.25, x1 - 0.25, 4.45, 4.75, plate("CHARGE AIR|COOLER", x1 - x0 - 0.5, 0.3, "white"))
        # The turbocharger's pedestal on its cooler, the filter-silencer's perforated band and plate, oil lines.
        b.box(tx - 0.18, COOLER_Y[1], 0.95, tx + 0.18, TURBO_Y - 0.26, 1.18, "gunmetal")
        b.cyl((tx, TURBO_Y, 0.12), (tx, TURBO_Y, 0.42), 0.565, BLACK, BLACK, 16)
        for z in (0.12, 0.42):
            b.cyl((tx, TURBO_Y, z - 0.015), (tx, TURBO_Y, z + 0.015), 0.575, "stainless", "stainless", 16)
        front_sign(b, 0.04, tx - 0.22, tx + 0.22, TURBO_Y - 0.08, TURBO_Y + 0.08, plate("TURBOCHARGER", 0.44, 0.16, "black"))
        run(b, [(tx + side * 0.2, TURBO_Y - 0.2, 1.06), (tx + side * 0.2, 3.6, 1.06), (tx + side * 0.2, 3.6, 1.65)], 0.025, "pipe_brown", joints="pipe_brown", sides=6)
        for z in (1.16, 1.76):
            bolts(b, (tx, TURBO_Y, z), 0.44, 12, 0.02, 0.025, MID)
    # The bellows (convolutions between flanges) and the silencer's bands, hangers to the roof, the roof thimble.
    sx, sz = STACK
    for y in (7.02, 7.43):
        b.cyl((sx, y - 0.03, sz), (sx, y + 0.03, sz), 0.36, "flange", sw("mgrey"), 14)
    for k in range(6):
        y = 7.07 + k * 0.06
        b.cyl((sx, y, sz), (sx, y + 0.035, sz), 0.33, "stainless_dark", sw("mgrey"), 14)
    for y in (8.2, 9.8):
        b.cyl((sx, y - 0.04, sz), (sx, y + 0.04, sz), 1.03, "gunmetal", DARK, 20)
        for dx in (-1.03, 1.03):
            b.box(sx + dx - 0.04, y - 0.04, sz - 0.04, sx + dx + 0.04, y + 0.04, sz + 0.04, DARK)
    for dx in (-1.03, 1.03):
        b.cyl((sx + dx, 8.2, sz), (sx + dx, ROOF - 0.03, sz), 0.022, "stainless_dark", "stainless_dark", 6)
        b.box(sx + dx - 0.1, ROOF - 0.03, sz - 0.1, sx + dx + 0.1, ROOF, sz + 0.1, DARK)
    b.cyl((sx, ROOF - 0.3, sz), (sx, ROOF - 0.02, sz), 0.55, "flange", sw("mgrey"), 14)
    front_sign(b, sz - 1.0, sx - 0.55, sx + 0.55, 9.4, 9.8, plate("EXHAUST SILENCER|HOT SURFACE", 1.1, 0.4, "yellow"))
    # The duplex fuel filter on the right, the supply up out of the floor into it and on to the headers.
    fx, fz = 7.85, 1.3
    b.box(fx - 0.28, 0.0, fz - 0.4, fx + 0.28, 0.28, fz + 0.4, "concrete")
    b.box(fx - 0.22, 0.28, fz - 0.35, fx + 0.22, 0.42, fz + 0.35, "gunmetal")
    for dz in (-0.18, 0.18):
        b.cyl((fx, 0.42, fz + dz), (fx, 1.12, fz + dz), 0.15, "gunmetal", DARK, 12)
        b.cyl((fx, 1.12, fz + dz), (fx, 1.17, fz + dz), 0.17, DARK, DARK, 12)
    b.box(fx - 0.06, 1.17, fz - 0.05, fx + 0.06, 1.3, fz + 0.05, sw("red"))
    b.box(fx - 0.06, 1.3, fz - 0.25, fx + 0.06, 1.36, fz + 0.25, sw("red"))
    sign(b, 1, fx + 0.22, fz - 0.3, fz + 0.3, 0.3, 0.4, plate("DUPLEX FUEL FILTER", 0.6, 0.1, "white"))
    # (Up out of the floor a block to the right of the starting air's riser, where the day tank's line ends under it.)
    run(b, [(8.6, 0.0, 0.5), (8.6, 0.35, 0.5), (8.6, 0.35, 1.0), (fx + 0.22, 0.35, 1.0)], 0.05, "pipe_yellow", joints=DARK, sides=8)
    b.cyl((8.6, 0.0, 0.5), (8.6, 0.03, 0.5), 0.1, sw("red"), sw("red"), 10)
    run(b, [(fx, 1.3, fz + 0.3), (fx, 1.6, fz + 0.3), (fx, 1.6, 1.6), (7.14, 1.6, 1.6), (7.14, 3.15, 1.6)], 0.045, "pipe_yellow", joints=DARK, sides=8)
    run(b, [(7.14, 3.32, 1.6), (7.5, 3.32, 1.6), (7.5, 0.0, 1.6)], 0.045, "pipe_yellow", joints=DARK, sides=8)
    run(b, [(2.86, 3.15, 1.6), (2.86, 3.32, 1.6)], 0.045, "pipe_yellow", joints=DARK, sides=8)
    # The jacket water outlet headers forward and down into the floor (to the thermostatic valve and the cooler).
    for hx, out in ((3.62, 2.4), (6.38, 7.6)):
        run(b, [(hx, 4.28, 1.9), (hx, 4.28, 1.85), (out, 4.28, 1.85), (out, 0.0, 1.85)], 0.09, "pipe_green", joints=DARK, sides=10)
        b.cyl((out, 0.0, 1.85), (out, 0.03, 1.85), 0.16, sw("red"), sw("red"), 10)
    # The starting air headers fed from the floor at the front (the receivers' lines come up here).
    for ax_, out in ((3.3, 2.1), (6.7, 7.9)):
        run(b, [(ax_, 4.12, 2.2), (ax_, 4.12, 1.9), (out, 4.12, 1.9), (out, 0.0, 1.9)], 0.06, "pipe_lblue", joints=DARK, sides=8)
        b.cyl((out, 0.0, 1.9), (out, 0.03, 1.9), 0.12, sw("red"), sw("red"), 10)


# ------------------------------------------------------------------------------------------------ the drive end and the generator

GUARD = (3.8, 6.2, 1.3, 3.7, 13.55, 14.4)     # the coupling guard's x0, x1, y0, y1, z0, z1
COUPLING = (13.62, 14.33, 0.85)               # the coupling's z0, z1 and radius (the game's: live)
FAN_Z, FAN_R = 20.55, 0.9                     # the fan behind the guard at the far end (the game's: live)
# The generator's terminal box on its left (x0, x1, y0, y1, z0, z1): toward the room's south wall, where its cables go
# to the neutral grounding resistor and the terminal cubicle (tools/medium_plant.py diesel_building).
TERMINAL = (2.3, 3.15, 1.2, 2.9, 16.5, 18.0)


def drive(b):
    rev(b, (AX, AY, 12.6), (AX, AY, 13.55), [(0, 2.05), (0.5, 2.05), (0.95, 1.2)], GREEN, None, GREEN, 24)
    g0, g1 = GEN_Z
    rev(b, (AX, AY, g0), (AX, AY, g1), [(0, GEN_R), (g1 - g0, GEN_R)], GEN, GEN, None, 28)
    rev(b, (AX, AY, g1), (AX, AY, 18.95), [(0, GEN_R), (0.12, GEN_R), (0.35, 1.5)], GEN, None, GEN, 28)
    b.box(2.95, 0.72, g0 + 0.1, 7.05, 1.6, g1 - 0.1, GEN)
    # The outboard bearing's pedestal and housing, the exciter on its own, the fan's cowl.
    b.box(4.3, 0.7, 18.95, 5.7, 2.0, 19.5, GEN)
    rev(b, (AX, AY, 18.95), (AX, AY, 19.5), [(0, 0.45), (0.08, 0.55), (0.47, 0.55), (0.55, 0.45)], GEN, None, None, 16)
    b.box(4.2, 0.7, 19.6, 5.8, 1.6, 20.3, GEN)
    rev(b, (AX, AY, 19.55), (AX, AY, 20.35), [(0, 0.85), (0.06, 1.0), (0.8, 1.0)], GEN, GEN, None, 20)
    rev(b, (AX, AY, 20.35), (AX, AY, 20.75), [(0, 1.0), (0.4, 1.0)], GEN, None, None, 20)
    # The cooling air hood on top.
    air_hood(b)
    # The coupling guard (its frame; the mesh in detail).
    x0, x1, y0, y1, z0, z1 = GUARD
    t = 0.08
    for x in (x0, x1 - t):
        for y in (y0, y1 - t):
            b.box(x, y, z0, x + t, y + t, z1, YELLOW)
    for z in (z0, z1 - t):
        b.box(x0 + t, y0, z, x1 - t, y0 + t, z + t, YELLOW)
        b.box(x0 + t, y1 - t, z, x1 - t, y1, z + t, YELLOW)
        b.box(x0, y0 + t, z, x0 + t, y1 - t, z + t, YELLOW)
        b.box(x1 - t, y0 + t, z, x1, y1 - t, z + t, YELLOW)
    b.box(x0 - 0.1, 0.7, z0 + 0.2, x0 + 0.1, y0, z0 + 0.4, YELLOW)
    b.box(x1 - 0.1, 0.7, z0 + 0.2, x1 + 0.1, y0, z0 + 0.4, YELLOW)
    cyl(b, (AX, AY, 13.5), (AX, AY, g0 + 0.02), 0.25, "stainless_dark", sw("mgrey"), 12)
    # The terminal box on the right.
    tx0, tx1, ty0, ty1, tz0, tz1 = TERMINAL
    b.box(tx0, ty0, tz0, tx1, ty1, tz1, "paint_grey")


def air_hood(b):
    """The generator's cooling air intake on top: a louvred hood (its posts and dark screen start down inside the
    round frame, so it meets the curve with no gap), real slats recessed in the frame, a band and a rain cap."""
    x0, x1, z0, z1, y0, band, top, p = 3.9, 6.1, 15.1, 17.9, 4.15, 5.27, 5.35, 0.08
    b.box(x0 + 0.06, y0, z0 + 0.06, x1 - 0.06, band, z1 - 0.06, BLACK)
    for x in (x0, x1 - p):
        for z in (z0, z1 - p):
            b.box(x, y0 - 0.01, z, x + p, top, z + p, GEN)
    b.box(x0, band, z0 + p, x0 + p, top, z1 - p, GEN)
    b.box(x1 - p, band, z0 + p, x1, top, z1 - p, GEN)
    b.box(x0 + p, band, z0, x1 - p, top, z0 + p, GEN)
    b.box(x0 + p, band, z1 - p, x1 - p, top, z1, GEN)
    y = 4.35
    while y + 0.04 < band - 0.04:
        b.box(x0 + 0.02, y, z0 + p, x0 + 0.06, y + 0.04, z1 - p, GEN)
        b.box(x1 - 0.06, y, z0 + p, x1 - 0.02, y + 0.04, z1 - p, GEN)
        b.box(x0 + p, y, z0 + 0.02, x1 - p, y + 0.04, z0 + 0.06, GEN)
        b.box(x0 + p, y, z1 - 0.06, x1 - p, y + 0.04, z1 - 0.02, GEN)
        y += 0.12
    b.box(x0 - 0.08, top, z0 - 0.08, x1 + 0.08, top + 0.08, z1 + 0.08, GEN)


def drive_detail(b):
    g0, g1 = GEN_Z
    # The flywheel housing's flange and bolts; the barring gear on its left with its motor and plate.
    b.cyl((AX, AY, 12.6), (AX, AY, 12.7), 2.12, GREEN, GREEN, 24)
    bolts(b, (AX, AY, 12.7), 2.0, 24, 0.025, 0.03, MID, "z")
    b.box(2.75, 0.95, 12.72, 3.2, 1.6, 13.25, "gunmetal")
    b.cyl((2.98, 1.6, 12.98), (2.98, 2.05, 12.98), 0.17, "paint_grey", DARK, 12)
    b.cyl((2.98, 2.05, 12.98), (2.98, 2.1, 12.98), 0.12, DARK, DARK, 10)
    b.box(2.72, 1.1, 13.25, 2.76, 1.5, 13.3, sw("red"))
    sign(b, -1, 2.75, 12.76, 13.2, 1.25, 1.45, plate("BARRING|GEAR", 0.44, 0.2, "yellow"))
    # The coupling guard's mesh (top and sides) and its caution plate.
    x0, x1, y0, y1, z0, z1 = GUARD
    t = 0.08
    m0, m1 = z0 + t, z1 - t
    b.quad((x0 + t, y1 - t / 2, m1), (x0 + t, y1 - t / 2, m0), (x1 - t, y1 - t / 2, m0), (x1 - t, y1 - t / 2, m1), "grating")
    b.quad((x0 + t / 2, y1 - t, m1), (x0 + t / 2, y0 + t, m1), (x0 + t / 2, y0 + t, m0), (x0 + t / 2, y1 - t, m0), "grating")
    b.quad((x1 - t / 2, y1 - t, m0), (x1 - t / 2, y0 + t, m0), (x1 - t / 2, y0 + t, m1), (x1 - t / 2, y1 - t, m1), "grating")
    sign(b, -1, x0, z0 + 0.12, z1 - 0.12, y1 - 0.62, y1 - 0.28, plate("CAUTION|ROTATING|EQUIPMENT", 0.61, 0.34, "yellow"))
    sign(b, 1, x1, z0 + 0.12, z1 - 0.12, y1 - 0.62, y1 - 0.28, plate("CAUTION|ROTATING|EQUIPMENT", 0.61, 0.34, "yellow"))
    # The generator's ribs, feet and their bolts.
    for z in (14.55, 15.35, 16.15, 16.95, 17.75, 18.45):
        b.cyl((AX, AY, z - 0.03), (AX, AY, z + 0.03), GEN_R + 0.05, GEN, GEN, 28)
    for fx0, fx1 in ((2.6, 3.0), (7.0, 7.4)):
        for fz in (14.6, 17.9):
            b.box(fx0, 0.7, fz, fx1, 0.86, fz + 0.5, GEN)
            for bz in (fz + 0.12, fz + 0.38):
                bx = (fx0 + fx1) / 2
                b.cyl((bx, 0.86, bz), (bx, 0.93, bz), 0.035, MID, MID, 6)
    # The nameplate on its bracket on the left, the set's sign on both sides (by train), lifting lugs.
    b.box(7.05, 2.25, 17.35, 7.15, 2.7, 18.15, GEN)
    sign(b, 1, 7.15, 17.3, 18.2, 2.2, 2.75, plate("SYNCHRONOUS GENERATOR|9000 KW  11250 KVA  0.8 PF|4160 V  3 PH  60 HZ  1561 A|514 RPM  14 POLES|"
                                              "CLASS F  BRUSHLESS EXCITER|SERIAL NO. G-77-3310", 0.9, 0.55, "white", 22), sw("label_white"))
    for side in (-1, 1):
        xb = 3.0 if side < 0 else 7.0
        b.box(min(xb, xb - side * 0.2), 3.32, 15.2, max(xb, xb - side * 0.2), 3.72, 17.6, GEN)
    H.trained(b, lambda t: [sign(b, s, 3.0 if s < 0 else 7.0, 15.15, 17.65, 3.28, 3.76,
                                 plate(f"EMERGENCY DIESEL|GENERATOR {('1A', '1B')[t]}", 2.5, 0.48, "white", 44), sw("label_white"))
                            for s in (-1, 1)])
    for lz in (14.7, 18.3):
        for side in (-1, 1):
            x, y = AX + side * 1.5, AY + 1.5
            b.box(x - 0.06, y - 0.05, lz - 0.1, x + 0.06, y + 0.25, lz + 0.1, GEN)
    # The terminal box's door, bolts and the conduits down into the floor.
    tx0, tx1, ty0, ty1, tz0, tz1 = TERMINAL
    H.side_box(b, -1, tx0, 0.02, ty0 + 0.08, ty1 - 0.08, tz0 + 0.08, tz1 - 0.08, "paint_grey", DARK)
    for by in (ty0 + 0.14, ty1 - 0.14):
        for bz in (tz0 + 0.14, (tz0 + tz1) / 2, tz1 - 0.14):
            b.cyl((tx0 - 0.02, by, bz), (tx0 - 0.04, by, bz), 0.02, MID, MID, 6)
    sign(b, -1, tx0 - 0.02, tz0 + 0.3, tz1 - 0.3, ty1 - 0.5, ty1 - 0.22, plate("DANGER|4160 VOLTS", 1.0, 0.28, "red"))
    for cz in (16.8, 17.25, 17.7):
        b.cyl((2.42, ty0, cz), (2.42, 0.0, cz), 0.06, "stainless_dark", "stainless_dark", 8)
        b.cyl((2.42, ty0 - 0.08, cz), (2.42, ty0, cz), 0.08, DARK, DARK, 8)
        b.cyl((2.42, 0.0, cz), (2.42, 0.03, cz), 0.11, DARK, DARK, 8)
    # The outboard bearing's oil sight glass and its temperature detector's conduit; the fan guard's mesh.
    b.cyl((AX - 0.55, AY - 0.15, 19.22), (AX - 0.6, AY - 0.15, 19.22), 0.07, BLACK, pic("glass"), 10)
    run(b, [(AX + 0.45, AY + 0.3, 19.22), (5.76, AY + 0.3, 19.22), (5.76, 0.7, 19.22), (6.9, 0.7, 19.22)], 0.025, "stainless_dark", sides=6)
    b.quad((AX + 0.98, AY + 0.98, 20.76), (AX + 0.98, AY - 0.98, 20.76), (AX - 0.98, AY - 0.98, 20.76), (AX - 0.98, AY + 0.98, 20.76), "grating")
    for k in range(8):
        a = math.pi * k / 4
        b.cyl((AX, AY, 20.77), (AX + 0.99 * math.cos(a), AY + 0.99 * math.sin(a), 20.77), 0.015, DARK, DARK, 6)
    b.cyl((AX, AY, 20.76), (AX, AY, 20.8), 0.1, DARK, DARK, 10)


# ------------------------------------------------------------------------------------------------ the walkways

def walkways(b):
    z0, z1 = DECK_Z
    for x0, x1, edge, inner in ((0.35, 2.76, 0.36, 2.66), (7.24, 9.65, 9.57, 7.27)):
        b.box(x0, DECK - 0.08, z0, x1, DECK, z1, "grating")
        for x in (edge, inner):
            b.box(x + 0.01, DECK - 0.26, z0, x + 0.06, DECK - 0.08, z1, YELLOW)
        for z in (z0 + 0.02, 4.5, 7.0, 9.5, z1 - 0.09):
            b.box(edge, 0, z, edge + 0.07, DECK + 1.05, z + 0.07, YELLOW)
            b.box(inner, 0, z, inner + 0.07, DECK - 0.26, z + 0.07, YELLOW)
        # The flight down beside the generator.
        t0, t1 = (x0 + 0.08, 2.62) if edge < 5 else (7.31, x1 - 0.08)
        for za, top in STEPS:
            b.box(t0, top - 0.06, za, t1, top, za + 0.94, "grating")


def walkways_detail(b):
    z0, z1 = DECK_Z
    for x0, x1, edge, inner in ((0.35, 2.76, 0.36, 2.66), (7.24, 9.65, 9.57, 7.27)):
        side = -1 if edge < 5 else 1
        # Joists every two blocks; the toe boards; the rails (top and middle) along the edge and across the front.
        for z in (3.0, 5.0, 7.0, 9.0, 11.0):
            b.box(min(edge, inner) + 0.07, DECK - 0.22, z, max(edge, inner), DECK - 0.12, z + 0.06, YELLOW)
        te = edge + 0.07 if side < 0 else edge - 0.03
        b.box(te, DECK, z0 + 0.09, te + 0.03, DECK + 0.12, z1, YELLOW)
        rx = edge + 0.035
        for y in (DECK + 0.5, DECK + 1.0):
            b.cyl((rx, y, z0 + 0.02), (rx, y, z1), 0.035, YELLOW, YELLOW, 8)
            b.cyl((rx, y, z0 + 0.055), (inner + 0.035, y, z0 + 0.055), 0.035, YELLOW, YELLOW, 8)
        b.box(min(edge, inner) + 0.07, DECK, z0 + 0.02, max(edge, inner), DECK + 0.12, z0 + 0.05, YELLOW)
        b.box(inner, DECK, z0 + 0.02, inner + 0.07, DECK + 1.05, z0 + 0.09, YELLOW)
        # The flight's nosings, its stringers (sloped plates) and handrails both sides, posts at the foot.
        t0, t1 = (x0 + 0.08, 2.62) if side < 0 else (7.31, x1 - 0.08)
        for za, top in STEPS:
            b.box(t0, top - 0.08, za + 0.94, t1, top, za + 1.0, YELLOW)
        foot = STEPS[-1][0] + 1.0
        for x in (t0 - 0.05, t1 + 0.05):
            # The stringer: from under the deck's end down to the floor past the last tread.
            # (Its middle line through the treads' middles: each tread's ends a quarter block either side of it.)
            ya, za_ = 1.755, z1 - 0.1
            yb, zb = 0.0, 15.41
            ln = math.hypot(ya - yb, za_ - zb)
            u = (0, (yb - ya) / ln, (zb - za_) / ln)
            b.obox((x, (ya + yb) / 2, (za_ + zb) / 2), u, (1, 0, 0), ln / 2, 0.025, 0.27, YELLOW)
            b.cyl((x, DECK + 1.0, z1), (x, 1.5, foot), 0.035, YELLOW, YELLOW, 8)
            b.box(x - 0.035, 0.0, foot - 0.035, x + 0.035, 1.5, foot + 0.035, YELLOW)
        b.cyl((t0 - 0.05, DECK + 1.0, z1), (t0 - 0.05, DECK + 1.0, z1 + 0.02), 0.035, YELLOW, YELLOW, 8)


# ------------------------------------------------------------------------------------------------ the engine gauge board

BOARD = (0.2, 2.0, 0.9, 2.55, 0.3, 0.42)       # x0, x1, y0, y1, z (its face), its back
GAUGES = [("ENGINE SPEED", "RPM", 0, 600, 7), ("LUBE OIL PRESS", "PSIG", 0, 100, 6), ("LUBE OIL TEMP", "°F", 50, 250, 5),
          ("JACKET WATER TEMP", "°F", 50, 250, 5), ("FUEL OIL PRESS", "PSIG", 0, 100, 6), ("STARTING AIR PRESS", "PSIG", 0, 400, 5)]
GAUGE_R = 0.15


def gauge_board(b, live):
    x0, x1, y0, y1, z, zb = BOARD
    grey, edge = "paint_grey", DARK
    for lx in (x0 + 0.12, x1 - 0.2):
        b.box(lx, 0.0, z + 0.02, lx + 0.08, y0, zb - 0.02, DARK)
        b.box(lx - 0.06, 0.0, z - 0.04, lx + 0.14, 0.03, zb + 0.04, DARK)
    b.box(x0, y0, z, x1, y1, zb, [grey, grey, grey, grey, grey, grey])
    b.box(x0 - 0.03, y1, z - 0.04, x1 + 0.03, y1 + 0.05, zb + 0.02, grey)
    H.trained(b, lambda t: front_sign(b, z, x0 + 0.2, x1 - 0.2, y1 - 0.15, y1 - 0.03,
                                      plate(f"DIESEL ENGINE {('1A', '1B')[t]}  GAUGE BOARD", x1 - x0 - 0.4, 0.12, "black")))
    for k, (name, unit, lo, hi, majors) in enumerate(GAUGES):
        gx = x0 + 0.4 + (k % 3) * 0.6
        gy = y1 - 0.42 - (k // 3) * 0.52
        face = L.dial_face_tex(unit, lo, hi, majors, GAUGE_R * 1.44, 270)
        gauge(b, live, name, (gx, gy, z), GAUGE_R, face)
        front_sign(b, z, gx - 0.26, gx + 0.26, gy - GAUGE_R - 0.09, gy - GAUGE_R - 0.02, plate(name, 0.52, 0.07, "white"), d=0.006)
    # The lamps (RUNNING red, READY green: the game's), the local emergency stop.
    ly = y0 + 0.25
    for k, (legend, colour) in enumerate((("ENGINE|RUNNING", "red"), ("READY|TO START", "green"))):
        lx = x0 + 0.25 + k * 0.55
        b.cyl((lx, ly, z), (lx, ly, z - 0.03), 0.06, DARK, DARK, 10)
        b.cyl((lx, ly, z - 0.03), (lx, ly, z - 0.045), 0.045, BLACK, BLACK, 10)
        live["lamps"].append({"name": ("running", "ready")[k], "c": _r(lx, ly, z - 0.047), "r": 0.04, "colour": colour,
                              "on": C.LENS[colour], "off": C.LENS["off_" + colour]})
        front_sign(b, z, lx + 0.09, lx + 0.33, ly - 0.06, ly + 0.06, plate(legend, 0.24, 0.12, "white"), d=0.006)
    ex = x1 - 0.3
    b.cyl((ex, ly, z), (ex, ly, z - 0.04), 0.1, "paint_yellow", "paint_yellow", 14)
    b.cyl((ex, ly, z - 0.04), (ex, ly, z - 0.07), 0.035, DARK, DARK, 8)
    b.cyl((ex, ly, z - 0.07), (ex, ly, z - 0.11), 0.075, sw("red"), sw("red"), 14)
    front_sign(b, z, ex - 0.18, ex + 0.18, ly - 0.2, ly - 0.12, plate("EMERGENCY STOP", 0.36, 0.08, "red"), d=0.006)


# ------------------------------------------------------------------------------------------------ the set

def diesel_set(seed=307):
    with own_atlas():
        return _diesel_set(seed)


def _diesel_set(seed=307):
    b = C.Board(W, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    foundation(b)
    engine(b)
    free_end(b)
    drive(b)
    walkways(b)
    b.core = len(b.still)
    foundation_detail(b)
    engine_detail(b)
    free_end_detail(b)
    drive_detail(b)
    walkways_detail(b)
    gauge_board(b, live)
    live["shaft"] = [AX, AY]
    live["coupling"] = {"z0": COUPLING[0], "z1": COUPLING[1], "r": COUPLING[2]}
    live["fan"] = {"z": FAN_Z, "r": FAN_R}
    live["stack"] = [STACK[0], ROOF, STACK[1]]
    live["mats"] = {"needle": sw("red"), "mark": sw("white"), "hub": "stainless_dark", "bolt": "stainless", "blade": "gunmetal"}
    b.live = live
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the starting air system

# Each set's two receivers, two compressors and dryer are labelled by the anchor's variant: the receivers 1A-1, 1A-2,
# 1B-1, 1B-2 (v0 to v3), the compressors 1A-A, 1A-B, 1B-A, 1B-B, the dryer 1A or 1B (v0, v1).
RECEIVER_TAGS = ("1A-1", "1A-2", "1B-1", "1B-2")
COMPRESSOR_TAGS = ("1A-A", "1A-B", "1B-A", "1B-B")
DRYER_TAGS = ("1A", "1B")
AIR_BLUE = "pipe_lblue"


def labelled(b, tags, f):
    """Draws f(tag) into each variant's state ("v0", "v1", ...)."""
    for k, tag in enumerate(tags):
        st = b.state(f"v{k}")
        keep = b.still
        b.still = st["still"]
        try:
            f(tag)
        finally:
            b.still = keep


def valve(b, at, axis_, r, out=(0, 0, -1), wheel=0.09):
    """A small flanged gate valve in a line along axis_ at at: its body and flanges, the bonnet standing out along out
    and its red handwheel."""
    x, y, z = at
    e = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1)}[axis_]
    a = (x - e[0] * r * 1.6, y - e[1] * r * 1.6, z - e[2] * r * 1.6)
    c = (x + e[0] * r * 1.6, y + e[1] * r * 1.6, z + e[2] * r * 1.6)
    b.cyl(a, c, r * 1.35, "gunmetal", DARK, 10)
    for p in (a, c):
        H.flange(b, p, axis_, r, 0.025)
    top = (x + out[0] * r * 2.6, y + out[1] * r * 2.6, z + out[2] * r * 2.6)
    b.cyl((x, y, z), top, r * 0.7, "gunmetal", DARK, 8)
    rim = (top[0] + out[0] * 0.025, top[1] + out[1] * 0.025, top[2] + out[2] * 0.025)
    b.cyl(top, rim, wheel, sw("red"), sw("red"), 12)


def seal(b, x, z, r):
    """Where a line goes down into the floor: its red fire seal collar."""
    b.cyl((x, 0.0, z), (x, 0.03, z), r + 0.06, sw("red"), sw("red"), 10)


def air_receiver(seed=311):
    with own_atlas():
        return _air_receiver(seed)


def _air_receiver(seed=311):
    """A starting air receiver, 2 wide, 2 deep, 5 tall: a vertical vessel (1.5 across, ellipsoidal heads) on four legs on
    its pad, its welds; the inlet (from the dryer) and the outlet (to the engine) out of its front low down, each
    through its valve and down into the floor; the drain off the bottom with its valve; the relief valve on top; the
    handhole; the low air pressure switch; the pressure gauge at eye height (the game's: live); its plates."""
    b = C.Board(2, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    cx, cz, R, y0, y1, hd = 1.0, 1.0, 0.75, 0.95, 4.0, 0.375
    b.box(0.08, 0.0, 0.08, 1.92, 0.1, 1.92, "concrete")
    prof = []
    for k in range(7):
        ph = math.pi / 2 * k / 6
        prof.append((hd - hd * math.cos(ph), max(0.001, R * math.sin(ph))))
    for k in range(7):
        ph = math.pi / 2 * k / 6
        prof.append((hd + (y1 - y0) + hd * math.sin(ph), max(0.001, R * math.cos(ph))))
    rev(b, (cx, y0 - hd, cz), (cx, y1 + hd, cz), prof, "paint_grey", None, None, 24)
    for lx, lz in ((cx - 0.78, cz), (cx + 0.78, cz), (cx, cz - 0.78), (cx, cz + 0.78)):
        b.box(lx - 0.05, 0.125, lz - 0.05, lx + 0.05, 1.25, lz + 0.05, "paint_grey")
        b.box(lx - 0.11, 0.1, lz - 0.11, lx + 0.11, 0.125, lz + 0.11, DARK)
    b.core = len(b.still)
    for lx, lz in ((cx - 0.78, cz), (cx + 0.78, cz), (cx, cz - 0.78), (cx, cz + 0.78)):
        for dx, dz in ((-0.07, -0.07), (0.07, 0.07)):
            b.cyl((lx + dx, 0.125, lz + dz), (lx + dx, 0.16, lz + dz), 0.018, MID, MID, 6)
    for y in (y0, (y0 + y1) / 2, y1):
        b.cyl((cx, y - 0.012, cz), (cx, y + 0.012, cz), R + 0.006, "weld_band", sw("mgrey"), 24)
    # The inlet (left) and the outlet (right): out of the shell toward the front corners, a valve, an elbow down into
    # the floor.
    for side, legend in ((-1, "INLET"), (1, "OUTLET")):
        d = (side * 0.7071, -0.7071)
        p0 = (cx + d[0] * (R - 0.02), 1.4, cz + d[1] * (R - 0.02))
        p1 = (cx + d[0] * 0.98, 1.4, cz + d[1] * 0.98)
        b.cyl(p0, p1, 0.065, AIR_BLUE, AIR_BLUE, 8)
        b.cyl((cx + d[0] * (R - 0.02), 1.4, cz + d[1] * (R - 0.02)), (cx + d[0] * (R + 0.04), 1.4, cz + d[1] * (R + 0.04)), 0.11, "paint_grey", "paint_grey", 10)
        run(b, [p1, (p1[0], 0.0, p1[2])], 0.065, AIR_BLUE)
        joint(b, p1, 0.07, DARK)
        valve(b, (p1[0], 0.85, p1[2]), "y", 0.065, (0, 0, -1))
        seal(b, p1[0], p1[2], 0.065)
        front_sign(b, p1[2] - 0.08, p1[0] - 0.12, p1[0] + 0.12, 0.42, 0.5, plate(legend, 0.24, 0.08, "blue"), d=0.008)
    # The drain off the bottom: its valve and the line down to the floor drain at the front.
    run(b, [(cx, y0 - hd + 0.02, cz), (cx, 0.32, cz), (cx, 0.32, 0.32), (cx, 0.0, 0.32)], 0.03, "stainless_dark", joints=DARK, sides=6)
    b.box(cx - 0.05, 0.27, 0.55, cx + 0.05, 0.37, 0.67, "gunmetal")
    b.box(cx - 0.015, 0.37, 0.58, cx + 0.015, 0.4, 0.75, sw("red"))
    b.cyl((cx, 0.0, 0.32), (cx, 0.02, 0.32), 0.12, BLACK, BLACK, 10)
    # The relief valve on top, its lever and outlet.
    top = y1 + hd
    b.cyl((cx, top - 0.02, cz), (cx, top + 0.12, cz), 0.04, "stainless", "stainless", 8)
    b.cyl((cx, top + 0.12, cz), (cx, top + 0.3, cz), 0.07, "gunmetal", DARK, 10)
    b.cyl((cx, top + 0.3, cz), (cx, top + 0.45, cz), 0.04, "gunmetal", DARK, 8)
    b.box(cx - 0.012, top + 0.4, cz - 0.15, cx + 0.012, top + 0.43, cz + 0.02, sw("red"))
    b.cyl((cx, top + 0.2, cz), (cx + 0.18, top + 0.2, cz), 0.035, "gunmetal", DARK, 8)
    # The handhole on the front, its cover and bolts.
    b.cyl((cx, 2.6, cz - R + 0.04), (cx, 2.6, cz - R - 0.08), 0.15, "paint_grey", "paint_grey", 14)
    b.cyl((cx, 2.6, cz - R - 0.08), (cx, 2.6, cz - R - 0.11), 0.19, "paint_grey", "paint_grey", 14)
    bolts(b, (cx, 2.6, cz - R - 0.11), 0.155, 8, 0.016, 0.02, MID)
    # The low air pressure switch on the right, its conduit down to the floor.
    b.cyl((cx + R - 0.02, 2.0, cz), (cx + R + 0.1, 2.0, cz), 0.025, "stainless", "stainless", 6)
    b.box(cx + R + 0.1, 1.88, cz - 0.1, cx + R + 0.22, 2.14, cz + 0.1, "paint_grey")
    b.cyl((cx + R + 0.16, 1.88, cz), (cx + R + 0.16, 0.1, cz), 0.02, "stainless_dark", "stainless_dark", 6)
    sign(b, 1, cx + R + 0.22, cz - 0.08, cz + 0.08, 2.0, 2.1, plate("PS", 0.16, 0.1, "black"), d=0.006)
    # The gauge at eye height on its siphon off the front, its isolating cock.
    gz = cz - R
    run(b, [(cx - 0.3, 1.55, gz + 0.06), (cx - 0.3, 1.55, gz - 0.12), (cx - 0.3, 1.8, gz - 0.12)], 0.016, "stainless", joints="stainless", sides=6)
    b.cyl((cx - 0.3, 1.8, gz - 0.12), (cx - 0.3, 1.8, gz - 0.16), 0.03, "gunmetal", DARK, 8)
    gauge(b, live, "AIR PRESS", (cx - 0.3, 1.8, gz - 0.16), 0.16, L.dial_face_tex("PSIG", 0, 400, 5, 0.16 * 1.36, 270))
    # Its plates on brackets off the shell: the name by variant, the vessel's data, the warning.
    def bracket_plate(x0, x1, ya, yb, mat):
        zf = cz - R - 0.04
        back = cz - math.sqrt(R * R - max((x0 - cx) ** 2, (x1 - cx) ** 2)) + 0.02
        b.box(x0 + 0.03, ya + 0.02, zf + 0.012, x1 - 0.03, yb - 0.02, back, "paint_grey")
        front_sign(b, zf + 0.012, x0, x1, ya, yb, mat)
    labelled(b, RECEIVER_TAGS, lambda tag: bracket_plate(0.62, 1.38, 3.2, 3.6, plate(f"STARTING AIR|RECEIVER {tag}", 0.76, 0.4, "blue", 40)))
    bracket_plate(0.72, 1.28, 2.05, 2.35, plate("ASME  MAWP 400 PSIG|AT 150 °F   NB 41177", 0.56, 0.3, "white", 22))
    bracket_plate(0.62, 1.38, 3.75, 4.0, plate("DANGER  COMPRESSED AIR|250 PSIG", 0.76, 0.25, "red", 24))
    b.live = live
    live["mats"] = {"needle": sw("red")}
    on_floor(b)
    return b


def air_compressor(seed=313):
    with own_atlas():
        return _air_compressor(seed)


def _air_compressor(seed=313):
    """A starting air compressor, 3 wide, 2 deep, 2 tall: on its skid on a pad, the motor and the two-stage compressor
    side by side, belt-driven (the flywheel pulley behind the guard's mesh, turning: the game's), the low and high
    pressure cylinders in a vee with their finned barrels and heads, the intake filter, the intercooler over them, the
    aftercooler on the guard, the discharge out of the back through its check valve and down into the floor, the safety
    valve; the local station (RUNNING and STOPPED lamps: the game's, the HAND-OFF-AUTO selector at AUTO); the discharge
    gauge (the game's)."""
    b = C.Board(3, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    blue = "paint_blue"
    b.box(0.08, 0.0, 0.1, 2.92, 0.12, 1.9, "concrete")
    for z0 in (0.3, 1.58):
        b.box(0.2, 0.12, z0, 2.8, 0.3, z0 + 0.12, DARK)
    b.box(0.2, 0.26, 0.42, 2.8, 0.3, 1.58, STEEL)
    # The motor (shaft along z at (0.75, 0.75)), its feet.
    mx, my = 0.75, 0.75
    H.pipe(b, (mx, my, 0.66), (mx, my, 1.6), 0.33, blue, sides=16)
    b.box(mx - 0.3, 0.3, 0.75, mx + 0.3, my - 0.25, 1.5, blue)
    # The compressor's crankcase; the cylinders' vee on it.
    b.box(1.45, 0.3, 0.7, 2.65, 1.0, 1.55, blue)
    px, py = 2.05, 0.75
    for ang, r, length in ((-30, 0.2, 0.42), (30, 0.14, 0.36)):
        a = math.radians(ang)
        ux, uy = math.sin(a), math.cos(a)
        bx = px + ux * 0.42
        by = 0.98
        b.cyl((bx, by, 1.12), (bx + ux * length, by + uy * length, 1.12), r, blue, blue, 14)
        hx, hy = bx + ux * (length + 0.06), by + uy * (length + 0.06)
        b.obox((hx, hy, 1.12), (ux, uy, 0), (0, 0, 1), 0.07, r + 0.05, r + 0.05, "gunmetal")
    # The belt guard round both pulleys (its mesh front in detail).
    gx0, gx1, gy0, gy1, gz0, gz1 = 0.5, 2.6, 0.3, 1.25, 0.28, 0.62
    b.box(gx0, gy0, gz0 + 0.3, gx1, gy1, gz1, [DARK, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW])
    b.core = len(b.still)
    # The motor's fins, fan cowl, terminal box and conduit; its plate.
    for z in [0.75 + k * 0.09 for k in range(9)]:
        b.cyl((mx, my, z), (mx, my, z + 0.03), 0.355, blue, blue, 16)
    b.cyl((mx, my, 1.6), (mx, my, 1.72), 0.3, blue, DARK, 16)
    b.box(mx + 0.2, my + 0.15, 1.0, mx + 0.42, my + 0.4, 1.3, "paint_grey")
    b.cyl((mx + 0.31, my + 0.15, 1.15), (mx + 0.31, 0.3, 1.15), 0.025, "stainless_dark", "stainless_dark", 6)
    sign(b, 1, mx + 0.42, 1.02, 1.28, my + 0.18, my + 0.37, placard_or("DANGER|480 VOLTS"), d=0.006)
    # The cylinders' fins, the intake filter, the intercooler over them, the safety valve.
    for ang, r, length in ((-30, 0.2, 0.42), (30, 0.14, 0.36)):
        a = math.radians(ang)
        ux, uy = math.sin(a), math.cos(a)
        bx, by = px + ux * 0.42, 0.98
        for k in range(1, 7):
            t = length * k / 7
            c0 = (bx + ux * t, by + uy * t, 1.12)
            c1 = (bx + ux * (t + 0.02), by + uy * (t + 0.02), 1.12)
            b.cyl(c0, c1, r + 0.05, blue, blue, 14)
    lx, ly = px - 0.5 * (0.42 + 0.48), 0.98 + 0.866 * 0.48
    b.cyl((lx - 0.07, ly + 0.11, 1.12), (lx - 0.07, ly + 0.3, 1.12), 0.12, "stainless", sw("enamel"), 14)
    b.cyl((lx - 0.07, ly + 0.3, 1.12), (lx - 0.07, ly + 0.33, 1.12), 0.14, DARK, DARK, 14)
    hx = px + 0.5 * (0.42 + 0.42)
    hy = 0.98 + 0.866 * 0.42
    ic = [(lx + 0.12, ly + 0.08, 1.32), (lx + 0.12, 1.78, 1.32), (hx, 1.78, 1.32), (hx, hy + 0.1, 1.32)]
    run(b, ic, 0.04, "copper", joints=DARK, sides=8)
    for t in range(1, 12):
        x = ic[1][0] + (ic[2][0] - ic[1][0]) * t / 12
        b.cyl((x - 0.008, 1.78, 1.32), (x + 0.008, 1.78, 1.32), 0.07, "copper", "copper", 8)
    b.cyl((hx, 1.78, 1.32), (hx, 1.9, 1.32), 0.02, "stainless", "stainless", 6)
    b.cyl((hx, 1.9, 1.32), (hx, 1.98, 1.32), 0.035, "gunmetal", DARK, 8)
    # The belt guard's front: a yellow frame, the mesh, the plates; the aftercooler (a finned coil) on top of it.
    t = 0.05
    b.box(gx0, gy0, gz0, gx1, gy0 + t, gz0 + 0.3, YELLOW)
    b.box(gx0, gy1 - 0.22, gz0, gx1, gy1, gz0 + 0.3, YELLOW)
    b.box(gx0, gy0 + t, gz0, gx0 + t, gy1 - 0.22, gz0 + 0.3, YELLOW)
    b.box(gx1 - t, gy0 + t, gz0, gx1, gy1 - 0.22, gz0 + 0.3, YELLOW)
    zq = gz0 + 0.025
    b.quad((gx0 + t, gy1 - 0.22, zq), (gx0 + t, gy0 + t, zq), (gx1 - t, gy0 + t, zq), (gx1 - t, gy1 - 0.22, zq), "grating")
    front_sign(b, gz0, 1.55, 2.5, gy1 - 0.2, gy1 - 0.02, plate("CAUTION  STARTS AUTOMATICALLY", 0.95, 0.18, "yellow", 26))
    labelled(b, COMPRESSOR_TAGS, lambda tag: front_sign(b, gz0, 0.6, 1.5, gy1 - 0.2, gy1 - 0.02, plate(f"AIR COMPRESSOR {tag}", 0.9, 0.18, "blue", 26)))
    # The belts (static: they run round the pulleys) and the motor pulley's hub; the flywheel's own is the game's.
    belt_top = ((mx, my + 0.12), (px, py + 0.42))
    belt_bot = ((mx, my - 0.12), (px, py - 0.42))
    for (a0, a1) in (belt_top, belt_bot):
        ln = math.hypot(a1[0] - a0[0], a1[1] - a0[1])
        u = ((a1[0] - a0[0]) / ln, (a1[1] - a0[1]) / ln, 0)
        b.obox(((a0[0] + a1[0]) / 2, (a0[1] + a1[1]) / 2, 0.45), u, (0, 0, 1), ln / 2, 0.05, 0.012, BLACK)
    b.cyl((mx, my, 0.4), (mx, my, 0.52), 0.12, "gunmetal", DARK, 12)
    b.cyl((mx, my, 0.52), (mx, my, 0.66), 0.05, "stainless_dark", DARK, 8)
    live["pulley"] = {"c": _r(px, py, 0.45), "r": 0.42, "w": 0.1}
    # The aftercooler on top of the guard; the discharge out of its end, back along the right through the check valve,
    # down into the floor; the drain trap.
    b.box(1.55, gy1, gz0 + 0.02, 2.55, gy1 + 0.32, gz0 + 0.28, DARK)
    b.box(1.58, gy1 + 0.02, gz0 + 0.0, 2.52, gy1 + 0.3, gz0 + 0.02, "fins")
    run(b, [(hx + 0.12, hy, 1.12), (2.62, hy, 1.12), (2.62, gy1 + 0.16, 1.12), (2.62, gy1 + 0.16, gz0 + 0.14), (2.55, gy1 + 0.16, gz0 + 0.14)], 0.035, "copper", joints=DARK, sides=8)
    run(b, [(2.55, gy1 + 0.1, gz0 + 0.2), (2.75, gy1 + 0.1, gz0 + 0.2), (2.75, gy1 + 0.1, 1.75), (2.75, 0.0, 1.75)], 0.04, AIR_BLUE, joints=DARK, sides=8)
    b.cyl((2.75, gy1 + 0.1, 1.2), (2.75, gy1 + 0.1, 1.42), 0.065, "gunmetal", DARK, 10)
    b.box(2.71, gy1 + 0.16, 1.26, 2.79, gy1 + 0.26, 1.36, "gunmetal")
    seal(b, 2.75, 1.75, 0.04)
    # The station on its post at the front left, and the gauge board on its post at the front right.
    b.box(0.26, 0.12, 0.17, 0.32, 1.0, 0.23, DARK)
    b.box(0.08, 1.0, 0.1, 0.5, 1.5, 0.26, "paint_grey")
    for k, (name, colour) in enumerate((("running", "red"), ("stopped", "green"))):
        x = 0.19 + k * 0.2
        b.cyl((x, 1.36, 0.1), (x, 1.36, 0.08), 0.035, DARK, DARK, 8)
        live["lamps"].append({"name": name, "c": _r(x, 1.36, 0.077), "r": 0.025, "colour": colour, "on": C.LENS[colour], "off": C.LENS["off_" + colour]})
    front_sign(b, 0.1, 0.11, 0.47, 1.25, 1.3, plate("RUN      STOP", 0.36, 0.05, "white"), d=0.004)
    b.cyl((0.29, 1.13, 0.1), (0.29, 1.13, 0.07), 0.05, BLACK, DARK, 10)
    b.box(0.28, 1.1, 0.04, 0.3, 1.18, 0.07, WHITE)
    front_sign(b, 0.1, 0.13, 0.45, 1.02, 1.06, plate("HAND   OFF   AUTO", 0.32, 0.04, "white"), d=0.004)
    b.box(2.69, 0.12, 0.2, 2.75, 1.0, 0.26, DARK)
    b.box(2.56, 1.0, 0.14, 2.88, 1.36, 0.2, "paint_grey")
    gauge(b, live, "DISCH PRESS", (2.72, 1.2, 0.14), 0.11, L.dial_face_tex("PSIG", 0, 400, 5, 0.11 * 1.36, 270))
    front_sign(b, 0.14, 2.58, 2.86, 1.04, 1.08, plate("DISCHARGE", 0.28, 0.04, "white"), d=0.004)
    run(b, [(2.72, 1.2, 0.2), (2.72, 1.2, 0.5), (2.72, gy1 + 0.1, 0.5)], 0.012, "stainless", joints="stainless", sides=6)
    b.live = live
    live["mats"] = {"needle": sw("red"), "spoke": "gunmetal", "rim": "gunmetal", "hub": "stainless_dark"}
    on_floor(b)
    return b


def placard_or(text):
    return plate(text, 0.26, 0.19, "red")


def air_dryer(seed=317):
    with own_atlas():
        return _air_dryer(seed)


def _air_dryer(seed=317):
    """The starting air dryer, 2 wide, 1 deep, 2 tall: twin desiccant towers on a frame, the switching valves under them
    and the purge manifold over them, the prefilter on the inlet (left) and the afterfilter on the outlet (right), each
    up out of the floor; the controller (its POWER lamp the game's) and the moisture indicator."""
    b = C.Board(2, seed, face=0.0)
    live = {"gauges": [], "lamps": []}
    b.box(0.04, 0.0, 0.04, 1.96, 0.08, 0.96, "concrete")
    for x in (0.4, 1.54):
        b.box(x, 0.08, 0.3, x + 0.06, 0.32, 0.36, DARK)
        b.box(x, 0.08, 0.84, x + 0.06, 0.32, 0.9, DARK)
    b.box(0.4, 0.32, 0.3, 1.6, 0.36, 0.9, DARK)
    towers = (0.72, 1.28)
    for x in towers:
        rev(b, (x, 0.36, 0.62), (x, 1.75, 0.62), [(0, 0.15), (0.06, 0.19), (1.27, 0.19), (1.39, 0.1)], "paint_blue", None, sw("blue"), 16)
    b.core = len(b.still)
    # The switching valves under the towers, the purge manifold over them.
    b.box(0.55, 0.36, 0.5, 1.45, 0.5, 0.74, "gunmetal")
    for x in towers:
        b.box(x - 0.05, 0.5, 0.46, x + 0.05, 0.56, 0.5, sw("red"))
    run(b, [(towers[0], 1.75, 0.62), (towers[0], 1.86, 0.62), (towers[1], 1.86, 0.62), (towers[1], 1.75, 0.62)], 0.035, "stainless", joints=DARK, sides=8)
    b.box(0.95, 1.81, 0.57, 1.05, 1.93, 0.67, "gunmetal")
    # The prefilter (left) and the afterfilter (right): canisters on the lines up out of the floor, across into the
    # valve block.
    for x, legend in ((0.18, "IN"), (1.82, "OUT")):
        b.cyl((x, 0.0, 0.5), (x, 0.95, 0.5), 0.035, AIR_BLUE, AIR_BLUE, 8)
        b.cyl((x, 0.62, 0.5), (x, 0.95, 0.5), 0.065, "stainless", sw("enamel"), 10)
        b.cyl((x, 0.95, 0.5), (x, 1.05, 0.5), 0.075, "gunmetal", DARK, 10)
        inner = 0.55 if x < 1 else 1.45
        run(b, [(x, 1.05, 0.5), (x, 1.12, 0.5), (x, 1.12, 0.62), (inner + (0.04 if x < 1 else -0.04), 1.12, 0.62), (inner + (0.04 if x < 1 else -0.04), 0.5, 0.62)],
            0.03, AIR_BLUE, joints=DARK, sides=6)
        seal(b, x, 0.5, 0.035)
        front_sign(b, 0.42, x - 0.08, x + 0.08, 0.36, 0.44, plate(legend, 0.16, 0.08, "blue"), d=0.006)
        b.box(x - 0.03, 0.08, 0.38, x + 0.03, 0.62, 0.42, DARK)
        b.box(x - 0.03, 0.55, 0.42, x + 0.03, 0.6, 0.47, DARK)
    # The controller on the front between the towers: its POWER lamp, the moisture indicator's window.
    b.box(0.75, 0.85, 0.24, 1.25, 1.35, 0.44, "paint_grey")
    b.cyl((0.88, 1.2, 0.24), (0.88, 1.2, 0.22), 0.03, DARK, DARK, 8)
    live["lamps"].append({"name": "power", "c": _r(0.88, 1.2, 0.217), "r": 0.02, "colour": "white", "on": C.LENS["white"], "off": C.LENS["off_white"]})
    front_sign(b, 0.24, 0.94, 1.2, 1.16, 1.24, plate("POWER", 0.26, 0.08, "white"), d=0.004)
    b.cyl((1.1, 1.0, 0.24), (1.1, 1.0, 0.21), 0.05, BLACK, sw("lblue"), 10)
    front_sign(b, 0.24, 0.8, 1.02, 0.96, 1.04, plate("MOISTURE", 0.22, 0.08, "white"), d=0.004)
    labelled(b, DRYER_TAGS, lambda tag: front_sign(b, 0.24, 0.77, 1.23, 1.26, 1.34, plate(f"AIR DRYER {tag}", 0.46, 0.08, "blue"), d=0.006))
    b.live = live
    live["mats"] = {"needle": sw("red")}
    on_floor(b)
    return b


def legend_image():
    """The diesel room's legend texture: every piece built, its drawings packed."""
    for v in BOARDS.values():
        v[4]()
    return ATLAS.pack()


BOARDS = {"emergency_diesel_generator": ("Emergency Diesel Generator", W, D, T, diesel_set),
          "edg_air_receiver": ("Starting Air Receiver", 2, 2, 5, air_receiver),
          "edg_air_compressor": ("Starting Air Compressor", 3, 2, 2, air_compressor),
          "edg_air_dryer": ("Starting Air Dryer", 2, 1, 2, air_dryer)}
C.SECTIONS.update({k: v[4] for k, v in BOARDS.items()})
import diesel_room  # noqa: E402,F401  (round 31 part 3: the diesel rooms' own plant)
