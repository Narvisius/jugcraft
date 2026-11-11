"""The Holo Projector's head (work list item 15, approved by Justin on 23 October 2026 after six takes; see
claude/jugcraft-tripillar-holo-head.md): a big hologram of his character's face, angry, looking down at you.

It is a real human head, not a sculpt from primitives (Justin: "make it more look real ... model it off a real person's
face"): the MakeHuman base mesh made a young woman with its female race targets (both released CC0 in September 2020;
tools/data/makehuman_head.npz is the head's part of them, see tools/data/README.md), shaped into her face and the
expression with smooth brushes (`feminine`, `angry`: slimmer cheeks, a narrower slightly pointed chin, tight under the
jaw, a finer nose; brows down and drawn in, heavy lids, a snarl), subdivided once (Catmull-Clark), its neck cut level
under the choker and carried on by a short skirt it fades away along. Her hair, brows, lashes and choker are
tools/holo_head_hair.py's. No picture of the character is used: the head is modelled after it.

Head units: the chin near y 0.07, the crown near 1.07, the face looking +z, x her left; 2.045 MakeHuman decimetres a
unit. `build()` gives everything the renderer needs (tools/holo_projector.py writes it out).
"""
from collections import Counter
from pathlib import Path

import numpy as np

import holo_head_hair as X

DATA = Path(__file__).resolve().parent / "data" / "makehuman_head.npz"
S = 2.045                           # MakeHuman decimetres a head unit (chin to crown)
O = np.array([0.0, 5.527, 0.587])   # dm: the head frame's origin
LABELS = ["skin", "lids", "brows", "lips", "teeth", "choker", "hair", "mouth"]
L = {k: i for i, k in enumerate(LABELS)}
NECK_CUT = 5.36          # dm: the neck's level bottom edge (above where it widens into the shoulders)
SKIRT_RINGS, SKIRT_STEP = 6, 0.03   # head units: the fading skirt below it
FADE_TOP = -0.068        # head units: fully there down to just under the choker
EYE_R = 0.143 / S * 0.97            # the eyeballs' radius (MakeHuman's, a little smaller)
GROUPS = ("body", "helper-upper-teeth", "helper-lower-teeth", "joint-l-eye", "joint-r-eye")


def load():
    """The head's part of the MakeHuman base mesh, a young woman (the female race targets, a third each, applied):
    vertices (dm), faces and each face's group, as MakeHuman's own base.obj has them."""
    z = np.load(DATA)
    v = z["v"].astype(np.float64)
    faces, groups = [], []
    for g in GROUPS:
        for f in z["f_" + g.replace("-", "_")]:
            faces.append([int(i) for i in f])
            groups.append(g)
    return v, faces, groups


def joints(v, faces, groups):
    out = {}
    for f, g in zip(faces, groups):
        if g.startswith("joint-"):
            out.setdefault(g[6:], set()).update(f)
    return {k: v[sorted(s)].mean(0) for k, s in out.items()}


def to_head(p):
    return (np.asarray(p, np.float64) - O) / S


def group_mesh(v, faces, groups, name):
    fs = [f for f, g in zip(faces, groups) if g == name]
    used = sorted({i for f in fs for i in f})
    remap = {o: n for n, o in enumerate(used)}
    return v[used].copy(), [[remap[i] for i in f] for f in fs]


def extract(v, faces, groups, keep=('body',), cut=5.2):
    out = []
    for f, g in zip(faces, groups):
        if g in keep and all(v[i][1] > cut for i in f):
            out.append(f)
    used = sorted({i for f in out for i in f})
    remap = {o: n for n, o in enumerate(used)}
    return v[used].copy(), [[remap[i] for i in f] for f in out], np.array(used)


def catmull_clark(v, faces):
    """One step of Catmull-Clark subdivision (quads and triangles in, quads out); open boundaries kept."""
    v = np.asarray(v, np.float64)
    nv = len(v)
    face_pts = np.array([v[f].mean(0) for f in faces])
    edges = {}
    for fi, f in enumerate(faces):
        n = len(f)
        for k in range(n):
            a, b = f[k], f[(k + 1) % n]
            key = (min(a, b), max(a, b))
            edges.setdefault(key, []).append(fi)
    ekeys = list(edges)
    eidx = {e: i for i, e in enumerate(ekeys)}
    edge_pts = np.zeros((len(ekeys), 3))
    boundary_e = np.zeros(len(ekeys), bool)
    for i, e in enumerate(ekeys):
        fs = edges[e]
        if len(fs) == 2:
            edge_pts[i] = (v[e[0]] + v[e[1]] + face_pts[fs[0]] + face_pts[fs[1]]) / 4
        else:
            edge_pts[i] = (v[e[0]] + v[e[1]]) / 2
            boundary_e[i] = True
    # the original points moved
    vf = [[] for _ in range(nv)]
    for fi, f in enumerate(faces):
        for i in f:
            vf[i].append(fi)
    ve = [[] for _ in range(nv)]
    for i, e in enumerate(ekeys):
        ve[e[0]].append(i)
        ve[e[1]].append(i)
    newv = np.zeros_like(v)
    for i in range(nv):
        be = [k for k in ve[i] if boundary_e[k]]
        if be:
            # a boundary point: the boundary curve's rule
            mids = [(v[ekeys[k][0]] + v[ekeys[k][1]]) / 2 for k in be]
            newv[i] = 0.75 * v[i] + 0.25 * np.mean(mids, 0) if len(be) == 2 else v[i]
            continue
        n = len(vf[i])
        if n == 0:
            newv[i] = v[i]
            continue
        F = face_pts[vf[i]].mean(0)
        R = np.mean([(v[ekeys[k][0]] + v[ekeys[k][1]]) / 2 for k in ve[i]], 0)
        newv[i] = (F + 2 * R + (n - 3) * v[i]) / n
    allv = np.concatenate([newv, edge_pts, face_pts])
    eo, fo = nv, nv + len(ekeys)
    quads = []
    for fi, f in enumerate(faces):
        n = len(f)
        for k in range(n):
            a = f[k]
            e_next = eidx[(min(a, f[(k + 1) % n]), max(a, f[(k + 1) % n]))]
            e_prev = eidx[(min(a, f[k - 1]), max(a, f[k - 1]))]
            quads.append([a, eo + e_next, fo + fi, eo + e_prev])
    return allv, quads


def tris(quads):
    t = []
    for q in quads:
        if len(q) == 4:
            t.append((q[0], q[1], q[2]))
            t.append((q[0], q[2], q[3]))
        else:
            t.append(tuple(q))
    return np.array(t, np.int32)


def normals(v, f):
    t = v[f]
    fn = np.cross(t[:, 1] - t[:, 0], t[:, 2] - t[:, 0])
    n = np.zeros_like(v)
    for k in range(3):
        np.add.at(n, f[:, k], fn)
    return n / np.maximum(np.linalg.norm(n, axis=1, keepdims=True), 1e-12)


# ------------------------------------------------------------------ her face and the expression (MakeHuman dm, the face looking +z)


def brush(v, centre, radius, move, mirror=True, mask=None):
    """Moves the vertices near centre by move, falling off smoothly (an ellipsoidal Gaussian of the given radii)."""
    c = np.asarray(centre, float)
    r = np.asarray(radius, float) * np.ones(3)
    mv = np.asarray(move, float)
    out = v.copy()
    sides = [1, -1] if mirror and abs(c[0]) > 1e-6 else [1]
    for s in sides:
        cc = c * [s, 1, 1]
        m = mv * [s, 1, 1]
        d = (v - cc) / r
        w = np.exp(-np.sum(d * d, 1) * 2.0)
        if mask is not None:
            w = w * mask
        out += w[:, None] * m
    return out


def sig(x):
    return 1 / (1 + np.exp(-np.clip(x, -60, 60)))


MOUTH_Y = 6.062


def feminine(v):
    """A finer face: a softer brow ridge, a narrower jaw and chin, a narrower nose, fuller lips."""
    v = np.asarray(v, float).copy()
    face = (v[:, 2] > 0.6).astype(float)
    v = brush(v, (0.25, 6.95, 1.33), (0.25, 0.07, 0.12), (0.0, 0.0, -0.02), mask=face)
    v = brush(v, (0.55, 5.95, 0.6), (0.16, 0.28, 0.35), (-0.045, 0.01, 0.0))
    v = brush(v, (0.42, 5.8, 0.95), (0.16, 0.16, 0.25), (-0.03, 0.01, 0.0))
    v = brush(v, (0.15, 5.75, 1.25), (0.1, 0.1, 0.15), (-0.02, 0.005, 0.0), mask=face)
    # (take 4) the cheeks in under the cheekbones, the lower face and chin a little narrower: not a round face
    v = brush(v, (0.47, 6.12, 1.02), (0.13, 0.13, 0.2), (-0.045, 0.0, -0.025))
    v = brush(v, (0.36, 6.0, 1.15), (0.1, 0.1, 0.12), (-0.022, 0.0, -0.012), mask=face)
    v = brush(v, (0.5, 5.92, 0.82), (0.16, 0.16, 0.3), (-0.03, 0.0, 0.0))
    v = brush(v, (0.13, 5.76, 1.27), (0.09, 0.09, 0.14), (-0.022, 0.0, 0.0), mask=face)
    # (take 5) the chin narrower and a little more pointed (still a chin, not a teardrop); the skin under the jaw tighter
    v = brush(v, (0.11, 5.74, 1.25), (0.07, 0.08, 0.14), (-0.022, 0.0, 0.0), mask=face)
    v = brush(v, (0.22, 5.8, 1.12), (0.09, 0.09, 0.15), (-0.02, 0.0, -0.005), mask=face)
    v = brush(v, (0.0, 5.71, 1.3), (0.06, 0.06, 0.08), (0.0, -0.01, 0.012), mirror=False, mask=face)
    v = brush(v, (0.0, 5.64, 0.86), (0.25, 0.06, 0.12), (0.0, 0.03, -0.035), mirror=False)
    v = brush(v, (0.25, 5.72, 0.8), (0.12, 0.07, 0.15), (-0.02, 0.025, -0.02))
    # a finer nose: the bridge and wings narrower, the tip smaller
    v = brush(v, (0.06, 6.55, 1.38), (0.05, 0.12, 0.08), (-0.012, 0.0, 0.0), mask=face)
    v = brush(v, (0.13, 6.31, 1.36), (0.06, 0.06, 0.1), (-0.02, 0.006, -0.008), mask=face)
    v = brush(v, (0.0, 6.37, 1.5), (0.06, 0.05, 0.06), (0.0, 0.006, -0.014), mirror=False, mask=face)
    v = brush(v, (0.27, 5.83, 1.15), (0.1, 0.1, 0.15), (-0.022, 0.0, -0.006), mask=face)
    v = brush(v, (0.16, 6.3, 1.32), (0.07, 0.07, 0.12), (-0.036, 0.0, 0.0), mask=face)
    v = brush(v, (0.0, 6.36, 1.5), (0.08, 0.07, 0.09), (0.0, 0.012, -0.024), mirror=False, mask=face)
    v = brush(v, (0.0, 6.6, 1.38), (0.06, 0.15, 0.08), (0.0, 0.0, -0.01), mirror=False, mask=face)
    return v


def angry(v):
    v = feminine(v)
    x, y, z = v[:, 0], v[:, 1], v[:, 2]
    face = (z > 0.95).astype(float)
    # brows down and in; the inner ends most (the corrugators), the outer ends a little
    v = brush(v, (0.12, 6.9, 1.34), (0.13, 0.11, 0.16), (-0.03, -0.075, 0.025), mask=face)
    v = brush(v, (0.3, 6.93, 1.27), (0.14, 0.09, 0.2), (0.0, -0.045, 0.012), mask=face)
    v = brush(v, (0.46, 6.92, 1.12), (0.1, 0.08, 0.2), (0.0, -0.012, 0.0), mask=face)
    # two short vertical furrows between the brows, and the skin between bunched forward
    v = brush(v, (0.04, 6.88, 1.36), (0.022, 0.07, 0.08), (0.004, 0.0, -0.016), mask=face)
    v = brush(v, (0.0, 6.86, 1.36), (0.03, 0.06, 0.08), (0.0, 0.0, 0.01), mirror=False, mask=face)
    # heavy upper lids (a hooded glare) and tensed lower lids
    v = brush(v, (0.27, 6.79, 1.24), (0.13, 0.05, 0.12), (0.0, -0.085, 0.006), mask=face)
    v = brush(v, (0.28, 6.575, 1.22), (0.12, 0.045, 0.12), (0.0, 0.036, 0.006), mask=face)
    # the cheeks lifted a little (a squint), the nose wrinkled, the nostrils flared
    v = brush(v, (0.42, 6.45, 1.2), (0.14, 0.1, 0.16), (0.0, 0.016, 0.01), mask=face)
    v = brush(v, (0.08, 6.52, 1.4), (0.07, 0.07, 0.1), (0.0, 0.012, -0.004), mask=face)
    v = brush(v, (0.15, 6.32, 1.33), (0.07, 0.06, 0.1), (0.006, 0.012, 0.0), mask=face)
    # the snarl: the upper lip lifted, most over the canines; the lower lip down; the corners pulled down
    x, y, z = v[:, 0], v[:, 1], v[:, 2]
    mouth = ((np.abs(x) < 0.34) & (y > 5.85) & (y < 6.32) & (z > 1.1)).astype(float)
    up = sig((y - MOUTH_Y) / 0.004) * mouth
    lo = (1 - sig((y - MOUTH_Y) / 0.004)) * mouth
    v = brush(v, (0.0, 6.11, 1.44), (0.13, 0.08, 0.2), (0.0, 0.03, -0.012), mirror=False, mask=up)
    v = brush(v, (0.11, 6.12, 1.42), (0.07, 0.07, 0.2), (0.0, 0.045, -0.01), mask=up)
    v = brush(v, (0.0, 6.02, 1.42), (0.16, 0.08, 0.2), (0.0, -0.036, -0.022), mirror=False, mask=lo)
    v = brush(v, (0.25, 6.06, 1.3), (0.07, 0.07, 0.12), (0.022, -0.016, -0.014), mask=face)
    v = brush(v, (0.0, 5.82, 1.33), (0.12, 0.08, 0.15), (0.0, 0.012, 0.006), mirror=False, mask=face)
    return v


def clip_below(v, tri, y0):
    """The triangles' parts above the plane y = y0 (crossing triangles split; the new points shared along each edge)."""
    v = np.asarray(v, float)
    above = v[:, 1] >= y0
    newv = list(v)
    cache = {}

    def cut(a, b):
        key = (min(a, b), max(a, b))
        if key not in cache:
            pa, pb = v[a], v[b]
            t = (y0 - pa[1]) / (pb[1] - pa[1])
            newv.append(pa + (pb - pa) * t)
            cache[key] = len(newv) - 1
        return cache[key]

    out = []
    for t3 in tri:
        ab = [above[i] for i in t3]
        n = sum(ab)
        if n == 3:
            out.append(tuple(t3))
        elif n == 0:
            continue
        else:
            # rotate so the odd one out is first, keeping the winding
            for r in range(3):
                tt = (t3[r], t3[(r + 1) % 3], t3[(r + 2) % 3])
                aa = [above[i] for i in tt]
                if (n == 1 and aa[0]) or (n == 2 and not aa[0]):
                    break
            a, b, c = tt
            if n == 1:          # a above: one smaller triangle
                out.append((a, cut(a, b), cut(a, c)))
            else:               # a below: a quad, as two triangles
                pab, pac = cut(a, b), cut(a, c)
                out.append((pab, b, c))
                out.append((pab, c, pac))
    newv = np.array(newv)
    used = np.unique(np.array(out))
    remap = -np.ones(len(newv), int)
    remap[used] = np.arange(len(used))
    return newv[used], remap[np.array(out)]


# ------------------------------------------------------------------ the whole head

def split(v, tri, rounds=1):
    """Each triangle into four (its edges halved), `rounds` times: the same shape in smaller pieces (so what of it is
    seen can be told finely)."""
    v = [np.asarray(p, float) for p in v]
    tri = [tuple(t) for t in tri]
    for _ in range(rounds):
        mid = {}

        def m(a, b):
            key = (min(a, b), max(a, b))
            if key not in mid:
                v.append((v[a] + v[b]) / 2)
                mid[key] = len(v) - 1
            return mid[key]
        out = []
        for a, b, c in tri:
            ab, bc, ca = m(a, b), m(b, c), m(c, a)
            out += [(a, ab, ca), (ab, b, bc), (ca, bc, c), (ab, bc, ca)]
        tri = out
    return np.array(v), np.array(tri, np.int32)


def skirt(skin_v, tri):
    """A short straight skirt under the neck's level edge (SKIRT_RINGS rings, SKIRT_STEP apart), for the fade."""
    cnt = Counter()
    for a, b, c in tri:
        for e in ((a, b), (b, c), (c, a)):
            cnt[(min(e), max(e))] += 1
    edge = [e for e, k in cnt.items() if k == 1]
    loop_v = np.array(sorted({i for e in edge for i in e}))
    ring = skin_v[loop_v]
    ctr = ring.mean(0)
    order = np.argsort(np.arctan2(ring[:, 0] - ctr[0], ring[:, 2] - ctr[2]))
    ring = ring[order]
    nr = len(ring)
    rings = []
    for j in range(SKIRT_RINGS + 1):
        rr = ctr + (ring - ctr) * np.array([1.0, 0, 1.0])
        rr[:, 1] = ring[:, 1] - j * SKIRT_STEP
        rings.append(rr)
    sv = np.concatenate(rings)
    sf = []
    for j in range(SKIRT_RINGS):
        for i in range(nr):
            a, b = j * nr + i, j * nr + (i + 1) % nr
            c, d = a + nr, b + nr
            sf += [(a, b, d), (a, d, c)]
    sf = np.array(sf)
    t_ = sv[sf]
    fn = np.cross(t_[:, 1] - t_[:, 0], t_[:, 2] - t_[:, 0])
    out = t_.mean(1) - ctr
    out[:, 1] = 0
    flip = np.sum(fn * out, 1) < 0
    sf[flip] = sf[flip][:, [0, 2, 1]]
    return sv, sf


def build():
    """The head: one mesh of triangles (vertices, triangles anticlockwise outwards, each vertex's label, normal, kind
    (0 a surface, 1 a strand), brightness, lip colour weight and fade alpha), the hair's cards (per group: points along
    each card, widths, the facing normals, two-sided or not, texture variants) and the eyes' middles. Head units."""
    v, faces, groups = load()
    v = angry(v)
    j = joints(v, faces, groups)
    hv, hf, used = extract(v, faces, groups, cut=5.2)
    hv, hf = catmull_clark(hv, hf)
    tri = tris(hf)
    # a clean, level bottom to the neck: the mesh cut through at one height (faces split where they cross it)
    hv, tri = clip_below(hv, tri, NECK_CUT)
    # labels by place (dm): the lips, the mouth's inside
    x, y, z = hv[:, 0], hv[:, 1], hv[:, 2]
    lab = np.full(len(hv), L["skin"], np.int8)
    nrm_dm = normals(hv.astype(np.float32), tri)
    # the lips' colour: between the mouth's line and each lip's border (a curve, highest in the middle), soft at the borders
    seam = MOUTH_Y + 0.004
    up_b = seam + 0.1 * np.clip(1 - (x / 0.235) ** 2, 0, 1) ** 0.7 - 0.012 * np.exp(-(x / 0.03) ** 2)
    lo_b = seam - 0.095 * np.clip(1 - (x / 0.215) ** 2, 0, 1) ** 0.8
    front = (z > 1.18) & (nrm_dm[:, 2] > 0.1)
    lw = np.clip(np.minimum((up_b - y) / 0.012, (y - lo_b) / 0.012), 0, 1) * np.clip((0.25 - np.abs(x)) / 0.03, 0, 1) * front
    # the mouth's inside: in the mouth's box, facing back or deep behind the lips
    inside = (np.abs(x) < 0.24) & (y > 5.9) & (y < 6.24) & (z > 0.5) & ((nrm_dm[:, 2] < -0.05) | (z < 1.3 - 0.4 * x ** 2))
    lab[inside] = L["mouth"]
    skin_v = to_head(hv)
    skin_n = normals(skin_v.astype(np.float32), tri)
    parts = [(skin_v, tri, lab, 0, None)]
    for g in ("helper-upper-teeth", "helper-lower-teeth"):
        gv, gf = group_mesh(v, faces, groups, g)
        tv, tf = split(to_head(gv), tris(gf), 2)
        parts.append((tv, tf, L["teeth"], 0, None))
    eyes = np.array([to_head(j["l-eye"]), to_head(j["r-eye"])])
    # the lash lines' roots: the lids' edges, where the skin meets each eyeball
    roots = []
    for eye in eyes:
        up_m, lo_m = X.lid_margins(skin_v, eye, EYE_R)
        roots.append((up_m, True, eye))
        roots.append((lo_m, False, eye))
    scalp = X.Scalp(skin_v)
    bv, bf = X.base_layer(skin_v, tri, skin_n)
    parts.append((bv, bf, L["hair"], 0, np.full(len(bv), 0.8)))
    # the base layer fades out at the hairline (no hard edge where the hair stops)
    base_fa = np.clip((bv[:, 1] - X.hairline_y(X.unit(bv - X.C))) / 0.07, 0, 1) * 0.85 + 0.15
    n_before_base = sum(len(pp[0]) for pp in parts[:-1])
    # the hair as cards (soft locks with a strand texture), drawn textured; the brows as two cards each
    cards = {}
    for name, (p, w, n), two in (("swept", X.cards_swept(scalp), False), ("bun", X.cards_bun(), False), ("loose", X.cards_loose(), True),
                                 ("bangs", X.cards_bangs(scalp, skin_v), True), ("side", X.cards_side(scalp, skin_v), True),
                                 ("brows", X.brow_cards(skin_v, skin_n), False)):
        cards[name] = {"p": np.asarray(p, np.float32), "w": np.asarray(w, np.float32), "n": np.asarray(n, np.float32), "two": bool(two),
                       "var": X.RNG.integers(0, 6, len(p)).astype(np.int8)}
    # the lashes: fine ribbons
    p, w, n = X.lashes(roots, eyes)
    rv, rf, sid = X.ribbons(p, w, n, two_sided=True)
    parts.append((rv, rf, L["lids"], 1, X.RNG.uniform(0.72, 1.22, size=sid.max() + 1)[sid]))
    # under the neck's level edge, the skirt it fades away along
    sv, sf = skirt(skin_v, tri)
    skirt_at = sum(len(pp[0]) for pp in parts)
    parts.append((sv, sf, L["skin"], 0, None))
    cv, cf = X.choker(skin_v, y=-0.04)
    parts.append((cv, cf, L["choker"], 0, None))
    vs, fs, ls, ns, ks, shs = [], [], [], [], [], []
    off = 0
    for pv, pf, plab, kind, sh in parts:
        pv = np.asarray(pv, np.float32)
        vs.append(pv)
        fs.append(np.asarray(pf) + off)
        ls.append(np.full(len(pv), plab, np.int8) if np.isscalar(plab) else plab)
        ns.append(normals(pv, np.asarray(pf)))
        ks.append(np.full(len(pv), kind, np.int8))
        shs.append(np.ones(len(pv)) if sh is None else sh)
        off += len(pv)
    lws = np.zeros(off, np.float32)
    lws[:len(lw)] = lw
    fas = np.ones(off, np.float32)
    fas[n_before_base:n_before_base + len(base_fa)] = base_fa
    # the neck fades from just under the choker to nothing at the skirt's end
    y_end = skin_v[:, 1].min() - SKIRT_RINGS * SKIRT_STEP

    def fade(yy):
        k = np.clip((yy - y_end) / (FADE_TOP - y_end), 0, 1)
        return k * k * (3 - 2 * k)
    fas[:len(skin_v)] = fade(skin_v[:, 1])
    fas[skirt_at:skirt_at + len(sv)] = fade(sv[:, 1])
    return {"v": np.concatenate(vs), "f": np.concatenate(fs).astype(np.int32), "l": np.concatenate(ls), "n": np.concatenate(ns).astype(np.float32),
            "k": np.concatenate(ks), "sh": np.concatenate(shs).astype(np.float32), "lw": lws, "fa": fas, "cards": cards,
            "eyes": eyes.astype(np.float32), "eye_r": EYE_R}


# ------------------------------------------------------------------ what can be seen from where

def icosphere(level=2):
    """Unit directions spread evenly over the sphere: an icosahedron's corners, its edges halved `level` times (162 at
    level 2)."""
    t = (1 + 5 ** 0.5) / 2
    v = [(-1, t, 0), (1, t, 0), (-1, -t, 0), (1, -t, 0), (0, -1, t), (0, 1, t), (0, -1, -t), (0, 1, -t), (t, 0, -1), (t, 0, 1),
         (-t, 0, -1), (-t, 0, 1)]
    f = [(0, 11, 5), (0, 5, 1), (0, 1, 7), (0, 7, 10), (0, 10, 11), (1, 5, 9), (5, 11, 4), (11, 10, 2), (10, 7, 6), (7, 1, 8), (3, 9, 4),
         (3, 4, 2), (3, 2, 6), (3, 6, 8), (3, 8, 9), (4, 9, 5), (2, 4, 11), (6, 2, 10), (8, 6, 7), (9, 8, 1)]
    v = [np.array(p, float) / np.linalg.norm(p) for p in v]
    for _ in range(level):
        mid = {}
        nf = []

        def m(a, b):
            key = (min(a, b), max(a, b))
            if key not in mid:
                p = v[a] + v[b]
                v.append(p / np.linalg.norm(p))
                mid[key] = len(v) - 1
            return mid[key]
        for a, b, c in f:
            ab, bc, ca = m(a, b), m(b, c), m(c, a)
            nf += [(a, ab, ca), (b, bc, ab), (c, ca, bc), (ab, bc, ca)]
        f = nf
    return np.array(v)


def _samples(tri_pts, spacing):
    """Points over each triangle (3 corners each), no further apart than `spacing`: (points, the triangle each is on)."""
    a, b, c = tri_pts[:, 0], tri_pts[:, 1], tri_pts[:, 2]
    longest = np.max(np.stack([np.linalg.norm(b - a, axis=1), np.linalg.norm(c - b, axis=1), np.linalg.norm(a - c, axis=1)]), 0)
    steps = np.clip(np.ceil(longest / spacing).astype(int), 1, 40)
    pts, ids = [], []
    for n in np.unique(steps):
        sel = np.nonzero(steps == n)[0]
        ij = np.array([(i, j) for i in range(n + 1) for j in range(n + 1 - i)], float) / n
        p = a[sel, None, :] + (b - a)[sel, None, :] * ij[None, :, 0:1] + (c - a)[sel, None, :] * ij[None, :, 1:2]
        pts.append(p.reshape(-1, 3))
        ids.append(np.repeat(sel, len(ij)))
    return np.concatenate(pts), np.concatenate(ids)


def visibility(occluders, others, dirs, res=420, extent=1.9, eps=0.008):
    """For each direction (towards the viewer), which primitives can be seen: an occluder (a triangle, 3 corners) is seen
    where it is the nearest surface along the direction (within eps); one of the others (triangles that hide nothing:
    strands, lashes, the hair's cards) is seen where it is in front of the nearest occluder. Orthographic, `res`
    pixels over `extent` head units. Returns booleans (directions x (occluders + others))."""
    spacing = extent / res / 1.6
    op, oi = _samples(occluders, spacing)
    xp, xi = _samples(others, spacing) if len(others) else (np.zeros((0, 3)), np.zeros(0, int))
    centre = np.concatenate([op, xp]).mean(0) if len(xp) else op.mean(0)
    out = np.zeros((len(dirs), len(occluders) + len(others)), bool)
    for k, d in enumerate(dirs):
        d = d / np.linalg.norm(d)
        up = np.array([0.0, 1.0, 0.0]) if abs(d[1]) < 0.9 else np.array([1.0, 0.0, 0.0])
        right = np.cross(up, d)
        right /= np.linalg.norm(right)
        up = np.cross(d, right)

        def project(p):
            q = p - centre
            px = np.clip(((q @ right) / extent + 0.5) * res, 0, res - 1).astype(np.int64)
            py = np.clip(((q @ up) / extent + 0.5) * res, 0, res - 1).astype(np.int64)
            return py * res + px, -(q @ d)
        pix, depth = project(op)
        zbuf = np.full(res * res, np.inf)
        np.minimum.at(zbuf, pix, depth)
        seen = depth <= zbuf[pix] + eps
        out[k, np.unique(oi[seen])] = True
        if len(xp):
            pix2, depth2 = project(xp)
            seen2 = depth2 <= zbuf[pix2] + eps
            out[k, len(occluders) + np.unique(xi[seen2])] = True
    return out


def dilate(vis, tri, rounds=1):
    """Each direction's seen triangles grown by their neighbours (sharing a corner), `rounds` times: closes pinholes
    where a sliver at a silhouette was missed."""
    import scipy.sparse as sp
    n = len(tri)
    rows = np.repeat(np.arange(n), 3)
    cols = tri.reshape(-1)
    inc = sp.csr_matrix((np.ones(len(rows), np.int8), (rows, cols)), shape=(n, int(tri.max()) + 1))
    adj = (inc @ inc.T).astype(bool).astype(np.int8)
    out = vis.copy()
    for _ in range(rounds):
        out = (adj @ out.T.astype(np.int8)).T > 0
    return out
