"""Textures for roadmap step 22, Sympathy and Dreamwalking (tools/concordance_hexes.py): the item icons of the Taglock,
Scrying Glass, Ward Sigil, Dreamglass and Oneiric Censer, the censer's GeckoLib sheet and the dream wisp's, for
tools/concordance_art.py's textures() to include.

Hexes and dreams are Practitioner research (the Hexweavers and the Dreamwalkers), so they keep the Arcane Concordance's
palette (dark wood, brass and gold, violet and amethyst), not the higher tiers' dieselpunk or heavy steampunk
(docs/ART_DIRECTION.md):

- the item icons are 16x16 maps in tools/item_icons/ (docs/ITEM_ICONS.md), drawn by item_icons.draw:
  - taglock.txt: the Taglock, a small corked glass vial holding a red thread, its neck bound with a turn of red string
    that runs out to a paper tag with a violet mark; upright, the glass shown by its outline and a lit stripe;
  - scrying_glass.txt: the Scrying Glass, a hand mirror of dark violet-blue glass in a round gold rim, an amethyst in
    the rim's crown, a violet glint at the glass's heart, on a short wooden handle with a gold collar and end cap. It
    stands upright, as its recipe lays it out, so its handle, dark glass and crown gem tell it from the Assay Glass's
    pale lens on the diagonal, the Astrolabe's gold disc and the Resonant Ring's open band;
  - ward_sigil.txt: the Ward Sigil, a narrow paper strip folded down at its top like an envelope's flap, a violet-ink
    warding ring round a shard of dreamglass, two inked strokes under it and a swallowtail foot; one icon for every
    ward. The narrow strip, the fold and the pale blue shard tell it from the Research Notes;
  - dreamglass.txt: Dreamglass, a small pointed shard of pale glass, its lit face violet and its shaded face blue, the
    ridge between them catching the light so that it seems to glow;
  - oneiric_censer.txt: the Oneiric Censer as an item, a round brass censer (its lid pierced and glowing violet, its
    bowl pierced under a lit rim) hanging by a rod from the arm of a brass post on a dark-oak foot, a violet smoke wisp
    rising from it. The smoke is never outlined (its map declares it, as a flame is never outlined);
- block/oneiric_censer: the 64x64 sheet of the censer's GeckoLib model (tools/concordance_hex_models.py), painted box
  by box from concordance_hex_models.SIZES (each box's UV origin and size; box UV: the top and bottom in a row d high,
  then the four sides h high). Each box takes a flat coat from concordance_ecology_art._paint_box (the top lit, the
  sides mid, the bottom dark; no specks, "Texturing: keep it clean") and then placed detail in flat tones lit from the
  top left: the dark-oak foot's top is two boards, each lit along its near edge and at its left end, shaded at its right
  end, with a seam at its far edge, and its sides are lit along their top edge; the brass post is lit down one edge,
  with a lit cap and a collar ring above a shadow where it meets the foot; the arm and rod are lit on top; the
  bowl's sides are a lit rim over a light band and a mid one, pierced by small holes, each dark at its top with the
  violet glow inside showing under it; the bowl's top is a lit rim round a dark violet well with a glowing heart (under
  the lid), its bottom a framed dark disc; the lid's top is a lit frame round a pierced vent whose holes are dark and
  glowing violet by turns, its sides a lit band with a violet slot of light; the plume is solid pale violet smoke with
  lighter wisps curling up it to pale crests (WISPS, a tile six texels wide, so it meets itself round the column),
  paler on top. Everything outside the boxes' face regions stays transparent;
- entity/dream_wisp: the 32x32 sheet of the dream wisp's model: the core, a bright pale violet-white cube (each face
  framed in a soft violet-white round a near-white glint, the top lit and the bottom a step darker), and the two
  crossed petals, zero-thickness planes whose two faces each carry the same soft violet-blue flame (FLAME), its pale
  heart, which shows round the core, fading to a violet-blue edge and a deeper tip, with alpha 0 round it. Each flame
  is symmetric left to right, so a plane's two faces, drawn on one plane, show the same colour at every point from
  either side; every texel is fully opaque or fully clear (no half-transparent texels, as cutout drawing needs).

Provenance (to be recorded with the feature's art, docs/features/arcane-concordance-hexes.md): everything here is drawn
fresh, nothing is taken from the owner's library (art/owner-library/README.md) or from Mojang's files, and no library
file is read when the textures are made. The colours are Jugcraft's own ramps, imported rather than copied:
tools/concordance_art.py's BRASS, GEM, VIOLET and OAK_DARK (the Concordance's brass, amethyst, deep violet and dark oak)
for the sheets, and tools/icon_materials.py's amethyst, dew, glass, gold, brass, dark_wood, wood, paper, smoked_glass
and cloth_red for the icons (their maps' `# materials:` lines); the plume's pale violets (SMOKE), the wisp core's
violet-whites (CORE) and the petals' violet-blues (PETAL) are the colours added here, between GEM's violet and the
dreamglass icon's pale blue. The library was searched for vials, bottles, mirrors, lenses, charms, talismans, paper,
glass shards, censers, incense, lanterns, smoke and wisps to reuse, and nothing fits, all under
art/owner-library/originals/Blocks/: Guns/item/lightning_in_a_bottle.png, farming and food textures/milk_bottle.png, Big
Cannons and Mounted Guns/textures/item/incendiarybottle.png and the fluid_containers_bottle_*.png are round corked
flasks, not a slim vial, Guns/item/wraith_bottle.png a capped canister and Weapon Smithing/Items/bottle/ only a bottle's
outline and fluid layers; Big Cannons and Mounted Guns/textures/item/implosionlense.png is a war machine's lens and
Airships and Planes/item/telescope.png a brass telescope, and the reference sheet "technical glasses and lenses
textures.jpg" shows goggles and gems, so the library has no hand mirror; Trinket Type Mod/entity/amulet.png is a worn
amulet's sheet, Trinket Type Mod/slot/empty_charm_slot.png a slot's outline and chunk_loader_paper.png a punched paper
tape, not a talisman; the shards (Big Cannons and Mounted Guns/textures/item/trinititeshard.png, nitrateshard.png and
sulfurshard.png, Guns/item/shard_anthralite.png, charged_amethyst_shard.png, nether_star_fragment.png and
shard_culler_amethyst.png) and the reference sheets "crystal blocks, shards, and dust textures.jpg" and "Selenite &
charoite textures.jpg" are other crystals in other hues, studied only for the owner's manner (a lit facet beside a
darker one, a dark outline in the crystal's own hue); lantern.png, Weapon Smithing/foundry/lantern.png and
Guns/item/plasma_lantern.png are lamps of other tiers; the smoke particles under Big Cannons and Mounted
Guns/textures/particle/ are grey gun and camp smoke; biomes and tree blocks/wispjelly.png is a block's texture and
biomes and tree blocks/hair.png an animated strip of swaying strands; and no censer, thurible or incense is in the
library. Vanilla's glass bottle and amethyst shard guide the manner only: no Mojang file was opened, and nothing is
traced, sampled or recoloured from one.
"""

from PIL import Image

import concordance_ecology_art as ecology_art
import concordance_hex_models as hex_models
import item_icons
from concordance_art import BRASS, GEM, OAK_DARK, VIOLET

# ------------------------------------------------------------------------------------------------------- palettes

# The censer's metal is the Concordance's brass (its dark, mid, light and lit tones) and its foot the dark oak, both
# from tools/concordance_art.py; the light inside its pierced holes is the Concordance's gem violet over its deep
# violet.
DARK, MID, LIGHT, LIT = BRASS[1:5]
GLOW_DIM, GLOW = GEM[1], GEM[2]
WELL = VIOLET[2]
# The plume's pale violet smoke, darkest first (added here): its underside, its shade, its body, its wisps and their
# palest crests.
SMOKE = [(118, 100, 178), (146, 128, 204), (172, 158, 224), (200, 190, 240), (226, 220, 250)]
# The plume's sides, a tile six texels wide repeated round the four sides laid side by side (twelve texels, so it meets
# itself round the column): each digit is a SMOKE tone. A lighter wisp curls up through the body to a pale crest, and
# the top row is paler, where the smoke thins.
WISPS = [
    "334333",
    "233322",
    "222332",
    "222232",
    "222332",
    "223322",
]
# The dream wisp (added here): the core's violet-whites, darkest first (the last a one-texel glint only), and the
# petals' violet-blues from the flame's tips to its heart.
CORE = [(176, 162, 236), (200, 190, 246), (222, 214, 252), (232, 226, 254), (250, 248, 255)]
PETAL = [(134, 132, 238), (172, 174, 250), (212, 216, 255)]
# A petal's flame, top row first (the top of the plane): o its tip, m its edge, i its heart, . clear. The core hides
# the flame's middle, so what shows round it is the pale heart fading to a violet-blue edge and a deeper tip. Symmetric
# left to right, so a plane's two faces agree from either side.
FLAME = [
    "..oo..",
    "..mm..",
    ".miim.",
    "miiiim",
    "miiiim",
    ".miim.",
]
FLAME_TONES = {"o": PETAL[0], "m": PETAL[1], "i": PETAL[2]}


# ------------------------------------------------------------------------------------------------- small helpers

def _put(img, x, y, colour):
    img.putpixel((x, y), tuple(colour[:3]) + (255,))


def _fill(img, rect, colour):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            _put(img, x, y, colour)


def faces(uv, size):
    """A box-UV cube's faces as (x, y, w, h) rectangles: (top, bottom, [the four sides]). A flat plane's edge faces
    (zero wide or high) are empty rectangles."""
    (u, v), (w, h, d) = uv, size
    return ((u + d, v, w, d), (u + d + w, v, w, d),
            [(u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h)])


def regions(key):
    """Every non-empty face rectangle of every box of a model in concordance_hex_models.SIZES, by box name: the only
    places its sheet is painted."""
    uvs, sizes = hex_models.SIZES[key]
    out = {}
    for name, uv in uvs.items():
        top, bottom, sides = faces(uv, sizes[name])
        out[name] = [rect for rect in [top, bottom] + sides if rect[2] and rect[3]]
    return out


# ------------------------------------------------------------------------------------------------ the censer sheet

def _foot(img, uv, size):
    """The dark-oak foot: its top two boards, each lit along its near edge and at its left end, shaded at its right
    end, with a seam at its far edge; its sides lit along their top edge; its underside the coat's dark."""
    top, _bottom, sides = faces(uv, size)
    x0, y0, w, h = top
    for dy in range(h):
        row = dy % 4
        tone = OAK_DARK[4] if row == 0 else OAK_DARK[2] if row == 3 else OAK_DARK[3]
        _fill(img, (x0, y0 + dy, w, 1), tone)
        if row in (1, 2):
            _put(img, x0, y0 + dy, OAK_DARK[4])
            _put(img, x0 + w - 1, y0 + dy, OAK_DARK[2])
    for x, y, w, h in sides:
        _fill(img, (x, y, w, 1), OAK_DARK[3])
        _fill(img, (x, y + 1, w, h - 1), OAK_DARK[2])


def _post(img, uv, size):
    """The brass post: each side lit down its left edge and mid on its right, a lit cap under the arm, and a collar
    (a lit ring over a light one) above a shadow where it meets the foot."""
    top, _bottom, sides = faces(uv, size)
    _fill(img, top, LIT)
    for x, y, w, h in sides:
        _fill(img, (x, y, 1, h), LIGHT)
        _fill(img, (x + 1, y, w - 1, h), MID)
        _fill(img, (x, y, w, 1), LIT)
        _fill(img, (x, y + h - 3, w, 1), LIT)
        _fill(img, (x, y + h - 2, w, 1), LIGHT)
        _fill(img, (x, y + h - 1, w, 1), DARK)


def _bar(img, uv, size):
    """The arm and the rod: the top light, lit along its near row; the long sides light and the ends mid, each a step
    darker along its foot where it is taller than a texel (the rod); the underside dark."""
    top, bottom, sides = faces(uv, size)
    x, y, w, h = top
    _fill(img, top, LIGHT)
    _fill(img, (x, y, w, 1), LIT)
    _fill(img, bottom, DARK)
    for i, (sx, sy, sw, sh) in enumerate(sides):
        _fill(img, (sx, sy, sw, sh), LIGHT if i % 2 else MID)
        if sh > 1:
            _fill(img, (sx, sy + sh - 1, sw, 1), MID if i % 2 else DARK)


def _bowl(img, uv, size):
    """The censer's bowl: each side a lit rim over a light band and a mid one, pierced by small holes (every other
    texel), each dark at its top with the violet glow inside showing under it; its top a lit rim (shaded on the right
    and bottom) round a dark violet well with a glowing heart, under the lid; its bottom a framed dark disc."""
    top, bottom, sides = faces(uv, size)
    for x, y, w, h in sides:
        _fill(img, (x, y, w, 1), LIT)
        _fill(img, (x, y + 1, w, 1), LIGHT)
        _fill(img, (x, y + 2, w, h - 2), MID)
        for dx in range(1, w - 1, 2):
            _put(img, x + dx, y + 1, WELL)
            _put(img, x + dx, y + 2, GLOW)
    x, y, w, h = top
    _fill(img, top, LIGHT)
    _fill(img, (x, y, w, 1), LIT)
    _fill(img, (x, y, 1, h), LIT)
    _fill(img, (x + w - 1, y + 1, 1, h - 1), MID)
    _fill(img, (x + 1, y + h - 1, w - 1, 1), MID)
    _fill(img, (x + 1, y + 1, w - 2, h - 2), WELL)
    _put(img, x + w // 2, y + h // 2, GLOW)
    x, y, w, h = bottom
    _fill(img, bottom, MID)
    _fill(img, (x + 1, y + 1, w - 2, h - 2), DARK)


def _lid(img, uv, size):
    """The lid: its top a lit frame (shaded on the right and bottom) round a pierced vent, where the rod meets it, its
    holes dark and glowing violet by turns; its sides a lit band with a violet slot of light in the middle; its
    underside the coat's dark."""
    top, _bottom, sides = faces(uv, size)
    x, y, w, h = top
    _fill(img, top, LIGHT)
    _fill(img, (x, y, w, 1), LIT)
    _fill(img, (x, y, 1, h), LIT)
    _fill(img, (x + w - 1, y + 1, 1, h - 1), MID)
    _fill(img, (x + 1, y + h - 1, w - 1, 1), MID)
    for dy in range(1, h - 1):
        for dx in range(1, w - 1):
            _put(img, x + dx, y + dy, GLOW if (dx + dy) % 2 == 0 else WELL)
    for sx, sy, sw, sh in sides:
        _fill(img, (sx, sy, sw, sh), LIT)
        _fill(img, (sx + 1, sy, sw - 2, sh), GLOW_DIM)


def _plume(img, uv, size):
    """The smoke plume: solid pale violet with lighter wisps curling up round it (WISPS, repeated across the four
    sides laid side by side), paler at its top; its top face pale with a palest heart, its underside a step darker."""
    top, bottom, sides = faces(uv, size)
    x0, y0 = sides[0][0], sides[0][1]
    width = sum(w for _x, _y, w, _h in sides)
    for sy in range(sides[0][3]):
        for sx in range(width):
            _put(img, x0 + sx, y0 + sy, SMOKE[int(WISPS[sy][sx % len(WISPS[sy])])])
    x, y, w, h = top
    _fill(img, top, SMOKE[3])
    _put(img, x + w // 2, y + h // 2, SMOKE[4])
    _fill(img, bottom, SMOKE[1])


def censer_sheet():
    uvs, sizes = hex_models.SIZES["oneiric_censer"]
    coats = {"foot": OAK_DARK, "post": BRASS, "arm": BRASS, "rod": BRASS, "bowl": BRASS, "lid": BRASS, "plume": SMOKE}
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for name, uv in uvs.items():
        # The flat coat (ecology_art._paint_box): the top lit, the sides mid with a lit top row, the bottom dark. No
        # specks ("Texturing: keep it clean"), so its seed is never used.
        ecology_art._paint_box(img, uv, sizes[name], coats[name], 0, speckle=0)
    _foot(img, uvs["foot"], sizes["foot"])
    _post(img, uvs["post"], sizes["post"])
    for name in ("arm", "rod"):
        _bar(img, uvs[name], sizes[name])
    _bowl(img, uvs["bowl"], sizes["bowl"])
    _lid(img, uvs["lid"], sizes["lid"])
    _plume(img, uvs["plume"], sizes["plume"])
    return img


# -------------------------------------------------------------------------------------------- the dream wisp sheet

def _core(img, uv, size):
    """The core: every face framed in a soft violet-white round a near-white glint at its centre; the top a step
    brighter, the bottom a step darker, and the top row of each side catching the light."""
    top, bottom, sides = faces(uv, size)
    looks = [(top, CORE[3], CORE[4], False), (bottom, CORE[1], CORE[3], False)]
    looks += [(side, CORE[2], CORE[4], True) for side in sides]
    for rect, frame, heart, lit_top in looks:
        x, y, w, h = rect
        _fill(img, rect, frame)
        if lit_top:
            _fill(img, (x, y, w, 1), CORE[3])
        _put(img, x + w // 2, y + h // 2, heart)


def _petal(img, rect):
    """One face of a petal: the flame, with alpha 0 round it."""
    x0, y0, w, h = rect
    for dy, row in enumerate(FLAME):
        for dx, ch in enumerate(row):
            if ch != "." and dx < w and dy < h:
                _put(img, x0 + dx, y0 + dy, FLAME_TONES[ch])


def wisp_sheet():
    uvs, sizes = hex_models.SIZES["dream_wisp"]
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # The core's flat coat (ecology_art._paint_box, no specks), then its glow.
    ecology_art._paint_box(img, uvs["core"], sizes["core"], CORE, 0, speckle=0)
    _core(img, uvs["core"], sizes["core"])
    for name in ("petal_x", "petal_z"):
        for rect in regions("dream_wisp")[name]:
            _petal(img, rect)
    return img


# ----------------------------------------------------------------------------------------------------- all of them

ICONS = ["taglock", "scrying_glass", "ward_sigil", "dreamglass", "oneiric_censer"]


def textures():
    """Every texture as {(kind, name): image}: the five item icons, the censer's sheet and the dream wisp's."""
    out = {("item", name): item_icons.draw(name) for name in ICONS}
    out[("block", "oneiric_censer")] = censer_sheet()
    out[("entity", "dream_wisp")] = wisp_sheet()
    return out
