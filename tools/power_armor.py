"""Narv-Tech power armour for the tower's security detail and the drop squads' legionaries (Justin, 9 November 2026:
"make the full set of Narv-Tech power armor for the legionaries/security. I want it to look cool, be black and purple
themed"; the sheets' takes 1 to 8, built on his "Build it"): a faceted mesh on each of the wearer's bones in place of the
old box exoskeleton, over a black ribbed undersuit (tools/crew_skins.py's security skin). Black plate with lighter worn
edges, gunmetal fittings, purple painted plates, violet lights.

The helmet is the legion's helm (a legionary's helmet made a sealed one: a bowl with a peaked brow over a recessed face,
a black visor with the eyes' light behind it, two cheek guards closing on the rebreather, a two-lame guard down the
neck), 9.1 px across its bowl on the 8 px head ("I don't like how fat the helmets seem ... Find a balance"). The leaders'
crests are groups of their own. The right vambrace carries the plasma blade's arm mount and the blade's moving parts,
each a group the client moves as the blade deploys (client/PowerArmor).

Writes assets/jugcraft/crew/armor.bin.gz (client/CrewShipMesh's format 2: one material, "armor"; a group for each bone's
plates and each moving part, its corners in its bone's own model space about the group's pivot) and
textures/entity/power_armor/armor.png with armor_glow.png (every material's tile on one sheet; the glow sheet is what's
added full bright over the lit faces). Ideas for the suit come from the Undying Mercenaries notes (Roman-styled legions,
a visor that overlays, a sealed suit with its own air, a powered frame, a blade in the suit's arm): ideas only.

Authored in the wearer's frame, pixels: x across (the wearer's right is -x), y up from the feet (0 to 32), z forward.
A face is wound counter-clockwise seen from outside. Run from the repository's root: python3 tools/power_armor.py"""
import math
import os
import sys
import numpy as np
from PIL import Image, ImageDraw

import gzip
import struct
from pathlib import Path

S = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, S)
import crew_skins as CS          # noqa: E402  (the decals' art: the NARV-TECH print and the hexagon N)

ROOT = Path(S).parent / "src/main/resources/assets/jugcraft"
MESH = ROOT / "crew/armor.bin.gz"
SHEET = ROOT / "textures/entity/power_armor/armor.png"
SHEET_GLOW = ROOT / "textures/entity/power_armor/armor_glow.png"

TPX, TILE = 4.0, 32               # texels to a figure pixel; a material tile's side
PIV = {"head": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0), "right_leg": (-2, 12, 0), "left_leg": (2, 12, 0)}
BONE = {"head": "head", "body": "body", "back": "body", "right_arm": "right_arm", "left_arm": "left_arm", "right_leg": "right_leg",
        "left_leg": "left_leg", "crest_0": "head", "crest_1": "head", "crest_2": "head"}
# the blade's moving parts, all on the right arm (client/PowerArmor moves them)
MOVING = ("shutter_a", "shutter_a_hot", "shutter_b", "shutter_b_hot", "fins_a", "fins_a_hot", "fins_b", "fins_b_hot", "carriage", "slot_lit",
          "spine_0", "spine_1", "spine_2", "spine_3", "plasma")
BONE.update({g: "right_arm" for g in MOVING})

# ------------------------------------------------------------------------------------------------ materials
BLACK, EDGE, GUN, PURPLE, DEEP = (27, 27, 35), (58, 58, 74), (48, 48, 60), (92, 40, 160), (56, 24, 100)
LIT, LIT_GLOW = (146, 78, 232), (64, 28, 108)
HOT, HOT_GLOW = (214, 178, 255), (80, 70, 60)


def _tile(rgb, var=0.07, seed=1, scuff=0.0, n=TILE):
    r = np.random.default_rng(seed)
    t = np.ones((n, n, 4), np.float32)
    k = 1 + r.normal(0, var, (n, n, 1))
    t[..., :3] = np.clip(np.asarray(rgb, np.float32) / 255 * k, 0, 1)
    if scuff:
        m = r.random((n, n)) > 1 - scuff
        t[m, :3] = np.clip(t[m, :3] * 1.9 + 0.03, 0, 1)
    return t


def _flat(rgb, n=4):
    t = np.ones((n, n, 4), np.float32)
    t[..., :3] = np.asarray(rgb, np.float32) / 255
    return t


def _mats():
    m = {}
    m["ab"] = (_tile(BLACK, 0.045, 1), None)                          # satin black plate
    m["ae"] = (_tile(EDGE, 0.035, 10), None)                          # its worn edges, lighter
    m["ag"] = (_tile(GUN, 0.04, 2), None)                             # gunmetal
    m["ah"] = (_tile((78, 78, 96), 0.035, 11), None)                  # gunmetal's edges
    m["ap"] = (_tile(PURPLE, 0.04, 3), None)                          # purple paint
    m["ad"] = (_tile(DEEP, 0.04, 4), None)                            # its edges, deeper
    rub = _tile((12, 12, 16), 0.05, 5)
    for y in range(0, TILE, 4):                                       # ribbed rubber: a rib every pixel
        rub[y, :, :3] = np.clip(rub[y, :, :3] * 2.6 + 0.02, 0, 1)
    m["rub"] = (rub, None)
    m["lit"] = (_flat(LIT), _flat(LIT_GLOW))
    m["hot"] = (_flat(HOT), _flat(HOT_GLOW))
    # a visor's band: black glass, the eyes' light behind it, slanted in to the middle
    bd = np.ones((8, 48, 4), np.float32)
    bg = np.zeros((8, 48, 4), np.float32); bg[..., 3] = 1
    for y in range(8):
        bd[y, :, :3] = np.array([8, 6, 14]) / 255 * (0.7 + 0.8 * y / 7)
    bd[1, 5:17, :3] = np.array([34, 28, 54]) / 255                     # a glint under the brow
    for y, x0, x1 in ((3, 6, 17), (4, 8, 19), (5, 12, 20)):
        for lo, hi in ((x0, x1), (48 - x1, 48 - x0)):
            bd[y, lo:hi, :3] = np.array(LIT) / 255
            bg[y, lo:hi, :3] = np.array(LIT_GLOW) / 255
    for lo, hi in ((11, 17), (31, 37)):
        bd[4, lo:hi, :3] = np.array(HOT) / 255
        bg[4, lo:hi, :3] = np.array(HOT_GLOW) / 255 + 0.12
    for lo, hi in ((1, 4), (44, 47)):
        bd[6, lo:hi, :3] = np.array(DEEP) / 255
        bg[6, lo:hi, :3] = np.array(DEEP) / 255 * 0.5
    m["band"] = (bd, bg)
    sp = _tile((22, 20, 30), 0.05, 13, n=16)                          # the blade's spine: dark ceramic, a groove down its middle,
    sp[:, (0, 15), :3] = np.array(DEEP) / 255 * 0.8                   # emitter notches along its edges
    sp[::4, (1, 14), :3] = np.array([58, 44, 86]) / 255
    m["spine"] = (sp, None)
    bl = np.ones((16, 16, 4), np.float32)                             # the plasma blade across its width (z 0 at the tile's seam):
    bgl = np.zeros((16, 16, 4), np.float32); bgl[..., 3] = 1          # a white-hot core, violet out to its edges
    for cols, rgb, glow in (((15, 0), (236, 218, 255), (70, 60, 80)), ((14, 1), (186, 124, 250), (80, 40, 120)), ((13, 2), (138, 66, 232), (70, 26, 120)),
                            ((12, 3, 11, 4), (112, 46, 214), (60, 20, 112))):
        for c in cols:
            bl[:, c, :3] = np.array(rgb) / 255
            bgl[:, c, :3] = np.array(glow) / 255
    bl[:, 5:11, :3] = np.array((112, 46, 214)) / 255
    bgl[:, 5:11, :3] = np.array((60, 20, 112)) / 255
    m["blade"] = (bl, bgl)
    m["bladeedge"] = (_flat((124, 54, 222)), _flat((70, 24, 124)))    # and its edge
    # the respirator's grille: slots, a lit dot each side
    gr = _tile(GUN, 0.05, 6, n=16)
    gg = np.zeros((16, 16, 4), np.float32); gg[..., 3] = 1
    for y in range(3, 14, 3):
        gr[y:y + 2, 3:13, :3] = 0.03
    for x in (1, 14):
        gr[7:9, x, :3] = np.array(LIT) / 255
        gg[7:9, x, :3] = np.array(LIT_GLOW) / 255
    m["grille"] = (gr, gg)
    # a hub's face: a lit ring round a dark centre
    pod = _tile((16, 16, 20), 0.04, 7, n=16)
    pg = np.zeros((16, 16, 4), np.float32); pg[..., 3] = 1
    yy, xx = np.mgrid[0:16, 0:16]
    rr = np.hypot(xx - 7.5, yy - 7.5)
    ring = (rr > 3.2) & (rr < 5.4)
    pod[ring, :3] = np.array(LIT) / 255
    pg[ring, :3] = np.array(LIT_GLOW) / 255
    m["pod"] = (pod, pg)
    # the wrist's screen: lines of light on dark glass
    sc = _flat((22, 10, 44), 16)
    sg = np.zeros((16, 16, 4), np.float32); sg[..., 3] = 1
    r = np.random.default_rng(9)
    for y in range(2, 15, 3):
        w = int(r.integers(5, 13))
        sc[y, 2:2 + w, :3] = np.array(HOT) / 255
        sg[y, 2:2 + w, :3] = np.array([70, 60, 70]) / 255
    sc[11:14, 10:14, :3] = np.array(LIT) / 255
    sg[11:14, 10:14, :3] = np.array(LIT_GLOW) / 255
    m["screen"] = (sc, sg)
    # the crest's ribbed side
    cr = _tile(PURPLE, 0.04, 8)
    for x in range(0, TILE, 4):
        cr[:, x, :3] *= 0.42
    m["crest"] = (cr, None)
    for name, under in (("emblem", GUN), ("wordmark", BLACK)):          # the decals, on their plates
        rgb, a, glow = CS.decal_art(name)
        t = np.ones(rgb.shape[:2] + (4,), np.float32)
        t[..., :3] = (rgb * a[..., None] + np.asarray(under, np.float32) * (1 - a[..., None])) / 255
        g = np.zeros_like(t); g[..., 3] = 1
        g[..., :3] = glow / 255 * a[..., None]
        m[name] = (t, g)
    return m


MATS = _mats()


# ------------------------------------------------------------------------------------------------ the builder
def oct_ring(y, x0, x1, z0, z1, c, cb=None):
    """An octagon at height y (corners c at the front, cb at the back): sides 0 front (+z), 1 front +x corner, 2 +x,
    3 back +x corner, 4 back, 5, 6 -x, 7."""
    cb = c if cb is None else cb
    return np.array([(x0 + c, y, z1), (x1 - c, y, z1), (x1, y, z1 - c), (x1, y, z0 + cb), (x1 - cb, y, z0), (x0 + cb, y, z0), (x0, y, z0 + cb),
                     (x0, y, z1 - c)], float)


EDGES = {"ab": "ae", "ag": "ah", "ap": "ad"}          # a plate's bevels: its edge tone


class Kit:
    def __init__(self):
        self.g = {}
        self.cur = None
        self.mx = 1.0

    def to(self, group, mirror=False):
        self.cur = self.g.setdefault(group, [])
        self.mx = -1.0 if mirror else 1.0
        return self

    def _p(self, P):
        P = np.array(P, float)
        P[..., 0] *= self.mx
        return P

    def face(self, P, mat, inside, uv=None):
        P = self._p(P)
        inside = self._p(inside)
        n = np.cross(P[1] - P[0], P[2] - P[0])
        if np.linalg.norm(n) < 1e-9:
            n = np.cross(P[2] - P[0], P[3] - P[0])
        if np.linalg.norm(n) < 1e-9:
            return
        if uv is not None:
            uv = np.array(uv, float)
        if n @ (P.mean(0) - inside) < 0:
            P = P[::-1].copy()
            uv = None if uv is None else uv[::-1].copy()
        self.cur.append((P, mat, uv))

    def poly(self, pts, mat, inside, uvf=None):
        pts = [np.asarray(p, float) for p in pts]
        i = 1
        while i < len(pts) - 1:
            q = [pts[0], pts[i], pts[i + 1], pts[i + 2] if i + 2 < len(pts) else pts[i + 1]]
            self.face(q, mat, inside, None if uvf is None else [uvf(self._p(p)) for p in q])
            i += 2 if i + 2 < len(pts) else 1

    def loft(self, rings, mat, caps=(True, True), pick=None, capmat=None, capuv=None):
        rings = [np.asarray(r, float) for r in rings]
        inside = np.mean([r.mean(0) for r in rings], 0)
        n = len(rings[0])
        for b in range(len(rings) - 1):
            for k in range(n):
                P = [rings[b][k], rings[b][(k + 1) % n], rings[b + 1][(k + 1) % n], rings[b + 1][k]]
                m, uv = mat, None
                if pick is not None:
                    r = pick(b, k, P)
                    if r is not None:
                        m, uv = r
                self.face(P, m, inside, uv)
        cm = capmat or mat
        if caps[0]:
            self.poly(rings[0], cm[0] if isinstance(cm, tuple) else cm, inside, capuv)
        if caps[1]:
            self.poly(rings[-1], cm[1] if isinstance(cm, tuple) else cm, inside, capuv)

    def box(self, lo, hi, mat):
        (x0, y0, z0), (x1, y1, z1) = lo, hi
        self.loft([[(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]], mat)

    def obox(self, c, a, b, d, mat):
        """A box about c with half-axes a, b, d (vectors)."""
        c, a, b, d = (np.asarray(v, float) for v in (c, a, b, d))
        self.loft([[c - a - b - d, c + a - b - d, c + a + b - d, c - a + b - d], [c - a - b + d, c + a - b + d, c + a + b + d, c - a + b + d]], mat)

    def plate(self, base, out, t, inset, mat, cap=None, closed=False, capuv=False, edge=None):
        """A bevelled plate standing t off its base outline (any polygon; for a decal cap: top-left, top-right,
        bottom-right, bottom-left seen from outside), its face inset from the outline."""
        base = np.asarray(base, float)
        out = np.asarray(out, float) / np.linalg.norm(out)
        c = base.mean(0)
        top = np.array([p + (c - p) * min(0.45, inset * 1.3 / max(np.linalg.norm(p - c), 1e-6)) + out * t for p in base])
        inside = c + out * (t * 0.4)
        n = len(base)
        edge = EDGES.get(mat, mat) if edge is None else edge
        for k in range(n):
            self.face([base[k], base[(k + 1) % n], top[(k + 1) % n], top[k]], edge, inside)
        if capuv and n == 4:
            self.face(top, cap or mat, inside, [(0, 0), (1, 0), (1, 1), (0, 1)])
        else:
            self.poly(top, cap or mat, inside)
        if closed:
            self.poly(base, mat, inside)

    def keel(self, tl, tr, br, bl, out, t, e, inset, mat):
        """A plate with a ridge down its middle: its outline raised e off the base, the ridge t, its ends inset."""
        tl, tr, br, bl = (np.asarray(v, float) for v in (tl, tr, br, bl))
        out = np.asarray(out, float) / np.linalg.norm(out)
        c = (tl + tr + br + bl) / 4
        inside = c
        rt = (tl + tr) / 2 + ((bl + br) / 2 - (tl + tr) / 2) * (inset / np.linalg.norm((bl + br) / 2 - (tl + tr) / 2)) + out * t
        rb = (bl + br) / 2 + ((tl + tr) / 2 - (bl + br) / 2) * (inset / np.linalg.norm((bl + br) / 2 - (tl + tr) / 2)) + out * t
        TL, TR, BR, BL = tl + out * e, tr + out * e, br + out * e, bl + out * e
        self.face([TL, rt, rb, BL], mat, inside)
        self.face([TR, rt, rb, BR], mat, inside)
        self.face([TL, TR, rt, rt], mat, inside)
        self.face([BL, BR, rb, rb], mat, inside)
        if e > 0:
            for a, b, A, B in ((tl, tr, TL, TR), (tr, br, TR, BR), (br, bl, BR, BL), (bl, tl, BL, TL)):
                self.face([a, b, B, A], mat, inside)

    def prism(self, profile, axis, a0, a1, mat, bevel=0.0, shrink=0.82, pick=None, capmat=None):
        """A profile (a convex polygon in the plane across the axis) run from a0 to a1; bevel: its ends drawn in."""
        prof = np.asarray(profile, float)

        def ring(a, k):
            p = prof.mean(0) + (prof - prof.mean(0)) * k
            if axis == "x":                                   # profile (y, z)
                return np.stack([np.full(len(p), a), p[:, 0], p[:, 1]], 1)
            if axis == "z":                                   # profile (x, y)
                return np.stack([p[:, 0], p[:, 1], np.full(len(p), a)], 1)
            return np.stack([p[:, 0], np.full(len(p), a), p[:, 1]], 1)        # y: profile (x, z)
        if bevel > 0:
            rings = [ring(a0, shrink), ring(a0 + bevel, 1), ring(a1 - bevel, 1), ring(a1, shrink)]
        else:
            rings = [ring(a0, 1), ring(a1, 1)]
        self.loft(rings, mat, pick=pick, capmat=capmat)

    def hexp(self, c, out, r, depth, mat, cap=None, right=None, up=(0, 1, 0), sink=0.3):
        """A hexagonal boss: its face r across the flats' corners, standing depth out of the surface at c."""
        c, out, up = (np.asarray(v, float) for v in (c, out, up))
        out = out / np.linalg.norm(out)
        up0, right0 = up.copy(), right
        right = np.cross(up, out) if right is None else np.asarray(right, float)
        right = right / np.linalg.norm(right)
        up = np.cross(out, right)
        ring = lambda d: np.array([c + out * d + (right * math.cos(math.radians(60 * k + 30)) + up * math.sin(math.radians(60 * k + 30))) * r
                                   for k in range(6)])
        cm, om = self._p(c), self._p(out)
        rm = np.cross(up0, om) if right0 is None else np.asarray(right0, float)       # the viewer's right, as worn
        rm = rm / np.linalg.norm(rm)
        um = np.cross(om, rm)
        uvf = lambda p: (0.5 + ((p - cm) @ rm) / (2 * r), 0.5 - ((p - cm) @ um) / (2 * r))
        self.loft([ring(-sink), ring(depth)], mat, caps=(False, True), capmat=cap or mat, capuv=uvf if cap else None)

    # ---- out
    def local(self, group):
        """The group's faces in its bone's model space: (P, UV, texture, normal, glow texture or None)."""
        px, py, pz = PIV[BONE[group]]
        out = []
        for P, mat, uv in self.g.get(group, []):
            n = np.cross(P[1] - P[0], P[2] - P[0])
            if np.linalg.norm(n) < 1e-9:
                n = np.cross(P[2] - P[0], P[3] - P[0])
            n = n / np.linalg.norm(n)
            tex, glow = MATS[mat]
            if uv is None:
                a = int(np.argmax(np.abs(n)))
                u, v = ((P[:, 2], -P[:, 1]) if a == 0 else (P[:, 0], P[:, 2]) if a == 1 else (P[:, 0], -P[:, 1]))
                uv = np.stack([u, v], 1) * TPX / tex.shape[0]
            L = np.stack([P[:, 0] - px, (24 - P[:, 1]) - py, -P[:, 2] - pz], 1)
            out.append((L, np.asarray(uv, float), tex, np.array([n[0], -n[1], -n[2]]), glow))
        return out

    def count(self):
        return {g: len(f) for g, f in self.g.items()}

    def quads(self, group):
        """The group's faces for the mesh: (corners in its bone's model space, uv or None for a tiled face, material,
        normal), wound as authored (counter-clockwise from outside: the model space turns y and z over together)."""
        px, py, pz = PIV[BONE[group]]
        out = []
        for P, mat, uv in self.g.get(group, []):
            n = np.cross(P[2] - P[0], P[3] - P[1])
            if np.linalg.norm(n) < 1e-9:
                n = np.cross(P[1] - P[0], P[2] - P[0])
            n = n / np.linalg.norm(n)
            L = np.stack([P[:, 0] - px, (24 - P[:, 1]) - py, -P[:, 2] - pz], 1)
            if uv is None:                                    # a tiled face: texels along the face, as local() lays them
                a = int(np.argmax(np.abs(n)))
                u, v = ((P[:, 2], -P[:, 1]) if a == 0 else (P[:, 0], P[:, 2]) if a == 1 else (P[:, 0], -P[:, 1]))
                tex = np.stack([u, v], 1) * TPX
                out.append((L, None, tex, mat, np.array([n[0], -n[1], -n[2]])))
            else:
                out.append((L, np.asarray(uv, float), None, mat, np.array([n[0], -n[1], -n[2]])))
        return out


# ------------------------------------------------------------------------------------------------ the armour
SQ = 2 - math.sqrt(2)
FRONT, FCORNER, SIDE, BCORNER, BACK = {0, 1, 11, 12}, {2, 10}, {3, 9}, {4, 8}, {5, 6, 7}


class Shell:
    """A helmet's shell: rings (y, half width, front, back, front corner, back corner, keel), octagons whose front
    comes to a ridge `keel` proud down the middle. Round it runs w: 0 the front's middle, 1 the front's end, 2 the side's
    front, 3 its back, 4 the back's end, 5 the back's middle, and on round the other side (6 to 10, or -1 to -4). A
    stripe `s` either side of the middle is its own faces (painted in loft), over the top from front to back."""

    def __init__(self, rows, s=0.95):
        self.r = [tuple(r) + (0.0,) * (7 - len(r)) for r in rows]
        self.ys = [r[0] for r in self.r]
        self.s = s

    def dims(self, y, t=0.0):
        hw, zf, zb, cf, cb, keel = (float(np.interp(y, self.ys, [r[c] for r in self.r])) for c in range(1, 7))
        return hw + t, zf + t, zb - t, cf + SQ * t, cb + SQ * t, keel

    def pt(self, w, y, t=0.0):
        """The point at w round the shell at height y, t off its surface."""
        hw, zf, zb, cf, cb, keel = self.dims(y, t)
        w = w % 10
        u = w if w <= 5 else w - 10
        sx, a = (1.0 if u >= 0 else -1.0), abs(u)
        if a <= 1:
            x, z = a * (hw - cf), zf + keel * (1 - a)
        elif a <= 2:
            x, z = hw - cf + (a - 1) * cf, zf - (a - 1) * cf
        elif a <= 3:
            x, z = hw, zf - cf + (a - 2) * (zb + cb - zf + cf)
        elif a <= 4:
            x, z = hw - (a - 3) * cb, zb + cb - (a - 3) * cb
        else:
            x, z = (hw - cb) * (5 - a), zb
        return np.array([sx * x, y, z])

    def nrm(self, w, y):
        """The surface's outward normal there."""
        y0, y1 = max(y - 0.2, self.ys[0]), min(y + 0.2, self.ys[-1])
        n = np.cross(self.pt(w, y1) - self.pt(w, y0), self.pt(w + 0.02, y) - self.pt(w - 0.02, y))
        n /= np.linalg.norm(n)
        hw, zf, zb = self.dims(y)[:3]
        return n if n @ (self.pt(w, y) - np.array([0, y, (zf + zb) / 2])) > 0 else -n

    def cut(self, *ys):
        """This shell with rings added at ys (a band between them to paint)."""
        rows = list(self.r)
        for y in ys:
            rows.append((y,) + self.dims(y))
        return Shell(sorted(rows), self.s)

    def W(self, y):
        return self.dims(y)[0]

    def F(self, y):
        return self.dims(y)[1]

    def B(self, y):
        return self.dims(y)[2]

    def ring(self, y):
        hw, zf, zb, cf, cb, keel = self.dims(y)
        f, b = self.s / (hw - cf), 5 - self.s / (hw - cb)
        return np.array([self.pt(w, y) for w in (0, f, 1, 2, 3, 4, b, 10 - b, 6, 7, 8, 9, 10 - f)])

    def loft(self, k, mat="ab", pick=None, stripe=None, caps=(True, True), smat="ap"):
        """The shell's faces; stripe (y0, y1): painted from y0 up the front, over the top and down the back to y1."""
        def pk(b, side, P):
            r = pick(b, side, P) if pick else None
            if r is None and stripe and ((side in (0, 12) and self.ys[b] >= stripe[0] - 1e-6) or (side == 6 and self.ys[b] >= stripe[1] - 1e-6)):
                r = (smat, None)
            return r
        rings = [self.ring(y) for y in self.ys]
        k.loft(rings, mat, caps=(False, False), pick=pk)
        inside = np.mean([r.mean(0) for r in rings], 0)
        if caps[0]:
            k.poly(rings[0], mat, inside)
        if caps[1]:
            R = rings[-1]
            if stripe:
                k.poly([R[12], R[0], R[1], R[6], R[7]], smat, inside)
                k.poly([R[1], R[2], R[3], R[4], R[5], R[6]], mat, inside)
                k.poly([R[7], R[8], R[9], R[10], R[11], R[12]], mat, inside)
            else:
                k.poly(R, mat, inside)

    def wrap(self, k, ws, y0, y1, t, mat, edge=None, b=0.14, flare=0.0, end=0.1, thick=None, uv=False, scale=None):
        """A plate wrapped round the shell through the stations ws, from y0 up to y1 (numbers, or functions of w),
        standing t off it (and `flare` more at its foot), its edges bevelled. thick: a skirt that thick, clear of the
        shell at its foot. uv: its face takes its material once over (u along it, v down it)."""
        f0 = y0 if callable(y0) else (lambda w: y0)
        f1 = y1 if callable(y1) else (lambda w: y1)
        edge = EDGES.get(mat, mat) if edge is None else edge
        wo = [min(max(w, ws[0] + end), ws[-1] - end) for w in ws]
        sc = scale or [1.0] * len(ws)
        I0 = [self.pt(w, f0(w), -0.03 if thick is None else (t + flare) * q - thick) for w, q in zip(ws, sc)]
        I1 = [self.pt(w, f1(w), -0.03) for w in ws]
        O0 = [self.pt(v, f0(w) + b, (t + flare) * q) for v, w, q in zip(wo, ws, sc)]
        O1 = [self.pt(v, f1(w) - b, t * q) for v, w, q in zip(wo, ws, sc)]
        c = np.mean(I0 + I1 + O0 + O1, 0)
        n = len(ws) - 1
        for i in range(n):
            k.face([O1[i], O1[i + 1], O0[i + 1], O0[i]], mat, c, [(i / n, 0), ((i + 1) / n, 0), ((i + 1) / n, 1), (i / n, 1)] if uv else None)
            k.face([O1[i], O1[i + 1], I1[i + 1], I1[i]], edge, c)
            k.face([O0[i], O0[i + 1], I0[i + 1], I0[i]], edge, c)
            if thick is not None:
                k.face([I1[i], I1[i + 1], I0[i + 1], I0[i]], "ab", c + (c - np.mean(I0 + I1, 0)) * 3)
        k.face([I0[0], I1[0], O1[0], O0[0]], edge, c)
        k.face([I0[-1], I1[-1], O1[-1], O0[-1]], edge, c)

    def brim(self, k, ws, ts, y, th, mat, droop=0.0, under=None):
        """A peak standing out of the shell at y: ts out at each of ws, th thick, its rim drooping."""
        under = EDGES.get(mat, mat) if under is None else under
        I1 = [self.pt(w, y + th, -0.05) for w in ws]
        I0 = [self.pt(w, y, -0.05) for w in ws]
        O1 = [self.pt(w, y + th - droop, t) for w, t in zip(ws, ts)]
        O0 = [self.pt(w, y - droop + th * 0.45, t) for w, t in zip(ws, ts)]
        c = np.mean(I0 + I1, 0) + np.array([0, th / 2, 0]) * 0
        c = np.mean(I0 + I1 + O0 + O1, 0)
        for i in range(len(ws) - 1):
            k.face([I1[i], I1[i + 1], O1[i + 1], O1[i]], mat, c)
            k.face([I0[i], I0[i + 1], O0[i + 1], O0[i]], under, c)
            k.face([O0[i], O0[i + 1], O1[i + 1], O1[i]], under, c)
        k.face([I0[0], I1[0], O1[0], O0[0]], under, c)
        k.face([I0[-1], I1[-1], O1[-1], O0[-1]], under, c)

    def bar(self, k, w, y, t, along, tall, mat="lit", proud=0.07):
        """A flat light on the surface (or on a plate t off it): `along` round, `tall` up."""
        n = self.nrm(w, y)
        tan = np.cross((0, 1, 0), n); tan /= np.linalg.norm(tan)
        k.obox(self.pt(w, y, t), tan * along / 2, np.array([0, tall / 2, 0]), n * proud, mat)


def _fittings(k, sh, fin=True, aerial=True, lights=30.5):
    """What every helmet carries: status lights at the back, an aerial, a fin along the top for the crests."""
    top = sh.ys[-1]
    for sx in (-1, 1):
        k.box((sx * 1.1 if sx > 0 else -2.3, lights - 0.14, sh.B(lights) - 0.12), (2.3 if sx > 0 else -1.1, lights + 0.14, sh.B(lights) + 0.1), "lit")
    if aerial:
        w = sh.W(30.8)
        k.box((w - 0.05, 30.5, -2.5), (w + 0.36, 31.05, -1.8), "ag")
        k.obox((w + 0.16, 32.45, -2.38), (0.08, 0, 0), (0, 1.45, -0.25), (0, 0.02, 0.08), "ah")
    if fin:
        z0, z1 = sh.r[-1][2], sh.r[-1][3]
        k.prism([(top - 0.15, z0 - 0.1), (top + 0.5, z0 - 0.8), (top + 0.68, z1 + 0.6), (top - 0.15, z1 + 0.05)], "x", -0.36, 0.36, "ag",
                pick=lambda b, side, P: ("lit", None) if side == 1 else None)


def _crests(k, sh, base=0.62):
    """The ranks' crests on the helmet's top (a comb taller up the ranks; the optio's two plumes)."""
    top = sh.ys[-1] + base
    z0, z1 = sh.r[-1][2], sh.r[-1][3]
    for rank, h in enumerate((1.2, 2.1, 3.0)):
        k.to("crest_%d" % rank)
        k.prism([(top - 0.05, z0 - 0.4), (top + 0.1 + h, z0 - 0.85), (top + 0.32 + h, z1 + 0.45), (top + 0.1, z1 - 0.1)], "x", -0.32, 0.32, "crest",
                pick=lambda b, side, P: ("lit", None) if side == 1 else ("ag", None), capmat="crest")
        if rank == 2:
            for sx in (-1, 1):
                base_p = np.array([sx * (sh.W(30.8) + 0.36), 30.7, -1.4])
                dv = np.array([sx * 0.24, 1.0, -0.12]); dv /= np.linalg.norm(dv)
                a = np.cross(dv, (0, 0, 1.0)); a /= np.linalg.norm(a)
                k.obox(base_p + dv * 2.0, a * 0.18, dv * 2.0, np.cross(a, dv) * 0.18, "lit")
                k.obox(base_p + dv * 0.1, a * 0.3, dv * 0.28, np.cross(a, dv) * 0.3, "ag")


def helmet_a(k):
    """A, the legion's helm: a galea made a sealed helmet. A bowl with a peaked brow over a recessed face, a black
    visor with the eyes' light behind it, two big cheek guards closing on the rebreather, a two-lame guard down the
    neck. Take 3: 9.1 px across its bowl (Justin: take 2's "look like fatties"; the head is 8)."""
    k.to("head")
    M = Shell([(25.4, 3.7, 3.85, -3.9, 1.4, 1.4, 0.5), (27.85, 4.0, 4.2, -4.05, 1.55, 1.5, 0.6), (29.6, 4.05, 4.05, -4.1, 1.6, 1.55, 0.45)], s=0.85)

    def pick(b, side, P):
        if b == 1 and side in FRONT | FCORNER:
            return "band", [((p[0] + 4.05) / 8.1, (29.6 - p[1]) / 1.75) for p in np.asarray(P)]
        return None
    M.loft(k, "ab", pick=pick, caps=(True, False))
    B = Shell([(29.4, 4.5, 4.5, -4.5, 1.8, 1.8, 0.38), (30.3, 4.55, 4.5, -4.55, 1.85, 1.8, 0.32), (31.3, 4.4, 4.2, -4.45, 1.95, 1.9, 0.15),
               (32.35, 3.6, 3.15, -3.5, 1.8, 1.8, 0.0), (33.0, 2.35, 1.7, -2.05, 1.2, 1.2, 0.0)], s=0.85)
    B.loft(k, "ab", stripe=(30.3, 29.4))
    # the brow: a reinforcing band round the front, a peak standing out of it
    B.wrap(k, [-2.55, -2, -1, 0, 1, 2, 2.55], 29.75, 30.4, 0.22, "ap", b=0.1)
    B.brim(k, [-2.35, -2, -1, 0, 1, 2, 2.35], [0.05, 0.3, 0.7, 1.15, 0.7, 0.3, 0.05], 29.32, 0.45, "ag", droop=0.14)
    for sx in (-1, 1):
        # a cheek guard: hung at the temple, round the jaw to the rebreather, cut away up to the eyes
        top = lambda w: float(np.interp(abs(w), [0.16, 0.55, 1, 2, 2.25, 2.5], [26.35, 27.7, 27.7, 27.8, 29.2, 29.2]))
        foot = lambda w: float(np.interp(abs(w), [0.16, 2.2, 2.5], [25.4, 25.4, 26.5]))
        ws = [0.16, 0.55, 1, 2, 2.25, 2.5]
        M.wrap(k, ws if sx > 0 else [-w for w in ws[::-1]], foot, top, 0.36, "ag", b=0.14)
        k.hexp(M.pt(sx * 2.38, 28.55, 0.32), (sx, 0, 0), 0.45, 0.25, "ah", cap="ap")         # its hinge
        M.bar(k, sx * 0.78, 26.15, 0.36, 0.18, 0.9)                                          # a breather light
        k.hexp(M.pt(sx * 2.72, 27.3, 0.0), (sx, 0, 0), 0.85, 0.34, "ag", cap="pod")          # the comms pod at the ear
    # the rebreather between them
    zc = M.F(26.2) + M.dims(26.2)[5]
    k.plate([(-0.45, 27.25, zc - 0.2), (0.45, 27.25, zc - 0.2), (0.38, 25.45, zc - 0.25), (-0.38, 25.45, zc - 0.25)], (0, 0, 1), 0.35, 0.07, "ag", cap="grille", capuv=True)
    # the neck guard: two lames lapped like a roof down from the bowl's rim, round the back from ear to ear
    back = [2.86, 3, 4, 6, 7, 7.14]
    ends = [0.2, 0.62, 1, 1, 0.62, 0.2]
    B.wrap(k, back, 27.9, 29.45, 0.04, "ab", flare=0.36, thick=0.25, b=0.08, scale=ends)
    B.wrap(k, back, 26.7, 28.25, 0.16, "ab", flare=0.3, thick=0.25, b=0.1, scale=ends)
    B.wrap(k, back, 26.4, 26.95, 0.5, "ag", flare=0.12, thick=0.25, b=0.09, scale=ends)     # its rim
    for sx in (-1, 1):
        B.bar(k, 5 + sx * 0.55, 27.45, 0.34, 1.15, 0.18, proud=0.09)
    _fittings(k, B, lights=30.6)
    _crests(k, B)


def torso(k):
    k.to("body")
    # the gorget: a ribbed seal up into the helmet
    k.loft([oct_ring(23.6, -4.3, 4.3, -2.7, 2.7, 1.0), oct_ring(25.9, -3.3, 3.3, -3.4, 3.4, 1.3)], "rub", caps=(False, False))
    for sx in (-1, 1):
        # a pectoral plate, its lower edge a chevron
        pec = [(sx * 0.14, 23.3, 1.95), (sx * 4.02, 23.3, 1.95), (sx * 4.02, 19.35, 1.95), (sx * 0.14, 18.35, 1.95)]
        k.plate(pec[::sx], (0, 0, 1), 1.0, 0.42, "ab")
        # the light under it
        a, b = np.array([sx * 0.3, 18.1, 2.2]), np.array([sx * 4.0, 19.05, 2.2])
        dv = (b - a) / 2
        up = np.array([-dv[1], dv[0], 0]); up = up / np.linalg.norm(up) * 0.13
        k.obox((a + b) / 2, dv, up, (0, 0, 0.25), "lit")
        # belt pouches, front and back
        k.plate([(sx * 1.45, 13.2, 2.55), (sx * 3.35, 13.2, 2.55), (sx * 3.35, 11.85, 2.55), (sx * 1.45, 11.85, 2.55)][::sx], (0, 0, 1), 0.7, 0.12, "ag")
        k.plate([(sx * 1.45, 13.2, -2.55), (sx * 3.35, 13.2, -2.55), (sx * 3.35, 11.85, -2.55), (sx * 1.45, 11.85, -2.55)][::-sx], (0, 0, -1), 0.6, 0.12, "ag")
    k.box((-0.12, 18.5, 1.9), (0.12, 23.2, 2.5), "lit")                    # light in the sternum's seam
    k.plate([(-3.2, 21.9, 2.98), (-1.25, 21.9, 2.98), (-1.25, 20.7, 2.98), (-3.2, 20.7, 2.98)], (0, 0, 1), 0.16, 0.08, "ag")   # an access panel
    k.hexp((2.1, 21.3, 2.98), (0, 0, 1), 1.0, 0.24, "ag", cap="emblem")
    for y0, y1, hw, t in ((16.15, 17.95, 3.75, 0.62), (14.55, 16.3, 3.6, 0.52), (13.25, 14.7, 3.45, 0.42)):     # the abdomen's lames
        k.plate([(-hw, y1, 1.95), (0, y1 - 0.55, 1.95), (hw, y1, 1.95), (hw - 0.1, y0 + 0.5, 1.95), (0, y0 - 0.05, 1.95), (-hw + 0.1, y0 + 0.5, 1.95)],
                (0, 0.18, 1), t, 0.16, "ag")
    # the belt, its buckle lit; the plate below it
    k.loft([oct_ring(11.75, -4.03, 4.03, -2.58, 2.58, 0.45), oct_ring(13.3, -4.03, 4.03, -2.58, 2.58, 0.45)], "ab")
    k.hexp((0, 12.5, 2.58), (0, 0, 1), 0.85, 0.36, "ag", cap="pod")
    k.plate([(-1.3, 11.8, 2.05), (1.3, 11.8, 2.05), (0.85, 10.75, 2.05), (-0.85, 10.75, 2.05)], (0, 0, 1), 0.85, 0.25, "ab", closed=True)
    # the back plate and its spine
    k.plate([(3.9, 23.3, -1.95), (-3.9, 23.3, -1.95), (-3.6, 13.6, -1.95), (3.6, 13.6, -1.95)], (0, 0, -1), 0.45, 0.3, "ab")
    k.box((-0.4, 13.9, -2.62), (0.4, 23.0, -2.3), "ap")

    # the power pack (off while they sit)
    k.to("back")
    PK = [(15.0, 2.6, -4.6), (16.0, 3.2, -5.65), (23.0, 3.4, -5.75), (23.75, 2.9, -4.9)]
    k.loft([oct_ring(y, -hw, hw, zb, -2.3, 0.7) for y, hw, zb in PK], "ab")
    zb = lambda y: float(np.interp(y, [r[0] for r in PK], [r[2] for r in PK]))
    k.box((-0.3, 17.2, zb(19) - 0.22), (0.3, 21.0, zb(19) + 0.3), "hot")  # the core, seen between its doors
    for sx in (-1, 1):
        door = [(sx * 0.5, 21.3), (sx * 2.95, 21.3), (sx * 2.8, 16.9), (sx * 0.5, 16.9)]
        k.plate([(x, y, zb(y) + 0.05) for x, y in door][::-sx], (0, 0, -1), 0.42, 0.18, "ag")
        k.box((sx * 3.2, 17.5, -5.0), (sx * 3.55, 20.9, -3.2), "lit")    # the heat sink's glow, behind its fins
        for y in (17.6, 18.5, 19.4, 20.3):
            k.box((sx * 3.3, y, -5.2), (sx * 3.9, y + 0.3, -3.0), "ag")
        k.hexp((sx * 1.65, 23.7, -3.7), (0, 1, 0), 0.78, 0.42, "ag", cap="pod", up=(0, 0, 1))    # a cell's cap
    wm = [(2.9, 22.95), (-2.9, 22.95), (-2.9, 21.65), (2.9, 21.65)]      # NARV-TECH across the top
    k.plate([(x, y, zb(y) + 0.05) for x, y in wm], (0, 0, -1), 0.3, 0.02, "ab", cap="wordmark", capuv=True)


def arm(k, left):
    k.to("left_arm" if left else "right_arm", mirror=left)
    # the pauldron: a cap over the shoulder, a lame under it, light along the join
    prof = [(-4.15, 24.0), (-5.2, 25.15), (-7.6, 25.3), (-9.6, 24.6), (-10.15, 21.7), (-8.05, 21.7)]
    k.prism(prof, "z", -3.15, 3.15, "ab", bevel=0.6, shrink=0.8, pick=lambda b, side, P: ("ap", None) if b != 1 and side in (1, 2, 3) else None)
    k.prism([(-8.05, 22.0), (-9.75, 22.0), (-10.45, 19.4), (-9.9, 19.15), (-8.05, 20.2)], "z", -2.75, 2.75, "ap", bevel=0.3, shrink=0.86)
    k.obox((-10.22, 21.72, 0), (0.13, 0, 0), (0, 0.13, 0), (0, 0, 2.5), "lit")
    if left:                                                              # the company's mark on the left shoulder
        c = np.array([-9.875, 23.15, 0.0])
        n = np.array([-(24.6 - 21.7), -(9.6 - 10.15), 0.0]); n /= np.linalg.norm(n)
        k.hexp(c + n * 0.0, n, 1.05, 0.16, "ag", cap="emblem", right=(0, 0, -1.0))
    else:                                                                 # two bars on the right
        n = np.array([-(24.6 - 21.7), -(9.6 - 10.15), 0.0]); n /= np.linalg.norm(n)
        up = np.array([-n[1], n[0], 0.0])
        for z in (-0.55, 0.55):
            k.obox(np.array([-9.875, 23.15, z]) + n * 0.02, n * 0.1, up * 0.9, (0, 0, 0.26), "lit")
    # the upper arm
    k.plate([(-7.8, 21.5, 1.95), (-4.2, 21.5, 1.95), (-4.2, 19.1, 1.95), (-7.8, 19.1, 1.95)], (0, 0, 1), 0.48, 0.25, "ab")
    k.plate([(-4.2, 21.5, -1.95), (-7.8, 21.5, -1.95), (-7.8, 19.1, -1.95), (-4.2, 19.1, -1.95)], (0, 0, -1), 0.48, 0.25, "ab")
    # the elbow: a hub outside, a blade behind
    k.hexp((-7.98, 17.55, 0.1), (-1, 0, 0), 1.35, 0.8, "ag", cap="pod")
    k.prism([(18.7, -1.95), (17.35, -3.25), (16.0, -1.95)], "x", -7.7, -4.35, "ab")
    # the vambrace
    VB = [(16.6, -8.45, 2.45, 0.7), (15.7, -8.92, 2.85, 0.95), (13.0, -8.76, 2.75, 0.9), (12.25, -8.45, 2.45, 0.7)]
    k.loft([oct_ring(y, xo, -3.98, -zh, zh, c) for y, xo, zh, c in VB], "ab")
    inlay = [(-8.8, 15.35, 1.35), (-8.8, 15.35, -1.35), (-8.8, 13.35, -1.35), (-8.8, 13.35, 1.35)]
    if left:                                                              # the wrist's screen
        k.plate(inlay, (-1, 0, 0), 0.3, 0.1, "ag", cap="screen", capuv=True)
    else:                                                                 # the plasma blade's emitter down the right forearm
        housing(k)
    for z in (-1, 1):                                                     # a strake fore and aft
        k.plate([(-8.2, 15.9, z * 2.7), (-4.6, 15.9, z * 2.7), (-4.6, 13.1, z * 2.7), (-8.2, 13.1, z * 2.7)][::z], (0, 0, z), 0.3, 0.25, "ag")


def leg(k, left):
    k.to("left_leg" if left else "right_leg", mirror=left)
    # the tasset on the hip
    k.prism([(-3.95, 11.95), (-4.65, 11.95), (-5.1, 8.5), (-4.6, 8.2), (-3.95, 9.0)], "z", -2.4, 2.4, "ab", bevel=0.3, shrink=0.88)
    k.obox((-5.03, 9.05, 0), (0.12, 0, 0), (0, 0.12, 0), (0, 0, 1.9), "lit")
    # the cuisse, keeled; the plate behind the thigh
    k.keel((-3.85, 11.45, 1.95), (-0.15, 11.45, 1.95), (-0.3, 7.55, 1.95), (-3.7, 7.55, 1.95), (0, 0, 1), 1.0, 0.35, 0.5, "ab")
    k.plate([(-0.2, 11.3, -1.95), (-3.8, 11.3, -1.95), (-3.65, 8.0, -1.95), (-0.35, 8.0, -1.95)], (0, 0, -1), 0.42, 0.25, "ab")
    # the knee: a pointed cop, a wing outside, a light under it
    k.keel((-3.95, 7.45, 1.95), (-0.05, 7.45, 1.95), (-0.3, 4.55, 1.95), (-3.7, 4.55, 1.95), (0, 0, 1), 1.6, 0.45, 0.9, "ap")
    k.hexp((-3.95, 6.0, -0.1), (-1, 0, 0), 1.2, 0.7, "ag", cap="pod")     # the knee's powered hub, and the assist's struts
    k.obox((-4.45, 7.5, -0.9), (0.16, 0, 0), (0, 1.0, 0.25), (0, -0.04, 0.16), "ah")
    k.obox((-4.45, 3.6, -1.05), (0.16, 0, 0), (0, 1.5, -0.2), (0, 0.02, 0.16), "ah")
    k.hexp((-3.95, 1.75, -1.3), (-1, 0, 0), 0.62, 0.55, "ag", cap="pod")
    k.box((-3.3, 4.28, 2.3), (-0.7, 4.5, 2.62), "lit")
    # the greave: keeled in front, a plate outside with a light down it, the calf behind
    k.keel((-3.8, 4.3, 1.95), (-0.2, 4.3, 1.95), (-0.35, 1.95, 1.95), (-3.65, 1.95, 1.95), (0, 0, 1), 0.9, 0.35, 0.4, "ab")
    k.plate([(-3.95, 4.3, 1.7), (-3.95, 4.3, -1.7), (-3.95, 2.1, -1.5), (-3.95, 2.1, 1.5)], (-1, 0, 0), 0.42, 0.2, "ab")
    k.box((-4.52, 2.5, -0.15), (-4.34, 3.9, 0.15), "lit")
    k.plate([(-0.25, 5.6, -1.95), (-3.75, 5.6, -1.95), (-3.5, 2.3, -1.95), (-0.5, 2.3, -1.95)], (0, 0, -1), 0.68, 0.45, "ab")
    # the boot: an armoured shoe, a lit welt round its sole, a heel plate
    k.prism([(0, -2.45), (0, 3.35), (0.95, 3.35), (1.75, 2.65), (2.5, 2.2), (2.5, -2.45)], "x", -4.2, 0.06, "ab")
    k.box((-4.33, 0.42, -2.3), (-4.15, 0.64, 3.25), "lit")
    k.box((-4.05, 0.42, 3.3), (-0.1, 0.64, 3.46), "lit")
    k.plate([(-0.55, 2.3, -2.42), (-3.6, 2.3, -2.42), (-3.4, 0.6, -2.42), (-0.75, 0.6, -2.42)], (0, 0, -1), 0.3, 0.15, "ag")
    k.plate([(-3.7, 1.55, 2.75), (-0.4, 1.55, 2.75), (-0.4, 0.75, 3.33), (-3.7, 0.75, 3.33)], (0, 0.6, 1), 0.2, 0.12, "ap")   # the toe cap


SPINE_X, MOUTH, SEG, TIP = -9.6, 12.75, 3.5, 1.3           # the blade's plane, the mount's mouth (y), a segment, the point
SADDLE = -9.1                                             # the saddle's outer face: the mount's parts stand on it
DEPLOY_TICKS = 12                                         # shutters 0-2, spine 2-7, plasma 7.5-11.5


def housing(k):
    """The plasma blade's arm mount on the right vambrace (the fixed part; take 5: Justin, of take 4's plain case, "the
    thing the sword comes out of ... the arm mount thing ... seems simplistic"). A saddle clamped round the forearm by
    two bands; on it, from the elbow: the breech (the plasma cell across its top, a power line in from the elbow's hub,
    cooling fins in its flanks), the barrel shroud (two rails along its top for the carriage, a vent in each flank), and
    the muzzle block (the emitter's ring in its face, the shutters hinged at its corners)."""
    X = SPINE_X
    k.plate([(-8.45, 17.3, 1.3), (-8.45, 17.3, -1.3), (-8.45, 12.8, -1.2), (-8.45, 12.8, 1.2)], (-1, 0, 0), 0.65, 0.08, "ag")
    for y, xo, zh, c in ((13.5, -8.79, 2.77, 0.9), (15.75, -8.92, 2.85, 0.95)):          # the clamp bands, over the vambrace's corners
        for sz in (-1, 1):
            path = [((xo, 1.15), (xo - 0.26, 1.15)), ((xo, zh - c), (xo - 0.26, zh - c + 0.11)), ((xo + c, zh), (xo + c - 0.11, zh + 0.26))]
            k.loft([[(i[0], y - 0.3, sz * i[1]), (i[0], y + 0.3, sz * i[1]), (o[0], y + 0.3, sz * o[1]), (o[0], y - 0.3, sz * o[1])] for i, o in path], "ag",
                   capmat="ah")
            n = np.array([-1.0, 0.0, sz * 1.0]) / math.sqrt(2)
            k.hexp(np.array([xo + c / 2, y, sz * (zh - c / 2)]) + n * 0.2, n, 0.2, 0.1, "ah", up=(0, 1, 0))      # its bolt
    S = SADDLE
    k.plate([(S, 17.2, 0.95), (S, 17.2, -0.95), (S, 15.5, -0.95), (S, 15.5, 0.95)], (-1, 0, 0), 1.15, 0.14, "ab")            # the breech
    k.plate([(S, 15.5, 0.8), (S, 15.5, -0.8), (S, 13.9, -0.8), (S, 13.9, 0.8)], (-1, 0, 0), 0.82, 0.12, "ab")                # the barrel shroud
    k.box((-10.1, MOUTH, -1.05), (S, 13.9, 1.05), "ag")                                                                      # the muzzle block
    k.plate([(-10.1, 13.8, 0.8), (-10.1, 13.8, -0.8), (-10.1, 12.85, -0.8), (-10.1, 12.85, 0.8)], (-1, 0, 0), 0.1, 0.08, "ap")
    k.hexp((X, MOUTH, 0), (0, -1, 0), 0.52, 0.05, "ag", cap="pod", up=(0, 0, 1), sink=0.1)                                   # the emitter's ring
    for sz in (-1, 1):
        k.box((-10.03, 13.92, sz * 0.36 if sz > 0 else -0.5), (-9.92, 15.48, 0.5 if sz > 0 else -0.36), "ah")                # the rails
        k.box((-10.14, MOUTH - 0.2, sz * 1.0 if sz > 0 else -1.18), (S + 0.04, MOUTH + 0.06, 1.18 if sz > 0 else -1.0), "ah")  # a hinge's knuckle
        q = [(-9.82, 15.3, sz * 0.8), (-9.28, 15.3, sz * 0.8), (-9.28, 14.15, sz * 0.8), (-9.82, 14.15, sz * 0.8)]
        k.plate(q[::sz], (0, 0, sz), 0.1, 0.05, "ag", cap="grille", capuv=True)                                              # a vent
        for y in (17.0, 15.72):
            k.hexp((-10.25, y, sz * 0.62), (-1, 0, 0), 0.15, 0.08, "ah")                                                     # the breech's bolts
    k.box((-9.98, 13.95, -0.14), (-9.92, 15.45, 0.14), "ad")                                                                 # the slot between them
    # the plasma cell across the breech's top, a ring at each end, a window along it
    cx, cy, r = -10.5, 16.35, 0.42
    hexa = [(cx + r * math.cos(math.radians(30 + 60 * i)), cy + r * math.sin(math.radians(30 + 60 * i))) for i in range(6)]
    k.loft([[(x, y, z) for x, y in hexa] for z in (-0.8, 0.8)], "ag", caps=(False, False))
    for sz in (-1, 1):
        k.hexp((cx, cy, sz * 0.8), (0, 0, sz), r, 0.1, "ah", cap="pod", up=(0, 1, 0), sink=0.0)
    k.box((cx - r * math.cos(math.radians(30)) - 0.06, cy - 0.13, -0.5), (cx - r * math.cos(math.radians(30)) + 0.02, cy + 0.13, 0.5), "lit")
    # the power line in from the elbow's hub
    k.box((-9.85, 17.15, 0.2), (-9.15, 17.4, 0.9), "ah")
    k.box((-9.75, 17.4, 0.3), (-9.25, 18.35, 0.8), "rub")
    k.box((-9.75, 18.0, 0.3), (-8.7, 18.4, 0.8), "rub")


FULL = 3 * SEG + TIP                                      # the spine's length out of the mouth
LEAD = (0.0, TIP, TIP + SEG, TIP + 2 * SEG)               # how far behind the point's end each of the spine's elements ends
FIN_OUT, CARRIAGE_RUN, SHUTTER_SWING = 0.42, 0.85, 100.0  # the fins' slide (px), the carriage's run (px), the shutters' swing (degrees)
OPEN_BY, SPINE_FROM, SPINE_BY, PLASMA_FROM, PLASMA_BY = 0.17, 0.17, 0.59, 0.62, 0.95   # the deployment's stages, as shares of it
PIVOTS = {}                                               # a moving group's pivot (the wearer's frame)


def mechanism(k):
    """The plasma blade's moving parts, each a group of its own in its pose named here; client/PowerArmor moves them
    through the deployment (DEPLOY_TICKS in all; d from 0 stowed to 1 lit):
    shutter_a / _b: the two shutters over the mouth, shut; they swing SHUTTER_SWING about their hinges (the group's
    pivot, about x) to stand as prongs either side of the blade's root (d 0 to OPEN_BY); their _hot groups are the
    emitters inside them, lit (drawn once the plasma runs);
    fins_a / _b: the cooling fins, out of the breech's flanks; stowed, FIN_OUT further in (they slide out with the
    shutters); their _hot groups are their glowing ends (the plasma running, and a second after it's stowed);
    carriage: at the top of its rails; it runs CARRIAGE_RUN towards the mouth as the spine goes out; slot_lit: the slot
    between the rails, lit behind it (drawn once it moves);
    spine_0 (the point) and spine_1 to 3 (its three telescoping segments, the narrowest leading): fully out; each is
    drawn back by FULL less how far the point's end has gone, never further in than its own nest (LEAD: d SPINE_FROM
    to SPINE_BY);
    plasma: the blade, full; it's drawn scaled along its length from the mouth (the group's pivot) as it runs up the
    spine (d PLASMA_FROM to PLASMA_BY). As the baton's rod, the plasma is held round a solid core."""
    X = SPINE_X
    for sz, side in ((-1, "a"), (1, "b")):
        hinge = np.array([X, MOUTH - 0.08, sz * 1.0])
        v, t = np.array([0.0, 0.0, -sz]), np.array([0.0, -1.0, 0.0])          # along the shutter from its hinge; out of its outer face
        PIVOTS["shutter_" + side] = PIVOTS["shutter_%s_hot" % side] = hinge
        k.to("shutter_" + side)
        k.obox(hinge + v * 0.5 + t * 0.08, (0.5, 0, 0), v * 0.5, t * 0.08, "ag")
        k.obox(hinge + v * 0.54 - t * 0.03, (0.26, 0, 0), v * 0.32, t * 0.03, "ad")          # its emitter, inside
        k.to("shutter_%s_hot" % side)
        k.obox(hinge + v * 0.54 - t * 0.045, (0.29, 0, 0), v * 0.35, t * 0.045, "hot")
        k.to("fins_" + side)
        for y in (16.98, 16.62, 16.26, 15.9):
            k.obox((-9.66, y, sz * (0.85 + (FIN_OUT + 0.15) / 2)), (0.4, 0, 0), (0, 0.07, 0), (0, 0, (FIN_OUT + 0.15) / 2), "ah")
        k.to("fins_%s_hot" % side)
        for y in (16.98, 16.62, 16.26, 15.9):
            k.obox((-9.66, y, sz * (1.0 + FIN_OUT + 0.01)), (0.43, 0, 0), (0, 0.1, 0), (0, 0, 0.04), "hot")
    k.to("carriage")
    yc = 15.12
    k.box((-10.34, yc - 0.32, -0.56), (-10.0, yc + 0.32, 0.56), "ag")
    k.box((-10.4, yc - 0.16, -0.3), (-10.34, yc + 0.16, 0.3), "ap")
    k.to("slot_lit")
    k.box((-10.02, 14.0, -0.1), (-9.98, 15.42, 0.1), "lit")
    ring = lambda y, hx, hz: [(X - hx, y, -hz), (X + hx, y, -hz), (X + hx, y, hz), (X - hx, y, hz)]
    edges = lambda mat: (lambda b, side, P: (mat, None) if side in (0, 2) else None)
    end = MOUTH - FULL
    k.to("spine_0")
    k.loft([ring(end + TIP, 0.08, 0.36), ring(end, 0.02, 0.02)], "spine", pick=edges("ah"))
    for i, hz in enumerate((0.34, 0.42, 0.5)):                                # the segments, the narrowest leading
        k.to("spine_%d" % (i + 1))
        d0, d1 = TIP + i * SEG, TIP + (i + 1) * SEG
        hx = 0.08 + 0.02 * i
        k.loft([ring(end + d1, hx, hz), ring(end + d0, hx, hz)], "spine", pick=edges("ah"))
        if i:                                                                 # its collar on the one before
            k.box((X - hx - 0.05, end + d0 - 0.02, -hz - 0.05), (X + hx + 0.05, end + d0 + 0.16, hz + 0.05), "ah")
    k.to("plasma")
    PIVOTS["plasma"] = np.array([X, MOUTH - 0.05, 0.0])
    prof = [(MOUTH - 0.05, 0.2, 0.45), (MOUTH - 1.1, 0.27, 0.8), (MOUTH - 3 * SEG, 0.24, 0.62), (MOUTH - FULL - 0.6, 0.03, 0.03)]
    k.loft([ring(y, hx, hz) for y, hx, hz in prof], "blade", pick=edges("bladeedge"))


def build():
    k = Kit()
    helmet_a(k)
    mechanism(k)
    torso(k)
    for left in (False, True):
        arm(k, left)
        leg(k, left)
    return k


REPEAT = 4                                                # a tiled material's tile is laid this many times each way on the sheet


def sheet(k):
    """Every material's texture on one sheet: a tiled one REPEAT times each way (a face's texels are taken to its
    first tiles, so nothing wraps off the sheet), one a face lays out itself (a decal, the visor, a hub) once with its
    edge texels repeated round it. Returns the sheet, the glow sheet, and where each landed: {(material, tiled): (x, y,
    w, h)}."""
    used = set()
    for g in k.g:
        for L, uv, tex, mat, n in k.quads(g):
            used.add((mat, uv is None))
    items = []
    for mat, tiled in sorted(used):
        t, g = MATS[mat]
        g = np.zeros_like(t) if g is None else g
        if tiled:
            assert t.shape[0] == t.shape[1], mat
            a, b = np.tile(t, (REPEAT, REPEAT, 1)), np.tile(g, (REPEAT, REPEAT, 1))
        else:
            a, b = np.pad(t, ((1, 1), (1, 1), (0, 0)), mode="edge"), np.pad(g, ((1, 1), (1, 1), (0, 0)), mode="edge")
        items.append(((mat, tiled), a, b))
    items.sort(key=lambda it: (-it[1].shape[0], -it[1].shape[1], it[0]))
    W = 512
    x = y = row = 0
    where, placed = {}, []
    for key, a, b in items:
        h, w = a.shape[:2]
        if x + w > W:
            x, y, row = 0, y + row, 0
        placed.append((x, y, a, b))
        where[key] = (x, y, w, h) if key[1] else (x + 1, y + 1, w - 2, h - 2)
        x += w
        row = max(row, h)
    H = 1
    while H < y + row:
        H *= 2
    img, glow = np.zeros((H, W, 4), np.float32), np.zeros((H, W, 4), np.float32)
    glow[..., 3] = 1
    for x, y, a, b in placed:
        img[y:y + a.shape[0], x:x + a.shape[1], :3] = a[..., :3]
        img[y:y + a.shape[0], x:x + a.shape[1], 3] = 1
        glow[y:y + a.shape[0], x:x + a.shape[1], :3] = b[..., :3]
    return img, glow, where


def write_all():
    k = build()
    img, glow, where = sheet(k)
    H, W = img.shape[:2]
    groups = []
    for g in k.g:
        pivot = np.zeros(3)
        if g in PIVOTS:
            px, py, pz = PIV[BONE[g]]
            P = PIVOTS[g]
            pivot = np.array([P[0] - px, (24 - P[1]) - py, -P[2] - pz])
        quads = []
        for L, uv, tex, mat, n in k.quads(g):
            t = MATS[mat][0]
            if uv is None:
                x0, y0, w, h = where[mat, True]
                tile = t.shape[0]
                if np.ptp(t.reshape(-1, 4), axis=0).max() == 0:               # one flat colour: any texel of it
                    tex = np.full((4, 2), tile * REPEAT / 2.0)
                else:
                    tex = tex - np.floor(tex.min(0) / tile + 1e-6) * tile      # to the first tiles
                    assert tex.max() <= tile * REPEAT + 1e-6, (g, mat, tex.max())
                st = (np.array([x0, y0]) + tex) / np.array([W, H])
            else:
                x0, y0, w, h = where[mat, False]
                st = (np.array([x0, y0]) + np.clip(uv, 0, 1) * np.array([w, h])) / np.array([W, H])
            face = np.cross(L[2] - L[0], L[3] - L[1])
            assert face @ n > 0, (g, mat)                                     # wound as the game's culling takes it
            quads.append((L - pivot, st, n, MATS[mat][1] is not None))
        groups.append((g, pivot, quads))
    raw = bytearray()
    raw += struct.pack(">ii", 0x4A435331, 2)
    raw += struct.pack(">i", 1)
    name = "armor".encode()
    raw += struct.pack(">H", len(name)) + name
    raw += struct.pack(">i", len(groups))
    for g, pivot, quads in groups:
        b = g.encode()
        raw += struct.pack(">H", len(b)) + b
        raw += struct.pack(">fffi", float(pivot[0]), float(pivot[1]), float(pivot[2]), len(quads))
        for L, st, n, lit in quads:
            raw += struct.pack(">BB", 0, 1 if lit else 0)
            nb = [int(round(max(-1.0, min(1.0, float(v))) * 127)) for v in n]
            for c in range(4):
                raw += struct.pack(">fffff", float(L[c, 0]), float(L[c, 1]), float(L[c, 2]), float(st[c, 0]), float(st[c, 1]))
                raw += struct.pack(">bbb", *nb)
    MESH.parent.mkdir(parents=True, exist_ok=True)
    SHEET.parent.mkdir(parents=True, exist_ok=True)
    with open(MESH, "wb") as f:
        with gzip.GzipFile(fileobj=f, mode="wb", mtime=0) as z:
            z.write(bytes(raw))
    Image.fromarray((np.clip(img, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA").save(SHEET, optimize=True)
    # (the glow sheet's brightness as its opacity: the eyes layer lays it on by that; painted bright on black it drew
    # its black over the armour: tools/glow_alpha.py)
    import glow_alpha
    Image.fromarray(glow_alpha.carried(glow), "RGBA").save(SHEET_GLOW, optimize=True)
    return sum(len(q) for g, p, q in groups), len(groups), (W, H)


if __name__ == "__main__":
    n, g, size = write_all()
    print(f"power armour: {n} faces in {g} groups, a {size[0]} x {size[1]} sheet")
