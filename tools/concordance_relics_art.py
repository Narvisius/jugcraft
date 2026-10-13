"""Textures for roadmap step 20, Relic Lore (tools/concordance_relics.py): the four relics' item icons and the
Reliquary Shrine's, the shrine's GeckoLib sheet, and the Owlsight Circlet's worn layer. tools/concordance_art.py
includes these in its textures().

Relics are old magical things of the Practitioner stage, so they keep the Arcane Concordance's palette (brass, gold,
violet and amethyst on dark stone), not the higher tiers' dieselpunk (docs/ART_DIRECTION.md):

- the item icons are 16x16 maps in tools/item_icons/ (docs/ITEM_ICONS.md), drawn by item_icons.draw:
  - wardlight.txt: the Wardlight Lantern, a small hexagonal lantern, its gold cage round panes lit a pale violet-white,
    under a brass spire with an amethyst crystal finial and on a brass foot. The finial, the hexagonal (chamfered)
    glass and the bright ward light tell it from the Kindled Lantern's boxy brass cage, ring and dark glass;
  - hearthstone.txt: the Hearthstone, a warm red heart-shaped stone glowing like an ember at its heart, hung by its two
    lobes from a gold chain whose loop shows above it, so the whole necklace is drawn;
  - stormglass.txt: the Stormglass Orb, a glass sphere of dark blue-grey storm cloud with a pale lightning bolt
    zigzagging through it, in a copper mount (the lightning rod's copper) whose claws grip its lower sides;
  - owlsight_circlet.txt: the Owlsight Circlet, a thin gold circlet seen a little from above, with a green eye gem
    (an Eye of Ender's) set at its front, a dark pupil across its middle;
  - reliquary_shrine.txt: the Reliquary Shrine as an item, its blackstone plinth, amethyst cradle, two lit candles and
    the violet crystal floating over them;
- block/reliquary_shrine: the 64x64 sheet of the shrine's GeckoLib model (tools/concordance_relic_models.py), painted
  box by box from concordance_relic_models.SIZES (each box's UV origin and size; box UV: the top and bottom in a row d
  high, then the four sides h high). Each box takes a flat coat from concordance_ecology_art._paint_box (the top lit,
  the sides mid, the bottom dark; no specks) and then placed detail, flat tones lit from the top left, no noise or wear
  ("Texturing: keep it clean"): the polished blackstone base, step and column are chiselled, every face lit along its
  top and left edges and shaded along its bottom and right, so where two faces meet at a corner one lit and one shaded
  edge show; the column's four faces each carry a small amethyst inlay; the amethyst cradle is chiselled the same way,
  with a recessed well under the floating focus; the cream candles are lit at their rims; the flames are warm
  yellow-orange, palest on top; the focus is a bright pale-violet crystal, its left facet lit and its right shaded;
  the halo is brass. Everything outside the boxes' regions stays transparent;
- entity/equipment/humanoid/owlsight_circlet: the 64x32 layer drawn on the head when the circlet is worn
  (assets/jugcraft/equipment/owlsight_circlet.json), on the humanoid armour layout's head box (8x8x8 at UV 0,0): a thin
  gold band two texels high round all four sides at the forehead (the sides' second and third rows), lit along its top
  with a glint at each of the head's corners, and at the centre of the front face the green eye, its pupil dark, in a
  small dark-gold setting that rises a texel above the band and hangs a texel below it, still clear of the face's eyes.
  Everything else, the top of the head among it, stays transparent.

Provenance (to be recorded with the feature's art, docs/features/arcane-concordance-relics.md): the blackstone's colours
are the owner's, from their library (art/owner-library/README.md): BLACKSTONE's two darker tones are colours of
art/owner-library/originals/Blocks/biomes and tree blocks/cut_black_sandstone.png (its darkest and its commonest) and
its three lighter tones the three colours of art/owner-library/originals/Blocks/biomes and tree
blocks/blackstone_spines.png (owner_colours() reads them back for a check; only the colours are used, no pixels or
layouts). The circlet's worn band is adapted from the owner's crown, art/owner-library/originals/Blocks/Trinket Type
Mod/entity/crown.png (a 32x32 worn trinket: a gold band four texels high with a pointed top row and a 2x2 red gem): its
three golds (CROWN_GOLD) are kept, and its two plain lower rows (a lit row over the band's own tone) become the
circlet's band, re-laid round the 32 texels of the humanoid head box's sides; its points and its patterned row are left
off (a circlet, not a crown), a glint marks each of the head's corners instead, and its red gem is replaced by a bigger
green eye in the icon's moss ramp, in a small setting of one added darker gold (icon_materials.GOLD's dark tone) so the
eye stands clear of the band. The other colours are Jugcraft's own ramps, imported rather than copied:
tools/concordance_art.py's VIOLET, GEM, BRASS, PAGE and PANE_LIT (the Concordance's violet, amethyst, brass, paper and
lit lantern glass) and tools/icon_materials.py's GOLD and MOSS; the focus's violet-white and the wax's pale rim are the
two colours added here. The five icon maps are drawn fresh, in tools/icon_materials.py's materials (their `# materials:`
lines). The library was also searched for lanterns, orbs, gems, necklaces, amulets, circlets, candles, altars and
shrines to reuse, and nothing else is used, all under art/owner-library/originals/Blocks/: lantern.png, cagelamp*.png,
electric_lantern*.png, Weapon Smithing/foundry/lantern.png, Weapon Smithing/smeltery/lantern.png and
Guns/item/plasma_lantern.png are iron, copper, brick and plasma lamps of other tiers and forms;
Guns/item/lightning_in_a_bottle.png is a corked bottle of sparks, not an orb; Trinket Type Mod/entity/amulet.png is a
worn amulet's sheet, not a necklace icon, and the library's only other necklace files are the empty necklace slot's
outlines; Guns/item/guano_candle.png and Guns/block/guano_candle*.png are a brown guano candle, not cream wax; the
reference sheets "crystal and magic staffs textures.jpg" and "more crystal textures.jpg" are crystals and staffs, not
these objects; and no altar, shrine, plinth, circlet or orb is in the library. No library file is read when the textures
are made, the originals are untouched, and nothing is traced, sampled or recoloured from Mojang's files: vanilla's
polished blackstone, candles and amethyst are followed for their manner only.
"""

import os

from PIL import Image

import concordance_ecology_art as ecology_art
import concordance_relic_models as relic_models
import icon_materials
import item_icons
from concordance_art import BRASS, GEM, PAGE, PANE_LIT, VIOLET

HERE = os.path.dirname(os.path.abspath(__file__))
LIBRARY = os.path.join(HERE, "..", "art", "owner-library", "originals", "Blocks")
BLACK_SANDSTONE = os.path.join(LIBRARY, "biomes and tree blocks", "cut_black_sandstone.png")
BLACKSTONE_SPINES = os.path.join(LIBRARY, "biomes and tree blocks", "blackstone_spines.png")
CROWN = os.path.join(LIBRARY, "Trinket Type Mod", "entity", "crown.png")

# ------------------------------------------------------------------------------------------------------- palettes

# Polished blackstone, darkest first: two colours of the owner's black sandstone (its darkest, for seams and the
# shadow under an edge, and its commonest, for shaded edges and undersides) and the three colours of their blackstone
# spines (the sides' own tone, lit tops and edges, and the chiselled lit edge), so the stone is near-black with a faint
# violet cast.
BLACKSTONE = [(33, 32, 36), (40, 39, 42), (49, 44, 54), (60, 57, 71), (78, 75, 84)]
DEEP, SHADE, STONE, LIT, EDGE = BLACKSTONE
# The cradle's amethyst, darkest first: the Concordance's violet (its well's deepest shadow) and gem ramps
# (tools/concordance_art.py VIOLET and GEM).
CRADLE = [VIOLET[2], GEM[0], GEM[1], GEM[2], GEM[3]]
# The focus: a bright pale-violet crystal, the gem ramp's light end up to a violet-white.
FOCUS = [GEM[1], GEM[2], GEM[3], (246, 240, 255)]
# Cream candle wax (the Concordance's paper, and a pale rim) and its flame (the lit lantern glass's warm yellow-orange).
WAX = [PAGE[0], PAGE[1], PAGE[2], (248, 242, 222)]
FLAME = [PANE_LIT[0], PANE_LIT[1], PANE_LIT[2], PANE_LIT[3]]
# The halo's brass, darkest first.
HALO = [BRASS[1], BRASS[2], BRASS[3], BRASS[4]]
# The stone's inlay: a small amethyst lozenge, its upper-left facet lit.
INLAY = {(0, 0): GEM[2], (1, 0): GEM[1], (0, 1): GEM[1], (1, 1): VIOLET[3]}

# The owner's crown's three golds (provenance above): the band's own tone (the crown's lowest row), its lit row (the
# crown's row above that) and a glint (the crown's points); and icon_materials.GOLD's dark tone, added for the
# setting's rim.
CROWN_GOLD = [(238, 185, 53), (246, 203, 96), (255, 233, 179)]
BAND, BAND_LIT, GLINT = CROWN_GOLD
BAND_DARK = icon_materials.GOLD.dark
# The eye: the icon's moss ramp (icon_materials.MOSS), so the worn gem matches the icon's.
EYE = icon_materials.MOSS


def owner_colours():
    """The colours of the owner's files the textures take colours from, as sets of RGB tuples: (black sandstone,
    blackstone spines, crown). For a check only; the textures never read the library."""
    def colours(path):
        with Image.open(path) as image:
            return {pixel[:3] for _count, pixel in image.convert("RGBA").getcolors(maxcolors=4096) if pixel[3] > 0}
    return colours(BLACK_SANDSTONE), colours(BLACKSTONE_SPINES), colours(CROWN)


# ------------------------------------------------------------------------------------------------- small helpers

def _put(img, x, y, colour):
    img.putpixel((x, y), tuple(colour[:3]) + (255,))


def faces(uv, size):
    """A box-UV cube's faces as (x, y, w, h) rectangles: (top, bottom, [the four sides])."""
    (u, v), (w, h, d) = uv, size
    return ((u + d, v, w, d), (u + d + w, v, w, d),
            [(u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h)])


def _fill(img, rect, colour):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            _put(img, x, y, colour)


def _frame(img, rect, top, left, right, bottom):
    """A one-texel frame round a face, drawn left, right, bottom, then top, so the top row wins the top corners and
    the bottom row the bottom ones."""
    x0, y0, w, h = rect
    _fill(img, (x0, y0, 1, h), left)
    _fill(img, (x0 + w - 1, y0, 1, h), right)
    _fill(img, (x0, y0 + h - 1, w, 1), bottom)
    _fill(img, (x0, y0, w, 1), top)


# ------------------------------------------------------------------------------------------------ the shrine sheet

def _stone(img, uv, size, inlay=False):
    """A polished blackstone box, chiselled: a face's top row and left column lit, its bottom row and right column
    shaded, so where two sides meet at a corner one lit and one shaded edge show. The top is lit and chiselled; the
    underside stays the coat's plain dark. With `inlay`, a small amethyst lozenge is set in the middle of each side."""
    top, _bottom, sides = faces(uv, size)
    _frame(img, top, EDGE, EDGE, SHADE, SHADE)
    for side in sides:
        x0, y0, w, h = side
        _frame(img, side, EDGE, LIT, SHADE, DEEP if h > 2 else STONE)
        if inlay:
            for (dx, dy), colour in INLAY.items():
                _put(img, x0 + w // 2 - 1 + dx, y0 + h // 2 - 1 + dy, colour)


def _cradle(img, uv, size):
    """The amethyst cradle: chiselled like the stone, its sides lit along their top edge, and its top with a recessed
    well under the floating focus (shaded on its upper and left inner walls, lit on its lower and right ones)."""
    top, _bottom, sides = faces(uv, size)
    _frame(img, top, CRADLE[4], CRADLE[4], CRADLE[1], CRADLE[1])
    x0, y0, w, h = top
    well = (x0 + 2, y0 + 2, w - 4, h - 4)
    _fill(img, well, CRADLE[2])
    _frame(img, well, CRADLE[0], CRADLE[0], CRADLE[3], CRADLE[3])
    for side in sides:
        _frame(img, side, CRADLE[4], CRADLE[3], CRADLE[1], CRADLE[2])


def _focus(img, uv, size):
    """The focus crystal: on each side its left facet lit and its right facet shaded, brightest along the top."""
    top, _bottom, sides = faces(uv, size)
    _fill(img, top, FOCUS[3])
    for x0, y0, w, h in sides:
        _fill(img, (x0, y0, 1, h), FOCUS[2])
        _fill(img, (x0 + 1, y0, w - 1, h), FOCUS[1])
        _fill(img, (x0, y0, w, 1), FOCUS[3])
        _put(img, x0 + w - 1, y0 + h - 1, FOCUS[0])


def _candle(img, uv, size):
    """A cream candle: its rim pale, its body cream, its foot a shade darker."""
    _top, _bottom, sides = faces(uv, size)
    for x0, y0, w, h in sides:
        _fill(img, (x0, y0 + h - 1, w, 1), WAX[1])


def shrine_sheet():
    uvs, sizes = relic_models.SIZES["reliquary_shrine"]
    coats = {"base": BLACKSTONE[:4], "step": BLACKSTONE[:4], "column": BLACKSTONE[:4], "cradle": CRADLE[:4],
             "candle": WAX, "flame": FLAME, "focus": FOCUS, "halo_x": HALO, "halo_z": HALO}
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for name, uv in uvs.items():
        # The flat coat (ecology_art._paint_box): the top lit, the sides mid with a lit top row, the bottom dark. No
        # specks ("Texturing: keep it clean"), so its seed is never used.
        ecology_art._paint_box(img, uv, sizes[name], coats[name], 0, speckle=0)
    for name in ("base", "step", "column"):
        _stone(img, uvs[name], sizes[name], inlay=name == "column")
    _cradle(img, uvs["cradle"], sizes["cradle"])
    _candle(img, uvs["candle"], sizes["candle"])
    _focus(img, uvs["focus"], sizes["focus"])
    return img


# --------------------------------------------------------------------------------------------- the worn circlet

# The humanoid armour layout's head box: 8x8x8 at UV (0, 0); its four sides run x 0..32, y 8..16, the face x 8..16.
HEAD_UV, HEAD_SIZE = (0, 0), (8, 8, 8)
BAND_ROW = 9  # the band's lit row: the forehead, a texel under the top of the head (the band is two rows)
# The face (x 8..15) from the row above the band: the eye in its setting, all above the face's eyes (its fifth row).
# Symbols: G a glint, l the band's lit row, b its own tone, D the dark gold of the setting's rim; h I i e o the eye's
# highlight, light, mid, dark and pupil; . leaves what is there (the band, or transparency).
FRONT = [
    "...DD...",
    "GlDhIDll",
    "bbIooibb",
    "..DieD..",
]
FRONT_TONES = {"G": GLINT, "l": BAND_LIT, "b": BAND, "D": BAND_DARK, "h": EYE.highlight, "I": EYE.light,
               "i": EYE.mid, "e": EYE.dark, "o": EYE.outline_dark}


def circlet_layer():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    _top, _bottom, sides = faces(HEAD_UV, HEAD_SIZE)
    for x0, _y0, w, _h in sides:
        _fill(img, (x0, BAND_ROW, w, 1), BAND_LIT)
        _fill(img, (x0, BAND_ROW + 1, w, 1), BAND)
        _put(img, x0, BAND_ROW, GLINT)  # a glint where the band turns each of the head's corners
    for dy, row in enumerate(FRONT):
        for dx, ch in enumerate(row):
            if ch != ".":
                _put(img, 8 + dx, BAND_ROW - 1 + dy, FRONT_TONES[ch])
    return img


# ----------------------------------------------------------------------------------------------------- all of them

ICONS = ["wardlight", "hearthstone", "stormglass", "owlsight_circlet", "reliquary_shrine"]


def textures():
    """Every texture as {(kind, name): image}: the five item icons, the shrine's sheet and the circlet's worn layer."""
    out = {("item", name): item_icons.draw(name) for name in ICONS}
    out[("block", "reliquary_shrine")] = shrine_sheet()
    out[("entity/equipment/humanoid", "owlsight_circlet")] = circlet_layer()
    return out
