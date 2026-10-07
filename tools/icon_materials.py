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

MATERIALS = {
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
}

# Materials meant to be vanilla's own metal: the distance test against copper, iron and gold is skipped for them.
VANILLA = {"copper", "iron", "gold"}

# Palettes the owner approved although they sit close to copper, iron or gold, by name (a material here, an arms style
# in arms_pixel.STYLES or an Arms VII line in arms_variants_art.LINE_STYLES), with the owner's words and the date. The
# distance test is skipped for them; the other palette rules still hold. None so far: the chosen bronze and steel pass.
OWNER_APPROVED = {}
