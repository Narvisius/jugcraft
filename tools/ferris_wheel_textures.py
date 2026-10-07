"""Original textures for the Ferris wheel (fall addition 27), painted at 64 by 64 (docs/ART_DIRECTION.md, "High
resolution"): the A-frames' lattice steel (cut out between its bracing), the cream-enamelled steel of its braces, spokes
and bearings, the red rims with their gold pinstripe, the hub's brass sunburst, the axle, the footings and the bulbs; the
cars in pumpkin, cranberry, mustard and spruce, each with a striped canopy and a scalloped valance to match, the tufted
bench and plank floor; the booth's painted sides, its control panel, deck and speed gauge; and the item.

The wheel's and the cars' textures are drawn clean (flat fills, bands and bevels, no noise) and laid out in regions
that tools/ferris_wheel_data.py pins each face to, at about four texels to a model pixel (the client draws them
without mipmaps); the booth's (a block model) keep their earlier painted look.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, the booth's from a fixed seed
(tools/fur_paint.py's painter); no other texture is read, traced or recoloured.
"""
import math

from PIL import Image

from fur_paint import Painter, ramp
from crop_textures import rgb
from ferris_wheel import CAR_COLOURS

N = 64

CREAM = [rgb("6e6656"), rgb("a49a84"), rgb("cfc6ae"), rgb("e9e2cc"), rgb("f9f5e8")]
RED = [rgb("3e0808"), rgb("6c1010"), rgb("9c1a16"), rgb("c42a22"), rgb("e45444")]
GOLD = [rgb("5a3c0a"), rgb("8c6418"), rgb("bc9030"), rgb("e0bc5a"), rgb("fbe7a4")]
BRASS = [rgb("4a3010"), rgb("7a5420"), rgb("a8803a"), rgb("d0aa5c"), rgb("f0d898"), rgb("fff4d4")]
# The rims' gold edge, warm (more red than green) so it stays gold, not olive, where a face is turned from the light;
# and their lit red.
RIM_GOLD, RIM_LIT = rgb("e2a03e"), rgb("d8402e")
# The wheel's polished brass: a warm, saturated yellow, so thin posts and rails read as brass, not tan.
BRASS_LIT, BRASS_FILL, BRASS_SHADE = rgb("f8dc80"), rgb("dcac48"), rgb("a8782c")
RUST = [rgb("3a1a0a"), rgb("6a3014"), rgb("8e4a22")]
IRON = [rgb("16181c"), rgb("2a2e34"), rgb("444a52"), rgb("666e78"), rgb("9aa2ac")]
CONCRETE = [rgb("5a5852"), rgb("7a776e"), rgb("989488"), rgb("b4b0a2"), rgb("ccc8ba")]
PLANK = [rgb("3a2416"), rgb("5c3c24"), rgb("7e5634"), rgb("9c7048"), rgb("b88c60")]
LEATHER = [rgb("2a0a08"), rgb("4a1410"), rgb("6c2218"), rgb("8e3426"), rgb("b05040")]
GREEN = [rgb("0e2a1a"), rgb("1a4428"), rgb("2a5e38"), rgb("40804e"), rgb("6aa874")]
COLOURS = {
    "pumpkin": [rgb("5a2204"), rgb("923a08"), rgb("c85a10"), rgb("ec7c1e"), rgb("ffa850")],
    "cranberry": [rgb("3a0612"), rgb("640c20"), rgb("8e1630"), rgb("b42642"), rgb("d8506a")],
    "mustard": [rgb("5a4206"), rgb("8c6a0c"), rgb("bc9216"), rgb("dcb22c"), rgb("f4d468")],
    "spruce": [rgb("0a2418"), rgb("123c28"), rgb("1c5638"), rgb("2c744c"), rgb("54a070")],
}


def rivet(p, x, y, colours, r=1.6):
    p.blob(x, y, r, r, ramp(colours, 0.25), ramp(colours, 0.95))


# ---------------------------------------------------------------- the wheel and its cars, drawn clean
# Repainted on 5 October 2026 (docs/ART_DIRECTION.md "Texturing: keep it clean"): flat fills from the palettes above,
# light from bevels and bands, no noise and no detail finer than a model pixel where the face is thin. The client draws
# these without mipmaps, so every part samples them at about four texels to a model pixel (tools/ferris_wheel_data.py
# pins each face's uv): thin members are plain or banded along their length, never a whole picture squeezed down.

def flat(colour, size=N):
    return Image.new("RGBA", (size, size), colour + (255,))


# The metals (cream steel, brass and the axle's iron) share one layout, in uv units (four texels each), which
# tools/ferris_wheel_data.py metal() pins every face of them to by its shape. A face's bands run along it, a pixel (four
# texels) each, so nothing finer than a model pixel crosses a member: a lit band along its top or left edge, the fill
# and, on members three pixels across or more, a shaded band along the other edge. Squarer faces show a plate, lit along
# its top and left and shaded along its bottom and right.
#   strips banded across u, for faces whose length runs down v (rows 0 to 15): a member's width in pixels -> (u0, u1)
METAL_ACROSS_U = {1: (0, 1), 2: (1, 3), 3: (3, 6), 4: (6, 10), 6: (10, 16)}
#   strips banded across v, for faces whose length runs along u: a member's height in pixels -> (u0, v0, u1, v1)
METAL_ACROSS_V = {1: (0, 4, 4, 5), 2: (0, 5, 4, 7), 3: (0, 7, 4, 10), 4: (4, 4, 8, 8), 6: (12, 4, 16, 10)}
METAL_PLATE_SMALL = (0, 10, 4, 14)          # a 4-pixel plate, for faces up to 6 pixels across
METAL_PLATE_BIG = (4, 8, 12, 16)            # an 8-pixel plate, for bigger ones
METAL_FILL = (8, 4, 12, 8)                  # plain fill


def metal_bands(width):
    """A strip's bands for a member `width` pixels across: lit, fill and (from three pixels) shaded, a pixel each, the
    fill taking the rest; a member a pixel across is one tone between lit and fill. As a list of band indices (0 lit,
    1 fill, 2 shade, 3 between), one a texel."""
    texels = 4 * width
    if width == 1:
        return [3] * 4
    if width == 2:
        return [0] * 4 + [1] * 4
    return [0] * 4 + [1] * (texels - 8) + [2] * 4


def metal(lit, fill, shade, boss=None):
    """A metal's texture in the layout above: `lit`, `fill` and `shade` colours; `boss` (a pair: lit, shaded) adds a
    round boss to the big plate's middle (the brass cap over the axle's end)."""
    colours = (lit, fill, shade, tuple((a + b) // 2 for a, b in zip(lit, fill)))
    img = flat(fill)

    def put(x, y, band):
        img.putpixel((x, y), colours[band] + (255,))
    for width, (u0, u1) in METAL_ACROSS_U.items():
        for i, band in enumerate(metal_bands(width)):
            for y in range(16):
                put(u0 * 4 + i, y, band)
    for width, (u0, v0, u1, v1) in METAL_ACROSS_V.items():
        for i, band in enumerate(metal_bands(width)):
            for x in range(u0 * 4, u1 * 4):
                put(x, v0 * 4 + i, band)
    for u0, v0, u1, v1 in (METAL_PLATE_SMALL, METAL_PLATE_BIG):
        x0, y0, x1, y1 = u0 * 4, v0 * 4, u1 * 4, v1 * 4
        for y in range(y0, y1):
            for x in range(x0, x1):
                band = 0 if y < y0 + 4 or x < x0 + 4 else 2 if y >= y1 - 4 or x >= x1 - 4 else 1
                # The bevel's two corners where lit meets shaded are split on the diagonal.
                if (y < y0 + 4 and x >= x1 - 4) or (x < x0 + 4 and y >= y1 - 4):
                    band = 0 if (x - x0) + (y - y0) < (x1 - x0) else 2
                put(x, y, band)
    if boss:
        u0, v0, u1, v1 = METAL_PLATE_BIG
        cx, cy = (u0 + u1) * 2, (v0 + v1) * 2
        for y in range(v0 * 4, v1 * 4):
            for x in range(u0 * 4, u1 * 4):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(dx, dy)
                if 6 <= d < 10:
                    img.putpixel((x, y), (boss[0] if dx + dy < 0 else boss[1]) + (255,))
                elif d < 6:
                    put(x, y, 1)
    return img


def steel():
    """The cream enamel of the braces, spokes, ties, bearings and the cars' hangers and floors."""
    return metal(CREAM[4], CREAM[3], CREAM[2])


def brass():
    """Polished brass: the cars' posts, rails and yokes, the pivot's collars and the cap over the axle's end, with a
    round boss on it."""
    return metal(BRASS_LIT, BRASS_FILL, BRASS_SHADE, boss=(BRASS_LIT, BRASS_SHADE))


def axle():
    """The axle's dark iron, lit along its top, shaded under it."""
    return metal(IRON[4], IRON[3], IRON[2])


def rim():
    """The rims, banded along their length (every column alike, so any length of rim shows the same bands), every band
    a pixel (four texels) or more, so none is finer than the model: the outer ring's front and back (rows 0 to 11,
    three pixels deep) a warm gold edge round the outside, the red and a shaded red along the inside; its outer face
    (rows 12 to 19) red and its inner face (rows 20 to 27) shaded; the inner ring's front and back (rows 28 to 35) lit
    red along their outer edge and red, and its edges (rows 36 to 41) shaded."""
    rows = ([RIM_GOLD] * 4 + [RED[3]] * 4 + [RED[2]] * 4 + [RED[3]] * 8 + [RED[2]] * 8
            + [RIM_LIT] * 4 + [RED[3]] * 4 + [RED[2]] * 6)
    img = flat(RED[3])
    for y, colour in enumerate(rows):
        for x in range(N):
            img.putpixel((x, y), colour + (255,))
    return img


def lattice():
    """Lattice steel for the A-frames' legs, its gaps cut out, every member at least four texels (a model pixel) wide:
    the legs' 5-pixel faces (columns 0 to 19) two chords with a zigzag of bracing between them and a batten every eight
    pixels; their 4-pixel faces (columns 24 to 39) a ladder of battens every four pixels; and a plain patch for their
    ends (columns 44 to 63, rows 0 to 15). The pattern repeats every 64 rows, a length of leg."""
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))

    def put(x, y, colour):
        img.putpixel((x, y), colour + (255,))
    for y in range(N):
        for x in list(range(0, 4)) + list(range(16, 20)) + list(range(24, 28)) + list(range(36, 40)):
            put(x, y, CREAM[3])
        bay, yy = y // 16, y % 16
        middle = 4 + (yy + 0.5) * 12 / 16 if bay % 2 == 0 else 16 - (yy + 0.5) * 12 / 16
        for x in range(4, 16):
            if abs(x + 0.5 - middle) < 2.0:
                put(x, y, CREAM[2])
            if y % 32 < 4:
                put(x, y, CREAM[3])
        for x in range(28, 36):
            if y % 16 < 4:
                put(x, y, CREAM[3])
    for y in range(16):
        for x in range(44, N):
            put(x, y, CREAM[3])
    return img


def footing():
    """A concrete footing: its top (columns 0 to 55, rows 0 to 47, a texel to a quarter pixel) bevelled, with an iron
    anchor bolt in each corner; its sides (rows 52 to 59) lit along the top."""
    img = flat(CONCRETE[3])

    def put(x, y, colour):
        img.putpixel((x, y), colour + (255,))
    for y in range(48):
        for x in range(56):
            if y == 0 or x == 0:
                put(x, y, CONCRETE[4])
            elif y == 47 or x == 55:
                put(x, y, CONCRETE[2])
    for bx, by in ((6, 6), (46, 6), (6, 38), (46, 38)):
        for dy in range(4):
            for dx in range(4):
                put(bx + dx, by + dy, IRON[3] if dx + dy < 2 else IRON[2] if dx + dy < 5 else IRON[1])
    for x in range(N):
        put(x, 52, CONCRETE[4])
        put(x, 59, CONCRETE[2])
    return img


def hub():
    """The hub's face, laid over a disc 22 pixels across: sixteen gold rays on red inside a brass rim, round a domed
    brass boss with a bolt circle; flat tones, lit from the top left."""
    img = flat(CREAM[3])
    c = N / 2
    for y in range(N):
        for x in range(N):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            d = math.hypot(dx, dy)
            if d >= 30.5:
                colour = BRASS[2]
            elif d >= 27:
                colour = BRASS[3]
            else:
                colour = GOLD[3] if math.cos(math.atan2(dy, dx) * 16) > 0.2 else RED[3]
            if d < 10:
                colour = BRASS[1] if d >= 9 else BRASS[4] if math.hypot(dx + 3, dy + 3) < 5 else BRASS[3]
            img.putpixel((x, y), colour + (255,))
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        bx, by = int(c + 18 * math.cos(a)) - 1, int(c + 18 * math.sin(a)) - 1
        for dy in range(3):
            for dx in range(3):
                img.putpixel((bx + dx, by + dy), (IRON[3] if dx + dy < 2 else IRON[1]) + (255,))
    return img


def bulb():
    """A lamp bulb, 16 x 16 but drawn in two-by-two blocks (an 8 x 8 picture), as it covers only two pixels of the
    wheel: a brass collar round amber glass, paler to its middle."""
    small = Image.new("RGBA", (8, 8))
    glass = [rgb("e09028"), rgb("ffc850"), rgb("fff0a0")]
    for y in range(8):
        for x in range(8):
            d = math.hypot(x + 0.5 - 4, y + 0.5 - 4) / 4
            colour = BRASS[2] if d > 0.9 else glass[0] if d > 0.62 else glass[1] if d > 0.3 else glass[2]
            small.putpixel((x, y), colour + (255,))
    return small.resize((16, 16), Image.NEAREST)


def stripes(colour):
    """Seven stripes across the 64 texels, the colour at both edges and in the middle, so they read the same either way
    round (a face's uv may run either way)."""
    paint = COLOURS[colour]
    return [paint[3] if int(x * 7 / N) % 2 == 0 else CREAM[4] for x in range(N)]


def car(colour):
    """A car's paint: the big panels (rows 0 to 31, for faces 8 pixels tall) lit along the top and shaded along the
    bottom, with a cream pinstripe inset round them and a cream cartouche in the middle with a maple leaf in the colour;
    the low front panel (rows 32 to 51) the same, its cartouche lower; and plain paint for the insides and edges (rows 52 to 63)."""
    paint = COLOURS[colour]
    img = flat(paint[2])

    def put(x, y, c):
        img.putpixel((x, y), c + (255,))

    def panel(top, bottom, cartouche):
        for y in range(top, bottom):
            for x in range(N):
                put(x, y, paint[4] if y < top + 2 else paint[1] if y >= bottom - 2 else paint[3])
        for x in range(4, N - 4):
            for y in (top + 4, top + 5, bottom - 6, bottom - 5):
                put(x, y, CREAM[3])
        for y in range(top + 4, bottom - 4):
            for x in (4, 5, N - 6, N - 5):
                put(x, y, CREAM[3])
        if cartouche:
            cy = (top + bottom) / 2
            for y in range(top, bottom):
                for x in range(N):
                    e = ((x + 0.5 - N / 2) / 11) ** 2 + ((y + 0.5 - cy) / cartouche) ** 2
                    if e <= 1:
                        put(x, y, CREAM[2] if e > 0.62 else CREAM[4])
            # A maple leaf: five points and a stem, its outline tested at each texel's middle.
            k = cartouche / 7.5
            half = [(0, -4.6), (1.5, -2.0), (4.4, -3.3), (3.5, -0.6), (6.2, 0.3), (2.7, 1.9), (3.1, 3.3), (0.7, 2.6), (0.7, 4.6)]
            outline = [(x, y * k) for x, y in half + [(-x, y) for x, y in reversed(half)]]
            for y in range(top, bottom):
                for x in range(N):
                    px, py = x + 0.5 - N / 2, y + 0.5 - cy
                    inside = False
                    for (x1, y1), (x2, y2) in zip(outline, outline[1:] + outline[:1]):
                        if (y1 > py) != (y2 > py) and px < (x2 - x1) * (py - y1) / (y2 - y1) + x1:
                            inside = not inside
                    if inside:
                        put(x, y, paint[1])

    panel(0, 32, 7.5)
    panel(32, 52, 5.5)
    return img


def canopy(colour):
    """The canopy: seven stripes of the colour and cream, running from front to back."""
    row = stripes(colour)
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            img.putpixel((x, y), row[x] + (255,))
    return img


def valance(colour):
    """The valance hanging round the canopy (rows 0 to 11, for a valance 3 pixels deep): a gold braid along its top, the
    canopy's stripes, and a scallop under each stripe, a pixel deep, rimmed a shade darker; cut out below."""
    paint = COLOURS[colour]
    row = stripes(colour)
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    width = N / 7
    for x in range(N):
        u = (x + 0.5) % width / width
        bottom = 7.5 + 4.5 * math.sqrt(max(0.0, 1 - (2 * u - 1) ** 2))
        kept = [y for y in range(12) if y + 0.5 < bottom]
        for y in kept:
            colour_ = GOLD[3] if y < 2 else row[x]
            if y == kept[-1] and y >= 2:
                colour_ = paint[2] if row[x] == paint[3] else CREAM[2]
            img.putpixel((x, y), colour_ + (255,))
    return img


def seat():
    """Tufted oxblood leather: flat, lit along its front edge, a brass button in a little shaded dimple every four
    pixels, the rows offset."""
    img = flat(LEATHER[2])
    for x in range(N):
        for y in (0, 1):
            img.putpixel((x, y), LEATHER[3] + (255,))
    for row, y in enumerate(range(8, N, 16)):
        for x in range(8 if row % 2 == 0 else 0, N, 16):
            for dy in range(-2, 2):
                for dx in range(-2, 2):
                    if abs(dx + 0.5) + abs(dy + 0.5) <= 2.5:
                        img.putpixel(((x + dx) % N, y + dy), LEATHER[1] + (255,))
            for dy in (-1, 0):
                for dx in (-1, 0):
                    img.putpixel(((x + dx) % N, y + dy), (BRASS[4] if dx + dy == -2 else BRASS[2]) + (255,))
    return img


def floor():
    """The car's floor: planks running front to back, alternately lit and shaded, with a dark seam between."""
    img = flat(PLANK[2])
    for y in range(N):
        for x in range(N):
            board = x // 16
            img.putpixel((x, y), (PLANK[1] if x % 16 == 0 else PLANK[3] if board % 2 == 0 else PLANK[2]) + (255,))
    return img


def enamel(p, colours, level=0.6, streak=0.06, grime=0.1):
    """A painted metal face: even paint, faint streaks along it, a little grime gathered at its ends."""
    for y in range(N):
        for x in range(N):
            f = level + streak * (p.noise(x * 4, y * 0.5, 9.0) - 0.5) + 0.05 * (p.noise(x, y, 4.0) - 0.5)
            f -= grime * max(0.0, abs(y - N / 2) / (N / 2) - 0.75) * 4
            p.put(x, y, ramp(colours, f))


def booth_steel():
    """The booth's riveted cream steel (its lever's slot, gauge housing and deck's underside), as it was painted before
    the wheel's metals were redrawn: the booth is a block model, sampling it by position."""
    p = Painter(N, N, 27002)
    enamel(p, CREAM, 0.62)
    for y in (6, N - 7):
        for x in (8, N // 2, N - 9):
            rivet(p, x, y, CREAM, 1.8)
    return p.img


def booth_brass():
    """The booth's brass (its lever and buttons), as it was painted before the wheel's metals were redrawn."""
    p = Painter(N, N, 27007)
    for y in range(N):
        for x in range(N):
            f = 0.5 + 0.35 * math.cos((x / N) * math.pi * 2.4 + 0.7) + 0.06 * (p.noise(x, y, 8.0) - 0.5)
            p.put(x, y, ramp(BRASS, f))
    return p.img


def booth_side():
    """The booth's side: a cream panel in a red frame, a gold sunburst rising from its foot."""
    p = Painter(N, N, 27050)
    for y in range(N):
        for x in range(N):
            frame = x < 6 or x >= N - 6 or y < 6 or y >= N - 6
            p.put(x, y, ramp(RED if frame else CREAM, (0.55 if frame else 0.7) + 0.05 * (p.noise(x, y, 6.0) - 0.5)))
    c = N / 2
    for y in range(6, N - 6):
        for x in range(6, N - 6):
            dx, dy = x + 0.5 - c, (N - 6) - (y + 0.5)
            a = math.atan2(dy, dx)
            d = math.hypot(dx, dy)
            if d < 26 and math.cos(a * 14) > 0.35:
                p.put(x, y, ramp(GOLD, 0.7 - 0.2 * d / 26))
    p.blob(c, N - 7, 7, 5, ramp(GOLD, 0.35), ramp(GOLD, 1.0))
    for x, y in ((3, 3), (N - 4, 3), (3, N - 4), (N - 4, N - 4)):
        rivet(p, x, y, BRASS, 1.6)
    return p.img


def booth_front():
    """The booth's front: a riveted steel control panel in a red frame, a brass plate across its top."""
    p = Painter(N, N, 27051)
    for y in range(N):
        for x in range(N):
            frame = x < 5 or x >= N - 5 or y < 5 or y >= N - 5
            if frame:
                p.put(x, y, ramp(RED, 0.55 + 0.05 * (p.noise(x, y, 6.0) - 0.5)))
            else:
                p.put(x, y, ramp(GREEN, 0.55 - 0.15 * y / N + 0.05 * (p.noise(x, y, 5.0) - 0.5)))
    for x in range(10, N - 10):
        for y in range(9, 16):
            p.put(x, y, ramp(BRASS, 0.8 - 0.05 * (y - 9)))
    for x in range(8, N - 8, 8):
        rivet(p, x, N - 9, GREEN, 1.4)
    return p.img


def booth_deck():
    """The deck: planks with a yellow and black safety edge."""
    p = Painter(N, N, 27052)
    for y in range(N):
        for x in range(N):
            board = y // 13
            f = 0.55 + 0.06 * math.sin(x * 0.3 + board * 1.7 + 3 * p.noise(x, y, 6.0)) - (0.3 if y % 13 == 0 else 0.0)
            p.put(x, y, ramp(PLANK, f))
            edge = min(x, y, N - 1 - x, N - 1 - y)
            if edge < 5:
                p.put(x, y, (240, 196, 40) if ((x + y) // 6) % 2 == 0 else (30, 28, 26))
    return p.img


def gauge():
    """The speed gauge: a cream dial with its marks, a red zone at the top of its sweep and a black needle, in brass."""
    p = Painter(N, N, 27053)
    c = N / 2
    for y in range(N):
        for x in range(N):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            if d > 28:
                p.put(x, y, ramp(BRASS, 0.5 + 0.3 * (y < c)))
            elif d > 25:
                p.put(x, y, ramp(BRASS, 0.85))
            else:
                p.put(x, y, ramp(CREAM, 0.9))
    for k in range(11):
        a = math.radians(225 - k * 27)
        colour = RED[2] if k >= 8 else IRON[0]
        p.line(c + 17 * math.cos(a), c - 17 * math.sin(a), c + 23 * math.cos(a), c - 23 * math.sin(a), colour, width=2 if k % 5 == 0 else 1)
    a = math.radians(225 - 6 * 27)
    p.line(c, c, c + 20 * math.cos(a), c - 20 * math.sin(a), IRON[0], width=2)
    p.blob(c, c, 3, 3, ramp(BRASS, 0.2), ramp(BRASS, 0.9))
    return p.img


def item():
    """The item: a little Ferris wheel on its A-frame over its booth, its cars in their colours."""
    p = Painter(N, N, 27060)
    c, hub_y, r = N / 2, 26, 21
    for side in (-1, 1):
        p.line(c + side * 17, 60, c + side * 1, hub_y, ramp(CREAM, 0.45), width=3)
        p.line(c + side * 17, 60, c + side * 1, hub_y, ramp(CREAM, 0.8), width=1.2)
    for k in range(16):
        a = k * math.pi / 8
        p.line(c, hub_y, c + r * math.cos(a), hub_y + r * math.sin(a), ramp(CREAM, 0.7), width=1)
    for y in range(N):
        for x in range(N):
            d = math.hypot(x + 0.5 - c, y + 0.5 - hub_y)
            if r - 1.6 <= d <= r + 1.6:
                p.put(x, y, ramp(RED, 0.75 - 0.3 * (d - r + 1.6) / 3.2))
    for k in range(16):
        a = k * math.pi / 8
        p.put(c + r * math.cos(a), hub_y + r * math.sin(a), (255, 236, 150))
    p.blob(c, hub_y, 3.5, 3.5, ramp(GOLD, 0.3), ramp(GOLD, 1.0))
    for k in range(8):
        a = -math.pi / 2 + k * math.pi / 4
        x, y = c + r * math.cos(a), hub_y - r * math.sin(a)
        paint = COLOURS[CAR_COLOURS[k % 4]]
        p.line(x, y, x, y + 3, ramp(BRASS, 0.6), width=1)
        for yy in range(int(y + 3), int(y + 8)):
            for xx in range(int(x - 3), int(x + 4)):
                p.put(xx, yy, ramp(paint, 0.85 - 0.4 * (yy - y - 3) / 5))
    for y in range(56, 63):
        for x in range(int(c - 7), int(c + 7)):
            p.put(x, y, ramp(RED if y > 57 else PLANK, 0.6))
    return p.img


def ferris_wheel_textures():
    blocks = {"ferris_wheel_lattice": lattice(), "ferris_wheel_steel": steel(), "ferris_wheel_rim": rim(), "ferris_wheel_hub": hub(),
              "ferris_wheel_axle": axle(), "ferris_wheel_footing": footing(), "ferris_wheel_brass": brass(), "ferris_wheel_bulb": bulb(),
              "ferris_wheel_seat": seat(), "ferris_wheel_floor": floor(), "ferris_wheel_booth_side": booth_side(),
              "ferris_wheel_booth_front": booth_front(), "ferris_wheel_booth_deck": booth_deck(), "ferris_wheel_gauge": gauge(),
              "ferris_wheel_booth_steel": booth_steel(), "ferris_wheel_booth_brass": booth_brass()}
    for colour in CAR_COLOURS:
        blocks[f"ferris_wheel_car_{colour}"] = car(colour)
        blocks[f"ferris_wheel_canopy_{colour}"] = canopy(colour)
        blocks[f"ferris_wheel_valance_{colour}"] = valance(colour)
    out = {("block", name): img for name, img in blocks.items()}
    out[("item", "ferris_wheel")] = item()
    return out
