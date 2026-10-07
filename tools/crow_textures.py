"""The crow's texture (fall additions 4) (requires Pillow): glossy blue-black feathers, a dark grey beak and feet, and
bright eyes, laid out for the client's CrowModel (64 by 32; each box's faces where a vanilla model box puts them).

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture is
read, traced or recoloured.
"""
from PIL import Image

from crop_textures import rgb

FEATHER = [rgb("0e0e14"), rgb("16161e"), rgb("1e1e2a"), rgb("2a2a3c"), rgb("3a3e58")]
BEAK = [rgb("2a2a2a"), rgb("444448")]
EYE = rgb("d8c870")

# (u, v, width, height, depth) of each box, as CrowModel lays them out.
BOXES = {"head": (0, 0, 3, 3, 3), "beak": (12, 0, 1, 1, 2), "tail": (24, 0, 3, 1, 3), "body": (0, 6, 4, 4, 7),
         "left_wing": (0, 17, 1, 3, 6), "right_wing": (14, 17, 1, 3, 6), "leg": (28, 6, 1, 2, 1)}


def box_area(u, v, w, h, d):
    """Every texel a model box with its texture at (u, v) reads."""
    for x in range(u + d, u + d + 2 * w):
        for y in range(v, v + d):
            yield x, y
    for x in range(u, u + 2 * (w + d)):
        for y in range(v + d, v + d + h):
            yield x, y


def crow():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for name, (u, v, w, h, d) in BOXES.items():
        for x, y in box_area(u, v, w, h, d):
            if name in ("beak", "leg"):
                colour = BEAK[1] if y == v or y == v + d else BEAK[0]
            else:
                # Flat black feathers, a glossy blue-black band along the top of each side, the wings' edge glossiest.
                colour = FEATHER[3] if y == v + d else FEATHER[2] if y < v + d else FEATHER[1]
                if name in ("left_wing", "right_wing") and y == v + d:
                    colour = FEATHER[4]
            img.putpixel((x, y), colour + (255,))
    # The eyes, one on each side of the head (its side faces: u 0-2 and 6-8, v 3-5).
    img.putpixel((1, 4), EYE + (255,))
    img.putpixel((7, 4), EYE + (255,))
    return img


def crow_textures():
    return {("entity", "crow"): crow()}
