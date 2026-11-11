"""The Tower Diorama (Justin's walk through of 24 October 2026, redesign 3: the hall's small hologram table "does nothing";
take 2 approved on 25 October: "a diorama of the tower and generic Minecraft surroundings, something still that doesn't
move and reflects true colors", 10 chunks round the tower, the square plot, always the finished tower).

A still scale model drawn on the hall's 3x3 hologram table (client/HoloMapRenderer, client/TowerDiorama): the finished
Tri-Pillar (this tree's generated data, every tier) in 2-block voxels, each the average colour of its blocks' textures,
standing in a square plot of made-up Minecraft land 20 chunks across (plains, a river with sandy banks through a gorge,
a lake, oak and birch woods, a spruce mountain with a snowy top, a gravel path from the tower's south door), cut at its
four sides so the soil shows (grass, dirt, stone, deepslate; no ores), on a thin dark plate. One scale across and up:
1 block on the table is 116 in the world. Lit by the room like any block; nothing moves and nothing is scanned.

Writes:
- assets/jugcraft/tower_mesh/tower_diorama.bin.gz: the quads in the table anchor's (middle cell's) own space, by kind
  (solid, drawn cut out; water, see-through) and by the way they face (+x, -x, +y, -y, +z, -z), each run sorted by
  where its plane is (so the renderer draws only those turned to the camera). Per quad four corners x y z (shorts,
  1/4096 block) u v (unsigned shorts over the texture). Header: "JTDM", version 1.
- assets/jugcraft/textures/entity/tower_diorama.png: its texture: a palette (one texel a colour) and the four cut
  sides, one texel a 2-block column across and a block up, clear over the land's top.

Run by tools/tower_tripillar.py after it writes the tower's data (the model follows the tower's design), or alone.
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from collections import deque
from functools import lru_cache
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
TOWER = ROOT / "src/main/resources/data/jugcraft/drone_tower/tower_tripillar.json.gz"
MESH = ASSETS / "tower_mesh/tower_diorama.bin.gz"
TEXTURE = ASSETS / "textures/entity/tower_diorama.png"

TOP = 13 / 16                      # the table's top over its cells (tower/../HoloTableBlock.HEIGHT)
PLOT = 2.75                        # the plot (blocks on the table), 1 px in from the table's edge each side
WORLD = 320                        # world blocks across it: 10 chunks round the tower
SC = PLOT / WORLD                  # table blocks per world block
CELL = 2                           # world blocks per model cell and voxel
BASE = -28                         # world y of the plot's bottom (the foot of its cut)
LIFT = 0.012                       # the plate's thickness: the soil stands on it
N = WORLD // CELL                  # cells across
TEX_W, TEX_H = 512, 256
PALETTE_X, PALETTE_Y, PALETTE_SIDE = 352, 0, 64      # one texel a colour
SOLID, WATER = 0, 1
DIRS = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
MAGIC = 0x4A54444D                 # "JTDM"

# The land's colours: the vanilla textures' averages (alpha weighted, an animated strip's first frame), tinted as the
# game tints them in plains (grass 91BD59, foliage 77AB2F, birch 80A755, spruce 619961, water 3F76E4). Worked out once
# from the 26.3 client's textures (they are not in this repository).
LAND = {
    "grass": (83, 109, 51), "dirt": (134, 96, 67), "stone": (125, 125, 125), "deepslate": (80, 80, 82),
    "sand": (219, 207, 163), "gravel": (131, 127, 126), "snow": (249, 254, 254), "oak": (67, 96, 26),
    "birch": (65, 84, 43), "spruce": (47, 75, 47), "water": (43, 81, 158), "grass_side": (126, 107, 65),
}
WATER_ALPHA = 205
PLATE = (34, 36, 40)


# ------------------------------------------------------------------ the tower and its colours

def finished_tower():
    """The finished tower's blocks (core-relative position -> state): the foundation and every tier, as built."""
    with gzip.open(TOWER, "rt") as f:
        d = json.load(f)
    pal, blocks = d["palette"], {}
    for t in [d["foundation"]] + d["tiers"]:
        for x, y, z in t["clear"]:
            blocks.pop((x, y, z), None)
        for x, y, z, i in t["place"]:
            blocks[(x, y, z)] = pal[i]
    return blocks


PALE = (176, 192, 204)              # what shows through glass on the model: a pale sky


@lru_cache(maxsize=None)
def _json(path):
    return json.loads(path.read_text()) if path.exists() else None


@lru_cache(maxsize=None)
def _model(model, depth=0):
    """A block model's textures and elements, its parents' filling in what it leaves out (vanilla parents are not in
    this repository: a model built on one has textures but no elements)."""
    ns, path = model.split(":") if ":" in model else ("minecraft", model)
    j = _json(ROOT / f"src/main/resources/assets/{ns}/models/{path}.json")
    if j is None:
        return {}, None
    tex, elements = _model(j["parent"], depth + 1) if "parent" in j and depth < 8 else ({}, None)
    tex = dict(tex)
    for k, v in j.get("textures", {}).items():
        tex[k] = v["sprite"] if isinstance(v, dict) else v
    return tex, j.get("elements", elements)


_IMAGES = {}


def _image(ref, tex):
    for _ in range(8):
        if ref and ref.startswith("#"):
            ref = tex.get(ref[1:])
    if not ref or ":" not in ref:
        return None
    ns, path = ref.split(":")
    png = ROOT / f"src/main/resources/assets/{ns}/textures/{path}.png"
    if png not in _IMAGES:
        if not png.exists():
            _IMAGES[png] = None
        else:
            im = np.asarray(Image.open(png).convert("RGBA"), np.float32) / 255
            _IMAGES[png] = im[:im.shape[1]] if im.shape[0] > im.shape[1] else im     # an animated strip: its first frame
    return _IMAGES[png]


_SEEN = {}


def _seen(im, uv=(0, 0, 16, 16)):
    """What a face shows on the model: its texels' average, clear ones left out and see-through ones over a pale sky.
    Returns (sum of colours, texels)."""
    key = (id(im), tuple(uv))
    if key not in _SEEN:
        _SEEN[key] = _seen_now(im, uv)
    return _SEEN[key]


def _seen_now(im, uv):
    h, w = im.shape[:2]
    u0, u1 = sorted((uv[0], uv[2]))
    v0, v1 = sorted((uv[1], uv[3]))
    x0, x1 = int(u0 / 16 * w), max(int(u0 / 16 * w) + 1, int(math.ceil(u1 / 16 * w)))
    y0, y1 = int(v0 / 16 * h), max(int(v0 / 16 * h) + 1, int(math.ceil(v1 / 16 * h)))
    px = im[max(0, y0):min(h, y1), max(0, x0):min(w, x1)].reshape(-1, 4)
    px = px[px[:, 3] >= 0.04]
    if len(px) == 0:
        return None
    a = px[:, 3:]
    c = px[:, :3] * a + np.array(PALE, np.float32) / 255 * (1 - a)
    return c.sum(0), len(px)


_COLOURS = {}


def _props(state):
    return dict(kv.split("=") for kv in state[state.index("[") + 1:-1].split(",")) if "[" in state else {}


def _matches(cond, props):
    """A blockstate variant key ("a=1,b=2") or multipart "when" (with OR lists and "x|y" values) against a state's properties."""
    if isinstance(cond, str):
        return all(props.get(k) == v for k, v in (kv.split("=") for kv in cond.split(",") if kv))
    if "OR" in cond:
        return any(_matches(c, props) for c in cond["OR"])
    if "AND" in cond:
        return all(_matches(c, props) for c in cond["AND"])
    return all(props.get(k) in str(v).split("|") for k, v in cond.items())


def _state_models(state):
    """The models the game draws for a state: its variant's, or every multipart piece that applies."""
    ns, n = state.split("[")[0].split(":")
    j = _json(ROOT / f"src/main/resources/assets/{ns}/blockstates/{n}.json")
    if j is None:
        return []
    props = _props(state)
    if "variants" in j:
        hit = next((v for k, v in j["variants"].items() if _matches(k, props)), next(iter(j["variants"].values())))
        return [(hit[0] if isinstance(hit, list) else hit)["model"]]
    out = []
    for part in j["multipart"]:
        if "when" not in part or _matches(part["when"], props):
            a = part["apply"]
            out.append((a[0] if isinstance(a, list) else a)["model"])
    return out


@lru_cache(maxsize=None)
def _model_seen(model):
    """What a model shows: (sum of its faces' colours, each times its size; their total size)."""
    tex, elements = _model(model)
    total, weight = np.zeros(3), 0.0
    for e in elements or ():
        f, t = e["from"], e["to"]
        dx, dy, dz = abs(t[0] - f[0]), abs(t[1] - f[1]), abs(t[2] - f[2])
        for side, face in e.get("faces", {}).items():
            area = {"up": dx * dz, "down": dx * dz, "north": dx * dy, "south": dx * dy}.get(side, dz * dy)
            im = _image(face.get("texture"), tex)
            if im is None or area <= 0:
                continue
            default = {"up": (f[0], f[2], t[0], t[2]), "down": (f[0], f[2], t[0], t[2]),
                       "north": (f[0], 16 - t[1], t[0], 16 - f[1]), "south": (f[0], 16 - t[1], t[0], 16 - f[1])}
            seen = _seen(im, face.get("uv", default.get(side, (f[2], 16 - t[1], t[2], 16 - f[1]))))
            if seen is not None:
                total += seen[0] / seen[1] * area
                weight += area
    if not elements:
        pick = next((tex[k] for k in ("top", "all", "up", "end", "side", "texture", "particle") if k in tex), None)
        im = _image(pick if pick is not None else next(iter(tex.values()), None), tex)
        seen = None if im is None else _seen(im)
        if seen is not None:
            total, weight = seen[0] / seen[1] * 256, 256.0
    return total, weight


_COLOURS = {}


def block_colour(state):
    """A block's colour on the model: what the game draws for its state shows, every face weighted by its size (a model
    with no elements of its own: its texture, top, all, side ...)."""
    if state not in _COLOURS:
        total, weight = np.zeros(3), 0.0
        for model in _state_models(state):
            t, w = _model_seen(model)
            total, weight = total + t, weight + w
        _COLOURS[state] = tuple(int(round(v)) for v in total / weight * 255) if weight > 0 else (128, 128, 130)
    return _COLOURS[state]


def _noise(*vals):
    """A well mixed 16-bit hash of some integers (the cut's blocks a shade apart)."""
    h = 0x811C9DC5
    for v in vals:
        h = ((h ^ (int(v) & 0xFFFFFFFF)) * 0x01000193) & 0xFFFFFFFF
        h ^= h >> 15
        h = (h * 0x2C1B3C6D) & 0xFFFFFFFF
        h ^= h >> 12
    return h & 0xFFFF


# ------------------------------------------------------------------ the land (made up, the same in every world)

def land():
    """Per cell: the ground (world y of its top block), its surface, the water's level (nan where dry), the trees'
    tops (-inf where none) and kinds; cells row by row from the north-west, x first."""
    c = (np.arange(N) - (N - 1) / 2) * CELL
    X, Z = np.meshgrid(c, c, indexing="ij")
    rng = np.random.default_rng(3)
    h = np.zeros_like(X)
    for k, amp in ((0.018, 3.0), (0.04, 1.6), (0.09, 0.8)):
        ph = rng.uniform(0, 6.28, 3)
        h += amp * np.sin(k * X + ph[0]) * np.cos(k * 0.9 * Z + ph[1]) + amp * 0.5 * np.sin(k * 1.4 * (X - Z) + ph[2])
    h += 4
    h += 50 * np.exp(-(((X + 118) / 40) ** 2 + ((Z + 112) / 36) ** 2)) + 22 * np.exp(-(((X + 60) / 30) ** 2 + ((Z + 140) / 22) ** 2))
    h += 12 * np.exp(-(((X - 110) / 42) ** 2 + ((Z + 95) / 34) ** 2))
    # the river: in from the north, round the east of the site, out at the south-east corner; a lake to the south-west
    t = (Z + 160) / 320
    rd = np.abs(X - (70 + 38 * np.sin(t * 5.2) + 30 * t))
    river, bank = rd < 4.5, (rd < 8) & (rd >= 4.5)
    lake = (X + 98) ** 2 + (Z - 96) ** 2 < 24 ** 2
    beach = ((X + 98) ** 2 + (Z - 96) ** 2 < 29 ** 2) & ~lake
    r = np.hypot(X, Z)
    ease = np.clip((r - 66) / 20, 0, 1)
    h = h * ease * ease * (3 - 2 * ease)                  # the tower's site: flat at its floor, eased into the land
    wet = river | lake
    h = np.where(wet, np.where(lake, -3, -2), h)
    h = np.where(bank | beach, np.minimum(h, 1.0), h)
    h = np.round(h)
    water = np.where(wet, 1.0, np.nan)
    kind = np.full(X.shape, "grass", object)
    kind[(bank | beach) & (h <= 1)] = "sand"
    kind[wet] = "sand"
    kind[h >= 24] = "stone"
    kind[h >= 40] = "snow"
    kind[(np.abs(X - 2 - 6 * np.sin(Z / 30)) < 2.2) & (Z > 60) & ~wet] = "gravel"
    tree = np.full(X.shape, -np.inf)
    tkind = np.full(X.shape, "", object)
    woods = [(95, 110, 34, "oak"), (120, 60, 22, "birch"), (-30, 128, 26, "oak"), (-130, 30, 26, "birch"), (40, -125, 24, "oak"),
             (-115, -85, 44, "spruce"), (-70, -130, 26, "spruce")]
    for wx, wz, wr, wk in woods:
        for _ in range(int(wr * wr * 0.09)):
            a, d = rng.uniform(0, 6.283), wr * math.sqrt(rng.uniform(0, 1))
            _plant(X, Z, h, kind, tree, tkind, wx + d * math.cos(a), wz + d * math.sin(a), wk, rng, r)
    for _ in range(70):
        a, d = rng.uniform(0, 6.283), rng.uniform(80, 220)
        x, z = d * math.cos(a), d * math.sin(a)
        if abs(x) < 158 and abs(z) < 158:
            _plant(X, Z, h, kind, tree, tkind, x, z, "oak", rng, r)
    return X, Z, h, kind, water, tree, tkind


def _plant(X, Z, h, kind, tree, tkind, x, z, wk, rng, r):
    i, j = int(round((x - X[0, 0]) / CELL)), int(round((z - Z[0, 0]) / CELL))
    if not (1 <= i < N - 1 and 1 <= j < N - 1) or kind[i, j] not in ("grass", "stone") or r[i, j] < 74:
        return
    if wk == "spruce" and h[i, j] >= 40:
        return
    top = h[i, j] + {"oak": rng.integers(5, 8), "birch": rng.integers(6, 9), "spruce": rng.integers(8, 12)}[wk]
    for di in (-1, 0, 1):
        for dj in (-1, 0, 1):
            if wk == "spruce" and di and dj:
                continue
            drop = 0 if (di == 0 and dj == 0) else (2 if wk != "spruce" else 4)
            if top - drop > max(h[i + di, j + dj], tree[i + di, j + dj]):
                tree[i + di, j + dj] = top - drop
                tkind[i + di, j + dj] = wk


def column_layers(g, tree, tk, kind):
    """A land column's layers top down to the plot's foot: (y0, y1, colour); its top block at g."""
    gt = g + 1
    out = []
    if np.isfinite(tree) and tree + 1 > gt:
        out.append((gt, tree + 1, LAND[tk]))            # a crown, down to the ground (a 2-block cell)
    out += {"grass": [(gt - 1, gt, LAND["grass_side"]), (gt - 4, gt - 1, LAND["dirt"])],
            "snow": [(gt - 1, gt, LAND["snow"]), (gt - 3, gt - 1, LAND["stone"])],
            "sand": [(gt - 4, gt, LAND["sand"])],
            "gravel": [(gt - 1, gt, LAND["gravel"]), (gt - 4, gt - 1, LAND["dirt"])]}.get(kind, [(gt - 4, gt, LAND["stone"])])
    bottom = min(l[0] for l in out)
    return out + [(-12, bottom, LAND["stone"]), (BASE, -12, LAND["deepslate"])]


# ------------------------------------------------------------------ the mesh

class Model:
    """Quads by kind and direction: corners (anchor space) and uvs (texels)."""

    def __init__(self):
        self.quads = {(k, d): [] for k in (SOLID, WATER) for d in range(6)}
        self.palette = {}

    def colour(self, rgb, alpha=255):
        key = tuple(int(v) for v in rgb) + (alpha,)
        if key not in self.palette:
            i = len(self.palette)
            if i >= PALETTE_SIDE * PALETTE_SIDE:
                raise ValueError("palette full")
            self.palette[key] = (PALETTE_X + i % PALETTE_SIDE + 0.5, PALETTE_Y + i // PALETTE_SIDE + 0.5)
        return self.palette[key]

    def add(self, kind, d, corners, uvs):
        """A quad facing DIRS[d]; corners in world blocks (core-relative), wound to face it."""
        p = [(0.5 + x * SC, TOP + LIFT + (y - BASE) * SC, 0.5 + z * SC) for x, y, z in corners]
        n = np.cross(np.subtract(p[1], p[0]), np.subtract(p[2], p[0]))
        if float(np.dot(n, DIRS[d])) < 0:
            p, uvs = p[::-1], uvs[::-1]
        self.quads[(kind, d)].append((p, uvs))

    def flat(self, kind, d, corners, rgb, alpha=255):
        uv = self.colour(rgb, alpha)
        self.add(kind, d, corners, [uv] * 4)

    def count(self):
        return sum(len(v) for v in self.quads.values())


def _rects(mask, key):
    """Greedy rectangles over a 2D mask whose cells share a key: (a0, a1, b0, b1) half-open."""
    used = np.zeros_like(mask)
    A, B = mask.shape
    for a in range(A):
        for b in range(B):
            if not mask[a, b] or used[a, b]:
                continue
            k = key[a][b]
            e = b
            while e + 1 < B and mask[a, e + 1] and not used[a, e + 1] and key[a][e + 1] == k:
                e += 1
            f = a
            while f + 1 < A and all(mask[f + 1, t] and not used[f + 1, t] and key[f + 1][t] == k for t in range(b, e + 1)):
                f += 1
            used[a:f + 1, b:e + 1] = True
            yield a, f + 1, b, e + 1, k


def tower_voxels(blocks):
    """The tower in voxels: solid where 2 of a voxel's 8 blocks are filled, its colour their average (to 8s); the
    voxel box's origin, and which empty voxels are the air outside (the ground below the land's top counts filled)."""
    xs = [p[0] for p in blocks]; ys = [p[1] for p in blocks]; zs = [p[2] for p in blocks]
    x0, y0, z0 = (min(xs) // CELL - 1) * CELL, (min(ys) // CELL - 1) * CELL, (min(zs) // CELL - 1) * CELL
    nx, ny, nz = (max(xs) - x0) // CELL + 2, (max(ys) - y0) // CELL + 2, (max(zs) - z0) // CELL + 2
    cnt = np.zeros((nx, ny, nz), np.int16)
    rgb = np.zeros((nx, ny, nz, 3), np.float32)
    for (x, y, z), s in blocks.items():
        i, j, k = (x - x0) // CELL, (y - y0) // CELL, (z - z0) // CELL
        cnt[i, j, k] += 1
        rgb[i, j, k] += block_colour(s)
    solid = cnt >= 2
    colour = (np.round(rgb / np.maximum(cnt, 1)[..., None] / 8) * 8).clip(0, 248).astype(int)
    ground = np.zeros_like(solid)
    ground[:, :max(0, (0 - y0) // CELL), :] = True     # layers wholly under the land's top (y 0 and below)
    filled = solid | ground
    outside = np.zeros_like(solid)
    todo = deque()
    for i in range(nx):
        for k in range(nz):
            j = ny - 1
            if not filled[i, j, k]:
                outside[i, j, k] = True
                todo.append((i, j, k))
    for i in (0, nx - 1):
        for j in range(ny):
            for k in range(nz):
                if not filled[i, j, k] and not outside[i, j, k]:
                    outside[i, j, k] = True
                    todo.append((i, j, k))
    for k in (0, nz - 1):
        for i in range(nx):
            for j in range(ny):
                if not filled[i, j, k] and not outside[i, j, k]:
                    outside[i, j, k] = True
                    todo.append((i, j, k))
    while todo:
        i, j, k = todo.popleft()
        for di, dj, dk in DIRS:
            a, b, c = i + di, j + dj, k + dk
            if 0 <= a < nx and 0 <= b < ny and 0 <= c < nz and not filled[a, b, c] and not outside[a, b, c]:
                outside[a, b, c] = True
                todo.append((a, b, c))
    return (x0, y0, z0), solid, colour, outside


def add_tower(m, blocks):
    (x0, y0, z0), solid, colour, outside = tower_voxels(blocks)
    shape = solid.shape
    keys = [[[tuple(colour[i, j, k]) for k in range(shape[2])] for j in range(shape[1])] for i in range(shape[0])]
    for d, (dx, dy, dz) in enumerate(DIRS):
        nb = np.zeros_like(outside)
        src = [slice(None)] * 3
        dst = [slice(None)] * 3
        axis = 0 if dx else 1 if dy else 2
        step = dx or dy or dz
        if step > 0:
            dst[axis], src[axis] = slice(0, shape[axis] - 1), slice(1, None)
        else:
            dst[axis], src[axis] = slice(1, None), slice(0, shape[axis] - 1)
        nb[tuple(dst)] = outside[tuple(src)]
        faces = solid & nb
        for layer in range(shape[axis]):
            sl = [slice(None)] * 3
            sl[axis] = layer
            mask = faces[tuple(sl)]
            if not mask.any():
                continue
            if axis == 0:
                key = keys[layer]
            elif axis == 1:
                key = [[keys[i][layer][k] for k in range(shape[2])] for i in range(shape[0])]
            else:
                key = [[keys[i][j][layer] for j in range(shape[1])] for i in range(shape[0])]
            plane = layer + (1 if step > 0 else 0)
            for a0, a1, b0, b1, rgb in _rects(mask, key):
                corners = []
                for ua, ub in ((a0, b0), (a1, b0), (a1, b1), (a0, b1)):
                    idx = [0, 0, 0]
                    idx[axis] = plane
                    o = [t for t in range(3) if t != axis]
                    idx[o[0]], idx[o[1]] = ua, ub
                    corners.append((x0 + idx[0] * CELL, y0 + idx[1] * CELL, z0 + idx[2] * CELL))
                m.flat(SOLID, d, corners, rgb)
    # which land cells the tower stands on (its voxels over the land's top): the land's top is not drawn there
    covered = np.zeros((N, N), bool)
    j0 = (0 - y0) // CELL
    for i in range(shape[0]):
        for k in range(shape[2]):
            if solid[i, j0, k]:
                ci, ck = (x0 + i * CELL + WORLD // 2) // CELL, (z0 + k * CELL + WORLD // 2) // CELL
                if 0 <= ci < N and 0 <= ck < N:
                    covered[ci, ck] = True
    return covered


def add_land(m, covered, tex):
    X, Z, h, kind, water, tree, tkind = land()
    lt = np.where(np.isfinite(tree) & (tree > h), tree + 1, h + 1).astype(int)       # each column's top (block tops)
    col = [[LAND[tkind[i, j]] if np.isfinite(tree[i, j]) and tree[i, j] > h[i, j] else LAND[kind[i, j]] for j in range(N)] for i in range(N)]
    # tops (where the tower does not stand), merged where height and colour run on
    key = [[(int(lt[i, j]), col[i][j]) for j in range(N)] for i in range(N)]
    for a0, a1, b0, b1, (y, rgb) in _rects(~covered, key):
        x0, x1 = -WORLD / 2 + a0 * CELL, -WORLD / 2 + a1 * CELL
        z0, z1 = -WORLD / 2 + b0 * CELL, -WORLD / 2 + b1 * CELL
        m.flat(SOLID, 2, [(x0, y, z0), (x1, y, z0), (x1, y, z1), (x0, y, z1)], rgb)
    # the steps between cells, in their layers, merged along each run
    for d, (dx, _, dz) in ((0, DIRS[0]), (1, DIRS[1]), (4, DIRS[4]), (5, DIRS[5])):
        runs = {}
        for i in range(N):
            for j in range(N):
                a, b = i + dx, j + dz
                if not (0 <= a < N and 0 <= b < N) or lt[a, b] >= lt[i, j] or covered[i, j]:
                    continue
                for y0, y1, rgb in column_layers(h[i, j], tree[i, j], tkind[i, j], kind[i, j]):
                    lo, hi = max(lt[a, b], y0), min(lt[i, j], y1)
                    if hi > lo:
                        fixed, run = (i, j) if dx else (j, i)
                        runs.setdefault((fixed, lo, hi, rgb), []).append(run)
        for (fixed, lo, hi, rgb), along in runs.items():
            along.sort()
            s = along[0]
            for t, nxt in zip(along, along[1:] + [None]):
                if nxt == t + 1:
                    continue
                if dx:
                    x = -WORLD / 2 + (fixed + (1 if dx > 0 else 0)) * CELL
                    z0, z1 = -WORLD / 2 + s * CELL, -WORLD / 2 + (t + 1) * CELL
                    m.flat(SOLID, d, [(x, lo, z0), (x, hi, z0), (x, hi, z1), (x, lo, z1)], rgb)
                else:
                    z = -WORLD / 2 + (fixed + (1 if dz > 0 else 0)) * CELL
                    x0, x1 = -WORLD / 2 + s * CELL, -WORLD / 2 + (t + 1) * CELL
                    m.flat(SOLID, d, [(x0, lo, z), (x0, hi, z), (x1, hi, z), (x1, lo, z)], rgb)
                if nxt is not None:
                    s = nxt
    # water: a sheet a little under its banks
    wet = ~np.isnan(water)
    key = [[float(water[i, j]) if wet[i, j] else 0.0 for j in range(N)] for i in range(N)]
    for a0, a1, b0, b1, level in _rects(wet, key):
        y = level + 1 - 0.15
        x0, x1 = -WORLD / 2 + a0 * CELL, -WORLD / 2 + a1 * CELL
        z0, z1 = -WORLD / 2 + b0 * CELL, -WORLD / 2 + b1 * CELL
        m.flat(WATER, 2, [(x0, y, z0), (x1, y, z0), (x1, y, z1), (x0, y, z1)], LAND["water"], WATER_ALPHA)
    # the four cut sides: one quad each, drawn from a strip of the texture (clear over the land)
    top = int(lt.max())
    rows = top - BASE
    half = WORLD / 2
    sides = ((0, "east", lambda c: (N - 1, c)), (1, "west", lambda c: (0, c)), (4, "south", lambda c: (c, N - 1)), (5, "north", lambda c: (c, 0)))
    for s, (d, name, cell) in enumerate(sides):
        u0, v0 = (s % 2) * 176, (s // 2) * 128
        if rows > 128:
            raise ValueError("cut too tall for its strip")
        for c in range(N):
            i, j = cell(c)
            for y0, y1, rgb in column_layers(h[i, j], tree[i, j], tkind[i, j], kind[i, j]) + (
                    [(int(h[i, j]) + 1, int(water[i, j]) + 2, LAND["water"])] if wet[i, j] else []):
                for y in range(int(max(BASE, y0)), int(min(top, y1))):
                    f = 0.94 + 0.12 * (_noise(i, j, y // CELL, d) / 0xFFFF)
                    tex[v0 + (top - 1 - y), u0 + c] = tuple(min(255, int(v * f)) for v in rgb) + (255,)
        uv = [(u0, v0 + rows), (u0, v0), (u0 + N, v0), (u0 + N, v0 + rows)]          # foot, top along the side's start, ...
        if d in (0, 1):
            x = half if d == 0 else -half
            corners = [(x, BASE, -half), (x, top, -half), (x, top, half), (x, BASE, half)]
        else:
            z = half if d == 4 else -half
            corners = [(-half, BASE, z), (-half, top, z), (half, top, z), (half, BASE, z)]
        m.add(SOLID, d, corners, uv)
    return lt


def add_plate(m):
    """The dark plate under the soil, a hair wider than the plot."""
    h = WORLD / 2 + 0.01 / SC
    y0, y1 = BASE - (LIFT - 0.0005) / SC, BASE
    m.flat(SOLID, 2, [(-h, y1, -h), (h, y1, -h), (h, y1, h), (-h, y1, h)], PLATE)
    m.flat(SOLID, 0, [(h, y0, -h), (h, y1, -h), (h, y1, h), (h, y0, h)], PLATE)
    m.flat(SOLID, 1, [(-h, y0, -h), (-h, y1, -h), (-h, y1, h), (-h, y0, h)], PLATE)
    m.flat(SOLID, 4, [(-h, y0, h), (-h, y1, h), (h, y1, h), (h, y0, h)], PLATE)
    m.flat(SOLID, 5, [(-h, y0, -h), (-h, y1, -h), (h, y1, -h), (h, y0, -h)], PLATE)


def build(blocks=None):
    blocks = finished_tower() if blocks is None else blocks
    m = Model()
    tex = np.zeros((TEX_H, TEX_W, 4), np.uint8)
    covered = add_tower(m, blocks)
    add_land(m, covered, tex)
    add_plate(m)
    for (r, g, b, a), (u, v) in m.palette.items():
        tex[int(v), int(u)] = (r, g, b, a)
    return m, tex


def mesh_bytes(m):
    def q(v):
        return int(round(v * 4096))
    buf = bytearray(struct.pack(">ii", MAGIC, 1))
    for kind in (SOLID, WATER):
        for d in range(6):
            axis = 0 if DIRS[d][0] else 1 if DIRS[d][1] else 2
            quads = sorted(m.quads[(kind, d)], key=lambda qd: qd[0][0][axis])
            buf += struct.pack(">i", len(quads))
            for p, uvs in quads:
                for (x, y, z), (u, v) in zip(p, uvs):
                    buf += struct.pack(">hhhHH", q(x), q(y), q(z), int(round(u / TEX_W * 65535)), int(round(v / TEX_H * 65535)))
    return bytes(buf)


def write(blocks=None):
    m, tex = build(blocks)
    import keep_same
    # The mesh's colours are places in the texture's palette, so the two are one thing: when either differs from what
    # is on disk both are written, and neither is ever to be put back alone (keep_same's note on r198).
    keep_same.gz(mesh_bytes(m), MESH, compresslevel=9)
    keep_same.png(Image.fromarray(tex, "RGBA"), TEXTURE, optimize=True)
    return m


if __name__ == "__main__":
    model = write()
    print(f"tower diorama: {model.count()} quads, {len(model.palette)} colours, mesh {MESH.stat().st_size} bytes, "
          f"texture {TEXTURE.stat().st_size} bytes")
