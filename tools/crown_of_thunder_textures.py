"""Textures for the Crown of Thunder (tools/crown_of_thunder.py, docs/features/crown-of-thunder.md): its three lair-only
blocks and the Thunder Vane's icon (its map, tools/item_icons/thunder_vane.txt). Painted here by code, 16 x 16, in the
clean style the other lairs use: a few flat tones a material, crisp edges, light from the top left, the patterns laid
out whole within the block rather than as noise. Called from tools/lair_textures.py. No Mojang texture is read, traced
or copied.
- the Storm Rod: a rod of weathered copper, banded with verdigris and bound with dark iron collars; charged, the same
  rod burning storm-blue and white along its length;
- the nest's woven branches: grey, storm-bleached sticks laid across each other, dark gaps between them, a few
  slate-grey feathers caught in the weave; its side shows the sticks' cut ends;
- the storm cloud: see-through billows of grey and slate-blue, lighter on top.

The models' faces (tools/lair_data.py) take the textures at a texel a pixel.
"""
import math

from PIL import Image

import item_icons
from lair_layout import hash01


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


COPPER = pal("5a2e1c", "8a4a2c", "b06a3e", "d48e58", "eeb07a")       # dark to light
VERDIGRIS = pal("2e5a50", "3f7a6a", "5aa08a")
IRON = pal("24262a", "3a3d43", "565a62")
STORM = pal("1e3a78", "3a6ad0", "7aa8ff", "c8e0ff", "ffffff")      # a charged rod: deep to white-hot
STICK = pal("3a3630", "57524a", "7a7468", "a19a8c", "c4bdae")       # storm-bleached wood, dark to light
GAP = pal("16140f", "221f19")
FEATHER = pal("2c333d", "46505c", "6a7684")
CLOUD = [(70, 76, 92, 120), (92, 98, 116, 150), (118, 126, 146, 175), (150, 158, 178, 200)]


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def storm_rod(charged):
    """The rod's face: a column of copper six pixels wide down the middle, lit on its left, verdigris in bands, a dark
    iron collar top and bottom of each block; charged, storm-blue with a white-hot core. The rest is see-through (the
    models only take the middle six columns)."""
    img = new()
    for y in range(16):
        collar = y in (0, 15)
        band = y % 5 == 2
        for x in range(5, 11):
            i = x - 5
            if collar:
                c = IRON[2] if i == 0 else IRON[1] if i < 5 else IRON[0]
            elif charged:
                c = STORM[0] if i == 5 else STORM[1] if i in (0, 4) else STORM[4] if i == 2 and y % 3 else STORM[3] if i in (1, 2) else STORM[2]
            elif band:
                c = VERDIGRIS[2] if i < 2 else VERDIGRIS[1] if i < 5 else VERDIGRIS[0]
            else:
                c = COPPER[4] if i == 0 else COPPER[3] if i == 1 else COPPER[2] if i < 4 else COPPER[1] if i == 4 else COPPER[0]
            img.putpixel((x, y), c)
    return img


def nest_top():
    """Woven branches seen from above: sticks laid in two crossing directions, over and under, with dark gaps."""
    img = new(GAP[0])
    for y in range(16):
        for x in range(16):
            # Sticks running east-west on even bands, north-south on odd: each 2 pixels wide with a gap between.
            ew = (y % 4) in (0, 1)
            ns = (x % 4) in (1, 2)
            over_ew = ((x // 4) + (y // 4)) % 2 == 0
            if ew and (over_ew or not ns):
                c = STICK[3] if y % 4 == 0 else STICK[2]
            elif ns:
                c = STICK[3] if x % 4 == 1 else STICK[1]
            else:
                c = GAP[1] if hash01(x, y, 131) < 0.5 else GAP[0]
            img.putpixel((x, y), c)
    # A few highlights on the sticks' tops, and feathers caught in the weave.
    for x, y in ((2, 0), (9, 4), (13, 8), (5, 12)):
        img.putpixel((x, y), STICK[4])
    for x0, y0 in ((11, 2), (3, 9)):
        for i in range(3):
            img.putpixel((x0 + i, y0 + i // 2), FEATHER[2] if i == 0 else FEATHER[1])
        img.putpixel((x0 + 1, y0 + 1), FEATHER[0])
    return img


def nest_side():
    """Woven branches from the side: the sticks' cut ends in rows, between long sticks laid along the side."""
    img = new(GAP[0])
    for y in range(16):
        for x in range(16):
            row = y // 4
            if y % 4 == 3:
                c = GAP[1]
            elif row % 2 == 0:
                # A long stick laid along the side: lit on its top row.
                c = STICK[3] if y % 4 == 0 else STICK[2] if y % 4 == 1 else STICK[1]
            else:
                # Cut ends, round, three pixels across, staggered.
                cx = (x + (2 if row % 4 == 1 else 0)) % 4
                c = STICK[4] if cx == 1 and y % 4 == 0 else STICK[2] if cx in (0, 1, 2) else GAP[0]
            img.putpixel((x, y), c)
    img.putpixel((6, 5), FEATHER[2])
    img.putpixel((7, 5), FEATHER[1])
    img.putpixel((7, 6), FEATHER[0])
    return img


def storm_cloud():
    """See-through billows: rounded lumps of grey and slate-blue, lighter on their tops, a soft dark under each."""
    img = new()
    for y in range(16):
        for x in range(16):
            v = 0.5 + 0.25 * math.sin(x * 0.7 + 0.6) + 0.25 * math.sin(y * 0.55 + x * 0.25 + 1.3)
            v += (hash01(x, y, 137) - 0.5) * 0.15
            idx = 0 if v < 0.3 else 1 if v < 0.55 else 2 if v < 0.78 else 3
            if y < 4:
                idx = min(3, idx + 1)
            img.putpixel((x, y), CLOUD[idx])
    return img


def crown_of_thunder_textures():
    return {
        ("block", "storm_rod"): storm_rod(False),
        ("block", "storm_rod_charged"): storm_rod(True),
        ("block", "roc_nest"): nest_top(),
        ("block", "roc_nest_side"): nest_side(),
        ("block", "storm_cloud"): storm_cloud(),
        ("item", "thunder_vane"): item_icons.draw("thunder_vane"),
    }


if __name__ == "__main__":
    import sys
    sheet = crown_of_thunder_textures()
    keys = sorted(sheet)
    scale = 8
    image = Image.new("RGBA", (len(keys) * (16 * scale + 8), 16 * scale), (40, 44, 52, 255))
    for i, key in enumerate(keys):
        tile = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        image.paste(tile, (i * (16 * scale + 8), 0), tile)
    image.save(sys.argv[1] if len(sys.argv) > 1 else "crown_of_thunder_textures.png")
    print(len(keys), "textures")
