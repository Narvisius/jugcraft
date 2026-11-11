"""More drop pod hulls (mockup sheet 2, Justin 7 November 2026: "give me a few more options"): D brick, E ball,
F spike, G cage, H tri, I heavy twin-bay. Same frame and materials as pod.py: blocks, y up from the pod's lowest point,
the hatch facing -z; every hatch on explosive bolts in a hazard frame."""
import math
import os
import sys
S = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, S)
import numpy as np
import pod as P
from pod import Model, UV1, bolt, nozzle, decal

UP = np.array([0, 1.0, 0])


# ------------------------------------------------------------------------------------------------ outlines
def rrect(w, d, r, seg=3):
    """A rounded rectangle in (x, z), w across x, d deep along z, corner radius r (seg + 1 points a corner); its last
    segment (index N - 1) is the front flat, facing -z."""
    pts = []
    for (cx, cz, a0) in ((w / 2 - r, -(d / 2 - r), -90), (w / 2 - r, d / 2 - r, 0), (-(w / 2 - r), d / 2 - r, 90),
                         (-(w / 2 - r), -(d / 2 - r), 180)):
        for i in range(seg + 1):
            a = math.radians(a0 + 90 * i / seg)
            pts.append((cx + r * math.cos(a), cz + r * math.sin(a)))
    return np.array(pts)


def rtri(ri, r, seg=4):
    """A rounded equilateral triangle in (x, z): ri from the centre to each flat, corner radius r; the front flat (its
    last segment) faces -z, the third corner at the back (+z)."""
    pts = []
    for (ca, a0) in ((330, 270), (90, 30), (210, 150)):
        cc = np.array([math.cos(math.radians(ca)), math.sin(math.radians(ca))]) * 2 * (ri - r)
        for i in range(seg + 1):
            a = math.radians(a0 + 120 * i / seg)
            pts.append(cc + r * np.array([math.cos(a), math.sin(a)]))
    return np.array(pts)


def loft(m, rings, mats, face_mat=None, caps=(False, False), cap_mats=(None, None), tile=1.0, inside=False):
    """A hull through rings [(y, outline (N, 2) in x, z)], the same N each: a band of faces between each pair. mats: one
    per band; face_mat(k, band) may override (or "skip")."""
    N = len(rings[0][1])
    vs = [0.0]
    rad = []
    for y, pts in rings:
        c = pts.mean(0)
        rad.append(float(np.mean(np.linalg.norm(pts - c, axis=1))))
    for i in range(len(rings) - 1):
        vs.append(vs[-1] + math.hypot(rings[i + 1][0] - rings[i][0], rad[i + 1] - rad[i]))
    for b in range(len(rings) - 1):
        (y0, p0), (y1, p1) = rings[b], rings[b + 1]
        r0, r1 = rad[b], rad[b + 1]
        u0 = 0.0
        for k in range(N):
            j = (k + 1) % N
            A = np.array([p1[k][0], y1, p1[k][1]]); B = np.array([p1[j][0], y1, p1[j][1]])
            C = np.array([p0[j][0], y0, p0[j][1]]); D = np.array([p0[k][0], y0, p0[k][1]])
            wt, wb = np.linalg.norm(B - A), np.linalg.norm(C - D)
            w = max(wt, wb, 1e-6)
            UV = [(u0 + (w - wt) / 2, -vs[b + 1]), (u0 + (w + wt) / 2, -vs[b + 1]), (u0 + (w + wb) / 2, -vs[b]), (u0 + (w - wb) / 2, -vs[b])]
            UV = [(u / tile, v / tile) for u, v in UV]
            u0 += w
            mm = mats[b] if isinstance(mats, (list, tuple)) else mats
            if face_mat:
                mm = face_mat(k, b) or mm
            if mm == "skip":
                continue
            cen = (A + B + C + D) / 4
            ax = np.array([(p0.mean(0)[0] + p1.mean(0)[0]) / 2, cen[1], (p0.mean(0)[1] + p1.mean(0)[1]) / 2])
            radial = cen - ax; radial[1] = 0
            nr = np.linalg.norm(radial)
            if abs(y1 - y0) < 1e-9:
                ref = UP * (1 if r1 < r0 else -1)
            else:
                ref = radial / max(nr, 1e-9) + UP * (r0 - r1) / (y1 - y0)
            m.quad([A, B, C, D], UV, mm, -ref if inside else ref)
    for which, (y, pts) in ((0, rings[0]), (1, rings[-1])):
        if caps[which]:
            c = np.array([pts.mean(0)[0], y, pts.mean(0)[1]])
            mm = cap_mats[which] or (mats[0] if isinstance(mats, (list, tuple)) else mats)
            for k in range(N):
                j = (k + 1) % N
                a, b_ = np.array([pts[k][0], y, pts[k][1]]), np.array([pts[j][0], y, pts[j][1]])
                UV = [(0.5, 0.5), (0.5 + pts[k][0] / 3, 0.5 + pts[k][1] / 3), (0.5 + pts[j][0] / 3, 0.5 + pts[j][1] / 3)] * 1
                m.quad([c, a, b_, b_], UV + [UV[-1]], mm, UP * (-1 if which == 0 else 1))


def flat_frame(a2, b2, y):
    """A vertical flat between outline points a2, b2 (x, z) at height y: (its centre, right as seen from outside, up,
    the outward normal)."""
    a, b = np.array([a2[0], y, a2[1]]), np.array([b2[0], y, b2[1]])
    c = (a + b) / 2
    t = b - a; t /= np.linalg.norm(t)
    n = np.cross(t, UP)
    if n @ np.array([c[0], 0, c[2]]) < 0:
        n = -n
    return c, np.cross(UP, n), UP.copy(), n


# ------------------------------------------------------------------------------------------------ shared parts
def wrap_decal(m, tex, pt, nn, n_sub=8):
    """A marking painted round a curved hull: pt(u, v) the surface point (u 0..1 left to right as seen, v 0..1 top to
    bottom), nn(u, v) its normal; drawn as n_sub strips."""
    us = np.linspace(0, 1, n_sub + 1)
    for u0, u1 in zip(us, us[1:]):
        m.quad([pt(u0, 0), pt(u1, 0), pt(u1, 1), pt(u0, 1)], [(u0, 0), (u1, 0), (u1, 1), (u0, 1)], tex, nn((u0 + u1) / 2, 0.5))


def hatch(m, c, right, up, nrm, dw, dh, number="07", fw=0.12, lift=0.07, warn="EXPLOSIVE BOLTS", nbolts=4, rails=True,
          extra=None):
    """A flat hatch in its hazard-striped surround standing off the hull, explosive bolts down both sides, its number,
    the warning, a pull bar and grab rails beside it (as the hex lander's)."""
    for (a0, a1, b0, b1) in ((-dw / 2 - fw, -dw / 2, -dh / 2 - fw, dh / 2 + fw), (dw / 2, dw / 2 + fw, -dh / 2 - fw, dh / 2 + fw),
                             (-dw / 2, dw / 2, dh / 2, dh / 2 + fw), (-dw / 2, dw / 2, -dh / 2 - fw, -dh / 2)):
        m.prism([(a0, b0), (a1, b0), (a1, b1), (a0, b1)], (c + nrm * lift / 2, right, up), lift, "hazard", edge="armor2", tile=0.5)
    P0 = c + nrm * (lift - 0.02)
    m.prism([(-dw / 2, -dh / 2), (dw / 2, -dh / 2), (dw / 2, dh / 2), (-dw / 2, dh / 2)], (P0 - nrm * 0.05, right, up), 0.10, "door",
            edge="armor2")
    m.box((-0.15, -0.04, 0.0), (0.15, 0.04, 0.05), "steel", rot=np.stack([right, up, nrm], 1), at=P0 - up * 0.05)
    labels = [(number, 128, 64, 40, (222, 222, 214), dh * 0.28, min(0.30, dw * 0.3), 0.16)]
    if warn:
        labels.append((warn, 256, 48, 22, (236, 186, 40), -dh * 0.23, min(0.42, dw * 0.42), 0.09 * min(0.42, dw * 0.42) / 0.42))
    for (txt, w, h, size, rgb, b, ww, hh) in labels + list(extra or []):
        d0 = P0 + up * b + nrm * 0.008
        m.quad([d0 - right * ww + up * hh, d0 + right * ww + up * hh, d0 + right * ww - up * hh, d0 - right * ww - up * hh], UV1,
               decal(txt, w, h, size, rgb), nrm)
    for s_ in (-1, 1):
        for b in np.linspace(-dh / 2 + 0.15, dh / 2 - 0.15, nbolts):
            bolt(m, c + right * s_ * (dw / 2 + fw / 2) + up * b + nrm * lift, nrm, r=0.036, h=0.05)
    if rails:
        for s_ in (-1, 1):
            a = s_ * (dw / 2 + fw + 0.12)
            mid = c + right * a + nrm * 0.10 + up * 0.05
            m.box((-0.02, -0.5, -0.02), (0.02, 0.5, 0.02), "steel", rot=np.stack([right, up, nrm], 1), at=mid)
            for e in (-0.48, 0.48):
                m.box((-0.02, -0.02, -0.10), (0.02, 0.02, 0.0), "steel", rot=np.stack([right, up, nrm], 1), at=mid + up * e)


def plate(m, c, right, up, nrm, w, h, text=None, sub="ORBITAL INSERTION", tone="armor3", lift=0.035, thick=0.05):
    """A raised armour plate (bevelled edge), with a stencilled marking on it if text."""
    m.prism([(-w / 2, -h / 2), (w / 2, -h / 2), (w / 2, h / 2), (-w / 2, h / 2)], (c + nrm * lift, right, up), thick, tone, edge="armor2")
    if text:
        d0 = c + nrm * (lift + thick / 2 + 0.008)
        ww, hh = min(0.42, w * 0.46), min(0.16, h * 0.4)
        m.quad([d0 - right * ww + up * hh, d0 + right * ww + up * hh, d0 + right * ww - up * hh, d0 - right * ww - up * hh], UV1,
               decal(text, 192, 64, 34, sub=sub), nrm)


def motor(m, out, throat_r, y, cant=22, size=1.0, housing=(0.34, 0.38, 0.24), hm="skirt"):
    """A retro motor: its housing on the hull and the bell under it, canted out (degrees) along the horizontal `out`."""
    out = np.asarray(out, float); out = out / np.linalg.norm(out)
    tang = np.cross(UP, out)
    hw, hh, hd = housing
    hc = out * (throat_r - 0.02) + UP * (y + 0.16)
    m.box((-hw / 2, -hh / 2, -hd / 2), (hw / 2, hh / 2, hd / 2), hm, rot=np.stack([tang, UP, out], 1), at=hc)
    d = -UP * math.cos(math.radians(cant)) + out * math.sin(math.radians(cant))
    e = nozzle(m, out * throat_r + UP * y, d, r_throat=0.085 * size, r_exit=0.19 * size, length=0.40 * size)
    m.thrusters.append((e, d))


def beacon(m, y, r=0.10):
    m.lathe([(y, r), (y + 0.12, r), (y + 0.16, r * 0.6)], 8, 0.0, ["dark", "beacon"], caps=(False, True), cap_mats=(None, "beacon"))
    m.lamps.append((np.array([0, y + 0.14, 0]), (1.0, 0.25, 0.15)))


def base_rings(poly, top):
    """The rings every flat-sided form shares: the ablative shield, the steel rim, the motor skirt, the straight body to
    the shoulder band; `top` adds the roof [(y, scale)]. poly(s) gives the outline at scale s."""
    prof = [(0.0, 0.55), (0.10, 0.86), (0.22, 1.0), (0.22, 1.03), (0.32, 1.03), (0.32, 1.0), (0.95, 0.98), (3.12, 0.98),
            (3.12, 1.0), (3.22, 1.0), (3.22, 0.98)] + list(top)
    mats = ["shield", "shield", "band", "band", "band", "skirt", "char", "band", "band", "band"]
    return [(y, poly(s)) for y, s in prof], mats


# ------------------------------------------------------------------------------------------------ D: brick
def brick_pod():
    """D: the brick. A square armoured box, its corners chamfered, 2.2 across the body (3 x 3 cells with the motor
    towers), 3.8 high: four corner towers carry the big retro motors, the whole front face is the hatch's."""
    m = Model(); m.thrusters = []
    W, Rc = 2.2, 0.30
    poly = lambda s: rrect(W * s, W * s, Rc * s, seg=1)
    rings, mats = base_rings(poly, [(3.60, 0.74), (3.68, 0.68)])
    mats += ["armor", "band"]
    loft(m, rings, mats, caps=(True, True), cap_mats=("shield", "armor3"))
    body = poly(0.98)
    N = len(body)
    # raised plates on the three plain faces, two a face; the marking on the side faces
    for k in range(N):
        if k % 2 == 0 or k == N - 1:
            continue
        for (yb, yt, txt) in ((1.10, 2.02, None), (2.10, 3.00, "DP-07" if k in (1, 5) else None)):
            c, r_, u_, n_ = flat_frame(body[k], body[(k + 1) % N], (yb + yt) / 2)
            plate(m, c, r_, u_, n_, 1.26, yt - yb, text=txt, tone="armor" if (k + int(yb > 2)) % 4 == 1 else "armor3")
    # the hatch over the front face
    c, r_, u_, n_ = flat_frame(body[N - 1], body[0], 2.025)
    hatch(m, c, r_, u_, n_, 1.0, 1.95)
    m.door_frame = (c, r_, u_, n_)
    # the corner towers: a motor housing low down, the armoured fairing up the corner, a sloped top
    for k in range(4):
        a = math.radians(45 + 90 * k)
        out = np.array([math.cos(a), 0, math.sin(a)])
        tang = np.cross(UP, out)
        Rm = np.stack([tang, UP, out], 1)
        dist = (W * 0.98 / 2 - Rc * 0.98) * math.sqrt(2) + Rc * 0.98
        m.box((-0.19, 0.55, -0.12), (0.19, 0.98, 0.20), "skirt", rot=Rm, at=out * dist)
        m.box((-0.15, 0.98, -0.10), (0.15, 2.05, 0.12), "armor2", rot=Rm, at=out * dist)
        m.prism([(-0.10, 2.05), (0.12, 2.05), (-0.10, 2.40)], (out * dist, out, UP), 0.30, "armor2", edge="armor2")
        for yy in (1.30, 1.75):
            m.box((-0.155, yy, -0.10), (0.155, yy + 0.05, 0.125), "band", rot=Rm, at=out * dist)
        d = -UP * math.cos(math.radians(22)) + out * math.sin(math.radians(22))
        m.thrusters.append((nozzle(m, out * (dist + 0.04) + UP * 0.58, d, r_throat=0.10, r_exit=0.22, length=0.44), d))
    # the roof: a lifting eye, the drogue hatch, the beacon, an antenna
    m.box((-0.30, 3.68, -0.30), (0.30, 3.72, 0.30), "armor3")
    for s_ in (-1, 1):
        m.box((s_ * 0.36 - 0.04, 3.68, 0.38), (s_ * 0.36 + 0.04, 3.86, 0.46), "steel")
    m.box((-0.40, 3.82, 0.38), (0.40, 3.88, 0.46), "steel")
    beacon(m, 3.72)
    m.box((0.50, 3.68, -0.52), (0.53, 4.30, -0.49), "steel")
    return m


# ------------------------------------------------------------------------------------------------ E: ball
def ball_pod():
    """E: the ball. A 2.9 armoured sphere (the soldier stands at its middle, the harness hung from the top) riding in a
    ring of eight motors; the hatch is three panels of the sphere, blown out together."""
    m = Model(); m.thrusters = []
    Rs, yc, n = 1.45, 1.72, 16
    ph = math.radians(-11.25)
    E = [-90, -76, -60, -44, -26.6, -12, 0, 14, 28, 40, 50, 62, 74, 84]
    prof = [(yc + Rs * math.sin(math.radians(e)), Rs * math.cos(math.radians(e))) for e in E]
    door_k = {15, 0, 1}
    b_lo, b_hi = E.index(-26.6), E.index(50)
    def fm(k, b):
        if b_lo <= b < b_hi and k in door_k:
            return "door"
        if E[b] >= 0:
            return "armor" if k % 2 else "armor3"
        if E[b] >= -44:
            return "char"
        return "shield"
    m.lathe(prof, n, ph, ["armor"] * (len(E) - 1), face_mat=fm, caps=(False, True), cap_mats=(None, "dark"))
    def surf(e, a, lift=0.0):
        r = (Rs + lift) * math.cos(e)
        return np.array([-math.sin(a) * r, yc + (Rs + lift) * math.sin(e), -math.cos(a) * r])
    def nrm(e, a):
        return np.array([-math.sin(a) * math.cos(e), math.sin(e), -math.cos(a) * math.cos(e)])
    # the belt round the equator (stopping at the hatch), the motor ring under it and its struts
    m.lathe([(yc - 0.09, Rs * 1.012), (yc + 0.09, Rs * 1.012)], n, ph, ["band"], face_mat=lambda k, b: "skip" if k in door_k else None)
    m.lathe([(0.60, 1.24), (0.60, 1.58), (0.86, 1.58), (0.94, 1.46), (0.94, 1.24)], n, ph, ["skirt", "skirt", "skirt", "skirt"])
    m.lathe([(0.60, 1.24), (0.94, 1.24)], n, ph, ["skirt"], inside=True)
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        out = np.array([-math.sin(a), 0, -math.cos(a)])
        tang = np.cross(UP, out)
        m.box((-0.05, -0.05, 0.0), (0.05, 0.05, 0.52), "dark", rot=np.stack([tang, UP, out], 1), at=out * 0.80 + UP * 0.62)
        d = -UP * math.cos(math.radians(20)) + out * math.sin(math.radians(20))
        m.box((-0.13, -0.08, -0.10), (0.13, 0.04, 0.10), "skirt", rot=np.stack([tang, UP, out], 1), at=out * 1.42 + UP * 0.60)
        m.thrusters.append((nozzle(m, out * 1.42 + UP * 0.56, d, r_throat=0.07, r_exit=0.16, length=0.30), d))
    # the hatch's hazard edge on the sphere, the bolts outside it, its number
    def strip(pts, nn):
        a_, b_, c_, d_ = pts
        L = np.linalg.norm(b_ - a_)
        m.quad([a_, b_, c_, d_], [(0, 0), (L * 4, 0), (L * 4, 0.4), (0, 0.4)], "hazard", nn)
    e0, e1 = math.radians(-26.6), math.radians(50)
    a_edge = math.radians(33.75)
    es = np.linspace(e0, e1, 9)
    for s_ in (-1, 1):
        for ea, eb in zip(es, es[1:]):
            w0, w1 = 0.05 / (Rs * math.cos(ea)), 0.05 / (Rs * math.cos(eb))
            a = s_ * a_edge
            strip([surf(eb, a - w1, 0.014), surf(eb, a + w1, 0.014), surf(ea, a + w0, 0.014), surf(ea, a - w0, 0.014)], nrm((ea + eb) / 2, a))
        for ee in np.linspace(e0 + 0.12, e1 - 0.10, 4):
            a = s_ * (a_edge + 0.09 / (Rs * math.cos(ee)))
            bolt(m, surf(ee, a, 0.0), nrm(ee, a), r=0.036, h=0.06)
    ass = np.linspace(-a_edge, a_edge, 7)
    for ee in (e0, e1):
        we = 0.05 / Rs
        for a0, a1 in zip(ass, ass[1:]):
            strip([surf(ee + we, a0, 0.014), surf(ee + we, a1, 0.014), surf(ee - we, a1, 0.014), surf(ee - we, a0, 0.014)], nrm(ee, (a0 + a1) / 2))
    o = surf(math.radians(24), 0.0, 0.016)
    nn = nrm(math.radians(24), 0.0); rr = np.cross(UP, nn); rr /= np.linalg.norm(rr); uu = np.cross(nn, rr)
    m.quad([o - rr * 0.22 + uu * 0.12, o + rr * 0.22 + uu * 0.12, o + rr * 0.22 - uu * 0.12, o - rr * 0.22 - uu * 0.12], UV1,
           decal("07", 128, 64, 40), nn)
    o = surf(math.radians(-14), 0.0, 0.016)
    nn = nrm(math.radians(-14), 0.0); rr = np.cross(UP, nn); rr /= np.linalg.norm(rr); uu = np.cross(nn, rr)
    m.quad([o - rr * 0.36 + uu * 0.07, o + rr * 0.36 + uu * 0.07, o + rr * 0.36 - uu * 0.07, o - rr * 0.36 - uu * 0.07], UV1,
           decal("EXPLOSIVE BOLTS", 256, 48, 22, (236, 186, 40)), nn)
    m.door_frame = (surf(math.radians(12), 0, 0), np.array([-1.0, 0, 0]), UP.copy(), np.array([0, 0, -1.0]))
    # the number plates on its flanks, the drogue can and beacon on top, a grab handle over the hatch
    for a in (math.radians(90), math.radians(-90), math.radians(180)):
        e = math.radians(22)
        c = surf(e, a, 0.0); nn = nrm(e, a); rr = np.cross(UP, nn); rr /= np.linalg.norm(rr); uu = np.cross(nn, rr)
        plate(m, c, rr, uu, nn, 0.86, 0.40, text="DP-07", lift=0.07, thick=0.12)
    m.lathe([(3.10, 0.26), (3.30, 0.26), (3.34, 0.22)], 10, 0.0, ["armor2", "band"], caps=(False, True), cap_mats=(None, "dark"))
    beacon(m, 3.34)
    o = surf(math.radians(56), 0.0, 0.0); nn = nrm(math.radians(56), 0.0)
    for s_ in (-1, 1):
        m.box((-0.02, -0.02, 0.0), (0.02, 0.02, 0.12), "steel", rot=np.stack([np.array([1.0, 0, 0]), np.cross(nn, [1.0, 0, 0]), nn], 1),
              at=o + np.array([s_ * 0.22, 0, 0]))
    m.box((-0.24, -0.02, 0.10), (0.24, 0.02, 0.14), "steel", rot=np.stack([np.array([1.0, 0, 0]), np.cross(nn, [1.0, 0, 0]), nn], 1), at=o)
    return m


# ------------------------------------------------------------------------------------------------ F: spike
def spike_pod():
    """F: the spike. A slim round pod (2 across, 4.8 high) on a hardened penetrator that drives a block into the
    ground and holds it there; the four motors ride high on outrigger arms so their blast clears the hull."""
    m = Model(); m.thrusters = []
    n = 16
    ph = math.radians(-33.75)
    # the penetrator: a hardened steel cone with four flutes, its collar
    m.lathe([(0.0, 0.0), (0.10, 0.05), (0.85, 0.26), (0.95, 0.30)], n, ph, ["steel", "steel", "steel"], tile=0.5)
    m.lathe([(0.95, 0.38), (1.07, 0.38)], n, ph, ["band"], caps=(True, False), cap_mats=("band", None))
    for k in range(4):
        a = math.radians(45 + 90 * k)
        out = np.array([math.cos(a), 0, math.sin(a)])
        m.prism([(0.06, 0.18), (0.20, 0.80), (0.24, 0.96), (0.10, 0.96)], (np.zeros(3), out, UP), 0.04, "steel", edge="steel")
    prof = [(1.05, 0.38), (1.22, 0.92), (1.34, 1.08), (1.44, 1.08), (1.56, 1.04), (2.00, 1.00), (2.08, 1.0), (3.98, 1.0), (4.05, 1.0),
            (4.13, 1.02), (4.20, 1.02), (4.58, 0.60), (4.70, 0.52), (4.78, 0.40)]
    mats = ["shield", "shield", "band", "band", "skirt", "band", "char", "armor", "band", "band", "armor", "band", "armor"]
    door_k = {0, 1, 2}
    m.lathe(prof, n, ph, mats, face_mat=lambda k, b: "door" if (b == 6 and k in door_k) else (("armor" if k % 2 else "armor3") if b == 6 else None),
            caps=(False, True), cap_mats=(None, "dark"))
    def surf(y, a, lift=0.0):
        r = 1.0 + lift
        return np.array([-math.sin(a) * r, y, -math.cos(a) * r])
    def nrm(a):
        return np.array([-math.sin(a), 0, -math.cos(a)])
    def strip(p, nn):
        L = np.linalg.norm(p[1] - p[0])
        m.quad(p, [(0, 0), (L * 4, 0), (L * 4, 0.4), (0, 0.4)], "hazard", nn)
    ae = math.radians(33.75); w = math.radians(3.2)
    for s_ in (-1, 1):
        a = s_ * ae
        strip([surf(4.03, a - w, 0.012), surf(4.03, a + w, 0.012), surf(2.03, a + w, 0.012), surf(2.03, a - w, 0.012)], nrm(a))
        for y in np.linspace(2.25, 3.80, 4):
            bolt(m, surf(y, s_ * (ae + math.radians(6.5))), nrm(s_ * (ae + math.radians(6.5))), r=0.036, h=0.06)
    aa = np.linspace(-ae, ae, 7)
    for y in (2.08, 3.98):
        for a0, a1 in zip(aa, aa[1:]):
            strip([surf(y + 0.05, a0, 0.012), surf(y + 0.05, a1, 0.012), surf(y - 0.05, a1, 0.012), surf(y - 0.05, a0, 0.012)], nrm((a0 + a1) / 2))
    for (txt, w_, h_, size, rgb, y, ww, hh) in (("07", 128, 64, 40, (222, 222, 214), 3.55, 0.26, 0.14),
                                                ("EXPLOSIVE BOLTS", 256, 48, 22, (236, 186, 40), 2.55, 0.40, 0.085)):
        wrap_decal(m, decal(txt, w_, h_, size, rgb), lambda u, v, y=y, ww=ww, hh=hh: surf(y + hh - 2 * hh * v, -(ww - 2 * ww * u), 0.010),
                   lambda u, v, ww=ww: nrm(-(ww - 2 * ww * u)))
    m.door_frame = (surf(3.03, 0.0), np.array([-1.0, 0, 0]), UP.copy(), np.array([0, 0, -1.0]))
    for a in (math.radians(90), math.radians(-90)):
        c = surf(3.30, a, 0.0); nn = nrm(a); rr = np.cross(UP, nn)
        plate(m, c, rr, UP, nn, 0.80, 0.42, text="DP-07", lift=0.06, thick=0.10)
    # the outriggers: arms off the shoulder band to four motor pods, braced back to the hull
    for k in range(4):
        a = math.radians(45 + 90 * k)
        out = np.array([-math.sin(a), 0, -math.cos(a)])
        tang = np.cross(UP, out)
        Rm = np.stack([tang, UP, out], 1)
        m.box((-0.08, -0.09, 0.90), (0.08, 0.09, 1.50), "armor2", rot=Rm, at=UP * 4.14)
        pc = out * 1.60
        m.lathe([(3.40, 0.17), (4.10, 0.17), (4.30, 0.10), (4.36, 0.05)], 10, 0.0, ["armor2", "armor2", "band"], origin=pc,
                caps=(True, True), cap_mats=("skirt", "band"))
        m.lathe([(3.80, 0.18), (3.86, 0.18)], 10, 0.0, ["band"], origin=pc)
        br0, br1 = out * 0.99 + UP * 3.25, out * 1.50 + UP * 3.58
        dv = br1 - br0; L = np.linalg.norm(dv); dv /= L
        m.box((-0.035, 0.0, -0.035), (0.035, L, 0.035), "steel", rot=np.stack([tang, dv, np.cross(tang, dv)], 1), at=br0)
        d = -UP * math.cos(math.radians(10)) + out * math.sin(math.radians(10))
        m.thrusters.append((nozzle(m, pc + UP * 3.40, d, r_throat=0.10, r_exit=0.21, length=0.42), d))
    m.lathe([(4.78, 0.22), (4.92, 0.22), (4.95, 0.18)], 10, 0.0, ["armor2", "band"], caps=(False, True), cap_mats=(None, "dark"))
    beacon(m, 4.95, 0.08)
    return m


# ------------------------------------------------------------------------------------------------ G: cage
def cage_pod():
    """G: the cage. A round capsule (2 across) slung on dampers inside a steel crash frame 2.5 square (3 x 3 cells):
    the frame's crumple feet take the hit, four motors on its bottom rails, the hatch three panels wide."""
    m = Model(); m.thrusters = []
    n = 12
    ph = math.radians(-45)
    door_k = {0, 1, 2}
    prof = [(0.30, 0.0), (0.42, 0.62), (0.55, 0.98), (0.62, 1.06), (0.72, 1.06), (0.90, 1.0), (1.0, 1.0), (2.95, 1.0), (3.02, 1.0),
            (3.40, 0.80), (3.62, 0.42), (3.70, 0.28)]
    mats = ["shield", "shield", "band", "band", "skirt", "band", "char", "band", "armor", "armor2", "band"]
    m.lathe(prof, n, ph, mats, face_mat=lambda k, b: "door" if (b == 6 and k in door_k) else (("armor" if k % 2 else "armor3") if b == 6 else None),
            caps=(False, True), cap_mats=(None, "dark"))
    def surf(y, a, lift=0.0):
        return np.array([-math.sin(a) * (1.0 + lift), y, -math.cos(a) * (1.0 + lift)])
    def nrm(a):
        return np.array([-math.sin(a), 0, -math.cos(a)])
    def strip(p, nn):
        L = np.linalg.norm(p[1] - p[0])
        m.quad(p, [(0, 0), (L * 4, 0), (L * 4, 0.4), (0, 0.4)], "hazard", nn)
    ae, w = math.radians(45), math.radians(3.0)
    for s_ in (-1, 1):
        a = s_ * ae
        strip([surf(2.93, a - w, 0.012), surf(2.93, a + w, 0.012), surf(1.02, a + w, 0.012), surf(1.02, a - w, 0.012)], nrm(a))
        for y in np.linspace(1.22, 2.75, 4):
            bolt(m, surf(y, s_ * (ae + math.radians(6))), nrm(s_ * (ae + math.radians(6))), r=0.036, h=0.06)
    aa = np.linspace(-ae, ae, 7)
    for y in (1.0, 2.95):
        for a0, a1 in zip(aa, aa[1:]):
            strip([surf(y + 0.05, a0, 0.012), surf(y + 0.05, a1, 0.012), surf(y - 0.05, a1, 0.012), surf(y - 0.05, a0, 0.012)], nrm((a0 + a1) / 2))
    for (txt, w_, h_, size, rgb, y, ww, hh) in (("07", 128, 64, 40, (222, 222, 214), 2.50, 0.26, 0.14),
                                                ("EXPLOSIVE BOLTS", 256, 48, 22, (236, 186, 40), 1.50, 0.40, 0.085)):
        wrap_decal(m, decal(txt, w_, h_, size, rgb), lambda u, v, y=y, ww=ww, hh=hh: surf(y + hh - 2 * hh * v, -(ww - 2 * ww * u), 0.010),
                   lambda u, v, ww=ww: nrm(-(ww - 2 * ww * u)))
    m.door_frame = (surf(1.98, 0.0), np.array([-1.0, 0, 0]), UP.copy(), np.array([0, 0, -1.0]))
    for a in (math.radians(90), math.radians(-90)):
        c = surf(2.40, a, 0.0); nn = nrm(a)
        plate(m, c, np.cross(UP, nn), UP, nn, 0.72, 0.40, text="DP-07", lift=0.06, thick=0.10)
    # the frame: four posts, rails at the foot, the middle and the head, cross braces on three sides, the top cross
    H = 1.20
    cs = [(-H, -H), (H, -H), (H, H), (-H, H)]
    t = 0.08
    for (x, z) in cs:
        m.box((x - t, 0.30, z - t), (x + t, 3.86, z + t), "band")
        m.box((x - 0.21, 0.0, z - 0.21), (x + 0.21, 0.32, z + 0.21), "skirt")           # the crumple foot
        m.box((x - 0.17, 0.32, z - 0.17), (x + 0.17, 0.40, z + 0.17), "dark")
    def beam(p0, p1, th=0.06, mm="band"):
        p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
        dv = p1 - p0; L = np.linalg.norm(dv); dv /= L
        side = np.cross(dv, UP) if abs(dv[1]) < 0.95 else np.array([1.0, 0, 0])
        side /= np.linalg.norm(side)
        m.box((-th, 0.0, -th), (th, L, th), mm, rot=np.stack([side, dv, np.cross(side, dv)], 1), at=p0)
    for i in range(4):
        (x0, z0), (x1, z1) = cs[i], cs[(i + 1) % 4]
        front = (z0 == -H and z1 == -H)
        for y in ((0.48, 3.78) if front else (0.48, 2.10, 3.78)):
            beam((x0, y, z0), (x1, y, z1), 0.06)
        if not front:
            for (ya, yb) in ((0.48, 2.10), (2.10, 3.78)):
                beam((x0, ya, z0), (x1, yb, z1), 0.04)
                beam((x1, ya, z1), (x0, yb, z0), 0.04)
    beam((-H, 3.80, -H), (H, 3.80, H), 0.05); beam((H, 3.80, -H), (-H, 3.80, H), 0.05)
    m.box((-0.16, 3.74, -0.16), (0.16, 3.88, 0.16), "armor2")
    beacon(m, 3.88)
    # the dampers: from the capsule to each post, low and high (a steel rod, the damper's body on it)
    for (x, z) in cs:
        out = np.array([x, 0, z]) / np.hypot(x, z)
        for y in (1.15, 2.85):
            p0 = out * 0.98 + UP * y
            p1 = np.array([x, y, z]) - out * 0.08
            beam(p0, p1, 0.025, "steel")
            mid0, mid1 = p0 + (p1 - p0) * 0.3, p0 + (p1 - p0) * 0.65
            beam(mid0, mid1, 0.05, "dark")
    # the motors: one under each side's foot rail, the front's under the hatch
    for k in range(4):
        a = math.radians(90 * k)
        out = np.array([math.sin(a), 0, -math.cos(a)])
        tang = np.cross(UP, out)
        m.box((-0.17, -0.18, -0.12), (0.17, 0.12, 0.12), "skirt", rot=np.stack([tang, UP, out], 1), at=out * 1.20 + UP * 0.62)
        d = -UP * math.cos(math.radians(24)) + out * math.sin(math.radians(24))
        m.thrusters.append((nozzle(m, out * 1.22 + UP * 0.50, d, r_throat=0.09, r_exit=0.19, length=0.36), d))
    return m


# ------------------------------------------------------------------------------------------------ H: tri
def tri_pod():
    """H: the tri. A three-sided hull (the soldier's flat at the front, 2.9 across it) with a long motor nacelle down
    each rounded corner; a fin on each nacelle keeps it flying shield first."""
    m = Model(); m.thrusters = []
    RI, RC = 0.95, 0.42
    poly = lambda s: rtri(RI * s, RC * s, seg=4)
    rings, mats = base_rings(poly, [(3.75, 0.46), (3.86, 0.36)])
    mats += ["armor", "band"]
    loft(m, rings, mats, caps=(True, True), cap_mats=("shield", "armor3"))
    body = poly(0.98)
    N = len(body)
    c, r_, u_, n_ = flat_frame(body[N - 1], body[0], 2.025)
    hatch(m, c, r_, u_, n_, 1.04, 1.95)
    m.door_frame = (c, r_, u_, n_)
    for (ka, kb) in ((4, 5), (9, 10)):
        for (yb, yt, txt) in ((1.10, 2.02, None), (2.10, 3.00, "DP-07")):
            cc, rr, uu, nn = flat_frame(body[ka], body[kb], (yb + yt) / 2)
            plate(m, cc, rr, uu, nn, 1.30, yt - yb, text=txt, tone="armor3" if yb < 2 else "armor")
    for ca in (330, 90, 210):
        out = np.array([math.cos(math.radians(ca)), 0, math.sin(math.radians(ca))])
        pc = out * 1.36
        m.lathe([(0.70, 0.22), (0.80, 0.30), (3.00, 0.30), (3.28, 0.18), (3.40, 0.08)], 12, 0.0, ["skirt", "armor2", "armor2", "band"],
                origin=pc, caps=(True, True), cap_mats=("skirt", "band"))
        for yy in (1.10, 2.10):
            m.lathe([(yy, 0.31), (yy + 0.06, 0.31)], 12, 0.0, ["band"], origin=pc)
        tang = np.cross(UP, out)
        m.box((-0.12, 1.0, -0.36), (0.12, 2.9, -0.10), "armor2", rot=np.stack([tang, UP, out], 1), at=pc)
        m.prism([(0.20, 2.30), (0.20, 3.20), (0.62, 3.36), (0.66, 2.78)], (pc, out, UP), 0.06, "armor2", edge="band")
        d = -UP * math.cos(math.radians(14)) + out * math.sin(math.radians(14))
        m.thrusters.append((nozzle(m, pc + UP * 0.70, d, r_throat=0.11, r_exit=0.25, length=0.46), d))
    beacon(m, 3.86)
    m.box((0.18, 3.80, 0.16), (0.21, 4.40, 0.19), "steel")
    return m


# ------------------------------------------------------------------------------------------------ I: heavy
def heavy_pod():
    """I: the heavy. A wide two-bay pod (3.4 x 2.1, 4.1 high; 4 x 3 cells): the soldier's hatch, and beside it a
    locker for the power armour or the heavy weapon with its own bolted hatch; six motors."""
    m = Model(); m.thrusters = []
    W, D, RC = 3.4, 2.1, 0.55
    poly = lambda s: rrect(W * s, D * s, RC * s, seg=4)
    rings, mats = base_rings(poly, [(3.70, 0.78), (3.80, 0.72)])
    mats += ["armor", "band"]
    loft(m, rings, mats, caps=(True, True), cap_mats=("shield", "armor3"))
    body = poly(0.98)
    N = len(body)
    c, r_, u_, n_ = flat_frame(body[N - 1], body[0], 2.025)
    hatch(m, c + r_ * -0.52, r_, u_, n_, 0.92, 1.95, fw=0.11, rails=False)
    m.door_frame = (c + r_ * -0.52, r_, u_, n_)
    cl = c + r_ * 0.58 + u_ * (1.80 - 2.025)
    hatch(m, cl, r_, u_, n_, 0.78, 1.50, number="07B", fw=0.11, warn="ARMOUR LOCKER", nbolts=3, rails=False)
    for s_ in (-1, 1):
        mid = c + r_ * s_ * 1.20 + n_ * 0.10 + u_ * 0.05
        m.box((-0.02, -0.5, -0.02), (0.02, 0.5, 0.02), "steel", rot=np.stack([r_, u_, n_], 1), at=mid)
        for e in (-0.48, 0.48):
            m.box((-0.02, -0.02, -0.10), (0.02, 0.02, 0.0), "steel", rot=np.stack([r_, u_, n_], 1), at=mid + u_ * e)
    # the sides and the back: plates, the markings
    for (ka, kb, txt) in ((4, 5, "DP-07"), (14, 15, "DP-07")):
        cc, rr, uu, nn = flat_frame(body[ka], body[kb], 2.55)
        plate(m, cc, rr, uu, nn, 0.86, 0.80, text=txt, sub="HEAVY")
        cc, rr, uu, nn = flat_frame(body[ka], body[kb], 1.56)
        plate(m, cc, rr, uu, nn, 0.86, 0.96, tone="armor")
    for yy in ((1.10, 2.02), (2.10, 3.00)):
        cc, rr, uu, nn = flat_frame(body[9], body[10], sum(yy) / 2)
        for a in (-0.62, 0.62):
            plate(m, cc + rr * a, rr, uu, nn, 1.05, yy[1] - yy[0], tone="armor3" if a < 0 else "armor")
    # six motors: the four corners and the middle of each end
    cx, cz = W / 2 - RC, D / 2 - RC
    for (sx, sz) in ((1, -1), (1, 1), (-1, 1), (-1, -1)):
        out = np.array([sx * 0.707, 0, sz * 0.707])
        base = np.array([sx * cx, 0, sz * cz]) + out * RC
        tang = np.cross(UP, out)
        m.box((-0.19, -0.20, -0.10), (0.19, 0.18, 0.15), "skirt", rot=np.stack([tang, UP, out], 1), at=base + UP * 0.78)
        d = -UP * math.cos(math.radians(22)) + out * math.sin(math.radians(22))
        m.thrusters.append((nozzle(m, base + out * 0.04 + UP * 0.62, d, r_throat=0.095, r_exit=0.21, length=0.42), d))
    for sx in (1, -1):
        out = np.array([sx, 0, 0.0])
        motor(m, out, W / 2 + 0.04, 0.62, cant=22, size=1.1, housing=(0.40, 0.38, 0.24))
    # the roof: two vanes at the back, a hoist bar, the beacon
    for sx in (1, -1):
        base = np.array([sx * 1.05, 3.72, 0.55])
        m.prism([(0.0, 0.0), (0.0, 0.42), (0.38, 0.52), (0.50, 0.10)], (base, np.array([sx * 0.5, 0, 0.866]), UP), 0.06, "armor2", edge="band")
    for s_ in (-1, 1):
        m.box((s_ * 0.40 - 0.04, 3.80, 0.41), (s_ * 0.40 + 0.04, 3.98, 0.49), "steel")
    m.box((-0.44, 3.94, 0.41), (0.44, 4.00, 0.49), "steel")
    beacon(m, 3.80)
    return m


FORMS = {"D": brick_pod, "E": ball_pod, "F": spike_pod, "G": cage_pod, "H": tri_pod, "I": heavy_pod}
