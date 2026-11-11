"""The Drone Parts Shelving's printed and moulded surfaces (work list item 9; tools/drone_parts_shelving.py), drawn at a
size that holds up close: Justin turned down the first mockup because things looked like "plain boxes with a label
slapped on" and a barcode ran over its text. Every print here is laid out to fit: text is measured and shrunk until it
fits its box; barcodes and QR codes get their own space. The prints go straight on their surfaces (flexo on kraft, a
wrap round a pack, a label round a cylinder), as the real ones are."""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FC = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
FCR = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed.ttf"
FM = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"


def arr(img):
    return np.asarray(img.convert("RGBA"), np.float32) / 255


def fit(d, text, box, font=FC, max_size=200, anchor="lm"):
    """Draws `text` as large as fits in box (x0, y0, x1, y1); returns the box it took."""
    x0, y0, x1, y1 = box
    size = max_size
    while size > 5:
        f = ImageFont.truetype(font, size)
        l, t, r, b = d.textbbox((0, 0), text, font=f)
        if r - l <= x1 - x0 and b - t <= y1 - y0:
            break
        size -= 1
    f = ImageFont.truetype(font, size)
    l, t, r, b = d.textbbox((0, 0), text, font=f)
    if anchor == "lm":
        pos = (x0 - l, (y0 + y1) / 2 - (t + b) / 2)
    elif anchor == "mm":
        pos = ((x0 + x1) / 2 - (l + r) / 2, (y0 + y1) / 2 - (t + b) / 2)
    else:  # "rm"
        pos = (x1 - r, (y0 + y1) / 2 - (t + b) / 2)
    return f, pos


def text(d, s, box, fill, font=FC, max_size=200, anchor="lm"):
    f, pos = fit(d, s, box, font, max_size, anchor)
    d.text(pos, s, font=f, fill=fill)


def barcode(d, box, seed, fill=(18, 18, 18, 255), digits=True):
    """A Code-128-looking barcode filling its box (the digits under it, inside the box)."""
    x0, y0, x1, y1 = box
    rnd = np.random.default_rng(seed)
    bars_y1 = y1 - (y1 - y0) * 0.22 if digits else y1
    x = x0 + 2
    while x < x1 - 4:
        w = int(rnd.integers(1, 4))
        d.rectangle((x, y0, max(x, min(x1 - 3, x + w - 1)), max(y0, bars_y1)), fill=fill)
        x += w + int(rnd.integers(1, 4))
    if digits:
        s = "".join(str(int(v)) for v in rnd.integers(0, 10, 12))
        text(d, s, (x0, bars_y1 + 1, x1, y1), fill, FM, anchor="mm")


def qr(d, box, seed, fill=(18, 18, 18, 255)):
    x0, y0, x1, y1 = box
    n = 21
    s = min(x1 - x0, y1 - y0) / n
    rnd = np.random.default_rng(seed)
    g = rnd.random((n, n)) > 0.5
    for (cx, cy) in ((0, 0), (n - 7, 0), (0, n - 7)):
        g[cy:cy + 7, cx:cx + 7] = False
        g[cy, cx:cx + 7] = g[cy + 6, cx:cx + 7] = True
        g[cy:cy + 7, cx] = g[cy:cy + 7, cx + 6] = True
        g[cy + 2:cy + 5, cx + 2:cx + 5] = True
    for j in range(n):
        for i in range(n):
            if g[j, i]:
                d.rectangle((x0 + i * s, y0 + j * s, x0 + (i + 1) * s - 1, y0 + (j + 1) * s - 1), fill=fill)


def logo(d, x, y, h, fill):
    """The Narv-Tech mark: a chevron N in a parallelogram, then the name."""
    w = h * 1.1
    d.polygon([(x + h * 0.25, y), (x + w + h * 0.25, y), (x + w, y + h), (x, y + h)], fill=fill)
    k = h * 0.18
    d.polygon([(x + h * 0.32, y + h - k), (x + h * 0.5, y + k), (x + h * 0.62, y + k), (x + h * 0.8, y + h * 0.55),
               (x + h * 0.95, y + k), (x + h * 1.08, y + k), (x + h * 0.9, y + h - k), (x + h * 0.78, y + h - k),
               (x + h * 0.6, y + h * 0.45), (x + h * 0.45, y + h - k)], fill=(0, 0, 0, 0))
    f, pos = fit(d, "NARV-TECH", (x + w + h * 0.45, y, x + w + h * 6, y + h), FB)
    d.text(pos, "NARV-TECH", font=f, fill=fill)
    return x + w + h * 0.45 + d.textlength("NARV-TECH", font=f)


# ------------------------------------------------------------------ cardboard

def kraft(w, h, seed, dirt=True, inside=False):
    rnd = np.random.default_rng(seed)
    base = np.array([150, 112, 72] if inside else [178, 136, 90], np.float32)
    a = np.ones((h, w, 3), np.float32) * base
    a += rnd.normal(0, 4, (h, w, 1))
    # fibres: short streaks
    for _ in range(w * h // 300):
        x, y = int(rnd.integers(0, w)), int(rnd.integers(0, h))
        ln = int(rnd.integers(3, 10))
        a[y, x:x + ln] += rnd.normal(0, 10)
    # the flutes faintly showing through as vertical banding
    a += (np.sin(np.arange(w) / max(1.5, w / 180) * math.pi) * 2.5)[None, :, None]
    if dirt:
        yy = np.linspace(0, 1, h)[:, None, None]
        a *= 1 - 0.10 * np.clip((yy - 0.85) / 0.15, 0, 1)
        xx = np.linspace(0, 1, w)[None, :, None]
        a *= 1 - 0.05 * (np.clip((0.05 - xx) / 0.05, 0, 1) + np.clip((xx - 0.95) / 0.05, 0, 1))
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return img


def handling(d, x, y, s, fill):
    """This-way-up arrows, keep dry, fragile: the three marks printed on every carton, in a row."""
    # this way up: two arrows over a bar
    for i in (0, 1):
        ax = x + s * (0.2 + i * 0.4)
        d.polygon([(ax, y + s * 0.1), (ax - s * 0.14, y + s * 0.35), (ax - s * 0.05, y + s * 0.35), (ax - s * 0.05, y + s * 0.8),
                   (ax + s * 0.05, y + s * 0.8), (ax + s * 0.05, y + s * 0.35), (ax + s * 0.14, y + s * 0.35)], fill=fill)
    d.rectangle((x + s * 0.02, y + s * 0.86, x + s * 0.78, y + s * 0.92), fill=fill)
    # keep dry: an umbrella with drops
    ux = x + s * 1.0
    d.pieslice((ux, y + s * 0.15, ux + s * 0.8, y + s * 0.85), 180, 360, fill=fill)
    d.line((ux + s * 0.4, y + s * 0.5, ux + s * 0.4, y + s * 0.85), fill=fill, width=max(1, int(s * 0.05)))
    d.arc((ux + s * 0.4, y + s * 0.72, ux + s * 0.56, y + s * 0.92), 0, 180, fill=fill, width=max(1, int(s * 0.05)))
    for k in range(3):
        d.ellipse((ux + s * (0.1 + k * 0.28), y, ux + s * (0.16 + k * 0.28), y + s * 0.09), fill=fill)
    # fragile: a stemmed glass
    gx = x + s * 2.0
    d.polygon([(gx + s * 0.1, y + s * 0.08), (gx + s * 0.6, y + s * 0.08), (gx + s * 0.5, y + s * 0.45), (gx + s * 0.2, y + s * 0.45)], fill=fill)
    d.rectangle((gx + s * 0.32, y + s * 0.45, gx + s * 0.38, y + s * 0.8), fill=fill)
    d.rectangle((gx + s * 0.15, y + s * 0.8, gx + s * 0.55, y + s * 0.87), fill=fill)


def carton_front(w_m, h_m, item, qty, seed, ink=(36, 40, 58, 255)):
    """The long side of a parts carton, flexo-printed in one ink straight on the kraft (no stuck-on label): the mark and
    the line under it, the part in big type, the count, the handling marks at the bottom right."""
    px = 500
    w, h = max(96, int(w_m * px)), max(72, int(h_m * px))
    img = kraft(w, h, seed)
    d = ImageDraw.Draw(img)
    m = min(w, h) * 0.07
    lh = min(h * 0.13, w * 0.06)
    right = logo(d, m, m, lh, ink)
    text(d, "DRONE SYSTEMS", (right + lh * 0.5, m + lh * 0.2, w - m, m + lh * 0.85), ink, FCR)
    d.rectangle((m, m + lh * 1.3, w - m, m + lh * 1.3 + max(2, h * 0.008)), fill=ink)
    text(d, item, (m, m + lh * 1.65, w - m, m + lh * 1.65 + h * 0.26), ink, FB)
    text(d, qty, (m, m + lh * 1.75 + h * 0.28, w * 0.5, m + lh * 1.75 + h * 0.40), ink, FC)
    s = min(h * 0.20, w * 0.085)
    handling(d, w - m - s * 2.7, h - m - s, s, ink)
    text(d, "RECYCLABLE CORRUGATED  -  KEEP DRY", (m, h - m - s * 0.42, w - m - s * 3.0, h - m), ink, FCR)
    return arr(img)


def carton_end(d_m, h_m, item, seed, part_no):
    """The end of a carton: the warehouse's shipping label (laid out so nothing overlaps) on the kraft."""
    px = 500
    w, h = max(64, int(d_m * px)), max(64, int(h_m * px))
    img = kraft(w, h, seed + 1)
    d = ImageDraw.Draw(img)
    lw, lh = int(w * 0.78), int(min(h * 0.72, w * 0.62))
    x0, y0 = (w - lw) // 2, int(h * 0.12)
    d.rectangle((x0 + 2, y0 + 2, x0 + lw + 2, y0 + lh + 2), fill=(120, 90, 60, 255))
    d.rectangle((x0, y0, x0 + lw, y0 + lh), fill=(246, 246, 240, 255))
    ink = (20, 20, 22, 255)
    p = lw * 0.05
    text(d, "NARV-TECH LOGISTICS", (x0 + p, y0 + p, x0 + lw - p, y0 + lh * 0.14), ink, FB)
    d.line((x0 + p, y0 + lh * 0.18, x0 + lw - p, y0 + lh * 0.18), fill=ink, width=1)
    rows = [("PART", part_no), ("DESC", item), ("QTY", "1 CTN"), ("LOT", "%05d" % (seed * 37 % 99999))]
    for i, (k, v) in enumerate(rows):
        ry0 = y0 + lh * (0.21 + i * 0.105)
        text(d, k, (x0 + p, ry0, x0 + lw * 0.22, ry0 + lh * 0.09), ink, FCR)
        text(d, v, (x0 + lw * 0.24, ry0, x0 + lw * 0.66, ry0 + lh * 0.09), ink, FC)
    qr(d, (x0 + lw * 0.70, y0 + lh * 0.22, x0 + lw - p, y0 + lh * 0.22 + (lw * 0.30 - p)), seed)
    barcode(d, (x0 + p, y0 + lh * 0.66, x0 + lw - p, y0 + lh - p), seed)
    return arr(img)


def tape(w=64, h=64):
    a = np.zeros((h, w, 4), np.float32)
    rnd = np.random.default_rng(3)
    a[..., :3] = np.array([0.62, 0.46, 0.27])
    a[..., :3] += (rnd.normal(0, 0.02, (1, w, 1)))
    a[..., :3] += (np.sin(np.arange(h) / 3.0) * 0.015)[:, None, None]
    a[:, :2, :3] *= 0.85
    a[:, -2:, :3] *= 0.85
    a[..., 3] = 1
    return np.clip(a, 0, 1)


def corrugated_edge(w=64, h=16):
    """The cut edge of a flap: liners and the wave of the flute between them."""
    img = Image.new("RGBA", (w, h), (176, 134, 88, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, w, 2), fill=(150, 112, 72, 255))
    d.rectangle((0, h - 3, w, h), fill=(150, 112, 72, 255))
    pts = [(x, h / 2 + math.sin(x / 2.2) * (h / 2 - 4)) for x in range(w)]
    d.line(pts, fill=(120, 88, 54, 255), width=2)
    return arr(img)


# ------------------------------------------------------------------ the shelf's labels

def beam_label(loc, desc, seed, colour):
    """A shelf-edge label in its clear holder: the location big on a colour block, the description, a barcode in its own
    space at the right (never over the text)."""
    w, h = 768, 60
    img = Image.new("RGBA", (w, h), (246, 246, 242, 255))
    d = ImageDraw.Draw(img)
    ink = (18, 18, 20, 255)
    d.rectangle((0, 0, 128, h), fill=colour + (255,))
    text(d, loc, (8, 6, 120, h - 6), (255, 255, 255, 255), FB, anchor="mm")
    text(d, desc, (142, 8, 525, h - 8), ink, FC)
    barcode(d, (548, 6, w - 9, h - 4), seed)
    return arr(img)


def bin_card(text_s, seed):
    w, h = 192, 72
    img = Image.new("RGBA", (w, h), (248, 248, 244, 255))
    d = ImageDraw.Draw(img)
    ink = (18, 18, 20, 255)
    text(d, text_s, (8, 6, w - 8, h * 0.58), ink, FB, anchor="mm")
    barcode(d, (30, h * 0.64, w - 30, h - 4), seed, digits=False)
    return arr(img)


# ------------------------------------------------------------------ cases, batteries, chargers

def plastic(w, h, base, seed, grain=3.0, rib=None):
    rnd = np.random.default_rng(seed)
    a = np.ones((h, w, 4), np.float32)
    a[..., :3] = np.array(base, np.float32) / 255
    a[..., :3] += rnd.normal(0, grain / 255, (h, w, 1))
    if rib:
        a[..., :3] *= (1 - 0.18 * (np.sin(np.arange(h) / rib * math.pi) > 0.6))[:, None, None]
    return np.clip(a, 0, 1)


def id_plate(title, sub, seed):
    """An engraved anodised aluminium plate (riveted into a recess on a case's lid)."""
    w, h = 384, 128
    a = plastic(w, h, (176, 180, 186), seed, 6)
    a[..., :3] += (np.random.default_rng(seed).normal(0, 0.03, (1, w, 1)))   # brushing
    img = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    ink = (40, 42, 46, 255)
    logo(d, 22, 12, 30, ink)
    text(d, title, (22, 50, w - 22, 84), ink, FB)
    text(d, sub, (22, 92, w - 60, 114), ink, FCR)
    for cx in (10, w - 10):
        for cy in (10, h - 10):
            d.ellipse((cx - 6, cy - 6, cx + 6, cy + 6), fill=(130, 134, 140, 255), outline=(90, 92, 96, 255))
    return arr(img)


def smart_battery_face(seed, level=3):
    """The front of a large smart pack: the moulded grip band, the embossed name, the printed spec block (part of the
    housing's print, small grey type), the power button and four gauge LEDs (lit by the renderer)."""
    w, h = 192, 512
    a = plastic(w, h, (54, 57, 62), seed, 2.5)
    img = Image.fromarray((a * 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    # the darker grip band down the middle with its ribs
    d.rectangle((0, int(h * 0.30), w, int(h * 0.74)), fill=(40, 42, 46, 255))
    for y in range(int(h * 0.31), int(h * 0.73), 9):
        d.line((8, y, w - 8, y), fill=(30, 31, 34, 255), width=3)
    # the embossed name (a lighter edge under a darker letter)
    f, pos = fit(d, "NARV-TECH", (14, h * 0.10, w - 14, h * 0.17), FB)
    d.text((pos[0] + 1, pos[1] + 1), "NARV-TECH", font=f, fill=(78, 82, 88, 255))
    d.text(pos, "NARV-TECH", font=f, fill=(34, 36, 40, 255))
    text(d, "SMART FLIGHT BATTERY", (14, h * 0.18, w - 14, h * 0.22), (120, 124, 130, 255), FCR)
    text(d, "44.4V  5935mAh  263Wh", (14, h * 0.79, w - 14, h * 0.83), (120, 124, 130, 255), FCR)
    text(d, "LI-ION 12S  DO NOT PUNCTURE", (14, h * 0.84, w - 14, h * 0.875), (110, 114, 120, 255), FCR)
    # the button well and the LED window (the LEDs themselves are lit quads)
    d.rounded_rectangle((w * 0.18, h * 0.235, w * 0.82, h * 0.285), radius=8, fill=(22, 23, 26, 255))
    return arr(img)


def lipo_wrap(cells, mah, seed, colour=(30, 120, 210)):
    """A LiPo pack's printed heat-shrink wrap (the whole pack is printed; there is no stuck-on label)."""
    w, h = 512, 192
    a = plastic(w, h, (24, 25, 28), seed, 2.0)
    img = Image.fromarray((a * 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    logo(d, 20, 12, 30, (236, 236, 236, 255))
    text(d, "%dS  %.1fV" % (cells, cells * 3.7), (20, 50, w * 0.48, 90), (236, 236, 236, 255), FB)
    text(d, "%d mAh" % mah, (w * 0.52, 50, w - 20, 90), (255, 186, 80, 255), FB, anchor="rm")
    text(d, "100C  LiPo  FLIGHT PACK", (20, 96, w * 0.62, 116), (180, 184, 190, 255), FCR)
    d.polygon([(0, 128), (w * 0.60, 128), (w * 0.66, h), (0, h)], fill=colour + (255,))
    d.polygon([(w * 0.62, 128), (w * 0.66, 128), (w * 0.72, h), (w * 0.68, h)], fill=colour + (255,))
    text(d, "CHARGE IN A FIRE-SAFE CONTAINER", (20, 144, w * 0.56, 172), (250, 250, 250, 255), FC)
    return arr(img)


def charger_screen(t=0.0, levels=(0.62, 0.35, 0.88, 0.47)):
    w, h = 320, 200
    img = Image.new("RGBA", (w, h), (6, 12, 18, 255))
    d = ImageDraw.Draw(img)
    text(d, "QUAD BALANCE CHARGER", (8, 4, w - 8, 26), (255, 186, 90, 255), FB)
    for i, lv in enumerate(levels):
        y = 34 + i * 40
        text(d, "CH%d" % (i + 1), (8, y, 58, y + 30), (170, 200, 210, 255), FC)
        d.rectangle((64, y + 6, 236, y + 24), outline=(90, 110, 120, 255))
        col = (90, 230, 120, 255) if lv > 0.5 else (255, 186, 80, 255)
        d.rectangle((66, y + 8, 66 + int(168 * lv), y + 22), fill=col)
        text(d, "%d%%" % int(lv * 100), (244, y, w - 8, y + 30), (220, 230, 235, 255), FC, anchor="rm")
    return arr(img)


def station_screen():
    w, h = 256, 96
    img = Image.new("RGBA", (w, h), (6, 12, 18, 255))
    d = ImageDraw.Draw(img)
    text(d, "BATTERY STATION", (8, 4, w - 8, 30), (255, 186, 90, 255), FB)
    text(d, "8 PACKS   6 READY   STORAGE MODE", (8, 38, w - 8, 60), (170, 220, 200, 255), FC)
    text(d, "NEXT  T4-07  41 MIN", (8, 66, w - 8, 90), (220, 230, 235, 255), FC)
    return arr(img)


def vents(w, h, base, seed, rows=True):
    a = plastic(w, h, base, seed, 2)
    img = Image.fromarray((a * 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    for y in range(int(h * 0.15), int(h * 0.85), 10):
        d.rounded_rectangle((w * 0.12, y, w * 0.88, y + 4), radius=2, fill=(10, 10, 12, 255))
    return arr(img)


# ------------------------------------------------------------------ the extinguisher

def extinguisher_label():
    """The label printed round the cylinder: red header, the agent and size, the A B C class marks, the four use pictograms
    (pull, aim, squeeze, sweep) with their words, the small print. Wraps the front 150 degrees."""
    w, h = 720, 480
    img = Image.new("RGBA", (w, h), (244, 244, 238, 255))
    d = ImageDraw.Draw(img)
    red, ink = (196, 28, 26, 255), (20, 20, 22, 255)
    d.rectangle((0, 0, w, 86), fill=red)
    text(d, "FIRE EXTINGUISHER", (30, 10, w - 30, 76), (255, 255, 255, 255), FB, anchor="mm")
    text(d, "ABC DRY POWDER  6 KG", (30, 96, w - 30, 150), ink, FB, anchor="mm")
    # the class marks
    for i, (k, col) in enumerate((("A", (40, 140, 60)), ("B", (196, 28, 26)), ("C", (30, 90, 180)))):
        x = w / 2 - 150 + i * 110
        d.rounded_rectangle((x, 164, x + 84, 240), radius=10, fill=col + (255,))
        text(d, k, (x, 166, x + 84, 238), (255, 255, 255, 255), FB, anchor="mm")
    # the four pictograms
    words = ("PULL", "AIM", "SQUEEZE", "SWEEP")
    for i, wd in enumerate(words):
        x = 40 + i * 165
        d.rounded_rectangle((x, 262, x + 140, 372), radius=8, outline=ink, width=3)
        cx, cy = x + 70, 312
        if i == 0:
            d.ellipse((cx - 28, cy - 28, cx + 2, cy + 2), outline=ink, width=5)
            d.line((cx - 2, cy - 2, cx + 34, cy + 30), fill=ink, width=6)
        elif i == 1:
            d.line((cx - 40, cy + 20, cx + 10, cy - 10), fill=ink, width=8)
            d.polygon([(cx + 10, cy - 24), (cx + 40, cy - 10), (cx + 14, cy + 6)], fill=ink)
        elif i == 2:
            d.rectangle((cx - 30, cy - 30, cx + 30, cy - 18), fill=ink)
            d.rectangle((cx - 30, cy + 2, cx + 30, cy + 14), fill=ink)
            d.polygon([(cx, cy - 14), (cx - 10, cy - 2), (cx + 10, cy - 2)], fill=ink)
        else:
            d.arc((cx - 44, cy - 30, cx + 44, cy + 40), 200, 340, fill=ink, width=7)
            d.polygon([(cx + 40, cy - 16), (cx + 46, cy + 4), (cx + 28, cy - 2)], fill=ink)
        text(d, wd, (x, 378, x + 140, 404), ink, FB, anchor="mm")
    text(d, "STORED PRESSURE  15 BAR  -30 TO +60 C   RECHARGE AFTER ANY USE   INSPECT MONTHLY", (20, 420, w - 20, 446), ink, FCR, anchor="mm")
    text(d, "NARV-TECH SAFETY   EN3   SERVICE 10/2026", (20, 448, w - 20, 472), (90, 90, 94, 255), FCR, anchor="mm")
    return arr(img)


def gauge_dial():
    w = 96
    img = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((2, 2, w - 3, w - 3), fill=(236, 236, 230, 255), outline=(60, 60, 64, 255), width=4)
    d.arc((14, 14, w - 15, w - 15), 200, 250, fill=(196, 28, 26, 255), width=10)
    d.arc((14, 14, w - 15, w - 15), 250, 290, fill=(40, 170, 60, 255), width=10)
    d.arc((14, 14, w - 15, w - 15), 290, 340, fill=(196, 28, 26, 255), width=10)
    a = math.radians(272)
    d.line((w / 2, w / 2, w / 2 + math.cos(a) * 34, w / 2 + math.sin(a) * 34), fill=(18, 18, 20, 255), width=3)
    d.ellipse((w / 2 - 5, w / 2 - 5, w / 2 + 5, w / 2 + 5), fill=(18, 18, 20, 255))
    return arr(img)


def ext_sign():
    """The fire-equipment sign over it: the white extinguisher pictogram and arrow on red (ISO 7010 F001 style)."""
    w = 256
    img = Image.new("RGBA", (w, w), (196, 28, 26, 255))
    d = ImageDraw.Draw(img)
    white = (255, 255, 255, 255)
    d.rectangle((6, 6, w - 7, w - 7), outline=(255, 255, 255, 255), width=4)
    # the extinguisher
    d.rounded_rectangle((96, 70, 146, 210), radius=18, fill=white)
    d.rectangle((108, 50, 134, 72), fill=white)
    d.polygon([(100, 50), (150, 40), (152, 50), (102, 58)], fill=white)
    d.line((134, 58, 176, 80, 186, 140), fill=white, width=8)
    # a flame beside it
    d.polygon([(40, 210), (34, 170), (52, 140), (56, 168), (70, 128), (82, 176), (88, 156), (90, 210)], fill=white)
    return arr(img)


# ------------------------------------------------------------------ bin contents

def heap_texture(kind, seed, size=128):
    """A heap of small parts seen from above (the mound in a bin is printed with it; loose pieces are modelled on top)."""
    rnd = np.random.default_rng(seed)
    base = {"screws": (60, 46, 30), "nuts": (70, 74, 80), "xt90": (150, 120, 30), "bearings": (90, 94, 100), "esc": (40, 60, 110),
            "standoffs": (130, 104, 50), "gps": (40, 42, 46)}[kind]
    big = 256
    img = Image.new("RGBA", (big, big), base + (255,))
    d = ImageDraw.Draw(img)
    n = {"screws": 900, "nuts": 600, "xt90": 160, "bearings": 260, "esc": 70, "standoffs": 420, "gps": 40}[kind]
    for _ in range(n):
        x, y = rnd.random() * big, rnd.random() * big
        a = rnd.random() * math.pi
        sh = 0.75 + 0.25 * rnd.random()
        if kind == "screws":
            dx, dy = math.cos(a) * 9, math.sin(a) * 9
            d.line((x, y, x + dx, y + dy), fill=tuple(int(c * sh) for c in (24, 24, 26)) + (255,), width=3)
            d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=tuple(int(c * sh) for c in (40, 40, 44)) + (255,))
        elif kind == "nuts":
            d.regular_polygon((x, y, 4), 6, rotation=a * 57, fill=tuple(int(c * sh) for c in (160, 166, 172)) + (255,))
            d.ellipse((x - 1.6, y - 1.6, x + 1.6, y + 1.6), fill=(30, 60, 160, 255))
        elif kind == "xt90":
            dx, dy = math.cos(a) * 10, math.sin(a) * 10
            d.line((x, y, x + dx, y + dy), fill=tuple(int(c * sh) for c in (236, 196, 36)) + (255,), width=9)
        elif kind == "bearings":
            d.ellipse((x - 7, y - 7, x + 7, y + 7), fill=tuple(int(c * sh) for c in (196, 200, 206)) + (255,))
            d.ellipse((x - 4, y - 4, x + 4, y + 4), fill=(24, 24, 26, 255))
        elif kind == "esc":
            d.rectangle((x - 12, y - 7, x + 12, y + 7), fill=tuple(int(c * sh) for c in (30, 90, 190)) + (255,))
            d.line((x + 12, y - 3, x + 22, y - 1), fill=(196, 30, 28, 255), width=2)
            d.line((x + 12, y + 3, x + 22, y + 5), fill=(20, 20, 22, 255), width=2)
        elif kind == "standoffs":
            dx, dy = math.cos(a) * 12, math.sin(a) * 12
            d.line((x, y, x + dx, y + dy), fill=tuple(int(c * sh) for c in (200, 164, 90)) + (255,), width=4)
        elif kind == "gps":
            d.ellipse((x - 14, y - 14, x + 14, y + 14), fill=(30, 31, 34, 255))
            d.ellipse((x - 8, y - 8, x + 8, y + 8), fill=(220, 220, 216, 255))
    return arr(img.resize((size, size), Image.LANCZOS))
