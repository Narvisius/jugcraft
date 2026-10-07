"""Original textures for Halloween nights (requires Pillow).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code from fixed seeds; no Mojang texture is
read, traced or recoloured. Block and item textures are 16x16. Entity textures follow their models' box layouts
(client/WispModel.java, client/HorsemanModel.java): a box at (u, v) of size w x h x d takes the top at (u + d, v),
the bottom at (u + d + w, v), and the sides in a row at v + d: (u, d wide), the front (u + d, w wide), (u + d + w,
d wide), the back (u + 2d + w, w wide). The cloak is drawn on the 64 x 32 humanoid armour layout.
"""
import math

from PIL import Image

from crop_textures import Canvas, rgb, outline
from halloween_textures import PLANK, IRON, shade
import block_style as bs

COAT = [rgb("101014"), rgb("17171d"), rgb("1f1f27"), rgb("292933")]
MANE = [rgb("1c1424"), rgb("2a1f36"), rgb("3a2c4a")]
HOOF = [rgb("2e2c2a"), rgb("44403c")]
CLOAK = [rgb("140f1a"), rgb("1d1626"), rgb("271e33"), rgb("322742")]
JACKET = [rgb("231812"), rgb("2f2119"), rgb("3b2a1f")]
GLOVE = [rgb("3a2414"), rgb("4e3220")]
PUMPKIN = [rgb("9a4a0c"), rgb("c4620e"), rgb("e07c18"), rgb("f09a30")]
GLOW = [rgb("ffd23c"), rgb("ffb21e"), rgb("ff8a10")]
EMBER = [rgb("ff5a10"), rgb("ff8a20"), rgb("ffc040")]
EYE = [rgb("ff2a10"), rgb("ff6a30")]
WISP = [(170, 255, 230, 255), (120, 240, 210, 255), (230, 255, 250, 255)]
HALO = (110, 230, 210, 110)
GLASS = (200, 230, 235, 90)
GLASS_EDGE = (225, 245, 250, 170)


def noise(img, x0, y0, w, h, palette, seed, weights=None):
    """Fills a rectangle with the palette's shades in small clumps, in the manner of the vanilla textures
    (tools/block_style.py)."""
    alpha = {color[:3]: (color[3] if len(color) == 4 else 255) for color in palette}
    surface = bs.surface([color[:3] for color in palette], seed, weights, spread=0.7, size=img.size)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            color = surface(x, y)
            img.putpixel((x, y), color + (alpha[color],))


def box_faces(u, v, w, h, d):
    """The faces of a model box at texture offset (u, v): name -> (x, y, width, height)."""
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint_box(img, u, v, w, h, d, palette, seed, weights=None):
    for i, (x, y, fw, fh) in enumerate(box_faces(u, v, w, h, d).values()):
        noise(img, x, y, fw, fh, palette, seed + i, weights)


# ---------------------------------------------------------------- the wisp

def wisp_entity():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    paint_box(img, 0, 0, 4, 4, 4, WISP, 7301, [2, 3, 1])
    for name, (x, y, w, h) in box_faces(0, 8, 6, 6, 6).items():
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                edge = xx in (x, x + w - 1) or yy in (y, y + h - 1)
                img.putpixel((xx, yy), (HALO[0], HALO[1], HALO[2], 60 if edge else HALO[3]))
    return img


# ---------------------------------------------------------------- the Headless Horseman

HORSEMAN_BOXES = {
    # name: (u, v, w, h, d, palette)
    "body": (0, 0, 10, 10, 22, COAT), "neck": (64, 0, 6, 14, 7, COAT), "head": (90, 0, 6, 6, 12, COAT),
    "mane": (64, 22, 2, 16, 4, MANE), "tail": (76, 22, 3, 14, 4, MANE), "leg": (0, 32, 4, 13, 4, COAT),
    "rider_leg": (16, 32, 4, 12, 4, JACKET), "torso": (32, 32, 8, 12, 4, JACKET), "arm": (56, 42, 4, 12, 4, CLOAK),
    "cloak": (90, 18, 9, 15, 1, CLOAK), "collar": (90, 36, 8, 2, 5, CLOAK), "lantern": (72, 44, 5, 5, 5, PUMPKIN),
}


def horseman_entity():
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for i, (name, (u, v, w, h, d, palette)) in enumerate(HORSEMAN_BOXES.items()):
        paint_box(img, u, v, w, h, d, palette, 7310 + i * 10)
    # Hooves: the bottom three rows of each leg's sides and its underside.
    u, v, w, h, d, _ = HORSEMAN_BOXES["leg"]
    for name, (x, y, fw, fh) in box_faces(u, v, w, h, d).items():
        rows = range(y, y + fh) if name == "down" else range(y + fh - 3, y + fh) if name not in ("up",) else range(0)
        for yy in rows:
            for xx in range(x, x + fw):
                img.putpixel((xx, yy), HOOF[(xx + yy) % 2] + (255,))
    # Gloves at the end of each sleeve; brass buttons down the jacket's front.
    u, v, w, h, d, _ = HORSEMAN_BOXES["arm"]
    for name, (x, y, fw, fh) in box_faces(u, v, w, h, d).items():
        rows = range(y, y + fh) if name == "down" else range(y + fh - 3, y + fh) if name != "up" else range(0)
        for yy in rows:
            for xx in range(x, x + fw):
                img.putpixel((xx, yy), GLOVE[(xx + yy) % 2] + (255,))
    x, y, fw, fh = box_faces(*HORSEMAN_BOXES["torso"][:5])["front"]
    for yy in range(y + 2, y + fh - 1, 3):
        img.putpixel((x + fw // 2, yy), rgb("b08a3a") + (255,))
    # The lantern's face, dark where it is cut.
    x, y, fw, fh = box_faces(*HORSEMAN_BOXES["lantern"][:5])["front"]
    for px, py in ((1, 1), (3, 1), (1, 3), (2, 3), (3, 3)):
        img.putpixel((x + px, y + py), rgb("3a1a04") + (255,))
    # The neck's stump above the collar: charred.
    x, y, fw, fh = box_faces(*HORSEMAN_BOXES["collar"][:5])["up"]
    noise(img, x, y, fw, fh, [rgb("1a0a06"), rgb("2a1208")], 7399)
    return img


def horseman_glow():
    """Only what glows: the horse's eyes, the embers where the head should be, and the lantern's cut face."""
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    x, y, fw, fh = box_faces(*HORSEMAN_BOXES["head"][:5])["front"]
    for px in (0, fw - 1):
        img.putpixel((x + px, y + 1), EYE[0] + (255,))
        img.putpixel((x + px, y + 2), EYE[1] + (255,))
    x, y, fw, fh = box_faces(*HORSEMAN_BOXES["collar"][:5])["up"]
    for yy in range(y + 1, y + fh - 1):
        for xx in range(x + 1, x + fw - 1):
            # Embers glowing hottest in the middle of the stump.
            d = abs(xx - (x + fw / 2 - 0.5)) + abs(yy - (y + fh / 2 - 0.5))
            img.putpixel((xx, yy), EMBER[2 if d < 1.5 else 1 if d < 3 else 0] + (255,))
    x, y, fw, fh = box_faces(*HORSEMAN_BOXES["lantern"][:5])["front"]
    for px, py in ((1, 1), (3, 1), (1, 3), (2, 3), (3, 3)):
        img.putpixel((x + px, y + py), GLOW[0] + (255,))
    return img


def cloak_equipment():
    """The Horseman's Cloak on the 64 x 32 humanoid armour layout: body (16, 16) and both arms (40, 16), 4 deep."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint_box(img, 16, 16, 8, 12, 4, CLOAK, 7410)
    paint_box(img, 40, 16, 4, 12, 4, CLOAK, 7420)
    # A high stiff collar on the body's top edge and a ragged hem.
    x, y, fw, fh = box_faces(16, 16, 8, 12, 4)["front"]
    for xx in range(x, x + fw):
        img.putpixel((xx, y), shade(CLOAK[3], 1.3) + (255,))
    for name in ("front", "back", "left", "right"):
        x, y, fw, fh = box_faces(16, 16, 8, 12, 4)[name]
        for xx in range(x, x + fw, 2):
            img.putpixel((xx, y + fh - 1), (0, 0, 0, 0))
    return img


def cloak_item():
    c = Canvas()
    for y in range(2, 15):
        half = 3 + (y - 2) * 0.35
        for x in range(int(8 - half), int(8 + half) + 1):
            c.px(x, y, CLOAK[1 + (x + y) % 3])
    for x in range(5, 11):
        c.px(x, 2, shade(CLOAK[3], 1.4))
    for x in range(int(8 - 7), 16, 2):
        c.img.putpixel((x, 14), (0, 0, 0, 0))
    c.px(8, 4, rgb("b08a3a"))
    outline(c, rgb("08060c"))
    return c.img


# ---------------------------------------------------------------- blocks

def jar_glass():
    """The jar's sides show u 5-11, v 7-16: a rim around that, a gleam down one side; the base is plain glass."""
    img = Image.new("RGBA", (16, 16), GLASS)
    for y in range(7, 16):
        img.putpixel((5, y), GLASS_EDGE)
        img.putpixel((10, y), GLASS_EDGE)
    for x in range(5, 11):
        img.putpixel((x, 7), GLASS_EDGE)
        img.putpixel((x, 15), GLASS_EDGE)
    for y in range(9, 13):
        img.putpixel((6, y), (255, 255, 255, 190))
    return img


def jar_light():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            img.putpixel((x, y), WISP[2] if d < 3 else WISP[0] if d < 6 else WISP[1])
    return img


def jar_lid():
    c = Canvas()
    noise(c.img, 0, 0, 16, 16, [rgb("8c6a44"), rgb("a8845a"), rgb("b89468")], 7430)
    return c.img


def jar_string():
    c = Canvas()
    for y in range(16):
        c.px(7, y, rgb("d8d4c8"))
        c.px(8, y, rgb("b8b4a8"))
    return c.img


def trebuchet_wood():
    c = Canvas()
    bs.planks(PLANK, 7440)(c)
    return c.img


def trebuchet_beam():
    c = Canvas()
    bs.planks([shade(p, 0.8) for p in PLANK], 7441, vertical=True)(c)
    for y in (0, 15):
        for x in range(16):
            c.px(x, y, IRON[1])
    return c.img


def trebuchet_iron():
    """The counterweight: an iron-strapped box of stones."""
    c = Canvas()
    bs.cobble([rgb("4a4640"), rgb("6a6660"), rgb("807a72"), rgb("948e84")], 7442, count=6)(c)  # the stones
    for i in range(16):
        for band in (0, 7, 8, 15):
            c.px(i, band, IRON[2] if i % 4 else IRON[3])
            c.px(band, i, IRON[1])
    return c.img


def trebuchet_rope():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                img.putpixel((x, y), rgb("c8b080" if (x + y) % 8 == 0 else "a89060") + (255,))
    return img


def lantern_pumpkin(face=False):
    c = Canvas()
    for y in range(16):
        for x in range(16):
            rib = x % 4 == 0
            c.px(x, y, PUMPKIN[0] if rib else PUMPKIN[2] if x % 4 == 2 else PUMPKIN[1])
    if face:
        for x, y in LANTERN_FACE:
            c.px(x, y, rgb("3a1a04"))
    return c.img


# The cut face, in the part of the texture the lantern's front shows (u 4-12, v 9-16): two eyes and a grin.
LANTERN_FACE = ((6, 11), (7, 11), (6, 12), (9, 11), (10, 11), (10, 12), (6, 13), (10, 13), (7, 14), (8, 14), (9, 14))


def lantern_glow():
    """Only the light through the cut face, drawn on its own glowing element over the front (u 5-11, v 10-15)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x, y in LANTERN_FACE:
        img.putpixel((x, y), GLOW[(x + y) % 3] + (255,))
    return img


def lantern_iron():
    c = Canvas()
    noise(c.img, 0, 0, 16, 16, IRON[:3], 7450)
    return c.img


def jar_item():
    c = Canvas()
    for y in range(5, 15):
        for x in range(4, 12):
            c.img.putpixel((x, y), GLASS_EDGE if x in (4, 11) or y == 14 else GLASS)
    for y in range(8, 12):
        for x in range(6, 10):
            c.px(x, y, (230, 255, 250) if (x, y) in ((7, 9), (8, 10)) else (150, 245, 220))
    for x in range(3, 13):
        c.px(x, 4, rgb("a8845a"))
        c.px(x, 3, rgb("8c6a44"))
    outline(c, rgb("2a4a48"))
    return c.img


def lantern_item():
    c = Canvas()
    for y in range(6, 15):
        for x in range(3, 13):
            c.px(x, y, PUMPKIN[1 + (x % 3 == 0)])
    for x, y in ((5, 8), (10, 8), (6, 12), (7, 12), (8, 12), (9, 12)):
        c.px(x, y, GLOW[0])
    for x in range(3, 13):
        c.px(x, 5, IRON[1])
    for y in range(1, 5):
        c.px(5 if y > 1 else 6, y, IRON[2])
        c.px(10 if y > 1 else 9, y, IRON[2])
    c.px(7, 1, IRON[2])
    c.px(8, 1, IRON[2])
    outline(c, rgb("3a1a04"))
    return c.img


def throw_marker():
    """The landing marker: a stake in the left two columns, a chequered orange flag in the top half on the right."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        img.putpixel((0, y), rgb("6a4a2a") + (255,))
        img.putpixel((1, y), rgb("8a6a40") + (255,))
    for y in range(8):
        for x in range(4, 16):
            img.putpixel((x, y), (rgb("f08a20") if (x // 2 + y // 2) % 2 else rgb("1a1a1a")) + (255,))
    return img


def night_textures():
    """(kind, name) -> image for every Halloween-nights texture."""
    return {
        ("entity", "will_o_wisp"): wisp_entity(),
        ("entity", "headless_horseman"): horseman_entity(),
        ("entity", "headless_horseman_glow"): horseman_glow(),
        ("entity", "throw_marker"): throw_marker(),
        ("entity", "equipment/humanoid/horseman_cloak"): cloak_equipment(),
        ("block", "wisp_jar_glass"): jar_glass(),
        ("block", "wisp_jar_light"): jar_light(),
        ("block", "wisp_jar_lid"): jar_lid(),
        ("block", "wisp_jar_string"): jar_string(),
        ("block", "trebuchet_wood"): trebuchet_wood(),
        ("block", "trebuchet_beam"): trebuchet_beam(),
        ("block", "trebuchet_iron"): trebuchet_iron(),
        ("block", "trebuchet_rope"): trebuchet_rope(),
        ("block", "horseman_lantern_pumpkin"): lantern_pumpkin(),
        ("block", "horseman_lantern_face"): lantern_pumpkin(face=True),
        ("block", "horseman_lantern_glow"): lantern_glow(),
        ("block", "horseman_lantern_iron"): lantern_iron(),
        ("item", "wisp_in_a_jar"): jar_item(),
        ("item", "horseman_lantern"): lantern_item(),
        ("item", "horseman_cloak"): cloak_item(),
    }


if __name__ == "__main__":
    # Writes only these textures (the full set comes from generate_textures.py).
    from pathlib import Path
    root = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures"
    for (kind, name), image in night_textures().items():
        path = root / kind / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, optimize=True)
