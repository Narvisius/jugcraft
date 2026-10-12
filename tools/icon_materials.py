"""The named materials an item icon map is coloured in (docs/ITEM_ICONS.md, rule 6 and "Maps and the checker").

A map names what each pixel is made of by role, never a colour (O D M L H the main material, g f F Y a second one, w b B
wood, k K a wrap, a A a stone, e E an accent). A map that is not an arm declares which material fills each role:

    # materials: main=copper second=brass wood=wood

and tools/check_icon_maps.py previews it in those materials and checks each one's ramp. The arms are coloured in their
style's materials instead (tools/arms_pixel.py STYLES, tools/arms_variants_art.py LINE_STYLES).

Each material is an arms_pixel.Material, a ramp darkest first: outline (away from the light), outline (towards it, used
by the 3D models and by the icons only for the chain's `c`), dark, mid, light, highlight. A new material is added here,
with a comment saying where it is used. Every colour here is drawn fresh; none is sampled from Mojang's textures.
"""
import arms_pixel as px
from arms_pixel import Material

# Vanilla's own metals, for an item that is meant to be made of one (copper wire, iron parts, gold trim, the steampunk
# tier's copper). Their dark, mid and light tones are the mod's stand-ins in tools/generate_textures.py (COPPER_METAL,
# IRON_METAL, GOLD_METAL, which copper_plate and copper_dust use); the outline is darkened to the approved metals' depth
# (the stand-ins' own outlines, 67 to 88 luma, made a test coin soft), and copper's highlight is lifted into the
# owner's range. Provisional: not yet shown to the owner.
COPPER = Material((64, 30, 16), (110, 52, 30), (156, 78, 46), (196, 108, 66), (226, 142, 96), (250, 206, 166))
IRON = Material((38, 38, 44), (88, 88, 92), (130, 130, 136), (170, 170, 176), (204, 204, 208), (232, 232, 236))
GOLD = Material((66, 40, 6), (120, 84, 14), (186, 140, 28), (230, 190, 50), (248, 222, 100), (255, 246, 180))

# Clear glass, for a bottle or jar in the second role (g f F Y): a dark blue-grey edge and pale panes. Provisional.
GLASS = Material((34, 44, 58), (60, 76, 94), (88, 112, 134), (150, 178, 200), (214, 232, 242), (244, 250, 255))

# The war machines' paints and stuffs (tools/item_icons/: the big guns, shells, Landship, Diesel Walker, Zeppelin and
# balloon), in the dieselpunk tier's manner (a hazard yellow, olive and khaki drab, a signal red) beside the approved
# steel and gunmetal. Hazard yellow is a paint, not a metal: it is only ever a second material, so it is not tested
# against gold. Concrete is the tower plinths', in the wood role (an outline and two flat tones). Canvas is the
# envelopes' doped cloth: a pale material, so its outline may be a third of its mid tone's luma.
HAZARD_YELLOW = Material((60, 40, 6), (100, 70, 10), (156, 108, 14), (214, 164, 30), (244, 204, 72), (255, 236, 156))
CONCRETE = Material((40, 40, 38), (70, 70, 66), (102, 101, 96), (140, 138, 130), (172, 170, 162), (204, 202, 194),
                    shine=False)
CANVAS = Material((62, 54, 36), (100, 90, 64), (146, 132, 96), (196, 184, 146), (224, 214, 180), (246, 240, 220),
                  shine=False)
KHAKI = Material((38, 34, 20), (70, 62, 40), (100, 90, 58), (140, 128, 86), (176, 164, 116), (214, 204, 160))
# The Landship's black lacquer: a dark material, so its light and highlight carry the read (ITEM_ICONS.md, rule 4).
LACQUER = Material((16, 16, 22), (30, 30, 40), (50, 50, 62), (70, 70, 86), (102, 102, 122), (156, 156, 178))
RED_PAINT = Material((52, 14, 12), (90, 22, 18), (132, 30, 24), (180, 48, 36), (220, 84, 62), (246, 150, 120))

# The Arcane Concordance's (tools/item_icons/: the wands, the Kindled Lantern, the Research Notes and the codex).
# Amethyst is its crystals' and the codex's violet: a stone, never a metal, so it is kept far from copper, iron and gold
# by its hue. Paper is a pale material (its outline may be a third of its mid tone's luma), for the notes and the
# codex's page edges.
AMETHYST = Material((44, 20, 64), (70, 36, 100), (98, 54, 140), (140, 88, 190), (186, 138, 228), (228, 198, 250))
# Smoked glass, the unlit Kindled Lantern's dark panes: a blue-violet dark glass, so that it never reads as iron.
SMOKED_GLASS = Material((14, 14, 28), (28, 28, 50), (44, 46, 80), (66, 70, 112), (104, 110, 160), (168, 176, 222))
# Reaper's Shade, Vesperine's night-black cloth (tools/item_icons/: the Reaper's Shade and the Reaper's Hood): a black
# with a violet cast, its light and highlight carrying the read as lacquer's do. A cloth, so it does not shine.
SHADE = Material((18, 12, 26), (34, 24, 48), (52, 40, 72), (72, 56, 98), (100, 80, 134), (150, 126, 196), shine=False)
# Her robe's crimson lining (the Reaper's Hood's): a deep cloth red, darker and duller than garnet.
CRIMSON_CLOTH = Material((34, 6, 12), (62, 12, 22), (100, 22, 38), (134, 32, 50), (170, 48, 64), (206, 84, 96),
                         shine=False)
# The Frost Horn's goat horn (tools/item_icons/frost_horn.txt, the Glacier Hall's ritual): a warm grey ivory. A horn,
# not a metal, so it does not shine.
HORN = Material((48, 40, 30), (80, 68, 50), (112, 94, 68), (158, 138, 102), (200, 180, 140), (234, 220, 186),
                shine=False)
# The Yeti King's white fur (tools/item_icons/yeti_fur.txt and yeti_mitten.txt): white with blue-grey shadows, as his
# body is painted (tools/yeti_king_art.py FUR). Fur, so it does not shine.
YETI_FUR = Material((40, 50, 76), (70, 84, 114), (112, 136, 184), (174, 198, 234), (216, 230, 250), (246, 250, 255),
                    shine=False)
# The Kiln Seal's (tools/item_icons/kiln_seal.txt, the Cinder Kiln's ritual): obsidian, a glassy violet-black whose light
# and highlight carry the read as lacquer's do; and magma, the glowing orange of its eye's blaze-powder marks, only ever
# an accent.
OBSIDIAN = Material((14, 8, 24), (30, 20, 46), (50, 36, 76), (72, 54, 106), (104, 84, 148), (156, 134, 206))
MAGMA = Material((60, 14, 6), (96, 28, 10), (124, 40, 12), (178, 66, 16), (230, 116, 30), (250, 176, 70), shine=False)
PAPER = Material((80, 64, 40), (124, 104, 74), (170, 152, 116), (214, 200, 166), (236, 226, 198), (250, 246, 230),
                 shine=False)
# The Greenwardens' garden (roadmap step 14; tools/item_icons/: the four crops, Verdant Chaff and the living devices).
# Leaf is the plants' green and moss a blue-green; gloam is the Gloamcap's dusky violet cap, and stem its pale stalk;
# petal is the Sunpetal's warm yellow, a flower and never a metal, so it is only a second material and is not tested
# against gold; straw is the chaff's dry stalks; terracotta is the devices' clay pots (a second material only, beside
# the green); dew is the moss's drops, an accent. Drawn fresh for these icons.
LEAF = Material((22, 46, 18), (40, 72, 30), (46, 92, 34), (70, 128, 48), (106, 166, 62), (156, 206, 96), shine=False)
MOSS = Material((16, 44, 40), (30, 70, 62), (34, 86, 72), (52, 124, 100), (84, 164, 128), (150, 212, 180), shine=False)
GLOAM = Material((36, 20, 52), (60, 36, 86), (72, 44, 110), (104, 70, 150), (142, 104, 190), (196, 164, 232),
                 shine=False)
STEM = Material((64, 56, 44), (100, 90, 72), (150, 140, 116), (206, 196, 170), (236, 228, 206), (250, 246, 232),
                shine=False)
PETAL = Material((82, 50, 8), (130, 84, 14), (184, 116, 22), (232, 176, 40), (250, 214, 88), (252, 236, 160),
                 shine=False)
STRAW = Material((54, 42, 12), (84, 66, 26), (120, 96, 40), (170, 140, 66), (206, 180, 96), (232, 214, 150), shine=False)
TERRACOTTA = Material((52, 24, 14), (86, 40, 24), (130, 62, 38), (172, 90, 56), (204, 124, 80), (230, 166, 120),
                      shine=False)
DEW = Material((30, 60, 90), (60, 100, 140), (90, 140, 190), (126, 192, 232), (206, 238, 255), (240, 250, 255))

MATERIALS = {
    # the Greenwardens' garden
    "leaf": LEAF, "moss": MOSS, "gloam": GLOAM, "stem": STEM, "petal": PETAL, "straw": STRAW, "terracotta": TERRACOTTA,
    "dew": DEW,
    # the approved metals ("I like the alternate versions for steel and bronze")
    "bronze": px.BRONZE, "steel": px.STEEL,
    # the arms' other materials (leather, rubber and cloth are wraps: their own outline and dark tones are never drawn,
    # so they fail the ramp rules if declared as a main or second material)
    "brass": px.BRASS, "gunmetal": px.GUNMETAL, "leather": px.LEATHER, "rubber": px.RUBBER, "wood": px.WOOD,
    "dark_wood": px.DARK_WOOD, "garnet": px.GARNET, "phosphor": px.PHOSPHOR, "cloth_red": px.CLOTH_RED,
    "olive": px.OLIVE,
    # vanilla's metals and glass
    "copper": COPPER, "iron": IRON, "gold": GOLD, "glass": GLASS,
    # the war machines' (tools/item_icons/)
    "hazard_yellow": HAZARD_YELLOW, "concrete": CONCRETE, "canvas": CANVAS, "khaki": KHAKI, "red_paint": RED_PAINT,
    "lacquer": LACQUER,
    # the Arcane Concordance's
    "amethyst": AMETHYST, "paper": PAPER, "smoked_glass": SMOKED_GLASS,
    # Vesperine's loot
    "shade": SHADE, "crimson_cloth": CRIMSON_CLOTH,
    # the Glacier Hall's, and the Yeti King's loot
    "horn": HORN, "yeti_fur": YETI_FUR,
    # the Cinder Kiln's
    "obsidian": OBSIDIAN, "magma": MAGMA,
}

# Materials meant to be vanilla's own metal: the distance test against copper, iron and gold is skipped for them.
VANILLA = {"copper", "iron", "gold"}

# Palettes the owner approved although they sit close to copper, iron or gold, by name (a material here, an arms style
# in arms_pixel.STYLES or an Arms VII line in arms_variants_art.LINE_STYLES), with the owner's words and the date. The
# distance test is skipped for them; the other palette rules still hold. None so far: the chosen bronze and steel pass.
OWNER_APPROVED = {}
