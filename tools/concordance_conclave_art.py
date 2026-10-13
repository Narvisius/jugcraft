"""Textures for roadmap step 23, factions and the Starbound Conclave (tools/concordance_conclave.py,
docs/features/arcane-concordance-conclave.md): the Conclave Lectern's block texture and its item icon.

The Conclave Lectern is where a player swears the Starbound Conclave's oath, fulfils its commissions and gives to its
projects: a reading desk of dark oak leaning back on a square post with a brass collar, a brass lip along the desk's
lower edge and the Conclave's star chart open on it, over a plinth of dark stone. It is a Practitioner's piece, in the
Arcane Concordance's dark wood, brass and gold, violet and a deep blue for the sky, with none of the higher tiers'
dieselpunk and no heavy steampunk (docs/ART_DIRECTION.md). Its block model (tools/concordance_conclave_models.py) is
built from boxes and reads one 16x16 texture of four 8x8 material regions, REGIONS there; block() paints each region
where REGIONS puts it. Every face of the model samples a rectangle from its region's top-left corner, sized to the face
and stretched when the face is wider than the region, so each region is drawn to read well cropped that way. The
texture is opaque, in flat tones lit from the top left, with no noise or wear ("Texturing: keep it clean"):

- wood (0,0)-(8,8), the post and the desk: two upright dark-oak boards of four columns, docs/NATURAL_TEXTURES.md's
  planks stood on end so the grain runs up the post. Each board is lit down its left column and shaded down its right,
  so where two of the post's sides meet, one lit and one shaded edge show; between them is the wood's tone, with a
  grain dash on the first board. The top row is lit and the bottom row shaded. The post's four sides show the first
  board (the left four columns): its top row hides in the collar and its bottom row is a darker foot where the post
  meets the plinth. The desk's four edges show the top two rows, a lit edge over the two boards' ends; its top shows
  only a one-pixel frame round the chart (the lit left column, the shaded right column and bottom row; the lip covers
  the top row), and its underside the whole region, the second board with a butt joint (a dark row with a lit one under
  it). The chart's half-pixel edges show the top row's lit wood;
- brass (8,0)-(16,8): a bevelled brass plate, its top row and left column lit, its right column and bottom row shaded,
  with a glint on its top-left corner. The top row is the lip's four long faces and the collar's four sides; the glint
  is the lip's ends and the lit corner of each of the collar's sides; the top-left 5x5 is the collar's top and
  underside, a half-pixel ring of which shows round the post;
- chart (0,8)-(8,16), the chart's top, the whole region stretched over its 10x8 pixels and seen from above: deep
  blue-violet vellum with the Conclave's constellation, five gold-ink stars in a W, the middle one brightest, joined by
  faint violet lines a step lighter than the vellum, and three pale stars near its corners. Row 0 lies along the lip,
  so the reader standing there sees the W turned round, an M; the chart has no light direction that could turn wrong;
- plinth (8,8)-(16,16): a chiselled slab of dark dressed stone, its top row and left column lit (a glint on their
  corner), its right column and bottom row shaded, with a lighter and a darker clump of two pixels where the slab's top
  shows round the post. Its four sides show the top two rows (a lit edge over the stone's tone, lit at one end and
  shaded at the other), its top and underside the whole region;
- the item icon: a 16x16 map, tools/item_icons/conclave_lectern.txt (docs/ITEM_ICONS.md), drawn by item_icons.draw: the
  whole lectern, upright, seen from the reader's side a little from the right and above. The chart is a panel leaning
  back on the desk, a zigzag of gold stars on the vellum, the brass lip straight along its front edge and the desk's
  dark front face and right end beside it; a brass collar rings the post's top, the post's front is lit and its side
  shaded, and the plinth shows its lit top round the post's foot, its front and its shaded right side. The slant, the
  star chart and the stone plinth tell it from the Lampwright's Bench (a desk on four legs with a lens on an arm), the
  Assayer's Scale (a level beam with pans) and vanilla's lectern (oak, with no chart, collar or stone plinth).

Provenance: everything here is drawn fresh, by code and in a text map, with no randomness, so every run writes the same
pixels. The colours are Jugcraft's own ramps, imported rather than copied, and none is added: OAK_DARK and BRASS from
tools/concordance_art.py (the Lampwright's Bench's dark oak and brass, as the Assayer's Scale uses them), the stars'
gold being BRASS's two lightest tones; the darker four tones of STONE from tools/concordance_ritual_art.py, the dark
dressed stone of the Ley Pylon's plinth; SMOKED_GLASS from tools/icon_materials.py (the unlit Kindled Lantern's
blue-violet dark glass) for the vellum and the pale stars, so the block's chart is the icon's; and VIOLET from
tools/concordance_art.py for the constellation's faint lines. The icon map takes its colours from
tools/icon_materials.py (its `# materials:` line: lacquer for the stone, as the Reliquary Shrine's and the Spirit
Anchor's maps draw dark stone, then brass, dark_wood and smoked_glass). Its pose was laid out from renders of this
block's own model, then drawn by hand in the map. The owner's library (art/owner-library/catalog/files.csv) was searched
for a lectern, desk, book stand, star chart, map, astrolabe, sky or star texture, brass fittings, dark oak and dark
stone to reuse or adapt, and none is used, all under art/owner-library/originals/Blocks/: it holds no lectern, desk,
book stand, star chart or astrolabe (Big Cannons and Mounted Guns/textures/block/chartop.png is, despite its name, a
near-black, noisy top face of 64 colours, and Guns/item/sculk_tome.png a teal sculk book); Airships and
Planes/entity/telescope.png and item/telescope.png are the owner's telescope, whose orange brass and red-brown wood
are the Orrery Observatory's colours, not the Concordance's; farming and food textures/dark_oak_cabinet_front.png,
_side.png and _top.png are a cabinet's doors (with gold knobs), side and top, each framed in a dark border, in much
the browns of OAK_DARK, but as framed panels they fit neither a post nor a desk's boards, and OAK_DARK is kept so the
Concordance has one dark oak; the brass (Big Cannons and Mounted Guns/textures/block/brass_block.png, and
Guns/block/treated_brass_block.png, treated_brass_plates.png, cut_treated_brass.png, chiseled_treated_brass_block.png
and ancient_brass_block.png) is paler or more orange, with diagonal sheens or speckle; and the dark stones (biomes and
tree blocks/black_sandstone.png, cut_black_sandstone.png and blackstone_spines.png, whose colours are the Reliquary
Shrine's blackstone, sky_stone_block.png, smooth_sky_stone_block.png and sky_stone_brick.png, and
block_refined_obsidian.png) are near-black with steps too small to show the plinth's bevel beside the dark oak (the
blackstone was tried on the model and sank into the post's shadow), or noisy (the refined obsidian has 229 colours on
256 pixels). The reference sheet "blue stone.jpg" is coursed slate, not a slab. No library file is read, copied or
recoloured here, and the originals are untouched. Nothing is traced, sampled or recoloured from Mojang's files:
vanilla's planks are followed for their manner only.
"""

from PIL import Image

import concordance_conclave_models as models
import item_icons
from concordance_art import BRASS, OAK_DARK, VIOLET
from concordance_ritual_art import STONE
from icon_materials import SMOKED_GLASS

# The block's symbols. Digits are the dark oak, darkest first (0 a corner of the foot and of the back edge, and the
# butt joint, 1 the shaded columns, the grain and the foot, 2 the wood's own tone, 3 the lit columns, the top row and
# the joint's lit row); o n m Y Z brass, darkest first (Z the glint); v r p q the chart (v the vellum, r its faint
# violet lines, p q the pale stars, dim and bright) and G W its gold-ink stars (W the brightest); b c d e the stone,
# darkest first (b shade and the darker clump, c the stone's tone, d lit edges and the lighter clump, e the glint).
TONES = {
    "0": OAK_DARK[0], "1": OAK_DARK[1], "2": OAK_DARK[2], "3": OAK_DARK[3],
    "o": BRASS[0], "n": BRASS[1], "m": BRASS[2], "Y": BRASS[3], "Z": BRASS[4],
    "v": SMOKED_GLASS.dark, "r": VIOLET[3], "p": SMOKED_GLASS.light, "q": SMOKED_GLASS.highlight,
    "G": BRASS[4], "W": BRASS[5],
    "b": STONE[0], "c": STONE[1], "d": STONE[2], "e": STONE[3],
}

# Two upright boards, columns 0-3 (the post's sides) and 4-7, each lit down its left column and shaded down its right.
# Rows 0 and 1 are the desk's edges, a lit row over the boards' ends, kept free of grain; the first board's grain dash
# is column 1, rows 3-5; the second board's butt joint is rows 4 (dark) and 5 (lit); row 7 is the shaded foot.
WOOD = [
    "33333333",
    "32213221",
    "32213221",
    "31213221",
    "31213001",
    "31213331",
    "32213221",
    "11101110",
]

# The brass plate: the top row (the lip, the collar's sides) and the left column lit, the glint on their corner, the
# right column and the bottom row shaded.
BRASS_PLATE = [
    "ZYYYYYYY",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "Ymmmmmmn",
    "mnnnnnno",
]

# The star chart, row 0 along the lip: the W's stars at (1,2), (2,5), (4,3) (the brightest), (5,5) and (7,3), each
# joined to the next by a line of faint violet (r); pale stars at (7,1), (6,7) and, brighter, (0,7).
CHART = [
    "vvvvvvvv",
    "vvvvvvvp",
    "vGvvvvvv",
    "vrvvWvvG",
    "vrvrvrrv",
    "vvGvvGvv",
    "vvvvvvvv",
    "qvvvvvpv",
]

# The plinth: a chiselled slab, the top row (its sides' lit edge) and the left column lit, the glint on their corner,
# the right column and the bottom row shaded; the lighter clump at column 6, rows 3-4, and the darker one at row 6,
# columns 1-2, where the slab's top shows round the post (which covers about columns and rows 3 to 5).
PLINTH = [
    "eddddddd",
    "dccccccb",
    "dccccccb",
    "dcccccdb",
    "dcccccdb",
    "dccccccb",
    "dbbccccb",
    "bbbbbbbb",
]

REGION_ROWS = {"wood": WOOD, "brass": BRASS_PLATE, "chart": CHART, "plinth": PLINTH}


def block():
    """The Conclave Lectern's 16x16 block texture: each region of models.REGIONS painted from REGION_ROWS."""
    if set(REGION_ROWS) != set(models.REGIONS):
        raise ValueError(f"the lectern's regions are {sorted(models.REGIONS)}, painted {sorted(REGION_ROWS)}")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for name, rows in REGION_ROWS.items():
        u0, v0, u1, v1 = models.REGIONS[name]
        if len(rows) != v1 - v0 or any(len(row) != u1 - u0 for row in rows):
            raise ValueError(f"region {name} is {u1 - u0}x{v1 - v0}, painted {[len(row) for row in rows]}")
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                img.putpixel((u0 + x, v0 + y), tuple(TONES[ch]) + (255,))
    if any(img.getpixel((x, y))[3] != 255 for x in range(16) for y in range(16)):
        raise ValueError("the lectern's regions leave part of its texture unpainted; a block texture is opaque")
    return img


ICONS = ["conclave_lectern"]


def textures():
    """Every texture as {(kind, name): image}: the block texture and the item icon."""
    out = {("block", "conclave_lectern"): block()}
    out.update({("item", name): item_icons.draw(name) for name in ICONS})
    return out
