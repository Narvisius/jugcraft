"""Original textures for knitting (fall additions 15) (requires Pillow): the Spinning Wheel's honey-coloured and dark
woods and the fluffy wool on its distaff; the ball of yarn, the needles, the beanie, socks and sweater as items (drawn
near-white where they take their dyed colour) and the sweaters' motifs (stripes, a pumpkin, a bat, an autumn leaf); and
the knit as worn, on the 64 by 32 humanoid armour layout: a beanie on the head's top half, a sweater on the body and arms,
socks on the lower legs, each with ribbed edges, and each motif on its own layer over the sweater.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured.
"""

from PIL import Image

from crop_textures import Canvas, rgb, outline
from halloween_textures import shade
import block_style as bs
from night_textures import box_faces

HONEY = [rgb("8a5a2c"), rgb("a06c36"), rgb("b98042"), rgb("cc9450")]
DARK = [rgb("3a2414"), rgb("4a2e1a"), rgb("5c3a22"), rgb("6c462a")]
# Near-white knit, light enough that a dye shows true: stitches in four tones.
KNIT = [(214, 214, 214), (228, 228, 228), (240, 240, 240), (252, 252, 252)]
RIB = (200, 200, 200)
CREAM = rgb("f4ecd8")
PUMPKIN = [rgb("c85a12"), rgb("e8761c"), rgb("f49434")]
STEM = rgb("3c6a24")
BAT = rgb("18141e")
LEAF = [rgb("a8261a"), rgb("d4461c"), rgb("e8742a")]


def plank(palette, seed):
    c = Canvas()
    bs.planks(palette, seed)(c)
    return c.img


def wool():
    """Fluffy carded wool, near-white for tinting."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wool = bs.surface(KNIT, 15015, [1, 1, 2, 2], spread=0.7)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), wool(x, y) + (255,))
    return img


def stitch(x, y):
    """A knit stitch's tone at (x, y): columns of little V's."""
    return KNIT[3 if (x + y) % 2 == 0 else (1 if x % 2 else 2)] if y % 2 == 0 else KNIT[2 if x % 2 == 0 else 0]


def yarn_item():
    """A ball of yarn, its wound strands showing, a loose end trailing."""
    c = Canvas()
    for y in range(3, 14):
        for x in range(3, 14):
            dx, dy = x - 8, y - 8.5
            if dx * dx + dy * dy <= 29:
                band = (x * 2 + y) // 3 % 3
                c.px(x, y, KNIT[1 + band] if (x + y) % 4 else KNIT[0])
    for x, y in ((13, 12), (14, 13), (14, 14), (15, 15)):
        c.px(x, y, KNIT[2])
    outline(c, (150, 150, 150))
    return c.img


def needles_item():
    """Two wooden knitting needles crossed, a few rows of cream knitting on them."""
    c = Canvas()
    for i in range(12):
        c.px(2 + i, 13 - i, HONEY[2] if i % 3 else HONEY[3])
        c.px(3 + i, 2 + i * 0.95, HONEY[1])
    c.px(14, 1, rgb("c0c0c8"))
    c.px(15, 14, rgb("c0c0c8"))
    for y in range(5, 9):
        for x in range(5, 11):
            if abs(x - 8) + abs(y - 7) < 5:
                c.px(x, y, CREAM if (x + y) % 2 else shade(CREAM, 0.88))
    outline(c, rgb("3a2414"))
    return c.img


def beanie_item():
    """A knit beanie: ribbed brim, a rounded crown, a pom-pom on top."""
    c = Canvas()
    for y in range(5, 12):
        half = 5 if y >= 7 else 3 + (y - 5)
        for x in range(8 - half, 8 + half):
            c.px(x, y, stitch(x, y))
    for x in range(2, 14):
        for y in range(11, 14):
            c.px(x, y, RIB if x % 2 else KNIT[3])
    for x, y in ((7, 2), (8, 2), (6, 3), (7, 3), (8, 3), (9, 3), (7, 4), (8, 4)):
        c.px(x, y, KNIT[3] if (x + y) % 2 else KNIT[1])
    outline(c, (140, 140, 140))
    return c.img


def socks_item():
    """A pair of wool socks, ribbed at the top, side by side."""
    c = Canvas()
    for ox in (1, 8):
        for y in range(1, 11):
            for x in range(ox + 1, ox + 5):
                c.px(x, y, RIB if y < 3 and x % 2 else stitch(x, y))
        for y in range(10, 14):
            for x in range(ox + 1, ox + 7):
                c.px(x, y, stitch(x, y))
    outline(c, (140, 140, 140))
    return c.img


def sweater_item():
    """A knit sweater laid flat: body, sleeves, ribbed neck, cuffs and hem."""
    c = Canvas()
    for y in range(3, 15):
        for x in range(4, 12):
            c.px(x, y, RIB if y >= 13 and x % 2 else stitch(x, y))
    for y in range(3, 12):
        for x in list(range(1, 4)) + list(range(12, 15)):
            c.px(x, y, RIB if y >= 10 and x % 2 else stitch(x, y))
    for x in range(6, 10):
        c.px(x, 3, RIB)
    c.img.putpixel((7, 3), (0, 0, 0, 0))
    c.img.putpixel((8, 3), (0, 0, 0, 0))
    outline(c, (140, 140, 140))
    return c.img


def motif(kind, width=16, height=16, ox=0, oy=0, img=None):
    """A sweater's motif, drawn into `img` (a new 16x16 item layer if none) with its box at (ox, oy)."""
    if img is None:
        img = Image.new("RGBA", (width, height), (0, 0, 0, 0))

    def px(x, y, color):
        if 0 <= x < width and 0 <= y < height:
            img.putpixel((ox + x, oy + y), color + (255,))
    if kind == "pumpkin":
        for y in range(2, 7):
            for x in range(1, 7):
                if (x - 3.5) ** 2 / 9 + (y - 4.4) ** 2 / 5.5 <= 1:
                    px(x, y, PUMPKIN[1 if x in (2, 5) else 2 if x in (3, 4) else 0])
        px(3, 1, STEM)
        px(4, 1, STEM)
    elif kind == "bat":
        for x, y in ((0, 2), (1, 2), (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (6, 2), (7, 2), (2, 4), (3, 4), (4, 4), (5, 4),
                     (3, 2), (4, 2), (3, 5), (4, 5), (1, 4), (6, 4)):
            px(x, y, BAT)
    elif kind == "leaf":
        for x, y in ((3, 0), (2, 1), (3, 1), (4, 1), (0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2), (1, 3), (2, 3), (3, 3), (4, 3),
                     (5, 3), (2, 4), (3, 4), (4, 4), (3, 5), (3, 6)):
            px(x, y, LEAF[(x + y) % 3] if (x, y) not in ((3, 5), (3, 6)) else rgb("5a2e14"))
    return img


def motif_item(kind):
    """The motif as an item layer over the sweater item: stripes across it, or the motif on its chest."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    if kind == "stripes":
        sweater = sweater_item()
        for y in (5, 6, 9, 10):
            for x in range(16):
                if sweater.getpixel((x, y))[3] and sweater.getpixel((x, y))[:3] != (140, 140, 140):
                    img.putpixel((x, y), CREAM + (255,))
        return img
    return motif(kind, 8, 7, 4, 5, img)


def knit_equipment():
    """The knit as worn (64 x 32): beanie on the top half of the head, sweater on body and arms, socks on the lower legs."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # The beanie: the head's top, and the top four rows of its sides (the last a ribbed brim).
    for name, (x, y, w, h) in box_faces(0, 0, 8, 8, 8).items():
        if name == "down":
            continue
        rows = h if name == "up" else 4
        for yy in range(y, y + rows):
            for xx in range(x, x + w):
                brim = name != "up" and yy == y + rows - 1
                img.putpixel((xx, yy), (RIB if brim and xx % 2 else stitch(xx, yy)) + (255,))
    # The sweater: body and arms, ribbed at the hem and cuffs.
    for u, v, w, h, d in ((16, 16, 8, 12, 4), (40, 16, 4, 12, 4)):
        for name, (x, y, fw, fh) in box_faces(u, v, w, h, d).items():
            for yy in range(y, y + fh):
                for xx in range(x, x + fw):
                    ribbed = name not in ("up", "down") and yy >= y + fh - 2
                    img.putpixel((xx, yy), (RIB if ribbed and xx % 2 else stitch(xx, yy)) + (255,))
    # The socks: the legs' lower five rows and their soles, ribbed at the top.
    for name, (x, y, fw, fh) in box_faces(0, 16, 4, 12, 4).items():
        if name == "up":
            continue
        top = y if name == "down" else y + fh - 5
        for yy in range(top, y + fh):
            for xx in range(x, x + fw):
                ribbed = name != "down" and yy == top
                img.putpixel((xx, yy), (RIB if ribbed and xx % 2 else stitch(xx, yy)) + (255,))
    return img


def motif_equipment(kind):
    """A sweater's motif as worn: stripes round the body and arms, or the motif on the chest (the body's front)."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    if kind == "stripes":
        for u, v, w, h, d in ((16, 16, 8, 12, 4), (40, 16, 4, 12, 4)):
            for name, (x, y, fw, fh) in box_faces(u, v, w, h, d).items():
                if name in ("up", "down"):
                    continue
                for yy in (y + 2, y + 3, y + 6, y + 7):
                    for xx in range(x, x + fw):
                        img.putpixel((xx, yy), CREAM + (255,))
        return img
    x, y, fw, fh = box_faces(16, 16, 8, 12, 4)["front"]
    return motif(kind, 8, 7, x, y + 2, img)


def knitting_textures():
    out = {("block", "spinning_wheel_wood"): plank(HONEY, 15101), ("block", "spinning_wheel_dark"): plank(DARK, 15102),
           ("block", "spinning_wheel_wool"): wool(),
           ("item", "yarn"): yarn_item(), ("item", "knitting_needles"): needles_item(), ("item", "knit_beanie"): beanie_item(),
           ("item", "wool_socks"): socks_item(), ("item", "knit_sweater"): sweater_item(),
           ("entity", "equipment/humanoid/knit"): knit_equipment()}
    for kind, asset in (("stripes", "knit_striped"), ("pumpkin", "knit_pumpkin"), ("bat", "knit_bat"), ("leaf", "knit_leaf")):
        out[("item", f"{kind}_motif")] = motif_item(kind)
        out[("entity", f"equipment/humanoid/{asset}")] = motif_equipment(kind)
    return out
