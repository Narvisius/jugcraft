"""Original pictures for the plant's offices: landscapes, the sea, cities, the plant itself, abstracts.

Each scene is painted in layers like a matte painting: a sky gradient with a soft sun or moon and clouds, ridges of
hills or mountains that fade into the haze with distance, water that mirrors what is above it with ripples, then
the foreground (trees, a lighthouse, boats, fields). Everything is drawn here from noise and shapes; no outside
images.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter


def _grad(w, h, stops):
    """A vertical gradient through (position 0..1, colour) stops."""
    t = np.linspace(0, 1, h)[:, None]
    out = np.zeros((h, w, 3), np.float32)
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        m = (t >= p0) & (t <= p1)
        k = np.clip((t - p0) / max(1e-6, p1 - p0), 0, 1)
        col = np.array(c0, np.float32) * (1 - k) + np.array(c1, np.float32) * k
        out = np.where(m[..., None], np.broadcast_to(col[:, None, :], (h, w, 3)), out)
    return out


def _noise1(w, seed, octaves=5, base=0.5):
    r = np.random.default_rng(seed)
    x = np.linspace(0, 1, w)
    out = np.zeros(w)
    amp, freq = 1.0, 2
    for _ in range(octaves):
        pts = r.random(freq + 1)
        out += amp * np.interp(x * freq, np.arange(freq + 1), pts)
        amp *= base
        freq *= 2
    return (out - out.min()) / max(1e-6, out.max() - out.min())


def _noise2(w, h, seed, scale=8, octaves=4):
    r = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    amp = 1.0
    for o in range(octaves):
        gw, gh = max(2, w // scale * 2 ** o // 4 + 2), max(2, h // scale * 2 ** o // 4 + 2)
        g = Image.fromarray((r.random((gh, gw)) * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(g, np.float32) / 255
        amp *= 0.5
    return (out - out.min()) / max(1e-6, out.max() - out.min())


def _ridge(a, top, amp, colour, haze, seed, rough=0.5, sharp=False):
    """Fill everything below a noisy skyline (top +- amp, as fractions of the height) with colour, blended toward
    the haze colour, darker toward the bottom."""
    h, w, _ = a.shape
    n = _noise1(w, seed, 6, rough)
    if sharp:
        n = 1 - np.abs(n * 2 - 1)
    line = (top + amp * (0.5 - n)) * h
    yy = np.arange(h)[:, None]
    mask = yy >= line[None, :]
    depth = np.clip((yy - line[None, :]) / (h * 0.5), 0, 1)
    col = np.array(colour, np.float32) * (1 - 0.35 * depth[..., None])
    col = col * (1 - haze) + np.array(a[int(h * 0.05), w // 2], np.float32) * haze
    a[mask] = col[mask]
    return line


def _sun(a, cx, cy, r, colour, glow=3.0):
    h, w, _ = a.shape
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.hypot(xx - cx * w, yy - cy * h)
    halo = np.exp(-(d / (r * w * glow)) ** 2)[..., None] * 0.6
    a[:] = a * (1 - halo) + np.array(colour, np.float32) * halo
    a[d < r * w] = colour


def _clouds(a, y0, y1, colour, seed, amount=0.6):
    h, w, _ = a.shape
    n = _noise2(w, h, seed, scale=6)
    n = np.clip((n - 0.5) * 3, 0, 1)
    band = np.zeros((h, 1), np.float32)
    band[int(y0 * h):int(y1 * h)] = 1
    band = np.asarray(Image.fromarray((band[:, 0] * 255).astype(np.uint8)[:, None].repeat(4, 1)).filter(
        ImageFilter.GaussianBlur(h * 0.05)), np.float32)[:, :1] / 255
    m = (n * band * amount)[..., None]
    a[:] = a * (1 - m) + np.array(colour, np.float32) * m


def _water(a, level, tint, seed, ripple=True):
    """Everything below level becomes water mirroring the picture above it, tinted and rippled."""
    h, w, _ = a.shape
    y0 = int(level * h)
    src = a[:y0][::-1].copy()
    n = min(h - y0, y0)
    refl = src[:n]
    r = random.Random(seed)
    if ripple:
        for y in range(n):
            shift = int(math.sin(y * 0.9 + r.random()) * (1 + y * 0.04))
            refl[y] = np.roll(refl[y], shift, axis=0)
    refl = refl * 0.7 + np.array(tint, np.float32) * 0.3
    a[y0:y0 + n] = refl
    if h - y0 > n:
        a[y0 + n:] = np.array(tint, np.float32)
    for _ in range(int(w * 0.6)):
        y = r.randint(y0 + 1, h - 1)
        x = r.randint(0, w - 1)
        L = r.randint(2, 6 + (y - y0) // 3)
        a[y, x:x + L] = np.clip(a[y, x:x + L] * 1.18 + 10, 0, 255)


def _pines(d, w, h, base, count, height, colour, seed, spread=(0, 1)):
    r = random.Random(seed)
    for _ in range(count):
        x = r.uniform(spread[0], spread[1]) * w
        th = height * h * r.uniform(0.6, 1.3)
        y = base * h + r.uniform(-0.02, 0.03) * h
        tw = th * 0.32
        c = tuple(int(v * r.uniform(0.8, 1.1)) for v in colour)
        for k in range(4):
            yy = y - th * k / 4.4
            d.polygon([(x - tw * (1 - k / 5), yy), (x, yy - th * 0.42), (x + tw * (1 - k / 5), yy)], fill=c)
        d.line([(x, y), (x, y + th * 0.08)], fill=(60, 40, 30), width=max(1, int(w / 200)))


def _broadleaf(d, w, h, x, y, size, colour, seed):
    r = random.Random(seed)
    d.line([(x * w, y * h), (x * w, (y - size * 0.5) * h)], fill=(70, 50, 35), width=max(2, int(size * w * 0.06)))
    for _ in range(30):
        cx = x * w + r.gauss(0, size * w * 0.18)
        cy = (y - size * 0.6) * h + r.gauss(0, size * h * 0.16)
        rr = size * w * r.uniform(0.05, 0.11)
        c = tuple(int(v * r.uniform(0.75, 1.15)) for v in colour)
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=c)


def scene(kind, w, h, seed):
    """One picture, w x h pixels (RGB uint8)."""
    r = random.Random(seed)
    k = kind % 16
    if k == 0:    # mountain lake at sunset
        a = _grad(w, h, [(0, (60, 70, 130)), (0.35, (230, 120, 90)), (0.55, (250, 200, 130)), (1, (250, 200, 130))])
        _sun(a, 0.62, 0.45, 0.05, (255, 230, 170))
        _clouds(a, 0.1, 0.35, (240, 150, 140), seed)
        _ridge(a, 0.42, 0.25, (90, 70, 110), 0.55, seed + 1, sharp=True)
        _ridge(a, 0.52, 0.12, (50, 40, 70), 0.25, seed + 2)
        _water(a, 0.6, (60, 60, 100), seed)
    elif k == 1:  # snowy peaks under a blue sky
        a = _grad(w, h, [(0, (70, 120, 200)), (0.6, (180, 210, 235)), (1, (180, 210, 235))])
        _clouds(a, 0.05, 0.3, (250, 250, 252), seed, 0.7)
        line = _ridge(a, 0.45, 0.4, (235, 238, 245), 0.15, seed + 1, sharp=True)
        _ridge(a, 0.6, 0.2, (90, 100, 120), 0.2, seed + 2, sharp=True)
        _ridge(a, 0.78, 0.1, (40, 70, 50), 0.05, seed + 3)
        img = Image.fromarray(a.astype(np.uint8))
        _pines(ImageDraw.Draw(img), w, h, 0.95, 18, 0.18, (25, 55, 35), seed)
        return np.asarray(img)
    elif k == 2:  # rolling green hills, patchwork fields
        a = _grad(w, h, [(0, (110, 170, 230)), (0.5, (210, 230, 245)), (1, (210, 230, 245))])
        _clouds(a, 0.05, 0.35, (255, 255, 255), seed, 0.8)
        _ridge(a, 0.45, 0.1, (110, 150, 90), 0.4, seed + 1, 0.4)
        _ridge(a, 0.55, 0.12, (90, 150, 70), 0.2, seed + 2, 0.4)
        _ridge(a, 0.7, 0.14, (120, 170, 60), 0.0, seed + 3, 0.4)
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        for i in range(6):
            y = h * (0.75 + i * 0.04)
            d.line([(0, y + r.uniform(-3, 3)), (w, y + r.uniform(-6, 6))], fill=(150, 180, 70), width=max(1, h // 60))
        _broadleaf(d, w, h, 0.72, 0.74, 0.25, (50, 110, 40), seed)
        return np.asarray(img)
    elif k == 3:  # cliffs, sea and a lighthouse
        a = _grad(w, h, [(0, (120, 170, 220)), (0.5, (220, 230, 240)), (1, (220, 230, 240))])
        _clouds(a, 0.05, 0.3, (250, 250, 252), seed)
        _water(a, 0.55, (40, 100, 150), seed, ripple=True)
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        d.polygon([(0, h * 0.42), (w * 0.35, h * 0.45), (w * 0.45, h), (0, h)], fill=(110, 100, 80))
        d.polygon([(0, h * 0.42), (w * 0.35, h * 0.45), (w * 0.33, h * 0.5), (0, h * 0.48)], fill=(90, 140, 70))
        lx = w * 0.2
        d.polygon([(lx - w * 0.025, h * 0.43), (lx - w * 0.015, h * 0.2), (lx + w * 0.015, h * 0.2), (lx + w * 0.025, h * 0.43)], fill=(240, 240, 236))
        for y in (0.27, 0.35):
            d.rectangle([lx - w * 0.022, h * y, lx + w * 0.022, h * (y + 0.035)], fill=(200, 40, 40))
        d.rectangle([lx - w * 0.02, h * 0.15, lx + w * 0.02, h * 0.2], fill=(250, 220, 120))
        d.polygon([(lx - w * 0.025, h * 0.15), (lx, h * 0.11), (lx + w * 0.025, h * 0.15)], fill=(60, 60, 64))
        for x in range(int(w * 0.45), w, max(4, w // 20)):
            d.line([(x, h * 0.62 + r.uniform(0, h * 0.3)), (x + w * 0.04, h * 0.62 + r.uniform(0, h * 0.3))], fill=(230, 240, 245))
        return np.asarray(img)
    elif k == 4:  # aurora over a pine forest and a lake
        a = _grad(w, h, [(0, (8, 12, 30)), (0.6, (20, 40, 70)), (1, (20, 40, 70))])
        rr = np.random.default_rng(seed)
        for _ in range(int(w * h * 0.002)):
            a[rr.integers(0, int(h * 0.6)), rr.integers(0, w)] = (230, 235, 255)
        n = _noise1(w, seed, 5)
        yy = np.arange(h)[:, None]
        for band, col in ((0.18, (60, 230, 150)), (0.28, (120, 120, 230))):
            c = (band + 0.12 * (n - 0.5))[None, :] * h
            g = np.exp(-((yy - c) / (h * 0.06)) ** 2)[..., None] * 0.8
            a[:] = a * (1 - g) + np.array(col, np.float32) * g
        _ridge(a, 0.58, 0.05, (10, 18, 25), 0.0, seed + 1)
        _water(a, 0.66, (10, 20, 40), seed)
        img = Image.fromarray(a.astype(np.uint8))
        _pines(ImageDraw.Draw(img), w, h, 0.66, 40, 0.2, (8, 16, 18), seed)
        return np.asarray(img)
    elif k == 5:  # desert dunes in evening light
        a = _grad(w, h, [(0, (230, 170, 120)), (0.45, (250, 220, 170)), (1, (250, 220, 170))])
        _sun(a, 0.25, 0.32, 0.04, (255, 245, 210))
        _ridge(a, 0.5, 0.12, (220, 150, 90), 0.3, seed + 1, 0.35)
        _ridge(a, 0.62, 0.16, (200, 120, 70), 0.15, seed + 2, 0.35)
        _ridge(a, 0.78, 0.18, (170, 95, 55), 0.0, seed + 3, 0.35)
    elif k == 6:  # a city skyline at dusk over a river
        a = _grad(w, h, [(0, (30, 30, 70)), (0.4, (180, 90, 110)), (0.6, (240, 170, 110)), (1, (240, 170, 110))])
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        x = 0
        while x < w:
            bw = r.randint(max(4, w // 30), max(6, w // 12))
            bh = r.uniform(0.15, 0.45) * h
            d.rectangle([x, h * 0.6 - bh, x + bw, h * 0.6], fill=(25, 25, 40))
            for wy in range(int(h * 0.6 - bh + 3), int(h * 0.6) - 2, max(3, h // 40)):
                for wx in range(x + 2, x + bw - 2, max(3, w // 80)):
                    if r.random() < 0.35:
                        d.point((wx, wy), fill=(250, 220, 140))
            x += bw + r.randint(0, 3)
        a = np.asarray(img).astype(np.float32)
        _water(a, 0.6, (30, 30, 60), seed)
    elif k == 7:  # the plant's cooling towers at dawn, steam rising
        a = _grad(w, h, [(0, (90, 110, 160)), (0.5, (240, 180, 140)), (0.7, (250, 220, 180)), (1, (250, 220, 180))])
        _sun(a, 0.7, 0.62, 0.05, (255, 235, 190))
        _ridge(a, 0.66, 0.05, (90, 100, 110), 0.5, seed + 1)
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        for cx, tw in ((0.3, 0.12), (0.5, 0.1)):
            x0 = cx * w
            d.polygon([(x0 - tw * w, h * 0.8), (x0 - tw * w * 0.6, h * 0.5), (x0 - tw * w * 0.7, h * 0.4),
                       (x0 + tw * w * 0.7, h * 0.4), (x0 + tw * w * 0.6, h * 0.5), (x0 + tw * w, h * 0.8)], fill=(150, 145, 150))
            d.polygon([(x0 - tw * w * 0.4, h * 0.42), (x0 - tw * w * 0.25, h * 0.8), (x0 - tw * w * 0.15, h * 0.42)], fill=(170, 165, 170))
            for k2 in range(8):
                rr2 = tw * w * (0.7 + k2 * 0.18)
                cy = h * (0.36 - k2 * 0.045)
                d.ellipse([x0 - rr2 + k2 * 3, cy - rr2 * 0.4, x0 + rr2 + k2 * 3, cy + rr2 * 0.4], fill=(245, 236, 228))
        d.rectangle([0, h * 0.8, w, h], fill=(70, 90, 60))
        return np.asarray(img.filter(ImageFilter.SMOOTH))
    elif k == 8:  # an autumn forest path
        a = _grad(w, h, [(0, (190, 210, 220)), (1, (220, 200, 160))])
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        d.polygon([(w * 0.45, h * 0.5), (w * 0.55, h * 0.5), (w * 0.8, h), (w * 0.2, h)], fill=(170, 140, 100))
        for i in range(14):
            x = r.uniform(0, 1)
            col = r.choice([(200, 80, 30), (230, 150, 40), (180, 50, 30), (210, 180, 60)])
            _broadleaf(d, w, h, x, r.uniform(0.75, 1.0), r.uniform(0.35, 0.55), col, seed + i)
        return np.asarray(img)
    elif k == 9:  # a sailboat on open water
        a = _grad(w, h, [(0, (90, 150, 210)), (0.55, (200, 225, 240)), (1, (200, 225, 240))])
        _clouds(a, 0.1, 0.4, (255, 255, 255), seed, 0.7)
        _water(a, 0.58, (30, 90, 140), seed)
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        bx, by = w * 0.45, h * 0.62
        d.polygon([(bx - w * 0.08, by), (bx + w * 0.08, by), (bx + w * 0.06, by + h * 0.05), (bx - w * 0.06, by + h * 0.05)], fill=(240, 240, 236))
        d.line([(bx, by), (bx, by - h * 0.35)], fill=(80, 60, 40), width=max(1, w // 150))
        d.polygon([(bx + 2, by - h * 0.33), (bx + 2, by - 2), (bx + w * 0.09, by - 2)], fill=(250, 250, 246))
        d.polygon([(bx - 2, by - h * 0.28), (bx - 2, by - 4), (bx - w * 0.06, by - 4)], fill=(236, 236, 230))
        return np.asarray(img)
    elif k == 10:  # a tulip field with a windmill
        a = _grad(w, h, [(0, (120, 180, 235)), (0.5, (220, 235, 245)), (1, (220, 235, 245))])
        _clouds(a, 0.05, 0.3, (255, 255, 255), seed)
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        for i, col in enumerate([(220, 40, 60), (250, 200, 40), (240, 120, 160), (230, 90, 40), (160, 60, 160)]):
            y0 = h * (0.55 + i * 0.09)
            d.polygon([(0, y0), (w, y0 - h * 0.02), (w, y0 + h * 0.09), (0, y0 + h * 0.1)], fill=col)
            d.line([(0, y0 + h * 0.095), (w, y0 + h * 0.08)], fill=(60, 120, 40), width=max(1, h // 50))
        mx, my = w * 0.75, h * 0.55
        d.polygon([(mx - w * 0.03, my), (mx - w * 0.02, my - h * 0.2), (mx + w * 0.02, my - h * 0.2), (mx + w * 0.03, my)], fill=(110, 80, 60))
        for ang in (20, 110, 200, 290):
            t = math.radians(ang)
            d.line([(mx, my - h * 0.2), (mx + math.cos(t) * w * 0.1, my - h * 0.2 + math.sin(t) * h * 0.15)], fill=(240, 236, 220), width=max(2, w // 60))
        return np.asarray(img)
    elif k == 11:  # a full moon over a still lake
        a = _grad(w, h, [(0, (10, 15, 40)), (0.6, (40, 55, 90)), (1, (40, 55, 90))])
        _sun(a, 0.4, 0.25, 0.05, (245, 240, 220), 4)
        _ridge(a, 0.55, 0.1, (20, 25, 40), 0.1, seed + 1)
        _water(a, 0.62, (15, 25, 50), seed)
    elif k == 12:  # colour field abstract
        a = _grad(w, h, [(0, (200, 70, 50)), (0.45, (230, 120, 60)), (0.5, (40, 40, 60)), (1, (30, 60, 110))])
        n = _noise2(w, h, seed, 4)[..., None]
        a = a * (0.85 + 0.3 * n)
    elif k == 13:  # a waterfall in a mossy gorge
        a = _grad(w, h, [(0, (180, 210, 220)), (1, (60, 90, 60))])
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        d.polygon([(0, 0), (w * 0.38, 0), (w * 0.42, h), (0, h)], fill=(60, 80, 55))
        d.polygon([(w, 0), (w * 0.62, 0), (w * 0.58, h), (w, h)], fill=(55, 75, 50))
        for i in range(int(w * 0.2)):
            x = w * 0.4 + r.uniform(0, w * 0.2)
            d.line([(x, h * 0.1), (x + r.uniform(-2, 2), h * 0.8)], fill=(230, 240, 245) if r.random() < 0.6 else (190, 210, 220))
        d.ellipse([w * 0.3, h * 0.75, w * 0.7, h * 0.95], fill=(220, 235, 240))
        d.rectangle([0, h * 0.88, w, h], fill=(50, 90, 90))
        return np.asarray(img.filter(ImageFilter.SMOOTH_MORE))
    elif k == 14:  # a lone tree in a wheat field
        a = _grad(w, h, [(0, (90, 140, 210)), (0.55, (230, 220, 200)), (1, (230, 220, 200))])
        _sun(a, 0.8, 0.3, 0.035, (255, 250, 230))
        _ridge(a, 0.58, 0.04, (210, 180, 90), 0.1, seed + 1)
        img = Image.fromarray(a.astype(np.uint8))
        d = ImageDraw.Draw(img)
        for _ in range(int(w * 1.2)):
            x = r.uniform(0, w)
            y = r.uniform(h * 0.6, h)
            d.line([(x, y), (x + r.uniform(-1, 1), y - h * 0.03)], fill=(220, 190, 100) if r.random() < 0.6 else (190, 150, 70))
        _broadleaf(d, w, h, 0.35, 0.62, 0.45, (60, 100, 40), seed)
        return np.asarray(img)
    else:          # geometric abstract
        img = Image.new("RGB", (w, h), (238, 232, 220))
        d = ImageDraw.Draw(img)
        for _ in range(9):
            col = r.choice([(30, 60, 120), (210, 70, 50), (240, 190, 60), (30, 30, 34), (90, 150, 130), (230, 140, 120)])
            if r.random() < 0.5:
                cx, cy, rr = r.uniform(0, w), r.uniform(0, h), r.uniform(0.08, 0.25) * h
                d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=col)
            else:
                x0, y0 = r.uniform(0, w), r.uniform(0, h)
                d.rectangle([x0, y0, x0 + r.uniform(0.1, 0.4) * w, y0 + r.uniform(0.1, 0.4) * h], fill=col)
        for _ in range(3):
            d.line([(r.uniform(0, w), 0), (r.uniform(0, w), h)], fill=(20, 20, 22), width=max(1, w // 120))
        return np.asarray(img)
    return np.clip(a, 0, 255).astype(np.uint8)


def painted(kind, w, h, seed):
    """The scene with a little canvas texture and soft brushwork."""
    img = Image.fromarray(scene(kind, w * 2, h * 2, seed)).resize((w, h), Image.LANCZOS)
    a = np.asarray(img).astype(np.float32)
    g = np.random.default_rng(seed).normal(0, 3.5, a.shape[:2])[..., None]
    return Image.fromarray(np.clip(a + g, 0, 255).astype(np.uint8))
