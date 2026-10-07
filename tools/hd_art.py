"""High-detail art (64x64): a small shaded-shape renderer, which drew the items that once needed more than 16x16 pixels.

Retired for item icons: a new item icon is 16x16, drawn from a map (docs/ITEM_ICONS.md), because this renderer dithers
and paints wear. The 64x64 icons it draws today stay until each is redrawn; it may still paint 3D model textures.

Items are built from shapes painted in order, each with a material:
- `capsule` (a rod or tube between two points), `box` (a rotated, bevelled block), `disc` (a dome, bolt head or gauge),
  `ring`, `polygon` (flat) and `blob` (a lumpy mass, e.g. foam).

Every shape gives each pixel a surface normal: a tube is round across its width, a box is flat with bevelled edges, a
disc is a dome. The pixel is lit from the top left (the light every Jugcraft item uses), with a specular glint, and the
brightness is snapped to the material's colour ramp, with ordered dithering between steps. So the result keeps a hand-
drawn pixel-art look at four times the detail, rather than a blurry render. Finally `finish` adds a dark outline.

Materials are colour ramps, darkest first. `worn` paints wear: bare metal showing through paint near the edges, as the
dieselpunk style asks for (docs/ART_DIRECTION.md). Everything is deterministic: no randomness without a fixed seed.

Minecraft draws an item texture of any square size, but beside vanilla's 16x16 items a 64x64 icon looks like it comes
from another game (docs/ITEM_ICONS.md, rule 1).
"""
import math
import random

from PIL import Image

SIZE = 64
# Light from the top left, slightly towards the viewer.
LIGHT = (-0.55, -0.65, 0.52)
_n = math.sqrt(sum(c * c for c in LIGHT))
LIGHT = tuple(c / _n for c in LIGHT)
# 4x4 Bayer matrix for ordered dithering between ramp steps.
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


class Material:
    """A colour ramp (darkest first) and how shiny it is."""

    def __init__(self, ramp, specular=0.3, shininess=12, ambient=0.28):
        self.ramp = [tuple(c) for c in ramp]
        self.specular = specular
        self.shininess = shininess
        self.ambient = ambient


# Shared materials, in the dieselpunk palette.
STEEL = Material([(34, 37, 44), (62, 67, 77), (96, 103, 115), (136, 144, 156), (182, 189, 199), (228, 233, 238)], 0.55, 18)
GUNMETAL = Material([(22, 24, 29), (38, 41, 49), (58, 62, 72), (82, 88, 99), (112, 119, 131), (150, 157, 168)], 0.35, 14)
CHROME = Material([(30, 34, 42), (70, 78, 92), (130, 140, 156), (190, 198, 210), (236, 241, 247), (255, 255, 255)], 0.9, 30)
BRASS = Material([(70, 46, 16), (118, 82, 30), (164, 120, 48), (204, 162, 74), (234, 202, 118), (252, 236, 178)], 0.6, 20)
RUBBER = Material([(12, 12, 14), (22, 22, 26), (34, 34, 39), (48, 48, 54), (64, 64, 72)], 0.12, 6, 0.35)
OLIVE = Material([(32, 38, 20), (50, 59, 32), (70, 82, 46), (94, 108, 62), (122, 138, 82), (156, 170, 110)], 0.18, 8)
SAFETY_YELLOW = Material([(96, 66, 6), (150, 106, 10), (200, 150, 18), (232, 188, 36), (248, 216, 84), (255, 240, 160)], 0.3, 10)
HAZARD_BLACK = Material([(14, 14, 16), (24, 24, 28), (36, 36, 42), (52, 52, 58)], 0.15, 6)
WHITE_PAINT = Material([(120, 122, 124), (168, 170, 172), (206, 208, 208), (232, 233, 232), (248, 248, 246)], 0.25, 10)
GLASS = Material([(20, 40, 46), (40, 74, 82), (70, 116, 124), (120, 170, 176), (196, 232, 236)], 0.9, 40, 0.4)
FOAM = Material([(150, 128, 68), (190, 168, 104), (220, 202, 142), (238, 226, 178), (250, 244, 214)], 0.08, 4, 0.4)
PAPER = Material([(118, 100, 66), (156, 136, 96), (190, 170, 126), (216, 198, 152), (236, 222, 182)], 0.05, 4, 0.35)
CONCRETE = Material([(70, 72, 74), (100, 102, 104), (130, 132, 132), (160, 162, 160), (190, 190, 186)], 0.05, 4, 0.35)
RUST = Material([(52, 26, 16), (84, 42, 24), (118, 62, 34), (150, 86, 48), (178, 112, 66)], 0.15, 6)
GAUGE_FACE = Material([(160, 156, 140), (200, 196, 178), (226, 222, 204), (240, 238, 226)], 0.05, 4, 0.6)
RED = Material([(70, 10, 10), (120, 20, 18), (170, 36, 30), (210, 64, 52), (236, 110, 96)], 0.35, 12)


class Canvas:
    """A 64x64 RGBA image with a per-pixel depth, so later shapes paint over earlier ones."""

    def __init__(self, size=SIZE):
        self.size = size
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.px = self.img.load()

    # ------------------------------------------------------------- lighting
    def shade(self, x, y, material, normal, tint=0.0):
        nx, ny, nz = normal
        lambert = max(0.0, nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2])
        # Blinn-Phong highlight with the viewer straight on.
        hx, hy, hz = LIGHT[0], LIGHT[1], LIGHT[2] + 1.0
        hn = math.sqrt(hx * hx + hy * hy + hz * hz)
        spec = max(0.0, (nx * hx + ny * hy + nz * hz) / hn) ** material.shininess * material.specular
        value = material.ambient + (1.0 - material.ambient) * lambert + spec + tint
        value = max(0.0, min(0.999, value))
        steps = len(material.ramp)
        level = value * steps
        base = int(level)
        frac = level - base
        # Dither only near the middle of a step, so flat areas stay clean and only the shading falloff is stippled.
        if 0.3 < frac < 0.7:
            if (frac - 0.3) / 0.4 * 16 > BAYER[y % 4][x % 4] + 0.5 and base + 1 < steps:
                base += 1
        elif frac >= 0.7 and base + 1 < steps:
            base += 1
        return material.ramp[min(steps - 1, base)]

    def put(self, x, y, colour, alpha=255):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[x, y] = tuple(colour[:3]) + (alpha,)

    # ------------------------------------------------------------- shapes
    def capsule(self, a, b, radius, material, flat_ends=False, tint=0.0, bands=None):
        """A rod from a to b, round across its width. bands: [(t0, t1, material)] along it (0 at a, 1 at b)."""
        ax, ay = a
        bx, by = b
        dx, dy = bx - ax, by - ay
        length = math.hypot(dx, dy) or 1.0
        ux, uy = dx / length, dy / length
        x0, x1 = int(min(ax, bx) - radius - 1), int(max(ax, bx) + radius + 2)
        y0, y1 = int(min(ay, by) - radius - 1), int(max(ay, by) + radius + 2)
        for y in range(max(0, y0), min(self.size, y1)):
            for x in range(max(0, x0), min(self.size, x1)):
                px, py = x + 0.5 - ax, y + 0.5 - ay
                t = px * ux + py * uy
                if flat_ends and (t < 0 or t > length):
                    continue
                tc = max(0.0, min(length, t))
                cx, cy = px - tc * ux, py - tc * uy
                d = math.hypot(cx, cy)
                if d > radius:
                    continue
                s = d / radius
                nz = math.sqrt(max(0.0, 1 - s * s))
                n = (cx / radius, cy / radius, nz) if d > 0 else (0, 0, 1)
                mat = material
                if bands:
                    f = t / length
                    for t0, t1, m in bands:
                        if t0 <= f <= t1:
                            mat = m
                self.put(x, y, self.shade(x, y, mat, n, tint))

    def box(self, center, half_w, half_h, angle, material, bevel=1.5, tint=0.0, paint=None):
        """A block seen face on, rotated by angle (degrees), with bevelled edges that catch the light.
        paint(u, v) may return a different material for a pixel, in box space (u across, v along, from the centre)."""
        cx, cy = center
        ca, sa = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        r = math.hypot(half_w, half_h) + 1
        for y in range(max(0, int(cy - r)), min(self.size, int(cy + r) + 1)):
            for x in range(max(0, int(cx - r)), min(self.size, int(cx + r) + 1)):
                px, py = x + 0.5 - cx, y + 0.5 - cy
                u = px * ca + py * sa
                v = -px * sa + py * ca
                if abs(u) > half_w or abs(v) > half_h:
                    continue
                # Normal: flat, tilting out within the bevel of each edge.
                nu = nv = 0.0
                eu, ev = half_w - abs(u), half_h - abs(v)
                if eu < bevel:
                    nu = math.copysign(1 - eu / bevel, u) * 0.8
                if ev < bevel:
                    nv = math.copysign(1 - ev / bevel, v) * 0.8
                nx = nu * ca - nv * sa
                ny = nu * sa + nv * ca
                nz = math.sqrt(max(0.05, 1 - nx * nx - ny * ny))
                mat = (paint(u, v) if paint else None) or material
                self.put(x, y, self.shade(x, y, mat, (nx, ny, nz), tint))

    def disc(self, center, radius, material, dome=1.0, tint=0.0):
        """A round dome (dome=1) or a nearly flat disc (dome near 0)."""
        cx, cy = center
        for y in range(max(0, int(cy - radius - 1)), min(self.size, int(cy + radius) + 2)):
            for x in range(max(0, int(cx - radius - 1)), min(self.size, int(cx + radius) + 2)):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(dx, dy)
                if d > radius:
                    continue
                nx, ny = dx / radius * dome, dy / radius * dome
                nz = math.sqrt(max(0.05, 1 - nx * nx - ny * ny))
                self.put(x, y, self.shade(x, y, material, (nx, ny, nz), tint))

    def ring(self, center, outer, inner, material, tint=0.0):
        """A round rim (a bezel or collar), bulging between inner and outer."""
        cx, cy = center
        mid, half = (outer + inner) / 2, (outer - inner) / 2
        for y in range(max(0, int(cy - outer - 1)), min(self.size, int(cy + outer) + 2)):
            for x in range(max(0, int(cx - outer - 1)), min(self.size, int(cx + outer) + 2)):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(dx, dy)
                if not inner <= d <= outer:
                    continue
                s = (d - mid) / half
                k = s / max(d, 0.01)
                nx, ny = dx * k * 0.9, dy * k * 0.9
                nz = math.sqrt(max(0.05, 1 - nx * nx - ny * ny))
                self.put(x, y, self.shade(x, y, material, (nx, ny, nz), tint))

    def polygon(self, points, material, normal=(0, 0, 1), tint=0.0):
        """A flat shape (a decal, a blade, a spoon)."""
        xs, ys = [p[0] for p in points], [p[1] for p in points]
        for y in range(max(0, int(min(ys))), min(self.size, int(max(ys)) + 1)):
            for x in range(max(0, int(min(xs))), min(self.size, int(max(xs)) + 1)):
                if _inside(points, x + 0.5, y + 0.5):
                    self.put(x, y, self.shade(x, y, material, normal, tint))

    def blob(self, center, radius, material, seed, lumps=7, tint=0.0):
        """A lumpy mass of overlapping domes (foam, putty, a heap of powder)."""
        rng = random.Random(seed)
        cx, cy = center
        parts = [(cx, cy, radius)]
        for _ in range(lumps):
            angle = rng.uniform(0, 2 * math.pi)
            dist = rng.uniform(0.3, 0.8) * radius
            parts.append((cx + math.cos(angle) * dist, cy + math.sin(angle) * dist, radius * rng.uniform(0.45, 0.7)))
        for px, py, r in parts:
            self.disc((px, py), r, material, 0.9, tint)

    def pixel(self, x, y, colour):
        self.put(int(x), int(y), colour)

    def line(self, a, b, colour, width=1.0):
        """A thin flat line (a seam, a stencil, a needle)."""
        ax, ay = a
        bx, by = b
        steps = int(max(abs(bx - ax), abs(by - ay)) * 2) + 1
        for i in range(steps + 1):
            t = i / steps
            x, y = ax + (bx - ax) * t, ay + (by - ay) * t
            for ox in range(int(-width // 2), int(width // 2) + 1):
                self.put(int(x + ox), int(y), colour)

    # ------------------------------------------------------------- finish
    def finish(self, outline=(10, 10, 14), alpha=200):
        """A one-pixel dark outline round everything painted."""
        src = self.img.copy()
        sp = src.load()
        for y in range(self.size):
            for x in range(self.size):
                if sp[x, y][3]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.size and 0 <= ny < self.size and sp[nx, ny][3] == 255:
                        self.px[x, y] = tuple(outline) + (alpha,)
                        break
        return self.img


def _inside(points, x, y):
    inside = False
    n = len(points)
    for i in range(n):
        x1, y1 = points[i]
        x2, y2 = points[(i + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


def hazard(width=4):
    """A paint function for box(): yellow-and-black diagonal hazard stripes."""
    def paint(u, v):
        return SAFETY_YELLOW if int((u + v + 100) // width) % 2 == 0 else HAZARD_BLACK
    return paint


def worn(paint_material, bare_material, edge=2.0, seed=1, chance=0.5):
    """A paint function for box(): paint worn through to bare metal near the edges (needs the box's half sizes)."""
    rng = random.Random(seed)
    chips = {}

    def make(half_w, half_h):
        def paint(u, v):
            near = min(half_w - abs(u), half_h - abs(v))
            if near < edge:
                key = (int(u), int(v))
                if key not in chips:
                    chips[key] = rng.random() < chance
                if chips[key]:
                    return bare_material
            return paint_material
        return paint
    return make
