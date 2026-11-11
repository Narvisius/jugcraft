"""Where the drop ship's paint doesn't run on, or isn't at its true size (Justin, 10 November 2026: "Do a pass over the
hull on the ship. There's still issues with the texture, like here at the wing on top").

The hull's three paints share one panel layout (tools/crew_ship.py: mat), 64 texels to the block. Every part of the
outside wears it, each laid its own way: the skin round the hull, fairings on their own planes, parts that turn where
they stand at rest. This looks at the ship as the game draws it (each quad as its two triangles, the texture as each
triangle carries it) and finds, for all of it at once:

- **size**: a triangle whose panels are not at the paint's size (off by more than SIZE_TOL) or are stretched (longer
  one way than the other by more than STRETCH_TOL);
- **breaks**: two triangles of different faces that meet along a line and lie within ANGLE of each other (one surface
  to the eye) where the panels of one don't carry on into the other: the pattern jumps by more than BREAK_TEXELS.

`survey(ship)` returns both lists; `python3 tools/crew_ship_seams.py` prints them by the part of the generator that
made the faces; `check(err)` (run by tools/check_mod_data.py) fails on any size fault and on more broken length than
ALLOWED_BREAKS (what is left by design: see there).
"""
from __future__ import annotations

import math
import sys
from collections import defaultdict
from pathlib import Path

import numpy as np

PAINTS = {"skin", "skin2", "skinD", "hull", "hull2", "hullD", "fillet", "fillet2", "filletD", "hullL", "hullDL", "hull2C"}
TEXELS = 512                     # the paint's texture across
PER_BLOCK = 64.0                 # its texels to the block
SIZE_TOL = 0.06                  # how far off its true size before it shows
STRETCH_TOL = 0.06
ANGLE = 50.0                     # degrees: faces further apart than this meet at a real corner, where a seam belongs
BREAK_TEXELS = 4.0               # 4 texels: 6 cm
TOUCH = 0.012                    # how near a point must lie to a triangle to be on it
# the generator's tools, not parts: left out of a face's maker
UTIL = {"append", "extend", "__iadd__", "<module>", "__init__", "build", "<lambda>", "face", "B", "facet", "rod", "quad",
        "decal", "brick", "bevel_box", "_bevel_faces", "extrude", "frustum", "disc", "prism", "bar", "box_faces", "raised",
        "oriented_box", "girder", "pocketed_face", "fixture", "lamp_in", "<listcomp>", "<genexpr>", "app"}


def traced(C, **kw):
    """A Ship whose every quad remembers the generator function that made it (the outermost of the Ship's own)."""
    QL = C.QList
    orig = QL.append
    made = []

    def app(self, x):
        f = sys._getframe(1)
        names = []
        while f is not None:
            if f.f_code.co_filename == C.__file__:
                names.append(f.f_code.co_name)
            f = f.f_back
        names = [n for n in names if n not in UTIL]
        made.append(">".join(reversed(names[-2:])) if names else "?")
        return orig(self, x)
    QL.append = app
    try:
        ship = C.Ship(**kw)
    finally:
        QL.append = orig
    ship.made = made
    return ship


def triangles(C, ship, tag="hull"):
    """The paint of the ship's `tag` part (None: of everything) as the game draws it: (corners 3x3, uv 3x2, normal,
    material, maker, face index)."""
    out = []
    for i, f in enumerate(ship.q):
        if isinstance(f[0], str) or (tag is not None and ship.q.tags[i] != tag):
            continue
        name = C._name_of(f[2])
        if name not in PAINTS:
            continue
        P = np.asarray(f[0], float)
        n = C.face_normal(P)
        UV = np.asarray(f[1], float) if C.local_uv(name) else C.world_uv(P, name, n)
        for a, b, c in ((0, 1, 3), (1, 2, 3)):               # the game's two triangles of a quad (its diagonal: 1-3)
            T = P[[a, b, c]]
            nn = np.cross(T[1] - T[0], T[2] - T[0])
            area = np.linalg.norm(nn) / 2
            if area < 1e-4 or min(np.linalg.norm(T[i] - T[j]) for i, j in ((0, 1), (1, 2), (2, 0))) < 2e-3:
                continue                                    # (a sliver: nothing of the paint shows on it)
            out.append((T, UV[[a, b, c]], -nn / (2 * area), name, ship.made[i] if hasattr(ship, "made") else "?", i))
    return out


def _affine(T, UV):
    """The triangle's texture as a map from its plane: (origin, two unit axes in the plane, 2x2 uv per block, uv at origin)."""
    e1, e2 = T[1] - T[0], T[2] - T[0]
    ax = e1 / np.linalg.norm(e1)
    n = np.cross(e1, e2)
    n /= np.linalg.norm(n)
    ay = np.cross(n, ax)
    E = np.array([[e1 @ ax, e1 @ ay], [e2 @ ax, e2 @ ay]])
    D = np.array([UV[1] - UV[0], UV[2] - UV[0]])
    M = np.linalg.solve(E, D)                               # rows: d(uv)/d(ax), d(uv)/d(ay)
    return T[0], ax, ay, n, M, UV[0]


def survey(C, ship, tag="hull"):
    tris = triangles(C, ship, tag)
    # the seams the paint was laid with on purpose (tools/crew_ship_paint.py: the keel, the nose's ring and chines)
    seams = defaultdict(list)
    for a, b in getattr(getattr(ship, "laid", None), "cuts", ()):
        n = max(2, int(np.linalg.norm(b - a) / 0.2) + 2)
        for k in {tuple(np.floor((a + (b - a) * i / (n - 1) + d) / 0.5).astype(int))
                  for i in range(n) for d in (np.zeros(3), np.full(3, 0.03), np.full(3, -0.03))}:
            seams[k].append((a, b))

    def on_seam(p):
        for a, b in seams.get(tuple(np.floor(p / 0.5).astype(int)), ()):
            e = b - a
            t = min(1.0, max(0.0, (p - a) @ e / (e @ e + 1e-12)))
            if np.linalg.norm(p - (a + e * t)) < 0.02:
                return True
        return False
    maps = [_affine(T, UV) for T, UV, *_ in tris]
    size = []
    for k, ((T, UV, n, name, maker, fi), (o, ax, ay, nn, M, uv0)) in enumerate(zip(tris, maps)):
        s = np.linalg.svd(M * TEXELS / PER_BLOCK, compute_uv=False)        # 1, 1 at the true size
        if abs(s[0] - 1) > SIZE_TOL and abs(s[1] - 1) > SIZE_TOL and s[0] / max(s[1], 1e-9) <= 1 + STRETCH_TOL:
            size.append(("size", k, float((s[0] + s[1]) / 2)))
        elif s[0] / max(s[1], 1e-9) > 1 + STRETCH_TOL or abs(s[0] - 1) > SIZE_TOL or abs(s[1] - 1) > SIZE_TOL:
            size.append(("stretch", k, float(s[0] / max(s[1], 1e-9))))
    # the triangles by the cells they reach into
    cell = 0.5
    grid = defaultdict(list)
    for k, (T, *_) in enumerate(tris):
        lo, hi = np.floor((T.min(0) - TOUCH) / cell).astype(int), np.floor((T.max(0) + TOUCH) / cell).astype(int)
        for x in range(lo[0], hi[0] + 1):
            for y in range(lo[1], hi[1] + 1):
                for z in range(lo[2], hi[2] + 1):
                    grid[(x, y, z)].append(k)
    cosA = math.cos(math.radians(ANGLE))
    breaks = []
    for k, (T, UV, n, name, maker, fi) in enumerate(tris):
        for a, b in ((0, 1), (1, 2), (2, 0)):
            L = float(np.linalg.norm(T[b] - T[a]))
            if L < 0.02:
                continue
            steps = max(2, int(math.ceil(L / 0.25)))
            for s in range(steps):
                t = (s + 0.5) / steps
                p = T[a] + (T[b] - T[a]) * t
                uv = UV[a] + (UV[b] - UV[a]) * t
                key = tuple(np.floor(p / cell).astype(int))
                for j in grid.get(key, ()):
                    if j <= k:
                        continue
                    T2, UV2, n2, name2, maker2, fi2 = tris[j]
                    if fi2 == fi or n @ n2 < cosA or name.endswith("D") != name2.endswith("D"):
                        continue                            # (the dark paint's fittings on the light skin are parts of their own)
                    o, ax, ay, nn, M, uv0 = maps[j]
                    d = p - o
                    if abs(d @ nn) > TOUCH:
                        continue
                    # inside the triangle (a little over its edges)?
                    v0, v1 = T2[1] - T2[0], T2[2] - T2[0]
                    d00, d01, d11, d20, d21 = v0 @ v0, v0 @ v1, v1 @ v1, d @ v0, d @ v1
                    den = d00 * d11 - d01 * d01
                    bv, bw = (d11 * d20 - d01 * d21) / den, (d00 * d21 - d01 * d20) / den
                    tol = TOUCH / math.sqrt(min(d00, d11))
                    if bv < -tol or bw < -tol or bv + bw > 1 + tol:
                        continue
                    uv2 = uv0 + np.array([d @ ax, d @ ay]) @ M
                    off = (uv - uv2 + 0.5) % 1.0 - 0.5
                    jump = float(np.linalg.norm(off)) * TEXELS
                    if jump > BREAK_TEXELS and not on_seam(p):
                        breaks.append((p, L / steps, jump, k, j, float(math.degrees(math.acos(min(1.0, n @ n2))))))
    return tris, size, breaks


# What is left by design (measured at r200: 1.3 square blocks a little off, on the turret's mound and the hump, which
# can't be flattened; 58 blocks of line, most of it where the sponsons' fairings, the wings' outer panels and the fin's
# root stand on a face they don't run on from. Before r200: 1,141 square blocks and 261 blocks). More than these means
# a new part's paint doesn't sit right.
ALLOWED_SIZE = 6.0               # square blocks
ALLOWED_BREAKS = 75.0            # blocks of line


def totals(C, ship):
    tris, size, breaks = survey(C, ship)
    area = sum(float(np.linalg.norm(np.cross(tris[k][0][1] - tris[k][0][0], tris[k][0][2] - tris[k][0][0])) / 2) for _, k, _ in size)
    return area, sum(b[1] for b in breaks), tris, size, breaks


def check(err):
    """For tools/check_mod_data.py: the hull's paint at its true size and running on, within what is left by design."""
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import crew_ship as CS
    area, line, *_ = totals(CS, CS.Ship(ramp_down=0.0, gear=1.0, side_doors=0.0))
    if area > ALLOWED_SIZE:
        err(f"crew_ship_seams: {area:.1f} square blocks of the drop ship's paint are not at their true size (allowed {ALLOWED_SIZE})")
    if line > ALLOWED_BREAKS:
        err(f"crew_ship_seams: the drop ship's panels break along {line:.1f} blocks of line (allowed {ALLOWED_BREAKS})")


def report(C, ship, out=print):
    tris, size, breaks = survey(C, ship)
    by = defaultdict(lambda: [0, 0.0])
    for kind, k, v in size:
        T = tris[k][0]
        by[(kind, tris[k][4], tris[k][3])][0] += 1
        by[(kind, tris[k][4], tris[k][3])][1] += float(np.linalg.norm(np.cross(T[1] - T[0], T[2] - T[0])) / 2)
    out(f"{len(tris)} triangles of paint")
    out("not at its true size (kind, maker, material: triangles, square blocks):")
    for key, (n, a) in sorted(by.items(), key=lambda kv: -kv[1][1]):
        out(f"  {key[0]:8s} {key[1]:16s} {key[2]:8s} {n:5d} {a:8.2f}")
    bb = defaultdict(float)
    for p, L, jump, k, j, ang in breaks:
        a, b = sorted((tris[k][4], tris[j][4]))
        bb[(a, b)] += L
    out(f"breaks: {sum(bb.values()):.1f} blocks of line in all")
    for key, L in sorted(bb.items(), key=lambda kv: -kv[1]):
        out(f"  {key[0]:16s} | {key[1]:16s} {L:7.1f}")
    return tris, size, breaks


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import crew_ship as CS
    report(CS, traced(CS, ramp_down=0.0, gear=0.0, side_doors=0.0, lights=True))
