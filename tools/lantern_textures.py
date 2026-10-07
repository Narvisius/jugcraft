"""Original textures for the sky lantern festival (fall additions 6) (requires Pillow): the Sky Lantern's item in two
layers (its paper, pale so the dye tints it, and, untinted, its bamboo ring and the flame in its opening), the paper the
client's SkyLanternRenderer wraps a lantern in (tinted as it is drawn), and each mooncake: a round golden cake pressed
with a flower, its scalloped rim, and a wedge cut away to show its filling.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from fixed seeds; no Mojang texture is
read, traced or recoloured. The items are see-through round their shapes.
"""
import math

from PIL import Image

from agriculture import LANTERNS
from crop_textures import Canvas, rgb
import block_style as bs
from decor9_textures import put
from decor13_textures import icon

LANTERN = [
    "................",
    "....pppppppp....",
    "...pqppppppqp...",
    "...pqppppppqp...",
    "...pqppppppqp...",
    "....pqppppqp....",
    "....pqppppqp....",
    "....pqppppqp....",
    ".....pqppqp.....",
    ".....pqppqp.....",
    ".....pppppp.....",
    "................",
    "................",
    "................",
    "................",
    "................",
]
FRAME = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....bbbbbb.....",
    "......ffff......",
    ".......ff.......",
    "................",
    "................",
]
FILLINGS = {"red_bean_mooncake": "5a1a1a", "chestnut_mooncake": "4a2e1a", "pumpkin_mooncake": "f6c83a"}
CRUST = [rgb("a8641e"), rgb("c47e2c"), rgb("d8963a"), rgb("ecb058")]


def lantern():
    return icon(LANTERN, {"p": rgb("f4f0e8"), "q": rgb("dcd4c4")})


def lantern_frame():
    return icon(FRAME, {"b": rgb("8a6a3a"), "f": rgb("ffe080")})


def paper():
    """Rice paper over a frame: pale, with faint ribs (tinted by the renderer)."""
    c = Canvas()
    bs.fill(c, 0, 0, 15, 15, [rgb("ece8e0"), rgb("f4f2ec"), rgb("fcfbf8")], 29101, [1, 2, 3], spread=0.6)
    for x in (0, 4, 8, 12):
        for y in range(16):
            c.px(x, y, rgb("d8d2c6"))
    return c.img


def mooncake(filling):
    """A round cake: a scalloped rim, a pressed flower in the middle, and a wedge cut out showing the filling."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    fill = rgb(filling)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            angle = math.atan2(dy, dx)
            rim = 6.6 + 0.5 * math.cos(angle * 8)
            if d > rim:
                continue
            if 0.1 < angle < 0.9 and d > 1.5:
                # The cut wedge: the filling, ringed by crust.
                colour = fill if d < rim - 1.0 else CRUST[1]
            elif d > rim - 1.2:
                colour = CRUST[0]
            else:
                petal = abs(math.cos(angle * 3)) * 2.6 + 0.6
                colour = CRUST[1] if abs(d - petal) < 0.55 or d < 0.9 else CRUST[3] if dx + dy < -3 else CRUST[2]
            put(img, x, y, colour)
    return img


def lantern_textures():
    out = {("item", LANTERNS["item"]): lantern(), ("item", f"{LANTERNS['item']}_frame"): lantern_frame(),
           ("entity", "sky_lantern"): paper()}
    for cake in LANTERNS["mooncakes"]:
        out[("item", cake)] = mooncake(FILLINGS[cake])
    return out
