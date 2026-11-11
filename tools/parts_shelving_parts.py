"""The parts on the Drone Parts Shelving (work list item 9; tools/drone_parts_shelving.py), modelled the way the real
things are made (Justin on the first mockup: "up the realism ... some of these just look like plain boxes with a label
slapped on"). Cartons are slotted corrugated boxes with their flaps, tape and printed kraft; bins are hopper bins with
their contents; cases are moulded with latches in recesses, a fold-down handle, a purge valve and an engraved plate;
smart packs stand in a battery station; LiPo packs charge in an open steel LiPo safe, their leads to a balance charger;
the extinguisher is lathe-turned, with its valve, gauge, pin, hose and wrapped label. Metres; +z is the front.

The mesh `m` is drone_parts_shelving.Mesh (quads with textures, kinds, live faces); the battery gauges, the station's
slot LEDs and the two screens are live faces the renderer animates (client/DronePartsShelvingRenderer)."""
import math
import numpy as np

import parts_shelving_art as T

# live faces (client/DronePartsShelvingRenderer): a pack's four gauge LEDs (id pack * 4 + LED), a station slot's LED (id
# slot), the battery station's screen, the balance charger's screen
GAUGE, SLOT, STATION, CHARGER = 8, 9, 10, 11


def solid(rgb, a=1.0):
    t = np.ones((2, 2, 4), np.float32)
    t[..., :3] = np.array(rgb, np.float32) / 255
    t[..., 3] = a
    return t


def noise(size, rgb, amt, seed, streak=False):
    rnd = np.random.default_rng(seed)
    t = np.ones((size, size, 4), np.float32)
    n = rnd.uniform(-1, 1, (size, size)) * amt
    if streak:
        n = n * 0.35 + rnd.uniform(-1, 1, (size, 1)) * amt
    for i in range(3):
        t[..., i] = np.clip((rgb[i] + n) / 255, 0, 1)
    return t


def twill(size=64, seed=9):
    """Carbon fibre 2x2 twill: tows crossing on the diagonal, each catching the light differently."""
    t = np.ones((size, size, 4), np.float32)
    k = 8
    yy, xx = np.mgrid[0:size, 0:size]
    band = ((xx + yy) // k) % 2
    weave = ((xx // k) + (yy // k)) % 2
    v = np.where(band == weave, 0.16, 0.10) + 0.03 * np.sin((xx - yy) * math.pi / k)
    for i in range(3):
        t[..., i] = v * (1.0, 1.04, 1.1)[i]
    return t


M = {
    "kraft": noise(64, (176, 134, 88), 5, 1), "kraft_in": noise(64, (150, 112, 72), 5, 2), "tape": T.tape(),
    "edge": T.corrugated_edge(), "steel": noise(32, (150, 156, 162), 7, 3, True), "chrome": noise(32, (196, 200, 206), 10, 4, True),
    "black": solid((18, 18, 20)), "rubber": noise(16, (26, 26, 28), 2, 5), "case_black": noise(256, (32, 33, 36), 1.6, 6),
    "case_orange": noise(256, (230, 102, 24), 2.5, 7), "latch": noise(32, (36, 37, 40), 2, 8), "red": noise(32, (190, 26, 24), 4, 9),
    "brass": noise(16, (196, 160, 86), 8, 10), "graphite": noise(32, (54, 57, 62), 2.5, 11), "graphite_dk": noise(32, (36, 38, 42), 2, 12),
    "amber": solid((255, 168, 58)), "yellow": noise(16, (238, 196, 36), 5, 13), "white": noise(16, (236, 236, 230), 3, 14),
    "green": solid((70, 170, 70)), "copper": noise(16, (188, 104, 52), 14, 15), "carbon": twill(),
    "bin": noise(32, (232, 146, 40), 4, 16), "bin_in": noise(32, (206, 124, 30), 4, 17), "foam": noise(128, (46, 48, 52), 5, 18),
    "foam_pit": noise(32, (24, 25, 28), 4, 19), "safe": noise(32, (28, 29, 32), 3, 20), "wire_red": solid((196, 30, 28)),
    "wire_black": solid((20, 20, 22)), "wire_white": solid((232, 232, 228)), "led_g": solid((110, 255, 140)), "led_a": solid((255, 186, 80)),
    "led_off": solid((24, 34, 26)), "lens": solid((14, 18, 24)), "clear": solid((210, 226, 232), 0.35), "pcb": noise(16, (20, 96, 60), 6, 21),
    "shrink_blue": noise(16, (30, 90, 190), 4, 22), "alu_amber": noise(32, (226, 140, 40), 6, 23, True), "alu_dark": noise(32, (70, 74, 80), 6, 24, True),
}


def uv4(u0=0.0, v0=0.0, u1=1.0, v1=1.0):
    return [(u0 * 16, v0 * 16), (u1 * 16, v0 * 16), (u1 * 16, v1 * 16), (u0 * 16, v1 * 16)]


def panel(m, tl, right, down, tex, uv=(0, 0, 1, 1), lit=False, ao=True):
    """A flat quad: top-left corner, the vector to its right edge and down to its bottom edge, as its viewer sees it."""
    tl, right, down = (np.asarray(v, float) for v in (tl, right, down))
    pts = [tl, tl + right, tl + right + down, tl + down]
    m.oquad(pts, np.cross(down, right), tex, uvs=uv4(*uv), lit=lit, ao=ao)


def box(m, lo, hi, tex, faces="nsewud"):
    lo, hi = np.asarray(lo, float), np.asarray(hi, float)
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    F = {"s": ((x0, y1, z1), (x1 - x0, 0, 0), (0, y0 - y1, 0)), "n": ((x1, y1, z0), (x0 - x1, 0, 0), (0, y0 - y1, 0)),
         "e": ((x1, y1, z1), (0, 0, z0 - z1), (0, y0 - y1, 0)), "w": ((x0, y1, z0), (0, 0, z1 - z0), (0, y0 - y1, 0)),
         "u": ((x0, y1, z0), (x1 - x0, 0, 0), (0, 0, z1 - z0)), "d": ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1))}
    for f in faces:
        t = tex[f] if isinstance(tex, dict) else tex
        if t is not None:
            panel(m, *F[f], t)


def rbox(m, c, w, h, d, r, tex, seg=3, faces="nsewud"):
    """A box standing on c (its bottom middle), its four upright edges rounded to r. tex: one texture or a dict by face
    (s front, n back, e, w, u top, d bottom, 'edge' the rounded corners); each flat face gets its whole texture."""
    c = np.asarray(c, float)
    get = (lambda k: tex.get(k, tex.get("edge"))) if isinstance(tex, dict) else (lambda k: tex)
    hx, hz = w / 2, d / 2
    r = min(r, hx * 0.95, hz * 0.95)
    y0, y1 = c[1], c[1] + h
    X, Z = c[0], c[2]
    if "s" in faces:
        panel(m, (X - hx + r, y1, Z + hz), (w - 2 * r, 0, 0), (0, -h, 0), get("s"))
    if "n" in faces:
        panel(m, (X + hx - r, y1, Z - hz), (-(w - 2 * r), 0, 0), (0, -h, 0), get("n"))
    if "e" in faces:
        panel(m, (X + hx, y1, Z + hz - r), (0, 0, -(d - 2 * r)), (0, -h, 0), get("e"))
    if "w" in faces:
        panel(m, (X - hx, y1, Z - hz + r), (0, 0, d - 2 * r), (0, -h, 0), get("w"))
    # corner surfaces
    et = get("edge")
    if r > 0:
        for (sx, sz) in ((1, 1), (1, -1), (-1, -1), (-1, 1)):
            cc = np.array([X + sx * (hx - r), Z + sz * (hz - r)])
            base = {(1, 1): 0, (1, -1): -90, (-1, -1): 180, (-1, 1): 90}[(sx, sz)]
            for k in range(seg):
                a0 = math.radians(base + 90 * k / seg)
                a1 = math.radians(base + 90 * (k + 1) / seg)
                p0 = np.array([cc[0] + r * math.cos(a0), cc[1] + r * math.sin(a0)])
                p1 = np.array([cc[0] + r * math.cos(a1), cc[1] + r * math.sin(a1)])
                pts = [(p0[0], y1, p0[1]), (p1[0], y1, p1[1]), (p1[0], y0, p1[1]), (p0[0], y0, p0[1])]
                mid = (p0 + p1) / 2 - cc
                m.oquad(pts, (mid[0], 0, mid[1]), et)
    # top and bottom: fans over the rounded outline
    ring = []
    for (sx, sz) in ((1, 1), (1, -1), (-1, -1), (-1, 1)):
        cc = np.array([X + sx * (hx - r), Z + sz * (hz - r)])
        base = {(1, 1): 90, (1, -1): 0, (-1, -1): -90, (-1, 1): -180}[(sx, sz)]
        for k in range(seg + 1):
            a = math.radians(base - 90 * k / seg)
            ring.append(cc + r * np.array([math.cos(a), math.sin(a)]))
    for face, y, up in (("u", y1, 1), ("d", y0, -1)):
        if face not in faces:
            continue
        t = get(face)
        for i in range(len(ring)):
            a, b = ring[i], ring[(i + 1) % len(ring)]
            uvs = [(((p[0] - X + hx) / w) * 16, ((p[1] - Z + hz) / d) * 16) for p in ((X, Z), a, b, b)]
            m.oquad([(X, y, Z), (a[0], y, a[1]), (b[0], y, b[1]), (b[0], y, b[1])], (0, up, 0), t, uvs=uvs)


def lathe(m, c, prof, tex, n=24, a0=0.0, a1=2 * math.pi, u=(0.0, 1.0), lit=False):
    """Turns a profile [(r, y) bottom to top] about the upright through c. u maps the angle range onto the texture so that
    it reads left to right from outside; v runs top (0) to bottom (1) along the profile."""
    c = np.asarray(c, float)
    L = [0.0]
    for j in range(1, len(prof)):
        L.append(L[-1] + math.hypot(prof[j][0] - prof[j - 1][0], prof[j][1] - prof[j - 1][1]))
    tot = L[-1] or 1.0
    for i in range(n):
        aa, ab = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
        ua, ub = u[0] + (u[1] - u[0]) * (1 - i / n), u[0] + (u[1] - u[0]) * (1 - (i + 1) / n)
        for j in range(len(prof) - 1):
            (r0, y0), (r1, y1) = prof[j], prof[j + 1]
            if r0 < 1e-6 and r1 < 1e-6:
                continue
            P = lambda r_, y_, a_: c + np.array([r_ * math.cos(a_), y_, r_ * math.sin(a_)])  # noqa: E731
            pts = [P(r1, y1, ab), P(r1, y1, aa), P(r0, y0, aa), P(r0, y0, ab)]
            dr, dy = r1 - r0, y1 - y0
            am = (aa + ab) / 2
            want = (dy * math.cos(am), -dr, dy * math.sin(am))
            va, vb = 1 - L[j + 1] / tot, 1 - L[j] / tot
            uvs = [(ub * 16, va * 16), (ua * 16, va * 16), (ua * 16, vb * 16), (ub * 16, vb * 16)]
            m.oquad(pts, want, tex, uvs=uvs, lit=lit)


def cyl(m, a, b, r, tex, n=14, cap=None, caps=True, tile=None):
    """A round bar from a to b. tile: metres a texture repeat covers (None: the whole texture on each side face)."""
    a, b = np.asarray(a, float), np.asarray(b, float)
    d = b - a
    L = np.linalg.norm(d)
    if L < 1e-9:
        return
    w = d / L
    uu = np.cross(w, (0, 1, 0) if abs(w[1]) < 0.9 else (1, 0, 0))
    uu /= np.linalg.norm(uu)
    vv = np.cross(w, uu)
    ring = [(uu * math.cos(2 * math.pi * i / n) + vv * math.sin(2 * math.pi * i / n)) * r for i in range(n)]
    for i in range(n):
        p, q = ring[i], ring[(i + 1) % n]
        if tile:
            arc = 2 * math.pi * r / n / tile * 16
            ln = L / tile * 16
            m.oquad([b + q, b + p, a + p, a + q], (p + q) / 2, tex, uvs=[(arc * (i + 1), 0), (arc * i, 0), (arc * i, ln), (arc * (i + 1), ln)])
        else:
            m.oquad([b + q, b + p, a + p, a + q], (p + q) / 2, tex)
        if caps:
            ct = cap if cap is not None else tex
            uvc = lambda v: (8 + 8 * (v @ uu) / r, 8 + 8 * (v @ vv) / r)  # noqa: E731
            m.oquad([b, b + p, b + q, b + q], w, ct, uvs=[(8, 8), uvc(p), uvc(q), uvc(q)])
            m.oquad([a, a + q, a + p, a + p], -w, ct, uvs=[(8, 8), uvc(q), uvc(p), uvc(p)])


def tube(m, pts, r, tex, n=8):
    pts = [np.asarray(p, float) for p in pts]
    rings = []
    for i, p in enumerate(pts):
        d = pts[min(i + 1, len(pts) - 1)] - pts[max(i - 1, 0)]
        d /= np.linalg.norm(d)
        uu = np.cross(d, (0, 1, 0) if abs(d[1]) < 0.9 else (1, 0, 0))
        uu /= np.linalg.norm(uu)
        vv = np.cross(d, uu)
        rings.append([p + (uu * math.cos(2 * math.pi * k / n) + vv * math.sin(2 * math.pi * k / n)) * r for k in range(n)])
    for i in range(len(rings) - 1):
        for k in range(n):
            a, b = rings[i][k], rings[i][(k + 1) % n]
            c, d = rings[i + 1][(k + 1) % n], rings[i + 1][k]
            m.oquad([a, b, c, d], (a + b + c + d) / 4 - (pts[i] + pts[i + 1]) / 2, tex)


def torus(m, c, ax, R, r, tex, n=16, k=6):
    c = np.asarray(c, float)
    w = np.asarray(ax, float) / np.linalg.norm(ax)
    uu = np.cross(w, (0, 1, 0) if abs(w[1]) < 0.9 else (1, 0, 0))
    uu /= np.linalg.norm(uu)
    vv = np.cross(w, uu)
    P = lambda i, j: c + (uu * math.cos(2 * math.pi * i / n) + vv * math.sin(2 * math.pi * i / n)) * (R + r * math.cos(2 * math.pi * j / k)) \
        + w * r * math.sin(2 * math.pi * j / k)  # noqa: E731
    for i in range(n):
        for j in range(k):
            pts = [P(i, j), P(i + 1, j), P(i + 1, j + 1), P(i, j + 1)]
            ctr = c + (uu * math.cos(2 * math.pi * (i + .5) / n) + vv * math.sin(2 * math.pi * (i + .5) / n)) * R
            m.oquad(pts, np.mean(pts, axis=0) - ctr, tex)


def bar(m, p0, p1, wy, wz, tex, up=(0, 1, 0)):
    """A rectangular bar from p0 to p1, wy across `up` and wz across both."""
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    ax = p1 - p0
    L = np.linalg.norm(ax)
    ax /= L
    ay = np.asarray(up, float) - ax * (ax @ np.asarray(up, float))
    if np.linalg.norm(ay) < 1e-6:
        ay = np.array([1.0, 0, 0]) - ax * ax[0]
    ay /= np.linalg.norm(ay)
    az = np.cross(ax, ay)
    c = (p0 + p1) / 2
    X, Y, Z = ax * L / 2, ay * wy / 2, az * wz / 2
    Q = lambda sx, sy, sz: c + X * sx + Y * sy + Z * sz  # noqa: E731
    for f, n in (((Q(-1, 1, -1), Q(1, 1, -1), Q(1, 1, 1), Q(-1, 1, 1)), ay), ((Q(-1, -1, 1), Q(1, -1, 1), Q(1, -1, -1), Q(-1, -1, -1)), -ay),
                 ((Q(-1, 1, 1), Q(1, 1, 1), Q(1, -1, 1), Q(-1, -1, 1)), az), ((Q(1, 1, -1), Q(-1, 1, -1), Q(-1, -1, -1), Q(1, -1, -1)), -az),
                 ((Q(1, 1, 1), Q(1, 1, -1), Q(1, -1, -1), Q(1, -1, 1)), ax), ((Q(-1, 1, -1), Q(-1, 1, 1), Q(-1, -1, 1), Q(-1, -1, -1)), -ax)):
        m.oquad(list(f), n, tex)


def bezier(p0, p1, p2, p3, n=10):
    p0, p1, p2, p3 = (np.asarray(p, float) for p in (p0, p1, p2, p3))
    return [(1 - s) ** 3 * p0 + 3 * (1 - s) ** 2 * s * p1 + 3 * (1 - s) * s * s * p2 + s ** 3 * p3 for s in np.linspace(0, 1, n + 1)]


# ------------------------------------------------------------------ cartons

def carton(m, c, w, h, d, item, qty, seed, part_no):
    """A closed regular slotted carton: printed kraft sides, the shipping label on its end, the outer flaps meeting along
    the top with their seam, the brown tape over it and down both ends, the corrugated edge of the flaps showing at the
    ends."""
    c = np.asarray(c, float)
    front = T.carton_front(w, h, item, qty, seed)
    back = T.carton_front(w, h, item, qty, seed + 50)
    end = T.carton_end(d, h, item, seed, part_no)
    rbox(m, c, w, h, d, 0.005, {"s": front, "n": back, "e": end, "w": M["kraft"], "u": M["kraft"], "d": M["kraft"], "edge": M["kraft"]},
         seg=2)
    y1 = c[1] + h
    # the seam between the outer flaps and the tape over it
    panel(m, (c[0] - w / 2 + 0.004, y1 + 0.0004, c[2] - 0.0012), (w - 0.008, 0, 0), (0, 0, 0.0024), M["black"])
    tw = min(0.05, d * 0.3)
    panel(m, (c[0] - w / 2 - 0.0008, y1 + 0.0009, c[2] - tw / 2), (w + 0.0016, 0, 0), (0, 0, tw), M["tape"])
    for sx in (1, -1):
        x = c[0] + sx * (w / 2 + 0.0009)
        drop = min(0.07, h * 0.3)
        panel(m, (x, y1 + 0.0009, c[2] - sx * tw / 2), (0, 0, sx * tw), (0, -drop, 0), M["tape"])
        # the flaps' corrugated edges along the top of each end
        panel(m, (x - sx * 0.0004, y1, c[2] - sx * (d / 2 - 0.006)), (0, 0, sx * (d - 0.012)), (0, -0.005, 0), M["edge"])


def open_carton(m, c, w, h, d, item, qty, seed, part_no, fill=None):
    """An opened carton: its four flaps folded out and hanging, the inside kraft and whatever is in it."""
    c = np.asarray(c, float)
    t = 0.004
    front = T.carton_front(w, h, item, qty, seed)
    end = T.carton_end(d, h, item, seed, part_no)
    x0, x1, z0, z1, y0, y1 = c[0] - w / 2, c[0] + w / 2, c[2] - d / 2, c[2] + d / 2, c[1], c[1] + h
    box(m, (x0, y0, z1 - t), (x1, y1, z1), {"s": front, "n": M["kraft_in"], "e": M["edge"], "w": M["edge"], "u": M["edge"], "d": None})
    box(m, (x0, y0, z0), (x1, y1, z0 + t), {"n": M["kraft"], "s": M["kraft_in"], "e": M["edge"], "w": M["edge"], "u": M["edge"], "d": None})
    box(m, (x1 - t, y0, z0 + t), (x1, y1, z1 - t), {"e": end, "w": M["kraft_in"], "u": M["edge"], "n": None, "s": None, "d": None})
    box(m, (x0, y0, z0 + t), (x0 + t, y1, z1 - t), {"w": M["kraft"], "e": M["kraft_in"], "u": M["edge"], "n": None, "s": None, "d": None})
    box(m, (x0 + t, y0, z0 + t), (x1 - t, y0 + t, z1 - t), {"u": M["kraft_in"], "d": M["kraft"]}, faces="ud")
    # the flaps, folded out and down over the sides (outer flaps along x, end flaps shorter)
    fl = d / 2 * 0.95
    for sz in (1, -1):
        z = z1 if sz > 0 else z0
        ang = math.radians(118)
        dv = np.array([0, math.cos(ang), sz * math.sin(ang)]) * fl
        p0 = np.array([x0 + 0.002, y1, z])
        quad = [p0, p0 + (w - 0.004, 0, 0), p0 + (w - 0.004, 0, 0) + dv, p0 + dv]
        m.oquad(quad, np.cross(dv, (1, 0, 0)) * sz * -1, M["kraft_in"])
        m.oquad(quad, np.cross(dv, (1, 0, 0)) * sz, M["kraft"])
    fe = min(w, d) * 0.35
    for sx in (1, -1):
        x = x1 if sx > 0 else x0
        ang = math.radians(105)
        dv = np.array([sx * math.sin(ang), math.cos(ang), 0]) * fe
        p0 = np.array([x, y1, z0 + 0.002])
        quad = [p0, p0 + (0, 0, d - 0.004), p0 + (0, 0, d - 0.004) + dv, p0 + dv]
        m.oquad(quad, np.cross((0, 0, 1), dv) * sx, M["kraft_in"])
        m.oquad(quad, np.cross((0, 0, 1), dv) * -sx, M["kraft"])
    if fill:
        fill(m, (x0 + t, y0 + t, z0 + t), (x1 - t, y1 - 0.01, z1 - t))


def product_boxes(m, lo, hi, seed, label="MOTOR 2810"):
    """White retail boxes standing in an open carton, their tops printed."""
    rnd = np.random.default_rng(seed)
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    bw, bd = 0.07, 0.07
    top = _product_top(label)
    side = _product_side(label)
    x = x0 + 0.004
    while x + bw < x1:
        z = z0 + 0.004
        while z + bd < z1:
            hh = (y1 - y0) * (0.86 + 0.08 * rnd.random())
            box(m, (x, y0, z), (x + bw - 0.003, y0 + hh, z + bd - 0.003), {"u": top, "s": side, "n": side, "e": side, "w": side, "d": None})
            z += bd
        x += bw


_cache = {}


def _product_top(label):
    if ("pt", label) not in _cache:
        from PIL import Image, ImageDraw
        img = Image.new("RGBA", (128, 128), (240, 240, 236, 255))
        d = ImageDraw.Draw(img)
        d.rectangle((0, 92, 128, 128), fill=(30, 34, 44, 255))
        T.logo(d, 10, 10, 18, (30, 34, 44, 255))
        T.text(d, label, (10, 40, 118, 70), (30, 34, 44, 255), T.FB)
        T.text(d, "BRUSHLESS", (10, 98, 118, 122), (255, 176, 70, 255), T.FC)
        _cache[("pt", label)] = T.arr(img)
    return _cache[("pt", label)]


def _product_side(label):
    if ("ps", label) not in _cache:
        from PIL import Image, ImageDraw
        img = Image.new("RGBA", (128, 128), (240, 240, 236, 255))
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, 128, 30), fill=(30, 34, 44, 255))
        T.text(d, label, (6, 4, 122, 26), (255, 255, 255, 255), T.FC, anchor="mm")
        d.ellipse((34, 44, 94, 104), fill=(70, 74, 80, 255))
        d.ellipse((46, 56, 82, 92), fill=(188, 104, 52, 255))
        _cache[("ps", label)] = T.arr(img)
    return _cache[("ps", label)]


# ------------------------------------------------------------------ hopper bins

def hopper_bin(m, x0, y, zf, w, h, d, card, contents, seed):
    """A stacking hopper bin (as Akro-Mils makes them): the tall back, side walls falling to a low front with its label
    window, the hang lip at the back, moulded 3 mm walls; its contents heaped inside."""
    t = 0.003
    zb = zf - d
    hf = h * 0.42
    xa, xb = x0, x0 + w
    B, Bi = M["bin"], M["bin_in"]
    # the side walls (outside and in): back top, front top, front bottom, back bottom
    for x, s in ((xa, -1), (xb, 1)):
        pts = [(x, y + h, zb), (x, y + hf, zf), (x, y, zf), (x, y, zb)]
        m.oquad(pts, (s, 0, 0), B)
        xi = x - s * t
        m.oquad([(xi, y + h, zb), (xi, y + hf, zf), (xi, y, zf), (xi, y, zb)], (-s, 0, 0), Bi)
        # the sloping top edge
        m.oquad([(x, y + h, zb), (xi, y + h, zb), (xi, y + hf, zf), (x, y + hf, zf)], (0, 1, (h - hf) / d), B)
    box(m, (xa, y, zb), (xb, y + h, zb + t), {"n": B, "s": Bi, "u": B}, faces="nsu")
    box(m, (xa + t, y, zb + t), (xb - t, y + t, zf - t), {"u": Bi, "d": B}, faces="ud")
    box(m, (xa, y, zf - t), (xb, y + hf, zf), {"s": B, "n": Bi, "u": B}, faces="snu")
    box(m, (xa - 0.002, y + h - 0.012, zb - 0.010), (xb + 0.002, y + h, zb), B, faces="nuewd")          # the hang lip
    # the label window: a moulded frame round the card
    lw, lh = w * 0.62, hf * 0.55
    lx, ly = xa + (w - lw) / 2, y + hf * 0.22
    panel(m, (lx, ly + lh, zf + 0.0012), (lw, 0, 0), (0, -lh, 0), card)
    for (a, b) in (((lx - 0.004, ly + lh, zf), (lx + lw + 0.004, ly + lh + 0.004, zf + 0.003)),
                   ((lx - 0.004, ly - 0.004, zf), (lx + lw + 0.004, ly, zf + 0.003))):
        box(m, a, b, B, faces="sue")
    if contents:
        contents(m, (xa + t, y + t, zb + t), (xb - t, y + hf * 0.9, zf - t), seed)


def heap(kind):
    """Bin contents: a mound of the parts (its surface printed with them, T.heap_texture) and loose pieces modelled on it."""
    def fill(m, lo, hi, seed):
        rnd = np.random.default_rng(seed)
        x0, y0, z0 = lo
        x1, y1, z1 = hi

        def height(x, z):
            fx = 1 - abs((x - (x0 + x1) / 2) / ((x1 - x0) / 2)) ** 2
            fz = 1 - (z - z0) / (z1 - z0)
            return y0 + (y1 - y0) * (0.08 + 0.70 * max(0.0, fx) * (0.35 + 0.65 * fz))
        tex = T.heap_texture(kind, 0)
        nx, nz = 5, 4
        for i in range(nx):
            for j in range(nz):
                xa, xb = x0 + (x1 - x0) * i / nx, x0 + (x1 - x0) * (i + 1) / nx
                za, zb = z0 + (z1 - z0) * j / nz, z0 + (z1 - z0) * (j + 1) / nz
                pts = [(xa, height(xa, za), za), (xb, height(xb, za), za), (xb, height(xb, zb), zb), (xa, height(xa, zb), zb)]
                m.oquad(pts, (0, 1, 0), tex, uvs=uv4(i / nx, j / nz, (i + 1) / nx, (j + 1) / nz))
        n = {"screws": 14, "nuts": 12, "xt90": 6, "bearings": 8, "esc": 4, "standoffs": 10, "gps": 3}[kind]
        for i in range(n):
            x = x0 + 0.012 + rnd.random() * (x1 - x0 - 0.024)
            z = z0 + 0.015 + rnd.random() * (z1 - z0 - 0.03)
            yy = height(x, z)
            a = rnd.random() * math.pi
            if kind == "screws":
                dv = np.array([math.cos(a), 0.1, math.sin(a)]) * 0.012
                cyl(m, (x, yy, z), (x + dv[0], yy + dv[1], z + dv[2]), 0.0016, M["black"], n=4, caps=False)
                cyl(m, (x, yy, z), (x - dv[0] * 0.2, yy, z - dv[2] * 0.2), 0.003, M["black"], n=6)
            elif kind == "nuts":
                cyl(m, (x, yy, z), (x, yy + 0.0024, z), 0.0034, M["steel"], n=6)
                cyl(m, (x, yy + 0.0024, z), (x, yy + 0.0036, z), 0.0028, M["shrink_blue"], n=6)
            elif kind == "xt90":
                dv = np.array([math.cos(a), 0, math.sin(a)])
                pv = np.array([-dv[2], 0, dv[0]])
                cc = np.array([x, yy + 0.005, z])
                for s_ in (-1, 1):
                    q = cc + pv * s_ * 0.005
                    cyl(m, q - dv * 0.011, q + dv * 0.011, 0.0055, M["yellow"], n=6)
            elif kind == "bearings":
                cyl(m, (x, yy, z), (x, yy + 0.005, z), 0.008, M["chrome"], n=10, cap=M["black"])
            elif kind == "esc":
                c = np.array([x, yy, z])
                rbox(m, c, 0.035, 0.006, 0.020, 0.004, M["shrink_blue"], seg=1)
                tube(m, [c + (0.017, 0.003, 0), c + (0.04, 0.003, 0.01)], 0.0018, M["wire_red"], n=4)
                tube(m, [c + (0.017, 0.003, -0.004), c + (0.04, 0.003, -0.002)], 0.0018, M["wire_black"], n=4)
            elif kind == "standoffs":
                dv = np.array([math.cos(a), 0, math.sin(a)]) * 0.02
                cyl(m, (x, yy + 0.003, z), (x + dv[0], yy + 0.003, z + dv[2]), 0.0028, M["brass"], n=6)
            elif kind == "gps":
                cyl(m, (x, yy, z), (x, yy + 0.010, z), 0.022, M["graphite_dk"], n=14, cap=M["graphite"])
                cyl(m, (x, yy + 0.010, z), (x, yy + 0.012, z), 0.012, M["white"], n=10)
    return fill


# ------------------------------------------------------------------ hard cases

def hard_case(m, c, w, h, d, colour, title, sub, seed, latches=2):
    """A moulded protective case (Pelican-style): base and lid with the tongue-and-groove seam, two raised ribs round
    the base, latches sitting in recesses across the seam, the fold-down handle, padlock hasps, the purge valve, an
    engraved plate riveted into a recess on the lid, feet."""
    c = np.asarray(c, float)
    body = M["case_orange" if colour == "orange" else "case_black"]
    r = min(0.03, w * 0.12)
    hs = h * 0.64
    rbox(m, c, w, hs, d, r, body, seg=4)
    rbox(m, c + (0, hs, 0), w - 0.006, 0.007, d - 0.006, r * 0.9, M["black"], seg=4, faces="nsew")     # the seam
    rbox(m, c + (0, hs + 0.007, 0), w, h - hs - 0.007, d, r, body, seg=4)
    # the lid's raised moulded panel
    rbox(m, c + (0, h, 0), w - 0.07, 0.006, d - 0.07, r * 0.7, body, seg=4, faces="nsewu")
    for y in (hs * 0.30, hs * 0.66):
        rbox(m, c + (0, y, 0), w + 0.008, 0.010, d + 0.008, r + 0.004, body, seg=4, faces="nsewud")
    xs = np.linspace(-w * 0.25, w * 0.25, latches)
    zf = c[2] + d / 2
    for x in xs:
        x = c[0] + x
        # the recess round the latch, then the latch over the seam
        panel(m, (x - 0.032, c[1] + hs + 0.05, zf + 0.0006), (0.064, 0, 0), (0, -0.10, 0), M["black"])
        rbox(m, (x, c[1] + hs - 0.045, zf - 0.006), 0.052, 0.088, 0.020, 0.006, M["latch"], seg=2)
        box(m, (x - 0.018, c[1] + hs + 0.026, zf + 0.004), (x + 0.018, c[1] + hs + 0.034, zf + 0.0045), M["graphite"], faces="s")
        cyl(m, (x - 0.026, c[1] + hs - 0.036, zf + 0.003), (x + 0.026, c[1] + hs - 0.036, zf + 0.003), 0.005, M["steel"], n=8)
    # the fold-down handle between the latches, lying against the lid's front
    hx0 = c[0] + (xs[0] + xs[-1]) / 2 if latches > 1 else c[0]
    for s in (-1, 1):
        box(m, (hx0 + s * 0.06 - 0.012, c[1] + hs + 0.012, zf), (hx0 + s * 0.06 + 0.012, c[1] + hs + 0.040, zf + 0.016), M["latch"])
    rbox(m, (hx0, c[1] + hs + 0.004, zf + 0.022), 0.15, 0.024, 0.022, 0.010, M["rubber"], seg=2)
    # padlock hasps (steel inserts) at both ends of the seam
    for s in (-1, 1):
        x = c[0] + s * (w / 2 - r - 0.012)
        box(m, (x - 0.008, c[1] + hs - 0.016, zf), (x + 0.008, c[1] + hs + 0.022, zf + 0.008), M["steel"])
        panel(m, (x - 0.004, c[1] + hs + 0.008, zf + 0.0085), (0.008, 0, 0), (0, -0.010, 0), M["black"])
    # the purge valve on the lid's front
    vx = c[0] + w / 2 - r - 0.024
    cyl(m, (vx, c[1] + hs + (h - hs) * 0.55, zf), (vx, c[1] + hs + (h - hs) * 0.55, zf + 0.008), 0.018, M["black"], n=12, cap=M["graphite"])
    # the plate in its recess on the lid
    pw, ph = min(w * 0.42, 0.20), min(d * 0.28, 0.066)
    ytop = c[1] + h + 0.006
    box(m, (c[0] - pw / 2 - 0.006, ytop, c[2] - ph / 2 - 0.006), (c[0] + pw / 2 + 0.006, ytop + 0.002, c[2] + ph / 2 + 0.006), M["black"], faces="u")
    panel(m, (c[0] - pw / 2, ytop + 0.0028, c[2] - ph / 2), (pw, 0, 0), (0, 0, ph), T.id_plate(title, sub, seed))
    for sx in (-1, 1):
        for sz in (-1, 1):
            cyl(m, (c[0] + sx * (w / 2 - 0.03), c[1] - 0.006, c[2] + sz * (d / 2 - 0.03)), (c[0] + sx * (w / 2 - 0.03), c[1], c[2] + sz * (d / 2 - 0.03)),
                0.012, M["rubber"], n=8)


# ------------------------------------------------------------------ smart batteries and their station

def smart_pack(m, late, c, w, h, d, level, seed):
    """A large smart flight battery standing in its station: rounded moulded body, ribbed grip sides, the moulded carry
    handle over the top, the front with its embossed name and printed spec, a power button and four gauge LEDs (live)."""
    c = np.asarray(c, float)
    face = T.smart_battery_face(7, 3)
    side = T.plastic(64, 256, (50, 53, 58), 8, 2.0, rib=6)
    rbox(m, c, w, h, d, 0.012, {"s": face, "n": M["graphite"], "e": side, "w": side, "u": M["graphite_dk"], "d": M["black"],
                                "edge": M["graphite"]}, seg=3)
    # the handle: two posts and the grip bar, moulded with the top
    yt = c[1] + h
    for s in (-1, 1):
        rbox(m, c + (0, h, s * d * 0.32), w * 0.86, 0.034, 0.020, 0.008, M["graphite"], seg=2)
    rbox(m, c + (0, h + 0.030, 0), w * 0.86, 0.016, d * 0.84, 0.008, M["graphite_dk"], seg=2)
    # the power button in its well, the gauge LEDs beside it
    zf = c[2] + d / 2
    by = yt - h * 0.26
    cyl(m, (c[0] - w * 0.26, by, zf), (c[0] - w * 0.26, by, zf + 0.003), 0.006, M["graphite"], n=10, cap=M["graphite_dk"])
    g = late.next_id("gauge")
    panel(m, (c[0] - w * 0.12 - 0.006, by + 0.005, zf + 0.0008), (w * 0.345 + 0.005, 0, 0), (0, -0.010, 0), M["black"])
    for i in range(4):
        x = c[0] - w * 0.12 + i * w * 0.115
        late.live_panel(GAUGE, g * 4 + i, (x - 0.0035, by + 0.003, zf + 0.0014), (0.007, 0, 0), (0, -0.006, 0))


def battery_station(m, late, c, rows, cols, pack, seed):
    """The battery station (a rugged charging case the packs stand in): chamfered moulded body, a slot a pack in rows,
    the status screen and a LED a slot on its sloped front, carry handles at the ends, rubber bumpers, the mains lead."""
    c = np.asarray(c, float)
    pw, ph, pd = pack
    gap = 0.012
    W = cols * (pw + gap) + 0.08
    D = rows * (pd + gap) + 0.10
    Hh = 0.11
    rbox(m, c, W, Hh, D, 0.03, {"edge": M["graphite_dk"], "s": T.vents(128, 64, (40, 42, 46), seed), "n": M["graphite_dk"],
                                "e": M["graphite_dk"], "w": M["graphite_dk"], "u": M["graphite_dk"], "d": M["black"]}, seg=4)
    # the sloped control strip along the front of the top
    zf = c[2] + D / 2
    pts = [(c[0] - W / 2 + 0.03, c[1] + Hh + 0.012, zf - 0.07), (c[0] + W / 2 - 0.03, c[1] + Hh + 0.012, zf - 0.07),
           (c[0] + W / 2 - 0.03, c[1] + Hh, zf - 0.005), (c[0] - W / 2 + 0.03, c[1] + Hh, zf - 0.005)]
    m.oquad(pts, (0, 1, 0.2), M["graphite"])
    sx0 = c[0] - 0.09
    tl = np.array([sx0, c[1] + Hh + 0.0105, zf - 0.062])
    r_ = np.array([0.15, 0, 0])
    dn = np.array([0, -0.0085, 0.048])
    m.oquad([tl + (0, 0.0003, 0), tl + r_ + (0, 0.0003, 0), tl + r_ + dn + (0, 0.0003, 0), tl + dn + (0, 0.0003, 0)], (0, 1, 0.18),
            M["black"])
    late.live_quad(STATION, 0, [tl + (0, 0.0009, 0), tl + r_ + (0, 0.0009, 0), tl + r_ + dn + (0, 0.0009, 0), tl + dn + (0, 0.0009, 0)])
    rnd = np.random.default_rng(seed)
    # the slots: dark recesses in the top, a pack in each
    k = 0
    for j in range(rows):
        for i in range(cols):
            x = c[0] - W / 2 + 0.04 + i * (pw + gap) + pw / 2
            z = c[2] - D / 2 + 0.03 + j * (pd + gap) + pd / 2 - 0.01
            panel(m, (x - pw / 2 - 0.004, c[1] + Hh + 0.0006, z - pd / 2 - 0.004), (pw + 0.008, 0, 0), (0, 0, pd + 0.008), M["black"])
            level = int(rnd.integers(1, 5))
            smart_pack(m, late, (x, c[1] + Hh - 0.03, z), pw, ph, pd, level, seed * 10 + k)
            # the slot's LED on the front strip
            lx = c[0] - W / 2 + 0.06 + k * (W - 0.12) / (rows * cols - 1) if rows * cols > 1 else c[0]
            late.live_panel(SLOT, k, (lx - 0.004, c[1] + Hh + 0.0035, zf - 0.020), (0.008, 0, 0), (0, -0.002, 0.008))
            k += 1
    # handles at the ends, bumpers at the corners, the mains lead out of the back
    for s in (-1, 1):
        x = c[0] + s * (W / 2 + 0.012)
        for z in (-0.06, 0.06):
            box(m, (x - 0.006, c[1] + 0.03, c[2] + z - 0.008), (x + 0.006, c[1] + 0.08, c[2] + z + 0.008), M["black"])
        box(m, (x + s * 0.004 - 0.008, c[1] + 0.07, c[2] - 0.07), (x + s * 0.004 + 0.008, c[1] + 0.085, c[2] + 0.07), M["rubber"])
    tube(m, bezier((c[0] + W / 2 - 0.06, c[1] + 0.04, c[2] - D / 2), (c[0] + W / 2 - 0.06, c[1] + 0.04, c[2] - D / 2 - 0.06),
                   (c[0] + W / 2 + 0.02, c[1] + 0.01, c[2] - D / 2 - 0.04), (c[0] + W / 2 + 0.06, c[1] + 0.006, c[2] - D / 2 - 0.02), 8),
         0.006, M["black"])
    return W, D


# ------------------------------------------------------------------ LiPo packs, the safe and the charger

def lipo(m, c, L, w, h, cells, mah, seed, lead_to=None, colour=(30, 120, 210), via=None):
    """A LiPo flight pack lying flat: the printed wrap, its main leads (red and black silicone) to an XT90 plug, the
    balance lead (white ribbon) to its plug; the leads run to lead_to if given (the charger's port)."""
    c = np.asarray(c, float)
    wrap = T.lipo_wrap(cells, mah, seed, colour)
    rbox(m, c, w, h, L, 0.006, {"u": wrap, "edge": M["black"], "e": M["black"], "w": M["black"], "s": M["black"], "n": M["black"],
                               "d": M["black"]}, seg=2)
    # the leads leave the front end (+z)
    e = c + (0, h * 0.5, L / 2)
    if lead_to is None:
        end = e + (0.02, -h * 0.3, 0.09)
        mid1, mid2 = e + (0, 0, 0.04), end + (-0.02, 0, -0.03)
    else:
        end = np.asarray(lead_to, float)
        mid1 = e + (0, 0.03, 0.06)
        mid2 = end + (0, 0.06, 0.04)
    for s, tex in ((-1, M["wire_red"]), (1, M["wire_black"])):
        off = np.array([s * 0.006, 0, 0])
        if via is None:
            tube(m, bezier(e + off, mid1 + off, mid2 + off, end + off, 10), 0.003, tex, n=6)
        else:
            v = np.asarray(via, float) + off
            pts = bezier(e + off, e + off + (0, 0.02, 0.03), v - (0, 0.03, 0.02), v, 6)
            pts += bezier(v, v + (0, 0.04, 0.03), end + off + (0, 0.05, 0.03), end + off, 8)[1:]
            tube(m, pts, 0.003, tex, n=6)
    # the XT90 at the end of the leads
    dv = end - mid2
    dv = dv / (np.linalg.norm(dv) + 1e-9)
    for s in (-1, 1):
        q = end + np.array([s * 0.006, 0, 0]) + dv * 0.012
        cyl(m, end + np.array([s * 0.006, 0, 0]), q, 0.0065, M["yellow"], n=6)
    # the balance lead: a thin white ribbon to its white plug
    bs = e + (w * 0.3, -h * 0.2, 0)
    bend = (end + (0.017, -0.004, 0.0)) if lead_to is not None else (bs + (0.03, -h * 0.4, 0.05))
    if via is None:
        tube(m, bezier(bs, bs + (0.01, 0.02, 0.04), bend + (0, 0.04, 0.03), bend, 8), 0.0022, M["wire_white"], n=4)
    else:
        v = np.asarray(via, float) + (0.016, 0, 0)
        pts = bezier(bs, bs + (0, 0.02, 0.03), v - (0, 0.03, 0.02), v, 6) + bezier(v, v + (0, 0.04, 0.03), bend + (0, 0.05, 0.03), bend, 8)[1:]
        tube(m, pts, 0.0022, M["wire_white"], n=4)
    box(m, bend - (0.009, 0.004, 0.004), bend + (0.009, 0.004, 0.004), M["white"])


def lipo_safe(m, c, w, h, d, packs, seed, charger_ports=None):
    """A steel LiPo charging safe with its lid open: double-walled black steel, the vent filter in the lid, the cable
    pass-through slot at the front, the packs lying inside with their leads out through the slot."""
    c = np.asarray(c, float)
    t = 0.006
    x0, x1, z0, z1, y0, y1 = c[0] - w / 2, c[0] + w / 2, c[2] - d / 2, c[2] + d / 2, c[1], c[1] + h
    S = M["safe"]
    box(m, (x0, y0, z1 - t), (x1, y1, z1), {"s": S, "n": M["graphite_dk"], "u": M["steel"], "e": S, "w": S, "d": None})
    box(m, (x0, y0, z0), (x1, y1, z0 + t), {"n": S, "s": M["graphite_dk"], "u": M["steel"], "e": S, "w": S, "d": None})
    for x, s in ((x0, -1), (x1 - t, 1)):
        box(m, (x, y0, z0 + t), (x + t, y1, z1 - t), {"e" if s < 0 else "w": M["graphite_dk"], "w" if s < 0 else "e": S, "u": M["steel"],
                                                       "n": None, "s": None, "d": None})
    box(m, (x0 + t, y0, z0 + t), (x1 - t, y0 + t, z1 - t), {"u": M["graphite_dk"]}, faces="u")
    # the cable slot (a rubber grommet strip) at the front top edge
    panel(m, (c[0] - w * 0.3, y1 + 0.0005, z1 - t - 0.0005), (w * 0.6, 0, 0), (0, 0, t + 0.001), M["rubber"])
    # the lid, hinged at the back, open past upright
    ang = math.radians(100)
    up = np.array([0, math.sin(ang), -math.cos(ang)])
    lid_d = d
    p0 = np.array([x0, y1, z0])
    outer = [p0 + up * lid_d, p0 + up * lid_d + (w, 0, 0), p0 + (w, 0, 0), p0]
    m.oquad(outer, np.cross(up, (1, 0, 0)) * -1, S)
    m.oquad(outer, np.cross(up, (1, 0, 0)), M["graphite_dk"])
    vc = p0 + (w / 2, 0, 0) + up * lid_d * 0.5 + np.cross(up, (1, 0, 0)) * -0.002
    panel(m, vc - (w * 0.2, 0, 0) + up * 0.04, (w * 0.4, 0, 0), -up * 0.08, T.vents(64, 32, (36, 38, 42), seed + 3))
    # the handle on the lid
    hc = p0 + (w / 2, 0, 0) + up * lid_d * 0.85 + np.cross(up, (1, 0, 0)) * -0.012
    for s in (-1, 1):
        cyl(m, hc + (s * 0.05, 0, 0), hc + (s * 0.05, 0, 0) - np.cross(up, (1, 0, 0)) * -0.012, 0.004, M["chrome"], n=6)
    cyl(m, hc - (0.05, 0, 0), hc + (0.05, 0, 0), 0.006, M["chrome"], n=8)
    # the packs inside
    n = len(packs)
    for i, (cells, mah, col) in enumerate(packs):
        x = x0 + t + (w - 2 * t) * (i + 0.5) / n
        lead = charger_ports[i] if charger_ports else None
        via = (x, y1 + 0.012, z1 - t / 2) if lead is not None else None
        lipo(m, (x, y0 + t + 0.001, c[2] - 0.012), 0.16, min(0.068, (w - 2 * t) / n - 0.008), 0.042, cells, mah, cells,
             lead_to=lead, colour=col, via=via)


def balance_charger(m, late, c, seed, levels):
    """A four-channel balance charger: its moulded case with side vents, the colour screen (live), the rotary knob and
    buttons, four XT60 outputs and four balance boards along the front, rubber feet; its power brick behind."""
    c = np.asarray(c, float)
    w, h, d = 0.20, 0.072, 0.15
    v = T.vents(96, 64, (40, 42, 46), seed)
    rbox(m, c + (0, 0.006, 0), w, h, d, 0.010, {"edge": M["graphite"], "e": v, "w": v, "s": M["graphite_dk"], "n": M["graphite_dk"],
                                                 "u": M["graphite"], "d": M["black"]}, seg=2)
    zf = c[2] + d / 2
    panel(m, (c[0] - w / 2 + 0.010, c[1] + h - 0.002, zf + 0.0006), (0.104, 0, 0), (0, -0.044, 0), M["black"])
    late.live_panel(CHARGER, late.next_id("charger"), (c[0] - w / 2 + 0.012, c[1] + h - 0.004, zf + 0.0012), (0.10, 0, 0), (0, -0.040, 0))
    cyl(m, (c[0] + 0.055, c[1] + h - 0.022, zf), (c[0] + 0.055, c[1] + h - 0.022, zf + 0.012), 0.012, M["black"], n=14, cap=M["graphite"])
    for i in range(2):
        box(m, (c[0] + 0.022 + i * 0.054 - 0.008 - (0.012 if i else 0), c[1] + h - 0.050, zf), (c[0] + 0.030 + i * 0.054 - (0.012 if i else 0),
            c[1] + h - 0.042, zf + 0.003), M["black"])
    ports = []
    xl = c[0] - w / 2
    for i in range(4):
        z = c[2] - d / 2 + 0.025 + i * 0.033
        box(m, (xl - 0.004, c[1] + 0.030, z - 0.010), (xl, c[1] + 0.040, z + 0.010), M["yellow"])
        box(m, (xl - 0.003, c[1] + 0.016, z - 0.008), (xl, c[1] + 0.022, z + 0.008), M["white"])
        ports.append(np.array([xl - 0.016, c[1] + 0.035, z]))
    for sx in (-1, 1):
        for sz in (-1, 1):
            cyl(m, (c[0] + sx * 0.08, c[1], c[2] + sz * 0.06), (c[0] + sx * 0.08, c[1] + 0.006, c[2] + sz * 0.06), 0.008, M["rubber"], n=8)
    # the power brick and its lead
    bc = c + (0, 0, -d / 2 - 0.05)
    rbox(m, bc, 0.16, 0.038, 0.07, 0.010, M["black"], seg=2)
    tube(m, bezier(bc + (0.08, 0.02, 0), bc + (0.12, 0.02, 0), c + (w / 2 + 0.02, 0.03, -0.02), c + (w / 2, 0.035, 0.0), 6), 0.004, M["black"], n=6)
    return ports


# ------------------------------------------------------------------ the extinguisher

def extinguisher(m, c, wall_dir=(0, 0, -1)):
    """A 6 kg stored-pressure powder extinguisher: a lathe-turned cylinder (domed foot ring, the shoulder and neck), the
    label printed round its front, the chrome valve with carry handle and squeeze lever, the pressure gauge, the
    safety pin with its ring and the green tamper seal, the hose clipped to the side and its horn."""
    c = np.asarray(c, float)
    R = 0.078
    body = [(0.0, 0.0), (0.06, 0.0), (0.072, 0.006), (0.074, 0.018), (R, 0.04), (R, 0.43), (0.074, 0.455), (0.062, 0.475),
            (0.042, 0.488), (0.022, 0.494), (0.0, 0.495)]
    lathe(m, c, body, M["red"], n=28)
    # the black foot ring
    lathe(m, c, [(0.074, 0.0), (0.0752, 0.02)], M["black"], n=28)
    # the printed label round the front 150 degrees
    lab = T.extinguisher_label()
    fwd = math.atan2(1, 0)          # +z faces the user
    a0, a1 = fwd - math.radians(75), fwd + math.radians(75)
    lathe(m, c, [(R + 0.0008, 0.10), (R + 0.0008, 0.38)], lab, n=20, a0=a0, a1=a1)
    # the valve: the neck nut, the body, the handles
    top = c + (0, 0.495, 0)
    cyl(m, top - (0, 0.004, 0), top + (0, 0.022, 0), 0.020, M["chrome"], n=12)
    rbox(m, top + (0, 0.022, 0), 0.040, 0.040, 0.030, 0.006, M["chrome"], seg=2)
    # carry handle (lower, fixed) and squeeze lever (upper), both reaching back over the cylinder
    for y, ln, tex, tilt in ((0.030, 0.13, M["chrome"], -6), (0.050, 0.12, M["black"], 10)):
        a = math.radians(tilt)
        p0 = top + (0, y, 0)
        p1 = p0 + np.array([-math.cos(a) * ln, math.sin(a) * ln, 0])
        bar(m, p0, p1, 0.007, 0.016, tex)
        bar(m, p1 + (0.004, 0, 0), p1 + (0.004, -0.010 if tilt < 0 else 0.008, 0), 0.007, 0.016, tex)
    # the gauge, facing the user
    g = top + (0.0, 0.040, 0.016)
    cyl(m, g, g + (0, 0, 0.012), 0.017, M["chrome"], n=14, cap=T.gauge_dial())
    # the safety pin through the valve, its ring, the green tamper seal
    pin_a, pin_b = top + (0.004, 0.044, -0.02), top + (0.004, 0.044, 0.024)
    cyl(m, pin_a, pin_b, 0.0022, M["chrome"], n=5)
    torus(m, pin_b + (0, 0, 0.012), (1, 0, 0), 0.012, 0.0022, M["chrome"])
    tube(m, [pin_b + (0, -0.002, 0.004), pin_b + (0.01, -0.012, 0.008), top + (0.016, 0.03, 0.012)], 0.0016, M["green"], n=4)
    # the hose: out of the valve, down the side to its clip, the horn
    h0 = top + (0.020, 0.030, 0)
    side = np.array([R + 0.012, 0, 0])
    pts = bezier(h0, h0 + (0.06, -0.03, 0.0), c + side + (0.02, 0.36, 0.01), c + side + (0, 0.30, 0.012), 10)
    pts += bezier(c + side + (0, 0.30, 0.012), c + side + (-0.002, 0.24, 0.014), c + side + (0, 0.14, 0.014), c + side + (0, 0.10, 0.014), 6)[1:]
    tube(m, pts, 0.0085, M["black"], n=8)
    cyl(m, pts[0], pts[0] + (0.012, -0.004, 0), 0.0115, M["chrome"], n=8)
    lathe(m, c + side + (0, 0.04, 0.014), [(0.004, 0.0), (0.016, 0.004), (0.012, 0.06), (0.0095, 0.065)], M["black"], n=10)
    box(m, c + side + (-0.012, 0.165, 0.004), c + side + (0.006, 0.18, 0.026), M["black"])         # the hose clip


def ext_bracket(m, c, plate_w=0.20, plate_h=0.36):
    """The steel wall bracket: a plate bolted across the end frame's uprights, the J-hook carrying the valve's neck and
    a strap round the cylinder; the fire-point sign above it."""
    c = np.asarray(c, float)
    box(m, (c[0] - plate_w / 2, c[1], c[2] - 0.004), (c[0] + plate_w / 2, c[1] + plate_h, c[2]), M["steel"])
    for sx in (-1, 1):
        for y in (0.03, plate_h - 0.03):
            cyl(m, (c[0] + sx * (plate_w / 2 - 0.02), c[1] + y, c[2]), (c[0] + sx * (plate_w / 2 - 0.02), c[1] + y, c[2] + 0.006), 0.007,
                M["chrome"], n=6)


def ext_sign(m, c, size=0.20):
    c = np.asarray(c, float)
    box(m, (c[0] - size / 2 - 0.004, c[1] - 0.004, c[2] - 0.003), (c[0] + size / 2 + 0.004, c[1] + size + 0.004, c[2]), M["white"])
    panel(m, (c[0] - size / 2, c[1] + size, c[2] + 0.0006), (size, 0, 0), (0, -size, 0), T.ext_sign())


# ------------------------------------------------------------------ motors in foam, carbon

def bell_top():
    from PIL import Image, ImageDraw
    img = Image.new("RGBA", (128, 128), (150, 156, 164, 255))
    d = ImageDraw.Draw(img)
    for k in range(6):
        a0 = k * 60 + 8
        d.pieslice((14, 14, 114, 114), a0, a0 + 38, fill=(16, 16, 18, 255))
    d.ellipse((44, 44, 84, 84), fill=(150, 156, 164, 255))
    d.ellipse((58, 58, 70, 70), fill=(70, 74, 80, 255))
    for k in range(4):
        a = math.radians(k * 90 + 45)
        d.ellipse((64 + math.cos(a) * 14 - 3, 64 + math.sin(a) * 14 - 3, 64 + math.cos(a) * 14 + 3, 64 + math.sin(a) * 14 + 3),
                  fill=(30, 30, 32, 255))
    return T.arr(img)


def bell_side():
    """The bell's side as it wraps round (top of the image to the top of the bell): brushed anodised metal, a polished
    ring at the top, and in the lower band the slanted vent windows with the copper windings dark behind them."""
    from PIL import Image, ImageDraw
    w, h = 256, 64
    rnd = np.random.default_rng(31)
    a = np.ones((h, w, 3), np.float32) * np.array([58, 62, 68], np.float32)
    a += rnd.normal(0, 2.5, (1, w, 1))
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, w, 6), fill=(168, 174, 182, 255))
    for x in range(0, w, 16):
        d.polygon([(x + 2, 40), (x + 9, 40), (x + 13, 56), (x + 6, 56)], fill=(54, 30, 16, 255))
        d.line((x + 4, 44, x + 10, 54), fill=(150, 84, 42, 255), width=2)
    return T.arr(img)


M["bell_top"] = bell_top()
M["bell_side"] = bell_side()


def motor(m, cx, y, cz, r, h, leads=True, seed=0):
    """A brushless outrunner: the base mount, the bell with its slanted vents (windings showing through), the spoked top,
    the shaft and its nut; three silicone leads with gold bullets."""
    cyl(m, (cx, y, cz), (cx, y + h * 0.18, cz), r * 0.92, M["black"], n=16)
    lathe(m, (cx, y + h * 0.18, cz), [(r * 0.98, 0.0), (r, h * 0.05), (r, h * 0.78), (r * 0.94, h * 0.82)], M["bell_side"], n=18)
    cyl(m, (cx, y + h, cz), (cx, y + h + 0.0008, cz), r * 0.94, M["bell_top"], n=18, cap=M["bell_top"])
    cyl(m, (cx, y + h, cz), (cx, y + h + 0.014, cz), r * 0.10, M["chrome"], n=8)
    cyl(m, (cx, y + h + 0.004, cz), (cx, y + h + 0.010, cz), r * 0.2, M["steel"], n=6)
    if leads:
        rnd = np.random.default_rng(seed)
        for k in range(3):
            a = math.radians(-30 + k * 30) + rnd.random() * 0.2
            s = np.array([cx + r * 0.9 * math.cos(a), y + 0.004, cz + r * 0.9 * math.sin(a)])
            e = s + np.array([math.cos(a) * 0.05, 0.0, math.sin(a) * 0.05 + 0.02])
            tube(m, bezier(s, s + (math.cos(a) * 0.02, 0.006, math.sin(a) * 0.02), e - (0, -0.004, 0.01), e, 5), 0.0018, M["wire_black"], n=4)
            cyl(m, e, e + (math.cos(a) * 0.008, 0, math.sin(a) * 0.008 + 0.004), 0.0022, M["brass"], n=5)


def foam_case(m, x0, x1, y, z0, z1, rows, cols, r, h, seed):
    """Pick-and-pluck foam in a shallow tray: each motor sits in its own pocket, sunk to half its height."""
    depth = h * 0.5
    box(m, (x0, y, z0), (x1, y + 0.006, z1), M["graphite_dk"], faces="nsewu")
    box(m, (x0 + 0.004, y + 0.006, z0 + 0.004), (x1 - 0.004, y + 0.006 + depth, z1 - 0.004), M["foam"], faces="nsewu")
    for i in range(cols):
        for j in range(rows):
            cx = x0 + (i + 0.5) * (x1 - x0) / cols
            cz = z0 + (j + 0.5) * (z1 - z0) / rows
            cyl(m, (cx, y + 0.006 + depth + 0.0006, cz), (cx, y + 0.006 + depth + 0.0007, cz), r * 1.08, M["foam_pit"], n=16)
            motor(m, cx, y + 0.006 + depth * 0.1, cz, r, h, leads=False)


def carbon_arm(m, a, b, r=0.022, mount=0.07, tex=None):
    a, b = np.asarray(a, float), np.asarray(b, float)
    cyl(m, a, b, r, M["carbon"], n=12, tile=0.03)
    d = (b - a) / np.linalg.norm(b - a)
    for e, s in ((a, 1), (b, -1)):
        c = e + d * s * (mount * 0.5)
        box(m, c - (mount / 2, -r + 0.004, mount / 2), c + (mount / 2, r + 0.006, mount / 2), (tex if tex is not None else M["alu_amber"]))
        for k in range(4):
            q = c + np.array([math.cos(k * math.pi / 2 + 0.785) * mount * 0.3, r + 0.0062, math.sin(k * math.pi / 2 + 0.785) * mount * 0.3])
            cyl(m, q, q + (0, 0.003, 0), 0.0045, M["black"], n=6)
        # the clamp collar where the tube enters the mount
        cyl(m, c - d * s * (mount / 2), c - d * s * (mount / 2 + 0.018), r * 1.25, (tex if tex is not None else M["alu_amber"]), n=12)
