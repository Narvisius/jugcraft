"""Original textures for the pumpkin regatta and trick-or-treating (requires Pillow).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code from fixed seeds; no Mojang
texture is read, traced or recoloured. Surfaces vary in small clumps in the manner of the vanilla blocks
(tools/block_style.py), never a random colour at every pixel. Block and item textures are 16x16; the pumpkin boats' cut flesh
(entity/pumpkin_boat_flesh, stretched over the inside of a boat) is 64x64, and the view from under a
ghost sheet (misc/ghost_sheet, stretched over the screen) is 256x128, and the sheet as worn (entity/ghost_sheet,
drawn by GhostSheetLayer) is 128x64.
"""
import math
import random

from PIL import Image

from crop_textures import Canvas, rgb, outline
from halloween_textures import GIANT, CARAMEL, PAPER, STRAW, shade
import block_style as bs

FLESH = [rgb("d8741c"), rgb("e8902e"), rgb("f2a844"), rgb("f8c066"), rgb("fcd890")]
SEEDS = [rgb("efe2c0"), rgb("fbf6e6")]
WATER = [rgb("2c4f9a"), rgb("3a64b4"), rgb("5482cc")]
FELT = [rgb("17141c"), rgb("221d2a"), rgb("2d2738"), rgb("3a3348")]
BAND = [rgb("4a1c62"), rgb("62287e"), rgb("7c3a9a")]
SHEET = [rgb("c8cad0"), rgb("dcdee2"), rgb("eeeff1"), rgb("fafafa")]
RED = [rgb("8a1414"), rgb("b02020"), rgb("cc3a30")]
WHITE = [rgb("c8c8c4"), rgb("e4e4e0"), rgb("f6f6f2")]
POLE = [rgb("4a3420"), rgb("6a4c30"), rgb("8a6844")]
WRAPPER = [rgb("6a1010"), rgb("9a1c18"), rgb("c42c22"), rgb("e04a34")]
FOIL = [rgb("9c9ca4"), rgb("c8c8d0"), rgb("eeeef4")]
CHOCOLATE = [rgb("3e2210"), rgb("5a3418")]
HAT_BAND = [rgb("6e1a14"), rgb("942820"), rgb("b43a2c")]


def fabric(colors, seed, weave=False):
    """A 16x16 cloth: its middle shades in small clumps, with a faint cross weave."""
    tones = colors[:3]
    cloth = bs.surface(tones, seed, [1, 3, 2][:len(tones)], spread=0.6)
    c = Canvas()
    for y in range(16):
        for x in range(16):
            i = tones.index(cloth(x, y))
            if weave and (x + y) % 4 == 0:
                i = max(0, i - 1)
            c.px(x, y, colors[min(i, len(colors) - 1)])
    return c.img


def straw_weave(seed):
    """Plaited straw: diagonal bands of light and dark gold."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            band = ((x + y) // 2) % 2
            c.px(x, y, STRAW[3 if band else 2])
            if (x - y) % 8 == 0:
                c.px(x, y, STRAW[1])
    return c.img


def ghost_face():
    """The front of the ghost sheet: two black eye holes and a small round mouth."""
    img = fabric(SHEET, 7101)
    for cx, cy in ((5, 6), (10, 6)):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 / 2.2 + (y - cy) ** 2 / 4.0 <= 1.0:
                    img.putpixel((x, y), (16, 16, 20, 255))
    for y in range(16):
        for x in range(16):
            if (x - 7.5) ** 2 + (y - 11) ** 2 <= 1.6:
                img.putpixel((x, y), (30, 30, 36, 255))
    return img


# The worn sheet's boxes, as in GhostSheetLayer.java: (u, v, width, height, depth) on a 128x64 texture, laid out like a
# vanilla model box (top and bottom in a row, then the sides west, north, east, south).
SHEET_BOXES = {"hood": (0, 0, 10, 10, 10), "crown": (40, 0, 8, 1, 8), "drape": (0, 20, 10, 13, 6), "skirt": (40, 20, 11, 9, 8),
               "right_sleeve": (80, 0, 5, 13, 5), "left_sleeve": (100, 0, 5, 13, 5),
               "right_thigh": (80, 20, 5, 9, 5), "left_thigh": (100, 20, 5, 9, 5)}


def worn_sheet():
    """The ghost sheet as worn (entity/ghost_sheet, 128x64): plain cloth with soft folds running down the sides, two
    eye holes and a round mouth on the front of the hood."""
    grain = bs.grain(128, 64, 7111)
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for name, (u, v, w, h, d) in SHEET_BOXES.items():
        for y in range(v, v + d + h):
            for x in range(u, u + 2 * (d + w)):
                top_row = y < v + d
                if top_row and not (u + d <= x < u + d + 2 * w):
                    continue
                v = grain(x, y)
                shade_index = 3 if v > 0.68 else 1 if v < 0.32 else 2
                if not top_row and (x - u) % 4 == 3:
                    shade_index -= 1  # a fold down the cloth
                if not top_row and y >= v + d + h - 1 and name in ("skirt", "drape"):
                    shade_index -= 1  # the hem
                img.putpixel((x, y), SHEET[max(0, min(3, shade_index))] + (255,))
    # The hood's front (north) face runs from (u + d, v + d), w by h; the eyes sit level with the wearer's eyes.
    u, v, w, h, d = SHEET_BOXES["hood"]
    fx, fy = u + d, v + d
    for y in range(fy, fy + h):
        for x in range(fx, fx + w):
            px, py = x - fx + 0.5, y - fy + 0.5
            eye = min((px - cx) ** 2 / 2.0 + (py - 4.6) ** 2 / 3.6 for cx in (2.9, 7.1))
            mouth = (px - 5.0) ** 2 / 1.1 + (py - 8.3) ** 2 / 0.9
            if eye <= 1.0:
                img.putpixel((x, y), (16, 16, 20, 255))
            elif mouth <= 1.0:
                img.putpixel((x, y), (30, 30, 36, 255))
    return img


def flag_cloth():
    """A chequered racing flag: 2-pixel squares."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            dark = ((x // 2) + (y // 2)) % 2
            c.px(x, y, rgb("1c1c20") if dark else rgb("f2f2ee"))
    return c.img


def flag_pole():
    c = Canvas()
    bs.planks(POLE, 7102, vertical=True, joint=False)(c)
    return c.img


def buoy_paint(colors, seed):
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, colors[:2], seed, [1, 5], spread=0.6)
    for y in range(2):
        for x in range(16):
            c.px(x, y, colors[2])
    return c.img


def boat_flesh():
    """The inside of a hollowed giant pumpkin, 64x64: fibrous orange flesh with a few seeds left in it."""
    rng = random.Random(7103)
    grain = bs.grain(64, 64, 7104)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 255))
    for y in range(64):
        for x in range(64):
            fibre = math.sin(x * 0.45 + math.sin(y * 0.2) * 2.0) * 0.5 + 0.5
            i = 1 + int(fibre * 2.5) + (1 if grain(x, y) > 0.72 else 0)
            img.putpixel((x, y), FLESH[min(i, len(FLESH) - 1)] + (255,))
    for _ in range(18):
        x, y = rng.randrange(2, 61), rng.randrange(2, 61)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            img.putpixel((x + dx, y + dy), SEEDS[rng.randrange(2)] + (255,))
    return img


def ghost_overlay():
    """Looking out from under a ghost sheet, 256x128: the sheet's white, see-through but for two clear eye holes."""
    img = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    for y in range(128):
        for x in range(256):
            hole = min(((x - cx) / 34.0) ** 2 + ((y - 56) / 30.0) ** 2 for cx in (82, 174))
            if hole <= 1.0:
                alpha = 0
            elif hole <= 1.5:
                alpha = int(150 * (hole - 1.0) / 0.5)
            else:
                alpha = 150 + (12 if (x // 3 + y // 5) % 7 == 0 else 0)
            img.putpixel((x, y), (244, 244, 240, alpha))
    return img


def boat_item(skin, carved):
    """A hollowed pumpkin afloat, seen from the side: a ribbed shell cut flat across the top, the barge with its
    carved face glowing, the racer smaller and plain."""
    c = Canvas()
    half = 7.0 if carved else 5.5
    top = 4 if carved else 6
    for y in range(top, 14):
        for x in range(16):
            if (x - 7.5) ** 2 / half ** 2 + (y - top - 1.5) ** 2 / 64.0 <= 1.0:
                rib = abs(x - 7.5) in (1.5, 4.5) or abs(x - 7.5) == 6.5
                c.px(x, y, skin[1] if rib else skin[3 if x < 8 else 2])
    for x in range(16):
        if c.get(x, top):
            c.px(x, top, FLESH[3])
    if carved:
        glow = rgb("ffd75a")
        for x, y in ((4, 7), (5, 7), (5, 6), (10, 7), (11, 7), (10, 6), (4, 10), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11), (11, 10)):
            c.px(x, y, glow)
    outline(c, skin[0])
    for x in range(16):
        for y in (13, 14):
            c.px(x, y, WATER[1 if (x + y) % 3 else 2])
    for x in range(0, 16, 3):
        c.px(x, 15, WATER[0])
    return c.img


def candy_bag_item():
    """An orange paper treat bag with a jack o'lantern face and a string handle."""
    c = Canvas()
    for y in range(5, 15):
        for x in range(3, 13):
            c.px(x, y, rgb("e07a1c") if (x + y) % 5 else rgb("c86a14"))
    for x in range(3, 13):
        c.px(x, 5, rgb("f09a3a"))
    for x, y in ((5, 8), (6, 8), (10, 8), (9, 8), (5, 11), (6, 12), (7, 12), (8, 12), (9, 12), (10, 11), (7, 11), (8, 11)):
        c.px(x, y, rgb("2a1406"))
    for i in range(5):
        c.px(5 + i * 0.5, 4 - i * 0.6, PAPER[0])
        c.px(10 - i * 0.5, 4 - i * 0.6, PAPER[0])
    c.px(7, 1, PAPER[0])
    c.px(8, 1, PAPER[0])
    outline(c, rgb("6a3008"))
    return c.img


def candy_bar_item():
    """A king-size bar: red wrapper with a gold band, torn open at one end on chocolate."""
    c = Canvas()
    for i in range(12):
        for w in range(4):
            x, y = 2 + i, 11 - i * 0.6 + w
            if i < 3:
                color = CHOCOLATE[1] if w in (1, 2) else CHOCOLATE[0]
            elif i == 3:
                color = FOIL[2 if w % 2 else 1]
            else:
                color = rgb("e8b830") if w == 1 else WRAPPER[2 if w == 2 else 1]
            c.px(x, y, color)
    outline(c, WRAPPER[0])
    return c.img


def regatta_textures():
    """(kind, name) -> image for every regatta and trick-or-treat texture."""
    return {
        ("item", "pumpkin_barge"): boat_item(GIANT[1:], True),
        ("item", "pumpkin_racer"): boat_item(GIANT[1:], False),
        ("item", "candy_bag"): candy_bag_item(),
        ("item", "king_size_candy_bar"): candy_bar_item(),
        ("item", "witch_hat"): fabric(FELT, 7104, weave=True),
        ("item", "witch_hat_band"): fabric(BAND, 7105),
        ("item", "ghost_sheet"): fabric(SHEET, 7106, weave=True),
        ("item", "ghost_sheet_face"): ghost_face(),
        ("item", "scarecrow_hat"): straw_weave(7107),
        ("item", "scarecrow_hat_band"): fabric(HAT_BAND, 7108),
        ("block", "regatta_flag_cloth"): flag_cloth(),
        ("block", "regatta_flag_pole"): flag_pole(),
        ("block", "regatta_buoy_red"): buoy_paint(RED, 7109),
        ("block", "regatta_buoy_white"): buoy_paint(WHITE, 7110),
        ("entity", "pumpkin_boat_flesh"): boat_flesh(),
        ("misc", "ghost_sheet"): ghost_overlay(),
        ("entity", "ghost_sheet"): worn_sheet(),
    }


if __name__ == "__main__":
    # Writes only these textures (the full set comes from generate_textures.py).
    from pathlib import Path
    root = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures"
    for (kind, name), image in regatta_textures().items():
        path = root / kind / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, optimize=True)
