"""Textures for roadmap step 19, Runesmithing (tools/concordance_artifice.py,
docs/features/arcane-concordance-artifice.md): the Artificer's Bench's three block faces and its item icon, and the
Resonant Ring's item icon. The bench's block model is minecraft:block/cube_bottom_top, so all four sides share one face.

The Artificer's Bench is the runesmith's workbench where Resonant Rings are forged and then reforged, inscribed,
socketed, bonded, repaired and salvaged. It is a Practitioner's piece of the Jugcraft Workshops, built as its recipe
reads (iron, an amethyst shard, smooth stone, a smithing table): a sturdy stone bench with an iron working plate, in the
Concordance's brass and violet, and none of the higher tiers' dieselpunk (docs/ART_DIRECTION.md). Its faces are 16x16
and opaque, in flat tones lit from the top left, with stone clumps of two or three pixels and no noise or wear
("Texturing: keep it clean"):

- artificer_bench_side (all four sides): a thick stone top slab over two stone legs, a seam of shadow under the slab, an
  apron inlaid with a small amethyst lozenge (its upper-left facet lit, its lower-right one dark), and between the legs
  a kneehole in shadow where the smith keeps stock: a gold and a copper ingot on its floor and an iron one across them
  (three of the ring's four substrates), over a plinth. Brass caps sit on the face's four corners, so where two sides
  meet at an edge of the block, and the top or bottom meets them, the caps close round the corner. The left leg is lit
  and the right one shaded, the slab's top edge lit and the plinth's foot shaded, so benches set side by side show one
  lit edge and one shade at each joint;
- artificer_bench_top: the working surface, a steel plate set in a two-pixel stone rim, raised a little (lit along its
  top and left, shaded along its bottom and right, with a dark seam round it), inlaid with a violet rune circle lit on
  its upper left round an amethyst at its centre; brass caps on the four corners meet the sides' caps;
- artificer_bench_bottom: the slab's plain stone underside, lit along its top and left edges, with the brass caps;
- the item icons, 16x16 maps drawn by item_icons.draw (docs/ITEM_ICONS.md): tools/item_icons/artificer_bench.txt, the
  bench from the front and a little from above (the dark plate with its violet rune and amethyst, the brass-capped slab
  and plinth, the stone legs round a dark kneehole holding an ingot), in tones near the block's; and
  tools/item_icons/resonant_ring.txt, a brass band with a round amethyst gripped by two prongs, lit like a round wire,
  centred with clear pixels to its left, right and above. One ring icon serves every substrate, so its band is a
  neutral brass rather than copper, iron, gold or netherite.

Provenance: everything here is drawn fresh, by code and in text maps, with no randomness, so every run writes the same
pixels. The stone's colours are adapted from the owner's library (art/owner-library/README.md): STONE's five lighter
tones are the swatch row beside the owner's grey stone sheet (art/owner-library/previews/grey-stone-variants.png), and
its deepest tone, used only for the kneehole's shadow, is the fourth swatch beside their dark stone sheet
(art/owner-library/previews/dark-teal-stone.png). They were sampled once, as the median of the middle of each swatch
(a swatch's pixels vary by a few levels), and are written below as numbers; only those colours are used, not the
sheets' pixels or layouts, no library file is read when the textures are made, and the originals are untouched. The
other colours are Jugcraft's own ramps, imported rather than copied so the bench stays in step with them: BRASS and GEM,
the Arcane Concordance's brass and violet (tools/concordance_art.py, as the Lampwright's Bench and the Courier Post use
them); arms_pixel.STEEL, the owner's chosen blue-grey steel, for the plate; and icon_materials.GOLD, IRON and COPPER,
the mod's stand-ins for those metals, for the ingots. The icon maps take their colours from tools/icon_materials.py
(their `# materials:` lines). The library was also searched for a bench, anvil, smithing table, ring, jewel, gem or
brass asset to reuse or adapt, and none is used, all under art/owner-library/originals/Blocks/: Trinket Type
Mod/slot/empty_ring_slot.png is a faint ring-slot outline for a menu, not a ring; Weapon Smithing/foundry/casting/
table_*.png is a foundry casting table of dark brick rimmed in gold, and Weapon Smithing/Items/cast/gem.png,
Items/materials/hollow_gem.png and silky_jewel.png are a gem cast and plain oval stones, not a set ring; Guns/block/
gun_bench.png and exo_suit_bench.png are 64x64 sheets for the guns' machinery, in a heavier industrial manner than a
Practitioner's bench; and the brass blocks (Guns/block/treated_brass_*.png, Big Cannons and Mounted
Guns/textures/block/brass_block.png) are a paler, more orange metal than the Concordance's. Nothing is traced, sampled
or recoloured from Mojang's files: vanilla's smithing table and stone are followed for their manner only.
"""

from PIL import Image

import icon_materials
import item_icons
from arms_pixel import STEEL
from concordance_art import BRASS, GEM

# The owner's grey stone, darkest first (provenance above): 0 the dark sheet's swatch (the kneehole's deepest shadow),
# then the grey sheet's five swatches: 1 seams and shade, 2 darker clumps, 3 the stone's own tone, 4 lit edges and
# lighter clumps, 5 the highlight (unused on these faces; kept so the ramp is the owner's whole row).
STONE = [(51, 50, 46), (77, 74, 57), (96, 93, 77), (111, 113, 96), (126, 128, 113), (141, 146, 136)]

# The faces' symbols. Digits are STONE, darkest first; a b c d e the steel plate (a its seam, b its shaded bevel, c the
# plate, d its lit bevel, e a glint on the lit corner); n m Y Z brass, darkest first; v V W X the violet, darkest first;
# G H gold, I J iron and C K copper (each ingot's front, then its lit top).
TONES = {
    "0": STONE[0], "1": STONE[1], "2": STONE[2], "3": STONE[3], "4": STONE[4], "5": STONE[5],
    "a": STEEL.outline_dark, "b": STEEL.outline_light, "c": STEEL.dark, "d": STEEL.mid, "e": STEEL.light,
    "n": BRASS[1], "m": BRASS[2], "Y": BRASS[3], "Z": BRASS[4],
    "v": GEM[0], "V": GEM[1], "W": GEM[2], "X": GEM[3],
    "G": icon_materials.GOLD.dark, "H": icon_materials.GOLD.mid,
    "I": icon_materials.IRON.dark, "J": icon_materials.IRON.mid,
    "C": icon_materials.COPPER.dark, "K": icon_materials.COPPER.mid,
}


def _paint(rows):
    """A 16x16 opaque face from 16 rows of 16 symbols (TONES)."""
    if len(rows) != 16 or any(len(row) != 16 for row in rows):
        raise ValueError(f"a face is 16 rows of 16 symbols, not {[len(row) for row in rows]}")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), tuple(TONES[ch]) + (255,))
    return img


# ------------------------------------------------------------------------------------------------------- the side

# Rows: 0-2 the top slab (its top edge lit) and 3 the shadow under it; 4-7 the apron, the amethyst lozenge at its
# middle; 8-12 the kneehole, shadowed along its top and its left wall, its back wall dark, with the gold (left) and
# copper (right) ingots on its floor and the iron one across them; 13-15 the plinth. Columns: 0-2 the left leg (lit at
# the block's edge), 13-15 the right leg (shaded at it). A brass cap is a triangle of six pixels on each corner,
# brightest on the top left, as on the Courier Post.
SIDE = [
    "ZYm4444444444YYY",
    "Ym333443333223mm",
    "m22333333443333n",
    "1111111111111111",
    "4333333333333332",
    "4332233WW3333442",
    "433333WWVv322332",
    "4344333vv3333332",
    "4330000000000332",
    "4330111JJJJ11232",
    "4430111IIII11332",
    "4430HHHH1KKKK332",
    "4330GGGG1CCCC222",
    "Y44444444444444m",
    "Ym333322333344mn",
    "mnn1111111111nnn",
]


def side():
    return _paint(SIDE)


# -------------------------------------------------------------------------------------------------------- the top

# A stone rim two pixels wide (lit on the top and left, shaded on the bottom and right) round the steel plate, columns
# and rows 2-13: its seam (a), its lit bevel (d, with a glint e on the corner) and shaded bevel (b), and inside them the
# plate (c) inlaid with the rune circle, eight pixels across, lit on its upper left (W), shaded on its lower right (v),
# round the amethyst at the centre (X lit). The brass caps match the sides'.
TOP = [
    "ZYm4444444444mYY",
    "Ym333333333333mm",
    "m3aaaaaaaaaaaa2n",
    "43aeddddddddca21",
    "43adccWWWVccba21",
    "43adcWccccVcba21",
    "43adWccccccVba21",
    "43adWccXWccvba21",
    "43adWccWVccvba21",
    "43adVccccccvba21",
    "43adcVccccvcba21",
    "43adccVvvvccba21",
    "43acbbbbbbbbba21",
    "m3aaaaaaaaaaaa2n",
    "Ym222222222222mn",
    "YYm1111111111nnn",
]


def top():
    return _paint(TOP)


# ----------------------------------------------------------------------------------------------------- the bottom

# The slab's underside: the stone's own tone with a few clumps of two or three pixels, lit along the top and left edges
# and shaded along the bottom and right, with the brass caps.
BOTTOM = [
    "ZYm4444444444mYY",
    "Ym333333333333mm",
    "m33223333333333n",
    "4332333333443332",
    "4333333333333332",
    "4333443333333332",
    "4333333332233332",
    "4333333333333332",
    "4332233333333442",
    "4333333333333332",
    "4333333443333332",
    "4333333333322332",
    "4344333333333332",
    "m33333333333333n",
    "Ym333322333333mn",
    "YYm2222222222nnn",
]


def bottom():
    return _paint(BOTTOM)


# ----------------------------------------------------------------------------------------------------- all of them

ICONS = ["artificer_bench", "resonant_ring"]


def textures():
    """Every texture as {(kind, name): image}: the bench's three block faces and the two item icons."""
    out = {
        ("block", "artificer_bench_side"): side(),
        ("block", "artificer_bench_top"): top(),
        ("block", "artificer_bench_bottom"): bottom(),
    }
    out.update({("item", name): item_icons.draw(name) for name in ICONS})
    return out
