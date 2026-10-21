"""The Exchange's casino games: blackjack, craps and roulette tables and four slot machines.

Like the poker tables (tools/exchange_poker.py), each design is built from boxes in model pixels over its whole
footprint: x across, y up, z away from the player who placed it (so the front, where players stand, is z = 0 and faces
south in a north-facing model). The boxes are cut into the design's blocks (part = (k * DEPTH + j) * WIDTH + i for
block i across, j away and k up) as block models facing north; the blockstate turns them for the other facings.

Two kinds of texture are drawn for one design as a whole and cut per block:
- "print": the felt layout seen from above (the up face of any box marked ("print", side texture)),
- "front": the machine's face seen from the front (the south face of any box marked ("front", side texture)).

    write_assets(write, rid, assets, data, lang)   models, blockstates, items, loot, recipes and names
    draw_textures(save_block)                      the textures (save_block(image, name, mcmeta or None))
"""
import math
import pathlib

from PIL import Image

from exchange_poker import box, oval, shoe, _r

MOD = "jugcraft"
FEATURE = "town"


class Design:
    def __init__(self, block, title, size, recipe, build, print_art=None, front_art=None, light=0):
        self.block = block
        self.title = title
        self.width, self.depth, self.height = size
        self.recipe = recipe
        self.build = build
        self.print_art = print_art
        self.front_art = front_art
        self.light = light

    @property
    def parts(self):
        return self.width * self.depth * self.height


# ---------------------------------------------------------------------------------------------------- colours

FELT = (0x2F, 0x5B, 0x3A)
FELT_DARK = (0x27, 0x4E, 0x31)
WHITE = (0xEF, 0xE8, 0xD6)
GOLD = (0xE8, 0xC4, 0x63)
GOLD_DARK = (0xA7, 0x7D, 0x24)
RED = (0xB3, 0x26, 0x22)
BLACK = (0x1C, 0x1A, 0x1A)
ZERO_GREEN = (0x2E, 0x8B, 0x46)
YELLOW = (0xE0, 0xC0, 0x3A)
WALNUT = (0x4A, 0x30, 0x1A)
WALNUT_DARK = (0x30, 0x1E, 0x10)
CHROME = (0xC8, 0xCC, 0xD2)
CHROME_DARK = (0x80, 0x86, 0x8E)
CHROME_LIGHT = (0xEE, 0xF1, 0xF4)


def _canvas(w, h, colour):
    return Image.new("RGBA", (w, h), colour + (255,))


def _px(img, x, y, colour):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((int(x), int(y)), colour + (255,))


def _rect(img, x0, y0, x1, y1, colour):
    """Fills x0 <= x < x1, y0 <= y < y1 (image rows)."""
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            _px(img, x, y, colour)


def _frame(img, x0, y0, x1, y1, colour):
    _rect(img, x0, y0, x1, y0 + 1, colour)
    _rect(img, x0, y1 - 1, x1, y1, colour)
    _rect(img, x0, y0, x0 + 1, y1, colour)
    _rect(img, x1 - 1, y0, x1, y1, colour)


def _felt(w, h):
    img = _canvas(w, h, FELT)
    for y in range(h):
        for x in range(w):
            if (x * 7 + y * 13) % 11 == 0:
                img.putpixel((x, y), (0x34, 0x63, 0x41, 255))
    return img


def _top(design):
    """A print image: x = model x, row = depth * 16 - z (the far side is the top row)."""
    return _felt(design.width * 16, design.depth * 16)


def _row(design, z):
    return design.depth * 16 - z


# ---------------------------------------------------------------------------------------------------- blackjack

def build_blackjack():
    """A half-moon table, 3 wide and 2 deep: the dealer stands at the straight far edge, players round the arc."""
    b = []
    cx, cz = 24, 30
    # Pedestal.
    oval(b, 24, 18, 10, 6, 0, 1.5, "gold")
    oval(b, 24, 18, 5, 3.5, 1.5, 10, "walnut")
    oval(b, 24, 18, 8, 5, 9, 11.5, "walnut")
    for x0, x1 in ((4, 44),):
        box(b, (x0, 9.5, 27), (x1, 11.5, 30), "walnut")
    # The half moon: walnut skirt, gold band, leather bumper round the arc, felt inside.
    _half(b, cx, cz, 24, 28, 11.5, 13, "walnut")
    _half(b, cx, cz, 24, 28, 13, 13.5, "gold")
    _half(b, cx, cz, 24, 28, 13.5, 15, "leather")
    _half(b, cx, cz, 21, 25, 14.9, 15.2, ("print", "felt"), stagger=0.3)
    _arc_ring(b, cx, cz, 22.5, 26.5, 2.4, 15, 16.6, "leather")
    # The dealer's straight edge, chip tray, shoe and discard rack.
    box(b, (0, 11.5, 30), (48, 16, 32), "walnut")
    box(b, (0, 16, 30.5), (48, 16.5, 32), "gold")
    box(b, (16, 15.2, 25.5), (32, 16.2, 29.5), "gold_dark")
    for k, chip in enumerate(("chip_red", "chip_green", "chip_black", "chip_blue", "chip_red", "chip_green")):
        x = 17 + k * 2.5
        box(b, (x, 15.6, 26), (x + 2, 17.2 - (k % 2) * 0.5, 29), chip)
    shoe(b, 38, 26.5, 15.35)
    box(b, (7, 15.25, 25), (11, 16.6, 28), "gold")
    box(b, (7.5, 16.6, 25.5), (10.5, 17.6, 27.5), "card")
    return b


def _half(boxes, cx, cz, rx, rz, y0, y1, tex, step=1.5, stagger=0.0):
    """The near half of an oval (z <= cz), in strips step pixels deep; stagger lowers every other strip's bottom so
    hidden faces of neighbouring strips never share a plane."""
    z = cz - rz
    k = 0
    while z < cz - 1e-6:
        z1 = min(cz, z + step)
        zc = (z + z1) / 2 - cz
        half = round(rx * math.sqrt(max(0.0, 1 - (zc * zc) / (rz * rz))) * 2) / 2
        if half > 0:
            boxes.append(((cx - half, y0 - stagger * (k % 2), z), (cx + half, y1, z1), tex))
        k += 1
        z = z1


def _arc_ring(boxes, cx, cz, rx, rz, size, y0, y1, tex):
    """Short blocks round the near half of an oval: the padded bumper."""
    steps = 22
    for i in range(steps + 1):
        a = math.pi + i / steps * math.pi
        x = cx + math.cos(a) * rx
        z = cz + math.sin(a) * rz
        x0, x1 = max(0.0, x - size / 2), min(48.0, x + size / 2)
        z0, z1 = max(0.0, z - size / 2), min(cz, z + size / 2)
        if x1 - x0 > 0.3 and z1 - z0 > 0.3:
            boxes.append(((round(x0, 2), y0, round(z0, 2)), (round(x1, 2), y1, round(z1, 2)), tex))


def print_blackjack(design):
    img = _top(design)
    cx, cz = 24, 30
    # Gold arc lines: "pays 3 to 2" and insurance, and five white betting circles near the players.
    for rx, rz, colour in ((15, 18, GOLD), (12.5, 15, GOLD_DARK)):
        for i in range(400):
            a = math.pi + i / 400 * math.pi
            _px(img, round(cx + math.cos(a) * rx), round(_row(design, cz + math.sin(a) * rz)), colour)
    for k in range(5):
        a = math.pi + (k + 1) / 6 * math.pi
        x, z = cx + math.cos(a) * 18.5, cz + math.sin(a) * 21.5
        for i in range(40):
            t = i / 40 * math.pi * 2
            _px(img, round(x + math.cos(t) * 1.6), round(_row(design, z + math.sin(t) * 1.6)), WHITE)
    # The dealer's chip tray outline.
    return img


# ---------------------------------------------------------------------------------------------------- craps

def build_craps():
    """A craps tub, 4 wide and 2 deep: rails all round, the layout printed twice, the dice in the middle."""
    b = []
    for lx, lz in ((5, 5), (59, 5), (5, 27), (59, 27)):
        box(b, (lx - 2.5, 0, lz - 2.5), (lx + 2.5, 1, lz + 2.5), "gold")
        box(b, (lx - 2, 1, lz - 2), (lx + 2, 10, lz + 2), "walnut")
    box(b, (2, 10, 2), (62, 12, 30), "walnut")
    box(b, (3, 11.9, 3), (61, 12.2, 29), ("print", "felt"))
    # The walls: walnut outside, the diamond rubber inside, a leather rail on top with a chip groove.
    for (x0, z0, x1, z1) in ((0, 0, 64, 3), (0, 29, 64, 32), (0, 3, 3, 29), (61, 3, 64, 29)):
        box(b, (x0, 10, z0), (x1, 14.5, z1), "walnut")
    for (x0, z0, x1, z1) in ((3, 3, 61, 3.6), (3, 28.4, 61, 29), (3, 3.6, 3.6, 28.4), (60.4, 3.6, 61, 28.4)):
        box(b, (x0, 12.2, z0), (x1, 14.5, z1), "diamonds")
    for (x0, z0, x1, z1) in ((0, 0, 64, 1.5), (0, 30.5, 64, 32), (0, 1.5, 1.5, 30.5), (62.5, 1.5, 64, 30.5)):
        box(b, (x0, 14.5, z0), (x1, 16, z1), "leather")
    for (x0, z0, x1, z1) in ((1.5, 1.5, 62.5, 3), (1.5, 29, 62.5, 30.5)):
        box(b, (x0, 14.5, z0), (x1, 15.2, z1), "walnut")
    for (x0, z0, x1, z1) in ((1.5, 3, 3, 29), (61, 3, 62.5, 29)):
        box(b, (x0, 14.5, z0), (x1, 15.6, z1), "gold")
    # Chips in the near groove, and the dice.
    for k, chip in enumerate(("chip_red", "chip_green", "chip_black", "chip_blue")):
        x = 22 + k * 5
        box(b, (x, 15.2, 1.8), (x + 3.5, 15.9, 2.8), chip)
    box(b, (30, 12.2, 15), (31.6, 13.8, 16.6), "die")
    box(b, (32.4, 12.2, 16.2), (34, 13.8, 17.8), "die")
    # The stickman's stick laid on the far rail.
    box(b, (24, 16, 30.8), (40, 16.6, 31.4), "walnut")
    box(b, (40, 16, 30.6), (41.5, 16.8, 31.6), "walnut")
    return b


def print_craps(design):
    img = _top(design)
    h = img.height

    def r(x0, z0, x1, z1, colour):
        _rect(img, x0, _row(design, z1), x1, _row(design, z0), colour)

    def outline(x0, z0, x1, z1, colour):
        _frame(img, x0, _row(design, z1), x1, _row(design, z0), colour)

    for mirror in (False, True):
        def m(x0, x1):
            return (64 - x1, 64 - x0) if mirror else (x0, x1)
        # Pass line round the outside of each end, don't pass inside it.
        a, c = m(4, 26)
        r(a, 4, c, 5, WHITE)
        a, c = m(4, 5)
        r(a, 4, c, 26, WHITE)
        a, c = m(6, 26)
        r(a, 6, c, 6.6, BLACK)
        # Field, come and the place numbers 4 5 6 8 9 10.
        a, c = m(8, 26)
        outline(a, 8, c, 13, WHITE)
        r(a + 1, 9, c - 1, 12, (0x3F, 0x77, 0x4C))
        for dot in range(6):
            dx = 9 + dot * 3
            a, c = m(dx, dx + 1)
            r(a, 10, c, 11, YELLOW)
        a, c = m(8, 26)
        outline(a, 14, c, 19, WHITE)
        for n in range(6):
            x0 = 6 + n * 3.5
            a, c = m(x0, x0 + 3)
            outline(round(a), 20, round(c), 27, WHITE)
            colour = YELLOW if n in (2, 3) else WHITE
            r(round(a) + 1, 23, round(a) + 2, 24, colour)
    # The proposition bets in the middle: red and white panels.
    r(27, 7, 37, 25, (0x24, 0x47, 0x2D))
    outline(27, 7, 37, 25, WHITE)
    for k in range(4):
        r(28, 9 + k * 4, 36, 11 + k * 4, RED if k % 2 == 0 else WHITE)
    del h
    return img


# ---------------------------------------------------------------------------------------------------- roulette

AMERICAN_WHEEL = ["0", "28", "9", "26", "30", "11", "7", "20", "32", "17", "5", "22", "34", "15", "3", "24", "36",
                  "13", "1", "00", "27", "10", "25", "29", "12", "8", "19", "31", "18", "6", "21", "33", "16", "4",
                  "23", "35", "14", "2"]
REDS = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36}


def pocket_colour(label):
    if label in ("0", "00"):
        return ZERO_GREEN
    return RED if int(label) in REDS else BLACK


def build_roulette():
    """An American roulette table, 3 wide and 2 deep: the wheel at the left, the layout running right."""
    b = []
    for lx, lz in ((4, 5), (44, 5), (4, 27), (44, 27)):
        box(b, (lx - 2.5, 0, lz - 2.5), (lx + 2.5, 1.2, lz + 2.5), "gold")
        box(b, (lx - 1.5, 1.2, lz - 1.5), (lx + 1.5, 11, lz + 1.5), "walnut")
    box(b, (1, 11, 1), (47, 14, 31), "walnut")
    box(b, (0, 13, 0), (48, 14.4, 32), "gold")
    box(b, (0, 14.4, 0), (48, 16, 2.5), "leather")
    box(b, (0, 14.4, 29.5), (48, 16, 32), "leather")
    box(b, (0, 14.4, 2.5), (2, 16, 29.5), "leather")
    box(b, (46, 14.4, 2.5), (48, 16, 29.5), "leather")
    box(b, (2, 14.4, 2.5), (46, 15.4, 29.5), ("print", "felt"))
    # The wheel: a walnut bowl standing proud of the table, its turning head printed with the 38 pockets.
    cx, cz = 11, 16
    oval(b, cx, cz, 9, 9, 15.4, 17.6, "walnut", 1)
    oval(b, cx, cz, 9.4, 9.4, 17.6, 18.1, "gold", 1)
    oval(b, cx, cz, 7.5, 7.5, 16.3, 18.3, ("print", "walnut"), 1)
    oval(b, cx, cz, 2.6, 2.6, 18.3, 19, "gold", 1)
    box(b, (cx - 0.5, 19, cz - 0.5), (cx + 0.5, 21.5, cz + 0.5), "gold")
    box(b, (cx - 3, 20.6, cz - 0.4), (cx + 3, 21.2, cz + 0.4), "gold")
    box(b, (cx - 0.4, 20.6, cz - 3), (cx + 0.4, 21.2, cz + 3), "gold")
    for x, z in ((cx - 3.2, cz), (cx + 3.2, cz), (cx, cz - 3.2), (cx, cz + 3.2)):
        box(b, (x - 0.6, 20.5, z - 0.6), (x + 0.6, 21.7, z + 0.6), "gold")
    box(b, (cx + 5.6, 18.3, cz - 0.4), (cx + 6.4, 19.1, cz + 0.4), "ball")
    # The dolly (the winning-number marker) and a stack of chips.
    box(b, (36, 15.4, 20), (37.2, 17.4, 21.2), "gold")
    for k, chip in enumerate(("chip_red", "chip_black", "chip_green")):
        box(b, (40 + k * 2, 15.4, 4), (41.6 + k * 2, 16.4 + k * 0.6, 5.6), chip)
    return b


def print_roulette(design):
    img = _top(design)

    def r(x0, z0, x1, z1, colour):
        _rect(img, x0, _row(design, z1), x1, _row(design, z0), colour)

    def outline(x0, z0, x1, z1, colour):
        _frame(img, x0, _row(design, z1), x1, _row(design, z0), colour)

    # The wheel head: 38 pockets round the rim, gold frets, a walnut cone.
    cx, cz = 11, 16
    for py in range(img.height):
        for px in range(0, 24):
            x = px + 0.5 - cx
            z = (design.depth * 16 - py - 0.5) - cz
            d = math.hypot(x, z)
            if d > 7.5:
                continue
            if d > 5.2:
                a = (math.atan2(z, x) + math.pi * 2) % (math.pi * 2)
                label = AMERICAN_WHEEL[int(a / (math.pi * 2) * 38) % 38]
                colour = pocket_colour(label)
                if d > 6.8:
                    colour = GOLD_DARK
            elif d > 4.6:
                colour = GOLD
            else:
                colour = (0x6A, 0x46, 0x26) if int(d) % 2 == 0 else WALNUT
            img.putpixel((px, py), colour + (255,))
    # The layout: 0 and 00, then 12 rows of three numbers across, with the outside bets on the near side.
    x0 = 21
    r(x0 - 3, 14, x0, 20, ZERO_GREEN)
    r(x0 - 3, 20, x0, 26, ZERO_GREEN)
    outline(x0 - 3, 14, x0 + 1, 20, WHITE)
    outline(x0 - 3, 19, x0 + 1, 26, WHITE)
    for row in range(12):
        for col in range(3):
            n = row * 3 + col + 1
            xa = x0 + row * 2
            za = 14 + col * 4
            r(xa, za, xa + 2, za + 4, RED if n in REDS else BLACK)
            _frame(img, xa, _row(design, za + 4), xa + 3, _row(design, za) + 1, WHITE)
    end = x0 + 24
    for col in range(3):
        outline(end, 14 + col * 4, end + 1.01, 18 + col * 4, WHITE)
    for dozen in range(3):
        outline(x0 + dozen * 8, 10, x0 + dozen * 8 + 9, 15, WHITE)
    for k, colour in enumerate((None, None, RED, BLACK, None, None)):
        xa = x0 + k * 4
        outline(xa, 6, xa + 5, 11, WHITE)
        if colour:
            r(xa + 1, 7, xa + 4, 10, colour)
    return img


# ---------------------------------------------------------------------------------------------------- slot machines

def _cabinet(b, body, trim, topper_h):
    """A slot cabinet 1 block wide and 2 high: a base, the button deck, the reel window and a topper."""
    f = ("front", body)
    box(b, (1, 0, 3), (15, 1, 14), trim)                     # plinth
    box(b, (1.5, 1, 3.5), (14.5, 11, 14), f)                 # lower cabinet (belly glass printed on the front)
    box(b, (4, 3, 2.5), (12, 5, 3.5), trim)                  # coin tray
    box(b, (1, 11, 1.5), (15, 13, 14), trim)                 # button deck
    box(b, (2, 12.9, 1.8), (14, 13.2, 4), f)                 # buttons, printed
    box(b, (1.5, 13, 4), (14.5, 26, 14), f)                  # the reel cabinet
    box(b, (1, 13, 3.5), (2, 26, 14), trim)                  # side pillars
    box(b, (14, 13, 3.5), (15, 26, 14), trim)
    box(b, (1, 26, 3.5), (15, 27, 14), trim)
    box(b, (1.5, 27, 4.5), (14.5, 27 + topper_h, 13), f)     # topper
    box(b, (1, 27 + topper_h, 4), (15, 28 + topper_h, 13.5), trim)


def glow(b, level, *names):
    """Makes the boxes so far with any of these textures glow (light_emission), like lit glass and bulbs."""
    for i, entry in enumerate(b):
        tex = entry[2]
        name = tex if isinstance(tex, str) else tex[1]
        if name in names and len(entry) == 3:
            b[i] = (entry[0], entry[1], tex, level)


def _screens(b, level=11):
    """Thin lit panels over the reel window and the topper: the machine's glowing glass."""
    box(b, (2.2, 14, 3.7), (13.8, 25.4, 3.88), ("front", "glass"))
    box(b, (2.2, 27.4, 4.2), (13.8, 30.6, 4.38), ("front", "glass"))
    for i in (-2, -1):
        b[i] = (b[i][0], b[i][1], b[i][2], level)


def build_classic():
    b = []
    _cabinet(b, "chrome", "red_lacquer", 4)
    _lever(b, "chrome", "red_lacquer")
    _screens(b)
    return b


def _lever(b, arm, knob):
    box(b, (15, 14, 8), (16, 17, 11), arm)
    box(b, (15.2, 17, 9), (16, 24, 10), arm)
    box(b, (14.8, 24, 8.6), (16, 25.6, 10.4), knob)


def build_clockwork():
    b = []
    _cabinet(b, "walnut", "gold", 4)
    _lever(b, "gold", "gold_dark")
    # Turning gears on both sides and a gilded crest.
    for side in (0.4, 15.6):
        for gy, r in ((7, 3.2), (19, 2.6)):
            _side_gear(b, side, gy, 9, r)
    box(b, (6, 32, 7), (10, 33.5, 10), "gold")
    box(b, (7.25, 33.5, 8), (8.75, 35, 9), "gold")
    _screens(b)
    return b


def _side_gear(b, x, y, z, r):
    """A gear on a side wall, its face turned to the side (the animated gear texture)."""
    t = 0.8
    box(b, (x - t / 2, y - r + 1, z - r + 1), (x + t / 2, y + r - 1, z + r - 1), "gear")
    for dy, dz in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        box(b, (x - t / 2 + 0.05, y + dy * (r - 1) - 0.75, z + dz * (r - 1) - 0.75),
            (x + t / 2 - 0.05, y + dy * (r - 1) + 0.75 + dy * 0.6, z + dz * (r - 1) + 0.75 + dz * 0.6), "gold")


def build_gem():
    b = []
    _cabinet(b, "deepslate", "timber", 4)
    # Timber props like a mine entrance, and a minecart of ore on top.
    box(b, (0, 0, 2.5), (1.5, 30, 4), "timber")
    box(b, (14.5, 0, 2.5), (16, 30, 4), "timber")
    box(b, (0, 30, 2.5), (16, 31.5, 4.5), "timber")
    box(b, (4, 32, 6), (12, 34, 11), "iron")
    box(b, (4.5, 34, 6.5), (11.5, 35, 10.5), "ore")
    _screens(b)
    return b


def build_progressive():
    b = []
    _cabinet(b, "neon", "chrome", 4)
    # Chrome trim and running lights down both sides of the reels.
    box(b, (0.5, 26, 3), (15.5, 26.6, 14), "chrome")
    for x0, x1 in ((0.5, 1.5), (14.5, 15.5)):
        box(b, (x0, 13, 3), (x1, 26, 4), "lights")
    _screens(b)
    glow(b, 15, "lights")
    return b


def _slot_front(base, trim, window, reels, topper, belly, buttons, reel_count=3):
    """The front of a slot machine, 16 wide and 32 high (row 0 is the top)."""
    img = _canvas(16, 32, base)
    # Topper (y 27..31 -> rows 1..5).
    topper(img)
    # Reel window (y 15..25 -> rows 7..17).
    _rect(img, 2, 7, 14, 18, trim)
    _rect(img, 3, 8, 13, 17, window)
    # Each reel shows three symbols; the middle row is the pay line, marked by red arrows on the frame.
    pitch = 10 // reel_count
    for k in range(reel_count):
        xa = 3 + k * pitch + (10 - pitch * reel_count) // 2
        w = pitch - 1 if reel_count < 5 else pitch
        _rect(img, xa, 8, xa + w, 17, (0xF4, 0xEE, 0xDC))
        for s, symbol in enumerate(reels[k]):
            symbol(img, xa, 8 + s * 3, w)
    _px(img, 2, 12, (0xD0, 0x30, 0x30))
    _px(img, 13, 12, (0xD0, 0x30, 0x30))
    # Buttons on the deck front (y 12.9..13.2 sits at rows 18..19).
    buttons(img)
    # The belly glass (y 1..11 -> rows 21..31).
    belly(img)
    return img


def _sym_cherry(img, x, y, w):
    _px(img, x, y + 2, RED)
    _px(img, x + w - 1, y + 2, RED)
    _px(img, x + w // 2, y + 1, (0x3C, 0x8A, 0x2E))
    _px(img, x + w // 2, y, (0x3C, 0x8A, 0x2E))


def _sym_bar(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, BLACK)
    _rect(img, x, y + 1, x + w, y + 2, WHITE)


def _sym_seven(img, x, y, w):
    _rect(img, x, y, x + w, y + 1, RED)
    _px(img, x + w - 1, y + 1, RED)
    _px(img, x + (w - 1) // 2, y + 2, RED)


def _sym_gear(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, GOLD_DARK)
    _px(img, x + w // 2, y + 1, WALNUT_DARK)


def _sym_crown(img, x, y, w):
    _rect(img, x, y + 1, x + w, y + 3, GOLD)
    _px(img, x, y, GOLD)
    _px(img, x + w - 1, y, GOLD)
    _px(img, x + w // 2, y + 2, RED)


def _gem(colour):
    def draw(img, x, y, w):
        _rect(img, x, y, x + w, y + 3, colour)
        _px(img, x, y, tuple(min(255, c + 60) for c in colour))
        _px(img, x + w - 1, y + 2, tuple(max(0, c - 60) for c in colour))
    return draw


def front_classic():
    def topper(img):
        _rect(img, 2, 0, 14, 5, RED)
        for x in (3, 7, 11):
            _rect(img, x, 1, x + 3, 2, GOLD)
            _px(img, x + 2, 2, GOLD)
            _px(img, x + 1, 3, GOLD)

    def buttons(img):
        for k, c in enumerate((RED, YELLOW, (0x3C, 0x8A, 0x2E), (0x40, 0x70, 0xD0))):
            _px(img, 3 + k * 3, 18, c)

    def belly(img):
        _rect(img, 3, 21, 13, 27, (0x7A, 0x1A, 0x18))
        for x in range(3, 13, 2):
            _px(img, x, 22, GOLD)
        _rect(img, 4, 28, 12, 30, CHROME_DARK)

    reels = [[_sym_cherry, _sym_seven, _sym_bar], [_sym_bar, _sym_seven, _sym_cherry], [_sym_cherry, _sym_seven, _sym_bar]]
    return _slot_front(CHROME, RED, BLACK, reels, topper, belly, buttons)


def front_clockwork():
    def topper(img):
        _rect(img, 2, 0, 14, 5, WALNUT_DARK)
        for i in range(24):
            a = i / 24 * math.pi * 2
            _px(img, round(7.5 + math.cos(a) * 2.4), round(2.5 + math.sin(a) * 2.4), GOLD)
        _px(img, 7, 2, GOLD_DARK)
        _px(img, 8, 1, GOLD_DARK)

    def buttons(img):
        for k in range(4):
            _px(img, 3 + k * 3, 18, GOLD)

    def belly(img):
        _rect(img, 3, 21, 13, 28, WALNUT_DARK)
        for i in range(30):
            a = i / 30 * math.pi * 2
            _px(img, round(7.5 + math.cos(a) * 3), round(24.5 + math.sin(a) * 3), GOLD_DARK)
        _rect(img, 4, 29, 12, 30, GOLD)

    reels = [[_sym_gear, _sym_crown, _sym_seven], [_sym_seven, _sym_crown, _sym_gear], [_sym_gear, _sym_crown, _sym_seven]]
    return _slot_front(WALNUT, GOLD, WALNUT_DARK, reels, topper, belly, buttons)


DIAMOND = (0x5C, 0xE0, 0xE6)
EMERALD = (0x2E, 0xC8, 0x5A)
REDSTONE = (0xD0, 0x20, 0x18)
LAPIS = (0x24, 0x4C, 0xC0)
GOLD_ORE = (0xF5, 0xD0, 0x40)


def front_gem():
    def topper(img):
        _rect(img, 2, 0, 14, 5, (0x2A, 0x2A, 0x2E))
        for x, y, c in ((4, 2, DIAMOND), (7, 1, EMERALD), (10, 2, GOLD_ORE), (6, 3, REDSTONE), (9, 3, LAPIS)):
            _px(img, x, y, c)

    def buttons(img):
        for k, c in enumerate((DIAMOND, EMERALD, GOLD_ORE, REDSTONE)):
            _px(img, 3 + k * 3, 18, c)

    def belly(img):
        _rect(img, 3, 21, 13, 28, (0x26, 0x26, 0x2B))
        for x, y, c in ((4, 23, DIAMOND), (8, 22, GOLD_ORE), (11, 24, EMERALD), (6, 26, LAPIS), (10, 27, REDSTONE)):
            _px(img, x, y, c)
        _rect(img, 4, 29, 12, 30, (0x6B, 0x4A, 0x2A))

    gems = [_gem(DIAMOND), _gem(EMERALD), _gem(GOLD_ORE), _gem(REDSTONE), _gem(LAPIS)]
    reels = [[gems[(k + 2) % 5], gems[0], gems[(k + 3) % 5]] for k in range(5)]
    return _slot_front((0x3A, 0x3A, 0x40), (0x6B, 0x4A, 0x2A), BLACK, reels, topper, belly, buttons, reel_count=5)


NEON = (0x5A, 0x24, 0x8C)
NEON_LIGHT = (0xE0, 0x6A, 0xF0)
LED = (0xFF, 0x3A, 0x2A)


def front_progressive():
    def topper(img):
        _rect(img, 1, 0, 15, 5, BLACK)
        _frame(img, 1, 0, 15, 5, NEON_LIGHT)
        for k in range(4):
            x = 2 + k * 3
            _rect(img, x, 1, x + 2, 2, LED)
            _rect(img, x, 3, x + 2, 4, LED)
            _px(img, x, 2, LED)
            _px(img, x + 1, 2, YELLOW)

    def buttons(img):
        for k, c in enumerate((NEON_LIGHT, YELLOW, NEON_LIGHT, YELLOW)):
            _px(img, 3 + k * 3, 18, c)

    def belly(img):
        _rect(img, 3, 21, 13, 28, (0x30, 0x12, 0x50))
        for x in range(3, 13):
            _px(img, x, 21 + (x % 2), NEON_LIGHT)
        _rect(img, 4, 29, 12, 30, CHROME_DARK)

    reels = [[_sym_bar, _sym_seven, _sym_cherry], [_sym_cherry, _sym_seven, _sym_bar], [_sym_bar, _sym_seven, _sym_cherry]]
    return _slot_front(NEON, CHROME, BLACK, reels, topper, belly, buttons)


# ---------------------------------------------------------------------------------------------------- four more machines

BLAZE = (0xF2, 0xA0, 0x20)
MAGMA = (0xE0, 0x5A, 0x14)
NETHER_BRICK = (0x3A, 0x16, 0x1C)
BLACKSTONE = (0x2A, 0x24, 0x2A)
PRISMARINE = (0x5A, 0xA8, 0x98)
PRISMARINE_DARK = (0x2E, 0x5E, 0x58)
SEA_GLOW = (0xD8, 0xF0, 0xE8)
CREEPER = (0x4C, 0xB0, 0x3C)
CREEPER_DARK = (0x2A, 0x6A, 0x22)
TNT_RED = (0xC8, 0x30, 0x24)
OAK = (0x9A, 0x70, 0x40)
YELLOW_LIT = (0xFF, 0xD8, 0x40)


def _disc_xy(b, cx, cy, r, z0, z1, tex, step=1.0):
    """An upright disc facing the player (a wheel on a machine's front), in strips step pixels tall."""
    y = -r
    while y < r - 1e-6:
        yc = y + step / 2
        half = round(math.sqrt(max(0.0, r * r - yc * yc)) * 2) / 2
        if half > 0:
            box(b, (cx - half, cy + y, z0), (cx + half, cy + y + step, z1), tex)
        y += step


def _disc_yz(b, x0, x1, cy, cz, r, tex, step=1.0):
    """An upright disc facing sideways (a ship's wheel on a machine's side)."""
    y = -r
    while y < r - 1e-6:
        yc = y + step / 2
        half = round(math.sqrt(max(0.0, r * r - yc * yc)) * 2) / 2
        if half > 0:
            box(b, (x0, cy + y, cz - half), (x1, cy + y + step, cz + half), tex)
        y += step


def build_nether():
    b = []
    _cabinet(b, "blackstone", "nether_brick", 4)
    # Magma seams down the sides and a prize wheel standing on the topper.
    for x0, x1 in ((0.6, 1.2), (14.8, 15.4)):
        box(b, (x0, 2, 6), (x1, 25, 7), "magma")
        box(b, (x0, 2, 10), (x1, 25, 11), "magma")
    _disc_xy(b, 8, 36, 6.5, 6.5, 8, "nether_brick", 1)
    _disc_xy(b, 8, 36, 5.6, 6, 6.6, "prize_wheel", 1)
    box(b, (7.3, 35.3, 5.2), (8.7, 36.7, 6.05), "gold")
    box(b, (7.4, 41.6, 5.6), (8.6, 43.4, 6.6), "gold")
    box(b, (6.5, 31, 7), (9.5, 32, 8), "nether_brick")
    _screens(b)
    glow(b, 12, "magma", "prize_wheel")
    return b


def build_treasure():
    b = []
    _cabinet(b, "prismarine", "prismarine_dark", 4)
    # Kelp up the front corners, a sea lantern crown, and a ship's wheel on the right as the lever.
    for x0 in (0.4, 14.6):
        box(b, (x0, 1, 2.6), (x0 + 1, 27, 3.4), "kelp")
    box(b, (5.5, 32, 6.5), (10.5, 35, 10.5), "sea_lantern")
    box(b, (6.5, 35, 7.5), (9.5, 36, 9.5), "prismarine_dark")
    _disc_yz(b, 15, 15.8, 19, 9, 4.2, "timber", 1)
    _disc_yz(b, 15.05, 15.75, 19, 9, 2.8, "prismarine_dark", 1)
    box(b, (15.1, 18.4, 8.4), (16.4, 19.6, 9.6), "gold")
    for dy, dz in ((5.4, 0), (-5.4, 0), (0, 5.4), (0, -5.4)):
        box(b, (15.2, 19 + min(0, dy) - 0.4 + (0 if dy == 0 else 0), 9 + min(0, dz) - 0.4),
            (15.6, 19 + max(0, dy) + 0.4, 9 + max(0, dz) + 0.4), "timber")
    _screens(b)
    glow(b, 15, "sea_lantern")
    return b


def build_creeper():
    b = []
    _cabinet(b, "creeper", "black_lacquer", 4)
    # A block of TNT on top with its fuse.
    box(b, (4, 32, 5), (12, 38, 12), "tnt")
    box(b, (7.6, 38, 8.1), (8.4, 40, 8.9), "black_lacquer")
    box(b, (7.4, 40, 7.9), (8.6, 40.8, 9.1), "sparks")
    _screens(b)
    glow(b, 15, "sparks")
    return b


def build_pub():
    b = []
    _cabinet(b, "oak", "chrome", 4)
    # Three chunky HOLD buttons standing proud of the deck, and a short chrome lever.
    for k in range(3):
        x = 3.2 + k * 3.6
        box(b, (x, 13.2, 1.6), (x + 2.6, 14.2, 3.4), "hold_button")
    box(b, (12.6, 13.2, 1.6), (14.2, 14.0, 3.4), "red_lacquer")
    _lever(b, "chrome", "red_lacquer")
    _screens(b)
    glow(b, 12, "hold_button")
    return b


def _sym_blaze(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, BLAZE)
    _px(img, x, y + 1, (0x50, 0x30, 0x10))
    _px(img, x + w - 1, y + 1, (0x50, 0x30, 0x10))


def _sym_ghast(img, x, y, w):
    _rect(img, x, y, x + w, y + 2, (0xF0, 0xF0, 0xF0))
    _px(img, x, y + 2, (0xF0, 0xF0, 0xF0))
    _px(img, x + w - 1, y + 2, (0xF0, 0xF0, 0xF0))


def _sym_netherite(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, (0x44, 0x3A, 0x3A))
    _px(img, x, y, (0x8A, 0x7A, 0x7A))


def _sym_fish(img, x, y, w):
    _rect(img, x, y + 1, x + w, y + 2, (0xE8, 0x8A, 0x3A))
    _px(img, x, y, (0xE8, 0x8A, 0x3A))
    _px(img, x, y + 2, (0xE8, 0x8A, 0x3A))


def _sym_chest(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, (0xA0, 0x6A, 0x2A))
    _rect(img, x, y + 1, x + w, y + 2, GOLD)


def _sym_trident(img, x, y, w):
    _rect(img, x + w // 2, y, x + w // 2 + 1, y + 3, (0x5A, 0xC8, 0xC0))
    _px(img, x, y, (0x5A, 0xC8, 0xC0))
    _px(img, x + w - 1, y, (0x5A, 0xC8, 0xC0))


def _sym_creeper(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, CREEPER)
    _px(img, x, y, BLACK)
    _px(img, x + w - 1, y, BLACK)
    _px(img, x + w // 2, y + 2, BLACK)


def _sym_tnt(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, TNT_RED)
    _rect(img, x, y + 1, x + w, y + 2, WHITE)


def _sym_bell(img, x, y, w):
    _rect(img, x, y + 1, x + w, y + 3, YELLOW_LIT)
    _px(img, x + w // 2, y, YELLOW_LIT)


def front_nether():
    def topper(img):
        _rect(img, 2, 0, 14, 5, NETHER_BRICK)
        for x in range(3, 13, 2):
            _px(img, x, 1, MAGMA)
            _px(img, x + 1, 3, BLAZE)

    def buttons(img):
        for k, c in enumerate((MAGMA, BLAZE, MAGMA, BLAZE)):
            _px(img, 3 + k * 3, 18, c)

    def belly(img):
        _rect(img, 3, 21, 13, 28, (0x1A, 0x10, 0x12))
        for x, y in ((4, 22), (7, 24), (10, 23), (5, 26), (11, 26), (8, 27)):
            _px(img, x, y, MAGMA)
        _rect(img, 4, 29, 12, 30, NETHER_BRICK)

    reels = [[_sym_ghast, _sym_blaze, _sym_netherite], [_sym_netherite, _sym_blaze, _sym_ghast],
             [_sym_ghast, _sym_blaze, _sym_netherite]]
    return _slot_front(BLACKSTONE, MAGMA, BLACK, reels, topper, belly, buttons)


OCEAN = (0x0E, 0x2A, 0x5A)
OCEAN_DARK = (0x06, 0x16, 0x34)
OCEAN_GLOW = (0x3A, 0xB8, 0xFF)
SHARK = (0x7A, 0x86, 0x94)
SHARK_LIGHT = (0xC8, 0xD0, 0xD8)
DRAGON_RED = (0xA8, 0x14, 0x14)
DRAGON_DARK = (0x5A, 0x08, 0x08)
JADE = (0x2E, 0xA8, 0x6A)


def build_shark():
    """Shark Frenzy: a deep-ocean cabinet with a glowing water line along the top and a shark's fin cutting through it."""
    b = []
    _cabinet(b, "ocean", "ocean_dark", 4)
    box(b, (1.5, 32, 5), (14.5, 33, 12.5), "water")                    # the water line on top
    # The fin: a tall curved triangle rising out of the water, its tip swept back.
    for k, (z0, z1) in enumerate(((5, 12), (5.6, 11.8), (6.3, 11.6), (7, 11.4), (7.8, 11.3), (8.6, 11.3), (9.4, 11.4),
                                  (10.2, 11.6))):
        y = 33 + k * 1.2
        box(b, (7.2, y, z0), (8.8, y + 1.2, z1), "shark")
    _lever(b, "chrome", "ocean")
    _screens(b)
    glow(b, 12, "water")
    return b


def build_dragon():
    """Golden Dragon: red lacquer and gold, a little pagoda roof with upturned eaves on top and a red lantern hanging
    at each side."""
    b = []
    _cabinet(b, "dragon", "dragon_roof", 4)
    box(b, (0.5, 32, 4), (15.5, 33, 13.5), "gold")                      # the roof's beam
    box(b, (2, 33, 5), (14, 34.5, 12.5), "dragon_roof")                 # the roof, stepping in
    box(b, (4, 34.5, 6), (12, 35.8, 11.5), "dragon_roof")
    box(b, (6.5, 35.8, 7.5), (9.5, 37, 10), "gold")                     # the finial
    box(b, (7.5, 37, 8.3), (8.5, 38.2, 9.2), "pearl")
    for x0 in (-0.5, 15.5):                                             # upturned eaves at the corners
        box(b, (x0, 33, 4), (x0 + 1, 34.2, 5), "gold")
        box(b, (x0, 33, 12.5), (x0 + 1, 34.2, 13.5), "gold")
    for x in (-1.6, 15.2):                                              # round lanterns, hanging from gold cords
        box(b, (x + 1, 27.5, 4.9), (x + 1.4, 32, 5.3), "gold")
        box(b, (x + 0.4, 27, 4.3), (x + 2, 27.5, 5.9), "gold")
        box(b, (x, 24, 3.9), (x + 2.4, 27, 6.3), "lantern")
        box(b, (x + 0.4, 23.4, 4.3), (x + 2, 28, 5.9), "lantern")
        box(b, (x + 0.6, 22.6, 4.5), (x + 1.8, 23.4, 5.7), "gold")
    _screens(b)
    glow(b, 13, "lantern", "pearl")
    return b


def _sym_octopus(img, x, y, w):
    _rect(img, x, y, x + w, y + 2, (0xC0, 0x4A, 0x8A))
    _px(img, x, y + 2, (0xC0, 0x4A, 0x8A))
    _px(img, x + w - 1, y + 2, (0xC0, 0x4A, 0x8A))


def _sym_diver(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, (0xE8, 0xC4, 0x63))
    _px(img, x + w // 2, y + 1, OCEAN_GLOW)


def _sym_shark(img, x, y, w):
    _rect(img, x, y + 1, x + w, y + 3, SHARK)
    _px(img, x + w // 2, y, SHARK)
    _rect(img, x, y + 2, x + w, y + 3, SHARK_LIGHT)


def _sym_coin(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, GOLD)
    _px(img, x + w // 2, y + 1, DRAGON_DARK)


def _sym_lantern(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, DRAGON_RED)
    _rect(img, x, y + 1, x + w, y + 2, GOLD)


def _sym_koi(img, x, y, w):
    _rect(img, x, y + 1, x + w, y + 2, (0xFF, 0x8A, 0x2A))
    _px(img, x, y, (0xFF, 0xFF, 0xFF))
    _px(img, x + w - 1, y + 2, (0xFF, 0xFF, 0xFF))


def _sym_dragon(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, JADE)
    _px(img, x, y, GOLD)
    _px(img, x + w - 1, y + 2, DRAGON_RED)


def _sym_pearl(img, x, y, w):
    _rect(img, x, y, x + w, y + 3, DRAGON_DARK)
    _px(img, x + w // 2, y + 1, (0xFF, 0xF4, 0xD0))
    _px(img, x + w // 2 - 1 if w > 2 else x, y + 1, (0xE8, 0xC4, 0x63))


def front_shark():
    def topper(img):
        _rect(img, 2, 0, 14, 5, OCEAN_DARK)
        for x in range(3, 13):
            _px(img, x, 2 + (x % 2), OCEAN_GLOW)
        _rect(img, 7, 1, 9, 2, SHARK)

    def buttons(img):
        for k in range(4):
            _px(img, 3 + k * 3, 18, OCEAN_GLOW)

    def belly(img):
        # Shark jaws: two rows of white teeth in a dark mouth.
        _rect(img, 3, 21, 13, 30, (0x2A, 0x0A, 0x14))
        for x in range(3, 13):
            _px(img, x, 21 + (x % 2), WHITE)
            _px(img, x, 29 - (x % 2), WHITE)
        _rect(img, 3, 25, 13, 26, (0x8A, 0x1A, 0x2A))

    pool = [_sym_fish, _sym_octopus, _sym_diver, _sym_chest, _sym_shark]
    reels = [[pool[(k + 1) % 5], _sym_shark if k in (1, 3) else pool[(k + 3) % 5], pool[(k + 2) % 5]] for k in range(5)]
    return _slot_front(OCEAN, (0x8A, 0x92, 0x9C), OCEAN_DARK, reels, topper, belly, buttons, reel_count=5)


def front_dragon():
    def topper(img):
        _rect(img, 2, 0, 14, 5, DRAGON_DARK)
        _rect(img, 3, 1, 13, 4, GOLD)
        _rect(img, 4, 2, 12, 3, DRAGON_RED)
        _px(img, 8, 2, (0xFF, 0xF4, 0xD0))

    def buttons(img):
        for k in range(4):
            _px(img, 3 + k * 3, 18, GOLD)

    def belly(img):
        # Gold dragon scales on red.
        _rect(img, 3, 21, 13, 30, DRAGON_RED)
        for y in range(21, 30, 2):
            for x in range(3 + (y // 2) % 2, 13, 2):
                _px(img, x, y, GOLD)
        _rect(img, 6, 24, 10, 27, JADE)
        _px(img, 8, 25, (0xFF, 0xF4, 0xD0))

    pool = [_sym_coin, _sym_lantern, _sym_koi, _gem(GOLD), _sym_dragon]
    reels = [[pool[(k + 2) % 5], _sym_dragon if k == 2 else _sym_pearl if k in (0, 4) else pool[(k + 1) % 5],
              pool[(k + 3) % 5]] for k in range(5)]
    return _slot_front(DRAGON_RED, GOLD, (0x1A, 0x06, 0x06), reels, topper, belly, buttons, reel_count=5)


def front_treasure():
    def topper(img):
        _rect(img, 2, 0, 14, 5, PRISMARINE_DARK)
        _rect(img, 6, 1, 10, 4, (0xA0, 0x6A, 0x2A))
        _rect(img, 6, 2, 10, 3, GOLD)

    def buttons(img):
        for k in range(4):
            _px(img, 3 + k * 3, 18, SEA_GLOW)

    def belly(img):
        _rect(img, 3, 21, 13, 28, (0x1E, 0x3E, 0x4A))
        for x, y in ((5, 23), (9, 25), (11, 22), (6, 27)):
            _px(img, x, y, (0xE8, 0x8A, 0x3A))
        _rect(img, 4, 26, 6, 28, (0x3A, 0x7A, 0x2A))
        _rect(img, 4, 29, 12, 30, PRISMARINE)

    pool = [_sym_fish, _sym_chest, _sym_trident, _sym_fish, _sym_trident]
    reels = [[pool[(k + 1) % 5], _sym_chest, pool[(k + 2) % 5]] for k in range(5)]
    return _slot_front(PRISMARINE, PRISMARINE_DARK, (0x0C, 0x1C, 0x22), reels, topper, belly, buttons, reel_count=5)


def front_creeper():
    def topper(img):
        _rect(img, 2, 0, 14, 5, BLACK)
        _rect(img, 3, 1, 13, 4, TNT_RED)
        _rect(img, 3, 2, 13, 3, WHITE)

    def buttons(img):
        for k in range(4):
            _px(img, 3 + k * 3, 18, CREEPER)

    def belly(img):
        # A creeper's face.
        _rect(img, 3, 21, 13, 30, CREEPER)
        _rect(img, 4, 22, 7, 25, BLACK)
        _rect(img, 9, 22, 12, 25, BLACK)
        _rect(img, 7, 25, 9, 28, BLACK)
        _rect(img, 5, 26, 7, 29, BLACK)
        _rect(img, 9, 26, 11, 29, BLACK)

    pool = [_sym_creeper, _sym_tnt, _gem((0x2E, 0xC8, 0x5A)), _sym_tnt, _gem((0x5C, 0xE0, 0xE6))]
    reels = [[pool[(k + 2) % 5], _sym_creeper if k in (1, 2, 3) else pool[k % 5], pool[(k + 3) % 5]] for k in range(5)]
    return _slot_front(CREEPER_DARK, BLACK, BLACK, reels, topper, belly, buttons, reel_count=5)


def front_pub():
    def topper(img):
        _rect(img, 2, 0, 14, 5, (0x1A, 0x10, 0x08))
        for k in range(5):
            _px(img, 3 + k * 2 + (k > 2), 2, YELLOW_LIT if k % 2 == 0 else RED)
        _rect(img, 3, 4, 13, 5, GOLD)

    def buttons(img):
        for k in range(3):
            _rect(img, 3 + k * 3, 18, 5 + k * 3, 19, YELLOW_LIT)
        _px(img, 13, 18, RED)

    def belly(img):
        _rect(img, 3, 21, 13, 28, (0x2A, 0x1A, 0x0E))
        for k, c in enumerate((RED, YELLOW_LIT, (0x3C, 0x8A, 0x2E), YELLOW_LIT, RED)):
            _px(img, 4 + k * 2, 23 + (k % 2), c)
        _rect(img, 4, 29, 12, 30, CHROME_DARK)

    reels = [[_sym_cherry, _sym_bell, _sym_bar], [_sym_bar, _sym_bell, _sym_cherry], [_sym_cherry, _sym_bell, _sym_bar]]
    return _slot_front(OAK, CHROME, BLACK, reels, topper, belly, buttons)


# ---------------------------------------------------------------------------------------------------- showpieces

ARCADE_BLUE = (0x1A, 0x22, 0x5A)
ARCADE_DARK = (0x0E, 0x10, 0x28)
CRT = (0x0A, 0x1A, 0x24)
CYAN = (0x5A, 0xE0, 0xFF)
PURPLE = (0x7A, 0x2A, 0x8A)
PURPLE_DARK = (0x4A, 0x14, 0x5A)
NIGHT = (0x14, 0x14, 0x48)


def lit(b, a, c, tex, level):
    """A box that glows (light_emission)."""
    b.append((tuple(a), tuple(c), tex, level))


def build_arcade():
    b = []
    f = ("front", "arcade")
    box(b, (1, 0, 2), (15, 1, 15), "black_lacquer")
    box(b, (1.5, 1, 4), (14.5, 12, 15), f)                       # lower cabinet, coin door printed
    box(b, (1, 11, 1), (15, 14, 8), "arcade")                     # the control panel, sloping forward
    box(b, (1.2, 13.95, 1.2), (14.8, 14.2, 7.8), "control_panel")
    box(b, (4, 14.2, 3), (5, 17, 4), "black_lacquer")            # joystick
    box(b, (3.4, 17, 2.4), (5.6, 19, 4.6), "red_lacquer")
    for k, x in enumerate((8, 10.5, 13)):
        box(b, (x - 0.8, 14.2, 3.2), (x + 0.8, 15, 4.8), "hold_button" if k == 1 else "red_lacquer")
    box(b, (1.5, 14, 6), (14.5, 28, 15), f)                       # the screen housing
    box(b, (1, 14, 5.5), (2, 30, 15), "black_lacquer")           # side panels
    box(b, (14, 14, 5.5), (15, 30, 15), "black_lacquer")
    box(b, (1.5, 28, 5), (14.5, 31.5, 14), f)                     # marquee
    box(b, (1, 31.5, 4.5), (15, 32.5, 14.5), "black_lacquer")
    lit(b, (2.6, 16, 5.7), (13.4, 27, 5.88), ("front", "glass"), 13)   # the glowing CRT
    lit(b, (2.2, 28.4, 4.7), (13.8, 31, 4.88), ("front", "glass"), 15)  # the lit marquee
    return b


def front_arcade():
    img = _canvas(16, 32, ARCADE_BLUE)
    # Marquee (rows 1..4): ARCADE in lights.
    _rect(img, 2, 1, 14, 4, ARCADE_DARK)
    for x in range(3, 13, 2):
        _px(img, x, 2, (0xFF, 0xD8, 0x40) if x % 4 == 1 else CYAN)
    # The screen (rows 5..16): a game of mines in cyan.
    _rect(img, 2, 5, 14, 16, (0x30, 0x30, 0x3A))
    _rect(img, 3, 6, 13, 15, CRT)
    for y in range(7, 15, 2):
        for x in range(4, 12, 2):
            _px(img, x, y, CYAN if (x + y) % 3 else (0xE0, 0x30, 0x2A))
    # Coin door (rows 21..31).
    _rect(img, 5, 22, 11, 29, (0x2A, 0x2A, 0x30))
    _px(img, 7, 24, (0xE0, 0x30, 0x2A))
    _px(img, 8, 24, (0xE0, 0x30, 0x2A))
    _rect(img, 6, 26, 10, 27, GOLD)
    return img


def build_plinko():
    b = []
    f = ("front", "walnut")
    for x in (3, 29):
        box(b, (x - 2.5, 0, 4), (x + 2.5, 1.5, 12), "gold")
        box(b, (x - 1.2, 1.5, 7), (x + 1.2, 4, 9), "walnut")
    box(b, (1, 4, 6), (31, 31, 10), f)                             # the board
    box(b, (0, 3, 5.5), (32, 4.5, 10.5), "gold")                   # frame
    box(b, (0, 31, 5.5), (32, 32.5, 10.5), "gold")
    box(b, (0, 4.5, 5.5), (1, 31, 10.5), "gold")
    box(b, (31, 4.5, 5.5), (32, 31, 10.5), "gold")
    box(b, (6, 32.5, 6.5), (26, 36, 9.5), ("front", "walnut"))     # sign
    # The pegs.
    for row in range(10):
        for k in range(row + 3):
            x = 16 + (k - (row + 2) / 2) * 2.2
            y = 28.5 - row * 2.1
            box(b, (x - 0.35, y - 0.35, 5.2), (x + 0.35, y + 0.35, 6), "gold")
    lit(b, (1.4, 4.6, 5.7), (30.6, 30.6, 5.88), ("front", "glass"), 9)
    return b


def front_plinko():
    img = _canvas(32, 32, (0x1C, 0x1A, 0x3A))
    # Slots along the bottom (rows 26..28), hot colours at the edges.
    colours = [(0xE0, 0x30, 0x2A), (0xE0, 0x7A, 0x2A), (0xE0, 0xC0, 0x3A), (0x7A, 0xBE, 0x4A), (0x3A, 0x7A, 0x8A)]
    for k in range(13):
        x = 3 + k * 2
        c = colours[max(0, 4 - abs(k - 6)) if abs(k - 6) <= 4 else 0][:3] if False else colours[min(4, max(0, 6 - abs(k - 6) - 2))]
        _rect(img, x, 25, x + 2, 28, c)
        _px(img, x + 1, 25, (0x10, 0x10, 0x20))
    # The ball on its way down.
    _rect(img, 15, 10, 17, 12, (0xE0, 0x30, 0x2A))
    # Sign above the board (rows 0..3 of the top block: y 32..36 is beyond the front image).
    return img


def build_wheel():
    b = []
    box(b, (11, 0, 4), (21, 1.5, 12), "gold")                       # pedestal
    box(b, (14, 1.5, 7), (18, 9, 9), "walnut")
    _disc_xy(b, 16, 21, 12, 7, 9, "walnut", 1)                      # the wheel's back
    _disc_xy(b, 16, 21, 11, 6.4, 7, ("front", "walnut"), 1)        # the wheel's face (printed)
    _disc_xy(b, 16, 21, 2.2, 5.6, 6.4, "gold", 1)                   # the hub
    box(b, (15.2, 33, 6.2), (16.8, 36, 7.2), "red_lacquer")         # the clapper
    box(b, (14.6, 35.5, 6.4), (17.4, 37, 8.6), "gold")
    for i in range(16):
        a = i / 16 * math.pi * 2
        x, y = 16 + math.cos(a) * 11.6, 21 + math.sin(a) * 11.6
        lit(b, (x - 0.45, y - 0.45, 6.1), (x + 0.45, y + 0.45, 6.6), "sparks", 15)
    for i, entry in enumerate(b):
        if isinstance(entry[2], tuple) and len(entry) == 3:
            b[i] = entry + (12,)
    return b


def front_wheel():
    img = _canvas(32, 32, WALNUT)
    colours = [(0xEF, 0xE2, 0xC2), (0x5F, 0x93, 0xC6), (0x7F, 0xC9, 0x6A), (0xE0, 0x62, 0x4F), (0x9C, 0x7A, 0xE0),
               (0x1C, 0x1A, 0x1A), (0xE8, 0xC4, 0x63)]
    pattern = [0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 4, 0, 1, 0, 2, 0, 5, 0, 1, 0, 3, 0, 1, 0, 2, 0, 6]
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, (32 - y - 0.5) - 21 + 0  # model y = 32 - row
            d = math.hypot(dx, dy)
            if d <= 11:
                a = (math.atan2(dy, dx) + math.pi * 2) % (math.pi * 2)
                seg = int(a / (math.pi * 2) * len(pattern))
                c = colours[pattern[seg]]
                if d < 2.5:
                    c = GOLD
                img.putpixel((x, y), c + (255,))
    return img


def build_pusher():
    b = []
    f = ("front", "purple")
    box(b, (0.5, 0, 2), (31.5, 1.5, 15), "gold")
    box(b, (1, 1.5, 3), (31, 13, 15), f)                            # base with the tray
    box(b, (6, 6, 1.5), (26, 7.5, 3), "black_lacquer")              # the prize tray
    box(b, (1, 13, 4), (31, 30, 15), f)                             # the glass case
    box(b, (0.5, 13, 3.5), (1.5, 31, 15), "gold")
    box(b, (30.5, 13, 3.5), (31.5, 31, 15), "gold")
    box(b, (1, 30, 3.5), (31, 34, 14), f)                           # the lit top
    box(b, (0.5, 34, 3), (31.5, 35, 14.5), "gold")
    lit(b, (2, 14, 3.7), (30, 29.5, 3.88), ("front", "glass"), 10)
    lit(b, (2, 30.4, 3.2), (30, 33.6, 3.38), ("front", "glass"), 15)
    return b


def front_pusher():
    img = _canvas(32, 32, PURPLE)
    # Lights along the top (rows 0..2).
    for x in range(1, 31, 2):
        _px(img, x, 1, (0xFF, 0xE0, 0x70) if x % 4 == 1 else (0xE0, 0x6A, 0xF0))
    # The glass: dark inside, the pusher block, shelves heaped with gold coins.
    _rect(img, 2, 3, 30, 19, (0x1A, 0x14, 0x28))
    _rect(img, 4, 4, 28, 8, (0xB8, 0x91, 0x3F))
    _rect(img, 3, 12, 29, 13, (0x8A, 0x8A, 0x92))
    for i in range(30):
        x = 4 + (i * 7) % 24
        y = 9 + (i * 3) % 3
        _rect(img, x, y, x + 2, y + 1, GOLD)
    _rect(img, 3, 17, 29, 18, (0x8A, 0x8A, 0x92))
    for i in range(12):
        _rect(img, 5 + (i * 5) % 22, 15 + i % 2, 7 + (i * 5) % 22, 16 + i % 2, GOLD)
    # The coin slot and the tray.
    _rect(img, 14, 22, 18, 23, (0x10, 0x10, 0x10))
    _rect(img, 7, 25, 25, 28, (0x24, 0x24, 0x2A))
    for i in range(6):
        _rect(img, 9 + i * 3, 26, 11 + i * 3, 27, GOLD)
    return img


def build_baccarat():
    b = []
    for lx, lz in ((5, 5), (43, 5), (5, 27), (43, 27)):
        box(b, (lx - 2.5, 0, lz - 2.5), (lx + 2.5, 1.2, lz + 2.5), "gold")
        box(b, (lx - 1.5, 1.2, lz - 1.5), (lx + 1.5, 11, lz + 1.5), "walnut")
    oval(b, 24, 16, 24, 16, 11, 13, "walnut")
    oval(b, 24, 16, 24, 16, 13, 13.5, "gold")
    oval(b, 24, 16, 24, 16, 13.5, 15, "leather")
    oval(b, 24, 16, 21, 13.5, 14.9, 15.2, ("print", "felt"))
    shoe(b, 39, 24, 15.2)
    for k, chip in enumerate(("chip_red", "chip_black", "chip_green", "chip_blue")):
        box(b, (18 + k * 3, 15.6, 25), (20.4 + k * 3, 16.8 - (k % 2) * 0.4, 27.4), chip)
    return b


def print_baccarat(design):
    img = _top(design)

    def outline(x0, z0, x1, z1, colour):
        _frame(img, x0, _row(design, z1), x1, _row(design, z0), colour)

    def fill(x0, z0, x1, z1, colour):
        _rect(img, x0, _row(design, z1), x1, _row(design, z0), colour)

    # Player (blue), Tie (green) and Banker (red) boxes across the near side; the card boxes behind.
    fill(8, 6, 18, 12, (0x3A, 0x6E, 0xA5))
    fill(19, 6, 29, 12, (0x2E, 0x8B, 0x46))
    fill(30, 6, 40, 12, (0xC0, 0x36, 0x2C))
    outline(8, 6, 40, 12, WHITE)
    outline(12, 15, 22, 21, WHITE)
    outline(26, 15, 36, 21, WHITE)
    for x in range(6, 42, 3):
        _px(img, x, _row(design, 13.5), GOLD)
    return img


def build_kiosk():
    b = []
    f = ("front", "night")
    box(b, (1, 0, 2), (15, 1, 15), "gold")
    box(b, (1.5, 1, 4), (14.5, 11, 15), f)                          # counter
    box(b, (1, 11, 2), (15, 12, 9), "gold")                          # ticket shelf
    box(b, (1.5, 12, 6), (14.5, 26, 15), f)                         # booth with the window
    box(b, (1, 12, 5.5), (2, 26, 15), "gold")
    box(b, (14, 12, 5.5), (15, 26, 15), "gold")
    box(b, (1, 26, 4), (15, 31, 14.5), f)                           # the pot sign
    lit(b, (2.2, 26.4, 3.7), (13.8, 30.6, 3.88), ("front", "glass"), 15)
    lit(b, (3, 14, 5.7), (13, 24, 5.88), ("front", "glass"), 9)
    # The ball cage on top.
    _disc_xy(b, 8, 35, 3.6, 6.5, 11.5, "glass", 1)
    box(b, (7.5, 31, 8.5), (8.5, 31.6, 9.5), "gold")
    for i, colour in enumerate(("chip_red", "chip_blue", "chip_green", "hold_button")):
        box(b, (6 + i * 1.2, 33 + (i % 2) * 1.5, 7.5), (6.9 + i * 1.2, 33.9 + (i % 2) * 1.5, 8.4), colour)
    glow(b, 6, "glass")
    return b


def front_kiosk():
    img = _canvas(16, 32, NIGHT)
    # The pot sign (rows 1..5).
    _rect(img, 2, 1, 14, 5, (0x0A, 0x0A, 0x2A))
    for x in range(3, 13, 2):
        _px(img, x, 2, (0xFF, 0xE0, 0x70))
        _px(img, x + 1, 3, (0xFF, 0xC0, 0x40))
    # The window (rows 8..17): a lit booth with tickets.
    _rect(img, 3, 8, 13, 18, (0x3A, 0x3A, 0x6A))
    _rect(img, 4, 9, 12, 17, (0xF4, 0xEE, 0xDC))
    for y in range(10, 16, 2):
        _rect(img, 5, y, 11, y + 1, (0xE8, 0xC4, 0x63))
    # The counter front (rows 21..31): stripes.
    for y in range(21, 31, 3):
        _rect(img, 2, y, 14, y + 1, (0xE8, 0xC4, 0x63))
    return img


RACE_RED = (0x7A, 0x14, 0x18)
TURF = (0x3E, 0x8E, 0x3A)
TURF_DARK = (0x2E, 0x72, 0x2C)
DIRT = (0xA8, 0x74, 0x42)
AMBER = (0xFF, 0xB0, 0x2A)


DERBY_TRACK = (0x3C, 0x8C, 0x3A)


def derby_lane(t, lane):
    """A point on the derby table's oval track, t from 0 to 1 round it (anticlockwise from the finish line on the near
    straight), lane 0 inside to 5 outside: (x, z) in model pixels. Shared with the in-game horse renderer."""
    cx, cz = 32, 25
    half_straight = 13
    r = 9 + lane * 1.6
    straight = 2 * half_straight
    bend = math.pi * r
    total = 2 * straight + 2 * bend
    d = (t % 1.0) * total
    # Near straight, left to right ... no: run along the near straight towards +x, round the right bend, back along the far
    # straight, round the left bend.
    if d < straight / 2:
        return cx + d, cz - r
    d -= straight / 2
    if d < bend:
        a = -math.pi / 2 + d / r
        return cx + half_straight + math.cos(a) * r, cz + math.sin(a) * r
    d -= bend
    if d < straight:
        return cx + half_straight - d, cz + r
    d -= straight
    if d < bend:
        a = math.pi / 2 + d / r
        return cx - half_straight + math.cos(a) * r, cz + math.sin(a) * r
    d -= bend
    return cx - half_straight + d, cz - r


def build_derby(horses=False):
    """A mechanical horse-racing table, 4 wide, 3 deep and 2 high: an oval track under a chrome dome frame, three betting
    stations along the near side and one at each end, and a big screen on an arch at the back. In game the six little
    horses are drawn by the table's renderer, running the race; {@code horses} adds them still, for previews only."""
    b = []
    # The body and table top.
    box(b, (4, 0, 8), (60, 1, 44), "gold")
    box(b, (5, 1, 9), (59, 10, 43), "black_lacquer")
    box(b, (3, 10, 7), (61, 11.5, 45), "chrome")
    box(b, (4, 11.5, 8), (60, 12, 44), ("print", "black_lacquer"))   # the track, printed
    # The dome's frame: chrome ribs arching over the track, and a ridge along the top.
    for x in (8, 16, 24, 32, 40, 48, 56):
        z = 9.0
        while z < 41:
            zc = z + 1
            y = 12 + 9 * math.sqrt(max(0.0, 1 - ((zc - 25) / 16) ** 2))
            box(b, (x - 0.4, y, z), (x + 0.4, y + 0.8, z + 2), "chrome")
            z += 2
    box(b, (7.6, 20.6, 24.6), (56.4, 21.4, 25.4), "chrome")
    # Betting stations: three along the near side, one at each end; each a console with a lit sloping screen.
    for x in (6, 25, 44):
        box(b, (x, 0, 0.5), (x + 14, 1, 7), "gold")
        box(b, (x + 0.5, 1, 1), (x + 13.5, 11, 7), "black_lacquer")
        box(b, (x, 11, 0.5), (x + 14, 12, 7.5), "chrome")
        box(b, (x + 1, 12, 4.5), (x + 13, 17.5, 6.5), ("front", "black_lacquer"))
        lit(b, (x + 1.6, 12.6, 4.3), (x + 12.4, 17, 4.48), ("front", "glass"), 11)
        box(b, (x + 3, 11.2, 1.5), (x + 11, 12.2, 3.5), "control_panel")
    for x0, x1 in ((0, 4), (60, 64)):
        box(b, (x0, 0, 18), (x1, 11, 32), "black_lacquer")
        box(b, (x0, 11, 18), (x1, 12, 32), "chrome")
    lit(b, (0.2, 12.4, 19), (0.4, 15.6, 31), "neon", 11)
    lit(b, (63.6, 12.4, 19), (63.8, 15.6, 31), "neon", 11)
    # The arch at the back with its big screen, and the lit sign over it.
    box(b, (8, 12, 44.5), (11, 31, 47.5), "chrome")
    box(b, (53, 12, 44.5), (56, 31, 47.5), "chrome")
    box(b, (11, 17, 45), (53, 30, 47), ("front", "black_lacquer"))
    lit(b, (12, 17.8, 44.8), (52, 29.4, 44.98), ("front", "glass"), 13)
    box(b, (10, 30, 44.5), (54, 32, 47.5), "black_lacquer")
    lit(b, (11, 30.3, 44.3), (53, 31.7, 44.48), ("front", "glass"), 15)
    # Green neon under the table's rim.
    lit(b, (5, 8.6, 8.8), (59, 9.4, 9), "neon", 10)
    if horses:
        silks = ["chip_red", "chip_blue", "hold_button", "chip_green", "purple", "chrome"]
        for lane in range(6):
            x, z = derby_lane(0.03 + (5 - lane) * 0.025 + (lane % 2) * 0.01, lane)
            box(b, (x - 1.8, 13, z - 0.6), (x + 1.8, 14.6, z + 0.6), "timber")            # body
            box(b, (x + 1.4, 14.2, z - 0.45), (x + 2.6, 15.8, z + 0.45), "timber")        # neck and head
            box(b, (x - 0.7, 14.6, z - 0.5), (x + 0.5, 16, z + 0.5), silks[lane])          # jockey
            for lx in (-1.5, 1.1):
                box(b, (x + lx, 12, z - 0.4), (x + lx + 0.5, 13, z + 0.4), "black_lacquer")
    return b


def print_derby(design):
    """The track seen from above: a green infield, a dirt oval with six lanes, white rails and the finish line."""
    img = _canvas(64, 48, (0x14, 0x14, 0x18))
    for row in range(48):
        for x in range(64):
            px, pz = x + 0.5, 48 - row - 0.5
            # Distance from the track's centre line (a stadium shape: straights plus round ends).
            cx = min(max(px, 32 - 13), 32 + 13)
            d = math.hypot(px - cx, pz - 25)
            if d < 8:
                c = DERBY_TRACK if (x // 4 + row // 4) % 2 else (0x34, 0x7E, 0x32)
            elif d < 8.8:
                c = WHITE
            elif d < 18.6:
                c = (0xA8, 0x74, 0x42) if (x * 7 + row * 3) % 11 else (0x96, 0x64, 0x36)
            elif d < 19.4:
                c = WHITE
            elif d < 20.4:
                c = DERBY_TRACK
            else:
                continue
            img.putpixel((x, row), c + (255,))
    # The finish line across the near straight, and the winning post.
    for z in range(int(25 - 18.6), int(25 - 8.8) + 1):
        img.putpixel((32, 48 - z - 1), WHITE + (255,) if z % 2 else BLACK + (255,))
    return img


def front_derby():
    """The big screen (rows 2..14), its sign (row 1) and the stations' screens (rows 15..20)."""
    img = _canvas(64, 32, (0x14, 0x14, 0x18))
    # The sign: DERBY in gold lights between chasing bulbs.
    _rect(img, 11, 0, 53, 2, (0x10, 0x30, 0x14))
    for x in range(12, 52, 2):
        _px(img, x, 0, (0x7A, 0xFF, 0x7A) if x % 4 else GOLD)
        _px(img, x + 1, 1, GOLD if x % 4 else (0x7A, 0xFF, 0x7A))
    # The big screen: a race in full stride, odds along the top.
    _rect(img, 12, 2, 52, 15, (0x7A, 0xB8, 0xE8))
    _rect(img, 12, 2, 52, 4, (0x10, 0x10, 0x14))
    for k in range(6):
        x = 14 + k * 6
        _rect(img, x, 2, x + 4, 3, AMBER)
    _rect(img, 12, 6, 52, 7, (0x9A, 0x9A, 0xA8))
    _rect(img, 12, 7, 52, 15, TURF)
    _rect(img, 12, 8, 52, 9, WHITE)
    _rect(img, 12, 9, 52, 15, DIRT)
    sprite = ["..S..H.", ".SS.HHH", "BBBBBB.", "BBBBB..", "L..L..."]
    for x, y, coat, silk in ((36, 9, (0x7A, 0x42, 0x1E), (0xC0, 0x36, 0x2C)), (27, 10, (0x2A, 0x1A, 0x10), (0x3A, 0x8E, 0xE0)),
                             (18, 10, (0xB0, 0xA8, 0xA0), (0xFF, 0xD8, 0x40))):
        for dy, line in enumerate(sprite):
            for dx, ch in enumerate(line):
                colour = {"B": coat, "H": coat, "S": silk, "L": (0x1A, 0x10, 0x08)}.get(ch)
                if colour:
                    _px(img, x + dx, y + dy, colour)
    _rect(img, 46, 7, 47, 15, WHITE)
    # The stations' screens (rows 15..20): a betting grid of six coloured horses and their odds.
    for x0 in (6, 25, 44):
        _rect(img, x0 + 1, 15, x0 + 13, 20, (0x10, 0x18, 0x24))
        for k, c in enumerate(((0xC0, 0x36, 0x2C), (0x3A, 0x8E, 0xE0), (0xFF, 0xD8, 0x40), (0x3A, 0xA0, 0x4A),
                               (0x9A, 0x4A, 0xC0), (0xD0, 0xD4, 0xDA))):
            xx = x0 + 2 + (k % 3) * 4
            yy = 16 + (k // 3) * 2
            _px(img, xx, yy, c)
            _px(img, xx + 1, yy, AMBER)
    return img


# ---------------------------------------------------------------------------------------------------- designs

PLANKS = "minecraft:dark_oak_planks"
DESIGNS = [
    Design("blackjack_table", "Blackjack Table", (3, 2, 1),
           (["WWW", "APA", "DDD"], {"W": "minecraft:green_wool", "A": "minecraft:gold_ingot", "P": "minecraft:paper",
                                    "D": PLANKS}),
           build_blackjack, print_art=print_blackjack),
    Design("craps_table", "Craps Table", (4, 2, 1),
           (["WWW", "BQB", "DDD"], {"W": "minecraft:green_wool", "B": "minecraft:bone", "Q": "minecraft:quartz_block",
                                    "D": PLANKS}),
           build_craps, print_art=print_craps),
    Design("roulette_table", "Roulette Table", (3, 2, 1),
           (["RWB", "GCG", "DDD"], {"R": "minecraft:red_wool", "W": "minecraft:green_wool", "B": "minecraft:black_wool",
                                    "G": "minecraft:gold_ingot", "C": "minecraft:compass", "D": PLANKS}),
           build_roulette, print_art=print_roulette),
    Design("slot_machine_classic", "Fruit Slot Machine", (1, 1, 2),
           (["IGI", "ARA", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "A": "minecraft:apple",
                                    "R": "minecraft:redstone"}),
           build_classic, front_art=front_classic, light=4),
    Design("slot_machine_clockwork", "Clockwork Slot Machine", (1, 1, 2),
           (["NGN", "CRC", "NLN"], {"N": "minecraft:gold_ingot", "G": "minecraft:glass_pane", "C": "minecraft:clock",
                                    "R": "minecraft:redstone", "L": "minecraft:lever"}),
           build_clockwork, front_art=front_clockwork, light=4),
    Design("slot_machine_gem", "Gem Mine Slot Machine", (1, 1, 2),
           (["SGS", "DRE", "SMS"], {"S": "minecraft:deepslate_bricks", "G": "minecraft:glass_pane",
                                    "D": "minecraft:diamond", "R": "minecraft:redstone", "E": "minecraft:emerald",
                                    "M": "minecraft:minecart"}),
           build_gem, front_art=front_gem, light=4),
    Design("slot_machine_progressive", "Progressive Jackpot Slot Machine", (1, 1, 2),
           (["LGL", "BRB", "OOO"], {"L": "minecraft:glowstone", "G": "minecraft:glass_pane", "B": "minecraft:gold_block",
                                    "R": "minecraft:redstone_block", "O": "minecraft:obsidian"}),
           build_progressive, front_art=front_progressive, light=7),
    Design("slot_machine_nether", "Nether Furnace Slot Machine", (1, 1, 2),
           (["SGS", "MRM", "NBN"], {"S": "minecraft:blackstone", "G": "minecraft:glass_pane", "M": "minecraft:magma_block",
                                    "R": "minecraft:redstone", "N": "minecraft:nether_bricks", "B": "minecraft:blaze_rod"}),
           build_nether, front_art=front_nether, light=6),
    Design("slot_machine_treasure", "Sunken Treasure Slot Machine", (1, 1, 2),
           (["PGP", "HRN", "PLP"], {"P": "minecraft:prismarine_bricks", "G": "minecraft:glass_pane",
                                    "H": "minecraft:heart_of_the_sea", "R": "minecraft:redstone",
                                    "N": "minecraft:nautilus_shell", "L": "minecraft:sea_lantern"}),
           build_treasure, front_art=front_treasure, light=5),
    Design("slot_machine_creeper", "Creeper Blast Slot Machine", (1, 1, 2),
           (["CGC", "URU", "KTK"], {"C": "minecraft:green_concrete", "G": "minecraft:glass_pane",
                                    "U": "minecraft:gunpowder", "R": "minecraft:redstone", "K": "minecraft:black_concrete",
                                    "T": "minecraft:tnt"}),
           build_creeper, front_art=front_creeper, light=4),
    Design("slot_machine_pub", "Pub Fruit Machine", (1, 1, 2),
           (["OGO", "ARS", "IBI"], {"O": "minecraft:oak_planks", "G": "minecraft:glass_pane", "A": "minecraft:apple",
                                    "R": "minecraft:redstone", "S": "minecraft:sweet_berries", "I": "minecraft:iron_ingot",
                                    "B": "minecraft:stone_button"}),
           build_pub, front_art=front_pub, light=4),
    Design("slot_machine_shark", "Shark Frenzy Slot Machine", (1, 1, 2),
           (["BGB", "FRF", "BIB"], {"B": "minecraft:blue_concrete", "G": "minecraft:glass_pane", "F": "minecraft:cod",
                                    "R": "minecraft:redstone", "I": "minecraft:iron_ingot"}),
           build_shark, front_art=front_shark, light=11),
    Design("slot_machine_dragon", "Golden Dragon Slot Machine", (1, 1, 2),
           (["RGR", "LDL", "RBR"], {"R": "minecraft:red_concrete", "G": "minecraft:glass_pane", "L": "minecraft:lantern",
                                    "D": "minecraft:redstone", "B": "minecraft:gold_block"}),
           build_dragon, front_art=front_dragon, light=11),
    Design("casino_arcade", "Casino Arcade", (1, 1, 2),
           (["KGK", "LRL", "KBK"], {"K": "minecraft:black_concrete", "G": "minecraft:glass_pane", "L": "minecraft:lapis_lazuli",
                                    "R": "minecraft:redstone", "B": "minecraft:stone_button"}),
           build_arcade, front_art=front_arcade, light=12),
    Design("plinko_board", "Plinko Board", (2, 1, 2),
           (["NGN", "NNN", "PRP"], {"N": "minecraft:iron_nugget", "G": "minecraft:glass_pane", "P": PLANKS,
                                    "R": "minecraft:redstone"}),
           build_plinko, front_art=front_plinko, light=10),
    Design("prize_wheel", "Prize Wheel", (2, 1, 2),
           (["RYB", "GCG", "PPP"], {"R": "minecraft:red_dye", "Y": "minecraft:yellow_dye", "B": "minecraft:blue_dye",
                                    "G": "minecraft:gold_ingot", "C": "minecraft:gold_block", "P": PLANKS}),
           build_wheel, front_art=front_wheel, light=12),
    Design("coin_pusher", "Coin Pusher", (2, 1, 2),
           (["GGG", "IPI", "IRI"], {"G": "minecraft:glass_pane", "I": "minecraft:iron_ingot", "P": "minecraft:piston",
                                    "R": "minecraft:redstone"}),
           build_pusher, front_art=front_pusher, light=10),
    Design("baccarat_table", "Baccarat Table", (3, 2, 1),
           (["WWW", "EPE", "DDD"], {"W": "minecraft:green_wool", "E": "minecraft:emerald", "P": "minecraft:paper",
                                    "D": PLANKS}),
           build_baccarat, print_art=print_baccarat),
    Design("lottery_kiosk", "Lottery Kiosk", (1, 1, 2),
           (["YGY", "PRP", "IHI"], {"Y": "minecraft:yellow_concrete", "G": "minecraft:glass_pane", "P": "minecraft:paper",
                                    "R": "minecraft:redstone", "I": "minecraft:iron_ingot", "H": "minecraft:hopper"}),
           build_kiosk, front_art=front_kiosk, light=9),
    Design("derby_table", "Derby Table", (4, 3, 2),
           (["LLL", "SCS", "WHW"], {"L": "minecraft:glass_pane", "S": "minecraft:saddle", "C": "minecraft:clock",
                                    "W": "minecraft:green_wool", "H": "minecraft:hay_block"}),
           build_derby, print_art=print_derby, front_art=front_derby, light=11),
]
BY_ID = {d.block: d for d in DESIGNS}
MAX_PARTS = 24
BLOCKS = [d.block for d in DESIGNS]

# Scratch cards: flat items bought at a lottery kiosk (no recipe). id -> (name, face colour, dark edge).
SCRATCH_CARDS = {
    "scratch_card_copper": ("Copper Scratch Card", (184, 106, 58), (106, 58, 26)),
    "scratch_card_iron": ("Iron Scratch Card", (168, 168, 176), (90, 90, 98)),
    "scratch_card_gold": ("Gold Scratch Card", (232, 196, 99), (138, 106, 44)),
}
ITEMS = list(SCRATCH_CARDS)

# Shared poker textures (exchange_poker) and this module's own.
POKER_TEXTURES = ["felt", "walnut", "leather", "gold", "gold_dark", "glass", "gear"]
OWN_TEXTURES = ["chip_red", "chip_green", "chip_black", "chip_blue", "card", "die", "ball", "diamonds", "chrome",
                "red_lacquer", "deepslate", "timber", "iron", "ore", "neon", "lights", "blackstone", "nether_brick",
                "magma", "prize_wheel", "prismarine", "prismarine_dark", "kelp", "sea_lantern", "creeper", "black_lacquer",
                "tnt", "sparks", "oak", "hold_button", "arcade", "control_panel", "purple", "night",
                "ocean", "ocean_dark", "water", "shark", "dragon", "dragon_roof", "lantern", "pearl"]


# ---------------------------------------------------------------------------------------------------- models

def _uv(x0, y0, z0, x1, y1, z1):
    v0, v1 = _r(16 - y1), _r(16 - y0)
    return {
        "north": [_r(16 - x1), v0, _r(16 - x0), v1],
        "south": [_r(x0), v0, _r(x1), v1],
        "east": [_r(16 - z1), v0, _r(16 - z0), v1],
        "west": [_r(z0), v0, _r(z1), v1],
        "up": [_r(x0), _r(z0), _r(x1), _r(z1)],
        "down": [_r(x0), _r(16 - z1), _r(x1), _r(16 - z0)],
    }


def _face_textures(tex):
    """{face: texture variable} for a box's texture: a name, ("print", side) or ("front", side)."""
    if isinstance(tex, tuple):
        kind, side = tex
        faces = {f: side for f in ("north", "south", "east", "west", "up", "down")}
        faces["up" if kind == "print" else "south"] = kind
        return faces
    return {f: tex for f in ("north", "south", "east", "west", "up", "down")}


def part_index(design, i, j, k):
    return (k * design.depth + j) * design.width + i


def part_elements(design, boxes, part):
    """The boxes inside one block of the design, in that block's own model space (facing north)."""
    i = part % design.width
    j = (part // design.width) % design.depth
    k = part // (design.width * design.depth)
    out = []
    for entry in boxes:
        (ax, ay, az), (bx, by, bz), tex = entry[:3]
        glow = entry[3] if len(entry) > 3 else 0
        x0, x1 = max(ax, 16 * i), min(bx, 16 * i + 16)
        # The top block's models may reach above it (Minecraft allows up to 32): table tops and toppers stand proud.
        y0 = max(ay, 16 * k)
        y1 = by if k == design.height - 1 else min(by, 16 * k + 16)
        z0, z1 = max(az, 16 * j), min(bz, 16 * j + 16)
        if x1 - x0 < 0.01 or y1 - y0 < 0.01 or z1 - z0 < 0.01:
            continue
        mx0, mx1 = x0 - 16 * i, x1 - 16 * i
        my0, my1 = y0 - 16 * k, y1 - 16 * k
        mz0, mz1 = 16 - (z1 - 16 * j), 16 - (z0 - 16 * j)
        uv = _uv(mx0, my0, mz0, mx1, my1, mz1)
        out.append({"from": [_r(mx0), _r(my0), _r(mz0)], "to": [_r(mx1), _r(my1), _r(mz1)],
                    "faces": {side: {"uv": uv[side], "texture": f"#{t}"} for side, t in _face_textures(tex).items()}})
        if glow:
            out[-1]["light_emission"] = glow
    return out


def item_elements(design, boxes):
    """The whole design scaled to fit the item's block: tables at half size, machines at half size."""
    scale = 0.5
    sx = design.width * 16 * scale
    sz = design.depth * 16 * scale
    sy = design.height * 16 * scale
    ox, oz, oy = 8 - sx / 2, 8 - sz / 2, max(0.0, 8 - sy / 2)
    out = []
    for entry in boxes:
        (ax, ay, az), (bx, by, bz), tex = entry[:3]
        glow = entry[3] if len(entry) > 3 else 0
        x0, x1 = ox + ax * scale, ox + bx * scale
        y0, y1 = oy + ay * scale, oy + by * scale
        z0, z1 = oz + (design.depth * 16 - bz) * scale, oz + (design.depth * 16 - az) * scale
        if x1 - x0 < 0.1 or y1 - y0 < 0.1 or z1 - z0 < 0.1:
            continue
        uv = _uv(x0, y0, z0, x1, y1, z1)  # model_writer.finish_elements fits any UV that leaves the sprite
        faces = {}
        for side, t in _face_textures(tex).items():
            # Prints and fronts belong to the blocks; the small item uses the plain side texture there.
            if t in ("print", "front"):
                t = tex[1]
            faces[side] = {"uv": uv[side], "texture": f"#{t}"}
        out.append({"from": [_r(x0), _r(y0), _r(z0)], "to": [_r(x1), _r(y1), _r(z1)], "faces": faces})
        if glow:
            out[-1]["light_emission"] = glow
    return out


def textures_for(design, rid, part=None):
    textures = {name: rid(f"block/poker_{name}") for name in POKER_TEXTURES}
    textures.update({name: rid(f"block/casino_{name}") for name in OWN_TEXTURES})
    textures["particle"] = rid("block/poker_walnut")
    if part is not None:
        if design.print_art:
            textures["print"] = rid(f"block/{design.block}_print{part}")
        if design.front_art:
            textures["front"] = rid(f"block/{design.block}_front{part}")
    return textures


def write_assets(write, rid, assets, data, lang):
    assets = pathlib.Path(assets)
    data = pathlib.Path(data)
    for design in DESIGNS:
        boxes = design.build()
        variants = {}
        for part in range(design.parts):
            write(assets / "models" / "block" / f"{design.block}_part{part}.json",
                  {"parent": "minecraft:block/block", "textures": textures_for(design, rid, part),
                   "elements": part_elements(design, boxes, part)})
        # CasinoBlock.PART runs 0..7 for every game; parts a game does not use show its first block.
        for part in range(MAX_PARTS):
            model = part if part < design.parts else 0
            for facing, turn in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                variant = {"model": rid(f"block/{design.block}_part{model}")}
                if turn:
                    variant["y"] = turn
                variants[f"facing={facing},part={part}"] = variant
        write(assets / "blockstates" / f"{design.block}.json", {"variants": variants})
        tall = design.height > 1
        write(assets / "models" / "item" / f"{design.block}.json", {
            "parent": "minecraft:block/block", "textures": textures_for(design, rid),
            "elements": item_elements(design, boxes),
            "display": {"gui": {"rotation": [30, 225 if not tall else 200, 0], "translation": [0, 0, 0],
                                "scale": [0.42, 0.42, 0.42] if design.width >= 4 else [0.6, 0.6, 0.6] if not tall
                                else [0.75, 0.75, 0.75]},
                        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.3, 0.3, 0.3]},
                        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.35, 0.35, 0.35]}}})
        write(assets / "items" / f"{design.block}.json",
              {"model": {"type": "minecraft:model", "model": rid(f"item/{design.block}")}})
        write(data / MOD / "loot_table" / "blocks" / f"{design.block}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(design.block)}],
                       "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": rid(f"blocks/{design.block}")})
        pattern, key = design.recipe
        write(data / MOD / "recipe" / f"{design.block}.json", {
            "fabric:load_conditions": [{"condition": f"{MOD}:feature_enabled", "feature": FEATURE}],
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
            "result": {"id": rid(design.block), "count": 1}})
        lang[f"block.{MOD}.{design.block}"] = design.title
    for item, (title, _, _) in SCRATCH_CARDS.items():
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
        lang[f"item.{MOD}.{item}"] = title


# ---------------------------------------------------------------------------------------------------- textures

def _solid(colour, light=None, dark=None, noise=0):
    img = _canvas(16, 16, colour)
    for y in range(16):
        for x in range(16):
            if noise and (x * 7 + y * 13) % noise == 0:
                img.putpixel((x, y), tuple(min(255, c + 12) for c in colour) + (255,))
    if light:
        _rect(img, 0, 0, 16, 1, light)
        _rect(img, 0, 0, 1, 16, light)
    if dark:
        _rect(img, 0, 15, 16, 16, dark)
        _rect(img, 15, 0, 16, 16, dark)
    return img


def _chip(colour):
    img = _canvas(16, 16, colour)
    for y in range(16):
        if y % 4 == 1:
            _rect(img, 0, y, 16, y + 1, WHITE)
        for x in range(0, 16, 4):
            if y % 4 == 2:
                _px(img, x, y, WHITE)
    return img


def _die():
    img = _canvas(16, 16, WHITE)
    for x, y in ((3, 3), (8, 8), (12, 12), (3, 12), (12, 3)):
        _rect(img, x - 1, y - 1, x + 1, y + 1, BLACK)
    return img


def _diamonds():
    img = _canvas(16, 16, (0x22, 0x22, 0x24))
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                img.putpixel((x, y), (0x38, 0x38, 0x3C, 255))
    return img


def _timber():
    img = _canvas(16, 16, (0x6B, 0x4A, 0x2A))
    for x in range(0, 16, 5):
        _rect(img, x, 0, x + 1, 16, (0x52, 0x37, 0x1E))
    return img


def _ore():
    img = _canvas(16, 16, (0x4A, 0x4A, 0x50))
    for i, c in enumerate((DIAMOND, EMERALD, GOLD_ORE, REDSTONE, LAPIS, DIAMOND, GOLD_ORE, EMERALD)):
        x, y = (i * 5 + 2) % 15, (i * 7 + 3) % 15
        _rect(img, x, y, x + 2, y + 2, c)
    return img


def _lights():
    img = _canvas(16, 16, CHROME_DARK)
    for y in range(1, 16, 3):
        _rect(img, 0, y, 16, y + 2, YELLOW if (y // 3) % 2 == 0 else NEON_LIGHT)
    return img


def _neon():
    img = _canvas(16, 16, NEON)
    _rect(img, 0, 0, 16, 1, NEON_LIGHT)
    _rect(img, 0, 15, 16, 16, (0x3A, 0x14, 0x5C))
    return img


def own_textures():
    return {
        "chip_red": _chip(RED), "chip_green": _chip(ZERO_GREEN), "chip_black": _chip(BLACK),
        "chip_blue": _chip((0x2A, 0x4C, 0xB0)),
        "card": _solid(WHITE, dark=(0xC8, 0xC0, 0xB0)),
        "die": _die(),
        "ball": _solid((0xF6, 0xF6, 0xF6)),
        "diamonds": _diamonds(),
        "chrome": _solid(CHROME, CHROME_LIGHT, CHROME_DARK),
        "red_lacquer": _solid(RED, (0xD8, 0x4A, 0x40), (0x7A, 0x16, 0x14)),
        "deepslate": _solid((0x3A, 0x3A, 0x40), noise=5),
        "timber": _timber(),
        "iron": _solid((0x9A, 0x9A, 0x9E), (0xC0, 0xC0, 0xC4), (0x60, 0x60, 0x66)),
        "ore": _ore(),
        "neon": _neon(),
        "lights": _lights(),
        "blackstone": _solid(BLACKSTONE, noise=4),
        "nether_brick": _bricks(NETHER_BRICK, (0x22, 0x0C, 0x10)),
        "magma": _magma(),
        "prize_wheel": _prize_wheel(),
        "prismarine": _solid(PRISMARINE, noise=3),
        "prismarine_dark": _bricks(PRISMARINE_DARK, (0x1E, 0x40, 0x3C)),
        "kelp": _kelp(),
        "sea_lantern": _sea_lantern(),
        "creeper": _creeper(),
        "black_lacquer": _solid((0x1C, 0x1C, 0x1E), (0x3A, 0x3A, 0x3E), (0x0A, 0x0A, 0x0A)),
        "tnt": _tnt(),
        "sparks": _solid((0xFF, 0xC0, 0x40), (0xFF, 0xF0, 0xA0)),
        "oak": _bricks(OAK, (0x7A, 0x56, 0x30), plank=True),
        "hold_button": _solid(YELLOW_LIT, (0xFF, 0xF4, 0xB0), (0xC0, 0x90, 0x10)),
        "arcade": _solid(ARCADE_BLUE, (0x2A, 0x34, 0x7A), ARCADE_DARK),
        "control_panel": _control_panel(),
        "purple": _solid(PURPLE, (0x9A, 0x4A, 0xAA), PURPLE_DARK),
        "night": _solid(NIGHT, (0x2A, 0x2A, 0x6A), (0x0A, 0x0A, 0x2A)),
        "ocean": _ocean(),
        "ocean_dark": _solid(OCEAN_DARK, (0x1A, 0x3A, 0x6A), (0x02, 0x08, 0x18)),
        "water": _solid((0x1E, 0x7A, 0xD0), (0x7A, 0xD8, 0xFF), (0x0E, 0x4A, 0x90)),
        "shark": _solid(SHARK, SHARK_LIGHT, (0x4A, 0x52, 0x5E)),
        "dragon": _scales(),
        "dragon_roof": _bricks(DRAGON_RED, DRAGON_DARK, plank=True),
        "lantern": _lantern(),
        "pearl": _solid((0xFF, 0xF4, 0xD0), (0xFF, 0xFF, 0xFF), (0xE0, 0xC8, 0x90)),
    }


def _ocean():
    img = _canvas(16, 16, OCEAN)
    for y in range(2, 16, 5):
        for x in range(16):
            if (x + y) % 6 < 3:
                _px(img, x, y + (x // 3) % 2, (0x1A, 0x44, 0x82))
    return img


def _scales():
    img = _canvas(16, 16, DRAGON_RED)
    for y in range(0, 16, 4):
        for x in range((y // 4) % 2 * 2, 16, 4):
            _rect(img, x, y + 2, x + 3, y + 3, DRAGON_DARK)
            _px(img, x + 1, y + 1, (0xC8, 0x3A, 0x2A))
    return img


def _lantern():
    img = _canvas(16, 16, (0xE0, 0x2A, 0x1A))
    _rect(img, 0, 0, 16, 2, GOLD)
    _rect(img, 0, 14, 16, 16, GOLD)
    for x in range(2, 16, 4):
        _rect(img, x, 2, x + 1, 14, (0xB0, 0x14, 0x10))
    _rect(img, 5, 5, 11, 11, (0xFF, 0x6A, 0x3A))
    return img


def _control_panel():
    img = _canvas(16, 16, (0x22, 0x22, 0x2A))
    for x in range(0, 16, 4):
        _rect(img, x, 0, x + 2, 1, CYAN)
    return img


def _bricks(colour, mortar, plank=False):
    img = _canvas(16, 16, colour)
    for y in range(0, 16, 4):
        _rect(img, 0, y, 16, y + 1, mortar)
        if not plank:
            off = 0 if (y // 4) % 2 == 0 else 4
            for x in range(off, 16, 8):
                _rect(img, x, y, x + 1, y + 4, mortar)
    return img


def _magma():
    img = _canvas(16, 16, (0x5A, 0x1E, 0x0C))
    for y in range(16):
        for x in range(16):
            if (x * 5 + y * 3) % 7 < 2:
                img.putpixel((x, y), MAGMA + (255,))
            elif (x * 5 + y * 3) % 7 == 2:
                img.putpixel((x, y), (0xFF, 0xA0, 0x30, 255))
    return img


def _prize_wheel():
    img = _canvas(16, 16, NETHER_BRICK)
    colours = [MAGMA, BLAZE, (0xC0, 0x30, 0x30), GOLD, (0x8A, 0x2A, 0x6A), BLAZE, MAGMA, (0xE0, 0xE0, 0xD0)]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            a = (math.atan2(dy, dx) + math.pi) / (2 * math.pi)
            img.putpixel((x, y), colours[int(a * 8) % 8] + (255,))
            if math.hypot(dx, dy) < 2:
                img.putpixel((x, y), GOLD + (255,))
    return img


def _kelp():
    img = _canvas(16, 16, (0x2E, 0x6A, 0x22))
    for y in range(0, 16, 3):
        _px(img, (y * 5) % 16, y, (0x5A, 0xA8, 0x3A))
    return img


def _sea_lantern():
    img = _canvas(16, 16, SEA_GLOW)
    for i in range(16):
        _px(img, i, i, (0xA8, 0xD8, 0xC8))
        _px(img, 15 - i, i, (0xA8, 0xD8, 0xC8))
    return img


def _creeper():
    img = _canvas(16, 16, CREEPER)
    for y in range(16):
        for x in range(16):
            v = (x * 7 + y * 11) % 9
            if v == 0:
                img.putpixel((x, y), CREEPER_DARK + (255,))
            elif v == 4:
                img.putpixel((x, y), (0x7A, 0xD0, 0x6A, 255))
    return img


def _tnt():
    img = _canvas(16, 16, TNT_RED)
    _rect(img, 0, 5, 16, 11, WHITE)
    for x, w in ((2, 3), (7, 3), (12, 3)):
        _rect(img, x, 6, x + w, 7, BLACK)
        _rect(img, x + 1, 7, x + 2, 10, BLACK)
    for x in range(0, 16, 4):
        _rect(img, x, 0, x + 1, 5, (0x9A, 0x20, 0x18))
        _rect(img, x, 11, x + 1, 16, (0x9A, 0x20, 0x18))
    return img


def design_images(design):
    """{texture name: image} for the design's printed tops and fronts, cut per block."""
    out = {}
    if design.print_art:
        top = design.print_art(design)
        for j in range(design.depth):
            for i in range(design.width):
                crop = top.crop((16 * i, (design.depth - 1 - j) * 16, 16 * i + 16, (design.depth - j) * 16))
                for k in range(design.height):
                    out[f"{design.block}_print{part_index(design, i, j, k)}"] = crop
    if design.front_art:
        front = design.front_art()
        for k in range(design.height):
            for i in range(design.width):
                crop = front.crop((16 * i, (design.height - 1 - k) * 16, 16 * i + 16, (design.height - k) * 16))
                for j in range(design.depth):
                    out[f"{design.block}_front{part_index(design, i, j, k)}"] = crop
    return out


def scratch_card_image(face, dark):
    """A 16x16 scratch card held slightly tilted: coloured card, a 3x3 grid of silver panels, two already rubbed off."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(2, 14):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (2, 13)
            px[x, y] = (*dark, 255) if edge else (*face, 255)
    # Title strip.
    for x in range(3, 13):
        px[x, 3] = (60, 20, 20, 255)
    silver, shine, paper, ink = (184, 188, 196, 255), (220, 223, 228, 255), (244, 238, 220, 255), (192, 54, 44, 255)
    for i in range(3):
        for j in range(3):
            x0, y0 = 3 + i * 4, 5 + j * 3
            rubbed = (i, j) in ((0, 0), (2, 1))
            for dx in range(3):
                for dy in range(2):
                    if rubbed:
                        px[x0 + dx, y0 + dy] = ink if (dx == 1 and dy == 0) else paper
                    else:
                        px[x0 + dx, y0 + dy] = shine if (dx + dy) % 3 == 0 else silver
    return img


def draw_item_textures(save_item):
    """save_item(image, name): the scratch cards' icons."""
    for item, (_, face, dark) in SCRATCH_CARDS.items():
        save_item(scratch_card_image(face, dark), item)


def draw_textures(save_block):
    """save_block(image, name, mcmeta): every casino texture."""
    for name, img in own_textures().items():
        save_block(img, f"casino_{name}", None)
    for design in DESIGNS:
        for name, img in design_images(design).items():
            save_block(img, name, None)
