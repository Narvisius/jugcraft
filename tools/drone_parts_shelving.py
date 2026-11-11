"""The Drone Parts Shelving: the Tri-Pillar workshop's parts wall (work list item 9, approved by Justin on 22 October 2026
from the take-2 mockup sheet; see claude/jugcraft-tripillar-parts-shelving.md). It replaces the two warehouse racks
behind the service bay.

Industrial longspan shelving the way a drone operator's workshop has it: graphite powder-coated perforated uprights in
three frames (two bays of 1.4 m) with zig-zag bracing on the end frames, amber step beams with the bay labels in clear
holders, galvanised wire-mesh decks, a pegboard back panel; 2.94 wide, 0.60 deep, 2.30 high, five levels. Two
stockings of it (the STOCK property):
- a: carbon arms in a tray, frame-kit cartons and landing skids; LiPo packs charging in an open steel LiPo safe with
  their leads to a four-channel balance charger, and eight smart packs standing in their battery station; motors in
  pick-and-pluck foam and hopper bins of hardware; props in a comb rack and the T4's fan rotors on posts; cartons (one
  opened) and the tool-kit and soldering cases on top; a 6 kg extinguisher on a plate across the east end frame with
  the fire-point sign over it;
- b: big carbon booms, the T4's fan ducts, gimbal cases, big motors in foam, long props, cartons.
The parts are parts_shelving_parts.py, their prints parts_shelving_art.py. The battery gauges, the station's slot LEDs
and the two screens are live faces client/DronePartsShelvingRenderer animates.

One item places it whole (tower/DronePartsShelvingBlock.java): the master is the middle cell at the bottom, its other 8
cells are Drone Parts Shelving Parts (no item) that know their offset from it. The master's `facing` is its front.

Frame of the meshes (tower_mesh/drone_parts_shelving_<stock>.bin.gz, the console's format, client/ConsoleMesh,
textured from textures/entity/drone_parts_shelving.png): the master cell's middle at x 0, z 0, the floor at y 0, the
front towards +z (south); the back of the shelving on the cell's north edge (z -0.5). Quad kinds: 0 shaded by the
world, 1 full bright, 2 translucent (the label holders). Live faces: kind 8 a pack's gauge LED (id pack * 4 + LED),
9 a station slot's LED (id slot), 10 the station's screen, 11 the charger's screen; corners tl tr br bl as the user
sees them. The mesh's first edge tile is a white patch (the live faces are drawn from it).
"""
from __future__ import annotations

import gzip
import hashlib
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image

import tower_control_console as TCC
import parts_shelving_art as T
import parts_shelving_parts as P

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
ID = "drone_parts_shelving"
PART_ID = "drone_parts_shelving_part"
NAME = "Drone Parts Shelving"
TEXTURE = ASSETS / "textures/entity/drone_parts_shelving.png"
STOCKS = ("a", "b")
SOLID, LIT, GLASS = 0, 1, 2
ATLAS_W = 4096

W2 = 1.47                       # half width
ZB, ZF = -0.46, 0.14            # the back and front of the decks
UPR = (0.06, 0.04)              # upright section (x, z)
XS = (-1.44, 0.0, 1.44)         # the three frames
DECKS = (0.10, 0.56, 1.02, 1.48, 1.94)
TOP = 2.30
BEAM_H = 0.055


# ------------------------------------------------------------------ the mesh

class Mesh:
    """Quads with their own textures (numpy RGBA), uvs in texture repeats x16 (as the mockups' meshes), kinds, a baked
    shade per corner; live faces."""

    def __init__(self):
        self.quads = []
        self.live = []
        self.ids = {}

    def quad(self, pts, tex, uvs=None, lit=False, translucent=None, shade=None, ao=True):
        P_ = np.asarray(pts, float)
        n = np.zeros(3)
        for i in range(4):
            a, b = P_[i], P_[(i + 1) % 4]
            n += np.array([(a[1] - b[1]) * (a[2] + b[2]), (a[2] - b[2]) * (a[0] + b[0]), (a[0] - b[0]) * (a[1] + b[1])])
        n = -n
        ln = np.linalg.norm(n)
        if ln < 1e-12:
            return
        if uvs is None:
            uvs = [(0, 0), (16, 0), (16, 16), (0, 16)]
        glass = translucent if translucent is not None else bool(tex[..., 3].max() < 0.9)
        kind = LIT if lit else (GLASS if glass else SOLID)
        self.quads.append(dict(P=P_, n=n / ln, uv=np.asarray(uvs, float) / 16, tex=tex, kind=kind, ao=ao and kind == SOLID))

    def oquad(self, pts, want, tex, uvs=None, lit=False, ao=True):
        p = np.asarray(pts, float)
        n = np.cross(p[3] - p[0], p[1] - p[0])
        if uvs is None:
            uvs = [(0, 0), (16, 0), (16, 16), (0, 16)]
        if n @ np.asarray(want, float) < 0:
            p = p[[1, 0, 3, 2]]
            uvs = [uvs[1], uvs[0], uvs[3], uvs[2]]
        self.quad(p, tex, uvs, lit, ao=ao)

    def box(self, lo, hi, tex, lit=False, faces="nsewud", scale=1.0):
        x0, y0, z0 = lo
        x1, y1, z1 = hi

        def uv(w, h):
            return [(0, 0), (16 * w / scale, 0), (16 * w / scale, 16 * h / scale), (0, 16 * h / scale)]
        F = {"u": ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], uv(x1 - x0, z1 - z0)),
             "d": ([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], uv(x1 - x0, z1 - z0)),
             "n": ([(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], uv(x1 - x0, y1 - y0)),
             "s": ([(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)], uv(x1 - x0, y1 - y0)),
             "e": ([(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)], uv(z1 - z0, y1 - y0)),
             "w": ([(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], uv(z1 - z0, y1 - y0))}
        for f in faces:
            pts, u = F[f]
            self.quad(pts, tex, u, lit)

    def obox(self, c, ax, ay, hx, hy, hz, tex, lit=False, faces="nsewud"):
        c = np.asarray(c, float)
        ax = np.asarray(ax, float) / np.linalg.norm(ax)
        ay = np.asarray(ay, float) / np.linalg.norm(ay)
        az = np.cross(ax, ay)
        X, Y, Z = ax * hx, ay * hy, az * hz
        Q = lambda sx, sy, sz: c + X * sx + Y * sy + Z * sz  # noqa: E731
        F = {"u": [Q(-1, 1, -1), Q(1, 1, -1), Q(1, 1, 1), Q(-1, 1, 1)], "d": [Q(-1, -1, 1), Q(1, -1, 1), Q(1, -1, -1), Q(-1, -1, -1)],
             "n": [Q(1, 1, -1), Q(-1, 1, -1), Q(-1, -1, -1), Q(1, -1, -1)], "s": [Q(-1, 1, 1), Q(1, 1, 1), Q(1, -1, 1), Q(-1, -1, 1)],
             "e": [Q(1, 1, 1), Q(1, 1, -1), Q(1, -1, -1), Q(1, -1, 1)], "w": [Q(-1, 1, -1), Q(-1, 1, 1), Q(-1, -1, 1), Q(-1, -1, -1)]}
        for f in faces:
            self.quad(F[f], tex, None, lit)

    def next_id(self, key):
        n = self.ids.get(key, 0)
        self.ids[key] = n + 1
        return n

    def live_quad(self, kind, ident, pts):
        self.live.append((kind, ident, [tuple(float(v) for v in p) for p in pts]))

    def live_panel(self, kind, ident, tl, right, down):
        tl, right, down = (np.asarray(v, float) for v in (tl, right, down))
        self.live_quad(kind, ident, [tl, tl + right, tl + right + down, tl + down])

    def merge(self, other, R, t):
        """Another mesh's quads and live faces turned by R and moved by t."""
        R, t = np.asarray(R, float), np.asarray(t, float)
        for q in other.quads:
            q = dict(q)
            q["P"] = q["P"] @ R.T + t
            q["n"] = R @ q["n"]
            self.quads.append(q)
        for kind, ident, pts in other.live:
            self.live_quad(kind, ident, [np.asarray(p) @ R.T + t for p in pts])


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


# ------------------------------------------------------------------ the frame's surfaces

def _perforated(size=64):
    a = P.noise(size, (44, 47, 52), 2.5, 41)
    for y in range(4, size, 10):
        for x in range(size // 2 - 3, size // 2 + 3):
            a[y:y + 5, x, :3] = (14 / 255, 15 / 255, 17 / 255)
    return a


def _mesh_deck(size=64, cell=8):
    a = np.zeros((size, size, 4), np.float32)
    c = np.array([178, 184, 190], np.float32) / 255
    for i in range(0, size, cell):
        a[i:i + 1, :, :3] = c
        a[i:i + 1, :, 3] = 1
        a[:, i:i + 1, :3] = c * 0.92
        a[:, i:i + 1, 3] = 1
    return a


def _pegboard(size=64):
    a = P.noise(size, (52, 55, 60), 2.0, 42)
    for y in range(4, size, 8):
        for x in range(4, size, 8):
            a[y:y + 2, x:x + 2, :3] = (10 / 255, 10 / 255, 12 / 255)
    return a


TX = {"upright": _perforated(), "beam": P.noise(32, (232, 146, 44), 4, 43), "deck": _mesh_deck(), "back": _pegboard(),
      "galv": P.noise(32, (176, 182, 188), 8, 45), "black": P.solid((18, 18, 20)), "carbon": P.noise(32, (26, 27, 30), 2.5, 46),
      "carbon_sheen": P.noise(32, (58, 61, 66), 4, 47, True), "duct": P.noise(32, (62, 66, 72), 3, 58), "rim_gold": P.solid((222, 176, 84)),
      "rim_white": P.solid((226, 228, 230)), "steel": P.noise(32, (150, 156, 162), 7, 44, True), "hub": P.noise(32, (60, 64, 70), 3, 57),
      "white": P.solid((236, 236, 230))}


def frame(m):
    ux, uz = UPR
    for x in XS:
        for z in (ZB + uz / 2, ZF - uz / 2):
            m.box((x - ux / 2, 0.0, z - uz / 2), (x + ux / 2, TOP, z + uz / 2), TX["upright"], scale=0.25)
            m.box((x - 0.05, 0.0, z - 0.04), (x + 0.05, 0.006, z + 0.04), TX["galv"], faces="nsewu", scale=0.5)
            m.box((x - ux / 2 - 0.002, TOP - 0.01, z - uz / 2 - 0.002), (x + ux / 2 + 0.002, TOP, z + uz / 2 + 0.002), TX["black"])
        ys = np.linspace(0.12, TOP - 0.12, 6)
        for i in range(len(ys) - 1):
            za, zb = (ZB + uz, ZF - uz) if i % 2 == 0 else (ZF - uz, ZB + uz)
            a, b = np.array([x, ys[i], za]), np.array([x, ys[i + 1], zb])
            d = b - a
            m.obox(tuple((a + b) / 2), tuple(d), (1, 0, 0), np.linalg.norm(d) / 2, 0.012, 0.0025, TX["upright"])
    for y in DECKS:
        for (xa, xb) in ((XS[0], XS[1]), (XS[1], XS[2])):
            x0, x1 = xa + ux / 2, xb - ux / 2
            m.box((x0, y - BEAM_H, ZF - 0.042), (x1, y, ZF), TX["beam"], scale=0.5)
            m.box((x0, y - BEAM_H, ZB), (x1, y, ZB + 0.042), TX["beam"], scale=0.5)
            m.box((x0, y - 0.012, ZB + 0.042), (x1, y - 0.004, ZF - 0.042), TX["galv"], faces="d", scale=0.5)
            s = 6.0
            uv = [(0, 0), ((x1 - x0) * s * 16, 0), ((x1 - x0) * s * 16, (ZF - ZB) * s * 16), (0, (ZF - ZB) * s * 16)]
            for up in (True, False):
                p = np.asarray([(x0, y, ZB + 0.04), (x1, y, ZB + 0.04), (x1, y, ZF - 0.04), (x0, y, ZF - 0.04)])
                n = np.cross(p[3] - p[0], p[1] - p[0])
                if (n[1] > 0) != up:
                    p = p[[1, 0, 3, 2]]
                m.quad(p, TX["deck"], uv, translucent=False)
    m.box((-W2 + 0.04, DECKS[0], ZB - 0.012), (W2 - 0.04, TOP - 0.05, ZB - 0.004), TX["back"], faces="sn", scale=0.5)


RED, BLUE, GREEN, GREY, AMBER = (196, 60, 52), (40, 96, 190), (40, 150, 80), (90, 92, 98), (214, 128, 30)
LABELS = {
    "a": [[("A-01", "FRAMES  T1-T3  CARBON ARMS", GREY), ("A-02", "FRAME KITS  LANDING SKIDS", GREY)],
          [("A-03", "LIPO  T1-T2  CHARGE IN SAFE", GREEN), ("A-04", "SMART PACKS  T3-T5", GREEN)],
          [("A-05", "MOTORS  2810 / 3115", BLUE), ("A-06", "HARDWARE  ESC  CONNECTORS", AMBER)],
          [("A-07", "PROPS  T1-T3  CW / CCW", RED), ("A-08", "T4 FAN ROTORS  9-BLADE", RED)],
          [("A-09", "SPARES", GREY), ("A-10", "TOOL KITS", GREY)]],
    "b": [[("B-01", "BOOMS  T4-T6  40 MM", GREY), ("B-02", "BOOMS  T4-T6  40 MM", GREY)],
          [("B-03", "FAN DUCTS  T4", GREY), ("B-04", "GIMBALS  CAMERAS", GREY)],
          [("B-05", "MOTORS  4120 / 5010", BLUE), ("B-06", "MOTORS  6215", BLUE)],
          [("B-07", "PROPS  T5-T6  24-28 IN", RED), ("B-08", "PROPS  T5-T6  24-28 IN", RED)],
          [("B-09", "SPARES", GREY), ("B-10", "SPARES", GREY)]],
}


def labels(m, texts):
    """Each bay's label in a clear C-channel holder clipped to the step beam."""
    for li, y in enumerate(DECKS):
        for bi, (xa, xb) in enumerate(((XS[0], XS[1]), (XS[1], XS[2]))):
            loc, desc, col = texts[li][bi]
            x0, x1 = xa + 0.10, xa + 0.10 + 0.62
            yt, yb = y - 0.010, y - 0.046
            P.panel(m, (x0, yt, ZF + 0.0015), (x1 - x0, 0, 0), (0, yb - yt, 0), T.beam_label(loc, desc, li * 7 + bi, col))
            for (ya, yb_) in ((yt, yt + 0.004), (yb - 0.004, yb)):
                P.box(m, (x0 - 0.004, ya, ZF), (x1 + 0.004, yb_, ZF + 0.0045), P.M["clear"], faces="su")
            P.panel(m, (x0 - 0.004, yt, ZF + 0.0045), (x1 - x0 + 0.008, 0, 0), (0, yb - yt, 0), P.M["clear"])


# ------------------------------------------------------------------ props, rotors, skids

def skid(m, x0, x1, y, z):
    pts = [(x0, y + 0.06, z), (x0 + 0.04, y + 0.015, z)] + [(x, y + 0.015, z) for x in np.linspace(x0 + 0.08, x1 - 0.08, 4)] + \
          [(x1 - 0.04, y + 0.015, z), (x1, y + 0.06, z)]
    P.tube(m, pts, 0.012, P.M["carbon"])
    for xs in (x0 + 0.25, x1 - 0.25):
        P.tube(m, [(xs, y + 0.015, z), (xs + 0.05, y + 0.12, z - 0.05)], 0.010, P.M["carbon"])


def comb_rack(m, x0, x1, y, z=-0.20):
    P.box(m, (x0, y, z - 0.03), (x1, y + 0.03, z + 0.03), TX["hub"])


def prop(m, cx, y, cz, length, chord, tilt=0.0, lean=0.12):
    """A two-blade carbon prop standing face-on in the comb rack, leaning back towards the pegboard."""
    span = np.array([math.sin(tilt), math.cos(tilt) * math.cos(lean), -math.cos(tilt) * math.sin(lean)])
    chord_dir = np.array([math.cos(tilt), -math.sin(tilt), 0.0])
    face = np.cross(chord_dir, span)
    face /= np.linalg.norm(face)
    if face[2] < 0:
        face = -face
    hub = np.array([cx, y, cz]) + span * (length / 2 + 0.005)
    for sgn in (1, -1):
        prev = None
        for k in range(8):
            t = k / 7
            rr = 0.022 + t * (length / 2 - 0.022)
            c = chord * (0.6 + 0.55 * math.sin(math.pi * min(1.0, t * 1.1))) * (1 - 0.6 * t ** 3)
            tw = math.radians(30 - 20 * t) * sgn
            p = hub + span * sgn * rr
            cv = (chord_dir * math.cos(tw) + face * math.sin(tw)) * c / 2
            cur = (p - cv, p + cv)
            if prev is not None:
                q = [prev[0], prev[1], cur[1], cur[0]]
                m.oquad(q, face, TX["white"] if t > 0.86 else TX["carbon_sheen"])
                m.oquad([q[1], q[0], q[3], q[2]], -face, TX["carbon"])
            prev = cur
    P.cyl(m, hub - face * 0.012, hub + face * 0.014, 0.017, TX["carbon"], n=10, cap=TX["steel"])


def rotor(m, cx, y, cz, r=0.26, blades=9):
    """A T4 ducted-fan rotor lying flat: hub, nine swept blades, its tip ring."""
    P.cyl(m, (cx, y, cz), (cx, y + 0.05, cz), 0.055, TX["black"], n=14)
    for k in range(blades):
        a0 = 2 * math.pi * k / blades
        prev = None
        for t in np.linspace(0, 1, 5):
            rr = 0.055 + t * (r - 0.06)
            a = a0 + 0.35 * t
            p = np.array([cx + rr * math.cos(a), y + 0.025, cz + rr * math.sin(a)])
            tang = np.array([-math.sin(a), 0, math.cos(a)])
            c = 0.07 - 0.03 * t
            cur = (p - tang * c / 2 + np.array([0, 0.012, 0]), p + tang * c / 2 - np.array([0, 0.012, 0]))
            if prev is not None:
                m.oquad([prev[0], prev[1], cur[1], cur[0]], (0, 1, 0), TX["carbon_sheen"])
                m.oquad([prev[1], prev[0], cur[0], cur[1]], (0, -1, 0), TX["carbon"])
            prev = cur
    n = 28
    ctr = np.array([cx, 0, cz])
    for i in range(n):
        a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        p0 = np.array([cx + r * math.cos(a0), 0, cz + r * math.sin(a0)])
        p1 = np.array([cx + r * math.cos(a1), 0, cz + r * math.sin(a1)])
        m.oquad([p0 + (0, y + 0.035, 0), p1 + (0, y + 0.035, 0), p1 + (0, y + 0.015, 0), p0 + (0, y + 0.015, 0)], (p0 + p1) / 2 - ctr, TX["duct"])
        m.oquad([p0 + (0, y + 0.035, 0), p1 + (0, y + 0.035, 0), p1 * 0.95 + ctr * 0.05 + (0, y + 0.035, 0),
                 p0 * 0.95 + ctr * 0.05 + (0, y + 0.035, 0)], (0, 1, 0), TX["duct"])


def ducts(m, cx, y, z, rim):
    """A T4 fan duct lying on its side: the shroud ring, its rim stripe (gold rear, white front, as the drone's)."""
    n, r = 30, 0.30
    c = np.array([cx, y, z])
    for a in range(n):
        a0, a1 = 2 * math.pi * a / n, 2 * math.pi * (a + 1) / n
        p0 = np.array([cx + r * math.cos(a0), y, z + r * 0.6 * math.sin(a0)])
        p1 = np.array([cx + r * math.cos(a1), y, z + r * 0.6 * math.sin(a1)])
        m.oquad([p0 + (0, 0.12, 0), p1 + (0, 0.12, 0), p1, p0], (p0 + p1) / 2 - c, TX["duct"])
        m.oquad([p0 + (0, 0.12, 0), p1 + (0, 0.12, 0), (p1 - c) * 0.9 + c + (0, 0.12, 0), (p0 - c) * 0.9 + c + (0, 0.12, 0)], (0, 1, 0), TX[rim])
        m.oquad([(p0 - c) * 0.9 + c + (0, 0.12, 0), (p1 - c) * 0.9 + c + (0, 0.12, 0), (p1 - c) * 0.9 + c, (p0 - c) * 0.9 + c],
                -((p0 + p1) / 2 - c), TX["duct"])


# ------------------------------------------------------------------ the two stockings

def stock_a(m):
    L, R = (XS[0] + 0.05, XS[1] - 0.05), (XS[1] + 0.05, XS[2] - 0.05)
    y0, y1, y2, y3, y4 = DECKS
    zc = (ZB + ZF) / 2
    P.box(m, (L[0], y0, ZB + 0.06), (L[1], y0 + 0.04, ZF - 0.05), P.M["graphite_dk"], faces="nsewu")
    for k in range(5):
        z = ZB + 0.12 + k * 0.085
        P.carbon_arm(m, (L[0] + 0.10, y0 + 0.062, z), (L[1] - 0.10, y0 + 0.062, z))
    P.carton(m, (R[0] + 0.30, y0, zc), 0.56, 0.18, 0.44, "CF FRAME KIT  T2", "1 SET", 31, "NT-FK-T2")
    P.carton(m, (R[0] + 0.29, y0 + 0.18, zc + 0.01), 0.48, 0.12, 0.38, "CF FRAME KIT  T3", "1 SET", 32, "NT-FK-T3")
    for z in (ZB + 0.14, ZB + 0.40):
        skid(m, R[0] + 0.66, R[1] - 0.04, y0, z)
    ports = P.balance_charger(m, m, (L[1] - 0.16, y1, zc - 0.02), 41, (0.62, 0.35, 0.88, 0.47))
    P.lipo_safe(m, (L[0] + 0.30, y1, zc - 0.02), 0.34, 0.10, 0.26, [(6, 5200, (30, 120, 210)), (6, 5200, (30, 120, 210)),
                                                                   (4, 3300, (200, 60, 40)), (4, 3300, (200, 60, 40))], 42, charger_ports=ports)
    P.lipo(m, (L[0] + 0.66, y1, zc + 0.02), 0.16, 0.068, 0.042, 6, 5200, 45)
    P.battery_station(m, m, (R[0] + 0.38, y1, zc - 0.03), 2, 4, (0.09, 0.24, 0.16), 43)
    P.foam_case(m, L[0] + 0.02, L[0] + 0.62, y2, ZB + 0.08, ZF - 0.06, 3, 4, 0.045, 0.05, 51)
    P.foam_case(m, L[0] + 0.68, L[1] - 0.02, y2, ZB + 0.08, ZF - 0.06, 2, 3, 0.07, 0.07, 52)
    kinds = [("M3 X 8", "screws"), ("M3 NYLOC", "nuts"), ("XT90", "xt90"), ("BEARING 625", "bearings"), ("ESC 60A", "esc"),
             ("STANDOFF M3", "standoffs"), ("GPS M10", "gps"), ("M4 X 12", "screws"), ("M4 NYLOC", "nuts")]
    for i in range(18):
        lbl, k = kinds[i % len(kinds)]
        x = R[0] + 0.02 + (i % 6) * 0.215
        y = y2 + (i // 6) * 0.136
        if x + 0.205 > R[1]:
            continue
        P.hopper_bin(m, x, y, ZF - 0.05, 0.205, 0.13, 0.36, T.bin_card(lbl, 60 + i), P.heap(k), 60 + i)
    comb_rack(m, L[0] + 0.02, L[1] - 0.02, y3)
    for i in range(9):
        ln = 0.30 + 0.04 * ((i // 3) % 3)
        prop(m, L[0] + 0.14 + i * 0.145, y3 + 0.01, -0.20 + 0.012 * (i % 2), ln, 0.036, tilt=math.radians(3))
    for k, cx in enumerate((R[0] + 0.32, R[0] + 0.98)):
        P.cyl(m, (cx, y3, -0.17), (cx, y3 + 0.30, -0.17), 0.012, TX["steel"], n=8)
        for j in range(3 - k):
            rotor(m, cx, y3 + 0.005 + j * 0.06, -0.17)
    P.carton(m, (L[0] + 0.26, y4, zc), 0.50, 0.26, 0.42, "MOTOR 2810 KV900", "24 PCS", 71, "NT-M2810-09")
    P.carton(m, (L[0] + 0.26, y4 + 0.26, zc + 0.01), 0.40, 0.10, 0.34, "PROPS 13 IN", "40 PCS", 72, "NT-P13-40")
    P.open_carton(m, (L[0] + 0.86, y4, zc + 0.02), 0.42, 0.22, 0.36, "MOTOR 3115 KV900", "16 PCS", 73, "NT-M3115-09",
                  fill=lambda m_, lo, hi: P.product_boxes(m_, lo, hi, 73, "MOTOR 3115"))
    P.hard_case(m, (R[0] + 0.36, y4, zc), 0.62, 0.22, 0.40, "orange", "TOOL KIT  T4", "SERVICE SET  NT-TK-04", 74, latches=2)
    P.hard_case(m, (R[0] + 1.02, y4, zc + 0.02), 0.46, 0.16, 0.34, "black", "SOLDERING", "STATION  NT-SD-01", 75, latches=2)
    extinguisher_on_end(m, XS[2])


def stock_b(m):
    L, R = (XS[0] + 0.05, XS[1] - 0.05), (XS[1] + 0.05, XS[2] - 0.05)
    y0, y1, y2, y3, y4 = DECKS
    zc = (ZB + ZF) / 2
    for k in range(4):
        z = ZB + 0.12 + k * 0.11
        P.carbon_arm(m, (L[0] + 0.08, y0 + 0.04, z), (R[1] - 0.08, y0 + 0.04, z), r=0.032, mount=0.11, tex=P.M["alu_dark"])
    for i, cx in enumerate((L[0] + 0.33, L[0] + 0.98)):
        for j in range(2):
            ducts(m, cx, y1 + j * 0.13, -0.16, "rim_gold" if i == 0 else "rim_white")
    for i in range(3):
        P.hard_case(m, (R[0] + 0.23 + i * 0.44, y1, zc), 0.40, 0.17, 0.34, "black", "GIMBAL G%d" % (i + 2), "CAMERA + GIMBAL", 80 + i, latches=2)
    P.foam_case(m, L[0] + 0.02, L[1] - 0.02, y2, ZB + 0.08, ZF - 0.06, 2, 4, 0.085, 0.08, 81)
    P.foam_case(m, R[0] + 0.02, R[1] - 0.02, y2, ZB + 0.08, ZF - 0.06, 2, 3, 0.11, 0.10, 82)
    comb_rack(m, L[0] + 0.02, R[1] - 0.02, y3)
    for i in range(12):
        prop(m, L[0] + 0.15 + i * 0.226, y3 + 0.01, -0.20 + 0.012 * (i % 2), 0.41, 0.05, tilt=math.radians(3))
    for i, (it, q, pn) in enumerate((("BOOM 40 MM", "4 PCS", "NT-B40"), ("MOTOR 5010", "8 PCS", "NT-M5010"), ("PROPS 26 IN", "12 PCS", "NT-P26"),
                                     ("DUCT T4", "2 PCS", "NT-D4"), ("GIMBAL G2", "1 PC", "NT-G2"))):
        w = 0.48 + 0.06 * (i % 2)
        x = L[0] + 0.02 + i * 0.56 + w / 2
        P.carton(m, (x, y4, zc), w, 0.22 + 0.05 * (i % 3), 0.42, it, q, 90 + i, pn)


def extinguisher_on_end(m, x_end):
    """The extinguisher and its sign on a steel plate bolted across the end frame's outside face (+x)."""
    tmp = Mesh()
    P.ext_bracket(tmp, (0, 0.62, 0.0), plate_w=0.30, plate_h=0.42)
    P.extinguisher(tmp, (0, 0.40, 0.09))
    P.ext_sign(tmp, (0, 1.40, 0.0), size=0.22)
    m.merge(tmp, rot_y(math.pi / 2), (x_end + 0.045, 0, (ZB + ZF) / 2))
    for y in (0.66, 0.98, 1.43, 1.57):
        P.box(m, (x_end + 0.03, y, ZB + 0.01), (x_end + 0.041, y + 0.03, ZF - 0.01), P.M["steel"])


def unit(stock):
    m = Mesh()
    frame(m)
    labels(m, LABELS[stock])
    (stock_a if stock == "a" else stock_b)(m)
    return m


# ------------------------------------------------------------------ the atlas and the files

def _key(tex):
    return hashlib.sha1(np.ascontiguousarray(tex).tobytes() + bytes(str(tex.shape), "ascii")).hexdigest()


def split(q):
    """The quad cut where its texture repeats (an atlas patch holds one repeat): sub-quads with uvs inside 0..1."""
    P_, uv = q["P"], q["uv"]
    sh = np.asarray(q.get("shade", [1.0] * 4), float)

    def cuts(a, b):
        lo, hi = min(a, b), max(a, b)
        k = [x for x in range(int(math.floor(lo)) + 1, int(math.ceil(hi))) if lo + 1e-6 < x < hi - 1e-6]
        return sorted({(x - a) / (b - a) for x in k} if abs(b - a) > 1e-9 else set())
    # parameters along s (corner 0 -> 1) and t (corner 0 -> 3) where u or v crosses a whole repeat
    ss = sorted(set([0.0, 1.0] + cuts(uv[0][0], uv[1][0]) + cuts(uv[0][1], uv[1][1]) + cuts(uv[3][0], uv[2][0]) + cuts(uv[3][1], uv[2][1])))
    ts = sorted(set([0.0, 1.0] + cuts(uv[0][0], uv[3][0]) + cuts(uv[0][1], uv[3][1]) + cuts(uv[1][0], uv[2][0]) + cuts(uv[1][1], uv[2][1])))
    if len(ss) > 66 or len(ts) > 66:
        ss, ts = [0.0, 1.0], [0.0, 1.0]

    def at(arr, s, t):
        return (1 - s) * (1 - t) * arr[0] + s * (1 - t) * arr[1] + s * t * arr[2] + (1 - s) * t * arr[3]
    out = []
    for i in range(len(ss) - 1):
        for j in range(len(ts) - 1):
            corners = [(ss[i], ts[j]), (ss[i + 1], ts[j]), (ss[i + 1], ts[j + 1]), (ss[i], ts[j + 1])]
            p = np.array([at(P_, s, t) for s, t in corners])
            u = np.array([at(uv, s, t) for s, t in corners])
            c = u.mean(0)
            u = np.clip(u - np.floor(c), 0.0, 1.0)
            out.append((p, u, [float(at(sh, s, t)) for s, t in corners]))
    return out


class Atlas:
    """Every distinct texture of the meshes on one sheet, each with a 2-pixel border of its own edge pixels."""

    def __init__(self, textures, width=ATLAS_W):
        self.box = {}
        items = sorted(textures.items(), key=lambda kv: (-kv[1].shape[0], -kv[1].shape[1], kv[0]))
        x = y = shelf = 0
        pad = 2
        placed = []
        for k, a in items:
            h, w = a.shape[:2]
            if x + w + 2 * pad > width:
                x, y, shelf = 0, y + shelf, 0
            placed.append((k, x + pad, y + pad, w, h))
            x += w + 2 * pad
            shelf = max(shelf, h + 2 * pad)
        height = 1
        while height < y + shelf:
            height *= 2
        self.size = (width, height)
        sheet = np.zeros((height, width, 4), np.uint8)
        for k, px, py, w, h in placed:
            a = (np.clip(textures[k], 0, 1) * 255).round().astype(np.uint8)
            big = np.pad(a, ((pad, pad), (pad, pad), (0, 0)), mode="edge")
            sheet[py - pad:py + h + pad, px - pad:px + w + pad] = big
            self.box[k] = (px, py, w, h)
        self.img = Image.fromarray(sheet, "RGBA")

    def uv(self, k, u, v):
        x, y, w, h = self.box[k]
        return ((x + 0.02 + u * (w - 0.04)) / self.size[0], (y + 0.02 + v * (h - 0.04)) / self.size[1])


WHITE = P.solid((255, 255, 255))


def write_mesh(m, atlas, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    rows = []
    for q in m.quads:
        k = _key(q["tex"])
        for p, u, sh in split(q):
            rows.append((q["kind"], p, [atlas.uv(k, a, b) for a, b in u], sh, q["n"]))
    buf = bytearray(struct.pack(">iii", TCC.MAGIC, 1, len(rows)))
    for kind, p, uv, sh, n in rows:
        nb = [int(round(max(-1, min(1, v)) * 127)) for v in n]
        buf += struct.pack(">b", kind)
        for c in (0, 3, 2, 1):
            buf += struct.pack(">fffbbbffB", p[c][0], p[c][1], p[c][2], nb[0], nb[1], nb[2], uv[c][0], uv[c][1],
                               int(round(255 * max(0.0, min(1.0, sh[c])))))
    buf += struct.pack(">i", len(m.live))
    for kind, ident, corners in m.live:
        buf += struct.pack(">bb", kind, ident)
        for p in corners:
            buf += struct.pack(">fff", *p)
    buf += struct.pack(">i", 0)
    wk = _key(WHITE)
    for _ in range(3):
        u0, v0 = atlas.uv(wk, 0.25, 0.25)
        u1, v1 = atlas.uv(wk, 0.75, 0.75)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))
    return len(rows)


def write_data():
    j = TCC._json
    model = f"jugcraft:block/{ID}"
    j(ASSETS / "blockstates" / f"{ID}.json", {"variants": {f"facing={d},stock={s}": {"model": model}
                                                           for d in ("north", "east", "south", "west") for s in STOCKS}})
    j(ASSETS / "blockstates" / f"{PART_ID}.json", {"variants": {"": {"model": model}}})
    j(ASSETS / "models/block" / f"{ID}.json", {"textures": {"particle": "jugcraft:block/carbon_composite_panel"}, "elements": []})
    j(ASSETS / "models/item" / f"{ID}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}})
    j(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    j(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", TCC._loot(ID))
    j(DATA / "jugcraft/loot_table/blocks" / f"{PART_ID}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{PART_ID}"})
    j(DATA / "jugcraft/recipe" / f"{ID}.json", TCC._recipe(["PPP", "BRB", "PPP"], {
        "P": "#c:plates/steel", "B": "minecraft:barrel", "R": "jugcraft:storage_rack"}, ID))
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{PART_ID}"] = NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Drone Parts Shelving needs a clear space 3 blocks wide and 3 high"
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
    meshes = {}
    for s in STOCKS:
        m = unit(s)
        TCC.bake(m, floor_y=0.0, res=1 / 40, reach=0.25, rays=12)
        meshes[s] = m
    textures = {_key(WHITE): WHITE}
    for m in meshes.values():
        for q in m.quads:
            textures.setdefault(_key(q["tex"]), q["tex"])
    atlas = Atlas(textures)
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    counts = {s: write_mesh(meshes[s], atlas, ASSETS / f"tower_mesh/{ID}_{s}.bin.gz") for s in STOCKS}
    write_data()
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return meshes, atlas, counts


if __name__ == "__main__":
    meshes, atlas, counts = write_all()
    for s, m in meshes.items():
        kinds = [q["kind"] for q in m.quads]
        print(s, counts[s], "quads written", {k: kinds.count(k) for k in sorted(set(kinds))}, len(m.live), "live faces")
    area = sum(w * h for x, y, w, h in atlas.box.values())
    print("atlas", atlas.size, len(atlas.box), "textures", area, "pixels used")
