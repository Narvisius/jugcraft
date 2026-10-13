"""The owner's milkshakes' textures (tools/milkshakes.py), read off their drawing of them (requires Pillow).

On 8 October 2026 the owner shared a page of seven milkshakes they drew, "CAKES & BAKES - 3D MILKSHAKE", rendered as
blocks seen from above the south-east as their cakes and pies were, and it is kept as
art/owner-library/drawings/milkshakes.png (an image viewer's two arrow buttons and a scribble painted out). Every glass
is the same shape (milkshakes.FOOT to milkshakes.STRAW); for each milkshake, CAMERAS below holds the projection that puts
it on the drawing (fitted to its cap and fruit once, on the day). Through it each quarter texel of every face the
drawing shows is read off the page (the median of a small grid across it) and lit back up (the page shades the sides
as Minecraft does, the south face to 0.8 of the top, the east to 0.6). What the drawing hides is filled: the cap's top
under the fruit and straw from the same place turned or mirrored, the rest from the nearest row or texel shown.

The glass, the cream, the base, the foot and the straw are read off the strawberry milkshake, which the page draws two
and a half times as large, for every milkshake; each one's own fruit is read off its own drawing, and its shake (the
glass's pink in the strawberry's) takes its own colours, ranked by lightness, from its own drawing's glass.

Each milkshake's texture is one 64 x 64 image, block/menu/<name> (milkshakes.LAYOUT: four pixels to a texel, so the
glass's quarter texels are whole pixels), packed by milkshakes.LAYOUT. Called from crop_textures.crop_textures().
Nothing here reads a Mojang texture.
"""
import math
import os
import statistics

from PIL import Image

import milkshakes as ms
from cake_art import Page, axes, hidden, lightness, view

HERE = os.path.dirname(os.path.abspath(__file__))
PAGE = os.path.join(os.path.dirname(HERE), "art", "owner-library", "drawings", "milkshakes.png")

# Each milkshake on the page: the screen position (pixels) of the glass's world (0, 0, 0), the view's turn and tilt
# (radians) and the pixels to a texel.
CAMERAS = {
    "strawberry_milkshake": (254.9928, 248.3448, 0.8037, 0.5743, 21.4170),
    "banana_milkshake": (605.4317, 113.6092, 0.7894, 0.5567, 8.9521),
    "plum_milkshake": (841.7635, 113.9246, 0.7864, 0.5550, 8.9625),
    "apple_milkshake": (542.6901, 288.8765, 0.7891, 0.5526, 9.0149),
    "blueberry_milkshake": (780.3528, 288.6135, 0.7987, 0.5461, 8.9416),
    "pumpkin_milkshake": (605.4817, 459.7016, 0.7901, 0.5552, 8.9657),
    "chocolate_milkshake": (841.3942, 459.7837, 0.7828, 0.5553, 8.9977),
}
SHADE = {"up": 1.0, "south": 0.8, "east": 0.6}
PX = ms.PIXELS


def boxes():
    """Every box of the glass but the straw, by name."""
    out = {"plate": ms.PLATE, "body": ms.BODY, "band": ms.BAND, "cap": ms.CAP, "fruit": ms.FRUIT}
    for i, bar in enumerate(ms.FOOT["bars"]):
        out[f"bar{i}"] = bar
    return out


def straw_turn(point, sign=1):
    """A point turned with the straw about its foot (sign -1: back into the straw's own upright frame)."""
    a = math.radians(ms.STRAW["angle"]) * sign
    ox, oy, oz = ms.STRAW["origin"]
    x, y, z = point
    y, z = y - oy, z - oz
    return (x, oy + y * math.cos(a) - z * math.sin(a), oz + y * math.sin(a) + z * math.cos(a))


def behind_straw(point, direction):
    """Whether a ray from `point` along `direction` passes through the straw."""
    p = straw_turn(point, -1)
    end = straw_turn(tuple(point[i] + direction[i] for i in range(3)), -1)
    d = tuple(end[i] - p[i] for i in range(3))
    return hidden(p, d, [(ms.STRAW["from"], ms.STRAW["to"])], grow=0.02)


def shown(point, direction, own):
    """Whether the drawing shows `point` (on the box named `own`): no other box and not the straw is in the way."""
    others = [box for name, box in boxes().items() if name != own]
    return not hidden(point, direction, others, grow=0.02) and (own == "straw" or not behind_straw(point, direction))


def face(box, side):
    """A box's face as (origin, du, dv, width, height): its top-left as seen from outside, one texel right and down."""
    (x0, y0, z0), (x1, y1, z1) = box
    if side == "up":
        return (x0, y1, z0), (1, 0, 0), (0, 0, 1), x1 - x0, z1 - z0
    if side == "south":
        return (x0, y1, z1), (1, 0, 0), (0, -1, 0), x1 - x0, y1 - y0
    return (x1, y1, z1), (0, 0, -1), (0, -1, 0), z1 - z0, y1 - y0


def read(page, camera, box, side, own, turn=None):
    """A face read off the page, a pixel per quarter texel: rows of colours, None where the drawing hides it."""
    (ox, oy), east, up, south = axes(camera)
    eye = view(camera)
    origin, du, dv, w, h = face(box, side)
    normal = {"up": (0, 1, 0), "south": (0, 0, 1), "east": (1, 0, 0)}[side]
    steps = (0.25, 0.5, 0.75)
    rows = []
    for j in range(int(round(h * PX))):
        row = []
        for i in range(int(round(w * PX))):
            point = [origin[k] + (i + 0.5) / PX * du[k] + (j + 0.5) / PX * dv[k] + 0.01 * normal[k] for k in range(3)]
            if turn:
                point = turn(point)
            if not shown(point, eye, own):
                row.append(None)
                continue
            samples = []
            for a in steps:
                for b in steps:
                    p = [origin[k] + (i + a) / PX * du[k] + (j + b) / PX * dv[k] for k in range(3)]
                    if turn:
                        p = turn(p)
                    samples.append(page.at(ox + p[0] * east[0] + p[1] * up[0] + p[2] * south[0],
                                           oy + p[0] * east[1] + p[1] * up[1] + p[2] * south[1]))
            row.append(tuple(min(255, int(round(statistics.median(s[k] for s in samples) / SHADE[side]))) for k in range(3)))
        rows.append(row)
    return rows


def cells(rows, phase):
    """A face read a pixel per quarter texel, as the drawing's texels: the grid of whole texels starting `phase`
    (u, v texels) in from its top left, each the median of the pixels the drawing shows in it, None where it shows too
    few of them (a quarter or less)."""
    h, w = len(rows), len(rows[0])
    pu, pv = int(round(phase[0] * PX)), int(round(phase[1] * PX))
    us = sorted({0, w} | set(range(pu, w, PX)) - {w + 1})
    vs = sorted({0, h} | set(range(pv, h, PX)))
    grid = []
    for j in range(len(vs) - 1):
        line = []
        for i in range(len(us) - 1):
            seen = [rows[y][x] for y in range(vs[j], vs[j + 1]) for x in range(us[i], us[i + 1]) if rows[y][x] is not None]
            size = (vs[j + 1] - vs[j]) * (us[i + 1] - us[i])
            line.append(tuple(int(statistics.median(c[k] for c in seen)) for k in range(3)) if len(seen) * 4 > size else None)
        grid.append(line)
    return grid, us, vs


def spread(grid, us, vs):
    """Texels back to a pixel per quarter texel."""
    out = []
    for j in range(len(vs) - 1):
        for _ in range(vs[j], vs[j + 1]):
            out.append([grid[j][i] for i in range(len(us) - 1) for _ in range(us[i], us[i + 1])])
    return out


def roughness(rows, phase):
    """How far a face's pixels are from their texel's median, on the grid at `phase`: low where the grid is the drawing's."""
    grid, us, vs = cells(rows, phase)
    total, n = 0.0, 0
    for j in range(len(vs) - 1):
        for i in range(len(us) - 1):
            m = grid[j][i]
            if m is None:
                continue
            for y in range(vs[j], vs[j + 1]):
                for x in range(us[i], us[i + 1]):
                    c = rows[y][x]
                    if c is not None:
                        total += sum((c[k] - m[k]) ** 2 for k in range(3))
                        n += 1
    return total / max(1, n)


def phase_of(rows):
    """The drawing's texel grid on a face: the quarter-texel offset that makes its texels most even."""
    steps = [k / PX for k in range(PX)]
    return min(((pu, pv) for pv in steps for pu in steps), key=lambda p: (round(roughness(rows, p), 3), p))


def symmetric(grid):
    """Each hidden texel of a square top takes the same place turned or mirrored (the cap is alike all round)."""
    n = len(grid)
    out = [list(r) for r in grid]
    for y in range(n):
        for x in range(n):
            if grid[y][x] is None:
                for cx, cy in ((n - 1 - x, y), (x, n - 1 - y), (n - 1 - x, n - 1 - y), (y, x), (n - 1 - y, x), (y, n - 1 - x),
                               (n - 1 - y, n - 1 - x)):
                    if grid[cy][cx] is not None:
                        out[y][x] = grid[cy][cx]
                        break
    return out


def nearest(grid):
    """Each hidden texel takes the nearest shown one (the one above first, then in a fixed order on a tie)."""
    shown_at = [(x, y) for y, r in enumerate(grid) for x, c in enumerate(r) if c is not None]
    out = [list(r) for r in grid]
    for y, r in enumerate(grid):
        for x, c in enumerate(r):
            if c is None:
                sx, sy = min(shown_at, key=lambda p: (abs(p[0] - x) + abs(p[1] - y), p[0] != x, p[1], p[0]))
                out[y][x] = grid[sy][sx]
    return out


def texels(rows, phase=None, square=False):
    """A face as the drawing's texels (at `phase`, else the one that fits), hidden ones filled, a pixel per quarter texel."""
    phase = phase if phase is not None else phase_of(rows)
    grid, us, vs = cells(rows, phase)
    grid = nearest(symmetric(grid) if square else grid)
    return spread(grid, us, vs), phase


def median(rows):
    colours = [c for r in rows for c in r if c is not None]
    return tuple(int(statistics.median(c[k] for c in colours)) for k in range(3))


def shared(page):
    """The parts every milkshake shares, read off the strawberry's drawing: ({layout key: rows}, {layout key: phase})."""
    cam = CAMERAS[ms.LARGE]
    b = boxes()
    faces = {"cap_up": (ms.CAP, "up", "cap"), "cap_s": (ms.CAP, "south", "cap"), "cap_e": (ms.CAP, "east", "cap"),
             "band_s": (ms.BAND, "south", "band"), "band_e": (ms.BAND, "east", "band"),
             "body_s": (ms.BODY, "south", "body"), "body_e": (ms.BODY, "east", "body"),
             "plate_s": (ms.PLATE, "south", "plate"), "plate_e": (ms.PLATE, "east", "plate"),
             "bar_up": (b["bar0"], "up", "bar0"), "bar_side": (b["bar0"], "south", "bar0"), "bar_end": (b["bar0"], "east", "bar0")}
    out, phases = {}, {}
    for key, (box, side, own) in faces.items():
        out[key], phases[key] = texels(read(page, cam, box, side, own), square=key == "cap_up")
    straw = (ms.STRAW["from"], ms.STRAW["to"])
    for key, side in (("straw_s", "south"), ("straw_e", "east"), ("straw_up", "up")):
        # The straw's foot is inside the cream: its sides there take the texel just above.
        out[key], phases[key] = texels(read(page, cam, straw, side, "straw", turn=straw_turn))
    ring = read(page, cam, ms.BAND, "up", "band")
    out["band_up"] = [[median(ring)] * PX for _ in range(PX)]
    # The glass's underside: its base's darkest.
    dark = min((c for r in out["plate_e"] for c in r), key=lightness)
    out["plate_down"] = [[dark] * PX for _ in range(PX)]
    return out, phases


def fruit(page, name):
    """A milkshake's fruit, read off its own drawing: two texels a side."""
    cam = CAMERAS[name]
    return {f"fruit_{key}": texels(read(page, cam, ms.FRUIT, side, "fruit"), phase=(0, 0))[0]
            for key, side in (("up", "up"), ("s", "south"), ("e", "east"))}


def shake(page, name, base, phases):
    """A milkshake's glass: the strawberry's, with the shake between its corner posts (the four texels in from each)
    read off the milkshake's own drawing, on the same texel grid; where its drawing hides it, the strawberry's own."""
    cam = CAMERAS[name]
    out = {}
    for key, side in (("body_s", "south"), ("body_e", "east")):
        rows = read(page, cam, ms.BODY, side, "body")
        grid, us, vs = cells(rows, phases[key])
        own = spread(grid, us, vs)
        out[key] = [[own[y][x] if own[y][x] is not None and PX <= x < 5 * PX else base[key][y][x] for x in range(len(row))]
                    for y, row in enumerate(base[key])]
    return out


def pack(parts):
    image = Image.new("RGBA", (16 * PX, 16 * PX), (0, 0, 0, 255))
    for key, rows in parts.items():
        u0, v0, u1, v1 = ms.LAYOUT[key]
        w, h = int(round((u1 - u0) * PX)), int(round((v1 - v0) * PX))
        for y in range(h):
            for x in range(w):
                c = rows[min(y, len(rows) - 1)][min(x, len(rows[0]) - 1)]
                image.putpixel((int(u0 * PX) + x, int(v0 * PX) + y), tuple(c) + (255,))
    return image


def milkshake_textures():
    """(kind, name) -> image for every milkshake's texture."""
    page = Page(PAGE)
    base, phases = shared(page)
    out = {}
    for name in ms.SHAKES:
        parts = dict(base)
        parts.update(fruit(page, name))
        if name != ms.LARGE:
            parts.update(shake(page, name, base, phases))
        out[("block", f"menu/{name}")] = pack(parts)
    return out
