"""Original textures for the hot-air balloon fiesta (fall addition 29):
  - the basket and rigging: close-woven wicker, padded oxblood leather, the floor's planks, gunmetal steel, brass, the
    burner's copper coils, a fuel tank in red enamel with a stencilled serial, the instrument panel's three green-lit
    dials, the load cables and the suede sleeves on the uprights;
  - each envelope as one wrap 768 by 384 (entity/hot_air_balloon/envelope_<kind>), 32 texels to each of its 24 gores,
    its crown at the top and its throat at the bottom:
    - Harvest Stripes: gores of pumpkin, gold, cranberry and cream, a band of maple leaves round its widest, a cream
      crown;
    - the Jack-o'-Lantern: a ribbed orange pumpkin with a carved face on its front and back, over a green skirt of
      leaves; glow_pumpkin is that face alone, lit, for the night glow;
    - Harvest Moon: a night sky deepening to the crown, stars, a great harvest moon on its front with a witch on her
      broom across it, bats round it, and hills with pumpkins along its foot;
  - the burner's flame (blue at its root, gold at its tongues), the pumpkin's stem, the pibal's red latex;
  - the Mooring Post: black cast iron, a granite plinth, tarred rope and brass;
  - the items: the three balloons, the burner and a pibal.

Drawn in vanilla's manner (tools/fair_pixels.py, after the owner's note of 6 October 2026): the basket, post and
items at 16 texels a block (or 16 by 16 icons), scaled up to the 64 by 64 the models were made for; the envelopes in flat
bands with pixel art four texels to a pixel (the Jack-o'-Lantern's face seven).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; nothing is read, traced or recoloured.
"""
import math
import random

import block_style as bs
from fair_pixels import box, px16
from fur_paint import clean_painter as Painter, clean_ramp as ramp
from crop_textures import rgb

N = 64
WRAP_W, WRAP_H = 768, 384
GORE = WRAP_W // 24

WICKER = [rgb("3e2810"), rgb("6a4820"), rgb("94703a"), rgb("bc9a5c"), rgb("dcc28a")]
LEATHER = [rgb("220806"), rgb("40120c"), rgb("62201a"), rgb("843228"), rgb("a8503e")]
PLANK = [rgb("3a2416"), rgb("5c3c24"), rgb("7e5634"), rgb("9c7048"), rgb("b88c60")]
GUNMETAL = [rgb("121416"), rgb("24282c"), rgb("3a4046"), rgb("596068"), rgb("8a929a")]
BRASS = [rgb("4a3010"), rgb("7a5420"), rgb("a8803a"), rgb("d0aa5c"), rgb("f0d898"), rgb("fff4d4")]
COPPER = [rgb("3a160a"), rgb("6a2c14"), rgb("9c4a24"), rgb("cc7040"), rgb("eea070")]
RED = [rgb("3a0606"), rgb("680e0c"), rgb("981a14"), rgb("c42c22"), rgb("e65a46")]
CREAM = [rgb("6e6656"), rgb("a49a84"), rgb("cfc6ae"), rgb("e9e2cc"), rgb("f9f5e8")]
SUEDE = [rgb("2a1c10"), rgb("44301c"), rgb("624a2e"), rgb("806446"), rgb("9e8262")]
ROPE = [rgb("2c2418"), rgb("4c4030"), rgb("70624a"), rgb("968868"), rgb("b8ac8c")]
PUMPKIN = [rgb("4a1a02"), rgb("8a3406"), rgb("c4560e"), rgb("ec7c1c"), rgb("ffaa4c"), rgb("ffd08a")]
GOLD = [rgb("5a3c08"), rgb("8e6412"), rgb("c0901e"), rgb("e6b838"), rgb("fbde7c")]
CRANBERRY = [rgb("380612"), rgb("620c22"), rgb("8c1632"), rgb("b42646"), rgb("d8506c")]
LEAF_GREEN = [rgb("0c2210"), rgb("183a1a"), rgb("285828"), rgb("3e7a36"), rgb("68a050")]
NIGHT = [rgb("05060f"), rgb("0c1028"), rgb("162048"), rgb("223268"), rgb("34508c")]
MOON = [rgb("6a3a10"), rgb("b06a1c"), rgb("e09a34"), rgb("f6c060"), rgb("fff0b0")]
GLOW = [rgb("a04008"), rgb("e07a10"), rgb("ffb02a"), rgb("ffe070"), rgb("fff8d0")]
IRON = [rgb("0c0c0e"), rgb("1c1d20"), rgb("2e3034"), rgb("464a50"), rgb("6a7078")]
GRANITE = [rgb("38363a"), rgb("58565a"), rgb("78747a"), rgb("9a969c"), rgb("bcb8be")]
SOOT = [rgb("101010"), rgb("1e1c1a"), rgb("2e2a26"), rgb("403a34")]


# ---------------------------------------------------------------- basket and rigging

def wicker():
    """Close basket weave: weavers two pixels deep passing in front of one stake and behind the next, each lit along its
    top, darker in the gaps between stakes."""
    def paint(p):
        for y in range(16):
            for x in range(16):
                over = (y // 2 + x // 4) % 2 == 0
                if x % 4 == 3:
                    k = 0
                elif over:
                    k = 3 if y % 2 == 0 else 2
                else:
                    k = 2 if y % 2 == 0 else 1
                p.put(x, y, WICKER[k])
    return px16(paint)


def leather():
    """Padded oxblood leather: lit along its top, a shade darker at its foot, a row of cream stitches along each edge."""
    def paint(p):
        for y in range(16):
            for x in range(16):
                p.put(x, y, LEATHER[3] if y == 0 else LEATHER[1] if y == 15 else LEATHER[2])
        for x in range(0, 16, 2):
            p.put(x, 1, CREAM[3])
            p.put(x, 14, CREAM[3])
    return px16(paint)


def floor():
    return px16(bs.planks(PLANK, 29003, boards=4, vertical=True))


def plate(palette, rivets=True, band=None):
    """A flat metal plate as a vanilla iron block: one tone, lit along its top and left, a shade darker along its
    bottom and right; raised bolts at the corners (lit, with a shadow below them); `band` rows rubbed bright."""
    def paint(p):
        for y in range(16):
            for x in range(16):
                k = 3 if x == 0 or y == 0 else 1 if x == 15 or y == 15 else 2
                if band and y in band:
                    k = 4 if y == band[0] else 3
                p.put(x, y, palette[k])
        if rivets:
            for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
                p.put(x, y, palette[4])
                p.put(x, y + 1, palette[1])
    return px16(paint)


def steel():
    """Gunmetal with heavy bolts at its corners."""
    return plate(GUNMETAL)


# Brass across a rail or a cap, lit on its left as a vanilla gold block is: an index into BRASS for each column.
BRASS_COLUMNS = [4, 4, 5, 4, 4, 3, 3, 3, 3, 3, 3, 2, 2, 2, 1, 1]


def brass():
    return px16(lambda p: [p.put(x, y, BRASS[BRASS_COLUMNS[x]]) for y in range(16) for x in range(16)])


HEAT = [rgb("4a3a6a"), rgb("6a5a8a"), rgb("8a7aa8")]


def coil():
    """The burner's coil from the side: copper tube in tight turns two pixels deep, each lit along its top, blued by
    the heat in the top turns."""
    def paint(p):
        for y in range(16):
            colours = HEAT if y < 4 else COPPER[1:]
            for x in range(16):
                k = 2 if y % 2 == 0 else 0
                if x == 0 and k < 2:
                    k += 1
                p.put(x, y, colours[k])
    return px16(paint)


def tank():
    """A fuel tank in red enamel, lit down its left; a cream band round it with a stencilled serial, and two chips in
    the paint to the steel."""
    columns = [3, 4, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1]

    def paint(p):
        for y in range(16):
            for x in range(16):
                p.put(x, y, (CREAM if 6 <= y < 10 else RED)[columns[x]])
        for x in (2, 3, 5, 6, 8, 9, 11, 12):
            p.put(x, 7, GUNMETAL[1])
            p.put(x, 8, GUNMETAL[1] if x % 3 else CREAM[columns[x]])
        for x, y in ((4, 2), (13, 12)):
            p.put(x, y, GUNMETAL[3])
    return px16(paint)


def gauge():
    """The instrument panel: three square dials with green-lit faces (altimeter, variometer, envelope heat) in brass
    rims on a gunmetal panel, each with its needle, and an amber lamp below."""
    def paint(p):
        for y in range(16):
            for x in range(16):
                p.put(x, y, GUNMETAL[2])
        for x0, needle in ((1, (2, 6)), (6, (8, 5)), (11, (13, 6))):
            box(p, x0, 4, x0 + 3, 7, BRASS[3])
            box(p, x0 + 1, 5, x0 + 2, 6, rgb("1c6a30"))
            p.put(*needle, rgb("b8ffcc"))
        box(p, 7, 11, 8, 12, rgb("e0a020"))
        p.put(7, 11, rgb("ffd060"))
    return px16(paint)


def rope():
    """Load cable: its strands twisting on a slant, three tones."""
    return px16(lambda p: [p.put(x, y, ROPE[(3, 2, 1)[((x + y) // 2) % 3]]) for y in range(16) for x in range(16)])


def suede():
    return px16(lambda p: bs.cloth(SUEDE[1:4], 29010)(p))


# ---------------------------------------------------------------- envelopes

def crown_and_throat(p, crown, throat_colours):
    """The crown's parachute valve (a ring of a darker shade and its edge tape) and the throat's scorch-proof scoop."""
    for y in range(WRAP_H):
        for x in range(WRAP_W):
            if y < 10:
                p.put(x, y, ramp(crown, 0.5 + 0.1 * math.sin(x * 0.05)))
            elif y < 13:
                p.put(x, y, ramp(BRASS, 0.55))
            if y >= WRAP_H - 16:
                f = 0.4 + 0.15 * (p.noise(x, y, 6.0) - 0.5) - 0.2 * (y - (WRAP_H - 16)) / 16
                p.put(x, y, ramp(throat_colours, f))


# The envelopes' cells: CELL texels to a pixel, so a gore is GORE // CELL pixels wide.
CELL = 4
CW, CH, CG = WRAP_W // CELL, WRAP_H // CELL, GORE // CELL


def gore_tone(colours, x):
    """A gore's flat bands across: its load tape dark, lit left of its middle, a shade darker towards its right seam."""
    across = x % CG
    return colours[1] if across == 0 else colours[3] if across in (1, 2, 3) else colours[2] if across < CG - 1 else colours[1]


def crown_and_throat_cells(p, crown):
    """The crown's valve in the crown colour with a brass edge tape, and the throat's dark scorch-proof scoop."""
    for x in range(CW):
        for y in range(3):
            p.put(x, y, crown[1] if y < 2 else crown[2])
        p.put(x, 3, BRASS[3])
        for y in range(CH - 4, CH):
            p.put(x, y, SOOT[1] if y < CH - 2 else SOOT[0])


MAPLE = [
    "...#...",
    ".#.#.#.",
    ".#####.",
    "#######",
    ".#####.",
    "...#...",
    "...#...",
]


def harvest():
    """Harvest Stripes in cells: gores of pumpkin, gold, cranberry and cream in flat bands down from a cream crown, and a
    dark band round its widest, edged in gold, with a pixel maple leaf on each gore in the colours of fall."""
    order = (PUMPKIN, GOLD, CRANBERRY, CREAM)
    band = (int(CH * 0.34), int(CH * 0.46))

    def paint(p):
        for y in range(CH):
            for x in range(CW):
                if y < 12:
                    colour = CREAM[3] if y > 5 else CREAM[2]
                elif band[0] <= y < band[1]:
                    colour = GOLD[3] if y in (band[0], band[1] - 1) else SOOT[1]
                else:
                    colour = gore_tone(order[(x // CG) % 4], x)
                p.put(x, y, colour)
        top = band[0] + 2
        for k in range(CW // CG):
            colours = (PUMPKIN, GOLD, CRANBERRY)[k % 3]
            left = k * CG + 1
            for r, row in enumerate(MAPLE):
                for c, ch in enumerate(row):
                    if ch == "#":
                        p.put(left + c, top + r, colours[3] if r < 3 else colours[2])
        crown_and_throat_cells(p, CREAM)
    return px16(paint, CW, CH, CELL)


WITCH = [
    "......#.................",
    ".....##.................",
    "....###.................",
    "..#######...............",
    "....###.................",
    "...#####................",
    "..#######...............",
    ".#########..............",
    "########################",
    "..#####.......######....",
    "...###.........######...",
    "................####....",
]
BAT = ["#...#", "##.##", ".###."]


def moon():
    """Harvest Moon in cells: night blue in three bands deepening to the crown, single-pixel stars, a great pixel moon
    on its front with a witch on her broom across it, bats round about, and dark hills edged in gold along its foot with
    lit pumpkins on them."""
    rng = random.Random(29104)
    hills = int(CH * 0.8)

    def paint(p):
        for y in range(CH):
            colour = NIGHT[1] if y < CH * 0.33 else NIGHT[2] if y < CH * 0.66 else NIGHT[3]
            for x in range(CW):
                p.put(x, y, colour)
        for _ in range(110):
            x, y = rng.randrange(CW), rng.randrange(5, hills - 2)
            p.put(x, y, (255, 248, 210) if rng.random() < 0.3 else NIGHT[4])
        mx, my, mr = CW * 0.25, CH * 0.42, 17
        for y in range(CH):
            for x in range(CW):
                dx, dy = x + 0.5 - mx, (y + 0.5 - my) * 1.15
                d = math.hypot(dx, dy)
                if d <= mr:
                    p.put(x, y, MOON[4] if dx + dy < -mr * 0.6 else MOON[2] if d > mr - 1.5 and dx + dy > 0 else MOON[3])
        for cx, cy in ((-6, -4), (5, 3), (-2, 7), (8, -6)):
            box(p, int(mx + cx), int(my + cy), int(mx + cx) + 1, int(my + cy) + 1, MOON[2])
        left, top = int(mx - 13), int(my - 3)
        for r, row in enumerate(WITCH):
            for c, ch in enumerate(row):
                if ch == "#":
                    p.put(left + c, top + r, (8, 8, 14))
        for k in range(14):
            bx, by = rng.randrange(CW - 5), rng.randrange(14, hills - 10)
            if abs(bx - mx) < mr + 6 and abs(by - my) < mr + 6:
                continue
            for r, row in enumerate(BAT):
                for c, ch in enumerate(row):
                    if ch == "#":
                        p.put(bx + c, by + r, (8, 8, 14))
        for x in range(CW):
            crest = hills + round(2.5 * math.sin(x * 0.084) + 1.5 * math.sin(x * 0.23 + 1.3))
            p.put(x, crest - 1, GOLD[3])
            for y in range(crest, CH):
                p.put(x, y, SOOT[2] if y == crest else SOOT[1])
        for k in range(14):
            x = 5 + k * 13 + rng.randrange(5)
            crest = hills + round(2.5 * math.sin(x * 0.084) + 1.5 * math.sin(x * 0.23 + 1.3))
            box(p, x, crest + 1, x + 2, crest + 2, PUMPKIN[3])
            p.put(x + 1, crest + 1, GLOW[3])
        crown_and_throat_cells(p, NIGHT)
    return px16(paint, CW, CH, CELL)


# The Jack-o'-Lantern's carved face as pixel art, in the manner of vanilla's carved pumpkin (the owner's note on the
# fall fair, 6 October 2026: nothing in it looked like Minecraft): stepped triangle eyes and nose and a grin with square
# teeth, one character a cell of FACE_CELL texels. '#' is cut.
FACE = [
    "........#............#........",
    ".......###..........###.......",
    ".......###..........###.......",
    "......#####........#####......",
    ".....#######......#######.....",
    ".....#######......#######.....",
    "....#########....#########....",
    "..............................",
    "..............##..............",
    ".............####.............",
    "..............................",
    "..............................",
    "...##....................##...",
    "....#####..########..#####....",
    "....#####..########..#####....",
    ".....####################.....",
    "......######..##..######......",
    "........####..##..####........",
    "..........##########..........",
]
FACE_CELL = 7


def pumpkin_face(p, cx, colours, lit):
    """The carved face (FACE) centred at `cx`, its top at the eyes' line. Lit, the cut glows in three flat tones,
    brightest at the middle; carved, the cut is dark, its top row a shade of the shell's inside for the depth."""
    top = int(WRAP_H * 0.26)
    left = int(cx) - len(FACE[0]) * FACE_CELL // 2
    rows, cols = len(FACE), len(FACE[0])

    def cut(r, c):
        return 0 <= r < rows and 0 <= c < cols and FACE[r][c] == "#"
    for r in range(-1, rows + 1):
        for c in range(-1, cols + 1):
            if cut(r, c):
                if lit:
                    d = abs(c + 0.5 - cols / 2) / (cols / 2)
                    colour = GLOW[4] if d < 0.3 else GLOW[3] if d < 0.65 else GLOW[2]
                else:
                    colour = SOOT[1] if cut(r - 1, c) else PUMPKIN[0]
            else:
                continue
            for y in range(top + r * FACE_CELL, top + (r + 1) * FACE_CELL):
                for x in range(left + c * FACE_CELL, left + (c + 1) * FACE_CELL):
                    p.put(x % WRAP_W, y, colour)


def pumpkin():
    """The Jack-o'-Lantern: an orange pumpkin, each rib (two gores) lit across its swell and shadowed into its creases,
    faint streaks along it; a face carved on its front and back; a green skirt of leaves below; the stem's foot."""
    p = Painter(WRAP_W, WRAP_H, 29102)
    skirt_y = int(WRAP_H * (1 - 0.18))
    for y in range(WRAP_H):
        for x in range(WRAP_W):
            # Each rib in flat bands, as the ribs of vanilla's pumpkin: a dark crease, a shoulder, the body and a lit
            # swell left of its middle.
            rib = (x % (2 * GORE)) / (2 * GORE)
            colour = (PUMPKIN[1] if rib < 0.06 or rib > 0.94 else PUMPKIN[2] if rib < 0.2 or rib > 0.8
                      else PUMPKIN[4] if 0.32 < rib < 0.48 else PUMPKIN[3])
            if y < 18:
                colour = LEAF_GREEN[2]
            if y >= skirt_y:
                colour = LEAF_GREEN[3] if (x // 12) % 2 else LEAF_GREEN[2]
            p.put(x, y, colour)
    # Scalloped leaf points over the skirt's top.
    for x in range(WRAP_W):
        u = (x % 24) / 24
        for y in range(skirt_y - 10, skirt_y):
            if y >= skirt_y - 10 * math.sin(u * math.pi):
                p.put(x, y, ramp(LEAF_GREEN, 0.55))
    for cx in (WRAP_W * 0.25, WRAP_W * 0.75):
        pumpkin_face(p, cx, PUMPKIN, lit=False)
    crown_and_throat(p, LEAF_GREEN, SOOT)
    return p.img


def pumpkin_glow():
    """The carved faces alone, lit: drawn over the envelope at full brightness while the burner fires."""
    p = Painter(WRAP_W, WRAP_H, 29103)
    for cx in (WRAP_W * 0.25, WRAP_W * 0.75):
        pumpkin_face(p, cx, GLOW, lit=True)
    return p.img


def flame():
    """The burner's flame, 64 by 128 (drawn 16 by 32): a hard blue root over the jets, flaring gold with a white-hot core,
    licking into tongues at the top, cut out round them."""
    def paint(p):
        for y in range(32):
            t = 1 - (y + 0.5) / 32
            half = 2.0 + 5.5 * math.sin(min(1.0, t * 1.6) * math.pi * 0.85) * (1 - t ** 3)
            for x in range(16):
                u = abs(x + 0.5 - 8)
                if u > half or (t > 0.7 and (x + y) % 3 == 0 and u > half - 2):
                    continue
                core = 1 - u / max(0.5, half)
                if t < 0.14:
                    colour = rgb("a0c8ff") if core > 0.5 else rgb("2040ff")
                else:
                    colour = GLOW[4] if core > 0.66 else GLOW[3] if core > 0.33 else GLOW[2]
                p.put(x, y, colour)
    return px16(paint, 16, 32)


def stem():
    """The Jack-o'-Lantern's stem: green in fibres running down it."""
    return px16(bs.streaks(LEAF_GREEN[1:5], 29202, vertical=True, spread=0.7))


def pibal_latex():
    """Red latex, lit down its left with a glint."""
    columns = [3, 4, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1]

    def paint(p):
        for y in range(16):
            for x in range(16):
                p.put(x, y, RED[columns[x]])
        box(p, 3, 3, 4, 5, (255, 220, 210))
    return px16(paint)


# ---------------------------------------------------------------- the mooring post

def iron():
    """Black cast iron, as a vanilla iron block in black, rubbed bright in a band where hands and ropes go."""
    return plate(IRON, rivets=False, band=(10, 11))


def granite():
    """The plinth's granite, as vanilla stone: small clumps of grey with a few cracks, lit along its top."""
    def paint(p):
        bs.stone(GRANITE[1:4], 29302, cracks=2, spread=0.7)(p)
        for x in range(16):
            p.put(x, 0, GRANITE[4])
    return px16(paint)


TAR = [rgb("1a140c"), rgb("3a2e1c"), rgb("5e4c30"), rgb("84704c")]


def tarred_rope():
    return px16(lambda p: [p.put(x, y, TAR[(3, 2, 1)[((x - y) // 2) % 3]]) for y in range(16) for x in range(16)])


# ---------------------------------------------------------------- items

def envelope_mask():
    """The balloon's envelope on a 16 by 16 icon: (x, y) -> inside, round over a tapering skirt down to its throat."""
    def inside(x, y):
        dx, dy = (x + 0.5 - 8) / 6.6, (y + 0.5 - 6.5) / 6.2
        if y > 11:
            return False
        if dy <= 0:
            return dx * dx + dy * dy <= 1
        return abs(dx) <= math.sqrt(max(0.0, 1 - dy * dy)) * (1 - 0.5 * dy)
    return inside


def balloon_item(kind):
    """A balloon aloft as a vanilla item icon: its envelope in its design, outlined, the basket below on its cables."""
    inside = envelope_mask()

    def paint(p):
        for y in range(16):
            for x in range(16):
                if not inside(x, y):
                    continue
                lit = x < 7
                if kind == "harvest":
                    colours = SOOT if y == 7 else (PUMPKIN, GOLD, CRANBERRY, CREAM)[(x // 2) % 4]
                elif kind == "pumpkin":
                    colours = PUMPKIN
                    lit = lit and x not in (4, 8)
                else:
                    colours = NIGHT
                p.put(x, y, colours[3] if lit else colours[2])
                if not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    p.put(x, y, colours[0] if kind != "moon" else NIGHT[0])
        if kind == "pumpkin":
            for x, y in ((5, 4), (4, 5), (5, 5), (6, 5), (10, 4), (9, 5), (10, 5), (11, 5),
                         (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (6, 9), (9, 9)):
                p.put(x, y, GLOW[3])
            p.put(8, 0, LEAF_GREEN[3])
        if kind == "moon":
            box(p, 4, 3, 6, 5, MOON[3])
            p.put(4, 3, MOON[4])
            for x, y in ((9, 2), (11, 5), (8, 7), (12, 8)):
                p.put(x, y, (255, 248, 210))
        for x, y in ((5, 12), (10, 12)):
            p.put(x, y, ROPE[2])
        for x in range(5, 11):
            p.put(x, 13, LEATHER[2])
            p.put(x, 14, WICKER[3])
            p.put(x, 15, WICKER[1])
    return px16(paint)


def burner_item():
    """The burner: two copper coils capped in brass on a gunmetal frame, a brass valve lever, a blue pilot flame."""
    def paint(p):
        for x0 in (3, 9):
            box(p, x0, 3, x0 + 3, 4, BRASS[4])
            for y in range(5, 11):
                for x in range(x0, x0 + 4):
                    p.put(x, y, COPPER[3] if y % 2 == 0 else COPPER[1])
        box(p, 2, 11, 13, 12, GUNMETAL[3])
        box(p, 2, 12, 13, 12, GUNMETAL[1])
        for k in range(3):
            p.put(10 + k, 13 + k, BRASS[3])
        box(p, 7, 0, 8, 2, rgb("6a9cff"))
        p.put(7, 1, rgb("d8e8ff"))
    return px16(paint)


def pibal_item():
    """The pibal: a red latex balloon lit at its upper left, knotted, on a wavy cream string."""
    def paint(p):
        for y in range(11):
            for x in range(16):
                dx, dy = (x + 0.5 - 8) / 5.2, (y + 0.5 - 5.5) / 5.4
                if dx * dx + dy * dy <= 1:
                    edge = (dx * dx + dy * dy) > 0.72
                    p.put(x, y, RED[1] if edge and dx + dy > 0 else RED[3] if dx + dy < -0.4 else RED[2])
        box(p, 5, 2, 6, 3, (255, 220, 210))
        p.put(7, 11, RED[1])
        p.put(8, 11, RED[1])
        for y, x in zip(range(12, 16), (8, 7, 8, 9)):
            p.put(x, y, CREAM[3])
    return px16(paint)


def hot_air_balloon_textures():
    blocks = {"balloon_wicker": wicker(), "balloon_leather": leather(), "balloon_floor": floor(), "balloon_steel": steel(),
              "balloon_brass": brass(), "balloon_coil": coil(), "balloon_tank": tank(), "balloon_gauge": gauge(),
              "balloon_rope": rope(), "balloon_suede": suede(),
              "mooring_post_iron": iron(), "mooring_post_stone": granite(), "mooring_post_rope": tarred_rope(), "mooring_post_brass": brass()}
    out = {("block", name): img for name, img in blocks.items()}
    entity = {"envelope_harvest": harvest(), "envelope_pumpkin": pumpkin(), "envelope_moon": moon(), "glow_pumpkin": pumpkin_glow(),
              "flame": flame(), "stem": stem(), "pibal": pibal_latex()}
    out.update({("entity", f"hot_air_balloon/{name}"): img for name, img in entity.items()})
    out[("item", "harvest_balloon")] = balloon_item("harvest")
    out[("item", "pumpkin_balloon")] = balloon_item("pumpkin")
    out[("item", "harvest_moon_balloon")] = balloon_item("moon")
    out[("item", "balloon_burner")] = burner_item()
    out[("item", "pibal")] = pibal_item()
    return out
