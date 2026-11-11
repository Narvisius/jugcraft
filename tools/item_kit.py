"""Items built as real 3D parts out of boxes (the NT-7's way, made general; first used for the plasma baton, r203): parts
in the item's own frame (x across, y up, z along it; px, a sixteenth of a block), round sections as eight-sided prisms,
each face painted once into one texture.

The item's model space: model = (z + gx, y + 8, 8 - x): its length along +x, up +y, +x of its own frame north. A turn
about the item's length is a turn about the model's x by the same angle.

Nothing here can z-fight (upstream's rule, tools/zfight.py: no two faces facing the same way within 0.09 px of each other
and overlapping):
- a prism with no end showing is four boxes (two of them turned 45 degrees), only their outer faces drawn: the eight
  sides of the octagon, no two in one plane;
- a prism with an end showing (a cap) is seven: a cross of three that don't overlap (their ends lie in one plane side by
  side), and four corner pieces turned 45 degrees whose ends stand RECESS back from the cross's, so no cap lies on
  another; the faces of the cross the recess lays bare are drawn too;
- sections meet end to end (lathe: the larger of two neighbours carries the cap between them); a sleeve round another
  section (a rib, a ring) must stand at least 0.1 px proud of it.
check(elements) runs tools/zfight.py over a model as written.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

import numpy as np

SIDES = ('+x', '-x', '+y', '-y', '+z', '-z')
FACE_OF = {'+x': 'north', '-x': 'south', '+y': 'up', '-y': 'down', '+z': 'east', '-z': 'west'}
RECESS = 0.11                               # a capped prism's corner pieces end this far short of its cross (px)


@dataclass
class Part:
    name: str
    lo: tuple
    hi: tuple
    faces: dict                             # side: (key, painter); only the sides given are drawn
    turn: float = 0.0                       # degrees about the item's length (x towards y), about the line through `about`
    about: tuple = (0.0, 0.0)
    glow: bool = False
    tpx: int = 8


class Kit:
    def __init__(self, tpx=8):
        self.parts: list[Part] = []
        self.tpx = tpx

    def box(self, lo, hi, faces, turn=0.0, about=(0.0, 0.0), glow=False, name='part', tpx=None, only=None):
        """A box; faces: {side or '*': (key, painter)}; only: the sides drawn (all six otherwise)."""
        sides = only or SIDES
        f = {s: faces.get(s, faces.get('*')) for s in sides}
        self.parts.append(Part(name, tuple(lo), tuple(hi), f, float(turn), tuple(about), glow, tpx or self.tpx))

    def prism(self, z0, z1, d, side, caps=(None, None), glow=False, name='part', tpx=None):
        """An eight-sided section d across its flats from z0 to z1; side: (key, painter) for its eight faces; caps: the
        (key, flat painter) of its end at z0 and at z1, None where another part covers the end."""
        s = d * math.tan(math.pi / 8)
        h = d / 2
        t = tpx or self.tpx
        if not caps[0] and not caps[1]:
            for turn, wide in ((0.0, 'x'), (0.0, 'y'), (45.0, 'x'), (-45.0, 'x')):
                if wide == 'x':
                    lo, hi, outer = (-h, -s / 2, z0), (h, s / 2, z1), ('+x', '-x')
                else:
                    lo, hi, outer = (-s / 2, -h, z0), (s / 2, h, z1), ('+y', '-y')
                self.parts.append(Part(name, lo, hi, {o: side for o in outer}, turn, (0.0, 0.0), glow, t))
            return
        # long enough that a recessed end is clear of the cap of whatever stands behind it, and of its own other end
        # (one end capped: the neighbour at the other end is larger and its cap faces the same way as the recessed one)
        need = 2 * RECESS + 0.02 if caps[0] and caps[1] else RECESS + 0.1
        assert z1 - z0 >= need - 1e-9, "item kit: the capped section %s is %.2f long; it needs %.2f" % (name, z1 - z0, need)
        inner = caps[0] or caps[1]                     # what the recess lays bare: the cap's colour

        def ends(f):
            if caps[0]:
                f['-z'] = caps[0]
            if caps[1]:
                f['+z'] = caps[1]
            return f
        # the cross: a bar across, a piece over it and one under it (their ends side by side in one plane)
        self.parts.append(Part(name, (-h, -s / 2, z0), (h, s / 2, z1), ends({'+x': side, '-x': side, '+y': inner, '-y': inner}), 0.0, (0.0, 0.0), glow, t))
        self.parts.append(Part(name, (-s / 2, s / 2, z0), (s / 2, h, z1), ends({'+y': side, '+x': inner, '-x': inner}), 0.0, (0.0, 0.0), glow, t))
        self.parts.append(Part(name, (-s / 2, -h, z0), (s / 2, -s / 2, z1), ends({'-y': side, '+x': inner, '-x': inner}), 0.0, (0.0, 0.0), glow, t))
        # the four corner pieces, their ends recessed
        a, b = z0 + (RECESS if caps[0] else 0.0), z1 - (RECESS if caps[1] else 0.0)
        for turn in (45.0, -45.0):
            self.parts.append(Part(name, (s / 2, -s / 2, a), (h, s / 2, b), ends({'+x': side}), turn, (0.0, 0.0), glow, t))
            self.parts.append(Part(name, (-h, -s / 2, a), (-s / 2, s / 2, b), ends({'-x': side}), turn, (0.0, 0.0), glow, t))

    def lathe(self, sections):
        """Sections end to end along the item: (z0, z1, d, side, cap, glow, name), cap the (key, flat painter) of its
        ends where they show. The larger of two neighbours carries the cap between them; the first and last their ends."""
        for i, (z0, z1, d, side, cap, glow, name) in enumerate(sections):
            before = sections[i - 1][2] if i else 0.0
            after = sections[i + 1][2] if i + 1 < len(sections) else 0.0
            self.prism(z0, z1, d, side, (cap if d > before else None, cap if d > after else None), glow=glow, name=name)


def face_dims(p: Part, side):
    """A face's (width, height) in px as its painter sees it: the sides along the item then across; the ends across (x)
    then up (y)."""
    (x0, y0, z0), (x1, y1, z1) = p.lo, p.hi
    dx, dy, dz = x1 - x0, y1 - y0, z1 - z0
    return {'+x': (dz, dy), '-x': (dz, dy), '+y': (dz, dx), '-y': (dz, dx), '+z': (dx, dy), '-z': (dx, dy)}[side]


class Atlas:
    """The item's texture: each painted face once (the same key and size shares its texels), packed in rows."""

    def __init__(self, width=128, height=1024):
        self.w = width
        self.img = np.zeros((height, width, 4), np.float32)
        self.x = self.y = self.row = 0
        self.at = {}

    def place(self, key, a):
        H, W = a.shape[:2]
        k = (key, W, H)
        if k in self.at:
            return self.at[k]
        if self.x + W > self.w:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        self.img[self.y:self.y + H, self.x:self.x + W] = a
        self.at[k] = (self.x, self.y)
        self.x += W
        self.row = max(self.row, H)
        return self.at[k]

    @property
    def used(self):
        return self.y + self.row


def painted(p: Part, side):
    """The face's texels as Minecraft lays them on it (top-left as seen from outside), and the key they're kept under."""
    key, fn = p.faces[side]
    fw, fh = face_dims(p, side)
    W, H = max(1, round(fw * p.tpx)), max(1, round(fh * p.tpx))
    if getattr(fn, 'flat', None) is not None:    # one colour: a swatch does for a face of any size
        W = H = 2
        key = 'flat:%s' % (fn.flat,)
    a = np.ones((H, W, 4), np.float32)
    fn(a)
    if side in ('+x', '-z'):                 # these faces run the other way across: the painter's left is their right
        b = a[:, ::-1]
        if not np.array_equal(a, b):
            return key + '~', np.ascontiguousarray(b)
    return key, a


def to_model(p, gx):
    x, y, z = p
    return (z + gx, y + 8, 8 - x)


def elements(parts, atlas: Atlas, size, gx):
    """The model's elements (uv in sixteenths of the texture: size = its (width, height) in texels)."""
    out = []
    for p in parts:
        lo, hi = to_model(p.lo, gx), to_model(p.hi, gx)
        frm = [round(min(a, b), 4) for a, b in zip(lo, hi)]
        to = [round(max(a, b), 4) for a, b in zip(lo, hi)]
        faces = {}
        for side in SIDES:
            if side not in p.faces:
                continue
            key, a = painted(p, side)
            x, y = atlas.place(key, a)
            H, W = a.shape[:2]
            faces[FACE_OF[side]] = {"uv": [round(x * 16 / size[0], 5), round(y * 16 / size[1], 5), round((x + W) * 16 / size[0], 5),
                                           round((y + H) * 16 / size[1], 5)], "texture": "#0"}
        e = {"name": p.name, "from": frm, "to": to, "faces": faces}
        if p.turn:
            ox, oy = p.about
            e["rotation"] = {"origin": [round(v, 4) for v in to_model((ox, oy, (p.lo[2] + p.hi[2]) / 2), gx)], "axis": "x", "angle": round(p.turn, 4)}
        if p.glow:
            e["light_emission"] = 15
        out.append(e)
    return out


def check(elements):
    """The model's faces lying in one plane on one another (tools/zfight.py's rule): must be none."""
    import zfight
    hits = zfight.overlaps(elements)
    assert not hits, "item kit: %d pairs of faces lie on one another, e.g. %s" % (len(hits), hits[:3])


def extents(parts):
    lo = np.min([p.lo for p in parts], axis=0)
    hi = np.max([p.hi for p in parts], axis=0)
    return lo, hi


# ------------------------------------------------------------------ painters: (H, W, 4) floats; W runs along the item to its tip
def flat(rgb, alpha=1.0):
    def f(a):
        a[..., :3] = rgb
        a[..., 3] = alpha
    f.flat = (tuple(round(float(c), 4) for c in rgb), alpha)
    return f


def turned(rgb, ends=0.16, grad=0.0, lines=(), bands=(), speck=0.0, seed=0, lip=0.0):
    """A turned or moulded round part: its two ends a touch darker (the shadow where it meets the next part), an edge
    catching the light at its tip end (lip), scribed lines round it (fractions along it), painted bands
    ((from, to, rgb) fractions), a fine speckle (cast metal, rubber)."""
    def f(a):
        H, W = a.shape[:2]
        base = np.array(rgb, np.float32)
        a[..., :3] = base
        if grad:
            a[..., :3] *= np.linspace(1 + grad / 2, 1 - grad / 2, W)[None, :, None]
        for f0, f1, c in bands:
            a[:, int(round(f0 * W)):max(int(round(f0 * W)) + 1, int(round(f1 * W))), :3] = c
        if speck:
            r = np.random.default_rng(seed + W * 31 + H)
            a[..., :3] *= 1 + r.uniform(-speck, speck, (H, W, 1)).astype(np.float32)
        for fr in lines:
            x = min(W - 1, int(round(fr * (W - 1))))
            a[:, x, :3] = base * 0.45
            if x + 1 < W:
                a[:, x + 1, :3] = np.clip(base * 1.18, 0, 1)
        if W > 3 and ends:
            a[:, 0, :3] *= 1 - ends
            a[:, -1, :3] *= 1 - ends
        if W > 4 and lip:
            a[:, -2, :3] = np.clip(a[:, -2, :3] * (1 + lip) + 0.02, 0, 1)
            a[:, 1, :3] = np.clip(a[:, 1, :3] * (1 + lip * 0.6) + 0.01, 0, 1)
        a[..., :3] = np.clip(a[..., :3], 0, 1)
    return f


def knurl(rgb, depth=0.22):
    """A knurled grip: a diamond pattern cut into it."""
    def f(a):
        H, W = a.shape[:2]
        y, x = np.mgrid[0:H, 0:W]
        on = ((x + y) % 2 == 0)
        base = np.array(rgb, np.float32)
        a[..., :3] = np.where(on[..., None], base * (1 + depth), base * (1 - depth))
    return f


def ribbed(rgb, period=4, groove=1, depth=0.45, lift=0.15):
    """Rings moulded round it: every `period` texels a groove `groove` wide, the texel after it catching the light."""
    def f(a):
        H, W = a.shape[:2]
        base = np.array(rgb, np.float32)
        a[..., :3] = base
        for x in range(W):
            m = x % period
            if m < groove:
                a[:, x, :3] = base * (1 - depth)
            elif m == groove:
                a[:, x, :3] = np.clip(base * (1 + lift) + 0.01, 0, 1)
    return f


def slotted(rgb, inner, margin=0.18, lit=None):
    """A slot cut along the face's middle (a vent): dark inside (or lit), a shaded lip."""
    def f(a):
        H, W = a.shape[:2]
        base = np.array(rgb, np.float32)
        a[..., :3] = base
        x0, x1 = int(round(margin * W)), W - int(round(margin * W))
        y0, y1 = max(1, H // 2 - max(1, H // 5)), min(H - 1, H // 2 + max(1, H // 5))
        if y1 <= y0:
            y0, y1 = H // 2, H // 2 + 1
        a[y0:y1, x0:x1, :3] = inner if lit is None else lit
        if y0 - 1 >= 0:
            a[y0 - 1, x0:x1, :3] = base * 0.7
        a[:, 0, :3] *= 0.84
        a[:, -1, :3] *= 0.84
    return f


def dots(rgb, lit, n=3, size=1):
    """A row of small lamps along the face."""
    def f(a):
        H, W = a.shape[:2]
        a[..., :3] = rgb
        for i in range(n):
            x = int(round((i + 0.5) * W / n - size / 2))
            y = max(0, H // 2 - size // 2 - (0 if size > 1 else 0))
            a[y:y + size, x:x + size, :3] = lit[i] if isinstance(lit, list) else lit
    return f
