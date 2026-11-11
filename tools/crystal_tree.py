"""The Crystal Tree: the Tri-Pillar lobby's feature tree (work list item 2, approved by Justin on 22 October 2026 from
the third mockup sheet with a slight glow on the crystals; claude/jugcraft-tripillar-feature-tree.md).

An alien tree of our own design (Justin asked for something in the spirit of a TV show's glowing tree, better; the
show's design itself is not copied): a trunk of four dark glossy strands braided round each other, gold veins glowing in
the grooves between them; limbs that twist and wander like a real tree's, some long and low, some short, branching as
they go; gnarled roots out of the strands, diving under black basalt chippings and coming up again; faceted gold
crystals lit from within hanging on thin dark woody threads at all lengths, each with a soft glow; clumps of glowing
bead moss. Light pulses travel up the veins, out along the limbs and down through the crystals; the threads sway.
Every tree placed is its own: client/CrystalTreeGeometry grows the tree from the placed block's position (its seed),
so no two are alike; this file makes what does not change: the round planter (precast graphite wall, stainless cap,
the gravel) and the iroko bench round it with its amber under-glow, the texture atlas they and the tree share, and the
atlas's map (where each tile is) for the Java side. The mockup scripts (claude/src/dronetower/tools/mockups/lt3_model.py)
are the tree's reference.

One item places it whole (tower/CrystalTreeBlock): the master is the middle cell on the floor; its parts are the
planter's and bench's cells round it (colliding with the wall and the seat) and the trunk's cells over it.

Frame of the mesh: the master cell's corner at the origin, the planter's middle over (0.5, 0.5), the floor at y 0.
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import tower_control_console as TCC

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"

ID = "crystal_tree"
PART_ID = "crystal_tree_part"
NAME = "Crystal Tree"
MESH = ASSETS / "tower_mesh/crystal_tree_planter.bin.gz"
ATLAS_MAP = ASSETS / "tower_mesh/crystal_tree.json"
TEXTURE = ASSETS / "textures/entity/crystal_tree.png"

SOLID, LIT, GLASS, GLOW = 0, 1, 2, 3
Y = np.array([0.0, 1.0, 0.0])
CX = CZ = 0.5
AMBER = np.array([255, 176, 72], float)

# the planter and the bench (client/CrystalTreeGeometry and tower/CrystalTreeBlock use the same numbers)
R_IN, R_OUT, WALL_H = 3.33, 3.55, 0.86      # the planter wall (its inner and outer faces), its top
GRAVEL_Y = 0.70
SEAT_Y, SEAT_T = 0.46, 0.04                 # the seat's top, its slats' thickness
SEAT_R0, SEAT_R1 = 3.60, 4.12               # the seat's back and front edges
BASE_R1 = 3.92                              # the recessed base's face (a shadow under the seat's front)
SECTIONS = 16
SLATS = 6
ATLAS = (1024, 512)


# ------------------------------------------------------------------ textures

def _periodic(rnd, h, w, fy, fx, terms=10):
    """Smooth noise that tiles both ways: a sum of waves with whole numbers of periods across the tile."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    out = np.zeros((h, w), np.float32)
    for _ in range(terms):
        ky, kx = int(rnd.integers(1, fy + 1)), int(rnd.integers(0, fx + 1))
        out += rnd.uniform(0.4, 1.0) * np.sin(2 * math.pi * (ky * yy / h + kx * xx / w) + rnd.uniform(0, 2 * math.pi))
    return out / terms


def _timber():
    """Iroko slats: warm mid-brown, the grain running along u (along the slat)."""
    w, h = 256, 32
    rnd = np.random.default_rng(13)
    a = np.zeros((h, w, 4), np.float32)
    rows = rnd.uniform(-1, 1, (h, 1)) * 10 + np.sin(np.arange(h)[:, None] * 0.9) * 4
    streak = rnd.uniform(-1, 1, (h, w // 16))
    streak = np.repeat(streak, 16, 1) * 3
    base = np.array([150, 98, 58], np.float32)
    for c in range(3):
        a[..., c] = base[c] * (1 + (rows + streak + rnd.uniform(-2, 2, (h, w))) / 160)
    a[..., 3] = 255
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _rock(shade=1.0):
    rnd = np.random.default_rng(17)
    w = 64
    n = rnd.uniform(-1, 1, (w // 4, w // 4))
    n = np.asarray(Image.fromarray(((n + 1) * 127).astype(np.uint8)).resize((w, w), Image.BICUBIC), np.float32) / 127 - 1
    a = np.zeros((w, w, 4), np.float32)
    for c, b in enumerate((188, 180, 162)):
        a[..., c] = b + n * 16 + rnd.uniform(-5, 5, (w, w))
    a[..., 3] = 255
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _wash():
    a = np.zeros((16, 16, 4), np.uint8)
    for v in range(16):
        k = (1 - v / 15) ** 2.0
        a[v, :, :3] = (255, 163, 66)
        a[v, :, 3] = int(round(255 * 0.7 * k))
    return Image.fromarray(a, "RGBA")


def _dark_bark():
    """Dark glossy bark, near-black with a bronze-violet sheen and fine fibres running along it; tiles both ways."""
    w, h = 128, 256
    rnd = np.random.default_rng(41)
    n = _periodic(rnd, h, w, 3, 8, 12) * 2
    a = np.zeros((h, w, 4), np.float32)
    fib = np.repeat(rnd.uniform(-1, 1, (1, w)), h, 0) * 0.6 + rnd.uniform(-1, 1, (h, w)) * 0.4
    for c, b in enumerate((50, 44, 48)):
        a[..., c] = b + n * 7 + fib * 5
    a[..., 0] += np.clip(n, 0, 1) * 8
    a[..., 3] = 255
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _pod():
    """A seed lantern: a bright pale-gold core fading to amber at its rim."""
    w = 32
    a = np.zeros((w, w, 4), np.float32)
    for y in range(w):
        for x in range(w):
            d = math.hypot(x + 0.5 - w / 2, y + 0.5 - w / 2) / (w / 2)
            if d > 1:
                continue
            k = 1 - d
            a[y, x, :3] = AMBER * (0.8 + 0.2 * k) + np.array([0, 50, 120]) * k ** 2
            a[y, x, 3] = 255 * (0.55 + 0.45 * k)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _basalt():
    """Black basalt chippings, a few with a glassy glint; tiles both ways (0.8 m a tile)."""
    w = 128
    rnd = np.random.default_rng(43)
    img = Image.new("RGBA", (w, w), (22, 22, 24, 255))
    d = ImageDraw.Draw(img)
    for _ in range(2600):
        x, y = rnd.uniform(0, w, 2)
        r = rnd.uniform(0.7, 1.9)
        g = int(rnd.uniform(18, 62))
        glint = rnd.random() < 0.03
        col = (90, 86, 84, 255) if glint else (g, g, g + 3, 255)
        for ox in (-w, 0, w):
            for oy in (-w, 0, w):
                d.ellipse((x + ox - r, y + oy - r * 0.8, x + ox + r, y + oy + r * 0.8), fill=col)
    return img


def _crystal():
    """Gold crystal: warm translucent glass with bright streaks across it (each facet takes the whole tile, so the
    streaks fall differently on every face)."""
    w = 64
    rnd = np.random.default_rng(61)
    a = np.zeros((w, w, 4), np.float32)
    yy, xx = np.mgrid[0:w, 0:w].astype(np.float32)
    streak = np.zeros((w, w), np.float32)
    for _ in range(5):
        k = rnd.uniform(-1.5, 1.5)
        off = rnd.uniform(-w, 2 * w)
        width = rnd.uniform(1.5, 5)
        streak += np.exp(-((yy - k * xx - off) / width) ** 2) * rnd.uniform(0.4, 1.0)
    streak = np.clip(streak, 0, 1)
    edge = np.minimum.reduce([xx, yy, w - 1 - xx, w - 1 - yy]) / (w / 2)
    base = np.array([250, 186, 84], np.float32)
    hi = np.array([255, 236, 176], np.float32)
    for c in range(3):
        a[..., c] = base[c] + (hi[c] - base[c]) * np.clip(streak * 0.9 + (1 - edge) ** 4 * 0.6, 0, 1)
    a[..., 3] = 255 * np.clip(0.52 + 0.35 * streak + 0.25 * (1 - edge) ** 3, 0, 1)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _halo():
    """A soft round glow: gold, faint, fading to nothing at its rim."""
    w = 32
    a = np.zeros((w, w, 4), np.float32)
    for y in range(w):
        for x in range(w):
            d = min(1.0, math.hypot(x + 0.5 - w / 2, y + 0.5 - w / 2) / (w / 2))
            a[y, x, :3] = (255, 196, 96)
            a[y, x, 3] = 255 * 0.5 * (1 - d) ** 1.4      # (the glow pass drops what is under 0.1)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")





def tiles():
    t = {
        "dark_bark": _dark_bark(), "crystal": _crystal(), "halo": _halo(), "pod": _pod(), "basalt": _basalt(), "timber": _timber(),
        "wash": _wash(), "wall": TCC._noise((44, 46, 50), 2.5, 31, size=64), "cap": TCC._noise((184, 188, 194), 8, 32, "h", size=64),
        "base": TCC._noise((20, 21, 23), 1.2, 33, size=32), "amber": TCC._solid((255, 172, 70)), "vein": TCC._solid((255, 186, 90)),
        "moss": TCC._solid((255, 200, 120)), "stem": TCC._solid((40, 38, 36)),
    }
    a = np.asarray(_rock()).astype(np.float32)
    a[..., :3] = a[..., :3] * 0.22
    t["basalt_rock"] = Image.fromarray(a.astype(np.uint8), "RGBA")
    return t


NOISE = {"wall", "cap", "base"}


class Atlas:
    """The tiles on one sheet; uv(name, u, v) maps 0..1 within a tile, half a texel in from its edges."""

    def __init__(self, t):
        self.img = Image.new("RGBA", ATLAS, (0, 0, 0, 0))
        self.box = {}
        x = y = shelf = 0
        for name, im in sorted(t.items(), key=lambda kv: (-kv[1].height, kv[0])):
            if x + im.width > ATLAS[0]:
                x, y, shelf = 0, y + shelf + 2, 0
            if y + im.height > ATLAS[1]:
                raise ValueError("atlas full")
            self.img.paste(im, (x, y))
            self.box[name] = (x, y, im.width, im.height)
            x += im.width + 2
            shelf = max(shelf, im.height)

    def uv(self, name, u, v):
        x, y, w, h = self.box[name]
        u = min(max(u, 0.0), 1.0)
        v = min(max(v, 0.0), 1.0)
        return ((x + 0.5 + u * (w - 1)) / ATLAS[0], (y + 0.5 + v * (h - 1)) / ATLAS[1])

    def tiles_map(self):
        """Per tile, the uv rectangle its 0..1 maps to (u0, v0, u1, v1), for the Java side."""
        return {n: [*self.uv(n, 0, 0), *self.uv(n, 1, 1)] for n in sorted(self.box)}


# ------------------------------------------------------------------ the planter and the bench

def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def arc_band(m, r, y0, y1, a0, a1, mat, n, outward=True, uv=None, kind=None):
    for i in range(n):
        t0, t1 = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
        p = [(r * math.cos(t0), y1, r * math.sin(t0)), (r * math.cos(t1), y1, r * math.sin(t1)), (r * math.cos(t1), y0, r * math.sin(t1)),
             (r * math.cos(t0), y0, r * math.sin(t0))]
        want = (math.cos((t0 + t1) / 2) * (1 if outward else -1), 0, math.sin((t0 + t1) / 2) * (1 if outward else -1))
        f = None if uv is None else [(uv[0] + (uv[1] - uv[0]) * i / n, uv[2]), (uv[0] + (uv[1] - uv[0]) * (i + 1) / n, uv[2]),
                                     (uv[0] + (uv[1] - uv[0]) * (i + 1) / n, uv[3]), (uv[0] + (uv[1] - uv[0]) * i / n, uv[3])]
        m.oquad(p, want, mat, f)


def annulus(m, r0, r1, y, a0, a1, mat, n, up=True, uvf=None, lit=False):
    for i in range(n):
        t0, t1 = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
        p = [(r1 * math.cos(t0), y, r1 * math.sin(t0)), (r1 * math.cos(t1), y, r1 * math.sin(t1)), (r0 * math.cos(t1), y, r0 * math.sin(t1)),
             (r0 * math.cos(t0), y, r0 * math.sin(t0))]
        m.oquad(p, Y if up else -Y, mat, uvf(i) if uvf else None, lit=lit)


def clip_convex(poly, clip):
    """Sutherland-Hodgman: the part of a polygon inside a convex one (both anticlockwise in x, z)."""
    out = poly
    for i in range(len(clip)):
        a, b = clip[i], clip[(i + 1) % len(clip)]
        inp, out = out, []
        if not inp:
            break

        def side(p):
            return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
        for k in range(len(inp)):
            p, q = inp[k], inp[(k + 1) % len(inp)]
            sp, sq = side(p), side(q)
            if sq >= 0:
                if sp < 0:
                    t = sp / (sp - sq)
                    out.append((p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t))
                out.append(q)
            elif sp >= 0:
                t = sp / (sp - sq)
                out.append((p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t))
    return out


def planter(m, gravel="basalt"):
    n = 96
    # the wall: outer face (the bench's backrest), inner face above the gravel, the stainless cap
    arc_band(m, R_OUT, SEAT_Y, WALL_H, 0, 2 * math.pi, "wall", n)
    arc_band(m, R_IN, GRAVEL_Y - 0.02, WALL_H, 0, 2 * math.pi, "wall", n, outward=False)
    annulus(m, R_IN - 0.015, R_OUT + 0.015, WALL_H + 0.012, 0, 2 * math.pi, "cap", n)
    arc_band(m, R_OUT + 0.015, WALL_H, WALL_H + 0.012, 0, 2 * math.pi, "cap", n)
    arc_band(m, R_IN - 0.015, WALL_H, WALL_H + 0.012, 0, 2 * math.pi, "cap", n, outward=False)
    # the gravel, in rings so no quad is huge
    # the gravel: 0.4 m squares of a grid clipped to the wall's inner face, the texture laid straight on (it tiles, so
    # it runs on from square to square without a seam)
    edge = [(R_IN * math.cos(2 * math.pi * i / 96), R_IN * math.sin(2 * math.pi * i / 96)) for i in range(96)]
    G = 0.4
    n = int(math.ceil(R_IN / G))
    for i in range(-n, n):
        for j in range(-n, n):
            poly = clip_convex([(i * G, j * G), ((i + 1) * G, j * G), ((i + 1) * G, (j + 1) * G), (i * G, (j + 1) * G)], edge)
            if len(poly) < 3:
                continue
            u0, v0 = 0.4 * (i % 2), 0.4 * (j % 2)
            uv = [((x - i * G + u0) / 0.8, (z - j * G + v0) / 0.8) for x, z in poly]
            for k in range(1, len(poly) - 1):
                pts = [(poly[0][0], GRAVEL_Y, poly[0][1]), (poly[k][0], GRAVEL_Y, poly[k][1]), (poly[k + 1][0], GRAVEL_Y, poly[k + 1][1]),
                       (poly[k + 1][0], GRAVEL_Y, poly[k + 1][1])]
                m.oquad(pts, Y, gravel, [uv[0], uv[k], uv[k + 1], uv[k + 1]])


def bench(m):
    """Sixteen sections round the wall: curved slats on a recessed black steel base, a joint between sections."""
    gap = 0.012 / R_OUT
    for s in range(SECTIONS):
        a0 = 2 * math.pi * s / SECTIONS + gap / 2
        a1 = 2 * math.pi * (s + 1) / SECTIONS - gap / 2
        n = 6
        # the slats, back to front, each a curved board with rounded-off look (top, front edge, back edge)
        pitch = (SEAT_R1 - SEAT_R0) / SLATS
        for k in range(SLATS):
            r0, r1 = SEAT_R0 + k * pitch + 0.006, SEAT_R0 + (k + 1) * pitch - 0.006
            span = (a1 - a0) * (r0 + r1) / 2
            v = 0.12 + 0.12 * (k % 6)
            annulus(m, r0, r1, SEAT_Y, a0, a1, "timber", n,
                    uvf=lambda i, k=k, n=n: [((i) / n * 0.9 + 0.05, 0.1 + 0.13 * k), ((i + 1) / n * 0.9 + 0.05, 0.1 + 0.13 * k),
                                             ((i + 1) / n * 0.9 + 0.05, 0.18 + 0.13 * k), (i / n * 0.9 + 0.05, 0.18 + 0.13 * k)])
            arc_band(m, r1, SEAT_Y - SEAT_T, SEAT_Y, a0, a1, "timber", n, uv=(0.05, 0.95, 0.80, 0.86))
            arc_band(m, r0, SEAT_Y - SEAT_T, SEAT_Y, a0, a1, "timber", n, outward=False, uv=(0.05, 0.95, 0.80, 0.86))
        # the slats' ends at the joint
        for a, sgn in ((a0, -1), (a1, 1)):
            t = np.array([-math.sin(a), 0, math.cos(a)]) * sgn
            p = [(SEAT_R0 * math.cos(a), SEAT_Y, SEAT_R0 * math.sin(a)), (SEAT_R1 * math.cos(a), SEAT_Y, SEAT_R1 * math.sin(a)),
                 (SEAT_R1 * math.cos(a), SEAT_Y - SEAT_T, SEAT_R1 * math.sin(a)), (SEAT_R0 * math.cos(a), SEAT_Y - SEAT_T, SEAT_R0 * math.sin(a))]
            m.oquad(p, t, "timber", [(0.02, 0.5), (0.1, 0.5), (0.1, 0.6), (0.02, 0.6)])
    # the base: a black steel plinth set back under the seat, its top hidden; the amber LED line under the seat's front
    arc_band(m, BASE_R1, 0.0, SEAT_Y - SEAT_T, 0, 2 * math.pi, "base", 96)
    # the seat's underside: black steel, a gap for the amber LED line (so no two faces share a plane)
    under = SEAT_Y - SEAT_T - 0.005
    annulus(m, R_OUT, BASE_R1 + 0.06, under, 0, 2 * math.pi, "base", 96, up=False)
    annulus(m, BASE_R1 + 0.09, SEAT_R1, under, 0, 2 * math.pi, "base", 96, up=False)
    annulus(m, BASE_R1 + 0.06, BASE_R1 + 0.09, under - 0.004, 0, 2 * math.pi, "amber", 96, up=False, lit=True)
    arc_band(m, BASE_R1 + 0.06, under - 0.004, under, 0, 2 * math.pi, "base", 96)
    arc_band(m, BASE_R1 + 0.09, under - 0.004, under, 0, 2 * math.pi, "base", 96, outward=False)
    # the floor wash under the seat's front, fading out
    first = len(m.quads)
    annulus(m, BASE_R1, SEAT_R1 + 0.45, 0.004, 0, 2 * math.pi, "wash", 96,
            uvf=lambda i: [(0.5, (SEAT_R1 + 0.45 - BASE_R1) / 0.85), (0.5, (SEAT_R1 + 0.45 - BASE_R1) / 0.85), (0.5, 0.0), (0.5, 0.0)], lit=True)
    for q in m.quads[first:]:
        q["kind"], q["ao"] = GLOW, False



def planter_mesh():
    m = TCC.Mesh()
    planter(m)
    bench(m)
    for q in m.quads:
        q["P"] = q["P"] + np.array([CX, 0.0, CZ])
    return m


# ------------------------------------------------------------------ writing

def _uvs(atlas, q):
    mat = q["mat"]
    if mat in NOISE and q["uvf"] is None:
        P_ = q["P"]
        sw = min(np.linalg.norm(P_[1] - P_[0]) * 2.0, 0.9)
        sh = min(np.linalg.norm(P_[3] - P_[0]) * 2.0, 0.9)
        TCC._seed[0] = (TCC._seed[0] * 1103515245 + 12345) & 0x7FFFFFFF
        s = TCC._seed[0]
        ou = (s % 997) / 997 * (1 - sw)
        ov = ((s // 997) % 991) / 991 * (1 - sh)
        f = [(ou, ov), (ou + sw, ov), (ou + sw, ov + sh), (ou, ov + sh)]
    else:
        f = q["uvf"] or [(0.25, 0.25), (0.75, 0.25), (0.75, 0.75), (0.25, 0.75)]
    return [atlas.uv(mat, u, v) for u, v in f]


def write_mesh(m, atlas, path):
    """The console's mesh format ("JTCM" 1, client/ConsoleMesh): quads by kind, no live faces, no step edges."""
    path.parent.mkdir(parents=True, exist_ok=True)
    TCC._seed[0] = 0
    buf = bytearray(struct.pack(">iii", TCC.MAGIC, 1, len(m.quads)))
    for q in m.quads:
        P_ = q["P"]
        uv = _uvs(atlas, q)
        nb = [int(round(max(-1, min(1, v)) * 127)) for v in q["n"]]
        sh = q.get("shade", [1.0] * 4)
        buf += struct.pack(">b", q["kind"])
        for k in (0, 3, 2, 1):
            buf += struct.pack(">fffbbbffB", P_[k][0], P_[k][1], P_[k][2], nb[0], nb[1], nb[2], uv[k][0], uv[k][1],
                               int(round(255 * max(0.0, min(1.0, sh[k])))))
    buf += struct.pack(">i", 0)
    buf += struct.pack(">i", 0)
    for mat in ("cap", "amber", "base"):
        u0, v0 = atlas.uv(mat, 0.1, 0.1)
        u1, v1 = atlas.uv(mat, 0.9, 0.9)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


def write_data():
    j = TCC._json
    j(ASSETS / "blockstates" / f"{ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "blockstates" / f"{PART_ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "models/block" / f"{ID}.json", {"textures": {"particle": "jugcraft:block/carbon_composite_panel"}, "elements": []})
    j(ASSETS / "models/item" / f"{ID}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}})
    j(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    j(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", TCC._loot(ID))
    j(DATA / "jugcraft/loot_table/blocks" / f"{PART_ID}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{PART_ID}"})
    j(DATA / "jugcraft/recipe" / f"{ID}.json", TCC._recipe(["GAG", "LOL", "CPC"], {
        "G": "minecraft:glowstone_dust", "A": "minecraft:amethyst_shard", "L": "minecraft:dark_oak_log", "O": "minecraft:oak_sapling",
        "C": "jugcraft:reinforced_concrete", "P": "minecraft:dark_oak_planks"}, ID))
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{PART_ID}"] = NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Crystal Tree needs a clear round space 9 blocks across, and 4 high in the middle"
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in (ID, PART_ID):
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")


def write_all():
    atlas = Atlas(tiles())
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    m = planter_mesh()
    TCC.bake(m, floor_y=0.0, res=1 / 16, reach=0.6, rays=16)
    write_mesh(m, atlas, MESH)
    TCC._json(ATLAS_MAP, {"atlas": list(ATLAS), "tiles": atlas.tiles_map()})
    write_data()
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return m


if __name__ == "__main__":
    m = write_all()
    kinds = [q["kind"] for q in m.quads]
    print(len(m.quads), "planter quads", {k: kinds.count(k) for k in sorted(set(kinds))})
