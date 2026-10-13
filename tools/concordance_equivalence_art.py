"""Textures for roadmap step 21, Assay: bounded material equivalence (tools/concordance_equivalence.py,
docs/features/arcane-concordance-equivalence.md): the Assayer's Scale's block texture and its item icon.

The Assayer's Scale is the balance on which a Balancewright weighs mundane materials exactly, dissolves them into grains
of Prima Materia and forms more from them: a brass pillar, beam and finial on a dark-oak plinth, with a brass pan
hanging on an iron chain from each end of the beam. It is a Practitioner's instrument, in the Arcane Concordance's dark
wood, brass and violet, with none of the higher tiers' dieselpunk and no heavy steampunk (docs/ART_DIRECTION.md). Its
block model (tools/concordance_equivalence_models.py) is built from boxes and reads one 16x16 texture of four 8x8
material regions, REGIONS there; block() paints each region where REGIONS puts it. Every face of the model samples a
rectangle from its region's top-left corner, sized to the face, so each region is drawn to read well cropped that way.
The texture is opaque, in flat tones lit from the top left, with no noise or wear ("Texturing: keep it clean"):

- wood (0,0)-(8,8), the plinth: its top shows the whole region (stretched over ten pixels), its four sides the top two
  rows. It is two dark-oak boards of four rows, docs/NATURAL_TEXTURES.md's planks at the plinth's size: a lit top row,
  two rows of the wood's tone with short grain dashes, a dark seam. The first board's two upper rows stay plain, so
  each side reads as one lit edge over the wood's tone; the second board has the butt joint (a dark pixel with a lit
  one beside it). At the region's centre, where the pillar stands, a 2x2 amethyst socket is inlaid (lit on its upper
  left), so the pillar rises from a ring of violet: the Concordance's one cue on the block;
- brass (8,0)-(16,8): a bevelled brass plate, its top row and left column lit, its right column and bottom row shaded.
  Its top row is the beam (all four long faces), the pans' rims and the top of the finial; its left column the pillar,
  whose last pixel, from the shaded bottom row, is a darker foot where it meets the plinth; its top-left 2x2 the finial
  (lit, with its lower right a tone down) and 4x4 the pans' undersides;
- iron (0,8)-(8,16): two dark iron chains, face-on links (lit on the left) alternating with edge-on links one texel
  apart, on the deepest shadow. Each chain of the model is half a pixel thick and four tall and shows the first half of
  the region's first column: two lit links over two dark gaps;
- pan (8,8)-(16,16): a pan's top is four pixels square and shows the region's top-left 4x4, so the dish is drawn there
  whole and repeated over the region: a rim lit along its top and left and a tone down along its bottom and right,
  round a bowl darker than the rim, deepest under the rim's upper-left edge (a recess, shaded the other way round from
  a raised panel);
- the item icon: a 16x16 map, tools/item_icons/assayers_scale.txt (docs/ITEM_ICONS.md), drawn by item_icons.draw: the
  whole balance from the front, upright, its pillar in its violet socket on the plinth, the beam level with its
  finial, and the two pans hanging on two-link chains, clear of the pillar and above the plinth.

Provenance: everything here is drawn fresh, by code and in a text map, with no randomness, so every run writes the same
pixels. The colours are Jugcraft's own ramps, imported rather than copied, so the scale stays in step with the
Concordance's other Practitioner pieces: OAK_DARK, BRASS and GEM from tools/concordance_art.py (the Lampwright's
Bench's dark oak and brass and the Concordance's violet, as the Courier Post and the Artificer's Bench use them), and
arms_pixel.CHAIN, the dark iron the item icons draw chains in (their `c` and `C`), so the block's chains match the
icon's. The icon map takes its colours from tools/icon_materials.py (its `# materials:` line: brass, dark_wood,
amethyst) and the chain's from arms_pixel.CHAIN. The owner's library (art/owner-library/catalog/files.csv) was searched
for a scale, balance, weight, pan, dish, chain, brass or dark-oak asset to reuse or adapt, and none is used, all under
art/owner-library/originals/Blocks/: it holds no balance, weighing scale, weight or chain (its "scale" files are
Weapon Smithing's dragon scales); Weapon Smithing/Items/tool/melting_pan/head.png is a grey, speckled pan the size of
an item, not a four-pixel brass dish; farming and food textures/dark_oak_cabinet_*.png are a cabinet's panelled doors
and frame, not planks, in other tones than the Concordance's dark oak; and its brass (Guns/block/treated_brass_*.png,
Big Cannons and Mounted Guns/textures/block/brass_block.png, and the telescope's, Airships and Planes/entity/
telescope.png) is a paler or more orange metal than the Concordance's. No library file is read, copied or recoloured
here, and the originals are untouched. Nothing is traced, sampled or recoloured from Mojang's files: vanilla's planks
are followed for their manner only.
"""

from PIL import Image

import concordance_equivalence_models as models
import item_icons
from arms_pixel import CHAIN
from concordance_art import BRASS, GEM, OAK_DARK

# The block's symbols. Digits are the dark oak, darkest first (0 seams and the joint, 1 grain dashes, 2 the wood's own
# tone, 3 a board's lit top row, 4 the joint's lit pixel); v V X the amethyst inlay, darkest first; o n m Y Z brass,
# darkest first; k d i C the chains' iron (k the deepest shadow, d a link's shaded side, i an edge-on link, C a lit
# face-on link).
TONES = {
    "0": OAK_DARK[0], "1": OAK_DARK[1], "2": OAK_DARK[2], "3": OAK_DARK[3], "4": OAK_DARK[4],
    "v": GEM[0], "V": GEM[1], "X": GEM[2],
    "o": BRASS[0], "n": BRASS[1], "m": BRASS[2], "Y": BRASS[3], "Z": BRASS[4],
    "k": CHAIN.outline_dark, "d": CHAIN.dark, "i": CHAIN.mid, "C": CHAIN.light,
}

# The plinth (rows 0-3 the first board, 4-7 the second). Rows 0 and 1 are the sides' lit edge and the wood's tone, kept
# plain; row 2 holds the first board's grain dashes, row 3 its seam. The second board's butt joint is column 5, with
# its lit pixel to the right; the amethyst socket is the 2x2 at columns and rows 3-4, round the pillar's foot.
WOOD = [
    "33333333",
    "22222222",
    "11222112",
    "000XV000",
    "333Vv043",
    "22222032",
    "21112031",
    "00000000",
]

# The brass plate: the top row (the beam, the pans' rims, the finial) and the left column (the pillar) lit, the right
# column and the bottom row shaded.
BRASS_PLATE = [
    "YYYYYYYY",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "mnnnnnno",
]

# Two chains, columns 0-2 and 4-6, each a face-on link (C C d: lit on the left, shaded on the right) over an edge-on
# link (i, between gaps), on the deepest shadow (k). The model's chains show column 0: C k C k.
IRON = [
    "CCdkCCdk",
    "kikkkikk",
] * 4

# The pan's dish, 4x4 (the pans' tops show this much), repeated over the region: the rim (Z lit, Y a tone down, m the
# far corner) round the bowl (n in the rim's shadow, m).
DISH = [
    "ZZZY",
    "ZnmY",
    "ZmmY",
    "YYYm",
]
PAN = [row * 2 for row in DISH] * 2

REGION_ROWS = {"wood": WOOD, "brass": BRASS_PLATE, "iron": IRON, "pan": PAN}


def block():
    """The Assayer's Scale's 16x16 block texture: each region of models.REGIONS painted from REGION_ROWS."""
    if set(REGION_ROWS) != set(models.REGIONS):
        raise ValueError(f"the scale's regions are {sorted(models.REGIONS)}, painted {sorted(REGION_ROWS)}")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for name, rows in REGION_ROWS.items():
        u0, v0, u1, v1 = models.REGIONS[name]
        if len(rows) != v1 - v0 or any(len(row) != u1 - u0 for row in rows):
            raise ValueError(f"region {name} is {u1 - u0}x{v1 - v0}, painted {[len(row) for row in rows]}")
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                img.putpixel((u0 + x, v0 + y), tuple(TONES[ch]) + (255,))
    if any(img.getpixel((x, y))[3] != 255 for x in range(16) for y in range(16)):
        raise ValueError("the scale's regions leave part of its texture unpainted; a block texture is opaque")
    return img


ICONS = ["assayers_scale"]


def textures():
    """Every texture as {(kind, name): image}: the block texture and the item icon."""
    out = {("block", "assayers_scale"): block()}
    out.update({("item", name): item_icons.draw(name) for name in ICONS})
    return out
