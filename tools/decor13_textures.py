"""Original textures for the thirteenth batch of Halloween decorations, treats (requires Pillow): the Witch's Brew Punch
Bowl's glass, iron stand, glowing punch, floating eyeballs and ladle, the Barmbrack's crust, glazed top, fruited crumb
and its item and ring, the Giant Candy props (candy corn, a swirled lollipop, a wrapped sweet and a gumdrop), and the
treats' items: soul cakes, pumpkin bread, spiderweb cupcakes, bat-wing cookies, a pumpkin spice latte and witch's brew
punch.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code or from the small pixel-art grids below,
from fixed seeds; no Mojang texture is read, traced or recoloured. Block textures are 16x16 and opaque, except the
bowl's glass (half see-through in the middle) and the punch (translucent); the items are see-through round their shapes.

Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a short palette in small clumps,
never a random colour at every pixel; wood as planks, and straw, bark and hair as streaks.
"""
import math

from PIL import Image

from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import put

IRON = [rgb("1e1e22"), rgb("2a2a30"), rgb("36363e"), rgb("4a4a54")]
PUNCH = [rgb("2a9a2a"), rgb("3ac83a"), rgb("5ae04a"), rgb("8af86a")]
CRUST = [rgb("5e3214"), rgb("6a3a16"), rgb("7a4620"), rgb("8a5228")]
GLAZE = [rgb("7a4214"), rgb("8e5018"), rgb("a8682a"), rgb("d09a5a")]
CRUMB = [rgb("c89a5a"), rgb("d6aa6a"), rgb("e0b878")]
RAISIN = [rgb("3a1a12"), rgb("4a2216")]
PEEL = rgb("e07a1a")
CHERRY = rgb("a81a1a")
ORANGE = [rgb("c85a0c"), rgb("e86a10"), rgb("f07c1c"), rgb("f89030")]
PURPLE = [rgb("4a1672"), rgb("5a1e8a"), rgb("6e2aa2"), rgb("8238ba")]
CANDY_WHITE = [rgb("e8e2d0"), rgb("f4f0e2"), rgb("faf6ea")]
CANDY_YELLOW = [rgb("e8b812"), rgb("f4c81a"), rgb("f8d432"), rgb("fce04a")]
GUMDROP = [rgb("2a9a2a"), rgb("3ab43a"), rgb("4ac84a")]
CHOCOLATE = [rgb("2e1a10"), rgb("3e2414"), rgb("4e2e1a"), rgb("5e3a22")]
GLASS = [rgb("4a6a56"), rgb("6a8a76")]


# ---------------------------------------------------------------- the punch bowl

def bowl_glass():
    """Pale green glass: a rim round the edges, half see-through between them, with a highlight."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), (200, 236, 210, 235) if edge else (170, 215, 188, 120))
    for y in range(3, 10):
        img.putpixel((3, y), (240, 255, 245, 170))
    img.putpixel((4, 4), (240, 255, 245, 140))
    return img


def stand():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 23101, [2, 3, 1])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.px(x, y, IRON[3])
    return c.img


def punch():
    """Glowing green punch, translucent, swirled darker and brighter."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    middle = bs.surface(PUNCH[1:3], 23102, [1, 2])
    for y in range(16):
        for x in range(16):
            swirl = math.sin(x * 0.7 + math.sin(y * 0.5) * 2.0)
            tone = 0 if swirl < -0.7 else 3 if swirl > 0.8 else PUNCH.index(middle(x, y))
            put(img, x, y, PUNCH[tone], 205)
    return img


def eyeball():
    """A floating eyeball: veined white, a green iris and a black pupil in the middle, and a glint."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.2:
                colour = rgb("101010")
            elif d < 4.8:
                colour = rgb("48a83a") if d < 3.0 or y < 6 and d < 4.0 else rgb("2e8a2e")
            else:
                colour = rgb("f4eee6")
            c.px(x, y, colour)
    for x, y in ((0, 3), (1, 4), (2, 4), (3, 5), (15, 11), (14, 11), (13, 10), (12, 10)):
        c.px(x, y, rgb("b02828"))
    c.px(6, 6, rgb("ffffff"))
    return c.img


def ladle():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[1:], 23104)
    return c.img


# ---------------------------------------------------------------- the barmbrack

def crust():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, CRUST, 23111, [1, 2, 3, 2])
    return c.img


def glazed_top():
    """A glossy sugar glaze, catching the light in streaks, with a few fruits showing through."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, GLAZE[:3], 23112, [2, 3, 2])
    for i in range(10):
        c.px(2 + i, 3 + i // 3, GLAZE[3])
    for x, y in ((4, 10), (11, 6), (8, 13), (13, 12)):
        c.px(x, y, RAISIN[0])
    return c.img


def crumb():
    """The cut loaf: a soft tea-soaked crumb studded with raisins, candied peel and a cherry, crusted at its top."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, CRUMB, 23113, [2, 3, 2])
    for x, y in ((2, 3), (8, 2), (12, 4), (5, 9), (13, 9), (1, 11), (9, 13), (6, 14), (12, 14)):
        c.px(x, y, RAISIN[0])  # a raisin, two pixels wide
        c.px(x + 1, y, RAISIN[1])
    for x, y in ((5, 6), (11, 12), (3, 13)):
        c.px(x, y, PEEL)
    c.px(9, 8, CHERRY)
    c.px(10, 8, CHERRY)
    return c.img


def loaf_end():
    """The end of the loaf: crust, browner round its edges."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, CRUST[1:], 23115, [2, 3, 2])
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            c.px(x, y, CRUST[0])
    return c.img


# ---------------------------------------------------------------- giant candy

def sugar(palette, seed):
    """Glossy boiled sugar: soft shading and a highlight streak."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, palette[-3:], seed, [2, 3, 2])
    for i in range(8):
        c.px(3 + i, 2 + i // 2, rgb("fffbea"))
    return c.img


def swirl():
    """A lollipop's face: a three-coloured spiral (orange, white and purple) wound to its middle, with a shine."""
    c = Canvas()
    stripes = [ORANGE[2], CANDY_WHITE[2], PURPLE[2]]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            turn = (math.atan2(dy, dx) / (2 * math.pi) + math.hypot(dx, dy) / 5.0) % 1.0
            c.px(x, y, stripes[int(turn * 3) % 3])
    for x, y in ((3, 4), (4, 3), (4, 4), (5, 3)):
        c.px(x, y, rgb("ffffff"))
    return c.img


def lollipop_edge():
    c = Canvas()
    stripes = [ORANGE[2], CANDY_WHITE[2], PURPLE[2]]
    for y in range(16):
        for x in range(16):
            c.px(x, y, stripes[((x + y) // 3) % 3])
    return c.img


def lollipop_stick():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("f2f0ea") if x % 4 else rgb("d8d4ca"))
    return c.img


def wrapper():
    """A shiny purple wrapper striped orange, with glints."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, PURPLE[1:], 23131, [2, 3, 2])
    for y in range(16):
        for x in (3, 4, 11, 12):
            c.px(x, y, ORANGE[2] if x in (3, 11) else ORANGE[1])
    for x, y in ((7, 3), (8, 4), (6, 10), (14, 7)):
        c.px(x, y, rgb("e6c8ff"))
    return c.img


def twist():
    """The wrapper's twisted, crinkled ends."""
    c = Canvas()
    for x in range(16):
        for y in range(16):
            # Crinkles: slanting folds, lit along one side.
            fold = (x + y // 4) % 3
            c.px(x, y, PURPLE[0] if fold == 0 else PURPLE[3] if fold == 1 else PURPLE[2])
    for y in (4, 12):
        for x in range(0, 16, 3):
            c.px(x, y, ORANGE[2])
    return c.img


def gumdrop():
    """A green gumdrop crusted with sugar crystals."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, GUMDROP, 23141, [2, 3, 2])
    for y in range(1, 16, 3):
        for x in range((y // 3) % 2 * 2 + 1, 16, 4):
            c.px(x, y, rgb("e8ffe8"))  # sugar crystals, set evenly
            c.px(x + 1, y + 1, rgb("b8f0b0"))
    return c.img


# ---------------------------------------------------------------- items

def icon(rows, colours):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colours:
                put(img, x, y, colours[ch])
    return img


SOUL_CAKE = [
    "................",
    "................",
    "................",
    "....CCCCCCCC....",
    "...CTTTTXTTTC...",
    "..CTTRTTXTTRTC..",
    "..CTTTTTXTTTTC..",
    "..CXXXXXXXXXXC..",
    "..CTTTTTXTTTTC..",
    "..CTRTTTXTTRTC..",
    "...CTTTTXTTTC...",
    "...SCCCCCCCCS...",
    "....SSSSSSSS....",
    "................",
    "................",
    "................"]

PUMPKIN_BREAD = [
    "................",
    "................",
    "................",
    "....CCCCCCCC....",
    "...CTTTTTTTTC...",
    "..CTTTTTTTTTTC..",
    "..CBBBBBBBBBBC..",
    "..CBBSBBBBBDBC..",
    "..CBBBBBBBBBBC..",
    "..CBDBBBBSBBBC..",
    "..CBBBBBBBBBBC..",
    "..CBBBSBBBBDBC..",
    "..CCCCCCCCCCCC..",
    "................",
    "................",
    "................"]

BAT_COOKIE = [
    "................",
    "................",
    "................",
    "................",
    "E.....E..E.....E",
    "EB....EBBE....BE",
    "EBB..EBBBBE..BBE",
    "EBBBEBBYBBYBEBBE",
    "EBBBBBBBBBBBBBBE",
    ".EBBBBBBBBBBBBE.",
    "..EBBEBBBBEBBE..",
    "...EE.EBBE.EE...",
    ".......EE.......",
    "................",
    "................",
    "................"]

BOTTLE = [
    "................",
    "......TTTT......",
    "......TTTT......",
    ".......GG.......",
    ".......GG.......",
    "......GLLG......",
    ".....GLLLLG.....",
    "....GLLLLLLG....",
    "...GLLLLLLLLG...",
    "...GLLLLLLLLG...",
    "...GLLLLLLLLG...",
    "...GHLLLLLLLG...",
    "....GHLLLLLG....",
    ".....GGGGGG.....",
    "................",
    "................"]

LOAF = [
    "................",
    "................",
    "................",
    "................",
    "....GGGGGGGG....",
    "...GGHHGGGGGGG..",
    "..GGGGGGGGGGGGI.",
    "..CCCCCCCCCCCIII",
    "..CCCCCCCCCCCIRI",
    "..CCCCCCCCCCCIII",
    "..CCCCCCCCCCCIPI",
    "..CCCCCCCCCCCIII",
    "...CCCCCCCCCCCI.",
    "................",
    "................",
    "................"]

ICING = [
    "................",
    "......DDDD......",
    "....DDDWWDDD....",
    "...DDDWDDWDDD...",
    "..DDWWDDDDWWDD..",
    "..DWDWWWWWWDWD..",
    ".DWDWDDDDDDWDWD.",
    ".DDWDDDDDDDDWDD.",
    ".DWWWDDDDDDWWWD.",
    "..DDWWWWWWWWDD.."]

RING = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......GGGG......",
    ".....GHDDDG.....",
    "....GH....DG....",
    "....GD....DG....",
    "....GD....DG....",
    ".....GDDDDG.....",
    "......GGGG......",
    "................",
    "................",
    "................",
    "................"]


def cupcake():
    """A cupcake in an orange-and-black striped case, iced in chocolate with a white spiderweb draped over it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(10, 15):
        inset = (y - 10) // 2
        for x in range(3 + inset, 13 - inset):
            put(img, x, y, ORANGE[2] if x % 2 else rgb("1a1a1a"))
    for y, row in enumerate(ICING):
        for x, ch in enumerate(row):
            if ch == "W":
                put(img, x, y, rgb("f4f4f0"))
            elif ch == "D":
                put(img, x, y, CHOCOLATE[2 if (x + y) % 3 else 1])
    return img


def soul_cake():
    return icon(SOUL_CAKE, {"C": rgb("b8803a"), "T": rgb("e0b46a"), "X": rgb("a86a2a"), "R": RAISIN[0], "S": rgb("9a6a2e")})


def pumpkin_bread():
    return icon(PUMPKIN_BREAD, {"C": rgb("6a3a16"), "T": rgb("8a5228"), "B": rgb("d8862e"), "S": rgb("7a3a12"), "D": rgb("4a7a2a")})


def bat_cookie():
    return icon(BAT_COOKIE, {"E": CHOCOLATE[3], "B": CHOCOLATE[1], "Y": ORANGE[3]})


def bottle(liquid, top, shine):
    return icon(BOTTLE, {"T": top, "G": GLASS[1], "L": liquid, "H": shine})


def latte():
    img = bottle(rgb("c8864a"), rgb("f6efe2"), rgb("e0a868"))
    for x, y in ((7, 1), (8, 2)):
        put(img, x, y, rgb("8a4a1a"))
    return img


def brew():
    img = bottle(PUNCH[2], rgb("7a5230"), PUNCH[3])
    for x, y in ((6, 8), (9, 10), (7, 11)):
        put(img, x, y, PUNCH[3])
    return img


def loaf_item():
    return icon(LOAF, {"G": GLAZE[1], "H": GLAZE[3], "C": CRUST[2], "I": CRUMB[1], "R": RAISIN[0], "P": PEEL})


def ring_item():
    return icon(RING, {"G": rgb("b88a22"), "D": rgb("dcb03c"), "H": rgb("fff0a0")})


def decor13_textures():
    """(kind, name) -> image for every texture of the thirteenth decorations batch."""
    return {
        ("block", "punch_bowl_glass"): bowl_glass(),
        ("block", "punch_bowl_stand"): stand(),
        ("block", "punch_bowl_punch"): punch(),
        ("block", "punch_bowl_eye"): eyeball(),
        ("block", "punch_bowl_ladle"): ladle(),
        ("block", "barmbrack_crust"): crust(),
        ("block", "barmbrack_top"): glazed_top(),
        ("block", "barmbrack_inside"): crumb(),
        ("block", "barmbrack_end"): loaf_end(),
        ("block", "giant_candy_white"): sugar(CANDY_WHITE, 23121),
        ("block", "giant_candy_orange"): sugar(ORANGE, 23122),
        ("block", "giant_candy_yellow"): sugar(CANDY_YELLOW, 23123),
        ("block", "giant_lollipop_swirl"): swirl(),
        ("block", "giant_lollipop_edge"): lollipop_edge(),
        ("block", "giant_lollipop_stick"): lollipop_stick(),
        ("block", "giant_wrapped_candy"): wrapper(),
        ("block", "giant_wrapped_candy_twist"): twist(),
        ("block", "giant_gumdrop"): gumdrop(),
        ("item", "soul_cake"): soul_cake(),
        ("item", "pumpkin_bread"): pumpkin_bread(),
        ("item", "spiderweb_cupcake"): cupcake(),
        ("item", "bat_wing_cookie"): bat_cookie(),
        ("item", "pumpkin_spice_latte"): latte(),
        ("item", "witchs_brew_punch"): brew(),
        ("item", "barmbrack"): loaf_item(),
        ("item", "barmbrack_ring"): ring_item(),
    }
