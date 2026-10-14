"""Textures for the Lantern Fen (tools/lantern_fen.py, docs/features/lantern-fen.md): its one lair-only block, the fen
lamp (its blackened iron frame and cap, and its panes, lit and gone out), and the Hag Stone's icon (its map,
tools/item_icons/hag_stone.txt). Painted here by code, 16 x 16, in the manner of the vanilla blocks
(tools/block_style.py) and the Witching Season's clean style: a few flat tones a material, and patterns (the plate's
bevel and rivets, the flame's bands) laid out whole within the block rather than as noise. Called from
tools/lair_textures.py. No Mojang texture is read, traced or copied.

The lamp's model (tools/lair_data.py) puts its panes on a core from 3 to 13 across and 2 to 13 high, behind corner posts
2 to 4 and 12 to 14 across: its panes show from 4 to 12 across and from 3 to 14 down the texture.
"""
import math

from PIL import Image

import item_icons


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


IRON = pal("16191a", "23282a", "313839", "424b4c", "57625f")     # blackened iron, a joint darkest
VERDIGRIS = pal("2f5446", "467260")                             # the fen's green bloom on it
GLOW = pal("4c5a18", "7f9426", "aebf3c", "d7e06a", "f0f2a6", "fffbe2")   # the marsh-light: deep to white-hot
SMOKED = pal("0d1411", "141e19", "1d2a23", "2a3a31", "3d5246")    # the panes gone out
PANE = (4, 12, 3, 14)   # x0, x1 (exclusive), y0, y1 (exclusive): where the panes show between the posts


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def lamp_frame():
    """Blackened iron plate: a bevel lit on its top and left edges and shaded on the others, a rivet in each corner
    and green bloom gathered low on it."""
    img = new(IRON[2])
    for i in range(16):
        img.putpixel((i, 0), IRON[3])
        img.putpixel((0, i), IRON[3])
        img.putpixel((i, 15), IRON[0])
        img.putpixel((15, i), IRON[1])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel((x, y), IRON[4])
        img.putpixel((x + 1, y + 1), IRON[0])
    for x, y in ((4, 12), (5, 12), (5, 13), (9, 13), (10, 12), (11, 13), (7, 14), (12, 14)):
        img.putpixel((x, y), VERDIGRIS[0] if (x + y) % 2 else VERDIGRIS[1])
    for x in (6, 7, 8, 9):
        img.putpixel((x, 7), IRON[1])   # a seam across the plate
    return img


def lamp_top():
    """The cap from above: the plate's bevel round a ring of darker iron, and the knob in its middle."""
    img = lamp_frame()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if 3.0 <= r < 4.2:
                img.putpixel((x, y), IRON[1])
            elif r < 2.2:
                img.putpixel((x, y), IRON[4] if x + y < 15 else IRON[3])
    return img


def flame_band(x, y):
    """How hot pixel (x, y) of a lit pane is, 0 (its edge) to 5 (the white heart of the flame): a teardrop flame on its
    wick low in the middle, in bands, and the glow of the glass round it."""
    dx, dy = x + 0.5 - 8.0, y + 0.5 - 10.0
    # The flame narrows upward: its half-width shrinks as it rises over the wick.
    rise = max(0.0, -dy)
    width = 2.6 - 0.32 * rise
    d = math.hypot(dx / max(width, 0.6), dy / (3.2 if dy < 0 else 1.6))
    if d < 0.55:
        return 5
    if d < 1.0:
        return 4
    if d < 1.6:
        return 3
    return 2 if (x + y) % 5 else 1


def lamp_glass(lit):
    """The panes between the posts: lit, the marsh-light's flame burning in bands within a glow; gone out, smoked dark
    glass with a streak of the sky on it and the dead wick at its foot. Outside the panes, the frame's iron."""
    img = lamp_frame()
    x0, x1, y0, y1 = PANE
    for y in range(y0, y1):
        for x in range(x0, x1):
            if lit:
                img.putpixel((x, y), GLOW[flame_band(x, y)])
            else:
                streak = (x - x0) - (y - y0) in (3, 4) and y < y0 + 6
                img.putpixel((x, y), SMOKED[3] if streak else SMOKED[1] if (x + 2 * y) % 7 else SMOKED[2])
    # The wick, a dark stub at the foot of the flame (lit) or of the empty pane.
    for y in (y1 - 2, y1 - 1):
        img.putpixel((8, y), GLOW[0] if lit else SMOKED[0])
    # The leading along the panes' top and foot.
    for x in range(x0, x1):
        img.putpixel((x, y0), IRON[1])
        img.putpixel((x, y1 - 1), IRON[1] if x != 8 else (GLOW[0] if lit else SMOKED[0]))
    return img


def lantern_fen_textures():
    return {
        ("block", "fen_lamp_frame"): lamp_frame(),
        ("block", "fen_lamp_top"): lamp_top(),
        ("block", "fen_lamp_glass"): lamp_glass(False),
        ("block", "fen_lamp_glass_lit"): lamp_glass(True),
        ("item", "hag_stone"): item_icons.draw("hag_stone"),
    }


if __name__ == "__main__":
    import sys
    sheet = lantern_fen_textures()
    keys = sorted(sheet)
    scale, cols = 8, 5
    rows = (len(keys) + cols - 1) // cols
    image = Image.new("RGBA", (cols * (16 * scale + 8), rows * (16 * scale + 8)), (40, 44, 52, 255))
    for i, key in enumerate(keys):
        tile = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        image.paste(tile, ((i % cols) * (16 * scale + 8), (i // cols) * (16 * scale + 8)), tile)
    image.save(sys.argv[1] if len(sys.argv) > 1 else "lantern_fen_textures.png")
    print(len(keys), "textures")
