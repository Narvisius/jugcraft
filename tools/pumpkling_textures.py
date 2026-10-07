"""Original textures for the Pumpkling (fall addition 25) (requires Pillow): its vine body (32 by 32, laid out as
client/PumpklingModel.java's boxes: twisted green vine for the legs, arms and tendril, a root curl for each foot, broad
leaves for hands and at the tendril's tip). The pumpkin it wears is the carved pumpkin's own model and carving. Clean,
cartoon style: even twisted bands, no random speckle.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured.
"""
from PIL import Image

from crop_textures import rgb

VINE = [rgb("2f5a1e"), rgb("3e7027"), rgb("4f8630"), rgb("63a03c")]
LEAF = [rgb("3d7a26"), rgb("4c9030"), rgb("5ea83a"), rgb("78bf4c")]
VEIN = rgb("8ccf5e")
ROOT = [rgb("4a3820"), rgb("5a4426"), rgb("6e5530"), rgb("82663a")]


def _box(img, u, v, w, h, d, palette, twist=False):
    """Paints a box's whole unwrapped area (as a model box at texOffs(u, v) of size w, h, d lays it out)."""
    width, height = 2 * (w + d), d + h
    for y in range(height):
        for x in range(width):
            if twist:
                # Spiralling light and dark bands, as a twisted vine.
                tone = (3, 2, 0, 1)[(x + y) % 4]
            else:
                tone = 2 if y < d else 1 if y % 3 else 0
            img.putpixel((u + x, v + y), palette[tone] + (255,))


def _leaf(img, u, v, w, d):
    """A flat leaf (a box w by 0 by d): a pointed blade with a pale midrib, on its top and underside."""
    for side in range(2):
        x0 = u + d + side * w
        for y in range(d):
            for x in range(w):
                across = abs(x - (w - 1) / 2.0) / max(1.0, w / 2.0)
                along = y / max(1.0, d - 1)
                if across <= 1.0 - 0.6 * along ** 2:
                    tone = 3 if x == w // 2 else 2 if across < 0.5 else 1
                    img.putpixel((x0 + x, v + y), (VEIN if x == w // 2 else LEAF[tone]) + (255,))


def pumpkling():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _box(img, 0, 0, 2, 5, 2, VINE, twist=True)   # legs
    _box(img, 0, 13, 3, 1, 3, ROOT)              # feet, a curl of root
    _box(img, 8, 0, 1, 5, 1, VINE, twist=True)   # arms
    _box(img, 12, 0, 1, 4, 1, VINE, twist=True)  # tendril
    _leaf(img, 0, 8, 3, 4)                            # hands
    _leaf(img, 16, 0, 3, 3)                           # the tendril's leaf
    return img


def pumpkling_textures():
    return {("entity", "pumpkling"): pumpkling()}
