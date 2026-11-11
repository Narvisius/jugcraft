"""The site compiler's core (towers and pump houses build, 19 October 2026): turns control_boards drawing operations into
the quads the game draws, cut at block boundaries and filed by block, so a whole structure can be baked into the world's
chunk meshes (client/SiteModel.java) instead of being drawn every frame by a block entity renderer.

The tessellation copies client/ControlBoards.java and NuclearRenderer.Frame exactly (box faces, oriented boxes, quads,
cylinders capped at their far end, the per-block texture tiling of a whole material, the cylinders' wrap), so a baked
piece looks as the same operations drawn by the renderer would. Coordinates are the site's own, in blocks; a quad's front
is where (tr - tl) x (bl - tl) points (the operations' rule).

A polygon here is (points [n x 3], uvs [n x 2] in 0..1 of its texture, normal [3], texture name, lit). Quads are cut at
every block boundary they cross and each piece goes to the block it lies in, so no geometry ever reaches outside its
block (the game culls a chunk section's geometry by the section's bounds).
"""
import gzip
import json
import math
import struct
from collections import defaultdict

import numpy as np

EPS = 1e-9


# ------------------------------------------------------------------ materials

def region(mat):
    """(texture, u0, v0, u1, v1, tiled) for an operation's material: a whole texture (tiled about a block a repeat) or
    [name, u0, v0, u1, v1] (stretched)."""
    if isinstance(mat, str):
        return mat, 0.0, 0.0, 1.0, 1.0, True
    return mat[0], float(mat[1]), float(mat[2]), float(mat[3]), float(mat[4]), False


# ------------------------------------------------------------------ tessellation (ControlBoards / NuclearRenderer.Frame)

class Tess:
    """Collects polygons: (pts, uvs, n, tex, lit)."""

    def __init__(self):
        self.polys = []

    def quad(self, tl, bl, br, tr, n, tex, u0, v0, u1, v1, lit):
        self.polys.append((np.array([tl, bl, br, tr], float), np.array([(u0, v0), (u0, v1), (u1, v1), (u1, v0)], float),
                           np.asarray(n, float), tex, lit))

    def tiled(self, tl, bl, br, tr, n, reg, lit, tile=1.0):
        tex, U0, V0, U1, V1, _ = reg
        tl, bl, br, tr = (np.asarray(p, float) for p in (tl, bl, br, tr))
        width, height = np.linalg.norm(tr - tl), np.linalg.norm(bl - tl)
        nu = max(1, int(math.ceil(width / tile - 0.01)))
        nv = max(1, int(math.ceil(height / tile - 0.01)))
        du, dv = width / tile, height / tile

        def lerp(a, b):
            top = tl + (tr - tl) * a
            bottom = bl + (br - bl) * a
            return top + (bottom - top) * b
        for i in range(nu):
            a0, a1 = i / nu, (i + 1) / nu
            tu0, tu1 = U0, U0 + (U1 - U0) * min(1.0, du / nu)
            for j in range(nv):
                b0, b1 = j / nv, (j + 1) / nv
                tv0, tv1 = V0, V0 + (V1 - V0) * min(1.0, dv / nv)
                self.quad(lerp(a0, b0), lerp(a0, b1), lerp(a1, b1), lerp(a1, b0), n, tex, tu0, tv0, tu1, tv1, lit)

    def face(self, tl, bl, br, tr, mat, lit):
        """ControlBoards.quad: a whole material tiled, a region stretched; the normal from the corners."""
        if mat is None:
            return
        tl, bl, br, tr = (np.asarray(p, float) for p in (tl, bl, br, tr))
        n = np.cross(tr - tl, bl - tl)
        if n @ n < 1e-14:
            return
        n = n / np.linalg.norm(n)
        reg = region(mat)
        if reg[5]:
            self.tiled(tl, bl, br, tr, n, reg, lit)
        else:
            self.quad(tl, bl, br, tr, n, reg[0], reg[1], reg[2], reg[3], reg[4], lit)

    def box6(self, faces, p, lit):
        """ControlBoards.box: six faces (front -z, back, left -x, right, top, bottom) of a box given by its corner
        function p(a, b, c)."""
        one = isinstance(faces, str) or (isinstance(faces, list) and len(faces) == 5 and isinstance(faces[1], (int, float)))
        mats = [faces] * 6 if one else list(faces)
        self.face(p(0, 1, 0), p(0, 0, 0), p(1, 0, 0), p(1, 1, 0), mats[0], lit)
        self.face(p(1, 1, 1), p(1, 0, 1), p(0, 0, 1), p(0, 1, 1), mats[1], lit)
        self.face(p(0, 1, 1), p(0, 0, 1), p(0, 0, 0), p(0, 1, 0), mats[2], lit)
        self.face(p(1, 1, 0), p(1, 0, 0), p(1, 0, 1), p(1, 1, 1), mats[3], lit)
        self.face(p(0, 1, 1), p(0, 1, 0), p(1, 1, 0), p(1, 1, 1), mats[4], lit)
        self.face(p(0, 0, 0), p(0, 0, 1), p(1, 0, 1), p(1, 0, 0), mats[5], lit)

    @staticmethod
    def repeats_for(circumference, sides):
        want = max(1, int(round(circumference)))
        best = 1
        for r in range(1, sides + 1):
            if sides % r == 0 and abs(r - want) < abs(best - want):
                best = r
        return best

    def revolve(self, a, b, profile, sides, reg, lit):
        a, b = np.asarray(a, float), np.asarray(b, float)
        axis = b - a
        axis = axis / np.linalg.norm(axis)
        ref = np.array([0.0, 0.0, 1.0]) if abs(axis[1]) > 0.9 else np.array([0.0, 1.0, 0.0])
        n1 = np.cross(axis, ref)
        n1 /= np.linalg.norm(n1)
        n2 = np.cross(n1, axis)
        n2 /= np.linalg.norm(n2)
        maxr = max(p[1] for p in profile)
        repeats = self.repeats_for(2 * math.pi * maxr, sides)
        tex, U0, V0, U1, V1, _ = reg
        for i in range(len(profile) - 1):
            d0, d1, r0, r1 = profile[i][0], profile[i + 1][0], profile[i][1], profile[i + 1][1]
            slant = math.hypot(d1 - d0, r1 - r0)
            rows = max(1, int(math.ceil(slant - 0.05)))
            for row in range(rows):
                t0, t1 = row / rows, (row + 1) / rows
                da, db = d0 + (d1 - d0) * t0, d0 + (d1 - d0) * t1
                ra, rb = r0 + (r1 - r0) * t0, r0 + (r1 - r0) * t1
                vspan = min(1.0, slant / rows)
                for k in range(sides):
                    g0, g1 = 2 * math.pi * k / sides, 2 * math.pi * (k + 1) / sides
                    dir0 = n1 * math.cos(g0) + n2 * math.sin(g0)
                    dir1 = n1 * math.cos(g1) + n2 * math.sin(g1)
                    p00, p01 = a + axis * da + dir0 * ra, a + axis * db + dir0 * rb
                    p10, p11 = a + axis * da + dir1 * ra, a + axis * db + dir1 * rb
                    n = dir0 + dir1
                    n = n / np.linalg.norm(n)
                    sl = repeats / sides
                    u0 = (k * sl) % 1.0
                    u1 = u0 + sl
                    fu0, fu1 = U0 + (U1 - U0) * u0, U0 + (U1 - U0) * min(1.0, u1)
                    fv0, fv1 = V0, V0 + (V1 - V0) * vspan
                    self.quad(p01, p00, p10, p11, n, tex, fu0, fv0, fu1, fv1, lit)

    def disc(self, centre, outward, radius, sides, reg, lit):
        centre, outward = np.asarray(centre, float), np.asarray(outward, float)
        ref = np.array([0.0, 0.0, 1.0]) if abs(outward[1]) > 0.9 else np.array([0.0, 1.0, 0.0])
        n1 = np.cross(outward, ref)
        n1 /= np.linalg.norm(n1)
        n2 = np.cross(n1, outward)
        n2 /= np.linalg.norm(n2)
        tex, U0, V0, U1, V1, _ = reg
        cu, cv = (U0 + U1) / 2, (V0 + V1) / 2
        hu, hv = (U1 - U0) / 2 * min(1.0, radius), (V1 - V0) / 2 * min(1.0, radius)
        for k in range(0, sides, 2):
            g0, g1, g2 = (2 * math.pi * j / sides for j in (k, k + 1, k + 2))
            p0 = centre + n1 * math.cos(g0) * radius + n2 * math.sin(g0) * radius
            p1 = centre + n1 * math.cos(g1) * radius + n2 * math.sin(g1) * radius
            p2 = centre + n1 * math.cos(g2) * radius + n2 * math.sin(g2) * radius
            self.quad(centre, p0, p1, p2, outward, tex, cu - hu, cv - hv, cu + hu, cv + hv, lit)

    def op(self, op, lit=False):
        k = op[0]
        if k == "b":
            x0, y0, z0, x1, y1, z1 = op[1:7]
            self.box6(op[7], lambda a, b, c: (x1 if a else x0, y1 if b else y0, z1 if c else z0), lit)
        elif k == "o":
            c = np.array(op[1:4], float)
            u = np.array(op[4:7], float)
            w0 = np.array(op[7:10], float)
            hu, hv, hw = op[10:13]
            w = np.cross(u, w0)
            self.box6(op[13], lambda a, b, kk: c + u * (hu * (2 * a - 1)) + w0 * (hv * (2 * b - 1)) + w * (hw * (2 * kk - 1)), lit)
        elif k == "q":
            self.face(op[1:4], op[4:7], op[7:10], op[10:13], op[13], lit)
        elif k == "c":
            a, b = np.array(op[1:4], float), np.array(op[4:7], float)
            L = np.linalg.norm(b - a)
            if L < 1e-9:
                return
            r, sides = op[7], sides_for(op[7], int(op[8]))
            self.revolve(a, b, [(0.0, r), (L, r)], sides, region(op[9]), lit)
            if op[10] is not None:
                self.disc(b, (b - a) / L, r, sides, region(op[10]), lit)
        elif k == "r":
            a, b = np.array(op[1:4], float), np.array(op[4:7], float)
            prof = [tuple(p) for p in op[7]]
            sides = int(op[8])
            self.revolve(a, b, prof, sides, region(op[9]), lit)
            axis = (b - a) / np.linalg.norm(b - a)
            if op[10] is not None and prof[0][1] > 1e-3:
                self.disc(a + axis * prof[0][0], -axis, prof[0][1], sides, region(op[10]), lit)
            if op[11] is not None and prof[-1][1] > 1e-3:
                self.disc(a + axis * prof[-1][0], axis, prof[-1][1], sides, region(op[11]), lit)
        else:
            raise ValueError(k)


def sides_for(r, sides):
    """Sides for a round part baked into the world: small ones need fewer facets to look round (a 2 cm rod with 12
    sides has facets 1 cm wide, under a texel at a few blocks)."""
    if r < 0.02:
        return min(sides, 4)
    if r < 0.05:
        return min(sides, 6)
    if r < 0.1:
        return min(sides, 10)
    return sides


def tessellate(still, lit=()):
    t = Tess()
    for op in still:
        t.op(op, False)
    for op in lit:
        t.op(op, True)
    return t.polys


# ------------------------------------------------------------------ cutting polygons at the block grid

def _clip(pts, uvs, axis, value, keep_below):
    """Sutherland-Hodgman clip of a convex polygon by the plane coordinate[axis] = value."""
    out_p, out_u = [], []
    n = len(pts)
    for i in range(n):
        p, q = pts[i], pts[(i + 1) % n]
        up, uq = uvs[i], uvs[(i + 1) % n]
        dp, dq = p[axis] - value, q[axis] - value
        inp = dp <= EPS if keep_below else dp >= -EPS
        inq = dq <= EPS if keep_below else dq >= -EPS
        if inp:
            out_p.append(p)
            out_u.append(up)
        if inp != inq:
            t = dp / (dp - dq)
            out_p.append(p + (q - p) * t)
            out_u.append(up + (uq - up) * t)
    return out_p, out_u


def _dedupe(pts, uvs):
    P, U = [], []
    for p, u in zip(pts, uvs):
        if not P or np.abs(p - P[-1]).max() > 1e-7:
            P.append(p)
            U.append(u)
    while len(P) > 1 and np.abs(P[0] - P[-1]).max() <= 1e-7:
        P.pop()
        U.pop()
    return P, U


def split_to_blocks(pts, uvs, cell=16):
    """Cuts a convex polygon at every chunk section boundary (every cell blocks) it crosses. Yields ((bx, by, bz), pts,
    uvs) per piece, filed under the block holding its middle (so it never reaches outside that block's section)."""
    pieces = [(list(pts), list(uvs))]
    for axis in range(3):
        nxt = []
        for P, U in pieces:
            lo, hi = min(p[axis] for p in P), max(p[axis] for p in P)
            c0, c1 = math.floor(lo / cell + 1e-9), math.floor(hi / cell - 1e-9)
            if c0 >= c1:
                nxt.append((P, U))
                continue
            rest_p, rest_u = P, U
            for cut in range(c0 + 1, c1 + 1):
                below_p, below_u = _clip(rest_p, rest_u, axis, cut * cell, True)
                rest_p, rest_u = _clip(rest_p, rest_u, axis, cut * cell, False)
                below_p, below_u = _dedupe(below_p, below_u)
                if len(below_p) >= 3:
                    nxt.append((below_p, below_u))
                rest_p, rest_u = _dedupe(rest_p, rest_u)
                if len(rest_p) < 3:
                    break
            if len(rest_p) >= 3:
                nxt.append((rest_p, rest_u))
        pieces = nxt
    for P, U in pieces:
        c = np.mean(P, axis=0)
        yield (math.floor(c[0]), math.floor(c[1]), math.floor(c[2])), P, U


def fan_quads(P, U):
    """A convex polygon as quads (each a quad or a triangle with its last corner doubled), in the polygon's order."""
    n = len(P)
    if n == 3:
        return [([P[0], P[1], P[2], P[2]], [U[0], U[1], U[2], U[2]])]
    if n == 4:
        return [(P, U)]
    out = []
    k = 1
    while k + 1 < n:
        if k + 2 < n:
            out.append(([P[0], P[k], P[k + 1], P[k + 2]], [U[0], U[k], U[k + 1], U[k + 2]]))
            k += 2
        else:
            out.append(([P[0], P[k], P[k + 1], P[k + 1]], [U[0], U[k], U[k + 1], U[k + 1]]))
            k += 1
    return out


def area(P):
    s = np.zeros(3)
    for i in range(len(P)):
        s += np.cross(P[i], P[(i + 1) % len(P)])
    return np.linalg.norm(s) / 2


def bake(polys):
    """{block: [(pts 4x3 local to the block, uvs 4x2, n, tex, lit)]}"""
    blocks = defaultdict(list)
    for pts, uvs, n, tex, lit in polys:
        for b, P, U in split_to_blocks(list(pts), list(uvs)):
            if area(P) < 1e-7:
                continue
            o = np.array(b, float)
            for QP, QU in fan_quads(P, U):
                blocks[b].append((np.array(QP) - o, np.array(QU), n, tex, lit))
    return blocks


# ------------------------------------------------------------------ collision (conservative voxels, merged into boxes)

RES = 16  # collision voxels a block


class Voxels:
    """Solid voxels (1/16 block) by block."""

    def __init__(self):
        self.blocks = defaultdict(lambda: np.zeros((RES, RES, RES), bool))

    _CENTRES = None

    def _mark(self, lo, hi, inside):
        """Marks voxels whose centres pass inside(points [n x 3]) within the box lo..hi (blocks), a block at a time."""
        if Voxels._CENTRES is None:
            g = np.stack(np.meshgrid(np.arange(RES), np.arange(RES), np.arange(RES), indexing="ij"), -1).reshape(-1, 3)
            Voxels._CENTRES = (g, (g + 0.5) / RES)
        g, c = Voxels._CENTRES
        lo, hi = np.asarray(lo, float), np.asarray(hi, float)
        b0 = np.floor(lo).astype(int)
        b1 = np.floor(hi - 1e-9).astype(int)
        for bx in range(b0[0], b1[0] + 1):
            for by in range(b0[1], b1[1] + 1):
                for bz in range(b0[2], b1[2] + 1):
                    o = np.array([bx, by, bz], float)
                    m = inside(c + o)
                    if not m.any():
                        continue
                    sel = g[m]
                    self.blocks[(bx, by, bz)][sel[:, 0], sel[:, 1], sel[:, 2]] = True

    def obox(self, c, axes, half):
        """An oriented box: centre c, unit axes (3 x 3, rows), half sizes; padded by half a voxel along each axis (so
        thin plates still make a voxel layer)."""
        c, axes, half = np.asarray(c, float), np.asarray(axes, float), np.asarray(half, float)
        pad = 0.5 / RES
        h = half + pad
        ext = np.abs(axes).T @ h
        self._mark(c - ext, c + ext, lambda p: np.all(np.abs((p - c) @ axes.T) <= h + 1e-9, axis=1))

    def box(self, x0, y0, z0, x1, y1, z1):
        c = np.array([(x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2])
        self.obox(c, np.eye(3), [abs(x1 - x0) / 2, abs(y1 - y0) / 2, abs(z1 - z0) / 2])

    def cyl(self, a, b, r):
        a, b = np.asarray(a, float), np.asarray(b, float)
        ax = b - a
        L = np.linalg.norm(ax)
        if L < 1e-9:
            return
        ax /= L
        pad = 0.5 / RES
        ext = (r + pad) * np.sqrt(np.clip(1 - ax * ax, 0, 1)) + pad
        lo = np.minimum(a, b) - ext
        hi = np.maximum(a, b) + ext

        def inside(p):
            d = p - a
            t = d @ ax
            perp = d - np.outer(t, ax)
            return (t >= -pad) & (t <= L + pad) & (np.einsum("ij,ij->i", perp, perp) <= (r + pad) ** 2)
        self._mark(lo, hi, inside)

    def op(self, op, min_thick=0.0, min_radius=0.0):
        k = op[0]
        if k == "b":
            x0, y0, z0, x1, y1, z1 = op[1:7]
            if min(abs(x1 - x0), abs(y1 - y0), abs(z1 - z0)) >= min_thick:
                self.box(x0, y0, z0, x1, y1, z1)
        elif k == "o":
            c = np.array(op[1:4], float)
            u = np.array(op[4:7], float)
            w0 = np.array(op[7:10], float)
            w = np.cross(u, w0)
            hu, hv, hw = op[10:13]
            if 2 * min(hu, hv, hw) >= min_thick:
                self.obox(c, np.stack([u, w0, w]), [hu, hv, hw])
        elif k == "c":
            if op[7] >= min_radius:
                self.cyl(op[1:4], op[4:7], op[7])

    def merged(self, b, limit=48):
        """The block's voxels as boxes (x0, y0, z0, x1, y1, z1 in 1/16), greedily merged."""
        v = self.blocks[b].copy()
        out = []
        while v.any():
            x, y, z = np.argwhere(v)[0]
            x1 = x
            while x1 + 1 < RES and v[x1 + 1, y, z]:
                x1 += 1
            z1 = z
            while z1 + 1 < RES and v[x:x1 + 1, y, z1 + 1].all():
                z1 += 1
            y1 = y
            while y1 + 1 < RES and v[x:x1 + 1, y1 + 1, z:z1 + 1].all():
                y1 += 1
            v[x:x1 + 1, y:y1 + 1, z:z1 + 1] = False
            out.append((int(x), int(y), int(z), int(x1) + 1, int(y1) + 1, int(z1) + 1))
        if len(out) > limit:
            # Too many: fall back to the bounding box of each horizontal layer band (coarser, still walkable).
            vv = self.blocks[b]
            out = []
            for y0 in range(0, RES, 4):
                layer = vv[:, y0:y0 + 4, :]
                if layer.any():
                    xs, ys, zs = np.nonzero(layer)
                    out.append((int(xs.min()), y0 + int(ys.min()), int(zs.min()), int(xs.max()) + 1, y0 + int(ys.max()) + 1,
                                int(zs.max()) + 1))
        return out


# ------------------------------------------------------------------ the binary site file

def texture_layer(img):
    """0 solid, 1 cutout (alpha 0 or 255 only), 2 translucent."""
    a = np.asarray(img.convert("RGBA"))[..., 3]
    if a.min() == 255:
        return 0
    if np.all((a == 0) | (a == 255)):
        return 1
    return 2


def _q(v, scale, lo, hi):
    return max(lo, min(hi, int(round(v * scale))))


def write_site(path, blocks, solids, flags, sprites, live, layers):
    """blocks: {block: [quads]}; solids: {block: [boxes]}; flags: {block: int}; sprites: [names]; live: JSON-able dict;
    layers: {name: 0|1|2}. Little-endian, gzipped."""
    index = {name: i for i, name in enumerate(sprites)}
    out = bytearray()
    out += b"JSIT"
    out += struct.pack("<H", 1)
    out += struct.pack("<H", len(sprites))
    for name in sprites:
        e = name.encode()
        out += struct.pack("<H", len(e)) + e + struct.pack("<B", layers.get(name, 0))
    keys = sorted(set(blocks) | set(solids) | set(flags))
    out += struct.pack("<I", len(keys))
    for b in keys:
        quads = blocks.get(b, [])
        boxes = solids.get(b, [])
        out += struct.pack("<hhhBHB", b[0], b[1], b[2], flags.get(b, 0), len(quads), len(boxes))
        for P, U, n, tex, lit in quads:
            out += struct.pack("<HB", index[tex], 1 if lit else 0)
            out += struct.pack("<bbb", *(_q(c, 127, -127, 127) for c in n))
            for p, u in zip(P, U):
                out += struct.pack("<hhhHH", _q(p[0], 4096, -32768, 32767), _q(p[1], 4096, -32768, 32767), _q(p[2], 4096, -32768, 32767),
                                   _q(u[0], 65535, 0, 65535), _q(u[1], 65535, 0, 65535))
        for bx in boxes:
            out += struct.pack("<6B", *bx)
    js = json.dumps(live, separators=(",", ":")).encode()
    out += struct.pack("<I", len(js)) + js
    with gzip.open(path, "wb", compresslevel=9) as f:
        f.write(bytes(out))
    return len(out)
