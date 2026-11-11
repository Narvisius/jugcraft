"""What goes on the realistic head (head units: the chin near y 0.07, the crown near 1.07, the face looking +z):
eyebrows and eyelashes as fine strands, the choker, and the hair (a base layer over the scalp, then thousands of strands:
long straight bangs to the eyes in pointed locks, side locks framing the face, the rest pulled back into a messy bun).

Every strand is a thin ribbon (two triangles a segment), its flat side facing out from the head."""
import numpy as np

RNG = np.random.default_rng(7)
C = np.array([0.0, 0.6, -0.05])          # the head's middle, for the scalp's map


def unit(a):
    a = np.asarray(a, float)
    return a / np.maximum(np.linalg.norm(a, axis=-1, keepdims=True), 1e-12)


# ------------------------------------------------------------------ the scalp, as a map of radius by direction

class Scalp:
    """The head's surface seen from its middle: the radius in each direction (a grid over the sphere, the farthest
    vertex in each cell, gaps filled), and the surface's normal there."""

    def __init__(self, v, nt=120, npp=240):
        self.nt, self.np_ = nt, npp
        d = v - C
        r = np.linalg.norm(d, axis=1)
        u = d / r[:, None]
        th = np.arccos(np.clip(u[:, 1], -1, 1))              # 0 at the top
        ph = np.arctan2(u[:, 0], u[:, 2])                    # 0 at the front
        ti = np.clip((th / np.pi * nt).astype(int), 0, nt - 1)
        pi = ((ph + np.pi) / (2 * np.pi) * npp).astype(int) % npp
        grid = np.full((nt, npp), np.nan)
        np.fmax.at(grid, (ti, pi), r)
        # fill the gaps from the neighbours, round and round
        for _ in range(60):
            m = np.isnan(grid)
            if not m.any():
                break
            acc = np.zeros_like(grid)
            cnt = np.zeros_like(grid)
            for dt, dp in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                sh = np.roll(np.roll(grid, dt, 0), dp, 1)
                ok = ~np.isnan(sh)
                acc[ok] += sh[ok]
                cnt[ok] += 1
            fill = m & (cnt > 0)
            grid[fill] = acc[fill] / cnt[fill]
        # smooth a little (the farthest vertex is noisy)
        for _ in range(2):
            grid = (grid * 4 + np.roll(grid, 1, 0) + np.roll(grid, -1, 0) + np.roll(grid, 1, 1) + np.roll(grid, -1, 1)) / 8
        self.grid = grid

    def radius(self, dirs):
        u = unit(dirs)
        th = np.arccos(np.clip(u[..., 1], -1, 1)) / np.pi * self.nt - 0.5
        ph = (np.arctan2(u[..., 0], u[..., 2]) + np.pi) / (2 * np.pi) * self.np_ - 0.5
        t0 = np.clip(np.floor(th).astype(int), 0, self.nt - 1)
        t1 = np.clip(t0 + 1, 0, self.nt - 1)
        ft = np.clip(th - np.floor(th), 0, 1)
        p0 = np.floor(ph).astype(int) % self.np_
        p1 = (p0 + 1) % self.np_
        fp = ph - np.floor(ph)
        g = self.grid
        return ((g[t0, p0] * (1 - fp) + g[t0, p1] * fp) * (1 - ft) + (g[t1, p0] * (1 - fp) + g[t1, p1] * fp) * ft)

    def point(self, dirs, off=0.0):
        u = unit(dirs)
        return C + u * (self.radius(u) + off)[..., None]

    def normal(self, dirs, h=0.01):
        u = unit(dirs)
        p = self.point(u)
        # two tangents, the surface's slope along each
        a = unit(np.cross(u, np.array([0.0, 1.0, 0.0]) + 1e-6))
        b = unit(np.cross(u, a))
        pa = self.point(u + a * h)
        pb = self.point(u + b * h)
        n = unit(np.cross(pa - p, pb - p))
        flip = np.sum(n * u, -1) < 0
        n[flip] *= -1
        return n


# ------------------------------------------------------------------ strands as ribbons

def ribbons(paths, widths, normals, two_sided=False):
    """Strands (n, m, 3) with widths (n, m) and their faces' normals (n, m, 3) -> vertices, triangles (anticlockwise
    seen from the normal's side), and each vertex's strand number."""
    paths = np.asarray(paths, float)
    n, m, _ = paths.shape
    t = np.gradient(paths, axis=1)
    t = unit(t)
    side = unit(np.cross(normals, t)) * (np.asarray(widths)[..., None] / 2)
    left = paths - side
    right = paths + side
    v = np.stack([left, right], 2).reshape(n, m * 2, 3)
    base = (np.arange(n) * m * 2)[:, None]
    i = np.arange(m - 1)
    a, b = 2 * i, 2 * i + 1
    c, d = 2 * i + 2, 2 * i + 3
    f1 = np.stack([a, b, d], 1)
    f2 = np.stack([a, d, c], 1)
    f = np.concatenate([f1, f2])[None, :, :] + base[:, :, None]
    f = f.reshape(-1, 3)
    # wind each face to face along the given normal
    vv = v.reshape(-1, 3)
    tri = vv[f]
    fn = np.cross(tri[:, 1] - tri[:, 0], tri[:, 2] - tri[:, 0])
    nrm = np.repeat(np.asarray(normals).reshape(n, m, 3)[:, :-1], 2, axis=1).reshape(-1, 3) if False else None
    want = np.asarray(normals).reshape(-1, 3)[f[:, 0] // 2]
    flip = np.sum(fn * want, 1) < 0
    f[flip] = f[flip][:, [0, 2, 1]]
    if two_sided:
        f = np.concatenate([f, f[:, [0, 2, 1]]])
    sid = np.repeat(np.arange(n), m * 2)
    return vv, f, sid


def taper(m, w, tip=0.35, root=1.0):
    s = np.linspace(0, 1, m)
    return w * (root + (1 - root) * np.clip(s / 0.15, 0, 1)) * np.clip((1 - s) / tip, 0, 1) ** 0.7


def smooth(p, rounds=4):
    p = p.copy()
    for _ in range(rounds):
        p[..., 1:-1, :] = 0.25 * p[..., :-2, :] + 0.5 * p[..., 1:-1, :] + 0.25 * p[..., 2:, :]
    return p


def slerp(a, b, s):
    a, b = unit(a), unit(b)
    dot = np.clip(np.sum(a * b, -1), -1, 1)
    om = np.arccos(dot)[..., None]
    so = np.sin(om)
    so = np.where(so < 1e-6, 1.0, so)
    s = np.asarray(s)[..., None] if np.ndim(s) else s
    return (np.sin((1 - s) * om) * a + np.sin(s * om) * b) / so


# ------------------------------------------------------------------ the hair

BUN = np.array([0.0, 0.93, -0.5])        # the knot's middle: high at the back
BUN_R = np.array([0.15, 0.12, 0.12])


def hairline_y(u):
    """How low the scalp's hair comes in a direction (pulled up: off the forehead, above the ears, to the nape)."""
    ph = np.degrees(np.arctan2(u[..., 0], u[..., 2]))           # 0 front, +-90 the sides, +-180 the back
    a = np.abs(ph)
    return np.interp(a, [0, 30, 55, 80, 100, 125, 150, 180], [0.91, 0.88, 0.80, 0.72, 0.60, 0.40, 0.27, 0.22])


def hair_mask(p):
    u = unit(p - C)
    return p[..., 1] > hairline_y(u)


def swept(scalp, n=1400):
    """Strands from all over the scalp's hairline and crown, pulled back over the scalp into the bun."""
    paths, normals = [], []
    m = 18
    s = np.linspace(0, 1, m)
    for k in range(n):
        # a start on or just inside the hairline, all round
        ph = RNG.uniform(-180, 180)
        th_hl = None
        u0 = np.array([np.sin(np.radians(ph)), 0.0, np.cos(np.radians(ph))])
        y0 = float(hairline_y(u0[None])[0])
        y_start = y0 + RNG.uniform(0.0, 0.09) * (1.0 if abs(ph) < 150 else 0.6)
        if abs(ph) < 50:
            # the front belongs to the bangs: these start behind the bangs' roots
            y_start = RNG.uniform(1.03, 1.07)
        # the direction from the middle to that height on the scalp
        horiz = u0 * 0.5
        d0 = unit(np.array([horiz[0], y_start - C[1], horiz[2] + 0.0]))
        # search the height along the scalp
        for _ in range(4):
            p0 = scalp.point(d0[None])[0]
            d0 = unit(np.array([d0[0], d0[1] + (y_start - p0[1]) * 1.5, d0[2]]))
        end = BUN + unit(RNG.normal(size=3)) * BUN_R * 0.4 + np.array([0, 0, 0.02])
        d1 = unit(end - C)
        dirs = slerp(np.tile(d0, (m, 1)), np.tile(d1, (m, 1)), s ** 0.9)
        # strands at the hairline lie low, the ones over them a little out; a slight wave across
        off = 0.012 + RNG.uniform(0.0, 0.016) + 0.012 * np.sin(np.pi * s)
        pts = scalp.point(dirs, off)
        pts[-3:] = pts[-3:] * 0.6 + end * 0.4 if False else pts[-3:]
        paths.append(pts)
        normals.append(scalp.normal(dirs))
    paths = smooth(np.array(paths), 2)
    w = np.tile(taper(m, 0.0085, tip=0.25), (n, 1))
    return paths, w, np.array(normals)


def bun(n=420):
    """The knot: strands wound round and round it, twisted, and loose ends sticking out of it (messy)."""
    paths, normals = [], []
    m = 22
    s = np.linspace(0, 1, m)
    ax = unit(np.array([0.0, 0.55, -1.0]))
    e1 = unit(np.cross(ax, [1.0, 0, 0]))
    e2 = unit(np.cross(ax, e1))
    for k in range(n):
        a0 = RNG.uniform(0, 2 * np.pi)
        turns = RNG.uniform(0.7, 1.3)
        lat = RNG.uniform(-1, 1)
        a = a0 + s * turns * 2 * np.pi
        lat_s = lat * (1 - 0.3 * s) + 0.25 * np.sin(a * 0.5)
        r = BUN_R[0] * np.sqrt(np.clip(1 - (lat_s * 0.8) ** 2, 0.1, 1)) * (1.0 + RNG.uniform(-0.05, 0.08))
        pts = BUN[None, :] + (np.cos(a)[:, None] * e1 + np.sin(a)[:, None] * e2) * r[:, None] + ax * (lat_s * BUN_R[2] * 0.8)[:, None]
        nrm = unit(pts - BUN)
        paths.append(pts)
        normals.append(nrm)
    paths = np.array(paths)
    w = np.tile(taper(m, 0.009, tip=0.2, root=0.4), (n, 1))
    return paths, w, np.array(normals)


def loose_ends(n=60):
    """Strands sticking out of the knot and falling from it: the messy ends."""
    paths, normals = [], []
    m = 12
    s = np.linspace(0, 1, m)
    for k in range(n):
        d = unit(RNG.normal(size=3) * [1.0, 0.5, 0.5] + [0, -0.35, -0.7])
        root = BUN + d * BUN_R * 0.9
        L = RNG.uniform(0.06, 0.15)
        curl = unit(np.cross(d, RNG.normal(size=3)))
        pts = root + d * (s * L)[:, None] + curl * (0.04 * s * s)[:, None] - np.array([0, 1, 0]) * (0.03 * s * s)[:, None]
        paths.append(pts)
        normals.append(np.tile(unit(np.cross(curl, d)), (m, 1)))
    w = np.tile(taper(m, 0.008, tip=0.5), (n, 1))
    return np.array(paths), w, np.array(normals)


def face_front_z(skin_v, x, y, clear=0.012):
    """The face's front (largest z) at (x, y), from the skin's vertices near there."""
    out = []
    xs, ys = np.atleast_1d(x), np.atleast_1d(y)
    cl = np.broadcast_to(np.asarray(clear, float), xs.shape)
    for xi, yi, ci in zip(xs, ys, cl):
        m = (np.abs(skin_v[:, 0] - xi) < 0.025) & (np.abs(skin_v[:, 1] - yi) < 0.025) & (skin_v[:, 2] > -0.1)
        out.append(skin_v[m, 2].max() + ci if m.any() else np.nan)
    return np.array(out)


BANG_LOCKS = [  # (tip x, tip y, strands, width at the root): long straight bangs to the eyes, choppy, one between them
    (-0.33, 0.42, 40, 0.10), (-0.24, 0.55, 46, 0.11), (-0.155, 0.60, 44, 0.10), (-0.07, 0.535, 40, 0.09),
    (0.0, 0.47, 14, 0.06), (0.075, 0.545, 40, 0.09), (0.16, 0.605, 44, 0.10), (0.245, 0.56, 46, 0.11), (0.33, 0.43, 40, 0.10),
]


def bangs(scalp, skin_v):
    """Locks of strands from the front of the crown, over the forehead (just off it), hanging straight down in front of
    the brows to the eyes; each lock's strands gather to a point."""
    paths, normals, widths = [], [], []
    m = 24
    s = np.linspace(0, 1, m)
    for tx, ty, n, wr in BANG_LOCKS:
        root_c = np.array([tx * 0.45, 1.05 + RNG.uniform(0, 0.03), 0.0])
        for k in range(n):
            jx = RNG.uniform(-0.5, 0.5) * wr
            r0 = root_c + np.array([jx, 0, 0])
            d0 = unit(r0 - C + np.array([0, 0, 0.12]))
            p_root = scalp.point(d0[None], 0.02)[0]
            # the tip: the lock's point, strands ending a little short of it at random (choppy)
            tip = np.array([tx + jx * 0.55, ty + RNG.uniform(0, 0.09) + abs(jx) * 0.35, 0.0])
            x = p_root[0] + (tip[0] - p_root[0]) * s ** 0.8
            y = p_root[1] + (tip[1] - p_root[1]) * s
            zf = face_front_z(skin_v, x, y, clear=0.014 + 0.01 * np.sin(np.pi * np.clip(s * 1.2, 0, 1)))
            z = np.where(np.isnan(zf), p_root[2], zf)
            z[0] = p_root[2]
            # hang: never back into the eye sockets
            for i in range(1, m):
                z[i] = max(z[i], z[i - 1] - 0.15 * (y[i - 1] - y[i]))
            pts = np.stack([x, y, z], 1)
            paths.append(pts)
            nn = unit(pts - C)
            nn[s > 0.4] = unit(np.array([0, 0.1, 1.0]))
            normals.append(nn)
            widths.append(taper(m, 0.008, tip=0.3))
    return smooth(np.array(paths), 3), np.array(widths), np.array(normals)


SIDE_LOCKS = [  # (z at the temple, tip y, strands): long, framing the face down past the jaw
    (0.16, 0.12, 60), (0.22, 0.26, 44), (0.08, 0.02, 60),
]


def side_locks(scalp, skin_v):
    """Long locks framing the face: rooted on the scalp from the temple up towards the parting, lying on the scalp
    down to the temple, then hanging straight down in front of the ear past the jaw."""
    paths, normals, widths = [], [], []
    m = 30
    for sd in (1, -1):
        for z0, ty, n in SIDE_LOCKS:
            for k in range(n):
                jz = RNG.uniform(-0.05, 0.05)
                # the root: up on the scalp (front-side), the temple point below it
                root_d = unit(np.array([sd * RNG.uniform(0.18, 0.32), RNG.uniform(0.95, 1.02) - C[1], z0 + 0.12 + jz - C[2]]))
                temple_d = unit(np.array([sd * 0.44, 0.84 - C[1], z0 + jz - C[2]]))
                s1 = np.linspace(0, 1, 10)
                top = scalp.point(slerp(np.tile(root_d, (10, 1)), np.tile(temple_d, (10, 1)), s1), 0.022)
                p0 = top[-1]
                m2 = m - 10
                s2 = np.linspace(0, 1, m2 + 1)[1:]
                y = p0[1] + (ty + RNG.uniform(0, 0.08) - p0[1]) * s2
                zz = p0[2] + (0.03 + jz * 0.5) * s2
                x = np.empty(m2)
                prev = p0[0]
                for i in range(m2):
                    mm = (np.abs(skin_v[:, 1] - y[i]) < 0.03) & (np.abs(skin_v[:, 2] - zz[i]) < 0.04) & (sd * skin_v[:, 0] > 0)
                    surf = sd * skin_v[mm, 0].max() if mm.any() else 0.0
                    x[i] = sd * max(surf + 0.022, sd * prev - 0.08 * ((y[i - 1] if i else p0[1]) - y[i]))
                    prev = x[i]
                pts = np.concatenate([top, np.stack([x, y, zz], 1)])
                paths.append(pts)
                nn = np.tile(unit(np.array([sd * 0.6, 0, 0.8])), (m, 1))
                nn[:10] = unit(top - C)
                normals.append(nn)
                widths.append(taper(m, 0.008, tip=0.3))
    return smooth(np.array(paths), 3), np.array(widths), np.array(normals)


def base_layer(skin_v, skin_f, skin_n):
    """The hair's underlayer: the scalp's own faces, lifted off it, wherever the hair grows."""
    inside = hair_mask(skin_v)
    keep = inside[skin_f].all(1)
    f = skin_f[keep]
    used = np.unique(f)
    remap = -np.ones(len(skin_v), int)
    remap[used] = np.arange(len(used))
    v = skin_v[used] + skin_n[used] * 0.011
    return v, remap[f]


# ------------------------------------------------------------------ brows, lashes, choker

def lashes(lash_roots, eyes):
    """Each lid's lash line: a dark band along the lid's edge and the lashes off it (the upper ones longer, curling up)."""
    paths, normals, widths = [], [], []
    m = 6
    s = np.linspace(0, 1, m)
    for roots, upper, eye in lash_roots:
        roots = roots[np.argsort(roots[:, 0])]
        # resample the edge
        tt = np.linspace(0, 1, len(roots))
        n = 70 if upper else 12
        for k in range(n):
            t = RNG.uniform(0.05, 0.95)
            r = np.array([np.interp(t, tt, roots[:, i]) for i in range(3)])
            out = unit(r - eye)
            up = np.array([0, 1.0, 0]) if upper else np.array([0, -1.0, 0])
            L = (0.026 if upper else 0.0045) * (0.6 + 0.6 * np.sin(np.pi * t)) * RNG.uniform(0.8, 1.1)
            d = unit(out * 0.7 + up * 0.5)
            pts = r + d * (s * L)[:, None] + up * (0.012 * s * s * (1 if upper else 0.5))[:, None]
            paths.append(pts)
            normals.append(np.tile(unit(np.cross(d, unit(np.cross(up, out)))) if False else np.tile(out, (m, 1)), (1, 1)))
            widths.append(taper(m, 0.0026 if upper else 0.0016, tip=0.6))
        # the line itself: a band along the edge, thick (the lashes' roots)
        line = np.array([np.array([np.interp(t, tt, roots[:, i]) for i in range(3)]) for t in np.linspace(0.02, 0.98, 14)])
        out = unit(line - eye)
        paths.append(line + out * 0.002)
        normals.append(out)
        widths.append(np.full(14, 0.007 if upper else 0.004) * np.sin(np.pi * np.linspace(0.1, 0.9, 14)) ** 0.5)
    # the strands have different lengths of path; pad to the same count
    mm = max(len(p) for p in paths)
    def pad(a):
        a = np.asarray(a)
        if len(a) == mm:
            return a
        idx = np.linspace(0, len(a) - 1, mm)
        return np.array([np.interp(idx, np.arange(len(a)), a[:, i]) for i in range(a.shape[1])]).T if a.ndim == 2 else np.interp(idx, np.arange(len(a)), a)
    return np.array([pad(p) for p in paths]), np.array([pad(w) for w in widths]), np.array([pad(n) for n in normals])


def choker(skin_v, y=-0.075, height=0.05, segs=72):
    """A band round the neck (just off it) and a small ring hanging at the front."""
    band = skin_v[np.abs(skin_v[:, 1] - y) < 0.02]
    cz = (band[:, 2].min() + band[:, 2].max()) / 2
    ang = np.linspace(0, 2 * np.pi, segs, endpoint=False)
    d = band[:, [0, 2]] - [0, cz]
    a_v = np.arctan2(d[:, 0], d[:, 1])
    r_v = np.linalg.norm(d, axis=1)
    rad = np.array([r_v[np.abs((a_v - a + np.pi) % (2 * np.pi) - np.pi) < 0.15].max() if (np.abs((a_v - a + np.pi) % (2 * np.pi) - np.pi) < 0.15).any() else r_v.mean() for a in ang]) + 0.008
    rad = (rad + np.roll(rad, 1) + np.roll(rad, -1)) / 3
    verts, faces = [], []
    for k, (a, r) in enumerate(zip(ang, rad)):
        for yy in (y - height / 2, y + height / 2):
            verts.append((np.sin(a) * r, yy, cz + np.cos(a) * r))
    verts = np.array(verts)
    for k in range(segs):
        a0, a1 = 2 * k, 2 * ((k + 1) % segs)
        faces += [(a0, a1, a1 + 1), (a0, a1 + 1, a0 + 1)]
    faces = np.array(faces)
    # wind outwards
    tri = verts[faces]
    fn = np.cross(tri[:, 1] - tri[:, 0], tri[:, 2] - tri[:, 0])
    out = tri.mean(1) - np.array([0, y, cz])
    out[:, 1] = 0
    flip = np.sum(fn * out, 1) < 0
    faces[flip] = faces[flip][:, [0, 2, 1]]
    # the ring at the front: a small torus facing forward
    front = np.array([0.0, y - height / 2 - 0.018, cz + rad[0] + 0.004])
    rv, rf = [], []
    R, r = 0.02, 0.005
    for i in range(16):
        a = 2 * np.pi * i / 16
        for j in range(6):
            b = 2 * np.pi * j / 6
            c = np.array([np.sin(a) * (R + r * np.cos(b)), np.cos(a) * (R + r * np.cos(b)), r * np.sin(b)])
            rv.append(front + c)
    for i in range(16):
        for j in range(6):
            a0 = i * 6 + j
            a1 = ((i + 1) % 16) * 6 + j
            b0 = i * 6 + (j + 1) % 6
            b1 = ((i + 1) % 16) * 6 + (j + 1) % 6
            rf += [(a0, a1, b1), (a0, b1, b0)]
    rv, rf = np.array(rv), np.array(rf)
    tri = rv[rf]
    fn = np.cross(tri[:, 1] - tri[:, 0], tri[:, 2] - tri[:, 0])
    # outward from the tube's circle
    ctr = tri.mean(1) - front
    ring_pt = np.stack([ctr[:, 0], ctr[:, 1], np.zeros(len(ctr))], 1)
    ring_pt = unit(ring_pt) * R
    out = ctr - ring_pt
    flip = np.sum(fn * out, 1) < 0
    rf[flip] = rf[flip][:, [0, 2, 1]]
    return np.concatenate([verts, rv]), np.concatenate([faces, rf + len(verts)])


def lid_margins(skin_v, eye, r_eye, n=48):
    """The lids' edges round an eye: in each direction round the eye's axis, the skin point nearest the gaze that
    still lies on the eyeball (where the lid meets it). Returns (upper points, lower points)."""
    d = skin_v - eye
    dist = np.linalg.norm(d, axis=1)
    ok = (d[:, 2] > 0.0) & (dist > r_eye * 0.95) & (dist < r_eye + 0.02)
    dd = d[ok]
    pol = np.arccos(np.clip(dd[:, 2] / np.linalg.norm(dd, axis=1), -1, 1))
    az = np.arctan2(dd[:, 1], dd[:, 0])
    up, lo = [], []
    for a in np.linspace(-np.pi, np.pi, n, endpoint=False):
        m = np.abs((az - a + np.pi) % (2 * np.pi) - np.pi) < np.radians(5)
        if not m.any():
            continue
        i = np.nonzero(m)[0][np.argmin(pol[m])]
        if pol[i] > 1.1:
            continue
        p = skin_v[ok][i]
        (up if np.sin(a) > 0.05 else lo if np.sin(a) < -0.05 else []).append(p)
    up, lo = np.array(up), np.array(lo)
    # each lid's edge in order across the eye
    return up[np.argsort(up[:, 0])], lo[np.argsort(lo[:, 0])]


def eye(lat=24, lon=36, pupil=10.0, iris=36.0):
    """An eyeball (unit radius, looking +z): vertices, faces, parts (0 white, 1 iris, 2 pupil), normals."""
    v, lab = [], []
    for i in range(lat + 1):
        th = np.pi * i / lat
        for j in range(lon):
            ph = 2 * np.pi * j / lon
            v.append((np.sin(th) * np.cos(ph), np.sin(th) * np.sin(ph), np.cos(th)))
            ang = np.degrees(th)
            lab.append(2 if ang < pupil else 1 if ang < iris else 0)
    f = []
    for i in range(lat):
        for j in range(lon):
            a, b = i * lon + j, i * lon + (j + 1) % lon
            c, d = (i + 1) * lon + (j + 1) % lon, (i + 1) * lon + j
            f.append((a, d, c))
            f.append((a, c, b))
    v = np.array(v, np.float32)
    return v, np.array(f, np.int32), np.array(lab, np.int8), v.copy()


# ------------------------------------------------------------------ hair cards (take 4): soft locks, not sticks

def card_texture(variant, size=(128, 40)):
    """A lock's texture (rows along it, root at row 0; columns across): a dozen fine strands, each its own shade, each
    ending somewhere in the last third (a ragged, tapering tip), soft at their edges. RGBA in 0..1 (rgb as brightness)."""
    rng = np.random.default_rng(100 + variant)
    th, tw = size
    tex = np.zeros((th, tw, 4), np.float32)
    v = (np.arange(th) + 0.5) / th
    u = (np.arange(tw) + 0.5) / tw
    n = 14
    for k in range(n):
        x0 = rng.uniform(0.04, 0.96)
        wid = rng.uniform(0.035, 0.07)
        end = rng.uniform(0.62, 1.0)
        bright = rng.uniform(0.7, 1.25)
        drift = rng.uniform(-0.05, 0.05)
        xc = x0 + drift * v                                   # strands drift a little along the lock
        prof = np.clip(1 - np.abs(u[None, :] - xc[:, None]) / wid, 0, 1) ** 0.8
        # the tip tapers: thinner and fainter over its last 15%
        taper = np.clip((end - v) / 0.15, 0, 1)[:, None]
        a = prof * taper
        # the lock as a whole narrows at the tip
        a *= np.clip(1 - np.abs(u[None, :] - 0.5) * 2 * (0.4 + 0.6 * v[:, None] ** 2), 0, 1) ** 0.3
        tex[..., 3] = np.maximum(tex[..., 3], a)
        tex[..., 0] = np.where(a >= tex[..., 3] - 1e-6, bright, tex[..., 0])
    # a soft sheen band (light catching the lock)
    sheen = 1 + 0.25 * np.exp(-((v - 0.3) / 0.08) ** 2)[:, None]
    tex[..., 0] *= sheen
    tex[..., 1] = tex[..., 0]
    tex[..., 2] = tex[..., 0]
    return tex


def cards_swept(scalp, n=300):
    p, w, nn = swept(scalp, n)
    m = p.shape[1]
    s = np.linspace(0, 1, m)
    w = np.tile(0.05 * (1 - 0.55 * s ** 1.5), (n, 1)) * RNG.uniform(0.85, 1.25, (n, 1))
    # layer them: each lifted a little more or less
    p = p + nn * RNG.uniform(0.0, 0.012, (n, 1, 1))
    return p, w, nn


def cards_bun(n=90):
    p, w, nn = bun(n)
    m = p.shape[1]
    w = np.full((n, m), 0.04) * RNG.uniform(0.8, 1.2, (n, 1))
    return p, w, nn


def cards_loose(n=9):
    p, w, nn = loose_ends(n)
    m = p.shape[1]
    w = np.full((n, m), 0.03)
    return p, w, nn


def cards_bangs(scalp, skin_v):
    """The bangs as locks of a few overlapping cards each, fanning a little; their tips ragged (the texture)."""
    global BANG_LOCKS
    saved = BANG_LOCKS
    BANG_LOCKS = [(tx, ty, max(4, n // 9), wr) for tx, ty, n, wr in saved]
    try:
        p, w, nn = bangs(scalp, skin_v)
    finally:
        BANG_LOCKS = saved
    m = p.shape[1]
    s = np.linspace(0, 1, m)
    w = np.tile(0.045 * (0.7 + 0.3 * np.sin(np.pi * np.clip(s * 1.3, 0, 1))), (len(p), 1)) * RNG.uniform(0.85, 1.15, (len(p), 1))
    return p, w, nn


def cards_side(scalp, skin_v):
    global SIDE_LOCKS
    saved = SIDE_LOCKS
    SIDE_LOCKS = [(z0, ty, max(4, n // 10)) for z0, ty, n in saved]
    try:
        p, w, nn = side_locks(scalp, skin_v)
    finally:
        SIDE_LOCKS = saved
    m = p.shape[1]
    w = np.full((len(p), m), 0.045) * RNG.uniform(0.85, 1.15, (len(p), 1))
    return p, w, nn


def brow_cards(skin_v, skin_n):
    """Each brow as two overlapping cards lying on the skin along its curve (head to tail, the strands running along
    it), thick at the head, thinning to the tail; scowling: the heads low and close."""
    from scipy.spatial import cKDTree
    tree = cKDTree(skin_v)
    paths, widths, normals = [], [], []
    m = 16
    for sd in (1, -1):
        for layer in range(2):
            t = np.linspace(0, 1, m)
            bx = 0.045 + 0.205 * t
            by = 0.598 + 0.058 * np.sin(np.pi * np.minimum(t / 0.75, 1.0) * 0.5) - 0.014 * np.maximum(0, t - 0.78) / 0.22
            by = by + (layer - 0.5) * 0.006
            pts, nrm = [], []
            for x_, y_ in zip(bx, by):
                mm = (np.abs(skin_v[:, 0] - sd * x_) < 0.012) & (np.abs(skin_v[:, 1] - y_) < 0.012) & (skin_v[:, 2] > 0.1)
                j = np.nonzero(mm)[0][np.argmax(skin_v[mm, 2])]
                pts.append(skin_v[j] + skin_n[j] * (0.004 + 0.001 * layer))
                nrm.append(skin_n[j])
            paths.append(smooth(np.array(pts)[None], 2)[0])
            normals.append(np.array(nrm))
            widths.append((0.03 - 0.018 * t ** 1.2) * (1 - 0.15 * layer))
    return np.array(paths), np.array(widths), np.array(normals)


def card_texture_dark(variant, size=(128, 40), under=0.42, under_rgb=0.28):
    """The lock's texture as drawn in game (no depth pre-pass there, so layers of locks blend): its strands as
    card_texture's, darker at their edges, over a dark, faint body of the lock between them, so each lock reads as strands
    and layers of locks do not wash out."""
    rng = np.random.default_rng(100 + variant)
    th, tw = size
    v = (np.arange(th) + 0.5) / th
    u = (np.arange(tw) + 0.5) / tw
    a_s = np.zeros((th, tw), np.float32)
    rgb = np.zeros((th, tw), np.float32)
    for k in range(14):
        x0 = rng.uniform(0.04, 0.96)
        wid = rng.uniform(0.035, 0.07)
        end = rng.uniform(0.62, 1.0)
        bright = rng.uniform(0.7, 1.25)
        drift = rng.uniform(-0.05, 0.05)
        xc = x0 + drift * v
        prof = np.clip(1 - np.abs(u[None, :] - xc[:, None]) / wid, 0, 1) ** 0.8
        taper = np.clip((end - v) / 0.15, 0, 1)[:, None]
        a = prof * taper
        a *= np.clip(1 - np.abs(u[None, :] - 0.5) * 2 * (0.4 + 0.6 * v[:, None] ** 2), 0, 1) ** 0.3
        top = a >= a_s - 1e-6
        rgb = np.where(top, bright * (0.55 + 0.45 * prof), rgb)
        a_s = np.maximum(a_s, a)
    sheen = 1 + 0.25 * np.exp(-((v - 0.3) / 0.08) ** 2)[:, None]
    rgb = rgb * sheen
    body = np.clip(1 - np.abs(u[None, :] - 0.5) * 2 * (0.4 + 0.6 * v[:, None] ** 2), 0, 1) ** 0.3 * np.clip((0.88 - v) / 0.2, 0, 1)[:, None]
    a_u = under * body
    alpha = a_s + a_u * (1 - a_s)
    col = (rgb * a_s + under_rgb * a_u * (1 - a_s)) / np.maximum(alpha, 1e-6)
    tex = np.zeros((th, tw, 4), np.float32)
    tex[..., 0] = tex[..., 1] = tex[..., 2] = col
    tex[..., 3] = alpha
    return tex
