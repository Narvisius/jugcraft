"""Textures for the Moonlit Ring (tools/moonlit_ring.py, docs/features/moonlit-ring.md): its two lair-only blocks and the
Howling Totem's icon (its map, tools/item_icons/howling_totem.txt). Painted here by code, 16 x 16, in the clean style the
other lairs use: a few flat tones a material, crisp edges, the patterns (a cluster of leaves, a bell's highlight, a
rope's twist) laid out whole within the block rather than as noise. Called from tools/lair_textures.py. No Mojang texture
is read, traced or copied.
- Moonwood Leaves: the giant oaks' foliage at night, dark blue-green, each cluster of leaves lit silver along its
  moonward (top left) edge, a few gaps between them cut out;
- the Warden Bell: its silver (a soft highlight down the bell's moonward side, darker toward its lip), its yoke of old
  dark wood banded with iron, its clapper of dark iron, and its pull rope of twisted hemp;
- raked turf (the Alpha's trail): the clearing's moss torn into furrows by his claws, the dark earth in them.

The models' faces (tools/lair_data.py) take the textures at a texel a pixel.
"""
from PIL import Image

import item_icons


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


FOLIAGE = pal("0e1f1c", "16302a", "1f4238", "2b5747", "3f6f60", "7e9c98")   # dark to moonlit
SILVER = pal("2a2e35", "454b55", "68707c", "939ca8", "c3cad3", "eef2f6")
WOOD = pal("1e140c", "2c1d12", "3d2818", "4f3520")
IRON = pal("1a1c20", "2a2d32", "3c4047", "565b63")
HEMP = pal("4e3c24", "6e5636", "8a7048", "a8895a")


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c)


# The leaf clusters of Moonwood Leaves: each a rounded clump (x, y of its middle, radius); the clumps overlap, the later
# drawn over the earlier, and the few pixels no clump covers are left as gaps. Laid out to tile: a clump near an edge is
# drawn again across it.
CLUMPS = [(3, 3, 3.2), (11, 2, 3.0), (7, 7, 3.4), (1, 10, 3.0), (13, 9, 3.2), (6, 13, 3.0), (12, 15, 2.6), (9, 11, 2.2),
          (15, 5, 2.6), (5, 0, 2.4), (0, 6, 2.4), (9, 3, 2.2), (15, 13, 2.4), (2, 15, 2.4), (11, 6, 2.2), (4, 9, 2.2)]


def moonwood_leaves():
    """Clumps of leaves, each dark at its foot, mid in its body and lit silver along its top-left rim, a darker vein down
    its middle; the gaps between clumps cut out."""
    img = new()
    for cx, cy, r in CLUMPS:
        for oy in (-16, 0, 16):
            for ox in (-16, 0, 16):
                for y in range(16):
                    for x in range(16):
                        dx, dy = x + 0.5 - (cx + ox), y + 0.5 - (cy + oy)
                        d = (dx * dx + dy * dy) ** 0.5
                        if d > r:
                            continue
                        # Lit along the rim toward the moon (up and to the left), shadowed toward the foot.
                        toward = (-dx - dy) / max(d, 0.01)
                        if d > r - 1.0 and toward > 0.55:
                            c = FOLIAGE[5] if toward > 0.85 else FOLIAGE[4]
                        elif d > r - 1.0 and toward < -0.4:
                            c = FOLIAGE[1]
                        elif abs(dx - dy) < 0.6 and d < r - 1.2:
                            c = FOLIAGE[2]
                        else:
                            c = FOLIAGE[3]
                        img.putpixel((x, y), c)
    # Darken where two clumps meet, so each reads apart: a pixel whose clump differs from its lower-right neighbour's.
    out = img.copy()
    for y in range(16):
        for x in range(16):
            here = img.getpixel((x, y))
            if here[3] and img.getpixel(((x + 1) % 16, (y + 1) % 16))[3] == 0:
                out.putpixel((x, y), FOLIAGE[0])
    return out


def bell_metal():
    """The bell's silver: a soft highlight down its moonward (left) side, mid tones across it, darker toward its right, and
    the bottom three rows (the lip, where the bell is struck) a band of brighter, then darker, metal."""
    img = new(SILVER[2])
    for y in range(16):
        for x in range(16):
            c = SILVER[4] if x in (3, 4) else SILVER[3] if x in (2, 5, 6) else SILVER[1] if x >= 12 else SILVER[2]
            if y >= 13:
                c = SILVER[5] if (y == 13 and x < 8) else SILVER[3] if y == 13 else SILVER[1] if y == 15 else c
            img.putpixel((x, y), c)
    for x in range(16):
        put(img, x, 0, SILVER[1])
    return img


def bell_yoke():
    """The yoke's old dark wood: grain along its length, two iron straps across it."""
    img = new(WOOD[2])
    for y in range(16):
        for x in range(16):
            c = WOOD[3] if y in (3, 10) else WOOD[1] if y in (6, 13) else WOOD[2]
            if (x + y * 3) % 11 == 0:
                c = WOOD[0]
            if x in (4, 11):
                c = IRON[3] if y % 4 == 0 else IRON[2]
            img.putpixel((x, y), c)
    return img


def bell_clapper():
    """The clapper's dark iron, lit on its left."""
    img = new(IRON[1])
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), IRON[3] if x < 5 else IRON[0] if x > 11 else IRON[1] if y % 5 else IRON[2])
    return img


def bell_rope():
    """The pull rope's twisted hemp: diagonal strands, each light along its top and dark at its foot."""
    img = new(HEMP[1])
    for y in range(16):
        for x in range(16):
            phase = (x + y) % 4
            img.putpixel((x, y), (HEMP[3], HEMP[2], HEMP[1], HEMP[0])[phase])
    return img


MOSS = pal("2e4519", "3d5a22", "4d6f2a", "5f8433", "74983d")
EARTH = pal("2a1c12", "3b2818", "513822", "684a2e")


def raked_turf():
    """Raked turf (the Alpha's trail): the clearing's moss torn by his claws, three furrows raked across it on a slant,
    the dark earth showing in them, a lip of torn moss lit along each furrow's upper edge."""
    img = new(MOSS[2])
    for y in range(16):
        for x in range(16):
            # The moss in flat tufts, each lit at its top left.
            tuft = (x // 4 + y // 4) % 2
            img.putpixel((x, y), MOSS[3] if tuft and (x % 4, y % 4) in ((0, 0), (1, 0), (0, 1)) else MOSS[2 + tuft - 1 + 1]
                         if tuft else MOSS[2])
    for k in range(3):
        for i in range(16):
            x = i
            y = (i // 2 + 2 + k * 5) % 16
            for dy in (0, 1):
                img.putpixel((x, (y + dy) % 16), EARTH[1 + dy])
            img.putpixel((x, (y - 1) % 16), MOSS[4])
            img.putpixel((x, (y + 2) % 16), MOSS[0])
    return img


def raked_turf_side():
    """Raked turf's side: the torn moss along the top, ragged, over its dark earth."""
    img = new(EARTH[1])
    for x in range(16):
        depth = 3 + (x * 7) % 3
        for y in range(16):
            if y < depth:
                img.putpixel((x, y), MOSS[3] if y == 0 else MOSS[2])
            elif (x * 5 + y * 3) % 11 == 0:
                img.putpixel((x, y), EARTH[0])
            elif (x + y * 7) % 13 == 0:
                img.putpixel((x, y), EARTH[3])
    return img


def moonlit_ring_textures():
    return {
        ("block", "moonwood_leaves"): moonwood_leaves(),
        ("block", "warden_bell_metal"): bell_metal(),
        ("block", "warden_bell_yoke"): bell_yoke(),
        ("block", "warden_bell_clapper"): bell_clapper(),
        ("block", "warden_bell_rope"): bell_rope(),
        ("block", "raked_turf"): raked_turf(),
        ("block", "raked_turf_side"): raked_turf_side(),
        ("item", "howling_totem"): item_icons.draw("howling_totem"),
    }


if __name__ == "__main__":
    import sys
    sheet = moonlit_ring_textures()
    keys = sorted(sheet)
    scale, cols = 8, 6
    out = Image.new("RGBA", (cols * 16 * scale + (cols + 1) * 4, ((len(keys) + cols - 1) // cols) * (16 * scale + 4) + 4),
                    (90, 90, 96, 255))
    for i, key in enumerate(keys):
        img = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        out.paste(img, (4 + (i % cols) * (16 * scale + 4), 4 + (i // cols) * (16 * scale + 4)), img)
    out.save(sys.argv[1])
