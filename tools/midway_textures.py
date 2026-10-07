"""Original textures for the fall fair midway (fall addition 26): the High Striker's painted wood, brass rail and
bell, its lamps lit and unlit, the puck and pad, the four bands of its scale and its signboard; Ring Toss's crate and its
green, amber and milk-glass bottles and the ring; the first seven plushes' felt and faces (the harvest plushes are
painted in tools/decor16_data.py); and the striker, mallet and ring as items.

Drawn in vanilla's manner (tools/fair_pixels.py, after the owner's note of 6 October 2026): 16 texels a block, scaled
up to the 64 by 64 the models were made for, so every file keeps its UVs. Painted wood is planks, brass is lit like a gold
block, felt is wool and the faces are square pixel eyes and mouths.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code; no other texture is read, traced or
recoloured.
"""
import math

import block_style as bs
import fair_pixels as fp
from fair_pixels import box, px16, sprite
from fur_paint import mix
from crop_textures import rgb
from midway import HIGH_STRIKER, RING_TOSS, PLUSHES

N = 64
S = 4  # texels to a model pixel

WOOD_WHITE = [rgb("8c8478"), rgb("bab2a2"), rgb("d8d0be"), rgb("ece6d6"), rgb("f8f4ea")]
LACQUER_RED = [rgb("4a0a0a"), rgb("7a1212"), rgb("a81c18"), rgb("cc2e26"), rgb("e85a48")]
GOLD = [rgb("5a3c0a"), rgb("8c6418"), rgb("bc9030"), rgb("e0bc5a"), rgb("fbe7a4")]
BRASS = [rgb("4a3010"), rgb("7a5420"), rgb("a8803a"), rgb("d0aa5c"), rgb("f0d898"), rgb("fff4d4")]
PLANK = [rgb("4a3020"), rgb("6c4a30"), rgb("8a6440"), rgb("a67e54"), rgb("c09a6c")]
RUBBER = [rgb("0c0c0e"), rgb("1c1c20"), rgb("2e2e34"), rgb("46464e")]
YELLOW = [rgb("7a5a04"), rgb("c8960a"), rgb("f0c428"), rgb("ffe680")]
GLASS = {"green": [rgb("0e2a14"), rgb("1c4a24"), rgb("2e6e36"), rgb("4c9a52"), rgb("9ad8a0")],
         "amber": [rgb("3a1c04"), rgb("6a3608"), rgb("9a5612"), rgb("c87e24"), rgb("f0c070")],
         "milk": [rgb("8a8e90"), rgb("b4b8ba"), rgb("d6d9da"), rgb("eceeee"), rgb("ffffff")]}
SCALE_BANDS = {1: [rgb("1e5a1e"), rgb("2e8a2e"), rgb("54b84a")], 2: [rgb("6a6a10"), rgb("b0b020"), rgb("e0dc40")],
               3: [rgb("8a4a08"), rgb("cc7212"), rgb("f09a2a")], 4: [rgb("7a0e0e"), rgb("c01c1c"), rgb("f04434")]}


# Brass across a rail or bell, lit on its left as a vanilla gold block is: an index into GOLD for each column.
GOLD_COLUMNS = [3, 3, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1]
# Painted planks: the paint's palette without its darkest tone, so the seams between boards stay a shade, not a gap.
WHITE_PAINT = WOOD_WHITE[1:]
RED_PAINT = [rgb("7a1212"), rgb("a81c18"), rgb("b42420"), rgb("cc2e26"), rgb("e85a48")]


def striker_textures():
    """The High Striker in vanilla's manner (px16): white- and red-painted planks, brass like a gold block, lamps of
    flat glass tones with a square glint, and a scale of plain coloured bands with ticks."""
    out = {}

    def post(p):
        bs.planks(WHITE_PAINT, 26001, boards=4, vertical=True)(p)
        for y in range(16):
            p.put(0, y, LACQUER_RED[3])
            p.put(15, y, LACQUER_RED[2])
    out["high_striker_post"] = px16(post)

    def red(p):
        bs.planks(RED_PAINT, 26002, boards=4)(p)
        for i in range(1, 15):
            for x, y in ((i, 1), (1, i)):
                p.put(x, y, GOLD[3])
            for x, y in ((i, 14), (14, i)):
                p.put(x, y, GOLD[2])
    out["high_striker_red"] = px16(red)

    def deck(p):
        bs.planks(PLANK, 26003, boards=4)(p)
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
                if d <= 1.6:
                    p.put(x, y, GOLD[4] if (x, y) == (7, 7) else GOLD[3])
                elif d <= 5.6:
                    p.put(x, y, LACQUER_RED[3] if 2.9 < d <= 4.1 else WOOD_WHITE[3])
    out["high_striker_deck"] = px16(deck)

    def rail(lift):
        def paint(p):
            for y in range(16):
                for x in range(16):
                    p.put(x, y, GOLD[min(4, GOLD_COLUMNS[x] + lift)])
        return paint
    out["high_striker_brass"] = px16(rail(0))

    for lit in (False, True):
        glass = [rgb("3a2a08"), rgb("6a4c10"), rgb("9a7020"), rgb("c09634")] if not lit else [rgb("e09a20"), rgb("ffc840"), rgb("fff090"), rgb("ffffff")]

        def lamp(p, glass=glass):
            for y in range(16):
                for x in range(16):
                    d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
                    p.put(x, y, glass[3 if d < 2.5 else 2 if d < 5 else 1 if d < 7 else 0])
            for x, y in ((4, 4), (5, 4), (4, 5)):
                p.put(x, y, glass[3])
        out["high_striker_lamp_on" if lit else "high_striker_lamp_off"] = px16(lamp)

    def puck(p):
        for y in range(16):
            band = WOOD_WHITE if 6 <= y < 10 else LACQUER_RED
            k = 4 if y in (0, 6) else 1 if y in (9, 15) else 3 if band is WOOD_WHITE else 2
            for x in range(16):
                p.put(x, y, band[k])
        for x in range(1, 15):
            p.put(x, 1, LACQUER_RED[4])
    out["high_striker_puck"] = px16(puck)

    def pad(p):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
                if d < 1.6:
                    c = LACQUER_RED[4] if (x, y) == (7, 7) else LACQUER_RED[3]
                else:
                    c = RUBBER[2] if int(d / 1.75) % 2 == 0 else YELLOW[2]
                p.put(x, y, c)
    out["high_striker_pad"] = px16(pad)

    for part, band in SCALE_BANDS.items():
        def scale(p, band=band, part=part):
            bs.planks(WHITE_PAINT, 26010 + part, boards=1, vertical=True, joint=False)(p)
            for y in range(16):
                for x in range(4, 12):
                    p.put(x, y, band[2] if x == 4 else band[0] if x == 11 else band[1])
                p.put(3, y, GOLD[3])
                p.put(12, y, GOLD[2])
            for y in (0, 8):
                for x in range(4, 12):
                    p.put(x, y, band[0])
            for y in (4, 12):
                for x in range(4, 7):
                    p.put(x, y, band[0])
        out[f"high_striker_scale_{part}"] = px16(scale)

    for lit in (False, True):
        def bell(p, lit=lit):
            rail(1 if lit else 0)(p)
            for x in range(16):
                p.put(x, 15, BRASS[1])
            if lit:
                for x, y in ((3, 3), (4, 3), (3, 4)):
                    p.put(x, y, rgb("fffbe8"))
        out["high_striker_bell_lit" if lit else "high_striker_bell"] = px16(bell)

    def sign(p):
        bs.planks(RED_PAINT, 26030, boards=4)(p)
        for cx in (3, 12):
            for x, y in ((cx, 1), (cx - 1, 2), (cx, 2), (cx + 1, 2), (cx, 3)):
                p.put(x, y, GOLD[4] if (x, y) == (cx, 1) else GOLD[3])
        for x, y in ((7, 1), (8, 1), (6, 2), (7, 2), (8, 2), (9, 2), (7, 3), (8, 3)):
            p.put(x, y, GOLD[4] if y == 1 else GOLD[3])
        for y in (4, 7):
            for x in range(16):
                p.put(x, y, GOLD[3])
        for x, y in ((7, 5), (8, 5), (7, 6), (8, 6)):
            p.put(x, y, GOLD[4] if (x, y) == (7, 5) else GOLD[3])
        for x in (3, 12):
            p.put(x, 5, GOLD[3])
            p.put(x, 6, GOLD[2])
    out["high_striker_sign"] = px16(sign)
    return out


def ring_toss_textures():
    """Ring Toss in vanilla's manner (fair_pixels): a crate of planks banded in red and white with a gold star, glass
    bottles lit down one side like vanilla's glass, and a ring striped red, yellow and blue."""
    out = {}

    def crate(p):
        bs.planks(PLANK, 26101, boards=4)(p)
        for y in range(5, 11):
            for x in range(16):
                p.put(x, y, (LACQUER_RED[3] if y < 7 else LACQUER_RED[2]) if (x // 2) % 2 == 0 else (WOOD_WHITE[3] if y < 7 else WOOD_WHITE[2]))
        for x, y in ((7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (7, 9), (8, 9)):
            p.put(x, y, GOLD[4] if y == 6 else GOLD[3])
    out["ring_toss_crate"] = px16(crate)
    out["ring_toss_crate_top"] = px16(bs.planks(PLANK, 26102, boards=4))

    # Glass across a bottle: lit down its left, a bright streak, darkest at its right edge.
    columns = [2, 3, 4, 3, 3, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 0]
    for name, glass in GLASS.items():
        def bottle(p, glass=glass):
            for y in range(16):
                for x in range(16):
                    p.put(x, y, glass[columns[x]])
        out[f"ring_toss_bottle_{name}"] = px16(bottle)

    blue = [rgb("0a2a6a"), rgb("1a4aa8"), rgb("3a74dc"), rgb("80b0f8")]

    def ring(p):
        for y in range(16):
            for x in range(16):
                colours = (LACQUER_RED, YELLOW, blue)[((x + y) // 4) % 3]
                p.put(x, y, colours[3] if y % 8 == 0 else colours[2])
    out["ring_toss_ring"] = px16(ring)
    return out


# Felt colours: from shadow to highlight.
FELT = {
    "pumpkin": [rgb("6a2a04"), rgb("a84a0a"), rgb("dc7014"), rgb("f39434"), rgb("ffbc6c")],
    "ghost": [rgb("9aa0a8"), rgb("c4c8ce"), rgb("e2e4e8"), rgb("f4f5f7"), rgb("ffffff")],
    "bat": [rgb("0c0a10"), rgb("1a1622"), rgb("2a2434"), rgb("3c344a"), rgb("564c66")],
    "cat": [rgb("08080a"), rgb("141418"), rgb("222228"), rgb("32323a"), rgb("4a4a54")],
    "squirrel": [rgb("4a1a08"), rgb("7a2e10"), rgb("a8461c"), rgb("c8642c"), rgb("e08a4c")],
    "werewolf": [rgb("2a180c"), rgb("442814"), rgb("603a1e"), rgb("7a4e2a"), rgb("96683e")],
    "green": [rgb("1a3a10"), rgb("2c5a1c"), rgb("40782a"), rgb("5a963c"), rgb("80b85a")],
    "stem": [rgb("2a2410"), rgb("44401c"), rgb("5e5a28"), rgb("787434")],
    "tan": [rgb("8a6a4a"), rgb("a8865e"), rgb("c4a276"), rgb("dcbe92")],
    "cream": [rgb("b8a888"), rgb("d4c6a4"), rgb("ece0c0"), rgb("faf2dc")],
    "acorn": [rgb("3a2410"), rgb("5a3a1c"), rgb("7c5428"), rgb("9c703a")],
    "cap": [rgb("2a2414"), rgb("4a3e24"), rgb("6a5a36"), rgb("8a7848")],
    "orange": [rgb("7a3404"), rgb("c0580c"), rgb("ec7c1c"), rgb("ffa448")],
    "blue": [rgb("0a2050"), rgb("1a3a8a"), rgb("2e5ac4"), rgb("6a90f0")],
}
THREAD_DARK = rgb("1a1210")
BLACK_FELT = [rgb("040404"), rgb("101012"), rgb("202024")]
PINK = [rgb("a04a5a"), rgb("d8788a"), rgb("f4a8b6")]


def plush_textures():
    """The plushes in vanilla's manner (fair_pixels): felt drawn as wool, seams as stitched lines, and faces of square
    pixel eyes and mouths as on vanilla's mobs."""
    out = {}
    white = (255, 255, 255)

    # The pumpkin: orange felt, its ribs stitched; a jack-o'-lantern face cut from black felt, as a carved pumpkin's.
    pumpkin = FELT["pumpkin"][1:4]
    for name in ("pumpkin_plush", "jumbo_pumpkin_plush"):
        def body(p):
            fp.wool(p, pumpkin, 26200)
            for column in (4, 8, 12):
                fp.seam(p, pumpkin, column=column)
        out[name] = px16(body)

        def face(p, jumbo=name.startswith("jumbo")):
            fp.wool(p, pumpkin, 26210)
            for x0 in (3, 10):
                for x, y in ((x0 + 1, 4), (x0, 5), (x0 + 1, 5), (x0 + 2, 5), (x0, 6), (x0 + 1, 6), (x0 + 2, 6)):
                    p.put(x, y, BLACK_FELT[1])
            p.put(7, 8, BLACK_FELT[1])
            p.put(8, 8, BLACK_FELT[1])
            grin = [(x, 10) for x in range(3, 13)] + [(x, 11) for x in range(4, 12)] + [(x, 12) for x in range(6, 10)]
            if jumbo:
                grin += [(2, 9), (13, 9)]
            for x, y in grin:
                p.put(x, y, BLACK_FELT[1])
            for x in (5, 10):
                p.put(x, 10, pumpkin[2])
            p.put(7, 12, pumpkin[2])
            p.put(8, 12, pumpkin[2])
        out[f"{name}_face"] = px16(face)
    stem = FELT["stem"][1:]
    out["plush_stem"] = px16(lambda p: (fp.wool(p, stem, 26220), fp.seam(p, stem, row=8)))
    leaf = FELT["green"][1:4]

    def plush_leaf(p):
        fp.wool(p, leaf, 26221)
        fp.seam(p, leaf, row=8)
        for k in range(1, 5):
            for x, y in ((4 + k, 8 - k), (4 + k, 8 + k), (9 + k, 8 - k), (9 + k, 8 + k)):
                p.put(x, y, leaf[0])
    out["plush_leaf"] = px16(plush_leaf)
    blue = FELT["blue"][1:4]

    def rosette(p):
        fp.wool(p, blue, 26222)
        for y in range(16):
            for x in range(16):
                d = max(abs(x + 0.5 - 8), abs(y + 0.5 - 8))
                if d <= 2:
                    p.put(x, y, LACQUER_RED[3] if d <= 1 else LACQUER_RED[2])
                elif d <= 4:
                    p.put(x, y, GOLD[3] if (x + y) % 2 else GOLD[2])
                elif d <= 6 and (x + y) % 2 == 0:
                    p.put(x, y, blue[2])
    out["plush_rosette"] = px16(rosette)

    # The ghost: white felt, square eyes, rosy cheeks, a little "o" of a mouth.
    ghost = FELT["ghost"][2:]
    out["ghost_plush"] = px16(lambda p: (fp.wool(p, ghost, 26230), fp.seam(p, ghost, row=14)))

    def ghost_face(p):
        fp.wool(p, ghost, 26231)
        for x0 in (5, 9):
            box(p, x0, 6, x0 + 1, 7, BLACK_FELT[1])
        for x in (3, 4, 11, 12):
            p.put(x, 9, PINK[1])
        for x in (7, 8):
            p.put(x, 10, BLACK_FELT[1])
            p.put(x, 11, PINK[0])
    out["ghost_plush_face"] = px16(ghost_face)

    # The bat: near-black felt; big white eyes, two little fangs; wings of purple-black felt, scalloped at their foot.
    bat = FELT["bat"][1:4]
    out["bat_plush"] = px16(lambda p: (fp.wool(p, bat, 26240), fp.seam(p, bat, column=8)))

    def bat_face(p):
        fp.wool(p, bat, 26241)
        for x0 in (3, 9):
            box(p, x0, 5, x0 + 3, 8, rgb("f2f0ea"))
            box(p, x0 + 1, 6, x0 + 2, 7, BLACK_FELT[0])
            p.put(x0 + 1, 6, white)
        for x in (6, 7, 8, 9):
            p.put(x, 11, rgb("c86a8a"))
        for x in (6, 9):
            p.put(x, 12, rgb("f4f0e6"))
    out["bat_plush_face"] = px16(bat_face)
    wing = FELT["bat"][2:] + [rgb("6a4c7e")]

    def bat_wing(p):
        fp.wool(p, wing[:3], 26242)
        for x0, x1 in ((1, 5), (1, 10), (1, 15)):
            for k in range(13):
                x = x0 + round((x1 - x0) * k / 12)
                p.put(x, 1 + k, wing[3])
        for x in range(16):
            depth = (0, 1, 2, 2, 1, 0)[x % 6] if x % 6 else 0
            for y in range(16 - 3 + depth, 16):
                p.clear(x, y)
    out["bat_plush_wing"] = px16(bat_wing)

    # The black cat: black felt, green-gold eyes with slit pupils, a pink nose, white whiskers; an orange bow.
    cat = [rgb("1c1c22"), rgb("222228"), rgb("2c2c34")]
    out["black_cat_plush"] = px16(lambda p: (fp.wool(p, cat, 26250), fp.seam(p, cat, column=8)))

    def cat_face(p):
        fp.wool(p, cat, 26251)
        for x0 in (4, 9):
            for x in range(x0, x0 + 3):
                for y in (6, 7):
                    p.put(x, y, BLACK_FELT[0] if x == x0 + 1 else rgb("d8e04a") if y == 6 else rgb("b4c030"))
        p.put(7, 9, PINK[2])
        p.put(8, 9, PINK[1])
        for x, y in ((7, 10), (8, 10), (6, 11), (9, 11)):
            p.put(x, y, PINK[0])
        for y in (9, 11):
            for x in (1, 2, 3, 12, 13, 14):
                p.put(x, y, rgb("d8d8d8"))
    out["black_cat_plush_face"] = px16(cat_face)
    orange = FELT["orange"][1:4]

    def bow(p):
        fp.wool(p, orange, 26252)
        for y in range(1, 16, 4):
            for x in range(1 + (y // 4) % 2 * 2, 16, 4):
                p.put(x, y, rgb("fff0d8"))
    out["plush_bow"] = px16(bow)

    # The squirrel: rust felt with a cream muzzle, a bushy tail with a pale tip; an acorn held in front.
    squirrel = FELT["squirrel"][1:4]
    cream = FELT["cream"][1:4]
    out["squirrel_plush"] = px16(lambda p: (fp.wool(p, squirrel, 26260), fp.seam(p, squirrel, column=8)))

    def squirrel_face(p):
        fp.wool(p, squirrel, 26261)
        box(p, 4, 9, 11, 14, cream[1])
        box(p, 5, 9, 10, 9, cream[2])
        for x0 in (4, 10):
            box(p, x0, 5, x0 + 1, 6, BLACK_FELT[0])
            p.put(x0, 5, white)
        box(p, 7, 9, 8, 10, PINK[1])
        p.put(7, 9, PINK[2])
        for x, y in ((6, 12), (7, 11), (8, 11), (9, 12)):
            p.put(x, y, THREAD_DARK)
    out["squirrel_plush_face"] = px16(squirrel_face)

    def tail(p):
        bs.streaks(FELT["squirrel"][1:5], 26262, vertical=True, spread=0.7)(p)
        for x in range(16):
            tip = 3 if x % 4 in (1, 2) else 2
            for y in range(tip):
                p.put(x, y, cream[2] if y == 0 else cream[1])
    out["squirrel_plush_tail"] = px16(tail)
    acorn = FELT["acorn"][1:4]

    def plush_acorn(p):
        fp.wool(p, acorn, 26263)
        box(p, 3, 3, 4, 6, FELT["acorn"][3])
    out["plush_acorn"] = px16(plush_acorn)
    cap = FELT["cap"][1:4]

    def acorn_cap(p):
        fp.wool(p, cap, 26264)
        for y in range(1, 16, 3):
            for x in range((y // 3) % 2 * 2, 16, 4):
                p.put(x, y, FELT["cap"][0])
                p.put(x + 1, y, cap[2])
    out["plush_acorn_cap"] = px16(acorn_cap)

    # The werewolf: brown felt, a tan muzzle with a black nose and two felt fangs, yellow eyes under cross brows.
    wolf = FELT["werewolf"][1:4]
    tan = FELT["tan"][1:4]
    out["werewolf_plush"] = px16(lambda p: (fp.wool(p, wolf, 26270), fp.seam(p, wolf, column=8)))

    def wolf_face(p):
        fp.wool(p, wolf, 26271)
        for x0, brow in ((4, ((3, 5), (4, 5), (5, 6))), (10, ((12, 5), (11, 5), (10, 6)))):
            box(p, x0, 7, x0 + 1, 8, rgb("f0c428"))
            p.put(x0 + (1 if x0 < 8 else 0), 8, BLACK_FELT[0])
            for x, y in brow:
                p.put(x, y, BLACK_FELT[1])
        for x0 in (2, 12):
            box(p, x0, 12, x0 + 1, 13, tan[1])
    out["werewolf_plush_face"] = px16(wolf_face)
    out["werewolf_plush_muzzle"] = px16(lambda p: fp.wool(p, tan, 26276))

    def snout(p):
        fp.wool(p, tan, 26273)
        box(p, 5, 2, 10, 5, BLACK_FELT[1])
        box(p, 6, 2, 7, 2, rgb("6a6a70"))
        for y in (6, 7, 8, 9):
            p.put(8, y, THREAD_DARK)
        for x, y in ((4, 11), (5, 10), (6, 10), (7, 10), (9, 10), (10, 10), (11, 11)):
            p.put(x, y, THREAD_DARK)
        for x0 in (5, 10):
            box(p, x0, 11, x0, 13, rgb("f4f0e6"))
    out["werewolf_plush_snout"] = px16(snout)
    return out


def items():
    """The midway's items as vanilla item icons (fair_pixels.sprite): 16 by 16 pixel art, outlined, lit from the top
    left."""
    out = {}
    colours = {"o": rgb("3a2008"), "G": GOLD[4], "g": GOLD[3], "d": GOLD[1], "R": LACQUER_RED[3], "r": LACQUER_RED[2],
               "W": WOOD_WHITE[4], "w": WOOD_WHITE[2], "y": rgb("f0c428"), "Y": rgb("fff090"), "b": LACQUER_RED[1],
               "k": LACQUER_RED[0]}
    out["high_striker"] = sprite([
        "................",
        "......oooo......",
        ".....oGGgdo.....",
        ".....ogggdo.....",
        "......oooo......",
        ".......Rr.......",
        "....Y..Ww..Y....",
        ".......Rr.......",
        "....y..Ww..y....",
        ".......Rr.......",
        "....y..Ww..y....",
        ".......Rr.......",
        ".......Ww.......",
        "....kRRRRRrk....",
        "...kbbbbbbbbk...",
        "................",
    ], colours)

    def mallet(p):
        for k in range(8):
            p.put(9 - k, 7 + k, PLANK[3])
            p.put(10 - k, 7 + k, PLANK[1])
        for y in range(1, 7):
            for x in range(8, 15):
                edge = y in (1, 6) or x in (8, 14)
                band = x in (9, 13)
                p.put(x, y, rgb("3a1008") if edge else (LACQUER_RED[3] if y < 4 else LACQUER_RED[2]) if band
                      else (WOOD_WHITE[4] if y < 4 else WOOD_WHITE[2]))
    out["carnival_mallet"] = px16(mallet)

    def toss_ring(p):
        for y in range(16):
            for x in range(16):
                dx, dy = (x + 0.5 - 8) / 7.0, (y + 0.5 - 8) / 4.4
                d = dx * dx + dy * dy
                if 0.45 <= d <= 1.0:
                    colours = LACQUER_RED if (x // 3) % 2 == 0 else YELLOW
                    p.put(x, y, colours[3] if y < 8 else colours[1])
    out["toss_ring"] = px16(toss_ring)
    return out


def midway_textures():
    out = {}
    for name, img in {**striker_textures(), **ring_toss_textures(), **plush_textures()}.items():
        out[("block", name)] = img
    for name, img in items().items():
        out[("item", name)] = img
    return out
