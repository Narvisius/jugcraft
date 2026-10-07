"""Original textures for the Spirit Board (fall additions 17) (requires Pillow): the board's lettered face (a 64 by 48
entity texture SpiritBoardRenderer lays over the board: YES by a sun and NO by a moon at the top, two arcs of letters,
the numbers and GOODBYE, in a three-by-five letter of its own, inside a double border), the walnut planchette (a heart
with a gold rim and a glass lens, pointed end up) and its wood; the board's birch top and dark edge as block textures;
and the item.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured. The letters sit where SpiritBoard.place() says, which tools/check_mod_data.py checks.
"""
import math

from PIL import Image

from crop_textures import Canvas, rgb, outline
from halloween_textures import shade
import block_style as bs

BIRCH = [rgb("dccaa0"), rgb("e4d4ac"), rgb("eadcb6"), rgb("f0e4c2")]
INK = rgb("2a1a10")
GOLD = rgb("b8902c")
MOON = rgb("4a5878")
EDGE = [rgb("3a2416"), rgb("4a301e"), rgb("5a3a24")]
WALNUT = [rgb("4a2c18"), rgb("5a3820"), rgb("6a4428"), rgb("7a5030")]
GLASS = rgb("b8d0d8")
SHINE = rgb("f4fbff")

# A three-by-five letter: each glyph is five rows of three, '#' inked.
GLYPHS = {
    "A": [".#.", "#.#", "###", "#.#", "#.#"], "B": ["##.", "#.#", "##.", "#.#", "##."], "C": [".##", "#..", "#..", "#..", ".##"],
    "D": ["##.", "#.#", "#.#", "#.#", "##."], "E": ["###", "#..", "##.", "#..", "###"], "F": ["###", "#..", "##.", "#..", "#.."],
    "G": [".##", "#..", "#.#", "#.#", ".##"], "H": ["#.#", "#.#", "###", "#.#", "#.#"], "I": ["###", ".#.", ".#.", ".#.", "###"],
    "J": ["..#", "..#", "..#", "#.#", ".#."], "K": ["#.#", "#.#", "##.", "#.#", "#.#"], "L": ["#..", "#..", "#..", "#..", "###"],
    "M": ["#.#", "###", "###", "#.#", "#.#"], "N": ["##.", "#.#", "#.#", "#.#", "#.#"], "O": [".#.", "#.#", "#.#", "#.#", ".#."],
    "P": ["##.", "#.#", "##.", "#..", "#.."], "Q": [".#.", "#.#", "#.#", "##.", ".##"], "R": ["##.", "#.#", "##.", "#.#", "#.#"],
    "S": [".##", "#..", ".#.", "..#", "##."], "T": ["###", ".#.", ".#.", ".#.", ".#."], "U": ["#.#", "#.#", "#.#", "#.#", "###"],
    "V": ["#.#", "#.#", "#.#", "#.#", ".#."], "W": ["#.#", "#.#", "###", "###", "#.#"], "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "Y": ["#.#", "#.#", ".#.", ".#.", ".#."], "Z": ["###", "..#", ".#.", "#..", "###"],
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"], "2": ["##.", "..#", ".#.", "#..", "###"],
    "3": ["##.", "..#", ".#.", "..#", "##."], "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "##.", "..#", "##."],
    "6": [".##", "#..", "###", "#.#", "###"], "7": ["###", "..#", ".#.", ".#.", ".#."], "8": ["###", "#.#", "###", "#.#", "###"],
    "9": ["###", "#.#", "###", "..#", "##."],
}
FACE = (64, 48)


def arc(i):
    """How far up the i-th of an arc's thirteen letters is raised (SpiritBoard.arc)."""
    return round(3.0 * math.sin(math.pi * (i + 0.5) / 13.0))


def letter_places():
    """Each letter or number's top-left pixel on the face, as SpiritBoard.place() puts its middle (1.5, 2.5 in)."""
    places = {}
    for i in range(13):
        places[chr(ord("A") + i)] = (6 + 4 * i, 13 - arc(i))
        places[chr(ord("N") + i)] = (6 + 4 * i, 22 - arc(i))
    for i, digit in enumerate("1234567890"):
        places[digit] = (12 + 4 * i, 31)
    return places


WORDS = {"YES": (6, 4), "NO": (49, 4), "GOODBYE": (19, 39)}


def glyph(img, char, x, y, color):
    for row, line in enumerate(GLYPHS[char]):
        for col, mark in enumerate(line):
            if mark == "#":
                img.putpixel((x + col, y + row), color + (255,))


def face():
    """The lettered face: birch grain, a double border, YES and a sun, NO and a moon, the letters, numbers and GOODBYE."""
    img = Image.new("RGBA", FACE)
    grain = bs.Field(FACE[0], FACE[1], 17001, 8.0, 1.5)    # long streaks along the board, as in planks
    for y in range(FACE[1]):
        for x in range(FACE[0]):
            img.putpixel((x, y), bs.tone(grain(x, y), BIRCH, spread=0.8) + (255,))
    for x in range(FACE[0]):
        for y in (0, FACE[1] - 1):
            img.putpixel((x, y), EDGE[1] + (255,))
        for y in (2, FACE[1] - 3):
            if 2 <= x <= FACE[0] - 3:
                img.putpixel((x, y), INK + (255,))
    for y in range(FACE[1]):
        for x in (0, FACE[0] - 1):
            img.putpixel((x, y), EDGE[1] + (255,))
        for x in (2, FACE[0] - 3):
            if 2 <= y <= FACE[1] - 3:
                img.putpixel((x, y), INK + (255,))
    # The sun between YES and the arcs, the crescent moon by NO.
    for x in range(18, 29):
        for y in range(1, 12):
            d = math.hypot(x - 23, y - 6)
            if d <= 2.5:
                img.putpixel((x, y), GOLD + (255,))
            elif 3.5 <= d <= 4.5 and (round(math.degrees(math.atan2(y - 6, x - 23))) % 45) < 15:
                img.putpixel((x, y), GOLD + (255,))
    for x in range(36, 46):
        for y in range(2, 11):
            if math.hypot(x - 41, y - 6) <= 3.5 and math.hypot(x - 42.5, y - 5) > 3.0:
                img.putpixel((x, y), MOON + (255,))
    for word, (x, y) in WORDS.items():
        for i, char in enumerate(word):
            glyph(img, char, x + 4 * i, y, INK)
    for char, (x, y) in letter_places().items():
        glyph(img, char, x, y, INK)
    # Little stars in the bottom corners.
    for cx, cy in ((7, 41), (56, 41)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            img.putpixel((cx + dx, cy + dy), GOLD + (255,))
    return img


def heart(x, y):
    """Whether (x, y) of a 32 by 32 picture is inside the planchette: a heart with its point at the top."""
    hx = (x + 0.5 - 16.0) / 13.0
    hy = (y + 0.5 - 14.0) / 13.0
    return (hx * hx + hy * hy - 1) ** 3 - hx * hx * hy ** 3 <= 0


def planchette():
    """The planchette from above: walnut with a gold rim, and a round glass lens in the middle."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    grain = bs.Field(32, 32, 17010, 6.0, 1.5)
    for x in range(32):
        for y in range(32):
            if not heart(x, y):
                continue
            edge = any(not (0 <= x + dx < 32 and 0 <= y + dy < 32 and heart(x + dx, y + dy))
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            d = math.hypot(x + 0.5 - 16.0, y + 0.5 - 16.0)
            if edge:
                color = GOLD
            elif d <= 4.0:
                color = SHINE if (x - 14) ** 2 + (y - 14) ** 2 <= 1 else GLASS
            elif d <= 5.2:
                color = GOLD
            else:
                color = bs.tone(grain(x, y), WALNUT, spread=0.8)
            img.putpixel((x, y), color + (255,))
    return img


def noise(palette, seed, weights=None):
    """A plain 16x16 of the palette in small clumps, in the manner of the vanilla blocks (tools/block_style.py)."""
    img = Image.new("RGBA", (16, 16))
    surface = bs.surface(palette, seed, weights)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), surface(x, y) + (255,))
    return img


def board_top():
    """The board's top seen from afar (where its face isn't drawn): birch with faint rows of letters."""
    img = noise(BIRCH, 17020, [1, 3, 3, 2])
    for y in (5, 8, 11):
        for x in range(2, 14, 2):
            img.putpixel((x, y), shade(INK, 2.2) + (255,))
    return img


def board_item():
    """The item: the board, its letters as dots, with the planchette on it."""
    c = Canvas()
    for y in range(3, 13):
        for x in range(1, 15):
            c.px(x, y, BIRCH[(x + y) % 4])
    for y, row in ((5, range(3, 13, 2)), (8, range(3, 13, 2))):
        for x in row:
            c.px(x, y, INK)
    for x, y in ((6, 9), (7, 9), (8, 9), (9, 9), (6, 10), (7, 10), (8, 10), (9, 10), (7, 8), (8, 8), (7, 11), (8, 11)):
        c.px(x, y, WALNUT[2])
    c.px(7, 10, GLASS)
    outline(c, EDGE[0])
    return c.img


def spirit_board_textures():
    return {("entity", "spirit_board"): face(), ("entity", "planchette"): planchette(),
            ("entity", "planchette_wood"): noise(WALNUT, 17030, [2, 3, 2, 1]),
            ("block", "spirit_board_top"): board_top(), ("block", "spirit_board_edge"): noise(EDGE, 17040, [2, 3, 2]),
            ("item", "spirit_board"): board_item()}
