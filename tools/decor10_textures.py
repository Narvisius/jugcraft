"""Original textures for the tenth batch of Halloween decorations, lighting and glow (requires Pillow): the Black
Light's fixture and tube, Glow Paint's six designs (faint, and glowing under a black light) and its jar, the Witch Fire
Brazier's iron, coals and flame, the Shadow Puppet Lamp's wood, brass, candle, paper panels and the shadows they throw,
the Mini Pumpkin Stack's rind, faces and stems, and the Floating Witch Hat's felt, band, candle and flame.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code or from the small pixel-art grids below
(some shared with tools/decor9_textures.py), from fixed seeds; no Mojang texture is read, traced or recoloured. Block
textures are 16x16 (the lamp's paper panels and shadows 32x32); the glow paint designs are faint and see-through round
their shapes, the lamp's paper panels are opaque; the glowing designs, flame and shadows (entity textures) are
see-through round their shapes.
"""
import math
import random

from PIL import Image

from crop_textures import Canvas, rgb
import block_style as bs
from decor_textures import noise
from decor9_textures import BAT, IRON, put, grid

GLOW = (190, 255, 170)
FAINT_ALPHA = 70

SKULL_SHAPE = [
    "................",
    ".....XXXXXX.....",
    "....XXXXXXXX....",
    "...XXXXXXXXXX...",
    "...XX..XX..XX...",
    "...X....X...X...",
    "...XX..XXX.XX...",
    "...XXXXX.XXXX...",
    "....XXX...XX....",
    ".....XXXXXX.....",
    ".....X.X.X.X....",
    ".....XXXXXX.....",
    "................",
    "................",
    "................",
    "................"]
SPIDER_SHAPE = [
    "................",
    ".X............X.",
    "..X....XX....X..",
    "...X..XXXX..X...",
    "X...XXXXXXXX...X",
    ".XXX.XXXXXX.XXX.",
    ".....XXXXXX.....",
    "..XXXXXXXXXXXX..",
    ".X...XXXXXX...X.",
    "X...X.XXXX.X...X",
    "...X...XX...X...",
    "..X..........X..",
    ".X............X.",
    "................",
    "................",
    "................"]
HAND_SHAPE = [
    "................",
    "....X..X..X.....",
    "....X..X..X.....",
    "....XX.XX.X..X..",
    "....XX.XX.XX.X..",
    "....XXXXXXXX.X..",
    ".X..XXXXXXXXXX..",
    ".XX.XXXXXXXXXX..",
    "..XXXXXXXXXXXX..",
    "...XXXXXXXXXX...",
    "....XXXXXXXXX...",
    ".....XXXXXXX....",
    ".....XXXXXX.....",
    ".....XXXXXX.....",
    "................",
    "................"]
EYE_SHAPE = [
    "................",
    "................",
    "................",
    "......XXXX......",
    "...XXX....XXX...",
    "..X....XX....X..",
    ".X....XXXX....X.",
    "X....XXXXXX....X",
    ".X....XXXX....X.",
    "..X....XX....X..",
    "...XXX....XXX...",
    "......XXXX......",
    "................",
    "................",
    "................",
    "................"]


def web_shape():
    """A spider's web: threads out from the middle and rings across them, as a grid of X."""
    cells = [["." for _ in range(16)] for _ in range(16)]
    for angle in range(0, 360, 45):
        for r in range(8):
            x = round(7.5 + r * math.cos(math.radians(angle)))
            y = round(7.5 + r * math.sin(math.radians(angle)))
            if 0 <= x < 16 and 0 <= y < 16:
                cells[y][x] = "X"
    for radius in (2.5, 4.5, 6.5):
        for t in range(0, 360, 8):
            x = round(7.5 + radius * math.cos(math.radians(t)))
            y = round(7.5 + radius * math.sin(math.radians(t)))
            if 0 <= x < 16 and 0 <= y < 16:
                cells[y][x] = "X"
    return ["".join(row) for row in cells]


DESIGNS = {"skull": SKULL_SHAPE, "bat": BAT, "spider": SPIDER_SHAPE, "web": web_shape(), "hand": HAND_SHAPE, "eye": EYE_SHAPE}


def shape(rows, color, alpha=255):
    """A see-through texture with the shape's X in one colour."""
    c = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "X":
                put(c.img, x, y, color, alpha)
    return c.img


# ---------------------------------------------------------------- the black light

def fixture():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("121214"), rgb("18181c"), rgb("202026")], 20101, [2, 3, 1])
    return c.img


def tube(lit):
    """A fluorescent tube: violet, white-hot down the middle while it is on."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = abs(y - 7.5) / 7.5
            if lit:
                c.px(x, y, (235, 200, 255) if d < 0.3 else (170, 90, 255) if d < 0.7 else (110, 40, 200))
            else:
                c.px(x, y, (120, 110, 130) if d < 0.5 else (90, 80, 100))
    return c.img


def paint_jar():
    """The Glow Paint item: a little glass jar of glowing green paint with a cork."""
    c = Canvas()
    for y in range(5, 15):
        for x in range(4, 12):
            edge = x in (4, 11) or y == 14
            c.px(x, y, (150, 180, 170) if edge else GLOW if y > 7 else (210, 235, 225))
    c.rect(5, 3, 10, 4, rgb("8a6a44"))
    c.rect(6, 2, 9, 2, rgb("a07a50"))
    c.px(6, 9, (255, 255, 255))
    return c.img


# ---------------------------------------------------------------- the witch fire brazier

def brazier_iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 20201, [2, 3, 1])
    for x in range(0, 16, 5):
        c.px(x, 2, IRON[3])
    return c.img


def coals(lit):
    """Coals heaped like vanilla gravel: rounded lumps lit at their upper left; glowing, or burnt out."""
    c = Canvas()
    if lit:
        lumps = [[rgb("c03a10"), rgb("ff7a1a"), rgb("ffb040")], [rgb("8a2a0c"), rgb("c03a10"), rgb("ff7a1a")]]
        bs.heap(lumps, 20211, count=12, joint=rgb("2a1a14"), weights=[3, 2])(c)
    else:
        lumps = [[rgb("1a1614"), rgb("26201c"), rgb("3a302a")], [rgb("121010"), rgb("1a1614"), rgb("26201c")]]
        bs.heap(lumps, 20211, count=12, joint=rgb("0a0808"), weights=[3, 2])(c)
    return c.img


def flame():
    """A white-hot flame, see-through round its edges (tinted by the client: orange, green, purple or blue)."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            height = 1 - y / 16
            width = 7 * (1 - height) ** 0.6 * (1 - 0.5 * height)
            d = abs(x - 7.5)
            if d < width:
                core = d < width * 0.45 and y > 6
                put(c.img, x, y, (255, 255, 235) if core else (255, 230, 190), 255 if core else 200)
    return c.img


# ---------------------------------------------------------------- the shadow puppet lamp
# Drawn clean (5 October 2026, docs/ART_DIRECTION.md "Texturing: keep it clean"): flat fills from short palettes, shape
# from light, regular pattern rather than noise. The paper panels and the shadows they throw are 32 x 32, their
# silhouettes drawn from smooth shapes with nothing thinner than two texels, so they hold together on a turning shade
# and blown up on a wall.

LAMP_WOOD = [rgb("3e2614"), rgb("4e321c"), rgb("5e3e24"), rgb("74502e")]
LAMP_BRASS = [rgb("7a5a1c"), rgb("a07a2c"), rgb("c49c40"), rgb("e2c46a")]
LAMP_WAX = [rgb("cfc3a2"), rgb("e4dac0"), rgb("f2ead6"), rgb("fbf6ea")]
PAPER = [rgb("e2c48a"), rgb("ecd29c"), rgb("f4dfae")]
FRAME = [rgb("3e2614"), rgb("5a3a20"), rgb("74502e")]
INK = rgb("1a1210")
SHEET = 32


def lamp_wood():
    """The turned base: rings round its middle on top, its sides a lit upper and a shaded lower row."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            ring = int(math.hypot(x + 0.5 - 8, y + 0.5 - 8))
            c.px(x, y, LAMP_WOOD[2] if ring % 2 == 0 else LAMP_WOOD[1])
    for x in range(16):
        c.px(x, 14, LAMP_WOOD[3])
        c.px(x, 15, LAMP_WOOD[0])
    return c.img


def lamp_brass():
    """The collar: flat brass, its side lit along its upper half."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, LAMP_BRASS[3] if y == 13 else LAMP_BRASS[2] if y < 13 else LAMP_BRASS[1])
    return c.img


def lamp_candle():
    """The candle: cream wax lit down its left, a pool of melted wax on top and one drip."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, LAMP_WAX[3] if x == 7 else LAMP_WAX[2])
    for y in (7, 8):
        for x in (7, 8):
            c.px(x, y, LAMP_WAX[1])
    c.px(8, 10, LAMP_WAX[3])
    c.px(8, 11, LAMP_WAX[3])
    return c.img


def _polygon(points):
    def inside(x, y):
        n, hit = len(points), False
        for i in range(n):
            (x1, y1), (x2, y2) = points[i], points[(i + 1) % n]
            if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / ((y2 - y1) or 1e-9) + x1:
                hit = not hit
        return hit
    return inside


def _ellipse(cx, cy, rx, ry):
    return lambda x, y: ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0


def _stroke(points, r):
    """A round-ended stroke of radius r along a polyline."""
    def inside(x, y):
        for (x1, y1), (x2, y2) in zip(points, points[1:]):
            dx, dy = x2 - x1, y2 - y1
            t = max(0.0, min(1.0, ((x - x1) * dx + (y - y1) * dy) / ((dx * dx + dy * dy) or 1e-9)))
            if math.hypot(x - x1 - dx * t, y - y1 - dy * t) <= r:
                return True
        return False
    return inside


def _curve(p0, p1, p2, steps=12):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]) for t in (i / steps for i in range(steps + 1))]


# Each silhouette: the shapes that make it, and the holes (the cat's eye) cut through it; coordinates on the 32 x 32 sheet.
SILHOUETTES = {
    "bat": ([_ellipse(16, 16, 3.2, 5.0), _ellipse(16, 10, 3.4, 3.0),
             _polygon([(13.0, 9.0), (13.6, 4.2), (15.4, 8.0)]), _polygon([(19.0, 9.0), (18.4, 4.2), (16.6, 8.0)]),
             _polygon([(14.5, 12.5), (9.0, 9.0), (3.5, 8.0), (1.5, 11.0), (3.0, 17.0), (5.8, 14.6), (8.2, 18.6), (10.6, 15.6),
                       (13.2, 20.0), (15.0, 18.0)]),
             _polygon([(17.5, 12.5), (23.0, 9.0), (28.5, 8.0), (30.5, 11.0), (29.0, 17.0), (26.2, 14.6), (23.8, 18.6), (21.4, 15.6),
                       (18.8, 20.0), (17.0, 18.0)]),
             _stroke([(14.6, 20.0), (14.2, 22.6)], 1.0), _stroke([(17.4, 20.0), (17.8, 22.6)], 1.0)],
            []),
    "cat": ([_ellipse(17.5, 21.5, 7.0, 6.5), _ellipse(11.5, 11.5, 4.6, 4.2),
             _polygon([(7.6, 9.6), (7.8, 3.6), (11.6, 7.6)]), _polygon([(11.8, 7.4), (15.2, 3.8), (15.8, 10.0)]),
             _polygon([(9.0, 14.0), (14.6, 13.0), (16.0, 18.0), (11.0, 20.0)]),
             _stroke([(11.2, 18.0), (11.2, 27.0)], 1.4), _stroke([(14.4, 19.0), (14.4, 27.0)], 1.4),
             _stroke(_curve((23.0, 26.0), (30.0, 25.0), (27.0, 13.0)), 1.3)],
            [_ellipse(10.0, 11.0, 1.25, 1.3)]),
    "witch": ([_stroke([(2.5, 21.0), (24.0, 19.0)], 1.1),
               _polygon([(22.0, 16.5), (30.5, 13.0), (28.6, 16.2), (31.0, 18.0), (28.6, 19.8), (30.8, 23.6), (22.0, 21.8)]),
               _polygon([(11.4, 11.0), (15.4, 11.0), (19.6, 20.0), (8.6, 20.6)]), _polygon([(15.0, 11.6), (22.6, 13.4), (19.6, 18.0)]),
               _ellipse(13.4, 9.0, 2.7, 2.6), _polygon([(10.9, 8.8), (8.4, 10.2), (11.0, 10.6)]),
               _ellipse(13.6, 6.6, 5.4, 1.2), _polygon([(10.8, 6.6), (16.6, 6.6), (19.8, 1.2), (14.6, 3.0)]),
               _polygon([(12.0, 13.0), (9.6, 18.4), (7.4, 19.4), (9.2, 13.6)]),
               _polygon([(15.0, 20.0), (17.4, 24.4), (15.0, 25.0), (13.4, 20.6)])],
              []),
}


# How much each silhouette is shrunk about the sheet's middle, and moved (texels), to sit inside the paper's frame.
FIT = {"bat": (0.84, 0.0, 1.0), "cat": (0.9, 0.0, -0.5), "witch": (0.8, 0.0, 1.5)}


def silhouette(design):
    """design -> set of (x, y) texels of the 32 x 32 sheet inside its silhouette (tested at each texel's middle)."""
    shapes, holes = SILHOUETTES[design]
    scale, dx, dy = FIT[design]
    middle = SHEET / 2

    def at(x, y):
        return middle + (x + 0.5 - middle - dx) / scale, middle + (y + 0.5 - middle - dy) / scale
    return {(x, y) for y in range(SHEET) for x in range(SHEET)
            if any(f(*at(x, y)) for f in shapes) and not any(f(*at(x, y)) for f in holes)}


def paper(design):
    """A panel of warm, lamp-lit paper in a thin wooden frame (lit along its top and left, shaded along its bottom and
    right), with fibre lines running down it every four texels and a silhouette in ink."""
    img = Image.new("RGBA", (SHEET, SHEET))
    ink = silhouette(design)
    for y in range(SHEET):
        for x in range(SHEET):
            colour = PAPER[1] if x % 4 == 2 else PAPER[2]
            if x in (2, SHEET - 3) or y in (2, SHEET - 3):
                colour = PAPER[0]
            if (x, y) in ink:
                colour = INK
            if x < 2 or y < 2 or x >= SHEET - 2 or y >= SHEET - 2:
                lit = x < 1 or y < 1
                dark = x >= SHEET - 1 or y >= SHEET - 1
                colour = FRAME[2] if lit and not dark else FRAME[0] if dark else FRAME[1]
            img.putpixel((x, y), colour + (255,))
    return img


def puppet_shadow(design):
    """The shadow a panel throws: its silhouette in black, see-through round it."""
    img = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
    for x, y in silhouette(design):
        img.putpixel((x, y), (0, 0, 0, 255))
    return img


# ---------------------------------------------------------------- the mini pumpkin stack

PUMPKIN = [rgb("b85a10"), rgb("d06c18"), rgb("e88424"), rgb("f8a040")]

MINI_FACE = [
    "................",
    "................",
    "................",
    "...YY......YY...",
    "..YYYY....YYYY..",
    "................",
    ".......YY.......",
    "................",
    "..Y.YYYYYYYY.Y..",
    "..YYYYYYYYYYYY..",
    "...YY.YYYY.YY...",
    "................",
    "................",
    "................",
    "................",
    "................"]


def pumpkin_side():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, PUMPKIN[[0, 1, 2, 3, 2, 1][x % 6]])
    return c.img


def pumpkin_top():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            c.px(x, y, PUMPKIN[max(0, 3 - int(d / 2.5))])
    return c.img


def pumpkin_face(lit):
    glow = rgb("ffd84a") if lit else rgb("3a2208")
    return grid(MINI_FACE, {"Y": glow}, [PUMPKIN[1], PUMPKIN[2]], 20411 if lit else 20412)


def stem():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("3a5a1a"), rgb("4a6a22"), rgb("2e4814")], 20421)
    return c.img


# ---------------------------------------------------------------- the floating witch hat

def felt():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("16101c"), rgb("1e1626"), rgb("281e32")], 20501, [2, 3, 1])
    return c.img


def band():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("16101c"), rgb("1e1626")], 20511)
    c.rect(0, 9, 15, 13, rgb("e07818"))
    c.rect(6, 8, 9, 14, rgb("d8b048"))
    c.rect(7, 10, 8, 12, rgb("1e1626"))
    return c.img


def candle():
    c = Canvas()
    noise(c, 0, 0, 15, 15, [rgb("e8e0c8"), rgb("f0eadc"), rgb("dcd2b8")], 20321, [3, 2, 1])
    return c.img


def hat_flame():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, (255, 240, 160) if 4 <= x <= 11 and y >= 4 else (255, 170, 60))
    return c.img


def decor10_textures():
    """(kind, name) -> image for every texture of the tenth decorations batch."""
    out = {
        ("block", "black_light_fixture"): fixture(),
        ("block", "black_light_tube"): tube(True),
        ("block", "black_light_tube_off"): tube(False),
        ("item", "glow_paint"): paint_jar(),
        ("block", "witch_fire_brazier_iron"): brazier_iron(),
        ("block", "witch_fire_brazier_coals"): coals(True),
        ("block", "witch_fire_brazier_coals_dark"): coals(False),
        ("entity", "witch_fire_flame"): flame(),
        ("block", "shadow_puppet_lamp_wood"): lamp_wood(),
        ("block", "shadow_puppet_lamp_brass"): lamp_brass(),
        ("block", "shadow_puppet_lamp_candle"): lamp_candle(),
        ("block", "mini_pumpkin_side"): pumpkin_side(),
        ("block", "mini_pumpkin_top"): pumpkin_top(),
        ("block", "mini_pumpkin_face"): pumpkin_face(True),
        ("block", "mini_pumpkin_face_off"): pumpkin_face(False),
        ("block", "mini_pumpkin_stem"): stem(),
        ("block", "floating_witch_hat_felt"): felt(),
        ("block", "floating_witch_hat_band"): band(),
        ("block", "floating_witch_hat_candle"): candle(),
        ("block", "floating_witch_hat_flame"): hat_flame(),
    }
    for design, rows in DESIGNS.items():
        out[("block", f"glow_paint_{design}")] = shape(rows, GLOW, FAINT_ALPHA)
        out[("entity", f"glow_paint_{design}_glow")] = shape(rows, GLOW)
    for design in ("bat", "cat", "witch"):
        out[("block", f"shadow_puppet_lamp_paper_{design}")] = paper(design)
        out[("entity", f"shadow_puppet_lamp_{design}_shadow")] = puppet_shadow(design)
    return out
