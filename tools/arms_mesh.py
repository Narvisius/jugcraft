"""The Runebound arms as real 3D meshes (docs/features/arms-vii.md, "Runebound meshes"; the owner, 5 October 2026:
"the runebound weapons need to look much better ... don't even use minecraft esque textures make like a nicer 3d
model and use that to make it really be cool and special").

The Runebound Nodachi, Moonblade, Staff and War Hammer are drawn in the hand as smooth quad meshes instead of the
restyle's boxes (tools/arms_pixel.py): lofted blades ground to a real edge, round grips and shafts turned on a lathe,
swept guards, claws and beaks, and faceted crystals. Normals are given per corner and smoothed within each panel (a
hard edge splits them), so curves shade smoothly and edges stay crisp.

Shape carries the detail, not low-resolution texture (texture boundaries that are not straight would show stair steps
when the arm fills the screen in first person):
- the glow is a raised, rounded bead of light set in the flat (the Moonblade's crescent of glyphs, about 0.7 px wide;
  the Nodachi's short rune panel, as wide as its narrow flat allows) or a glowing edge (the Nodachi's temper), never a
  thin sunk slot, so the Moonblade's bead and the Nodachi's edge read as steady lines at any distance;
- the Nodachi's temper line is a real boundary in the mesh: the edge side glows, a white line runs along the wave;
- the tsuka's diamonds are the mesh's own diamond tiling; the cords spiral because their texture rows follow the
  spiral; glyphs and sigils are drawn in straight strokes only;
- every material is a flat colour or a gentle ramp (runebound_mesh, 128x128), and the glow is one pulsing strip
  (runebound_rune, animated).

Each mesh is built along the weapon's own axis in its design's units, exactly as long as the restyle design it
replaces (tools/arms_variants_art.py) and held at the same grip, then laid on the diagonal by the same 45-degree turn
about the hand, so every hand pose in tools/arms_variants.py holds it as it held the box model. The icon is rendered
from the same mesh (icon()).

In game the model is read by client/MeshItemModels.java (Fabric's model loading and renderer API meshes, already in
fabric-api: no new dependency). The model JSON also keeps the box model as "elements", so if that loader were
missing the arm still loads as before.

Mesh rules (checked by tools/check_mod_data.py, check_mesh_models): every part is closed; where a part goes into
another its end is sunk and, inside, stepped down so the two keep LIFT apart; no two surfaces within PARALLEL degrees
of parallel lie closer than LIFT where they overlap without crossing (parallel_overlaps); quads turn anticlockwise
seen from outside.

Space: points are (t, s, w): t across the weapon (negative towards the light, the icon's top left), s along it from
the butt, w through it (towards the viewer of the icon). All original geometry.
"""
import math

MOD = "jugcraft"
NAMES = ("runebound_nodachi", "runebound_moonblade", "runebound_staff", "runebound_war_hammer")
ATLAS = "runebound_mesh"          # textures/item/runebound_mesh.png: the painted materials
RUNE = "runebound_rune"           # textures/item/runebound_rune.png: the glowing glyphs, a pulsing animated strip
ATLAS_SIZE = 128
RUNE_SIZE = 64
RUNE_FRAMES = 8
RUNE_FRAMETIME = 3                # ticks a frame: a pulse every 1.2 s
ICON = 48                         # icon size (as the long arms' icons)
LIFT = 0.1                        # model pixels: the least gap between two near-parallel surfaces that overlap
PARALLEL = 10.0                   # degrees: surfaces closer to parallel than this keep LIFT apart
MAX_QUADS = 2000


# ---------------------------------------------------------------- vectors


def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def mul(a, k):
    return (a[0] * k, a[1] * k, a[2] * k)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def length(a):
    return math.sqrt(dot(a, a))


def unit(a):
    n = length(a)
    return (0.0, 0.0, 0.0) if n < 1e-12 else (a[0] / n, a[1] / n, a[2] / n)


def lerp(a, b, k):
    return a + (b - a) * k


def smooth(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3 - 2 * x)


def spline(points, x):
    """A Catmull-Rom curve through (x, y) points (x increasing), at x."""
    if x <= points[0][0]:
        return points[0][1]
    if x >= points[-1][0]:
        return points[-1][1]
    k = max(i for i in range(len(points) - 1) if points[i][0] <= x)
    p0 = points[max(0, k - 1)]
    p1, p2 = points[k], points[k + 1]
    p3 = points[min(len(points) - 1, k + 2)]
    u = (x - p1[0]) / (p2[0] - p1[0])
    m1 = (p2[1] - p0[1]) / (p2[0] - p0[0]) * (p2[0] - p1[0])
    m2 = (p3[1] - p1[1]) / (p3[0] - p1[0]) * (p2[0] - p1[0])
    u2, u3 = u * u, u * u * u
    return (2 * u3 - 3 * u2 + 1) * p1[1] + (u3 - 2 * u2 + u) * m1 + (-2 * u3 + 3 * u2) * p2[1] + (u3 - u2) * m2


# ---------------------------------------------------------------- materials (regions of the painted atlas)

class Mat:
    """A material: its sprite slot ("mesh" or "rune"), its region of that sprite in pixels, whether it glows, and its
    icon ramp (five colours, shadow to highlight: icons are drawn in these flat tones)."""
    ALL = []

    def __init__(self, slot, region, glow=False, size=ATLAS_SIZE, icon=None, register=True):
        self.slot = slot
        self.region = region
        self.glow = glow
        self.size = size
        self.icon = icon
        if register:
            Mat.ALL.append(self)

    def uv(self, a, b):
        """The sprite's (u, v), 0 to 1, of the point (a, b) of this region (each 0 to 1), half a pixel inside its
        edges so a neighbour never bleeds in."""
        x0, y0, x1, y1 = self.region
        x = x0 + 0.5 + (x1 - x0 - 1.0) * min(1.0, max(0.0, a))
        y = y0 + 0.5 + (y1 - y0 - 1.0) * min(1.0, max(0.0, b))
        return (x / self.size, y / self.size)


# The icon ramps, shadow to highlight: lighter than the model's own colours, so the icons sit beside the steel arms.
GLOW_RAMP = ((40, 150, 200), (70, 196, 240), (104, 226, 255), (170, 244, 255), (236, 255, 255))
# The atlas, 128x128 (atlas() paints it): each region a flat colour or a gentle ramp along b.
STEEL = Mat("mesh", (0, 0, 32, 32), icon=((84, 78, 128), (116, 108, 166), (146, 140, 196), (180, 176, 222),
                                          (222, 222, 246)))                # moon steel flats: violet silver
EDGE = Mat("mesh", (34, 0, 50, 32), icon=((150, 152, 196), (186, 188, 224), (214, 216, 240), (236, 238, 252),
                                          (255, 255, 255)))                # the ground edge, lightest at it (b = 1)
SPINE = Mat("mesh", (52, 0, 68, 32), icon=((64, 58, 98), (86, 80, 126), (110, 104, 154), (136, 130, 180),
                                           (170, 166, 210)))               # the back of a blade, a shade darker
IRON = Mat("mesh", (70, 0, 86, 32), icon=((48, 42, 74), (68, 62, 100), (92, 86, 128), (120, 114, 158),
                                          (160, 156, 196)))                # dark violet iron: fittings, claws
SILVER = Mat("mesh", (88, 0, 104, 32), icon=((130, 134, 170), (170, 174, 206), (204, 208, 232), (228, 232, 248),
                                             (255, 255, 255)))             # moon silver: the moonblade's bezel
GILT = Mat("mesh", (106, 0, 122, 32), icon=((136, 106, 58), (178, 148, 88), (212, 186, 120), (238, 218, 160),
                                            (255, 244, 206)))              # pale moon gold: habaki, hammer band
CORD = Mat("mesh", (0, 34, 32, 66), icon=((58, 32, 92), (86, 54, 130), (114, 76, 166), (140, 102, 194),
                                          (170, 136, 220)))                # spiral cords (rows follow the spiral)
LEATHER = Mat("mesh", (34, 34, 50, 66), icon=((66, 38, 100), (88, 54, 126), (110, 72, 154), (134, 94, 180),
                                              (160, 122, 204)))            # violet leather, seams along it
WOOD = Mat("mesh", (52, 34, 84, 66), icon=((56, 40, 56), (76, 56, 76), (98, 76, 96), (118, 94, 116),
                                           (140, 116, 138)))               # dark ironwood, grain along it
SAME = Mat("mesh", (86, 34, 102, 66), icon=((150, 142, 176), (180, 172, 204), (206, 198, 228), (226, 220, 242),
                                            (244, 240, 252)))              # the tsuka's pale ray skin diamonds
ITO = Mat("mesh", (104, 34, 120, 66), icon=((56, 30, 90), (82, 50, 126), (110, 72, 162), (136, 98, 190),
                                            (166, 132, 216)))              # the tsuka's violet silk cord
CRYSTAL = Mat("mesh", (0, 68, 16, 100), glow=True, icon=((40, 150, 200), (80, 204, 244), (120, 230, 255),
                                                         (186, 248, 255), (240, 255, 255)))
CRYSTAL_PALE = Mat("mesh", (18, 68, 34, 100), glow=True, icon=((60, 170, 214), (110, 220, 250), (160, 240, 255),
                                                               (210, 252, 255), (245, 255, 255)))
HEAD = Mat("mesh", (36, 68, 52, 100), icon=((92, 86, 134), (126, 120, 170), (158, 152, 202), (190, 186, 228),
                                            (226, 224, 248)))              # the hammer's head: moon steel
GROUND = Mat("mesh", (54, 68, 70, 100), icon=((130, 128, 176), (160, 158, 204), (188, 186, 226), (214, 212, 242),
                                              (240, 240, 255)))            # the nodachi's ground steel over its temper
# The glyph strip (rune_frames()), 64x64 frames: 2 rows of 4 glyphs (16x16 each), and under them a banded ring, the
# sigil's stave, a plain glow and a white core.
GLYPH_ROWS, GLYPH_COLS, GLYPH_CELL = 2, 4, 16
BAND = Mat("rune", (0, 32, 32, 40), glow=True, size=RUNE_SIZE, icon=GLOW_RAMP)
SIGIL = Mat("rune", (32, 32, 64, 48), glow=True, size=RUNE_SIZE, icon=GLOW_RAMP)
GLOW = Mat("rune", (0, 40, 32, 64), glow=True, size=RUNE_SIZE, icon=GLOW_RAMP)
CORE = Mat("rune", (32, 48, 64, 64), glow=True, size=RUNE_SIZE, icon=((200, 246, 255), (220, 250, 255),
                                                                      (236, 254, 255), (246, 255, 255),
                                                                      (255, 255, 255)))


def glyph(index):
    """The glyph cell `index` (0 to 7) of the rune strip."""
    index %= GLYPH_ROWS * GLYPH_COLS
    x0, y0 = GLYPH_CELL * (index % GLYPH_COLS), GLYPH_CELL * (index // GLYPH_COLS)
    return Mat("rune", (x0, y0, x0 + GLYPH_CELL, y0 + GLYPH_CELL), glow=True, size=RUNE_SIZE, icon=GLOW_RAMP,
               register=False)


# ---------------------------------------------------------------- the mesh


class Quad:
    __slots__ = ("mat", "points", "uvs", "normals")

    def __init__(self, mat, points, uvs, normals):
        self.mat = mat
        self.points = points
        self.uvs = uvs
        self.normals = normals


def face_normal(points):
    """A quad's area-weighted normal, from its diagonals (as Minecraft and Fabric's renderer take it)."""
    return cross(sub(points[2], points[0]), sub(points[3], points[1]))


class Mesh:
    def __init__(self):
        self.quads = []

    def add(self, mat, points, uvs, normals):
        """A quad, its corners turned anticlockwise as seen from the side its normals face; skipped if it has no area."""
        fn = face_normal(points)
        if length(fn) < 1e-7:
            return
        if dot(fn, add(add(normals[0], normals[1]), add(normals[2], normals[3]))) < 0:
            points, uvs, normals = points[::-1], uvs[::-1], normals[::-1]
        self.quads.append(Quad(mat, list(points), list(uvs), [unit(n) for n in normals]))


def surface(mesh, P, mat, closed=True, hard_j=(), hard_i=(), uv=None, faceted_j=()):
    """Quads over a grid of points P[i][j] (i along, j round; j a closed loop when `closed`), with per-corner normals
    smoothed over the quads round each grid point, except across a hard point j (a crease running along) or a hard
    ring i (a crease running round): there each side keeps its own normals. The quads of a column in `faceted_j`
    never share normals across rings (a recess cut in a few flat facets).

    `mat` is a Mat or a function (i, j) -> Mat; `uv(i, j, di, dj)` gives the (a, b) in its region of corner (i + di,
    j + dj) of quad (i, j) (default: a along, b round). The normals face away from the rings' centres."""
    I, J = len(P), len(P[0])
    nj = J if closed else J - 1
    hard_j, hard_i, faceted_j = set(hard_j), set(hard_i), set(faceted_j)
    F = {}
    for i in range(I - 1):
        for j in range(nj):
            F[i, j] = face_normal((P[i][j], P[i + 1][j], P[i + 1][(j + 1) % J], P[i][(j + 1) % J]))
    # Which way is out: away from the ring's centre, summed over the whole surface.
    score = 0.0
    for (i, j), n in F.items():
        centre = mul(add(add(P[i][j], P[i + 1][j]), add(P[i + 1][(j + 1) % J], P[i][(j + 1) % J])), 0.25)
        ring = P[i] + P[i + 1]
        ref = mul(tuple(sum(p[k] for p in ring) for k in range(3)), 1.0 / len(ring))
        score += dot(n, sub(centre, ref))
    sign = -1.0 if score < 0 else 1.0
    for key in F:
        F[key] = mul(F[key], sign)

    def normal_at(i, j, gi, gj):
        total = (0.0, 0.0, 0.0)
        for qi in (gi - 1, gi):
            if qi < 0 or qi >= I - 1:
                continue
            if qi != i and (gi in hard_i or j in faceted_j):
                continue
            for qj in (gj - 1, gj):
                if closed:
                    qj %= J
                elif qj < 0 or qj >= nj:
                    continue
                if qj != j and gj % J in hard_j:
                    continue
                total = add(total, F[qi, qj])
        n = unit(total)
        return n if length(n) > 0.5 else unit(F[i, j])

    for i in range(I - 1):
        for j in range(nj):
            m = mat(i, j) if callable(mat) else mat
            corners = ((0, 0), (1, 0), (1, 1), (0, 1))
            pts = [P[i + di][(j + dj) % J] for di, dj in corners]
            if uv is None:
                abs_ = [((i + di) / (I - 1), (j + dj) / nj) for di, dj in corners]
            else:
                abs_ = [uv(i, j, di, dj) for di, dj in corners]
            uvs = [m.uv(a, b) for a, b in abs_]
            ns = [normal_at(i, j, i + di, j + dj) for di, dj in corners]
            mesh.add(m, pts, uvs, ns)


def cap(mesh, ring, mat, outward):
    """A flat cap over a ring of an even number of points: quads from its centre over each pair of edges."""
    n = len(ring)
    centre = mul(tuple(sum(p[k] for p in ring) for k in range(3)), 1.0 / n)
    normal = unit(outward)
    # Planar UVs: two axes in the cap's plane.
    a_axis = unit(sub(ring[0], centre))
    b_axis = unit(cross(normal, a_axis))
    reach = max(length(sub(p, centre)) for p in ring) or 1.0

    def uvof(p):
        d = sub(p, centre)
        return mat.uv(0.5 + 0.5 * dot(d, a_axis) / reach, 0.5 + 0.5 * dot(d, b_axis) / reach)
    for j in range(0, n, 2):
        pts = [centre, ring[j], ring[(j + 1) % n], ring[(j + 2) % n]]
        mesh.add(mat, pts, [uvof(p) for p in pts], [normal] * 4)


# ---------------------------------------------------------------- shapes

AXIS = ((0.0, 1.0, 0.0), (1.0, 0.0, 0.0), (0.0, 0.0, 1.0))    # along s; round it, t then w


def ring_points(origin, frame, r_a, r_b, seg, phase=0.0):
    """A ring of `seg` points round `origin` in the plane of frame[1], frame[2], radii r_a and r_b along them."""
    _axis, e1, e2 = frame
    out = []
    for k in range(seg):
        a = 2 * math.pi * (k + phase) / seg
        out.append(add(origin, add(mul(e1, r_a * math.cos(a)), mul(e2, r_b * math.sin(a)))))
    return out


def lathe(mesh, profile, mat, seg=16, origin=(0.0, 0.0, 0.0), frame=AXIS, hard=(), caps=(True, True), phase=0.0,
          uv=None, flat_round=False):
    """A turned part round frame[0] through `origin`: profile [(s, r)] or [(s, r_a, r_b)] (an oval), each end capped.
    `hard`: profile points that are creases; `flat_round`: facets instead of a round (every j hard)."""
    axis = frame[0]
    P = []
    for entry in profile:
        s, ra = entry[0], entry[1]
        rb = entry[2] if len(entry) > 2 else ra
        P.append(ring_points(add(origin, mul(axis, s)), frame, ra, rb, seg, phase))
    surface(mesh, P, mat, closed=True, hard_i=hard, hard_j=range(seg) if flat_round else (), uv=uv)
    caps_mat = mat if not callable(mat) else mat(0, 0)
    if caps[0]:
        cap(mesh, P[0], caps_mat, mul(axis, -1.0 if profile[1][0] > profile[0][0] else 1.0))
    if caps[1]:
        cap(mesh, P[-1], caps_mat, mul(axis, 1.0 if profile[-1][0] > profile[-2][0] else -1.0))
    return P


def sunk(profile, inner, inner_lo=True, inner_hi=True, depth=0.4):
    """A lathe profile whose ends go into fittings: the profile's first and last rings are the fittings' mouths; a
    ring `depth` (or (lower, upper)) further in, stepped down to `inner` times the radius, ends it inside, so the part
    never runs parallel to the fitting round it (and never closer than LIFT to it)."""
    out = list(profile)
    lo_d, hi_d = depth if isinstance(depth, tuple) else (depth, depth)

    def stepped(entry, s):
        return (s,) + tuple(r * inner for r in entry[1:])
    if inner_lo:
        out.insert(0, stepped(profile[0], profile[0][0] - lo_d))
    if inner_hi:
        out.append(stepped(profile[-1], profile[-1][0] + hi_d))
    return out


def helix_uv(profile, seg, pitch, cords):
    """(a, b) for a lathe wrapped in `cords` cords winding once round every `pitch` design units: b follows the spiral
    (the CORD texture's rows hold two turns of it), so the cords' edges are smooth spirals, not stair steps."""
    def phase(i, j):
        return cords * (j / seg + profile[i][0] / pitch)

    def uv(i, j, di, dj):
        base = math.floor(phase(i, j))
        return ((j + dj) / seg, (phase(i + di, j + dj) - base) / 2.0)
    return uv


def frames_along(path, up):
    """Parallel-transport frames (tangent, n1, n2) along a polyline path, starting with n1 as near `up` as can be."""
    out = []
    n1 = None
    for k in range(len(path)):
        a = path[max(0, k - 1)]
        b = path[min(len(path) - 1, k + 1)]
        tangent = unit(sub(b, a))
        if n1 is None:
            n1 = unit(sub(up, mul(tangent, dot(up, tangent))))
        else:
            n1 = unit(sub(n1, mul(tangent, dot(n1, tangent))))
        n2 = cross(tangent, n1)
        out.append((tangent, n1, n2))
    return out


def sweep(mesh, path, radius, mat, seg=10, up=(0.0, 0.0, 1.0), caps=(True, True), squash=1.0, uv=None, frames=None,
          phase=0.0):
    """A tube along `path`, radius(k) at its k-th point (an oval `squash` times as wide along n2), each end capped.
    `frames`: (tangent, n1, n2) at each point instead of parallel transport from `up`."""
    frames = frames or frames_along(path, up)
    P = []
    for k, c in enumerate(path):
        r = radius(k)
        P.append(ring_points(c, frames[k], r, r * squash, seg, phase))
    surface(mesh, P, mat, closed=True, uv=uv)
    m0 = mat if not callable(mat) else mat(0, 0)
    if caps[0]:
        cap(mesh, P[0], m0, mul(frames[0][0], -1.0))
    if caps[1]:
        cap(mesh, P[-1], m0, frames[-1][0])
    return P


def crystal(mesh, origin, frame, length_, radius, seg=6, waist=0.55, tip=0.12, phase=0.5):
    """A faceted crystal along frame[0]: a `seg`-sided prism `waist` of its length long, pointed at both ends to a
    small flat tip (`tip` of the radius), every facet flat, alternate facets a paler cyan."""
    half = length_ / 2.0
    w = waist * half
    profile = [(-half, radius * tip), (-w, radius), (w, radius), (half, radius * tip)]
    lathe(mesh, profile, lambda i, j: CRYSTAL_PALE if j % 2 else CRYSTAL, seg=seg, origin=origin, frame=frame,
          hard=range(4), flat_round=True, phase=phase, uv=lambda i, j, di, dj: (di, dj))


def frame_toward(axis, hint=(0.0, 0.0, 1.0)):
    """A frame whose first vector is `axis`."""
    a = unit(axis)
    h = hint if abs(dot(unit(hint), a)) < 0.95 else (1.0, 0.0, 0.0)
    e1 = unit(sub(h, mul(a, dot(h, a))))
    e2 = cross(a, e1)
    return (a, e1, e2)


def lattice(mesh, ss, radius, seg, mat, oval=1.0, phase=0.0, ends=None):
    """A turned part tiled in diamonds (a tsuka's wrap): rings at `ss`, every other one turned half a facet, each
    quad a diamond from a ring's point below to its two neighbours on the next ring to the point above; `mat(i, k)`
    colours the diamond centred on ring i between its points k and k + 1, so the pattern's edges are the mesh's own.
    The zigzag ends are closed by a skirt to a flat ring at s = ends[0] (ends[1]) and capped (bury them in fittings).
    `radius(s)`; `oval`: the radius through w."""
    rings, norms = [], []
    for i, s in enumerate(ss):
        r = radius(s)
        dr = (radius(s + 1e-3) - radius(s - 1e-3)) / 2e-3
        pts, ns = [], []
        for k in range(seg):
            a = 2 * math.pi * (k + 0.5 * (i % 2) + phase) / seg
            pts.append((r * math.cos(a), s, r * oval * math.sin(a)))
            ns.append(unit((math.cos(a), -dr, math.sin(a) / oval)))
        rings.append(pts)
        norms.append(ns)
    uvs = ((0.5, 0.0), (1.0, 0.5), (0.5, 1.0), (0.0, 0.5))
    for i in range(1, len(ss) - 1):
        for k in range(seg):
            m = mat(i, k)
            top = k if i % 2 == 0 else k + 1
            corners = [(i - 1, top % seg), (i, (k + 1) % seg), (i + 1, top % seg), (i, k)]
            mesh.add(m, [rings[a][b] for a, b in corners], [m.uv(*x) for x in uvs], [norms[a][b] for a, b in corners])
    # The ends: the zigzag through the first two (and last two) rings, joined by a skirt to a flat ring `end` beyond
    # them, which is capped.
    for i, outward, end in ((1, (0.0, -1.0, 0.0), ends[0]), (len(ss) - 2, (0.0, 1.0, 0.0), ends[1])):
        j = i - 1 if outward[1] < 0 else i + 1
        loop, flat = [], []
        r = radius(end)
        for k in range(seg):
            for ring, idx, off in ((i, k, 0.5 * (i % 2)), (j, (k if i % 2 == 0 else k + 1) % seg, 0.5 * (j % 2))):
                loop.append(rings[ring][idx])
                a = math.atan2(rings[ring][idx][2] / oval, rings[ring][idx][0])
                flat.append((r * math.cos(a), end, r * oval * math.sin(a)))
        m = mat(i, 0)
        surface(mesh, [loop, flat], m, closed=True, uv=lambda i_, j_, di, dj: (di, 0.5))
        cap(mesh, flat, m, outward)


# ---------------------------------------------------------------- blades

GROOVE_MIN = 0.03   # design units: a bead's width where it has run out (a sliver of the flat, so no quad is a triangle)
NIOI = 0.07         # the white line along the temper: its width, as a share of the bevel


def on_bevel(te, tb, hf, f):
    """The point a share f of the way from the edge (te) to the top of the bevel (tb), on the bevel's two faces."""
    t = lerp(te, tb, f)
    w = hf * 0.62 * f / 0.55 if f <= 0.55 else lerp(hf * 0.62, hf, (f - 0.55) / 0.45)
    return (t, w)


def blade(mesh, ss, spine, edge, half_spine, half_flat, bevel, bead=None, hamon=None, glyph_seed=0, glyph_len=2,
          spine_round=0.35, edge_mat=EDGE, flat_mat=STEEL, spine_mat=SPINE, tip_mat=None):
    """A single-edged blade lofted through the sections at `ss`: at s its back is at t = spine(s) and its edge at
    t = edge(s); the back is half_spine(s) thick either side of w = 0, the flats half_flat(s), ground down over the
    last bevel(s) of the width to the edge.

    `bead(s)` -> (where, width, rise) sets a rounded bead of light into both flats, `where` (0 to 1) of the way from
    the bevel to the back; where it is near its full width its top shows glyphs (`glyph_len` sections a glyph), where
    it tapers it is plain light, and where it has run out it is a sliver of the flat. `hamon(s)` -> the share of the
    bevel, from the edge, that is tempered: that part glows, a white line runs along it, and the rest of the bevel is
    ground steel. Returns the sections (rings of points)."""
    sides, beads = [], []
    for s in ss:
        te, ts = edge(s), spine(s)
        sign = 1.0 if te >= ts else -1.0
        width = abs(te - ts)
        hs, hf = half_spine(s), half_flat(s)
        bw = min(bevel(s), width * 0.6)
        tb = te - sign * bw
        r = min(spine_round, width * 0.18)
        side = [(te, 0.0)]
        if hamon:
            f = hamon(s)
            side += [on_bevel(te, tb, hf, f), on_bevel(te, tb, hf, f + NIOI)]
        side += [(lerp(te, tb, 0.55), hf * 0.62), (tb, hf)]
        # The flat, from the bevel (A) to the back's shoulder (B), and the bead set in it.
        A, B = (tb, hf), (ts + sign * r, hs)
        d = (B[0] - A[0], B[1] - A[1])
        L = math.hypot(*d) or 1e-6
        d = (d[0] / L, d[1] / L)
        n = (-d[1], d[0]) if d[0] * sign < 0 else (d[1], -d[0])
        if n[1] < 0:
            n = (-n[0], -n[1])
        where, g_w, rise = bead(s) if bead else (0.5, 0.0, 0.0)
        hw = max(GROOVE_MIN / 2.0, min(g_w / 2.0, 0.45 * (L - 0.08)))
        x_c = min(max(where * L, 0.04 + hw), L - 0.04 - hw) if L > 0.08 + 2 * hw else L / 2.0
        for x, h in ((-1.0, 0.0), (-0.8, 0.6), (-0.35, 0.94), (0.35, 0.94), (0.8, 0.6), (1.0, 0.0)):
            side.append((A[0] + d[0] * (x_c + x * hw) + n[0] * h * rise, A[1] + d[1] * (x_c + x * hw) + n[1] * h * rise))
        side += [B, (ts + sign * r * 0.3, hs * 0.75)]
        sides.append((side, ts))
        beads.append((hw, rise))
    count = len(sides[0][0])                   # points a side; the loop is the edge, one side, the back, the other
    P = []
    for k, (side, ts) in enumerate(sides):
        s = ss[k]
        P.append([(t, s, w) for t, w in side] + [(ts, s, 0.0)] + [(t, s, -w) for t, w in side[1:]][::-1])
    J = len(P[0])
    first_bead = 6 if hamon else 4             # index of the bead's first lip
    panels = (["temper", "nioi", "ground", "ground"] if hamon else ["edge", "edge"]) + ["flat"] + ["bead"] * 5 + \
        ["flat", "spine", "spine"]
    hard = {0, first_bead - 1, first_bead, first_bead + 5, first_bead + 6}   # edge, bevel top, lips, shoulder
    hard_j = hard | {(J - h) % J for h in hard}
    widest = max(hw for hw, _rise in beads) or 1.0

    def lit(i):
        return beads[i][1] > 0.02

    def full(i):
        return beads[i][0] > 0.6 * widest and lit(i)
    cells = {}
    run = 0
    for i in range(len(ss) - 1):
        if full(i) and full(i + 1):
            cells[i] = run
            run += 1
        else:
            run = 0 if not (full(i) or full(i + 1)) else run

    def side_index(p):
        return p if p < count else (None if p == count else J - p)

    def mat(i, j):
        p = panels[j if j < count else J - 1 - j]
        if p == "bead":
            if i in cells:
                return glyph(glyph_seed + cells[i] // glyph_len)
            return GLOW if lit(i) or lit(i + 1) else flat_mat
        return {"edge": edge_mat, "flat": flat_mat, "spine": spine_mat, "temper": GLOW, "nioi": CORE,
                "ground": GROUND}[p]
    bead_b = (0.0, 0.12, 0.33, 0.67, 0.88, 1.0)

    def uvf(i, j, di, dj):
        p = panels[j if j < count else J - 1 - j]
        if p == "bead" and i in cells:
            k = side_index((j + dj) % J) - first_bead
            return (((cells[i] % glyph_len) + di) / glyph_len, bead_b[k])
        a = (ss[i + di] - ss[0]) / (ss[-1] - ss[0])
        q = P[i + di][(j + dj) % J]
        te, ts = edge(ss[i + di]), spine(ss[i + di])
        b = 0.5 if abs(te - ts) < 1e-6 else (q[0] - ts) / (te - ts)
        return (a, b)
    surface(mesh, P, mat, closed=True, hard_j=hard_j, uv=uvf)
    cap(mesh, P[0], flat_mat, (0.0, -1.0, 0.0))
    cap(mesh, P[-1], tip_mat or edge_mat, (0.0, 1.0, 0.0))
    return P


# ---------------------------------------------------------------- the four arms


def lathe_uv(seg, period=None, rings=None):
    """(a, b) for lathe quads: a along (repeating every `period` rings, or across all `rings`), b round."""
    def uv(i, j, di, dj):
        a = ((i % period) + di) / period if period else (i + di) / max(1, rings - 1)
        return (a, (j + dj) / seg)
    return uv


def runebound_moonblade():
    """A crescent moonblade of moon steel: a ground edge along its belly and a raised bead of glowing glyphs following
    it, a crescent-moon guard with glowing horn tips round a heart crystal in a silver bezel, a cord-wrapped grip
    and a faceted crystal pommel in an iron cup. Design units as tools/arms_variants_art.py (50 long, held at 7)."""
    m = Mesh()
    # The pommel: a crystal sitting in an iron cup.
    crystal(m, (0.0, 1.1, 0.0), AXIS, 2.2, 0.9, seg=6, waist=0.5)
    lathe(m, [(1.6, 0.98), (1.8, 1.2), (2.4, 1.22), (2.7, 1.06), (2.85, 1.0)], IRON, seg=14, hard=(0, 4))
    # The grip: a cord wrap between two iron ferrules, its ends stepped down inside them.
    prof = sunk([(3.6 + 6.95 * k / 15.0, 0.98 + 0.08 * math.sin(math.pi * k / 15.0)) for k in range(16)], 0.74,
                depth=(0.4, 0.3))
    lathe(m, prof, CORD, seg=14, uv=helix_uv(prof, 14, 4.2, 4))
    for s0, s1, r in ((2.55, 3.6, 1.18), (10.55, 11.6, 1.2)):  # half a facet round from the grip's facets
        lathe(m, [(s0, r - 0.08), (s0 + 0.08, r), (s1 - 0.08, r), (s1, r - 0.08)], IRON, seg=14, hard=(1, 2), phase=0.5)
    # The guard: a crescent moon, horns curling up towards the blade, thickest in the middle.
    n = 25
    path, rad = [], []
    for k in range(n):
        u = -1.0 + 2.0 * k / (n - 1)
        a = u * 1.25
        path.append((4.7 * math.sin(a) / math.sin(1.25), 12.9 + 2.4 * (1 - math.cos(a)) / (1 - math.cos(1.25)), 0.0))
        rad.append(0.16 + 0.98 * (1 - abs(u) ** 1.7))
    # Turned so a ridge, not a facet, runs along its underside, where the ferrule's top goes into it.
    sweep(m, path, lambda k: rad[k], IRON, seg=10, up=(0.0, 0.0, 1.0), squash=1.4, phase=0.5,
          uv=lambda i, j, di, dj: ((i + di) / (n - 1), (j + dj) / 10))
    for end in (0, n - 1):
        tip = path[end]
        out = unit(sub(path[end], path[end - 1 if end else 1]))
        crystal(m, add(tip, mul(out, 0.35)), frame_toward(out), 1.4, 0.36, seg=6, waist=0.3)
    # The heart: a crystal through the guard in a silver bezel.
    w_axis = ((0.0, 0.0, 1.0), (1.0, 0.0, 0.0), (0.0, 1.0, 0.0))
    lathe(m, [(-1.48, 0.9), (-1.38, 1.0), (1.38, 1.0), (1.48, 0.9)], SILVER, seg=16, origin=(0.0, 12.9, 0.0),
          frame=w_axis, hard=(1, 2))
    crystal(m, (0.0, 12.9, 0.0), w_axis, 3.6, 0.72, seg=8, waist=0.35, tip=0.3)
    # The blade, its bead of glyphs a third of the way in from its edge's bevel.
    spine_pts = [(13.0, -1.5), (13.6, -1.6), (22.0, -2.2), (30.0, -2.4), (38.0, -2.25), (44.0, -1.8), (47.5, -1.0),
                 (49.2, -0.35), (50.0, -0.02)]
    edge_pts = [(13.0, 1.55), (13.6, 1.8), (19.0, 4.6), (28.0, 6.3), (38.0, 5.4), (44.5, 2.9), (47.5, 1.3),
                (49.2, 0.4), (50.0, 0.02)]

    def spine(s):
        return spline(spine_pts, s)

    def edge(s):
        return spline(edge_pts, s)

    def thick(s):
        x = (s - 13.0) / 37.0
        return lerp(0.62, 0.17, x ** 1.15)

    def bead(s):
        k = smooth((s - 15.0) / 2.6) * smooth((44.6 - s) / 3.0)
        return (0.32, 1.8 * k, 0.32 * k)
    blade(m, [13.0 + 37.0 * k / 40 for k in range(41)], spine, edge, thick, lambda s: thick(s) * 0.86,
          lambda s: min(1.9, 0.3 * (edge(s) - spine(s))), bead=bead, glyph_seed=0, glyph_len=2)
    return m


def runebound_nodachi():
    """A long curved nodachi of moon steel: its edge tempered in a glowing wave with a white line along it, a short
    panel of glowing glyphs in the flat by the habaki, a gold habaki, an oval tsuba round whose rim runs a ring of
    light, and a long diamond-wrapped tsuka between an iron kashira and fuchi. 56 units long, held at 9."""
    m = Mesh()
    oval = 0.8                                     # the tsuka and its fittings are oval: thinner through w
    lathe(m, [(0.0, 0.8, 0.8 * oval), (0.12, 1.08, 1.08 * oval), (0.7, 1.2, 1.2 * oval), (1.6, 1.16, 1.16 * oval)],
          IRON, seg=16, hard=(1,))
    # The tsuka: diamonds of ray skin between silk cords, its ends stepped down inside the kashira and the fuchi.
    main = [1.6 + 13.4 * k / 24 for k in range(25)]
    ss = [0.85, 1.2] + main + [15.35, 15.6]

    def radius(s):
        if s < 1.6 - 1e-6 or s > 15.0 + 1e-6:
            return 0.76
        return 1.06 + 0.05 * math.sin(math.pi * (s - 1.6) / 13.4)
    # Windows of ray skin on every other ring, in four columns (the flats and the edges), between crossing cords.
    lattice(m, ss, radius, 8, lambda i, k: SAME if i % 2 == 0 and k % 2 == 1 and 1 < i < len(ss) - 2 else ITO,
            oval=oval, phase=0.5, ends=(0.6, 15.75))
    lathe(m, [(15.0, 1.12, 1.12 * oval), (15.08, 1.22, 1.22 * oval), (16.55, 1.22, 1.22 * oval)], IRON, seg=16,
          hard=(1,))
    # The tsuba: an oval plate, a glowing ring sunk round its rim.
    t_prof = [(16.2, 2.84), (16.28, 3.0), (16.5, 3.0), (16.52, 2.86), (16.88, 2.86), (16.9, 3.0), (17.12, 3.0),
              (17.2, 2.84)]

    def tsuba_mat(i, j):
        return BAND if i == 3 else IRON

    def tsuba_uv(i, j, di, dj):
        if i == 3:
            return (((j % 4) + dj) / 4.0, di)
        return ((i + di) / 7.0, (j + dj) / 24.0)
    lathe(m, [(s_, r, r * 0.86) for s_, r in t_prof], tsuba_mat, seg=24, hard=range(8), uv=tsuba_uv)
    # The habaki, a gold collar round the blade's root.
    lathe(m, [(16.85, 1.94, 0.9), (17.1, 2.04, 1.02), (18.7, 2.0, 1.0), (18.9, 1.9, 0.9)], GILT, seg=16,
          hard=(1, 2), origin=(-0.02, 0.0, 0.0))

    def bend(s):
        return 0.0016 * (s - 18.8) ** 2 if s > 18.8 else 0.0
    tip_s = 56.0
    tip_t = -0.6 - bend(52.5) * 1.3

    def spine(s):
        if s <= 52.5:
            return -bend(s) - 1.45 + 0.15 * (s - 18.6) / 33.9
        return spline([(52.5, -bend(52.5) - 1.3), (54.2, -bend(54.2) - 1.22), (tip_s, tip_t - 0.02)], s)

    def edge(s):
        if s <= 52.5:
            return -bend(s) + 1.5 - 0.2 * (s - 18.6) / 33.9
        x = (s - 52.5) / (tip_s - 52.5)
        e0 = -bend(52.5) + 1.3
        return lerp(e0, tip_t + 0.02, x ** 1.8)

    def thick(s):
        return lerp(0.5, 0.34, (s - 18.6) / 37.4) * (1.0 if s < 53.5 else max(0.56, 1 - (s - 53.5) / 2.5))

    def hamon(s):
        # A gentle, uneven wave (notare), rising to a steady line round the point (the boshi).
        x = s - 18.6
        f = 0.34 + 0.1 * math.sin(2 * math.pi * x / 5.4 + 0.6) * (0.75 + 0.25 * math.sin(2 * math.pi * x / 17.0))
        return lerp(f, 0.42, smooth((s - 51.0) / 2.0))

    def bead(s):
        k = smooth((s - 20.0) / 1.0) * smooth((28.6 - s) / 1.0)
        return (0.5, 0.82 * k, 0.32 * k)
    at = [18.5] + [18.6 + 0.9 * k for k in range(1, 38)] + [53.0, 53.6, 54.2, 54.8, 55.4, 55.95]
    blade(m, at, spine, edge, lambda s: thick(s) * 0.86, thick, lambda s: 0.6 * (edge(s) - spine(s)), bead=bead,
          hamon=hamon, glyph_seed=3, glyph_len=1, spine_round=0.2, tip_mat=GROUND)
    return m


def staff_end(m, top):
    """An end of the staff: an iron ferrule whose three claws hold a floating crystal (top: the upper end)."""
    sign = 1.0 if top else -1.0
    base = 50.0 if top else 0.0

    def at(d):
        return base - sign * d          # d: how far in from the end
    prof = [(at(4.6), 0.98), (at(4.45), 1.14), (at(3.4), 1.12), (at(3.1), 1.26), (at(2.75), 1.26), (at(2.6), 1.06)]
    if not top:
        prof = prof[::-1]
    lathe(m, prof, IRON, seg=16, hard=range(len(prof)))
    crystal(m, (0.0, at(1.1), 0.0), AXIS, 2.3, 0.66, seg=6, waist=0.4)
    for k in range(3):
        a = 2 * math.pi * (k / 3.0 + 1 / 12.0)
        out = (math.cos(a), 0.0, math.sin(a))
        path = []
        for q in range(7):
            u = q / 6.0
            r = 1.02 + 0.42 * math.sin(math.pi * min(1.0, u * 1.15)) - 0.38 * u * u
            path.append(add(mul(out, r), (0.0, at(2.85 - 2.75 * u), 0.0)))
        # Tapering to a blunt tip still over 0.1 px across, so its two sides never lie closer than that.
        sweep(m, path, lambda q: 0.28 * (1 - q / 6.0) + 0.14, IRON, seg=8, up=(0.0, 1.0, 0.0))


def runebound_staff():
    """A quarterstaff of dark ironwood: a glowing helix of runes winding up each half, a violet leather grip between
    iron collars, and at each end an iron ferrule whose three claws hold a floating crystal. 50 long, held at 25."""
    m = Mesh()

    def wood_r(s):
        return 0.88 + 0.06 * math.sin(math.pi * (s - 2.6) / 44.8)
    # Each half runs from a ferrule's mouth to a collar's, its ends stepped down inside them.
    for s0, s1, h0, h1 in ((4.6, 19.2, 4.9, 18.35), (30.8, 45.4, 31.65, 45.1)):
        n = 18
        prof = sunk([(s0 + (s1 - s0) * k / (n - 1), wood_r(s0 + (s1 - s0) * k / (n - 1))) for k in range(n)], 0.7,
                    depth=0.36)
        lathe(m, prof, WOOD, seg=14, uv=lathe_uv(14, rings=len(prof)))
        # The helix: two turns of glowing light, half sunk into the wood, thinning to its ends, where it dives into
        # the wood (its capped ends lie inside it), short of the ferrule and the collar.
        path, rad, radial = [], [], []
        turns, steps = 2.0, 36
        for q in range(steps + 1):
            u = q / steps
            s = lerp(h0, h1, u)
            a = 2 * math.pi * turns * u + (0.0 if s0 < 20 else math.pi)
            dive = smooth(min(q, steps - q) / 3.0)
            r = wood_r(s) + 0.03 - 0.5 * (1.0 - dive)
            path.append((r * math.cos(a), s, r * math.sin(a)))
            radial.append((math.cos(a), 0.0, math.sin(a)))
            rad.append(0.16 + 0.1 * dive)                 # never so thin that its sides come within 0.1 px
        # Its six facets turned so a ridge, not a facet, runs along the wood: none lies parallel to the wood's skin.
        frames = []
        for q, (tangent, _n1, _n2) in enumerate(frames_along(path, (0.0, 1.0, 0.0))):
            n2 = unit(sub(radial[q], mul(tangent, dot(radial[q], tangent))))
            frames.append((tangent, cross(n2, tangent), n2))
        sweep(m, path, lambda q: rad[q], GLOW, seg=6, frames=frames, phase=0.5)
    n = 12
    prof = sunk([(20.2 + 9.6 * k / (n - 1), 1.02 + 0.03 * math.sin(math.pi * k / (n - 1))) for k in range(n)], 0.74,
                depth=0.3)
    lathe(m, prof, LEATHER, seg=14, uv=lathe_uv(14, rings=len(prof)))
    for s0, s1 in ((19.2, 20.2), (29.8, 30.8)):       # half a facet round from the grip's facets
        lathe(m, [(s0, 1.1), (s0 + 0.1, 1.24), (s1 - 0.1, 1.24), (s1, 1.1)], IRON, seg=14, hard=(1, 2), phase=0.5)
    staff_end(m, True)
    staff_end(m, False)
    return m


def head_ring(t, s0, half_s, half_w, chamfer, recess, boss=(0.0, 0.0)):
    """A ring of the war hammer's head across t: an octagon in (s, w) round s0, its corners cut by `chamfer`, with a
    recess (half-length along s, depth) sunk in the middle of each broad face (w = +-half_w), and in the recess's floor
    a boss (half-length, height above the floor); 24 points."""
    d, depth = recess
    d = max(d, GROOVE_MIN)
    inset = min(0.12, d * 0.4)
    d2, rise = boss
    d2 = min(max(d2, GROOVE_MIN / 2.0), d - inset - GROOVE_MIN / 2.0)
    inset2 = min(0.1, d2 * 0.4)
    hs, hw, c = half_s, half_w, chamfer
    face = [(s0 - d, hw), (s0 - d + inset, hw - depth), (s0 - d2, hw - depth), (s0 - d2 + inset2, hw - depth + rise),
            (s0 + d2 - inset2, hw - depth + rise), (s0 + d2, hw - depth), (s0 + d - inset, hw - depth), (s0 + d, hw)]
    pts = [(s0 + hs, hw - c), (s0 + hs - c, hw)]
    pts += face[::-1]
    pts += [(s0 - hs + c, hw), (s0 - hs, hw - c), (s0 - hs, -hw + c), (s0 - hs + c, -hw)]
    pts += [(s_, -w) for s_, w in face]
    pts += [(s0 + hs - c, -hw), (s0 + hs, -hw + c)]
    return [(t, s_, w) for s_, w in pts]


def runebound_war_hammer():
    """A war hammer: a chamfered octagonal head of moon steel with a glowing sigil sunk in each side and a band of
    moon gold, a curved beak tapering behind it and a spike above, iron langets down an iron-banded ironwood haft, a
    cord-wrapped grip and an iron pommel. 38 long, held at 6."""
    m = Mesh()
    # The pommel, with a collar round its neck that the grip comes out of.
    lathe(m, [(-0.2, 0.55), (0.0, 0.95), (0.5, 1.28), (1.05, 1.22), (1.4, 1.02), (1.5, 1.24), (1.95, 1.3),
              (2.05, 1.2)], IRON, seg=14, hard=(0, 4, 5, 6))
    # The grip: a cord wrap from the pommel's collar to an iron band, its ends stepped down inside them.
    n = 16
    prof = sunk([(2.05 + 7.55 * k / (n - 1), 0.96 + 0.04 * math.sin(math.pi * k / (n - 1))) for k in range(n)], 0.72,
                depth=(0.32, 0.3))
    lathe(m, prof, CORD, seg=14, uv=helix_uv(prof, 14, 3.6, 4))
    # The haft, from inside that band up into the head, and the bands round it.
    prof = sunk([(10.6 + 15.4 * k / 15, 0.82) for k in range(16)], 0.72, inner_hi=False, depth=0.25)
    lathe(m, prof, WOOD, seg=12, uv=lathe_uv(12, rings=len(prof)))
    for s0, s1, r in ((9.6, 10.6, 1.24), (15.6, 16.4, 1.06), (19.6, 20.2, 1.06)):
        # Turned half a facet against the haft, so no facet of a band lies parallel to one of the haft's.
        lathe(m, [(s0, r - 0.08), (s0 + 0.08, r), (s1 - 0.08, r), (s1, r - 0.08)], IRON, seg=12, hard=(1, 2), phase=0.5)
    # The socket, octagonal, with a lip, and the langets down the haft.
    # The socket goes up into the head and steps in there, clear of the beak's shoulder.
    lathe(m, [(24.6, 1.14), (24.7, 1.32), (25.4, 1.32), (25.5, 1.16), (27.45, 1.16), (27.9, 0.86)], IRON, seg=8,
          hard=range(6), flat_round=True, phase=0.5)
    for side in (1.0, -1.0):
        P = []
        for k in range(7):
            s = lerp(20.8, 24.6, k / 5.0) if k < 6 else 24.9
            half = lerp(0.12, 0.4, smooth(k / 5.0)) if k < 6 else 0.4
            w0, w1 = 0.55, (1.06 if k < 6 else 0.8)      # the top stepped in inside the socket
            P.append([(-half, s, side * w0), (half, s, side * w0), (half, s, side * w1), (-half, s, side * w1)])
        surface(m, P, IRON, closed=True, hard_j=range(4))
        cap(m, P[0], IRON, (0.0, -1.0, 0.0))
        cap(m, P[-1], IRON, (0.0, 1.0, 0.0))
    # The head and its beak, one piece lofted across t: flared towards its face, a sigil sunk in each side.
    s0, c = 30.4, 0.72

    def half_s(t):
        return 2.72 + 0.42 * smooth((t - 2.4) / 3.4) + 0.2 * smooth((0.6 - t) / 1.6)

    def half_w(t):
        return 1.74 + 0.28 * smooth((t - 2.4) / 3.4) + 0.14 * smooth((0.6 - t) / 1.6)

    def sigil(t):
        k = max(0.0, 1.0 - abs(t - 3.3) / 1.7)
        return (1.55 * k, min(0.3, 1.3 * k))

    def boss(t):
        # A white diamond raised in the recess's floor, half its size.
        k = max(0.0, 1.0 - abs(t - 3.3) / 0.94)
        return (0.86 * k, min(0.18, 0.9 * k))
    rings, hard_i = [], [0, 1]
    for t in (6.55, 6.1):
        sc = 0.86 if t > 6.3 else 1.0
        rings.append(head_ring(t, s0, half_s(6.1) * sc, half_w(6.1) * sc, c * sc, (0.0, 0.0)))
    for t in (5.6, 5.0, 4.6, 4.2, 3.8, 3.3, 2.8, 2.4, 2.0, 1.6, 1.0, 0.3, -0.4, -1.0):
        rings.append(head_ring(t, s0, half_s(t), half_w(t), c, sigil(t), boss(t)))
    hard_i.append(len(rings) - 1)
    beak = 7.2
    for k in range(10):
        u = k / 9.0
        t = -1.25 - (beak - 1.25) * u
        drop = 1.6 * u ** 1.6
        size = lerp(1.0, 0.04, u ** 0.9)
        rings.append(head_ring(t, s0 - drop, 1.45 * size, 1.05 * size, 0.5 * size, (0.0, 0.0)))
    hard_i.append(len(rings) - 10)
    # The recess: every quad between two rings either of which is sunk, the taper to its corners too.
    sunk_ring = [sigil(ring[0][0])[1] > 1e-6 for ring in rings]
    lit = {i for i in range(len(rings) - 1) if sunk_ring[i] or sunk_ring[i + 1]}
    J = len(rings[0])
    floors = (3, 7, 15, 19)

    def mat(i, j):
        if i not in lit or j not in range(2, 9) and j not in range(14, 21):
            return HEAD
        if j in floors:
            return SIGIL                     # the floor, a stave through it
        if j in (5, 17):
            return CORE                      # the boss's face
        return GLOW                          # the recess's and the boss's walls: no dark slit where they narrow

    def uvf(i, j, di, dj):
        q = rings[i + di][(j + dj) % J]
        if j in floors and i in lit:
            return ((q[0] - 1.6) / 3.4, (q[1] - (s0 - 1.7)) / 3.4)
        return ((q[0] + 7.2) / 13.8, (j + dj) / J)
    surface(m, rings, mat, closed=True, hard_i=hard_i, hard_j=range(J), uv=uvf,
            faceted_j=list(range(2, 9)) + list(range(14, 21)))
    cap(m, rings[0], HEAD, (1.0, 0.0, 0.0))
    cap(m, rings[-1], HEAD, (-1.0, 0.0, 0.0))
    # The spike above.
    P = []
    for k in range(7):
        u = k / 6.0
        s = lerp(32.6, 37.6, u)
        r = lerp(0.95, 0.05, u ** 0.85)
        P.append([(r, s, 0.0), (0.0, s, r * 0.9), (-r, s, 0.0), (0.0, s, -r * 0.9)])
    surface(m, P, HEAD, closed=True, hard_j=range(4))
    cap(m, P[0], HEAD, (0.0, -1.0, 0.0))
    cap(m, P[-1], HEAD, (0.0, 1.0, 0.0))
    # A band of moon gold round the head behind its face, standing proud of it, its ends sunk into the head.
    P = []
    for t, grow in ((5.08, -0.22), (5.2, 0.24), (5.6, 0.24), (5.72, -0.22)):
        hs_, hw_ = half_s(t), half_w(t)
        P.append(head_ring(t, s0, hs_ + grow, hw_ + grow, c + grow * 0.4, (0.0, 0.0)))
    surface(m, P, GILT, closed=True, hard_i=(1, 2), hard_j=range(len(P[0])))
    cap(m, P[0], GILT, (-1.0, 0.0, 0.0))
    cap(m, P[-1], GILT, (1.0, 0.0, 0.0))
    return m


DESIGNS = {"runebound_nodachi": runebound_nodachi, "runebound_moonblade": runebound_moonblade,
           "runebound_staff": runebound_staff, "runebound_war_hammer": runebound_war_hammer}


def design(name):
    return DESIGNS[name]()


# ---------------------------------------------------------------- the textures


def _ramp(c0, c1, k):
    return tuple(int(round(lerp(a, b, min(1.0, max(0.0, k))))) for a, b in zip(c0, c1)) + (255,)


def atlas():
    """The painted materials, 128x128: flat fills and gentle ramps along b, patterns as straight stripes only."""
    from PIL import Image
    img = Image.new("RGBA", (ATLAS_SIZE, ATLAS_SIZE), (0, 0, 0, 0))
    px = img.load()

    def fill(mat, colour):
        x0, y0, x1, y1 = mat.region
        for y in range(y0, y1):
            for x in range(x0, x1):
                a, b = (x - x0 + 0.5) / (x1 - x0), (y - y0 + 0.5) / (y1 - y0)
                px[x, y] = colour(a, b) if callable(colour) else colour + (255,)
    # Moon steel: violet silver, a shade lighter towards the edge.
    fill(STEEL, lambda a, b: _ramp((98, 90, 146), (130, 122, 178), b))
    fill(EDGE, lambda a, b: _ramp((170, 172, 212), (242, 244, 255), (b - 0.6) / 0.4))
    fill(SPINE, lambda a, b: _ramp((70, 64, 106), (96, 90, 136), b))
    fill(IRON, (66, 60, 94))
    fill(SILVER, (204, 208, 232))
    fill(GILT, (214, 188, 122))

    def cord(a, b):
        # Rows hold two turns of a cord (helix_uv): a dark gap, then the cord, lit along its middle.
        q = (b * 2.0 * (CORD.region[3] - CORD.region[1]) / (CORD.region[3] - CORD.region[1] - 1.0)) % 1.0
        if q < 0.14:
            return (48, 28, 74, 255)
        if 0.36 < q < 0.74:
            return (134, 96, 186, 255)
        return (104, 68, 152, 255)
    fill(CORD, cord)
    fill(LEATHER, lambda a, b: (98, 62, 140, 255) if int(b * 16) % 8 else (78, 46, 116, 255))

    def wood(a, b):
        y = int(b * 32)
        if y % 8 == 3:
            return (66, 48, 66, 255)
        if y % 8 == 6:
            return (100, 78, 98, 255)
        return (84, 64, 84, 255)
    fill(WOOD, wood)
    fill(SAME, (204, 196, 226))
    fill(ITO, (108, 68, 158))
    fill(CRYSTAL, (110, 226, 255))
    fill(CRYSTAL_PALE, (196, 248, 255))
    fill(HEAD, (118, 112, 160))
    fill(GROUND, (176, 174, 214))
    # The gaps between regions take their neighbours' colours, so the sprite is solid and no see-through edge bleeds
    # into a smaller mipmap.
    while True:
        empty = [(x, y) for y in range(ATLAS_SIZE) for x in range(ATLAS_SIZE) if px[x, y][3] == 0]
        if not empty:
            break
        grown = {}
        for x, y in empty:
            for dx, dy in ((-1, 0), (0, -1), (1, 0), (0, 1)):
                if 0 <= x + dx < ATLAS_SIZE and 0 <= y + dy < ATLAS_SIZE and px[x + dx, y + dy][3] == 255:
                    grown[x, y] = px[x + dx, y + dy]
                    break
        for (x, y), colour in grown.items():
            px[x, y] = colour
    return img


# The glyphs: original stave runes on a 16x16 cell, in straight one-texel strokes only (x along the bead, y across
# it), so their edges stay straight lines however large the arm is drawn: a stave across the bead, branches along it.
GLYPHS = [
    [(8, 3, 8, 12), (8, 4, 12, 4), (4, 9, 8, 9)],
    [(8, 3, 8, 12), (8, 4, 12, 4), (8, 8, 12, 8)],
    [(8, 3, 8, 12), (4, 4, 8, 4), (8, 11, 12, 11)],
    [(8, 3, 8, 12), (8, 5, 12, 5), (12, 5, 12, 9), (8, 9, 12, 9)],
    [(8, 3, 8, 12), (4, 7, 12, 7), (8, 11, 12, 11)],
    [(8, 3, 8, 12), (4, 4, 8, 4), (4, 8, 8, 8), (8, 12, 12, 12)],
    [(6, 3, 6, 12), (10, 3, 10, 12), (6, 7, 10, 7)],
    [(8, 3, 8, 12), (8, 3, 12, 3), (12, 3, 12, 6), (4, 10, 8, 10)],
]


def rune_frames():
    """The glowing strip: RUNE_FRAMES frames of 64x64 (eight glyphs, a banded ring, the hammer's stave, a plain glow
    and a white core): thin white strokes on clean cyan light, so from afar a bead reads as one glowing line and up
    close as runes. It pulses from bright to brighter (Minecraft blends between the frames: "interpolate")."""
    from PIL import Image, ImageDraw
    frames = []
    for f in range(RUNE_FRAMES):
        k = 0.5 - 0.5 * math.cos(2 * math.pi * f / RUNE_FRAMES)       # 0 .. 1 .. 0
        light = _ramp((76, 206, 244), (124, 236, 255), k)
        edge = _ramp((52, 168, 214), (84, 204, 244), k)
        core = _ramp((222, 252, 255), (255, 255, 255), k)
        img = Image.new("RGBA", (RUNE_SIZE, RUNE_SIZE), light)
        draw = ImageDraw.Draw(img)
        n = GLYPH_CELL
        for index, strokes in enumerate(GLYPHS):
            x0, y0 = n * (index % GLYPH_COLS), n * (index // GLYPH_COLS)
            draw.line((x0, y0, x0 + n - 1, y0), fill=edge)          # the bead's two flanks, a shade deeper
            draw.line((x0, y0 + n - 1, x0 + n - 1, y0 + n - 1), fill=edge)
            for a0, b0, a1, b1 in strokes:
                draw.line((x0 + a0, y0 + b0, x0 + a1, y0 + b1), fill=core)
        x0, y0, x1, _y1 = BAND.region
        for k, colour in enumerate((edge, light, light, core, core, light, light, edge)):
            draw.line((x0, y0 + k, x1 - 1, y0 + k), fill=colour)
        # The sigil's floor (the Runecarver's Pattern's emblem: a diamond pierced by a stave; the war hammer's
        # recess and its raised boss are the diamonds): the stave.
        x0, y0, x1, y1 = SIGIL.region
        draw.rectangle(((x0 + x1) // 2 - 1, y0, (x0 + x1) // 2, y1 - 1), fill=core)
        draw.rectangle(CORE.region[:2] + (CORE.region[2] - 1, CORE.region[3] - 1), fill=core)
        frames.append(img)
    return frames


def rune_strip():
    from PIL import Image
    frames = rune_frames()
    strip = Image.new("RGBA", (RUNE_SIZE, RUNE_SIZE * len(frames)))
    for k, f in enumerate(frames):
        strip.paste(f, (0, RUNE_SIZE * k))
    return strip


def rune_animation():
    return {"frametime": RUNE_FRAMETIME, "interpolate": True}


# ---------------------------------------------------------------- into the model


C45 = math.sqrt(0.5)


def stable(x, digits):
    """`x` rounded to `digits`, the same on every machine: rounded to ten places first, so a value a hair either side of
    a halfway point (0.65625 computed as 0.656250000000001 here and 0.65625 in CI) rounds the same way. Plain round() let
    the committed models differ from CI's."""
    return round(round(x, 10), digits) + 0.0


def placer(grip, grip_model, unit_):
    """Design (t, s, w) -> model pixels: `unit_` pixels a design unit, the hand (s = grip, t = 0) at grip_model, the
    weapon turned 45 degrees about the hand to lie along the diagonal (as tools/arms_pixel.py model_elements turns
    its boxes), so the pose that held the box model holds this one."""
    gx, gy = grip_model

    def point(p):
        x, y = p[0] * unit_, (p[1] - grip) * unit_
        return (gx + (x + y) * C45, gy + (y - x) * C45, 8.0 + p[2] * unit_)

    def normal(n):
        return (stable((n[0] + n[1]) * C45, 2), stable((n[1] - n[0]) * C45, 2), stable(n[2], 2))
    return point, normal


def export(mesh, grip, grip_model, unit_):
    """The quads as the model JSON's "quads": {"t": slot, "e": 1 if it glows, "v": four corners of
    [x, y, z (model pixels), u, v (0 to 1 of the sprite), nx, ny, nz]}, rounded so the output is stable (positions to
    a thousandth of a pixel, normals to a hundredth: Minecraft packs normals into bytes)."""
    point, normal = placer(grip, grip_model, unit_)
    out = []
    for q in mesh.quads:
        v = []
        for p, (u, vv), n in zip(q.points, q.uvs, q.normals):
            x, y, z = point(p)
            v.append([stable(x, 3), stable(y, 3), stable(z, 3), stable(u, 4), stable(vv, 4), *normal(n)])
        entry = {"t": q.mat.slot, "v": v}
        if q.mat.glow:
            entry["e"] = 1
        out.append(entry)
    return out


def placement(name):
    """(the design's grip, the hand in model pixels, model pixels a design unit) of a Runebound arm: those of the
    restyle design it replaces (tools/arms_variants_art.py layout and model), so the hand poses hold it as they held
    the box model."""
    import arms
    import arms_variants
    import arms_variants_art
    held = arms.KINDS[arms_variants.kind(name)]["held"]
    size, grip_px, scale, _factor = arms_variants_art.layout(name, held)
    unit_ = scale * math.sqrt(2.0) * 16.0 / size
    grip_model = (grip_px[0] * 16.0 / size, 16.0 - grip_px[1] * 16.0 / size)
    return arms_variants_art.design(name).grip, grip_model, unit_


def held_model(name, box):
    """The arm's in-hand model: its mesh, read by client/MeshItemModels.java, with the box model `box` (as
    tools/arms_variants.py held_model makes it: textures, elements, display) kept as "elements" for a game without
    that loader ("optional"). The display (hand poses) is the box model's."""
    grip, grip_model, unit_ = placement(name)
    textures = dict(box["textures"])
    textures.update({"particle": f"{MOD}:item/{ATLAS}", "mesh": f"{MOD}:item/{ATLAS}", "rune": f"{MOD}:item/{RUNE}"})
    return {"fabric:type": {"id": f"{MOD}:mesh", "optional": True}, "textures": textures, "display": box["display"],
            "elements": box["elements"], "quads": export(design(name), grip, grip_model, unit_)}


def draw(name):
    """The arm's icon, rendered from its mesh."""
    grip, grip_model, unit_ = placement(name)
    return icon(design(name), grip, grip_model, unit_, fatten=ICON_FATTEN[name])


def compact(path):
    """Rewrites a model JSON that the shared writer (tools/generate_material_data.py write: the box elements'
    post-processing, indent 2) has written, one quad a line: the content is unchanged, the file ten times smaller."""
    import json
    model = json.loads(path.read_text(encoding="utf-8"))
    quads = model.pop("quads")
    text = json.dumps(model, separators=(",", ":"))[:-1]
    text += ',"quads":[\n' + ",\n".join(json.dumps(q, separators=(",", ":")) for q in quads) + "\n]}\n"
    path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------- an offline rasterizer (icons and previews)


def rasterize(quads, textures, width, height, shade, background=None, persp=None, cull=False):
    """Draws quads [(slot, glow, [(x, y, depth), ...4] in pixels (bigger depth nearer), [(u, v)]*4, [normal]*4)] with a
    depth buffer and nearest texels; `shade(normals (n, 3), glow) -> brightness (n,)`. `persp`: per-quad lists of four
    clip-space w for perspective-correct texturing; `cull`: skip triangles turned away (their corners clockwise on
    screen, y down). Returns (rgb float array, coverage bool array)."""
    import numpy as np
    rgb = np.zeros((height, width, 3), np.float32)
    if background is not None:
        rgb[:] = background
    depth = np.full((height, width), -np.inf, np.float32)
    tex = {k: np.asarray(v.convert("RGBA"), np.float32) for k, v in textures.items()}
    for qi, (slot, glow, pts, uvs, ns) in enumerate(quads):
        img = tex[slot]
        th, tw = img.shape[:2]
        P = np.array(pts, np.float64)
        UV = np.array(uvs, np.float64)
        N = np.array(ns, np.float64)
        W = np.array(persp[qi], np.float64) if persp is not None else np.ones(4)
        for tri in ((0, 1, 2), (0, 2, 3)):
            x, y, z = P[list(tri), 0], P[list(tri), 1], P[list(tri), 2]
            den = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2])
            if abs(den) < 1e-12 or (cull and den > 0):
                continue
            x0, x1 = int(max(0, math.floor(x.min()))), int(min(width - 1, math.ceil(x.max())))
            y0, y1 = int(max(0, math.floor(y.min()))), int(min(height - 1, math.ceil(y.max())))
            if x1 < x0 or y1 < y0:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            a = ((y[1] - y[2]) * (gx - x[2]) + (x[2] - x[1]) * (gy - y[2])) / den
            b = ((y[2] - y[0]) * (gx - x[2]) + (x[0] - x[2]) * (gy - y[2])) / den
            c = 1.0 - a - b
            inside = (a >= -1e-9) & (b >= -1e-9) & (c >= -1e-9)
            if not inside.any():
                continue
            zz = a * z[0] + b * z[1] + c * z[2]
            sub_d = depth[y0:y1 + 1, x0:x1 + 1]
            hit = inside & (zz > sub_d)
            if not hit.any():
                continue
            iw = 1.0 / W[list(tri)]
            pa, pb, pc = a * iw[0], b * iw[1], c * iw[2]
            norm = pa + pb + pc
            pa, pb, pc = pa / norm, pb / norm, pc / norm
            uv = pa[..., None] * UV[tri[0]] + pb[..., None] * UV[tri[1]] + pc[..., None] * UV[tri[2]]
            nn = pa[..., None] * N[tri[0]] + pb[..., None] * N[tri[1]] + pc[..., None] * N[tri[2]]
            nn /= np.linalg.norm(nn, axis=-1, keepdims=True) + 1e-12
            ui = np.clip((uv[..., 0] * tw).astype(int), 0, tw - 1)
            vi = np.clip((uv[..., 1] * th).astype(int), 0, th - 1)
            texel = img[vi, ui]
            hit &= texel[..., 3] > 0
            k = shade(nn[hit], glow)
            sub_rgb = rgb[y0:y1 + 1, x0:x1 + 1]
            sub_rgb[hit] = texel[..., :3][hit] * k[:, None]
            sub_d[hit] = zz[hit]
    return rgb, np.isfinite(depth)


# ---------------------------------------------------------------- checks


CROSSING_REACH = 0.5     # model pixels: two surfaces whose planes meet this near where they come close are crossing
CROSSING_ANGLE = 2.0     # degrees: surfaces closer to parallel than this run side by side even where they cross


def parallel_overlaps(quads, gap=LIFT, max_angle=PARALLEL, min_area=1e-3):
    """Pairs of quads (lists of four (x, y, z) in model pixels) that could flicker: each is split into the two
    triangles it is drawn as, and two triangles of different quads are flagged when they are within `max_angle`
    degrees of parallel (either facing), overlap seen along the first's normal, and lie closer than `gap` pixels over
    more than `min_area` square pixels of that overlap, unless they cross at CROSSING_ANGLE or more apart: there, or
    within CROSSING_REACH of it, as a part sunk into another does. Two surfaces that cross only meet along a line,
    which does not flicker; two that run side by side closer than `gap`, or cross at a smaller angle (side by side
    over a wide band), do. Every pair is compared. Returns sorted (quad, quad) pairs."""
    import numpy as np
    tris, owner = [], []
    for qi, q in enumerate(quads):
        for t in ((0, 1, 2), (0, 2, 3)):
            T = [tuple(map(float, q[k])) for k in t]
            if length(cross(sub(T[1], T[0]), sub(T[2], T[0]))) > 1e-9:
                tris.append(T)
                owner.append(qi)
    if len(tris) < 2:
        return []
    P = np.array(tris, np.float64)
    N = np.cross(P[:, 1] - P[:, 0], P[:, 2] - P[:, 0])
    N /= np.linalg.norm(N, axis=1, keepdims=True)
    lo, hi = P.min(axis=1), P.max(axis=1)
    cos = math.cos(math.radians(max_angle))
    owner = np.array(owner)
    out = set()
    for a in range(len(P) - 1):
        rest = slice(a + 1, None)
        cand = (np.abs(N[rest] @ N[a]) >= cos) & (owner[rest] != owner[a])
        cand &= np.all((lo[rest] <= hi[a] + gap) & (lo[a] <= hi[rest] + gap), axis=1)
        for b in np.nonzero(cand)[0] + a + 1:
            if _close_area(P[a], N[a], P[b], N[b], gap) > min_area:
                out.add((int(min(owner[a], owner[b])), int(max(owner[a], owner[b]))))
    return sorted(out)


def _close_area(A, nA, B, nB, gap):
    """The area of the overlap of triangles A and B (seen along A's normal) where they lie closer than `gap`; 0 where
    they cross at CROSSING_ANGLE or more."""
    import numpy as np
    e1 = A[1] - A[0]
    e1 = e1 / np.linalg.norm(e1)
    e2 = np.cross(nA, e1)

    def flat(T):
        pts = [(float((p - A[0]) @ e1), float((p - A[0]) @ e2)) for p in T]
        return pts if _area(pts) >= 0 else pts[::-1]
    overlap = flat(B)
    a_pts = flat(A)
    for m_ in range(3):
        overlap = _clip(overlap, a_pts[m_], a_pts[(m_ + 1) % 3])
        if not overlap:
            return 0.0
    if _area(overlap) < 1e-7:
        return 0.0
    c = float(nB @ nA)
    # The distance from A's plane to B's along A's normal, at (u, v) of A's plane: l0 + lu * u + lv * v.
    l0 = float(nB @ (B[0] - A[0])) / c
    lu, lv = -float(nB @ e1) / c, -float(nB @ e2) / c
    dist = [l0 + lu * u + lv * v for u, v in overlap]
    slope = math.hypot(lu, lv)
    if slope >= math.tan(math.radians(CROSSING_ANGLE)):
        if min(dist) < -1e-4 and max(dist) > 1e-4:
            return 0.0
        if min(abs(x) for x in dist) / slope < CROSSING_REACH:
            return 0.0
    for sign in (1.0, -1.0):         # keep where -gap < distance < gap
        k0, ku, kv = gap - sign * l0, -sign * lu, -sign * lv
        overlap = _clip_linear(overlap, k0, ku, kv)
        if not overlap:
            return 0.0
    return abs(_area(overlap))


def _clip_linear(poly, k0, ku, kv):
    """The part of a polygon where k0 + ku * u + kv * v >= 0."""
    out = []
    for k in range(len(poly)):
        p, q = poly[k], poly[(k + 1) % len(poly)]
        fp, fq = k0 + ku * p[0] + kv * p[1], k0 + ku * q[0] + kv * q[1]
        if fp >= 0:
            out.append(p)
        if (fp >= 0) != (fq >= 0):
            t = fp / (fp - fq)
            out.append((p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t))
    return out


def open_edges(quads):
    """Edges (of quads given as four (x, y, z), as exported) that only one quad has: a part that is not closed shows
    them as holes. Returns [(point, point)]."""
    from collections import Counter
    count, where = Counter(), {}
    for q in quads:
        pts = [tuple(round(float(x), 3) for x in p) for p in q]
        for k in range(4):
            a, b = pts[k], pts[(k + 1) % 4]
            if a == b:
                continue
            key = (min(a, b), max(a, b))
            count[key] += 1
            where[key] = (a, b)
    return [where[k] for k, n in count.items() if n % 2]


def _area(poly):
    return 0.5 * sum(poly[k][0] * poly[(k + 1) % len(poly)][1] - poly[(k + 1) % len(poly)][0] * poly[k][1]
                     for k in range(len(poly)))


def _clip(poly, a, b):
    """The part of a polygon left of the edge a -> b (Sutherland-Hodgman)."""
    out = []
    for k in range(len(poly)):
        p, q = poly[k], poly[(k + 1) % len(poly)]
        side_p = (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
        side_q = (b[0] - a[0]) * (q[1] - a[1]) - (b[1] - a[1]) * (q[0] - a[0])
        if side_p >= 0:
            out.append(p)
        if (side_p >= 0) != (side_q >= 0):
            k_ = side_p / (side_p - side_q)
            out.append((p[0] + (q[0] - p[0]) * k_, p[1] + (q[1] - p[1]) * k_))
    return out


# ---------------------------------------------------------------- the icon

ICON_LIGHT = unit((-0.62, 0.66, 0.42))
ICON_OUTLINE = (22, 14, 38)
ICON_GLOW_OUTLINE = (24, 110, 150)
ICON_FATTEN = {"runebound_nodachi": 1.3, "runebound_moonblade": 1.0, "runebound_staff": 1.15,
               "runebound_war_hammer": 1.12}


def icon(mesh, grip, grip_model, unit_, size=ICON, roll=0.0, tilt=35.0, fatten=1.0, supersample=6):
    """The inventory icon, rendered from the mesh: the arm on the diagonal as it lies in the hand (rolled `roll`
    degrees about its length, its point tipped `tilt` degrees away from the viewer), `fatten` times as thick across
    so a thin arm reads at 48 pixels (the owner: icons "maybe not even accurate but cooler"). Each pixel takes the
    material and light band that most of its subsamples show, and is painted that material's flat icon tone for the
    band: lit from the top left in four bands and a highlight, glowing parts always light. A one-pixel outline turns
    cyan beside a glowing part; every pixel solid or clear."""
    import numpy as np
    from PIL import Image
    point, normal = placer(grip, grip_model, unit_)

    def fat(p):
        return (p[0] * fatten, p[1], p[2] * fatten)

    def fat_n(n):
        return unit((n[0] / fatten, n[1], n[2] / fatten))
    axis = np.array([C45, C45, 0.0])
    a = math.radians(roll)
    K = np.array([[0, -axis[2], axis[1]], [axis[2], 0, -axis[0]], [-axis[1], axis[0], 0]])
    R = np.eye(3) + math.sin(a) * K + (1 - math.cos(a)) * (K @ K)
    across = np.array([C45, -C45, 0.0])
    b = math.radians(tilt)
    K = np.array([[0, -across[2], across[1]], [across[2], 0, -across[0]], [-across[1], across[0], 0]])
    R = (np.eye(3) + math.sin(b) * K + (1 - math.cos(b)) * (K @ K)) @ R
    pts = np.array([point(fat(p)) for q in mesh.quads for p in q.points]) @ R.T
    lo, hi = pts.min(0), pts.max(0)
    big = size * supersample
    span = max(hi[0] - lo[0], hi[1] - lo[1])
    scale = (size - 2.6) * supersample / span
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    # Each material gets an id; the id sprites are painted with it, so a render with no shading reads it back.
    mats = [m for m in Mat.ALL if m.icon]
    ids = {id(m): k + 1 for k, m in enumerate(mats)}
    id_mesh = Image.new("RGBA", (ATLAS_SIZE, ATLAS_SIZE), (0, 0, 0, 255))
    id_rune = Image.new("RGBA", (RUNE_SIZE, RUNE_SIZE), (ids[id(GLOW)], 0, 0, 255))
    for m in mats:
        (id_mesh if m.slot == "mesh" else id_rune).paste((ids[id(m)], 0, 0, 255), m.region)
    for m in (BAND, SIGIL, GLOW):
        id_rune.paste((ids[id(GLOW)], 0, 0, 255), m.region)
    white = {"mesh": Image.new("RGBA", (1, 1), (255, 255, 255, 255)), "rune": Image.new("RGBA", (1, 1),
                                                                                          (255, 255, 255, 255))}
    quads, flat_uv = [], []
    for q in mesh.quads:
        P = np.array([point(fat(p)) for p in q.points]) @ R.T
        sp = [(big / 2 + (p[0] - cx) * scale, big / 2 - (p[1] - cy) * scale, p[2]) for p in P]
        ns = [tuple(R @ np.array(normal(fat_n(n)))) for n in q.normals]
        quads.append((q.mat.slot, q.mat.glow, sp, q.uvs, ns))
        flat_uv.append((q.mat.slot, q.mat.glow, sp, [(0.0, 0.0)] * 4, ns))
    half = ICON_LIGHT + np.array([0.0, 0.0, 1.0])
    half /= np.linalg.norm(half)

    def band(n, glow):
        lam = n @ ICON_LIGHT
        k = np.where(lam < -0.1, 0, np.where(lam < 0.3, 1, np.where(lam < 0.68, 2, 3)))
        if glow:
            k = np.maximum(k, 2)
        k = np.where(np.maximum(0.0, n @ half) ** 24 > 0.5, 4, k)
        return (k + 1) / 16.0
    id_rgb, cov = rasterize(quads, {"mesh": id_mesh, "rune": id_rune}, big, big, lambda n, g: np.ones(len(n)))
    band_rgb, _ = rasterize(flat_uv, white, big, big, band)
    key = np.where(cov, np.round(id_rgb[..., 0]).astype(int) * 8 + np.round(band_rgb[..., 0] * 16 / 255).astype(int)
                   - 1, -1)
    key = key.reshape(size, supersample, size, supersample).transpose(0, 2, 1, 3).reshape(size, size, -1)
    covered = (key >= 0).sum(axis=2)
    solid = covered >= supersample * supersample * 0.45
    out = np.zeros((size, size, 4), np.uint8)
    glowing = np.zeros((size, size), bool)
    by_id = {k + 1: m for k, m in enumerate(mats)}
    for y in range(size):
        for x in range(size):
            if not solid[y, x]:
                continue
            values, counts = np.unique(key[y, x][key[y, x] >= 0], return_counts=True)
            best = int(values[counts.argmax()])
            m = by_id.get(best // 8)
            if m is None:
                continue
            out[y, x, :3] = m.icon[min(4, best % 8)]
            out[y, x, 3] = 255
            glowing[y, x] = m.glow
    solid = out[..., 3] == 255

    def grow(mask):
        g = np.zeros_like(mask)
        g[1:, :] |= mask[:-1, :]
        g[:-1, :] |= mask[1:, :]
        g[:, 1:] |= mask[:, :-1]
        g[:, :-1] |= mask[:, 1:]
        return g
    edge = grow(solid) & ~solid
    near_glow = grow(glowing & solid)
    out[edge, :3] = ICON_OUTLINE
    out[edge & near_glow, :3] = ICON_GLOW_OUTLINE
    out[edge, 3] = 255
    return Image.fromarray(out, "RGBA")
