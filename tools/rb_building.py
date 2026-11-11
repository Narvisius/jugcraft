"""The medium plant's reactor building, inside: the structure the approved machines stand in (tools/reactor_site.py).

The machines (tools/reactor_head.py ... loop_piping.py) were approved on 19 October in the context of the operating
floor slab and refuelling cavity; this draws that context in the building's frame (reactor_head.py: the vessel's axis
at (C, C), -z north, one block a metre, local y = plant y + 6):

- the operating floor (y 20 to 21, its top level with the plant's y 15), open round the refuelling cavity, the upper
  internals' bay west of it and the transfer canal east of it, round each steam generator's and the pressurizer's
  enclosure (their walls rise through it) and the steam and feedwater lines' collars; epoxy on top, coated concrete
  under it; a handrail round the cavity; two caged ladders down to the loop compartments (west and east);
- the refuelling cavity (13 x 13, floor y 12) lined in stainless: its floor round the vessel's seal ring, its walls up
  to the operating floor (thin under the coolant pumps' motor platforms, which come within centimetres of it), the
  internals' bay (floor y 9.5), the canal and the upender pit at its deep end (floor y 2: the plant's loop floor is
  level with the grass (y 6 = the plant's 0), so the pit's floor, the transfer tube and what stands in the pit are a
  metre higher than drawn, to stay in a flat world's ground (it ends at the plant's y -4)) with the transfer tube's
  opening;
- the primary shield wall round the vessel (r 4.15 to 7.1, y 6 to 11.6) with openings for the eight legs, and the pit
  under it down to y 2 (the vessel's bottom head under its floor);
- the polar crane's ring girder (a concrete corbel r 19.8 to 21.8) and its rail (r 20.6, top y 46);
- light fittings: high-bay LEDs under the operating floor, floodlights under the ring girder (the game's light comes
  from light blocks the plant puts by them and over the floors: light_spots()).

The containment's shell, liner, base slab and loop compartment floor (y 6: the plant's epoxy floor blocks) stay
blocks (tools/medium_plant.py containment); reactor_site.keep_cell() tells the compiler which cells it may draw in.
"""
import math

import control_boards as CB
import reactor_head as RH
import rb_parts as RP
import refuelling_machine as RM
import steam_generator as SG
import fuel_transfer as FT
import refuel_floor as RF
from reactor_head import Head, C

OP = RM.OP                      # 21: the operating floor's top
SLAB = OP - 1.0                 # its underside
CF = RH.CAVITY_FLOOR            # 12
LOOP = 6.0                      # the loop compartments' floor (blocks under it)
WALL = SG.WALL                  # the coated concrete (as the enclosures')
EPOXY = "rb_epoxy"              # the floors' top (the plant's epoxy floor texture)
LINER = "stainless"
H = RH.CAVITY_HALF
CAV = (C - H, C + H)            # the cavity, x and z
BAY_X, BAY_Z, BAY_FLOOR = RF.BAY_X, RF.BAY_Z, RF.BAY_FLOOR
CANAL_Z = (FT.ZC - FT.CANAL_HALF, FT.ZC + FT.CANAL_HALF)
CANAL_X = (CAV[1], FT.CANAL_END)
PIT_X = FT.PIT_X
PIT_Z = (FT.ZC - FT.PIT_HALF, FT.ZC + FT.PIT_HALF)
PIT_LIFT = 1.0                  # the upender pit's floor and what stands in it, lifted (reactor_site.ft_lift)
PIT_FLOOR = 0.95 + PIT_LIFT + 0.055   # (a hair over the stanchions' base plates: the floor's face in the world's lowest cell)
VPIT_FLOOR = 2.0                # the vessel's pit (its bottom head under it)
T = 1.0                         # the cavity's walls
SHIELD_IN, SHIELD_OUT, SHIELD_TOP = 4.15, 7.1, 11.6
SEAL_R = 3.45
PZR_XZ = (C - 13.6, C + 10.9)
PZR_HOLE = 3.3
RCP_RING = 3.05                 # a coolant pump's motor platform's reach (its handrail ring), from its axis
RCP_TOP = 14.45                 # ... and its handrail's top
# The caged ladders down to the loop compartments: the rungs' line (x, z) and the liner's side (a block's edge 0.4 off)
LADDERS = [(-16.6, -2.5, -1), (24.6, 3.5, 1)]
GIRDER = (19.8, 21.8, 44.6, 45.55)     # (its top under the rail's plinth: the crane's lugs hang down beside the rail)
RAIL_R = 20.6
# Where the site draws the loop floor itself (round the pits it opens): (x0, z0, x1, z1), whole blocks
PATCHES = [(C - 6.5, C - 6.5, C + 6.5, C + 6.5), (CANAL_X[0], CANAL_Z[0] - T - 1.0, CANAL_X[1] + T + 1.0, CANAL_Z[1] + T + 1.0)]


def rcp_xz():
    return [SG.pump_xz(loop) for loop in (1, 2, 3, 4)]


def ladder_hole(x, z, w):
    wall = x + w * 0.4
    return (min(wall, wall - w), max(wall, wall - w), z - 0.5, z + 0.5)


# ---------------------------------------------------------------- flat surfaces with holes

def holes():
    """The operating floor's openings: rectangles ("r", x0, x1, z0, z1) and circles ("c", x, z, r)."""
    out = [("r", CAV[0], CAV[1], CAV[0], CAV[1]),
           ("r", BAY_X[0], BAY_X[1], BAY_Z[0], BAY_Z[1]),
           ("r", CANAL_X[0], CANAL_X[1], CANAL_Z[0], CANAL_Z[1])]
    for loop in (1, 2, 3, 4):
        for k, (x, z, r) in enumerate(SG.deck_holes(SG.SG(loop))):
            out.append(("c", x, z, r - (0.05 if k == 0 else 0.0)))
    out.append(("c", PZR_XZ[0], PZR_XZ[1], PZR_HOLE - 0.05))
    for (x, z, w) in LADDERS:
        out.append(("r",) + ladder_hole(x, z, w))
    return out


def _span(h, x):
    """A hole's z interval at x, or None."""
    if h[0] == "r":
        if h[1] - 1e-9 <= x <= h[2] + 1e-9:
            return (h[3], h[4])
        return None
    dx = x - h[1]
    if abs(dx) >= h[3]:
        return None
    s = math.sqrt(h[3] * h[3] - dx * dx)
    return (h[2] - s, h[2] + s)


def floor_strips(x0, x1, z0, z1, hs, step=0.25):
    """The region x0..x1, z0..z1 less the holes, as strips along x: [((xa, za_lo, za_hi), (xb, zb_lo, zb_hi))] (a
    circle's edge straight across each strip)."""
    xs = {x0, x1}
    k = math.floor(x0 / step) * step
    while k < x1:
        if k > x0:
            xs.add(round(k, 6))
        k += step
    for h in hs:
        if h[0] == "r":
            xs |= {v for v in (h[1], h[2]) if x0 < v < x1}
        else:
            xs |= {v for v in (h[1] - h[3], h[1] + h[3]) if x0 < v < x1}
    xs = sorted(xs)
    out = []
    for xa, xb in zip(xs, xs[1:]):
        xm = (xa + xb) / 2
        cuts = []
        for h in hs:
            sm = _span(h, xm)
            if sm is None:
                continue
            if h[0] == "c":
                sa = _span(h, xa) or (h[2], h[2])
                sb = _span(h, xb) or (h[2], h[2])
            else:
                sa = sb = sm
            cuts.append((sm[0], sa, sb))
        cuts.sort()
        lo_a = lo_b = z0
        for _, sa, sb in cuts:
            ha, hb = max(z0, min(sa[0], z1)), max(z0, min(sb[0], z1))
            if ha > lo_a + 1e-6 or hb > lo_b + 1e-6:
                out.append(((xa, lo_a, max(lo_a, ha)), (xb, lo_b, max(lo_b, hb))))
            lo_a, lo_b = min(z1, max(lo_a, sa[1])), min(z1, max(lo_b, sb[1]))
        if lo_a < z1 - 1e-6 or lo_b < z1 - 1e-6:
            out.append(((xa, lo_a, z1), (xb, lo_b, z1)))
    return out


def _clip2(poly, k, below):
    """A convex polygon [(x, z)] clipped by the line z = k (keeping z <= k, or z >= k)."""
    out = []
    n = len(poly)
    for i in range(n):
        p, q = poly[i], poly[(i + 1) % n]
        dp, dq = p[1] - k, q[1] - k
        inp = dp <= 1e-9 if below else dp >= -1e-9
        inq = dq <= 1e-9 if below else dq >= -1e-9
        if inp:
            out.append(p)
        if inp != inq:
            t = dp / (dp - dq)
            out.append((p[0] + (q[0] - p[0]) * t, k))
    clean = []
    for p in out:
        if not clean or abs(p[0] - clean[-1][0]) + abs(p[1] - clean[-1][1]) > 1e-9:
            clean.append(p)
    if len(clean) > 1 and abs(clean[0][0] - clean[-1][0]) + abs(clean[0][1] - clean[-1][1]) <= 1e-9:
        clean.pop()
    return clean


def surface(b, strips, y, mat, down=False, lst=None):
    """The strips as faces at y facing up (or down), cut at the blocks' edges, each piece its own part of the texture
    as the blocks lie (so the floor's pattern runs on across the blocks round it, and each block draws its own)."""
    for (xa, za0, za1), (xb, zb0, zb1) in strips:
        if za1 - za0 < 1e-5 and zb1 - zb0 < 1e-5:
            continue
        poly = [(xa, za0), (xb, zb0), (xb, zb1), (xa, za1)]
        zmin, zmax = min(za0, zb0), max(za1, zb1)
        rest = poly
        k = math.floor(zmin + 1e-9) + 1
        pieces = []
        while k < zmax - 1e-9 and len(rest) >= 3:
            pieces.append(_clip2(rest, k, True))
            rest = _clip2(rest, k, False)
            k += 1
        pieces.append(rest)
        for pc in pieces:
            if len(pc) < 3:
                continue
            xs, zs = [p[0] for p in pc], [p[1] for p in pc]
            # (from the corner nearest the low x, low z one: the texture's corner, as the strip's own order has it)
            i0 = min(range(len(pc)), key=lambda i: (pc[i][0] - min(xs)) + (pc[i][1] - min(zs)))
            pc = pc[i0:] + pc[:i0]
            cx, cz = math.floor(min(xs) + 1e-9), math.floor(min(zs) + 1e-9)
            u0, u1 = min(zs) - cz, max(zs) - cz
            v0, v1 = min(xs) - cx, max(xs) - cx
            reg = mat if not isinstance(mat, str) else [mat, max(0.0, u0) + 0.001, max(0.0, v0) + 0.001, min(1.0, u1) - 0.001, min(1.0, v1) - 0.001]
            quads = [pc[:4]] if len(pc) == 4 else [[pc[0], pc[1], pc[2], pc[2]]] if len(pc) == 3 else \
                [[pc[0], pc[i], pc[i + 1], pc[i + 2] if i + 2 < len(pc) else pc[i + 1]] for i in range(1, len(pc) - 1, 2)]
            for q in quads:
                p = [(v[0], y, v[1]) for v in q]
                if down:
                    p = p[::-1]
                b.quad(p[0], p[1], p[2], p[3], reg, lst)


def strip_boxes(strips, y0, y1):
    out = []
    for (xa, za0, za1), (xb, zb0, zb1) in strips:
        z0, z1 = max(za0, zb0), min(za1, zb1)
        if z1 - z0 > 0.05 and xb - xa > 0.01:
            out.append((xa, y0, z0, xb, y1, z1))
    return out


def vface(b, axis, f, normal, a0, a1, y0, y1, mat, lst=None):
    """A vertical face at x = f (axis "x") or z = f (axis "z"), facing normal (+1 / -1 along that axis), from a0 to a1
    along it and y0 to y1."""
    if axis == "x":
        p = [(f, y1, a0), (f, y0, a0), (f, y0, a1), (f, y1, a1)]          # facing +x
    else:
        p = [(a1, y1, f), (a1, y0, f), (a0, y0, f), (a0, y1, f)]          # facing +z
    if normal < 0:
        p = p[::-1]
    b.quad(p[0], p[1], p[2], p[3], mat, lst)


# ---------------------------------------------------------------- the building

class Building(Head):
    def __init__(self):
        super().__init__()
        self.collide = []           # collision boxes the faces don't give: (x0, y0, z0, x1, y1, z1), or ring pieces
        self.lights = []            # light fittings' spots (x, y, z): the plant puts light blocks by them
        self.fill = []              # spots for lights over the floors with no fitting (the floors' light)
        self.voids = []             # what the plant keeps open under the ground (the pits): boxes, or ("ring", r, y0, y1)


def op_floor(b):
    hs = holes()
    R = 23.0
    strips = floor_strips(C - R, C + R, C - R, C + R, hs)
    surface(b, strips, OP, EPOXY)
    surface(b, strips, SLAB, WALL, down=True)
    b.collide += strip_boxes(strips, SLAB, OP)
    # the ladders' openings: their edges (the side by the liner is the liner's block)
    for (x, z, w) in LADDERS:
        x0, x1, z0, z1 = ladder_hole(x, z, w)
        far = x0 if w > 0 else x1
        vface(b, "x", far, w, z0, z1, SLAB, OP, WALL)
        vface(b, "z", z0, +1, x0, x1, SLAB, OP, WALL)
        vface(b, "z", z1, -1, x0, x1, SLAB, OP, WALL)
    # the steam and feedwater lines' collars: a steel sleeve through the slab with a lip on top
    for loop in (1, 2, 3, 4):
        for (x, z, r) in SG.deck_holes(SG.SG(loop))[1:]:
            _sleeve(b, x, z, r)


def _sleeve(b, x, z, r):
    n = 16
    p = lambda a, y, rr: (x + rr * math.cos(a), y, z + rr * math.sin(a))
    for i in range(n):
        a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        top = OP + 0.04
        b.quad(p(a1, top, r), p(a1, SLAB, r), p(a0, SLAB, r), p(a0, top, r), "stainless_dark")                 # inside
        b.quad(p(a0, top, r), p(a0, top, r + 0.08), p(a1, top, r + 0.08), p(a1, top, r), "stainless_dark")     # lip
        b.quad(p(a0, top, r + 0.08), p(a0, OP, r + 0.08), p(a1, OP, r + 0.08), p(a1, top, r + 0.08), "stainless_dark")


def _recess(face, out, lo, hi, axis):
    """Where a wall (its face at `face`, thickening toward `out` along its axis) must stay thin under the pumps'
    platforms: [(lo, hi, thickness)] along it."""
    res = []
    for (px, pz) in rcp_xz():
        pa, pb = (px, pz) if axis == "x" else (pz, px)          # pa across the wall, pb along it
        d = (pa - face) * out
        if d <= 0 or d - RCP_RING >= T:
            continue
        t = max(0.06, d - RCP_RING)
        half = math.sqrt(max(0.0, RCP_RING ** 2 - max(0.0, d - T) ** 2))
        a, c = max(lo, pb - half), min(hi, pb + half)
        if c > a:
            res.append((a, c, t))
    return res


def _wall(b, axis, face, out, lo, hi, y0, y1, gaps=(), liner_top=None):
    """A wall lined on its face at `face` (x for axis "x") toward the pool, concrete T thick toward `out`, lo to hi
    along it and y0 to y1; thin where a pump's platform comes near it (below RCP_TOP). gaps: (lo, hi, ya, yb)."""
    rec = _recess(face, out, lo, hi, axis)
    cuts = sorted({lo, hi} | {v for r in rec for v in r[:2]} | {v for g in gaps for v in g[:2] if lo < v < hi})
    for a, c in zip(cuts, cuts[1:]):
        m = (a + c) / 2
        t = min([r[2] for r in rec if r[0] <= m <= r[1]] or [T])
        ys = [(y0, y1)]
        for g in gaps:
            if g[0] <= m <= g[1]:
                new = []
                for ya, yb in ys:
                    if g[3] <= ya or g[2] >= yb:
                        new.append((ya, yb))
                    else:
                        if g[2] > ya:
                            new.append((ya, g[2]))
                        if g[3] < yb:
                            new.append((g[3], yb))
                ys = new
        for ya, yb in ys:
            parts = [(ya, min(yb, RCP_TOP), t), (max(ya, RCP_TOP), yb, T)] if t < T else [(ya, yb, T)]
            for pa, pb, tt in parts:
                if pb - pa < 1e-6:
                    continue
                f0, f1 = sorted((face, face + out * tt))
                if axis == "x":
                    liner_i = 2 if out > 0 else 3
                else:
                    liner_i = 0 if out > 0 else 1
                faces = [WALL] * 6
                faces[liner_i] = LINER
                faces[4] = WALL if pb < y1 - 1e-6 else liner_top
                faces[5] = WALL
                if axis == "x":
                    b.box(f0, pa, a, f1, pb, c, faces)
                else:
                    b.box(a, pa, f0, c, pb, f1, faces)
                b.collide.append((f0, pa, a, f1, pb, c) if axis == "x" else (a, pa, f0, c, pb, f1))


def cavity(b):
    x0, x1 = CAV
    # its floor round the vessel's seal ring: stainless on a slab over the primary shield
    strips = floor_strips(x0, x1, x0, x1, [("c", C, C, SEAL_R - 0.03)])
    surface(b, strips, CF, LINER)
    surface(b, strips, SHIELD_TOP, WALL, down=True)
    b.collide += strip_boxes(strips, SHIELD_TOP, CF)
    # its walls: west (the bay's opening), east (the canal's), north and south
    _wall(b, "x", x0, -1, x0 - T, x1 + T, SHIELD_TOP, SLAB, gaps=[(BAY_Z[0], BAY_Z[1], SHIELD_TOP - 1, SLAB + 1)])
    _wall(b, "x", x1, +1, x0 - T, x1 + T, SHIELD_TOP, SLAB, gaps=[(CANAL_Z[0], CANAL_Z[1], SHIELD_TOP - 1, SLAB + 1)])
    _wall(b, "z", x0, -1, x0, x1, SHIELD_TOP, SLAB)
    _wall(b, "z", x1, +1, x0, x1, SHIELD_TOP, SLAB)
    # the liner on up through the operating floor's edge
    vface(b, "z", x0, +1, x0, x1, SLAB, OP, LINER)
    vface(b, "z", x1, -1, BAY_X[1], x1, SLAB, OP, LINER)
    vface(b, "x", x0, +1, x0, BAY_Z[0], SLAB, OP, LINER)
    vface(b, "x", x1, -1, x0, CANAL_Z[0], SLAB, OP, LINER)
    vface(b, "x", x1, -1, CANAL_Z[1], x1, SLAB, OP, LINER)


def bay(b):
    x0, x1 = BAY_X
    z0, z1 = BAY_Z
    b.box(x0, BAY_FLOOR - 0.5, z0, x1, BAY_FLOOR, z1, [None, None, None, None, LINER, WALL])
    b.collide.append((x0, BAY_FLOOR - 0.5, z0, x1, BAY_FLOOR, z1))
    # the step up into the cavity (the cavity floor's edge over the bay)
    b.box(x1, BAY_FLOOR - 0.5, z0, x1 + 0.4, CF, z1, [None, None, LINER, None, None, WALL])
    b.collide.append((x1, BAY_FLOOR - 0.5, z0, x1 + 0.4, CF, z1))
    _wall(b, "z", z0, -1, x0 - T, x1 - T, BAY_FLOOR - 0.5, SLAB)
    _wall(b, "z", z1, +1, x0 - T, x1 - T, BAY_FLOOR - 0.5, SLAB)
    _wall(b, "x", x0, -1, z0, z1, BAY_FLOOR - 0.5, SLAB)
    vface(b, "z", z0, +1, x0, x1, SLAB, OP, LINER)
    vface(b, "z", z1, -1, x0, x1, SLAB, OP, LINER)
    vface(b, "x", x0, +1, z0, z1, SLAB, OP, LINER)


def canal(b):
    x0, x1 = CANAL_X
    z0, z1 = CANAL_Z
    p0, p1 = PIT_X
    q0, q1 = PIT_Z
    # the canal's floor from the cavity to the pit, on the concrete under it down to the loop floor
    u0, u1 = z0 - T + 0.15, z1 + T - 0.15       # (clear of the pumps' tie bars to the primary shield)
    b.box(x0, LOOP, u0, p0, CF, u1, [WALL, WALL, None, None, LINER, None])
    b.collide.append((x0, LOOP, u0, p0, CF, u1))
    # the pit's west face (the step down from the canal's floor) and its side walls (their tops the ledges)
    vface(b, "x", p0, +1, q0, q1, PIT_FLOOR, CF, LINER)
    for (za, zb, liner_i) in ((u0, q0, 1), (q1, u1, 0)):
        faces = [WALL, WALL, None, None, LINER, None]
        faces[liner_i] = LINER
        b.box(p0, PIT_FLOOR, za, p1, CF, zb, faces)
        b.collide.append((p0, PIT_FLOOR, za, p1, CF, zb))
    surface(b, [((p0, q0, q1), (p1, q0, q1))], PIT_FLOOR, LINER)
    b.collide.append((p0, PIT_FLOOR - 0.5, q0, p1, PIT_FLOOR, q1))
    # the end wall, from the pit's floor to the slab, its liner round the transfer tube's opening (the tube's
    # flange covers the opening's corners)
    e = x1
    b.box(e, PIT_FLOOR, u0, e + T, CF, u1, [WALL, WALL, None, WALL, None, None])
    b.box(e, CF, z0 - T, e + T, SLAB, z1 + T, [WALL, WALL, None, WALL, None, WALL])
    b.collide.append((e, PIT_FLOOR, u0, e + T, CF, u1))
    b.collide.append((e, CF, z0 - T, e + T, SLAB, z1 + T))
    ty, h = FT.TUBE_Y + PIT_LIFT, FT.TUBE_R + 0.005
    zc = FT.ZC
    for (za, zb, ya, yb) in ((q0, q1, ty + h, CF), (q0, q1, PIT_FLOOR, ty - h), (q0, zc - h, ty - h, ty + h), (zc + h, q1, ty - h, ty + h)):
        vface(b, "x", e, -1, za, zb, ya, yb, LINER)
    vface(b, "x", e, -1, z0, z1, CF, OP, LINER)
    # the canal's side walls over its floor, from the cavity's wall to the end wall
    _wall(b, "z", z0, -1, x0 + T, x1, CF, SLAB)
    _wall(b, "z", z1, +1, x0 + T, x1, CF, SLAB)
    vface(b, "z", z0, +1, x0, x1, SLAB, OP, LINER)
    vface(b, "z", z1, -1, x0, x1, SLAB, OP, LINER)
    b.voids.append((p0, PIT_FLOOR, q0, p1, LOOP, q1))


def primary_shield(b):
    gaps = shield_gaps()
    _ring(b, SHIELD_IN, SHIELD_OUT, LOOP, SHIELD_TOP, gaps, outer=True)
    _ring(b, SHIELD_IN, SHIELD_OUT, VPIT_FLOOR, LOOP, [], outer=False, solid_to=SHIELD_IN + 0.3)
    # the pit's floor round the vessel's bottom head
    r_head = math.sqrt(RH.R_SHELL ** 2 - (RH.R_SHELL - VPIT_FLOOR) ** 2) - 0.15     # (tucked under the head's facets)
    b.ring_top(r_head, SHIELD_IN + 0.02, VPIT_FLOOR, WALL)
    b.collide.append(("disc", 0.0, SHIELD_IN, VPIT_FLOOR - 0.5, VPIT_FLOOR))
    b.voids.append(("ring", SHIELD_IN, VPIT_FLOOR, LOOP))


def shield_gaps():
    """Openings in the primary shield for the legs: (f0, f1, y0, y1) about the vessel's axis."""
    out = []
    for hot, cold in RH.LOOP_AZ:
        for a, r in ((hot, 0.62), (cold, 0.58)):
            half = math.asin(min(0.95, (r + 0.08) / SHIELD_IN))
            out.append((a - half, a + half, RH.NOZZLE_Y - r - 0.08, RH.NOZZLE_Y + r + 0.08))
    return out


def _ring(b, r_in, r_out, y0, y1, gaps, outer=True, solid_to=None):
    """A round wall about the vessel's axis with openings (f0, f1, ya, yb); outer: its outside face too; solid_to: its
    collision only that far out (under the ground, where the ground's own blocks are the rest)."""
    ys = sorted({y0, y1} | {g[2] for g in gaps if y0 < g[2] < y1} | {g[3] for g in gaps if y0 < g[3] < y1})
    for ya, yb in zip(ys, ys[1:]):
        active = [(g[0], g[1]) for g in gaps if g[2] <= ya + 1e-6 and g[3] >= yb - 1e-6]
        for fa, fb in SG._solid(active):
            y = ya
            while y < yb - 1e-6:
                yt = min(yb, math.floor(y + 1e-6) + 1.0)
                b.band(r_in, y, r_in, yt, WALL, fa, fb, inward=True, stretch=True)
                if outer:
                    b.band(r_out, y, r_out, yt, WALL, fa, fb, stretch=True)
                y = yt
        for g0, g1 in active:
            _jamb(b, g0, r_in, r_out, ya, yb, +1)
            _jamb(b, g1, r_in, r_out, ya, yb, -1)
    for g in gaps:
        if g[2] > y0 + 1e-6:
            b.ring_top(r_in, r_out, g[2], WALL, g[0], g[1])
        if g[3] < y1 - 1e-6:
            b.ring_top(r_in, r_out, g[3], WALL, g[0], g[1], down=True)
    b.collide.append(("ring", r_in, solid_to or r_out, y0, y1, gaps))


def _jamb(b, f, r_in, r_out, y0, y1, facing):
    p = lambda r, y: (C + r * math.cos(f), y, C + r * math.sin(f))
    a, b_, c, d = p(r_in, y1), p(r_in, y0), p(r_out, y0), p(r_out, y1)
    if facing < 0:
        b.quad(a, b_, c, d, WALL)
    else:
        b.quad(d, c, b_, a, WALL)


def ring_girder(b):
    r0, r1, y0, y1 = GIRDER
    b.band(r0, y0, r0, y1, WALL, inward=True, stretch=True)
    b.ring_top(r0, r1, y1, WALL)
    b.ring_top(r0, r1, y0, WALL, down=True)
    # the rail: its grouted plinth, sole plate, foot, web, head
    for (ra, rb, ya, yb, m) in ((RAIL_R - 0.045, RAIL_R + 0.09, y1, 45.85, WALL),
                                (RAIL_R - 0.05, RAIL_R + 0.1, 45.85, 45.87, "gunmetal"),
                                (RAIL_R - 0.04, RAIL_R + 0.075, 45.87, 45.89, "stainless_dark"),
                                (RAIL_R - 0.012, RAIL_R + 0.018, 45.89, 45.95, "stainless_dark"),
                                (RAIL_R - 0.035, RAIL_R + 0.04, 45.95, 46.0, "stainless_dark")):
        b.band(ra, ya, ra, yb, m, inward=True, stretch=not isinstance(m, str))
        b.band(rb, ya, rb, yb, m, stretch=not isinstance(m, str))
        b.ring_top(ra, rb, yb, m)
    b.collide.append(("ring", r0, r1, y0, y1, []))
    # floodlights under it, aimed in at the operating floor
    for k in range(24):
        _flood(b, 2 * math.pi * (k + 0.5) / 24)


def _flood(b, a):
    r, y = GIRDER[0] + 0.6, GIRDER[2]
    top = RH.P(r, a, y)
    b.obox(RH.P(r, a, y - 0.02), (math.cos(a), 0, math.sin(a)), (0, 1, 0), 0.12, 0.02, 0.1, "gunmetal")
    pivot = RH.P(r, a, y - 0.22)
    b.cyl(top, pivot, 0.02, "gunmetal", "gunmetal", 6)
    d = RP.unit((-math.cos(a) * 0.6, -0.8, -math.sin(a) * 0.6))
    back = tuple(pivot[i] - d[i] * 0.14 for i in range(3))
    tip = tuple(pivot[i] + d[i] * 0.12 for i in range(3))
    b.cyl(back, tip, 0.2, "paint_grey", "paint_grey", 12)
    lens = tuple(tip[i] + d[i] * 0.008 for i in range(3))
    b.cyl(tip, lens, 0.18, "paint_grey", CB.pic("lamp_white"), 12, lst=b.lit)


def handrails(b, refuel=False):
    """Round the cavity, the bay and the canal, a little back from the edge (the refuelling machine's bridge runs over
    it; while refuelling, open where the head package's cable bridges stand raised), and either side of the ladders'
    openings."""
    e = 0.15
    x0, x1 = CAV
    pts = [(x0 - e, x0 - e), (x1 + e, x0 - e), (x1 + e, CANAL_Z[0] - e), (CANAL_X[1] + e, CANAL_Z[0] - e),
           (CANAL_X[1] + e, CANAL_Z[1] + e), (x1 + e, CANAL_Z[1] + e), (x1 + e, x1 + e), (BAY_X[0] - e, x1 + e),
           (BAY_X[0] - e, BAY_Z[0] - e), (x0 - e, BAY_Z[0] - e)]
    gaps = []
    if refuel:
        xa, xb = 6.55, 7.85                     # (the raised cable bridges: reactor_head.cable_bridge)
        n_len = (x1 + e) - (x0 - e)
        s_len = (x1 + e) - (BAY_X[0] - e)
        gaps = [(0, (xa - (x0 - e)) / n_len, (xb - (x0 - e)) / n_len), (7, ((x1 + e) - xb) / s_len, ((x1 + e) - xa) / s_len)]
    RP.handrail(b, b.still, pts, OP, gaps=gaps, closed=True)
    for (x, z, w) in LADDERS:
        hx0, hx1, hz0, hz1 = ladder_hole(x, z, w)
        wall = hx1 if w > 0 else hx0
        far = (hx0 - 0.15) if w > 0 else (hx1 + 0.15)
        for zz in (hz0 - 0.12, hz1 + 0.12):
            RP.handrail(b, b.still, [(wall - w * 0.05, zz), (far, zz)], OP)


def ladders(b):
    for (x, z, w) in LADDERS:
        RP.ladder(b, b.still, x, z, LOOP, OP, along=(0, 0, 1), standoffs=[LOOP + 1.5, LOOP + 5.5, LOOP + 9.5, OP - 1.5],
                  standoff_to=0.4, wall_dir=(w, 0, 0))
        # its cage: hoops every 0.9 m from 2.4 m up, straps between them
        cx = x - w * 0.42
        hoops = [LOOP + 2.4 + 0.9 * k for k in range(int((OP - LOOP - 2.4) / 0.9) + 1)]
        for y in hoops:
            pts = [(x + w * 0.38, z - 0.36), (cx, z - 0.36), (cx, z + 0.36), (x + w * 0.38, z + 0.36)]
            for pa, pb in zip(pts, pts[1:]):
                b.cyl((pa[0], y, pa[1]), (pb[0], y, pb[1]), 0.012, RP.YELLOW, RP.YELLOW, 6)
        for (sx, sz) in ((cx, z - 0.36), (cx, z), (cx, z + 0.36), (x, z - 0.36), (x, z + 0.36)):
            b.cyl((sx, hoops[0], sz), (sx, hoops[-1], sz), 0.01, RP.YELLOW, RP.YELLOW, 6)


def loop_patches(b):
    """The loop compartments' floor where the site draws it (round the pits it opens)."""
    pit = ("c", C, C, SHIELD_IN - 0.05)
    canal_ = ("r", CANAL_X[0], CANAL_X[1] + T, CANAL_Z[0] - T, CANAL_Z[1] + T)
    for (x0, z0, x1, z1) in PATCHES:
        strips = floor_strips(x0, x1, z0, z1, [pit, canal_])
        surface(b, strips, LOOP, EPOXY)
        b.collide += strip_boxes(strips, LOOP - 1.0, LOOP)


def fittings(b):
    """High-bay LED fittings under the operating floor (a grid, clear of what stands there); the floors' light."""
    for gx in range(-16, 25, 6):
        for gz in range(-16, 25, 6):
            x, z = gx + 0.5, gz + 0.5
            r = math.hypot(x - C, z - C)
            if r > 19.5 or r < 8.0:
                continue
            if any(math.hypot(x - sx, z - sz) < 4.6 for sx, sz in RH.SG_XZ) or math.hypot(x - PZR_XZ[0], z - PZR_XZ[1]) < 3.9:
                continue
            if any(math.hypot(x - px, z - pz) < 3.4 for px, pz in rcp_xz()):
                continue
            if CANAL_X[0] - 1.5 < x < CANAL_X[1] + T + 1.0 and CANAL_Z[0] - T - 1.0 < z < CANAL_Z[1] + T + 1.0:
                continue
            if BAY_X[0] - T - 1.0 < x < CAV[1] + T + 1.0 and CAV[0] - T - 1.0 < z < CAV[1] + T + 1.0:
                continue
            _high_bay(b, x, z)
    # light over the floors (no fitting: the plant's light blocks, where nothing stands): the loop floor, the
    # operating floor, the cavity
    for gx in range(-17, 26, 7):
        for gz in range(-17, 26, 7):
            x, z = gx + 0.5, gz + 0.5
            if math.hypot(x - C, z - C) < 20.5:
                b.fill.append((x, LOOP + 3.5, z))
                b.fill.append((x, OP + 3.5, z))
    for (x, z) in ((C - 3.5, C - 3.5), (C + 3.5, C - 3.5), (C - 3.5, C + 3.5), (C + 3.5, C + 3.5), (C - 6.0, C + 6.0)):
        b.fill.append((x, CF + 4.5, z))


def _high_bay(b, x, z):
    y = SLAB
    b.cyl((x, y, z), (x, y - 0.18, z), 0.03, "gunmetal", "gunmetal", 8)
    b.cyl((x, y - 0.18, z), (x, y - 0.32, z), 0.22, "paint_grey", "paint_grey", 16)
    b.cyl((x, y - 0.32, z), (x, y - 0.34, z), 0.2, "paint_grey", CB.pic("lamp_white"), 16, lst=b.lit)
    for k in range(8):
        a = 2 * math.pi * k / 8
        c = (x + 0.24 * math.cos(a), y - 0.25, z + 0.24 * math.sin(a))
        b.obox(c, (math.cos(a), 0, math.sin(a)), (0, 1, 0), 0.02, 0.06, 0.008, "gunmetal")
    b.lights.append((x, y - 0.6, z))


def build(refuel=False):
    b = Building()
    op_floor(b)
    cavity(b)
    bay(b)
    canal(b)
    primary_shield(b)
    ring_girder(b)
    handrails(b, refuel)
    ladders(b)
    loop_patches(b)
    fittings(b)
    return b
