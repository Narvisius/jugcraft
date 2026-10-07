"""Tool and armor textures (batch 25, docs/features/tools-and-armor.md): bronze and steel swords, pickaxes, axes,
shovels and hoes, and paxels for every tier. Bronze and steel armor's icons are the knight armor's (armor_icons.py;
docs/features/knight-armor.md), worn as 3D models. Steampunk and Kaiser Armor (docs/features/steampunk-and-kaiser-armor.md)
are drawn in armor_styles.py, icons and worn layers.

All original: each icon is a hand-drawn mask below, coloured from a five-shade palette (0 darkest .. 4 lightest);
handles are oak brown.
"""
from PIL import Image

import armor_icons
import armor_styles
import arms_variants_art
import gear
import hitech
import thallite_armor

# Mask characters: digits are the head's palette shade; h/H the handle (dark/light); . is empty.
HANDLE = [(58, 40, 22), (98, 70, 40)]

SWORD = [
    "................",
    "............344.",
    "...........3442.",
    "..........3442..",
    ".........3442...",
    "........3442....",
    ".......3442.....",
    "..1...3442......",
    "..21.3442.......",
    "...2342.........",
    "...1H2..........",
    "..hH.21.........",
    ".hH....1........",
    "1h..............",
    "11..............",
    "................",
]

PICKAXE = [
    "................",
    "....233332......",
    "...3444444431...",
    "..341....hH443..",
    "..21....hH..342.",
    "..1....hH....42.",
    "......hH.....32.",
    ".....hH......31.",
    "....hH........1.",
    "...hH...........",
    "..hH............",
    ".hH.............",
    "hH..............",
    "h...............",
    "................",
    "................",
]

AXE = [
    "................",
    ".......2333.....",
    "......244443....",
    ".....24444432...",
    ".....2443hH31...",
    "......23hH31....",
    ".......hH1......",
    "......hH........",
    ".....hH.........",
    "....hH..........",
    "...hH...........",
    "..hH............",
    ".hH.............",
    "hH..............",
    "h...............",
    "................",
]

SHOVEL = [
    "................",
    "...........233..",
    "..........24443.",
    ".........244443.",
    "..........44432.",
    ".........hH332..",
    "........hH.21...",
    ".......hH.......",
    "......hH........",
    ".....hH.........",
    "....hH..........",
    "...hH...........",
    "..hH............",
    ".hH.............",
    "hH..............",
    "................",
]

HOE = [
    "................",
    "......23333.....",
    ".....2444443....",
    ".....21..hH43...",
    "........hH..1...",
    ".......hH.......",
    "......hH........",
    ".....hH.........",
    "....hH..........",
    "...hH...........",
    "..hH............",
    ".hH.............",
    "hH..............",
    "h...............",
    "................",
    "................",
]

# A paxel: a pick head on one side, an axe blade on the other and a shovel tip on top, on one long handle.
PAXEL = [
    "..........2333..",
    "...23332.244443.",
    "..34444443444432",
    "..341...hH44431.",
    "..21...hH.3442..",
    "..1...hH...321..",
    "......hH........",
    ".....hH.........",
    "....hH..........",
    "...hH...........",
    "..hH............",
    ".hH.............",
    "hH..............",
    "h...............",
    "................",
    "................",
]


# The power bow (batch 27). Letters: R rubber (black), G glass (pale blue), Y yellow (hazard paint), and the digits are steel.
BOW_BASE = [
    "........RR......",
    ".......R4.W.....",
    "......R4..W.....",
    ".....43...W.....",
    "....43....W.....",
    "...43.....W.....",
    "..43......W.....",
    "..2YG.....W.....",
    "..2YG.....W.....",
    "..43......W.....",
    "...43.....W.....",
    "....43....W.....",
    ".....43...W.....",
    "......R4..W.....",
    ".......R4.W.....",
    "........RR......",
]
EXTRA_COLORS = {"R": (32, 32, 36), "G": (120, 220, 255), "Y": (232, 186, 40), "W": (226, 226, 220)}

TOOLS = {"sword": SWORD, "pickaxe": PICKAXE, "axe": AXE, "shovel": SHOVEL, "hoe": HOE}

# Head palettes for the paxel tiers that are not Jugcraft metals (darkest .. lightest), our own picks.
VANILLA_TIERS = {
    "wood": [(64, 44, 22), (96, 68, 36), (128, 94, 52), (160, 122, 72), (190, 152, 98)],
    "stone": [(58, 58, 58), (88, 88, 88), (116, 116, 116), (142, 142, 142), (170, 170, 170)],
    "iron": [(96, 96, 96), (150, 150, 150), (196, 196, 196), (224, 224, 224), (248, 248, 248)],
    "gold": [(140, 96, 20), (196, 150, 30), (234, 196, 52), (250, 226, 96), (255, 246, 170)],
    "diamond": [(18, 92, 86), (30, 150, 140), (60, 206, 196), (130, 236, 226), (206, 252, 248)],
    "netherite": [(30, 26, 30), (50, 44, 48), (72, 64, 68), (96, 88, 92), (122, 114, 116)],
}


def icon(mask, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch.isdigit():
                img.putpixel((x, y), tuple(palette[int(ch)]) + (255,))
            elif ch in "hH":
                img.putpixel((x, y), HANDLE[ch == "H"] + (255,))
            elif ch in EXTRA_COLORS:
                img.putpixel((x, y), EXTRA_COLORS[ch] + (255,))
    return img


def bow_drawn(step):
    """The power bow drawn back: the string pulled further left each step, with an energy bolt nocked."""
    rows = [list(r) for r in BOW_BASE]
    for row in rows:
        for x, ch in enumerate(row):
            if ch == "W":
                row[x] = "."
    pull = 10 - 2 * (step + 1)
    for y in range(1, 15):
        bend = pull - min(abs(y - 7.5), 6) * 0.5
        rows[y][max(5, int(round(bend)))] = "W"
    for x in range(max(3, int(pull) - 4), 15):
        rows[8][x] = "G" if x > int(pull) else rows[8][x]
    return ["".join(r) for r in rows]


# The flat worn layers bronze and steel armor keep as a fallback (no equipment asset draws them; they wear the knight
# armor's 3D models): the stylized looks they were first drawn in (2 October 2026), which Steampunk and Kaiser Armor keep.
METAL_ARMOR_LOOK = {"bronze": "steampunk", "steel": "kaiser"}


def draw_all(save, save_armor, part_palette):
    """save(img, kind, name) as in generate_textures; save_armor(img, layer, name) for the worn layers."""
    for metal in ("bronze", "steel"):
        palette = part_palette(metal)
        for tool, mask in TOOLS.items():
            save(icon(mask, palette), "item", f"{metal}_{tool}")
        # Armor: the icons are the knight armor's (tools/armor_icons.py, a hand-drawn map a piece), and the worn look is
        # its 3D model (tools/knight_armor.py). These flat layers, in the stylized looks Steampunk and Kaiser Armor keep,
        # are a fallback that no equipment asset draws.
        for piece in armor_icons.PIECES:
            save(armor_icons.icon(metal, piece), "item", f"{metal}_{piece}")
        look = METAL_ARMOR_LOOK[metal]
        armor = armor_styles.PALETTES[look]
        save_armor(armor_styles.layer(look, armor, False), "humanoid", metal)
        save_armor(armor_styles.layer(look, armor, True), "humanoid_leggings", metal)
    # Steampunk and Kaiser Armor: the stylized looks, unchanged (tools/armor_styles.py), and their patterns.
    for style, armor in armor_styles.PALETTES.items():
        for piece in gear.ARMOR:
            save(armor_styles.icon(style, piece, armor), "item", f"{style}_{piece}")
        save_armor(armor_styles.layer(style, armor, False), "humanoid", style)
        save_armor(armor_styles.layer(style, armor, True), "humanoid_leggings", style)
        save(arms_variants_art.pattern16(style), "item", gear.ARMOR_STYLES[style]["template"])
    # Thallite armor, plain and Earthbound, and the Earthbinding Template: hand-drawn maps (tools/thallite_armor.py).
    thallite_armor.draw(save, save_armor)

    # The armor-only tiers (gear.ARMOR_TIERS; Bloodthorn first): icons from each tier's own maps
    # (tools/armor_icons/<tier>/), coloured as its 3D model is painted; worn, each is only that model, with no flat layer.
    for tier in gear.ARMOR_TIERS:
        for piece in armor_icons.PIECES:
            save(armor_icons.icon(tier, piece), "item", f"{tier}_{piece}")
    steel = part_palette("steel")
    save(icon(BOW_BASE, steel), "item", "power_bow")
    for step in range(3):
        save(icon(bow_drawn(step), steel), "item", f"power_bow_pulling_{step}")
    # The rest of the high-tech gear is drawn at double resolution in tools/hitech.py.
    save(hitech.scuba_mask_icon(), "item", "scuba_mask")
    save(hitech.scuba_tank_icon(), "item", "scuba_tank")
    save(hitech.free_runners_icon(), "item", "free_runners")
    save(hitech.katana(), "item", "power_katana", animation={"frametime": 2})
    save_armor(hitech.scuba_layer(), "humanoid", "scuba")
    save_armor(hitech.runners_layer(), "humanoid", "free_runners")
    for tier, palette in VANILLA_TIERS.items():
        save(icon(PAXEL, palette), "item", f"{tier}_paxel")
    # Bronze and steel tools and paxels: the material-set maps (tools/material_icons.py, docs/MATERIAL_SETS.md), drawn
    # over the masks the loop at the top of this function still draws for them.
    import material_icons
    material_icons.draw_tools(save)
