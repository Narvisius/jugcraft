"""Original 16x16 textures for the Agriculture branch's Halloween harvest (requires Pillow).

Called from crop_textures.crop_textures(). As with the rest of the branch's art, every pixel is drawn here
by code from fixed seeds; no Mojang texture is read, traced or recoloured. Surfaces are painted in the manner of the
vanilla blocks with tools/block_style.py: variation in small clumps, never a random colour at every pixel; wood as
planks and straw as streaks. The giant pumpkin's sides and top
are drawn as one big picture per size (32x32 or 48x48) and cut into 16x16 tiles, one per block face; each
picture is mirror-symmetric, so the pumpkin looks the same from every side. Block textures are opaque except
where a plant, decoration or cut-out shape needs see-through pixels.
"""
import math
import random

from PIL import Image

from agriculture import GIANT_PUMPKIN, DYE_COLORS, giant_tile
from crop_textures import Canvas, rgb, outline, seeds_item, LEAF
from festival_textures import gourd_side, gourd_top, STALK, BARK
from kitchen_textures import bowl_item
import block_style as bs

GIANT = [rgb("7c3e0e"), rgb("a45616"), rgb("c86e1e"), rgb("df8a2c"), rgb("eda444"), rgb("f6c06a")]
GIANT_STEM = [rgb("3d3a16"), rgb("5a5422"), rgb("7a7232"), rgb("9a9046")]
WHITE_PUMPKIN = [rgb("9c9a88"), rgb("c4c2b0"), rgb("dddbca"), rgb("eeede2"), rgb("fbfaf3")]
JARRAHDALE = [rgb("4a5a5c"), rgb("62757a"), rgb("7c9196"), rgb("98abae"), rgb("b6c6c6")]
CINDERELLA = [rgb("6e1c0a"), rgb("9a2c10"), rgb("c04218"), rgb("dc5e24"), rgb("ee8240")]
# Pumpkin Night's heirlooms (tools/decor20.py): Red Kuri, a smooth teardrop of deep red-orange; Kabocha, squat and dark
# green, flecked and striped pale.
RED_KURI = [rgb("5a1406"), rgb("86220a"), rgb("b0360e"), rgb("d04e16"), rgb("e8702a")]
KABOCHA = [rgb("10241a"), rgb("1a3622"), rgb("274c2e"), rgb("37643c"), rgb("4e7e50")]
KABOCHA_FLECK = [rgb("7e9a6c"), rgb("9cb48a"), rgb("b8c8a2")]
TEMPURA = [rgb("9a6a1e"), rgb("c48e2e"), rgb("e0b04a"), rgb("f2d27a"), rgb("fbeab0")]
BOTTLE = [rgb("4c6a2c"), rgb("67873a"), rgb("86a64e"), rgb("a8c070"), rgb("cad898")]
BOTTLE_SPOT = [rgb("d8dcae"), rgb("eef0cc")]
DRIED = [rgb("6a4a22"), rgb("8c6630"), rgb("ad8442"), rgb("c9a35c"), rgb("e0c080")]
STRAW = [rgb("8a6a24"), rgb("b08c34"), rgb("d0ae4c"), rgb("e8cc6e"), rgb("f6e29a")]
DENIM = [rgb("26324a"), rgb("33445f"), rgb("435778"), rgb("566c90")]
PATCH = [rgb("7a3a22"), rgb("9a5030")]
PLANK = [rgb("5a3c1e"), rgb("75502a"), rgb("8f6636"), rgb("a87c44")]
IRON = [rgb("3c3f44"), rgb("5c6168"), rgb("868c94"), rgb("b4bac2")]
DIAL = [rgb("e8e2d0"), rgb("faf6ea")]
HOLE = rgb("1e140a")
CARAMEL = [rgb("6a3a0c"), rgb("945412"), rgb("bc7420"), rgb("dc9a3a"), rgb("f0bc64")]
APPLE = [rgb("6a0c10"), rgb("9a161a"), rgb("c42a26")]
POP = [rgb("d9c79a"), rgb("f3e8c8"), rgb("fffaf0")]
PAPER = [rgb("a88a5c"), rgb("c8aa78"), rgb("e0c898"), rgb("f0e0bc")]
LEATHER = [rgb("4a2a14"), rgb("6c4020"), rgb("8c5a30")]
CORK = [rgb("8c6a44"), rgb("b08c5c")]
GUTS = [rgb("b0520e"), rgb("d8741c"), rgb("f09a36"), rgb("f8c070")]
SEED_PALE = [rgb("d8c8a0"), rgb("ece0c0"), rgb("fbf6e6")]
# Flint corn kernels: deep red, purple, blue-black, gold, cream and orange.
FLINT = [rgb("8a1e1e"), rgb("5a2a6a"), rgb("2a2a44"), rgb("e0a82a"), rgb("efe2c0"), rgb("c8641e")]
# Dye colours as vanilla names them, for the scarecrow's flannel (approximations of each dye's colour).
DYE = {"white": "f2f2ee", "orange": "f08020", "magenta": "c44eb8", "light_blue": "3aa8d4", "yellow": "f0cc38",
       "lime": "7cc020", "pink": "ee8aa8", "gray": "4a5054", "light_gray": "9c9c96", "cyan": "18949a",
       "purple": "8434b0", "blue": "3a44a8", "brown": "7e5232", "green": "5c7a18", "red": "aa2e26", "black": "24242a"}
MUM_COLORS = {
    "yellow_mum": [rgb("a87a0a"), rgb("d8a818"), rgb("f4cc30"), rgb("fde678")],
    "orange_mum": [rgb("9a420a"), rgb("cc6414"), rgb("ec8a26"), rgb("f8b25a")],
    "red_mum": [rgb("5e0c12"), rgb("8a1820"), rgb("b42c30"), rgb("d85a54")],
    "purple_mum": [rgb("3c1446"), rgb("5e2468"), rgb("84408c"), rgb("ac6cb0")],
    "marigold": [rgb("a8520a"), rgb("e07a10"), rgb("f8a020"), rgb("ffc84a")],
}


def shade(color, factor):
    return tuple(max(0, min(255, int(round(c * factor)))) for c in color)


# ---------------------------------------------------------------- the giant pumpkin

def giant_side_picture(size):
    """One side of a giant pumpkin `size` blocks wide: deep ribs every 8 pixels, swelling between them,
    darker towards the top, bottom and the two edges where it curves away. Mirror-symmetric."""
    width = size * 16
    grain = bs.grain(width, width, 4100 + size)
    img = Image.new("RGBA", (width, width))
    for y in range(width):
        for x in range(width):
            swell = math.cos(2 * math.pi * (x + 0.5) / 8)  # -1 at the middle of a lobe, +1 at a rib
            v = (y + 0.5) / width
            u = (x + 0.5) / width
            light = 3.4 - 1.6 * max(0.0, swell) ** 2 + 0.5 * (1 - swell) / 2
            light -= 1.8 * max(0.0, abs(v - 0.45) - 0.3) * 4 + 1.6 * max(0.0, abs(u - 0.5) - 0.38) * 5
            v = grain(min(x, width - 1 - x), y)    # mirrored, so the side stays symmetric
            if v < 0.3:
                light -= 0.45
            elif v > 0.7:
                light += 0.4
            img.putpixel((x, y), GIANT[max(0, min(5, int(round(light))))] + (255,))
    return img


def giant_top_picture(size):
    """The top: ribs running into a thick, corky stem in the middle, darker out at the rim."""
    width = size * 16
    middle = width / 2
    grain = bs.grain(width, width, 4200 + size)
    img = Image.new("RGBA", (width, width))
    for y in range(width):
        for x in range(width):
            dx, dy = x + 0.5 - middle, y + 0.5 - middle
            dist = math.hypot(dx, dy) / middle
            angle = math.atan2(dy, dx)
            rib = math.cos(angle * size * 6)
            light = 4.2 - 2.4 * dist - (0.9 if rib > 0.75 and dist > 0.15 else 0)
            if grain(x, y) < 0.3:
                light -= 0.45
            color = GIANT[max(0, min(5, int(round(light))))]
            stem_radius = 2.5 + size
            if math.hypot(dx, dy) < stem_radius:
                color = GIANT_STEM[3 if dx + dy < -1 else 2 if dx + dy < 2 else 1]
                if math.hypot(dx, dy) > stem_radius - 1:
                    color = GIANT_STEM[0]
            img.putpixel((x, y), color + (255,))
    return img


def giant_bottom():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, GIANT[1:3], 4300, [1, 4])
    return c.img


def giant_textures():
    out = {("block", "giant_pumpkin_bottom"): giant_bottom()}
    for size in range(2, GIANT_PUMPKIN["max_size"] + 1):
        side, top = giant_side_picture(size), giant_top_picture(size)
        for a in range(size):
            for b in range(size):
                out[("block", giant_tile("side", size, a, b))] = side.crop((a * 16, b * 16, a * 16 + 16, b * 16 + 16))
                out[("block", giant_tile("top", size, a, b))] = top.crop((a * 16, b * 16, a * 16 + 16, b * 16 + 16))
    return out


# ---------------------------------------------------------------- decorations

def wood_grain(c, palette, seed, vertical=True, x0=0, y0=0, x1=15, y1=15):
    rng = random.Random(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            line = x if vertical else y
            tone = 2 if line % 4 else 1
            if rng.random() < 0.12:
                tone += rng.choice((-1, 1))
            c.px(x, y, palette[max(0, min(len(palette) - 1, tone))])


def scale_side():
    """Planks with an iron band along the top of the platform (rows 12-15 show on the platform's sides)."""
    c = Canvas()
    bs.planks(PLANK, 5100)(c)
    for x in range(16):
        c.px(x, 12, IRON[2])
        c.px(x, 15, IRON[1])
    for x in (1, 14):
        c.px(x, 13, IRON[3])
    return c.img


def scale_top():
    c = Canvas()
    bs.planks(PLANK, 5200)(c)
    for i in range(16):
        for edge in (0, 15):
            c.px(i, edge, IRON[1])
            c.px(edge, i, IRON[1])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        c.px(x, y, IRON[3])
    return c.img


def scale_dial():
    """The dial in columns 5-10, rows 2-11 (where the post shows it): a white face, ticks and a red needle."""
    c = Canvas()
    bs.planks(PLANK, 5300, vertical=True)(c)
    for y in range(2, 12):
        for x in range(5, 11):
            c.px(x, y, IRON[1])
    for y in range(3, 9):
        for x in range(6, 10):
            c.px(x, y, DIAL[1] if (x + y) % 5 else DIAL[0])
    for x, y in ((6, 3), (9, 3), (6, 8), (9, 8)):
        c.px(x, y, IRON[2])
    c.px(7, 6, rgb("b02020"))
    c.px(8, 5, rgb("b02020"))
    c.px(8, 4, rgb("d03030"))
    c.px(7, 7, IRON[0])
    for x in range(6, 10):
        c.px(x, 10, IRON[2])
    return c.img


def straw():
    c = Canvas()
    bs.streaks(STRAW, 5400, spread=0.9)(c)
    return c.img


def post():
    c = Canvas()
    bs.streaks(BARK[1:], 5500, along=6.0)(c)
    return c.img


def trousers():
    """Patched denim, stuffed: the legs show the whole texture's width."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, DENIM[2 if (x + y) % 3 else 1])  # the twill
    for y in range(8, 12):
        for x in range(3, 7):
            c.px(x, y, PATCH[0])
    for x in range(3, 7):
        c.px(x, 8, PATCH[1])
    for y in (0, 15):
        for x in range(16):
            c.px(x, y, DENIM[0])
    return c.img


def flannel(color):
    """A flannel check in one dye colour: darker bands every 4 pixels both ways, a thin light line, buttons."""
    base = rgb(DYE[color])
    dark, darker, light = shade(base, 0.72), shade(base, 0.52), shade(base, 1.18)
    c = Canvas()
    for y in range(16):
        for x in range(16):
            band_x, band_y = x % 6 in (0, 1), y % 6 in (0, 1)
            col = darker if band_x and band_y else dark if band_x or band_y else base
            if x % 6 == 4 or y % 6 == 4:
                col = light
            c.px(x, y, col)
    for y in (3, 7, 11):
        c.px(8, y, rgb("e8e0c8"))
    return c.img


def stook(top):
    """Dry corn stalks standing round: wide at the ground (lower) or drawn in and tied with a band (upper)."""
    c = Canvas()
    for i in range(9):
        x_ground = 1 + i * 1.75
        for y in range(16):
            t = y / 15
            if top:
                # From a tie at row 9 out to splayed tips at the top.
                x = 8 + (x_ground - 8) * (0.3 + 0.5 * (1 - t)) if y < 9 else 8 + (x_ground - 8) * (0.25 + 0.15 * (t - 0.6))
            else:
                x = 8 + (x_ground - 8) * (0.45 + 0.55 * t)
            c.px(x, y, STRAW[1 + (i % 3)])
    if top:
        for x in range(5, 11):
            c.px(x, 9, rgb("8a7a5a"))
            c.px(x, 10, rgb("6a5a40"))
        for x, y in ((3, 1), (12, 0), (6, 0), (10, 2)):
            c.px(x, y, STRAW[4])
    return c.img


def ear_of(c, x, top, length, seed, husk=True):
    """A hanging ear of flint corn: kernels of one colour, every third kernel another, with its husk pulled up above
    it."""
    rng = random.Random(seed)
    main, accent = rng.sample(FLINT, 2)
    for i in range(length):
        y = top + i
        half = 1.5 if 1 <= i < length - 1 else 1.0
        for dx in (-1, 0, 1):
            if abs(dx) <= half:
                kernel = accent if (i + dx) % 3 == 0 else main
                c.px(x + dx, y, kernel if dx < 1 else shade(kernel, 0.8))
    if husk:
        for i in range(4):
            c.px(x - 1 - i * 0.4, top - 1 - i, rgb("c8b47c"))
            c.px(x + 1 + i * 0.4, top - 1 - i, rgb("a89458"))
            c.px(x, top - 1 - i, rgb("dccb98"))


def corn_bundle():
    c = Canvas()
    for x, top, seed in ((5, 7, 1), (8, 8, 2), (11, 7, 3)):
        ear_of(c, x, top, 8, 5800 + seed)
    for x in range(4, 13):
        c.px(x, 3, rgb("7a6a48"))
    c.px(8, 2, rgb("7a6a48"))
    c.px(8, 1, rgb("5a4a30"))
    return c.img


def birdhouse_side(front):
    """A dried gourd's skin, mottled; the front has a round door with a perch peg under it (columns 4-11, rows 4-10)."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, DRIED[1:4], 5900 + front, [1, 4, 1])
    if front:
        for y in range(5, 9):
            for x in range(6, 10):
                if (x - 7.5) ** 2 + (y - 6.5) ** 2 <= 3.2:
                    c.px(x, y, HOLE)
        c.px(7, 9, DRIED[0])
        c.px(8, 9, DRIED[0])
        c.px(7, 10, STALK[1])
        c.px(8, 10, STALK[2])
    return c.img


def birdhouse_top():
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c.px(x, y, DRIED[3 if d < 3 else 2 if d < 6 else 1])
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        c.px(x, y, STALK[1])
    return c.img


def birdhouse_string():
    c = Canvas()
    for y in range(16):
        c.px(7, y, rgb("d8c8a0"))
        c.px(8, y, rgb("b8a880"))
    return c.img


def mum_bush(colors, seed):
    """A garden mum: a mound of small leaves topped with round, many-petalled blooms."""
    c = Canvas()
    leaves = bs.surface(LEAF[1:4], seed)
    for y in range(9, 16):
        for x in range(2, 14):
            if ((x - 7.5) / 6.2) ** 2 + ((y - 15.5) / 6.5) ** 2 <= 1:
                c.px(x, y, leaves(x, y))
    for x in (5, 8, 11):
        for y in range(10, 16):
            c.px(x + (y % 3 == 0), y, LEAF[1])
    for cx, cy, r in ((4.5, 7.5, 2.4), (8, 5, 2.6), (11.5, 7.5, 2.4), (6.5, 10, 2.0), (10, 10.5, 2.0)):
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                d = math.hypot(dx, dy)
                if d <= r:
                    # A round bloom: a dark heart, then petals lit along its upper left.
                    petal = 0 if d < 0.9 else 1 if d < 1.6 else 3 if dx + dy < 0 else 2
                    c.px(cx + dx, cy + dy, colors[petal])
    return c.img


# ---------------------------------------------------------------- items

def giant_seeds_item():
    c = Canvas()
    for x, y in ((2, 3), (8, 2), (5, 8), (10, 9), (3, 12)):
        for dy in range(3):
            for dx in range(4):
                if not (dx in (0, 3) and dy in (0, 2)):
                    c.px(x + dx, y + dy, SEED_PALE[2] if dy == 0 else SEED_PALE[1])
        c.px(x, y + 1, SEED_PALE[0])
        c.px(x + 3, y + 1, SEED_PALE[0])
    outline(c, rgb("8a7a58"))
    return c.img


def guts_item():
    c = Canvas()
    for k in range(3):
        # Three stringy strands, each shaded below and lit along its top every few pixels.
        for x in range(2, 14):
            y = 5.5 + k * 2.5 + math.sin(x * 1.1 + k * 2) * 1.5
            c.px(x, y, GUTS[3] if x % 3 == k else GUTS[1 + k % 2])
            c.px(x, y + 1, GUTS[0])
    for x, y in ((4, 7), (9, 6), (11, 10), (6, 11)):
        c.px(x, y, SEED_PALE[2])
        c.px(x + 1, y, SEED_PALE[1])
    outline(c, rgb("6a2c08"))
    return c.img


def bottle_gourd_item(dried=False, strap=False):
    """A bottle gourd standing up: a round bottom, a narrow neck, a small top bulb."""
    palette = DRIED if dried else BOTTLE
    c = Canvas()
    for y in range(16):
        for x in range(16):
            if y >= 8:
                inside = (x - 7.5) ** 2 / 20 + (y - 11.5) ** 2 / 14 <= 1
            elif y >= 5:
                inside = abs(x - 7.5) <= 1.6
            else:
                inside = (x - 7.5) ** 2 / 5 + (y - 3.5) ** 2 / 3.5 <= 1
            if inside:
                c.px(x, y, palette[3 if x < 7 else 2 if x < 10 else 1])
    if not dried:
        for x, y in ((5, 11), (9, 13), (6, 14), (10, 10)):
            c.px(x, y, BOTTLE_SPOT[1])
    if strap:
        for i in range(14):
            c.px(2 + i * 0.85, 13 - i * 0.9, LEATHER[1])
        c.px(7, 0, CORK[1])
        c.px(8, 0, CORK[0])
    else:
        c.px(8, 0, STALK[1])
        c.px(7, 1, STALK[2])
    outline(c, rgb("3a2a14") if dried else rgb("2a3a14"))
    return c.img


def ornamental_corn_item():
    c = Canvas()
    for i in range(10):
        cx, cy = 4 + i * 0.85, 12 - i * 0.85
        for w in range(-1, 2):
            kernel = FLINT[3] if (i + w) % 3 == 0 else FLINT[0]
            c.px(cx - w * 0.7, cy - w * 0.7, kernel)
            c.px(cx - w * 0.7 + 1, cy - w * 0.7, shade(kernel, 0.85))
    for x, y in ((2, 13), (3, 14), (1, 12), (4, 15), (2, 11), (5, 14), (1, 14), (0, 15), (3, 12)):
        c.px(x, y, rgb("d8c690") if (x + y) % 2 else rgb("b8a46c"))
    outline(c, rgb("3a2a14"))
    return c.img


def ornamental_kernels_item():
    c = Canvas()
    rng = random.Random(6300)
    for x, y in ((4, 6), (8, 5), (11, 8), (6, 10), (9, 11), (3, 11), (12, 12)):
        kernel = FLINT[rng.randrange(len(FLINT))]
        c.px(x, y, kernel)
        c.px(x + 1, y, shade(kernel, 1.15))
        c.px(x, y + 1, shade(kernel, 0.85))
        c.px(x + 1, y + 1, kernel)
        c.px(x, y + 2, shade(kernel, 0.7))
    return c.img


def stalks_item():
    c = Canvas()
    for i, offset in enumerate((-2, 0, 2)):
        for t in range(14):
            c.px(2 + t + offset * 0.3, 14 - t + offset, STRAW[1 + i])
    for x in range(5, 9):
        c.px(x, 9 - (x - 5), rgb("8a7a5a"))
    outline(c, rgb("5a4a20"))
    return c.img


def caramel_item():
    c = Canvas()
    for x0, y0 in ((2, 8), (8, 9), (5, 3)):
        for y in range(y0, y0 + 5):
            for x in range(x0, x0 + 5):
                c.px(x, y, CARAMEL[3 if y == y0 or x == x0 else 2])
        c.px(x0 + 1, y0 + 1, CARAMEL[4])
    outline(c, CARAMEL[0])
    return c.img


def caramel_apple_item():
    c = Canvas()
    for y in range(6, 16):
        for x in range(2, 14):
            if (x - 7.5) ** 2 + (y - 10.5) ** 2 / 0.9 <= 25:
                coated = y > 8 or abs(x - 7.5) > 3.5
                c.px(x, y, (CARAMEL[3] if x < 7 else CARAMEL[2]) if coated else (APPLE[2] if x < 7 else APPLE[1]))
    for x in (4, 7, 10):
        c.px(x, 9, CARAMEL[4])
        c.px(x, 10, CARAMEL[3])
    for y in range(0, 7):
        c.px(8, y, rgb("c8a26a") if y % 2 else rgb("a8824a"))
    outline(c, CARAMEL[0])
    return c.img


def popcorn_ball_item():
    c = Canvas()
    pop = bs.surface(POP, 6400)
    for y in range(3, 14):
        for x in range(3, 14):
            if (x - 8) ** 2 + (y - 8.5) ** 2 <= 26:
                # Popped kernels in clumps, drizzled with caramel in slanting lines.
                c.px(x, y, CARAMEL[3] if (x + y) % 5 == 0 else pop(x, y))
    outline(c, CARAMEL[1])
    return c.img


def ribbon_item(color):
    """A prize rosette: a pleated ring with a pale button in the middle and two tails hanging below."""
    base = rgb(color)
    c = Canvas()
    for y in range(1, 11):
        for x in range(3, 13):
            d = math.hypot(x - 7.5, y - 5.5)
            if d <= 4.6:
                angle = math.atan2(y - 5.5, x - 7.5)
                pleat = int((angle + math.pi) / (2 * math.pi) * 12) % 2
                c.px(x, y, shade(base, 1.15 if pleat else 0.85) if d > 2.2 else rgb("f4e8b0") if d > 1.2 else rgb("d8b84a"))
    for i in range(6):
        c.px(6 - i * 0.3, 10 + i, shade(base, 0.9))
        c.px(7 - i * 0.3, 10 + i, base)
        c.px(9 + i * 0.3, 10 + i, base)
        c.px(10 + i * 0.3, 10 + i, shade(base, 0.8))
    outline(c, shade(base, 0.45))
    return c.img


def stencil_item(cut):
    """A sheet of stencil card; once traced, a jack o'lantern face is cut through it."""
    c = Canvas()
    for y in range(2, 15):
        for x in range(2, 14):
            c.px(x, y, PAPER[2] if (x + y) % 7 else PAPER[1])
    for x in range(2, 14):
        c.px(x, 2, PAPER[3])
    if not cut:
        for i in range(3, 13, 3):
            for j in range(3, 14):
                c.px(i, j, PAPER[1])
    outline(c, PAPER[0])
    if cut:
        # Cut out after the outline, so the holes stay see-through.
        for x, y in ((5, 6), (4, 7), (5, 7), (6, 7), (10, 6), (9, 7), (10, 7), (11, 7), (4, 10), (5, 11), (6, 11), (7, 11), (8, 11),
                     (9, 11), (10, 11), (11, 10), (7, 10), (8, 10)):
            c.img.putpixel((x, y), (0, 0, 0, 0))
    return c.img


def giant_pumpkin_icon():
    """A whole giant pumpkin: wider than it is tall, flattened under its own weight, deep ribs and a thick stem."""
    c = Canvas()
    for y in range(3, 16):
        for x in range(16):
            dx, dy = (x - 7.5) / 8.0, (y - 9.5) / 6.0
            # Flat-bottomed: the lower half bulges out (a superellipse) and sits on its base.
            if (abs(dx) ** 2 + abs(dy) ** 2 if y <= 9 else abs(dx) ** 3.2 + abs(dy) ** 3.2) > 1:
                continue
            # Ribs: darker grooves curving round the body, lighter on the side the light comes from.
            rib = abs(((x - 7.5) / (1.0 - 0.35 * dy * dy)) % 4.0 - 2.0)
            tone = 2 if rib > 1.4 else 3 if rib > 0.6 else 4
            if x < 6 and y < 10 and tone > 2:
                tone += 1
            if y > 13:
                tone -= 1
            c.px(x, y, GIANT[max(0, min(5, tone))])
    for x, y, tone in ((7, 1, 1), (8, 1, 2), (7, 2, 2), (8, 2, 3), (6, 3, 1), (7, 3, 2), (8, 3, 3), (9, 3, 1), (9, 0, 1)):
        c.px(x, y, GIANT_STEM[tone])
    outline(c, rgb("4a2408"))
    return c.img


def scarecrow_icon():
    c = Canvas()
    for y in range(4, 16):
        c.px(7, y, BARK[2])
    for x in range(1, 15):
        c.px(x, 7, rgb(DYE["red"]))
        c.px(x, 8, shade(rgb(DYE["red"]), 0.7))
    for y in range(6, 12):
        for x in range(5, 10):
            c.px(x, y, rgb(DYE["red"]) if (x + y) % 3 else shade(rgb(DYE["red"]), 0.7))
    for y in range(12, 15):
        c.px(5, y, DENIM[2])
        c.px(9, y, DENIM[2])
    for x, y in ((0, 7), (15, 7), (5, 15), (9, 15)):
        c.px(x, y, STRAW[3])
    for y in range(0, 5):
        for x in range(4, 11):
            if (x - 7) ** 2 / 9 + (y - 2.5) ** 2 / 5 <= 1:
                c.px(x, y, GIANT[3] if x < 7 else GIANT[2])
    c.px(6, 2, HOLE)
    c.px(8, 2, HOLE)
    outline(c, rgb("2a1a0e"))
    return c.img


def stook_icon():
    c = Canvas()
    for i in range(7):
        xb = 2 + i * 2
        for y in range(1, 16):
            t = (y - 1) / 14
            c.px(8 + (xb - 8) * t, y, STRAW[1 + i % 3])
    for x in range(6, 11):
        c.px(x, 5, rgb("6a5a40"))
    outline(c, STRAW[0])
    return c.img


def birdhouse_icon():
    img = bottle_gourd_item(dried=True)
    c = Canvas()
    c.img = img
    for x, y in ((7, 11), (8, 11), (7, 12), (8, 12)):
        c.px(x, y, HOLE)
    c.px(7, 14, STALK[1])
    return c.img


def kabocha_side():
    """A kabocha's skin: dark green with faint ribs, flecked and streaked here and there with pale grey-green."""
    img = gourd_side(KABOCHA, 4, 6505, warts=3, clean=True)
    c = Canvas()
    c.img = img
    for x in (2, 6, 10, 14):
        for y in range(16):
            if (y * 3 + x) % 7 in (0, 1):
                c.px(x, y, KABOCHA_FLECK[(y // 2) % 2])  # short pale streaks down the middle of each lobe
    return c.img


def tempura_item():
    """Kabocha Tempura: three crescent slices of kabocha, green rind and orange flesh, in a crisp golden batter, fanned on
    a paper."""
    c = Canvas()
    for y in range(9, 15):
        for x in range(1, 15):
            if abs(x - 7.5) + abs(y - 12) * 1.6 < 8:
                c.px(x, y, PAPER[2 if (x + y) % 5 else 3])
    crisp = bs.grain(16, 16, 6506)
    for i, (cx, cy) in enumerate(((5.0, 9.5), (8.5, 7.5), (11.5, 9.0))):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, (y - cy) * 1.4)
                if 1.6 < d < 4.4 and y <= cy + 1:
                    edge = d > 3.6
                    v = crisp(x, y)
                    batter = (TEMPURA[3] if v > 0.68 else TEMPURA[2]) if v > 0.52 else None
                    if edge:
                        c.px(x, y, KABOCHA[3] if not batter else batter)
                    else:
                        c.px(x, y, batter if batter else rgb("e88a2a") if d > 2.4 else rgb("f2a444"))
        c.px(cx, cy - 3, TEMPURA[4])
    outline(c, TEMPURA[0])
    return c.img


def halloween_textures():
    """(kind, name) -> image for every Halloween-harvest texture."""
    out = giant_textures()
    out.update({
        ("block", "white_pumpkin_side"): gourd_side(WHITE_PUMPKIN, 4, 6500, clean=True),
        ("block", "white_pumpkin_top"): gourd_top(WHITE_PUMPKIN, 8, STALK[1]),
        ("block", "jarrahdale_pumpkin_side"): gourd_side(JARRAHDALE, 3, 6501, clean=True),
        ("block", "jarrahdale_pumpkin_top"): gourd_top(JARRAHDALE, 10, STALK[0]),
        ("block", "cinderella_pumpkin_side"): gourd_side(CINDERELLA, 3, 6502, clean=True),
        ("block", "cinderella_pumpkin_top"): gourd_top(CINDERELLA, 10, STALK[1]),
        ("block", "red_kuri_pumpkin_side"): gourd_side(RED_KURI, 8, 6504, warts=2, clean=True),
        ("block", "red_kuri_pumpkin_top"): gourd_top(RED_KURI, 6, STALK[0]),
        ("block", "kabocha_pumpkin_side"): kabocha_side(),
        ("block", "kabocha_pumpkin_top"): gourd_top(KABOCHA, 8, STALK[1]),
        ("block", "bottle_gourd_side"): gourd_side(BOTTLE, 0, 6503, spot=BOTTLE_SPOT + [BOTTLE_SPOT[0]], clean=True),
        ("block", "bottle_gourd_top"): gourd_top(BOTTLE, 0, STALK[1]),
        ("block", "harvest_scale_side"): scale_side(),
        ("block", "harvest_scale_top"): scale_top(),
        ("block", "harvest_scale_dial"): scale_dial(),
        ("block", "scarecrow_post"): post(),
        ("block", "scarecrow_trousers"): trousers(),
        ("block", "scarecrow_straw"): straw(),
        ("block", "corn_shock_lower"): stook(False),
        ("block", "corn_shock_upper"): stook(True),
        ("block", "ornamental_corn_bundle"): corn_bundle(),
        ("block", "gourd_birdhouse_front"): birdhouse_side(True),
        ("block", "gourd_birdhouse_side"): birdhouse_side(False),
        ("block", "gourd_birdhouse_top"): birdhouse_top(),
        ("block", "gourd_birdhouse_string"): birdhouse_string(),
        ("item", "giant_pumpkin_seeds"): giant_seeds_item(),
        ("item", "pumpkin_guts"): guts_item(),
        ("item", "pumpkin_soup"): bowl_item([rgb("c0601a"), rgb("de8028"), rgb("ee9c3c"), rgb("f6bc62")],
                                            [(SEED_PALE[2], [(5, 6), (9, 7)]), (GUTS[3], [(7, 6), (11, 6)])]),
        ("item", "white_pumpkin_seeds"): seeds_item(SEED_PALE, [(3, 4), (8, 3), (11, 7), (5, 9), (10, 12), (3, 13)], size=(3, 1)),
        ("item", "jarrahdale_pumpkin_seeds"): seeds_item([rgb("c8b88a"), rgb("dcd0a8"), rgb("f0e8cc")],
                                                         [(4, 4), (9, 4), (6, 8), (11, 9), (3, 12), (8, 13)], size=(3, 1)),
        ("item", "cinderella_pumpkin_seeds"): seeds_item([rgb("d0b47c"), rgb("e4cc98"), rgb("f6e6c0")],
                                                         [(3, 5), (8, 3), (12, 6), (5, 10), (10, 11), (6, 13)], size=(3, 1)),
        ("item", "red_kuri_pumpkin_seeds"): seeds_item([rgb("c89a5c"), rgb("dcb478"), rgb("f0d4a0")],
                                                       [(4, 3), (10, 4), (6, 7), (12, 9), (3, 11), (8, 12)], size=(3, 1)),
        ("item", "kabocha_pumpkin_seeds"): seeds_item([rgb("b8a878"), rgb("d0c294"), rgb("e8dcb4")],
                                                      [(3, 4), (9, 3), (12, 8), (6, 9), (10, 13), (3, 12)], size=(3, 1)),
        ("item", "red_kuri_soup"): bowl_item([rgb("8a2a0c"), rgb("b23e12"), rgb("d0581c"), rgb("e87a34")],
                                             [(SEED_PALE[2], [(6, 6), (10, 7)]), (rgb("4a7a32"), [(8, 6), (5, 7)])]),
        ("item", "kabocha_tempura"): tempura_item(),
        ("item", "bottle_gourd_seeds"): seeds_item([rgb("8a7046"), rgb("a88a5a"), rgb("c8aa78")],
                                                   [(3, 4), (8, 4), (12, 7), (5, 9), (10, 12), (3, 13)], size=(2, 2)),
        ("item", "dried_bottle_gourd"): bottle_gourd_item(dried=True),
        ("item", "ornamental_corn"): ornamental_corn_item(),
        ("item", "ornamental_corn_kernels"): ornamental_kernels_item(),
        ("item", "corn_stalks"): stalks_item(),
        ("item", "caramel"): caramel_item(),
        ("item", "caramel_apple"): caramel_apple_item(),
        ("item", "popcorn_ball"): popcorn_ball_item(),
        ("item", "first_prize_ribbon"): ribbon_item("2a4ec0"),
        ("item", "second_prize_ribbon"): ribbon_item("c02a2a"),
        ("item", "third_prize_ribbon"): ribbon_item("e8e8e4"),
        ("item", "blank_stencil"): stencil_item(False),
        ("item", "pumpkin_stencil"): stencil_item(True),
        ("item", "gourd_canteen"): bottle_gourd_item(dried=True, strap=True),
        ("item", "scarecrow"): scarecrow_icon(),
        ("item", "giant_pumpkin"): giant_pumpkin_icon(),
        ("item", "corn_shock"): stook_icon(),
        ("item", "gourd_birdhouse"): birdhouse_icon(),
    })
    for color in DYE_COLORS:
        out[("block", f"scarecrow_shirt_{color}")] = flannel(color)
    for i, (mum, colors) in enumerate(MUM_COLORS.items()):
        out[("block", mum)] = mum_bush(colors, 6600 + i)
    return out

