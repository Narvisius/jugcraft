"""Draws the pumpkin-carving textures (the Agriculture branch's carving slice): the Carving Knife, the
hand-carved pumpkin's item icons (dark and candle-lit) and roasted pumpkin seeds.

Called from crop_textures.py. Everything is drawn here by code; no Mojang texture is read, traced or
recoloured. The hand-carved pumpkin block itself uses vanilla's pumpkin model and textures (by reference,
so resource packs apply), with the carving drawn over it by the client at run time.
"""
from crop_textures import Canvas, rgb, outline, seeds_item

PUMPKIN = [rgb("7a3a08"), rgb("a8520e"), rgb("cf6e14"), rgb("e88a22"), rgb("f6a840")]
STEM = [rgb("3e3a14"), rgb("5c5420"), rgb("7d7430")]
HOLE = rgb("2a1406")
GLOW = [rgb("f59a2c"), rgb("ffe07a"), rgb("fff2b8")]
# A soul torch's cold flame (the carving's soul colours in client/CarvingTextures.java).
SOUL_GLOW = [rgb("37a9c9"), rgb("c8faff"), rgb("f0feff")]
STEEL = [rgb("4a5058"), rgb("7c848e"), rgb("aeb6c0"), rgb("dfe5ec")]
HANDLE = [rgb("3b2414"), rgb("5e3a1e"), rgb("84562c")]
BRASS = rgb("c89a3a")


def carving_knife():
    """A short kitchen knife, tip to the upper right like vanilla's handheld tools, with a riveted wooden handle."""
    c = Canvas()
    # Blade: a narrow wedge from (7, 8) to the tip at (13, 2), spine on the upper edge.
    for i in range(7):
        x, y = 7 + i, 8 - i
        c.px(x, y, STEEL[3] if i < 6 else STEEL[2])
        c.px(x, y + 1, STEEL[2])
        if i < 5:
            c.px(x + 1, y + 1, STEEL[1])
    # Bolster, then the handle running down to the lower left.
    c.px(6, 9, BRASS)
    c.px(7, 10, BRASS)
    for i in range(5):
        x, y = 5 - i, 10 + i
        c.px(x, y, HANDLE[2])
        c.px(x + 1, y, HANDLE[1])
        c.px(x + 1, y + 1, HANDLE[0])
    c.px(4, 11, BRASS)
    c.px(2, 13, BRASS)
    outline(c, rgb("1c1c20"))
    return c.img


def pumpkin_icon(lit, soul=False):
    """A round, ribbed pumpkin with a stalk and a carved face: a dark hollow, or candlelight when lit (blue from a
    soul torch)."""
    glow = SOUL_GLOW if soul else GLOW
    c = Canvas()
    for y in range(3, 15):
        for x in range(1, 15):
            if ((x - 7.5) / 6.6) ** 2 + ((y - 9.0) / 5.4) ** 2 <= 1.0:
                shade = 3
                if x in (4, 11) or abs(x - 7.5) < 0.6:
                    shade = 2  # the ribs
                if x <= 3 or y <= 4:
                    shade = min(4, shade + 1)  # light from the upper left
                if y >= 13 or x >= 13:
                    shade = max(1, shade - 1)
                c.px(x, y, PUMPKIN[shade])
    for x, y, shade in ((7, 2, 2), (8, 2, 1), (8, 1, 1), (7, 3, 0), (8, 3, 0)):
        c.px(x, y, STEM[shade])
    face = [(5, 7), (4, 8), (5, 8), (6, 8), (10, 7), (9, 8), (10, 8), (11, 8),
            (4, 11), (5, 11), (7, 11), (8, 11), (10, 11), (11, 11), (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12)]
    for x, y in face:
        above = (x, y - 1) in face
        c.px(x, y, (glow[1] if above else glow[0]) if lit else HOLE)
    if lit:
        for x, y in ((5, 8), (10, 8), (7, 12), (8, 12)):
            c.px(x, y, glow[2])
    outline(c, PUMPKIN[0])
    return c.img


def carving_textures():
    """(kind, name) -> image for every carving texture."""
    return {
        ("item", "carving_knife"): carving_knife(),
        ("item", "hand_carved_pumpkin"): pumpkin_icon(False),
        ("item", "hand_carved_pumpkin_lit"): pumpkin_icon(True),
        ("item", "hand_carved_pumpkin_soul"): pumpkin_icon(True, soul=True),
        ("item", "roasted_pumpkin_seeds"): seeds_item([rgb("8a6a34"), rgb("b08c4a"), rgb("d2b06a")],
                                                      [(3, 4), (8, 3), (12, 6), (5, 8), (10, 10), (3, 12), (8, 13)], size=(2, 1)),
    }
