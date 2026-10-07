"""Original textures for the seventh batch of Halloween decorations, the haunted house inside (requires Pillow): the
Haunted Chandelier's iron, wax and flame, the Phantom Pipe Organ's case, panels, pipes, keys, pedals and music, the
Suit of Armor's plate, mail, stand, visor, plume, blade and visor glow, the Dust Sheet, the Spirit Mirror's gilt,
glass and face, the Tattered Curtains' cheesecloth, hem and rod, and the Creepy Doll's dress, lace, porcelain, face
and hair.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code or from the small pixel-art grids below,
from fixed seeds; no Mojang texture is read, traced or recoloured. Surfaces are painted in the manner of the vanilla
blocks with tools/block_style.py: a short palette in small clumps, never a random colour at every pixel, and wood as
planks. Block textures are 16x16 and opaque; the flame, the glow, the sheet's sides, the face, the cheesecloth and the
item icons have see-through pixels.
"""
import math
import random

from crop_textures import Canvas, rgb
import block_style as bs

IRON = [rgb("16141a"), rgb("1f1c22"), rgb("2a262c"), rgb("3a3438")]
RUST = rgb("5a3420")
WAX = [rgb("c4b694"), rgb("d8ccae"), rgb("e6dcc2"), rgb("f2ead6")]
FLAME = {"o": rgb("e8641a"), "y": rgb("ffb02a"), "w": rgb("fff0a8"), "r": rgb("b8340e")}
CASE = [rgb("1e110b"), rgb("2a1810"), rgb("342016"), rgb("40281c")]
PEWTER = [rgb("585e64"), rgb("767c84"), rgb("969ca4"), rgb("bcc2c8"), rgb("dde2e6")]
IVORY = [rgb("d4caa8"), rgb("e2d8ba"), rgb("ece4cc")]
EBONY = [rgb("0e0b0b"), rgb("161212"), rgb("221c1c")]
PAPER = [rgb("c8b890"), rgb("d6c8a2"), rgb("e2d6b4")]
INK = rgb("1a1410")
STEEL = [rgb("6c7278"), rgb("8a9096"), rgb("a8aeb4"), rgb("c8ced2"), rgb("e6eaec")]
OAK = [rgb("3a2a18"), rgb("4a3620"), rgb("5a4228")]
PLUME = [rgb("5a0a0c"), rgb("7a1014"), rgb("9c1a1e"), rgb("bc2a2a")]
LINEN = [rgb("aaa69a"), rgb("bcb8ac"), rgb("ccc8bc"), rgb("dad6ca"), rgb("e6e2d6")]
GILT = [rgb("6a4a12"), rgb("98741e"), rgb("c49c34"), rgb("e8c862")]
GLASS = [rgb("2a3034"), rgb("384046"), rgb("4a545a"), rgb("62707a"), rgb("8a98a2")]
GAUZE = [rgb("8c887c"), rgb("a29e92"), rgb("b6b2a6"), rgb("c8c4b8")]
VELVET = [rgb("2e1426"), rgb("3c1a32"), rgb("4a2240"), rgb("5c2c50")]
LACE = [rgb("d8d2c8"), rgb("ece8e0"), rgb("faf8f2")]
PORCELAIN = [rgb("e2d8d0"), rgb("ece4de"), rgb("f6f0ec")]
HAIR = [rgb("22140c"), rgb("301c10"), rgb("402616"), rgb("54341e")]
BOW = [rgb("4a0a10"), rgb("6a1018"), rgb("8a1a22")]


def put(img, x, y, color, alpha=255):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), color + (alpha,))


# ---------------------------------------------------------------- the haunted chandelier

def chandelier_iron():
    """Black wrought iron with hammer marks and a few rust specks."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[:3], 17101, [2, 3, 2])
    for x, y in ((3, 4), (11, 9), (6, 13)):
        c.px(x, y, RUST)
        c.px(x + 1, y, RUST)
    for x, y in ((9, 2), (2, 10), (13, 14)):
        c.px(x, y, IRON[3])
    return c.img


def chandelier_wax():
    """Old ivory wax, with darker drips running down from the top."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, WAX[1:], 17111, [2, 3, 1])
    rng = random.Random(17112)
    for x in range(0, 16, 3):
        length = rng.randint(3, 9)
        for y in range(length):
            c.px(x + (1 if y > length // 2 else 0), y, WAX[0])
    return c.img


FLAME_GRID = [
    "................",
    ".......r........",
    ".......o........",
    "......ooo.......",
    "......oyo.......",
    ".....ooyoo......",
    ".....oyyyo......",
    "....ooywyoo.....",
    "....oyywyyo.....",
    "....oywwwyo.....",
    "....oywwwyo.....",
    "....ooywyoo.....",
    ".....oyyyo......",
    "......ooo.......",
    "................",
    "................"]


def flame():
    """A candle flame on a see-through ground, filling the card it is drawn on."""
    c = Canvas()
    for y, row in enumerate(FLAME_GRID):
        for x, ch in enumerate(row):
            if ch in FLAME:
                put(c.img, x + 1, y, FLAME[ch])
    return c.img


# ---------------------------------------------------------------- the phantom pipe organ

def organ_case():
    """Dark stained wood: upright boards with a fine grain."""
    c = Canvas()
    bs.planks(CASE, 17201, vertical=True)(c)
    return c.img


def organ_panel():
    """A carved panel: a raised frame round a pointed gothic arch sunk into the wood."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, CASE[1:3], 17211, [3, 2])
    c.rect(0, 0, 15, 0, CASE[3])
    c.rect(0, 0, 0, 15, CASE[3])
    c.rect(0, 15, 15, 15, CASE[0])
    c.rect(15, 0, 15, 15, CASE[0])
    for y in range(3, 14):
        # The arch narrows to a point at the top.
        half = 4 if y >= 7 else max(0, 4 - (7 - y))
        if half:
            c.rect(8 - half, y, 7 + half, y, CASE[0])
            c.px(8 - half, y, CASE[3])
    c.px(7, 2, CASE[3])
    c.px(8, 2, CASE[3])
    c.rect(5, 9, 10, 9, CASE[2])
    return c.img


def organ_pipe():
    """Tarnished pewter, bright down one side of each pipe and dark down the other."""
    c = Canvas()
    tarnish = bs.wobble(17221, 1)
    shades = [3, 4, 2, 1, 2, 3]
    for x in range(16):
        tone = shades[x % len(shades)]
        for y in range(16):
            c.px(x, y, PEWTER[max(0, min(4, tone + min(0, int(tarnish(x, y)))))])
    for y in (4, 11):
        c.rect(0, y, 15, y, PEWTER[1])
    return c.img


def organ_mouth():
    """The dark slot of a pipe's mouth under its lip."""
    c = Canvas()
    c.rect(0, 0, 15, 15, rgb("0c0a0a"))
    c.rect(0, 0, 15, 2, PEWTER[2])
    return c.img


def organ_pedals():
    """The pedalboard from above: pale pedals with dark sharps between."""
    c = Canvas()
    for x in range(16):
        dark = x % 4 == 3
        for y in range(16):
            c.px(x, y, CASE[0] if dark else IVORY[0] if y % 8 else IVORY[1])
    return c.img


def organ_music():
    """A sheet of old music: staves and notes in faded ink."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, PAPER, 17241, [1, 3, 2])
    for staff in (2, 9):
        for line in range(4):
            c.rect(1, staff + line, 14, staff + line, rgb("8a7c64"))
    rng = random.Random(17242)
    for staff in (2, 9):
        for x in range(2, 14, 2):
            y = staff + rng.randint(0, 3)
            c.px(x, y, INK)
            c.px(x + 1, y, INK)
            c.px(x + 1, y - 1, INK)
    return c.img


def ivory():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IVORY, 17251, [1, 3, 2])
    c.rect(0, 15, 15, 15, IVORY[0])
    return c.img


def ebony():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, EBONY, 17261, [2, 3, 1])
    return c.img


# ---------------------------------------------------------------- the suit of armor

def steel():
    """Polished plate: mid grey with a bright sheen across it and rivets at the corners."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            sheen = math.exp(-((x - y + 2) / 3.5) ** 2)
            c.px(x, y, STEEL[min(4, 1 + int(sheen * 3.2))])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        c.px(x, y, STEEL[4])
        c.px(x + 1 if x < 8 else x - 1, y + 1 if y < 8 else y - 1, STEEL[0])
    return c.img


def mail():
    """Chain mail: rows of little rings, offset row to row."""
    c = Canvas()
    c.rect(0, 0, 15, 15, rgb("2a2c30"))
    for row in range(0, 16, 2):
        for col in range(0, 16, 2):
            x = col + (1 if (row // 2) % 2 else 0)
            c.px(x, row, STEEL[2])
            c.px(x, row + 1, STEEL[1])
    return c.img


def armor_wood():
    c = Canvas()
    bs.planks(OAK, 17311, vertical=True)(c)
    return c.img


def visor():
    """A great helm's face plate: a ridge down the middle, the eye slit across, breathing holes below."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, STEEL[2] if (x + y) % 7 else STEEL[3])
    c.rect(7, 0, 8, 15, STEEL[4])
    c.rect(1, 7, 14, 8, rgb("08080a"))
    c.rect(1, 6, 14, 6, STEEL[0])
    for x, y in ((10, 11), (12, 11), (11, 13), (13, 13), (10, 15), (12, 15)):
        c.px(x, y, rgb("08080a"))
    c.rect(0, 0, 15, 0, STEEL[0])
    return c.img


def plume():
    """Red feathers four pixels wide, their barbs slanting in bands two pixels wide, dark between the feathers."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, PLUME[0] if x % 4 == 3 else PLUME[1 + (x + y // 2) // 2 % 2 + (1 if x % 4 == 1 and y % 4 == 0 else 0)])
    return c.img


def blade():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, STEEL[3] if x % 5 else STEEL[4])
    c.rect(0, 0, 0, 15, STEEL[1])
    return c.img


def glow():
    """The red glow in the visor: brightest along the middle."""
    c = Canvas()
    for y in range(16):
        level = 1 - abs(y - 7.5) / 8
        for x in range(16):
            c.px(x, y, (255, int(40 + 120 * level), int(20 + 40 * level)))
    return c.img


# ---------------------------------------------------------------- the dust sheet

def linen_folds(seed):
    """Off-white linen gone grey with dust, with soft folds running down it."""
    c = Canvas()
    rng = random.Random(seed)
    folds = [rng.uniform(0, 16) for _ in range(3)]
    grain = bs.wobble(seed, 0.5)
    for y in range(16):
        for x in range(16):
            shade = 2.6 + sum(math.cos((x - f) * 0.9) * 0.45 for f in folds) + grain(x, y)
            c.px(x, y, LINEN[max(0, min(4, int(shade)))])
    return c


def dust_sheet_top():
    return linen_folds(17401).img


def dust_sheet_side():
    """A side of the sheet: folds falling to a ragged, see-through hem."""
    c = linen_folds(17411)
    rng = random.Random(17412)
    for x in range(16):
        cut = rng.choice((0, 0, 1, 1, 2, 3))
        for y in range(16 - cut, 16):
            put(c.img, x, y, (0, 0, 0), 0)
    return c.img


def dust_sheet_item():
    """A folded white sheet."""
    c = Canvas()
    for y in range(4, 13):
        for x in range(2, 14):
            c.px(x, y, LINEN[4] if (y - 4) % 3 else LINEN[2])
    c.rect(2, 12, 13, 12, LINEN[1])
    c.rect(13, 4, 13, 12, LINEN[1])
    c.rect(2, 4, 13, 4, LINEN[3])
    return c.img


# ---------------------------------------------------------------- the spirit mirror

def gilt():
    """Gilded carving: gold leaf in soft clumps, with a carved bead moulding across it every eight rows."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, GILT, 17491)
    for y in (3, 11):
        for x in range(16):
            c.px(x, y, GILT[3] if x % 2 == 0 else GILT[2])
            c.px(x, y + 1, GILT[0] if x % 2 == 0 else GILT[1])
    return c.img


def mirror_glass():
    """Old silvered glass: dark and grey, a pale streak of reflection across it, the silvering gone at the corners."""
    c = Canvas()
    grain = bs.wobble(17501, 0.4)
    for y in range(16):
        for x in range(16):
            streak = math.exp(-((x + y - 12) / 2.5) ** 2) * 2.5
            corner = min(x, 15 - x) + min(y, 15 - y)
            tone = 1.6 + streak + grain(x, y) - (1.2 if corner < 4 else 0)
            c.px(x, y, GLASS[max(0, min(4, int(tone)))])
    return c.img


def mirror_face():
    """A pale face, half there: an oval of cold white with dark hollow eyes and an open mouth, see-through round it."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) / 6.0) ** 2 + ((y - 7.0) / 7.5) ** 2
            if d <= 1.0:
                alpha = int(255 * min(1.0, (1.0 - d) * 2.2))
                put(c.img, x, y, (226, 234, 242), alpha)
    for x0 in (4, 9):
        for dx in range(3):
            for dy in range(2):
                put(c.img, x0 + dx, 6 + dy, (16, 18, 26), 255)
    for x in range(6, 10):
        put(c.img, x, 11, (24, 26, 34), 255)
    put(c.img, 7, 12, (24, 26, 34), 255)
    put(c.img, 8, 12, (24, 26, 34), 255)
    return c.img


# ---------------------------------------------------------------- tattered curtains

def gauze(hem):
    """Loose-woven cheesecloth gone grey, with holes and tears through it; with `hem`, a ragged bottom edge."""
    c = Canvas()
    rng = random.Random(17601 + (1 if hem else 0))
    grain = bs.wobble(17601 + (1 if hem else 0), 1)
    for y in range(16):
        for x in range(16):
            if x % 3 == 2 and y % 3 == 1:
                continue  # the weave's gaps
            c.px(x, y, GAUZE[max(0, min(3, 1 + (1 if x % 3 == 0 else 0) + int(grain(x, y))))])
    for _ in range(2):
        x, y = rng.randrange(2, 13), rng.randrange(2, 12)
        for dy in range(rng.randint(2, 4)):
            put(c.img, x, y + dy, (0, 0, 0), 0)
            put(c.img, x + 1, y + dy, (0, 0, 0), 0)
    if hem:
        for x in range(16):
            cut = rng.choice((1, 2, 3, 4, 5, 6))
            for y in range(16 - cut, 16):
                put(c.img, x, y, (0, 0, 0), 0)
            if cut > 3 and rng.random() < 0.5:
                put(c.img, x, 16 - cut + 1, GAUZE[0])  # a hanging thread
    return c.img


def curtain_rod():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[1:], 17611, [2, 2, 1])
    return c.img


CURTAIN_ICON = [
    "################",
    ".#.##########.#.",
    "..g.gggggggg.g..",
    "..gggg.ggggggg..",
    "..ggggggg.gggg..",
    "..g.gggggggg.g..",
    "..gggggg.ggggg..",
    "..ggg.gggggggg..",
    "..gggggggg.ggg..",
    "..gg.ggggggg.g..",
    "..ggggg.gggggg..",
    "..g.gggggg.ggg..",
    "..gg.g.gggg.g...",
    "...g.g..gg.g.g..",
    "...g....g...g...",
    "................"]


def curtains_item():
    c = Canvas()
    for y, row in enumerate(CURTAIN_ICON):
        for x, ch in enumerate(row):
            if ch == "#":
                c.px(x, y, IRON[3])
            elif ch == "g":
                c.px(x, y, GAUZE[2 + (x + y) % 2])
    return c.img


# ---------------------------------------------------------------- the creepy doll

def dress():
    """Faded plum velvet, worn pale in patches."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, VELVET, 17701, [2, 3, 2, 1], spread=0.7)
    return c.img


def lace():
    """Yellowed lace: eyelets in offset rows, each a little hole ringed with raised thread."""
    c = Canvas()
    c.rect(0, 0, 15, 15, LACE[1])
    for y in range(1, 16, 4):
        for x in range(1 + (y // 4) % 2 * 2, 16, 4):
            c.px(x, y, LACE[0])
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                c.px((x + dx) % 16, (y + dy) % 16, LACE[2])
    return c.img


def stocking():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, LACE, 17721, [1, 3, 2])
    return c.img


def shoe():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("0c0a0c"), rgb("161216"), rgb("26202a")], 17731, [3, 2, 1])
    return c.img


def porcelain():
    """Pale porcelain with a faint blush."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, PORCELAIN, 17741, [1, 3, 3], spread=0.6)
    return c.img


DOLL_FACE = [
    "pppppppppppppppp",
    "pppppppppppppppp",
    "pppppppppkpppppp",
    "pppppppppkkppppp",
    "ppbbbppppkpbbbpp",
    "pbeeebppkpbeeebp",
    "pbeEebpkppbeEebp",
    "pbeeebpkppbeeebp",
    "ppbbbppkpppbbbpp",
    "pppppppkpppppppp",
    "pcccppkpppppcccp",
    "ppcppkpppppppcpp",
    "pppppprrrrpppppp",
    "pppppprllrpppppp",
    "pppppppppppppppp",
    "pppppppppppppppp"]


def doll_face():
    """Big glassy black eyes with a glint, rosy cheeks, a little red mouth, and a crack from the brow down past one eye."""
    c = Canvas()
    colours = {"b": rgb("3a2a2a"), "e": rgb("0e0a0c"), "E": rgb("e8e8f0"), "c": rgb("e8b0b0"), "r": rgb("9a1c24"), "l": rgb("c8303a"),
               "k": rgb("6a5a50")}
    skin = bs.surface(PORCELAIN[1:], 17751, [1, 2], spread=0.6)
    for y, row in enumerate(DOLL_FACE):
        for x, ch in enumerate(row):
            c.px(x, y, colours.get(ch) or skin(x, y))
    return c.img


def hair():
    """Dark ringlets: curls in rows."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            curl = (x + (y // 3) * 2) % 4
            c.px(x, y, HAIR[(curl + (1 if y % 3 == 0 else 0)) % 4])
    return c.img


def bow():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BOW, 17771, [1, 3, 2])
    return c.img


def decor7_textures():
    """(kind, name) -> image for every texture of the seventh decorations batch."""
    return {
        ("block", "haunted_chandelier_iron"): chandelier_iron(),
        ("block", "haunted_chandelier_wax"): chandelier_wax(),
        ("entity", "haunted_chandelier_flame"): flame(),
        ("block", "phantom_pipe_organ_case"): organ_case(),
        ("block", "phantom_pipe_organ_panel"): organ_panel(),
        ("block", "phantom_pipe_organ_pipe"): organ_pipe(),
        ("block", "phantom_pipe_organ_mouth"): organ_mouth(),
        ("block", "phantom_pipe_organ_pedals"): organ_pedals(),
        ("block", "phantom_pipe_organ_music"): organ_music(),
        ("block", "phantom_pipe_organ_ivory"): ivory(),
        ("block", "phantom_pipe_organ_ebony"): ebony(),
        ("block", "suit_of_armor_steel"): steel(),
        ("block", "suit_of_armor_mail"): mail(),
        ("block", "suit_of_armor_wood"): armor_wood(),
        ("block", "suit_of_armor_visor"): visor(),
        ("block", "suit_of_armor_plume"): plume(),
        ("block", "suit_of_armor_blade"): blade(),
        ("entity", "suit_of_armor_glow"): glow(),
        ("block", "dust_sheet"): dust_sheet_top(),
        ("entity", "dust_sheet_side"): dust_sheet_side(),
        ("item", "dust_sheet"): dust_sheet_item(),
        ("block", "spirit_mirror_gilt"): gilt(),
        ("block", "spirit_mirror_glass"): mirror_glass(),
        ("entity", "spirit_mirror_face"): mirror_face(),
        ("entity", "tattered_curtains"): gauze(False),
        ("entity", "tattered_curtains_hem"): gauze(True),
        ("block", "tattered_curtains_rod"): curtain_rod(),
        ("item", "tattered_curtains"): curtains_item(),
        ("block", "creepy_doll_dress"): dress(),
        ("block", "creepy_doll_lace"): lace(),
        ("block", "creepy_doll_stocking"): stocking(),
        ("block", "creepy_doll_shoe"): shoe(),
        ("block", "creepy_doll_porcelain"): porcelain(),
        ("block", "creepy_doll_face"): doll_face(),
        ("block", "creepy_doll_hair"): hair(),
        ("block", "creepy_doll_bow"): bow(),
    }
