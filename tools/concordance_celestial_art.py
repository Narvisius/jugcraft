"""Textures for roadmap step 15, the Starwatchers' sky (tools/concordance_celestial.py): the Orrery Observatory's GeckoLib
sheet. The item icons (the observatory and the Astrolabe) are 16x16 maps in tools/item_icons/ (docs/ITEM_ICONS.md).
tools/concordance_art.py includes these in its textures().

Provenance (docs/features/arcane-concordance-celestial.md, "Art"): the sheet's brass, dark wood, iron and lens colours
are the owner's, read from the telescope in their library,
art/owner-library/originals/Blocks/Airships and Planes/entity/telescope.png (its colours only; no pixels are copied, and
the iron ramp is extended by one darker and one lighter tone). The boxes are painted here from code with fixed seeds.
The library file is only read, never changed.
"""

import os
import zlib

from PIL import Image

import concordance_celestial as celestial
import concordance_ecology_art as ecology_art
import item_icons

HERE = os.path.dirname(os.path.abspath(__file__))
TELESCOPE = os.path.join(HERE, "..", "art", "owner-library", "originals", "Blocks", "Airships and Planes", "entity",
                         "telescope.png")

# The owner's telescope's colours, darkest first (a ramp is darkest, dark, mid, light for ecology_art._paint_box).
BRASS = [(160, 81, 17), (189, 106, 40), (222, 146, 58), (255, 168, 69)]
WOOD = [(51, 23, 18), (78, 32, 27), (85, 41, 37), (119, 56, 50)]
IRON = [(78, 78, 80), (105, 105, 105), (144, 144, 144), (170, 170, 172)]
LENS = [(150, 181, 214), (150, 181, 214), (191, 212, 235), (226, 239, 253)]


def owner_colours():
    """The colours of the owner's telescope (for the checker: every ramp tone above that is not marked as an extension
    must be one of them)."""
    with Image.open(TELESCOPE) as image:
        return {pixel[:3] for _count, pixel in image.convert("RGBA").getcolors(maxcolors=4096) if pixel[3] > 0}


# The iron ramp's first and last tones are extensions (not in the owner's file).
EXTENDED = {IRON[0], IRON[3]}


def observatory_sheet():
    uvs, sizes = celestial.SIZES["observatory"]
    materials = {"plinth": WOOD, "ring": BRASS, "pillar": IRON, "yoke": BRASS, "arm": BRASS, "tube": BRASS,
                 "eyepiece": IRON, "lens": LENS}
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for name, uv in uvs.items():
        ecology_art._paint_box(img, uv, sizes[name], materials[name], zlib.crc32(f"observatory.{name}".encode()))
    # Dark bands round the tube where its sections join, across its sides, top and bottom.
    (u, v), (w, h, d) = uvs["tube"], sizes["tube"]
    for z in (3, 7):
        for y in range(v + d, v + d + h):
            img.putpixel((u + z, y), WOOD[2] + (255,))  # the west side runs along the tube's length
            img.putpixel((u + d + w + (d - 1 - z), y), WOOD[2] + (255,))  # the east side, mirrored
        for x in range(u + d, u + d + w):
            img.putpixel((x, v + z), WOOD[2] + (255,))  # the top
            img.putpixel((x + w, v + z), WOOD[2] + (255,))  # the bottom
    return img


ICONS = ["astrolabe", "observatory"]


def textures():
    out = {("item", name): item_icons.draw(name) for name in ICONS}
    out[("block", "observatory")] = observatory_sheet()
    return out
