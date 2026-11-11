"""The chosen drop pod (Justin, 7 November 2026): C, the dart, worked up (mockup sheet 3). Octagonal, 2.4 across the
flats (widened from 2.2 so the hatch clears the soldier's shoulders), 4.8 high, four fins on the shoulder, eight retro
motors at the corners. Inside, barely room for the soldier: a padded cocoon hung on dampers over a crush core, the
soldier locked in by a shoulder yoke, a knee bar, side bolsters and a head cradle; the heavy weapon and ammunition ride
in slots beside the cocoon, the propellant tanks, battery and avionics in the bay behind it.

Same frame as pod.py: blocks, y up from the shield's lowest point, the hatch facing -z. The soldier stands at SOLDIER
(feet), facing -z."""
import math
import os
import sys
S = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, S)
import numpy as np
from PIL import Image, ImageDraw
import pod as P
import pods2 as P2
from pod import Model, UV1, bolt, nozzle, decal

UP = np.array([0, 1.0, 0])
AP = 1.20                                   # across the flats / 2
R0 = AP / math.cos(math.pi / 8)             # corner radius
PH = math.radians(-22.5)                    # a flat (k = 0, the hatch's) facing -z
AP_IN = 1.02                                # the inside of the wall
YF = 1.0                                    # the floor
DOOR = dict(w=0.88, y0=1.05, y1=3.05, fw=0.055, lift=0.07)
def honeycomb():
    """The crush core's face: aluminium honeycomb, hex cells."""
    if "honeycomb" in P._T:
        return P._T["honeycomb"]
    n = 64
    im = Image.new("RGB", (n * 4, n * 4), (150, 152, 150))
    d = ImageDraw.Draw(im)
    r = 18
    for row in range(-1, 9):
        for col in range(-1, 9):
            cx = col * r * 1.5 * 2
            cy = row * r * math.sqrt(3) + (r * math.sqrt(3) / 2 if col % 2 else 0)
            for off in (0, r * 1.5):
                x0 = cx + off
                y0 = cy + (r * math.sqrt(3) / 2 if off else 0)
                pts = [(x0 + r * math.cos(math.radians(60 * k)), y0 + r * math.sin(math.radians(60 * k))) for k in range(6)]
                d.polygon(pts, fill=(96, 98, 98), outline=(176, 178, 176))
    t = np.ones((n, n, 4), np.float32)
    t[..., :3] = np.asarray(im.resize((n, n), Image.LANCZOS), np.float32) / 255
    P._T["honeycomb"] = t
    return t


def face(k, y, ap=AP):
    """Flat k (centred 45k degrees round from -z) of the body at height y: (centre, right as seen, up, normal)."""
    a = math.radians(45 * k)
    n = np.array([-math.sin(a), 0, -math.cos(a)])
    return n * ap + UP * y, np.cross(UP, n), UP.copy(), n


def corner_dir(k):
    a = math.radians(22.5 + 45 * k)
    return np.array([-math.sin(a), 0, -math.cos(a)])


# ------------------------------------------------------------------------------------------------ the hull
def dart(door_apart=False, interior=False, restraints="closed", scorched=1.0):
    """The dart. door_apart: the hatch built as its own model (m.door, about m.door_centre) and the doorway open;
    interior: the inside (m.lining, cut with the hull in a cutaway, and m.inner, never cut)."""
    m = Model(); m.thrusters = []
    m.lining, m.inner = Model(), Model()
    m.lining.thrusters = m.inner.thrusters = []
    R = R0
    prof = [(0.0, 0.0), (0.30, 0.95 * R), (0.38, 1.02 * R), (0.46, 1.02 * R), (0.92, R), (1.00, R), (3.10, R), (3.30, R), (3.38, 0.98 * R),
            (4.30, 0.42 * R), (4.62, 0.30 * R), (4.80, 0.18 * R)]
    mats = ["shield", "shield", "band", "skirt", "band", "char" if scorched > 0.5 else "armor", "armor", "band", "armor", "armor2", "dark"]
    opened = door_apart or interior
    m.lathe(prof, 8, PH, mats, caps=(False, True), cap_mats=(None, "dark"),
            face_mat=(lambda k, b: "skip" if (k == 0 and b == 5) else None) if opened else None)
    # the launch ring: where the drop gun's sabot grips it
    m.lathe([(3.30, R * 0.99), (3.30, R * 1.045), (3.42, R * 1.045), (3.42, R * 0.97)], 8, PH, ["band", "band", "band"])
    # raised plates, two a flat (the hatch's flat has its frame; the back's upper plate is the service panel)
    for k in range(1, 8):
        for (yb, yt) in ((1.10, 2.06), (2.14, 3.04)):
            c, r_, u_, n_ = face(k, (yb + yt) / 2)
            if k == 4 and yb > 2:
                P2.plate(m, c, r_, u_, n_, 0.80, yt - yb, text="SERVICE", sub="NO STEP", tone="armor")
                for sx in (-0.34, 0.34):
                    for sy in (-0.38, 0.38):
                        bolt(m, c + r_ * sx + u_ * sy + n_ * 0.06, n_, r=0.022, h=0.02)
                continue
            tone = "char" if (scorched > 0.5 and yb < 1.5) else ("armor" if (k + int(yb > 2)) % 2 else "armor3")
            txt = "DP-07" if (k in (2, 6) and yb > 2) else None
            P2.plate(m, c, r_, u_, n_, 0.80, yt - yb, text=txt, tone=tone)
    # corner ribs (not the hatch's two corners: its frame runs to them)
    for k in range(1, 7):
        out = corner_dir(k)
        tang = np.cross(UP, out)
        m.box((-0.07, 0.95, -0.05), (0.07, 3.28, 0.035), "armor2", rot=np.stack([tang, UP, -out], 1), at=out * R)
    # the hatch's frame, the bolts down it, the hatch (its own model when it is to be blown off)
    yc = (DOOR["y0"] + DOOR["y1"]) / 2
    c, right, up, nrm = face(0, yc)
    dw, dh, fw, lift = DOOR["w"], DOOR["y1"] - DOOR["y0"], DOOR["fw"], DOOR["lift"]
    for (a0, a1, b0, b1) in ((-dw / 2 - fw, -dw / 2, -dh / 2 - fw, dh / 2 + fw), (dw / 2, dw / 2 + fw, -dh / 2 - fw, dh / 2 + fw),
                             (-dw / 2, dw / 2, dh / 2, dh / 2 + fw), (-dw / 2, dw / 2, -dh / 2 - fw, -dh / 2)):
        m.prism([(a0, b0), (a1, b0), (a1, b1), (a0, b1)], (c + nrm * lift / 2, right, up), lift, "hazard", edge="armor2", tile=0.5)
    m.door_frame = (c, right, up, nrm)
    dm = Model()
    P0 = c + nrm * (lift - 0.02)
    dm.prism([(-dw / 2, -dh / 2), (dw / 2, -dh / 2), (dw / 2, dh / 2), (-dw / 2, dh / 2)], (P0 - nrm * 0.05, right, up), 0.10, "door", edge="armor2")
    dm.box((-0.15, -0.04, 0.0), (0.15, 0.04, 0.05), "steel", rot=np.stack([right, up, nrm], 1), at=P0 - up * 0.05)
    for (txt, w, h, size, rgb, b, ww, hh) in (("07", 128, 64, 40, (222, 222, 214), 0.56, 0.28, 0.15),
                                                ("EXPLOSIVE BOLTS", 256, 48, 22, (236, 186, 40), -0.46, 0.38, 0.075)):
        d0 = P0 + up * b + nrm * 0.008
        dm.quad([d0 - right * ww + up * hh, d0 + right * ww + up * hh, d0 + right * ww - up * hh, d0 - right * ww - up * hh], UV1,
                decal(txt, w, h, size, rgb), nrm)
    m.door, m.door_centre = dm, P0
    if not door_apart:
        m.faces += dm.faces
    for s_ in (-1, 1):
        for b in np.linspace(-dh / 2 + 0.16, dh / 2 - 0.16, 5):
            bolt(m, c + right * s_ * (dw / 2 + fw / 2) + up * b + nrm * lift, nrm, r=0.026, h=0.04)
    if opened:                                    # the doorway's reveal back through the wall
        inside = c - nrm * (AP - AP_IN)
        for (a0, nn) in ((-dw / 2, right), (dw / 2, -right)):
            m.quad([c + right * a0 + up * dh / 2 + nrm * lift, inside + right * a0 + up * dh / 2, inside + right * a0 - up * dh / 2,
                    c + right * a0 - up * dh / 2 + nrm * lift], [(0, 0), (0.3, 0), (0.3, 2), (0, 2)], "armor2", nn)
        for (b, nn) in ((dh / 2, -up), (-dh / 2, up)):
            m.quad([c - right * dw / 2 + up * b + nrm * lift, c + right * dw / 2 + up * b + nrm * lift, inside + right * dw / 2 + up * b,
                    inside - right * dw / 2 + up * b], [(0, 0), (1, 0), (1, 0.3), (0, 0.3)], "armor2", nn)
    # grab rails on the flats either side of the hatch
    for k in (1, 7):
        c2, r2, u2, n2 = face(k, 2.10)
        mid = c2 + n2 * 0.11
        m.box((-0.02, -0.45, -0.02), (0.02, 0.45, 0.02), "steel", rot=np.stack([r2, u2, n2], 1), at=mid)
        for e in (-0.42, 0.42):
            m.box((-0.02, -0.02, -0.11), (0.02, 0.02, 0.0), "steel", rot=np.stack([r2, u2, n2], 1), at=mid + u2 * e)
    # eight retro motors, one at each corner of the skirt, in their housings, canted out
    for k in range(8):
        out = corner_dir(k)
        tang = np.cross(UP, out)
        m.box((-0.15, -0.18, -0.10), (0.15, 0.16, 0.12), "skirt", rot=np.stack([tang, UP, out], 1), at=out * (R * 1.0) + UP * 0.76)
        d = -UP * math.cos(math.radians(22)) + out * math.sin(math.radians(22))
        m.thrusters.append((nozzle(m, out * (R * 1.02) + UP * 0.62, d, r_throat=0.07, r_exit=0.155, length=0.32), d))
    # the four fins on the shoulder, the beacon on the nose, an antenna
    for k in (1, 3, 5, 7):
        out = face(k, 0)[3]
        m.prism([(0.97, 3.56), (0.43, 4.42), (0.95, 4.72), (1.62, 3.92), (1.42, 3.56)], (np.zeros(3), out, UP), 0.07, "armor2", edge="band")
    m.lathe([(4.80, 0.07), (4.92, 0.07), (4.95, 0.045)], 8, 0.0, ["dark", "beacon"], caps=(False, True), cap_mats=(None, "beacon"))
    m.lamps.append((np.array([0, 4.93, 0]), (1.0, 0.25, 0.15)))
    m.box((0.30, 4.00, 0.30), (0.33, 4.70, 0.33), "steel")
    if interior:
        build_interior(m, restraints)
    return m


# ------------------------------------------------------------------------------------------------ the inside
FY = YF + 0.05                                 # the soldier's feet: on the foot plate
SOLDIER = np.array([0.0, FY, -0.50])
ZB = SOLDIER[2] + 0.30 + 0.009                 # the back pad's face, just off the soldier's back


def _bar(mod, a, b, half, m_):
    """A square bar from a to b (the yoke's, the knee bar's arms), its sides square to x."""
    a, b = np.asarray(a, float), np.asarray(b, float)
    d = b - a; L = np.linalg.norm(d); d /= L
    side = np.array([1.0, 0, 0])
    mod.box((-half[0], -half[1], 0.0), (half[0], half[1], L), m_, rot=np.stack([side, np.cross(d, side), d], 1), at=a)


def cocoon():
    """The cocoon's fixed part: the padded back shell on its frame, the side bolsters, the head cradle's wings and the
    pad over the helmet, the foot plate with its heel cups (in the pod's frame, the soldier's feet at FY)."""
    c = Model()
    y = FY
    c.box((-0.56, y - 0.01, ZB), (0.56, y + 2.02, ZB + 0.10), "pad", mats={"south": "frame", "up": "frame", "east": "frame", "west": "frame"})
    c.box((-0.60, YF, ZB + 0.10), (0.60, y + 2.12, ZB + 0.16), "frame")
    for s_ in (-1, 1):
        x_in, x_out = s_ * 0.585, s_ * 0.70
        c.box((min(x_in, x_out), y, -0.80), (max(x_in, x_out), y + 1.35, ZB + 0.02), "frame", mats={"east" if s_ < 0 else "west": "pad"})
        c.box((min(s_ * 0.335, s_ * 0.44), y + 1.50, -0.66), (max(s_ * 0.335, s_ * 0.44), y + 1.97, ZB + 0.01), "frame",
              mats={"east" if s_ < 0 else "west": "pad"})
    c.box((-0.44, y + 2.04, -0.58), (0.44, y + 2.12, ZB + 0.02), "pad", mats={"up": "frame"})
    c.box((-0.48, YF, -0.82), (0.48, FY, ZB), "frame")
    for s_ in (-1, 1):
        c.box((min(s_ * 0.05, s_ * 0.26), y, SOLDIER[2] + 0.16), (max(s_ * 0.05, s_ * 0.26), y + 0.09, SOLDIER[2] + 0.22), "pad")
    for s_ in (-1, 1):                              # the yoke's pivot blocks on the back shell
        c.box((min(s_ * 0.33, s_ * 0.45), y + 1.37, -0.205), (max(s_ * 0.33, s_ * 0.45), y + 1.47, ZB + 0.01), "frame")
    return c


YOKE_PIVOT = np.array([0, FY + 1.42, -0.15])
KNEE_PIVOT = np.array([0, FY + 0.25, -0.36])


def yoke(state="closed"):
    """The shoulder yoke: two padded bars from the pivots behind the shoulders, over them and down the chest to the
    chest pad. Open: swung up and back, clear of the head."""
    m = Model()
    y = FY
    for s_ in (-1, 1):
        x = s_ * 0.39
        path = [(x, y + 1.42, -0.15), (x, y + 1.42, -0.62), (x, y + 1.31, -0.76), (x, y + 0.97, -0.78)]
        for a, b in zip(path, path[1:]):
            _bar(m, a, b, (0.05, 0.05), "pad")
    m.box((-0.44, y + 0.93, -0.84), (0.44, y + 1.17, -0.72), "pad", mats={"north": "frame"})
    if state == "open":
        _turn(m, YOKE_PIVOT, math.radians(70))
    return m


def knee_bar(state="closed"):
    """The knee bar on its two arms from the bolsters. Open: swung down to the floor's front."""
    m = Model()
    y = FY
    m.box((-0.56, y + 0.33, -0.80), (0.56, y + 0.47, -0.70), "pad", mats={"north": "frame"})
    for s_ in (-1, 1):
        _bar(m, (s_ * 0.57, KNEE_PIVOT[1], KNEE_PIVOT[2]), (s_ * 0.57, y + 0.40, -0.75), (0.015, 0.03), "steel")
    if state == "open":
        _turn(m, KNEE_PIVOT, math.radians(-55))
    return m


def _turn(mod, pivot, ang):
    c, s = math.cos(ang), math.sin(ang)
    Rx = np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    mod.faces = [(((Pq - pivot) @ Rx.T + pivot), UV, mm, gl) for Pq, UV, mm, gl in mod.faces]


def crush_core():
    """The crush core under the floor: an aluminium honeycomb block the cocoon stands on, crushed by the landing."""
    m = Model()
    m.box((-0.62, 0.50, -0.86), (0.62, YF - 0.01, 0.05), honeycomb(), mats={"up": None})
    return m


def tank(cc, label, pipe=True):
    m = Model()
    cc = np.asarray(cc, float)
    m.lathe([(1.16, 0.0), (1.20, 0.14), (1.30, 0.23), (1.42, 0.26), (2.52, 0.26), (2.64, 0.23), (2.74, 0.14), (2.78, 0.0)], 12, 0.0,
            ["band", "band", "band", "steel", "band", "band", "band"], origin=cc, tile=0.5)
    d0 = np.array([cc[0], 2.0, cc[2] - 0.268])
    m.quad([d0 + np.array([0.12, 0.07, 0]), d0 + np.array([-0.12, 0.07, 0]), d0 + np.array([-0.12, -0.07, 0]), d0 + np.array([0.12, -0.07, 0])],
           UV1, decal(label, 128, 64, 34, (30, 30, 30), bg="steel"), (0, 0, -1))
    if pipe:
        m.box((cc[0] - 0.025, YF - 0.30, cc[2] - 0.02), (cc[0] + 0.025, 1.18, cc[2] + 0.03), "steel")
    return m


def damper(a, b):
    m = Model()
    a, b = np.asarray(a, float), np.asarray(b, float)
    _bar(m, a, b, (0.03, 0.03), "steel")
    _bar(m, a + (b - a) * 0.3, a + (b - a) * 0.66, (0.055, 0.055), "dark")
    return m


def weapon_slot():
    """The heavy weapon (a launcher) clamped in its slot outside the left bolster, the ammunition case by the right."""
    m = Model()
    tube_c = np.array([-0.84, 1.90, -0.42])
    m.lathe([(-0.68, 0.075), (0.68, 0.075)], 10, 0.0, ["olive"], origin=tube_c, axis=UP, ref=(1.0, 0, 0), tile=0.5, caps=(True, True),
            cap_mats=("dark", "dark"))
    for b in (-0.62, 0.62):
        m.lathe([(b - 0.05, 0.09), (b + 0.05, 0.09)], 10, 0.0, ["dark"], origin=tube_c, axis=UP, ref=(1.0, 0, 0), tile=0.5)
    for b in (-0.36, 0.36):
        m.box((-0.84, 1.90 + b - 0.05, -0.47), (-0.70, 1.90 + b + 0.05, -0.37), "steel")
    m.box((0.72, YF + 0.02, -0.54), (0.89, YF + 0.62, -0.24), "crate")
    m.box((0.70, YF + 0.30, -0.42), (0.72, YF + 0.36, -0.36), "steel")
    return m


def build_interior(m, restraints="closed"):
    L, I = m.lining, m.inner
    RI = AP_IN / math.cos(math.pi / 8)
    L.lathe([(YF, RI), (3.28, RI)], 8, PH, ["dark"], inside=True)
    e1, e2 = np.array([0, 0, -1.0]), np.array([-1.0, 0, 0])
    corners = [(e1 * math.cos(PH + k * math.pi / 4) + e2 * math.sin(PH + k * math.pi / 4)) * RI for k in range(9)]
    for k in range(8):                             # the floor (grating) and the ceiling over the inside
        a, b = corners[k], corners[k + 1]
        I.quad([np.array([0, YF, 0]), a + UP * YF, b + UP * YF, b + UP * YF], [(0.5, 0.5), (0.5 + a[0] / 2, 0.5 + a[2] / 2),
               (0.5 + b[0] / 2, 0.5 + b[2] / 2), (0.5 + b[0] / 2, 0.5 + b[2] / 2)], "grate", UP)
        L.quad([np.array([0, 3.28, 0]), a + UP * 3.28, b + UP * 3.28, b + UP * 3.28], UV1, "dark", -UP)
    for part in (cocoon(), yoke(restraints), knee_bar(restraints), crush_core(), weapon_slot(), tank((-0.40, 0, 0.45), "PROP"),
                 tank((0.40, 0, 0.45), "OX")):
        I.faces += part.faces
    I.box((-0.12, YF, 0.06), (0.12, 1.90, 0.62), "dark")                     # the battery
    for yy in np.arange(YF + 0.12, 1.86, 0.16):
        I.box((-0.125, yy, 0.06), (0.125, yy + 0.02, 0.625), "skirt")
    I.box((-0.60, 2.92, -0.02), (0.60, 3.22, 0.78), "dark")                  # the avionics
    I.box((0.60, 3.02, 0.20), (0.605, 3.06, 0.26), "amber")
    I.box((0.60, 3.02, 0.30), (0.605, 3.06, 0.36), "lamp")
    for sx in (-0.60, 0.60):
        for yy in (1.10, 2.86):
            I.faces += damper((sx, yy, ZB + 0.16), (sx, yy, 0.80)).faces
    sc = np.array([0.43, FY + 1.73, -0.62])         # the soldier's status display, on the cradle's wing, turned to the eyes
    nn = np.array([-0.85, 0, -0.53]); nn /= np.linalg.norm(nn)
    rr = np.cross(UP, nn)
    I.quad([sc - rr * 0.11 + UP * 0.075, sc + rr * 0.11 + UP * 0.075, sc + rr * 0.11 - UP * 0.075, sc - rr * 0.11 - UP * 0.075], UV1, "screen", nn)
    I.box((-0.30, 3.22, -0.92), (0.30, 3.26, -0.82), "lamp")
    m.lamps.append((np.array([0, 3.20, -0.87]), (1.0, 0.95, 0.85)))


def restraint_parts(state="closed"):
    return yoke(state), knee_bar(state)


# ------------------------------------------------------------------------------------------------ cutaways
def cut_front(m):
    """Off: the body's front half (and the lining's); the cut edges of the wall drawn."""
    keep = [f for f in m.faces if not (f[0].mean(0)[2] < -0.02 and 0.94 < f[0].mean(0)[1] < 3.40)]
    lin = [f for f in m.lining.faces if not (f[0].mean(0)[2] < -0.02)]
    out = Model(); out.faces = keep + lin + m.inner.faces; out.lamps = m.lamps; out.thrusters = m.thrusters
    for s_ in (-1, 1):
        out.quad([(s_ * AP_IN, 3.30, -0.019), (s_ * AP, 3.30, -0.019), (s_ * AP, 0.94, -0.019), (s_ * AP_IN, 0.94, -0.019)], UV1, "armor2", (0, 0, -1))
    out.quad([(-AP_IN, YF, -0.019), (AP_IN, YF, -0.019), (AP_IN, 0.94, -0.019), (-AP_IN, 0.94, -0.019)], UV1, "armor2", (0, 0, -1))
    return out


def cut_side(m):
    """Off: everything right of the middle (+x) through the body, and the inside's pieces whose middles are there; the
    cut wall, door and floor drawn on the middle plane (seen from +x)."""
    keep = [f for f in m.faces if not (f[0].mean(0)[0] > 0.02 and 0.94 < f[0].mean(0)[1] < 3.44)]
    lin = [f for f in m.lining.faces if not (f[0].mean(0)[0] > 0.02)]
    inner = [f for f in m.inner.faces if not (f[0].mean(0)[0] > 0.06)]
    out = Model(); out.faces = keep + lin + inner; out.lamps = m.lamps; out.thrusters = m.thrusters
    x = 0.019
    cut = [((AP_IN, 0.94), (AP, 3.30)),                   # the back wall
           ((-AP, 3.05 + 0.055), (-AP_IN, 3.30)), ((-AP, 0.94), (-AP_IN, 1.05 - 0.055)),
           ((-AP_IN, 0.94), (AP_IN, YF)),                 # the floor
           ((-AP - DOOR["lift"] + 0.02, DOOR["y0"]), (-AP + 0.05, DOOR["y1"]))]   # the hatch
    for (z0, y0), (z1, y1) in cut:
        out.quad([(x, y1, z0), (x, y1, z1), (x, y0, z1), (x, y0, z0)], [(0, 0), (z1 - z0, 0), (z1 - z0, y1 - y0), (0, y1 - y0)], "armor2", (1, 0, 0))
    return out
