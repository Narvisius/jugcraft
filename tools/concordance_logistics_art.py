"""Textures for roadmap step 18, logistics with reservations and accountable transit (the Courier Post,
docs/features/arcane-concordance-logistics.md): the post's three block faces and its item icon. The block model is
minecraft:block/cube_bottom_top, so all four sides share one face.

The Courier Post is a Practitioner's piece of Jugcraft Workshops joinery, not a machine: a sturdy sorting cabinet in
the Concordance's palette, the Lampwright's Bench's dark oak and brass with a touch of violet, and none of the higher
tiers' dieselpunk (docs/ART_DIRECTION.md). Its faces are 16x16 and opaque, in flat tones lit from the top left, with
no noise or wear ("Texturing: keep it clean"):

- courier_post_side (all four sides): a frame round nine pigeonholes, one for each of the post's nine slots, under a
  top rail and over a plinth. Each pigeonhole is a recess, shadowed under its shelf and along its left wall; six hold
  mail (envelopes, a rolled scroll, bundles of letters), the middle one an envelope sealed in violet. Brass caps sit
  on the frame's four corners, so where two sides meet at an edge of the block, and the top meets them, the caps close
  round the corner. The left stile is lit and the right one shaded, the top rail lit and the plinth's foot shaded, so
  posts set side by side or stacked show one lit edge and one shade at each joint, never a doubled dark seam;
- courier_post_top: four dark oak boards with brass caps on the corners, and on them the post's ledger lying open,
  bound in violet, entries written on its pages and a violet wax seal on the right one;
- courier_post_bottom: the same four boards, plain;
- the item icon: a 16x16 map, tools/item_icons/courier_post.txt (docs/ITEM_ICONS.md), drawn by item_icons.draw: the
  cabinet from the front, its corners capped in brass, its pigeonholes holding letters, the middle one sealed in violet.

The boards follow docs/NATURAL_TEXTURES.md's planks (four boards of four rows: a lit top row, the wood's tone with a few
darker pixels, a dark seam; three grain dashes and a butt joint a board, the joints at least four pixels apart), in
the bench's dark oak rather than a new wood's colour, since the post is joinery, not a new tree.

Provenance: everything here is drawn fresh, by code, with no randomness, so every run writes the same pixels. The
colours are the Arcane Concordance's own ramps, imported from tools/concordance_art.py (not copied, so the post stays
in step with the bench): OAK_DARK, the Lampwright's Bench's dark oak; BRASS, its brass; GEM, the Concordance's violet;
PAGE, its paper. The owner's library (art/owner-library/catalog/files.csv) was searched for a cabinet, crate, shelf,
wood, brass, mail or ledger asset to reuse or adapt, and none is used. Its cabinets, crates and shelves (under
art/owner-library/originals/Blocks/: farming and food textures/*_cabinet_*.png and *crate*.png,
Guns/block/supply_crate.png, biomes and tree blocks/*_shelf.png) are other woods' colours and layouts, and its brass
(Guns/block/treated_brass_*.png, Big Cannons and Mounted Guns/textures/block/brass_block.png) is a paler, orange
metal than the Concordance's. No library file is read, copied or recoloured here. Nothing is traced, sampled or
recoloured from Mojang's files: vanilla's planks and bookshelves are followed for their manner only.
"""

from PIL import Image

import item_icons
from concordance_art import BRASS, GEM, OAK_DARK, PAGE

# The faces' symbols. Digits are the dark oak, darkest first (0 the deepest shadow and the seams, 1 shade, 2 the wood's
# own tone, 3 lit, 4 a lit top edge); n m Y Z are brass, darkest first; p P Q the paper, darkest first (p is also the
# ledger's ink); v V the violet, dark and light.
TONES = {
    "0": OAK_DARK[0], "1": OAK_DARK[1], "2": OAK_DARK[2], "3": OAK_DARK[3], "4": OAK_DARK[4],
    "n": BRASS[1], "m": BRASS[2], "Y": BRASS[3], "Z": BRASS[4],
    "p": PAGE[0], "P": PAGE[1], "Q": PAGE[2],
    "v": GEM[0], "V": GEM[1],
}


def _paint(img, rows):
    """Paint 16 rows of 16 symbols (TONES) onto img; '.' leaves a pixel as it is."""
    if len(rows) != 16 or any(len(row) != 16 for row in rows):
        raise ValueError(f"a face is 16 rows of 16 symbols, not {[len(row) for row in rows]}")
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), tuple(TONES[ch]) + (255,))
    return img


# ------------------------------------------------------------------------------------------------------- the side

# Columns: 0 the lit stile, 1-4, 6-9 and 11-14 the pigeonholes with dividers at 5 and 10, 15 the shaded stile. Rows: 0-1
# the top rail, 2-4, 6-8 and 10-12 the pigeonholes with shelves at 5 and 9, 13-15 the plinth. A pigeonhole is shadowed
# along its top and its left wall (0) round a dark back wall (1). Its mail, by row: a standing envelope, nothing, a
# rolled scroll; a bundle of letters, the envelope sealed in violet, nothing; nothing, an envelope, a bundle. A brass
# cap is a triangle of six pixels on each corner, brightest on the top left.
SIDE = [
    "ZYm4444444444YYY",
    "Ym333333333333mm",
    "m00002000020000n",
    "30QP12011120QQP1",
    "30Pp12011120PPp1",
    "3333333333333331",
    "3000020000200001",
    "301QP20QPP201111",
    "30QPp20PVp201111",
    "3333333333333331",
    "3000020000200001",
    "3011120QP1201QP1",
    "3011120Pp120QPp1",
    "Y44444444444444m",
    "Ym222222222222mn",
    "mnn1111111111nnn",
]


def side():
    return _paint(Image.new("RGBA", (16, 16), (0, 0, 0, 255)), SIDE)


# --------------------------------------------------------------------------------------------- planks: top, bottom

# Four boards of four rows (docs/NATURAL_TEXTURES.md, rule 4), placed by hand: for each board its three grain dashes
# (x, row in the board, length; they wrap at the edge), the x of its butt joint (a dark pixel with a lit one to its
# right), and the darker pixels on its lower row.
BOARDS = [
    {"dashes": [(1, 1, 3), (9, 2, 2), (12, 1, 3)], "joint": 6, "dark": [4, 13]},
    {"dashes": [(3, 2, 4), (8, 1, 2), (14, 2, 3)], "joint": 11, "dark": [1, 8]},
    {"dashes": [(0, 1, 2), (6, 2, 3), (12, 2, 2)], "joint": 3, "dark": [10, 15]},
    {"dashes": [(4, 1, 3), (9, 2, 3), (0, 1, 2)], "joint": 13, "dark": [6, 2]},
]


def _planks():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))

    def put(x, y, tone):
        img.putpixel((x % 16, y), tuple(OAK_DARK[tone]) + (255,))

    for board, spec in enumerate(BOARDS):
        y0 = board * 4
        for x in range(16):
            put(x, y0, 3)
            put(x, y0 + 1, 2)
            put(x, y0 + 2, 2)
            put(x, y0 + 3, 0)
        for x in spec["dark"]:
            put(x, y0 + 2, 1)
        for x0, row, length in spec["dashes"]:
            for i in range(length):
                put(x0 + i, y0 + row, 1)
        j = spec["joint"]
        for y in range(y0, y0 + 3):
            put(j, y, 0)
            put(j + 1, y, 4 if y == y0 else 3)
    return img


def bottom():
    return _planks()


# The top: the ledger lying open on the boards, bound in violet (lit along its top and left edges), its two pages (Q)
# meeting at a shaded gutter (P), entries written in ink (p) and a violet wax seal on the right page; and the brass
# caps on the four corners, which meet the sides' caps at the block's top corners. '.' leaves the boards.
TOP = [
    "ZYm..........mYY",
    "Ym............mm",
    "m..............n",
    "................",
    "..VVVVVVVVVVVv..",
    "..VQQQQPPQQQQv..",
    "..VQpppPPpppQv..",
    "..VQQQQPPQQQQv..",
    "..VQppQPPQVvQv..",
    "..VQQQQPPQvvQv..",
    "..VQpppPPQQQQv..",
    "..vvvvvvvvvvvv..",
    "................",
    "m..............n",
    "Ym............mn",
    "YYm..........nnn",
]


def top():
    return _paint(_planks(), TOP)


# ----------------------------------------------------------------------------------------------------- all of them

ICONS = ["courier_post"]


def textures():
    """Every texture as {(kind, name): image}: the three block faces and the item icon."""
    out = {
        ("block", "courier_post_side"): side(),
        ("block", "courier_post_top"): top(),
        ("block", "courier_post_bottom"): bottom(),
    }
    out.update({("item", name): item_icons.draw(name) for name in ICONS})
    return out
