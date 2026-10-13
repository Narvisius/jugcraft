"""The cakes' textures (tools/cakes.py), rebuilt from the owner's drawing of them (requires Pillow).

On 8 October 2026 the owner shared a page of seven cakes they drew, "CAKES & BAKES - 3D CAKES", rendered as blocks seen
from above the south-east, and asked for them to be rebuilt from it. The page is kept as
art/owner-library/drawings/cakes_and_bakes.png (the corner where the owner had scribbled out some text blanked to the
page's grey). Each cake is a box 16 texels square and cakes.HEIGHT tall; for each, CAMERAS below holds the projection that
puts the box's outline on the drawing's (fitted to it once, on the day): where the box's back bottom corner falls on the
page, the turn and tilt of the view, and the pixels to a texel. Through it every texel of the three faces the drawing
shows is read off the page (the median of a small grid in the middle of the texel, so the texel's edges and the drawing's
smoothing do not bleed in), and the sides are lit back up: the drawing shades its faces as Minecraft does (the left,
south face to 0.8 of the top, the right, east face to 0.6), and the game shades them again.

- **Top** (`<cake>_top`): as drawn, but where the drawing shows a topping (or a topping's shadow side) over the top,
  whose texels are filled from the nearest frosting; turned half round, as the model is (cakes.turn).
- **Front** and **side** (`<cake>_front`, `<cake>_side`): the drawing's south and east faces, the front on the model's
  front and back, the side on its left and right; in the bottom nine rows, as vanilla's cake keeps its side.
- **Inside** (`<cake>_inside`, the faces a cut shows): the carrot cake's from the owner's own INTERIOR drawing of it (its
  two cut faces, read the same way); the others, which the owner drew only whole, from their own sides: a layered cake's
  side already shows its layers; the frosted ones' sides with the frosting's drips taken out, and the birthday cake, whose
  sides are all frosting, a vanilla sponge in pink cream; the bottom row is the cake's underside.
- **Toppings** (`<cake>_toppings`): the carrots, apple slices, candles and jam, in the colours the drawing gives them.
- **Items**: each cake's slice is the owner's own Slice of Cake icon (their library's `cake_slice.png`) recoloured in the
  cake's colours; the raw cakes are drawn here, a square tin of batter flecked with what went in; Cake Batter is the owner's
  cornbread batter icon recoloured to a vanilla batter.
- **Burnt Cake**: the carrot cake's top, sides and inside, charred (each texel's lightness on a burnt ramp).

Called from crop_textures.crop_textures(). Nothing here reads a Mojang texture.
"""
import colorsys
import math
import os
import statistics

from PIL import Image

import cakes

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
PAGE = os.path.join(ROOT, "art", "owner-library", "drawings", "cakes_and_bakes.png")
OWNER_FOOD = os.path.join(ROOT, "art", "owner-library", "originals", "Blocks", "farming and food textures")

# Each cake on the page: the screen position (pixels) of its box's back bottom corner (x 0, y 0, z 0), the view's turn
# and tilt (radians) and the pixels to a texel. The carrot cake's INTERIOR drawing has its own.
CAMERAS = {
    "carrot_cake": (255.3022, 140.1823, 0.8128, 0.5535, 11.2733),
    "birthday_cake": (623.5495, 109.6833, 0.8224, 0.5404, 6.8106),
    "ice_cream_cake": (842.2577, 107.967, 0.8114, 0.5542, 6.8486),
    "red_velvet_cake": (544.3856, 282.4347, 0.8122, 0.5556, 6.8428),
    "cheesecake": (762.7616, 282.3843, 0.8107, 0.5537, 6.8523),
    "coffee_cake": (622.5785, 455.4462, 0.8118, 0.554, 6.8293),
    "apple_cake": (841.4747, 454.0794, 0.8035, 0.5693, 6.8264),
}
INTERIOR = (253.134, 410.3779, 0.7967, 0.5483, 11.226)
# The drawing's light: the south face at 0.8 of the top, the east face at 0.6 (as Minecraft shades them).
SOUTH, EAST = 0.8, 0.6
# A sampled top texel counts as a topping (and is filled) when its colour is further from grey than this (max - min of
# its channels): the carrots, apple slices and jam stand out of their pale frosting. The birthday cake's sprinkles are as
# bright as its candles, so only the candles' boxes decide there.
TOPPING_CHROMA = {"carrot_cake": 110, "apple_cake": 95, "cheesecake": 120}
# How far round a topping's box (and above it) the top counts as hidden by it in the drawing: the thin candles stand where
# the drawing has them to within a texel, so theirs reach further.
HIDDEN_MARGIN = {"birthday_cake": (0.6, 1.0)}

# Topping colours, as the drawing gives them (read off it at full light).
CARROT = {"leaf": [(135, 183, 15), (109, 160, 18)], "light": (232, 126, 28), "mid": (222, 92, 18), "dark": (183, 70, 14)}
APPLE = {"top": [[(212, 227, 110), (211, 226, 108), (189, 211, 85), (169, 195, 70)],
                 [(186, 209, 85), (170, 194, 70), (142, 174, 59), (142, 174, 59)]],
         "side": [(214, 226, 111), (212, 226, 111), (189, 212, 86), (169, 195, 72)], "end": [(189, 211, 85), (170, 195, 71)]}
CANDLE_WHITE = (238, 250, 248)
CANDLE_COLOURS = {"blue": (99, 186, 211), "yellow": (222, 214, 133), "green": (53, 153, 77), "pink": (222, 129, 195)}
JAM_SIDE = [(221, 42, 46), (244, 76, 66), (248, 118, 95), (243, 76, 67)]
# The jam's top face on the page: a square from (5.5, 5.5) to (10, 10), read as four by four.
JAM_DRAWN = (5.5, 10.0)

# The slice icon's recolouring (the owner's cake_slice.png): its sponge's four browns, its frosting's four creams and the
# tan where they meet, and its cherry's three reds, each onto the cake's own (the sponge and frosting ramps are taken from
# the cake's textures; the topping's ramp is the cake's own topping, dark to light).
SLICE_SPONGE = [(89, 37, 16), (127, 58, 29), (151, 73, 26), (175, 88, 35)]
SLICE_FROSTING = [(213, 200, 172), (245, 230, 198), (255, 248, 226), (255, 253, 254)]
SLICE_EDGE = (199, 130, 91)
SLICE_CHERRY = [(176, 33, 50), (232, 53, 53), (240, 115, 90)]
TOPPING_RAMP = {
    "carrot_cake": [(183, 70, 14), (222, 92, 18), (232, 126, 28)],
    "birthday_cake": [(60, 150, 180), (99, 186, 211), (150, 214, 232)],
    "ice_cream_cake": [(58, 34, 24), (86, 52, 34), (118, 74, 46)],
    "red_velvet_cake": [(176, 24, 40), (226, 56, 66), (244, 118, 118)],
    "cheesecake": [(169, 21, 45), (222, 42, 45), (249, 117, 94)],
    "coffee_cake": [(62, 36, 22), (92, 56, 34), (128, 84, 52)],
    "apple_cake": [(142, 174, 59), (189, 211, 85), (212, 227, 110)],
}
# What flecks each raw cake's batter, and the batter's own colour.
FLECKS = {"carrot_cake": [(232, 126, 28), (222, 92, 18)], "birthday_cake": [(99, 186, 211), (222, 129, 195), (222, 214, 133)],
          "ice_cream_cake": [(86, 52, 34), (238, 236, 230)], "red_velvet_cake": [(150, 20, 30), (120, 16, 26)],
          "cheesecake": [(222, 42, 45), (169, 21, 45)], "coffee_cake": [(92, 56, 34), (62, 36, 22)],
          "apple_cake": [(189, 211, 85), (142, 174, 59)]}
BATTER = {"carrot_cake": (214, 150, 92), "birthday_cake": (244, 226, 176), "ice_cream_cake": (150, 96, 62),
          "red_velvet_cake": (196, 52, 58), "cheesecake": (246, 228, 170), "coffee_cake": (214, 170, 118),
          "apple_cake": (226, 196, 140)}
TIN = [(78, 80, 86), (110, 112, 118), (150, 152, 158), (184, 186, 192)]
VANILLA = [(150, 118, 70), (196, 162, 104), (226, 198, 140), (240, 218, 164), (248, 232, 190), (252, 242, 212), (255, 250, 232)]
BURNT = [(22, 14, 10), (34, 22, 14), (48, 30, 18), (62, 40, 24), (78, 52, 32)]


# ---------------------------------------------------------------- reading the drawing

class Page:
    def __init__(self, path=PAGE):
        image = Image.open(path).convert("RGB")
        self.size = image.size
        self.px = image.load()

    def at(self, x, y):
        """The page's colour at (x, y), between pixel centres."""
        w, h = self.size
        x = min(max(x - 0.5, 0.0), w - 1.001)
        y = min(max(y - 0.5, 0.0), h - 1.001)
        x0, y0 = int(x), int(y)
        fx, fy = x - x0, y - y0
        a, b, c, d = self.px[x0, y0], self.px[x0 + 1, y0], self.px[x0, y0 + 1], self.px[x0 + 1, y0 + 1]
        return tuple(a[i] * (1 - fx) * (1 - fy) + b[i] * fx * (1 - fy) + c[i] * (1 - fx) * fy + d[i] * fx * fy for i in range(3))


def axes(camera):
    """Screen position of world (0, 0, 0) and the screen steps of one texel east, up and south."""
    ox, oy, turn, tilt, scale = camera
    east = (scale * math.cos(turn), scale * math.sin(turn) * math.sin(tilt))
    south = (-scale * math.sin(turn), scale * math.cos(turn) * math.sin(tilt))
    up = (0.0, -scale * math.cos(tilt))
    return (ox, oy), east, up, south


def read_face(page, camera, origin, du, dv, nu, nv, shade=1.0):
    """The face whose texel (u, v) spans origin + [u, u+1] du + [v, v+1] dv: each texel the median of a 4 x 4 grid of
    samples across its middle, divided by the face's shade. Rows of (r, g, b)."""
    (ox, oy), east, up, south = axes(camera)
    steps = [0.3 + 0.4 * i / 3 for i in range(4)]
    rows = []
    for v in range(nv):
        row = []
        for u in range(nu):
            samples = []
            for a in steps:
                for b in steps:
                    p = [origin[i] + (u + a) * du[i] + (v + b) * dv[i] for i in range(3)]
                    samples.append(page.at(ox + p[0] * east[0] + p[1] * up[0] + p[2] * south[0],
                                           oy + p[0] * east[1] + p[1] * up[1] + p[2] * south[1]))
            row.append(tuple(min(255, int(round(statistics.median(s[i] for s in samples) / shade))) for i in range(3)))
        rows.append(row)
    return rows


def drawn_faces(page, cake):
    camera, h = CAMERAS[cake], cakes.HEIGHT
    return {"top": read_face(page, camera, (0, h, 0), (1, 0, 0), (0, 0, 1), 16, 16),
            "front": read_face(page, camera, (0, h, 16), (1, 0, 0), (0, -1, 0), 16, h, SOUTH),
            "side": read_face(page, camera, (16, h, 16), (0, 0, -1), (0, -1, 0), 16, h, EAST)}


# ---------------------------------------------------------------- the top

def view(camera):
    """The direction from the cake towards the drawing's eye."""
    turn, tilt = camera[2], camera[3]
    return (math.sin(turn) * math.cos(tilt), math.sin(tilt), math.cos(turn) * math.cos(tilt))


def hidden(point, direction, boxes, grow=0.25):
    """Whether a ray from `point` along `direction` passes through any box (each grown by `grow` all round)."""
    for lo, hi in boxes:
        near, far = 0.0, 1e9
        for i in range(3):
            a, b = lo[i] - grow, hi[i] + grow
            if abs(direction[i]) < 1e-9:
                if not a <= point[i] <= b:
                    break
                continue
            t0, t1 = (a - point[i]) / direction[i], (b - point[i]) / direction[i]
            near, far = max(near, min(t0, t1)), min(far, max(t0, t1))
            if near > far:
                break
        else:
            return True
    return False


def chroma(colour):
    return max(colour) - min(colour)


def fill(rows, holes):
    """Each hole in a square top takes the colour of the same place mirrored or turned (a cake's top is laid out alike
    all round: its rim, its middle), the first of those that is not a hole; failing all, the nearest texel that is not one
    (the first found in a fixed order on a tie)."""
    n = len(rows) - 1
    keep = [(x, y) for y in range(len(rows)) for x in range(len(rows[0])) if (x, y) not in holes]
    out = [list(r) for r in rows]
    for (x, y) in sorted(holes, key=lambda p: (p[1], p[0])):
        for (a, b) in ((n - x, y), (x, n - y), (n - x, n - y), (y, x), (n - y, x), (y, n - x), (n - y, n - x)):
            if (a, b) not in holes:
                out[y][x] = rows[b][a]
                break
        else:
            best = min(keep, key=lambda k: ((k[0] - x) ** 2 + (k[1] - y) ** 2, k[1], k[0]))
            out[y][x] = rows[best[1]][best[0]]
    return out


def clean_top(cake, top):
    """The drawn top with what its toppings cover or hide in the drawing filled from the frosting around it."""
    boxes = cakes.topping_boxes(cake)
    if not boxes:
        return top
    grow, taller = HIDDEN_MARGIN.get(cake, (0.25, 0.0))
    boxes = [(lo, [hi[0], hi[1] + taller, hi[2]]) for lo, hi in boxes]
    eye = view(CAMERAS[cake])
    limit = TOPPING_CHROMA.get(cake)
    holes = set()
    for z in range(16):
        for x in range(16):
            if hidden((x + 0.5, cakes.HEIGHT, z + 0.5), eye, boxes, grow) or (limit is not None and chroma(top[z][x]) > limit):
                holes.add((x, z))
    return fill(top, holes)


def turned(rows):
    """The rows turned half round (as the model turns the drawing, cakes.turn)."""
    return [list(reversed(r)) for r in reversed(rows)]


# ---------------------------------------------------------------- sides and insides

def lightness(colour):
    return colorsys.rgb_to_hls(*(c / 255 for c in colour))[1]


def dedrip(rows, frosting_rows, light):
    """A frosted side with its drips taken out: below the first `frosting_rows`, every texel lighter than `light` takes
    the nearest darker texel's colour in its row."""
    out = [list(r) for r in rows]
    for y in range(frosting_rows, len(rows)):
        cake = [x for x in range(len(rows[y])) if lightness(rows[y][x]) <= light]
        if not cake:
            continue
        for x in range(len(rows[y])):
            if lightness(rows[y][x]) > light:
                out[y][x] = rows[y][min(cake, key=lambda c: (abs(c - x), c))]
    return out


def cream_layer(rows, at, frosting_row=0):
    """A layer of the frosting (row `frosting_row`'s colours, shifted along) laid through the cake at row `at`."""
    out = [list(r) for r in rows]
    source = rows[frosting_row]
    out[at] = [source[(x + 5) % len(source)] for x in range(len(source))]
    return out


SPONGE = [(236, 206, 140), (244, 218, 156), (226, 192, 124), (248, 228, 172)]
PINK_CREAM = [(246, 196, 218), (236, 170, 202), (250, 214, 230)]


def birthday_inside(front):
    """Vanilla sponge in three layers between bands of pink cream, under the white frosting."""
    out = []
    for y in range(cakes.HEIGHT):
        row = []
        for x in range(16):
            n = (x * 7 + y * 13) % 11
            if y == 0:
                row.append(front[0][x])
            elif y in (3, 6):
                row.append(PINK_CREAM[n % 3])
            else:
                row.append(SPONGE[n % 4] if y < cakes.HEIGHT - 1 else SPONGE[2])
        out.append(row)
    return out


def interior(page):
    """The carrot cake's INTERIOR drawing: its two cut faces (the east-facing then the south-facing, as the model's
    west- and north-facing cuts show them), sixteen texels wide, with the frosting the drawing runs down their inner corner
    taken out."""
    h = cakes.HEIGHT
    east = read_face(page, INTERIOR, (8, h, 16), (0, 0, -1), (0, -1, 0), 8, h, EAST)
    south = read_face(page, INTERIOR, (8, h, 8), (1, 0, 0), (0, -1, 0), 8, h, SOUTH)
    rows = [east[y] + south[y] for y in range(h)]
    # The inner corner (the east face's last column and the south face's first) is drawn as a drip of frosting.
    for y in range(1, h):
        rows[y][7] = rows[y][6]
        rows[y][8] = rows[y][9]
    return rows


# How each cake's inside is made (see the module's notes): "drawn" (the INTERIOR drawing), "side" (the side as it is),
# ("dedrip", frosting rows, lightness of a drip, cream layer rows), or "birthday".
INSIDE = {"carrot_cake": "drawn", "birthday_cake": "birthday", "ice_cream_cake": "side", "red_velvet_cake": ("dedrip", 2, 0.72, [5]),
          "cheesecake": "side", "coffee_cake": "side", "apple_cake": ("dedrip", 2, 0.7, [5])}


def inside(page, cake, faces):
    how = INSIDE[cake]
    if how == "drawn":
        return interior(page)
    if how == "side":
        return faces["front"]
    if how == "birthday":
        return birthday_inside(faces["front"])
    _, frosting_rows, light, layers = how
    rows = dedrip(faces["front"], frosting_rows, light)
    for at in layers:
        rows = cream_layer(rows, at)
    return rows


def sheet(rows):
    """A 16 x 16 texture with the cake's nine rows at the bottom (as the model's faces read them, cakes.HEIGHT rows from
    the bottom); the rows above repeat them, so mipmaps do not darken the edge."""
    h = len(rows)
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        source = rows[(y - (16 - h)) % h]
        for x in range(16):
            image.putpixel((x, y), tuple(source[x]) + (255,))
    return image


def square(rows):
    image = Image.new("RGBA", (len(rows[0]), len(rows)))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            image.putpixel((x, y), tuple(c) + (255,))
    return image


# ---------------------------------------------------------------- toppings

def toppings(page, cake):
    """The cake's toppings texture (tools/cake_data.py maps the model's toppings onto these regions), or None."""
    image = Image.new("RGBA", (16, 16))
    put = lambda x, y, c: image.putpixel((x, y), tuple(c) + (255,))
    if cake == "carrot_cake":
        c = CARROT
        along = [c["leaf"], [c["light"], c["mid"]], [c["mid"], c["dark"]], [c["light"], c["mid"]], [c["mid"], c["dark"]]]
        for v, pair in enumerate(along):          # the top, lying along z: across u 0-1, leaves at v 0
            for u, colour in enumerate(pair):
                put(u, v, colour)
                put(8 + v, u, colour)              # and lying along x: along u 8-12, leaves at u 8
        for u, colour in enumerate([c["leaf"][1], c["light"], c["mid"], c["light"], c["mid"]]):
            put(3 + u, 0, colour)                  # the long sides: u 3-7, leaves at u 3
        put(3, 2, c["mid"]), put(4, 2, c["dark"])  # the tip's end
        put(5, 2, c["leaf"][0]), put(6, 2, c["leaf"][1])  # the leaves' end
        return image
    if cake == "apple_cake":
        a = APPLE
        for u, row in enumerate(a["top"]):
            for v, colour in enumerate(row):
                put(u, v, colour)                  # lying along z: across u 0-1, along v 0-3
                put(8 + v, u, colour)              # lying along x: along u 8-11, across v 0-1
        for u, colour in enumerate(a["side"]):
            put(3 + u, 0, colour)
        put(3, 2, a["end"][0]), put(4, 2, a["end"][1])
        return image
    if cake == "birthday_cake":
        for u, name in enumerate(cakes.CANDLE_ORDER):
            colour = CANDLE_COLOURS[name]
            for v in range(cakes.CANDLE_HEIGHT):  # the sides, white then the colour from the top down
                put(u, v, CANDLE_WHITE if v % 2 == 0 else colour)
            put(u, cakes.CANDLE_HEIGHT, colour)   # the top
        return image
    if cake == "cheesecake":
        lo, hi = JAM_DRAWN
        step = (hi - lo) / 4
        jam = read_face(page, CAMERAS[cake], (lo, cakes.HEIGHT + cakes.JAM[cake][1], lo), (step, 0, 0), (0, 0, step), 4, 4)
        for v, row in enumerate(turned(jam)):       # the top, turned as the model is: u 0-3, v 0-3
            for u, colour in enumerate(row):
                put(u, v, colour)
        for u, colour in enumerate(JAM_SIDE):       # the sides: u 0-3, v 4
            put(u, 4, colour)
        return image
    return None


# ---------------------------------------------------------------- items

def ramp_of(colours, points):
    """The colours at the given points (0-1) of their order by lightness."""
    ordered = sorted(colours, key=lambda c: (lightness(c), c))
    return [ordered[min(len(ordered) - 1, int(p * len(ordered)))] for p in points]


def recolour(image, mapping):
    out = image.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = px[x, y]
            if p[3] and p[:3] in mapping:
                px[x, y] = tuple(mapping[p[:3]]) + (p[3],)
    return out


def owner_icon(name):
    return Image.open(os.path.join(OWNER_FOOD, name + ".png")).convert("RGBA")


def slice_icon(cake, faces, top):
    """The owner's Slice of Cake, its sponge, frosting and cherry in this cake's colours."""
    sponge = ramp_of([c for row in faces["front"][4:] for c in row], (0.08, 0.35, 0.6, 0.88))
    frosting = ramp_of([c for row in top for c in row], (0.15, 0.45, 0.75, 0.97))
    mapping = dict(zip(SLICE_SPONGE, sponge))
    mapping.update(zip(SLICE_FROSTING, frosting))
    mapping[SLICE_EDGE] = tuple((a + b) // 2 for a, b in zip(sponge[3], frosting[0]))
    mapping.update(zip(SLICE_CHERRY, TOPPING_RAMP[cake]))
    return recolour(owner_icon("cake_slice"), mapping)


def shade(colour, k):
    return tuple(max(0, min(255, int(round(c * k)))) for c in colour)


def raw_icon(cake):
    """A square cake tin seen from above and in front, full of the cake's batter flecked with what went in."""
    image = Image.new("RGBA", (16, 16))
    px = image.load()
    batter = BATTER[cake]
    flecks = FLECKS[cake]
    for y in range(3, 14):
        for x in range(1, 15):
            if y == 3 or x in (1, 14):
                px[x, y] = TIN[2] + (255,)            # the rim
            elif y >= 12:
                px[x, y] = (TIN[1] if y == 12 else TIN[0]) + (255,)  # the tin's front
            else:
                n = (x * 5 + y * 11) % 13
                colour = flecks[n % len(flecks)] if n in (0, 6) and 2 < x < 13 else shade(batter, 1.06 if (x + y) % 5 == 0 else 1.0)
                px[x, y] = colour + (255,)
    for x in range(2, 14):
        px[x, 4] = shade(batter, 1.12) + (255,)       # the batter's lit back edge
    px[1, 3] = px[14, 3] = TIN[3] + (255,)
    return image


def batter_icon():
    """The owner's cornbread batter, recoloured to a vanilla cake batter (each of its colours to the vanilla tone of the
    same rank by lightness)."""
    icon = owner_icon("cornbread_batter")
    colours = sorted({p[:3] for p in icon.get_flattened_data() if p[3]}, key=lambda c: (lightness(c), c))
    return recolour(icon, {c: VANILLA[min(len(VANILLA) - 1, i)] for i, c in enumerate(colours)})


def charred(rows):
    return [[BURNT[min(len(BURNT) - 1, int(lightness(c) * len(BURNT)))] for c in row] for row in rows]


# ---------------------------------------------------------------- everything

def cake_textures():
    """(kind, name) -> image for every cake texture."""
    page = Page()
    out = {}
    carrot = None
    for cake in cakes.CAKES:
        faces = drawn_faces(page, cake)
        top = clean_top(cake, faces["top"])
        out[("block", f"{cake}_top")] = square(turned(top))
        out[("block", f"{cake}_front")] = sheet(faces["front"])
        out[("block", f"{cake}_side")] = sheet(faces["side"])
        out[("block", f"{cake}_inside")] = sheet(inside(page, cake, faces))
        extra = toppings(page, cake)
        if extra is not None:
            out[("block", f"{cake}_toppings")] = extra
        out[("item", cakes.slice_item(cake))] = slice_icon(cake, faces, top)
        out[("item", cakes.raw(cake))] = raw_icon(cake)
        if cake == "carrot_cake":
            carrot = (top, faces, inside(page, cake, faces))
    top, faces, cut = carrot
    out[("block", f"{cakes.BURNT}_top")] = square(charred(turned(top)))
    out[("block", f"{cakes.BURNT}_front")] = sheet(charred(faces["front"]))
    out[("block", f"{cakes.BURNT}_side")] = sheet(charred(faces["side"]))
    out[("block", f"{cakes.BURNT}_inside")] = sheet(charred(cut))
    out[("item", cakes.BATTER)] = batter_icon()
    return out
