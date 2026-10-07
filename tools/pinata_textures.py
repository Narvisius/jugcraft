"""Original textures for the piñata party (fall addition 28): crepe-paper fringe in tiers in orange, green, black, pink,
yellow and turquoise, and torn to the papier-mâché beneath; the pumpkin's black-paper face and the bat's; a fringe skirt
and tassel streamers, cut out between their strips; the bat's scalloped wings, whole and torn; the rope; the items; the
Blindfold's view (misc/blindfold, 256 by 128, as vanilla's pumpkin blur is drawn) and its band as worn (four times the 64
by 32 armour layout).

Drawn in vanilla's manner (tools/fair_pixels.py, after the owner's note of 6 October 2026): 16 texels a block, scaled
up to the 64 by 64 the models were made for, so every file keeps its UVs; the items are 16 by 16 pixel art.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no other texture is read, traced or
recoloured.
"""
from PIL import Image

from fair_pixels import box, px16, sprite
from fur_paint import mix
from crop_textures import rgb

N = 64

PAPER = {
    "orange": [rgb("6a2a04"), rgb("b04c0a"), rgb("e4741a"), rgb("fa9a38"), rgb("ffc480")],
    "green": [rgb("0e3010"), rgb("1e5a20"), rgb("348a34"), rgb("58b450"), rgb("9ad88a")],
    "black": [rgb("08080a"), rgb("16141a"), rgb("26222c"), rgb("3a3442"), rgb("5a5266")],
    "pink": [rgb("6a0c34"), rgb("b01c5a"), rgb("e83c84"), rgb("fa70a8"), rgb("ffb0d0")],
    "yellow": [rgb("6a5004"), rgb("b08a0a"), rgb("e8c01c"), rgb("fadc48"), rgb("fff0a0")],
    "turquoise": [rgb("04404a"), rgb("0a7a84"), rgb("1cb4c0"), rgb("48d8e0"), rgb("a0f0f4")],
    "purple": [rgb("1e0a30"), rgb("3c1660"), rgb("5e2a92"), rgb("8448c0"), rgb("b88ae0")],
}
NEWSPRINT = [rgb("6a5a42"), rgb("9a8a6c"), rgb("c4b494"), rgb("dccfae"), rgb("eee4c8")]
JUTE = [rgb("4a3418"), rgb("7a5a2c"), rgb("a68048"), rgb("c8a46a"), rgb("e4c894")]
CLOTH = [rgb("060608"), rgb("121216"), rgb("1e1e24"), rgb("2c2c34"), rgb("40404a")]
WOOD = [rgb("4a2e14"), rgb("7a5028"), rgb("a8743c"), rgb("c89a5c")]
TIERS = 8


# Patches ripped off a torn fringe, (x0, y0, x1, y1), their corners left on.
TEARS = [(2, 3, 6, 6), (9, 8, 13, 12), (3, 11, 6, 13)]
# How far each strip of a skirt or tassel hangs, strip by strip.
SKIRT = [14, 12, 15, 13]
TASSEL = [15, 13, 16, 14, 12, 15]


def fringe(paper, seed, torn=False):
    """Crepe paper in tiers in vanilla's manner (fair_pixels): eight tiers two pixels deep, each lit along its top row,
    its lower row cut into strips (a darker cut every third pixel, staggered tier by tier). Torn, a few patches are
    ripped off to the tan papier-mâché beneath, its newsprint lines showing, edged in the paper's pale torn edge."""
    def paint(p):
        for y in range(16):
            tier, row = divmod(y, 2)
            for x in range(16):
                cut = row == 1 and (x + tier) % 3 == 0
                p.put(x, y, paper[1] if cut else paper[3] if row == 0 else paper[2])
        if torn:
            for x0, y0, x1, y1 in TEARS:
                for y in range(y0, y1 + 1):
                    for x in range(x0, x1 + 1):
                        if x in (x0, x1) and y in (y0, y1):
                            continue
                        edge = x in (x0, x1) or y in (y0, y1)
                        p.put(x, y, paper[4] if edge else NEWSPRINT[1] if y % 2 == 0 else NEWSPRINT[3])
    return px16(paint)


def edge(paper, seed):
    """A fringe skirt: a band along the top, then strips three pixels wide hanging to their ends, cut out between."""
    def paint(p):
        for x in range(16):
            for y in range(4):
                p.put(x, y, paper[3] if y == 0 else paper[2])
            if x % 4 == 3:
                continue
            length = SKIRT[x // 4]
            for y in range(4, length):
                p.put(x, y, paper[3] if x % 4 == 0 else paper[1] if y == length - 1 else paper[2])
    return px16(paint)


def tassel():
    """Tassel streamers: strips of every colour two pixels wide, hanging to their ends, cut out between them."""
    colours = ["pink", "yellow", "turquoise", "orange", "green", "purple"]

    def paint(p):
        for x in range(16):
            if x % 3 == 2:
                continue
            paper = PAPER[colours[(x // 3) % len(colours)]]
            for y in range(TASSEL[x // 3]):
                p.put(x, y, paper[3] if x % 3 == 0 else paper[2])
    return px16(paint)


FACE_PUMPKIN = [
    "................",
    "................",
    "...#........#...",
    "..###......###..",
    ".#####....#####.",
    "................",
    ".......##.......",
    "......####......",
    "................",
    "................",
    "..##...##...##..",
    "..############..",
    "...##########...",
    "....###..###....",
    "................",
    "................",
]


def face_pumpkin():
    """The pumpkin's face cut from black paper, in the manner of a carved pumpkin: triangle eyes, a nose and a grin
    with square teeth, left clear round it so the fringe shows."""
    return sprite(FACE_PUMPKIN, {"#": PAPER["black"][1]})


def face_bat():
    """The bat's face on black fringe: round yellow eyes with black pupils and a glint, a pink nose, two white fangs."""
    def paint(p):
        p.img.paste(fringe(PAPER["black"], 28013).resize((16, 16), Image.NEAREST))
        for x0 in (3, 10):
            box(p, x0, 4, x0 + 2, 6, rgb("e8c020"))
            p.put(x0 + 1, 3, rgb("e8c020"))
            p.put(x0 + 1, 7, rgb("c89a10"))
            box(p, x0 + 1, 5, x0 + 1, 6, (0, 0, 0))
            p.put(x0, 4, (255, 255, 255))
        box(p, 7, 9, 8, 9, rgb("ff90b8"))
        for x in (6, 9):
            box(p, x, 11, x, 12, rgb("f4f0e8"))
    return px16(paint)


def wing(torn=False):
    """A bat wing of black paper: finger bones in purple fanning from the shoulder (top inner corner), the membrane
    scalloped between them at its foot, cut out below. Torn, it has holes through it."""
    fingers = [(15, 0, 0, 10), (15, 0, 5, 13), (15, 0, 11, 14)]

    def paint(p):
        for y in range(16):
            for x in range(16):
                scallop = 12 + (0, 1, 2, 2, 1)[x % 5]
                if y <= scallop:
                    p.put(x, y, PAPER["black"][2] if y > 0 else PAPER["black"][3])
        for x0, y0, x1, y1 in fingers:
            steps = max(abs(x1 - x0), abs(y1 - y0))
            for k in range(steps + 1):
                p.put(round(x0 + (x1 - x0) * k / steps), round(y0 + (y1 - y0) * k / steps), PAPER["purple"][3])
        if torn:
            for x0, y0 in ((3, 3), (9, 6), (5, 9)):
                for x, y in ((x0, y0), (x0 + 1, y0), (x0, y0 + 1), (x0 + 1, y0 + 1), (x0 + 2, y0 + 1)):
                    p.clear(x, y)
    return px16(paint)


def rope():
    """Jute rope: its strands twisting on a slant, three tones."""
    def paint(p):
        for y in range(16):
            for x in range(16):
                p.put(x, y, JUTE[(3, 2, 1)[((x + y) // 2) % 3]])
    return px16(paint)


# ---------------------------------------------------------------- items

def item_pumpkin():
    """The Pumpkin Piñata: an orange paper pumpkin in tiers on its rope, its face cut in black."""
    orange = PAPER["orange"]

    def paint(p):
        for y in range(3):
            p.put(7, y, JUTE[2])
        p.put(7, 3, PAPER["green"][2])
        p.put(8, 3, PAPER["green"][3])
        for y in range(4, 15):
            for x in range(1, 15):
                dx, dy = (x + 0.5 - 8) / 6.6, (y + 0.5 - 9.5) / 5.6
                if dx * dx + dy * dy <= 1:
                    k = 3 if y % 2 == 0 else 2
                    p.put(x, y, orange[k - 1] if x in (4, 11) else orange[k])
        for y in range(4, 15):
            for x in range(1, 15):
                inside = ((x + 0.5 - 8) / 6.6) ** 2 + ((y + 0.5 - 9.5) / 5.6) ** 2 <= 1
                near = any(((x + dx + 0.5 - 8) / 6.6) ** 2 + ((y + dy + 0.5 - 9.5) / 5.6) ** 2 > 1
                           for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if inside and near:
                    p.put(x, y, orange[0])
        for x, y in ((5, 7), (4, 8), (5, 8), (6, 8), (10, 7), (9, 8), (10, 8), (11, 8),
                     (4, 11), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11), (11, 11), (5, 12), (7, 12), (8, 12), (10, 12)):
            p.put(x, y, PAPER["black"][1])
    return px16(paint)


STAR_PINATA = [
    ".......jj.......",
    "........j.......",
    ".......Yy.......",
    ".......Yy.......",
    "....dd.YyYy.dd..",
    "....dD.YYyy.Dd..",
    "..TT..CCcc..GG..",
    "..TtTtCccc.gGgG.",
    "..ttTtcccc.gGgG.",
    "..tt..cccc..gg..",
    "....dd.OOoo.dd..",
    "....dd.OOoo.dd..",
    ".......Oo.......",
    ".......Oo.......",
    ".....T....Y.....",
    ".....T....Y.....",
]


def item_star():
    """The Star Piñata: a pink centre with points of yellow, orange, turquoise and green round it and four little
    purple ones between, streamers hanging below, on its rope."""
    return sprite(STAR_PINATA, {"j": JUTE[2], "Y": PAPER["yellow"][4], "y": PAPER["yellow"][3], "O": PAPER["orange"][4],
                                "o": PAPER["orange"][3], "T": PAPER["turquoise"][4], "t": PAPER["turquoise"][3],
                                "G": PAPER["green"][4], "g": PAPER["green"][3], "C": PAPER["pink"][4], "c": PAPER["pink"][3],
                                "D": PAPER["purple"][4], "d": PAPER["purple"][3]})


BAT_PINATA = [
    "................",
    ".......j........",
    ".......j........",
    ".....k....k.....",
    ".....kkkkkk.....",
    ".....kYkkYk.....",
    "..w..kkkkkk..w..",
    ".www.kkkkkk.www.",
    "wwwwwkkkkkkwwwww",
    "wwwwwwkkkkwwwwww",
    "w.w.wwkkkkww.w.w",
    "......kkkk......",
    ".......kk.......",
    "................",
    "................",
    "................",
]


def item_bat():
    """The Bat Piñata: a black paper bat, wings spread and scalloped, ears up, yellow eyes, on its rope."""
    return sprite(BAT_PINATA, {"j": JUTE[2], "k": PAPER["black"][1], "w": PAPER["black"][3], "Y": rgb("f0c428")})


def item_stick():
    """The Piñata Stick: a stick in red and white bands on a slant, a tassel at its grip."""
    def paint(p):
        for k in range(12):
            colour = rgb("c41e1e") if (k // 2) % 2 == 0 else rgb("f4f0e8")
            p.put(2 + k, 13 - k, colour)
            p.put(3 + k, 13 - k, mix(colour, (0, 0, 0), 0.3))
        for y in range(13, 16):
            p.put(1, y, PAPER["yellow"][3])
            p.put(2, y + (1 if y < 15 else 0), PAPER["turquoise"][3])
    return px16(paint)


def item_blindfold():
    """The Blindfold: a band of black cloth sagging a little, knotted at one end with its two tails hanging."""
    def paint(p):
        for x in range(1, 13):
            sag = 1 if 3 <= x <= 9 else 0
            p.put(x, 6 + sag, CLOTH[4])
            p.put(x, 7 + sag, CLOTH[3])
            p.put(x, 8 + sag, CLOTH[2])
        box(p, 12, 5, 14, 8, CLOTH[2])
        p.put(12, 5, CLOTH[4])
        for k in range(4):
            p.put(13 - k // 2, 9 + k, CLOTH[3])
            p.put(14, 9 + k, CLOTH[2])
    return px16(paint)


# ---------------------------------------------------------------- the blindfold, seen and worn

def blindfold_view():
    """Looking out from under the Blindfold, 256x128 (drawn at 64 by 32, four texels to a pixel): dark woven cloth, a
    faint row of weave every fourth pixel, over everything but a sliver at the bottom, which fades in steps."""
    def paint(p):
        for y in range(32):
            alpha = 252 if y < 27 else 252 - (y - 26) * 30
            for x in range(64):
                p.put(x, y, (22, 22, 28) if y % 4 == 0 else (14, 14, 18), alpha)
    return px16(paint, 64, 32, 4)


def blindfold_worn():
    """The Blindfold as worn, on vanilla's 64 by 32 armour layout four times over: a band of black cloth round the head at
    the eyes, knotted at the back with its two ends hanging."""
    def paint(p):
        for x in range(32):
            p.put(x, 11, CLOTH[3])
            p.put(x, 12, CLOTH[2])
        box(p, 27, 10, 29, 13, CLOTH[3])
        p.put(28, 10, CLOTH[4])
        for k in range(3):
            p.put(27, 14 + k, CLOTH[2])
            p.put(29, 14 + k, CLOTH[2])
    return px16(paint, 64, 32, 4)


def pinata_textures():
    blocks = {}
    for i, paper in enumerate(("orange", "green", "black", "pink", "yellow", "turquoise")):
        blocks[f"pinata_fringe_{paper}"] = fringe(PAPER[paper], 28100 + i)
    for i, paper in enumerate(("orange", "black", "pink", "yellow", "turquoise")):
        blocks[f"pinata_fringe_{paper}_torn"] = fringe(PAPER[paper], 28100 + i, torn=True)
    blocks.update({"pinata_edge_orange": edge(PAPER["orange"], 28030), "pinata_edge_purple": edge(PAPER["purple"], 28031),
                   "pinata_tassel": tassel(), "pinata_face_pumpkin": face_pumpkin(), "pinata_face_bat": face_bat(),
                   "pinata_wing": wing(), "pinata_wing_torn": wing(torn=True), "pinata_rope": rope()})
    out = {("block", name): img for name, img in blocks.items()}
    out.update({("item", "pumpkin_pinata"): item_pumpkin(), ("item", "star_pinata"): item_star(), ("item", "bat_pinata"): item_bat(),
                ("item", "pinata_stick"): item_stick(), ("item", "blindfold"): item_blindfold(),
                ("misc", "blindfold"): blindfold_view(), ("entity", "equipment/humanoid/blindfold"): blindfold_worn()})
    return out
