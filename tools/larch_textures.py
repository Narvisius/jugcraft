"""Original 16x16 textures for Alpine Spawn's larch (requires Pillow): its sapling.

Called from crop_textures.crop_textures(), next to the chestnut tree's. As with the rest of the branch's art,
every pixel is drawn here by code from fixed seeds; no Mojang texture is read, traced or recolored. The larch's
wood and needles (green, gold and bare) are drawn with every other tree's by wood_style.py.
"""
from crop_textures import Canvas, rgb

BARK = [rgb("2e1914"), rgb("44241a"), rgb("5c3222"), rgb("74422c"), rgb("8e5638")]
NEEDLES = {
    "green": [rgb("1e3a12"), rgb("2c5418"), rgb("3e6e1e"), rgb("568c28"), rgb("74aa38"), rgb("98c656")],
}


def sapling():
    """A young larch: a thin reddish stem with tiers of green tufts, narrowing to a point."""
    c = Canvas()
    c.line(8, 15, 8, 3, BARK[3])
    green = NEEDLES["green"]
    for y, half in ((4, 1), (6, 2), (8, 3), (10, 4), (12, 4)):
        for dx in range(-half, half + 1):
            c.px(8 + dx, y, green[4] if dx % 2 else green[3])
            if abs(dx) == half:
                c.px(8 + dx, y - 1, green[5])
    c.px(8, 2, green[5])
    return c.img


def larch_textures():
    """(kind, name) -> image for every larch texture."""
    out = {
        ("block", "larch_sapling"): sapling(),
    }
    return out
