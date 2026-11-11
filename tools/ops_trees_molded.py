"""The Ember Oak cast in one piece (r183; Justin, 8 November 2026: "you need to work on like where the roots go to the
trunk, where the trunk go to the limbs"): the three strands of the trunk, the roots, the limbs and the limbs' own
branches joined as one smooth surface (each branch the plain union of its round cones, the branches blended into each
other by a soft minimum, then polygonised), so every root and limb grows out of what it springs from with a filleted
join instead of being pushed through it, and no seam anywhere where the bark has plates. Over it raised plates of bark,
each its own shape (the cells of seeds spread over the surface itself, so they run over the joins too), each a few
quads: its outline simplified to straight sides, its top a low fan, its sides down to a V where it meets its
neighbour (a second mesh, drawn full bright) and a thin strip of molten glow in each crack. Where a branch gets thinner
than the plates are worth, it goes on as the plain bark and glowing cracks of before, starting under the last plates.

Needs scikit-image (marching cubes) and scipy (nearest seeds)."""
import math
import numpy as np
from scipy.spatial import cKDTree
from skimage import measure

_Y = np.array([0.0, 1.0, 0.0])
_X = np.array([1.0, 0.0, 0.0])


def unit(v):
    n = np.linalg.norm(v)
    return v / n if n > 1e-12 else v


def _cone(q, a, b, ra, rb):
    """Signed distance (near enough) from points q (..., 3) to the round cone a-b with radii ra, rb."""
    ab = b - a
    L2 = max(float(ab @ ab), 1e-12)
    t = np.clip(((q - a) @ ab) / L2, 0.0, 1.0)
    return np.linalg.norm(q - (a + t[..., None] * ab), axis=-1) - (ra + (rb - ra) * t)


class Field:
    """Branches [(points, radii, level)]: each the plain union of its round cones; the branches of each level joined to
    each other and to everything of the levels before by a soft minimum of that level's width ks[level] (so where they
    meet the surface swells into a fillet: the trunk's strands into each other, the roots and limbs into the trunk
    with a broader one, the limbs' own branches into the limbs with a smaller). Each round cone is felt only within
    `pad` of it."""

    def __init__(self, tubes, ks, pad=0.3):
        self.ks, self.pad = dict(ks), pad
        self.tubes = []
        for P, R, lv in tubes:
            P = np.asarray(P, float)
            R = np.asarray(R, float)
            segs = []
            for i in range(len(P) - 1):
                a, b, ra, rb = P[i], P[i + 1], float(R[i]), float(R[i + 1])
                rr = max(ra, rb) + pad
                segs.append((a, b, ra, rb, np.minimum(a, b) - rr, np.maximum(a, b) + rr))
            lo = np.min([s[4] for s in segs], 0)
            hi = np.max([s[5] for s in segs], 0)
            self.tubes.append((lo, hi, segs, lv))
        self.levels = sorted(set(t[3] for t in self.tubes))

    def _each(self, Q, level=None):
        """Per branch (of one level, or all), the points near it, their distances to it and its radius there."""
        for ti, (lo, hi, segs, lv) in enumerate(self.tubes):
            if level is not None and lv != level:
                continue
            idx = np.nonzero(np.all((Q >= lo) & (Q <= hi), axis=1))[0]
            if not len(idx):
                continue
            q = Q[idx]
            d = np.full(len(idx), np.inf)
            r = np.zeros(len(idx))
            for a, b, ra, rb, slo, shi in segs:
                s = np.nonzero(np.all((q >= slo) & (q <= shi), axis=1))[0]
                if not len(s):
                    continue
                ab = b - a
                t = np.clip(((q[s] - a) @ ab) / max(float(ab @ ab), 1e-12), 0.0, 1.0)
                rs = ra + (rb - ra) * t
                ds = np.linalg.norm(q[s] - (a + t[:, None] * ab), axis=1) - rs
                nearer = ds < d[s]
                d[s[nearer]] = ds[nearer]
                r[s[nearer]] = rs[nearer]
            f = np.isfinite(d)
            yield ti, idx[f], d[f], r[f]

    def at(self, Q):
        Q = np.asarray(Q, float)
        prev = None
        for lv in self.levels:
            k = self.ks[lv]
            m = np.full(len(Q), np.inf)
            acc = np.zeros(len(Q))
            if prev is not None:
                f = prev < self.pad
                m[f] = prev[f]
                acc[f] = 1.0
            for _, j, d, _ in self._each(Q, lv):
                nm = np.minimum(m[j], d)
                with np.errstate(invalid="ignore"):
                    keep = np.where(np.isfinite(m[j]), np.exp(-(m[j] - nm) / k), 0.0)
                acc[j] = acc[j] * keep + np.exp(-(d - nm) / k)
                m[j] = nm
            out = np.full(len(Q), self.pad)
            f = np.isfinite(m)
            out[f] = np.minimum(m[f] - k * np.log(acc[f]), self.pad)
            prev = out
        return prev

    def nearest(self, Q):
        """The radius of the branch nearest each point there, and which branch it is."""
        Q = np.asarray(Q, float)
        best = np.full(len(Q), np.inf)
        r = np.zeros(len(Q))
        which = np.full(len(Q), -1)
        for ti, j, d, rr in self._each(Q):
            nearer = d < best[j]
            best[j[nearer]] = d[nearer]
            r[j[nearer]] = rr[nearer]
            which[j[nearer]] = ti
        return r, which

    def normal(self, Q, h=1e-3):
        Q = np.asarray(Q, float)
        g = np.zeros_like(Q)
        for i in range(3):
            e = np.zeros(3)
            e[i] = h
            g[:, i] = self.at(Q + e) - self.at(Q - e)
        return g / np.maximum(np.linalg.norm(g, axis=1)[:, None], 1e-12)

    def project(self, Q, passes=2):
        Q = np.asarray(Q, float)
        for _ in range(passes):
            Q = Q - self.normal(Q) * self.at(Q)[:, None]
        return Q

    def grid(self, lo, voxel, n):
        """The field on the grid lo + voxel * (i, j, k), i, j, k < n; branch by branch, each over only its own box."""
        lo = np.asarray(lo, float)
        D = np.full(n, np.inf, np.float32)
        ax = [lo[i] + np.arange(n[i]) * voxel for i in range(3)]

        def box(a, b):
            i0 = np.clip(np.floor((a - lo) / voxel).astype(int), 0, n)
            i1 = np.clip(np.ceil((b - lo) / voxel).astype(int) + 1, 0, n)
            return tuple(slice(int(i0[d]), int(i1[d])) for d in range(3))
        F = None
        for lv in self.levels:
            k = self.ks[lv]
            M = np.full(n, np.inf, np.float32)
            A = np.zeros(n, np.float32)
            if F is not None:
                f = F < self.pad
                M[f] = F[f]
                A[f] = 1.0
            for tlo, thi, segs, tl in self.tubes:
                if tl != lv:
                    continue
                sl = box(tlo, thi)
                if any(s.stop <= s.start for s in sl):
                    continue
                D[sl] = np.inf
                for a, b, ra, rb, slo, shi in segs:
                    ss = box(slo, shi)
                    if any(s.stop <= s.start for s in ss):
                        continue
                    q = np.stack(np.meshgrid(ax[0][ss[0]], ax[1][ss[1]], ax[2][ss[2]], indexing="ij"), -1)
                    D[ss] = np.minimum(D[ss], _cone(q, a, b, ra, rb))
                d, mm, aa = D[sl], M[sl], A[sl]
                f = np.isfinite(d)
                nm = np.where(f, np.minimum(mm, d), mm)
                with np.errstate(invalid="ignore", over="ignore"):
                    e1 = np.where(f & np.isfinite(mm), np.exp(-(mm - nm) / k), 0.0)
                    e2 = np.where(f, np.exp(-(d - nm) / k), 0.0)
                A[sl] = np.where(f, aa * e1 + e2, aa)
                M[sl] = nm
            F = np.full(n, self.pad, np.float32)
            f = np.isfinite(M)
            F[f] = np.minimum(M[f] - k * np.log(A[f]), self.pad)
        return F


def cast(field, lo, hi, voxel):
    """The field's surface inside the box lo..hi: (vertices, triangles), the vertices put back on the true surface."""
    lo, hi = np.asarray(lo, float), np.asarray(hi, float)
    n = tuple(int(x) for x in np.ceil((hi - lo) / voxel).astype(int) + 1)
    F = field.grid(lo, voxel, n)
    F[0, :, :] = F[-1, :, :] = F[:, 0, :] = F[:, -1, :] = F[:, :, 0] = F[:, :, -1] = field.pad
    V, T, _, _ = measure.marching_cubes(F, 0.0, spacing=(voxel, voxel, voxel))
    V = V.astype(float) + lo
    return field.project(V), T.astype(np.int64)


def _clip_plane(poly, nrm, c):
    """The part of a planar polygon (3D points) where nrm . p <= c."""
    out = []
    m = len(poly)
    for i in range(m):
        a, b = poly[i], poly[(i + 1) % m]
        fa, fb = float(nrm @ a) - c, float(nrm @ b) - c
        if fa <= 0:
            out.append(a)
        if (fa < 0) != (fb < 0) and abs(fa - fb) > 1e-15:
            out.append(a + (b - a) * (fa / (fa - fb)))
    return out


def _weld(tris, tol=2e-5):
    flat = np.asarray(tris, float).reshape(-1, 3)
    key = np.round(flat / tol).astype(np.int64)
    _, idx, inv = np.unique(key, axis=0, return_index=True, return_inverse=True)
    V = flat[idx]
    T = inv.reshape(-1, 3)
    good = (T[:, 0] != T[:, 1]) & (T[:, 1] != T[:, 2]) & (T[:, 0] != T[:, 2])
    return V, T, good


def clip(V, T, planes, field, floor_y=None):
    """Triangles cut back to the inner side of each plane (c, n, branch): a plane cuts only the surface of its own
    branch (the triangles nearest it). With floor_y, triangles wholly below it dropped."""
    C = V[T].mean(1)
    _, which = field.nearest(C)
    tris = []
    by_branch = {}
    for c, nrm, ti in planes:
        by_branch.setdefault(ti, []).append((np.asarray(c, float), np.asarray(nrm, float)))
    for f, t in enumerate(T):
        P = [V[i] for i in t]
        if floor_y is not None and max(p[1] for p in P) < floor_y:
            continue
        poly = P
        for c, nrm in by_branch.get(int(which[f]), ()):
            poly = _clip_plane(poly, nrm, float(nrm @ c))
            if len(poly) < 3:
                break
        for i in range(1, len(poly) - 1):
            tris.append([poly[0], poly[i], poly[i + 1]])
    V2, T2, good = _weld(tris, 1e-5)
    return V2, T2[good]


def _dp(P, tol):
    """The points of polyline P that Douglas-Peucker keeps (its ends always)."""
    keep = {0, len(P) - 1}
    stack = [(0, len(P) - 1)]
    while stack:
        i, j = stack.pop()
        if j <= i + 1:
            continue
        a, b = P[i], P[j]
        ab = b - a
        L = np.linalg.norm(ab)
        seg = P[i + 1:j]
        d = np.linalg.norm(seg - a, axis=1) if L < 1e-12 else np.linalg.norm(np.cross(seg - a, ab / L), axis=1)
        w = int(np.argmax(d))
        if d[w] > tol:
            keep.add(i + 1 + w)
            stack += [(i, i + 1 + w), (i + 1 + w, j)]
    return sorted(keep)


def plates(m, edges, field, V, T, rnd, spacing, gap, thick, r_min, plate_mat, edge_mat, glow_mat, solid, lit, glow, tol=0.005,
           stats=None, pre_seeds=(), bend=0.05, short=0.025):
    """Raised plates cut from the surface (V, T): the cells of seeds spread over it about spacing(r) apart (r the
    branch's radius there), the triangles split along the planes halfway between seeds so the cells' sides run
    straight; each cell's outline simplified (the same way from both sides, so neighbours keep matching), its top a fan
    raised by its thickness (thinner towards r_min, where the plates give out), its sides down to the line it shares
    with its neighbour (so each crack is a V, closed at the bottom) and in each crack a strip of glow. A top whose fan
    would fold over (its middle cannot see its whole outline, or the outline crosses a groove) is cut into flat
    triangles instead, or failing that drawn with a second ring of points on the surface; a cell that is not one
    simple patch keeps its own triangles. Smaller plates in the hollows. Tops into m (solid), sides into edges (lit),
    glow into m (glow)."""
    stats = {} if stats is None else stats
    C = V[T].mean(1)
    rloc, _ = field.nearest(C)
    # smaller plates where the surface is hollow (a crotch, the fillet where a root or a limb leaves), so they follow
    # the saddle there instead of folding across it: the most it bends inwards, from how its normal turns a little
    # way off in four directions
    Cn = field.normal(C)
    t1 = np.cross(Cn, _Y)
    t1[np.linalg.norm(t1, axis=1) < 0.3] = np.cross(Cn[np.linalg.norm(t1, axis=1) < 0.3], _X)
    t1 /= np.linalg.norm(t1, axis=1)[:, None]
    t2 = np.cross(Cn, t1)
    eps = 0.04
    kmin = np.full(len(C), np.inf)
    for d in (t1, t2, (t1 + t2) / math.sqrt(2), (t1 - t2) / math.sqrt(2)):
        kmin = np.minimum(kmin, np.einsum("ij,ij->i", field.normal(C + d * eps) - Cn, d) / eps)
    shrink = 1.0 - 0.4 * np.clip((-kmin - 3.0) / 6.0, 0.0, 1.0)
    seeds = []
    grid = {}
    g = 0.3
    for p in pre_seeds:                           # (seeds given: a few round each end where the plates give out)
        p = np.asarray(p, float)
        seeds.append(p)
        grid.setdefault(tuple(np.floor(p / g).astype(int)), []).append(p)
    for i in rnd.permutation(len(C)):
        sp = spacing(rloc[i]) * shrink[i]
        key = tuple(np.floor(C[i] / g).astype(int))
        ok = True
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for q in grid.get((key[0] + dx, key[1] + dy, key[2] + dz), ()):
                        if np.linalg.norm(C[i] - q) < sp:
                            ok = False
                            break
                    if not ok:
                        break
                if not ok:
                    break
            if not ok:
                break
        if ok:
            seeds.append(C[i])
            grid.setdefault(key, []).append(C[i])
    _draw_cells(m, edges, field, V, T, np.array(seeds), rnd, gap, thick, r_min, plate_mat, edge_mat, glow_mat, solid, lit, glow, tol, stats,
                 bend, short)
    return stats


def _draw_cells(m, edges, field, V, T, S, rnd, gap, thick, r_min, plate_mat, edge_mat, glow_mat, solid, lit, glow, tol, stats, bend, short):
    """The rest of plates(): the plates of the cells of the seeds S."""
    _, vs = cKDTree(S).query(V)
    # each triangle cut where its corners are nearer different seeds, along the planes halfway between them
    tris, cell = [], []
    for t in T:
        P = [V[i] for i in t]
        owners = sorted(set(int(vs[i]) for i in t))
        if len(owners) == 1:
            tris.append(P)
            cell.append(owners[0])
            continue
        for a in owners:
            poly = P
            for b in owners:
                if b == a:
                    continue
                nrm = S[b] - S[a]
                poly = _clip_plane(poly, nrm, float(nrm @ ((S[a] + S[b]) / 2)))
                if len(poly) < 3:
                    break
            for i in range(1, len(poly) - 1):
                tris.append([poly[0], poly[i], poly[i + 1]])
                cell.append(a)
    V, T, good = _weld(tris)
    T = T[good]
    cell = np.array(cell)[good]
    N = field.normal(V)
    TL, cl = T.tolist(), cell.tolist()
    # the edges, which cells meet at each corner, the lines between cells
    ef = {}
    for f, t in enumerate(TL):
        for i in range(3):
            a, b = t[i], t[(i + 1) % 3]
            ef.setdefault((a, b) if a < b else (b, a), []).append(f)
    # a cell is the seed's own patch: any stray piece of it (on a branch beside it, across a crotch) given to the cell
    # it shares the most edge with
    nbf = [[] for _ in range(len(TL))]
    for fs in ef.values():
        if len(fs) == 2:
            nbf[fs[0]].append(fs[1])
            nbf[fs[1]].append(fs[0])
    moved = 0
    for _ in range(4):
        comp = [-1] * len(TL)
        pieces = {}
        for f0 in range(len(TL)):
            if comp[f0] >= 0:
                continue
            stack, cur = [f0], []
            comp[f0] = f0
            while stack:
                f = stack.pop()
                cur.append(f)
                for g2 in nbf[f]:
                    if comp[g2] < 0 and cl[g2] == cl[f0]:
                        comp[g2] = f0
                        stack.append(g2)
            pieces.setdefault(cl[f0], []).append(cur)
        changed = 0
        for c, comps in pieces.items():
            if len(comps) < 2:
                continue
            comps.sort(key=len, reverse=True)
            for cur in comps[1:]:
                votes = {}
                for f in cur:
                    for g2 in nbf[f]:
                        if cl[g2] != c:
                            votes[cl[g2]] = votes.get(cl[g2], 0) + 1
                if votes:
                    to = max(sorted(votes), key=lambda x: votes[x])
                    for f in cur:
                        cl[f] = to
                    changed += 1
        moved += changed
        if not changed:
            break
    stats["moved"] = moved
    vcells = [set() for _ in range(len(V))]
    for f, t in enumerate(TL):
        for v in t:
            vcells[v].add(cl[f])
    vopen = set()
    pair_edges = {}
    for (a, b), fs in ef.items():
        cs = sorted(set(cl[f] for f in fs))
        if len(fs) == 1:
            pair_edges.setdefault((-1, cs[0]), []).append((a, b))
            vopen.update((a, b))
        elif len(cs) >= 2:
            for x in range(len(cs)):
                for y in range(x + 1, len(cs)):
                    pair_edges.setdefault((cs[x], cs[y]), []).append((a, b))
    stop = set(v for v in range(len(V)) if len(vcells[v]) >= 3 or (v in vopen and len(vcells[v]) >= 2))
    keep = set(stop)
    chains = []                                   # (pair, kept vertices in order)
    for pair, es in pair_edges.items():
        adj = {}
        for a, b in es:
            adj.setdefault(a, []).append(b)
            adj.setdefault(b, []).append(a)
        ends = set(v for v, nb in adj.items() if len(nb) != 2 or v in stop)
        used = set()

        def walk(v0, v1):
            path = [v0, v1]
            used.add((min(v0, v1), max(v0, v1)))
            while path[-1] not in ends and path[-1] != v0:
                nxt = [w for w in adj[path[-1]] if (min(path[-1], w), max(path[-1], w)) not in used]
                if not nxt:
                    break
                used.add((min(path[-1], nxt[0]), max(path[-1], nxt[0])))
                path.append(nxt[0])
            return path
        paths = []
        for v in sorted(ends):
            for w in adj[v]:
                if (min(v, w), max(v, w)) not in used:
                    paths.append(walk(v, w))
        for v in sorted(adj):                     # closed loops with no end on them
            for w in adj[v]:
                if (min(v, w), max(v, w)) not in used:
                    ends.add(v)
                    keep.add(v)
                    paths.append(walk(v, w))
        for path in paths:
            P = V[path]
            kept = [path[i] for i in _dp(P, tol)]
            keep.update(kept)
            chains.append((pair, kept))
    # sides shorter than `short` (two corners all but on top of each other) closed up, their ends made one point, so
    # no plate has a sliver of a side to draw in
    alias = {}

    def root(v):
        while v in alias:
            v = alias[v]
        return v
    V = V.copy()
    for pair, kept in chains:
        for i in range(len(kept) - 1):
            a, b = root(kept[i]), root(kept[i + 1])
            if a != b and np.linalg.norm(V[a] - V[b]) < short:
                V[a] = (V[a] + V[b]) / 2
                N[a] = unit(N[a] + N[b])
                alias[b] = a

    def squeeze(seq, closed):
        out = []
        for v in seq:
            v = root(v)
            if not out or out[-1] != v:
                out.append(v)
        if closed and len(out) > 1 and out[0] == out[-1]:
            out.pop()
        return out
    chains = [(pair, squeeze(kept, False)) for pair, kept in chains]
    stats["closed_up"] = len(alias)
    # each cell's outline, as its triangles go round it
    cell_faces = {}
    for f, c in enumerate(cl):
        cell_faces.setdefault(c, []).append(f)
    fallback = 0
    cells = []
    for c in sorted(cell_faces):
        fs = cell_faces[c]
        nxt = {}
        for f in fs:
            t = TL[f]
            for i in range(3):
                a, b = t[i], t[(i + 1) % 3]
                if not any(g2 != f and cl[g2] == c for g2 in ef[(a, b) if a < b else (b, a)]):
                    nxt.setdefault(a, []).append(b)
        loops, seen = [], set()
        for a0 in list(nxt):
            for b0 in nxt[a0]:
                if (a0, b0) in seen:
                    continue
                loop, cur = [a0], b0
                seen.add((a0, b0))
                while cur != a0:
                    loop.append(cur)
                    cand = [b for b in nxt.get(cur, []) if (cur, b) not in seen]
                    if not cand:
                        break
                    seen.add((cur, cand[0]))
                    cur = cand[0]
                if cur == a0 and len(loop) >= 3:
                    loops.append(loop)
        if len(loops) > 1:
            stats["multi"] = stats.get("multi", 0) + 1
        while len(loops) > 1:                     # one outline touching itself at a corner: walked as one
            for x in range(1, len(loops)):
                common = set(loops[0]) & set(loops[x])
                if common:
                    v = min(common)
                    A, B = loops[0], loops[x]
                    A = A[A.index(v):] + A[:A.index(v)]
                    B = B[B.index(v):] + B[:B.index(v)]
                    loops[0] = A + B
                    del loops[x]
                    break
            else:
                break
        w = np.array([np.linalg.norm(np.cross(V[TL[f][1]] - V[TL[f][0]], V[TL[f][2]] - V[TL[f][0]])) for f in fs])
        ctr = (V[T[fs]].mean(1) * w[:, None]).sum(0) / max(w.sum(), 1e-12)
        # (the middle the plate's fan comes from: the corner of the cell's own nearest that, so it is always on the
        # plate even when the cell is a crescent and its centroid falls outside it)
        cv = np.array(sorted(set(v for f in fs for v in TL[f])))
        inner_v = cv[[len(vcells[v]) == 1 and v not in vopen for v in cv]]
        pool = inner_v if len(inner_v) else cv
        ctr = V[pool[int(np.argmin(np.linalg.norm(V[pool] - ctr, axis=1)))]]
        ring = squeeze([v for v in max(loops, key=len) if v in keep], True) if loops else []
        cells.append(dict(c=c, fs=fs, nxt=nxt, loops=loops, ring=ring, ctr=ctr))
    if not cells:
        return stats
    # the cells' middles put on the surface, all at once
    CT = field.project(np.array([e["ctr"] for e in cells]))
    NC = field.normal(CT)
    RH, _ = field.nearest(CT)
    inner_raw = []
    for e, ctr, nc, r_here in zip(cells, CT, NC, RH):
        e["ctr"], e["nc"] = ctr, nc
        e["t"] = thick * rnd.uniform(0.8, 1.15) * float(np.clip((r_here - r_min) / 0.07, 0.25, 1.0))
        ring = e["ring"]
        e["ok"] = len(e["loops"]) == 1 and len(ring) >= 3
        e["inner"] = None
        if e["ok"]:
            e["inner"] = len(inner_raw)
            inner_raw += [ctr + (V[v] - ctr) * 0.5 for v in ring]
    IR = np.array(inner_raw) if inner_raw else np.zeros((0, 3))
    IP = field.project(IR) if inner_raw else IR
    IN = field.normal(IP) if inner_raw else IR
    # a plate over a strongly curved patch (a crotch, a fillet), where the surface half way out from its middle is
    # more than `bend` off the straight line, gets a second ring of points there, so its top follows the surface
    # instead of cutting across it
    for e in cells:
        e["curved"] = False
        if e["inner"] is not None:
            o, k = e["inner"], len(e["ring"])
            e["curved"] = float(np.max(np.linalg.norm(IP[o:o + k] - IR[o:o + k], axis=1))) > bend
    plan = []
    for e in cells:
        fs, ring, ctr, nc, t_c = e["fs"], e["ring"], e["ctr"], e["nc"], e["t"]
        if not e["ok"]:
            # (rare: a cell in two pieces or with a hole) its own triangles raised, its sides along every edge
            fallback += 1
            stats.setdefault("why", []).append((len(e["loops"]), len(ring), len(fs), np.round(ctr, 2).tolist()))
            _raw_plate(m, edges, V, N, TL, fs, e["nxt"], ctr, nc, t_c, gap, plate_mat, edge_mat, solid, lit)
            continue
        L, Nn, k = V[ring], N[ring], len(ring)
        sgn = 1.0 if sum(float(np.cross(L[i] - ctr, L[(i + 1) % k] - ctr) @ nc) for i in range(k)) > 0 else -1.0
        tops, feet = [], []
        for i in range(k):
            n_i = Nn[i]
            e1, e2 = unit(L[i] - L[i - 1]), unit(L[(i + 1) % k] - L[i])
            in1, in2 = unit(np.cross(n_i, e1)) * sgn, unit(np.cross(n_i, e2)) * sgn
            bis = unit(in1 + in2)
            ins = (gap / 2 + min(0.008, 0.25 * t_c)) / max(0.6, float(bis @ in1))
            ins = min(ins, 0.35 * float(np.linalg.norm(ctr - L[i])))
            tops.append(L[i] + bis * ins + n_i * t_c)
            feet.append(L[i] - n_i * 0.03)
        _uncross(tops, L)
        _undip(tops, nc)
        o = e["inner"]
        mids = [IP[o + i] + IN[o + i] * t_c * 1.05 for i in range(k)]
        _uncross(mids, L)
        ct = ctr + nc * t_c * 1.1
        ax = unit(np.cross(nc, _Y)) if abs(nc[1]) < 0.9 else unit(np.cross(nc, _X))
        ay = np.cross(nc, ax)
        allp = np.array(tops + mids + [ct])
        us, ws = (allp - ct) @ ax, (allp - ct) @ ay
        span = max(us.max() - us.min(), ws.max() - ws.min(), 1e-6)
        uv = [((u - us.min()) / span, (x - ws.min()) / span) for u, x in zip(us, ws)]
        g = dict(L=L, k=k, sgn=sgn, tops=tops, feet=feet, mids=mids, ct=ct, nc=nc, uv_t=uv[:k], uv_m=uv[k:2 * k], uv_c=uv[2 * k],
                 cn=[unit(nc * 0.6 + n * 0.4) for n in Nn], mcn=[unit(nc * 0.5 + IN[o + i] * 0.5) for i in range(k)])
        plan.append((e, g, _top(g, e["curved"])))
    # every top checked at once: each of its triangles must face out the way the surface under it does (a fan from a
    # middle that cannot see the whole outline would fold over); one that does not tries the other way of drawing it,
    # then being cut into triangles, then its own triangles
    def check(items):
        cs, ns, own = [], [], []
        for n_, quads in enumerate(items):
            for P4, _, _ in quads:
                for tri in ((0, 1, 2), (0, 2, 3)):
                    A, B, C = P4[tri[0]], P4[tri[1]], P4[tri[2]]
                    nn = np.cross(B - A, C - A)
                    if np.linalg.norm(nn) < 1e-9:
                        continue
                    cs.append((A + B + C) / 3)
                    ns.append(unit(nn))
                    own.append((n_, float(np.linalg.norm(nn)) / 2))
        good = [True] * len(items)
        if cs:
            d = np.abs(np.einsum("ij,ij->i", np.array(ns), field.normal(np.array(cs))))
            for (n_, area), x in zip(own, d):
                # (a sliver at the edge may stand steep unseen; a triangle of any size may not)
                if x < 0.15 or (x < 0.4 and area > 0.0012):
                    good[n_] = False
        return good
    good = check([q for _, _, q in plan])
    retry = [i for i, ok in enumerate(good) if not ok]
    alt = [_top(plan[i][1], not plan[i][0]["curved"]) for i in retry]
    good2 = check(alt)
    folded = 0
    for n_, i in enumerate(retry):
        e, g, fan = plan[i]
        folded += 1
        # first choice: cut into triangles flat in the plate (if they all lie along the surface, above it)
        ears = _ear_clip(g["L"], g["nc"], g["sgn"])
        if ears is not None:
            t_ = g["tops"]
            quads = [([t_[a], t_[b], t_[c], t_[c]], [g["uv_t"][a], g["uv_t"][b], g["uv_t"][c], g["uv_t"][c]],
                      [g["cn"][a], g["cn"][b], g["cn"][c], g["cn"][c]]) for a, b, c in ears]
            TC = np.array([(P4[0] + P4[1] + P4[2]) / 3 for P4, _, _ in quads])
            if check([quads])[0] and np.min(field.at(TC)) >= 0.25 * e["t"]:
                plan[i] = (e, g, quads)
                continue
        # then the other way of drawing the top; failing both, the fan as it was (one steep sliver beats a ragged
        # plate)
        if good2[n_]:
            plan[i] = (e, g, alt[n_])
    for e, g, quads in plan:
        if quads is None:
            fallback += 1
            _raw_plate(m, edges, V, N, TL, e["fs"], e["nxt"], e["ctr"], e["nc"], e["t"], gap, plate_mat, edge_mat, solid, lit)
            continue
        for P4, UV4, CN4 in quads:
            m.oquad(P4, g["nc"], plate_mat, UV4, solid, cn=CN4)
        k, L, tops, feet = g["k"], g["L"], g["tops"], g["feet"]
        for i in range(k):
            j = (i + 1) % k
            _quad(edges, [tops[j], tops[i], feet[i], feet[j]], (L[i] + L[j]) / 2 - e["ctr"], edge_mat, [(0, 1), (1, 1), (1, 0), (0, 0)], lit,
                  ao=False)
    stats["curved"] = sum(1 for e in cells if e.get("curved"))
    stats["folded"] = folded
    # the glow in each crack: a strip along every simplified side two plates share
    strips = 0
    for (a, b), kept in chains:
        if a < 0:
            continue
        for i in range(len(kept) - 1):
            p, q = V[kept[i]], V[kept[i + 1]]
            n_p, n_q = N[kept[i]], N[kept[i + 1]]
            e = unit(q - p)
            if np.linalg.norm(q - p) < 0.04:      # (a stub at a corner: the walls round it show the glow well enough)
                continue
            sp, sq = unit(np.cross(n_p, e)) * 0.011, unit(np.cross(n_q, e)) * 0.011
            P = [p + n_p * 0.006 - sp, q + n_q * 0.006 - sq, q + n_q * 0.006 + sq, p + n_p * 0.006 + sp]
            L = float(np.linalg.norm(q - p)) / 0.3
            m.oquad(P, (n_p + n_q) / 2, glow_mat, [(0, 0), (L, 0), (L, 0.06), (0, 0.06)], glow)
            strips += 1
    stats.update(seeds=len(S), fallback=fallback, strips=strips, tris=len(T))


def _ear_clip(L, n, sgn):
    """The polygon L (seen along n, going round the way sgn says) cut into triangles (index triples), or None."""
    ax = unit(np.cross(n, _Y)) if abs(n[1]) < 0.9 else unit(np.cross(n, _X))
    ay = np.cross(n, ax)
    P = [(float(p @ ax), float(p @ ay) * sgn) for p in L]
    idx = list(range(len(P)))
    out = []

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    guard = 0
    while len(idx) > 3 and guard < 400:
        guard += 1
        for j in range(len(idx)):
            a, b, c = idx[j - 1], idx[j], idx[(j + 1) % len(idx)]
            if cross(P[a], P[b], P[c]) <= 1e-12:
                continue
            if any(cross(P[a], P[b], P[x]) >= 0 and cross(P[b], P[c], P[x]) >= 0 and cross(P[c], P[a], P[x]) >= 0
                   for x in idx if x not in (a, b, c)):
                continue
            out.append((a, b, c))
            del idx[j]
            break
        else:
            return None
    if len(idx) == 3:
        out.append(tuple(idx))
    return out


def _undip(P, n, most=0.008):
    """A corner of a plate's outline dipping well below both its neighbours (where the outline crosses a groove) lifted
    level with them, so the plate stays a stiff plate across the groove instead of folding down into it."""
    k = len(P)
    for _ in range(2):
        h = [float(p @ n) for p in P]
        for i in range(k):
            a, b = h[i - 1], h[(i + 1) % k]
            if h[i] < min(a, b) - most:
                lift = min((a + b) / 2 - h[i], 0.02)
                P[i] = P[i] + n * lift
                h[i] += lift


def _uncross(P, L):
    """Where drawing the ring L in has turned one of its sides round (a short side between two sharp corners), that
    side's ends brought together, so the ring never crosses itself."""
    k = len(P)
    for _ in range(3):
        bad = False
        for i in range(k):
            j = (i + 1) % k
            if float((P[j] - P[i]) @ (L[j] - L[i])) <= 0:
                P[i] = P[j] = (P[i] + P[j]) / 2
                bad = True
        if not bad:
            break


def _convex(q, n, eps=1e-9):
    """Whether the quad q (its corners in order) is convex seen along n."""
    s = [float(np.cross(q[(i + 1) % 4] - q[i], q[(i + 2) % 4] - q[(i + 1) % 4]) @ n) for i in range(4)]
    return all(x > eps for x in s) or all(x < -eps for x in s)


def _top(g, ring):
    """A plate's top as quads (corners, uvs, normals): a fan from its middle out to its outline, or (ring) out to a
    ring half way and a band from there to the outline; two triangles to a quad where they make a convex one (so it
    comes out the same whichever way the quad is split), else one each."""
    k, ct, nc, uv_c = g["k"], g["ct"], g["nc"], g["uv_c"]
    tops, uv_t, cn = g["tops"], g["uv_t"], g["cn"]
    if ring:
        mids, uv_m, mcn = g["mids"], g["uv_m"], g["mcn"]
    else:
        mids, uv_m, mcn = tops, uv_t, cn
    out = []
    i = 0
    while i < k:
        a, b, c = i, (i + 1) % k, (i + 2) % k
        q = [ct, mids[a], mids[b], mids[c]]
        if i + 2 <= k and _convex(q, nc):
            out.append((q, [uv_c, uv_m[a], uv_m[b], uv_m[c]], [nc, mcn[a], mcn[b], mcn[c]]))
            i += 2
        else:
            out.append(([ct, mids[a], mids[b], mids[b]], [uv_c, uv_m[a], uv_m[b], uv_m[b]], [nc, mcn[a], mcn[b], mcn[b]]))
            i += 1
    if ring:
        for i in range(k):
            j = (i + 1) % k
            q = [tops[i], tops[j], mids[j], mids[i]]
            quv, qcn = [uv_t[i], uv_t[j], uv_m[j], uv_m[i]], [cn[i], cn[j], mcn[j], mcn[i]]
            if _convex(q, nc):
                out.append((q, quv, qcn))
            else:
                for x, y, z in ((0, 1, 2), (0, 2, 3)):
                    out.append(([q[x], q[y], q[z], q[z]], [quv[x], quv[y], quv[z], quv[z]], [qcn[x], qcn[y], qcn[z], qcn[z]]))
    return out


def _quad(mesh, q, want, mat, uv, kind, cn=None, ao=True):
    """A quad, or (where it is not convex, so would come out differently split one way or the other) its two
    triangles."""
    q = [np.asarray(p, float) for p in q]
    nq = np.cross(q[2] - q[0], q[3] - q[1])
    if np.linalg.norm(nq) < 1e-12 or _convex(q, nq):
        mesh.oquad(q, want, mat, uv, kind, cn=cn, ao=ao)
        return
    for a, b, c in ((0, 1, 2), (0, 2, 3)):
        mesh.oquad([q[a], q[b], q[c], q[c]], want, mat, [uv[a], uv[b], uv[c], uv[c]], kind,
                   cn=None if cn is None else [cn[a], cn[b], cn[c], cn[c]], ao=ao)


def _raw_plate(m, edges, V, N, TL, fs, nxt, ctr, nc, t_c, gap, plate_mat, edge_mat, solid, lit):
    """A cell's own triangles raised by t_c (its rim drawn in by half the crack), its sides along every rim edge."""
    rim = [(a, b) for a, bs in nxt.items() for b in bs]
    on_rim = set(a for a, _ in rim) | set(b for _, b in rim)
    verts = sorted(set(v for f in fs for v in TL[f]))
    top = {}
    for v in verts:
        n = N[v]
        if v in on_rim:
            w = ctr - V[v]
            inward = unit(w - n * (w @ n))
            top[v] = V[v] + inward * (gap / 2 + min(0.008, 0.25 * t_c)) + n * t_c
        else:
            top[v] = V[v] + n * t_c
    ax = unit(np.cross(nc, _Y)) if abs(nc[1]) < 0.9 else unit(np.cross(nc, _X))
    ay = np.cross(nc, ax)
    P = V[verts]
    us, ws = (P - ctr) @ ax, (P - ctr) @ ay
    span = max(us.max() - us.min(), ws.max() - ws.min(), 1e-6)
    uv = {v: ((u - us.min()) / span, (x - ws.min()) / span) for v, u, x in zip(verts, us, ws)}
    for f in fs:
        a, b, c = TL[f]
        m.oquad([top[a], top[b], top[c], top[c]], N[[a, b, c]].mean(0), plate_mat, [uv[a], uv[b], uv[c], uv[c]], solid)
    for a, b in rim:
        edges.oquad([top[b], top[a], V[a] - N[a] * 0.03, V[b] - N[b] * 0.03], (V[a] + V[b]) / 2 - ctr, edge_mat,
                    [(0, 1), (1, 1), (1, 0), (0, 0)], lit, ao=False)
