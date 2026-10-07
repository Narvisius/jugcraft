"""Halloween decorations, batch 17: the Witch's Workshop (docs/features/witchs-workshop.md), the first five prop sets of
the Witching Season plan (docs/features/witching-season.md), drawn from the owner's reference pictures. Models and
textures: tools/decor17_data.py, sculpted on the toolkit in tools/flora_art.py.

1. Horned Skull Cauldron (agriculture/HornedSkullCauldronBlock): an iron pot with a ram's skull on its front that holds
   water or `levels` bottles of one potion, bubbles over heat, floats up to `floaters` things, and, stirred with the
   Brew Ladle over heat, wafts a potion's effect onto up to `waft_players` players within `waft_range` blocks at
   `waft_fraction` of its duration. The Ember Bed is a heat source (block tag jugcraft:heat_sources).
2. Wrought-iron candelabra (agriculture/CandelabrumBlock and its kinds): iron fittings whose candles (CANDLES, drawn by
   the client) take one of WAXES and burn with one of FLAMES; wax drips grow while lit (`drip_stages` stages, one
   random tick in `drip_chance`) and shears scrape them off.
3. Enchanted Broom and Dustpan (agriculture/EnchantedBroomBlock, DustpanBlock): rubbed with Flying Ointment the broom
   sweeps dropped items within `range` blocks to a Dustpan every `sweep_ticks`, at most `max_moves` a sweep, for
   `charge_ticks`. The Broom Rack shows three brooms.
4. Cabinet of Curiosities (agriculture/ShowcaseBlock): the Curiosity Cabinet shows nine things, the Bell Jar one, the
   Moth Display Case pinned moths that stir at night.
5. Oddity Jars (agriculture/OddityJarBlock): eyeballs that watch, a heart that beats as a redstone clock, a bat that
   wakes (in a wider, taller jar, so its wings stay inside the glass), a two-headed snake and a drumming hand.
6. The bigger jars, several blocks placed and broken as one (agriculture/MultiDecorationBlock with a third, "away",
   axis), each a small jar made bigger and nothing else: the Giant's Beating Heart (GIANT_HEART, the Beating Heart Jar
   three times over, 3 x 3 x 3, GiantBeatingHeartBlock), a slow redstone clock whose heart swells with each beat, and
   the Tall Specimen Jar (the Specimen Jar twice over, 1 x 2 x 1) and Specimen Tank (three times over, 3 x 3 x 3)
   (SPECIMEN_VESSELS, SpecimenVesselBlock), each specimen floating as big in its glowing fluid.
Every recipe follows the agriculture feature switch.
"""

FEATURE = "agriculture"

CAULDRON = {"block": "horned_skull_cauldron", "display": "Horned Skull Cauldron", "levels": 3, "potion_light": 6, "heat_light": 2,
            "floaters": 3, "waft_players": 4, "waft_range": 3.0, "waft_fraction": 0.25,
            # The ram skull's face (its north side, at z 0.7) and its square eye sockets, {x0, y0, x1, y1} in pixels: Minecraft
            # proportions, each socket a quarter of the face wide, a little over a quarter apart, an eighth in from the sides.
            # The client's glow (HornedSkullCauldronRenderer.EYES) covers the sockets exactly, at `eye_z`, 0.1 in front.
            "skull_face": (5.2, 7.4, 10.8, 11.8), "skull_eyes": [(5.8, 8.6, 7.2, 9.8), (8.8, 8.6, 10.2, 9.8)], "skull_face_z": 0.7, "eye_z": 0.6}
EMBER_BED = {"block": "ember_bed", "display": "Ember Bed", "light": 9}
LADLE = {"item": "brew_ladle", "display": "Brew Ladle"}

# Each fitting: its name, light when lit, whether it is two blocks tall, and each candle's (x, y, z, height) in pixels:
# the bottom middle of the candle in the fitting's block (the lower block, for a tall one), for a fitting facing north.
CANDELABRA = {
    "floor_candelabrum": {"display": "Floor Candelabrum", "light": 14, "tall": True, "candles": [
        (1.5, 18.5, 8.0, 5.0), (14.5, 18.5, 8.0, 5.0), (8.0, 18.5, 1.5, 5.0), (8.0, 18.5, 14.5, 5.0),
        (4.5, 22.5, 8.0, 5.5), (11.5, 22.5, 8.0, 5.5), (8.0, 26.0, 8.0, 6.0)]},
    "table_candelabrum": {"display": "Table Candelabrum", "light": 9, "tall": False, "candles": [
        (3.0, 8.5, 8.0, 4.5), (13.0, 8.5, 8.0, 4.5), (8.0, 11.0, 8.0, 5.0)]},
    "wall_girandole": {"display": "Wall Girandole", "light": 9, "tall": False, "candles": [
        (3.0, 7.0, 10.0, 4.5), (13.0, 7.0, 10.0, 4.5), (8.0, 10.0, 9.0, 5.0)]},
    "branching_chandelier": {"display": "Branching Chandelier", "light": 15, "tall": False, "candles": [
        (-10.0, -5.5, 8.0, 4.5), (26.0, -5.5, 8.0, 4.5), (8.0, -5.5, -10.0, 4.5), (8.0, -5.5, 26.0, 4.5),
        (-1.0, -5.5, -1.0, 4.5), (17.0, -5.5, -1.0, 4.5), (-1.0, -5.5, 17.0, 4.5), (17.0, -5.5, 17.0, 4.5),
        (2.5, -0.5, 2.5, 4.5), (13.5, -0.5, 2.5, 4.5), (2.5, -0.5, 13.5, 4.5), (13.5, -0.5, 13.5, 4.5),
        (4.0, 4.0, 8.0, 4.0), (12.0, 4.0, 8.0, 4.0), (8.0, 4.0, 4.0, 4.0), (8.0, 4.0, 12.0, 4.0)]},
}
CANDLE_WIDTH = 1.6
# Wax colours (RGB) and the dyes that give them.
WAXES = {"ivory": {"rgb": (236, 226, 200), "dyes": ["minecraft:white_dye", "minecraft:bone_meal"]},
         "black": {"rgb": (52, 46, 58), "dyes": ["minecraft:black_dye"]},
         "purple": {"rgb": (128, 66, 176), "dyes": ["minecraft:purple_dye"]},
         "green": {"rgb": (76, 142, 74), "dyes": ["minecraft:green_dye"]},
         "orange": {"rgb": (230, 128, 44), "dyes": ["minecraft:orange_dye"]},
         "red": {"rgb": (170, 36, 40), "dyes": ["minecraft:red_dye"]}}
# Flame colours (RGB) and what turns a fitting's flames to them (ordinary is the default).
FLAMES = {"ordinary": {"rgb": (255, 196, 96), "catalysts": []},
          "soul": {"rgb": (96, 226, 255), "catalysts": ["minecraft:soul_sand", "minecraft:soul_soil"]},
          "witchfire": {"rgb": (204, 112, 255), "catalysts": ["minecraft:amethyst_shard"]},
          "ghostfire": {"rgb": (120, 255, 150), "catalysts": ["minecraft:glow_ink_sac"]}}
DRIPS = {"drip_stages": 3, "drip_chance": 18}

BROOM = {"block": "enchanted_broom", "display": "Enchanted Broom", "range": 4, "sweep_ticks": 20, "max_moves": 16,
         "charge_ticks": 72000, "pan_reach": 1.25, "push_speed": 0.22}
DUSTPAN = {"block": "dustpan", "display": "Dustpan", "slots": 9}
BROOM_RACK = {"block": "broom_rack", "display": "Broom Rack", "pegs": 3}
# What a Broom Rack holds (item tag jugcraft:brooms).
BROOMS = ["jugcraft:witchs_broom", "jugcraft:enchanted_broom", "jugcraft:flying_broomstick"]
# What floats in the Horned Skull Cauldron (item tag jugcraft:cauldron_floaters): a witch's ingredients, nothing that
# would be lost by mistake (no tools, weapons or containers).
FLOATERS = ["minecraft:apple", "minecraft:bone", "minecraft:spider_eye", "minecraft:fermented_spider_eye", "minecraft:rotten_flesh",
            "minecraft:red_mushroom", "minecraft:brown_mushroom", "minecraft:carrot", "minecraft:potato", "minecraft:poisonous_potato",
            "minecraft:beetroot", "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:egg", "minecraft:feather",
            "minecraft:slime_ball", "minecraft:magma_cream", "minecraft:ghast_tear", "minecraft:nether_wart", "minecraft:pufferfish",
            "minecraft:rabbit_foot", "minecraft:phantom_membrane", "minecraft:amethyst_shard", "minecraft:wither_rose",
            "jugcraft:mandrake_root", "jugcraft:garlic", "jugcraft:acorn", "jugcraft:chestnut", "jugcraft:cranberries"]
MESSAGES = {"message.jugcraft.enchanted_broom.anointed": "The broom stirs and twitches its bristles: it will sweep for three days.",
            "message.jugcraft.enchanted_broom.full": "The broom is still wide awake.",
            "message.jugcraft.beating_heart_jar.tempo": "%s beats a minute",
            "message.jugcraft.giant_beating_heart.tempo": "The giant's heart: %s beats a minute",
            "container.jugcraft.dustpan": "Dustpan"}

CABINET = {"block": "curiosity_cabinet", "display": "Curiosity Cabinet", "slots": 9, "door_ticks": 40}
BELL_JAR = {"block": "bell_jar", "display": "Bell Jar", "turn_ticks": 240}
MOTH_CASE = {"block": "moth_display_case", "display": "Moth Display Case", "moths": ["luna", "deaths_head", "atlas"]}

# The oddity jars' shape, in pixels: each part's inset from the block's side (base, glass, lid), the glass's and lid's
# tops, the knob's half-width and top, and the label's box (x0, y0, x1, y1) on the front. The bat's jar is bigger
# (JARS bat_in_a_jar "jar").
ODDITY_JAR = {"base": 4.0, "glass": 4.3, "lid": 3.9, "glass_top": 11.6, "lid_top": 12.8, "knob": 1.0, "knob_top": 13.6,
              "label": (5.2, 2.6, 10.8, 6.0)}
JARS = {
    "jar_of_eyeballs": {"display": "Jar of Eyeballs", "range": 8},
    # The heart: where the client draws its middle (`heart`, pixels in the jar) and how much more it swells at the peak
    # of a beat (`swell`); the audit checks that at its fullest, turned any way, it stays inside the glass.
    "beating_heart_jar": {"display": "Beating Heart Jar", "tempos": [60, 80, 100, 120], "pulse_ticks": 2, "heart": (8.0, 6.4, 8.0),
                          "swell": 0.14},
    # The bat's jar is wider and taller than the others (5 October 2026: its wings poked through the glass), and the bat
    # is drawn at `scale`: awake it circles `orbit` pixels round the middle at `fly_y` (bobbing `bob` up and down), asleep
    # it hangs at `hang_y`. The audit checks that its wings stay `margin` inside the glass at every beat.
    "bat_in_a_jar": {"display": "Bat in a Jar", "range": 3, "flutter_ticks": 100,
                     "jar": {"base": 1.9, "glass": 2.2, "lid": 1.8, "glass_top": 13.4, "lid_top": 14.6, "knob": 1.2, "knob_top": 15.4,
                             "label": (4.4, 3.0, 11.6, 7.2)},
                     "scale": 0.9, "orbit": 0.2, "fly_y": 7.2, "bob": 1.0, "hang_y": 11.2, "margin": 0.25},
    "two_headed_snake_jar": {"display": "Two-Headed Snake Jar"},
    "hand_in_a_jar": {"display": "Hand in a Jar", "range": 8},
}

# The bigger jars (5 October 2026, after the owner's "the beating heart is awesome, make a version that is like a giant's
# beating heart, 3x3x3" and "make bigger versions of the specimen jar"): props of several blocks, placed and broken as one
# (agriculture/MultiDecorationBlock: from the block aimed at, to the placer's right, up and away). Each is a small jar
# made bigger and nothing else (6 October 2026: the owner found a vat of the giant's own design "so so ugly", and wanted
# "the heart in the jar but just a bigger version in the exact same style of jar"): every box of the small jar's model,
# and what floats in it, `scale` times bigger about the middle of the frame's floor, drawing on the small jar's own
# textures. Each is modelled whole in its own frame (x across to the placer's left, y up, z away from the placer, in
# pixels) and cut into one model per block; what moves in it is drawn by the client from its first block, just as the
# small jar's renderer draws it, scaled. `size` is in blocks (across, up, deep).
# The Giant's Beating Heart: the Beating Heart Jar three times over. It is a slow redstone clock like the jar: it beats
# at one of `tempos` beats a minute (use it to change), each beat a `pulse_ticks` signal of 15 from its first block to
# every side but below, and a signal into any of its bottom blocks from below stops it. Its `size` is its scale. In a slot
# it shows the Beating Heart Jar with its heart, `icon` times as big as the jar's item.
GIANT_HEART = {"block": "giant_beating_heart", "display": "Giant's Beating Heart", "size": 3, "tempos": [40, 50, 60, 72],
               "pulse_ticks": 2, "icon": 1.05}
# The Tall Specimen Jar (the Specimen Jar twice over, one block across and two tall) and the Specimen Tank (three times
# over, three blocks every way), each holding one of the jar's specimens (sneak-use for the next, kept when broken),
# bobbing and turning slowly among rising bubbles; `light` on every block. In a slot each shows the Specimen Jar's own
# item (with the specimen in it) `icon` times as big as the Specimen Jar's.
SPECIMEN_VESSELS = {
    "tall_specimen_jar": {"display": "Tall Specimen Jar", "size": (1, 2, 1), "scale": 2, "light": 8, "icon": 1.25},
    "specimen_tank": {"display": "Specimen Tank", "size": (3, 3, 3), "scale": 3, "light": 10, "icon": 1.45},
}

SHAPED = [
    {"id": CAULDRON["block"], "pattern": ["HBH", "ICI"], "key": {"H": "minecraft:goat_horn", "B": "minecraft:bone_block",
                                                                 "I": "minecraft:iron_ingot", "C": "minecraft:cauldron"},
     "result": CAULDRON["block"], "count": 1, "category": "building"},
    {"id": "horned_skull_cauldron_from_bones", "pattern": ["NBN", "ICI"], "key": {"N": "minecraft:bone", "B": "minecraft:bone_block",
                                                                                   "I": "minecraft:iron_ingot", "C": "minecraft:cauldron"},
     "result": CAULDRON["block"], "count": 1, "category": "building"},
    {"id": EMBER_BED["block"], "pattern": [" S ", "SCS", " S "], "key": {"S": "minecraft:cobblestone", "C": "#minecraft:coals"},
     "result": EMBER_BED["block"], "count": 2, "category": "building"},
    {"id": LADLE["item"], "pattern": ["B", "S", "S"], "key": {"B": "minecraft:bowl", "S": "minecraft:stick"},
     "result": LADLE["item"], "count": 1, "category": "equipment"},
    {"id": "floor_candelabrum", "pattern": ["CCC", "NIN", " I "], "key": {"C": "#minecraft:candles", "N": "minecraft:iron_nugget",
                                                                         "I": "minecraft:iron_ingot"},
     "result": "floor_candelabrum", "count": 1, "category": "building"},
    {"id": "table_candelabrum", "pattern": ["CCC", " N ", " I "], "key": {"C": "#minecraft:candles", "N": "minecraft:iron_nugget",
                                                                         "I": "minecraft:iron_ingot"},
     "result": "table_candelabrum", "count": 1, "category": "building"},
    {"id": "wall_girandole", "pattern": ["CCC", "NGN"], "key": {"C": "#minecraft:candles", "N": "minecraft:iron_nugget",
                                                               "G": "minecraft:tinted_glass"},
     "result": "wall_girandole", "count": 1, "category": "building"},
    {"id": "branching_chandelier", "pattern": ["NIN", "CIC", "CCC"], "key": {"C": "#minecraft:candles", "N": "minecraft:iron_nugget",
                                                                            "I": "minecraft:iron_ingot"},
     "result": "branching_chandelier", "count": 1, "category": "building"},
    {"id": BROOM_RACK["block"], "pattern": ["NNN", "PPP"], "key": {"N": "minecraft:iron_nugget", "P": "#minecraft:planks"},
     "result": BROOM_RACK["block"], "count": 1, "category": "building"},
    {"id": CABINET["block"], "pattern": ["PNP", "PGP", "PGP"], "key": {"P": "minecraft:dark_oak_planks", "N": "minecraft:gold_nugget",
                                                                       "G": "minecraft:glass_pane"},
     "result": CABINET["block"], "count": 1, "category": "building"},
    {"id": BELL_JAR["block"], "pattern": [" G ", "G G", " S "], "key": {"G": "minecraft:glass", "S": "#minecraft:wooden_slabs"},
     "result": BELL_JAR["block"], "count": 1, "category": "building"},
    {"id": "jar_of_eyeballs", "pattern": ["EEE", "ENE", "EGE"], "key": {"E": "minecraft:spider_eye", "N": "minecraft:iron_nugget",
                                                                        "G": "minecraft:glass"},
     "result": "jar_of_eyeballs", "count": 1, "category": "building"},
]
SHAPED += [
    {"id": GIANT_HEART["block"], "pattern": ["GGG", "GHG", "IRI"], "key": {"G": "minecraft:glass", "H": "jugcraft:beating_heart_jar",
                                                                        "I": "jugcraft:brass_ingot", "R": "minecraft:redstone_block"},
     "result": GIANT_HEART["block"], "count": 1, "category": "redstone"},
    {"id": "tall_specimen_jar", "pattern": ["N", "J", "G"], "key": {"N": "minecraft:iron_ingot", "J": "jugcraft:specimen_jar",
                                                                    "G": "minecraft:glass"},
     "result": "tall_specimen_jar", "count": 1, "category": "building"},
    {"id": "specimen_tank", "pattern": ["IGI", "GJG", "IGI"], "key": {"I": "minecraft:iron_ingot", "G": "minecraft:glass",
                                                                      "J": "jugcraft:tall_specimen_jar"},
     "result": "specimen_tank", "count": 1, "category": "building"},
]
SHAPELESS = [
    {"id": BROOM["block"], "inputs": ["jugcraft:witchs_broom", "minecraft:amethyst_shard", "minecraft:string"],
     "result": BROOM["block"], "count": 1, "category": "building"},
    {"id": DUSTPAN["block"], "inputs": ["minecraft:iron_ingot", "minecraft:copper_ingot"], "result": DUSTPAN["block"], "count": 1,
     "category": "building"},
    {"id": MOTH_CASE["block"], "inputs": ["minecraft:item_frame", "minecraft:glass_pane", "minecraft:green_dye"],
     "result": MOTH_CASE["block"], "count": 1, "category": "building"},
    {"id": "beating_heart_jar", "inputs": ["minecraft:glass", "minecraft:iron_nugget", "minecraft:rotten_flesh", "minecraft:redstone",
                                           "jugcraft:brass_nugget"], "result": "beating_heart_jar", "count": 1, "category": "redstone"},
    {"id": "bat_in_a_jar", "inputs": ["minecraft:glass", "minecraft:iron_nugget", "minecraft:phantom_membrane", "minecraft:leather"],
     "result": "bat_in_a_jar", "count": 1, "category": "building"},
    {"id": "two_headed_snake_jar", "inputs": ["minecraft:glass", "minecraft:iron_nugget", "minecraft:string", "minecraft:slime_ball"],
     "result": "two_headed_snake_jar", "count": 1, "category": "building"},
    {"id": "hand_in_a_jar", "inputs": ["minecraft:glass", "minecraft:iron_nugget", "minecraft:rotten_flesh", "minecraft:bone"],
     "result": "hand_in_a_jar", "count": 1, "category": "building"},
]


def blocks():
    return ([CAULDRON["block"], EMBER_BED["block"]] + list(CANDELABRA) + [BROOM["block"], DUSTPAN["block"], BROOM_RACK["block"],
            CABINET["block"], BELL_JAR["block"], MOTH_CASE["block"]] + list(JARS) + big_jars())


def big_jars():
    """The props of several blocks: the Giant's Beating Heart and the two bigger specimen jars."""
    return [GIANT_HEART["block"]] + list(SPECIMEN_VESSELS)


def items():
    return blocks() + [LADLE["item"]]


def names():
    out = {CAULDRON["block"]: CAULDRON["display"], EMBER_BED["block"]: EMBER_BED["display"], BROOM["block"]: BROOM["display"],
           DUSTPAN["block"]: DUSTPAN["display"], BROOM_RACK["block"]: BROOM_RACK["display"], CABINET["block"]: CABINET["display"],
           BELL_JAR["block"]: BELL_JAR["display"], MOTH_CASE["block"]: MOTH_CASE["display"]}
    out.update({kind: info["display"] for kind, info in CANDELABRA.items()})
    out.update({jar: info["display"] for jar, info in JARS.items()})
    out[GIANT_HEART["block"]] = GIANT_HEART["display"]
    out.update({vessel: info["display"] for vessel, info in SPECIMEN_VESSELS.items()})
    return out


def layout():
    """The candelabra's candles for Java (resource /jugcraft/candelabra.json): {fitting: [[x, y, z, height], ...]}."""
    return {kind: [list(c) for c in info["candles"]] for kind, info in CANDELABRA.items()}
