"""Z-fighting in block models (Justin: "make sure textures can't z-fight"): finds faces of different elements that
face the same way in the same plane and overlap, and pulls one of each pair a hair (0.02 model px) off the plane, so
the game never draws two faces in the same place.

    model = zfight.fix(model)      # a model dict with "elements"
    zfight.overlaps(elements)      # [(area, i, face, j, face)] left

Element rotations (any axis and angle, rescale) are taken into account. Used on the plant kit's, the storage tank's
and the radwaste storage's models as they are written (tools/plant_interior.py, storage_tank.py, nuclear_storage.py).
"""
import json
import math

import numpy as np

EPS = 0.02
_OUT = {"north": (2, 0, -1), "south": (2, 1, 1), "west": (0, 0, -1), "east": (0, 1, 1), "down": (1, 0, -1), "up": (1, 1, 1)}


def _corners(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[face]


def _rot(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def _faces(elements):
    out = []
    for i, e in enumerate(elements):
        f, t = e["from"], e["to"]
        rot = e.get("rotation")
        for face in e.get("faces", {}):
            pts = np.array(_corners(face, f, t), float)
            if rot:
                o = np.array(rot.get("origin", [8, 8, 8]), float)
                if "axis" in rot:
                    M = _rot(rot["axis"], rot["angle"])
                    if rot.get("rescale"):
                        k = 1 / math.cos(math.radians(rot["angle"]))
                        sc = np.ones(3) * k
                        sc["xyz".index(rot["axis"])] = 1
                        M = M @ np.diag(sc)
                else:
                    M = _rot("x", rot.get("x", 0)) @ _rot("y", rot.get("y", 0)) @ _rot("z", rot.get("z", 0))
                pts = (pts - o) @ M.T + o
            n = np.cross(pts[3] - pts[0], pts[1] - pts[0])
            ln = np.linalg.norm(n)
            if ln < 1e-9:
                continue
            n /= ln
            out.append((i, face, pts, n, float(n @ pts[0])))
    return out


def _clip(subject, a, b):
    def inside(p):
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -1e-9

    def cut(p, q):
        dx, dy = q[0] - p[0], q[1] - p[1]
        ex, ey = b[0] - a[0], b[1] - a[1]
        den = ex * dy - ey * dx
        if abs(den) < 1e-12:
            return q
        s = (ex * (a[1] - p[1]) - ey * (a[0] - p[0])) / den
        return (p[0] + s * dx, p[1] + s * dy)
    out = []
    for k in range(len(subject)):
        p, q = subject[k - 1], subject[k]
        if inside(q):
            if not inside(p):
                out.append(cut(p, q))
            out.append(q)
        elif inside(p):
            out.append(cut(p, q))
    return out


def _area(poly):
    return abs(sum(poly[k - 1][0] * poly[k][1] - poly[k][0] * poly[k - 1][1] for k in range(len(poly)))) / 2


def _ccw(poly):
    if sum(poly[k - 1][0] * poly[k][1] - poly[k][0] * poly[k - 1][1] for k in range(len(poly))) < 0:
        poly = poly[::-1]
    return poly


def overlaps(elements, tol=1e-3):
    """[(area, i, face_i, j, face_j)]: same-direction coplanar overlapping faces of different elements."""
    fs = _faces(elements)
    hits = []
    for a in range(len(fs)):
        ia, fa, pa, na, da = fs[a]
        for b in range(a + 1, len(fs)):
            ib, fb, pb, nb, db = fs[b]
            if ia == ib or na @ nb < 0.9999 or abs(da - db) > tol:
                continue
            u = np.cross(na, [0, 1, 0] if abs(na[1]) < 0.9 else [1, 0, 0])
            u /= np.linalg.norm(u)
            v = np.cross(na, u)
            A = _ccw([(float(p @ u), float(p @ v)) for p in pa])
            B = _ccw([(float(p @ u), float(p @ v)) for p in pb])
            inter = A
            for k in range(len(B)):
                inter = _clip(inter, B[k - 1], B[k])
                if not inter:
                    break
            if inter and _area(inter) > 1e-4:
                hits.append((_area(inter), ia, fa, ib, fb))
    return hits


def nudge(e, face, eps=EPS):
    """Moves one face of an element off its plane by eps: outwards, unless that would take it past the block's edge
    (where it would lie on a neighbouring block's faces), then inwards (the element a hair thinner)."""
    axis, end, sign = _OUT[face]
    key = "to" if end else "from"
    v = e[key][axis]
    out = round(v + sign * eps, 4)
    other = e["from" if end else "to"][axis]
    inside = 0 <= min(e["from"][axis], e["to"][axis]) and max(e["from"][axis], e["to"][axis]) <= 16
    if inside and not 0 <= out <= 16 and abs(v - other) > 2 * eps:
        out = round(v - sign * eps, 4)
    e[key][axis] = out


def fix(model, passes=24):
    """The model with every z-fighting pair separated (one face of each pulled off the plane): a face not moved before
    goes first, and a face moved again goes further each time, so any number of faces in one plane end up apart."""
    els = model.get("elements")
    if not els:
        return model
    els = json.loads(json.dumps(els))
    count = {}
    for _ in range(passes):
        hits = overlaps(els)
        if not hits:
            break
        moved = set()
        for area, i, fi, j, fj in hits:
            if (j, fj) in moved or (i, fi) in moved:
                continue
            if (j, fj) in count and (i, fi) not in count:
                i, fi, j, fj = j, fj, i, fi
            count[(j, fj)] = count.get((j, fj), 0) + 1
            nudge(els[j], fj, EPS * count[(j, fj)])
            moved.add((j, fj))
    out = dict(model)
    out["elements"] = els
    return out


def unfighting(write):
    """A generator's write(path, obj) that fixes block and item models with elements on the way."""
    def w(path, obj):
        if isinstance(obj, dict) and obj.get("elements") and "models" in str(path):
            obj = fix(obj)
        return write(path, obj)
    return w
