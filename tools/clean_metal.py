"""Shared drawing helpers for the clean dieselpunk texture style (docs/ART_DIRECTION.md, "Texturing").

The owner asked on 4 October 2026 for the dieselpunk textures to stop being so noisy and rusty, pointing at vanilla
copper, a weathered pipe, Immersive Engineering Reimmersed's machines and a vanilla-palette car. What those share, and
what these helpers draw:

- Flat fills from a short palette (four or five shades), never a random shade per pixel.
- Shape from light: a one-pixel bevel, lit on the top and left and shaded on the bottom and right; recessed insets
  shaded the other way round; bolts as two-by-two heads lit at the top left.
- Wear only where it would gather (corners, seams, under bolts) and in small clusters of one or two shades, never
  speckled over a whole face.

plate() draws a machine panel with a dark seam all round it. A building block that tiles in a wall must not do that
(two blocks side by side would show a doubled dark seam, a grid of framed tiles): use sheet(), which splits the seam
across the block's edge ("Tiling building blocks" in docs/ART_DIRECTION.md).

canvas() and ramp() make 16x16 images; the other helpers draw on an image of any size. Every helper draws
deterministically: the same arguments give the same pixels.
"""

from PIL import Image


def canvas(fill=(0, 0, 0), alpha=255):
    return Image.new("RGBA", (16, 16), tuple(fill[:3]) + (alpha,))


def put(img, x, y, color, alpha=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(color[:3]) + (alpha,))


def rect(img, x0, y0, x1, y1, color):
    """Fill the rectangle from (x0, y0) to (x1, y1), both corners included."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, color)


def bevel(img, x0, y0, x1, y1, light, shade, fill=None):
    """A raised panel: lit along the top and left edges, shaded along the bottom and right, optionally filled."""
    if fill is not None:
        rect(img, x0, y0, x1, y1, fill)
    for x in range(x0, x1 + 1):
        put(img, x, y0, light)
        put(img, x, y1, shade)
    for y in range(y0, y1 + 1):
        put(img, x0, y, light)
        put(img, x1, y, shade)
    put(img, x1, y0, fill if fill is not None else light)
    put(img, x0, y1, fill if fill is not None else shade)


def inset(img, x0, y0, x1, y1, light, shade):
    """A recessed groove or panel: the reverse of a bevel (shaded top and left, lit bottom and right)."""
    bevel(img, x0, y0, x1, y1, shade, light)


def plate(img, palette, outline=True):
    """A machine panel: an outline seam in the darkest shade, then a bevelled panel filled with the middle shade.

    `palette` runs dark to light with at least four shades; the panel uses palette[1], [2] and [3]. Not for building
    blocks that tile in a wall (the outline doubles where two meet): use sheet() for those."""
    if outline:
        rect(img, 0, 0, 15, 15, palette[0])
        bevel(img, 1, 1, 14, 14, palette[3], palette[1], palette[2])
    else:
        bevel(img, 0, 0, 15, 15, palette[3], palette[1], palette[2])


def sheet(img, light, fill, seam):
    """A tiling building block's sheet of metal: filled, lit along the top row and left column, with the seam along
    the bottom row and right column. In a wall the seam meets the next block's lit edge, so each boundary shows one
    seam and one lit edge, the way bastion concrete splits its course lines, never a doubled dark outline. The two
    corners where a lit edge meets the seam take the fill."""
    n = img.width
    rect(img, 0, 0, n - 1, n - 1, fill)
    for i in range(n):
        put(img, i, 0, light)
        put(img, 0, i, light)
        put(img, i, n - 1, seam)
        put(img, n - 1, i, seam)
    put(img, n - 1, 0, fill)
    put(img, 0, n - 1, fill)


def bolt(img, x, y, palette):
    """A two-by-two bolt head at (x, y): lit at the top left, shaded at the bottom right."""
    put(img, x, y, palette[-1])
    put(img, x + 1, y, palette[-2])
    put(img, x, y + 1, palette[-2])
    put(img, x + 1, y + 1, palette[0])


def corner_bolts(img, palette, at=2):
    for x, y in ((at, at), (14 - at, at), (at, 14 - at), (14 - at, 14 - at)):
        bolt(img, x, y, palette)


def patch(img, pixels, color, ox=0, oy=0):
    """Paint a hand-placed cluster (a list of (x, y) offsets) in one colour: wear, chips and stains."""
    for x, y in pixels:
        put(img, ox + x, oy + y, color)


# Hand-placed wear shapes, so wear reads as a few deliberate marks rather than noise.
CHIP = [(0, 0), (1, 0), (0, 1)]
CHIP_WIDE = [(0, 0), (1, 0), (2, 0), (1, 1)]
STAIN_CORE = [(0, 0), (1, 0)]
STAIN_RIM = [(0, 1), (2, 0)]
# Short streaks one shade off the fill, like the faint marks on vanilla metal blocks: a plate looks worked, not empty.
SCUFFS = [(3, 4, 3), (9, 6, 2), (5, 10, 2), (10, 11, 3)]


def stain(img, x, y, core, rim, drip=None, drip_length=0):
    """A small weeping stain: a two-pixel core, a lighter edge and an optional run straight down from it."""
    patch(img, STAIN_RIM, rim, x, y)
    patch(img, STAIN_CORE, core, x, y)
    for i in range(drip_length):
        put(img, x, y + 2 + i, drip or rim)


def scuffs(img, color, marks=SCUFFS, only=None):
    """Hand-placed short horizontal streaks (x, y, length); with `only`, they draw only over pixels of that colour."""
    for x, y, length in marks:
        for i in range(length):
            if only is None or img.getpixel((x + i, y))[:3] == tuple(only[:3]):
                put(img, x + i, y, color)


def ramp(img, rows, palette, period):
    """Horizontal bands repeating every `period` rows, each row painted palette[rows[y % period]]."""
    for y in range(16):
        c = palette[rows[y % period]]
        for x in range(16):
            put(img, x, y, c)
