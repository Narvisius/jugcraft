"""Original textures for the ninth batch of Halloween decorations, the yard and porch (requires Pillow): the Yard
Inflatables' vinyl, faces, bow, hat, legs and blower, the Porch Witch's pot, brew, robe, skin, face, hat, hair, spoon
and glowing eyes, the Grasping Hands' dirt and skin, the Poseable Skeleton's bone, skull and ribs, the Bone Wind
Chimes' iron, wood, bone, string and little skull, the Weathervanes' iron, compass letters and bat and witch
silhouettes, the Spooky Sign's boards and stake, the Haunted Archway's stone, iron, skull and cobweb, the yard lanterns,
and the Dead Hollow Tree's bark, hollow, face and eyes.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code or from the small pixel-art grids below,
from fixed seeds; no Mojang texture is read, traced or recoloured. Block textures are 16x16 and opaque, except the
weathervane silhouettes and the cobweb (see-through round their shapes) and the witch's eyes (an entity texture).

Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a short palette in small clumps,
never a random colour at every pixel; wood as planks, and straw, bark and hair as streaks.
"""
import math

from crop_textures import Canvas, rgb
import block_style as bs

WHITE_VINYL = [rgb("c8ccd4"), rgb("dadee6"), rgb("eceff4"), rgb("ffffff")]
BLACK_VINYL = [rgb("101014"), rgb("1a1a20"), rgb("26262e"), rgb("44444e")]
ORANGE_VINYL = [rgb("b84e0c"), rgb("d8661a"), rgb("ee8228"), rgb("ffa64a")]
PURPLE = [rgb("2e1440"), rgb("3e1e56"), rgb("52286e"), rgb("6c3a8e")]
IRON = [rgb("1e1e22"), rgb("2a2a30"), rgb("383840"), rgb("5a5a64")]
POT = [rgb("141416"), rgb("1e1e22"), rgb("2a2a2e"), rgb("3c3c44")]
BREW = [rgb("2e8a1e"), rgb("44b02a"), rgb("6ad43e"), rgb("a8f070")]
ROBE = [rgb("1a1020"), rgb("24162c"), rgb("301e3a")]
WITCH_SKIN = [rgb("4e7a32"), rgb("5e8c3c"), rgb("6e9e48")]
HAIR = [rgb("6a6a62"), rgb("86867c"), rgb("a2a296")]
SPOON = [rgb("5a3a1e"), rgb("6e4826"), rgb("82562e")]
DIRT = [rgb("4a3020"), rgb("5a3a26"), rgb("6a4630"), rgb("3a2416")]
ROT = [rgb("5a6650"), rgb("6a7860"), rgb("7a8870"), rgb("4a5442")]
BONE = [rgb("bcb49a"), rgb("d0c8ae"), rgb("e2dcc4"), rgb("f2eedc")]
WOOD = [rgb("3e2c1c"), rgb("4c3624"), rgb("5a402a")]
GREY_WOOD = [rgb("4e4a44"), rgb("625c54"), rgb("767066"), rgb("8a847a")]
STONE = [rgb("4e524e"), rgb("5e625c"), rgb("6e726a"), rgb("80847a")]
MOSS = [rgb("3a5228"), rgb("4a6630"), rgb("5a7a3a")]
BARK = [rgb("2a221c"), rgb("3a2e24"), rgb("4a3c2e"), rgb("5a4a3a")]
GLASS = [rgb("e8a840"), rgb("f8c860"), rgb("fff0a0")]


def put(img, x, y, color, alpha=255):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), color + (alpha,))


def grid(rows, colours, fill, seed=0):
    """A texture from a 16x16 grid of letters; letters not in `colours` take a clumped surface of `fill`
    (tools/block_style.py)."""
    c = Canvas()
    surface = bs.surface(fill, seed)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            c.px(x, y, colours.get(ch) or surface(x, y))
    return c.img


def cutout(rows, colours):
    """A see-through texture from a grid: '.' is clear."""
    c = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                put(c.img, x, y, colours[ch])
    return c.img


# ---------------------------------------------------------------- the yard inflatables

def vinyl(palette, seed, seams=True):
    """Shiny vinyl: smooth, a soft highlight down one side and a sewn seam."""
    c = Canvas()
    crease = bs.wobble(seed, 1)
    for y in range(16):
        for x in range(16):
            tone = 1 + (1 if 3 <= x <= 5 else 0) + (1 if x == 4 and y % 5 != 0 else 0)
            tone = max(0, tone + min(0, int(crease(x, y))))
            c.px(x, y, palette[min(3, tone)])
    if seams:
        for y in range(16):
            c.px(11, y, palette[0])
    return c.img


def blower():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BLACK_VINYL[:3], 19101, [2, 3, 1])
    for y in (4, 6, 8, 10, 12):
        c.line(3, y, 12, y, BLACK_VINYL[0])
    c.px(13, 3, rgb("e02020"))  # the power light
    return c.img


GHOST_FACE = [
    "................",
    "................",
    "................",
    "...KKK....KKK...",
    "..KKKKK..KKKKK..",
    "..KKwKK..KKwKK..",
    "..KKKKK..KKKKK..",
    "...KKK....KKK...",
    "................",
    "................",
    "......KKKK......",
    ".....KKKKKK.....",
    ".....KKKKKK.....",
    "......KKKK......",
    "................",
    "................"]
CAT_FACE = [
    "................",
    "................",
    "................",
    "..YYYY....YYYY..",
    "..YYKY....YKYY..",
    "..YYKY....YKYY..",
    "..YYYY....YYYY..",
    "................",
    ".......PP.......",
    "...WWW.KK.WWW...",
    "......K..K......",
    "................",
    "................",
    "................",
    "................",
    "................"]
PUMPKIN_FACE = [
    "................",
    "................",
    "...YY......YY...",
    "..YYYY....YYYY..",
    ".YYYYYY..YYYYYY.",
    "................",
    ".......YY.......",
    "......YYYY......",
    "................",
    ".Y.YYYYYYYYYY.Y.",
    ".YYYYYYYYYYYYYY.",
    "..YYY.YYYY.YYY..",
    "...Y...YY...Y...",
    "................",
    "................",
    "................"]
SPIDER_FACE = [
    "................",
    "................",
    "...RR......RR...",
    "..RRRR....RRRR..",
    "..RRwR....RwRR..",
    "..RRRR....RRRR..",
    "...RR......RR...",
    ".....RR..RR.....",
    ".....Rw..wR.....",
    "......R..R......",
    "................",
    "....FF....FF....",
    ".....FF..FF.....",
    "......F..F......",
    "................",
    "................"]


def face(rows, colours, palette, seed):
    """A face printed on vinyl."""
    c = Canvas()
    base = vinyl(palette, seed, seams=False)
    for y in range(16):
        for x in range(16):
            ch = rows[y][x]
            c.px(x, y, colours[ch] if ch in colours else base.getpixel((x, y))[:3])
    return c.img


def ribbed(palette, seed):
    """Pumpkin vinyl: printed ribs."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            tone = [0, 1, 2, 3, 2, 1, 1, 2][x % 8]
            c.px(x, y, palette[tone])
    return c.img


def striped_spider():
    c = Canvas()
    base = vinyl(BLACK_VINYL, 19151, seams=False)
    for y in range(16):
        for x in range(16):
            c.px(x, y, PURPLE[3] if y % 6 in (2, 3) else base.getpixel((x, y))[:3])
    return c.img


def bow():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, ORANGE_VINYL[1:], 19131, [2, 3, 1])
    c.rect(6, 5, 9, 10, ORANGE_VINYL[0])
    return c.img


def hat():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, PURPLE[:3], 19141, [2, 3, 1])
    c.rect(0, 13, 15, 15, rgb("1a1a1a"))
    c.rect(6, 13, 9, 15, rgb("c8a030"))  # a buckle
    return c.img


# ---------------------------------------------------------------- the porch witch

def pot():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, POT[:3], 19201, [2, 3, 1])
    c.rect(0, 0, 15, 1, POT[3])
    for x in (2, 8, 13):
        c.px(x, 4, POT[3])
    return c.img


def brew():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BREW[:3], 19211, [1, 2, 1])
    for x, y in ((3, 4), (10, 2), (7, 9), (12, 12), (4, 12)):
        c.rect(x, y, x + 1, y + 1, BREW[3])
    return c.img


def robe():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, ROBE, 19221, [2, 3, 2])
    for x in range(0, 16, 4):
        for y in range(16):
            if (y + x) % 7 == 0:
                c.px(x, y, PURPLE[1])
    return c.img


def witch_skin():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, WITCH_SKIN, 19231, [2, 3, 2])
    c.px(4, 9, WITCH_SKIN[0])
    c.px(11, 5, WITCH_SKIN[0])
    return c.img


WITCH_FACE = [
    "................",
    "................",
    "...HHHH..HHHH...",
    "................",
    "...wwg....gww...",
    "...wKg....gKw...",
    "................",
    "................",
    ".......nn.......",
    "........n.......",
    "..m..........m..",
    "...mmmmmmmmmm...",
    "....mWmmmmWm....",
    ".....mmmmmm.....",
    "................",
    "................"]


def witch_face():
    skin = bs.surface(WITCH_SKIN[1:], 19241, [2, 1], spread=0.6)
    colours = {"H": rgb("2a3a1c"), "w": rgb("e8e8d0"), "g": rgb("8ae040"), "K": rgb("101010"), "n": WITCH_SKIN[0],
               "m": rgb("2a1414"), "W": rgb("e0dcc0")}
    c = Canvas()
    for y, row in enumerate(WITCH_FACE):
        for x, ch in enumerate(row):
            c.px(x, y, colours.get(ch) or skin(x, y))
    return c.img


def witch_hat():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("121214"), rgb("1a1a1e"), rgb("22222a")], 19251, [2, 3, 1])
    c.rect(0, 12, 15, 14, PURPLE[2])
    c.rect(6, 12, 9, 14, rgb("c8a030"))
    return c.img


def hair():
    c = Canvas()
    bs.streaks(HAIR, 19261)(c)
    return c.img


def spoon():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, SPOON[[0, 1, 2, 1][(x + y // 5) % 4]])
    return c.img


def witch_eyes():
    """The glow of her eyes as she cackles and at night."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            put(c.img, x, y, (170, 255, 90) if 5 <= x <= 10 and 5 <= y <= 10 else (90, 220, 40))
    return c.img


# ---------------------------------------------------------------- grasping hands

def hands_dirt():
    c = Canvas()
    bs.dirt([DIRT[3]] + DIRT[:3], 19301)(c)
    for x, y in ((3, 5), (11, 9), (6, 13)):
        c.px(x, y, rgb("2a2a2a"))
    return c.img


def hands_skin():
    """Grey-green rotting skin, dark sores and yellowed nails along the top."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, ROT, 19311, [2, 3, 2, 1])
    for x, y in ((4, 6), (10, 10), (7, 13), (12, 4)):
        c.px(x, y, rgb("3a2a22"))
    for x in range(16):
        c.px(x, 0, rgb("b8b080") if x % 4 != 3 else ROT[3])
    return c.img


# ---------------------------------------------------------------- the poseable skeleton

def bone(seed):
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BONE[:3], seed, [2, 3, 2])
    for x, y in ((3, 4), (12, 9), (7, 13)):
        c.px(x, y, BONE[0])
    return c.img


SKULL = [
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbKKKKbbbbKKKKbb",
    "bKKKKKKbbKKKKKKb",
    "bKKKKKKbbKKKKKKb",
    "bbKKKKbbbbKKKKbb",
    "bbbbbbbKKbbbbbbb",
    "bbbbbbKKKKbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbTbTbTbTbTbbbb",
    "bbbTTTTTTTTTbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb"]


def skull(seed):
    return grid(SKULL, {"K": rgb("1a1612"), "T": rgb("8a826a")}, BONE[1:4], seed)


def ribs():
    """Ribs: bone bars with dark gaps between them, and the breastbone down the middle."""
    c = Canvas()
    bone = bs.surface(BONE[1:], 19411, spread=0.6)
    for y in range(16):
        for x in range(16):
            if 7 <= x <= 8 or y % 3 != 2:
                c.px(x, y, bone(x, y))
            else:
                c.px(x, y, rgb("1e1a16"))
    return c.img


# ---------------------------------------------------------------- bone wind chimes

def chimes_iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 19501, [2, 3, 1])
    return c.img


def chimes_wood():
    c = Canvas()
    bs.planks(WOOD, 19502, vertical=True, joint=False)(c)
    return c.img


def string():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("b8b4a8"), rgb("cac6ba"), rgb("dcd8cc")], 19511)
    return c.img


CHIME_SKULL = [
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbKKKbbbbKKKbbb",
    "bbKKKKKbbKKKKKbb",
    "bbKKKKKbbKKKKKbb",
    "bbbKKKbbbbKKKbbb",
    "bbbbbbbKKbbbbbbb",
    "bbbbbbbKKbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbKbKbKbKbKbbbb",
    "bbbKKKKKKKKKbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbbb"]


# ---------------------------------------------------------------- weathervanes

def vane_iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 19601, [3, 3, 1])
    return c.img


LETTERS = {
    "n": ["X...X", "XX..X", "X.X.X", "X..XX", "X...X"],
    "e": ["XXXXX", "X....", "XXXX.", "X....", "XXXXX"],
    "s": [".XXXX", "X....", ".XXX.", "....X", "XXXX."],
    "w": ["X...X", "X...X", "X.X.X", "XX.XX", "X...X"],
}


def letter(name):
    """A compass point: a raised gilt letter on a black iron plate."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 19611 + ord(name))
    for y, row in enumerate(LETTERS[name]):
        for x, ch in enumerate(row):
            if ch == "X":
                c.rect(3 + x * 2, 3 + y * 2, 4 + x * 2, 4 + y * 2, rgb("d8b048"))
    return c.img


BAT = [
    "................",
    "................",
    "................",
    "......X..X......",
    "......XXXX......",
    "XX....XXXX....XX",
    ".XXX..XXXX..XXX.",
    ".XXXXXXXXXXXXXX.",
    "..XXXXXXXXXXXX..",
    "..XXXXXXXXXXXX..",
    "...XX.XXXX.XX...",
    "...X...XX...X...",
    "................",
    "................",
    "................",
    "................"]
WITCH = [
    "................",
    "........X.......",
    ".......XX.......",
    "......XXX.......",
    ".....XXXXX......",
    "....XXXXXXX.....",
    "......XXX.......",
    "......XXXX......",
    ".....XXXXXX.....",
    "....XXXXXXX.....",
    "XXXXXXXXXXXXX..X",
    "....XXXXXX.XXXXX",
    "......X.X..XXXX.",
    "......X.X...XX..",
    "................",
    "................"]


# ---------------------------------------------------------------- the spooky sign

def sign_wood():
    """Weathered grey boards with dark gaps, cracks and two nails."""
    c = Canvas()
    bs.planks(GREY_WOOD, 19701)(c)
    c.line(10, 1, 13, 3, GREY_WOOD[0])
    for x, y in ((1, 1), (14, 6), (1, 11)):
        c.px(x, y, rgb("2a2a2e"))
    return c.img


def stake():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, GREY_WOOD[[1, 2, 1, 0][x % 4]])
    return c.img


# ---------------------------------------------------------------- the haunted archway and the lanterns

def archway_stone():
    """Mossy stone blocks, as vanilla mossy stone bricks: two courses of two blocks, moss in clumps, thicker low down."""
    c = Canvas()
    bs.bricks(STONE, 19801, rows=2, cols=2)(c)
    bs.moss_over(c, MOSS, 19802, amount=0.25, ground=5)
    return c.img


SKULL_PLAQUE = [
    "IIIIIIIIIIIIIIII",
    "IIIIIbbbbbbIIIII",
    "IIIIbbbbbbbbIIII",
    "IIIbbbbbbbbbbIII",
    "IIIbKKbbbbKKbIII",
    "IIIbKKKbbKKKbIII",
    "IIIbbKbbbbKbbIII",
    "IIIIbbbKKbbbIIII",
    "IIIIIbbbbbbIIIII",
    "IIIIIbTbTbTIIIII",
    "IIIIIbbbbbbIIIII",
    "IIbIIIIIIIIIIbII",
    "IIIbbIIIIIIbbIII",
    "IIIIIbbIIbbIIIII",
    "IIIbbIIIIIIbbIII",
    "IIbIIIIIIIIIIbII"]


def web():
    """A cobweb in a corner: threads radiating from the top left and rings across them."""
    c = Canvas()
    silk = (220, 220, 225)
    for x1, y1 in ((15, 2), (15, 8), (12, 14), (6, 15), (1, 15)):
        c.line(0, 0, x1, y1, silk)
    for r in (4, 8, 12):
        for t in range(0, 91, 5):
            x = round(r * math.cos(math.radians(t)))
            y = round(r * math.sin(math.radians(t)))
            c.px(x, y, silk)
    return c.img


def lantern_iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 19811, [2, 3, 1])
    return c.img


def lantern_glass():
    """Warm candle-lit glass in an iron frame."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c.px(x, y, IRON[1])
            else:
                d = abs(x - 7.5) + abs(y - 9) * 0.7
                c.px(x, y, GLASS[2] if d < 3 else GLASS[1] if d < 6 else GLASS[0])
    return c.img


# ---------------------------------------------------------------- the dead hollow tree

def bark(seed):
    """Grey-brown dead bark in long furrows."""
    c = Canvas()
    bs.streaks(BARK[1:], seed, along=6.0)(c)
    for x in range(0, 16, 4):
        for y in range(16):
            if (y + x) % 7 not in (0, 1):
                c.px(x, y, BARK[0])  # the furrows, broken here and there
    return c.img


def tree_end():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            c.px(x, y, BARK[0] if d > 7 else [rgb("6a5642"), rgb("7a6450")][int(d) % 2])
    return c.img


def hollow():
    """The bark round a dark hollow at the tree's foot."""
    c = Canvas()
    base = bark(19901)
    for y in range(16):
        for x in range(16):
            inside = ((x - 7.5) / 4.5) ** 2 + ((y - 10) / 6) ** 2 < 1
            c.px(x, y, rgb("0a0806") if inside else base.getpixel((x, y))[:3])
    return c.img


TREE_FACE = [
    "................",
    "................",
    "...HHH....HHH...",
    "....HH....HH....",
    "................",
    "...KKKK..KKKK...",
    "..KK..KKKK..KK..",
    "...KKKK..KKKK...",
    "................",
    ".......KK.......",
    "................",
    "....KK....KK....",
    ".....KKKKKK.....",
    "....KKKKKKKK....",
    ".....KKKKKK.....",
    "................"]


def tree_face():
    """A face in the bark: knotted brows, hollow sockets (the eyes glow in them) and a gaping mouth."""
    c = Canvas()
    base = bark(19911)
    for y in range(16):
        for x in range(16):
            ch = TREE_FACE[y][x]
            c.px(x, y, {"K": rgb("0e0a08"), "H": BARK[0]}.get(ch) or base.getpixel((x, y))[:3])
    return c.img


def tree_eye():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("fff070") if 5 <= x <= 10 else rgb("f0b830"))
    return c.img


def decor9_textures():
    """(kind, name) -> image for every texture of the ninth decorations batch."""
    black = {"K": rgb("101010"), "w": rgb("f0f0f0")}
    return {
        ("block", "inflatable_blower"): blower(),
        ("block", "inflatable_ghost_vinyl"): vinyl(WHITE_VINYL, 19111),
        ("block", "inflatable_ghost_face"): face(GHOST_FACE, black, WHITE_VINYL, 19112),
        ("block", "inflatable_cat_vinyl"): vinyl(BLACK_VINYL, 19121),
        ("block", "inflatable_cat_face"): face(CAT_FACE, {"Y": rgb("f0d020"), "K": rgb("101010"), "P": rgb("e07090"),
                                                          "W": rgb("c0c0c8")}, BLACK_VINYL, 19122),
        ("block", "inflatable_bow"): bow(),
        ("block", "inflatable_pumpkin_vinyl"): ribbed(ORANGE_VINYL, 19131),
        ("block", "inflatable_pumpkin_face"): face(PUMPKIN_FACE, {"Y": rgb("ffe46a")}, ORANGE_VINYL, 19132),
        ("block", "inflatable_hat"): hat(),
        ("block", "inflatable_spider_vinyl"): striped_spider(),
        ("block", "inflatable_spider_face"): face(SPIDER_FACE, {"R": rgb("e02020"), "w": rgb("ffffff"), "F": rgb("e8e8e0")},
                                                  BLACK_VINYL, 19152),
        ("block", "inflatable_leg"): vinyl(BLACK_VINYL, 19161, seams=False),
        ("block", "porch_witch_pot"): pot(),
        ("block", "porch_witch_brew"): brew(),
        ("block", "porch_witch_robe"): robe(),
        ("block", "porch_witch_skin"): witch_skin(),
        ("block", "porch_witch_face"): witch_face(),
        ("block", "porch_witch_hat"): witch_hat(),
        ("block", "porch_witch_hair"): hair(),
        ("block", "porch_witch_spoon"): spoon(),
        ("entity", "porch_witch_eyes"): witch_eyes(),
        ("block", "grasping_hands_dirt"): hands_dirt(),
        ("block", "grasping_hands_skin"): hands_skin(),
        ("block", "poseable_skeleton_bone"): bone(19401),
        ("block", "poseable_skeleton_skull"): skull(19402),
        ("block", "poseable_skeleton_ribs"): ribs(),
        ("block", "wind_chimes_iron"): chimes_iron(),
        ("block", "wind_chimes_wood"): chimes_wood(),
        ("block", "wind_chimes_bone"): bone(19521),
        ("block", "wind_chimes_string"): string(),
        ("block", "wind_chimes_skull"): grid(CHIME_SKULL, {"K": rgb("1a1612")}, BONE[1:4], 19531),
        ("block", "weathervane_iron"): vane_iron(),
        ("block", "weathervane_n"): letter("n"),
        ("block", "weathervane_e"): letter("e"),
        ("block", "weathervane_s"): letter("s"),
        ("block", "weathervane_w"): letter("w"),
        ("block", "weathervane_bat"): cutout(BAT, {"X": rgb("141418")}),
        ("block", "weathervane_witch"): cutout(WITCH, {"X": rgb("141418")}),
        ("block", "spooky_sign_wood"): sign_wood(),
        ("block", "spooky_sign_stake"): stake(),
        ("block", "haunted_archway_stone"): archway_stone(),
        ("block", "haunted_archway_iron"): lantern_iron(),
        ("block", "haunted_archway_skull"): grid(SKULL_PLAQUE, {"I": IRON[1], "K": rgb("0e0c0a"), "T": rgb("6a6250")}, BONE[1:4], 19821),
        ("block", "haunted_archway_web"): web(),
        ("block", "yard_lantern_iron"): lantern_iron(),
        ("block", "yard_lantern_glass"): lantern_glass(),
        ("block", "dead_tree_bark"): bark(19921),
        ("block", "dead_tree_end"): tree_end(),
        ("block", "dead_tree_hollow"): hollow(),
        ("block", "dead_tree_face"): tree_face(),
        ("block", "dead_tree_eye"): tree_eye(),
    }
