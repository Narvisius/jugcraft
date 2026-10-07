"""Original textures for squirrels and acorns (fall addition 24) (requires Pillow): the squirrel, red and grey (32 by 32,
laid out as client/SquirrelModel.java's boxes: soft fur, a cream belly, dark eyes and nose, a bushy tail paler at its
tip); the acorn and roasted acorns as items. Clean, cartoon style: flat fur lit along the top, no random speckle.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured.
"""
from PIL import Image

from agriculture import SQUIRRELS
from crop_textures import rgb

COATS = {"red": [rgb("6e3214"), rgb("8e4620"), rgb("a85a2c"), rgb("c47640")],
         "grey": [rgb("55555a"), rgb("6e6e74"), rgb("8a8a90"), rgb("a8a8ae")]}
BELLY = {"red": rgb("e6d4b0"), "grey": rgb("e2e2dc")}
EYE = rgb("141010")
NUT = [rgb("6a4320"), rgb("8a5a2c"), rgb("a8743c")]
CAP = [rgb("4a3a24"), rgb("66523a"), rgb("80704e")]
ROAST = [rgb("3e2412"), rgb("5c361a"), rgb("7a4c26")]


def squirrel(coat):
    palette = COATS[coat]
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Flat fur: the coat's middle tone, a step lighter in a band along the top of each strip of the layout.
    for y in range(32):
        for x in range(32):
            img.putpixel((x, y), palette[3 if y % 8 == 0 else 2] + (255,))
    # The body's underside (4 x 7 at 11, 0) and the head's underside (4 x 4 at 8, 11): cream.
    for y in range(0, 7):
        for x in range(11, 15):
            img.putpixel((x, y), BELLY[coat] + (255,))
    for y in range(11, 15):
        for x in range(8, 12):
            img.putpixel((x, y), BELLY[coat] + (255,))
    # The head's front (4 x 4 at 4, 15): two dark eyes with a glint, a pale muzzle below.
    for ex in (4, 7):
        img.putpixel((ex, 16), EYE + (255,))
        img.putpixel((ex, 17), EYE + (255,))
    for x in range(5, 7):
        img.putpixel((x, 18), BELLY[coat] + (255,))
    # The snout's front (2 x 2 at 17, 12): a dark nose.
    img.putpixel((17, 12), EYE + (255,))
    img.putpixel((18, 12), EYE + (255,))
    # The tail's tip (4 x 5 x 4 at 12, 19): paler, frosted fur.
    for y in range(19, 28):
        for x in range(12, 28):
            img.putpixel((x, y), palette[3] + (255,) if y >= 25 or y == 19 else palette[2] + (255,))
    return img


def acorn():
    """An acorn: a brown nut under a scaly cap with a little stalk."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(7, 15):
        half = 3.6 if y < 12 else 3.6 - (y - 11) * 1.0
        for x in range(16):
            if abs(x - 7.5) <= half:
                tone = 2 if x < 7 else 1 if x < 10 else 0
                img.putpixel((x, y), NUT[tone] + (255,))
    for y in range(4, 8):
        for x in range(3, 13):
            if abs(x - 7.5) <= 4.4 - (7 - y) * 0.4:
                img.putpixel((x, y), CAP[2 if y == 4 else 1 if (x + y // 2 * 2) % 3 else 0] + (255,))
    img.putpixel((8, 3), CAP[0] + (255,))
    img.putpixel((8, 2), CAP[0] + (255,))
    img.putpixel((6, 9), NUT[2] + (255,))
    return img


def roasted():
    """A little heap of roasted acorns, shelled and browned."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for cx, cy in ((5, 10), (10, 10), (7.5, 6.5), (8, 12)):
        for y in range(16):
            for x in range(16):
                if ((x - cx) / 2.6) ** 2 + ((y - cy) / 2.1) ** 2 <= 1.0:
                    tone = 2 if y < cy else 1 if y < cy + 1 else 0
                    img.putpixel((x, y), ROAST[tone] + (255,))
    return img


def squirrel_textures():
    out = {("entity", f"squirrel_{coat}"): squirrel(coat) for coat in COATS}
    out[("item", SQUIRRELS["acorn"])] = acorn()
    out[("item", SQUIRRELS["roasted"])] = roasted()
    return out
