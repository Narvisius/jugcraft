"""Pixel-art arms (the arms restyle, docs/features/arms-restyle.md): a weapon is designed once, along its own axis, and drawn
from that design twice:

- as its inventory icon, on the exact 45-degree pixel diagonal, in flat tones lit from the top left with a one-pixel
  outline, as vanilla draws its swords and as the best weapon mods draw theirs (studied: Simply Swords and Epic Knights,
  32 and 48 pixels a side; nothing of theirs is used);
- as the texture of a 3D model held in the hand, upright (the model lays it along the diagonal), in which every part
  has its own thickness: a thin blade with a thicker ridge, a chunky guard and pommel, a round grip (after studying
  RPG Style More Weapons, whose weapons are built of many small boxes; nothing of theirs is used either).

A design is in design units: s along the weapon from the butt up (the hand at `grip`), t across it, negative towards
the lit side. Parts are flat shapes (strips with a profile, polygons, discs, single pixels) in a material; the shading
is worked out from the shapes, as a pixel artist would: the edge facing the light is the brightest tone, the edge
away from it the darkest, the inside between, each part outlined against the next.
"""
import math

from PIL import Image


class Material:
    """A ramp of flat tones, darkest first: outline (away from the light), outline (towards it), dark, mid, light,
    highlight. `shine`: whether the lit edge takes the highlight (metal) or only the light tone (wood, leather).
    `glow`: whether it gives light (runes, embers): its boxes in a 3D model are drawn at full light, as a glowing block's
    are (light_emission 15)."""

    def __init__(self, outline_dark, outline_light, dark, mid, light, highlight, shine=True, glow=False):
        self.outline_dark = outline_dark
        self.outline_light = outline_light
        self.dark = dark
        self.mid = mid
        self.light = light
        self.highlight = highlight
        self.shine = shine
        self.glow = glow

    def tones(self):
        return [self.outline_dark, self.outline_light, self.dark, self.mid, self.light, self.highlight]

    @property
    def ramp(self):
        """The tones darkest to lightest (as tools/hd_art.py's materials give theirs)."""
        return self.tones()


# Tones (an index into Material.tones()).
OUT_DARK, OUT_LIGHT, DARK, MID, LIGHT, HIGHLIGHT = range(6)

# The owner's chosen bronze (a tan gold, apart from vanilla's copper) and steel (a dark blue-grey, apart from iron).
BRONZE = Material((62, 36, 16), (94, 59, 25), (126, 82, 34), (180, 128, 60), (220, 170, 92), (246, 214, 150))
STEEL = Material((30, 33, 41), (52, 57, 69), (74, 82, 98), (110, 120, 137), (152, 162, 180), (208, 216, 228))
BRASS = Material((62, 42, 6), (100, 70, 12), (140, 104, 20), (200, 160, 42), (242, 218, 106), (255, 242, 176))
GUNMETAL = Material((22, 24, 30), (38, 42, 50), (54, 58, 68), (86, 92, 104), (134, 142, 156), (174, 182, 194))
LEATHER = Material((45, 28, 14), (58, 34, 20), (74, 42, 24), (98, 58, 34), (122, 74, 44), (142, 90, 56), shine=False)
RUBBER = Material((14, 14, 18), (28, 28, 34), (38, 38, 44), (52, 52, 60), (74, 74, 84), (98, 98, 110), shine=False)
WOOD = Material((45, 28, 14), (74, 48, 22), (90, 58, 30), (107, 69, 36), (154, 110, 60), (178, 132, 80), shine=False)
DARK_WOOD = Material((30, 20, 12), (52, 36, 26), (68, 48, 31), (90, 63, 40), (122, 88, 56), (148, 112, 74), shine=False)
GARNET = Material((60, 6, 16), (110, 14, 30), (160, 28, 46), (208, 52, 72), (255, 106, 120), (255, 184, 192))
PHOSPHOR = Material((6, 48, 26), (14, 90, 44), (30, 138, 68), (60, 188, 102), (140, 240, 168), (216, 255, 226))
CLOTH_RED = Material((44, 8, 10), (80, 14, 16), (110, 20, 22), (150, 32, 32), (188, 56, 50), (220, 96, 84), shine=False)
OLIVE = Material((24, 28, 12), (46, 52, 24), (62, 70, 34), (86, 96, 48), (112, 124, 66), (140, 152, 90), shine=False)
EMBER = Material((90, 20, 4), (154, 44, 8), (200, 72, 16), (232, 100, 26), (255, 216, 72), (255, 244, 192))
CHAIN = Material((24, 25, 30), (44, 46, 54), (74, 78, 88), (106, 112, 124), (140, 146, 158), (180, 186, 198))


class Style:
    """The materials a metal's arms are made of: the blade, the fittings (guards, pommels, ferrules), the grip, the haft,
    a set stone, and an accent."""

    def __init__(self, blade, fitting, grip, haft, gem, accent, cloth=CLOTH_RED):
        self.blade = blade
        self.fitting = fitting
        self.grip = grip
        self.haft = haft
        self.gem = gem
        self.accent = accent
        self.cloth = cloth


STYLES = {
    "bronze": Style(BRONZE, BRASS, LEATHER, WOOD, GARNET, BRASS),
    "steel": Style(STEEL, GUNMETAL, RUBBER, DARK_WOOD, PHOSPHOR, BRASS, OLIVE),
}


def _glint(material):
    """The glint's colour: the highlight, most of the way to white."""
    return tuple(int(c + (255 - c) * 0.6) for c in material.highlight)


def _profile(value):
    return value if callable(value) else (lambda s, v=value: v)


class Design:
    """A weapon along its own axis. Shapes are added in order; a later shape covers an earlier one where they meet
    (unless given a lower `z`). `depth` is a part's thickness in the 3D model, in design units."""

    def __init__(self, length, grip):
        self.length = length
        self.grip = grip
        self.shapes = []
        self.order = 0
        self.glints = []

    def _add(self, test, material, depth, z, part, tone, bounds, bevel=None):
        """bounds: (s0, s1, t0, t1), a box the shape lies within. bevel: where across (t, or a function of s) the part's
        lit face meets its shaded one, as a blade's two faces meet at its spine."""
        self.order += 1
        self.shapes.append({"test": test, "material": material, "depth": depth, "z": z, "order": self.order,
                            "part": part if part is not None else self.order, "tone": tone, "bounds": bounds,
                            "bevel": _profile(bevel) if bevel is not None else None})

    def glint(self, s, t):
        """A glint of light on the metal at (s, t): one pixel brighter than the highlight."""
        self.glints.append((s, t))

    def points(self, step=0.5):
        """Sample points (s, t) the design covers, every `step` within each shape's box."""
        out = []
        for shape in self.shapes:
            s0, s1, t0, t1 = shape["bounds"]
            s = s0
            while s <= s1 + 1e-9:
                t = t0
                while t <= t1 + 1e-9:
                    if shape["test"](s, t) is not None:
                        out.append((s, t))
                    t += step
                s += step
        return out

    def strip(self, s0, s1, left, right=None, material=None, depth=1.0, z=0, part=None, tone=None, stripes=None, bevel=None):
        """A part along the axis from s0 to s1, from -left(s) to +right(s) across (a number or a function of s).
        `stripes`: (period, tone) bands across it every `period` along (a grip's wrap)."""
        left = _profile(left)
        right = _profile(right) if right is not None else left

        def test(s, t):
            if not s0 <= s <= s1:
                return None
            if not -left(s) <= t <= right(s):
                return None
            if stripes and int(math.floor((s - s0) / stripes[0])) % 2 == 1:
                return stripes[1]
            return tone if tone is not None else True
        samples = [s0 + (s1 - s0) * i / 16 for i in range(17)]
        self._add(test, material, depth, z, part, tone, (s0, s1, min(-left(v) for v in samples), max(right(v) for v in samples)),
                  bevel)

    def poly(self, points, material, depth=1.0, z=0, part=None, tone=None, bevel=None):
        """A flat polygon, its corners (s, t)."""
        ss = [p[0] for p in points]
        ts = [p[1] for p in points]

        def test(s, t):
            if not _inside(points, s, t):
                return None
            return tone if tone is not None else True
        self._add(test, material, depth, z, part, tone, (min(ss), max(ss), min(ts), max(ts)), bevel)

    def disc(self, s, t, r, material, depth=1.0, z=0, part=None, tone=None):
        def test(ss, tt):
            if (ss - s) ** 2 + (tt - t) ** 2 > r * r:
                return None
            return tone if tone is not None else True
        self._add(test, material, depth, z, part, tone, (s - r, s + r, t - r, t + r))

    def ring(self, s, t, r_out, r_in, material, depth=1.0, z=0, part=None, tone=None):
        def test(ss, tt):
            d2 = (ss - s) ** 2 + (tt - t) ** 2
            if not r_in * r_in <= d2 <= r_out * r_out:
                return None
            return tone if tone is not None else True
        self._add(test, material, depth, z, part, tone, (s - r_out, s + r_out, t - r_out, t + r_out))

    def line(self, s0, t0, s1, t1, width, material, depth=1.0, z=0, part=None, tone=None):
        """A straight bar from (s0, t0) to (s1, t1), `width` across."""
        ds, dt = s1 - s0, t1 - t0
        length = math.hypot(ds, dt) or 1.0

        def test(s, t):
            u = ((s - s0) * ds + (t - t0) * dt) / length
            v = (-(s - s0) * dt + (t - t0) * ds) / length
            if not (-width / 2 <= u <= length + width / 2 and abs(v) <= width / 2):
                return None
            return tone if tone is not None else True
        self._add(test, material, depth, z, part, tone,
                  (min(s0, s1) - width, max(s0, s1) + width, min(t0, t1) - width, max(t0, t1) + width))

    def sample(self, s, t):
        """The shape on top at (s, t): (shape, tone override or True), or None."""
        best = None
        for shape in self.shapes:
            got = shape["test"](s, t)
            if got is None:
                continue
            if best is None or (shape["z"], shape["order"]) > (best[0]["z"], best[0]["order"]):
                best = (shape, got)
        return best


def _inside(points, x, y):
    inside = False
    n = len(points)
    for i in range(n):
        x1, y1 = points[i]
        x2, y2 = points[(i + 1) % n]
        if (y1 > y) != (y2 > y):
            if x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
                inside = not inside
    return inside


def _probe(design, s, t, n=4):
    """What a pixel centred on (s, t) is covered by, sampled n by n: (the shape covering most of it, with its tone),
    and the share it covers; or None."""
    hits = {}
    for i in range(n):
        for j in range(n):
            got = design.sample(s - 0.5 + (i + 0.5) / n, t - 0.5 + (j + 0.5) / n)
            if got is not None:
                key = (got[0]["order"], got[1] if not isinstance(got[1], bool) else True)
                entry = hits.setdefault(key, [got, 0])
                entry[1] += 1
    if not hits:
        return None
    got, count = max(hits.values(), key=lambda e: e[1])
    return got, sum(e[1] for e in hits.values()) / float(n * n)


FOUR = ((1, 0), (-1, 0), (0, 1), (0, -1))
EIGHT = FOUR + ((1, 1), (1, -1), (-1, 1), (-1, -1))


def _pieces(cells, steps):
    left, pieces = set(cells), []
    while left:
        start = left.pop()
        piece, todo = {start}, [start]
        while todo:
            x, y = todo.pop()
            for dx, dy in steps:
                q = (x + dx, y + dy)
                if q in left:
                    left.remove(q)
                    piece.add(q)
                    todo.append(q)
        pieces.append(piece)
    return sorted(pieces, key=len)


def _join(grid, probe, steps, reach=4):
    """Joins pieces of the design that sampling each pixel at its centre split apart (a thin sickle's point, a chain's
    links, a narrow swept hilt): while there is more than one piece, the smallest is joined to another by the shortest
    run of pixels the design partly covers (the most covered preferred), at most `reach` long. A piece that cannot be
    joined that way is left as it is. `probe(x, y)`: (shape and tone, share covered) or None."""
    cache = {}

    def covered(cell):
        if cell not in cache:
            cache[cell] = probe(*cell)
        return cache[cell]
    stuck = set()
    while True:
        pieces = _pieces(grid, steps)
        loose = [p for p in pieces if not p & stuck]
        if len(pieces) < 2 or not loose:
            return grid
        piece = loose[0]
        others = set(grid) - piece
        # Cheapest paths out of the piece over partly covered pixels: each costs 1 + (1 - its share).
        best = {cell: (0.0, None) for cell in piece}
        frontier = sorted((0.0, cell) for cell in piece)
        found = None
        while frontier:
            cost, cell = frontier.pop(0)
            if cost > best[cell][0]:
                continue
            for dx, dy in steps:
                q = (cell[0] + dx, cell[1] + dy)
                if q in others:
                    found = cell
                    break
                if q in grid or q in best and best[q][0] <= cost:
                    continue
                got = covered(q)
                if got is None:
                    continue
                n, c = 0, cell
                while best[c][1] is not None:
                    n, c = n + 1, best[c][1]
                if n >= reach:
                    continue
                new = cost + 2.0 - got[1]
                if q not in best or new < best[q][0]:
                    best[q] = (new, cell)
                    frontier.append((new, q))
                    frontier.sort()
            if found:
                break
        if found is None:
            stuck |= piece
            continue
        cell = found
        while best[cell][1] is not None:
            grid[cell] = covered(cell)[0]
            cell = best[cell][1]


def _shade(grid, size_x, size_y, lit, dark, coords=None):
    """Flat tones for every filled cell of `grid` ({(x, y): (shape, override)}), lit from `lit` (cell offsets that face the
    light) and shaded towards `dark`: the lit edge bright, the far edge dark, the middle mid. A bevelled part (a blade)
    has two faces instead of a middle: light on the lit side of its bevel line, mid beyond it, which `coords` ({cell:
    (s, t)}) places. Returns {(x, y): colour}."""
    out = {}

    def same(a, b):
        cb = grid.get(b)
        return cb is not None and cb[0]["part"] == grid[a][0]["part"]

    def run(cell, offsets):
        """Cells from `cell` to the part's edge in the offsets' directions (the nearest)."""
        best = 99
        for dx, dy in offsets:
            n, x, y = 0, cell[0], cell[1]
            while True:
                n += 1
                x, y = x + dx, y + dy
                if not same(cell, (x, y)) or n > 8:
                    break
            best = min(best, n)
        return best

    for cell, (shape, override) in grid.items():
        material = shape["material"]
        tones = material.tones()
        if override is not True and override is not None:
            out[cell] = tones[override]
            continue
        dl, dd = run(cell, lit), run(cell, dark)
        if dl == 1:
            tone = HIGHLIGHT if material.shine else LIGHT
        elif dd == 1:
            tone = DARK
        elif shape.get("bevel") is not None and coords is not None and cell in coords:
            s, t = coords[cell]
            tone = LIGHT if t < shape["bevel"](s) else MID
        elif dl == 2:
            tone = LIGHT
        else:
            tone = MID
        out[cell] = tones[tone]
    return out


def _outline(grid, colours, size_x, size_y, lit):
    """A one-pixel outline round everything: lighter where the outline lies on a part's lit side."""
    out = dict(colours)
    for y in range(size_y):
        for x in range(size_x):
            if (x, y) in grid:
                continue
            light = dark = None
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                cell = grid.get((x + dx, y + dy))
                if cell is None:
                    continue
                material = cell[0]["material"]
                # This empty pixel lies on the neighbour's side facing (-dx, -dy): lit if that side faces the light.
                if (-dx, -dy) in lit:
                    light = light or material.outline_light
                else:
                    dark = dark or material.outline_dark
            if dark or light:
                out[(x, y)] = dark or light
    return out


def fit(designs, size):
    """Where designs lie on an icon of `size`, fitted whole a pixel in from the edges along the diagonal (all of them in
    the same place, as the frames of a drawn bow): (grip pixel, diagonal steps to a design unit)."""
    us, vs = [], []
    for d in designs:
        for s, t in d.points():
            # A design point's pixel offset from the grip, in diagonal steps: x along (s - grip) + t, y along t - (s - grip).
            us.append((s - d.grip) + t)
            vs.append(t - (s - d.grip))
    room = size - 3.0
    span = max(max(us) - min(us), max(vs) - min(vs)) + 1.0
    scale = room / span
    gx = 1.5 + (room - scale * (max(us) - min(us))) / 2.0 - scale * min(us)
    gy = 1.5 + (room - scale * (max(vs) - min(vs))) / 2.0 - scale * min(vs)
    return (gx, gy), scale


def icon(design, size, grip_px, scale, mirrored=False):
    """The inventory icon: the design on the pixel diagonal, butt at the bottom left and point at the top right (the top
    left if mirrored, for the spear's hand pose), `scale` pixels-diagonal-steps to a design unit, the hand at `grip_px`."""
    gx, gy = grip_px
    grid = {}
    coords = {}
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - gx, y + 0.5 - gy
            if mirrored:
                dx = -dx
            # Along: half the difference (a diagonal step a unit); across: half the sum (two pixel lines a unit).
            s = (dx - dy) / 2.0 / scale + design.grip
            t = (dx + dy) / 2.0 / scale
            coords[(x, y)] = (s, t)
            got = design.sample(s, t)
            if got is not None:
                grid[(x, y)] = got

    def probe(x, y):
        dx, dy = x + 0.5 - gx, y + 0.5 - gy
        if mirrored:
            dx = -dx
        return _probe(design, (dx - dy) / 2.0 / scale + design.grip, (dx + dy) / 2.0 / scale)
    _join(grid, probe, EIGHT)
    lit = [(-1, 0), (0, -1)] if not mirrored else [(1, 0), (0, -1)]
    dark = [(1, 0), (0, 1)] if not mirrored else [(-1, 0), (0, 1)]
    colours = _shade(grid, size, size, lit, dark, coords)
    colours = _outline(grid, colours, size, size, lit)
    for s, t in design.glints:
        # The pixel the point falls in (the inverse of the mapping above).
        dx, dy = scale * ((s - design.grip) + t), scale * (t - (s - design.grip))
        cell = (int(math.floor(gx + (-dx if mirrored else dx))), int(math.floor(gy + dy)))
        if cell in grid:
            colours[cell] = _glint(grid[cell][0]["material"])
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for (x, y), colour in colours.items():
        img.putpixel((x, y), tuple(colour) + (255,))
    return img


def upright_width(design):
    """How wide the upright image must be to hold the design, centred on its axis."""
    reach = max([abs(t) for _s, t in design.points()] + [1.0])
    return 2 * int(math.ceil(reach + 0.5))


def upright(design, width=None):
    """The design upright, a texel a design unit: butt at the bottom, point at the top, lit from the left (which the
    model's lean along the diagonal turns to the top left). Returns (image, cells {(x, y): shape}, origin (x of t = 0,
    y of s = 0 counted from the top))."""
    width = width or upright_width(design)
    height = int(math.ceil(design.length))
    cx = width / 2.0
    grid = {}
    coords = {}
    for y in range(height):
        for x in range(width):
            s = height - y - 0.5
            t = x + 0.5 - cx
            coords[(x, y)] = (s, t)
            got = design.sample(s, t)
            if got is not None:
                grid[(x, y)] = got
    _join(grid, lambda x, y: _probe(design, height - y - 0.5, x + 0.5 - cx), FOUR)
    lit = [(-1, 0)]
    dark = [(1, 0)]
    colours = _shade(grid, width, height, lit, dark, coords)
    for s, t in design.glints:
        cell = (int(math.floor(cx + t)), int(math.floor(height - s)))
        if cell in grid:
            colours[cell] = _glint(grid[cell][0]["material"])
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for (x, y), colour in colours.items():
        img.putpixel((x, y), tuple(colour) + (255,))
    return img, {cell: got[0] for cell, got in grid.items()}, (cx, height)


def _rectangles(cells):
    """Greedy rectangles over a set of (x, y) cells: each row run, grown down while the rows below match."""
    left = set(cells)
    rects = []
    for y in sorted({c[1] for c in cells}):
        for x in sorted(c[0] for c in left if c[1] == y):
            if (x, y) not in left:
                continue
            x1 = x
            while (x1 + 1, y) in left:
                x1 += 1
            y1 = y
            while all((xx, y1 + 1) in left for xx in range(x, x1 + 1)):
                y1 += 1
            for yy in range(y, y1 + 1):
                for xx in range(x, x1 + 1):
                    left.discard((xx, yy))
            rects.append((x, y, x1 + 1, y1 + 1))
    return rects


def merged(designs):
    """One design covering all of several (an animated arm's frames), for a model shaped to fit every frame."""
    out = Design(designs[0].length, designs[0].grip)
    for d in designs:
        out.shapes.extend(d.shapes)
    return out


def model_elements(design, texture_size, offset, grip_model, unit, mirrored=False, geometry=None, width=None):
    """The 3D model's elements: the upright design (at `offset` on a `texture_size` texture), each part as thick as its
    `depth`, laid along the diagonal by a 45-degree turn about the hand (`grip_model`, in model pixels), `unit` model
    pixels a texel, so that it lies over the icon exactly. Faces are textured from the design: front and back from the
    upright image, the sides from its edge texels (as vanilla's extruded items are). `geometry`: the design to shape the
    boxes by, if not this one (the merged frames of an animated arm); `width`: the upright image's width."""
    img, cells, (cx, height) = upright(design, width)
    if geometry is not None:
        _img, cells, _origin = upright(geometry, width)
    gx, gy = grip_model
    k = 16.0 / texture_size
    ox, oy = offset
    by_depth = {}
    for cell, shape in cells.items():
        by_depth.setdefault((shape["depth"], shape["material"].glow), set()).add(cell)
    elements = []
    for depth, glow in sorted(by_depth):
        half = depth * unit / 2.0
        for x0, y0, x1, y1 in _rectangles(by_depth[(depth, glow)]):
            # Upright texel (x, y) spans t in [x - cx, x + 1 - cx] and s in [height - y - 1, height - y].
            fx = gx + (x0 - cx) * unit
            tx = gx + (x1 - cx) * unit
            fy = gy + (height - y1 - design.grip) * unit
            ty = gy + (height - y0 - design.grip) * unit
            u0, v0, u1, v1 = (ox + x0) * k, (oy + y0) * k, (ox + x1) * k, (oy + y1) * k

            def r(v):
                return round(v, 4)
            faces = {
                "south": {"uv": [r(u0), r(v0), r(u1), r(v1)], "texture": "#tex"},
                "north": {"uv": [r(u1), r(v0), r(u0), r(v1)], "texture": "#tex"},
                "east": {"uv": [r(u1 - k), r(v0), r(u1), r(v1)], "texture": "#tex"},
                "west": {"uv": [r(u0), r(v0), r(u0 + k), r(v1)], "texture": "#tex"},
                "up": {"uv": [r(u0), r(v0), r(u1), r(v0 + k)], "texture": "#tex"},
                "down": {"uv": [r(u0), r(v1 - k), r(u1), r(v1)], "texture": "#tex"},
            }
            element = {"from": [r(fx), r(fy), r(8 - half)], "to": [r(tx), r(ty), r(8 + half)],
                       "rotation": {"angle": 45.0 if mirrored else -45.0, "axis": "z", "origin": [r(gx), r(gy), 8]},
                       "faces": faces}
            if glow:
                element["light_emission"] = 15
            elements.append(element)
    return img, elements
