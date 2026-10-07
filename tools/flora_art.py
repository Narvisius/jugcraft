"""Sculpted models and textures for the graveyard flora (tools/plants.py plants with "art": "flora", and the mandrake in
tools/agriculture.py): chunky hand-built plants rather than vanilla's crossed pictures. Stems are bent from short boxes,
leaves and petals are flat cut-out pieces set at angles, bells, berries, roots and caps are little boxes.

Each plant has one 64 x 64 texture (block/<plant>) holding every piece it uses, painted here by code at two texels to
a model unit; a model's faces map onto their pieces. Block models may turn an element only by 22.5 or 45 degrees about
one axis, so leaves point along the four sides (tilted up or down) or lie flat at 45 degrees between them.

Everything is drawn here from fixed seeds. No Mojang texture is read, traced or copied; flower pots use vanilla's own
pot and dirt textures by reference.
"""
import math
import random

from PIL import Image

MOD = "jugcraft"
SIZE = 64
TEXELS = 2          # texels of a piece per model unit
UV = 16 / SIZE      # model uv units per texel


def rgb(hex_color):
    hex_color = hex_color.lstrip("#")
    return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) for h in hexes]


# ---------------------------------------------------------------- palettes, darkest first
STEM = pal("26401f", "35562a", "4a7238", "62904a")
LEAF = pal("1c331a", "284a24", "386530", "4f8240", "6c9c55")
DUSK_LEAF = pal("1a2a1e", "243c29", "324f36", "466a48", "5f8560")
SPIDER_RED = pal("4e0a10", "7a1019", "a5172a", "cf2a33", "ef5446", "fb8a6a")
SNOW_WHITE = pal("8e9a95", "b9c4bf", "dce4e0", "f3f7f5", "ffffff")
SNOW_GREEN = pal("4a7a32", "6c9c48", "93bf64")
NIGHT_PURPLE = pal("2a1636", "45285a", "634080", "8660a2", "a985bf")
BERRY = pal("08070b", "16131c", "282432", "4a4658", "9a98b0")
HEART_PINK = pal("6a1236", "992650", "c23f72", "e2689a", "f49cc0")
HEART_WHITE = pal("d8c8d0", "f3eaef", "ffffff")
GHOST = pal("9fa9b0", "c1cad0", "dde4e8", "f0f4f6", "ffffff")
GHOST_FLECK = pal("2c2a30", "585460")
ROSE = pal("070405", "140a0e", "241016", "38161f", "54202c", "7a2c3a")
FOX = pal("5e1446", "8a2a6c", "b44a92", "d675b4", "eda6d4", "fbd6ec")
FOX_SPOT = pal("3e0c2a", "6a1846")
LILY = pal("aab3a2", "cfd5c8", "e9ece2", "f8f9f3", "ffffff")
LILY_THROAT = pal("94ae5e", "b8cc80")
ANTHER = pal("8a3e10", "c0641c", "e48c30")
ASPHODEL = pal("b7aab0", "d8ccd1", "eee5e8", "fbf7f8")
ASPHODEL_VEIN = pal("6e3e30", "8e5644")
DRY = pal("4a4236", "665a46", "857559", "a4936f", "c0b18a", "d8cca6")
FERN = pal("4e5e5c", "6f807c", "91a39e", "b4c4bf", "d6e2de")
FERN_RIB = pal("4a1a26", "6e2a3a", "8e3e4e")
FINGER = pal("0b0a09", "171512", "25221d", "383329", "4f493d")
FINGER_TIP = pal("8c887e", "b4b0a4", "d6d2c6")
MOSS = pal("17281a", "213a22", "2e4f2a", "3f6834", "578443")
SPORE = pal("6e2a1c", "984030", "b8603c")
CAPSULE = pal("4a3420", "6e5030", "8e6c40")
SHROUD = pal("4a5648", "667262", "86917e", "a6b09a", "c8cfba")
IVY = pal("0f2614", "17361d", "234a28", "336234", "4c7e48")
IVY_VEIN = pal("6c9064", "8eb084")
IVY_BERRY = pal("120e18", "251c30", "43365a")
MANDRAKE_LEAF = pal("1e3519", "2c4b23", "3d632f", "52803e", "6d9a52")
MANDRAKE_FLOWER = pal("4e3468", "71508e", "9677b2", "bca2d4")
ROOT = pal("3c2818", "5a3c24", "7a5634", "9a7448", "b8925e", "d4b27c")
ROOT_DARK = pal("1c120a", "2e1e12")
SOIL = pal("2e2116", "3e2d1e", "513b28")
CALYX = pal("2a4a22", "3e6630", "5a8444")


# ---------------------------------------------------------------- the piece atlas

class Atlas:
    """A square texture (64 x 64 unless `size` says otherwise) filled row by row with painted pieces, a texel apart."""

    def __init__(self, size=SIZE):
        self.size = size
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.x = self.y = self.row = 0
        self.uvs = {}

    def piece(self, key, w, h, painter, turn=0):
        """Paints a w x h piece (texels) once under `key` and returns its uv box. `turn` (0, 90, 180, 270) turns the
        painted image clockwise before it is placed, so one painter serves pieces pointing different ways."""
        if key in self.uvs:
            return self.uvs[key]
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        painter(Px(img))
        if turn:
            img = img.rotate(-turn, expand=True)
        w, h = img.size
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row + 1, 0
        if self.y + h > self.size:
            raise ValueError(f"texture full placing {key}")
        self.img.alpha_composite(img, (self.x, self.y))
        unit = 16 / self.size
        uv = (self.x * unit, self.y * unit, (self.x + w) * unit, (self.y + h) * unit)
        self.uvs[key] = uv
        self.x += w + 1
        self.row = max(self.row, h)
        return uv


class Px:
    """Pixel access to a piece being painted."""

    def __init__(self, img):
        self.img = img
        self.w, self.h = img.size

    def put(self, x, y, c, a=255):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((x, y), tuple(c) + (a,))

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            p = self.img.getpixel((x, y))
            return p if p[3] else None
        return None

    def filled(self):
        return [(x, y) for y in range(self.h) for x in range(self.w) if self.get(x, y)]

    def outline(self, c):
        """Darkens the edge pixels of what is drawn (inside the shape, keeping its size)."""
        edge = [(x, y) for x, y in self.filled() if any(self.get(x + dx, y + dy) is None
                                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
        for x, y in edge:
            self.put(x, y, c)


def shade(palette, k):
    return palette[max(0, min(len(palette) - 1, k))]


# ---------------------------------------------------------------- painters

def profile(shape, t):
    """Half-width of a leaf or petal, 0..1, at t from its base (0) to its tip (1)."""
    if shape == "oval":
        return max(0.0, math.sin(math.pi * min(1.0, t * 0.96 + 0.02))) ** 0.75
    if shape == "lance":
        return max(0.0, (t ** 0.6) * (1 - t) * 2.6) if t < 1 else 0.0
    if shape == "strap":
        return 0.85 if t < 0.8 else max(0.0, 0.85 * (1 - t) / 0.2)
    if shape == "heart":
        return max(0.0, math.sin(math.pi * (1 - t) ** 0.8) ** 0.6) if t > 0.02 else 0.3
    if shape == "spoon":
        return max(0.0, math.sin(math.pi * t ** 1.4)) ** 0.7
    if shape == "round":
        return max(0.0, math.sin(math.pi * t)) ** 0.5
    raise ValueError(shape)


# While True, the painters below leave out their per-pixel random tones, knots, speckles and spots: the clean style
# (docs/ART_DIRECTION.md, "Creatures and faces: cute and clean"). Set by a builder for what it paints (tools/flora_models.py).
QUIET = False


def quietly(builder):
    """`builder`, run with QUIET set, so everything it paints is in the clean style (a decorator)."""
    def run(*args, **kwargs):
        global QUIET
        quiet, QUIET = QUIET, True
        try:
            return builder(*args, **kwargs)
        finally:
            QUIET = quiet
    run.__name__, run.__doc__ = builder.__name__, builder.__doc__
    return run


def leaf(palette, shape="oval", seed=1, vein=True, wavy=0.0, curl=0.0, light_side=True, rib=None, tip_colour=None):
    """A leaf, petal or tepal standing on its base (bottom row) with its tip at the top: shaded lighter on its left half,
    a midrib down the middle, an optional wavy edge (`wavy`, in texels) and a sideways curl of its tip (`curl`)."""
    def paint(p):
        rng = random.Random(seed)
        cx = (p.w - 1) / 2
        n = len(palette)
        for y in range(p.h):
            t = (p.h - 1 - y) / max(1, p.h - 1)
            half = profile(shape, t) * p.w / 2
            if wavy:
                half += wavy * math.sin(t * 17 + seed) * (0.4 + 0.6 * t)
            mid = cx + curl * t * t * p.w / 2
            for x in range(p.w):
                d = x - mid
                if abs(d) > half - 0.15:
                    continue
                k = n - 2 if (d < 0) == light_side else n - 3
                if abs(d) > half - 1.2:
                    k -= 1
                if t > 0.82:
                    k += 1
                k += rng.choice((0, 0, 0, 1, -1)) if n > 4 and not QUIET else 0
                c = shade(palette, k)
                if tip_colour and t > 0.86:
                    c = tip_colour
                p.put(x, y, c)
            if vein and abs(mid - round(mid)) < 0.6 and half > 1.2 and t < 0.9:
                p.put(mid, y, rib or shade(palette, n - 1 if t < 0.5 else n - 2))
        p.outline(palette[0])
    return paint


def strip(palette, seed=1, knots=0, speckle=None, light=False, horizontal=False):
    """A stem's or box's face: shaded across its width (lighter one side), with a few knots and speckles."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        for y in range(p.h):
            for x in range(p.w):
                across = (y / max(1, p.h - 1)) if horizontal else (x / max(1, p.w - 1))
                k = n - 1 - int(across * (n - 1) * 0.9) if light else n - 2 - int(across * (n - 2))
                k += 0 if QUIET else rng.choice((0, 0, 0, -1, 1))
                p.put(x, y, shade(palette, k))
        for _ in range(0 if QUIET else knots):
            p.put(rng.randrange(p.w), rng.randrange(p.h), palette[0])
        if speckle and not QUIET:
            for _ in range(max(1, p.w * p.h // 10)):
                p.put(rng.randrange(p.w), rng.randrange(p.h), rng.choice(speckle))
    return paint


def solid(palette, seed=1, rim=None, spots=None, spot_count=0, glossy=None):
    """A box face of one colour family, mottled, with an optional darker rim, spots and a glossy highlight."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        for y in range(p.h):
            for x in range(p.w):
                k = n // 2 + (0 if QUIET else rng.choice((0, 0, -1, 1)))
                if rim and (x in (0, p.w - 1) or y in (0, p.h - 1)):
                    k = 0
                p.put(x, y, shade(palette, k))
        for _ in range(0 if QUIET else spot_count):
            p.put(rng.randrange(1, max(2, p.w - 1)), rng.randrange(1, max(2, p.h - 1)), rng.choice(spots))
        if glossy:
            p.put(max(0, p.w // 3), max(0, p.h // 3), glossy)
    return paint


def star(n, palette, centre=None, inner=0.18, width=0.5, wavy=0.0, seed=1, twist=0.0, centre_r=0.16):
    """A flower seen from above: `n` petals round a centre, each `width` of its share of the circle wide."""
    def paint(p):
        rng = random.Random(seed)
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        r_max = min(p.w, p.h) / 2
        m = len(palette)
        for y in range(p.h):
            for x in range(p.w):
                dx, dy = x - cx, y - cy
                r = math.hypot(dx, dy) / r_max
                if r > 1.0:
                    continue
                a = math.atan2(dy, dx) + twist * r
                sector = 2 * math.pi / n
                off = (a + sector / 2) % sector - sector / 2
                wave = 1 + wavy * math.sin(r * 11 + a * 3)
                petal_half = sector * width / 2 * wave * (1.0 if r < 0.85 else (1 - r) / 0.15) * min(1.0, (r + 0.25) / 0.5)
                if r < inner or abs(off) <= petal_half:
                    k = min(m - 1, int(1 + r * (m - 1)))
                    if abs(off) > petal_half * 0.6:
                        k -= 1
                    k += 0 if QUIET else rng.choice((0, 0, 0, 1, -1))
                    p.put(x, y, shade(palette, k))
        if centre:
            for y in range(p.h):
                for x in range(p.w):
                    if math.hypot(x - cx, y - cy) / r_max < centre_r:
                        p.put(x, y, centre[len(centre) // 2] if QUIET else rng.choice(centre))
        p.outline(palette[0])
    return paint


def blades(palette, count, seed=1, heads=None, droop=0.4, broken=0.3, base_spread=0.7):
    """A tuft of grass blades rising from the bottom row, some bent over or snapped; `heads` adds seed heads."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        for i in range(count):
            x0 = p.w / 2 + (rng.random() - 0.5) * p.w * base_spread
            height = p.h * (0.45 + rng.random() * 0.55)
            lean = (rng.random() - 0.5) * 1.6
            bend = rng.random() < droop
            snap = rng.random() < broken
            k = rng.randrange(1, n)
            x, y = x0, p.h - 1
            steps = int(height)
            for s in range(steps):
                t = s / max(1, steps)
                if snap and t > 0.7:
                    break
                dx = lean * 0.12 + (lean * 0.5 * t if bend and t > 0.55 else 0)
                x += dx
                y -= 1 if not (bend and t > 0.8) else 0.5
                c = shade(palette, k + (1 if t > 0.6 else 0))
                p.put(x, y, c)
                if t < 0.15 and rng.random() < 0.5:
                    p.put(x + 1, y, shade(palette, k - 1))
            if heads and not snap:
                for j in range(3):
                    p.put(x + rng.choice((-1, 0, 1)), y - j, rng.choice(heads))
    return paint


def frond(palette, rib, seed=1, curve=0.0, pinnae=9, droop=0.0):
    """A fern frond standing on its base: a dark rachis up the middle (curving by `curve`) and pinnae either side,
    each a little lobed blade angled toward the tip, longest a third of the way up and shrinking to the tip."""
    def paint(p):
        rng = random.Random(seed)
        cx = (p.w - 1) / 2
        n = len(palette)
        rows = p.h - 1
        for y in range(rows, -1, -1):
            t = (rows - y) / max(1, rows)
            p.put(cx + curve * t * t * p.w / 2, y, shade(rib, 1 if t < 0.5 else 2))
        count = max(3, pinnae)
        for i in range(count):
            t = 0.08 + 0.86 * i / count
            y0 = rows - t * rows
            x0 = cx + curve * t * t * p.w / 2
            reach = (p.w / 2 - 0.5) * min(1.0, 0.55 + t * 1.6) * (1 - t) ** 0.55
            for side in (-1, 1):
                if side > 0 and i % 2:
                    y0b = y0 - 0.5
                else:
                    y0b = y0
                steps = int(reach * 1.4) + 1
                for j in range(steps):
                    f = j / max(1, steps - 1)
                    x = x0 + side * (0.6 + f * reach)
                    y = y0b - f * reach * 0.55 + droop * f * f * reach * 0.6
                    k = n - 1 - int(f * 2.2) - (1 if side > 0 else 0)
                    p.put(x, y, shade(palette, k))
                    if f < 0.75:
                        p.put(x, y + 1, shade(palette, k - 1))
                    if f < 0.4 and (QUIET or rng.random() < 0.6):
                        p.put(x, y - 1, shade(palette, k))
    return paint


def wisps(palette, seed=1, density=0.8, taper=False):
    """Hanging lichen: strands falling from the top row, swaying, of uneven lengths (shorter and sparser if `taper`)."""
    def paint(p):
        rng = random.Random(seed)
        n = len(palette)
        for i in range(int(p.w * density * 1.6)):
            x = rng.random() * p.w
            length = p.h * ((0.3 + rng.random() * 0.6) if taper else (0.75 + rng.random() * 0.25))
            phase = rng.random() * 6
            k = rng.randrange(1, n)
            for y in range(int(length)):
                sway = math.sin(y * 0.35 + phase) * 0.8
                p.put(x + sway, y, shade(palette, k + (1 if y % 5 == 0 else 0)))
                if rng.random() < 0.08:
                    p.put(x + sway + rng.choice((-1, 1)), y, shade(palette, k - 1))
    return paint


def ivy_sheet(seed=1):
    """A sheet of ivy as it grows on a wall: wandering stems, three- and five-lobed leaves with pale veins and a few
    clusters of black berries."""
    def paint(p):
        rng = random.Random(seed)
        stems = []
        for s in range(6):
            x, y = rng.random() * p.w, p.h - 1
            for step in range(p.h * 2):
                x += rng.choice((-1, 0, 0, 1)) * 0.7
                y -= 0.6
                if y < 0:
                    break
                p.put(x, y, IVY[0])
                stems.append((x, y))
        for i in range(int(p.w * p.h / 22)):
            if not stems:
                break
            x, y = rng.choice(stems)
            ivy_leaf(p, x + rng.choice((-2, 2)), y, rng.choice((3, 4, 4, 5)), rng)
        for i in range(3):
            x, y = rng.choice(stems)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 1)):
                p.put(x + dx, y + dy, rng.choice(IVY_BERRY))
            p.put(x, y, IVY_BERRY[2])
    return paint


def ivy_leaf(p, cx, cy, r, rng):
    """One ivy leaf: three main lobes (pointing up, left and right) round a pale vein star."""
    lobes = [(-90, 1.0), (-160, 0.75), (-20, 0.75), (150, 0.45), (30, 0.45)]
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if d < 0.5:
                p.put(x, y, IVY_VEIN[0])
                continue
            a = math.degrees(math.atan2(dy, dx))
            reach = max(l * r * max(0.0, math.cos(math.radians((a - ang + 180) % 360 - 180) * 1.6)) for ang, l in lobes)
            if d <= max(reach, r * 0.45):
                k = 3 if dx < 0 else 2
                if d > max(reach, r * 0.45) - 1:
                    k -= 1
                p.put(x, y, IVY[k + (0 if QUIET else rng.choice((0, 0, 1))) if k < 4 else 4])
    for ang, l in lobes[:3]:
        for j in range(1, int(r * l)):
            p.put(cx + math.cos(math.radians(ang)) * j, cy + math.sin(math.radians(ang)) * j, IVY_VEIN[j % 2])


def root_face(seed=1, scream=True):
    """The front of a mandrake root: wrinkled, with two hollow eyes and a mouth open in a scream."""
    def paint(p):
        strip(ROOT, seed, knots=4)(p)
        rng = random.Random(seed)
        for _ in range(p.w):
            p.put(rng.randrange(p.w), rng.randrange(p.h), ROOT[1])
        ex = [p.w * 0.3, p.w * 0.7 - 1]
        ey = p.h * 0.3
        for x in ex:
            for dx in (0, 1):
                for dy in (0, 1):
                    p.put(x + dx, ey + dy, ROOT_DARK[0])
            p.put(x - 1, ey - 1, ROOT[1])
            p.put(x + 2, ey - 1, ROOT[1])
        mx, my = p.w / 2 - 1, p.h * 0.55
        if scream:
            for dx in range(-1, 3):
                for dy in range(0, 3):
                    if not (dx in (-1, 2) and dy in (0, 2)):
                        p.put(mx + dx, my + dy, ROOT_DARK[0 if dy else 1])
        else:
            for dx in range(0, 2):
                p.put(mx + dx, my, ROOT_DARK[0])
    return paint


# ---------------------------------------------------------------- elements

def _r(v):
    return round(v, 4)


def rotation(origin, axis, angle):
    if not angle:
        return None
    if angle not in (-45, -22.5, 22.5, 45):
        raise ValueError(f"block models turn elements only by 22.5 or 45 degrees, not {angle}")
    return {"origin": [_r(c) for c in origin], "axis": axis, "angle": angle}


def _el(frm, to, faces, rot=None, light=None, shade_=True):
    e = {"from": [_r(c) for c in frm], "to": [_r(c) for c in to], "faces": faces}
    if rot:
        e["rotation"] = rot
    if light:
        e["light_emission"] = light
    if not shade_:
        e["shade"] = False
    return e


def _uv(uv):
    return [_r(c) for c in uv]


def plane_xy(x0, x1, y0, y1, z, uv, rot=None, light=None):
    """An upright sheet across x (its picture's left at x0, top at y1), seen alike from north and south."""
    u0, v0, u1, v1 = uv
    return _el((x0, y0, z), (x1, y1, z), {"south": {"uv": _uv((u0, v0, u1, v1)), "texture": "#p"},
                                          "north": {"uv": _uv((u1, v0, u0, v1)), "texture": "#p"}}, rot, light)


def plane_zy(z0, z1, y0, y1, x, uv, rot=None, light=None):
    """An upright sheet across z (its picture's left at z0), seen alike from west and east."""
    u0, v0, u1, v1 = uv
    return _el((x, y0, z0), (x, y1, z1), {"west": {"uv": _uv((u0, v0, u1, v1)), "texture": "#p"},
                                          "east": {"uv": _uv((u1, v0, u0, v1)), "texture": "#p"}}, rot, light)


def plane_xz(x0, x1, z0, z1, y, uv, rot=None, light=None):
    """A level sheet (its picture's left at x0, top at z0), seen alike from above and below."""
    u0, v0, u1, v1 = uv
    return _el((x0, y, z0), (x1, y, z1), {"up": {"uv": _uv((u0, v0, u1, v1)), "texture": "#p"},
                                          "down": {"uv": _uv((u0, v1, u1, v0)), "texture": "#p"}}, rot, light)


def cube(lo, hi, faces, rot=None, light=None):
    """A box; `faces` maps each side drawn to the uv box of its piece."""
    return _el(lo, hi, {side: {"uv": _uv(uv), "texture": "#p"} for side, uv in faces.items()}, rot, light)


SIDES4 = ("north", "south", "east", "west")


def box_ring(x0, z0, x1, z1, t, y0, y1, out_uv, in_uv=None, top=None, bottom=None, ends=None, light=None):
    """A hollow square frame of four boxes `t` thick, its outer edge x0..x1 by z0..z1, from y0 to y1: a pot's wall, a
    rim, a band, a tank's lining. Its outer sides are drawn with `out_uv`, its inner sides with `in_uv` (left out if
    None, for a band whose inside is buried), and its top and bottom where given. Unlike one solid box it leaves the
    middle open, so it can be capped without the cap covering what is inside it. The north and south boxes run the full
    width and show their short ends (`ends`, else `out_uv`) at the corners; the east and west ones fit between them, so
    no two of its faces overlap. `top` and `bottom` are one uv box for all four boxes or a pair: the north and south
    boxes' (long across x) and the east and west boxes' (long along z), so a strip's texture runs along it."""
    def pair(uv):
        if uv is None:
            return None, None
        return (uv[0], uv[1]) if len(uv) == 2 else (uv, uv)
    top_ns, top_ew = pair(top)
    bottom_ns, bottom_ew = pair(bottom)
    end = ends or out_uv

    def caps(up, down):
        return {**({"up": up} if up else {}), **({"down": down} if down else {})}
    inner = (lambda side: {side: in_uv}) if in_uv else (lambda side: {})
    return [cube((x0, y0, z0), (x1, y1, z0 + t), {"north": out_uv, "east": end, "west": end, **inner("south"), **caps(top_ns, bottom_ns)}, light=light),
            cube((x0, y0, z1 - t), (x1, y1, z1), {"south": out_uv, "east": end, "west": end, **inner("north"), **caps(top_ns, bottom_ns)}, light=light),
            cube((x0, y0, z0 + t), (x0 + t, y1, z1 - t), {"west": out_uv, **inner("east"), **caps(top_ew, bottom_ew)}, light=light),
            cube((x1 - t, y0, z0 + t), (x1, y1, z1 - t), {"east": out_uv, **inner("west"), **caps(top_ew, bottom_ew)}, light=light)]


def column(x, z, y0, y1, width, uv_side, uv_end=None, rot=None, light=None, ends=("up",)):
    """A square column (a stem, a finger, a stalk) centred on (x, z)."""
    h = width / 2
    faces = {side: uv_side for side in SIDES4}
    for end in ends:
        faces[end] = uv_end or uv_side
    return cube((x - h, y0, z - h), (x + h, y1, z + h), faces, rot, light)


DIRECTIONS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
TURN = {"north": 0, "east": 90, "south": 180, "west": 270}


def leaf_out(sculpt, key, painter, base, direction, length, width, tilt, light=None, texel_scale=1.0, tex=None):
    """A level leaf (or petal) from `base` pointing `direction`, tilted up by `tilt` degrees (down if negative),
    painted tip-up by `painter` and turned to point its way. `tex` fixes the piece's size in texels (w, h), so leaves
    of several sizes can share one picture."""
    dx, dz = DIRECTIONS[direction]
    w = max(2, round(width * TEXELS * texel_scale))
    h = max(2, round(length * TEXELS * texel_scale))
    if tex:
        w, h = tex
    uv = sculpt.atlas.piece(f"{key}@{direction}", w, h, painter, turn=TURN[direction])
    bx, by, bz = base
    if dx:
        x0, x1 = (bx, bx + length) if dx > 0 else (bx - length, bx)
        z0, z1 = bz - width / 2, bz + width / 2
        rot = rotation(base, "z", tilt * dx)
    else:
        z0, z1 = (bz, bz + length) if dz > 0 else (bz - length, bz)
        x0, x1 = bx - width / 2, bx + width / 2
        rot = rotation(base, "x", -tilt * dz)
    return plane_xz(x0, x1, z0, z1, by, uv, rot, light)


def leaf_flat(sculpt, key, painter, base, diagonal, length, width, light=None, tex=None):
    """A level leaf from `base` pointing between two sides (`diagonal`: "ne", "se", "sw" or "nw"), lying flat (an
    element turned 45 degrees about y cannot also tilt)."""
    w = max(2, round(width * TEXELS))
    h = max(2, round(length * TEXELS))
    if tex:
        w, h = tex
    uv = sculpt.atlas.piece(f"{key}@east", w, h, painter, turn=90)
    bx, by, bz = base
    # Laid pointing east, then turned about y round its base: +45 turns east toward north.
    angle = {"ne": 45, "se": -45, "sw": 45, "nw": -45}[diagonal]
    if diagonal in ("ne", "se"):
        return plane_xz(bx, bx + length, bz - width / 2, bz + width / 2, by, uv, rotation(base, "y", angle), light)
    uv_w = sculpt.atlas.piece(f"{key}@west", w, h, painter, turn=270)
    return plane_xz(bx - length, bx, bz - width / 2, bz + width / 2, by, uv_w, rotation(base, "y", angle), light)


def upright(sculpt, key, painter, centre, width, y0, y1, yaw=0, lean=0, lean_axis=None, light=None, w_tex=None, h_tex=None):
    """An upright sheet centred on `centre` (x, z), facing south before `yaw` (0, 22.5, 45, 90, ...), or leaning by
    `lean` degrees about its own bottom edge."""
    cx, cz = centre
    w = w_tex or max(2, round(width * TEXELS))
    h = h_tex or max(2, round((y1 - y0) * TEXELS))
    uv = sculpt.atlas.piece(key, w, h, painter)
    yaw = yaw % 180
    if yaw >= 67.5 + 1e-6 and yaw < 157.5:
        # Across z, turned what is left of the yaw past 90.
        rest = yaw - 90
        rot = rotation((cx, y0, cz), "y", rest) if rest else (rotation((cx, y0, cz), "z", -lean) if lean else None)
        return plane_zy(cz - width / 2, cz + width / 2, y0, y1, cx, uv, rot, light)
    rest = yaw if yaw < 67.5 else yaw - 180
    rot = rotation((cx, y0, cz), "y", rest) if rest else (rotation((cx, y0, cz), "x", lean) if lean else None)
    return plane_xy(cx - width / 2, cx + width / 2, y0, y1, cz, uv, rot, light)


def segment(base, length, axis=None, angle=0, width=1.0, uv=None, light=None, ends=("up",)):
    """A stem segment from `base` up `length`, leaning by `angle` about `axis` ("x" leans toward +z for positive
    angles; "z" leans toward -x). Returns (element, top point)."""
    bx, by, bz = base
    e = column(bx, bz, by, by + length, width, uv, rot=rotation(base, axis, angle) if axis else None, light=light, ends=ends)
    a = math.radians(angle)
    if axis == "x":
        top = (bx, by + length * math.cos(a), bz + length * math.sin(a))
    elif axis == "z":
        top = (bx - length * math.sin(a), by + length * math.cos(a), bz)
    else:
        top = (bx, by + length, bz)
    return e, top


class Sculpt:
    """A plant being built: its texture's pieces and its models' elements (several models may share the pieces). Its
    texture is 64 x 64 unless `size` asks for a bigger one (128 for a detailed prop)."""

    def __init__(self, name, seed, size=SIZE):
        self.name = name
        self.atlas = Atlas(size)
        self.rng = random.Random(seed)
        self.models = {}

    def piece(self, key, w, h, painter, turn=0):
        return self.atlas.piece(key, w, h, painter, turn)


# ---------------------------------------------------------------- model files

PLANT_DISPLAY = {
    "gui": {"rotation": [25, 225, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2.5, 1.5], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
}
TALL_DISPLAY = {
    "gui": {"rotation": [25, 225, 0], "translation": [0, -3.5, 0], "scale": [0.52, 0.52, 0.52]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.3, 0.3, 0.3]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -4, 0], "scale": [0.48, 0.48, 0.48]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 8, 7], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0.5, 1.5], "scale": [0.35, 0.35, 0.35]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0], "scale": [0.38, 0.38, 0.38]},
}


def model(name, elements, display=None):
    """A block model of `elements` drawing on the plant's texture (block/<name>), without ambient occlusion."""
    out = {"parent": "minecraft:block/block", "ambientocclusion": False,
           "textures": {"p": f"{MOD}:block/{name}", "particle": f"{MOD}:block/{name}"}, "elements": elements}
    if display:
        out["display"] = display
    return out


def opaque_boxes(img):
    """For model_writer.finish_closed: whether every face of an element reads only fully opaque texels of the Sculpt
    texture `img` (its "#p"), as art_check O1 judges it. A see-through box (glass, a web) may leave faces out."""
    alpha = img.convert("RGBA").getchannel("A")
    w, h = img.size

    def opaque(e):
        for spec in e.get("faces", {}).values():
            if spec.get("texture") != "#p" or "uv" not in spec:
                return False
            us, vs = (spec["uv"][0] / 16, spec["uv"][2] / 16), (spec["uv"][1] / 16, spec["uv"][3] / 16)
            shift_u, shift_v = math.floor(min(us) + 1e-6), math.floor(min(vs) + 1e-6)
            x0 = max(0, int((min(us) - shift_u) * w + 0.01))  # within 0.01 texel of a texel line: on it (art_check)
            x1 = min(w, max(x0 + 1, int(math.ceil((max(us) - shift_u) * w - 0.01))))
            y0 = max(0, int((min(vs) - shift_v) * h + 0.01))
            y1 = min(h, max(y0 + 1, int(math.ceil((max(vs) - shift_v) * h - 0.01))))
            if x1 > x0 and y1 > y0 and alpha.crop((x0, y0, x1, y1)).getextrema()[0] < 255:
                return False
        return True
    return opaque


def closing_writer(write, image_of):
    """write(path, obj) for a Sculpt set's block and item models that draws every face its boxes leave out where
    nothing covers it (model_writer.finish_closed; docs/ART_DIRECTION.md, Closed geometry). image_of(name) is the
    texture a model's "#p" names (jugcraft:block/<name>), or None to write the model as it is."""
    import model_writer

    def closed(path, obj):
        ref = obj.get("textures", {}).get("p", "") if isinstance(obj, dict) and obj.get("elements") else ""
        img = image_of(ref.split("/", 1)[1]) if isinstance(ref, str) and ref.startswith(f"{MOD}:block/") else None
        if img is not None:
            obj = dict(obj, elements=list(obj["elements"]))
            model_writer.finish_closed(obj["elements"], opaque_boxes(img))
        return write(path, obj)
    return closed


def transformed(elements, scale=1.0, offset=(0, 0, 0), centre=(8, 0, 8)):
    """`elements` scaled about `centre` and moved by `offset` (the turns keep their angles)."""
    out = []
    for e in elements:
        def tf(p):
            return [_r(centre[k] + (p[k] - centre[k]) * scale + offset[k]) for k in range(3)]
        copy = dict(e)
        copy["from"], copy["to"] = tf(e["from"]), tf(e["to"])
        if "rotation" in e:
            copy["rotation"] = dict(e["rotation"], origin=tf(e["rotation"]["origin"]))
        out.append(copy)
    return out


def flower_pot():
    """A flower pot of five boxes on vanilla's pot texture, filled with vanilla's dirt (both by reference)."""
    def box(lo, hi, tex, sides=("north", "south", "east", "west", "up", "down")):
        faces = {}
        for side in sides:
            x0, y0, z0 = lo
            x1, y1, z1 = hi
            uv = {"north": (16 - x1, 16 - y1, 16 - x0, 16 - y0), "south": (x0, 16 - y1, x1, 16 - y0),
                  "east": (16 - z1, 16 - y1, 16 - z0, 16 - y0), "west": (z0, 16 - y1, z1, 16 - y0),
                  "up": (x0, z0, x1, z1), "down": (x0, 16 - z1, x1, 16 - z0)}[side]
            faces[side] = {"uv": _uv(uv), "texture": tex}
        return {"from": list(lo), "to": list(hi), "faces": faces}
    return [box((5, 0, 5), (6, 6, 11), "#pot"), box((10, 0, 5), (11, 6, 11), "#pot"), box((6, 0, 5), (10, 6, 6), "#pot"),
            box((6, 0, 10), (10, 6, 11), "#pot"), box((6, 0, 6), (10, 4, 10), "#dirt", ("up",))]


def potted_model(name, elements, scale=0.62):
    out = model(name, flower_pot() + transformed(elements, scale, (0, 4, 0)))
    out["textures"].update({"pot": "minecraft:block/flower_pot", "dirt": "minecraft:block/dirt"})
    return out
