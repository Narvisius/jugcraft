"""Original textures for the second batch of Halloween decorations (requires Pillow): the Luminaria in every dye
colour, Floating Candles, the Skeleton Hand Sconce and Bat Bunting.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code from fixed seeds; no Mojang texture is
read, traced or recoloured. Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a
short palette in small clumps, never a random colour at every pixel. The dye colours below are plain colour values.
Block and item textures are 16x16; the bunting's strip is 64x16.
"""
from PIL import Image

from agriculture import DYE_COLORS
from crop_textures import Canvas, rgb, outline
from halloween_textures import IRON, shade
import block_style as bs

# Paper in each dye colour: plain paper for white, otherwise paper dyed (lighter than the dye itself).
DYES = {"white": "f2ecdc", "orange": "f08a2a", "magenta": "c45ab8", "light_blue": "58b8de", "yellow": "f6d24a", "lime": "8ccc34",
        "pink": "f2a0b8", "gray": "5a6266", "light_gray": "a6a6a0", "cyan": "2aa2a2", "purple": "8a44b6", "blue": "4a54b0",
        "brown": "8a5a36", "green": "607e22", "red": "b8382e", "black": "2a2a2e"}
GLOW = rgb("ffe4a0")
WAX = [rgb("d8ccb0"), rgb("e6dcc2"), rgb("f2ead6"), rgb("faf6ea")]
FLAME = [rgb("ff9a1c"), rgb("ffc23a"), rgb("ffe27a"), rgb("fff6c8")]
BONE = [rgb("b0a688"), rgb("c8bea0"), rgb("d8cfb4"), rgb("e8e0c8")]
SAND = [rgb("c8b880"), rgb("d8c890"), rgb("e2d4a2")]
TWINE = [rgb("9a7a48"), rgb("b09060"), rgb("c8a870")]
ORANGE = [rgb("b85a0e"), rgb("e07a18"), rgb("f2922a")]
BLACK = [rgb("141416"), rgb("1e1e22"), rgb("2c2c32")]

# The face cut through each wall of the bag, in the wall's own pixels (x 0-7 across, y 0-9 down from the rim). Inside,
# the walls are plain paper (luminaria_inside), so through the holes one sees the far wall lit by the candle.
BAG_X, BAG_Y = 4, 6
BAG_HOLES = [(1, 4), (2, 4), (2, 3), (6, 4), (5, 4), (5, 3), (3, 5), (4, 5), (1, 6), (6, 6), (2, 7), (3, 7), (4, 7), (5, 7)]


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


def luminaria(color, lit):
    """Paper in `color` with a rim folded over at the top, faint creases and the face cut through; lit, the paper
    glows warm from the candle inside."""
    paper = rgb(DYES[color])
    if lit:
        paper = mix(paper, GLOW, 0.45 if color != "black" else 0.25)
    grain = bs.wobble(9200 + DYE_COLORS.index(color), 0.04)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            tone = 1.0 + grain(x, y)
            local_x, local_y = x - BAG_X, y - BAG_Y
            if local_y == 0:
                tone += 0.08  # the rim, folded over
            elif local_y == 1:
                tone -= 0.12  # its shadow
            if local_x in (0, 7):
                tone -= 0.06  # the creases at the corners
            if lit and 2 <= local_y <= 9 and 1 <= local_x <= 6:
                tone += 0.06  # brightest round the candle
            img.putpixel((x, y), shade(paper, tone) + (255,))
    for hx, hy in BAG_HOLES:
        img.putpixel((BAG_X + hx, BAG_Y + hy), (0, 0, 0, 0))
    return img


def luminaria_inside(lit):
    """The inside of the bag: plain paper in shadow, or lit warm by the candle, brightest low down in the middle."""
    grain = bs.wobble(9221 + lit, 0.03)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if lit:
                near = 1.0 - min(1.0, (abs(x - 7.5) / 8 + abs(y - 12) / 10) / 1.4)
                color = mix(rgb("f6c25a"), rgb("fff4c8"), near)
            else:
                color = shade(rgb(DYES["white"]), 0.62)
            img.putpixel((x, y), shade(color, 1.0 + grain(x, y)) + (255,))
    return img


def luminaria_sand():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, SAND, 9231, [2, 3, 2])
    return c.img


def luminaria_candle():
    """Cream wax with a drip or two below the top, and the wick in the middle of the top."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, WAX[1:], 9241, [2, 3, 1])
    for x, y in ((7, 10), (8, 11), (8, 10)):
        c.px(x, y, WAX[3])
    c.px(8, 7, rgb("2a2018"))
    return c.img


def flame():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, FLAME[1:], 9251, [2, 3, 2])
    return c.img


# ---------------------------------------------------------------- floating candles

def floating_candle_entity():
    """The renderer's texture: four 2-pixel strips of wax with drips at the top (u 0-8), a flame (u 8-12, 6 rows) and
    the top of the wax with its wick (u 12-14, 2 rows)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wax = bs.surface(WAX[:3], 9301)
    for y in range(16):
        for x in range(8):
            img.putpixel((x, y), wax(x, y) + (255,))
    for x, depth in ((0, 2), (1, 1), (2, 3), (3, 1), (4, 1), (5, 2), (6, 1), (7, 3)):
        for y in range(depth):
            img.putpixel((x, y), WAX[3] + (255,))
    # The flame: a teardrop, white at the heart, orange at the edge.
    shape = {0: (10, 10), 1: (9, 10), 2: (9, 11), 3: (8, 11), 4: (8, 11), 5: (9, 10)}
    for y, (x0, x1) in shape.items():
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (0, 5)
            img.putpixel((x, y), (FLAME[0] if edge else FLAME[3] if y >= 3 else FLAME[2]) + (255,))
    for x in (12, 13):
        for y in (0, 1):
            img.putpixel((x, y), WAX[3] + (255,))
    img.putpixel((12, 1), rgb("2a2018") + (255,))
    return img


def floating_candle_block():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, WAX[:3], 9311, [1, 3, 2])
    return c.img


def floating_candle_item():
    """Two candles with flames, hanging at different heights, and a faint sparkle round them."""
    c = Canvas()
    for x0, top, bottom in ((4, 6, 12), (10, 4, 9)):
        for y in range(top, bottom + 1):
            c.px(x0, y, WAX[2])
            c.px(x0 + 1, y, WAX[1])
        c.px(x0, top, WAX[3])
        c.px(x0 + 1, top - 1, rgb("2a2018"))
        c.px(x0 + 1, top - 2, FLAME[2])
        c.px(x0 + 1, top - 3, FLAME[1])
        c.px(x0, top - 2, FLAME[0])
    outline(c, rgb("3a2c1c"))
    for x, y in ((2, 3), (13, 12), (8, 14), (7, 1)):
        c.px(x, y, rgb("cfe6ff"))
    return c.img


# ---------------------------------------------------------------- the skeleton hand sconce

def sconce_bone():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BONE, 9401, [1, 2, 3, 2])
    return c.img


def sconce_iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 9411, [2, 3, 1], spread=0.6)
    for x, y in ((6, 4), (9, 4), (6, 11), (9, 11)):
        c.px(x, y, IRON[3])
    return c.img


def sconce_wood():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("4a3018"), rgb("5e3e20"), rgb("74502a")], 9421, [1, 3, 2])
    return c.img


def sconce_coal():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("16161a"), rgb("222226"), rgb("303036")], 9431)
    return c.img


def sconce_item():
    """A bony hand gripping a burning torch, the forearm reaching down to an iron plate in the corner."""
    c = Canvas()
    for y in range(4, 12):
        c.px(7, y, rgb("5e3e20"))
        c.px(8, y, rgb("74502a"))
    for x, y, color in ((7, 2, FLAME[1]), (8, 2, FLAME[2]), (7, 3, FLAME[0]), (8, 3, FLAME[1]), (8, 1, FLAME[3])):
        c.px(x, y, color)
    c.rect(6, 7, 9, 9, BONE[3])
    for x in (6, 7, 8, 9):
        c.px(x, 8, BONE[1])
    c.px(10, 8, BONE[2])
    for i in range(4):
        c.px(9 + i, 10 + i, BONE[2])
        c.px(10 + i, 10 + i, BONE[1])
    c.rect(13, 13, 15, 15, IRON[1])
    c.px(14, 14, IRON[3])
    outline(c, rgb("2a2418"))
    return c.img


# ---------------------------------------------------------------- bat bunting

def pennant(img, x0, palette, seed):
    """A triangle hanging point down from a folded top."""
    paper = bs.surface(palette[1:], seed)
    for y in range(16):
        half = 7.5 * (1 - y / 16)
        for x in range(16):
            if abs(x - 7.5) <= half:
                edge = abs(x - 7.5) > half - 1
                color = palette[0] if y <= 1 or edge else paper(x, y)
                img.putpixel((x0 + x, y), color + (255,))


def bat(img, x0):
    """A paper bat, wings spread, scalloped along their trailing edges, with two orange eyes."""
    rows = {2: [(6, 6), (9, 9)], 3: [(6, 9)], 4: [(1, 14)], 5: [(0, 15)], 6: [(0, 15)], 7: [(1, 14)], 8: [(2, 4), (6, 9), (11, 13)],
            9: [(3, 3), (6, 9), (12, 12)], 10: [(7, 8)]}
    for y, spans in rows.items():
        for start, end in spans:
            for x in range(start, end + 1):
                img.putpixel((x0 + x, y), BLACK[1] + (255,))
    for x in (7, 8):
        img.putpixel((x0 + x, 4), rgb("f2a21e") + (255,))


def bunting_entity():
    img = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), TWINE[(x + y) // 2 % 3] + (255,))  # the cord's twist, two pixels wide
    pennant(img, 16, ORANGE, 9511)
    pennant(img, 32, BLACK, 9521)
    bat(img, 48)
    return img


def bunting_item():
    """A sagging cord hung with an orange pennant, a paper bat and another orange pennant, a black one between."""
    c = Canvas()
    cord = {x: 2 + round(3 * (1 - ((x - 7.5) / 7.5) ** 2)) for x in range(16)}
    for x, y in cord.items():
        c.px(x, y, TWINE[0])
    for x0, palette in ((1, ORANGE), (6, BLACK), (11, ORANGE)):
        top = max(cord[x] for x in range(x0, x0 + 4)) + 1
        for row, (a, b) in enumerate(((0, 3), (0, 3), (1, 2), (1, 2))):
            for x in range(x0 + a, x0 + b + 1):
                c.px(x, top + row, palette[2] if palette is ORANGE else palette[1])
    # A paper bat under the middle of the cord, wings spread.
    x, y = 8, 12
    for dx, dy in ((-3, -1), (3, -1), (-3, 0), (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (3, 0), (-2, 1), (-1, 1), (0, 1), (1, 1),
                   (2, 1), (-1, 2), (1, 2), (-1, -1), (1, -1)):
        c.px(x + dx, y + dy, BLACK[1])
    c.px(x - 1, y, rgb("f2a21e"))
    c.px(x + 1, y, rgb("f2a21e"))
    return c.img


def decor2_textures():
    """(kind, name) -> image for every texture of the second decorations batch."""
    out = {
        ("block", "luminaria_inside"): luminaria_inside(False),
        ("block", "luminaria_inside_lit"): luminaria_inside(True),
        ("block", "luminaria_sand"): luminaria_sand(),
        ("block", "luminaria_candle"): luminaria_candle(),
        ("block", "luminaria_flame"): flame(),
        ("entity", "floating_candle"): floating_candle_entity(),
        ("block", "floating_candle"): floating_candle_block(),
        ("item", "floating_candle"): floating_candle_item(),
        ("block", "skeleton_hand_sconce_bone"): sconce_bone(),
        ("block", "skeleton_hand_sconce_iron"): sconce_iron(),
        ("block", "skeleton_hand_sconce_wood"): sconce_wood(),
        ("block", "skeleton_hand_sconce_flame"): flame(),
        ("block", "skeleton_hand_sconce_coal"): sconce_coal(),
        ("item", "skeleton_hand_sconce"): sconce_item(),
        ("entity", "bat_bunting"): bunting_entity(),
        ("item", "bat_bunting"): bunting_item(),
    }
    for color in DYE_COLORS:
        out[("block", f"luminaria_{color}")] = luminaria(color, False)
        out[("block", f"luminaria_{color}_lit")] = luminaria(color, True)
    return out
