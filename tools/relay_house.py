"""The 345 kV switchyard's relay house (control house), built as board ops like the yard's machines.

Real basis: a utility switchyard control house for a three-bay breaker-and-a-half 345 kV yard, about 24 x 12 m,
precast concrete wall panels on a slab at grade, flat roof with parapet, a relay room with rows of protection
panels, a separate ventilated battery room, a communications / SCADA room, wall-mounted HVAC units, cable entry from
the yard's trench through the floor into covered floor trenches, ground risers to the grid at every corner.

Frame: x along the length (0..L), z across (0..D), front wall (doors, cable entry) at z = 0 facing -z (the yard).
"""
import math

import numpy as np

import transformer_yard as TY
import line_supports as LS
from control_boards import Board, sw
from transformer_yard import ATLAS, cyl, rod, bar, path, ring, tag, face_quad, flange, _rnd, stencil, GREY, BLACK, gauge, lamp, selector
import control_boards as CB

ls = LS.ls

RH = dict(L=24.0, D=12.0, H=4.2, wall=0.2, panel=3.0,
          batt=(0.0, 5.0, 0.0, 5.2),          # battery room x0, x1, z0, z1
          comm=(0.0, 5.0, 7.0, 12.0),         # communications / SCADA room
          front_door=7.0, batt_door=0.35, equip_door=(6.1, 8.9),   # equipment double door on the +x end wall, z range
          trench_x=13.5)


def _wall_with_openings(b, axis, fixed, a0, a1, y0, y1, t, openings, mat, outward):
    """A straight wall along axis ("x" or "z") at fixed (its outer face), from a0 to a1, thickness t inward
    (outward: +-1, the outside normal's sign), with rectangular openings [(a0, a1, y0, y1)]."""
    cuts = sorted(openings)
    pieces = []
    a = a0
    for o0, o1, oy0, oy1 in cuts:
        if o0 > a:
            pieces.append((a, o0, y0, y1))
        if oy0 > y0:
            pieces.append((o0, o1, y0, oy0))
        if oy1 < y1:
            pieces.append((o0, o1, oy1, y1))
        a = o1
    if a < a1:
        pieces.append((a, a1, y0, y1))
    f0, f1 = sorted((fixed, fixed - outward * t))
    for p0, p1, q0, q1 in pieces:
        if axis == "x":
            b.box(p0, q0, f0, p1, q1, f1, mat)
        else:
            b.box(f0, q0, p0, f1, q1, p1, mat)


def _door(b, axis, fixed, a0, a1, h, outward, mat=None, leaves=1, open_deg=0.0, hinge_lo=True, vision=False, panic=True):
    """A hollow-metal door set in an opening of a wall along axis at fixed (outer face): frame, leaf (or two), three
    hinges, lever or panic hardware, closer arm on the push side; a vision lite in Framed Glass when vision."""
    mat = mat or sw("dgrey")
    n = np.array([0, 0, outward], float) if axis == "x" else np.array([outward, 0, 0], float)
    u = np.array([1.0, 0, 0]) if axis == "x" else np.array([0, 0, 1.0])
    P = lambda a, y, d: (u * a + np.array([0, y, 0]) + n * d + (np.array([0, 0, fixed]) if axis == "x" else np.array([fixed, 0, 0])))
    # frame: jambs and head, a little proud of the outer face
    for j0, j1 in ((a0, a0 + 0.06), (a1 - 0.06, a1)):                 # frame inside the opening
        lo, hi = np.minimum(P(j0, 0.004, 0.02), P(j1, h - 0.07, -0.21)), np.maximum(P(j0, 0.004, 0.02), P(j1, h - 0.07, -0.21))
        b.box(*_rnd(lo), *_rnd(hi), mat)
    lo, hi = np.minimum(P(a0, h - 0.07, 0.02), P(a1, h - 0.004, -0.21)), np.maximum(P(a0, h - 0.07, 0.02), P(a1, h - 0.004, -0.21))
    b.box(*_rnd(lo), *_rnd(hi), mat)
    a0, a1, h = a0 + 0.06, a1 - 0.06, h - 0.07
    w = (a1 - a0) / leaves
    for k in range(leaves):
        l0, l1 = a0 + k * w + 0.004, a0 + (k + 1) * w - 0.004
        lo, hi = np.minimum(P(l0, 0.01, -0.03), P(l1, h - 0.005, -0.075)), np.maximum(P(l0, 0.01, -0.03), P(l1, h - 0.005, -0.075))
        b.box(*_rnd(lo), *_rnd(hi), mat)
        hinge_a = l0 + 0.01 if (k == 0) == hinge_lo else l1 - 0.01
        free_a = l1 - 0.09 if (k == 0) == hinge_lo else l0 + 0.09
        for y in (0.25, h / 2, h - 0.25):
            c0, c1 = P(hinge_a, y - 0.06, -0.022), P(hinge_a, y + 0.06, -0.022)
            cyl(b, c0, c1, 0.012, ls("galv"), sides=8)
        # lever handle and rose outside; panic bar inside
        rose = P(free_a, 1.0, -0.028)
        cyl(b, rose, rose + n * 0.02, 0.03, ls("stainless"), sides=10)
        rod(b, rose + n * 0.03, rose + n * 0.06, 0.008, ls("stainless"), 6)
        dirn = 1 if free_a > hinge_a else -1
        rod(b, rose + n * 0.06, rose + n * 0.06 + u * (-dirn) * 0.12, 0.009, ls("stainless"), 6)
        if panic:
            pb0, pb1 = P(min(l0, l1) + 0.1, 1.0, -0.08), P(max(l0, l1) - 0.1, 1.0, -0.08)
            for e in (pb0, pb1):
                b.box(*_rnd(np.minimum(e - [0.03, 0.04, 0.03], e + [0.03, 0.04, 0.03])), *_rnd(np.maximum(e - [0.03, 0.04, 0.03], e + [0.03, 0.04, 0.03])), ls("stainless"))
            rod(b, pb0 - n * 0.03, pb1 - n * 0.03, 0.015, ls("stainless"), 8)
        # closer on the inside head
        cp = P(hinge_a + (0.25 if dirn > 0 else -0.25), h - 0.12, -0.1)
        b.box(*_rnd(np.minimum(cp - u * 0.15 - [0, 0.03, 0] - n * 0.03, cp + u * 0.15 + [0, 0.03, 0] + n * 0.03)),
              *_rnd(np.maximum(cp - u * 0.15 - [0, 0.03, 0] - n * 0.03, cp + u * 0.15 + [0, 0.03, 0] + n * 0.03)), sw("mgrey"))
        if vision:
            vc = P((l0 + l1) / 2, 1.55, -0.026)
            b.box(*_rnd(np.minimum(vc - u * 0.13 - [0, 0.3, 0], vc + u * 0.13 + [0, 0.3, 0] + n * 0.004)),
                  *_rnd(np.maximum(vc - u * 0.13 - [0, 0.3, 0], vc + u * 0.13 + [0, 0.3, 0] + n * 0.004)), ls("galv"))
            face_quad(b, vc + n * 0.006, ("-z" if outward < 0 else "z") if axis == "x" else ("-x" if outward < 0 else "x"), 0.2, 0.54, ls("meter_glass"))


def relay_house(number="SWITCHYARD RELAY HOUSE"):
    T = RH
    TY._faces()
    b = Board(24, 3101, face=0.0)
    b.live = []
    L, D, H, t = T["L"], T["D"], T["H"], T["wall"]
    c = ls("concrete_spun")             # smooth precast panels and slab
    # Slab at grade with a thickened edge, a gravel-stop apron round it.
    b.box(-0.4, -0.35, -0.4, L + 0.4, 0.0, D + 0.4, [c, c, c, c, c, None])
    # Exterior walls: precast panels with sealant joints; openings for doors, HVAC sleeves, the exhaust fan, the cable
    # entry (below grade, so none in the wall).
    fd, bd = T["front_door"], T["batt_door"]
    e0, e1 = T["equip_door"]
    dh = 2.15
    _wall_with_openings(b, "x", 0.0, 0.0, L, 0.0, H, t, [(fd, fd + 0.95, 0, dh), (bd, bd + 0.95, 0, dh)], c, -1)
    _wall_with_openings(b, "x", D, 0.0, L, 0.0, H, t, [(9.2, 10.2, 2.0, 2.9), (15.2, 16.2, 2.0, 2.9)], c, 1)
    _wall_with_openings(b, "z", 0.0, t, D - t, 0.0, H, t, [(1.6, 2.2, 3.1, 3.7)], c, -1)
    _wall_with_openings(b, "z", L, t, D - t, 0.0, H, t, [(e0, e1, 0, 2.6)], c, 1)
    for x in np.arange(T["panel"], L - 0.1, T["panel"]):                                   # vertical panel joints
        for z, d in ((-0.004, -1), (D + 0.004, 1)):
            b.box(x - 0.012, 0.02, min(z, z + d * 0.006), x + 0.012, H - 0.02, max(z, z + d * 0.006), sw("dgrey"))
    for z in np.arange(T["panel"], D - 0.1, T["panel"]):
        for x, d in ((-0.004, -1), (L + 0.004, 1)):
            b.box(min(x, x + d * 0.006), 0.02, z - 0.012, max(x, x + d * 0.006), H - 0.02, z + 0.012, sw("dgrey"))
    # Roof slab, membrane, parapet with metal coping; scuppers and downspouts at the back corners; a roof hatch.
    b.box(-0.05, H, -0.05, L + 0.05, H + 0.3, D + 0.05, [c, c, c, c, None, c])
    for (x0, z0, x1, z1) in ((-0.05, -0.05, L + 0.05, 0.15), (-0.05, D - 0.15, L + 0.05, D + 0.05),
                             (-0.05, 0.15, 0.15, D - 0.15), (L - 0.15, 0.15, L + 0.05, D - 0.15)):
        b.box(x0, H + 0.3, z0, x1, H + 0.75, z1, [c, c, c, c, None, None])
        side = z1 - z0 > 1 and x1 - x0 < 1
        b.box(x0 - 0.03, H + 0.75, z0 + 0.03 if side else z0 - 0.03, x1 + 0.03, H + 0.8, z1 - 0.03 if side else z1 + 0.03, ls("galv"))
    b.box(0.15, H + 0.3, 0.15, L - 0.15, H + 0.32, D - 0.15, [None] * 4 + [sw("dgrey"), None])
    for x in (1.0, L - 1.0):
        b.box(x - 0.15, H + 0.38, D + 0.05, x + 0.15, H + 0.48, D + 0.3, ls("galv"))
        path(b, [(x, H + 0.38, D + 0.22), (x, 0.25, D + 0.22), (x, 0.06, D + 0.55)], 0.05, ls("galv"))
        for y in (1.0, 2.6):
            b.box(x - 0.04, y - 0.03, D + 0.05, x + 0.04, y + 0.03, D + 0.17, ls("galv"))
        b.box(x - 0.3, -0.02, D + 0.4, x + 0.3, 0.02, D + 0.9, c)                                 # splash block
    b.box(18.0, H + 0.32, 6.0, 19.0, H + 0.55, 7.0, sw("mgrey"))                                   # roof hatch
    b.box(17.95, H + 0.55, 5.95, 19.05, H + 0.6, 7.05, sw("mgrey"))
    # Name stencilled on the front, engraved plates by the doors.
    stencil(b, (13.0, H - 0.5, -0.004), "-z", "345 kV RELAY HOUSE", 0.26)
    tag(b, (fd + 0.48, dh + 0.25, -0.005), "-z", "RELAY ROOM", 0.3, 0.05)
    tag(b, (bd + 0.48, dh + 0.25, -0.005), "-z", "BATTERY ROOM  -  NO SMOKING  -  HYDROGEN", 0.6, 0.05)
    tag(b, (L + 0.005, 2.85, (e0 + e1) / 2), "x", "EQUIPMENT DOOR", 0.36, 0.05)
    # Doors.
    _door(b, "x", 0.0, fd, fd + 0.95, dh, -1, vision=True)
    _door(b, "x", 0.0, bd, bd + 0.95, dh, -1, mat=sw("dgrey"), hinge_lo=False)
    _door(b, "z", L, e0, e1, 2.6, 1, leaves=2)
    # Wall lights over each door (lit), with their conduits.
    for x, z, n_ in ((fd + 0.48, -0.004, "-z"), (bd + 0.48, -0.004, "-z")):
        b.box(x - 0.15, dh + 0.42, -0.14, x + 0.15, dh + 0.62, 0.0, sw("dgrey"))
        b.box(x - 0.12, dh + 0.41, -0.12, x + 0.12, dh + 0.42, -0.02, CB.LENS["white"], b.lit)
    b.box(L, 2.85 + 0.25, (e0 + e1) / 2 - 0.15, L + 0.14, 3.3, (e0 + e1) / 2 + 0.15, sw("dgrey"))
    b.box(L + 0.02, 3.09, (e0 + e1) / 2 - 0.12, L + 0.12, 3.1, (e0 + e1) / 2 + 0.12, CB.LENS["white"], b.lit)
    # Two wall-mounted air conditioners (lead / lag) on the back wall through their sleeves; condensate drains.
    for x in (9.7, 15.7):
        b.box(x - 0.55, 0.9, D, x + 0.55, 3.15, D + 0.6, ls("enclosure_grey"))
        b.box(x - 0.57, 3.15, D - 0.02, x + 0.57, 3.2, D + 0.63, ls("enclosure_grey"))
        face_quad(b, (x, 1.55, D + 0.603), "z", 0.9, 0.9, "louvre")
        face_quad(b, (x - 0.553, 2.5, D + 0.3), "-x", 0.45, 0.8, "louvre")
        face_quad(b, (x + 0.553, 2.5, D + 0.3), "x", 0.45, 0.8, "louvre")
        b.box(x - 0.5, 2.0, D - t - 0.02, x + 0.5, 2.9, D - t, sw("mgrey"))                       # its grille inside
        path(b, [(x + 0.4, 0.95, D + 0.45), (x + 0.4, 0.3, D + 0.45), (x + 0.4, 0.05, D + 0.8)], 0.015, ls("polymer"))
        tag(b, (x, 2.95, D + 0.604), "z", f"HVAC {'1' if x < 12 else '2'}", 0.22, 0.045)
    # Battery-room exhaust fan high on the -x end wall: a hooded fan, its louvre; a make-up air louvre low on the front.
    b.box(-0.45, 2.95, 1.45, 0.0, 3.85, 2.35, ls("galv"))
    face_quad(b, (-0.453, 3.3, 1.9), "-x", 0.7, 0.5, "louvre")
    # Station service on the +x end wall: AC disconnect, the transfer switch and the portable-generator receptacle box,
    # their conduits down into the ground.
    for z0, z1, y0, y1, nm in ((1.0, 1.6, 1.2, 2.0, "AC SERVICE DISC"), (1.9, 2.8, 0.9, 2.1, "TRANSFER SWITCH"), (3.1, 3.6, 0.9, 1.5, "GEN RECEPTACLE")):
        b.box(L, y0, z0, L + 0.25, y1, z1, sw("mgrey"))
        tag(b, (L + 0.253, y1 - 0.08, (z0 + z1) / 2), "x", nm, 0.4 if z1 - z0 > 0.7 else 0.36, 0.04)
        path(b, [(L + 0.12, y0, (z0 + z1) / 2), (L + 0.12, 0.1, (z0 + z1) / 2), (L + 0.12, -0.3, (z0 + z1) / 2)], 0.03, ls("galv"))
    b.box(L + 0.25, 1.4, 1.2, L + 0.32, 1.6, 1.4, ls("galv"))                                        # disconnect handle
    # Cable entry: the yard's cable trench comes in under the front wall into the floor trench; its precast covers.
    tx = T["trench_x"]
    b.box(tx - 0.75, -0.9, -9.0, tx + 0.75, -0.02, -0.4, [c, c, c, c, None, c])                      # trench walls (outside)
    for z in np.arange(-9.0, -0.4, 1.0):
        b.box(tx - 0.72, -0.02, z + 0.01, tx + 0.72, 0.06, z + 0.99, ls("concrete_spun"))
        for dx in (-0.4, 0.4):
            b.box(tx + dx - 0.04, 0.06, z + 0.45, tx + dx + 0.04, 0.08, z + 0.55, ls("galv"))          # lifting eyes
    b.box(tx - 1.0, -0.9, -10.2, tx + 1.0, 0.1, -9.0, c)                                             # pull box
    b.box(tx - 0.9, 0.1, -10.1, tx + 0.9, 0.12, -9.1, ls("galv"))
    tag(b, (tx, 0.124, -9.6), "y", "CABLE PULL BOX  345 kV YARD", 0.5, 0.05)
    # Ground risers at the corners and the building ground bar by the front door.
    for x, z in ((-0.05, -0.05), (L + 0.05, -0.05), (-0.05, D + 0.05), (L + 0.05, D + 0.05)):
        sx, sz = (1 if x > 1 else -1), (1 if z > 1 else -1)
        path(b, [(x + sx * 0.03, 0.6, z + sz * 0.03), (x + sx * 0.03, 0.05, z + sz * 0.03), (x + sx * 0.4, -0.02, z + sz * 0.4)], 0.012, ls("copper_wire"))
        b.box(x + sx * 0.0 - 0.04, 0.55, z - 0.04, x + 0.04, 0.65, z + 0.04, ls("copper_wire"))
    # ---------------- inside
    # Partition walls (block, painted) round the battery room and the comm room, their doors.
    pb = "paint_grey"
    bx0, bx1, bz0, bz1 = T["batt"]
    _wall_with_openings(b, "z", bx1 + 0.15, t, bz1, 0.0, H, 0.15, [(1.7, 2.2, 0.25, 0.65), (3.4, 4.35, 0, dh)], pb, 1)   # battery room to relay room
    # Make-up air: a transfer grille low in the partition (from the conditioned relay room), across the room from the
    # exhaust fan high on the end wall, so the air sweeps both racks.
    for xg, ng in ((bx1 + 0.02, "-x"), (bx1 + 0.13, "x")):
        face_quad(b, (xg, 0.45, 1.95), ng, 0.5, 0.4, "louvre")
    _wall_with_openings(b, "x", bz1 + 0.15, t, bx1 + 0.15, 0.0, H, 0.15, [], pb, 1)
    cx0, cx1, cz0, cz1 = T["comm"]
    _wall_with_openings(b, "z", cx1 + 0.15, cz0 + 0.15, D - t, 0.0, H, 0.15, [(8.2, 9.15, 0, dh), (9.38, 9.68, 3.2, 3.42)], pb, 1)
    _wall_with_openings(b, "x", cz0, t, cx1 + 0.15, 0.0, H, 0.15, [], pb, -1)
    _door(b, "z", bx1 + 0.15, 3.4, 4.35, dh, 1, panic=True)
    _door(b, "z", cx1 + 0.15, 8.2, 9.15, dh, 1, panic=False, vision=True)
    # Floor: sealed concrete, the floor trenches with checker-plate covers along the panel rows and to the entry.
    b.box(t, 0.0, t, L - t, 0.01, D - t, [None] * 4 + [ls("concrete_spun"), None])
    for (x0, z0, x1, z1) in ((tx - 0.45, t, tx + 0.45, 5.8), (6.0, 5.8, L - 1.0, 6.5), (6.0, 9.0, L - 1.0, 9.7)):
        b.box(x0, 0.01, z0, x1, 0.016, z1, ls("galv"))
        for xx in np.arange(x0 + 0.6, x1 - 0.1, 0.6) if x1 - x0 > z1 - z0 else []:
            b.box(xx - 0.004, 0.016, z0, xx + 0.004, 0.018, z1, ls("galv_dark"))
        for zz in np.arange(z0 + 0.6, z1 - 0.1, 0.6) if z1 - z0 > x1 - x0 else []:
            b.box(x0, 0.016, zz - 0.004, x1, 0.018, zz + 0.004, ls("galv_dark"))
    # Overhead ladder tray over the panel rows, on threaded-rod hangers; its drop to the comm room.
    for zc in (6.15, 9.35):
        for z in (zc - 0.3, zc + 0.3):
            b.box(6.0, 3.25, z - 0.012, L - 1.0, 3.35, z + 0.012, ls("galv"))
        for x in np.arange(6.2, L - 1.0, 0.3):
            b.box(x - 0.012, 3.255, zc - 0.288, x + 0.012, 3.275, zc + 0.288, ls("galv"))
        for x in np.arange(6.5, L - 1.0, 2.0):
            for z in (zc - 0.34, zc + 0.34):
                rod(b, (x, 3.24, z), (x, H, z), 0.006, ls("galv"), 4)
            b.box(x - 0.02, 3.2, zc - 0.36, x + 0.02, 3.24, zc + 0.36, ls("galv"))
    b.box(5.4, 3.25, 6.1, 6.0, 3.35, 9.4, ls("galv"))                                                # link between rows
    # Lights (LED fixtures, lit), smoke detectors, exit signs over the doors, extinguishers on solid walls.
    for x in np.arange(7.0, L - 0.5, 3.0):
        for z in (4.2, 7.8, 10.8):
            b.box(x - 0.6, H - 0.06, z - 0.15, x + 0.6, H, z + 0.15, sw("white"))
            b.box(x - 0.56, H - 0.065, z - 0.12, x + 0.56, H - 0.06, z + 0.12, CB.LENS["white"], b.lit)
    for x, z in ((2.5, 2.6), (2.5, 9.5), (10.0, 4.5), (16.0, 4.5), (21.0, 8.0)):
        cyl(b, (x, H, z), (x, H - 0.04, z), 0.06, sw("white"), sides=12)
        cyl(b, (x, H - 0.04, z), (x, H - 0.045, z), 0.02, CB.LENS["red"], sides=8, lst=b.lit)
    for x, z, n_ in ((fd + 0.48, t + 0.003, "z"), (bd + 0.48, t + 0.003, "z")):
        b.box(x - 0.18, dh + 0.18, t, x + 0.18, dh + 0.36, t + 0.06, sw("white"))
        face_quad(b, (x, dh + 0.27, t + 0.062), "z", 0.3, 0.12, ATLAS.mat(ATLAS.add("exit_sign", TY.flag_img("EXIT", (30, 150, 70), (240, 255, 240), 0.3, 0.12))), lst=b.lit)
    b.box(L - t - 0.06, 2.42, (e0 + e1) / 2 - 0.18, L - t, 2.6, (e0 + e1) / 2 + 0.18, sw("white"))
    face_quad(b, (L - t - 0.062, 2.51, (e0 + e1) / 2), "-x", 0.3, 0.12, ATLAS.mat(ATLAS.add("exit_sign", TY.flag_img("EXIT", (30, 150, 70), (240, 255, 240), 0.3, 0.12))), lst=b.lit)
    for x, z, n_ in ((8.25, t + 0.004, "z"), (L - t - 0.004, 10.5, "-x")):
        nv = TY._v(n_)
        p = np.array([x, 0.9, z])
        b.box(*_rnd(np.minimum(p - [0.04, 0, 0.04], p + [0.04, 0.12, 0.04] + nv * 0.05)), *_rnd(np.maximum(p - [0.04, 0, 0.04], p + [0.04, 0.12, 0.04] + nv * 0.05)), ls("galv"))
        cyl(b, p + nv * 0.12 + [0, 0.12, 0], p + nv * 0.12 + [0, 0.55, 0], 0.075, TY.RED, sides=12)
        cyl(b, p + nv * 0.12 + [0, 0.55, 0], p + nv * 0.12 + [0, 0.65, 0], 0.035, BLACK, sides=8)
    # Battery room: eyewash station, the hydrogen detector high, a spill kit; (the racks come with the battery machine).
    ex, ez = bx0 + t + 0.05, 3.6
    b.box(ex, 0.0, ez - 0.25, ex + 0.35, 1.0, ez + 0.25, sw("green"))
    cyl(b, (ex + 0.25, 1.0, ez), (ex + 0.25, 1.05, ez), 0.18, sw("yellow"), sides=16)
    for dz in (-0.08, 0.08):
        cyl(b, (ex + 0.25, 1.05, ez + dz), (ex + 0.25, 1.12, ez + dz), 0.03, sw("green"), sides=10)
    tag(b, (ex + 0.002, 1.5, ez), "x", "EMERGENCY EYEWASH", 0.4, 0.05)
    b.box(bx1 - 0.25, 3.5, t + 0.3, bx1 + 0.0, 3.75, t + 0.6, sw("white"))                          # H2 detector
    b.box(bx1 - 0.45, 0.0, 4.4, bx1 - 0.05, 0.6, 4.95, sw("yellow"))                                 # spill kit
    tag(b, (bx1 - 0.25, 0.45, 4.398), "-z", "ACID SPILL KIT", 0.32, 0.045)
    return b


# ================================================================ machine 2: the protection relay panels

from PIL import Image, ImageDraw

PANEL = dict(W=0.8, D=0.6, H=2.29, plinth=0.1)


def lcd_img(lines, w_px=360, h_px=150, bg=(118, 168, 196), fg=(20, 34, 44)):
    """A relay's backlit LCD: dark text on the blue-green backlight."""
    im = Image.new("RGBA", (w_px, h_px), bg + (255,))
    d = ImageDraw.Draw(im)
    fs = int(h_px / (len(lines) + 0.6))
    size = fs * 0.86
    while size > 8 and max(d.textlength(t, font=TY._font(size, cond=True)) for t in lines) > w_px - 18:
        size -= 1
    for i, t in enumerate(lines):
        d.text((10, 6 + i * fs), t, font=TY._font(size, cond=True), fill=fg)
    return im


def led_label_img(labels, w_px=200, h_px=200):
    """The printed legend strip beside a relay's target LEDs."""
    im = Image.new("RGBA", (w_px, h_px), (52, 56, 64, 255))
    d = ImageDraw.Draw(im)
    fs = h_px / len(labels)
    for i, t in enumerate(labels):
        d.text((4, i * fs + fs * 0.12), t, font=TY._font(fs * 0.62, cond=True), fill=(214, 218, 222))
    return im


class PanelFrame:
    """Panel-local coordinates: u across from the viewer's left, y up, w back from the front plane. facing -1: the
    front looks toward -z; +1: toward +z."""

    def __init__(self, b, x0, zf, facing, W=None):
        self.b, self.x0, self.zf, self.f = b, x0, zf, facing
        self.W = W if W is not None else PANEL["W"]
        self.n = "-z" if facing < 0 else "z"
        self.nv = np.array([0, 0, -1.0]) if facing < 0 else np.array([0, 0, 1.0])

    def P(self, u, y, w):
        if self.f < 0:
            return np.array([self.x0 + u, y, self.zf + w])
        return np.array([self.x0 + self.W - u, y, self.zf - w])

    def box(self, u0, y0, w0, u1, y1, w1, mat, lst=None):
        a, c = self.P(u0, y0, w0), self.P(u1, y1, w1)
        self.b.box(*_rnd(np.minimum(a, c)), *_rnd(np.maximum(a, c)), mat, lst)

    def quad(self, u, y, w, wd, ht, mat, lst=None):
        face_quad(self.b, self.P(u, y, w), self.n, wd, ht, mat, lst)

    def cyl(self, u, y, w0, w1, r, mat, sides=10, lst=None):
        cyl(self.b, self.P(u, y, w0), self.P(u, y, w1), r, mat, sides=sides, lst=lst)

    def tag(self, u, y, w, text, wd=0.3, ht=0.035):
        tag(self.b, self.P(u, y, w), self.n, text, wd, ht)


def relay_case(F, u0, u1, y0, y1, lcd_lines, leds, labels, model, name, on_leds=(0,)):
    """A 19-inch rack-mounted microprocessor relay: steel case into the rack, the front bezel, the LCD set back behind
    its frame (lit), the target LEDs (lit when on) with their legend, navigation and target-reset buttons, the front
    serial port, the model plate; its engraved tag above."""
    b = F.b
    F.box(u0, y0, -0.012, u1, y1, 0.3, ls("galv_dark"))                          # case into the rack
    F.box(u0 - 0.008, y0 - 0.004, -0.018, u1 + 0.008, y1 + 0.004, -0.012, sw("dgrey"))   # bezel
    w, h = u1 - u0, y1 - y0
    su0, su1, sy0, sy1 = u0 + 0.03, u0 + 0.03 + w * 0.36, y0 + h * 0.35, y1 - 0.022
    for a0, c0, a1, c1 in ((su0 - 0.008, sy0 - 0.008, su1 + 0.008, sy0), (su0 - 0.008, sy1, su1 + 0.008, sy1 + 0.008),
                           (su0 - 0.008, sy0, su0, sy1), (su1, sy0, su1 + 0.008, sy1)):
        F.box(a0, c0, -0.026, a1, c1, -0.018, BLACK)                              # LCD frame
    key = ATLAS.add(f"lcd:{name}", lcd_img(lcd_lines))
    F.quad((su0 + su1) / 2, (sy0 + sy1) / 2, -0.021, su1 - su0, sy1 - sy0, ATLAS.mat(key), lst=b.lit)
    # target LEDs, two columns of eight, legend strips beside them
    for i in range(len(leds)):
        col, row = i // 8, i % 8
        lu = u0 + w * 0.47 + col * 0.11
        ly = y1 - 0.02 - row * (h - 0.04) / 8
        on = i in on_leds
        lens = CB.LENS[leds[i]] if on else CB.LENS["off_" + leds[i]]
        F.box(lu - 0.004, ly - 0.004, -0.024, lu + 0.004, ly + 0.004, -0.018, lens, b.lit if on else None)
    for col in range(2):
        lab = labels[col * 8:(col + 1) * 8]
        if lab:
            key2 = ATLAS.add(f"ledlab:{name}:{col}", led_label_img(lab))
            F.quad(u0 + w * 0.47 + col * 0.11 + 0.05, y1 - 0.02 - (h - 0.04) * 3.5 / 8, -0.0205, 0.085, (h - 0.04) * 8 / 8, ATLAS.mat(key2))
    # navigation pad, ENT / ESC, target reset; the front port
    nu, ny = u0 + w * 0.76, y0 + h * 0.42
    for du, dy in ((0, 0.022), (0, -0.022), (-0.022, 0), (0.022, 0), (0, 0)):
        F.box(nu + du - 0.008, ny + dy - 0.008, -0.03, nu + du + 0.008, ny + dy + 0.008, -0.018, sw("mgrey"))
    for k, colr in enumerate(("mgrey", "mgrey", "blue")):
        bu = u0 + w * 0.88
        by = y1 - 0.03 - k * 0.03
        F.box(bu - 0.012, by - 0.008, -0.028, bu + 0.012, by + 0.008, -0.018, sw(colr))
    F.box(u0 + w * 0.88 - 0.012, y0 + 0.02, -0.022, u0 + w * 0.88 + 0.012, y0 + 0.034, -0.018, BLACK)    # serial port
    F.tag(u0 + 0.12, y0 + 0.012, -0.0185, model, 0.18, 0.016)
    F.tag((u0 + u1) / 2, y1 + 0.03, -0.0185, name, 0.42, 0.032)
    for uu in (u0 - 0.003, u1 + 0.003):                                        # rack screws
        for yy in (y0 + 0.012, y1 - 0.012):
            F.cyl(uu, yy, -0.018, -0.024, 0.005, ls("stainless"), sides=6)


def test_switch(F, u0, y0, n, name, red=(0, 1, 2, 3)):
    """An FT-style test switch: black base, a row of knife-blade switches (red handles for the trip and CT circuits),
    the clear cover's rails; its tag."""
    pitch = 0.024
    F.box(u0 - 0.01, y0 - 0.012, -0.02, u0 + n * pitch + 0.01, y0 + 0.11, -0.004, BLACK)
    for i in range(n):
        uu = u0 + i * pitch
        F.box(uu + 0.003, y0, -0.024, uu + pitch - 0.004, y0 + 0.1, -0.02, sw("enamel"))
        F.box(uu + 0.007, y0 + 0.045, -0.055, uu + pitch - 0.008, y0 + 0.085, -0.024, sw("red") if i in red else BLACK)
        F.cyl(uu + pitch / 2, y0 + 0.018, -0.024, -0.03, 0.005, ls("copper_wire"), sides=6)
    for yy in (y0 - 0.008, y0 + 0.104):
        F.box(u0 - 0.008, yy - 0.003, -0.062, u0 + n * pitch + 0.008, yy + 0.003, -0.02, ls("stainless"))
    F.tag(u0 + n * pitch / 2, y0 + 0.13, -0.0045, name, 0.3, 0.022)


def lockout_86(F, u, y, name, tripped=False):
    """An 86 lockout switch: the round black escutcheon, the red pistol-grip handle (vertical when reset), its target
    window, its tag."""
    F.cyl(u, y, -0.004, -0.03, 0.05, BLACK, sides=16)
    t = math.radians(-45 if tripped else 90)
    d = (math.cos(t), math.sin(t))
    a = F.P(u - d[0] * 0.04, y - d[1] * 0.04, -0.045)
    c = F.P(u + d[0] * 0.055, y + d[1] * 0.055, -0.045)
    bar(F.b, a, c, 0.024, sw("red"), h=0.03)
    F.box(u - 0.012, y - 0.042, -0.032, u + 0.012, y - 0.03, -0.03, CB.LENS["amber"] if tripped else sw("white"))
    F.tag(u, y + 0.085, -0.0045, name, 0.16, 0.022)


def mcb_row(F, u0, y, names):
    """Miniature DC breakers on a DIN rail (handles up = on), their labels."""
    F.box(u0, y - 0.018, -0.012, u0 + len(names) * 0.036 + 0.02, y + 0.018, -0.004, ls("galv"))
    for i, nm in enumerate(names):
        uu = u0 + 0.01 + i * 0.036
        F.box(uu, y - 0.045, -0.07, uu + 0.034, y + 0.045, -0.012, sw("white"))
        F.box(uu + 0.011, y + 0.004, -0.085, uu + 0.023, y + 0.03, -0.07, BLACK)
    F.tag(u0 + len(names) * 0.018 + 0.01, y + 0.07, -0.0045, "125 VDC  " + "  ".join(names), max(0.2, len(names) * 0.06), 0.02)


def terminal_strip(F, u0, u1, y):
    """Terminal blocks on a DIN rail, wires down into a slotted duct, the duct along the bottom."""
    F.box(u0, y - 0.016, -0.012, u1, y + 0.016, -0.004, ls("galv"))
    n = int((u1 - u0) / 0.012)
    for i in range(n):
        uu = u0 + 0.002 + i * 0.012
        F.box(uu, y - 0.034, -0.052, uu + 0.01, y + 0.034, -0.012, sw("blue") if i % 6 == 0 else sw("yellow"))
        if i % 2 == 0:
            col = (sw("red"), BLACK, sw("white"), sw("blue"))[(i // 2) % 4]
            F.box(uu + 0.003, y - 0.13, -0.032, uu + 0.007, y - 0.034, -0.028, col)
    F.box(u0, y - 0.21, -0.07, u1, y - 0.13, -0.004, sw("mgrey"))
    for uu in np.arange(u0 + 0.01, u1 - 0.01, 0.02):
        F.box(uu, y - 0.134, -0.071, uu + 0.008, y - 0.128, -0.069, BLACK)


PANEL_KINDS = {
    "line": dict(title="{n}  LINE PROTECTION", relays=[("21/87L-1  PRIMARY", "LINE DIFF  87L 21"), ("21/67-2  SECONDARY", "DISTANCE  21 67N")],
                 lockouts=["86L-1", "86L-2"], leds=["green", "red", "red", "red", "red", "amber", "amber", "red", "red", "red", "amber", "red", "red", "red", "red", "green"],
                 labels=["ENABLED", "TRIP", "87L", "ZONE 1", "ZONE 2", "COMM", "RECLOSE", "PHASE A", "PHASE B", "PHASE C", "GROUND", "67N", "50", "SOTF", "TIME", "TEST"]),
    "bus": dict(title="{n}  BUS DIFFERENTIAL", relays=[("87B-1  PRIMARY", "BUS DIFF  87B 50BF"), ("87B-2  SECONDARY", "BUS DIFF  87B")],
                lockouts=["86B-1", "86B-2"], leds=["green", "red", "red", "red", "red", "red", "amber", "amber"] * 2,
                labels=["ENABLED", "TRIP", "87B", "ZONE 1", "ZONE 2", "CT OPEN", "CHECK", "ALARM", "PHASE A", "PHASE B", "PHASE C", "50", "51", "TEST", "COMM", "FAULT"]),
    "breaker": dict(title="{n}  BREAKER FAILURE / RECLOSE", relays=[("50BF/79  {b1}", "BKR CONTROL  50BF 79 25"), ("50BF/79  {b2}", "BKR CONTROL  50BF 79 25")],
                    lockouts=["86BF-{b1}", "86BF-{b2}"], leds=["green", "red", "red", "amber", "amber", "green", "red", "amber"] * 2,
                    labels=["ENABLED", "TRIP", "50BF", "79 RESET", "79 LOCKOUT", "SYNC OK", "BKR OPEN", "SF6 ALARM", "PHASE A", "PHASE B", "PHASE C", "RETRIP", "TRIP 1", "TRIP 2", "TEST", "COMM"]),
    "xfmr": dict(title="{n}  TRANSFORMER PROTECTION", relays=[("87T-1  PRIMARY", "XFMR DIFF  87T 51 63"), ("87T-2  SECONDARY", "XFMR DIFF  87T 51G")],
                 lockouts=["86T-1", "86T-2"], leds=["green", "red", "red", "red", "red", "amber", "amber", "red"] * 2,
                 labels=["ENABLED", "TRIP", "87T", "87U", "REF", "63 BUCHHOLZ", "26 OIL TEMP", "49 WDG", "PHASE A", "PHASE B", "PHASE C", "51", "51G", "TEST", "COMM", "FAULT"]),
}


def panel_shell(F, facing, title):
    """The 19-inch swing-rack panel's enclosure (shared by the relay panels, the fault recorder and the SCADA
    cabinets): plinth, sides, top, back, the header with its engraved title, side louvres, the swing frame's rails and
    hinges."""
    b = F.b
    W, D, H, pl = PANEL["W"], PANEL["D"], PANEL["H"], PANEL["plinth"]
    g = sw("enamel")
    F.box(0.035, 0.0, 0.03, W - 0.035, pl, D - 0.03, sw("dgrey"))                       # plinth
    F.box(0.0, pl, 0.0, 0.03, H, D, g)                                                  # sides, top, back
    F.box(W - 0.03, pl, 0.0, W, H, D, g)
    F.box(0.03, H - 0.03, 0.0, W - 0.03, H, D, g)
    F.box(0.03, pl, D - 0.02, W - 0.03, H - 0.03, D, g)
    F.box(0.03, H - 0.25, 0.0, W - 0.03, H - 0.03, 0.02, g)                              # header with the title
    F.tag(W / 2, H - 0.14, -0.0045, title, 0.66, 0.05)
    for side_u in (-0.0045, W + 0.0045):                                                # side louvres
        nn = "-x" if (side_u < 0) == (facing < 0) else "x"
        face_quad(b, F.P(side_u, 1.9, D / 2), nn, 0.3, 0.2, "louvre")
    # the swing frame (rails with holes), its hinges on the left
    for uu in (0.08, W - 0.08):
        F.box(uu - 0.02, pl + 0.05, 0.005, uu + 0.02, H - 0.27, 0.025, ls("galv"))
    for yy in (0.6, 1.9):
        F.cyl(0.05, yy, 0.0, 0.03, 0.012, ls("galv"), sides=8)


def relay_panel(b, x0, zf, facing, kind, n, readings, b1="", b2=""):
    """A 345 kV protection relay panel (real basis: a 19-inch swing-rack relay panel, 2.29 m, ANSI 61 grey, open
    front): the enclosure on its plinth, the swing frame on its hinges, the engraved panel title; two microprocessor
    relays (primary and secondary, different makers as utilities require), each with its FT test switch below; the
    86 lockout switches; a row of 125 VDC breakers, terminal strips and duct at the bottom; cables up through the gland
    plate from the floor trench; louvres, the panel light."""
    K = PANEL_KINDS[kind]
    W, D, H, pl = PANEL["W"], PANEL["D"], PANEL["H"], PANEL["plinth"]
    F = PanelFrame(b, x0, zf, facing)
    panel_shell(F, facing, K["title"].format(n=n))
    # relays and test switches
    y = H - 0.55
    for k, (nm, model) in enumerate(K["relays"]):
        nm = nm.format(b1=b1, b2=b2)
        lines = readings[k] if k < len(readings) else ["", "", "", ""]
        relay_case(F, 0.085, W - 0.085, y - 0.133, y, lines, K["leds"], K["labels"], model, nm)
        test_switch(F, 0.13, y - 0.31, 10, f"TS-{k + 1}  {nm.split()[0]}")
        y -= 0.52
    # lockouts
    for k, nm in enumerate(K["lockouts"]):
        lockout_86(F, 0.25 + k * 0.3, y + 0.08, nm.format(b1=b1, b2=b2))
    # DC breakers, terminals, duct; the gland plate and cables
    mcb_row(F, 0.1, y - 0.14, ["RLY 1", "RLY 2", "TRIP 1", "TRIP 2", "ALM"])
    terminal_strip(F, 0.1, W - 0.1, pl + 0.32)
    F.box(0.1, pl, 0.05, W - 0.1, pl + 0.012, D - 0.05, ls("galv"))                     # gland plate
    for k in range(6):
        uu = 0.16 + k * 0.09
        cyl(b, F.P(uu, pl + 0.012, 0.3), F.P(uu, pl + 0.11, 0.3), 0.016, BLACK, sides=8)
        cyl(b, F.P(uu, pl + 0.012, 0.3), F.P(uu, pl + 0.03, 0.3), 0.024, sw("dgrey"), sides=8)     # gland
    # the panel light on the inside top, lit
    F.box(0.2, H - 0.29, 0.05, W - 0.2, H - 0.25, 0.1, sw("white"))
    F.box(0.22, H - 0.295, 0.055, W - 0.22, H - 0.29, 0.095, CB.LENS["white"], b.lit)


def relay_room_desk(b, local=False):
    """The local control desk in the relay room's open end, the operator facing the panel rows' ends."""
    control_desk(b, 15.6, 4.2, 1, local)


def relay_room_panels(b):
    """The relay room's two rows over the floor trenches, fronts to the aisle between them."""
    T = RH
    rows = [
        (6.45, 1, [("line", "LINE 1"), ("line", "LINE 2"), ("line", "LINE 3"), ("bus", "BUS 1"), ("bus", "BUS 2"),
                   ("xfmr", "MT-1 BANK"), ("xfmr", "SAT-1")]),
        (9.0, -1, [("breaker", "BAY 1", "1-1", "1-2"), ("breaker", "BAY 1/2", "1-3", "2-1"), ("breaker", "BAY 2", "2-2", "2-3"),
                   ("breaker", "BAY 3", "3-1", "3-2"), ("breaker", "BAY 3 SPARE", "3-3", "SPARE")]),
    ]
    for zf, facing, panels in rows:
        x = 7.0
        if facing > 0:                                     # read left to right from the aisle
            panels = panels[::-1]
        for p in panels:
            kind, n = p[0], p[1]
            b1, b2 = (p[2], p[3]) if len(p) > 3 else ("", "")
            ln = "L" + n.split()[-1]
            amps = {"LINE 1": (412, 409, 415, 238, 41), "LINE 2": (388, 391, 386, 224, 37), "LINE 3": (0, 0, 0, 0, 0)}.get(n, (0, 0, 0, 0, 0))
            rd = {"line": [[f"{ln} 345 kV  87L OK", f"IA {amps[0]}A  IB {amps[1]}A", f"IC {amps[2]}A  3I0 {4 if amps[0] else 0}A",
                            f"P {amps[3]}MW  Q {amps[4]}MVAR"],
                           [f"{ln}  21/67N", "VA 199.2kV  F 60.00", "Z1 OK  Z2 OK", "LAST TRIP: NONE"]],
                  "bus": [["BUS 87B  OK", "IDIFF 0.01 PU", "IRES  1.92 PU", "ZONES 1 2 IN"], ["87B CHECK", "IDIFF 0.01 PU", "CT SUPV OK", "ALARMS: 0"]],
                  "xfmr": [["87T  OK", "I1 1180A  I2 1176A", "IDIFF 0.02 PU", "TOP OIL 71C"], ["87T/51G", "IG 0.0A", "WDG 84C", "ALARMS: 0"]],
                  "breaker": [[f"BKR {b1}  CLOSED", "SF6 0.64 MPA", "OPS 0412", "79 READY"], [f"BKR {b2}  CLOSED", "SF6 0.64 MPA", "OPS 0398", "79 READY"]]}[kind]
            relay_panel(b, x, zf if facing > 0 else zf, facing, kind, n, rd, b1, b2)
            x += PANEL["W"] + 0.002



# ================================================================ machine 3: the local control desk

def oneline_img(w_px=900, h_px=520, local=False):
    """The yard's live one-line on the HMI: two 345 kV buses, three breaker-and-a-half bays (red closed, green open),
    the main transformer bank, SAT-1 and the three lines with their MW, the LOCAL / REMOTE banner."""
    im = Image.new("RGBA", (w_px, h_px), (18, 22, 30, 255))
    d = ImageDraw.Draw(im)
    f = lambda s: TY._font(s, cond=True)
    d.text((14, 8), "345 kV SWITCHYARD  ONE-LINE", font=f(26), fill=(220, 226, 232))
    d.rectangle([w_px - 250, 6, w_px - 12, 38], fill=(160, 120, 20) if local else (30, 110, 60))
    d.text((w_px - 131, 22), "LOCAL CONTROL" if local else "REMOTE (CONTROL ROOM)", font=f(18), fill=(255, 255, 255), anchor="mm")
    yb1, yb2 = 80, h_px - 70
    d.line([(40, yb1), (w_px - 40, yb1)], fill=(230, 60, 60), width=6)
    d.line([(40, yb2), (w_px - 40, yb2)], fill=(230, 60, 60), width=6)
    d.text((44, yb1 - 30), "BUS 1  345.2 kV", font=f(18), fill=(230, 230, 230))
    d.text((44, yb2 + 8), "BUS 2  345.1 kV", font=f(18), fill=(230, 230, 230))
    bays = [("BAY 1", ["1-1", "1-2", "1-3"], ("MT-1  GEN", "LINE 1"), ("+246 MW", "238 MW")),
            ("BAY 2", ["2-1", "2-2", "2-3"], ("SAT-1", "LINE 2"), ("+4 MW", "224 MW")),
            ("BAY 3", ["3-1", "3-2", "3-3"], ("SPARE", "LINE 3"), ("", "0 MW"))]
    for k, (nm, bk, taps, mw) in enumerate(bays):
        x = 230 + k * 255
        d.line([(x, yb1), (x, yb2)], fill=(230, 60, 60), width=4)
        ys = [yb1 + (yb2 - yb1) * t for t in (0.2, 0.5, 0.8)]
        for b_, y in zip(bk, ys):
            closed = not (k == 2 and b_ == "3-3")
            col = (220, 40, 40) if closed else (40, 200, 80)
            d.rectangle([x - 16, y - 16, x + 16, y + 16], fill=col if closed else None, outline=col, width=4)
            d.text((x + 24, y - 10), b_, font=f(16), fill=(200, 206, 214))
        for (lbl, val), y in zip(zip(taps, mw), ((ys[0] + ys[1]) / 2, (ys[1] + ys[2]) / 2)):
            d.line([(x, y), (x - 90, y)], fill=(230, 60, 60), width=3)
            d.text((x - 96, y - 26), lbl, font=f(16), fill=(230, 230, 230), anchor="rm")
            d.text((x - 96, y + 6), val, font=f(16), fill=(120, 220, 255), anchor="rm")
        d.text((x - 20, yb2 + 32), nm, font=f(16), fill=(170, 176, 186))
    return im


def alarms_img(w_px=900, h_px=520):
    im = Image.new("RGBA", (w_px, h_px), (18, 22, 30, 255))
    d = ImageDraw.Draw(im)
    f = lambda s: TY._font(s, cond=True)
    d.text((14, 8), "ALARMS AND EVENTS", font=f(26), fill=(220, 226, 232))
    rows = [("14:02:11", "LINE 3  BKR 3-3 OPEN  (SPARE POSITION)", (230, 200, 60)),
            ("13:58:40", "SAT-1  FANS GROUP 1 ON", (200, 206, 214)),
            ("13:41:07", "MT-1C  TOP OIL 71 C", (200, 206, 214)),
            ("12:15:30", "LINE 2  79 RECLOSE SUCCESSFUL", (120, 220, 120)),
            ("12:15:28", "LINE 2  21 ZONE 1 TRIP  B-G  12.4 km", (240, 80, 70)),
            ("09:00:00", "BATTERY 125 VDC  FLOAT 132.6 V", (200, 206, 214)),
            ("08:30:12", "HVAC 1 LEAD  HVAC 2 STANDBY", (200, 206, 214))]
    for i, (t, txt, col) in enumerate(rows):
        y = 60 + i * 56
        d.rectangle([10, y - 4, w_px - 10, y + 44], fill=(28, 34, 44))
        d.text((20, y + 8), t, font=f(20), fill=(150, 156, 166))
        d.text((150, y + 8), txt, font=f(20), fill=col)
    return im


def trend_img(w_px=900, h_px=520):
    im = Image.new("RGBA", (w_px, h_px), (18, 22, 30, 255))
    d = ImageDraw.Draw(im)
    f = lambda s: TY._font(s, cond=True)
    d.text((14, 8), "LINE FLOWS  MW  (LAST 2 H)", font=f(26), fill=(220, 226, 232))
    x0, y0, x1, y1 = 60, 60, w_px - 30, h_px - 40
    for k in range(5):
        y = y0 + (y1 - y0) * k / 4
        d.line([(x0, y), (x1, y)], fill=(44, 52, 64), width=1)
        d.text((x0 - 8, y), f"{300 - 75 * k}", font=f(14), fill=(150, 156, 166), anchor="rm")
    import math as _m
    for col, base, amp, ph in (((240, 90, 70), 238, 8, 0.0), ((90, 180, 250), 224, 10, 1.2), ((120, 220, 120), 4, 1, 2.0)):
        pts = [(x0 + (x1 - x0) * i / 119, y1 - (y1 - y0) * (base + amp * _m.sin(i / 9 + ph) + (6 if i > 80 else 0)) / 300) for i in range(120)]
        d.line(pts, fill=col, width=3)
    d.text((x1 - 10, y0 + 6), "L1 238   L2 224   SAT 4", font=f(16), fill=(200, 206, 214), anchor="ra")
    return im


def monitor(F, u, y_foot, w, h, img_key):
    """A flat monitor on its stand standing on y_foot: thin bezel case, the lit screen, the neck and foot."""
    b = F.b
    y = y_foot + 0.12 + h / 2
    F.box(u - w / 2 - 0.012, y - h / 2 - 0.012, 0.0, u + w / 2 + 0.012, y + h / 2 + 0.03, 0.035, BLACK)
    F.quad(u, y + 0.006, -0.002, w, h, ATLAS.mat(img_key), lst=b.lit)
    F.box(u - 0.025, y_foot + 0.015, 0.04, u + 0.025, y - h / 2 + 0.05, 0.07, sw("dgrey"))
    F.box(u - 0.13, y_foot, -0.02, u + 0.13, y_foot + 0.015, 0.16, sw("dgrey"))


def control_desk(b, x0, zf, facing, local=False):
    """The relay house's local control desk (real basis: a substation HMI console): a three-bay steel console with a
    laminate top and a cable riser, three monitors (the yard one-line, alarms and events, line flows), keyboard and
    mouse, the LOCAL / REMOTE key switch with its lamps on a small panel, a desk phone with its handset on a coiled
    cord, the logbook and pen; the operator's chair; the computer under the desk."""
    W = 2.4
    F = PanelFrame(b, x0, zf, facing, W=W)
    # carcass: end panels, modesty panel, top, rear riser; drawers and the computer bay
    top_y, dep = 0.74, 0.9
    for u0 in (0.0, W - 0.04):
        F.box(u0, 0.0, -dep, u0 + 0.04, top_y, 0.0, sw("enamel"))
    F.box(0.04, 0.05, -0.05, W - 0.04, top_y - 0.04, -0.02, sw("enamel"))             # modesty panel (the operator side's back)
    F.box(-0.02, top_y, -dep - 0.03, W + 0.02, top_y + 0.035, 0.0, ls("enclosure_grey"))   # laminate top
    F.box(-0.02, top_y + 0.035, -0.02, W + 0.02, top_y + 0.36, 0.17, sw("enamel"))     # rear riser (cable way, monitor shelf)
    F.box(0.06, 0.04, -0.82, 0.5, top_y - 0.02, -0.06, sw("enamel"))                    # pedestal with drawers
    for k in range(3):
        y = 0.12 + k * 0.2
        F.box(0.08, y, -0.825, 0.48, y + 0.17, -0.82, sw("enamel"))
        F.box(0.22, y + 0.12, -0.84, 0.34, y + 0.135, -0.825, ls("stainless"))
    F.box(W - 0.48, 0.04, -0.75, W - 0.08, 0.52, -0.1, BLACK)                            # the HMI computer
    F.box(W - 0.46, 0.4, -0.755, W - 0.3, 0.42, -0.75, CB.LENS["green"], b.lit)
    # three monitors on the riser
    keys = [ATLAS.add("hmi:oneline:" + ("L" if local else "R"), oneline_img(local=local)), ATLAS.add("hmi:alarms", alarms_img()), ATLAS.add("hmi:trend", trend_img())]
    for k, key in enumerate(keys):
        monitor(F, 0.45 + k * 0.75, top_y + 0.36, 0.6, 0.345, key)
    # keyboard and mouse
    F.box(0.95, top_y + 0.035, -0.55, 1.45, top_y + 0.06, -0.38, BLACK)
    for r in range(4):
        for c in range(14):
            F.box(0.965 + c * 0.034, top_y + 0.06, -0.535 + r * 0.04, 0.99 + c * 0.034, top_y + 0.068, -0.505 + r * 0.04, sw("dgrey"))
    F.box(1.55, top_y + 0.035, -0.5, 1.61, top_y + 0.055, -0.41, BLACK)
    # LOCAL / REMOTE: a small sloped panel at the right with a key switch, LOCAL amber and REMOTE green lamps
    pu = 1.95
    F.box(pu - 0.2, top_y + 0.035, -0.32, pu + 0.2, top_y + 0.2, -0.12, sw("dgrey"))
    F.tag(pu, top_y + 0.17, -0.3205, "43 LOCAL / REMOTE", 0.3, 0.025)
    F.cyl(pu, top_y + 0.1, -0.32, -0.335, 0.03, ls("stainless"), sides=12)
    t = math.radians(135 if local else 45)
    a, c = F.P(pu - 0.012 * math.cos(t), top_y + 0.1 - 0.012 * math.sin(t), -0.34), F.P(pu + 0.022 * math.cos(t), top_y + 0.1 + 0.022 * math.sin(t), -0.34)
    bar(b, a, c, 0.008, ls("galv"), h=0.012)                                                # the key
    for k, (col, on) in enumerate((("amber", local), ("green", not local))):
        lu = pu - 0.12 + k * 0.24
        lens = CB.LENS[col] if on else CB.LENS["off_" + col]
        F.cyl(lu, top_y + 0.1, -0.32, -0.335, 0.016, ls("stainless"), sides=10)
        F.cyl(lu, top_y + 0.1, -0.335, -0.345, 0.012, lens, sides=10, lst=b.lit if on else None)
    # the phone: base, keypad, handset on its cradle, coiled cord
    ph = 0.3
    F.box(ph - 0.1, top_y + 0.035, -0.4, ph + 0.1, top_y + 0.09, -0.2, BLACK)
    for r in range(4):
        for c in range(3):
            F.box(ph - 0.04 + c * 0.027, top_y + 0.09, -0.36 + r * 0.025, ph - 0.022 + c * 0.027, top_y + 0.096, -0.344 + r * 0.025, sw("mgrey"))
    F.box(ph + 0.045, top_y + 0.09, -0.39, ph + 0.085, top_y + 0.12, -0.21, BLACK)
    path(b, [F.P(ph + 0.065, top_y + 0.1, -0.21), F.P(ph + 0.12, top_y + 0.05, -0.15), F.P(ph + 0.1, top_y + 0.045, -0.2)], 0.004, BLACK)
    # logbook and pen
    F.box(0.6, top_y + 0.035, -0.62, 0.86, top_y + 0.05, -0.3, sw("blue"))
    F.box(0.61, top_y + 0.05, -0.61, 0.85, top_y + 0.052, -0.31, sw("white"))
    bar(b, F.P(0.66, top_y + 0.056, -0.5), F.P(0.8, top_y + 0.056, -0.42), 0.007, BLACK)
    # the chair, pushed in a little: five-star base on casters, gas lift, seat, back
    cu, cw = 1.2, -1.25
    hub = F.P(cu, 0.1, cw)
    for k in range(5):
        a_ = 2 * math.pi * k / 5
        tip = hub + np.array([math.cos(a_) * 0.32, -0.02, math.sin(a_) * 0.32])
        bar(b, hub, tip, 0.04, BLACK, h=0.03)
        cyl(b, tip - [0, 0.03, 0], tip - [0, 0.07, 0], 0.03, BLACK, sides=8)
    cyl(b, hub, hub + [0, 0.35, 0], 0.025, ls("stainless"), sides=8)
    F.box(cu - 0.25, 0.45, cw - 0.25, cu + 0.25, 0.53, cw + 0.25, sw("dgrey"))
    F.box(cu - 0.06, 0.5, cw - 0.315, cu + 0.06, 0.62, cw - 0.285, BLACK)
    F.box(cu - 0.22, 0.6, cw - 0.33, cu + 0.22, 1.05, cw - 0.27, sw("dgrey"))


# ================================================================ machine 4: the 125 VDC station battery

CELL = dict(W=0.19, D=0.28, H=0.40, cover=0.025, gap=0.012, level=0.32, mn=0.305, mx=0.335, plates=11)
RACK = dict(n=15, front_y=0.18, back_y=0.46, front_w=0.04, back_w=0.40, depth=0.72, end=0.08, base=0.02)
RACK["W"] = 2 * RACK["end"] + RACK["n"] * CELL["W"] + (RACK["n"] - 1) * CELL["gap"]
BATT_LIVE = dict(volts=132.6, amps=0.4, pilot=27, pilot_v=2.21, pilot_t=24, mode="FLOAT")


def battery_cell(F, u0, y0, w0, number, flip=False):
    """One flooded lead-calcium cell (real basis: a 2 V, about 400 Ah stationary cell in a clear jar): the clear jar with
    the electrolyte in it, the plate pack (dark brown positive plates between grey negatives, 6 negatives / 5 positives
    standing on the jar's bridges over the sediment space), the lead straps over the plates and four lead posts up
    through the cover with their seal rings (red positive, black negative), the flame-arrestor vent, MIN / MAX level
    lines on the jar's front, the cell's engraved number. flip: positive posts on the right (a row that runs right to
    left). Returns the post positions {+: [...], -: [...]} as (u, w), front post first."""
    C = CELL
    W_, D_, H_ = C["W"], C["D"], C["H"]
    jar = ls("jar_clear")
    F.box(u0, y0, w0, u0 + W_, y0 + H_, w0 + D_, [jar, jar, jar, jar, None, jar])
    e = 0.006
    F.box(u0 + e, y0 + e, w0 + e, u0 + W_ - e, y0 + C["level"], w0 + D_ - e, ls("electrolyte"))
    for wb in (0.05, D_ - 0.05):                                                         # bridges
        F.box(u0 + 0.012, y0 + 0.002, w0 + wb - 0.008, u0 + W_ - 0.012, y0 + 0.055, w0 + wb + 0.008, jar)
    n = C["plates"]
    pw0, pw1 = w0 + 0.018, w0 + D_ - 0.018
    pitch = (pw1 - pw0) / n
    for k in range(n):
        a = pw0 + k * pitch + pitch * 0.18
        F.box(u0 + 0.016, y0 + 0.058, a, u0 + W_ - 0.016, y0 + 0.282, a + pitch * 0.64, ls("plate_neg" if k % 2 == 0 else "plate_pos"))
    pos_u = (u0 + W_ - 0.045) if flip else (u0 + 0.045)
    neg_u = (u0 + 0.045) if flip else (u0 + W_ - 0.045)
    top = y0 + H_
    posts = {"+": [], "-": []}
    for pol, pu in (("+", pos_u), ("-", neg_u)):
        F.box(pu - 0.02, y0 + 0.282, pw0 + 0.004, pu + 0.02, y0 + 0.302, pw1 - 0.004, ls("lead"))       # strap
        for wp in (w0 + 0.075, w0 + D_ - 0.075):
            cyl(F.b, F.P(pu, y0 + 0.302, wp), F.P(pu, top + C["cover"] + 0.045, wp), 0.017, ls("lead"), sides=12)
            cyl(F.b, F.P(pu, top + C["cover"], wp), F.P(pu, top + C["cover"] + 0.008, wp), 0.028,
                TY.RED if pol == "+" else BLACK, sides=12)
            posts[pol].append((pu, wp))
    F.box(u0 - 0.003, top, w0 - 0.003, u0 + W_ + 0.003, top + C["cover"], w0 + D_ + 0.003, sw("dgrey"))   # cover
    vc = F.P(u0 + W_ / 2, top + C["cover"], w0 + D_ / 2)
    cyl(F.b, vc, vc + [0, 0.03, 0], 0.022, sw("mgrey"), sides=12)                         # flame-arrestor vent
    cyl(F.b, vc + [0, 0.03, 0], vc + [0, 0.042, 0], 0.027, sw("dgrey"), sides=12)
    for yl in (C["mn"], C["mx"]):
        F.box(u0 + 0.06, y0 + yl - 0.002, w0 - 0.003, u0 + 0.13, y0 + yl + 0.002, w0, sw("red"))
    F.tag(u0 + W_ / 2, top + C["cover"] / 2, w0 - 0.003, str(number), 0.05, 0.018)
    return posts


def intercell(F, pa, pb, y):
    """Lead-plated copper intercell connectors between two cells' posts (one per pair), bolted on top of the posts."""
    for (ua, wa), (ub, wb) in zip(pa, pb):
        F.box(min(ua, ub) - 0.024, y, wa - 0.014, max(ua, ub) + 0.024, y + 0.012, wa + 0.014, ls("lead"))
        for u in (ua, ub):
            cyl(F.b, F.P(u, y + 0.012, wa), F.P(u, y + 0.03, wa), 0.012, ls("stainless"), sides=6)


def battery_rack(b, x0, zf, facing, first, name, rtl=False):
    """A two-step seismic battery rack with 30 cells (real basis: a zone-4 two-step rack: welded end frames, base
    channels bolted down on anchor plates, insulated rails under each row, cell restraint rails at mid-height front and
    back, X bracing; a spill containment pan with its yellow berm under it). The front step's 15 cells run left to
    right (rtl: right to left) from first, the back step's 15 come back the other way; a pair of insulated jumpers
    joins the steps. Returns the two series ends ({"+": first's terminal, "-": the last cell's}) as world points."""
    R, C = RACK, CELL
    W = R["W"]
    F = PanelFrame(b, x0, zf, facing, W=W)
    y0, D = R["base"], R["depth"]
    steel, rail = sw("dgrey"), ls("polymer")
    # spill containment: the liner pan and its berm
    F.box(-0.12, 0.01, -0.12, W + 0.12, y0, D + 0.06, ls("rubber_black"))
    for (a0, a1, c0, c1) in ((-0.16, W + 0.16, -0.16, -0.12), (-0.16, W + 0.16, D + 0.06, D + 0.1),
                             (-0.16, -0.12, -0.12, D + 0.06), (W + 0.12, W + 0.16, -0.12, D + 0.06)):
        F.box(a0, 0.01, c0, a1, 0.13, c1, ls("guard_yellow"))
    # end frames on anchor plates; a low middle frame under the cells
    for k, u in enumerate((0.0, W / 2 - 0.025, W - 0.05)):
        mid = k == 1
        yb = y0 if mid else y0 + 0.006
        if not mid:
            for wb in (0.03, D - 0.03):
                ua = u - 0.03 if k == 0 else u - 0.03
                F.box(u - 0.03 if k == 0 else u - 0.03, y0, wb - 0.035, u + 0.08, y0 + 0.006, wb + 0.035, steel)
                ub = u - 0.012 if k == 0 else u + 0.062
                cyl(b, F.P(ub, y0 + 0.006, wb), F.P(ub, y0 + 0.03, wb), 0.008, ls("galv"), sides=6)
        F.box(u, yb, 0.0, u + 0.05, y0 + 0.046, D, steel)                                              # base channel
        tops = (R["front_y"] - 0.07, R["back_y"] - 0.07, R["back_y"] - 0.07) if mid else (0.40, 0.70, 0.70)
        for wp, top in zip((0.0, 0.34, D - 0.05), tops):
            F.box(u, y0 + 0.046, wp, u + 0.05, top, wp + 0.05, steel)
        F.box(u + 0.005, R["front_y"] - 0.12, 0.05, u + 0.045, R["front_y"] - 0.07, 0.34, steel)     # front step cross
        F.box(u + 0.005, R["back_y"] - 0.12, 0.39, u + 0.045, R["back_y"] - 0.07, D - 0.05, steel)    # back step cross
        if not mid:
            bar(b, F.P(u + 0.025, y0 + 0.07, 0.39), F.P(u + 0.025, 0.66, D - 0.05), 0.008, steel, h=0.03)
    # X bracing on the back
    ya, yz = y0 + 0.09, 0.64
    for k, (a, c) in enumerate((((0.06, ya), (W / 2 - 0.04, yz)), ((0.06, yz), (W / 2 - 0.04, ya)),
                                ((W / 2 + 0.04, ya), (W - 0.06, yz)), ((W / 2 + 0.04, yz), (W - 0.06, ya)))):
        wx = D - 0.022 - 0.008 * (k % 2)          # the two flats of an X lap, one behind the other, bolted where they cross
        bar(b, F.P(a[0], a[1], wx), F.P(c[0], c[1], wx), 0.006, steel, h=0.035)
    for uc in ((0.06 + W / 2 - 0.04) / 2, (W / 2 + 0.04 + W - 0.06) / 2):
        cyl(b, F.P(uc, (ya + yz) / 2, D - 0.034), F.P(uc, (ya + yz) / 2, D - 0.012), 0.008, ls("galv"), sides=6)
    # rails under each row (steel, insulating covers), restraint rails at the cells' mid-height
    for cy, cw in ((R["front_y"], R["front_w"]), (R["back_y"], R["back_w"])):
        for wr in (cw + 0.03, cw + C["D"] - 0.07):
            F.box(0.012, cy - 0.07, wr, W - 0.012, cy - 0.01, wr + 0.04, steel)
            F.box(0.012, cy - 0.01, wr - 0.004, W - 0.012, cy, wr + 0.044, rail)
    for cy, wr in ((R["front_y"], 0.006), (R["back_y"], 0.355), (R["back_y"], D - 0.035)):
        F.box(0.02, cy + 0.18, wr, W - 0.02, cy + 0.21, wr + 0.028, rail)
    F.tag(W / 2, R["front_y"] + 0.195, 0.006, name, 0.5, 0.024)
    # the cells
    pitch = C["W"] + C["gap"]
    ucell = (lambda i: W - R["end"] - C["W"] - i * pitch) if rtl else (lambda i: R["end"] + i * pitch)
    n = R["n"]
    front = [battery_cell(F, ucell(i), R["front_y"], R["front_w"], first + i, flip=rtl) for i in range(n)]
    back = [battery_cell(F, ucell(n - 1 - i), R["back_y"], R["back_w"], first + n + i, flip=not rtl) for i in range(n)]
    yt_f = R["front_y"] + C["H"] + C["cover"] + 0.03
    yt_b = R["back_y"] + C["H"] + C["cover"] + 0.03
    for row, yt in ((front, yt_f), (back, yt_b)):
        for a, c in zip(row, row[1:]):
            intercell(F, a["-"], c["+"], yt)
    # step jumpers: the front row's last cell (-) up to the back row's first (+): lugs on the posts, insulated cables
    # dressed out past the cell end, back along the slot between the jars and the end frame, up and over to the back row
    sgn = -1 if rtl else 1
    u_end = ucell(n - 1) + (C["W"] + 0.017 if not rtl else -0.017)
    # the front pair of posts goes to the back pair and the back pair to the front, the first cable low and the
    # second stacked above it, so the two never cross on the way (Justin: wires crossing over look terrible)
    for k, ((ua, wa), (ub, wb)) in enumerate(zip(front[-1]["-"], back[0]["+"][::-1])):
        for (u, w, yt) in ((ua, wa, yt_f), (ub, wb, yt_b)):
            F.box(u - 0.022, yt, w - 0.02, u + 0.022, yt + 0.012, w + 0.02, ls("lead"))
            cyl(b, F.P(u, yt + 0.012, w), F.P(u, yt + 0.03, w), 0.012, ls("stainless"), sides=6)
        yr = yt_f + (0.06 if k == 0 else 0.105)
        path(b, [F.P(ua + sgn * 0.016, yt_f + 0.024, wa), F.P(ua + sgn * 0.016, yr, wa), F.P(u_end, yr, wa), F.P(u_end, yr, wb),
                 F.P(u_end, yt_b + 0.06, wb), F.P(ub + sgn * 0.016, yt_b + 0.06, wb), F.P(ub + sgn * 0.016, yt_b + 0.024, wb)], 0.011, BLACK)
    # terminal plates with their insulating covers at the two series ends
    ends = {}
    for pol, cell, yt in (("+", front[0]["+"], yt_f), ("-", back[-1]["-"], yt_b)):
        us, (w_a, w_b) = cell[0][0], (cell[0][1], cell[1][1])
        F.box(us - 0.03, yt, w_a - 0.03, us + 0.03, yt + 0.012, w_b + 0.03, ls("copper_wire"))
        F.box(us - 0.04, yt + 0.012, w_a - 0.04, us + 0.04, yt + 0.05, w_b + 0.04, TY.RED if pol == "+" else BLACK)
        ends[pol] = F.P(us, yt + 0.05, (w_a + w_b) / 2)
    return ends


def battery_monitor_img():
    v = BATT_LIVE
    return lcd_img([f"STRING  {v['volts']:.1f} V   {v['mode']}", f"CURRENT  +{v['amps']:.1f} A  CHARGING",
                    f"PILOT CELL {v['pilot']}  {v['pilot_v']:.2f} V  {v['pilot_t']} C", "CELLS 60/60 OK   GND FAULT: NONE"],
                   w_px=440, h_px=170)


def battery_room_racks(b):
    """The 125 VDC station battery (real basis: 60 flooded lead-calcium cells of about 400 Ah, floating at 2.21 V a
    cell = 132.6 V, sized for the yard's DC load for eight hours with every breaker tripping at the end): two seismic
    two-step racks, B along the front wall (cells 1-30) and A against the relay-room partition (cells 31-60), both
    series ends at the partition end; the main cables and the inter-rack cable up into a wall tray, the fused battery
    disconnect, its conduit through the partition to a pull box (the charger and DC panel come next); the battery
    monitor with its live LCD; the acid PPE board by the eyewash."""
    T = RH
    bx0, bx1, bz0, bz1 = T["batt"]
    t = T["wall"]
    R = RACK
    W = R["W"]
    endsB = battery_rack(b, bx1 - 0.2 - W, t + 0.12 + R["depth"], 1, 1, "RACK B   CELLS 1-30")
    endsA = battery_rack(b, bx0 + t + 0.68, bz1 - 0.12 - R["depth"], -1, 31, "RACK A   CELLS 31-60", rtl=True)
    # the wall tray along the partition, high, on brackets
    xt0, xt1, yt = bx1 - 0.22, bx1 - 0.02, 2.45
    z0t, z1t = 0.3, 5.05
    b.box(xt0, yt, z0t, xt1, yt + 0.01, z1t, ls("galv"))
    for xs in (xt0, xt1 - 0.01):
        b.box(xs, yt + 0.01, z0t, xs + 0.01, yt + 0.1, z1t, ls("galv"))
    for z in np.arange(z0t + 0.25, z1t, 1.15):
        b.box(xt0 + 0.02, yt - 0.12, z - 0.02, bx1, yt, z + 0.02, ls("galv"))
    yc = yt + 0.026
    zd = 2.55
    dz0, dz1, dy0, dy1 = zd - 0.27, zd + 0.27, 1.25, 1.95
    def rise(p, x):
        p = np.array(p, float)
        return [p, p + [0, 0.12, 0], (x, p[1] + 0.12, p[2]), (x, yc, p[2])]
    # battery positive (rack B cell 1) and negative (rack A cell 60) down into the disconnect's top
    # in the tray, wall side outward: battery +, the inter-rack cable, battery -, the monitor's sense lead; each
    # cable rises at the place that keeps it clear of the others (none crosses another on the way up)
    for p, x, col in ((endsB["+"], bx1 - 0.06, TY.RED), (endsA["-"], bx1 - 0.15, BLACK)):
        path(b, rise(p, x) + [(x, yc, zd), (x, dy1, zd)], 0.014, col)
    # inter-rack: rack B cell 30 (-) to rack A cell 31 (+)
    xi = bx1 - 0.1
    pa, pz = np.array(endsB["-"], float), np.array(endsA["+"], float)
    path(b, rise(pa, xi) + [(xi, yc, pz[2]), (xi, pz[1] + 0.12, pz[2]), pz + [0, 0.12, 0], pz], 0.014, BLACK)
    # the fused battery disconnect on the partition, its handle, the conduit through to the DC panel (machine 6)
    b.box(bx1 - 0.22, dy0, dz0, bx1, dy1, dz1, sw("mgrey"))
    b.box(bx1 - 0.225, dy0 + 0.03, dz0 + 0.03, bx1 - 0.22, dy1 - 0.03, dz1 - 0.03, sw("mgrey"))       # door
    b.box(bx1 - 0.2, dy0 + 0.25, dz1, bx1 - 0.08, dy0 + 0.33, dz1 + 0.05, sw("dgrey"))                 # handle hub
    bar(b, (bx1 - 0.14, dy0 + 0.29, dz1 + 0.05), (bx1 - 0.14, dy0 + 0.47, dz1 + 0.05), 0.03, TY.RED, h=0.02)
    tag(b, (bx1 - 0.225, dy1 - 0.1, zd), "-x", "BATTERY MAIN DISC  125 VDC  400 A FUSED", 0.5, 0.04)
    cyl(b, (bx1, 1.6, zd), (bx1 + 0.15, 1.6, zd), 0.035, ls("galv"), sides=10)       # through to the DC panel's back
    # the battery monitor: LCD on its face, its sense lead up into the tray and over to the disconnect
    mz0, mz1, my0, my1 = 1.05, 1.45, 1.3, 1.66
    b.box(bx1 - 0.12, my0, mz0, bx1, my1, mz1, sw("dgrey"))
    face_quad(b, (bx1 - 0.123, (my0 + my1) / 2 + 0.02, (mz0 + mz1) / 2), "-x", 0.3, 0.12,
              ATLAS.mat(ATLAS.add("lcd:battmon", battery_monitor_img())), lst=b.lit)
    tag(b, (bx1 - 0.123, my0 + 0.05, (mz0 + mz1) / 2), "-x", "BATTERY MONITOR", 0.26, 0.03)
    xs = bx1 - 0.18
    path(b, [((bx1 - 0.06), my1, mz1 - 0.06), ((bx1 - 0.06), my1 + 0.1, mz1 - 0.06), (xs, my1 + 0.1, mz1 - 0.06),
             (xs, yc, mz1 - 0.06), (xs, yc, zd), (xs, dy1, zd)], 0.006, BLACK)
    # vapour-tight LED fittings over the aisle (gasketed body, lit diffuser)
    for xl in (1.8, 3.4):
        b.box(xl - 0.6, T["H"] - 0.09, 2.97, xl + 0.6, T["H"], 3.13, sw("white"))
        b.box(xl - 0.56, T["H"] - 0.095, 2.99, xl + 0.56, T["H"] - 0.09, 3.11, CB.LENS["white"], b.lit)
    # acid PPE board by the eyewash: apron, face shield, gloves on hooks
    pz0, pz1 = 2.45, 3.15
    b.box(t, 1.05, pz0, t + 0.02, 1.95, pz1, sw("white"))
    tag(b, (t + 0.02, 1.88, (pz0 + pz1) / 2), "x", "BATTERY PPE", 0.3, 0.04)
    b.box(t + 0.03, 1.0, pz0 + 0.06, t + 0.04, 1.78, pz0 + 0.38, ls("rubber_black"))                  # apron
    b.box(t + 0.02, 1.78, pz0 + 0.2, t + 0.05, 1.8, pz0 + 0.24, ls("stainless"))                       # its hook
    b.box(t + 0.03, 1.45, pz0 + 0.46, t + 0.06, 1.7, pz0 + 0.66, ls("jar_clear"))                      # face shield
    b.box(t + 0.02, 1.7, pz0 + 0.47, t + 0.07, 1.74, pz0 + 0.65, sw("dgrey"))                          # its headband
    for zg in (pz0 + 0.47, pz0 + 0.58):
        b.box(t + 0.03, 1.12, zg, t + 0.05, 1.36, zg + 0.08, ls("rubber_black"))                       # gloves
    return endsA, endsB


# ================================================================ machine 5: the battery chargers and their AC feed

CHG = dict(W=0.8, D=0.55, H=1.78, x=(6.15, 5.3), zf=0.77)
FACES_RH = {}
CHG_LIVE = [dict(v=132.6, a=6.4, mode="FLOAT", ac=True), dict(v=132.6, a=6.4, mode="FLOAT", ac=True)]


def charger_lcd_img(n, live):
    return lcd_img([f"CHARGER {n}   {live['mode']}", f"OUT  {live['v']:.1f} V   {live['a']:.1f} A",
                    "SET  FLOAT 132.6  EQ 139.2", "LOAD SHARE  ON   ALARMS 0"], w_px=360, h_px=150)


def battery_charger(b, n, x0, zf, live):
    """A 125 VDC, 50 A SCR battery charger (real basis: a floor-standing utility charger, one of a redundant pair
    sharing the DC load): steel cabinet on a kick base with side louvres and lifting eyes; on the hinged door the DC
    voltmeter and ammeter (live), the controller's LCD, the alarm and status lamps with their tags (AC ON, FLOAT,
    EQUALIZE, CHARGER FAIL, HIGH VDC, LOW VDC, GROUND FAULT), the FLOAT / EQUALIZE selector and the equalize timer,
    the AC input and DC output breaker handles through the door, the latch; conduit hubs on the top."""
    W, D, H = CHG["W"], CHG["D"], CHG["H"]
    FACES_RH["chg_v"] = ATLAS.add("dial:chg_v", TY.dial_img(0, 150, "DC VOLTS", "V", r_m=0.07, green=(129, 140)))
    FACES_RH["chg_a"] = ATLAS.add("dial:chg_a", TY.dial_img(0, 60, "DC AMPERES", "A", 50, r_m=0.07))
    F = PanelFrame(b, x0, zf, 1, W=W)
    grey = sw("enamel")
    F.box(0.03, 0.0, 0.03, W - 0.03, 0.08, D - 0.03, BLACK)                                  # kick base
    F.box(0.0, 0.08, 0.0, W, H, D, grey)                                                      # the cabinet
    F.box(0.0, H, 0.0, W, H + 0.02, D, sw("mgrey"))                                           # top cap
    for u in (0.12, W - 0.12):                                                                # lifting eyes
        cyl(b, F.P(u, H + 0.02, D / 2 - 0.02), F.P(u, H + 0.06, D / 2 - 0.02), 0.022, ls("galv"), sides=8)
    for side, nrm in ((0.0, F.P(-1, 0, 0) - F.P(0, 0, 0)), (W, F.P(1, 0, 0) - F.P(0, 0, 0))):     # side louvres
        nv = "x" if nrm[0] > 0 else "-x"
        for yl in (0.35, 1.45):
            face_quad(b, F.P(side, yl, D / 2) + nrm * 0.003, nv, 0.36, 0.22, "louvre")
    # the door, proud of the front, its hinges on the left, the latch on the right
    d0 = -0.014
    F.box(0.025, 0.1, d0, W - 0.025, H - 0.03, 0.0, grey)
    for y in (0.3, H - 0.3):
        cyl(b, F.P(0.022, y - 0.05, d0 + 0.006), F.P(0.022, y + 0.05, d0 + 0.006), 0.009, ls("stainless"), sides=8)
    F.cyl(W - 0.07, 1.0, d0, d0 - 0.02, 0.022, ls("stainless"), sides=10)
    bar(b, F.P(W - 0.07, 1.0, d0 - 0.025), F.P(W - 0.07, 0.9, d0 - 0.025), 0.016, ls("stainless"), h=0.012)
    fz = d0 - 0.0005                                                                          # the door's face (w)
    nn = F.n
    F.tag(W / 2, H - 0.09, fz, f"BATTERY CHARGER {n}   125 VDC  50 A", 0.56, 0.04)
    # meters
    for k, (u, key, val, lo, hi) in enumerate(((0.22, "chg_v", live["v"], 0, 150), (0.58, "chg_a", live["a"], 0, 60))):
        gauge(b, F.P(u, 1.47, fz), nn, 0.07, FACES_RH[key], val, lo, hi, channel=f"chg{n}_{'v' if k == 0 else 'a'}")
    # controller LCD
    F.box(0.2, 1.24, fz - 0.006, 0.6, 1.36, fz, BLACK)
    F.quad(0.4, 1.3, fz - 0.008, 0.36, 0.1, ATLAS.mat(ATLAS.add(f"lcd:chg{n}", charger_lcd_img(n, live))), lst=b.lit)
    # lamps and their tags
    lamps = (("AC ON", "white", live["ac"]), ("FLOAT", "green", live["mode"] == "FLOAT"), ("EQUALIZE", "amber", live["mode"] == "EQUALIZE"),
             ("CHGR FAIL", "red", False), ("HIGH VDC", "red", False), ("LOW VDC", "red", False), ("GND FAULT", "amber", False))
    for k, (lab, col, on) in enumerate(lamps):
        u = 0.1 + k * 0.1
        lamp(b, F.P(u, 1.12, fz), nn, col, on, r=0.016)
        F.tag(u, 1.075, fz, lab, 0.085, 0.016)
    # selector and equalize timer
    selector(b, F.P(0.25, 0.92, fz), nn, 135 if live["mode"] == "FLOAT" else 45, r=0.028)
    F.tag(0.25, 0.85, fz, "FLOAT / EQUALIZE", 0.17, 0.02)
    cyl(b, F.P(0.55, 0.92, fz), F.P(0.55, 0.92, fz - 0.02), 0.035, BLACK, sides=14)
    bar(b, F.P(0.55, 0.92, fz - 0.022), F.P(0.55, 0.95, fz - 0.022), 0.008, sw("white"), h=0.002)
    F.tag(0.55, 0.85, fz, "EQ TIMER  0-72 H", 0.17, 0.02)
    # breaker handles through the door: escutcheon, the toggle up (on)
    for u, lab in ((0.25, "AC INPUT"), (0.55, "DC OUTPUT")):
        F.box(u - 0.05, 0.62, fz - 0.003, u + 0.05, 0.76, fz, sw("mgrey"))                       # trim plate
        F.box(u - 0.032, 0.64, fz - 0.012, u + 0.032, 0.74, fz - 0.003, BLACK)                   # the breaker's face
        bar(b, F.P(u, 0.69, fz - 0.012), F.P(u, 0.725, fz - 0.045), 0.02, sw("dgrey"), h=0.026)  # handle up: ON
        F.box(u - 0.01, 0.725, fz - 0.0135, u + 0.01, 0.732, fz - 0.012, sw("white"))            # its ON mark
        F.tag(u, 0.585, fz, lab, 0.12, 0.022)
    face_quad(b, F.P(W / 2, 0.3, fz - 0.0005), nn, 0.6, 0.25, "louvre")                     # door louvre, low
    return F


def charger_corner(b):
    """Machine 5 placed: the two chargers against the front wall between the partition and the front door, load
    sharing; their DC outputs in conduit up the wall, along it and down into the battery pull box; their AC feeds
    from the station AC panel (ACDP-1, wall-mounted past the door), which is fed in conduit from the transfer switch
    on the end wall. Conduits on wall straps and strut brackets, AC on the wall, DC on the strut's outer layer."""
    T = RH
    t, L = T["wall"], T["L"]
    bx1 = T["batt"][1]
    W, D, H = CHG["W"], CHG["D"], CHG["H"]
    for k, x0 in enumerate(CHG["x"]):
        battery_charger(b, k + 1, x0, CHG["zf"], CHG_LIVE[k])
    top = H + 0.02
    zA, zD = t + 0.032, t + 0.2           # AC conduits strapped to the wall, DC conduits on the strut's outer layer
    r = 0.02
    pull = bx1 + 0.15                    # the pull box's wall face (relay-room side of the partition)
    # DC outputs: CH-2 (x 5.3-6.1) low run, CH-1 above it, down into the DC panel's top
    path(b, [(5.45, top, zD), (5.45, 2.9, zD), (pull + 0.07, 2.9, zD), (pull + 0.07, 2.9, 2.5), (pull + 0.07, DCDP["y1"], 2.5)], r, ls("galv"))
    path(b, [(6.3, top, zD), (6.3, 3.05, zD), (pull + 0.13, 3.05, zD), (pull + 0.13, 3.05, 2.62), (pull + 0.13, DCDP["y1"], 2.62)], r, ls("galv"))
    # the AC panel, wall-mounted past the door and the extinguisher
    ax0, ax1, ay0, ay1 = 8.7, 9.3, 0.9, 2.1
    b.box(ax0, ay0, t, ax1, ay1, t + 0.18, sw("mgrey"))
    b.box(ax0 + 0.03, ay0 + 0.03, t + 0.18, ax1 - 0.03, ay1 - 0.03, t + 0.185, sw("mgrey"))
    cyl(b, (ax1 - 0.08, 1.5, t + 0.185), (ax1 - 0.08, 1.5, t + 0.205), 0.016, ls("stainless"), sides=8)
    tag(b, ((ax0 + ax1) / 2, ay1 - 0.1, t + 0.185), "z", "ACDP-1  120/240 VAC", 0.32, 0.04)
    tag(b, ((ax0 + ax1) / 2, ay1 - 0.17, t + 0.185), "z", "STATION SERVICE", 0.24, 0.03)
    # its feed from the transfer switch: in through the end wall at an LB body, up the end wall, along the front wall
    # high, down into the panel's top
    xe = L - t - 0.03
    b.box(L - t - 0.075, 1.4, 2.3, L - t, 1.6, 2.4, ls("galv"))                                       # LB body
    b.box(L - t - 0.08, 1.42, 2.31, L - t - 0.075, 1.58, 2.39, ls("galv_dark"))                       # its cover
    path(b, [(xe, 1.6, 2.35), (xe, 3.6, 2.35), (xe, 3.6, zA), (9.15, 3.6, zA), (9.15, ay1, zA)], 0.026, ls("galv"))
    # charger AC feeds: CH-1 (x 6.15-6.95) low, CH-2 above it
    path(b, [(8.8, ay1, zA), (8.8, 3.0, zA), (6.85, 3.0, zA), (6.85, top, zA)], r, ls("galv"))
    path(b, [(8.95, ay1, zA), (8.95, 3.15, zA), (6.0, 3.15, zA), (6.0, top, zA)], r, ls("galv"))
    # a strut bracket on the wall carrying CH-1's DC run; one-hole straps on the AC runs
    xs = 5.85
    b.box(xs - 0.02, 2.75, t, xs + 0.02, 3.25, t + 0.04, ls("galv"))
    b.box(xs - 0.02, 3.05 - r - 0.03, t + 0.04, xs + 0.02, 3.05 - r, zD + 0.03, ls("galv"))
    def strap(c, run, r_):
        """A one-hole strap clamping a conduit to the wall behind it (the front wall, or the end wall for run "y"/"z"
        at the far end): a band round the conduit, its foot on the wall."""
        x, y, z = c
        if run == "x":
            b.box(x - 0.015, y - r_ - 0.004, t + 0.002, x + 0.015, y + r_ + 0.004, z + r_ * 0.7, ls("galv"))
        elif run == "y":
            b.box(L - t - 0.002 - (L - t - x) - r_ * 0.7, y - 0.015, z - r_ - 0.004, L - t - 0.002, y + 0.015, z + r_ + 0.004, ls("galv"))
        else:
            b.box(L - t - 0.002 - (L - t - x) - r_ * 0.7, y - r_ - 0.004, z - 0.015, L - t - 0.002, y + r_ + 0.004, z + 0.015, ls("galv"))
    for x in (7.3, 8.0):
        for y in (3.0, 3.15):
            strap((x, y, zA), "x", r)
    for x in np.arange(10.0, L - 0.5, 1.5):
        strap((x, 3.6, zA), "x", 0.026)
    for y in (2.3, 3.1):
        strap((xe, y, 2.35), "y", 0.026)
    strap((xe, 3.6, 1.3), "z", 0.026)
    for z in (1.0, 1.8):                                                                              # partition run
        b.box(pull, 2.75, z - 0.02, pull + 0.04, 3.1, z + 0.02, ls("galv"))
        for ya, xe in ((2.9 - r - 0.03, 0.07), (3.05 - r - 0.03, 0.13)):
            b.box(pull + 0.04, ya, z - 0.02, pull + xe + 0.03, ya + 0.03, z + 0.02, ls("galv"))
    return


# ================================================================ machine 6: the DC distribution panel and ground detector

DCDP = dict(z0=2.25, z1=2.85, y0=0.55, y1=2.05, d=0.18)
DC_LOAD_A = 12.4
DC_GND = (66.3, 66.3)            # volts, positive bus to ground and negative bus to ground (balanced: no ground)
DC_CIRCUITS = [  # (number, name, on)
    (1, "LINE 1 PROT A", 1), (2, "LINE 1 PROT B", 1), (3, "LINE 2 PROT A", 1), (4, "LINE 2 PROT B", 1),
    (5, "LINE 3 PROT A", 1), (6, "LINE 3 PROT B", 1), (7, "BUS 1 DIFF", 1), (8, "BUS 2 DIFF", 1),
    (9, "MT-1 BANK PROT", 1), (10, "SAT-1 PROT", 1), (11, "BKR 1-1 1-2 TRIP 1", 1), (12, "BKR 1-1 1-2 TRIP 2", 1),
    (13, "BKR 2-1 2-2 TRIP 1", 1), (14, "BKR 2-1 2-2 TRIP 2", 1), (15, "BKR 3-1 3-2 TRIP 1", 1), (16, "BKR 3-1 3-2 TRIP 2", 1),
    (17, "BKR X-3 TRIP 1", 1), (18, "BKR X-3 TRIP 2", 1), (19, "BKR CLOSE + MOTORS", 1), (20, "SCADA / COMM", 1),
    (21, "FAULT RECORDER", 1), (22, "CONTROL DESK", 1), (23, "EMERGENCY LIGHTS", 1), (24, "SPARE", 0)]


class XFrame:
    """Wall-mounted on the partition's relay-room face, facing +x: u across from the viewer's left (+z), y up, w back
    into the wall (-x)."""

    def __init__(self, b, xf, z0):
        self.b, self.xf, self.z0, self.n = b, xf, z0, "x"

    def P(self, u, y, w):
        return np.array([self.xf - w, y, self.z0 + u])

    def box(self, u0, y0, w0, u1, y1, w1, mat, lst=None):
        a, c = self.P(u0, y0, w0), self.P(u1, y1, w1)
        self.b.box(*_rnd(np.minimum(a, c)), *_rnd(np.maximum(a, c)), mat, lst)

    def tag(self, u, y, w, text, wd=0.3, ht=0.035):
        tag(self.b, self.P(u, y, w), "x", text, wd, ht)


def directory_img(rows, w_px=300, h_px=520):
    """The panel's typed circuit directory (a card in a clear pocket on the door's inside)."""
    im = Image.new("RGBA", (w_px, h_px), (238, 236, 226, 255))
    d = ImageDraw.Draw(im)
    f = lambda sz: TY._font(sz, cond=True)
    d.text((w_px / 2, 10), "DCDP-1  125 VDC", font=f(26), fill=(20, 20, 24), anchor="ma")
    d.text((w_px / 2, 40), "CIRCUIT DIRECTORY", font=f(18), fill=(20, 20, 24), anchor="ma")
    y0, rh = 70, (h_px - 80) / len(rows)
    for i, (nm, name, on) in enumerate(rows):
        y = y0 + i * rh
        d.line([(8, y + rh - 2), (w_px - 8, y + rh - 2)], fill=(170, 170, 160))
        d.text((12, y + 2), f"{nm:>2}", font=f(rh * 0.7), fill=(20, 20, 24))
        d.text((46, y + 2), name, font=f(rh * 0.7), fill=(20, 20, 24))
    return im


def dc_panel(b):
    """DCDP-1, the 125 VDC distribution panelboard (real basis: a wall-mounted DC panelboard fed from the battery and
    both chargers, two-pole branch breakers to every protection, trip and control circuit, split A / B so the two
    protection systems and the two trip coils never share a breaker): steel box on the partition by the battery
    disconnect, its door standing open: the 225 A two-pole main at the top, 24 branch breakers in two columns over
    the bus cover (handles out = ON; the spare OFF), circuit numbers, the typed directory in the door; feeders down
    in conduit into the slab to the panel trenches. Beside it the DC ground detector: positive and negative to ground
    voltmeters (live, balanced 66 V each), POS GND / NEG GND lamps, the NORMAL lamp, a test button."""
    T = RH
    pull = T["batt"][1] + 0.15
    D = DCDP
    W = D["z1"] - D["z0"]
    F = XFrame(b, pull + D["d"], D["z0"])
    d = D["d"]
    grey = sw("mgrey")
    # the box: back, sides, top, bottom (open front), the trim round the opening
    F.box(0, D["y0"], d - 0.01, W, D["y1"], d, grey)
    for u0, u1 in ((0.0, 0.02), (W - 0.02, W)):
        F.box(u0, D["y0"], 0.0, u1, D["y1"], d - 0.01, grey)
    for y0, y1 in ((D["y0"], D["y0"] + 0.02), (D["y1"] - 0.02, D["y1"])):
        F.box(0.02, y0, 0.0, W - 0.02, y1, d - 0.01, grey)
    # the dead-front, recessed, with the bus cover strip down the middle
    df = 0.025
    F.box(0.02, D["y0"] + 0.02, df, W - 0.02, D["y1"] - 0.02, df + 0.005, sw("enamel"))
    F.box(W / 2 - 0.045, 0.75, df - 0.004, W / 2 + 0.045, 1.66, df, sw("dgrey"))
    F.tag(W / 2, 1.97, df - 0.0005, "DCDP-1  125 VDC  225 A", 0.3, 0.032)
    # the main breaker: a bigger two-pole frame, its handle up (ON)
    F.box(W / 2 - 0.1, 1.72, df - 0.04, W / 2 + 0.1, 1.9, df, BLACK)
    bar(b, F.P(W / 2, 1.8, df - 0.04), F.P(W / 2, 1.85, df - 0.085), 0.05, sw("dgrey"), h=0.03)
    F.tag(W / 2, 1.69, df - 0.0005, "MAIN", 0.08, 0.018)
    # branch breakers, two columns of twelve, odd numbers left
    rows = 12
    for i, (nm, name, on) in enumerate(DC_CIRCUITS):
        col, row = (nm - 1) % 2, (nm - 1) // 2
        y = 1.62 - row * 0.072
        uc = W / 2 - 0.105 if col == 0 else W / 2 + 0.105
        F.box(uc - 0.055, y - 0.03, df - 0.022, uc + 0.055, y + 0.03, df, BLACK)
        out = -1 if col == 0 else 1                               # ON: the handle toward the outside edge
        dirn = out if on else -out
        bar(b, F.P(uc, y, df - 0.022), F.P(uc + dirn * 0.03, y, df - 0.045), 0.03, sw("dgrey"), h=0.018)
        F.tag(uc + out * 0.085, y, df - 0.0005, str(nm), 0.032, 0.018)
    # the door standing open on its left hinges, the directory in its pocket
    th = math.radians(105)
    hinge = F.P(0.0, (D["y0"] + D["y1"]) / 2, -0.012)
    dvec = np.array([math.sin(th), 0, math.cos(th)])              # closed it lies along +z (u); open it swings out (+x)
    leaf_c = hinge + dvec * (W / 2)
    bar(b, hinge, hinge + dvec * W, 0.02, grey, h=D["y1"] - D["y0"])
    inner = np.array([-math.cos(th), 0, math.sin(th)])            # the face that was toward the breakers
    face_quad(b, leaf_c + inner * 0.0115, inner, 0.3, 0.52, ATLAS.mat(ATLAS.add("dcdp:dir", directory_img(DC_CIRCUITS))))
    for y in (D["y0"] + 0.2, D["y1"] - 0.2):
        cyl(b, F.P(-0.004, y - 0.05, -0.012), F.P(-0.004, y + 0.05, -0.012), 0.01, ls("stainless"), sides=8)
    # feeders down into the slab (embedded conduits to the panel-row trenches)
    for u in (0.12, 0.22, 0.38, 0.48):
        cyl(b, F.P(u, D["y0"], d / 2), F.P(u, 0.0, d / 2), 0.022, ls("galv"), sides=10)
    # the DC ground detector beside it
    gz0, gz1, gy0, gy1, gd = 2.98, 3.32, 1.2, 1.9, 0.12
    G = XFrame(b, pull + gd, gz0)
    gw = gz1 - gz0
    G.box(0, gy0, 0, gw, gy1, gd, sw("enamel"))
    G.tag(gw / 2, gy1 - 0.05, -0.0005, "DC GROUND DETECTOR", 0.3, 0.032)
    FACES_RH["gnd_p"] = ATLAS.add("dial:gnd_p", TY.dial_img(0, 150, "+ TO GND", "V", r_m=0.05, green=(60, 72)))
    FACES_RH["gnd_n"] = ATLAS.add("dial:gnd_n", TY.dial_img(0, 150, "- TO GND", "V", r_m=0.05, green=(60, 72)))
    for k, (u, key, val) in enumerate(((0.09, "gnd_p", DC_GND[0]), (0.25, "gnd_n", DC_GND[1]))):
        gauge(b, G.P(u, 1.62, -0.0005), "x", 0.05, FACES_RH[key], val, 0, 150, channel=f"dc_gnd_{'p' if k == 0 else 'n'}")
    for u, lab, col, on in ((0.07, "POS GND", "red", False), (0.17, "NORMAL", "green", True), (0.27, "NEG GND", "red", False)):
        lamp(b, G.P(u, 1.43, -0.0005), "x", col, on, r=0.016)
        G.tag(u, 1.385, -0.0005, lab, 0.085, 0.016)
    cyl(b, G.P(gw / 2, 1.28, -0.0005), G.P(gw / 2, 1.28, -0.012), 0.024, ls("stainless"), sides=12)
    cyl(b, G.P(gw / 2, 1.28, -0.012), G.P(gw / 2, 1.28, -0.03), 0.018, sw("black"), sides=12)
    G.tag(gw / 2, 1.235, -0.0005, "TEST", 0.06, 0.016)
    path(b, [G.P(0.03, gy0, gd / 2), G.P(0.03, gy0 - 0.08, gd / 2), F.P(W - 0.06, gy0 - 0.08, d / 2), F.P(W - 0.06, D["y0"] + 0.4, d / 2)], 0.012, ls("galv"))


# ================================================================ machine 7: the digital fault recorder

DFR_LAST = dict(time="12:15:28.412", line="LINE 2", elem="21 ZONE 1", phase="B-G", km=12.4, ib=8.62, rec=147)
CLOCK_NOW = "14:02:37"


def waveform_img(w_px=900, h_px=560):
    """The recorder's screen: the last record (Line 2's B-to-ground fault, tripped in zone 1, reclosed): three phase
    voltages and currents over twelve cycles, the trip / breaker / reclose channels, the record's header."""
    f = DFR_LAST
    im = Image.new("RGBA", (w_px, h_px), (14, 18, 26, 255))
    d = ImageDraw.Draw(im)
    fn = lambda s: TY._font(s, cond=True)
    d.text((12, 8), f"DFR-1  REC {f['rec']:04d}   {f['line']}   {f['time']}", font=fn(24), fill=(226, 230, 236))
    d.text((12, 38), f"{f['elem']}   {f['phase']}   {f['km']} km   IB {f['ib']} kA", font=fn(20), fill=(250, 120, 100))
    x0, x1 = 90, w_px - 14
    cols = {"A": (240, 90, 70), "B": (250, 210, 70), "C": (90, 170, 250)}
    trip_c, open_c = 3.0, 5.5                                   # fault from cycle 3, breaker open at 5.5
    def fault(t):
        return trip_c <= t < open_c
    rows = [("VA", "A", "v"), ("VB", "B", "v"), ("VC", "C", "v"), ("IA", "A", "i"), ("IB", "B", "i"), ("IC", "C", "i")]
    top, rh = 74, 58
    for k, (lab, ph, kind) in enumerate(rows):
        yc = top + k * rh + rh / 2
        d.line([(x0, yc), (x1, yc)], fill=(40, 46, 58))
        d.text((12, yc), lab, font=fn(18), fill=cols[ph], anchor="lm")
        pts = []
        sh = {"A": 0, "B": -2.094, "C": 2.094}[ph]
        for i in range(600):
            t = 12 * i / 599
            a = 0.8
            dead = open_c <= t < 9.5                                 # both breakers open: the line (and its CVTs) dead
            if kind == "v":
                a = 0.8 if not fault(t) else (0.35 if ph == "B" else 0.86)
                a = 0.8 * math.exp(-(t - open_c) * 6) if dead else a
            else:
                a = 0.25 if t < trip_c else (0.95 if (ph == "B" and fault(t)) else (0.3 if fault(t) else (0.0 if t < 9.5 else 0.25)))
            pts.append((x0 + (x1 - x0) * t / 12, yc - a * rh * 0.45 * math.sin(2 * math.pi * t + sh)))
        d.line(pts, fill=cols[ph], width=2)
    ytop = top + 6 * rh + 8
    for k, (lab, on0, on1) in enumerate((("21 Z1 TRIP", trip_c + 0.9, open_c), ("52a", -1, open_c), ("79 CLOSE", 9.3, 9.6))):
        y = ytop + k * 26
        d.text((12, y + 10), lab, font=fn(16), fill=(200, 206, 214), anchor="lm")
        if lab.startswith("52a"):
            segs = [(0, open_c), (9.5, 12)]
        else:
            segs = [(on0, on1)]
        d.line([(x0, y + 18), (x1, y + 18)], fill=(60, 66, 78))
        for a_, c_ in segs:
            d.rectangle([x0 + (x1 - x0) * a_ / 12, y + 4, x0 + (x1 - x0) * c_ / 12, y + 18], fill=(120, 220, 120))
    for c in range(13):
        x = x0 + (x1 - x0) * c / 12
        d.line([(x, top), (x, top + 6 * rh)], fill=(32, 38, 48))
    d.text((x1, h_px - 8), "CYCLES 0-12  (60 Hz)", font=fn(14), fill=(150, 156, 166), anchor="rd")
    return im


def digits_img(text, w_px=300, h_px=70):
    """Red seven-segment style digits behind a dark filter (the satellite clock's time)."""
    im = Image.new("RGBA", (w_px, h_px), (24, 6, 6, 255))
    d = ImageDraw.Draw(im)
    d.text((w_px / 2, h_px / 2), text, font=TY._font(h_px * 0.8, cond=True), fill=(255, 52, 40), anchor="mm")
    return im


def dfr_panel(b, x0, zf, facing):
    """DFR-1, the digital fault recorder (real basis: a substation DFR in a 19-inch swing-rack panel, recording every
    line's and bus's voltages and currents and the protection's trip, breaker and reclose contacts on any fault,
    time-stamped by a GPS satellite clock): the GPS clock with its live red time display and SYNC lamp; a rack monitor
    showing the last record's waveforms (live); the keyboard drawer; the rack PC; the 4U acquisition chassis with its
    status LEDs and LCD; blanking plates; DC breakers, terminals, the gland plate; the GPS antenna on the roof."""
    W, D, H, pl = PANEL["W"], PANEL["D"], PANEL["H"], PANEL["plinth"]
    F = PanelFrame(b, x0, zf, facing)
    panel_shell(F, facing, "DFR-1  DIGITAL FAULT RECORDER")
    u0, u1 = 0.085, W - 0.085
    def unit(y0, y1, mat=BLACK):
        F.box(u0, y0, -0.012, u1, y1, 0.32, ls("galv_dark"))
        F.box(u0 - 0.008, y0, -0.018, u1 + 0.008, y1, -0.012, mat)
        for uu in (u0 - 0.003, u1 + 0.003):
            F.cyl(uu, (y0 + y1) / 2, -0.018, -0.024, 0.005, ls("stainless"), sides=6)
    # GPS clock (1U)
    y = 1.93
    unit(y, y + 0.044)
    F.box(0.32, y + 0.006, -0.024, 0.52, y + 0.038, -0.018, BLACK)
    F.quad(0.42, y + 0.022, -0.026, 0.19, 0.03, ATLAS.mat(ATLAS.add("clock:now", digits_img(CLOCK_NOW))), lst=b.lit)
    F.box(0.6, y + 0.018, -0.022, 0.608, y + 0.026, -0.018, CB.LENS["green"], b.lit)
    F.tag(0.2, y + 0.022, -0.0185, "GPS CLOCK", 0.12, 0.018)
    # rack monitor (8U): bezel, the screen lit
    y = 1.5
    unit(y, y + 0.4)
    F.box(0.13, y + 0.035, -0.026, W - 0.13, y + 0.38, -0.018, BLACK)
    F.quad(W / 2, y + 0.205, -0.0265, W - 0.3, 0.315, ATLAS.mat(ATLAS.add("dfr:wave", waveform_img())), lst=b.lit)
    F.box(W - 0.14, y + 0.012, -0.022, W - 0.132, y + 0.02, -0.018, CB.LENS["green"], b.lit)
    # keyboard drawer (1U), its handle
    y = 1.44
    unit(y, y + 0.044, sw("dgrey"))
    F.box(W / 2 - 0.08, y + 0.016, -0.03, W / 2 + 0.08, y + 0.028, -0.018, ls("stainless"))
    # rack PC (2U): drive bay, power button and LED
    y = 1.33
    unit(y, y + 0.088)
    F.box(0.15, y + 0.03, -0.022, 0.32, y + 0.058, -0.018, sw("dgrey"))
    F.cyl(0.62, y + 0.044, -0.018, -0.026, 0.01, ls("stainless"), sides=10)
    F.box(0.58, y + 0.04, -0.022, 0.588, y + 0.048, -0.018, CB.LENS["green"], b.lit)
    F.tag(0.45, y + 0.044, -0.0185, "DFR PC", 0.09, 0.018)
    # acquisition chassis (4U): status LEDs and labels, its LCD
    y = 1.08
    unit(y, y + 0.177)
    leds = (("POWER", "green", True), ("RUN", "green", True), ("GPS SYNC", "green", True), ("TRIGGERED", "amber", True), ("DISK", "green", False), ("FAULT", "red", False))
    for k, (lab, col, on) in enumerate(leds):
        ly = y + 0.155 - k * 0.026
        lens = CB.LENS[col] if on else CB.LENS["off_" + col]
        F.box(0.13, ly - 0.004, -0.024, 0.138, ly + 0.004, -0.018, lens, b.lit if on else None)
        F.tag(0.19, ly, -0.0185, lab, 0.09, 0.016)
    F.box(0.34, y + 0.06, -0.026, 0.66, y + 0.15, -0.018, BLACK)
    F.quad(0.5, y + 0.105, -0.0265, 0.3, 0.08, ATLAS.mat(ATLAS.add("lcd:dfr", lcd_img(
        [f"RECORDS {DFR_LAST['rec']:04d}  DISK 41%", f"LAST {DFR_LAST['time'][:8]} L2 B-G", "64 ANALOG  128 DIGITAL", "GPS LOCKED  10 SATS"]))), lst=b.lit)
    F.tag(0.5, y + 0.03, -0.0185, "DFR-1  ACQUISITION  64A / 128D", 0.3, 0.02)
    # blanking plates
    for y0, y1 in ((0.88, 1.06), (1.98, 2.0)):
        F.box(u0 - 0.008, y0, -0.016, u1 + 0.008, y1, -0.012, sw("enamel"))
    # DC breakers, terminals, the gland plate and its cables
    mcb_row(F, 0.1, 0.78, ["DFR", "PC", "CLOCK", "MON"])
    terminal_strip(F, 0.1, W - 0.1, pl + 0.32)
    F.box(0.1, pl, 0.05, W - 0.1, pl + 0.012, D - 0.05, ls("galv"))
    for k in range(6):
        uu = 0.16 + k * 0.09
        cyl(b, F.P(uu, pl + 0.012, 0.3), F.P(uu, pl + 0.11, 0.3), 0.016, BLACK, sides=8)
        cyl(b, F.P(uu, pl + 0.012, 0.3), F.P(uu, pl + 0.03, 0.3), 0.024, sw("dgrey"), sides=8)
    F.box(0.2, H - 0.29, 0.05, W - 0.2, H - 0.25, 0.1, sw("white"))
    F.box(0.22, H - 0.295, 0.055, W - 0.22, H - 0.29, 0.095, CB.LENS["white"], b.lit)
    return F


def relay_room_dfr(b):
    """Machine 7 placed: DFR-1 as the sixth panel of row 2; its GPS antenna's coax up out of the panel top into the
    row's ladder tray and up through the roof to the antenna on its mast."""
    T = RH
    x0 = 7.0 + 5 * (PANEL["W"] + 0.002)
    F = dfr_panel(b, x0, 9.0, -1)
    H = T["H"]
    xa, za = x0 + 0.62, 9.45
    path(b, [F.P(0.62, PANEL["H"], 0.45), (xa, 3.26, za)], 0.007, BLACK)
    path(b, [(xa, 3.36, za), (xa, H, za)], 0.007, BLACK)
    b.box(xa - 0.06, H + 0.32, za - 0.06, xa + 0.06, H + 0.38, za + 0.06, ls("rubber_black"))        # roof boot
    cyl(b, (xa, H + 0.38, za), (xa, H + 0.85, za), 0.02, ls("galv"), sides=8)                       # mast
    cyl(b, (xa, H + 0.85, za), (xa, H + 0.88, za), 0.05, sw("white"), sides=12)
    cyl(b, (xa, H + 0.88, za), (xa, H + 0.94, za), 0.042, sw("white"), sides=12)                  # the dome
    for a in (0, 120, 240):
        t = math.radians(a)
        bar(b, (xa, H + 0.6, za), (xa + math.cos(t) * 0.25, H + 0.32, za + math.sin(t) * 0.25), 0.012, ls("galv"), h=0.012)



# ================================================================ machine 8: the SCADA / communications room

COMM_LIVE = dict(v48=-48.2, a48=6.1, points=1842, poll=0.4)


def rack_unit(F, y0, y1, mat=BLACK, u0=0.085, u1=None):
    """A 19-inch rack-mounted unit: its case back into the rack, the front panel, the rack screws."""
    u1 = PANEL["W"] - 0.085 if u1 is None else u1
    F.box(u0, y0 + 0.002, -0.012, u1, y1 - 0.002, 0.32, ls("galv_dark"))
    F.box(u0 - 0.008, y0 + 0.002, -0.018, u1 + 0.008, y1 - 0.002, -0.012, mat)
    for uu in (u0 - 0.003, u1 + 0.003):
        F.cyl(uu, (y0 + y1) / 2, -0.018, -0.024, 0.005, ls("stainless"), sides=6)


def led(F, u, y, colour, on, s=0.004, w=-0.018):
    lens = CB.LENS[colour] if on else CB.LENS["off_" + colour]
    F.box(u - s, y - s, w - 0.005, u + s, y + s, w, lens, F.b.lit if on else None)


def jumper_loop(F, ua, ya, ub, yb, mat, sag=0.08, out=0.05, r=0.003):
    """A patch cord between two front ports, hanging in a loop in front of the rack."""
    path(F.b, [F.P(ua, ya, -0.03), F.P(ua, ya - 0.02, -0.03 - out * 0.5), F.P((ua + ub) / 2, min(ya, yb) - sag, -0.03 - out),
               F.P(ub, yb - 0.02, -0.03 - out * 0.5), F.P(ub, yb, -0.03)], r, mat)


def ports(F, u0, y, n, pitch=0.02, s=0.006, colour="green", lit_every=1):
    """A row of ports (RJ45 / LC): dark sockets with a link LED over each (lit)."""
    out = []
    for i in range(n):
        u = u0 + i * pitch
        F.box(u - s, y - s, -0.02, u + s, y + s, -0.018, sw("dgrey"))
        if lit_every and i % lit_every == 0:
            led(F, u, y + s + 0.005, colour, True, s=0.002)
        out.append(u)
    return out


def panel_base(F, names):
    """The bottom of a swing-rack cabinet: DC breakers, terminals and duct, the gland plate with cables from below."""
    W, D, pl = PANEL["W"], PANEL["D"], PANEL["plinth"]
    mcb_row(F, 0.1, 0.78, names)
    terminal_strip(F, 0.1, W - 0.1, pl + 0.32)
    F.box(0.1, pl, 0.05, W - 0.1, pl + 0.012, D - 0.05, ls("galv"))
    for k in range(4):
        uu = 0.2 + k * 0.12
        cyl(F.b, F.P(uu, pl + 0.012, 0.3), F.P(uu, pl + 0.11, 0.3), 0.016, BLACK, sides=8)
        cyl(F.b, F.P(uu, pl + 0.012, 0.3), F.P(uu, pl + 0.03, 0.3), 0.024, sw("dgrey"), sides=8)
    H = PANEL["H"]
    F.box(0.2, H - 0.29, 0.05, W - 0.2, H - 0.25, 0.1, sw("white"))
    F.box(0.22, H - 0.295, 0.055, W - 0.22, H - 0.29, 0.095, CB.LENS["white"], F.b.lit)


def rtu_cabinet(b, x0, zf, facing):
    """RTU-1, the SCADA remote terminal unit / substation gateway (real basis: the station's RTU talking DNP3 to the
    utility's control centre and to the plant): the gateway computer with its LCD (live), the RTU card cage (CPU,
    comm and I/O cards with their LEDs and handles), the interposing relays for the breaker controls on a DIN rail."""
    W = PANEL["W"]
    F = PanelFrame(b, x0, zf, facing)
    panel_shell(F, facing, "RTU-1  SCADA REMOTE TERMINAL UNIT")
    v = COMM_LIVE
    rack_unit(F, 1.9, 1.99)
    F.box(0.2, 1.915, -0.026, 0.5, 1.975, -0.018, BLACK)
    F.quad(0.35, 1.945, -0.027, 0.28, 0.05, ATLAS.mat(ATLAS.add("lcd:rtu", lcd_img(
        ["RTU-1  ONLINE   DNP3 MASTER UP", f"POINTS {v['points']}   POLL {v['poll']} S"], w_px=420, h_px=80))), lst=b.lit)
    led(F, 0.58, 1.945, "green", True); led(F, 0.61, 1.945, "green", True); led(F, 0.64, 1.945, "amber", False)
    F.tag(0.66, 1.92, -0.0185, "GATEWAY", 0.09, 0.016)
    # the card cage
    y0, y1 = 1.32, 1.86
    rack_unit(F, y0, y1, sw("dgrey"))
    names = ["PWR", "PWR", "CPU", "COM", "COM", "DI", "DI", "DI", "DI", "DO", "DO", "AI"]
    for i, nm in enumerate(names):
        u = 0.11 + i * 0.049
        F.box(u, y0 + 0.02, -0.028, u + 0.043, y1 - 0.02, -0.018, sw("mgrey"))
        for k in range(4 if nm in ("DI", "DO") else 3):
            on = nm in ("PWR", "CPU", "COM") or (k % 2 == 0)
            led(F, u + 0.0215, y1 - 0.06 - k * 0.04, "green" if nm != "DO" else "amber", on, w=-0.028)
        F.box(u + 0.008, y0 + 0.05, -0.045, u + 0.035, y0 + 0.065, -0.028, BLACK)                # extractor handle
        if nm == "COM":
            for k in range(2):
                F.box(u + 0.012, y1 - 0.25 - k * 0.05, -0.032, u + 0.031, y1 - 0.23 - k * 0.05, -0.028, sw("blue"))
        F.tag(u + 0.0215, y0 + 0.1, -0.0285, nm, 0.036, 0.014)
    # interposing relays on a DIN rail: white bases, the relays, an LED on each energised one
    F.box(0.1, 1.0, -0.012, W - 0.1, 1.035, -0.004, ls("galv"))
    for i in range(14):
        u = 0.13 + i * 0.039
        F.box(u - 0.016, 0.97, -0.03, u + 0.016, 1.065, -0.012, sw("white"))
        F.box(u - 0.013, 1.0, -0.075, u + 0.013, 1.06, -0.03, sw("lblue"))
        led(F, u, 1.045, "red", i % 3 == 0, s=0.003, w=-0.075)
    F.tag(W / 2, 1.1, -0.0045, "INTERPOSING RELAYS  52 CLOSE / TRIP", 0.4, 0.02)
    panel_base(F, ["RTU", "GWY", "I/O", "INTP"])
    return F


def comm_cabinet(b, x0, zf, facing):
    """COMM-1: the teleprotection multiplexer that carries the line differential and the permissive trip signals
    to the far ends over the OPGW fibre (real basis: a substation SONET / teleprotection multiplexer), the fibre patch
    panel with its yellow jumpers to the mux, and the 125 / -48 VDC converter shelf for the telecom gear (live)."""
    W = PANEL["W"]
    F = PanelFrame(b, x0, zf, facing)
    panel_shell(F, facing, "COMM-1  TELEPROTECTION / MUX")
    v = COMM_LIVE
    # fibre patch panel (1U): LC ports, jumpers down to the mux cards
    rack_unit(F, 1.93, 1.99)
    pp = ports(F, 0.14, 1.96, 12, pitch=0.022, colour="green", lit_every=0)
    # the mux shelf (6U): cards, the optical cards with their ports
    y0, y1 = 1.5, 1.85
    rack_unit(F, y0, y1, sw("dgrey"))
    cards = ["PWR", "CTL", "OPT", "OPT", "87L", "87L", "87L", "TPS", "TPS", "TPS", "VF", "PWR"]
    card_ports = []
    for i, nm in enumerate(cards):
        u = 0.11 + i * 0.049
        F.box(u, y0 + 0.02, -0.028, u + 0.043, y1 - 0.02, -0.018, sw("mgrey"))
        led(F, u + 0.0215, y1 - 0.05, "green", True, w=-0.028)
        led(F, u + 0.0215, y1 - 0.075, "amber", False, w=-0.028)
        if nm in ("OPT", "87L"):
            F.box(u + 0.012, y1 - 0.14, -0.034, u + 0.031, y1 - 0.12, -0.028, sw("dgrey"))
            card_ports.append((u + 0.0215, y1 - 0.13))
        F.tag(u + 0.0215, y0 + 0.05, -0.0285, nm, 0.036, 0.014)
    for (ua, ya), ub in zip(card_ports, pp[::2]):
        path(b, [F.P(ub, 1.96, -0.026), F.P(ub, 1.93, -0.07), F.P(ua, y1 - 0.06, -0.07), F.P(ua, y1 - 0.13, -0.036)], 0.003, sw("yellow"))
    F.tag(W / 2, 1.475, -0.0045, "TELEPROTECTION MUX  87L  POTT  DTT", 0.4, 0.02)
    # -48 V converter shelf (2U): two modules, the display
    y0 = 1.2
    rack_unit(F, y0, y0 + 0.088)
    for k in range(2):
        u = 0.12 + k * 0.16
        F.box(u, y0 + 0.01, -0.028, u + 0.14, y0 + 0.078, -0.018, sw("mgrey"))
        led(F, u + 0.02, y0 + 0.06, "green", True, w=-0.028)
        F.box(u + 0.05, y0 + 0.02, -0.032, u + 0.12, y0 + 0.032, -0.028, ls("stainless"))     # handle
    F.box(0.48, y0 + 0.018, -0.026, 0.68, y0 + 0.07, -0.018, BLACK)
    F.quad(0.58, y0 + 0.044, -0.027, 0.18, 0.042, ATLAS.mat(ATLAS.add("lcd:m48", lcd_img(
        [f"{v['v48']:.1f} V   {v['a48']:.1f} A", "IN 132.6 V   2 MODULES OK"], w_px=360, h_px=80))), lst=b.lit)
    F.tag(W / 2, y0 - 0.02, -0.0045, "-48 VDC CONVERTERS  FROM DCDP-1 CCT 20", 0.4, 0.02)
    panel_base(F, ["MUX A", "MUX B", "-48 A", "-48 B"])
    return F


def network_cabinet(b, x0, zf, facing):
    """NET-1: the station LAN (real basis: two managed substation-hardened Ethernet switches, the firewall / router to
    the utility WAN, a fibre patch panel from the splice enclosure, horizontal cable managers): port LEDs lit, blue
    patch cords through the managers, yellow fibre jumpers."""
    W = PANEL["W"]
    F = PanelFrame(b, x0, zf, facing)
    panel_shell(F, facing, "NET-1  STATION NETWORK")
    rack_unit(F, 1.88, 1.99)                                                   # fibre patch (2U)
    fp = ports(F, 0.13, 1.955, 12, pitch=0.022, lit_every=0) + ports(F, 0.13, 1.91, 12, pitch=0.022, lit_every=0)
    F.tag(0.66, 1.935, -0.0185, "FIBRE FROM SPLICE", 0.12, 0.016)
    rack_unit(F, 1.82, 1.875, BLACK)                                           # cable manager
    for u in np.arange(0.13, 0.7, 0.09):
        F.box(u - 0.008, 1.825, -0.06, u + 0.008, 1.87, -0.018, BLACK)
    sw_ports = []
    for k, y in enumerate((1.72, 1.6)):                                        # two switches
        rack_unit(F, y, y + 0.055, sw("dgrey"))
        sw_ports.append(ports(F, 0.13, y + 0.025, 16, pitch=0.026, colour="green", lit_every=1))
        F.tag(0.6, y + 0.04, -0.0185, f"SWITCH {'A' if k == 0 else 'B'}", 0.09, 0.016)
        rack_unit(F, y - 0.06, y - 0.005, BLACK)                               # its cable manager
        for u in np.arange(0.13, 0.7, 0.09):
            F.box(u - 0.008, y - 0.055, -0.06, u + 0.008, y - 0.01, -0.018, BLACK)
    for i in range(0, 16, 2):                                                  # patch cords between the switches
        ua = sw_ports[0][i]
        ub = sw_ports[1][i]
        path(b, [F.P(ua, 1.745, -0.022), F.P(ua, 1.705, -0.05), F.P(ub + 0.01, 1.66, -0.05), F.P(ub, 1.625, -0.022)], 0.0025, sw("blue"))
    for k, i in enumerate(range(0, 12, 3)):                                    # fibre to the switches' uplinks, in order
        up = sw_ports[0][12 + k]
        path(b, [F.P(fp[i], 1.955, -0.022), F.P(fp[i], 1.9, -0.065 - 0.006 * k), F.P(up, 1.79, -0.065 - 0.006 * k),
                 F.P(up, 1.745, -0.022)], 0.0025, sw("yellow"))
    rack_unit(F, 1.4, 1.48)                                                    # firewall / router
    ports(F, 0.13, 1.44, 6, pitch=0.026, colour="green")
    led(F, 0.6, 1.44, "green", True); led(F, 0.63, 1.44, "green", True)
    F.tag(0.6, 1.415, -0.0185, "FIREWALL / WAN", 0.12, 0.016)
    for y0, y1 in ((1.0, 1.38), (0.88, 0.98)):
        F.box(0.077, y0, -0.016, W - 0.077, y1, -0.012, sw("enamel"))          # blanking plates
    panel_base(F, ["SW A", "SW B", "FW"])
    return F


def ladder(b, axis, a0, a1, c0, c1, y0, y1, step=0.3):
    """A ladder cable tray running along axis ("x" or "z") from a0 to a1 between side rails at c0 and c1."""
    t = 0.012
    for c in (c0, c1):
        if axis == "x":
            b.box(a0, y0, c - t, a1, y1, c + t, ls("galv"))
        else:
            b.box(c - t, y0, a0, c + t, y1, a1, ls("galv"))
    for a in np.arange(a0 + 0.15, a1 - 0.05, step):
        if axis == "x":
            b.box(a - t, y0 + 0.005, c0 + t, a + t, y0 + 0.025, c1 - t, ls("galv"))
        else:
            b.box(c0 + t, y0 + 0.005, a - t, c1 - t, y0 + 0.025, a + t, ls("galv"))


def comm_room(b):
    """Machine 8 placed: the comm room's three cabinets along its back wall (RTU-1, COMM-1, NET-1), the fibre splice
    enclosure on the wall with the OPGW ducts up from the yard's duct bank, the yellow fibre raceway overhead into
    the cabinets, the ladder tray over the cabinets, its riser and the sleeve through the partition into the relay
    room's row-2 tray; the room's lights."""
    T = RH
    t, H, D = T["wall"], T["H"], T["D"]
    cx0, cx1, cz0, cz1 = T["comm"]
    zf = D - t - 0.05 - PANEL["D"]
    xs = [0.6 + k * (PANEL["W"] + 0.002) for k in range(3)]
    rtu_cabinet(b, xs[0], zf, -1)
    comm_cabinet(b, xs[1], zf, -1)
    network_cabinet(b, xs[2], zf, -1)
    # fibre splice enclosure on the back wall, the OPGW innerducts up from the floor
    ex0, ex1, ey0, ey1, ez0 = 3.4, 4.1, 1.1, 1.75, D - t - 0.32
    b.box(ex0, ey0, ez0, ex1, ey1, D - t, sw("mgrey"))
    b.box(ex0 + 0.02, ey0 + 0.02, ez0 - 0.005, ex1 - 0.02, ey1 - 0.02, ez0, sw("mgrey"))
    for y in (ey0 + 0.15, ey1 - 0.15):
        b.box(ex1 - 0.06, y - 0.02, ez0 - 0.02, ex1 - 0.04, y + 0.02, ez0 - 0.005, BLACK)                # latches
    tag(b, ((ex0 + ex1) / 2, ey1 - 0.08, ez0 - 0.005), "-z", "FIBRE SPLICE  OPGW L1 L2 L3", 0.4, 0.035)
    for x in (ex0 + 0.18, ex0 + 0.35, ex0 + 0.52):
        cyl(b, (x, 0.0, ez0 + 0.12), (x, ey0, ez0 + 0.12), 0.025, sw("orange"), sides=10)
    # yellow fibre raceway: up from the enclosure, along over the cabinets, drops into COMM-1 and NET-1
    yr0, yr1, zr = 2.62, 2.72, D - t - 0.2
    b.box(ex0 + 0.3, ey1, zr - 0.05, ex0 + 0.4, yr0, zr + 0.05, sw("yellow"))
    b.box(xs[1] + 0.3, yr0, zr - 0.05, ex0 + 0.4, yr1, zr + 0.05, sw("yellow"))
    for x in (xs[1] + 0.35, xs[2] + 0.35):
        b.box(x - 0.04, PANEL["H"], zr - 0.04, x + 0.04, yr0, zr + 0.04, sw("yellow"))
    # the ladder tray over the cabinets, its run to the partition, the riser, the sleeve through to the relay room
    yl0, yl1 = 2.42, 2.5
    zl0, zl1 = zf + 0.1, zf + 0.4
    ladder(b, "x", 0.4, cx1 - 0.35, zl0, zl1, yl0, yl1)
    ladder(b, "z", 9.4, zl0 - 0.012, cx1 - 0.33, cx1 - 0.03, yl0, yl1)
    for z in (9.43, 9.66):                                                        # riser rails
        b.box(cx1 - 0.33, yl1, z - 0.012, cx1 - 0.3, 3.3, z + 0.012, ls("galv"))
    ladder(b, "x", cx1 - 0.3, 5.99, 9.43, 9.66, 3.25, 3.33)
    b.box(cx1, 3.2, 9.38, cx1 + 0.15, 3.22, 9.68, ls("galv"))                       # the sleeve's sill
    b.box(cx1 + 0.002, 3.33, 9.39, cx1 + 0.148, 3.42, 9.67, sw("red"))               # firestop pillows
    # cable bundles in the trays (DNP3, control, the relays' comm)
    for (a, c_, y_, axis) in ((0.6, cx1 - 0.35, yl0 + 0.025, "x"),):
        b.box(a, y_, zl0 + 0.06, c_, y_ + 0.04, zl1 - 0.06, BLACK)
    b.box(cx1 - 0.3, yl0 + 0.025, 9.45, cx1 - 0.06, yl0 + 0.065, zl0 - 0.012, BLACK)
    b.box(cx1 - 0.27, yl1, 9.45, cx1 - 0.23, 3.275, 9.61, BLACK)
    b.box(cx1 - 0.23, 3.275, 9.45, 5.99, 3.31, 9.61, BLACK)
    # hanger rods
    for x in (1.0, 2.6, 4.2):
        for z in (zl0 - 0.03, zl1 + 0.03):
            rod(b, (x, yl0 - 0.02, z), (x, H, z), 0.006, ls("galv"), 4)
        b.box(x - 0.02, yl0 - 0.04, zl0 - 0.05, x + 0.02, yl0, zl1 + 0.05, ls("galv"))
    # lights
    for xl in (1.6, 3.4):
        b.box(xl - 0.6, H - 0.06, 9.35, xl + 0.6, H, 9.65, sw("white"))
        b.box(xl - 0.56, H - 0.065, 9.38, xl + 0.56, H - 0.06, 9.62, CB.LENS["white"], b.lit)
