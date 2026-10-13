"""The one texture of the feasts and food displays (tools/feasts.py) that the owner's library has no drawing for: the
Plate, a round white glazed plate with a blue band, its rim and its well. Everything else in the slice wears the owner's
own textures (tools/owner_art.py). Called from tools/generate_textures.py before the owner's imports.

The plate's top is the 10 x 10 square from (3, 3), round at its corners; its 1-pixel edge is row 13 (tools/feasts_data.py
display_model).
"""
from PIL import Image

GLAZE = (240, 236, 226, 255)
WELL = (248, 246, 240, 255)
RIM = (222, 214, 198, 255)
EDGE = (186, 176, 158, 255)
BAND = (96, 128, 176, 255)
SHINE = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)


def plate():
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(3, 13):
        for x in range(3, 13):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5  # from the middle, in pixels
            if r > 5.2:
                continue  # round, not square
            colour = EDGE if r > 4.4 else BAND if r > 3.6 else RIM if r > 2.6 else WELL
            img.putpixel((x, y), colour)
    for x, y in ((6, 6), (7, 6), (6, 7)):
        img.putpixel((x, y), SHINE)  # a glint in the well
    for x in range(4, 12):
        img.putpixel((x, 13), EDGE if x in (4, 11) else GLAZE)
    return img


def draw_all(save):
    save(plate(), "block", "plate")
