"""Faces of the drop ship that would flicker against each other (z-fighting: Justin, 10 November 2026, of the striped
bar under the bay's seats; his standing rule: "make sure textures can't z-fight").

Two faces drawn in one plane, turned the same way, one over the other, show whichever the graphics card happens to put
in front, pixel by pixel, and it changes as you move: a jagged, crawling edge. `fights(C, ship)` finds every such pair
in the ship as the game draws it (what `hidden_faces` leaves in), in one part or in parts that stand still together:
same plane within NEAR, the same way round, overlapping by more than SLIVER, and not showing the same thing there (two
faces of one material laid flat on their plane at its true size show the same texels, so nothing flickers).

`python3 tools/crew_ship_planes.py` lists them by material and place; `check(err)` (tools/check_mod_data.py) fails on
any.
"""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path

import numpy as np

NEAR = 0.004                     # blocks: planes closer than this fight (the depth buffer can't tell them apart from afar)
SLIVER = 0.0004                  # square blocks of overlap that don't show (4 square centimetres)


def _clip(poly, a, b):
    """poly (2D, any winding) cut to the left of a -> b."""
    out = []
    n = len(poly)
    for i in range(n):
        p, q = poly[i], poly[(i + 1) % n]
        sp = (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
        sq = (b[0] - a[0]) * (q[1] - a[1]) - (b[1] - a[1]) * (q[0] - a[0])
        if sp >= 0:
            out.append(p)
        if (sp >= 0) != (sq >= 0):
            t = sp / (sp - sq)
            out.append(p + (q - p) * t)
    return out


def _area(poly):
    return 0.0 if len(poly) < 3 else abs(sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1]
                                             for i in range(len(poly)))) / 2


def _poly(P2):
    out = [P2[0]]
    for p in P2[1:]:
        if np.linalg.norm(p - out[-1]) > 1e-9:
            out.append(p)
    if len(out) > 2 and np.linalg.norm(out[0] - out[-1]) < 1e-9:
        out.pop()
    if len(out) >= 3 and sum(out[i][0] * out[(i + 1) % len(out)][1] - out[(i + 1) % len(out)][0] * out[i][1] for i in range(len(out))) < 0:
        out = out[::-1]
    return out


def fights(C, ship):
    """[(overlap in square blocks, where, (material, part) of each, the two faces' corners)], the largest first."""
    faces, tags, where = [], [], []
    for qi, (f, tag) in enumerate(zip(ship.q, ship.q.tags)):
        if not isinstance(f[0], str):
            faces.append(f)
            tags.append(tag)
            where.append(qi)
    names = [C._name_of(f[2]) for f in faces]
    group = ["static" if t in ("hull", "interior") else t for t in tags]
    gone = C.hidden_faces(faces, names, group)
    by_dir = defaultdict(list)
    info = {}
    for i, f in enumerate(faces):
        if i in gone:
            continue
        P = np.asarray(f[0], float)
        n = C.face_normal(P)
        if not np.isfinite(n).all() or abs(np.linalg.norm(n) - 1) > 1e-6:
            continue
        info[i] = (P, n, float(n @ P.mean(0)))
        by_dir[(tuple(np.round(n, 2)), group[i])].append(i)
    out = []
    for (nk, g), idx in by_dir.items():
        idx.sort(key=lambda i: info[i][2])
        for a in range(len(idx)):
            i = idx[a]
            Pi, ni, di = info[i]
            for b in range(a + 1, len(idx)):
                j = idx[b]
                Pj, nj, dj = info[j]
                if dj - di > NEAR + 0.02:                  # (sorted by the planes' places: nothing further on can be near)
                    break
                if ni @ nj < 0.9995 or np.abs((Pj - Pi[0]) @ ni).max() > NEAR:
                    continue
                if (Pi.max(0) < Pj.min(0) - 1e-6).any() or (Pj.max(0) < Pi.min(0) - 1e-6).any():
                    continue
                if names[i] == names[j] and not C.local_uv(names[i]) and not faces[i][3] and not faces[j][3]:
                    continue                                # one material laid flat at its true size: the same texels
                e1 = np.cross(ni, [0, 1.0, 0]) if abs(ni[1]) < 0.9 else np.cross(ni, [1.0, 0, 0])
                e1 /= np.linalg.norm(e1)
                e2 = np.cross(ni, e1)
                A = _poly([np.array([p @ e1, p @ e2]) for p in Pi])
                B = _poly([np.array([p @ e1, p @ e2]) for p in Pj])
                if len(A) < 3 or len(B) < 3:
                    continue
                cut = A
                for k in range(len(B)):
                    cut = _clip(cut, B[k], B[(k + 1) % len(B)])
                    if len(cut) < 3:
                        break
                ar = _area(cut)
                if ar > SLIVER:
                    mid = np.mean([c[0] * e1 + c[1] * e2 for c in cut], 0) + ni * di
                    out.append((ar, mid, (names[i], tags[i]), (names[j], tags[j]), Pi, Pj, where[i], where[j]))
    out.sort(key=lambda k: -k[0])
    return out


LIFT = 0.008                     # blocks: how far a face is stood off the one it lay in
_DONE = {}


def separate(C, ship, report=None):
    """Stands every face that lies in another's plane off it (Ship.build, before the paint is laid): of each pair the
    smaller one, the fitting on the surface, moves LIFT out along the way it faces; run again until none are left
    (a face stood off one plane may have come to lie in another). What is keyed by a face's corners (the shading's lift,
    a cut piece's parent, the paint's petals) follows it. The same ship is not worked out twice."""
    key = hash(b"".join(np.round(np.asarray(f[0], float), 4).tobytes() for f in ship.q if not isinstance(f[0], str)))
    if key not in _DONE:
        moved = {}
        for _ in range(6):
            found = fights(C, ship)
            if not found:
                break
            step = {}
            for ar, mid, a, b, Pi, Pj, qi, qj in found:
                small = qi if _area(_flat(Pi)) <= _area(_flat(Pj)) else qj
                step[small] = C.face_normal(np.asarray(ship.q[small][0], float)) * LIFT
            for q, d in step.items():
                _move(C, ship, q, d)
                moved[q] = moved.get(q, 0) + d
        _DONE[key] = moved
        if report is not None:
            report(f"planes: {len(moved)} faces stood off the faces they lay in; {len(fights(C, ship))} left")
        return len(moved)
    for q, d in _DONE[key].items():
        _move(C, ship, q, d)
    return len(_DONE[key])


def _flat(P):
    n = np.cross(P[1] - P[0], P[2] - P[0])
    if np.linalg.norm(n) < 1e-12:
        n = np.cross(P[2] - P[0], P[3] - P[0])
    n = n / (np.linalg.norm(n) + 1e-12)
    e1 = np.cross(n, [0, 1.0, 0]) if abs(n[1]) < 0.9 else np.cross(n, [1.0, 0, 0])
    e1 /= np.linalg.norm(e1)
    e2 = np.cross(n, e1)
    return _poly([np.array([p @ e1, p @ e2]) for p in P])


def _move(C, ship, q, d):
    f = ship.q[q]
    P = np.asarray(f[0], float)
    old, new = C.face_key(P), C.face_key(P + d)
    for reg in (C.LIFT, C.PARENT_OF, C.PETALS):
        if old in reg:
            reg[new] = reg[old]
    ship.q[q] = (P + d,) + tuple(f[1:])


def check(err):
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import crew_ship as CS
    found = fights(CS, CS.Ship(ramp_down=0.0, gear=1.0, side_doors=0.0))
    if found:
        worst = ", ".join(f"{k[2][0]} on {k[3][0]} at {np.round(k[1], 2).tolist()}" for k in found[:5])
        err(f"crew_ship_planes: {len(found)} pairs of the drop ship's faces lie in one plane and would flicker: {worst}")


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import crew_ship as CS
    for pose in (dict(ramp_down=0.0, gear=1.0, side_doors=0.0), dict(ramp_down=1.0, gear=1.0, side_doors=1.0)):
        found = fights(CS, CS.Ship(**pose))
        print(pose, ":", len(found), "pairs,", round(sum(f[0] for f in found), 2), "square blocks")
        by = defaultdict(lambda: [0, 0.0, None])
        for ar, mid, a, b, *_ in found:
            k = tuple(sorted((a, b)))
            by[k][0] += 1
            by[k][1] += ar
            by[k][2] = mid if by[k][2] is None else by[k][2]
        for k, (n, ar, mid) in sorted(by.items(), key=lambda kv: -kv[1][1])[:40]:
            print(f"  {n:4d} pairs {ar:7.3f} sq  {k[0][0]}/{k[0][1]} on {k[1][0]}/{k[1][1]}  e.g. at {np.round(mid, 2).tolist()}")
