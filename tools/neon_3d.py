"""The neon light lines as real 3D neon (Justin's review, 24 Oct 2026: "make the lights be 3D modeled ... neon light";
redesign 2, approved with the round inset light). Models and textures for tools/neon_lines.py:

- Neon Line (walls, slab edges; tower/NeonLineBlock): the wall's own block, drawn as its neighbours are (the
  same texture at the same phase, the same shading: nothing painted on it), with a groove 2.4 px deep and 3 px tall
  round each side open to the air, lined in satin aluminium, a round glass neon tube (2 px) in it on clips, glowing.
  Round a convex corner the tube bends; where another line touches it only at a corner (a chamfer one block thick) a
  45 degree piece takes the tube on to it; beside another line it runs on; beside a plain block it ends in a cap.
  Its blockstate is a multipart: the material (by its open sides and phase) and the fitting (lining, tube, clips; by
  its sides and joins), so the fittings are shared by the three materials.
- Neon Floor Line (tower/NeonFloorBlock): the floor's own block with the same profile as a channel in its top, joined
  to the floor lines beside it: straight, round a corner (the tube bends), a tee, a cross, closed at a run's end; on
  its own a short dash.
- Inset Floor Light (tower/InsetFloorLightBlock, new): a brushed steel square trim plate flush with the floor, a round
  frosted lens set a hair down in it, glowing; where the layout had a single floor line on its own.

Geometry: boxes; a face covered by other boxes is left out (so nothing is drawn twice in one plane); the bends' pieces
are turned by multiples of 22.5 degrees, each a hair slimmer than the last so no two faces share a plane.
"""
import itertools
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
TEX = ASSETS / "textures/block"
MODELS = ASSETS / "models/block"

GD = 2.4            # the groove's depth
LT = 0.3            # the lining's thickness
BAND = (6.5, 9.5)   # the wall groove's height
TUBE_D = 2.0        # the tube
CH = (6.5, 9.5)     # the floor channel, across
CH_D = 2.4          # its depth
J = 2.0             # at a diagonal join the tube stops this far from the corner
CAP = 1.2           # at a run's end beside a plain block the cap stops this far from the edge
RB = 1.6            # a bend's radius (the tube's middle)
FLOOR_END = 4.0     # a floor run's closed end: the channel stops this far from the far edge

PHASED = {"concrete": "reinforced_concrete", "steel": "steel_armor_plate", "carbon": "carbon_composite_panel"}
FLAT = {"epoxy": "epoxy_floor", "dock": "dock_plating"}
LINE_MATS = ("concrete", "steel", "carbon")
FLOOR_MATS = ("concrete", "steel", "epoxy", "dock")
SIDES = ("north", "east", "south", "west")          # clockwise seen from above
CORNERS = {"ne": ("north", "east"), "se": ("east", "south"), "sw": ("south", "west"), "nw": ("west", "north")}
SIDE_CODE = {"open": "o", "line": "l", "wall": "w"}


# ------------------------------------------------------------------------------------------------------ textures

def _noise(n, seed, amp, shape=None):
    return np.random.default_rng(seed).normal(0, amp, shape or (n, n))


def textures():
    """{name: RGBA array}: the satin aluminium lining (its back wall warmed by the tube), the amber tube, its cap, the
    clips, the inset light's plate and lens."""
    out = {}
    n = 32
    base = np.zeros((n, n, 4))
    base[..., 3] = 255
    streak = np.repeat(_noise(n, 3, 4, (n, 1)), n, 1)
    for c, v in enumerate((128, 131, 136)):
        base[..., c] = v + streak + _noise(n, 4 + c, 2.0)
    out["neon_track"] = base.copy()
    glow = base.copy()
    for y in range(n):
        t = max(0.0, 1 - abs((y + 0.5) / n - 0.5) / 0.5) ** 2
        glow[y, :, 0] += 60 * t
        glow[y, :, 1] += 18 * t
        glow[y, :, 2] -= 30 * t
    out["neon_track_glow"] = glow
    tube = np.zeros((16, 16, 4))
    tube[..., 3] = 255
    amber, hot = np.array((246, 112, 8.)), np.array((255, 190, 96.))
    for y in range(16):
        t = 1 - abs((y + 0.5) / 16 - 0.5) / 0.5
        tube[y, :, :3] = amber * (1 - t ** 1.6) + hot * t ** 1.6
    out["neon_amber_tube"] = tube
    cap = np.zeros((16, 16, 4))
    cap[..., 3] = 255
    cap[..., :3] = (232, 196, 150)
    out["neon_amber_cap"] = cap
    clip = np.zeros((16, 16, 4))
    clip[..., 3] = 255
    clip[..., :3] = (30, 31, 34)
    out["neon_clip"] = clip
    m = 64
    yy, xx = np.mgrid[0:m, 0:m] + 0.5
    plate = np.zeros((m, m, 4))
    plate[..., 3] = 255
    bs = np.repeat(_noise(m, 9, 6, (1, m)), m, 0)
    for c, v in enumerate((168, 171, 176)):
        plate[..., c] = v + bs + _noise(m, 10 + c, 2)
    for sx, sy in ((0.13, 0.13), (0.87, 0.13), (0.13, 0.87), (0.87, 0.87)):
        d = np.hypot(xx / m - sx, yy / m - sy) * m
        plate[d < 2.6, :3] = (96, 98, 102)
        plate[d < 1.6, :3] = (140, 142, 146)
        plate[(abs(xx / m - sx) * m < 0.5) & (d < 2.2), :3] = (70, 72, 76)
    edge = np.minimum(np.minimum(xx, m - xx), np.minimum(yy, m - yy)) < 1.2
    plate[edge, :3] *= 0.72
    out["inset_light_plate"] = plate
    lens = np.zeros((m, m, 4))
    lens[..., 3] = 255
    r = np.hypot(xx / m - 0.5, yy / m - 0.5) * 2
    warm, white = np.array((255, 170, 70.)), np.array((255, 236, 200.))
    for c in range(3):
        lens[..., c] = white[c] * (1 - r ** 2) + warm[c] * r ** 2
    lens[(r > 0.80) & (r <= 0.88), :3] = (58, 56, 54)          # the gasket
    lens[(r > 0.74) & (r <= 0.80), :3] = (200, 160, 110)
    outside = r > 0.88                                         # the plate's steel round it (drawn full bright: dimmed)
    for c, v in enumerate((150, 153, 158)):
        lens[..., c][outside] = (v + bs[outside] * 0.8) * 0.97
    lens[..., :3] += _noise(m, 21, 3)[..., None] * (r <= 0.74)[..., None]
    out["inset_light_lens"] = lens
    return {k: np.clip(v, 0, 255).astype(np.uint8) for k, v in out.items()}


# ------------------------------------------------------------------------------------------------------ geometry

class Box:
    def __init__(self, f, t, kind, tex, glow=False, rotation=None, faces=None, uvmode="default"):
        self.f, self.t, self.kind, self.tex, self.glow = tuple(f), tuple(t), kind, tex, glow
        self.rotation, self.faces, self.uvmode = rotation, faces, uvmode


OPAQUE = ("mat", "lining", "plate")


def _inside(p, boxes):
    return any(all(b.f[i] - 1e-6 <= p[i] <= b.t[i] + 1e-6 for i in range(3)) for b in boxes)


def _face_points(face, f, t, eps=0.05):
    ax = {"north": 2, "south": 2, "east": 0, "west": 0, "up": 1, "down": 1}[face]
    pos = {"north": f[2] - eps, "south": t[2] + eps, "west": f[0] - eps, "east": t[0] + eps, "down": f[1] - eps, "up": t[1] + eps}[face]
    others = [i for i in range(3) if i != ax]
    pts = []
    for a in np.linspace(f[others[0]] + 0.02, t[others[0]] - 0.02, 6):
        for b in np.linspace(f[others[1]] + 0.02, t[others[1]] - 0.02, 6):
            p = [0.0, 0.0, 0.0]
            p[ax], p[others[0]], p[others[1]] = pos, a, b
            pts.append(p)
    return pts


def default_uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {"north": (16 - x1, 16 - y1, 16 - x0, 16 - y0), "south": (x0, 16 - y1, x1, 16 - y0),
            "east": (16 - z1, 16 - y1, 16 - z0, 16 - y0), "west": (z0, 16 - y1, z1, 16 - y0),
            "up": (x0, z0, x1, z1), "down": (x0, 16 - z1, x1, 16 - z0)}[face]


def _sub(quad, uv):
    u0, v0, u1, v1 = quad
    return [round(u0 + (u1 - u0) * uv[0] / 16, 4), round(v0 + (v1 - v0) * uv[1] / 16, 4),
            round(u0 + (u1 - u0) * uv[2] / 16, 4), round(v0 + (v1 - v0) * uv[3] / 16, 4)]


def _cull(face, f, t):
    v = {"north": f[2] <= 1e-6, "south": t[2] >= 16 - 1e-6, "west": f[0] <= 1e-6, "east": t[0] >= 16 - 1e-6,
         "down": f[1] <= 1e-6, "up": t[1] >= 16 - 1e-6}[face]
    return face if v else None


def elements(boxes, mat_quads, kinds=None):
    """Model elements of the boxes (of the given kinds; all if None): each opaque box's faces not covered by other
    opaque boxes of the same model (a multipart's material and fitting are each whole: no face left out against the
    other's boxes); material faces read the material model's own uv at its phase (mat_quads); the tube's parts glow."""
    opaque = [b for b in boxes if b.kind in OPAQUE and (kinds is None or b.kind in kinds)]
    els = []
    for b in boxes:
        if kinds is not None and b.kind not in kinds:
            continue
        e = {"from": [round(v, 4) for v in b.f], "to": [round(v, 4) for v in b.t], "faces": {}}
        for face in ("north", "south", "east", "west", "up", "down"):
            if b.faces is not None and face not in b.faces:
                continue
            if b.kind in OPAQUE and b.rotation is None:
                pts = _face_points(face, b.f, b.t)
                lo = [min(p[i] for p in pts) for i in range(3)]
                hi = [max(p[i] for p in pts) for i in range(3)]
                others = [o for o in opaque if o is not b and all(o.f[i] <= hi[i] + 1e-6 and o.t[i] >= lo[i] - 1e-6 for i in range(3))]
                if others and all(_inside(p, others) for p in pts):
                    continue
            if b.kind == "mat":
                uv = _sub(mat_quads[face], default_uv(face, b.f, b.t))
            elif b.uvmode == "full":
                uv = [0, 0, 16, 16]
            else:
                uv = [round(v, 4) for v in default_uv(face, b.f, b.t)]
            d = {"texture": b.tex(face) if callable(b.tex) else b.tex, "uv": uv}
            cf = _cull(face, b.f, b.t) if b.rotation is None else None
            if cf:
                d["cullface"] = cf
            e["faces"][face] = d
        if not e["faces"]:
            continue
        if b.glow:
            e["light_emission"] = 15
            e["shade"] = False
        if b.rotation:
            e["rotation"] = b.rotation
        els.append(e)
    return els


def tube_x(x0, x1, yc, zc, d=TUBE_D):
    """A round tube along x (its middle at yc, zc): two crossed boxes, a twelve-sided section."""
    r, k = d / 2, d / 2 * 0.7
    return [Box((x0, yc - k, zc - r), (x1, yc + k, zc + r), "tube", "#tube", glow=True),
            Box((x0, yc - r, zc - k), (x1, yc + r, zc + k), "tube", "#tube", glow=True)]


def tube_z(z0, z1, yc, xc, d=TUBE_D):
    r, k = d / 2, d / 2 * 0.7
    return [Box((xc - r, yc - k, z0), (xc + r, yc + k, z1), "tube", "#tube", glow=True),
            Box((xc - k, yc - r, z0), (xc + k, yc + r, z1), "tube", "#tube", glow=True)]


def _norm180(t):
    while t > 90:
        t -= 180
    while t <= -90:
        t += 180
    return t


def chord(p0, p1, yc, slim=0.03):
    """A straight piece of tube from p0 to p1 ((x, z)), turned about its middle by a multiple of 22.5 degrees
    (Minecraft's turn about y: a positive angle takes +x toward -z); slim: how much thinner than the tube (each piece of
    a bend a little more, so no two of their faces share a plane)."""
    (a0, b0), (a1, b1) = p0, p1
    length = math.hypot(a1 - a0, b1 - b0) + 0.35           # a hair long: the pieces overlap at the knuckles
    ang = math.degrees(math.atan2(b1 - b0, a1 - a0))
    ma, mb = (a0 + a1) / 2, (b0 + b1) / 2
    tx, tz = _norm180(-ang), _norm180(90 - ang)
    along_x = abs(tx) <= 45 + 1e-6
    th = round((tx if along_x else tz) / 22.5) * 22.5
    assert abs(th) <= 45, (ang, th)
    r, k = TUBE_D / 2 - slim, TUBE_D / 2 * 0.7 - slim
    out = []
    for (w, h) in ((r, k), (k, r)):
        if along_x:
            f, t = (ma - length / 2, yc - h, mb - w), (ma + length / 2, yc + h, mb + w)
        else:
            f, t = (ma - w, yc - h, mb - length / 2), (ma + w, yc + h, mb + length / 2)
        rot = {"origin": [round(ma, 4), round(yc, 4), round(mb, 4)], "axis": "y", "angle": th} if th else None
        out.append(Box(f, t, "tube", "#tube", glow=True, rotation=rot))
    return out


def bend(c, radius, a0, a1, yc):
    """The tube bending round centre c ((x, z)) from angle a0 to a1 (degrees, x toward +z), in 45 degree pieces."""
    n = max(1, int(round(abs(a1 - a0) / 45)))
    pts = []
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * i / n)
        pts.append((c[0] + radius * math.cos(a), c[1] + radius * math.sin(a)))
    out = []
    for i, (p, q) in enumerate(zip(pts, pts[1:])):
        out += chord(p, q, yc, slim=0.03 * (i + 1))
    return out


def grid_boxes(cuts, classify, y0, y1):
    """Boxes over a grid of x/z cuts: classify(x, z) -> (kind, texture) or None, for each cell's middle; neighbouring
    cells of one kind merged into rectangles (runs along z, then equal runs along x)."""
    cuts = [c for i, c in enumerate(cuts) if i == 0 or c - cuts[i - 1] > 1e-6]
    nx = len(cuts) - 1
    cls = [[classify((cuts[i] + cuts[i + 1]) / 2, (cuts[j] + cuts[j + 1]) / 2) for j in range(nx)] for i in range(nx)]
    runs = []                                   # (i, j0, j1, kind) per row
    for i in range(nx):
        j = 0
        while j < nx:
            k = cls[i][j]
            j1 = j
            while j1 + 1 < nx and cls[i][j1 + 1] == k:
                j1 += 1
            if k:
                runs.append([i, i, j, j1, k])
            j = j1 + 1
    merged = []
    for r in runs:
        for m in merged:
            if m[1] == r[0] - 1 and m[2] == r[2] and m[3] == r[3] and m[4] == r[4]:
                m[1] = r[0]
                break
        else:
            merged.append(r)
    return [Box((cuts[i0], y0, cuts[j0]), (cuts[i1 + 1], y1, cuts[j1 + 1]), k[0], k[1]) for i0, i1, j0, j1, k in merged]


def _lining_tex(face):
    return "#glow" if face in ("north", "south", "east", "west") else "#track"


# ------------------------------------------------------------------------------------------------------ the line

def line_boxes(sides, joins=()):
    """The wall line. sides: {side: "open" | "line" | "wall"}; joins: corners (ne, se, sw, nw) where another line touches
    it only diagonally (both sides there open)."""
    y0, y1 = BAND
    boxes = [Box((0, 0, 0), (16, y0, 16), "mat", "#base"), Box((0, y1, 0), (16, 16, 16), "mat", "#base")]
    o = {k for k, v in sides.items() if v == "open"}
    joins = {c for c in joins if set(CORNERS[c]) <= o}

    def groove(x, z):
        return ("north" in o and z < GD) or ("south" in o and z > 16 - GD) or ("west" in o and x < GD) or ("east" in o and x > 16 - GD)

    def lining(x, z):
        return ("north" in o and z < GD + LT) or ("south" in o and z > 16 - GD - LT) or ("west" in o and x < GD + LT) or \
            ("east" in o and x > 16 - GD - LT)
    cuts = sorted({0, GD, GD + LT, 16 - GD - LT, 16 - GD, 16})
    boxes += grid_boxes(cuts, lambda x, z: None if groove(x, z) else (("lining", _lining_tex) if lining(x, z) else ("mat", "#base")), y0, y1)
    for g in grid_boxes(cuts, lambda x, z: ("lining", "#track") if groove(x, z) else None, 0, 1):
        boxes.append(Box((g.f[0], y0, g.f[2]), (g.t[0], y0 + LT, g.t[2]), "lining", "#track"))
        boxes.append(Box((g.f[0], y1 - LT, g.f[2]), (g.t[0], y1, g.t[2]), "lining", "#track"))
    yc = (y0 + y1) / 2
    off = (GD - LT) / 2
    corner_of = {("north", "east"): "ne", ("east", "south"): "se", ("south", "west"): "sw", ("west", "north"): "nw"}
    k = 0.75
    for i, s in enumerate(SIDES):
        if s not in o:
            continue
        prev, nxt = SIDES[i - 1], SIDES[(i + 1) % 4]
        ends = []
        for lat, c in ((prev, corner_of[(prev, s)]), (nxt, corner_of[(s, nxt)])):
            if lat in o:
                ends.append(("join", J) if c in joins else ("bend", off + RB))
            elif sides.get(lat) == "line":
                ends.append(("run", 0.0))
            else:
                ends.append(("cap", CAP + 0.8))
        a, b = ends[0][1], 16 - ends[1][1]
        if s == "north":
            boxes += tube_x(a, b, yc, off)
        elif s == "east":
            boxes += tube_z(a, b, yc, 16 - off)
        elif s == "south":
            boxes += tube_x(16 - b, 16 - a, yc, 16 - off)
        else:
            boxes += tube_z(16 - b, 16 - a, yc, off)
        for (kind, _), at_start in ((ends[0], True), (ends[1], False)):
            if kind != "cap":
                continue
            p0, p1 = (CAP, CAP + 0.8) if at_start else (16 - CAP - 0.8, 16 - CAP)
            if s == "north":
                boxes.append(Box((p0, yc - k, off - k), (p1, yc + k, off + k), "tube", "#cap", glow=True))
            elif s == "east":
                boxes.append(Box((16 - off - k, yc - k, p0), (16 - off + k, yc + k, p1), "tube", "#cap", glow=True))
            elif s == "south":
                boxes.append(Box((16 - p1, yc - k, 16 - off - k), (16 - p0, yc + k, 16 - off + k), "tube", "#cap", glow=True))
            else:
                boxes.append(Box((off - k, yc - k, 16 - p1), (off + k, yc + k, 16 - p0), "tube", "#cap", glow=True))
        if nxt in o and corner_of[(s, nxt)] not in joins:
            cx = {"north": 16 - off - RB, "east": 16 - off - RB, "south": off + RB, "west": off + RB}[s]
            cz = {"north": off + RB, "east": 16 - off - RB, "south": 16 - off - RB, "west": off + RB}[s]
            a0 = {"north": -90, "east": 0, "south": 90, "west": 180}[s]
            boxes += bend((cx, cz), RB, a0, a0 + 90, yc)
    # the 45 degree pieces across a diagonal join south of this block (the block north of a join draws them)
    if "sw" in joins:
        boxes += chord((off, 16 - J), (-J, 16 + off), yc) + chord((J, 16 - off), (-off, 16 + J), yc, slim=0.06)
    if "se" in joins:
        boxes += chord((16 - off, 16 - J), (16 + J, 16 + off), yc) + chord((16 - J, 16 - off), (16 + off, 16 + J), yc, slim=0.06)
    for s in o:
        for p in (4.0, 12.0):
            if s == "north":
                boxes.append(Box((p - 0.4, y0 + LT, GD - LT - 0.5), (p + 0.4, y1 - LT, GD - LT), "clip", "#clip"))
            elif s == "south":
                boxes.append(Box((p - 0.4, y0 + LT, 16 - GD + LT), (p + 0.4, y1 - LT, 16 - GD + LT + 0.5), "clip", "#clip"))
            elif s == "west":
                boxes.append(Box((GD - LT - 0.5, y0 + LT, p - 0.4), (GD - LT, y1 - LT, p + 0.4), "clip", "#clip"))
            else:
                boxes.append(Box((16 - GD + LT, y0 + LT, p - 0.4), (16 - GD + LT + 0.5, y1 - LT, p + 0.4), "clip", "#clip"))
    return boxes


def open_code(sides):
    return "".join("o" if sides[s] == "open" else "x" for s in SIDES)


def fit_code(sides, joins):
    j = sorted(c for c in joins if set(CORNERS[c]) <= {k for k, v in sides.items() if v == "open"})
    return "".join(SIDE_CODE[sides[s]] for s in SIDES) + ("_" + "".join(j) if j else "")


def line_states():
    """Every (sides, joins) a line can be in with an open side: each side open, line or wall; a join only at a corner
    whose two sides are open."""
    for vals in itertools.product(("open", "line", "wall"), repeat=4):
        sides = dict(zip(SIDES, vals))
        if "open" not in vals:
            continue
        both = [c for c, (a, b) in CORNERS.items() if sides[a] == "open" and sides[b] == "open"]
        for n in range(len(both) + 1):
            for js in itertools.combinations(both, n):
                yield sides, set(js)


# ------------------------------------------------------------------------------------------------------ the floor line

def floor_boxes(conn):
    """The floor line: the floor block with the lined channel in its top joining the connected sides, the tube in it."""
    top, yb = 16.0, 16.0 - CH_D
    c = set(conn)
    lo, hi = CH
    mid = (lo + hi) / 2
    end = FLOOR_END

    def chan(x, z):
        inx, inz = lo <= x <= hi, lo <= z <= hi
        if not c:                                    # a dash: along x, closed at both ends
            return inz and end <= x <= 16 - end
        if inx and inz:
            return True
        if inx and z < lo and ("north" in c or (c == {"south"} and z > end)):
            return True
        if inx and z > hi and ("south" in c or (c == {"north"} and z < 16 - end)):
            return True
        if inz and x < lo and ("west" in c or (c == {"east"} and x > end)):
            return True
        if inz and x > hi and ("east" in c or (c == {"west"} and x < 16 - end)):
            return True
        return False

    def lin(x, z):
        return any(chan(x + dx * LT * 1.01, z + dz * LT * 1.01) for dx in (-1, 0, 1) for dz in (-1, 0, 1))
    cuts = sorted({0, 16, lo - LT, lo, hi, hi + LT, end, end + LT, 16 - end - LT, 16 - end, end - LT, 16 - end + LT})
    boxes = [Box((0, 0, 0), (16, yb - LT, 16), "mat", "#base")]
    boxes += grid_boxes(cuts, lambda x, z: ("lining", "#track") if chan(x, z) else ("mat", "#base"), yb - LT, yb)
    boxes += grid_boxes(cuts, lambda x, z: None if chan(x, z) else (("lining", _lining_tex) if lin(x, z) else ("mat", "#base")), yb, top)
    yc = yb + 0.25 + TUBE_D / 2
    r = TUBE_D / 2
    stop = end + LT + 0.9
    kc = 0.7

    def cap_z(z0, z1):
        return Box((mid - kc, yc - kc, z0), (mid + kc, yc + kc, z1), "tube", "#cap", glow=True)

    def cap_x(x0, x1):
        return Box((x0, yc - kc, mid - kc), (x1, yc + kc, mid + kc), "tube", "#cap", glow=True)
    arms = [s for s in SIDES if s in c]
    if not arms:
        boxes += tube_x(stop, 16 - stop, yc, mid) + [cap_x(stop - 0.8, stop), cap_x(16 - stop, 16 - stop + 0.8)]
    elif len(arms) == 1:
        s = arms[0]
        if s == "north":
            boxes += tube_z(0, 16 - stop, yc, mid) + [cap_z(16 - stop, 16 - stop + 0.8)]
        elif s == "south":
            boxes += tube_z(stop, 16, yc, mid) + [cap_z(stop - 0.8, stop)]
        elif s == "west":
            boxes += tube_x(0, 16 - stop, yc, mid) + [cap_x(16 - stop, 16 - stop + 0.8)]
        else:
            boxes += tube_x(stop, 16, yc, mid) + [cap_x(stop - 0.8, stop)]
    elif len(arms) == 2 and c not in ({"north", "south"}, {"east", "west"}):
        sx = 1 if "east" in c else -1
        sz = 1 if "south" in c else -1
        cx, cz = mid + sx * RB, mid + sz * RB
        boxes += tube_z(0, cz, yc, mid) if sz < 0 else tube_z(cz, 16, yc, mid)
        boxes += tube_x(cx, 16, yc, mid) if sx > 0 else tube_x(0, cx, yc, mid)
        a0 = 180 if sx > 0 else 0
        a1 = -90 if sz > 0 else 90
        if a1 - a0 < -180:
            a1 += 360
        if a1 - a0 > 180:
            a1 -= 360
        boxes += bend((cx, cz), RB, a0, a1, yc)
    else:
        # straight through, a tee, a cross: the axis with both arms whole, the others from its side out (never overlapping)
        through_ns = {"north", "south"} <= c
        if through_ns:
            boxes += tube_z(0, 16, yc, mid)
            if "west" in c:
                boxes += tube_x(0, mid - r, yc, mid)
            if "east" in c:
                boxes += tube_x(mid + r, 16, yc, mid)
        else:
            boxes += tube_x(0, 16, yc, mid)
            if "north" in c:
                boxes += tube_z(0, mid - r, yc, mid)
            if "south" in c:
                boxes += tube_z(mid + r, 16, yc, mid)
    for s in arms:
        p = {"north": 3.0, "south": 13.0, "west": 3.0, "east": 13.0}[s]
        if s in ("north", "south"):
            boxes.append(Box((lo, yb, p - 0.4), (hi, yb + 0.5, p + 0.4), "clip", "#clip"))
        else:
            boxes.append(Box((p - 0.4, yb, lo), (p + 0.4, yb + 0.5, hi), "clip", "#clip"))
    return boxes


def floor_code(conn):
    return "".join(s[0] if s in conn else "x" for s in SIDES)


# ------------------------------------------------------------------------------------------------------ the inset light

def inset_boxes():
    """The inset floor light: the floor block, a square brushed steel trim plate flush with its top, the lens a hair
    down in the plate's opening."""
    p0, p1 = 3.0, 13.0
    h0, h1 = 4.6, 11.4
    boxes = [Box((0, 0, 0), (16, 15.0, 16), "mat", "#base")]
    boxes += grid_boxes([0, p0, p1, 16], lambda x, z: None if (p0 < x < p1 and p0 < z < p1) else ("mat", "#base"), 15.0, 16.0)
    boxes += grid_boxes([p0, h0, h1, p1], lambda x, z: None if (h0 < x < h1 and h0 < z < h1) else ("plate", "#plate"), 15.0, 16.0)
    boxes.append(Box((h0, 15.0, h0), (h1, 15.6, h1), "tube", "#lens", glow=True, uvmode="full", faces=["up"]))
    return boxes


# ------------------------------------------------------------------------------------------------------ writing

def phase_uv(material, phase):
    if material in PHASED and phase is not None:
        m = json.loads((MODELS / f"{PHASED[material]}_phase_{phase}.json").read_text())
        return {f: v["uv"] for f, v in m["elements"][0]["faces"].items()}
    return {f: (0, 0, 16, 16) for f in ("north", "south", "east", "west", "up", "down")}


def base_texture(material):
    return f"jugcraft:block/{PHASED.get(material) or FLAT[material]}"


FIT_TEX = {"track": "jugcraft:block/neon_track", "glow": "jugcraft:block/neon_track_glow", "tube": "jugcraft:block/neon_amber_tube",
           "cap": "jugcraft:block/neon_amber_cap", "clip": "jugcraft:block/neon_clip"}


def _model(elements_, textures_):
    return {"parent": "minecraft:block/block", "textures": textures_, "elements": elements_}


def write(write_json):
    """Writes the textures, models, blockstates and item models; write_json(path, obj) as tools/neon_lines._json.
    Returns how many models."""
    TEX.mkdir(parents=True, exist_ok=True)
    for name, arr in textures().items():
        Image.fromarray(arr).save(TEX / f"{name}.png", optimize=True)
    n = 0
    # the wall lines: the material by open sides and phase, the fitting by sides and joins (shared by the materials)
    fits = {}
    for sides, joins in line_states():
        fits[fit_code(sides, joins)] = (sides, joins)
    for code, (sides, joins) in fits.items():
        write_json(MODELS / f"neon_line_fit_{code}.json", _model(elements(line_boxes(sides, joins), phase_uv("concrete", 0),
                                                                          kinds=("lining", "tube", "clip")), {"particle": FIT_TEX["track"], **FIT_TEX}))
        n += 1
    masks = sorted({open_code(s) for s, _ in fits.values()})
    for mat in LINE_MATS:
        parts = []
        for ph in range(8):
            q = phase_uv(mat, ph)
            closed = f"neon_line_{mat}_closed_p{ph}"
            write_json(MODELS / f"{closed}.json", _model(elements([Box((0, 0, 0), (16, 16, 16), "mat", "#base")], q),
                                                        {"particle": base_texture(mat), "base": base_texture(mat)}))
            n += 1
            parts.append({"when": {**{s: "line|wall" for s in SIDES}, "phase": str(ph)}, "apply": {"model": f"jugcraft:block/{closed}"}})
            for mask in masks:
                sides = {s: ("open" if ch == "o" else "wall") for s, ch in zip(SIDES, mask)}
                name = f"neon_line_{mat}_{mask}_p{ph}"
                write_json(MODELS / f"{name}.json", _model(elements(line_boxes(sides), q, kinds=("mat",)),
                                                          {"particle": base_texture(mat), "base": base_texture(mat)}))
                n += 1
                parts.append({"when": {**{s: ("open" if ch == "o" else "line|wall") for s, ch in zip(SIDES, mask)}, "phase": str(ph)},
                              "apply": {"model": f"jugcraft:block/{name}"}})
        for code, (sides, joins) in sorted(fits.items()):
            when = {s: sides[s] for s in SIDES}
            for c, (a, b) in CORNERS.items():
                if sides[a] == "open" and sides[b] == "open":
                    when[c] = "true" if c in joins else "false"
            parts.append({"when": when, "apply": {"model": f"jugcraft:block/neon_line_fit_{code}"}})
        write_json(ASSETS / "blockstates" / f"neon_line_{mat}.json", {"multipart": parts})
        # the item: a length of line, its front open
        sides = {"north": "open", "east": "line", "south": "wall", "west": "line"}
        boxes = line_boxes(sides)
        write_json(MODELS / f"neon_line_{mat}_item.json", _model(elements(boxes, phase_uv(mat, 0)),
                                                                {"particle": base_texture(mat), "base": base_texture(mat), **FIT_TEX}))
        write_json(ASSETS / "items" / f"neon_line_{mat}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:block/neon_line_{mat}_item"}})
        n += 1
    # the floor lines
    for mat in FLOOR_MATS:
        variants = {}
        for bits in itertools.product((False, True), repeat=4):
            conn = {s for s, bit in zip(SIDES, bits) if bit}
            for ph in (range(8) if mat in PHASED else [None]):
                name = f"neon_floor_{mat}_{floor_code(conn)}" + (f"_p{ph}" if ph is not None else "")
                write_json(MODELS / f"{name}.json", _model(elements(floor_boxes(conn), phase_uv(mat, ph)),
                                                          {"particle": base_texture(mat), "base": base_texture(mat), **FIT_TEX}))
                n += 1
                key = ",".join(f"{s}={'true' if s in conn else 'false'}" for s in ("east", "north", "south", "west"))
                if ph is not None:
                    key += f",phase={ph}"
                variants[key] = {"model": f"jugcraft:block/{name}"}
        write_json(ASSETS / "blockstates" / f"neon_floor_{mat}.json", {"variants": variants})
        first = f"neon_floor_{mat}_xexw" + ("_p0" if mat in PHASED else "")
        write_json(ASSETS / "items" / f"neon_floor_{mat}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:block/{first}"}})
    # the inset floor lights
    for mat in FLOOR_MATS:
        variants = {}
        for ph in (range(8) if mat in PHASED else [None]):
            name = f"inset_light_{mat}" + (f"_p{ph}" if ph is not None else "")
            write_json(MODELS / f"{name}.json", _model(elements(inset_boxes(), phase_uv(mat, ph)), {
                "particle": base_texture(mat), "base": base_texture(mat), "plate": "jugcraft:block/inset_light_plate",
                "lens": "jugcraft:block/inset_light_lens"}))
            n += 1
            variants[f"phase={ph}" if ph is not None else ""] = {"model": f"jugcraft:block/{name}"}
        write_json(ASSETS / "blockstates" / f"inset_light_{mat}.json", {"variants": variants})
        first = f"inset_light_{mat}" + ("_p0" if mat in PHASED else "")
        write_json(ASSETS / "items" / f"inset_light_{mat}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:block/{first}"}})
    return n
