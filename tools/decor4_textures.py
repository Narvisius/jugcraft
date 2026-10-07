"""Original textures for the fourth batch of Halloween decorations, the witch's cottage (requires Pillow): the
Bubbling Cauldron and its brews, the Apothecary Shelf and its jars, the Crystal Ball, the Grimoire Stand and its four
spreads, and the Witch's Broom.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code from fixed seeds; no Mojang texture is
read, traced or recoloured. Surfaces are painted in the manner of the vanilla blocks with tools/block_style.py: a
short palette in small clumps, never a random colour at every pixel, wood as planks and straw in streaks. Block and
item textures are 16x16. The jar glass and the crystal orb are partly
see-through; every model face using them reads the whole texture.
"""
import math
import random

from PIL import Image

from crop_textures import Canvas, rgb, outline
from halloween_textures import shade
import block_style as bs

IRON = [rgb("18181c"), rgb("222228"), rgb("2c2c34"), rgb("3c3c46")]
WATER = [rgb("2a4a8a"), rgb("3460a8"), rgb("4a78c0")]
BREWS = {"green": [rgb("3c8a1c"), rgb("5cb82a"), rgb("8ae04a"), rgb("c8ff8a")],
         "purple": [rgb("5a1e8a"), rgb("7a32b4"), rgb("a050e0"), rgb("d8a8ff")],
         "orange": [rgb("b04a0a"), rgb("e06a14"), rgb("ff9a2a"), rgb("ffd88a")]}
WOOD = [rgb("3e2a1a"), rgb("4e3622"), rgb("5e4430"), rgb("6e5238")]
JARS = {"green": [rgb("2e6a24"), rgb("4a8a34")], "purple": [rgb("4a2a6a"), rgb("6a3e8e")], "red": [rgb("7a1a1a"), rgb("a02a24")],
        "amber": [rgb("a86a14"), rgb("d08a24")]}
BONE = [rgb("b8ae90"), rgb("ccc2a6"), rgb("ddd4ba"), rgb("ece4cc")]
SOCKET = rgb("1a1612")
GOLD = [rgb("7a5a10"), rgb("a8801c"), rgb("d0aa36"), rgb("f0d470")]
MIST = [rgb("3a1e5a"), rgb("5a2e8a"), rgb("8a5ac0"), rgb("c0a0f0")]
PAPER = [rgb("c8b890"), rgb("d8c8a0"), rgb("e6d8b4")]
INK = rgb("3a1e14")
STRAW = [rgb("8a6a2a"), rgb("a8843a"), rgb("c8a24e"), rgb("dcc070")]


def iron():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON, 9951, [2, 3, 2, 1])
    return c.img


def liquid(palette, seed, bubbles):
    """A liquid surface with soft swirls and, for a brew, a few bright bubbles."""
    c = Canvas()
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            wave = (math.sin((x + y * 0.6) * 0.8) + 1) / 2
            c.px(x, y, palette[min(len(palette) - 2, int(wave * (len(palette) - 1)))])
    if bubbles:
        for _ in range(7):
            x, y = rng.randrange(1, 15), rng.randrange(1, 15)
            c.px(x, y, palette[-1])
            c.px(x + 1, y, palette[-2])
    return c.img


# ---------------------------------------------------------------- hex brews (fall addition 21)

HEXES = {"shrinking": [rgb("2a6a3a"), rgb("3e9a54"), rgb("6ccf7a"), rgb("d8ffd0")],
         "giant": [rgb("8a3a0a"), rgb("c8601a"), rgb("f0962a"), rgb("ffe0a0")],
         "flying": [rgb("2e1a5a"), rgb("4e2e8e"), rgb("8a62d0"), rgb("e8d8ff")]}
DRAUGHTS = {"shrinking": "shrinking_draught", "giant": "giants_draught", "flying": "flying_ointment"}
GLASS = [rgb("8aa8b8"), rgb("b8d0dc"), rgb("e6f2f8")]
CORK = [rgb("8a6236"), rgb("a77a45")]


def hex_liquid(palette, seed):
    """A hex brew's surface: a slow spiral round the middle, glittering with motes."""
    c = Canvas()
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            angle = math.atan2(dy, dx)
            radius = math.hypot(dx, dy)
            band = (math.sin(angle * 2 + radius * 0.9) + 1) / 2
            c.px(x, y, palette[min(len(palette) - 2, int(band * (len(palette) - 1)))])
    for _ in range(9):
        c.px(rng.randrange(16), rng.randrange(16), palette[-1])
    return c.img


def draught_item(hex_name, palette):
    """A hex draught: a round-bellied bottle (a squat jar for the ointment), stoppered, the brew glowing inside."""
    c = Canvas()
    if hex_name == "flying":
        c.rect(3, 6, 12, 14, GLASS[0])
        c.rect(4, 7, 11, 13, palette[2])
        c.rect(4, 7, 11, 8, palette[3])
        c.rect(3, 4, 12, 5, CORK[1])
        c.rect(4, 3, 11, 3, CORK[0])
        c.px(5, 9, palette[3])
        c.px(9, 11, palette[3])
    else:
        for y in range(7, 15):
            half = 4 if 8 <= y <= 13 else 3
            for x in range(8 - half, 8 + half):
                c.px(x, y, palette[1] if y > 9 else palette[2])
        c.rect(6, 4, 9, 6, GLASS[1])
        c.rect(6, 2, 9, 3, CORK[1])
        c.px(5, 10, palette[3])
        c.px(6, 9, palette[3])
        c.px(10, 12, GLASS[2])
    outline(c, rgb("1a120c"))
    return c.img


def effect_icon(kind):
    """The Shrunk and Giant effects' icons: a little figure beside a big one, the one that is the drinker brightest."""
    c = Canvas()
    small, big = (rgb("6ccf7a"), rgb("4a4a52")) if kind == "shrunk" else (rgb("4a4a52"), rgb("f0962a"))
    c.rect(2, 10, 4, 14, small)
    c.rect(2, 8, 4, 9, small)
    c.rect(8, 6, 12, 14, big)
    c.rect(9, 2, 11, 5, big)
    outline(c, rgb("1a120c"))
    return c.img


# ---------------------------------------------------------------- the apothecary shelf

def shelf_wood():
    c = Canvas()
    bs.planks(WOOD, 9961)(c)
    return c.img


def bracket():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, IRON[1:], 9962)
    return c.img


def glass():
    """Jar glass: a pale rim and edges, clear (mostly see-through) between them, with a highlight."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), (210, 230, 225, 220) if edge else (200, 225, 220, 48))
    for y in range(3, 9):
        img.putpixel((3, y), (245, 255, 250, 170))
    return img


def jar_contents(colour):
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, JARS[colour], 9970 + list(JARS).index(colour))
    rng = random.Random(9980 + list(JARS).index(colour))
    for _ in range(4):
        c.px(rng.randrange(16), rng.randrange(16), shade(JARS[colour][1], 1.4))
    return c.img


def cork():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("7a5a34"), rgb("94703e"), rgb("a8844e")], 9991)
    return c.img


def skull_side():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BONE, 9992, [1, 2, 3, 2], spread=0.6)
    return c.img


def skull_face():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, BONE[1:], 9993, spread=0.6)
    for x0 in (2, 9):
        c.rect(x0, 4, x0 + 4, 8, SOCKET)
    c.rect(7, 9, 8, 11, SOCKET)
    for x in range(3, 13, 2):
        c.px(x, 13, SOCKET)
    return c.img


def wax():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("d8ccb0"), rgb("e6dcc2"), rgb("f2ead6")], 9994)
    return c.img


def flame():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("ffc23a"), rgb("ffe27a"), rgb("fff6c8")], 9995)
    return c.img


def shelf_item():
    c = Canvas()
    c.rect(0, 13, 15, 14, WOOD[2])
    c.rect(0, 6, 15, 7, WOOD[2])
    c.rect(1, 9, 3, 12, JARS["green"][1])
    c.rect(6, 10, 7, 12, JARS["purple"][1])
    c.rect(10, 9, 12, 12, BONE[2])
    c.px(10, 10, SOCKET)
    c.px(12, 10, SOCKET)
    c.rect(2, 2, 3, 5, JARS["red"][1])
    c.rect(7, 2, 9, 5, JARS["amber"][1])
    c.rect(13, 3, 13, 5, rgb("e6dcc2"))
    c.px(13, 2, rgb("ffc23a"))
    outline(c, rgb("1a120c"))
    return c.img


# ---------------------------------------------------------------- the crystal ball

def gold():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, GOLD, 10001, [1, 2, 3, 1])
    return c.img


def orb():
    """The orb's glass: a bright rim and a highlight, otherwise faintly tinted and see-through."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 7.5
            alpha = int(40 + 120 * max(0.0, d - 0.6) / 0.4) if d <= 1.1 else 160
            img.putpixel((x, y), (190, 170, 230, min(200, alpha)))
    for x, y in ((4, 4), (5, 4), (4, 5), (5, 3)):
        img.putpixel((x, y), (255, 255, 255, 220))
    return img


def mist(bright):
    """Swirling violet mist, brighter at the heart."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            swirl = (math.sin(math.atan2(y - 7.5, x - 7.5) * 3 + math.hypot(x - 7.5, y - 7.5) * 0.9) + 1) / 2
            level = min(3, int(swirl * 3) + (1 if bright else 0))
            c.px(x, y, MIST[level])
    return c.img


# ---------------------------------------------------------------- the grimoire

def stand_wood():
    c = Canvas()
    bs.planks(WOOD, 10011, vertical=True)(c)
    return c.img


def cover():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("3a1018"), rgb("4a1620"), rgb("5a1e2a")], 10012, spread=0.6)
    return c.img


def paper():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, PAPER, 10013, spread=0.6)
    return c.img


def spread(name):
    """An open spread drawn where the model's pages read it: the left page on x 2-7, the right on x 8-14, rows 3-12."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, PAPER, 10020 + ["moons", "bats", "brew", "pumpkin"].index(name), spread=0.6)
    for y in range(3, 13):
        c.px(8, y, shade(PAPER[0], 0.85))
    for y in (4, 6, 8, 10, 12):
        for x in range(2, 7):
            if (x + y) % 3:
                c.px(x, y, INK)
    if name == "moons":
        for i, (x, y) in enumerate(((10, 4), (13, 4), (10, 9), (13, 9))):
            c.rect(x - 1, y - 1, x + 1, y + 1, INK)
            if i % 2:
                c.px(x, y, PAPER[2])
    elif name == "bats":
        for x, y in ((9, 5), (13, 5), (11, 9)):
            c.px(x - 1, y, INK)
            c.px(x, y + 1, INK)
            c.px(x + 1, y, INK)
            c.px(x, y, INK)
    elif name == "brew":
        c.rect(9, 7, 13, 11, INK)
        c.rect(10, 7, 12, 8, rgb("4a8a34"))
        c.px(11, 5, rgb("4a8a34"))
        c.px(12, 4, rgb("4a8a34"))
    else:
        c.rect(9, 6, 13, 10, rgb("c8641a"))
        c.px(11, 5, rgb("3a5a1a"))
        c.px(10, 7, INK)
        c.px(12, 7, INK)
        c.rect(10, 9, 12, 9, INK)
    return c.img


# ---------------------------------------------------------------- the broom

def bristle():
    c = Canvas()
    bs.streaks(STRAW, 10031)(c)
    return c.img


def handle():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, WOOD[1] if (x + y // 3) % 4 == 0 else WOOD[2])
    return c.img


def twine():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("6a5a3a") if (x + y) // 2 % 3 == 0 else rgb("8a7650"))  # the twist, two pixels wide
    return c.img


def broom_item():
    c = Canvas()
    for i in range(10):
        c.px(13 - i, 2 + i, WOOD[2])
        c.px(14 - i, 2 + i, WOOD[1])
    for x, y in ((4, 11), (3, 12), (2, 13), (3, 13), (4, 12), (5, 12), (1, 14), (2, 14), (3, 14), (4, 13), (5, 13), (1, 13), (4, 14)):
        c.px(x, y, STRAW[2])
    c.px(5, 11, rgb("6a5a3a"))
    c.px(6, 11, rgb("6a5a3a"))
    outline(c, rgb("1a120c"))
    return c.img


def decor4_textures():
    """(kind, name) -> image for every texture of the fourth decorations batch."""
    out = {
        ("block", "bubbling_cauldron_iron"): iron(),
        ("block", "bubbling_cauldron_water"): liquid(WATER, 10041, False),
        ("block", "apothecary_shelf_wood"): shelf_wood(),
        ("block", "apothecary_shelf_bracket"): bracket(),
        ("block", "apothecary_shelf_glass"): glass(),
        ("block", "apothecary_shelf_cork"): cork(),
        ("block", "apothecary_shelf_skull"): skull_side(),
        ("block", "apothecary_shelf_skull_face"): skull_face(),
        ("block", "apothecary_shelf_wax"): wax(),
        ("block", "apothecary_shelf_flame"): flame(),
        ("item", "apothecary_shelf"): shelf_item(),
        ("block", "crystal_ball_gold"): gold(),
        ("block", "crystal_ball_orb"): orb(),
        ("block", "crystal_ball_mist"): mist(False),
        ("block", "crystal_ball_mist_bright"): mist(True),
        ("block", "grimoire_stand_wood"): stand_wood(),
        ("block", "grimoire_stand_cover"): cover(),
        ("block", "grimoire_stand_paper"): paper(),
        ("block", "witchs_broom_bristle"): bristle(),
        ("block", "witchs_broom_handle"): handle(),
        ("block", "witchs_broom_twine"): twine(),
        ("item", "witchs_broom"): broom_item(),
    }
    for colour, palette in BREWS.items():
        out[("block", f"bubbling_cauldron_brew_{colour}")] = liquid(palette, 10050 + list(BREWS).index(colour), True)
    for k, (hex_name, palette) in enumerate(HEXES.items()):
        out[("block", f"bubbling_cauldron_hex_{hex_name}")] = hex_liquid(palette, 10070 + k)
        out[("item", DRAUGHTS[hex_name])] = draught_item(hex_name, palette)
    out[("mob_effect", "shrunk")] = effect_icon("shrunk")
    out[("mob_effect", "giant")] = effect_icon("giant")
    for colour in JARS:
        out[("block", f"apothecary_shelf_{colour}")] = jar_contents(colour)
    for name in ("moons", "bats", "brew", "pumpkin"):
        out[("block", f"grimoire_stand_{name}")] = spread(name)
    return out
