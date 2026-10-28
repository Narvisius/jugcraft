"""The surround speaker for the flat screen TVs (Justin, 27 October 2026: "add in speakers ... two blocks tall, a speaker
on top of a thin stand ... realistic ... automatically syncs with the nearest screen ... a sync key ... so that people
can create a surround sound system"). Mockup first (scratchpad spk/), then built as the TV's own pieces are.

Real-world basis: a wireless surround speaker of the kind sold for the rear channels of a TV system (a two-way
bass-reflex box with a 6.5 in woven-fibre woofer and a 1 in silk dome tweeter in a shallow waveguide, a front slot
port, a status light and a SYNC button; powered, so a thin power cord) on a single-column speaker stand (a square
black anodised aluminium column with its cable in a channel up the back, a steel base plate on four adjustable spikes
with floor cups, a top plate with rubber pads). Dolby puts surrounds 0.6-0.9 m above seated ear height, about 1.8 m:
the cabinet's middle here is 1.77 m, the whole thing 1.97 m (two blocks). The name on it, NARV-TECH SR-6, is the
screen's own make (the TV window says NARV-TECH).

Frame: metres; the lower block's middle at x 0, z 0, the floor at y 0; the speaker faces +z (south). The mesh is
drone_parts_shelving.Mesh (textures, kinds, AO, live faces). Live faces (the client animates them): kind 8 the status
light (id 0 front, 1 back); the woofer's cone and dust cap are their own mesh (CONE) so the renderer can move them
with the sound."""
from __future__ import annotations

import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import drone_parts_shelving as DPS
import parts_shelving_parts as P

FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FC = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"

# ------------------------------------------------------------------ sizes (metres)
FLOOR = 0.0
SPIKE = 0.018                    # the base plate stands this high on its spikes
PLATE_W, PLATE_T = 0.30, 0.022   # the base plate: square, thickness
PLATE_Y = FLOOR + SPIKE          # its underside
COL = 0.048                      # the column's section
COL_TOP = 1.552
TOP_W, TOP_D, TOP_T = 0.16, 0.20, 0.012
CAB_W, CAB_H, CAB_D = 0.220, 0.400, 0.270
CAB_Y = COL_TOP + TOP_T + 0.006  # the cabinet stands on the top plate's pads
ZF = CAB_D / 2                   # the cabinet's front (before the baffle)
BAFFLE = 0.003                   # the baffle's thickness proud of the cabinet's front
WOOFER_Y = CAB_Y + 0.165
TWEETER_Y = CAB_Y + 0.328
PORT_Y = CAB_Y + 0.040
STATUS = 8
CONE_EXCURSION = 0.0018          # client/TvSpeakerRenderer.EXCURSION


# ------------------------------------------------------------------ textures

def _rng(seed):
    return np.random.default_rng(seed)


def walnut(size=512, seed=3):
    """American walnut veneer, quarter-cut: dark brown with straight grain, figure bands and pores; satin lacquer."""
    r = _rng(seed)
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    lines = np.zeros((size, size), np.float32)
    x = xx / size
    warp = 0.012 * np.sin(yy / size * 2 * math.pi * 1.5 + 0.7) + 0.006 * np.sin(yy / size * 2 * math.pi * 4.2)
    for k in range(70):
        c = r.uniform(0, 1)
        w = r.uniform(0.0015, 0.006)
        a = r.uniform(0.25, 1.0)
        d = np.abs(((x + warp - c + 0.5) % 1.0) - 0.5)
        lines += a * np.exp(-(d / w) ** 2)
    figure = 0.5 + 0.5 * np.sin(x * 2 * math.pi * 3.0 + yy / size * 1.3)
    pores = (r.random((size, size)) > 0.985).astype(np.float32) * r.uniform(0.3, 0.8, (size, size))
    pores = np.maximum(pores, np.roll(pores, 1, 0))
    base = np.array([84, 56, 38], np.float32)
    dark = np.array([46, 30, 20], np.float32)
    v = np.clip(lines * 0.55, 0, 1)
    col = base[None, None] * (1 - v[..., None]) + dark[None, None] * v[..., None]
    col *= (0.92 + 0.10 * figure)[..., None]
    col *= (1 - 0.25 * pores)[..., None]
    col += r.normal(0, 2.0, (size, size, 1))
    t = np.ones((size, size, 4), np.float32)
    t[..., :3] = np.clip(col, 0, 255) / 255
    return t


def satin(rgb, size=128, seed=5, amt=1.6):
    return P.noise(size, rgb, amt, seed)


def anodised(size=128, seed=7):
    """Black anodised aluminium extrusion: fine lengthwise brushing (the texture's v is along the column)."""
    r = _rng(seed)
    t = np.ones((size, size, 4), np.float32)
    streak = r.normal(0, 2.2, (1, size)) + r.normal(0, 0.8, (size, size))
    for i, c in enumerate((30, 31, 34)):
        t[..., i] = np.clip((c + streak) / 255, 0, 1)
    return t


def powder(size=128, seed=9):
    """Black textured powder coat (the base plate)."""
    r = _rng(seed)
    t = np.ones((size, size, 4), np.float32)
    n = r.normal(0, 3.2, (size, size)) + (r.random((size, size)) > 0.97) * 8
    for i, c in enumerate((25, 26, 28)):
        t[..., i] = np.clip((c + n) / 255, 0, 1)
    return t


def woven_cone(size=256, seed=11):
    """A woven glass-fibre cone: charcoal plain weave with a silver sheen across the tows (laid out flat: u round the cone,
    v from the rim in)."""
    t = np.ones((size, size, 4), np.float32)
    yy, xx = np.mgrid[0:size, 0:size]
    k = 6
    over = ((xx // k) + (yy // k)) % 2
    along = np.where(over == 0, np.sin((xx % k) / k * math.pi), np.sin((yy % k) / k * math.pi))
    v = 0.12 + 0.07 * along + 0.02 * _rng(seed).random((size, size))
    for i, m in enumerate((1.0, 1.02, 1.08)):
        t[..., i] = np.clip(v * m, 0, 1)
    return t


def silk_dome(size=64, seed=13):
    """A coated silk dome: matte dark grey fabric, a soft sheen."""
    t = P.noise(size, (78, 78, 82), 3.0, seed)
    yy, xx = np.mgrid[0:size, 0:size] / size - 0.5
    sheen = np.exp(-((xx + 0.12) ** 2 + (yy + 0.15) ** 2) / 0.02) * 0.10
    t[..., :3] = np.clip(t[..., :3] + sheen[..., None], 0, 1)
    return t


def tag_img(text="NARV-TECH", w=360, h=72):
    """A small engraved aluminium tag (dark letters on brushed metal, a rivet at each end)."""
    r = _rng(17)
    a = np.ones((h, w, 4), np.float32)
    streak = r.normal(0, 4, (h, 1)) + r.normal(0, 2, (h, w))
    for i, c in enumerate((188, 192, 198)):
        a[..., i] = np.clip((c + streak) / 255, 0, 1)
    img = Image.fromarray((a * 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle((1, 1, w - 2, h - 2), outline=(120, 124, 130, 255), width=2)
    f = ImageFont.truetype(FC, int(h * 0.58))
    d.text((w / 2, h / 2 + 1), text, font=f, fill=(34, 36, 40, 255), anchor="mm")
    for x in (18, w - 18):
        d.ellipse((x - 8, h / 2 - 8, x + 8, h / 2 + 8), fill=(150, 154, 160, 255), outline=(90, 94, 100, 255), width=2)
        d.ellipse((x - 3, h / 2 - 5, x + 3, h / 2 - 1), fill=(228, 230, 234, 255))
    return np.asarray(img, np.float32) / 255


def rating_label(w=420, h=300):
    """The rating label on the back (what a real one prints: model, ratings, marks, the serial's barcode)."""
    img = Image.new("RGBA", (w, h), (236, 234, 226, 255))
    d = ImageDraw.Draw(img)
    fb, fr = ImageFont.truetype(FB, 30), ImageFont.truetype(FR, 18)
    d.text((18, 14), "NARV-TECH", font=fb, fill=(20, 20, 22, 255))
    d.text((18, 52), "SR-6 WIRELESS SURROUND SPEAKER", font=ImageFont.truetype(FB, 19), fill=(20, 20, 22, 255))
    for i, line in enumerate(("INPUT: 100-240 V ~ 50/60 Hz 30 W", "AMPLIFIER: 2 x 50 W CLASS D", "DRIVERS: 165 mm WOOFER + 25 mm DOME",
                              "WIRELESS: 5.8 GHz  PAIR WITH SYNC")):
        d.text((18, 88 + i * 30), line, font=fr, fill=(30, 30, 32, 255))
    P_ = __import__("parts_shelving_art")
    P_.barcode(d, (18, 214, 250, 282), 6, digits=True)
    d.rectangle((296, 214, 400, 282), outline=(30, 30, 32, 255), width=3)
    d.text((348, 236), "CE", font=ImageFont.truetype(FB, 26), fill=(30, 30, 32, 255), anchor="mm")
    d.text((348, 266), "SN 6R0419", font=ImageFont.truetype(FR, 14), fill=(30, 30, 32, 255), anchor="mm")
    return np.asarray(img, np.float32) / 255


# the back's parts (world x, y): the SYNC button and its light, the power inlet; the rating label; the keyhole slot
BACK_PLATE = (0.0, CAB_Y + 0.105, 0.120, 0.090)        # the terminal area: centre x, y, width, height
SYNC_AT = (0.025, CAB_Y + 0.113)
SYNC_LED = (0.045, CAB_Y + 0.113)
INLET_AT = (-0.028, CAB_Y + 0.113)
LABEL = (0.055, CAB_Y + 0.330, 0.110, 0.078)            # its top-left (as seen from behind: +x), width, height
KEYHOLE = (0.0, CAB_Y + 0.366)


def back_face(ppm=10000):
    """The cabinet's back, printed as it is made: satin black, the terminal area's moulded panel with its legends
    (white ink), the rating label stuck on, the keyhole slot for hanging it. Laid over the back from its top-left as
    seen from behind (+x, the top) at ppm pixels a metre."""
    r = 0.010
    w_m, h_m = CAB_W - 2 * r, CAB_H
    W_, H_ = int(w_m * ppm / 4), int(h_m * ppm / 4)
    k = W_ / w_m

    def px(x, y):
        return (w_m / 2 - x) * k, (CAB_Y + h_m - y) * k
    base = (satin((22, 22, 24), 1, 8)[0, 0, :3] * 255).astype(int)
    noise = np.random.default_rng(31).normal(0, 1.4, (H_, W_, 1))
    a = np.ones((H_, W_, 4), np.float32)
    a[..., :3] = np.clip((np.array([22, 22, 24]) + noise) / 255, 0, 1)
    img = Image.fromarray((a * 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    cx, cy, w, h = BACK_PLATE
    x0, y0 = px(cx + w / 2, cy + h / 2)
    x1, y1 = px(cx - w / 2, cy - h / 2)
    d.rounded_rectangle((x0, y0, x1, y1), radius=8, fill=(28, 28, 31, 255), outline=(12, 12, 13, 255), width=3)
    f = ImageFont.truetype(FB, int(0.0065 * k))
    bx, by = px(*SYNC_AT)
    d.text((bx, by + 0.016 * k), "SYNC", font=f, fill=(224, 224, 220, 255), anchor="mm")
    lx, ly = px(*SYNC_LED)
    d.text((lx, ly + 0.012 * k), "LINK", font=ImageFont.truetype(FB, int(0.0045 * k)), fill=(224, 224, 220, 255), anchor="mm")
    ix, iy = px(*INLET_AT)
    d.text((ix, iy - 0.016 * k), "100-240V~ 30W", font=ImageFont.truetype(FB, int(0.0045 * k)), fill=(224, 224, 220, 255), anchor="mm")
    d.text((px(cx, cy + h / 2 - 0.010)[0], px(cx, cy + h / 2 - 0.010)[1]), "NARV-TECH  SR-6", font=ImageFont.truetype(FB, int(0.0055 * k)),
           fill=(150, 152, 156, 255), anchor="mm")
    lab = Image.fromarray((rating_label() * 255).astype(np.uint8), "RGBA")
    tlx, tly = px(LABEL[0], LABEL[1])
    lab = lab.resize((int(LABEL[2] * k), int(LABEL[3] * k)), Image.LANCZOS)
    img.paste(lab, (int(tlx), int(tly)))
    kx, ky = px(*KEYHOLE)
    d.ellipse((kx - 0.006 * k, ky - 0.006 * k, kx + 0.006 * k, ky + 0.006 * k), fill=(5, 5, 6, 255))
    d.rounded_rectangle((kx - 0.0028 * k, ky, kx + 0.0028 * k, ky + 0.016 * k), radius=4, fill=(5, 5, 6, 255))
    return np.asarray(img, np.float32) / 255


def floor_cup(size=64):
    t = P.noise(size, (190, 194, 200), 8, 21, True)
    return t


M = {
    "walnut": walnut(), "black": satin((20, 20, 22)), "baffle": satin((17, 17, 19), seed=6, amt=1.0), "back": satin((22, 22, 24), seed=8),
    "abs": satin((24, 24, 26), seed=10), "rubber": P.noise(32, (30, 30, 32), 2, 12), "cone": woven_cone(), "cap": satin((34, 34, 38), 32, 14, 0.8),
    "dome": silk_dome(), "port": P.solid((6, 6, 7)), "anod": anodised(), "anod_dk": anodised(seed=8), "powder": powder(), "chrome": P.M["chrome"],
    "steel": P.M["steel"], "cable": satin((16, 16, 18), 16, 15, 0.6), "tag": tag_img(), "backface": back_face(),
    "pad": P.noise(16, (40, 40, 42), 2, 16), "hole": P.solid((4, 4, 5)), "basket": satin((44, 46, 50), 32, 18, 2.0),
    "copper": P.M["copper"], "white": P.noise(16, (226, 226, 222), 2, 19), "led": P.solid((236, 244, 255)),
    "stainless": P.noise(32, (132, 136, 142), 6, 23, True),
}


# ------------------------------------------------------------------ helpers

def lathe_axis(m, c, axis, prof, tex, n=48, lit=False, ao=True):
    """Turns a profile [(r, h)] about the axis through c (h along it). The surface faces left of the profile's direction:
    a stretch going up the axis (+h) faces outward, one going in toward the axis faces +h (a solid's outline is traced
    bottom outward, up its side, then in across its top)."""
    c = np.asarray(c, float)
    w = np.asarray(axis, float) / np.linalg.norm(axis)
    uu = np.cross(w, (0, 1, 0) if abs(w[1]) < 0.9 else (1, 0, 0))
    uu /= np.linalg.norm(uu)
    vv = np.cross(w, uu)
    L = [0.0]
    for j in range(1, len(prof)):
        L.append(L[-1] + math.hypot(prof[j][0] - prof[j - 1][0], prof[j][1] - prof[j - 1][1]))
    tot = L[-1] or 1.0
    for i in range(n):
        a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        e0 = uu * math.cos(a0) + vv * math.sin(a0)
        e1 = uu * math.cos(a1) + vv * math.sin(a1)
        em = uu * math.cos((a0 + a1) / 2) + vv * math.sin((a0 + a1) / 2)
        for j in range(len(prof) - 1):
            (r0, h0), (r1, h1) = prof[j], prof[j + 1]
            if r0 < 1e-7 and r1 < 1e-7:
                continue
            pts = [c + e0 * r0 + w * h0, c + e1 * r0 + w * h0, c + e1 * r1 + w * h1, c + e0 * r1 + w * h1]
            dr, dh = r1 - r0, h1 - h0
            want = em * dh - w * dr    # left of the profile: going up (+h) faces out, going in faces +h
            uvs = [(16 * i / n * 4, 16 * L[j] / tot), (16 * (i + 1) / n * 4, 16 * L[j] / tot), (16 * (i + 1) / n * 4, 16 * L[j + 1] / tot),
                   (16 * i / n * 4, 16 * L[j + 1] / tot)]
            m.oquad(pts, want, tex, uvs=uvs, lit=lit, ao=ao)


def disc(m, c, axis, r, tex, n=32, lit=False):
    lathe_axis(m, c, axis, [(r, 0.0), (0.0, 0.0)], tex, n=n, lit=lit)


# ------------------------------------------------------------------ the stand

def stand(m):
    T = M
    py0, py1 = PLATE_Y, PLATE_Y + PLATE_T
    # base plate, its edges eased
    P.rbox(m, (0, py0, 0), PLATE_W, PLATE_T, PLATE_W, 0.03, {"s": T["powder"], "n": T["powder"], "e": T["powder"], "w": T["powder"],
                                                           "u": T["powder"], "d": T["powder"], "edge": T["powder"]}, seg=4)
    # spikes: threaded stud through the plate, lock nut over it, cone point down into a floor cup
    for sx in (-1, 1):
        for sz in (-1, 1):
            x, z = sx * 0.112, sz * 0.112
            lathe_axis(m, (x, FLOOR, z), (0, 1, 0), [(0.0, 0.0), (0.020, 0.0), (0.020, 0.0035)], T["rubber"], n=20)   # the cup's rubber foot
            lathe_axis(m, (x, FLOOR, z), (0, 1, 0), [(0.020, 0.0035), (0.019, 0.0075), (0.016, 0.0075), (0.006, 0.0055), (0.0, 0.0055)],
                       T["stainless"], n=20)
            lathe_axis(m, (x, FLOOR, z), (0, 1, 0), [(0.0, 0.0050), (0.0085, 0.0140), (0.0085, 0.0175), (0.0, 0.0175)], T["chrome"], n=16)
            P.cyl(m, (x, py0 - 0.001, z), (x, py1 + 0.010, z), 0.0042, T["steel"], n=10, caps=True)
            hexnut(m, (x, py1, z), 0.009, 0.006, T["chrome"])
    # the column's foot flange with its four bolts
    P.rbox(m, (0, py1, 0), 0.090, 0.012, 0.090, 0.008, T["anod_dk"], seg=2)
    for sx in (-1, 1):
        for sz in (-1, 1):
            lathe_axis(m, (sx * 0.032, py1 + 0.012, sz * 0.032), (0, 1, 0), [(0.0062, 0.0), (0.0062, 0.0060), (0.0, 0.0060)], T["chrome"], n=12)
    # the column: square, its corners eased; the cable channel's cover strip on its back
    P.rbox(m, (0, py1 + 0.012, 0), COL, COL_TOP - (py1 + 0.012), COL, 0.006, T["anod"], seg=2)
    zb = -COL / 2
    P.box(m, (-0.014, py1 + 0.03, zb - 0.006), (0.014, COL_TOP - 0.03, zb), T["anod_dk"], faces="nsewud")
    # the column's head: a cast collar under the top plate
    P.rbox(m, (0, COL_TOP - 0.022, 0), 0.062, 0.022, 0.062, 0.006, T["anod_dk"], seg=2)
    # top plate and its rubber pads
    P.rbox(m, (0, COL_TOP, 0), TOP_W, TOP_T, TOP_D, 0.012, T["powder"], seg=3)
    for sx in (-1, 1):
        for sz in (-1, 1):
            lathe_axis(m, (sx * 0.060, COL_TOP + TOP_T, sz * 0.078), (0, 1, 0), [(0.011, 0.0), (0.011, 0.006), (0.0, 0.006)], T["pad"], n=14)
    # the cable grommet at the top plate's back (the cord goes down inside the column) and its exit at the foot
    hexnut(m, (0, COL_TOP + TOP_T, -0.072), 0.010, 0.006, T["chrome"])        # cable gland: its nut and dome
    lathe_axis(m, (0, COL_TOP + TOP_T + 0.006, -0.072), (0, 1, 0), [(0.0085, 0.0), (0.0075, 0.003), (0.0045, 0.004), (0.0045, -0.002)], T["rubber"], n=14)
    lathe_axis(m, (0, py1 + 0.045, zb - 0.006), (0, 0, -1), [(0.008, 0.0), (0.008, 0.002), (0.0045, 0.002), (0.0045, -0.004)], T["rubber"], n=14)


def hexnut(m, c, r, h, tex):
    c = np.asarray(c, float)
    ring = [c + np.array([r * math.cos(math.pi / 3 * k + math.pi / 6), 0, r * math.sin(math.pi / 3 * k + math.pi / 6)]) for k in range(6)]
    up = np.array([0, h, 0])
    for k in range(6):
        a, b = ring[k], ring[(k + 1) % 6]
        m.oquad([a + up, b + up, b, a], (a + b) / 2 - c, tex)
        m.oquad([c + up, a + up, b + up, b + up], (0, 1, 0), tex)


# ------------------------------------------------------------------ the speaker

def cabinet(m):
    T = M
    y0, y1 = CAB_Y, CAB_Y + CAB_H
    # the box: walnut sides and top, black back; its upright edges eased
    P.rbox(m, (0, y0, 0), CAB_W, CAB_H, CAB_D, 0.010, {"s": None, "n": T["backface"], "e": T["walnut"], "w": T["walnut"], "u": T["walnut"],
                                                       "d": T["black"], "edge": T["walnut"]}, seg=3, faces="newud")
    # the baffle (black, proud of the walnut): its edges, and its face with the drivers', port's, tag's and light's cut-outs
    zf = ZF + BAFFLE
    P.box(m, (-CAB_W / 2 + 0.002, y0 + 0.002, ZF - 0.001), (CAB_W / 2 - 0.002, y1 - 0.002, zf), T["baffle"], faces="ewud")
    baffle_face(m, zf)
    # a black panel inside, behind the drivers (the cone at its furthest back too): a hairline between the baffle's
    # pieces shows black, never what's behind the speaker
    P.panel(m, (-CAB_W / 2 + 0.006, y1 - 0.006, zf - 0.030), (CAB_W - 0.012, 0, 0), (0, -(CAB_H - 0.012), 0), T["port"], ao=False)
    return zf


# the cut-outs (the parts over them hide their edges)
WOOFER_HOLE, TWEETER_HOLE = 0.080, 0.0458
PORT_W, PORT_H, PORT_R = 0.150, 0.020, 0.010
TAG_W, TAG_H = 0.052, 0.0105
LED_X, LED_S = 0.097, 0.0028


def tag_y():
    return (WOOFER_Y + 0.086 + TWEETER_Y - 0.046) / 2


def baffle_face(m, zf):
    """The baffle's face in bands up it: plain, the port's (the light's window beside it), plain, the woofer's (a
    circle cut from it), the tag's, the tweeter's, plain. Its texture is laid flat over the whole face."""
    T = M
    X0, X1 = -CAB_W / 2 + 0.002, CAB_W / 2 - 0.002
    Y0, Y1 = CAB_Y + 0.002, CAB_Y + CAB_H - 0.002

    def uv(x, y):
        return ((x - X0) / (X1 - X0) * 16, (Y1 - y) / (Y1 - Y0) * 16)

    def rect(x0, x1, y0, y1):
        if x1 - x0 < 1e-6 or y1 - y0 < 1e-6:
            return
        pts = [(x0, y1, zf), (x1, y1, zf), (x1, y0, zf), (x0, y0, zf)]
        m.oquad(pts, (0, 0, 1), T["baffle"], uvs=[uv(p[0], p[1]) for p in pts])

    def ring(x0, x1, y0, y1, cx, cy, r, n=64):
        angs = sorted(set([2 * math.pi * i / n for i in range(n)] + [math.atan2(y - cy, x - cx) % (2 * math.pi)
                                                                     for x in (x0, x1) for y in (y0, y1)]))
        def edge(a):
            dx, dy = math.cos(a), math.sin(a)
            t = min((x1 - cx) / dx if dx > 1e-9 else (x0 - cx) / dx if dx < -1e-9 else 1e9,
                    (y1 - cy) / dy if dy > 1e-9 else (y0 - cy) / dy if dy < -1e-9 else 1e9)
            return cx + dx * t, cy + dy * t
        for i in range(len(angs)):
            a, b = angs[i], angs[(i + 1) % len(angs)] + (2 * math.pi if i == len(angs) - 1 else 0)
            pa, pb = edge(a), edge(b)
            ca, cb = (cx + r * math.cos(a), cy + r * math.sin(a)), (cx + r * math.cos(b), cy + r * math.sin(b))
            pts = [(pb[0], pb[1], zf), (pa[0], pa[1], zf), (ca[0], ca[1], zf), (cb[0], cb[1], zf)]
            m.oquad(pts, (0, 0, 1), T["baffle"], uvs=[uv(p[0], p[1]) for p in pts])

    px0, px1 = -PORT_W / 2 - PORT_R, PORT_W / 2 + PORT_R
    p_lo, p_hi = PORT_Y - PORT_H / 2 - PORT_R, PORT_Y + PORT_H / 2 + PORT_R
    w_lo, w_hi = WOOFER_Y - 0.094, WOOFER_Y + 0.094
    t_lo, t_hi = TWEETER_Y - 0.050, TWEETER_Y + 0.050
    ty = tag_y()
    g_lo, g_hi = ty - TAG_H / 2, ty + TAG_H / 2
    rect(X0, X1, Y0, p_lo)
    rect(X0, px0, p_lo, p_hi)
    # beside the port: the light's square window
    rect(px1, X1, p_lo, PORT_Y - LED_S)
    rect(px1, X1, PORT_Y + LED_S, p_hi)
    rect(px1, LED_X - LED_S, PORT_Y - LED_S, PORT_Y + LED_S)
    rect(LED_X + LED_S, X1, PORT_Y - LED_S, PORT_Y + LED_S)
    rect(X0, X1, p_hi, w_lo)
    ring(X0, X1, w_lo, w_hi, 0.0, WOOFER_Y, WOOFER_HOLE)
    rect(X0, X1, w_hi, g_lo)
    rect(X0, -TAG_W / 2, g_lo, g_hi)
    rect(TAG_W / 2, X1, g_lo, g_hi)
    rect(X0, X1, g_hi, t_lo)
    ring(X0, X1, t_lo, t_hi, 0.0, TWEETER_Y, TWEETER_HOLE, n=48)
    rect(X0, X1, t_hi, Y1)


def woofer(m, zf):
    """The 6.5 in woofer: trim ring, the roll surround, the woven cone, the dust cap (the last three their own mesh: they
    move, at most CONE_EXCURSION either way)."""
    T = M
    c = np.array([0.0, WOOFER_Y, zf])
    ax = (0, 0, 1)
    lathe_axis(m, c, ax, [(0.086, 0.0), (0.086, 0.0055), (0.0835, 0.0070), (0.0750, 0.0062), (0.072, 0.001)], T["abs"], n=64)
    # the recess behind the trim (the basket's edge) to the surround
    # (on back past where the surround meets it: the cone and surround move, and never leave a gap to see through)
    lathe_axis(m, c, ax, [(0.072, 0.001), (0.0715, -0.004), (0.0715, -0.0085)], T["basket"], n=64)
    # the surround (a half roll) and the cone with its dust cap: their own mesh, moved with the sound (CONE_EXCURSION)
    cone = DPS.Mesh()
    roll = [(0.0715 - 0.0075 * k / 8, -0.004 + 0.0055 * math.sin(math.pi * k / 8)) for k in range(9)]
    lathe_axis(cone, c, ax, roll, T["rubber"], n=64)
    prof = [(0.064, -0.004), (0.050, -0.0085), (0.036, -0.0135), (0.0305, -0.0155)]
    lathe_axis(cone, c, ax, prof, T["cone"], n=64)
    dome = [(0.0305, -0.0155)] + [(0.0305 * math.cos(math.pi / 2 * k / 6), -0.0155 + 0.0085 * math.sin(math.pi / 2 * k / 6)) for k in range(1, 7)]
    lathe_axis(cone, c, ax, dome, T["cap"], n=64)   # (as many sides as the cone: no crack where they meet)
    return cone


def tweeter(m, zf):
    T = M
    c = np.array([0.0, TWEETER_Y, zf])
    ax = (0, 0, 1)
    # the waveguide's face plate and its shallow horn, the dome's surround, the silk dome
    lathe_axis(m, c, ax, [(0.046, 0.0), (0.046, 0.0058), (0.044, 0.0066)], T["abs"], n=48)
    horn = [(0.044 - 0.026 * k / 8, 0.0066 - 0.0096 * (k / 8) ** 0.6) for k in range(9)]
    lathe_axis(m, c, ax, horn, T["abs"], n=48)
    lathe_axis(m, c, ax, [(0.018, -0.003), (0.0145, -0.0028)], T["rubber"], n=48)   # (the horn's sides: no crack)
    dome = [(0.0145, -0.0028)] + [(0.0145 * math.cos(math.pi / 2 * k / 7), -0.0028 + 0.0078 * math.sin(math.pi / 2 * k / 7)) for k in range(1, 8)]
    lathe_axis(m, c, ax, dome, T["dome"], n=48)


def port(m, zf):
    """The front slot port: a flared slot 150 x 20 mm, dark inside."""
    T = M
    w, h, r = PORT_W, PORT_H, PORT_R
    y = PORT_Y
    x0, x1 = -w / 2, w / 2
    # flare: the baffle face to the slot's mouth in four steps (a rounded lip)
    for k in range(4):
        a0, a1 = math.pi / 2 * k / 4, math.pi / 2 * (k + 1) / 4
        o0, o1 = r * (1 - math.sin(a0)), r * (1 - math.sin(a1))
        d0, d1 = -r * (1 - math.cos(a0)), -r * (1 - math.cos(a1))
        # top and bottom lips
        for sy in (1, -1):
            ya0, ya1 = y + sy * (h / 2 + o0), y + sy * (h / 2 + o1)
            pts = [(x0 - o0, ya0, zf + d0), (x1 + o0, ya0, zf + d0), (x1 + o1, ya1, zf + d1), (x0 - o1, ya1, zf + d1)]
            m.oquad(pts, (0, -sy * 0.7, 0.7), T["abs"])
        for sx in (1, -1):
            xa0, xa1 = (x1 + o0, x1 + o1) if sx > 0 else (x0 - o0, x0 - o1)
            pts = [(xa0, y + h / 2 + o0, zf + d0), (xa1, y + h / 2 + o1, zf + d1), (xa1, y - h / 2 - o1, zf + d1), (xa0, y - h / 2 - o0, zf + d0)]
            m.oquad(pts, (-sx * 0.7, 0, 0.7), T["abs"])
    # the slot's walls going in, and its dark end
    zi = zf - r - 0.05
    for sy in (1, -1):
        yy = y + sy * h / 2
        m.oquad([(x0, yy, zf - r), (x1, yy, zf - r), (x1, yy, zi), (x0, yy, zi)], (0, -sy, 0), T["abs"])
    for sx in (1, -1):
        xx = x1 if sx > 0 else x0
        m.oquad([(xx, y + h / 2, zf - r), (xx, y - h / 2, zf - r), (xx, y - h / 2, zi), (xx, y + h / 2, zi)], (-sx, 0, 0), T["abs"])
    P.panel(m, (x0, y + h / 2, zi), (w, 0, 0), (0, -h, 0), T["port"], ao=False)


def front_fittings(m, zf):
    T = M
    # the name tag, set into its cut-out between the woofer and the tweeter's waveguide (its face a little proud)
    ty = tag_y()
    P.box(m, (-TAG_W / 2, ty - TAG_H / 2, zf - 0.003), (TAG_W / 2, ty + TAG_H / 2, zf + 0.0008),
          {"s": T["tag"], "e": T["steel"], "w": T["steel"], "u": T["steel"], "d": T["steel"], "n": None}, faces="sewud")
    # the status light behind its square window (live: the client colours it), the window's walls
    s = LED_S
    zl = zf - 0.002
    for (a, b, nrm) in (((LED_X - s, PORT_Y + s), (LED_X + s, PORT_Y + s), (0, -1, 0)), ((LED_X + s, PORT_Y - s), (LED_X - s, PORT_Y - s), (0, 1, 0)),
                        ((LED_X - s, PORT_Y - s), (LED_X - s, PORT_Y + s), (1, 0, 0)), ((LED_X + s, PORT_Y + s), (LED_X + s, PORT_Y - s), (-1, 0, 0))):
        m.oquad([(a[0], a[1], zf), (b[0], b[1], zf), (b[0], b[1], zl), (a[0], a[1], zl)], nrm, T["abs"])
    m.live_quad(STATUS, 0, [(LED_X - s, PORT_Y + s, zl), (LED_X + s, PORT_Y + s, zl), (LED_X + s, PORT_Y - s, zl), (LED_X - s, PORT_Y - s, zl)])


def back_fittings(m):
    """The back (its print is the back face's texture, back_face): the SYNC button in a chrome ring and its LINK light
    (live), the power inlet with the cord's plug in it; the cord down under the cabinet into the stand's gland."""
    T = M
    zb = -CAB_D / 2
    # SYNC button: a chrome ring standing 6 mm off the back and a rubber cap in it
    bx, by = SYNC_AT
    lathe_axis(m, (bx, by, zb), (0, 0, -1), [(0.0105, 0.0), (0.0105, 0.0060), (0.0085, 0.0066)], T["chrome"], n=20)
    lathe_axis(m, (bx, by, zb - 0.0040), (0, 0, -1), [(0.0080, 0.0), (0.0080, 0.0040), (0.0060, 0.0052), (0.0, 0.0054)], T["rubber"], n=20)
    # the LINK light: a lens in a short bezel (live: the client colours it)
    lx, ly = SYNC_LED
    lathe_axis(m, (lx, ly, zb), (0, 0, -1), [(0.0040, 0.0), (0.0040, 0.0060), (0.0026, 0.0062), (0.0026, 0.0055)], T["abs"], n=12)
    s = 0.0019
    m.live_quad(STATUS, 1, [(lx + s, ly + s, zb - 0.0056), (lx - s, ly + s, zb - 0.0056), (lx - s, ly - s, zb - 0.0056), (lx + s, ly - s, zb - 0.0056)])
    # IEC C7 inlet (a figure eight in a moulded surround)
    ix, iy = INLET_AT
    P.box(m, (ix - 0.014, iy - 0.009, zb - 0.006), (ix + 0.014, iy + 0.009, zb), T["abs"], faces="nsewud")
    # the plug in it and the cord: down the back, under the cabinet into the stand's gland
    P.box(m, (ix - 0.011, iy - 0.007, zb - 0.022), (ix + 0.011, iy + 0.007, zb - 0.006), T["cable"])
    P.cyl(m, (ix, iy, zb - 0.022), (ix, iy, zb - 0.032), 0.0045, T["cable"], n=10)
    gland = COL_TOP + TOP_T + 0.006 + 0.004
    path = [(ix, iy, zb - 0.032), (ix, iy - 0.012, zb - 0.036), (ix * 0.6, CAB_Y + 0.020, zb - 0.026), (ix * 0.25, CAB_Y - 0.004, -0.090),
            (0.0, gland + 0.004, -0.074), (0.0, gland - 0.006, -0.072)]
    P.tube(m, path, 0.0032, T["cable"], n=8)


def foot_cord(m):
    """The cord out of the column's foot, down off the base plate's back edge and into a floor outlet behind it."""
    T = M
    zb = -COL / 2 - 0.0025
    y = PLATE_Y + PLATE_T + 0.045
    path = [(0.0, y, zb - 0.002), (0.0, y - 0.020, zb - 0.020), (0.0, PLATE_Y + PLATE_T + 0.004, -0.070), (0.0, PLATE_Y + PLATE_T + 0.004, -0.140),
            (0.0, PLATE_Y + PLATE_T - 0.004, -0.158), (0.0, FLOOR + 0.0036, -0.172), (0.0, FLOOR + 0.0034, -0.208)]
    P.tube(m, path, 0.0032, T["cable"], n=8)
    # the floor outlet's lid it plugs into (a low round box: the cord goes in at its side)
    lathe_axis(m, (0.0, FLOOR, -0.232), (0, 1, 0), [(0.026, 0.0), (0.026, 0.0045), (0.023, 0.0070), (0.0, 0.0072)], T["stainless"], n=28)


def speaker():
    """The whole speaker on its stand (the cone apart: it moves)."""
    m = DPS.Mesh()
    stand(m)
    zf = cabinet(m)
    cone = woofer(m, zf)
    tweeter(m, zf)
    port(m, zf)
    front_fittings(m, zf)
    back_fittings(m)
    foot_cord(m)
    return m, cone


# ------------------------------------------------------------------ the Sync Key (item)

def sync_key_icon(size=32):
    """The Sync Key's item picture: a graphite key fob with a round rubber SYNC button (two arrows round), a cyan light
    ring, a stainless split ring through its eye."""
    img = Image.new("RGBA", (size * 8, size * 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    S = size * 8 / 32
    body = (9 * S, 6 * S, 23 * S, 29 * S)
    d.rounded_rectangle(body, radius=6 * S, fill=(52, 55, 60, 255), outline=(22, 23, 26, 255), width=int(1.2 * S))
    d.rounded_rectangle((10 * S, 7 * S, 22 * S, 12 * S), radius=4 * S, fill=(66, 70, 76, 255))
    d.ellipse((14 * S, 2.5 * S, 18 * S, 6.5 * S), outline=(196, 200, 206, 255), width=int(1.2 * S))
    d.ellipse((13.2 * S, 0.8 * S, 18.8 * S, 6.4 * S), outline=(168, 172, 178, 255), width=int(0.9 * S))
    d.ellipse((11 * S, 13 * S, 21 * S, 23 * S), fill=(90, 220, 240, 255))
    d.ellipse((12 * S, 14 * S, 20 * S, 22 * S), fill=(30, 31, 34, 255))
    d.arc((13.5 * S, 15.5 * S, 18.5 * S, 20.5 * S), 200, 340, fill=(230, 232, 228, 255), width=int(1.1 * S))
    d.arc((13.5 * S, 15.5 * S, 18.5 * S, 20.5 * S), 20, 160, fill=(230, 232, 228, 255), width=int(1.1 * S))
    d.polygon([(18.6 * S, 16.6 * S), (19.4 * S, 18.4 * S), (17.4 * S, 18.0 * S)], fill=(230, 232, 228, 255))
    d.polygon([(13.4 * S, 19.4 * S), (12.6 * S, 17.6 * S), (14.6 * S, 18.0 * S)], fill=(230, 232, 228, 255))
    d.rectangle((13 * S, 25 * S, 19 * S, 26.2 * S), fill=(120, 126, 134, 255))
    return img


SYNC_KEY_PIXELS = [
    "......RRR.......",
    ".....R...R......",
    ".....R...R......",
    "....KKRRRKK.....",
    "...KhhhhhhhK....",
    "...KhhhhhhhK....",
    "...KbbCCCbbK....",
    "...KbCdWWCbK....",
    "...KCWddddCK....",
    "...KCddddWCK....",
    "...KbCWWdCbK....",
    "...KbbCCCbbK....",
    "...KbbbbbbbK....",
    "...KbbsssbbK....",
    "...KbbbbbbbK....",
    "....KKKKKKK.....",
]
SYNC_KEY_COLOURS = {"K": (20, 21, 24), "h": (78, 82, 88), "b": (56, 59, 64), "C": (90, 220, 240), "d": (30, 31, 34), "W": (232, 234, 230),
                    "s": (128, 134, 142), "R": (196, 200, 206)}


def sync_key_icon16():
    """The Sync Key's 16 x 16 item texture, pixel by pixel: the fob's outline and graphite body, its top lit, the cyan
    light ring round the SYNC button with its two arrows, the slot at the bottom, the split ring through its eye."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SYNC_KEY_PIXELS):
        for x, ch in enumerate(row):
            if ch in SYNC_KEY_COLOURS:
                img.putpixel((x, y), SYNC_KEY_COLOURS[ch] + (255,))
    return img


SPEAKER_PIXELS = [
    "....wwwwwwww....",
    "....wbbddbbw....",
    "....wbdTTdbw....",
    "....wbbddbbw....",
    "....wbbbbbbw....",
    "....wbccccbw....",
    "....wcckkccw....",
    "....wcckkccw....",
    "....wbccccbw....",
    "....wbppppLw....",
    "....WWWWWWWW....",
    ".......ss.......",
    ".......ss.......",
    ".......ss.......",
    ".....gggggg.....",
    ".....h....h.....",
]
SPEAKER_COLOURS = {"w": (124, 76, 42), "W": (82, 50, 28), "b": (24, 24, 26), "d": (48, 48, 52), "T": (156, 156, 162), "c": (66, 66, 70),
                   "k": (38, 38, 42), "p": (6, 6, 7), "L": (226, 238, 255), "s": (44, 46, 50), "g": (32, 32, 34), "h": (172, 176, 182)}


def speaker_icon16():
    """The speaker's 16 x 16 item texture: the walnut cabinet, its black baffle with the tweeter, the woofer, the port and
    the status light; the column and the base plate on its spikes."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SPEAKER_PIXELS):
        for x, ch in enumerate(row):
            if ch in SPEAKER_COLOURS:
                img.putpixel((x, y), SPEAKER_COLOURS[ch] + (255,))
    return img


# ------------------------------------------------------------------ the files

from tv_speaker_data import ID, KEY_ID, LANG   # (the names: the material data generator writes them too)


def recipe(pattern, key, result, count=1):
    return {"fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"}],
            "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern, "key": key,
            "result": {"id": f"jugcraft:{result}", "count": count}}


def write_data():
    import json
    import tower_control_console as TCC
    j = TCC._json
    A, D = DPS.ASSETS, DPS.DATA
    j(A / "blockstates" / f"{ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(A / "models/block" / f"{ID}.json", {"textures": {"particle": "minecraft:block/black_concrete"}, "elements": []})
    for i in (ID, KEY_ID):
        j(A / "models/item" / f"{i}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{i}"}})
        j(A / "items" / f"{i}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{i}"}})
    (A / "textures/item").mkdir(parents=True, exist_ok=True)
    speaker_icon16().save(A / f"textures/item/{ID}.png")
    sync_key_icon16().save(A / f"textures/item/{KEY_ID}.png")
    import tv_speaker_data
    j(D / "jugcraft/loot_table/blocks" / f"{ID}.json", tv_speaker_data.loot())
    j(D / "jugcraft/recipe" / f"{ID}.json", recipe(["PNP", "PCP", " I "], {
        "P": "#minecraft:planks", "N": "minecraft:note_block", "C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot"}, ID, 2))
    j(D / "jugcraft/recipe" / f"{KEY_ID}.json", {"fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"}],
                                                 "type": "minecraft:crafting_shapeless", "category": "equipment",
                                                 "ingredients": ["minecraft:iron_nugget", "minecraft:redstone", "minecraft:copper_ingot"],
                                                 "result": {"id": f"jugcraft:{KEY_ID}", "count": 1}})
    lang_path = A / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang.update(LANG)
    if sorted_before:
        lang = dict(sorted(lang.items()))
    # (the file's own accents kept as they are, not escaped)
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    p = D / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    if f"jugcraft:{ID}" not in t["values"]:
        t["values"].append(f"jugcraft:{ID}")
    p.write_text(json.dumps(t, indent=2) + "\n")


def write_all():
    """The speaker's meshes (tower_mesh/tv_speaker.bin.gz, and tv_speaker_cone.bin.gz: the surround, cone and dust cap
    the renderer moves), their atlas (textures/entity/tv_speaker.png), the block's and items' files."""
    import tower_control_console as TCC
    m, cone = speaker()
    both = DPS.Mesh()
    both.quads = m.quads + cone.quads
    TCC.bake(both, floor_y=0.0, res=1 / 250, reach=0.06, rays=12)   # (the cone's shade in place, with the rest round it)
    textures = {DPS._key(DPS.WHITE): DPS.WHITE}
    for q in both.quads:
        textures.setdefault(DPS._key(q["tex"]), q["tex"])
    atlas = DPS.Atlas(textures, width=2048)
    tex = DPS.ASSETS / f"textures/entity/{ID}.png"
    tex.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(tex, optimize=True)
    counts = (DPS.write_mesh(m, atlas, DPS.ASSETS / f"tower_mesh/{ID}.bin.gz"),
              DPS.write_mesh(cone, atlas, DPS.ASSETS / f"tower_mesh/{ID}_cone.bin.gz"))
    write_data()
    return m, cone, atlas, counts


if __name__ == "__main__":
    m, cone, atlas, counts = write_all()
    print("speaker", counts[0], "quads written,", len(m.live), "live faces; cone", counts[1], "; atlas", atlas.size, len(atlas.box), "textures")
