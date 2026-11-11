"""The turbine hall's overhead crane (turbine hall part 3; Justin, 2026-10-18: "Works from a pendant"), drawn from its own
parts as tools/turbine_set.py draws the turbine, into the same legend texture (textures/entity/turbine_legends.png).

A turbine building's crane as the Westinghouse four-loop plants have them (a 160-ton main hoist and a 25-ton auxiliary,
over the whole hall from the laydown bay to the generator end): a double box-girder bridge whose girder ends sit on the
two end trucks, each truck on its two wheels on the runway rails, one wheel of each driven by a flange-mounted geared
motor, spring buffers at the trucks' corners, a collector on the north truck taking the crane's power off the runway's
conductor bars; a walkway with its handrail along the bridge's west side, the hoist, trolley and bridge panels and the
main disconnect on it, the trolley's conductor bars on the walkway girder under its top flange; floodlights, travel
beacons and the horn under the girders. The trolley on its own rails along the girders' tops: the main hoist's grooved
drum on its bearing pedestals, the gearbox, the brake and the motor on its base, the rotary limit switch; the upper
sheave block under the deck; the auxiliary hoist's drum, gearbox and motor; the trolley's geared drive, its electrical
cabinet, its collector arm onto the bridge's conductor bars, its buffers. The main hook on eight falls of rope from its
sheave block, the auxiliary hook on two, both hanging between the girders.

The frame (as the boards'): x across the hall (the bridge's span, 42 blocks: an end truck over each runway beam, its
wheels on the beam's middle at x 0.5 and 41.5), y up from the runway beams' tops (the runway's rails 0.15 high), z along
the hall (the bridge's own depth, 6 blocks). What moves is drawn in groups the game shifts (client/CraneParts.java): the
bridge along z, the trolley along x on the bridge, the hooks down below their rest, the ropes stretched from the trolley
to the blocks. The runway (its rails and clips, the conductor bars on the north beam, the end stops) is drawn a block at a
time along as far as the bridge can run (its beams under both trucks)."""
import math
import zlib

import board_live as L
import control_boards as C
from control_boards import sw, _r
from diesel_set import rev, run, bolts
import turbine_set as T
from turbine_set import front_plate, back_plate, side_plate, own_atlas
from feed_train import tag_tex, motor_h

GUN, BLACK, DARK, MID, STEEL = T.GUN, T.BLACK, T.DARK, T.MID, T.STEEL
CRANE = "paint_yellow"
HAZARD = "hazard"
PANEL = T.PANEL
INK = (26, 26, 28)

SPAN, DEEP, TALL = 42, 6, 5
TRUCKS = ((0.08, 0.92), (SPAN - 0.92, SPAN - 0.08))   # the end trucks (x0, x1), over the runway beams
TRUCK_Y = (0.35, 1.25)                                # their bottom and top
TRUCK_Z = (0.3, 5.7)
WHEEL_R, WHEEL_Y = 0.36, 0.15 + 0.36                  # on the runway rail (its top at 0.15)
WHEELS_Z = (0.95, 5.05)
GIRDERS = ((1.3, 2.1), (3.9, 4.7))                    # the two box girders (z0, z1)
G_Y = (0.6, 2.8)                                      # their bottom and top (the top flange 2.8 to 2.86)
RAIL_Y = 2.98                                         # the trolley rails' top
RAILS = (1.7, 4.3)                                    # the trolley rails (z: the girders' middles)
TROLLEY_X = (4.5, 37.5)                               # the trolley's travel (its middle, x)
T_HALF = 2.5                                          # the trolley's half length (x)
DECK_Y = (RAIL_Y + 0.5, RAIL_Y + 0.6)                 # the trolley's deck plate
DRUM = (-1.6, 0.88)                                   # the main hoist's drum (x along it), its axis and radius
DRUM_Y, DRUM_R = DECK_Y[1] + 0.6, 0.55
MAIN_DX = -0.35                                       # the main hook's place on the trolley (x from its middle)
AUX_DX = -2.0                                         # the auxiliary hook's
HOOK_Z = 3.0                                          # both hooks hang midway between the girders
FALLS = [(MAIN_DX + (k - 1.5) * 0.3, HOOK_Z + s * 0.25) for k in range(4) for s in (-1, 1)]
UPPER_BLOCK_Y = DECK_Y[0] - 0.33                      # the main falls' top (under the upper sheave block)
AUX_FALLS = [(AUX_DX, HOOK_Z - 0.12), (AUX_DX, HOOK_Z + 0.12)]
AUX_DRUM_Y, AUX_DRUM_R = DECK_Y[1] + 0.4, 0.28
HOOK_REST = 3.0                                       # the main hook block's top at rest (between the girders)
AUX_REST = 3.1
MAIN_BLOCK = (-0.95, 0.25, 0.95)                      # its x0, x1 (about the trolley), its height
CRANE_TAG = "TB CRANE 1"
# The trolley's conductor bars on girder 1's outer web, under its top flange (y of each), and their face.
BAR_Y = (2.62, 2.5, 2.38, 2.26)
BAR_Z = (GIRDERS[0][0] - 0.16, GIRDERS[0][0] - 0.12)
# The runway's conductor bars on the north beam's hall-side face (x 1), under its top (y of each).
RUN_BAR_Y = (-0.22, -0.34, -0.46, -0.58)
RUN_BAR_X = (1.0, 1.05)


def big_stencil(text, w, h, colour=INK, density=160):
    """Big painted lettering (the crane's capacity, read from the floor): drawn coarser than the tags, the letters as tall
    as the space allows, no plate behind them."""
    pw, ph = L.px(w, density), L.px(h, density)

    def draw(img):
        import random
        img.paste((0, 0, 0, 0), (0, 0, img.width, img.height))
        d = L.ImageDraw.Draw(img)
        L._lines(d, text.split("|"), 0, 0, img.width, img.height, colour + (255,), int(ph * 0.92))
        r = random.Random(zlib.crc32(text.encode()))
        px_ = img.load()
        for _ in range(img.width * img.height // 80):
            x, y = r.randrange(img.width), r.randrange(img.height)
            if px_[x, y][3]:
                px_[x, y] = (0, 0, 0, 0)
    return L.ATLAS.add(f"bigstencil:{pw}x{ph}:{colour}:{text}", pw, ph, draw)


def gmotor(b, c, axis, sgn, length, r, paint=CRANE, cowl=True):
    """A flange-mounted geared motor's motor from c along axis ("x" or "z") in direction sgn: its flange, finned frame,
    terminal box on top, the brake and fan cowl at its far end."""
    x, y, z = c
    u = (sgn, 0, 0) if axis == "x" else (0, 0, sgn)
    p = lambda t: (x + u[0] * t, y, z + u[2] * t)
    b.cyl(c, p(0.04), r + 0.05, paint, paint, 16)
    b.cyl(p(0.04), p(length), r, paint, paint, 16)
    n = max(2, int(length / 0.1))
    for k in range(1, n):
        t = 0.04 + (length - 0.04) * k / n
        b.cyl(p(t), p(t + 0.02), r + 0.025, paint, paint, 16)
    if cowl:
        b.cyl(p(length), p(length + 0.1), r * 1.02, DARK, DARK, 16)
    tx, tz = x + u[0] * length * 0.5, z + u[2] * length * 0.5
    hx, hz = (0.09, 0.07) if axis == "x" else (0.07, 0.09)
    b.box(tx - hx, y + r - 0.01, tz - hz, tx + hx, y + r + 0.1, tz + hz, paint)


def _wheel(b, x, y, z, r, axis="x", w=0.16):
    """A crane wheel on its axle: its tread and its two flanges."""
    d = (w / 2, 0, 0) if axis == "x" else (0, 0, w / 2)
    b.cyl((x - d[0], y, z - d[2]), (x + d[0], y, z + d[2]), r, DARK, MID, 18)
    for s in (-1, 1):
        a = (x + s * d[0], y, z + s * d[2])
        e = (a[0] + s * (0.03 if axis == "x" else 0), y, a[2] + s * (0.03 if axis == "z" else 0))
        b.cyl(a, e, r + 0.05, DARK, DARK, 18)


def _buffer(b, c, axis, sgn, r=0.12, body=0.2, face=0.08):
    """A spring buffer: its striped body out from c along axis in direction sgn, the black rubber face."""
    x, y, z = c
    u = (sgn, 0, 0) if axis == "x" else (0, 0, sgn)
    p = lambda t: (x + u[0] * t, y, z + u[2] * t)
    b.cyl(c, p(0.03), r + 0.05, GUN, GUN, 12)
    b.cyl(p(0.03), p(body), r, HAZARD, HAZARD, 12)
    b.cyl(p(body), p(body + face), r + 0.03, BLACK, BLACK, 12)


def _beacon(b, live, x, y, z, name="crane_beacon"):
    """A travel beacon hung under the bridge: its bracket, the amber lens (lit on four sides and below)."""
    b.box(x - 0.04, y - 0.18, z - 0.04, x + 0.04, y, z + 0.04, DARK)
    b.box(x - 0.09, y - 0.22, z - 0.09, x + 0.09, y - 0.18, z + 0.09, DARK)
    lo, hi = y - 0.42, y - 0.22
    # The lens: a box lit on its four sides and its foot (CraneParts draws it from its corners).
    live["lamps"].append({"name": name, "box": _r(x - 0.08, lo, z - 0.08, x + 0.08, hi, z + 0.08), "faces": [1, 1, 1, 1, 0, 1],
                          "colour": "amber", "on": C.LENS["amber"], "off": C.LENS["off_amber"]})


def bridge():
    with own_atlas():
        return _bridge()


def _bridge():
    b = C.Board(SPAN, 4511, face=0.0)
    live = {"gauges": [], "lamps": []}
    ty0, ty1 = TRUCK_Y
    tz0, tz1 = TRUCK_Z
    for k, (x0, x1) in enumerate(TRUCKS):
        xc = (x0 + x1) / 2
        s = 1 if k == 0 else -1            # toward the span's middle
        xi = x1 if k == 0 else x0          # the truck's inner face
        # The end truck: a box beam, its end plates.
        b.box(x0, ty0, tz0, x1, ty1, tz1, CRANE)
        for zz, d in ((tz0, -1), (tz1, 1)):
            b.box(x0 + 0.04, ty0 + 0.04, min(zz, zz + d * 0.025), x1 - 0.04, ty1 - 0.04, max(zz, zz + d * 0.025), CRANE)
        # Its wheels, the axle boxes bolted either side (the inner one at the driven wheel is the gearbox's).
        for j, zw in enumerate(WHEELS_Z):
            _wheel(b, xc, WHEEL_Y, zw, WHEEL_R)
            for side in (-1, 1):
                if side == s and j == 1:
                    continue
                xf = x0 if side < 0 else x1
                b.box(min(xf, xf + side * 0.05), ty0 + 0.02, zw - 0.24, max(xf, xf + side * 0.05), ty0 + 0.36, zw + 0.24, GUN)
                for dz in (-0.17, 0.17):
                    for dy in (0.08, 0.28):
                        b.cyl((xf + side * 0.05, ty0 + 0.02 + dy, zw + dz), (xf + side * 0.075, ty0 + 0.02 + dy, zw + dz), 0.022, MID, MID, 6)
        # The buffers at its corners.
        for zz, d in ((tz0 - 0.025, -1), (tz1 + 0.025, 1)):
            for dx in (-0.22, 0.22):
                _buffer(b, (xc + dx, 0.8, zz), "z", d, r=0.1, body=0.16, face=0.06)
        # The drive at the far wheel: its gearbox on the axle, the motor out along the truck's inside.
        zw = WHEELS_Z[1]
        g0, g1 = xi, xi + s * 0.36
        b.box(min(g0, g1), ty0 + 0.03, zw - 0.28, max(g0, g1), ty0 + 0.55, zw + 0.3, GUN)
        b.box(min(g0, g1) + 0.03, ty0 + 0.55, zw - 0.2, max(g0, g1) - 0.03, ty0 + 0.6, zw + 0.22, GUN)
        gmotor(b, (g1, ty0 + 0.29, zw + 0.03), "x", s, 0.55, 0.19)
        side_plate(b, -s, g1, zw - 0.2, zw + 0.2, ty0 + 0.08, ty0 + 0.18, tag_tex(f"BRIDGE DRIVE {'N' if k == 0 else 'S'}", 0.4, 0.1), d=0.003)
        # The crane's number on the truck's inner face, between the girders.
        side_plate(b, s, xi, 2.7, 3.8, ty0 + 0.45, ty0 + 0.7, big_stencil(CRANE_TAG, 1.1, 0.25), edge=None, d=0.003)
    # The collector on the north truck: arms out under the girders and down beside the runway beam, its shoes on the
    # runway's four conductor bars, the cable up into the truck.
    xt = TRUCKS[0][1]
    xr = RUN_BAR_X[1]
    for z in (2.55, 3.35):
        b.box(xt, ty0 + 0.03, z, xr + 0.09, ty0 + 0.09, z + 0.06, DARK)
        b.box(xr + 0.03, RUN_BAR_Y[-1] - 0.06, z, xr + 0.09, ty0 + 0.03, z + 0.06, DARK)
    b.box(xr + 0.035, RUN_BAR_Y[-1] - 0.04, 2.61, xr + 0.085, RUN_BAR_Y[0] + 0.04, 3.35, DARK)
    for y in RUN_BAR_Y:
        b.box(xr, y - 0.025, 2.75, xr + 0.035, y + 0.025, 3.15, BLACK)
    run(b, [(xr + 0.12, RUN_BAR_Y[0] + 0.06, 2.58), (xr + 0.12, ty0 + 0.17, 2.58), (xt, ty0 + 0.17, 2.58)], 0.03, BLACK, sides=8)
    # The two box girders: their ends on the trucks, the webs and flanges, the trolley rails on the top with their clips,
    # the trolley's end stops, the stiffeners on the outer webs.
    xa, xb = TRUCKS[0][1], TRUCKS[1][0]
    for gi, (z0, z1) in enumerate(GIRDERS):
        b.box(TRUCKS[0][0] + 0.02, ty1, z0, xa, G_Y[1], z1, [CRANE, CRANE, CRANE, None, None, None])
        b.box(xb, ty1, z0, TRUCKS[1][1] - 0.02, G_Y[1], z1, [CRANE, CRANE, None, CRANE, None, None])
        b.box(xa, G_Y[0], z0, xb, G_Y[1], z1, [CRANE, CRANE, None, None, None, None])
        b.box(TRUCKS[0][0] + 0.02, G_Y[1], z0 - 0.06, TRUCKS[1][1] - 0.02, G_Y[1] + 0.06, z1 + 0.06, CRANE)
        b.box(xa, G_Y[0] - 0.05, z0 - 0.06, xb, G_Y[0], z1 + 0.06, CRANE)
        # Connection plates and bolts where the girder sits on each truck.
        for xt0, xt1 in TRUCKS:
            for zz, d in ((z0, -1), (z1, 1)):
                b.box(xt0 + 0.06, ty1 - 0.25, min(zz, zz + d * 0.02), xt1 - 0.06, ty1 + 0.35, max(zz, zz + d * 0.02), CRANE)
                for bx in (xt0 + 0.2, (xt0 + xt1) / 2, xt1 - 0.2):
                    for by in (ty1 - 0.12, ty1 + 0.22):
                        b.cyl((bx, by, zz + d * 0.02), (bx, by, zz + d * 0.045), 0.025, MID, MID, 6)
        rz = RAILS[gi]
        b.box(1.7, G_Y[1] + 0.06, rz - 0.07, SPAN - 1.7, RAIL_Y - 0.06, rz + 0.07, [None, None, DARK, DARK, DARK, None])
        b.box(1.7, RAIL_Y - 0.06, rz - 0.035, SPAN - 1.7, RAIL_Y, rz + 0.035, [None, None, STEEL, STEEL, STEEL, None])
        x = 2.2
        while x < SPAN - 1.9:
            for d in (-1, 1):
                b.box(x - 0.06, G_Y[1] + 0.06, min(rz + d * 0.07, rz + d * 0.13), x + 0.06, G_Y[1] + 0.1, max(rz + d * 0.07, rz + d * 0.13), MID)
            x += 1.0
        for xs, d in ((1.7, -1), (SPAN - 1.7, 1)):
            b.box(min(xs, xs + d * 0.25), G_Y[1] + 0.06, rz - 0.2, max(xs, xs + d * 0.25), RAIL_Y + 0.62, rz + 0.2, HAZARD)
        # The stiffeners on the outer web (clear of the stencils and the walkway's panels).
        zo, so = (z0, -1) if gi == 0 else (z1, 1)
        x = 3.0
        while x < SPAN - 2:
            if all(abs(x - xm) > 2.0 for xm in (9.0, 21.0, 33.0)) and not (gi == 0 and 2.0 < x < 7.6) \
                    and not (gi == 1 and any(abs(x - xm) < 1.4 for xm in (15.0, 27.0))):
                b.box(x - 0.03, G_Y[0] + 0.04, min(zo, zo + so * 0.025), x + 0.03, G_Y[1] - 0.04, max(zo, zo + so * 0.025), CRANE)
            x += 3.0
        # The capacity stencilled on the outer web either side, the crane's number between (read from the floor below).
        for xm, text, w in ((9.0, "160 TON", 3.2), (21.0, CRANE_TAG, 3.6), (33.0, "160 TON", 3.2)):
            tex = big_stencil(text, w, 0.75)
            if gi == 0:
                front_plate(b, z0, xm - w / 2, xm + w / 2, 1.4, 2.15, tex, edge=None, d=0.003)
            else:
                back_plate(b, z1, xm - w / 2, xm + w / 2, 1.4, 2.15, tex, edge=None, d=0.003)
        if gi == 1:
            for xm in (15.0, 27.0):
                back_plate(b, z1, xm - 1.2, xm + 1.2, 0.9, 1.35, big_stencil("AUX 25 TON", 2.4, 0.45), edge=None, d=0.003)
        # The floodlights under the girder.
        zc = (z0 + z1) / 2
        for xl in (7.0, 21.0, 35.0):
            b.box(xl - 0.04, G_Y[0] - 0.15, zc - 0.04, xl + 0.04, G_Y[0] - 0.05, zc + 0.04, DARK)
            b.box(xl - 0.28, G_Y[0] - 0.3, zc - 0.22, xl + 0.28, G_Y[0] - 0.19, zc + 0.22, DARK)
            for fx in range(-2, 3):
                b.box(xl + fx * 0.1 - 0.015, G_Y[0] - 0.19, zc - 0.2, xl + fx * 0.1 + 0.015, G_Y[0] - 0.15, zc + 0.2, DARK)
            live["lamps"].append({"name": "crane_flood", "box": _r(xl - 0.24, G_Y[0] - 0.302, zc - 0.18, xl + 0.24, G_Y[0] - 0.3, zc + 0.18),
                                  "faces": [0, 0, 0, 0, 0, 1], "colour": "white", "on": C.LENS["white"], "off": C.LENS["off_white"]})
    # The walkway along the west side (outside girder 1): grating on brackets off the web, its toe plate, the posts and
    # the rails of its handrail, closed at both ends.
    wz0, wz1 = 0.3, GIRDERS[0][0] - 0.06
    gx0, gx1 = 1.0, SPAN - 1.0
    b.box(gx0, G_Y[0], wz0, gx1, G_Y[0] + 0.05, wz1, "grating")
    b.box(gx0, G_Y[0] + 0.05, wz0, gx1, G_Y[0] + 0.15, wz0 + 0.03, CRANE)
    x = gx0 + 0.5
    while x < gx1:
        b.box(x - 0.03, G_Y[0] - 0.25, wz0 + 0.02, x + 0.03, G_Y[0], wz1, CRANE)
        x += 2.0
    posts = [gx0 + 0.03] + [gx0 + 2.0 * k for k in range(1, int((gx1 - gx0) / 2.0))] + [gx1 - 0.03]
    for x in posts:
        b.box(x - 0.025, G_Y[0] + 0.15, wz0 + 0.035, x + 0.025, G_Y[0] + 1.12, wz0 + 0.085, CRANE)
    for y in (G_Y[0] + 0.6, G_Y[0] + 1.1):
        b.cyl((gx0, y, wz0 + 0.06), (gx1, y, wz0 + 0.06), 0.022, CRANE, CRANE, 8)
        for x in (gx0, gx1):
            b.cyl((x, y, wz0 + 0.06), (x, y, wz1 - 0.02), 0.022, CRANE, CRANE, 8)
    # The panels on the walkway at its north end against the girder (the hoist's, the trolley's, the bridge's), the main
    # disconnect beside them, their conduits up the web to the conductor bars.
    pz0 = wz1 - 0.4
    for px, name in ((2.75, "HOIST PANEL"), (3.95, "TROLLEY PANEL"), (5.15, "BRIDGE PANEL")):
        b.box(px - 0.55, G_Y[0] + 0.05, pz0, px + 0.55, G_Y[0] + 1.55, wz1, [None, None, PANEL, PANEL, None, None])
        b.box(px - 0.52, G_Y[0] + 0.08, pz0 - 0.015, px + 0.52, G_Y[0] + 1.52, pz0, [PANEL, None, PANEL, PANEL, PANEL, PANEL])
        b.box(px - 0.57, G_Y[0] + 1.55, pz0 - 0.03, px + 0.57, G_Y[0] + 1.6, wz1, GUN)
        b.box(px + 0.38, G_Y[0] + 0.7, pz0 - 0.035, px + 0.42, G_Y[0] + 0.9, pz0 - 0.015, sw("dgrey"))
        for j in range(6):
            b.box(px - 0.3, G_Y[0] + 0.2 + j * 0.06, pz0 - 0.022, px + 0.3, G_Y[0] + 0.225 + j * 0.06, pz0 - 0.015, sw("dgrey"))
        front_plate(b, pz0 - 0.015, px - 0.3, px + 0.3, G_Y[0] + 1.3, G_Y[0] + 1.38, tag_tex(name, 0.6, 0.08), d=0.003)
        b.cyl((px, G_Y[0] + 1.6, pz0 + 0.2), (px, BAR_Y[-1] - 0.05, pz0 + 0.2), 0.03, GUN, GUN, 8)
    dx0, dx1 = 5.85, 6.35
    b.box(dx0, G_Y[0] + 0.65, wz1 - 0.3, dx1, G_Y[0] + 1.25, wz1, [None, None, sw("dgrey"), sw("dgrey"), sw("dgrey"), sw("dgrey")])
    b.box(dx0 + 0.03, G_Y[0] + 0.68, wz1 - 0.315, dx1 - 0.03, G_Y[0] + 1.22, wz1 - 0.3, [sw("dgrey"), None, sw("dgrey"), sw("dgrey"), sw("dgrey"), sw("dgrey")])
    b.box(dx0 + 0.08, G_Y[0] + 0.85, wz1 - 0.38, dx0 + 0.14, G_Y[0] + 1.05, wz1 - 0.315, sw("red"))
    front_plate(b, wz1 - 0.315, dx0 + 0.17, dx1 - 0.06, G_Y[0] + 1.1, G_Y[0] + 1.17, tag_tex("MAIN DISC", 0.27, 0.07), d=0.003)
    b.cyl(((dx0 + dx1) / 2, G_Y[0] + 1.25, wz1 - 0.15), ((dx0 + dx1) / 2, BAR_Y[-1] - 0.05, wz1 - 0.15), 0.03, GUN, GUN, 8)
    # The trolley's conductor bars under girder 1's top flange (three phases and the earth), on their insulators.
    for k, y in enumerate(BAR_Y):
        b.box(1.9, y - 0.022, BAR_Z[0], SPAN - 1.9, y + 0.022, BAR_Z[1], sw("orange") if k < 3 else sw("green"))
    x = 2.2
    while x < SPAN - 1.9:
        b.box(x - 0.03, BAR_Y[-1] - 0.05, BAR_Z[1], x + 0.03, G_Y[1] - 0.04, GIRDERS[0][0], DARK)
        x += 3.0
    # The travel beacons and the horn under the girders at both ends.
    for xb_ in (1.5, SPAN - 1.5):
        _beacon(b, live, xb_, G_Y[0] - 0.05, sum(GIRDERS[1]) / 2)
    b.box(2.1, G_Y[0] - 0.2, 4.2, 2.3, G_Y[0] - 0.05, 4.4, DARK)
    rev(b, (2.2, G_Y[0] - 0.2, 4.3), (2.2, G_Y[0] - 0.55, 4.3), [(0, 0.05), (0.2, 0.06), (0.35, 0.13)], sw("dgrey"), DARK, None, 12)
    live["mats"] = {"rope": "stainless_dark"}
    b.live = live
    return b


def trolley():
    with own_atlas():
        return _trolley()


def _trolley():
    """The trolley about its own middle (x 0): the game shifts it along the bridge."""
    b = C.Board(6, 4512, face=0.0)
    live = {"gauges": [], "lamps": []}
    x0, x1 = -T_HALF, T_HALF
    d0, d1 = DECK_Y
    # The frame: its side beams over the rails (the wheels in them, their axle boxes), the buffers at the ends, the deck
    # plate between with its openings for the falls.
    sb = 0.22
    for rz in RAILS:
        for xw in (x0 + 0.5, x1 - 0.5):
            _wheel(b, xw, RAIL_Y + 0.2, rz, 0.2, axis="z", w=0.11)
        b.box(x0, RAIL_Y + 0.25, rz - sb, x1, d1, rz + sb, CRANE)
        for xw in (x0 + 0.5, x1 - 0.5):
            for d in (-1, 1):
                if rz == RAILS[0] and xw < 0 and d < 0:
                    continue
                zf = rz + d * sb
                b.box(xw - 0.16, RAIL_Y + 0.27, min(zf, zf + d * 0.04), xw + 0.16, RAIL_Y + 0.5, max(zf, zf + d * 0.04), GUN)
        for xe, d in ((x0, -1), (x1, 1)):
            _buffer(b, (xe, RAIL_Y + 0.42, rz), "x", d, r=0.08, body=0.14, face=0.05)
    zi0, zi1 = RAILS[0] + sb, RAILS[1] - sb
    for a0, c0, a1, c1 in ((x0, zi0, x1, 2.5), (x0, 3.5, x1, zi1), (x0, 2.5, -2.2, 3.5), (-1.8, 2.5, -1.0, 3.5), (0.3, 2.5, x1, 3.5)):
        b.box(a0, d0, c0, a1, d1, c1, [CRANE if c0 > 2.4 else None, CRANE if c1 < 3.6 else None, CRANE if a0 > x0 else None,
                                       CRANE if a1 < x1 else None, CRANE, CRANE])
    # The upper sheave block under the deck (the main falls go down from it), its sheaves' pin ends.
    ub0, ub1 = MAIN_BLOCK[0] + 0.02, MAIN_BLOCK[1] - 0.02
    b.box(ub0, UPPER_BLOCK_Y, 2.62, ub1, d0, 3.38, [CRANE, CRANE, CRANE, CRANE, None, DARK])
    for k in range(4):
        xr = MAIN_DX + (k - 1.5) * 0.3
        for zz, d in ((2.62, -1), (3.38, 1)):
            b.cyl((xr, UPPER_BLOCK_Y + 0.17, zz), (xr, UPPER_BLOCK_Y + 0.17, zz + d * 0.03), 0.05, MID, MID, 8)
    # The main hoist: the drum on its pedestals (its grooves, flanges, bearings), the gearbox, the brake, the motor on
    # its base, the rotary limit switch on the far pedestal.
    p0, p1 = DRUM
    for xp, xq in ((p0 - 0.15, p0), (p1, p1 + 0.15)):
        b.box(xp, d1, HOOK_Z - 0.42, xq, DRUM_Y + 0.12, HOOK_Z + 0.42, CRANE)
        b.box(xp - 0.01, DRUM_Y - 0.16, HOOK_Z - 0.2, xq + 0.01, DRUM_Y + 0.2, HOOK_Z + 0.2, GUN)
    b.cyl((p0 + 0.06, DRUM_Y, HOOK_Z), (p1 - 0.06, DRUM_Y, HOOK_Z), DRUM_R, "stainless_dark", "stainless_dark", 24)
    for xa, xb in ((p0, p0 + 0.06), (p1 - 0.06, p1)):
        b.cyl((xa, DRUM_Y, HOOK_Z), (xb, DRUM_Y, HOOK_Z), DRUM_R + 0.07, CRANE, CRANE, 24)
    x = p0 + 0.13
    while x < p1 - 0.1:
        rev(b, (x, DRUM_Y, HOOK_Z), (x + 0.035, DRUM_Y, HOOK_Z), [(0, DRUM_R + 0.004), (0.0175, DRUM_R + 0.018), (0.035, DRUM_R + 0.004)], "stainless_dark", None, None, 24)
        x += 0.07
    gx0, gx1 = p1 + 0.15, p1 + 0.62
    b.box(gx0, d1, HOOK_Z - 0.5, gx1, DRUM_Y + 0.5, HOOK_Z + 0.55, GUN)
    b.box(gx0 - 0.02, DRUM_Y - 0.05, HOOK_Z - 0.52, gx1 + 0.02, DRUM_Y + 0.02, HOOK_Z + 0.57, GUN)
    for bz in (HOOK_Z - 0.4, HOOK_Z, HOOK_Z + 0.45):
        for bx in (gx0 + 0.1, gx1 - 0.1):
            b.cyl((bx, DRUM_Y + 0.02, bz), (bx, DRUM_Y + 0.06, bz), 0.025, MID, MID, 6)
    my, mz = DRUM_Y + 0.15, HOOK_Z + 0.25
    b.cyl((gx1, my, mz), (gx1 + 0.1, my, mz), 0.21, DARK, DARK, 16)
    b.box(gx1 + 0.02, d1, mz - 0.3, gx1 + 0.08, my + 0.26, mz - 0.24, DARK)
    b.box(gx1 + 0.02, d1, mz + 0.24, gx1 + 0.08, my + 0.26, mz + 0.3, DARK)
    b.box(gx1, my + 0.26, mz - 0.3, gx1 + 0.1, my + 0.32, mz + 0.3, DARK)
    b.box(gx1 - 0.05, my + 0.32, mz - 0.08, gx1 + 0.15, my + 0.5, mz + 0.08, sw("dgrey"))
    motor_h(b, (gx1 + 0.12, my, mz), "x", 0.62, 0.24, CRANE)
    b.box(gx1 + 0.12, d1, mz - 0.26, gx1 + 0.76, my - 0.3, mz + 0.26, GUN)
    side_plate(b, 1, gx1, HOOK_Z - 0.45, HOOK_Z - 0.05, DRUM_Y - 0.25, DRUM_Y - 0.13, tag_tex("MAIN HOIST 160 T", 0.4, 0.12), d=0.003)
    b.box(p0 - 0.31, DRUM_Y - 0.12, HOOK_Z - 0.1, p0 - 0.15, DRUM_Y + 0.12, HOOK_Z + 0.14, sw("dgrey"))
    side_plate(b, -1, p0 - 0.31, HOOK_Z - 0.08, HOOK_Z + 0.12, DRUM_Y + 0.02, DRUM_Y + 0.09, tag_tex("LIMIT SW", 0.2, 0.07), d=0.003)
    # The auxiliary hoist at the trolley's north end: its drum along z on its pedestals, the gearbox, the motor on its
    # base along the east side.
    ax = AUX_DX
    for zp0, zp1 in ((2.2, 2.3), (3.7, 3.8)):
        b.box(ax - 0.24, d1, zp0, ax + 0.24, AUX_DRUM_Y + 0.1, zp1, CRANE)
    b.cyl((ax, AUX_DRUM_Y, 2.3), (ax, AUX_DRUM_Y, 3.7), AUX_DRUM_R, "stainless_dark", "stainless_dark", 18)
    for za, zb in ((2.3, 2.35), (3.65, 3.7)):
        b.cyl((ax, AUX_DRUM_Y, za), (ax, AUX_DRUM_Y, zb), AUX_DRUM_R + 0.05, CRANE, CRANE, 18)
    z = 2.42
    while z < 3.6:
        rev(b, (ax, AUX_DRUM_Y, z), (ax, AUX_DRUM_Y, z + 0.03), [(0, AUX_DRUM_R + 0.003), (0.015, AUX_DRUM_R + 0.014), (0.03, AUX_DRUM_R + 0.003)], "stainless_dark", None, None, 18)
        z += 0.06
    b.box(ax - 0.3, d1, 3.8, ax + 0.3, AUX_DRUM_Y + 0.32, zi1, GUN)
    b.box(ax + 0.3, d1, 3.78, ax + 0.85, AUX_DRUM_Y - 0.17, zi1, GUN)
    gmotor(b, (ax + 0.3, AUX_DRUM_Y, 3.92), "x", 1, 0.5, 0.15)
    side_plate(b, 1, ax + 0.3, 3.82, 4.05, AUX_DRUM_Y + 0.15, AUX_DRUM_Y + 0.25, tag_tex("AUX HOIST 25 T", 0.23, 0.1), d=0.003)
    # The trolley's drive on the west side beam: its gearbox on the wheel's axle, the flange-mounted motor along it.
    zf = RAILS[0] - sb
    xw = x0 + 0.5
    b.box(xw - 0.22, RAIL_Y + 0.12, zf - 0.28, xw + 0.24, d1 - 0.02, zf, GUN)
    gmotor(b, (xw + 0.24, RAIL_Y + 0.36, zf - 0.14), "x", 1, 0.5, 0.12)
    front_plate(b, zf - 0.28, xw - 0.18, xw + 0.2, RAIL_Y + 0.4, RAIL_Y + 0.48, tag_tex("TROLLEY DRIVE", 0.38, 0.08), d=0.003)
    # Its electrical cabinet on the deck's west edge, its door toward the walkway; the collector arm out over girder 1's
    # flange and down onto the conductor bars; the cable from the arm into the cabinet.
    cx0, cx1, cz0, cz1 = 0.45, 1.75, zi0 + 0.03, 2.38
    b.box(cx0, d1, cz0, cx1, d1 + 0.95, cz1, [None, PANEL, PANEL, PANEL, PANEL, None])
    b.box(cx0 + 0.03, d1 + 0.03, cz0 - 0.015, cx1 - 0.03, d1 + 0.92, cz0, [PANEL, None, PANEL, PANEL, PANEL, PANEL])
    b.box(cx0 - 0.02, d1 + 0.95, cz0 - 0.03, cx1 + 0.02, d1 + 1.0, cz1 + 0.02, GUN)
    for j in range(5):
        b.box(cx0 + 0.25, d1 + 0.12 + j * 0.06, cz0 - 0.022, cx0 + 0.75, d1 + 0.14 + j * 0.06, cz0 - 0.015, sw("dgrey"))
    b.box(cx1 - 0.15, d1 + 0.4, cz0 - 0.035, cx1 - 0.11, d1 + 0.58, cz0 - 0.015, sw("dgrey"))
    front_plate(b, cz0 - 0.015, cx0 + 0.85, cx1 - 0.2, d1 + 0.78, d1 + 0.86, tag_tex("TROLLEY CTRL", 0.45, 0.08), d=0.003)
    xa = 0.62
    za = BAR_Z[0] - 0.075
    b.box(xa - 0.03, RAIL_Y + 0.36, za, xa + 0.03, RAIL_Y + 0.42, zf, DARK)
    b.box(xa - 0.03, BAR_Y[-1] - 0.06, za, xa + 0.03, RAIL_Y + 0.36, za + 0.035, DARK)
    for y in BAR_Y:
        b.box(xa - 0.14, y - 0.025, za + 0.035, xa + 0.14, y + 0.025, BAR_Z[0], BLACK)
    cy = RAIL_Y + 0.445
    run(b, [(xa, cy, za + 0.05), (xa, cy, zf - 0.03), (xa, d1 + 0.05, zf - 0.03), (xa, d1 + 0.05, cz0 - 0.02)], 0.025, BLACK, sides=8)
    b.box(xa - 0.05, d1, cz0 - 0.02, xa + 0.05, d1 + 0.1, cz0, DARK)
    # The falls' tops (the game stretches them down to the hook blocks).
    live["ropes"] = [{"x": _r(x)[0], "z": _r(z)[0], "y": _r(UPPER_BLOCK_Y)[0], "hook": "main"} for x, z in FALLS]
    live["ropes"] += [{"x": _r(x)[0], "z": _r(z)[0], "y": _r(AUX_DRUM_Y - AUX_DRUM_R)[0], "hook": "aux"} for x, z in AUX_FALLS]
    live["mats"] = {"rope": "stainless_dark"}
    b.live = live
    return b


def hook(main=True):
    with own_atlas():
        return _hook(main)


def _hook(main):
    """A hook block at rest (its top at HOOK_REST or AUX_REST), drawn about its own place on the trolley (MAIN_DX or
    AUX_DX at x 0)."""
    b = C.Board(2, 4513 if main else 4514, face=0.0)
    if main:
        top = HOOK_REST
        x0, x1, h = MAIN_BLOCK[0] - MAIN_DX, MAIN_BLOCK[1] - MAIN_DX, MAIN_BLOCK[2]
        y0 = top - h
        # The block's side plates (yellow, a striped band along their foot), the sheaves' guard between, the sheave pin's
        # ends, the capacity stencilled on both sides.
        for za, zb, d in ((HOOK_Z - 0.42, HOOK_Z - 0.36, -1), (HOOK_Z + 0.36, HOOK_Z + 0.42, 1)):
            b.box(x0, y0, za, x1, top, zb, CRANE)
            zs = za if d < 0 else zb
            if d < 0:
                b.box(x0 + 0.02, y0 + 0.02, zs - 0.004, x1 - 0.02, y0 + 0.2, zs, [HAZARD, None, HAZARD, HAZARD, HAZARD, HAZARD])
                front_plate(b, zs, -0.4, 0.4, y0 + 0.36, y0 + 0.66, big_stencil("160 T", 0.8, 0.3), edge=None, d=0.003)
            else:
                b.box(x0 + 0.02, y0 + 0.02, zs, x1 - 0.02, y0 + 0.2, zs + 0.004, [None, HAZARD, HAZARD, HAZARD, HAZARD, HAZARD])
                back_plate(b, zs, -0.4, 0.4, y0 + 0.36, y0 + 0.66, big_stencil("160 T", 0.8, 0.3), edge=None, d=0.003)
            b.cyl((0.0, top - 0.2, zs), (0.0, top - 0.2, zs + d * 0.05), 0.08, GUN, GUN, 12)
            bolts(b, (0.0, top - 0.2, zs + d * 0.05), 0.055, 4, br=0.014, length=0.02, axis_="z" if d > 0 else "-z", phase=0.785)
        b.box(x0 + 0.04, y0 + 0.05, HOOK_Z - 0.36, x1 - 0.04, top - 0.04, HOOK_Z + 0.36, [None, None, CRANE, CRANE, DARK, CRANE])
        # The crosshead under the block, the thrust bearing's housing, the hook on its shank.
        b.box(-0.35, y0 - 0.16, HOOK_Z - 0.28, 0.35, y0, HOOK_Z + 0.28, GUN)
        b.cyl((0.0, y0 - 0.16, HOOK_Z), (0.0, y0 - 0.26, HOOK_Z), 0.16, GUN, GUN, 16)
        _the_hook(b, 0.0, y0 - 0.26, 1.5)
    else:
        top = AUX_REST
        # A single sheave between its side plates over the overhaul ball, the hook.
        b.box(-0.12, top - 0.36, HOOK_Z - 0.2, 0.12, top, HOOK_Z + 0.2, CRANE)
        b.box(-0.124, top - 0.34, HOOK_Z - 0.18, 0.124, top - 0.24, HOOK_Z + 0.18, [None, None, HAZARD, HAZARD, None, None])
        b.cyl((-0.124, top - 0.16, HOOK_Z), (-0.15, top - 0.16, HOOK_Z), 0.05, GUN, GUN, 10)
        b.cyl((0.124, top - 0.16, HOOK_Z), (0.15, top - 0.16, HOOK_Z), 0.05, GUN, GUN, 10)
        rev(b, (0.0, top - 0.36, HOOK_Z), (0.0, top - 0.86, HOOK_Z), [(0, 0.06), (0.06, 0.18), (0.25, 0.22), (0.44, 0.18), (0.5, 0.06)], HAZARD, None, None, 14)
        b.cyl((0.0, top - 0.86, HOOK_Z), (0.0, top - 0.92, HOOK_Z), 0.07, GUN, GUN, 12)
        _the_hook(b, 0.0, top - 0.92, 0.75)
    b.live = {"gauges": [], "lamps": []}
    return b


def _the_hook(b, x, y, s):
    """A forged hook hanging from (x, y): its round shank, the curve round under and up to its point (opening toward
    +x), the spring latch across the mouth."""
    b.cyl((x, y, HOOK_Z), (x, y - 0.32 * s, HOOK_Z), 0.075 * s, DARK, DARK, 12)
    r = 0.26 * s
    cx, cy = x + r * 0.95, y - 0.32 * s - r * 0.55
    pts = [(x, y - 0.32 * s)]
    n = 12
    for k in range(n + 1):
        a = math.radians(195 - 235 * k / n)          # from the shank's foot round under to the point
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    for k, (p, q) in enumerate(zip(pts, pts[1:])):
        dx, dy = q[0] - p[0], q[1] - p[1]
        ln = math.hypot(dx, dy)
        f = 1.0 - 0.45 * k / n                       # thinner toward the point
        t = 0.085 * s * f
        hw = 0.07 * s * f * (1.0 if k % 2 == 0 else 0.985)
        b.obox(((p[0] + q[0]) / 2, (p[1] + q[1]) / 2, HOOK_Z), (dx / ln, dy / ln, 0), (-dy / ln, dx / ln, 0), ln / 2 + t * 0.35, t, hw, DARK)
    # The latch: from the shank's side across to the point.
    px_, py_ = pts[-1]
    sx, sy = x + 0.075 * s, y - 0.36 * s
    dx, dy = px_ - sx, py_ - sy
    ln = math.hypot(dx, dy)
    b.obox(((sx + px_) / 2, (sy + py_) / 2, HOOK_Z), (dx / ln, dy / ln, 0), (-dy / ln, dx / ln, 0), ln / 2, 0.012 * s, 0.045 * s, sw("red"))


def runway():
    """The runway's parts a block at a time (z 0 to 1 along it), the game laying them along the bridge's travel: the rail
    on each beam with its clips, the conductor bars on the north beam, their bracket (every third block), the end stops."""
    with own_atlas():
        parts = {}
        centres = [(x0 + x1) / 2 for x0, x1 in TRUCKS]
        for name in ("rail", "bars", "bracket", "stop_low", "stop_high"):
            b = C.Board(SPAN, 4515, face=0.0)
            if name == "rail":
                for xc in centres:
                    b.box(xc - 0.11, 0.0, 0.0, xc + 0.11, 0.04, 1.0, [None, None, DARK, DARK, DARK, None])
                    b.box(xc - 0.035, 0.04, 0.0, xc + 0.035, 0.11, 1.0, [None, None, DARK, DARK, None, None])
                    b.box(xc - 0.05, 0.11, 0.0, xc + 0.05, 0.15, 1.0, [None, None, STEEL, STEEL, STEEL, None])
                    for d in (-1, 1):
                        a = xc + d * 0.11
                        b.box(min(a, a + d * 0.07), 0.0, 0.42, max(a, a + d * 0.07), 0.06, 0.58,
                              [MID, MID, MID if d < 0 else None, MID if d > 0 else None, MID, None])
                        b.cyl((a + d * 0.045, 0.06, 0.5), (a + d * 0.045, 0.09, 0.5), 0.025, MID, MID, 6)
            elif name == "bars":
                for k, y in enumerate(RUN_BAR_Y):
                    c = sw("orange") if k < 3 else sw("green")
                    b.box(RUN_BAR_X[0] + 0.01, y - 0.022, 0.0, RUN_BAR_X[1], y + 0.022, 1.0, [None, None, None, c, c, c])
            elif name == "bracket":
                b.box(RUN_BAR_X[0], RUN_BAR_Y[-1] - 0.05, 0.47, RUN_BAR_X[0] + 0.01, -0.12, 0.53, [DARK, DARK, None, DARK, DARK, DARK])
            else:
                low = name == "stop_low"
                z0, z1 = (0.0, 0.3) if low else (0.7, 1.0)
                for xc in centres:
                    b.box(xc - 0.3, 0.0, z0, xc + 0.3, 1.0, z1, [HAZARD, HAZARD, HAZARD, HAZARD, HAZARD, None])
                    b.box(xc - 0.12, 0.65, z1 if low else z0 - 0.08, xc + 0.12, 0.95, z1 + 0.08 if low else z0, BLACK)
            parts[name] = b.ops()["still"]
        return parts


def _lowest(b):
    """The lowest y any of a group's boxes and cylinders reach (its ops: "b" boxes, "c" cylinders, "q" quads)."""
    lo = 1e9
    for op in b.ops()["still"]:
        if op[0] == "b":
            lo = min(lo, op[2], op[5])
        elif op[0] == "c":
            lo = min(lo, op[2] - op[7], op[5] - op[7])
        elif op[0] == "o":
            # An oriented box: its middle less its three half-extents along its axes' y components.
            import numpy as np
            u, w0 = np.array(op[4:7], float), np.array(op[7:10], float)
            w = np.cross(u, w0)
            lo = min(lo, op[2] - abs(u[1]) * op[10] - abs(w0[1]) * op[11] - abs(w[1]) * op[12])
        elif op[0] == "q":
            lo = min(lo, op[2], op[5], op[8], op[11])
    return lo


def overhead_crane(seed=4511):
    """The crane as the game has it: the bridge's parts still, the trolley's and the hooks' in groups the game moves, the
    runway's parts to lay along its travel."""
    b = bridge()
    t, h, a = trolley(), hook(True), hook(False)
    b.live["trolley"] = {"still": t.ops()["still"], "lamps": t.live["lamps"], "ropes": t.live["ropes"]}
    # (Each hook's lowest point at rest, "tip": HallCrane lets it down until that is on the first solid block under it.)
    b.live["hook"] = {"still": h.ops()["still"], "top": HOOK_REST, "dx": MAIN_DX, "tip": _r(_lowest(h))[0]}
    b.live["aux"] = {"still": a.ops()["still"], "top": AUX_REST, "dx": AUX_DX, "tip": _r(_lowest(a))[0]}
    b.live["runway"] = runway()
    b.live["travel"] = {"trolley": list(TROLLEY_X), "hook_z": HOOK_Z}
    return b


BOARDS = {"overhead_crane": ("Overhead Crane", SPAN, DEEP, TALL, overhead_crane)}
