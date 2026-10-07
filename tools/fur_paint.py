"""Painted fur for high-resolution creature textures (docs/ART_DIRECTION.md, "High resolution"): a creature's texture
painted at several times its model's texture size, so each model pixel holds a patch of fur rather than one flat
colour. Minecraft samples a model's texture by its declared size, so a texture four times as big simply draws four
times finer on the same model.

The painter works face by face on an RGBA canvas:
- `shade` lays a face's base colour along a ramp, lighter towards the light (the top of a side face, the middle of a
  top face), with soft darkening into its edges, as fur shades into the next face;
- `locks` lays the fur on in clumps: tapering locks a few pixels wide, flowing one way (down a side, back over a top),
  each lit along its upper side and shadowed along its lower, so the coat reads as painted tufts rather than streaks;
- `strands` adds fine single hairs over them, a little lighter or darker than the fur beneath;
- `tufts` hangs points of darker fur over a face's lower edge, and `ragged` cuts the edge itself into points (mob
  textures are drawn as cut-outs, so cleared pixels let the fur's outline break the box's straight edge);
- `ellipse`, `blob`, `line` and `glint` paint eyes, noses, pads, teeth and claws on top.

A painter made with `clean=True` paints the clean, cartoon style (docs/ART_DIRECTION.md, "Creatures and faces: cute and
clean"): every tone is snapped to a few flat bands, there is no soft noise or jitter, locks sit in neat staggered rows
as big flat tapering points with a dark edge on their shadow side, fine strands are left out, and tufts and ragged
edges are even points. `clean_painter` and `clean_ramp` go one step further for props painted mostly by formula (the
fall fair): the painter's `noise` is flat, so formulas built on it lose their mottling, and `clean_ramp` snaps the
tones those formulas pick to the same flat bands.

Everything is deterministic (each texture is painted from a fixed seed) and drawn by code: no other texture is read,
traced or recoloured.
"""
import math
import random

from PIL import Image


def clean_ramp(colours, f):
    """`ramp`, with `f` snapped to the clean style's flat bands."""
    return ramp(colours, band(f))


def clean_painter(width, height, seed):
    """A clean painter whose `noise` is flat: every formula that adds a little noise to a tone paints it evenly."""
    return Painter(width, height, seed, clean=True, quiet=True)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def ramp(colours, f):
    """The colour `f` (0 to 1) of the way along `colours`, darkest first."""
    f = max(0.0, min(1.0, f)) * (len(colours) - 1)
    i = min(int(f), len(colours) - 2)
    return mix(colours[i], colours[i + 1], f - i)


# The clean style's band width: tones snap to multiples of this along a palette's ramp.
BAND = 0.16


def band(f):
    return round(f / BAND) * BAND


class Painter:
    def __init__(self, width, height, seed, clean=False, quiet=False):
        self.width = width
        self.height = height
        self.quiet = quiet
        self.img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rng = random.Random(seed)
        self._grid = {}
        self.clean = clean

    def noise(self, x, y, cell=8.0):
        """Smooth value noise (0 to 1) with features about `cell` pixels across: the soft light and dark patches of a coat
        (a flat 0.5 for a `quiet` painter)."""
        if self.quiet:
            return 0.5
        gx, gy = x / cell, y / cell
        ix, iy = math.floor(gx), math.floor(gy)
        tx, ty = gx - ix, gy - iy
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)

        def at(i, j):
            key = (i, j, cell)
            if key not in self._grid:
                self._grid[key] = random.Random(hash((i, j, int(cell * 10), self.width)) & 0xFFFFFFFF).random()
            return self._grid[key]
        top = at(ix, iy) + (at(ix + 1, iy) - at(ix, iy)) * tx
        bottom = at(ix, iy + 1) + (at(ix + 1, iy + 1) - at(ix, iy + 1)) * tx
        return top + (bottom - top) * ty

    def put(self, x, y, colour, alpha=1.0):
        x, y = int(x), int(y)
        if not (0 <= x < self.width and 0 <= y < self.height):
            return
        if alpha >= 1.0:
            self.px[x, y] = tuple(colour[:3]) + (255,)
            return
        old = self.px[x, y]
        if old[3] == 0:
            self.px[x, y] = tuple(colour[:3]) + (int(255 * alpha),)
            return
        self.px[x, y] = mix(old, colour, alpha) + (255,)

    def shade(self, rect, colours, level=0.5, spread=0.35, light="top", edge=0.18):
        """Fills `rect` (x, y, w, h) with `colours` around `level`: `spread` lighter towards the light ("top": the top
        rows; "centre": the middle; "flat": evenly), darker by up to `edge` into its border."""
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                fy = y / max(1, h - 1)
                fx = x / max(1, w - 1)
                if light == "top":
                    f = level + spread * (0.5 - fy)
                elif light == "centre":
                    f = level + spread * (0.5 - math.hypot(fx - 0.5, fy - 0.5))
                else:
                    f = level
                border = min(x, w - 1 - x, y, h - 1 - y) / max(2.0, min(w, h) * 0.18)
                f -= edge * max(0.0, 1.0 - border)
                if self.clean:
                    f = band(f)
                else:
                    f += 0.2 * (self.noise(x0 + x, y0 + y, 15.0) - 0.5) + self.rng.uniform(-0.015, 0.015)
                self.put(x0 + x, y0 + y, ramp(colours, f))

    def strands(self, rect, colours, level=0.5, flow=(0.0, 1.0), density=0.5, length=(4, 9), spread=0.22, light=0.15):
        """Short tapering hairs over `rect`, flowing along `flow`: `density` hairs per pixel of area (times 1/8), each
        `length` pixels long, its tone `level` give or take `spread`, lighter by `light` near the face's top."""
        x0, y0, w, h = rect
        if self.clean:
            return
        count = int(w * h * density / 8) + 1
        fx, fy = flow
        norm = math.hypot(fx, fy) or 1.0
        fx, fy = fx / norm, fy / norm
        for _ in range(count):
            sx = x0 + self.rng.uniform(-1, w)
            sy = y0 + self.rng.uniform(-2, h)
            n = self.rng.randint(*length)
            tone = level + self.rng.uniform(-spread, spread) + light * (1.0 - (sy - y0) / max(1, h))
            colour = ramp(colours, tone)
            bend = self.rng.uniform(-0.25, 0.25)
            dx, dy = fx, fy
            x, y = sx, sy
            for k in range(n):
                if x0 <= x < x0 + w and y0 <= y < y0 + h:
                    self.put(x, y, colour, 0.85 * (1.0 - k / (n + 1)) + 0.15)
                dx, dy = dx + bend * dy * 0.15, dy - bend * dx * 0.15
                x += dx
                y += dy

    def locks(self, rect, colours, level=0.5, flow=(0.0, 1.0), density=1.0, length=(6, 13), width=(2.0, 3.6), spread=0.18,
              light=0.12):
        """Clumps of fur over `rect`: tapering locks flowing along `flow` (give or take a little), each lit along one side
        and shadowed along the other, darker at the root where it tucks under the lock above."""
        x0, y0, w, h = rect
        if self.clean:
            self._clean_locks(rect, colours, level, flow, length, width, light)
            return
        fx, fy = flow
        norm = math.hypot(fx, fy) or 1.0
        fx, fy = fx / norm, fy / norm
        count = int(w * h * density / 14) + 1
        for _ in range(count):
            sx = x0 + self.rng.uniform(-2, w + 1)
            sy = y0 + self.rng.uniform(-4, h)
            n = self.rng.randint(*length)
            half = self.rng.uniform(*width) / 2.0
            turn = self.rng.uniform(-0.35, 0.35)
            dx, dy = fx * math.cos(turn) - fy * math.sin(turn), fx * math.sin(turn) + fy * math.cos(turn)
            px, py = -dy, dx
            tone = level + self.rng.uniform(-spread, spread) + light * (1.0 - (sy - y0) / max(1, h))
            tone += 0.16 * (self.noise(sx, sy, 15.0) - 0.5)
            for k in range(n):
                t = k / n
                r = half * (1.0 - t) + 0.35
                cx, cy = sx + dx * k, sy + dy * k
                for j in range(-int(r) - 1, int(r) + 2):
                    if abs(j) > r:
                        continue
                    x, y = cx + px * j, cy + py * j
                    if not (x0 <= x < x0 + w and y0 <= y < y0 + h):
                        continue
                    side = j / max(0.5, r)
                    # Lit along one side, a soft core, shadowed at the root where it tucks under the lock above.
                    f = tone + 0.11 * (-side) - 0.06 * side * side - 0.14 * (1.0 - min(1.0, k / 4.0)) + 0.06 * t
                    self.put(x, y, ramp(colours, f), 0.95 if abs(side) < 0.7 else 0.6)
                if k < n - 1:
                    edge = cx - px * (r + 0.6), cy - py * (r + 0.6)
                    if x0 <= edge[0] < x0 + w and y0 <= edge[1] < y0 + h:
                        self.put(edge[0], edge[1], ramp(colours, tone - 0.28), 0.5)

    def ragged(self, rect, depth=6, every=4):
        """Cuts the bottom `depth` rows of `rect` into points of fur (cleared pixels show through as cut-outs)."""
        x0, y0, w, h = rect
        x = 0
        while x < w:
            span = every + 1 if self.clean else self.rng.randint(every - 1, every + 2)
            tip = (depth * 3) // 4 if self.clean else self.rng.randint(depth // 2, depth)
            for k in range(span):
                # A V between two points: the gap is deepest between them.
                cut = int(tip * (1.0 - abs(2.0 * k / max(1, span - 1) - 1.0)) ** 0.8)
                for d in range(cut):
                    xx, yy = x0 + x + k, y0 + h - 1 - d
                    if x0 <= xx < x0 + w and 0 <= xx < self.width and 0 <= yy < self.height:
                        self.px[xx, yy] = (0, 0, 0, 0)
            x += span

    def tufts(self, rect, colours, level=0.35, depth=4, every=3):
        """Points of darker fur hanging into a face's bottom `depth` rows, one every `every` pixels."""
        x0, y0, w, h = rect
        for x in range(0, w, every):
            tip = depth if self.clean else self.rng.randint(depth // 2, depth)
            for k in range(tip):
                for dx in range(-(tip - k) // 2, (tip - k) // 2 + 1):
                    if self.clean:
                        self.put(x0 + x + dx, y0 + h - 1 - k, ramp(colours, band(level - 0.1)))
                    else:
                        self.put(x0 + x + dx, y0 + h - 1 - k, ramp(colours, level - 0.1 + 0.05 * k / tip), 0.9)

    def _clean_locks(self, rect, colours, level, flow, length, width, light):
        """The clean style's locks: flat tapering points in neat staggered rows along `flow`, each one flat tone (two
        alternating bands, lighter towards the face's top), edged with a darker band on its shadow side."""
        x0, y0, w, h = rect
        fx, fy = flow
        norm = math.hypot(fx, fy) or 1.0
        fx, fy = fx / norm, fy / norm
        px, py = -fy, fx
        n = int((length[0] + length[1]) * 0.9)
        half = (width[0] + width[1]) / 2.4
        step_along = max(3.0, n * 0.75)
        step_across = max(2.0, half * 2.0)
        # Cover the rect in lattice coordinates (along, across) about its centre.
        cx0, cy0 = x0 + w / 2.0, y0 + h / 2.0
        reach = math.hypot(w, h) / 2.0 + n
        rows = int(reach / step_along) + 1
        cols = int(reach / step_across) + 1
        # Far rows first, so each lock's tip lies over the root of the lock beyond it: points hang the way the fur flows.
        for i in range(rows, -rows - 1, -1):
            for j in range(-cols, cols + 1):
                a = i * step_along
                b = j * step_across + (step_across / 2.0 if i % 2 else 0.0)
                sx, sy = cx0 + fx * a + px * b, cy0 + fy * a + py * b
                if not (x0 - n <= sx < x0 + w + n and y0 - n <= sy < y0 + h + n):
                    continue
                top = 1.0 - (sy - y0) / max(1, h)
                tone = band(level + light * top + (0.08 if (i + j) % 2 else -0.04))
                for k in range(n):
                    t = k / n
                    r = half * (1.0 - t) + 0.35
                    lx, ly = sx + fx * k, sy + fy * k
                    for jj in range(-int(r) - 1, int(r) + 2):
                        if abs(jj) > r + 0.5:
                            continue
                        x, y = lx + px * jj, ly + py * jj
                        if not (x0 <= x < x0 + w and y0 <= y < y0 + h):
                            continue
                        self.put(x, y, ramp(colours, tone - (BAND if jj > r - 0.5 else 0.0)))

    def ellipse(self, cx, cy, rx, ry, colour, alpha=1.0):
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
                d = ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2
                if d <= 1.0:
                    self.put(x, y, colour, alpha)

    def blob(self, cx, cy, rx, ry, dark, light, alpha=1.0):
        """A rounded shape lit from the top left: `light` at its upper left, `dark` at its lower right."""
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
                u = (x + 0.5 - cx) / rx
                v = (y + 0.5 - cy) / ry
                if u * u + v * v <= 1.0:
                    t = 0.5 + 0.5 * (u + v) / 1.414
                    if self.clean:
                        t = 0.15 if t < 0.35 else 0.55 if t < 0.75 else 0.9
                    self.put(x, y, mix(light, dark, t), alpha)

    def line(self, xa, ya, xb, yb, colour, width=1.0, alpha=1.0):
        n = int(max(abs(xb - xa), abs(yb - ya)) * 2) + 1
        for i in range(n + 1):
            t = i / n
            x = xa + (xb - xa) * t
            y = ya + (yb - ya) * t
            r = width / 2.0
            for yy in range(int(y - r), int(y + r) + 1):
                for xx in range(int(x - r), int(x + r) + 1):
                    if (xx + 0.5 - x) ** 2 + (yy + 0.5 - y) ** 2 <= max(0.3, r * r):
                        self.put(xx, yy, colour, alpha)

    def glint(self, x, y, colour=(255, 255, 255)):
        self.put(x, y, colour)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            self.put(x + dx, y + dy, colour, 0.45)

    def point(self, rect, colours, root=0.15, tip=0.95, down=True, gloss=True):
        """Fills `rect` with a tapering point (a claw or fang seen side-on), clearing the rest: dark at the root, pale at
        the tip, with a glossy line down one side."""
        x0, y0, w, h = rect
        for y in range(h):
            t = y / max(1, h - 1) if down else 1.0 - y / max(1, h - 1)
            half = w / 2.0 * (1.0 - 0.85 * t)
            for x in range(w):
                d = x + 0.5 - w / 2.0
                if abs(d) <= half:
                    f = root + (tip - root) * t - 0.2 * abs(d) / max(0.5, half)
                    if gloss and -half * 0.5 < d < -half * 0.1:
                        f += 0.2
                    self.put(x0 + x, y0 + y, ramp(colours, f))
                else:
                    self.px[x0 + x, y0 + y] = (0, 0, 0, 0)

    def tooth(self, x, y, width, height, colours, down=True):
        """A pointed tooth `width` wide at its root (x, y), `height` long, pointing down (or up), shaded across."""
        for k in range(height):
            half = width / 2.0 * (1.0 - k / height)
            yy = y + k if down else y - k
            for xx in range(int(x - half), int(x + half) + 1):
                f = 0.8 - 0.6 * k / height + 0.2 * (xx - x) / max(1.0, half)
                self.put(xx, yy, ramp(colours, f))
