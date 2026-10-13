"""Textures for roadmap step 25, the Concord Spire: the Spire Heart's GeckoLib sheet and its item icon.

The Spire Heart is the keystone the Arcane Concordance's endgame project is founded at and raised around: four Ley
Pylons and four Warding Stones stand round it, a column of polished deepslate rises from its top and a crown caps the
column. It is the Architect tier's, the highest, so it leans dieselpunk rather than steampunk (docs/ART_DIRECTION.md):
heavy, engineered and clean, blackened steel straps with heavy brass bolts on dark dressed stone, round a violet-white
core in a turning brass cage. Every texture is in flat tones lit from the top left, a lit top and left edge and a shaded
bottom and right one on every face, no noise and no wear ("Texturing: keep it clean"), drawn by code with no randomness.

- block/spire_heart: the 64x64 sheet of the heart's GeckoLib model (tools/concordance_spire_models.py), painted box by
  box from concordance_spire_models.SIZES (each box's UV origin and size; box UV, as GeckoLib 5.5.7 reads it: the top
  and bottom in a row d high, then the east, north, west and south sides h high). Every face's rectangle is painted to
  its corners; everything outside the boxes' regions stays transparent and is never sampled:
  - the foot (dark dressed stone, the full block wide): each side two dressed stones of eight, every stone lit along
    its top row and left column with a glint on that corner and shaded down its right column, so the joint between two
    stones, and every corner where two sides meet, shows one shaded and one lit edge. Its top is the stone's tone,
    chiselled round the edge (only that edge shows, half a pixel round the lower strap); its underside is the seam's
    dark;
  - the straps (one region for both): blackened steel, every side a lit top row (a sheen at its left end) over the
    plate's tone, shaded down its right end, with two heavy brass bolts, two texels square, lit at the top left, where
    the strap crosses a post. The top face is the lower strap's: lit round its edge (the lip half a pixel proud of the
    posts) and, in the middle where the chamber's floor is, a pool of violet light under the gem, two rings brightening
    to the middle. The underside is the upper strap's: the chamber's ceiling, in the steel's shade with a dimmer pool;
  - the posts (one region for all four): dark dressed stone in two courses of four, each course lit along its top row
    (a glint at its left) and down its left column, shaded down its right column and along its foot, so the posts'
    outer faces and the window jambs read as coursed masonry;
  - the top plate, the seat the column stands on: dark steel lit round its top and left edges (a sheen on that corner)
    and shaded round the others, a heavy brass bolt in each corner, and in the middle the founding mark, a violet ring
    ten texels across inlaid round a small violet-white lens, which the column covers once it is raised. Its sides are
    a lit row over the plate's tone, clean; its underside is the steel's shade;
  - the cage (brass): every bar lit on top, its sides the brass's mid tone, darker underneath; each rib lit at its
    top and darker at its foot. The marked rib has a violet stud in the middle of each side (the gem's light tone), the
    one mark that breaks the cage's symmetry, so its turn reads;
  - the gem: violet-white, every face lit at one corner (a near-white glint and the glow's light tone along the two
    edges from it) over the glow's mid tone, darkening at the far corner, so its facets catch the light by turns as it
    spins.
- the item icon: a 16x16 map, tools/item_icons/spire_heart.txt (docs/ITEM_ICONS.md), drawn by item_icons.draw: the
  heart upright, from the front a little from above. Its steel cap overhangs the stone posts by a pixel, lit along the
  left of its top and bolted in brass over each post; between the posts the violet-white gem, lit at its top left,
  floats in the dark violet of its chamber between two of the cage's brass ribs; under it the lower strap, lit along
  its top and bolted, and the stone foot as wide as the cap. The flat steel top, the bolted straps and the caged gem
  tell it from the Spirit Anchor (a roofed stone lantern), the Reliquary Shrine (a plinth under candles and a floating
  crystal), the Circle Anchor (a crystal floating over a ring), the Conclave Lectern (a slanted desk) and the Ley Pylon
  (a stone plinth under a copper frame and a blue crystal).

The palettes are imported from the Concordance's modules, not copied: STONE's four darker tones (tools/
concordance_ritual_art.py), the Ley Pylons' dark dressed stone, as the Conclave Lectern's plinth takes them, so the
heart matches the pylons round it; BRASS (tools/concordance_art.py), the Concordance's brass; FOCUS (tools/
concordance_relics_art.py, the Reliquary Shrine's violet-white crystal) under GEM's darkest (tools/concordance_art.py)
for the gem and the marked rib's stud; VIOLET's three lighter tones (tools/concordance_art.py) for the light the gem
throws on the steel and for the founding ring. The steel is the one new ramp, STEEL below, because none of the
Concordance's ramps is a dark, cool steel: its five tones are the owner's (provenance).

Provenance: everything here is drawn fresh, by code and in a text map, with no randomness, so every run writes the same
pixels. The owner's library (art/owner-library/README.md, catalog/files.csv) was searched for keystones, pedestals,
plinths, pillars, altars, cores, crystals, lenses, cages, gyroscopes and rings, dark steel, deepslate, brass, rivets and
bolts and machine cores. One set is used, for its colours only: STEEL's five tones are colours of the owner's dark-steel
reactor blocks, art/owner-library/originals/Blocks/Big Cannons and Mounted Guns/textures/block/solidsteel.png (its
frame's shade (38, 40, 46), its fill (53, 57, 65) and its lit edge (70, 75, 84)) and reactorchambertop.png in the same
folder (its darkest, (18, 18, 21), in the lens's socket, and its lit inner edge (91, 96, 105)); owner_colours() reads
them back for a check. No pixels or layouts of them are copied: their fills are mottled in tones two to four steps
apart, which the clean manner does not use, so the steel is painted flat in these five tones with the steps between
them. reactorchambertop.png, a lit lens sunk in a dark steel plate, is also the reference for the top plate's lens
(drawn fresh, violet instead of its cyan). Nothing else in the library fits, all under
art/owner-library/originals/Blocks/: the other reactor and steel blocks of Big Cannons and Mounted Guns (machineframe,
steelopticfront, reactorporttop, largesteelcyllindertop, powerconduitface) are framed faces of the same steel whose
openings (a round port, a dark slot, a red ring, an orange dot) are not this core; reactorcasingside.png puts a tan
stone band across the steel; the cores and core items (green_core.png, quantumcoreanimatedtexture.png,
Guns/item/energy_core.png, plasma_core.png, empty_core.png, shulker_core.png, cog_heart.png, and Big Cannons and
Mounted Guns/textures/item/fusioncore.png, fissioncore.png and implosionlense.png) are hand-held or animated items of
other mods' kinds and colours; Guns/block/charged_amethyst_relay_base.png and its crystals are red, not violet;
Airships and Planes/item/gyroscope.png is an aircraft instrument, not a gimbal cage; cagelamp.png is a copper lamp;
the purple metals (Big Cannons and Mounted Guns/textures/block/opticfront_purple.png, purple_aluminum_plating.png,
armor_purple.png and sheetmetal_purple.png) are a bright magenta-violet paint, far from the Concordance's violet;
Weapon Smithing/geode/ender/block.png and cluster.png are a dark mottled ender crystal; the dark stones (Weapon
Smithing/foundry/scorched/ and smeltery/seared/, and the preview sheet previews/dark-teal-stone.png) are brown or
olive and noisy, not the pylons' violet-grey; the deepslate ores carry ore over a deepslate ground that may be Mojang's,
so their colours are not taken; the brass blocks (Big Cannons and Mounted Guns/textures/block/brass_block.png, and
Guns/block/treated_brass_block.png, treated_brass_plates.png and ancient_brass_block.png) are paler or more orange,
with sheens or speckle, and BRASS is kept so the Concordance has one brass; and the reference sheets "technical glasses
and lenses textures.jpg" and "crystal blocks, shards, and dust textures.jpg" show goggles, gems and dust, not this
block. No library file is read when the textures are made, the originals are untouched, and nothing is traced,
sampled or recoloured from Mojang's files: vanilla's polished deepslate is followed for its value only, so the heart
sits under the column.
"""

import os

from PIL import Image

import concordance_spire_models as models
import item_icons
from concordance_art import BRASS, GEM, VIOLET
from concordance_relics_art import FOCUS, faces
from concordance_ritual_art import STONE

HERE = os.path.dirname(os.path.abspath(__file__))
LIBRARY = os.path.join(HERE, "..", "art", "owner-library", "originals", "Blocks", "Big Cannons and Mounted Guns",
                       "textures", "block")
SOLID_STEEL = os.path.join(LIBRARY, "solidsteel.png")
REACTOR_CHAMBER = os.path.join(LIBRARY, "reactorchambertop.png")

# ------------------------------------------------------------------------------------------------------- palettes

# Dark dressed stone, darkest first: STONE's darker four (the seam and shaded edges, the stone's tone, lit edges, a
# glint).
SEAM, ROCK, DRESSED, GLINT = STONE[0], STONE[1], STONE[2], STONE[3]
# Blackened steel, darkest first: the owner's dark steel (provenance): the deepest shadow, the shade, the plate's tone,
# the lit edge and a sheen.
STEEL = [(18, 18, 21), (38, 40, 46), (53, 57, 65), (70, 75, 84), (91, 96, 105)]
DEEP, SHADE, PLATE, LIT, SHEEN = STEEL
# The core's glow, darkest first: GEM's darkest under FOCUS (the shrine's violet-white crystal).
CORE = [GEM[0]] + list(FOCUS)
# The light it throws on the steel, darkest first, and the founding ring's inlay.
POOL = [VIOLET[2], VIOLET[3], VIOLET[4]]
# The cage's and the bolts' brass, darkest first.
B1, B2, B3, B4 = BRASS[1], BRASS[2], BRASS[3], BRASS[4]
# A heavy bolt, two texels square, lit at the top left.
BOLT = [[B4, B3], [B3, B2]]


def owner_colours():
    """The colours of the owner's two dark-steel blocks STEEL takes its tones from, as one set of RGB tuples. For a check
    only; the textures never read the library."""
    found = set()
    for path in (SOLID_STEEL, REACTOR_CHAMBER):
        with Image.open(path) as image:
            found |= {pixel[:3] for _count, pixel in image.convert("RGBA").getcolors(maxcolors=4096) if pixel[3] > 0}
    return found


# ------------------------------------------------------------------------------------------------- small helpers

def _put(img, x, y, colour):
    img.putpixel((x, y), tuple(colour[:3]) + (255,))


def _fill(img, rect, colour):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            _put(img, x, y, colour)


def _frame(img, rect, top, left, right, bottom, corner=None):
    """A one-texel frame round a face, drawn left, right, bottom, then top, so the top row wins the top corners and the
    bottom row the bottom ones; `corner` marks the top-left texel (a glint)."""
    x0, y0, w, h = rect
    _fill(img, (x0, y0, 1, h), left)
    _fill(img, (x0 + w - 1, y0, 1, h), right)
    _fill(img, (x0, y0 + h - 1, w, 1), bottom)
    _fill(img, (x0, y0, w, 1), top)
    if corner is not None:
        _put(img, x0, y0, corner)


def _stamp(img, x0, y0, grid):
    for dy, row in enumerate(grid):
        for dx, colour in enumerate(row):
            _put(img, x0 + dx, y0 + dy, colour)


def _pool(img, rect, tones, radii):
    """A round pool of light at a face's middle: tones[i] where a texel's centre lies within radii[i], darkest and
    widest first."""
    x0, y0, w, h = rect
    cx, cy = w / 2, h / 2
    for y in range(h):
        for x in range(w):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            for tone, radius in zip(tones, radii):
                if d <= radius:
                    _put(img, x0 + x, y0 + y, tone)


def _ring(size, across):
    """The texels of a one-texel ring `across` wide (its outline included) centred on a size x size face: the outline
    round a disc drawn in the rows of docs/ITEM_ICONS.md, rule 7."""
    rows = {6: [2, 4, 4, 2], 8: [4, 6, 6, 6, 6, 4], 10: [4, 6, 8, 8, 8, 8, 6, 4]}[across]
    top = (size - len(rows)) // 2
    disc = {(x, top + i) for i, width in enumerate(rows) for x in range((size - width) // 2, (size + width) // 2)}
    return sorted({(x + dx, y + dy) for x, y in disc for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))} - disc)


def _region(name):
    uvs, sizes = models.SIZES["spire_heart"]
    return faces(uvs[name], sizes[name])


# ------------------------------------------------------------------------------------------------ the heart's sheet

def _foot(img):
    """Dark dressed stone: each side two stones of eight, lit along the top and down the left (a glint on that corner),
    shaded down the right; the top chiselled round its edge; the underside the seam's dark."""
    top, bottom, sides = _region("foot")
    _fill(img, top, ROCK)
    _frame(img, top, DRESSED, DRESSED, SEAM, SEAM, GLINT)
    _fill(img, bottom, SEAM)
    for x0, y0, w, h in sides:
        for stone in range(0, w, 8):
            _stamp(img, x0 + stone, y0, [[GLINT] + [DRESSED] * 6 + [SEAM], [DRESSED] + [ROCK] * 6 + [SEAM]])


def _strap(img):
    """Blackened steel, both straps: lit top rows and shaded right ends, a brass bolt over each post; the top the lower
    strap's lip, sills and floor with the core's pool of light, the underside the upper strap's ceiling."""
    top, bottom, sides = _region("strap")
    for x0, y0, w, h in sides:
        _fill(img, (x0, y0, w, 1), LIT)
        _fill(img, (x0, y0 + 1, w, h - 1), PLATE)
        _fill(img, (x0 + w - 1, y0, 1, h), SHADE)
        _put(img, x0, y0, SHEEN)
        for bolt in (1, w - 3):
            _stamp(img, x0 + bolt, y0, BOLT)
    _fill(img, top, PLATE)
    _frame(img, top, LIT, LIT, SHADE, SHADE, SHEEN)
    _pool(img, top, POOL, (4.3, 3.0, 1.6))
    _fill(img, bottom, SHADE)
    _frame(img, bottom, PLATE, PLATE, DEEP, DEEP)
    _pool(img, bottom, POOL[:2], (3.4, 1.6))


# A post's side, two courses of four: G a glint, d the lit edge, c the stone, b the seam and shade.
POST = ["Gdd", "dcb", "dcb", "bbb", "Gdd", "dcb", "dcb", "bbb"]
POST_TONES = {"G": GLINT, "d": DRESSED, "c": ROCK, "b": SEAM}


def _posts(img):
    top, bottom, sides = _region("post")
    _fill(img, top, ROCK)
    _fill(img, bottom, ROCK)
    for x0, y0, w, h in sides:
        _stamp(img, x0, y0, [[POST_TONES[ch] for ch in row] for row in POST])


def _plate(img):
    """The seat: dark steel lit round its top and left edges, a bolt in each corner, the founding ring and its lens."""
    top, bottom, sides = _region("plate")
    x0, y0, w, h = top
    _fill(img, top, PLATE)
    _frame(img, top, LIT, LIT, SHADE, SHADE, SHEEN)
    for bx, by in ((2, 2), (w - 4, 2), (2, h - 4), (w - 4, h - 4)):
        _stamp(img, x0 + bx, y0 + by, BOLT)
    for x, y in _ring(w, 10):
        _put(img, x0 + x, y0 + y, POOL[1])
    _stamp(img, x0 + w // 2 - 1, y0 + h // 2 - 1, [[CORE[4], CORE[3]], [CORE[3], CORE[2]]])
    _fill(img, bottom, SHADE)
    for sx, sy, sw, sh in sides:
        _fill(img, (sx, sy, sw, 1), LIT)
        _fill(img, (sx, sy + 1, sw, sh - 1), PLATE)
        _fill(img, (sx + sw - 1, sy, 1, sh), SHADE)
        _put(img, sx, sy, SHEEN)


def _cage(img):
    """Brass: every bar lit on top, mid on its sides, darker underneath; the ribs lit at the top and darker at the
    foot, the marked one with a violet stud in the middle of each side."""
    for name in ("frame_x", "frame_z"):
        top, bottom, sides = _region(name)
        _fill(img, top, B4)
        _fill(img, bottom, B2)
        for side in sides:
            _fill(img, side, B3)
    for name, middle in (("rib", B3), ("rib_mark", CORE[2])):
        top, bottom, sides = _region(name)
        _fill(img, top, B4)
        _fill(img, bottom, B2)
        for x0, y0, _w, h in sides:
            column = [B4] + [B3] * (h - 2) + [B2]
            column[h // 2] = middle
            for dy in range(h):
                _put(img, x0, y0 + dy, column[dy])


# A face of the gem: lit at one corner (a glint and the light tone along the two edges from it), the glow's mid tone,
# darkest at the far corner. 4 the glint, 3 the light tone, 2 the mid tone, 1 the shade (indices into CORE).
GEM_FACE = ["432", "322", "221"]


def _gem(img):
    top, bottom, sides = _region("gem")
    for x0, y0, _w, _h in [top, bottom] + sides:
        _stamp(img, x0, y0, [[CORE[int(ch)] for ch in row] for row in GEM_FACE])


def heart_sheet():
    """The Spire Heart's 64x64 GeckoLib sheet: every face rectangle of every box in models.SIZES painted, nothing else."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    _foot(img)
    _strap(img)
    _posts(img)
    _plate(img)
    _cage(img)
    _gem(img)
    return img


ICONS = ["spire_heart"]


def textures():
    """Every texture as {(kind, name): image}, in order: the heart's GeckoLib sheet, then its item icon."""
    out = {("block", "spire_heart"): heart_sheet()}
    out.update({("item", name): item_icons.draw(name) for name in ICONS})
    return out
