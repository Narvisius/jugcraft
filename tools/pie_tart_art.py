"""The owner's pies and tarts' textures (tools/pies_and_tarts.py), rebuilt from their drawing of them (requires Pillow).

On 8 October 2026 the owner shared a page of ten pies and tarts they drew, "CAKES & BAKES - 3D PIES & TARTS", rendered as
blocks seen from above the south-east as their cakes were, and it is kept as art/owner-library/drawings/pies_and_tarts.png
(an image viewer's button and a scribble painted out). It draws two shapes, every pie alike and every tart alike
(pies_and_tarts.PIE and TART); for each bake, CAMERAS below holds the projection that puts its shape's outline on the
drawing's (fitted to it once, on the day). Through it each texel of every face the drawing shows is read off the page
(the median of a small grid in the middle of the texel, as tools/cake_art.py reads the cakes) and the sides are lit back
up (the page shades them as Minecraft does, the south face to 0.8 of the top, the east face to 0.6).

- **Top** (`<bake>_top`): a pie's lid as drawn; a tart's rim read at its top and its filling a texel below. What the
  drawing hides (behind a topping, or a tart's front rim, or the quarter the page cuts away) is filled from the same place
  mirrored or turned (tools/cake_art.py fill). Turned half round, as the model is.
- **Front** and **side** (`<bake>_front`, `<bake>_side`): the drawing's south and east faces, each part's rows where
  pie_tart_data.LAYOUT puts them: a pie's lid and its inset body; a tart's wall, its inset base and the inner wall of its
  rim (read off the back rim, where the drawing shows it). Where the page cuts a quarter away, the half it shows is
  mirrored.
- **Inside** (`<bake>_inside`, the faces a cut shows): the strawberry pie's and the blueberry tart's from the page's own
  cuts (each half of the texture from one of the two cut faces it draws); the others', which the owner drew whole, are
  those, their filling in the bake's own (its colours from its top, ranked by lightness), but the pork pie's, which is
  meat.
- **Toppings** (`<bake>_toppings`): every topping of a bake read off the drawing at the box it was fitted to
  (pies_and_tarts.BAKES), texel by texel the median of them all: the top, the south face below it and the east face
  beside that.
- **Items**: each slice is one of the owner's own pie slices (their library's `apple_pie_slice.png` for the lattice pies,
  `pumpkin_pie_slice.png` for the plain ones, `sweet_berry_cheesecake_slice.png` for the tarts) recoloured to the bake;
  the raw bakes are drawn here, a square tin of pastry with the raw filling.

Called from crop_textures.crop_textures(). Nothing here reads a Mojang texture.
"""
import colorsys
import os
import statistics

from PIL import Image

import pie_tart_data as data
import pies_and_tarts as pt
from cake_art import EAST, SOUTH, TIN, Page, axes, fill, lightness, owner_icon, ramp_of, recolour, shade, square, turned, view, hidden

HERE = os.path.dirname(os.path.abspath(__file__))
PAGE = os.path.join(os.path.dirname(HERE), "art", "owner-library", "drawings", "pies_and_tarts.png")

# Each bake on the page: the screen position (pixels) of its box's back bottom corner (x 0, y 0, z 0), the view's turn
# and tilt (radians) and the pixels to a texel.
CAMERAS = {
    "strawberry_pie": (261.7374, 136.0618, 0.7635, 0.5774, 12.6521),
    "blueberry_tart": (267.3617, 366.3088, 0.7745, 0.5715, 12.7401),
    "plum_pie": (648.4829, 78.2019, 0.771, 0.5819, 6.1963),
    "banoffee_pie": (853.2695, 78.5009, 0.7737, 0.5759, 6.1914),
    "sweet_berry_tart": (587.4334, 202.7356, 0.7724, 0.5883, 6.0988),
    "lemon_tart": (793.5065, 202.825, 0.7755, 0.5863, 6.104),
    "whipped_pumpkin_pie": (647.885, 342.3946, 0.7671, 0.58, 6.1903),
    "pork_pie": (852.667, 342.1646, 0.7713, 0.5823, 6.2074),
    "strawberry_tart": (588.7399, 458.0036, 0.7981, 0.577, 6.0837),
    "coffee_tart": (792.9762, 457.3177, 0.7671, 0.5834, 6.114),
}
# The two the page draws large are a little squatter than its small ones (and than the models, which follow those): their
# sides and cuts are read through a projection fitted to their own outline, of a bake `height` tall whose lower part is
# inset by `inset` (a texel on the small ones), each row of the model's shape read at the same height in proportion.
DRAWN = {
    "strawberry_pie": {"camera": (263.3311, 133.2548, 0.775, 0.567, 12.7375), "height": 6.5868, "inset": 0.578},
    "blueberry_tart": {"camera": (267.5197, 361.1943, 0.775, 0.567, 12.7569), "height": 3.4729, "inset": 0.3027},
}
# The pork pie's filling, which no cut shows: meat, darkest to lightest.
MEAT = [(110, 52, 40), (146, 74, 56), (178, 98, 76), (204, 126, 100), (226, 156, 128)]
# A raw bake's pastry (unbaked, pale) and how its filling is flecked.
PASTRY = [(214, 190, 150), (232, 212, 174), (244, 228, 196)]


# ---------------------------------------------------------------- reading the drawing

def read(page, camera, origin, du, dv, nu, extents, shade_by=1.0, drawn=None):
    """The texels of a face: column u spans origin + [u, u+1] du, row r spans origin + extents[r] along dv (a half-texel
    row is read across its half). Each the median of a 4 x 4 grid across its middle, divided by the face's shade. With
    `drawn` (a DRAWN entry and the model's height), each point is read where the drawing has it: its height scaled to the
    drawing's, and a point on the model's inset plane on the drawing's."""
    if drawn is not None:
        spec, model_height = drawn
        camera = spec["camera"]
    (ox, oy), east, up, south = axes(camera)
    steps = [0.3 + 0.4 * i / 3 for i in range(4)]
    rows = []
    for a, b in extents:
        row = []
        for u in range(nu):
            samples = []
            for s in steps:
                for t in steps:
                    v = a + (b - a) * t
                    p = [origin[i] + (u + s) * du[i] + v * dv[i] for i in range(3)]
                    if drawn is not None:
                        p = drawn_point(p, origin, du, dv, spec, model_height)
                    samples.append(page.at(ox + p[0] * east[0] + p[1] * up[0] + p[2] * south[0],
                                           oy + p[0] * east[1] + p[1] * up[1] + p[2] * south[1]))
            row.append(tuple(min(255, int(round(statistics.median(c[i] for c in samples) / shade_by))) for i in range(3)))
        rows.append(row)
    return rows


def drawn_point(p, origin, du, dv, spec, model_height):
    """A point of the model's shape where the drawing has it (DRAWN): its height in proportion, and on the inset plane
    of the face it lies on if that face is the inset body's or base's."""
    q = list(p)
    q[1] = p[1] * spec["height"] / model_height
    inset = pt.INSET
    for axis in (0, 2):
        if du[axis] == 0 and dv[axis] == 0:      # the face's own plane along this axis
            if abs(origin[axis] - inset) < 1e-9:
                q[axis] = spec["inset"]
            elif abs(origin[axis] - (16 - inset)) < 1e-9:
                q[axis] = 16 - spec["inset"]
    return q


def unit_rows(n, first=0.0):
    """Row extents a texel apart from `first` on, the last cut short at n."""
    out, v = [], first
    while v < n - 1e-9:
        out.append((v, min(n, v + 1)))
        v += 1
    return out


def half_first(n):
    """Rows of a part n tall whose first row is the half one (a body below a lid, a base below a wall)."""
    k = n - int(n)
    return ([(0, k)] if k else []) + [(k + i, k + i + 1) for i in range(int(n))]


def cut(bake):
    return bake in pt.CUT.values()


def occluders(bake):
    """What hides a bake's top in the drawing: its toppings, and a tart's front rims (south and east)."""
    boxes = [(lo, hi) for lo, hi in pt.topping_boxes(bake)]
    if pt.BAKES[bake]["shape"] == "tart":
        h, fill_at = pt.TART["height"], pt.TART["filling"]
        boxes += [([0, fill_at, 15], [16, h, 16]), ([15, fill_at, 0], [16, h, 16])]
    return boxes


def top_of(page, bake):
    """The bake's top as drawn, what the drawing hides filled from around it (in the drawing's orientation)."""
    camera = CAMERAS[bake]
    shape = pt.BAKES[bake]["shape"]
    if shape == "pie":
        h = pt.PIE["height"]
        rows = read(page, camera, (0, h, 0), (1, 0, 0), (0, 0, 1), 16, unit_rows(16))
        level = lambda x, z: h
    else:
        h, f = pt.TART["height"], pt.TART["filling"]
        rim = read(page, camera, (0, h, 0), (1, 0, 0), (0, 0, 1), 16, unit_rows(16))
        inner = read(page, camera, (0, f, 0), (1, 0, 0), (0, 0, 1), 16, unit_rows(16))
        ring = lambda x, z: x in (0, 15) or z in (0, 15)
        rows = [[rim[z][x] if ring(x, z) else inner[z][x] for x in range(16)] for z in range(16)]
        level = lambda x, z: h if ring(x, z) else f
    eye = view(camera)
    boxes = occluders(bake)
    holes = set()
    for z in range(16):
        for x in range(16):
            if cut(bake) and x >= 8 and z >= 8:
                holes.add((x, z))
            elif hidden((x + 0.5, level(x, z), z + 0.5), eye, boxes, 0.1) or greyish(rows[z][x]):
                holes.add((x, z))
    return fill(rows, holes)


def greyish(colour):
    """Whether a texel took the page's grey (at the outline, where the drawing's edge blends into the page)."""
    return max(colour) - min(colour) < 24 and 95 <= sum(colour) / 3 <= 160


def wall(page, bake, face):
    """The front (face "south") or side ("east") texture's rows, each part at its LAYOUT rows (16 rows; unused rows
    repeat the nearest used one, so mipmaps keep the colours)."""
    camera = CAMERAS[bake]
    shape = pt.BAKES[bake]["shape"]
    out = [[None] * 16 for _ in range(16)]
    i = pt.INSET
    # The plane of the face, and the direction along it as seen from outside.
    if face == "south":
        at = lambda d, y: (0, y, 16 - d)
        along = (1, 0, 0)
        shade_by = SOUTH
    else:
        at = lambda d, y: (16 - d, y, 16)
        along = (0, 0, -1)
        shade_by = EAST
    parts = []
    if shape == "pie":
        h, lid = pt.PIE["height"], pt.PIE["lid"]
        parts.append((data.LAYOUT["pie"]["lid"], at(0, h), unit_rows(h - lid), 0, 16))
        parts.append((data.LAYOUT["pie"]["body"], at(i, lid), half_first(lid), i, 16 - i))
    else:
        h, base, f = pt.TART["height"], pt.TART["base"], pt.TART["filling"]
        parts.append((data.LAYOUT["tart"]["wall"], at(0, h), unit_rows(h - base), 0, 16))
        parts.append((data.LAYOUT["tart"]["base"], at(i, base), half_first(base), i, 16 - i))
    drawn = (DRAWN[bake], pt.height(bake)) if bake in DRAWN else None
    for (r0, r1), origin, extents, u0, u1 in parts:
        o = tuple(origin[k] + u0 * along[k] for k in range(3))
        rows = read(page, camera, o, along, (0, -1, 0), u1 - u0, extents, shade_by, drawn)
        first = int(r0)
        for k, row in enumerate(rows):
            for u, c in enumerate(row):
                out[first + k][u0 + u] = c
    if shape == "tart":
        # The rim's inner wall: the rim's own crust, as its outer face shows it (the drawing's inner walls are a sliver
        # behind the filling and the toppings); Minecraft shades it as it shades any side.
        r = int(data.LAYOUT["tart"]["rim_inner"][0])
        w0 = int(data.LAYOUT["tart"]["wall"][0])
        out[r] = list(out[w0])
    if cut(bake):
        # The page shows each outer face of a cut bake only beside the quarter it cuts away: the south face's west half
        # (columns 0-7 as seen from outside) and the east face's north half (columns 8-15); the other half is mirrored.
        have = set(range(8)) if face == "south" else set(range(8, 16))
        out = [[row[u] if u in have or row[15 - u] is None else row[15 - u] for u in range(16)] for row in out]
    return complete(out)


def complete(rows):
    """Every empty texel takes the nearest filled one in its row; every empty row the nearest filled row."""
    rows = [list(r) for r in rows]
    for r in rows:
        if any(c is not None for c in r):
            filled = [u for u, c in enumerate(r) if c is not None]
            for u in range(16):
                if r[u] is None:
                    r[u] = r[min(filled, key=lambda k: (abs(k - u), k))]
    full = [k for k, r in enumerate(rows) if r[0] is not None]
    return [rows[k] if rows[k][0] is not None else rows[min(full, key=lambda j: (abs(j - k), j))] for k in range(16)]


def cut_faces(page, bake):
    """A cut bake's inside: the right half (columns 8-15, the middle to the outside) from the page's south-facing cut,
    the left (0-7, the outside to the middle) from its east-facing cut, each part at its LAYOUT rows."""
    camera = CAMERAS[bake]
    shape = pt.BAKES[bake]["shape"]
    out = [[None] * 16 for _ in range(16)]
    i = pt.INSET
    if shape == "pie":
        h, lid = pt.PIE["height"], pt.PIE["lid"]
        parts = [(data.LAYOUT["pie"]["lid"], h, unit_rows(h - lid), 0), (data.LAYOUT["pie"]["body"], lid, half_first(lid), i)]
    else:
        h, base = pt.TART["height"], pt.TART["base"]
        parts = [(data.LAYOUT["tart"]["wall"], h, unit_rows(h - base), 0), (data.LAYOUT["tart"]["base"], base, half_first(base), i)]
    drawn = (DRAWN[bake], pt.height(bake)) if bake in DRAWN else None
    for (r0, r1), top, extents, inset in parts:
        n = 8 - inset
        south = read(page, camera, (8, top, 8), (1, 0, 0), (0, -1, 0), n, extents, SOUTH, drawn)
        east = read(page, camera, (8, top, 16 - inset), (0, 0, -1), (0, -1, 0), n, extents, EAST, drawn)
        for k in range(len(extents)):
            for u in range(n):
                out[int(r0) + k][8 + u] = south[k][u]
                out[int(r0) + k][inset + u] = east[k][u]
    if shape == "tart":
        # Above the filling the cut shows only the rim, a texel at the outside: the rest of its row is the filling's top.
        r = int(data.LAYOUT["tart"]["wall"][0])
        for u in range(1, 15):
            out[r][u] = out[r + 1][u]
    return complete(out)


def crusty(colour):
    """Whether a colour is the crust's (its golden browns), not a filling's."""
    h, l, s = colorsys.rgb_to_hls(*(c / 255 for c in colour))
    return 15 <= h * 360 <= 60 and s > 0.3


def fill_ramp(rows):
    """The colours of a cut's filling (every texel not the crust's), by lightness."""
    return sorted({c for row in rows for c in row if not crusty(c)}, key=lambda c: (lightness(c), c))


def inside(page, bake, cuts, tops):
    """A bake's inside: its own cut if the page draws one, else its shape's drawn cut with the filling in its colours."""
    shape = pt.BAKES[bake]["shape"]
    template = cuts[pt.CUT[shape]]
    if cut(bake):
        return template
    source = fill_ramp(template)
    if bake == "pork_pie":
        target = MEAT
    else:
        target = filling_colours(bake, tops[bake])
    mapping = {c: target[min(len(target) - 1, int(k * len(target) / len(source)))] for k, c in enumerate(source)}
    return [[mapping.get(c, c) for c in row] for row in template]


def filling_colours(bake, top):
    """The bake's filling, darkest to lightest: its top's colours inside the crust (a pie's lid within its crust border,
    less a lattice's crust; a tart's filling within its rim), five of them evenly by lightness."""
    shape = pt.BAKES[bake]["shape"]
    edge = 2 if shape == "pie" else 1
    inner = [top[z][x] for z in range(edge, 16 - edge) for x in range(edge, 16 - edge)]
    if shape == "pie":
        inner = [c for c in inner if not crusty(c)] or inner
    # Its lighter four fifths: the darkest are flecks on the top (toffee, coffee, the vents), not the filling.
    return ramp_of(inner, (0.24, 0.42, 0.6, 0.78, 0.96))


# ---------------------------------------------------------------- toppings

def toppings(page, bake):
    """The toppings' texture: every topping's faces read off the drawing, the median of them all texel by texel."""
    spec = pt.BAKES[bake].get("toppings")
    if not spec:
        return None
    w, d, h = spec["size"]
    w, d = int(w), int(d)
    camera = CAMERAS[bake]
    tops, souths, easts = [], [], []
    for lo, hi in pt.topping_boxes(bake):
        tops.append(read(page, camera, (lo[0], hi[1], lo[2]), (1, 0, 0), (0, 0, 1), w, unit_rows(d)))
        souths.append(read(page, camera, (lo[0], hi[1], hi[2]), (1, 0, 0), (0, -1, 0), w, unit_rows(h), SOUTH))
        easts.append(read(page, camera, (hi[0], hi[1], hi[2]), (0, 0, -1), (0, -1, 0), d, unit_rows(h), EAST))

    def median(grids):
        return [[tuple(int(statistics.median(g[r][c][k] for g in grids)) for k in range(3)) for c in range(len(grids[0][0]))]
                for r in range(len(grids[0]))]
    image = Image.new("RGBA", (16, 16))
    for (u0, v0), grid in (((0, 0), median(tops)), ((0, d), median(souths)), ((w, d), median(easts))):
        for r, row in enumerate(grid):
            for c, colour in enumerate(row):
                image.putpixel((u0 + c, v0 + r), tuple(colour) + (255,))
    return image


# ---------------------------------------------------------------- items

SLICES = {"pie": "apple_pie_slice", "plain": "pumpkin_pie_slice", "tart": "sweet_berry_cheesecake_slice"}
# Each slice icon's colours by role: the crust's are kept; the filling's (and the cheesecake's berries) take the bake's.
SLICE_FILLING = {
    "apple_pie_slice": [(148, 30, 51), (165, 7, 0), (185, 105, 19), (193, 72, 54), (197, 133, 62), (216, 139, 80), (224, 175, 121)],
    "pumpkin_pie_slice": [(164, 84, 19), (183, 92, 18), (219, 116, 34), (216, 139, 80), (234, 158, 96), (238, 172, 110)],
    "sweet_berry_cheesecake_slice": [(212, 190, 164), (245, 230, 198), (255, 248, 226)],
}
SLICE_TOPPING = {"sweet_berry_cheesecake_slice": [(105, 31, 33), (165, 7, 0), (223, 70, 126), (239, 159, 188)]}


def slice_icon(bake, filling, topping):
    """The owner's pie slice that suits the bake, its filling (and fruit) in the bake's colours."""
    info = pt.BAKES[bake]
    kind = "tart" if info["shape"] == "tart" else ("pie" if bake in ("strawberry_pie", "plum_pie") else "plain")
    name = SLICES[kind]
    source = sorted(SLICE_FILLING[name], key=lambda c: (lightness(c), c))
    mapping = {c: filling[min(len(filling) - 1, int(k * len(filling) / len(source)))] for k, c in enumerate(source)}
    if name in SLICE_TOPPING:
        berries = sorted(SLICE_TOPPING[name], key=lambda c: (lightness(c), c))
        fruit = topping or filling
        mapping.update({c: fruit[min(len(fruit) - 1, int(k * len(fruit) / len(berries)))] for k, c in enumerate(berries)})
    return recolour(owner_icon(name), mapping)


def raw_icon(bake, filling):
    """A square tin seen from above and in front, lined with raw pastry round the bake's raw filling (a pie's under a
    lattice or lid of pastry, the pork pie's closed)."""
    image = Image.new("RGBA", (16, 16))
    px = image.load()
    pie = pt.BAKES[bake]["shape"] == "pie"
    raw = [shade(c, 1.08) for c in filling]
    for y in range(4, 14):
        for x in range(1, 15):
            if y == 4 or x in (1, 14):
                px[x, y] = TIN[2] + (255,)
            elif y >= 12:
                px[x, y] = (TIN[1] if y == 12 else TIN[0]) + (255,)
            elif y == 5 or x in (2, 13) or y == 11:
                px[x, y] = PASTRY[1] + (255,)
            elif bake == "pork_pie" or (pie and bake in ("strawberry_pie", "plum_pie") and (x + y) % 3 == 0):
                px[x, y] = PASTRY[2 if (x + y) % 2 else 0] + (255,)
            else:
                px[x, y] = raw[(x * 3 + y * 5) % len(raw)] + (255,)
    px[1, 4] = px[14, 4] = TIN[3] + (255,)
    if bake == "pork_pie":
        for x, y in ((7, 7), (8, 8), (7, 9), (8, 7)):
            px[x, y] = PASTRY[0] + (255,)
    return image


# ---------------------------------------------------------------- everything

def pie_tart_textures():
    """(kind, name) -> image for every texture of the pies and tarts."""
    page = Page(PAGE)
    out = {}
    tops = {bake: top_of(page, bake) for bake in pt.BAKES}
    cuts = {bake: cut_faces(page, bake) for bake in pt.CUT.values()}
    for bake in pt.BAKES:
        out[("block", f"{bake}_top")] = square(turned(tops[bake]))
        out[("block", f"{bake}_front")] = square(wall(page, bake, "south"))
        out[("block", f"{bake}_side")] = square(wall(page, bake, "east"))
        out[("block", f"{bake}_inside")] = square(inside(page, bake, cuts, tops))
        extra = toppings(page, bake)
        if extra is not None:
            out[("block", f"{bake}_toppings")] = extra
        filling = MEAT if bake == "pork_pie" else filling_colours(bake, tops[bake])
        topping = None
        if extra is not None:
            w, d = (int(v) for v in pt.BAKES[bake]["toppings"]["size"][:2])
            topping = ramp_of([extra.getpixel((u, v))[:3] for u in range(w) for v in range(d)], (0.1, 0.4, 0.7, 0.95))
        out[("item", pt.slice_item(bake))] = slice_icon(bake, filling, topping)
        out[("item", pt.raw(bake))] = raw_icon(bake, filling)
    return out
