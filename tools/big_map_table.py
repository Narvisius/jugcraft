"""The Big Map Table and the map room's sign (work list item 16, "Big map table for the topographic hologram, in its own
map room upstairs", approved by Justin on 23 October 2026 after three takes; docs/features/drone-tower-tripillar.md).

The table: a 13 x 13 octagon (the tower's chamfers) standing 0.86 high: a dark plinth set in under the top with a lit
toe-kick, a composite top with a brushed steel edge band and a stainless lean rail round it on brackets, a round glass
well 12 across with the projector's emitter grid under the glass, and a touch panel in each chamfered corner (live: the
renderer draws its page on the mesh's live faces). Over the well the renderer draws the world round the tower in
miniature, read from the world itself (tower/MapScan): Justin asked for "actual minecraft terrain from the world its
in", trees, the topography, progress as things change, everything at its true height ("if there's a floating island
... it'll show it floating"), the map zooming out as the drones go out, its edge fading "from visible to not visible",
and, against lag, a refresh of only the area it maps, every 15 seconds.

Writes:
- assets/jugcraft/tower_mesh/big_map_table.bin.gz: the table (the console mesh format, client/ConsoleMesh: quads, the
  four panels as live faces, kind LCD, ids 0-3 from the north-east corner clockwise), its texture
  textures/entity/big_map_table.png;
- assets/jugcraft/tower_mesh/big_map_table.json: the cells the table covers (the master, its middle, first) and each
  one's collision boxes (pixels), read by tower/BigMapTableBlock on both sides;
- the blockstates, item, lang, loot tables, recipe and pickaxe tag of `big_map_table`, `big_map_table_part` and
  `map_room_sign` (a 4-wide light box over the room's doors, nuclear/MultiFurnitureBlock as the data hall's sign).
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import tower_control_console as TCC

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"

ID, PART_ID, SIGN = "big_map_table", "big_map_table_part", "map_room_sign"
NAME, SIGN_NAME = "Big Map Table", "Map Room Sign"
MESH = ASSETS / "tower_mesh/big_map_table.bin.gz"
TEXTURE = ASSETS / "textures/entity/big_map_table.png"
FOOTPRINT = ASSETS / "tower_mesh/big_map_table.json"
ATLAS = (256, 256)

# the table (blocks; its middle at x 0.5, z 0.5 of the master cell, the floor at y 0)
CX = CZ = 0.5
HALF, CHAMFER = 6.5, 2.0
TOP0, TOP1 = 0.72, 0.86
PLINTH_IN, KICK = 0.42, 0.12
WELL_R = 6.0
MAP_R = 5.7                 # client/BigMapTableRenderer.MAP_R
RAIL_OUT, RAIL_Y, RAIL_R = 0.16, 0.80, 0.035
PANEL_HW, PANEL_HD, PANEL_TILT = 0.5, 0.28, 18.0
LIP_OUT, LIP_UP = 0.05, 0.006       # the well's steel lip: how far out over the top it reaches and how far it stands up
GLASS_Y = TOP1 - 0.014              # the well's glass
REACH = 6                   # cells either side of the master
Y = np.array((0, 1.0, 0))
LCD = 5                     # client/ConsoleMesh.LCD


def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def octagon(half, cham):
    """The octagon's corners (x, z) about the table's middle, clockwise seen from above from the north edge's west end."""
    h, c = half, half - cham
    return [(-c, -h), (c, -h), (h, -c), (h, c), (c, h), (-c, h), (-h, c), (-h, -c)]


def inset(half, cham, d):
    """The octagon d further in (every edge, the chamfers' too, moves d in; a negative d, out)."""
    return half - d, cham - d * (2 - math.sqrt(2))


def inside(poly, x, z):
    """Whether (x, z) (about the middle) is inside a convex octagon."""
    n = len(poly)
    for k in range(n):
        (ax, az), (bx, bz) = poly[k], poly[(k + 1) % n]
        if (bx - ax) * (z - az) - (bz - az) * (x - ax) < -1e-9:
            return False
    return True


# ------------------------------------------------------------------ the mesh

def prism(m, poly, y0, y1, mat, top=True, bottom=False, lit=False, sides=True):
    n = len(poly)
    P = [(CX + x, CZ + z) for x, z in poly]
    if sides:
        for k in range(n):
            (ax, az), (bx, bz) = P[k], P[(k + 1) % n]
            out = (bz - az, 0, -(bx - ax))
            m.oquad([(ax, y0, az), (bx, y0, bz), (bx, y1, bz), (ax, y1, az)], out, mat, lit=lit)
    for want, y, on in (((0, 1, 0), y1, top), ((0, -1, 0), y0, bottom)):
        if not on:
            continue
        # the octagon as a middle band and two trapezoids (no degenerate corners)
        (a, b, c, d, e, f, g, h) = P
        rows = [(g, h, c, d), (h, a, b, c), (g, d, e, f)]
        for p0, p1, p2, p3 in rows:
            m.oquad([(p0[0], y, p0[1]), (p1[0], y, p1[1]), (p2[0], y, p2[1]), (p3[0], y, p3[1])], want, mat, lit=lit)


def tube(m, a, b, r, mat, sides=8):
    a, b = np.array(a, float), np.array(b, float)
    w = unit(b - a)
    u = unit(np.cross(w, (0, 1, 0) if abs(w[1]) < 0.9 else (1, 0, 0)))
    v = np.cross(w, u)
    ring = [u * math.cos(2 * math.pi * i / sides) * r + v * math.sin(2 * math.pi * i / sides) * r for i in range(sides)]
    for i in range(sides):
        p, q = ring[i], ring[(i + 1) % sides]
        m.oquad([tuple(a + p), tuple(b + p), tuple(b + q), tuple(a + q)], tuple((p + q) / 2), mat)


def ring(m, y, r0, r1, mat, n=96, lit=False, want=(0, 1, 0)):
    for k in range(n):
        a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        pts = [(CX + math.sin(a) * rr, y, CZ - math.cos(a) * rr) for a, rr in ((a0, r0), (a1, r0), (a1, r1), (a0, r1))]
        m.oquad(pts, want, mat, lit=lit)


def wall(m, r, y0, y1, mat, out=True, n=96):
    """A round wall (radius r about the middle) from y0 to y1 facing out or in."""
    for k in range(n):
        a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        pts = [(CX + math.sin(a) * r, y, CZ - math.cos(a) * r) for a, y in ((a0, y0), (a1, y0), (a1, y1), (a0, y1))]
        am = (a0 + a1) / 2
        d = (math.sin(am), 0, -math.cos(am))
        m.oquad(pts, d if out else tuple(-v for v in d), mat)


def edge_r(poly, a):
    """Where a ray from the middle at angle a (clockwise from north) meets the octagon."""
    dx, dz = math.sin(a), -math.cos(a)
    best = 1e9
    for i in range(8):
        (ax, az), (bx, bz) = poly[i], poly[(i + 1) % 8]
        ex, ez = bx - ax, bz - az
        den = dx * ez - dz * ex
        if abs(den) < 1e-9:
            continue
        t = (ax * ez - az * ex) / den
        u = (ax * dz - az * dx) / den
        if t > 0 and -1e-9 <= u <= 1 + 1e-9:
            best = min(best, t)
    return best


def panels():
    """The four touch panels' corners (tl, tr, br, bl as their reader sees them) and their 'up' (the screen's normal)."""
    out = []
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        out_dir = np.array((math.sin(a), 0, -math.cos(a)))
        side = np.array((math.cos(a), 0, math.sin(a)))       # the reader faces in: their left is +side
        chamfer_mid = (HALF + HALF - CHAMFER) / 2 * math.sqrt(2)
        mid = np.array((CX, TOP1, CZ)) + out_dir * (chamfer_mid - 1.05)
        t = math.radians(PANEL_TILT)
        up = Y * math.cos(t) + out_dir * math.sin(t)
        back = mid - out_dir * PANEL_HD * math.cos(t) + Y * (PANEL_HD * math.sin(t) + 0.012)
        front = mid + out_dir * PANEL_HD * math.cos(t) + Y * 0.012
        corners = [back + side * PANEL_HW, back - side * PANEL_HW, front - side * PANEL_HW, front + side * PANEL_HW]
        out.append((corners, up, out_dir, side, back, front))
    return out


def build():
    m = TCC.Mesh()
    top = octagon(HALF, CHAMFER)
    plinth = octagon(*inset(HALF, CHAMFER, PLINTH_IN))
    kick = octagon(*inset(HALF, CHAMFER, PLINTH_IN + 0.06))
    lit_band = octagon(*inset(HALF, CHAMFER, PLINTH_IN + 0.02))
    prism(m, kick, 0.0, KICK, "kick", top=False)
    prism(m, plinth, KICK, TOP0, "plinth", top=False)
    prism(m, lit_band, KICK - 0.035, KICK - 0.012, "kicklight", top=False, lit=True)
    # the top: its underside where it overhangs the plinth, the brushed edge band
    prism(m, top, TOP0, TOP1 - 0.03, "band", top=False, bottom=True)
    prism(m, octagon(HALF - 0.01, CHAMFER - 0.004), TOP1 - 0.03, TOP1, "band", top=False)
    # the top surface round the well: quads from the well's lip out to the octagon's edge
    n = 128
    r_in = WELL_R + LIP_OUT
    for k in range(n):
        a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        r0, r1 = edge_r(top, a0), edge_r(top, a1)
        p = [(CX + math.sin(a0) * r_in, TOP1, CZ - math.cos(a0) * r_in), (CX + math.sin(a0) * r0, TOP1, CZ - math.cos(a0) * r0),
             (CX + math.sin(a1) * r1, TOP1, CZ - math.cos(a1) * r1), (CX + math.sin(a1) * r_in, TOP1, CZ - math.cos(a1) * r_in)]
        m.oquad(p, Y, "top")
    # the well: a raised steel lip round it, the glass set down in it, the projector's emitter grid seen through the glass
    lip_y = TOP1 + LIP_UP
    ring(m, lip_y, WELL_R, r_in, "steel")
    wall(m, r_in, TOP1, lip_y, "steel", out=True)
    wall(m, WELL_R, GLASS_Y, lip_y, "steel", out=False)
    for q in range(8):
        ring(m, GLASS_Y, WELL_R * q / 8 + (1e-3 if q == 0 else 0), WELL_R * (q + 1) / 8, "glass", n=64)
    span = math.floor(WELL_R / 0.5) * 0.5 - 0.25
    ey = GLASS_Y + 0.006
    for gx in np.arange(-span, span + 0.01, 0.5):
        for gz in np.arange(-span, span + 0.01, 0.5):
            if math.hypot(gx, gz) < WELL_R - 0.2:
                x, z = CX + gx, CZ + gz
                m.oquad([(x - 0.03, ey, z - 0.03), (x + 0.03, ey, z - 0.03), (x + 0.03, ey, z + 0.03), (x - 0.03, ey, z + 0.03)], Y,
                        "emitter", lit=True)
    # the lean rail on its brackets
    rail = octagon(*inset(HALF, CHAMFER, -RAIL_OUT))
    pts = [np.array((CX + x, RAIL_Y, CZ + z)) for x, z in rail]
    for k in range(8):
        a, b = pts[k], pts[(k + 1) % 8]
        tube(m, a, b, RAIL_R, "steel")
        L = np.linalg.norm(b - a)
        for f in np.arange(0.0, 1.0, 1.0 / max(1, round(L / 2.2))):
            p = a + (b - a) * f
            inward = np.array((CX, p[1], CZ)) - p
            inward[1] = 0
            inward = unit(inward)
            tube(m, p, p + inward * (RAIL_OUT + 0.02), 0.018, "steel", sides=6)
    # the touch panels: a bezel set in the top, the screen (a live face) on it
    for k, (corners, up, out_dir, side, back, front) in enumerate(panels()):
        e = 0.035
        bz = [corners[0] + side * e - out_dir * e, corners[1] - side * e - out_dir * e, corners[2] - side * e + out_dir * e,
              corners[3] + side * e + out_dir * e]
        bz = [c - up * 0.004 for c in bz]
        m.oquad([tuple(c) for c in bz], tuple(up), "bezel")
        for i in range(4):
            p0, p1 = bz[i], bz[(i + 1) % 4]
            q0, q1 = np.array((p0[0], TOP1, p0[2])), np.array((p1[0], TOP1, p1[2]))
            mid = (p0 + p1) / 2 - np.array((CX, TOP1, CZ))
            mid[1] = 0
            m.oquad([tuple(p0), tuple(p1), tuple(q1), tuple(q0)], tuple(mid), "bezel")
        # the screen is a live face: the renderer draws its page there (nothing of the mesh under it but the bezel)
        m.live.append((LCD, k, [tuple(float(v) for v in c) for c in corners]))
    return m


def tiles():
    return {"plinth": TCC._noise((34, 37, 41), 2.5, 811, size=64), "kick": TCC._noise((22, 24, 27), 2, 812, size=32),
            "top": TCC._noise((30, 33, 37), 2.0, 813, size=64), "band": TCC._noise((132, 138, 146), 3.0, 814, "h", size=64),
            "steel": TCC._noise((176, 182, 190), 3.0, 815, size=32), "glass": TCC._noise((14, 17, 21), 1.5, 816, size=32),
            "bezel": TCC._noise((52, 56, 62), 2.0, 817, size=32),
            "emitter": TCC._solid((150, 90, 30)), "kicklight": TCC._solid((255, 170, 70))}


NOISE = {"plinth", "kick", "top", "band", "steel", "glass", "bezel"}


class Atlas:
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


_seed = [0]


def _uvs(atlas, q):
    mat = q["mat"]
    if mat in NOISE:
        P_ = q["P"]
        sw = min(np.linalg.norm(P_[1] - P_[0]) * 0.5, 0.9)
        sh = min(np.linalg.norm(P_[3] - P_[0]) * 0.5, 0.9)
        _seed[0] = (_seed[0] * 1103515245 + 12345) & 0x7FFFFFFF
        ou = (_seed[0] % 997) / 997 * (1 - sw)
        ov = ((_seed[0] // 997) % 991) / 991 * (1 - sh)
        f = [(ou, ov), (ou + sw, ov), (ou + sw, ov + sh), (ou, ov + sh)]
    else:
        f = [(0.25, 0.25), (0.75, 0.25), (0.75, 0.75), (0.25, 0.75)]
    return [atlas.uv(mat, u, v) for u, v in f]


def bake(m, reach=0.45, rays=20, strength=0.55):
    """Ambient occlusion at every corner, traced exactly against the mesh's triangles and the floor."""
    occluders = [q["P"] for q in m.quads] + [np.array([(-8, 0, -8), (9, 0, -8), (9, 0, 9), (-8, 0, 9)], float)]
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
    todo = [q for q in m.quads if q["ao"]]
    for q in m.quads:
        q["shade"] = [1.0] * 4
    corners = np.concatenate([q["P"] for q in todo])
    normals = np.concatenate([np.repeat(q["n"][None], 4, 0) for q in todo])
    key = np.concatenate([np.round(corners / 1e-3), np.round(normals * 50)], 1).astype(np.int64)
    uk, inv = np.unique(key, axis=0, return_inverse=True)
    inv = inv.reshape(-1)
    up_p = np.zeros((len(uk), 3))
    up_n = np.zeros((len(uk), 3))
    up_p[inv] = corners
    up_n[inv] = normals
    # only triangles near a corner can occlude it (reach): bucket them
    lo_t = np.minimum(np.minimum(V0, V0 + E1), V0 + E2)
    hi_t = np.maximum(np.maximum(V0, V0 + E1), V0 + E2)
    ao = np.ones(len(uk))
    for i in range(len(uk)):
        p, n = up_p[i], up_n[i]
        near = np.all((lo_t <= p + reach) & (hi_t >= p - reach), axis=1)
        if not near.any():
            continue
        v0, e1, e2 = V0[near], E1[near], E2[near]
        a_ = np.array([0, 1.0, 0]) if abs(n[1]) < 0.9 else np.array([1.0, 0, 0])
        u = unit(np.cross(n, a_))
        v = np.cross(n, u)
        D = local[:, 0:1] * u + local[:, 1:2] * v + local[:, 2:3] * n
        O = p + n * 2e-3
        pv = np.cross(D[:, None, :], e2[None])
        det = np.einsum("rtk,tk->rt", pv, e1)
        ok = np.abs(det) > 1e-12
        inv_det = np.where(ok, 1.0 / np.where(ok, det, 1.0), 0.0)
        tv = O - v0
        uu = np.einsum("rtk,tk->rt", pv, tv) * inv_det
        qv = np.cross(tv, e1)
        vv = np.einsum("rk,tk->rt", D, qv) * inv_det
        tt = np.einsum("tk,tk->t", e2, qv)[None, :] * inv_det
        hit = ok & (uu >= 0) & (vv >= 0) & (uu + vv <= 1) & (tt > 1e-3) & (tt < reach)
        dist = np.where(hit, tt, np.inf).min(1)
        w = np.where(np.isfinite(dist), 1 - 0.55 * (dist / reach), 0.0)
        ao[i] = 1 - strength * w.mean()
    ao = np.clip(ao, 0.5, 1.0)
    per = ao[inv].reshape(-1, 4)
    for j, q in enumerate(todo):
        q["shade"] = [float(min(1.0, s * 0.88)) for s in per[j]]


def write_mesh(m, atlas, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    _seed[0] = 0
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
    buf += struct.pack(">i", len(m.live))
    for kind, ident, corners in m.live:
        buf += struct.pack(">bb", kind, ident)
        for p in corners:
            buf += struct.pack(">fff", *p)
    buf += struct.pack(">i", 0)
    for mat in ("steel", "kicklight", "band"):
        u0, v0 = atlas.uv(mat, 0.1, 0.1)
        u1, v1 = atlas.uv(mat, 0.9, 0.9)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


# ------------------------------------------------------------------ the cells and their collision

RES = 8


def footprint():
    """[(cell, boxes)]: every cell the table's body reaches into (the master, its middle, first), its collision boxes in
    pixels (the plinth and the top, in 1/8-block voxels)."""
    top = octagon(HALF, CHAMFER)
    plinth = octagon(*inset(HALF, CHAMFER, PLINTH_IN))
    out = []
    cells = [(dx, dz) for dz in range(-REACH, REACH + 1) for dx in range(-REACH, REACH + 1)]
    cells.sort(key=lambda c: (c != (0, 0), c[1], c[0]))
    for dx, dz in cells:
        cols = {}
        for i in range(RES):
            for k in range(RES):
                x = dx + (i + 0.5) / RES - 0.5          # about the middle (the master cell's middle is 0.5, 0.5)
                z = dz + (k + 0.5) / RES - 0.5
                if inside(top, x, z):
                    cols[(i, k)] = TOP0 if not inside(plinth, x, z) else 0.0
        if not cols:
            continue
        boxes = []
        # the top slab over every covered voxel column; the plinth under it where the plinth is
        for (i, k), y0 in sorted(cols.items()):
            boxes.append([i * 2, round(y0 * 16, 3), k * 2, i * 2 + 2, TOP1 * 16, k * 2 + 2])
        out.append(((dx, 0, dz), _merge(boxes)))
    return out


def _merge(boxes):
    """Boxes with the same y extent merged along x, then along z."""
    rows = {}
    for b in boxes:
        rows.setdefault((b[1], b[4], b[2], b[5]), []).append(b)
    merged = []
    for (y0, y1, z0, z1), bs in rows.items():
        bs.sort(key=lambda b: b[0])
        cur = list(bs[0])
        for b in bs[1:]:
            if b[0] == cur[3]:
                cur[3] = b[3]
            else:
                merged.append(cur)
                cur = list(b)
        merged.append(cur)
    cols = {}
    for b in merged:
        cols.setdefault((b[0], b[3], b[1], b[4]), []).append(b)
    out = []
    for (x0, x1, y0, y1), bs in cols.items():
        bs.sort(key=lambda b: b[2])
        cur = list(bs[0])
        for b in bs[1:]:
            if b[2] == cur[5]:
                cur[5] = b[5]
            else:
                out.append(cur)
                cur = list(b)
        out.append(cur)
    return out


# ------------------------------------------------------------------ the sign over the room's doors

SIGN_Y = (2.72, 13.28)


def sign_models():
    out = {}
    y0, y1 = SIGN_Y
    import aisle_containment as AC
    for p in range(4):
        t = {"case": "jugcraft:block/dhs_case", "trim": "jugcraft:block/acd_alu_dark", "face": f"jugcraft:block/mrs_face_{p}",
             "particle": "jugcraft:block/dhs_case"}
        sides = ("up", "down") + (("west",) if p == 0 else ()) + (("east",) if p == 3 else ())
        e = [AC.element((0, y0, 0), (16, y1, 1.12), "case", sides),
             AC.element((0, y0, 1.12), (16, y0 + 0.48, 1.36), "trim", ("south", "up", "down") + sides[2:]),
             AC.element((0, y1 - 0.48, 1.12), (16, y1, 1.36), "trim", ("south", "up", "down") + sides[2:]),
             AC.element((0, y0 + 0.48, 1.12), (16, y1 - 0.48, 1.2), "face", ("south",), emit=AC.GLOW,
                        uv={"south": [0, 0, 16, round(16 * (y1 - y0 - 0.96) / 16, 4)]})]
        out[f"{SIGN}_{p}"] = {"textures": t, "elements": e}
    return out


def sign_faces():
    """The light box's face, 4 blocks by 0.6 (2048 x 307 px), cut into the four parts' square textures."""
    import aisle_containment as AC
    W, H = 2048, 307
    im = Image.new("RGBA", (W, H), (16, 17, 19, 255))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(AC.FONT_B, 170)
    fs = ImageFont.truetype(AC.FONT_B, 44)
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glow).text((W / 2, 122), "MAP ROOM", font=f, fill=(255, 170, 60, 255), anchor="mm")
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(9)))
    d.text((W / 2, 122), "MAP ROOM", font=f, fill=(255, 196, 110, 255), anchor="mm")
    d.line((230, 228, W - 230, 228), fill=(90, 92, 96, 255), width=4)
    d.text((W / 2, 268), "THE WORLD ROUND THE TOWER   ·   LIVE", font=fs, fill=(226, 228, 230, 255), anchor="mm")
    out = []
    for p in range(4):
        tile = Image.new("RGBA", (512, 512), (16, 17, 19, 255))
        tile.paste(im.crop((p * 512, 0, p * 512 + 512, H)), (0, 0))
        out.append(tile)
    return out


# ------------------------------------------------------------------ the blocks' data

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def write_data(cells):
    j = TCC._json
    j(ASSETS / "models/block" / f"{ID}.json", {"textures": {"particle": "jugcraft:block/carbon_composite_panel"}, "elements": []})
    j(ASSETS / "blockstates" / f"{ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "blockstates" / f"{PART_ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    for item in (ID, SIGN):
        j(ASSETS / "models/item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{item}"}})
        j(ASSETS / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{item}"}})
        j(DATA / "jugcraft/loot_table/blocks" / f"{item}.json", TCC._loot(item))
    j(DATA / "jugcraft/loot_table/blocks" / f"{PART_ID}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{PART_ID}"})
    # No recipe: a hologram, the Drone Tower's own (tower/Holograms, Justin 28 October 2026); apply() removes an old one.
    import advanced_crafting
    advanced_crafting.apply(DATA / "jugcraft/recipe", [ID])
    j(DATA / "jugcraft/recipe" / f"{SIGN}.json", TCC._recipe(["SSS", "GMG"], {
        "S": "#c:plates/steel", "G": "minecraft:glowstone_dust", "M": "minecraft:map"}, SIGN))
    for name, m in sign_models().items():
        j(ASSETS / "models/block" / f"{name}.json", m)
    j(ASSETS / "blockstates" / f"{SIGN}.json", {"variants": {
        f"facing={f},part={p}": {"model": f"jugcraft:block/{SIGN}_{p}", **({"y": y} if y else {})} for f, y in FACINGS.items() for p in range(4)}})
    for p, img in enumerate(sign_faces()):
        img.save(ASSETS / "textures/block" / f"mrs_face_{p}.png", optimize=True)
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{PART_ID}"] = NAME
    lang[f"block.jugcraft.{SIGN}"] = SIGN_NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Big Map Table needs a clear space 13 by 13 blocks"
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in (ID, PART_ID, SIGN):
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")
    j(FOOTPRINT, {"comment": "tools/big_map_table.py: the cells the table covers, the master's (its middle) first, offsets from "
                             "it; each one's collision boxes in pixels of its own cell",
                  "cells": [list(c) for c, _ in cells], "boxes": [b for _, b in cells]})


def write_all():
    m = build()
    cells = footprint()
    atlas = Atlas(tiles())
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    bake(m)
    write_mesh(m, atlas, MESH)
    write_data(cells)
    import item_icons
    for item in (ID, SIGN):
        item_icons.draw(item).save(ASSETS / f"textures/item/{item}.png")
    return m, cells


if __name__ == "__main__":
    m, cells = write_all()
    kinds = [q["kind"] for q in m.quads]
    print(len(m.quads), "quads", {k: kinds.count(k) for k in sorted(set(kinds))}, len(m.live), "live faces;", len(cells), "cells,",
          sum(len(b) for _, b in cells), "boxes")
