"""Paints the atlas of a 3D armor set (tools/armor_models.py): every face of every part in its own texel region, one
texel per model pixel, so the look stays chunky.

Every colour comes from the set's palette, by name: a painter only ever says "light", "seam" or "leather_dark", so
the bronze variant of a steel set is the same geometry and paint with BRONZE in place of STEEL (plus any accent
changes). The names, in ramps from light to dark (shade() steps along a ramp):
    metal      light, mid_light, mid, dark, seam, void
    leather    leather_light, leather_mid_light, leather_mid, leather_dark, leather_darkest
    under      under_light, under_mid, under_dark, under_darkest     (the dark padded layer under the plates)
    gold       gold_light, gold_dark                                  (trim: brass in BRONZE)

A part's paint is one spec for the whole box, or a dict of specs by face ("front", "sides", "ends", "*" for the rest;
vanilla names too). A spec is a painter name, P(name, **options), or a list of them painted in order (a plate, then
rivets on it). With a whole-box spec, faces one texel thin (the edges of a plate) get a plain edge instead of a
squeezed pattern.

Painters (options in brackets):
    plate     a hammered plate: the fill broken into runs of a tone lighter or darker, staggered like brickwork, with
              a lighter top row and darker bottom row [tone, strips "h"/"v", bevel, border]
    chevron   nested L's (or V's) one texel wide about one corner, a light border and a mottled 2x2 core at the far
              corner [corner "bl"/"br"/"tl"/"tr"/"in"/"out"/"v"/"^", bands, border, core, outside]
    lames     horizontal plates `rows` texels tall, each a lighter top row over darker ones, one tone darker at the
              centre line [rows, tones, centre "middle"/"in"/None]
    leather   leather fill with a light top row [tone, rim]
    strap     a leather strap with holes and an optional buckle [tone, rim, holes, buckle]
    gold      gold trim [tone, rim]
    under     the dark under-layer in 2x2 mottles with a light top edge
    rivets    a row of 1-texel rivets or 2x2 bolts lit at the top left [row, every, offset, style, tone]
    hammer    a layer over another: long runs of one tone broken by 2-3 texels a tone lighter or darker, staggered
              like brickwork, so nested L's read as the owner's hammered strips; the outer ring stays whole
              [keep_border, every]
    marks     rectangles of one tone (eye slits, breaths, holes; "clear" cuts holes in a cutout part)
              [rects (x, y, w, h[, tone]) with negative x/y from the far edge, tone, symmetric]
    solid     one tone [tone];  edge   a plain edge [tone]
    test      the smoke test's face colours, a marker at the texture's top-left and the face's letter

Texture space on each face: row 0 is the top. On the four sides the texture's top is the visual top and texture-left is
the viewer's left looking at that face from outside (the front's left is the model's right). On the top face the
texture's top is the back; on the bottom face, the back too. A painter knows the face's `inward` side (+1 when
texture-right points toward the body's centre line, -1 when texture-left does, 0 when neither), so a pattern can point
inward on both mirrored halves.
"""
import random
import zlib
from dataclasses import dataclass, replace

import numpy as np
from PIL import Image

import armor_models as am

METAL = ("light", "mid_light", "mid", "dark", "seam", "void")
LEATHER = ("leather_light", "leather_mid_light", "leather_mid", "leather_dark", "leather_darkest")
UNDER = ("under_light", "under_mid", "under_dark", "under_darkest")
GOLD = ("gold_light", "gold_dark")
RAMPS = (METAL, LEATHER, UNDER, GOLD)

# The owner's steel knight design, sampled exactly (its render is unlit, so these are its texture colours).
STEEL = {
    "light": (180, 190, 192), "mid_light": (152, 161, 166), "mid": (126, 135, 144), "dark": (103, 112, 121),
    "seam": (81, 87, 99), "void": (65, 68, 75),
    "gold_light": (166, 131, 72), "gold_dark": (147, 114, 62),
    "leather_light": (90, 73, 66), "leather_mid_light": (77, 64, 59), "leather_mid": (71, 60, 58),
    "leather_dark": (55, 49, 49), "leather_darkest": (41, 37, 36),
    "under_light": (75, 78, 87), "under_mid": (62, 63, 68), "under_dark": (49, 52, 59), "under_darkest": (38, 39, 43),
}
# Bronze, the knight's steam-age make: a warm copper-bronze ramp with the steel's six steps, so the design's light top
# and dark skirt survive, but hue-shifted as metal is (golden highlights, coppery red shadows) and a little more
# contrast at the ends than the steel, or the hammered strips read as wood grain. It sits between tools/arms_pixel.py's
# BRONZE (mid near its mid, the darker steps between its own) and the owner's approved bronze ramp of the material
# sets (3e2410 7e5222 b4803c dcaa5c f6d696: the light step falls between its light and highlight).
# "gold" is brass, lighter than the bronze's light and yellower (near arms_pixel's BRASS), so the trim reads on it.
# The leather is darker and redder than the steel's; the under-layer is the steel's slate, which sets off both.
BRONZE = {**STEEL,
          "light": (225, 182, 107), "mid_light": (202, 139, 72), "mid": (174, 109, 55), "dark": (148, 83, 43),
          "seam": (121, 62, 32), "void": (89, 45, 26),
          "gold_light": (236, 204, 96), "gold_dark": (196, 160, 60),
          "leather_light": (82, 56, 42), "leather_mid_light": (70, 48, 36), "leather_mid": (62, 42, 32),
          "leather_dark": (46, 31, 25), "leather_darkest": (32, 22, 18)}
# Bloodthorn, the owner's crimson design (tools/bloodthorn_armor.py), sampled from their render. That render is lit as
# Blockbench lights a model (front and back faces 0.8, sides 0.6, tops 1.0: its side faces measure 0.75 of the fronts,
# and a lit top edge 1.22), so each tone is the median of its texel interiors on front and back faces divided by 0.8.
# Its metal runs orange, coral, red, crimson, magenta, plum and dark plum (bright at the top of the armor, dark at the
# feet); the ramp keeps six of them and folds the red, the rarest, into coral and crimson. The near-black purple
# under-layer is the design's own, with a darker step added for the eye slits. The design has no leather or gold:
# "gold" names its red, for trim (the owner's red between coral and crimson), and the leathers are a dark wine ramp
# for straps, unused by the armor itself.
BLOODTHORN = {
    "light": (239, 92, 70), "mid_light": (232, 71, 72), "mid": (178, 34, 76), "dark": (150, 34, 89),
    "seam": (109, 28, 78), "void": (80, 21, 64),
    "gold_light": (212, 49, 71), "gold_dark": (176, 38, 62),
    "leather_light": (110, 42, 58), "leather_mid_light": (94, 35, 52), "leather_mid": (80, 29, 46),
    "leather_dark": (62, 23, 38), "leather_darkest": (44, 17, 29),
    "under_light": (66, 55, 81), "under_mid": (44, 42, 55), "under_dark": (32, 31, 39), "under_darkest": (23, 21, 29),
}
# Reforged White Diamond, the owner's icy design (tools/white_diamond_armor.py), sampled from their render. That render
# is unlit: each tone shows at one value on faces of every direction, and no darker copy of a light tone occurs (none of
# its texel interiors is 0.5 to 0.9 of one, as a lit side, bottom or front face would be), so these are its texture
# colours as sampled, as the steel knight's are. Its metal is a hue-shifted ramp of six tones, one per step: icy white,
# pale cyan, light cyan, light blue, blue and lavender. The under-layer is its charcoal, with the grey that lights the
# arms. The design has no leather or gold: "gold" names its blue and lavender, for trim, and the leathers are a slate
# ramp between the charcoal and the lavender for straps, unused by the armor itself.
WHITE_DIAMOND = {
    "light": (243, 255, 253), "mid_light": (204, 240, 240), "mid": (165, 218, 226), "dark": (140, 184, 213),
    "seam": (120, 146, 197), "void": (113, 113, 177),
    "gold_light": (120, 146, 197), "gold_dark": (113, 113, 177),
    "leather_light": (98, 104, 134), "leather_mid_light": (86, 90, 118), "leather_mid": (76, 79, 104),
    "leather_dark": (63, 64, 86), "leather_darkest": (50, 50, 68),
    "under_light": (82, 92, 102), "under_mid": (68, 74, 86), "under_dark": (58, 61, 70), "under_darkest": (48, 49, 55),
}
# The smoke test's face colours (tools/armor_smoke.py): one hue per face, so a render shows which face is where.
TEST = {**STEEL, "t_top": (230, 230, 90), "t_bottom": (90, 70, 40), "t_right": (220, 70, 70), "t_front": (80, 200, 90),
        "t_left": (70, 110, 230), "t_back": (200, 90, 210), "t_mark": (20, 20, 20), "t_rule": (255, 255, 255)}


def P(kind, **options):
    """A paint spec: painter `kind` with its options."""
    if kind not in PAINTERS:
        raise ValueError(f"unknown painter {kind!r}: use one of {sorted(PAINTERS)}")
    return (kind, options)


def shade(name, steps):
    """The tone `steps` darker (negative: lighter) along its ramp, held at the ends."""
    for ramp in RAMPS:
        if name in ramp:
            return ramp[max(0, min(len(ramp) - 1, ramp.index(name) + steps))]
    return name


@dataclass
class Face:
    """What a painter knows about the face it paints."""
    item: str
    bone: str
    part: object
    name: str           # top, bottom, right, front, left, back
    w: int              # texels
    h: int
    du: tuple           # bone-space direction of texture-right on the placed part
    dv: tuple           # bone-space direction of texture-down
    inward: int         # +1: texture-right points toward the body's centre line; -1: texture-left; 0: neither
    seed: int

    def rng(self, salt=""):
        return random.Random(self.seed ^ zlib.crc32(salt.encode()))


# ---------------------------------------------------------------- painters: each fills g (h x w tone names) in place
def _runs(rng, n, base, alt, run=(3, 6), alt_run=(2, 3)):
    """A row of n tones: runs of `base` 3-6 long broken by runs of `alt` 2-3 long, from a random phase."""
    out, use_alt, first = [], rng.random() < 0.4, True
    while len(out) < n:
        lo, hi = alt_run if use_alt else run
        length = rng.randint(lo, hi)
        if first:   # start part-way through a run, so rows stagger like brickwork
            length, first = rng.randint(1, length), False
        out.extend([alt if use_alt else base] * length)
        use_alt = not use_alt
    return out[:n]


def plate(g, f, tone="mid_light", strips="h", bevel=True, border=None):
    h, w = g.shape
    rng = f.rng("plate")
    g[:, :] = tone
    lines = range(h) if strips == "h" else range(w)
    for i in lines:
        alt = shade(tone, -1 if (i + rng.randint(0, 1)) % 2 else 1)
        row = _runs(rng, w if strips == "h" else h, tone, alt)
        if strips == "h":
            g[i, :] = row
        else:
            g[:, i] = row
    if bevel and h >= 3:
        g[0, :] = _runs(rng, w, shade(tone, -1), tone, run=(4, 8), alt_run=(1, 2))
        g[-1, :] = _runs(rng, w, shade(tone, 1), shade(tone, 2), run=(4, 8), alt_run=(2, 2))
    if border:
        g[0, :] = g[-1, :] = border
        g[:, 0] = g[:, -1] = border


def _corner(f, corner):
    if corner in ("in", "out"):
        side = f.inward if corner == "in" else -f.inward
        return "br" if side > 0 else "bl"
    return corner


def chevron(g, f, corner="in", bands=("light", "mid", "dark", "mid"), border="light", core=("light", "mid_light"),
            outside="mid_light", thick=1):
    h, w = g.shape
    rng = f.rng("chevron")
    corner = _corner(f, corner)
    g[:, :] = outside
    inset = 1 if border and h > 2 and w > 2 else 0
    ih, iw = h - 2 * inset, w - 2 * inset
    for y in range(ih):
        for x in range(iw):
            if corner in ("v", "^"):
                up = ih - 1 - y if corner == "v" else y
                k = up - abs(2 * x - (iw - 1)) // 2   # an even width gets a two-texel point
            else:
                dx = x if corner[1] == "l" else iw - 1 - x
                dy = ih - 1 - y if corner[0] == "b" else y
                k = min(dx, dy)
            band = k // thick if k >= 0 else -1
            if band < 0:
                tone = outside
            elif band < len(bands):
                tone = bands[band]
            else:
                block = ((x // 2) + (y // 2) + rng.randint(0, 1)) % 2
                tone = core[block % len(core)]
            g[y + inset, x + inset] = tone
    if inset:
        g[0, :] = g[-1, :] = border
        g[:, 0] = g[:, -1] = border


SKIRT = (("mid_light", "dark"), ("mid", "mid"), ("mid_light", "seam"), ("mid", "dark"))   # the owner's four lames


def lames(g, f, rows=2, tones=SKIRT, centre="middle", lighter_ends=False):
    h, w = g.shape
    rng = f.rng("lames")
    for y in range(h):
        top, low = tones[(y // rows) % len(tones)]
        tone = top if y % rows == 0 else low
        g[y, :] = _runs(rng, w, tone, shade(tone, -1 if y % rows else 1), run=(3, 6), alt_run=(2, 3))
    if centre == "middle" and w >= 6:
        cols = range(w // 2 - 2 + w % 2, w // 2 + 2)
    elif centre == "in" and f.inward:
        cols = range(w - 2, w) if f.inward > 0 else range(0, 2)
    else:
        cols = ()
    for x in cols:
        for y in range(h):
            g[y, x] = shade(g[y, x], 1)
    if lighter_ends and w >= 6:
        for x in (0, w - 1):
            for y in range(h):
                g[y, x] = shade(g[y, x], -1)


def leather(g, f, tone="leather_mid", rim="leather_light"):
    h, w = g.shape
    rng = f.rng("leather")
    for y in range(h):
        g[y, :] = _runs(rng, w, tone, "leather_mid_light", run=(3, 7), alt_run=(2, 2))
    if rim and h >= 2:
        g[0, :] = rim
    if h >= 3 and w >= 3:
        g[-1, -1 if f.inward > 0 else 0] = "leather_darkest"


def strap(g, f, tone="leather_dark", rim="leather_mid", holes=4, hole="leather_darkest", buckle=None):
    h, w = g.shape
    g[:, :] = tone
    if rim and h >= 2:
        g[0, :] = rim
    if holes:
        y = h // 2 if h >= 2 else 0
        for x in range(holes // 2, w, holes):
            g[y, x] = hole
    if buckle and w >= 3:
        x0 = w // 2 - 1 if buckle == "centre" else int(buckle)
        g[:, x0:x0 + 3] = "gold_light"
        if h >= 3:
            g[1:-1, x0 + 1] = "gold_dark"


def gold(g, f, tone="gold_dark", rim="gold_light"):
    g[:, :] = tone
    if rim and g.shape[0] >= 2:
        g[0, :] = rim


def under(g, f, light="under_light"):
    h, w = g.shape
    rng = f.rng("under")
    for y in range(h):
        for x in range(w):
            block = ((x // 2) * 7 + (y // 2) * 3 + rng.randint(0, 2)) % 3
            g[y, x] = ("under_dark", "under_mid", "under_dark")[block]
    for y in range(1, h, 3):
        x = rng.randrange(w)
        if g[y - 1, x] != "under_darkest":
            g[y, x] = "under_darkest"
    if light and h >= 2:
        g[0, :] = light


def rivets(g, f, row="middle", every=3, offset=1, style="rivet", tone="light"):
    h, w = g.shape
    y = {"top": 1, "middle": h // 2, "bottom": h - 2}.get(row, row) if isinstance(row, str) else row
    y = max(0, min(h - 1, y if y >= 0 else h + y))
    for x in range(offset, w, every):
        if style == "bolt":
            if x + 1 < w and y + 1 < h:
                g[y, x], g[y, x + 1] = tone, shade(tone, 1)
                g[y + 1, x], g[y + 1, x + 1] = shade(tone, 1), shade(tone, 3)
        else:
            g[y, x] = tone


def hammer(g, f, keep_border=True, every=5):
    h, w = g.shape
    rng = f.rng("hammer")
    src = g.copy()
    lo = 1 if keep_border and h > 2 and w > 2 else 0

    def runs(line):
        out, start = [], 0
        for i in range(1, len(line) + 1):
            if i == len(line) or line[i] != line[start]:
                out.append((start, i))
                start = i
        return out

    def broken(tone):
        up, down = shade(tone, -1), shade(tone, 1)
        if tone is None or (up == tone and down == tone):
            return tone
        return down if up == tone else up if down == tone else (up if rng.random() < 0.5 else down)

    # rows: a run of `every` or more gets one break, never at its ends, placed at random so rows stagger
    row_len = np.ones((h, w), dtype=int)
    for y in range(lo, h - lo):
        for s, e in runs(list(src[y, lo:w - lo])):
            row_len[y, lo + s:lo + e] = e - s
            n = e - s
            if n >= every and src[y, lo + s] is not None:
                k = 2 if n < 8 else rng.choice((2, 3))
                at = s + rng.randint(1, n - k - 1)
                g[y, lo + at:lo + at + k] = broken(src[y, lo + s])
    # columns: the same along one- and two-texel-wide upright bands (the upright arms of nested L's)
    for x in range(lo, w - lo):
        col = [src[y, x] if row_len[y, x] <= 2 else None for y in range(lo, h - lo)]
        for s, e in runs(col):
            n = e - s
            if n >= every and col[s] is not None:
                at = s + rng.randint(1, n - 3)
                g[lo + at:lo + at + 2, x] = broken(col[s])


def marks(g, f, rects=(), tone="void", symmetric=False):
    h, w = g.shape
    for rect in rects:
        x, y, rw, rh = rect[:4]
        t = rect[4] if len(rect) > 4 else tone
        x, y = x if x >= 0 else w + x, y if y >= 0 else h + y
        spans = [(x, x + rw)] + ([(w - x - rw, w - x)] if symmetric else [])
        for x0, x1 in spans:
            g[max(0, y):max(0, y + rh), max(0, x0):max(0, x1)] = None if t == "clear" else t


def solid(g, f, tone="mid_light"):
    g[:, :] = tone


def edge(g, f, tone="mid_light"):
    g[:, :] = tone
    if g.shape[0] >= 2:
        g[0, :] = shade(tone, -1)


GLYPHS = {"top": ("###", ".#.", ".#.", ".#.", ".#."), "bottom": ("#.#", "#.#", "#.#", "#.#", "###"),
          "right": ("##.", "#.#", "##.", "#.#", "#.#"), "front": ("###", "#..", "##.", "#..", "#.."),
          "left": ("#..", "#..", "#..", "#..", "###"), "back": ("##.", "#.#", "##.", "#.#", "##.")}


def test(g, f):
    h, w = g.shape
    g[:, :] = f"t_{f.name}"
    m = 2 if h >= 4 and w >= 4 else 1
    g[:m, :m] = "t_mark"
    if w > m + 1:
        g[0, m + 1:min(w, m + 4)] = "t_rule"
    if w >= 5 and h >= 7:
        x0, y0 = (w - 3) // 2, (h - 5) // 2 + 1
        for dy, line in enumerate(GLYPHS[f.name]):
            for dx, c in enumerate(line):
                if c == "#":
                    g[y0 + dy, x0 + dx] = "t_mark"


PAINTERS = {"plate": plate, "chevron": chevron, "lames": lames, "leather": leather, "strap": strap, "gold": gold,
            "under": under, "rivets": rivets, "hammer": hammer, "marks": marks, "solid": solid, "edge": edge,
            "test": test}
PATTERNED = {"plate", "chevron", "lames"}   # squeezed onto a one-texel edge they read as noise: an edge instead


def _layers(spec):
    if isinstance(spec, str):
        return [P(spec)]
    if isinstance(spec, tuple) and len(spec) == 2 and isinstance(spec[0], str) and isinstance(spec[1], dict):
        return [P(spec[0], **spec[1])]
    if isinstance(spec, list):
        return [layer for s in spec for layer in _layers(s)]
    raise ValueError(f"bad paint spec {spec!r}")


def face_layers(spec, face):
    """(layers, explicit) for one face: a dict spec is looked up by face name or alias, then "sides"/"ends", then
    "all"/"*"; explicit is False for a whole-box spec or the "*" fallback."""
    if not isinstance(spec, dict):
        return _layers(spec), False
    keys = {}
    for key, value in spec.items():
        for name in (am.GROUPS[key] if key in am.GROUPS else am.FACES if key == "*" else [am.face_name(key)]):
            rank = 0 if key not in am.GROUPS and key != "*" else 1 if key in ("sides", "ends") else 2
            if name not in keys or rank < keys[name][0]:
                keys[name] = (rank, value)
    if face not in keys:
        raise ValueError(f"paint spec {spec!r} has nothing for the {face} face (add '*')")
    rank, value = keys[face]
    return _layers(value), rank < 2


def _directions(part, density):
    """{face: (du, dv)} on the placed part, for every face (skipped ones too)."""
    out = {}
    for face, pts, uvs, _ in am.part_faces(replace(part, skip=frozenset()), density):
        du = dv = (0.0, 0.0, 0.0)
        for i in range(4):
            j = (i + 1) % 4
            (ui, vi), (uj, vj) = uvs[i], uvs[j]
            step = am._sub(pts[j], pts[i])
            if ui != uj and vi == vj:
                du = am._scale(step, 1 / (uj - ui))
            elif vi != vj and ui == uj:
                dv = am._scale(step, 1 / (vj - vi))
        out[face] = (du, dv)
    return out


def _inward(bone, part):
    if bone.startswith("right_"):
        return (1.0, 0.0, 0.0)
    if bone.startswith("left_"):
        return (-1.0, 0.0, 0.0)
    cx = am.place(part, part.centre)[0]
    return (-1.0 if cx > 0 else 1.0, 0.0, 0.0) if abs(cx) > 0.25 else (0.0, 0.0, 0.0)


def paint_face(spec, face):
    """The face's texels: an (h, w) array of tone names (None = see-through)."""
    g = np.full((face.h, face.w), "mid_light", dtype=object)
    layers, explicit = face_layers(spec, face.name)
    if not explicit and min(face.w, face.h) <= 1 and layers[0][0] in PATTERNED:
        layers = [P("edge", tone=layers[0][1].get("tone", "mid_light"))]
    for kind, options in layers:
        PAINTERS[kind](g, face, **options)
    return g


def sources(s):
    """{net: (item, bone, part)}: the part whose paint fills each net (the one it is named after, else the first)."""
    out = {}
    for item, bone, part in s.parts():
        current = out.get(part.key)
        if current is None or (part.name == part.key and current[2].name != part.key):
            out[part.key] = (item, bone, part)
    return out


def paint_grid(s):
    """(grid of tone names, mask of face texels) for the whole atlas."""
    nets, (width, height) = am.layout(s)
    grid = np.full((height, width), None, dtype=object)
    mask = np.zeros((height, width), dtype=bool)
    for key, (item, bone, part) in sources(s).items():
        u, v, w, h, d = nets[key]
        dirs = _directions(part, s.density)
        inward = _inward(bone, part)
        for name, (u0, v0, u1, v1) in am.face_rects(w, h, d).items():
            du, dv = dirs[name]
            side = am._dot(am._unit(du) if any(du) else du, inward)
            face = Face(item, bone, part, name, u1 - u0, v1 - v0, du, dv,
                        (1 if side > 0.5 else -1 if side < -0.5 else 0), zlib.crc32(f"{key}/{name}".encode()))
            grid[v + v0:v + v1, u + u0:u + u1] = paint_face(part.paint, face)
            mask[v + v0:v + v1, u + u0:u + u1] = True
    return grid, mask


def paint_atlas(s):
    """The set's atlas as an RGBA image. Texels round the faces copy their neighbour, so sampling at a face's very edge
    never picks up another part's colour."""
    grid, mask = paint_grid(s)
    height, width = grid.shape
    rgba = np.zeros((height, width, 4), dtype=np.uint8)
    for y in range(height):
        for x in range(width):
            name = grid[y, x]
            if name is not None:
                if name not in s.palette:
                    raise ValueError(f"{s.name}: tone {name!r} is not in the palette")
                rgba[y, x] = (*s.palette[name], 255)
    fill = rgba.copy()
    for y in range(height):
        for x in range(width):
            if mask[y, x]:
                continue
            for dy, dx in ((0, -1), (0, 1), (-1, 0), (1, 0), (-1, -1), (-1, 1), (1, -1), (1, 1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < height and 0 <= xx < width and mask[yy, xx] and rgba[yy, xx, 3]:
                    fill[y, x] = rgba[yy, xx]
                    break
    return Image.fromarray(fill, "RGBA")


def atlas_problems(s, image):
    """Messages for an atlas image that does not fit the set: the wrong size, or a see-through texel on a drawn face of
    a part that is not cutout."""
    nets, size = am.layout(s)
    if image.size != size:
        return [f"{s.name}: atlas is {image.size}, the layout needs {size}"]
    alpha = np.asarray(image.convert("RGBA"))[:, :, 3]
    out = []
    for item, bone, part in s.parts():
        if part.cutout:
            continue
        u, v, w, h, d = nets[part.key]
        for name, (u0, v0, u1, v1) in am.face_rects(w, h, d).items():
            if name not in part.skip and (alpha[v + v0:v + v1, u + u0:u + u1] < 255).any():
                out.append(f"{s.name}: {part.name} {name} face has see-through texels")
    return out


def save(s, textures):
    """Writes the set's atlas under the mod's textures folder."""
    path = textures / f"{s.texture}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    paint_atlas(s).save(path, optimize=True)
    return path


def draw_all(textures):
    """Paints every registered set (generate_textures.py)."""
    for s in am.sets():
        save(s, textures)
