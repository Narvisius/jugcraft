"""Pixel art for the fall fair in vanilla's manner. On 6 October 2026 the owner found that nothing in the fair looked like
Minecraft: its eyes, mouths, brass and bulbs. So the midway, the plushes, the piñatas and the balloons are drawn at
vanilla's density, one texel to a model pixel and 16 to a block, and scaled up without smoothing to the size their
models were made for. Each file keeps its size and UVs, but its pixels are vanilla-sized. Faces are square pixel eyes and
mouths as on vanilla's mobs; brass is lit like a gold block; wood is planks; felt is wool.

Every pixel is drawn here by code; no Mojang texture is read, traced or recoloured.
"""
from PIL import Image

import block_style as bs

N = 64


class Grid:
    """Pixel access to a small drawing (block_style's painters draw on anything with `w`, `h` and `put`)."""

    def __init__(self, img):
        self.img = img
        self.w, self.h = img.size

    def put(self, x, y, c, alpha=255):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((x, y), tuple(c[:3]) + (alpha,))

    def clear(self, x, y):
        self.put(x, y, (0, 0, 0), 0)


def px16(painter, w=16, h=16, scale=N // 16):
    """A `w` by `h` drawing by `painter` (a function of a Grid), scaled up `scale` times without smoothing."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    painter(Grid(img))
    return img.resize((w * scale, h * scale), Image.NEAREST)


def sprite(rows, colours, scale=N // 16):
    """Pixel art from rows of characters, one a pixel: each character a colour from `colours`, '.' left clear."""
    def paint(p):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != ".":
                    p.put(x, y, colours[ch])
    return px16(paint, len(rows[0]), len(rows), scale)


def wool(p, colours, seed):
    """Felt in the manner of vanilla's wool: soft low-contrast clumps in `colours` (three neighbouring tones)."""
    bs.cloth(colours, seed)(p)


def seam(p, colours, column=None, row=None):
    """A seam where two felt pieces meet: a darker line, its stitches a tone lighter every other pixel."""
    for i in range(16):
        x, y = (column, i) if column is not None else (i, row)
        p.put(x, y, colours[2] if i % 2 else colours[0])


def box(p, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p.put(x, y, colour)
