"""Original textures for the eleventh batch of Halloween decorations, party games (requires Pillow): the Jump-Scare Trap's
crate, lid, ghost and spring, the Costume Runway's carpet, braid and footlights, the Judges' Table's drape, bell, ballot
box and score cards, the Best Costume Ribbon, the Skeleton Pin's ribbon (its bones and skull are the Poseable
Skeleton's), the Bowling Pumpkin, the Bowling Scoreboard's frame, slate and chalk, the Candy Cache's bark, knot-hole,
rings, moss and hollow, the Monster Mash Dance Floor's tiles and their glow, the Ghost Bell's post, bronze, iron and
ghost, and the Fortune Teller's Table's cloth, spirit board, tarot cards and planchette.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code or from the small pixel-art grids below,
from fixed seeds; no Mojang texture is read, traced or recoloured. Block textures are 16x16 and opaque; the ribbon item
and the dance floor's glow (an entity texture) are see-through round their shapes. The spirit board is drawn at 32x16
and cut into two block textures, one for each half of the board.

Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a short palette in small clumps,
never a random colour at every pixel; wood as planks, and straw, bark and hair as streaks.
"""

from PIL import Image

from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import IRON, BONE, MOSS, BARK, put, grid

PLANK = [rgb("6e5232"), rgb("7e5e3a"), rgb("8e6c44"), rgb("5c4428")]
DARK_WOOD = [rgb("2a1e14"), rgb("34261a"), rgb("3e2e20")]
WHITE = [rgb("d4d6dc"), rgb("e4e6ea"), rgb("f2f3f5"), rgb("ffffff")]
RED_VELVET = [rgb("7a0a16"), rgb("8e101e"), rgb("a41826"), rgb("ba2232")]
PURPLE_VELVET = [rgb("2e1242"), rgb("3a1852"), rgb("482064"), rgb("582a78")]
GOLD = [rgb("8a6a1e"), rgb("b8902e"), rgb("d8b048"), rgb("f0d478")]
BRONZE = [rgb("5a4220"), rgb("74562a"), rgb("8c6a34"), rgb("4a7a5e")]
ORANGE = [rgb("a84a0c"), rgb("c85e16"), rgb("e07422"), rgb("f49038")]
SLATE = [rgb("1c2420"), rgb("222c26"), rgb("28322c")]
BLACK = rgb("0c0a0a")

# A small pixel font, rows of "X" (ink) and "." (none), for the board's words and the score cards' numbers.
FONT = {
    "Y": ["X.X", "X.X", ".X.", ".X.", ".X."],
    "E": ["XXX", "X..", "XX.", "X..", "XXX"],
    "S": [".XX", "X..", ".X.", "..X", "XX."],
    "N": ["X..X", "XX.X", "X.XX", "X..X", "X..X"],
    "O": [".X.", "X.X", "X.X", "X.X", ".X."],
    "G": [".XX", "X..", "X.X", "X.X", ".XX"],
    "D": ["XX.", "X.X", "X.X", "X.X", "XX."],
    "B": ["XX.", "X.X", "XX.", "X.X", "XX."],
    "0": ["XXX", "X.X", "X.X", "X.X", "XXX"],
    "1": ["X", "X", "X", "X", "X"],
    "8": ["XXX", "X.X", "XXX", "X.X", "XXX"],
    "9": ["XXX", "X.X", "XXX", "..X", "XXX"],
}


def text_width(text, scale=1):
    return sum(len(FONT[ch][0]) * scale for ch in text) + (len(text) - 1) * scale


def write_text(img, x, y, text, color, scale=1):
    """Writes `text` in the pixel font with its top-left at (x, y)."""
    for ch in text:
        rows = FONT[ch]
        for row, line in enumerate(rows):
            for col, mark in enumerate(line):
                if mark == "X":
                    for dy in range(scale):
                        for dx in range(scale):
                            img.putpixel((x + col * scale + dx, y + row * scale + dy), color + (255,))
        x += (len(rows[0]) + 1) * scale


# ---------------------------------------------------------------- the jump-scare trap

def planks(seed, palette=PLANK):
    """Rough planks laid across, four pixels wide, dark gaps between them and a nail at each end."""
    c = Canvas()
    bs.planks([palette[3]] + palette[:3], seed)(c)
    for y in range(1, 16, 4):
        c.px(1, y, IRON[0])
        c.px(14, y, IRON[0])
    return c.img


QUESTION = [
    "................",
    "................",
    "......RRRR......",
    ".....RR..RR.....",
    ".........RR.....",
    "........RR......",
    ".......RR.......",
    ".......RR.......",
    "................",
    ".......RR.......",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................"]


def crate_front():
    """The crate's front, a red question mark painted across its planks."""
    c = Canvas()
    base = planks(21102)
    for y in range(16):
        for x in range(16):
            # The front shows the lower ten rows of the texture (the crate is ten pixels tall), so the mark sits low.
            mark = QUESTION[y - 5][x] if 5 <= y else "."
            c.px(x, y, rgb("b01c1c") if mark == "R" else base.getpixel((x, y))[:3])
    return c.img


def lid():
    c = Canvas()
    base = planks(21103)
    for y in range(16):
        for x in range(16):
            c.px(x, y, IRON[2] if 7 <= x <= 8 else base.getpixel((x, y))[:3])
    for y in (2, 7, 13):
        c.px(7, y, IRON[3])
    return c.img


def inside():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, DARK_WOOD, 21104)
    return c.img


def band():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[1:4], 21105, [3, 2, 1])
    for y in (2, 8, 13):
        c.px(7, y, IRON[3])
    return c.img


def sheet(seed):
    """A white sheet with soft grey folds."""
    c = Canvas()
    cloth = bs.surface(WHITE[1:], seed, spread=0.6)
    for y in range(16):
        for x in range(16):
            fold = (x + y // 3) % 6 == 0
            c.px(x, y, WHITE[0] if fold else cloth(x, y))
    return c.img


SCREAM = [
    "................",
    "................",
    "...KKK....KKK...",
    "..KKKKK..KKKKK..",
    "..KKKKK..KKKKK..",
    "..KKKKK..KKKKK..",
    "...KKK....KKK...",
    "................",
    "......KKKK......",
    ".....KKKKKK.....",
    ".....KKKKKK.....",
    ".....KKKKKK.....",
    ".....KKKKKK.....",
    "......KKKK......",
    "................",
    "................"]


def scream_face():
    return grid(SCREAM, {"K": BLACK}, WHITE[1:4], 21107)


def spring():
    """Steel coils, slanting."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, [rgb("6a6e76"), rgb("9aa0a8"), rgb("c8ccd2"), rgb("3a3e44")][(y + x // 4) % 4])
    return c.img


# ---------------------------------------------------------------- the costume runway

def velvet(palette, seed):
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, palette, seed, [1, 3, 3, 1])
    return c.img


def braid():
    """Gold braid: slanting cords."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, GOLD[(x + y) % 4 if (x + y) % 4 else 0] if (x - y) % 4 else GOLD[3])
    return c.img


def footlight():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            c.px(x, y, rgb("fffbe0") if d < 4 else rgb("ffe68a") if d < 7 else rgb("e8b840"))
    return c.img


# ---------------------------------------------------------------- the judges' table

def drape(palette, seed, stars=False):
    """Velvet hanging to a gold trim and fringe at its foot (the bottom rows), with gold stars sprinkled if `stars`."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, palette, seed, [1, 3, 3, 1])
    for x in range(16):
        c.px(x, 11, GOLD[2])
        for y in range(12, 16):
            c.px(x, y, GOLD[1 + (x % 2)] if (x % 2 == 0 or y < 14) else palette[0])
    if stars:
        for x, y in ((3, 2), (11, 4), (6, 7), (13, 9), (1, 8)):
            c.px(x, y, GOLD[3])
    return c.img


def velvet_top(palette, seed, stars=False):
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, palette, seed, [1, 3, 3, 1])
    if stars:
        for x, y in ((2, 3), (12, 2), (7, 9), (14, 12), (4, 13)):
            c.px(x, y, GOLD[3])
    return c.img


def polished(palette, seed):
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, palette, seed)
    for y in range(16):
        c.px(4, y, palette[-1])
    return c.img


def ballot():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("141214"), rgb("1c1a1c"), rgb("242024")], 21141)
    for x in range(16):
        c.px(x, 1, GOLD[1])
        c.px(x, 14, GOLD[1])
    return c.img


def ballot_slot():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("141214"), rgb("1c1a1c"), rgb("242024")], 21142)
    c.rect(3, 7, 12, 8, rgb("020202"))
    return c.img


def score_card(number):
    """A white card with a black edge and a big black number."""
    img = Image.new("RGBA", (16, 16), rgb("f4f0e4") + (255,))
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), rgb("202020") + (255,))
    if number is not None:
        text = str(number)
        width = text_width(text, 2)
        write_text(img, (16 - width) // 2, 3, text, BLACK, 2)
    return img


ROSETTE = [
    "................",
    "....PO.PO.P.....",
    "...OPOPOPOPO....",
    "..POPOGGGOPOP...",
    "..OPOGGGGGOPO...",
    "..POGGKGKGGOP...",
    "..OPGGKKKGGPO...",
    "..POGGGGGGGOP...",
    "...OPOGGGOPO....",
    "....POPOPOP.....",
    ".....PP.OO......",
    ".....PP.OO......",
    "....PPP.OOO.....",
    "....PP...OO.....",
    "....P.....O.....",
    "................"]


def rosette():
    """The Best Costume Ribbon: a purple and orange rosette round a gold medallion with a little black mask on it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    colours = {"P": rgb("6a2a9a"), "O": rgb("e8781e"), "G": rgb("e8c050"), "K": rgb("161216")}
    for y, row in enumerate(ROSETTE):
        for x, ch in enumerate(row):
            if ch != ".":
                put(img, x, y, colours[ch])
    return img


# ---------------------------------------------------------------- pumpkin bowling

def pin_band():
    """A red ribbon with a white stripe, as round a bowling pin's neck."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("f0eee8") if 7 <= y <= 8 else rgb("b01a1a") if y in (6, 9) else rgb("c82222"))
    return c.img


def rind(seed):
    """Orange rind in ribs."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, ORANGE[0 if x % 4 == 0 else 2 if x % 4 == 2 else 1])
    return c.img


def holes():
    """The rind drilled with three finger holes, two above and one below."""
    c = Canvas()
    base = rind(21171)
    for y in range(16):
        for x in range(16):
            c.px(x, y, base.getpixel((x, y))[:3])
    for hx, hy in ((4, 4), (10, 4), (7, 9)):
        c.rect(hx, hy, hx + 2, hy + 2, rgb("1a0c04"))
        c.px(hx, hy, rgb("5a2a08"))
    return c.img


def pumpkin_top():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            ribbed = (abs(x - 7.5) < 0.8 or abs(y - 7.5) < 0.8 or abs(abs(x - 7.5) - abs(y - 7.5)) < 0.8)
            c.px(x, y, rgb("6a4a12") if d < 2 else ORANGE[0] if ribbed else ORANGE[2])
    return c.img


def pumpkin_stem():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("3e4a1c"), rgb("4e5a22"), rgb("5e6a2a")], 21174)
    return c.img


def frame_wood():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("5a3a1e"), rgb("6a4626"), rgb("7a522e")], 21181)
    for y in range(0, 16, 5):
        for x in range(16):
            c.px(x, y, rgb("4a2e16"))
    return c.img


def slate():
    """A dark slate, rubbed with old chalk."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, SLATE, 21182, [3, 2, 1])
    for x, y in ((3, 2), (4, 2), (11, 5), (6, 9), (7, 9), (13, 12), (2, 13)):
        c.px(x, y, rgb("3c4640"))  # old chalk, rubbed in a few smudges
    return c.img


def chalk():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("e8e8e2"), rgb("f4f4f0"), rgb("ffffff")], 21184)
    return c.img


# ---------------------------------------------------------------- the candy cache

def mossy_bark(seed):
    """Dark bark in furrows, flecked with moss."""
    c = Canvas()
    bs.streaks(BARK[1:], seed, along=6.0)(c)
    for x in range(0, 16, 4):
        for y in range(16):
            if (y + x) % 7 not in (0, 1):
                c.px(x, y, BARK[0])  # the furrows, broken here and there
    bs.moss_over(c, MOSS, seed + 1, amount=0.15, ground=4)
    return c.img


def knot():
    """The bark round a dark knot-hole, just big enough for a hand."""
    c = Canvas()
    base = mossy_bark(21191)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) / 3.2) ** 2 + ((y - 9.5) / 2.6) ** 2
            c.px(x, y, rgb("060403") if d < 1 else BARK[3] if d < 1.6 else base.getpixel((x, y))[:3])
    return c.img


def rings():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            c.px(x, y, BARK[0] if d > 7 else [rgb("6a5038"), rgb("7c6046")][int(d) % 2])
    return c.img


def moss():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, MOSS, 21194, [1, 2, 2])
    return c.img


def hollow():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("0a0806"), rgb("120e0a"), rgb("1a140e")], 21195)
    return c.img


# ---------------------------------------------------------------- the monster mash dance floor

LAMPS = [rgb("4a1e0a"), rgb("2a1240"), rgb("1a3010"), rgb("3a0a2a")]


def dance_tile():
    """Black glass over a four by four grid of coloured lamps, dim until the music lights them."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0:
                c.px(x, y, rgb("2c2c32"))
            else:
                lamp = LAMPS[(x // 4 + y // 4) % 4]
                c.px(x, y, lamp if (x % 4, y % 4) != (1, 1) else tuple(min(255, v + 40) for v in lamp))
    return c.img


def dance_side():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("0c0c10"), rgb("121216"), rgb("18181e")], 21202)
    for x in range(16):
        c.px(x, 0, rgb("8a8c94"))
        c.px(x, 1, rgb("4a4c54"))
    return c.img


def dance_glow():
    """The light a lit tile gives off: white, brightest in the middle of each of its sixteen lamps (tinted by the
    renderer)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0:
                img.putpixel((x, y), (255, 255, 255, 60))
            else:
                img.putpixel((x, y), (255, 255, 255, 230 if (x % 4, y % 4) == (2, 2) else 190))
    return img


# ---------------------------------------------------------------- the ghost bell

def post_wood():
    c = Canvas()
    bs.planks(DARK_WOOD, 21211, vertical=True)(c)
    return c.img


def bronze():
    """Old bronze, gone green in places."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BRONZE, 21212, [2, 3, 3, 1])
    for y in range(16):
        c.px(5, y, rgb("a8844a"))
    return c.img


def iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 21213)
    return c.img


GHOST_FACE = [
    "................",
    "................",
    "................",
    "....KK....KK....",
    "...KKKK..KKKK...",
    "...KKKK..KKKK...",
    "....KK....KK....",
    "................",
    "................",
    "......KKKK......",
    ".....KK..KK.....",
    "......KKKK......",
    "................",
    "................",
    "................",
    "................"]


def ghost_face():
    return grid(GHOST_FACE, {"K": BLACK}, WHITE[1:4], 21215)


# ---------------------------------------------------------------- the fortune teller's table

PARCHMENT = [rgb("a87c4a"), rgb("b88a54"), rgb("c49a62")]


def spirit_board():
    """The spirit board at 32x16: YES and NO in the top corners, a moon and a sun between them, two rows of letters
    (worn to little bars) and GOODBYE along the foot, inside a dark border."""
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 255))
    parchment = bs.surface(PARCHMENT, 21221, spread=0.6, size=(32, 16))
    for y in range(16):
        for x in range(32):
            edge = x in (0, 31) or y in (0, 15)
            img.putpixel((x, y), (rgb("3a2410") if edge else parchment(x, y)) + (255,))
    ink = rgb("1e1208")
    write_text(img, 2, 1, "YES", ink)
    write_text(img, 30 - text_width("NO"), 1, "NO", ink)
    for x, y in ((14, 2), (15, 1), (15, 2), (15, 3), (14, 4)):            # a crescent moon
        img.putpixel((x, y), ink + (255,))
    for x, y in ((18, 2), (17, 2), (19, 2), (18, 1), (18, 3)):            # a sun
        img.putpixel((x, y), rgb("8a2a10") + (255,))
    for row, y in enumerate((7, 9)):
        for x in range(4 + row, 28, 3):
            img.putpixel((x, y), ink + (255,))
            img.putpixel((x, y - 1), ink + (255,))
    write_text(img, (32 - text_width("GOODBYE")) // 2, 10, "GOODBYE", ink)
    return img


def board_half(half):
    board = spirit_board()
    return board.crop((0, 0, 16, 16) if half == "left" else (16, 0, 32, 16))


def card_back():
    """A tarot card's back: deep purple, a double gold border and a gold eye in the middle."""
    img = Image.new("RGBA", (16, 16), rgb("2a1240") + (255,))
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), GOLD[2] + (255,))
    for i in range(2, 14):
        for x, y in ((i, 2), (i, 13), (2, i), (13, i)):
            img.putpixel((x, y), GOLD[1] + (255,))
    for x, y in ((5, 7), (6, 6), (7, 6), (8, 6), (9, 6), (10, 7), (6, 8), (7, 9), (8, 9), (9, 8), (7, 7), (8, 7), (8, 8), (7, 8)):
        img.putpixel((x, y), GOLD[3] + (255,))
    return img


# The tarot cards' pictures, 8x8: the Moon, the Bat, the Pumpkin, the Ghost, the Cat and Death.
CARD_PICTURES = {
    "moon": (["..MMM...", ".MM.....", "MM......", "MM......", "MM......", "MM......", ".MM.....", "..MMM..."], {"M": rgb("e8d878")}),
    "bat": (["........", "..K..K..", "..KKKK..", "K.KKKK.K", "KKKKKKKK", ".KKKKKK.", ".K.KK.K.", "........"], {"K": rgb("1a1420")}),
    "pumpkin": (["...G....", "..OOOO..", ".OOOOOO.", "OKOOOOKO", "OOOOOOOO", "OKKKKKKO", ".OOOOOO.", "..OOOO.."],
                {"G": rgb("3e6a1c"), "O": rgb("e07422"), "K": rgb("2a1004")}),
    "ghost": (["..WWWW..", ".WWWWWW.", ".WKWWKW.", ".WWWWWW.", ".WWKKWW.", ".WWWWWW.", ".WWWWWW.", ".W.WW.W."], {"W": rgb("ffffff"), "K": BLACK}),
    "cat": (["K.....K.", "KK...KK.", "KKKKKKK.", "KYKKKYK.", "KKKKKKK.", ".KKKKK..", ".KKKKK..", "KK.K.KK."], {"K": rgb("141018"), "Y": rgb("e8d020")}),
    "skull": ([".BBBBBB.", "BBBBBBBB", "BKKBBKKB", "BKKBBKKB", "BBBKKBBB", ".BBBBBB.", ".BKBKBK.", "..BBBB.."], {"B": rgb("ece4cc"), "K": BLACK}),
}
CARD_ORDER = ("moon", "bat", "pumpkin", "ghost", "cat", "skull")


def card_face(name):
    """A tarot card: a gold border round a night-blue field with its picture, and its number in bars below."""
    rows, colours = CARD_PICTURES[name]
    img = Image.new("RGBA", (16, 16), rgb("1a2040") + (255,))
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), GOLD[2] + (255,))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colours:
                img.putpixel((4 + x, 2 + y), colours[ch] + (255,))
    number = CARD_ORDER.index(name) + 1
    start = 8 - number
    for i in range(number):
        img.putpixel((start + i * 2, 12), GOLD[3] + (255,))
        img.putpixel((start + i * 2, 13), GOLD[3] + (255,))
    return img


def planchette():
    """Pale wood with a darker rim and a round glass lens in the middle."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("c8a46c"), rgb("d4b07a"), rgb("dcbc88")], 21231)
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            c.px(x, y, rgb("8a6438"))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.2:
                c.px(x, y, rgb("d8eef4") if d < 2.2 else rgb("5a4020"))
    return c.img


def decor11_textures():
    """(kind, name) -> image for every texture of the eleventh decorations batch."""
    out = {
        ("block", "jump_scare_trap_crate"): planks(21101),
        ("block", "jump_scare_trap_front"): crate_front(),
        ("block", "jump_scare_trap_lid"): lid(),
        ("block", "jump_scare_trap_inside"): inside(),
        ("block", "jump_scare_trap_band"): band(),
        ("block", "jump_scare_trap_ghost"): sheet(21106),
        ("block", "jump_scare_trap_face"): scream_face(),
        ("block", "jump_scare_trap_spring"): spring(),
        ("block", "costume_runway_carpet"): velvet(RED_VELVET, 21121),
        ("block", "costume_runway_edge"): braid(),
        ("block", "costume_runway_light"): footlight(),
        ("block", "judges_table_cloth"): drape(RED_VELVET, 21131),
        ("block", "judges_table_top"): velvet_top(RED_VELVET, 21132),
        ("block", "judges_table_brass"): polished(GOLD[1:], 21133),
        ("block", "judges_table_ballot"): ballot(),
        ("block", "judges_table_ballot_slot"): ballot_slot(),
        ("block", "judges_table_card_back"): score_card(None),
        ("item", "best_costume_ribbon"): rosette(),
        ("block", "skeleton_pin_band"): pin_band(),
        ("block", "bowling_pumpkin_rind"): rind(21170),
        ("block", "bowling_pumpkin_holes"): holes(),
        ("block", "bowling_pumpkin_top"): pumpkin_top(),
        ("block", "bowling_pumpkin_stem"): pumpkin_stem(),
        ("block", "bowling_scoreboard_frame"): frame_wood(),
        ("block", "bowling_scoreboard_slate"): slate(),
        ("block", "bowling_scoreboard_chalk"): chalk(),
        ("block", "candy_cache_bark"): mossy_bark(21190),
        ("block", "candy_cache_knot"): knot(),
        ("block", "candy_cache_rings"): rings(),
        ("block", "candy_cache_moss"): moss(),
        ("block", "candy_cache_hollow"): hollow(),
        ("block", "dance_floor"): dance_tile(),
        ("block", "dance_floor_side"): dance_side(),
        ("entity", "dance_floor_glow"): dance_glow(),
        ("block", "ghost_bell_wood"): post_wood(),
        ("block", "ghost_bell_brass"): bronze(),
        ("block", "ghost_bell_iron"): iron(),
        ("block", "ghost_bell_ghost"): sheet(21214),
        ("block", "ghost_bell_face"): ghost_face(),
        ("block", "fortune_table_cloth"): drape(PURPLE_VELVET, 21220, stars=True),
        ("block", "fortune_table_top"): velvet_top(PURPLE_VELVET, 21222, stars=True),
        ("block", "fortune_board_left"): board_half("left"),
        ("block", "fortune_board_right"): board_half("right"),
        ("block", "fortune_card_back"): card_back(),
        ("block", "fortune_planchette"): planchette(),
    }
    for number in (8, 9, 10):
        out[("block", f"judges_table_card_{number}")] = score_card(number)
    for index, name in enumerate(CARD_ORDER):
        out[("block", f"fortune_card_{index}")] = card_face(name)
    return out
