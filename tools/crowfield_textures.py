"""Textures for the Crowfield (tools/crowfield.py, docs/features/crowfield.md): its two lair-only blocks and the Harvest
Crown's icon (its map, tools/item_icons/harvest_crown.txt). Painted here by code, 16 x 16, in the clean style the other
lairs use: a few flat tones a material, crisp edges, the patterns (a stalk and its leaves, a gourd's ribs, a carved
grin) laid out whole within the block rather than as noise. Called from tools/lair_textures.py. No Mojang texture is
read, traced or copied.
- Crowfield corn: standing corn at dusk, dry and late in the year: pale straw stalks, long leaves gone khaki and tan
  arcing off them, a few gaps between them cut out; its top, the stalks' tassels against the sky and an ear in its husk;
- the King's Gourd: its rind green (resting), gold-green and orange as it ripens (ribs down it, lit along their left
  edges), its carved face burning when lit (eyes and a grin with the fire behind them), the cap of its stem scar, and the
  vine (its stem, and the stub left when it is picked).

The models' faces (tools/lair_data.py) take the textures at a texel a pixel.
"""
from PIL import Image

import item_icons


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


STALK = pal("5a4a2a", "7e6a3a", "a48c52", "c8b070", "e2d090")       # dark to lit
LEAF = pal("3e3a20", "5c5428", "7a7034", "988a46", "b8a65e")
TASSEL = pal("8a6a32", "b08c44", "d4b062", "eed892")
EAR = pal("7a5212", "a87018", "d29a2a", "f0c04a")
GREEN = pal("1e3618", "2c4a20", "3c6228", "507a32", "6c9442")
GOLD = pal("4e4a16", "6e6a1e", "908a28", "b2a838", "d0c650")
ORANGE = pal("6a2a08", "92400c", "b85a12", "dc7a1c", "f29a34")
FIRE = pal("ffd75a", "ffb030", "ff8a1a")
VINE = pal("2a3a16", "3e5220", "566e2c", "6e8838")


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c)


# The stalks (their x; each two pixels wide, lit on its left) and their leaves: (the stalk, the row the leaf leaves it, which
# way it arcs). A leaf rises a little off the stalk and droops to its tip, as dry corn's do. Laid out to tile across the
# block's edges.
STALKS = [1, 5, 9, 13]
LEAVES = [(1, 2, 1), (1, 10, -1), (5, 6, 1), (5, 13, -1), (9, 1, -1), (9, 9, 1), (13, 4, -1), (13, 12, 1)]
LEAF_RUN = [(1, 0), (2, -1), (3, -1), (4, 0), (5, 1), (6, 2), (7, 4)]   # (out from the stalk, rows up or down)


def corn_side(top=False):
    """Dense dry corn: behind, a mass of leaves in shadow; in front, four stalks of pale straw, knotted where their leaves
    leave them, each leaf rising a little and drooping to its tip, lit along its upper edge; a few gaps cut out low in the
    mass. Its top: the upper rows the stalks' tassels, thin spikes of straw fanning against the sky with gaps between
    them, and an ear in its husk on one stalk."""
    img = new()
    first = 6 if top else 0
    # The shadowed mass behind: blades on a slant, dark and darker, with a few gaps.
    for y in range(first, 16):
        for x in range(16):
            if (x * 3 + y * 5) % 17 == 0 and y > first + 2:
                continue
            img.putpixel((x, y), LEAF[1] if (x + y) % 4 < 2 else LEAF[0])
    for x in STALKS:
        for y in range(first, 16):
            put(img, x, y, STALK[3])
            put(img, (x + 1) % 16, y, STALK[1])
    for sx, row, way in LEAVES:
        if top and row < first + 1:
            continue
        for i, (out, rise) in enumerate(LEAF_RUN):
            x = (sx + (1 if way > 0 else 0) + way * out) % 16
            y = row + rise
            if not first <= y < 16:
                continue
            put(img, x, y, LEAF[4] if i < 3 else LEAF[3] if i < 5 else LEAF[2])
            if y + 1 < 16:
                put(img, x, y + 1, LEAF[2] if i < 5 else LEAF[1])
        # The knot where the leaf leaves its stalk.
        put(img, sx, row, STALK[4])
    if top:
        # The tassels: thin spikes fanning up from each stalk, the middle one tallest, lit at their tips.
        for n, x in enumerate(STALKS):
            for dx, height in ((-1, 3), (0, 5), (1, 6), (2, 4)):
                height -= (n + dx) % 3
                for k in range(height):
                    y = first - 1 - k
                    lean = dx * k // 2
                    put(img, (x + dx + lean) % 16, y, TASSEL[3] if k == height - 1 else TASSEL[2] if k % 2 else TASSEL[1])
        # The ear in its husk on the third stalk: gold kernels peeping out of pale husk.
        for y in range(9, 14):
            put(img, 11, y, EAR[3] if y == 10 else EAR[2])
            put(img, 12, y, EAR[1] if y % 2 else EAR[2])
        for y in range(9, 15):
            put(img, 13, y, LEAF[4] if y < 12 else LEAF[3])
        put(img, 11, 14, LEAF[3])
        put(img, 12, 14, LEAF[2])
    return img


def corn_end():
    """The corn's cut ends (top and bottom faces): the stalks' cross-sections in their rows, straw between, cut out."""
    img = new()
    for y in range(16):
        for x in range(16):
            if (x % 5, y % 5) in ((2, 2), (3, 2), (2, 3), (3, 3)):
                img.putpixel((x, y), STALK[3] if (x % 5, y % 5) == (2, 2) else STALK[1])
            elif (x + y) % 7 == 0:
                img.putpixel((x, y), LEAF[2])
    return img


def rind(colours, face=False):
    """A gourd's rind: ribs down it, each lit along its left edge and dark in the groove to its right; its top and foot
    darker. A carved face burns in the lit stage's front: two eyes and a grin, the fire behind them bright in the middle."""
    img = new(colours[2])
    for y in range(16):
        for x in range(16):
            rib = x % 4
            c = colours[3] if rib == 0 else colours[2] if rib in (1, 2) else colours[1]
            if y in (0, 15):
                c = colours[1] if rib else colours[2]
            elif y in (1, 14) and rib == 3:
                c = colours[0]
            img.putpixel((x, y), c)
    if face:
        for eye in (3, 10):
            for x, y in ((eye, 4), (eye + 1, 4), (eye + 2, 4), (eye, 5), (eye + 1, 5), (eye + 2, 5), (eye + 1, 6)):
                put(img, x, y, FIRE[1] if (x, y) != (eye + 1, 5) else FIRE[0])
        for x in range(3, 13):
            low = 10 if x in (3, 12) else 12
            for y in range(9, low):
                put(img, x, y, FIRE[0] if 5 <= x <= 10 and y == 10 else FIRE[1])
        for x in (5, 8, 11):
            put(img, x, 9, colours[2])
        for x in (6, 9):
            put(img, x, 11, colours[2])
    return img


def rind_top(colours):
    """The gourd's top: the ribs running in to the stem scar at the middle, a ring of darker rind round it."""
    img = new(colours[2])
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5
            spoke = (abs(dx) < 0.7 or abs(dy) < 0.7 or abs(abs(dx) - abs(dy)) < 0.7) and d > 2.5
            c = colours[1] if spoke else colours[3] if d > 6.5 else colours[2]
            if 2.0 < d <= 3.0:
                c = colours[0]
            img.putpixel((x, y), c)
    return img


def vine():
    """The vine: a twisted green-brown stem, its strands on a slant, a curl of tendril."""
    img = new(VINE[1])
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (VINE[3], VINE[2], VINE[1], VINE[0])[(x + y // 2) % 4])
    return img


def crowfield_textures():
    return {
        ("block", "crowfield_corn"): corn_side(),
        ("block", "crowfield_corn_top"): corn_side(top=True),
        ("block", "crowfield_corn_end"): corn_end(),
        ("block", "kings_gourd_green"): rind(GREEN),
        ("block", "kings_gourd_green_top"): rind_top(GREEN),
        ("block", "kings_gourd_gold"): rind(GOLD),
        ("block", "kings_gourd_gold_top"): rind_top(GOLD),
        ("block", "kings_gourd_orange"): rind(ORANGE),
        ("block", "kings_gourd_orange_top"): rind_top(ORANGE),
        ("block", "kings_gourd_face"): rind(ORANGE, face=True),
        ("block", "kings_gourd_vine"): vine(),
        ("item", "harvest_crown"): item_icons.draw("harvest_crown"),
    }


if __name__ == "__main__":
    import sys
    sheet = crowfield_textures()
    keys = sorted(sheet)
    scale, cols = 8, 6
    out = Image.new("RGBA", (cols * 16 * scale + (cols + 1) * 4, ((len(keys) + cols - 1) // cols) * (16 * scale + 4) + 4),
                    (90, 90, 96, 255))
    for i, key in enumerate(keys):
        img = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        out.paste(img, (4 + (i % cols) * (16 * scale + 4), 4 + (i // cols) * (16 * scale + 4)), img)
    out.save(sys.argv[1])
