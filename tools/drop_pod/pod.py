"""Drop pods (mockup, Justin 7 November 2026): a one-soldier pod fired from a ship like a shell, on a high arc through
the sky trailing its re-entry flame, a hard retro burn at the last second, buried one block in the ground, its door
blown out on explosive bolts. Three forms (A hex lander, B blunt cone, C dart), each as a model in its own frame:
blocks, y up the pod's axis from the heat shield's lowest point, the door facing -z.

Faces: (P (4,3), UV (4,2), material name or texture array, glow) in the pod's frame; place() turns them into the
renderer's quads in the world (lit, heated on the faces into the air on entry, glows full bright)."""
import math
import os
import sys
S = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, S)
import numpy as np
from PIL import Image, ImageDraw, ImageFont
try:
    import render as R                     # (the mockups' renderer: only for place(), not for the export)
except ImportError:
    R = None

FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
UV1 = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
_T = {}


# ------------------------------------------------------------------------------------------------ materials
def _noise(h, w, seed, octaves=4):
    r = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    for o in range(octaves):
        gh, gw = 2 + 2 ** (o + 1), 2 + 2 ** (o + 1)
        g = r.random((gh, gw)).astype(np.float32)
        img = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(img, np.float32) / 255 / 2 ** o
    out -= out.min()
    out /= max(out.max(), 1e-6)
    return out


def _base(rgb, n=64, var=0.05, seed=1):
    t = np.ones((n, n, 4), np.float32)
    nz = _noise(n, n, seed)
    t[..., :3] = np.asarray(rgb, np.float32)[None, None] / 255 * (1 + var * (nz[..., None] * 2 - 1))
    return t


def _stencil(t, text, xy, size, rgb, anchor="mm", rot=0):
    """Paints white-ish stencil letters into a texture (RGBA float), worn."""
    h, w = t.shape[:2]
    im = Image.new("L", (w * 4, h * 4), 0)
    d = ImageDraw.Draw(im)
    d.text((xy[0] * 4, xy[1] * 4), text, font=ImageFont.truetype(FB, size * 4), fill=255, anchor=anchor)
    if rot:
        im = im.rotate(rot, center=(xy[0] * 4, xy[1] * 4))
    a = np.asarray(im.resize((w, h), Image.LANCZOS), np.float32) / 255
    wear = 0.75 + 0.25 * _noise(h, w, 77)
    a = a * wear
    t[..., :3] = t[..., :3] * (1 - a[..., None]) + np.asarray(rgb, np.float32) / 255 * a[..., None]
    return t


def mat(name):
    """The pod's materials, 64 texels a tile (one tile a block unless the face's UV says otherwise)."""
    if name in _T:
        return _T[name]
    n = 64
    if name in ("armor", "armor2", "armor3"):       # ceramic-composite armour plates over the hull: gunmetal, panel seams
        t = _base({"armor": (58, 61, 66), "armor2": (72, 75, 80), "armor3": (50, 53, 57)}[name], n, 0.07, 3)
        for k in (0, 32):
            t[k:k + 1, :, :3] *= 0.45; t[k + 1:k + 2, :, :3] *= 1.12      # seam and its lit edge
        t[:, 0:1, :3] *= 0.45; t[:, 1:2, :3] *= 1.10
        for (y, x) in ((5, 5), (5, 58), (37, 5), (37, 58), (26, 5), (26, 58), (58, 5), (58, 58)):
            t[y - 1:y + 2, x - 1:x + 2, :3] *= 0.62                           # fasteners
            t[y - 2, x - 1:x + 2, :3] *= 1.18
        chips = _noise(n, n, 11) > 0.86
        t[chips, :3] = t[chips, :3] * 0.7 + np.array([0.42, 0.42, 0.40]) * 0.3   # chipped paint: the bare ceramic under it
    elif name == "char":                           # the lower hull, scorched on entry: armour going sooty and blued
        t = mat("armor").copy()
        g = np.linspace(1.0, 0.0, n)[:, None]       # v 0 at the top of the tile: more soot lower down
        soot = 0.35 + 0.45 * (1 - g) + 0.2 * _noise(n, n, 21)[:, :]
        t[..., :3] *= np.clip(1.0 - 0.55 * soot[..., None], 0.25, 1)
        t[..., 2] *= 1.06
        streak = _noise(8, n, 23, 2)
        streak = np.asarray(Image.fromarray((streak * 255).astype(np.uint8)).resize((n, n), Image.BICUBIC), np.float32) / 255
        t[..., :3] *= (0.85 + 0.15 * streak)[..., None]
    elif name == "shield":                          # the ablative heat shield: charred carbon tiles, cracked, glassy in places
        t = _base((34, 28, 24), n, 0.10, 5)
        cell = 16
        for y in range(n):
            for x in range(n):
                xx = (x + (y // cell) * cell // 2) % cell
                if y % cell == 0 or xx == 0:
                    t[y, x, :3] *= 0.45
        burn = _noise(n, n, 31)
        t[..., 0] += 0.10 * (burn > 0.7)
        t[..., 1] += 0.04 * (burn > 0.7)
        glass = _noise(n, n, 37) > 0.9
        t[glass, :3] = t[glass, :3] * 0.6 + np.array([0.22, 0.20, 0.22]) * 0.4
    elif name == "skirt":                           # the thruster bay's plating: heat-resistant, darker, louvres
        t = _base((44, 45, 48), n, 0.06, 7)
        for y in range(6, n - 3, 8):
            t[y:y + 2, 6:58, :3] *= 0.4
            t[y + 2, 6:58, :3] *= 1.2
        t[:, 0:1, :3] *= 0.45
    elif name == "band":                            # the steel band round the shoulder and the shield's rim
        t = _base((112, 114, 118), n, 0.05, 9)
        t[::16, :, :3] *= 0.8
        t[0:2, :, :3] *= 0.6; t[-2:, :, :3] *= 0.6
    elif name == "hazard":
        t = _base((34, 33, 30), n, 0.04, 13)
        yy, xx = np.mgrid[0:n, 0:n]
        y_ = ((xx + yy) // 8) % 2 == 0
        t[y_, :3] = np.array([226, 172, 32]) / 255 * (0.9 + 0.1 * _noise(n, n, 14)[y_, None])
    elif name == "door":                            # the hatch: armour with its own seam, a recessed pull and a stencil
        t = mat("armor").copy()
        t[2:4, :, :3] *= 0.4; t[-4:-2, :, :3] *= 0.4; t[:, 2:4, :3] *= 0.4; t[:, -4:-2, :3] *= 0.4
    elif name == "bolt":                            # explosive bolt housings: yellow caps, black
        t = _base((30, 30, 32), 16, 0.05, 15)
        t[4:12, 4:12, :3] = np.array([232, 180, 40]) / 255
    elif name == "nozzle":                          # a thruster bell's outside: heat-blued steel, bronzed towards the lip
        t = _base((50, 52, 58), 32, 0.06, 17)
        v = np.linspace(0, 1, 32)[:, None]
        t[..., 0] += 0.10 * v; t[..., 1] += 0.05 * v; t[..., 2] += 0.02 * (1 - v)
        t[::4, :, :3] *= 0.82
    elif name == "throat":                          # inside the bell: sooty, blackened
        t = _base((22, 20, 20), 16, 0.08, 19)
    elif name == "dark":
        t = _base((24, 25, 28), 16, 0.06, 21)
    elif name == "steel":
        t = _base((128, 130, 134), 32, 0.05, 23)
    elif name == "pad":                             # inside: quilted padding, dark slate
        t = _base((62, 66, 72), n, 0.05, 25)
        for k in range(0, n, 32):
            t[k:k + 2, :, :3] *= 0.75; t[:, k:k + 2, :3] *= 0.75
        yy, xx = np.mgrid[0:n, 0:n]
        bulge = (np.cos((xx % 32) / 32 * 2 * math.pi) + np.cos((yy % 32) / 32 * 2 * math.pi)) * 0.05
        t[..., :3] *= (1 - bulge)[..., None]
    elif name == "frame":                           # inside: the seat's tube frame, the racks
        t = _base((60, 62, 66), 16, 0.05, 27)
    elif name == "strap":                           # harness straps: red-orange webbing
        t = _base((170, 70, 32), 16, 0.06, 29)
        t[:, ::3, :3] *= 0.85
    elif name == "screen":                          # the pod's status panel: a live display (cyan UI on black)
        t = np.zeros((64, 96, 4), np.float32); t[..., 3] = 1
        t[..., :3] = np.array([0.02, 0.04, 0.05])
        t[4:7, 6:90, :3] = [0.1, 0.8, 0.9]
        for k in range(5):
            t[14 + k * 9:18 + k * 9, 8:8 + (14 * (k + 2)) % 60 + 14, :3] = [0.1, 0.7, 0.85]
        t[50:58, 60:90, :3] = [0.95, 0.65, 0.12]
        t = _stencil(t, "ALT 1240", (70, 30), 9, (40, 230, 240))
    elif name == "beacon":
        t = np.ones((8, 8, 4), np.float32); t[..., :3] = [1.0, 0.25, 0.15]
    elif name == "lamp":
        t = np.ones((8, 8, 4), np.float32); t[..., :3] = [1.0, 0.95, 0.85]
    elif name == "amber":
        t = np.ones((8, 8, 4), np.float32); t[..., :3] = [1.0, 0.62, 0.12]
    elif name == "grate":                           # the floor: a steel grating over the thruster bay
        t = _base((84, 86, 90), 32, 0.05, 33)
        t[::4, :, :3] *= 0.35; t[:, ::4, :3] *= 0.5
    elif name == "olive":                           # the heavy weapon's finish
        t = _base((72, 78, 58), 32, 0.06, 35)
        t[::8, :, :3] *= 0.8
    elif name == "crate":                           # an ammunition case
        t = _base((66, 72, 52), 64, 0.05, 39)
        t[0:3, :, :3] *= 0.6; t[-3:, :, :3] *= 0.6; t[:, 0:3, :3] *= 0.6; t[:, -3:, :3] *= 0.6
        t = _stencil(t, "AMMO", (32, 32), 16, (220, 214, 180))
    elif name == "glasstint":
        t = _base((40, 56, 66), 16, 0.02, 31)
    else:
        t = _base((128, 128, 128), 16, 0.05, 1)
    _T[name] = t
    return t


GLOWS = {"beacon", "lamp", "amber", "screen"}


def interior(m, H):
    """What is inside the hex lander: padded walls, the grated floor, the standing shock harness the soldier rides in
    (room behind it to lift it for power armour), the heavy weapon clamped on the wall, an ammunition case, the status
    screen and the ceiling light. Adds to m (in the pod's frame)."""
    ph = H.ph
    yf, yc = 1.0, 3.10
    ri = lambda y: H.r(y) - 0.24
    m.lathe([(yf, ri(yf)), (yc, ri(yc))], 6, ph, ["pad"], inside=True, caps=(True, True), cap_mats=("grate", "pad"))
    # the back pad and the harness: shoulder bars down from the ceiling, a chest bar, straps
    cb, rb, ub, nb = H.face(3, 2.05)
    back = cb - nb * 0.24
    m.prism([(-0.40, -0.95), (0.40, -0.95), (0.40, 0.95), (-0.40, 0.95)], (back - nb * 0.06, rb, ub), 0.10, "pad", edge="frame")
    for s_ in (-1, 1):
        top = back - nb * 0.10 + rb * 0.20 * s_ + ub * 1.0
        low = back - nb * 0.42 + rb * 0.22 * s_ + ub * 0.36
        d = low - top; L = np.linalg.norm(d); d /= L
        side = rb
        fwd = np.cross(side, d)
        m.box((-0.05, 0.0, -0.06), (0.05, L, 0.06), "pad", rot=np.stack([side, d, fwd], 1), at=top)
        m.box((-0.035, -0.6, -0.01), (0.035, 0.0, 0.01), "strap", rot=np.stack([rb, ub, -nb], 1), at=back - nb * 0.115 + rb * 0.2 * s_ + ub * 0.9)
    m.box((-0.36, -0.05, -0.05), (0.36, 0.05, 0.05), "frame", rot=np.stack([rb, ub, -nb], 1), at=back - nb * 0.48 + ub * 0.32)
    # the heavy weapon on the back-left wall (a launcher on two clamps) and its ammunition case on the floor
    cw, rw, uw, nw = H.face(2, 2.0)
    wall = cw - nw * 0.24
    tube_c = wall - nw * 0.16
    ax = uw
    ref = rw
    m.lathe([(-0.55, 0.075), (0.55, 0.075)], 10, 0.0, ["olive"], origin=tube_c, axis=ax, ref=ref, tile=0.5, caps=(True, True),
            cap_mats=("dark", "dark"))
    m.lathe([(0.48, 0.09), (0.56, 0.09)], 10, 0.0, ["dark"], origin=tube_c, axis=ax, ref=ref, tile=0.5)
    m.box((-0.03, -0.12, 0.0), (0.03, 0.05, 0.14), "dark", rot=np.stack([rw, uw, -nw], 1), at=tube_c - nw * 0.06 - uw * 0.05)
    m.box((-0.04, -0.03, 0.0), (0.04, 0.10, 0.06), "frame", rot=np.stack([rw, uw, -nw], 1), at=tube_c - nw * 0.07 + uw * 0.18)
    for b in (-0.32, 0.32):
        m.box((-0.12, -0.03, 0.0), (0.12, 0.03, 0.16), "frame", rot=np.stack([rw, uw, -nw], 1), at=wall + uw * b)
    cc, rc, uc, nc = H.face(4, 1.2)
    crate_at = cc - nc * 0.62
    m.box((-0.28, 0.0, -0.18), (0.28, 0.34, 0.18), "crate", rot=np.stack([rc, [0, 1.0, 0], -nc], 1), at=np.array([crate_at[0], yf, crate_at[2]]))
    # the status screen on the back-right wall at eye level, the light along the ceiling
    cs, rs, us, ns = H.face(4, 2.45)
    sc = cs - ns * 0.235
    m.quad([sc - rs * 0.24 + us * 0.16, sc + rs * 0.24 + us * 0.16, sc + rs * 0.24 - us * 0.16, sc - rs * 0.24 - us * 0.16], UV1, "screen", -ns)
    m.box((-0.27, -0.19, -0.005), (0.27, 0.19, 0.0), "dark", rot=np.stack([rs, us, -ns], 1), at=sc + ns * 0.004)
    m.lathe([(yc - 0.06, 0.32), (yc - 0.01, 0.32)], 6, ph, ["lamp"], caps=(True, False), cap_mats=("lamp", None))
    m.lamps.append((np.array([0, yc - 0.08, 0]), (1.0, 0.95, 0.85)))



def decal(text, w=192, h=64, size=36, rgb=(222, 222, 214), bg="armor", sub=None):
    """A painted marking on an armour plate (its own texture): the pod's number, its unit, warnings."""
    t = mat(bg).copy()
    t = np.asarray(Image.fromarray((np.clip(t, 0, 1) * 255).astype(np.uint8)).resize((w, h), Image.NEAREST), np.float32) / 255
    t = _stencil(t, text, (w // 2, h // 2 - (8 if sub else 0)), size, rgb)
    if sub:
        t = _stencil(t, sub, (w // 2, h - 12), max(8, size // 3), rgb)
    return t


# ------------------------------------------------------------------------------------------------ geometry
class Model:
    def __init__(self):
        self.faces = []        # (P, UV, mat, glow)
        self.lamps = []        # (point, colour) for the scenes' light sprites

    def quad(self, P, UV, m, ref_out, glow=False):
        """A face seen from the side ref_out points to (the renderer culls the back of opaque faces)."""
        P = np.asarray(P, float); UV = np.asarray(UV, float)
        n = -np.cross(P[1] - P[0], P[2] - P[0])
        if np.linalg.norm(n) < 1e-12:
            n = -np.cross(P[2] - P[0], P[3] - P[0])
        if n @ np.asarray(ref_out, float) < 0:
            P, UV = P[::-1].copy(), UV[::-1].copy()
        if isinstance(m, str):
            glow = glow or m in GLOWS
        self.faces.append((P, UV, m, glow))

    def lathe(self, profile, n, phase, mats, origin=(0, 0, 0), axis=(0, 1, 0), ref=(0, 0, -1), tile=1.0, face_mat=None,
              caps=(False, False), cap_mats=(None, None), inside=False):
        """Rings round an axis: profile [(t along the axis, radius)], n sides, the first corner at angle `phase` (radians,
        from `ref` towards axis x ref). mats: one per band (or one for all); face_mat(k, band) may override."""
        o, ax = np.asarray(origin, float), np.asarray(axis, float) / np.linalg.norm(axis)
        e1 = np.asarray(ref, float) - ax * (np.asarray(ref, float) @ ax); e1 /= np.linalg.norm(e1)
        e2 = np.cross(ax, e1)
        ang = [phase + 2 * math.pi * k / n for k in range(n + 1)]
        def pt(t, r, a):
            return o + ax * t + (e1 * math.cos(a) + e2 * math.sin(a)) * r
        vs = [0.0]
        for (t0, r0), (t1, r1) in zip(profile, profile[1:]):
            vs.append(vs[-1] + math.hypot(t1 - t0, r1 - r0))
        for b, ((t0, r0), (t1, r1)) in enumerate(zip(profile, profile[1:])):
            for k in range(n):
                a0, a1 = ang[k], ang[k + 1]
                P = [pt(t1, r1, a0), pt(t1, r1, a1), pt(t0, r0, a1), pt(t0, r0, a0)]
                wtop = 2 * max(r1, 1e-6) * math.sin(math.pi / n)
                wbot = 2 * max(r0, 1e-6) * math.sin(math.pi / n)
                w = max(wtop, wbot)
                u0 = k * w / tile
                UV = [(u0 + (w - wtop) / 2 / tile, vs[b + 1] / tile), (u0 + (w + wtop) / 2 / tile, vs[b + 1] / tile),
                      (u0 + (w + wbot) / 2 / tile, vs[b] / tile), (u0 + (w - wbot) / 2 / tile, vs[b] / tile)]
                UV = [(u, -v) for u, v in UV]                       # v grows down the texture: the top of the band first
                m = mats[b] if isinstance(mats, (list, tuple)) else mats
                if face_mat:
                    m = face_mat(k, b) or m
                if m == "skip":
                    continue
                mid = (a0 + a1) / 2
                out = e1 * math.cos(mid) + e2 * math.sin(mid)
                slope = (r0 - r1) / max(abs(t1 - t0), 1e-6) if t1 != t0 else (1.0 if r1 < r0 else -1.0)
                ref_out = out + ax * slope * (1 if t1 >= t0 else -1)
                if t1 == t0:
                    ref_out = ax * (1 if r1 < r0 else -1)
                self.quad(P, UV, m, -ref_out if inside else ref_out)
        for which, (t, r) in ((0, profile[0]), (1, profile[-1])):
            if caps[which] and r > 1e-6:
                c = o + ax * t
                m = cap_mats[which] or (mats[0] if isinstance(mats, (list, tuple)) else mats)
                for k in range(n):
                    P = [c, pt(t, r, ang[k]), pt(t, r, ang[k + 1]), pt(t, r, ang[k + 1])]
                    UV = [(0.5, 0.5)] + [(0.5 + 0.5 * math.cos(a), 0.5 + 0.5 * math.sin(a)) for a in (ang[k], ang[k + 1], ang[k + 1])]
                    self.quad(P, UV, m, ax * (-1 if which == 0 else 1))

    def box(self, lo, hi, m, rot=None, at=(0, 0, 0), tile=1.0, mats=None):
        """An axis-aligned box in its own frame, turned by rot (3x3) and moved to at. mats: per face override dict."""
        (x0, y0, z0), (x1, y1, z1) = lo, hi
        Rm = np.eye(3) if rot is None else np.asarray(rot, float)
        at = np.asarray(at, float)
        faces = {"up": ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], (0, 1, 0), (x1 - x0, z1 - z0)),
                 "down": ([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], (0, -1, 0), (x1 - x0, z1 - z0)),
                 "north": ([(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], (0, 0, -1), (x1 - x0, y1 - y0)),
                 "south": ([(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)], (0, 0, 1), (x1 - x0, y1 - y0)),
                 "west": ([(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], (-1, 0, 0), (z1 - z0, y1 - y0)),
                 "east": ([(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)], (1, 0, 0), (z1 - z0, y1 - y0))}
        for f, (P, nrm, (w, h)) in faces.items():
            P = np.array(P, float) @ Rm.T + at
            UV = np.array([(0, 0), (w, 0), (w, h), (0, h)], float) / tile
            mm = (mats or {}).get(f, m)
            if mm is None:
                continue
            self.quad(P, UV, mm, Rm @ np.array(nrm, float))

    def prism(self, poly, frame, thick, m, edge=None, tile=1.0):
        """A flat plate: poly [(a, b)] in the plane origin + a*e_a + b*e_b, thick along e_n (both faces and the edges)."""
        o, ea, eb = (np.asarray(v, float) for v in frame)
        en = np.cross(ea, eb); en /= np.linalg.norm(en)
        n = len(poly)
        top = [o + ea * a + eb * b + en * thick / 2 for a, b in poly]
        bot = [o + ea * a + eb * b - en * thick / 2 for a, b in poly]
        uv = [(a / tile, -b / tile) for a, b in poly]
        for i in range(1, n - 1):
            self.quad([top[0], top[i], top[i + 1], top[i + 1]], [uv[0], uv[i], uv[i + 1], uv[i + 1]], m, en)
            self.quad([bot[0], bot[i], bot[i + 1], bot[i + 1]], [uv[0], uv[i], uv[i + 1], uv[i + 1]], m, -en)
        c = np.mean(top, 0)
        for i in range(n):
            j = (i + 1) % n
            P = [top[i], top[j], bot[j], bot[i]]
            L = np.linalg.norm(top[j] - top[i])
            out = (top[i] + top[j]) / 2 - c
            out -= en * (out @ en)
            self.quad(P, [(0, 0), (L / tile, 0), (L / tile, thick / tile), (0, thick / tile)], edge or m, out)


def frame_at(origin, normal, up=(0, 1, 0)):
    """An orthonormal frame on a face: (origin, right, up) with the face's outward normal."""
    nrm = np.asarray(normal, float); nrm /= np.linalg.norm(nrm)
    upv = np.asarray(up, float) - nrm * (np.asarray(up, float) @ nrm); upv /= np.linalg.norm(upv)
    right = np.cross(upv, nrm)
    return np.asarray(origin, float), right, upv, nrm


def nozzle(m, at, direction, r_throat=0.08, r_exit=0.18, length=0.34, n=12, inner=True):
    """A retro thruster's bell from its throat (at) out along direction."""
    d = np.asarray(direction, float); d /= np.linalg.norm(d)
    ref = np.cross(d, [0, 0, 1.0]) if abs(d[2]) < 0.9 else np.cross(d, [1.0, 0, 0])
    prof = [(0.0, r_throat * 1.25), (0.06, r_throat), (length * 0.55, r_throat + (r_exit - r_throat) * 0.62), (length, r_exit)]
    m.lathe(prof, n, 0.0, ["nozzle"] * 3, origin=at, axis=d, ref=ref, tile=0.5)
    if inner:
        m.lathe([(length - 0.01, r_exit - 0.012), (0.08, r_throat * 0.9)], n, 0.0, ["throat"], origin=at, axis=d, ref=ref, tile=0.5,
                caps=(False, True), cap_mats=(None, "throat"))
    return np.asarray(at, float) + d * length


def bolt(m, at, normal, r=0.055, h=0.07):
    """An explosive bolt's housing standing off a face: a short yellow-capped cylinder."""
    nrm = np.asarray(normal, float); nrm /= np.linalg.norm(nrm)
    ref = np.cross(nrm, [0, 1.0, 0]) if abs(nrm[1]) < 0.9 else np.array([1.0, 0, 0])
    m.lathe([(0.0, r), (h, r), (h, r * 0.7)], 8, 0.0, ["dark", "bolt"], origin=at, axis=nrm, ref=ref, tile=0.25,
            caps=(False, True), cap_mats=(None, "bolt"))


# ------------------------------------------------------------------------------------------------ the three forms
HEX_R = 2.5 / math.sqrt(3)          # A: 2.5 across the flats
DOOR = dict(w=1.06, y0=1.05, y1=3.0)


class HexBody:
    """The hex lander's body: radius by height (corner radius), the six faces' frames at any height."""
    PROF = [(0.0, 0.0), (0.24, 0.94), (0.34, 1.10), (0.44, 1.10), (0.60, 1.07), (0.98, 1.0), (3.05, 0.88), (3.13, 0.875),
            (3.66, 0.56), (3.80, 0.52), (3.92, 0.44)]           # (y, corner radius / HEX_R)

    def __init__(self, R0=None):
        self.R0 = R0 or HEX_R
        self.ph = math.radians(-30)
        self.e1 = np.array([0, 0, -1.0]); self.e2 = np.cross([0, 1.0, 0], self.e1)

    def r(self, y):
        pr = self.PROF
        for (y0, r0), (y1, r1) in zip(pr, pr[1:]):
            if y0 <= y <= y1:
                return self.R0 * (r0 + (r1 - r0) * (y - y0) / max(y1 - y0, 1e-9))
        return self.R0 * pr[-1][1]

    def dir(self, a):
        return self.e1 * math.cos(a) + self.e2 * math.sin(a)

    def face(self, k, y):
        """Face k (centred at ph + 60k + 30 degrees): (the face's centre at height y, right, up along the face, normal)."""
        a = self.ph + math.radians(60 * k + 30)
        out = self.dir(a)
        ap = lambda yy: self.r(yy) * math.cos(math.pi / 6)
        p0, p1 = out * ap(y - 0.05) + [0, y - 0.05, 0], out * ap(y + 0.05) + [0, y + 0.05, 0]
        up = (p1 - p0) / np.linalg.norm(p1 - p0)
        right = np.cross(up, out); right /= np.linalg.norm(right)
        nrm = np.cross(right, up)
        return out * ap(y) + [0, y, 0], right, up, nrm

    def corner(self, k, y):
        return self.dir(self.ph + math.radians(60 * k)) * self.r(y) + [0, y, 0]


def hex_pod(door_open=0.0, door_gone=False, scorched=1.0, plates=True, door_apart=False):
    """A: the hex lander. 4.0 high, 2.5 across the flats at the skirt (3 x 3 cells; 5 high with the block it is buried
    in): an ablative shield under a flared thruster skirt, a tapering armoured body with the hatch, a shoulder with the
    launch ring and three stabiliser fins, the cap with its beacon."""
    m = Model()
    H = HexBody()
    R0, ph = H.R0, H.ph
    prof = [(y, R0 * r) for y, r in H.PROF]
    mats = ["shield", "shield", "band", "skirt", "skirt", "char" if scorched > 0.5 else "armor", "band", "armor", "band", "armor"]
    opened = door_open > 0 or door_gone or door_apart
    m.lathe(prof, 6, ph, mats, caps=(False, True), cap_mats=(None, "dark"), tile=1.0,
            face_mat=(lambda k, b: "skip" if (k == 0 and b == 5) else None) if opened else None)
    if opened:
        interior(m, H)
    # raised armour plates on the body's faces (the hatch's face has its surround instead): two a face, a bevelled edge
    for k in range(6):
        if k == 0:
            continue
        for (yb, yt) in ((1.08, 2.02), (2.10, 2.96)):
            c, right, up, nrm = H.face(k, (yb + yt) / 2)
            half = (yt - yb) / 2
            w = H.r((yb + yt) / 2) * 0.82 / 2
            poly = [(-w, -half), (w, -half), (w * 0.97, half), (-w * 0.97, half)]
            tone = "char" if (scorched > 0.5 and yb < 1.5) else ("armor" if (k + int(yb > 2)) % 2 else "armor3")
            m.prism(poly, (c + nrm * 0.035, right, up), 0.05, tone, edge="armor2", tile=1.0)
    # corner ribs: strips down each corner of the body, following its taper
    for k in range(6):
        p0, p1 = H.corner(k, 1.0), H.corner(k, 3.05)
        mid = (p0 + p1) / 2
        upv = (p1 - p0) / np.linalg.norm(p1 - p0)
        out = H.dir(ph + math.radians(60 * k))
        tang = np.cross(upv, out); tang /= np.linalg.norm(tang)
        outn = np.cross(tang, upv)
        L = np.linalg.norm(p1 - p0)
        m.box((-0.08, -L / 2, -0.06), (0.08, L / 2, 0.04), "armor2", rot=np.stack([tang, upv, -outn], 1), at=mid + outn * 0.01)
    # the hatch's surround: a thick armoured frame standing out from the face, the hatch set in it
    yc = (DOOR["y0"] + DOOR["y1"]) / 2
    c, right, up, nrm = H.face(0, yc)
    dw, dh = DOOR["w"], DOOR["y1"] - DOOR["y0"]
    fw = 0.12                                     # the frame's width
    lift = 0.07                                   # how far it stands off the face
    for (a0, a1, b0, b1) in ((-dw / 2 - fw, -dw / 2, -dh / 2 - fw, dh / 2 + fw), (dw / 2, dw / 2 + fw, -dh / 2 - fw, dh / 2 + fw),
                             (-dw / 2, dw / 2, dh / 2, dh / 2 + fw), (-dw / 2, dw / 2, -dh / 2 - fw, -dh / 2)):
        poly = [(a0, b0), (a1, b0), (a1, b1), (a0, b1)]
        m.prism(poly, (c + nrm * lift / 2, right, up), lift, "hazard", edge="armor2", tile=0.5)
    m.door_frame = (c, right, up, nrm)
    if not door_gone:
        dm = Model()
        sink = 0.02                                # the hatch's face a little in from the frame's
        P0 = c + nrm * (lift - sink)
        poly = [(-dw / 2, -dh / 2), (dw / 2, -dh / 2), (dw / 2, dh / 2), (-dw / 2, dh / 2)]
        dm.prism(poly, (P0 - nrm * 0.05, right, up), 0.10, "door", edge="armor2", tile=1.0)
        front = P0 + nrm * 0.0
        # the pull bar, a stencilled 07 and the warning
        dm.box((-0.17, -0.04, 0.0), (0.17, 0.04, 0.05), "steel", rot=np.stack([right, up, nrm], 1), at=front + up * -0.05)
        for (txt, w, h, size, rgb, b, ww, hh) in (("07", 128, 64, 40, (222, 222, 214), 0.55, 0.30, 0.16),
                                                    ("EXPLOSIVE BOLTS", 256, 48, 22, (236, 186, 40), -0.45, 0.42, 0.09)):
            d0 = front + up * b + nrm * 0.008
            P = [d0 - right * ww + up * hh, d0 + right * ww + up * hh, d0 + right * ww - up * hh, d0 - right * ww - up * hh]
            dm.quad(P, UV1, decal(txt, w, h, size, rgb), nrm)
        if door_open > 0:
            t = door_open
            off = nrm * (2.4 * t) + up * (1.7 * t - 1.7 * t * t) + right * 1.6 * t
            axis_r = right
            ang = -2.0 * t
            K = np.array([[0, -axis_r[2], axis_r[1]], [axis_r[2], 0, -axis_r[0]], [-axis_r[1], axis_r[0], 0]])
            rot = np.eye(3) + math.sin(ang) * K + (1 - math.cos(ang)) * K @ K
            piv = P0
            dm.faces = [(((Pq - piv) @ rot.T + piv + off), UV, mm, gl) for Pq, UV, mm, gl in dm.faces]
        m.door = dm
        m.door_centre = P0
        if not door_apart:
            m.faces += dm.faces
    if opened:
        # the doorway: its reveal back through the wall to the padded inside
        inside = c - nrm * 0.22
        for (a0, a1, b0, b1, nn) in ((-dw / 2, -dw / 2, -dh / 2, dh / 2, right), (dw / 2, dw / 2, -dh / 2, dh / 2, -right)):
            P = [c + right * a0 + up * b1 + nrm * lift, inside + right * a0 + up * b1, inside + right * a0 + up * b0, c + right * a0 + up * b0 + nrm * lift]
            m.quad(P, [(0, 0), (0.3, 0), (0.3, 2), (0, 2)], "armor2", nn)
        for (b, nn) in ((dh / 2, -up), (-dh / 2, up)):
            P = [c + right * (-dw / 2) + up * b + nrm * lift, c + right * (dw / 2) + up * b + nrm * lift, inside + right * (dw / 2) + up * b,
                 inside + right * (-dw / 2) + up * b]
            m.quad(P, [(0, 0), (1, 0), (1, 0.3), (0, 0.3)], "armor2", nn)
    # the explosive bolts round the frame, four a side
    for s_ in (-1, 1):
        for b in np.linspace(-dh / 2 + 0.15, dh / 2 - 0.15, 4):
            bolt(m, c + right * s_ * (dw / 2 + fw / 2) + up * b + nrm * lift, nrm, r=0.038, h=0.05 if not door_gone else 0.02)
    # markings: the pod's number on two faces, the unit's insertion stripe round the shoulder band is in the material
    for k in (2, 4):
        cc, rr, uu, nn = H.face(k, 2.53)
        d0 = cc + nn * (0.035 + 0.025 + 0.006)
        P = [d0 - rr * 0.42 + uu * 0.16, d0 + rr * 0.42 + uu * 0.16, d0 + rr * 0.42 - uu * 0.16, d0 - rr * 0.42 - uu * 0.16]
        m.quad(P, UV1, decal("DP-07", 192, 64, 34, sub="ORBITAL INSERTION"), nn)
    # the retro thrusters: one at each corner, out past the shield's rim, canted 24 degrees out
    m.thrusters = []
    for k in range(6):
        a = ph + math.radians(60 * k)
        out = H.dir(a)
        throat = out * (R0 * 1.04) + np.array([0, 0.62, 0])
        d = -np.array([0, 1.0, 0]) * math.cos(math.radians(24)) + out * math.sin(math.radians(24))
        housing_c = out * (R0 * 1.02) + np.array([0, 0.78, 0])
        tang = np.cross([0, 1.0, 0], out)
        m.box((-0.17, -0.20, -0.10), (0.17, 0.18, 0.14), "skirt", rot=np.stack([tang, [0, 1.0, 0], out], 1), at=housing_c)
        exitp = nozzle(m, throat, d, r_throat=0.085, r_exit=0.19, length=0.40)
        m.thrusters.append((exitp, d))
    # the launch ring on the shoulder (where the gun's sabot holds it), the stabiliser fins
    m.lathe([(3.13, R0 * 0.90), (3.13, R0 * 0.95), (3.22, R0 * 0.95), (3.22, R0 * 0.86)], 6, ph, ["band", "band", "band"])
    for k in (1, 3, 5):
        a = ph + math.radians(60 * k)
        out = H.dir(a)
        base = out * (R0 * 0.66) + np.array([0, 3.30, 0])
        poly = [(0.0, 0.0), (0.0, 0.50), (0.46, 0.66), (0.62, 0.22)]
        m.prism(poly, (base, out, np.array([0, 1.0, 0])), 0.06, "armor2", edge="band", tile=1.0)
    # the cap: the beacon, an antenna
    m.lathe([(3.92, 0.10), (4.04, 0.10), (4.08, 0.06)], 8, 0.0, ["dark", "beacon"], caps=(False, True), cap_mats=(None, "beacon"))
    m.lamps.append((np.array([0, 4.06, 0]), (1.0, 0.25, 0.15)))
    m.box((-0.015, 3.90, 0.26), (0.015, 4.50, 0.29), "steel")
    # grab rails beside the hatch
    for s_ in (-1, 1):
        a = s_ * (dw / 2 + fw + 0.13)
        bot, top = c + right * a + up * -0.45, c + right * a + up * 0.55
        upv = (top - bot) / np.linalg.norm(top - bot)
        mid = (top + bot) / 2 + nrm * 0.10
        m.box((-0.02, -0.5, -0.02), (0.02, 0.5, 0.02), "steel", rot=np.stack([right, upv, nrm], 1), at=mid)
        for e in (bot, top):
            m.box((-0.02, -0.02, 0.0), (0.02, 0.02, 0.10), "steel", rot=np.stack([right, upv, nrm], 1), at=e)
    return m


def cone_pod(scorched=1.0):
    """B: the blunt cone, a re-entry capsule's shape: 3 across the heat shield, 3.9 high, four big retro motors."""
    m = Model()
    n = 20
    def r_at(y):
        return 1.44 - (y - 0.52) * (0.58 / 2.43)
    prof = [(0.0, 0.0), (0.18, 1.2), (0.30, 1.46), (0.40, 1.48), (0.52, 1.44), (1.0, r_at(1.0)), (2.9, r_at(2.9)), (2.95, 0.86),
            (3.10, 0.80), (3.45, 0.74), (3.60, 0.62), (3.70, 0.30)]
    door_k = {n - 1, 0, 1}                                # the hatch: three of the cone's panels round -z
    ph = math.radians(-9)
    m.lathe(prof, n, ph, ["shield", "shield", "band", "skirt", "char", "armor", "char", "band", "armor", "band", "dark"],
            face_mat=lambda k, b: "door" if (b == 5 and k in door_k) else None, caps=(False, True), cap_mats=(None, "dark"))
    def surf(y, a, lift=0.0):
        """A point on the cone at height y, angle a (radians from -z), lifted off it."""
        r = r_at(y) + lift
        return np.array([-math.sin(a) * r, y, -math.cos(a) * r])
    slope = math.atan(0.58 / 2.43)
    def normal(a):
        return np.array([-math.sin(a) * math.cos(slope), math.sin(slope), -math.cos(a) * math.cos(slope)])
    def strip(pa, pb, pc, pd, nn):
        m.quad([pa, pb, pc, pd], [(0, 0), (np.linalg.norm(pb - pa) * 4, 0), (np.linalg.norm(pb - pa) * 4, 0.4), (0, 0.4)], "hazard", nn)
    w_ = math.radians(4.5)
    for a in (math.radians(-27), math.radians(27)):           # the hatch's sides
        for y0_, y1_ in ((1.0, 2.9),):
            strip(surf(y1_, a - w_, 0.012), surf(y1_, a + w_, 0.012), surf(y0_, a + w_, 0.012), surf(y0_, a - w_, 0.012), normal(a))
    for y_ in (1.0, 2.9):                                      # its top and bottom
        for a0_, a1_ in ((-27, -9), (-9, 9), (9, 27)):
            a0r, a1r = math.radians(a0_), math.radians(a1_)
            strip(surf(y_ + 0.05, a0r, 0.012), surf(y_ + 0.05, a1r, 0.012), surf(y_ - 0.05, a1r, 0.012), surf(y_ - 0.05, a0r, 0.012),
                  normal((a0r + a1r) / 2))
    edge = math.radians(27 + 4)
    for y in np.linspace(1.15, 2.75, 4):
        for a in (-edge, edge):
            bolt(m, surf(y, a, 0.0), normal(a), r=0.045, h=0.06)
    for a in np.radians([-16, 0, 16]):
        for y in (0.94, 2.96):
            bolt(m, surf(y, a, 0.0), normal(a), r=0.045, h=0.06)
    oo, rr, uu, nn = frame_at(np.array([0.95, 1.6, 0.55]) * 1.0, np.array([0.86, 0.25, 0.5]))
    m.quad([oo + rr * -0.45 + uu * 0.2, oo + rr * 0.45 + uu * 0.2, oo + rr * 0.45 + uu * -0.2, oo + rr * -0.45 + uu * -0.2], UV1,
           decal("DP-07", 192, 64, 34, sub="ORBITAL INSERTION"), nn)
    m.thrusters = []
    for k in range(4):
        a = math.radians(45 + 90 * k)
        out = np.array([math.sin(a), 0, -math.cos(a)])
        throat = out * 1.56 + np.array([0, 0.60, 0])
        tang = np.cross([0, 1.0, 0], out)
        m.box((-0.24, -0.18, -0.18), (0.24, 0.30, 0.16), "skirt", rot=np.stack([tang, [0, 1.0, 0], out], 1), at=out * 1.50 + [0, 0.78, 0])
        d = np.array([0, -1.0, 0]) * math.cos(math.radians(18)) + out * math.sin(math.radians(18))
        exitp = nozzle(m, throat, d, r_throat=0.10, r_exit=0.22, length=0.40)
        m.thrusters.append((exitp, d))
    m.lathe([(3.70, 0.08), (3.82, 0.08), (3.86, 0.05)], 8, 0.0, ["dark", "beacon"], caps=(False, True), cap_mats=(None, "beacon"))
    m.lamps.append((np.array([0, 3.84, 0]), (1.0, 0.25, 0.15)))
    return m


def dart_pod(scorched=1.0):
    """C: the dart: octagonal, narrow and tall (2.2 across, 4.8 high), a long shoulder with four big fins at the top
    that keep it flying shield first, eight small thrusters."""
    m = Model()
    n = 8
    R0 = 1.1 / math.cos(math.pi / 8)
    prof = [(0.0, 0.0), (0.30, R0 * 0.95), (0.38, R0 * 1.02), (0.46, R0 * 1.02), (0.92, R0), (3.30, R0), (3.38, R0 * 0.98),
            (4.30, R0 * 0.42), (4.62, R0 * 0.30), (4.80, R0 * 0.18)]
    m.lathe(prof, n, math.radians(-22.5), ["shield", "shield", "band", "skirt", "char", "band", "armor", "armor2", "dark"],
            caps=(False, True), cap_mats=(None, "dark"))
    apothem = 1.1
    o, right, up, nrm = frame_at(np.array([0, 0, -apothem]), (0, 0, -1))
    P = [o + right * -0.40 + up * 3.05 + nrm * 0.02, o + right * 0.40 + up * 3.05 + nrm * 0.02, o + right * 0.40 + up * 1.05 + nrm * 0.02,
         o + right * -0.40 + up * 1.05 + nrm * 0.02]
    m.quad(P, [(0, 0), (0.8, 0), (0.8, 2.0), (0, 2.0)], "door", nrm)
    for (a0, a1, b0, b1) in ((-0.52, -0.42, 0.97, 3.13), (0.42, 0.52, 0.97, 3.13), (-0.42, 0.42, 3.05, 3.13), (-0.42, 0.42, 0.97, 1.05)):
        m.prism([(a0, b0), (a1, b0), (a1, b1), (a0, b1)], (o + nrm * 0.04, right, up), 0.05, "hazard", edge="armor2", tile=0.5)
    for b in np.linspace(1.15, 2.95, 4):
        for a in (-0.47, 0.47):
            bolt(m, o + right * a + up * b + nrm * 0.02, nrm, r=0.04, h=0.06)
    m.thrusters = []
    for k in range(8):
        a = math.radians(-90 + 45 * k)
        out = np.array([math.cos(a), 0, math.sin(a)])
        throat = out * 1.24 + np.array([0, 0.62, 0])
        tang = np.cross([0, 1.0, 0], out)
        m.box((-0.13, -0.16, -0.12), (0.13, 0.16, 0.10), "skirt", rot=np.stack([tang, [0, 1.0, 0], out], 1), at=out * 1.18 + [0, 0.76, 0])
        d = np.array([0, -1.0, 0]) * math.cos(math.radians(20)) + out * math.sin(math.radians(20))
        m.thrusters.append((nozzle(m, throat, d, r_throat=0.06, r_exit=0.13, length=0.28), d))
    for k in range(4):
        a = math.radians(45 + 90 * k)
        out = np.array([math.cos(a), 0, math.sin(a)])
        base = out * 0.70 + np.array([0, 3.55, 0])
        poly = [(0.0, 0.0), (0.0, 1.05), (0.55, 1.15), (0.78, 0.25)]
        m.prism(poly, (base, out, np.array([0, 1.0, 0])), 0.07, "armor2", edge="band")
    oo, rr, uu, nn = frame_at(np.array([0.78, 2.4, 0.78]), np.array([1, 0, 1.0]))
    m.quad([oo + rr * -0.4 + uu * 0.2, oo + rr * 0.4 + uu * 0.2, oo + rr * 0.4 + uu * -0.2, oo + rr * -0.4 + uu * -0.2], UV1,
           decal("DP-07", 192, 64, 34, sub="ORBITAL INSERTION"), nn)
    m.lathe([(4.80, 0.06), (4.92, 0.06), (4.95, 0.04)], 8, 0.0, ["dark", "beacon"], caps=(False, True), cap_mats=(None, "beacon"))
    m.lamps.append((np.array([0, 4.93, 0]), (1.0, 0.25, 0.15)))
    return m


# ------------------------------------------------------------------------------------------------ into the world
def rotx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def roty(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rotz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def place(m, at, rot=np.eye(3), sun=(0.45, 0.85, -0.3), amb=0.42, sky=(1.0, 1.0, 1.0), heat=0.0, travel=None, light=None):
    """The model in the world: turned by rot and moved to at, each face lit by the sun and the sky (or light(p): an RGB
    multiplier), its glows full bright; on entry the faces into the air glow hot (travel: the direction it flies)."""
    sun = np.asarray(sun, float); sun /= np.linalg.norm(sun)
    at = np.asarray(at, float)
    q = []
    hot = np.ones((2, 2, 4), np.float32)
    for P, UV, mm, gl in m.faces:
        Wp = P @ np.asarray(rot).T + at
        n = -np.cross(Wp[1] - Wp[0], Wp[2] - Wp[0])
        nn = np.linalg.norm(n)
        if nn < 1e-12:
            n = -np.cross(Wp[2] - Wp[0], Wp[3] - Wp[0]); nn = np.linalg.norm(n)
            if nn < 1e-12:
                continue
        n /= nn
        t = mat(mm) if isinstance(mm, str) else mm
        if gl:
            q.append((Wp, UV, t, np.ones((4, 3)), R.OPAQUE))
            q.append((Wp, UV, t, np.ones((4, 3)) * 0.6, R.GLOW_FACE))
            continue
        k = amb + (1 - amb) * max(0.0, n @ sun) + 0.12 * max(0.0, n[1])
        base = np.asarray(sky, float) * k
        if light is not None:
            sh = np.array([np.asarray(light(p + n * 0.5)) * k for p in Wp])
        else:
            sh = np.tile(base, (4, 1))
        q.append((Wp, UV, t, sh, R.OPAQUE))
        if heat > 0 and travel is not None:
            h = n @ (np.asarray(travel, float) / np.linalg.norm(travel))
            if h > 0.1:
                q.append((Wp, np.array(UV1), hot, np.tile(np.array([1.0, 0.40, 0.10]) * heat * h ** 1.3, (4, 1)), R.GLOW_FACE))
    return q
