"""Art for the Arcane Concordance's first content (tools/concordance.py; docs/features/arcane-concordance-first-light.md).

Every pixel is drawn here by code, with no randomness at all, so the same run always writes the same files. Nothing is
traced or copied from another game or mod. The style follows docs/ART_DIRECTION.md:

- the item icons (the Initiate's Wand, the Kindled Lantern unlit and lit, the Research Notes blank and written, and
  the codex) are 16x16 maps in tools/item_icons/, drawn by tools/item_icons.py in the owner's manner
  (docs/ITEM_ICONS.md). The wand lies on the held-item diagonal (grip bottom left, crystal top right) and is early
  tier: dark wood and brass, not dieselpunk. The two lanterns share every shape, so they read as one item; only the
  glass and its light change;
- the spell icons (Kindle, the inscribed spell, and the step 10 invocations: Dawn Aegis, Revelation, Lance of Dawn,
  Flashstep and Lanternward) are 32x32 pixel art: flat fills from four or five shades a material, lit from the top
  left, a dark outline. They share one round violet ground and each shows its role at a glance (a shell, an eye, a
  beam, chevrons, a lantern in a ring);
- the Lampwright's Bench faces are opaque block textures: an oak desk top in planks with a brass inlay (32x32), darker
  oak for its legs and apron (32x32), polished brass with a bevel (16x16) and the lens, violet glass in a brass rim
  (16x16), which is also the Kindled mote's particle.

Called from tools/generate_textures.py: draw_all(save).
"""
import math

from PIL import Image

import clean_metal as cm
import hd_art as hd
import item_icons
from construction_art import Tool

# ------------------------------------------------------------------------------------------------------- palettes

# 64x64 materials (hd_art colour ramps, darkest first).
DARK_WOOD = hd.Material([(30, 18, 12), (48, 30, 19), (68, 43, 27), (90, 59, 37), (114, 78, 50), (140, 100, 66)],
                        0.25, 10)
COPPER = hd.Material([(74, 32, 18), (116, 54, 30), (160, 84, 48), (200, 118, 72), (230, 158, 110), (250, 206, 168)],
                     0.6, 20)
AMETHYST = hd.Material([(84, 58, 128), (118, 90, 166), (154, 126, 204), (188, 164, 232), (218, 202, 248),
                        (246, 240, 255)], 0.7, 30, 0.35)

# Flat colours (painted directly, not shaded) for glass, light and glints.
GLINT_GOLD = [(214, 164, 70), (246, 210, 120), (255, 240, 190)]
WHITE = (255, 255, 250)
# Lantern glass, unlit: dim and cool, a faint violet.
PANE_DIM = [(34, 30, 54), (46, 42, 72), (60, 56, 92), (96, 92, 136)]
# Lantern glass, lit: warm gold-white, brightest round the wick.
PANE_LIT = [(196, 120, 44), (232, 166, 70), (250, 206, 118), (255, 234, 172), (255, 250, 226)]
# Rim light on the cage's inner edges when lit.
CAGE_GLOW = [(236, 168, 86), (255, 214, 140)]

# 32x32 and 16x16 palettes, darkest first.
VIOLET = [(18, 10, 30), (40, 22, 64), (60, 36, 94), (82, 52, 124), (108, 74, 152)]
BRASS = [(76, 50, 18), (120, 84, 32), (166, 124, 50), (206, 166, 76), (236, 206, 122), (250, 236, 180)]
GEM = [(70, 40, 112), (124, 86, 182), (178, 144, 232), (232, 218, 255)]
PAGE = [(150, 130, 96), (206, 190, 150), (232, 220, 186)]
OAK = [(84, 54, 28), (112, 76, 40), (142, 102, 58), (170, 128, 78), (196, 154, 100)]
OAK_DARK = [(38, 22, 12), (56, 35, 19), (76, 50, 28), (98, 66, 38), (122, 86, 52)]
SPELL_BG = [(20, 10, 34), (38, 20, 62), (54, 32, 88), (74, 48, 116), (98, 70, 146)]
HAND = [(118, 98, 160), (168, 150, 210), (206, 192, 240), (236, 228, 255)]
MOTE = [(226, 156, 54), (246, 196, 96), (255, 226, 150), (255, 246, 210), (255, 255, 248)]
LENS = [(46, 30, 82), (72, 50, 120), (104, 78, 160), (140, 116, 196), (188, 172, 232)]


# ------------------------------------------------------------------------------------------------- small helpers

def _flat_rect(c, x0, y0, x1, y1, colour):
    for y in range(int(y0), int(y1) + 1):
        for x in range(int(x0), int(x1) + 1):
            c.put(x, y, colour)


def _flat_disc(c, cx, cy, r, colour):
    for y in range(int(cy - r) - 1, int(cy + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r:
                c.put(x, y, colour)


def _disc(img, cx, cy, r, colour):
    """A flat round fill on a PIL image (pixel centres within r of the centre)."""
    for y in range(img.height):
        for x in range(img.width):
            if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r:
                cm.put(img, x, y, colour)


def _mask(img, rows, colours, ox=0, oy=0):
    """Paint a character mask: each character a key in colours, '.' left alone."""
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colours:
                cm.put(img, ox + x, oy + y, colours[ch])


def _outline(img, colour=(14, 8, 22)):
    """A one-pixel dark outline round every opaque pixel of a see-through image."""
    src = img.copy()
    sp = src.load()
    for y in range(img.height):
        for x in range(img.width):
            if sp[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < img.width and 0 <= ny < img.height and sp[nx, ny][3] == 255:
                    img.putpixel((x, y), tuple(colour) + (255,))
                    break
    return img


# ------------------------------------------------------------------------------------------------- the wand (64)

# ----------------------------------------------------------------------------------------------- the lantern (64)

# ------------------------------------------------------------------------------------------------- the codex (32)

# --------------------------------------------------------------------------------------------- the spell icon (32)

HAND_MASK = [
    "......t...................",
    ".....tht.............tf...",
    ".....hhh............tffj..",
    ".....hhhh..........tfffj..",
    "......hhh........ttfffj...",
    "......hhhh....tthhhhffj...",
    ".......hhhhhhhhhhhhhhj....",
    "........hhhhhhhhhhhhj.....",
    ".........hhhhhhhhhhj......",
    "..........hhhhhhhjj.......",
    "...........hhhhhhj........",
    "...........hhhhhhj........",
    "...........hhhhhhj........",
]


def kindle_icon():
    """The Kindle invocation: an open cupped hand, palm up, with a bright mote of light above it, on a round dark
    violet ground. The hand's rim catches the mote's gold light. Flat bands only, so it reads at 16 pixels."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # The ground: a dark rim, a violet disc, a lighter halo round the mote.
    _disc(img, 16, 16, 15.6, SPELL_BG[0])
    _disc(img, 16, 16, 14.6, SPELL_BG[1])
    _disc(img, 15.4, 15.4, 13.4, SPELL_BG[2])
    _disc(img, 16, 11, 9.0, SPELL_BG[3])
    _disc(img, 16, 11, 6.2, SPELL_BG[4])
    # The hand, palm up, its wrist going down off the ground.
    _mask(img, HAND_MASK, {"h": HAND[2], "j": HAND[1], "t": MOTE[2], "f": HAND[3]}, 3, 17)
    # Finger divisions: short dark grooves between the curled fingertips on the right.
    for x, y in ((22, 21), (23, 20), (21, 22)):
        cm.put(img, x, y, HAND[0])
    # The palm's hollow, in shadow under the light.
    for x in range(12, 21):
        cm.put(img, x, 23, HAND[1])
    # The mote: smooth bands, gold to a white core, with four short rays.
    mx, my = 16, 11
    for r, colour in ((4.6, MOTE[0]), (3.4, MOTE[1]), (2.3, MOTE[2]), (1.3, MOTE[4])):
        _disc(img, mx, my, r, colour)
    for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
        for k in (5, 6):
            cm.put(img, mx + dx * k - (1 if dx < 0 else 0), my + dy * k - (1 if dy < 0 else 0),
                   MOTE[2] if k == 5 else MOTE[1])
    # A small glint on the ground's rim, top left.
    for x, y in ((7, 6), (6, 7), (8, 5)):
        cm.put(img, x, y, SPELL_BG[3])
    return img


def composed_icon():
    """An inscribed spell: three gold nodes joined by a line (delivery, selection, operation) with a short branch off
    the last, the way a composition reads, on the same round violet ground as Kindle. Flat bands only."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _disc(img, 16, 16, 15.6, SPELL_BG[0])
    _disc(img, 16, 16, 14.6, SPELL_BG[1])
    _disc(img, 15.4, 15.4, 13.4, SPELL_BG[2])
    nodes = [(8, 22), (14, 15), (21, 10)]
    branch = (24, 20)
    sizes = (2.4, 2.4, 3.0, 1.8)

    def line(a, b, colour):
        steps = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
        for i in range(steps + 1):
            x = round(a[0] + (b[0] - a[0]) * i / steps)
            y = round(a[1] + (b[1] - a[1]) * i / steps)
            cm.put(img, x, y, colour)
            cm.put(img, x + 1, y, colour)
    for (x, y), r in zip(nodes + [branch], sizes):
        _disc(img, x, y, r + 1.4, SPELL_BG[3])
    line(nodes[0], nodes[1], MOTE[0])
    line(nodes[1], nodes[2], MOTE[0])
    line(nodes[2], branch, MOTE[0])
    for (x, y), r in zip(nodes + [branch], sizes):
        _disc(img, x, y, r, MOTE[1])
        _disc(img, x, y, r - 1.0, MOTE[3])
    for x, y in ((7, 6), (6, 7), (8, 5)):
        cm.put(img, x, y, SPELL_BG[3])
    return img


def _spell_ground(img):
    """The round dark violet ground every Concordance spell icon shares, with its rim glint top left."""
    _disc(img, 16, 16, 15.6, SPELL_BG[0])
    _disc(img, 16, 16, 14.6, SPELL_BG[1])
    _disc(img, 15.4, 15.4, 13.4, SPELL_BG[2])
    for x, y in ((7, 6), (6, 7), (8, 5)):
        cm.put(img, x, y, SPELL_BG[3])


def _line(img, a, b, colour, width=1):
    """A straight line of square pixels from a to b, width pixels thick across x."""
    steps = max(abs(b[0] - a[0]), abs(b[1] - a[1]), 1)
    for i in range(steps + 1):
        x = round(a[0] + (b[0] - a[0]) * i / steps)
        y = round(a[1] + (b[1] - a[1]) * i / steps)
        for w in range(width):
            cm.put(img, x + w, y, colour)


def _ring(img, cx, cy, r, thickness, colour, start=0.0, end=360.0):
    """An arc of a ring (angles in degrees, 0 to the right, clockwise on screen)."""
    for y in range(img.height):
        for x in range(img.width):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r - thickness <= d <= r:
                angle = math.degrees(math.atan2(y + 0.5 - cy, x + 0.5 - cx)) % 360
                if start <= angle <= end:
                    cm.put(img, x, y, colour)


def aegis_icon():
    """Dawn Aegis: a gold shell of light arched over a small violet figure, brighter along its crown. Defense."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    # The figure: a head and rounded shoulders in the hand's lilac, inside the shell, lit from the top left.
    for y in range(18, 26):
        for x in range(32):
            if math.hypot(x + 0.5 - 16, y + 0.5 - 26) <= 7.2:
                cm.put(img, x, y, HAND[2] if x < 16 and y < 22 else HAND[1])
    _disc(img, 16, 15, 3.2, HAND[1])
    _disc(img, 15.4, 14.4, 2.4, HAND[2])
    cm.put(img, 14, 13, HAND[3])
    # The shell: three bands of a dome over it, open at the bottom, its crown brightest.
    _ring(img, 16, 21, 12.5, 2.2, MOTE[0], 180, 360)
    _ring(img, 16, 21, 11.0, 1.2, MOTE[2], 195, 345)
    _ring(img, 16, 21, 11.0, 1.2, MOTE[4], 240, 300)
    # Its two feet resting on the ground line.
    for x in (4, 27):
        cm.put(img, x, 21, MOTE[1])
        cm.put(img, x, 22, MOTE[0])
    return img


def _flat_rect_img(img, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cm.put(img, x, y, colour)


def revelation_icon():
    """Revelation: an open eye of gold light with a violet iris, and short rays all round it. Investigation."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    # Rays: eight short strokes round the eye.
    for angle in range(0, 360, 45):
        rad = math.radians(angle)
        inner = (round(16 + math.cos(rad) * 9.5), round(16 + math.sin(rad) * 7.0))
        outer = (round(16 + math.cos(rad) * 12.0), round(16 + math.sin(rad) * 10.0))
        _line(img, inner, outer, MOTE[1])
    # The eye: an almond of light (two arcs), white within.
    for y in range(32):
        for x in range(32):
            dx, dy = (x + 0.5 - 16) / 9.0, (y + 0.5 - 16) / 5.0
            if dx * dx + dy * dy <= 1.0:
                inner = (x + 0.5 - 16) ** 2 / 64.0 + (y + 0.5 - 16) ** 2 / 16.0 <= 1.0
                cm.put(img, x, y, MOTE[3] if inner else MOTE[0])
    # The iris and pupil, with a catchlight.
    _disc(img, 16, 16, 3.6, LENS[3])
    _disc(img, 16, 16, 2.6, LENS[1])
    _disc(img, 16, 16, 1.4, SPELL_BG[0])
    cm.put(img, 14, 14, MOTE[4])
    return img


def lance_icon():
    """Lance of Dawn: a narrow beam of light crossing the ground from bottom left to top right, a bright spear point
    at its head and sparks where it strikes. Damage."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    # The beam: a wide dim band under a narrow bright core.
    _line(img, (6, 25), (22, 9), MOTE[0], 3)
    _line(img, (7, 25), (23, 9), MOTE[2], 1)
    # The point: a gold diamond at the head.
    _mask(img, ["..a..", ".aba.", "abcba", ".aba.", "..a.."], {"a": MOTE[1], "b": MOTE[3], "c": MOTE[4]}, 21, 5)
    # Sparks where it strikes.
    for x, y in ((27, 6), (26, 11), (20, 4), (28, 9)):
        cm.put(img, x, y, MOTE[2])
    return img


def flashstep_icon():
    """Flashstep: a stride of light, three chevrons swept to the right with fading trails behind them. Movement."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    for offset, colour, trail in ((-7, MOTE[0], SPELL_BG[3]), (0, MOTE[1], MOTE[0]), (7, MOTE[3], MOTE[1])):
        tip = 16 + offset + 3
        _line(img, (tip - 5, 10), (tip, 16), colour, 2)
        _line(img, (tip - 5, 22), (tip, 16), colour, 2)
        for y in (13, 19):
            cm.put(img, tip - 9, y, trail)
            cm.put(img, tip - 10, y, trail)
    return img


def lanternward_icon():
    """Lanternward: a small brass lantern with a gold flame, inside a ring of light that shelters three small allies'
    marks. Support."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _spell_ground(img)
    _ring(img, 16, 16, 12.5, 1.6, MOTE[0])
    _ring(img, 16, 16, 12.5, 0.8, MOTE[2], 200, 340)
    # Three allies' marks on the ring: small lilac discs.
    for angle in (150, 270, 30):
        rad = math.radians(angle)
        _disc(img, 16 + math.cos(rad) * 11.7, 16 + math.sin(rad) * 11.7, 1.8, HAND[2])
    # The lantern: a brass cap, ring and base round a gold glass with a white flame.
    _flat_rect_img(img, 14, 8, 17, 9, BRASS[3])
    cm.put(img, 15, 7, BRASS[4])
    cm.put(img, 16, 7, BRASS[4])
    _flat_rect_img(img, 12, 10, 19, 11, BRASS[2])
    _flat_rect_img(img, 12, 12, 19, 20, MOTE[1])
    _flat_rect_img(img, 13, 13, 18, 19, MOTE[2])
    _flat_rect_img(img, 15, 14, 16, 18, MOTE[4])
    for y in range(12, 21):
        cm.put(img, 12, y, BRASS[2])
        cm.put(img, 19, y, BRASS[1])
    _flat_rect_img(img, 11, 21, 20, 22, BRASS[3])
    return img


# ------------------------------------------------------------------------------------------------- the bench

def bench_top():
    """The desk top: oak planks running across, each lit along its top edge, with a staggered butt joint and regular
    grain lines, inside a thin brass inlay that frames the top a pixel in from the edge."""
    img = Image.new("RGBA", (32, 32), OAK[2] + (255,))
    joints = (11, 23, 5, 17)
    for plank in range(4):
        y0 = 2 + plank * 7
        joint = joints[plank]
        for y in range(y0, y0 + 7):
            for x in range(2, 30):
                if y == y0:
                    colour = OAK[3]
                elif y == y0 + 6 or x == joint:
                    colour = OAK[0] if y == y0 + 6 else OAK[1]
                else:
                    colour = OAK[2]
                cm.put(img, x, y, colour)
        # Grain: two rows of even dashes, offset plank to plank.
        for row, offset in ((2, 0), (4, 5)):
            for x in range(2, 30):
                if (x + offset + plank * 3) % 10 in (0, 1, 2, 3) and x not in (joint, joint + 1):
                    cm.put(img, x, y0 + row, OAK[1])
        cm.put(img, joint + 1, y0 + 1, OAK[4])  # the joint's lit lip
    # The frame round the planks and the brass inlay line set into it.
    cm.bevel(img, 0, 0, 31, 31, OAK[4], OAK[0])
    for x in range(1, 31):
        cm.put(img, x, 1, BRASS[4])
        cm.put(img, x, 30, BRASS[2])
    for y in range(1, 31):
        cm.put(img, 1, y, BRASS[4])
        cm.put(img, 30, y, BRASS[2])
    for x, y in ((1, 1), (30, 1), (1, 30), (30, 30)):  # brass corner squares
        cm.put(img, x, y, BRASS[5])
    return img


def bench_wood():
    """Darker oak for the legs and apron: upright planks eight pixels wide, each lit down its left edge with a dark
    joint at its right, regular grain lines and one staggered butt joint."""
    img = Image.new("RGBA", (32, 32), OAK_DARK[2] + (255,))
    butts = (9, 25, 3, 17)
    for plank in range(4):
        x0 = plank * 8
        for x in range(x0, x0 + 8):
            for y in range(32):
                if x == x0:
                    colour = OAK_DARK[3]
                elif x == x0 + 7:
                    colour = OAK_DARK[0]
                else:
                    colour = OAK_DARK[2]
                cm.put(img, x, y, colour)
        # Grain: two columns of even dashes per plank, offset so neighbours never line up.
        for col, offset in ((2, 0), (5, 4)):
            for y in range(32):
                if (y + offset + plank * 5) % 9 in (0, 1, 2, 3, 4):
                    cm.put(img, x0 + col, y, OAK_DARK[1])
        butt = butts[plank]
        for x in range(x0 + 1, x0 + 7):
            cm.put(img, x, butt, OAK_DARK[0])
            cm.put(img, x, butt + 1, OAK_DARK[4] if x == x0 + 1 else OAK_DARK[3])
    return img


def bench_brass():
    """Polished brass: a soft horizontal sheen (bright near the top, deeper towards the bottom), a one-pixel bevel lit
    on the top and left, and two short bright scratches."""
    img = cm.canvas()
    bands = [4, 4, 3, 4, 5, 4, 3, 3, 3, 3, 2, 3, 2, 2, 2, 1]
    for y in range(16):
        for x in range(16):
            cm.put(img, x, y, BRASS[bands[y]])
    cm.bevel(img, 0, 0, 15, 15, BRASS[5], BRASS[1])
    cm.put(img, 15, 0, BRASS[3])
    cm.put(img, 0, 15, BRASS[2])
    cm.scuffs(img, BRASS[4], [(3, 8, 3), (9, 11, 2)])
    return img


def bench_lens():
    """The lens: a disc of violet glass in a bevelled brass rim on a darker brass mount, lit from the top left, with a
    gold glint. Opaque, like every block texture."""
    img = cm.canvas(BRASS[1])
    cm.bevel(img, 0, 0, 15, 15, BRASS[3], BRASS[0])
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            d = math.hypot(dx, dy)
            if d <= 6.9:
                # The rim: lit on its upper left, shaded on its lower right.
                lightness = -(dx + dy) / max(d, 0.01)
                colour = BRASS[4] if lightness > 0.35 else BRASS[2] if lightness < -0.35 else BRASS[3]
                cm.put(img, x, y, colour)
            if d <= 5.4:
                # The glass: bands of violet, lighter towards the top left, deepest at the bottom right.
                k = (dx + dy) / 2 + 0.6 * d
                colour = LENS[3] if k < -2.5 else LENS[2] if k < 0.8 else LENS[1] if k < 3.2 else LENS[0]
                cm.put(img, x, y, colour)
            if 5.4 < d <= 6.1 and dx + dy > 0:
                cm.put(img, x, y, BRASS[1])  # the rim's inner shadow
    # The glint: a soft gold catch of light upper left, with a white heart, and a faint pale ring opposite.
    cm.patch(img, [(0, 0), (1, 0), (0, 1)], GLINT_GOLD[1], 5, 5)
    cm.put(img, 5, 5, GLINT_GOLD[2])
    cm.put(img, 6, 6, GLINT_GOLD[0])
    cm.patch(img, [(0, 0), (1, -1)], LENS[4], 9, 11)
    return img


# --------------------------------------------------------------------------------------------------------- all

# ------------------------------------------------------------------------------------------- research notes (32)

# The item icons are 16x16 maps in tools/item_icons/ (docs/ITEM_ICONS.md), drawn by tools/item_icons.py.
ITEMS = {name: (lambda name=name: item_icons.draw(name)) for name in (
    "research_notes", "research_notes_written", "initiate_wand", "kindled_lantern", "kindled_lantern_lit",
    "arcane_concordance")}
BLOCKS = {
    "lampwright_bench_top": bench_top,
    "lampwright_bench_wood": bench_wood,
    "lampwright_bench_brass": bench_brass,
    "lampwright_bench_lens": bench_lens,
}
SPELLS = {"kindle": kindle_icon, "composed": composed_icon, "aegis": aegis_icon, "revelation": revelation_icon,
          "lance": lance_icon, "flashstep": flashstep_icon, "lanternward": lanternward_icon}


def textures():
    """Every texture as {(kind, name): image}, in a fixed order (roadmap steps 12 to 25 from concordance_ritual_art,
    concordance_alchemy_art, concordance_ecology_art, concordance_celestial_art, concordance_crimson_art,
    concordance_worker_art, concordance_logistics_art, concordance_artifice_art, concordance_relics_art,
    concordance_equivalence_art, concordance_hexes_art, concordance_conclave_art and concordance_spire_art) and
    Ember's (concordance_ember_art)."""
    import concordance_alchemy_art
    import concordance_artifice_art
    import concordance_celestial_art
    import concordance_conclave_art
    import concordance_crimson_art
    import concordance_ecology_art
    import concordance_ember_art
    import concordance_equivalence_art
    import concordance_hexes_art
    import concordance_logistics_art
    import concordance_relics_art
    import concordance_ritual_art
    import concordance_spire_art
    import concordance_worker_art
    out = {}
    for kind, table in (("item", ITEMS), ("block", BLOCKS), ("spell", SPELLS)):
        for name, draw in table.items():
            out[(kind, name)] = draw()
    out.update(concordance_ritual_art.textures())
    out.update(concordance_alchemy_art.textures())
    out.update(concordance_ecology_art.textures())
    out.update(concordance_celestial_art.textures())
    out.update(concordance_crimson_art.textures())
    out.update(concordance_worker_art.textures())
    out.update(concordance_logistics_art.textures())
    out.update(concordance_artifice_art.textures())
    out.update(concordance_relics_art.textures())
    out.update(concordance_equivalence_art.textures())
    out.update(concordance_hexes_art.textures())
    out.update(concordance_conclave_art.textures())
    out.update(concordance_spire_art.textures())
    out.update(concordance_ember_art.textures())
    return out


def draw_all(save):
    for (kind, name), image in textures().items():
        save(image, kind, name)
