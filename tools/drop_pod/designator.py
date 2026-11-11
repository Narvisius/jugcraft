"""The laser designator (mockup, Justin 7 November 2026): a handheld target designator, a rubber-armoured gunmetal box
with the big receiving objective and the laser's own aperture in its front bezel, the eyepiece and a live display at
the back, a rail on top, a pistol grip with the mark trigger under it, grey trim down its sides; black and grey only
(Justin).
In its own frame: blocks, x right, y up, the lens end at -z; the body's middle at the origin."""
import math
import os
import sys
S = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, S)
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import pod as P
import pods2 as P2
from pod import Model, UV1

UP = np.array([0, 1.0, 0])
FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
L = 0.46                                   # the body's length
W, H = 0.20, 0.15


def _mats():
    if "rubber" in P._T:
        return
    n = 32
    t = P._base((38, 40, 44), n, 0.10, 61)
    rng = np.random.default_rng(3)
    dots = rng.random((n, n)) > 0.82
    t[dots, :3] *= 0.8                                          # the armour's stipple
    P._T["rubber"] = t
    t = P._base((70, 73, 79), n, 0.05, 63)
    t[0:1, :, :3] *= 0.7; t[:, 0:1, :3] *= 0.7
    P._T["metal"] = t
    t = np.ones((4, 4, 4), np.float32); t[..., :3] = [0.62, 0.32, 1.0]
    P._T["purple"] = t
    P.GLOWS.add("purple")
    n = 48
    t = np.ones((n, n, 4), np.float32)
    yy, xx = np.mgrid[0:n, 0:n] / (n - 1)
    r = np.hypot(xx - 0.5, yy - 0.5) * 2
    t[..., 0] = 0.05 + 0.10 * (1 - r); t[..., 1] = 0.08 + 0.12 * (1 - r); t[..., 2] = 0.12 + 0.22 * (1 - r)
    hl = np.exp(-((xx - 0.33) ** 2 + (yy - 0.30) ** 2) / 0.006)                 # the glint on the glass
    t[..., :3] += hl[..., None] * 0.7
    t[r > 0.98, :3] = 0.03
    P._T["lens"] = t
    t = P._T["lens"].copy(); t[..., 0] += 0.25 * (1 - np.clip(r, 0, 1)); t[..., 2] += 0.15 * (1 - np.clip(r, 0, 1))
    P._T["laserlens"] = t
    # the back's display: the support picked, the range to the mark, a little reticle
    im = Image.new("RGB", (192, 128), (6, 10, 12))
    d = ImageDraw.Draw(im)
    f1, f2 = ImageFont.truetype(FB, 22), ImageFont.truetype(FB, 15)
    d.rectangle((3, 3, 188, 124), outline=(32, 44, 48), width=2)
    d.text((10, 8), "SQUAD 5", font=f1, fill=(255, 186, 116))
    d.text((10, 38), "RNG 0612m", font=f2, fill=(195, 219, 220))
    d.text((10, 60), "ACCT  x1", font=f2, fill=(91, 227, 138))
    d.ellipse((132, 64, 176, 108), outline=(95, 212, 232), width=2)
    d.line((154, 58, 154, 114), fill=(95, 212, 232), width=2); d.line((126, 86, 182, 86), fill=(95, 212, 232), width=2)
    d.text((10, 96), "LASE", font=f2, fill=(255, 91, 77))
    t = np.ones((128, 192, 4), np.float32); t[..., :3] = np.asarray(im, np.float32) / 255
    P._T["ltdscreen"] = t
    P.GLOWS.add("ltdscreen")


def designator():
    _mats()
    m = Model()
    # the body: a rubber-armoured box, its edges rounded, bevelled at the back, the front bezel standing proud
    poly = lambda s: P2.rrect(W * s, H * s, 0.03 * s, seg=2)
    prof = [(0.0, 0.86), (0.015, 0.96), (0.03, 1.0), (0.395, 1.0), (0.395, 1.07), (0.46, 1.07)]
    mats = ["metal", "rubber", "rubber", "metal", "metal"]
    body = Model()
    P2.loft(body, [(t, poly(s)) for t, s in prof], mats, caps=(True, True), cap_mats=("metal", "metal"), tile=0.25)
    # the loft runs up y: turn it so its axis runs to -z (y -> -z, the outline's depth -> up), the middle at the origin
    T = np.array([[1, 0, 0], [0, 0, 1], [0, -1, 0.0]])
    m.faces += [(Pq @ T.T + np.array([0, 0, L / 2]), UV, mm, gl) for Pq, UV, mm, gl in body.faces]
    zf = -L / 2 - 0.0                                       # the front face
    zb = L / 2                                              # the back face
    ax = np.array([0, 0, -1.0])
    def ring(c, r0, r1, depth, mat="metal"):
        """A lens's bezel ring standing out of the front face, its glass set back inside it."""
        m.lathe([(0.0, r1), (depth, r1), (depth, r0), (0.004, r0)], 20, 0.0, [mat, mat, mat], origin=c, axis=ax, ref=(1.0, 0, 0), tile=0.1)
    # the front: the receiving objective (big), the laser's aperture, the rangefinder's receiver
    for (cx, cy, r, lens) in ((-0.040, -0.005, 0.058, "lens"), (0.062, 0.034, 0.024, "lens"), (0.062, -0.036, 0.020, "lens")):
        c = np.array([cx, cy, zf])
        ring(c, r, r + 0.012, 0.018)
        g = c + ax * 0.004
        e1, e2 = np.array([1.0, 0, 0]), np.array([0, 1.0, 0])
        for k in range(20):
            a0, a1 = 2 * math.pi * k / 20, 2 * math.pi * (k + 1) / 20
            p0, p1 = g + (e1 * math.cos(a0) + e2 * math.sin(a0)) * r, g + (e1 * math.cos(a1) + e2 * math.sin(a1)) * r
            uv = [(0.5, 0.5), (0.5 + 0.5 * math.cos(a0), 0.5 - 0.5 * math.sin(a0)), (0.5 + 0.5 * math.cos(a1), 0.5 - 0.5 * math.sin(a1))]
            m.quad([g, p0, p1, p1], uv + [uv[-1]], lens, ax)
    # the back: the eyepiece with its rubber eyecup, the live display beside it
    ec = np.array([-0.045, 0.012, zb])
    m.lathe([(0.0, 0.030), (0.035, 0.030), (0.040, 0.037), (0.060, 0.039), (0.062, 0.030)], 16, 0.0,
            ["metal", "rubber", "rubber", "rubber"], origin=ec, axis=-ax, ref=(1.0, 0, 0), tile=0.1, caps=(False, True), cap_mats=(None, "lens"))
    sc = np.array([0.045, 0.004, zb + 0.006])
    m.quad([sc + np.array([-0.038, 0.025, 0]), sc + np.array([0.038, 0.025, 0]), sc + np.array([0.038, -0.025, 0]), sc + np.array([-0.038, -0.025, 0])],
           UV1, "ltdscreen", (0, 0, 1))
    for (x0, x1, y0, y1) in ((-0.044, 0.044, 0.025, 0.031), (-0.044, 0.044, -0.031, -0.025), (-0.044, -0.038, -0.025, 0.025), (0.038, 0.044, -0.025, 0.025)):
        m.box((0.045 + x0, 0.004 + y0, zb - 0.002), (0.045 + x1, 0.004 + y1, zb + 0.012), "metal")
    # the top rail (its teeth), a stencilled name on the top front
    m.box((-0.026, H / 2, -0.12), (0.026, H / 2 + 0.010, 0.12), "metal")
    for z in np.arange(-0.11, 0.115, 0.022):
        m.box((-0.030, H / 2 + 0.010, z - 0.006), (0.030, H / 2 + 0.017, z + 0.006), "metal")
    d0 = np.array([0.0, H / 2 + 0.006, -0.145])
    m.quad([d0 + np.array([-0.06, 0, -0.018]), d0 + np.array([0.06, 0, -0.018]), d0 + np.array([0.06, 0, 0.018]), d0 + np.array([-0.06, 0, 0.018])],
           [(0, 0), (1, 0), (1, 1), (0, 1)], P.decal("LTD-7", 160, 48, 28, (200, 200, 196), bg="metal"), UP)
    # the purple strips down both sides, the side buttons (left), a recessed panel each side
    for s_ in (-1, 1):
        xa, xb = s_ * (W / 2 - 0.001), s_ * (W / 2 + 0.004)
        m.box((min(xa, xb), 0.0, -0.16), (max(xa, xb), 0.008, 0.16), "metal")
        xa, xb = s_ * (W / 2 - 0.001), s_ * (W / 2 + 0.005)
        m.box((min(xa, xb), -0.050, -0.10), (max(xa, xb), -0.018, 0.10), "metal")
    for k, zz in enumerate((0.06, 0.10, 0.14)):
        m.lathe([(0.0, 0.009), (0.006, 0.009)], 10, 0.0, ["metal"], origin=(-W / 2, 0.035, zz), axis=(-1, 0, 0),
                ref=(0, 1.0, 0), caps=(False, True), cap_mats=(None, "dark" if k else "metal"))
    # the pistol grip under the back half (raked back), the mark trigger and its guard, a lanyard loop
    rake = math.radians(-14)
    Rg = np.array([[1, 0, 0], [0, math.cos(rake), -math.sin(rake)], [0, math.sin(rake), math.cos(rake)]])
    m.box((-0.024, -0.12, -0.032), (0.024, 0.0, 0.032), "rubber", rot=Rg, at=(0, -H / 2 + 0.005, 0.11))
    m.box((-0.026, -0.124, -0.035), (0.026, -0.112, 0.035), "metal", rot=Rg, at=(0, -H / 2 + 0.005, 0.11))
    m.box((-0.008, -0.040, -0.006), (0.008, 0.0, 0.006), "dark", rot=Rg, at=(0, -H / 2, 0.055))
    m.box((-0.006, -0.058, -0.004), (0.006, -0.050, 0.060), "metal", at=(0, -H / 2, 0.025))
    m.box((-0.006, -0.058, -0.004), (0.006, 0.0, 0.004), "metal", at=(0, -H / 2, 0.025))
    m.box((-0.004, -0.030, 0.19), (0.004, 0.0, 0.23), "metal", at=(W / 2 - 0.02, -H / 2, 0))
    return m


def parts():
    """The designator taken apart for the parts tile: (name, model, offset) of each piece in the designator's frame."""
    m = designator()
    groups = {"BODY": [], "OPTICS": [], "EYEPIECE + DISPLAY": [], "GRIP + TRIGGER": [], "RAIL": []}
    for f in m.faces:
        c = f[0].mean(0)
        if c[2] < -L / 2 + 0.002:
            groups["OPTICS"].append(f)
        elif c[2] > L / 2 - 0.001:
            groups["EYEPIECE + DISPLAY"].append(f)
        elif c[1] < -H / 2 + 0.001 and abs(c[0]) < 0.05:
            groups["GRIP + TRIGGER"].append(f)
        elif c[1] > H / 2 + 0.001 and abs(c[0]) < 0.04:
            groups["RAIL"].append(f)
        else:
            groups["BODY"].append(f)
    out = []
    for k, fs in groups.items():
        mm = Model(); mm.faces = fs
        out.append((k, mm))
    return out
