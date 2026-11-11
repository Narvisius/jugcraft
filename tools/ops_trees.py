"""The flight-ops trees: the Ember Oak and the Drift Isle, which take the place of the Tri-Pillar hall's two clusters of
flight-ops desks (Justin, 5 November 2026: "i wanna put other exotic trees like the other we made"; from the options
sheet ops_trees_options he chose option A for one floor and option D for the other; claude/jugcraft-tripillar-ops-trees.md).
The Ember Oak stands on the west floor, the Drift Isle on the east.

A  Ember Oak   charred black bark split by cracks that glow like embers, a wide flat crown of ember-red leaves (some of
               them glowing hot), sparks drifting up out of it; in a stepped bed of hexagonal basalt columns, the outer
               ring at seat height, coals glowing in the black gravel.
D  Drift Isle  an old twisted tree with violet foliage on a mossy rock island floating over a dark slate basin; roots
               hang under it with glowing tips, a soft blue lift beam rises from two emitter rings in the basin's floor,
               small stones circle it; a timber seat runs round the basin.

Both are our own designs (this is the approved mockup sheet's generator, ported). This file bakes everything the game
draws: VARIANTS of each tree (the renderer picks one by the placed tree's seed and turns it by a random angle, so no two
trees in a world need look alike), their texture atlas and its map, and what the renderer moves:

* tower_mesh/ops_ember_bed.bin.gz: the Ember Oak's bed (ConsoleMesh's format), the basalt columns and the gravel, the
  same under every variant and never turned (tower/OpsTreeBlock collides with each column, from
  data/jugcraft/ops_trees/ember_oak_bed.json); tower_mesh/ops_ember_oak_<k>.bin.gz: the tree, its roots and the rocks
  and coals in the gravel: bark, rocks and leaves shaded by the world (SOLID), the coals and the hot leaves full bright
  (LIT), the glowing cracks over the bark (GLOW, pulsed by the renderer: light breathing up the trunk and out the limbs);
* tower_mesh/ops_drift_isle_base.bin.gz: the basin, the emitter rings (LIT, pulsed), the lift beam (GLOW, waves of light
  going up it) and the seat; tower_mesh/ops_drift_isle_<k>.bin.gz: the island with its tree and roots (the renderer
  bobs it and turns it slowly); tower_mesh/ops_drift_stone.bin.gz: one small stone (the renderer has five circle the
  island, each tumbling);
* tower_mesh/ops_trees.json: the atlas map (tiles), and per variant the billboards: the Ember Oak's soft ember glows in
  its crown and its sparks (each rising from the crown to the ceiling and fading), the Drift Isle's root tips (pulsing)
  and its stones' orbits.

Frame of every mesh: the master cell's corner at the origin, the planter's middle over (0.5, 0.5), the floor at y 0 (the
island's too: the renderer turns it about x, z = 0.5).

One item places each tree whole (tower/OpsTreeBlock): the master is the middle cell on the floor, its parts the cells of
the bed (or basin and seat) round it and (the Ember Oak) the trunk's over it or (the Drift Isle) the island's.
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image

import ops_trees_tex as T
import tower_control_console as TCC

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
TEXTURE = ASSETS / "textures/entity/ops_trees.png"
ATLAS_MAP = ASSETS / "tower_mesh/ops_trees.json"
MESH_DIR = ASSETS / "tower_mesh"

EMBER, DRIFT = "ember_oak", "drift_isle"
NAMES = {EMBER: "Ember Oak", DRIFT: "Drift Isle"}
# the approved mockup's seed first (5 Nov 2026), then two more chosen from a sheet of candidates
SEEDS = {EMBER: (5, 37, 73), DRIFT: (12, 44, 83)}
VARIANTS = 3

SOLID, LIT, GLASS, GLOW = 0, 1, 2, 3
Y = np.array([0.0, 1.0, 0.0])
CX = CZ = 0.5
ATLAS = (1024, 1024)
CEIL = 7.7                       # the hall's ceiling is 8 over the floor; nothing reaches past this

# the beds (tower/OpsTreeBlock uses the same numbers for what collides)
SEAT_R = 4.12                    # the Drift Isle's seat's front edge
EMBER_R = 4.3                    # the Ember Oak's columns reach no further out
EMBER_GRAVEL, EMBER_GRAVEL_R, EMBER_RING, EMBER_SEAT = 0.5, 3.0, (2.9, 3.55), 0.46
EMBER_COLUMN_R = 0.62 / math.sqrt(3) * 0.985    # a column's corners from its middle (pitch 0.62, a hair's gap)
BED_JSON = DATA / "jugcraft/ops_trees/ember_oak_bed.json"
DRIFT_BASIN, DRIFT_FLOOR, DRIFT_WALL = (2.9, 3.2), 0.2, 0.6
DRIFT_ISLE_Y = 2.85              # the island's top (its middle; the renderer bobs it a little)


def unit(v):
    v = np.asarray(v, float)
    n = np.linalg.norm(v)
    return v / n if n > 1e-12 else v


def rot(v, axis, ang):
    axis = unit(axis)
    return v * math.cos(ang) + np.cross(axis, v) * math.sin(ang) + axis * (axis @ v) * (1 - math.cos(ang))


# ------------------------------------------------------------------ textures

def _img(a):
    return Image.fromarray(np.clip(np.asarray(a, np.float32) * 255 + 0.5, 0, 255).astype(np.uint8), "RGBA")


def _solid(rgb, size=8):
    return Image.new("RGBA", (size, size), tuple(rgb) + (255,))


def _halo(rgb, peak, power, w=32):
    return _img(T.halo_tex(rgb, peak, power, w))


def _beam():
    """The lift beam: soft blue, fading out towards its top (v 0 is the top)."""
    a = np.zeros((64, 8, 4), np.float32)
    a[..., :3] = np.array([110, 200, 255], np.float32) / 255
    a[..., 3] = (np.linspace(0.0, 1.0, 64) ** 1.4 * 0.32)[:, None]
    return _img(a)


def _moss_top():
    """The island's moss, one sheet over the whole island (no repeat to see)."""
    rnd = np.random.default_rng(25)
    h = w = 256
    n = T.fbm(rnd, h, w, 6, base=3)
    d1, _ = T.voronoi(rnd, h, w, 360)
    tuft = np.clip(1 - d1 / 5, 0, 1)
    rgb = np.stack([40 + 50 * n + 30 * tuft, 70 + 70 * n + 40 * tuft, 48 + 40 * n], -1)
    return _img(T.tex_from(rgb))


FLAME_FRAMES = 4                 # the Ember Oak's flames: moments side by side in their tile
LEAF_TEX = 128                   # the Ember Oak's leaf sprays and each moment of their flames, texels across
SOIL_MOUND = 0.17                # the Ember Oak's soil: how high its mound rises at the trunk's foot (tower/OpsTreeBlock.MOUND)
DRIFT_LEAVES = [([90, 30, 120], [150, 60, 190], [200, 110, 230]), ([110, 28, 100], [180, 60, 160], [230, 120, 210]),
                ([70, 26, 110], [120, 50, 180], [170, 100, 230])]
SHRUB_LEAVES = [([34, 70, 36], [62, 120, 56], [104, 164, 78]), ([30, 64, 40], [54, 110, 62], [96, 156, 90]),
                ([40, 76, 32], [72, 128, 50], [120, 172, 70])]


def tiles():
    bark, crack = T.charred_bark()
    t = {
        # the Ember Oak's
        "ember_bark": _img(bark), "ember_crack": _img(crack),
        "ember_plate": _img(T.ember_plate()), "ember_edge": _img(T.ember_edge()), "ember_core": _img(T.ember_core()),
        "basalt_side": _img(T.basalt_side()), "basalt_top": _img(T.basalt_top()),
        "burnt_loam": _img(T.burnt_loam()), "ember_coal": _img(T.ember_coal()), "ember_speck": _img(T.ember_speck()),
        "ember_halo": _halo((255, 120, 40), 0.30, 2.2), "spark": _halo((255, 200, 120), 0.95, 1.4, 16),
        # the Drift Isle's
        "slate": _img(T.slate()), "dark_wood": _img(T.dark_wood()), "emitter": _solid((90, 190, 255)), "beam": _beam(),
        "isle_rock": _img(T.rock_tex(47, warm=True)), "isle_under": _img(T.rock_tex(48)), "moss": _moss_top(),
        "bonsai_bark": _img(T.bonsai_bark()), "tip": _halo((110, 210, 255), 0.9, 1.5, 16),
    }
    # r184: the Ember Oak's leaves, molten-veined, some standing up with small flames licking off their tips (Justin,
    # 8 November 2026: "between a solid leaf and flames", look A)
    for i in range(3):
        t[f"ember_spray{i}"] = _img(T.ember_spray(50 + i, w=LEAF_TEX))
    for i in range(2):          # (the same, in shade: for the sprays low in a pad, as their baked shade was before)
        dark = T.ember_spray(50 + i, w=LEAF_TEX)
        dark[..., :3] *= 0.66
        t[f"ember_spray_dark{i}"] = _img(dark)
    for i in range(2):
        body, flames = T.ember_flaming(60 + i, w=LEAF_TEX, frames=FLAME_FRAMES)
        t[f"ember_stand{i}"], t[f"ember_flame{i}"] = _img(body), _img(flames)
    # r181: the Drift Isle's foliage tidied: single leaves laid over dense cores (Justin, 8 November 2026: "It just looks
    # like leaves messily put together and shoved through each other")
    for i, c in enumerate(DRIFT_LEAVES):
        t[f"drift_leafs{i}"] = _img(T.single_leaf(c, 80 + i))
    t["drift_foliage"] = _img(T.foliage_tile(DRIFT_LEAVES[0], 84))
    for i, c in enumerate(SHRUB_LEAVES):
        t[f"shrub_leaf{i}"] = _img(T.single_leaf(c, 90 + i, w=40, h=40, round_tip=True))
    t["shrub_foliage"] = _img(T.foliage_tile(SHRUB_LEAVES[0], 94, n=220, leaf=(9, 5), round_tip=True))
    return t


# tiles whose texture repeats (a quad's uv may run past 1: the writer cuts it at the repeats)
TILED = {"ember_bark", "ember_crack", "ember_core", "basalt_side", "burnt_loam", "slate", "dark_wood", "isle_rock", "isle_under", "bonsai_bark"}


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
        return {n: [round(c, 7) for c in (*self.uv(n, 0, 0), *self.uv(n, 1, 1))] for n in sorted(self.box)}


# ------------------------------------------------------------------ the mesh

class Mesh:
    """Quads as dicts: kind, P (corners tl tr br bl seen from the front), mat (a tile), uv (per corner, in the tile's
    repeats), cn (per-corner normals, or None for the face's), bright (a factor per corner on the baked shade), ao."""

    def __init__(self):
        self.quads = []

    def quad(self, P, mat, uv=None, kind=SOLID, bright=None, cn=None, ao=True):
        P = np.asarray(P, float)
        n = np.zeros(3)
        for i in range(4):
            a, b = P[i], P[(i + 1) % 4]
            n += np.array([(a[1] - b[1]) * (a[2] + b[2]), (a[2] - b[2]) * (a[0] + b[0]), (a[0] - b[0]) * (a[1] + b[1])])
        n = -n
        ln = np.linalg.norm(n)
        if ln < 1e-12:
            return
        uv = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float) if uv is None else np.asarray(uv, float)
        self.quads.append(dict(kind=kind, P=P, mat=mat, uv=uv, n=n / ln, cn=None if cn is None else np.asarray(cn, float),
                               bright=None if bright is None else np.asarray(bright, float), ao=ao and kind == SOLID))

    def oquad(self, P, want, mat, uv=None, kind=SOLID, bright=None, cn=None, ao=True):
        P = np.asarray(P, float)
        n = np.cross(P[3] - P[0], P[1] - P[0])
        if np.linalg.norm(n) < 1e-12:
            n = np.cross(P[2] - P[0], P[1] - P[0])
        if np.linalg.norm(n) < 1e-12:
            # (its first two corners one point, as a rock's top: its facing from all four, the way quad() works it out;
            # r185, before which every rock's top cap faced in and the chunk showed a ring where a stone should be)
            n = np.zeros(3)
            for i in range(4):
                a, b = P[i], P[(i + 1) % 4]
                n -= np.array([(a[1] - b[1]) * (a[2] + b[2]), (a[2] - b[2]) * (a[0] + b[0]), (a[0] - b[0]) * (a[1] + b[1])])
        if n @ np.asarray(want, float) < 0:
            o = [1, 0, 3, 2]
            P = P[o]
            uv = None if uv is None else np.asarray(uv, float)[o]
            cn = None if cn is None else np.asarray(cn, float)[o]
            bright = None if bright is None else np.asarray(bright, float)[o]
        self.quad(P, mat, uv, kind, bright, cn, ao)

    # -------------------------------------------------------------- primitives (as the mockup's)
    def tube(self, pts, rad, mat, sides=8, cap=True, vlen=None, kind=SOLID, bright=None):
        """A tube along pts (radii per point), its frame carried along without twisting; the texture wraps once round
        (u) and repeats every vlen blocks along (v); normals smooth round it."""
        pts = [np.asarray(p, float) for p in pts]
        n = len(pts)
        if n < 2:
            return
        tang = [unit(pts[min(n - 1, i + 1)] - pts[max(0, i - 1)]) for i in range(n)]
        up = Y if abs(tang[0] @ Y) < 0.9 else np.array([1.0, 0, 0])
        nrm = unit(np.cross(tang[0], up))
        frames, dist = [], [0.0]
        for i in range(n):
            if i:
                dist.append(dist[-1] + np.linalg.norm(pts[i] - pts[i - 1]))
                v = np.cross(tang[i - 1], tang[i])
                if np.linalg.norm(v) > 1e-9:
                    ang = math.asin(min(1.0, np.linalg.norm(v)))
                    if tang[i - 1] @ tang[i] < 0:
                        ang = math.pi - ang
                    nrm = rot(nrm, v, ang)
            frames.append((nrm, np.cross(tang[i], nrm)))
        vlen = vlen or max(0.5, 2 * math.pi * max(rad))
        dirs = [[nn * math.cos(2 * math.pi * k / sides) + bn * math.sin(2 * math.pi * k / sides) for k in range(sides + 1)]
                for nn, bn in frames]
        rings = [[pts[i] + dirs[i][k] * rad[i] for k in range(sides + 1)] for i in range(n)]
        for i in range(n - 1):
            v0, v1 = dist[i] / vlen, dist[i + 1] / vlen
            for k in range(sides):
                u0, u1 = k / sides, (k + 1) / sides
                P = [rings[i + 1][k], rings[i + 1][k + 1], rings[i][k + 1], rings[i][k]]
                cn = [dirs[i + 1][k], dirs[i + 1][k + 1], dirs[i][k + 1], dirs[i][k]]
                b = None if bright is None else [bright[i + 1], bright[i + 1], bright[i], bright[i]]
                self.oquad(P, (rings[i][k] + rings[i + 1][k + 1]) / 2 - (pts[i] + pts[i + 1]) / 2, mat,
                           [(u0, v1), (u1, v1), (u1, v0), (u0, v0)], kind, b, cn)
        if cap:
            c = pts[-1] + tang[-1] * rad[-1] * 0.4
            for k in range(sides):
                self.oquad([rings[-1][k], rings[-1][k + 1], c, c], tang[-1], mat,
                           [(0.1, 0.1), (0.3, 0.1), (0.2, 0.3), (0.2, 0.3)], kind)

    def fan_disc(self, c, r, y, mat, seg=32, up=True, kind=SOLID):
        """A disc of one untiled texture (its whole tile across the disc)."""
        c = np.asarray(c, float)
        for k in range(seg):
            a0, a1 = 2 * math.pi * k / seg, 2 * math.pi * (k + 1) / seg
            p0 = c + np.array([r * math.cos(a0), y, r * math.sin(a0)])
            p1 = c + np.array([r * math.cos(a1), y, r * math.sin(a1)])
            m = c + np.array([0, y, 0])
            uv = [((p[0] - c[0]) / (2 * r) + 0.5, (p[2] - c[2]) / (2 * r) + 0.5) for p in (p0, p1, m, m)]
            self.oquad([p0, p1, m, m], Y if up else -Y, mat, uv, kind)

    def annulus(self, c, r0, r1, y, mat, seg=48, up=True, kind=SOLID, ulen=None):
        """A flat ring; with ulen its texture runs round it (u along the arc, a repeat every ulen blocks; v across)."""
        c = np.asarray(c, float)
        for k in range(seg):
            a0, a1 = 2 * math.pi * k / seg, 2 * math.pi * (k + 1) / seg
            pts = [c + np.array([r * math.cos(a), y, r * math.sin(a)]) for r, a in ((r1, a0), (r1, a1), (r0, a1), (r0, a0))]
            if ulen:
                rm = (r0 + r1) / 2
                uv = [(rm * a0 / ulen, 0), (rm * a1 / ulen, 0), (rm * a1 / ulen, (r1 - r0) / ulen), (rm * a0 / ulen, (r1 - r0) / ulen)]
            else:
                uv = [(0.5 + 0.5 * math.cos(a) * r / r1, 0.5 + 0.5 * math.sin(a) * r / r1) for r, a in ((r1, a0), (r1, a1), (r0, a1), (r0, a0))]
            self.oquad(pts, Y if up else -Y, mat, uv, kind)

    def planar(self, c, r0, r1, y, mat, tile, up=True, kind=SOLID):
        """A flat disc or ring of a repeating texture laid straight on (a repeat every `tile` blocks): squares of a grid,
        each one repeat, cut to the ring (so the texture runs on from square to square without a seam)."""
        c = np.asarray(c, float)
        outer = [(r1 * math.cos(2 * math.pi * i / 96), r1 * math.sin(2 * math.pi * i / 96)) for i in range(96)]
        n = int(math.ceil(r1 / tile))
        for i in range(-n, n):
            for j in range(-n, n):
                sq = [(i * tile, j * tile), ((i + 1) * tile, j * tile), ((i + 1) * tile, (j + 1) * tile), (i * tile, (j + 1) * tile)]
                poly = clip_convex(sq, outer)
                if r0 > 0 and len(poly) >= 3:
                    poly = TCC.clip_circle(poly, r0)
                if len(poly) < 3:
                    continue
                uv = [((x - i * tile) / tile, (z - j * tile) / tile) for x, z in poly]
                for k in range(1, len(poly) - 1, 2):
                    idx = [0, k, k + 1, min(k + 2, len(poly) - 1)]
                    pts = [c + np.array([poly[q][0], y, poly[q][1]]) for q in idx]
                    self.oquad(pts, Y if up else -Y, mat, [uv[q] for q in idx], kind)

    def wall(self, c, r, y0, y1, mat, seg=48, out=True, ulen=1.0, kind=SOLID):
        """A vertical round wall (the outside when out), its texture repeating every ulen blocks."""
        c = np.asarray(c, float)
        for k in range(seg):
            b0, b1 = 2 * math.pi * k / seg, 2 * math.pi * (k + 1) / seg
            p = [c + np.array([r * math.cos(b), yy, r * math.sin(b)]) for b, yy in ((b0, y1), (b1, y1), (b1, y0), (b0, y0))]
            s = 1 if out else -1
            cn = [np.array([math.cos(b) * s, 0, math.sin(b) * s]) for b in (b0, b1, b1, b0)]
            h = (y1 - y0) / ulen
            self.oquad(p, cn[0] + cn[1], mat, [(r * b0 / ulen, 0), (r * b1 / ulen, 0), (r * b1 / ulen, h), (r * b0 / ulen, h)], kind, cn=cn)

    def prism(self, base, rad, length, sides, mat, top_mat, rot0=0.0):
        """An upright prism (a basalt column): its sides and its top."""
        base = np.asarray(base, float)
        ring = [np.array([math.cos(rot0 + 2 * math.pi * k / sides), 0, math.sin(rot0 + 2 * math.pi * k / sides)]) * rad
                for k in range(sides)]
        arc = 2 * math.pi * rad / sides
        top = base + Y * length
        for k in range(sides):
            j = (k + 1) % sides
            self.oquad([top + ring[k], top + ring[j], base + ring[j], base + ring[k]], (ring[k] + ring[j]) / 2, mat,
                       [(0, 0), (arc, 0), (arc, length), (0, length)])
        for k in range(sides):
            j = (k + 1) % sides
            self.oquad([top + ring[k], top + ring[j], top, top], Y, top_mat,
                       [(0.5 + 0.5 * ring[k][0] / rad, 0.5 + 0.5 * ring[k][2] / rad), (0.5 + 0.5 * ring[j][0] / rad, 0.5 + 0.5 * ring[j][2] / rad),
                        (0.5, 0.5), (0.5, 0.5)])

    def cap(self, c, rad, sides, mat, rot0=0.0):
        """A prism's top alone (a flat polygon facing up) round c, its texture laid as a prism's top's."""
        c = np.asarray(c, float)
        ring = [np.array([math.cos(rot0 + 2 * math.pi * k / sides), 0, math.sin(rot0 + 2 * math.pi * k / sides)]) * rad
                for k in range(sides)]
        for k in range(sides):
            j = (k + 1) % sides
            self.oquad([c + ring[k], c + ring[j], c, c], Y, mat,
                       [(0.5 + 0.5 * ring[k][0] / rad, 0.5 + 0.5 * ring[k][2] / rad), (0.5 + 0.5 * ring[j][0] / rad, 0.5 + 0.5 * ring[j][2] / rad),
                        (0.5, 0.5), (0.5, 0.5)])

    def rock(self, c, size, rnd, mat, squash=0.6, n_lat=6, n_lon=9, rough=0.25, kind=SOLID):
        """A lumpy rock (a noisy, squashed sphere), smooth shaded."""
        c = np.asarray(c, float)
        bumps = [(unit(rnd.normal(0, 1, 3)), rnd.uniform(-rough, rough)) for _ in range(7)]

        def pt(lat, lon):
            d = np.array([math.cos(lat) * math.cos(lon), math.sin(lat), math.cos(lat) * math.sin(lon)])
            r = 1 + sum(a * max(0, d @ b) ** 2 for b, a in bumps)
            return c + d * size * r * np.array([1, squash, 1]), unit(d * np.array([1, 1 / squash, 1]))
        for i in range(n_lat):
            la0 = -math.pi / 2 + math.pi * i / n_lat
            la1 = -math.pi / 2 + math.pi * (i + 1) / n_lat
            for j in range(n_lon):
                lo0, lo1 = 2 * math.pi * j / n_lon, 2 * math.pi * (j + 1) / n_lon
                pn = [pt(la1, lo0), pt(la1, lo1), pt(la0, lo1), pt(la0, lo0)]
                P = [p for p, _ in pn]
                self.oquad(P, np.mean(P, 0) - c, mat,
                           [(j / n_lon, 1 - (i + 1) / n_lat), ((j + 1) / n_lon, 1 - (i + 1) / n_lat), ((j + 1) / n_lon, 1 - i / n_lat),
                            (j / n_lon, 1 - i / n_lat)], kind, cn=[q for _, q in pn])

    def clump(self, c, rx, ry, rz, rnd, core, leaves, leaf, flat=0.45, density=1.0, under=0.3, tilt=(0.25, 0.65), shade=(0.5, 1.05),
              n=2, tile=0.9, sink=None, reach=None, ceil=None):
        """A clump of foliage (r181): a lumpy core in a dense foliage texture that nothing shows through, with single
        leaves laid over it like shingles, each from a point on the core and lying out along it from the crown and down
        its sides, tipped a little off it; fewer underneath. Lighter on top, darker below. The core is a rounded box (a
        cube's faces pushed out round, n by n quads each, flatter underneath), each quad showing its own patch of the
        foliage texture at its size: no pole where the texture would bunch up and stretch. Leaves are laid on the quads
        as drawn, so none hovers. sink: a height under which no leaves are laid (a shrub's foot in the ground); reach and
        ceil: how far out and how high leaves may go."""
        c = np.asarray(c, float)
        bumps = [(unit(rnd.normal(0, 1, 3)), rnd.uniform(-0.14, 0.14)) for _ in range(6)]

        def surf(d):
            k = 1 + sum(a * max(0.0, float(d @ b)) ** 2 for b, a in bumps)
            return c + d * np.array([rx, ry if d[1] >= 0 else ry * flat, rz]) * k

        def normal(d):
            return unit(d / np.array([rx, ry if d[1] >= 0 else ry * flat, rz]))

        def shade_at(d):
            return shade[0] + (shade[1] - shade[0]) * (0.5 + 0.5 * d[1])
        faces = []
        for axis in range(3):
            for sign in (1, -1):
                o = (axis + 1) % 3, (axis + 2) % 3
                grid = []
                for i in range(n + 1):
                    row = []
                    for j in range(n + 1):
                        v = np.zeros(3)
                        v[axis] = sign
                        v[o[0]] = -1 + 2 * i / n
                        v[o[1]] = -1 + 2 * j / n
                        row.append(unit(v))
                    grid.append(row)
                for i in range(n):
                    for j in range(n):
                        faces.append([grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1]])
        golden = []
        for ds in faces:
            P = [surf(d) for d in ds]
            size = (np.linalg.norm(P[1] - P[0]) + np.linalg.norm(P[2] - P[3]) + np.linalg.norm(P[3] - P[0]) + np.linalg.norm(P[2] - P[1])) / 4
            w = min(1.0, max(0.2, size / tile))
            u0, v0 = rnd.uniform(0, 1 - w), rnd.uniform(0, 1 - w)
            uv = [(u0, v0), (u0 + w, v0), (u0 + w, v0 + w), (u0, v0 + w)]
            mid = np.mean(P, 0)
            self.oquad(P, mid - c, core, uv, bright=[shade_at(d) for d in ds], cn=[normal(d) for d in ds])
            golden.append((P, ds))
        # leaves on the quads as drawn
        per = density / (0.42 * leaf * leaf * 0.75)
        for P, ds in golden:
            P = [np.asarray(x) for x in P]
            fn = np.cross(P[2] - P[0], P[3] - P[1])
            area = np.linalg.norm(fn) / 2
            dm = unit(np.mean(ds, 0))
            fn = unit(fn if fn @ (np.mean(P, 0) - c) > 0 else -fn)
            k = area * per
            count = int(k) + (1 if rnd.random() < k - int(k) else 0)
            for _ in range(count):
                su, sv = rnd.uniform(0.05, 0.95), rnd.uniform(0.05, 0.95)
                p = (P[0] * (1 - su) + P[1] * su) * (1 - sv) + (P[3] * (1 - su) + P[2] * su) * sv
                d = unit(p - c)
                if d[1] < -under and rnd.random() < 0.7:
                    continue
                if sink is not None and p[1] < sink or ceil is not None and p[1] > ceil or reach is not None and math.hypot(p[0], p[2]) > reach:
                    continue
                out = np.array([d[0], -0.3, d[2]])
                t = out - fn * (out @ fn)
                if np.linalg.norm(t) < 1e-3:
                    a = rnd.uniform(0, 2 * math.pi)
                    t = np.array([math.cos(a), 0, math.sin(a)])
                    t = t - fn * (t @ fn)
                t = unit(t)
                # flatter on the crown (no tips sticking up out of it), standing off more down the sides (a leafy edge)
                a = rnd.uniform(*tilt) * (1 - 0.85 * max(0.0, float(dm[1])))
                sz = leaf * rnd.uniform(0.85, 1.2)
                base = p + fn * rnd.uniform(-0.01, 0.025)
                # aimed so its tip comes down onto the clump's curve (lifted by its tilt), not out past a flat facet
                td = unit(p + t * sz - c)
                tip = surf(td) + normal(td) * sz * math.sin(a) * 0.6
                up = unit(tip - base)
                face = np.cross(np.cross(up, fn), up)
                face = unit(face if face @ fn > 0 else -face)
                b = min(1.15, shade_at(d) * rnd.uniform(0.92, 1.12) + 0.08)
                self.card(base + up * sz * 0.5, face, up, sz * 0.75, sz, leaves[int(rnd.integers(0, len(leaves)))], [b] * 4)

    def card(self, c, normal, up, w, h, mat, bright=None, kind=SOLID):
        """A flat card (a leaf spray) centred on c: one quad (the renderer's cut-out type draws both its sides)."""
        c = np.asarray(c, float)
        n = unit(normal)
        up = unit(up - n * (up @ n))
        r = np.cross(up, n)
        P = [c - r * w / 2 + up * h / 2, c + r * w / 2 + up * h / 2, c + r * w / 2 - up * h / 2, c - r * w / 2 - up * h / 2]
        self.oquad(P, n, mat, None, kind, bright, ao=False)


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


def limb(p, d, length, r0, r1, rnd, step=0.2, droop=0.0, wander=0.2, crown_r=3.9, ceil=CEIL, rise=0.0):
    """A limb grown step by step: wandering (now and then a kink), bending, kept over the bed and under the ceiling."""
    pts, rad = [np.asarray(p, float).copy()], [r0]
    d = unit(d)
    n = max(2, int(length / step))
    for i in range(n):
        side = unit(np.cross(d, Y)) if np.linalg.norm(np.cross(d, Y)) > 1e-3 else np.array([1.0, 0, 0])
        kink = 2.4 if rnd.random() < 0.1 else 1.0
        d = rot(d, Y, rnd.normal(0, wander) * kink)
        d = rot(d, side, rnd.normal(0, wander * 0.6) * kink - droop * step + rise * step)
        q = pts[-1] + d * step
        r = math.hypot(q[0], q[2])
        if r > crown_r:
            d = unit(d - np.array([q[0], 0, q[2]]) / r * 0.9)
            q = pts[-1] + d * step
        if q[1] > ceil:
            d = unit(d - Y * 1.0)
            q = pts[-1] + d * step
        pts.append(q)
        rad.append(r0 + (r1 - r0) * (i + 1) / n)
    return pts, rad


def chaikin(pts, k=2):
    pts = [np.asarray(p, float) for p in pts]
    for _ in range(k):
        out = [pts[0]]
        for a, b in zip(pts[:-1], pts[1:]):
            out += [a * 0.75 + b * 0.25, a * 0.25 + b * 0.75]
        out.append(pts[-1])
        pts = out
    return pts


def hexgrid(r_max, pitch):
    out = []
    n = int(r_max / pitch) + 2
    for i in range(-n, n + 1):
        for j in range(-n, n + 1):
            x = pitch * (i + 0.5 * j)
            z = pitch * j * math.sqrt(3) / 2
            if math.hypot(x, z) <= r_max:
                out.append((x, z))
    return out


def overlay(rad):
    """The glowing cracks' tube over the bark: never closer than 0.11 px to it (no z-fighting)."""
    return [r + max(0.007, 0.01 * r) for r in rad]


def molded_trunk(m, edges, prng, strands, branches, voxel=0.03, ks=((0, 0.07), (1, 0.14), (2, 0.06)), r_min=0.075, floor_y=EMBER_GRAVEL - 0.04,
                 collar=0.32, stats=None):
    """The whole tree cast in one piece (tools/ops_trees_molded.py): the trunk's strands and every branch, as far out
    as each is at least r_min thick (and every branch for at least its first `collar`), its plates (tops and glow into
    m, sides into edges). branches: (pts, rad, sides, vlen, level), level 1 growing from the trunk, 2 from a level 1
    branch (ks: how broad the fillets are where each level meets what it grows from). Returns the thin ends left to
    draw as plain bark: (pts, rad, sides, vlen), each starting a little inside the last plates."""
    import ops_trees_molded as MO
    cast_tubes, planes, twigs = [], [], []
    for pts, rad in strands:
        cast_tubes.append(([np.asarray(p, float) for p in pts], list(rad), 0))
    for pts, rad, sides, vl, lv in branches:
        P = [np.asarray(p, float) for p in pts]
        # every branch cast for at least its first `collar` (thickened to r_min there if it is thinner), so even a
        # thin one grows out of what it springs from in one piece and only goes on as a plain tube clear of the join
        s_ = np.concatenate([[0.0], np.cumsum([np.linalg.norm(P[i + 1] - P[i]) for i in range(len(P) - 1)])])
        R = [max(r, r_min + 0.005) if s_[i] < collar else r for i, r in enumerate(rad)]
        j = next((i for i in range(1, len(P)) if R[i] < r_min), None)
        if j is None:
            cast_tubes.append((P, R, lv))
            continue
        f = (R[j - 1] - r_min) / max(R[j - 1] - R[j], 1e-9)
        c = P[j - 1] + (P[j] - P[j - 1]) * f
        n = unit(P[j] - P[j - 1])
        planes.append((c, n, len(cast_tubes)))
        cast_tubes.append((P[:j] + [c + n * 0.1], R[:j] + [r_min], lv))
        twigs.append(([c - n * 0.08, c] + P[j:], [r_min, r_min] + R[j:], sides, vl))
    field = MO.Field(cast_tubes, ks)
    allp = np.array([p for P, R, _ in cast_tubes for p in P])
    allr = max(r for P, R, _ in cast_tubes for r in R)
    lo = np.maximum(allp.min(0) - allr - 0.12, [-9, floor_y - 0.1, -9])
    hi = allp.max(0) + allr + 0.12
    V, T = MO.cast(field, lo, hi, voxel)
    V, T = MO.clip(V, T, planes, field, floor_y)
    # three plates round each end where the plates give out (so none wraps right round it)
    ring = []
    for c, n, _ in planes:
        u = unit(np.cross(n, Y if abs(n @ Y) < 0.9 else np.array([1.0, 0, 0])))
        w = np.cross(n, u)
        a0 = prng.uniform(0, 2 * math.pi)
        for i in range(3):
            a = a0 + 2 * math.pi * i / 3
            ring.append(c - n * 0.07 + (u * math.cos(a) + w * math.sin(a)) * r_min)
    ring = list(field.project(np.array(ring))) if ring else []
    MO.plates(m, edges, field, V, T, prng, lambda r: float(np.clip(min(0.6 * r + 0.1, 2 * math.pi * r / 4.6), 0.1, 0.3)), 0.018, 0.045, r_min,
              "ember_plate", "ember_edge", "ember_core", SOLID, LIT, GLOW, stats=stats, pre_seeds=ring)
    return twigs


def flare(rad, by=(1.45, 1.2, 1.06)):
    """A branch's radii swelling where it leaves what it grows from (a collar, not a pipe stuck in)."""
    return [r * (by[i] if i < len(by) else 1.0) for i, r in enumerate(rad)]


def soil_height(x, z, roots):
    """How far the Ember Oak's soil rises over the bed's gravel level at (x, z) (r185): a soft mound to the trunk's foot,
    low ridges along the roots (their paths, pts in the tree's frame), a little unevenness; level at the columns."""
    x, z = np.asarray(x, float), np.asarray(z, float)
    r, a = np.hypot(x, z), np.arctan2(z, x)
    h = SOIL_MOUND * np.exp(-(r / 1.25) ** 2)
    for pts in roots:
        P = np.array([(p[0], p[2]) for p in pts])
        d = np.full(r.shape, np.inf)
        for i in range(len(P) - 1):
            ab = P[i + 1] - P[i]
            t = np.clip(((x - P[i][0]) * ab[0] + (z - P[i][1]) * ab[1]) / max(float(ab @ ab), 1e-12), 0, 1)
            d = np.minimum(d, np.hypot(x - P[i][0] - t * ab[0], z - P[i][1] - t * ab[1]))
        h = h + 0.06 * np.exp(-(d / 0.22) ** 2) * np.clip(1 - r / 2.4, 0, 1)
    h = h + 0.02 * np.sin(3 * a + 2.1 * r) * np.sin(5 * a - 1.3 * r) * np.clip(1 - r / 2.9, 0, 1)
    return h * np.clip((2.95 - r) / 0.35, 0, 1)


def ember_soil(m, bright, roots, stones, rnd):
    """The Ember Oak's soil (r185; Justin, 8 November 2026: "I'd like the soil to look a little bit better", look C of
    the mockup): dark burnt earth with drifts of pale ash, a mound rising to the trunk's foot with low ridges along the
    roots (soil_height; the roots sink into it), out to the columns; in the tree's mesh, as it follows the roots. On it
    the stones where they were: the coals (and a few more of the stones) glowing in their cracks, a little bigger and
    rounder, full bright (into bright: baked, no longer flickering, which saves drawing them every frame); the rest dark
    basalt. Sparks of coal in the soil, more near the trunk, and fallen leaves (both full bright, into bright)."""
    def ground(x, z):
        return EMBER_GRAVEL + float(soil_height(np.array(x), np.array(z), roots))
    # the soil: rings out from inside the trunk's foot to under the columns (r212: past the gravel's edge, level, to
    # 3.3, where all of it the columns leave open is covered and the seat's lower columns don't yet start)
    rings = np.concatenate([np.arange(0.45, 2.98, 0.1), [2.98, 3.1, 3.2, 3.3]])
    seg = 72
    A = np.linspace(0, 2 * math.pi, seg + 1)
    RR, AA = np.meshgrid(rings, A, indexing="ij")
    X, Z = RR * np.cos(AA), RR * np.sin(AA)
    H = EMBER_GRAVEL + soil_height(X, Z, roots)
    for i in range(len(rings) - 1):
        for j in range(seg):
            idx = [(i + 1, j), (i + 1, j + 1), (i, j + 1), (i, j)]
            P = [np.array([X[a, b], H[a, b], Z[a, b]]) for a, b in idx]
            m.oquad(P, Y, "burnt_loam", [(p[0] / 1.2, p[2] / 1.2) for p in P])
    # the stones on it
    for (c, size, coal) in stones:
        p = np.array([c[0], ground(c[0], c[2]), c[2]])
        if coal or rnd.random() < 0.3:
            bright.rock(p, size * (1.25 if coal else 0.9), rnd, "ember_coal", squash=0.55, n_lat=5, n_lon=8, kind=LIT)
        else:
            m.rock(p, size, rnd, "basalt_top", squash=0.5, n_lat=4, n_lon=7)
    # sparks of coal in the soil
    n = 0
    while n < 170:
        a, r = rnd.uniform(0, 2 * math.pi), math.sqrt(rnd.uniform(0.62 ** 2, 2.9 ** 2))
        if rnd.random() > 0.35 + 0.65 * math.exp(-(r / 1.4) ** 2):
            continue
        x, z = r * math.cos(a), r * math.sin(a)
        s_ = rnd.uniform(0.022, 0.045) / 2
        y = ground(x, z) + 0.004
        bright.oquad([(x - s_, y, z - s_), (x + s_, y, z - s_), (x + s_, y, z + s_), (x - s_, y, z + s_)], Y, "ember_speck", None, LIT, ao=False)
        n += 1
    # fallen leaves
    for k in range(55):
        a, r = rnd.uniform(0, 2 * math.pi), math.sqrt(rnd.uniform(0.7 ** 2, 2.8 ** 2))
        x, z = r * math.cos(a), r * math.sin(a)
        s_ = rnd.uniform(0.2, 0.34)
        t = rnd.uniform(0, 2 * math.pi)
        u, v = np.array([math.cos(t), 0, math.sin(t)]) * s_ / 2, np.array([-math.sin(t), 0, math.cos(t)]) * s_ / 2
        P = [np.array([x, 0, z]) + d for d in (-u - v, u - v, u + v, -u + v)]
        for p in P:
            p[1] = ground(p[0], p[2]) + 0.006
        bright.oquad(P, Y, f"ember_spray{int(rnd.integers(0, 3))}", None, LIT, ao=False)


def _both_sides(mesh, P, n, mat, uv, kind):
    """A card as two quads back to back (the chunk's mesh shows a face from its front only)."""
    mesh.oquad(P, n, mat, uv, kind, ao=False)
    mesh.oquad(P, -np.asarray(n, float), mat, uv, kind, ao=False)


def ember_leaves(sprays, bright, flames, rnd, flaming=0.1):
    """The Ember Oak's leaf sprays (r184; Justin, 8 November 2026: "between a solid leaf and flames", look A of the
    mockup): each a card of molten-veined leaves, full bright, both sides (into bright). Most lie in the canopy as they
    always have; about `flaming` of them (more along each pad's top, fewer deep in it) stand up, their leaves pointing
    up and small flames licking off the tips: the flames a card of their own over the same place (into flames, glow),
    its uv on the first of the tile's moments (the game steps through them, and lights them with the wave that runs up
    the tree). sprays: (centre, normal, up, size, low in its pad) as the canopy laid them out (a low one in shade)."""
    from scipy.spatial import cKDTree
    P = np.array([s_[0] for s_ in sprays])
    xz = P[:, [0, 2]]
    kd = cKDTree(xz)
    UV = [(0, 0), (1, 0), (1, 1), (0, 1)]
    f1 = (LEAF_TEX - 1) / (LEAF_TEX * FLAME_FRAMES - 1)   # (the first moment's width in the tile's 0..1, texel centres)
    for i, (p, nrm, up, sz, low) in enumerate(sprays):
        near = kd.query_ball_point(xz[i], 0.9)
        top = p[1] > max(P[j][1] for j in near) - 0.25
        edge = len(near) < 160
        if rnd.random() < flaming * (2.6 if top else 1.4 if edge else 0.45):
            n = unit(rnd.normal(0, 1, 3) * np.array([1.0, 0, 1.0]) + Y * 0.25)
            u = unit(Y - n * n[1])
            r = np.cross(u, n)
            s_ = sz * 1.3
            Q = [p - r * s_ / 2 + u * s_ * 0.7, p + r * s_ / 2 + u * s_ * 0.7, p + r * s_ / 2 - u * s_ * 0.3, p - r * s_ / 2 - u * s_ * 0.3]
            v = int(rnd.integers(0, 2))
            _both_sides(bright, Q, n, f"ember_stand{v}", UV, LIT)
            flames.oquad(Q, n, f"ember_flame{v}", [(0, 0), (f1, 0), (f1, 1), (0, 1)], GLOW, ao=False)
        else:
            u = unit(up - nrm * (up @ nrm))
            r = np.cross(u, nrm)
            s_ = sz * 1.3
            Q = [p - r * s_ / 2 + u * s_ / 2, p + r * s_ / 2 + u * s_ / 2, p + r * s_ / 2 - u * s_ / 2, p - r * s_ / 2 - u * s_ / 2]
            v = int(rnd.integers(0, 3))
            _both_sides(bright, Q, nrm, f"ember_spray_dark{v % 2}" if low else f"ember_spray{v}", UV, LIT)


# ------------------------------------------------------------------ A: the Ember Oak (the mockup's ember_oak, ported)

def ember_oak(seed):
    """The tree in the planter's frame (its middle at x, z 0); returns (bed, tree, halos, sparks, columns). The bed is
    the basalt columns and the gravel: the renderer draws variant 0's under every variant and does not turn it, and
    tower/OpsTreeBlock collides with its columns (data/jugcraft/ops_trees/ember_oak_bed.json); the tree is the rest."""
    rnd = np.random.default_rng(seed)
    bed, m = Mesh(), Mesh()
    halos, sparks, columns = [], [], []
    # r183: the bark's plates from draws of their own (the rest of the tree comes out as approved); the full-bright
    # parts (the plates' sides, the leaves: r184) a mesh of their own, baked full bright
    prng, bright = np.random.default_rng(seed + 9001), Mesh()
    roots = []
    # the bed: hexagonal basalt columns, the outer ring at seat height, stepping up and down inside it
    pitch = 0.62
    col_r = EMBER_COLUMN_R
    for (x, z) in hexgrid(4.15, pitch):
        r = math.hypot(x, z)
        if r < EMBER_RING[0]:
            continue
        if r > EMBER_RING[1]:
            h = EMBER_SEAT + rnd.uniform(-0.04, 0.04)
        else:
            h = rnd.choice([0.62, 0.75, 0.9, 1.05]) + rnd.uniform(-0.05, 0.05)
        if r + col_r > EMBER_R - 0.01:
            continue                # (kept inside the tree's cells; its random draws taken all the same)
        bed.prism((x, 0, z), col_r, h, 6, "basalt_side", "basalt_top", rot0=math.pi / 6)
        columns.append((x, z, h))
    # r181 laid a sheet of soil under the columns, a little below the seat, for the gaps between them and round the soil
    # (Justin, 8 November 2026: "some gaps in here and between what I'm guessing is basalt and the soil"). It showed as a
    # lower, darker band between the soil and the columns and stuck out past the outer ones (Justin, 11 November 2026:
    # "it's discolored there in between the original circle and the basalt ... you overextended it outwards so that it's
    # going through the basalt on the outside"): gone (r212). The tree's own soil now runs on under the columns
    # (ember_soil: all of it the columns leave open is within 3.18 of the middle, and the seat's columns start at 3.42),
    # and the hairlines between the columns show basalt: under each, 0.03 below its top, its top again, wide enough to
    # meet its neighbours'. Nothing reaches out past the columns.
    for (x, z, h) in columns:
        bed.cap((x, h - 0.03, z), col_r / 0.985 + 0.0005, 6, "basalt_top", rot0=math.pi / 6)
    stones = []                     # (where, size, a coal?): laid on the soil once the roots are known (r185)
    scratch = Mesh()
    for k in range(70):
        a = rnd.uniform(0, 2 * math.pi)
        r = math.sqrt(rnd.uniform(0.6 ** 2, 2.8 ** 2))
        c = (r * math.cos(a), EMBER_GRAVEL, r * math.sin(a))
        if rnd.random() < 0.45:
            size = rnd.uniform(0.04, 0.09)
            scratch.rock(c, size, rnd, "coal", squash=0.55, n_lat=3, n_lon=5)       # (its draws kept: the rest comes out the same)
            stones.append((c, size, True))
        else:
            size = rnd.uniform(0.06, 0.14)
            scratch.rock(c, size, rnd, "basalt_top", squash=0.5, n_lat=3, n_lon=6)
            stones.append((c, size, False))
    # roots: out from the trunk's foot over the gravel, dipping in and out
    for k in range(7):
        a = 2 * math.pi * k / 7 + rnd.uniform(-0.25, 0.25)
        pts, rad = [], []
        L = rnd.uniform(1.6, 2.6)
        for i in range(12):
            f = i / 11
            rr = 0.35 + L * f
            y = EMBER_GRAVEL + 0.55 * (1 - f) ** 1.8 + 0.05 * math.sin(f * 8 + k)
            aa = a + 0.25 * math.sin(f * 4 + k)
            pts.append((rr * math.cos(aa), y, rr * math.sin(aa)))
            rad.append(0.24 * (1 - f) ** 1.2 + 0.03)
        roots.append((pts, flare(rad, (1.3, 1.12))))
    ember_soil(m, bright, [p for p, _ in roots], stones, np.random.default_rng(seed + 9003))
    # the trunk: three gnarled strands round each other, splitting into the limbs (cast with the roots and limbs: r183)
    strands = []
    for s_ in range(3):
        pts, rad = [], []
        for i in range(16):
            f = i / 15
            a = s_ * 2 * math.pi / 3 + f * 2.2
            rr = 0.32 * (1 - 0.3 * f)
            pts.append((rr * math.cos(a) + 0.04 * math.sin(i * 1.3 + s_), 0.45 + 2.0 * f, rr * math.sin(a)))
            rad.append(0.36 - 0.1 * f)
        strands.append((pts, rad))
    # the limbs: wide, low and spreading, like a flat-topped tree of the savannah
    limbs = []
    for k in range(6):
        a = 2 * math.pi * k / 6 + rnd.uniform(-0.3, 0.3)
        p = np.array([0.22 * math.cos(a), 2.3, 0.22 * math.sin(a)])
        elev = math.radians(rnd.uniform(16, 30))
        d = np.array([math.cos(a) * math.cos(elev), math.sin(elev), math.sin(a) * math.cos(elev)])
        pts, rad = limb(p, d, rnd.uniform(3.4, 4.6), 0.22, 0.05, rnd, step=0.22, droop=0.02, wander=0.12, crown_r=4.05, ceil=6.2,
                        rise=0.06)
        limbs.append((pts, rad, 0))
        for i in range(len(pts) // 2, len(pts) - 2):
            if rnd.random() < 0.35:
                dd = rot(unit(pts[i + 1] - pts[i]), Y, rnd.choice([-1, 1]) * rnd.uniform(0.5, 1.1))
                dd = unit(dd + Y * rnd.uniform(0.1, 0.5))
                cp, cr = limb(pts[i], dd, rnd.uniform(0.8, 1.8), rad[i] * 0.7, 0.03, rnd, step=0.18, wander=0.2, crown_r=4.0, ceil=6.6)
                limbs.append((cp, cr, 1))
    # r183 (Justin, 8 November 2026: "where the roots go to the trunk, where the trunk go to the limbs"): the trunk,
    # the roots, the limbs and the limbs' branches cast as one piece (ops_trees_molded), each swelling a little where it
    # leaves what it grows from; the thin ends go on as the plain bark and glowing cracks of before
    branches = [(pts, rad, 8, 1.4, 1) for pts, rad in roots]
    for pts, rad, lv in limbs:
        sm = chaikin(pts, 1)
        sr = list(np.interp(np.linspace(0, 1, len(sm)), np.linspace(0, 1, len(rad)), flare(rad) if lv == 0 else flare(rad, (1.35, 1.12))))
        branches.append((sm, sr, 8 if lv == 0 else 6, 1.2, lv + 1))
    for pts, rad, sides, vl in molded_trunk(m, bright, prng, strands, branches):
        # the thin ends: six sides and a ring every 0.16 or so are plenty that thin
        keep = [0, 1]
        for i in range(2, len(pts) - 1):
            if np.linalg.norm(pts[i] - pts[keep[-1]]) >= 0.16:
                keep.append(i)
        keep.append(len(pts) - 1)
        pts, rad = [pts[i] for i in keep], [rad[i] for i in keep]
        m.tube(pts, rad, "ember_bark", sides=6, cap=True, vlen=vl)
        m.tube(pts, overlay(rad), "ember_crack", sides=6, cap=False, vlen=vl, kind=GLOW)
    tips = []
    for pts, rad, lv in limbs:
        tips.append(pts[-1])
        if lv == 0 and len(pts) > 8:
            tips.append(pts[int(len(pts) * 0.72)])
    # the crown: flat pads of ember leaves over the limbs' ends (where each spray goes: the draws as approved; how it is
    # drawn: ember_leaves, r184)
    sprays = []
    for tp in tips:
        c = np.array(tp) + Y * 0.22
        if c[1] < 3.6:
            c[1] = 3.6 + rnd.uniform(0, 0.4)
        rx, ry = rnd.uniform(0.75, 1.15), rnd.uniform(0.2, 0.3)
        for j in range(int(130 * rx)):
            u = rnd.normal(0, 1, 3)
            u = u / np.linalg.norm(u) * rnd.uniform(0.3, 1.0) ** 0.5
            p = c + np.array([u[0] * rx, u[1] * ry, u[2] * rx])
            if math.hypot(p[0], p[2]) > 4.15 or p[1] > 7.4:
                continue
            nrm = unit(Y * 1.0 + rnd.normal(0, 0.45, 3))
            sz = rnd.uniform(0.32, 0.5)
            if rnd.random() >= 0.14:
                rnd.integers(0, 3)          # (the draws of the old hot leaves and leaf textures, kept so the rest comes out the same)
            sprays.append((p, nrm, unit(rnd.normal(0, 1, 3)), sz, u[1] < -0.25))
        for j in range(2):
            p = c + np.array([rnd.normal(0, rx * 0.5), rnd.normal(0, 0.15), rnd.normal(0, rx * 0.5)])
            halos.append([p[0], p[1], p[2], rnd.uniform(0.5, 0.8), rnd.uniform(0, 2 * math.pi)])
    flames = Mesh()
    ember_leaves(sprays, bright, flames, np.random.default_rng(seed + 9002))
    # sparks drifting up out of the crown: where each starts, how high it goes, its size, speed and phase
    for k in range(55):
        a = rnd.uniform(0, 2 * math.pi)
        r = math.sqrt(rnd.uniform(0, 3.7 ** 2))
        y0 = rnd.uniform(3.4, 5.2)
        sparks.append([r * math.cos(a), r * math.sin(a), y0, rnd.uniform(6.6, 7.6), rnd.uniform(0.025, 0.045), rnd.uniform(0.25, 0.6),
                       rnd.uniform(0, 1)])
    m.bright, m.flames = bright, flames
    return bed, m, halos, sparks, columns


# ------------------------------------------------------------------ D: the Drift Isle (the mockup's drift_isle, ported)

def drift_base():
    """The basin, its emitter rings and lift beam, the seat round it (the same under every island)."""
    m = Mesh()
    r_in, r_out = DRIFT_BASIN
    m.wall((0, 0, 0), r_out, 0.0, DRIFT_WALL, "slate", seg=64, ulen=1.0)
    m.annulus((0, 0, 0), r_in, r_out, DRIFT_WALL, "slate", seg=64, ulen=1.0)
    m.wall((0, 0, 0), r_in, DRIFT_FLOOR, DRIFT_WALL, "slate", seg=64, out=False, ulen=1.0)
    m.planar((0, 0, 0), 0.0, r_in, DRIFT_FLOOR, "slate", 1.0)
    for r0 in (1.0, 1.9):
        m.annulus((0, 0, 0), r0, r0 + 0.07, DRIFT_FLOOR + 0.007, "emitter", seg=48, kind=LIT)
    # the seat: slats on a dark plinth
    m.wall((0, 0, 0), SEAT_R, 0.0, 0.4, "slate", seg=64, ulen=1.0)
    m.annulus((0, 0, 0), r_out, SEAT_R, 0.42, "dark_wood", seg=64, ulen=1.0)
    m.wall((0, 0, 0), SEAT_R, 0.4, 0.46, "dark_wood", seg=64, ulen=2.0)
    m.annulus((0, 0, 0), 3.25, SEAT_R, 0.46, "dark_wood", seg=64, ulen=1.0)
    # the lift beam: a soft column of blue light from the rings up to the rock (v 0 at its top)
    for k in range(16):
        a0, a1 = 2 * math.pi * k / 16, 2 * math.pi * (k + 1) / 16
        for r in (1.95, 1.2):
            p = [np.array([r * math.cos(a), y, r * math.sin(a)]) for a, y in ((a0, 2.0), (a1, 2.0), (a1, 0.215), (a0, 0.215))]
            m.oquad(p, np.array([math.cos(a0 + a1), 0, math.sin(a0 + a1)]), "beam", [(0, 0), (1, 0), (1, 1), (0, 1)], GLOW)
    return m


def drift_isle(seed):
    """The floating island with its tree, in the planter's frame; returns (mesh, root tips, stones)."""
    rnd = np.random.default_rng(seed)
    m = Mesh()
    tips, stones = [], []
    cy = DRIFT_ISLE_Y
    NT, NR = 48, 9
    noise = [(rnd.uniform(0.6, 1.4), rnd.integers(2, 7), rnd.uniform(0, 6.3)) for _ in range(6)]

    def rim(a):
        return 2.05 + sum(0.07 * amp * math.sin(k * a + ph) for amp, k, ph in noise)

    def top_y(r, a):
        return cy + 0.1 * (1 - (r / 2.1) ** 2) + 0.03 * math.sin(5 * a + r * 3)

    def under(r, a):
        f = r / 2.1
        depth = 1.55 * (1 - f ** 1.5) + 0.12 * math.sin(7 * a + 2 * r) * f + 0.09 * math.sin(11 * a) * f
        return cy - 0.05 - max(0.0, depth)
    grid_t, grid_u = [], []
    for i in range(NR + 1):
        f = i / NR
        rt, ru = [], []
        for j in range(NT):
            a = 2 * math.pi * j / NT
            r = rim(a) * f
            jit = rnd.uniform(0.92, 1.06) if 0 < i < NR else 1.0
            rt.append(np.array([r * math.cos(a), top_y(r, a), r * math.sin(a)]))
            ru.append(np.array([r * jit * math.cos(a), under(r, a) - (rnd.uniform(0, 0.05) if 0 < i < NR else 0), r * jit * math.sin(a)]))
        grid_t.append(rt)
        grid_u.append(ru)
    span = 2.45
    for i in range(NR):
        for j in range(NT):
            k = (j + 1) % NT
            P = [grid_t[i + 1][j], grid_t[i + 1][k], grid_t[i][k], grid_t[i][j]]
            m.oquad(P, Y, "moss", [(0.5 + p[0] / (2 * span), 0.5 + p[2] / (2 * span)) for p in P])
            P = [grid_u[i + 1][j], grid_u[i + 1][k], grid_u[i][k], grid_u[i][j]]
            nrm = np.mean(P, 0) - np.array([0, cy + 0.8, 0])
            u0, u1 = j / NT * 9, (j + 1) / NT * 9
            m.oquad(P, nrm, "isle_rock" if i >= NR - 2 else "isle_under",
                    [(u0, (cy - P[0][1]) / 0.55), (u1, (cy - P[1][1]) / 0.55), (u1, (cy - P[2][1]) / 0.55), (u0, (cy - P[3][1]) / 0.55)])
    # (the rim's lip: a band of rock between the moss and the underside, down at least to the underside's edge, which
    # where the rim draws in sits lower than the band's 0.22: r181, Justin, 8 November 2026, "you can see into the
    # texture" through the gaps that left)
    def lip_foot(j):
        return np.array([grid_t[NR][j][0], min(grid_t[NR][j][1] - 0.22, grid_u[NR][j][1]), grid_t[NR][j][2]])
    for j in range(NT):
        k = (j + 1) % NT
        lip = [grid_t[NR][j], grid_t[NR][k], lip_foot(k), lip_foot(j)]
        u0, u1 = j / NT * 9, (j + 1) / NT * 9
        m.oquad(lip, np.array([grid_t[NR][j][0] + grid_t[NR][k][0], 0, grid_t[NR][j][2] + grid_t[NR][k][2]]), "isle_rock",
                [(u0, 0), (u1, 0), (u1, (grid_t[NR][k][1] - lip[2][1]) / 0.55), (u0, (grid_t[NR][j][1] - lip[3][1]) / 0.55)])
    # grass and small plants on the moss
    for k in range(120):
        a = rnd.uniform(0, 2 * math.pi)
        r = math.sqrt(rnd.uniform(0, 1.95 ** 2))
        # (r181: the crossed leaf sprays are gone for shrubs, laid after the boulders; their draws kept so the rest of
        # the island comes out as approved)
    # boulders on top, roots hanging below with glowing tips
    boulders = []
    for k in range(7):
        a = rnd.uniform(0, 2 * math.pi)
        r = rnd.uniform(0.6, 1.8)
        size = rnd.uniform(0.15, 0.32)
        m.rock((r * math.cos(a), cy + 0.05, r * math.sin(a)), size, rnd, "isle_rock", squash=0.55)
        boulders.append((r * math.cos(a), r * math.sin(a), size))
    # the plants on the moss: round shrubs of small leaves, kept off the trunk's foot and the boulders (their own draws)
    frng = np.random.default_rng(seed + 7001)
    shrubs = []
    foot = np.array([-0.2, 0.1 + 0.35 * math.sin(1.2)])
    for k in range(400):
        if len(shrubs) >= 15:
            break
        a = frng.uniform(0, 2 * math.pi)
        r = math.sqrt(frng.uniform(0.3 ** 2, 1.8 ** 2))
        x, z = r * math.cos(a), r * math.sin(a)
        s = frng.uniform(0.12, 0.26)
        if math.hypot(x - foot[0], z - foot[1]) < 0.55 + s or math.hypot(x, z) + s > 1.95:
            continue
        if any(math.hypot(x - bx, z - bz) < bs + s + 0.05 for bx, bz, bs in boulders):
            continue
        if any(math.hypot(x - sx, z - sz) < (s + ss) * 0.8 for sx, sz, ss in shrubs):
            continue
        shrubs.append((x, z, s))
        y = top_y(r, a)
        m.clump((x, y + s * 0.25, z), s, s * 0.7, s * frng.uniform(0.85, 1.1), frng, "shrub_foliage",
                ["shrub_leaf0", "shrub_leaf1", "shrub_leaf2"], 0.1, flat=0.3, density=0.9, under=0.0, tilt=(0.2, 0.5),
                shade=(0.6, 1.0), tile=0.6, sink=y + 0.01)
    for k in range(16):
        a = rnd.uniform(0, 2 * math.pi)
        r = rnd.uniform(0.3, 1.5)
        y0 = cy - 0.4 - 1.1 * (1 - r / 1.7)
        pts = [(r * math.cos(a), y0, r * math.sin(a))]
        L = rnd.uniform(0.5, 1.2)
        for i in range(1, 7):
            f = i / 6
            pts.append((r * math.cos(a) + 0.12 * math.sin(f * 4 + k), y0 - L * f, r * math.sin(a) + 0.12 * math.cos(f * 3 + k)))
        if pts[-1][1] < 0.9:
            continue
        m.tube(pts, [0.05 * (1 - 0.7 * i / 6) for i in range(7)], "bonsai_bark", sides=5)
        tips.append([*pts[-1], 0.06, rnd.uniform(0, 2 * math.pi)])
    # the tree: an old trunk rising in an S out of the rock, branches reaching sideways, foliage in flat pads
    pts, rad = [], []
    for i in range(18):
        f = i / 17
        pts.append((0.55 * math.sin(f * 4.2) * (1 - 0.3 * f) - 0.2, cy + 0.05 + 2.4 * f, 0.35 * math.sin(f * 3.1 + 1.2) * (1 - f) + 0.1))
        rad.append(0.36 * (1 - f) ** 0.9 + 0.08)
    m.tube(chaikin(pts, 1), list(np.interp(np.linspace(0, 1, 36), np.linspace(0, 1, 18), rad)), "bonsai_bark", sides=10, vlen=1.5)
    for k in range(6):
        a = 2 * math.pi * k / 6 + rnd.uniform(-0.3, 0.3)
        fp = [(pts[0][0] + 0.15 * math.cos(a), cy + 0.35, pts[0][2] + 0.15 * math.sin(a))]
        for i in range(1, 6):
            f = i / 5
            fp.append((pts[0][0] + (0.15 + 0.9 * f) * math.cos(a + 0.2 * f), cy + 0.32 * (1 - f) ** 1.5 + 0.06,
                       pts[0][2] + (0.15 + 0.9 * f) * math.sin(a + 0.2 * f)))
        m.tube(fp, [0.16 * (1 - f) + 0.03 for f in np.linspace(0, 1, 6)], "bonsai_bark", sides=6)
    pads = []
    for k in range(7):
        i = rnd.integers(7, 17)
        base = np.array(pts[i])
        a = rnd.uniform(0, 2 * math.pi)
        d = unit(np.array([math.cos(a), rnd.uniform(0.05, 0.4), math.sin(a)]))
        lp, lr = limb(base, d, rnd.uniform(1.4, 2.6), rad[i] * 0.55, 0.04, rnd, step=0.18, droop=0.1, wander=0.18, crown_r=3.5, ceil=7.2)
        sm = chaikin(lp, 1)
        m.tube(sm, list(np.interp(np.linspace(0, 1, len(sm)), np.linspace(0, 1, len(lr)), lr)), "bonsai_bark", sides=6, vlen=1.0)
        pads.append(np.array(lp[-1]))
        pads.append(np.array(lp[len(lp) // 2]))
    pads.append(np.array(pts[-1]))
    for c in pads:
        c = c + Y * 0.22
        rx, ry = rnd.uniform(0.6, 1.0), rnd.uniform(0.25, 0.38)
        # (r181: the old sprays' draws kept, so what follows comes out as before)
        for j in range(int(170 * rx)):
            u = rnd.normal(0, 1, 3)
            u = u / np.linalg.norm(u) * rnd.uniform(0.25, 1.0) ** 0.5
            p = c + np.array([u[0] * rx, u[1] * ry + 0.05, u[2] * rx])
            if math.hypot(p[0], p[2]) > 4.0 or p[1] > 7.5:
                continue
            rnd.normal(0, 0.5, 3), rnd.uniform(0.3, 0.46), rnd.normal(0, 1, 3), rnd.integers(0, 3)
        # a cloud of foliage: a broad flat-topped clump and two or three smaller ones round it, thick with leaves
        # (kept under the hall's ceiling and inside the tree's 4 blocks, as the sprays were)
        lr = frng.uniform(0.88, 0.98)
        c = c + Y * 0.04
        wide = max(0.25, min(rx * lr, 3.8 - math.hypot(c[0], c[2])))
        tall = max(0.12, min(ry * 1.05, (CEIL - 0.45 - c[1]) / 1.2))
        bound = dict(reach=4.0, ceil=CEIL - 0.2)
        m.clump(c, wide, tall, wide * frng.uniform(0.8, 1.0), frng, "drift_foliage",
                ["drift_leafs0", "drift_leafs1", "drift_leafs2"], 0.28, flat=0.5, density=1.1, under=0.3, n=3, **bound)
        for q in range(int(frng.integers(2, 4))):
            a = frng.uniform(0, 2 * math.pi)
            f = frng.uniform(0.5, 0.7)
            off = np.array([math.cos(a) * wide * 0.75, tall * frng.uniform(-0.2, 0.3), math.sin(a) * wide * 0.75])
            m.clump(c + off, wide * f, min(tall * f * 1.3, max(0.1, (CEIL - 0.45 - c[1] - off[1]) / 1.2)), wide * f, frng,
                    "drift_foliage", ["drift_leafs0", "drift_leafs1", "drift_leafs2"], 0.25, flat=0.5, density=1.1, under=0.3, **bound)
    # small stones circling it: orbit radius, height, starting angle, size, how fast it goes round, its bob's phase
    for k in range(5):
        a = 2 * math.pi * k / 5 + rnd.uniform(-0.3, 0.3)
        stones.append([rnd.uniform(2.6, 3.3), rnd.uniform(2.2, 4.6), a, rnd.uniform(0.12, 0.22), rnd.uniform(0.10, 0.22) * rnd.choice([-1, 1]),
                       rnd.uniform(0, 2 * math.pi)])
    return m, tips, stones


def drift_stone():
    m = Mesh()
    m.rock((0, 0, 0), 1.0, np.random.default_rng(91), "isle_rock", squash=0.7, n_lat=5, n_lon=8)
    return m


# ------------------------------------------------------------------ cutting repeats, shading, writing

def _cross(v0, v1, n, eps=1e-7):
    lo, hi = min(v0, v1), max(v0, v1)
    if lo + eps < n < hi - eps:
        return (n - v0) / (v1 - v0)
    return None


def split_repeats(q):
    """The quad cut where its texture repeats (uv crossing a whole number), each piece's uv brought into 0..1."""
    pieces = [(q["P"], q["uv"], q["cn"], q["bright"])]
    for axis in (0, 1):
        out = []
        while pieces:
            P, UV, CN, BR = pieces.pop()
            vals = UV[:, axis]
            n = math.floor(vals.min() + 1e-6) + 1
            if n >= vals.max() - 1e-6:
                out.append((P, UV, CN, BR))
                continue
            done = False
            for (a0, a1), (b0, b1), order in (((0, 3), (1, 2), "t"), ((0, 1), (3, 2), "s")):
                fa, fb = _cross(vals[a0], vals[a1], n), _cross(vals[b0], vals[b1], n)
                if fa is None or fb is None:
                    continue

                def lerp(arr, i, j, f):
                    return None if arr is None else arr[i] + (arr[j] - arr[i]) * f
                A = [lerp(x, a0, a1, fa) for x in (P, UV, CN, BR)]
                B = [lerp(x, b0, b1, fb) for x in (P, UV, CN, BR)]
                parts = []
                for idx, src in ((0, P), (1, UV), (2, CN), (3, BR)):
                    if src is None:
                        parts.append((None, None))
                        continue
                    if order == "t":
                        first = np.array([src[0], src[1], B[idx], A[idx]])
                        second = np.array([A[idx], B[idx], src[2], src[3]])
                    else:
                        first = np.array([src[0], A[idx], B[idx], src[3]])
                        second = np.array([A[idx], src[1], src[2], B[idx]])
                    parts.append((first, second))
                pieces.append(tuple(p[0] for p in parts))
                pieces.append(tuple(p[1] for p in parts))
                done = True
                break
            if not done:
                out.append((P, UV, CN, BR))
        pieces = out
    res = []
    for P, UV, CN, BR in pieces:
        UV = UV - np.floor(UV.min(0) + 1e-6)
        r = dict(q)
        r.update(P=P, uv=np.clip(UV, 0, 1), cn=CN, bright=BR)
        res.append(r)
    return res


def finish(m, ao=True):
    """Cut the repeating tiles' quads at their repeats and bake the corner shading (TCC.bake's ambient occlusion on what
    the world lights, times each quad's own brightness)."""
    quads = []
    for q in m.quads:
        if q["mat"] in TILED:
            quads += split_repeats(q)
        else:
            q = dict(q)
            q["uv"] = np.clip(q["uv"], 0, 1)
            quads.append(q)
    m.quads = quads
    if ao and any(q["ao"] for q in quads):
        TCC.bake(m, floor_y=0.0, res=1 / 16, reach=0.6, rays=16)
    for q in m.quads:
        s = np.asarray(q.get("shade", [1.0] * 4), float)
        if q["kind"] == SOLID and not q["ao"]:
            s = s * 0.88
        if q["bright"] is not None:
            s = s * q["bright"]
        q["shade"] = [float(min(1.0, max(0.0, v))) for v in s]
    return m


def write_mesh(m, atlas, path, offset=(CX, 0.0, CZ)):
    """ConsoleMesh's format ("JTCM" 1): quads by kind (four corners counter-clockwise as Minecraft draws front faces:
    x y z, normal as bytes, u v, shade 0-255), no live faces, no step edges, three unused tiles."""
    off = np.asarray(offset, float)
    path.parent.mkdir(parents=True, exist_ok=True)
    buf = bytearray(struct.pack(">iii", TCC.MAGIC, 1, len(m.quads)))
    for q in m.quads:
        P = q["P"] + off
        uv = [atlas.uv(q["mat"], u, v) for u, v in q["uv"]]
        cn = q["cn"] if q["cn"] is not None else [q["n"]] * 4
        buf += struct.pack(">b", q["kind"])
        for k in (0, 3, 2, 1):
            nb = [int(round(max(-1, min(1, v)) * 127)) for v in unit(cn[k])]
            buf += struct.pack(">fffbbbffB", P[k][0], P[k][1], P[k][2], nb[0], nb[1], nb[2], uv[k][0], uv[k][1],
                               int(round(255 * q["shade"][k])))
    buf += struct.pack(">ii", 0, 0)
    for _ in range(3):
        buf += struct.pack(">ffff", 0, 0, 0, 0)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


def _r(v, d=4):
    return [round(float(x), d) for x in v]


def finish_with(bed, tree):
    """The tree baked with the bed under it (each shades the other); returns the two."""
    both = Mesh()
    both.quads = [dict(q, part=0) for q in bed.quads] + [dict(q, part=1) for q in tree.quads]
    finish(both)
    a, b = Mesh(), Mesh()
    a.quads = [q for q in both.quads if q["part"] == 0]
    b.quads = [q for q in both.quads if q["part"] == 1]
    return a, b


def build():
    """Every mesh, the map and the Ember Oak's columns; nothing written."""
    meshes = {}
    info = {EMBER: [], DRIFT: []}
    bed0 = columns0 = None
    for k, seed in enumerate(SEEDS[EMBER]):
        bed, tree, halos, sparks, columns = ember_oak(seed)
        if k == 0:
            bed0, columns0 = bed, columns
        fb, ft = finish_with(bed0, tree)
        if k == 0:
            meshes["ops_ember_bed"] = fb
        meshes[f"ops_ember_oak_{k}"] = ft
        meshes[f"ops_ember_oak_{k}_bright"] = finish(tree.bright, ao=False)
        meshes[f"ops_ember_oak_{k}_flame"] = finish(tree.flames, ao=False)
        info[EMBER].append({"halos": [_r([h[0] + CX, h[1], h[2] + CZ, h[3], h[4]]) for h in halos],
                            "sparks": [_r([s[0] + CX, s[1] + CZ, *s[2:]]) for s in sparks]})
    meshes["ops_drift_isle_base"] = finish(drift_base())
    for k, seed in enumerate(SEEDS[DRIFT]):
        m, tips, stones = drift_isle(seed)
        meshes[f"ops_drift_isle_{k}"] = finish(m)
        info[DRIFT].append({"tips": [_r([t[0] + CX, t[1], t[2] + CZ, t[3], t[4]]) for t in tips], "stones": [_r(s) for s in stones]})
    meshes["ops_drift_stone"] = finish(drift_stone(), ao=False)
    bed_json = {"column_r": round(EMBER_COLUMN_R, 6), "gravel_r": EMBER_GRAVEL_R, "gravel_y": EMBER_GRAVEL,
                "columns": [_r(c) for c in columns0]}
    return meshes, info, bed_json


RECIPES = {
    EMBER: (["BBAABB", "LLMMLL", "SSKKSS"], {"B": "minecraft:blaze_powder", "A": "minecraft:acacia_sapling", "L": "minecraft:dark_oak_log",
                                             "M": "minecraft:magma_block", "S": "minecraft:basalt", "K": "minecraft:coal_block"}),
    DRIFT: (["PPAAPP", "MMEEMM", "DDWWDD"], {"P": "minecraft:phantom_membrane", "A": "minecraft:flowering_azalea", "M": "minecraft:moss_block",
                                             "E": "minecraft:ender_pearl", "D": "minecraft:polished_deepslate", "W": "minecraft:dark_oak_planks"}),
}


def recipe(tree):
    """The Advanced Workbench's recipe (as the Crystal Tree's: two trees a craft)."""
    pattern, key = RECIPES[tree]
    return {"fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"},
                                       {"condition": "jugcraft:feature_enabled", "feature": "drones"}],
            "type": "jugcraft:advanced_crafting", "pattern": pattern, "key": dict(sorted(key.items())),
            "result": {"id": f"jugcraft:{tree}", "count": 2}}


def write_data():
    j = TCC._json
    for tree in (EMBER, DRIFT):
        part = f"{tree}_part"
        j(ASSETS / "blockstates" / f"{tree}.json", {"variants": {"": {"model": f"jugcraft:block/{tree}"}}})
        j(ASSETS / "blockstates" / f"{part}.json", {"variants": {"": {"model": f"jugcraft:block/{tree}"}}})
        j(ASSETS / "models/block" / f"{tree}.json",
          {"textures": {"particle": "minecraft:block/basalt_side" if tree == EMBER else "minecraft:block/moss_block"}, "elements": []})
        j(ASSETS / "models/item" / f"{tree}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{tree}"}})
        j(ASSETS / "items" / f"{tree}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{tree}"}})
        j(DATA / "jugcraft/loot_table/blocks" / f"{tree}.json", TCC._loot(tree))
        j(DATA / "jugcraft/loot_table/blocks" / f"{part}.json", {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{part}"})
        j(DATA / "jugcraft/recipe" / f"{tree}.json", recipe(tree))
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    for tree in (EMBER, DRIFT):
        lang[f"block.jugcraft.{tree}"] = NAMES[tree]
        lang[f"block.jugcraft.{tree}_part"] = NAMES[tree]
    lang[f"message.jugcraft.{EMBER}.blocked"] = "The Ember Oak needs a clear round space 9 blocks across, and 3 high in the middle"
    lang[f"message.jugcraft.{DRIFT}.blocked"] = "The Drift Isle needs a clear round space 9 blocks across, and the island's place over it"
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for tree in (EMBER, DRIFT):
        for i in (tree, f"{tree}_part"):
            if f"jugcraft:{i}" not in t["values"]:
                t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")


# ------------------------------------------------------------------ item icons (16 x 16, in the style of tools/item_icons.py)

ICON_COLOURS = {
    ".": None, "k": (22, 20, 22), "r": (150, 38, 18), "o": (222, 92, 28), "y": (255, 176, 70), "b": (46, 36, 32), "c": (255, 132, 40),
    "g": (62, 64, 70), "G": (98, 100, 108), "v": (120, 50, 170), "V": (176, 104, 226), "m": (70, 128, 60), "s": (120, 112, 104),
    "S": (78, 72, 70), "w": (96, 70, 52), "u": (110, 200, 255), "U": (190, 236, 255), "d": (52, 56, 64), "t": (120, 84, 54),
}
ICONS = {
    EMBER: ["....kkkk.kkkk...",
            "..kkroorkoyorkk.",
            ".krooyorooroyork",
            ".kroorrooyoorrk.",
            "..kkrkbbkrkkkk..",
            "......kbck......",
            ".....kbcbk......",
            ".....kcbbk......",
            ".....kbbck......",
            ".....kbcbk......",
            "....kbcbbck.....",
            "..kkbckkkbckk...",
            ".kGGkGGkGGkGGk..",
            ".kggkggkggkggk..",
            ".kggkggkggkggk..",
            "..kkkkkkkkkkk..."],
    DRIFT: [".....kkkkkk.....",
            "...kkvVvvVvkk...",
            "..kvVvvVvvVvvk..",
            "..kvvVvvvVvVvk..",
            "...kkkvwwkkkk...",
            "......kwwk......",
            ".....kwwk.......",
            "..kkkkmmmmkkkk..",
            "..kmmmmmmmmmmk..",
            "...ksssssssSk...",
            "....kSssssSk....",
            ".....kuSSuk.....",
            "....kUuuuuUk....",
            ".kkkkUuuuuUkkkk.",
            ".kttddddddddttk.",
            "..kkkkkkkkkkkk.."],
}


def icon(tree):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ICONS[tree]):
        for x, ch in enumerate(row):
            c = ICON_COLOURS[ch]
            if c is not None:
                img.putpixel((x, y), c + (255,))
    return img



def write_all():
    atlas = Atlas(tiles())
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    meshes, info, bed_json = build()
    for name, m in meshes.items():
        write_mesh(m, atlas, MESH_DIR / f"{name}.bin.gz")
    TCC._json(ATLAS_MAP, {"atlas": list(ATLAS), "tiles": atlas.tiles_map(), "variants": VARIANTS,
                          "flame_frames": FLAME_FRAMES, "flame_step": LEAF_TEX / ATLAS[0], **info})
    TCC._json(BED_JSON, bed_json)
    write_data()
    for tree in (EMBER, DRIFT):
        icon(tree).save(ASSETS / f"textures/item/{tree}.png")
    return meshes, info, bed_json


if __name__ == "__main__":
    meshes, info, _ = write_all()
    for name, m in meshes.items():
        kinds = [q["kind"] for q in m.quads]
        size = (MESH_DIR / f"{name}.bin.gz").stat().st_size
        print(f"{name}: {len(m.quads)} quads {dict((k, kinds.count(k)) for k in sorted(set(kinds)))}, {size} bytes")
