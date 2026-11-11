"""The crew drop ship (approved 6 November 2026, take 4 of its sheet, the guns' close-ups): its mesh and textures for the
game. The same geometry the mockup drew (nt/dropship4.py): a lofted, faceted hull, the sponsons with their sliding
doors, the cranked wings and their lasers, the two VTOL thrusters, the troop bay, the ramp and the stern door, the
gear and the guns, as quads in the ship's frame (blocks; x to starboard, y up from the ground under its gear, z aft).

Writes assets/jugcraft/crew_ship/ship.bin.gz (the parts, each with its own frame, and the guns as separate models),
textures/entity/crew_ship/*.png (a tiling texture per material, 32 texels to the block on the hull, laid on each face at
its true size: world_uv), the flame (flame.png) and data/jugcraft/crew_ship/collision.bin.gz (the landed hull's shell in
half-block cells, for the game's collisions).
Run from the build root: python3 tools/crew_ship.py
"""
import gzip, json, math, os, struct, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
UV1 = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
_TEX = {}


def noise(n, seed, scale=1.0):
    rng = np.random.default_rng(seed)
    return rng.uniform(-1, 1, (n, n)) * scale


# Each material's texture: its size (texels) and how many blocks one repeat of it covers (Justin, 6 November 2026: "the
# texture ... looks super stretched across ... I'm fine with it being a texture as long as it's a good looking texture"):
# 32 texels to the block on the hull, laid on every face at its true size (export: world_uv), so nothing stretches.
# (Justin, 7 November 2026, on the rework's sheet: "see what textures you can make a higher quality"): 64 texels to the
# block on the hull and its plates (was 32), 64 on the fittings.
TEX_SIZE = {"hull": 512, "hull2": 512, "hullD": 512, "plate": 256, "dark": 256, "grille": 64, "metal": 128, "glass": 128,
            "floor": 128, "bay": 256, "hazard": 64, "rubber": 64, "chrome": 64, "nozzle": 128, "fabric": 64, "net": 64,
            "strap": 32, "cockpit": 256, "danger": 64, "mat": 64, "seat": 64, "fillet": 256, "fillet2": 256, "filletD": 256, "paint": 128, "boom": 64, "paintD": 128, "radome": 64}
TEX_BLOCKS = {"hull": 8.0, "hull2": 8.0, "hullD": 8.0, "plate": 4.0, "dark": 4.0, "grille": 1.0, "metal": 2.0, "glass": 2.0,
              "floor": 2.0, "bay": 4.0, "hazard": 1.0, "rubber": 1.0, "chrome": 1.0, "nozzle": 2.0, "fabric": 1.0, "net": 1.0,
              "strap": 0.5, "cockpit": 4.0, "danger": 0.5, "mat": 1.0, "seat": 1.0, "fillet": 4.0, "fillet2": 4.0, "filletD": 4.0, "paint": 4.0, "boom": 1.0, "paintD": 4.0, "radome": 2.0}
# drawn see-through in the game (a translucent pass): the canopy and the windows (Justin, 7 November 2026: "windows ...
# detail inside the hull and the pilot area")
TRANSLUCENT = {"glass", "lens_hud"}


# Shaded smooth across their creases (a normal at each corner, averaged over the faces meeting there that lie within
# SMOOTH_ANGLE of each other): the hull's skin and the fairings between it and what stands out of it, so the hull
# reads as one shaped body (Justin, 7 November 2026: "one concurrent ship put together").
SMOOTH = {"skin", "skin2", "skinD", "fillet", "fillet2", "filletD", "hull", "hull2", "hullD", "paint", "boom", "hullL", "hullDL", "paintD", "radome", "hull2C"}
SMOOTH_ANGLE = 50.0


def face_normal(P):
    P = np.asarray(P, float)
    n = -np.cross(P[1] - P[0], P[2] - P[0])
    if np.linalg.norm(n) < 1e-12:
        n = -np.cross(P[2] - P[0], P[3] - P[0])
    return n / (np.linalg.norm(n) + 1e-12)


# Faces whose shading normals lean up by this much (a big upright surface standing on the roof, read against the roof
# under the game's entity light: the fin, Justin's playtest, "darker than the rest of the ship: match the colours"),
# keyed by the face's corners
LIFT = {}


def lifted(i, vs, faces):
    """The corner normals vs of face i leant up by its LIFT, if it has one."""
    k = LIFT.get(face_key(faces[i][0])) if LIFT else None
    if not k:
        return vs
    out = []
    for v in vs:
        w = np.asarray(v, float) + np.array([0.0, k, 0.0])
        out.append(w / (np.linalg.norm(w) + 1e-12))
    return np.array(out)


# Faces cut out of a bigger one (round an opening): their parent's corners, keyed by the face's own corners, so their
# shading is the parent's spread over them and no band shows round the opening (Justin, 8 November 2026: the vents)
PARENT_OF = {}
# Faces of a faceted fairing that each carry the panels of what they stand on (keyed by the face's corners: a group's
# name): the paint is laid over the creases inside a group, never from one group to another (tools/crew_ship_paint.py)
PETALS = {}


def face_key(P):
    return tuple(sorted(tuple(np.round(np.asarray(p_, float), 3)) for p_ in P))


def _inv_bilinear(p, C):
    """(a, b) with C0 + (C1 - C0) a + (C3 - C0) b + (C2 - C1 - C3 + C0) a b nearest p (Newton's method)."""
    a, b = 0.5, 0.5
    for _ in range(12):
        F = C[0] + (C[1] - C[0]) * a + (C[3] - C[0]) * b + (C[2] - C[1] - C[3] + C[0]) * a * b - p
        Ja = (C[1] - C[0]) + (C[2] - C[1] - C[3] + C[0]) * b
        Jb = (C[3] - C[0]) + (C[2] - C[1] - C[3] + C[0]) * a
        J = np.stack([Ja, Jb], 1)
        d = np.linalg.lstsq(J, -F, rcond=None)[0]
        a, b = a + d[0], b + d[1]
        if abs(d[0]) + abs(d[1]) < 1e-10:
            break
    return a, b


def vertex_normals(faces, names):
    """Each face's corner normals: None for a face shaded flat, else four normals averaged over the smooth faces that
    share the corner and lie within SMOOTH_ANGLE of this one (for a piece of a face cut round an opening, its parent's
    corners' normals spread over it)."""
    c = math.cos(math.radians(SMOOTH_ANGLE))
    fn = [face_normal(f[0]) for f in faces]
    area = [np.linalg.norm(np.cross(np.asarray(f[0][2]) - np.asarray(f[0][0]), np.asarray(f[0][3]) - np.asarray(f[0][1]))) + 1e-6 for f in faces]
    keyed = {}
    for i, f in enumerate(faces):
        if names[i] in SMOOTH:
            for p in f[0]:
                keyed.setdefault(tuple(np.round(np.asarray(p, float), 3)), []).append(i)
    def at_corner(p, i):
        acc = np.zeros(3)
        for j in keyed.get(tuple(np.round(np.asarray(p, float), 3)), []):
            if fn[j] @ fn[i] > c:
                acc += fn[j] * area[j]
        return acc / (np.linalg.norm(acc) + 1e-12) if np.linalg.norm(acc) > 1e-12 else fn[i]
    out = []
    for i, f in enumerate(faces):
        if names[i] not in SMOOTH:
            out.append(None)
            continue
        par = PARENT_OF.get(face_key(f[0])) if PARENT_OF else None
        if par is not None:
            Cn = [at_corner(q_, i) for q_ in par]
            vs = []
            for p in f[0]:
                a, b = _inv_bilinear(np.asarray(p, float), par)
                v = Cn[0] * (1 - a) * (1 - b) + Cn[1] * a * (1 - b) + Cn[2] * a * b + Cn[3] * (1 - a) * b
                vs.append(v / (np.linalg.norm(v) + 1e-12))
            out.append(lifted(i, np.array(vs), faces))
            continue
        out.append(lifted(i, np.array([at_corner(p, i) for p in f[0]]), faces))
    return out


def local_uv(name):
    """Materials laid on their face by its own corners (0..1, or along a strip), not at their true size: the painted
    words and the lamps' lenses."""
    return name in ("word", "louvre", "skin", "skin2", "skinD", "hullL", "hullDL", "hull2C", "blanket", "kick", "deck", "ceil") or \
        name.startswith(("st_", "lens_", "led_", "mdl_"))


def mat_alias(name):
    """The fairings use their paint's texture under a name of their own (so they shade smooth with the skin); hullL and
    hullDL: the paints laid where the part stands at rest, so on a part that turns (the thrusters) the panels turn
    with it, as the game draws them."""
    return {"fillet": "hull", "fillet2": "hull2", "filletD": "hullD", "hullL": "hull", "hullDL": "hullD", "hull2C": "hull2"}.get(name)


def periodic_noise(n, seed, cutoff):
    """Smooth noise that tiles (filtered in frequency space), -1..1; cutoff: the highest frequency kept (cycles a tile)."""
    rng = np.random.default_rng(seed)
    f = np.fft.fft2(rng.normal(0, 1, (n, n)))
    k = np.fft.fftfreq(n) * n
    kk = np.hypot(k[:, None], k[None, :])
    f *= np.exp(-(kk / cutoff) ** 2)
    out = np.real(np.fft.ifft2(f))
    return out / (np.abs(out).max() + 1e-9)


def plating(n, ppb, rgb, seed, rows=(1.0, 1.5, 2.0, 2.5, 3.0), widths=(1.5, 2.0, 2.5, 3.0, 4.0), seam=0.5, rivets=True, bolts=False):
    """Hull plating that tiles (ppb texels to the block): rows of panels of different sizes, each its own shade; recessed
    seams (a dark groove, its lower lip lit), screws along them (a dark ring, a lit head), paint worn pale along the
    panels' edges here and there, grime streaking down from the screws and seams, faint blotches, fine grain, scratches;
    now and then a small service label. Armour plates (bolts) get bevelled edges and big bolts in their corners."""
    rng = np.random.default_rng(seed)
    base = np.array(rgb, np.float32) / 255
    tone = np.ones((n, n), np.float32)
    seamm = np.zeros((n, n), np.float32)
    lip = np.zeros((n, n), np.float32)
    edge = np.zeros((n, n), np.float32)           # distance-to-edge band (1 at the panel's edge, fading in)
    bevel = np.zeros((n, n), np.float32)          # +1 lit (top, left), -1 shaded (bottom, right): plates only
    foot = np.zeros((n, n), np.float32)
    screws = []
    labels = []
    blocks = n / ppb
    sw = max(1, int(round(ppb / 32)))             # seam width in texels
    y = 0.0
    while y < blocks - 1e-6:
        h = min(float(rng.choice(rows)), blocks - y)
        if blocks - y - h < 0.75:
            h = blocks - y
        y0, y1 = int(round(y * ppb)), int(round((y + h) * ppb))
        x = float(rng.uniform(0, blocks))
        left = blocks
        while left > 1e-6:
            w = min(float(rng.choice(widths)), left)
            if left - w < 1.0:
                w = left
            x0, x1 = int(round(x * ppb)), int(round((x + w) * ppb))
            xs = np.arange(x0, x1) % n
            ys = np.arange(y0, y1) % n
            tone[np.ix_(ys, xs)] = 1 + rng.normal(0, 0.018)
            seamm[np.ix_(np.arange(y0, y0 + sw) % n, xs)] = 1
            seamm[np.ix_(ys, np.arange(x0, x0 + sw) % n)] = 1
            lip[np.ix_([(y0 + sw) % n], xs)] = 1
            lip[np.ix_(ys, [(x0 + sw) % n])] = 1
            yy = np.arange(y1 - y0)[:, None]; xx = np.arange(x1 - x0)[None, :]
            d = np.minimum(np.minimum(yy, y1 - y0 - 1 - yy), np.minimum(xx, x1 - x0 - 1 - xx)).astype(np.float32)
            edge[np.ix_(ys, xs)] = np.clip(1 - (d - sw) / (ppb * 0.05), 0, 1)
            foot[np.ix_(ys, xs)] = np.linspace(0, 1, y1 - y0)[:, None]
            if bolts:
                bw = max(2, int(ppb * 0.06))
                bv = np.zeros((y1 - y0, x1 - x0), np.float32)
                bv[sw:sw + bw, :] = 1; bv[:, sw:sw + bw] = 1
                bv[-bw:, :] = -1; bv[:, -bw:] = -1
                bevel[np.ix_(ys, xs)] = bv
                inset = int(ppb * 0.16)
                for (py, px) in ((y0 + inset, x0 + inset), (y0 + inset, x1 - inset), (y1 - inset, x0 + inset), (y1 - inset, x1 - inset)):
                    screws.append((py, px, max(2, int(ppb * 0.05))))
            elif rivets:
                step = max(6, int(ppb / 4))
                r = max(1, int(ppb / 40))
                for xx_ in range(x0 + step // 2, x1 - 2, step):
                    screws.append((y0 + sw + r + 2, xx_, r))
                for yy_ in range(y0 + step // 2, y1 - 2, step):
                    screws.append((yy_, x0 + sw + r + 2, r))
            if rng.random() < 0.12 and (x1 - x0) > ppb and (y1 - y0) > ppb * 0.8:
                labels.append((y0 + int(ppb * 0.3), x0 + int(ppb * 0.3), int(ppb * 0.42), int(ppb * 0.16)))
            x += w
            left -= w
        y += h
    grain = np.random.default_rng(seed + 1).normal(0, 0.018, (n, n))
    blot = periodic_noise(n, seed + 2, 6) * 0.04 + periodic_noise(n, seed + 5, 24) * 0.02
    shade = tone + grain + blot - 0.045 * foot ** 2
    img = base[None, None, :] * shade[..., None]
    # worn paint at the panels' edges, broken up by noise
    wear = edge * (periodic_noise(n, seed + 6, 40) > 0.15)
    img *= (1 + 0.09 * wear)[..., None]
    img *= (1 + 0.10 * bevel)[..., None]
    img *= (1 - (1 - seam) * seamm)[..., None]
    img *= (1 + 0.12 * lip * (1 - seamm))[..., None]
    # grime: streaks running down (the texture's v is down the face) from some screws and from the seams
    gr = np.random.default_rng(seed + 7)
    streak = np.zeros((n, n), np.float32)
    for (py, px, r) in screws:
        if gr.random() < 0.35:
            ln = int(gr.integers(ppb // 6, ppb // 1.5))
            a = gr.uniform(0.05, 0.16)
            for k in range(ln):
                streak[(py + r + k) % n, px % n] = max(streak[(py + r + k) % n, px % n], a * (1 - k / ln))
    for _ in range(int(n * n / 9000)):
        px, py = gr.integers(0, n, 2)
        ln = int(gr.integers(ppb // 4, ppb))
        a = gr.uniform(0.03, 0.08)
        wdt = int(gr.integers(1, 3))
        for k in range(ln):
            for dx in range(wdt):
                streak[(py + k) % n, (px + dx) % n] = max(streak[(py + k) % n, (px + dx) % n], a * (1 - k / ln))
    img *= (1 - streak)[..., None]
    # screws: a dark ring, the head lit up and to the left
    for (py, px, r) in screws:
        for dy in range(-r - 1, r + 2):
            for dx in range(-r - 1, r + 2):
                dd = math.hypot(dy, dx)
                yy_, xx_ = (py + dy) % n, (px + dx) % n
                if dd <= r + 0.5:
                    img[yy_, xx_] *= 1.12 if (dx + dy) < 0 else 0.92
                elif dd <= r + 1.4:
                    img[yy_, xx_] *= 0.8
    # small service labels: a pale panel with a dark border and two lines of print
    for (ly, lx, lw, lh) in labels:
        for dy in range(lh):
            for dx in range(lw):
                yy_, xx_ = (ly + dy) % n, (lx + dx) % n
                if dy in (0, lh - 1) or dx in (0, lw - 1):
                    img[yy_, xx_] = img[yy_, xx_] * 0.6
                elif dy in (lh // 3, 2 * lh // 3) and 2 < dx < lw - 3 and (dx // 2) % 3 != 2:
                    img[yy_, xx_] = img[yy_, xx_] * 0.75
                else:
                    img[yy_, xx_] = img[yy_, xx_] * 0.55 + np.array([0.62, 0.62, 0.58]) * 0.45
    # scratches: thin pale lines
    sr = np.random.default_rng(seed + 3)
    for _ in range(int(n * n / 2200)):
        cx, cy = sr.integers(0, n, 2)
        ang = sr.uniform(0, math.pi)
        ln = int(sr.integers(4, int(ppb * 0.4)))
        for k in range(ln):
            img[int(cy + k * math.sin(ang)) % n, int(cx + k * math.cos(ang)) % n] *= 1.08
    out = np.ones((n, n, 4), np.float32)
    out[..., :3] = np.clip(img, 0, 1)
    return out


def mat(name):
    """A tiling material (RGBA 0..1); its size and the blocks it covers in TEX_SIZE and TEX_BLOCKS (16 texels for the
    plain glowing ones)."""
    if name in _TEX:
        return _TEX[name]
    if mat_alias(name):                                     # the fairings: the hull's own panels and paint
        t = mat(mat_alias(name)).copy()
        _TEX[name] = t
        return t
    if name in ("skin", "skin2", "skinD"):                  # the hull's unwrapped skin: its paint's texture, laid round the hull
        t = mat({"skin": "hull", "skin2": "hull2", "skinD": "hullD"}[name]).copy()
        _TEX[name] = t
        return t
    n = TEX_SIZE.get(name, 16)
    ppb = n / TEX_BLOCKS.get(name, 1.0)
    seed = sum(ord(c) * 131 ** i for i, c in enumerate(name)) % 2 ** 31
    if name in ("hull2", "hullD"):                          # one panel layout under all three paints, so the seams run on
        seed = sum(ord(c) * 131 ** i for i, c in enumerate("hull")) % 2 ** 31
    def flat(rgb, var=0.0):
        t = np.ones((n, n, 4), np.float32)
        t[..., :3] = np.array(rgb) / 255 * (1 + var * np.random.default_rng(seed).uniform(-1, 1, (n, n, 1)))
        return t
    if name == "hull":
        t = plating(n, ppb, (70, 73, 84), seed, rows=(1.5, 2.0, 2.5), widths=(2.0, 2.5, 3.0), seam=0.64)
    elif name == "hull2":                                   # (one paint above the belly now: Justin, "more uniformity")
        t = plating(n, ppb, (70, 73, 84), seed, rows=(1.5, 2.0, 2.5), widths=(2.0, 2.5, 3.0), seam=0.64)
    elif name == "hullD":
        t = plating(n, ppb, (50, 52, 59), seed, rows=(1.5, 2.0, 2.5), widths=(2.0, 2.5, 3.0), seam=0.68)
    elif name == "plate":                                   # armour plates: big, bolted at the corners, thick seams
        t = plating(n, ppb, (82, 84, 94), seed, rows=(2.0,), widths=(2.0,), seam=0.42, rivets=False, bolts=True)
    elif name == "dark":
        t = plating(n, ppb, (28, 28, 33), seed, rows=(2.0,), widths=(2.0, 4.0), seam=0.7, rivets=False)
    elif name == "grille":                                  # louvres: a lit top edge, the slat shading down, a deep gap
        t = flat((40, 40, 46))
        per = n // 8
        for y in range(n):
            k = y % per
            f = 1.45 if k == 0 else 0.25 if k >= per - 2 else 1.1 - 0.5 * k / per
            t[y, :, :3] *= f
        t[..., :3] *= (1 + periodic_noise(n, seed, 4) * 0.05)[..., None]
    elif name == "metal":                                   # brushed steel: fine streaks along, a few scratches
        t = flat((128, 131, 140))
        rng = np.random.default_rng(seed)
        streak = rng.normal(0, 1, (n, 1)) * 0.035 + rng.normal(0, 0.018, (n, n)) + np.repeat(rng.normal(0, 0.02, (n, 1)), n, 1)
        t[..., :3] *= (1 + streak)[..., None]
        for _ in range(n // 6):
            cx, cy = rng.integers(0, n, 2); ang = rng.uniform(0, math.pi); ln = int(rng.integers(3, n // 6))
            for k in range(ln):
                t[int(cy + k * math.sin(ang)) % n, int(cx + k * math.cos(ang)) % n, :3] *= 1.1
    elif name == "chrome":                                  # a strut's polished piston: bands of reflection
        t = flat((160, 166, 176))
        xs = np.arange(n) / n
        band = 0.75 + 0.55 * np.exp(-((xs - 0.3) / 0.08) ** 2) - 0.25 * np.exp(-((xs - 0.7) / 0.12) ** 2)
        t[..., :3] *= band[None, :, None]
    elif name == "nozzle":                                  # the nozzles' metal, tempered by the heat: straw, blue, soot
        t = flat((96, 92, 90))
        h1, h2 = periodic_noise(n, seed, 3), periodic_noise(n, seed + 1, 6)
        straw = np.array([1.14, 1.03, 0.84]); blue = np.array([0.86, 0.9, 1.14])
        mixw = np.clip(h1 * 0.5 + 0.5, 0, 1)[..., None]
        t[..., :3] *= straw * mixw + blue * (1 - mixw)
        t[..., :3] *= (1 - 0.25 * np.clip(h2, 0, 1))[..., None]
        t[..., :3] *= (1 + np.random.default_rng(seed).normal(0, 0.03, (n, n)))[..., None]
    elif name == "glass":                                   # tinted glass: deep blue-green, the sky's sheen, two streaks
        t = flat((40, 66, 82))
        yy, xx = np.mgrid[0:n, 0:n] / n
        refl = periodic_noise(n, seed, 1.5) * 0.5 + 0.5
        t[..., :3] += (refl * 0.07)[..., None] * np.array([0.8, 1.0, 1.1])
        diag = (xx + yy) % 1.0
        st = np.exp(-((diag - 0.3) / 0.03) ** 2) * 0.10 + np.exp(-((diag - 0.42) / 0.012) ** 2) * 0.07
        t[..., :3] += st[..., None] * np.array([0.9, 1.0, 1.05])
        dust = np.random.default_rng(seed).random((n, n)) > 0.996
        t[dust, :3] += 0.05
        t[..., 3] = 0.42 + st * 1.5 + refl * 0.08                 # see-through, the streaks a little less
    elif name == "floor":                                   # the deck and the ramp: steel tread plate, worn bright
        ppb = n / TEX_BLOCKS["floor"]
        t = flat((92, 94, 102), 0.02)
        c = max(8, int(ppb / 6))                           # a lozenge every c texels
        yy, xx = np.mgrid[0:n, 0:n]
        cu, cv = (xx % c) - (c - 1) / 2, (yy % c) - (c - 1) / 2
        odd = ((xx // c + yy // c) % 2) == 1
        a = np.where(odd, cu + cv, cu - cv) / c
        b = np.where(odd, cu - cv, cu + cv) / c
        raised = (np.abs(a) < 0.13) & (np.abs(b) < 0.62)
        lit = (np.abs(a + 0.06) < 0.07) & (np.abs(b) < 0.6)
        shadow = (np.abs(a - 0.16) < 0.05) & (np.abs(b) < 0.62)
        wear = periodic_noise(n, seed + 3, 6) * 0.5 + 0.5
        t[raised, :3] *= 1.18
        t[lit, :3] *= 1.12
        t[shadow, :3] *= 0.72
        t[..., :3] *= (1 + 0.10 * wear * raised - 0.08 * (1 - wear) * (~raised))[..., None]
    elif name == "bay":                                     # inside: quilted grey insulation blankets, snapped on
        t = flat((112, 116, 112))
        yy, xx = np.mgrid[0:n, 0:n]
        c = int(ppb / 2)                                    # a diamond every half block
        a = ((xx + yy) % c) / c; b = ((xx - yy) % c) / c
        pillow = np.sin(a * math.pi) * np.sin(b * math.pi)
        t[..., :3] *= (0.8 + 0.28 * pillow)[..., None]
        stitch = (np.minimum(a, 1 - a) < 0.03) | (np.minimum(b, 1 - b) < 0.03)
        t[stitch, :3] *= 0.72
        snap = (np.minimum(a, 1 - a) < 0.07) & (np.minimum(b, 1 - b) < 0.07)
        t[snap, :3] = np.array([0.55, 0.56, 0.58])
        blanket = ((yy // (ppb * 2)) % 2 == 0) & (yy % (ppb * 2) < 2)
        t[blanket, :3] *= 0.5
        t[..., :3] *= (1 + periodic_noise(n, seed, 4) * 0.05 + np.random.default_rng(seed).normal(0, 0.02, (n, n)))[..., None]
    elif name == "blanket":                                 # the bay's walls (laid one panel to a tile, frame to frame): an
        # insulation blanket, quilted in channels, its edge bound in tape with snaps along it, a stencilled label
        H_, W_ = 208, 96
        t = np.ones((H_, W_, 4), np.float32)
        t[..., :3] = np.array([146, 150, 140]) / 255
        yy, xx = np.mgrid[0:H_, 0:W_]
        ch = (yy % 26) / 26.0                                # quilted channels, puffed between their stitch lines
        t[..., :3] *= (0.84 + 0.2 * np.sin(ch * math.pi) ** 0.6)[..., None]
        t[(yy % 26) < 1, :3] *= 0.7
        bx = (xx < 5) | (xx >= W_ - 5); by = (yy < 5) | (yy >= H_ - 5)
        t[bx | by, :3] = np.array([96, 100, 94]) / 255      # the binding tape
        t[(xx == 5) | (xx == W_ - 6) | (yy == 5) | (yy == H_ - 6), :3] *= 0.75
        for yv in range(13, H_, 26):                          # the snaps down both edges
            for xv in (2, W_ - 3):
                t[max(0, yv - 2):yv + 2, max(0, xv - 2):xv + 2, :3] = np.array([0.66, 0.67, 0.7])
                t[yv - 1:yv + 1, xv - 1:xv + 1, :3] = np.array([0.85, 0.86, 0.88])
        t[18:28, 24:72, :3] = np.array([0.82, 0.8, 0.62])     # a label: yellow-white, two dark lines of print
        t[20:22, 28:68, :3] = 0.25; t[24:26, 28:56, :3] = 0.25
        rng = np.random.default_rng(seed)
        t[..., :3] *= (1 + rng.normal(0, 0.025, (H_, W_)) + periodic_noise(max(H_, W_), seed, 5)[:H_, :W_] * 0.03)[..., None]
    elif name == "kick":                                    # the walls' foot: hard composite kick panels, bolted, scuffed
        H_, W_ = 72, 96
        t = np.ones((H_, W_, 4), np.float32)
        t[..., :3] = np.array([64, 67, 74]) / 255
        yy, xx = np.mgrid[0:H_, 0:W_]
        t[(xx < 2) | (yy < 2), :3] *= 0.55
        t[(xx == 2) | (yy == 2), :3] *= 1.25
        for xv in (6, W_ // 2, W_ - 6):
            for yv in (6, H_ - 6):
                t[yv - 1:yv + 2, xv - 1:xv + 2, :3] = np.array([0.55, 0.56, 0.6])
        rng = np.random.default_rng(seed)
        for _ in range(14):                                  # boot scuffs, low down
            cx_, cy_ = rng.integers(4, W_ - 4), rng.integers(H_ // 2, H_ - 4)
            t[cy_:cy_ + 2, cx_:cx_ + int(rng.integers(4, 14)), :3] *= 0.8
        t[..., :3] *= (1 + rng.normal(0, 0.02, (H_, W_)))[..., None]
    elif name == "deck":                                    # the bay's deck (a tile is four panels across, one along): plates
        # in a grit coating, the seams between, countersunk screws along them, a walkway's edge stripe worn pale
        H_, W_ = 96, 208
        t = np.ones((H_, W_, 4), np.float32)
        rng = np.random.default_rng(seed)
        t[..., :3] = np.array([84, 87, 94]) / 255
        t[..., :3] *= (1 + rng.normal(0, 0.06, (H_, W_)))[..., None]        # the grit
        yy, xx = np.mgrid[0:H_, 0:W_]
        pw = W_ // 4
        seam = ((xx % pw) < 2) | (yy < 2)
        t[seam, :3] = np.array([0.18, 0.18, 0.2])
        def screw(yv, xv):                                   # countersunk: a dark ring, the head's face in it
            t[yv - 1:yv + 2, xv - 1:xv + 2, :3] = np.array([0.25, 0.25, 0.27])
            t[yv, xv, :3] = np.array([0.4, 0.41, 0.44])
        for x0_ in range(0, W_, pw):
            for xv in (x0_ + 6, x0_ + pw - 6):
                for yv in range(8, H_, 16):
                    screw(yv, xv)
            for yv in (6, H_ - 5):
                for xv in range(x0_ + 12, x0_ + pw - 8, 12):
                    screw(yv, xv)
        wear = periodic_noise(max(H_, W_), seed + 2, 4)[:H_, :W_] * 0.5 + 0.5
        t[..., :3] *= (0.92 + 0.14 * wear)[..., None]
    elif name == "ceil":                                    # the bay's ceiling: perforated acoustic panels between the frames
        H_, W_ = 96, 96
        t = np.ones((H_, W_, 4), np.float32)
        t[..., :3] = np.array([132, 136, 140]) / 255
        yy, xx = np.mgrid[0:H_, 0:W_]
        holes = ((xx % 6) == 3) & ((yy % 6) == 3) & (xx > 8) & (xx < W_ - 8) & (yy > 8) & (yy < H_ - 8)
        t[holes, :3] *= 0.55
        t[(xx < 2) | (yy < 2), :3] *= 0.5
        t[(xx == 2) | (yy == 2), :3] *= 1.2
        t[..., :3] *= (1 + np.random.default_rng(seed).normal(0, 0.015, (H_, W_)))[..., None]
    elif name == "cockpit":                                 # the flight deck's lining: dark padded panels
        t = plating(n, ppb, (54, 56, 62), seed, rows=(1.0, 1.5), widths=(1.0, 1.5), seam=0.6, rivets=False)
    elif name == "fabric":                                  # the seats' cushions: a tight weave
        t = flat((52, 58, 66))
        yy, xx = np.mgrid[0:n, 0:n]
        t[..., :3] *= (1 + 0.06 * ((xx // 2 + yy // 2) % 2) - 0.03 + np.random.default_rng(seed).normal(0, 0.03, (n, n)))[..., None]
    elif name == "net":                                     # the jump seats' backs: webbing, open between (cut out)
        t = flat((70, 76, 64))
        yy, xx = np.mgrid[0:n, 0:n]
        k = n // 8
        web = ((xx % k) < 3) | ((yy % k) < 3)
        t[..., 3] = np.where(web, 1.0, 0.0)
        t[..., :3] *= (1 + np.random.default_rng(seed).normal(0, 0.05, (n, n)))[..., None]
    elif name == "strap":                                   # harness webbing: olive with a stitched edge
        t = flat((80, 86, 60))
        t[:, :2, :3] *= 0.7; t[:, -2:, :3] *= 0.7
        t[::4, :, :3] *= 0.9
    elif name == "danger":                                  # the intakes' lips: red and white chevrons
        t = flat((200, 30, 26))
        yy, xx = np.mgrid[0:n, 0:n]
        white = ((xx + np.abs((yy % (n // 2)) - n // 4)) % (n // 2)) < n // 8
        t[white, :3] = np.array([0.92, 0.92, 0.9])
        t[..., :3] *= (1 + np.random.default_rng(seed).normal(0, 0.04, (n, n)))[..., None]
    elif name == "hazard":                                  # yellow and black stripes, the paint chipped at the edges
        t = flat((26, 26, 26))
        yy, xx = np.mgrid[0:n, 0:n]
        per = n // 4
        ph = ((xx + yy) % per) / per
        yel = ph < 0.5
        t[yel, :3] = np.array([226, 178, 32]) / 255
        rng = np.random.default_rng(seed)
        chip = (np.minimum(np.abs(ph - 0.5), np.minimum(ph, 1 - ph)) < 0.06) & (rng.random((n, n)) > 0.55)
        t[chip & yel, :3] = np.array([70, 66, 58]) / 255
        t[chip & ~yel, :3] = np.array([150, 120, 40]) / 255
        t[..., :3] *= (1 + rng.normal(0, 0.05, (n, n)) + periodic_noise(n, seed + 1, 3) * 0.08)[..., None]
    elif name == "rubber":
        t = flat((22, 22, 24), 0.1)
    elif name == "mat":                                     # the flight deck's rubber mat: fine ribs
        t = flat((34, 35, 38), 0.06)
        t[::4, :, :3] *= 0.7
        t[1::4, :, :3] *= 1.15
    elif name == "seat":                                    # the pilots' seats: dark blue-grey fabric in stitched channels
        t = flat((74, 82, 96))
        yy, xx = np.mgrid[0:n, 0:n]
        ch = (xx % (n // 4)) / (n // 4)
        t[..., :3] *= (0.86 + 0.22 * np.sin(ch * math.pi))[..., None]
        t[(xx % (n // 4)) < 1, :3] *= 0.6
        t[..., :3] *= (1 + 0.04 * ((xx // 2 + yy // 2) % 2) + np.random.default_rng(seed).normal(0, 0.025, (n, n)))[..., None]
    elif name == "boom":                                    # the probes' tubes: smooth light metal, no streaks to stretch
        t = flat((156, 160, 168), 0.0)
        t[..., :3] *= (1 + np.random.default_rng(seed).normal(0, 0.012, (n, n)))[..., None]
    elif name == "paint":                                   # a moulded fairing: the skin's paint with no panels or screws
        t = flat((70, 73, 84), 0.0)
        t[..., :3] *= (1 + 0.03 * periodic_noise(n, seed, 5) + np.random.default_rng(seed).normal(0, 0.012, (n, n)))[..., None]
    elif name == "paintD":                                  # the darker paint, moulded (no panels)
        t = flat((50, 52, 59), 0.0)
        t[..., :3] *= (1 + 0.03 * periodic_noise(n, seed, 5) + np.random.default_rng(seed).normal(0, 0.012, (n, n)))[..., None]
    elif name == "radome":                                  # a radome's skin: smooth dark composite, a faint sheen
        t = flat((52, 55, 63), 0.0)
        t[..., :3] *= (1 + 0.02 * periodic_noise(n, seed, 3) + np.random.default_rng(seed).normal(0, 0.006, (n, n)))[..., None]
    elif name == "paint_red":
        t = flat((172, 32, 28), 0.05)
    elif name == "louvre":                                  # a vent's slats: plain painted steel, darker toward the gap
        t = flat((62, 64, 70), 0.03)
        t[..., :3] *= np.linspace(1.08, 0.82, n)[:, None, None]
    elif name == "glow":
        t = flat((178, 104, 255))
        t[..., :3] *= (1 + np.random.default_rng(seed).normal(0, 0.03, (n, n)))[..., None]
    elif name == "lamp":
        t = flat((255, 250, 232))
    elif name == "engine":
        t = flat((255, 214, 150))
    elif name == "red":
        t = flat((255, 36, 30))
    elif name == "green":
        t = flat((36, 255, 96))
    elif name == "white":
        t = flat((255, 255, 255))
    elif name.startswith(("lens_", "led_")):
        t = lens(name)
    else:
        t = flat((128, 128, 128))
    t = np.clip(t, 0, 1)
    _TEX[name] = t
    return t


def lens(name):
    """A lamp's lens (laid on by its corners): the floodlights' LED clusters in their reflectors, the navigation lights'
    and beacons' Fresnel rings, the tail lights' cells, the bay's light panels, the ramp's LED strip."""
    def radial(n, rgb, hot=1.0, rings=0):
        yy, xx = (np.mgrid[0:n, 0:n] + 0.5) / n - 0.5
        r = np.hypot(xx, yy) * 2
        t = np.ones((n, n, 4), np.float32)
        k = np.clip(1.0 - 0.45 * r ** 1.5, 0.45, 1.0)
        if rings:
            k *= 1 + 0.08 * np.cos(r * math.pi * rings * 2)
        t[..., :3] = np.array(rgb) / 255 * k[..., None]
        t[..., :3] += np.exp(-(r / 0.25) ** 2)[..., None] * 0.35 * hot
        return t
    if name == "lens_flood":                              # 4 x 3 white LEDs, each in a little round reflector
        w, h = 64, 48
        t = np.ones((h, w, 4), np.float32); t[..., :3] = 0.32
        for cy in range(3):
            for cx in range(4):
                yy, xx = np.mgrid[0:16, 0:16] + 0.5 - 8
                r = np.hypot(xx, yy) / 7.0
                cell = np.where(r <= 1, 0.62 + 0.38 * np.exp(-(r / 0.35) ** 2) + 0.1 * (1 - r), 0.3)
                t[cy * 16:(cy + 1) * 16, cx * 16:(cx + 1) * 16, :3] = cell[..., None] * np.array([1.0, 0.98, 0.93])
        return np.clip(t, 0, 1)
    if name == "lens_spot":                               # a round lamp: seven white LEDs in their reflectors, a dark ring
        n_ = 64
        yy, xx = np.mgrid[0:n_, 0:n_] + 0.5 - n_ / 2
        t = np.ones((n_, n_, 4), np.float32); t[..., :3] = 0.3
        t[np.hypot(xx, yy) > n_ / 2 - 3, :3] = 0.18
        for (ox, oy) in [(0, 0)] + [(19 * math.cos(k * math.pi / 3), 19 * math.sin(k * math.pi / 3)) for k in range(6)]:
            r = np.hypot(xx - ox, yy - oy) / 9.0
            cell = 0.62 + 0.38 * np.exp(-(r / 0.35) ** 2) + 0.1 * (1 - r)
            t[r <= 1, :3] = (cell[r <= 1, None] * np.array([1.0, 0.98, 0.93]))
        return np.clip(t, 0, 1)
    if name in ("lens_red", "lens_green", "lens_white"):
        rgb = {"lens_red": (255, 40, 32), "lens_green": (40, 255, 110), "lens_white": (250, 252, 255)}[name]
        return np.clip(radial(32, rgb, rings=4), 0, 1)
    if name == "lens_tail":                               # three red cells stacked, dark between
        w, h = 16, 48
        t = np.ones((h, w, 4), np.float32); t[..., :3] = np.array([0.35, 0.03, 0.03])
        for k in range(3):
            cell = radial(14, (255, 36, 30), rings=2)
            t[k * 16 + 1:k * 16 + 15, 1:15, :3] = cell[..., :3]
        return np.clip(t, 0, 1)
    if name == "lens_panel":                              # the bay's light panels: a prismatic diffuser in a soft frame
        w, h = 64, 32
        yy, xx = np.mgrid[0:h, 0:w]
        t = np.ones((h, w, 4), np.float32)
        k = 0.9 + 0.1 * np.minimum(np.minimum(xx, w - 1 - xx) / 6, np.minimum(yy, h - 1 - yy) / 6).clip(0, 1)
        k = k * np.where((xx % 4 == 0) | (yy % 4 == 0), 0.9, 1.0)
        t[..., :3] = k[..., None] * np.array([0.97, 0.99, 1.0])
        return np.clip(t, 0, 1)
    if name.startswith("lens_mfd_") or name in ("lens_cam", "lens_cpanel", "lens_exit", "lens_medkit", "lens_keys", "lens_key", "lens_gauge",
                                                 "lens_annun", "lens_hud", "lens_flares", "lens_mount", "lens_mountD"):
        return screen(name)
    if name == "led_strip":                               # along u: LEDs every quarter block behind a diffuser
        w, h = 16, 8
        yy, xx = (np.mgrid[0:h, 0:w] + 0.5)
        t = np.ones((h, w, 4), np.float32)
        across = np.exp(-((yy - h / 2) / (h * 0.32)) ** 2)
        dot = np.exp(-(((xx - w / 2) / 2.2) ** 2 + ((yy - h / 2) / 1.6) ** 2))
        k = 0.4 + 0.45 * across + 0.4 * dot
        t[..., :3] = np.clip(k, 0, 1)[..., None] * np.array([0.95, 0.98, 1.0])
        return t
    t = np.ones((16, 16, 4), np.float32)
    return t


def screen(name):
    """The flight deck's displays (they only look live: Justin's rule for cosmetic things) and panels; the bay's signs."""
    F2 = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
    if name == "lens_mfd_att":                              # attitude: sky over ground, the pitch ladder, speed and height tapes
        im = Image.new("RGB", (128, 96), (0, 0, 0)); d = ImageDraw.Draw(im)
        d.polygon([(14, 0), (114, 0), (114, 40), (14, 52)], fill=(40, 110, 200)); d.polygon([(14, 52), (114, 40), (114, 96), (14, 96)], fill=(130, 84, 40))
        d.line([(14, 52), (114, 40)], fill=(255, 255, 255), width=2)
        for k in (-2, -1, 1, 2):
            y = 46 - k * 12
            d.line([(48, y + 4 * (k * 0)), (80, y - 4)], fill=(240, 240, 240), width=1)
        d.line([(50, 48), (60, 48), (64, 54), (68, 48), (78, 48)], fill=(255, 220, 40), width=2)
        for x0 in (0, 114):
            d.rectangle((x0, 0, x0 + 13, 95), fill=(20, 20, 24))
            for y in range(4, 96, 10):
                d.line([(x0 + 2, y), (x0 + 7, y)], fill=(200, 200, 200))
        d.rectangle((0, 42, 13, 52), fill=(0, 0, 0), outline=(255, 255, 255)); d.rectangle((114, 42, 127, 52), fill=(0, 0, 0), outline=(255, 255, 255))
    elif name == "lens_mfd_radar":                          # a radar sweep: range rings, contacts
        im = Image.new("RGB", (128, 96), (2, 14, 6)); d = ImageDraw.Draw(im)
        for r in (14, 28, 42):
            d.arc((64 - r, 90 - r, 64 + r, 90 + r), 200, 340, fill=(30, 160, 70))
        d.line([(64, 90), (64, 46)], fill=(30, 160, 70)); d.pieslice((22, 48, 106, 132), 245, 275, fill=(40, 200, 90))
        for (x, y) in ((40, 60), (82, 54), (70, 70)):
            d.rectangle((x - 2, y - 2, x + 2, y + 2), fill=(250, 220, 60))
        d.text((4, 4), "RDR 20", font=ImageFont.truetype(F2, 10), fill=(60, 220, 110))
    elif name == "lens_mfd_map":                            # the moving map: contours, the route, the ship
        im = Image.new("RGB", (128, 96), (24, 40, 30)); d = ImageDraw.Draw(im)
        rng = np.random.default_rng(5)
        for k in range(7):
            cx, cy, r = rng.integers(10, 118), rng.integers(10, 86), rng.integers(10, 30)
            for rr_ in range(r, 2, -6):
                d.ellipse((cx - rr_, cy - rr_ * 0.7, cx + rr_, cy + rr_ * 0.7), outline=(60 + rr_, 110, 70))
        d.line([(10, 80), (40, 60), (70, 52), (110, 20)], fill=(255, 0, 255), width=2)
        d.polygon([(64, 50), (60, 60), (68, 60)], fill=(255, 255, 255))
    elif name == "lens_mfd_sys":                            # systems: the thrusters' bars, fuel, text
        im = Image.new("RGB", (128, 96), (6, 8, 12)); d = ImageDraw.Draw(im)
        f = ImageFont.truetype(F2, 9)
        for k, h in enumerate((62, 58)):
            x = 14 + k * 26
            d.rectangle((x, 20, x + 16, 84), outline=(120, 120, 130)); d.rectangle((x + 2, 84 - h, x + 14, 82), fill=(60, 220, 110))
            d.text((x, 86), "L" if k == 0 else "R", font=f, fill=(200, 200, 200))
        d.text((70, 18), "FUEL 82%", font=f, fill=(255, 190, 60)); d.text((70, 34), "HYD  OK", font=f, fill=(60, 220, 110))
        d.text((70, 50), "GEAR DN", font=f, fill=(60, 220, 110)); d.text((70, 66), "RAMP", font=f, fill=(255, 190, 60))
    elif name == "lens_cam":                                # the chin sensor's glass
        im = Image.new("RGB", (32, 32), (10, 10, 16)); d = ImageDraw.Draw(im)
        d.ellipse((4, 4, 28, 28), fill=(30, 20, 50)); d.ellipse((10, 10, 22, 22), fill=(70, 40, 120)); d.ellipse((12, 11, 16, 15), fill=(200, 180, 255))
    elif name == "lens_cpanel":                             # a switch panel: rows of toggles and their labels
        im = Image.new("RGB", (128, 64), (40, 42, 46)); d = ImageDraw.Draw(im)
        for row in range(4):
            for col in range(8):
                x, y = 8 + col * 15, 8 + row * 14
                d.rectangle((x, y, x + 6, y + 6), fill=(24, 24, 26)); d.rectangle((x + 2, y - 2, x + 4, y + 3), fill=(190, 190, 196))
                d.line([(x - 1, y + 10), (x + 7, y + 10)], fill=(150, 150, 150))
        d.rectangle((0, 0, 127, 63), outline=(20, 20, 22), width=2)
    elif name == "lens_keys":                               # a console's keypad and small screen
        im = Image.new("RGB", (64, 64), (36, 38, 42)); d = ImageDraw.Draw(im)
        d.rectangle((6, 4, 58, 20), fill=(8, 30, 14)); d.text((9, 7), "NAV 2", font=ImageFont.truetype(F2, 10), fill=(60, 220, 110))
        for r in range(3):
            for c in range(4):
                d.rectangle((7 + c * 13, 26 + r * 12, 16 + c * 13, 34 + r * 12), fill=(70, 72, 78), outline=(20, 20, 22))
    elif name == "lens_exit":                               # an EXIT sign over a door
        im = Image.new("RGB", (96, 32), (20, 160, 70)); d = ImageDraw.Draw(im)
        d.text((48, 17), "EXIT", font=ImageFont.truetype(FONT, 20), fill=(240, 255, 240), anchor="mm")
    elif name == "lens_key":                                # a display's key: grey, a pale mark on it
        im = Image.new("RGB", (16, 12), (70, 72, 78)); d = ImageDraw.Draw(im)
        d.rectangle((0, 0, 15, 11), outline=(30, 30, 34)); d.line([(5, 6), (10, 6)], fill=(220, 220, 220))
    elif name == "lens_gauge":                              # a standby gauge: a black dial, ticks, a needle
        im = Image.new("RGB", (48, 48), (12, 12, 14)); d = ImageDraw.Draw(im)
        d.ellipse((2, 2, 45, 45), fill=(8, 8, 10), outline=(150, 150, 150))
        for k in range(12):
            a = k * math.pi / 6
            d.line([(24 + 18 * math.cos(a), 24 + 18 * math.sin(a)), (24 + 21 * math.cos(a), 24 + 21 * math.sin(a))], fill=(230, 230, 230))
        d.line([(24, 24), (35, 14)], fill=(255, 255, 255), width=2)
    elif name == "lens_annun":                              # the warning lights: little windows, a few lit
        im = Image.new("RGB", (112, 16), (14, 14, 16)); d = ImageDraw.Draw(im)
        cols = [(60, 50, 20), (40, 120, 50), (60, 50, 20), (230, 170, 40), (40, 120, 50), (60, 50, 20), (40, 120, 50), (60, 20, 20)]
        for k, c in enumerate(cols):
            d.rectangle((2 + k * 14, 3, 12 + k * 14, 12), fill=c)
    elif name == "lens_hud":                                # the head-up display's glass: green symbology, see-through
        im = Image.new("RGBA", (96, 80), (40, 80, 60, 40)); d = ImageDraw.Draw(im)
        g = (90, 255, 140, 230)
        d.ellipse((42, 34, 54, 46), outline=g); d.line([(30, 40), (42, 40)], fill=g); d.line([(54, 40), (66, 40)], fill=g)
        for y in (20, 60):
            d.line([(32, y), (44, y)], fill=g); d.line([(52, y), (64, y)], fill=g)
        d.rectangle((6, 34, 22, 44), outline=g); d.rectangle((74, 34, 90, 44), outline=g)
        d.line([(30, 8), (66, 8)], fill=g)
        t = np.asarray(im, np.float32) / 255
        return t
    elif name in ("lens_mount", "lens_mountD"):              # a lamp's mounting panel: the skin's paint, a seam round it, screws
        base = (50, 52, 59) if name.endswith("D") else (70, 73, 84)
        n_ = 96
        rng = np.random.default_rng(7)
        t = np.ones((n_, n_ // 96 * 80, 4), np.float32)
        h_, w_ = 80, 96
        t = np.ones((h_, w_, 4), np.float32)
        t[..., :3] = np.array(base) / 255 * (1 + rng.normal(0, 0.016, (h_, w_)) + periodic_noise(96, 3, 6)[:h_, :w_] * 0.03)[..., None]
        t[0:2, :, :3] *= 0.5; t[-2:, :, :3] *= 0.5; t[:, 0:2, :3] *= 0.5; t[:, -2:, :3] *= 0.5
        t[2:3, 2:-2, :3] *= 1.12; t[2:-2, 2:3, :3] *= 1.12
        for (yy, xx) in [(6, 6), (6, w_ // 2), (6, w_ - 7), (h_ - 7, 6), (h_ - 7, w_ // 2), (h_ - 7, w_ - 7), (h_ // 2, 6), (h_ // 2, w_ - 7)]:
            t[yy - 1:yy + 2, xx - 1:xx + 2, :3] *= 0.7
            t[yy - 1, xx - 1, :3] *= 1.4
        return np.clip(t, 0, 1)
    elif name == "lens_flares":                             # a flare dispenser's face: rows of cartridges' caps
        im = Image.new("RGB", (64, 48), (44, 46, 50)); d = ImageDraw.Draw(im)
        for r in range(4):
            for c in range(6):
                x, y = 4 + c * 10, 4 + r * 11
                d.ellipse((x, y, x + 8, y + 8), fill=(20, 20, 22)); d.ellipse((x + 2, y + 2, x + 6, y + 6), fill=(70, 66, 50))
        d.rectangle((0, 0, 63, 47), outline=(24, 24, 26), width=2)
    elif name == "lens_medkit":                             # the first aid kit's lid
        im = Image.new("RGB", (32, 32), (230, 230, 226)); d = ImageDraw.Draw(im)
        d.rectangle((13, 6, 19, 26), fill=(210, 30, 30)); d.rectangle((6, 13, 26, 19), fill=(210, 30, 30))
    t = np.ones((im.height, im.width, 4), np.float32)
    t[..., :3] = np.asarray(im, np.float32) / 255
    return t


def flame_texture():
    """The thrusters' flame (textures/entity/crew_ship/flame.png): white streaks of fire up its length that tile along it
    (the renderer slides it down the plume and tints it), soft at the sides."""
    w, h = 64, 256
    rng = np.random.default_rng(7)
    f = np.fft.fft2(rng.normal(0, 1, (h, w)))
    ky = np.fft.fftfreq(h)[:, None] * h
    kx = np.fft.fftfreq(w)[None, :] * w
    f *= np.exp(-(ky / 10.0) ** 2 - (kx / 6.0) ** 2)      # long along the flame, tight across it
    s = np.real(np.fft.ifft2(f))
    s = (s - s.min()) / (s.max() - s.min())
    a = np.clip(0.35 + 0.9 * (s - 0.35), 0, 1)
    img = np.ones((h, w, 4), np.float32)
    img[..., 3] = a
    return img


GLOWS = {"glow", "lamp", "engine", "red", "green", "white"}


def wordmark():
    """The NARV-TECH name painted on the flanks (white on the hull), its own texture."""
    if "word" in _TEX:
        return _TEX["word"]
    # (only the letters: the hull shows round them; the quad is 3.9 x 0.65 blocks, so about 32 texels to the block)
    im = Image.new("RGBA", (16 * 48, 16 * 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(FONT, 96)
    d.text((384, 66), "NARV-TECH", font=f, fill=(222, 223, 230, 255), anchor="mm")
    t = np.asarray(im, np.float32) / 255
    _TEX["word"] = t
    return t



def stencil(text, rgb=(222, 223, 230), w=384, h=64, size=46, stroke=0):
    """A painted stencil (letters only; the hull shows round them), its own texture named st_<text>."""
    name = "st_" + text.lower().replace(" ", "_").replace("-", "_")
    if name in _TEX:
        return _TEX[name]
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.text((w / 2, h / 2 + 2), text, font=ImageFont.truetype(FONT, size), fill=rgb + (255,), anchor="mm", stroke_width=stroke, stroke_fill=rgb + (255,))
    _TEX[name] = np.asarray(im, np.float32) / 255
    return _TEX[name]


def sign(text, fg=(236, 240, 236), bg=(18, 92, 52), w=256, h=48, size=30):
    """A lit sign's face: letters on its colour, its own texture named st_sign_<text>."""
    name = "st_sign_" + text.lower().replace(" ", "_").replace("-", "_")
    if name in _TEX:
        return _TEX[name]
    im = Image.new("RGBA", (w, h), bg + (255,))
    d = ImageDraw.Draw(im)
    d.rectangle((2, 2, w - 3, h - 3), outline=tuple(min(255, int(c * 1.6) + 20) for c in bg) + (255,), width=2)
    d.text((w / 2, h / 2 + 1), text, font=ImageFont.truetype(FONT, size), fill=fg + (255,), anchor="mm")
    _TEX[name] = np.asarray(im, np.float32) / 255
    return _TEX[name]


HAZARD_EDGE = 0.08               # a hazard-striped box's faces narrower than this are its edges
SPINE_TOP = 1.195                # the roof ridge's top, half across (its sides 52 degrees up from the roof)


def box_faces(lo, hi, m, glow, tile=1.0):
    x0, y0, z0 = lo; x1, y1, z1 = hi
    t = wordmark() if m == "word" else mat(m)
    fs = []
    # (corners, the face's size in blocks along its u and v for tiling)
    for P, su, sv in (([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], x1 - x0, z1 - z0),     # top
                      ([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], x1 - x0, z1 - z0),     # bottom
                      ([(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)], x1 - x0, y1 - y0),     # aft
                      ([(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], x1 - x0, y1 - y0),     # fore
                      ([(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)], z1 - z0, y1 - y0),     # starboard
                      ([(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], z1 - z0, y1 - y0)):    # port
        if su <= 1e-6 or sv <= 1e-6:
            continue
        if m == "hazard" and min(su, sv) < HAZARD_EDGE:
            # (a striped bar's or plate's thin edges: plain dark. Striped too, each edge carried the stripes at its own
            # place, so along the kick strip's top yellow teeth stood against the black of its face: Justin's picture of
            # the bay, 10 November 2026)
            fs.append((np.array(P, float), np.array([(0, 0), (su, 0), (su, sv), (0, sv)], float), mat("rubber"), False))
            continue
        UV = np.array([(0, 0), (su, 0), (su, sv), (0, sv)], float) / tile if m != "word" else UV1
        if m == "word" and abs(P[0][0] - x1) < 1e-9 and abs(P[1][0] - x1) < 1e-9:   # the starboard face: read from outside
            UV = UV1[[1, 0, 3, 2]]
        fs.append((np.array(P, float), UV, t, glow))
    return fs



# The far model (Justin, 8 November 2026: "Build, with distance cutoff"): beyond CrewShipRenderer.FAR the ship is drawn
# from a second build with its round parts in fewer sides, its humps coarser and its bolt heads left off.
FAR = False


class _Here:
    """This module, for the tools it hands itself to (it may be loaded under another name)."""

    def __getattr__(self, k):
        return globals()[k]


_HERE = _Here()
_NEAR_LAID = []                  # the near model's laid paint, for the far model to take its panels from


def _near_laid():
    global FAR
    if not _NEAR_LAID:
        was, FAR = FAR, False
        try:
            Ship()
        finally:
            FAR = was
    return _NEAR_LAID[0]


def far_sides(n, least=6):
    return max(least, n // 3) if FAR else n


# Optimising (Justin, 9 November 2026: "optimize what you can"), for everything at once rather than part by part:
# anything round gets as many sides as its size wants (an edge no shorter than ROUND_SEG near, ROUND_SEG_FAR in the far
# model; never more than it asked for, never under six, four for the smallest); the far model leaves out anything
# smaller than FAR_LEAST across.
ROUND_SEG, ROUND_SEG_FAR, FAR_LEAST = 0.08, 0.25, 0.1


def round_sides(n, r, least=6):
    """The sides for something round of radius r that asked for n (n of four or under: a square bar, as asked)."""
    if n <= 4:
        return n
    least = 4 if r < 0.03 else least
    want = math.ceil(2 * math.pi * r / (ROUND_SEG_FAR if FAR else ROUND_SEG))
    return max(min(least, n), min(n // 3 if FAR else n, want))


def too_small(size, thick=None):
    """Whether something size across (its largest extent; thick: a rod's or a strut's thickness) is left out: of the far
    model when under FAR_LEAST, and of the collision cells (COLLIDING) when under a cell or thinner than COLLIDE_THIN
    (Justin's playtest of the crew list, via the C4 test: the troop seats' struts caught the crew standing up; nobody
    trips on a strut, a bolt or a ring)."""
    if COLLIDING and (size < COLLIDE or thick is not None and thick < COLLIDE_THIN):
        return True
    return FAR and size < FAR_LEAST


COLLIDING, COLLIDE_THIN = False, 0.1


G = 2.6                                     # the belly's height on its gear
# Openings cut through the hull's skin for the vents set into it (Justin, 7 November 2026: fix them one by one): the
# facet (the hull's own numbering, 0-13), z from and to, and how far across the facet (0 at its point j, 1 at j + 1).
# The landing gear (Justin's playtest, 8 November 2026: "make it actually deploy, with working doors and stowing
# inside"): four telescoping legs, each in a well let into the belly's flat and closed by two doors that slide fore and
# aft. Down, the doors open first (up into the hull, then aside), then the legs run out; up, the other way round.
# GEAR_AT: the legs (x, z); GEAR_WELL: a well's half size and depth; the legs' stages: a fixed sleeve and five that run
# out, each GEAR_STAGE long and GEAR_RUN further than the one above it (stowed, all up in the sleeve with the footpad
# under them, just inside the doors; down, the pad on the ground); the doors' rise into the hull and slide.
GEAR_AT = [(-3.2, -9.5), (3.2, -9.5), (-3.2, 12.5), (3.2, 12.5)]
GEAR_WELL = (0.9, 0.95)
GEAR_TOP = G + 0.935                         # the running stages' tops, stowed
GEAR_STAGE, GEAR_RUN = 0.70, (G + 0.035) / 5
GEAR_RADII = (0.44, 0.39, 0.34, 0.29, 0.24, 0.19)
GEAR_DOOR_RISE, GEAR_DOOR_SLIDE = 0.06, 0.95


def gear_phase(g):
    """The gear's doors' and legs' progress (each 0 to 1) at the gear's g (0 up, 1 down): the doors open over the first
    0.3, the legs run out over the rest; and the doors' rise and slide."""
    o = min(1.0, max(0.0, g / 0.3))
    e = min(1.0, max(0.0, (g - 0.3) / 0.7))
    rise = min(1.0, o / 0.3) * GEAR_DOOR_RISE
    slide = min(1.0, max(0.0, (o - 0.3) / 0.7)) * GEAR_DOOR_SLIDE
    return o, e, rise, slide


SKIN_HOLES = [(5, 2.6, 6.2, 0.3, 0.75), (8, 2.6, 6.2, 0.25, 0.7),          # the roof's side vents
              (3, -13.0, -11.9, 0.22, 0.78), (10, -13.0, -11.9, 0.22, 0.78),   # the flanks' vents behind the side windows
              (3, -21.25, -20.35, 0.27, 0.75), (10, -21.25, -20.35, 0.25, 0.73),  # the nose's attitude thruster ports
              (2, 14.95, 16.4, 0.3, 0.72), (11, 14.95, 16.4, 0.28, 0.7),   # the flare dispensers
              (1, 13.2, 15.8, 0.2, 0.8), (12, 13.2, 15.8, 0.2, 0.8),       # the vents low on the flanks aft
              (0, 0.5, 2.6, 0.0, 0.6 / 4.34), (13, 0.5, 2.6, 1 - 0.6 / 4.34, 1.0),    # the cargo hook's well in the belly
              (0, 3.84, 4.16, 0.0, 0.16 / 4.34), (13, 3.84, 4.16, 1 - 0.16 / 4.34, 1.0)]  # the belly's beacon


# the cheeks' attitude thruster ports' openings in the stern's cut: x from and to, y from and to
CHEEK_HOLES = [(sd * 5.95 - 0.32, sd * 5.95 + 0.32, G + 3.8 - 0.42, G + 3.8 + 0.42) for sd in (-1, 1)]
# the lamps set into the stern's cut (Justin, 8 November 2026: the tail lights): centre x and y, width, height (in the
# cut's plane), lens; their openings join CHEEK_HOLES once the cut's slope is known (below z_cut)
WING_NAV = [(-11.65, 0.72, 0.22), (-10.57, 0.3, 0.18)]       # the wing tip's lamps: z, width (along z), height
WING_FLOOD = (14.4, -12.3, 0.56, 0.3)                       # the floodlight under the tip: |x|, z, width (along x), depth (z)
RAMP_LAMPS = [(sx * 3.75 - 0.18, sx * 3.75 + 0.18, 6.68 - 0.6, 6.68 + 0.6) for sx in (-1, 1)]   # the ramp's tail lights (x, z)
CUT_LAMPS = [(sd * 5.55, G + 5.0, 0.6, 0.24, "lens_red") for sd in (-1, 1)] + [(0.0, G + 7.05, 0.55, 0.2, "lens_white")]


def _clip_half(poly, k, v, keep_lt):
    """Sutherland-Hodgman: the convex polygon poly ((x, y) pairs) cut by x (k=0) or y (k=1) at v, keeping < v or > v."""
    out = []
    n = len(poly)
    for i in range(n):
        a, b = poly[i], poly[(i + 1) % n]
        ina, inb = (a[k] < v) == keep_lt, (b[k] < v) == keep_lt
        if ina:
            out.append(a)
        if ina != inb:
            t = (v - a[k]) / (b[k] - a[k])
            out.append((a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t))
    return out


def cut_convex(poly, rects):
    """The convex polygon poly ((u, v) pairs) with the rectangles (u0, u1, v0, v1) left out: convex pieces."""
    uniq = []
    for q_ in poly:
        if not uniq or np.hypot(q_[0] - uniq[-1][0], q_[1] - uniq[-1][1]) > 1e-9:
            uniq.append(q_)
    if len(uniq) > 1 and np.hypot(uniq[0][0] - uniq[-1][0], uniq[0][1] - uniq[-1][1]) < 1e-9:
        uniq.pop()
    polys = [uniq]
    for (x0, x1, y0, y1) in rects:
        nxt = []
        for pl in polys:
            us, vs = [q_[0] for q_ in pl], [q_[1] for q_ in pl]
            if max(us) <= x0 or min(us) >= x1 or max(vs) <= y0 or min(vs) >= y1:
                nxt.append(pl)
                continue
            mid = _clip_half(_clip_half(pl, 0, x0, False), 0, x1, True)
            for pc in (_clip_half(pl, 0, x0, True), _clip_half(pl, 0, x1, False), _clip_half(mid, 1, y0, True), _clip_half(mid, 1, y1, False)):
                if len(pc) >= 3:
                    nxt.append(pc)
        polys = nxt
    return polys


# The mod's own block and item models, placed in the ship as they are (Justin's playtest, 8 November 2026: "replace the
# ship's fire extinguisher with the existing fire extinguisher model"): any model's elements, read from its JSON, each
# face with its own texture, cropped to the part of it the model uses (a material named for the model and texture).
ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets")
# a face's right and up seen from outside, and its normal (the game's own uv layout: tools/plasma_rifle_nt7.py's check)
MC_FACES = {"north": ((-1, 0, 0), (0, 1, 0), (0, 0, -1)), "south": ((1, 0, 0), (0, 1, 0), (0, 0, 1)),
            "west": ((0, 0, 1), (0, 1, 0), (-1, 0, 0)), "east": ((0, 0, -1), (0, 1, 0), (1, 0, 0)),
            "up": ((1, 0, 0), (0, 0, -1), (0, 1, 0)), "down": ((1, 0, 0), (0, 0, 1), (0, -1, 0))}


def _res(ref, kind):
    ns, _, path = ref.partition(":") if ":" in ref else ("minecraft", None, ref)
    return os.path.join(ASSETS, ns, kind, path)


def _default_uv(face, f, t):
    return {"north": (16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]), "south": (f[0], 16 - t[1], t[0], 16 - f[1]),
            "west": (f[2], 16 - t[1], t[2], 16 - f[1]), "east": (16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]),
            "up": (f[0], f[2], t[0], t[2]), "down": (f[0], 16 - t[2], t[0], 16 - f[2])}[face]


def model_faces(ref, place, tag=None):
    """The quads of the model ref ("jugcraft:block/fire_extinguisher"), each corner p (the model's 0-16 units) put at
    place(p) in the ship. Its textures become materials "mdl_<tag>_<texture>", cropped to what it uses."""
    model = json.load(open(_res(ref, "models") + ".json"))
    tex = dict(model.get("textures", {}))
    def resolve(k):
        for _ in range(8):
            if not k.startswith("#"):
                return k
            k = tex.get(k[1:], "")
        return k
    tag = tag or ref.split("/")[-1]
    faces, used = [], {}
    for e in model["elements"]:
        f, t = np.array(e["from"], float), np.array(e["to"], float)
        c, h = (f + t) / 2, (t - f) / 2
        Rm, org = np.eye(3), np.zeros(3)
        rot = e.get("rotation")
        if rot:
            org = np.array(rot.get("origin", (8, 8, 8)), float)
            if "axis" in rot:
                Rm = rot_m(rot["axis"], math.radians(rot["angle"]))
            else:
                Rm = rot_m("z", math.radians(rot.get("z", 0))) @ rot_m("y", math.radians(rot.get("y", 0))) @ rot_m("x", math.radians(rot.get("x", 0)))
        glow = e.get("light_emission", 0) > 0
        for face, spec in e["faces"].items():
            r, u, n = (np.array(v, float) for v in MC_FACES[face])
            fc, hr, hu = c + n * h, r * h, u * h
            P = np.array([fc - hr + hu, fc + hr + hu, fc + hr - hu, fc - hr - hu])
            P = (P - org) @ Rm.T + org
            uv = spec.get("uv") or _default_uv(face, f, t)
            corners = [(uv[0], uv[1]), (uv[2], uv[1]), (uv[2], uv[3]), (uv[0], uv[3])]
            k = (spec.get("rotation", 0) // 90) % 4
            corners = corners[k:] + corners[:k]
            name = resolve(spec.get("texture", "#missing"))
            used.setdefault(name, []).append(corners)
            Pw = np.array([place(p_) for p_ in P])
            cw = np.asarray(place((P[0] + P[2]) / 2), float)
            nw = np.asarray(place((P[0] + P[2]) / 2 + (np.asarray(n) @ Rm.T)), float) - cw
            faces.append((Pw, corners, name, glow, nw))
    # each texture cropped to what's used of it (the uvs in its 0-16), laid as 0-1 over the crop
    crops = {}
    for name, cs in used.items():
        img = np.asarray(Image.open(_res(name, "textures") + ".png").convert("RGBA"), np.float32) / 255
        H, W = img.shape[:2]
        if H > W and H % W == 0:                           # (an animated strip: its first frame)
            img = img[:W]; H = W
        us = [u_ for c_ in cs for u_, _ in c_]; vs = [v_ for c_ in cs for _, v_ in c_]
        x0, x1 = int(math.floor(min(us) * W / 16)), int(math.ceil(max(us) * W / 16))
        y0, y1 = int(math.floor(min(vs) * H / 16)), int(math.ceil(max(vs) * H / 16))
        x1, y1 = max(x1, x0 + 1), max(y1, y0 + 1)
        mname = "mdl_%s_%s" % (tag, name.split("/")[-1])
        if mname not in _TEX or _TEX[mname].shape != img[y0:y1, x0:x1].shape:     # (one texture however often it's placed)
            _TEX[mname] = img[y0:y1, x0:x1].copy()
        if name.endswith("glass") or img[..., 3].min() < 0.98 and img[..., 3].max() > 0.02 and (img[..., 3] > 0.05).all():
            TRANSLUCENT.add(mname)
        crops[name] = (mname, x0, y0, x1 - x0, y1 - y0, W, H)
    out = []
    for P, corners, name, glow, nw in faces:
        mname, x0, y0, w, hh, W, H = crops[name]
        UV = np.array([((u_ * W / 16 - x0) / w, (v_ * H / 16 - y0) / hh) for u_, v_ in corners])
        out.append(quad(P, UV, _TEX[mname], glow, nw))
    return out


def fan_quads(V):
    """A convex polygon's corners (3D) as a fan of triangles, each a quad with its last corner doubled."""
    out = []
    for i in range(1, len(V) - 1):
        if np.linalg.norm(np.cross(V[i] - V[0], V[i + 1] - V[0])) > 1e-9:
            out.append([V[0], V[i], V[i + 1], V[i + 1]])
    return out


def cut_cheek(f):
    """A face of the stern's cheeks with the openings for what is set into them left out (it and the openings in its cut
    plane, so the pieces are found on x and y and set back on the plane by z_cut)."""
    P = np.asarray(f[0], float)
    if not any(not (P[:, 0].max() <= x0 or P[:, 0].min() >= x1 or P[:, 1].max() <= y0 or P[:, 1].min() >= y1) for (x0, x1, y0, y1) in CHEEK_HOLES):
        return [f]
    ref = face_normal(P)
    out = []
    for pc in cut_convex([(p_[0], p_[1]) for p_ in P], CHEEK_HOLES):
        for Q in fan_quads([np.array([x_, y_, z_cut(y_)]) for x_, y_ in pc]):
            out.append(quad(Q, [(0, 0), (1, 0), (1, 1), (1, 1)], f[2], f[3], ref))
    return out


def cut_quad(P, UV, a0, a1, b0, b1):
    """The quad P (its texture UV) with the opening a0..a1 along P0-P1, b0..b1 along P0-P3 left out: the four pieces
    round it, each (corners, texture)."""
    P = [np.asarray(p_, float) for p_ in P]; U = [np.asarray(u_, float) for u_ in UV]
    def at(a, b, A):
        return A[0] + (A[1] - A[0]) * a + (A[3] - A[0]) * b + (A[2] - A[1] - A[3] + A[0]) * a * b
    out = []
    for (aa, ab, ba, bb) in ((0, 1, 0, b0), (0, 1, b1, 1), (0, a0, b0, b1), (a1, 1, b0, b1)):
        if ab - aa > 1e-6 and bb - ba > 1e-6:
            out.append(([at(aa, ba, P), at(ab, ba, P), at(ab, bb, P), at(aa, bb, P)], [at(aa, ba, U), at(ab, ba, U), at(ab, bb, U), at(aa, bb, U)]))
    return out
GLOWS = GLOWS | {"beam"}

# the hull's stations: z, half-width, belly, roof (the nose tip to the stern)
# The hull's key stations: z, half-width, belly, roof. (Justin, 7 November 2026: "make the parts where it goes from one
# angle to another look better ... right now it looks like multiple parts shoved together"): the nose and the step up to
# the troop box are no longer straight runs with a crease at each station but one smooth curve through them (a monotone
# cubic), sampled about every block; the step up behind the cockpit eases over three blocks instead of 0.6.
KEY_STATIONS = [(-25.0, 1.2, G + 1.8, G + 2.6), (-21.5, 3.8, G + 0.9, G + 4.0), (-18.5, 5.0, G + 0.4, G + 5.0),
                (-16.0, 5.4, G + 0.2, G + 6.2), (-13.8, 5.75, G + 0.05, G + 6.45), (-10.8, 7.0, G, G + 7.6)]
_NOSE_Z = [-25.0, -24.2, -23.35, -22.45, -21.5, -20.5, -19.5, -18.5, -17.25, -16.0, -15.2, -14.4, -13.8, -13.1, -12.35, -11.55, -10.8]


def _pchip(xs, ys, x):
    """Fritsch-Carlson monotone cubic through (xs, ys) at x."""
    n = len(xs)
    h = [xs[i + 1] - xs[i] for i in range(n - 1)]
    d = [(ys[i + 1] - ys[i]) / h[i] for i in range(n - 1)]
    m = [d[0]] + [0.0 if d[i - 1] * d[i] <= 0 else 3 * (h[i - 1] + h[i]) / ((2 * h[i] + h[i - 1]) / d[i - 1] + (h[i] + 2 * h[i - 1]) / d[i])
                  for i in range(1, n - 1)] + [d[-1]]
    for i in range(n - 1):
        if xs[i] <= x <= xs[i + 1]:
            t = (x - xs[i]) / h[i]
            h00, h10, h01, h11 = 2 * t ** 3 - 3 * t ** 2 + 1, t ** 3 - 2 * t ** 2 + t, -2 * t ** 3 + 3 * t ** 2, t ** 3 - t ** 2
            return h00 * ys[i] + h10 * h[i] * m[i] + h01 * ys[i + 1] + h11 * h[i] * m[i + 1]
    return ys[0] if x < xs[0] else ys[-1]


STATIONS = [(z,) + tuple(_pchip([k[0] for k in KEY_STATIONS], [k[c] for k in KEY_STATIONS], z) for c in (1, 2, 3)) for z in _NOSE_Z] + \
           [(-2.4, 7.0, G, G + 7.6), (2.0, 7.0, G, G + 7.6), (13.0, 7.0, G, G + 7.6), (19.9, 7.0, G, G + 7.6)]
PORT = [(0.62, 0.0), (0.92, 0.18), (1.0, 0.46), (0.86, 0.74), (0.5, 0.96), (0.16, 1.0)]
SEG_MAT = {0: "hullD", 13: "hullD", 1: "hull", 12: "hull", 2: "hull", 11: "hull", 3: "hull", 10: "hull",
           4: "hull2", 9: "hull2", 5: "hull2", 8: "hull2", 6: "hull2", 7: "hull2"}
GLASS_Z = (-18.5, -16.0)
# The canopy (Justin, 7 November 2026: "windows ... the pilot area"): glass from the nose's break to the brow, round the
# top and down the shoulders (facets 4-9), so the pilots see ahead; framed, and a real window each side behind it.
CANOPY = ((-21.5, -18.5), (-18.5, -16.0))
CANOPY_SEGS = (4, 5, 6, 7, 8, 9)
SIDE_WIN = (-15.8, -13.4, 0.3, 0.85)        # z0, z1 and t0, t1 up facet 3 (its twin 10 counts t from the top)
FLIGHT_DECK = G + 1.9                       # the flight deck's floor, two steps up from the bay's
LINING = 0.15                               # how far in from the skin the flight deck's lining is
BULK = (-11.25, -11.0)                      # the bulkhead between the flight deck and the bay: its two faces (z)
DOORWAY = (-1.15, 1.15, G + 1.9, G + 4.6)   # the open doorway through it (x, y): the flight deck's floor up
DOOR_SLIDE = 2.32                           # how far its door slides aside into the bulkhead, open
BAY = [(-5.2, G + 1.0), (-5.2, G + 4.4), (-4.2, G + 5.8), (4.2, G + 5.8), (5.2, G + 4.4), (5.2, G + 1.0)]
BAY_Z = (-11.0, 19.9)
BAY_PITCH, BAY_FRAME0 = 1.2, -9.2           # the bay's frames: one every pitch from here, in the seats' gaps
LEAF = 8.0                                  # the ramp's leaf (blocks)
LEAF_RIDE = 0.7                             # how high over the platform's deck the leaf rides stowed and sliding out


def smooth01(u):
    u = min(1.0, max(0.0, u))
    return u * u * (3 - 2 * u)
PODS = [(s * 11.8, G + 4.2, 12.5) for s in (-1, 1)]       # the two VTOL thrusters, at the back
DOOR_Z = (-2.4, 2.0)                                      # the side doors (in the sponsons)
SP_O = dict(x=9.4, zf=-7.2, za=7.0, yb=G + 0.9, yt=G + 4.9)     # a sponson's outer face (port: -x)
SP_I = dict(x=5.25, zf=-10.5, za=10.0, yb=G + 0.6, yt=G + 6.1)  # where it meets the hull
DOOR_Y = (G + 1.3, G + 4.4)
GUN_BED = (-0.7, 0.3)                                      # the door guns' booms' beds (z), down the middle of their bays
GUN_SILL = 9.0                                             # where a gun bay's sill starts (|x|)
GUN_RAIL = 0.36                                            # the bed's rails, either side of its middle (z)
GUN_TRUCKS = (2.66, 1.54)                                  # the booms' trucks, back from the gun (each 0.28 long: in, the
                                                           # inner one rests on the bed's inner stops, out the outer on its outer)
GUN_SWING = 2.65                                           # how far the door guns' booms run out (CrewShip.GUN_SWING)
DOOR_GUN_STOW = 0.94                                       # a door gun's yaw stowed (times its side): down its bay toward the
                                                           # bay's inner forward corner, clear of everything in it


def boom_out(s):
    """A door gun's boom's run out (0 in, 1 out) at the side doors' state s (Justin, 9 November 2026: "work on the side
    gun mounts"; the door slid through the gun as both moved at once, and the gun stowed poked out through the shut
    door): the door opens over the first half, then the boom runs out; going in, the boom comes in first, then the door
    shuts."""
    return min(1.0, max(0.0, (s - 0.5) * 2))


def door_open(s):
    """A side door's opening (0 shut, 1 open) at the side doors' state s (see boom_out)."""
    return min(1.0, max(0.0, s * 2))


def door_gun_yaw(aim, s, sd):
    """A door gun's yaw drawn at the side doors' state s: its aim turned (the short way) to its stowed yaw as its boom
    starts in, done while the gun is still out in the air (a fifth of the way in), so it comes in barrels first down
    its bay."""
    w = min(1.0, max(0.0, (1 - boom_out(s)) / 0.2))
    d = (sd * DOOR_GUN_STOW - aim + math.pi) % (2 * math.pi) - math.pi
    return aim + d * w
# the landing lamps' openings in the chin's sloped front: x either side, half-width, centre and half-height up the slope
CHIN_LAMP = (0.85, 0.28, 0.42, 0.16)
SPONSON_FLOOD_Z = (-6.3, 6.3)                              # the floodlights set into the sponsons' sides, fore and aft of the door
SPONSON_FLOOD_Y = G + 3.95
# the ramp's leaf (in its hinge's frame: along +z, its walking face at y 0) and the stern door (hanging from its hinge)
LEAF_BOXES = [((-4.4, -0.32, 0), (4.4, 0.0, LEAF), "floor"), ((-4.4, -0.62, 0), (4.4, -0.32, LEAF), "hullD"),
              ((-4.4, 0.0, 0), (-4.15, 0.12, LEAF), "hazard"), ((4.15, 0.0, 0), (4.4, 0.12, LEAF), "hazard")]
DOOR_HINGE = (0.0, G + 5.85, 18.18)
DOOR_BOXES = [((-5.25, -4.85, 0.0), (5.25, 0.0, 0.2), "hull2"), ((-5.25, -4.85, 0.2), (5.25, -4.6, 0.24), "hazard"),
              ((-3.1, -3.75, -0.05), (-1.9, -3.25, 0.0), "dark"), ((1.9, -3.75, -0.05), (3.1, -3.25, 0.0), "dark"),
              ((-3.0, -3.7, -0.07), (-2.0, -3.3, -0.05), "lamp"), ((2.0, -3.7, -0.07), (3.0, -3.3, -0.05), "lamp")]
NOSE_GUNS = 2

# The slanted back (Justin, 7 November 2026: "make the back of this thing slanted ... the ramp like goes up ... get rid of
# the ugly looking top thing"): the ramp, hinged at the bay floor's aft edge, folds up to close and is itself the back,
# leaning forward at RAMP_SHUT from the ground; the hull is cut along its outer face (the cheeks round it) and swept up
# under the hinge so the ramp clears it all the way down.
HINGE = np.array([0.0, G + 0.7, 19.0])      # the ramp's hinge line (x across)
BAY_GUN = (0.0, G + 5.8, 12.5)              # the bay gun, hung from the bay's ceiling over the ramp
CEILING_HUNG = [(BAY_GUN[0], BAY_GUN[2], 0.85 * 0.85 + 0.04)]   # what hangs from the bay's ceiling: x, z, half its base
RAMP_L = 7.5                                # from the hinge to its end
RAMP_ROOT = -0.15                           # its root reaches a little forward of the hinge (meets the bay floor)
RAMP_T = 0.3                                # half its thickness: the walking face +RAMP_T over the hinge, the outer -RAMP_T
RAMP_SHUT = math.radians(52.0)              # how far the shut ramp leans back from upright... from the level: 52 degrees
RAMP_W = 5.3                                # its half-width (the opening's 5.2 and a lap over the cheeks)
_cs, _sn = math.cos(RAMP_SHUT), math.sin(RAMP_SHUT)
UP_N = np.array([0.0, _cs, _sn])            # the upper cut's outward normal (up and aft)
UP_P = HINGE + np.array([0.0, RAMP_T * _cs, RAMP_T * _sn]) - UP_N * 0.1     # 0.1 inside the shut ramp's outer face
LO_N = np.array([0.0, -0.6, 1.0]) / math.hypot(0.6, 1.0)                    # the sweep up under the hinge
LO_P = np.array([0.0, G + 0.4, HINGE[2] - 0.5])
CUTS = ((UP_P, UP_N), (LO_P, LO_N))


def cut_lamp_frame(x, y, w, h):
    """A lamp in the stern's cut at (x, y): its centre, the cut's outward normal there, and its across and up."""
    c = np.array([x, y, z_cut(y)])
    up = np.array([0, 1.0, 0]) - UP_N[1] * UP_N
    up /= np.linalg.norm(up)
    return c, UP_N, np.cross(up, UP_N), up


def z_cut(y):
    """How far aft the hull reaches at height y (the nearer of the two cuts)."""
    zu = UP_P[2] - (y - UP_P[1]) * _cs / _sn
    zl = LO_P[2] + 0.6 * (y - LO_P[1])
    return min(zu, zl)


for (_x, _y, _w, _h, _l) in CUT_LAMPS:                       # (their openings, on x and y)
    _c, _n, _rt, _up = cut_lamp_frame(_x, _y, _w, _h)
    _H = [_c + _rt * a * _w / 2 + _up * b * _h / 2 for a in (-1, 1) for b in (-1, 1)]
    CHEEK_HOLES.append((min(p_[0] for p_ in _H), max(p_[0] for p_ in _H), min(p_[1] for p_ in _H), max(p_[1] for p_ in _H)))


def ramp_angle(ramp, reach=None):
    """The ramp's turn about its hinge (radians, about x; + tips its end down): shut at ramp 0 (leaning forward, its end
    up), down on the ground (or reach below the hinge) at 1."""
    drop = HINGE[1] if reach is None else reach
    # the walking face's end on the ground: drop + RAMP_T cos(a) = RAMP_L sin(a)
    a = 0.3
    for _ in range(30):
        a = math.asin(min(1.0, (drop + RAMP_T * math.cos(a)) / RAMP_L))
    shut = -(math.pi - RAMP_SHUT)
    u = smooth01(ramp)
    return shut + (a - shut) * u


def clip_poly(P, UV, cuts=CUTS):
    """A face clipped to the hull's side of the cuts (Sutherland-Hodgman), its texture coordinates carried along."""
    pts = [(np.asarray(p, float), np.asarray(u, float)) for p, u in zip(P, UV)]
    for c, n in cuts:
        out = []
        for i in range(len(pts)):
            a, ua = pts[i]
            b, ub = pts[(i + 1) % len(pts)]
            da, db = (a - c) @ n, (b - c) @ n
            if da <= 0:
                out.append((a, ua))
            if (da <= 0) != (db <= 0):
                t = da / (da - db)
                out.append((a + (b - a) * t, ua + (ub - ua) * t))
        pts = out
        if len(pts) < 3:
            return []
    # drop corners that coincide
    clean = []
    for p, u in pts:
        if not clean or np.linalg.norm(p - clean[-1][0]) > 1e-6:
            clean.append((p, u))
    if len(clean) > 1 and np.linalg.norm(clean[0][0] - clean[-1][0]) < 1e-6:
        clean.pop()
    return clean


def poly_quads(pts):
    """A convex polygon as quads (a triangle as a quad with a repeated corner)."""
    out = []
    n = len(pts)
    if n < 3:
        return out
    k = 1
    while k < n - 1:
        if k + 2 <= n - 1:
            idx = [0, k, k + 1, k + 2]
            k += 2
        else:
            idx = [0, k, k + 1, k + 1]
            k += 1
        out.append(([pts[i][0] for i in idx], [pts[i][1] for i in idx]))
    return out


# The hull's constant part begins here (the troop box: every station from it aft has one section): there the skin's own
# mapping (its girth and its length) is exact, and it anchors the paint of everything else (tools/crew_ship_paint.py).
BODY_FROM = next(z for k, (z, *sec) in enumerate(STATIONS) if all(tuple(s_[1:]) == tuple(sec) for s_ in STATIONS[k:]))
# Lines the panels are not carried across although the skin is smooth there: the keel (the girth's two ends meet under
# the hull), and the ring where the nose leaves the troop box.
PAINT_CUTS = [("x", 0.0, lambda pa, pb, na, nb: na[1] + nb[1] < 0), ("z", BODY_FROM)]


def ring(W, Yb, Yt):
    H = Yt - Yb
    port = [(-a * W, Yb + b * H) for a, b in PORT]
    return [(0.0, Yb)] + port + [(0.0, Yt)] + [(-x, y) for x, y in reversed(port)]


def params_at(z):
    for (z0, *p0), (z1, *p1) in zip(STATIONS, STATIONS[1:]):
        if z0 <= z <= z1:
            f = (z - z0) / (z1 - z0)
            return [a + (b - a) * f for a, b in zip(p0, p1)]
    return list(STATIONS[0][1:] if z < STATIONS[0][0] else STATIONS[-1][1:])


def ring_at(z):
    return ring(*params_at(z))


def roof_y(x, z):
    """The height of the hull's skin over x at z (its upper side)."""
    r = ring_at(z)
    mid = (min(p[1] for p in r) + max(p[1] for p in r)) / 2
    top = sorted(p for p in r if p[1] > mid)
    return float(np.interp(x, [p[0] for p in top], [p[1] for p in top]))


def outward(P, ref_out):
    """P ordered so the renderer sees it from the side ref_out points to (its front: -cross(P1-P0, P2-P0))."""
    n = -np.cross(P[1] - P[0], P[2] - P[0])
    return n @ ref_out >= 0


def quad(P, UV, m, glow, ref_out):
    P = np.asarray(P, float); UV = np.asarray(UV, float)
    if not outward(P, ref_out):
        P, UV = P[::-1].copy(), UV[::-1].copy()
    t = mat(m) if isinstance(m, str) else m
    return (P, UV, t, glow)


def arc(r):
    u = [0.0]
    for a, b in zip(r, r[1:]):
        u.append(u[-1] + math.dist(a, b))
    return u


def ray_poly(poly, c, ang):
    """Where the ray from c at angle ang leaves the closed polygon."""
    d = np.array([math.cos(ang), math.sin(ang)])
    best = None
    for a, b in zip(poly, poly[1:] + poly[:1]):
        a, b = np.array(a), np.array(b)
        e = b - a
        M = np.array([[d[0], -e[0]], [d[1], -e[1]]])
        if abs(np.linalg.det(M)) < 1e-12:
            continue
        t, s = np.linalg.solve(M, a - c)
        if t > 1e-9 and -1e-9 <= s <= 1 + 1e-9 and (best is None or t < best):
            best = t
    return c + d * best


def frame_between(outer, inner, z, c, m, ref_out, tile=2.0):
    """The flat ring between two closed polygons at z (a bulkhead with a hole in it), resampled by angle."""
    angs = sorted({math.atan2(y - c[1], x - c[0]) for x, y in outer + inner} | {-math.pi + k * math.pi / 24 for k in range(48)})
    pts_o = [ray_poly(outer, np.array(c), a) for a in angs]
    pts_i = [ray_poly(inner, np.array(c), a) for a in angs]
    out = []
    n = len(angs)
    for k in range(n):
        a, b = pts_o[k], pts_o[(k + 1) % n]
        ci, d = pts_i[(k + 1) % n], pts_i[k]
        P = [(a[0], a[1], z), (b[0], b[1], z), (ci[0], ci[1], z), (d[0], d[1], z)]
        UV = [(p[0] / tile, -p[1] / tile) for p in P]
        out.append(quad(P, UV, m, False, ref_out))
    return out


def skin_uv(p):
    """The hull skin's texture coordinates at a point on (or near) it: the arc round its cross-section from the belly's
    middle, and z, both in repeats of the hull's texture (as hull() lays them)."""
    r = ring_at(p[2])
    rr_ = r + r[:1]
    ua = arc(rr_)
    best = None
    for j in range(len(r)):
        a, b = np.array(rr_[j]), np.array(rr_[j + 1])
        e = b - a
        t = min(1.0, max(0.0, (np.array(p[:2]) - a) @ e / (e @ e + 1e-12)))
        d = np.linalg.norm(np.array(p[:2]) - (a + e * t))
        if best is None or d < best[0]:
            best = (d, ua[j] + t * (ua[j + 1] - ua[j]))
    TB = TEX_BLOCKS["hull"]
    return np.array([best[1] / TB, p[2] / TB])


def frame_cut(outer, inner, c, m, tile=2.0):
    """frame_between in the stern's cuts: the cheeks round the ramp's opening (and the solid sweep under the floor), each
    corner at the hull's end for its height (z_cut)."""
    angs = sorted({math.atan2(y - c[1], x - c[0]) for x, y in outer + inner} | {-math.pi + k * math.pi / 24 for k in range(48)})
    pts_o = [ray_poly(outer, np.array(c), a) for a in angs]
    pts_i = [ray_poly(inner, np.array(c), a) for a in angs]
    out = []
    n = len(angs)
    for k in range(n):
        a, b = pts_o[k], pts_o[(k + 1) % n]
        ci, d = pts_i[(k + 1) % n], pts_i[k]
        P = [(p[0], p[1], z_cut(p[1])) for p in (a, b, ci, d)]
        my = sum(p[1] for p in P) / 4
        zu = UP_P[2] - (my - UP_P[1]) * _cs / _sn
        zl = LO_P[2] + 0.6 * (my - LO_P[1])
        ref = UP_N if zu <= zl else LO_N
        UV = [(p[0] / tile, -p[1] / tile) for p in P]
        out.append(quad(P, UV, m, False, ref))
    return out


def fan(poly, z, m, ref_out, tile=2.0, glow=False):
    cx = sum(p[0] for p in poly) / len(poly); cy = sum(p[1] for p in poly) / len(poly)
    out = []
    for a, b in zip(poly, poly[1:] + poly[:1]):
        P = [(cx, cy, z), (a[0], a[1], z), (b[0], b[1], z), (b[0], b[1], z)]
        UV = [(p[0] / tile, -p[1] / tile) for p in P]
        out.append(quad(P, UV, m, glow, ref_out))
    return out


class QList(list):
    """The ship's quads, each tagged with the part it belongs to (the ship's current part when it was added)."""

    def __init__(self, ship):
        super().__init__()
        self.ship, self.tags = ship, []

    def append(self, x):
        super().append(x)
        self.tags.append(self.ship.cur)

    def extend(self, xs):
        for x in xs:
            self.append(x)

    def __iadd__(self, xs):
        self.extend(xs)
        return self


class Ship:
    """The ship's quads in its own frame. ramp_out/ramp_down 0..1; tilt: the nacelles, 0 flat (cruise) to 1 straight
    down (hover, landing); thrust 0..1 (the exhaust); gear 0 (up) to 1; door: the stern door, 0 shut, 1 up out of the way;
    entry 0..1 (the re-entry glow round the nose); lights: floodlights on; turret_aim {name: (yaw, pitch)}; firing."""

    def __init__(self, ramp_out=1.0, ramp_down=1.0, tilt=1.0, thrust=0.0, gear=1.0, door=1.0, entry=0.0, lights=True,
                 turret_aim=None, firing=(), ramp_reach=None, side_doors=1.0):
        self.side_doors = side_doors
        self.ramp_out, self.ramp_down, self.tilt, self.thrust, self.gear = ramp_out, ramp_down, tilt, thrust, gear
        self.door, self.entry, self.lights = door, entry, lights
        self.turret_aim = turret_aim or {}
        self.firing = set(firing)
        self.ramp_reach = ramp_reach                 # how far below the deck the leaf's end comes down (default: the ground)
        self.cur = "hull"
        self.q = QList(self)
        self.lamps = []                              # (where, which way) for the beams and the light blocks
        self.build()

    # ---------------------------------------------------------------- pieces
    def B(self, lo, hi, m, glow=None, rot=None, tile=None):
        lo, hi = np.array(lo, float), np.array(hi, float)
        lo, hi = np.minimum(lo, hi), np.maximum(lo, hi)
        if too_small(float(np.max(hi - lo))):
            return
        t = tile if tile is not None else (2.0 if m in ("hull", "hull2", "hullD", "plate") else 1.0)
        fs = box_faces(lo, hi, m, (m in GLOWS) if glow is None else glow, t)
        if rot is not None:
            ax, deg, piv = rot
            Rm = rot_m(ax, math.radians(deg))
            piv = np.array(piv, float)
            fs = [((P - piv) @ Rm.T + piv, UV, tx, g) for P, UV, tx, g in fs]
        self.q += fs

    def facet(self, seg, z0, z1, t0, t1, m, off=0.03, glow=False, uv=None, side=-1):
        """A decal on the hull's facet seg (counted up the port side; side 1: its starboard twin) between z0 and z1 and
        between t0 and t1 across it (from its lower edge), off proud of it; split where the hull creases."""
        cuts = [z for z, *_ in STATIONS if z0 + 1e-6 < z < z1 - 1e-6]
        if cuts:
            zs = [z0] + cuts + [z1]
            if uv is None:
                ts = 2.0 if isinstance(m, str) and m in ("hull", "hull2", "hullD", "plate") else 1.0
                r0 = ring_at(z0)
                h = abs(t1 - t0) * math.dist(r0[13 - seg if side > 0 else seg], r0[(13 - seg + 1) % 14 if side > 0 else seg + 1])
                uv = [(0, 0), ((z1 - z0) / ts, 0), ((z1 - z0) / ts, h / ts), (0, h / ts)]
            U = np.array(uv, float)
            for za, zb in zip(zs, zs[1:]):
                fa, fb = (za - z0) / (z1 - z0), (zb - z0) / (z1 - z0)
                sub = [U[0] + (U[1] - U[0]) * fa, U[0] + (U[1] - U[0]) * fb, U[3] + (U[2] - U[3]) * fb, U[3] + (U[2] - U[3]) * fa]
                self.facet(seg, za, zb, t0, t1, m, off, glow, [tuple(x) for x in sub], side)
            return
        P = []
        if side > 0:
            seg, t0, t1 = 13 - seg, 1 - t0, 1 - t1
        for z, t in ((z0, t0), (z1, t0), (z1, t1), (z0, t1)):
            r = ring_at(z)
            a, b = np.array(r[seg]), np.array(r[(seg + 1) % 14])
            p = a + (b - a) * t
            P.append([p[0], p[1], z])
        P = np.array(P)
        e = P[3] - P[0]
        nrm = np.array([e[1], -e[0], 0.0])
        mid = P.mean(0)
        _, Yb, Yt = params_at(mid[2])
        cen = np.array([0, (Yb + Yt) / 2, mid[2]])
        if nrm @ (mid - cen) < 0:
            nrm = -nrm
        nrm /= np.linalg.norm(nrm) + 1e-9
        P = P + nrm * off
        if uv is None:
            su, sv = abs(z1 - z0), np.linalg.norm(P[3] - P[0])
            ts = 2.0 if isinstance(m, str) and m in ("hull", "hull2", "hullD", "plate") else 1.0
            uv = [(0, 0), (su / ts, 0), (su / ts, sv / ts), (0, sv / ts)]
        self.q.append(quad(P, uv, m, glow, nrm))

    def face(self, P, m, ref, glow=False, ts=None):
        """A quad, UVs from the plane it mostly lies in."""
        P = np.asarray(P, float)
        n = np.abs(np.cross(P[1] - P[0], P[2] - P[0]))
        ax = int(np.argmax(n))
        a, b = [i for i in range(3) if i != ax]
        t = ts if ts is not None else (2.0 if m in ("hull", "hull2", "hullD", "plate") else 1.0)
        UV = [(p[a] / t, -p[b] / t) for p in P]
        self.q.append(quad(P, UV, m, glow, np.asarray(ref, float)))

    def brick(self, C, mats, skip=()):
        """A convex brick from 8 corners (0-3 one end, 4-7 the other, in the same order round): its six faces, each
        with its material (top, bottom, a, b, side 0-1, side 2-3 order: 'f0'..'f5'); skip some."""
        C = [np.asarray(c, float) for c in C]
        cen = np.mean(C, 0)
        F = {"e0": [C[0], C[1], C[2], C[3]], "e1": [C[4], C[5], C[6], C[7]], "s01": [C[0], C[1], C[5], C[4]],
             "s12": [C[1], C[2], C[6], C[5]], "s23": [C[2], C[3], C[7], C[6]], "s30": [C[3], C[0], C[4], C[7]]}
        for k, P in F.items():
            if k in skip:
                continue
            self.face(P, mats.get(k, "hullD") if isinstance(mats, dict) else mats, np.mean(P, 0) - cen)

    def decal(self, P, u0, u1, v0, v1, m, ref, off=0.05, glow=False, uv=None):
        """A patch of face P (corners in order) between u0-u1 along P0->P1 and v0-v1 along P0->P3, off proud."""
        P = [np.asarray(p, float) for p in P]
        def at(u, v):
            return P[0] + (P[1] - P[0]) * u + (P[3] - P[0]) * v + (P[2] - P[1] - P[3] + P[0]) * u * v
        Q = np.array([at(u0, v0), at(u1, v0), at(u1, v1), at(u0, v1)])
        n = np.cross(Q[1] - Q[0], Q[3] - Q[0]); n /= np.linalg.norm(n) + 1e-12
        if n @ np.asarray(ref, float) < 0:
            n = -n
        Q = Q + n * off
        if uv is None:
            self.face(Q, m, n, glow)
        else:
            self.q.append(quad(Q, uv, m, glow, n))

    # ---------------------------------------------------------------- the ship
    def build(self):
        self.hull()
        self.bay()
        self.cur = "hull"
        self.sponsons()
        self.details()
        self.details2()
        self.flight_deck()
        self.cur = "interior"                                # (the bay's fittings: drawn only near, CrewShipRenderer)
        self.bay_kit()
        self.cur = "hull"
        self.outside_kit()
        self.review_kit()
        self.engines()
        self.gear_legs()
        self.lighting()
        self.ramp()
        self.turrets()
        self.unfight()
        self.paint()

    def unfight(self):
        """No two faces left in one plane, one over the other (they flicker: z-fighting. Justin, 10 November 2026, of
        the striped bar under the bay's seats, where the seats' strut feet lay flush in its face; his standing rule):
        whatever is built, tools/crew_ship_planes.py finds such pairs over the whole ship and stands the smaller face
        of each a little off the other."""
        if COLLIDING:
            return
        here = os.path.dirname(os.path.abspath(__file__))
        if here not in sys.path:
            sys.path.insert(0, here)
        import crew_ship_planes
        crew_ship_planes.separate(_HERE, self)

    def paint(self):
        """The outside's paint laid as one skin over everything built so far (tools/crew_ship_paint.py: Justin, 10
        November 2026, "Do a pass over the hull on the ship. There's still issues with the texture"): the troop box's
        skin as it is, every other painted face unfolded from it at its true size, the panels running on wherever two
        faces read as one surface. The far model takes the near model's panels."""
        if COLLIDING:                                       # (the shell for the game's collisions: no paint to lay)
            return
        here = os.path.dirname(os.path.abspath(__file__))
        if here not in sys.path:
            sys.path.insert(0, here)
        import crew_ship_paint
        self.laid = crew_ship_paint.unwrap(_HERE, self, prior=_near_laid() if FAR else None)
        if not FAR:
            _NEAR_LAID[:] = [self.laid]

    def hull(self):
        zs = [s[0] for s in STATIONS]
        rings = [ring(*s[1:]) for s in STATIONS]
        for i in range(len(STATIONS) - 1):
            za, zb = zs[i], zs[i + 1]
            ra, rb = rings[i], rings[i + 1]
            ua, ub = arc(ra + ra[:1]), arc(rb + rb[:1])
            cen = np.array([0, (STATIONS[i][2] + STATIONS[i][3]) / 2, (za + zb) / 2])
            for j in range(14):
                k = (j + 1) % 14
                P = [(ra[j][0], ra[j][1], za), (ra[k][0], ra[k][1], za), (rb[k][0], rb[k][1], zb), (rb[j][0], rb[j][1], zb)]
                m = SEG_MAT[j]
                if za >= DOOR_Z[0] - 1e-6 and zb <= DOOR_Z[1] + 1e-6 and j in (1, 2, 3, 10, 11, 12):
                    if j in (1, 12):
                        # (the belly beside the side door, from its lowest corner to where the sponson's underside
                        # meets it: left open until 6 November 2026, Justin: "I can see into the belly of the ship")
                        lo = np.array(ra[1 if j == 1 else 13]); hi = np.array(ra[2 if j == 1 else 12])
                        f = (SP_I["x"] - abs(lo[0])) / (abs(hi[0]) - abs(lo[0]))
                        mid2 = lo + (hi - lo) * f
                        Q = [(lo[0], lo[1], za), (mid2[0], mid2[1], za), (mid2[0], mid2[1], zb), (lo[0], lo[1], zb)]
                        self.q.append(quad(Q, UV1, "hullD", False, np.mean(np.array(Q), 0) - cen))
                    continue
                glass = za >= CANOPY[0][0] - 1e-6 and zb <= CANOPY[-1][1] + 1e-6 and j in CANOPY_SEGS
                if glass:
                    m = "glass"
                else:                                       # (Justin, 7 November 2026: "they don't all flow together well")
                    m = {"hull": "skin", "hull2": "skin2", "hullD": "skinD"}[m]
                ts = 1.25 if glass else TEX_BLOCKS["hull"]
                UV = [(ua[j] / ts, za / ts), (ua[k if k else 14] / ts, za / ts), (ub[k if k else 14] / ts, zb / ts), (ub[j] / ts, zb / ts)]
                mid = np.mean(np.array(P), 0)
                # the openings cut through it (P0-P1 across the facet, P0-P3 along; an opening may run over a station
                # into the next span, and a span may have several): the pieces round them, found across and along it
                holes = [(hv0, hv1, max(0.0, (hz0 - za) / (zb - za)), min(1.0, (hz1 - za) / (zb - za)))
                         for (hj, hz0, hz1, hv0, hv1) in SKIN_HOLES if hj == j and za < hz1 and zb > hz0]
                cut = bool(holes)
                parent = [np.asarray(p_, float) for p_ in P]
                n0 = len(self.q)
                if cut:
                    Pa, Ua = parent, [np.asarray(u_, float) for u_ in UV]
                    bil = lambda v_, u_, A: A[0] + (A[1] - A[0]) * v_ + (A[3] - A[0]) * u_ + (A[2] - A[1] - A[3] + A[0]) * v_ * u_
                    for pc in cut_convex([(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0)], holes):
                        Pp, Up = [bil(v_, u_, Pa) for v_, u_ in pc], [bil(v_, u_, Ua) for v_, u_ in pc]
                        pts = clip_poly(Pp, Up) if zb > HINGE[2] - 7.0 else list(zip(Pp, Up))
                        for Pq, Uq in poly_quads(pts):
                            self.q.append(quad(Pq, Uq, m, False, mid - cen))
                elif zb > SIDE_WIN[0] + 1e-6 and za < SIDE_WIN[1] - 1e-6 and j in (3, 10):
                    self.q += self.window_cut(P, UV, za, zb, j, m, mid - cen)
                elif zb > HINGE[2] - 7.0:                     # the stern: cut along the shut ramp and swept up under the hinge
                    for Pq, Uq in poly_quads(clip_poly(P, UV)):
                        self.q.append(quad(Pq, Uq, m, False, mid - cen))
                else:
                    self.q.append(quad(P, UV, m, False, mid - cen))
                if cut:
                    for f_ in self.q[n0:]:
                        if not isinstance(f_[0], str):
                            PARENT_OF[face_key(f_[0])] = parent
        # the nose's tip, and the stern: the cheeks round the ramp's opening, in the cuts
        self.q += fan(rings[0], zs[0], "hullD", np.array([0, 0, -1.0]))
        # (their panels laid as one over both cuts' planes, so they carry on across the crease between them: laid on each
        # face's own plane they broke there, Justin's playtest, "textures don't line up, one piece is off")
        n_up = np.asarray(UP_N, float) / np.linalg.norm(UP_N)
        for f_ in frame_cut(rings[-1], BAY, (0.0, G + 3.4), "hull2"):
            for g_ in cut_cheek(f_):
                self.q.append((g_[0], world_uv(np.asarray(g_[0], float), "hull2", n_up), mat("hull2C"), g_[3]))
        # (no rim of bars along the stern's edge any more, Justin, 8 November 2026: a bar laid on the edge, notched at
        # each corner; the skin and the cut meet in a clean edge)

    def bay(self):
        """The troop bay: a lofted room inside the hull (seen through the open stern), the jump seats, the rails.
        Lined as a transport's hold is (Justin, 9 November 2026: "work on the interior of the ship, like the walls and
        floors"): the walls kick panels at their foot and insulation blankets above, one to each bay between the frames
        (every BAY_PITCH, in the seats' gaps); the ceiling perforated panels; the deck plates in a grit coating."""
        z0, z1 = BAY_Z
        r = BAY + BAY[:1]
        u = arc(r)
        cen = np.array([0, G + 3.4, 0])
        pz = lambda z: (z - BAY_FRAME0) / BAY_PITCH
        ytrim = G + 1.85
        for j in range(6):
            a, b = r[j], r[j + 1]
            spans = [(z0, DOOR_Z[0]), (DOOR_Z[1], z1)] if j in (0, 4) else [(z0, z1)]
            for za, zb in spans:
                if j in (0, 4):                             # the walls: kick panels under the trim, blankets over it
                    ylo, yhi = min(a[1], b[1]), max(a[1], b[1])
                    xw = a[0]
                    parts = [(ylo, ytrim, "kick", lambda y: (ytrim - y) / (ytrim - ylo)), (ytrim, yhi, "blanket", lambda y: (yhi - y) / (yhi - ytrim))]
                    for y0_, y1_, m_, vf in parts:
                        P = [(xw, y0_, za), (xw, y1_, za), (xw, y1_, zb), (xw, y0_, zb)]
                        UV = [(pz(za), vf(y0_)), (pz(za), vf(y1_)), (pz(zb), vf(y1_)), (pz(zb), vf(y0_))]
                        ref = np.array([-np.sign(xw), 0, 0])
                        for Pq, Uq in poly_quads(clip_poly(P, UV)):
                            self.q.append(quad(Pq, Uq, m_, False, ref))
                    continue
                P = [(a[0], a[1], za), (b[0], b[1], za), (b[0], b[1], zb), (a[0], a[1], zb)]
                if j == 5:                                  # the deck: four plates across, one bay along to a tile (laid half
                    m_ = "deck"                             # a plate over, so the rings and the tracks sit in plates' middles)
                    UV = [((p_[0] + 5.2 + 0.65) / 5.2, pz(p_[2]) + 0.5) for p_ in P]
                else:                                       # the ceiling and its chamfers
                    m_ = "ceil"
                    UV = [(pz(za), u[j] / BAY_PITCH), (pz(za), u[j + 1] / BAY_PITCH), (pz(zb), u[j + 1] / BAY_PITCH), (pz(zb), u[j] / BAY_PITCH)]
                mid = np.mean(np.array(P), 0)
                ref = cen - np.array([mid[0], mid[1], 0]) * np.array([1, 1, 0])
                for Pq, Uq in poly_quads(clip_poly(P, UV)):
                    self.q.append(quad(Pq, Uq, m_, False, ref))
        # the front wall, round the doorway through to the flight deck (Justin's playtest, 8 November 2026: "can't walk
        # through it (must be able to)"), lined as the side walls are: kick panels under the trim, blankets over it to the
        # walls' height, perforated panels above that; the panels 1.3 across, four each side of the middle
        x0d, x1d, y0d, y1d = DOORWAY
        yw = G + 4.4
        for ya, yb, m_ in ((G + 1.0, ytrim, "kick"), (ytrim, yw, "blanket"), (yw, G + 5.8, "ceil")):
            band = _clip_half(_clip_half(list(BAY), 1, ya, False), 1, yb, True)
            for pc in cut_convex(band, [(x0d, x1d, y0d, y1d)]):
                for Q in fan_quads([np.array([x_, y_, z0]) for x_, y_ in pc]):
                    self.q.append(quad(Q, [((p_[0] + 5.2) / 1.3, (yb - p_[1]) / (yb - ya)) for p_ in Q], m_, False, np.array([0, 0, 1.0])))
        self.seats = 0
        for s in (-1, 1):
            self.cur = "interior"                           # (the seats and rails: drawn only near, CrewShipRenderer)
            for z in np.arange(-10.3, 16.2, 1.2):           # jump seats down the walls (not across the doors)
                if z + 1.0 > DOOR_Z[0] - 0.1 and z < DOOR_Z[1] + 0.1 or z + 1.0 > z_cut(G + 3.6) - 0.2:
                    continue
                self.seats += 1
                self.troop_seat((s * 5.2, z + 0.5), (-s, 0.0), G + 2.02)
            for xa in (2.1, 3.25, 4.4):                     # and three each side of the doorway, facing aft
                self.seats += 1
                self.troop_seat((s * xa, z0), (0.0, 1.0), G + 2.05)
            for za, zb in ((z0, DOOR_Z[0]), (DOOR_Z[1], z1)):
                zr = min(zb, z_cut(G + 5.0) - 0.15)
                self.B((s * 4.6, G + 4.9, za), (s * 4.5, G + 5.0, zr), "metal")                                  # grab rail
                # held off the chamfer on brackets: one near each end, one at every other frame between
                out_ = np.array([s * 1.4, 1.0, 0.0]) / math.hypot(1.4, 1.0)
                c_ = np.array([s * 4.55, G + 4.95, 0.0])
                dch = float(np.dot(np.array([s * 5.2, G + 4.4, 0.0]) - c_, out_))      # the rail's middle to the chamfer
                zs_ = [za + 0.12, zr - 0.12] + [zf_ for zf_ in np.arange(BAY_FRAME0 + BAY_PITCH, zr, 2 * BAY_PITCH) if za + 0.5 < zf_ < zr - 0.5]
                for zq in zs_:
                    p0_ = c_ + np.array([0, 0, zq])
                    self.prism(p0_ + out_ * 0.05, p0_ + out_ * (dch - 0.02), 0.018, "metal", sides=6)
                    self.prism(p0_ + out_ * (dch - 0.025), p0_ + out_ * (dch + 0.01), 0.045, "metal", sides=8)    # its foot
                self.B((s * 5.2, G + 1.0, za), (s * 5.15, G + 1.28, min(zb, z_cut(G + 1.3) - 0.1)), "hazard", tile=0.5)  # kick strip
        # the ceiling's lights: in every other bay between the frames (never across one), one that would meet anything
        # hung from the ceiling moved on to the next bay
        last = -1e9
        for z in np.arange(BAY_FRAME0 + BAY_PITCH / 2, z_cut(G + 5.8) - 0.7, BAY_PITCH):
            if z < last + 2 * BAY_PITCH - 1e-6:
                continue
            if any(abs(z - cz_) < hw_ + 0.26 and abs(cx_) < 1.55 + hw_ for (cx_, cz_, hw_) in CEILING_HUNG):
                continue
            last = z
            self.q += self.fixture((0.0, G + 5.8, z), (0, -1, 0), (0, 0, -1), 3.1, 0.42, "lens_panel", depth=0.06, bezel=0.06, back=0.02, lit=True)
        self.doorway()

    def doorway(self):
        """The doorway through the bulkhead between the bay and the flight deck (Justin's playtest, 8 November 2026: the
        door's frame z-fought, it couldn't be walked through, the light over it looked bad): an opening through the
        bulkhead's thickness lined with metal jambs, a trim round it on both sides, a sliding door (its own part, 'fdoor':
        the game slides it aside into the bulkhead as someone comes near), steps up to it from the bay's floor, and a lit
        FLIGHT DECK sign over it."""
        x0, x1, y0, y1 = DOORWAY
        zf, zb = BULK
        self.cur = "hull"
        # the jambs, head and sill through the bulkhead's thickness
        self.face([(x0, y0, zf), (x0, y0, zb), (x0, y1, zb), (x0, y1, zf)], "metal", (1, 0, 0), ts=1.0)
        self.face([(x1, y0, zf), (x1, y0, zb), (x1, y1, zb), (x1, y1, zf)], "metal", (-1, 0, 0), ts=1.0)
        self.face([(x0, y1, zf), (x1, y1, zf), (x1, y1, zb), (x0, y1, zb)], "metal", (0, -1, 0), ts=1.0)
        self.face([(x0, y0, zf), (x1, y0, zf), (x1, y0, zb), (x0, y0, zb)], "floor", (0, 1, 0))
        # the trim round it on both faces (standing off them: nothing in either face's plane)
        for zz, d in ((zb, 1.0), (zf, -1.0)):
            t, off = 0.12, 0.025
            for P in ([(x0 - t, y0, zz), (x0, y0, zz), (x0, y1, zz), (x0 - t, y1 + t, zz)],
                      [(x1, y0, zz), (x1 + t, y0, zz), (x1 + t, y1 + t, zz), (x1, y1, zz)],
                      [(x0 - t, y1 + t, zz), (x0, y1, zz), (x1, y1, zz), (x1 + t, y1 + t, zz)]):
                Pq = np.array(P, float) + np.array([0, 0, d * off])
                self.face(Pq, "metal", (0, 0, d), ts=1.0)
            for P in ([(x0 - t, y0, zz), (x0 - t, y1 + t, zz)], [(x1 + t, y0, zz), (x1 + t, y1 + t, zz)]):
                a_, b_ = np.array(P[0], float), np.array(P[1], float)
                self.face([a_, b_, b_ + np.array([0, 0, d * off]), a_ + np.array([0, 0, d * off])], "metal", (np.sign(a_[0]), 0, 0), ts=1.0)
            a_, b_ = np.array((x0 - t, y1 + t, zz)), np.array((x1 + t, y1 + t, zz))
            self.face([a_, b_, b_ + np.array([0, 0, d * off]), a_ + np.array([0, 0, d * off])], "metal", (0, 1, 0), ts=1.0)
        # the door: a leaf in the bulkhead's middle, a vision panel in it, a recessed pull each side
        self.cur = "fdoor"
        zm, th = (zf + zb) / 2, 0.035
        wx0, wx1, wy0, wy1 = -0.32, 0.32, G + 3.35, G + 4.25
        for lo, hi in (((x0, y0), (x1, wy0)), ((x0, wy1), (x1, y1)), ((x0, wy0), (wx0, wy1)), ((wx1, wy0), (x1, wy1))):
            self.B((lo[0], lo[1], zm - th), (hi[0], hi[1], zm + th), "hullD")
        for d in (1.0, -1.0):
            self.q.append(quad([(wx0, wy0, zm), (wx1, wy0, zm), (wx1, wy1, zm), (wx0, wy1, zm)], UV1, "glass", False, np.array([0, 0, d])))
            for xp in (0.82,):
                self.B((xp - 0.04, G + 2.9, zm + d * th), (xp + 0.04, G + 3.5, zm + d * (th + 0.02)), "metal")
        self.cur = "interior"
        # the steps up to it from the bay's floor
        for k, (za, zb_, top) in enumerate(((zb, zb + 0.3, y0), (zb + 0.3, zb + 0.6, G + 1.6), (zb + 0.6, zb + 0.9, G + 1.3))):
            self.B((x0 - 0.25, G + 1.0, za), (x1 + 0.25, top, zb_), "floor")
            self.B((x0 - 0.25, top - 0.02, zb_ - 0.06), (x1 + 0.25, top + 0.005, zb_), "hazard", tile=0.5)
        # the sign over it, lit
        self.q += self.fixture((0.0, y1 + 0.38, zb), (0, 0, 1.0), (0, 1.0, 0), 1.2, 0.24, sign("FLIGHT DECK"), depth=0.05, bezel=0.04, back=0.0, lit=True)
        self.cur = "hull"

    def troop_seat(self, wall, f, top, w=0.92):
        """A troop seat on a wall (Justin's playtest, 8 November 2026: "make them look better"): wall (x, z) the wall's
        point behind the seat's middle, f (x, z) the way it faces, top its cushion's top. A padded pan on a tray, hinged at
        the wall on two brackets and held by two struts; a padded back and a headrest on two rails; a four-point harness
        lying over it to its buckle."""
        F = np.array([f[0], 0.0, f[1]]); F /= np.linalg.norm(F)
        A = np.cross(np.array([0, 1.0, 0]), F)
        W0 = np.array([wall[0], 0.0, wall[1]])
        def at(a, d, y):
            return W0 + A * a + F * d + np.array([0, y, 0])
        def box(a0, a1, d0, d1, y0, y1, m, bevel=0.0):
            C = [at(a, d, y) for a, d, y in ((a0, d0, y0), (a1, d0, y0), (a1, d1, y0), (a0, d1, y0), (a0, d0, y1), (a1, d0, y1), (a1, d1, y1), (a0, d1, y1))]
            lo, hi = np.min(C, 0), np.max(C, 0)
            if bevel > 0:
                self.bevel_box(lo, hi, bevel, m)
            else:
                self.B(lo, hi, m)
        hw = w / 2
        box(-hw + 0.03, hw - 0.03, 0.06, 0.64, top - 0.14, top, "seat", 0.045)          # the pan's cushion
        box(-hw + 0.01, hw - 0.01, 0.05, 0.6, top - 0.19, top - 0.14, "dark")            # its tray
        for a in (-hw + 0.06, hw - 0.06):
            box(a - 0.035, a + 0.035, 0.008, 0.11, top - 0.26, top - 0.08, "metal")          # the hinge brackets
            self.prism(at(a, 0.55, top - 0.19), at(a, 0.012, top - 0.78), 0.022, "metal", sides=6)   # the struts
            # their feet on the wall (standing 2 cm proud of the striped kick bar they come through: flush with its
            # face they flickered against it, Justin's picture of the bay, 10 November 2026: "z fighting")
            box(a - 0.04, a + 0.04, 0.008, 0.07, top - 0.84, top - 0.72, "metal")
            self.prism(at(a * 1.03, 0.03, top - 0.3), at(a * 1.03, 0.03, top + 1.42), 0.022, "metal", sides=6)   # the back's rails
        box(-hw + 0.05, hw - 0.05, 0.035, 0.14, top + 0.06, top + 1.0, "seat", 0.035)    # the back
        box(-0.24, 0.24, 0.035, 0.155, top + 1.06, top + 1.36, "seat", 0.045)            # the headrest
        # the harness: two shoulder straps down the back, two lap straps over the pan, the buckle
        for sg in (-1, 1):
            self.face([at(sg * 0.13, 0.145, top + 0.98), at(sg * 0.21, 0.145, top + 0.98), at(sg * 0.17, 0.145, top + 0.1), at(sg * 0.09, 0.145, top + 0.1)],
                      "strap", F, ts=0.5)
            self.face([at(sg * 0.36, 0.12, top + 0.006), at(sg * 0.42, 0.18, top + 0.006), at(sg * 0.1, 0.5, top + 0.006), at(sg * 0.04, 0.44, top + 0.006)],
                      "strap", (0, 1, 0), ts=0.5)
        box(-0.06, 0.06, 0.42, 0.52, top, top + 0.02, "metal")

    def sponsons(self):
        """A big angled block out of each flank (swept front, sloped top) carrying the side door: the door slides forward
        to open, a plasma gun swings out of it; the name on its side, a purple light along its top edge."""
        z0, z1 = DOOR_Z
        y0, y1 = DOOR_Y
        for sd in (-1, 1):
            xo, xi = sd * SP_O["x"], sd * SP_I["x"]
            o = [(xo, SP_O["yt"], SP_O["zf"]), (xo, SP_O["yt"], SP_O["za"]), (xo, SP_O["yb"], SP_O["za"]), (xo, SP_O["yb"], SP_O["zf"])]
            i = [(xi, SP_I["yt"], SP_I["zf"]), (xi, SP_I["yt"], SP_I["za"]), (xi, SP_I["yb"], SP_I["za"]), (xi, SP_I["yb"], SP_I["zf"])]
            ib0 = len(self.q)
            self.brick(o + i, {"s01": "hull", "s12": "hull", "s23": "hullD", "s30": "hull"}, skip=("e0", "e1", "s30"))
            ib1 = len(self.q)
            O, I_ = [np.asarray(p, float) for p in o], [np.asarray(p, float) for p in i]
            nrm = lambda a, b, c: np.cross(b - a, c - a)
            nt = nrm(O[0], O[1], I_[0]); nt = nt if nt[1] > 0 else -nt
            nfr = nrm(O[0], O[3], I_[0]); nfr = nfr if nfr[2] < 0 else -nfr
            nb = nrm(O[3], O[2], I_[3]); nb = nb if nb[1] < 0 else -nb
            na = nrm(O[1], O[2], I_[1]); na = na if na[2] > 0 else -na
            fns = [(nt, self.fillet(O[0], O[1], I_[0], I_[1], nt, r=1.1, m="fillet2", n=14)),
                   (nfr, self.fillet(O[0], O[3], I_[0], I_[3], nfr, r=0.9, m="fillet", n=8)),
                   (nb, self.fillet(O[3], O[2], I_[3], I_[2], nb, r=0.75, m="filletD", n=14)),
                   (na, self.fillet(O[1], O[2], I_[1], I_[2], na, r=0.8, m="filletD", n=8))]
            self.remap(ib0, ib1, fns)
            # its swept front, laid round the vent's opening (the panels carried on from the fairing as on the one face)
            front = [np.asarray(p, float) for p in (o[0], o[3], i[3], i[0])]
            fnf = fns[1][1]
            Uf = [fnf(p_) for p_ in front] if fnf is not None else [(0, 0), (1, 0), (1, 1), (0, 1)]
            for Pq, Uq in cut_quad(front, Uf, 0.3, 0.8, 0.05, 0.27):
                self.q.append(quad(Pq, Uq, "skin" if fnf is not None else "hull", False, nfr))
                PARENT_OF[face_key(Pq)] = front
            out = (sd, 0, 0)
            for (za, zb, ya, yb) in ((SP_O["zf"], SP_O["za"], y1, SP_O["yt"]), (SP_O["zf"], SP_O["za"], SP_O["yb"], y0)):
                self.face([(xo, yb, za), (xo, yb, zb), (xo, ya, zb), (xo, ya, za)], "hull", out)
            # the panels fore and aft of the door, each with a floodlight set into it (Justin, 7 November 2026, on the
            # fared housings: "half the things you changed ... look all fucked up"): a recess in the skin, the lens at
            # its back, nothing standing out
            # (the panel ahead of the door is where the door slides when it opens: its floodlight is on the sponson's
            # swept front instead, flush in the skin over the vent)
            self.face([(xo, y1, SP_O["zf"]), (xo, y1, z0), (xo, y0, z0), (xo, y0, SP_O["zf"])], "hull", out)
            self.recessed_light(xo, sd, z1, SP_O["za"], y0, y1, SPONSON_FLOOD_Z[1], SPONSON_FLOOD_Y, 0.62, 0.4, "lens_flood")
            # the gun's bay, through to the troop bay: its deck flat with the troop bay's either side of the boom's bed,
            # a sill at the door, the head, the two side walls; then all that's in it (gun_bay)
            zb0, zb1 = GUN_BED
            xs = GUN_SILL
            for za_, zb_ in ((z0, zb0), (zb1, z1)):
                self.face([(sd * 5.2, G + 1.0, za_), (sd * 5.2, G + 1.0, zb_), (sd * xs, G + 1.0, zb_), (sd * xs, G + 1.0, za_)], "floor", (0, 1, 0))
                self.face([(sd * xs, y0, za_), (sd * xs, y0, zb_), (xo, y0, zb_), (xo, y0, za_)], "floor", (0, 1, 0))         # the sill
                self.face([(sd * xs, G + 1.0, za_), (sd * xs, G + 1.0, zb_), (sd * xs, y0, zb_), (sd * xs, y0, za_)], "kick", (-sd, 0, 0))
                self.B((sd * (xs + 0.24), y0 - 0.01, za_), (sd * 9.385, y0 + 0.004, zb_), "hazard", tile=0.5)          # its edge
            self.face([(xo, y1, z0), (xo, y1, z1), (sd * 5.2, y1, z1), (sd * 5.2, y1, z0)], "hull2", (0, -1, 0))
            for z, d in ((z0, 1), (z1, -1)):
                self.face([(sd * xs, y1, z), (sd * 5.2, y1, z), (sd * 5.2, G + 1.0, z), (sd * xs, G + 1.0, z)], "hull2", (0, 0, d))
                self.face([(xo, y1, z), (sd * xs, y1, z), (sd * xs, y0, z), (xo, y0, z)], "hull2", (0, 0, d))
            self.gun_bay(sd)
            # the door, slid forward; hazard edges round the opening; its window and handle
            sh = -4.4 * door_open(self.side_doors)
            self.cur = "door_l" if sd < 0 else "door_r"
            self.B((xo, y0 - 0.1, z0 - 0.1 + sh), (xo + sd * 0.12, y1 + 0.1, z1 + 0.1 + sh), "hull2")
            self.B((xo + sd * 0.12, y0 + 1.9, z0 + 0.6 + sh), (xo + sd * 0.14, y1 - 0.4, z1 - 0.6 + sh), "glass")
            self.B((xo + sd * 0.12, y0 + 1.2, z1 - 1.0 + sh), (xo + sd * 0.16, y0 + 1.5, z1 - 0.4 + sh), "dark")
            self.cur = "hull"
            for (za, zb, ya, yb) in ((z0 - 0.3, z0, y0, y1), (z1, z1 + 0.3, y0, y1), (z0 - 0.3, z1 + 0.3, y1, y1 + 0.25)):
                self.B((xo, ya, za), (xo + sd * 0.05, yb, zb), "hazard", tile=0.5)
            # the name aft of the door (clear of the grab handle by the door's aft edge), the light along the top edge, a rib
            self.q.append(quad([(xo + sd * 0.03, G + 3.3, 2.85), (xo + sd * 0.03, G + 3.3, 6.75), (xo + sd * 0.03, G + 2.65, 6.75),
                                (xo + sd * 0.03, G + 2.65, 2.85)],
                               [(0, 0), (1, 0), (1, 1), (0, 1)] if sd < 0 else [(1, 0), (0, 0), (0, 1), (1, 1)], wordmark(), False, np.array(out, float)))
            self.B((xo, SP_O["yt"] - 0.22, SP_O["zf"] + 0.2), (xo + sd * 0.06, SP_O["yt"] - 0.1, SP_O["za"] - 0.2), "glow", glow=True)
            # an armour plate on the sloped top and vents on the swept front
            top = [o[0], o[1], i[1], i[0]]
            front = [np.asarray(p, float) for p in (o[0], o[3], i[3], i[0])]
            fat = lambda u, v: front[0] + (front[1] - front[0]) * u + (front[3] - front[0]) * v + (front[2] - front[1] - front[3] + front[0]) * u * v
            Q = [fat(0.3, 0.05), fat(0.3, 0.27), fat(0.8, 0.27), fat(0.8, 0.05)]      # (clear of the fairing to the flank)
            nq = np.cross(Q[1] - Q[0], Q[3] - Q[0])
            if nq[2] > 0:
                nq = -nq
            self.recessed_vent(Q, nq, 0.15, 6)                     # (set into an opening in the front: above)
            # its floodlight, flush in the skin above the vent
            fc = fat(0.15, 0.16)
            upf = fat(0.05, 0.16) - fat(0.25, 0.16); upf /= np.linalg.norm(upf)
            nqu = nq / np.linalg.norm(nq)
            rtf = np.cross(upf, nqu)
            pw, ph = 0.58 + 0.36, 0.34 + 0.3
            Pm = [fc - rtf * pw / 2 + upf * ph / 2, fc + rtf * pw / 2 + upf * ph / 2, fc + rtf * pw / 2 - upf * ph / 2, fc - rtf * pw / 2 - upf * ph / 2]
            self.q.append(quad([p_ + nqu * 0.012 for p_ in Pm], UV1, mat("lens_mount"), False, nqu))
            self.q += self.fixture(fc + nqu * 0.012, nqu, upf, 0.58, 0.34, "lens_flood", depth=0.06, bezel=0.045, back=0.05)
            self.sponson_front_flood = (fc + nqu * 0.05, nqu)

    def details(self):
        B = self.B
        Yt = G + 7.6
        for sd in (-1, 1):
            # the purple light on the waist crease, behind the sponsons; plates on the shoulders; the cockpit's windows
            # (aft of the thruster pylon's root and short of the slanted stern: it ran under the one and out past the other,
            # Justin's playtest, "the purple part sticks out too far")
            rg = ring_at(16.0)
            ytop = rg[2][1] + (rg[3][1] - rg[2][1]) * 0.98
            self.facet(2, PODS[0][2] + 2.3, z_cut(ytop) - 0.25, 0.90, 0.98, "glow", glow=True, side=sd)
            self.facet(2, -12.0, -10.7, 0.90, 0.98, "glow", glow=True, side=sd)
            self.facet(3, -15.85, -13.35, 0.2, 0.3, "dark", off=0.035, side=sd)
            self.recessed_vent(*self.facet_quad(1, 13.2, 15.8, 0.2, 0.8, sd), 0.18, 6)     # (set into the skin, like the roof's)
            # the troop box's front, stepped up behind the cockpit: an intake each side, flush on the step (a box standing
            # off it until 6 November 2026: "that little striped black box thing ... that's not lined up well")
            self.recessed_vent(*self.facet_quad(3, -13.0, -11.9, 0.22, 0.78, sd), 0.1, 4)     # (set into the skin: SKIN_HOLES)
        # the canopy's frame: bars along every pane's edge, through the skin to the lining
        def out_n(p, z):
            _, Yb, Yt = params_at(z)
            c = np.array([0.0, (Yb + Yt) / 2])
            d = np.array(p) - c
            return np.array([d[0], d[1], 0.0]) / (np.linalg.norm(d) + 1e-9)
        for z in (-21.5, -18.5, -16.0):
            r = ring_at(z)
            for j in CANOPY_SEGS:
                a, b = np.array([*r[j], z]), np.array([*r[j + 1], z])
                self.bar(a - out_n(r[j], z) * 0.06, b - out_n(r[j + 1], z) * 0.06, 0.13, "dark")
        cz = [z for z, *_ in STATIONS if CANOPY[0][0] - 1e-6 <= z <= CANOPY[-1][1] + 1e-6]
        for j in range(4, 11):
            for za, zb in zip(cz, cz[1:]):
                ra, rb = ring_at(za)[j], ring_at(zb)[j]
                self.bar(np.array([*ra, za]) - out_n(ra, za) * 0.06, np.array([*rb, zb]) - out_n(rb, zb) * 0.06, 0.13, "dark")
        for sd in (-1, 1):                                  # the brow: a trim strip along the windscreen's top edge
            for seg in (5, 6):
                self.facet(seg, -16.0, -15.55, 0.0, 1.0, "hullD", off=0.035, side=sd)
        # the roof ridge: the spine, vents, a mast, the turret's plinth, the fin
        # (its feet and its two ends lie on the roof's skin, not under it: Justin's hull pass, 10 November 2026: where it
        # dipped into the roof the paint had no line to carry on from; and in the roof's own paint at the roof's size:
        # as a fairing its panels came out at half the size)
        # (its sides 52 degrees as before, a real corner at its top's edges. For the game's collision shell it keeps its
        # old outline, feet dipped 12 cm under the roof's top: the top turret's dead ground is that shell round it, and
        # the test that only the door gun reaches a target off the port quarter, dropShipDoorGunsSpinUpStowAndPlayersSit,
        # failed with the feet standing on the roof: one column of quarter-block cells fewer beside the turret)
        sp = [(-1.55, 0.0), (-SPINE_TOP, 1.0), (SPINE_TOP, 1.0), (1.55, 0.0)]
        zs_sp = [(-11.9, 0.0), (-11.0, 1.0), (12.6, 1.0), (13.5, 0.0)]

        def sp_at(x, up, f, z):
            if COLLIDING:
                x = (1.15 if up else 1.55) * (1 if x > 0 else -1)
                return (x * (0.7 + 0.3 * f), Yt - 0.12 + 0.52 * up * f, z)
            x = x * (0.7 + 0.3 * f)
            y = roof_y(x, z)
            return (x, y + (Yt + 0.4 - y) * up * f, z)
        # (the paint: the top with its two end slopes is one sheet, started from the roof at its ends; each side another,
        # standing on the roof along its foot; nothing is carried round the ends from a side onto the top, which turned
        # the top's panels askew: PETALS)
        def petal(name):
            PETALS[face_key(self.q[-1][0])] = name
        for (za, fa), (zb, fb) in zip(zs_sp, zs_sp[1:]):
            for (x0, u0), (x1, u1) in zip(sp, sp[1:]):
                P = [sp_at(x0, u0, fa, za), sp_at(x1, u1, fa, za), sp_at(x1, u1, fb, zb), sp_at(x0, u0, fb, zb)]
                sd = 1 if x0 + x1 > 0 else -1
                if u0 == u1:                                # the top
                    if fa == fb == 1.0:                     # (the vent's opening cut through it)
                        for Pq, _ in cut_quad(P, [(0, 0)] * 4, (-1.0 - x0) / (x1 - x0), (1.0 - x0) / (x1 - x0), (-8.6 - za) / (zb - za), (-4.2 - za) / (zb - za)):
                            self.face(Pq, "hull2", (0, 1.0, 0))
                            petal("spine top")
                    else:
                        self.face(P, "hull2", (0, 1.0, 0))
                        petal("spine top")
                    continue
                ref = ((x0 + x1) / 2, 1.0, 0)
                if fa != fb:
                    # a side's end, twisted down into the roof: as its two triangles, the one that lies in the top's end
                    # slope's own plane with the top, the steep one with the side
                    foot, top_ = (0, 1) if u0 == 0 else (1, 0)      # which of the pair is the foot
                    hi, lo_ = ((za, fa), (zb, fb)) if fa == 1.0 else ((zb, fb), (za, fa))
                    xs, us = (x0, x1), (u0, u1)
                    F_h, T_h = sp_at(xs[foot], us[foot], hi[1], hi[0]), sp_at(xs[top_], us[top_], hi[1], hi[0])
                    F_e, T_e = sp_at(xs[foot], us[foot], lo_[1], lo_[0]), sp_at(xs[top_], us[top_], lo_[1], lo_[0])
                    self.face([F_h, T_h, F_e, F_e], "hull2", ref)
                    petal(("spine side", sd))
                    self.face([T_h, T_e, F_e, F_e], "hull2", (0, 1.0, 0))
                    petal("spine top")
                    continue
                self.face(P, "hull2", ref)
                petal(("spine side", sd))
        # the vents (Justin, 7 November 2026: fix them one by one): set down into the skin through openings cut in it, not
        # boxes standing on it
        self.recessed_vent([(-1.0, Yt + 0.4, -4.2), (1.0, Yt + 0.4, -4.2), (1.0, Yt + 0.4, -8.6), (-1.0, Yt + 0.4, -8.6)], (0, 1, 0), 0.24, 14)
        for sd in (-1, 1):
            self.recessed_vent(*self.facet_quad(5, 2.6, 6.2, 0.3, 0.75, sd), 0.2, 6)
        # the top turret's ring housing: a round mound out of the spine, its shoulder rounded, its skirt running down into
        # the roof; its panels lie on as the spine's top's do and fold on over the shoulder and down the skirt
        # (its skirt flares out at its foot to meet the roof almost flat, as if cast with it: it stood out of it at a crease,
        # Justin's playtest, "like a mold, not welded")
        flare = [(h_, 1.631 + 1.35 * math.exp(-h_ / 0.24)) for h_ in (-0.06, 0.0, 0.06, 0.13, 0.21, 0.3, 0.41, 0.54, 0.7, 0.92)]
        self.rod((0.0, Yt - 0.3, -1.0), (0, 1.0, 0), flare + [(0.97, 1.62), (1.02, 1.58), (1.05, 1.54), (1.06, 1.49),
                                                            (1.06, 0.8), (1.06, 0.0)], "skin2", sides=40, uv=("fold", len(flare) + 3, (0.0, -1.0)))
        # the sensor fairing on the roof's ridge behind the windscreen (Justin, 8 November 2026: the last pass): a low
        # streamlined hump moulded on the roof, its panels the roof's own carried over it; its whip antenna on a socket
        self.hump(6, 1.0, -1, [(-13.15, 0.22, 0.0), (-13.6, 0.4, 0.3), (-14.3, 0.44, 0.4), (-15.0, 0.34, 0.28), (-15.45, 0.12, 0.04)], "skin2",
                  uvf=skin_uv)
        sw, nw = self.skin_at(-13.95, 6, 1.0, -1)
        topw = sw + nw * 0.36
        self.rod(topw - nw * 0.05, nw, [(0.0, 0.07), (0.08, 0.07), (0.1, 0.05), (0.1, 0.0)], "metal", sides=12)
        self.rod(topw, nw, [(0.08, 0.025), (1.4, 0.018), (1.42, 0.0)], "boom", sides=8)
        self.fin(Yt + 0.4)
        # the chin: an angled beak with a sensor; belly plates
        ch = [(-1.6, G + 1.1, -20.6), (1.6, G + 1.1, -20.6), (1.6, G - 0.35, -18.6), (-1.6, G - 0.35, -18.6),
              (-1.6, G + 1.1, -16.4), (1.6, G + 1.1, -16.4), (1.6, G - 0.35, -16.4), (-1.6, G - 0.35, -16.4)]
        ib0 = len(self.q)
        self.brick(ch, "hullD", skip=("e0",))
        # its sloped front laid round the landing lamps' two openings (Justin, 7 November 2026: the lamps stood out of
        # it as boxes): a grid of panels with the openings left out, so the lamps sit in the face itself
        b_, a_ = np.array([0.0, G - 0.35, -18.6]), np.array([0.0, G + 1.1, -20.6])
        upl_ = (a_ - b_) / np.linalg.norm(a_ - b_)
        nl_ = np.cross(np.array([1.0, 0, 0]), upl_)
        nl_ = -nl_ if nl_[2] > 0 else nl_
        xs_ = [-1.6, -CHIN_LAMP[0] - CHIN_LAMP[1], -CHIN_LAMP[0] + CHIN_LAMP[1], CHIN_LAMP[0] - CHIN_LAMP[1], CHIN_LAMP[0] + CHIN_LAMP[1], 1.6]
        vs_ = [0.0, CHIN_LAMP[2] - CHIN_LAMP[3], CHIN_LAMP[2] + CHIN_LAMP[3], np.linalg.norm(a_ - b_)]
        at_ = lambda x, v: np.array([x, b_[1] + upl_[1] * v, b_[2] + upl_[2] * v])
        ip0 = len(self.q)
        for j in range(3):
            for i in range(5):
                if j == 1 and i in (1, 3):
                    continue
                self.face([at_(xs_[i], vs_[j]), at_(xs_[i + 1], vs_[j]), at_(xs_[i + 1], vs_[j + 1]), at_(xs_[i], vs_[j + 1])], "hullD", nl_)
        ib1 = len(self.q)
        Cn = [np.asarray(c_, float) for c_ in ch]
        fns = []
        for (oa, ob, ia, ib, nf) in ((Cn[3], Cn[7], Cn[0], Cn[4], (-1, 0, 0)), (Cn[2], Cn[6], Cn[1], Cn[5], (1, 0, 0)),
                                      (Cn[3], Cn[2], Cn[0], Cn[1], (0, -0.5, -1))):
            fns.append((nf, self.fillet(oa, ob, ia, ib, nf, r=0.55, m="filletD", n=8)))
        self.remap(ib0, ib1, fns)
        fnf = fns[2][1]
        if fnf is not None:                       # the front's pieces laid as the one face was: its corners' coordinates
            Vt = np.linalg.norm(a_ - b_)          # spread over it, so the panels run on across the openings unchanged
            U0, U1, U2, U3 = [np.asarray(fnf(c_), float) for c_ in Cn[:4]]
            def bil(p_):
                u_, w_ = (p_[0] + 1.6) / 3.2, (p_ - b_) @ upl_ / Vt
                return (1 - u_) * (1 - w_) * U3 + u_ * (1 - w_) * U2 + u_ * w_ * U1 + (1 - u_) * w_ * U0
            for i in range(ip0, ib1):
                f_ = self.q[i]
                self.q[i] = (f_[0], np.array([bil(p_) for p_ in f_[0]]), f_[2], f_[3])
        self.wings()

    def details2(self):
        """The detail pass (Justin, 7 November 2026: "make things look more a bit like an actual ship instead of like this
        low detail slop"): pitot booms and thruster ports on the nose, the hull number, a satcom dome and blade aerials,
        the side doors' track, grab handle and step, an access hatch, thruster ports and tail lights on the cheeks."""
        B = self.B
        Yt = G + 7.6
        # the nose: one air-data boom from its tip (Justin, 7 November 2026: "The long rod things and the mount they're
        # on still don't look right"): a round fared root in the skin's paint, stepped tube, the sensing head with its
        # two vanes; attitude thruster ports each side
        tip = np.array([0.0, G + 2.2, -25.0])
        ax_ = np.array([0.0, 0.0, -1.0])
        Rb, rt = 0.2, 0.075
        flare = [(0.05 - 0.05 * math.cos(t_), Rb + 0.05 - 0.05 * math.sin(t_)) for t_ in np.linspace(0, math.pi / 2, 5)]
        body = [(0.05 + 0.85 * s_, rt + (Rb - rt) * math.cos(s_ * math.pi / 2) ** 0.85) for s_ in np.linspace(0, 1, 12)[1:]]
        self.rod(tip, ax_, [(-0.04, Rb + 0.05)] + flare + body, "paint", sides=24)
        self.rod(tip, ax_, [(0.9, rt), (0.9, 0.092), (0.97, 0.092), (0.97, rt)], "metal", sides=24)          # the clamp ring
        self.rod(tip, ax_, [(0.97, rt), (2.15, rt), (2.18, 0.062), (2.9, 0.062), (2.93, 0.048), (3.42, 0.048), (3.45, 0.054),
                            (3.6, 0.054), (3.78, 0.028), (3.79, 0.0)], "boom", sides=20)
        for (vx, vy) in ((0.0, 1.0), (1.0, 0.0)):                   # the two vanes (angle of attack, sideslip)
            v = np.array([vx, vy, 0.0])
            c0 = tip + ax_ * 3.1
            for sg in (1, -1):
                nn = sg * (np.cross(v, ax_) if vx else np.array([1.0, 0, 0]))
                self.face([c0 + v * 0.045 + nn * 0.006, c0 + v * 0.045 + ax_ * 0.2 + nn * 0.006, c0 + v * 0.17 + ax_ * 0.16 + nn * 0.006,
                           c0 + v * 0.17 + ax_ * 0.07 + nn * 0.006], "dark", nn, ts=1.0)
        for sx in (-1, 1):
            self.recessed_vent(*self.facet_quad(3, -21.25, -20.35, 0.27, 0.75, sx), 0.1, 3)   # (set into the skin: SKIN_HOLES)
            # the hull number on the nose, under the cockpit
            # (aft of the refuelling probe's housing)
            self.facet(3, -17.45, -15.95, 0.27, 0.68, stencil("NT-07", w=128, h=52, size=30, stroke=1), off=0.1, side=sx,
                       uv=[(0, 1), (1, 1), (1, 0), (0, 0)] if sx < 0 else [(1, 1), (0, 1), (0, 0), (1, 0)])
            # an access hatch low on the flank ahead of the thruster pylon: its frame, panel and handle
            self.facet(2, 7.5, 9.0, 0.22, 0.72, "dark", off=0.03, side=sx)
            self.facet(2, 7.58, 8.92, 0.27, 0.67, "hullD", off=0.045, side=sx)
            self.facet(2, 8.55, 8.75, 0.42, 0.52, "metal", off=0.07, side=sx)
        # blade aerials under the belly (the roof's two are gone: Justin's playtest, "a wing-like piece ... delete both")
        for zb in (-3.2, 9.4):
            self.brick([(-0.04, G - 0.04, zb - 0.4), (0.04, G - 0.04, zb - 0.4), (0.04, G - 0.04, zb + 0.4), (-0.04, G - 0.04, zb + 0.4),
                        (-0.03, G - 0.68, zb + 0.15), (0.03, G - 0.68, zb + 0.15), (0.03, G - 0.68, zb + 0.45), (-0.03, G - 0.68, zb + 0.45)], "dark")
        # the satcom dome on the spine, forward (Justin, 8 November 2026: the last pass): round now, a moulded collar
        # flaring out of the spine's top, the radome on it
        cz = -9.6
        db = np.array([0.0, Yt + 0.4, cz])
        self.rod(db, (0, 1.0, 0), [(-0.03, 0.92), (0.0, 0.92), (0.03, 0.89), (0.07, 0.86), (0.12, 0.85), (0.14, 0.83), (0.14, 0.8)], "paintD", sides=32)
        self.rod(db, (0, 1.0, 0), [(0.14 + 0.42 * math.sin(f_), 0.8 * math.cos(f_)) for f_ in np.linspace(0, math.pi / 2, 9)], "radome", sides=32)
        # the side doors: the track they slide on under the opening, a grab handle aft of it, a step under the sill
        z0, z1 = DOOR_Z
        for sd in (-1, 1):
            xo = sd * SP_O["x"]
            B(*sorted([(xo, G + 1.0, z0 - 4.7), (xo + sd * 0.09, G + 1.12, z1 + 0.3)], key=lambda p: p[0]), "metal")
            B(*sorted([(xo + sd * 0.12, G + 2.0, z1 + 0.55), (xo + sd * 0.17, G + 3.5, z1 + 0.65)], key=lambda p: p[0]), "metal")
            for yh in (G + 2.0, G + 3.45):
                B(*sorted([(xo, yh, z1 + 0.55), (xo + sd * 0.15, yh + 0.05, z1 + 0.65)], key=lambda p: p[0]), "metal")
            B(*sorted([(xo + sd * 0.05, G + 0.5, z0 + 0.6), (xo + sd * 0.55, G + 0.6, z1 - 0.6)], key=lambda p: p[0]), "metal")
            for zh in (z0 + 0.7, z1 - 0.8):
                B(*sorted([(xo, G + 0.6, zh), (xo + sd * 0.06, G + 0.9, zh + 0.1)], key=lambda p: p[0]), "dark")
        # the cheeks: an attitude thruster port each side, a red tail light over it
        for sd in (-1, 1):
            yc, hh, w = G + 3.8, 0.42, 0.32
            xc = sd * 5.95
            P = [np.array([xc + dx, yc + dy, z_cut(yc + dy)]) for dx, dy in ((-w, hh), (w, hh), (w, -hh), (-w, -hh))]
            self.recessed_vent(P, UP_N, 0.1, 4)                     # (set into an opening in the cut: CHEEK_HOLES)
        for (x_, y_, w_, h_, l_) in CUT_LAMPS:                  # the red tail lights and the white one, set into the cut
            c_, n_, rt_, up_ = cut_lamp_frame(x_, y_, w_, h_)
            self.q += self.lamp_in(c_, n_, rt_, up_, w_, h_, l_, depth=0.09)

    def fin(self, y0):
        """The tail fin (Justin, 7 November 2026: "This wing isn't really attached to the aircraft"): rooted along the spine
        forward of the slanted back, its root fared in by fillets each side and a dorsal strake growing out of the spine
        behind the turret; a diamond section, a darker leading edge, the rudder's seam, the beacon on top, a white tail
        light, a purple line down each side."""
        zl0, zt0, zl1, zt1, h = 4.6, 12.4, 10.4, 13.0, 3.3
        t0, t1 = 0.34, 0.2
        def sec(y, zl, zt, t):
            c = zt - zl
            return [(0.0, y, zl), (t, y, zl + 0.3 * c), (t, y, zl + 0.75 * c), (0.05, y, zt), (-0.05, y, zt), (-t, y, zl + 0.75 * c), (-t, y, zl + 0.3 * c)]
        i_fin = len(self.q)
        A, Bs = sec(y0 - 0.02, zl0, zt0, t0), sec(y0 + h, zl1, zt1, t1)
        cen = np.array([0.0, y0 + h / 2, (zl0 + zt1) / 2])
        mats = ["hull"] * 7
        tl0, tl1 = y0 + h - 0.34, y0 + h - 0.1          # the white tail light in the trailing edge, flush (Justin's playtest)
        for k in range(7):
            k2 = (k + 1) % 7
            P = [A[k], A[k2], Bs[k2], Bs[k]]
            ref = np.mean(np.array(P), 0) - cen
            ref[1] = 0
            if k == 3:                                      # the trailing edge's strip: below the light, the light, above it
                at_y = lambda pa, pb, y: np.asarray(pa, float) + (np.asarray(pb, float) - np.asarray(pa, float)) * (y - pa[1]) / (pb[1] - pa[1])
                a0, a1 = at_y(A[3], Bs[3], tl0), at_y(A[4], Bs[4], tl0)
                b0, b1 = at_y(A[3], Bs[3], tl1), at_y(A[4], Bs[4], tl1)
                self.face([A[3], A[4], a1, a0], "hull", ref)
                self.q.append(quad([a0, a1, b1, b0], UV1, "lens_white", True, ref))
                self.face([b0, b1, Bs[4], Bs[3]], "hull", ref)
                continue
            self.face(P, mats[k], ref)
        # the top, round the anti-collision beacon's opening (set into it, not a box on it: Justin's playtest)
        bz, bw, bl = 11.6, 0.18, 0.5
        for pc in cut_convex([(p_[0], p_[2]) for p_ in Bs], [(-bw / 2, bw / 2, bz - bl / 2, bz + bl / 2)]):
            for Q in fan_quads([np.array([x_, y0 + h, z_]) for x_, z_ in pc]):
                self.q.append(quad(Q, UV_UNIT, "hullD", False, np.array([0, 1.0, 0])))
        self.q += self.lamp_in((0.0, y0 + h, bz), (0, 1.0, 0), (1.0, 0, 0), (0, 0, -1.0), bw, bl, "lens_red", depth=0.05, lip=0.02, lit=True)
        # the root's fairing (Justin's hull pass, 10 November 2026, his picture of the fin's foot: "issues with the
        # texture, like here"): one moulded piece each side instead of a fillet and a strake laid through each other.
        # Its foot is the spine's top edge, its top the fin's own section three quarters of a block up, so it stands
        # on the one and closes on the other all round (the old fillets' tops stood off the fin's thin ends, an open
        # slit at the trailing edge, and their feet overhung the spine's sides); ahead it runs out in a ridge to the
        # turret's plinth, astern it closes behind the trailing edge.
        fy = y0 + 0.75
        zlf, ztf, tf = zl0 + (zl1 - zl0) * 0.75 / h, zt0 + (zt1 - zt0) * 0.75 / h, t0 + (t1 - t0) * 0.75 / h
        cf = ztf - zlf
        sz0 = 0.6
        for sd in (-1, 1):
            base = [np.array([sd * x_, y0, z_]) for x_, z_ in ((0.55, sz0), (SPINE_TOP, 5.8), (SPINE_TOP, 11.6), (0.25, 12.6))]
            top = [np.array(p_, float) for p_ in ((0.0, y0 + 0.15, sz0), (0.0, fy, zlf), (sd * tf, fy, zlf + 0.3 * cf),
                                                 (sd * tf, fy, zlf + 0.75 * cf), (sd * 0.05, fy, ztf))]
            # (Justin, 10 November 2026, of the sheet's plain casting: "Panels like the roof". Each of its faces takes the
            # roof ridge's panels over the line it stands on, as a sheet bent up from it would (PETALS: the paint is
            # not carried from one of its faces to the next, which turned the seams a different way on each))
            n0 = len(self.q)
            self.face([base[0], base[1], top[1], top[0]], "hull2", (sd, 0.6, 0))            # the ridge's side
            PETALS[face_key(self.q[-1][0])] = ("fin", sd, "ridge")
            self.face([base[1], top[2], top[1], top[1]], "hull2", (sd, 1, -0.3))            # round the leading edge
            PETALS[face_key(self.q[-1][0])] = ("spine side", sd)                             # (with the ridge's side it runs on from)
            self.face([base[1], base[2], top[3], top[2]], "hull2", (sd, 1, 0))              # along the fin
            PETALS[face_key(self.q[-1][0])] = ("spine side", sd)
            self.face([base[2], base[3], top[4], top[3]], "hull2", (sd, 1, 0.3))            # in to the trailing edge
            PETALS[face_key(self.q[-1][0])] = ("fin", sd, "aft")
        self.face([(-0.25, y0, 12.6), (0.25, y0, 12.6), (0.05, fy, ztf), (-0.05, fy, ztf)], "hull2", (0, 0.3, 1))
        PETALS[face_key(self.q[-1][0])] = ("fin", 0, "end")
        self.face([(-0.55, y0, sz0), (0.55, y0, sz0), (0.0, y0 + 0.15, sz0), (0.0, y0 + 0.15, sz0)], "hullD", (0, 0, -1))
        # the rudder's seam, the purple line, the beacon on top and the tail light at the trailing edge
        for sd in (-1, 1):
            def on_side(f, y, off):
                zl, zt, t = zl0 + (zl1 - zl0) * (y - y0) / h, zt0 + (zt1 - zt0) * (y - y0) / h, t0 + (t1 - t0) * (y - y0) / h
                z = zl + f * (zt - zl)
                return np.array([sd * (t + off), y, z])
            for (f0, f1, ya, yb_, m, gl) in ((0.70, 0.715, y0 + 0.3, y0 + h - 0.15, "dark", False),):
                self.q.append(quad([on_side(f0, ya, 0.02), on_side(f1, ya, 0.02), on_side(f1, yb_, 0.02), on_side(f0, yb_, 0.02)],
                                   UV_UNIT, m, gl, np.array([sd, 0, 0.0])))
            ya, yb_ = y0 + 1.0, y0 + 2.8
            self.q.append(quad([on_side(0.36, ya, 0.025), on_side(0.62, ya, 0.025), on_side(0.62, ya + 0.09, 0.025), on_side(0.36, ya + 0.09, 0.025)],
                               UV_UNIT, "glow", True, np.array([sd, 0, 0.0])))
            self.q.append(quad([on_side(0.42, yb_, 0.025), on_side(0.66, yb_, 0.025), on_side(0.66, yb_ + 0.09, 0.025), on_side(0.42, yb_ + 0.09, 0.025)],
                               UV_UNIT, "glow", True, np.array([sd, 0, 0.0])))
        # its sides lit as the roof they stand on reads (they're upright: the entity light leaves them darker)
        for f_ in self.q[i_fin:]:
            if not isinstance(f_[0], str) and _name_of(f_[2]) in ("hull", "hull2", "hullD", "paint"):
                LIFT[face_key(f_[0])] = 0.4

    def wings(self):
        """The forward wings: thick and broad at the root, cranked down at the outer panel; a twin laser pod under each
        crank (for strafing runs later; idle here)."""
        for sd in (-1, 1):
            secs = [(4.4, G + 3.0, G + 1.4, -17.0, -9.2), (11.6, G + 2.7, G + 1.65, -14.2, -9.8), (16.0, G + 0.4, G - 0.15, -12.4, -10.2)]
            for k, (a, b) in enumerate(zip(secs, secs[1:])):
                C = []
                for (x, yt, yb, zl, zt) in (a, b):
                    C += [(sd * x, yt, zl), (sd * x, yt, zt), (sd * x, yb, zt), (sd * x, yb, zl)]
                ib0 = len(self.q)
                self.brick(C, {"s01": "hull", "s12": "hull", "s23": "hullD", "s30": "hull", "e1": "hull"},
                           skip=("e0",) if k == 0 else ("e0", "e1", "s23"))
                ib1 = len(self.q)
                if k == 0:
                    ib1 = self.clip_outside(ib0, ib1)
                if k == 1:
                    # (Justin, 8 November 2026: the wing tip lights) the tip, round its navigation light's and strobe's
                    # openings, and the underside round the floodlight's: the lamps set into them, nothing standing out
                    Cn = [np.asarray(c_, float) for c_ in C]
                    tip = [Cn[4], Cn[5], Cn[6], Cn[7]]
                    for pc in cut_convex([(p_[2], p_[1]) for p_ in tip], [(z_ - w_ / 2, z_ + w_ / 2, G + 0.14 - h_ / 2, G + 0.14 + h_ / 2)
                                                                       for (z_, w_, h_) in WING_NAV]):
                        for Q in fan_quads([np.array([sd * 16.0, y_, z_]) for z_, y_ in pc]):
                            self.face(Q, "hull", (sd, 0, 0))
                    und = [Cn[2], Cn[3], Cn[7], Cn[6]]
                    nu = np.cross(und[1] - und[0], und[3] - und[0]); nu = nu / np.linalg.norm(nu)
                    nu = -nu if nu[1] > 0 else nu
                    yof = lambda x_, z_: und[0][1] - (nu[0] * (x_ - und[0][0]) + nu[2] * (z_ - und[0][2])) / nu[1]
                    fx, fz, fw, fh = WING_FLOOD
                    for pc in cut_convex([(p_[0] * sd, p_[2]) for p_ in und], [(fx - fw / 2, fx + fw / 2, fz - fh / 2, fz + fh / 2)]):
                        for Q in fan_quads([np.array([sd * x_, yof(sd * x_, z_), z_]) for x_, z_ in pc]):
                            self.face(Q, "hullD", nu)
                    for (z_, w_, h_), lens_ in zip(WING_NAV, ("lens_red" if sd < 0 else "lens_green", "lens_white")):
                        self.q += self.lamp_in((sd * 16.0, G + 0.14, z_), (sd, 0, 0), (0, 0, -sd), (0, 1.0, 0), w_, h_, lens_, depth=0.07, lit=True)
                    cf = np.array([sd * fx, yof(sd * fx, fz), fz])
                    rtu = np.array([sd, 0, 0.0]) - (np.array([sd, 0, 0.0]) @ nu) * nu; rtu /= np.linalg.norm(rtu)
                    self.q += self.lamp_in(cf, nu, rtu, np.cross(nu, rtu), fw, fh, "lens_flood", depth=0.035)     # (shallow: seen from the side)
                top = [C[0], C[4], C[5], C[1]]
                if k == 0:
                    Cn = [np.asarray(c_, float) for c_ in C]
                    fns = [((0, 1, 0), self.fillet(Cn[4], Cn[5], Cn[0], Cn[1], (0, 1, 0), r=1.1, m="fillet2", n=14, hull_r=0.55)),   # (clear of the side window)
                           ((0, -1, 0), self.fillet(Cn[7], Cn[6], Cn[3], Cn[2], (0, -1, 0), r=0.8, m="filletD", n=14)),
                           ((0, 0, -1), self.fillet(Cn[4], Cn[7], Cn[0], Cn[3], (0, 0, -1), r=0.6, m="filletD", n=5)),
                           ((0, 0, 1), self.fillet(Cn[5], Cn[6], Cn[1], Cn[2], (0, 0, 1), r=0.6, m="filletD", n=5))]
                    self.remap(ib0, ib1, fns)
                lead = [C[0], C[4], C[7], C[3]]
                id0 = len(self.q)
                self.decal(lead, 0.0, 1.0, 0.0, 0.35, "dark", (0, 0, -1), off=0.03)
                if k == 0:
                    self.clip_outside(id0, len(self.q))
            # the glow at the crank; under it the ball turret's pylon (Justin, 7 November 2026: "These front guns also don't
            # seem to work": the fixed laser pods were only for show; now a ball turret that turns and fires)
            x = sd * 11.6
            self.B((x - 0.06, G + 2.68, -14.0), (x + 0.06, G + 2.76, -9.9), "glow", glow=True)
            # (Justin, 8 November 2026: the last pass) a streamlined pylon, its section long and narrow, flaring out where
            # it grows from the wing's underside, its foot closed where the turret's plate hangs from it
            self.rod((sd * 11.6, G + 1.115, -12.5), (0, 1.0, 0), [(0.0, 0.0, 1.0, 1.0), (0.0, 0.28, 1.0, 1.9), (0.04, 0.3, 1.0, 1.9), (0.3, 0.33, 1.0, 2.7),
                                                                (0.47, 0.37, 1.0, 3.6), (0.56, 0.43, 1.0, 3.6), (0.62, 0.52, 1.0, 3.3), (0.68, 0.6, 1.0, 3.0)],
                     "paintD", sides=28, e1=(1.0, 0, 0))
            # two flap-track fairings under its trailing edge, static wicks at the tip (the fence on its top is gone:
            # Justin's playtest, "a random rail up top")
            for xc, yb in ((7.0, G + 1.49), (9.9, G + 1.57)):
                self.brick([(sd * xc - 0.2, yb, -11.2), (sd * xc + 0.2, yb, -11.2), (sd * xc + 0.14, yb - 0.32, -10.4), (sd * xc - 0.14, yb - 0.32, -10.4),
                            (sd * xc - 0.2, yb, -8.9), (sd * xc + 0.2, yb, -8.9), (sd * xc + 0.1, yb - 0.18, -8.5), (sd * xc - 0.1, yb - 0.18, -8.5)], "hullD")
            for xw in (15.0, 15.5):
                self.B((sd * xw - 0.025, G + 0.36, -10.25), (sd * xw + 0.025, G + 0.41, -9.55), "dark")
            # NO STEP on the wing root's top
            wy = lambda xx: G + 3.0 - 0.3 * (xx - 4.4) / 7.2 + 0.05
            Pn = [(sd * 6.9, wy(6.9), -13.5), (sd * 8.5, wy(8.5), -13.5), (sd * 8.5, wy(8.5), -12.95), (sd * 6.9, wy(6.9), -12.95)]   # (clear of the fairing)
            self.q.append(quad(Pn, [(0, 1), (1, 1), (1, 0), (0, 0)] if sd > 0 else [(1, 0), (0, 0), (0, 1), (1, 1)],
                               stencil("NO STEP", (226, 200, 70)), False, np.array([0, 1.0, 0])))

    def engines(self):
        """The two VTOL thrusters at the back: an eight-sided ducted fan (a chamfered square) with a heavy intake lip, a
        stepped cowl with armour plates and a glowing band, a waist, a collared nozzle with vanes across the exhaust; on a
        thick pylon with a big pivot hub inboard and a bearing cap outboard; they swing from flat (tilt 0) to straight down."""
        th = math.radians(90.0 * self.tilt)
        Rt = rot_m("x", th)
        rr = lambda r: [(r * x, r * y) for x, y in ((1, 0.72), (0.72, 1), (-0.72, 1), (-1, 0.72), (-1, -0.72), (-0.72, -1), (0.72, -1), (1, -0.72))]
        for (cx, cy, cz) in PODS:
            sd = 1 if cx > 0 else -1
            R0 = 2.3
            # the pylon, the hub and the bearing cap (they don't turn). The pylon: one moulded fairing from inside the hub's
            # drum to inside the hull, its section a long oval, flaring out in a smooth curve where it grows out of the
            # hull (Justin's playtest, 8 November 2026: a box with a fillet down each edge, "pivot fairing pieces jagged"
            # where those met at its corners; "parts should look like a mould")
            hb = np.array([sd * (abs(cx) - 2.25), cy, cz]); ai = np.array([-sd, 0.0, 0.0])
            self.rod(hb, ai, [(1.2, 1.0, 0.85, 1.1), (1.6, 1.0, 0.85, 1.3), (2.0, 1.0, 0.86, 1.46), (2.35, 1.0, 0.9, 1.6), (2.6, 1.0, 0.97, 1.74),
                              (2.8, 1.0, 1.06, 1.9), (3.0, 1.0, 1.16, 2.1), (3.25, 1.0, 1.24, 2.3), (3.6, 1.0, 1.28, 2.45)],
                     "paint", sides=36, e1=(0, 1.0, 0), step=0.1)
            A_top = np.array([[1 / TEX_BLOCKS["hull"], 0], [0, 1 / TEX_BLOCKS["hull"]], [0, 0]])
            # (Justin, 7 November 2026: fix them one by one) the pivot's two ends, round now: inboard a cast drum out of
            # the pylon's end, its end rounded down into the pylon, a dark joint and a bearing ring at the duct; outboard
            # a bearing cap on a bolted flange, its shoulder rounded, a landing lamp set in its middle
            # (the duct's sides at the pivot stand 2.25 out from its axis)
            self.rod(hb, ai, [(-0.25, 1.28), (0.035, 1.28), (0.035, 1.18)], "boom", sides=32)
            self.rod(hb, ai, [(0.035, 1.18), (0.075, 1.18), (0.075, 1.25)], "dark", sides=32)
            self.rod(hb, ai, [(0.075, 1.25), (0.85, 1.25), (1.0, 1.23), (1.2, 1.15), (1.5, 1.0), (1.8, 0.86), (2.1, 0.74)], "skin", sides=32,
                     e1=(0, 1.0, 0), uv=("unroll", A_top, cz, float(np.cross(ai, [0, 1.0, 0])[2])))
            cb = np.array([sd * (abs(cx) + 2.25), cy, cz]); ao = np.array([sd, 0.0, 0.0])
            self.rod(cb, ao, [(-0.3, 1.12), (0.08, 1.12), (0.08, 1.0), (0.26, 1.0), (0.29, 0.99), (0.31, 0.97), (0.32, 0.94), (0.32, 0.66),
                              (0.4, 0.66), (0.42, 0.645), (0.43, 0.62), (0.43, 0.5)], "skin", sides=32,
                     uv=lambda p_, sd=sd, piv_=np.array([cx, cy, cz]): (lambda q_: (-sd * q_[2] / TEX_BLOCKS["hull"], -q_[1] / TEX_BLOCKS["hull"]))(
                         (p_ - piv_) @ rot_m("x", math.pi / 2) + piv_))
            # (the duct's side round it carries its panels as laid at rest, turned with it; the cap takes them on where the
            # duct stands landed, straight down, so there they run straight on from the duct onto the cap)
            for k in range(0 if FAR else 12):              # the flange's bolts, and the face's round the boss
                ang = 2 * math.pi * (k + 0.5) / 12
                bp = cb + np.array([0.0, math.cos(ang), math.sin(ang)]) * 1.06
                self.prism(bp + ao * 0.08, bp + ao * 0.115, 0.032, "metal", sides=6)
                if k % 2 == 0:
                    bp = cb + np.array([0.0, math.cos(ang), math.sin(ang)]) * 0.8
                    self.prism(bp + ao * 0.32, bp + ao * 0.355, 0.032, "metal", sides=6)
            self.rod(cb, ao, [(0.43, 0.5), (0.452, 0.5), (0.452, 0.4)], "metal", sides=32)
            self.rod(cb, ao, [(0.452, 0.4), (0.35, 0.4)], "dark", sides=32)
            self.disc(cb + ao * 0.355, ao, 0.4, "lens_spot", sides=32, glow=self.lights)
            self.pivot_lamp = cb + ao * 0.36
            piv = np.array([cx, cy, cz])
            self.cur = "thruster_l" if sd < 0 else "thruster_r"
            L, a0 = 8.0, 4.0
            def W(p):
                return (np.array([p[0], p[1], p[2] - a0]) @ Rt.T) + piv
            fwd = W((0, 0, -1)) - W((0, 0, 0))
            st = [(0.0, 2.5), (0.6, 2.5), (0.6, 2.25), (5.2, 2.25), (5.2, 2.0), (6.0, 2.0), (6.0, 2.15), (6.5, 2.15), (8.0, 1.8)]
            for (a, r), (b, r2) in zip(st, st[1:]):
                ra, rb = rr(r), rr(r2)
                for k in range(8):
                    k2 = (k + 1) % 8
                    P = [W((ra[k][0], ra[k][1], a)), W((ra[k2][0], ra[k2][1], a)), W((rb[k2][0], rb[k2][1], b)), W((rb[k][0], rb[k][1], b))]
                    if abs(a - b) < 1e-6:                     # a step: faces along the axis
                        ref = -fwd if r2 < r else fwd
                        m = "hullD"
                    else:
                        ref = np.mean(P, 0) - W((0, 0, (a + b) / 2))
                        m = {0: "hullD", 1: "hull", 2: "hull", 3: "hull", 4: "dark", 5: "metal", 6: "hullD", 7: "nozzle"}[st.index((a, r))]
                    UV = [(k * 0.8, a / 2), ((k + 1) * 0.8, a / 2), ((k + 1) * 0.8, b / 2), (k * 0.8, b / 2)]
                    if m in ("hull", "hullD"):                # its panels laid where it stands at rest (they turn with it)
                        Pq = quad(P, UV, m, False, ref)[0]
                        rest = np.array([(np.asarray(pp_) - piv) @ Rt + piv for pp_ in Pq])
                        nr = -np.cross(rest[1] - rest[0], rest[2] - rest[0]); nr /= np.linalg.norm(nr) + 1e-12
                        self.q.append((Pq, world_uv(rest, m, nr), mat(m + "L"), False))
                        continue
                    self.q.append(quad(P, UV, m, False, ref))
            # the glowing band
            for k in range(8):
                k2 = (k + 1) % 8
                r = rr(2.28)
                P = [W((r[k][0], r[k][1], 3.0)), W((r[k2][0], r[k2][1], 3.0)), W((r[k2][0], r[k2][1], 3.35)), W((r[k][0], r[k][1], 3.35))]
                self.q.append(quad(P, UV_UNIT, "glow", True, np.mean(P, 0) - W((0, 0, 3.2))))
            # the intake: a dark ring, the fan's blades and hub; the nozzle: a dark lip, the burning core, vanes across it
            aft = -fwd
            inner = rr(1.95)
            self.q += [quad([W((p[0], p[1], 0.0)) for p in quadp], UV_UNIT, "danger", False, fwd) for quadp in ring_quads(rr(2.5), inner)]
            self.q += [quad([W((x, y, 0.72)) for x, y in tri], UV_UNIT, "rubber", False, fwd) for tri in fan_tris(inner)]
            for k in range(8):                                  # the duct's wall down to the fan
                k2 = (k + 1) % 8
                P = [W((inner[k][0], inner[k][1], -0.01)), W((inner[k2][0], inner[k2][1], -0.01)), W((inner[k2][0], inner[k2][1], 0.74)),
                     W((inner[k][0], inner[k][1], 0.74))]
                self.q.append(quad(P, UV_UNIT, "dark", False, W((0, 0, 0.35)) - np.mean(P, 0)))
            # the fan itself (Justin, 7 November 2026: "... or other things"): fourteen twisted blades round a spinner
            for kb in range(14):
                ab = 2 * math.pi * kb / 14
                pt = lambda r, a, d: W((r * math.cos(a), r * math.sin(a), d))
                Pb = [pt(0.6, ab, 0.32), pt(1.9, ab + 0.12, 0.3), pt(1.9, ab + 0.36, 0.56), pt(0.6, ab + 0.26, 0.58)]
                self.q.append(quad(Pb, UV_UNIT, "metal", False, fwd))
                Pd = [pt(0.6, ab, 0.34), pt(1.9, ab + 0.12, 0.32), pt(1.9, ab + 0.36, 0.58), pt(0.6, ab + 0.26, 0.6)]
                self.q.append(quad(Pd, UV_UNIT, "dark", False, -fwd))
            sp = rr(0.6)
            for k in range(8):
                k2 = (k + 1) % 8
                self.q.append(quad([W((sp[k][0], sp[k][1], 0.34)), W((sp[k2][0], sp[k2][1], 0.34)), W((0.0, 0.0, -0.12)), W((0.0, 0.0, -0.12))],
                                   UV_UNIT, "metal", False, W(((sp[k][0] + sp[k2][0]) / 2, (sp[k][1] + sp[k2][1]) / 2, 0.1)) - W((0, 0, 0.3))))
            core = rr(1.35)
            self.q += [quad([W((p[0], p[1], L)) for p in quadp], UV_UNIT, "nozzle", False, aft) for quadp in ring_quads(rr(1.8), core)]
            for k in range(8):                                  # the nozzle's wall in to the burning core
                k2 = (k + 1) % 8
                P = [W((core[k][0], core[k][1], L - 0.25)), W((core[k2][0], core[k2][1], L - 0.25)), W((core[k2][0], core[k2][1], L + 0.01)),
                     W((core[k][0], core[k][1], L + 0.01))]
                self.q.append(quad(P, UV_UNIT, "dark", False, W((0, 0, L - 0.1)) - np.mean(P, 0)))
            hot = "engine"
            self.q += [quad([W((x, y, L - 0.2)) for x, y in tri], UV_UNIT, hot, True, aft) for tri in fan_tris(core)]
            for vy in (-0.65, 0.0, 0.65):
                P = [W((-1.3, vy - 0.07, L - 0.05)), W((1.3, vy - 0.07, L - 0.05)), W((1.3, vy + 0.07, L - 0.05)), W((-1.3, vy + 0.07, L - 0.05))]
                self.q.append(quad(P, UV_UNIT, "dark", False, aft))
            self.cur = "hull"
            if self.thrust > 0:
                Lx = 1.5 + 6.0 * self.thrust
                for k, (r0, r1) in enumerate(((1.25, 0.45), (0.85, 0.24), (0.45, 0.06))):
                    a, b = L, L + Lx * (1 - 0.25 * k)
                    ra, rb = rr(r0), rr(r1)
                    for j in range(8):
                        j2 = (j + 1) % 8
                        P = [W((ra[j][0], ra[j][1], a)), W((ra[j2][0], ra[j2][1], a)), W((rb[j2][0], rb[j2][1], b)), W((rb[j][0], rb[j][1], b))]
                        self.q.append(("EXH", np.array(P), 0.55 - 0.12 * k))

    def gear_legs(self):
        """The landing gear as it stands at self.gear (see GEAR_AT): each well's walls and roof and the fixed sleeve in it
        (the hull's); the five stages that run out (parts gear_s1 to gear_s5, the footpad with the last); the two doors
        (gear_door_a, the forward ones, sliding forward; gear_door_b aft). The game moves the parts from this, the gear
        down, to where gear_phase puts them."""
        o, e, rise, slide = gear_phase(self.gear)
        hw, dep = GEAR_WELL
        for (x, z) in GEAR_AT:
            self.cur = "hull"
            # the well: its walls and roof, dark; a metal lip round its mouth
            Wc = [np.array(v_) for v_ in ((x - hw, G, z - hw), (x + hw, G, z - hw), (x + hw, G, z + hw), (x - hw, G, z + hw))]
            cw = np.array([x, G, z])
            for k in range(4):
                k2 = (k + 1) % 4
                self.q.append(quad([Wc[k], Wc[k2], Wc[k2] + np.array([0, dep, 0]), Wc[k] + np.array([0, dep, 0])], UV_UNIT, "dark", False,
                                   cw - (Wc[k] + Wc[k2]) / 2))
            self.q.append(quad([p_ + np.array([0, dep, 0]) for p_ in Wc], UV_UNIT, "dark", False, np.array([0, -1.0, 0])))
            # the fixed sleeve from the roof, with its collar
            self.rod((x, G + dep, z), (0, -1.0, 0), [(0.0, GEAR_RADII[0]), (GEAR_STAGE, GEAR_RADII[0]), (GEAR_STAGE, GEAR_RADII[0] + 0.04),
                                                    (GEAR_STAGE + 0.05, GEAR_RADII[0] + 0.04), (GEAR_STAGE + 0.05, GEAR_RADII[1] + 0.005)], "hullD", sides=20)
            # the stages, each from its top (stowed: all up in the sleeve), a collar at its foot; the footpad on the last
            for k in range(1, 6):
                self.cur = "gear_s%d" % k
                top = GEAR_TOP - k * GEAR_RUN * e
                r = GEAR_RADII[k]
                m = "chrome" if k == 5 else "metal" if k == 4 else "hullD"
                prof = [(0.0, r), (GEAR_STAGE, r)]
                if k < 5:
                    prof += [(GEAR_STAGE, r + 0.025), (GEAR_STAGE + 0.04, r + 0.025), (GEAR_STAGE + 0.04, GEAR_RADII[k + 1] + 0.004)]
                self.rod((x, top, z), (0, -1.0, 0), prof, m, sides=20)
                if k == 5:                                  # the ankle and the footpad (its foot on the ground, down)
                    foot = top - GEAR_STAGE
                    self.rod((x, foot, z), (0, -1.0, 0), [(0.0, 0.16), (0.03, 0.2), (0.06, 0.2), (0.08, 0.17)], "metal", sides=16)
                    self.bevel_box((x - 0.72, foot - 0.14, z - 0.72), (x + 0.72, foot - 0.08, z + 0.72), 0.025, "metal")
                    self.B((x - 0.76, foot - 0.2, z - 0.76), (x + 0.76, foot - 0.14, z + 0.76), "rubber")
                    for d_ in (-1, 1):                      # ribs across the pad's top
                        self.B((x - 0.66, foot - 0.08, z + d_ * 0.35 - 0.03), (x + 0.66, foot - 0.05, z + d_ * 0.35 + 0.03), "metal")
            # the doors: the belly's paint outside, dark inside; forward one slides forward, the aft one aft
            for part, z0_, z1_, sg in (("gear_door_a", z - hw, z, -1), ("gear_door_b", z, z + hw, 1)):
                self.cur = part
                dz = sg * slide
                y_ = G - 0.006 + rise
                self.q.append(quad([(x - hw, y_, z0_ + dz), (x + hw, y_, z0_ + dz), (x + hw, y_, z1_ + dz), (x - hw, y_, z1_ + dz)],
                                   [(0, 0), (1, 0), (1, 1), (0, 1)], "hullD", False, np.array([0, -1.0, 0])))
                self.q.append(quad([(x - hw, y_ + 0.04, z0_ + dz), (x + hw, y_ + 0.04, z0_ + dz), (x + hw, y_ + 0.04, z1_ + dz), (x - hw, y_ + 0.04, z1_ + dz)],
                                   UV_UNIT, "dark", False, np.array([0, 1.0, 0])))
                zm_ = z1_ if sg < 0 else z0_                 # the meeting edge: a rubber seal
                self.B((x - hw, y_, zm_ + dz - 0.015), (x + hw, y_ + 0.04, zm_ + dz + 0.015), "rubber")
        self.cur = "hull"

    def lighting(self):
        """Floodlights under the belly (aimed down and out), over the ramp, in the nose and out all round; nav lights.
        Each a proper fitting (Justin, 7 November 2026: "what things you can make look better such as light fixtures"):
        a housing standing out of the hull, turned to where it shines, a metal bezel, its lens set back in it (LEDs in
        their reflectors, a navigation light's Fresnel rings)."""
        L = self.lights
        # (inboard enough to sit wholly on the belly's flat, not hang over the chine's slope beside it)
        for (x, z, ox, oz) in ((-3.2, -6.0, -0.35, -0.2), (3.2, -6.0, 0.35, -0.2), (-3.2, 6.5, -0.35, 0.2), (3.2, 6.5, 0.35, 0.2),
                               (0.0, -14.5, 0.0, -0.45)):
            d = np.array([ox, -1.0, oz])
            yb = params_at(z)[1]                           # flush in the belly (Justin: no blisters stuck on); on the belly
            # its own mounting panel round it, so no seam of the skin's panels runs under the lamp (Justin: "make the
            # textures around the light blend with the light ... there's a line that runs through where it sits")
            pw, ph = 0.95 + 0.6, 0.7 + 0.6
            self.q.append(quad([(x - pw / 2, yb - 0.012, z - ph / 2), (x + pw / 2, yb - 0.012, z - ph / 2), (x + pw / 2, yb - 0.012, z + ph / 2),
                                (x - pw / 2, yb - 0.012, z + ph / 2)], UV1, mat("lens_mountD"), False, np.array([0, -1.0, 0])))
            self.q += self.fixture((x, yb - 0.012, z), (0, -1, 0), (0, 0, -1), 0.95, 0.7, "lens_flood", depth=0.06, bezel=0.06, back=0.04)
            if L:
                self.lamps.append((np.array([x, yb - 0.08, z]), d))
        for s in (-1, 1):                                  # under the bay ceiling's aft end: down the ramp
            zl = z_cut(G + 5.8) - 0.75
            d = np.array([0.0, -1.0, 0.75])
            self.q += self.fixture((s * 2.4, G + 5.8, zl), d, (0, 0, 1), 0.7, 0.42, "lens_flood", depth=0.12, back=0.05, surf=(0, -1, 0))
            if L:
                self.lamps.append((np.array([s * 2.4, G + 5.6, zl + 0.1]), d))
            # the landing lamps, flush in the chin housing's sloped front, each in its own mounting panel (off the
            # curved nose, where a flat lamp could only stand out of it)
            # set into the opening left in the face: its mounting panel round it (a frame lying on the face), a thin
            # metal lip at the mouth, the recess's walls going in, the lens at its back (nothing stands out of the face)
            cn, nl, upl = self.chin_lamp(s)
            rtl = np.cross(upl, nl)
            hw, hh = CHIN_LAMP[1], CHIN_LAMP[3]
            def fr(w0, h0, w1, h1, d, m, uvp=None):
                """A flat frame from the rectangle w0 x h0 out to w1 x h1 (half sizes), d off the face."""
                O = [cn - rtl * w1 + upl * h1, cn + rtl * w1 + upl * h1, cn + rtl * w1 - upl * h1, cn - rtl * w1 - upl * h1]
                I = [cn - rtl * w0 + upl * h0, cn + rtl * w0 + upl * h0, cn + rtl * w0 - upl * h0, cn - rtl * w0 - upl * h0]
                for k in range(4):
                    k2 = (k + 1) % 4
                    P = [O[k] + nl * d, O[k2] + nl * d, I[k2] + nl * d, I[k] + nl * d]
                    if uvp:
                        uv = [((p_ - cn) @ rtl / (2 * w1) + 0.5, 0.5 - (p_ - cn) @ upl / (2 * h1)) for p_ in (O[k], O[k2], I[k2], I[k])]
                        self.q.append(quad(P, uv, mat(m), False, nl))
                    else:
                        self.face(P, m, nl, ts=1.0)
            fr(hw, hh, hw + 0.15, hh + 0.12, 0.008, "lens_mountD", uvp=True)
            fr(hw, hh, hw + 0.035, hh + 0.035, 0.018, "metal")
            Hm = [cn - rtl * hw + upl * hh, cn + rtl * hw + upl * hh, cn + rtl * hw - upl * hh, cn - rtl * hw - upl * hh]
            dep = 0.13
            for k in range(4):
                k2 = (k + 1) % 4
                P = [Hm[k] + nl * 0.018, Hm[k2] + nl * 0.018, Hm[k2] - nl * dep, Hm[k] - nl * dep]
                self.face(P, "dark", cn - (Hm[k] + Hm[k2]) / 2, ts=1.0)
            self.q.append(quad([p_ - nl * (dep - 0.005) for p_ in Hm], UV1, "lens_flood", self.lights, nl))
            self.nose_lamp = cn
        # Floodlights pointing out all round (Justin, 6 November 2026: "when it lands, have lights that go outward, so they
        # point in all directions, so people can see"): the nose lamps ahead; two on each sponson, out and fore and aft;
        # one under each wing tip, out and ahead; one on each thruster pylon's cap, out and astern.
        for s in (-1, 1):
            if L:
                self.lamps.append((self.chin_lamp(s)[0] + np.array([0, -0.05, -0.08]), np.array([s * 0.12, -0.32, -1.0])))
            if L:                                          # the sponson's two: flush in its swept front, set into its side aft
                xo = s * SP_O["x"]
                fp = np.array([xo - s * 0.45, G + 4.4, SP_O["zf"] - 0.6])
                self.lamps.append((fp, np.array([s * 0.82, -0.42, -0.38])))
                self.lamps.append((np.array([xo - s * 0.05, SPONSON_FLOOD_Y, SPONSON_FLOOD_Z[1]]), np.array([s * 0.82, -0.42, 0.38])))
            # under the wing tip (set into its underside: wings())
            d = np.array([s * 0.55, -0.45, -0.7])
            if L:
                self.lamps.append((np.array([s * 14.4, G + 0.42, -12.3]), d))
            # on the thruster pylon's outer cap, low and aft
            # (set in the middle of the pivot's bearing cap: engines())
            d = np.array([s * 0.62, -0.45, 0.64])
            if L:
                self.lamps.append((np.array([s * (abs(PODS[0][0]) + 2.25 + 0.45), PODS[0][1], PODS[0][2]]), d))
        # (the navigation lights at the wing tips, red and green, a white strobe aft: set into the tips, wings())
        # (the tail light atop the cheeks: set into the cut with the red ones, CUT_LAMPS)
        # an anti-collision beacon under the belly
        self.q += self.lamp_in((0.0, G, 4.0), (0, -1, 0), (1.0, 0, 0), (0, 0, -1.0), 0.32, 0.32, "lens_red", depth=0.04, lit=True)   # (set in)

    def window_cut(self, P, UV, za, zb, j, m, ref, lining=False):
        """A hull face (or the lining's) with the side window through the part of it the window spans: the face round
        the window, the glass, the frame's bars along the window's edges."""
        z0, z1, t0, t1 = SIDE_WIN
        if j == 10:
            t0, t1 = 1 - t1, 1 - t0
        # (the hull's faces run across the facet first, then along: reordered so u runs along the ship, v up the facet)
        P = [np.asarray(P[i], float) for i in (0, 3, 2, 1)]; U = [np.asarray(UV[i], float) for i in (0, 3, 2, 1)]
        def at(u, v, A):
            return A[0] + (A[1] - A[0]) * u + (A[3] - A[0]) * v + (A[2] - A[1] - A[3] + A[0]) * u * v
        u0, u1 = max(0.0, (z0 - za) / (zb - za)), min(1.0, (z1 - za) / (zb - za))
        out = []
        parts = [(u0, u1, 0, t0), (u0, u1, t1, 1)]
        if u0 > 1e-6:
            parts.append((0, u0, 0, 1))
        if u1 < 1 - 1e-6:
            parts.append((u1, 1, 0, 1))
        for (ua_, ub_, va, vb) in parts:
            Q = [at(ua_, va, P), at(ub_, va, P), at(ub_, vb, P), at(ua_, vb, P)]
            Qu = [at(ua_, va, U), at(ub_, va, U), at(ub_, vb, U), at(ua_, vb, U)]
            out.append(quad(Q, Qu, m, False, ref))
        if lining:
            return out
        W_ = [at(u0, t0, P), at(u1, t0, P), at(u1, t1, P), at(u0, t1, P)]
        out.append(quad(W_, [(u0, 0), (u1, 0), (u1, 1), (u0, 1)], "glass", False, ref))
        nrm = np.asarray(ref, float); nrm[2] = 0; nrm /= np.linalg.norm(nrm)
        edges = [(W_[0], W_[1]), (W_[3], W_[2])]
        if u0 > 1e-6:
            edges.append((W_[0], W_[3]))
        if u1 < 1 - 1e-6:
            edges.append((W_[1], W_[2]))
        for a, b in edges:
            self.bar(a - nrm * 0.06, b - nrm * 0.06, 0.13, "dark")
        return out

    def jump_seat(self, s, z):
        """A jump seat on the bay's wall (Justin, 7 November 2026: "detail inside the hull"): a cushion on a front tube,
        webbing stretched between two posts for its back, the harness's two straps and buckle."""
        x0, x1 = sorted((s * 5.17, s * 4.6))
        self.B((x0, G + 1.88, z + 0.04), (x1, G + 2.02, z + 0.96), "fabric")
        self.prism((s * 4.6, G + 1.86, z), (s * 4.6, G + 1.86, z + 1.0), 0.04, "metal", sides=4, caps=False)
        for zp in (z + 0.05, z + 0.95):
            self.prism((s * 5.1, G + 1.86, zp), (s * 5.1, G + 3.5, zp), 0.035, "metal", sides=4, caps=False)
        self.face([(s * 5.12, G + 2.05, z + 0.08), (s * 5.12, G + 2.05, z + 0.92), (s * 5.12, G + 3.45, z + 0.92), (s * 5.12, G + 3.45, z + 0.08)],
                  "net", (-s, 0, 0), ts=1.0)
        for zs_ in (z + 0.3, z + 0.6):
            self.face([(s * 5.05, G + 3.42, zs_), (s * 5.05, G + 3.42, zs_ + 0.1), (s * 4.75, G + 2.04, zs_ + 0.1), (s * 4.75, G + 2.04, zs_)],
                      "strap", (-s, 0.3, 0), ts=0.5)
        self.face([(s * 4.62, G + 2.03, z + 0.42), (s * 4.62, G + 2.03, z + 0.58), (s * 4.75, G + 2.03, z + 0.58), (s * 4.75, G + 2.03, z + 0.42)],
                  "metal", (0, 1, 0), ts=1.0)

    def bay_kit(self):
        """The bay fitted out (Justin, 7 November 2026: "detail inside the hull"): frames round it every 2.4 blocks
        between the seats, conduits and static lines along the ceiling, tie-down rings in the floor, LED strips along the
        floor's edges, the jump lights by the ramp, EXIT signs over the side doors, a rifle rack, a first aid kit and an
        extinguisher by the flight deck's door. (All of it with the interior's fittings, drawn only near: "optimize what you
        can", 9 November 2026.)"""
        self.cur = "interior"
        zc = z_cut(G + 5.8) - 0.4
        inset = 0.07
        for z in np.arange(BAY_FRAME0, zc, BAY_PITCH):
            for k in range(5):
                (xa, ya), (xb, yb) = BAY[k], BAY[k + 1]
                if k in (0, 4) and DOOR_Z[0] - 0.2 < z < DOOR_Z[1] + 0.2:
                    continue
                if z_cut(max(ya, yb)) < z + 0.1:
                    continue
                pa = np.array([xa - np.sign(xa) * inset, ya - (inset if ya > G + 5 else 0), z])
                pb = np.array([xb - np.sign(xb) * inset, yb - (inset if yb > G + 5 else 0), z])
                # a frame stops at anything hung from the ceiling across its way (the bay gun's base: it ran through it,
                # Justin's playtest, "the bar should stop at it")
                cuts = sorted((cx_ - hw_, cx_ + hw_) for (cx_, cz_, hw_) in CEILING_HUNG if k == 2 and abs(z - cz_) < hw_ + 0.06)
                xs_ = [pa[0]]
                for c0_, c1_ in cuts:
                    xs_ += [c0_, c1_]
                xs_.append(pb[0])
                for x0_, x1_ in zip(xs_[0::2], xs_[1::2]):
                    if x1_ - x0_ > 0.05:
                        self.bar(np.array([x0_, pa[1], z]), np.array([x1_, pb[1], z]), 0.12, "hullD")
        for sx in (-1, 1):
            self.prism((sx * 4.25, G + 5.55, -10.95), (sx * 4.25, G + 5.55, z_cut(G + 5.55) - 0.2), 0.08, "metal", caps=False)
            self.prism((sx * 4.55, G + 5.25, -10.95), (sx * 4.55, G + 5.25, z_cut(G + 5.25) - 0.2), 0.05, "dark", caps=False)
            self.prism((sx * 2.4, G + 5.62, -10.95), (sx * 2.4, G + 5.62, zc), 0.025, "metal", sides=4, caps=False)
            # the floor's edge lights
            za, zb = -10.6, z_cut(G + 1.0) - 0.3
            self.q.append(quad([(sx * 5.12, G + 1.015, za), (sx * 5.12, G + 1.015, zb), (sx * 4.98, G + 1.015, zb), (sx * 4.98, G + 1.015, za)],
                               [(za / 0.25, 0), (zb / 0.25, 0), (zb / 0.25, 1), (za / 0.25, 1)], mat("led_strip"), True, np.array([0, 1.0, 0])))
            # the jump lights, red and green, on the walls by the ramp (between two of the frames: one ran through the
            # red one until Justin's 8 November 2026 playtest, "stuck inside the posts")
            for zl, lens_ in ((15.3, "lens_red"), (15.8, "lens_green")):
                self.q += self.fixture((sx * 5.2, G + 4.0, zl), (-sx, 0, 0), (0, 1, 0), 0.3, 0.3, lens_, depth=0.08, back=0.02, lit=True)
            # EXIT over the side door, on the ceiling's chamfer
            ch = np.array([-sx * 1.0, 1.4, 0.0]); ch /= np.linalg.norm(ch)
            nin = np.array([-sx * 1.4, -1.0, 0.0]); nin /= np.linalg.norm(nin)
            self.q += self.fixture(np.array([sx * 4.9, G + 4.82, -0.2]), nin, ch, 0.9, 0.3, "lens_exit", depth=0.05, back=0.02, lit=True)
        # tie-down rings in the deck, in the plates' middles: each in its cup, a D-ring lying flat
        for x in (-3.9, -1.3, 1.3, 3.9):
            for z in np.arange(BAY_FRAME0, z_cut(G + 1.0) - 1.2, BAY_PITCH):
                self.disc((x, G + 1.003, z), (0, 1.0, 0), 0.1, "dark", sides=12)
                self.rod((x, G + 1.0, z), (0, 1.0, 0), [(0.0, 0.1), (0.012, 0.1), (0.012, 0.085), (0.0, 0.085)], "metal", sides=12)
                self.B((x - 0.06, G + 1.004, z - 0.015), (x + 0.06, G + 1.022, z + 0.015), "metal")
        # two roller tracks down the deck for cargo: a channel, its rails, rollers across it
        zt0, zt1 = -9.95, z_cut(G + 1.0) - 0.7
        for xc in (-2.6, 2.6):
            self.q.append(quad([(xc - 0.16, G + 1.003, zt0), (xc + 0.16, G + 1.003, zt0), (xc + 0.16, G + 1.003, zt1), (xc - 0.16, G + 1.003, zt1)],
                               UV_UNIT, "dark", False, np.array([0, 1.0, 0])))
            for sx in (-1, 1):
                self.B((xc + sx * 0.16 - 0.02, G + 1.0, zt0), (xc + sx * 0.16 + 0.02, G + 1.07, zt1), "metal")
            for z in np.arange(zt0 + 0.2, zt1 - 0.1, 0.35):
                self.prism((xc - 0.14, G + 1.045, z), (xc + 0.14, G + 1.045, z), 0.035, "chrome", sides=8, caps=False)
        # the walls' trim along the kick panels' tops, and a light strip where each wall meets the ceiling's chamfer
        for sx in (-1, 1):
            for za, zb in ((BAY_Z[0], DOOR_Z[0]), (DOOR_Z[1], z_cut(G + 1.9) - 0.1)):
                self.B((sx * 5.2, G + 1.83, za), (sx * 5.165, G + 1.88, zb), "metal")
            # and across the front wall, from the side walls' trim to the steps up to the doorway
            self.B((sx * 5.165, G + 1.83, BAY_Z[0]), (sx * (DOORWAY[1] + 0.27), G + 1.88, BAY_Z[0] + 0.035), "metal")
            za, zb = -10.9, z_cut(G + 4.4) - 0.25
            d_ = np.array([-sx * 1.0, -1.4, 0.0]); d_ /= np.linalg.norm(d_)
            ch = np.array([-sx * 1.0, 1.4, 0.0]); ch /= np.linalg.norm(ch)
            c0 = np.array([sx * 5.2, G + 4.4, 0.0]) + ch * 0.08
            for za_, zb_ in ((za, DOOR_Z[0] - 0.1), (DOOR_Z[1] + 0.1, zb)):
                Pq = [c0 + ch * 0.06 + d_ * 0.01 + np.array([0, 0, za_]), c0 + ch * 0.06 + d_ * 0.01 + np.array([0, 0, zb_]),
                      c0 - ch * 0.06 + d_ * 0.01 + np.array([0, 0, zb_]), c0 - ch * 0.06 + d_ * 0.01 + np.array([0, 0, za_])]
                self.q.append(quad(Pq, [(za_ / 0.25, 0), (zb_ / 0.25, 0), (zb_ / 0.25, 1), (za_ / 0.25, 1)], mat("led_strip"), True, d_))
        # On the front wall over the front seats (Justin's playtest, 8 November 2026: "the guns mounted up there need to
        # look better", "med kit in the wall looks bad", "replace the ship's fire extinguisher with the existing fire
        # extinguisher model"): the rifle rack with three of the NT-7 itself (the item's own model), and the mod's own
        # first aid box and fire extinguisher cabinet.
        zw = BAY_Z[0]
        self.rifle_rack(-3.74, G + 3.6, zw)
        self.q += model_faces("jugcraft:block/first_aid_box",
                              lambda p_: (3.62 - (p_[0] - 8) / 16, G + 3.72 + (p_[1] - 3) / 16, zw + 0.004 + (16 - p_[2]) / 16), "first_aid")
        self.q += model_faces("jugcraft:block/fire_extinguisher",
                              lambda p_: (2.1 + (p_[0] - 8) / 16, G + 3.6 + (p_[1] - 0.5) / 16, zw + 0.004 + p_[2] / 16), "extinguisher")

    def rifle_rack(self, xc, y0, zw):
        """A rifle rack on the wall at zw (xc its middle, y0 its foot): a padded board in a frame, three NT-7s (the item's
        own model, its muzzle toward the doorway) each resting on two padded pegs, a locking bar down through their trigger
        guards with its lock at the foot."""
        w, h = 1.5, 1.62
        x0, x1 = xc - w / 2, xc + w / 2
        self.bevel_box((x0, y0, zw + 0.004), (x1, y0 + h, zw + 0.06), 0.02, "dark")                  # the frame
        self.B((x0 + 0.06, y0 + 0.06, zw + 0.06), (x1 - 0.06, y0 + h - 0.06, zw + 0.064), "rubber")  # the board's pad
        L = (18.9 - 0.28) / 16
        rx0 = xc - L / 2
        zb = zw + 0.072
        for k in range(3):
            yb = y0 + 0.16 + k * 0.5                       # the rifle's lowest point (its grip's foot)
            self.q += model_faces("jugcraft:item/plasma_rifle",
                                  lambda p_, yb=yb: (rx0 + (p_[0] - 0.28) / 16, yb + (p_[1] - 5.34) / 16, zb + (p_[2] - 6.88) / 16), "nt7")
            for mx, my in ((2.0, 7.15), (15.5, 8.0)):       # the pegs under the stock's floor and the spine
                px, py = rx0 + (mx - 0.28) / 16, yb + (my - 5.34) / 16
                self.B((px - 0.05, py - 0.07, zw + 0.064), (px + 0.05, py - 0.005, zb + 0.16), "metal")
                self.B((px - 0.055, py - 0.005, zb - 0.005), (px + 0.055, py, zb + 0.155), "rubber")
        bx = rx0 + (10.0 - 0.28) / 16
        self.B((bx - 0.025, y0 + 0.08, zb + 0.16), (bx + 0.025, y0 + h - 0.12, zb + 0.2), "metal")      # the locking bar
        for yy in (y0 + 0.1, y0 + h - 0.14):
            self.B((bx - 0.04, yy - 0.03, zw + 0.064), (bx + 0.04, yy + 0.03, zb + 0.2), "metal")
        self.bevel_box((bx - 0.07, y0 + 0.02, zb + 0.13), (bx + 0.07, y0 + 0.13, zb + 0.24), 0.015, "paint_red")   # its lock

    def outside_kit(self):
        """More outside (Justin, 7 November 2026: "or other areas I missed on the outside"): a sensor ball under the
        chin, fairings where the wings meet the hull, actuator fairings and hydraulic lines on the thruster pylons, a
        bolted ring under the top turret, a RESCUE marking under each side window."""
        self.cur = "hull"
        # the chin's sensor ball (Justin, 8 November 2026: the last pass): a round collar flaring out of the chin's
        # underside, the ball (a sphere, its front flat for the window), the window in a dark ring
        self.rod((0.0, G - 0.35, -17.6), (0, -1.0, 0), [(-0.03, 0.56), (0.0, 0.56), (0.03, 0.51), (0.07, 0.46), (0.12, 0.43), (0.28, 0.38), (0.32, 0.34),
                                                     (0.34, 0.28), (0.34, 0.0)], "paintD", sides=28)
        cb_ = np.array([0.0, G - 0.98, -17.6])
        axb = np.array([0.0, -0.25, -1.0]); axb /= np.linalg.norm(axb)
        Rb_ = 0.4
        prof = [(a_, math.sqrt(max(Rb_ * Rb_ - a_ * a_, 0.0))) for a_ in np.linspace(-Rb_, 0.3, 14)]
        self.rod(cb_, axb, prof + [(0.3, 0.2)], "paint", sides=28, e1=(1.0, 0, 0))
        self.rod(cb_, axb, [(0.3, 0.2), (0.3, 0.17)], "dark", sides=28, e1=(1.0, 0, 0))
        self.disc(cb_ + axb * 0.296, axb, 0.17, "lens_cam", sides=28, glow=False)
        # (the thruster pylons' hydraulic lines went with the boxy pylon: engines())
        # (the top turret's ring: the housing on the spine, details())
        # RESCUE under each side window, yellow
        for sd in (-1, 1):
            self.facet(2, -15.6, -13.6, 0.55, 0.85, stencil("RESCUE", (230, 200, 60), w=192, h=48, size=34, stroke=1), off=0.1, side=sd,
                       uv=[(0, 1), (1, 1), (1, 0), (0, 0)] if sd < 0 else [(1, 1), (0, 1), (0, 0), (1, 0)])

    def pilot_seat(self, x, FL, sx):
        """A pilot's seat (centred on x, the floor at FL; sx: which side, the collective on its outboard side)."""
        zf, zh = -15.8, -14.95                     # the cushion's front edge, the hip line
        rec = 0.22                                  # how far the back leans over its height
        # the rails and the base, the energy-absorbing struts behind the shell
        for rx in (-0.28, 0.28):
            self.B((x + rx - 0.05, FL, -16.2), (x + rx + 0.05, FL + 0.05, -13.9), "metal")
        self.B((x - 0.36, FL + 0.05, -15.6), (x + 0.36, FL + 0.32, -14.7), "dark")
        self.B((x - 0.4, FL + 0.32, -15.75), (x + 0.4, FL + 0.4, -14.6), "metal")
        for rx in (-0.22, 0.22):
            self.prism((x + rx, FL + 0.08, -14.42), (x + rx, FL + 1.55, -14.38), 0.035, "chrome", caps=False)
            self.prism((x + rx, FL + 0.08, -14.42), (x + rx, FL + 0.7, -14.41), 0.055, "metal", caps=False)
        # the shell: two shaped sides and its back
        prof = [(zf + 0.02, FL + 0.4), (zf - 0.02, FL + 0.74), (zh, FL + 0.8), (zh + rec * 0.55, FL + 1.55), (zh + rec, FL + 2.0),
                (zh + rec + 0.16, FL + 2.0), (zh + 0.17, FL + 0.55), (zh + 0.1, FL + 0.4)]
        for sd in (-1, 1):
            self.extrude(prof, x + sd * 0.5, x + sd * 0.44, "hullD")
        self.face([(x - 0.5, FL + 0.55, zh + 0.17), (x + 0.5, FL + 0.55, zh + 0.17), (x + 0.5, FL + 2.0, zh + rec + 0.16), (x - 0.5, FL + 2.0, zh + rec + 0.16)],
                  "hullD", (0, 0, 1))
        # the cushions: the seat with its thigh bolsters, the back with its side bolsters, the headrest's wings
        self.bevel_box((x - 0.4, FL + 0.4, zf), (x + 0.4, FL + 0.6, zh + 0.05), 0.05, "seat")
        for bx in (-0.44, 0.44):
            self.bevel_box((x + bx - 0.07, FL + 0.42, zf + 0.05), (x + bx + 0.07, FL + 0.72, zh), 0.035, "seat")
        T = np.array([0, 1.35, rec]); T /= np.linalg.norm(T)
        Nb = np.array([0, -rec, 1.35]); Nb /= np.linalg.norm(Nb)
        def back_box(xa, xb, s0, s1, d0, d1, b, m):
            base = np.array([0, FL + 0.6, zh + 0.03])
            C_ = []
            for (s_, d_) in ((s0, d0), (s1, d0), (s1, d1), (s0, d1)):
                pass
            self.oriented_box(base + T * (s0 + s1) / 2 - Nb * (d0 + d1) / 2 + np.array([(xa + xb) / 2, 0, 0]),
                              np.array([1.0, 0, 0]), T, (xb - xa) / 2, (s1 - s0) / 2, (d1 - d0) / 2, b, m)
        back_box(x - 0.36, x + 0.36, 0.0, 1.22, 0.0, 0.16, 0.05, "seat")
        for bx in (-0.43, 0.43):
            back_box(x + bx - 0.07, x + bx + 0.07, 0.1, 1.0, 0.0, 0.24, 0.035, "seat")
        back_box(x - 0.24, x + 0.24, 1.3, 1.62, 0.0, 0.2, 0.05, "seat")
        for bx in (-0.28, 0.28):
            back_box(x + bx - 0.05, x + bx + 0.05, 1.3, 1.62, 0.0, 0.3, 0.025, "seat")
        # the armrest on the inboard side, folded down
        ax = x - sx * 0.52
        self.B((ax - 0.04, FL + 0.7, zh - 0.05), (ax + 0.04, FL + 0.9, zh + 0.08), "metal")
        self.bevel_box((ax - 0.06, FL + 0.88, zh - 0.6), (ax + 0.06, FL + 0.98, zh + 0.05), 0.025, "seat")
        # the harness: two shoulder straps down the back cushion, two lap belts, the crotch strap, the rotary buckle
        buckle = np.array([x, FL + 0.66, -15.32])
        base = np.array([0, FL + 0.6, zh + 0.03])
        for bx in (-0.13, 0.13):
            top = base + T * 1.22 - Nb * 0.18 + np.array([x + bx, 0, 0])
            mid = base + T * 0.4 - Nb * 0.18 + np.array([x + bx * 0.6, 0, 0])
            self.strap(top, mid, 0.075, -Nb)
            self.strap(mid, buckle + np.array([bx * 0.25, 0.02, 0.06]), 0.075, np.array([0, 0.6, -0.8]))
            self.B((x + bx - 0.05, top[1] - 0.45, top[2] - 0.17), (x + bx + 0.05, top[1] - 0.38, top[2] - 0.13), "metal")
        for bx in (-0.42, 0.42):
            self.strap(np.array([x + bx, FL + 0.62, zh - 0.02]), buckle + np.array([bx * 0.15, 0, 0]), 0.075, np.array([0, 1.0, 0]))
        self.strap(np.array([x, FL + 0.61, zf + 0.04]), buckle - np.array([0, 0.02, 0.05]), 0.07, np.array([0, 1.0, 0]))
        bn = np.array([0, 0.55, -0.83])
        self.prism(buckle - bn * 0.01, buckle + bn * 0.05, 0.095, "metal")
        self.prism(buckle + bn * 0.05, buckle + bn * 0.07, 0.05, "dark")
        # the controls: the cyclic between the knees (its boot, the shaft, a shaped grip with its hat, trigger and
        # guard), the collective at the outboard side (its arm, twist grip and switch head), the rudder pedals
        self.frustum((x, FL, -16.05), (x, FL + 0.16, -16.04), 0.13, 0.05, "rubber")
        self.bar((x, FL + 0.15, -16.04), (x, FL + 0.82, -15.95), 0.05, "metal")
        g0 = np.array([x, FL + 0.82, -15.95])
        gu = np.array([0, 1.0, -0.22]); gu /= np.linalg.norm(gu)
        self.oriented_box(g0 + gu * 0.14, np.array([1.0, 0, 0]), gu, 0.055, 0.15, 0.07, 0.02, "rubber")
        self.oriented_box(g0 + gu * 0.33, np.array([1.0, 0, 0]), gu, 0.07, 0.06, 0.09, 0.025, "dark")
        self.B((x - 0.02, g0[1] + 0.38, -16.02), (x + 0.02, g0[1] + 0.42, -15.98), "metal")
        self.B((x - 0.025, g0[1] + 0.2, -16.08), (x + 0.025, g0[1] + 0.27, -16.03), "paint_red")
        self.bar((x - 0.04, g0[1] + 0.14, -16.07), (x - 0.04, g0[1] + 0.3, -16.14), 0.015, "metal")
        cx_ = x + sx * 0.62
        self.B((cx_ - 0.06, FL, -14.95), (cx_ + 0.06, FL + 0.22, -14.75), "dark")
        self.bar((cx_, FL + 0.18, -14.85), (cx_, FL + 0.62, -15.7), 0.06, "metal")
        self.prism((cx_, FL + 0.6, -15.66), (cx_, FL + 0.66, -15.78), 0.055, "rubber", caps=False)
        self.bevel_box((cx_ - 0.07, FL + 0.6, -15.98), (cx_ + 0.07, FL + 0.74, -15.76), 0.02, "dark")
        self.q.append(quad([(cx_ - 0.05, FL + 0.745, -15.95), (cx_ + 0.05, FL + 0.745, -15.95), (cx_ + 0.05, FL + 0.745, -15.79), (cx_ - 0.05, FL + 0.745, -15.79)],
                           UV1, "lens_keys", False, np.array([0, 1.0, 0])))
        for px in (-0.25, 0.25):
            self.B((x + px - 0.04, FL + 0.55, -17.8), (x + px + 0.04, FL + 0.62, -17.55), "dark")
            self.bar((x + px, FL + 0.58, -17.6), (x + px, FL + 0.18, -17.35), 0.04, "metal")
            self.oriented_box(np.array([x + px, FL + 0.2, -17.32]), np.array([1.0, 0, 0]), np.array([0, 0.85, -0.53]), 0.1, 0.14, 0.025, 0.012, "metal")
            self.oriented_box(np.array([x + px, FL + 0.36, -17.39]), np.array([1.0, 0, 0]), np.array([0, 0.85, -0.53]), 0.1, 0.04, 0.03, 0.01, "rubber")

    def strap(self, a, b, w, nrm):
        """A harness strap from a to b, w wide, its face toward nrm."""
        a, b = np.asarray(a, float), np.asarray(b, float)
        d = b - a; d /= np.linalg.norm(d)
        side = np.cross(d, np.asarray(nrm, float)); side /= np.linalg.norm(side) + 1e-9
        P = [a - side * w / 2, a + side * w / 2, b + side * w / 2, b - side * w / 2]
        self.face(P, "strap", nrm, ts=0.5)

    def oriented_box(self, c, ax_x, ax_y, hx, hy, hz, b, m):
        """A bevelled box centred on c, its axes ax_x and ax_y (and their cross), half-sizes hx, hy, hz, bevel b."""
        ex = np.asarray(ax_x, float); ex /= np.linalg.norm(ex)
        ey = np.asarray(ax_y, float); ey = ey - (ey @ ex) * ex; ey /= np.linalg.norm(ey)
        ez = np.cross(ex, ey)
        M = np.stack([ex, ey, ez], 1)
        for P, UV, t, g in self._bevel_faces(np.array([-hx, -hy, -hz]), np.array([hx, hy, hz]), b, m):
            self.q.append((np.asarray(P, float) @ M.T + np.asarray(c, float), UV, t, g))

    def gun_bay(self, sd):
        """A door gun's bay, inside its side door (Justin, 9 November 2026: "put some stuff into the area the side guns
        sit, work on the side gun mounts too"). The boom's bed down its middle: a plinth from the troop bay's floor out to
        the door, two rails along it with stops at both ends (rubber faced), the power rail between them on insulators
        (the boom's shoe rides it), yellow edges. Framed at both ends, a trim round its opening into the troop bay. Along
        its forward wall the gun's plasma cell magazine (four cells behind bars, the feed under a cable guard across the
        deck into the bed) and the gunner's control box (guarded switches, lamps, a knob); along its aft wall a spare
        barrel cluster on padded brackets, strapped, and a fire extinguisher; on its head a light, a red night lamp, a
        cable tray and the gunner's tether anchor, the tether stowed on a clip on the aft wall. The gun stows down the bay
        toward its inner forward corner (DOOR_GUN_STOW), over the magazine and clear of it all."""
        X = lambda v: sd * v
        z0, z1 = DOOR_Z
        zb0, zb1 = GUN_BED
        zm = (zb0 + zb1) / 2
        y0, yh = DOOR_Y
        self.cur = "interior"                                           # (all of it drawn only near)
        # the bed (no face of it in the skin's plane at the door, nor under it)
        self.face([(X(5.0), y0, zb0), (X(9.4), y0, zb0), (X(9.4), y0, zb1), (X(5.0), y0, zb1)], "hullD", (0, 1, 0))
        for z_, d in ((zb0, -1), (zb1, 1)):
            self.face([(X(5.0), G + 1.0, z_), (X(9.4), G + 1.0, z_), (X(9.4), y0, z_), (X(5.0), y0, z_)], "hullD", (0, 0, d))
        self.face([(X(5.0), G + 1.0, zb0), (X(5.0), G + 1.0, zb1), (X(5.0), y0, zb1), (X(5.0), y0, zb0)], "hullD", (-sd, 0, 0))
        for z_ in (zb0, zb1 - 0.06):                                    # its yellow edges
            self.B((X(5.0), y0 - 0.01, z_), (X(9.385), y0 + 0.004, z_ + 0.06), "hazard", tile=0.5)
        for zr in (zm - GUN_RAIL, zm + GUN_RAIL):                       # the rails, and their stops
            self.B((X(5.2), y0, zr - 0.06), (X(9.25), y0 + 0.03, zr + 0.06), "metal")
            self.B((X(5.2), y0 + 0.03, zr - 0.035), (X(9.25), G + 1.42, zr + 0.035), "chrome")
            for sa_, sb_, pa_, pb_ in ((5.0, 5.14, 5.14, 5.2), (9.31, 9.4, 9.25, 9.31)):     # a stop, its rubber face
                self.B((X(sa_), y0, zr - 0.09), (X(sb_), G + 1.52, zr + 0.09), "hullD")
                self.B((X(pa_), y0 + 0.03, zr - 0.07), (X(pb_), G + 1.49, zr + 0.07), "rubber")
        for xi in (5.5, 6.3, 7.1, 7.9, 8.7):                            # the power rail, on its insulators
            self.B((X(xi - 0.04), y0, zm - 0.035), (X(xi + 0.04), G + 1.355, zm + 0.035), "rubber")
        self.B((X(5.3), G + 1.355, zm - 0.045), (X(9.2), G + 1.385, zm + 0.045), "metal")
        self.B((X(5.3), G + 1.385, zm - 0.008), (X(9.2), G + 1.388, zm + 0.008), "glow", glow=True)      # its live strip
        # framed at both ends: a rib up each wall and across the head
        for xr, ylo in ((5.45, G + 1.0), (9.15, y0)):
            self.B((X(xr - 0.06), ylo, z0), (X(xr + 0.06), yh, z0 + 0.08), "hullD")
            self.B((X(xr - 0.06), ylo, z1 - 0.08), (X(xr + 0.06), yh, z1), "hullD")
            self.B((X(xr - 0.06), yh - 0.08, z0 + 0.08), (X(xr + 0.06), yh, z1 - 0.08), "hullD")
        for za_, zb_ in ((z0 - 0.12, z0), (z1, z1 + 0.12)):              # the trim round the opening, on the troop bay's side
            self.B((X(5.16), G + 1.0, za_), (X(5.2), yh, zb_), "metal")
        # the forward wall: the plasma cell magazine
        mx0, mx1, zw = 6.0, 7.4, z0
        self.B((X(mx0), G + 1.0, zw), (X(mx1), G + 2.15, zw + 0.03), "hullD")                       # its back
        self.bevel_box((X(mx0), G + 1.0, zw + 0.03), (X(mx1), G + 1.12, zw + 0.5), 0.02, "dark")    # its tray
        self.bevel_box((X(mx0), G + 2.04, zw + 0.03), (X(mx1), G + 2.15, zw + 0.5), 0.02, "dark")   # its cap
        for xp in (mx0, mx1 - 0.07):                                                                 # its front posts
            self.B((X(xp), G + 1.12, zw + 0.43), (X(xp + 0.07), G + 2.04, zw + 0.5), "dark")
        for k in range(4):                                                                           # the cells
            xc = X(mx0 + 0.25 + 0.3 * k)
            for yc_ in (G + 1.12, G + 1.97):
                self.rod((xc, yc_, zw + 0.25), (0, 1.0, 0), [(0.0, 0.0), (0.0, 0.1), (0.07, 0.1), (0.07, 0.0)], "metal", sides=14)
            self.rod((xc, G + 1.19, zw + 0.25), (0, 1.0, 0), [(0.0, 0.085), (0.78, 0.085)], "glow", sides=14)
        for yb in (G + 1.45, G + 1.75):                                                              # the bars across
            self.B((X(mx0 + 0.07), yb, zw + 0.445), (X(mx1 - 0.07), yb + 0.035, zw + 0.48), "metal")
        lx0, lx1 = sorted((X(mx0 + 0.2), X(mx1 - 0.2)))
        self.q.append(quad([(lx0, G + 2.13, zw + 0.503), (lx1, G + 2.13, zw + 0.503), (lx1, G + 2.06, zw + 0.503), (lx0, G + 2.06, zw + 0.503)],
                           UV_UNIT, sign("PLASMA CELLS", fg=(24, 24, 22), bg=(214, 178, 40)), False, np.array([0, 0, 1.0])))
        xg = X(6.7)                                                     # its feed, under a cable guard across to the bed
        C_ = [(xg + dx, y_, zz) for zz in (zw + 0.5, zb0) for dx, y_ in ((-0.17, G + 1.0), (0.17, G + 1.0), (0.06, G + 1.065), (-0.06, G + 1.065))]
        self.brick(C_, {"s12": "hazard", "s23": "hazard", "s30": "hazard"}, skip=("e0", "e1", "s01"))
        # the gunner's control box
        bx0, bx1 = 7.65, 8.45
        self.bevel_box((X(bx0), G + 2.55, zw + 0.004), (X(bx1), G + 3.3, zw + 0.16), 0.02, "dark")
        zf = zw + 0.16
        lx0, lx1 = sorted((X(bx0 + 0.08), X(bx1 - 0.08)))
        self.q.append(quad([(lx0, G + 3.25, zf + 0.002), (lx1, G + 3.25, zf + 0.002), (lx1, G + 3.17, zf + 0.002), (lx0, G + 3.17, zf + 0.002)],
                           UV_UNIT, sign("DOOR GUN", fg=(236, 236, 230), bg=(40, 42, 46)), False, np.array([0, 0, 1.0])))
        for k in range(3):                                              # switches under red guard covers (the first lifted)
            xc = X(bx0 + 0.2 + 0.2 * k)
            self.B((xc - 0.045, G + 3.03, zf), (xc + 0.045, G + 3.05, zf + 0.012), "metal")                  # the hinge
            if k == 0:
                self.B((xc - 0.04, G + 3.04, zf + 0.01), (xc + 0.04, G + 3.06, zf + 0.12), "red", glow=False)
                self.prism((xc, G + 2.97, zf), (xc, G + 3.0, zf + 0.06), 0.009, "metal", sides=6)
            else:
                self.bevel_box((xc - 0.04, G + 2.92, zf), (xc + 0.04, G + 3.04, zf + 0.04), 0.008, "red")
        for k, lens_ in enumerate(("lens_green", "lens_green", "glow", "lens_red")):        # lamps
            xc = X(bx0 + 0.16 + 0.16 * k)
            self.B((xc - 0.03, G + 2.75, zf), (xc + 0.03, G + 2.81, zf + 0.012), lens_, glow=True)
        self.rod((X(bx1 - 0.16), G + 2.64, zf), (0, 0, 1.0), [(0.0, 0.0), (0.0, 0.045), (0.035, 0.045), (0.035, 0.0)], "rubber", sides=12)
        # the aft wall: a spare barrel cluster on padded brackets, strapped
        zw = z1
        yc, zc = G + 3.35, zw - 0.3
        ax = np.array([float(sd), 0, 0])
        c0 = np.array([X(5.95), yc, zc])
        self.rod(c0, ax, [(0.0, 0.0), (0.0, 0.19), (0.3, 0.19), (0.33, 0.16), (0.33, 0.0)], "dark", sides=18)
        for k in range(6):
            a_ = 2 * math.pi * (k + 0.5) / 6
            self.rod(c0 + np.array([0, math.cos(a_) * 0.115, math.sin(a_) * 0.115]) + ax * 0.33, ax, [(0.0, 0.032), (1.85, 0.032)], "metal", sides=8)
        for zc_ in (0.95, 1.75):
            self.rod(c0 + ax * zc_, ax, [(0.0, 0.0), (0.0, 0.165), (0.07, 0.165), (0.07, 0.0)], "hullD", sides=6)
        self.rod(c0 + ax * 2.13, ax, [(0.0, 0.0), (0.0, 0.17), (0.24, 0.17), (0.24, 0.155), (0.25, 0.0)], "dark", sides=12)
        for xb_ in (6.4, 8.0):
            self.B((X(xb_ - 0.04), yc - 0.45, zw - 0.05), (X(xb_ + 0.04), yc - 0.17, zw), "metal")             # on the wall
            self.B((X(xb_ - 0.04), yc - 0.22, zc - 0.2), (X(xb_ + 0.04), yc - 0.17, zw - 0.05), "metal")       # the arm
            self.B((X(xb_ - 0.06), yc - 0.17, zc - 0.16), (X(xb_ + 0.06), yc - 0.13, zc + 0.16), "rubber")     # its pad
            self.rod(np.array([X(xb_), yc, zc]) - ax * 0.025, ax, [(0.0, 0.155), (0.05, 0.155)], "strap", sides=12)
        # a crate of spare cells on the deck by it, strapped down to rings in the deck
        cx0, cx1, cz0, cz1 = 5.75, 6.75, zw - 0.6, zw - 0.04
        self.bevel_box((X(cx0), G + 1.0, cz0), (X(cx1), G + 1.5, cz1), 0.03, "hullD")
        self.bevel_box((X(cx0), G + 1.47, cz0 - 0.005), (X(cx1), G + 1.53, cz1), 0.02, "dark")                     # its lid
        for xh_, d_ in ((cx0, -1), (cx1, 1)):                           # its handles
            self.B((X(xh_ + d_ * 0.035), G + 1.3, (cz0 + cz1) / 2 - 0.12), (X(xh_), G + 1.34, (cz0 + cz1) / 2 + 0.12), "metal")
        lx0, lx1 = sorted((X(cx0 + 0.22), X(cx1 - 0.22)))
        self.q.append(quad([(lx0, G + 1.36, cz0 - 0.003), (lx1, G + 1.36, cz0 - 0.003), (lx1, G + 1.26, cz0 - 0.003), (lx0, G + 1.26, cz0 - 0.003)],
                           [(1, 0), (0, 0), (0, 1), (1, 1)], sign("SPARE CELLS", fg=(24, 24, 22), bg=(214, 178, 40)), False, np.array([0, 0, -1.0])))
        for xs2 in (cx0 + 0.12, cx1 - 0.12):                            # the straps over it, to the deck
            self.B((X(xs2 - 0.03), G + 1.0, cz0 - 0.012), (X(xs2 + 0.03), G + 1.542, cz0), "strap")
            self.B((X(xs2 - 0.03), G + 1.53, cz0 - 0.012), (X(xs2 + 0.03), G + 1.542, cz1), "strap")
            self.B((X(xs2 - 0.045), G + 1.0, cz0 - 0.07), (X(xs2 + 0.045), G + 1.02, cz0 - 0.01), "metal")
        # and a fire extinguisher (the mod's own)
        self.q += model_faces("jugcraft:block/fire_extinguisher",
                              lambda p_: (X(8.7) - (p_[0] - 8) / 16, G + 1.9 + (p_[1] - 0.5) / 16, zw - 0.004 - p_[2] / 16), "extinguisher")
        # the head: a light, a red night lamp, a cable tray, the tether's anchor
        self.q += self.fixture((X(7.2), yh, zm), (0, -1, 0), (0, 0, -1), 0.9, 0.3, "lens_panel", depth=0.05, bezel=0.04, back=0.02, lit=True)
        self.q += self.fixture((X(6.05), yh, zm), (0, -1, 0), (0, 0, -1), 0.2, 0.2, "lens_red", depth=0.05, bezel=0.03, back=0.02, lit=True)
        tz0, tz1, ty = 1.1, 1.5, yh - 0.2
        self.B((X(5.6), ty, tz0), (X(9.0), ty + 0.015, tz1), "hullD")
        for z_ in (tz0, tz1 - 0.015):
            self.B((X(5.6), ty, z_), (X(9.0), ty + 0.08, z_ + 0.015), "hullD")
        for xh in (6.4, 7.5, 8.5):
            for z_ in (tz0 + 0.008, tz1 - 0.008):
                self.prism((X(xh), ty + 0.08, z_), (X(xh), yh, z_), 0.01, "metal", sides=6)
        for z_, m_ in ((1.2, "rubber"), (1.3, "red"), (1.4, "rubber")):
            self.prism((X(5.62), ty + 0.045, z_), (X(8.98), ty + 0.045, z_), 0.028, m_, sides=8)
            for xe in (5.62, 8.98):
                self.prism((X(xe), ty + 0.045, z_), (X(xe), yh, z_), 0.028, m_, sides=8)
        xt_, zt_ = 8.55, 0.75
        self.rod((X(xt_), yh, zt_), (0, -1.0, 0), [(0.0, 0.0), (0.0, 0.07), (0.015, 0.07), (0.015, 0.0)], "metal", sides=12)
        self.B((X(xt_ - 0.04), yh - 0.06, zt_ - 0.01), (X(xt_ + 0.04), yh - 0.015, zt_ + 0.01), "metal")
        self.prism((X(xt_), yh - 0.055, zt_), (X(xt_), G + 3.7, z1 - 0.06), 0.018, "strap", sides=4)
        self.B((X(xt_ - 0.04), G + 3.62, z1 - 0.05), (X(xt_ + 0.04), G + 3.74, z1), "metal")
        self.B((X(xt_ - 0.025), G + 3.62, z1 - 0.075), (X(xt_ + 0.025), G + 3.7, z1 - 0.05), "metal")
        self.cur = "hull"

    def pocketed_face(self, P0, eu, ev, w, h, n, pockets, depth, m, m_in=None, rc=0.5, seg=5):
        """A flat face (the rectangle P0 + eu [0, w] + ev [0, h], n its outward normal) with pockets sunk in it, as a cast
        part's lightening pockets are: each pocket (uc, vc, pw, ph) a slot with rounded corners (their radius rc times
        the slot's shorter side) depth deep, walls and floor m_in. The face is cut in a cell round each pocket (at the
        midpoints between them, along u) and each cell laid from the pocket's rim straight out to the cell's edge."""
        P0, eu, ev, n = [np.asarray(a, float) for a in (P0, eu, ev, n)]
        eu, ev, n = eu / np.linalg.norm(eu), ev / np.linalg.norm(ev), n / np.linalg.norm(n)
        m_in = m_in or m
        at = lambda u, v, d=0.0: P0 + eu * u + ev * v - n * d
        pk = sorted(pockets)
        cuts = [0.0] + [(a[0] + a[2] / 2 + b[0] - b[2] / 2) / 2 for a, b in zip(pk, pk[1:])] + [w]
        if not pk:
            self.face([at(0, 0), at(w, 0), at(w, h), at(0, h)], m, n)
            return
        for (uc, vc, pw, ph), ua, ub in zip(pk, cuts, cuts[1:]):
            r = rc * min(pw, ph)
            hx, hy = pw / 2 - r, ph / 2 - r
            rim = []                                     # the rim (counter-clockwise from +u), finely
            for k, (cx_, cy_) in enumerate(((hx, hy), (-hx, hy), (-hx, -hy), (hx, -hy))):
                for j in range(seg + 1):
                    a_ = (k + j / seg) * math.pi / 2
                    rim.append((cx_ + r * math.cos(a_), cy_ + r * math.sin(a_)))
            ang = np.unwrap([math.atan2(y_, x_) for x_, y_ in rim])
            corners = [math.atan2(cv - vc, cu - uc) for cu, cv in ((ub, h), (ua, h), (ua, 0), (ub, 0))]
            want = sorted(set([round(a_ % (2 * math.pi), 9) for a_ in list(np.linspace(0, 2 * math.pi, 4 * seg + 4, endpoint=False)) + corners]))
            def rim_at(t):
                t = (t - ang[0]) % (2 * math.pi) + ang[0]
                i = int(np.searchsorted(ang, t, side="right"))
                if i >= len(rim):
                    a0, a1, (x0_, y0_), (x1_, y1_) = ang[-1], ang[0] + 2 * math.pi, rim[-1], rim[0]
                else:
                    a0, a1, (x0_, y0_), (x1_, y1_) = ang[i - 1], ang[i], rim[i - 1], rim[i]
                f = 0.0 if abs(a1 - a0) < 1e-12 else (t - a0) / (a1 - a0)
                return np.array([x0_ + (x1_ - x0_) * f, y0_ + (y1_ - y0_) * f])
            def edge_at(t):
                d_ = np.array([math.cos(t), math.sin(t)])
                ts = [((ub if d_[0] > 0 else ua) - uc) / d_[0] if abs(d_[0]) > 1e-12 else 1e9,
                      ((h if d_[1] > 0 else 0.0) - vc) / d_[1] if abs(d_[1]) > 1e-12 else 1e9]
                return np.array([uc, vc]) + d_ * min(ts)
            R_ = [rim_at(t) + np.array([uc, vc]) for t in want]
            E_ = [edge_at(t) for t in want]
            K_ = len(want)
            for i in range(K_):
                j = (i + 1) % K_
                self.face([at(*R_[i]), at(*R_[j]), at(*E_[j]), at(*E_[i])], m, n)
                mid = (R_[i] + R_[j]) / 2
                inward = eu * (uc - mid[0]) + ev * (vc - mid[1])
                self.face([at(*R_[i]), at(*R_[j]), at(*R_[j], depth), at(*R_[i], depth)], m_in, inward)
            fl = [at(*R_[i], depth) for i in range(K_)]
            for Q in fan_quads(fl):
                self.face(Q, m_in, n)

    def girder(self, x0, x1, yc, hy, zc, hz, b, m, pockets=(), depth=0.05, m_in=None, caps=(True, True)):
        """A box girder along x from x0 to x1 (x0 < x1; y yc +- hy, z zc +- hz), its four long edges chamfered b, pockets
        sunk in both its sides ((x, y) their middles, length, height: as pocketed_face, on x and y)."""
        y0_, y1_, z0_, z1_ = yc - hy, yc + hy, zc - hz, zc + hz
        oct_ = [(z1_ - b, y1_), (z0_ + b, y1_), (z0_, y1_ - b), (z0_, y0_ + b), (z0_ + b, y0_), (z1_ - b, y0_), (z1_, y0_ + b), (z1_, y1_ - b)]
        for k in range(8):
            (za_, ya_), (zb_, yb_) = oct_[k], oct_[(k + 1) % 8]
            nrm = np.array([0.0, (ya_ + yb_) / 2 - yc, (za_ + zb_) / 2 - zc])
            if k in (2, 6):                              # the sides, pocketed
                zs_ = za_
                pk = [(px - x0, py - (y0_ + b), pl, ph) for (px, py, pl, ph) in pockets]
                if k == 2:
                    self.pocketed_face((x1, y0_ + b, zs_), (-1, 0, 0), (0, 1, 0), x1 - x0, 2 * (hy - b), (0, 0, -1),
                                       [(x1 - x0 - u_, v_, pl, ph) for (u_, v_, pl, ph) in pk], depth, m, m_in)
                else:
                    self.pocketed_face((x0, y0_ + b, zs_), (1, 0, 0), (0, 1, 0), x1 - x0, 2 * (hy - b), (0, 0, 1), pk, depth, m, m_in)
                continue
            self.face([(x0, ya_, za_), (x1, ya_, za_), (x1, yb_, zb_), (x0, yb_, zb_)], m, nrm)
        for x_, d, on in ((x0, -1.0, caps[0]), (x1, 1.0, caps[1])):
            if on:
                for Q in fan_quads([np.array([x_, y_, z_]) for z_, y_ in oct_]):
                    self.face(Q, m, (d, 0, 0))

    def bevel_box(self, lo, hi, b, m):
        """An axis-aligned box with its edges bevelled b (a cushion, a knob)."""
        lo, hi = np.minimum(lo, hi), np.maximum(lo, hi)
        if too_small(float(np.max(np.asarray(hi, float) - np.asarray(lo, float)))):
            return
        self.q += self._bevel_faces(np.asarray(lo, float), np.asarray(hi, float), b, m)

    def _bevel_faces(self, lo, hi, b, m):
        lo, hi = np.asarray(lo, float), np.asarray(hi, float)
        b = min(b, *(0.45 * (hi - lo)))
        c = (lo + hi) / 2
        out = []
        def P(sx, sy, sz, ix, iy, iz):
            # a corner of the face-inset grid: s = -1/1 side, i = whether pulled in by the bevel on that axis
            return np.array([c[0] + sx * ((hi - lo)[0] / 2 - (b if ix else 0)), c[1] + sy * ((hi - lo)[1] / 2 - (b if iy else 0)),
                             c[2] + sz * ((hi - lo)[2] / 2 - (b if iz else 0))])
        for ax in range(3):
            for sg in (-1, 1):
                o = [a for a in range(3) if a != ax]
                corners = []
                for s1, s2 in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
                    sv = [0, 0, 0]; iv = [True, True, True]
                    sv[ax] = sg; iv[ax] = False
                    sv[o[0]] = s1; sv[o[1]] = s2
                    corners.append(P(*sv, *iv))
                nrm = np.zeros(3); nrm[ax] = sg
                out.append(quad(corners, UV_UNIT, m, False, nrm))
        # the twelve bevels along the edges and the eight corner facets
        for ax in range(3):
            o = [a for a in range(3) if a != ax]
            for s1 in (-1, 1):
                for s2 in (-1, 1):
                    q4 = []
                    for se in (-1, 1):
                        for which in (0, 1):
                            sv = [0, 0, 0]; iv = [True, True, True]
                            sv[ax] = se
                            sv[o[0]] = s1; sv[o[1]] = s2
                            iv[o[which]] = False
                            q4.append(P(*sv, *iv))
                    nrm = np.zeros(3); nrm[o[0]] = s1; nrm[o[1]] = s2
                    out.append(quad([q4[0], q4[1], q4[3], q4[2]], UV_UNIT, m, False, nrm))
        for sx_ in (-1, 1):
            for sy_ in (-1, 1):
                for sz_ in (-1, 1):
                    A = P(sx_, sy_, sz_, False, True, True); Bq = P(sx_, sy_, sz_, True, False, True); Cq = P(sx_, sy_, sz_, True, True, False)
                    out.append(quad([A, Bq, Cq, Cq], UV_UNIT, m, False, np.array([sx_, sy_, sz_], float)))
        return out

    def extrude(self, prof, x0, x1, m):
        """A flat plate in the shape of the polygon prof ((z, y) points) between x0 and x1 (a seat shell's side)."""
        n = len(prof)
        cz = sum(p[0] for p in prof) / n; cy = sum(p[1] for p in prof) / n
        for x, d in ((x0, np.sign(x0 - x1)), (x1, np.sign(x1 - x0))):
            for k in range(n):
                a, b_ = prof[k], prof[(k + 1) % n]
                self.face([(x, cy, cz), (x, a[1], a[0]), (x, b_[1], b_[0]), (x, b_[1], b_[0])], m, (d, 0, 0))
        for k in range(n):
            a, b_ = prof[k], prof[(k + 1) % n]
            mid = np.array([0, (a[1] + b_[1]) / 2 - cy, (a[0] + b_[0]) / 2 - cz])
            self.face([(x0, a[1], a[0]), (x1, a[1], a[0]), (x1, b_[1], b_[0]), (x0, b_[1], b_[0])], m, mid)

    def frustum(self, p0, p1, r0, r1, m, sides=8):
        """A tapered rod (a rubber boot)."""
        p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
        if too_small(max(2 * r0, 2 * r1, np.linalg.norm(p1 - p0)), 2 * max(r0, r1)):
            return
        sides = round_sides(sides, max(r0, r1))
        ax = p1 - p0; ax /= np.linalg.norm(ax)
        e1 = np.cross(ax, [0, 0, 1.0]) if abs(ax[2]) < 0.9 else np.cross(ax, [1.0, 0, 0])
        e1 /= np.linalg.norm(e1); e2 = np.cross(ax, e1)
        rg = lambda r: [e1 * math.cos(2 * math.pi * k / sides) * r + e2 * math.sin(2 * math.pi * k / sides) * r for k in range(sides)]
        A, B_ = rg(r0), rg(r1)
        for k in range(sides):
            k2 = (k + 1) % sides
            self.face([p0 + A[k], p0 + A[k2], p1 + B_[k2], p1 + B_[k]], m, (A[k] + A[k2]) / 2, ts=1.0)

    def rod(self, base, ax, prof, m, sides=20, e1=None, step=None, uv=None, voff=0.0):
        """A body of revolution along ax from base (Justin, 7 November 2026: "The long rod things and the mount they're
        on still don't look right, I don't like textures that look stretched"): rings through the profile's keys (a, r)
        or (a, r, kn, kt) (kn: its stretch along e1, kt: across), sampled every step along a smooth curve through them
        when step is given; enough sides to shade round. uv="cyl" (for the skin's panels, Justin: "Panel the round parts"):
        the texture wrapped round it a whole number of times (so it meets itself) and run along its profile at true size,
        so the panels' seams go round it and along it and on over its ends as rings and spokes; voff: where along the
        texture it starts (clear of the plating's little service labels, which read as stickers on a part this small)."""
        base, ax = np.asarray(base, float), np.asarray(ax, float) / np.linalg.norm(ax)
        if e1 is None:
            e1 = np.cross(ax, [0, 0, 1.0]) if abs(ax[2]) < 0.9 else np.cross(ax, [1.0, 0, 0])
        e1 = np.asarray(e1, float); e1 = e1 - (e1 @ ax) * ax; e1 /= np.linalg.norm(e1)
        e2 = np.cross(ax, e1)
        keys = [tuple(k) + (1.0, 1.0)[len(k) - 2:] if len(k) < 4 else tuple(k) for k in prof]
        rmax = max(k[1] * max(k[2], k[3]) for k in keys)
        if too_small(max(2 * rmax, abs(keys[-1][0] - keys[0][0])), 2 * rmax):
            return
        sides = round_sides(sides, rmax)
        if step:
            A_ = [k[0] for k in keys]
            n_ = max(1, int(round((A_[-1] - A_[0]) / step)))
            keys = [(a,) + tuple(_pchip(A_, [k[c] for k in keys], a) for c in (1, 2, 3)) for a in np.linspace(A_[0], A_[-1], n_ + 1)]
        rings = []
        for (a, r, kn, kt) in keys:
            r = max(r, 0.003)
            c = base + ax * a
            rings.append([c + e1 * math.cos(2 * math.pi * k / sides) * r * kn + e2 * math.sin(2 * math.pi * k / sides) * r * kt
                          for k in range(sides)])
        TB = TEX_BLOCKS["hull"]
        reps = max(1, int(round(2 * math.pi * max(max(k[1], 0.003) * max(k[2], k[3]) for k in keys) / TB)))
        sv = [0.0]
        for i in range(len(keys) - 1):
            sv.append(sv[-1] + math.hypot(keys[i + 1][0] - keys[i][0], max(keys[i + 1][1], 0.003) - max(keys[i][1], 0.003)))
        for i in range(len(rings) - 1):
            A, B_ = rings[i], rings[i + 1]
            da, dr = keys[i + 1][0] - keys[i][0], max(keys[i + 1][1], 0.003) - max(keys[i][1], 0.003)
            for k in range(sides):
                k2 = (k + 1) % sides
                rad = (A[k] + A[k2]) / 2 - (base + ax * keys[i][0])
                rad /= np.linalg.norm(rad) + 1e-12
                ref = rad * da - ax * dr                   # (the profile's outward side: a recess's walls face in)
                if uv == "cyl":
                    u0, u1, v0, v1 = k / sides * reps, (k + 1) / sides * reps, sv[i] / TB + voff, sv[i + 1] / TB + voff
                    UVq = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
                elif isinstance(uv, tuple) and uv[0] == "unroll":
                    # unrolled flat onto the face it rises out of (uv[1]: that face's texture, an affine map of x and z;
                    # its parts round the top lie on the face's own panels, the seam where the ends meet underneath)
                    Am, cz_, e2z = uv[1], uv[2], uv[3]
                    kk = k - sides if k >= sides // 2 else k
                    t0_, t1_ = 2 * math.pi * kk / sides, 2 * math.pi * (kk + 1) / sides
                    ri, rj = max(keys[i][1], 0.003), max(keys[i + 1][1], 0.003)
                    f_ = lambda x_, r_, t_: tuple(np.array([x_, cz_ + e2z * r_ * t_, 1.0]) @ Am)
                    UVq = [f_(A[k][0], ri, t0_), f_(A[k2][0], ri, t1_), f_(B_[k2][0], rj, t1_), f_(B_[k][0], rj, t0_)]
                elif isinstance(uv, tuple) and uv[0] == "fold":
                    # from ring uv[1] on (a face across the axis) the world's own layout, as the face it lies on carries
                    # (x and z over the texture's size); short of it the same, folded on over the edge: each point where
                    # it would lie were the profile laid flat out from that edge
                    # (uv[3], when given: the face's texture as an affine map of x and z, for a face not laid flat)
                    ke, (cx_, cz_) = uv[1], uv[2]
                    Af = uv[3] if len(uv) > 3 else np.array([[1 / TB, 0], [0, 1 / TB], [0, 0]])
                    re_ = max(keys[ke][1], 0.003)
                    def fu(p_, ii):
                        if ii >= ke:
                            return tuple(np.array([p_[0], p_[2], 1.0]) @ Af)
                        d_ = np.array([p_[0] - cx_, p_[2] - cz_]); d_ /= np.linalg.norm(d_) + 1e-12
                        ro = re_ + (sv[ke] - sv[ii])
                        return tuple(np.array([cx_ + d_[0] * ro, cz_ + d_[1] * ro, 1.0]) @ Af)
                    UVq = [fu(A[k], i), fu(A[k2], i), fu(B_[k2], i + 1), fu(B_[k], i + 1)]
                elif callable(uv):
                    UVq = [uv(p_) for p_ in (A[k], A[k2], B_[k2], B_[k])]
                else:
                    UVq = [(0, 0), (1, 0), (1, 1), (0, 1)]
                self.q.append(quad([A[k], A[k2], B_[k2], B_[k]], UVq, m, False, ref))

    def disc(self, c, ax, r, m, sides=24, glow=False):
        """A round flat face (a lens) facing ax, its texture laid on corner to corner."""
        if too_small(2 * r, 0.0):
            return
        sides = round_sides(sides, r, 8)
        c, ax = np.asarray(c, float), np.asarray(ax, float) / np.linalg.norm(ax)
        e1 = np.cross(ax, [0, 1.0, 0]) if abs(ax[1]) < 0.9 else np.cross(ax, [1.0, 0, 0])
        e1 /= np.linalg.norm(e1); e2 = np.cross(ax, e1)
        for k in range(sides):
            a0_, a1_ = 2 * math.pi * k / sides, 2 * math.pi * (k + 1) / sides
            p0_ = c + (e1 * math.cos(a0_) + e2 * math.sin(a0_)) * r
            p1_ = c + (e1 * math.cos(a1_) + e2 * math.sin(a1_)) * r
            uv = [(0.5, 0.5), (0.5 + 0.5 * math.cos(a0_), 0.5 - 0.5 * math.sin(a0_)), (0.5 + 0.5 * math.cos(a1_), 0.5 - 0.5 * math.sin(a1_)),
                  (0.5 + 0.5 * math.cos(a1_), 0.5 - 0.5 * math.sin(a1_))]
            self.q.append(quad([c, p0_, p1_, p1_], uv, m, glow, ax))

    @staticmethod
    def _ring_arc(z):
        r = [np.array(p, float) for p in ring_at(z)]
        cum = [0.0]
        for j in range(14):
            cum.append(cum[-1] + np.linalg.norm(r[(j + 1) % 14] - r[j]))
        return r, cum

    def skin_at(self, z, seg, t, side=-1, ds=0.0):
        """A point on the skin (and its outward normal, eased between the facets' so it turns smoothly) at facet seg's t
        (as facet_quad counts it) moved ds round the cross-section."""
        if side > 0:
            seg, t = 13 - seg, 1 - t
        r, cum = self._ring_arc(z)
        _, Yb, Yt = params_at(z)
        c = np.array([0.0, (Yb + Yt) / 2])
        en = []
        for j in range(14):
            e = r[(j + 1) % 14] - r[j]
            nn = np.array([e[1], -e[0]]); nn /= np.linalg.norm(nn) + 1e-12
            if nn @ ((r[j] + r[(j + 1) % 14]) / 2 - c) < 0:
                nn = -nn
            en.append(nn)
        s_ = (cum[seg] + t * (cum[seg + 1] - cum[seg]) + ds * (1 if side < 0 else -1)) % cum[-1]
        j = max(k for k in range(14) if cum[k] <= s_)
        u = (s_ - cum[j]) / (cum[j + 1] - cum[j] + 1e-12)
        p = r[j] + (r[(j + 1) % 14] - r[j]) * u
        vn0 = en[j - 1] + en[j]; vn1 = en[j] + en[(j + 1) % 14]
        nn = vn0 / np.linalg.norm(vn0) * (1 - u) + vn1 / np.linalg.norm(vn1) * u
        nn /= np.linalg.norm(nn)
        return np.array([p[0], p[1], z]), np.array([nn[0], nn[1], 0.0])

    def hump(self, seg, t, side, keys, m, cap=None, step=0.1, k=17, uvf=None):
        """A low fairing moulded on the skin along z (keys (z, half-width round the skin, height)): each cross-section
        follows the skin round, a soft bell standing off it, its edges easing a hair under the skin; the front end
        capped (cap: its material) when asked."""
        if FAR:
            step, k = step * 2.5, 9
        Z = [q[0] for q in keys]
        zs = np.linspace(Z[0], Z[-1], max(2, int(round(abs(Z[-1] - Z[0]) / step)) + 1))
        Zs, W_, H_ = (Z, [q[1] for q in keys], [q[2] for q in keys]) if Z[0] < Z[-1] else (Z[::-1], [q[1] for q in keys][::-1], [q[2] for q in keys][::-1])
        rows = []
        for z in zs:
            w, h = _pchip(Zs, W_, z), _pchip(Zs, H_, z)
            row = []
            for x in np.linspace(-1, 1, k):
                p, nn = self.skin_at(z, seg, t, side, ds=x * w)
                row.append(p + nn * (h * (1 - x * x) ** 1.4 - 0.02 * x * x))
            rows.append(row)
        for i in range(len(rows) - 1):
            for j in range(k - 1):
                P = [rows[i][j], rows[i][j + 1], rows[i + 1][j + 1], rows[i + 1][j]]
                _, nn = self.skin_at((zs[i] + zs[i + 1]) / 2, seg, t, side)
                UVq = [uvf(p_) for p_ in P] if uvf else [(0, 0), (1, 0), (1, 1), (0, 1)]       # (uvf: e.g. the skin's own)
                self.q.append(quad(P, UVq, m, False, nn))
        if cap:
            row = rows[-1]
            c = np.mean(row, 0)
            fwd = np.array([0, 0, np.sign(zs[-1] - zs[0])])
            for j in range(k - 1):
                self.q.append(quad([c, row[j], row[j + 1], row[j + 1]], [(0, 0), (1, 0), (1, 1), (1, 1)], cap, False, fwd))

    def recessed_light(self, xo, sd, za, zb, ya, yb, zc, yc, w, h, lens, depth=0.14):
        """A flat panel of the sponson's side (x = xo, z za..zb, y ya..yb) with a lamp set into it at (zc, yc): the panel
        round the hole, the recess's walls, a thin bezel at its mouth, the lens at its back."""
        out = (sd, 0, 0)
        hz0, hz1, hy0, hy1 = zc - w / 2, zc + w / 2, yc - h / 2, yc + h / 2
        for (a0, a1, b0, b1) in ((za, hz0, ya, yb), (hz1, zb, ya, yb), (hz0, hz1, ya, hy0), (hz0, hz1, hy1, yb)):
            if a1 - a0 > 1e-6 and b1 - b0 > 1e-6:
                self.face([(xo, b1, a0), (xo, b1, a1), (xo, b0, a1), (xo, b0, a0)], "hull", out)
        xi = xo - sd * depth
        rim = [(hz0, hy1), (hz1, hy1), (hz1, hy0), (hz0, hy0)]
        for k in range(4):
            (z0_, y0_), (z1_, y1_) = rim[k], rim[(k + 1) % 4]
            P = [(xo, y0_, z0_), (xo, y1_, z1_), (xi, y1_, z1_), (xi, y0_, z0_)]
            ref = np.array([0.0, yc - (y0_ + y1_) / 2, zc - (z0_ + z1_) / 2])
            self.face(P, "dark", ref)
            # the bezel: a thin metal lip round the mouth, flush with the skin
            Pm = np.array([(z0_, y0_), (z1_, y1_)])
            cz, cy = zc, yc
            q0 = [(xo + sd * 0.012, y0_, z0_), (xo + sd * 0.012, y1_, z1_)]
            o0 = [(xo + sd * 0.012, y1_ + np.sign(y1_ - cy) * 0.05 if abs(y1_ - cy) > 1e-6 else y1_, z1_ + np.sign(z1_ - cz) * 0.05),
                  (xo + sd * 0.012, y0_ + np.sign(y0_ - cy) * 0.05 if abs(y0_ - cy) > 1e-6 else y0_, z0_ + np.sign(z0_ - cz) * 0.05)]
            self.face([q0[0], q0[1], o0[0], o0[1]], "metal", out, ts=1.0)
        self.q.append(quad([(xi + sd * 0.01, hy1, hz0), (xi + sd * 0.01, hy1, hz1), (xi + sd * 0.01, hy0, hz1), (xi + sd * 0.01, hy0, hz0)],
                           UV1 if sd > 0 else UV1[[1, 0, 3, 2]], lens, self.lights, np.array(out, float)))

    def raised(self, P, n, h, front, side="hullD", uv=None, lit=False):
        """A box standing h out of the hull on the patch P (its outward normal n), front: its outer face's material."""
        n = np.asarray(n, float); n = n / np.linalg.norm(n)
        P = [np.asarray(p, float) for p in P]
        T = [p + n * h for p in P]
        cen = np.mean(P, 0)
        su, sv = (P[1] - P[0]) / np.linalg.norm(P[1] - P[0]), (P[3] - P[0]) / np.linalg.norm(P[3] - P[0])
        fl = 0.1 + h
        base = [P[0] - (su + sv) * fl, P[1] + (su - sv) * fl, P[2] + (su + sv) * fl, P[3] + (sv - su) * fl]
        base = [b_ - n * 0.02 for b_ in base]
        for k in range(4):
            k2 = (k + 1) % 4
            Q = [base[k], base[k2], T[k2], T[k]]
            self.q.append(quad(Q, UV_UNIT, side, False, np.mean(Q, 0) - cen + n * 0.3))
        self.q.append(quad(T, UV1 if uv is None else uv, front, lit, n))

    def review_kit(self):
        """A last pass over the outside (Justin, 7 November 2026: "do another go over, see if any parts need to be added or
        moved"): a rescue hoist over the starboard door, flare dispensers low on the flanks aft, a refuelling probe along
        the starboard side of the nose, gear doors shut over the bays in flight, a cargo hook under the belly, footholds
        up the sponsons' sides, static wicks on the fin."""
        self.cur = "hull"
        B = self.B
        # the rescue hoist (Justin, 7 November 2026: fix them one by one): a round pedestal grown out of the sponson's
        # sloping top (its panels lined up with the top's), a turning collar, a knuckle, the arm out over the door's aft
        # end, the hoist's housing on its end, the hook stowed up under it (clear of the name on the sponson's side)
        sd, zh = 1, 1.2
        xo_ = SP_O["x"]
        sx_ = sd * 8.25                                                                          # (its skirt clear of the top's edge)
        ys_ = SP_O["yt"] + (SP_I["yt"] - SP_O["yt"]) * (xo_ - abs(sx_)) / (xo_ - SP_I["x"])        # the top's height there
        tops = [f_ for f_ in self.q if not isinstance(f_[0], str) and f_[2] is mat("skin") and face_normal(f_[0])[1] > 0.9
                and abs(np.asarray(f_[0])[:, 0].max() - xo_) < 1e-3 and np.asarray(f_[0])[:, 0].min() > SP_I["x"] - 1e-3]
        if tops:
            Mt = np.c_[np.asarray(tops[0][0])[:, [0, 2]], np.ones(4)]
            A_sp = np.linalg.lstsq(Mt, np.asarray(tops[0][1], float), rcond=None)[0]
        else:
            A_sp = np.array([[1 / TEX_BLOCKS["hull"], 0], [0, 1 / TEX_BLOCKS["hull"]], [0, 0]])
        yb_ = ys_ - 0.35
        self.rod((sx_, yb_, zh), (0, 1.0, 0), [(0.0, 0.72), (0.5, 0.48), (0.55, 0.45), (0.59, 0.42), (0.6, 0.38), (0.6, 0.0)], "skin", sides=28,
                 uv=("fold", 4, (sx_, zh), A_sp))
        yc_ = yb_ + 0.6
        self.rod((sx_, yc_, zh), (0, 1.0, 0), [(-0.05, 0.17), (0.04, 0.17), (0.04, 0.15), (0.07, 0.15), (0.07, 0.17), (0.2, 0.17), (0.2, 0.0)],
                 "boom", sides=20)
        yk_ = yc_ + 0.2
        self.rod((sx_, yk_, zh), (0, 1.0, 0), [(-0.02, 0.2), (0.16, 0.2), (0.2, 0.18), (0.23, 0.14), (0.24, 0.08), (0.24, 0.0)], "paint", sides=20)
        ya_ = yk_ + 0.12
        xu_ = sd * 10.3
        self.rod((sx_, ya_, zh), (sd, 0, 0), [(0.0, 0.1), (abs(xu_ - sx_) - 0.1, 0.085)], "paint", sides=16)
        # the hoist's housing: a drum across the arm's end, its ends rounded, a dark band, a metal end cap
        hc = np.array([xu_, ya_, zh])
        ax_h = np.array([0, 0, 1.0])
        self.rod(hc, ax_h, [(-0.4, 0.0), (-0.4, 0.18), (-0.39, 0.21), (-0.37, 0.23), (-0.34, 0.24), (-0.06, 0.24), (-0.06, 0.25), (0.02, 0.25),
                            (0.02, 0.24), (0.34, 0.24), (0.37, 0.23), (0.39, 0.21), (0.4, 0.18), (0.4, 0.0)], "paint", sides=24)
        self.rod(hc, ax_h, [(-0.06, 0.25), (0.02, 0.25)], "dark", sides=24)
        self.rod(hc, ax_h, [(0.4, 0.12), (0.415, 0.12), (0.415, 0.0)], "metal", sides=16)
        # the cable's last bit and the hook, stowed up under the housing
        self.bar(hc - np.array([0, 0.24, 0]), hc - np.array([0, 0.34, 0]), 0.025, "dark")
        self.rod(hc - np.array([0, 0.34, 0]), (0, -1.0, 0), [(0.0, 0.0), (0.0, 0.05), (0.03, 0.08), (0.14, 0.085), (0.17, 0.06), (0.18, 0.0)],
                 "paint_red", sides=12)
        hk = hc - np.array([0, 0.52, 0])
        self.bar(hk, hk - np.array([0, 0.12, 0]), 0.03, "metal")
        self.bar(hk - np.array([0, 0.12, 0]), hk - np.array([0, 0.16, -0.06]), 0.03, "metal")
        self.bar(hk - np.array([0, 0.16, -0.06]), hk - np.array([0, 0.11, -0.13]), 0.03, "metal")
        # flare dispensers, two magazines each side, low on the flank behind the thruster pylons (Justin, 7 November
        # 2026: fix them one by one): set into an opening in the skin, not boxes on it
        for side in (-1, 1):
            self.flare_bay(*self.facet_quad(2, 14.95, 16.4, 0.3, 0.72, side))
        # the refuelling probe along the nose's starboard side: its root growing out of the skin (a long round fairing
        # in the skin's paint, sunk into it), the tube in two steps, the nozzle's cone and collar
        # the probe's housing: a low hump moulded on the skin (it follows the skin round and along, its edges easing
        # into it, its tail sinking back in), open at its front where the tube comes out through a collar
        # (high on the flank under the canopy's sill, clear of it and ahead of the hull number)
        zf = -19.6
        tc = 1 - 0.38 / np.linalg.norm(np.subtract(ring_at(zf)[11], ring_at(zf)[10]))
        S0, N0 = self.skin_at(zf, 3, tc, 1)
        F = S0 + N0 * 0.1
        self.hump(3, tc, 1, [(-17.6, 0.16, 0.0), (-17.95, 0.25, 0.1), (-18.4, 0.29, 0.17), (-19.0, 0.28, 0.2), (zf, 0.23, 0.2)],
                  "paint", cap="hullD")
        dp = np.array([-0.72, -0.32, -8.6]); dp /= np.linalg.norm(dp)
        L_ = (27.0 + zf) / -dp[2]
        self.rod(F, dp, [(-0.9, 0.085), (0.02, 0.085)], "boom", sides=20, e1=N0)
        self.rod(F, dp, [(0.02, 0.085), (0.02, 0.105), (0.1, 0.105), (0.1, 0.085)], "metal", sides=20, e1=N0)
        self.rod(F, dp, [(0.1, 0.085), (L_ - 1.6, 0.085), (L_ - 1.56, 0.072), (L_ - 0.3, 0.072), (L_ - 0.14, 0.13), (L_ - 0.12, 0.13)],
                 "boom", sides=20, e1=N0)
        self.rod(F, dp, [(L_ - 0.12, 0.13), (L_ - 0.04, 0.13), (L_ - 0.04, 0.1)], "metal", sides=20, e1=N0)
        self.rod(F, dp, [(L_ - 0.04, 0.1), (L_ + 0.09, 0.05), (L_ + 0.11, 0.0)], "dark", sides=20, e1=N0)
        # the cargo hook (Justin, 8 November 2026: fix them one by one): in a well let into the belly (an opening in
        # the skin, SKIN_HOLES; a thin lip, its dark walls, its roof), the hook hung from a beam across it, its tip just
        # under the belly
        x0w, x1w, z0w, z1w, dw = -0.6, 0.6, 0.5, 2.6, 0.38
        Wc = [np.array(v_) for v_ in ((x0w, G, z0w), (x1w, G, z0w), (x1w, G, z1w), (x0w, G, z1w))]
        cw = np.mean(Wc, 0)
        dn = np.array([0, -1.0, 0])
        Ow = [Wc[0] + np.array([-0.05, 0, -0.05]), Wc[1] + np.array([0.05, 0, -0.05]), Wc[2] + np.array([0.05, 0, 0.05]), Wc[3] + np.array([-0.05, 0, 0.05])]
        for k in range(4):
            k2 = (k + 1) % 4
            self.q.append(quad([Ow[k] + dn * 0.012, Ow[k2] + dn * 0.012, Wc[k2] + dn * 0.012, Wc[k] + dn * 0.012], UV_UNIT, "hullD", False, dn))
            self.q.append(quad([Wc[k] + dn * 0.012, Wc[k2] + dn * 0.012, Wc[k2] - dn * dw, Wc[k] - dn * dw], UV_UNIT, "dark", False, cw - (Wc[k] + Wc[k2]) / 2))
        self.q.append(quad([p_ - dn * dw for p_ in Wc], UV_UNIT, "dark", False, dn))
        self.B((-0.5, G + dw - 0.12, 1.45), (0.5, G + dw, 1.65), "metal")                          # the beam
        self.bar((0.0, G + dw - 0.12, 1.55), (0.0, G - 0.12, 1.55), 0.08, "metal")
        self.bar((0.0, G - 0.12, 1.55), (0.0, G - 0.22, 1.75), 0.07, "metal")
        self.bar((0.0, G - 0.22, 1.75), (0.0, G - 0.1, 1.88), 0.06, "metal")
        # footholds up each sponson's side aft of the door (clear of the door's slide and the name), a grab handle on
        # the sponson's top edge over them
        for side in (-1, 1):
            xo = side * SP_O["x"]
            for y in (G + 1.35, G + 2.25, G + 3.55):                  # (the floodlight sits over the top one's place)
                self.B(*sorted([(xo + side * 0.005, y, 5.35), (xo + side * 0.02, y + 0.16, 5.8)], key=lambda p: p[0]), "dark")
                self.B(*sorted([(xo + side * 0.005, y + 0.16, 5.35), (xo + side * 0.025, y + 0.19, 5.8)], key=lambda p: p[0]), "hazard")
            ty = SP_O["yt"] + 0.02
            a, b = np.array([xo - side * 0.35, ty + 0.12, 5.4]), np.array([xo - side * 0.35, ty + 0.12, 5.75])
            self.bar(a, b, 0.05, "metal")
            for e in (a, b):
                self.bar(e - np.array([0, 0.14, 0]), e, 0.05, "metal")
        # (no static wicks off the fin's trailing edge: Justin's playtest, the rods sticking out of it)

    def flight_deck(self):
        """The pilots' deck behind the canopy (Justin, 7 November 2026: "detail inside the hull and the pilot area"): its
        lining with the windows through it, the floor, the bulkheads fore and aft (the door through to the bay, two steps
        down on the far side), two pilot seats with their harnesses, sticks and pedals, the instrument panel's five
        displays under a glare shield, the centre console with its throttles, side consoles, the overhead switch panel,
        dim red map lights."""
        self.cur = "hull"
        FL = FLIGHT_DECK
        zs = [z for z, *_ in STATIONS if -21.5 - 1e-6 <= z <= BULK[0] + 1e-6]
        if zs[-1] < BULK[0]:
            zs.append(BULK[0])
        rin = [ring(params_at(z)[0] - LINING, params_at(z)[1] + LINING, params_at(z)[2] - LINING) for z in zs]
        win = SIDE_WIN
        for i in range(len(zs) - 1):
            za, zb = zs[i], zs[i + 1]
            ra, rb = rin[i], rin[i + 1]
            cen = np.array([0, (params_at(za)[1] + params_at(za)[2]) / 2, (za + zb) / 2])
            for j in range(2, 12):
                if za >= CANOPY[0][0] - 1e-6 and zb <= CANOPY[-1][1] + 1e-6 and j in CANOPY_SEGS:
                    continue
                k = j + 1
                P = [(ra[j][0], ra[j][1], za), (ra[k][0], ra[k][1], za), (rb[k][0], rb[k][1], zb), (rb[j][0], rb[j][1], zb)]
                ref = cen - np.mean(np.array(P), 0); ref[2] = 0
                if zb > win[0] + 1e-6 and za < win[1] - 1e-6 and j in (3, 10):
                    self.q += self.window_cut(P, [(0, 0), (1, 0), (1, 1), (0, 1)], za, zb, j, "cockpit", ref, lining=True)
                    continue
                self.q.append(quad(P, UV_UNIT, "cockpit", False, ref))
        # the window frames' inner faces over the canopy: the lining's edge round the glass
        # the floor (its edges where it meets the lining) and the bulkheads
        def half_w(z):
            r = ring(params_at(z)[0] - LINING, params_at(z)[1] + LINING, params_at(z)[2] - LINING)
            for a, b in zip(r[1:7], r[2:8]):
                if (a[1] - FL) * (b[1] - FL) <= 0 and a[1] != b[1]:
                    return abs(a[0] + (b[0] - a[0]) * (FL - a[1]) / (b[1] - a[1]))
            return 3.0
        for za, zb in zip(zs, zs[1:]):
            self.face([(-half_w(za), FL, za), (half_w(za), FL, za), (half_w(zb), FL, zb), (-half_w(zb), FL, zb)], "mat", (0, 1, 0))
        self.q += fan(rin[0], -21.5, "cockpit", np.array([0, 0, 1.0]))
        # the bulkhead's face toward the flight deck, round the doorway (a quarter block forward of the bay's: the two
        # lay in one plane, and what stood on either z-fought from the other side, Justin's playtest)
        x0d, x1d, y0d, y1d = DOORWAY
        for pc in cut_convex([tuple(p_) for p_ in rin[-1]], [(x0d, x1d, y0d, y1d)]):
            for Q in fan_quads([np.array([x_, y_, BULK[0]]) for x_, y_ in pc]):
                self.q.append(quad(Q, UV_UNIT, "cockpit", False, np.array([0, 0, -1.0])))
        self.cur = "interior"                               # (the furnishings from here on: drawn only near, CrewShipRenderer)
        # (the doorway through to the bay, its door and steps: doorway())
        # the pilots' seats (Justin, 7 November 2026: "The cockpit needs work to, especially the seats"): crashworthy
        # seats on floor rails: a moulded shell (its sides shaped round the sitter), padded cushions with rounded edges,
        # bolsters, a winged headrest, an armrest, energy-absorbing struts behind, a five-point harness to a rotary buckle
        for sx in (-1, 1):
            self.pilot_seat(sx * 1.35, FL, sx)
        # the instrument panel: its face leaning back toward the pilots, five displays with keys round them, three
        # standby gauges, the warning lights along the glare shield, a head-up display's glass over it for each pilot
        pz0, pz1, py0, py1 = -18.15, -18.55, FL + 0.9, FL + 1.75
        hw = 2.9
        self.face([(-hw, py0, pz0), (hw, py0, pz0), (hw, py1, pz1), (-hw, py1, pz1)], "cockpit", (0, 0.4, 1))
        self.face([(-hw, FL, pz0 - 0.2), (hw, FL, pz0 - 0.2), (hw, py0, pz0), (-hw, py0, pz0)], "cockpit", (0, 0, 1))
        for x0 in (-hw, hw):
            self.face([(x0, FL, pz0 - 0.2), (x0, py0, pz0), (x0, py1, pz1), (x0, FL, pz1 - 0.3)], "cockpit", (np.sign(x0), 0, 0))
        nrm = np.cross(np.array([1.0, 0, 0]), np.array([0, py1 - py0, pz1 - pz0])); nrm /= np.linalg.norm(nrm)
        if nrm[2] < 0:
            nrm = -nrm
        upd = np.array([0, py1 - py0, pz1 - pz0]); upd /= np.linalg.norm(upd)
        rtd = np.array([1.0, 0, 0])
        pm = np.array([0, (py0 + py1) / 2, (pz0 + pz1) / 2]) + upd * 0.08
        def key(p, w=0.075, h=0.05):
            self.q += self.fixture(p, nrm, upd, w, h, "lens_key", depth=0.03, bezel=0.012, back=0.0, lit=False)
        for xc, scr in ((-2.05, "lens_mfd_att"), (-1.05, "lens_mfd_sys"), (0.0, "lens_mfd_map"), (1.05, "lens_mfd_radar"), (2.05, "lens_mfd_att")):
            c = pm + rtd * xc
            self.q += self.fixture(c, nrm, upd, 0.66, 0.5, scr, depth=0.06, bezel=0.12, back=0.02, lit=True)
            for kb in range(5):                                         # the keys round the display
                key(c + rtd * (-0.3 + kb * 0.15) - upd * 0.33 + nrm * 0.06)
                key(c + rtd * (-0.3 + kb * 0.15) + upd * 0.33 + nrm * 0.06)
            for kb in range(4):
                key(c + rtd * 0.42 + upd * (-0.2 + kb * 0.133) + nrm * 0.06, 0.05, 0.075)
                key(c - rtd * 0.42 + upd * (-0.2 + kb * 0.133) + nrm * 0.06, 0.05, 0.075)
        for xg in (-0.42, 0.0, 0.42):                                   # the standby gauges under the map
            self.q += self.fixture(pm + rtd * xg - upd * 0.62, nrm, upd, 0.26, 0.26, "lens_gauge", depth=0.05, bezel=0.04, back=0.02, lit=True)
        gs = [(-hw - 0.05, py1 + 0.02, pz1 - 0.1), (hw + 0.05, py1 + 0.02, pz1 - 0.1), (hw + 0.05, py1 + 0.12, pz1 + 0.55), (-hw - 0.05, py1 + 0.12, pz1 + 0.55)]
        self.face(gs, "dark", (0, 1, 0.1))
        self.face([gs[3], gs[2], (hw + 0.05, py1 - 0.06, pz1 + 0.55), (-hw - 0.05, py1 - 0.06, pz1 + 0.55)], "dark", (0, 0, 1))
        self.face([(-hw - 0.05, py1 - 0.06, pz1 + 0.55), (hw + 0.05, py1 - 0.06, pz1 + 0.55), (hw + 0.05, py1 + 0.02, pz1 - 0.1), (-hw - 0.05, py1 + 0.02, pz1 - 0.1)], "dark", (0, -1, 0))
        self.q.append(quad([(-0.7, py1 + 0.06, pz1 + 0.551), (0.7, py1 + 0.06, pz1 + 0.551), (0.7, py1 - 0.04, pz1 + 0.551), (-0.7, py1 - 0.04, pz1 + 0.551)],
                           UV1, "lens_annun", True, np.array([0, 0, 1.0])))
        for sx in (-1, 1):                                              # the head-up displays
            x = sx * 1.35
            for px in (-0.24, 0.24):
                self.bar((x + px, py1 + 0.1, pz1 + 0.3), (x + px, py1 + 0.55, pz1 + 0.18), 0.03, "dark")
            self.q.append(quad([(x - 0.26, py1 + 0.58, pz1 + 0.17), (x + 0.26, py1 + 0.58, pz1 + 0.17), (x + 0.26, py1 + 0.16, pz1 + 0.28), (x - 0.26, py1 + 0.16, pz1 + 0.28)],
                               UV1, mat("lens_hud"), True, np.array([0, 0.25, 1.0])))
            self.B((x - 0.18, py1 + 0.12, pz1 + 0.12), (x + 0.18, py1 + 0.2, pz1 + 0.4), "dark")
        # the centre console: a throttle quadrant (two levers in their slots, T-handles, a friction knob), a keypad,
        # a switch panel
        self.brick([(-0.42, FL, -18.0), (0.42, FL, -18.0), (0.42, FL, -14.4), (-0.42, FL, -14.4),
                    (-0.42, FL + 1.05, -17.9), (0.42, FL + 1.05, -17.9), (0.42, FL + 0.72, -14.4), (-0.42, FL + 0.72, -14.4)], "cockpit")
        ct = lambda z: FL + 1.05 - (FL + 1.05 - (FL + 0.72)) * (z + 17.9) / 3.5
        cn = np.array([0, 1.0, 0.094]); cn /= np.linalg.norm(cn)
        Pk = [(-0.34, ct(-17.6) + 0.02, -17.6), (0.34, ct(-17.6) + 0.02, -17.6), (0.34, ct(-16.9) + 0.02, -16.9), (-0.34, ct(-16.9) + 0.02, -16.9)]
        self.q.append(quad(Pk, UV1, "lens_keys", True, cn))
        self.B((-0.36, ct(-16.0) - 0.02, -16.75), (0.36, ct(-16.75) + 0.05, -15.75), "dark")
        for tx in (-0.14, 0.14):
            self.B((tx - 0.025, ct(-16.0) + 0.04, -16.65), (tx + 0.025, ct(-16.0) + 0.052, -15.85), "rubber")
            self.bar((tx, ct(-16.0) + 0.05, -16.15), (tx, ct(-16.0) + 0.45, -16.38), 0.045, "metal")
            self.bevel_box((tx - 0.11, ct(-16.0) + 0.43, -16.46), (tx + 0.11, ct(-16.0) + 0.53, -16.3), 0.025, "dark")
        self.prism((0.3, ct(-15.9) + 0.05, -15.9), (0.3, ct(-15.9) + 0.13, -15.9), 0.045, "metal")
        Pc = [(-0.34, ct(-15.6) + 0.02, -15.6), (0.34, ct(-15.6) + 0.02, -15.6), (0.34, ct(-14.6) + 0.02, -14.6), (-0.34, ct(-14.6) + 0.02, -14.6)]
        self.q.append(quad(Pc, UV1, "lens_cpanel", False, cn))
        # side consoles along the walls, a switch panel on each
        for sx in (-1, 1):
            x0, x1 = sorted((sx * 2.75, sx * 3.45))
            self.B((x0, FL, -17.2), (x1, FL + 0.95, -13.6), "cockpit")
            Ps = [(sx * 2.8, FL + 0.97, -17.0), (sx * 3.4, FL + 0.97, -17.0), (sx * 3.4, FL + 0.97, -13.8), (sx * 2.8, FL + 0.97, -13.8)]
            self.q.append(quad(Ps, UV1, "lens_cpanel", False, np.array([0, 1.0, 0])))
        # the overhead panel under the roof behind the canopy, map lights either side
        ry = params_at(-14.5)[2] - LINING
        self.B((-1.05, ry - 0.2, -15.7), (1.05, ry, -13.2), "cockpit")
        Po = [(-0.95, ry - 0.21, -15.6), (0.95, ry - 0.21, -15.6), (0.95, ry - 0.21, -13.3), (-0.95, ry - 0.21, -13.3)]
        self.q.append(quad(Po, UV1, "lens_cpanel", False, np.array([0, -1.0, 0])))
        for sx in (-1, 1):
            self.q += self.fixture((sx * 1.35, ry - 0.2, -14.2), (0, -1, 0), (0, 0, -1), 0.22, 0.22, "lens_red", depth=0.06, bezel=0.03, back=0.02, lit=True)

    @staticmethod
    def chin_lamp(s):
        """Where a landing lamp sits on the chin housing's sloped front (its centre, outward normal and up)."""
        a, b = np.array([0.0, G + 1.1, -20.6]), np.array([0.0, G - 0.35, -18.6])
        upl = (a - b) / np.linalg.norm(a - b)
        nl = np.cross(np.array([1.0, 0, 0]), upl)
        if nl[2] > 0:
            nl = -nl
        c = b + upl * 0.42
        return np.array([s * 0.85, c[1], c[2]]), nl, upl

    def on_skin_ahead(self, p, step=0.01):
        """The point on the hull's skin straight ahead (toward the nose) of p, and the skin's outward normal there."""
        p = np.array(p, float)
        while self.hull_inside(p) and p[2] > STATIONS[0][0]:
            p[2] -= step
        e = 0.05
        def surf_z(x, y):
            q = np.array([x, y, p[2] + 0.5])
            while self.hull_inside(q) and q[2] > STATIONS[0][0]:
                q[2] -= step
            return q[2]
        dzx = (surf_z(p[0] + e, p[1]) - surf_z(p[0] - e, p[1])) / (2 * e)
        dzy = (surf_z(p[0], p[1] + e) - surf_z(p[0], p[1] - e)) / (2 * e)
        nrm = np.array([dzx, dzy, -1.0]); nrm /= np.linalg.norm(nrm)
        return p, nrm

    @staticmethod
    def hull_inside(p):
        """Whether p is inside the hull's skin (its cross-section at p's z)."""
        x, y, z = p
        if z < STATIONS[0][0] or z > STATIONS[-1][0]:
            return False
        r = ring_at(z)
        inside = False
        for (ax, ay), (bx, by) in zip(r, r[1:] + r[:1]):
            if (ay > y) != (by > y) and x < ax + (bx - ax) * (y - ay) / (by - ay):
                inside = not inside
        return inside

    @staticmethod
    def to_hull(p):
        """p moved onto the hull's skin, straight out from (or in to) the middle of its cross-section."""
        _, Yb, Yt = params_at(p[2])
        c = np.array([0.0, (Yb + Yt) / 2])
        d = np.array([p[0], p[1]]) - c
        hit = ray_poly(ring_at(p[2]), c, math.atan2(d[1], d[0]))
        return np.array([hit[0], hit[1], p[2]])

    def clip_outside(self, i0, i1, tuck=0.03, strips=12):
        """The faces self.q[i0:i1] cut back to their parts outside the hull's skin (each tucked tuck into it), the rest
        dropped: so a part that grows out of the hull doesn't carry on through it into the rooms inside (Justin's
        playtest, 8 November 2026: the wing's root came through into the bay and the flight deck). Faces crossing the skin
        between two opposite edges are cut in strips across; the faces after i1 move up. Returns the new end."""
        kept = []
        for f in self.q[i0:i1]:
            if isinstance(f[0], str):
                kept.append(f)
                continue
            P, UV, t, g = np.asarray(f[0], float), np.asarray(f[1], float), f[2], f[3]
            ins = [self.hull_inside(p_) for p_ in P]
            if all(ins):
                continue
            if not any(ins):
                kept.append(f)
                continue
            order = None
            for rot in range(4):                           # E0 = Q0-Q3 inside, E1 = Q1-Q2 outside
                o = [(rot + k) % 4 for k in range(4)]
                if ins[o[0]] and ins[o[3]] and not ins[o[1]] and not ins[o[2]]:
                    order = o
                    break
            if order is None:                              # (a corner alone in or out: kept whole, the skin hides the rest)
                kept.append(f)
                continue
            Q, U = P[order], UV[order]
            bil = lambda u, v, A: A[0] + (A[1] - A[0]) * u + (A[3] - A[0]) * v + (A[2] - A[1] - A[3] + A[0]) * u * v
            ss = []
            for i in range(strips + 1):
                v = i / strips
                if not self.hull_inside(bil(0.0, v, Q)):
                    ss.append(0.0)
                    continue
                if self.hull_inside(bil(1.0, v, Q)):
                    ss.append(1.0)
                    continue
                lo, hi = 0.0, 1.0
                for _ in range(28):
                    mid = (lo + hi) / 2
                    if self.hull_inside(bil(mid, v, Q)):
                        lo = mid
                    else:
                        hi = mid
                ln = np.linalg.norm(bil(1.0, v, Q) - bil(0.0, v, Q)) + 1e-9
                ss.append(max(0.0, hi - tuck / ln))
            for i in range(strips):
                va, vb = i / strips, (i + 1) / strips
                if ss[i] > 1 - 1e-6 and ss[i + 1] > 1 - 1e-6:
                    continue
                pts = [(ss[i], va), (1.0, va), (1.0, vb), (ss[i + 1], vb)]
                kept.append((np.array([bil(u, v, Q) for u, v in pts]), np.array([bil(u, v, U) for u, v in pts]), t, g))
        tags = self.q.tags[i0:i1]
        tag = tags[0] if tags else self.cur
        rest, rest_tags = list(self.q[i1:]), list(self.q.tags[i1:])
        del self.q[i0:]
        del self.q.tags[i0:]
        for f in kept:
            list.append(self.q, f)
            self.q.tags.append(tag)
        end = len(self.q)
        for f, tg in zip(rest, rest_tags):
            list.append(self.q, f)
            self.q.tags.append(tg)
        return end

    def fillet(self, out_a, out_b, in_a, in_b, nf, r=0.42, m="fillet", n=8, hull_r=None):
        """A fairing filling the corner where a flat face of a part meets the hull (Justin, 7 November 2026: "make the
        parts where it goes from one angle to another look better"): across the face from its edge outside the hull
        (out_a-out_b) to its edge inside (in_a-in_b), found where it meets the skin at n+1 places along; there a quarter
        round of radius r (hull_r up the skin) curving from the face onto the hull."""
        nf = np.asarray(nf, float); nf /= np.linalg.norm(nf)
        hr = r if hull_r is None else hull_r
        rows = []
        for k in range(n + 1):
            s_ = k / n
            pa = np.asarray(out_a, float) + (np.asarray(out_b, float) - np.asarray(out_a, float)) * s_
            pb = np.asarray(in_a, float) + (np.asarray(in_b, float) - np.asarray(in_a, float)) * s_
            if self.hull_inside(pa) or not self.hull_inside(pb):
                rows.append(None)
                continue
            lo, hi = 0.0, 1.0                     # pa outside .. pb inside
            for _ in range(30):
                mid = (lo + hi) / 2
                if self.hull_inside(pa + (pb - pa) * mid):
                    hi = mid
                else:
                    lo = mid
            J = pa + (pb - pa) * lo
            out = (pa - pb) / np.linalg.norm(pa - pb)
            F = J + out * r
            H = self.to_hull(J + nf * hr)
            Cc = F + (H - J)
            arc = []
            for a in np.linspace(0, math.pi / 2, 4):
                arc.append(Cc + (F - Cc) * math.cos(a) * 0 + (J - Cc) * 0 + ((F - Cc) * math.cos(a) + (H - Cc) * math.sin(a)))
            # (a quarter round from F to H about Cc, pulled toward the corner J)
            arc = [Cc + ((F - Cc) * (1 - math.sin(a)) + (H - Cc) * (1 - math.cos(a))) for a in np.linspace(0, math.pi / 2, 4)]
            rows.append((arc, Cc - J, J, out))
        # even out the round along its length (the skin's facets make the meeting line wander)
        sm = []
        for k in range(n + 1):
            if rows[k] is None:
                sm.append(None)
                continue
            nb = [rows[j] for j in (k - 1, k + 1) if 0 <= j <= n and rows[j] is not None]
            if len(nb) < 2:
                sm.append(rows[k])
                continue
            arc = [rows[k][0][i] * 0.5 + (nb[0][0][i] + nb[1][0][i]) * 0.25 for i in range(len(rows[k][0]))]
            sm.append((arc,) + tuple(rows[k][1:]))
        rows = sm
        # The panels carry on from the hull (Justin, 7 November 2026: "line up the plating texture with the rest of the
        # aircraft"): each round's corners take the hull's own texture coordinates where it leaves the skin, and carry
        # them on over the round (and the face beyond) the way the skin's run toward the face, at the skin's density.
        sk = "skinD" if m in ("filletD", "hullD", "skinD") else "skin"
        uvs = []
        for k in range(n + 1):
            if rows[k] is None:
                uvs.append(None)
                continue
            A, ref, J, out = rows[k]
            Hh = self.to_hull(A[-1])
            uh = skin_uv(Hh)
            g = skin_uv(J) - uh
            dist = np.linalg.norm(J - Hh)
            g = g / (np.linalg.norm(g) + 1e-9) / TEX_BLOCKS["hull"] if dist > 1e-4 else np.zeros(2)
            ws = [0.0]
            for i in range(len(A) - 1, 0, -1):
                ws.append(ws[-1] + np.linalg.norm(A[i] - A[i - 1]))
            ws = ws[::-1]                                  # (walk from the hull: 0 at H, the round's length at F)
            uvs.append(([uh + g * w for w in ws], g, out))
        for k in range(n):
            if rows[k] is None or rows[k + 1] is None:
                continue
            A, ra = rows[k][0], rows[k][1]
            Bq, rb = rows[k + 1][0], rows[k + 1][1]
            ua, ub = uvs[k][0], uvs[k + 1][0]
            for j in range(len(A) - 1):
                self.q.append(quad([A[j], A[j + 1], Bq[j + 1], Bq[j]], [ua[j], ua[j + 1], ub[j + 1], ub[j]], sk, False, (ra + rb) / 2))
        valid = [k for k in range(n + 1) if rows[k] is not None]
        if not valid:
            return None
        Fs = [rows[k][0][0] for k in valid]
        UVF = [uvs[k][0][0] for k in valid]
        gs = [uvs[k][1] for k in valid]
        outs = [uvs[k][2] for k in valid]
        def uv_at(pnt):
            """The face's texture coordinates at pnt, carried on from the round's outer edge."""
            pnt = np.asarray(pnt, float)
            if len(Fs) == 1:
                k0, t = 0, 0.0
                along = np.zeros(2)
            else:
                best = None
                for i in range(len(Fs) - 1):
                    e = Fs[i + 1] - Fs[i]
                    t = (pnt - Fs[i]) @ e / (e @ e + 1e-12)
                    tc = min(1.0, max(0.0, t))
                    dd = np.linalg.norm(pnt - (Fs[i] + e * tc))
                    if best is None or dd < best[0]:
                        best = (dd, i, t)
                _, k0, t = best
            if len(Fs) > 1:
                base = UVF[k0] + (UVF[k0 + 1] - UVF[k0]) * t
                F0 = Fs[k0] + (Fs[k0 + 1] - Fs[k0]) * t
                gg = gs[k0] * (1 - min(1, max(0, t))) + gs[k0 + 1] * min(1, max(0, t))
                oo = outs[k0]
            else:
                base, F0, gg, oo = UVF[0], Fs[0], gs[0], outs[0]
            return base + gg * ((pnt - F0) @ oo)
        return uv_at

    def remap(self, i0, i1, uv_fns, m_up="skin", m_down="skinD"):
        """The faces self.q[i0:i1] (a part's sides) laid with the hull's panels carried on to them: each face takes its
        corners' coordinates from whichever of uv_fns (one per side that meets the hull, keyed by the side's normal) fits
        its normal best; faces that meet nothing keep their own."""
        for i in range(i0, i1):
            f = self.q[i]
            if isinstance(f[0], str):
                continue
            nrm = face_normal(f[0])
            best = None
            for nf, fn in uv_fns:
                if fn is None:
                    continue
                d = nrm @ (np.asarray(nf, float) / np.linalg.norm(nf))
                if d > 0.8 and (best is None or d > best[0]):
                    best = (d, fn)
            if best is None:
                continue
            name = next((k for k, v in _TEX.items() if v is f[2]), "")
            mm = m_down if name in ("hullD", "skinD") else m_up
            self.q[i] = quad(f[0], [best[1](p) for p in f[0]], mm, False, nrm)

    def prism(self, p0, p1, r, m, sides=8, caps=True):
        """An eight-sided rod from p0 to p1, capped."""
        p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
        if too_small(max(2 * r, np.linalg.norm(p1 - p0)), 2 * r):
            return
        sides = round_sides(sides, r)
        ax = p1 - p0; ax /= np.linalg.norm(ax)
        e1 = np.cross(ax, [0, 0, 1.0]) if abs(ax[2]) < 0.9 else np.cross(ax, [1.0, 0, 0])
        e1 /= np.linalg.norm(e1); e2 = np.cross(ax, e1)
        ring_ = [e1 * math.cos(2 * math.pi * k / sides + math.pi / sides) * r + e2 * math.sin(2 * math.pi * k / sides + math.pi / sides) * r for k in range(sides)]
        for k in range(sides):
            k2 = (k + 1) % sides
            self.face([p0 + ring_[k], p0 + ring_[k2], p1 + ring_[k2], p1 + ring_[k]], m, (ring_[k] + ring_[k2]) / 2, ts=1.0)
        for c, d in (((p0, -ax), (p1, ax)) if caps else ()):
            for k in range(sides):
                k2 = (k + 1) % sides
                self.face([c, c + ring_[k], c + ring_[k2], c + ring_[k2]], m, d, ts=1.0)

    def bar(self, p0, p1, w, m):
        """A square bar w across from p0 to p1."""
        self.prism(p0, p1, w * 0.7071, m, sides=4)

    def fixture(self, c, n, up, w, h, lens_name, depth=0.1, bezel=0.05, back=0.15, housing="hullD", lit=None, surf=None, faired=None):
        """A lamp: a housing from back behind c (into the hull) out to depth along n, a metal bezel round the front, the lens
        set back in it (its texture laid on corner to corner, up the way 'up' points). Returns its quads."""
        c = np.asarray(c, float); n = np.asarray(n, float); n = n / np.linalg.norm(n)
        up = np.asarray(up, float); up = up - (up @ n) * n
        if np.linalg.norm(up) < 1e-6:
            up = np.cross(n, [1.0, 0, 0])
        up = up / np.linalg.norm(up)
        rt = np.cross(up, n)
        if surf is not None:                               # turned off the surface: out far enough that no corner sinks in
            sn = np.asarray(surf, float); sn = sn / np.linalg.norm(sn)
            ns = max(n @ sn, 0.2)
            for o in (rt * (w / 2 + bezel) + up * (h / 2 + bezel), rt * (w / 2 + bezel) - up * (h / 2 + bezel),
                      -rt * (w / 2 + bezel) + up * (h / 2 + bezel), -rt * (w / 2 + bezel) - up * (h / 2 + bezel)):
                depth = max(depth, (0.07 - o @ sn) / ns)
        def rect(hw, hh, d):
            return [c - rt * hw + up * hh + n * d, c + rt * hw + up * hh + n * d, c + rt * hw - up * hh + n * d, c - rt * hw - up * hh + n * d]
        out = []
        ow, oh = w / 2 + bezel, h / 2 + bezel
        Of, Ob = rect(ow, oh, depth), rect(ow, oh, -back)
        mid = c + n * (depth - back) / 2
        if faired:
            # (Justin, 7 November 2026: "Make sure it doesn't seem like something is a block slapped on top"): the housing
            # flares out to a footprint lying on the skin, in the skin's paint, a blister grown out of the hull
            sn = n if surf is None else np.asarray(surf, float) / np.linalg.norm(surf)
            fl = 0.08 + 0.45 * depth
            foot = []
            for q_ in rect(ow + fl, oh + fl, 0.0):
                foot.append(q_ - n * ((q_ - c) @ sn) / max(n @ sn, 0.2) - sn * 0.02)
            mid = c + n * depth * 0.4
            for k in range(4):
                k2 = (k + 1) % 4
                P = [foot[k], foot[k2], Of[k2], Of[k]]
                out.append(quad(P, UV_UNIT, faired, False, np.mean(P, 0) - mid))
        else:
            for k in range(4):
                k2 = (k + 1) % 4
                P = [Ob[k], Ob[k2], Of[k2], Of[k]]
                out.append(quad(P, UV_UNIT, housing, False, np.mean(P, 0) - mid))
        If, Ib = rect(w / 2, h / 2, depth), rect(w / 2, h / 2, depth - 0.035)
        for k in range(4):
            k2 = (k + 1) % 4
            out.append(quad([Of[k], Of[k2], If[k2], If[k]], UV_UNIT, "metal", False, n))
            P = [If[k], If[k2], Ib[k2], Ib[k]]
            out.append(quad(P, UV_UNIT, "dark", False, c + n * depth - np.mean(P, 0) + n * 0.0))
        out.append(quad(Ib, UV1, lens_name, self.lights if lit is None else lit, n))
        return out

    def lamp_in(self, c, n, rt, up, w, h, lens, depth=0.08, lip=0.035, lit=None):
        """A lamp set into an opening w x h in a face (c its centre, n out, rt across, up): a thin metal lip round the
        mouth, the opening's dark walls, the lens at the back. Returns its quads."""
        c, n, rt, up = [np.asarray(v, float) for v in (c, n, rt, up)]
        H = [c - rt * w / 2 + up * h / 2, c + rt * w / 2 + up * h / 2, c + rt * w / 2 - up * h / 2, c - rt * w / 2 - up * h / 2]
        O = [c - rt * (w / 2 + lip) + up * (h / 2 + lip), c + rt * (w / 2 + lip) + up * (h / 2 + lip),
             c + rt * (w / 2 + lip) - up * (h / 2 + lip), c - rt * (w / 2 + lip) - up * (h / 2 + lip)]
        out = []
        for k in range(4):
            k2 = (k + 1) % 4
            out.append(quad([O[k] + n * 0.012, O[k2] + n * 0.012, H[k2] + n * 0.012, H[k] + n * 0.012], UV_UNIT, "metal", False, n))
            out.append(quad([H[k] + n * 0.012, H[k2] + n * 0.012, H[k2] - n * depth, H[k] - n * depth], UV_UNIT, "dark", False,
                            c - (H[k] + H[k2]) / 2))
        out.append(quad([p_ - n * (depth - 0.004) for p_ in H], UV1, lens, self.lights if lit is None else lit, n))
        return out

    def flare_bay(self, P, n, depth=0.07, frame=0.05):
        """A flare dispenser set into an opening in the skin (P its corners, P0-P1 along the ship): a thin flush lip round
        the mouth, the opening's walls going in, two magazines' faces of cartridge caps at its back, a dark bar between."""
        n = np.asarray(n, float); n = n / np.linalg.norm(n)
        P = [np.asarray(p_, float) for p_ in P]
        cen = np.mean(P, 0)
        su = (P[1] - P[0]) / np.linalg.norm(P[1] - P[0]); sv = (P[3] - P[0]) / np.linalg.norm(P[3] - P[0])
        O = [P[0] - (su + sv) * frame, P[1] + (su - sv) * frame, P[2] + (su + sv) * frame, P[3] + (sv - su) * frame]
        for k in range(4):
            k2 = (k + 1) % 4
            self.q.append(quad([O[k] + n * 0.012, O[k2] + n * 0.012, P[k2] + n * 0.012, P[k] + n * 0.012], UV_UNIT, "hullD", False, n))
            self.q.append(quad([P[k] + n * 0.012, P[k2] + n * 0.012, P[k2] - n * depth, P[k] - n * depth], UV_UNIT, "dark", False,
                               cen - (P[k] + P[k2]) / 2))
        L = np.linalg.norm(P[1] - P[0])
        gap = 0.1 / L
        for (a0, a1) in ((0.0, 0.5 - gap / 2), (0.5 + gap / 2, 1.0)):
            Q = [P[0] + (P[1] - P[0]) * a0, P[0] + (P[1] - P[0]) * a1, P[3] + (P[2] - P[3]) * a1, P[3] + (P[2] - P[3]) * a0]
            self.q.append(quad([q_ - n * depth for q_ in Q], UV1, mat("lens_flares"), False, n))
        Q = [P[0] + (P[1] - P[0]) * (0.5 - gap / 2), P[0] + (P[1] - P[0]) * (0.5 + gap / 2), P[3] + (P[2] - P[3]) * (0.5 + gap / 2),
             P[3] + (P[2] - P[3]) * (0.5 - gap / 2)]
        self.q.append(quad([q_ - n * depth for q_ in Q], UV_UNIT, "dark", False, n))
        self.q.append(quad([q_ - n * (depth - 0.03) for q_ in Q], UV_UNIT, "hullD", False, n))

    def recessed_vent(self, P, n, depth, slats, frame=0.05):
        """A vent set into an opening in the skin (P its corners, P0-P1 along its louvres, P0-P3 across; n outward): a thin
        flush lip round the mouth, the opening's walls going in, louvres slanting down across it, a dark floor."""
        n = np.asarray(n, float); n = n / np.linalg.norm(n)
        P = [np.asarray(p_, float) for p_ in P]
        cen = np.mean(P, 0)
        su = (P[1] - P[0]) / np.linalg.norm(P[1] - P[0]); sv = (P[3] - P[0]) / np.linalg.norm(P[3] - P[0])
        O = [P[0] - (su + sv) * frame, P[1] + (su - sv) * frame, P[2] + (su + sv) * frame, P[3] + (sv - su) * frame]
        for k in range(4):
            k2 = (k + 1) % 4
            self.q.append(quad([O[k] + n * 0.012, O[k2] + n * 0.012, P[k2] + n * 0.012, P[k] + n * 0.012], UV_UNIT, "hullD", False, n))
            W_ = [P[k] + n * 0.012, P[k2] + n * 0.012, P[k2] - n * depth, P[k] - n * depth]
            self.q.append(quad(W_, UV_UNIT, "dark", False, cen - (P[k] + P[k2]) / 2))
        self.q.append(quad([p_ - n * depth for p_ in P], UV_UNIT, "rubber", False, n))
        def at(u, v):
            return P[0] + (P[1] - P[0]) * u + (P[3] - P[0]) * v
        Lv = np.linalg.norm(P[3] - P[0])
        slats = max(slats, int(Lv / 0.18))
        dv = 1.0 / slats
        for i in range(slats):
            v = dv * (i + 0.85)
            A_, B2 = at(0, v) - n * 0.03, at(1, v) - n * 0.03
            C_, D_ = at(1, v - dv * 0.6) - n * (depth - 0.03), at(0, v - dv * 0.6) - n * (depth - 0.03)
            self.q.append(quad([A_, B2, C_, D_], UV1, mat("louvre"), False, n + sv * 0.8))
            self.q.append(quad([D_, C_, B2, A_], UV1, mat("louvre"), False, -n - sv * 0.8))
            self.q.append(quad([at(1, v) - n * 0.03, at(0, v) - n * 0.03, at(0, v) - n * 0.018, at(1, v) - n * 0.018], UV_UNIT, "metal", False, sv))

    def vent(self, P, n, slats=6, height=0.12, frame=0.06, paint="hull"):
        """A vent standing out of the hull on the quad P (P0-P1 along its louvres, P0-P3 across them): a raised frame, a
        dark cavity, angled louvres (Justin, 7 November 2026: "... vents, or other things"). Returns its quads."""
        n = np.asarray(n, float); n = n / np.linalg.norm(n)
        P = [np.asarray(p, float) + n * 0.005 for p in P]
        def at(u, v):
            return P[0] + (P[1] - P[0]) * u + (P[3] - P[0]) * v + (P[2] - P[1] - P[3] + P[0]) * u * v
        out = []
        height = min(height, 0.09)
        top = [p + n * height for p in P]
        cen = np.mean(P, 0)
        Lu, Lv = np.linalg.norm(P[1] - P[0]), np.linalg.norm(P[3] - P[0])
        fl = 0.16                                          # the lip slopes out this far onto the skin
        su, sv = (P[1] - P[0]) / (Lu + 1e-9), (P[3] - P[0]) / (Lv + 1e-9)
        base = [P[0] - (su + sv) * fl, P[1] + (su - sv) * fl, P[2] + (su + sv) * fl, P[3] + (sv - su) * fl]
        base = [b_ - n * 0.02 for b_ in base]
        for k in range(4):
            k2 = (k + 1) % 4
            Q = [base[k], base[k2], top[k2], top[k]]
            out.append(quad(Q, UV_UNIT, paint, False, np.mean(Q, 0) - cen + n * 0.3))
        fu, fv = min(0.3, frame / Lu), min(0.3, frame / Lv)
        It = [at(fu, fv) + n * height, at(1 - fu, fv) + n * height, at(1 - fu, 1 - fv) + n * height, at(fu, 1 - fv) + n * height]
        Ib = [p - n * (height - 0.02) for p in It]
        for k in range(4):
            k2 = (k + 1) % 4
            out.append(quad([top[k], top[k2], It[k2], It[k]], UV_UNIT, paint, False, n))
            Q = [It[k], It[k2], Ib[k2], Ib[k]]
            out.append(quad(Q, UV_UNIT, "dark", False, cen + n * height - np.mean(Q, 0)))
        out.append(quad(Ib, UV_UNIT, "rubber", False, n))
        slats = max(slats, int(Lv / 0.2))
        dv = (1 - 2 * fv) / slats
        vdir = (P[3] - P[0]) / (Lv + 1e-9)
        for i in range(slats):
            v = fv + dv * (i + 0.8)
            A, B_ = at(fu, v) + n * (height - 0.012), at(1 - fu, v) + n * (height - 0.012)
            C_, D = at(1 - fu, v - dv * 0.55) + n * 0.03, at(fu, v - dv * 0.55) + n * 0.03
            out.append(quad([A, B_, C_, D], UV1, mat("louvre"), False, n + vdir * 0.8))
            # the slat's lit top edge
            E, F_ = at(1 - fu, v) + n * (height - 0.012), at(fu, v) + n * (height - 0.012)
            out.append(quad([F_, E, E + n * 0.012, F_ + n * 0.012], UV_UNIT, "metal", False, vdir))
        return out

    def facet_quad(self, seg, z0, z1, t0, t1, side=-1):
        """The corners of a patch of the hull's facet seg (as facet places a decal) and its outward normal."""
        if side > 0:
            seg, t0, t1 = 13 - seg, 1 - t0, 1 - t1
        P = []
        for z, t in ((z0, t0), (z1, t0), (z1, t1), (z0, t1)):
            r = ring_at(z)
            a, b = np.array(r[seg]), np.array(r[(seg + 1) % 14])
            p = a + (b - a) * t
            P.append(np.array([p[0], p[1], z]))
        e = P[3] - P[0]
        nrm = np.array([e[1], -e[0], 0.0])
        mid = np.mean(P, 0)
        _, Yb, Yt = params_at(mid[2])
        if nrm @ (mid - np.array([0, (Yb + Yt) / 2, mid[2]])) < 0:
            nrm = -nrm
        return P, nrm / (np.linalg.norm(nrm) + 1e-9)

    def ramp(self):
        """The ramp (Justin, 7 November 2026): one leaf on a hinge across the bay floor's aft edge. Shut, it leans forward
        over the opening at RAMP_SHUT and is the ship's back; it swings down and aft to the ground (ramp_down 0 shut, 1
        down), no platform and no stern door over it any more. Lit along both edges of its deck and from two lamps in
        the header over the opening; red tail lights and a purple line on its back."""
        a = ramp_angle(self.ramp_down, self.ramp_reach)
        self.ramp_turn = a
        self.cur = "leaf"
        Rm = rot_m("x", a)
        for P, UV, t, gl in self.ramp_faces():
            self.q.append((np.asarray(P, float) @ Rm.T + HINGE, UV, t, gl))
        if self.lights:                                           # the deck's edge lights shine along it (beams: the header's)
            pass
        self.cur = "hull"

    def ramp_faces(self):
        """The ramp in its hinge's frame (level: along +z, its deck +RAMP_T over the hinge line)."""
        T = RAMP_T
        secs = [(RAMP_ROOT, 4.85), (0.6, RAMP_W), (4.40, RAMP_W), (6.17, 4.3), (6.49, 4.3), (6.87, 4.1), (RAMP_L, 4.1)]
        fs = []
        def F(P, m, ref, glow=False, tile=None):
            P = np.asarray(P, float)
            n = np.abs(np.cross(P[1] - P[0], P[2] - P[0]))
            ax = int(np.argmax(n)); a_, b_ = [i for i in range(3) if i != ax]
            ts = tile if tile is not None else (2.0 if m in ("hull", "hull2", "hullD", "plate") else 1.0)
            fs.append(quad(P, [(p[a_] / ts, -p[b_] / ts) for p in P], m, glow, np.asarray(ref, float)))
        def box(lo, hi, m, glow=None):
            lo, hi = np.minimum(lo, hi), np.maximum(lo, hi)
            fs.extend(box_faces(np.array(lo, float), np.array(hi, float), m, (m in GLOWS) if glow is None else glow,
                                2.0 if m in ("hull", "hull2", "hullD", "plate") else 1.0))
        for (s0, w0), (s1, w1) in zip(secs, secs[1:]):
            F([(-w0, T, s0), (w0, T, s0), (w1, T, s1), (-w1, T, s1)], "floor", (0, 1, 0))          # the deck
            for pc in cut_convex([(-w0, s0), (w0, s0), (w1, s1), (-w1, s1)], RAMP_LAMPS):        # its back, round the tail
                for Q in fan_quads([np.array([x_, -T, z_]) for x_, z_ in pc]):                    # lights' openings
                    F(Q, "hull", (0, -1, 0))
            for sx in (-1, 1):
                F([(sx * w0, T, s0), (sx * w1, T, s1), (sx * w1, -T, s1), (sx * w0, -T, s0)], "hullD", (sx, 0, 0))
                # the hazard band along the edge, the light strip inboard of it
                F([(sx * w0, T + 0.03, s0), (sx * w1, T + 0.03, s1), (sx * (w1 - 0.32), T + 0.03, s1), (sx * (w0 - 0.32), T + 0.03, s0)],
                  "hazard", (0, 1, 0), tile=0.5)
                if s0 >= 0.3:
                    # the LED strip in its channel: the lens (its LEDs every quarter block along it), a lip each side
                    ya, yb = T + 0.03, T + 0.07
                    xa0, xa1, xb0, xb1 = sx * (w0 - 0.38), sx * (w1 - 0.38), sx * (w0 - 0.54), sx * (w1 - 0.54)
                    fs.append(quad([(xa0, ya, s0), (xa1, ya, s1), (xb1, ya, s1), (xb0, ya, s0)],
                                   [(s0 / 0.25, 0), (s1 / 0.25, 0), (s1 / 0.25, 1), (s0 / 0.25, 1)], mat("led_strip"), self.lights, np.array([0, 1.0, 0])))
                    for xo0, xo1, dx in ((xa0, xa1, sx * 0.03), (xb0, xb1, -sx * 0.03)):
                        F([(xo0, ya, s0), (xo1, ya, s1), (xo1, yb, s1), (xo0, yb, s0)], "metal", (-dx, 0, 0), tile=1.0)
                        F([(xo0, yb, s0), (xo1, yb, s1), (xo1 + dx, yb, s1), (xo0 + dx, yb, s0)], "metal", (0, 1, 0), tile=1.0)
        w = secs[0][1]
        F([(-w, T, RAMP_ROOT), (w, T, RAMP_ROOT), (w, -T, RAMP_ROOT), (-w, -T, RAMP_ROOT)], "hullD", (0, 0, -1))
        w = secs[-1][1]
        F([(-w, T, RAMP_L), (w, T, RAMP_L), (w, -T, RAMP_L), (-w, -T, RAMP_L)], "hullD", (0, 0, 1))
        # the deck: cleats across it, a hazard band at the toe, the lip plate at the root
        for z in np.arange(0.9, 6.9, 0.55):
            wz = min(RAMP_W, 4.1 + (RAMP_W - 4.1) * max(0.0, min(1.0, (6.87 - z - 0.12) / (6.87 - 4.40))))
            box((-(wz - 0.75), T, z), (wz - 0.75, T + 0.05, z + 0.12), "dark")
        box((-3.9, T, RAMP_L - 0.45), (3.9, T + 0.04, RAMP_L - 0.05), "hazard")
        box((-4.7, T, RAMP_ROOT), (4.7, T + 0.035, 0.35), "metal")
        # its back (the ship's back, shut): plates, the name, red tail lights at the top corners, the purple line, the hinge
        Pw = np.array([(2.3, -T - 0.1, 5.95), (-2.3, -T - 0.1, 5.95), (-2.3, -T - 0.1, 6.6), (2.3, -T - 0.1, 6.6)])
        fs.append(quad(Pw, UV1[[2, 3, 0, 1]], wordmark(), False, np.array([0, -1.0, 0])))
        for sx in (-1, 1):                                         # (set into openings in the back, RAMP_LAMPS)
            fs.extend(self.lamp_in((sx * 3.75, -T, 6.68), (0, -1, 0), (1.0, 0, 0), (0, 0, 1), 0.36, 1.2, "lens_tail",
                                   depth=0.08))
        box((-3.0, -T - 0.07, 7.05), (3.0, -T, 7.17), "glow", glow=True)
        # the hinge's knuckles along its root (round the hinge line), the rotary actuators at its ends
        rr = lambda r: [(r * math.cos(k * math.pi / 4 + math.pi / 8), r * math.sin(k * math.pi / 4 + math.pi / 8)) for k in range(8)]
        def drum(x0, x1, r, m):
            ring_ = rr(r)
            for k in range(8):
                (y0, z0), (y1, z1) = ring_[k], ring_[(k + 1) % 8]
                F([(x0, y0, z0), (x1, y0, z0), (x1, y1, z1), (x0, y1, z1)], m, (0, (y0 + y1) / 2, (z0 + z1) / 2), tile=1.0)
            for x, d in ((x0, -1), (x1, 1)):
                for k in range(8):
                    (y0, z0), (y1, z1) = ring_[k], ring_[(k + 1) % 8]
                    F([(x, 0, 0), (x, y0, z0), (x, y1, z1), (x, y1, z1)], "dark" if m == "metal" else m, (d, 0, 0), tile=1.0)
        for xc in (-3.6, -1.2, 1.2, 3.6):
            drum(xc - 0.45, xc + 0.45, 0.28, "metal")
        for sx in (-1, 1):
            drum(min(sx * 4.55, sx * 5.05), max(sx * 4.55, sx * 5.05), 0.36, "hullD")
        return fs

    def turrets(self):
        zm = (DOOR_Z[0] + DOOR_Z[1]) / 2
        xo = SP_O["x"]
        out = xo - 1.4 + GUN_SWING * boom_out(self.side_doors)
        self.spots = {
            "door_l": ((-out, G + 2.35, zm), "left", (math.pi / 2, 0.0)), "door_r": ((out, G + 2.35, zm), "right", (-math.pi / 2, 0.0)),
            "top": ((0.0, G + 8.1, -1.0), "up", (0.0, 0.06)), "bay": (BAY_GUN, "down", (math.pi, -0.05)),
            "wing_l": ((-11.6, G + 1.12, -12.6), "down", (0.0, 0.0)), "wing_r": ((11.6, G + 1.12, -12.6), "down", (0.0, 0.0))}
        rr = lambda r: [(r * math.cos(k * math.pi / 4 + math.pi / 8), r * math.sin(k * math.pi / 4 + math.pi / 8)) for k in range(8)]
        for sd in (-1, 1):
            # The door guns' booms (Justin, 7 November 2026: "These gun mounts look like shit"; 9 November 2026: "work on
            # the side gun mounts too"): a cast boom, its long edges chamfered, three lightening pockets sunk in each
            # side, riding the bed's rails (gun_bay) on two trucks, its shoe on the power rail between them; the turret
            # ring on a flared pedestal moulded under the boom's end; the gun's power in a conduit along its top, clamped,
            # into the ring. All of it runs out with the gun.
            self.cur = "gun_l" if sd < 0 else "gun_r"
            X = lambda v, sd=sd: sd * v
            yb0, yb1 = G + 1.55, G + 2.1
            xa, xb = sorted((X(out - 3.7), X(out - 0.1)))
            self.girder(xa, xb, (yb0 + yb1) / 2, (yb1 - yb0) / 2, zm, 0.36, 0.05, "hullD",
                        pockets=[(X(out - xp), (yb0 + yb1) / 2, 0.56, 0.26) for xp in (3.1, 2.3, 1.5)], depth=0.06, m_in="dark",
                        caps=(sd > 0, sd < 0))
            for xt in (out - GUN_TRUCKS[0], out - GUN_TRUCKS[1]):             # the trucks, straddling the rails
                for zr in (zm - GUN_RAIL, zm + GUN_RAIL):
                    self.bevel_box((X(xt - 0.14), G + 1.36, zr - 0.075), (X(xt + 0.14), yb0, zr + 0.075), 0.015, "hullD")
                    for dx in (-0.07, 0.07):                                     # the wheels' hubs
                        for dz in (-1, 1):
                            self.disc((X(xt + dx), G + 1.44, zr + dz * 0.0752), (0, 0, dz), 0.034, "metal", sides=10)
                self.B((X(xt - 0.1), G + 1.5, zm - GUN_RAIL + 0.07), (X(xt + 0.1), yb0, zm + GUN_RAIL - 0.07), "hullD")
            xs_ = out - 2.1                                                     # the shoe, on the power rail
            self.B((X(xs_ - 0.07), G + 1.41, zm - 0.035), (X(xs_ + 0.07), yb0, zm + 0.035), "dark")
            self.B((X(xs_ - 0.12), G + 1.385, zm - 0.05), (X(xs_ + 0.12), G + 1.41, zm + 0.05), "metal")
            cx = X(out)
            self.rod((cx, G + 1.48, zm), (0, 1.0, 0),
                     [(0.0, 0.0)] + [(a_, 0.44 + 0.36 * (a_ / 0.57) ** 2.5) for a_ in np.linspace(0.0, 0.57, 10)] + [(0.57, 0.0)], "hullD", sides=32)
            self.rod((cx, G + 2.05, zm), (0, 1.0, 0), [(0.0, 0.0), (0.0, 0.84), (0.03, 0.88), (0.13, 0.88), (0.13, 0.85), (0.16, 0.85),
                                                       (0.16, 0.84), (0.3, 0.84), (0.3, 0.0)], "metal", sides=32)
            for k in range(16):                                                 # the ring's bolts
                a_ = 2 * math.pi * (k + 0.5) / 16
                p_ = np.array([cx + math.cos(a_) * 0.76, G + 2.35, zm + math.sin(a_) * 0.76])
                self.prism(p_, p_ + np.array([0, 0.02, 0]), 0.025, "metal", sides=6)
            zc_ = zm + 0.2                                                      # the conduit, clamped along the top
            self.prism((X(out - 3.52), yb1 - 0.04, zc_), (X(out - 3.47), yb1 + 0.035, zc_), 0.035, "rubber", sides=8)
            self.prism((X(out - 3.5), yb1 + 0.035, zc_), (X(out - 0.8), yb1 + 0.035, zc_), 0.035, "rubber", sides=8)
            for xk in np.arange(out - 3.15, out - 1.0, 0.55):
                self.rod((X(xk), yb1 + 0.035, zc_), (sd, 0, 0), [(0.0, 0.0), (0.0, 0.046), (0.04, 0.046), (0.04, 0.0)], "metal", sides=10)
                self.B((X(xk - 0.005), yb1, zc_ - 0.06), (X(xk + 0.045), yb1 + 0.012, zc_ + 0.06), "metal")
            self.cur = "hull"

    # ---------------------------------------------------------------- out
    def aim(self, name, aim):
        """A turret's yaw and pitch drawn: its aim, but the door guns turned to their stowed yaw (level) as their booms
        come in (door_gun_yaw)."""
        yaw, pitch = self.turret_aim.get(name, aim)
        if name in ("door_l", "door_r"):
            w = min(1.0, max(0.0, (1 - boom_out(self.side_doors)) / 0.2))
            yaw, pitch = door_gun_yaw(yaw, self.side_doors, -1 if name == "door_l" else 1), pitch * (1 - w)
        return yaw, pitch

    def faces(self):
        out = [f for f in self.q if not isinstance(f[0], str)]
        for name, (at, mount, aim) in self.spots.items():
            yaw, pitch = self.aim(name, aim)
            out += turret_quads(np.array(at, float), mount, yaw, pitch, self.kind(name))
        return out

    @staticmethod
    def kind(name):
        return "door" if name.startswith("door") else "wing" if name.startswith("wing") else name

    def muzzles(self):
        """The firing turrets' barrel tips (ship frame), for their flashes and bolts."""
        out = []
        for name, (at, mount, aim) in self.spots.items():
            if name in self.firing:
                yaw, pitch = self.aim(name, aim)
                out += turret_muzzles(at, mount, yaw, pitch, self.kind(name))
        return out

    def exhaust(self):
        return [(f[1], f[2]) for f in self.q if isinstance(f[0], str)]


UV_UNIT = [(0, 0), (1, 0), (1, 1), (0, 1)]
for (_gx, _gz) in GEAR_AT:                                  # the gear's wells, through the belly's flat (facets 0 and 13)
    _w = 4.34
    _v0, _v1 = (abs(_gx) - GEAR_WELL[0]) / _w, (abs(_gx) + GEAR_WELL[0]) / _w
    SKIN_HOLES.append((0, _gz - GEAR_WELL[0], _gz + GEAR_WELL[0], _v0, _v1) if _gx < 0 else (13, _gz - GEAR_WELL[0], _gz + GEAR_WELL[0], 1 - _v1, 1 - _v0))

TURRET_SCALE = {"door": 1.0, "top": 1.3, "bay": 0.85, "wing": 0.85}
BARREL = {"door": 3.0, "top": 3.0, "bay": 2.6, "wing": 2.05}
TURRET_SPREAD = {"door": 0.0, "top": 0.3, "bay": 0.0, "wing": 0.22}     # half the spacing of a turret's barrels (its muzzles')


class Kit(Ship):
    """A builder for a part apart from the ship (the guns): the ship's own shapes (rods, bevelled boxes, prisms ...), each
    face tagged with how it moves (its cur: base, yaw, pitch, spin)."""

    def __init__(self):
        self.lights, self.side_doors, self.gear = True, 1.0, 1.0
        self.cur = "base"
        self.q = QList(self)
        self.lamps = []


def turret_parts(kind):
    """A turret's faces in its own frame (model blocks; the base on y 0, the barrels along -z, the trunnion at y 0.75,
    z -0.05): (corners, uvs, texture, glow, moves) with moves 'base' (fixed), 'yaw' (turns), 'pitch' (turns and
    elevates about the trunnion), 'spin' (the door guns' barrel cluster, turning about its axis as they fire) or
    'recoil_l' / 'recoil_r' (a barrel that kicks back along itself as it fires: the left one, and the right of a pair).

    Drawn after real guns and mounts (Justin, 9 November 2026: "improve the turrets ... similarly with how you did the
    rifle checking for realistic designs", then "refine it to make it look better"): the door guns are rotary cannons,
    as a GAU-17/M134 on a remote head: six barrels in a rotor, held by a mid and a front clamp, a slotted flash hider, the
    round gun housing in a saddle cradle on the yoke's trunnions, its drive motor slung under it, the feed in a flexible
    chute from its cell; the top turret a remote weapon station laid out as a Protector is (a bolted turntable ring, a low
    armoured body with its glacis and side plates, smoke dischargers, the power pack's fins behind, the cradle between two
    cheeks, the sensor head riding the cradle's side under a hood) carrying two rail cannons (the rails' housing, its
    insulators showing, then the wound barrel in clamping bands with a coolant line, a vented field shroud at the
    muzzle); the bay gun a chin turret hung as the Apache's M230 is (a bolted ring, the azimuth drum and its gear, a yoke,
    the receiver on its trunnions, a long barrel in a finned jacket with the recoil cylinder under it and a brake at the
    muzzle, the elevation actuator ahead of the pivot, the feed chute's links curving up into the drum); the wing guns
    ball turrets after the Sperry ball (the ball on its trunnions in a yoke under the azimuth ring, ribbed and bolted
    round its waist, its armoured window framed between the barrels, an access hatch, the barrels in perforated jackets
    through slots in its face)."""
    K = Kit()
    T = np.array([0.0, 0.75, -0.05])                       # the trunnion
    L = BARREL[kind]
    ax_z = np.array([0, 0, -1.0])
    def bore(c, r):                                        # a muzzle's glowing bore
        K.disc(np.asarray(c, float) + ax_z * 0.002, ax_z, r, "glow", sides=12, glow=True)
    def band(c, z0, z1, r, m, sides=14, r0=0.0):           # a ring round an axis along -z from c
        K.rod(c, ax_z, [(z0, r0), (z0, r), (z1, r), (z1, r0)], m, sides=sides)
    def bolts(c, ax, r, n, m="metal", rb=0.025, h=0.02):
        ax = np.asarray(ax, float) / np.linalg.norm(ax)
        e1 = np.cross(ax, [0, 0, 1.0]) if abs(ax[2]) < 0.9 else np.cross(ax, [1.0, 0, 0]); e1 /= np.linalg.norm(e1)
        e2 = np.cross(ax, e1)
        for k in range(n):
            a_ = 2 * math.pi * (k + 0.5) / n
            p_ = np.asarray(c, float) + (e1 * math.cos(a_) + e2 * math.sin(a_)) * r
            K.prism(p_, p_ + ax * h, rb, m, sides=6)
    def faces():
        return [f + (t,) for f, t in zip(K.q, K.q.tags)]
    if kind == "door":
        # base: a slim bolted bearing on the boom's ring
        K.cur = "base"
        K.rod((0, 0, 0), (0, 1.0, 0), [(0.0, 0.66), (0.08, 0.66), (0.08, 0.6), (0.12, 0.6), (0.12, 0.0)], "hullD", sides=28)
        bolts((0, 0.08, 0), (0, 1.0, 0), 0.63, 12)
        # yaw: the turntable, the pedestal with its cable, the yoke's arms (lightened), the trunnion bosses
        K.cur = "yaw"
        K.rod((0, 0.12, 0), (0, 1.0, 0), [(0.0, 0.56), (0.06, 0.56), (0.08, 0.53), (0.08, 0.0)], "metal", sides=28)
        K.rod((0, 0.2, 0.06), (0, 1.0, 0), [(0.0, 0.22), (0.05, 0.22), (0.07, 0.19), (0.3, 0.16), (0.34, 0.12), (0.34, 0.0)], "hullD", sides=18)
        K.bevel_box((-0.44, 0.48, -0.2), (0.44, 0.56, 0.3), 0.02, "hullD")
        for sx in (-1, 1):
            K.brick([(sx * 0.34, 0.5, -0.2), (sx * 0.44, 0.5, -0.2), (sx * 0.44, 0.5, 0.3), (sx * 0.34, 0.5, 0.3),
                     (sx * 0.34, 0.92, -0.13), (sx * 0.44, 0.92, -0.13), (sx * 0.44, 0.92, 0.12), (sx * 0.34, 0.92, 0.12)], "hullD")
            K.face([(sx * 0.4401, 0.58, -0.11), (sx * 0.4401, 0.58, 0.19), (sx * 0.4401, 0.66, 0.15), (sx * 0.4401, 0.66, -0.09)], "dark", (sx, 0, 0), ts=1.0)
            K.rod((sx * 0.34, 0.75, -0.05), (sx, 0, 0), [(-0.02, 0.1), (0.13, 0.1), (0.13, 0.07), (0.15, 0.07), (0.15, 0.0)], "metal", sides=14)
        pts = [(0.0, 0.26, 0.27), (0.0, 0.4, 0.33), (0.08, 0.55, 0.32), (0.2, 0.62, 0.24)]
        for p0_, p1_ in zip(pts, pts[1:]):
            K.prism(p0_, p1_, 0.04, "rubber", sides=8)
        # pitch: the saddle cradle, the round gun housing in it, the drive motor slung under it, the cell and its feed
        # chute on the left, the sight on top
        K.cur = "pitch"
        for sx in (-1, 1):
            K.bevel_box((sx * 0.21 - 0.05, 0.6, -0.42), (sx * 0.21 + 0.05, 0.9, 0.24), 0.02, "hullD")
        K.bevel_box((-0.26, 0.56, -0.42), (0.26, 0.62, 0.24), 0.015, "hullD")
        c_ = T.copy()
        K.rod(c_, ax_z, [(-0.36, 0.0), (-0.36, 0.13), (-0.33, 0.165), (0.5, 0.165), (0.52, 0.15), (0.55, 0.15), (0.57, 0.0)], "paintD", sides=18)
        for zr in (-0.12, 0.2):
            band(c_, zr, zr + 0.03, 0.172, "metal", sides=18)
        K.rod(c_ + np.array([0.06, -0.21, 0.0]), ax_z, [(-0.3, 0.0), (-0.3, 0.06), (-0.27, 0.08), (0.16, 0.08), (0.19, 0.06), (0.19, 0.0)], "metal", sides=12)
        K.B((0.02, 0.54, -0.18), (0.1, 0.6, 0.12), "hullD")                                  # its mount
        K.bevel_box((-0.48, 0.6, -0.36), (-0.27, 0.86, 0.2), 0.025, "dark")                 # the cell
        for zf in (-0.28, -0.16, -0.04, 0.08):
            K.B((-0.495, 0.63, zf), (-0.48, 0.83, zf + 0.05), "metal")
        chute = [(-0.38, 0.86, -0.08), (-0.34, 0.95, -0.2), (-0.22, 0.97, -0.32), (-0.1, 0.9, -0.42)]
        for k, (p0_, p1_) in enumerate(zip(chute, chute[1:])):
            for j in range(3):
                q0 = np.asarray(p0_) + (np.asarray(p1_) - np.asarray(p0_)) * (j / 3)
                q1 = np.asarray(p0_) + (np.asarray(p1_) - np.asarray(p0_)) * ((j + 0.8) / 3)
                K.prism(q0, q1, 0.05, "metal" if (k + j) % 2 == 0 else "dark", sides=8)
        K.bevel_box((-0.09, 0.91, -0.34), (0.09, 1.04, -0.06), 0.02, "dark")              # the sight
        K.disc((0.0, 0.975, -0.341), ax_z, 0.045, "lens_cam", sides=12)
        K.B((0.05, 0.94, -0.345), (0.075, 0.96, -0.34), "red", glow=True)
        # spin: the rotor, six barrels, their coils, the clamps (each a hexagon round the cluster), the flash hider
        K.cur = "spin"
        K.rod(c_ + np.array([0, 0, -0.55]), ax_z, [(0.0, 0.0), (0.0, 0.19), (0.3, 0.19), (0.33, 0.16), (0.33, 0.0)], "dark", sides=18)
        bolts(c_ + np.array([0, 0, -0.88]), ax_z, 0.17, 6, rb=0.018, h=0.012)
        for k in range(6):
            a_ = 2 * math.pi * (k + 0.5) / 6
            cb = c_ + np.array([math.cos(a_) * 0.115, math.sin(a_) * 0.115, 0.0])
            K.rod(cb + np.array([0, 0, -0.88]), ax_z, [(0.0, 0.034), (L - 0.95, 0.032)], "metal", sides=8)
            for zc in (L - 1.42, L - 1.28):
                K.rod(cb + np.array([0, 0, -0.88]), ax_z, [(zc, 0.0), (zc, 0.044), (zc + 0.06, 0.044), (zc + 0.06, 0.0)], "glow", sides=8)
            bore(cb + np.array([0, 0, -(L + 0.04)]), 0.02)
        for zc, r in ((-1.45, 0.175), (-2.4, 0.165)):
            band(c_, -zc, -zc + 0.07, r, "hullD", sides=6)
            band(c_, -zc - 0.01, -zc + 0.08, 0.06, "metal", sides=10)
        K.rod(c_, ax_z, [(L - 0.2, 0.0), (L - 0.2, 0.17), (L + 0.04, 0.17), (L + 0.04, 0.155), (L + 0.03, 0.0)], "dark", sides=12)
        for k in range(6):                                  # the flash hider's slots
            a_ = 2 * math.pi * k / 6
            n_ = np.array([math.cos(a_), math.sin(a_), 0.0])
            e_ = np.cross(n_, ax_z)
            c0 = c_ + n_ * 0.171 + np.array([0, 0, -(L - 0.14)])
            K.face([c0 - e_ * 0.02, c0 + e_ * 0.02, c0 + e_ * 0.02 + ax_z * 0.12, c0 - e_ * 0.02 + ax_z * 0.12], "glow", n_, ts=1.0)
        return faces()
    if kind == "top":
        K.cur = "base"
        K.rod((0, 0.14, 0), (0, 1.0, 0), [(0.0, 0.98), (0.07, 0.98), (0.07, 0.0)], "metal", sides=36)
        bolts((0, 0.21, 0), (0, 1.0, 0), 0.92, 16, rb=0.025, h=0.015)
        # yaw: the armoured body (glacis, side plates proud of it), smoke dischargers, the power pack and its fins, cheeks
        K.cur = "yaw"
        K.brick([(-0.78, 0.21, -0.55), (0.78, 0.21, -0.55), (0.78, 0.21, 0.95), (-0.78, 0.21, 0.95),
                 (-0.7, 0.54, -0.22), (0.7, 0.54, -0.22), (0.7, 0.54, 0.88), (-0.7, 0.54, 0.88)], "hull2")
        for sx in (-1, 1):
            K.brick([(sx * 0.785, 0.25, -0.4), (sx * 0.81, 0.25, -0.4), (sx * 0.81, 0.25, 0.8), (sx * 0.785, 0.25, 0.8),
                     (sx * 0.705, 0.5, -0.16), (sx * 0.73, 0.5, -0.16), (sx * 0.73, 0.5, 0.76), (sx * 0.705, 0.5, 0.76)], "hullD")
            for k, zl in enumerate((0.15, 0.4)):            # the smoke dischargers, angled out and up
                d_ = np.array([sx * 0.55, 0.65, -0.2]); d_ /= np.linalg.norm(d_)
                p0_ = np.array([sx * 0.62, 0.54, zl])
                K.rod(p0_, d_, [(0.0, 0.07), (0.28, 0.07), (0.28, 0.05), (0.27, 0.05), (0.27, 0.0)], "paintD", sides=10)
            K.brick([(sx * 0.48, 0.54, -0.42), (sx * 0.66, 0.54, -0.42), (sx * 0.66, 0.54, 0.36), (sx * 0.48, 0.54, 0.36),
                     (sx * 0.48, 1.0, -0.2), (sx * 0.66, 1.0, -0.2), (sx * 0.66, 1.0, 0.16), (sx * 0.48, 1.0, 0.16)], "hullD")
            K.rod((sx * 0.66, 0.75, -0.05), (sx, 0, 0), [(0.0, 0.12), (0.04, 0.12), (0.06, 0.09), (0.06, 0.0)], "metal", sides=14)
            bolts((sx * 0.72, 0.75, -0.05), (sx, 0, 0), 0.09, 6, rb=0.012, h=0.01)
        K.bevel_box((-0.6, 0.54, 0.42), (0.6, 0.8, 0.95), 0.03, "hullD")                   # the power pack
        for xf in np.linspace(-0.5, 0.5, 9):
            K.B((xf - 0.018, 0.8, 0.48), (xf + 0.018, 0.88, 0.9), "metal")
        # pitch: the cradle (its covers bolted), the sensor head under its hood on the right
        K.cur = "pitch"
        K.bevel_box((-0.46, 0.58, -0.75), (0.46, 0.92, 0.42), 0.04, "hullD")
        for zc in (-0.55, 0.22):
            for sx in (-1, 1):
                K.prism((sx * 0.36, 0.92, zc), (sx * 0.36, 0.935, zc), 0.02, "metal", sides=6)
        K.bevel_box((0.47, 0.6, -0.58), (0.79, 0.98, -0.02), 0.035, "dark")
        for yl, rl in ((0.86, 0.07), (0.71, 0.05)):
            K.rod((0.63, yl, -0.58), ax_z, [(0.0, 0.0), (0.0, rl + 0.02), (0.03, rl + 0.02), (0.03, rl), (0.02, rl), (0.02, 0.0)], "metal", sides=14)
            K.disc((0.63, yl, -0.601), ax_z, rl, "lens_cam", sides=14)
        K.B((0.71, 0.63, -0.605), (0.75, 0.66, -0.59), "red", glow=True)
        K.bevel_box((0.46, 0.98, -0.68), (0.8, 1.02, -0.04), 0.01, "metal")
        # recoil: each rail cannon (the rails' housing with its insulators, the wound barrel in bands, a coolant line
        # along its top, the vented field shroud), kicking back as it fires
        for sx, part in ((-1, "recoil_l"), (1, "recoil_r")):
            K.cur = part
            x = sx * 0.3
            c_ = np.array([x, 0.76, 0.0])
            K.bevel_box((x - 0.12, 0.64, -1.5), (x + 0.12, 0.88, -0.7), 0.025, "hullD")
            K.B((x - 0.124, 0.748, -1.47), (x + 0.124, 0.772, -0.73), "glow", glow=True)
            for zi in np.linspace(-1.42, -0.8, 6):
                K.B((x - 0.122, 0.79, zi - 0.02), (x + 0.122, 0.83, zi + 0.02), "metal")
            K.rod(c_, ax_z, [(1.5, 0.0), (1.5, 0.13), (1.56, 0.11), (L - 0.28, 0.1), (L - 0.28, 0.0)], "paintD", sides=16)
            for zb in np.linspace(1.75, L - 0.5, 4):
                band(c_, zb, zb + 0.08, 0.125, "metal", sides=16)
            K.prism((x, 0.885, -1.0), (x, 0.885, -(L - 0.32)), 0.02, "metal", sides=6)
            K.rod(c_, ax_z, [(L - 0.33, 0.0), (L - 0.33, 0.13), (L - 0.28, 0.15), (L + 0.06, 0.155), (L + 0.08, 0.14), (L + 0.08, 0.0)], "hullD", sides=16)
            for k in range(8):
                a_ = 2 * math.pi * k / 8
                n_ = np.array([math.cos(a_), math.sin(a_), 0.0])
                e_ = np.cross(n_, ax_z)
                c0 = c_ + n_ * 0.153 + np.array([0, 0, -(L - 0.2)])
                K.face([c0 - e_ * 0.025, c0 + e_ * 0.025, c0 + e_ * 0.025 + ax_z * 0.16, c0 - e_ * 0.025 + ax_z * 0.16], "glow", n_, ts=1.0)
            bore(c_ + np.array([0, 0, -(L + 0.082)]), 0.085)
        return faces()
    if kind == "bay":
        K.cur = "base"
        K.rod((0, 0, 0), (0, 1.0, 0), [(0.0, 0.82), (0.1, 0.82), (0.1, 0.7), (0.13, 0.7), (0.13, 0.0)], "hullD", sides=32)
        bolts((0, 0.1, 0), (0, 1.0, 0), 0.76, 12, rb=0.028, h=0.025)
        # yaw: the azimuth drum and its gear ring, the yoke's arms curving down, the trunnion bosses
        K.cur = "yaw"
        K.rod((0, 0.13, 0), (0, 1.0, 0), [(0.0, 0.6), (0.05, 0.6), (0.05, 0.64), (0.1, 0.64), (0.1, 0.58), (0.34, 0.57), (0.4, 0.5), (0.42, 0.42), (0.42, 0.0)],
              "hullD", sides=32)
        for k in range(24):                                  # the gear's teeth
            a_ = 2 * math.pi * k / 24
            K.B((math.cos(a_) * 0.64 - 0.015, 0.18, math.sin(a_) * 0.64 - 0.015), (math.cos(a_) * 0.64 + 0.015, 0.23, math.sin(a_) * 0.64 + 0.015), "metal")
        for sx in (-1, 1):
            K.brick([(sx * 0.28, 0.48, -0.34), (sx * 0.42, 0.48, -0.34), (sx * 0.42, 0.48, 0.28), (sx * 0.28, 0.48, 0.28),
                     (sx * 0.3, 0.66, -0.26), (sx * 0.42, 0.66, -0.26), (sx * 0.42, 0.66, 0.18), (sx * 0.3, 0.66, 0.18)], "hullD")
            K.brick([(sx * 0.3, 0.66, -0.26), (sx * 0.42, 0.66, -0.26), (sx * 0.42, 0.66, 0.18), (sx * 0.3, 0.66, 0.18),
                     (sx * 0.3, 0.88, -0.15), (sx * 0.42, 0.88, -0.15), (sx * 0.42, 0.88, 0.05), (sx * 0.3, 0.88, 0.05)], "hullD")
            K.rod((sx * 0.42, 0.75, -0.05), (sx, 0, 0), [(0.0, 0.1), (0.04, 0.1), (0.05, 0.075), (0.05, 0.0)], "metal", sides=12)
        # pitch: the receiver (a feed port on its left), the actuator ahead of the pivot, the feed's links, the sight
        K.cur = "pitch"
        K.bevel_box((-0.17, 0.6, -0.6), (0.17, 0.92, 0.55), 0.035, "hullD")
        K.bevel_box((-0.22, 0.66, -0.1), (-0.16, 0.86, 0.25), 0.015, "metal")
        K.prism((0.0, 0.6, -0.78), (0.0, 0.44, -0.24), 0.045, "chrome", sides=8)
        K.prism((0.0, 0.47, -0.35), (0.0, 0.38, -0.08), 0.075, "hullD", sides=10)
        chute = [np.array([-0.22, 0.76, 0.08]), np.array([-0.34, 0.66, 0.16]), np.array([-0.38, 0.5, 0.12]), np.array([-0.32, 0.38, 0.0])]
        for k, (p0_, p1_) in enumerate(zip(chute, chute[1:])):
            for j in range(3):
                q0 = p0_ + (p1_ - p0_) * (j / 3); q1 = p0_ + (p1_ - p0_) * ((j + 0.8) / 3)
                K.prism(q0, q1, 0.07, "metal" if (k + j) % 2 == 0 else "dark", sides=8)
        K.bevel_box((-0.09, 0.92, -0.52), (0.09, 1.02, -0.24), 0.02, "dark")
        K.disc((0.0, 0.97, -0.521), ax_z, 0.035, "lens_cam", sides=10)
        # recoil: the barrel in its finned jacket, the recoil cylinder under it, the muzzle brake
        K.cur = "recoil_l"
        c_ = np.array([0.0, 0.76, 0.0])
        K.rod(c_, ax_z, [(0.6, 0.0), (0.6, 0.12), (1.38, 0.12), (1.42, 0.085), (1.42, 0.0)], "dark", sides=16)
        for zf in np.linspace(0.68, 1.28, 7):
            band(c_, zf, zf + 0.03, 0.15, "metal", sides=16)
        K.rod(c_, ax_z, [(1.42, 0.08), (L - 0.1, 0.07)], "metal", sides=12)
        K.rod(c_ + np.array([0, -0.15, 0]), ax_z, [(0.6, 0.0), (0.6, 0.045), (1.6, 0.045), (1.62, 0.03), (1.62, 0.0)], "chrome", sides=10)
        K.B((-0.03, 0.6, -1.5), (0.03, 0.66, -1.42), "dark")
        band(c_, L - 0.42, L - 0.32, 0.1, "metal", sides=12)
        band(c_, L - 0.38, L - 0.35, 0.105, "glow", sides=12)
        K.bevel_box((-0.12, 0.64, -(L + 0.16)), (0.12, 0.88, -(L - 0.12)), 0.025, "dark")
        for sx in (-1, 1):
            for zs_ in (-(L + 0.08), -(L - 0.02)):
                K.B((sx * 0.12, 0.7, zs_ - 0.03), (sx * 0.122, 0.82, zs_ + 0.03), "glow", glow=True)
        bore(c_ + np.array([0, 0, -(L + 0.162)]), 0.05)
        return faces()
    # the wing's ball turret
    K.cur = "base"
    K.rod((0, 0, 0), (0, 1.0, 0), [(0.0, 0.6), (0.1, 0.6), (0.1, 0.52), (0.12, 0.52), (0.12, 0.0)], "hullD", sides=28)
    bolts((0, 0.1, 0), (0, 1.0, 0), 0.56, 10, rb=0.022, h=0.02)
    K.cur = "yaw"
    K.rod((0, 0.12, 0), (0, 1.0, 0), [(0.0, 0.47), (0.04, 0.47), (0.04, 0.5), (0.08, 0.5), (0.08, 0.4), (0.16, 0.4), (0.16, 0.0)], "metal", sides=28)
    for sx in (-1, 1):                                                     # the yoke's arms, curving round the ball
        K.brick([(sx * 0.26, 0.28, -0.18), (sx * 0.36, 0.28, -0.18), (sx * 0.36, 0.28, 0.14), (sx * 0.26, 0.28, 0.14),
                 (sx * 0.47, 0.52, -0.16), (sx * 0.56, 0.52, -0.16), (sx * 0.56, 0.52, 0.1), (sx * 0.47, 0.52, 0.1)], "hullD")
        K.brick([(sx * 0.47, 0.52, -0.16), (sx * 0.56, 0.52, -0.16), (sx * 0.56, 0.52, 0.1), (sx * 0.47, 0.52, 0.1),
                 (sx * 0.52, 0.88, -0.14), (sx * 0.6, 0.88, -0.14), (sx * 0.6, 0.88, 0.04), (sx * 0.52, 0.88, 0.04)], "hullD")
        K.rod((sx * 0.52, 0.75, -0.05), (sx, 0, 0), [(-0.06, 0.11), (0.08, 0.11), (0.1, 0.08), (0.1, 0.0)], "metal", sides=14)
    K.cur = "pitch"
    R = 0.47
    prof = [(a_, math.sqrt(max(R * R - a_ * a_, 0.0))) for a_ in np.linspace(-R, R, 15)]
    K.rod(T, (1.0, 0, 0), prof, "hull2", sides=28)                                                                   # the ball
    K.rod(T, (1.0, 0, 0), [(-0.035, 0.0), (-0.035, R + 0.014), (0.035, R + 0.014), (0.035, 0.0)], "hullD", sides=28)  # its rib
    K.rod(T, (0, 0, 1.0), [(-0.03, 0.0), (-0.03, R + 0.012), (0.03, R + 0.012), (0.03, 0.0)], "hullD", sides=28)    # its waist
    for k in range(16):                                                                                              # bolted
        a_ = 2 * math.pi * (k + 0.5) / 16
        p_ = T + np.array([math.cos(a_), math.sin(a_), 0.0]) * (R + 0.012)
        K.prism(p_ + np.array([0, 0, 0.032]), p_ + np.array([0, 0, 0.045]), 0.014, "metal", sides=6)
    wd = np.array([0, 0.48, -0.88]); wd /= np.linalg.norm(wd)
    wc = T + wd * (R + 0.002)
    K.rod(wc - wd * 0.03, wd, [(0.0, 0.17), (0.035, 0.17), (0.035, 0.13), (0.03, 0.13), (0.03, 0.0)], "metal", sides=18)   # the window's frame
    K.disc(wc + wd * 0.031, wd, 0.13, "lens_cam", sides=18)
    hd = np.array([0, 0.55, 0.84]); hd /= np.linalg.norm(hd)
    K.rod(T + hd * (R - 0.02), hd, [(0.0, 0.2), (0.03, 0.2), (0.03, 0.17), (0.034, 0.17), (0.034, 0.0)], "hullD", sides=18)   # the hatch
    for sx, part in ((-1, "recoil_l"), (1, "recoil_r")):                                                           # the barrels
        K.cur = "pitch"
        c_ = np.array([sx * 0.22, 0.76, -0.05])
        K.bevel_box((sx * 0.22 - 0.085, 0.675, -0.05 - R * 0.95), (sx * 0.22 + 0.085, 0.845, -0.05 - R * 0.8), 0.02, "dark")
        K.cur = part
        K.rod(c_, ax_z, [(0.42, 0.0), (0.42, 0.072), (0.95, 0.072), (0.97, 0.045), (L - 0.12, 0.04)], "dark", sides=12)
        for zj in np.linspace(0.5, 0.88, 6):
            band(c_, zj, zj + 0.03, 0.077, "metal", sides=12)
        for zc in (L - 0.65, L - 0.5):
            band(c_, zc, zc + 0.06, 0.055, "glow", sides=10)
        K.rod(c_, ax_z, [(L - 0.15, 0.0), (L - 0.15, 0.06), (L + 0.04, 0.075), (L + 0.08, 0.075), (L + 0.08, 0.0)], "dark", sides=12)
        bore(c_ + np.array([0, 0, -(L + 0.082)]), 0.04)
    return faces()


def brick_quads(C, m):
    C = [np.asarray(c, float) for c in C]
    cen = np.mean(C, 0)
    out = []
    for idx in ((0, 1, 2, 3), (4, 5, 6, 7), (0, 1, 5, 4), (1, 2, 6, 5), (2, 3, 7, 6), (3, 0, 4, 7)):
        P = [C[i] for i in idx]
        n = np.abs(np.cross(P[1] - P[0], P[2] - P[0]))
        ax = int(np.argmax(n)); a, b = [i for i in range(3) if i != ax]
        out.append(quad(P, [(p[a] * 2, -p[b] * 2) for p in P], m, m in GLOWS, np.mean(P, 0) - cen))
    return out


def turret_quads(at, mount, yaw, pitch, kind, spin=0.0):
    """The turret's quads in the ship frame: hung under (down) or stood on (up, left, right)."""
    k = TURRET_SCALE[kind]
    Ry, Rx, Rz = rot_m("y", yaw), rot_m("x", pitch), rot_m("z", spin)
    flip = np.array([1, -1, 1]) if mount == "down" else np.array([1, 1, 1])
    pv = np.array([0, 0.75, -0.05]) * k * flip
    out = []
    for P, UV, t, g, mv in turret_parts(kind):
        P = np.asarray(P, float) * k * flip
        if mount == "down":                                # (mirrored: the winding turned back)
            P, UV = P[::-1].copy(), np.asarray(UV)[::-1].copy()
        if mv == "spin":
            P = (P - pv) @ Rz.T + pv
        if mv in ("pitch", "spin", "recoil_l", "recoil_r"):
            P = (P - pv) @ Rx.T + pv
        if mv != "base":
            P = P @ Ry.T
        out.append((P + np.asarray(at, float), UV, t, g))
    return out


def turret_muzzles(at, mount, yaw, pitch, kind):
    k = TURRET_SCALE[kind]
    Ry, Rx = rot_m("y", yaw), rot_m("x", pitch)
    sy = -1 if mount == "down" else 1
    piv = np.array([0, 0.75 * sy, -0.05]) * k
    out = []
    for sx in (-1, 1):
        c = np.array([sx * TURRET_SPREAD[kind], 0.76 * sy, -BARREL[kind] - 0.2]) * k
        out.append((((c - piv) @ Rx.T + piv) @ Ry.T) + np.asarray(at, float))
    return out


def rot_m(ax, a):
    c, s = math.cos(a), math.sin(a)
    return {"x": np.array([[1, 0, 0], [0, c, -s], [0, s, c]]), "y": np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]]),
            "z": np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])}[ax]


def ring_quads(outer, inner):
    n = len(outer)
    return [((outer[k][0], outer[k][1]), (outer[(k + 1) % n][0], outer[(k + 1) % n][1]), (inner[(k + 1) % n][0], inner[(k + 1) % n][1]),
             (inner[k][0], inner[k][1])) for k in range(n)]


def fan_tris(poly):
    n = len(poly)
    return [((0.0, 0.0), poly[k], poly[(k + 1) % n], poly[(k + 1) % n]) for k in range(n)]




# ---------------------------------------------------------------------------------------------- the export
OUT = "src/main/resources/assets/jugcraft"
DATA_OUT = "src/main/resources/data/jugcraft/crew_ship"          # the hull's collision cells (tower/crew/CrewShipHull)
MOUNTS = {"up": 0, "down": 1, "left": 2, "right": 3}


def _name_of(t):
    for k, v in _TEX.items():
        if v is t:
            return k
    raise ValueError("a texture with no name")


def _utf(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def world_uv(P, name, n):
    """A face's texture at its true size (Justin, 6 November 2026: "the texture ... looks super stretched across"): laid
    flat on the face's own plane, one repeat to TEX_BLOCKS[name] blocks, its v running up the face (along the ship on a
    level one), anchored to the part's frame so faces in one plane carry on one pattern."""
    vax = np.array([0, 0, -1.0]) if abs(n[1]) > 0.92 else np.array([0, 1.0, 0])
    vax = vax - (vax @ n) * n
    vax /= np.linalg.norm(vax) + 1e-12
    uax = np.cross(vax, n)
    k = TEX_BLOCKS.get(name, 1.0)
    return np.stack([P @ uax / k, -(P @ vax) / k], 1)


def _quad_bytes(P, UV, t, glow, mats, vn=None):
    """A quad for the game: its material, glow, and its four corners turned to the game's winding, each with its texture
    coordinates and its normal (the face's, or for a face shaded smooth its corner's own, vn)."""
    P = np.asarray(P, float); UV = np.asarray(UV, float)
    n = -np.cross(P[1] - P[0], P[2] - P[0])
    if np.linalg.norm(n) < 1e-12:
        n = -np.cross(P[2] - P[0], P[3] - P[0])
    n = n / (np.linalg.norm(n) + 1e-12)
    name = _name_of(t)
    if not local_uv(name):
        UV = world_uv(P, name, n)
    if name not in mats:
        mats.append(name)
    out = struct.pack(">BB", mats.index(name), 1 if glow else 0)
    for i in (3, 2, 1, 0):
        v = n if vn is None else vn[i]
        out += struct.pack(">5f3b", P[i][0], P[i][1], P[i][2], UV[i][0], UV[i][1], *[int(round(c * 127)) for c in v])
    return out


_HOLES = {}


def _opaque(name):
    """Whether a material hides what's behind it everywhere (not see-through, no cut-out holes in its texture)."""
    if name not in _HOLES:
        t = _TEX.get(name)
        if t is None:
            t = mat(name)
        _HOLES[name] = name in TRANSLUCENT or bool((np.asarray(t)[..., 3] < 0.5).any())
    return not _HOLES[name]


def hidden_faces(faces, names, groups=None):
    """Which faces can never be seen (Justin, 9 November 2026: "optimize what you can"): a face lying in the plane of an
    opaque face turned the other way that covers it (a box's face against a wall or a floor, a fitting's back on what
    it's fixed to: seen only from behind that face, where it's hollow), and the second of any two faces exactly alike.
    groups: each face's group, a face covered only by one in its own group (parts that move apart can't cover each
    other). A set of indices."""
    groups = groups or [0] * len(faces)
    out = set()
    planes = {}
    info = []
    for i, f in enumerate(faces):
        P = np.asarray(f[0], float)
        n = face_normal(P)
        d = float(n @ P[0])
        key = (tuple(np.round(n, 3)), round(d, 3))
        info.append((P, n, key))
        planes.setdefault(key, []).append(i)
    seen = {}
    for i, (P, n, key) in enumerate(info):
        dup = (key, tuple(sorted(tuple(np.round(p_, 4)) for p_ in P)), names[i], groups[i])
        if dup in seen:
            out.add(i)
        else:
            seen[dup] = i
    def basis(n):
        e1 = np.cross(n, [0, 1.0, 0]) if abs(n[1]) < 0.9 else np.cross(n, [1.0, 0, 0])
        e1 /= np.linalg.norm(e1)
        return e1, np.cross(n, e1)
    for key, idx in planes.items():
        if not any(key[0]):
            continue
        back = planes.get((tuple(-np.array(key[0]) + 0.0), round(-key[1], 3)))
        if not back:
            continue
        covers = [j for j in back if _opaque(names[j])]
        if not covers:
            continue
        n = np.array(key[0], float); n /= np.linalg.norm(n)
        e1, e2 = basis(n)
        cov2 = []
        for j in covers:
            Q = info[j][0]
            q2 = [np.array([q_ @ e1, q_ @ e2]) for q_ in Q]
            uq = [q2[0]] + [q_ for k_, q_ in enumerate(q2[1:], 1) if np.linalg.norm(q_ - q2[k_ - 1]) > 1e-9]
            if len(uq) > 2 and np.linalg.norm(uq[0] - uq[-1]) < 1e-9:
                uq.pop()
            if len(uq) < 3:
                continue
            area = sum(uq[k_][0] * uq[(k_ + 1) % len(uq)][1] - uq[(k_ + 1) % len(uq)][0] * uq[k_][1] for k_ in range(len(uq)))
            cov2.append((uq, 1.0 if area > 0 else -1.0, np.min(uq, 0), np.max(uq, 0), abs(area) / 2, groups[j]))
        for i in idx:
            if i in out:
                continue
            p2 = [np.array([p_ @ e1, p_ @ e2]) for p_ in info[i][0]]
            lo_, hi_ = np.min(p2, 0), np.max(p2, 0)
            a_i = abs(sum(p2[k_][0] * p2[(k_ + 1) % 4][1] - p2[(k_ + 1) % 4][0] * p2[k_][1] for k_ in range(4))) / 2
            for uq, sg, clo, chi, a_c, g_c in cov2:
                if g_c != groups[i] or (lo_ < clo - 1e-4).any() or (hi_ > chi + 1e-4).any():
                    continue
                if abs(a_c - a_i) < 1e-5:                 # the two sides of one panel, seen from both: both stay
                    continue
                if all(sg * ((uq[(k_ + 1) % len(uq)][0] - uq[k_][0]) * (pt[1] - uq[k_][1]) - (uq[(k_ + 1) % len(uq)][1] - uq[k_][1]) * (pt[0] - uq[k_][0])) >= -1e-5
                       for pt in p2 for k_ in range(len(uq))):
                    out.add(i)
                    break
    return out


def _ship_groups(ship, mats, suffix=""):
    """A built ship's quads by part, each in its part's frame about its pivot, with their corners' normals (worked out
    over the whole ship as it stands, so the parts' smooth faces meet smoothly), turned into the part's frame."""
    pivots = {"hull": (0, 0, 0), "interior": (0, 0, 0), "gear_s1": (0, 0, 0), "gear_s2": (0, 0, 0), "gear_s3": (0, 0, 0), "gear_s4": (0, 0, 0), "gear_s5": (0, 0, 0),
              "gear_door_a": (0, 0, 0), "gear_door_b": (0, 0, 0), "fdoor": (0, 0, 0), "door_l": (0, 0, 0), "door_r": (0, 0, 0),
              "gun_l": (0, 0, 0), "gun_r": (0, 0, 0), "thruster_l": PODS[0], "thruster_r": PODS[1], "leaf": tuple(HINGE)}
    faces, tags = [], []
    for f, tag in zip(ship.q, ship.q.tags):
        if not isinstance(f[0], str):
            faces.append(f)
            tags.append(tag)
    names = [_name_of(f[2]) for f in faces]
    gone = hidden_faces(faces, names, ["static" if t in ("hull", "interior") else t for t in tags])
    faces = [f for i, f in enumerate(faces) if i not in gone]
    tags = [t for i, t in enumerate(tags) if i not in gone]
    names = [n for i, n in enumerate(names) if i not in gone]
    vns = vertex_normals(faces, names)
    Rleaf = rot_m("x", ship.ramp_turn)          # the shut leaf back to its own frame (level, along +z from the hinge)
    groups = {}
    for (P, UV, t, g), tag, vn in zip(faces, tags, vns):
        P = np.asarray(P, float)
        piv = np.array(pivots[tag], float)
        L = P - piv
        if tag == "leaf":
            L = L @ Rleaf
            vn = None if vn is None else np.asarray(vn) @ Rleaf
        groups.setdefault(tag + suffix, []).append(_quad_bytes(L, UV, t, g, mats, vn))
    return groups, {k + suffix: v for k, v in pivots.items()}


def export():
    """The ship for the game (format 2): its parts near and far (FAR), the guns' models, the lamps, the guns' spots, its
    collision cells and its textures."""
    global FAR
    mats = []
    pose = dict(ramp_out=0.0, ramp_down=0.0, tilt=0.0, thrust=0.0, gear=1.0, door=0.0, side_doors=0.0, lights=True)
    FAR = False
    ship = Ship(**pose)
    groups, pivots = _ship_groups(ship, mats)
    FAR = True
    far, fpiv = _ship_groups(Ship(**pose), mats, "~far")
    FAR = False
    far.pop("interior~far", None)
    groups.update(far)
    pivots.update(fpiv)
    # the guns: each kind as its own model, in its own frame (the base on y 0, the barrels along -z)
    # (each kind's base, yaw and pitch parts, and the door guns' spin: the barrel cluster, about its axis through the
    # trunnion, along the barrels; its pivot the trunnion's)
    # (and each again as a coarser far model, "~far": "optimize what you can", 9 November 2026)
    for kind, mount in (("door", "up"), ("top", "up"), ("bay", "down"), ("wing", "down")):
        k = TURRET_SCALE[kind]
        flip = np.array([1, -1, 1]) if mount == "down" else np.array([1, 1, 1])
        pv = np.array([0, 0.75, -0.05]) * k * flip
        for far_, sfx in ((False, ""), (True, "~far")):
            FAR = far_
            parts = turret_parts(kind)
            FAR = False
            tfaces = []
            for P, UV, t, g, mv in parts:
                P = np.asarray(P, float) * k * flip
                if mount == "down":
                    P, UV = P[::-1].copy(), np.asarray(UV)[::-1].copy()
                tfaces.append(((P, UV, t, g), mv))
            names_ = [_name_of(f[2]) for f, _ in tfaces]
            gone = hidden_faces([f for f, _ in tfaces], names_, [mv for _, mv in tfaces])
            tfaces = [x for i, x in enumerate(tfaces) if i not in gone]
            names_ = [n for i, n in enumerate(names_) if i not in gone]
            vns_ = vertex_normals([f for f, _ in tfaces], names_)
            for mv in ("base", "yaw", "pitch", "spin", "recoil_l", "recoil_r"):
                moving = mv not in ("base", "yaw")
                qs = [_quad_bytes(f[0] - (pv if moving else 0), f[1], f[2], f[3], mats, vn)
                      for (f, m_), vn in zip(tfaces, vns_) if m_ == mv]
                if not qs:
                    continue
                name = f"turret_{kind}_{mv}{sfx}"
                groups[name] = qs
                pivots[name] = tuple(pv) if moving else (0.0, 0.0, 0.0)
    body = b"JCS1" + struct.pack(">i", 2)
    body += struct.pack(">i", len(mats)) + b"".join(_utf(m) for m in mats)
    body += struct.pack(">i", len(groups))
    for name, qs in groups.items():
        body += _utf(name) + struct.pack(">3f", *pivots[name]) + struct.pack(">i", len(qs)) + b"".join(qs)
    lamps = Ship(lights=True).lamps
    body += struct.pack(">i", len(lamps)) + b"".join(struct.pack(">6f", *p, *(d / np.linalg.norm(d))) for p, d in lamps)
    spots = ship.spots
    body += struct.pack(">i", len(spots))
    for name, (at, mount, aim) in spots.items():
        body += _utf(name) + struct.pack(">3fB2f", *at, MOUNTS[mount], *aim)
    cells = collision_cells()
    # (every file through tools/keep_same.py: rewritten only when what it holds has changed)
    import keep_same
    keep_same.gz(struct.pack(">fi", COLLIDE, len(cells)) + b"".join(struct.pack(">3h", *c) for c in cells), f"{DATA_OUT}/collision.bin.gz")
    keep_same.gz(body, f"{OUT}/crew_ship/ship.bin.gz")
    for m in mats:
        arr = wordmark() if m == "word" else _TEX[m] if m.startswith(("st_", "mdl_")) else mat(m)
        keep_same.png(Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA"), f"{OUT}/textures/entity/crew_ship/{m}.png")
    keep_same.png(Image.fromarray((flame_texture() * 255).astype(np.uint8), "RGBA"), f"{OUT}/textures/entity/crew_ship/flame.png")
    near = sum(len(q) for k_, q in groups.items() if not k_.endswith("~far") and not k_.startswith("turret_"))
    farq = sum(len(q) for k_, q in groups.items() if k_.endswith("~far"))
    print(f"crew ship: {len(groups)} parts; near {near} quads (interior {len(groups.get('interior', []))}), far {farq}; "
          f"{len(mats)} materials: {', '.join(mats)}; {len(cells)} collision cells")
    for name, qs in groups.items():
        print(f"  {name}: {len(qs)}")
    print("lamps:")
    for p_, d_ in lamps:
        d_ = d_ / np.linalg.norm(d_)
        print("  {%.2fF, %.2fF, %.2fF, %.2fF, %.2fF, %.2fF}," % (p_[0], p_[1], p_[2], d_[0], d_[1], d_[2]))
    print("spots:", {k_: (tuple(round(float(c), 3) for c in v[0]), v[1], v[2]) for k_, v in spots.items()})


COLLIDE = 0.25


def collision_cells():
    """The landed ship's shell as cells of COLLIDE blocks (ship frame), for the game's collisions (Justin, 6 November 2026:
    "when I fly into the ship, I'm just flying through it"): the hull, the gear down, the thrusters straight down, the
    the side doors open. The ramp is the game's own (its slope is the ground's, CrewShipHull), and
    the guns are left out (players pass their barrels)."""
    global COLLIDING
    COLLIDING = True
    try:
        ship = Ship(ramp_out=1.0, ramp_down=0.0, tilt=1.0, thrust=0.0, gear=1.0, door=1.0, side_doors=1.0, lights=True)
    finally:
        COLLIDING = False
    cells = set()
    for f, tag in zip(ship.q, ship.q.tags):
        if isinstance(f[0], str) or tag in ("leaf", "gun_l", "gun_r", "fdoor"):     # (the flight deck's door slides aside for anyone)
            continue
        P = np.asarray(f[0], float)
        la = max(np.linalg.norm(P[1] - P[0]), np.linalg.norm(P[2] - P[3]))
        lb = max(np.linalg.norm(P[3] - P[0]), np.linalg.norm(P[2] - P[1]))
        nu, nv = int(la / 0.08) + 2, int(lb / 0.08) + 2
        u = np.linspace(0, 1, nu)[:, None, None]
        v = np.linspace(0, 1, nv)[None, :, None]
        pts = (P[0] * (1 - u) * (1 - v) + P[1] * u * (1 - v) + P[2] * u * v + P[3] * (1 - u) * v).reshape(-1, 3)
        for c in np.unique(np.floor(pts / COLLIDE).astype(int), axis=0):
            cells.add(tuple(int(x) for x in c))
    return sorted(cells)


def ring_texture():
    """The keep-out ring's band (textures/entity/crew_ship/zone_ring.png): translucent red with bright edges, STAY BACK
    in black letters along it (repeated round the ring; drawn emissive and translucent, a hologram on the ground)."""
    w, h = 512, 32
    im = Image.new("RGBA", (w, h), (225, 30, 30, 170))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, w, 2), fill=(255, 70, 60, 245))
    d.rectangle((0, h - 3, w, h), fill=(255, 70, 60, 245))
    d.rectangle((0, 5, w, 5), fill=(255, 110, 100, 200))
    d.rectangle((0, h - 6, w, h - 6), fill=(255, 110, 100, 200))
    f = ImageFont.truetype(FONT, 19)
    for k in range(2):
        cx = w * (k + 0.5) / 2
        d.text((cx, h / 2 + 1), "STAY BACK", font=f, fill=(8, 0, 0, 250), anchor="mm")
        d.polygon([(cx + w / 4 - 5, h / 2), (cx + w / 4, h / 2 - 5), (cx + w / 4 + 5, h / 2), (cx + w / 4, h / 2 + 5)], fill=(8, 0, 0, 250))
    im.save(f"{OUT}/textures/entity/crew_ship/zone_ring.png")


def item_icon():
    """Drop Ship (Landed)'s icon: the ship from above, nose up (textures/item/crew_drop_ship.png)."""
    s = 2
    im = Image.new("RGBA", (16 * s, 16 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    P = lambda pts: [(x * s, y * s) for x, y in pts]
    d.polygon(P([(2.5, 7), (13.5, 7), (12, 9.5), (4, 9.5)]), fill=(70, 72, 82, 255), outline=(30, 30, 36, 255))   # the wings
    d.polygon(P([(6.5, 1), (9.5, 1), (10.5, 4), (10.5, 14), (5.5, 14), (5.5, 4)]), fill=(96, 98, 110, 255), outline=(30, 30, 36, 255))
    d.rectangle(P([(4.2, 6), (5.4, 11)]), fill=(66, 67, 76, 255)); d.rectangle(P([(10.6, 6), (11.8, 11)]), fill=(66, 67, 76, 255))
    d.rectangle(P([(2.5, 10.5), (5, 13.5)]), fill=(48, 48, 56, 255), outline=(26, 26, 30, 255))                    # the thrusters
    d.rectangle(P([(11, 10.5), (13.5, 13.5)]), fill=(48, 48, 56, 255), outline=(26, 26, 30, 255))
    d.rectangle(P([(6.8, 2), (9.2, 3)]), fill=(70, 130, 160, 255))                                                  # the windscreen
    d.line(P([(5.6, 5), (5.6, 13)]), fill=(190, 110, 255, 255)); d.line(P([(10.4, 5), (10.4, 13)]), fill=(190, 110, 255, 255))
    d.rectangle(P([(6.5, 14), (9.5, 15.5)]), fill=(140, 140, 150, 255))                                             # the ramp
    os.makedirs(f"{OUT}/textures/item", exist_ok=True)
    im.save(f"{OUT}/textures/item/crew_drop_ship.png")


if __name__ == "__main__":
    export()
    ring_texture()
    item_icon()
