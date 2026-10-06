"""Materials for the nuclear equipment, the hazmat suit and the nuclear pipes (tools/nuclear.py): realistic in what
they show (brushed metal, chipped paint, rust at seams and bolts, poured concrete) but kept in Minecraft's language.
Each one is drawn at 128 px with layered noise and real-looking details, then brought down to 64 px a block and its
colours reduced to a small palette (stylize), so it reads as a crisp, slightly hand-painted block texture next to
vanilla rather than a photo. Each material tiles on itself, since the renderer repeats it about once a block.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

CELL = 128
OUT = 64


def stylize(img, colours=14):
    """Brings a 128 px material down to the game's 64 px and a small palette, keeping its transparency."""
    img = img.convert("RGBA")
    alpha = img.split()[3].resize((OUT, OUT), Image.LANCZOS)
    small = img.convert("RGB").resize((OUT, OUT), Image.LANCZOS)
    small = small.quantize(colors=colours, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGBA")
    # Alpha is either cut out or kept (glowing texels are marked with 254 in the full image).
    a = np.asarray(alpha, dtype=np.uint8)
    full = np.asarray(img.split()[3].resize((OUT, OUT), Image.NEAREST), dtype=np.uint8)
    a = np.where(a < 128, 0, np.where(full == 254, 254, 255)).astype(np.uint8)
    out = np.asarray(small).copy()
    out[..., 3] = a
    return Image.fromarray(out, "RGBA")


# ---------------------------------------------------------------- noise and helpers

def _rng(seed):
    return np.random.default_rng(seed)


def value_noise(size, seed, scale):
    """Smooth, tiling value noise in [-0.5, 0.5]: random values on a coarse grid, enlarged with wrap-around."""
    r = _rng(seed)
    small = max(2, size // scale)
    grid = r.random((small, small))
    # Tile the grid 3x3 and crop the middle after resizing, so the edges wrap.
    big = np.tile(grid, (3, 3))
    img = Image.fromarray((big * 255).astype(np.uint8), "L").resize((size * 3, size * 3), Image.BICUBIC)
    return np.asarray(img, dtype=np.float32)[size:2 * size, size:2 * size] / 255 - 0.5


def fbm(size, seed, octaves=4, base_scale=32):
    """Several octaves of value noise, halving in size and strength: natural mottling."""
    out = np.zeros((size, size), dtype=np.float32)
    amp, scale = 1.0, base_scale
    total = 0
    for i in range(octaves):
        out += value_noise(size, seed * 7 + i, max(2, scale)) * amp
        total += amp
        amp *= 0.5
        scale //= 2
    return out / total


def base(colour, size=CELL):
    a = np.zeros((size, size, 4), dtype=np.float32)
    a[..., :3] = colour[:3]
    a[..., 3] = 255
    return a


def shade(a, amount):
    """Adds a per-pixel brightness map (2D array) to the colour channels."""
    a[..., :3] += amount[..., None]
    return a


def tint(a, mask, colour, strength=1.0):
    """Blends colour in where mask (0..1) is set."""
    m = np.clip(mask, 0, 1)[..., None] * strength
    a[..., :3] = a[..., :3] * (1 - m) + np.array(colour[:3], dtype=np.float32) * m
    return a


def to_img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def grain(size, seed, amount):
    return (_rng(seed).random((size, size)).astype(np.float32) - 0.5) * amount


def streaks(size, seed, direction="v", count=180, length=(20, 90), amount=14):
    """Fine brushed-metal lines: short random segments with a soft falloff, along one axis."""
    r = _rng(seed)
    out = np.zeros((size, size), dtype=np.float32)
    for _ in range(count):
        pos = r.integers(0, size)
        start = r.integers(0, size)
        ln = r.integers(length[0], length[1])
        strength = r.uniform(-1, 1) * amount
        idx = (np.arange(start, start + ln)) % size
        fade = np.sin(np.linspace(0, math.pi, ln)).astype(np.float32)
        if direction == "v":
            out[idx, pos] += strength * fade
        else:
            out[pos, idx] += strength * fade
    return out


def drips(size, seed, count=6, amount=18, colour_shift=None):
    """Dirty water streaks running down from random points, widening a little and fading."""
    r = _rng(seed)
    out = np.zeros((size, size), dtype=np.float32)
    for _ in range(count):
        x = r.integers(0, size)
        y0 = r.integers(0, size // 2)
        ln = r.integers(size // 3, size)
        w = r.uniform(0.8, 2.2)
        for i in range(ln):
            y = (y0 + i) % size
            f = (1 - i / ln) ** 1.5
            for dx in range(-3, 4):
                xx = (x + dx) % size
                out[y, xx] -= amount * f * math.exp(-(dx / w) ** 2)
    return out


def chips(a, seed, count, under=(70, 72, 76), size=CELL, max_r=5):
    """Chipped paint: small ragged patches showing the metal underneath, with a light worn rim."""
    r = _rng(seed)
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for _ in range(count):
        cx, cy = int(r.integers(0, size)), int(r.integers(0, size))
        pts = []
        n = 7
        rad = r.uniform(2.5, max_r)
        for k in range(n):
            ang = 2 * math.pi * k / n
            rr = rad * r.uniform(0.6, 1.3)
            pts.append((cx + math.cos(ang) * rr, cy + math.sin(ang) * rr))
        d.polygon(pts, fill=under + (255,))
    return np.asarray(img).astype(np.float32)


def rust(a, mask, seed, strength=0.8):
    """Rust blooming where mask is set: orange-brown, broken up by noise so it looks grown, not painted."""
    n = fbm(a.shape[0], seed, octaves=3, base_scale=16) + 0.5
    m = np.clip(mask, 0, 1) * np.clip(n * 1.4, 0, 1)
    tint(a, m, (150, 78, 36), strength)
    tint(a, m * (n > 0.6), (96, 46, 22), strength)
    return a


def seam_mask(size, y=None, x=None, width=3, bloom=10):
    """A soft band round a horizontal (y) or vertical (x) seam, 1 at the seam fading to 0 at bloom pixels."""
    yy, xx = np.mgrid[0:size, 0:size]
    if y is not None:
        d = np.minimum(np.abs(yy - y), size - np.abs(yy - y))
    else:
        d = np.minimum(np.abs(xx - x), size - np.abs(xx - x))
    return np.clip(1 - (d - width) / bloom, 0, 1).astype(np.float32)


def spot_mask(size, points, radius=6):
    yy, xx = np.mgrid[0:size, 0:size]
    out = np.zeros((size, size), dtype=np.float32)
    for px, py in points:
        d = np.hypot(np.minimum(np.abs(xx - px), size - np.abs(xx - px)), np.minimum(np.abs(yy - py), size - np.abs(yy - py)))
        out = np.maximum(out, np.clip(1 - d / radius, 0, 1))
    return out


def bolt(d, x, y, r, head=(150, 154, 160), hexagonal=True):
    """A bolt head lit from the top left: a dark socket ring, a bright crown, a shadow below."""
    d.ellipse([x - r - 1, y - r - 1, x + r + 1, y + r + 1], fill=(40, 42, 46, 255))
    if hexagonal:
        pts = [(x + math.cos(math.radians(60 * k + 30)) * r, y + math.sin(math.radians(60 * k + 30)) * r) for k in range(6)]
        d.polygon(pts, fill=head + (255,))
        inner = [(x - 0.4 + math.cos(math.radians(60 * k + 30)) * r * 0.55, y - 0.4 + math.sin(math.radians(60 * k + 30)) * r * 0.55) for k in range(6)]
        d.polygon(inner, fill=tuple(min(255, c + 40) for c in head) + (255,))
    else:
        d.ellipse([x - r, y - r, x + r, y + r], fill=head + (255,))
        d.ellipse([x - r * 0.6, y - r * 0.6, x + r * 0.1, y + r * 0.1], fill=tuple(min(255, c + 50) for c in head) + (255,))
    d.arc([x - r, y - r, x + r, y + r], 20, 160, fill=(30, 32, 36, 255), width=1)


def seam_line(d, size, y=None, x=None, dark=(40, 42, 46), light=(210, 214, 218), width=1):
    """A recessed panel seam: a dark groove with a lit edge below or right of it."""
    if y is not None:
        d.line([(0, y), (size, y)], fill=dark + (255,), width=width)
        d.line([(0, y + width), (size, y + width)], fill=light + (255,), width=1)
    else:
        d.line([(x, 0), (x, size)], fill=dark + (255,), width=width)
        d.line([(x + width, 0), (x + width, size)], fill=light + (255,), width=1)


# ---------------------------------------------------------------- metals

def brushed(colour, seed, direction="v", seams=(), bolts=(), dirt=0.35, size=CELL):
    """Brushed stainless or steel plate: long fine streaks, soft mottling, a hint of oil-stain colour, grooves and bolts."""
    a = base(colour, size)
    shade(a, fbm(size, seed, 3, 32) * 18)
    shade(a, streaks(size, seed + 1, direction, count=120, length=(40, 128), amount=12))
    shade(a, grain(size, seed + 2, 4))
    # Faint warm/cool stain patches, like handled or heated metal.
    n = fbm(size, seed + 3, 3, 64)
    tint(a, np.clip(n * 2, 0, 1) * dirt, (colour[0] + 10, colour[1] - 4, colour[2] - 18), 0.5)
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for kind, pos in seams:
        if kind == "h":
            seam_line(d, size, y=pos, dark=tuple(max(0, c - 110) for c in colour), light=tuple(min(255, c + 30) for c in colour))
        else:
            seam_line(d, size, x=pos, dark=tuple(max(0, c - 110) for c in colour), light=tuple(min(255, c + 30) for c in colour))
    for x, y in bolts:
        bolt(d, x, y, 4, head=tuple(min(255, c + 10) for c in colour))
    a = np.asarray(img).astype(np.float32)
    if bolts:
        a = rust(a, spot_mask(size, bolts, 9) * 0.5, seed + 4, 0.45)
    return to_img(a)


def painted(colour, seed, seams=(), bolts=(), chip_count=10, drip_count=5, dirt=0.5, size=CELL):
    """Painted steel: a smooth coat with broad mottling, dirt streaks running down, chips at the edges and bolts,
    and rust creeping out of the seams."""
    a = base(colour, size)
    shade(a, fbm(size, seed, 3, 64) * 14)
    shade(a, grain(size, seed + 1, 3))
    shade(a, drips(size, seed + 2, count=drip_count, amount=14 * dirt))
    img = to_img(a)
    d = ImageDraw.Draw(img)
    dark = tuple(max(0, int(c * 0.45)) for c in colour)
    light = tuple(min(255, c + 26) for c in colour)
    for kind, pos in seams:
        if kind == "h":
            seam_line(d, size, y=pos, dark=dark, light=light)
        else:
            seam_line(d, size, x=pos, dark=dark, light=light)
    for x, y in bolts:
        bolt(d, x, y, 4, head=tuple(min(255, c + 16) for c in colour))
    a = np.asarray(img).astype(np.float32)
    mask = np.zeros((size, size), dtype=np.float32)
    for kind, pos in seams:
        mask = np.maximum(mask, (seam_mask(size, y=pos, width=2, bloom=8) if kind == "h" else seam_mask(size, x=pos, width=2, bloom=8)) * 0.55)
    if bolts:
        mask = np.maximum(mask, spot_mask(size, bolts, 10) * 0.7)
    if seams or bolts:
        a = rust(a, mask * dirt, seed + 3, 0.7)
    if chip_count:
        a = chips(a, seed + 4, chip_count, size=size)
    return to_img(a)


def weld_band(seed, size=CELL):
    """A circumferential weld: stacked ripples of bead down the middle, a straw-and-blue heat tint either side, and
    the ground plate beyond."""
    img = brushed((176, 180, 184), seed, "h")
    a = np.asarray(img).astype(np.float32)
    yy, xx = np.mgrid[0:size, 0:size]
    centre = size / 2
    heat = np.clip(1 - np.abs(yy - centre) / 26, 0, 1) * (np.abs(yy - centre) > 7)
    tint(a, heat * 0.5, (190, 150, 90), 0.5)                                  # straw
    tint(a, np.clip(1 - np.abs(np.abs(yy - centre) - 18) / 6, 0, 1) * 0.5, (96, 120, 160), 0.5)   # blue band
    img = to_img(a)
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for x in range(-6, size + 6, 5):
        wob = r.randint(-1, 1)
        d.ellipse([x - 5, centre - 6 + wob, x + 5, centre + 6 + wob], fill=(118, 112, 106, 255), outline=(86, 80, 76, 255))
        d.ellipse([x - 3, centre - 4 + wob, x + 2, centre - 1 + wob], fill=(160, 152, 144, 255))
    return img


def flange(seed, size=CELL):
    """A bolted flange ring: brushed steel with a row of hex bolts and nuts, grease stains under them."""
    img = brushed((146, 150, 156), seed, "h", seams=(("h", 6), ("h", size - 8)))
    a = np.asarray(img).astype(np.float32)
    pts = [(x, size // 2) for x in range(8, size, 16)]
    a = rust(a, spot_mask(size, [(x, y + 10) for x, y in pts], 8) * 0.6, seed + 9, 0.5)
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for x, y in pts:
        bolt(d, x, y, 6, head=(170, 174, 180))
    return img


def crdm(seed, size=CELL):
    """Control-rod drive housings: dark steel tubes with cooling-fin bands and a yellow position mark."""
    img = brushed((88, 94, 102), seed, "v", dirt=0.2)
    d = ImageDraw.Draw(img)
    for y in range(0, size, 24):
        d.rectangle([0, y, size, y + 2], fill=(150, 156, 164, 255))
        d.rectangle([0, y + 3, size, y + 4], fill=(60, 64, 70, 255))
    for y in range(12, size, 24):
        d.rectangle([size // 2 - 8, y, size // 2 + 8, y + 5], fill=(226, 184, 40, 255))
        d.rectangle([size // 2 - 8, y + 5, size // 2 + 8, y + 6], fill=(150, 120, 20, 255))
    return img


def copper(seed, size=CELL):
    a = base((190, 116, 70), size)
    shade(a, fbm(size, seed, 4, 32) * 20)
    shade(a, streaks(size, seed + 1, "h", count=200, amount=10))
    n = fbm(size, seed + 2, 3, 48)
    tint(a, np.clip(n * 2.5, 0, 1) * 0.6, (96, 140, 120), 0.55)   # verdigris
    tint(a, np.clip(-n * 2.5, 0, 1) * 0.4, (120, 60, 30), 0.5)    # dark tarnish
    return to_img(a)


# ---------------------------------------------------------------- concrete, grating, hazard, insulation

def concrete(seed, size=CELL, tie_holes=True, stains=True):
    """Poured concrete: layered mottling, aggregate speckle, pits, faint form-board lines, water stains and the
    four form-tie holes a cast panel has."""
    a = base((160, 160, 154), size)
    shade(a, fbm(size, seed, 5, 64) * 24)
    shade(a, grain(size, seed + 1, 14))
    r = _rng(seed + 2)
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for _ in range(120):
        x, y = r.integers(0, size), r.integers(0, size)
        s = r.integers(1, 3)
        c = [(140, 138, 132, 255), (150, 148, 142, 255), (176, 176, 172, 255), (128, 126, 122, 255)][int(r.integers(0, 4))]
        d.ellipse([int(x - s), int(y - s), int(x + s), int(y + s)], fill=c)
    for _ in range(18):                                              # pits
        x, y = int(r.integers(0, size)), int(r.integers(0, size))
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(104, 102, 98, 255))
        d.point((x - 1, y - 1), fill=(190, 190, 186, 255))
    d.line([(0, size // 2), (size, size // 2)], fill=(132, 130, 126, 255))   # form-board joint
    d.line([(0, size // 2 + 1), (size, size // 2 + 1)], fill=(184, 184, 180, 255))
    if tie_holes:
        for x, y in ((24, 24), (size - 24, 24), (24, size - 24), (size - 24, size - 24)):
            d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=(88, 88, 86, 255))
            d.ellipse([x - 4, y - 4, x + 2, y + 2], fill=(60, 60, 58, 255))
            d.arc([x - 5, y - 5, x + 5, y + 5], 200, 340, fill=(196, 196, 192, 255), width=1)
    a = np.asarray(img).astype(np.float32)
    if stains:
        shade(a, drips(size, seed + 3, count=5, amount=14))
        a = rust(a, drips(size, seed + 4, count=2, amount=1) * -0.6, seed + 5, 0.35)
    return to_img(a)


def grating(seed, size=CELL):
    """Steel bar grating with see-through gaps: bearing bars with a lit top edge, cross rods, and dirt in the corners."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pitch = 16
    for x in range(0, size, pitch):
        d.rectangle([x, 0, x + 4, size], fill=(74, 78, 84, 255))
        d.rectangle([x, 0, x + 1, size], fill=(140, 146, 154, 255))
        d.rectangle([x + 4, 0, x + 4, size], fill=(40, 42, 46, 255))
    for y in range(8, size, 32):
        d.rectangle([0, y, size, y + 2], fill=(110, 116, 124, 255))
        d.rectangle([0, y + 2, size, y + 3], fill=(50, 52, 56, 255))
    a = np.asarray(img).astype(np.float32)
    solid = a[..., 3] > 0
    shade(a, grain(size, seed, 10) * solid)
    a[..., 3] = np.where(solid, 255, 0)
    return to_img(a)


def hazard(seed, size=CELL):
    """Yellow and black safety striping, scuffed and faded."""
    img = Image.new("RGBA", (size, size), (228, 184, 32, 255))
    d = ImageDraw.Draw(img)
    for k in range(-size, size * 2, 32):
        d.polygon([(k, size), (k + 16, size), (k + 16 + size, 0), (k + size, 0)], fill=(28, 28, 30, 255))
    a = np.asarray(img).astype(np.float32)
    shade(a, fbm(size, seed, 3, 32) * 18)
    shade(a, grain(size, seed + 1, 10))
    a = chips(a, seed + 2, 10, under=(150, 150, 146), size=size, max_r=3)
    return to_img(a)


def insulation(seed, size=CELL):
    """Aluminium insulation jacket: dull sheet with soft dents, overlapping band joints and banding straps."""
    a = base((198, 202, 204), size)
    shade(a, fbm(size, seed, 4, 32) * 26)
    shade(a, streaks(size, seed + 1, "h", count=120, amount=8))
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for y in (0, size // 2):
        d.line([(0, y), (size, y)], fill=(120, 124, 126, 255), width=2)
        d.line([(0, y + 2), (size, y + 2)], fill=(232, 236, 238, 255))
        d.rectangle([0, y + 10, size, y + 14], fill=(170, 172, 174, 255))        # strap
        d.rectangle([0, y + 10, size, y + 10], fill=(214, 216, 218, 255))
        for x in range(10, size, 48):
            d.rectangle([x, y + 8, x + 8, y + 16], fill=(96, 98, 100, 255))      # strap seal
            d.rectangle([x + 2, y + 10, x + 6, y + 14], fill=(150, 152, 154, 255))
    return img


def radiator(seed, size=CELL):
    """Transformer radiator panels: vertical fins with deep dark gaps and streaks of rust down them."""
    a = base((108, 116, 120), size)
    shade(a, fbm(size, seed, 3, 32) * 10)
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for x in range(0, size, 8):
        d.rectangle([x, 0, x + 2, size], fill=(62, 68, 72, 255))
        d.rectangle([x + 3, 0, x + 3, size], fill=(150, 158, 162, 255))
    a = np.asarray(img).astype(np.float32)
    shade(a, drips(size, seed + 1, count=6, amount=12))
    a = rust(a, (drips(size, seed + 2, count=3, amount=1) < -0.3).astype(np.float32) * 0.6, seed + 3, 0.5)
    return to_img(a)


def fins(seed, size=CELL):
    """Motor cooling fins: horizontal ribs with highlights and shadow."""
    a = base((98, 104, 112), size)
    shade(a, fbm(size, seed, 3, 32) * 8)
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for y in range(0, size, 8):
        d.rectangle([0, y, size, y + 1], fill=(160, 166, 174, 255))
        d.rectangle([0, y + 4, size, y + 6], fill=(50, 54, 60, 255))
    return img


# ---------------------------------------------------------------- fittings and signs

def glaze(colour, seed, size=CELL):
    """Glazed porcelain: a smooth colour with a soft wrapping highlight and tiny crazing."""
    a = base(colour, size)
    shade(a, fbm(size, seed, 3, 32) * 10)
    yy, xx = np.mgrid[0:size, 0:size]
    shade(a, np.exp(-((xx - size * 0.3) / (size * 0.1)) ** 2) * 60)
    shade(a, -np.exp(-((xx - size * 0.85) / (size * 0.12)) ** 2) * 30)
    return to_img(a)


def glass(seed, size=CELL):
    a = base((110, 150, 170), size)
    yy, xx = np.mgrid[0:size, 0:size]
    shade(a, (1 - yy / size) * 40)
    shade(a, np.exp(-(((xx - yy * 0.7) - size * 0.3) / (size * 0.08)) ** 2) * 90)
    shade(a, np.exp(-(((xx - yy * 0.7) - size * 0.42) / (size * 0.03)) ** 2) * 50)
    shade(a, fbm(size, seed, 3, 32) * 8)
    img = to_img(a)
    ImageDraw.Draw(img).rectangle([0, 0, size - 1, size - 1], outline=(50, 54, 60, 255), width=4)
    return img


def screen(seed, size=CELL):
    """A plant display: dark glass with faint scanlines, a mimic diagram in cyan and a few amber readouts."""
    img = Image.new("RGBA", (size, size), (12, 20, 28, 255))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for y in range(0, size, 3):
        d.line([(0, y), (size, y)], fill=(16, 26, 34, 255))
    d.rectangle([0, 0, size - 1, size - 1], outline=(36, 46, 54, 255), width=3)
    # Mimic: boxes joined by lines.
    boxes = [(14, 20, 40, 44), (56, 14, 86, 36), (96, 30, 118, 60), (20, 70, 52, 96), (70, 66, 110, 92)]
    for x0, y0, x1, y1 in boxes:
        d.rectangle([x0, y0, x1, y1], outline=(70, 190, 220, 255), width=1)
        d.rectangle([x0 + 3, y0 + 3, x0 + 3 + r.randint(6, (x1 - x0) - 8), y0 + 6], fill=(70, 190, 220, 255))
    d.line([(40, 32), (56, 25)], fill=(70, 190, 220, 255))
    d.line([(86, 25), (96, 45)], fill=(70, 190, 220, 255))
    d.line([(36, 70), (36, 44)], fill=(70, 190, 220, 255))
    d.line([(90, 66), (107, 60)], fill=(70, 190, 220, 255))
    for i in range(4):
        x = 14 + i * 28
        d.rectangle([x, 106, x + 20, 118], fill=(30, 26, 14, 255), outline=(240, 180, 40, 255))
        d.rectangle([x + 3, 110, x + 3 + r.randint(4, 14), 114], fill=(240, 180, 40, 255))
    a = np.asarray(img).astype(np.float32)
    yy, xx = np.mgrid[0:size, 0:size]
    shade(a, np.exp(-(((xx + yy) - size * 0.5) / (size * 0.12)) ** 2) * 14)   # glass reflection
    return to_img(a)


def desk(seed, size=CELL):
    """A control desk top: grey laminate with a seam, rows of push buttons (lit) and a label strip."""
    img = painted((150, 156, 160), seed, seams=(("h", 18),), chip_count=3, drip_count=0, dirt=0.1)
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for row in range(2):
        for k in range(6):
            x, y = 10 + k * 20, 36 + row * 36
            d.rectangle([x, y, x + 13, y + 13], fill=(42, 44, 48, 255))
            d.rectangle([x + 1, y + 1, x + 12, y + 12], fill=(70, 72, 78, 255))
            c = r.choice([(220, 60, 50), (70, 200, 90), (240, 190, 40), (70, 160, 220)])
            d.rectangle([x + 4, y + 4, x + 9, y + 9], fill=c + (254,))
            d.rectangle([x + 1, y + 16, x + 12, y + 20], fill=(230, 232, 228, 255))
            d.line([(x + 2, y + 18), (x + 11, y + 18)], fill=(80, 80, 84, 255))
    return img


def rubber(seed, size=CELL, colour=(38, 40, 44)):
    a = base(colour, size)
    shade(a, fbm(size, seed, 4, 32) * 10)
    shade(a, grain(size, seed + 1, 6))
    yy, xx = np.mgrid[0:size, 0:size]
    shade(a, np.exp(-((yy - size * 0.22) / (size * 0.08)) ** 2) * 18)
    return to_img(a)


def lamp(colour, seed, size=CELL):
    """An indicator lamp lens: a domed glass with a hot core and a specular point (alpha 254 marks it as glowing)."""
    img = Image.new("RGBA", (size, size), tuple(int(c * 0.3) for c in colour) + (255,))
    d = ImageDraw.Draw(img)
    for k in range(10, 0, -1):
        f = k / 10
        c = tuple(int(colour[i] * (0.55 + 0.45 * (1 - f))) for i in range(3))
        d.ellipse([size / 2 - size * 0.42 * f, size / 2 - size * 0.42 * f, size / 2 + size * 0.42 * f, size / 2 + size * 0.42 * f], fill=c + (254,))
    d.ellipse([size * 0.3, size * 0.22, size * 0.46, size * 0.38], fill=(255, 255, 255, 254))
    return img


def trefoil_sign(seed, size=CELL):
    """The radiation warning sign: yellow enamel plate, black border, trefoil, and CAUTION lettering bars."""
    from nuclear import trefoil
    img = Image.new("RGBA", (size, size), (244, 200, 36, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, size - 4, size - 4], outline=(20, 20, 22, 255), width=4)
    trefoil(d, size // 2, size // 2 - 10, 38, ring=False, bg=(244, 200, 36, 255))
    for x in range(22, size - 22, 12):
        d.rectangle([x, size - 24, x + 8, size - 16], fill=(20, 20, 22, 255))
    a = np.asarray(img).astype(np.float32)
    shade(a, fbm(size, seed, 3, 32) * 10)
    return to_img(a)


def generator(seed, size=CELL):
    """The generator's enamelled frame: deep green with a lighter panel line and riveted edge."""
    return painted((58, 118, 88), seed, seams=(("v", 62),), bolts=[(x, 10) for x in range(10, size, 24)] + [(x, size - 10) for x in range(22, size, 24)],
                   chip_count=3, drip_count=2, dirt=0.3)


def handrail(seed, size=CELL):
    """Yellow safety railing on a transparent background: two rails, posts and a kick plate."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    y_ = (236, 190, 30, 255)
    dark = (170, 130, 20, 255)
    for y in (4, 60):
        d.rectangle([0, y, size, y + 9], fill=y_)
        d.rectangle([0, y + 7, size, y + 9], fill=dark)
    for x in (4, 60, 116):
        d.rectangle([x, 0, x + 8, size], fill=y_)
        d.rectangle([x + 6, 0, x + 8, size], fill=dark)
    d.rectangle([0, size - 12, size, size], fill=y_)
    d.rectangle([0, size - 3, size, size], fill=dark)
    a = np.asarray(img).astype(np.float32)
    solid = a[..., 3] > 0
    shade(a, grain(size, seed, 12) * solid)
    a[..., 3] = np.where(solid, 255, 0)
    return to_img(a)


def nameplate(seed, size=CELL):
    """An engraved stainless nameplate: brushed, with stamped text lines and four corner rivets."""
    img = brushed((214, 218, 220), seed, "h", dirt=0.1)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, size - 3, size - 3], outline=(90, 94, 98, 255), width=3)
    r = random.Random(seed)
    d.rectangle([16, 14, size - 16, 30], fill=(60, 62, 66, 255))
    for y in range(44, size - 16, 14):
        d.line([(16, y), (16 + r.randint(30, size - 40), y)], fill=(70, 72, 76, 255), width=4)
        d.line([(16, y + 4), (16 + r.randint(30, size - 40), y + 4)], fill=(236, 238, 240, 255), width=1)
    for x, y in ((8, 8), (size - 8, 8), (8, size - 8), (size - 8, size - 8)):
        bolt(d, x, y, 3, head=(200, 204, 208), hexagonal=False)
    return img


def pipe_stripe(colour, seed, size=CELL, arrows=True):
    """A painted pipe with a colour band and flow arrows."""
    img = painted(colour, seed, chip_count=5, drip_count=3, dirt=0.4)
    d = ImageDraw.Draw(img)
    if arrows:
        for x in (20, 84):
            d.polygon([(x, size // 2 - 12), (x + 22, size // 2), (x, size // 2 + 12)], fill=(240, 240, 236, 255))
    return img


def water(seed, size=CELL):
    a = base((40, 92, 150), size)
    shade(a, fbm(size, seed, 4, 32) * 30)
    n = value_noise(size, seed + 1, 16)
    tint(a, np.clip(n * 3, 0, 1), (120, 190, 230), 0.6)
    return to_img(a)


def tank_grey(seed, size=CELL):
    return painted((126, 134, 136), seed, seams=(("h", 32), ("h", 96)),
                   bolts=[(x, 32) for x in range(8, size, 16)] + [(x, 96) for x in range(16, size, 16)], chip_count=4, drip_count=4, dirt=0.6)


def turbine_blue(seed, size=CELL):
    return painted((70, 110, 140), seed, seams=(("h", 62),), bolts=[(x, 62) for x in range(8, size, 16)], chip_count=3, drip_count=3, dirt=0.4)


# ---------------------------------------------------------------- the hazmat suit

def fabric(colour, seed, size=CELL):
    """Coated PVC fabric: a visible weave under a glossy coat, soft creases, a taped seam with stitches."""
    colour = tuple(colour[:3])
    a = base(colour, size)
    yy, xx = np.mgrid[0:size, 0:size]
    weave = ((xx // 2 + yy // 2) % 2) * 4 - 2 + ((xx + yy) % 3 == 0) * 2
    shade(a, weave)
    shade(a, fbm(size, seed, 4, 64) * 16)
    crease = np.sin((xx * 0.3 + yy * 0.8) / 9.0 + fbm(size, seed + 1, 2, 48) * 5)
    shade(a, (np.clip(crease, 0.8, 1) - 0.8) * 70)
    shade(a, -(np.clip(-crease, 0.9, 1) - 0.9) * 90)
    shade(a, drips(size, seed + 2, count=3, amount=10))
    img = to_img(a)
    d = ImageDraw.Draw(img)
    dark = tuple(max(0, c - 40) for c in colour)
    d.rectangle([0, size - 14, size, size - 6], fill=tuple(max(0, c - 22) for c in colour) + (255,))
    d.line([(0, size - 15), (size, size - 15)], fill=dark + (255,))
    d.line([(0, size - 5), (size, size - 5)], fill=dark + (255,))
    for x in range(3, size, 7):
        d.line([(x, size - 12), (x + 3, size - 8)], fill=dark + (255,), width=1)
    return img


def tape(seed, size=CELL):
    a = base((200, 202, 200), size)
    shade(a, fbm(size, seed, 3, 32) * 10)
    shade(a, streaks(size, seed + 1, "h", count=100, amount=6))
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for y in range(0, size, 42):
        d.line([(0, y), (size, y + 6)], fill=(150, 152, 150, 255), width=2)
        d.line([(0, y + 2), (size, y + 8)], fill=(226, 228, 226, 255), width=1)
    return img


def visor(seed, size=CELL):
    a = base((80, 120, 140), size)
    yy, xx = np.mgrid[0:size, 0:size]
    shade(a, (1 - yy / size) * 60)
    shade(a, np.exp(-(((xx - yy * 0.8) - size * 0.22) / (size * 0.06)) ** 2) * 110)
    shade(a, np.exp(-(((xx - yy * 0.8) - size * 0.4) / (size * 0.03)) ** 2) * 60)
    shade(a, fbm(size, seed, 3, 32) * 10)
    return to_img(a)


def tank(seed, size=CELL):
    """Painted aluminium air cylinder, plain so it wraps cleanly; the blue band is its own material."""
    a = base((202, 206, 210), size)
    shade(a, fbm(size, seed, 3, 32) * 10)
    shade(a, streaks(size, seed + 1, "v", count=80, amount=6))
    return to_img(a)


def filter_face(seed, size=CELL):
    """A respirator filter's face: perforated dark plastic with a ring."""
    img = rubber(seed, size, (70, 72, 78))
    d = ImageDraw.Draw(img)
    for y in range(8, size, 14):
        for x in range(8 + (y // 14 % 2) * 7, size, 14):
            d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(22, 22, 24, 255))
            d.arc([x - 4, y - 4, x + 4, y + 4], 200, 340, fill=(120, 122, 128, 255))
    d.ellipse([4, 4, size - 5, size - 5], outline=(190, 194, 200, 255), width=5)
    return img


def badge(seed, size=CELL):
    img = Image.new("RGBA", (size, size), (232, 232, 228, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, size - 1, size - 1], outline=(150, 150, 146, 255), width=5)
    d.rectangle([16, 20, 80, 60], fill=(40, 60, 50, 255))
    d.rectangle([22, 26, 74, 54], fill=(90, 220, 120, 254))
    d.rectangle([16, 76, 112, 84], fill=(60, 60, 64, 255))
    d.rectangle([16, 94, 88, 100], fill=(110, 110, 114, 255))
    d.ellipse([92, 20, 112, 40], fill=(200, 50, 40, 255))
    return img


def sole(seed, size=CELL):
    img = rubber(seed, size, (30, 30, 32))
    d = ImageDraw.Draw(img)
    for y in range(6, size, 16):
        d.rectangle([4, y, size - 5, y + 6], fill=(16, 16, 18, 255))
        d.rectangle([4, y, size - 5, y], fill=(60, 60, 64, 255))
    return img


def zip_tex(seed, size=CELL):
    img = rubber(seed, size, (60, 62, 66))
    d = ImageDraw.Draw(img)
    for y in range(0, size, 8):
        d.rectangle([20, y, 60, y + 3], fill=(200, 204, 210, 255))
        d.rectangle([68, y + 4, 108, y + 7], fill=(200, 204, 210, 255))
    return img


def tank_band(seed, size=CELL):
    return painted((44, 112, 190), seed, chip_count=3, drip_count=0, dirt=0.1)


# ---------------------------------------------------------------- the plant buildings' equipment (decor pieces)

def mimic(seed, size=CELL):
    """A control board's vertical section: grey enamel with a coloured mimic of the plant's flow paths (red reactor
    coolant, orange steam, blue feedwater), round meters, rows of lit indicators and a label strip along the top."""
    img = painted((176, 182, 178), seed, seams=(("h", 22),), chip_count=1, drip_count=0, dirt=0.1, size=size)
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    d.rectangle([4, 4, size - 5, 16], fill=(236, 238, 232, 255))
    for x in range(10, size - 12, 14):
        d.line([(x, 10), (x + 8, 10)], fill=(60, 62, 66, 255), width=2)
    # Flow paths.
    d.line([(10, 44), (60, 44), (60, 70), (100, 70)], fill=(200, 50, 40, 255), width=3)
    d.line([(20, 56), (90, 56), (90, 34), (118, 34)], fill=(230, 140, 40, 255), width=3)
    d.line([(10, 84), (70, 84), (70, 100), (118, 100)], fill=(50, 110, 200, 255), width=3)
    for x, y in ((60, 44), (90, 56), (70, 84)):
        d.rectangle([x - 4, y - 4, x + 4, y + 4], fill=(30, 32, 36, 255), outline=(230, 232, 228, 255))
    # Meters.
    for x in (22, 48):
        d.ellipse([x - 9, 22 + 4, x + 9, 22 + 22], fill=(240, 240, 232, 255), outline=(40, 40, 44, 255), width=2)
        d.line([(x, 35), (x + r.randint(-6, 6), 28)], fill=(200, 40, 30, 255), width=2)
    # Indicator rows (glowing).
    for row in range(2):
        for k in range(9):
            x, y = 8 + k * 13, 108 + row * 9
            c = r.choice([(80, 220, 110), (80, 220, 110), (230, 60, 50), (245, 190, 40), (240, 240, 230)])
            d.rectangle([x, y, x + 8, y + 5], fill=c + (254,))
    return img


def display(seed, size=CELL):
    """A large plant overview display: the unit's one-line diagram (reactor, steam generators, turbine, condenser) in
    bright lines on dark blue, a trend graph below and a strip of status tiles."""
    img = Image.new("RGBA", (size, size), (10, 22, 40, 254))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    g = (110, 230, 140, 254)
    c = (90, 200, 240, 254)
    a = (250, 190, 50, 254)
    for y in range(0, size, 4):
        d.line([(0, y), (size, y)], fill=(14, 28, 48, 254))
    d.ellipse([10, 14, 34, 52], outline=c, width=2)                 # vessel
    d.rectangle([44, 12, 58, 52], outline=c, width=2)               # steam generator
    d.line([(34, 22), (44, 22)], fill=(240, 80, 60, 254), width=2)
    d.line([(34, 44), (44, 44)], fill=c, width=2)
    d.polygon([(70, 22), (100, 14), (100, 44), (70, 36)], outline=g)  # turbine
    d.line([(58, 18), (70, 26)], fill=a, width=2)
    d.rectangle([104, 22, 120, 40], outline=g, width=2)             # generator
    d.rectangle([70, 46, 98, 56], outline=c, width=2)               # condenser
    d.line([(84, 46), (84, 40)], fill=c, width=1)
    d.text((12, 3), "UNIT 1", fill=(220, 230, 240, 254))
    # Trend graph.
    d.rectangle([8, 64, size - 9, 100], outline=(60, 80, 110, 254))
    pts, y = [], 84.0
    for x in range(10, size - 10, 3):
        y = max(68, min(96, y + r.uniform(-2.5, 2.5)))
        pts.append((x, y))
    d.line(pts, fill=a, width=2)
    d.line([(10, 76), (size - 10, 76)], fill=(70, 100, 140, 254))
    # Status tiles.
    for k in range(6):
        x = 8 + k * 19
        d.rectangle([x, 108, x + 15, 120], fill=(r.choice([(40, 140, 70), (40, 140, 70), (40, 140, 70), (170, 120, 30)])) + (254,))
    return img


def cherenkov(seed, size=CELL):
    """Water glowing blue over spent fuel: a bright cyan-blue core fading to deep blue, rippled."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    ripple = fbm(size, seed, 3, 24) * 0.25
    t = np.clip(0.55 + ripple + 0.15 * np.sin(xx / 7.0) * np.sin(yy / 9.0), 0, 1)
    a = np.zeros((size, size, 4), np.float32)
    a[..., 0] = 40 + 90 * t
    a[..., 1] = 120 + 110 * t
    a[..., 2] = 230 + 25 * t
    a[..., 3] = 254
    return to_img(a)


def fuel_rack(seed, size=CELL):
    """The top of a spent fuel rack: a stainless grid of square cells, most holding an assembly's top nozzle (the
    four corner posts and the hold-down springs), seen through glowing blue water."""
    img = Image.new("RGBA", (size, size), (70, 140, 220, 254))
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    n = 4
    w = size // n
    for i in range(n):
        for j in range(n):
            x, y = i * w, j * w
            d.rectangle([x, y, x + w - 1, y + w - 1], outline=(200, 210, 220, 254), width=3)
            if r.random() < 0.85:
                d.rectangle([x + 6, y + 6, x + w - 7, y + w - 7], fill=(150, 175, 200, 254), outline=(220, 230, 240, 254))
                for px, py in ((x + 8, y + 8), (x + w - 12, y + 8), (x + 8, y + w - 12), (x + w - 12, y + w - 12)):
                    d.rectangle([px, py, px + 3, py + 3], fill=(235, 240, 245, 254))
                d.rectangle([x + w // 2 - 4, y + w // 2 - 4, x + w // 2 + 4, y + w // 2 + 4], fill=(90, 150, 220, 254))
    return img


def louvre(seed, size=CELL):
    """Air intake louvres: angled slats in a painted frame, dark behind."""
    a = base((40, 44, 48), size)
    img = to_img(a)
    d = ImageDraw.Draw(img)
    for y in range(6, size - 6, 10):
        d.polygon([(6, y), (size - 7, y), (size - 7, y + 6), (6, y + 6)], fill=(170, 174, 172, 255))
        d.line([(6, y), (size - 7, y)], fill=(210, 214, 212, 255), width=2)
        d.line([(6, y + 6), (size - 7, y + 6)], fill=(90, 94, 96, 255), width=1)
    d.rectangle([0, 0, size - 1, size - 1], outline=(150, 154, 152, 255), width=6)
    return img


def switchgear(seed, size=CELL):
    """Medium-voltage switchgear: a cabinet door with a meter window, breaker position lamps, a control switch and a
    handle; a nameplate at the top."""
    img = painted((150, 158, 160), seed, seams=(("h", 64),), chip_count=1, drip_count=0, dirt=0.15, size=size)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, size - 3, size - 3], outline=(80, 86, 90, 255), width=3)
    d.rectangle([20, 8, 108, 20], fill=(30, 32, 36, 255))
    d.line([(26, 14), (90, 14)], fill=(230, 230, 230, 255), width=2)
    d.rectangle([24, 28, 60, 52], fill=(240, 240, 230, 255), outline=(40, 42, 46, 255), width=2)
    d.line([(42, 50), (52, 34)], fill=(30, 30, 30, 255), width=2)
    d.ellipse([72, 30, 84, 42], fill=(230, 60, 50, 254))
    d.ellipse([90, 30, 102, 42], fill=(80, 220, 110, 254))
    d.ellipse([78, 72, 98, 92], fill=(40, 42, 46, 255))
    d.rectangle([86, 70, 90, 94], fill=(200, 60, 40, 255))
    d.rectangle([12, 76, 18, 112], fill=(60, 62, 66, 255))
    for y in range(98, 122, 6):
        d.line([(30, y), (70, y)], fill=(100, 106, 110, 255), width=2)
    return img


def filter_bank(seed, size=CELL):
    """An air handler's access side: two hinged doors with latches and a pressure gauge."""
    img = painted((196, 200, 196), seed, seams=(("v", 64),), chip_count=1, drip_count=1, dirt=0.2, size=size)
    d = ImageDraw.Draw(img)
    for x0 in (6, 70):
        d.rectangle([x0, 8, x0 + 52, size - 9], outline=(110, 114, 112, 255), width=3)
        d.rectangle([x0 + 40, 54, x0 + 46, 74], fill=(50, 52, 56, 255))
    d.ellipse([22, 20, 40, 38], fill=(240, 240, 232, 255), outline=(40, 40, 44, 255), width=2)
    d.line([(31, 29), (36, 23)], fill=(200, 40, 30, 255), width=2)
    return img
