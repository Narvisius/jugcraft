"""Procedural textures for the hall's flight-ops trees (tools/ops_trees.py): the Ember Oak's and the Drift Isle's
(all original; the approved mockup's, ops_trees_options 5 Nov 2026)."""
import math
import numpy as np

# ------------------------------------------------------------------ helpers

def periodic(rnd, h, w, fy, fx, terms=10):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    out = np.zeros((h, w), np.float32)
    for _ in range(terms):
        ky, kx = int(rnd.integers(1, fy + 1)), int(rnd.integers(0, fx + 1))
        out += rnd.uniform(0.4, 1.0) * np.sin(2 * math.pi * (ky * yy / h + kx * xx / w) + rnd.uniform(0, 2 * math.pi))
    return out / terms


def fbm(rnd, h, w, octaves=5, base=2):
    out = np.zeros((h, w), np.float32)
    amp = 1.0
    for o in range(octaves):
        f = base * 2 ** o
        out += amp * periodic(rnd, h, w, f, f, terms=8)
        amp *= 0.55
    out -= out.min(); out /= max(out.max(), 1e-6)
    return out


def tex_from(rgb, a=None):
    rgb = np.clip(np.asarray(rgb, np.float32), 0, 255) / 255
    h, w = rgb.shape[:2]
    out = np.ones((h, w, 4), np.float32)
    out[..., :3] = rgb
    if a is not None:
        out[..., 3] = np.clip(a, 0, 1)
    return out


def solid(rgb, a=1.0):
    t = np.ones((2, 2, 4), np.float32)
    t[..., :3] = np.array(rgb, np.float32) / 255
    t[..., 3] = a
    return t


def halo_tex(rgb, peak=0.45, power=2.2, w=48):
    yy, xx = np.mgrid[0:w, 0:w].astype(np.float32) + 0.5
    d = np.clip(np.hypot(xx - w / 2, yy - w / 2) / (w / 2), 0, 1)
    t = np.zeros((w, w, 4), np.float32)
    t[..., :3] = np.array(rgb, np.float32) / 255
    t[..., 3] = peak * (1 - d) ** power
    return t


R_ = np.random.default_rng


def voronoi(rnd, h, w, n):
    """Distances to the nearest and second-nearest of n random points (tiling both ways)."""
    pts = rnd.uniform(0, 1, (n, 2)) * [w, h]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d1 = np.full((h, w), 1e9, np.float32); d2 = np.full((h, w), 1e9, np.float32)
    for px, py in pts:
        for ox in (-w, 0, w):
            for oy in (-h, 0, h):
                d = np.hypot(xx - px - ox, yy - py - oy)
                m = d < d1
                d2 = np.where(m, d1, np.minimum(d2, d))
                d1 = np.where(m, d, d1)
    return d1, d2


def charred_bark():
    """Charred bark: black-brown plates split by deep cracks; returns (bark, crack glow) textures, 128 x 256 (v along
    the branch, the plates stretched along it)."""
    rnd = R_(11)
    h, w = 256, 128
    d1, d2 = voronoi(rnd, h // 2, w, 26)
    d1 = np.repeat(d1, 2, 0); d2 = np.repeat(d2, 2, 0)
    edge = d2 - d1
    crack = np.clip(1 - edge / 3.2, 0, 1) ** 1.5
    n = fbm(rnd, h, w, 5)
    plate = 0.25 + 0.75 * np.clip(edge / 14, 0, 1)
    base = np.array([34, 27, 24], np.float32)
    hi = np.array([74, 62, 54], np.float32)
    rgb = base + (hi - base) * (plate * (0.55 + 0.45 * n))[..., None]
    rgb *= (1 - 0.85 * crack)[..., None]
    ember = np.array([255, 120, 30], np.float32)
    bark = tex_from(rgb + ember * (crack ** 3 * 0.6)[..., None])
    glow = np.zeros((h, w, 4), np.float32)
    glow[..., :3] = np.array([255, 128, 36], np.float32) / 255
    glow[..., 3] = np.clip(crack ** 2 * 1.1 + np.clip(1 - edge / 7, 0, 1) ** 3 * 0.25, 0, 1) * (0.6 + 0.4 * n)
    return bark, glow


def basalt_side():
    rnd = R_(12)
    h, w = 64, 32
    n = fbm(rnd, h, w, 4)
    stria = 0.5 + 0.5 * periodic(rnd, h, w, 1, 6, 6)
    v = 38 + 26 * n + 10 * stria
    rgb = np.stack([v, v * 1.0, v * 1.06], -1)
    return tex_from(rgb)


def basalt_top():
    rnd = R_(13)
    h = w = 64
    n = fbm(rnd, h, w, 5)
    d1, d2 = voronoi(rnd, h, w, 9)
    crack = np.clip(1 - (d2 - d1) / 1.6, 0, 1)
    v = 54 + 30 * n
    v = v * (1 - 0.6 * crack)
    rgb = np.stack([v, v, v * 1.05], -1)
    return tex_from(rgb)


def moss():
    rnd = R_(25)
    h = w = 64
    n = fbm(rnd, h, w, 5)
    d1, d2 = voronoi(rnd, h, w, 60)
    tuft = np.clip(1 - d1 / 5, 0, 1)
    rgb = np.stack([40 + 50 * n + 30 * tuft, 70 + 70 * n + 40 * tuft, 48 + 40 * n], -1)
    return tex_from(rgb)










def slate():
    rnd = R_(41)
    h = w = 128
    n = fbm(rnd, h, w, 5)
    v = 46 + 24 * n
    rgb = np.stack([v * 0.95, v, v * 1.08], -1)
    return tex_from(rgb)


def rock_tex(seed=42, warm=False):
    rnd = R_(seed)
    h = w = 128
    n = fbm(rnd, h, w, 6)
    d1, d2 = voronoi(rnd, h, w, 14)
    crack = np.clip(1 - (d2 - d1) / 2.0, 0, 1)
    v = 88 + 70 * n
    rgb = np.stack([v * (1.06 if warm else 0.98), v, v * (0.9 if warm else 1.04)], -1) * (1 - 0.5 * crack)[..., None]
    return tex_from(rgb)


def dark_wood():
    rnd = R_(43)
    h, w = 64, 256
    n = fbm(rnd, h, w, 4)
    grain = 0.5 + 0.5 * periodic(rnd, h, w, 10, 1, 8)
    v = 62 + 30 * n + 16 * grain
    rgb = np.stack([v * 1.25, v * 0.92, v * 0.7], -1)
    return tex_from(rgb)


def bonsai_bark():
    rnd = R_(44)
    h, w = 256, 96
    n = fbm(rnd, h, w, 5)
    fiss = 0.5 + 0.5 * periodic(rnd, h, w, 2, 7, 10)
    v = 70 + 50 * n
    rgb = np.stack([v * 1.1, v * 0.95, v * 0.88], -1) * (0.6 + 0.4 * fiss)[..., None]
    return tex_from(rgb)


# ------------------------------------------------------------------ r181: the Drift Isle's foliage, tidied (Justin, 8 Nov 2026)

def _leaf_mask(xx, yy, cx, cy, ang, L, W, round_tip=False):
    """One leaf's shape: a pointed oval from its base (cx, cy) along angle ang, length L, width W; returns
    (inside, t along it 0 base to 1 tip, v across it in pixels, its half-width there)."""
    u = (xx - cx) * math.cos(ang) + (yy - cy) * math.sin(ang)
    v = -(xx - cx) * math.sin(ang) + (yy - cy) * math.cos(ang)
    t = u / L
    s = np.clip(t, 0, 1)
    if round_tip:
        half = W * np.sqrt(np.clip(1 - (2 * s - 1) ** 2, 0, 1))
    else:
        half = W * np.sin(np.pi * np.clip(s, 0, 1) ** 0.8) * (1 - 0.25 * s)
    inside = (t >= 0) & (t <= 1) & (np.abs(v) < half)
    return inside, s, v, half


def single_leaf(colours, seed, w=48, h=64, round_tip=False):
    """One leaf filling its card (cut out), its base at the bottom middle and its tip at the top: shaded from the base
    colour to the tip's, a darker midrib and veins, a faint light edge. The foliage's leaves are laid on its clumps one
    by one, each overlapping the next like shingles, instead of sprays of leaves pushed through each other."""
    rnd = R_(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32) + 0.5
    inside, s, v, half = _leaf_mask(xx, yy, w / 2, h - 1.0, -math.pi / 2, h - 3.0, w * (0.46 if round_tip else 0.38), round_tip)
    c0, c1, c2 = (np.array(c, np.float32) for c in colours)
    col = np.where(s[..., None] < 0.5, c0 * (1 - 2 * s[..., None]) + c1 * 2 * s[..., None],
                   c1 * (2 - 2 * s[..., None]) + c2 * (2 * s[..., None] - 1))
    col = col * (0.92 + 0.16 * fbm(rnd, h, w, 3, base=2)[..., None])
    rib = np.exp(-(v / 0.9) ** 2) * (1 - 0.6 * s)
    veins = np.exp(-((np.abs(v) * 0.9 - (s * h * 0.5) % 7.0 + 3.5) / 0.9) ** 2) * (np.abs(v) < half * 0.85) * 0.18
    edge = np.clip(1 - (half - np.abs(v)) / 1.4, 0, 1)
    col = col * (1 - 0.35 * rib[..., None] - veins[..., None]) + 40 * edge[..., None]
    a = np.zeros((h, w, 4), np.float32)
    a[inside, :3] = np.clip(col[inside], 0, 255) / 255
    a[inside, 3] = 1
    return a


def foliage_tile(colours, seed, w=128, n=170, leaf=(14, 6), round_tip=False):
    """Dense foliage that repeats both ways (the clumps' cores under their leaves): small leaves overlapping on a deep
    shadow, those painted later (nearer) lighter, so it reads as depth and nothing shows through."""
    rnd = R_(seed)
    yy, xx = np.mgrid[0:w, 0:w].astype(np.float32) + 0.5
    deep = np.array(colours[0], np.float32) * 0.45
    out = np.ones((w, w, 3), np.float32) * deep
    c0, c1, c2 = (np.array(c, np.float32) for c in colours)
    for k in range(n):
        f = k / n
        cx, cy = rnd.uniform(0, w), rnd.uniform(0, w)
        ang = -math.pi / 2 + rnd.uniform(-0.9, 0.9)
        L, W = leaf[0] * rnd.uniform(0.8, 1.25), leaf[1] * rnd.uniform(0.8, 1.2)
        for ox in (-w, 0, w):
            for oy in (-w, 0, w):
                inside, s, v, half = _leaf_mask(xx, yy, cx + ox, cy + oy, ang, L, W, round_tip)
                if not inside.any():
                    continue
                base = c0 * (1 - f) + c1 * f
                col = base[None, None] * (1 - s[..., None]) + (c1 * (1 - f) + c2 * f)[None, None] * s[..., None]
                col = col * (1 - 0.3 * np.exp(-(v / 0.8) ** 2))[..., None] * (0.75 + 0.35 * f)
                out[inside] = col[inside]
    return tex_from(out)


# ------------------------------------------------------------------ r182: the Ember Oak's bark in depth (Justin, 8 Nov 2026)

def ember_plate(seed=31, w=64):
    """A raised plate of charred bark (one plate per tile): black-brown char with a fine grain and small checks, its
    rim warming to a dark ember red where the heat from the crack below catches it."""
    rnd = R_(seed)
    n = fbm(rnd, w, w, 5)
    grain = periodic(rnd, w, w, 14, 1, terms=8)
    yy, xx = np.mgrid[0:w, 0:w].astype(np.float32) + 0.5
    rim = np.minimum(np.minimum(xx, w - xx), np.minimum(yy, w - yy)) / (w / 2)
    warm = np.clip(1 - rim / 0.22, 0, 1) ** 2
    base = np.array([30, 24, 22], np.float32)
    hi = np.array([70, 58, 50], np.float32)
    rgb = base + (hi - base) * (0.35 + 0.65 * n * (0.8 + 0.2 * grain))[..., None]
    d1, d2 = voronoi(rnd, w, w, 18)
    check = np.clip(1 - (d2 - d1) / 1.0, 0, 1)
    rgb *= (1 - 0.12 * check)[..., None]
    rgb = rgb * (1 - 0.35 * warm[..., None]) + np.array([150, 44, 14], np.float32) * (warm * 0.55)[..., None]
    return tex_from(rgb)


def ember_edge(seed=32, w=32, h=32):
    """A plate's side, full bright: molten yellow-orange at its foot (the crack), cooling up through red to the char at
    its top edge (v 0 is the foot)."""
    rnd = R_(seed)
    n = fbm(rnd, h, w, 4)
    v = (np.mgrid[0:h, 0:w][0].astype(np.float32) + 0.5) / h
    hot = np.array([255, 196, 80], np.float32)
    mid = np.array([235, 92, 24], np.float32)
    cool = np.array([60, 22, 14], np.float32)
    t = np.clip(v * (0.85 + 0.3 * n), 0, 1)[..., None]
    # (r183: the wall's lower part, all a crack shows of it from outside, kept hot; only its lip cools to the char)
    rgb = np.where(t < 0.5, hot * (1 - t / 0.5) + mid * (t / 0.5), mid * np.clip(1 - (t - 0.5) / 0.4, 0, 1) + cool * np.clip((t - 0.5) / 0.4, 0, 1))
    return tex_from(rgb)


def ember_core(seed=33, w=64, h=128):
    """The molten wood under the plates, seen in the cracks between them (full bright, pulsing in the game): bright
    orange-yellow, mottled, with darker veins along the branch."""
    rnd = R_(seed)
    n = fbm(rnd, h, w, 5)
    vein = np.abs(periodic(rnd, h, w, 3, 6, terms=10))
    hot = np.array([255, 196, 92], np.float32)
    mid = np.array([240, 104, 26], np.float32)
    deep = np.array([140, 36, 10], np.float32)
    t = np.clip(0.35 + 0.6 * n + 0.5 * vein, 0, 1)[..., None]
    rgb = np.where(t < 0.5, hot * (1 - t / 0.5) + mid * (t / 0.5), mid * (1 - (t - 0.5) / 0.5) + deep * ((t - 0.5) / 0.5))
    return tex_from(rgb)


# ------------------------------------------------------------------ the Ember Oak's leaves (r184; Justin, 8 November 2026,
# "between a solid leaf and flames", look A of the mockup chosen: molten-veined leaves, small flames licking off the tips)

_FLAME = np.array([[255, 248, 214], [255, 214, 96], [255, 140, 34], [214, 58, 16], [120, 20, 8]], np.float32)


def _flame_colour(h):
    """Heat 1 (a flame's core) to 0 (its tips): white-yellow, yellow, orange, red, then gone (0..255)."""
    xs = np.array([1.0, 0.78, 0.5, 0.25, 0.0])
    out = np.zeros(h.shape + (3,), np.float32)
    for c in range(3):
        out[..., c] = np.interp(-h, -xs, _FLAME[:, c])
    return out


def _smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def _molten_leaf(xx, yy, bx, by, ang, L, Wd):
    """A pointed leaf from its stalk's end (bx, by) along ang: its mask and its colour (0..255), dark red going orange to
    the tip, its veins and rim molten."""
    dx, dy = math.cos(ang), math.sin(ang)
    u = (xx - bx) * dx + (yy - by) * dy
    v = (xx - bx) * -dy + (yy - by) * dx
    t = u / L
    prof = np.sqrt(np.clip(t * (1.15 - t) / 0.33, 0, 1)) * (1 - 0.45 * np.clip(t, 0, 1) ** 2)
    half = Wd * prof
    leaf = (t > 0.06) & (t < 1.12) & (np.abs(v) < half)
    mid = np.exp(-(v / 0.8) ** 2)
    side = np.exp(-((np.mod(u - np.abs(v) * 1.3, L / 5) - L / 10) / 0.8) ** 2) * (np.abs(v) < half * 0.85)
    rim = np.clip(1 - (half - np.abs(v)) / 1.4, 0, 1)
    vein = np.clip(mid + side * 0.8, 0, 1)
    tt = np.clip(t, 0, 1)[..., None]
    base = np.array([120, 20, 10], np.float32) * (1 - tt) + np.array([205, 64, 16], np.float32) * tt
    hot = np.clip(vein * 0.7 + rim * 0.8, 0, 1)[..., None]
    return leaf, base * (1 - hot) + _flame_colour(0.55 + 0.4 * hot[..., 0]) * hot


def ember_spray(seed, w=64):
    """A spray lying in the canopy: nine molten-veined leaves scattered over the card (cut out; drawn full bright)."""
    rnd = R_(seed)
    yy, xx = np.mgrid[0:w, 0:w].astype(np.float32) + 0.5
    out = np.zeros((w, w, 4), np.float32)
    for k in range(9):
        ang = rnd.uniform(0, 2 * math.pi)
        L, Wd = rnd.uniform(0.28, 0.4) * w, rnd.uniform(0.08, 0.11) * w
        cx, cy = rnd.uniform(0.25, 0.75) * w, rnd.uniform(0.25, 0.75) * w
        leaf, col = _molten_leaf(xx, yy, cx - math.cos(ang) * L * 0.5, cy - math.sin(ang) * L * 0.5, ang, L, Wd)
        sel = leaf & (out[..., 3] < 0.5)
        out[sel, :3] = np.clip(col[sel], 0, 255) / 255
        out[sel, 3] = 1
    return out


def _tongue(xx, yy, x0, y0, length, width, rnd, sway=0.18, lean=0.0):
    """A licking flame's heat (1 in its core, 0 outside): from (x0, y0) up the card for `length`, `width` across at
    its root, swaying more the higher it gets, pointed at its tip."""
    s = (y0 - yy) / length
    ph, f = rnd.uniform(0, 6.3), rnd.uniform(1.4, 2.4)
    cx = x0 + length * (lean * s + sway * np.sin(f * s * math.pi + ph) * s)
    w = width * np.clip(1 - s, 0, 1) ** 0.75 * (1 + 0.35 * np.clip(0.25 - s, 0, 1) / 0.25) * (s > -0.12)
    d = np.abs(xx - cx) / np.maximum(w, 1e-3)
    h = np.clip(1 - d, 0, 1) * np.clip(1 - np.clip(s, 0, 1) ** 1.6, 0, 1) * _smooth(-0.12, 0.05, s)
    return np.where((s > -0.12) & (s < 1), h, 0)


def ember_flaming(seed, w=64, frames=4):
    """A spray standing up out of the canopy (its top is up in the world): six molten-veined leaves pointing up, low on
    the card (cut out, full bright), and the small flames licking off their tips as `frames` moments side by side (w
    each; the game steps through them so they flicker), soft-edged, none laid over the leaves."""
    rnd = R_(seed)
    yy, xx = np.mgrid[0:w, 0:w].astype(np.float32) + 0.5
    body = np.zeros((w, w, 4), np.float32)
    leaves = []
    for k in range(6):
        bx, by = rnd.uniform(0.27, 0.73) * w, rnd.uniform(0.74, 0.9) * w
        ang = -math.pi / 2 + rnd.uniform(-0.7, 0.7)
        L, Wd = rnd.uniform(0.36, 0.48) * w, rnd.uniform(0.09, 0.12) * w
        leaf, col = _molten_leaf(xx, yy, bx, by, ang, L, Wd)
        sel = leaf & (body[..., 3] < 0.5)
        body[sel, :3] = np.clip(col[sel], 0, 255) / 255
        body[sel, 3] = 1
        leaves.append((bx + math.cos(ang) * L, by + math.sin(ang) * L, math.cos(ang), L, Wd))
    strip = np.zeros((w, w * frames, 4), np.float32)
    up = np.clip((w - yy) / w, 0, 1)
    for f in range(frames):
        frnd = R_(seed * 101 + 7 + f * 7919)
        wx = (fbm(frnd, w, w, 4) - 0.5) * w * 0.07
        wy = (fbm(frnd, w, w, 4) - 0.5) * w * 0.04
        fx, fy = xx + wx * (0.3 + up), yy + wy * (0.3 + up)
        heat = np.zeros((w, w), np.float32)
        for tx, ty, dx, L, Wd in leaves:
            heat = np.maximum(heat, _tongue(fx, fy, tx, ty + L * 0.12, L * 0.55, Wd * 0.75, frnd, lean=dx * 0.5))
        heat = np.clip(heat, 0, 1)
        a = _smooth(0.06, 0.3, heat) * 0.95
        a[body[..., 3] > 0.5] = 0
        strip[:, f * w:(f + 1) * w, :3] = _flame_colour(np.clip(heat * 1.15, 0, 1)) / 255
        strip[:, f * w:(f + 1) * w, 3] = a
    return body, strip


# ------------------------------------------------------------------ the Ember Oak's soil (r185; Justin, 8 November 2026,
# "I'd like the soil to look a little bit better", look C of the mockup chosen: burnt soil, ash, coals, fallen leaves)

def burnt_loam(seed=321, w=128):
    """Dark, rich burnt soil with soft drifts of pale ash and darker crumbs (repeats)."""
    rnd = R_(seed)
    n = fbm(rnd, w, w, 5)
    drift = np.clip((fbm(rnd, w, w, 3) - 0.68) / 0.22, 0, 1)
    drift = drift * drift * (3 - 2 * drift) * 0.7
    speck = rnd.random((w, w))
    soil = np.stack([30 + 18 * n, 21 + 12 * n, 16 + 8 * n], -1)
    ash = np.stack([84 + 22 * n, 80 + 20 * n, 76 + 18 * n], -1)
    rgb = soil * (1 - drift[..., None] * 0.85) + ash * drift[..., None] * 0.85
    rgb[speck > 0.97] *= 0.5
    return tex_from(rgb)


def ember_coal(seed=333, w=32):
    """A coal half burnt through: charred lumps, a deep red glow in them, cracked all over with bright orange."""
    rnd = R_(seed)
    n = fbm(rnd, w, w, 4)
    d1, d2 = voronoi(rnd, w, w, 14)
    crack = np.clip(1 - (d2 - d1) / 1.6, 0, 1)
    base = np.stack([52 + 34 * n, 18 + 10 * n, 12 + 5 * n], -1)
    rgb = base * (1 - crack[..., None]) + _flame_colour(0.32 + 0.33 * crack) * crack[..., None]
    return tex_from(rgb)


def ember_speck(w=8):
    """A spark of a coal in the soil (cut out; drawn full bright): hot in the middle, orange at its edge."""
    yy, xx = np.mgrid[0:w, 0:w].astype(np.float32) + 0.5
    d = np.hypot(xx - w / 2, yy - w / 2) / (w / 2)
    a = (d < 0.85).astype(np.float32)
    return tex_from(_flame_colour(np.clip(0.75 - 0.5 * d, 0, 1)), a)
