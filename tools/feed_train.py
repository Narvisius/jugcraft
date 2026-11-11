"""The turbine hall's condensate and feedwater train and the turbine's oil, EHC, seal oil and hydrogen plant (turbine
hall part 2; Justin, 2026-10-18: "Pumps you can switch, plant responds"), drawn from their own parts as
tools/turbine_set.py draws the turbine, into the same legend texture (textures/entity/turbine_legends.png), their live
parts drawn by client/TurbineParts.java.

A Westinghouse four-loop plant's secondary (the PWR technology manual, 7.2 and 7.3): the three LP exhausts drop into a
three-shell surface condenser, one shell under each LP, the circulating water through each shell's tubes from one
waterbox to the other, the LP heaters 1 and 2 in the shells' necks; the condensate collects in each shell's hotwell and
three 50% condensate pumps (vertical can pumps in their pits) send it on through the LP heaters to the two 60%
turbine-driven main feed pumps, which push it through the HP heaters to the feed regulating valves. The main turbine's
bearings take oil from the main lube oil reservoir (its AC auxiliary, DC emergency and turning gear oil pumps and the lift
oil pumps on its top, the vapour extractor, the twin coolers and the duplex filter); its valves work on fire-resistant
fluid from the EHC unit; the generator's shaft seals take oil from the seal oil unit; its hydrogen comes through the gas
control panel.

The pieces (frame as the boards': x to the right, y up from the floor, z away from whoever placed it, so z 0 is their
front):

- condenser (12 wide, 6 deep, 6 tall; the plant's working condenser) and condenser_shell (the same, decor for the other
  two shells): the shell from its stiffened sides up through the expansion joint of its neck to the LP's exhaust, the
  waterboxes on both ends with their manways and the circulating water nozzles down, the LP heaters' heads through the
  ends, the air removal line, the vacuum breaker;
- condenser_hotwell (12 x 6 x 6, decor, under each shell): the shell's lower part, the hotwell on its concrete plinths,
  the condensate outlet down into the pit toward the pumps, the circulating water lines down from the waterboxes into
  the floor with their expansion joints and butterfly valves, the level glass and gauge board on the near end;
- condensate_pump (2 x 2 x 4): a vertical can pump, its barrel down in the pit under the grating, the discharge head
  and its nozzle, the motor stand with the coupling turning in its window, the motor, the local station with START
  and STOP;
- main_feed_pump (3 x 10 x 4): the barrel pump, the coupling in its guard, the driving turbine with its HP and LP stop
  valves and governor valve, steam up out of the floor and its exhaust down to the condenser, its lube oil console, the
  local panel with its speed, suction and discharge, TRIP and RESET;
- lube_oil_reservoir (4 x 8 x 4), ehc_unit (3 x 5 x 3), seal_oil_unit (3 x 5 x 3), h2_gas_panel (4 x 2 x 3)."""
import math
import zlib

import board_live as L
import control_boards as C
from control_boards import sw, _r
from diesel_set import rev, run, bolts, on_floor, plate, labelled
import turbine_set as T
from turbine_set import fl, gauge_z, gauge_x, lamp_z, front_plate, back_plate, side_plate, own_atlas, half_cyl, ring

DARK, MID, BLACK, WHITE = T.DARK, T.MID, T.BLACK, T.WHITE
STEEL, GUN, PANEL, YELLOW = T.STEEL, T.GUN, T.PANEL, T.YELLOW
SHELL = "tank_grey"              # the condenser's shells and hotwells
RIB = "carbon_steel"
MOTOR = "paint_blue"             # the pump motors
PUMP = "paint_green"             # the pump casings
OIL = "pipe_brown"


def tag_tex(text, w, h):
    """An engraved aluminium equipment tag: brushed metal, its legend cut in dark, a rivet hole at each end."""
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        import random
        d = L.ImageDraw.Draw(img)
        r = random.Random(zlib.crc32(text.encode()))
        for y in range(img.height):
            v = 196 + r.randint(-10, 10)
            d.line([(0, y), (img.width, y)], fill=(v, v + 3, v + 5, 255))
        d.rectangle([0, 0, img.width - 1, img.height - 1], outline=(120, 124, 128, 255), width=max(1, ph // 24))
        hr = max(2, ph // 9)
        for x in (ph // 3, img.width - ph // 3):
            d.ellipse([x - hr, img.height // 2 - hr, x + hr, img.height // 2 + hr], fill=(70, 72, 76, 255))
        lines = text.split("|")
        L._lines(d, lines, ph // 2 + hr, 2, img.width - ph - 2 * hr, img.height - 4, (36, 38, 42, 255), max(7, min(40, ph // len(lines) - 4)))
    return L.ATLAS.add(f"tag:{pw}x{ph}:{text}", pw, ph, draw)


def stencil_tex(text, w, h, colour=(236, 236, 228)):
    """Lettering stencilled on in paint: no plate behind it (cut out), the paint a little rough."""
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        import random
        img.paste((0, 0, 0, 0), (0, 0, img.width, img.height))
        d = L.ImageDraw.Draw(img)
        lines = text.split("|")
        L._lines(d, lines, 0, 0, img.width, img.height, colour + (255,), max(7, min(60, ph // len(lines) - 2)))
        r = random.Random(zlib.crc32(text.encode()))
        px_ = img.load()
        for _ in range(img.width * img.height // 60):
            x, y = r.randrange(img.width), r.randrange(img.height)
            if px_[x, y][3]:
                px_[x, y] = (0, 0, 0, 0)
    return L.ATLAS.add(f"stencil:{pw}x{ph}:{colour}:{text}", pw, ph, draw)


def gauge(name, unit, lo, hi, majors, r):
    return L.dial_face_tex(unit, lo, hi, majors, r * 1.44, 270)


def button(b, x, y, zf, colour, r=0.04):
    """A push button in its chrome collar on a face toward -z at zf."""
    b.cyl((x, y, zf), (x, y, zf - 0.02), r + 0.018, "stainless", "stainless", 12)
    b.cyl((x, y, zf - 0.02), (x, y, zf - 0.045), r, sw(colour), sw(colour), 12)


def handwheel(b, c, axis, r, mat=sw("red")):
    """A valve's handwheel: rim and spokes, square to axis ("x", "y" or "z")."""
    x, y, z = c
    d = {"x": (0.02, 0, 0), "y": (0, 0.02, 0), "z": (0, 0, 0.02)}[axis]
    rev(b, (x - d[0], y - d[1], z - d[2]), (x + d[0], y + d[1], z + d[2]), [(0, r), (0.04, r)], mat, None, None, 16)
    for k in range(3):
        a = math.pi * k / 3
        ca, sa = math.cos(a) * r, math.sin(a) * r
        if axis == "y":
            b.cyl((x - ca, y, z - sa), (x + ca, y, z + sa), 0.015, mat, mat, 4)
        elif axis == "x":
            b.cyl((x, y - ca, z - sa), (x, y + ca, z + sa), 0.015, mat, mat, 4)
        else:
            b.cyl((x - ca, y - sa, z), (x + ca, y + sa, z), 0.015, mat, mat, 4)


def gate_valve(b, c, axis, r, wheel=True):
    """A gate valve in a line along axis: its body, bonnet up, yoke and handwheel on top."""
    x, y, z = c
    d = {"x": (r * 1.2, 0, 0), "z": (0, 0, r * 1.2)}[axis]
    b.cyl((x - d[0], y, z - d[2]), (x + d[0], y, z + d[2]), r * 1.25, GUN, GUN, 12)
    b.cyl((x, y, z), (x, y + r * 2.2, z), r * 0.6, GUN, GUN, 10)
    b.cyl((x, y + r * 2.2, z), (x, y + r * 2.4, z), r * 0.9, GUN, GUN, 10)
    if wheel:
        b.cyl((x, y + r * 2.4, z), (x, y + r * 3.4, z), 0.02, "stainless", None, 6)
        handwheel(b, (x, y + r * 3.4, z), "y", max(0.12, r * 0.9))


def motor_v(b, cx, cz, y0, y1, r, tag=None, front=None):
    """A vertical motor from y0 up to y1: its frame with cooling ribs, the bearing bracket and the fan cowl on top,
    the terminal box on its side, lifting eyes."""
    b.cyl((cx, y0, cz), (cx, y0 + 0.08, cz), r + 0.08, MOTOR, MOTOR, 20)
    b.cyl((cx, y0 + 0.08, cz), (cx, y1 - 0.25, cz), r, MOTOR, None, 20)
    for k in range(int((y1 - y0 - 0.45) / 0.18)):
        y = y0 + 0.22 + k * 0.18
        rev(b, (cx, y, cz), (cx, y + 0.04, cz), [(0, r + 0.05), (0.04, r + 0.05)], MOTOR, None, None, 20)
    rev(b, (cx, y1 - 0.25, cz), (cx, y1, cz), [(0, r + 0.04), (0.12, r + 0.04), (0.25, r * 0.55)], MOTOR, None, MOTOR, 20)
    b.cyl((cx, y1 - 0.05, cz), (cx, y1, cz), r * 0.55, sw("dgrey"), sw("dgrey"), 16)
    # The terminal box on its right side, its conduit down to the floor.
    tb = cx + r
    b.box(tb - 0.02, y0 + 0.5, cz - 0.18, tb + 0.22, y0 + 0.85, cz + 0.18, MOTOR)
    b.cyl((tb + 0.12, y0 + 0.5, cz), (tb + 0.12, 0.0, cz), 0.035, "stainless", None, 6)
    for s in (-1, 1):
        b.box(cx + s * (r * 0.5) - 0.03, y1 - 0.12, cz - 0.03, cx + s * (r * 0.5) + 0.03, y1 + 0.06, cz + 0.03, GUN)
    if tag and front is not None:
        front_plate(b, cz - r - 0.002, cx - 0.22, cx + 0.22, y0 + 0.35, y0 + 0.55, tag, d=0.006)


def motor_h(b, c, axis, length, r, paint=MOTOR):
    """A horizontal motor along axis ("x" or "z") from c: its frame and ribs, its fan cowl at the far end, its feet."""
    x, y, z = c
    e = (length, 0, 0) if axis == "x" else (0, 0, length)
    end = (x + e[0], y, z + e[2])
    b.cyl(c, end, r, paint, paint, 16)
    for k in range(1, int(length / 0.12)):
        p = (x + e[0] * k / int(length / 0.12), y, z + e[2] * k / int(length / 0.12))
        q = (p[0] + (0.02 if axis == "x" else 0), y, p[2] + (0.02 if axis == "z" else 0))
        b.cyl(p, q, r + 0.03, paint, paint, 16)
    tip = (end[0] + (0.12 if axis == "x" else 0), y, end[2] + (0.12 if axis == "z" else 0))
    b.cyl(end, tip, r * 0.95, sw("dgrey"), sw("dgrey"), 16)
    for s in (-1, 1):
        if axis == "x":
            b.box(x + 0.05, 0.0 if y - r < 0.4 else y - r - 0.06, z + s * r * 0.6 - 0.05, end[0] - 0.05, y - r * 0.6, z + s * r * 0.6 + 0.05, paint)
        else:
            b.box(x + s * r * 0.6 - 0.05, y - r - 0.06, z + 0.05, x + s * r * 0.6 + 0.05, y - r * 0.6, end[2] - 0.05, paint)


def volute(b, c, axis, r, w, paint=PUMP):
    """A small pump's casing on axis: the volute and its suction and discharge stubs."""
    x, y, z = c
    d = (w / 2, 0, 0) if axis == "x" else (0, 0, w / 2)
    b.cyl((x - d[0], y, z - d[2]), (x + d[0], y, z + d[2]), r, paint, paint, 16)
    b.cyl((x, y + r * 0.6, z), (x, y + r * 1.5, z), r * 0.32, paint, paint, 10)


def gauge_board_z(b, live, x0, x1, y0, y1, zf, entries, title=None):
    """A small gauge board facing -z at zf: its panel, each gauge (name, unit, lo, hi, majors) across it, a title plate."""
    b.box(x0, y0, zf - 0.04, x1, y1, zf, [PANEL, None, DARK, DARK, DARK, DARK])
    n = len(entries)
    r = min(0.13, (x1 - x0) / n * 0.36, (y1 - y0) * 0.3)
    for k, (name, unit, lo, hi, majors) in enumerate(entries):
        gx = x0 + (x1 - x0) * (k + 0.5) / n
        gy = y0 + (y1 - y0) * 0.55
        gauge_z(b, live, name, (gx, gy, zf - 0.04), r, gauge(name, unit, lo, hi, majors, r))
        front_plate(b, zf - 0.04, gx - (x1 - x0) / n * 0.45, gx + (x1 - x0) / n * 0.45, y0 + 0.03, y0 + 0.03 + r * 0.55,
                    plate(name, (x1 - x0) / n * 0.9, r * 0.55, "white"), d=0.006)
    if title:
        front_plate(b, zf - 0.04, x0 + 0.04, x1 - 0.04, y1 - 0.09, y1 - 0.02, plate(title, x1 - x0 - 0.08, 0.07, "black"), d=0.006)


def lamps_z(b, live, x0, y, zf, entries, pitch=0.42):
    """Lamps with their legends along a face toward -z: (name, legend, colour)."""
    for k, (name, legend, colour) in enumerate(entries):
        lx = x0 + k * pitch
        lamp_z(b, live, name, lx, y, zf, colour, 0.04)
        front_plate(b, zf, lx - pitch / 2 + 0.02, lx + pitch / 2 - 0.02, y - 0.13, y - 0.065, plate(legend, pitch - 0.04, 0.065, "white"), d=0.006)


# ------------------------------------------------------------------------------------------------ the condenser
#
# One condenser shell is two pieces stacked: the condenser (the plant's working one, or condenser_shell for the other
# shells) over its hotwell (condenser_hotwell). Both are drawn from one description of the whole shell, 12 high from
# the floor to the turbine's exhaust, each piece taking its slice (the hotwell 0 to 6, the condenser 6 to 12): the
# concrete piers and the hotwell; the shell with its I-beam stiffeners; the tube sheets and their bolted flanges; the
# D-shaped waterboxes on both ends with their manways, vents, drains and anode boxes; the circulating water coming up
# out of the floor into each waterbox through its butterfly valve and rubber expansion joint; the air off-takes and
# their header; the makeup line and its control valve; the hotwell's outlet down to its pump; the level glass, level
# transmitter and gauges on the near end; the rubber expansion joint and its clamping bars under the LP's exhaust.

CW, CD, CT = 12, 6, 6
SX = (1.0, 11.0)                 # the tube sheets
SZ = (0.4, 5.6)                  # the shell's sides
HZ = (0.75, 5.25)                # the hotwell's sides
Y_HOT, Y_SHELL, Y_TOP = 0.7, 2.6, 9.6   # the hotwell's bottom, the shell's bottom and top (from the floor)
WB_Y = (2.9, 9.2)                # the waterboxes
WB_D, WB_HW = 0.8, 2.3           # their depth out from the tube sheet, half their width
WB_Z = 3.0
CW_R = 0.45                      # the circulating water lines (both on the near end: two passes)
CW_SPLIT = 1.2                   # their distance either side of the waterbox's middle
OUTLET = (10.75, 3.25)           # the hotwell's outlet (x, z): its pump stands beyond the far end, in line with it
SHELL_TAGS = ("1A", "1B", "1C")
SHELL = "cond_shell"
WB_PAINT = "cond_shell"
CW_PAINT = "pipe_green"


class Slice:
    """Draws into a board the part of the whole shell between lo and hi above the floor, moved down by lo: boxes and
    upright cylinders cut at the slice's ends (their cut faces left open), anything else drawn by the slice its middle
    falls in."""

    def __init__(self, b, lo, hi):
        self.b, self.lo, self.hi = b, lo, hi

    def has(self, y):
        return self.lo <= y < self.hi

    def box(self, x0, y0, z0, x1, y1, z1, faces):
        ya, yb = max(y0, self.lo), min(y1, self.hi)
        if yb - ya < 1e-4:
            return
        if isinstance(faces, str) or (isinstance(faces, list) and len(faces) == 5 and isinstance(faces[1], (int, float))):
            faces = [faces] * 6
        faces = list(faces)
        if y0 < self.lo - 1e-6:
            faces[5] = None
        if y1 > self.hi + 1e-6:
            faces[4] = None
        self.b.box(x0, ya - self.lo, z0, x1, yb - self.lo, z1, faces)

    def vcyl(self, x, z, y0, y1, r, mat, cap=None, sides=16):
        ya, yb = max(y0, self.lo), min(y1, self.hi)
        if yb - ya < 1e-4:
            return
        cap = mat if cap is None else cap
        rev(self.b, (x, ya - self.lo, z), (x, yb - self.lo, z), [(0, r), (yb - ya, r)], mat,
            cap if y0 >= self.lo - 1e-6 else None, cap if y1 <= self.hi + 1e-6 else None, sides)

    def cyl(self, a, c, r, mat, cap=None, sides=10):
        if self.has((a[1] + c[1]) / 2):
            self.b.cyl((a[0], a[1] - self.lo, a[2]), (c[0], c[1] - self.lo, c[2]), r, mat, cap, sides)

    def quad(self, p, q, r_, s, mat):
        if self.has(sum(v[1] for v in (p, q, r_, s)) / 4):
            self.b.quad(*[(v[0], v[1] - self.lo, v[2]) for v in (p, q, r_, s)], mat)

    def run(self, pts, r, mat, joints=None, sides=10):
        """A pipe along pts: each leg in the slices it crosses (an upright leg cut at their boundary)."""
        for p, q in zip(pts, pts[1:]):
            if abs(p[0] - q[0]) < 1e-6 and abs(p[2] - q[2]) < 1e-6:
                self.vcyl(p[0], p[2], min(p[1], q[1]), max(p[1], q[1]), r, mat, sides=sides)
            else:
                self.cyl(p, q, r, mat, None, sides)
        for p in pts[1:-1]:
            if self.has(p[1]):
                from diesel_set import joint
                joint(self.b, (p[0], p[1] - self.lo, p[2]), r * 1.04, joints or mat)

    def at(self, y):
        """A thing drawn whole by the slice its height y falls in: a shifted board to draw it on, or None."""
        return _Shifted(self.b, self.lo) if self.has(y) else None


class _Shifted:
    """A board's drawing calls with their heights moved down."""

    def __init__(self, b, dy):
        self.b, self.dy = b, dy

    def _p(self, p):
        return (p[0], p[1] - self.dy, p[2])

    def box(self, x0, y0, z0, x1, y1, z1, faces):
        self.b.box(x0, y0 - self.dy, z0, x1, y1 - self.dy, z1, faces)

    def cyl(self, a, c, r, mat, cap=None, sides=10):
        self.b.cyl(self._p(a), self._p(c), r, mat, cap, sides)

    def quad(self, p, q, r_, s, mat):
        self.b.quad(self._p(p), self._p(q), self._p(r_), self._p(s), mat)

    def obox(self, c, u, v, hu, hv, hw, faces):
        self.b.obox(self._p(c), u, v, hu, hv, hw, faces)

    def rev(self, a, c, profile, mat, cap_a=None, cap_b=None, sides=16):
        rev(self.b, self._p(a), self._p(c), profile, mat, cap_a, cap_b, sides)


def _ibeam_v(s, x, y0, y1, zf, side):
    """An upright I-beam stiffener on a long side (zf its face, side -1 the front): web out from the plate, flange."""
    zw = (zf - 0.24, zf) if side < 0 else (zf, zf + 0.24)
    zfl = (zf - 0.29, zf - 0.24) if side < 0 else (zf + 0.24, zf + 0.29)
    s.box(x - 0.035, y0, zw[0], x + 0.035, y1, zw[1], [None, None, SHELL, SHELL, None, None])
    s.box(x - 0.16, y0, zfl[0], x + 0.16, y1, zfl[1], SHELL)


def _ibeam_h(s, y, x0, x1, zf, side):
    """A horizontal girt across the side's stiffeners: its web and flange, shallower than the stiffeners."""
    zw = (zf - 0.16, zf) if side < 0 else (zf, zf + 0.16)
    zfl = (zf - 0.2, zf - 0.16) if side < 0 else (zf + 0.16, zf + 0.2)
    s.box(x0, y - 0.035, zw[0], x1, y + 0.035, zw[1], [None, None, None, None, SHELL, SHELL])
    s.box(x0, y - 0.13, zfl[0], x1, y + 0.13, zfl[1], SHELL)


def _waterbox(s, end, tag_live=None):
    """A D-shaped waterbox on an end (end -1 the near end, at x SX[0]; +1 the far): its curved cover, flat top and bottom,
    two ribs round it, the tube sheet's bolted flange behind it, its manway and davit, vent, drain and anode boxes."""
    xs = SX[0] if end < 0 else SX[1]
    n = 14
    pts = []
    for k in range(n + 1):
        t = math.pi * k / n
        pts.append((xs + end * WB_D * math.sin(t), WB_Z - WB_HW * math.cos(t)))
    y0, y1 = WB_Y
    for (xa, za), (xb, zb) in zip(pts, pts[1:]):
        # Its curved side, cut at the slices' boundary.
        for ya, yb in ((y0, min(y1, 6.0)), (max(y0, 6.0), y1)):
            if yb - ya > 1e-4 and s.has((ya + yb) / 2):
                if end < 0:
                    s.quad((xb, yb, zb), (xb, ya, zb), (xa, ya, za), (xa, yb, za), WB_PAINT)
                else:
                    s.quad((xa, yb, za), (xa, ya, za), (xb, ya, zb), (xb, yb, zb), WB_PAINT)
        # Its two ribs.
        k = pts.index((xa, za))
        ta, tb = math.pi * k / n, math.pi * (k + 1) / n
        ra = (xs + end * (WB_D + 0.06) * math.sin(ta), WB_Z - (WB_HW + 0.06) * math.cos(ta))
        rb = (xs + end * (WB_D + 0.06) * math.sin(tb), WB_Z - (WB_HW + 0.06) * math.cos(tb))
        for yr in (4.6, 7.6):
            if end < 0:
                s.quad((rb[0], yr + 0.06, rb[1]), (rb[0], yr - 0.06, rb[1]), (ra[0], yr - 0.06, ra[1]), (ra[0], yr + 0.06, ra[1]), WB_PAINT)
            else:
                s.quad((ra[0], yr + 0.06, ra[1]), (ra[0], yr - 0.06, ra[1]), (rb[0], yr - 0.06, rb[1]), (rb[0], yr + 0.06, rb[1]), WB_PAINT)
            for y, up in ((yr + 0.06, True), (yr - 0.06, False)):
                pa, pb = (xa, y, za), (xb, y, zb)
                qa, qb = (ra[0], y, ra[1]), (rb[0], y, rb[1])
                if (end < 0) == up:
                    s.quad(pa, qa, qb, pb, WB_PAINT)
                else:
                    s.quad(pb, qb, qa, pa, WB_PAINT)
    # Its top and bottom: fans from the tube sheet's middle, two segments to a quad.
    for y, up in ((y1, True), (y0, False)):
        c = (xs, y, WB_Z)
        for k in range(0, n, 2):
            a, m, e = [(pts[j][0], y, pts[j][1]) for j in (k, k + 1, k + 2)]
            if (end < 0) == up:
                s.quad(c, e, m, a, WB_PAINT)
            else:
                s.quad(c, a, m, e, WB_PAINT)
    # The tube sheet's flange round the cover: its frame and the bolt heads along it.
    fx = (xs - 0.1, xs) if end < 0 else (xs, xs + 0.1)
    za0, za1 = WB_Z - WB_HW - 0.2, WB_Z + WB_HW + 0.2
    ya0, ya1 = y0 - 0.15, y1 + 0.15
    for (zz0, zz1, yy0, yy1) in ((za0, za1, ya0, y0), (za0, za1, y1, ya1), (za0, WB_Z - WB_HW, y0, y1), (WB_Z + WB_HW, za1, y0, y1)):
        s.box(fx[0], yy0, zz0, fx[1], yy1, zz1, GUN)
    bx = xs + end * 0.1
    y = ya0 + 0.08
    while y < ya1:
        for z in (za0 + 0.09, za1 - 0.09):
            d = s.at(y)
            if d:
                d.cyl((bx, y, z), (bx + end * 0.04, y, z), 0.03, MID, MID, 6)
        y += 0.4
    z = za0 + 0.45
    while z < za1 - 0.3:
        for y in (ya0 + 0.07, ya1 - 0.07):
            d = s.at(y)
            if d:
                d.cyl((bx, y, z), (bx + end * 0.04, y, z), 0.03, MID, MID, 6)
        z += 0.4
    xa = xs + end * WB_D
    # The manway on the cover's face, its hinge and davit.
    d = s.at(4.6)
    if d:
        my, mz = 5.2, (WB_Z if end > 0 else WB_Z - 1.1)
        d.cyl((xa - end * 0.02, my, mz), (xa + end * 0.1, my, mz), 0.45, GUN, GUN, 20)
        for k in range(14):
            a = 2 * math.pi * k / 14
            d.cyl((xa + end * 0.1, my + math.cos(a) * 0.38, mz + math.sin(a) * 0.38), (xa + end * 0.14, my + math.cos(a) * 0.38, mz + math.sin(a) * 0.38), 0.025, MID, MID, 6)
        d.box(xa + end * 0.1 - 0.03, my - 0.06, mz + 0.42, xa + end * 0.1 + 0.03, my + 0.06, mz + 0.62, DARK)
        d.cyl((xa + end * 0.13, my - 0.25, mz + 0.62), (xa + end * 0.13, my + 0.75, mz + 0.62), 0.03, DARK, DARK, 6)
        d.cyl((xa + end * 0.13, my + 0.75, mz + 0.62), (xa + end * 0.13, my + 0.75, mz), 0.03, DARK, DARK, 6)
    # Anode junction boxes on the cover, their conduit down to the floor.
    for z in (WB_Z - 1.4, WB_Z + 1.4):
        xx = xs + end * (WB_D * math.sin(math.acos(max(-1, min(1, (WB_Z - z) / WB_HW)))) + 0.01)
        d = s.at(3.7)
        if d:
            d.box(min(xx, xx + end * 0.14), 3.55, z - 0.12, max(xx, xx + end * 0.14), 3.85, z + 0.12, PANEL)
            d.cyl((xx + end * 0.07, 3.55, z), (xx + end * 0.07, 3.0, z), 0.02, DARK, DARK, 4)
    # The vent on its top, its valve; the drain under it down to the floor (between the water lines on the inlet end).
    vx = xs + end * 0.45
    vz = WB_Z + 1.2 if end > 0 else WB_Z
    s.run([(vx, y1, vz), (vx, y1 + 0.35, vz)], 0.05, STEEL, sides=8)
    d = s.at(y1 + 0.3)
    if d:
        d.cyl((vx, y1 + 0.3, vz), (vx, y1 + 0.38, vz), 0.08, GUN, GUN, 8)
        d.cyl((vx, y1 + 0.38, vz), (vx, y1 + 0.5, vz), 0.015, "stainless", None, 4)
        d.cyl((vx - 0.1, y1 + 0.5, vz), (vx + 0.1, y1 + 0.5, vz), 0.012, sw("red"), sw("red"), 4)
    dz = WB_Z + 1.25 if end > 0 else WB_Z
    s.run([(vx, y0, dz), (vx, 0.0, dz)], 0.05, STEEL, sides=8)
    d = s.at(1.6)
    if d:
        d.cyl((vx, 1.55, dz), (vx, 1.7, dz), 0.09, GUN, GUN, 8)
        d.cyl((vx, 1.62, dz), (vx, 1.62, dz + 0.2), 0.015, "stainless", None, 4)
        d.rev((vx - 0.1, 1.62, dz + 0.2), (vx + 0.1, 1.62, dz + 0.2), [(0, 0.001), (0.0, 0.08), (0.02, 0.08), (0.02, 0.001)], sw("red"))
    if end > 0:
        return
    # Two passes: the inlet and outlet waterbox is split down its middle (its pass partition's bolted flange on the
    # cover); the circulating water comes up out of the floor into the inlet half and goes back down out of the outlet
    # half, each line through its butterfly valve and rubber expansion joint, its temperature element on it.
    xa = xs + end * WB_D
    s.box(xa - 0.07, y0 + 0.05, WB_Z - 0.09, xa + 0.0, y1 - 0.05, WB_Z + 0.09, GUN)
    y = y0 + 0.3
    while y < y1 - 0.2:
        d = s.at(y)
        if d:
            for dzb in (-0.05, 0.05):
                d.cyl((xa - 0.07, y, WB_Z + dzb), (xa - 0.11, y, WB_Z + dzb), 0.02, MID, MID, 4)
        y += 0.45
    for cz, side in ((WB_Z - CW_SPLIT, -1), (WB_Z + CW_SPLIT, 1)):
        cx = xs + end * 0.5
        s.vcyl(cx, cz, -0.1, y0, CW_R, CW_PAINT, sides=24)
        for (ya, yb, r, mat) in ((0.0, 0.06, CW_R + 0.12, MID), (1.05, 1.13, CW_R + 0.08, "flange"), (1.13, 1.43, CW_R + 0.05, GUN),
                                 (1.43, 1.51, CW_R + 0.08, "flange"), (2.2, 2.27, CW_R + 0.08, "flange"), (2.27, 2.6, CW_R + 0.06, BLACK),
                                 (2.6, 2.67, CW_R + 0.08, "flange"), (y0 - 0.07, y0, CW_R + 0.08, "flange")):
            s.vcyl(cx, cz, ya, yb, r, mat, sides=24)
        d = s.at(0.7)
        if d:
            tx = cx - CW_R
            d.cyl((tx, 0.75, cz), (tx - 0.12, 0.75, cz), 0.03, "stainless", "stainless", 6)
            d.box(tx - 0.26, 0.67, cz - 0.08, tx - 0.12, 0.83, cz + 0.08, PANEL)
            d.cyl((tx - 0.19, 0.67, cz), (tx - 0.19, 0.0, cz), 0.02, "stainless", None, 4)
        d = s.at(1.3)
        if d:
            # The butterfly valve's operator on the outer side: gearbox, the motor actuator over it, the handwheel.
            oz = cz + side * (CW_R + 0.06)
            za, zb = sorted((oz, oz + side * 0.3))
            d.box(cx - 0.17, 1.12, za, cx + 0.17, 1.48, zb, GUN)
            d.box(cx - 0.14, 1.48, za + 0.02, cx + 0.14, 1.88, zb - 0.02, sw("blue"))
            d.box(cx - 0.11, 1.88, za + 0.05, cx + 0.11, 1.94, zb - 0.05, GUN)
            hz = oz + side * 0.3
            d.cyl((cx, 1.3, hz), (cx, 1.3, hz + side * 0.12), 0.02, "stainless", None, 4)
            d.rev((cx, 1.3, hz + side * 0.12), (cx, 1.3, hz + side * 0.14), [(0, 0.15), (0.02, 0.15)], sw("red"))
            d.cyl((cx - 0.15, 1.3, hz + side * 0.13), (cx + 0.15, 1.3, hz + side * 0.13), 0.012, sw("red"), sw("red"), 4)


def _shell(s):
    """The shell, hotwell and piers, the stiffeners, the side manway, air off-takes, makeup line, outlet, expansion joint."""
    # The piers and their bearing plates.
    for z0, z1 in ((1.0, 1.8), (4.2, 5.0)):
        s.box(1.6, 0.0, z0, 10.4, Y_HOT - 0.06, z1, "concrete")
        s.box(1.5, Y_HOT - 0.06, z0 - 0.08, 10.5, Y_HOT, z1 + 0.08, STEEL)
    # The hotwell, the sloped plates up to the shell, the shell itself.
    s.box(SX[0], Y_HOT, HZ[0], SX[1], Y_SHELL - 0.6, HZ[1], [SHELL, SHELL, SHELL, SHELL, None, SHELL])
    yt = Y_SHELL - 0.6
    for (za, zb, front) in ((HZ[0], SZ[0], True), (HZ[1], SZ[1], False)):
        if front:
            s.quad((SX[0], Y_SHELL, zb), (SX[0], yt, za), (SX[1], yt, za), (SX[1], Y_SHELL, zb), SHELL)
        else:
            s.quad((SX[1], Y_SHELL, zb), (SX[1], yt, za), (SX[0], yt, za), (SX[0], Y_SHELL, zb), SHELL)
    for x, end in ((SX[0], -1), (SX[1], 1)):
        p = [(x, yt, HZ[0]), (x, yt, HZ[1]), (x, Y_SHELL, SZ[1]), (x, Y_SHELL, SZ[0])]
        s.quad(*(p if end > 0 else p[::-1]), SHELL)
    s.box(SX[0], Y_SHELL, SZ[0], SX[1], Y_TOP, SZ[1], [SHELL, SHELL, SHELL, SHELL, None, None])
    # The I-beam stiffeners and girts on both long sides, small ribs on the hotwell.
    for zf, side in ((SZ[0], -1), (SZ[1], 1)):
        for x in (2.0, 4.0, 6.0, 8.0, 10.0):
            _ibeam_v(s, x, Y_SHELL + 0.05, Y_TOP - 0.05, zf, side)
        for y in (4.3, 7.9):
            _ibeam_h(s, y, SX[0] + 0.05, SX[1] - 0.05, zf, side)
        zh = HZ[0] if side < 0 else HZ[1]
        for x in (2.0, 4.0, 6.0, 8.0, 10.0):
            zz = (zh - 0.08, zh) if side < 0 else (zh, zh + 0.08)
            s.box(x - 0.03, Y_HOT + 0.05, zz[0], x + 0.03, Y_SHELL - 0.65, zz[1], SHELL)
    # The expansion joint under the exhaust: the shell's top flange, the rubber belt, the clamping bars; the neck.
    s.box(SX[0] - 0.08, Y_TOP, SZ[0] - 0.08, SX[1] + 0.08, Y_TOP + 0.08, SZ[1] + 0.08, GUN)
    s.box(SX[0] + 0.05, Y_TOP + 0.08, SZ[0] + 0.05, SX[1] - 0.05, Y_TOP + 0.32, SZ[1] - 0.05, BLACK)
    s.box(SX[0] - 0.08, Y_TOP + 0.32, SZ[0] - 0.08, SX[1] + 0.08, Y_TOP + 0.4, SZ[1] + 0.08, GUN)
    x = SX[0] + 0.3
    while x < SX[1] - 0.2:
        for z in (SZ[0] - 0.08, SZ[1] + 0.08):
            d = s.at(Y_TOP + 0.36)
            if d:
                d.cyl((x, Y_TOP + 0.4, z), (x, Y_TOP + 0.46, z), 0.025, MID, MID, 6)
        x += 0.5
    s.box(SX[0] + 0.1, Y_TOP + 0.4, SZ[0] + 0.1, SX[1] - 0.1, 12.0, SZ[1] - 0.1, [SHELL, SHELL, SHELL, SHELL, BLACK, None])
    # The manway on the near side of the shell, its davit.
    d = s.at(3.6)
    if d:
        mx, my, zf = 3.0, 3.5, SZ[0]
        d.cyl((mx, my, zf + 0.02), (mx, my, zf - 0.3), 0.36, SHELL, GUN, 20)
        d.cyl((mx, my, zf - 0.3), (mx, my, zf - 0.38), 0.45, GUN, GUN, 20)
        for k in range(14):
            a = 2 * math.pi * k / 14
            d.cyl((mx + math.cos(a) * 0.4, my + math.sin(a) * 0.4, zf - 0.38), (mx + math.cos(a) * 0.4, my + math.sin(a) * 0.4, zf - 0.42), 0.025, MID, MID, 6)
        d.cyl((mx + 0.62, my - 0.5, zf - 0.45), (mx + 0.62, my + 0.75, zf - 0.45), 0.03, DARK, DARK, 6)
        d.cyl((mx + 0.62, my + 0.75, zf - 0.45), (mx, my + 0.75, zf - 0.45), 0.03, DARK, DARK, 6)
        front_plate(d.b, zf - 0.3, mx - 0.62, mx - 0.02, my + 0.55 - s.lo, my + 0.95 - s.lo, plate("DANGER|CONFINED SPACE|ENTRY BY PERMIT", 0.6, 0.4, "red"), d=0.006)
    # The air off-takes from the air cooling sections down the near side into their header, on to the vacuum pumps.
    for x in (5.0, 7.0):
        s.run([(x, 7.2, SZ[0]), (x, 7.2, SZ[0] - 0.36), (x, 3.1, SZ[0] - 0.36)], 0.07, STEEL, joints=DARK, sides=8)
        d = s.at(6.4)
        if d:
            d.cyl((x, 6.3, SZ[0] - 0.36), (x, 6.45, SZ[0] - 0.36), 0.11, GUN, GUN, 8)
            d.cyl((x, 6.38, SZ[0] - 0.36), (x, 6.38, SZ[0] - 0.56), 0.015, "stainless", None, 4)
            d.rev((x, 6.38, SZ[0] - 0.56), (x, 6.38, SZ[0] - 0.58), [(0, 0.09), (0.02, 0.09)], sw("red"))
    s.run([(4.7, 3.1, SZ[0] - 0.36), (8.9, 3.1, SZ[0] - 0.36), (8.9, 0.0, SZ[0] - 0.36)], 0.1, STEEL, joints=DARK, sides=10)
    # The makeup water line up out of the floor into the hotwell through its control valve.
    mz = HZ[0] - 0.25
    s.run([(6.0, 0.0, mz), (6.0, 1.5, mz), (6.0, 1.5, HZ[0])], 0.06, "pipe_lblue", joints=DARK, sides=8)
    d = s.at(0.9)
    if d:
        d.cyl((5.85, 0.82, mz), (6.15, 0.82, mz), 0.1, GUN, GUN, 10)
        d.cyl((6.0, 0.82, mz), (6.0, 1.05, mz), 0.03, "stainless", None, 6)
        d.rev((6.0, 1.05, mz), (6.0, 1.25, mz), [(0, 0.04), (0.05, 0.16), (0.15, 0.16), (0.2, 0.04)], sw("blue"))
        d.box(6.12, 0.9, mz - 0.06, 6.28, 1.08, mz + 0.06, sw("dgrey"))
    # The shell pressure and hotwell level transmitters on their rack at the front's far end, their tubing up to the
    # taps on the shell.
    d = s.at(1.0)
    if d:
        rz = HZ[0] - 0.35
        for x in (9.55, 10.55):
            d.box(x - 0.03, 0.0, rz - 0.03, x + 0.03, 1.75, rz + 0.03, DARK)
        d.box(9.45, 0.0, rz - 0.12, 10.65, 0.03, rz + 0.12, DARK)
        d.box(9.58, 1.25, rz - 0.025, 10.52, 1.3, rz + 0.025, DARK)
        for k, x in enumerate((9.8, 10.3)):
            d.box(x - 0.1, 1.0, rz - 0.15, x + 0.1, 1.25, rz - 0.04, sw("blue"))
            d.box(x - 0.06, 1.05, rz - 0.16, x + 0.06, 1.14, rz - 0.15, [sw("white")] * 6)
    for k, x in enumerate((9.8, 10.3)):
        # Up the rack's face, in to the sloped plate, up its face to the tap.
        ty = (1.9, 3.1)[k]
        s.run([(x, 1.25, HZ[0] - 0.42), (x, ty, HZ[0] - 0.42), (x, ty, SZ[0] - 0.02)], 0.012, "stainless", sides=4)
    # The hotwell's outlet down into the pit to its pump: nozzle, expansion joint, isolation valve.
    ox, oz = OUTLET
    s.vcyl(ox, oz, -0.1, Y_HOT, 0.24, "pipe_lblue", sides=16)
    for (ya, yb, r, mat) in ((Y_HOT - 0.06, Y_HOT, 0.32, "flange"), (0.42, 0.62, 0.28, BLACK), (0.36, 0.42, 0.32, "flange"),
                             (0.62, 0.68, 0.32, "flange"), (0.0, 0.05, 0.34, MID)):
        s.vcyl(ox, oz, ya, yb, r, mat, sides=16)


def _near_end(s, live):
    """The far end's instruments (toward the corridor and the pump): the gauge board on the pump's left, the level
    glass and the level transmitter on its right."""
    d = s.at(1.5)
    if not d:
        return
    xf = SX[1]
    # The gauge board on its stand, facing +x.
    z0, z1 = 0.3, 1.9
    d.box(xf + 0.05, 0.0, (z0 + z1) / 2 - 0.04, xf + 0.13, 1.4, (z0 + z1) / 2 + 0.04, DARK)
    d.box(xf + 0.02, 0.0, (z0 + z1) / 2 - 0.2, xf + 0.18, 0.03, (z0 + z1) / 2 + 0.2, DARK)
    d.box(xf + 0.13, 1.4, z0, xf + 0.18, 2.45, z1, [DARK, DARK, None, PANEL, DARK, DARK])
    for k, (name, unit, lo, hi, majors) in enumerate((("VACUUM", "IN HG", 0, 30, 7), ("HOTWELL", "IN", 0, 60, 7))):
        z = z0 + 0.4 + k * 0.8
        gauge_x(d.b, live, name, (xf + 0.18, 2.0 - s.lo, z), 0.2, gauge(name, unit, lo, hi, majors, 0.2), 1)
        side_plate(d.b, 1, xf + 0.18, z - 0.33, z + 0.33, 1.5 - s.lo, 1.66 - s.lo, plate(name, 0.66, 0.16, "white"), d=0.005)
    labelled(d.b, SHELL_TAGS, lambda tg: side_plate(d.b, 1, xf + 0.18, z0, z1, 2.3 - s.lo, 2.43 - s.lo, plate(f"CONDENSER {tg} HOTWELL", 1.6, 0.13, "black"), d=0.005))
    # The level glass: its cocks into the hotwell and the shell, the glass in its guard (the level the game's).
    gz = 5.0
    for y in (1.0, 2.75):
        d.cyl((xf, y, gz), (xf + 0.2, y, gz), 0.03, "stainless", "stainless", 6)
        d.box(xf + 0.16, y - 0.05, gz - 0.05, xf + 0.24, y + 0.05, gz + 0.05, GUN)
    d.box(xf + 0.14, 1.05, gz - 0.08, xf + 0.26, 2.7, gz - 0.06, "stainless")
    d.box(xf + 0.14, 1.05, gz + 0.06, xf + 0.26, 2.7, gz + 0.08, "stainless")
    d.box(xf + 0.14, 1.05, gz - 0.06, xf + 0.16, 2.7, gz + 0.06, sw("white"))
    live["levels"].append({"name": "HOTWELL", "z0": _r(gz - 0.05)[0], "z1": _r(gz + 0.05)[0], "y0": _r(1.07 - s.lo)[0], "y1": _r(2.68 - s.lo)[0],
                           "x": _r(xf + 0.165)[0], "side": 1})
    # The level transmitter's standpipe and its transmitter.
    tz = 5.55
    for y in (0.95, 2.8):
        d.cyl((xf, y, tz), (xf + 0.22, y, tz), 0.025, "stainless", "stainless", 6)
    d.cyl((xf + 0.22, 0.95, tz), (xf + 0.22, 2.8, tz), 0.07, "stainless", "stainless", 10)
    d.box(xf + 0.3, 1.6, tz - 0.1, xf + 0.46, 1.85, tz + 0.1, sw("blue"))
    d.cyl((xf + 0.22, 1.72, tz), (xf + 0.3, 1.72, tz), 0.02, "stainless", None, 4)
    d.box(xf + 0.33, 1.85, tz - 0.06, xf + 0.43, 1.93, tz + 0.06, GUN)


def _condenser_board(seed, lo, hi):
    b = C.Board(CW, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "levels": []}
    s = Slice(b, lo, hi)
    _shell(s)
    b.core = len(b.still)
    for end in (-1, 1):
        _waterbox(s, end)
    _near_end(s, live)
    # The shell's tag on its near side, the waterboxes' on their covers.
    if s.has(8.0):
        # (Painted on: its tag stencilled on the shell, as the hall's other machines have theirs.)
        labelled(b, SHELL_TAGS, lambda tg: front_plate(b, SZ[0] - 0.225, 4.2, 7.8, 8.4 - lo, 9.0 - lo, stencil_tex(f"CONDENSER {tg}", 3.6, 0.6, (34, 36, 38)),
                                                       edge=None, d=0.003))
        for end, word, z in ((-1, "INLET", WB_Z - 1.1), (-1, "OUTLET", WB_Z + 1.1), (1, "RETURN", WB_Z)):
            xa = (SX[0] if end < 0 else SX[1]) + end * (WB_D * math.sin(math.acos(max(-1, min(1, (WB_Z - z) / WB_HW)))) + 0.04)
            labelled(b, SHELL_TAGS, lambda tg, xa=xa, end=end, word=word, z=z: side_plate(
                b, end, xa, z - 0.55, z + 0.55, 8.2 - lo, 8.6 - lo, plate(f"WATERBOX {tg}|{word}", 1.1, 0.4, "white"), d=0.008))
    live["mats"] = {"needle": sw("red"), "water": "water"}
    b.live = live
    on_floor(b)
    return b


def condenser(seed=421):
    """A condenser shell's upper half (the plant's working condenser, or another shell's): see the section's notes."""
    with own_atlas():
        return _condenser_board(seed, 6.0, 12.0)


def hotwell(seed=423):
    """A condenser shell's lower half, the hotwell under it: see the section's notes."""
    with own_atlas():
        return _condenser_board(seed, 0.0, 6.0)


# ------------------------------------------------------------------------------------------------ condensate pump
#
# A vertical can pump (3 wide, 2 deep, 5 tall; its front toward the corridor): the can down in the pit under the
# grating, its suction isolation valve's floor stand; the discharge head on its sole plate, the discharge nozzle through
# its swing check and gate valves and down into the pit, the gauge on its root valve; the motor stand with its two
# guarded windows, the stuffing box and the spacer coupling turning inside (the game's); the vertical motor: the
# finned frame, end bell, fan cover and drip cover, terminal box and conduit, nameplate, lifting lugs; the drain
# funnel; the local station at its front left: the ammeter, RUN, STOPPED and AUTO lamps, START and STOP.

CP_TAGS = ("1A", "1B", "1C")
CPW, CPD, CPT = 3, 2, 5
CPX, CPZ = 1.25, 1.05            # the pump's axis
CP_MOTOR = "paint_blue"
CP_STATION = (0.05, 0.55, 0.02)  # the local station: its x across, its front face
CP_START = (0.17, 1.12)          # its buttons (x, y; the game's click spots, TurbineHall)
CP_STOP = (0.43, 1.12)


def _arc(b, cx, cz, r0, r1, y0, y1, a0, a1, mat, n=6):
    """A part of a thick upright cylinder between angles a0 and a1 (0 toward +x, 90 toward +z): its outer and inner
    faces and its two cut ends."""
    pts = [math.radians(a0 + (a1 - a0) * k / n) for k in range(n + 1)]
    for ta, tb in zip(pts, pts[1:]):
        oa = (cx + r1 * math.cos(ta), cz + r1 * math.sin(ta))
        ob = (cx + r1 * math.cos(tb), cz + r1 * math.sin(tb))
        ia = (cx + r0 * math.cos(ta), cz + r0 * math.sin(ta))
        ib = (cx + r0 * math.cos(tb), cz + r0 * math.sin(tb))
        b.quad((ob[0], y1, ob[1]), (ob[0], y0, ob[1]), (oa[0], y0, oa[1]), (oa[0], y1, oa[1]), mat)
        b.quad((ia[0], y1, ia[1]), (ia[0], y0, ia[1]), (ib[0], y0, ib[1]), (ib[0], y1, ib[1]), mat)
    for t, s in ((pts[0], 1), (pts[-1], -1)):
        o = (cx + r1 * math.cos(t), cz + r1 * math.sin(t))
        i = (cx + r0 * math.cos(t), cz + r0 * math.sin(t))
        q = [(o[0], y1, o[1]), (o[0], y0, o[1]), (i[0], y0, i[1]), (i[0], y1, i[1])]
        b.quad(*(q if s > 0 else q[::-1]), mat)


def condensate_pump(seed=425):
    with own_atlas():
        return _condensate_pump(seed)


def _condensate_pump(seed=425):
    b = C.Board(CPW, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "couplings": []}
    cx, cz = CPX, CPZ
    # The pit's grating round the can and its yellow toe plate at the front; the sole plate and its anchor nuts.
    b.box(0.05, 0.0, 0.25, 2.95, 0.02, 1.97, "grating")
    b.box(0.05, 0.0, 0.2, 2.95, 0.1, 0.25, YELLOW)
    b.box(0.6, 0.02, 0.4, 1.9, 0.1, 1.7, STEEL)
    for x in (0.72, 1.78):
        for z in (0.52, 1.58):
            b.cyl((x, 0.1, z), (x, 0.16, z), 0.04, MID, MID, 6)
            b.cyl((x, 0.16, z), (x, 0.2, z), 0.018, MID, MID, 4)
    # The can's head flange and the discharge head, its top flange.
    b.cyl((cx, 0.1, cz), (cx, 0.22, cz), 0.62, PUMP, PUMP, 22)
    bolts(b, (cx, 0.22, cz), 0.56, 16, 0.025, 0.04, MID, "y")
    rev(b, (cx, 0.22, cz), (cx, 1.05, cz), [(0, 0.5), (0.45, 0.46), (0.73, 0.46), (0.73, 0.58), (0.83, 0.58)], PUMP, None, PUMP, 22)
    b.core = len(b.still)
    # The discharge: out of the head to the right, the swing check valve (its cover and hinge boss), the gate valve
    # (bonnet, yoke, stem, handwheel), down into the pit at the right.
    dy = 0.62
    run(b, [(cx + 0.4, dy, cz), (2.7, dy, cz), (2.7, -0.1, cz)], 0.19, PUMP, joints=PUMP, sides=14)
    for x in (1.74, 2.14, 2.2, 2.52):
        b.cyl((x - 0.03, dy, cz), (x + 0.03, dy, cz), 0.27, "flange", MID, 16)
    b.cyl((1.77, dy, cz), (2.11, dy, cz), 0.25, GUN, GUN, 16)
    rev(b, (1.94, dy + 0.18, cz), (1.94, dy + 0.38, cz), [(0, 0.17), (0.12, 0.15), (0.2, 0.06)], GUN, None, GUN, 14)
    b.cyl((1.94, dy + 0.05, cz - 0.27), (1.94, dy + 0.05, cz - 0.33), 0.06, GUN, GUN, 8)
    b.cyl((2.23, dy, cz), (2.49, dy, cz), 0.25, GUN, GUN, 16)
    b.cyl((2.36, dy + 0.2, cz), (2.36, dy + 0.55, cz), 0.12, GUN, GUN, 12)
    b.cyl((2.36, dy + 0.55, cz), (2.36, dy + 0.62, cz), 0.17, GUN, GUN, 12)
    for dz in (-0.1, 0.1):
        b.box(2.33, dy + 0.62, cz + dz - 0.025, 2.39, dy + 1.0, cz + dz + 0.025, GUN)
    b.box(2.31, dy + 0.98, cz - 0.13, 2.41, dy + 1.04, cz + 0.13, GUN)
    b.cyl((2.36, dy + 0.62, cz), (2.36, dy + 1.18, cz), 0.022, "stainless", None, 6)
    handwheel(b, (2.36, dy + 1.1, cz), "y", 0.2)
    b.cyl((2.7, 0.0, cz), (2.7, 0.05, cz), 0.29, MID, MID, 14)
    # The discharge gauge on its root valve over the nozzle.
    run(b, [(1.6, dy + 0.18, cz), (1.6, 1.15, cz), (1.6, 1.15, cz - 0.42)], 0.018, "stainless", sides=4)
    b.cyl((1.6, 0.95, cz - 0.05), (1.6, 0.95, cz + 0.05), 0.04, GUN, GUN, 6)
    gauge_z(b, live, "CP DISCH", (1.6, 1.15, cz - 0.44), 0.12, gauge("DISCH", "PSIG", 0, 600, 7, 0.12))
    # The suction valve's floor stand at the back left: its pedestal, position indicator, handwheel.
    fx, fz = 0.28, 1.72
    b.box(fx - 0.14, 0.02, fz - 0.14, fx + 0.14, 0.06, fz + 0.14, GUN)
    rev(b, (fx, 0.06, fz), (fx, 0.95, fz), [(0, 0.1), (0.1, 0.06), (0.85, 0.05), (0.89, 0.08)], GUN, None, GUN, 10)
    b.box(fx - 0.03, 0.55, fz - 0.07, fx + 0.03, 0.85, fz - 0.05, [sw("white")] * 6)
    b.cyl((fx, 0.95, fz), (fx, 1.05, fz), 0.02, "stainless", None, 4)
    handwheel(b, (fx, 1.02, fz), "y", 0.16)
    # The drain funnel at the back right, its hub.
    rev(b, (2.55, 0.0, 1.7), (2.55, 0.3, 1.7), [(0, 0.05), (0.15, 0.05), (0.3, 0.13)], STEEL, None, None, 10)
    run(b, [(cx + 0.35, 0.3, cz + 0.3), (2.55, 0.3, 1.55), (2.55, 0.35, 1.7)], 0.015, "stainless", sides=4)
    # The motor stand: its flanges and two solid sides, open front and back with guards over the windows; inside it
    # the stuffing box, the shaft and the coupling (the game's), the seal water tube.
    y0, y1 = 1.05, 2.12
    b.cyl((cx, y0, cz), (cx, y0 + 0.1, cz), 0.6, PUMP, PUMP, 22)
    b.cyl((cx, y1 - 0.1, cz), (cx, y1, cz), 0.66, PUMP, PUMP, 22)
    for a0, a1 in ((-40, 40), (140, 220)):
        _arc(b, cx, cz, 0.42, 0.52, y0 + 0.1, y1 - 0.1, a0, a1, PUMP)
    for zf, s in ((cz - 0.5, -1), (cz + 0.5, 1)):
        q = [(cx - 0.36, y1 - 0.12, zf), (cx - 0.36, y0 + 0.12, zf), (cx + 0.36, y0 + 0.12, zf), (cx + 0.36, y1 - 0.12, zf)]
        b.quad(*(q if s < 0 else q[::-1]), "grating")
        b.box(cx - 0.38, y0 + 0.1, zf - 0.015, cx + 0.38, y0 + 0.14, zf + 0.015, YELLOW)
        b.box(cx - 0.38, y1 - 0.14, zf - 0.015, cx + 0.38, y1 - 0.1, zf + 0.015, YELLOW)
    rev(b, (cx, y0 + 0.1, cz), (cx, y0 + 0.36, cz), [(0, 0.2), (0.18, 0.2), (0.18, 0.13), (0.26, 0.13)], GUN, None, GUN, 14)
    b.cyl((cx, y0 + 0.36, cz), (cx, y0 + 0.47, cz), 0.07, "stainless_dark", None, 10)
    b.cyl((cx, y0 + 0.85, cz), (cx, y1 - 0.1, cz), 0.07, "stainless_dark", None, 10)
    live["couplings"].append({"c": _r(cx, y0 + 0.47, cz), "axis": [0, 1, 0], "len": 0.38, "r": 0.2})
    run(b, [(cx - 0.4, y0 + 0.2, cz + 0.15), (cx - 0.18, y0 + 0.2, cz + 0.15)], 0.015, "stainless", sides=4)
    run(b, [(cx - 0.4, y0 + 0.2, cz + 0.15), (cx - 0.4, 0.5, cz + 0.15)], 0.015, "stainless", sides=4)
    # The motor: base flange, the frame and its fins, the end bell, the fan cover and its drip cover on three legs.
    m0, m1, mr = y1, 4.1, 0.58
    b.cyl((cx, m0, cz), (cx, m0 + 0.1, cz), mr + 0.1, CP_MOTOR, CP_MOTOR, 24)
    b.cyl((cx, m0 + 0.1, cz), (cx, m1, cz), mr, CP_MOTOR, None, 24)
    for k in range(16):
        a = 2 * math.pi * (k + 0.5) / 16
        if abs(a - 1.5 * math.pi) < 0.45:
            continue                 # the front's flat pad, the nameplates on it
        u = (math.cos(a), 0.0, math.sin(a))
        c = (cx + math.cos(a) * (mr + 0.05), (m0 + m1) / 2 + 0.05, cz + math.sin(a) * (mr + 0.05))
        b.obox(c, u, (0, 1, 0), 0.06, (m1 - m0 - 0.3) / 2, 0.012, CP_MOTOR)
    rev(b, (cx, m1, cz), (cx, m1 + 0.22, cz), [(0, mr + 0.02), (0.08, mr + 0.02), (0.22, mr - 0.12)], CP_MOTOR, None, None, 24)
    b.cyl((cx, m1 + 0.22, cz), (cx, m1 + 0.55, cz), mr - 0.1, sw("dgrey"), sw("dgrey"), 24)
    for y in (m1 + 0.3, m1 + 0.4):
        rev(b, (cx, y, cz), (cx, y + 0.03, cz), [(0, mr - 0.08), (0.03, mr - 0.08)], "grating", None, None, 24)
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.4
        px, pz = cx + math.cos(a) * (mr - 0.2), cz + math.sin(a) * (mr - 0.2)
        b.cyl((px, m1 + 0.55, pz), (px, m1 + 0.78, pz), 0.025, GUN, GUN, 6)
    b.cyl((cx, m1 + 0.78, cz), (cx, m1 + 0.84, cz), mr + 0.05, CP_MOTOR, CP_MOTOR, 24)
    for s in (-1, 1):
        b.box(cx + s * 0.35 - 0.04, m1 - 0.05, cz - 0.04, cx + s * 0.35 + 0.04, m1 + 0.2, cz + 0.04, CP_MOTOR)
    # Its terminal box on the right, the flexible conduit down to the floor; the heater and RTD box on the back.
    b.box(cx + mr + 0.02, m0 + 0.5, cz - 0.3, cx + mr + 0.32, m0 + 1.05, cz + 0.22, CP_MOTOR)
    b.box(cx + mr + 0.32, m0 + 0.55, cz - 0.25, cx + mr + 0.34, m0 + 1.0, cz + 0.17, sw("dgrey"))
    run(b, [(cx + mr + 0.17, m0 + 0.5, cz + 0.12), (cx + mr + 0.17, m0 + 0.2, cz + 0.45), (cx + mr + 0.17, 0.0, cz + 0.45)], 0.045, BLACK, sides=8)
    b.box(cx - 0.2, m0 + 0.9, cz + mr + 0.02, cx + 0.2, m0 + 1.15, cz + mr + 0.18, CP_MOTOR)
    # The motor's nameplates on a flat pad on its front, between the fins.
    pz = cz - mr + 0.04
    b.box(cx - 0.24, m0 + 0.55, pz - 0.04, cx + 0.24, m0 + 1.45, pz, [CP_MOTOR, None, CP_MOTOR, CP_MOTOR, CP_MOTOR, CP_MOTOR])
    labelled(b, CP_TAGS, lambda tg: front_plate(b, pz - 0.04, cx - 0.2, cx + 0.2, m0 + 1.08, m0 + 1.38, plate(f"CP-{tg}", 0.4, 0.3, "white"), d=0.006))
    front_plate(b, pz - 0.04, cx - 0.2, cx + 0.2, m0 + 0.65, m0 + 0.95, plate("4000 V|1250 HP|1185 RPM", 0.4, 0.3, "white"), d=0.006)
    # The local station on its post at the front left.
    sx0, sx1, zf = CP_STATION
    b.box(0.27, 0.0, zf + 0.06, 0.33, 1.0, zf + 0.12, DARK)
    b.box(0.2, 0.0, zf + 0.02, 0.4, 0.02, zf + 0.16, DARK)
    b.box(sx0, 1.0, zf, sx1, 1.72, zf + 0.16, [PANEL] * 6)
    gauge_z(b, live, "CP AMPS", (0.3, 1.55, zf), 0.075, gauge("AMPS", "A", 0, 300, 4, 0.075))
    for k, (name, colour) in enumerate((("cp_run", "red"), ("cp_stop", "green"), ("cp_auto", "amber"))):
        lamp_z(b, live, name, 0.13 + k * 0.17, 1.33, zf, colour, 0.028)
    for (bx, by), colour, word in ((CP_START, "black", "START"), (CP_STOP, "red", "STOP")):
        button(b, bx, by, zf, colour, 0.04)
        front_plate(b, zf, bx - 0.11, bx + 0.11, by - 0.1, by - 0.06, plate(word, 0.22, 0.04, "white"), d=0.004)
    front_plate(b, zf, 0.07, 0.53, 1.24, 1.27, plate("RUN  STOP  AUTO", 0.46, 0.03, "white"), d=0.004)
    labelled(b, CP_TAGS, lambda tg: front_plate(b, zf, sx0, sx1, 1.74, 1.9, plate(f"COND PUMP {tg}", 0.5, 0.16, "black"), d=0.008))
    live["mats"] = {"needle": sw("red"), "hub": "stainless_dark", "spacer": "gunmetal", "bolt": "stainless", "mark": sw("white")}
    b.live = live
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ main feed pump
#
# A turbine-driven main feed pump set (4 wide, 11 long, 5 tall; its front, the pump end, toward the aisle) on its
# concrete plinth: the barrel pump (its forged barrel, the end cover's big stud nuts, bearing housings and seal
# coolers, suction up out of the floor, discharge up through its check valve and over through the motor-operated gate
# valve and down, the recirculation line); the coupling in its guard (the game's coupling inside); the driving turbine
# (its lagged casing, horizontal joint and stud nuts, the exhaust down through its expansion joint toward the
# condenser, LP and HP steam up out of the floor through their stop valves into the steam chest, the governor valves'
# bar lift and servo, the governor pedestal at the far end with its trip block); the lube oil console beside the pump
# (reservoir, AC and DC oil pumps, cooler, duplex filter, the oil lines along the set); the local panel at the front.

MFP_TAGS = ("1A", "1B")
MFW, MFD, MFT = 4, 11, 5
MX, MY = 1.6, 1.3                # the shaft line (x, y), along z
PLINTH = 0.35
PUMP_Z = (1.0, 3.9)
COUP_Z = (3.9, 4.8)
EXH_Z = (4.8, 6.9)               # the turbine's exhaust end
HP_Z = (6.9, 9.2)                # its steam end
GOV_Z = (9.2, 10.5)              # the governor pedestal
MFP_TRIP = (3.1, 1.35)           # the control station's TRIP and RESET (x, y on its door at z 0.06; the game's click spots)
MFP_RESET = (3.44, 1.35)
ACT_MFP = "gunmetal"             # the hydraulic actuators
OIL_X, OIL_Y = 2.82, (0.9, 0.72)  # the oil supply and drain headers along the set's right side (x; their heights)


def vmotor(b, cx, cz, y0, y1, r, paint=MOTOR, cowl=True):
    """A vertical motor (as the condensate pump's, smaller): base flange, finned frame, end bell, fan cover."""
    b.cyl((cx, y0, cz), (cx, y0 + 0.06, cz), r + 0.06, paint, paint, 16)
    b.cyl((cx, y0 + 0.06, cz), (cx, y1 - 0.15, cz), r, paint, None, 16)
    for k in range(10):
        a = 2 * math.pi * (k + 0.5) / 10
        c = (cx + math.cos(a) * (r + 0.03), (y0 + y1) / 2 - 0.05, cz + math.sin(a) * (r + 0.03))
        b.obox(c, (math.cos(a), 0.0, math.sin(a)), (0, 1, 0), 0.035, (y1 - y0 - 0.35) / 2, 0.008, paint)
    rev(b, (cx, y1 - 0.15, cz), (cx, y1, cz), [(0, r), (0.15, r * 0.8)], sw("dgrey"), None, sw("dgrey"), 16)


def main_feed_pump(seed=427):
    with own_atlas():
        return _main_feed_pump(seed)


def _main_feed_pump(seed=427):
    b = C.Board(MFW, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "couplings": [], "digits": []}
    # The concrete plinth and the steel sole plates under each pedestal.
    b.box(0.25, 0.0, 0.55, 3.95, PLINTH, 10.8, "concrete")
    b.core = len(b.still)
    for z0, z1 in ((0.75, 1.25), (3.65, 4.15), (8.55, 9.15), (9.4, 10.4)):
        b.box(MX - 0.75, PLINTH, z0, MX + 0.75, PLINTH + 0.06, z1, STEEL)
    # ---------------------------------------------------------------- the pump
    z0, z1 = PUMP_Z
    # Its pedestals and bearing housings at both ends, the shaft out of them.
    for zb, s in ((z0 + 0.1, -1), (z1 - 0.1, 1)):
        b.box(MX - 0.5, PLINTH + 0.06, zb - 0.22, MX + 0.5, MY - 0.15, zb + 0.22, PUMP)
        rev(b, (MX, MY, zb - 0.22), (MX, MY, zb + 0.22), [(0, 0.24), (0.06, 0.3), (0.38, 0.3), (0.44, 0.24)], PUMP, PUMP, PUMP, 16)
        b.cyl((MX, MY + 0.3, zb), (MX, MY + 0.42, zb), 0.06, GUN, GUN, 8)
        run(b, [(MX + 0.3, MY - 0.05, zb), (OIL_X, MY - 0.05, zb), (OIL_X, OIL_Y[0], zb)], 0.03, OIL, joints=OIL, sides=6)
    # The barrel: a forging, its suction end and its discharge end cover with the big stud nuts.
    bz0, bz1 = z0 + 0.45, z1 - 0.45
    rev(b, (MX, MY, bz0), (MX, MY, bz1), [(0, 0.55), (0.12, 0.72), (bz1 - bz0 - 0.25, 0.72), (bz1 - bz0 - 0.25, 0.8), (bz1 - bz0, 0.8)],
        PUMP, PUMP, PUMP, 26)
    for k in range(16):
        a = 2 * math.pi * (k + 0.5) / 16
        c = (MX + math.cos(a) * 0.68, MY + math.sin(a) * 0.68)
        b.cyl((c[0], c[1], bz1), (c[0], c[1], bz1 + 0.12), 0.055, GUN, GUN, 6)
    for zz in (bz0 + 0.6, bz1 - 0.7):
        ring(b, MX, MY, zz, 0.735, 0.05, sw("dgrey"), 26)
    # Feet on the plinth.
    for zz in (bz0 + 0.3, bz1 - 0.4):
        for s in (-1, 1):
            b.box(MX + s * 0.55 - 0.15, PLINTH + 0.06, zz - 0.15, MX + s * 0.55 + 0.15, MY - 0.35, zz + 0.15, PUMP)
    # Suction up out of the floor into its underside through the strainer spool.
    sz = bz0 + 0.55
    b.cyl((MX, 0.0, sz), (MX, MY - 0.6, sz), 0.33, PUMP, None, 18)
    for y in (0.0, PLINTH + 0.25, MY - 0.65):
        b.cyl((MX, y, sz), (MX, y + 0.06, sz), 0.42, "flange", MID, 18)
    b.cyl((MX - 0.33, PLINTH + 0.45, sz), (MX - 0.5, PLINTH + 0.45, sz), 0.05, GUN, GUN, 6)
    # Discharge: up out of its top through the check valve, over to the left through the motor-operated gate valve
    # (its actuator on top, the handwheel on its side), down into the floor; the recirculation line off it.
    dz = bz1 - 0.55
    dtop = 3.0
    run(b, [(MX, MY + 0.6, dz), (MX, dtop, dz), (0.42, dtop, dz), (0.42, -0.1, dz)], 0.25, PUMP, joints=PUMP, sides=16)
    fl(b, (MX, MY + 0.7, dz), "y", 0.25, 0.06)
    b.cyl((MX, 1.95, dz), (MX, 2.45, dz), 0.33, GUN, GUN, 16)
    for y in (1.9, 2.45):
        b.cyl((MX, y, dz), (MX, y + 0.06, dz), 0.36, "flange", MID, 16)
    b.cyl((MX, 2.2, dz - 0.33), (MX, 2.2, dz - 0.42), 0.08, GUN, GUN, 8)
    vx = 0.95
    b.cyl((vx - 0.2, dtop, dz), (vx + 0.2, dtop, dz), 0.33, GUN, GUN, 16)
    b.cyl((vx, dtop + 0.25, dz), (vx, dtop + 0.55, dz), 0.16, GUN, GUN, 12)
    b.box(vx - 0.28, dtop + 0.55, dz - 0.32, vx + 0.28, dtop + 1.05, dz + 0.22, sw("blue"))
    b.box(vx - 0.2, dtop + 1.05, dz - 0.26, vx + 0.2, dtop + 1.25, dz + 0.16, sw("blue"))
    b.box(vx - 0.18, dtop + 0.62, dz + 0.22, vx + 0.18, dtop + 0.98, dz + 0.5, MOTOR)
    b.cyl((vx + 0.28, dtop + 0.8, dz - 0.1), (vx + 0.4, dtop + 0.8, dz - 0.1), 0.02, "stainless", None, 4)
    handwheel(b, (vx + 0.4, dtop + 0.8, dz - 0.1), "x", 0.18)
    labelled(b, MFP_TAGS, lambda tg: front_plate(b, dz - 0.32, vx - 0.2, vx + 0.2, dtop + 0.78, dtop + 0.92, tag_tex(f"MFP-{tg}|DISCH MOV", 0.4, 0.14), d=0.004))
    b.cyl((0.42, 0.0, dz), (0.42, 0.05, dz), 0.36, MID, MID, 16)
    run(b, [(0.42, 1.6, dz), (0.42, 1.6, dz - 0.8), (0.42, -0.1, dz - 0.8)], 0.08, STEEL, joints=DARK, sides=8)
    rev(b, (0.42, 1.0, dz - 0.8), (0.42, 1.3, dz - 0.8), [(0, 0.12), (0.3, 0.12)], GUN, GUN, GUN, 10)
    rev(b, (0.42, 1.3, dz - 0.8), (0.42, 1.55, dz - 0.8), [(0, 0.04), (0.08, 0.14), (0.2, 0.14), (0.25, 0.04)], sw("blue"), None, None, 12)
    # Seal water to the seals and the seal coolers on the barrel's sides.
    for zz in (bz0 - 0.05, bz1 + 0.25):
        b.cyl((MX + 0.6, MY + 0.55, zz - 0.3), (MX + 0.6, MY + 0.55, zz + 0.3), 0.09, STEEL, STEEL, 10)
        run(b, [(MX + 0.6, MY + 0.55, zz), (MX + 0.3, MY + 0.25, zz)], 0.02, "stainless", sides=4)
    # Its nameplate on the drive end's pedestal; its number stencilled on the plinth.
    labelled(b, MFP_TAGS, lambda tg: side_plate(b, -1, MX - 0.5, z1 - 0.3, z1 + 0.1, 0.6, 0.85, tag_tex(f"BOILER FEED PUMP|MFP-{tg}  5200 RPM", 0.4, 0.25), d=0.004))
    labelled(b, MFP_TAGS, lambda tg: side_plate(b, -1, 0.25, 1.0, 3.4, 0.06, 0.3, stencil_tex(f"MFP-{tg}", 2.4, 0.24, (236, 196, 36)), edge=None, d=0.003))
    # The balance leak-off from the discharge end back over the barrel and down to the suction, its orifice.
    run(b, [(MX + 0.3, MY + 0.62, bz1 + 0.06), (MX + 0.3, MY + 0.98, bz1 + 0.06), (MX + 0.85, MY + 0.98, bz1 + 0.06), (MX + 0.85, PLINTH, bz1 + 0.06)],
        0.055, STEEL, joints=DARK, sides=8)
    for y in (1.0, 1.06):
        b.cyl((MX + 0.85, y, bz1 + 0.06), (MX + 0.85, y + 0.04, bz1 + 0.06), 0.1, "flange", MID, 10)
    # The barrel's vents with their little valves.
    for zz in (bz0 + 0.35, bz1 - 0.95):
        b.cyl((MX - 0.25, MY + 0.66, zz), (MX - 0.25, MY + 0.95, zz), 0.03, STEEL, STEEL, 6)
        b.cyl((MX - 0.25, MY + 0.86, zz), (MX - 0.25, MY + 0.92, zz), 0.055, GUN, GUN, 6)
        handwheel(b, (MX - 0.25, MY + 1.0, zz), "y", 0.07)
    # The bearings' RTD heads and their conduit down to the plinth; the seal water rotameter on its bracket.
    for zb in (z0 + 0.1, z1 - 0.1):
        b.box(MX - 0.1, MY + 0.42, zb - 0.07, MX + 0.06, MY + 0.52, zb + 0.07, GUN)
        run(b, [(MX - 0.1, MY + 0.47, zb), (MX - 0.42, MY + 0.47, zb), (MX - 0.42, PLINTH + 0.06, zb)], 0.018, BLACK, sides=4)
    rx0 = 0.72
    b.box(rx0 - 0.07, 0.75, 1.62, rx0 + 0.07, 1.3, 1.66, "stainless")
    b.box(rx0 - 0.03, 0.8, 1.6, rx0 + 0.03, 1.25, 1.62, sw("white"))
    b.box(rx0 - 0.015, 0.95, 1.595, rx0 + 0.015, 0.98, 1.6, sw("red"))
    run(b, [(rx0, 1.3, 1.64), (rx0, 1.4, 1.64), (MX - 0.3, 1.4, z0 + 0.45)], 0.012, "stainless", sides=4)
    run(b, [(rx0, 0.75, 1.64), (rx0, 0.0, 1.64)], 0.012, "stainless", sides=4)
    # ---------------------------------------------------------------- the coupling
    c0, c1 = COUP_Z
    b.cyl((MX, MY, z1), (MX, MY, c0 + 0.1), 0.09, "stainless_dark", None, 10)
    b.cyl((MX, MY, c1 - 0.1), (MX, MY, c1 + 0.1), 0.09, "stainless_dark", None, 10)
    live["couplings"].append({"c": _r(MX, MY, c0 + 0.15), "axis": [0, 0, 1], "len": c1 - c0 - 0.3, "r": 0.24})
    for x0, x1 in ((MX - 0.42, MX - 0.38), (MX + 0.38, MX + 0.42)):
        b.box(x0, PLINTH + 0.06, c0 + 0.15, x1, MY + 0.42, c1 - 0.08, [YELLOW] * 6)
    b.box(MX - 0.42, MY + 0.42, c0 + 0.15, MX + 0.42, MY + 0.46, c1 - 0.08, [YELLOW] * 6)
    for zz in (c0 + 0.17, c1 - 0.1):
        b.box(MX - 0.38, MY + 0.12, zz - 0.02, MX + 0.38, MY + 0.42, zz + 0.02, [YELLOW] * 6)
    side_plate(b, -1, MX - 0.42, c0 + 0.2, c1 - 0.2, MY - 0.2, MY + 0.2, "grating", d=0.005)
    side_plate(b, 1, MX + 0.42, c0 + 0.2, c1 - 0.2, MY - 0.2, MY + 0.2, "grating", d=0.005)
    # ---------------------------------------------------------------- the driving turbine
    e0, e1 = EXH_Z
    h0, h1 = HP_Z
    # The lower casing on its pedestal; the lagged upper casing, larger over the exhaust end.
    b.box(MX - 1.05, PLINTH + 0.3, e0 + 0.15, MX + 1.05, MY, h1, "lp_hood")
    b.box(MX - 0.7, PLINTH + 0.06, h1 - 0.6, MX + 0.7, PLINTH + 0.3, h1 - 0.05, "lp_hood")
    half_cyl(b, MX, MY, e0 + 0.15, e1, 1.12, "turbine_lagging", "turbine_lagging", 22)
    half_cyl(b, MX, MY, e1, h1, 0.95, "turbine_lagging", "turbine_lagging", 22)
    # The exhaust end's step down to the steam end, closed over the top.
    for k in range(22):
        a0, a1 = math.pi * k / 22, math.pi * (k + 1) / 22
        p = [(MX + 1.12 * math.cos(a0), MY + 1.12 * math.sin(a0)), (MX + 1.12 * math.cos(a1), MY + 1.12 * math.sin(a1)),
             (MX + 0.95 * math.cos(a1), MY + 0.95 * math.sin(a1)), (MX + 0.95 * math.cos(a0), MY + 0.95 * math.sin(a0))]
        b.quad((p[0][0], p[0][1], e1), (p[1][0], p[1][1], e1), (p[2][0], p[2][1], e1), (p[3][0], p[3][1], e1), "turbine_lagging")
    # The horizontal joint's flanges along both sides, their stud nuts.
    for s in (-1, 1):
        for (za, zb, r) in ((e0 + 0.15, e1, 1.12), (e1 + 0.02, h1 + 0.03, 0.95)):
            xf = MX + s * r
            xa, xb = sorted((xf, xf + s * 0.14))
            b.box(xa, MY - 0.05, za, xb, MY + 0.05, zb, GUN)
            z = za + 0.15
            while z < zb - 0.1:
                b.cyl((xf + s * 0.08, MY + 0.05, z), (xf + s * 0.08, MY + 0.12, z), 0.028, MID, MID, 6)
                z += 0.28
    # Lagging seams.
    for zz in (e0 + 0.9, e0 + 1.6):
        rev(b, (MX, MY + 0.001, zz - 0.02), (MX, MY + 0.001, zz + 0.02), [(0, 1.135), (0.04, 1.135)], sw("mgrey"), None, None, 22)
    for zz in (h0 + 0.8, h0 + 1.6):
        rev(b, (MX, MY + 0.001, zz - 0.02), (MX, MY + 0.001, zz + 0.02), [(0, 0.965), (0.04, 0.965)], sw("mgrey"), None, None, 22)
    # The exhaust down out of the casing's bottom through its rubber expansion joint into the floor.
    b.box(MX - 0.8, -0.05, e0 + 0.4, MX + 0.8, PLINTH + 0.06, e1 - 0.25, "turbine_lagging")
    b.box(MX - 0.95, PLINTH + 0.06, e0 + 0.3, MX + 0.95, PLINTH + 0.3, e1 - 0.15, BLACK)
    for (ya, yb) in ((PLINTH + 0.08, PLINTH + 0.12), (PLINTH + 0.24, PLINTH + 0.28)):
        b.box(MX - 0.99, ya, e0 + 0.26, MX + 0.99, yb, e1 - 0.11, GUN)
    # LP steam up out of the floor on the left through its stop valve into the chest; HP steam on the right.
    for (sx, z, r, tag, side) in ((0.32, h0 + 0.5, 0.26, "LP STOP VLV", -1), (2.88, h0 + 1.5, 0.15, "HP STOP VLV", 1)):
        run(b, [(sx, -0.1, z), (sx, 2.85, z), (MX + side * 0.3, 2.85, z)], r, "insulation", joints="insulation", sides=14)
        for y in (0.6, 1.2, 1.8):
            b.cyl((sx, y, z), (sx, y + 0.03, z), r + 0.02, "stainless", "stainless", 14)
        b.cyl((sx, 0.0, z), (sx, 0.05, z), r + 0.12, MID, MID, 14)
        # The stop valve: its body, the bonnet and the hydraulic actuator with its trip cylinder.
        b.cyl((sx, 2.25, z), (sx, 2.6, z), r + 0.14, GUN, GUN, 14)
        b.cyl((sx + side * (r + 0.14), 2.42, z), (sx + side * (r + 0.4), 2.42, z), 0.11, GUN, GUN, 10)
        b.box(min(sx + side * (r + 0.4), sx + side * (r + 0.75)), 2.25, z - 0.18, max(sx + side * (r + 0.4), sx + side * (r + 0.75)), 2.6, z + 0.18, ACT_MFP)
        b.cyl((sx + side * (r + 0.58), 2.6, z), (sx + side * (r + 0.58), 2.9, z), 0.08, sw("red"), sw("red"), 8)
        labelled(b, MFP_TAGS, lambda tg, sx=sx, side=side, r=r, z=z, tag=tag: side_plate(
            b, side, sx + side * (r + 0.75), z - 0.16, z + 0.16, 2.3, 2.42, tag_tex(f"MFP-{tg}|{tag.replace(' VLV', '')}", 0.32, 0.12), d=0.004))
    # The steam chest on top of the steam end; the governor valves' stems up into the bar lift, its servo.
    ch0, ch1 = h0 + 0.25, h1 - 0.25
    b.box(MX - 0.42, MY + 0.85, ch0, MX + 0.42, MY + 1.35, ch1, "turbine_lagging")
    b.box(MX - 0.45, MY + 1.35, ch0 - 0.03, MX + 0.45, MY + 1.42, ch1 + 0.03, GUN)
    for zz in (ch0 + 0.15, ch1 - 0.15):
        b.box(MX - 0.05, MY + 1.42, zz - 0.05, MX + 0.05, MY + 2.1, zz + 0.05, GUN)
    b.box(MX - 0.08, MY + 2.1, ch0 + 0.05, MX + 0.08, MY + 2.22, ch1 - 0.05, GUN)
    for k in range(4):
        zz = ch0 + 0.45 + k * (ch1 - ch0 - 0.9) / 3
        b.cyl((MX, MY + 1.42, zz), (MX, MY + 2.1, zz), 0.03, "stainless", None, 6)
        b.cyl((MX, MY + 1.42, zz), (MX, MY + 1.52, zz), 0.07, GUN, GUN, 8)
    b.box(MX - 0.2, MY + 2.22, ch1 - 0.45, MX + 0.2, MY + 2.6, ch1 - 0.05, ACT_MFP)
    b.cyl((MX, MY + 2.6, ch1 - 0.25), (MX, MY + 2.9, ch1 - 0.25), 0.12, ACT_MFP, ACT_MFP, 10)
    # The governor pedestal at the far end: its bearing, the control housing, the speed pickup, the trip block and its
    # red hand trip lever, the oil lines.
    g0, g1 = GOV_Z
    b.box(MX - 0.55, PLINTH + 0.06, g0, MX + 0.55, MY + 0.35, g1, "lp_hood")
    half_cyl(b, MX, MY + 0.35, g0, g1, 0.55, "lp_hood", "lp_hood", 16)
    b.box(MX - 0.6, MY + 0.3, g0 - 0.03, MX + 0.6, MY + 0.38, g1 + 0.03, GUN)
    b.box(MX + 0.55, MY - 0.1, g0 + 0.3, MX + 0.85, MY + 0.35, g0 + 0.8, GUN)
    b.box(MX + 0.85, MY + 0.05, g0 + 0.45, MX + 0.88, MY + 0.3, g0 + 0.65, [sw("red")] * 6)
    b.cyl((MX + 0.7, MY + 0.35, g0 + 0.55), (MX + 0.7, MY + 0.75, g0 + 0.85), 0.03, sw("red"), sw("red"), 6)
    b.cyl((MX + 0.7, MY + 0.75, g0 + 0.85), (MX + 0.7, MY + 0.82, g0 + 0.9), 0.06, sw("red"), sw("red"), 8)
    b.cyl((MX - 0.55, MY + 0.6, g1 - 0.4), (MX - 0.7, MY + 0.6, g1 - 0.4), 0.05, "stainless", "stainless", 8)
    b.cyl((MX - 0.7, MY + 0.6, g1 - 0.4), (MX - 0.7, 0.0, g1 - 0.4), 0.02, "stainless", None, 4)
    for zz in (g0 + 0.3, g1 - 0.3):
        run(b, [(MX + 0.55, MY - 0.2, zz), (OIL_X, MY - 0.2, zz), (OIL_X, OIL_Y[0], zz)], 0.03, OIL, joints=OIL, sides=6)
    # The turning gear on the governor end: its gearbox and the motor on it.
    b.box(MX - 0.22, MY + 0.05, g1, MX + 0.22, MY + 0.45, g1 + 0.26, GUN)
    motor_h(b, (MX - 0.35, MY + 0.62, g1 + 0.13), "x", 0.5, 0.13)
    b.box(MX - 0.33, MY + 0.45, g1 + 0.05, MX + 0.18, MY + 0.49, g1 + 0.21, GUN)
    labelled(b, MFP_TAGS, lambda tg: side_plate(b, -1, MX - 1.05, e0 + 0.5, e0 + 2.3, 0.72, 0.98, stencil_tex(f"MFP-{tg} TURBINE", 1.8, 0.26), edge=None, d=0.003))
    # The casing drains down off the lower casing through their valves into the plinth.
    for zz in (e0 + 0.7, h0 + 0.2, h1 - 0.4):
        run(b, [(MX - 1.05, PLINTH + 0.45, zz), (0.42, PLINTH + 0.45, zz), (0.42, PLINTH - 0.02, zz)], 0.035, STEEL, joints=DARK, sides=6)
        b.cyl((0.42, PLINTH + 0.15, zz), (0.42, PLINTH + 0.27, zz), 0.06, GUN, GUN, 6)
        b.cyl((0.42, PLINTH + 0.21, zz), (0.3, PLINTH + 0.21, zz), 0.012, "stainless", None, 4)
        handwheel(b, (0.3, PLINTH + 0.21, zz), "x", 0.06)
    # The casing's thermocouples: their heads on the joint flange and their conduit down to the junction box on the
    # plinth.
    jz = e1 - 0.6
    b.box(3.35, PLINTH, jz - 0.25, 3.75, PLINTH + 0.45, jz + 0.25, PANEL)
    b.box(3.75, PLINTH + 0.05, jz - 0.2, 3.77, PLINTH + 0.4, jz + 0.2, sw("dgrey"))
    for k, zz in enumerate((e0 + 0.6, e1 - 0.2, h0 + 0.9)):
        xh = MX + 1.12 + 0.17 if zz < e1 else MX + 0.95 + 0.17
        b.cyl((xh - 0.03, MY + 0.05, zz), (xh + 0.06, MY + 0.05, zz), 0.03, "stainless", "stainless", 6)
        cx = 3.1 + k * 0.06
        run(b, [(xh + 0.06, MY + 0.05, zz), (cx, MY + 0.05, zz), (cx, PLINTH + 0.1 + k * 0.08, zz), (cx, PLINTH + 0.1 + k * 0.08, jz),
                (3.35, PLINTH + 0.1 + k * 0.08, jz)], 0.016, BLACK, sides=4)
    # ---------------------------------------------------------------- the lube oil console beside the pump
    ox0, ox1, oz0, oz1 = 2.75, 3.9, 0.75, 4.2
    b.box(ox0, 0.0, oz0, ox1, 1.0, oz1, PANEL)
    b.box(ox0 - 0.03, 1.0, oz0 - 0.03, ox1 + 0.03, 1.05, oz1 + 0.03, GUN)
    for k, (zz, paint) in enumerate(((oz0 + 0.5, MOTOR), (oz0 + 1.35, MOTOR), (oz0 + 2.2, sw("orange")))):
        b.cyl((3.3, 1.05, zz), (3.3, 1.25, zz), 0.14, PUMP, PUMP, 10)
        vmotor(b, 3.3, zz, 1.25, 1.95, 0.24, paint)
    # The duplex filter at the front, its transfer handle; the cooler along the back on its saddles, its water down
    # into the floor; the breather.
    for zz in (oz0 + 0.25, oz0 + 0.55):
        rev(b, (3.65, 1.05, zz), (3.65, 1.55, zz), [(0, 0.12), (0.42, 0.12), (0.5, 0.07)], PANEL, None, GUN, 10)
    b.box(3.6, 1.55, oz0 + 0.3, 3.7, 1.6, oz0 + 0.5, GUN)
    b.box(3.63, 1.6, oz0 + 0.38, 3.67, 1.85, oz0 + 0.42, sw("red"))
    b.cyl((3.65, 1.3, oz0 + 1.2), (3.65, 1.3, oz1 - 0.15), 0.17, PANEL, GUN, 14)
    for zz in (oz0 + 1.4, oz1 - 0.35):
        b.box(3.5, 1.05, zz - 0.05, 3.8, 1.18, zz + 0.05, GUN)
    for zz in (oz0 + 1.6, oz1 - 0.6):
        run(b, [(3.65, 1.47, zz), (3.65, 1.6, zz), (3.86, 1.6, zz), (3.86, -0.05, zz)], 0.04, "pipe_green", joints=DARK, sides=6)
    b.cyl((2.95, 1.05, oz1 - 0.2), (2.95, 1.25, oz1 - 0.2), 0.05, STEEL, STEEL, 6)
    b.cyl((2.95, 1.25, oz1 - 0.2), (2.95, 1.35, oz1 - 0.2), 0.09, GUN, GUN, 8)
    labelled(b, MFP_TAGS, lambda tg: side_plate(b, 1, ox1, oz0 + 0.3, oz1 - 0.3, 0.45, 0.68, stencil_tex(f"MFP-{tg} L.O. CONSOLE", 2.8, 0.22, (34, 36, 38)), edge=None, d=0.003))
    # Its level glass, pressure and temperature gauges on the front, their tags.
    b.box(3.62, 0.25, oz0 - 0.02, 3.7, 0.85, oz0, [sw("white"), None, "stainless", "stainless", "stainless", "stainless"])
    for k, (name, unit, lo, hi, majors) in enumerate((("MFP LO PRESS", "PSIG", 0, 100, 5), ("MFP LO TEMP", "°F", 0, 200, 5))):
        gx = 2.98 + k * 0.38
        gauge_z(b, live, name, (gx, 0.72, oz0), 0.11, gauge(name, unit, lo, hi, majors, 0.11))
        front_plate(b, oz0, gx - 0.16, gx + 0.16, 0.5, 0.57, tag_tex(("OIL PRESS", "OIL TEMP")[k], 0.32, 0.07), d=0.003)
    # The oil supply and drain headers along the set's right side to the turbine's bearings.
    run(b, [(OIL_X, OIL_Y[0], oz1), (OIL_X, OIL_Y[0], g1 - 0.3)], 0.035, OIL, sides=6)
    run(b, [(OIL_X + 0.1, OIL_Y[1], oz1), (OIL_X + 0.1, OIL_Y[1], g1 - 0.15), (OIL_X + 0.1, PLINTH - 0.05, g1 - 0.15)], 0.05, OIL, joints=OIL, sides=6)
    # ---------------------------------------------------------------- the local gauge board at the front
    # A stainless gauge board on a unistrut stand: six gauges in two rows, each on its isolation valve with its tubing
    # down behind the board to the floor, each with its engraved tag; the speed indicator in its box on top; the
    # board's tag.
    zf = 0.14
    for x in (0.3, 2.3):
        b.box(x - 0.03, 0.0, zf + 0.04, x + 0.03, 2.2, zf + 0.1, GUN)
        b.box(x - 0.09, 0.0, zf - 0.02, x + 0.09, 0.02, zf + 0.16, GUN)
    for y in (0.92, 2.14):
        b.box(0.33, y, zf + 0.04, 2.27, y + 0.06, zf + 0.1, GUN)
    b.box(0.36, 0.98, zf, 2.24, 2.12, zf + 0.02, ["stainless", None, "stainless", "stainless", "stainless", "stainless"])
    gauges = ((("MFP SUCTION", "SUCTION", "PSIG", 0, 600, 7), ("MFP DISCHARGE", "DISCHARGE", "PSIG", 0, 1600, 5), ("MFP BAL LEAKOFF", "BAL LEAKOFF", "PSIG", 0, 600, 7)),
              (("MFP BRG OIL", "BRG OIL", "PSIG", 0, 60, 7), ("MFP SEAL WTR", "SEAL WATER", "PSIG", 0, 600, 7), ("MFP EXH PRESS", "EXHAUST", "IN HG", 0, 30, 7)))
    for row, y in ((0, 1.8), (1, 1.32)):
        for k, (name, legend, unit, lo, hi, majors) in enumerate(gauges[row]):
            gx = 0.68 + k * 0.62
            gauge_z(b, live, name, (gx, y, zf), 0.13, gauge(legend, unit, lo, hi, majors, 0.13))
            front_plate(b, zf, gx - 0.22, gx + 0.22, y - 0.25, y - 0.18, tag_tex(legend, 0.44, 0.07), d=0.003)
            # Its tubing: through the board to its isolation valve behind, down to the floor.
            tx = gx + 0.12 * (k - 1)
            run(b, [(gx, y, zf + 0.02), (gx, y, zf + 0.16), (tx, y - 0.15, zf + 0.16), (tx, 0.0, zf + 0.16)], 0.011, "stainless", sides=4)
            b.cyl((gx, y, zf + 0.06), (gx, y, zf + 0.12), 0.03, GUN, GUN, 6)
    b.box(0.45, 2.2, zf - 0.02, 1.2, 2.5, zf + 0.12, [PANEL] * 6)
    b.box(0.5, 2.27, zf - 0.05, 1.15, 2.45, zf - 0.02, BLACK)
    live["digits"].append({"name": "mfp_speed", "c": _r(0.825, 2.36, zf - 0.052), "w": 0.62, "h": 0.15})
    front_plate(b, zf - 0.02, 0.6, 1.05, 2.2, 2.26, tag_tex("SPEED  RPM", 0.45, 0.06), d=0.003)
    labelled(b, MFP_TAGS, lambda tg: front_plate(b, zf + 0.04, 1.3, 2.2, 2.21, 2.36, tag_tex(f"MAIN FEED PUMP {tg}|LOCAL GAUGE BOARD", 0.9, 0.15), d=0.004))
    # The turbine's local control station on its own post at the front right: a hinged steel box (its hinges and
    # latch), TRIP under its shroud, RESET, the RUNNING and TRIPPED lamps, their tags, its conduit.
    cx0, cx1, czf = 2.92, 3.62, 0.06
    b.box(3.24, 0.0, czf + 0.12, 3.3, 1.1, czf + 0.18, GUN)
    b.box(3.18, 0.0, czf + 0.06, 3.36, 0.02, czf + 0.24, GUN)
    b.box(cx0, 1.08, czf + 0.01, cx1, 1.82, czf + 0.2, [PANEL] * 6)
    b.box(cx0 + 0.02, 1.1, czf, cx1 - 0.02, 1.8, czf + 0.01, [PANEL, None, PANEL, PANEL, PANEL, PANEL])
    for y in (1.18, 1.7):
        b.cyl((cx0 - 0.01, y - 0.05, czf + 0.02), (cx0 - 0.01, y + 0.05, czf + 0.02), 0.018, GUN, GUN, 6)
    b.box(cx1 - 0.06, 1.38, czf - 0.03, cx1 - 0.03, 1.5, czf, sw("dgrey"))
    tx, ty = MFP_TRIP
    rx, ry = MFP_RESET
    b.box(tx - 0.11, ty - 0.11, czf - 0.07, tx + 0.11, ty + 0.11, czf, [None, None, YELLOW, YELLOW, YELLOW, None])
    button(b, tx, ty, czf, "red", 0.065)
    button(b, rx, ry, czf, "black", 0.045)
    for (x, word) in ((tx, "TRIP"), (rx, "RESET")):
        front_plate(b, czf, x - 0.12, x + 0.12, ty - 0.2, ty - 0.14, tag_tex(word, 0.24, 0.06), d=0.003)
    for k, (name, legend, colour) in enumerate((("mfp_run", "RUNNING", "red"), ("mfp_trip", "TRIPPED", "amber"))):
        lx = tx + k * (rx - tx)
        lamp_z(b, live, name, lx, 1.62, czf, colour, 0.035)
        front_plate(b, czf, lx - 0.13, lx + 0.13, 1.52, 1.57, tag_tex(legend, 0.26, 0.05), d=0.003)
    labelled(b, MFP_TAGS, lambda tg: front_plate(b, czf, cx0 + 0.05, cx1 - 0.05, 1.73, 1.79, tag_tex(f"MFP-{tg} TURBINE", 0.6, 0.06), d=0.003))
    run(b, [(3.27, 1.08, czf + 0.1), (3.27, 0.95, czf + 0.1), (3.4, 0.6, czf + 0.1), (3.4, 0.0, czf + 0.1)], 0.025, BLACK, sides=6)
    live["mats"] = {"needle": sw("red"), "hub": "stainless_dark", "spacer": "gunmetal", "bolt": "stainless", "mark": sw("white")}
    b.live = live
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ the oil plant

def lamp_side(b, live, name, x, y, z, colour, side, legend, w=0.36):
    """A lamp on a side (side -1 facing -x) with its legend under it."""
    T.lamp_x(b, live, name, x, y, z, colour, side, 0.04)
    side_plate(b, side, x, z - w / 2, z + w / 2, y - 0.13, y - 0.065, plate(legend, w, 0.065, "white"), d=0.005)


# ------------------------------------------------------------------------------------------------ lube oil reservoir
#
# The main turbine's lube oil reservoir (4 wide, 8 long, 4 tall; its left side, the operating side, toward the room's
# door): the tank on its two skid beams, its stiffeners and lifting lugs, the cover plate on top. On the cover: the
# return oil screen box (the bearings' drain header up into it), the AC auxiliary, turning gear and DC emergency oil
# pumps (vertical, their motor stands with the coupling behind guards, finned motors, nameplates, terminal boxes and
# conduit into the cable tray), their discharges through check and gate valves into the header; the two lift oil pumps
# on their bracket, their high-pressure tubing down the back; the two vapour extractor blowers into the demister and
# its vent; the manway, the fill connection. On the right: the twin coolers on their stands (cooling water up out of
# the floor through their valves) with the transfer valve between them, the duplex filter on its stand over the drip
# pan with its transfer valve and differential gauge, the supply down into the floor to the bearings. On the left:
# the level glass and the level switches, the thermometer, the local gauge board, the pumps' local control panel, the
# tank's stencil. On the back the immersion heaters; at the front the drain and sample valves and the nameplate.

LOR_W, LOR_D, LOR_T = 4, 8, 4
TX0, TX1, TY0, TY1, TZ0, TZ1 = 0.3, 2.7, 0.3, 2.2, 0.45, 7.55
DECK = TY1 + 0.06
LO_PX = 1.05                     # the oil pumps' line (x), their (name, tag, z, motor radius, frame top, paint)
LO_PUMPS = (("aux_oil", "AUX OIL PUMP", 2.0, 0.27, 3.45, MOTOR, "460 V  75 HP|1780 RPM"),
            ("tg_oil", "TURN GEAR OIL PUMP", 3.15, 0.22, 3.3, MOTOR, "460 V  40 HP|1780 RPM"),
            ("emerg_oil", "EMERG OIL PUMP", 4.3, 0.27, 3.45, sw("orange"), "250 VDC  50 HP|1750 RPM"))
HDR_X, HDR_Y = 2.45, 2.62        # the pumps' discharge header along z


def lube_oil_reservoir(seed=431):
    with own_atlas():
        return _lube_oil_reservoir(seed)


def _oil_pump(b, live, cz, r, m1, paint, tag, rating):
    """A vertical oil pump on the reservoir's cover: its mounting plate, the motor stand (two solid sides, the windows
    toward the sides behind guards, the shaft and coupling inside), the finned motor with its fan cowl, the nameplate
    pad on its left, the terminal box at its front and the conduit along the cover into the tray; its discharge up
    through the cover, through the check and gate valves into the header."""
    cx = LO_PX
    b.box(cx - 0.36, DECK, cz - 0.36, cx + 0.36, DECK + 0.06, cz + 0.36, GUN)
    for sx in (-1, 1):
        for sz in (-1, 1):
            b.cyl((cx + sx * 0.3, DECK + 0.06, cz + sz * 0.3), (cx + sx * 0.3, DECK + 0.1, cz + sz * 0.3), 0.025, MID, MID, 6)
    s0, s1 = DECK + 0.06, DECK + 0.56
    b.cyl((cx, s0, cz), (cx, s0 + 0.06, cz), r + 0.05, PUMP, PUMP, 18)
    b.cyl((cx, s1 - 0.06, cz), (cx, s1, cz), r + 0.05, PUMP, PUMP, 18)
    for a0, a1 in ((40, 140), (220, 320)):
        _arc(b, cx, cz, r - 0.06, r, s0 + 0.06, s1 - 0.06, a0, a1, PUMP)
    for s in (-1, 1):
        xg = cx + s * r * 0.8
        q = [(xg, s1 - 0.08, cz + r * 0.6), (xg, s0 + 0.08, cz + r * 0.6), (xg, s0 + 0.08, cz - r * 0.6), (xg, s1 - 0.08, cz - r * 0.6)]
        b.quad(*(q if s < 0 else q[::-1]), "grating")
        for y in (s0 + 0.06, s1 - 0.1):
            b.box(xg - 0.012, y, cz - r * 0.62, xg + 0.012, y + 0.04, cz + r * 0.62, YELLOW)
    b.cyl((cx, s0 + 0.06, cz), (cx, s1 - 0.06, cz), 0.04, "stainless_dark", None, 8)
    for y in (s0 + 0.2, s0 + 0.3):
        b.cyl((cx, y, cz), (cx, y + 0.08, cz), 0.1, GUN, GUN, 12)
    # The motor.
    m0 = s1
    b.cyl((cx, m0, cz), (cx, m0 + 0.06, cz), r + 0.06, paint, paint, 20)
    b.cyl((cx, m0 + 0.06, cz), (cx, m1, cz), r, paint, None, 20)
    for k in range(12):
        a = 2 * math.pi * (k + 0.5) / 12
        if abs(a - math.pi) < 0.5:
            continue
        u = (math.cos(a), 0.0, math.sin(a))
        c = (cx + math.cos(a) * (r + 0.03), (m0 + m1) / 2 + 0.03, cz + math.sin(a) * (r + 0.03))
        b.obox(c, u, (0, 1, 0), 0.035, (m1 - m0 - 0.2) / 2, 0.01, paint)
    rev(b, (cx, m1, cz), (cx, m1 + 0.12, cz), [(0, r + 0.02), (0.05, r + 0.02), (0.12, r - 0.07)], paint, None, None, 20)
    b.cyl((cx, m1 + 0.12, cz), (cx, m1 + 0.3, cz), r - 0.05, sw("dgrey"), sw("dgrey"), 20)
    rev(b, (cx, m1 + 0.2, cz), (cx, m1 + 0.23, cz), [(0, r - 0.035), (0.03, r - 0.035)], "grating", None, None, 20)
    # The nameplate pad on its left, its tag and nameplate.
    xp = cx - r - 0.03
    b.box(xp, m0 + 0.1, cz - 0.14, cx - r + 0.05, m1 - 0.04, cz + 0.14, [paint, paint, paint, None, paint, paint])
    side_plate(b, -1, xp, cz - 0.13, cz + 0.13, m1 - 0.16, m1 - 0.07, tag_tex(tag, 0.26, 0.09), d=0.004)
    side_plate(b, -1, xp, cz - 0.11, cz + 0.11, m0 + 0.14, m0 + 0.3, tag_tex(rating, 0.22, 0.16), d=0.004)
    # The terminal box at its front, the conduit down and along the cover into the tray.
    zt = cz - r
    b.box(cx - 0.11, m0 + 0.12, zt - 0.17, cx + 0.11, m0 + 0.36, zt + 0.02, paint)
    b.box(cx - 0.09, m0 + 0.14, zt - 0.18, cx + 0.09, m0 + 0.34, zt - 0.17, sw("dgrey"))
    run(b, [(cx, m0 + 0.12, zt - 0.08), (cx, DECK + 0.15, zt - 0.08), (0.46, DECK + 0.15, zt - 0.08), (0.46, DECK + 0.05, zt - 0.08)],
        0.022, BLACK, sides=6)
    # The discharge up through the cover, the check valve, the gate valve, into the header.
    dx = cx + 0.5
    b.cyl((dx, DECK, cz), (dx, DECK + 0.05, cz), 0.12, "flange", MID, 12)
    run(b, [(dx, DECK + 0.05, cz), (dx, HDR_Y, cz), (HDR_X, HDR_Y, cz)], 0.055, OIL, joints=OIL, sides=10)
    b.cyl((1.7, HDR_Y, cz), (1.9, HDR_Y, cz), 0.09, GUN, GUN, 10)
    rev(b, (1.8, HDR_Y + 0.06, cz), (1.8, HDR_Y + 0.14, cz), [(0, 0.07), (0.08, 0.04)], GUN, None, GUN, 8)
    for x in (1.67, 1.9):
        b.cyl((x, HDR_Y, cz), (x + 0.03, HDR_Y, cz), 0.1, "flange", MID, 10)
    gate_valve(b, (2.12, HDR_Y, cz), "x", 0.06)


def _lube_oil_reservoir(seed=431):
    b = C.Board(LOR_W, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "levels": []}
    # The skid beams under the tank, their anchor nuts.
    for x in (0.45, 2.25):
        for (ya, yb) in ((0.0, 0.04), (0.26, 0.3)):
            b.box(x, ya, 0.3, x + 0.3, yb, 7.7, STEEL)
        b.box(x + 0.13, 0.04, 0.3, x + 0.17, 0.26, 7.7, [STEEL, STEEL, STEEL, STEEL, None, None])
        for z in (0.4, 7.6):
            b.cyl((x + 0.06, 0.04, z), (x + 0.06, 0.1, z), 0.025, MID, MID, 6)
            b.cyl((x + 0.24, 0.04, z), (x + 0.24, 0.1, z), 0.025, MID, MID, 6)
    # The tank, its cover plate, its stiffeners and base angles, its lifting lugs.
    b.box(TX0, TY0, TZ0, TX1, TY1, TZ1, [SHELL, SHELL, SHELL, SHELL, None, None])
    b.box(TX0 - 0.05, TY1, TZ0 - 0.05, TX1 + 0.05, DECK, TZ1 + 0.05, GUN)
    b.core = len(b.still)
    for z in (1.3, 2.45, 4.38, 6.0, 7.0):
        if z < 7:                    # (the left side's last panel carries the stencil)
            b.box(TX0 - 0.06, TY0 + 0.08, z - 0.03, TX0, TY1, z + 0.03, SHELL)
        b.box(TX1, TY0 + 0.08, z - 0.03, TX1 + 0.06, TY1, z + 0.03, SHELL)
    for x in (0.6, 1.45, 2.35):
        b.box(x - 0.03, TY0, TZ0 - 0.06, x + 0.03, TY1, TZ0, SHELL)
    for x in (1.5, 2.1):
        b.box(x - 0.03, TY0, TZ1, x + 0.03, TY1, TZ1 + 0.06, SHELL)
    b.box(TX0 - 0.04, TY0, TZ0, TX0, TY0 + 0.07, TZ1, SHELL)
    b.box(TX1, TY0, TZ0, TX1 + 0.04, TY0 + 0.07, TZ1, SHELL)
    for x in (0.42, 2.58):
        for z in (0.55, 7.45):
            b.box(x - 0.02, DECK, z - 0.1, x + 0.02, DECK + 0.22, z + 0.1, GUN)
            rev(b, (x - 0.03, DECK + 0.14, z), (x + 0.03, DECK + 0.14, z), [(0, 0.05), (0.06, 0.05)], GUN, GUN, GUN, 8)
    # ---------------------------------------------------------------- on the cover
    # The return oil screen box, its bolted cover and handles; the bearings' drain header up into it from the floor.
    b.box(0.62, DECK, 0.6, 1.6, DECK + 0.3, 1.5, SHELL)
    b.box(0.6, DECK + 0.3, 0.58, 1.62, DECK + 0.34, 1.52, GUN)
    for k in range(6):
        for z in (0.63, 1.47):
            b.cyl((0.68 + k * 0.17, DECK + 0.34, z), (0.68 + k * 0.17, DECK + 0.37, z), 0.018, MID, MID, 6)
    for z in (0.85, 1.25):
        run(b, [(0.95, DECK + 0.34, z), (0.95, DECK + 0.44, z), (1.27, DECK + 0.44, z), (1.27, DECK + 0.34, z)], 0.015, STEEL, sides=4)
    side_plate(b, -1, 0.62, 0.72, 1.38, DECK + 0.1, DECK + 0.2, tag_tex("RETURN OIL SCREEN", 0.66, 0.1), d=0.004)
    run(b, [(1.1, -0.1, 0.2), (1.1, DECK + 0.15, 0.2), (1.1, DECK + 0.15, 0.66)], 0.18, OIL, joints=OIL, sides=14)
    fl(b, (1.1, DECK + 0.15, 0.56), "z", 0.18)
    fl(b, (1.1, 1.2, 0.2), "y", 0.18)
    b.cyl((1.1, 0.0, 0.2), (1.1, 0.04, 0.2), 0.3, MID, MID, 14)
    # The oil pumps, their header.
    for (name, tag, cz, r, m1, paint, rating) in LO_PUMPS:
        _oil_pump(b, live, cz, r, m1, paint, tag, rating)
    run(b, [(HDR_X, HDR_Y, 1.85), (HDR_X, HDR_Y, 4.45)], 0.085, OIL, sides=12)
    for z in (1.85, 4.45):
        b.cyl((HDR_X, HDR_Y, z - 0.02), (HDR_X, HDR_Y, z + 0.02), 0.11, "flange", MID, 12)
    for z in (2.7, 3.9):
        b.box(HDR_X - 0.05, DECK, z - 0.04, HDR_X + 0.05, HDR_Y - 0.085, z + 0.04, GUN)
    # The cable tray along the cover's left edge and down the back.
    for (xa, xb) in ((0.34, 0.37), (0.55, 0.58)):
        b.box(xa, DECK, 1.45, xb, DECK + 0.1, 7.72, GUN)
        b.box(xa, 0.0, 7.62, xb, DECK - 0.01, 7.72, GUN)
    z = 1.6
    while z < 7.5:
        b.box(0.37, DECK + 0.01, z - 0.02, 0.55, DECK + 0.03, z + 0.02, GUN)
        z += 0.3
    for y in (0.3, 0.8, 1.3, 1.8):
        b.box(0.37, y, 7.63, 0.55, y + 0.04, 7.66, GUN)
    # The lift oil pumps on their bracket, their tubing over and down the back.
    for k, z in enumerate((5.25, 5.85)):
        b.box(0.72, DECK, z - 0.2, 1.75, DECK + 0.1, z + 0.2, GUN)
        b.cyl((0.75, DECK + 0.26, z), (1.0, DECK + 0.26, z), 0.1, GUN, GUN, 12)
        b.cyl((0.87, DECK + 0.1, z), (0.87, DECK + 0.17, z), 0.045, GUN, GUN, 8)
        motor_h(b, (1.0, DECK + 0.26, z), "x", 0.5, 0.13)
        side_plate(b, -1, 0.72, z - 0.17, z + 0.17, DECK + 0.02, DECK + 0.08, tag_tex(f"LIFT OIL PP {'AB'[k]}", 0.34, 0.06), d=0.003)
        # Its high-pressure tubing over the cover and down the right side, clipped to the tank.
        yt = DECK + 0.5
        run(b, [(0.87, DECK + 0.36, z), (0.87, yt, z), (TX1 + 0.08, yt, z), (TX1 + 0.08, 0.0, z)], 0.022, "stainless", joints="stainless", sides=6)
        for y in (0.6, 1.4):
            b.box(TX1 + 0.0, y, z - 0.04, TX1 + 0.11, y + 0.03, z + 0.04, GUN)
        b.cyl((TX1 + 0.08, 0.0, z), (TX1 + 0.08, 0.03, z), 0.05, MID, MID, 8)
    # The vapour extractor blowers on their pedestals, into the demister; the demister, its drain, its vent.
    for xv in (0.8, 1.9):
        b.box(xv - 0.14, DECK, 7.0, xv + 0.62, DECK + 0.17, 7.3, GUN)
        b.cyl((xv - 0.07, DECK + 0.34, 7.15), (xv + 0.07, DECK + 0.34, 7.15), 0.17, GUN, GUN, 14)
        motor_h(b, (xv + 0.07, DECK + 0.34, 7.15), "x", 0.36, 0.11)
        run(b, [(xv, DECK + 0.5, 7.15), (xv, 2.95, 7.15), (xv, 2.95, 6.5), (1.5 + (0.22 if xv > 1.5 else -0.22), 2.95, 6.5)], 0.05, GUN, sides=8)
        b.cyl((xv, 2.72, 7.15), (xv, 2.76, 7.15), 0.08, "flange", MID, 8)
    rev(b, (1.5, DECK, 6.5), (1.5, 3.3, 6.5), [(0, 0.2), (0.06, 0.24), (0.96, 0.24), (1.04, 0.16)], SHELL, None, SHELL, 16)
    b.cyl((1.5, 3.3, 6.5), (1.5, 3.98, 6.5), 0.07, GUN, None, 10)
    for y in (3.1, 3.9):
        b.cyl((1.5, y, 6.5), (1.5, y + 0.04, 6.5), 0.13, "flange", MID, 10)
    run(b, [(1.26, DECK + 0.15, 6.5), (1.16, DECK + 0.15, 6.5), (1.16, DECK, 6.5)], 0.02, STEEL, sides=4)
    side_plate(b, -1, 1.26, 6.38, 6.62, 2.75, 2.83, tag_tex("DEMISTER", 0.24, 0.08), d=0.003)
    # The manway, the fill connection.
    b.cyl((2.25, DECK, 5.55), (2.25, DECK + 0.08, 5.55), 0.28, GUN, GUN, 18)
    b.cyl((2.25, DECK + 0.08, 5.55), (2.25, DECK + 0.13, 5.55), 0.31, GUN, GUN, 18)
    bolts(b, (2.25, DECK + 0.13, 5.55), 0.27, 12, 0.02, 0.03, MID, "y")
    run(b, [(2.12, DECK + 0.13, 5.55), (2.12, DECK + 0.22, 5.55), (2.38, DECK + 0.22, 5.55), (2.38, DECK + 0.13, 5.55)], 0.015, STEEL, sides=4)
    b.cyl((2.2, DECK, 4.9), (2.2, DECK + 0.2, 4.9), 0.06, OIL, None, 8)
    b.cyl((2.2, DECK + 0.2, 4.9), (2.2, DECK + 0.26, 4.9), 0.085, GUN, GUN, 8)
    # ---------------------------------------------------------------- the coolers and filters on the right
    # The coolers on their stands: shells, flanged channels and bonnets, oil in and out of the transfer valve between
    # them, the cooling water up out of the floor through its valves into the bottom bonnet, out of the top channel and
    # down; vents, drains, tags.
    for k, cz in enumerate((1.35, 3.55)):
        b.box(3.0, 0.0, cz - 0.4, 3.8, 0.23, cz + 0.4, GUN)
        rev(b, (3.4, 0.22, cz), (3.4, 0.45, cz), [(0, 0.26), (0.18, 0.36), (0.23, 0.36)], SHELL, SHELL, None, 18)
        b.cyl((3.4, 0.45, cz), (3.4, 2.15, cz), 0.33, SHELL, None, 18)
        rev(b, (3.4, 2.15, cz), (3.4, 2.5, cz), [(0, 0.36), (0.25, 0.36), (0.35, 0.24)], SHELL, None, SHELL, 18)
        for y in (0.42, 2.12, 2.38):
            b.cyl((3.4, y, cz), (3.4, y + 0.05, cz), 0.41, "flange", MID, 18)
        b.cyl((3.4, 2.5, cz), (3.4, 2.6, cz), 0.03, STEEL, None, 6)
        handwheel(b, (3.4, 2.6, cz), "y", 0.05)
        side_plate(b, 1, 3.73, cz - 0.17, cz + 0.17, 1.55, 1.65, tag_tex(f"LO COOLER {'AB'[k]}", 0.34, 0.1), d=0.004)
        zi, zo = cz - 0.2, cz + 0.2
        run(b, [(3.9, -0.05, zi), (3.9, 0.33, zi), (3.66, 0.33, zi)], 0.055, "pipe_green", joints="pipe_green", sides=8)
        run(b, [(3.66, 2.26, zo), (3.9, 2.26, zo), (3.9, -0.05, zo)], 0.055, "pipe_green", joints="pipe_green", sides=8)
        for (zz, y) in ((zi, 0.95), (zo, 1.2)):
            b.cyl((3.9, y, zz), (3.9, y + 0.06, zz), 0.09, GUN, GUN, 10)
            b.box(3.88, y + 0.06, zz - 0.02, 3.98, y + 0.1, zz + 0.02, sw("red"))
            b.cyl((3.9, 0.0, zz), (3.9, 0.03, zz), 0.1, MID, MID, 10)
        # Oil in from the valve near the top, out near the bottom.
        zs = 1 if k == 0 else -1
        for y in (2.0, 0.72):
            run(b, [(3.4, y, 2.45 - zs * 0.15), (3.4, y, cz + zs * 0.3)], 0.06, OIL, sides=10)
    # The cooler transfer valve: its body, bonnet, lever and tag; the oil to it from the header.
    b.cyl((3.4, 0.55, 2.45), (3.4, 2.3, 2.45), 0.16, GUN, GUN, 14)
    for y in (0.55, 2.25):
        b.cyl((3.4, y, 2.45), (3.4, y + 0.05, 2.45), 0.19, "flange", MID, 14)
    b.cyl((3.4, 2.3, 2.45), (3.4, 2.42, 2.45), 0.11, GUN, GUN, 10)
    b.cyl((3.4, 2.42, 2.45), (3.4, 2.5, 2.45), 0.03, STEEL, None, 6)
    b.box(3.36, 2.5, 2.42, 3.92, 2.55, 2.48, sw("red"))
    side_plate(b, 1, 3.56, 2.29, 2.61, 1.45, 1.53, tag_tex("COOLER XFER", 0.32, 0.08), d=0.004)
    run(b, [(HDR_X, HDR_Y, 2.45), (3.0, HDR_Y, 2.45), (3.0, 2.15, 2.45), (3.26, 2.15, 2.45)], 0.07, OIL, joints=OIL, sides=10)
    # The cooled oil from the valve along to the filters.
    run(b, [(3.26, 1.05, 2.45), (2.92, 1.05, 2.45), (2.92, 1.05, 4.95), (3.28, 1.05, 4.95)], 0.06, OIL, joints=OIL, sides=10)
    # The duplex filter on its stand over the drip pan: the housings, their bolted lids, vents and drains; the transfer
    # valve between them and its handle; the differential gauge on its bracket; the supply out to the bearings.
    b.box(3.05, 0.0, 4.1, 3.85, 0.05, 5.8, STEEL)
    b.box(3.05, 0.05, 4.1, 3.08, 0.12, 5.8, STEEL)
    b.box(3.82, 0.05, 4.1, 3.85, 0.12, 5.8, STEEL)
    for x in (3.15, 3.65):
        for z in (4.25, 5.65):
            b.box(x - 0.03, 0.05, z - 0.03, x + 0.03, 0.32, z + 0.03, GUN)
    b.box(3.1, 0.32, 4.2, 3.7, 0.36, 5.7, GUN)
    for z in (4.5, 5.4):
        rev(b, (3.4, 0.36, z), (3.4, 1.45, z), [(0, 0.16), (0.08, 0.22), (1.0, 0.22), (1.09, 0.2)], SHELL, SHELL, None, 16)
        b.cyl((3.4, 1.38, z), (3.4, 1.45, z), 0.25, "flange", MID, 16)
        rev(b, (3.4, 1.45, z), (3.4, 1.6, z), [(0, 0.24), (0.15, 0.12)], SHELL, None, SHELL, 16)
        bolts(b, (3.4, 1.45, z), 0.23, 8, 0.018, 0.03, MID, "y")
        b.cyl((3.4, 1.6, z), (3.4, 1.7, z), 0.02, STEEL, None, 4)
        b.cyl((3.4, 1.66, z), (3.4, 1.7, z), 0.04, GUN, GUN, 6)
        b.cyl((3.62, 0.5, z), (3.72, 0.5, z), 0.025, STEEL, None, 4)
        b.box(3.72, 0.46, z - 0.03, 3.76, 0.54, z + 0.03, GUN)
    for y in (1.05, 0.6):
        run(b, [(3.4, y, 4.7), (3.4, y, 5.2)], 0.05, OIL, sides=8)
    b.cyl((3.4, 0.45, 4.95), (3.4, 1.25, 4.95), 0.13, GUN, GUN, 12)
    b.cyl((3.4, 1.25, 4.95), (3.4, 1.32, 4.95), 0.03, STEEL, None, 6)
    b.box(3.36, 1.32, 4.92, 3.86, 1.36, 4.98, sw("red"))
    b.box(3.53, 1.36, 4.9, 3.57, 1.78, 5.0, GUN)
    gauge_x(b, live, "LO FILTER DP", (3.57, 1.65, 4.95), 0.09, gauge("FILTER DP", "PSID", 0, 30, 7, 0.09), 1)
    side_plate(b, 1, 3.57, 4.82, 5.08, 1.46, 1.52, tag_tex("FILTER DP", 0.26, 0.06), d=0.003)
    for z in (4.62, 5.28):
        run(b, [(3.55, 1.65, 4.95 + (z - 4.95) * 0.3), (3.55, 1.65, z), (3.55, 1.3, z)], 0.01, "stainless", sides=4)
    side_plate(b, 1, 3.62, 4.3, 4.7, 0.9, 1.0, tag_tex("LO FILTER A", 0.4, 0.1), d=0.004)
    side_plate(b, 1, 3.62, 5.2, 5.6, 0.9, 1.0, tag_tex("LO FILTER B", 0.4, 0.1), d=0.004)
    run(b, [(3.26, 0.6, 4.95), (2.92, 0.6, 4.95), (2.92, 0.6, 6.4), (3.4, 0.6, 6.4), (3.4, -0.05, 6.4)], 0.08, OIL, joints=OIL, sides=12)
    fl(b, (3.4, 0.3, 6.4), "y", 0.08)
    b.cyl((3.4, 0.0, 6.4), (3.4, 0.04, 6.4), 0.17, MID, MID, 12)
    # The supply's pressure transmitter on its stand, its tubing, its conduit; the supply's tag.
    b.box(3.68, 0.0, 6.85, 3.74, 1.2, 6.91, GUN)
    b.box(3.6, 1.0, 6.78, 3.82, 1.22, 6.98, sw("blue"))
    b.cyl((3.71, 1.22, 6.88), (3.71, 1.3, 6.88), 0.05, sw("blue"), sw("blue"), 8)
    run(b, [(3.6, 1.1, 6.88), (3.4, 1.1, 6.88), (3.4, 0.68, 6.5)], 0.01, "stainless", sides=4)
    run(b, [(3.71, 1.3, 6.88), (3.71, 1.4, 6.88), (3.92, 1.4, 6.88), (3.92, 0.0, 6.88)], 0.018, BLACK, sides=4)
    side_plate(b, 1, 3.82, 6.78, 6.98, 1.05, 1.17, tag_tex("PT|LO SUPPLY", 0.2, 0.12), d=0.003)
    # ---------------------------------------------------------------- the operating side (left)
    # The level glass on its valves (the level the game's), its tag; the level switches and their conduit.
    xf = TX0
    for y in (0.65, 2.0):
        b.cyl((xf, y, 0.95), (xf - 0.16, y, 0.95), 0.03, "stainless", "stainless", 6)
        b.cyl((xf - 0.07, y - 0.05, 0.95), (xf - 0.07, y + 0.05, 0.95), 0.04, GUN, GUN, 6)
        handwheel(b, (xf - 0.07, y + 0.07, 0.95), "y", 0.045)
    b.box(xf - 0.22, 0.65, 0.87, xf - 0.1, 2.0, 0.89, "stainless")
    b.box(xf - 0.22, 0.65, 1.01, xf - 0.1, 2.0, 1.03, "stainless")
    b.box(xf - 0.12, 0.65, 0.89, xf - 0.1, 2.0, 1.01, sw("white"))
    live["levels"].append({"name": "LO RESERVOIR", "z0": 0.9, "z1": 1.0, "y0": 0.67, "y1": 1.98, "x": _r(xf - 0.123)[0], "side": -1})
    side_plate(b, -1, xf, 0.75, 1.15, 0.43, 0.51, tag_tex("LO RES LEVEL", 0.4, 0.08), d=0.003)
    for y in (0.95, 1.55):
        b.cyl((xf, y + 0.1, 1.6), (xf - 0.08, y + 0.1, 1.6), 0.03, STEEL, STEEL, 6)
        b.box(xf - 0.2, y, 1.52, xf - 0.08, y + 0.2, 1.68, GUN)
        side_plate(b, -1, xf - 0.2, 1.54, 1.66, y + 0.04, y + 0.1, tag_tex("LSL" if y < 1 else "LSH", 0.12, 0.06), d=0.003)
    run(b, [(xf - 0.14, 1.15, 1.6), (xf - 0.14, 1.55, 1.6)], 0.018, BLACK, sides=4)
    run(b, [(xf - 0.14, 1.75, 1.6), (xf - 0.14, DECK + 0.15, 1.6), (0.46, DECK + 0.15, 1.6), (0.46, DECK + 0.05, 1.6)], 0.018, BLACK, sides=4)
    # The thermometer in its well, its tag.
    b.cyl((xf, 1.05, 2.12), (xf - 0.12, 1.05, 2.12), 0.025, "stainless", "stainless", 6)
    gauge_x(b, live, "LO RES TEMP", (xf - 0.12, 1.05, 2.12), 0.09, gauge("RES TEMP", "°F", 0, 200, 5, 0.09), -1)
    side_plate(b, -1, xf, 1.97, 2.27, 0.85, 0.91, tag_tex("RES TEMP", 0.3, 0.06), d=0.003)
    # The local gauge board on its brackets.
    gx = 0.1
    for z in (2.72, 4.18):
        for y in (1.2, 2.0):
            b.box(gx + 0.02, y, z - 0.03, xf, y + 0.05, z + 0.03, GUN)
    b.box(gx, 1.12, 2.6, gx + 0.02, 2.12, 4.3, ["stainless", "stainless", "stainless", None, "stainless", "stainless"])
    for k, (name, legend, unit, lo, hi, majors) in enumerate((("LO PUMP DISCH", "PUMP DISCH", "PSIG", 0, 150, 7),
                                                              ("BRG OIL HDR", "BRG OIL HDR", "PSIG", 0, 60, 7),
                                                              ("OIL TEMP", "BRG OIL TEMP", "°F", 0, 200, 5))):
        z = 3.92 - k * 0.47          # (read from the left: as the oil goes)
        gauge_x(b, live, name, (gx, 1.7, z), 0.13, gauge(legend, unit, lo, hi, majors, 0.13), -1)
        side_plate(b, -1, gx, z - 0.21, z + 0.21, 1.44, 1.51, tag_tex(legend, 0.42, 0.07), d=0.003)
    side_plate(b, -1, gx, 2.75, 4.15, 1.93, 2.06, tag_tex("MAIN TURBINE LUBE OIL|LOCAL GAUGE BOARD", 1.4, 0.13), d=0.004)
    # The pumps' local control panel: its enclosure on brackets, the door (hinges, latch), a row per pump (tag, RUNNING
    # lamp, selector), its title, its conduit up into the tray.
    for y in (1.0, 1.86):
        b.box(0.24, y, 4.52, xf, y + 0.05, 5.78, GUN)
    b.box(0.07, 0.85, 4.45, 0.24, 2.05, 5.85, PANEL)
    b.box(0.055, 0.87, 4.47, 0.07, 2.03, 5.83, [PANEL, PANEL, PANEL, None, PANEL, PANEL])
    for y in (1.0, 1.9):
        b.cyl((0.06, y - 0.06, 4.44), (0.06, y + 0.06, 4.44), 0.015, GUN, GUN, 6)
    b.box(0.035, 1.38, 5.76, 0.055, 1.52, 5.8, sw("dgrey"))
    for k, (name, legend) in enumerate((("aux_oil", "AUX OIL PP"), ("tg_oil", "TG OIL PP"), ("emerg_oil", "EMERG OIL PP"), ("lift_oil", "LIFT OIL PPS"))):
        y = 1.76 - k * 0.24
        # (seen from the left, the higher z is to the left: its tag, its RUNNING lamp, its selector)
        side_plate(b, -1, 0.055, 5.24, 5.78, y - 0.04, y + 0.04, tag_tex(legend, 0.54, 0.08), d=0.003)
        T.lamp_x(b, live, name, 0.055, y, 5.08, "red", -1, 0.032)
        b.cyl((0.055, y, 4.75), (0.04, y, 4.75), 0.045, "stainless", "stainless", 10)
        b.box(0.022, y - 0.012, 4.71, 0.04, y + 0.012, 4.79, BLACK)
        side_plate(b, -1, 0.055, 4.63, 4.87, y - 0.105, y - 0.065, tag_tex("STOP  AUTO  RUN", 0.24, 0.04), d=0.003)
    side_plate(b, -1, 0.055, 4.55, 5.75, 1.88, 1.99, tag_tex("MAIN TURBINE L.O. PUMPS - LOCAL", 1.2, 0.11), d=0.003)
    run(b, [(0.15, 2.05, 5.6), (0.15, DECK + 0.15, 5.6), (0.46, DECK + 0.15, 5.6), (0.46, DECK + 0.05, 5.6)], 0.025, BLACK, sides=6)
    # The tank's stencil.
    side_plate(b, -1, TX0, 6.12, 7.42, 1.2, 1.95, stencil_tex("MAIN TURBINE|LUBE OIL|RESERVOIR", 1.3, 0.75, (40, 42, 44)), edge=None, d=0.003)
    # ---------------------------------------------------------------- the ends
    # The back: the immersion heaters (flanges, terminal heads, conduit down), their tags; the temperature switch.
    for k, x in enumerate((1.2, 1.8)):
        b.cyl((x, 0.75, TZ1), (x, 0.75, TZ1 + 0.05), 0.14, GUN, GUN, 12)
        b.cyl((x, 0.75, TZ1 + 0.05), (x, 0.75, TZ1 + 0.22), 0.1, GUN, GUN, 12)
        run(b, [(x, 0.65, TZ1 + 0.14), (x, 0.0, TZ1 + 0.14)], 0.022, BLACK, sides=6)
        back_plate(b, TZ1, x - 0.15, x + 0.15, 0.97, 1.05, tag_tex(f"LO HTR {'AB'[k]}", 0.3, 0.08), d=0.003)
    b.cyl((0.85, 1.3, TZ1), (0.85, 1.3, TZ1 + 0.1), 0.03, "stainless", None, 6)
    b.box(0.76, 1.22, TZ1 + 0.1, 0.94, 1.38, TZ1 + 0.2, GUN)
    run(b, [(0.85, 1.22, TZ1 + 0.15), (0.85, 0.0, TZ1 + 0.15)], 0.018, BLACK, sides=4)
    # The front: the drain (its valve and blind), the sample valve, the nameplate.
    run(b, [(1.6, 0.45, TZ0), (1.6, 0.45, 0.12)], 0.06, OIL, sides=8)
    gate_valve(b, (1.6, 0.45, 0.28), "z", 0.06)
    b.cyl((1.6, 0.45, 0.1), (1.6, 0.45, 0.14), 0.11, "flange", MID, 10)
    b.cyl((2.2, 0.9, TZ0), (2.2, 0.9, 0.33), 0.02, "stainless", "stainless", 6)
    b.box(2.17, 0.86, 0.33, 2.23, 0.94, 0.39, GUN)
    b.box(2.19, 0.94, 0.35, 2.21, 1.04, 0.37, sw("red"))
    front_plate(b, TZ0 - 0.06, 1.75, 2.55, 1.45, 1.8, tag_tex("MAIN TURBINE|LUBE OIL RESERVOIR|12,000 GAL  ISO VG 32", 0.8, 0.35), d=0.004)
    live["mats"] = {"needle": sw("red"), "water": "glaze_brown"}
    b.live = live
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ EHC unit
#
# The EHC (electro-hydraulic control) fluid supply unit (3 wide, 5 deep, 3 tall; its front toward the aisle): the
# skid's base with its containment lip; the stainless reservoir up on its frame over the pumps (its cover, inspection
# cover, return filter with the return up the back into it, desiccant breather, fill cap, level transmitter; the level
# glass on its right side, its stencil on the left); under it the two pump sets (motor, coupling in its guard, the
# piston pump with its compensator, suction down from the reservoir through its valve), each discharging through its
# high-pressure filter (the differential indicator on its head) and check valve into the header along the right, the
# two bladder accumulators on their manifold blocks beside it, the supply down into the floor to the turbine's valves;
# at the back under the reservoir the fluid conditioning (fuller's earth and cellulose filters, their pump) and the
# cooler with its water; at the front the local panel (the gauges, the pumps' RUNNING lamps and selectors).

EW, ED, ET = 3, 5, 3
ER = (0.15, 2.0, 1.45, 2.45, 1.4, 4.8)     # the reservoir: x0, x1, y0, y1, z0, z1
EHC_PX = (0.6, 1.55)                        # the pump sets' lines (x)
EHC_HDR = 2.15                              # the high-pressure header along z (x)
SS = "stainless"


def ehc_unit(seed=433):
    with own_atlas():
        return _ehc_unit(seed)


def _ehc_unit(seed=433):
    b = C.Board(EW, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "levels": []}
    x0, x1, y0, y1, z0, z1 = ER
    # The base and its containment lip.
    b.box(0.1, 0.0, 0.15, 2.9, 0.06, 4.85, STEEL)
    b.core = len(b.still)
    for (xa, xb, za, zb) in ((0.1, 2.9, 0.15, 0.18), (0.1, 2.9, 4.82, 4.85), (0.1, 0.13, 0.18, 4.82), (2.87, 2.9, 0.18, 4.82)):
        b.box(xa, 0.06, za, xb, 0.2, zb, YELLOW)
    # The reservoir on its frame: corner posts and rails, the tank, its seams, its cover.
    for x in (x0 + 0.05, x1 - 0.05):
        for z in (z0 + 0.05, z1 - 0.05):
            b.box(x - 0.04, 0.06, z - 0.04, x + 0.04, y0 - 0.06, z + 0.04, GUN)
    for x in (x0 + 0.05, x1 - 0.05):
        b.box(x - 0.04, y0 - 0.06, z0 + 0.01, x + 0.04, y0, z1 - 0.01, GUN)
    b.box(x0, y0, z0, x1, y1, z1, [SS, SS, SS, SS, None, None])
    for z in (2.0, 3.75):
        b.box(x0 - 0.02, y0, z - 0.02, x0, y1, z + 0.02, "stainless_dark")
        b.box(x1, y0, z - 0.02, x1 + 0.02, y1, z + 0.02, "stainless_dark")
    b.box(x0 - 0.03, y1, z0 - 0.03, x1 + 0.03, y1 + 0.05, z1 + 0.03, "stainless_dark")
    top = y1 + 0.05
    # On the cover: the inspection cover and its bolts, the return filter (the return up the back into it), the
    # desiccant breather, the fill cap, the level transmitter.
    b.box(0.45, top, 2.0, 1.45, top + 0.04, 2.9, SS)
    for k in range(5):
        for z in (2.04, 2.86):
            b.cyl((0.53 + k * 0.21, top + 0.04, z), (0.53 + k * 0.21, top + 0.07, z), 0.018, MID, MID, 6)
    b.box(0.9, top + 0.04, 2.4, 1.0, top + 0.1, 2.5, SS)
    rev(b, (1.45, top, 4.3), (1.45, top + 0.38, 4.3), [(0, 0.1), (0.04, 0.14), (0.3, 0.14), (0.38, 0.1)], SS, None, SS, 14)
    b.cyl((1.45, top + 0.26, 4.3), (1.45, top + 0.3, 4.3), 0.17, "flange", MID, 14)
    b.cyl((1.45, top + 0.38, 4.3), (1.45, top + 0.45, 4.3), 0.035, sw("red"), sw("red"), 8)
    run(b, [(1.45, -0.05, 4.93), (1.45, top + 0.18, 4.93), (1.45, top + 0.18, 4.42)], 0.045, SS, joints=SS, sides=8)
    b.cyl((0.45, top, 4.35), (0.45, top + 0.1, 4.35), 0.04, SS, None, 6)
    b.cyl((0.45, top + 0.1, 4.35), (0.45, top + 0.13, 4.35), 0.11, SS, SS, 12)
    b.cyl((0.45, top + 0.13, 4.35), (0.45, top + 0.38, 4.35), 0.1, sw("blue"), None, 12)
    b.cyl((0.45, top + 0.38, 4.35), (0.45, top + 0.42, 4.35), 0.11, SS, SS, 12)
    b.cyl((0.5, top, 3.5), (0.5, top + 0.08, 3.5), 0.07, SS, None, 10)
    b.cyl((0.5, top + 0.08, 3.5), (0.5, top + 0.13, 3.5), 0.09, sw("yellow"), sw("yellow"), 10)
    b.cyl((1.7, top, 3.4), (1.7, top + 0.12, 3.4), 0.04, SS, None, 6)
    b.box(1.6, top + 0.12, 3.3, 1.8, top + 0.32, 3.5, sw("blue"))
    b.cyl((1.7, top + 0.32, 3.4), (1.7, top + 0.4, 3.4), 0.04, sw("blue"), sw("blue"), 8)
    run(b, [(1.8, top + 0.22, 3.4), (1.92, top + 0.22, 3.4), (1.92, top + 0.22, 4.86), (1.92, -0.05, 4.86)], 0.018, BLACK, sides=4)
    for y in (0.7, 1.9):
        b.box(1.88, y, z1, 1.96, y + 0.03, 4.89, GUN)
    # The level glass on the reservoir's right side (the level the game's), its tag; the stencil on its left.
    xf = x1
    gz = 4.1
    for y in (y0 + 0.12, y1 - 0.08):
        b.cyl((xf, y, gz), (xf + 0.16, y, gz), 0.025, SS, SS, 6)
    b.box(xf + 0.1, y0 + 0.12, gz - 0.08, xf + 0.2, y1 - 0.08, gz - 0.06, SS)
    b.box(xf + 0.1, y0 + 0.12, gz + 0.06, xf + 0.2, y1 - 0.08, gz + 0.08, SS)
    b.box(xf + 0.1, y0 + 0.12, gz - 0.06, xf + 0.12, y1 - 0.08, gz + 0.06, sw("white"))
    live["levels"].append({"name": "EHC RESERVOIR", "z0": gz - 0.05, "z1": gz + 0.05, "y0": _r(y0 + 0.14)[0], "y1": _r(y1 - 0.1)[0], "x": _r(xf + 0.123)[0], "side": 1})
    side_plate(b, 1, xf, 4.22, 4.7, 2.2, 2.28, tag_tex("EHC RES LEVEL", 0.48, 0.08), d=0.003)
    side_plate(b, -1, x0, 2.08, 3.67, 1.6, 2.3, stencil_tex("EHC FLUID|FIRE RESISTANT|NO PETROLEUM OIL", 1.59, 0.7, (36, 38, 40)), edge=None, d=0.003)
    # ---------------------------------------------------------------- the pump sets under the reservoir
    for k, px in enumerate(EHC_PX):
        b.box(px - 0.22, 0.06, 1.3, px + 0.22, 0.2, 2.95, GUN)
        motor_h(b, (px, 0.42, 2.0), "z", 0.72, 0.19)
        # The coupling guard; the piston pump, its compensator; the suction down from the reservoir through its valve.
        b.box(px - 0.13, 0.28, 1.76, px + 0.13, 0.58, 2.0, [YELLOW] * 6)
        b.box(px - 0.15, 0.27, 1.35, px + 0.15, 0.57, 1.76, GUN)
        b.cyl((px, 0.42, 1.28), (px, 0.42, 1.35), 0.12, GUN, GUN, 12)
        b.cyl((px + 0.15, 0.48, 1.5), (px + 0.24, 0.48, 1.5), 0.035, GUN, GUN, 8)
        b.cyl((px + 0.24, 0.48, 1.5), (px + 0.27, 0.48, 1.5), 0.05, BLACK, BLACK, 8)
        run(b, [(px, 0.57, 1.62), (px, y0, 1.62)], 0.045, SS, sides=8)
        b.cyl((px, 0.95, 1.62), (px, 1.07, 1.62), 0.07, GUN, GUN, 8)
        b.box(px - 0.01, 1.0, 1.62, px + 0.01, 1.03, 1.85, sw("yellow"))
        # The case drain back up into the reservoir.
        run(b, [(px - 0.15, 0.5, 1.45), (px - 0.2, 0.5, 1.45), (px - 0.2, y0, 1.45)], 0.015, SS, sides=4)
        # The discharge forward into its filter: the housing, its bowl, the indicator on its head; up out of it through
        # the check valve into the line over to the header.
        run(b, [(px, 0.42, 1.28), (px, 0.42, 0.96)], 0.04, SS, sides=8)
        rev(b, (px, 0.22, 0.85), (px, 0.98, 0.85), [(0, 0.06), (0.06, 0.1), (0.56, 0.1), (0.56, 0.13), (0.76, 0.13)], SS, SS, GUN, 12)
        b.cyl((px + 0.1, 0.9, 0.85), (px + 0.16, 0.9, 0.85), 0.03, GUN, GUN, 8)
        b.cyl((px + 0.16, 0.9, 0.85), (px + 0.18, 0.9, 0.85), 0.02, sw("green"), sw("green"), 8)
        b.cyl((px, 0.98, 0.85), (px, 1.08, 0.85), 0.035, SS, None, 8)
        b.cyl((px, 1.08, 0.85), (px, 1.2, 0.85), 0.06, GUN, GUN, 10)
        run(b, [(px, 1.2, 0.85), (px, 1.28, 0.85), (EHC_HDR, 1.28, 0.85)] if k == 0 else [(px, 1.2, 0.85), (px, 1.28, 0.85)], 0.035, SS, joints=SS, sides=8)
        side_plate(b, 1, px + 0.13, 0.75, 0.95, 0.66, 0.73, tag_tex(f"FLTR {'AB'[k]}", 0.2, 0.07), d=0.003)
    run(b, [(EHC_HDR, 1.28, 0.85), (EHC_HDR, 0.32, 0.85), (EHC_HDR, 0.32, 2.7), (EHC_HDR, -0.05, 2.7)], 0.045, SS, joints=SS, sides=8)
    b.cyl((EHC_HDR, 0.0, 2.7), (EHC_HDR, 0.03, 2.7), 0.09, MID, MID, 10)
    # ---------------------------------------------------------------- the accumulators beside the header
    for k, az in enumerate((1.1, 2.1)):
        b.box(2.36, 0.06, az - 0.14, 2.64, 0.4, az + 0.14, GUN)
        run(b, [(2.36, 0.32, az), (EHC_HDR, 0.32, az)], 0.03, SS, sides=6)
        b.cyl((2.64, 0.25, az), (2.72, 0.25, az), 0.03, GUN, GUN, 6)
        b.box(2.72, 0.24, az - 0.01, 2.85, 0.26, az + 0.01, sw("red"))
        rev(b, (2.5, 0.4, az), (2.5, 2.7, az), [(0, 0.08), (0.12, 0.17), (2.12, 0.17), (2.3, 0.06)], "paint_white", None, GUN, 16)
        b.cyl((2.5, 2.7, az), (2.5, 2.8, az), 0.04, GUN, GUN, 6)
        b.cyl((2.5, 2.8, az), (2.5, 2.86, az), 0.055, sw("red"), sw("red"), 8)
        for y in (0.9, 2.1):
            rev(b, (2.5, y, az), (2.5, y + 0.06, az), [(0, 0.185), (0.06, 0.185)], GUN, None, None, 16)
            b.box(2.69, y, az - 0.04, 2.84, y + 0.06, az + 0.04, GUN)
    # Their rack: two uprights and the rails their clamps bolt to, their tags on the rails.
    for z in (0.85, 2.35):
        b.box(2.8, 0.06, z - 0.03, 2.86, 2.4, z + 0.03, GUN)
    for y in (0.9, 2.1):
        b.box(2.84, y - 0.01, 0.88, 2.88, y + 0.07, 2.32, GUN)
    for k, az in enumerate((1.1, 2.1)):
        side_plate(b, 1, 2.88, az - 0.13, az + 0.13, 2.17, 2.25, tag_tex(f"ACCUM {'AB'[k]}", 0.26, 0.08), d=0.003)
    # ---------------------------------------------------------------- the conditioning loop and the cooler at the back
    for k, (cx, r, tag) in enumerate(((0.5, 0.17, "FULLERS EARTH"), (1.15, 0.15, "CELLULOSE"))):
        rev(b, (cx, 0.06, 4.25), (cx, 1.2, 4.25), [(0, r - 0.03), (0.08, r), (1.0, r), (1.14, r - 0.06)], SS, None, SS, 14)
        b.cyl((cx, 1.0, 4.25), (cx, 1.04, 4.25), r + 0.04, "flange", MID, 14)
        run(b, [(cx, 1.2, 4.25), (cx, 1.3, 4.25), (cx, 1.3, 3.75), (cx, y0, 3.75)] if k else [(cx, 1.2, 4.25), (cx, 1.3, 4.25), (1.15, 1.3, 4.25)], 0.03, SS, sides=6)
    motor_h(b, (0.75, 0.3, 3.35), "x", 0.38, 0.11)
    b.cyl((0.6, 0.3, 3.35), (0.75, 0.3, 3.35), 0.1, GUN, GUN, 10)
    run(b, [(0.6, 0.3, 3.35), (0.5, 0.3, 3.35), (0.5, 0.3, 4.08)], 0.03, SS, sides=6)
    b.box(0.5, 0.06, 3.22, 1.3, 0.17, 3.48, GUN)
    run(b, [(0.68, 0.4, 3.35), (0.68, y0, 3.35)], 0.03, SS, sides=6)
    # The cooler along the right at the back: shell, channel heads, the fluid in and out of the reservoir, the water.
    b.cyl((2.5, 0.75, 2.95), (2.5, 0.75, 4.6), 0.16, SS, None, 14)
    for (za, zb) in ((2.85, 2.95), (4.6, 4.7)):
        b.cyl((2.5, 0.75, za), (2.5, 0.75, zb), 0.2, GUN, GUN, 14)
    for z in (3.1, 4.45):
        b.box(2.36, 0.06, z - 0.05, 2.64, 0.62, z + 0.05, GUN)
        run(b, [(2.5, 0.9, z), (2.5, 1.7, z), (x1 + 0.03, 1.7, z)], 0.035, SS, joints=SS, sides=8)
    for z in (2.9, 4.65):
        run(b, [(2.68, 0.75, z), (2.8, 0.75, z), (2.8, -0.05, z)], 0.04, "pipe_green", joints="pipe_green", sides=8)
        b.cyl((2.8, 0.35, z), (2.8, 0.42, z), 0.07, GUN, GUN, 8)
        b.box(2.79, 0.42, z - 0.01, 2.88, 0.45, z + 0.01, sw("red"))
    side_plate(b, 1, 2.66, 3.5, 4.0, 0.72, 0.8, tag_tex("EHC COOLER", 0.5, 0.08), d=0.003)
    # ---------------------------------------------------------------- the local panel at the front
    zf = 0.24
    for x in (0.28, 1.92):
        b.box(x - 0.03, 0.08, zf + 0.04, x + 0.03, 1.25, zf + 0.1, GUN)
        b.box(x - 0.08, 0.06, zf, x + 0.08, 0.08, zf + 0.14, GUN)
    b.box(0.2, 1.25, zf + 0.02, 2.0, 2.45, zf + 0.04, [SS, None, SS, SS, SS, SS])
    for y in (1.3, 2.35):
        b.box(0.25, y, zf + 0.04, 1.95, y + 0.05, zf + 0.1, GUN)
    gauges = (("EHC PRESS", "EHC HDR", "PSIG", 0, 2000, 5), ("EHC PP A DISCH", "PUMP A DISCH", "PSIG", 0, 2500, 6),
              ("EHC PP B DISCH", "PUMP B DISCH", "PSIG", 0, 2500, 6), ("FLUID TEMP", "FLUID TEMP", "°F", 0, 200, 5))
    for k, (name, legend, unit, lo, hi, majors) in enumerate(gauges):
        gx = 0.43 + k * 0.45
        gauge_z(b, live, name, (gx, 1.98, zf + 0.02), 0.13, gauge(legend, unit, lo, hi, majors, 0.13))
        front_plate(b, zf + 0.02, gx - 0.2, gx + 0.2, 1.73, 1.8, tag_tex(legend, 0.4, 0.07), d=0.003)
    for k, (name, legend) in enumerate((("ehc_a", "EHC PUMP A"), ("ehc_b", "EHC PUMP B"))):
        gx = 0.25 + k * 0.9
        front_plate(b, zf + 0.02, gx + 0.03, gx + 0.67, 1.58, 1.65, tag_tex(legend, 0.64, 0.07), d=0.003)
        lamp_z(b, live, name, gx + 0.2, 1.45, zf + 0.02, "red", 0.032)
        b.cyl((gx + 0.5, 1.45, zf + 0.02), (gx + 0.5, 1.45, zf + 0.005), 0.045, SS, SS, 10)
        b.box(gx + 0.46, 1.438, zf - 0.015, gx + 0.54, 1.462, zf + 0.005, BLACK)
        front_plate(b, zf + 0.02, gx + 0.08, gx + 0.32, 1.36, 1.4, tag_tex("RUNNING", 0.24, 0.04), d=0.003)
        front_plate(b, zf + 0.02, gx + 0.38, gx + 0.62, 1.36, 1.4, tag_tex("STOP AUTO RUN", 0.24, 0.04), d=0.003)
    front_plate(b, zf + 0.02, 0.3, 1.9, 2.2, 2.34, tag_tex("EHC FLUID SUPPLY UNIT|LOCAL PANEL", 1.6, 0.14), d=0.004)
    run(b, [(1.92, 1.25, zf + 0.13), (1.92, 0.06, zf + 0.13)], 0.022, BLACK, sides=6)
    # The gauges' tubing from behind the panel down to the header.
    run(b, [(0.88, 1.98, zf + 0.04), (0.88, 1.98, zf + 0.16), (0.88, 1.32, zf + 0.16), (EHC_HDR - 0.04, 1.32, zf + 0.16), (EHC_HDR - 0.04, 1.32, 0.85)],
        0.01, SS, sides=4)
    live["mats"] = {"needle": sw("red"), "water": "glaze_brown"}
    b.live = live
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ seal oil unit
#
# The generator's seal oil unit (3 wide, 5 deep, 3 tall; its front toward the aisle): a dual-flow unit on its skid.
# At the back the three pump sets across it (the air side pump, its DC backup, the hydrogen side pump; each a gear
# pump, its coupling guard and motor, its suction strainer, its discharge up through the check and ball valves); the
# hydrogen side's drain regulator tank beside its pump (its level glass, the level the game's; the float switch, the
# vent and relief valve, the drains in), the hydrogen side's cooler and filter, its line out through the pressure
# equalizing valve (sensing the air side) into the floor. At the front the air side's cooler (its water) and filter,
# the differential pressure regulator (sensing the generator's casing) and the air side's supply into the floor; the
# local gauge board at the front left; the pumps' control station and the hydrogen danger sign at the front right.

SOW, SOD, SOT = 3, 5, 3
SO_PUMPS = (("so_dc", "DC BACKUP|SO PUMP", 2.6, sw("orange")), ("so_air", "AIR SIDE|SO PUMP", 3.45, MOTOR), ("so_h2", "H2 SIDE|SO PUMP", 4.3, MOTOR))
SO_TOP = 1.75                    # the pumps' discharge lines' height


def seal_oil_unit(seed=435):
    with own_atlas():
        return _seal_oil_unit(seed)


def _so_cooler(b, cx, cz, r, cw_x, tag):
    """A small vertical shell-and-tube cooler on its stand: bonnets, shell, flanges, vent; its water up out of the floor
    into the bottom and out of the top back down; its tag."""
    b.box(cx - r - 0.03, 0.06, cz - r - 0.03, cx + r + 0.03, 0.14, cz + r + 0.03, GUN)
    rev(b, (cx, 0.12, cz), (cx, 0.32, cz), [(0, r - 0.06), (0.15, r), (0.2, r)], SHELL, SHELL, None, 14)
    b.cyl((cx, 0.32, cz), (cx, 1.35, cz), r - 0.02, SHELL, None, 14)
    rev(b, (cx, 1.35, cz), (cx, 1.55, cz), [(0, r), (0.06, r), (0.2, r - 0.08)], SHELL, None, SHELL, 14)
    for y in (0.3, 1.33):
        b.cyl((cx, y, cz), (cx, y + 0.04, cz), r + 0.04, "flange", MID, 14)
    b.cyl((cx, 1.55, cz), (cx, 1.62, cz), 0.02, STEEL, None, 4)
    s = 1 if cw_x > cx else -1
    for (y, dz) in ((0.24, -0.1), (1.42, 0.1)):
        run(b, [(cx + s * (r - 0.05), y, cz + dz), (cw_x, y, cz + dz), (cw_x, -0.05, cz + dz)], 0.04, "pipe_green", joints="pipe_green", sides=8)
    b.cyl((cw_x, 0.7, cz + 0.1), (cw_x, 0.76, cz + 0.1), 0.07, GUN, GUN, 8)
    b.box(cw_x - 0.01, 0.76, cz + 0.1, cw_x + 0.01, 0.79, cz + 0.22, sw("red"))
    front_plate(b, cz - r + 0.01, cx - 0.15, cx + 0.15, 0.85, 0.93, tag_tex(tag, 0.3, 0.08), d=0.003)


def _so_filter(b, cx, cz, tag):
    """A cartridge filter: its housing, the bolted lid, its vent, the differential indicator, its tag."""
    rev(b, (cx, 0.06, cz), (cx, 1.0, cz), [(0, 0.1), (0.06, 0.14), (0.8, 0.14), (0.8, 0.17), (0.94, 0.17)], SHELL, SHELL, SHELL, 14)
    bolts(b, (cx, 1.0, cz), 0.15, 8, 0.015, 0.025, MID, "y")
    b.cyl((cx, 1.0, cz), (cx, 1.08, cz), 0.02, STEEL, None, 4)
    b.cyl((cx + 0.15, 0.95, cz), (cx + 0.21, 0.95, cz), 0.025, GUN, GUN, 8)
    b.cyl((cx + 0.21, 0.95, cz), (cx + 0.23, 0.95, cz), 0.018, sw("green"), sw("green"), 8)
    front_plate(b, cz - 0.13, cx - 0.12, cx + 0.12, 0.55, 0.62, tag_tex(tag, 0.24, 0.07), d=0.003)


def _diaphragm_valve(b, c, axis, r, dome):
    """A self-acting control valve in a line along axis: its globe body and flanges, the yoke, the diaphragm casings
    with their bolted rim on top."""
    x, y, z = c
    d = (r * 1.6, 0, 0) if axis == "x" else (0, 0, r * 1.6)
    b.cyl((x - d[0], y, z - d[2]), (x + d[0], y, z + d[2]), r * 1.3, GUN, GUN, 12)
    for s in (-1, 1):
        q = (x + s * d[0] * 1.05, y, z + s * d[2] * 1.05)
        b.cyl((q[0] - d[0] * 0.08, y, q[2] - d[2] * 0.08), (q[0] + d[0] * 0.08, y, q[2] + d[2] * 0.08), r * 1.5, "flange", MID, 12)
    yt = y + r * 1.3
    b.cyl((x, yt, z), (x, yt + 0.06, z), r, GUN, GUN, 10)
    for s in (-1, 1):
        ox, oz = (0, s * r * 0.8) if axis == "x" else (s * r * 0.8, 0)
        b.box(x + ox - 0.015, yt + 0.06, z + oz - 0.015, x + ox + 0.015, yt + 0.2, z + oz + 0.015, GUN)
    b.cyl((x, yt + 0.06, z), (x, yt + 0.2, z), 0.015, STEEL, None, 4)
    y0 = yt + 0.2
    rev(b, (x, y0, z), (x, y0 + 0.09, z), [(0, 0.05), (0.09, dome)], sw("blue"), sw("blue"), None, 16)
    b.cyl((x, y0 + 0.09, z), (x, y0 + 0.11, z), dome + 0.025, sw("blue"), sw("blue"), 16)
    bolts(b, (x, y0 + 0.11, z), dome + 0.005, 10, 0.012, 0.018, MID, "y")
    rev(b, (x, y0 + 0.11, z), (x, y0 + 0.2, z), [(0, dome), (0.09, 0.05)], sw("blue"), None, sw("blue"), 16)
    return y0 + 0.2


def _so_pump(b, z, paint, tag):
    """A seal oil pump set across the skid (its pump at the left): base, gear pump on its feet with the relief valve,
    the coupling guard (its tag), the motor, its terminal box and conduit; the discharge up through the check valve and
    the ball valve."""
    b.box(0.95, 0.06, z - 0.27, 2.29, 0.18, z + 0.27, GUN)
    b.box(1.05, 0.18, z - 0.2, 1.35, 0.24, z + 0.2, GUN)
    b.box(1.02, 0.24, z - 0.16, 1.38, 0.58, z + 0.16, PUMP)
    b.cyl((1.0, 0.41, z), (1.02, 0.41, z), 0.13, PUMP, PUMP, 12)
    b.cyl((1.3, 0.58, z + 0.08), (1.3, 0.7, z + 0.08), 0.035, GUN, GUN, 8)
    b.cyl((1.3, 0.7, z + 0.08), (1.3, 0.74, z + 0.08), 0.02, BLACK, BLACK, 6)
    b.box(1.38, 0.26, z - 0.13, 1.6, 0.58, z + 0.13, [YELLOW] * 6)
    front_plate(b, z - 0.13, 1.4, 1.58, 0.36, 0.5, tag_tex(tag, 0.18, 0.14), d=0.003)
    motor_h(b, (1.6, 0.42, z), "x", 0.55, 0.17, paint)
    b.box(1.76, 0.5, z + 0.12, 1.96, 0.66, z + 0.25, paint)
    run(b, [(1.86, 0.58, z + 0.25), (1.86, 0.58, z + 0.34), (1.86, -0.05, z + 0.34)], 0.02, BLACK, sides=6)
    run(b, [(1.2, 0.58, z), (1.2, SO_TOP, z)], 0.04, OIL, joints=OIL, sides=8)
    b.cyl((1.2, 0.82, z), (1.2, 0.96, z), 0.065, GUN, GUN, 10)
    rev(b, (1.2, 0.89, z - 0.065), (1.2, 0.89, z - 0.1), [(0, 0.04), (0.035, 0.025)], GUN, None, GUN, 8)
    b.cyl((1.2, 1.12, z), (1.2, 1.22, z), 0.06, GUN, GUN, 10)
    b.box(1.18, 1.2, z - 0.25, 1.22, 1.23, z, sw("red"))


def _seal_oil_unit(seed=435):
    b = C.Board(SOW, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "levels": []}
    # The skid: its deck and the channel round it.
    b.box(0.1, 0.0, 0.15, 2.9, 0.06, 4.85, STEEL)
    b.core = len(b.still)
    for (xa, xb, za, zb) in ((0.1, 0.18, 0.15, 4.85), (2.82, 2.9, 0.15, 4.85), (0.18, 2.82, 0.15, 0.23), (0.18, 2.82, 4.77, 4.85)):
        b.box(xa, 0.06, za, xb, 0.16, zb, GUN)
    # ---------------------------------------------------------------- the pumps at the back
    for (name, tag, z, paint) in SO_PUMPS:
        _so_pump(b, z, paint, tag)
        if name != "so_h2":
            # Suction up out of the floor through its strainer into the pump.
            run(b, [(0.82, -0.05, z), (0.82, 0.41, z), (1.03, 0.41, z)], 0.045, OIL, joints=OIL, sides=8)
            b.cyl((0.82, 0.18, z), (0.82, 0.32, z), 0.07, GUN, GUN, 10)
            b.cyl((0.82, 0.22, z), (0.7, 0.14, z), 0.04, GUN, GUN, 8)
    # The air side's header from its two pumps forward to its cooler.
    run(b, [(1.2, SO_TOP, 3.45), (1.2, SO_TOP, 1.6), (0.55, SO_TOP, 1.6), (0.55, 1.5, 1.6)], 0.05, OIL, joints=OIL, sides=8)
    # ---------------------------------------------------------------- the hydrogen side
    # The drain regulator tank: its skirt, shell and heads; the drains in from the floor; the float switch and its
    # conduit; the vent and relief valve; the level glass on its left (the level the game's) and its tag; its outlet
    # into the hydrogen side's pump.
    tx, tz, tr = 0.55, 4.3, 0.32
    b.cyl((tx, 0.06, tz), (tx, 0.18, tz), tr - 0.04, GUN, None, 16)
    rev(b, (tx, 0.18, tz), (tx, 1.95, tz), [(0, 0.15), (0.12, tr), (1.55, tr), (1.77, 0.12)], SHELL, SHELL, SHELL, 18)
    b.cyl((tx, 0.4, tz), (tx, 0.44, tz), tr + 0.03, "flange", MID, 18)
    run(b, [(tx, -0.05, 4.8), (tx, 1.5, 4.8), (tx, 1.5, tz + tr - 0.05)], 0.05, OIL, joints=OIL, sides=8)
    b.cyl((tx + 0.12, 1.88, tz), (tx + 0.12, 2.0, tz), 0.06, GUN, GUN, 8)
    b.box(tx + 0.03, 2.0, tz - 0.09, tx + 0.21, 2.16, tz + 0.09, GUN)
    run(b, [(tx + 0.21, 2.08, tz), (tx + 0.4, 2.08, tz), (tx + 0.4, 2.08, 4.62), (tx + 0.4, -0.05, 4.62)], 0.018, BLACK, sides=4)
    b.cyl((tx - 0.1, 1.9, tz), (tx - 0.1, 2.6, tz), 0.035, STEEL, None, 6)
    b.cyl((tx - 0.1, 2.3, tz), (tx - 0.1, 2.42, tz), 0.06, GUN, GUN, 8)
    b.cyl((tx - 0.1, 2.6, tz), (tx - 0.1, 2.64, tz), 0.07, "flange", MID, 8)
    xg = tx - tr
    for y in (0.45, 1.65):
        b.cyl((xg + 0.02, y, tz), (0.08, y, tz), 0.025, SS, SS, 6)
    b.box(0.02, 0.45, tz - 0.08, 0.14, 1.65, tz - 0.06, SS)
    b.box(0.02, 0.45, tz + 0.06, 0.14, 1.65, tz + 0.08, SS)
    b.box(0.12, 0.45, tz - 0.06, 0.14, 1.65, tz + 0.06, sw("white"))
    live["levels"].append({"name": "SO DRAIN REG", "z0": tz - 0.05, "z1": tz + 0.05, "y0": 0.47, "y1": 1.63, "x": 0.117, "side": -1})
    b.box(0.03, 0.2, tz - 0.2, 0.14, 0.38, tz + 0.2, GUN)
    side_plate(b, -1, 0.03, tz - 0.18, tz + 0.18, 0.22, 0.36, tag_tex("H2 SIDE|DRAIN REG TANK", 0.36, 0.14), d=0.003)
    run(b, [(tx + tr - 0.03, 0.41, tz), (1.03, 0.41, tz)], 0.045, OIL, sides=8)
    # Its cooler and filter at the back right, its line forward through the pressure equalizing valve into the floor.
    run(b, [(1.2, SO_TOP, 4.3), (2.55, SO_TOP, 4.3), (2.55, 1.5, 4.3)], 0.045, OIL, joints=OIL, sides=8)
    _so_cooler(b, 2.55, 4.3, 0.2, 2.8, "H2 SIDE CLR")
    run(b, [(2.55, 0.22, 4.12), (2.55, 0.22, 3.6)], 0.04, OIL, sides=8)
    _so_filter(b, 2.55, 3.45, "H2 SIDE FLTR")
    run(b, [(2.55, 1.0, 3.45), (2.55, 1.4, 3.45), (2.55, 1.4, 2.35), (2.55, -0.05, 2.35)], 0.045, OIL, joints=OIL, sides=8)
    pev = _diaphragm_valve(b, (2.55, 1.4, 2.9), "z", 0.06, 0.15)
    # ---------------------------------------------------------------- the air side at the front
    _so_cooler(b, 0.55, 1.6, 0.22, 0.2, "AIR SIDE CLR")
    run(b, [(0.75, 0.22, 1.6), (1.18, 0.22, 1.6)], 0.04, OIL, sides=8)
    _so_filter(b, 1.32, 1.6, "AIR SIDE FLTR")
    run(b, [(1.32, 1.0, 1.6), (1.32, 1.3, 1.6), (2.4, 1.3, 1.6), (2.4, -0.05, 1.6)], 0.045, OIL, joints=OIL, sides=8)
    dpr = _diaphragm_valve(b, (1.92, 1.3, 1.6), "x", 0.07, 0.2)
    side_plate(b, 1, 2.08, 1.48, 1.72, 1.12, 1.19, tag_tex("DP REG", 0.24, 0.07), d=0.003)
    side_plate(b, 1, 2.65, 2.78, 3.02, 1.22, 1.29, tag_tex("PEV", 0.24, 0.07), d=0.003)
    # The regulator's sensing line from the generator's casing (up out of the floor beside it); the equalizing valve's
    # from the air side's supply.
    run(b, [(1.92, dpr, 1.6), (1.92, dpr + 0.05, 1.6), (1.92, dpr + 0.05, 1.86), (1.92, -0.05, 1.86)], 0.012, SS, sides=4)
    b.box(1.88, 0.06, 1.82, 1.96, 0.1, 1.9, GUN)
    run(b, [(2.55, pev, 2.9), (2.55, pev + 0.05, 2.9), (2.68, pev + 0.05, 2.9), (2.68, 0.9, 2.9), (2.68, 0.9, 1.6), (2.445, 0.9, 1.6)],
        0.012, SS, sides=4)
    # ---------------------------------------------------------------- the local gauge board, front left
    zf = 0.2
    for x in (0.25, 1.45):
        b.box(x - 0.03, 0.06, zf + 0.04, x + 0.03, 2.2, zf + 0.1, GUN)
    b.box(0.15, 0.95, zf, 1.55, 2.25, zf + 0.02, [SS, None, SS, SS, SS, SS])
    for k, (name, legend, unit, lo, hi, majors) in enumerate((("SEAL OIL", "AIR SIDE S.O.", "PSIG", 0, 150, 7),
                                                              ("SO H2 SIDE", "H2 SIDE S.O.", "PSIG", 0, 150, 7),
                                                              ("H2 CASING", "H2 CASING", "PSIG", 0, 100, 6),
                                                              ("SEAL DP", "S.O. - H2 DP", "PSID", 0, 20, 5))):
        gx, gy = (0.5, 1.2)[k % 2], (1.8, 1.32)[k // 2]
        gauge_z(b, live, name, (gx, gy, zf), 0.14, gauge(legend, unit, lo, hi, majors, 0.14))
        front_plate(b, zf, gx - 0.24, gx + 0.24, gy - 0.25, gy - 0.18, tag_tex(legend, 0.48, 0.07), d=0.003)
        run(b, [(gx, gy, zf + 0.02), (gx, gy, zf + 0.14), (gx + 0.06 * (k - 1.5), gy - 0.2, zf + 0.14), (gx + 0.06 * (k - 1.5), 0.06, zf + 0.14)],
            0.01, SS, sides=4)
    front_plate(b, zf, 0.22, 1.48, 2.07, 2.21, tag_tex("GENERATOR SEAL OIL|LOCAL GAUGE BOARD", 1.26, 0.14), d=0.004)
    # ---------------------------------------------------------------- the control station and the sign, front right
    cz = 0.06
    b.box(2.27, 0.0, cz + 0.01, 2.33, 1.0, cz + 0.07, GUN)
    b.box(2.2, 0.0, 0.03, 2.4, 0.02, 0.145, GUN)
    b.box(1.8, 1.0, cz, 2.8, 1.95, cz + 0.18, PANEL)
    b.box(1.82, 1.02, cz - 0.015, 2.78, 1.93, cz, [PANEL, None, PANEL, PANEL, PANEL, PANEL])
    for y in (1.1, 1.85):
        b.cyl((1.81, y - 0.05, cz - 0.01), (1.81, y + 0.05, cz - 0.01), 0.015, GUN, GUN, 6)
    b.box(2.76, 1.4, cz - 0.04, 2.79, 1.55, cz - 0.015, sw("dgrey"))
    for k, (name, legend) in enumerate((("so_air", "AIR SIDE PP"), ("so_dc", "DC BACKUP PP"), ("so_h2", "H2 SIDE PP"))):
        y = 1.66 - k * 0.24
        front_plate(b, cz - 0.015, 1.86, 2.36, y - 0.04, y + 0.04, tag_tex(legend, 0.5, 0.08), d=0.003)
        lamp_z(b, live, name, 2.46, y, cz - 0.015, "red", 0.032)
        b.cyl((2.66, y, cz - 0.015), (2.66, y, cz - 0.03), 0.045, SS, SS, 10)
        b.box(2.62, y - 0.012, cz - 0.048, 2.7, y + 0.012, cz - 0.03, BLACK)
        front_plate(b, cz - 0.015, 2.54, 2.78, y - 0.1, y - 0.06, tag_tex("STOP AUTO RUN", 0.24, 0.04), d=0.003)
    front_plate(b, cz - 0.015, 1.88, 2.72, 1.8, 1.9, tag_tex("SEAL OIL PUMPS - LOCAL", 0.84, 0.1), d=0.003)
    run(b, [(2.5, 1.0, cz + 0.12), (2.5, 0.0, cz + 0.12)], 0.022, BLACK, sides=6)
    b.box(2.08, 0.4, cz - 0.01, 2.52, 0.84, cz + 0.01, GUN)
    front_plate(b, cz - 0.01, 2.1, 2.5, 0.42, 0.82, plate("DANGER|HYDROGEN|NO OPEN|FLAMES", 0.4, 0.4, "red"), d=0.004)
    live["mats"] = {"needle": sw("red"), "water": "glaze_brown"}
    b.live = live
    on_floor(b)
    return b


# ------------------------------------------------------------------------------------------------ gas panel
#
# The generator's gas control station (4 wide, 2 deep, 3 tall; its front toward the generator): on the left the gas
# analyzer cabinet on its plinth (its door with hinges, handle and louvres; the purity readout, the game's digits; the
# casing pressure and dew point gauges; the low purity and low pressure lamps; the sample tubing in at its side, its
# vent and conduit up out of the top); beside it the hydrogen dryer (twin desiccant towers on their skid, the switching
# valve, the heaters, the blower, the gas in and out of the floor); on the right the cylinder rack (four hydrogen, two
# carbon dioxide; chains, pigtails, the manifolds and their valves) under the regulator panel (the hydrogen supply and
# carbon dioxide purge regulators, each with its bottle and outlet gauges, the outlets down behind into the floor) and
# the hydrogen danger sign.

GPW, GPD, GPT = 4, 2, 3
CYL_X = (2.44, 2.72, 3.0, 3.28, 3.56, 3.84)


def h2_gas_panel(seed=437):
    with own_atlas():
        return _h2_gas_panel(seed)


def _regulator(b, live, x, y, zp, names, legends, ranges, tag):
    """A two-gauge regulator on the panel face (toward -z at zp): its body, the spring bonnet and T-handle, the bottle
    and outlet gauges either side on their stems, its tag."""
    b.cyl((x, y, zp), (x, y, zp - 0.12), 0.07, GUN, GUN, 12)
    b.cyl((x, y, zp - 0.12), (x, y, zp - 0.26), 0.05, SS, SS, 12)
    b.cyl((x, y, zp - 0.26), (x, y, zp - 0.3), 0.012, STEEL, None, 4)
    b.box(x - 0.06, y - 0.01, zp - 0.32, x + 0.06, y + 0.01, zp - 0.3, BLACK)
    for k, s in enumerate((-1, 1)):
        gx = x + s * 0.2
        run(b, [(x + s * 0.06, y, zp - 0.06), (gx, y, zp - 0.06), (gx, y + 0.24, zp - 0.06)], 0.012, SS, sides=4)
        unit, lo, hi, majors = ranges[k]
        gauge_z(b, live, names[k], (gx, y + 0.28, zp - 0.04), 0.09, gauge(legends[k], unit, lo, hi, majors, 0.09))
        b.box(gx - 0.015, y + 0.2, zp - 0.04, gx + 0.015, y + 0.24, zp, GUN)
        front_plate(b, zp, gx - 0.12, gx + 0.12, y + 0.38, y + 0.42, tag_tex(legends[k], 0.24, 0.04), d=0.003)
    front_plate(b, zp, x + 0.05, x + 0.29, y - 0.22, y - 0.12, tag_tex(tag, 0.24, 0.1), d=0.003)


def _h2_gas_panel(seed=437):
    b = C.Board(GPW, seed, face=0.0)
    live = {"gauges": [], "lamps": [], "digits": []}
    # ---------------------------------------------------------------- the analyzer cabinet
    b.box(0.1, 0.0, 0.45, 1.45, 0.1, 1.85, GUN)
    b.box(0.12, 0.1, 0.47, 1.43, 2.35, 1.83, [PANEL, PANEL, PANEL, PANEL, PANEL, None])
    b.core = len(b.still)
    zf = 0.455
    b.box(0.15, 0.14, zf, 1.4, 2.31, 0.47, [PANEL, None, PANEL, PANEL, PANEL, PANEL])
    for y in (0.4, 2.05):
        b.cyl((0.14, y - 0.07, zf + 0.005), (0.14, y + 0.07, zf + 0.005), 0.015, GUN, GUN, 6)
    b.box(1.31, 1.05, zf - 0.025, 1.35, 1.25, zf, sw("dgrey"))
    b.cyl((1.33, 1.15, zf - 0.025), (1.33, 1.15, zf - 0.05), 0.02, sw("dgrey"), sw("dgrey"), 6)
    for k in range(5):
        y = 0.24 + k * 0.06
        b.box(0.35, y, zf - 0.012, 1.2, y + 0.022, zf, sw("dgrey"))
    b.box(0.3, 1.78, zf - 0.025, 1.1, 2.02, zf, BLACK)
    live["digits"].append({"name": "h2_purity", "c": _r(0.7, 1.9, zf - 0.027), "w": 0.72, "h": 0.18})
    front_plate(b, zf, 0.4, 1.0, 1.7, 1.76, tag_tex("H2 PURITY  %", 0.6, 0.06), d=0.003)
    for k, (name, legend, unit, lo, hi, majors) in enumerate((("CASING PRESS", "CASING PRESS", "PSIG", 0, 100, 6),
                                                              ("DEW POINT", "DEW POINT", "°F", -40, 80, 7))):
        gx = 0.45 + k * 0.6
        gauge_z(b, live, name, (gx, 1.38, zf), 0.15, gauge(legend, unit, lo, hi, majors, 0.15))
        front_plate(b, zf, gx - 0.25, gx + 0.25, 1.12, 1.18, tag_tex(legend, 0.5, 0.06), d=0.003)
    for k, (name, legend) in enumerate((("h2_pur_lo", "PURITY LOW"), ("h2_press_lo", "CASING PRESS LOW"))):
        gx = 0.45 + k * 0.6
        lamp_z(b, live, name, gx, 0.85, zf, "amber", 0.035)
        front_plate(b, zf, gx - 0.25, gx + 0.25, 0.71, 0.76, tag_tex(legend, 0.5, 0.05), d=0.003)
    front_plate(b, zf, 0.25, 1.3, 2.12, 2.26, tag_tex("GENERATOR GAS|ANALYZER CABINET", 1.05, 0.14), d=0.004)
    # Its sides' louvres, the nameplate on its left, the ground lug.
    for (xs, s_) in ((0.12, -1), (1.43, 1)):
        for y0 in (0.3, 1.9):
            for k in range(4):
                y = y0 + k * 0.06
                b.box(min(xs, xs + s_ * 0.012), y, 0.75, max(xs, xs + s_ * 0.012), y + 0.022, 1.55, sw("dgrey"))
    side_plate(b, -1, 0.12, 0.85, 1.45, 1.35, 1.6, tag_tex("GAS ANALYZER|H2 PURITY - DEW POINT|CASING PRESSURE", 0.6, 0.25), d=0.004)
    b.box(0.1, 0.12, 1.6, 0.12, 0.2, 1.68, "copper")
    run(b, [(0.11, 0.16, 1.64), (0.11, 0.16, 1.9), (0.11, -0.05, 1.9)], 0.008, "copper", sides=4)
    # The sample tubing up out of the floor into its side; its vent and conduit up out of its top.
    for k in range(3):
        z = 0.8 + k * 0.1
        run(b, [(1.5, -0.05, z), (1.5, 0.9 + k * 0.08, z), (1.43, 0.9 + k * 0.08, z)], 0.012, SS, sides=4)
    b.box(1.43, 0.85, 0.74, 1.47, 1.15, 1.06, GUN)
    b.cyl((0.4, 2.35, 1.4), (0.4, 2.95, 1.4), 0.035, STEEL, None, 6)
    b.cyl((0.4, 2.9, 1.4), (0.4, 2.94, 1.4), 0.07, "flange", MID, 8)
    b.cyl((0.4, 2.35, 1.4), (0.4, 2.4, 1.4), 0.08, GUN, None, 8)
    b.cyl((1.15, 2.35, 1.55), (1.15, 2.98, 1.55), 0.03, BLACK, None, 6)
    # ---------------------------------------------------------------- the hydrogen dryer
    b.box(1.52, 0.0, 0.9, 2.2, 0.08, 1.75, GUN)
    for k, tx in enumerate((1.69, 2.03)):
        rev(b, (tx, 0.08, 1.35), (tx, 1.88, 1.35), [(0, 0.06), (0.1, 0.12), (1.66, 0.12), (1.8, 0.05)], SS, SS, SS, 14)
        b.cyl((tx, 1.88, 1.35), (tx, 1.95, 1.35), 0.025, SS, None, 6)
        b.box(tx - 0.06, 0.5, 1.17, tx + 0.06, 0.66, 1.25, GUN)
        run(b, [(tx, 0.5, 1.21), (tx, 0.08, 1.21)], 0.015, BLACK, sides=4)
    run(b, [(1.69, 1.95, 1.35), (2.03, 1.95, 1.35)], 0.03, SS, sides=6)
    b.cyl((1.86, 1.95, 1.35), (1.86, 2.06, 1.35), 0.06, GUN, GUN, 10)
    b.box(1.84, 2.06, 1.1, 1.88, 2.09, 1.36, sw("red"))
    b.box(1.6, 1.32, 1.21, 2.12, 1.42, 1.23, GUN)
    front_plate(b, 1.21, 1.62, 2.1, 1.33, 1.41, tag_tex("H2 DRYER", 0.48, 0.08), d=0.003)
    motor_h(b, (1.58, 0.24, 1.0), "x", 0.26, 0.1)
    b.cyl((1.53, 0.24, 1.0), (1.58, 0.24, 1.0), 0.11, GUN, GUN, 10)
    run(b, [(1.55, 0.33, 1.0), (1.55, 0.4, 1.0), (1.55, 0.4, 1.35), (1.57, 0.4, 1.35)], 0.025, SS, sides=6)
    run(b, [(1.86, -0.05, 1.68), (1.86, 2.0, 1.68), (1.86, 2.0, 1.4)], 0.025, SS, joints=SS, sides=6)
    run(b, [(2.12, -0.05, 1.6), (2.12, 0.3, 1.6), (2.12, 0.3, 1.35)], 0.025, SS, joints=SS, sides=6)
    # ---------------------------------------------------------------- the cylinder rack and the regulator panel
    b.box(2.25, 0.0, 1.0, 3.97, 0.04, 1.9, STEEL)
    for x in (2.27, 3.95):
        b.box(x - 0.03, 0.04, 1.72, x + 0.03, 2.75, 1.78, GUN)
    for y in (0.85, 1.35):
        b.box(2.3, y, 1.66, 3.92, y + 0.05, 1.7, GUN)
        b.cyl((2.3, y + 0.05, 1.22), (3.92, y + 0.05, 1.22), 0.008, DARK, DARK, 4)
        for x in (2.3, 3.92):
            b.box(x - 0.01, y + 0.03, 1.22, x + 0.01, y + 0.07, 1.66, DARK)
    b.box(2.3, 1.95, 1.78, 3.92, 2.75, 1.8, [SS, None, SS, SS, SS, SS])
    for k, x in enumerate(CYL_X):
        h2 = k < 4
        body = sw("red") if h2 else sw("mgrey")
        rev(b, (x, 0.04, 1.35), (x, 1.62, 1.35), [(0, 0.1), (0.04, 0.12), (1.36, 0.12), (1.5, 0.07), (1.58, 0.045)], body, None, body, 12)
        rev(b, (x, 1.42, 1.35), (x, 1.5, 1.35), [(0, 0.115), (0.08, 0.075)], sw("white") if h2 else sw("dgrey"), None, None, 12)
        b.cyl((x, 1.62, 1.35), (x, 1.75, 1.35), 0.03, "copper", "copper", 8)
        handwheel(b, (x, 1.77, 1.35), "y", 0.04, BLACK)
        run(b, [(x, 1.7, 1.38), (x, 1.7, 1.52), (x + 0.05, 1.78, 1.6), (x, 1.85, 1.68), (x, 1.85, 1.72)], 0.012, "copper", sides=4)
    for (xa, xb, xr) in ((2.36, 3.36, 2.95), (3.48, 3.92, 3.62)):
        run(b, [(xa, 1.85, 1.72), (xb, 1.85, 1.72)], 0.022, "copper", sides=6)
        run(b, [(xr, 1.85, 1.72), (xr, 2.16, 1.72)], 0.02, "copper", sides=6)
    front_plate(b, 1.7, 2.4, 2.8, 1.9, 1.96, tag_tex("H2 MANIFOLD", 0.4, 0.06), d=0.003)
    front_plate(b, 1.7, 3.5, 3.9, 1.9, 1.96, tag_tex("CO2 MANIFOLD", 0.4, 0.06), d=0.003)
    for k, x in enumerate(CYL_X):
        b.cyl((x, 1.85, 1.72), (x, 1.85, 1.64), 0.03, "copper", "copper", 6)
        b.box(x - 0.005, 1.84, 1.6, x + 0.005, 1.86, 1.64, sw("red"))
    _regulator(b, live, 2.95, 2.2, 1.78, ("H2 BOTTLE", "H2 REG OUT"), ("H2 BOTTLE", "H2 OUTLET"),
               (("PSIG", 0, 3000, 7), ("PSIG", 0, 150, 7)), "H2 SUPPLY|REG")
    _regulator(b, live, 3.62, 2.2, 1.78, ("CO2 BOTTLE", "CO2 REG OUT"), ("CO2 BOTTLE", "CO2 OUTLET"),
               (("PSIG", 0, 1500, 7), ("PSIG", 0, 150, 7)), "CO2 PURGE|REG")
    for xr in (2.95, 3.62):
        run(b, [(xr, 2.2, 1.8), (xr, 2.2, 1.9), (xr, -0.05, 1.9)], 0.025, "copper", joints="copper", sides=6)
        b.cyl((xr, 1.2, 1.9), (xr, 1.3, 1.9), 0.045, GUN, GUN, 8)
        b.box(xr - 0.01, 1.28, 1.9, xr + 0.01, 1.31, 1.99, sw("red"))
    front_plate(b, 1.78, 2.7, 3.88, 2.645, 2.74, tag_tex("H2 / CO2 SUPPLY MANIFOLD", 1.18, 0.095), d=0.004)
    b.box(2.08, 2.3, 1.7, 2.52, 2.74, 1.72, GUN)
    front_plate(b, 1.7, 2.1, 2.5, 2.32, 2.72, plate("DANGER|HYDROGEN|FLAMMABLE GAS|NO SMOKING", 0.4, 0.4, "red"), d=0.004)
    live["mats"] = {"needle": sw("red")}
    b.live = live
    on_floor(b)
    return b


BOARDS = {"condenser": ("Condenser", CW, CD, CT, condenser),
          "condenser_shell": ("Condenser Shell", CW, CD, CT, condenser),
          "condenser_hotwell": ("Condenser Hotwell", CW, CD, CT, hotwell),
          "condensate_pump": ("Condensate Pump", CPW, CPD, CPT, condensate_pump),
          "main_feed_pump": ("Main Feed Pump", MFW, MFD, MFT, main_feed_pump),
          "lube_oil_reservoir": ("Lube Oil Reservoir", LOR_W, LOR_D, LOR_T, lube_oil_reservoir),
          "ehc_unit": ("EHC Fluid Supply Unit", 3, 5, 3, ehc_unit),
          "seal_oil_unit": ("Seal Oil Unit", SOW, SOD, SOT, seal_oil_unit),
          "h2_gas_panel": ("Generator Gas Control Panel", 4, 2, 3, h2_gas_panel)}
