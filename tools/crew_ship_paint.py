"""The drop ship's paint laid as one skin (Justin, 10 November 2026: "Do a pass over the hull on the ship. There's still
issues with the texture, like here at the wing on top. See what you can improve.").

Until now every part of the outside carried the hull's panels its own way: the skin round the hull by its girth and its
length, fairings by rules that carried the skin's pattern outward along one direction, everything else flat on each
face's own plane. Where those met, the panels broke; where a rule didn't fit the shape, they stretched (the wings' tops
nearly three times, in zigzags; the nose; the gun bays' boxes; the stern's cheeks) or came out at the wrong size (the
spine at half). `unwrap` replaces all of it with one rule for the whole outside, whatever is built on it later:

- The constant part of the hull (the troop box, where the skin's own mapping is exact) stays as it is and **anchors**
  the rest.
- Everything else is unfolded from it, the way sheet metal would be bent: faces that meet within ANGLE (one surface to
  the eye, and to the shading) share their panels across the line where they meet, each face keeping its true size.
  A part that only touches the skin (a fairing's foot on a facet, a corner on another face's edge) takes the skin's
  panels there (`contacts`).
- Where a shape can't be flattened without stretching (the nose, a dome), the stretch is spread as thin as it will go
  (as-rigid-as-possible: every triangle is asked to be a turned copy of its own true shape, and the corners are solved
  for together).
- Faces that meet at a real corner (more than ANGLE) keep their own panels: a seam belongs there.
- CUTS are lines the panels are not carried across although the surface is smooth: the keel, and the ring where the
  nose leaves the troop box (a cone can't carry on a cylinder's panels without stretching at the join); the unfolding
  still starts from the anchored side there, so the panels beyond run the same way.

The far model takes the near model's panels wherever it has the same surface (`prior`), so nothing shifts when the game
swaps them.

`tools/crew_ship_seams.py` measures the result (size, stretch, breaks) for the whole ship.
"""
from __future__ import annotations

import math
from collections import defaultdict, deque

import numpy as np
import scipy.sparse as sp
import scipy.sparse.linalg as spl

ANGLE = 50.0                     # degrees; the same as the shading's (crew_ship.SMOOTH_ANGLE)
TOUCH = 0.012                    # how near a corner must lie to another face to be on it
# A corner that touches another face starts the unfolding of its own part from that face's panels (`through_touch`);
# after that it is not held to it: a part is set where the skin's panels are, as a whole, and is never stretched to
# meet them all along a curved foot (held firmly, the pieces round the nose's vents pulled each other out of shape).
# (Pieces of one flat face cut round an opening are the exception: lying in one plane they can't disagree, and they are
# held together firmly.)
CONTACT_FLAT, CONTACT = 0.0, 0.0
FLAT = math.cos(math.radians(0.5))
MEET = 0.06                      # blocks: two ways round that disagree by more than this have closed a tube or a dome
FOLD = 18.0                      # degrees: a crease this sharp or sharper ...
DEFECT = 2.0                     # ... at a corner this far from flat (the angles round it short of, or over, a full turn)
                                 # is a seam: the skin there can't be flattened, so each side keeps its own panels
REST = 1e-5                      # every corner's faint pull to where the unfolding first put it (fixes a free part's place)
PRIOR = 0.5                      # the far model's pull to the near model's panels
ITERS = 60
# the paints, and the skin each becomes once its corners carry their own coordinates
SKIN_OF = {"hull": "skin", "hull2": "skin2", "hullD": "skinD", "fillet": "skin", "fillet2": "skin2", "filletD": "skinD",
           "hull2C": "skin2", "skin": "skin", "skin2": "skin2", "skinD": "skinD"}
_CACHE = {}


class Laid:
    """A laid skin to look panels up in: for a point on (or within `near` of) one of its triangles facing the same way,
    its texture coordinates."""

    def __init__(self, T, UV, N, TB, cell=0.5):
        self.T, self.UV, self.N, self.TB, self.cell = T, UV, N, TB, cell
        self.grid = defaultdict(list)
        for k in range(len(T)):
            lo, hi = np.floor(T[k].min(0) / cell).astype(int), np.floor(T[k].max(0) / cell).astype(int)
            for x in range(lo[0], hi[0] + 1):
                for y in range(lo[1], hi[1] + 1):
                    for z in range(lo[2], hi[2] + 1):
                        self.grid[(x, y, z)].append(k)

    def at(self, p, n, near=0.04, cos=0.9, which=False):
        best = None
        for k in self.grid.get(tuple(np.floor(p / self.cell).astype(int)), ()):
            if self.N[k] @ n < cos:
                continue
            T = self.T[k]
            d = p - T[0]
            h = abs(d @ self.N[k])
            if h > near or (best is not None and h >= best[0]):
                continue
            v0, v1 = T[1] - T[0], T[2] - T[0]
            d00, d01, d11, d20, d21 = v0 @ v0, v0 @ v1, v1 @ v1, d @ v0, d @ v1
            den = d00 * d11 - d01 * d01
            if den < 1e-14:
                continue
            b1, b2 = (d11 * d20 - d01 * d21) / den, (d00 * d21 - d01 * d20) / den
            if b1 < -0.02 or b2 < -0.02 or b1 + b2 > 1.02:
                continue
            best = (h, self.UV[k][0] * (1 - b1 - b2) + self.UV[k][1] * b1 + self.UV[k][2] * b2, k)
        if best is None:
            return None
        return best[2] if which else best[1]

    def carried(self, P, n):
        """A face's corners' coordinates as this skin has them, if the face lies on it: each read off the triangle
        under a point just inside the face (so a corner on a seam takes its own side's panels). None if it doesn't lie
        on this skin, or wouldn't keep its shape (a face of another build that only crosses it)."""
        P = np.asarray(P, float)
        mid = P.mean(0)
        out = []
        for p in P:
            d = mid - p
            L = np.linalg.norm(d)
            k = self.at(p + d * min(1.0, 0.03 / L) if L > 1e-9 else p, n, which=True)
            if k is None:
                return None
            T = self.T[k]
            v0, v1, dd = T[1] - T[0], T[2] - T[0], p - T[0]
            d00, d01, d11, d20, d21 = v0 @ v0, v0 @ v1, v1 @ v1, dd @ v0, dd @ v1
            den = d00 * d11 - d01 * d01
            b1, b2 = (d11 * d20 - d01 * d21) / den, (d00 * d21 - d01 * d20) / den
            out.append(self.UV[k][0] * (1 - b1 - b2) + self.UV[k][1] * b1 + self.UV[k][2] * b2)
        out = np.array(out)
        for a, b, c in ((0, 1, 3), (1, 2, 3)):               # still its true shape?
            e1, e2 = P[b] - P[a], P[c] - P[a]
            if np.linalg.norm(np.cross(e1, e2)) < 1e-6:
                continue
            ax = e1 / np.linalg.norm(e1)
            ay = np.cross(np.cross(e1, e2) / np.linalg.norm(np.cross(e1, e2)), ax)
            E = np.array([[e1 @ ax, e1 @ ay], [e2 @ ax, e2 @ ay]])
            sv = np.linalg.svd(np.linalg.solve(E, np.array([out[b] - out[a], out[c] - out[a]])) * self.TB, compute_uv=False)
            if sv[0] > 1.1 or sv[1] < 0.9:
                return None
        return out


def _find(par, a):
    while par[a] != a:
        par[a] = par[par[a]]
        a = par[a]
    return a


def unwrap(C, ship, tag="hull", prior=None, report=None):
    """Lays the paint of the ship's `tag` part (see the module's note); the faces' coordinates and materials are
    rewritten in ship.q. Returns the Laid skin (for the far model's `prior`)."""
    TB = C.TEX_BLOCKS["hull"]
    cosA = math.cos(math.radians(ANGLE))
    idx = [i for i, f in enumerate(ship.q)
           if not isinstance(f[0], str) and ship.q.tags[i] == tag and C._name_of(f[2]) in SKIN_OF]
    if not idx:
        return None
    key = (bool(C.FAR), hash(b"".join(np.round(np.asarray(ship.q[i][0], float), 4).tobytes() for i in idx)))
    if key in _CACHE:
        uvs, laid, none = _CACHE[key]
        for i, UV in zip(idx, uvs):
            f = ship.q[i]
            if i not in none:
                ship.q[i] = (f[0], UV, C.mat(SKIN_OF[C._name_of(f[2])])) + tuple(f[3:])
        return laid

    # ---- the triangles the game draws (a quad's diagonal runs 1-3), their corners welded where they coincide
    vid, pos = {}, []

    def vertex(p):
        k = tuple(np.round(p, 3))
        if k not in vid:
            vid[k] = len(pos)
            pos.append(np.asarray(p, float))
        return vid[k]

    tris = []                    # (face, its corners in the quad (-1: not one), vertex ids)
    raw = []
    TP, TN, TUV, pinned_face = [], [], [], []
    ring_body = C.ring_at(0.0)
    body_from = C.BODY_FROM

    def on_body(P):
        """Whether a face lies on the hull's constant part (the skin there is exact: an anchor)."""
        if P[:, 2].min() < body_from - 1e-6:
            return False
        rr = ring_body + ring_body[:1]
        for p in P:
            d = min(_seg_dist(p[:2], np.array(rr[j]), np.array(rr[j + 1])) for j in range(len(ring_body)))
            if d > 1e-3:
                return False
        return True

    for i in idx:
        f = ship.q[i]
        P = np.asarray(f[0], float)
        name = C._name_of(f[2])
        n = C.face_normal(P)
        UV = np.asarray(f[1], float) if C.local_uv(name) else C.world_uv(P, name, n)
        pin = name in ("skin", "skin2", "skinD") and on_body(P)
        for a, b, c in ((0, 1, 3), (1, 2, 3)):
            T = P[[a, b, c]]
            nn = np.cross(T[1] - T[0], T[2] - T[0])
            ar = np.linalg.norm(nn) / 2
            if ar < 1e-7:
                continue
            ids = tuple(vertex(p) for p in T)
            if len(set(ids)) < 3:                           # (a sliver whose corners weld together)
                continue
            raw.append((i, (a, b, c), ids, T, UV[[a, b, c]], pin))
    # A corner of one face lying on another's edge (pieces cut round an opening, a wing's strips along its outer
    # panel): that edge is broken there, so the two share it properly and unfold as one. A triangle with such corners on
    # its edges is drawn, for this, as a fan round its middle (it stays flat, so its own corners come out as before).
    vgrid = defaultdict(list)
    for v, p_ in enumerate(pos):
        vgrid[tuple(np.floor(p_ / 0.5).astype(int))].append(v)
    n_split = 0
    for (i, abc, ids, T, UVt, pin) in raw:
        lo, hi = np.floor((T.min(0) - 2e-3) / 0.5).astype(int), np.floor((T.max(0) + 2e-3) / 0.5).astype(int)
        near_v = [v for x in range(lo[0], hi[0] + 1) for y in range(lo[1], hi[1] + 1) for z in range(lo[2], hi[2] + 1)
                  for v in vgrid.get((x, y, z), ()) if v not in ids]
        on_edge = [[] for _ in range(3)]
        for v in near_v:
            p_ = pos[v]
            for e in range(3):
                A_, B_ = T[e], T[(e + 1) % 3]
                d = B_ - A_
                L2 = d @ d
                t_ = (p_ - A_) @ d / L2
                if 2e-3 / math.sqrt(L2) < t_ < 1 - 2e-3 / math.sqrt(L2) and np.linalg.norm(p_ - (A_ + d * t_)) < 1e-3:
                    on_edge[e].append((t_, v))
                    break
        nn = np.cross(T[1] - T[0], T[2] - T[0])
        ar = np.linalg.norm(nn) / 2
        nrm = -nn / (2 * ar)
        if not any(on_edge):
            tris.append((i, abc, ids))
            TP.append(T)
            TN.append(nrm)
            TUV.append(UVt)
            pinned_face.append(pin)
            continue
        n_split += 1
        rim = []                                            # (which corner of the quad or -1, vertex, place, coordinates)
        for e in range(3):
            rim.append((abc[e], ids[e], T[e], UVt[e]))
            for t_, v in sorted(on_edge[e]):
                rim.append((-1, v, pos[v], UVt[e] + (UVt[(e + 1) % 3] - UVt[e]) * t_))
        mid_p, mid_uv = T.mean(0), UVt.mean(0)
        mid = len(pos)
        pos.append(mid_p)                                   # (its own corner: nothing else welds to it)
        for k in range(len(rim)):
            r0, r1 = rim[k], rim[(k + 1) % len(rim)]
            Ts = np.array([r0[2], r1[2], mid_p])
            if np.linalg.norm(np.cross(Ts[1] - Ts[0], Ts[2] - Ts[0])) / 2 < 1e-7:
                continue
            tris.append((i, (r0[0], r1[0], -1), (r0[1], r1[1], mid)))
            TP.append(Ts)
            TN.append(nrm)
            TUV.append(np.array([r0[3], r1[3], mid_uv]))
            pinned_face.append(pin)
    petal_of = {i: C.PETALS.get(C.face_key(ship.q[i][0])) for i in idx} if getattr(C, "PETALS", None) else {}
    petal = [petal_of.get(tr[0]) for tr in tris]
    nt = len(tris)
    TP, TN, TUV = np.array(TP), np.array(TN), np.array(TUV)
    pos = np.array(pos)

    # ---- which triangles carry on into which (an edge shared, within ANGLE, not a cut, one each side of it)
    edges = defaultdict(list)
    for t, (_, _, vs) in enumerate(tris):
        for e in range(3):
            a, b = vs[e], vs[(e + 1) % 3]
            if a != b:
                edges[(min(a, b), max(a, b))].append((t, e))

    def is_cut(a, b, s, t):
        pa, pb = pos[a], pos[b]
        for kind, at, *rest in C.PAINT_CUTS:
            ax = "xyz".index(kind)
            if abs(pa[ax] - at) < 1e-4 and abs(pb[ax] - at) < 1e-4:
                if not rest or rest[0](pa, pb, TN[s], TN[t]):
                    return True
        return False

    par = list(range(nt * 3))                               # the corners (triangle, which) that are one corner of the skin
    cand = []
    for (a, b), lst in edges.items():
        for x in range(len(lst)):
            for y in range(x + 1, len(lst)):
                (s, es), (t, et) = lst[x], lst[y]
                if TN[s] @ TN[t] < cosA:
                    continue
                # one each side of the edge (not one lying on the other)
                e = pos[b] - pos[a]
                cs = TP[s][(es + 2) % 3] - pos[a]
                ct = TP[t][(et + 2) % 3] - pos[a]
                if (np.cross(e, TN[s]) @ cs) * (np.cross(e, TN[t]) @ ct) > 0 and tris[s][0] != tris[t][0]:
                    continue
                cand.append((s, t, a, b))
    # how far from flat the skin is at each corner: the angles of the triangles round it against a full turn (only
    # where the skin runs unbroken all the way round it)
    turn = np.zeros(len(pos))
    whole = np.ones(len(pos), bool)
    for (a, b), lst in edges.items():
        if len(lst) != 2 or TN[lst[0][0]] @ TN[lst[1][0]] < cosA:
            whole[a] = whole[b] = False
    for t in range(nt):
        for c in range(3):
            u, v = TP[t][(c + 1) % 3] - TP[t][c], TP[t][(c + 2) % 3] - TP[t][c]
            turn[tris[t][2][c]] += math.acos(max(-1.0, min(1.0, (u @ v) / (np.linalg.norm(u) * np.linalg.norm(v)))))
    defect = np.where(whole, np.abs(np.degrees(turn) - 360.0), 0.0)
    cosF = math.cos(math.radians(FOLD))
    # ---- corners that touch another face (a fairing's foot on the skin, a corner on an edge): they take its panels
    cell = 0.5
    grid = defaultdict(list)
    for t in range(nt):
        lo, hi = np.floor((TP[t].min(0) - TOUCH) / cell).astype(int), np.floor((TP[t].max(0) + TOUCH) / cell).astype(int)
        for x in range(lo[0], hi[0] + 1):
            for y in range(lo[1], hi[1] + 1):
                for z in range(lo[2], hi[2] + 1):
                    grid[(x, y, z)].append(t)
    at_vertex = defaultdict(list)
    for t in range(nt):
        for c in range(3):
            at_vertex[tris[t][2][c]].append((t, c))
    contacts = []                                           # (corner, the triangle it lies on, where on it)
    touching = defaultdict(list)                            # by the triangle whose corner it is
    seen = set()
    for v, lst in at_vertex.items():
        p = pos[v]
        for t2 in grid.get(tuple(np.floor(p / cell).astype(int)), ()):
            if v in tris[t2][2]:
                # a corner of that face too: it touches it there, unless the two share an edge (then they are
                # joined, or parted on purpose)
                k2 = tris[t2][2].index(v)
                one = np.zeros(3)
                one[k2] = 1.0
                for t, c in lst:
                    if t == t2 or tris[t][0] == tris[t2][0] or TN[t] @ TN[t2] < cosA:
                        continue
                    if len(set(tris[t][2]) & set(tris[t2][2])) >= 2:
                        continue
                    if (t, c, t2) not in seen:
                        seen.add((t, c, t2))
                        touching[t].append((c, t2, one))
                continue
            d = p - TP[t2][0]
            if abs(d @ TN[t2]) > TOUCH:
                continue
            v0, v1 = TP[t2][1] - TP[t2][0], TP[t2][2] - TP[t2][0]
            d00, d01, d11, d20, d21 = v0 @ v0, v0 @ v1, v1 @ v1, d @ v0, d @ v1
            den = d00 * d11 - d01 * d01
            b1, b2 = (d11 * d20 - d01 * d21) / den, (d00 * d21 - d01 * d20) / den
            tol = TOUCH / math.sqrt(min(d00, d11))
            if b1 < -tol or b2 < -tol or b1 + b2 > 1 + tol:
                continue
            for t, c in lst:
                if TN[t] @ TN[t2] < cosA or tris[t][0] == tris[t2][0]:
                    continue
                if (t, c, t2) in seen:
                    continue
                seen.add((t, c, t2))
                contacts.append((t, c, t2, np.array([1 - b1 - b2, b1, b2]), CONTACT_FLAT if TN[t] @ TN[t2] > FLAT else CONTACT))
                touching[t].append((c, t2, np.array([1 - b1 - b2, b1, b2])))

    # ---- the unfolding, run again while it meets itself out of step somewhere (each such edge becomes a seam)
    met = set()
    for round_ in range(200):
        met_now = set()
        smooth, cuts = [], []
        par = list(range(nt * 3))
        for s, t, a, b in cand:
            # (never between the two triangles of one quad: a face stays one piece)
            if tris[s][0] != tris[t][0] and ((min(s, t), max(s, t)) in met or is_cut(a, b, s, t) or
                                             (petal[s] is not None and petal[t] is not None and petal[s] != petal[t]) or
                                             (TN[s] @ TN[t] < cosF and max(defect[a], defect[b]) > DEFECT)):
                cuts.append((s, t, a, b))
                continue
            smooth.append((s, t, a, b))
            for v in (a, b):
                ks = s * 3 + tris[s][2].index(v)
                kt = t * 3 + tris[t][2].index(v)
                ra, rb = _find(par, ks), _find(par, kt)
                if ra != rb:
                    par[ra] = rb
        wid = {}
        W = np.empty((nt, 3), int)                              # each triangle's three corners of the skin
        for t in range(nt):
            for c in range(3):
                W[t, c] = wid.setdefault(_find(par, t * 3 + c), len(wid))
        nw = len(wid)

        # ---- the anchors
        fixed = np.zeros(nw, bool)
        U = np.zeros((nw, 2))
        for t in range(nt):
            if pinned_face[t]:
                for c in range(3):
                    fixed[W[t, c]] = True
                    U[W[t, c]] = TUV[t][c]

        # ---- each triangle's own true shape in the texture's units, and which way round it lies
        X = np.zeros((nt, 3, 2))
        for t in range(nt):
            ex = TP[t][1] - TP[t][0]
            ex /= np.linalg.norm(ex)
            ey = np.cross(TN[t], ex)
            d = TP[t] - TP[t][0]
            X[t, :, 0], X[t, :, 1] = d @ ex / TB, d @ ey / TB

        def handed(t, uv):
            a = uv[1] - uv[0]
            b = uv[2] - uv[0]
            xa, xb = X[t, 1] - X[t, 0], X[t, 2] - X[t, 0]
            return 1.0 if (a[0] * b[1] - a[1] * b[0]) * (xa[0] * xb[1] - xa[1] * xb[0]) >= 0 else -1.0

        # ---- the unfolding: outward from the anchors, then each part left from its largest face as it lies now
        nb = defaultdict(list)
        for s, t, a, b in smooth:
            nb[s].append((t, False))
            nb[t].append((s, False))
        for s, t, a, b in cuts:
            nb[s].append((t, True))
            nb[t].append((s, True))
        placed = fixed.copy()
        hand = np.zeros(nt)
        done = np.zeros(nt, bool)
        queue = deque()
        for t in range(nt):
            if pinned_face[t]:
                hand[t] = handed(t, TUV[t])
                done[t] = True
                queue.append(t)

        def lay(t, s, across):
            """Triangle t unfolded about the edge it shares with s (already laid)."""
            vs, vt = tris[s][2], tris[t][2]
            common = [v for v in vt if v in vs]
            if len(common) < 2:
                return False
            a, b = common[0], common[1]
            ia, ib = vt.index(a), vt.index(b)
            ic = 3 - ia - ib
            ua, ub = U[W[s, vs.index(a)]], U[W[s, vs.index(b)]]
            if across:                                          # over a cut: its own corners, begun where the other side's are
                for i_, u_ in ((ia, ua), (ib, ub)):
                    if not placed[W[t, i_]]:
                        U[W[t, i_]] = u_
                        placed[W[t, i_]] = True
            hand[t] = hand[s]
            ua, ub = U[W[t, ia]], U[W[t, ib]]
            d = ub - ua
            L = np.linalg.norm(d)
            d = d / L if L > 1e-12 else np.array([1.0, 0.0])
            xe = X[t, ib] - X[t, ia]
            Lx = np.linalg.norm(xe)
            xe = xe / Lx
            xc = X[t, ic] - X[t, ia]
            al, be = xc @ xe, xc[1] * xe[0] - xc[0] * xe[1]            # along the edge, and off it (in the triangle's own plane)
            uc = ua + d * al + np.array([-d[1], d[0]]) * be * hand[t]
            if placed[W[t, ic]]:
                # reached before by another way round: if the two ways disagree the skin has closed on itself (a tube, a
                # dome) and can't be flat here: this edge becomes a seam, and the unfolding is run again
                if not across and not fixed[W[t, ic]] and np.linalg.norm(uc - U[W[t, ic]]) > MEET / TB:
                    if tris[s][0] != tris[t][0]:
                        met_now.add((min(s, t), max(s, t)))
                        return False
                    # (the other half of the same quad: a face stays whole, so the seam goes on its far side, between
                    # it and the faces that reached that corner first)
                    vc = vt[ic]
                    for n_, acr in nb[t]:
                        if not acr and done[n_] and tris[n_][0] != tris[t][0] and vc in tris[n_][2]:
                            met_now.add((min(t, n_), max(t, n_)))
            else:
                U[W[t, ic]] = uc
                placed[W[t, ic]] = True
            return True

        def run():
            while queue:
                s = queue.popleft()
                for t, across in nb[s]:
                    if done[t] or (across and not fixed[W[s]].all()):
                        continue
                    if lay(t, s, across):
                        done[t] = True
                        queue.append(t)

        run()
        # Then the parts not reached (a part: triangles joined through smooth edges). The skin grows outward from what
        # is laid: of all the triangles with two corners touching laid faces, the one that lies flattest against them
        # starts its part there (a fairing on an edge between two faces carries on the one it runs on from, not the
        # one it turns away from). When nothing left touches anything laid, the largest part left starts from its
        # largest triangle as that lies flat on its own plane (world_uv: as it was drawn before), and the growing goes
        # on from it: so a small part takes its panels from the large one it stands on, never the other way round.
        part = list(range(nt))
        for s_, t_, a_, b_ in smooth:
            ra, rb = _find(part, s_), _find(part, t_)
            if ra != rb:
                part[ra] = rb
        tri_area = [float(np.linalg.norm(np.cross(TP[t][1] - TP[t][0], TP[t][2] - TP[t][0])) / 2) for t in range(nt)]
        members = defaultdict(list)
        for t in range(nt):
            members[_find(part, t)].append(t)
        by_size = sorted(members, key=lambda r: -sum(tri_area[t] for t in members[r]))
        nxt = 0
        # where a triangle meets others without sharing an edge: its own corners lying on their faces, and their
        # corners lying on its face: (the other triangle, the place in this one's own flat shape, how to read the
        # panels there off the other)
        links = defaultdict(list)
        for t, lst in touching.items():
            for c, t2, bary in lst:
                links[t].append((t2, X[t, c], ("on", bary)))
        guests = [t for t in links if len(links[t]) >= 2]
        while True:
            pick = None
            left = []
            for t in guests:
                if done[t]:
                    continue
                left.append(t)
                use = []
                for t2, x, how in links[t]:
                    if done[t2]:
                        use.append((float(TN[t] @ TN[t2]), t2, x, how))
                if len(use) < 2:
                    continue
                use.sort(key=lambda k: -k[0])
                first = use[0]
                apart = [k for k in use[1:] if np.linalg.norm(k[2] - first[2]) > 0.02 / TB]
                home = _find(part, first[1])
                second = next((k for k in apart if _find(part, k[1]) == home), None) or (apart[0] if apart else None)
                if second is None:
                    continue
                sc = min(first[0], second[0])
                if pick is None or sc > pick[0]:
                    pick = (sc, t, first, second)
            guests = left
            if pick is not None:
                _, t, first, second = pick
                at = []
                for _, t2, x, how in (first, second):
                    at.append((x, how[1] @ U[W[t2]] if how[0] == "on" else U[W[t2, how[1]]]))
                (x1, u1), (x2, u2) = at
                hand[t] = hand[first[1]]
                d = u2 - u1
                L = np.linalg.norm(d)
                d = d / L if L > 1e-12 else np.array([1.0, 0.0])
                xe = x2 - x1
                xe = xe / np.linalg.norm(xe)
                for c in range(3):
                    if not placed[W[t, c]]:
                        xc = X[t, c] - x1
                        al, be = xc @ xe, xc[1] * xe[0] - xc[0] * xe[1]
                        U[W[t, c]] = u1 + d * al + np.array([-d[1], d[0]]) * be * hand[t]
                        placed[W[t, c]] = True
            else:
                todo = []
                while nxt < len(by_size) and not todo:
                    todo = [t for t in members[by_size[nxt]] if not done[t]]
                    if not todo:
                        nxt += 1
                if not todo:
                    break
                t = max(todo, key=lambda t: tri_area[t])
                uv = C.world_uv(TP[t], "hull", TN[t])
                for c in range(3):
                    if not placed[W[t, c]]:
                        U[W[t, c]] = uv[c]
                        placed[W[t, c]] = True
                hand[t] = handed(t, np.array([U[W[t, c]] for c in range(3)]))
            done[t] = True
            queue.append(t)
            run()
        if not met_now:
            break
        met |= met_now
    U0 = U.copy()

    # ---- the solve: every triangle a turned copy of its true shape, the corners found together
    I, J, V_ = [], [], []
    wts = np.zeros((nt, 3))
    for t in range(nt):
        for e in range(3):
            i_, j_, k_ = e, (e + 1) % 3, (e + 2) % 3
            a, b = X[t, i_] - X[t, k_], X[t, j_] - X[t, k_]
            cr = abs(a[0] * b[1] - a[1] * b[0])
            w = min(20.0, max(0.02, (a @ b) / (cr + 1e-12)))
            wts[t, e] = w
            wi, wj = W[t, i_], W[t, j_]
            I += [wi, wj, wi, wj]
            J += [wi, wj, wj, wi]
            V_ += [w, w, -w, -w]
    base_rhs = np.zeros((nw, 2))
    once = {}
    for t, c, t2, bary, hold in contacts:                   # (one hold for each corner of the skin on each face it touches)
        once.setdefault((W[t, c], t2), (W[t, c], t2, bary, hold))
    contacts = list(once.values())
    contacts = [k for k in contacts if k[3] > 0]
    for wv, t2, bary, hold in contacts:
        I.append(wv)
        J.append(wv)
        V_.append(hold)
    I += list(range(nw))
    J += list(range(nw))
    V_ += [REST] * nw
    base_rhs += REST * U0
    if prior is not None:
        vn = defaultdict(lambda: np.zeros(3))
        for t in range(nt):
            for c in range(3):
                vn[W[t, c]] = vn[W[t, c]] + TN[t]
        wp = {}
        for t in range(nt):
            for c in range(3):
                wp[W[t, c]] = TP[t][c]
        for w_, p in wp.items():
            if fixed[w_]:
                continue
            n = vn[w_] / (np.linalg.norm(vn[w_]) + 1e-12)
            uv = prior.at(p, n)
            if uv is not None:
                I.append(w_)
                J.append(w_)
                V_.append(PRIOR)
                base_rhs[w_] += PRIOR * uv
                U[w_] = uv
    Lm = sp.csr_matrix((V_, (I, J)), shape=(nw, nw))
    free = np.flatnonzero(~fixed)
    fx = np.flatnonzero(fixed)
    solve = spl.factorized(sp.csc_matrix(Lm[free][:, free]))
    Lfp = Lm[free][:, fx]
    Wi, Wj = W, W[:, [1, 2, 0]]
    dX = X - X[:, [1, 2, 0]]                                # each edge i -> j of each triangle, true shape
    dX[:, :, 1] *= hand[:, None]
    it = -1
    for it in range(ITERS):
        dU = U[Wi] - U[Wj]                                  # nt x 3 x 2
        A00 = (wts * dU[:, :, 0] * dX[:, :, 0]).sum(1)
        A01 = (wts * dU[:, :, 0] * dX[:, :, 1]).sum(1)
        A10 = (wts * dU[:, :, 1] * dX[:, :, 0]).sum(1)
        A11 = (wts * dU[:, :, 1] * dX[:, :, 1]).sum(1)
        th = np.arctan2(A10 - A01, A00 + A11)
        c_, s_ = np.cos(th)[:, None], np.sin(th)[:, None]
        Rx = c_ * dX[:, :, 0] - s_ * dX[:, :, 1]
        Ry = s_ * dX[:, :, 0] + c_ * dX[:, :, 1]
        rhs = base_rhs.copy()
        for wv, t2, bary, hold in contacts:                 # (where the face it touches stands now)
            rhs[wv] += hold * (bary @ U[W[t2]])
        for k, R in enumerate((Rx, Ry)):
            np.add.at(rhs[:, k], Wi.ravel(), (wts * R).ravel())
            np.add.at(rhs[:, k], Wj.ravel(), -(wts * R).ravel())
        b = rhs[free] - Lfp @ U[fx]
        new = np.stack([solve(b[:, 0]), solve(b[:, 1])], 1)
        move = np.abs(new - U[free]).max() if len(free) else 0.0
        U[free] = new
        if move < 2e-5:
            break
    if report is not None:
        report(f"paint: {len(idx)} faces, {nt} triangles ({n_split} broken at corners on their edges), {nw} corners ({int(fixed.sum())} anchored), "
               f"{len(contacts)} touching, unfolded {round_ + 1} times ({len(met)} seams where it met itself), relaxed {it + 1} rounds")

    # ---- back onto the faces
    uvs, none = [], set()
    n_same = 0
    corner = {}
    for t, (i, abc, _) in enumerate(tris):
        for c, q in enumerate(abc):
            if q >= 0:
                corner[(i, q)] = U[W[t, c]]
    for i in idx:
        f = ship.q[i]
        P = np.asarray(f[0], float)
        have = [q for q in range(4) if (i, q) in corner]
        if not have:                                        # (a face with no area: left as it is)
            uvs.append(np.asarray(f[1], float))
            none.add(i)
            continue
        UV = []
        for q in range(4):
            if (i, q) in corner:
                UV.append(corner[(i, q)])
            else:                                           # a pinched quad's spare corner: the one it lies on
                UV.append(corner[(i, min(have, key=lambda r: float(np.abs(P[r] - P[q]).sum())))])
        UV = np.array(UV)
        if prior is not None:                               # the far model: the near model's panels wherever it has the face
            same = prior.carried(P, C.face_normal(P))
            if same is not None:
                UV = same
                n_same += 1
        uvs.append(UV)
        ship.q[i] = (f[0], UV, C.mat(SKIN_OF[C._name_of(f[2])])) + tuple(f[3:])
    laid = Laid(TP, np.array([[U[W[t, c]] for c in range(3)] for t in range(nt)]), TN, TB)
    laid.cuts = [(pos[a], pos[b]) for s_, t_, a, b in cuts]  # (the seams by design, for tools/crew_ship_seams.py)
    if report is not None and prior is not None:
        report(f"paint: {n_same} of {len(idx)} faces carry the near model's panels")
    laid.dbg = dict(touching=dict(touching), tris=tris, W=W, U=U, U0=U0, contacts=contacts, fixed=fixed, TP=TP, TN=TN, X=X, hand=hand, met=met)
    _CACHE[key] = (uvs, laid, none)
    return laid


def _seg_dist(p, a, b):
    e = b - a
    t = min(1.0, max(0.0, (p - a) @ e / (e @ e + 1e-12)))
    return float(np.linalg.norm(p - (a + e * t)))
