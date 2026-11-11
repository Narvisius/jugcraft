"""Materials, pictures and shape helpers for the Tri-Pillar's roof plant (tools/roof_plant.py).

Model pixels: 1 px = 6.25 cm. Plain materials are 64 x 64 (4 texels a pixel), read by position; the pictures (unit
tags, nameplates, warning labels, a gauge's dial, the fans' wire guards) are packed into one atlas (rp_atlas, 16
texels a pixel) and each stretched over its own face. Round things are octagonal prisms (a cross of two slabs and four
corner squares turned 45 degrees) or stepped slabs; model_writer keeps every two faces apart (no z-fighting).
"""
from __future__ import annotations

import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
TS = 64                       # a plain material's size (4 texels a model pixel)


# ------------------------------------------------------------------ materials
def _rng(seed):
    return np.random.default_rng(seed)


def _img(a, mode="RGB"):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), mode)


def painted(rgb, seed, sigma=2.2, streak=1.2):
    r = _rng(seed)
    a = np.zeros((TS, TS, 3)) + np.array(rgb, float)
    a += r.normal(0, sigma, (TS, TS, 1))
    a += np.repeat(r.normal(0, streak, (1, TS, 1)), TS, axis=0)      # faint vertical streaks (rain)
    return _img(a)


def galv(seed):
    """Hot-dip galvanized: spangle patches."""
    r = _rng(seed)
    base = np.zeros((TS, TS)) + 176
    for _ in range(40):
        cx, cy, rad = r.integers(0, TS, 2).tolist() + [r.integers(4, 12)]
        yy, xx = np.ogrid[:TS, :TS]
        d = np.minimum(np.abs(xx - cx), TS - np.abs(xx - cx)) ** 2 + np.minimum(np.abs(yy - cy), TS - np.abs(yy - cy)) ** 2
        base[d < rad * rad] += r.normal(0, 9)
    base += r.normal(0, 2.5, (TS, TS))
    a = np.stack([base - 4, base, base + 4], -1)
    return _img(a)


def coil():
    """Finned coil seen face on: fine vertical fins, copper tube ends in staggered rows."""
    a = np.zeros((TS, TS, 3)) + np.array([150, 156, 160], float)
    for x in range(TS):
        if x % 2 == 0:
            a[:, x] *= 0.72
    for row in range(4):
        for col in range(4):
            cx = col * 16 + (8 if row % 2 else 0) + 4
            cy = row * 16 + 8
            yy, xx = np.ogrid[:TS, :TS]
            m = ((xx - cx) % TS - 0) ** 2 + (yy - cy) ** 2 < 6
            a[m] = [176, 104, 58]
    a += _rng(7).normal(0, 3, (TS, TS, 1))
    return _img(a)


def coil_top():
    """The coil from above or below: fin edges in close lines."""
    a = np.zeros((TS, TS, 3)) + np.array([120, 126, 130], float)
    for y in range(TS):
        if y % 2:
            a[y] *= 0.55
    a += _rng(8).normal(0, 3, (TS, TS, 1))
    return _img(a)


def guard(im):
    """A fan's wire guard (a picture in the atlas, stretched over the guard): rings, eight spokes, clear between."""
    n = im.size[0]
    im.paste((0, 0, 0, 0), (0, 0, n, n))
    d = ImageDraw.Draw(im)
    c = n / 2
    for k in range(3, 17):
        r = c * k / 16.5
        d.ellipse((c - r, c - r, c + r, c + r), outline=(52, 54, 56, 255), width=max(1, n // 85))
    for i in range(8):
        a = math.tau * i / 8 + 0.2
        d.line((c + n * 0.07 * math.cos(a), c + n * 0.07 * math.sin(a), c + (c - 2) * math.cos(a), c + (c - 2) * math.sin(a)),
               fill=(52, 54, 56, 255), width=max(1, n // 64))
    d.ellipse((c - n * 0.08, c - n * 0.08, c + n * 0.08, c + n * 0.08), outline=(52, 54, 56, 255), width=max(1, n // 64))


def bird_screen():
    a = np.zeros((TS, TS, 3)) + 34
    for i in range(TS):
        if i % 4 == 0:
            a[i, :] = 120
            a[:, i] = 120
    return _img(a)


def louvre():
    a = np.zeros((TS, TS, 3)) + np.array([60, 62, 64], float)
    for y in range(TS):
        k = y % 8
        a[y] = np.array([178, 182, 184]) * (0.55 + 0.06 * k) if k < 6 else np.array([28, 28, 30])
    return _img(a)


def rubber():
    a = np.zeros((TS, TS, 3)) + 36
    a += _rng(3).normal(0, 4, (TS, TS, 1))
    return _img(a)


def concrete():
    r = _rng(11)
    a = np.zeros((TS, TS, 3)) + np.array([152, 150, 144], float)
    a += r.normal(0, 7, (TS, TS, 1))
    return _img(a)


def canvas():
    a = np.zeros((TS, TS, 3)) + np.array([46, 46, 48], float)
    for y in range(TS):
        if y % 3 == 0:
            a[y] += 10
    a += _rng(5).normal(0, 3, (TS, TS, 1))
    return _img(a)


def radome():
    a = np.zeros((TS, TS, 3)) + np.array([226, 226, 220], float)
    a += _rng(9).normal(0, 2.5, (TS, TS, 1))
    return _img(a)


def grating():
    """Galvanized bar grating: bearing bars one way, cross bars, open between (alpha)."""
    im = np.zeros((TS, TS, 4))
    for x in range(TS):
        if x % 5 == 0:
            im[:, x] = [168, 172, 174, 255]
    for y in range(TS):
        if y % 16 == 0:
            im[y, :] = [150, 154, 156, 255]
    return _img(im, "RGBA")


def blade():
    a = np.zeros((TS, TS, 3)) + np.array([40, 42, 46], float)
    a += _rng(4).normal(0, 2, (TS, TS, 1))
    return _img(a)


MATERIALS = {
    "rp_ahu": lambda: painted((206, 207, 201), 1),           # RAL 7035 light grey
    "rp_ahu_roof": lambda: painted((192, 194, 189), 2, 2.6, 0.6),
    "rp_dunnage": lambda: painted((96, 100, 104), 3, 3.0, 0.8),
    "rp_galv": lambda: galv(4),
    "rp_galv_dark": lambda: painted((132, 136, 138), 5, 3.0, 0.5),
    "rp_cooler": lambda: painted((204, 206, 202), 6, 1.8, 0.8),    # RAL 9002
    "rp_coil": coil,
    "rp_coil_top": coil_top,
    "rp_screen": bird_screen,
    "rp_louvre": louvre,
    "rp_rubber": rubber,
    "rp_gasket": lambda: painted((84, 86, 88), 24, 1.5, 0.3),
    "rp_concrete": concrete,
    "rp_canvas": canvas,
    "rp_radome": radome,
    "rp_grating": grating,
    "rp_alu": lambda: painted((186, 190, 194), 12, 2.0, 2.0),
    "rp_yellow": lambda: painted((232, 182, 30), 13, 2.0, 0.6),
    "rp_red": lambda: painted((178, 34, 30), 14, 2.0, 0.4),
    "rp_black": lambda: painted((26, 27, 29), 15, 1.5, 0.3),
    "rp_cable": lambda: painted((18, 18, 20), 16, 1.0, 0.2),
    "rp_cab": lambda: painted((198, 200, 196), 17, 1.8, 0.8),
    "rp_pvc": lambda: painted((140, 142, 146), 18, 1.5, 0.4),
    "rp_led_red": lambda: painted((255, 40, 30), 19, 1.0, 0.0),
    "rp_led_green": lambda: painted((60, 255, 90), 20, 1.0, 0.0),
    "rp_lamp_off": lambda: painted((150, 110, 40), 21, 1.0, 0.0),
    "rp_copper": lambda: painted((176, 104, 58), 23, 2.0, 0.0),
}
EMISSIVE = {"rp_led_red", "rp_led_green"}


# ------------------------------------------------------------------ pictures (labels, gauges, the fans' guards)
def _font(size, bold=True):
    return ImageFont.truetype(FONT if bold else FONT_R, size)


def plate(lines, bg=(236, 236, 230), fg=(20, 20, 22), border=(60, 60, 64)):
    def draw(im):
        d = ImageDraw.Draw(im)
        W, H = im.size
        d.rectangle((0, 0, W - 1, H - 1), fill=bg, outline=border, width=max(2, W // 60))
        n = len(lines)
        for i, t in enumerate(lines):
            f = _fit(d, t, W * 0.88, H / (n + 0.9), i == 0)
            d.text((W / 2, H * (i + 0.95) / (n + 0.9)), t, font=f, fill=fg, anchor="mm")
    return draw


def stencil(text, fg=(30, 32, 34), bg=None):
    def draw(im):
        d = ImageDraw.Draw(im)
        W, H = im.size
        if bg:
            d.rectangle((0, 0, W, H), fill=bg)
        f = _fit(d, text, W * 0.9, H * 0.78)
        d.text((W / 2, H / 2), text, font=f, fill=fg, anchor="mm")
    return draw


def _fit(d, text, wmax, hmax, bold=True):
    size = max(6, int(hmax))
    while size > 6:
        f = _font(size, bold)
        l, t, r, b = d.textbbox((0, 0), text, font=f)
        if r - l <= wmax and b - t <= hmax:
            return f
        size -= 1
    return _font(6, bold)


def warning(lines):
    def draw(im):
        d = ImageDraw.Draw(im)
        W, H = im.size
        d.rectangle((0, 0, W, H), fill=(250, 204, 20))
        d.rectangle((2, 2, W - 3, H - 3), outline=(20, 20, 20), width=max(2, H // 24))
        tri = [(H * 0.14, H * 0.8), (H * 0.5, H * 0.18), (H * 0.86, H * 0.8)]
        d.polygon(tri, fill=(20, 20, 20))
        d.text((H * 0.5, H * 0.6), "!", font=_font(int(H * 0.4)), fill=(250, 204, 20), anchor="mm")
        n = len(lines)
        for i, t in enumerate(lines):
            f = _fit(d, t, (W - H) * 0.9, H / (n + 1.2))
            d.text((H + (W - H) * 0.46, H * (i + 1) / (n + 1)), t, font=f, fill=(20, 20, 20), anchor="mm")
    return draw


def gauge():
    def draw(im):
        d = ImageDraw.Draw(im)
        W, H = im.size
        d.rectangle((0, 0, W, H), fill=(26, 26, 28))
        d.ellipse((2, 2, W - 3, H - 3), fill=(244, 244, 238), outline=(30, 30, 30), width=3)
        c = W / 2
        for i in range(11):
            a = math.radians(225 - 27 * i)
            d.line((c + c * 0.62 * math.cos(a), c - c * 0.62 * math.sin(a), c + c * 0.8 * math.cos(a), c - c * 0.8 * math.sin(a)),
                   fill=(20, 20, 20), width=2)
        a = math.radians(225 - 27 * 3.4)
        d.line((c, c, c + c * 0.7 * math.cos(a), c - c * 0.7 * math.sin(a)), fill=(200, 30, 20), width=3)
    return draw


# ------------------------------------------------------------------ geometry helpers (tuples for MW: from, to, tex[, opts])
def B(f, t, tex, rot=None):
    if rot:
        return (list(f), list(t), tex, {"rotation": rot})
    return (list(f), list(t), tex)


def octagon_ring(cx, cz, r_in, wall, y0, y1, tex):
    """An upright octagonal ring (a fan's nozzle): four plates square to the axes, four turned 45 degrees."""
    out = []
    s = r_in * math.tan(math.radians(22.5))       # half a side
    for sx in (-1, 1):
        x0 = cx + sx * r_in if sx > 0 else cx - r_in - wall
        out.append(B((x0, y0, cz - s), (x0 + wall, y1, cz + s), tex))
    for sz in (-1, 1):
        z0 = cz + sz * r_in if sz > 0 else cz - r_in - wall
        out.append(B((cx - s, y0, z0), (cx + s, y1, z0 + wall), tex))
    # the diagonal plates: built square to x at the centre's east, then turned about y round the ring's centre
    for k in range(4):
        ang = 45 if k % 2 == 0 else -45
        # place a plate centred at distance r_in + wall/2 along the diagonal: build along z at (cx + r_in, cz) and turn
        a = math.radians(45 + 90 * k)
        mx, mz = cx + (r_in + wall / 2) * math.cos(a), cz + (r_in + wall / 2) * math.sin(a)
        # a plate square to x (thin in x) centred at (mx, mz) and turned 45 degrees about y round its own centre
        out.append(B((mx - wall / 2, y0, mz - s), (mx + wall / 2, y1, mz + s), tex,
                     ("y", -45 if k % 2 == 0 else 45, (mx, (y0 + y1) / 2, mz))))
    return out


def round_y(cx, cz, r, y0, y1, tex, n=3):
    """A round part standing up (axis y) as slabs side by side, stepping in to the circle (training_kit.round_part)."""
    if n == 3:
        bands = ((0.42, 1.0), (0.80, 0.82), (1.0, 0.48))
    else:
        edges = [(k + 0.5) / (n - 0.5) for k in range(n)]
        bands = [(e, math.sqrt(max(0.0, 1 - ((edges[k - 1] + e) / 2) ** 2)) if k else 1.0) for k, e in enumerate(edges)]
    out, z_in = [], 0.0
    for i, (zo, hw) in enumerate(bands):
        spans = [(cz - zo * r, cz + zo * r)] if i == 0 else [(cz + z_in * r, cz + zo * r), (cz - zo * r, cz - z_in * r)]
        for za, zb in spans:
            out.append(B((cx - hw * r, y0, za), (cx + hw * r, y1, zb), tex))
        z_in = zo
    return out


def round_axis(axis, c, r, a0, a1, tex, n=3):
    """A round part along `axis` ('x' or 'z'), centred at c = (u, v) in the plane across it (u: the other horizontal
    axis, v: y), from a0 to a1 along the axis."""
    if n == 3:
        bands = ((0.42, 1.0), (0.80, 0.82), (1.0, 0.48))
    else:
        edges = [(k + 0.5) / (n - 0.5) for k in range(n)]
        bands = [(e, math.sqrt(max(0.0, 1 - ((edges[k - 1] + e) / 2) ** 2)) if k else 1.0) for k, e in enumerate(edges)]
    cu, cv = c
    out, v_in = [], 0.0
    for i, (vo, hw) in enumerate(bands):
        spans = [(cv - vo * r, cv + vo * r)] if i == 0 else [(cv + v_in * r, cv + vo * r), (cv - vo * r, cv - v_in * r)]
        for va, vb in spans:
            if axis == "x":
                out.append(B((a0, va, cu - hw * r), (a1, vb, cu + hw * r), tex))
            else:
                out.append(B((cu - hw * r, va, a0), (cu + hw * r, vb, a1), tex))
        v_in = vo
    return out


def door(x_face, z0, z1, y0, y1, side, tex, gasket="rp_gasket", hinge_z="low", latches=()):
    """A 3D access door on a face square to x at x_face (side +1: the face looks east, -1 west): a gasket proud of
    the face, the leaf proud of the gasket, two hinges on one edge, cam latches on the other."""
    s = side
    g0, g1 = (x_face, x_face + 0.25 * s)
    l0, l1 = (x_face + 0.25 * s, x_face + 0.75 * s)
    out = [B((min(g0, g1), y0 - 0.25, z0 - 0.25), (max(g0, g1), y1 + 0.25, z1 + 0.25), gasket),
           B((min(l0, l1), y0, z0), (max(l0, l1), y1, z1), tex)]
    hz = z0 if hinge_z == "low" else z1
    for hy in (y0 + 3.5, y1 - 3.5):
        k0, k1 = x_face + 0.75 * s, x_face + 1.45 * s
        out.append(B((min(k0, k1), hy - 1.4, hz - 0.7), (max(k0, k1), hy + 1.4, hz + 0.7), "rp_galv_dark"))
    lz = z1 - 1.8 if hinge_z == "low" else z0 + 1.8
    for ly in latches:
        b0, b1 = x_face + 0.75 * s, x_face + 1.1 * s
        out.append(B((min(b0, b1), ly - 1.1, lz - 0.6), (max(b0, b1), ly + 1.1, lz + 0.6), "rp_black"))
        h0, h1 = x_face + 1.1 * s, x_face + 1.7 * s
        out.append(B((min(h0, h1), ly - 0.35, lz - 0.45 - (1.4 if hinge_z == "low" else -1.4) * 0.5),
                     (max(h0, h1), ly + 0.35, lz + 0.45 - (1.4 if hinge_z == "low" else -1.4) * 0.5 + 0.0), "rp_black"))
    return out


PICS = {}          # pictures: key -> (draw, width px, height px); packed into rp_atlas, each stretched over its face


def pic(key, wpx, hpx, draw):
    """A picture's texture name for a face (roof_plant.write packs it into the atlas and gives the face its region)."""
    PICS.setdefault(key, (draw, wpx, hpx))
    return f"rp_pic_{key}!"


def label_x(x_face, side, z0, z1, y0, y1, tex, proud=0.15, back="rp_galv_dark"):
    """A label plate on a face square to x, its picture facing out."""
    a, b = x_face, x_face + proud * side
    return (list((min(a, b), y0, z0)), list((max(a, b), y1, z1)), {"*": back, ("east" if side > 0 else "west"): tex})


def label_z(z_face, side, x0, x1, y0, y1, tex, proud=0.15, back="rp_galv_dark"):
    a, b = z_face, z_face + proud * side
    return (list((x0, y0, min(a, b))), list((x1, y1, max(a, b))), {"*": back, ("south" if side > 0 else "north"): tex})


def octagon_prism(cx, cz, r, y0, y1, tex):
    """An upright octagonal prism (apothem r): a cross of two slabs and four 45-degree corner squares."""
    s = r * math.tan(math.radians(22.5))
    out = [B((cx - r, y0, cz - s), (cx + r, y1, cz + s), tex),
           B((cx - s, y0, cz - r), (cx + s, y1, cz - s), tex),
           B((cx - s, y0, cz + s), (cx + s, y1, cz + r), tex)]
    for sx in (-1, 1):
        for sz in (-1, 1):
            px, pz = cx + sx * s, cz + sz * s
            out.append(B((px - s, y0, pz - s), (px + s, y1, pz + s), tex, ("y", 45, (px, (y0 + y1) / 2, pz))))
    return out


def octagon_axis(axis, c, r, a0, a1, tex):
    """An octagonal prism lying along x or z (c = (u, v): u the other horizontal coordinate, v = y)."""
    cu, cv = c
    s = r * math.tan(math.radians(22.5))
    out = []
    if axis == "x":
        out += [B((a0, cv - s, cu - r), (a1, cv + s, cu + r), tex), B((a0, cv - r, cu - s), (a1, cv - s, cu + s), tex),
                B((a0, cv + s, cu - s), (a1, cv + r, cu + s), tex)]
        for sv in (-1, 1):
            for su in (-1, 1):
                pv, pu = cv + sv * s, cu + su * s
                out.append(B((a0, pv - s, pu - s), (a1, pv + s, pu + s), tex, ("x", 45, ((a0 + a1) / 2, pv, pu))))
    else:
        out += [B((cu - r, cv - s, a0), (cu + r, cv + s, a1), tex), B((cu - s, cv - r, a0), (cu + s, cv - s, a1), tex),
                B((cu - s, cv + s, a0), (cu + s, cv + r, a1), tex)]
        for sv in (-1, 1):
            for su in (-1, 1):
                pv, pu = cv + sv * s, cu + su * s
                out.append(B((pu - s, pv - s, a0), (pu + s, pv + s, a1), tex, ("z", 45, (pu, pv, (a0 + a1) / 2))))
    return out
