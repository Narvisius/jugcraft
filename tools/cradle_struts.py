"""The Cradle Struts: the pair of raked steel struts under each of the Tri-Pillar's supply cradles (work list item 12,
"Steel girders that join into one solid beam, straight or raked (the cradle supports)", approved by Justin on
23 October 2026 after three takes; docs/features/drone-tower-tripillar.md). They replaced the staircases of steel
girder blocks the cradles stood on (Justin: "the cradle's steel girders look tacky stacked").

Each strut is one wide-flange member from the pillar's chamfer up to the cradle's underside, its ends cut in the planes
they land on so it sits flat on its plates (take 1: "where the beam meets the metal that secures it ... it doesn't sit
against it right"):
- the foot: cut in the chamfer's plane onto an end plate, bolted to a bracket plate on the chamfer, a seat angle under
  them;
- the head: a block in under the cradle (off its hazard edge, take 2: "the beam to the socket ... cap plate"), cut flat
  onto an end plate on a bearing plate fixed to the deck's underside, four bolts up through both;
- the bracing between the two: gusset plates welded to the struts' webs at a fifth, half and four fifths of their
  length, standing out past the flange tips; flat-bar braces lying on the gussets' faces, square-ended, two bolts at
  each end: a tie at mid-length and an X in each bay.

The geometry is the approved mockup's, with the head's end plate moved down onto the member's cut end (the mockup left a
hairline between them) and the long faces cut in half-block pieces so their shading can vary along them.

Writes:
- assets/jugcraft/tower_mesh/cradle_struts.bin.gz: the mesh (the console mesh format, client/ConsoleMesh) in the master
  cell's frame for the canonical cradle (its diagonal toward -x +z, `facing=south`), its texture
  textures/entity/cradle_struts.png;
- assets/jugcraft/tower_mesh/cradle_struts.json: the cells the struts pass through (the master, the foot of the strut on
  the cradle's left seen from the pillar, first) and each one's collision boxes (pixels, the cell's own frame), read by
  tower/CradleStrutsBlock on both sides;
- the blockstates, item, lang, loot tables, recipe and pickaxe tag of `cradle_struts` and `cradle_struts_part`.

One item places the pair whole (tower/CradleStrutsBlock): its master is the left strut's foot cell, its other cells are
Cradle Struts Parts (no item) that know their index in the footprint; `facing` turns the canonical pair a quarter at a
time (south: the diagonal -x +z; west: -x -z; north: +x -z; east: +x +z). client/CradleStrutsRenderer draws it.
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image

import tower_control_console as TCC

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"

ID = "cradle_struts"
PART_ID = "cradle_struts_part"
NAME = "Cradle Struts"
MESH = ASSETS / "tower_mesh/cradle_struts.bin.gz"
TEXTURE = ASSETS / "textures/entity/cradle_struts.png"
FOOTPRINT = ASSETS / "tower_mesh/cradle_struts.json"
ATLAS = (256, 128)

# The canonical cradle: its diagonal (out from the pillar), the across vector from the left strut to the right one.
DIAG = (-1, 1)
FACINGS = {(-1, 1): "south", (-1, -1): "west", (1, -1): "north", (1, 1): "east"}
TURNS = {"south": 0, "west": 1, "north": 2, "east": 3}
# The struts' foot cells (the master's first), the deck's underside over them (blocks over the feet's floor) and how far
# the heads reach out along the diagonal from the feet (cells, then the block further in under the cradle).
FEET = ((0, 0, 0), (2, 0, 2))
RISE = 7
REACH = 3
HEAD_IN = 1.0
# The members (blocks): a wide-flange section (depth, width, flange, web, root fillet); the plates.
SECTION = (0.62, 0.46, 0.08, 0.055, 0.035)
FOOT_GAP = 0.11          # the foot's cut plane off the chamfer: the bracket plate (0.06) and the end plate (0.05)
PLATE, BRACKET, BEARING = 0.05, 0.06, 0.05
HEAD_GAP = PLATE + BEARING
GUSSETS = (0.2, 0.5, 0.8)
BRACES = ((0.5, 0.5), (0.2, 0.5), (0.5, 0.2), (0.5, 0.8), (0.8, 0.5))
GUSSET_REACH = 0.23 + 0.16
BAR = (0.16, 0.035)
Y = np.array((0, 1.0, 0))


def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


# ------------------------------------------------------------------ geometry (from the approved mockup)

class Mesh(TCC.Mesh):
    """The console mesh plus the solids that collide (convex hexahedra: eight corners by (end, side, side))."""

    def __init__(self):
        super().__init__()
        self.solids = []


def frame(p0, p1, up):
    p0, p1, up = (np.array(v, float) for v in (p0, p1, up))
    t = p1 - p0
    L = np.linalg.norm(t)
    t /= L
    n = up - np.dot(up, t) * t
    if np.linalg.norm(n) < 1e-6:
        n = np.array((1.0, 0, 0)) - t[0] * t
    n /= np.linalg.norm(n)
    b = np.cross(t, n)
    return p0, t, n, b, L


SEGMENT = 0.5             # a long face is cut into pieces this long (blocks), so its ambient occlusion can vary along it
_patch = [12345]


def _rand():
    _patch[0] = (_patch[0] * 1103515245 + 12345) & 0x7FFFFFFF
    return _patch[0]


def face(m, a0, a1, b1, b0, want, mat):
    """A face from a0 along to a1 (b0 to b1 the edge opposite), cut into pieces SEGMENT long, the noise patch the whole
    face takes (half a tile a block, at most 0.9 of it, at a varying offset) shared out among them, so it reads as one."""
    a0, a1, b1, b0 = (np.asarray(v, float) for v in (a0, a1, b1, b0))
    lu = max(np.linalg.norm(a1 - a0), np.linalg.norm(b1 - b0))
    lv = max(np.linalg.norm(b0 - a0), np.linalg.norm(b1 - a1))
    sw, sh = min(lu * 0.5, 0.9), min(lv * 0.5, 0.9)
    r = _rand()
    ou = (r % 997) / 997 * (1 - sw)
    ov = ((r // 997) % 991) / 991 * (1 - sh)
    n = max(1, int(math.ceil(lu / SEGMENT - 1e-9)))
    for k in range(n):
        f0, f1 = k / n, (k + 1) / n
        pts = [a0 + (a1 - a0) * f0, a0 + (a1 - a0) * f1, b0 + (b1 - b0) * f1, b0 + (b1 - b0) * f0]
        uvf = [(ou + sw * f0, ov), (ou + sw * f1, ov), (ou + sw * f1, ov + sh), (ou + sw * f0, ov + sh)]
        m.oquad([tuple(v) for v in pts], tuple(want), mat, uvf=uvf)


def _keyed_face(m, corners, keys, want, mat):
    """A face given by four corner keys (i: the end, j, k: the section's sides), cut along the member if it runs along it."""
    k0, k1, k2, k3 = keys
    if k0[0] == k1[0] and k1[0] != k2[0]:
        k0, k1, k2, k3 = k1, k2, k3, k0
    face(m, corners[k0], corners[k1], corners[k2], corners[k3], want, mat)


def obox(m, origin, t, n, b, lo, hi, mat, solid=False):
    """A box in the frame (t, n, b) at origin: lo/hi are (along t, along n, along b)."""
    def P(a, c, d):
        return origin + t * a + n * c + b * d
    (a0, c0, d0), (a1, c1, d1) = lo, hi
    corners = {}
    for i, a in enumerate((a0, a1)):
        for j, c in enumerate((c0, c1)):
            for k, d in enumerate((d0, d1)):
                corners[(i, j, k)] = P(a, c, d)
    faces = [((0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 0, 1), -t), ((1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0), t),
             ((0, 0, 0), (0, 0, 1), (1, 0, 1), (1, 0, 0), -n), ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1), n),
             ((0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), -b), ((0, 0, 1), (0, 1, 1), (1, 1, 1), (1, 0, 1), b)]
    for q in faces:
        _keyed_face(m, corners, q[:4], q[4], mat)
    if solid:
        m.solids.append(corners)


def hexa(m, corners, mat):
    """A six-faced solid from its eight corners: corners[(i, j, k)] with i the end (0, 1), j, k the section's sides."""
    faces = [((0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 0, 1)), ((1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)),
             ((0, 0, 0), (0, 0, 1), (1, 0, 1), (1, 0, 0)), ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)),
             ((0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)), ((0, 0, 1), (0, 1, 1), (1, 1, 1), (1, 0, 1))]
    centre = sum(corners.values()) / 8
    for q in faces:
        pts = [corners[k] for k in q]
        n = np.cross(pts[1] - pts[0], pts[3] - pts[0])
        if np.linalg.norm(n) < 1e-9:
            continue
        if np.dot(n, sum(pts) / 4 - centre) < 0:
            n = -n
        _keyed_face(m, corners, q, n / np.linalg.norm(n), mat)


def cut_corners(o, t, n, b, rect, planes):
    """The corners of a prism along t through the section rect ((n0, b0), (n1, b1)), its ends cut in the planes."""
    (n0, b0), (n1, b1) = rect
    corners = {}
    for i, (pp, pn) in enumerate(planes):
        pp, pn = np.array(pp, float), np.array(pn, float)
        for j, c in enumerate((n0, n1)):
            for k, d in enumerate((b0, b1)):
                q = o + n * c + b * d
                s = np.dot(pp - q, pn) / np.dot(t, pn)
                corners[(i, j, k)] = q + t * s
    return corners


def fitted_ibeam(m, p0, p1, up, planes):
    """A wide-flange section along p0 -> p1, its ends cut to fit the planes it lands on (so it sits flat on them).
    Returns its frame and every corner its cut ends make in each plane; its whole section collides."""
    depth, width, tf, tw, fillet = SECTION
    o, t, n, b, L = frame(p0, p1, up)
    d2, w2 = depth / 2, width / 2
    parts = [(((d2 - tf, -w2), (d2, w2)), "paint"), (((-d2, -w2), (-d2 + tf, w2)), "paint"),
             (((-d2 + tf, -tw / 2), (d2 - tf, tw / 2)), "paint_dk")]
    for c in (d2 - tf - fillet, -d2 + tf):
        parts.append((((c, tw / 2), (c + fillet, tw / 2 + fillet)), "paint_dk"))
        parts.append((((c, -tw / 2 - fillet), (c + fillet, -tw / 2)), "paint_dk"))
    ends = ([], [])
    for rect, mat in parts:
        cs = cut_corners(o, t, n, b, rect, planes)
        hexa(m, cs, mat)
        for (i, j, k), p in cs.items():
            ends[i].append(p)
    m.solids.append(cut_corners(o, t, n, b, ((-d2, -w2), (d2, w2)), planes))
    return (o, t, n, b, L), ends


def end_plate(m, plane_point, normal, along, pts, margin, thick, bolts, behind=None):
    """A plate in a plane (its point and normal, its face toward +normal in the plane; along: one of its in-plane axes)
    covering the points (a member's cut end) with a margin, thick toward -normal (the support's side). bolts: their
    offsets in the plate's (along, across) axes as fractions of its half-size, their nuts on its face. behind: (margin,
    thickness) of the larger plate behind it on the support."""
    nrm = unit(normal)
    u = np.array(along, float)
    u = unit(u - np.dot(u, nrm) * nrm)
    v = np.cross(nrm, u)
    c0 = np.array(plane_point, float)
    us = [np.dot(p - c0, u) for p in pts]
    vs = [np.dot(p - c0, v) for p in pts]
    u0, u1, v0, v1 = min(us) - margin, max(us) + margin, min(vs) - margin, max(vs) + margin
    obox(m, c0, nrm, u, v, (-thick, u0, v0), (0.0, u1, v1), "plate", solid=True)
    cu, cv = (u0 + u1) / 2, (v0 + v1) / 2
    hu, hv = (u1 - u0) / 2, (v1 - v0) / 2
    for fu, fv in bolts:
        q = c0 + u * (cu + fu * hu) + v * (cv + fv * hv)
        obox(m, q, nrm, u, v, (0.0, -0.035, -0.035), (0.045, 0.035, 0.035), "bolt")
    if behind:
        obox(m, c0, nrm, u, v, (-thick - behind[1], u0 - behind[0], v0 - behind[0]), (-thick, u1 + behind[0], v1 + behind[0]),
             "plate", solid=True)
    return c0 + u * cu + v * cv, u, v, (hu, hv)


def strut(m, foot_cell):
    """One raked strut from the chamfer at its foot cell to the cradle's underside. Returns its frame (for bracing)."""
    dx, dz = DIAG
    wn = unit((dx, 0, dz))
    a = np.array(foot_cell, float)
    wall = a + 0.5                                   # the chamfer's plane: through the foot cell's middle
    foot = wall + wn * 0.12
    top = a + np.array((REACH * dx + 0.5, RISE, REACH * dz + 0.5)) + wn * HEAD_IN
    up = np.cross(np.cross(top - foot, Y), top - foot)
    foot_plane = (wall + wn * FOOT_GAP, wn)
    head_plane = (np.array((0, top[1] - HEAD_GAP, 0)), -Y)
    fr, ends = fitted_ibeam(m, foot, top, up, (foot_plane, head_plane))
    o, t, n, b, L = fr
    # the foot: the end plate on the bracket plate on the chamfer, four bolts, a seat angle under them (its flat leg on
    # the chamfer, its outstanding leg the plates rest on)
    c, u, v, (hu, hv) = end_plate(m, foot_plane[0], wn, Y, ends[0], 0.07, PLATE,
                                  [(-0.75, -0.75), (-0.75, 0.75), (0.75, -0.75), (0.75, 0.75)], behind=(0.08, BRACKET))
    q = c - u * hu
    obox(m, q, wn, u, v, (-FOOT_GAP, -0.16, -hv - 0.08), (-FOOT_GAP + 0.03, 0.0, hv + 0.08), "plate", solid=True)
    obox(m, q, wn, u, v, (-FOOT_GAP, -0.03, -hv - 0.08), (0.05, 0.0, hv + 0.08), "plate", solid=True)
    # the head: the end plate flat on the member's cut end, on the bearing plate under the deck, four bolts up through both
    end_plate(m, np.array((top[0], top[1] - HEAD_GAP, top[2])), -Y, t, ends[1], 0.07, PLATE,
              [(-0.72, -0.72), (-0.72, 0.72), (0.72, -0.72), (0.72, 0.72)], behind=(0.1, BEARING))
    return fr


def brace(m, p0, p1, face_normal):
    """A flat-bar brace lying on the gusset plates' faces (face_normal: out of the bracing plane), from one gusset's
    middle to the other's, its ends square, two bolts through each end into its gusset."""
    o, t, n, b, L = frame(p0, p1, unit(face_normal))
    w, th = BAR
    obox(m, o, t, n, b, (-0.12, 0.0, -w / 2), (L + 0.12, th, w / 2), "paint")
    for e in (0.0, L):
        for dt in (-0.06, 0.06):
            q = o + t * (e + dt)
            obox(m, q, n, t, b, (th, -0.03, -0.03), (th + 0.045, 0.03, 0.03), "bolt")


def bracing(m, frames):
    """Between the two struts: gusset plates on the webs in the bracing plane, a tie at mid-length and an X each bay."""
    (o1, t1, n1, b1, L1), (o2, t2, n2, b2, L2) = frames
    across = unit((o2 - o1) - np.dot(o2 - o1, t1) * t1)
    face = np.cross(t1, across)                # out of the bracing plane
    if face[1] < 0:
        face = -face

    def at(k, f):
        o, t, L = (o1, t1, L1) if k == 0 else (o2, t2, L2)
        side = across if k == 0 else -across
        return o + t * L * f + side * GUSSET_REACH, side, o + t * L * f, t

    for f in GUSSETS:
        for k in (0, 1):
            _, side, axis_p, t = at(k, f)
            obox(m, axis_p, t, side, face, (-0.22, 0.03, -0.02), (0.22, GUSSET_REACH + 0.08, 0.0), "plate")
    for f0, f1 in BRACES:
        brace(m, at(0, f0)[0], at(1, f1)[0], face)


def build():
    _patch[0] = 12345
    m = Mesh()
    frames = [strut(m, f) for f in FEET]
    bracing(m, frames)
    return m


# ------------------------------------------------------------------ the cells it stands in, and their collision

RES = 8                   # collision voxels a block


def _inside(corners, pts):
    """Which of pts (N, 3) lie inside a convex hexahedron."""
    centre = sum(corners.values()) / 8
    faces = [((0, 0, 0), (0, 1, 0), (0, 1, 1)), ((1, 0, 0), (1, 0, 1), (1, 1, 1)), ((0, 0, 0), (0, 0, 1), (1, 0, 1)),
             ((0, 1, 0), (1, 1, 0), (1, 1, 1)), ((0, 0, 0), (1, 0, 0), (1, 1, 0)), ((0, 0, 1), (0, 1, 1), (1, 1, 1))]
    ok = np.ones(len(pts), bool)
    for a, b_, c in faces:
        p0, p1, p2 = corners[a], corners[b_], corners[c]
        n = np.cross(p1 - p0, p2 - p0)
        if np.linalg.norm(n) < 1e-12:
            continue
        if np.dot(n, centre - p0) > 0:
            n = -n
        ok &= (pts - p0) @ n <= 1e-9
    return ok


def footprint(m):
    """[(cell, [boxes in pixels, the cell's frame])], the master's cell first: every cell a colliding solid reaches into
    (voxels of 1/8 block whose middle is inside one), the voxels of each merged into boxes."""
    lo = np.floor(np.min([p for s in m.solids for p in s.values()], 0)).astype(int)
    hi = np.ceil(np.max([p for s in m.solids for p in s.values()], 0)).astype(int)
    dims = (hi - lo) * RES
    g = np.stack(np.meshgrid(*[np.arange(d) for d in dims], indexing="ij"), -1).reshape(-1, 3)
    pts = lo + (g + 0.5) / RES
    occ = np.zeros(len(pts), bool)
    for s in m.solids:
        occ |= _inside(s, pts)
    occ = occ.reshape(tuple(dims))
    cells = {}
    for idx in zip(*np.nonzero(occ)):
        cell = tuple(int(v) for v in (lo + np.array(idx) // RES))
        cells.setdefault(cell, set()).add(tuple(int(v) for v in np.array(idx) % RES))
    out = []
    for cell in sorted(cells, key=lambda c: (c != (0, 0, 0), c[1], c[0], c[2])):
        out.append((cell, _merge(cells[cell])))
    return out


def _merge(vox):
    """Voxels (x, y, z in 0..RES-1) as few boxes: runs along x, then grown along z, then along y."""
    left = set(vox)
    boxes = []
    k = 16 // RES
    for y in range(RES):
        for z in range(RES):
            for x in range(RES):
                if (x, y, z) not in left:
                    continue
                x1 = x
                while (x1 + 1, y, z) in left:
                    x1 += 1
                z1 = z
                while all((i, y, z1 + 1) in left for i in range(x, x1 + 1)):
                    z1 += 1
                y1 = y
                while all((i, y1 + 1, j) in left for i in range(x, x1 + 1) for j in range(z, z1 + 1)):
                    y1 += 1
                for i in range(x, x1 + 1):
                    for j in range(z, z1 + 1):
                        for h in range(y, y1 + 1):
                            left.discard((i, h, j))
                boxes.append([x * k, y * k, z * k, (x1 + 1) * k, (y1 + 1) * k, (z1 + 1) * k])
    return boxes


def cells():
    """The cells the written footprint has (the master's first), offsets facing south."""
    return [tuple(c) for c in json.loads(FOOTPRINT.read_text())["cells"]]


def turn(offset, facing):
    """A canonical (facing south) cell offset turned to `facing` (a quarter clockwise seen from above per turn)."""
    x, y, z = offset
    for _ in range(TURNS[facing]):
        x, z = -z, x
    return x, y, z


def cells_touched(m):
    """Every cell any part of the mesh (bracing included) reaches into (sampled on its quads)."""
    out = set()
    for q in m.quads:
        P_ = q["P"]
        for s in np.linspace(0.01, 0.99, 12):
            for t_ in np.linspace(0.01, 0.99, 12):
                p = (1 - s) * (1 - t_) * P_[0] + s * (1 - t_) * P_[1] + s * t_ * P_[2] + (1 - s) * t_ * P_[3]
                # a point on a cell's face belongs to the cell on the quad's back side (the solid's own)
                p = p - q["n"] * 1e-4
                out.add(tuple(int(math.floor(v)) for v in p))
    return out


MAX_PARTS = 64          # tower/CradleStrutsPartBlock.INDEX's range (0 to MAX_PARTS - 1)


# ------------------------------------------------------------------ the mesh file

def tiles():
    return {"paint": TCC._noise((74, 79, 86), 4, 501, size=64), "paint_dk": TCC._noise((58, 62, 68), 4, 502, size=64),
            "plate": TCC._noise((88, 93, 100), 4, 503, size=64), "bolt": TCC._noise((150, 154, 160), 3, 504, size=16)}




class Atlas:
    """The tiles on one sheet (as tools/holo_projector.Atlas)."""

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


def _uvs(atlas, q):
    """Every quad carries its patch of its tile (tools: face)."""
    return [atlas.uv(q["mat"], u, v) for u, v in q["uvf"]]


def write_mesh(m, atlas, path):
    """The console's mesh format ("JTCM" 1, client/ConsoleMesh): shaded quads only, no live faces, no step edges."""
    path.parent.mkdir(parents=True, exist_ok=True)
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
    for mat in ("plate", "plate", "plate"):
        u0, v0 = atlas.uv(mat, 0.1, 0.1)
        u1, v1 = atlas.uv(mat, 0.9, 0.9)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


def bake(m, reach=0.45, rays=24, strength=0.45):
    """Ambient occlusion at every corner: rays over its hemisphere traced against the mesh's own triangles (exactly: the
    struts' faces are raked, so a voxel grid would shade them as if buried in themselves), with the chamfer and the deck
    the struts meet standing in as occluders. Kept as quad['shade'] as tools/tower_control_console.bake keeps it."""
    dx, dz = DIAG
    wn = unit((dx, 0, dz))
    along = unit((dz, 0, -dx))
    occluders = [q["P"] for q in m.quads]
    for foot in FEET:
        c = np.array(foot, float) + 0.5
        occluders.append(np.array([c + along * 1.5 + Y * 2, c - along * 1.5 + Y * 2, c - along * 1.5 - Y * 2, c + along * 1.5 - Y * 2]))
    occluders.append(np.array([(-7, RISE, -5), (5, RISE, -5), (5, RISE, 9), (-7, RISE, 9)], float))
    tri = []
    for P_ in occluders:
        tri.append((P_[0], P_[1], P_[2]))
        tri.append((P_[0], P_[2], P_[3]))
    V0 = np.array([t[0] for t in tri])
    E1 = np.array([t[1] for t in tri]) - V0
    E2 = np.array([t[2] for t in tri]) - V0
    k = np.arange(rays) + 0.5
    rr = np.sqrt(k / rays)
    th = k * math.pi * (3 - math.sqrt(5))
    local = np.stack([rr * np.cos(th), rr * np.sin(th), np.sqrt(1 - rr * rr)], 1)
    corners = np.concatenate([q["P"] for q in m.quads])
    normals = np.concatenate([np.repeat(q["n"][None], 4, 0) for q in m.quads])
    key = np.concatenate([np.round(corners / 1e-3), np.round(normals * 50)], 1).astype(np.int64)
    uk, inv = np.unique(key, axis=0, return_inverse=True)
    inv = inv.reshape(-1)
    up_p = np.zeros((len(uk), 3))
    up_n = np.zeros((len(uk), 3))
    up_p[inv] = corners
    up_n[inv] = normals
    ao = np.ones(len(uk))
    for i in range(len(uk)):
        p, n = up_p[i], up_n[i]
        a_ = np.array([0, 1.0, 0]) if abs(n[1]) < 0.9 else np.array([1.0, 0, 0])
        u = unit(np.cross(n, a_))
        v = np.cross(n, u)
        D = local[:, 0:1] * u + local[:, 1:2] * v + local[:, 2:3] * n          # (rays, 3)
        O = p + n * 2e-3
        # Moller-Trumbore, every ray against every triangle
        pv = np.cross(D[:, None, :], E2[None])                               # (rays, tris, 3)
        det = np.einsum("rtk,tk->rt", pv, E1)
        ok = np.abs(det) > 1e-12
        inv_det = np.where(ok, 1.0 / np.where(ok, det, 1.0), 0.0)
        tv = O - V0                                                          # (tris, 3)
        uu = np.einsum("rtk,tk->rt", pv, tv) * inv_det
        qv = np.cross(tv, E1)                                                # (tris, 3)
        vv = np.einsum("rk,tk->rt", D, qv) * inv_det
        tt = np.einsum("tk,tk->t", E2, qv)[None, :] * inv_det
        hit = ok & (uu >= 0) & (vv >= 0) & (uu + vv <= 1) & (tt > 1e-3) & (tt < reach)
        dist = np.where(hit, tt, np.inf).min(1)
        w = np.where(np.isfinite(dist), 1 - 0.55 * (dist / reach), 0.0)
        ao[i] = 1 - strength * w.mean()
    ao = np.clip(ao, 0.5, 1.0)
    per = ao[inv].reshape(-1, 4)
    for j, q in enumerate(m.quads):
        q["shade"] = [float(min(1.0, v * 0.88)) for v in per[j]]


# ------------------------------------------------------------------ the blocks' data

def write_data(cells):
    j = TCC._json
    model = {"textures": {"particle": "jugcraft:block/girder_paint"}, "elements": []}
    j(ASSETS / "models/block" / f"{ID}.json", model)
    j(ASSETS / "blockstates" / f"{ID}.json", {"variants": {f"facing={f}": {"model": f"jugcraft:block/{ID}"} for f in TURNS}})
    j(ASSETS / "blockstates" / f"{PART_ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "models/item" / f"{ID}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}})
    j(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    j(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", TCC._loot(ID))
    j(DATA / "jugcraft/loot_table/blocks" / f"{PART_ID}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{PART_ID}"})
    j(DATA / "jugcraft/recipe" / f"{ID}.json", TCC._recipe(["GPG", "PBP", "GPG"], {
        "G": "jugcraft:steel_girder", "P": "#c:plates/steel", "B": "minecraft:iron_bars"}, ID))
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{PART_ID}"] = NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Cradle Struts need the space they rise through clear"
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in (ID, PART_ID):
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")
    j(FOOTPRINT, {"comment": "tools/cradle_struts.py: the cells the struts pass through, the master's first, offsets from it "
                             "facing south; each one's collision boxes in pixels of its own cell",
                  "cells": [list(c) for c, _ in cells], "boxes": [b for _, b in cells]})


def write_all():
    m = build()
    cells = footprint(m)
    if len(cells) - 1 > MAX_PARTS:
        raise ValueError(f"{len(cells) - 1} parts: more than CradleStrutsPartBlock.INDEX holds ({MAX_PARTS})")
    atlas = Atlas(tiles())
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    bake(m)
    write_mesh(m, atlas, MESH)
    write_data(cells)
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return m, cells


if __name__ == "__main__":
    m, cells = write_all()
    pts = np.concatenate([q["P"] for q in m.quads])
    print(len(m.quads), "quads,", len(cells), "cells,", sum(len(b) for _, b in cells), "boxes; bounds",
          pts.min(0).round(2), pts.max(0).round(2))
    print([c for c, _ in cells])
