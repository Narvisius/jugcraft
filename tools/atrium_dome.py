"""The atrium dome (Justin, 27 October 2026: "for the pillars ... just the internals ... cylindrical at the bottom, but
then it becomes more of like a sphere as it goes up and then blends into the rest of the architecture"; option A of the
take 2 sheet, approved).

Over the rotunda the three pillar feet hung into the atrium as square concrete masses with flat soffits (y 11) up to the
roof slab (y 20). Now the rotunda's cylinder (R 15, the curved video walls on its face) goes on up as one surface of
revolution: at the ticker ring (y 11) it springs into a sphere (centre on the hall's axis at y 7.5, through r 15 at y 11,
r 9 at the roof) that the three feet are cut back to; at its top it meets the feet's roof (the pillars' floor over it).

Inside the sphere the feet's cells go (air). A cell the sphere passes through is a Dome Shell block
(jugcraft:dome_shell[index=n]): its look is the concrete of that cell outside the sphere, smooth (the sphere's surface
in it, cut exactly at the cell's sides, and the cell's faces cut round the sphere where they look onto air), baked into
the chunk like any block model (client/DomeShellModel); its collision the same at a quarter block. A formwork ring
joint every 1.5 blocks up the curve and a warm light line at its foot and its top. Cells outside the sphere stay as the
foot has them (its walls; the riser room behind).

The riser rooms' ladder goes up the feet's outer corner from the ground to the roof now (the inner one stood in the
dome); the pillars' tiers carry it on up from that corner (tower_tripillar.pillar_ladder).

write() writes data/jugcraft/drone_tower/atrium_dome.bin.gz (both sides read it: the cells' shapes and looks),
fit(b, put, M) puts the blocks into the design.
"""
from __future__ import annotations

import gzip
import math
import struct

import tripillar_geometry as G

AX = (0.5, 0.5)                                     # the hall's axis (the core cell's middle)
YC = 7.5                                            # the sphere's centre height
Y0, YR = 11.0, 20.0                                 # it springs at the slab (y 11); the roof slab's underside (y 20)
R2 = 15.0 ** 2 + (Y0 - YC) ** 2                     # through r 15 at y 11
R = math.sqrt(R2)
HW = G.half_width(2)
CENTRES = G.pillar_centres(2)
JOINTS = [Y0 + 1.5 * k for k in range(1, 6)]        # the formwork's ring joints
JOINT_H, JOINT_IN = 0.07, 0.02
LINE_IN = 0.035
LINES = [(Y0 + 0.02, Y0 + 0.13), (YR - 0.15, YR - 0.03)]   # the light lines at its foot and its top
GRID = 0.25                                         # the sphere's own grid (blocks)
STRIPS = 16                                         # a cut face's strips
SUB = 4                                             # collision: quarter blocks
MAT_CONCRETE, MAT_LINE, MAT_JOINT = 0, 1, 2
OUT = "../src/main/resources/data/jugcraft/drone_tower/atrium_dome.bin.gz"
DIRS = [(0, -1, 0), (0, 1, 0), (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0)]   # Direction order: down up north south west east


def r_at(Y):
    """The sphere's radius about the axis at height Y (the cylinder below the spring)."""
    return 15.0 if Y <= Y0 else math.sqrt(max(0.0, R2 - (Y - YC) ** 2))


def inside(x, y, z):
    """Whether a point is in the dome's air (inside the sphere, between the spring and the roof)."""
    return Y0 < y < YR and (x - AX[0]) ** 2 + (z - AX[1]) ** 2 + (y - YC) ** 2 < R2


def box_dist(x, y, z):
    """Nearest and farthest distance of cell (x, y, z) from the sphere's centre."""
    c = (AX[0], YC, AX[1])
    lo = [x, y, z]
    nearest = sum(max(lo[i] - c[i], 0.0, c[i] - (lo[i] + 1)) ** 2 for i in range(3))
    farthest = sum(max(abs(lo[i] - c[i]), abs(lo[i] + 1 - c[i])) ** 2 for i in range(3))
    return math.sqrt(nearest), math.sqrt(farthest)


def foot_cells(k):
    cx, cz = CENTRES[k]
    return {(x, z) for x in range(cx - HW, cx + HW + 1) for z in range(cz - HW, cz + HW + 1) if G.inside_pillar(x, z, cx, cz, HW)}


def classify():
    """Every foot cell from the slab to under the roof: 'air' (all inside the sphere), 'shell' (the sphere through it).
    Cells all outside it are left out (they stay as they are)."""
    out = {}
    for k in "ABC":
        for (x, z) in foot_cells(k):
            for y in range(int(Y0), int(YR)):
                near, far = box_dist(x, y, z)
                if far <= R:
                    out[(x, y, z)] = "air"
                elif near < R:
                    out[(x, y, z)] = "shell"
    return out


# ------------------------------------------------------------------ geometry

def clip(poly, axis, value, keep_above):
    """Sutherland-Hodgman against the plane coordinate[axis] = value; each vertex is (pos(3), extra...)."""
    out = []
    n = len(poly)
    for i in range(n):
        a, b = poly[i], poly[(i + 1) % n]
        da, db = a[0][axis] - value, b[0][axis] - value
        ina, inb = (da >= 0) if keep_above else (da <= 0), (db >= 0) if keep_above else (db <= 0)
        if ina:
            out.append(a)
        if ina != inb:
            t = da / (da - db)
            p = tuple(a[0][j] + (b[0][j] - a[0][j]) * t for j in range(3))
            out.append((p,))
    return out


def clip_to_cell(poly, cell):
    for axis in range(3):
        poly = clip(poly, axis, cell[axis], True)
        if len(poly) < 3:
            return []
        poly = clip(poly, axis, cell[axis] + 1, False)
        if len(poly) < 3:
            return []
    return poly


def sphere_point(theta, Y, inset=0.0):
    r = r_at(Y) - inset
    return (AX[0] + r * math.sin(theta), Y, AX[1] + r * math.cos(theta))


def sphere_normal(p):
    """Into the dome (toward the centre): the surface's face, smooth."""
    d = (AX[0] - p[0], YC - p[1], AX[1] - p[2])
    m = math.sqrt(sum(v * v for v in d))
    return tuple(v / m for v in d)


def project_uv(p, n, cell):
    """The block's own texture projection by the face's main direction (as a cube's faces), in 0..1 over the cell."""
    lx, ly, lz = p[0] - cell[0], p[1] - cell[1], p[2] - cell[2]
    ax = max(range(3), key=lambda i: abs(n[i]))
    if ax == 1:
        u, v = lx, lz
    elif ax == 0:
        u, v = lz, 1 - ly
    else:
        u, v = lx, 1 - ly
    return (min(1.0, max(0.0, u)), min(1.0, max(0.0, v)))


def to_quads(poly):
    """A convex polygon as quads (a triangle repeats its last corner)."""
    out = []
    i = 1
    while i + 1 < len(poly):
        if i + 2 < len(poly):
            out.append([poly[0], poly[i], poly[i + 1], poly[i + 2]])
            i += 2
        else:
            out.append([poly[0], poly[i], poly[i + 1], poly[i + 1]])
            i += 1
    return out


def surface_bands():
    """The sphere's grid quads for each foot (and the joints' and lines' bands): (corners, material)."""
    bands = []
    for k, (cx, cz) in CENTRES.items():
        cells = foot_cells(k)
        angles = [math.atan2(x + dx - AX[0], z + dz - AX[1]) for (x, z) in cells for dx in (0, 1) for dz in (0, 1)]
        a0 = math.atan2(cx + 0.5 - AX[0], cz + 0.5 - AX[1])
        rel = [((a - a0 + math.pi) % (2 * math.pi)) - math.pi for a in angles]
        lo, hi = a0 + min(rel) - 0.02, a0 + max(rel) + 0.02
        steps_y = int(round((YR - Y0) / GRID))
        rmax = 15.0
        steps_t = max(8, int(math.ceil((hi - lo) * rmax / GRID)))
        for j in range(steps_y):
            Ya, Yb = Y0 + j * GRID, Y0 + (j + 1) * GRID
            for i in range(steps_t):
                ta, tb = lo + (hi - lo) * i / steps_t, lo + (hi - lo) * (i + 1) / steps_t
                bands.append(([sphere_point(ta, Ya), sphere_point(tb, Ya), sphere_point(tb, Yb), sphere_point(ta, Yb)], MAT_CONCRETE, 0.0))
        for (ya, yb, mat, inset) in [(y, y + JOINT_H, MAT_JOINT, JOINT_IN) for y in JOINTS] + [(a, b, MAT_LINE, LINE_IN) for (a, b) in LINES]:
            for i in range(steps_t):
                ta, tb = lo + (hi - lo) * i / steps_t, lo + (hi - lo) * (i + 1) / steps_t
                bands.append(([sphere_point(ta, ya, inset), sphere_point(tb, ya, inset), sphere_point(tb, yb, inset), sphere_point(ta, yb, inset)],
                              mat, inset))
    return bands


def face_pieces(cell, d):
    """A cell's face (direction d) cut round the sphere: the parts outside it, as polygons (world corners)."""
    x0, y0, z0 = cell
    out = []
    ax = [i for i in range(3) if d[i] != 0][0]
    plane = cell[ax] + (1 if d[ax] > 0 else 0)
    # the face's two in-plane axes: strips along the first, intervals along the second
    u_ax, v_ax = [i for i in range(3) if i != ax]
    base = list(cell)

    def outside_intervals(u):
        """Along the strip at u: the v intervals (0..1 local) outside the sphere's air (just inside the cell: the face is
        the cell's solid's)."""
        p = [0.0, 0.0, 0.0]
        p[ax] = plane - d[ax] * 1e-6
        p[u_ax] = base[u_ax] + u
        cuts = [0.0, 1.0]
        # the air in this plane at u: a range of v where inside() holds; find its ends by sampling then bisecting
        n = 64
        prev = None
        for i in range(n + 1):
            v = i / n
            p[v_ax] = base[v_ax] + v
            ins = inside(*p)
            if prev is not None and ins != prev:
                lo_, hi_ = (i - 1) / n, v
                for _ in range(30):
                    m = (lo_ + hi_) / 2
                    p[v_ax] = base[v_ax] + m
                    if inside(*p) == prev:
                        lo_ = m
                    else:
                        hi_ = m
                cuts.append((lo_ + hi_) / 2)
            prev = ins
        cuts = sorted(set(cuts))
        keep = []
        for a, b in zip(cuts, cuts[1:]):
            m = (a + b) / 2
            p[v_ax] = base[v_ax] + m
            if not inside(*p) and b - a > 1e-6:
                keep.append((a, b))
        return keep

    def pt(u, v):
        p = [0.0, 0.0, 0.0]
        p[ax] = plane
        p[u_ax] = base[u_ax] + u
        p[v_ax] = base[v_ax] + v
        return tuple(p)
    whole = [(0.0, 1.0)]
    if all(outside_intervals(u) == whole for u in (1e-7, 0.25, 0.5, 0.75, 1 - 1e-7)):
        return [[pt(0, 0), pt(1, 0), pt(1, 1), pt(0, 1)]]      # the sphere doesn't reach it: one quad
    run = None                                                  # strips all solid in a row, as one quad
    for s in range(STRIPS):
        ua, ub = s / STRIPS, (s + 1) / STRIPS
        ia, ib = outside_intervals(ua + 1e-7), outside_intervals(ub - 1e-7)
        if ia == whole and ib == whole:
            run = (run if run is not None else ua)
            if s == STRIPS - 1:
                out.append([pt(run, 0), pt(ub, 0), pt(ub, 1), pt(run, 1)])
            continue
        if run is not None:
            out.append([pt(run, 0), pt(ua, 0), pt(ua, 1), pt(run, 1)])
            run = None
        if len(ia) != len(ib):
            im = outside_intervals((ua + ub) / 2)
            ia = ib = im
        for (va0, va1), (vb0, vb1) in zip(ia, ib):
            out.append([pt(ua, va0), pt(ub, vb0), pt(ub, vb1), pt(ua, va1)])
    return out


def orient(corners, normal):
    """Corners wound so the quad faces along normal (counter-clockwise seen from that side)."""
    a, b, c = corners[0], corners[1], corners[2]
    if b == c:
        c = corners[3]
    e1 = [b[i] - a[i] for i in range(3)]
    e2 = [c[i] - a[i] for i in range(3)]
    cr = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0])
    if sum(cr[i] * normal[i] for i in range(3)) < 0:
        return [corners[0], corners[3], corners[2], corners[1]]
    return corners


def build(cls=None):
    """Each shell cell's quads and quarter-block solid: {cell: (quads, occupancy)}; a quad is (corners, uvs, normals,
    material, cull face index or -1)."""
    cls = cls if cls is not None else classify()
    shells = sorted(p for p, kind in cls.items() if kind == "shell")
    shell_set = set(shells)
    geo = {p: [] for p in shells}
    # the sphere (and its joints and lines): its grid's quads cut into the cells they cross
    for corners, mat, inset in surface_bands():
        xs = [c[0] for c in corners]
        ys = [c[1] for c in corners]
        zs = [c[2] for c in corners]
        for x in range(math.floor(min(xs)), math.floor(max(xs)) + 1):
            for y in range(math.floor(min(ys)), math.floor(max(ys)) + 1):
                for z in range(math.floor(min(zs)), math.floor(max(zs)) + 1):
                    if (x, y, z) not in shell_set:
                        continue
                    poly = clip_to_cell([(c,) for c in corners], (x, y, z))
                    if len(poly) < 3:
                        continue
                    pts = [v[0] for v in poly]
                    for q in to_quads(pts):
                        nn = [sphere_normal(p) for p in q]
                        mid = tuple(sum(p[i] for p in q) / 4 for i in range(3))
                        q = orient(q, sphere_normal(mid))
                        nn = [sphere_normal(p) for p in q]
                        uv = [project_uv(p, n, (x, y, z)) for p, n in zip(q, nn)]
                        geo[(x, y, z)].append((q, uv, nn, mat, -1))
    # the cells' own faces, cut round the sphere, where they look onto anything but another shell cell
    for cell in shells:
        for di, d in enumerate(DIRS):
            nb = (cell[0] + d[0], cell[1] + d[1], cell[2] + d[2])
            if nb in shell_set:
                continue
            for q in face_pieces(cell, d):
                q = orient(q, d)
                n = tuple(float(v) for v in d)
                uv = [project_uv(p, n, cell) for p in q]
                geo[cell].append((q, uv, [n] * 4, MAT_CONCRETE, di))
    occ = {}
    for cell in shells:
        bits = 0
        for i in range(SUB):
            for j in range(SUB):
                for k in range(SUB):
                    p = (cell[0] + (i + 0.5) / SUB, cell[1] + (j + 0.5) / SUB, cell[2] + (k + 0.5) / SUB)
                    if not inside(*p):
                        bits |= 1 << (i * SUB * SUB + j * SUB + k)
        occ[cell] = bits
    return shells, geo, occ


def write(path=OUT):
    """atrium_dome.bin.gz: 'DOME' 1, the cell count; per cell (in index order) its place (x, y, z from the core), its
    quarter-block solid (64 bits: x, then y, then z), its quad count, and per quad the material (0 concrete, 1 light line,
    2 joint), the cull face (Direction index, -1 none), four corners of x, y, z (within the cell), u, v, nx, ny, nz."""
    shells, geo, occ = build()
    buf = bytearray(b"DOME")
    buf += struct.pack(">ii", 1, len(shells))
    quads = 0
    for cell in shells:
        x, y, z = cell
        buf += struct.pack(">iiiq", x, y, z, occ[cell] - (1 << 64) if occ[cell] >= 1 << 63 else occ[cell])
        qs = geo[cell]
        buf += struct.pack(">i", len(qs))
        for corners, uvs, nns, mat, cull in qs:
            buf += struct.pack(">bb", mat, cull)
            for p, uv, n in zip(corners, uvs, nns):
                buf += struct.pack(">8f", p[0] - x, p[1] - y, p[2] - z, uv[0], uv[1], n[0], n[1], n[2])
            quads += 1
    import keep_same
    keep_same.gz(bytes(buf), path, compresslevel=9)     # (the same data leaves the file as it is)
    return len(shells), quads


def index_of():
    shells = sorted(p for p, kind in classify().items() if kind == "shell")
    return {p: i for i, p in enumerate(shells)}


def fit(b, put, M):
    """The dome into the design: the feet's cells inside the sphere gone, the shell cells, the lantern's glass over its
    opening, the soffit's downlights (which hung from the slab the dome takes) as hidden light where they were."""
    cls = classify()
    idx = {p: i for i, p in enumerate(sorted(p for p, kind in cls.items() if kind == "shell"))}
    for (x, y, z), kind in cls.items():
        b.pop((x, y, z), None)
        if kind == "shell":
            put(b, x, y, z, f"jugcraft:dome_shell[index={idx[(x, y, z)]}]")
    for k in "ABC":
        for (x, z) in foot_cells(k):
            # the cut's downlights hung from its soffit: light, unseen, where each was
            if b.get((x, 10, z)) == M["lamp"] and (x, 11, z) in cls:
                b[(x, 10, z)] = "minecraft:light[level=15,waterlogged=false]"
    return len(idx)


if __name__ == "__main__":
    n, q = write()
    print("dome shell cells", n, "quads", q)
