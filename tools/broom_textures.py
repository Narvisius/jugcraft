"""Original textures for the flying broomstick (fall addition 22) (requires Pillow): the item, a broom held aslant with
a purple-bound bundle of straw and a trail of sparkles; and the entity's grain, a pale streaked texture that the renderer
tints for the ash handle, the cord and the straw.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured.
"""
import random

from PIL import Image

from agriculture import BROOMSTICK
from crop_textures import rgb
import block_style as bs

WOOD = [rgb("4e3420"), rgb("6b4a2b"), rgb("86603a"), rgb("a07a4c")]
STRAW = [rgb("8a6a2a"), rgb("b89440"), rgb("d2b060"), rgb("e8cc80")]
CORD = [rgb("2e1c40"), rgb("4a2d63"), rgb("6a4488")]
SPARK = [rgb("c89ae8"), rgb("f0d8ff")]


def item():
    """A broom from the lower left to the upper right: the handle's knob at the top, two turns of purple cord, and the
    bristles fanning out in strands to the lower left, a few sparkles trailing."""
    import math
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(22001)
    # The bristles first, so the cord sits over their roots: strands from the neck, fanning between left and down.
    for k in range(9):
        angle = math.radians(188 + k * 11.5)
        length = 7.6 - abs(k - 4) * 0.3
        shade = STRAW[(1, 2, 0, 2, 1, 3, 2, 0, 2)[k]]
        for step in range(1, 13):
            t = step / 12 * length
            x = round(4.5 + math.cos(angle) * t)
            y = round(10.5 - math.sin(angle) * t)
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (STRAW[3] if step > 10 else shade) + (255,))
    # The handle: two pixels wide up the diagonal, shaded, with a darker knob at its tip.
    for i in range(6, 16):
        x, y = i, 15 - i
        img.putpixel((x, y), WOOD[2] + (255,))
        if x - 1 >= 0:
            img.putpixel((x - 1, y), WOOD[1] + (255,))
    img.putpixel((15, 0), WOOD[0] + (255,))
    img.putpixel((14, 0), WOOD[0] + (255,))
    # Two turns of cord round the neck.
    for (x, y), shade in (((5, 9), 2), ((6, 9), 1), ((4, 10), 1), ((5, 10), 0), ((3, 11), 2), ((4, 11), 1)):
        img.putpixel((x, y), CORD[shade] + (255,))
    # Sparkles trailing behind.
    for x, y in ((2, 5), (4, 3), (1, 2), (8, 12), (10, 14)):
        img.putpixel((x, y), SPARK[rng.randrange(2)] + (255,))
    return img


def grain():
    """A pale grain, streaked lengthwise, for the renderer to tint."""
    img = Image.new("RGBA", (16, 16))
    rng = random.Random(22002)
    grain = bs.Field(16, 16, 22003, 1.5, 6.0)    # thin streaks running along the handle
    for x in range(16):
        streak = rng.choice((0, 0, 1, 2))
        for y in range(16):
            v = grain(x, y)
            value = 200 + streak * 18 + (6 if v > 0.68 else -10 if v < 0.32 else 0)
            img.putpixel((x, y), (value, value, value, 255))
    return img


def broom_textures():
    return {("item", BROOMSTICK["item"]): item(), ("entity", BROOMSTICK["item"]): grain()}
