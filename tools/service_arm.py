"""The Drone Service Arm: the Tri-Pillar service bay's 6-axis industrial robot (work list item 8, approved by Justin on 22
October 2026 from the take-2 mockup sheet; see claude/jugcraft-tripillar-service-arm.md).

A real 6-axis robot in the tower's own livery (graphite castings, amber joint rings, no maker's look), about 2.1 m of
reach: a steel floor plate with four anchor bolts, a 0.30 riser with a hazard band, the base casting with its cable
connector at the back; the turret (axis 1) with its motor; the shoulder (axis 2) with its gearbox discs and motor; the
upper arm (0.95) with its label; the elbow (axis 3) and its motor; the forearm housing with the wrist motors and the
forearm tube (1.02 to the wrist); the wrist fork and body (axis 5) and the tool flange: a nut runner, a camera with a
ring light. client/ServiceArm draws it: these meshes, each in its own joint's frame, posed by its inverse kinematics
every frame, and the parts that flex between the joints (the gas-spring counterbalance, the dress pack of cables).

Meshes (the console's format, client/ConsoleMesh; textured from textures/entity/service_arm.png), metres:
- service_arm_base: in the base's frame (the cell's middle on the floor, +x the arm's front, the way it faces), with the
  keep-clear square painted round it;
- service_arm_turret: axis 1's frame (J1_Y up, turned by j1 about y), with the shoulder's discs and motor;
- service_arm_upper: axis 2's frame (the shoulder, SH_X forward and SH_Y up of axis 1; leaned by j2 about z), with the
  elbow (L1 up it);
- service_arm_forearm: the forearm's frame (on the upper arm L1 + EL_UP from the shoulder; +y out to the wrist), its
  housing, the tube and the wrist's fork (L2 out);
- service_arm_wrist: axis 5's frame (+y down the tool), the wrist body, the flange, the camera;
- service_arm_runner: the nut runner's frame (0.16 down the tool; it spins about +y), shaft and socket (tip 0.30 down);
- service_arm_ring: the camera's ring light in the wrist's frame (full bright while it scans).
The base mesh's three edge tiles are the tubes' patches: the cable, the gas spring's body, its rod.
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
TEXTURE = ASSETS / "textures/entity/service_arm.png"
MESHES = ASSETS / "tower_mesh"
ID = "drone_service_arm"

# the robot's dimensions (client/ServiceArm keeps the same numbers)
PLATE, RISER = 0.025, 0.30            # the floor plate's thickness, the riser's height over it
BASE_H = 0.22                          # the base casting's height
J1_Y = PLATE + RISER + BASE_H          # where the turret turns
SH_X, SH_Y = 0.16, 0.36                # the shoulder axis: forward of axis 1, over the turret's foot
L1 = 0.95                              # upper arm (shoulder to elbow)
EL_UP = 0.13                           # the forearm's axis on from the elbow along the upper arm
L2 = 1.02                              # forearm axis to the wrist (axis 5)
TOOL = 0.30                            # the wrist to the runner's socket tip
RUNNER = 0.16                          # the wrist to the runner's spindle
RING = (0.085, 0.10 + 0.0905, 0.0)     # the ring light's middle in the wrist's frame
ZONE = 0.85                            # the keep-clear square's half size (client/ServiceArm.ZONE)
SOLID, LIT = 0, 1
ATLAS = (512, 256)

TILES = {
    "graphite": TCC._noise((58, 62, 68), 2.0, 21), "dark": TCC._noise((30, 32, 36), 1.5, 22),
    "steel": TCC._noise((160, 166, 172), 6, 23, "h"), "plate": TCC._noise((96, 100, 106), 5, 24),
    "cable": TCC._noise((24, 24, 26), 2, 25, size=32), "amber": TCC._solid((255, 168, 60)), "black": TCC._solid((14, 14, 16)),
    "lens": TCC._solid((20, 30, 40)), "ring": TCC._solid((246, 250, 255)), "hazard": TCC._solid((236, 190, 40)),
    "label": TCC._solid((236, 236, 230)),
}
NOISE = {"graphite", "dark", "steel", "plate", "cable"}


class Atlas:
    def __init__(self, tiles):
        self.img = Image.new("RGBA", ATLAS, (0, 0, 0, 0))
        self.box = {}
        x = y = shelf = 0
        for name, im in sorted(tiles.items(), key=lambda kv: (-kv[1].height, kv[0])):
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


# ------------------------------------------------------------------ shapes (in the part's own frame)

def _frame(w):
    w = np.asarray(w, float)
    w = w / np.linalg.norm(w)
    u = np.cross(w, (0, 1, 0) if abs(w[1]) < 0.9 else (1, 0, 0))
    u /= np.linalg.norm(u)
    return w, u, np.cross(w, u)


def cyl(m, a, b, r, mat, n=20, caps=True, cap=None):
    A, B = np.asarray(a, float), np.asarray(b, float)
    if np.linalg.norm(B - A) < 1e-9:
        return
    w, u, v = _frame(B - A)
    ring = [(u * math.cos(2 * math.pi * i / n) + v * math.sin(2 * math.pi * i / n)) * r for i in range(n)]
    for i in range(n):
        p, q = ring[i], ring[(i + 1) % n]
        m.oquad([B + q, B + p, A + p, A + q], (p + q) / 2, mat)
    if caps:
        for i in range(n):
            p, q = ring[i], ring[(i + 1) % n]
            m.oquad([B, B + p, B + q, B + q], w, cap or mat)
            m.oquad([A, A + q, A + p, A + p], -w, cap or mat)


def frustum(m, a, b, r0, r1, mat, n=20):
    A, B = np.asarray(a, float), np.asarray(b, float)
    w, u, v = _frame(B - A)
    for i in range(n):
        t0, t1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        p0, p1 = u * math.cos(t0) + v * math.sin(t0), u * math.cos(t1) + v * math.sin(t1)
        m.oquad([B + p1 * r1, B + p0 * r1, A + p0 * r0, A + p1 * r0], (p0 + p1) / 2, mat)
        m.oquad([B, B + p0 * r1, B + p1 * r1, B + p1 * r1], w, mat)
        m.oquad([A, A + p1 * r0, A + p0 * r0, A + p0 * r0], -w, mat)


def rbox(m, lo, hi, mat, bevel=0.0, faces="nsewud"):
    """A box (lo..hi), its four upright edges bevelled by `bevel` (an octagon in x-z extruded along y)."""
    lo, hi = np.array(lo, float), np.array(hi, float)
    if bevel <= 0:
        c, h = (lo + hi) / 2, (hi - lo) / 2
        P = lambda sx, sy, sz: c + h * np.array([sx, sy, sz])  # noqa: E731
        F = {"u": ([P(-1, 1, -1), P(1, 1, -1), P(1, 1, 1), P(-1, 1, 1)], (0, 1, 0)),
             "d": ([P(-1, -1, 1), P(1, -1, 1), P(1, -1, -1), P(-1, -1, -1)], (0, -1, 0)),
             "n": ([P(1, 1, -1), P(-1, 1, -1), P(-1, -1, -1), P(1, -1, -1)], (0, 0, -1)),
             "s": ([P(-1, 1, 1), P(1, 1, 1), P(1, -1, 1), P(-1, -1, 1)], (0, 0, 1)),
             "e": ([P(1, 1, 1), P(1, 1, -1), P(1, -1, -1), P(1, -1, 1)], (1, 0, 0)),
             "w": ([P(-1, 1, -1), P(-1, 1, 1), P(-1, -1, 1), P(-1, -1, -1)], (-1, 0, 0))}
        for f in faces:
            pts, n = F[f]
            m.oquad(pts, n, mat)
        return
    x0, x1, z0, z1, b = lo[0], hi[0], lo[2], hi[2], bevel
    o = [(x0 + b, z0), (x1 - b, z0), (x1, z0 + b), (x1, z1 - b), (x1 - b, z1), (x0 + b, z1), (x0, z1 - b), (x0, z0 + b)]
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    for i in range(8):
        a, c = o[i], o[(i + 1) % 8]
        m.oquad([(a[0], hi[1], a[1]), (c[0], hi[1], c[1]), (c[0], lo[1], c[1]), (a[0], lo[1], a[1])],
                ((a[0] + c[0]) / 2 - cx, 0, (a[1] + c[1]) / 2 - cz), mat)
    for y, s in ((hi[1], 1), (lo[1], -1)):
        for i in range(8):
            a, c = o[i], o[(i + 1) % 8]
            m.oquad([(cx, y, cz), (a[0], y, a[1]), (c[0], y, c[1]), (c[0], y, c[1])], (0, s, 0), mat)


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


# ------------------------------------------------------------------ the parts

def base():
    m = TCC.Mesh()
    rbox(m, (-0.36, 0, -0.36), (0.36, PLATE, 0.36), "plate")
    for sx in (-1, 1):
        for sz in (-1, 1):
            cyl(m, (sx * 0.30, PLATE, sz * 0.30), (sx * 0.30, PLATE + 0.03, sz * 0.30), 0.022, "steel", n=6)
    rbox(m, (-0.25, PLATE, -0.25), (0.25, PLATE + RISER, 0.25), "graphite", bevel=0.04)
    rbox(m, (-0.2505, PLATE + 0.02, -0.2505), (0.2505, PLATE + 0.07, 0.2505), "hazard", bevel=0.0402)
    yb = PLATE + RISER
    frustum(m, (0, yb, 0), (0, yb + BASE_H, 0), 0.27, 0.24, "graphite", n=28)
    # the cable connector at the back, the side away from the work
    rbox(m, (-0.36, yb + 0.03, -0.09), (-0.24, yb + 0.15, 0.09), "dark")
    cyl(m, (-0.36, yb + 0.09, 0), (-0.42, yb + 0.09, 0), 0.045, "black", n=12)
    cyl(m, (0, J1_Y - 0.012, 0), (0, J1_Y, 0), 0.245, "amber", n=28)
    keep_clear(m)
    return m


def keep_clear(m, n=14, w=0.08):
    """The keep-clear square painted on the floor round the base (ZONE either side of its middle): yellow and black
    stripes, 5 mm proud of the floor. While a player stands in it the arm stops (client/ServiceArm.blocked)."""
    y = 0.005
    for (ax, az, bx, bz) in ((-ZONE, -ZONE, ZONE, -ZONE), (-ZONE, ZONE, ZONE, ZONE), (-ZONE, -ZONE, -ZONE, ZONE),
                             (ZONE, -ZONE, ZONE, ZONE)):
        along_x = az == bz
        for i in range(n):
            s0, s1 = i / n, (i + 1) / n
            x0, z0 = ax + (bx - ax) * s0, az + (bz - az) * s0
            x1, z1 = ax + (bx - ax) * s1, az + (bz - az) * s1
            lo = (min(x0, x1) - (0 if along_x else w / 2), min(z0, z1) - (w / 2 if along_x else 0))
            hi = (max(x0, x1) + (0 if along_x else w / 2), max(z0, z1) + (w / 2 if along_x else 0))
            m.oquad([(lo[0], y, lo[1]), (hi[0], y, lo[1]), (hi[0], y, hi[1]), (lo[0], y, hi[1])], (0, 1, 0),
                    "hazard" if i % 2 == 0 else "black")


def turret():
    m = TCC.Mesh()
    frustum(m, (0, 0, 0), (0, 0.16, 0), 0.24, 0.22, "graphite", n=28)
    rbox(m, (-0.10, 0.16, -0.21), (SH_X + 0.16, SH_Y + 0.12, 0.21), "graphite", bevel=0.05)
    cyl(m, (-0.16, 0.10, 0.0), (-0.16, 0.36, 0.0), 0.075, "dark", n=16, cap="black")     # the axis-1 motor
    cyl(m, (-0.16, 0.36, 0.0), (-0.16, 0.39, 0.0), 0.05, "black", n=12)
    # the shoulder (axis 2): gearbox discs either side, its motor on the left
    s = np.array([SH_X, SH_Y, 0])
    cyl(m, s + (0, 0, -0.24), s + (0, 0, -0.20), 0.17, "amber", n=28)
    cyl(m, s + (0, 0, -0.20), s + (0, 0, 0.20), 0.165, "graphite", n=28)
    cyl(m, s + (0, 0, 0.20), s + (0, 0, 0.24), 0.17, "dark", n=28)
    cyl(m, s + (0, 0, -0.24), s + (0, 0, -0.40), 0.085, "dark", n=16, cap="black")
    # the counterbalance's anchor pin at the back of the turret (client/ServiceArm CB_A)
    cyl(m, (-0.06, SH_Y + 0.02, 0.24), (-0.06, SH_Y + 0.02, 0.29), 0.04, "steel", n=10)
    return m


def upper():
    m = TCC.Mesh()
    rbox(m, (-0.12, 0.05, -0.15), (0.12, L1 - 0.05, 0.15), "graphite", bevel=0.05)
    rbox(m, (-0.121, 0.42, 0.151), (0.121, 0.52, 0.152), "label", faces="s")
    # the elbow (axis 3) and its motor
    e = np.array([0, L1, 0])
    cyl(m, e + (0, 0, -0.20), e + (0, 0, 0.20), 0.135, "graphite", n=24)
    cyl(m, e + (0, 0, -0.205), e + (0, 0, -0.17), 0.14, "amber", n=24)
    cyl(m, e + (0, 0, 0.20), e + (0, 0, 0.33), 0.07, "dark", n=14, cap="black")
    # the dress pack's clamps (client/ServiceArm C2, C3), round the cable along the arm
    for c in ((0.0, 0.25, 0.20), (0.0, 0.75, 0.20)):
        c = np.array(c)
        cyl(m, c - (0, 0.02, 0), c + (0, 0.02, 0), 0.036, "dark", n=10)
    return m


def forearm():
    m = TCC.Mesh()
    rbox(m, (-0.15, -0.30, -0.14), (0.15, 0.10, 0.14), "graphite", bevel=0.05)          # the housing
    for zz in (-0.075, 0.075):
        cyl(m, (0, -0.30, zz), (0, -0.44, zz), 0.055, "dark", n=12, cap="black")      # the wrist motors
    cyl(m, (0, 0.10, 0), (0, 0.13, 0), 0.105, "amber", n=20)
    frustum(m, (0, 0.13, 0), (0, L2 - 0.07, 0), 0.10, 0.075, "graphite", n=20)      # the tube
    for zz in (-0.085, 0.085):                                                          # the wrist's fork
        rbox(m, (-0.06, L2 - 0.09, zz - 0.022), (0.06, L2 + 0.05, zz + 0.022), "graphite", bevel=0.015)
    return m


def wrist():
    m = TCC.Mesh()
    cyl(m, (0, 0, -0.063), (0, 0, 0.063), 0.06, "dark", n=16)
    cyl(m, (0, 0.0, 0), (0, 0.10, 0), 0.055, "graphite", n=16)
    # the flange (axis 6), the runner's gearbox, the camera beside it
    cyl(m, (0, 0.10, 0), (0, 0.12, 0), 0.05, "amber", n=16)
    rbox(m, (-0.06, 0.12, -0.05), (0.06, 0.16, 0.05), "dark", bevel=0.015)
    cyl(m, (0.085, 0.13, 0), (0.085, 0.19, 0), 0.032, "dark", n=14, cap="lens")
    return m


def runner():
    m = TCC.Mesh()
    cyl(m, (0, 0, 0), (0, 0.11, 0), 0.032, "steel", n=12)
    cyl(m, (0, 0.11, 0), (0, TOOL - RUNNER, 0), 0.020, "black", n=6)
    return m


def ring():
    m = TCC.Mesh()
    c = np.array(RING) + (0, 0.0005, 0)
    for i in range(16):
        a0, a1 = 2 * math.pi * i / 16, 2 * math.pi * (i + 1) / 16
        P = lambda r, a: c + (r * math.cos(a), 0, r * math.sin(a))  # noqa: E731
        m.oquad([P(0.030, a0), P(0.030, a1), P(0.022, a1), P(0.022, a0)], (0, 1, 0), "ring", lit=True)
    return m


PARTS = {"base": base, "turret": turret, "upper": upper, "forearm": forearm, "wrist": wrist, "runner": runner, "ring": ring}


# ------------------------------------------------------------------ writing

def _uvs(atlas, q, seed):
    mat = q["mat"]
    if mat in NOISE:
        P_ = q["P"]
        sw = min(np.linalg.norm(P_[1] - P_[0]) * 2.0, 0.9)
        sh = min(np.linalg.norm(P_[3] - P_[0]) * 2.0, 0.9)
        seed[0] = (seed[0] * 1103515245 + 12345) & 0x7FFFFFFF
        s = seed[0]
        ou = (s % 997) / 997 * (1 - sw)
        ov = ((s // 997) % 991) / 991 * (1 - sh)
        f = [(ou, ov), (ou + sw, ov), (ou + sw, ov + sh), (ou, ov + sh)]
    else:
        f = [(0.25, 0.25), (0.75, 0.25), (0.75, 0.75), (0.25, 0.75)]
    return [atlas.uv(mat, u, v) for u, v in f]


def write_mesh(m, atlas, path, tiles=("cable", "dark", "steel")):
    path.parent.mkdir(parents=True, exist_ok=True)
    seed = [0]
    buf = bytearray(struct.pack(">iii", TCC.MAGIC, 1, len(m.quads)))
    for q in m.quads:
        P_ = q["P"]
        uv = _uvs(atlas, q, seed)
        nb = [int(round(max(-1, min(1, v)) * 127)) for v in q["n"]]
        sh = q.get("shade", [1.0] * 4)
        buf += struct.pack(">b", q["kind"])
        for k in (0, 3, 2, 1):
            buf += struct.pack(">fffbbbffB", P_[k][0], P_[k][1], P_[k][2], nb[0], nb[1], nb[2], uv[k][0], uv[k][1],
                               int(round(255 * max(0.0, min(1.0, sh[k])))))
    buf += struct.pack(">i", 0)
    buf += struct.pack(">i", 0)
    for mat in tiles:
        u0, v0 = atlas.uv(mat, 0.1, 0.1)
        u1, v1 = atlas.uv(mat, 0.9, 0.9)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


def write_all():
    atlas = Atlas(TILES)
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    out = {}
    for name, fn in PARTS.items():
        m = fn()
        if name != "ring":
            # each part's own shade: the base on the floor, the rest away from it
            TCC.bake(m, floor_y=0.0 if name == "base" else -50.0, res=1 / 64, reach=0.25, rays=16)
        write_mesh(m, atlas, MESHES / f"service_arm_{name}.bin.gz")
        out[name] = m
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return out


if __name__ == "__main__":
    parts = write_all()
    print({k: len(m.quads) for k, m in parts.items()})
