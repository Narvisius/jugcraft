"""Sculpting for block models: a figure described by smooth solids (ellipsoids, tapering limbs, boxes), cut out of a
grid of small cubes and merged back into as few model boxes as cover it exactly. Used for the graveyard pack's statues
(tools/graveyard_models.py), whose angels, lamb and hound would read as lumps if built from a handful of boxes.

Each solid carries a texture variable; where solids overlap, the last added wins. `subtract` carves. The grid's cube
is `res` pixels; the boxes come out unturned, so a model cut into blocks (graveyard_data.cut) can split them.
"""
import math


class Ellipsoid:
    def __init__(self, centre, radii, tex, axes=None):
        """An ellipsoid at `centre` with `radii` along its `axes` (three orthogonal directions; x, y, z if None)."""
        self.c = centre
        self.r = radii
        self.tex = tex
        if axes is None:
            axes = ((1, 0, 0), (0, 1, 0), (0, 0, 1))
        self.axes = [_unit(a) for a in axes]
        reach = max(radii)
        self.lo = [centre[k] - reach for k in range(3)]
        self.hi = [centre[k] + reach for k in range(3)]

    def inside(self, p):
        d = [p[k] - self.c[k] for k in range(3)]
        total = 0.0
        for axis, radius in zip(self.axes, self.r):
            total += (_dot(d, axis) / radius) ** 2
        return total <= 1.0


class Limb:
    """A tapering rod from `a` (radius `ra`) to `b` (radius `rb`) with round ends: an arm, a leg, a feather."""

    def __init__(self, a, b, ra, rb, tex, flat=None):
        self.a, self.b, self.ra, self.rb, self.tex = a, b, ra, rb, tex
        # `flat`: (direction, factor) squashes the limb across that direction, as for a feather or a fold of cloth.
        self.flat = (_unit(flat[0]), flat[1]) if flat else None
        reach = max(ra, rb)
        self.lo = [min(a[k], b[k]) - reach for k in range(3)]
        self.hi = [max(a[k], b[k]) + reach for k in range(3)]

    def inside(self, p):
        ab = [self.b[k] - self.a[k] for k in range(3)]
        ap = [p[k] - self.a[k] for k in range(3)]
        length2 = _dot(ab, ab) or 1e-9
        t = max(0.0, min(1.0, _dot(ap, ab) / length2))
        q = [self.a[k] + ab[k] * t for k in range(3)]
        d = [p[k] - q[k] for k in range(3)]
        if self.flat:
            axis, factor = self.flat
            along = _dot(d, axis)
            d = [d[k] + axis[k] * along * (1 / factor - 1) for k in range(3)]
        radius = self.ra + (self.rb - self.ra) * t
        return _dot(d, d) <= radius * radius


class Block:
    def __init__(self, lo, hi, tex):
        self.lo, self.hi, self.tex = list(lo), list(hi), tex

    def inside(self, p):
        return all(self.lo[k] <= p[k] <= self.hi[k] for k in range(3))


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _unit(v):
    n = math.sqrt(_dot(v, v))
    return (v[0] / n, v[1] / n, v[2] / n)


class Sculpture:
    def __init__(self, res=0.5):
        self.res = res
        self.parts = []  # (solid, carve)

    def add(self, *solids):
        for s in solids:
            self.parts.append((s, False))
        return self

    def subtract(self, *solids):
        for s in solids:
            self.parts.append((s, True))
        return self

    def voxels(self):
        res = self.res
        lo = [min(s.lo[k] for s, carve in self.parts if not carve) for k in range(3)]
        hi = [max(s.hi[k] for s, carve in self.parts if not carve) for k in range(3)]
        start = [math.floor(lo[k] / res) for k in range(3)]
        stop = [math.ceil(hi[k] / res) for k in range(3)]
        grid = {}
        for i in range(start[0], stop[0]):
            for j in range(start[1], stop[1]):
                for k in range(start[2], stop[2]):
                    p = ((i + 0.5) * res, (j + 0.5) * res, (k + 0.5) * res)
                    tex = None
                    for solid, carve in self.parts:
                        if all(solid.lo[a] - res <= p[a] <= solid.hi[a] + res for a in range(3)) and solid.inside(p):
                            tex = None if carve else solid.tex
                    if tex:
                        grid[(i, j, k)] = tex
        return grid

    def boxes(self, faces_out=True):
        """The sculpture as model boxes: cubes of one texture merged greedily along x, then z, then y."""
        from decor_data import box
        grid = self.voxels()
        res = self.res
        used = set()
        out = []
        for key in sorted(grid, key=lambda v: (v[1], v[2], v[0])):
            if key in used:
                continue
            tex = grid[key]
            i, j, k = key
            # Grow along x.
            i1 = i
            while (i1 + 1, j, k) in grid and (i1 + 1, j, k) not in used and grid[(i1 + 1, j, k)] == tex:
                i1 += 1
            # Then along z, while the whole row matches.
            k1 = k
            while all((x, j, k1 + 1) in grid and (x, j, k1 + 1) not in used and grid[(x, j, k1 + 1)] == tex for x in range(i, i1 + 1)):
                k1 += 1
            # Then along y, while the whole slab matches.
            j1 = j
            while all((x, j1 + 1, z) in grid and (x, j1 + 1, z) not in used and grid[(x, j1 + 1, z)] == tex
                      for x in range(i, i1 + 1) for z in range(k, k1 + 1)):
                j1 += 1
            for x in range(i, i1 + 1):
                for y in range(j, j1 + 1):
                    for z in range(k, k1 + 1):
                        used.add((x, y, z))
            lo = (round(i * res, 4), round(j * res, 4), round(k * res, 4))
            hi = (round((i1 + 1) * res, 4), round((j1 + 1) * res, 4), round((k1 + 1) * res, 4))
            faces = _exposed(grid, (i, j, k), (i1, j1, k1)) if faces_out else ("north", "south", "east", "west", "up", "down")
            if faces:
                out.append(box(lo, hi, tex, faces=faces))
        return out


def _exposed(grid, lo, hi):
    """The sides of the box of cubes lo..hi (inclusive) not wholly covered by other cubes."""
    i0, j0, k0 = lo
    i1, j1, k1 = hi
    faces = []
    checks = {
        "west": [(i0 - 1, y, z) for y in range(j0, j1 + 1) for z in range(k0, k1 + 1)],
        "east": [(i1 + 1, y, z) for y in range(j0, j1 + 1) for z in range(k0, k1 + 1)],
        "down": [(x, j0 - 1, z) for x in range(i0, i1 + 1) for z in range(k0, k1 + 1)],
        "up": [(x, j1 + 1, z) for x in range(i0, i1 + 1) for z in range(k0, k1 + 1)],
        "north": [(x, y, k0 - 1) for x in range(i0, i1 + 1) for y in range(j0, j1 + 1)],
        "south": [(x, y, k1 + 1) for x in range(i0, i1 + 1) for y in range(j0, j1 + 1)],
    }
    for side, cells in checks.items():
        if not all(c in grid for c in cells):
            faces.append(side)
    return tuple(faces)
