"""Machinery details for the reactor building's pieces (board operations, tools/control_boards.py): the parts real
plant machinery is made of, drawn in 3D so a close-up reads as the real thing (jugcraft-detail-standard.md: real 3D
parts, not pictures).

Every function draws into a Board (reactor_head.Head) and a list (lst: the piece's still list by default, or a moving
group's). Axes are unit vectors along x, y or z; boxes along them are oriented boxes so one function serves any
direction. Fasteners and round parts are cylinders (the overlap check leaves them out; their ends stand clear of the
faces they sit on).

- Fasteners: hex bolts (with washers), bolt rows and circles, grease nipples.
- Drive train: TEFC motors (finned body, fan cowl with its grille, terminal box, feet, lifting eye, nameplate),
  parallel-shaft gear reducers (split housing with its bolted flange, ribs, breather, oil sight glass, drain plug,
  shaft bosses), disc brakes (disc, caliper, thruster), couplings and their guards, pillow-block bearings.
- Rope drums (flanges, grooves, the rope's wraps), cable and hose reels (flanged, wound, spring housing, swivel).
- Steel: I-beams, channels, angles, gusset plates; crane rail (foot, web, head) on its sole plate with bolted clamps,
  fishplated joints; spring buffers; end stops.
- Fittings: handrails (posts on base plates, top and knee rails, toe plates, gaps for gates and ladders), grating decks
  with their frames, ladders with standoffs, cabinets (door, hinges, handle, louvres, gland plate, tag), conduits.
"""
import math

import control_boards as CB

YELLOW = "paint_yellow"


# ---------------------------------------------------------------- small vector helpers

def add(*vs):
    return tuple(sum(v[i] for v in vs) for i in range(3))


def mul(v, s):
    return tuple(c * s for c in v)


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def unit(a):
    l = math.sqrt(sum(c * c for c in a)) or 1.0
    return tuple(c / l for c in a)


def frame(d):
    """Two unit vectors square to the axis d (e1 up where d is level)."""
    d = unit(d)
    e1 = (0.0, 1.0, 0.0) if abs(d[1]) < 0.9 else (1.0, 0.0, 0.0)
    e2 = unit(cross(d, e1))
    e1 = unit(cross(e2, d))
    return d, e1, e2


def obox(b, lst, c, u, v, hu, hv, hw, faces):
    """An oriented box (half sizes along u, v and u x v)."""
    b.obox(c, unit(u), unit(v), hu, hv, hw, faces, lst)


def cyl(b, lst, a, c, r, mat, cap=None, sides=10, both=None):
    b.cyl(a, c, r, mat, cap, sides, lst=lst, both=both)


# ---------------------------------------------------------------- fasteners

def bolt(b, lst, c, n, r=0.014, h=0.01, washer=True, mat="gunmetal"):
    """A hex bolt head (or nut) on a face: c on the face, n the face's outward normal."""
    n = unit(n)
    if washer:
        cyl(b, lst, c, add(c, mul(n, 0.003)), r * 1.45, "stainless_dark", sides=10, both=False)
        c = add(c, mul(n, 0.003))
    cyl(b, lst, c, add(c, mul(n, h)), r, mat, sides=6, both=False)


def bolt_row(b, lst, p0, p1, n, count, r=0.012, h=0.008, washer=False):
    for k in range(count):
        t = (k + 0.5) / count
        bolt(b, lst, add(mul(p0, 1 - t), mul(p1, t)), n, r, h, washer)


def bolt_circle(b, lst, c, n, R, count, r=0.01, h=0.007, a0=0.0):
    _, e1, e2 = frame(n)
    for k in range(count):
        a = a0 + 2 * math.pi * k / count
        bolt(b, lst, add(c, mul(e1, R * math.cos(a)), mul(e2, R * math.sin(a))), n, r, h, False)


def grease_nipple(b, lst, c, n):
    cyl(b, lst, c, add(c, mul(n, 0.02)), 0.008, "copper", sides=6, both=False)
    cyl(b, lst, add(c, mul(n, 0.02)), add(c, mul(n, 0.03)), 0.011, "copper", sides=6, both=False)


# ---------------------------------------------------------------- drive train

def motor(b, lst, p0, d, length, r, colour="paint_blue", feet_to=None, fins=12, tag=None, terminal_side=1, brake=False):
    """A totally enclosed fan-cooled motor: its shaft end at p0, the body along d. Finned frame, end shields, the fan
    cowl at the far end with its grille, the terminal box on top, the lifting eye, a nameplate; feet down to the height
    feet_to (or no feet: flange mounted)."""
    d, e1, e2 = frame(d)
    body0 = add(p0, mul(d, 0.05))
    body1 = add(p0, mul(d, length * 0.74))
    cowl1 = add(p0, mul(d, length))
    # the shaft and its key
    cyl(b, lst, add(p0, mul(d, -0.07)), add(p0, mul(d, 0.01)), r * 0.16, "stainless", "stainless", 10, both=True)
    # drive end shield (a stepped bell), the finned frame, the far end shield
    cyl(b, lst, p0, body0, r * 0.86, colour, "gunmetal", 16, both=True)
    cyl(b, lst, body0, body1, r * 0.92, colour, colour, 18, both=True)
    L = length * 0.74 - 0.05
    mid = add(p0, mul(d, 0.05 + L / 2))
    for k in range(fins):
        a = 2 * math.pi * (k + 0.5) / fins
        rad = add(mul(e1, math.cos(a)), mul(e2, math.sin(a)))
        if feet_to is not None and math.cos(a) < -0.75:
            continue                                  # no fins where the feet are
        c = add(mid, mul(rad, r * 0.92 + r * 0.06))
        obox(b, lst, c, d, rad, L / 2 - 0.01, r * 0.07, r * 0.025, colour)
    # a brake motor's brake housing (red) between the frame and the fan cowl
    if brake:
        cyl(b, lst, add(body1, mul(d, -0.07)), add(body1, mul(d, -0.005)), r * 1.0, "red_iron", "red_iron", 18, both=True)
    # fan cowl and its grille end
    cyl(b, lst, body1, add(body1, mul(d, 0.02)), r * 1.0, colour, colour, 18, both=True)
    cyl(b, lst, add(body1, mul(d, 0.02)), cowl1, r * 1.0, colour, ["louvre", 0.05, 0.05, 0.95, 0.95], 18, both=False)
    cyl(b, lst, add(body1, mul(d, 0.02)), add(body1, mul(d, 0.0201)), r * 1.0, colour, colour, 18, both=False)
    # terminal box on top (to one side), its cable gland
    tb = add(mid, mul(e1, r * 1.0 + 0.045), mul(e2, terminal_side * r * 0.1), mul(d, -L * 0.15))
    obox(b, lst, tb, d, e1, r * 0.42, 0.05, r * 0.36, colour)
    obox(b, lst, add(tb, mul(e1, 0.052)), d, e1, r * 0.38, 0.004, r * 0.32, "gunmetal")
    bolt_row(b, lst, add(tb, mul(e1, 0.056), mul(d, -r * 0.34), mul(e2, r * 0.27)),
             add(tb, mul(e1, 0.056), mul(d, r * 0.34), mul(e2, r * 0.27)), e1, 2, 0.007, 0.004)
    bolt_row(b, lst, add(tb, mul(e1, 0.056), mul(d, -r * 0.34), mul(e2, -r * 0.27)),
             add(tb, mul(e1, 0.056), mul(d, r * 0.34), mul(e2, -r * 0.27)), e1, 2, 0.007, 0.004)
    g = add(tb, mul(e2, terminal_side * r * 0.36))
    cyl(b, lst, g, add(g, mul(e2, terminal_side * 0.035)), 0.022, "black", "black", 8, both=True)
    # lifting eye
    le = add(mid, mul(e1, r * 1.0), mul(d, L * 0.25))
    cyl(b, lst, le, add(le, mul(e1, 0.04)), 0.01, "gunmetal", "gunmetal", 6, both=False)
    cyl(b, lst, add(le, mul(e1, 0.05), mul(d, -0.022)), add(le, mul(e1, 0.05), mul(d, 0.022)), 0.016, "gunmetal", "gunmetal", 8, both=True)
    # nameplate on the side
    npc = add(mid, mul(e2, -terminal_side * (r * 0.98 + r * 0.13 + 0.002)), mul(d, L * 0.12))
    nu = d if terminal_side > 0 else mul(d, -1)          # front (-(u x v)) faces away from the terminal box
    obox(b, lst, npc, nu, e1, min(0.06, L * 0.2), 0.035, 0.002, [["nameplate", 0.02, 0.02, 0.98, 0.98], None] + ["gunmetal"] * 4)
    if feet_to is not None:
        base = feet_to
        for t in (0.2, 0.8):
            fc = add(p0, mul(d, 0.05 + L * t))
            yb = fc[1] - r * 0.9
            hgt = yb - base
            if hgt > 0.02:
                c = (fc[0], base + 0.012 + (hgt - 0.012) / 2, fc[2])
                obox(b, lst, c, d, (0, 1, 0), min(0.04, L * 0.2), (hgt - 0.012) / 2, r * 0.75, colour)
            obox(b, lst, (fc[0], base + 0.006, fc[2]), d, (0, 1, 0), min(0.06, L * 0.28), 0.006, r * 0.95, colour)
            for s in (-1, 1):
                bolt(b, lst, add((fc[0], base + 0.012, fc[2]), mul(e2, s * r * 0.82)), (0, 1, 0), 0.01, 0.008, False)
    if tag:
        tag(add(mid, mul(e2, -terminal_side * (r * 1.0 + 0.002))), mul(e2, -terminal_side))


def gearbox(b, lst, c, d, hd, hu, hs, colour="paint_grey", bosses=()):
    """A parallel-shaft gear reducer: housing centred at c, d along the shafts' line of centres... here d is the shafts'
    axis; half sizes hd along d, hu up, hs sideways. Split at its middle height with a bolted flange, ribs on its
    sides, a breather on top, an oil sight glass and a drain plug on its side; shaft bosses at bosses: (offset up,
    offset side, which end +1/-1, radius)."""
    d, e1, e2 = frame(d)
    obox(b, lst, c, d, e1, hd, hu, hs, colour)
    # split flange round the middle (a band standing out a little)
    obox(b, lst, add(c, mul(e1, -0.02)), d, e1, hd + 0.018, 0.022, hs + 0.018, colour)
    for s in (-1, 1):
        bolt_row(b, lst, add(c, mul(e1, 0.002), mul(e2, s * (hs + 0.018)), mul(d, -hd)),
                 add(c, mul(e1, 0.002), mul(e2, s * (hs + 0.018)), mul(d, hd)), mul(e2, s), max(2, int(hd * 2 / 0.09)), 0.009, 0.006)
    # ribs down the sides (below the flange)
    for s in (-1, 1):
        for t in (-0.6, 0.0, 0.6):
            rc = add(c, mul(d, hd * t), mul(e2, s * (hs + 0.012)), mul(e1, -hu * 0.55))
            obox(b, lst, rc, d, e1, 0.012, hu * 0.42, 0.012, colour)
    # breather on top, sight glass and drain plug on the side
    top = add(c, mul(e1, hu), mul(d, hd * 0.4))
    cyl(b, lst, top, add(top, mul(e1, 0.03)), 0.012, "gunmetal", "gunmetal", 8, both=False)
    cyl(b, lst, add(top, mul(e1, 0.03)), add(top, mul(e1, 0.05)), 0.024, "gunmetal", "gunmetal", 10, both=True)
    sg = add(c, mul(e2, hs), mul(e1, -hu * 0.2), mul(d, -hd * 0.5))
    cyl(b, lst, sg, add(sg, mul(e2, 0.012)), 0.026, "gunmetal", "gunmetal", 12, both=False)
    cyl(b, lst, add(sg, mul(e2, 0.012)), add(sg, mul(e2, 0.014)), 0.018, "gunmetal", ["glass", 0.2, 0.2, 0.8, 0.8], 12, both=False)
    dp = add(c, mul(e2, hs), mul(e1, -hu * 0.85), mul(d, hd * 0.5))
    cyl(b, lst, dp, add(dp, mul(e2, 0.015)), 0.014, "gunmetal", "gunmetal", 6, both=False)
    for up, side, end, rr in bosses:
        bc = add(c, mul(e1, up), mul(e2, side), mul(d, end * hd))
        cyl(b, lst, bc, add(bc, mul(d, end * 0.035)), rr, colour, "gunmetal", 14, both=False)
        bolt_circle(b, lst, add(bc, mul(d, end * 0.035)), mul(d, end), rr * 0.75, 6, 0.007, 0.005)


def disc_brake(b, lst, c, d, r, side=1):
    """A disc brake on a shaft: the disc, the caliper over its top edge, the thruster (release) behind."""
    d, e1, e2 = frame(d)
    cyl(b, lst, add(c, mul(d, -0.012)), add(c, mul(d, 0.012)), r, "stainless_dark", "stainless_dark", 18, both=True)
    cyl(b, lst, add(c, mul(d, -0.03)), add(c, mul(d, 0.03)), r * 0.3, "gunmetal", "gunmetal", 10, both=True)
    cc = add(c, mul(e1, r * 0.85))
    obox(b, lst, cc, d, e1, 0.04, r * 0.3, r * 0.32, "red_iron")
    th = add(cc, mul(e2, side * (r * 0.32 + 0.06)), mul(e1, -r * 0.1))
    cyl(b, lst, add(th, mul(e1, -r * 0.4)), add(th, mul(e1, r * 0.35)), 0.04, "red_iron", "gunmetal", 10, both=True)


def coupling_guard(b, lst, a, c, r):
    """A flexible coupling between two shafts, under a yellow guard (a bent plate over the top and sides)."""
    d = unit(tuple(c[i] - a[i] for i in range(3)))
    d, e1, e2 = frame(d)
    L = math.dist(a, c)
    m = mul(add(a, c), 0.5)
    cyl(b, lst, add(m, mul(d, -L * 0.3)), add(m, mul(d, L * 0.3)), r * 0.6, "gunmetal", "gunmetal", 12, both=True)
    obox(b, lst, add(m, mul(e1, r * 0.95)), d, e1, L / 2, 0.006, r, YELLOW)
    for s in (-1, 1):
        obox(b, lst, add(m, mul(e2, s * r), mul(e1, r * 0.1)), d, e1, L / 2 - 0.003, r * 0.84, 0.006, YELLOW)


def pillow_block(b, lst, c, d, r_shaft, base_y=None):
    """A pillow-block bearing round a shaft (axis d, centre c): its housing, cap, two hold-down bolts on its foot, a
    grease nipple; down to base_y."""
    d, e1, e2 = frame(d)
    yb = c[1] - r_shaft * 2.2 if base_y is None else base_y
    h = c[1] - yb
    obox(b, lst, (c[0], yb + h * 0.4, c[2]), d, (0, 1, 0), r_shaft * 1.3, h * 0.4, r_shaft * 2.6, "gunmetal")
    cyl(b, lst, add(c, mul(d, -r_shaft * 1.25)), add(c, mul(d, r_shaft * 1.25)), r_shaft * 1.9, "gunmetal", "gunmetal", 14, both=True)
    for s in (-1, 1):
        bolt(b, lst, (c[0] + e2[0] * s * r_shaft * 2.2, yb + h * 0.8, c[2] + e2[2] * s * r_shaft * 2.2), (0, 1, 0), 0.01, 0.008, False)
    grease_nipple(b, lst, add(c, mul(e1, r_shaft * 1.9)), e1)


def rope_drum(b, lst, a, c, r, rope_r=0.011, wraps=(0.15, 0.85), mat="gunmetal"):
    """A grooved rope drum from a to c: its shell, end flanges, the rope's wraps over part of its length (wraps:
    fractions where the rope lies), the hub ends."""
    d = unit(tuple(c[i] - a[i] for i in range(3)))
    L = math.dist(a, c)
    cyl(b, lst, a, c, r, mat, "gunmetal", 20, both=True)
    for e, s in ((a, 1), (c, -1)):
        cyl(b, lst, e, add(e, mul(d, s * 0.025)), r + 0.05, "gunmetal", "gunmetal", 20, both=True)
        bolt_circle(b, lst, add(e, mul(d, -s * 0.0)), mul(d, -s), r * 0.6, 8, 0.008, 0.005)
    pitch = rope_r * 2.3
    t0, t1 = wraps
    x = L * t0
    while x < L * t1:
        p = add(a, mul(d, x))
        cyl(b, lst, add(p, mul(d, -rope_r)), add(p, mul(d, rope_r)), r + rope_r, "stainless_dark", "stainless_dark", 16, both=False)
        x += pitch


def reel(b, lst, a, c, r_core, r_flange, wound_r, mat, frame_mat="paint_grey", spring_side=1):
    """A spring-loaded hose or cable reel from a to c: the core with its wound layers, two flanges with spokes, the
    spring housing at one end, the swivel joint at the other."""
    d = unit(tuple(c[i] - a[i] for i in range(3)))
    L = math.dist(a, c)
    cyl(b, lst, add(a, mul(d, 0.02)), add(c, mul(d, -0.02)), r_core, "gunmetal", "gunmetal", 16, both=True)
    # the wound hose or cable: layered rings
    k = 0
    pitch = 0.042
    x = 0.04
    while x < L - 0.04:
        p = add(a, mul(d, x))
        cyl(b, lst, add(p, mul(d, -0.0205)), add(p, mul(d, 0.0205)), wound_r, mat, mat, 18, both=False)
        x += pitch
        k += 1
    _, e1, e2 = frame(d)
    for e, s in ((a, 1), (c, -1)):
        cyl(b, lst, e, add(e, mul(d, s * 0.016)), r_flange, frame_mat, frame_mat, 22, both=True)
        for j in range(6):
            ang = 2 * math.pi * j / 6
            rad = add(mul(e1, math.cos(ang)), mul(e2, math.sin(ang)))
            obox(b, lst, add(e, mul(d, -s * 0.01), mul(rad, (r_flange + r_core) / 2)), rad, d, (r_flange - r_core) / 2, 0.006, 0.014, frame_mat)
    sp = c if spring_side > 0 else a
    s = 1 if spring_side > 0 else -1
    cyl(b, lst, add(sp, mul(d, s * 0.03)), add(sp, mul(d, s * 0.13)), r_core * 1.35, frame_mat, frame_mat, 16, both=True)
    bolt_circle(b, lst, add(sp, mul(d, s * 0.13)), mul(d, s), r_core * 1.0, 6, 0.008, 0.005)
    sw_ = a if spring_side > 0 else c
    cyl(b, lst, add(sw_, mul(d, -s * 0.03)), add(sw_, mul(d, -s * 0.1)), 0.035, "copper", "copper", 10, both=True)


# ---------------------------------------------------------------- steel

def ibeam(b, lst, a, c, up, h, w, tf=0.02, tw=0.014, mat="gunmetal"):
    """An I-beam from a to c, its web along up, h deep and w wide."""
    d = unit(tuple(c[i] - a[i] for i in range(3)))
    up = unit(up)
    side = unit(cross(d, up))
    L = math.dist(a, c)
    m = mul(add(a, c), 0.5)
    obox(b, lst, add(m, mul(up, h / 2 - tf / 2)), d, up, L / 2, tf / 2, w / 2, mat)
    obox(b, lst, add(m, mul(up, -h / 2 + tf / 2)), d, up, L / 2, tf / 2, w / 2, mat)
    obox(b, lst, m, d, up, L / 2 - 0.0005, h / 2 - tf, tw / 2, mat)


def channel(b, lst, a, c, up, h, w, toward, tf=0.016, tw=0.012, mat="gunmetal"):
    """A channel from a to c, web along up, its flanges pointing toward (a unit vector square to d and up)."""
    d = unit(tuple(c[i] - a[i] for i in range(3)))
    up = unit(up)
    t = unit(toward)
    L = math.dist(a, c)
    m = mul(add(a, c), 0.5)
    obox(b, lst, m, d, up, L / 2, h / 2, tw / 2, mat)
    for s in (-1, 1):
        obox(b, lst, add(m, mul(up, s * (h / 2 - tf / 2)), mul(t, w / 2)), d, up, L / 2 - 0.0005, tf / 2, w / 2 - tw / 2 - 0.0005, mat)


def crane_rail(b, lst, x0, x1, y, z, along=(1, 0, 0), clamp_pitch=0.6, joint_pitch=6.0):
    """A crane rail from x0 to x1 (along x) on the floor at y, centred on z: the sole plate on its grout, the rail
    (foot, web, head), bolted clamps both sides, fishplated joints. Returns the head's top."""
    # grout pad and sole plate
    b.box(x0 - 0.02, y, z - 0.19, x1 + 0.02, y + 0.012, z + 0.19, "concrete", lst)
    b.box(x0, y + 0.012, z - 0.16, x1, y + 0.032, z + 0.16, "gunmetal", lst)
    ys = y + 0.032
    # rail: foot, web, head (A75 profile, scaled)
    b.box(x0, ys, z - 0.075, x1, ys + 0.016, z + 0.075, "stainless_dark", lst)
    b.box(x0, ys + 0.016, z - 0.02, x1, ys + 0.08, z + 0.02, "stainless_dark", lst)
    b.box(x0, ys + 0.08, z - 0.037, x1, ys + 0.118, z + 0.037, "stainless_dark", lst)
    top = ys + 0.118
    # clamps: a clamp plate over the foot's edge each side, two bolts each
    x = x0 + clamp_pitch / 2
    while x < x1 - 0.1:
        for s in (-1, 1):
            za, zb = (z + 0.06, z + 0.13) if s > 0 else (z - 0.13, z - 0.06)
            lo = (z + 0.076, z + 0.13) if s > 0 else (z - 0.13, z - 0.076)
            b.box(x - 0.05, ys, lo[0], x + 0.05, ys + 0.016, lo[1], "gunmetal", lst)
            b.box(x - 0.05, ys + 0.016, za, x + 0.05, ys + 0.026, zb, "gunmetal", lst)
            for dx in (-0.028, 0.028):
                bolt(b, lst, (x + dx, ys + 0.026, (za + zb) / 2 + s * 0.012), (0, 1, 0), 0.011, 0.012, True)
        x += clamp_pitch
    # joints: a fine gap line and a fishplate each side of the web with four bolts
    xj = x0 + joint_pitch
    while xj < x1 - 0.5:
        for s in (-1, 1):
            zf = z + s * 0.02
            za, zb = (zf, zf + 0.016) if s > 0 else (zf - 0.016, zf)
            b.box(xj - 0.22, ys + 0.022, za, xj + 0.22, ys + 0.074, zb, "gunmetal", lst)
            for dx in (-0.16, -0.06, 0.06, 0.16):
                bolt(b, lst, (xj + dx, ys + 0.048, zb if s > 0 else za), (0, 0, s), 0.011, 0.012, False)
        xj += joint_pitch
    return top


def spring_buffer(b, lst, c, d, length=0.32, r=0.09):
    """A spring buffer on its mounting plate at c (the plate's face, d outward): plate with four bolts, the housing,
    the plunger, the rubber pad, hazard stripes on the housing's band."""
    d, e1, e2 = frame(d)
    obox(b, lst, add(c, mul(d, 0.012)), d, e1, 0.012, r * 1.5, r * 1.5, "gunmetal")
    for s1 in (-1, 1):
        for s2 in (-1, 1):
            bolt(b, lst, add(c, mul(d, 0.024), mul(e1, s1 * r * 1.15), mul(e2, s2 * r * 1.15)), d, 0.011, 0.01, False)
    h0 = add(c, mul(d, 0.024))
    cyl(b, lst, h0, add(h0, mul(d, length * 0.55)), r, YELLOW, YELLOW, 14, both=False)
    cyl(b, lst, add(h0, mul(d, length * 0.25)), add(h0, mul(d, length * 0.35)), r * 1.04, "hazard", "hazard", 14, both=False)
    cyl(b, lst, add(h0, mul(d, length * 0.55)), add(h0, mul(d, length * 0.85)), r * 0.6, "stainless", "stainless", 12, both=False)
    cyl(b, lst, add(h0, mul(d, length * 0.85)), add(h0, mul(d, length)), r * 0.95, "rubber", "rubber", 14, both=False)


def gusset(b, lst, corner, a_dir, b_dir, size, t=0.01, mat="gunmetal"):
    """A triangular gusset plate in the corner between two faces (a_dir and b_dir along them from the corner)."""
    a_dir, b_dir = unit(a_dir), unit(b_dir)
    n = unit(cross(a_dir, b_dir))
    for s in (1, -1):
        off = mul(n, s * t / 2)
        p0 = add(corner, off)
        p1 = add(corner, mul(a_dir, size), off)
        p2 = add(corner, mul(b_dir, size), off)
        if s > 0:
            b.quad(p2, p0, p1, p1, mat, lst)
        else:
            b.quad(p1, p0, p2, p2, mat, lst)


# ---------------------------------------------------------------- fittings

def handrail(b, lst, pts, y, gaps=(), post_pitch=1.2, toe=True, mat=YELLOW, closed=False):
    """A handrail along the polyline pts (x, z) at floor height y: posts on base plates, top rail at 1.07 and knee rail
    at 0.53, a toe plate (0.1) along the floor's edge. gaps: (segment index, t0, t1) left open (gates, ladders)."""
    segs = list(zip(pts, pts[1:] + ([pts[0]] if closed else [])))
    for k, (p, q) in enumerate(segs):
        L = math.dist(p, q)
        if L < 1e-6:
            continue
        spans = [(0.0, 1.0)]
        for gk, g0, g1 in gaps:
            if gk == k:
                new = []
                for a0, a1 in spans:
                    if g1 <= a0 or g0 >= a1:
                        new.append((a0, a1))
                    else:
                        if g0 > a0:
                            new.append((a0, g0))
                        if g1 < a1:
                            new.append((g1, a1))
                spans = new
        for a0, a1 in spans:
            pa = (p[0] + (q[0] - p[0]) * a0, p[1] + (q[1] - p[1]) * a0)
            pb = (p[0] + (q[0] - p[0]) * a1, p[1] + (q[1] - p[1]) * a1)
            l = math.dist(pa, pb)
            n = max(1, int(round(l / post_pitch)))
            for j in range(n + 1):
                x = pa[0] + (pb[0] - pa[0]) * j / n
                z = pa[1] + (pb[1] - pa[1]) * j / n
                cyl(b, lst, (x, y + 0.012, z), (x, y + 1.07, z), 0.021, mat, mat, 8, both=False)
                cyl(b, lst, (x, y, z), (x, y + 0.012, z), 0.045, mat, mat, 8, both=False)      # base plate
                for ang in (0.785, 3.927):
                    bolt(b, lst, (x + 0.032 * math.cos(ang), y + 0.012, z + 0.032 * math.sin(ang)), (0, 1, 0), 0.007, 0.005, False)
            for hy, r in ((1.07, 0.024), (0.53, 0.019)):
                cyl(b, lst, (pa[0], y + hy, pa[1]), (pb[0], y + hy, pb[1]), r, mat, mat, 8, both=False)
            if toe and l > 0.05:
                dx, dz = (pb[0] - pa[0]) / l, (pb[1] - pa[1]) / l
                c = ((pa[0] + pb[0]) / 2, y + 0.05, (pa[1] + pb[1]) / 2)
                obox(b, lst, c, (dx, 0, dz), (0, 1, 0), l / 2 - 0.006, 0.05, 0.004, mat)


def grating_deck(b, lst, x0, z0, x1, z1, y_top, edge="gunmetal", t=0.04):
    """A grating panel with its angle frame round the edges."""
    b.box(x0 + 0.02, y_top - t, z0 + 0.02, x1 - 0.02, y_top - 0.002, z1 - 0.02, ["grating"] * 6, lst)
    for (a, c) in (((x0, z0), (x1, z0 + 0.02)), ((x0, z1 - 0.02), (x1, z1)), ((x0, z0 + 0.02), (x0 + 0.02, z1 - 0.02)), ((x1 - 0.02, z0 + 0.02), (x1, z1 - 0.02))):
        b.box(a[0], y_top - t - 0.01, a[1], c[0], y_top, c[1], edge, lst)


def ladder(b, lst, x, z, y0, y1, along=(1, 0, 0), half=0.22, top_ext=1.07, standoffs=(), standoff_to=None, wall_dir=None):
    """A fixed steel ladder: two stiles from y0 to y1 + top_ext (grab rails over the landing), rungs at 0.3, stand-off
    brackets back to a wall (standoff_to: the wall's distance along wall_dir from the ladder's line)."""
    a = unit(along)
    for s in (-1, 1):
        p = (x + a[0] * s * half, 0, z + a[2] * s * half)
        cyl(b, lst, (p[0], y0, p[2]), (p[0], y1 + top_ext, p[2]), 0.024, YELLOW, YELLOW, 8, both=False)
        cyl(b, lst, (p[0], y0, p[2]), (p[0], y0 + 0.01, p[2]), 0.05, YELLOW, YELLOW, 8, both=False)
        if standoff_to is not None:
            w = unit(wall_dir)
            for ys in standoffs:
                c = (p[0] + w[0] * standoff_to / 2, ys, p[2] + w[2] * standoff_to / 2)
                obox(b, lst, c, w, (0, 1, 0), standoff_to / 2, 0.025, 0.012, "gunmetal")
    y = y0 + 0.3
    while y < y1 + 0.01:
        cyl(b, lst, (x - a[0] * half, y, z - a[2] * half), (x + a[0] * half, y, z + a[2] * half), 0.016, "gunmetal", "gunmetal", 6, both=False)
        y += 0.3


def cabinet(b, lst, c, n, w, h, dpt, tag=None, colour="paint_grey", louvres=True, hinge_left=True):
    """A steel enclosure standing against a wall: c the centre of its back on the wall, n out of the wall. Door with a
    raised edge, three hinges, a quarter-turn handle and lock, louvres low and high, a gland plate under it."""
    n = unit(n)
    up = (0.0, 1.0, 0.0)
    u = unit(cross(up, n))
    u = mul(u, -1)                    # right as seen from in front (the frame is mirrored: reactor_head notes)
    body = add(c, mul(n, dpt / 2))
    obox(b, lst, body, u, up, w / 2, h / 2, dpt / 2, colour)
    front = add(c, mul(n, dpt))
    # door: a panel standing proud, its hinges, handle, lock
    obox(b, lst, add(front, mul(n, 0.008)), u, up, w / 2 - 0.02, h / 2 - 0.02, 0.008, colour)
    hs = -1 if hinge_left else 1
    for t in (-0.38, 0.0, 0.38):
        hc = add(front, mul(u, hs * (w / 2 - 0.02)), mul(up, t * h), mul(n, 0.008))
        cyl(b, lst, add(hc, mul(up, -0.04)), add(hc, mul(up, 0.04)), 0.011, "gunmetal", "gunmetal", 8, both=True)
    hc = add(front, mul(u, -hs * (w / 2 - 0.07)), mul(n, 0.016))
    cyl(b, lst, hc, add(hc, mul(n, 0.012)), 0.018, "black", "black", 8, both=False)
    obox(b, lst, add(hc, mul(n, 0.02), mul(up, -0.03)), u, up, 0.009, 0.045, 0.006, "black")
    lk = add(hc, mul(up, -0.12))
    cyl(b, lst, lk, add(lk, mul(n, 0.008)), 0.01, "stainless", "gunmetal", 8, both=False)
    if louvres:
        for yy in (-h / 2 + 0.12, h / 2 - 0.12):
            lc = add(front, mul(n, 0.017), mul(up, yy), mul(u, hs * 0.03))
            b.quad(add(lc, mul(u, -w * 0.3), mul(up, 0.05)), add(lc, mul(u, -w * 0.3), mul(up, -0.05)),
                   add(lc, mul(u, w * 0.3), mul(up, -0.05)), add(lc, mul(u, w * 0.3), mul(up, 0.05)), ["louvre", 0.05, 0.05, 0.95, 0.95], lst)
    if tag:
        tag(add(front, mul(n, 0.016), mul(up, h * 0.32), mul(u, hs * 0.03)), n)
    # gland plate and conduits under it
    gp = add(body, mul(up, -h / 2 - 0.01))
    obox(b, lst, gp, u, up, w / 2 - 0.03, 0.01, dpt / 2 - 0.03, "gunmetal")


def conduit(b, lst, pts, r=0.022, mat="gunmetal", clamps=0.8, walls=None):
    """A rigid conduit run through pts (3D), with clamp saddles along it. walls: for each segment, None or (n, d): the
    wall the segment's clamps are strapped back to (n its normal toward the conduit, the wall at n . p = d)."""
    for k, (p, q) in enumerate(zip(pts, pts[1:])):
        cyl(b, lst, p, q, r, mat, mat, 8, both=False)
        L = math.dist(p, q)
        n = int(L / clamps)
        d = unit(tuple(q[i] - p[i] for i in range(3)))
        wall = walls[k] if walls else None
        for j in range(1, n + 1):
            t = j / (n + 1)
            m = add(mul(p, 1 - t), mul(q, t))
            cyl(b, lst, add(m, mul(d, -0.012)), add(m, mul(d, 0.012)), r * 1.35, "stainless_dark", "stainless_dark", 8, both=False)
            if wall:
                wn, wd = unit(wall[0]), wall[1]
                gap = sum(m[i] * wn[i] for i in range(3)) - wd
                if gap > r:
                    ln = gap - r * 0.5 + 0.006           # from the conduit back into the wall a little
                    obox(b, lst, add(m, mul(wn, -(r * 0.5 + ln / 2))), wn, d, ln / 2, 0.012, 0.022, "stainless_dark")
    for p in pts[1:-1]:
        cyl(b, lst, add(p, (0, -r * 1.2, 0)), add(p, (0, r * 1.2, 0)), r * 1.25, mat, mat, 8, both=True)     # the elbow
