"""The medium plant's main transformer yard and 345 kV switchyard (Justin, 18 October 2026: "Do the transformer yard,
make it functional as well"; design approved after round 2: claude/jugcraft-transformer-yard-proposal.md). Every piece
is drawing operations (tools/control_boards.py's Board: boxes, oriented boxes, quads, cylinders) in the equipment
sheet's materials, plus the yard's own legend atlas (textures/entity/yard_legends.png, 512 px a block: gauge faces,
engraved aluminium tags, painted stencils). What is previewed is what the game draws.

Real-plant basis (a Westinghouse four-loop unit's generator step-up bank, as at Vogtle 1/2 and similar):
- Three single-phase GSU transformers, 345/sqrt(3) kV : 22 kV, about 400 MVA each, OFAF (forced oil, forced air),
  plus an installed spare; core-form, 0.4 % per-unit losses; about 268 t each, 62 000 L of mineral oil.
- Accessories per IEEE C57.12.10 / C57.91 practice: conservator with rubber bag, gas-accumulation (Buchholz) relay
  with shut-off valves either side, desiccant breather with oil cup, magnetic oil level gauge (alarm on low), pressure
  relief device with directional shield and a discharge pipe to the pit, sudden pressure relay, top-oil temperature
  indicator (OTI) and winding hot-spot indicator (WTI) with red drag hands, drain / filter-press / sampling valves,
  ground pads, jacking pads, lifting lugs, manholes.
- HV bushing: 345 kV oil-impregnated paper condenser bushing, brown glazed porcelain, oil sight glass in the head,
  CT turret; H0 neutral bushing solidly grounded; LV X1 / X2 bushings inside the isolated phase bus's flange box.
- Cooling: four OFAF unit coolers on the back, each an oil pump (motor, terminal box, flow indicator) on the bottom
  header and two fans on the finned cooler, isolation valves on both headers. Groups start in stages: group 1 at 65 C
  top oil / 75 C winding, group 2 at 75 / 85 C. Alarms: top oil 95 C, winding 110 C; trips 105 / 130 C. At full load
  top oil about 71 C, winding hot spot about 84 C.
- Fire protection (the bay): water spray (deluge) ring at two levels on pipe stanchions from the pit floor.

Frame: the equipment's own, x across the front (blocks), y up from the floor, z away from the front. The front (-z)
is the side a player walks along: control cabinet, valves, ladder, nameplate, stencil. HV bushing near the -x end
(toward the switchyard), LV bushings near the +x end (the bus duct), coolers along the back (+z)."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import control_boards as CB
from control_boards import Board, sw, pic

FONT_DIR = "/usr/share/fonts/truetype/dejavu/"
PXB = 512                       # legend atlas pixels a block
ATLAS_W = 2048                  # 4 blocks wide (no single legend wider)
LEGENDS = "yard_legends"        # the atlas's material name: [LEGENDS, u0, v0, u1, v1] over the whole atlas

GSU_SIZE = (9, 8, 13)           # wide (x), deep (z), tall


def _font(px, bold=True, cond=True):
    name = ("DejaVuSansCondensed" if cond else "DejaVuSans") + ("-Bold" if bold else "") + ".ttf"
    return ImageFont.truetype(FONT_DIR + name, max(6, int(px)))


# ---------------------------------------------------------------- the legend atlas (shelf packed, 512 px a block)

class Atlas:
    def __init__(self):
        self.items = []         # (key, image)
        self.at = {}            # key -> (x, y, w, h) px, after pack()

    def add(self, key, img):
        if key not in dict(self.items):
            assert img.width <= ATLAS_W, key
            self.items.append((key, img))
        return key

    def pack(self):
        x = y = row = 0
        for key, img in sorted(self.items, key=lambda kv: -kv[1].height):
            if x + img.width > ATLAS_W:
                x, y, row = 0, y + row + 2, 0
            self.at[key] = (x, y, img.width, img.height)
            x += img.width + 2
            row = max(row, img.height)
        h = 1
        while h < y + row:
            h *= 2
        self.image = Image.new("RGBA", (ATLAS_W, h), (0, 0, 0, 0))
        for key, img in self.items:
            ax, ay, _, _ = self.at[key]
            self.image.paste(img, (ax, ay))
        self.h = h

    def mat(self, key):
        """A placeholder for the legend's place, filled in by resolve() once the atlas is packed."""
        return ["@" + key]

    def region(self, key, inset=0.5):
        x, y, w, h = self.at[key]
        return [LEGENDS, (x + inset) / ATLAS_W, (y + inset) / self.h, (x + w - inset) / ATLAS_W, (y + h - inset) / self.h]

    def resolve(self, obj):
        """The ops with every legend placeholder replaced by its atlas region."""
        if isinstance(obj, list):
            if len(obj) == 1 and isinstance(obj[0], str) and obj[0].startswith("@"):
                return self.region(obj[0][1:])
            return [self.resolve(o) for o in obj]
        if isinstance(obj, dict):
            return {k: self.resolve(v) for k, v in obj.items()}
        return obj


ATLAS = Atlas()


def tag_img(text, w_m, h_m=0.045):
    """An engraved aluminium tag: brushed metal, dark letters, a rivet at each end (no white plate)."""
    w, h = int(w_m * PXB), int(h_m * PXB)
    rng = np.random.default_rng(len(text) * 7 + w)
    a = np.ones((h, w, 3)) * np.array([176, 180, 184], float)
    a += (rng.normal(0, 1, (1, w, 1)) * 6)          # brushing along the tag
    a += rng.normal(0, 2, (h, w, 1))
    im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, w - 1, h - 1], outline=(120, 124, 128), width=2)
    r = h * 0.16
    for cx in (h * 0.42, w - h * 0.42):
        d.ellipse([cx - r, h / 2 - r, cx + r, h / 2 + r], fill=(130, 134, 138), outline=(96, 98, 100))
    fs = h * 0.62
    while d.textlength(text, font=_font(fs)) > w - h * 1.4 and fs > 6:
        fs -= 1
    d.text((w / 2, h / 2), text, font=_font(fs), fill=(36, 38, 42), anchor="mm")
    return im


def stencil_img(text, h_m, colour=(30, 30, 32)):
    """Painted stencil letters, cut out (transparent between them): no plate, no edge."""
    h = int(h_m * PXB)
    f = _font(h * 0.9)
    tmp = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    w = int(tmp.textlength(text, font=f) + h * 0.2)
    w = min(w, ATLAS_W)
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.text((w / 2, h / 2), text, font=f, fill=colour + (255,), anchor="mm")
    # stencil bridges: a thin gap through each letter's middle, as cut stencils have
    for x in range(0, w, max(2, int(h * 0.55))):
        pass
    return im


def dial_img(lo, hi, label, unit, red=None, r_m=0.15, start=225, sweep=270, minor=4, red_below=None, green=None):
    """A gauge's printed face (round, transparent outside): scale, numbers, red band, legend. No needle."""
    s = int(2 * r_m * PXB)
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c, r = s / 2, s / 2 - 1
    d.ellipse([c - r, c - r, c + r, c + r], fill=(244, 244, 236, 255))
    P = lambda t, k: (c + math.cos(math.radians(t)) * k, c - math.sin(math.radians(t)) * k)
    if red is not None:
        f0 = (red - lo) / (hi - lo)
        d.arc([c - r * .86, c - r * .86, c + r * .86, c + r * .86], -(start - sweep * f0), -(start - sweep), fill=(200, 30, 30), width=max(2, int(r * .09)))
    if red_below is not None:
        f1 = (red_below - lo) / (hi - lo)
        d.arc([c - r * .86, c - r * .86, c + r * .86, c + r * .86], -start, -(start - sweep * f1), fill=(200, 30, 30), width=max(2, int(r * .09)))
    if green is not None:
        g0, g1 = ((v - lo) / (hi - lo) for v in green)
        d.arc([c - r * .86, c - r * .86, c + r * .86, c + r * .86], -(start - sweep * g0), -(start - sweep * g1), fill=(40, 150, 60), width=max(2, int(r * .09)))
    n = 10
    for i in range(n * minor // 2 + 1):
        f = i / (n * minor // 2)
        t = start - sweep * f
        major = i % (minor // 2) == 0
        d.line([P(t, r * (.74 if major else .82)), P(t, r * .94)], fill=(20, 20, 20), width=max(1, int(r * (.035 if major else .02))))
    for i in range(0, n + 1, 2):
        t = start - sweep * i / n
        v = lo + (hi - lo) * i / n
        d.text(P(t, r * .56), f"{v:g}", font=_font(r * .17), fill=(20, 20, 20), anchor="mm")
    d.text((c, c + r * .3), label, font=_font(r * .14), fill=(20, 20, 20), anchor="mm")
    if unit:
        d.text((c, c + r * .5), unit, font=_font(r * .14), fill=(20, 20, 20), anchor="mm")
    return im


def plate_img(lines, w_m, h_m):
    """The transformer's rating plate: etched stainless, black-filled letters."""
    w, h = int(w_m * PXB), int(h_m * PXB)
    rng = np.random.default_rng(3)
    a = np.ones((h, w, 3)) * np.array([190, 192, 194], float) + rng.normal(0, 1, (1, w, 1)) * 5
    im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, w - 2, h - 2], outline=(110, 112, 114), width=2)
    for x, y in ((8, 8), (w - 9, 8), (8, h - 9), (w - 9, h - 9)):
        d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(120, 122, 124))
    y = h * 0.08
    for i, ln in enumerate(lines):
        fs = h * (0.11 if i == 0 else 0.075)
        while d.textlength(ln, font=_font(fs, i == 0)) > w * 0.9:
            fs -= 1
        d.text((w * 0.06, y), ln, font=_font(fs, i == 0), fill=(26, 26, 28))
        y += fs * 1.35
    return im


# ---------------------------------------------------------------- shapes on a Board (any part, any frame)

GREY = "paint_grey"
DARK = sw("dgrey")
BLACK = sw("black")
ALU = "stainless"
COPPER = "copper"
PORC = "glaze_brown"
GALV = "carbon_steel"
YELLOW = "paint_yellow"


def _rnd(v):
    return [round(float(a), 4) for a in v]


AXES = {"x": (1, 0, 0), "-x": (-1, 0, 0), "y": (0, 1, 0), "-y": (0, -1, 0), "z": (0, 0, 1), "-z": (0, 0, -1)}


def _v(a):
    return np.array(AXES[a] if isinstance(a, str) else a, float)


def cyl(b, a, c, r, mat, cap=None, sides=16, lst=None):
    b.cyl(tuple(_rnd(a)), tuple(_rnd(c)), r, mat, cap if cap is not None else mat, sides, lst)


def rod(b, a, c, r, mat, sides=8, lst=None):
    cyl(b, a, c, r, mat, mat, sides, lst)


def path(b, pts, r, mat, sides=10):
    pts = [np.array(p, float) for p in pts]
    for p, q in zip(pts, pts[1:]):
        d = q - p
        L = np.linalg.norm(d)
        e = d / L * r * 0.98           # each run reaches into the elbow so the bends are closed
        rod(b, p - (e if p is not pts[0] else 0), q + (e if q is not pts[-1] else 0), r, mat, sides)
    for p in pts[1:-1]:
        rod(b, p - [0, r * 1.05, 0], p + [0, r * 1.05, 0], r * 1.06, mat, sides)


def bar(b, a, c, w, mat, h=None, lst=None):
    """A square bar (a strut, a rung, a brace) from a to c, any direction."""
    a, c = np.array(a, float), np.array(c, float)
    d = c - a
    L = np.linalg.norm(d)
    if L < 1e-6:
        return
    u = d / L
    ref = np.array([0, 1.0, 0]) if abs(u[1]) < 0.9 else np.array([1.0, 0, 0])
    v = np.cross(ref, u)
    v /= np.linalg.norm(v)
    b.obox(tuple(_rnd((a + c) / 2)), tuple(_rnd(u)), tuple(_rnd(v)), round(L / 2, 4), round(w / 2, 4), round((h or w) / 2, 4), mat, lst)


def ring(b, c, axis, R, t, mat, n=16):
    """A torus as n short bars (a corona ring, a handwheel's rim, a fan ring)."""
    c, ax = np.array(c, float), _v(axis)
    ref = np.array([0, 1.0, 0]) if abs(ax[1]) < 0.9 else np.array([1.0, 0, 0])
    e1 = np.cross(ax, ref); e1 /= np.linalg.norm(e1)
    e2 = np.cross(ax, e1)
    pts = [c + R * (math.cos(2 * math.pi * k / n) * e1 + math.sin(2 * math.pi * k / n) * e2) for k in range(n + 1)]
    for p, q in zip(pts, pts[1:]):
        dd = q - p
        rod(b, p - dd * 0.04, q + dd * 0.04, t / 2, mat, 6)


def flange(b, c, axis, r, th, mat, bolts=8, br=None):
    """A flange: the disc and its bolt heads round the edge (on the side toward axis)."""
    c, ax = np.array(c, float), _v(axis)
    cyl(b, c - ax * th / 2, c + ax * th / 2, r, mat, sides=20)
    ref = np.array([0, 1.0, 0]) if abs(ax[1]) < 0.9 else np.array([1.0, 0, 0])
    e1 = np.cross(ax, ref); e1 /= np.linalg.norm(e1)
    e2 = np.cross(ax, e1)
    br = br or r * 0.78
    hb = max(0.012, r * 0.09)
    for k in range(bolts):
        t = 2 * math.pi * (k + 0.5) / bolts
        p = c + br * (math.cos(t) * e1 + math.sin(t) * e2)
        cyl(b, p + ax * th / 2, p + ax * (th / 2 + hb * 0.7), hb, BLACK, sides=6)
        cyl(b, p - ax * th / 2, p - ax * (th / 2 + hb * 0.7), hb, BLACK, sides=6)


def handwheel(b, c, axis, R, mat=sw("red")):
    c, ax = np.array(c, float), _v(axis)
    ring(b, c, ax, R, R * 0.14, mat, 14)
    ref = np.array([0, 1.0, 0]) if abs(ax[1]) < 0.9 else np.array([1.0, 0, 0])
    e1 = np.cross(ax, ref); e1 /= np.linalg.norm(e1)
    e2 = np.cross(ax, e1)
    for k in range(3):
        t = 2 * math.pi * k / 3 + 0.3
        rod(b, c, c + R * (math.cos(t) * e1 + math.sin(t) * e2), R * 0.05, mat, 6)
    cyl(b, c - ax * R * 0.12, c + ax * R * 0.12, R * 0.18, mat, sides=8)


def gate_valve(b, c, run, stem, size=0.1, mat=GREY, wheel=sw("red")):
    """A flanged gate valve on a pipe running along run: body, flanges with bolts, bonnet, yoke, stem, handwheel."""
    c, run, st = np.array(c, float), _v(run), _v(stem)
    L = size * 2.4
    for s in (-1, 1):
        flange(b, c + run * s * L / 2, run, size * 1.25, size * 0.25, mat, 6)
    cyl(b, c - run * L * 0.4, c + run * L * 0.4, size * 0.9, mat, sides=12)
    cyl(b, c, c + st * size * 1.6, size * 0.7, mat, sides=10)                     # bonnet
    flange(b, c + st * size * 1.6, st, size * 0.9, size * 0.18, mat, 6)
    side = np.cross(st, run)
    for s in (-1, 1):                                                             # the yoke's two arms
        bar(b, c + st * size * 1.7 + side * s * size * 0.45, c + st * size * 3.0 + side * s * size * 0.3, size * 0.18, mat)
    bar(b, c + st * size * 3.0 - side * size * 0.38, c + st * size * 3.0 + side * size * 0.38, size * 0.2, mat, h=size * 0.26)
    rod(b, c + st * size * 1.6, c + st * size * 3.6, size * 0.08, ALU)            # the stem
    handwheel(b, c + st * size * 3.3, st, size * 1.2, wheel)


def ball_valve(b, c, run, lever, size=0.04, mat=GREY):
    """A small ball valve (a sampling point): body, its lever, a cap on the outlet."""
    c, run, lv = np.array(c, float), _v(run), _v(lever)
    cyl(b, c - run * size * 1.6, c + run * size * 1.6, size, mat, sides=8)
    cyl(b, c, c + lv * size * 1.4, size * 0.4, mat, sides=6)
    side = np.cross(lv, run)
    bar(b, c + lv * size * 1.5, c + lv * size * 1.5 + side * size * 4, size * 0.5, sw("red"), size * 0.25)
    cyl(b, c + run * size * 1.6, c + run * size * 2.3, size * 0.8, sw("dgrey"), sides=6)


def face_quad(b, c, normal, w, h, mat, lst=None, up=(0, 1, 0)):
    """A picture on a face toward normal (a gauge face, a tag, a stencil), the right way up from in front."""
    c, n, up = np.array(c, float), _v(normal), np.array(up, float)
    up = up - n * (up @ n)
    up /= np.linalg.norm(up)
    right = np.cross(n, up)         # the viewer's right seen from in front, in the equipment frame (as the game shows it: from the front, +x is to the right)
    tl = c - right * w / 2 + up * h / 2
    bl = c - right * w / 2 - up * h / 2
    br = c + right * w / 2 - up * h / 2
    tr = c + right * w / 2 + up * h / 2
    b.quad(tuple(_rnd(tl)), tuple(_rnd(bl)), tuple(_rnd(br)), tuple(_rnd(tr)), mat, lst)


def gauge(b, c, normal, r, face_key, value, lo, hi, drag=None, start=225, sweep=270, channel=None, case=BLACK):
    """A round gauge: case and bezel standing out of its mounting, the printed face set back inside, a separate needle
    and hub in front of the face, a red drag hand (live: the needle and the drag hand follow channel)."""
    c, n = np.array(c, float), _v(normal)
    cyl(b, c, c + n * 0.03, r + 0.025, case, sides=20)                         # the case, its face end dark
    face_quad(b, c + n * 0.034, n, 2 * r, 2 * r, ATLAS.mat(face_key))          # the printed dial
    ring(b, c + n * 0.042, n, r + 0.012, 0.024, "stainless", 20)              # the bezel round the glass
    up = np.array([0, 1.0, 0]) - n * n[1]
    up /= np.linalg.norm(up)
    right = np.cross(n, up)
    for val, mat, wd, dz in ((drag, sw("red"), 0.008, 0.04), (value, BLACK, 0.011, 0.045)):
        if val is None:
            continue
        t = math.radians(start - sweep * max(0.0, min(1.0, (val - lo) / (hi - lo))))
        tip = c + n * dz + (math.cos(t) * right + math.sin(t) * up) * r * 0.84
        tail = c + n * dz - (math.cos(t) * right + math.sin(t) * up) * r * 0.16
        bar(b, tail, tip, wd, mat, h=0.004)
    cyl(b, c + n * 0.034, c + n * 0.052, r * 0.08, BLACK, sides=8)              # the hub
    if channel:
        b.live.append({"t": "needle", "c": _rnd(c + n * 0.045), "n": _rnd(n), "r": round(r * 0.84, 4), "lo": lo, "hi": hi,
                       "start": start, "sweep": sweep, "ch": channel, "drag": drag is not None})


def tag(b, c, normal, text, w=0.3, h=0.045, depth=0.004, lst=None):
    """An engraved aluminium tag riveted to the part it names (stands depth out)."""
    key = ATLAS.add(f"tag:{text}:{w}", tag_img(text, w, h))
    c, n = np.array(c, float), _v(normal)
    up = np.array([0, 1.0, 0]) - n * n[1]
    if np.linalg.norm(up) < 1e-6:
        up = np.array([0, 0, -1.0])
    up /= np.linalg.norm(up)
    right = np.cross(n, up)
    lo = c - right * w / 2 - up * h / 2
    hi = c + right * w / 2 + up * h / 2 + n * depth
    b.box(*_rnd(np.minimum(lo, hi)), *_rnd(np.maximum(lo, hi)), _faces_toward(n, ATLAS.mat(key), "stainless"), lst)


def _faces_toward(n, front, sides):
    """Six faces [-z, +z, -x, +x, +y, -y] with front on the one toward n."""
    i = {(0, 0, -1): 0, (0, 0, 1): 1, (-1, 0, 0): 2, (1, 0, 0): 3, (0, 1, 0): 4, (0, -1, 0): 5}[tuple(int(round(v)) for v in n)]
    out = [sides] * 6
    out[i] = front
    return out


def stencil(b, c, normal, text, h, colour=(30, 30, 32)):
    """Painted stencil letters on a big flat face (cut out, a hair off the face)."""
    img = stencil_img(text, h, colour)
    key = ATLAS.add(f"stencil:{text}:{h}", img)
    face_quad(b, c, normal, img.width / PXB, img.height / PXB, ATLAS.mat(key))


def lamp(b, c, normal, colour, lit, r=0.022):
    """A pilot light: chrome ring and a domed lens (lit: full bright)."""
    c, n = np.array(c, float), _v(normal)
    cyl(b, c, c + n * 0.012, r + 0.01, "stainless", sides=12)
    lens = CB.LENS[colour] if lit else CB.LENS["off_" + colour]
    cyl(b, c + n * 0.012, c + n * 0.03, r, lens, lens, 12, b.lit if lit else None)


def selector(b, c, normal, at_deg, r=0.03):
    c, n = np.array(c, float), _v(normal)
    cyl(b, c, c + n * 0.01, r * 1.25, sw("dgrey"), sides=12)
    up = np.array([0, 1.0, 0]) - n * n[1]
    up /= np.linalg.norm(up)
    right = np.cross(n, up)
    t = math.radians(at_deg)
    d = math.cos(t) * right + math.sin(t) * up
    bar(b, c + n * 0.03 - d * r, c + n * 0.03 + d * r, r * 0.55, BLACK, h=0.04)
    bar(b, c + n * 0.051 + d * r * 0.3, c + n * 0.051 + d * r * 0.95, r * 0.14, sw("white"), h=0.002)


# ---------------------------------------------------------------- the main transformer (one phase)

FACES = {}   # gauge faces registered once


def _faces():
    if FACES:
        return
    FACES["oti"] = ATLAS.add("dial:oti", dial_img(0, 150, "TOP OIL", "°C", 95, 0.15))
    FACES["wti"] = ATLAS.add("dial:wti", dial_img(0, 150, "WINDING", "°C", 110, 0.15))
    FACES["olg"] = ATLAS.add("dial:olg", dial_img(0, 100, "OIL LEVEL", "%", None, 0.12, start=180, sweep=180))
    FACES["flow"] = ATLAS.add("dial:flow", dial_img(0, 100, "OIL FLOW", "%", None, 0.06, start=180, sweep=180))
    FACES["ctr"] = ATLAS.add("dial:ctr", dial_img(0, 10, "OPS", "", None, 0.05, start=180, sweep=180))
    FACES["air"] = ATLAS.add("dial:air", dial_img(0, 1, "BUS AIR", "kPa", None, 0.05, start=180, sweep=180))
    FACES["plate"] = ATLAS.add("plate:gsu", plate_img(
        ["GENERATOR STEP-UP TRANSFORMER", "1 PHASE  60 Hz  OFAF  400 MVA  65 C RISE", "HV 345 000 / √3 V  BIL 1300 kV",
         "LV 22 000 V  BIL 150 kV", "IMPEDANCE 14.2 %  AT 400 MVA", "OIL 62 000 L  TOTAL 268 000 kg",
         "SERIAL 26-1187-C  YEAR 2026", "JUGCRAFT HEAVY ELECTRIC"], 0.62, 0.42))


# Layout (blocks)
TX0, TX1, TZ0, TZ1, TY0, TY1 = 0.55, 8.45, 1.6, 5.4, 0.62, 5.5      # the tank
COVER = TY1 + 0.16


def gsu(name="MT-1C", phase="C", spare=False, state=None):
    """One single-phase generator step-up transformer. state: oil, wdg, oil_max, wdg_max, level, fans (0..2 groups),
    pumps, flow, alarms (set of HI TEMP, BUCHHOLZ, PRD), running."""
    _faces()
    st = dict(oil=71, wdg=84, oil_max=78, wdg_max=92, level=62, fans=2, pumps=True, flow=96, alarms=(), running=True)
    st.update(state or {})
    b = Board(GSU_SIZE[0], 1187, face=0.0)
    b.live = []
    b.core = 0
    tank(b)
    b.core = len(b.still)
    cover_fittings(b)
    hv_bushing(b, spare)
    lv_side(b, spare)
    conservator(b, st)
    coolers(b, st)
    front_fittings(b, name, phase)
    cabinet(b, name, st)
    return b


def finished(b):
    """A board's ops with the legends placed (packs the atlas)."""
    ATLAS.pack()
    out = ATLAS.resolve(b.ops())
    out["live"] = ATLAS.resolve(b.live)
    return out


def tank(b):
    g = GREY
    # Skids (two I-beams on the plinth), jacking pads at the corners.
    for x in (1.5, 7.5):
        b.box(x - 0.18, 0.0, TZ0 - 0.2, x + 0.18, 0.06, TZ1 + 0.2, [g, g, g, g, g, None])
        b.box(x - 0.04, 0.06, TZ0 - 0.2, x + 0.04, 0.5, TZ1 + 0.2, [g, g, g, g, None, None])
        b.box(x - 0.18, 0.5, TZ0 - 0.2, x + 0.18, 0.56, TZ1 + 0.2, [g, g, g, g, g, g])
    b.box(TX0, 0.56, TZ0, TX1, TY0, TZ1, [g, g, g, g, None, g])                                 # bottom plate
    for x in (TX0 + 0.25, TX1 - 0.25):
        for z in (TZ0, TZ1):
            s = -1 if z == TZ0 else 1
            b.box(x - 0.2, 1.0, min(z, z + s * 0.25), x + 0.2, 1.22, max(z, z + s * 0.25), g)   # jacking pad
    # The tank wall, the bell's bolted flange low down, two stiffening bands.
    b.box(TX0, TY0, TZ0, TX1, TY1, TZ1, [g, g, g, g, None, None])
    for y0, y1, out in ((1.38, 1.5, 0.12), (2.9, 3.02, 0.1), (4.4, 4.52, 0.1)):
        b.box(TX0 - out, y0, TZ0 - out, TX1 + out, y1, TZ1 + out, g)
    for x in np.arange(TX0 + 0.1, TX1, 0.42):                                                   # flange bolts
        for z, s in ((TZ0 - 0.12, -1), (TZ1 + 0.12, 1)):
            cyl(b, (x, 1.5, z - s * 0.04), (x, 1.56, z - s * 0.04), 0.025, BLACK, sides=6)
    # Vertical stiffeners (channels) between the bands, on all four walls.
    for x in np.arange(TX0 + 0.55, TX1 - 0.3, 0.9):
        for z, s in ((TZ0, -1), (TZ1, 1)):
            for y0, y1 in ((1.5, 2.9), (3.02, 4.4), (4.52, TY1 - 0.06)):
                b.box(x - 0.06, y0, min(z, z + s * 0.1), x + 0.06, y1, max(z, z + s * 0.1), [g, g, g, g, None, None])
    for z in np.arange(TZ0 + 0.6, TZ1 - 0.3, 0.9):
        for x, s in ((TX0, -1), (TX1, 1)):
            for y0, y1 in ((1.5, 2.9), (3.02, 4.4), (4.52, TY1 - 0.06)):
                b.box(min(x, x + s * 0.1), y0, z - 0.06, max(x, x + s * 0.1), y1, z + 0.06, [g, g, g, g, None, None])
    # The cover with its flange and bolts.
    b.box(TX0 - 0.1, TY1, TZ0 - 0.1, TX1 + 0.1, TY1 + 0.1, TZ1 + 0.1, g)
    b.box(TX0, TY1 + 0.1, TZ0, TX1, COVER, TZ1, [g, g, g, g, g, None])
    for x in np.arange(TX0 + 0.1, TX1, 0.4):
        for z in (TZ0 - 0.05, TZ1 + 0.05):
            cyl(b, (x, TY1 + 0.1, z), (x, TY1 + 0.15, z), 0.022, BLACK, sides=6)
    for z in np.arange(TZ0 + 0.15, TZ1, 0.4):
        for x in (TX0 - 0.05, TX1 + 0.05):
            cyl(b, (x, TY1 + 0.1, z), (x, TY1 + 0.15, z), 0.022, BLACK, sides=6)
    # Lifting lugs at the four corners.
    for x in (TX0 + 0.25, TX1 - 0.35):
        for z, s in ((TZ0, -1), (TZ1, 1)):
            b.box(x - 0.2, TY1 - 0.7, min(z, z + s * 0.14), x + 0.2, TY1 - 0.06, max(z, z + s * 0.14), g)
            cyl(b, (x - 0.06, TY1 - 0.32, z + s * 0.07), (x + 0.06, TY1 - 0.32, z + s * 0.07), 0.07, sw("dgrey"), sides=10)


def cover_fittings(b):
    # Two manholes, bolted.
    for x in (3.0, 5.6):
        flange(b, (x, COVER + 0.04, 3.5), "y", 0.36, 0.08, GREY, 12)
        cyl(b, (x, COVER + 0.08, 3.5), (x, COVER + 0.14, 3.5), 0.3, GREY, sides=20)
    # Pressure relief device with its directional shield, discharge pipe down the back into the pit.
    px, pz = 6.4, 2.3
    cyl(b, (px, COVER, pz), (px, COVER + 0.2, pz), 0.18, GREY, sides=16)
    flange(b, (px, COVER + 0.22, pz), "y", 0.24, 0.05, GREY, 8)
    b.box(px - 0.3, COVER + 0.25, pz - 0.3, px + 0.3, COVER + 0.55, pz + 0.3, [YELLOW, YELLOW, YELLOW, None, YELLOW, YELLOW])
    path(b, [(px + 0.3, COVER + 0.4, pz), (TX1 + 0.35, COVER + 0.4, pz), (TX1 + 0.35, 0.3, pz), (TX1 + 0.35, 0.3, pz - 0.6)], 0.09, GREY)
    for x in (7.1, 7.9):                                   # its stanchions on the cover
        b.box(x - 0.04, COVER, pz - 0.04, x + 0.04, COVER + 0.31, pz + 0.04, [GREY, GREY, GREY, GREY, None, None])
    for y in (1.2, 2.6, 4.0):
        b.box(TX1 + 0.1, y - 0.05, pz - 0.05, TX1 + 0.27, y + 0.05, pz + 0.05, GREY)              # pipe clamps' brackets
    # Sudden pressure relay on the front wall near the bottom.
    b.box(4.98, 0.85, TZ0 - 0.26, 5.32, 1.2, TZ0, [GREY, None, GREY, GREY, GREY, GREY])
    tag(b, (5.15, 1.32, TZ0 - 0.004), "-z", "63 SPR", 0.2)


def hv_bushing(b, spare):
    """The 345 kV bushing on its CT turret, tilted 15 degrees toward -x (the switchyard)."""
    hx, hz = 1.7, 3.5
    cyl(b, (hx, COVER, hz), (hx, COVER + 0.6, hz), 0.62, GREY, sides=20)
    flange(b, (hx, COVER + 0.6, hz), "y", 0.72, 0.08, GREY, 16)
    a = np.array([-math.sin(math.radians(15)), math.cos(math.radians(15)), 0])
    base = np.array([hx, COVER + 0.64, hz])
    cyl(b, base, base + a * 0.5, 0.55, GREY, sides=20)                          # CT pocket
    flange(b, base + a * 0.52, a, 0.44, 0.06, ALU, 12)
    s0 = base + a * 0.58
    L = 4.8
    cyl(b, s0, s0 + a * L, 0.21, PORC, sides=16)
    for i in range(int(L / 0.16)):
        c = s0 + a * (0.16 * (i + 0.5))
        r = 0.46 if i % 2 == 0 else 0.37
        cyl(b, c - a * 0.02, c + a * 0.02, r, PORC, sides=20)
        cyl(b, c + a * 0.02, c + a * 0.05, (r + 0.21) / 2, PORC, sides=16)
    top = s0 + a * L
    cyl(b, top, top + a * 0.5, 0.29, ALU, sides=16)                             # the head with its oil sight glass
    cyl(b, top + a * 0.14, top + a * 0.26, 0.295, "glass", sides=16)
    cyl(b, top + a * 0.5, top + a * 0.85, 0.07, COPPER, sides=8)                # the terminal stud
    ring(b, top + a * 0.42, a, 0.55, 0.09, ALU, 20)                             # the corona ring
    b.hv_top = (top + a * 0.85).tolist()
    # Surge arrester on its bracket off the -x end, its counter and ground lead.
    ax, az = -0.25, 3.5
    b.box(-0.55, 4.7, az - 0.55, TX0 - 0.1, 4.86, az + 0.55, GREY)
    for z in (az - 0.45, az + 0.45):
        bar(b, (-0.45, 4.7, z), (TX0 - 0.1, 2.2, z), 0.08, GREY)
    cyl(b, (ax, 4.86, az), (ax, 5.0, az), 0.2, ALU, sides=16)
    for i in range(int(3.7 / 0.13)):
        y = 5.0 + 0.13 * (i + 0.5)
        r = 0.27 if i % 2 == 0 else 0.21
        cyl(b, (ax, y - 0.015, az), (ax, y + 0.015, az), r, "paint_grey", sides=16)
    cyl(b, (ax, 5.0, az), (ax, 8.7, az), 0.13, sw("mgrey"), sides=12)
    cyl(b, (ax, 8.7, az), (ax, 8.82, az), 0.16, ALU, sides=12)
    ring(b, (ax, 8.5, az), "y", 0.38, 0.05, ALU, 20)
    b.arr_top = [ax, 8.82, az]
    # The surge counter where it can be read from the ground, on a bracket off the tank's end, the ground lead
    # from the arrester's base down through it to the grid.
    b.box(TX0 - 0.42, 1.78, az - 0.12, TX0 - 0.3, 2.06, az + 0.12, GREY)
    b.box(TX0 - 0.3, 1.88, az - 0.03, TX0, 1.96, az + 0.03, GREY)
    gauge(b, (TX0 - 0.42, 1.92, az), "-x", 0.05, FACES["ctr"], 3, 0, 10, channel="arr_ops", case=GREY)
    # (clipped along the bracket's strut, not hanging free)
    s0, s1 = np.array([-0.45, 4.7, az + 0.45]), np.array([TX0 - 0.1, 2.2, az + 0.45])
    off = np.array([0, 0, 0.065])
    path(b, [(ax + 0.12, 4.92, az + 0.12), (ax + 0.12, 4.92, az + 0.515), s0 + off + [0, 0.1, 0], s1 + off + [-0.05, 0.05, 0],
             (TX0 - 0.36, 2.25, az + 0.515), (TX0 - 0.36, 2.25, az), (TX0 - 0.36, 2.06, az)], 0.02, COPPER)
    for t in (0.3, 0.65):
        p = s0 + (s1 - s0) * t
        b.box(*_rnd(p + [-0.03, -0.03, 0.03]), *_rnd(p + [0.03, 0.03, 0.1]), GALV)
    path(b, [(TX0 - 0.36, 1.78, az), (TX0 - 0.36, 0.3, az), (TX0 - 0.36, 0.02, az + 0.5)], 0.02, COPPER)
    if not spare:
        a2 = np.array(b.arr_top) - np.array(b.hv_top)
        pts = [np.array(b.hv_top) + a2 * t + np.array([0, -0.35 * 4 * t * (1 - t), 0]) for t in np.linspace(0, 1, 9)]
        for p, q in zip(pts, pts[1:]):
            rod(b, p, q, 0.03, ALU)


def lv_side(b, spare):
    """LV bushings X1, X2 inside the bus duct's flange box (rising to the duct); the H0 neutral bushing and its
    grounding strap."""
    lx, lz = 7.2, 3.5
    b.box(lx - 0.8, COVER, lz - 1.1, lx + 0.8, COVER + 0.7, lz + 1.1, ALU)
    for z in (lz - 0.5, lz + 0.5):
        cyl(b, (lx, COVER + 0.7, z), (lx, COVER + 0.8, z), 0.5, ALU, sides=20)
        if not spare:
            cyl(b, (lx, COVER + 0.8, z), (lx, YARD["branch_y"] - YARD["plinth"] + IPB_R, z), 0.46, ALU, sides=20)   # up into the bus branch
            for y in (COVER + 1.1, COVER + 1.25):
                ring(b, (lx, y, z), "y", 0.5, 0.05, ALU, 20)                       # its bellows
        else:
            cyl(b, (lx, COVER + 0.8, z), (lx, COVER + 0.9, z), 0.52, sw("dgrey"), sides=20)   # blanked
    nx, nz = 4.9, 4.7
    cyl(b, (nx, COVER, nz), (nx, COVER + 0.22, nz), 0.2, GREY, sides=12)
    for i in range(6):
        y = COVER + 0.22 + 0.13 * (i + 0.5)
        cyl(b, (nx, y - 0.015, nz), (nx, y + 0.015, nz), 0.17 if i % 2 == 0 else 0.14, PORC, sides=14)
    cyl(b, (nx, COVER + 0.22, nz), (nx, COVER + 1.05, nz), 0.08, PORC, sides=10)
    path(b, [(nx, COVER + 1.05, nz), (nx, COVER + 1.25, nz), (nx, COVER + 1.25, TZ1 + 0.3), (nx, 0.4, TZ1 + 0.3),
             (nx, 0.02, TZ1 + 0.7)], 0.03, COPPER)
    rod(b, (nx, COVER, TZ1 - 0.15), (nx, COVER + 0.95, TZ1 - 0.15), 0.035, GREY)            # its support post
    for i in range(3):
        y = COVER + 0.97 + 0.07 * i
        cyl(b, (nx, y, TZ1 - 0.15), (nx, y + 0.03, TZ1 - 0.15), 0.07, PORC, sides=12)         # and insulator
    rod(b, (nx, COVER + 0.95, TZ1 - 0.15), (nx, COVER + 1.22, TZ1 - 0.15), 0.035, PORC)
    for y in (1.2, 2.6, 4.0):
        b.box(nx - 0.04, y - 0.04, TZ1 + 0.1, nx + 0.04, y + 0.04, TZ1 + 0.27, GREY)
    # Ground pads at two corners, straps down into the grid.
    for x in (TX0 + 0.12, TX1 - 0.12):
        b.box(x - 0.1, 0.7, TZ0 - 0.03, x + 0.1, 0.85, TZ0, [COPPER, None, COPPER, COPPER, COPPER, COPPER])
        path(b, [(x, 0.72, TZ0 - 0.06), (x, 0.3, TZ0 - 0.06), (x, 0.02, TZ0 - 0.4)], 0.02, COPPER)


def conservator(b, st):
    """The conservator along the top at the back, its saddles, the magnetic oil level gauge on the -x end (angled
    down to the walkway), the Buchholz relay with shut-off valves, the breather piped down the front."""
    cx0, cx1, cy, cz = 1.2, 6.2, COVER + 1.55, 4.6
    cyl(b, (cx0, cy, cz), (cx1, cy, cz), 0.62, GREY, sides=24)
    for x, s in ((cx0, -1), (cx1, 1)):
        cyl(b, (x, cy, cz), (x + s * 0.12, cy, cz), 0.5, GREY, sides=24)
        cyl(b, (x + s * 0.12, cy, cz), (x + s * 0.2, cy, cz), 0.3, GREY, sides=20)
    for x in (cx0 + 0.7, cx1 - 0.7):
        b.box(x - 0.08, COVER, cz - 0.5, x + 0.08, cy - 0.5, cz + 0.5, [GREY, GREY, GREY, GREY, None, None])  # saddle
        b.box(x - 0.1, cy - 0.5, cz - 0.55, x + 0.1, cy - 0.42, cz + 0.55, GREY)
    # Oil level gauge on the -x end (face toward -x, tilted down 30 degrees).
    n = np.array([-math.cos(math.radians(30)), -math.sin(math.radians(30)), 0])
    c = np.array([cx0 - 0.22, cy - 0.05, cz])
    cyl(b, c + n * -0.02, c, 0.13, GREY, sides=16)
    gauge(b, c, n, 0.12, FACES["olg"], st["level"], 0, 100, start=180, sweep=180, channel="oil_level")
    # Buchholz relay in the pipe from the conservator to the cover, a valve each side.
    bx = 4.2
    path(b, [(bx, cy - 0.55, cz), (bx, cy - 0.85, cz), (bx, cy - 0.85, 3.3), (bx, COVER, 3.3)], 0.07, GREY)
    b.box(bx - 0.18, cy - 1.05, 3.6, bx + 0.18, cy - 0.65, 4.05, GREY)
    for z in (3.7, 3.95):
        cyl(b, (bx + 0.18, cy - 0.85, z), (bx + 0.2, cy - 0.85, z), 0.05, "glass", sides=10)
    ball_valve(b, (bx, cy - 0.85, 4.3), "z", "y", 0.05)
    ball_valve(b, (bx, cy - 0.85, 3.4), "z", "y", 0.05)
    tag(b, (bx, cy - 0.85, 3.58), "-z", "BUCHHOLZ 63", 0.26, 0.04)
    # Breather: pipe from the conservator's top over the front edge and down to the silica gel canister.
    path(b, [(cx1 - 0.18, cy - 0.6, cz - 0.15), (cx1 - 0.18, COVER + 0.04, cz - 0.15), (cx1 - 0.18, COVER + 0.04, TZ0 + 0.06),
             (cx1 - 0.18, COVER + 0.04, TZ0 - 0.3), (cx1 - 0.18, 2.05, TZ0 - 0.3)], 0.03, GREY)
    for y in (2.6, 4.0):
        b.box(cx1 - 0.22, y - 0.04, TZ0 - 0.26, cx1 - 0.14, y + 0.04, TZ0, GREY)
    bx2 = cx1 - 0.18
    cyl(b, (bx2, 1.95, TZ0 - 0.3), (bx2, 2.05, TZ0 - 0.3), 0.13, GREY, sides=14)
    cyl(b, (bx2, 1.4, TZ0 - 0.3), (bx2, 1.95, TZ0 - 0.3), 0.11, "glass", sides=14)
    cyl(b, (bx2, 1.42, TZ0 - 0.3), (bx2, 1.85, TZ0 - 0.3), 0.095, sw("orange"), sides=12)    # desiccant (orange when dry)
    cyl(b, (bx2, 1.3, TZ0 - 0.3), (bx2, 1.4, TZ0 - 0.3), 0.13, GREY, sides=14)
    cyl(b, (bx2, 1.15, TZ0 - 0.3), (bx2, 1.3, TZ0 - 0.3), 0.09, "glass", sides=12)             # oil cup
    b.box(bx2 - 0.05, 1.6, TZ0 - 0.17, bx2 + 0.05, 1.7, TZ0, GREY)


# The fans' rotation in game (tools/yard_sites.py draws each step of it): degrees added to every fan of a channel.
FAN_PHASE = {}


def fan(b, fc, angle, ch):
    """A cooling fan blowing out along +z from fc: shroud, guard (rings and spokes), motor, five blades (live: turns)."""
    angle = angle + math.radians(FAN_PHASE.get(ch, 0.0))
    cyl(b, fc, fc + [0, 0, 0.32], 0.58, GREY, sides=20)
    ring(b, fc + [0, 0, 0.33], "z", 0.58, 0.04, GREY, 20)
    for R in (0.18, 0.32, 0.46):
        ring(b, fc + [0, 0, 0.36], "z", R, 0.012, BLACK, 16)
    for k in range(8):
        t = k * math.pi / 4
        rod(b, fc + [0, 0, 0.36], fc + [math.cos(t) * 0.58, math.sin(t) * 0.58, 0.36], 0.006, BLACK, 4)
    cyl(b, fc + [0, 0, 0.2], fc + [0, 0, 0.34], 0.12, sw("dgrey"), sides=12)        # motor
    for k in range(5):
        t = angle + k * 2 * math.pi / 5
        d = np.array([math.cos(t), math.sin(t), 0])
        b.obox(tuple(_rnd(fc + [0, 0, 0.15 + k * 0.009] + d * 0.3)), tuple(_rnd(d)), (0, 0, 1.0), 0.24, 0.004, 0.07, BLACK)
    b.live.append({"t": "fan", "c": _rnd(fc + [0, 0, 0.17]), "axis": [0, 0, 1], "r": 0.54, "ch": ch})


def coolers(b, st):
    """Four OFAF unit coolers on the back: the top header off the tank with its butterfly valve, the finned cooler,
    two fans each blowing out, and under each cooler its return: down from the cooler's bottom box through the oil
    pump (motor with fins and fan cowl, terminal box, conduit to the pad) and back into the tank low down, the flow
    indicator on the drop."""
    zt = TZ1
    for i, xc in enumerate((1.4, 3.4, 5.4, 7.4)):
        group = i // 2 + 1
        z0, z1 = zt + 0.75, zt + 1.75                    # the cooler's body
        zm = (z0 + z1) / 2
        # Top header: flanged off the tank, a butterfly valve with its lever.
        y = 4.6
        rod(b, (xc, y, zt), (xc, y, z0), 0.13, GREY, 12)
        flange(b, (xc, y, zt + 0.12), "z", 0.22, 0.05, GREY, 8)
        flange(b, (xc, y, z0 - 0.08), "z", 0.22, 0.05, GREY, 8)
        cyl(b, (xc, y, zt + 0.32), (xc, y, zt + 0.5), 0.2, GREY, sides=14)
        rod(b, (xc, y + 0.2, zt + 0.41), (xc, y + 0.42, zt + 0.41), 0.025, GREY, 6)
        bar(b, (xc - 0.18, y + 0.44, zt + 0.41), (xc + 0.18, y + 0.44, zt + 0.41), 0.035, sw("red"), 0.02)
        # The cooler: bottom and top boxes, the fin block between, its back face of fins.
        b.box(xc - 0.85, 1.3, z0, xc + 0.85, 1.6, z1, GREY)
        b.box(xc - 0.85, 4.3, z0, xc + 0.85, 4.9, z1, GREY)
        b.box(xc - 0.85, 1.6, z0, xc + 0.85, 4.3, z1, ["fins", None, GREY, GREY, None, None])
        b.box(xc - 0.83, 1.62, z1, xc + 0.83, 4.28, z1 + 0.02, ["fins", "fins", "fins", "fins", None, None])
        # Two fans on the back face, blowing out: shroud, guard (rings and spokes), hub, blades, motor.
        for yf in (2.3, 3.6):
            fan(b, np.array([xc, yf, z1 + 0.02]), 0.4 + i * 0.7 + yf, f"fans{group}")
        # Legs.
        for x in (xc - 0.75, xc + 0.75):
            for z in (z0 + 0.1, z1 - 0.2):
                b.box(x - 0.05, 0.0, z, x + 0.05, 1.3, z + 0.1, [GREY, GREY, GREY, GREY, None, None])
        # The return: down from the bottom box, the flow indicator on the drop, the pump, back into the tank.
        path(b, [(xc, 1.3, zm), (xc, 0.8, zm), (xc, 0.8, zt)], 0.1, GREY)
        flange(b, (xc, 0.8, zt + 0.08), "z", 0.17, 0.04, GREY, 6)
        gauge(b, (xc, 1.08, zm + 0.1), "z", 0.06, FACES["flow"], st["flow"] if st["pumps"] else 0, 0, 100, start=180, sweep=180,
              channel=f"flow{i + 1}", case=GREY)
        pc = np.array([xc, 0.8, zt + 0.45])
        cyl(b, pc - [0, 0, 0.17], pc + [0, 0, 0.17], 0.22, GREY, sides=16)               # the volute
        cyl(b, pc + [0.12, 0, 0], pc + [0.2, 0, 0], 0.15, GREY, sides=12)                # bracket
        cyl(b, pc + [0.2, 0, 0], pc + [0.62, 0, 0], 0.17, GREY, sides=14)                # the motor
        for k in range(8):
            t = k * math.pi / 4
            p = pc + [0.41, math.cos(t) * 0.17, math.sin(t) * 0.17]
            b.box(p[0] - 0.19, p[1] - 0.012, p[2] - 0.012, p[0] + 0.19, p[1] + 0.012, p[2] + 0.012, GREY)
        cyl(b, pc + [0.62, 0, 0], pc + [0.72, 0, 0], 0.18, sw("dgrey"), sides=14)        # fan cowl
        b.box(pc[0] + 0.3, pc[1] + 0.17, pc[2] - 0.08, pc[0] + 0.48, pc[1] + 0.3, pc[2] + 0.08, GREY)   # terminal box
        path(b, [(pc[0] + 0.39, pc[1] + 0.3, pc[2]), (pc[0] + 0.39, pc[1] + 0.4, pc[2]), (pc[0] + 0.39, pc[1] + 0.4, pc[2] + 0.25),
                 (pc[0] + 0.39, 0.0, pc[2] + 0.25)], 0.022, GALV)                       # conduit down into the pad
        for x in (pc[0] - 0.1, pc[0] + 0.55):
            b.box(x - 0.04, 0.0, pc[2] - 0.15, x + 0.04, pc[1] - 0.17 if x > pc[0] else pc[1] - 0.22, pc[2] + 0.15, [GREY, GREY, GREY, GREY, None, None])
        b.live.append({"t": "pump", "i": i + 1, "ch": f"pumps{group}"})
        tag(b, (xc, 1.45, z1 + 0.004), "z", f"COOLER {i + 1}", 0.28, 0.045)


def front_fittings(b, name, phase):
    """On the front: drain, filter-press and sampling valves, the rating plate, the stencil, the ladder with its
    locked guard."""
    z = TZ0
    # Bottom drain / filter press (gate valve, blind flange), top filter press, sampling valve.
    for x, y, txt in ((2.22, 0.95, "DRAIN"), (2.22, 4.85, "FILTER PRESS")):
        rod(b, (x, y, z), (x, y, z - 0.3), 0.07, GREY)
        gate_valve(b, (x, y, z - 0.45), "-z", "y", 0.07)
        flange(b, (x, y, z - 0.64), "-z", 0.1, 0.03, GREY, 6)
        tag(b, (x + 0.37, y + 0.2, z - 0.004), "-z", txt, 0.3, 0.045)
    rod(b, (3.3, 1.0, z), (3.3, 1.0, z - 0.14), 0.025, GREY)
    ball_valve(b, (3.3, 1.0, z - 0.2), "-z", "y", 0.035)
    tag(b, (3.3, 0.86, z - 0.004), "-z", "OIL SAMPLE", 0.26, 0.04)
    # Rating plate on stand-offs, its tag line, the phase stencil.
    px, py = 6.95, 3.65
    for dx in (-0.27, 0.27):
        for dy in (-0.17, 0.17):
            b.box(px + dx - 0.02, py + dy - 0.02, z - 0.04, px + dx + 0.02, py + dy + 0.02, z, sw("dgrey"))
    b.box(px - 0.31, py - 0.21, z - 0.05, px + 0.31, py + 0.21, z - 0.04, [ATLAS.mat(FACES["plate"]), None, ALU, ALU, ALU, ALU])
    # The phase letter, stencilled on the panel between two stiffeners (none may cross it).
    stencil(b, (3.35, 3.95, z - 0.003), "-z", name, 0.17)
    stencil(b, (3.35, 3.45, z - 0.003), "-z", phase, 0.62)
    # Ladder up the -x end of the front with a locked guard plate.
    lx = 0.95
    for dx in (-0.22, 0.22):
        b.box(lx + dx - 0.03, 0.0, z - 0.34, lx + dx + 0.03, COVER + 1.1, z - 0.28, [GALV] * 5 + [None])
        for y in (1.7, 3.3, 4.2):
            b.box(lx + dx - 0.02, y - 0.03, z - 0.28, lx + dx + 0.02, y + 0.03, z - (0.1 if dx > 0 else 0.0), GALV)
    for y in np.arange(0.95, COVER + 0.9, 0.3):
        cyl(b, (lx - 0.19, y, z - 0.31), (lx + 0.19, y, z - 0.31), 0.016, GALV, sides=6)
    b.box(lx - 0.26, 0.8, z - 0.4, lx + 0.26, 2.9, z - 0.38, [GALV, GALV, GALV, GALV, GALV, GALV])
    b.box(lx + 0.12, 1.85, z - 0.47, lx + 0.2, 2.0, z - 0.4, sw("yellow"))


def cabinet_shell(b, x0, x1, y0, y1, wall_z):
    """A weatherproof control cabinet standing off a tank wall facing -z (brackets, legs to the plinth, conduits from
    the trench, rain hood) with its white mounting plate; returns the plate's face depth and the cabinet's front."""
    z1 = wall_z - 0.18
    z0 = z1 - 0.55
    g = GREY
    for x in (x0 + 0.15, x1 - 0.15):                                   # stand-off brackets to the tank
        for y in (y0 + 0.32, y1 - 0.25):
            b.box(x - 0.05, y, z1, x + 0.05, y + 0.08, wall_z, g)
    b.box(x0, y0, z1 - 0.02, x1, y1, z1, [None, g, g, g, g, g])           # back
    b.box(x0, y0, z0, x0 + 0.03, y1, z1 - 0.02, g)                        # sides, top, bottom
    b.box(x1 - 0.03, y0, z0, x1, y1, z1 - 0.02, g)
    b.box(x0 + 0.03, y1 - 0.03, z0, x1 - 0.03, y1, z1 - 0.02, g)
    b.box(x0 + 0.03, y0, z0, x1 - 0.03, y0 + 0.03, z1 - 0.02, g)
    b.box(x0 - 0.04, y1, z0 - 0.06, x1 + 0.04, y1 + 0.04, z1, g)          # rain hood
    for x in (x0 + 0.1, x1 - 0.1):                                       # legs to the plinth
        b.box(x - 0.04, 0.56, z0 + 0.05, x + 0.04, y0, z0 + 0.13, g)
    rod(b, (x0 + 0.3, 0.0, z0 + 0.3), (x0 + 0.3, y0, z0 + 0.3), 0.04, GALV)    # conduits from the trench
    rod(b, (x0 + 0.5, 0.0, z0 + 0.3), (x0 + 0.5, y0, z0 + 0.3), 0.03, GALV)
    # Mounting plate (set off the back), its instruments facing -z.
    pz = z1 - 0.06
    b.box(x0 + 0.06, y0 + 0.06, pz, x1 - 0.06, y1 - 0.06, pz + 0.02, ["paint_white", None, None, None, None, None])
    return pz, z0


def cabinet_wiring(b, x0, x1, f, on):
    """Contactors on a DIN rail (on: which are pulled in), a terminal strip, wires down into a slotted duct."""
    b.box(x0 + 0.1, 1.48, f - 0.012, x1 - 0.1, 1.52, f, GALV)
    for k, onk in enumerate(on):
        cx = x0 + 0.16 + k * 0.13
        b.box(cx - 0.045, 1.42, f - 0.09, cx + 0.045, 1.58, f - 0.012, sw("dgrey"))
        b.box(cx - 0.035, 1.52, f - 0.1, cx + 0.035, 1.56, f - 0.09, sw("enamel"))
        lamp(b, (cx, 1.46, f - 0.09), "-z", "green", onk, 0.008)
    b.box(x0 + 0.1, 1.2, f - 0.008, x1 - 0.1, 1.24, f, GALV)
    for k in range(int((x1 - x0 - 0.22) / 0.016)):
        cx = x0 + 0.12 + k * 0.016
        b.box(cx, 1.17, f - 0.05, cx + 0.013, 1.27, f - 0.008, sw("blue") if k % 6 == 0 else sw("yellow"))
        if k % 2 == 0:
            col = (sw("red"), sw("black"), sw("white"), sw("blue"))[(k // 2) % 4]
            b.box(cx + 0.0045, 1.062, f - 0.03, cx + 0.0085, 1.17, f - 0.02, col)
    b.box(x0 + 0.08, 0.98, f - 0.07, x1 - 0.08, 1.06, f, sw("mgrey"))
    for k in range(int((x1 - x0 - 0.2) / 0.03)):
        cx = x0 + 0.1 + k * 0.03
        b.box(cx + 0.001, 1.055, f - 0.071, cx + 0.013, 1.061, f - 0.001, sw("black"))


def cabinet_door(b, x0, x1, y0, y1, z0, drawing):
    """The door, open 100 degrees on its -x hinge: stiffened frame, the drawing pocket with drawings."""
    g = GREY
    hx = x0
    dw = x1 - x0
    t = math.radians(100)
    u = np.array([math.cos(t), 0, -math.sin(t)])                  # along the door from the hinge (Ry(t) of +x)
    inside = np.array([math.sin(t), 0, math.cos(t)])             # its inside face (Ry(t) of +z)
    c = np.array([hx, (y0 + y1) / 2, z0]) + u * dw / 2 - inside * 0.015
    b.obox(tuple(_rnd(c)), tuple(_rnd(u)), (0, 1, 0), round(dw / 2, 4), round((y1 - y0) / 2, 4), 0.015, g)
    for y in (y0 + 0.3, y1 - 0.3):
        cyl(b, (hx - 0.02, y - 0.06, z0), (hx - 0.02, y + 0.06, z0), 0.02, sw("dgrey"), sides=8)
    pc = c + inside * 0.04 + np.array([0, 0.2, 0])
    b.obox(tuple(_rnd(pc)), tuple(_rnd(u)), (0, 1, 0), 0.4, 0.45, 0.03, g)
    key = ATLAS.add("plate:" + drawing[1], plate_img(drawing, 0.6, 0.7))
    b.obox(tuple(_rnd(pc + inside * 0.032 + np.array([0, 0.15, 0]))), tuple(_rnd(u)), (0, 1, 0), 0.3, 0.35, 0.002,
           [ATLAS.mat(key)] * 6)


def cabinet(b, name, st):
    """The cooling and monitoring cabinet on the front near the +x end, door swung open: OTI and WTI with drag hands,
    cooling group lamps and selectors, alarm lamps, contactors, breakers, terminal strip into a slotted duct."""
    x0, x1, y0, y1 = 6.9, 8.25, 0.95, 3.05
    f, z0 = cabinet_shell(b, x0, x1, y0, y1, TZ0)
    tag(b, ((x0 + x1) / 2, y1 - 0.14, f), "-z", f"{name}  COOLING & MONITORING", 0.9, 0.05)
    gauge(b, (x0 + 0.33, 2.55, f), "-z", 0.15, FACES["oti"], st["oil"], 0, 150, st["oil_max"], channel="top_oil")
    gauge(b, (x0 + 0.78, 2.55, f), "-z", 0.15, FACES["wti"], st["wdg"], 0, 150, st["wdg_max"], channel="winding")
    tag(b, (x0 + 0.33, 2.33, f), "-z", "26Q  TOP OIL", 0.26, 0.035)
    tag(b, (x0 + 0.78, 2.33, f), "-z", "49  WINDING", 0.26, 0.035)
    # Lamps: cooling groups and alarms; under each its tag (reading order: tag, lamp, switch).
    rows = [((x0 + 1.12, 2.66), "lamp", ("green", st["fans"] >= 1), "GROUP 1"),
            ((x0 + 1.12, 2.46), "lamp", ("green", st["fans"] >= 2), "GROUP 2"),
            ((x0 + 1.12, 2.26), "lamp", ("amber", st["pumps"]), "PUMPS")]
    for (cx, cy), _, (col, on), txt in rows:
        tag(b, (cx - 0.08, cy, f), "-z", txt, 0.14, 0.03)
        lamp(b, (cx + 0.06, cy, f), "-z", col, on)
    for k, txt in enumerate(("HI TEMP", "BUCHHOLZ", "PRD", "LOW OIL")):
        cx = x0 + 0.2 + k * 0.25
        tag(b, (cx, 2.06, f), "-z", txt, 0.2, 0.03)
        lamp(b, (cx, 1.96, f), "-z", "red", txt in st["alarms"])
    for k, txt in enumerate(("GROUP 1  MAN-OFF-AUTO", "GROUP 2  MAN-OFF-AUTO")):
        cx = x0 + 0.33 + k * 0.6
        tag(b, (cx, 1.82, f), "-z", txt, 0.44, 0.03)
        selector(b, (cx, 1.7, f), "-z", 45)
    cabinet_wiring(b, x0, x1, f, [(k < 2 and st["fans"] >= 1) or (2 <= k < 4 and st["fans"] >= 2) or (k in (4, 5) and st["pumps"])
                                  for k in range(8)])
    cabinet_door(b, x0, x1, y0, y1, z0, ["WIRING DIAGRAM", "MT COOLING CONTROL", "DWG E-4471  REV 3", "", "TB1-TB4  K1-K8"])


# ---------------------------------------------------------------- the unit auxiliary transformers (UAT-1A, UAT-1B)
#
# Real basis: three-phase, 22 kV delta : 4.16 kV wye, ONAN/ONAF 36/48 MVA, 9.5 % impedance, about 78 t and 21 000 L of
# oil; fed by an isolated phase bus tap off the generator's main bus (no breaker between: they trip with the
# generator), the 4.16 kV side out through a non-segregated phase bus duct to the plant's 4160 V switchgear, the wye
# neutral grounded through a neutral grounding resistor (400 A for 10 s). De-energized tap changer (5 positions,
# +-2 x 2.5 %), padlocked. Panel radiators on the back with one fan stage: fans on at 65 C top oil / 75 C winding.
# Alarms and trips as the main transformers. With the plant at full power each carries about 30 MVA (the house
# load is split over both), so the fans run: top oil about 58 C, winding hot spot about 77 C.

UAT_SIZE = (7, 6, 8)
UX0, UX1, UZ0, UZ1, UY0, UY1 = 0.6, 5.4, 1.3, 4.5, 0.5, 3.9
UCOV = UY1 + 0.14
TAP_Y = 7.18                    # the main bus's underside (centre 7.6, radius 0.42); the taps rise into it
TAP_Z = (1.8, 2.9, 4.0)         # its three phases, front to back
UAT_FRONT = (1.2, 2.0, 3.6, 4.4)                 # front stiffeners (none at 2.8: the stencil's panel)
UAT_BACK = (1.2, 2.0, 2.8, 3.6, 4.4)
UAT_RADS = (1.6, 2.4, 3.2, 4.0, 4.8)             # panel radiators on the back, between the stiffeners
UAT_BAY = dict(plinth=(0.3, 0.9, 7.0, 5.95), curb_x=(-1.2, 7.4), curb_z=(-0.6, 6.8), wall=(-0.6, 7.0, 6.6),
               rings=((3.1, (-0.1, 7.15, 0.2, 6.2)), (4.6, (0.05, 5.9, 0.75, 5.3))), aim=(3.0, 2.9),
               stands=(((-0.1, 0.2), (7.15, 0.2), (-0.1, 6.2), (7.15, 6.2), (5.6, 0.2), (3.5, 6.2)),
                       ((0.05, 0.75), (5.9, 0.75), (0.05, 5.3), (5.9, 5.3))))


def tap_dial_img(r_m=0.1):
    """The de-energized tap changer's position plate: 1 to 5 round the top, its legend."""
    s = int(2 * r_m * PXB)
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c, r = s / 2, s / 2 - 1
    d.ellipse([c - r, c - r, c + r, c + r], fill=(196, 198, 200, 255), outline=(110, 112, 114), width=2)
    for i in range(5):
        t = math.radians(150 - 30 * i)
        d.text((c + math.cos(t) * r * .72, c - math.sin(t) * r * .72), str(i + 1), font=_font(r * .26), fill=(26, 26, 28), anchor="mm")
        d.line([(c + math.cos(t) * r * .86, c - math.sin(t) * r * .86), (c + math.cos(t) * r * .97, c - math.sin(t) * r * .97)],
               fill=(26, 26, 28), width=max(2, int(r * .03)))
    d.text((c, c + r * .45), "TAP", font=_font(r * .16), fill=(26, 26, 28), anchor="mm")
    return im


def uat(name="UAT-1A", state=None):
    """One unit auxiliary transformer. state: oil, wdg, oil_max, wdg_max, level, fans (bool), alarms, tap (1..5)."""
    _faces()
    if "tap" not in FACES:
        FACES["tap"] = ATLAS.add("dial:tap", tap_dial_img())
        FACES["plate_uat"] = ATLAS.add("plate:uat", plate_img(
            ["UNIT AUXILIARY TRANSFORMER", "3 PHASE  60 Hz  ONAN/ONAF  36/48 MVA", "HV 22 000 V DELTA  BIL 150 kV",
             "LV 4160 V WYE  NGR 400 A 10 s", "IMPEDANCE 9.5 %  AT 36 MVA", "DETC 5 POS  +-2 x 2.5 %",
             "OIL 21 000 L  TOTAL 78 000 kg", "SERIAL 26-1190  YEAR 2026"], 0.5, 0.36))
    st = dict(oil=58, wdg=77, oil_max=64, wdg_max=85, level=55, fans=True, alarms=(), tap=3)
    st.update(state or {})
    b = Board(UAT_SIZE[0], 1190, face=0.0)
    b.live = []
    uat_tank(b)
    b.core = len(b.still)
    uat_cover(b)
    uat_hv(b)
    uat_lv(b, name)
    uat_conservator(b, st)
    uat_radiators(b, st)
    uat_front(b, name, st)
    uat_end(b)
    uat_cabinet(b, name, st)
    return b


def uat_tank(b):
    g = GREY
    for x in (1.3, 4.7):                                                                     # skids
        b.box(x - 0.15, 0.0, UZ0 - 0.15, x + 0.15, 0.05, UZ1 + 0.15, [g, g, g, g, g, None])
        b.box(x - 0.035, 0.05, UZ0 - 0.15, x + 0.035, 0.4, UZ1 + 0.15, [g, g, g, g, None, None])
        b.box(x - 0.15, 0.4, UZ0 - 0.15, x + 0.15, 0.45, UZ1 + 0.15, g)
    b.box(UX0, 0.45, UZ0, UX1, UY0, UZ1, [g, g, g, g, None, g])
    for x in (UX0 + 0.22, UX1 - 0.22):
        for z in (UZ0, UZ1):
            s = -1 if z == UZ0 else 1
            b.box(x - 0.17, 0.85, min(z, z + s * 0.22), x + 0.17, 1.03, max(z, z + s * 0.22), g)    # jacking pads
    b.box(UX0, UY0, UZ0, UX1, UY1, UZ1, [g, g, g, g, None, None])
    for y0, y1, out in ((1.3, 1.42, 0.1), (2.55, 2.65, 0.08)):
        b.box(UX0 - out, y0, UZ0 - out, UX1 + out, y1, UZ1 + out, g)
    for x in np.arange(UX0 + 0.1, UX1, 0.42):                                                   # flange bolts
        for z, s in ((UZ0 - 0.12, -1), (UZ1 + 0.12, 1)):
            cyl(b, (x, 1.42, z - s * 0.04), (x, 1.47, z - s * 0.04), 0.022, BLACK, sides=6)
    segs = ((1.42, 2.55), (2.65, UY1 - 0.06))
    for xs, z, s in ((UAT_FRONT, UZ0, -1), (UAT_BACK, UZ1, 1)):
        for x in xs:
            for y0, y1 in segs:
                b.box(x - 0.06, y0, min(z, z + s * 0.1), x + 0.06, y1, max(z, z + s * 0.1), [g, g, g, g, None, None])
    for z in (1.9, 2.7, 3.5, 4.3):
        for x, s in ((UX0, -1), (UX1, 1)):
            for y0, y1 in segs:
                b.box(min(x, x + s * 0.1), y0, z - 0.06, max(x, x + s * 0.1), y1, z + 0.06, [g, g, g, g, None, None])
    b.box(UX0 - 0.08, UY1, UZ0 - 0.08, UX1 + 0.08, UY1 + 0.08, UZ1 + 0.08, g)                  # cover and its flange
    b.box(UX0, UY1 + 0.08, UZ0, UX1, UCOV, UZ1, [g, g, g, g, g, None])
    for x in np.arange(UX0 + 0.1, UX1, 0.4):
        for z in (UZ0 - 0.04, UZ1 + 0.04):
            cyl(b, (x, UY1 + 0.08, z), (x, UY1 + 0.12, z), 0.02, BLACK, sides=6)
    for z in np.arange(UZ0 + 0.15, UZ1, 0.4):
        for x in (UX0 - 0.04, UX1 + 0.04):
            cyl(b, (x, UY1 + 0.08, z), (x, UY1 + 0.12, z), 0.02, BLACK, sides=6)
    for x in (UX0 + 0.25, UX1 - 0.25):                                                          # lifting lugs
        for z, s in ((UZ0, -1), (UZ1, 1)):
            b.box(x - 0.15, UY1 - 0.6, min(z, z + s * 0.12), x + 0.15, UY1 - 0.06, max(z, z + s * 0.12), g)
            cyl(b, (x - 0.05, UY1 - 0.3, z + s * 0.06), (x + 0.05, UY1 - 0.3, z + s * 0.06), 0.06, sw("dgrey"), sides=10)
    for x in (UX0 + 0.12, UX1 - 0.12):                                                          # ground pads
        b.box(x - 0.1, 0.6, UZ0 - 0.03, x + 0.1, 0.75, UZ0, [COPPER, None, COPPER, COPPER, COPPER, COPPER])
        path(b, [(x, 0.62, UZ0 - 0.06), (x, 0.3, UZ0 - 0.06), (x, 0.02, UZ0 - 0.4)], 0.02, COPPER)


def uat_cover(b):
    """Manhole; pressure relief device with its shield open to the back and the discharge pipe down the back wall
    (behind the radiators) into the pit; the sudden pressure relay low on the -x end."""
    flange(b, (2.75, UCOV + 0.04, 2.3), "y", 0.32, 0.08, GREY, 10)
    cyl(b, (2.75, UCOV + 0.08, 2.3), (2.75, UCOV + 0.14, 2.3), 0.26, GREY, sides=20)
    px, pz = 3.0, 3.75
    cyl(b, (px, UCOV, pz), (px, UCOV + 0.18, pz), 0.15, GREY, sides=16)
    flange(b, (px, UCOV + 0.2, pz), "y", 0.2, 0.04, GREY, 8)
    b.box(px - 0.25, UCOV + 0.22, pz - 0.25, px + 0.25, UCOV + 0.48, pz + 0.25, [YELLOW, None, YELLOW, YELLOW, YELLOW, YELLOW])
    path(b, [(px, UCOV + 0.35, pz + 0.25), (px, UCOV + 0.35, UZ1 + 0.25), (px, 0.3, UZ1 + 0.25), (px, 0.3, UZ1 + 1.5),
             (px, -0.1, UZ1 + 1.5)], 0.07, GREY)
    for y in (1.2, 2.2, 3.2):
        b.box(px - 0.04, y - 0.04, UZ1, px + 0.04, y + 0.04, UZ1 + 0.18, GREY)
    tag(b, (px, UCOV + 0.35, pz - 0.254), "-z", "PRD 63Q", 0.2, 0.035)


def uat_hv(b):
    """The isolated phase bus tap: a flange box over the HV bushings on the cover at the -x end, three round tap
    enclosures rising to the main bus's tee flanges, each with its bellows."""
    hx = 1.6
    b.box(hx - 0.65, UCOV, UZ0 + 0.12, hx + 0.65, UCOV + 0.5, UZ1 - 0.12, ALU)
    for z in TAP_Z:
        flange(b, (hx, UCOV + 0.54, z), "y", 0.36, 0.06, ALU, 12)
        cyl(b, (hx, UCOV + 0.57, z), (hx, 7.6, z), 0.29, ALU, sides=20)          # up into the main bus (its centre)
        for y in (UCOV + 0.9, UCOV + 1.02):
            ring(b, (hx, y, z), "y", 0.3, 0.05, ALU, 20)
        flange(b, (hx, 6.6, z), "y", 0.34, 0.05, ALU, 12)                          # the field joint
    for z in TAP_Z:
        tag(b, (hx - 0.654, UCOV + 0.25, z), "-x", "H" + str(TAP_Z.index(z) + 1), 0.12, 0.04)


def uat_lv(b, name):
    """The 4.16 kV side: the bus duct's throat on the +x end, the non-segregated phase bus duct out and down into the
    floor (buried to the plant); the X0 neutral bushing and its strap to the neutral grounding resistor."""
    a = ALU
    b.box(UX1, 2.2, 2.45, UX1 + 0.3, 3.3, 4.15, [a, a, None, a, a, a])                 # throat
    b.box(UX1 + 0.3, 2.3, 2.6, 6.2, 3.0, 4.0, [a, a, None, None, a, a])                # duct, out
    b.box(6.2, 0.0, 2.6, 6.8, 3.0, 4.0, [a, a, a, a, a, None])                         # and down
    for (x0, y0, x1, y1) in ((UX1 + 0.3, 2.26, UX1 + 0.36, 3.04), (6.12, 2.26, 6.2, 3.04)):
        b.box(x0, y0, 2.56, x1, y1, 4.04, a)                                           # bolted joints
    for y in (0.06, 1.5):
        b.box(6.16, y, 2.56, 6.84, y + 0.06, 4.04, a)
    b.box(6.1, -0.02, 2.5, 6.9, 0.04, 4.1, GALV)                                        # floor collar
    # Bolt heads round the joints, the bolted inspection covers on the duct's front.
    for x in (UX1 + 0.33, 6.16):
        for y in np.arange(2.36, 3.0, 0.16):
            for z in (2.56, 4.04):
                s_ = -1 if z < 3 else 1
                cyl(b, (x, y, z), (x, y, z + s_ * 0.02), 0.014, BLACK, sides=6)
        for z in np.arange(2.7, 4.0, 0.2):
            cyl(b, (x, 3.04, z), (x, 3.06, z), 0.014, BLACK, sides=6)
    for y in (0.09, 1.53):
        for x in np.arange(6.26, 6.8, 0.16):
            for z in (2.56, 4.04):
                s_ = -1 if z < 3 else 1
                cyl(b, (x, y, z), (x, y, z + s_ * 0.02), 0.014, BLACK, sides=6)
    for (y0, y1) in ((0.25, 1.35), (1.7, 2.85)):
        b.box(6.28, y0, 2.585, 6.72, y1, 2.6, [a, None, a, a, a, a])
        for x in (6.32, 6.68):
            for y in np.linspace(y0 + 0.05, y1 - 0.05, 4):
                cyl(b, (x, y, 2.585), (x, y, 2.57), 0.012, BLACK, sides=6)
    # The duct's ground strap down into the grid.
    path(b, [(6.84, 0.5, 3.3), (6.9, 0.5, 3.3), (6.9, 0.02, 3.3), (6.9, 0.02, 3.7)], 0.018, COPPER)
    tag(b, (6.5, 1.6, 2.581), "-z", f"BD-{name[-2:]}  4160 V", 0.36, 0.045)
    # X0 bushing on the end wall, its strap down to the resistor's bushing.
    zc = 1.65
    cyl(b, (UX1, 3.05, zc), (UX1 + 0.07, 3.05, zc), 0.14, GREY, sides=14)
    for i in range(4):
        x = UX1 + 0.1 + 0.08 * i
        cyl(b, (x, 3.05, zc), (x + 0.03, 3.05, zc), 0.12 if i % 2 == 0 else 0.1, PORC, sides=14)
    cyl(b, (UX1 + 0.07, 3.05, zc), (UX1 + 0.42, 3.05, zc), 0.06, PORC, sides=10)
    cyl(b, (UX1 + 0.42, 3.05, zc), (UX1 + 0.55, 3.05, zc), 0.025, COPPER, sides=8)
    path(b, [(UX1 + 0.52, 3.05, zc), (6.15, 3.05, zc), (6.15, 2.32, zc)], 0.02, COPPER)
    # The neutral grounding resistor: louvred stainless box on legs, entry bushing on top, ground lead down a leg.
    nx0, nx1, nz0, nz1, ny0, ny1 = 5.75, 6.55, zc - 0.42, zc + 0.42, 0.95, 1.95
    lv = "louvre"
    b.box(nx0, ny0, nz0, nx1, ny1, nz1, [lv, lv, lv, lv, ALU, ALU])
    b.box(nx0 - 0.04, ny1, nz0 - 0.04, nx1 + 0.04, ny1 + 0.05, nz1 + 0.04, ALU)       # roof
    for x in (nx0 + 0.05, nx1 - 0.05):
        for z in (nz0 + 0.05, nz1 - 0.05):
            b.box(x - 0.035, 0.0, z - 0.035, x + 0.035, ny0, z + 0.035, GALV)
    cyl(b, (6.15, ny1 + 0.05, zc), (6.15, ny1 + 0.09, zc), 0.08, GREY, sides=12)
    for i in range(3):
        y = ny1 + 0.1 + 0.06 * i
        cyl(b, (6.15, y, zc), (6.15, y + 0.025, zc), 0.07, PORC, sides=12)
    cyl(b, (6.15, ny1 + 0.09, zc), (6.15, 2.3, zc), 0.04, PORC, sides=8)
    tag(b, ((nx0 + nx1) / 2, 1.75, nz0 - 0.004), "-z", f"NGR-{name[-2:]}  400 A 10 s", 0.5, 0.045)
    b.box(nx1 - 0.2, 1.05, nz0 - 0.08, nx1 - 0.08, 1.2, nz0, GREY)                     # its terminal box, lead out
    path(b, [(nx1 - 0.14, 1.05, nz0 - 0.04), (nx1 - 0.14, 0.3, nz0 - 0.04), (nx1 - 0.14, 0.02, nz0 - 0.4)], 0.02, COPPER)


def uat_conservator(b, st):
    """Conservator across the +x end of the cover (axis along z) on saddles; the oil level gauge on its front end,
    tilted down to the walkway; the Buchholz relay in its pipe with a valve each side; the breather down the front."""
    cx, cy, z0, z1 = 4.7, UCOV + 1.25, 1.7, 4.1
    cyl(b, (cx, cy, z0), (cx, cy, z1), 0.4, GREY, sides=24)
    for z, s in ((z0, -1), (z1, 1)):
        cyl(b, (cx, cy, z), (cx, cy, z + s * 0.1), 0.33, GREY, sides=24)
        cyl(b, (cx, cy, z + s * 0.1), (cx, cy, z + s * 0.16), 0.2, GREY, sides=20)
    for z in (2.2, 3.6):
        b.box(cx - 0.36, UCOV, z - 0.07, cx + 0.36, cy - 0.36, z + 0.07, [GREY, GREY, GREY, GREY, None, None])
        b.box(cx - 0.4, cy - 0.36, z - 0.09, cx + 0.4, cy - 0.3, z + 0.09, GREY)
    n = np.array([0, -math.sin(math.radians(30)), -math.cos(math.radians(30))])
    c = np.array([cx, cy - 0.02, z0 - 0.19])
    cyl(b, c + n * -0.03, c, 0.12, GREY, sides=16)
    gauge(b, c, n, 0.11, FACES["olg"], st["level"], 0, 100, start=180, sweep=180, channel="oil_level")
    # Buchholz: down from the conservator, along -x through the relay, down into the cover.
    bz = 2.9
    path(b, [(cx, cy - 0.38, bz), (cx, cy - 0.72, bz), (3.45, cy - 0.72, bz), (3.45, UCOV, bz)], 0.06, GREY)
    b.box(3.86, cy - 0.92, bz - 0.17, 4.2, cy - 0.52, bz + 0.17, GREY)
    for x in (3.95, 4.11):
        cyl(b, (x, cy - 0.66, bz - 0.17), (x, cy - 0.66, bz - 0.19), 0.045, "glass", sides=10)
    tag(b, (4.03, cy - 0.84, bz - 0.174), "-z", "BUCHHOLZ 63", 0.3, 0.04)
    ball_valve(b, (4.4, cy - 0.72, bz), "x", "y", 0.045)
    ball_valve(b, (3.66, cy - 0.72, bz), "x", "y", 0.045)
    # Breather: from the conservator's underside near the front, down to the cover, to the front edge, down the wall.
    bx = 4.9
    path(b, [(bx, cy - 0.36, 1.95), (bx, UCOV + 0.04, 1.95), (bx, UCOV + 0.04, UZ0 - 0.3), (bx, 2.05, UZ0 - 0.3)], 0.03, GREY)
    for y in (2.8, 3.5):
        b.box(bx - 0.04, y - 0.04, UZ0 - 0.26, bx + 0.04, y + 0.04, UZ0, GREY)
    cyl(b, (bx, 1.95, UZ0 - 0.3), (bx, 2.05, UZ0 - 0.3), 0.12, GREY, sides=14)
    cyl(b, (bx, 1.45, UZ0 - 0.3), (bx, 1.95, UZ0 - 0.3), 0.1, "glass", sides=14)
    cyl(b, (bx, 1.47, UZ0 - 0.3), (bx, 1.86, UZ0 - 0.3), 0.085, sw("orange"), sides=12)
    cyl(b, (bx, 1.35, UZ0 - 0.3), (bx, 1.45, UZ0 - 0.3), 0.12, GREY, sides=14)
    cyl(b, (bx, 1.2, UZ0 - 0.3), (bx, 1.35, UZ0 - 0.3), 0.08, "glass", sides=12)
    b.box(bx - 0.05, 1.62, UZ0 - 0.18, bx + 0.05, 1.72, UZ0, GREY)


def uat_radiators(b, st):
    """Five panel radiators on the back, each hung off the tank on a valved top and bottom header: twelve pressed
    panels between collector pipes, a tie bar across their outer edges. Two fans on a frame blow into them."""
    zr0, zr1 = UZ1 + 0.35, UZ1 + 0.95
    for xc in UAT_RADS:
        for y in (3.45, 1.05):
            rod(b, (xc, y, UZ1), (xc, y, zr0 + 0.07), 0.075, GREY, 12)
            flange(b, (xc, y, UZ1 + 0.06), "z", 0.13, 0.03, GREY, 6)
            cyl(b, (xc, y, UZ1 + 0.13), (xc, y, UZ1 + 0.23), 0.11, GREY, sides=12)           # its valve
            rod(b, (xc, y + 0.11, UZ1 + 0.18), (xc, y + 0.2, UZ1 + 0.18), 0.015, GREY, 6)
            bar(b, (xc - 0.1, y + 0.21, UZ1 + 0.18), (xc + 0.1, y + 0.21, UZ1 + 0.18), 0.025, sw("red"), 0.015)
            rod(b, (xc - 0.3, y, zr0 + 0.08), (xc + 0.3, y, zr0 + 0.08), 0.07, GREY, 12)     # collector
        for k in range(12):
            x = xc - 0.275 + k * 0.05
            b.box(x - 0.007, 0.98, zr0, x + 0.007, 3.52, zr1, GREY)
        b.box(xc - 0.3, 2.2, zr1, xc + 0.3, 2.26, zr1 + 0.02, GREY)                           # tie bar
    # The fan frame: posts from the plinth, crossbars, a fan between each pair of radiators.
    zf = zr1 + 0.05
    for xf in (2.0, 4.4):
        for x in (xf - 0.7, xf + 0.7):
            b.box(x - 0.04, 0.0, zf + 0.05, x + 0.04, 3.0, zf + 0.13, [GREY, GREY, GREY, GREY, GREY, None])
            b.box(x - 0.1, 0.0, zf - 0.01, x + 0.1, 0.02, zf + 0.19, GALV)
        for y in (2.25 - 0.62, 2.25 + 0.62):
            b.box(xf - 0.66, y - 0.03, zf + 0.06, xf + 0.66, y + 0.03, zf + 0.12, GREY)
        fan(b, np.array([xf, 2.25, zf]), 0.9 + xf, "fans1")
        # its motor lead in conduit down a post into the trench
        path(b, [(xf + 0.12, 2.25, zf + 0.27), (xf + 0.12, 2.25 - 0.62, zf + 0.27), (xf + 0.6, 2.25 - 0.62, zf + 0.27),
                 (xf + 0.6, 0.0, zf + 0.27)], 0.016, GALV)


def uat_front(b, name, st):
    """The front: the de-energized tap changer (position plate, crank, padlock), drain and sampling valves, the
    rating plate, the stencil, the breather (in uat_conservator)."""
    z = UZ0
    # De-energized tap changer between the first two stiffeners.
    tx, ty = 1.6, 3.0
    cyl(b, (tx, ty, z), (tx, ty, z - 0.08), 0.14, GREY, sides=16)
    face_quad(b, (tx, ty, z - 0.082), "-z", 0.2, 0.2, ATLAS.mat(FACES["tap"]))
    t = math.radians(150 - 30 * (st["tap"] - 1))
    d = np.array([math.cos(t), math.sin(t), 0])           # (from the front, +x is the viewer's right)
    rod(b, (tx, ty, z - 0.08), (tx, ty, z - 0.13), 0.025, GREY, 8)
    bar(b, (tx, ty, z - 0.135), np.array([tx, ty, z - 0.135]) + d * 0.09, 0.03, BLACK, 0.015)
    rod(b, np.array([tx, ty, z - 0.14]) + d * 0.08, np.array([tx, ty, z - 0.22]) + d * 0.08, 0.012, GREY, 6)
    cyl(b, np.array([tx, ty, z - 0.22]) + d * 0.08, np.array([tx, ty, z - 0.28]) + d * 0.08, 0.02, BLACK, sides=8)
    b.box(tx + 0.07, ty - 0.2, z - 0.1, tx + 0.13, ty - 0.12, z - 0.07, YELLOW)                 # padlock
    rod(b, (tx + 0.1, ty - 0.12, z - 0.085), (tx + 0.1, ty - 0.07, z - 0.085), 0.008, ALU, 6)
    tag(b, (tx, ty + 0.4, z - 0.004), "-z", "DETC  DE-ENERGIZED ONLY", 0.6, 0.04)
    # Drain (gate valve) low, sampling valve.
    rod(b, (1.6, 0.78, z), (1.6, 0.78, z - 0.22), 0.05, GREY)
    gate_valve(b, (1.6, 0.78, z - 0.33), "-z", "y", 0.05)
    flange(b, (1.6, 0.78, z - 0.47), "-z", 0.07, 0.025, GREY, 6)
    tag(b, (1.6, 1.17, z - 0.004), "-z", "DRAIN", 0.22, 0.04)
    rod(b, (4.0, 1.0, z), (4.0, 1.0, z - 0.12), 0.02, GREY)
    ball_valve(b, (4.0, 1.0, z - 0.17), "-z", "y", 0.03)
    tag(b, (4.0, 1.17, z - 0.004), "-z", "OIL SAMPLE", 0.26, 0.04)
    # Rating plate on stand-offs.
    px, py = 4.0, 3.25
    for dx in (-0.22, 0.22):
        for dy in (-0.15, 0.15):
            b.box(px + dx - 0.02, py + dy - 0.02, z - 0.04, px + dx + 0.02, py + dy + 0.02, z, sw("dgrey"))
    b.box(px - 0.25, py - 0.18, z - 0.05, px + 0.25, py + 0.18, z - 0.04, [ATLAS.mat(FACES["plate_uat"]), None, ALU, ALU, ALU, ALU])
    stencil(b, (2.8, 3.6, z - 0.003), "-z", name, 0.26)


def uat_end(b):
    """The -x end: ladder with its locked guard, the top filter-press valve, the sudden pressure relay."""
    x = UX0
    lz = 2.3
    for dz in (-0.22, 0.22):
        b.box(x - 0.34, 0.0, lz + dz - 0.03, x - 0.28, UCOV + 1.1, lz + dz + 0.03, [GALV] * 5 + [None])
        for y in (1.9, 3.2):
            b.box(x - 0.28, y - 0.03, lz + dz - 0.02, x, y + 0.03, lz + dz + 0.02, GALV)
    for y in np.arange(0.8, UCOV + 0.9, 0.3):
        cyl(b, (x - 0.31, y, lz - 0.19), (x - 0.31, y, lz + 0.19), 0.016, GALV, sides=6)
    b.box(x - 0.4, 0.7, lz - 0.26, x - 0.38, 2.6, lz + 0.26, GALV)
    b.box(x - 0.47, 1.75, lz + 0.12, x - 0.4, 1.9, lz + 0.2, YELLOW)
    # Filter press (top) on the end wall.
    fz = 3.9
    rod(b, (x, 3.3, fz), (x - 0.2, 3.3, fz), 0.05, GREY)
    gate_valve(b, (x - 0.31, 3.3, fz), "-x", "y", 0.05)
    flange(b, (x - 0.45, 3.3, fz), "-x", 0.07, 0.025, GREY, 6)
    tag(b, (x - 0.004, 3.0, fz), "-x", "FILTER PRESS", 0.3, 0.04)
    # Sudden pressure relay, its conduit down to the trench.
    b.box(x - 0.24, 0.72, fz - 0.15, x, 1.06, fz + 0.15, [GREY, GREY, GREY, None, GREY, GREY])
    tag(b, (x - 0.004, 1.19, fz), "-x", "63 SPR", 0.2, 0.04)
    path(b, [(x - 0.12, 0.72, fz), (x - 0.12, 0.0, fz)], 0.016, GALV)


def uat_cabinet(b, name, st):
    """The cooling and monitoring cabinet on the front, door open: OTI and WTI with drag hands, fan and supply lamps,
    alarm lamps, the fan selector, contactors, terminal strip and duct."""
    x0, x1, y0, y1 = 2.15, 3.45, 0.9, 2.95
    f, z0 = cabinet_shell(b, x0, x1, y0, y1, UZ0)
    tag(b, ((x0 + x1) / 2, y1 - 0.14, f), "-z", f"{name}  COOLING & MONITORING", 0.9, 0.05)
    gauge(b, (x0 + 0.3, 2.47, f), "-z", 0.14, FACES["oti"], st["oil"], 0, 150, st["oil_max"], channel="top_oil")
    gauge(b, (x0 + 0.71, 2.47, f), "-z", 0.14, FACES["wti"], st["wdg"], 0, 150, st["wdg_max"], channel="winding")
    tag(b, (x0 + 0.3, 2.26, f), "-z", "26Q  TOP OIL", 0.25, 0.035)
    tag(b, (x0 + 0.71, 2.26, f), "-z", "49  WINDING", 0.25, 0.035)
    for (cy, col, on, txt) in ((2.6, "green", st["fans"], "FANS"), (2.42, "white", True, "AC SUPPLY")):
        tag(b, (x0 + 1.04, cy, f), "-z", txt, 0.16, 0.03)
        lamp(b, (x0 + 1.18, cy, f), "-z", col, on)
    for k, txt in enumerate(("HI TEMP", "BUCHHOLZ", "PRD", "LOW OIL")):
        cx = x0 + 0.2 + k * 0.25
        tag(b, (cx, 2.06, f), "-z", txt, 0.2, 0.03)
        lamp(b, (cx, 1.96, f), "-z", "red", txt in st["alarms"])
    tag(b, (x0 + 0.4, 1.82, f), "-z", "FANS  MAN-OFF-AUTO", 0.4, 0.03)
    selector(b, (x0 + 0.4, 1.7, f), "-z", 45)
    cabinet_wiring(b, x0, x1, f, [st["fans"], st["fans"], True, False])
    cabinet_door(b, x0, x1, y0, y1, z0, ["WIRING DIAGRAM", "UAT COOLING CONTROL", "DWG E-4482  REV 1", "", "TB1-TB2  K1-K4"])


def ipb_run(b, x0, x1, bents, wall_bents=(), taps=()):
    """A length of the main isolated phase bus overhead (the UATs' taps tee into it from below): three round
    aluminium enclosures along x at y 7.6 over TAP_Z, bolted joints every 3 blocks; steel bents from the ground
    (bents) or on a firewall's top (wall_bents: (x, wall top))."""
    for z in TAP_Z:
        cyl(b, (x0, 7.6, z), (x1, 7.6, z), 0.42, ALU, sides=24)
        for x in np.arange(x0 + 1.5, x1, 3.0):
            ring(b, (x, 7.6, z), "x", 0.44, 0.05, ALU, 24)
        for x in taps:                                     # the welded saddle round a UAT tap's entry
            ring(b, (x, TAP_Y + 0.06, z), "y", 0.31, 0.06, ALU, 20)
    za, zb = TAP_Z[0] - 0.75, TAP_Z[-1] + 0.75
    for x, base in [(x, 0.0) for x in bents] + list(wall_bents):
        for z in (za, zb):
            b.box(x - 0.1, base, z - 0.1, x + 0.1, 6.98, z + 0.1, [GREY, GREY, GREY, GREY, None, None])
            b.box(x - 0.18, base, z - 0.18, x + 0.18, base + 0.03, z + 0.18, GALV)
        b.box(x - 0.12, 6.98, za - 0.15, x + 0.12, 7.13, zb + 0.15, GREY)
        if base == 0.0:
            bar(b, (x, 0.6, za + 0.08), (x, 6.9, zb - 0.08), 0.08, GREY)
            for z in (za, zb):
                b.box(x - 0.3, -0.12, z - 0.3, x + 0.3, 0.0, z + 0.3, ["concrete"] * 5 + [None])   # pier
        for z in TAP_Z:
            b.box(x - 0.1, 7.13, z - 0.2, x + 0.1, 7.2, z + 0.2, GREY)


# ---------------------------------------------------------------- the isolated phase bus and its trestles
#
# Real basis: a 22 kV, ~32 kA generator's isolated phase bus: each phase its own welded aluminium tube (about 0.85 m
# across here) round its own conductor, the three side by side about 1.1 m apart, held on steel trestles every 5 to
# 6 m; leaving the turbine hall through a wall seal plate, offsetting down to run along the yard, tapped from below
# by the unit aux transformers and branching up and over to each single-phase main transformer's two LV bushings
# (the transformers' LV delta is made in the bus: A on phases a-b, B on b-c, C on c-a). Bolted hand-hole covers on
# top, weld bands at the joints, painted phase letters, rubber bellows at every equipment flange, a bolted end
# closure, a dry-air pressurising unit, every trestle grounded to the grid.
#
# Site layout (round 3, site coordinates: x east, z south, y up from the grass): the bus leaves the turbine hall's
# east wall at x 203.5, 10 up, on z 59 (phase b; a north of it, c south) and runs east 7.72 up. The main transformers
# stand in a row north of it, fronts to the north walkway, HV ends east toward the switchyard; the UATs stand under
# it, between the row's back and the bus.

YARD = dict(hall_x=203.5, exit_y=10.0, bus_z=59.0, ph=1.1, bus_y=7.72, branch_y=9.12, plinth=0.12,
            gsu_z=43.8, gsu=(("MT-1A", "A", 216.2), ("MT-1B", "B", 229.6), ("MT-1C", "C", 243.0), ("MT-1S", "S", 256.4)),
            uat_z=56.1, uat=(("UAT-1A", 226.5), ("UAT-1B", 217.5)), uat_wall=218.9,
            bents=(207.0, 212.5, 218.9, 225.0, 230.8, 236.5), end_x=237.6)
DELTA = {"A": "ab", "B": "bc", "C": "ca"}
IPB_R = 0.42


def phase_z(p):
    return YARD["bus_z"] + {"a": -1, "b": 0, "c": 1}[p] * YARD["ph"]


def ipb(b=None):
    """The whole isolated phase bus in its own frame (origin at the hall wall's exit point on the grass, site
    (hall_x, 0, bus_z); frame x = west from there, as every equipment frame is drawn mirrored)."""
    if b is None:
        b = Board(9, 1201, face=0.0)
        b.live = []
    Y = YARD
    O = (Y["hall_x"], 0.0, Y["bus_z"])
    F = lambda wx, wy, wz: (O[0] - wx, wy - O[1], wz - O[2])
    by, ey, r = Y["bus_y"], Y["exit_y"], IPB_R
    x_drop = Y["hall_x"] + 1.6
    for p in "abc":
        z = phase_z(p)
        # Out of the wall, the mitred bend down, the drop, the bend east, the run to the end closure.
        cyl(b, F(Y["hall_x"] - 0.3, ey, z), F(x_drop + r, ey, z), r, ALU, sides=24)
        cyl(b, F(x_drop, ey + r, z), F(x_drop, by - r, z), r, ALU, sides=24)
        cyl(b, F(x_drop - r, by, z), F(Y["end_x"], by, z), r, ALU, sides=24)
        for c, ax in ((F(x_drop - 0.45, ey, z), "x"), (F(x_drop, by + 0.45, z), "y"), (F(x_drop, ey - 0.45, z), "y"),
                      (F(x_drop + 0.45, by, z), "x")):
            ring(b, c, ax, r + 0.015, 0.035, ALU, 24)                                 # the bends' weld bands
        for x in np.arange(x_drop + 3.0, Y["end_x"] - 0.5, 3.0):
            ring(b, F(x, by, z), "x", r + 0.015, 0.035, ALU, 24)                       # joints
        for x in np.arange(x_drop + 4.5, Y["end_x"] - 1.0, 6.0):                        # hand-hole covers on top
            c = np.array(F(x, by + r - 0.02, z))
            flange(b, c + [0, 0.03, 0], "y", 0.17, 0.03, ALU, 8)
        # End closure: a flange and its bolted cover.
        flange(b, F(Y["end_x"], by, z), "x", r + 0.07, 0.05, ALU, 16)
        cyl(b, F(Y["end_x"] + 0.03, by, z), F(Y["end_x"] + 0.06, by, z), r - 0.05, ALU, sides=24)
    # The wall seal plate round the three phases, its bolts.
    wx = Y["hall_x"] + 0.02
    b.box(*F(wx + 0.03, ey - 0.7, phase_z("a") - 0.75), *F(wx, ey + 0.7, phase_z("c") + 0.75), GALV)
    for p in "abc":
        flange(b, F(wx + 0.08, ey, phase_z(p)), "x", r + 0.09, 0.08, ALU, 12)
    stencil(b, F(x_drop + 1.5, by, phase_z("a") - r - 0.006), "-z", "A", 0.36)
    for x in (Y["end_x"] - 1.5,):
        stencil(b, F(x, by, phase_z("a") - r - 0.006), "-z", "A", 0.36)
    # Trestles.
    for x in Y["bents"]:
        trestle(b, F, x)
    # Taps down to the UATs (each phase's tee saddle), branches up and over to the main transformers.
    for name, ux in Y["uat"]:
        for p in "abc":
            ring(b, F(ux - 1.6, by - r + 0.06, phase_z(p)), "y", 0.31, 0.06, ALU, 20)
    for name, ph, gx in Y["gsu"]:
        if ph == "S":
            continue
        for k, p in enumerate(DELTA[ph]):
            branch(b, F, gx, k, p)
    # The dry-air unit at the foot of the hall wall: cabinet on a pad, its tube up the wall to phase a's underside.
    ax_, az_ = Y["hall_x"] + 1.9, phase_z("a") - 2.3
    b.box(*F(ax_ + 0.5, 0.0, az_ - 0.35), *F(ax_ - 0.5, 0.12, az_ + 0.35), ["concrete"] * 5 + [None])
    b.box(*F(ax_ + 0.4, 0.12, az_ - 0.28), *F(ax_ - 0.4, 1.5, az_ + 0.28), [GREY, GREY, GREY, GREY, GREY, None])
    b.box(*F(ax_ + 0.43, 1.5, az_ - 0.31), *F(ax_ - 0.43, 1.54, az_ + 0.31), GREY)
    face_quad(b, F(ax_, 0.65, az_ - 0.282), "-z", 0.5, 0.5, "louvre")
    gauge(b, F(ax_ - 0.2, 1.2, az_ - 0.28), "-z", 0.05, FACES["air"], 0.7, 0, 1.0, start=180, sweep=180, channel="ipb_air", case=GREY)
    lamp(b, F(ax_ + 0.15, 1.2, az_ - 0.28), "-z", "green", True)
    tag(b, F(ax_, 1.4, az_ - 0.284), "-z", "IPB DRY AIR", 0.36, 0.045)
    tag(b, F(ax_ + 0.15, 1.1, az_ - 0.284), "-z", "RUN", 0.1, 0.03)
    wx = Y["hall_x"] + 0.1
    zt = phase_z("a")
    path(b, [F(ax_ - 0.25, 1.54, az_), F(ax_ - 0.25, 1.75, az_), F(wx, 1.75, az_), F(wx, ey - 1.5, az_),
             F(wx, ey - 1.5, zt), F(wx + 0.25, ey - 1.5, zt), F(wx + 0.25, ey - r + 0.02, zt)], 0.016, COPPER)
    for y in (2.6, 4.0, 5.4, 6.8):
        b.box(*F(wx - 0.08, y - 0.03, az_ - 0.03), *F(wx + 0.04, y + 0.03, az_ + 0.03), GALV)
    for z in (az_ + 0.8, az_ + 1.6):
        b.box(*F(wx - 0.08, ey - 1.53, z - 0.03), *F(wx + 0.04, ey - 1.47, z + 0.03), GALV)
    return b


def ibeam(b, a, c, w, d, mat=GREY):
    """A wide-flange steel member from a to c (axis-aligned): two flanges and the web, w across the flanges,
    d deep; the web is along the member's second axis (across x for z members, across z for x and y members)."""
    a, c = np.minimum(a, c), np.maximum(a, c)
    L = c - a
    ax = int(np.argmax(L))
    mid = (a + c) / 2
    if ax == 1:            # a column: flanges face +-z, web along z
        for s in (-1, 1):
            b.box(mid[0] - w / 2, a[1], mid[2] + s * d / 2 - (0.025 if s > 0 else 0), mid[0] + w / 2, c[1],
                  mid[2] + s * d / 2 + (0.025 if s < 0 else 0), [mat] * 4 + [None, None])
        b.box(mid[0] - 0.012, a[1], mid[2] - d / 2 + 0.025, mid[0] + 0.012, c[1], mid[2] + d / 2 - 0.025, [None, None, mat, mat, None, None])
    else:                  # a beam along x or z: flanges top and bottom
        lo, hi = a.copy(), c.copy()
        other = 2 if ax == 0 else 0
        lo[other], hi[other] = mid[other] - w / 2, mid[other] + w / 2
        b.box(*_rnd([lo[0], c[1] - 0.025, lo[2]]), *_rnd([hi[0], c[1], hi[2]]), mat)
        b.box(*_rnd([lo[0], a[1], lo[2]]), *_rnd([hi[0], a[1] + 0.025, hi[2]]), mat)
        lo[other], hi[other] = mid[other] - 0.012, mid[other] + 0.012
        f = [mat, mat, mat, mat, None, None]
        if ax == 0:
            f = [mat, mat, None, None, None, None]
        else:
            f = [None, None, mat, mat, None, None]
        b.box(*_rnd([lo[0], a[1] + 0.025, lo[2]]), *_rnd([hi[0], c[1] - 0.025, hi[2]]), f)


def trestle(b, F, x):
    """A portal trestle across the bus: two wide-flange columns on concrete piers (anchor bolts, base plates), the
    cross beam with knee braces, a saddle under each phase, a ground strap down to the grid."""
    Y = YARD
    za, zb = Y["bus_z"] - 3.75, Y["bus_z"] + 4.35
    top = Y["bus_y"] - IPB_R - 0.1 - 0.3
    for z in (za, zb):
        b.box(*F(x + 0.35, -0.2, z - 0.35), *F(x - 0.35, 0.15, z + 0.35), ["concrete"] * 5 + [None])
        b.box(*F(x + 0.22, 0.15, z - 0.22), *F(x - 0.22, 0.18, z + 0.22), GALV)
        for dx in (-0.15, 0.15):
            for dz in (-0.15, 0.15):
                cyl(b, F(x + dx, 0.18, z + dz), F(x + dx, 0.24, z + dz), 0.022, BLACK, sides=6)
        ibeam(b, np.array(F(x - 0.12, 0.18, z - 0.15)), np.array(F(x + 0.12, top, z + 0.15)), 0.24, 0.3)
        s = 1 if z == za else -1
        bar(b, F(x, top - 1.2, z + s * 0.16), F(x, top - 0.04, z + s * 1.3), 0.1, GREY)     # knee brace
    ibeam(b, np.array(F(x - 0.12, top, za - 0.25)), np.array(F(x + 0.12, top + 0.3, zb + 0.25)), 0.24, 0.3)
    for p in "abc":
        z = phase_z(p)
        b.box(*F(x + 0.16, top + 0.3, z - 0.3), *F(x - 0.16, Y["bus_y"] - IPB_R + 0.02, z + 0.3), GALV)
    path(b, [F(x - 0.13, 0.6, za), F(x - 0.2, 0.6, za), F(x - 0.2, 0.02, za), F(x - 0.2, 0.02, za - 0.6)], 0.018, COPPER)


def branch(b, F, gx, k, p):
    """One branch of the bus up from phase p's top and north over the bay to main transformer gx's LV riser k
    (k 0: the riser at the tank's z 4.0, straight; k 1: the riser at z 3.0, offset a block east and jogged back)."""
    Y = YARD
    by, hy, r = Y["bus_y"], Y["branch_y"], IPB_R
    rx = gx - 7.2
    bx = rx if k == 0 else gx - 8.2
    zr = Y["gsu_z"] + (4.0 if k == 0 else 3.0)
    zp = phase_z(p)
    cyl(b, F(bx, by, zp), F(bx, hy + r, zp), r, ALU, sides=24)                                  # up from the phase
    ring(b, F(bx, by + r + 0.06, zp), "y", r + 0.015, 0.05, ALU, 24)                            # its tee saddle
    cyl(b, F(bx, hy, zp + r), F(bx, hy, zr - (r if k == 0 else -r)), r, ALU, sides=24)           # north over the bay
    ring(b, F(bx, hy, zp - 0.45), "z", r + 0.015, 0.035, ALU, 24)
    if k == 1:
        cyl(b, F(bx - r, hy, zr), F(rx, hy, zr), r, ALU, sides=24)                                # the jog west
    for z in np.arange(zp - 3.0, zr + 1.0, -3.0):
        ring(b, F(bx, hy, z), "z", r + 0.015, 0.035, ALU, 24)
    stencil(b, F(bx, by + 0.95, zp - r - 0.006), "-z", p.upper(), 0.3)                         # its phase letter
    # A support: a trestle post on the walkway side of the bay's back curb under each branch.
    zs = Y["gsu_z"] + 10.6
    post_top = hy - r - 0.1
    b.box(*F(bx + 0.3, -0.2, zs - 0.3), *F(bx - 0.3, 0.15, zs + 0.3), ["concrete"] * 5 + [None])
    b.box(*F(bx + 0.18, 0.15, zs - 0.18), *F(bx - 0.18, 0.18, zs + 0.18), GALV)
    ibeam(b, np.array(F(bx - 0.1, 0.18, zs - 0.12)), np.array(F(bx + 0.1, post_top - 0.25, zs + 0.12)), 0.2, 0.24)
    b.box(*F(bx + 0.35, post_top - 0.25, zs - 0.2), *F(bx - 0.35, post_top, zs + 0.2), GREY)
    b.box(*F(bx + 0.3, post_top, zs - 0.15), *F(bx - 0.3, hy - r + 0.02, zs + 0.15), GALV)


# ---------------------------------------------------------------- the 345 kV strain bus and its dead-end gantries
#
# Real basis: each single-phase main transformer's 345 kV bushing is jumpered up to one phase of a strung (strain)
# bus over the row: twin-bundle ACSR (two ~30 mm sub-conductors 0.45 m apart, spacers every ~8 m) dead-ended both ends
# on lattice-steel H-frame gantries through double strain strings of 18 glass/porcelain discs with yoke plates,
# grading rings and compression dead-end clamps; about 1.2 m sag over the ~50 m span; phases 4 m apart and at least
# 3 m from steel. The east gantry is the switchyard's incoming structure: its lower beam carries three station post
# insulators whose terminal pads take the jumpers down off the dead-ends (the switchyard's rigid bus starts there).
# Two overhead shield wires between the gantries' peaks; every leg on a concrete pier, grounded; step bolts up one leg
# behind a locked anti-climb gate.

GANTRY = dict(west=211.0, east=260.5, zs=(40.3, 54.3), beam_y=17.5, low_beam_y=9.5, peak=20.9,
              phases={"A": 43.3, "B": 47.3, "C": 51.3}, sag=1.2, string=3.3)


def yard_F(origin):
    return lambda wx, wy, wz: (origin[0] - wx, wy - origin[1], wz - origin[2])


def strain_bus(b=None):
    """Both gantries, the strain strings, the twin-bundle phases, the taps down to the bushings, the jumpers to
    the east gantry's post insulators, the shield wires. Frame: site coordinates mirrored about the west gantry."""
    if b is None:
        b = Board(9, 1301, face=0.0)
        b.live = []
    G, Y = GANTRY, YARD
    O = (G["west"], 0.0, G["zs"][0])
    F = yard_F(O)
    for x in (G["west"], G["east"]):
        gantry(b, F, x, low=(x == G["east"]))
    xw, xe = G["west"] + 0.5 + G["string"], G["east"] - 0.5 - G["string"]
    hv = {}
    for name, ph, gx in Y["gsu"]:
        if ph != "S":
            t = TRANSFORMER_HV
            hv[ph] = (gx - t[0], t[1] + Y["plinth"], Y["gsu_z"] + t[2])
    for ph, zc in G["phases"].items():
        strain_string(b, F, G["west"] + 0.5, zc, +1)
        strain_string(b, F, G["east"] - 0.5, zc, -1)
        y0 = G["beam_y"]
        cy = lambda x: y0 - G["sag"] * 4 * (x - xw) * (xe - x) / (xe - xw) ** 2
        xs = np.linspace(xw, xe, 25)
        for dz in (-0.225, 0.225):
            for xa, xb_ in zip(xs, xs[1:]):
                rod(b, F(xa, cy(xa), zc + dz), F(xb_, cy(xb_), zc + dz), 0.016, sw("mgrey"), 6)
        for x in np.arange(xw + 4.0, xe - 2.0, 8.0):                                      # spacers
            rod(b, F(x, cy(x), zc - 0.24), F(x, cy(x), zc + 0.24), 0.022, ALU, 6)
        # The tap down to the bushing: a T-connector across the bundle, the jumper in a smooth curve.
        hx, hy_, hz = hv[ph]
        tx_ = hx + 1.2
        rod(b, F(tx_, cy(tx_) - 0.03, zc - 0.28), F(tx_, cy(tx_) - 0.03, zc + 0.28), 0.035, ALU, 8)
        p0 = np.array([tx_, cy(tx_) - 0.06, zc]); p2 = np.array([hx, hy_ + 0.02, hz])
        p1 = np.array([(p0[0] + p2[0]) / 2 + 0.3, p2[1] + 0.4, (p0[2] + p2[2]) / 2])
        pts = [(1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t ** 2 * p2 for t in np.linspace(0, 1, 14)]
        for pa, pb in zip(pts, pts[1:]):
            rod(b, F(*pa), F(*pb), 0.018, sw("mgrey"), 6)
        cyl(b, F(hx, hy_ - 0.02, hz), F(hx, hy_ + 0.08, hz), 0.06, ALU, sides=10)          # its terminal clamp
        # At the east gantry: the jumper off the dead-end clamp down to the post insulator's pad.
        p0 = np.array([xe + 0.25, y0 - 0.05, zc]); p2 = np.array([G["east"], POST_TOP, zc])
        p1 = np.array([(p0[0] + p2[0]) / 2 - 0.2, p2[1] - 0.6, zc])
        pts = [(1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t ** 2 * p2 for t in np.linspace(0, 1, 12)]
        for dz in (-0.12, 0.12):
            for pa, pb in zip(pts, pts[1:]):
                rod(b, F(pa[0], pa[1], pa[2] + dz), F(pb[0], pb[1], pb[2] + dz), 0.016, sw("mgrey"), 6)
    # Shield wires peak to peak.
    for z in G["zs"]:
        xs = np.linspace(G["west"], G["east"], 21)
        cy = lambda x: G["peak"] - 0.9 * 4 * (x - G["west"]) * (G["east"] - x) / (G["east"] - G["west"]) ** 2
        for xa, xb_ in zip(xs, xs[1:]):
            rod(b, F(xa, cy(xa), z), F(xb_, cy(xb_), z), 0.008, GALV, 4)
    return b


TRANSFORMER_HV = (0.0876, 12.3177, 3.5)       # the main transformer's HV terminal in its frame (gsu().hv_top)
POST_TOP = 12.9


def lattice(b, F, c0, c1, w, axis, panel=1.5, leg=0.1, mat=GALV, open_faces=(), ends=(True, True)):
    """A square lattice member (a column or a beam) from c0 to c1 (site coords, along one axis): four angle legs,
    struts every panel on each face, a zig-zag diagonal per panel per face (set inside the legs so no faces fight)."""
    c0, c1 = np.array(c0, float), np.array(c1, float)
    ax = {"x": 0, "y": 1, "z": 2}[axis]
    others = [i for i in range(3) if i != ax]
    L = abs(c1[ax] - c0[ax])
    n = max(1, int(round(L / panel)))
    corners = []
    for s0 in (-1, 1):
        for s1 in (-1, 1):
            o = np.zeros(3); o[others[0]] = s0 * (w / 2 - leg / 2); o[others[1]] = s1 * (w / 2 - leg / 2)
            corners.append(o)
            a, e = c0 + o, c1 + o
            lo, hi = np.minimum(F(*a), F(*e)), np.maximum(F(*a), F(*e))
            h = np.zeros(3); h[others[0]] = leg / 2; h[others[1]] = leg / 2
            b.box(*_rnd(np.minimum(lo - h, hi + h)), *_rnd(np.maximum(lo - h, hi + h)), mat)
    d = (c1 - c0) / n
    for face_i, (oi, s) in enumerate(((others[0], -1), (others[0], 1), (others[1], -1), (others[1], 1))):
        if (oi, s) in open_faces:
            continue
        oj = others[1] if oi == others[0] else others[0]
        for k in range(n + 1):
            p = c0 + d * k
            for depth, t, members in ((0.035, 0.05, "strut"), (0.055 + 0.012 * (k % 2), 0.04, "diag")):
                q = p.copy(); q[oi] += s * (w / 2 - depth)
                a_, e_ = q.copy(), q.copy(); a_[oj] -= w / 2 - leg; e_[oj] += w / 2 - leg
                if members == "strut":
                    if (k == 0 and not ends[0]) or (k == n and not ends[1]):
                        continue
                    bar(b, F(*a_), F(*e_), t, mat)
                elif k < n:
                    a2, e2 = a_.copy(), e_.copy()
                    if (k + face_i) % 2:
                        a2, e2 = e_.copy(), a_.copy()
                    bar(b, F(*a2), F(*(e2 + d)), t, mat)


def gantry(b, F, x, low=False):
    """An H-frame lattice dead-end gantry across the row at x: two columns on piers, the top beam, the peaks for the
    shield wires, step bolts and a locked anti-climb gate on the south column; low: the lower beam with three station
    post insulators."""
    G = GANTRY
    w = 1.0
    for z in G["zs"]:
        b.box(*F(x + 0.75, -0.3, z - 0.75), *F(x - 0.75, 0.2, z + 0.75), ["concrete"] * 5 + [None])
        for dx in (-0.4, 0.4):
            for dz in (-0.4, 0.4):
                b.box(*F(x + dx + 0.12, 0.2, z + dz - 0.12), *F(x + dx - 0.12, 0.23, z + dz + 0.12), GALV)
                cyl(b, F(x + dx, 0.23, z + dz), F(x + dx, 0.3, z + dz), 0.025, BLACK, sides=6)
        lattice(b, F, (x, 0.23, z), (x, G["beam_y"] + 0.5, z), w, "y", ends=(True, False))
        lattice(b, F, (x, G["beam_y"] + 0.5, z), (x, G["peak"], z), 0.5, "y", panel=1.2, leg=0.07)
        cyl(b, F(x, G["peak"], z), F(x, G["peak"] + 0.15, z), 0.06, GALV, sides=8)
        path(b, [F(x - 0.45, 0.6, z + 0.45), F(x - 0.55, 0.6, z + 0.55), F(x - 0.55, 0.02, z + 0.55), F(x - 0.55, 0.02, z + 1.1)], 0.018, COPPER)
    zs, zn = G["zs"]
    lattice(b, F, (x, G["beam_y"], zs + w / 2), (x, G["beam_y"], zn - w / 2), w, "z", open_faces=())
    if low:
        lattice(b, F, (x, G["low_beam_y"], zs + w / 2), (x, G["low_beam_y"], zn - w / 2), 0.8, "z", leg=0.08)
        for ph, zc in G["phases"].items():
            post_insulator(b, F, x, G["low_beam_y"] + 0.4, zc)
    # Step bolts up the south column's west leg, the locked gate across the bottom of that face.
    zc = zn
    for y in np.arange(2.6, G["beam_y"], 0.4):
        rod(b, F(x - 0.5, y, zc - 0.45), F(x - 0.68, y, zc - 0.45), 0.012, GALV, 6)
    b.box(*F(x + 0.02 - 0.5 - 0.06, 0.4, zc - 0.5), *F(x - 0.5 - 0.06, 2.5, zc + 0.5), GALV)
    b.box(*F(x - 0.5 - 0.08 + 0.02, 1.3, zc - 0.1), *F(x - 0.5 - 0.15, 1.45, zc - 0.02), YELLOW)


def strain_string(b, F, x0, zc, s):
    """A double strain string from the beam face at x0 out along s (+1 east): yoke plates, two strings of 18 discs,
    the grading ring and the dead-end clamps for the twin bundle."""
    G = GANTRY
    y = G["beam_y"]
    L = G["string"]
    xa = x0 + s * 0.1
    bar(b, F(x0, y, zc), F(xa, y, zc), 0.08, GALV)                                         # its shackle
    b.box(*F(max(xa, xa + s * 0.04), y - 0.32, zc - 0.03), *F(min(xa, xa + s * 0.04), y + 0.32, zc + 0.03), GALV)   # yoke
    for dy in (-0.22, 0.22):
        x1, x2 = xa + s * 0.06, xa + s * (L - 0.35)
        rod(b, F(x1, y + dy, zc), F(x2, y + dy, zc), 0.025, GALV, 6)
        for k in range(18):
            xd = x1 + s * (0.12 + k * (L - 0.6) / 18)
            cyl(b, F(xd, y + dy, zc), F(xd + s * 0.035, y + dy, zc), 0.13, "glass", sides=12)
            cyl(b, F(xd + s * 0.035, y + dy, zc), F(xd + s * 0.06, y + dy, zc), 0.06, "glass", sides=10)
    xb_ = xa + s * (L - 0.3)
    b.box(*F(max(xb_, xb_ + s * 0.04), y - 0.32, zc - 0.03), *F(min(xb_, xb_ + s * 0.04), y + 0.32, zc + 0.03), GALV)
    ring(b, F(xb_ - s * 0.25, y, zc), "x", 0.38, 0.05, ALU, 20)                             # grading ring
    for dz in (-0.225, 0.225):
        bar(b, F(xb_ + s * 0.04, y, zc), F(xb_ + s * 0.12, y - 0.04, zc + dz), 0.04, GALV)
        cyl(b, F(xb_ + s * 0.12, y - 0.04, zc + dz), F(xb_ + s * 0.42, y - 0.06, zc + dz), 0.04, ALU, sides=8)


def post_insulator(b, F, x, y0, zc):
    """A 345 kV station post insulator on the gantry's lower beam: base flange, two stacked units of brown sheds,
    the cap and the terminal pad."""
    flange(b, F(x, y0, zc), "y", 0.2, 0.05, GALV, 8)
    h = POST_TOP - y0 - 0.15
    cyl(b, F(x, y0 + 0.03, zc), F(x, y0 + 0.03 + h, zc), 0.09, PORC, sides=12)
    for k in range(int(h / 0.12)):
        yy = y0 + 0.08 + k * 0.12
        cyl(b, F(x, yy, zc), F(x, yy + 0.025, zc), 0.2 if k % 2 == 0 else 0.16, PORC, sides=14)
    flange(b, F(x, y0 + h / 2 + 0.03, zc), "y", 0.13, 0.04, GALV, 6)
    cyl(b, F(x, y0 + h + 0.03, zc), F(x, POST_TOP - 0.02, zc), 0.12, GALV, sides=12)
    b.box(*F(x + 0.15, POST_TOP - 0.02, zc - 0.15), *F(x - 0.15, POST_TOP + 0.02, zc + 0.15), ALU)   # terminal pad


# ---------------------------------------------------------------- the deluge valve house
#
# Real basis (NFPA 15 water-spray protection of oil-filled transformers): a small heated masonry house on the plant's
# buried fire main, one 6 in deluge valve set per protected transformer (MT-1A, 1B, 1C, the spare, UAT-1A, 1B and the
# startup transformer SAT-1 by the switchyard, its line buried the whole way). Each
# riser comes up through the floor: a supervised OS&Y gate valve (tamper switch), the deluge valve (red, clapper held
# shut by water in its diaphragm chamber), its trim (priming line with strainer and restriction, solenoid release
# valve, manual pull release, pressure switch, supply and priming gauges, drain), and the outlet back down into the
# ground to the bay's riser. A releasing panel takes the transformers' heat detectors: on a fire it opens the
# solenoid, the priming water dumps and the valve flies open; the alarm line runs the water-motor gong on the
# outside wall. Supply about 125 psi; a set flows about 750 US gpm. Unit heater (it must never freeze), lights, a
# floor trench drain, a fire department connection outside.

DVH = dict(w=10.3, d=5.6, wall=0.3, h=3.7, zones=(("DV-1", "MT-1A"), ("DV-2", "MT-1B"), ("DV-3", "MT-1C"), ("DV-4", "MT-1S"),
                                                   ("DV-5", "UAT-1A"), ("DV-6", "UAT-1B"), ("DV-7", "SAT-1")))
RED = "pipe_red"


def push_button(b, c, normal, colour, r=0.022):
    c, n = np.array(c, float), _v(normal)
    cyl(b, c, c + n * 0.012, r + 0.012, sw("dgrey"), sides=12)
    cyl(b, c + n * 0.012, c + n * 0.03, r, sw(colour), sides=12)


def deluge_house(state=None):
    """The deluge valve house, front (door) toward -z. state: tripped zones (set of DV names), supply psi."""
    _faces()
    if "dv_sup" not in FACES:
        FACES["dv_sup"] = ATLAS.add("dial:dv_sup", dial_img(0, 300, "SUPPLY", "psi", None, 0.07))
        FACES["dv_pri"] = ATLAS.add("dial:dv_pri", dial_img(0, 300, "PRIMING", "psi", None, 0.07))
    st = dict(tripped=(), supply=125)
    st.update(state or {})
    D = DVH
    b = Board(9, 1401, face=0.0)
    b.live = []
    W, Dp, t, H = D["w"], D["d"], D["wall"], D["h"]
    c, g = "concrete", GREY
    # Slab (floor at the grass), walls with the door opening, roof slab, parapet and coping.
    b.box(-0.3, -0.25, -0.6, W + 0.3, 0.0, Dp + 0.3, [c, c, c, c, c, None])
    b.box(0.95, 0.0, -0.9, 2.15, 0.02, 0.0, c)                                               # apron at the door
    dx0, dx1, dh = 1.0, 2.1, 2.2
    cw = [c, c, c, c, c, None]
    b.box(0, 0, 0, dx0, H, t, cw); b.box(dx1, 0, 0, W, H, t, cw); b.box(dx0, dh, 0, dx1, H, t, c)
    b.box(0, 0, Dp - t, W, H, Dp, cw)
    b.box(0, 0, t, t, H, Dp - t, cw); b.box(W - t, 0, t, W, H, Dp - t, cw)
    b.box(-0.05, H, -0.05, W + 0.05, H + 0.3, Dp + 0.05, [c, c, c, c, None, c])           # roof slab
    for (x0, z0, x1, z1) in ((-0.05, -0.05, W + 0.05, 0.15), (-0.05, Dp - 0.15, W + 0.05, Dp + 0.05),
                             (-0.05, 0.15, 0.15, Dp - 0.15), (W - 0.15, 0.15, W + 0.05, Dp - 0.15)):
        b.box(x0, H + 0.3, z0, x1, H + 0.65, z1, [c, c, c, c, None, None])                 # parapet
        side = z1 - z0 > 1 and x1 - x0 < 1
        b.box(x0 - 0.03, H + 0.65, z0 + 0.03 if side else z0 - 0.03, x1 + 0.03, H + 0.7, z1 - 0.03 if side else z1 + 0.03,
              "stainless_dark")
    b.box(0.15, H + 0.3, 0.15, W - 0.15, H + 0.32, Dp - 0.15, [None] * 4 + [sw("dgrey"), None])   # roof membrane
    # A scupper and its downspout down the +x end.
    b.box(W + 0.05, H + 0.36, Dp - 0.7, W + 0.25, H + 0.46, Dp - 0.5, "stainless_dark")
    path(b, [(W + 0.2, H + 0.36, Dp - 0.6), (W + 0.2, 0.15, Dp - 0.6), (W + 0.45, 0.05, Dp - 0.6)], 0.05, "stainless_dark")
    for y in (1.0, 2.4):
        b.box(W + 0.05, y - 0.03, Dp - 0.64, W + 0.15, y + 0.03, Dp - 0.56, "stainless_dark")
    # Door frame and the steel door, open outward 95 degrees on its -x hinge; lever handle; the light over it.
    for x0, x1 in ((dx0 - 0.06, dx0 + 0.04), (dx1 - 0.04, dx1 + 0.06)):
        b.box(x0, 0.0, -0.04, x1, dh - 0.04, t + 0.02, [sw("dgrey")] * 4 + [None, None])
    b.box(dx0 - 0.06, dh - 0.04, -0.04, dx1 + 0.06, dh + 0.06, t + 0.02, sw("dgrey"))
    a = math.radians(95)
    u = np.array([math.cos(a), 0, -math.sin(a)])
    dc = np.array([dx0, dh / 2, -0.04]) + u * (dx1 - dx0) / 2 + np.array([0, 0.01, 0])
    b.obox(tuple(_rnd(dc - np.array([math.sin(a), 0, math.cos(a)]) * 0.025)), tuple(_rnd(u)), (0, 1, 0),
           round((dx1 - dx0) / 2 - 0.01, 4), round(dh / 2 - 0.01, 4), 0.025, sw("dgrey"))
    for y in (0.3, 1.1, 1.9):
        cyl(b, (dx0 - 0.03, y - 0.06, -0.04), (dx0 - 0.03, y + 0.06, -0.04), 0.02, BLACK, sides=8)
    hp = np.array([dx0, 1.0, -0.04]) + u * (dx1 - dx0 - 0.1)
    for s in (-1, 1):
        n_ = np.array([math.sin(a), 0, math.cos(a)]) * s
        rod(b, hp + n_ * 0.03, hp + n_ * 0.08, 0.01, ALU, 6)
        rod(b, hp + n_ * 0.08, hp + n_ * 0.08 - u * 0.12, 0.01, ALU, 6)
    b.box(1.35, dh + 0.25, -0.12, 1.75, dh + 0.4, 0.0, sw("dgrey"))                         # wall light
    b.box(1.38, dh + 0.22, -0.11, 1.72, dh + 0.25, -0.01, CB.LENS["white"], b.lit)
    stencil(b, (5.2, 2.75, -0.003), "-z", "DELUGE VALVE HOUSE", 0.24)
    # Louvres high on both ends (with their frames).
    for x, n_ in ((-0.003, "-x"), (W + 0.003, "x")):
        face_quad(b, (x, 2.9, Dp / 2), n_, 0.9, 0.6, "louvre")
    # Fire department connection on the front: a Siamese inlet with caps, its tag; the pipe into the wall.
    fx = 3.2
    cyl(b, (fx, 0.9, 0.0), (fx, 0.9, -0.2), 0.08, RED, sides=12)
    b.box(fx - 0.22, 0.82, -0.3, fx + 0.22, 0.98, -0.18, RED)
    for s in (-1, 1):
        cyl(b, (fx + s * 0.12, 0.9, -0.3), (fx + s * 0.12, 0.9, -0.38), 0.065, ALU, sides=12)
        cyl(b, (fx + s * 0.12, 0.9, -0.38), (fx + s * 0.12, 0.9, -0.42), 0.07, ALU, sides=8)
    tag(b, (fx, 1.17, -0.004), "-z", "FDC  DELUGE SUPPLY", 0.42, 0.045)
    # Inside: the six valve sets along the back wall.
    for k, (dv, zone) in enumerate(D["zones"]):
        valve_set(b, 1.25 + k * 1.3, dv, zone, dv in st["tripped"], st["supply"])
    # Cable tray along the back wall top, down the +x end and along the front to the panel.
    zt = Dp - t - 0.25
    b.box(0.6, 3.05, zt - 0.15, W - 0.5, 3.07, zt + 0.15, GALV)
    for z in (zt - 0.15, zt + 0.15):
        b.box(0.6, 3.07, z - 0.01, W - 0.5, 3.15, z + 0.01, GALV)
    for x in np.arange(1.0, W - 0.5, 1.3):
        b.box(x - 0.02, 3.03, zt + 0.16, x + 0.02, 3.065, Dp - t, GALV)                  # its wall brackets
    px0, px1 = 5.4, 6.6
    path(b, [(W - 0.6, 3.1, zt), (W - 0.6, 3.1, t + 0.2), (px1 - 0.2, 3.1, t + 0.2), (px1 - 0.2, 2.35, t + 0.2)], 0.025, GALV)
    releasing_panel(b, px0, px1, st)
    # Unit heater in the -x back corner on hanger rods, its conduit; two ceiling lights.
    hx, hz = 0.75, Dp - 0.9
    b.box(hx - 0.3, 2.6, hz - 0.25, hx + 0.3, 3.05, hz + 0.25, g)
    face_quad(b, (hx, 2.82, hz - 0.253), "-z", 0.5, 0.36, "louvre")
    for dx in (-0.2, 0.2):
        rod(b, (hx + dx, 3.05, hz), (hx + dx, H, hz), 0.012, GALV, 6)
    path(b, [(hx + 0.3, 2.9, hz), (hx + 0.5, 2.9, hz), (hx + 0.5, 3.1, hz), (hx + 0.5, 3.1, zt)], 0.02, GALV)
    for x in (3.0, 7.3):
        b.box(x - 0.6, H - 0.12, 2.6, x + 0.6, H, 3.0, g)
        b.box(x - 0.55, H - 0.13, 2.65, x + 0.55, H - 0.12, 2.95, CB.LENS["white"], b.lit)
    # The floor's trench drain in front of the valves, its grating.
    b.box(0.7, -0.02, 3.55, W - 0.7, 0.004, 3.85, ["grating"] * 4 + ["grating", None])
    return b


def valve_set(b, x, dv, zone, tripped, supply):
    """One deluge valve set at x against the back wall: the riser up through the floor, OS&Y gate valve with tamper
    switch, the deluge valve and its trim, the outlet back down into the floor; tags; the gong outside."""
    D = DVH
    zp = D["d"] - D["wall"] - 0.75            # the supply riser's line
    zo = D["d"] - D["wall"] - 0.32            # the outlet's
    r = 0.085
    flange(b, (x, 0.04, zp), "y", 0.15, 0.04, RED, 8)
    cyl(b, (x, 0.0, zp), (x, 0.5, zp), r, RED, sides=14)
    gate_valve(b, (x, 0.72, zp), "y", "-z", 0.085, RED, sw("red"))
    b.box(x + 0.09, 0.95, zp - 0.36, x + 0.2, 1.08, zp - 0.26, sw("dgrey"))                # tamper switch
    cyl(b, (x, 0.92, zp), (x, 1.15, zp), r, RED, sides=14)
    # The deluge valve: flanged body, the diaphragm chamber bulging to the front, its cover bolts.
    flange(b, (x, 1.17, zp), "y", 0.17, 0.04, RED, 8)
    cyl(b, (x, 1.19, zp), (x, 1.61, zp), 0.15, RED, sides=16)
    flange(b, (x, 1.63, zp), "y", 0.17, 0.04, RED, 8)
    cyl(b, (x, 1.4, zp - 0.12), (x, 1.4, zp - 0.24), 0.12, RED, sides=16)
    flange(b, (x, 1.4, zp - 0.25), "-z", 0.14, 0.03, RED, 8)
    # Trim: priming line (strainer, restriction, solenoid, pull release) from below the valve into the chamber.
    tz = zp - 0.3
    path(b, [(x - 0.09, 1.0, zp - 0.07), (x - 0.32, 1.0, zp - 0.07), (x - 0.32, 1.0, tz), (x - 0.32, 1.4, tz),
             (x - 0.1, 1.4, tz)], 0.012, COPPER)
    cyl(b, (x - 0.32, 1.06, tz), (x - 0.32, 1.16, tz), 0.03, ALU, sides=8)                      # strainer
    gauge(b, (x - 0.32, 1.66, tz), "-z", 0.07, FACES["dv_pri"], 0 if tripped else supply, 0, 300, channel=f"{dv}_prime", case=GREY)
    rod(b, (x - 0.32, 1.4, tz), (x - 0.32, 1.66, tz), 0.012, COPPER, 6)
    b.box(x - 0.42, 1.22, tz - 0.07, x - 0.22, 1.34, tz + 0.05, sw("dgrey"))                   # solenoid valve
    cyl(b, (x - 0.32, 1.34, tz - 0.01), (x - 0.32, 1.42, tz - 0.01), 0.035, BLACK, sides=10)
    b.box(x + 0.2, 1.25, tz - 0.06, x + 0.38, 1.55, tz + 0.06, sw("red"))                        # manual pull release
    bar(b, (x + 0.29, 1.47, tz - 0.07), (x + 0.29, 1.35, tz - 0.07), 0.03, sw("white"), 0.015)
    path(b, [(x + 0.2, 1.3, tz), (x + 0.12, 1.3, tz), (x + 0.12, 1.3, zp - 0.12)], 0.012, COPPER)
    # Supply gauge below the valve; pressure switch on the alarm line; drain to the trench.
    path(b, [(x + 0.09, 1.05, zp - 0.05), (x + 0.2, 1.05, zp - 0.05), (x + 0.2, 1.05, tz)], 0.012, COPPER)
    gauge(b, (x + 0.2, 1.05, tz - 0.012), "-z", 0.07, FACES["dv_sup"], supply, 0, 300, channel=f"{dv}_supply", case=GREY)
    b.box(x + 0.1, 1.68, zp - 0.15, x + 0.24, 1.8, zp - 0.05, sw("dgrey"))                       # pressure switch
    path(b, [(x + 0.17, 1.8, zp - 0.1), (x + 0.17, 3.1, zp - 0.1), (x + 0.17, 3.1, D["d"] - D["wall"] - 0.25)], 0.012, GALV)
    path(b, [(x - 0.42, 1.28, tz), (x - 0.5, 1.28, tz), (x - 0.5, 3.1, tz), (x - 0.5, 3.1, D["d"] - D["wall"] - 0.25)], 0.012, GALV)
    path(b, [(x + 0.08, 1.25, zp + 0.06), (x + 0.3, 1.25, zp + 0.06), (x + 0.3, 0.3, zp + 0.06), (x + 0.3, 0.3, 3.7),
             (x + 0.3, 0.02, 3.7)], 0.02, RED)
    ball_valve(b, (x + 0.3, 0.6, zp + 0.06), "y", "-z", 0.025, RED)
    # The outlet: up, back, down into the floor to the bay; its tag on the wall above.
    cyl(b, (x, 1.65, zp), (x, 2.4 + r, zp), r, RED, sides=14)
    cyl(b, (x, 2.4, zp - r), (x, 2.4, zo + r), r, RED, sides=14)
    cyl(b, (x, 2.4 + r, zo), (x, 0.0, zo), r, RED, sides=14)
    flange(b, (x, 0.04, zo), "y", 0.15, 0.04, RED, 8)
    for y in (0.8, 1.9):
        b.box(x - 0.04, y - 0.04, zo + r, x + 0.04, y + 0.04, D["d"] - D["wall"], GALV)           # its wall brackets
    tag(b, (x, 2.75, D["d"] - D["wall"] - 0.004), "-z", f"{dv}  {zone}", 0.42, 0.05)
    # The alarm line out through the wall to the water-motor gong.
    gx, gy = x, 2.4
    cyl(b, (gx, gy, D["d"]), (gx, gy, D["d"] + 0.12), 0.07, RED, sides=12)
    cyl(b, (gx, gy, D["d"] + 0.12), (gx, gy, D["d"] + 0.22), 0.16, RED, sides=16)
    cyl(b, (gx, gy, D["d"] + 0.22), (gx, gy, D["d"] + 0.25), 0.1, RED, sides=12)
    path(b, [(gx, gy - 0.16, D["d"] + 0.1), (gx, 0.15, D["d"] + 0.1), (gx, 0.05, D["d"] + 0.35)], 0.02, GALV)


def releasing_panel(b, x0, x1, st):
    """The deluge releasing panel on the front wall's inside, facing into the house: a zone row each (tag, ARMED,
    TRIPPED), AC POWER and TROUBLE, SILENCE and RESET buttons, the key switch. (It faces +z, so the viewer's right is
    -x: X(o) is o across from the viewer's left.)"""
    D = DVH
    z = D["wall"]
    y0, y1 = 1.0, 2.45
    X = lambda o: x1 - o
    b.box(x0, y0, z, x1, y1, z + 0.22, [None, GREY, GREY, GREY, GREY, GREY])
    f = z + 0.224
    tag(b, ((x0 + x1) / 2, y1 - 0.1, f), "z", "DELUGE RELEASING PANEL", 0.8, 0.05)
    tag(b, (X(0.62), y1 - 0.2, f), "z", "ARMED", 0.14, 0.03)
    tag(b, (X(0.85), y1 - 0.2, f), "z", "TRIPPED", 0.16, 0.03)
    for k, (dv, zone) in enumerate(D["zones"]):
        y = y1 - 0.3 - k * 0.11
        tag(b, (X(0.27), y, f), "z", f"{dv}  {zone}", 0.36, 0.035)
        lamp(b, (X(0.62), y, f), "z", "green", dv not in st["tripped"], 0.018)
        lamp(b, (X(0.85), y, f), "z", "red", dv in st["tripped"], 0.018)
    y = y0 + 0.24
    for o, col, on, txt in ((0.2, "white", True, "AC POWER"), (0.47, "amber", False, "TROUBLE")):
        tag(b, (X(o), y + 0.08, f), "z", txt, 0.2, 0.03)
        lamp(b, (X(o), y, f), "z", col, on, 0.018)
    for o, txt in ((0.72, "SILENCE"), (0.95, "RESET")):
        tag(b, (X(o), y + 0.08, f), "z", txt, 0.18, 0.03)
        push_button(b, (X(o), y, f), "z", "black")
    b.box(X(1.08), y0 + 0.06, f, X(1.0), y0 + 0.14, f + 0.02, ALU)                              # key switch
    b.box(X(1.05), y0 + 0.08, f + 0.02, X(1.03), y0 + 0.12, f + 0.05, BLACK)


# ---------------------------------------------------------------- switchyard: the 345 kV SF6 dead-tank breaker
#
# Real basis: a 362 kV, 4000 A, 63 kA dead-tank SF6 circuit breaker (as the big makers build them): three grounded
# aluminium-alloy tanks, one per pole, on a common steel frame about 2 m up; each tank carries two SF6-insulated
# bushings in a V (brown porcelain sheds, aluminium terminal pads, corona rings) with a multi-ratio bushing CT in a
# housing at each foot. One spring-operated mechanism (motor-charged closing spring, trip springs) in a cabinet at one
# end drives all three poles through a linkage under the tanks. On the cabinet: position flag (CLOSED red / OPEN
# green), spring indicator (CHARGED yellow), operations counter, LOCAL/REMOTE switch, control switch with its red and
# green lamps. A temperature-compensated SF6 density monitor per pole at reach height: rated 0.64 MPa abs, alarm 0.55,
# lockout 0.52 (fill valve beside it). Concrete pad, anchor bolts, every leg grounded.
#
# In Jugcraft the switchyard breakers are cosmetic (Justin, 18 Oct): they follow the plant on their own, the flag,
# lamps and counter change when they open or close, nobody has to operate them.

BKR = dict(poles=(1.5, 5.5, 9.5), tank_y=2.9, tank_r=0.62, tz0=1.4, tz1=4.6, legs=(2.2, 3.8), cab=(-1.75, -0.25))


def flag_img(text, bg, fg, w_m, h_m):
    w, h = int(w_m * PXB), int(h_m * PXB)
    im = Image.new("RGBA", (w, h), bg + (255,))
    d = ImageDraw.Draw(im)
    fs = h * 0.62
    while d.textlength(text, font=_font(fs)) > w * 0.9 and fs > 6:
        fs -= 1
    d.text((w / 2, h / 2), text, font=_font(fs), fill=fg, anchor="mm")
    return im


def counter_img(n, w_m=0.1, h_m=0.03):
    w, h = int(w_m * PXB), int(h_m * PXB)
    im = Image.new("RGBA", (w, h), (20, 20, 22, 255))
    d = ImageDraw.Draw(im)
    s = f"{n:05d}"
    cw = w / 5
    for i, ch in enumerate(s):
        d.rectangle([i * cw + 1, 1, (i + 1) * cw - 2, h - 2], fill=(236, 236, 228) if i < 4 else (200, 40, 40))
        d.text((i * cw + cw / 2, h / 2), ch, font=_font(h * 0.78), fill=(20, 20, 22) if i < 4 else (240, 240, 240), anchor="mm")
    return im


def bushing(b, base, axis, L, core=0.16, sheds=(0.34, 0.27), pitch=0.15):
    """A porcelain bushing from base along axis: core, alternating sheds, aluminium head, terminal pad and stud, a
    corona ring; returns the terminal pad's centre."""
    base, a = np.array(base, float), _v(axis) / np.linalg.norm(_v(axis))
    cyl(b, base, base + a * L, core, PORC, sides=14)
    for i in range(int(L / pitch)):
        c = base + a * (pitch * (i + 0.5))
        r = sheds[i % 2]
        cyl(b, c - a * 0.015, c + a * 0.015, r, PORC, sides=16)
        cyl(b, c + a * 0.015, c + a * 0.04, (r + core) / 2, PORC, sides=14)
    top = base + a * L
    cyl(b, top, top + a * 0.35, core + 0.06, ALU, sides=14)
    flange(b, top, a, core + 0.1, 0.04, ALU, 8)
    ring(b, top + a * 0.22, a, 0.42, 0.07, ALU, 20)
    stud = top + a * 0.35
    cyl(b, stud, stud + a * 0.18, 0.05, ALU, sides=8)
    pad = stud + a * 0.24
    up = np.array([0, 1.0, 0])
    side = np.cross(a, up) if abs(a[1]) < 0.99 else np.array([1.0, 0, 0])
    side = side / np.linalg.norm(side)
    b.obox(tuple(_rnd(pad)), tuple(_rnd(a)), tuple(_rnd(side)), 0.07, 0.07, 0.015, ALU)
    return pad


def sf6_breaker(name="BKR 1-1", state=None):
    """One 345 kV dead-tank breaker, front (mechanism cabinet's door, gauges, tags) toward -z."""
    _faces()
    if "sf6" not in FACES:
        FACES["sf6"] = ATLAS.add("dial:sf6", dial_img(0, 1.0, "SF6 DENSITY", "MPa abs", None, 0.07, red_below=0.52, green=(0.6, 0.7)))
        FACES["flag_closed"] = ATLAS.add("flag:closed", flag_img("CLOSED", (190, 32, 32), (245, 245, 245), 0.2, 0.07))
        FACES["flag_open"] = ATLAS.add("flag:open", flag_img("OPEN", (40, 140, 60), (245, 245, 245), 0.2, 0.07))
        FACES["flag_charged"] = ATLAS.add("flag:charged", flag_img("CHARGED", (230, 196, 40), (20, 20, 20), 0.2, 0.07))
        FACES["plate_bkr"] = ATLAS.add("plate:bkr", plate_img(
            ["SF6 DEAD TANK CIRCUIT BREAKER", "362 kV  60 Hz  BIL 1300 kV", "4000 A  63 kA  3 CYCLE", "SF6 0.64 MPa abs AT 20 C",
             "SF6 MASS 3 x 46 kg", "SPRING MECHANISM  MOTOR 125 V DC", "SERIAL 26-3311  YEAR 2026"], 0.6, 0.45))
    st = dict(closed=True, charged=True, ops=1284, sf6=(0.64, 0.63, 0.64))
    st.update(state or {})
    B = BKR
    b = Board(12, 1501, face=0.0)
    b.live = []
    c = "concrete"
    b.box(-2.2, -0.3, 0.6, 11.0, 0.15, 5.4, [c, c, c, c, c, None])
    top = 0.15
    zf = B["legs"][0]
    # Frame: two legs under each pole, a cap beam on them, two long beams the whole length.
    for xp in B["poles"]:
        for z in B["legs"]:
            ibeam(b, np.array((xp - 0.125, top + 0.03, z - 0.15)), np.array((xp + 0.125, 2.1, z + 0.15)), 0.25, 0.3)
            b.box(xp - 0.22, top, z - 0.22, xp + 0.22, top + 0.03, z + 0.22, GALV)
            for dx in (-0.15, 0.15):
                for dz in (-0.15, 0.15):
                    cyl(b, (xp + dx, top + 0.03, z + dz), (xp + dx, top + 0.09, z + dz), 0.022, BLACK, sides=6)
        ibeam(b, np.array((xp - 0.13, 2.1, B["legs"][0] - 0.35)), np.array((xp + 0.13, 2.3, B["legs"][1] + 0.35)), 0.26, 0.2)
        path(b, [(xp + 0.14, 0.6, zf + 0.15), (xp + 0.2, 0.6, zf + 0.15), (xp + 0.2, top + 0.01, zf + 0.15), (xp + 0.2, top + 0.01, zf + 0.6)],
             0.018, COPPER)
    for z in B["legs"]:
        ibeam(b, np.array((B["cab"][1] + 0.05, 1.85, z - 0.1)), np.array((B["poles"][-1] + 0.6, 2.1, z + 0.1)), 0.2, 0.25)
    tag(b, (3.5, 1.975, zf - 0.104), "-z", f"{name}  345 kV", 0.6, 0.12)
    # The poles.
    for k, xp in enumerate(B["poles"]):
        pole(b, xp, "ABC"[k], st["sf6"][k])
    # The operating linkage under the tanks, a crank box at each pole.
    rod(b, (B["cab"][1], 2.0, 3.0), (B["poles"][-1] + 0.2, 2.0, 3.0), 0.03, GALV, 8)
    for xp in B["poles"]:
        b.box(xp - 0.16, 1.9, 2.82, xp + 0.16, 2.28, 3.18, GREY)
        bar(b, (xp + 0.1, 2.0, 3.19), (xp + 0.1, 2.25, 3.19), 0.04, GALV)
    mechanism(b, name, st)
    return b


def pole(b, xp, ph, sf6):
    B = BKR
    y, r, z0, z1 = B["tank_y"], B["tank_r"], B["tz0"], B["tz1"]
    cyl(b, (xp, y, z0), (xp, y, z1), r, GREY, sides=24)
    for z, s in ((z0, -1), (z1, 1)):
        cyl(b, (xp, y, z), (xp, y, z + s * 0.1), r - 0.1, GREY, sides=24)
        cyl(b, (xp, y, z + s * 0.1), (xp, y, z + s * 0.16), r - 0.3, GREY, sides=20)
    flange(b, (xp, y, 3.0), "z", r + 0.07, 0.06, GREY, 20)
    for z in (2.1, 3.9):                                                                       # saddles
        b.box(xp - 0.42, 2.3, z - 0.07, xp + 0.42, 2.48, z + 0.07, GREY)
    stencil(b, (xp, y, z0 - 0.166), "-z", ph, 0.3)
    for zt, s in ((1.95, -1), (4.05, 1)):
        a = np.array([0, math.cos(math.radians(18)), s * math.sin(math.radians(18))])
        base = np.array([xp, y + r - 0.12, zt])
        cyl(b, base, base + a * 0.8, 0.4, GREY, sides=20)                                     # CT housing
        flange(b, base + a * 0.8, a, 0.46, 0.05, GREY, 12)
        bt = base + a * 0.4 + np.array([0.4, 0, 0])
        b.box(*_rnd(bt + [0, -0.12, -0.1]), *_rnd(bt + [0.14, 0.12, 0.1]), GREY)              # its CT terminal box
        bushing(b, base + a * 0.84, a, 4.3)
    # SF6 density monitor and fill valve at reach height on the front leg, the tube up to the tank.
    gx, zf = xp + 0.36, B["legs"][0] - 0.15
    b.box(xp + 0.125, 1.35, zf - 0.03, xp + 0.5, 1.75, zf, GREY)
    gauge(b, (gx, 1.6, zf - 0.03), "-z", 0.07, FACES["sf6"], sf6, 0, 1.0, channel=f"sf6_{ph}", case=GREY)
    cyl(b, (gx, 1.42, zf - 0.03), (gx, 1.42, zf - 0.08), 0.025, ALU, sides=8)
    cyl(b, (gx, 1.42, zf - 0.08), (gx, 1.42, zf - 0.11), 0.03, BLACK, sides=8)
    path(b, [(gx, 1.75, zf + 0.0), (gx, 1.8, zf + 0.0), (gx, 1.8, 2.45), (gx, y - math.sqrt(r * r - 0.36 ** 2) + 0.03, 2.45)], 0.01, COPPER)
    tag(b, (gx, 1.47, zf - 0.034), "-z", f"SF6  POLE {ph}", 0.2, 0.03)


def mechanism(b, name, st):
    """The spring mechanism cabinet at the -x end, on legs, door open: closing spring and motor on the left, the
    flag, spring indicator and counter, the control switches and lamps, terminal strip."""
    B = BKR
    x0, x1 = B["cab"]
    y0, y1 = 0.75, 2.75
    zw = 3.3                                    # its back
    zfr = 2.05                                  # its front
    g = GREY
    for x in (x0 + 0.12, x1 - 0.12):
        for z in (zfr + 0.12, zw - 0.12):
            b.box(x - 0.05, 0.15, z - 0.05, x + 0.05, y0, z + 0.05, [g] * 5 + [None])
    b.box(x0, y0, zw - 0.02, x1, y1, zw, [None, g, g, g, g, g])
    b.box(x0, y0, zfr, x0 + 0.03, y1, zw - 0.02, g)
    b.box(x1 - 0.03, y0, zfr, x1, y1, zw - 0.02, g)
    b.box(x0 + 0.03, y1 - 0.03, zfr, x1 - 0.03, y1, zw - 0.02, g)
    b.box(x0 + 0.03, y0, zfr, x1 - 0.03, y0 + 0.03, zw - 0.02, g)
    b.box(x0 - 0.05, y1, zfr - 0.08, x1 + 0.05, y1 + 0.05, zw + 0.03, g)
    face_quad(b, (x1 + 0.003, 2.2, (zfr + zw) / 2), "x", 0.6, 0.35, "louvre")
    rod(b, (x0 + 0.4, 0.15, zw - 0.3), (x0 + 0.4, y0, zw - 0.3), 0.04, GALV)
    pz = zw - 0.06
    b.box(x0 + 0.06, y0 + 0.06, pz, x1 - 0.06, y1 - 0.06, pz + 0.02, ["paint_white", None, None, None, None, None])
    f = pz
    # Closing spring (coil round a guide rod) and the charging motor with its gearbox, on the left.
    sx = x0 + 0.33
    cyl(b, (sx, 1.75, f - 0.18), (sx, 2.5, f - 0.18), 0.03, GALV, sides=8)
    b.box(sx - 0.17, 2.5, f - 0.34, sx + 0.17, 2.58, f - 0.02, GALV)
    b.box(sx - 0.17, 1.67, f - 0.34, sx + 0.17, 1.75, f - 0.02, GALV)
    for i in range(10):
        ring(b, (sx, 1.8 + i * 0.07, f - 0.18), "y", 0.12, 0.03, sw("yellow"), 12)
    cyl(b, (sx + 0.22, 1.9, f - 0.15), (sx + 0.5, 1.9, f - 0.15), 0.1, sw("dgrey"), sides=14)     # charging motor
    b.box(sx + 0.17, 1.78, f - 0.27, sx + 0.22, 2.02, f - 0.02, sw("dgrey"))                      # its gearbox
    tag(b, (sx + 0.1, 1.62, f), "-z", "CLOSING SPRING", 0.28, 0.035)
    # Indicators on the right: position flag, spring flag, counter (live), each with its tag above.
    ix = x1 - 0.42
    for y, key, label in ((2.45, "flag_closed" if st["closed"] else "flag_open", "POSITION"),
                          (2.2, "flag_charged", "SPRING")):
        tag(b, (ix, y + 0.08, f), "-z", label, 0.16, 0.03)
        b.box(ix - 0.12, y - 0.045, f - 0.02, ix + 0.12, y + 0.045, f, sw("dgrey"))
        face_quad(b, (ix, y, f - 0.021), "-z", 0.2, 0.07, ATLAS.mat(FACES[key]))
    b.live.append({"t": "flag", "ch": "bkr_pos", "at": [ix, 2.45, f - 0.021]})
    ckey = ATLAS.add(f"counter:{st['ops']}", counter_img(st["ops"]))
    tag(b, (ix, 2.03, f), "-z", "OPERATIONS", 0.18, 0.03)
    b.box(ix - 0.07, 1.935, f - 0.02, ix + 0.07, 1.975, f, sw("dgrey"))
    face_quad(b, (ix, 1.955, f - 0.021), "-z", 0.1, 0.03, ATLAS.mat(ckey))
    # Controls: LOCAL/REMOTE, the control switch between its lamps (green OPEN left, red CLOSED right).
    tag(b, (ix, 1.8, f), "-z", "LOCAL - REMOTE", 0.26, 0.03)
    selector(b, (ix, 1.7, f), "-z", 135)
    tag(b, (ix, 1.55, f), "-z", "TRIP - CLOSE", 0.26, 0.03)
    selector(b, (ix, 1.43, f), "-z", 90)
    lamp(b, (ix - 0.12, 1.43, f), "-z", "green", not st["closed"], 0.02)
    lamp(b, (ix + 0.12, 1.43, f), "-z", "red", st["closed"], 0.02)
    cabinet_wiring(b, x0, x1, f, [st["charged"], st["closed"], True, False])
    cabinet_door(b, x0, x1, y0, y1, zfr, ["WIRING DIAGRAM", "345 kV BKR MECHANISM", "DWG E-6120  REV 2", "", "TB1-TB3  52a 52b"])
    # Rating plate on the cabinet's +x side.
    b.box(x1, 1.0, zfr + 0.25, x1 + 0.01, 1.45, zfr + 0.85, [None, None, None, ATLAS.mat(FACES["plate_bkr"]), ALU, ALU])
    # The linkage leaves the cabinet's back through a boot.
    cyl(b, (x1 - 0.1, 2.0, 3.0), (x1 + 0.05, 2.0, 3.0), 0.07, BLACK, sides=10)


# ---------------------------------------------------------------- switchyard: the 345 kV disconnect switch
#
# Real basis: a 362 kV, 3000 A vertical-break disconnect, three poles gang-operated by one motor operator. Each pole on
# its own steel stand (two wide-flange columns, a base channel about 4.6 m up): three station post insulators, the
# hinge post and the rotating post at one end, the jaw post at the other. The rotating post turns a quarter turn and its
# crank lifts the aluminium blade (about 4.4 m long, corona rings at both ends) through 90 degrees about the hinge;
# closed, the blade's contact sits in the jaw's finger contacts. An interphase pipe links the three rotating posts;
# the vertical operating pipe runs down the middle stand to the motor operator at reach height (motor and gearbox,
# position flag, LOCAL/REMOTE, OPEN/CLOSE buttons, manual crank socket, key interlock). The earth switch on the jaw
# end: a ground blade per pole, lying along the base when open, swinging up into the jaw's earthing contact when
# closed, ganged by a torque tube and worked by a padlocked hand operator with its own key interlock (it can only be
# closed with the disconnect open and the key released). Disconnects can't break load: in Jugcraft opening one under
# load draws an arc (decided Oct 18).

DS = dict(poles=(1.5, 5.5, 9.5), z_hinge=0.8, z_rot=1.5, z_jaw=5.2, cols=(1.2, 4.8), base_y=4.6, post_h=3.2)


def disconnect(name="89-1", state=None):
    """One gang-operated 3-pole vertical-break disconnect with its earth switch: poles across x, blades along z, the
    operators facing -z."""
    _faces()
    if "flag_ds_closed" not in FACES:
        FACES["flag_ds_closed"] = ATLAS.add("flag:ds_closed", flag_img("CLOSED", (190, 32, 32), (245, 245, 245), 0.16, 0.055))
        FACES["flag_ds_open"] = ATLAS.add("flag:ds_open", flag_img("OPEN", (40, 140, 60), (245, 245, 245), 0.16, 0.055))
        FACES["flag_es_open"] = ATLAS.add("flag:es_open", flag_img("EARTH OPEN", (40, 140, 60), (245, 245, 245), 0.2, 0.05))
        FACES["flag_es_on"] = ATLAS.add("flag:es_on", flag_img("EARTHED", (190, 32, 32), (245, 245, 245), 0.2, 0.05))
    st = dict(closed=True, earthed=False)
    st.update(state or {})
    D = DS
    b = Board(12, 1601, face=0.0)
    b.live = []
    for k, xp in enumerate(D["poles"]):
        ds_pole(b, xp, "ABC"[k], st)
    # Interphase pipe across the rotating posts' cranks, the vertical operating pipe down the middle stand.
    y_ip = D["base_y"] + 0.3
    rod(b, (D["poles"][0] - 0.35, y_ip, D["z_rot"] - 0.25), (D["poles"][-1] + 0.05, y_ip, D["z_rot"] - 0.25), 0.035, GALV, 8)
    xm = D["poles"][1]
    path(b, [(xm - 0.3, y_ip, D["z_rot"] - 0.25), (xm - 0.3, y_ip - 0.25, D["z_rot"] - 0.25), (xm - 0.3, y_ip - 0.25, D["cols"][0] - 0.3),
             (xm - 0.3, 2.0, D["cols"][0] - 0.3)], 0.03, GALV)
    for y in (2.6, 3.6):
        b.box(xm - 0.34, y - 0.04, D["cols"][0] - 0.3, xm - 0.26, y + 0.04, D["cols"][0] - 0.12, GALV)       # pipe guides
    motor_operator(b, xm - 0.3, D["cols"][0] - 0.3, name, st)
    # Earth switch torque tube along the jaw-end bases and its hand operator down pole A's stand.
    zt = D["z_jaw"] + 0.35
    rod(b, (D["poles"][0] + 0.5, D["base_y"] - 0.15, zt), (D["poles"][-1] + 0.5, D["base_y"] - 0.15, zt), 0.03, GALV, 8)
    xa = D["poles"][0] + 0.5
    path(b, [(xa, D["base_y"] - 0.15, zt), (xa, 1.35, zt)], 0.025, GALV)
    for y in (2.3, 3.4):
        b.box(xa - 0.04, y - 0.04, zt - 0.2, xa + 0.04, y + 0.04, zt - 0.03, GALV)
    earth_operator(b, xa, zt, name, st)
    return b


def ds_pole(b, xp, ph, st):
    D = DS
    by = D["base_y"]
    c0, c1 = D["cols"]
    # The stand: two columns on piers, cross bracing, the base channel on top.
    for z in (c0, c1):
        b.box(xp - 0.35, -0.3, z - 0.35, xp + 0.35, 0.15, z + 0.35, ["concrete"] * 5 + [None])
        b.box(xp - 0.2, 0.15, z - 0.2, xp + 0.2, 0.18, z + 0.2, GALV)
        for dx in (-0.13, 0.13):
            for dz in (-0.13, 0.13):
                cyl(b, (xp + dx, 0.18, z + dz), (xp + dx, 0.24, z + dz), 0.02, BLACK, sides=6)
        ibeam(b, np.array((xp - 0.11, 0.18, z - 0.13)), np.array((xp + 0.11, by - 0.2, z + 0.13)), 0.22, 0.26, GALV)
        path(b, [(xp + 0.12, 0.6, z), (xp + 0.18, 0.6, z), (xp + 0.18, 0.16, z), (xp + 0.18, 0.16, z + 0.5)], 0.016, COPPER)
    for y0, y1 in ((0.8, 2.6), (2.6, 4.2)):
        za, zb = (c0 + 0.14, c1 - 0.14) if y0 < 1 else (c1 - 0.14, c0 + 0.14)
        bar(b, (xp + 0.14 + (0.0 if y0 < 1 else 0.05), y0, za), (xp + 0.14 + (0.0 if y0 < 1 else 0.05), y1, zb), 0.05, GALV)   # bracing
    b.box(xp - 0.12, by - 0.2, D["z_hinge"] - 0.4, xp + 0.12, by, D["z_jaw"] + 0.5, GALV)       # base channel
    stencil(b, (xp, by - 0.1, D["z_hinge"] - 0.404), "-z", ph, 0.16)
    # The three posts.
    top = by + D["post_h"]
    for z in (D["z_hinge"], D["z_rot"], D["z_jaw"]):
        flange(b, (xp, by + 0.03, z), "y", 0.17, 0.05, GALV, 8)
        h = D["post_h"] - 0.1
        cyl(b, (xp, by + 0.05, z), (xp, by + 0.05 + h, z), 0.085, PORC, sides=12)
        for i in range(int(h / 0.12)):
            yy = by + 0.1 + i * 0.12
            cyl(b, (xp, yy, z), (xp, yy + 0.025, z), 0.19 if i % 2 == 0 else 0.15, PORC, sides=14)
        flange(b, (xp, by + 0.05 + h / 2, z), "y", 0.12, 0.04, GALV, 6)
        cyl(b, (xp, top - 0.05, z), (xp, top, z), 0.12, GALV, sides=12)
    # The rotating post's bearing and crank under the base (the interphase pipe's arm).
    zr = D["z_rot"]
    cyl(b, (xp, by - 0.2, zr), (xp, by - 0.05, zr), 0.12, GALV, sides=12)
    bar(b, (xp, by + 0.3, zr), (xp - 0.35, by + 0.3, zr - 0.25), 0.05, GALV)
    cyl(b, (xp, by + 0.02, zr), (xp, by + 0.36, zr), 0.06, GALV, sides=10)
    # Hinge casting and the crank link on top; the jaw with its finger contacts and earthing contact; terminal pads.
    zh, zj = D["z_hinge"], D["z_jaw"]
    b.box(xp - 0.12, top, zh - 0.2, xp + 0.12, top + 0.22, zr + 0.15, ALU)
    b.box(xp - 0.08, top + 0.04, zh - 0.38, xp + 0.08, top + 0.06, zh - 0.2, ALU)                # line pad, hinge end
    b.box(xp - 0.12, top, zj - 0.18, xp + 0.12, top + 0.12, zj + 0.18, ALU)
    for dx in (-0.075, 0.075):
        b.box(xp + dx - 0.015, top + 0.12, zj - 0.12, xp + dx + 0.015, top + 0.42, zj + 0.06, ALU)   # fingers
    b.box(xp - 0.08, top + 0.04, zj + 0.18, xp + 0.08, top + 0.06, zj + 0.36, ALU)                # line pad, jaw end
    ring(b, (xp, top + 0.25, zj), "z", 0.3, 0.05, ALU, 18)
    # The blade (live: swings up 90 degrees about the hinge when open).
    pivot = np.array([xp, top + 0.25, zh])
    ang = st.get("ds_deg", 0.0 if st["closed"] else 90.0)      # (in game the blade's travel between: ds_deg)
    a = math.radians(ang)
    d = np.array([0, math.sin(a), math.cos(a)])
    L = zj - zh
    rod(b, pivot, pivot + d * L, 0.06, ALU, 12)
    cyl(b, pivot - d * 0.05, pivot + d * 0.12, 0.09, ALU, sides=12)
    ring(b, pivot + d * 0.35, d, 0.22, 0.04, ALU, 16)
    b.live.append({"t": "blade", "pivot": _rnd(pivot), "axis": [1, 0, 0], "len": L, "ch": "ds_pos", "deg": ang})
    rod(b, (xp, top + 0.22, zr), pivot + d * 0.6 + np.array([0, -0.04, 0]), 0.025, GALV, 6)          # the crank link
    # The earth blade: hinged on the base's jaw end, lying back along the base on its rest when open, standing up into
    # the earthing contact on the jaw casting when closed.
    ex = xp + 0.25
    eh = np.array([ex, by + 0.05, zj + 0.35])
    eL = D["post_h"] - 0.01
    ea = math.radians(st.get("es_deg", 90 if st["earthed"] else 1.6))
    ed = np.array([0, math.sin(ea), -math.cos(ea)])
    b.box(ex - 0.06, by, zj + 0.25, ex + 0.06, by + 0.12, zj + 0.45, GALV)
    rod(b, eh, eh + ed * eL, 0.03, ALU, 8)
    b.box(ex - 0.05, by, zj - 2.6, ex + 0.05, by + 0.1, zj - 2.5, GALV)                            # its rest
    b.box(ex - 0.04, top - 0.02, zj + 0.1, ex + 0.04, top + 0.1, zj + 0.42, ALU)                    # earthing contact
    b.live.append({"t": "blade", "pivot": _rnd(eh), "axis": [1, 0, 0], "len": eL, "ch": "es_pos"})
    path(b, [(ex, by, zj + 0.35), (ex, by - 0.25, zj + 0.35), (ex, by - 0.25, c1 + 0.14),
             (ex, 0.16, c1 + 0.14), (ex, 0.16, c1 + 0.6)], 0.016, COPPER)                            # its ground lead


def motor_operator(b, x, zc, name, st):
    """The motor operator on the middle stand's front column at reach height, door open: the gearbox under the
    operating pipe with the motor below it and the manual crank socket; on the right the title, the position flag,
    LOCAL/REMOTE, OPEN and CLOSE buttons, the key interlock."""
    x0, x1, y0, y1 = x - 0.17, x + 0.58, 1.0, 1.95
    zb = zc + 0.12
    z0 = zb - 0.4
    g = GREY
    b.box(x0, y0, zb - 0.02, x1, y1, zb, [None, g, g, g, g, g])
    b.box(x0, y0, z0, x0 + 0.025, y1, zb - 0.02, g)
    b.box(x1 - 0.025, y0, z0, x1, y1, zb - 0.02, g)
    b.box(x0 + 0.025, y1 - 0.025, z0, x1 - 0.025, y1, zb - 0.02, g)
    b.box(x0 + 0.025, y0, z0, x1 - 0.025, y0 + 0.025, zb - 0.02, g)
    b.box(x0 - 0.04, y1, z0 - 0.06, x1 + 0.04, y1 + 0.04, zb, g)
    b.box(x + 0.14, y0 - 0.25, zb - 0.1, x + 0.26, y0, zb + 0.05, GALV)                            # its bracket
    rod(b, (x1 - 0.12, 0.15, z0 + 0.2), (x1 - 0.12, y0, z0 + 0.2), 0.025, GALV)                    # conduit
    f = zb - 0.06
    b.box(x0 + 0.04, y0 + 0.04, f, x1 - 0.04, y1 - 0.04, f + 0.02, ["paint_white", None, None, None, None, None])
    # Gearbox under the pipe, the shaft up through the roof, the motor below, the crank socket under that.
    b.box(x - 0.12, 1.6, f - 0.2, x + 0.12, 1.76, f - 0.02, sw("dgrey"))
    b.box(x - 0.04, 1.76, f - 0.1, x + 0.04, y1 + 0.07, f - 0.02, GALV)
    cyl(b, (x, 1.3, f - 0.11), (x, 1.6, f - 0.11), 0.08, sw("dgrey"), sides=14)
    cyl(b, (x, 1.15, f), (x, 1.15, f - 0.04), 0.03, BLACK, sides=6)
    tag(b, (x, 1.22, f), "-z", "MANUAL", 0.12, 0.025)
    xr = x + 0.36
    tag(b, (xr, y1 - 0.1, f), "-z", f"{name}  MOTOR OPERATOR", 0.36, 0.035)
    b.box(xr - 0.11, 1.66, f - 0.02, xr + 0.11, 1.74, f, sw("dgrey"))
    face_quad(b, (xr, 1.7, f - 0.021), "-z", 0.16, 0.055, ATLAS.mat(FACES["flag_ds_closed" if st["closed"] else "flag_ds_open"]))
    b.live.append({"t": "flag", "ch": "ds_pos", "at": [xr, 1.7, f - 0.021]})
    tag(b, (xr, 1.585, f), "-z", "LOCAL - REMOTE", 0.22, 0.028)
    selector(b, (xr, 1.5, f), "-z", 135, 0.025)
    for dx, txt, col in ((-0.09, "OPEN", "green"), (0.09, "CLOSE", "red")):
        tag(b, (xr + dx, 1.36, f), "-z", txt, 0.11, 0.028)
        push_button(b, (xr + dx, 1.29, f), "-z", col, 0.02)
    b.box(xr - 0.07, 1.08, f - 0.04, xr + 0.07, 1.2, f, sw("yellow"))                                # key interlock
    cyl(b, (xr, 1.14, f - 0.04), (xr, 1.14, f - 0.06), 0.02, ALU, sides=8)
    # Door open 100 degrees on its -x hinge.
    t = math.radians(100)
    u = np.array([math.cos(t), 0, -math.sin(t)])
    inside = np.array([math.sin(t), 0, math.cos(t)])
    c = np.array([x0, (y0 + y1) / 2, z0]) + u * (x1 - x0) / 2 - inside * 0.012
    b.obox(tuple(_rnd(c)), tuple(_rnd(u)), (0, 1, 0), round((x1 - x0) / 2, 4), round((y1 - y0) / 2, 4), 0.012, g)


def earth_operator(b, x, zt, name, st):
    """The earth switch's hand operator at the foot of pole A's operating pipe: gearbox, crank handle, position
    plate, padlock and key interlock."""
    b.box(x - 0.12, 1.1, zt - 0.12, x + 0.12, 1.35, zt + 0.12, GREY)
    b.box(x - 0.1, 0.95, zt - 0.1, x + 0.1, 1.1, zt + 0.1, GALV)
    rod(b, (x, 0.15, zt), (x, 0.95, zt), 0.03, GALV)
    b.box(x - 0.18, 0.15, zt - 0.18, x + 0.18, 0.18, zt + 0.18, GALV)
    cyl(b, (x, 1.22, zt - 0.12), (x, 1.22, zt - 0.2), 0.025, GALV, sides=8)
    bar(b, (x, 1.22, zt - 0.2), (x + 0.2, 1.22, zt - 0.2), 0.03, GALV)
    cyl(b, (x + 0.2, 1.22, zt - 0.2), (x + 0.2, 1.22, zt - 0.3), 0.025, BLACK, sides=8)
    face_quad(b, (x, 1.42, zt - 0.121), "-z", 0.2, 0.05, ATLAS.mat(FACES["flag_es_on" if st["earthed"] else "flag_es_open"]))
    b.box(x + 0.06, 1.0, zt - 0.14, x + 0.12, 1.07, zt - 0.1, YELLOW)                                 # padlock
    b.box(x - 0.11, 1.12, zt - 0.16, x - 0.01, 1.2, zt - 0.12, sw("yellow"))                          # key interlock
    tag(b, (x, 1.52, zt - 0.124), "-z", f"{name}E  EARTH SWITCH", 0.36, 0.035)


# ---------------------------------------------------------------- switchyard: line-entry equipment and rigid bus
#
# Real basis (345 kV): rigid bus of 5 in schedule 40 aluminium tube on station post insulators (fixed and slip-fit
# clamps, bolted couplers, corona end bells), about 8 m up; a surge arrester per phase (metal-oxide, MCOV 209 kV, three
# porcelain units, grading ring at the top, an insulating base so the ground lead runs through a surge monitor at reach
# height: leakage milliammeter and discharge counter); a capacitor voltage transformer per phase (two capacitor units
# on the oil-filled electromagnetic unit, secondary terminal box, conduit to the marshalling box) that on phases A and C
# also couples the power-line carrier, with its line tuner box; and on A and C a line trap (air-core coil about 1.5 m
# across, aluminium winding between spacer bars, bird barrier, corona rings) on three small posts on its pedestal,
# in series with the line. Steel pedestals (square tube, base plate on a pier with anchor bolts), every one grounded.

LE = dict(poles=(1.5, 5.5, 9.5), bus_y=8.1, supports=(0.9, 7.4), arrester=3.0, cvt=5.2, trap=10.2, bus_end=8.5)


def pedestal(b, x, z, h, w=0.3):
    """A square-tube steel pedestal on a concrete pier: base plate and anchor bolts, top plate, a ground lead."""
    b.box(x - 0.35, -0.3, z - 0.35, x + 0.35, 0.15, z + 0.35, ["concrete"] * 5 + [None])
    b.box(x - 0.24, 0.15, z - 0.24, x + 0.24, 0.18, z + 0.24, GALV)
    for dx in (-0.16, 0.16):
        for dz in (-0.16, 0.16):
            cyl(b, (x + dx, 0.18, z + dz), (x + dx, 0.24, z + dz), 0.02, BLACK, sides=6)
    b.box(x - w / 2, 0.18, z - w / 2, x + w / 2, h - 0.03, z + w / 2, [GALV] * 4 + [None, None])
    b.box(x - w / 2 - 0.08, h - 0.03, z - w / 2 - 0.08, x + w / 2 + 0.08, h, z + w / 2 + 0.08, GALV)
    path(b, [(x + w / 2, 0.55, z + 0.05), (x + w / 2 + 0.05, 0.55, z + 0.05), (x + w / 2 + 0.05, 0.16, z + 0.05),
             (x + w / 2 + 0.05, 0.16, z + 0.5)], 0.016, COPPER)


def stack(b, x, z, y0, h, core, sheds, mat=PORC, units=1):
    """A vertical porcelain stack of units, alternating sheds, metal flanges between units."""
    uh = h / units
    for u in range(units):
        ya = y0 + u * uh
        cyl(b, (x, ya, z), (x, ya + uh, z), core, mat, sides=14)
        for i in range(int((uh - 0.1) / 0.12)):
            yy = ya + 0.08 + i * 0.12
            cyl(b, (x, yy, z), (x, yy + 0.025, z), sheds[i % 2], mat, sides=16)
        flange(b, (x, ya + uh, z), "y", core + 0.05, 0.05, ALU, 8)


def line_entry(name="LINE 1", traps="AC"):
    """Three phases of line-entry equipment along a length of rigid bus (bus along z, phases across x): bus supports,
    arresters, CVTs, line traps on the trap phases. Facing -z (the gauges, tags and boxes)."""
    _faces()
    if "leak" not in FACES:
        FACES["leak"] = ATLAS.add("dial:leak", dial_img(0, 5, "LEAKAGE", "mA", 3, 0.06))
        FACES["plate_cvt"] = ATLAS.add("plate:cvt", plate_img(
            ["CAPACITOR VOLTAGE TRANSFORMER", "345 000 / √3 V   BIL 1300 kV", "SEC 115 / √3 - 115 / √3 V",
             "C = 5000 pF   PLC COUPLING", "SERIAL 26-4410  YEAR 2026"], 0.34, 0.24))
    L = LE
    b = Board(12, 1701, face=0.0)
    b.live = []
    for k, xp in enumerate(L["poles"]):
        ph = "ABC"[k]
        # The rigid bus: tube along z, couplers, end bells, the drop to the disconnect's jaw pad at the -z end.
        by = L["bus_y"]
        cyl(b, (xp, by, -0.45), (xp, by, L["bus_end"]), 0.065, ALU, sides=12)
        cyl(b, (xp, by, 4.1), (xp, by, 4.4), 0.08, ALU, sides=12)                                  # bolted coupler
        cyl(b, (xp, by, L["bus_end"]), (xp, by, L["bus_end"] + 0.12), 0.11, ALU, sides=14)           # end bell
        b.box(xp - 0.06, by - 0.21, -0.52, xp + 0.06, by - 0.06, -0.38, ALU)                        # T to the jaw pad
        b.box(xp - 0.1, by - 0.24, -0.54, xp + 0.1, by - 0.21, -0.36, ALU)
        for z in L["supports"]:
            bus_support(b, xp, z, z == L["supports"][0])
        arrester(b, xp, L["arrester"], ph)
        cvt(b, xp, L["cvt"], ph, ph in traps)
        if ph in traps:
            line_trap(b, xp, L["trap"], ph)
        stencil(b, (xp, 1.2, L["supports"][0] - 0.153), "-z", ph, 0.2)
    tag(b, (L["poles"][1], 2.0, L["cvt"] - 0.204), "-z", f"{name}  CVT", 0.3, 0.04)
    # Marshalling box for the CVT secondaries at the -x end, conduits to it along a trench (buried).
    mx = L["poles"][0] - 1.4
    b.box(mx - 0.3, 0.15, L["cvt"] - 0.25, mx + 0.3, 1.5, L["cvt"] + 0.25, [GREY] * 5 + [None])
    b.box(mx - 0.33, 1.5, L["cvt"] - 0.3, mx + 0.33, 1.54, L["cvt"] + 0.28, GREY)
    tag(b, (mx, 1.3, L["cvt"] - 0.254), "-z", f"{name}  CVT MARSHALLING", 0.5, 0.04)
    b.box(mx - 0.5, -0.3, L["cvt"] - 0.4, mx + 0.5, 0.15, L["cvt"] + 0.4, ["concrete"] * 5 + [None])
    return b


def bus_support(b, x, z, fixed):
    """A station post on its pedestal carrying the bus in a fixed or slip-fit clamp."""
    by = LE["bus_y"]
    ph_ = by - 0.25 - 3.2
    pedestal(b, x, z, ph_)
    flange(b, (x, ph_ + 0.03, z), "y", 0.17, 0.05, GALV, 8)
    stack(b, x, z, ph_ + 0.05, 3.2 - 0.05, 0.085, (0.19, 0.15), units=2)
    b.box(x - 0.1, by - 0.2, z - 0.12, x + 0.1, by - 0.1, z + 0.12, ALU)                             # clamp base
    for dz in (-0.08, 0.08):
        b.box(x - 0.09, by - 0.1, z + dz - 0.03, x + 0.09, by + 0.09, z + dz + 0.03, ALU if fixed else GALV)


def arrester(b, x, z, ph):
    """A metal-oxide surge arrester: pedestal, insulating base, three units, grading ring, its jumper up to the bus;
    the ground lead down through the surge monitor (leakage mA meter and counter) at reach height."""
    by = LE["bus_y"]
    h = 3.9
    pedestal(b, x, z, h)
    for dx in (-0.12, 0.12):
        cyl(b, (x + dx, h, z), (x + dx, h + 0.12, z), 0.05, sw("dgrey"), sides=8)                   # insulating base
    b.box(x - 0.2, h + 0.12, z - 0.2, x + 0.2, h + 0.17, z + 0.2, ALU)
    stack(b, x, z, h + 0.17, 3.5, 0.11, (0.24, 0.19), units=3)
    top = h + 0.17 + 3.5
    cyl(b, (x, top + 0.05, z), (x, top + 0.12, z), 0.14, ALU, sides=12)
    ring(b, (x, top - 0.25, z), "y", 0.5, 0.06, ALU, 20)
    path(b, [(x, top + 0.12, z), (x, by - 0.06, z)], 0.02, sw("mgrey"))                            # jumper to the bus
    # The surge monitor at reach height, the ground lead into its top and out of its bottom.
    mz = z - 0.15 - 0.09
    b.box(x - 0.11, 1.45, mz - 0.01, x + 0.11, 1.8, mz + 0.09, GREY)
    gauge(b, (x, 1.68, mz - 0.01), "-z", 0.055, FACES["leak"], 0.6, 0, 5, channel=f"arr_leak_{ph}", case=GREY)
    ckey = ATLAS.add("counter:arr", counter_img(3, 0.08, 0.025))
    b.box(x - 0.05, 1.5, mz - 0.03, x + 0.05, 1.54, mz - 0.01, sw("dgrey"))
    face_quad(b, (x, 1.52, mz - 0.031), "-z", 0.08, 0.025, ATLAS.mat(ckey))
    b.live.append({"t": "counter", "ch": f"arr_ops_{ph}", "at": [x, 1.52, mz - 0.031]})
    tag(b, (x, 1.88, mz + 0.0), "-z", f"ARRESTER {ph}", 0.2, 0.03)
    path(b, [(x + 0.2, h + 0.14, z), (x + 0.2, h - 0.05, z), (x + 0.2, h - 0.05, mz - 0.03), (x + 0.2, 1.8, mz - 0.03),
             (x + 0.09, 1.8, mz + 0.04)], 0.016, COPPER)
    path(b, [(x, 1.45, mz + 0.04), (x, 0.4, mz + 0.04), (x, 0.16, mz - 0.4)], 0.016, COPPER)
    for y in (2.4, 3.2):
        b.box(x + 0.15, y - 0.03, mz - 0.06, x + 0.23, y + 0.03, mz + 0.03, GALV)                  # lead clamps


def cvt(b, x, z, ph, plc):
    """A capacitor voltage transformer: pedestal, the oil-filled EMU tank with its secondary box and nameplate, two
    capacitor units, top cap and pad, the jumper up to the bus; the PLC line tuner box on the carrier phases."""
    by = LE["bus_y"]
    h = 3.0
    pedestal(b, x, z, h, 0.36)
    b.box(x - 0.32, h, z - 0.32, x + 0.32, h + 0.75, z + 0.32, GREY)                                 # EMU tank
    b.box(x - 0.35, h + 0.75, z - 0.35, x + 0.35, h + 0.8, z + 0.35, GREY)
    cyl(b, (x + 0.33, h + 0.55, z), (x + 0.38, h + 0.55, z), 0.05, "glass", sides=10)                # oil sight glass
    b.box(x - 0.2, h + 0.1, z - 0.48, x + 0.2, h + 0.5, z - 0.32, GREY)                              # secondary box
    b.box(x - 0.17, h + 0.53, z - 0.325, x + 0.17, h + 0.71, z - 0.32, [ATLAS.mat(FACES["plate_cvt"]), None, ALU, ALU, ALU, ALU])
    path(b, [(x, h + 0.1, z - 0.4), (x, h - 0.2, z - 0.4), (x, h - 0.2, z - 0.25), (x, 0.0, z - 0.25)], 0.022, GALV)
    for y in (1.2, 2.2):
        b.box(x - 0.04, y - 0.03, z - 0.3, x + 0.04, y + 0.03, z - 0.18, GALV)
    stack(b, x, z, h + 0.8, 3.8, 0.13, (0.27, 0.22), units=2)
    top = h + 0.8 + 3.8
    cyl(b, (x, top + 0.05, z), (x, top + 0.2, z), 0.2, ALU, sides=14)
    ring(b, (x, top + 0.1, z), "y", 0.4, 0.06, ALU, 20)
    path(b, [(x, top + 0.2, z), (x, by - 0.06, z)], 0.02, sw("mgrey"))
    if plc:
        tx = x + 0.6
        b.box(tx - 0.2, 1.0, z - 0.25, tx + 0.2, 1.6, z + 0.05, GREY)
        tag(b, (tx, 1.48, z - 0.254), "-z", f"LINE TUNER {ph}", 0.3, 0.035)
        path(b, [(x + 0.2, h + 0.3, z - 0.32), (tx, h + 0.3, z - 0.32), (tx, 1.6, z - 0.32), (tx, 1.6, z - 0.1)], 0.016, GALV)
        rod(b, (tx - 0.1, 0.15, z - 0.1), (tx - 0.1, 1.0, z - 0.1), 0.025, GALV)                   # conduit
        b.box(tx - 0.04, 0.18, z + 0.05, tx + 0.04, 1.7, z + 0.13, [GALV] * 5 + [None])                # its post
        b.box(tx - 0.12, 0.15, z - 0.03, tx + 0.12, 0.18, z + 0.21, GALV)
        b.box(tx - 0.2, -0.3, z - 0.1, tx + 0.2, 0.15, z + 0.3, ["concrete"] * 5 + [None])


def line_trap(b, x, z, ph):
    """A pedestal-mounted line trap: three small posts on the pedestal's frame, the coil (aluminium winding between
    spacer bars, top and bottom spiders), the bird barrier, corona rings; its lower pad jumpered to the bus end, its
    upper pad the line's connection."""
    by = LE["bus_y"]
    h = 5.8
    pedestal(b, x, z, h, 0.36)
    b.box(x - 0.6, h, z - 0.6, x + 0.6, h + 0.08, z + 0.6, GALV)
    for k in range(3):
        t = math.radians(90 + k * 120)
        px, pz = x + math.cos(t) * 0.45, z + math.sin(t) * 0.45
        flange(b, (px, h + 0.1, pz), "y", 0.1, 0.04, GALV, 6)
        stack(b, px, pz, h + 0.12, 1.1, 0.06, (0.12, 0.1))
    y0 = h + 1.3
    H = 1.9
    R = 0.75
    for k in range(3):                                                                                 # bottom spider
        t = math.radians(90 + k * 120)
        rod(b, (x, y0, z), (x + math.cos(t) * R, y0, z + math.sin(t) * R), 0.035, ALU, 8)
    cyl(b, (x, y0 - 0.05, z), (x, y0 + 0.05, z), 0.12, ALU, sides=10)
    for i in range(int(H / 0.06)):
        yy = y0 + 0.05 + i * 0.06
        cyl(b, (x, yy, z), (x, yy + 0.04, z), R, "stainless", sides=28)
    cyl(b, (x, y0 + 0.05, z), (x, y0 + H, z), R - 0.04, sw("dgrey"), sides=28)
    for k in range(8):                                                                                 # spacer bars
        t = k * math.pi / 4
        bar(b, (x + math.cos(t) * (R + 0.02), y0, z + math.sin(t) * (R + 0.02)),
            (x + math.cos(t) * (R + 0.02), y0 + H + 0.05, z + math.sin(t) * (R + 0.02)), 0.035, sw("mgrey"), 0.05)
    yt = y0 + H + 0.05
    for k in range(4):                                                                                 # top spider
        t = k * math.pi / 2 + math.pi / 4
        rod(b, (x, yt, z), (x + math.cos(t) * R, yt, z + math.sin(t) * R), 0.035, ALU, 8)
    for R2 in (0.25, 0.5):                                                                             # bird barrier
        ring(b, (x, yt + 0.06, z), "y", R2, 0.025, sw("mgrey"), 20)
    ring(b, (x, yt + 0.02, z), "y", R + 0.06, 0.07, ALU, 28)
    ring(b, (x, y0 - 0.02, z), "y", R + 0.06, 0.07, ALU, 28)
    b.box(x - 0.08, yt + 0.02, z + R - 0.05, x + 0.08, yt + 0.06, z + R + 0.25, ALU)                  # upper pad (line)
    b.box(x - 0.08, y0 - 0.06, z - R - 0.25, x + 0.08, y0 - 0.02, z - R + 0.05, ALU)                  # lower pad (bus)
    p0 = np.array([x, y0 - 0.06, z - R - 0.15]); p2 = np.array([x, by - 0.07, LE["bus_end"] - 0.2])
    p1 = np.array([x, (p0[1] + by) / 2 - 0.5, (p0[2] + p2[2]) / 2])
    pts = [(1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t ** 2 * p2 for t in np.linspace(0, 1, 8)]
    for pa, pb in zip(pts, pts[1:]):
        rod(b, pa, pb, 0.02, sw("mgrey"), 6)
    tag(b, (x, 2.0, z - 0.184), "-z", f"LINE TRAP {ph}", 0.24, 0.035)


# ---------------------------------------------------------------- the startup transformer SAT-1
#
# Real basis: a three-winding startup (reserve auxiliary) transformer, 345 kV wye (solidly grounded neutral) :
# 4.16 kV : 4.16 kV, ONAN/ONAF/ONAF 60/80/100 MVA, about 140 t and 45 000 L of oil. Because the grid voltage swings,
# it has an on-load tap changer (33 positions, 16L-N-16R, +-10 %) in its own oil compartment on the tank with a separate
# conservator section, oil-flow (surge) relay and breather, driven by a motor drive cabinet at reach height (tap
# position indicator with min and max drag hands, operations counter, RAISE / LOWER, LOCAL / REMOTE, hand crank).
# HV bushings on the cover with surge arresters beside them (their ground leads down the end wall through surge
# counters); two LV windings X and Y each out through a non-segregated bus duct buried to 4160 V buses 1A and 1B, each
# neutral through its own grounding resistor. Panel radiators with two fan stages. In service it carries the house
# load whenever the generator is off (fast transfer on a trip), otherwise it stands energised and lightly loaded.

SX0, SX1, SZ0, SZ1, SY0, SY1 = 0.6, 8.6, 1.6, 5.6, 0.5, 5.0
SCOV = SY1 + 0.14
SAT_FRONT = (1.2, 2.8, 4.0, 5.2)
SAT_BACK = (1.2, 2.2, 3.2, 4.2, 5.2, 6.2, 7.2)
SAT_RADS = (1.7, 2.7, 3.7, 4.7, 5.7, 6.7, 7.7)
SAT_HV = (2.0, 3.8, 5.6)
SAT_BAY = dict(plinth=(0.0, 0.3, 11.2, 8.4), curb_x=(-2.3, 11.8), curb_z=(-1.4, 9.4), wall=(-1.4, 9.6, 9.0),
               rings=((3.3, (-0.6, 11.0, -0.5, 8.6)), (5.9, (0.0, 9.3, 0.6, 6.7))), aim=(4.6, 3.6),
               stands=(((-0.6, -0.5), (11.0, -0.5), (-0.6, 8.6), (11.0, 8.6), (4.6, 8.6), (8.6, -0.5)),
                       ((0.0, 0.6), (9.3, 0.6), (0.0, 6.7), (9.3, 6.7))))


def sat(name="SAT-1", state=None):
    """The startup transformer. state: oil, wdg, maxima, level, ltc_level, fans (0..2), tap (1..33), tap_min, tap_max,
    alarms."""
    _faces()
    if "ltc" not in FACES:
        FACES["ltc"] = ATLAS.add("dial:ltc", tap_pos_img())
        FACES["plate_sat"] = ATLAS.add("plate:sat", plate_img(
            ["STARTUP TRANSFORMER  3 WINDING", "3 PHASE 60 Hz ONAN/ONAF/ONAF 60/80/100 MVA", "H 345 000 Y   BIL 1300 kV",
             "X 4160 V   Y 4160 V   NGR 400 A 10 s", "LTC 33 POS  +-10 %  ON H", "IMPEDANCE H-X 11.8 %  H-Y 11.8 %",
             "OIL 45 000 L  TOTAL 140 000 kg", "SERIAL 26-1195  YEAR 2026"], 0.52, 0.38))
    st = dict(oil=46, wdg=55, oil_max=52, wdg_max=63, level=58, ltc_level=60, fans=0, tap=17, tap_min=14, tap_max=19, alarms=())
    st.update(state or {})
    b = Board(10, 1195, face=0.0)
    b.live = []
    sat_tank(b)
    b.core = len(b.still)
    sat_cover(b, st)
    sat_lv(b, name)
    sat_radiators(b, st)
    sat_front(b, name, st)
    sat_end(b)
    sat_cabinet(b, name, st)
    ltc_drive(b, name, st)
    return b


def tap_pos_img(r_m=0.12):
    """The tap changer's position indicator: 1 to 33 round the dial (N at 17 at the top), its legend."""
    s = int(2 * r_m * PXB)
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c, r = s / 2, s / 2 - 1
    d.ellipse([c - r, c - r, c + r, c + r], fill=(244, 244, 236, 255))
    for i in range(33):
        t = math.radians(225 - 270 * i / 32)
        k0 = .82 if i % 4 == 0 else .88
        d.line([(c + math.cos(t) * r * k0, c - math.sin(t) * r * k0), (c + math.cos(t) * r * .95, c - math.sin(t) * r * .95)],
               fill=(20, 20, 20), width=2)
        if i % 4 == 0:
            lab = "N" if i == 16 else (f"{16 - i}L" if i < 16 else f"{i - 16}R")
            d.text((c + math.cos(t) * r * .66, c - math.sin(t) * r * .66), lab, font=_font(r * .13), fill=(20, 20, 20), anchor="mm")
    d.text((c, c + r * .42), "TAP POSITION", font=_font(r * .11), fill=(20, 20, 20), anchor="mm")
    return im


def sat_tank(b):
    g = GREY
    for x in (1.6, 7.6):
        b.box(x - 0.16, 0.0, SZ0 - 0.15, x + 0.16, 0.05, SZ1 + 0.15, [g, g, g, g, g, None])
        b.box(x - 0.035, 0.05, SZ0 - 0.15, x + 0.035, 0.44, SZ1 + 0.15, [g, g, g, g, None, None])
        b.box(x - 0.16, 0.44, SZ0 - 0.15, x + 0.16, 0.49, SZ1 + 0.15, g)
    b.box(SX0, 0.49, SZ0, SX1, SY0, SZ1, [g, g, g, g, None, g])
    for x in (SX0 + 0.22, SX1 - 0.22):
        for z in (SZ0, SZ1):
            s = -1 if z == SZ0 else 1
            b.box(x - 0.18, 0.85, min(z, z + s * 0.22), x + 0.18, 1.03, max(z, z + s * 0.22), g)
    b.box(SX0, SY0, SZ0, SX1, SY1, SZ1, [g, g, g, g, None, None])
    for y0, y1, out in ((1.3, 1.42, 0.1), (3.0, 3.1, 0.08)):
        b.box(SX0 - out, y0, SZ0 - out, SX1 + out, y1, SZ1 + out, g)
    for x in np.arange(SX0 + 0.1, SX1, 0.42):
        for z, s in ((SZ0 - 0.12, -1), (SZ1 + 0.12, 1)):
            cyl(b, (x, 1.42, z - s * 0.04), (x, 1.47, z - s * 0.04), 0.022, BLACK, sides=6)
    segs = ((1.42, 3.0), (3.1, SY1 - 0.06))
    for xs, z, s in ((SAT_FRONT, SZ0, -1), (SAT_BACK, SZ1, 1)):
        for x in xs:
            for y0, y1 in segs:
                b.box(x - 0.06, y0, min(z, z + s * 0.1), x + 0.06, y1, max(z, z + s * 0.1), [g, g, g, g, None, None])
    for z in (2.4, 3.2, 4.0, 4.8):
        for x, s in ((SX0, -1), (SX1, 1)):
            for y0, y1 in segs:
                b.box(min(x, x + s * 0.1), y0, z - 0.06, max(x, x + s * 0.1), y1, z + 0.06, [g, g, g, g, None, None])
    b.box(SX0 - 0.08, SY1, SZ0 - 0.08, SX1 + 0.08, SY1 + 0.08, SZ1 + 0.08, g)
    b.box(SX0, SY1 + 0.08, SZ0, SX1, SCOV, SZ1, [g, g, g, g, g, None])
    for x in np.arange(SX0 + 0.1, SX1, 0.4):
        for z in (SZ0 - 0.04, SZ1 + 0.04):
            cyl(b, (x, SY1 + 0.08, z), (x, SY1 + 0.12, z), 0.02, BLACK, sides=6)
    for z in np.arange(SZ0 + 0.15, SZ1, 0.4):
        for x in (SX0 - 0.04, SX1 + 0.04):
            cyl(b, (x, SY1 + 0.08, z), (x, SY1 + 0.12, z), 0.02, BLACK, sides=6)
    for x in (SX0 + 0.3, SX1 - 0.3):
        for z, s in ((SZ0, -1), (SZ1, 1)):
            b.box(x - 0.15, SY1 - 0.6, min(z, z + s * 0.12), x + 0.15, SY1 - 0.06, max(z, z + s * 0.12), g)
            cyl(b, (x - 0.05, SY1 - 0.3, z + s * 0.06), (x + 0.05, SY1 - 0.3, z + s * 0.06), 0.06, sw("dgrey"), sides=10)
    for x in (SX0 + 0.12, SX1 - 0.12):
        b.box(x - 0.1, 0.6, SZ0 - 0.03, x + 0.1, 0.75, SZ0, [COPPER, None, COPPER, COPPER, COPPER, COPPER])
        path(b, [(x, 0.62, SZ0 - 0.06), (x, 0.3, SZ0 - 0.06), (x, 0.02, SZ0 - 0.4)], 0.02, COPPER)


def sat_cover(b, st):
    """HV bushings (with CT turrets) and their arresters on the cover, the H0 neutral bushing and strap, the manhole,
    the PRD and its pipe, the two-section conservator (main and LTC) with gauges, Buchholz and LTC surge relay."""
    hz, az = 2.7, 3.75
    for k, x in enumerate(SAT_HV):
        cyl(b, (x, SCOV, hz), (x, SCOV + 0.5, hz), 0.42, GREY, sides=20)
        flange(b, (x, SCOV + 0.5, hz), "y", 0.5, 0.06, GREY, 12)
        top = bushing(b, (x, SCOV + 0.55, hz), "y", 3.9, core=0.18, sheds=(0.38, 0.3))
        # The arrester behind it on an insulating base on the cover, jumper over to the bushing's pad.
        for dx in (-0.12, 0.12):
            cyl(b, (x + dx, SCOV, az), (x + dx, SCOV + 0.12, az), 0.05, sw("dgrey"), sides=8)
        b.box(x - 0.2, SCOV + 0.12, az - 0.2, x + 0.2, SCOV + 0.17, az + 0.2, ALU)
        stack(b, x, az, SCOV + 0.17, 3.4, 0.11, (0.24, 0.19), units=3)
        at = SCOV + 0.17 + 3.4
        cyl(b, (x, at + 0.05, az), (x, at + 0.12, az), 0.14, ALU, sides=12)
        ring(b, (x, at - 0.25, az), "y", 0.46, 0.06, ALU, 20)
        p0, p2 = np.array([x, at + 0.12, az]), np.array(top) + [0, 0.02, 0]
        p1 = (p0 + p2) / 2 + [0, 0.25, 0]
        pts = [(1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t ** 2 * p2 for t in np.linspace(0, 1, 7)]
        for pa, pb in zip(pts, pts[1:]):
            rod(b, pa, pb, 0.02, sw("mgrey"), 6)
        # Its ground lead: along the cover to the -x edge, down the end wall to its counter (in sat_end).
        zl = 3.1 + k * 0.6
        path(b, [(x + 0.2, SCOV + 0.14, az), (x + 0.2, SCOV + 0.04, az), (x + 0.2, SCOV + 0.04, zl), (SX0 - 0.15, SCOV + 0.04, zl),
                 (SX0 - 0.15, 1.95, zl)], 0.016, COPPER)
        for y in (2.5, 3.8):
            b.box(SX0 - 0.19, y - 0.03, zl - 0.03, SX0, y + 0.03, zl + 0.03, GALV)
        stencil(b, (x, SCOV + 0.25, hz - 0.424), "-z", "H" + str(k + 1), 0.16)
    # H0 neutral bushing at the +x end of the cover, solid strap down the end wall to the grid.
    nx, nz = 7.4, 2.7
    cyl(b, (nx, SCOV, nz), (nx, SCOV + 0.2, nz), 0.2, GREY, sides=12)
    for i in range(6):
        y = SCOV + 0.22 + 0.13 * (i + 0.5)
        cyl(b, (nx, y - 0.015, nz), (nx, y + 0.015, nz), 0.17 if i % 2 == 0 else 0.14, PORC, sides=14)
    cyl(b, (nx, SCOV + 0.2, nz), (nx, SCOV + 1.05, nz), 0.08, PORC, sides=10)
    path(b, [(nx, SCOV + 1.05, nz), (nx, SCOV + 1.2, nz), (SX1 + 0.2, SCOV + 1.2, nz), (SX1 + 0.2, 0.3, nz),
             (SX1 + 0.6, 0.02, nz)], 0.03, COPPER)
    rod(b, (SX1 - 0.1, SCOV, nz), (SX1 - 0.1, SCOV + 1.12, nz), 0.035, GREY)
    for y in (1.6, 3.6):
        b.box(SX1 + 0.1, y - 0.04, nz - 0.04, SX1 + 0.17, y + 0.04, nz + 0.04, GREY)
    # Manhole, PRD with shield and its discharge pipe down the back corner.
    flange(b, (4.7, SCOV + 0.04, 3.9), "y", 0.32, 0.08, GREY, 10)
    cyl(b, (4.7, SCOV + 0.08, 3.9), (4.7, SCOV + 0.14, 3.9), 0.26, GREY, sides=20)
    px, pz = 6.6, 3.9
    cyl(b, (px, SCOV, pz), (px, SCOV + 0.18, pz), 0.15, GREY, sides=16)
    flange(b, (px, SCOV + 0.2, pz), "y", 0.2, 0.04, GREY, 8)
    b.box(px - 0.25, SCOV + 0.22, pz - 0.25, px + 0.25, SCOV + 0.48, pz + 0.25, [YELLOW, YELLOW, YELLOW, None, YELLOW, YELLOW])
    path(b, [(px + 0.25, SCOV + 0.35, pz), (8.4, SCOV + 0.35, pz), (8.4, SCOV + 0.35, SZ1 + 0.2), (8.4, 0.3, SZ1 + 0.2),
             (8.4, 0.3, SZ1 + 1.6), (8.4, -0.1, SZ1 + 1.6)], 0.07, GREY)
    for y in (1.2, 2.4, 3.7):
        b.box(8.36, y - 0.04, SZ1, 8.44, y + 0.04, SZ1 + 0.13, GREY)
    # Conservator: main section and the LTC's, on saddles along the back of the cover.
    cy, cz = SCOV + 1.5, 4.95
    for x0, x1 in ((1.0, 5.5), (5.75, 7.4)):
        cyl(b, (x0, cy, cz), (x1, cy, cz), 0.55, GREY, sides=24)
    for x, s in ((1.0, -1), (7.4, 1)):
        cyl(b, (x, cy, cz), (x + s * 0.1, cy, cz), 0.45, GREY, sides=24)
        cyl(b, (x + s * 0.1, cy, cz), (x + s * 0.16, cy, cz), 0.26, GREY, sides=20)
    cyl(b, (5.5, cy, cz), (5.75, cy, cz), 0.5, GREY, sides=24)
    for x in (1.6, 4.9, 6.6):
        b.box(x - 0.08, SCOV, cz - 0.45, x + 0.08, cy - 0.45, cz + 0.45, [GREY, GREY, GREY, GREY, None, None])
        b.box(x - 0.1, cy - 0.45, cz - 0.5, x + 0.1, cy - 0.38, cz + 0.5, GREY)
    for x, s, key, ch, lvl in ((1.0, -1, "olg", "oil_level", st["level"]), (7.4, 1, "olg", "ltc_level", st["ltc_level"])):
        n = np.array([s * math.cos(math.radians(30)), -math.sin(math.radians(30)), 0])
        c = np.array([x + s * 0.2, cy - 0.04, cz])
        cyl(b, c - n * 0.03, c, 0.12, GREY, sides=16)
        gauge(b, c, n, 0.11, FACES[key], lvl, 0, 100, start=180, sweep=180, channel=ch)
    # Main Buchholz pipe; the LTC's surge relay pipe forward along the cover to the LTC compartment.
    bx = 3.3
    path(b, [(bx, cy - 0.5, cz), (bx, cy - 0.8, cz), (bx, cy - 0.8, 4.0), (bx, SCOV, 4.0)], 0.06, GREY)
    b.box(bx - 0.17, cy - 1.0, 4.25, bx + 0.17, cy - 0.6, 4.6, GREY)
    tag(b, (bx, cy - 0.92, 4.246), "-z", "BUCHHOLZ 63", 0.3, 0.04)
    for z in (4.15, 4.75):
        ball_valve(b, (bx, cy - 0.8, z), "z", "y", 0.045)
    lx = 6.9
    path(b, [(lx, cy - 0.5, cz), (lx, cy - 0.7, cz), (lx, cy - 0.7, 3.4), (lx, SCOV + 0.08, 3.4), (lx, SCOV + 0.08, SZ0 - 0.3),
             (lx, 4.62, SZ0 - 0.3)], 0.04, GREY)
    b.box(lx - 0.13, cy - 0.85, 4.1, lx + 0.13, cy - 0.55, 4.4, GREY)
    tag(b, (lx, cy - 0.79, 4.096), "-z", "LTC SURGE 63", 0.24, 0.035)


def sat_lv(b, name):
    """Two LV throats on the +x end (X and Y windings), their bus ducts out and down into the floor; X0 and Y0
    neutral bushings to their grounding resistors in front of the end."""
    a = ALU
    for zc, w in ((2.75, "X"), (4.55, "Y")):
        z0, z1 = zc - 0.65, zc + 0.65
        b.box(SX1, 2.2, z0 - 0.1, SX1 + 0.3, 3.3, z1 + 0.1, [a, a, None, a, a, a])
        b.box(SX1 + 0.3, 2.3, z0, 9.3, 3.0, z1, [a, a, None, None, a, a])
        b.box(9.3, 0.0, z0, 9.9, 3.0, z1, [a, a, a, a, a, None])
        for x0, x1 in ((SX1 + 0.3, SX1 + 0.36), (9.22, 9.3)):
            b.box(x0, 2.26, z0 - 0.04, x1, 3.04, z1 + 0.04, a)
        for y in (0.06, 1.5):
            b.box(9.26, y, z0 - 0.04, 9.94, y + 0.06, z1 + 0.04, a)
        b.box(9.2, -0.02, z0 - 0.1, 10.0, 0.04, z1 + 0.1, GALV)
        tag(b, (9.94, 1.75, zc), "x", f"BD-{w}  4160 V", 0.36, 0.045)
    # Neutral bushings high on the end, straps down to the two resistors in front of the end.
    for k, (w, yb) in enumerate((("X", 3.75), ("Y", 4.35))):
        zc = 1.85
        cyl(b, (SX1, yb, zc), (SX1 + 0.07, yb, zc), 0.13, GREY, sides=14)
        for i in range(4):
            x = SX1 + 0.1 + 0.08 * i
            cyl(b, (x, yb, zc), (x + 0.03, yb, zc), 0.11 if i % 2 == 0 else 0.09, PORC, sides=14)
        cyl(b, (SX1 + 0.07, yb, zc), (SX1 + 0.42, yb, zc), 0.055, PORC, sides=10)
        cyl(b, (SX1 + 0.42, yb, zc), (SX1 + 0.55, yb, zc), 0.025, COPPER, sides=8)
        nx0 = 8.9 + k * 0.95
        ncx = nx0 + 0.4
        path(b, [(SX1 + 0.52, yb, zc), (ncx, yb, zc), (ncx, yb, 0.65), (ncx, 2.32, 0.65)], 0.02, COPPER)
        nz0, nz1, ny0, ny1 = 0.25, 1.05, 0.95, 1.95
        b.box(nx0, ny0, nz0, nx0 + 0.8, ny1, nz1, ["louvre", "louvre", "louvre", "louvre", ALU, ALU])
        b.box(nx0 - 0.04, ny1, nz0 - 0.04, nx0 + 0.84, ny1 + 0.05, nz1 + 0.04, ALU)
        for x in (nx0 + 0.05, nx0 + 0.75):
            for z in (nz0 + 0.05, nz1 - 0.05):
                b.box(x - 0.035, 0.0, z - 0.035, x + 0.035, ny0, z + 0.035, GALV)
        cyl(b, (ncx, ny1 + 0.05, 0.65), (ncx, ny1 + 0.09, 0.65), 0.08, GREY, sides=12)
        for i in range(3):
            y = ny1 + 0.1 + 0.06 * i
            cyl(b, (ncx, y, 0.65), (ncx, y + 0.025, 0.65), 0.07, PORC, sides=12)
        cyl(b, (ncx, ny1 + 0.09, 0.65), (ncx, 2.3, 0.65), 0.04, PORC, sides=8)
        tag(b, (ncx, 1.75, nz0 - 0.004), "-z", f"NGR-{w}  400 A 10 s", 0.5, 0.045)
        path(b, [(nx0 + 0.6, 1.05, nz0), (nx0 + 0.6, 1.05, nz0 - 0.04), (nx0 + 0.6, 0.3, nz0 - 0.04), (nx0 + 0.6, 0.02, nz0 - 0.4)], 0.02, COPPER)


def sat_radiators(b, st):
    """Seven panel radiators on the back on valved headers, three fans on a frame in two stages."""
    zr0, zr1 = SZ1 + 0.35, SZ1 + 0.95
    for xc in SAT_RADS:
        for y in (4.4, 1.05):
            rod(b, (xc, y, SZ1), (xc, y, zr0 + 0.07), 0.075, GREY, 12)
            flange(b, (xc, y, SZ1 + 0.06), "z", 0.13, 0.03, GREY, 6)
            cyl(b, (xc, y, SZ1 + 0.13), (xc, y, SZ1 + 0.23), 0.11, GREY, sides=12)
            rod(b, (xc, y + 0.11, SZ1 + 0.18), (xc, y + 0.2, SZ1 + 0.18), 0.015, GREY, 6)
            bar(b, (xc - 0.1, y + 0.21, SZ1 + 0.18), (xc + 0.1, y + 0.21, SZ1 + 0.18), 0.025, sw("red"), 0.015)
            rod(b, (xc - 0.3, y, zr0 + 0.08), (xc + 0.3, y, zr0 + 0.08), 0.07, GREY, 12)
        for k in range(12):
            x = xc - 0.275 + k * 0.05
            b.box(x - 0.007, 0.98, zr0, x + 0.007, 4.47, zr1, GREY)
        b.box(xc - 0.3, 2.7, zr1, xc + 0.3, 2.76, zr1 + 0.02, GREY)
    zf = zr1 + 0.05
    for xf, grp in ((2.2, 1), (4.7, 1), (7.2, 2)):
        for x in (xf - 0.7, xf + 0.7):
            b.box(x - 0.04, 0.0, zf + 0.05, x + 0.04, 3.4, zf + 0.13, [GREY, GREY, GREY, GREY, GREY, None])
            b.box(x - 0.1, 0.0, zf - 0.01, x + 0.1, 0.02, zf + 0.19, GALV)
        for y in (2.6 - 0.62, 2.6 + 0.62):
            b.box(xf - 0.66, y - 0.03, zf + 0.06, xf + 0.66, y + 0.03, zf + 0.12, GREY)
        fan(b, np.array([xf, 2.6, zf]), 0.5 + xf, f"fans{grp}")
        path(b, [(xf + 0.12, 2.6, zf + 0.27), (xf + 0.12, 2.6 - 0.62, zf + 0.27), (xf + 0.6, 2.6 - 0.62, zf + 0.27),
                 (xf + 0.6, 0.0, zf + 0.27)], 0.016, GALV)


def sat_front(b, name, st):
    """Front: the LTC compartment (bolted cover), drain and sample valves, rating plate, stencil, the breathers."""
    z = SZ0
    # LTC compartment on the front, +x end.
    cx0, cx1, cy0, cy1 = 5.6, 8.1, 2.6, 4.6
    b.box(cx0, cy0, z - 0.55, cx1, cy1, z, [GREY, None, GREY, GREY, GREY, GREY])
    b.box(cx0 + 0.08, cy0 + 0.08, z - 0.58, cx1 - 0.08, cy1 - 0.08, z - 0.55, GREY)
    for x in np.arange(cx0 + 0.15, cx1 - 0.1, 0.3):
        for y in (cy0 + 0.12, cy1 - 0.12):
            cyl(b, (x, y, z - 0.58), (x, y, z - 0.61), 0.018, BLACK, sides=6)
    for y in np.arange(cy0 + 0.4, cy1 - 0.2, 0.3):
        for x in (cx0 + 0.12, cx1 - 0.12):
            cyl(b, (x, y, z - 0.58), (x, y, z - 0.61), 0.018, BLACK, sides=6)
    tag(b, ((cx0 + cx1) / 2, cy1 - 0.3, z - 0.584), "-z", "ON-LOAD TAP CHANGER", 0.6, 0.05)
    # Drain (gate valve), oil sample.
    rod(b, (3.4, 0.82, z), (3.4, 0.82, z - 0.22), 0.05, GREY)
    gate_valve(b, (3.4, 0.82, z - 0.33), "-z", "y", 0.05)
    flange(b, (3.4, 0.82, z - 0.47), "-z", 0.07, 0.025, GREY, 6)
    tag(b, (3.4, 1.2, z - 0.004), "-z", "DRAIN", 0.22, 0.04)
    rod(b, (4.6, 1.0, z), (4.6, 1.0, z - 0.12), 0.02, GREY)
    ball_valve(b, (4.6, 1.0, z - 0.17), "-z", "y", 0.03)
    tag(b, (4.6, 1.2, z - 0.004), "-z", "OIL SAMPLE", 0.26, 0.04)
    px, py = 4.6, 3.6
    for dx in (-0.24, 0.24):
        for dy in (-0.16, 0.16):
            b.box(px + dx - 0.02, py + dy - 0.02, z - 0.04, px + dx + 0.02, py + dy + 0.02, z, sw("dgrey"))
    b.box(px - 0.27, py - 0.2, z - 0.05, px + 0.27, py + 0.2, z - 0.04, [ATLAS.mat(FACES["plate_sat"]), None, ALU, ALU, ALU, ALU])
    stencil(b, (3.4, 4.3, z - 0.003), "-z", name, 0.32)
    # Breathers: the main one down the front at the -x end, the LTC's beside its compartment.
    cy, cz = SCOV + 1.5, 4.95
    for bx, x_src, top_y in ((0.95, 1.25, cy - 0.5), (8.35, 7.2, cy - 0.5)):
        path(b, [(x_src, top_y, cz - 0.3), (x_src, SCOV + 0.04, cz - 0.3), (bx, SCOV + 0.04, cz - 0.3), (bx, SCOV + 0.04, z - 0.3),
                 (bx, 2.05, z - 0.3)], 0.03, GREY)
        for y in (3.4, 3.95):
            b.box(bx - 0.04, y - 0.04, z - 0.26, bx + 0.04, y + 0.04, z, GREY)
        cyl(b, (bx, 1.95, z - 0.3), (bx, 2.05, z - 0.3), 0.12, GREY, sides=14)
        cyl(b, (bx, 1.45, z - 0.3), (bx, 1.95, z - 0.3), 0.1, "glass", sides=14)
        cyl(b, (bx, 1.47, z - 0.3), (bx, 1.86, z - 0.3), 0.085, sw("orange"), sides=12)
        cyl(b, (bx, 1.35, z - 0.3), (bx, 1.45, z - 0.3), 0.12, GREY, sides=14)
        cyl(b, (bx, 1.2, z - 0.3), (bx, 1.35, z - 0.3), 0.08, "glass", sides=12)
        b.box(bx - 0.05, 1.62, z - 0.18, bx + 0.05, 1.72, z, GREY)


def sat_end(b):
    """The -x end: ladder with locked guard, the three arrester surge counters, filter press valve."""
    x = SX0
    lz = 2.0
    for dz in (-0.22, 0.22):
        b.box(x - 0.34, 0.0, lz + dz - 0.03, x - 0.28, SCOV + 1.1, lz + dz + 0.03, [GALV] * 5 + [None])
        for y in (1.9, 3.6):
            b.box(x - 0.28, y - 0.03, lz + dz - 0.02, x, y + 0.03, lz + dz + 0.02, GALV)
    for y in np.arange(0.8, SCOV + 0.9, 0.3):
        cyl(b, (x - 0.31, y, lz - 0.19), (x - 0.31, y, lz + 0.19), 0.016, GALV, sides=6)
    b.box(x - 0.4, 0.7, lz - 0.26, x - 0.38, 2.6, lz + 0.26, GALV)
    b.box(x - 0.47, 1.75, lz + 0.12, x - 0.4, 1.9, lz + 0.2, YELLOW)
    for k in range(3):
        zl = 3.1 + k * 0.6
        b.box(x - 0.22, 1.67, zl - 0.12, x - 0.1, 1.95, zl + 0.12, GREY)
        b.box(x - 0.1, 1.77, zl - 0.03, x, 1.85, zl + 0.03, GREY)
        gauge(b, (x - 0.22, 1.81, zl), "-x", 0.05, FACES["ctr"], 2 + k, 0, 10, channel=f"sat_arr{k + 1}", case=GREY)
        tag(b, (x - 0.004, 2.1, zl), "-x", f"ARR H{k + 1}", 0.16, 0.03)
        path(b, [(x - 0.16, 1.67, zl), (x - 0.16, 0.3, zl), (x - 0.16, 0.02, zl - 0.45)], 0.016, COPPER)
    fz = 4.9
    rod(b, (x, 3.6, fz), (x - 0.2, 3.6, fz), 0.05, GREY)
    gate_valve(b, (x - 0.31, 3.6, fz), "-x", "y", 0.05)
    flange(b, (x - 0.45, 3.6, fz), "-x", 0.07, 0.025, GREY, 6)
    tag(b, (x - 0.004, 3.3, fz), "-x", "FILTER PRESS", 0.3, 0.04)


def sat_cabinet(b, name, st):
    x0, x1, y0, y1 = 1.35, 2.7, 0.9, 2.95
    f, z0 = cabinet_shell(b, x0, x1, y0, y1, SZ0)
    tag(b, ((x0 + x1) / 2, y1 - 0.14, f), "-z", f"{name}  COOLING & MONITORING", 0.9, 0.05)
    gauge(b, (x0 + 0.3, 2.47, f), "-z", 0.14, FACES["oti"], st["oil"], 0, 150, st["oil_max"], channel="top_oil")
    gauge(b, (x0 + 0.71, 2.47, f), "-z", 0.14, FACES["wti"], st["wdg"], 0, 150, st["wdg_max"], channel="winding")
    tag(b, (x0 + 0.3, 2.26, f), "-z", "26Q  TOP OIL", 0.25, 0.035)
    tag(b, (x0 + 0.71, 2.26, f), "-z", "49  WINDING", 0.25, 0.035)
    for cy, col, on, txt in ((2.62, "green", st["fans"] >= 1, "GROUP 1"), (2.46, "green", st["fans"] >= 2, "GROUP 2"),
                             (2.3, "white", True, "AC SUPPLY")):
        tag(b, (x0 + 1.04, cy, f), "-z", txt, 0.16, 0.03)
        lamp(b, (x0 + 1.18, cy, f), "-z", col, on, 0.02)
    for k, txt in enumerate(("HI TEMP", "BUCHHOLZ", "LTC SURGE", "LOW OIL")):
        cx = x0 + 0.2 + k * 0.25
        tag(b, (cx, 2.06, f), "-z", txt, 0.2, 0.03)
        lamp(b, (cx, 1.96, f), "-z", "red", txt in st["alarms"])
    for k, txt in enumerate(("GROUP 1  MAN-OFF-AUTO", "GROUP 2  MAN-OFF-AUTO")):
        cx = x0 + 0.33 + k * 0.6
        tag(b, (cx, 1.82, f), "-z", txt, 0.44, 0.03)
        selector(b, (cx, 1.7, f), "-z", 45)
    cabinet_wiring(b, x0, x1, f, [st["fans"] >= 1, st["fans"] >= 1, st["fans"] >= 2, True, False, False])
    cabinet_door(b, x0, x1, y0, y1, z0, ["WIRING DIAGRAM", "SAT COOLING CONTROL", "DWG E-4490  REV 1", "", "TB1-TB3  K1-K6"])


def ltc_drive(b, name, st):
    """The LTC motor drive cabinet at reach height under the compartment, door open: the tap position indicator with
    min and max drag hands, operations counter, RAISE / LOWER, LOCAL / REMOTE, the hand crank, the drive shaft up
    through a bevel gearbox into the compartment."""
    x0, x1, y0, y1 = 6.15, 7.55, 0.7, 2.3
    f, z0 = cabinet_shell(b, x0, x1, y0, y1, SZ0)
    tag(b, ((x0 + x1) / 2, y1 - 0.12, f), "-z", "LTC MOTOR DRIVE", 0.6, 0.045)
    # Tap position dial: needle on the tap, red drag hands on the lowest and highest since reset (live).
    tc = np.array([x0 + 0.4, 1.88, f])
    cyl(b, tc, tc + [0, 0, -0.03], 0.145, BLACK, sides=20)
    face_quad(b, tc + [0, 0, -0.034], "-z", 0.24, 0.24, ATLAS.mat(FACES["ltc"]))
    ring(b, tc + [0, 0, -0.042], "-z", 0.132, 0.024, "stainless", 20)
    up, right = np.array([0, 1.0, 0]), np.array([1.0, 0, 0])
    for val, mat, wd, dz in ((st["tap_min"], sw("red"), 0.007, 0.04), (st["tap_max"], sw("red"), 0.007, 0.041),
                             (st["tap"], BLACK, 0.011, 0.046)):
        t = math.radians(225 - 270 * (val - 1) / 32)
        tip = tc + [0, 0, -dz] + (math.cos(t) * right + math.sin(t) * up) * 0.1
        bar(b, tc + [0, 0, -dz] - (math.cos(t) * right + math.sin(t) * up) * 0.02, tip, wd, mat, h=0.004)
    cyl(b, tc + [0, 0, -0.034], tc + [0, 0, -0.052], 0.01, BLACK, sides=8)
    b.live.append({"t": "needle", "c": _rnd(tc + [0, 0, -0.046]), "n": [0, 0, -1], "r": 0.1, "lo": 1, "hi": 33,
                   "start": 225, "sweep": 270, "ch": "ltc_tap", "drag": True})
    ckey = ATLAS.add("counter:ltc", counter_img(21877, 0.1, 0.03))
    tag(b, (x0 + 0.4, 1.68, f), "-z", "OPERATIONS", 0.18, 0.028)
    b.box(x0 + 0.33, 1.6, f - 0.02, x0 + 0.47, 1.64, f, sw("dgrey"))
    face_quad(b, (x0 + 0.4, 1.62, f - 0.021), "-z", 0.1, 0.03, ATLAS.mat(ckey))
    xr = x0 + 0.98
    tag(b, (xr, 2.05, f), "-z", "LOCAL - REMOTE", 0.24, 0.03)
    selector(b, (xr, 1.95, f), "-z", 135, 0.025)
    for dx, txt, col in ((-0.12, "LOWER", "black"), (0.12, "RAISE", "black")):
        tag(b, (xr + dx, 1.82, f), "-z", txt, 0.13, 0.028)
        push_button(b, (xr + dx, 1.74, f), "-z", col, 0.022)
    cyl(b, (xr + 0.16, 1.58, f), (xr + 0.16, 1.58, f - 0.04), 0.028, BLACK, sides=6)
    tag(b, (xr - 0.05, 1.58, f), "-z", "HAND CRANK", 0.2, 0.026)
    cabinet_wiring(b, x0, x1, f, [False, False, True, False])
    b.box(x0 + 0.65, y1 + 0.04, z0 + 0.15, x0 + 0.77, y1 + 0.16, z0 + 0.27, GREY)                    # bevel gearbox
    rod(b, (x0 + 0.71, y1 + 0.16, z0 + 0.21), (x0 + 0.71, 2.6, z0 + 0.21), 0.025, GALV, 8)            # drive shaft up
    cabinet_door(b, x0, x1, y0, y1, z0, ["WIRING DIAGRAM", "LTC DRIVE", "DWG E-4492  REV 1", "", "TB1-TB2  K1-K4"])


# ---------------------------------------------------------------- power lines: the 345 kV lattice tower (new lattice tier I)
#
# Real basis: a 345 kV single-circuit "waist" (window) suspension tower as US utilities use: four galvanized angle
# legs on concrete pier footings (stub angles, leg extensions on slopes), the body tapering from about 8 m square to a
# 2.4 m waist at 16 m, X-braced faces with horizontal struts and redundant members; above the waist two upper legs
# spread out to the bridge (a box-truss crossarm about 18 m wide, 25 m up). The outer phases hang from the bridge ends
# on I-strings, the centre phase inside the window on a V-string so it cannot swing into the steel; each phase is a
# twin bundle (two 30 mm ACSR sub-conductors 0.45 m apart) in a suspension clamp below a grading ring. Two shield-wire
# peaks over the outer legs. Anti-climbing guard at 3 m, step bolts up one leg, a tower number plate and a danger
# plate, every leg bonded to the ground. Dead-end variant (line ends and angles over about 10 degrees): heavier, the
# phases on double strain strings both ways with jumper loops between them.
#
# Frame: tower centred on x = z = 0, the line along z (the head turns to the line in game, the body stays square).

T345 = dict(base=4.0, waist=1.2, waist_y=16.0, bridge_y=25.0, bridge_w=9.0, peak_y=30.5, phase_x=(-8.5, 0.0, 8.5), string=3.3)


def tower_345(dead_end=False, number="LINE 1  TOWER 1"):
    _faces()
    if "danger" not in FACES:
        FACES["danger"] = ATLAS.add("flag:danger", flag_img("DANGER  345 kV", (196, 30, 30), (250, 250, 250), 0.34, 0.12))
    T = T345
    b = Board(10, 1801, face=0.0)
    b.live = []
    B, Wd, wy, by = T["base"], T["waist"], T["waist_y"], T["bridge_y"]
    half = lambda y: B - (B - Wd) * min(1.0, y / wy)
    leg = 0.2
    # Footings and stubs, the four legs (angle sections: two flanges each), their ground straps.
    for sx in (-1, 1):
        for sz in (-1, 1):
            x, z = sx * B, sz * B
            b.box(x - 0.55, -0.6, z - 0.55, x + 0.55, 0.35, z + 0.55, ["concrete"] * 5 + [None])
            pts = [(sx * half(y), y, sz * half(y)) for y in (0.35, wy)]
            angle_member(b, pts[0], pts[1], sx, sz, leg)
            path(b, [(x - sx * 0.35, 0.6, z - sz * 0.0), (x - sx * 0.62, 0.6, z), (x - sx * 0.62, 0.36, z), (x - sx * 0.62, 0.02, z + sz * 0.7)],
                 0.016, COPPER)
    # Body faces: panels of X bracing (the two diagonals a hair apart), struts at the panel lines.
    levels = [0.35, 3.0, 5.6, 8.0, 10.2, 12.3, 14.2, wy]
    for f in range(4):
        for y0, y1 in zip(levels, levels[1:]):
            a0, b0 = face_pts(f, half(y0), y0)
            a1, b1 = face_pts(f, half(y1), y1)
            n = face_normal(f)
            bar(b, np.array(a0) + n * 0.02, np.array(b1) + n * 0.02, 0.09, GALV)
            bar(b, np.array(b0) - n * 0.03, np.array(a1) - n * 0.03, 0.09, GALV)
            if y0 > 0.4:
                sa, sb = shrink(a0, b0, 0.2)
                bar(b, sa - n * 0.06, sb - n * 0.06, 0.11, GALV, h=0.07)
    # Horizontal plan bracing at the waist (a diamond so nothing crosses in one plane).
    for f in range(4):
        a, c = face_pts(f, Wd, wy)
        m = (np.array(a) + np.array(c)) / 2
        q0, q1 = face_pts((f + 1) % 4, Wd, wy)
        bar(b, *shrink(m + [0, 0.05 + 0.03 * (f % 2), 0], (np.array(q0) + np.array(q1)) / 2 + [0, 0.05 + 0.03 * (f % 2), 0], 0.1),
            0.08, GALV)
    # Anti-climbing guard: a girdle of angles round the body at 3 m with spikes out, a locked gap on one leg.
    for f in range(4):
        a, c = face_pts(f, half(3.2) + 0.08, 3.2)
        bar(b, *shrink(a, c, 0.12 + 0.02 * (f % 2)), 0.06, GALV, h=0.06 + 0.01 * (f % 2))
        a = np.array(a); c = np.array(c); n = face_normal(f)
        for t in np.linspace(0.06, 0.94, 14):
            p = a + (c - a) * t
            rod(b, p + [0, 0.03, 0], p + n * 0.22 + [0, 0.25, 0], 0.008, GALV, 4)
    # Step bolts up the leg at (+x, -z) from the guard to the bridge, the plates on that leg below the guard.
    for y in np.arange(3.8, wy, 0.4):
        p = np.array([half(y), y, -half(y)])
        rod(b, p + [0.1, 0, 0], p + [0.28, 0, 0], 0.012, GALV, 6)
    face_quad(b, (half(2.3) - 0.6, 2.3, -half(2.3) - 0.16), "-z", 0.34, 0.12, ATLAS.mat(FACES["danger"]))
    b.box(half(2.3) - 0.79, 2.22, -half(2.3) - 0.15, half(2.3) - 0.41, 2.38, -half(2.3) - 0.11, GALV)
    tag(b, (half(1.95) - 0.6, 1.95, -half(1.95) - 0.154), "-z", number, 0.4, 0.06)
    b.box(half(1.95) - 0.81, 1.9, -half(1.95) - 0.15, half(1.95) - 0.39, 2.0, -half(1.95) - 0.11, GALV)
    # The head: upper legs from the waist out to the bridge, the bridge, peaks, strings.
    tower_head(b, dead_end)
    return b


def angle_member(b, p0, p1, sx, sz, w, t=0.03):
    """A galvanized angle from p0 to p1 (its heel line), the two flanges reaching w inward (toward -sx and -sz), t
    thick; the x flange stops at the z flange's inner face so their faces never coincide."""
    p0, p1 = np.array(p0, float), np.array(p1, float)
    u = (p1 - p0) / np.linalg.norm(p1 - p0)
    mid, L = (p0 + p1) / 2, np.linalg.norm(p1 - p0)
    for axis, s_, other, so, full in ((np.array([1.0, 0, 0]), sx, np.array([0, 0, 1.0]), sz, False),
                                      (np.array([0, 0, 1.0]), sz, np.array([1.0, 0, 0]), sx, True)):
        v = axis - u * (axis @ u)
        v /= np.linalg.norm(v)
        wv = np.cross(u, v)
        wv = wv if wv @ other * so > 0 else -wv
        span = w if full else w - t
        c = mid - v * s_ * (span / 2 + (0 if full else t)) - wv * (t / 2) + (wv * 0.006 if full else 0)
        b.obox(tuple(_rnd(c)), tuple(_rnd(u)), tuple(_rnd(v)), round(L / 2, 4), round(span / 2, 4), round(t / 2, 4), GALV)


def shrink(a, c, d):
    """The segment a-c pulled in by d at each end."""
    a, c = np.array(a, float), np.array(c, float)
    u = (c - a) / np.linalg.norm(c - a)
    return a + u * d, c - u * d


def face_pts(f, r, y):
    """A body face's two corners at half-width r and height y (faces -z, +x, +z, -x)."""
    c = [(-r, -r), (r, -r), (r, r), (-r, r)]
    a, d = c[f], c[(f + 1) % 4]
    return (a[0], y, a[1]), (d[0], y, d[1])


def face_normal(f):
    return np.array([(0, 0, -1), (1, 0, 0), (0, 0, 1), (-1, 0, 0)][f], float)


def tower_head(b, dead_end):
    T = T345
    Wd, wy, by, bw = T["waist"], T["waist_y"], T["bridge_y"], T["bridge_w"]
    # Upper legs: from each waist corner up and out to the bridge's bottom chords at x +-5.6.
    for sx in (-1, 1):
        for sz in (-1, 1):
            angle_member(b, (sx * Wd, wy, sz * Wd), (sx * 5.6, by, sz * 0.75), sx, sz, 0.18)
        # their bracing (the two upper legs on one side tied together in a K)
        for t in (0.35, 0.7):
            p = np.array([sx * (Wd + (5.6 - Wd) * t), wy + (by - wy) * t, 0])
            zz = Wd + (0.75 - Wd) * t
            bar(b, p + [0, 0, -zz], p + [0, 0, zz], 0.09, GALV)
            p2 = np.array([sx * (Wd + (5.6 - Wd) * (t - 0.17)), wy + (by - wy) * (t - 0.17), 0])
            bar(b, p + [0, 0, -zz + 0.04], p2 + [0, 0.04, 0.0], 0.07, GALV)
            bar(b, p + [0, 0, zz - 0.04], p2 + [0, -0.04, 0.0], 0.07, GALV)
    # The bridge: a box truss, chords at z +-0.75, y by and by + 1.5, from x -bw to bw.
    for zz in (-0.75, 0.75):
        for yy in (by, by + 1.5):
            bar(b, (-bw, yy, zz), (bw, yy, zz), 0.16, GALV, h=0.16 if yy == by else 0.14)
    xs = np.linspace(-bw, bw, 13)
    for i, (x0, x1) in enumerate(zip(xs, xs[1:])):
        for zz in (-0.75, 0.75):
            s = 1 if i % 2 == 0 else -1
            bar(b, (x0, by + 0.08, zz + 0.04 * s), (x1, by + 1.42, zz + 0.04 * s), 0.07, GALV)        # face diagonals
        yp = by + 1.47 + 0.035 * (i % 2)
        bar(b, (x0, yp, -0.7 * (1 - 2 * (i % 2))), (x1, yp, 0.7 * (1 - 2 * (i % 2))), 0.06, GALV, h=0.05)  # top plan bracing
    for x in xs:
        for yy in (by, by + 1.5):
            bar(b, (x, yy + 0.02, -0.72), (x, yy + 0.02, 0.72), 0.07, GALV, h=0.06)                  # cross struts
        for zz in (-0.75, 0.75):
            bar(b, (x, by + 0.08, zz), (x, by + 1.42, zz), 0.08, GALV, h=0.09)                      # verticals
    # Shield-wire peaks over the outer legs.
    for sx in (-1, 1):
        for zz in (-0.75, 0.75):
            bar(b, (sx * 5.0, by + 1.5, zz), (sx * 5.6, T["peak_y"], zz * 0.15), 0.12, GALV)
            bar(b, (sx * 6.2, by + 1.5, zz), (sx * 5.6, T["peak_y"], zz * 0.15), 0.105, GALV, h=0.135)
        for t in (0.3, 0.6):
            y = by + 1.5 + (T["peak_y"] - by - 1.5) * t
            bar(b, (sx * 5.6 - 0.5 * (1 - t), y, -0.7 * (1 - t)), (sx * 5.6 + 0.5 * (1 - t), y, 0.7 * (1 - t)), 0.05, GALV)
        cyl(b, (sx * 5.6, T["peak_y"], 0), (sx * 5.6, T["peak_y"] + 0.15, 0), 0.08, GALV, sides=8)
        b.box(sx * 5.6 - 0.06, T["peak_y"] + 0.15, -0.25, sx * 5.6 + 0.06, T["peak_y"] + 0.27, 0.25, ALU)   # shield clamp
    # The phases.
    for k, px in enumerate(T["phase_x"]):
        if dead_end:
            for s in (-1, 1):
                dead_end_string(b, px, by, s)
            loop = [np.array([px, by - 0.05, -T["string"] - 0.55]), np.array([px, by - 2.0, 0.0]), np.array([px, by - 0.05, T["string"] + 0.55])]
            pts = [(1 - t) ** 2 * loop[0] + 2 * (1 - t) * t * loop[1] + t ** 2 * loop[2] for t in np.linspace(0, 1, 12)]
            for dz in (-0.12, 0.12):
                for pa, pb in zip(pts, pts[1:]):
                    rod(b, pa + [dz, 0, 0], pb + [dz, 0, 0], 0.016, sw("mgrey"), 6)
        elif px == 0.0:
            for s in (-1, 1):
                disc_string(b, np.array([s * 1.7, by - 0.05, 0]), np.array([s * 0.12, by - 3.15, 0]), 14)
            suspension_clamp(b, np.array([0, by - 3.2, 0]))
        else:
            disc_string(b, np.array([px, by - 0.05, 0]), np.array([px, by - 3.25, 0]), 18)
            suspension_clamp(b, np.array([px, by - 3.3, 0]))


def disc_string(b, top, bottom, n):
    """An insulator string of n glass discs from top to bottom: shackle, discs on their caps, grading ring at the
    line end."""
    top, bottom = np.array(top, float), np.array(bottom, float)
    a = (bottom - top) / np.linalg.norm(bottom - top)
    L = np.linalg.norm(bottom - top)
    rod(b, top, top + a * 0.15, 0.03, GALV, 6)
    rod(b, top + a * 0.15, bottom - a * 0.2, 0.022, GALV, 6)
    for i in range(n):
        c = top + a * (0.25 + i * (L - 0.5) / n)
        cyl(b, c, c + a * 0.03, 0.13, "glass", sides=12)
        cyl(b, c + a * 0.03, c + a * 0.07, 0.055, "glass", sides=10)
    ring(b, bottom - a * 0.25, a, 0.32, 0.05, ALU, 18)


def suspension_clamp(b, c):
    """The yoke under the string and the two suspension clamps holding the twin bundle."""
    c = np.array(c, float)
    b.box(*_rnd(c + [-0.3, -0.05, -0.05]), *_rnd(c + [0.3, 0.05, 0.05]), GALV)
    for dx in (-0.225, 0.225):
        b.box(*_rnd(c + [dx - 0.05, -0.22, -0.12]), *_rnd(c + [dx + 0.05, -0.05, 0.12]), ALU)
        rod(b, c + [dx, -0.2, -0.6], c + [dx, -0.2, 0.6], 0.016, sw("mgrey"), 6)                       # conductor stubs


def dead_end_string(b, px, by, s):
    """Double strain strings from the bridge's side along the line (s: +-z), yoke plates and compression dead-ends."""
    y = by - 0.05
    z0 = s * 0.8
    L = T345["string"]
    b.box(px - 0.32, y - 0.04, min(z0, z0 + s * 0.05), px + 0.32, y + 0.04, max(z0, z0 + s * 0.05), GALV)
    for dx in (-0.22, 0.22):
        disc_string(b, np.array([px + dx, y, z0 + s * 0.05]), np.array([px + dx, y, z0 + s * (L - 0.2)]), 18)
    ze = z0 + s * (L - 0.15)
    b.box(px - 0.32, y - 0.04, min(ze, ze + s * 0.05), px + 0.32, y + 0.04, max(ze, ze + s * 0.05), GALV)
    for dx in (-0.225, 0.225):
        cyl(b, (px + dx, y, ze + s * 0.05), (px + dx, y, ze + s * 0.45), 0.04, ALU, sides=8)


# ---------------------------------------------------------------- switchyard: the outgoing line dead-end gantry


LINE_ENTRY_PADS = {"A": (291.05, 9.09, 43.3), "B": (288.65, 8.1, 47.3), "C": (291.05, 9.09, 51.3)}   # line 1's, in the site


def line_gantry(x=294.0, name="LINE 1"):
    """The switchyard's outgoing dead-end gantry for a line (the same H-frame as the incoming one): the line's three
    phases dead-ended on its outer face, slack jumpers looping back under the beam and down onto the line traps'
    upper pads (phases A and C) and the bus end bell's terminal on B."""
    b = Board(9, 1311, face=0.0)
    b.live = []
    G = GANTRY
    O = (x, 0.0, G["zs"][0])
    F = yard_F(O)
    gantry(b, F, x, low=False)
    for ph, zc in G["phases"].items():
        strain_string(b, F, x + 0.5, zc, +1)
        xe = x + 0.5 + G["string"] - 0.05
        tgt = LINE_ENTRY_PADS[ph]
        p0, p2 = np.array([xe, G["beam_y"] - 0.05, zc]), np.array(tgt)
        p1 = np.array([x + 0.3, G["beam_y"] - 3.2, (zc + tgt[2]) / 2])
        pts = [(1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t ** 2 * p2 for t in np.linspace(0, 1, 16)]
        for dz in (-0.12, 0.12):
            for pa, pb in zip(pts, pts[1:]):
                rod(b, F(pa[0], pa[1], pa[2] + dz), F(pb[0], pb[1], pb[2] + dz), 0.016, sw("mgrey"), 6)
    return b


# ---------------------------------------------------------------- the bay (the pit, firewalls, deluge) for "in place"

GSU_BAY = dict(plinth=(0.2, 1.0, 8.8, 7.6), curb_x=(-2.0, 11.0), curb_z=(-1.4, 9.6), wall=(-1.4, 9.8, 10.6),
               rings=((3.5, (-0.7, 9.7, 0.4, 8.2)), (6.5, (0.1, 8.9, 0.9, 6.1))), aim=(4.5, 3.5), stands=None)


def bay(b, firewall_x=(-2.2, 11.2), spec=None):
    """The transformer's bay: the concrete plinth it stands on (top at 0.12; the transformer's frame starts there),
    the oil containment pit's curb front and back (the pit itself is gravel blocks), the firewalls, the deluge rings on
    pipe stanchions with base plates, nozzles aimed at the tank. spec: GSU_BAY (default) or UAT_BAY."""
    sp = spec or GSU_BAY
    c = "concrete"
    px0, pz0, px1, pz1 = sp["plinth"]
    b.box(px0, 0, pz0, px1, 0.12, pz1, [c, c, c, c, c, None])
    (cx0, cx1), (cz0, cz1) = sp["curb_x"], sp["curb_z"]
    for a, z in (((cx0, 0, cz0), (cx1, 0.45, cz0 + 0.2)), ((cx0, 0, cz1), (cx1, 0.45, cz1 + 0.2))):
        b.box(*a, *z, [c, c, c, c, c, None])
    wz0, wz1, wh = sp["wall"]
    for x in firewall_x:
        b.box(x - 0.2, 0, wz0, x + 0.2, wh, wz1, [c, c, c, c, c, None])
    ax_, az_ = sp["aim"]
    for k, (y, (x0, x1, z0, z1)) in enumerate(sp["rings"]):
        pts = [(x0, y, z0), (x1, y, z0), (x1, y, z1), (x0, y, z1), (x0, y, z0)]
        path(b, pts, 0.055, "pipe_red")
        stands = ((x0, z0), (x1, z0), (x0, z1), (x1, z1), ((x0 + x1) / 2, z0), ((x0 + x1) / 2, z1)) if sp["stands"] is None \
            else sp["stands"][k]
        for (xa, za) in stands:
            rod(b, (xa, 0.0, za), (xa, y - 0.055, za), 0.045, GALV)
            b.box(xa - 0.12, 0.0, za - 0.12, xa + 0.12, 0.02, za + 0.12, GALV)
            b.box(xa - 0.07, y - 0.1, za - 0.07, xa + 0.07, y - 0.055, za + 0.07, GALV)
        for (pa, pb) in zip(pts, pts[1:]):
            pa, pb = np.array(pa, float), np.array(pb, float)
            nn = int(np.linalg.norm(pb - pa) / 1.4)
            for kk in range(1, nn):
                q = pa + (pb - pa) * kk / nn
                if sp["stands"] is None and abs(q[0] - (x0 + x1) / 2) < 0.2:
                    continue
                if sp["stands"] is not None and any(abs(q[0] - xa) < 0.2 and abs(q[2] - za) < 0.2 for xa, za in stands):
                    continue
                inn = np.array([ax_ - q[0], 0, az_ - q[2]])
                inn = inn / np.linalg.norm(inn) * 0.14
                rod(b, q, q + inn - [0, 0.05, 0], 0.025, "pipe_red")
                cyl(b, q + inn - [0, 0.05, 0], q + inn * 1.3 - [0, 0.07, 0], 0.035, "stainless", sides=8)
    # The riser from the deluge valve house, up the back corner to the lower ring.
    y, (x0, x1, z0, z1) = sp["rings"][0]
    path(b, [(x1, -0.5, z1), (x1, y - 0.055, z1)], 0.075, "pipe_red")
