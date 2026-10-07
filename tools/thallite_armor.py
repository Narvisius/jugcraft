"""Thallite armor (docs/features/thallite.md): the plain thallite armor and the gold-trimmed Earthbound thallite armor,
their worn 64x32 layers and inventory icons, and the Earthbinding Template. tools/gear_textures.py draws them (draw).

After the owner's chartreuse set sheet of 5 October 2026, used as a reference for the design and style only: nothing is
traced from it, and no Mojang texture is read, traced or recoloured. Every map below is drawn fresh, by hand.
- Helmet: a rounded dome over a closed, dark visor with two eye slits and a mouth-like grille.
- Chestplate: rounded shoulder plates and a round boss in the middle of the breastplate.
- Leggings: a waistband, two legs, and an emerald set in each knee.
- Boots: two boots, each with an emerald set in its cuff.
- Earthbound: the same pieces trimmed in gold: a band on the helmet's brow, the chestplate's shoulders and lower hem, a
  band on the leggings' knees above the gems, and the boots' cuffs. The emeralds are the anchors of its roots.

The icons follow the material-set rules (docs/MATERIAL_SETS.md): 16x16, a one-pixel outline in each part's darkest tone
(the transparent four-neighbours of the fill), light from the top left, flat tones. The worn layers use the same
palette and light, with bevelled plates and a few rivets, in vanilla's humanoid armor layout (armor_styles.faces).

Symbols, in the icon maps and the worn layers' face rows:
- '.' empty;
- O D M L H thallite: outline, dark, mid, light, highlight (material_icons.METAL_RAMPS["thallite"]);
- g G y Y W gold: outline, dark, mid, light, highlight (GOLD below);
- e E a A emerald: dark, mid, light, glint (GEM below);
- v the visor's slits, a near-black green.
"""
from PIL import Image

import armor_styles
import arms_variants_art
from generate_textures import GOLD_METAL
from material_icons import METAL_RAMPS, hx

THALLITE = METAL_RAMPS["thallite"]
# The mod's stand-in for vanilla's gold (GOLD_METAL in tools/generate_textures.py) for the dark, mid, light and
# highlight. Its own outline (luma 87) is lighter than thallite's dark tone, so the outline is darkened to 422806, the
# gold outline of the item icon rules (docs/ITEM_ICONS.md, PR #201).
GOLD = (hx("422806"),) + tuple(tuple(c) for c in GOLD_METAL[1:])
# A cool emerald, well to the blue of thallite's chartreuse, so it reads as a set stone: dark, mid, light, glint.
GEM = (hx("1d6b45"), hx("33b06f"), hx("8fe8bb"), hx("e6fff2"))
VISOR = hx("1b240a")

COLORS = {**dict(zip("ODMLH", THALLITE)), **dict(zip("gGyYW", GOLD)), **dict(zip("eEaA", GEM)), "v": VISOR}
OUTLINES = "Og"   # the outline symbols, thallite's and gold's: the rest is fill, which never touches air

PIECES = ("helmet", "chestplate", "leggings", "boots")

# ---------------------------------------------------------------- inventory icons (16x16)
ICONS = {
    "helmet": [
        "................",
        "................",
        ".....OOOOOO.....",
        "....OHHHLLMO....",
        "...OHHLLLLMMO...",
        "..OHLLLLLLMMDO..",
        "..OLLMMMMMMMDO..",
        "..OLDDDDDDDDMO..",
        "..OLDvvMMvvDMO..",
        "..OLDDDMMDDDMO..",
        "..OMDvDvvDvDMO..",
        "..OMDvDvvDvDDO..",
        "...OMDDDDDDDO...",
        "....OOOOOOOO....",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..OOOO....OOOO..",
        ".OHHHLO..OLLLMO.",
        ".OHLDLO..OLDMDO.",
        ".OLLLMLOOLLLMDO.",
        ".OMMMDHHHLMMDDO.",
        ".OMMMDLLLLMMDDO.",
        "..OHLLLLLLLMDO..",
        "..OHLLLDDLLMDO..",
        "..OLLLDHMDLMDO..",
        "..OLLLDMMOLMDO..",
        "..OLLLLDOLLMDO..",
        "..OLLLLLLLLMDO..",
        "...OMMMMMMMDO...",
        "....OOOOOOOO....",
        "................",
    ],
    "leggings": [
        "................",
        "...OOOOOOOOOO...",
        "..OHHHHHHHHLMO..",
        "..OLLLLLLLLMDO..",
        "..ODDDDDDDDDDO..",
        "..OLMDOOOOLMDO..",
        "..OLMDO..OLMDO..",
        "..OLMDO..OLMDO..",
        "..OAaEO..OAaEO..",
        "..OaEeO..OaEeO..",
        "..OEeeO..OEeeO..",
        "..OLMDO..OLMDO..",
        "..OMDDO..OMDDO..",
        "...OOO....OOO...",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "...OOO....OOO...",
        "..OHLMO..OHLMO..",
        "..OAaEO..OAaEO..",
        "..OaEeO..OaEeO..",
        "..OLMDO..OLMDO..",
        "..OLMDO..OLMDO..",
        ".OLLMDO..OLMMLO.",
        "OHLLMDO..OLMMLMO",
        "ODDDDDO..ODDDDDO",
        ".OOOOO....OOOOO.",
        "................",
        "................",
        "................",
    ],
}

EARTHBOUND_ICONS = {
    "helmet": [
        "................",
        "................",
        ".....OOOOOO.....",
        "....OHHHLLMO....",
        "...OHHLLLLMMO...",
        "..OHLLLLLLMMDO..",
        "..gWYYYyyyyGGg..",
        "..OLDDDDDDDDMO..",
        "..OLDvvMMvvDMO..",
        "..OLDDDMMDDDMO..",
        "..OMDvDvvDvDMO..",
        "..OMDvDvvDvDDO..",
        "...OMDDDDDDDO...",
        "....OOOOOOOO....",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..gggg....gggg..",
        ".gWYYyg..gYyyGg.",
        ".gYLDLO..OLDMGg.",
        ".gYLLMLOOLLLMGg.",
        ".gyMMDHHHLMMDGg.",
        ".OMMMDLLLLMMDDO.",
        "..OHLLLLLLLMDO..",
        "..OHLLLDDLLMDO..",
        "..OLLLDHMDLMDO..",
        "..OLLLDMMOLMDO..",
        "..OLLLLDOLLMDO..",
        "..OLLLLLLLLMDO..",
        "...gWYYyyyGGg...",
        "....gggggggg....",
        "................",
    ],
    "leggings": [
        "................",
        "...OOOOOOOOOO...",
        "..OHHHHHHHHLMO..",
        "..OLLLLLLLLMDO..",
        "..ODDDDDDDDDDO..",
        "..OLMDOOOOLMDO..",
        "..OLMDO..OLMDO..",
        "..gWyGg..gYyGg..",
        "..OAaEO..OAaEO..",
        "..OaEeO..OaEeO..",
        "..OEeeO..OEeeO..",
        "..OLMDO..OLMDO..",
        "..OMDDO..OMDDO..",
        "...OOO....OOO...",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "...ggg....ggg...",
        "..gWyGg..gYyGg..",
        "..OAaEO..OAaEO..",
        "..OaEeO..OaEeO..",
        "..gWyGg..gYyGg..",
        "..OLMDO..OLMDO..",
        ".OLLMDO..OLMMLO.",
        "OHLLMDO..OLMMLMO",
        "ODDDDDO..ODDDDDO",
        ".OOOOO....OOOOO.",
        "................",
        "................",
        "................",
    ],
}

# The Earthbinding Template's emblem (10x8, laid on arms_variants_art.parchment16's sheet): a green shoot of thallite
# with gold roots.
TEMPLATE_EMBLEM = [
    "....OO....",
    ".OO.OHO.OO",
    "OHLOOLOOMO",
    "OLMDLDLMDO",
    ".OOOLDOOO.",
    "...OLDO...",
    "..gYyGGg..",
    "..Yg.gGg..",
]

# ---------------------------------------------------------------- worn layers (64x32)
# Face rows per box face (armor_styles.faces); faces not listed stay empty, as vanilla's do: the helmet's underside and
# hat layer, the arms' hand ends, the boots' tops, and above the leggings' waistband and below their knees.
HEAD_TOP = ["MLLHLLMD", "LLLHLLMD", "LLLHLLMD", "LLLHLLMD", "LLLHLLMD", "LLLHLLMD", "LLLHLLMD", "MLLHLLMD"]
# The visor: the brow, then two eye slits over a nasal bar, and the grille.
HEAD_FRONT = ["LHHHHLLM", "LLLLLLMD", "MMMMMMMD", "MvvDDvvD", "MDDLMDDD", "MvDvvDvD", "MvDvvDvD", "DDDDDDDO"]
HEAD_SIDE = ["MLLLLLLM", "LLLLLLLM", "MMMMMMMD", "DMLLLLMD", "DMLHLLMD", "DMLLLLMD", "DMMMMMMD", "ODDDDDDO"]
HEAD_BACK = ["MLLLLLLM", "LLLLLLLM", "MMMMMMMD", "LLLDLLMD", "LHLDLHMD", "LLLDLLMD", "MMMDMMMD", "ODDDDDDO"]
# The breastplate's round boss, a seam at the waist and the lower hem; a ridge down the backplate.
BODY_FRONT = ["DLHHHHLD", "LHLLLLMD", "LLLDDLMD", "LLDHMDMD", "LLDMMOMD", "LLLDOLMD", "LLLLLLMD", "MLLLLLMD",
              "DDDDDDDD", "LLLLLLMD", "MMMMMMMD", "DDDDDDDO"]
BODY_BACK = ["DLLLLLLD", "LLLHLLMD", "LHLHLHMD", "LLLHLLMD", "LLLHLLMD", "LLLHLLMD", "LLLHLLMD", "MMMHMMMD",
             "DDDDDDDD", "LHLLLHMD", "MMMMMMMD", "DDDDDDDO"]
BODY_SIDE = ["LLMD", "LLMD", "LLMD", "LHMD", "LLMD", "LLMD", "LLMD", "MMMD", "DDDD", "LLMD", "MMMD", "DDDO"]
# A shoulder plate (rows 0 to 4) over the upper arm and the forearm.
ARM = ["LHHL", "LLLM", "LHLM", "LLMD", "MMMD", "DDDD", "LLMD", "LLMD", "MMMD", "DDDD", "LLMD", "DDDO"]
# The boots, on the legs' lowest five rows: the cuff, the emerald on the front, the foot.
BOOT = ["...."] * 7 + ["HLLM", "LLMD", "LHMD", "LLMD", "DDDO"]
BOOT_FRONT = ["...."] * 7 + ["HLLM", "LAaD", "LaED", "LLMD", "DDDO"]
# The leggings: the waistband on the body's lowest four rows, then the thigh, the knee plate with its emerald, and the
# shin under the boots. The legs' top row and the waistband's bottom row overlap on the same plane in vanilla's model,
# so both are the belt's dark edge: whichever wins the depth test, it looks the same.
WAIST_FRONT = ["........"] * 8 + ["HHLLLLLM", "LHLLLLHD", "MMMMMMMD", "DDDDDDDD"]
WAIST_BACK = ["........"] * 8 + ["LLLLLLLM", "LHLLLLHD", "MMMMMMMD", "DDDDDDDD"]
WAIST_SIDE = ["...."] * 8 + ["HLLM", "LLMD", "MMMD", "DDDD"]
LEG = ["DDDD", "HLLM", "MMMD", "HLLM", "LLMD", "LLMD", "DDDD", "LLMD", "MMDD"] + ["...."] * 3
LEG_FRONT = ["DDDD", "HLLM", "MMMD", "HLLM", "LAaD", "LaED", "DDDD", "LLMD", "MMDD"] + ["...."] * 3

LAYERS = {
    "layer1": {
        "head": {"top": HEAD_TOP, "front": HEAD_FRONT, "right": HEAD_SIDE, "left": HEAD_SIDE, "back": HEAD_BACK},
        "body": {"top": ["LLLLLLLM", "LHLLLLHM", "LLLLLLLM", "MMMMMMMD"], "bottom": ["DDDDDDDD"] * 4,
                 "front": BODY_FRONT, "back": BODY_BACK, "right": BODY_SIDE, "left": BODY_SIDE},
        "arm": {"top": ["HHLM", "HLLM", "LLMD", "MMDD"], "front": ARM, **armor_styles._sides(ARM)},
        "leg": {"bottom": ["DDDD"] * 4, "front": BOOT_FRONT, **armor_styles._sides(BOOT)},
    },
    "layer2": {
        "body": {"front": WAIST_FRONT, "back": WAIST_BACK, "right": WAIST_SIDE, "left": WAIST_SIDE},
        "leg": {"front": LEG_FRONT, **armor_styles._sides(LEG)},
    },
}


def _trim(rows, at, *trim):
    """A plain face with Earthbound's gold rows laid over it, from row at down."""
    return rows[:at] + list(trim) + rows[at + len(trim):]


# Earthbound's layers: the plain faces with the gold trim where the icons have it. A band round the helmet's brow, the
# shoulder plates' lower rims and the chestplate's lower hem, a band over the knee plates, and the boots' cuffs, with
# the boots' emeralds set in gold.
EB_HEAD_SIDE = _trim(HEAD_SIDE, 2, "YYYyyyyG")
EB_BODY_SIDE = _trim(BODY_SIDE, 11, "yyyG")
EB_ARM = _trim(ARM, 4, "WYyG")
EB_BOOT = _trim(BOOT, 7, "WYyG")
EB_LEG = _trim(LEG, 3, "WYyG")
EARTHBOUND_LAYERS = {
    "layer1": {
        "head": {"top": HEAD_TOP, "front": _trim(HEAD_FRONT, 2, "WYYYyyyG"), "right": EB_HEAD_SIDE,
                 "left": EB_HEAD_SIDE, "back": _trim(HEAD_BACK, 2, "YYYyyyyG")},
        "body": {**LAYERS["layer1"]["body"], "front": _trim(BODY_FRONT, 11, "WYYyyyyG"),
                 "back": _trim(BODY_BACK, 11, "YYyyyyGG"), "right": EB_BODY_SIDE, "left": EB_BODY_SIDE},
        "arm": {**LAYERS["layer1"]["arm"], "front": EB_ARM, **armor_styles._sides(EB_ARM)},
        "leg": {**LAYERS["layer1"]["leg"], "front": _trim(BOOT_FRONT, 7, "WYyG", "YAaG", "yaEG"),
                **armor_styles._sides(EB_BOOT)},
    },
    "layer2": {
        "body": LAYERS["layer2"]["body"],
        "leg": {"front": _trim(LEG_FRONT, 3, "WYyG"), **armor_styles._sides(EB_LEG)},
    },
}

def _paint(img, rect, rows):
    armor_styles._paint(img, rect, rows, (), COLORS)


def icon(piece, earthbound):
    """A piece's inventory icon (16x16): plain thallite, or Earthbound."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _paint(img, (0, 0, 16, 16), (EARTHBOUND_ICONS if earthbound else ICONS)[piece])
    return img


def layer(earthbound, leggings):
    """A worn layer (64x32): layer 1 (humanoid: helmet, chestplate and boots), or layer 2 (humanoid_leggings)."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for box, face_rows in (EARTHBOUND_LAYERS if earthbound else LAYERS)["layer2" if leggings else "layer1"].items():
        rects = armor_styles.faces(box)
        for face, rows in face_rows.items():
            _paint(img, rects[face], rows)
    return img


def template_icon():
    """The Earthbinding Template (16x16): the smithing patterns' rolled parchment with a shoot of thallite on gold
    roots."""
    return arms_variants_art.parchment16(TEMPLATE_EMBLEM, COLORS)


def draw(save, save_armor):
    """save(img, kind, name) and save_armor(img, layer, name) as in tools/generate_textures.py."""
    for earthbound, prefix in ((False, "thallite"), (True, "earthbound_thallite")):
        for piece in PIECES:
            save(icon(piece, earthbound), "item", f"{prefix}_{piece}")
        save_armor(layer(earthbound, False), "humanoid", prefix)
        save_armor(layer(earthbound, True), "humanoid_leggings", prefix)
    save(template_icon(), "item", "earthbinding_template")
