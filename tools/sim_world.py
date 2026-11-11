"""The Pilot Simulator's world (work list item 22 part B, room 9, take 2): what the simulators show is a Minecraft world,
drawn the way the game draws one (the game's own block textures, its face shading and smooth-lighting corners, its fog,
its sky, square sun and flat blocky clouds), seen from the drone's camera.

The world is made here once from a seed: rolling plains and woods (oak, birch), a river from the snowy mountains to a
lake with sand beaches, spruce on the slopes, a village of plank houses, and the drone base: the Tri-Pillar tower with its
landing pads on a concrete apron. Each simulator flies a scenario in it (CAMS): free flight over the woods, a run past the
village, a take-off from a pad, an approach to land, a GPS fault over the mountains, and the menu of a free simulator.

pod(n) gives simulator n's three screens (left, centre, right: the side screens turned 60 degrees, so the three make one
view round the pilot) with the HUD; repeater(n) the instructor's copy of its centre screen. In game the views are these
pictures and the HUD over them is drawn live.

Textures come from the game's own assets (VANILLA: its textures/block folder, unpacked from the client jar)."""
from __future__ import annotations

import math
import os
from functools import lru_cache
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

# the game's block textures (assets/minecraft/textures/block unpacked from the client jar; not in the repo): JUG_VANILLA
VANILLA = Path(os.environ.get("JUG_VANILLA", "")) if os.environ.get("JUG_VANILLA") else None
# the pictures drawn (tools/data/sim_world: kept in the repo, so the generators need no game textures to run)
CACHE = Path(os.environ.get("JUG_SIMCACHE", str(Path(__file__).resolve().parent / "data" / "sim_world")))

N, HY, SEA = 640, 176, 62
C = 320                                  # the middle of the world (the tower's a little west of it)
TOWER = (300, 330)
VILLAGE = (420, 380)
LAKE = (190, 470)
GRASS_TINT, FOLIAGE_TINT = (145, 189, 89), (119, 171, 47)
BIRCH_TINT, SPRUCE_TINT, WATER_TINT = (128, 167, 85), (97, 153, 97), (63, 118, 228)
SKY, FOG = np.array([120, 167, 255], float), np.array([192, 216, 255], float)

# ------------------------------------------------------------------ blocks
# id: (top, side, bottom, kind) ; kind 0 opaque, 1 cut-out (leaves, glass), 2 water
BLOCKS = {
    1: ("grass_top", "grass_side", "dirt", 0), 2: ("dirt", "dirt", "dirt", 0), 3: ("stone", "stone", "stone", 0),
    4: ("sand", "sand", "sand", 0), 5: ("water", "water", "water", 2), 6: ("oak_log_top", "oak_log", "oak_log_top", 0),
    7: ("oak_leaves", "oak_leaves", "oak_leaves", 1), 8: ("birch_log_top", "birch_log", "birch_log_top", 0),
    9: ("birch_leaves", "birch_leaves", "birch_leaves", 1), 10: ("spruce_log_top", "spruce_log", "spruce_log_top", 0),
    11: ("spruce_leaves", "spruce_leaves", "spruce_leaves", 1), 12: ("snow", "grass_block_snow", "dirt", 0),
    13: ("gravel", "gravel", "gravel", 0), 14: ("oak_planks", "oak_planks", "oak_planks", 0),
    15: ("cobblestone", "cobblestone", "cobblestone", 0), 16: ("spruce_planks", "spruce_planks", "spruce_planks", 0),
    17: ("glass", "glass", "glass", 1), 18: ("white_concrete", "white_concrete", "white_concrete", 0),
    19: ("light_gray_concrete", "light_gray_concrete", "light_gray_concrete", 0),
    20: ("yellow_concrete", "yellow_concrete", "yellow_concrete", 0), 21: ("gray_concrete", "gray_concrete", "gray_concrete", 0),
    22: ("stone_bricks", "stone_bricks", "stone_bricks", 0), 23: ("dirt_path_top", "dirt_path_side", "dirt", 0),
    24: ("snow", "snow", "snow", 0), 25: ("light_blue_stained_glass", "light_blue_stained_glass", "light_blue_stained_glass", 0),
    26: ("black_concrete", "black_concrete", "black_concrete", 0), 27: ("hay_block_top", "hay_block_side", "hay_block_top", 0),
    28: ("farmland_moist", "dirt", "dirt", 0), 29: ("red_concrete", "red_concrete", "red_concrete", 0),
}
TEXNAMES = sorted({t for b in BLOCKS.values() for t in b[:3]})


def _load(name):
    if VANILLA is None:
        raise SystemExit("tools/sim_world.py draws with the game's block textures: set JUG_VANILLA to them")
    a = np.array(Image.open(VANILLA / f"{name}.png").convert("RGBA"), float)[:16, :16]
    return a


def _tint(a, rgb):
    b = a.copy()
    b[..., :3] *= np.array(rgb, float) / 255
    return b


@lru_cache(None)
def textures():
    """The block textures (tinted as the game tints them in a plains biome), with their mipmaps."""
    T = {}
    for n in TEXNAMES:
        if n == "grass_top":
            T[n] = _tint(_load("grass_block_top"), GRASS_TINT)
        elif n == "grass_side":
            base, ov = _load("grass_block_side"), _tint(_load("grass_block_side_overlay"), GRASS_TINT)
            al = ov[..., 3:] / 255
            t = base.copy()
            t[..., :3] = base[..., :3] * (1 - al) + ov[..., :3] * al
            T[n] = t
        elif n == "oak_leaves":
            T[n] = _tint(_load(n), FOLIAGE_TINT)
        elif n == "birch_leaves":
            T[n] = _tint(_load(n), BIRCH_TINT)
        elif n == "spruce_leaves":
            T[n] = _tint(_load(n), SPRUCE_TINT)
        elif n == "water":
            t = _tint(_load("water_still"), WATER_TINT)
            t[..., 3] = 190
            T[n] = t
        else:
            T[n] = _load(n)
    names = TEXNAMES
    levels = []
    stack = np.stack([T[n] for n in names])            # (k, 16, 16, 4)
    s = stack
    for _ in range(5):
        levels.append(s)
        if s.shape[1] == 1:
            break
        a = s[..., 3:] / 255
        pm = np.concatenate([s[..., :3] * a, a * 255], -1)
        k, h, w, _c = pm.shape
        pm = pm.reshape(k, h // 2, 2, w // 2, 2, 4).mean((2, 4))
        al = np.maximum(pm[..., 3:] / 255, 1e-6)
        s = np.concatenate([pm[..., :3] / al, pm[..., 3:]], -1)
    return names, levels


# ------------------------------------------------------------------ the world
def _noise(shape, cell, seed, octaves=4):
    rng = np.random.default_rng(seed)
    out = np.zeros(shape)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = max(2, int(cell / 2 ** o))
        gh, gw = shape[0] // c + 3, shape[1] // c + 3
        g = rng.random((gh, gw))
        im = Image.fromarray(g.astype(np.float32), "F").resize((gw * c, gh * c), Image.BICUBIC)
        a = np.clip(np.array(im, float)[:shape[0], :shape[1]], 0, 1)
        out += amp * a
        tot += amp
        amp *= 0.5
    return out / tot


def _disc(x, z, cx, cz):
    return np.hypot(x - cx, z - cz)


def build_world(seed=2207):
    rng = np.random.default_rng(seed)
    xs, zs = np.meshgrid(np.arange(N), np.arange(N), indexing="ij")
    roll = _noise((N, N), 96, seed + 1) - 0.5
    small = _noise((N, N), 24, seed + 2, 3) - 0.5
    h = 66 + roll * 16 + small * 4
    # the mountains, north-east
    mt = np.clip(1 - _disc(xs, zs, 470, 150) / 190, 0, 1) ** 1.4
    ridge = 1 - np.abs(_noise((N, N), 70, seed + 3) * 2 - 1)
    h += mt * (40 + ridge * 45)
    # the lake, south-west, and the river down to it from the mountains
    lake = np.clip(1 - _disc(xs, zs, *LAKE) / 95, 0, 1)
    h -= lake ** 0.6 * 22
    t = np.linspace(0, 1, 400)
    rx = 430 + (LAKE[0] - 430) * t + 30 * np.sin(t * 9)
    rz = 210 + (LAKE[1] - 210) * t + 22 * np.sin(t * 6 + 1)
    riv = np.full((N, N), 1e9)
    for px, pz in zip(rx[::2], rz[::2]):
        riv = np.minimum(riv, _disc(xs, zs, px, pz))
    h = np.where(riv < 9, np.minimum(h, SEA - 2 + riv * 0.5), np.where(riv < 16, np.minimum(h, SEA + (riv - 9) * 0.9 + 1), h))
    # flats for the drone base and the village
    for (cx, cz), r, y in ((TOWER, 36, 66), (VILLAGE, 30, 66)):
        d = _disc(xs, zs, cx, cz)
        w = np.clip((r + 14 - d) / 14, 0, 1)
        h = h * (1 - w) + y * w
    h = np.round(h).astype(int)
    W = np.zeros((N, N, HY), np.uint8)
    beach = (h <= SEA + 1) & (h >= SEA - 3)
    g0, g1 = np.gradient(h.astype(float))
    steep = (np.abs(g0) + np.abs(g1) > 3.2) & (h > 84)
    topb = np.where(beach, 4, np.where(h < SEA - 3, 13, np.where(h > 118, 24, np.where(steep, 3, np.where(h > 104, 12, 1)))))
    for y in range(HY):
        L = W[:, :, y]
        L[y < h - 3] = 3
        L[(y >= h - 3) & (y < h)] = np.where(beach & (y >= h - 2), 4, 2)[(y >= h - 3) & (y < h)]
        L[y == h] = topb[y == h]
        L[(y > h) & (y <= SEA)] = 5
    surf = h.copy()

    def put(x, y, z, b):
        if 0 <= x < N and 0 <= z < N and 0 <= y < HY:
            W[x, z, y] = b

    def tree(x, z, kind):
        y0 = surf[x, z] + 1
        if W[x, z, y0 - 1] not in (1, 12):
            return
        if kind == "spruce":
            ht = int(rng.integers(7, 11))
            for y in range(ht):
                put(x, y0 + y, z, 10)
            for y in range(2, ht + 1):
                r = max(0, int((ht - y) * 0.42) + (y % 2))
                for dx in range(-r, r + 1):
                    for dz in range(-r, r + 1):
                        if abs(dx) + abs(dz) <= r + 1 and (dx or dz or y == ht) and W[x + dx, z + dz, y0 + y] == 0:
                            put(x + dx, y0 + y, z + dz, 11)
            put(x, y0 + ht, z, 11)
            return
        log, lv = (6, 7) if kind == "oak" else (8, 9)
        ht = int(rng.integers(4, 7) if kind == "oak" else rng.integers(5, 8))
        for y in range(ht):
            put(x, y0 + y, z, log)
        for y, r in ((ht - 3, 2), (ht - 2, 2), (ht - 1, 1), (ht, 1)):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if abs(dx) == r and abs(dz) == r and (r == 1 and y == ht or rng.random() < 0.5):
                        continue
                    if W[x + dx, z + dz, y0 + y] == 0:
                        put(x + dx, y0 + y, z + dz, lv)

    forest = _noise((N, N), 60, seed + 5, 3)
    birchy = _noise((N, N), 80, seed + 6, 2)
    keep = np.ones((N, N), bool)
    for (cx, cz), r in ((TOWER, 44), (VILLAGE, 36)):
        keep &= _disc(xs, zs, cx, cz) > r
    for gx in range(10, N - 10, 5):
        for gz in range(10, N - 10, 5):
            x, z = gx + int(rng.integers(-2, 3)), gz + int(rng.integers(-2, 3))
            if not keep[x, z] or riv[x, z] < 12 or surf[x, z] <= SEA + 1:
                continue
            hgt = surf[x, z]
            if hgt > 86:
                p, kind = (0.55 if hgt < 112 else 0.0), "spruce"
            else:
                p = 0.04 + 0.85 * max(0.0, forest[x, z] - 0.48) * 3
                kind = "birch" if birchy[x, z] > 0.56 else "oak"
            if rng.random() < p:
                tree(x, z, kind)
    _drone_base(W, surf, put)
    _village(W, surf, put, rng)
    return W, surf


def _drone_base(W, surf, put):
    """The Tri-Pillar tower on its apron with the landing pads round it."""
    tx, tz = TOWER
    y = 66
    for x in range(tx - 34, tx + 35):
        for z in range(tz - 34, tz + 35):
            r = math.hypot(x - tx, z - tz)
            if r <= 34:
                W[x, z, y + 1:y + 12] = 0
                put(x, y, z, 21 if r > 32.5 or (x - tx) % 9 == 0 or (z - tz) % 9 == 0 else 19)
    # the pads: a yellow ring, a white H, on dark concrete
    for k, ang in enumerate((30, 150, 270)):
        px = int(round(tx + 22 * math.cos(math.radians(ang))))
        pz = int(round(tz + 22 * math.sin(math.radians(ang))))
        for dx in range(-5, 6):
            for dz in range(-5, 6):
                r = math.hypot(dx, dz)
                b = 21
                if 3.6 <= r <= 4.6:
                    b = 20
                if r > 5.3:
                    continue
                if (abs(dx) == 2 and abs(dz) <= 2) or (dz == 0 and abs(dx) <= 2):
                    b = 18
                put(px + dx, y, pz + dz, b)
    # the three pillars, banded with glass, the deck ring near the top, the mast
    for ang in (90, 210, 330):
        cx = tx + 7 * math.cos(math.radians(ang))
        cz = tz + 7 * math.sin(math.radians(ang))
        for x in range(int(cx) - 5, int(cx) + 6):
            for z in range(int(cz) - 5, int(cz) + 6):
                r = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
                if r <= 3.7:
                    for yy in range(y + 1, 152):
                        put(x, yy, z, 25 if (yy - y) % 12 in (6, 7) and r > 2.6 else 18)
    for x in range(tx - 14, tx + 15):
        for z in range(tz - 14, tz + 15):
            r = math.hypot(x - tx, z - tz)
            if r <= 13.5:
                put(x, 142, z, 19)
                put(x, 152, z, 21 if r < 12.5 else 19)
                if r > 12.5:
                    for yy in range(143, 152):
                        put(x, yy, z, 25 if yy % 3 else 19)
    for yy in range(153, 168):
        put(tx, yy, tz, 26)
    put(tx, 168, tz, 29)


def _village(W, surf, put, rng):
    vx, vz = VILLAGE
    y = 66
    for x in range(vx - 28, vx + 29):
        for z in range(vz - 28, vz + 29):
            if math.hypot(x - vx, z - vz) <= 28:
                W[x, z, y + 1:y + 14] = 0
                put(x, y, z, 1)
    for x in range(vx - 26, vx + 27):
        for w in (-1, 0, 1):
            put(x, y, vz + w, 23)
    for z in range(vz - 26, vz + 27):
        for w in (-1, 0, 1):
            put(vx + w, y, z, 23)
    # the well
    for dx in range(-2, 2):
        for dz in range(-2, 2):
            put(vx + 3 + dx, y + 1, vz + 3 + dz, 15 if dx in (-2, 1) or dz in (-2, 1) else 5)
    houses = [(-18, -12, 7, 6), (-8, -14, 6, 7), (6, -13, 8, 6), (16, -12, 6, 6), (-17, 6, 6, 7), (-7, 8, 7, 6), (8, 7, 6, 7),
              (17, 8, 7, 6)]
    for hx, hz, w, d in houses:
        x0, z0 = vx + hx, vz + hz
        for x in range(x0, x0 + w):
            for z in range(z0, z0 + d):
                edge = x in (x0, x0 + w - 1) or z in (z0, z0 + d - 1)
                corner = x in (x0, x0 + w - 1) and z in (z0, z0 + d - 1)
                put(x, y + 1, z, 15 if edge else 14)
                if edge:
                    for yy in range(y + 2, y + 5):
                        b = 6 if corner else 14
                        if not corner and yy == y + 3 and ((x - x0) % 3 == 1 or (z - z0) % 3 == 1):
                            b = 17
                        put(x, yy, z, b)
        # the roof: steps of spruce planks from the long sides up to the ridge
        for k in range(0, d // 2 + 2):
            for x in range(x0 - 1, x0 + w + 1):
                for z in (z0 - 1 + k, z0 + d - k):
                    put(x, y + 5 + k, z, 16)
        for x in range(x0, x0 + w):
            for k in range(0, d // 2 + 1):
                for z in range(z0 + k, z0 + d - k):
                    if (x in (x0, x0 + w - 1)) and W[x, z, y + 5 + k] == 0:
                        put(x, y + 5 + k, z, 14)
    # the fields east of the village: farmland rows and hay
    for x in range(vx + 32, vx + 52):
        for z in range(vz - 10, vz + 12):
            put(x, surf[x, z], z, 5 if (z - vz) % 5 == 0 else 28)
    for (x, z) in ((vx + 30, vz - 14), (vx + 31, vz - 14), (vx + 30, vz - 13)):
        put(x, surf[x, z] + 1, z, 27)


@lru_cache(None)
def world():
    # (the voxels are rebuilt from the seed in a few seconds: kept out of the repo, in the system's temporary folder)
    import tempfile
    f = Path(os.environ.get("JUG_SIMWORLD", tempfile.gettempdir())) / "jug_sim_world_v4.npz"
    if f.exists():
        z = np.load(f)
        return z["W"], z["surf"]
    W, surf = build_world()
    np.savez_compressed(f, W=W, surf=surf)
    return W, surf


# ------------------------------------------------------------------ the renderer
@lru_cache(None)
def _tables():
    names, levels = textures()
    idx = {n: i for i, n in enumerate(names)}
    top = np.zeros(256, int)
    side = np.zeros(256, int)
    bot = np.zeros(256, int)
    kind = np.full(256, -1)
    for b, (t, s, bt, k) in BLOCKS.items():
        top[b], side[b], bot[b], kind[b] = idx[t], idx[s], idx[bt], k
    return top, side, bot, kind


def _sample(tex, uv_u, uv_v, footprint):
    names, levels = textures()
    lv = np.clip(np.floor(np.log2(np.maximum(footprint, 1e-6))), 0, len(levels) - 1).astype(int)
    out = np.zeros((len(tex), 4))
    for L in range(len(levels)):
        m = lv == L
        if not m.any():
            continue
        s = levels[L].shape[1]
        iu = np.clip((uv_u[m] * s).astype(int), 0, s - 1)
        iv = np.clip((uv_v[m] * s).astype(int), 0, s - 1)
        out[m] = levels[L][tex[m], iv, iu]
    return out


def _cloud(x, z):
    cx, cz = np.floor(x / 12).astype(np.int64), np.floor(z / 12).astype(np.int64)
    h = (cx * 73856093) ^ (cz * 19349663)
    h = (h ^ (h >> 13)) * 1274126177
    v = ((h >> 8) & 1023) / 1023
    # clumps: a low-frequency mask so the clouds come in banks, not salt and pepper
    big = np.sin(cx * 0.37 + np.sin(cz * 0.21) * 2) + np.sin(cz * 0.29 + 1.3)
    return (v > 0.55) & (big > 0.2)


def _basis(cam, head=0.0):
    """Camera to world: the drone's yaw (0 north, positive east), pitch (up positive), roll, then the screen's own turn
    (head) in the drone's frame, so the side screens bank and pitch with it."""
    def ry(a):
        c, s_ = math.cos(a), math.sin(a)
        return np.array([[c, 0, -s_], [0, 1, 0], [s_, 0, c]])
    def rx(a):
        c, s_ = math.cos(a), math.sin(a)
        return np.array([[1, 0, 0], [0, c, -s_], [0, s_, c]])
    def rz(a):
        c, s_ = math.cos(a), math.sin(a)
        return np.array([[c, -s_, 0], [s_, c, 0], [0, 0, 1]])
    r = math.radians
    return ry(r(cam["yaw"])) @ rx(r(cam["pitch"])) @ rz(r(cam.get("roll", 0))) @ ry(r(head))


def project(cam, head, wdirs, w, h, fov=60.0):
    """World directions to pixel positions on a screen (None behind it)."""
    R = _basis(cam, head)
    f = 1 / math.tan(math.radians(fov) / 2)
    out = []
    for v in wdirs:
        c = R.T @ np.asarray(v, float)
        if c[2] >= -1e-6:
            out.append(None)
            continue
        sx, sy = c[0] * f / -c[2], c[1] * f / -c[2]
        out.append(((sx + 1) / 2 * w, (-sy * w / h + 1) / 2 * h))
    return out


def render(cam, w, h, fov=60.0, maxt=230.0, head=0.0):
    """Draws what the camera (pos, yaw, pitch, roll; yaw 0 looks north (-z), positive turns east) sees, turned by head
    in the drone's frame."""
    W, surf = world()
    top, side, bot, kind = _tables()
    ox, oy, oz = cam["pos"]
    f = 1 / math.tan(math.radians(fov) / 2)
    js, is_ = np.meshgrid((np.arange(w) + 0.5) / w * 2 - 1, (np.arange(h) + 0.5) / h * 2 - 1)
    sx, sy = js * 1.0, -is_ * h / w
    d = np.stack([sx, sy, -np.full_like(sx, f)], -1) @ _basis(cam, head).T
    d /= np.linalg.norm(d, axis=-1, keepdims=True)
    D = d.reshape(-1, 3)
    M = len(D)
    o = np.array([ox, oy, oz], float)
    pixang = 2 / f / w                                  # radians per pixel, about
    with np.errstate(divide="ignore"):
        tDelta = np.abs(1 / D)
    step = np.where(D >= 0, 1, -1).astype(int)
    pos = np.tile(np.floor(o).astype(int), (M, 1))
    tMax = np.where(D >= 0, (pos + 1 - o) / np.where(D == 0, 1e-12, D), (pos - o) / np.where(D == 0, -1e-12, D))
    tMax = np.where(D == 0, np.inf, tMax)
    hit_b = np.zeros(M, int)
    hit_t = np.full(M, np.inf)
    hit_ax = np.zeros(M, int)
    hit_pos = np.zeros((M, 3), int)
    wat_t = np.full(M, np.inf)
    wat_ax = np.zeros(M, int)
    act = np.arange(M)
    names, levels = textures()
    for _ in range(1400):
        if len(act) == 0:
            break
        tm = tMax[act]
        ax = np.argmin(tm, 1)
        t = tm[np.arange(len(act)), ax]
        pos[act, ax] += step[act, ax]
        tMax[act, ax] += tDelta[act, ax]
        p = pos[act]
        out = (p[:, 0] < 0) | (p[:, 0] >= N) | (p[:, 2] < 0) | (p[:, 2] >= N) | (p[:, 1] < 0) | (p[:, 1] >= HY) | (t > maxt)
        pc = np.clip(p, 0, [N - 1, HY - 1, N - 1])
        b = W[pc[:, 0], pc[:, 2], pc[:, 1]].astype(int)
        b[out] = 0
        k = kind[b]
        solid = (k == 0) & ~out
        # cut-outs: through the clear texels
        cut = (k == 1) & ~out
        if cut.any():
            ci = np.nonzero(cut)[0]
            a = act[ci]
            tt = t[ci]
            hp = o + D[a] * tt[:, None]
            axc = ax[ci]
            u, v = _uv(hp, axc, step[a])
            tex = np.where(axc == 1, np.where(step[a, 1] > 0, bot[b[ci]], top[b[ci]]), side[b[ci]])
            al = _sample(tex, u, v, tt * pixang * 16)[:, 3]
            solid[ci[al > 127]] = True
        water = (k == 2) & ~out
        if water.any():
            wi = np.nonzero(water)[0]
            a = act[wi]
            first = np.isinf(wat_t[a])
            wat_t[a[first]] = t[wi][first]
            wat_ax[a[first]] = ax[wi][first]
        done = solid | out
        di = np.nonzero(solid)[0]
        a = act[di]
        hit_b[a] = b[di]
        hit_t[a] = t[di]
        hit_ax[a] = ax[di]
        hit_pos[a] = p[di]
        act = act[~done]
    col = np.zeros((M, 3))
    hit = hit_b > 0
    hi = np.nonzero(hit)[0]
    if len(hi):
        tt = hit_t[hi]
        hp = o + D[hi] * tt[:, None]
        axh = hit_ax[hi]
        stp = step[hi]
        u, v = _uv(hp, axh, stp)
        bb = hit_b[hi]
        tex = np.where(axh == 1, np.where(stp[:, 1] > 0, bot[bb], top[bb]), side[bb])
        cos_in = np.abs(D[hi, axh])
        texel = _sample(tex, u, v, tt * pixang * 16 / np.maximum(cos_in, 0.25))[:, :3]
        shade = np.select([axh == 1, axh == 2, axh == 0], [np.where(stp[:, 1] > 0, 0.5, 1.0), 0.8, 0.6])
        ao = _ao(W, hit_pos[hi], axh, stp, hp)
        col[hi] = texel * (shade * ao)[:, None]
    # water over what is under it
    wi = np.nonzero(np.isfinite(wat_t) & (wat_t < hit_t))[0]
    if len(wi):
        tt = wat_t[wi]
        hp = o + D[wi] * tt[:, None]
        u, v = _uv(hp, wat_ax[wi], step[wi])
        idx = names.index("water")
        wc = _sample(np.full(len(wi), idx), u, v, tt * pixang * 16)[:, :3]
        depth = np.clip((np.where(np.isfinite(hit_t[wi]), hit_t[wi], tt + 30) - tt) * np.abs(D[wi, 1]), 0, 12)
        a = 0.62 + 0.3 * depth / 12
        under = np.where(np.isfinite(hit_t[wi])[:, None], col[wi], wc * 0.6)
        col[wi] = under * (1 - a)[:, None] + wc * a[:, None]
        hit_t[wi] = np.minimum(hit_t[wi], tt)
        hit[wi] = True
    # the sky: a gradient from the fog at the horizon, the sun, the clouds
    el = D[:, 1]
    skyc = FOG[None, :] + (SKY - FOG)[None, :] * np.clip(el * 2.4, 0, 1)[:, None] ** 0.7
    sun = np.array([0.35, 0.62, -0.70])
    sun /= np.linalg.norm(sun)
    su = np.cross(sun, [0, 1, 0])
    su /= np.linalg.norm(su)
    sv = np.cross(su, sun)
    ds = D @ sun
    pu, pv = D @ su, D @ sv
    sunm = (ds > 0.9) & (np.abs(pu) < 0.045) & (np.abs(pv) < 0.045)
    skyc[sunm] = (255, 252, 214)
    up = el > 0.005
    tc = np.where(up, (190 - oy) / np.maximum(el, 1e-6), np.inf)
    cxp, czp = ox + D[:, 0] * tc, oz + D[:, 2] * tc
    cm = up & (tc < 900) & _cloud(np.where(up, cxp, 0), np.where(up, czp, 0))
    cf = np.clip(1 - tc / 900, 0, 1) ** 0.6
    skyc[cm] = skyc[cm] * (1 - 0.82 * cf[cm, None]) + np.array([248, 248, 252]) * 0.82 * cf[cm, None]
    fogd = np.clip((np.where(hit, hit_t, maxt) - maxt * 0.62) / (maxt * 0.38), 0, 1)
    horiz = FOG[None, :] + (SKY - FOG)[None, :] * np.clip(el * 2.4, 0, 1)[:, None] ** 0.7
    col = np.where(hit[:, None], col * (1 - fogd[:, None]) + horiz * fogd[:, None], skyc)
    return Image.fromarray(np.clip(col.reshape(h, w, 3), 0, 255).astype(np.uint8))


def _uv(hp, ax, stp):
    fx, fy, fz = hp[:, 0] % 1, hp[:, 1] % 1, hp[:, 2] % 1
    u = np.where(ax == 1, fx, np.where(ax == 0, fz, fx))
    v = np.where(ax == 1, fz, 1 - fy)
    return np.clip(u, 0, 0.9999), np.clip(v, 0, 0.9999)


def _ao(W, vox, ax, stp, hp):
    """The game's smooth lighting at the face's corners: the blocks beside and across the corner darken it."""
    n = np.zeros((len(vox), 3), int)
    n[np.arange(len(vox)), ax] = -stp[np.arange(len(vox)), ax]
    c = vox + n
    t1 = np.where(ax == 0, 2, 0)
    t2 = np.where(ax == 1, 2, 1)
    E1 = np.eye(3, dtype=int)[t1]
    E2 = np.eye(3, dtype=int)[t2]

    def occ(q):
        q = np.clip(q, 0, [N - 1, HY - 1, N - 1])
        b = W[q[:, 0], q[:, 2], q[:, 1]]
        return ((b != 0) & (b != 5)).astype(int)
    s = {}
    for a in (-1, 1):
        s[("1", a)] = occ(c + a * E1)
        s[("2", a)] = occ(c + a * E2)
    lev = {}
    for a in (-1, 1):
        for bb in (-1, 1):
            corner = occ(c + a * E1 + bb * E2)
            s1, s2 = s[("1", a)], s[("2", bb)]
            l = np.where((s1 == 1) & (s2 == 1), 0, 3 - s1 - s2 - corner)
            lev[(a, bb)] = np.array([0.42, 0.6, 0.8, 1.0])[l]
    fu = np.take_along_axis(hp, t1[:, None], 1)[:, 0] % 1
    fv = np.take_along_axis(hp, t2[:, None], 1)[:, 0] % 1
    return (lev[(-1, -1)] * (1 - fu) * (1 - fv) + lev[(1, -1)] * fu * (1 - fv) + lev[(-1, 1)] * (1 - fu) * fv + lev[(1, 1)] * fu * fv)


# ------------------------------------------------------------------ the scenarios
def look(pos, target):
    dx, dy, dz = (target[i] - pos[i] for i in range(3))
    yaw = math.degrees(math.atan2(dx, -dz))
    pitch = math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    return yaw, pitch


def _cam(pos, target, roll=0.0, dpitch=0.0):
    y, p = look(pos, target)
    return {"pos": pos, "yaw": y, "pitch": p + dpitch, "roll": roll}


TX, TZ = TOWER
VX, VZ = VILLAGE
PAD1 = (TX + 22 * math.cos(math.radians(30)), 67.0, TZ + 22 * math.sin(math.radians(30)))
CAMS = {
    0: dict(state="FLYING", spd=14, alt=34, wp=("WP 2", (330.0, 72.0, 215.0)),
            cam=_cam((190.5, 101.0, 300.5), (430, 92, 120), roll=-6, dpitch=-4)),
    1: dict(state="FLYING", spd=18, alt=28, wp=("WP 4", (VX + 0.5, 70.0, VZ + 0.5)),
            cam=_cam((505.5, 95.0, 455.5), (VX, 78, VZ), roll=8, dpitch=-3)),
    2: dict(state="TAKEOFF", spd=3, alt=7, wp=("WP 1", (200.0, 82.0, 440.0)),
            cam=_cam((PAD1[0] + 0.5, 74.0, PAD1[2] + 0.5), (200, 74, 440), roll=0, dpitch=-5)),
    3: dict(state="LANDING", spd=6, alt=25, wp=("PAD 1", PAD1),
            cam=_cam((372.5, 92.0, 296.5), PAD1, roll=-3, dpitch=7)),
    4: dict(state="FAULT: GPS", spd=11, alt=46, wp=None,
            cam=_cam((470.5, 134.0, 250.5), (430, 112, 60), roll=5, dpitch=-3)),
    5: dict(state="IDLE", spd=0, alt=0, wp=None,
            cam=_cam((TX - 46.5, 112.0, TZ + 64.5), (TX, 108, TZ), roll=0, dpitch=-6)),
}


# ------------------------------------------------------------------ the HUD (a blocky 5 x 7 font, as the game's own)
from importlib import import_module


@lru_cache(None)
def _glyphs():
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    return import_module("dotfont").G


def text(img, x, y, s, col, scale=1, anchor="l", shadow=True):
    """Blocky text with the game's drop shadow; anchor l, m or r on x, y the top."""
    G = _glyphs()
    s = s.upper()
    adv = 6 * scale
    wtot = len(s) * adv - scale
    if anchor == "r":
        x -= wtot
    elif anchor == "m":
        x -= wtot / 2
    d = ImageDraw.Draw(img)
    passes = ((tuple(int(c * 0.25) for c in col), scale), (col, 0)) if shadow else ((col, 0),)
    for c, off in passes:
        cx = x
        for ch in s:
            g = G.get(ch, G[" "])
            for r, row in enumerate(g):
                for k, cc in enumerate(row):
                    if cc == "#":
                        X0, Y0 = round(cx + k * scale + off), round(y + r * scale + off)
                        d.rectangle((X0, Y0, X0 + scale - 1, Y0 + scale - 1), fill=c)
            cx += adv
    return wtot


HUD = (120, 255, 150)
AMBER = (255, 210, 90)
RED = (255, 96, 84)


def _dir(az, el):
    a, e = math.radians(az), math.radians(el)
    return (math.sin(a) * math.cos(e), math.sin(e), -math.cos(a) * math.cos(e))


def _ladder(img, cam, w, h, x0=0, sc=1, full=True):
    """The horizon bar and the pitch ladder, where the world's horizon and its 10-degree lines really are."""
    d = ImageDraw.Draw(img)
    cxs, cys = w / 2, h / 2
    for k in ((-20, -10, 0, 10, 20) if full else (0,)):
        pts = project(cam, 0, [_dir(cam["yaw"] - 6, k), _dir(cam["yaw"] + 6, k)], w, h)
        if None in pts:
            continue
        (ax_, ay), (bx, by) = pts
        L = math.hypot(bx - ax_, by - ay)
        ux, uy = (bx - ax_) / L, (by - ay) / L
        t = (cxs - ax_) * ux + (cys - ay) * uy
        mx, my = ax_ + ux * t, ay + uy * t
        if not (h * 0.12 < my < h * 0.88):
            continue
        hw, gap = (w * 0.24, w * 0.06) if k == 0 else (w * 0.1, w * 0.035)
        for sg in (-1, 1):
            p0 = (x0 + mx + sg * gap * ux, my + sg * gap * uy)
            p1 = (x0 + mx + sg * hw * ux, my + sg * hw * uy)
            if k < 0:          # below the horizon: dashed
                for q in range(4):
                    a0, a1 = q / 4, q / 4 + 0.14
                    d.line((p0[0] + (p1[0] - p0[0]) * a0, p0[1] + (p1[1] - p0[1]) * a0,
                            p0[0] + (p1[0] - p0[0]) * a1, p0[1] + (p1[1] - p0[1]) * a1), fill=HUD, width=sc)
            else:
                d.line((*p0, *p1), fill=HUD, width=sc)
            if k:
                d.line((*p1, p1[0] - uy * 3 * sc * (1 if k > 0 else -1), p1[1] + ux * 3 * sc * (1 if k > 0 else -1)), fill=HUD, width=sc)
        if k and full:
            text(img, x0 + mx + (hw + 3) * ux, my + (hw + 3) * uy - 3 * sc, f"{abs(k)}", HUD, sc, shadow=False)
    # the drone's mark, fixed in the middle
    cx, cy = x0 + cxs, cys
    d.line((cx - 16 * sc, cy, cx - 6 * sc, cy, cx - 3 * sc, cy + 3 * sc, cx, cy, cx + 3 * sc, cy + 3 * sc, cx + 6 * sc, cy, cx + 16 * sc, cy),
           fill=HUD, width=sc)


def _waypoint(img, cam, w, h, x0, name, pt, sc=1):
    d = ImageDraw.Draw(img)
    v = np.array(pt) - np.array(cam["pos"])
    dist = int(np.linalg.norm(v))
    q = project(cam, 0, [v / np.linalg.norm(v)], w, h)[0]
    if q is None or not (6 < q[0] < w - 6 and 6 < q[1] < h - 6):
        return
    x, y = x0 + q[0], q[1]
    r = 5 * sc
    for k in range(sc):
        d.polygon([(x, y - r + k), (x + r - k, y), (x, y + r - k), (x - r + k, y)], outline=AMBER)
    text(img, x + r + 4 * sc, y - 3 * sc, f"{name}  {dist} M", AMBER, sc)


def _mark(marks, rect, fg, **params):
    """A HUD figure left for the game to draw live (client/LiveFaces.text): its box (pixels of the strip) and readout
    (its seed the simulator's and the figure's, so a pod and its repeater on the instructor's desk read alike)."""
    marks.append({"kind": "text", "rect": [round(v, 2) for v in rect], "fg": list(fg), "far": True, **params})


def _hud_strip(img, n, info, w, h, marks=None):
    """The drone's HUD across the three screens: the ladder and the drone's mark, the speed and height tapes, the
    heading strip, the waypoint's mark, the route map (left), the drone's state (right). With marks (a list), the
    figures that change in flight (the speed, the height, the battery, the wind) are left out and listed there for
    the game to draw live."""
    d = ImageDraw.Draw(img)
    cam = info["cam"]
    sc = 1
    _ladder(img, cam, w, h, x0=w, sc=sc)
    cy = h // 2
    for x0, label, val, unit in ((w + w * 0.11, "SPD", info["spd"], "M/S"), (2 * w - w * 0.11, "ALT", info["alt"], "M")):
        bw = w * 0.055
        d.rectangle((x0 - bw, cy - h * 0.3, x0 + bw, cy + h * 0.3), outline=HUD)
        for j in range(-6, 7):
            yy = cy + j * h * 0.045
            tick = 4 if j % 2 == 0 else 2
            if x0 < 1.5 * w:
                d.line((x0 + bw - tick, yy, x0 + bw, yy), fill=HUD)
            else:
                d.line((x0 - bw, yy, x0 - bw + tick, yy), fill=HUD)
        d.rectangle((x0 - bw - 3, cy - 6, x0 + bw + 3, cy + 6), fill=(6, 22, 12), outline=HUD)
        if marks is None:
            text(img, x0, cy - 3, f"{val}", HUD, sc, "m", shadow=False)
        else:
            spread = max(0.6, val * 0.06) if label == "SPD" else 1.4
            _mark(marks, (x0 - bw - 2, cy - 5, x0 + bw + 2, cy + 5), HUD, bg=[6, 22, 12], align=1, cap=0.7, src="value", base=val,
                  spread=round(spread, 2), period=7 if label == "SPD" else 11, min=0, seed=n * 10 + (0 if label == "SPD" else 1))
        text(img, x0, cy - h * 0.3 - 10, label, HUD, sc, "m")
        text(img, x0, cy + h * 0.3 + 3, unit, HUD, sc, "m")
    # heading strip
    hdg = cam["yaw"] % 360
    cxs = w + w / 2
    d.line((cxs - w * 0.3, 14, cxs + w * 0.3, 14), fill=HUD)
    for j in range(-30, 31):
        a = math.floor(hdg) - math.floor(hdg) % 5 + j * 5
        xx = cxs + (a - hdg) * w * 0.012
        if abs(xx - cxs) > w * 0.3:
            continue
        major = a % 30 == 0
        d.line((xx, 14, xx, 14 + (5 if major else 2)), fill=HUD)
        if major:
            lab = {0: "N", 90: "E", 180: "S", 270: "W"}.get(a % 360, f"{(a % 360) // 10:02d}")
            text(img, xx, 21, lab, HUD, sc, "m", shadow=False)
    d.rectangle((cxs - 11, 2, cxs + 11, 12), fill=(6, 22, 12), outline=HUD)
    text(img, cxs, 4, f"{round(hdg) % 360:03d}", HUD, sc, "m", shadow=False)
    if info.get("wp"):
        _waypoint(img, cam, w, h, w, *info["wp"], sc=sc)
    # the sim's name and its state, top left
    st = info["state"]
    warn = st.startswith("FAULT")
    text(img, 8, 6, f"SIM {n + 1}", (255, 255, 255), 2)
    text(img, 8, 23, st, RED if warn else HUD, sc)
    text(img, 8, 33, "T4 FAN-WING  /  LESSON 3", (220, 230, 220), sc)
    if warn:
        bx0, by0 = cxs - w * 0.28, h * 0.2
        d.rectangle((bx0, by0, cxs + w * 0.28, by0 + 29), fill=(70, 10, 10), outline=RED)
        text(img, cxs, by0 + 4, "GPS LOST", (255, 130, 120), 2, "m")
        text(img, cxs, by0 + 19, "HOLD HEIGHT - USE COMPASS", (255, 205, 195), 1, "m")
    # the route map inset (left)
    mx0, my0, mw, mh = 8, int(h * 0.6), int(w * 0.46), int(h * 0.35)
    img.paste(_map_tile(mw, mh, cam["pos"], info.get("wp"), not warn), (mx0, my0))
    d.rectangle((mx0 - 1, my0 - 1, mx0 + mw, my0 + mh), outline=HUD)
    if warn:
        text(img, mx0 + mw / 2, my0 + mh / 2 - 3, "NO FIX", RED, 1, "m")
    # the drone's state (right)
    batt = 74 + (n * 7) % 22
    lines = [(f"BATT {batt}%", HUD), (f"WIND {3 + n % 4} M/S", HUD), ("LINK  OK", HUD),
             ("GPS  LOST", RED) if warn else ("GPS  12 SAT", HUD)]
    if info.get("wp"):
        lines.insert(0, (f"NEXT {info['wp'][0]}", AMBER))
    for i, (t, c) in enumerate(lines):
        x, y = 2 * w + w - 8, h - 12 - (len(lines) - 1 - i) * 10
        if marks is not None and t.startswith(("BATT", "WIND")):
            # live: the battery running down through the flight (a fresh pack each half hour), the wind gusting
            wide = 9 * 6 * sc
            live = dict(src="value", pre="BATT ", unit="%", base=batt, rate=-0.02, cycle=1800, min=12) if t.startswith("BATT") else \
                dict(src="value", pre="WIND ", unit=" M/S", base=3 + n % 4, spread=1.6, period=9, min=0)
            _mark(marks, (x - wide - 1, y - 1, x + 1, y + 8 * sc), c, align=2, cap=round(7 / (9 * sc + 1), 3), shadow=True,
                  seed=n * 10 + (2 if t.startswith("BATT") else 3), **live)
        else:
            text(img, x, y, t, c, sc, "r")


@lru_cache(None)
def _map_full():
    """The world from above, in the colours a map item gives each block."""
    W, surf = world()
    cols = {1: (127, 178, 56), 2: (151, 109, 77), 3: (112, 112, 112), 4: (247, 233, 163), 5: (64, 64, 255), 6: (143, 119, 72),
            7: (0, 124, 0), 8: (247, 233, 163), 9: (0, 124, 0), 10: (129, 86, 49), 11: (0, 124, 0), 12: (255, 255, 255),
            13: (112, 112, 112), 14: (143, 119, 72), 15: (112, 112, 112), 16: (129, 86, 49), 17: (200, 200, 200),
            18: (209, 177, 161), 19: (135, 107, 98), 20: (229, 229, 51), 21: (76, 76, 76), 22: (112, 112, 112),
            23: (151, 109, 77), 24: (255, 255, 255), 25: (102, 153, 216), 26: (25, 25, 25), 27: (229, 229, 51),
            28: (151, 109, 77), 29: (153, 51, 51)}
    nz = (W != 0)
    topy = HY - 1 - np.argmax(nz[:, :, ::-1], axis=2)
    tb = np.take_along_axis(W, topy[..., None], 2)[..., 0]
    lut = np.zeros((256, 3), np.uint8)
    for b, c in cols.items():
        lut[b] = c
    rgb = lut[tb].astype(float)
    dzh = np.diff(topy.astype(float), axis=1, prepend=topy[:, :1])
    rgb *= np.where(dzh > 0, 1.0, np.where(dzh < 0, 0.71, 0.86))[..., None]
    return np.transpose(np.clip(rgb, 0, 255).astype(np.uint8), (1, 0, 2))      # rows z, columns x


def _map_tile(mw, mh, pos, wp=None, fix=True):
    full = _map_full()
    span = 240
    zs = int(span * mh / mw)
    x0 = max(0, min(N - span, int(pos[0] - span / 2)))
    z0 = max(0, min(N - zs, int(pos[2] - zs / 2)))
    im = Image.fromarray(full[z0:z0 + zs, x0:x0 + span]).resize((mw, mh), Image.NEAREST)
    if not fix:
        return Image.blend(im, Image.new("RGB", im.size, (30, 30, 30)), 0.55)
    d = ImageDraw.Draw(im)
    s = mw / span
    px, pz = (pos[0] - x0) * s, (pos[2] - z0) * s
    if wp:
        wx, wz = (wp[1][0] - x0) * s, (wp[1][2] - z0) * s
        d.line((px, pz, wx, wz), fill=(255, 200, 60), width=1)
        d.rectangle((wx - 1, wz - 1, wx + 1, wz + 1), outline=(255, 200, 60))
    d.polygon([(px, pz - 3), (px + 3, pz + 3), (px - 3, pz + 3)], fill=(255, 255, 255), outline=(0, 0, 0))
    return im


# ------------------------------------------------------------------ the idle simulator: its menu over the world
def _button(img, x0, y0, x1, y1, label, hot=False):
    d = ImageDraw.Draw(img)
    d.rectangle((x0, y0, x1, y1), fill=(0, 0, 0))
    d.rectangle((x0 + 1, y0 + 1, x1 - 1, y1 - 1), fill=(126, 136, 190) if hot else (111, 111, 111))
    d.rectangle((x0 + 1, y0 + 1, x1 - 1, y0 + 2), fill=(184, 194, 240) if hot else (166, 166, 166))
    d.rectangle((x0 + 1, y1 - 2, x1 - 1, y1 - 1), fill=(88, 96, 150) if hot else (80, 80, 80))
    text(img, (x0 + x1) / 2, (y0 + y1) / 2 - 3, label, (255, 255, 160) if hot else (224, 224, 224), 1, "m")


def _menu(img, n, w, h):
    cx = w + w / 2
    text(img, cx, h * 0.1, "TRI-PILLAR", (255, 255, 255), 3, "m")
    text(img, cx, h * 0.1 + 26, "FLIGHT SCHOOL", AMBER, 1, "m")
    bw, bh = w * 0.56, 15
    for i, (lab, hot) in enumerate((("FREE FLIGHT", True), ("LESSONS", False), ("SCENARIOS", False), ("SETTINGS", False))):
        y0 = h * 0.4 + i * (bh + 5)
        _button(img, cx - bw / 2, y0, cx + bw / 2, y0 + bh, lab, hot)
    text(img, w + 6, h - 12, f"SIM {n + 1}  -  FREE", (214, 222, 230), 1)
    text(img, 2 * w - 6, h - 12, "V 2.4", (214, 222, 230), 1, "r")
    text(img, w / 2, h - 12, "PUT THE HEADSET ON", (214, 222, 230), 1, "m")
    text(img, 2 * w + w / 2, h - 12, "PRESS START", (214, 222, 230), 1, "m")


# ------------------------------------------------------------------ the outputs
def _cached(name, make):
    """A picture and its live marks, made once (make() gives both) and kept in CACHE: the generators then need no
    game textures."""
    import json
    CACHE.mkdir(exist_ok=True)
    f, j = CACHE / f"{name}.png", CACHE / f"{name}.json"
    if f.exists() and j.exists():
        return Image.open(f).convert("RGB"), json.loads(j.read_text())
    img, marks = make()
    img.save(f)
    j.write_text(json.dumps(marks) + "\n")
    return img, marks


def pod(n, w=320, h=188, live=False):
    """Simulator n's three screens side by side (left, centre, right), HUD drawn. live: its changing figures left out,
    and returned with it as (strip, marks) (pixels of the strip; tools/room_extra_kit.py puts them on the screens)."""
    if not live:
        CACHE.mkdir(exist_ok=True)
        f = CACHE / f"pod_{n}_{w}x{h}.png"
        if f.exists():
            return Image.open(f).convert("RGB")
        strip = _pod(n, w, h, None)
        strip.save(f)
        return strip

    def make():
        marks = []
        return _pod(n, w, h, marks), marks
    return _cached(f"pod_{n}_{w}x{h}_live", make)


def _pod(n, w, h, marks):
    info = CAMS[n]
    cam = info["cam"]
    strip = Image.new("RGB", (3 * w, h))
    for i, dy in enumerate((-60, 0, 60)):
        strip.paste(render(cam, w, h, 60, head=dy), (i * w, 0))
    if info["state"] == "IDLE":
        strip = strip.filter(ImageFilter.GaussianBlur(2.4))
        strip = Image.blend(strip, Image.new("RGB", strip.size, (20, 24, 30)), 0.25)
        _menu(strip, n, w, h)
    else:
        _hud_strip(strip, n, info, w, h, marks)
    return strip


def repeater(n, w=240, h=134, live=False):
    """The instructor's repeater of simulator n: its centre screen, the HUD's main lines. live: as pod's (the speed
    and the height left out, returned as (picture, marks))."""
    if live:
        def make():
            marks = []
            return _repeater(n, w, h, marks), marks
        return _cached(f"rep_{n}_{w}x{h}_live", make)
    CACHE.mkdir(exist_ok=True)
    f = CACHE / f"rep_{n}_{w}x{h}.png"
    if f.exists():
        return Image.open(f).convert("RGB")
    img = _repeater(n, w, h, None)
    img.save(f)
    return img


def _repeater(n, w, h, marks):
    info = CAMS[n]
    img = render(info["cam"], w, h, 60)
    u = max(1, round(h / 134))
    if info["state"] == "IDLE":
        img = img.filter(ImageFilter.GaussianBlur(2.0 * u))
        img = Image.blend(img, Image.new("RGB", img.size, (20, 24, 30)), 0.3)
        text(img, w / 2, h * 0.3, "TRI-PILLAR", (255, 255, 255), 2 * u, "m")
        text(img, w / 2, h * 0.3 + 20 * u, "FLIGHT SCHOOL", AMBER, u, "m")
        text(img, w / 2, h * 0.72, f"SIM {n + 1}  -  FREE", (214, 222, 230), u, "m")
    else:
        _ladder(img, info["cam"], w, h, 0, u, full=False)
        warn = info["state"].startswith("FAULT")
        text(img, 6 * u, 5 * u, f"SIM {n + 1}", (255, 255, 255), 2 * u)
        text(img, w - 6 * u, 5 * u, info["state"], RED if warn else (255, 255, 255), u, "r")
        if marks is None:
            text(img, 6 * u, h - 12 * u, f"SPD {info['spd']}", HUD, u)
            text(img, w - 6 * u, h - 12 * u, f"ALT {info['alt']}", HUD, u, "r")
        else:
            # live: the speed and the height (as the pilot's own HUD has them)
            wide, y = 7 * 6 * u, h - 12 * u
            cap = round(7 * u / (9 * u + 1), 3)
            _mark(marks, (6 * u - 1, y - 1, 6 * u + wide + 1, y + 8 * u), HUD, align=0, cap=cap, shadow=True, src="value", pre="SPD ",
                  base=info["spd"], spread=round(max(0.6, info["spd"] * 0.06), 2), period=7, min=0, seed=n * 10)
            _mark(marks, (w - 6 * u - wide - 1, y - 1, w - 6 * u + 1, y + 8 * u), HUD, align=2, cap=cap, shadow=True, src="value", pre="ALT ",
                  base=info["alt"], spread=1.4, period=11, min=0, seed=n * 10 + 1)
        if info.get("wp"):
            _waypoint(img, info["cam"], w, h, 0, *info["wp"], sc=u)
    return img


if __name__ == "__main__":
    import sys, time
    out = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    t0 = time.time()
    world()
    print("world", round(time.time() - t0, 1), "s")
    for n in range(6):
        t0 = time.time()
        pod(n).save(out / f"simpod_{n}.png")
        repeater(n).save(out / f"simrep_{n}.png")
        print(n, round(time.time() - t0, 1), "s")
