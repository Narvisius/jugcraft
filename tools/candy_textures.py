"""Original textures for the candy kitchen (fall additions 11) (requires Pillow): the Candy Kettle's polished copper, its
dark inside and its thermometer dial (the candy stages painted round it, from syrup to burnt, matching the needle the
client draws); for the client's renderer the syrup's surface (pale, tinted by its colour as it is drawn) and the needle;
and the items: the Candy Tray, empty and with candy on it, and each candy, drawn pale where it takes its colour from the
batch (a dyed colour, or for candy corn one for each band) with its stick or wrapper in a layer of its own.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. The block textures are 16x16 (the dial is round, see-through at its corners); the items are
see-through round their shapes.
"""
import math

from agriculture import CANDY
from crop_textures import Canvas, rgb, outline
import block_style as bs

COPPER = [rgb("9a5228"), rgb("b86a36"), rgb("d4844a"), rgb("f0a868")]
DARK = [rgb("3a2014"), rgb("4a2a1a"), rgb("5a3420")]
PALE = [rgb("a8a8a8"), rgb("c4c4c4"), rgb("dcdcdc"), rgb("f2f2f2")]
STICK = [rgb("b89058"), rgb("e0c890")]
PAPER = [rgb("d8d4c8"), rgb("f8f6f0")]
# The dial's colour for each stage, syrup to burnt.
STAGE_COLORS = {"syrup": rgb("8ab0c8"), "thread": rgb("f4f0b0"), "soft_ball": rgb("f0d860"), "firm_ball": rgb("f0b840"),
                "hard_ball": rgb("e89030"), "soft_crack": rgb("d07020"), "hard_crack": rgb("c04818"), "caramel": rgb("7a3a14"),
                "burnt": rgb("1a1008")}
SWING = 270.0


def kettle():
    """Polished copper: bright, with a few dents catching the light and a band of rivets."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, COPPER[1:4], 26101, [2, 3, 1])
    for x, y in ((3, 5), (10, 4), (6, 9), (13, 10), (2, 13), (9, 13)):
        c.px(x, y, COPPER[3])  # a dent catching the light
        c.px(x + 1, y, COPPER[0])
    for x in range(1, 16, 3):
        c.px(x, 1, COPPER[3])
        c.px(x, 2, COPPER[0])
    return c.img


def trivet():
    """Black iron, a little rough."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("1e1e22"), rgb("2a2a30"), rgb("36363c")], 26105, [2, 3, 1])
    return c.img


def kettle_inside():
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, DARK, 26103, [2, 3, 2])
    return c.img


def temperature_at(angle):
    """The temperature the needle shows at `angle` (degrees clockwise from straight up)."""
    return CANDY["room"] + (angle + SWING / 2) / SWING * (CANDY["max_temp"] - CANDY["room"])


def stage_at(temperature):
    stage = "syrup"
    for name, (_, start) in CANDY["stages"].items():
        if temperature >= start:
            stage = name
    return stage


def dial():
    """A round candy thermometer dial: a dark bezel, a cream face with the stages painted round it in their colours, a tick
    where each starts, and the needle's hub (the needle itself is drawn by the client)."""
    c = Canvas()
    starts = {start for _, start in CANDY["stages"].values() if start > 0}
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy)
            if r > 7.9:
                continue
            if r > 6.9:
                c.px(x, y, rgb("2a1e14"))
                continue
            angle = math.degrees(math.atan2(dx, -dy))
            if r > 4.6 and abs(angle) <= SWING / 2:
                t = temperature_at(angle)
                c.px(x, y, STAGE_COLORS[stage_at(t)])
                if any(abs(t - s) < 2.4 for s in starts) and r > 5.8:
                    c.px(x, y, rgb("2a1e14"))
            else:
                c.px(x, y, rgb("f4eedc"))
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        c.px(x, y, rgb("2a1e14"))
    return c.img


def syrup():
    """Boiling syrup seen from above: pale, with bubbles (tinted by its colour as it is drawn)."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            wave = math.sin(x * 0.7 + math.sin(y * 0.5) * 2.0)
            c.px(x, y, PALE[3] if wave > 0.6 else PALE[2])
    for x, y in ((3, 3), (10, 2), (6, 7), (12, 8), (2, 11), (8, 12)):
        c.px(x, y, PALE[3])
        c.px(x + 1, y, PALE[1])
    return c.img


def needle():
    """The thermometer's needle: red, darker at its tail (the client draws it as a thin strip)."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            c.px(x, y, rgb("c02018") if y < 12 else rgb("3a1a14"))
    return c.img


# ---------------------------------------------------------------- items

def tray():
    """A tin tray seen from a little above: a rolled rim round a buttery-bright floor."""
    c = Canvas()
    tin = [rgb("8a8e94"), rgb("aeb2b8"), rgb("d0d4da"), rgb("eef0f2")]
    for y in range(5, 13):
        for x in range(1, 15):
            c.px(x, y, tin[2] if (x + y) % 5 else tin[3])
    for x in range(1, 15):
        c.px(x, 4, tin[1])
        c.px(x, 13, tin[0])
    for y in range(5, 13):
        c.px(0, y, tin[1])
        c.px(15, y, tin[0])
    return c.img


def tray_candy():
    """Candy poured on the tray, scored into squares (tinted by its colour)."""
    c = Canvas()
    for y in range(6, 12):
        for x in range(2, 14):
            c.px(x, y, PALE[1] if x in (5, 9) or y == 9 else PALE[3] if (x + y) % 4 == 0 else PALE[2])
    return c.img


def rock_candy():
    """Clusters of crystals grown up a stick (tinted)."""
    c = Canvas()
    for y in range(2, 11):
        half = 3 if 3 <= y <= 9 else 2
        for x in range(8 - half, 8 + half + 1):
            # Crystals two pixels across, each lit along its upper left edge.
            c.px(x, y, PALE[3] if (x + y) % 4 == 0 else PALE[1] if (x + y) % 4 == 3 else PALE[2])
    for x, y in ((4, 4), (11, 6), (4, 8), (12, 9), (8, 1)):
        c.px(x, y, PALE[3])
    outline(c, PALE[0])
    return c.img


def rock_candy_stick():
    c = Canvas()
    for y in range(10, 16):
        c.px(8, y, STICK[0])
        c.px(7, y, STICK[1])
    return c.img


def taffy():
    """A soft pillow of taffy, striped (tinted)."""
    c = Canvas()
    for y in range(5, 11):
        for x in range(4, 12):
            c.px(x, y, PALE[3] if (x - y) % 3 == 0 else PALE[2])
    outline(c, PALE[0])
    return c.img


def taffy_wrapper():
    """Its wax paper, twisted at both ends."""
    c = Canvas()
    for y, w in ((5, 0), (6, 1), (7, 2), (8, 2), (9, 1), (10, 0)):
        for i in range(w + 1):
            c.px(2 - i + 1, y, PAPER[i % 2])
            c.px(13 + i - 1, y, PAPER[i % 2])
    return c.img


def hard_candy():
    """Three round drops (tinted)."""
    c = Canvas()
    for cx, cy in ((5, 6), (10, 5), (8, 10)):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= 5:
                    c.px(x, y, PALE[2] if x + y > cx + cy else PALE[3])
    outline(c, PALE[0])
    return c.img


def hard_candy_shine():
    c = Canvas()
    for x, y in ((4, 5), (9, 4), (7, 9)):
        c.px(x, y, rgb("ffffff"))
    return c.img


def lollipop():
    """A swirled disc (tinted)."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 8, y - 5.5
            r = math.hypot(dx, dy)
            if r <= 4.6:
                swirl = math.sin(r * 1.6 + math.atan2(dy, dx) * 2)
                c.px(x, y, PALE[3] if swirl > 0 else PALE[1])
    outline(c, PALE[0])
    return c.img


def lollipop_stick():
    c = Canvas()
    for y in range(11, 16):
        c.px(8, y, PAPER[1])
        c.px(7, y, PAPER[0])
    return c.img


def fudge():
    """Two cut squares of fudge, one on the other (tinted)."""
    c = Canvas()
    for x0, y0 in ((2, 7), (7, 3)):
        for y in range(y0, y0 + 7):
            for x in range(x0, x0 + 7):
                c.px(x, y, PALE[3] if y == y0 else PALE[1] if x == x0 + 6 else PALE[2])
    outline(c, PALE[0])
    return c.img


def cream_caramel():
    """A soft caramel square (tinted)."""
    c = Canvas()
    for y in range(5, 11):
        for x in range(5, 11):
            c.px(x, y, PALE[3] if y == 5 else PALE[2])
    outline(c, PALE[0])
    return c.img


def cream_caramel_wrapper():
    """Its cellophane, twisted either side."""
    c = Canvas()
    for y in range(6, 10):
        for x in (2, 3, 12, 13):
            c.px(x, y, PAPER[(x + y) % 2])
    return c.img


def toffee():
    """Jagged shards of toffee (tinted)."""
    c = Canvas()
    shards = [[(2, 9), (7, 5), (8, 11)], [(8, 4), (14, 6), (10, 11)], [(5, 11), (11, 12), (7, 14)]]
    for tri in shards:
        (x0, y0), (x1, y1), (x2, y2) = tri
        for y in range(16):
            for x in range(16):
                d = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
                a = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) / d
                b = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) / d
                if a >= 0 and b >= 0 and a + b <= 1:
                    c.px(x, y, PALE[3] if a > 0.6 else PALE[2])
    outline(c, PALE[0])
    return c.img


def burnt_sugar():
    """Black lumps with a bitter brown glint."""
    c = Canvas()
    for cx, cy, r in ((5, 8, 3), (10, 7, 3), (8, 11, 2)):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    # Each lump lit at its upper left.
                    d = (x - cx) + (y - cy)
                    c.px(x, y, rgb("2e1c12" if d < -1 else "1a1008" if d > 1 else "241610"))
    for x, y in ((4, 7), (9, 6)):
        c.px(x, y, rgb("6a3a1a"))
    outline(c, rgb("0a0604"))
    return c.img


def candy_corn_band(band):
    """One band of the candy corn sprite's three pieces (tip, middle or base), pale, for its own colour."""
    c = Canvas()
    rows = [(0, 0), (-1, 0), (-1, 1), (-2, 1), (-2, 2), (-2, 2)]
    for cx, top in ((4, 2), (10, 5), (6, 9)):
        for i, (left, right) in enumerate(rows):
            if (0 if i < 2 else 1 if i < 4 else 2) != band:
                continue
            for x in range(cx + left, cx + right + 1):
                c.px(x, top + i, PALE[3] if x == cx + left else PALE[2])
    if band == 2:
        for cx, top in ((4, 2), (10, 5), (6, 9)):
            for x in range(cx - 2, cx + 3):
                c.px(x, top + 6, PALE[1])
    return c.img


def candy_corn_outline():
    """The dark edge round all three pieces, in a layer of its own so the bands can take any colour."""
    c = Canvas()
    for band in range(3):
        img = candy_corn_band(band)
        for y in range(16):
            for x in range(16):
                if img.getpixel((x, y))[3]:
                    c.px(x, y, (1, 1, 1))
    edge = Canvas()
    for y in range(16):
        for x in range(16):
            if c.empty(x, y) and any(c.get(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.px(x, y, rgb("5a2a06"))
    return edge.img


def candy_textures():
    return {
        ("block", "candy_kettle"): kettle(),
        ("block", "candy_kettle_inside"): kettle_inside(),
        ("block", "candy_trivet"): trivet(),
        ("block", "candy_dial"): dial(),
        ("entity", "candy_syrup"): syrup(),
        ("entity", "candy_needle"): needle(),
        ("item", "candy_tray"): tray(),
        ("item", "candy_tray_candy"): tray_candy(),
        ("item", "rock_candy"): rock_candy(),
        ("item", "rock_candy_stick"): rock_candy_stick(),
        ("item", "salt_water_taffy"): taffy(),
        ("item", "salt_water_taffy_wrapper"): taffy_wrapper(),
        ("item", "hard_candy"): hard_candy(),
        ("item", "hard_candy_shine"): hard_candy_shine(),
        ("item", "lollipop"): lollipop(),
        ("item", "lollipop_stick"): lollipop_stick(),
        ("item", "fudge"): fudge(),
        ("item", "cream_caramel"): cream_caramel(),
        ("item", "cream_caramel_wrapper"): cream_caramel_wrapper(),
        ("item", "toffee"): toffee(),
        ("item", "burnt_sugar"): burnt_sugar(),
        ("item", "candy_corn_tip"): candy_corn_band(0),
        ("item", "candy_corn_middle"): candy_corn_band(1),
        ("item", "candy_corn_base"): candy_corn_band(2),
        ("item", "candy_corn_outline"): candy_corn_outline(),
    }
