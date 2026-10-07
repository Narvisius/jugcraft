"""The 3D armor sets' 16x16 inventory icons: the bronze and steel armor's, the knight armor (tools/knight_armor.py)
drawn small, and Bloodthorn Armor's and Reforged White Diamond's (tools/bloodthorn_armor.py and
tools/white_diamond_armor.py, below), in the owner's style for item icons (vanilla's own size, a one-pixel outline in
each part's darkest tone, never pure black, light from the top left, a few flat tones, chunky parts that read at a
glance).

Each piece is one hand-drawn map, tools/armor_icons/<piece>.txt: 16 lines of 16 symbols, which the owner can edit
directly (lines starting with # are comments). A map names no colours, only what each pixel is made of, so one map
serves both metals: steel_<piece> is coloured from armor_paint.STEEL (the owner's sampled steel, gold and leather) and
bronze_<piece> from armor_paint.BRONZE, the same palettes the 3D armor is painted in.

What each icon carries of the knight (front view, as vanilla draws armor; boots in vanilla's stance):
    helmet      the big diamond crest overhanging the helm, with its nested chevrons and the finial, the horn fins at
                its corners, the dark seam under it, the eye slits beside the nasal bar, and the gold collar
    chestplate  the two layered pauldrons with nested L's toward the armpit, the light chevron plate over the chest
                with its bold dark V and the keel at its point, the hem lame, and the dark under-layer at the waist
    leggings    the brown leather belt (the under-layer's light top edge, two plates, the strap across, the
                under-layer showing at the centre) over the skirt of four plate lames, light over dark, widening to
                the hem, with the dark seam down the middle
    boots       greaves with a rolled top, a shadow under it and the knee cop's small chevron, and sabatons of banded
                instep lames, light over dark, with pointed toes
The bronze icons add what the bronze variant adds in brass: the finial and horn tips, rivets on the pauldron lames, the
hem lame and the skirt lames' ends, and the belt buckle.

Symbols:
    .           transparent
    O D M L H   the plate: outline, dark, mid, mid-light, light (the ramp's "dark", "mid", "mid_light", "light"; the
                outline is its "void" taken down to an item icon's outline depth, OUTLINE_LUMA)
    F Y g       the gold trim (gold on steel, brass on bronze): dark, light, outline
    R r         fittings (finial, horn tips, rivets): brass on bronze (light, dark); on steel the plate's own light
                and mid, since the steel has no brass there (knight_armor.py, the bronze variant): put R where the
                steel plate is lit and r where it is mid
    Q           the belt's centre: a brass buckle on bronze, the strap's lighter middle on steel (as the owner drew it)
    w k b B     leather: outline, strap, plate, top edge ("leather_darkest", "leather_dark", "leather_mid",
                "leather_light")
    u U         the dark under-layer: fill, top edge ("under_dark", "under_light")

A set may instead have maps of its own, tools/armor_icons/<set>/<piece>.txt, with its own symbols (OWN): Bloodthorn
Armor (tools/bloodthorn_armor.py) has bloodthorn/helmet.txt and so on, coloured from armor_paint.BLOODTHORN:
    helmet      the boxy great helm cut by three slits (two wide, a narrower one below) under its crown of spikes: the
                tall one at the centre, a long pair and a lower pair splayed wider
    chestplate  the pauldrons tilted up toward the outside, the collar with its dark inside showing, the breastplate
                running coral to crimson, and the magenta V over the dark waist
    leggings    the dark belt; the tassets, lit at the top left with a nested L toward the inner lower corner, their
                tops stepping down to the centre; the front plate with its dark strap; the diamond knee plates on the
                dark knees
    boots       two chunky boots in magenta and plum strips, notched dark at the top
Their symbols:
    .               transparent
    O               the outline: the metal's "void" taken down to OUTLINE_LUMA
    H L R M D S V   the metal, light to dark: "light" (orange), "mid_light" (coral), "gold_light" (the design's red,
                    see armor_paint.BLOODTHORN), "mid" (crimson), "dark" (magenta), "seam" (plum), "void" (dark plum)
    U m u x         the under-layer: "under_light", "under_mid", "under_dark", and "under_darkest", which is both the
                    eye slits and the under-layer's own outline
Reforged White Diamond (tools/white_diamond_armor.py) has reforged_white_diamond/helmet.txt and so on, coloured from
armor_paint.WHITE_DIAMOND and drawn after the owner's renders of each piece on its own:
    helmet      the white V crest, its point dipping into the charcoal T of the face, the light blue peak behind it, and
                the swept wings rising outside it
    chestplate  the wing pauldrons rising toward the outside over their lames, the light blue gem at the collar and the
                white V down the chest, strips at the waist (no sleeves: the owner's chestplate on its own has none)
    leggings    the belt, its top edge light blue, over the A of the two tassets, white along their outer edges and
                lined lavender, then blue, inside, with the striped under-skirt between and under them
    boots       two chunky boots under a light blue band, each with its diamond plate, a blue rim round a white heart
Their symbols:
    .               transparent
    O               the outline: the metal's "void" (lavender) taken down to OUTLINE_LUMA, a deep blue violet
    H L M D S V     the metal, light to dark: "light" (icy white), "mid_light" (pale cyan), "mid" (light cyan), "dark"
                    (light blue), "seam" (blue), "void" (lavender)
    U m u x         the charcoal under-layer: "under_light", "under_mid", "under_dark", "under_darkest" (the face)
"""
import os

from PIL import Image

import armor_paint

FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "armor_icons")
SIZE = 16
PIECES = ("helmet", "chestplate", "leggings", "boots")
METALS = {"steel": armor_paint.STEEL, "bronze": armor_paint.BRONZE}
# An item icon's outline sits at about this luma (the owner's outlines measure about 35 to 42; the item-icon rules in
# review on the arms-icons branch, docs/ITEM_ICONS.md, keep it at 45 or less). The knight's void (steel 68, bronze 56)
# is lighter, so the outline is the void carried down to it, keeping its hue.
OUTLINE_LUMA = 40

# symbol -> palette tone name, the same for both metals
TONES = {"D": "dark", "M": "mid", "L": "mid_light", "H": "light", "F": "gold_dark", "Y": "gold_light",
         "k": "leather_dark", "b": "leather_mid", "B": "leather_light", "w": "leather_darkest",
         "u": "under_dark", "U": "under_light"}
# symbol -> tone, per metal: the bronze's brass fittings are its own plate on the steel
FITTINGS = {"steel": {"R": "light", "r": "mid", "Q": "leather_mid"},
            "bronze": {"R": "gold_light", "r": "gold_dark", "Q": "gold_light"}}
# Sets with maps of their own (armor_icons/<set>/<piece>.txt): set -> (palette, {symbol: tone name}); "O" is always the
# metal's outline, its "void" taken down to OUTLINE_LUMA.
OWN = {"bloodthorn": (armor_paint.BLOODTHORN, {"H": "light", "L": "mid_light", "R": "gold_light", "M": "mid",
                                               "D": "dark", "S": "seam", "V": "void", "U": "under_light",
                                               "m": "under_mid", "u": "under_dark", "x": "under_darkest"}),
       "reforged_white_diamond": (armor_paint.WHITE_DIAMOND, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark",
                                                              "S": "seam", "V": "void", "U": "under_light",
                                                              "m": "under_mid", "u": "under_dark",
                                                              "x": "under_darkest"})}


def luma(colour):
    r, g, b = colour[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def deepen(colour, target=OUTLINE_LUMA):
    """`colour` scaled down to luma `target` (its hue kept)."""
    k = min(1.0, target / max(1.0, luma(colour)))
    return tuple(int(round(c * k)) for c in colour[:3])


def palette(metal):
    """Symbol -> RGBA for `metal` ("steel" or "bronze")."""
    tones = METALS[metal]
    colours = {symbol: tones[name] for symbol, name in {**TONES, **FITTINGS[metal]}.items()}
    colours["O"] = deepen(tones["void"])
    colours["g"] = deepen(tones["gold_dark"])
    return {symbol: tuple(colour) + (255,) for symbol, colour in colours.items()}


def own_palette(name):
    """Symbol -> RGBA for a set with maps of its own (OWN)."""
    tones, symbols = OWN[name]
    colours = {symbol: tones[tone] for symbol, tone in symbols.items()}
    colours["O"] = deepen(tones["void"])
    return {symbol: tuple(colour) + (255,) for symbol, colour in colours.items()}


def path(piece, folder=None):
    """A map's file: armor_icons/<piece>.txt, or armor_icons/<folder>/<piece>.txt for a set with its own maps."""
    return os.path.join(FOLDER, folder, piece + ".txt") if folder else os.path.join(FOLDER, piece + ".txt")


def load(piece, folder=None):
    """The map's 16 rows (comment lines, starting with #, are skipped)."""
    with open(path(piece, folder), encoding="utf-8") as f:
        rows = [line.rstrip("\n") for line in f if line.strip() and not line.startswith("#")]
    if len(rows) != SIZE or any(len(row) != SIZE for row in rows):
        raise ValueError(f"{path(piece, folder)}: a map is {SIZE} rows of {SIZE} symbols, not {[len(r) for r in rows]}")
    return rows


def icon(metal, piece):
    """The 16x16 icon of `metal`_`piece` (an RGBA image): a knight metal from the shared maps, or a set of OWN from
    its own."""
    folder = metal if metal in OWN else None
    colours = own_palette(metal) if folder else palette(metal)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(load(piece, folder)):
        for x, symbol in enumerate(row):
            if symbol == ".":
                continue
            if symbol not in colours:
                raise ValueError(f"{path(piece, folder)}: unknown symbol {symbol!r} at ({x}, {y})")
            img.putpixel((x, y), colours[symbol])
    return img
