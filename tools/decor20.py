"""Halloween decorations batch 20: Pumpkin Night (docs/features/pumpkin-night.md), prop sets 16 to 20 of the Witching Season
plan (docs/features/witching-season.md), drawn from the owner's reference pictures. Models and textures:
tools/decor20_data.py, sculpted on the toolkit in tools/flora_art.py; the choir's voices: tools/choir_sounds.py.

16. Red Kuri and Kabocha pumpkins join the heirlooms (their crops, seeds, wild patches and trades are in
    tools/agriculture.py with the other heirlooms); each carves into its own hand-carved pumpkin that glows its own
    colour, and each cooks into a dish in the Cooking Pot.
17. The Farm Stand (agriculture/FarmStandBlock.java): an owner's stall of `crates` crates sold for Jugs (town/Jugs.java).
18. Two garlands hung between String Light Hooks like the Jack-o'-Lantern String Lights (StringLightHookBlockEntity.Strand).
19. The Harvest Effigy (agriculture/HarvestEffigyBlock.java): burnt on a fall night for Harvest Cheer, it leaves Effigy
    Ashes that give Hearth Ash.
20. The Singing Pumpkins (agriculture/SingingPumpkinBlock.java): four voices tuned like note blocks.
Every recipe follows the agriculture feature switch.
"""

FEATURE = "agriculture"

# 16. The new heirlooms. Their glow (client/CarvingTextures.Glow) is the colour their hand-carved pumpkins shine when lit.
HEIRLOOMS = {
    "red_kuri_pumpkin": {"display": "Red Kuri Pumpkin", "seeds": "red_kuri_pumpkin_seeds", "seeds_display": "Red Kuri Pumpkin Seeds",
                         "biomes": ["IS_TAIGA", "IS_SPOOKY"], "map_color": "COLOR_ORANGE", "throw": 0.99, "carved_throw": 1.05,
                         "glow": "deep"},
    "kabocha_pumpkin": {"display": "Kabocha Pumpkin", "seeds": "kabocha_pumpkin_seeds", "seeds_display": "Kabocha Pumpkin Seeds",
                        "biomes": ["IS_FOREST", "IS_JUNGLE"], "map_color": "COLOR_GREEN", "throw": 0.96, "carved_throw": 1.02,
                        "glow": "gold"},
}
DISHES = {
    "red_kuri_soup": {"display": "Red Kuri Soup", "food": [9, 0.7], "stew": True, "tags": ["c:foods/soup"],
                      "pot": {"minecraft:bowl": 1, "jugcraft:red_kuri_pumpkin": 1, "jugcraft:onion": 1, "jugcraft:garlic": 1}},
    "kabocha_tempura": {"display": "Kabocha Tempura", "food": [6, 0.8], "compost": "medium_high", "tags": ["c:foods"],
                        "pot": {"jugcraft:kabocha_pumpkin": 1, "minecraft:wheat": 1, "minecraft:egg": 1}, "count": 2},
}
POT_TIME = 200

# 17. The Farm Stand: two blocks wide; its owner stocks `crates` crates and chalks a price in Jugs on each, from 1 to the
# town's maximum balance, moving it by `price_steps`; anyone within `reach` blocks buys one item at a time, at most once
# every `rate_ticks` ticks, the Jugs going straight to the owner (offline or not).
FARM_STAND = {"block": "farm_stand", "display": "Farm Stand", "crates": 6, "reach": 6.0, "rate_ticks": 4,
              "price_steps": [1, 10, 100, 1000],
              # Where the client draws the goods and chalk (client/FarmStandRenderer.java), in pixels for a stand facing
              # north, x from the seam between its blocks: each crate's middle (x, floor, z), each row's price tags
              # (height, face's distance from the north side), and the slate header board over the awning's front (x, y, face,
              # width).
              "crate_middles": [[10.0, 8.5, 4.5], [0.0, 8.5, 4.5], [-10.0, 8.5, 4.5], [10.0, 13.5, 11.5], [0.0, 13.5, 11.5],
                                [-10.0, 13.5, 11.5]],
              "tags": [[6.4, 0.4], [14.8, 7.4]], "board": [0.0, 26.8, 0.2, 12.0]}

# 18. The garlands: strand kinds hung between String Light Hooks, up to the string lights' length; their bulbs light with
# the hook.
GARLANDS = {"pumpkin_vine": {"item": "pumpkin_vine_garland", "display": "Pumpkin Vine Garland"},
            "autumn_leaves": {"item": "autumn_leaf_garland", "display": "Autumn Leaf Garland"}}

# 19. The Harvest Effigy: three blocks tall. Lit at night it burns for `burn_ticks`; every player within `cheer_radius`
# gets Regeneration I for `regeneration_ticks` and Luck for `luck_ticks`, once per player per night; crows within
# `crow_radius` scatter; rain puts it out. It leaves Effigy Ashes, which give `ash_yield` Hearth Ash (a weak bone meal:
# `radius`, `doses`).
EFFIGY = {"block": "harvest_effigy", "display": "Harvest Effigy", "burn_ticks": 600, "cheer_radius": 16,
          "regeneration_ticks": 600, "luck_ticks": 6000, "crow_radius": 24, "light": 15, "check_ticks": 20,
          # Where the client draws his head and his hands' flames (client/HarvestEffigyRenderer.java), in pixels up from
          # his feet and out from his middle: the top of his neck, the head's size, his hands.
          "neck_top": 36.0, "head": 13.0, "hand_x": 13.0, "hand_y": 31.0}
ASHES = {"block": "effigy_ashes", "display": "Effigy Ashes", "ash_yield": 2}
HEARTH_ASH = {"item": "hearth_ash", "display": "Hearth Ash", "radius": 1, "doses": 1}

# 20. The Singing Pumpkins: tuned by use across `notes` notes (two octaves, as a note block), each voice's middle note
# (note 12, pitch 1) given; a redstone pulse sings. The mouth opens for `open_ticks`. Each voice's pumpkin is `shape`
# (width, height in pixels) and its face is at `face` (client/SingingPumpkinRenderer.FACES: the front's distance from
# the north side, the eyes' height and distance either side, an eye's size, the mouth's height and width, and its
# height open).
VOICES = {
    "bass": {"block": "singing_pumpkin_bass", "display": "Singing Pumpkin (Bass)", "middle": "F#3", "hz": 185.0,
             "formants": [[600, 1040, 2250], [400, 750, 2400]], "brightness": 0.55,
             "shape": [15.0, 12.0], "face": [0.5, 7.5, 3.5, 3.5, 3.5, 7.0, 4.5]},
    "tenor": {"block": "singing_pumpkin_tenor", "display": "Singing Pumpkin (Tenor)", "middle": "F#4", "hz": 370.0,
              "formants": [[650, 1080, 2650], [400, 800, 2600]], "brightness": 0.65,
              "shape": [13.0, 13.0], "face": [1.5, 8.5, 3.0, 3.0, 4.5, 5.5, 4.0]},
    "alto": {"block": "singing_pumpkin_alto", "display": "Singing Pumpkin (Alto)", "middle": "F#4", "hz": 370.0,
             "formants": [[800, 1150, 2800], [450, 800, 2830]], "brightness": 0.75,
             "shape": [12.0, 14.0], "face": [2.0, 9.5, 2.8, 2.8, 5.0, 5.0, 4.0]},
    "soprano": {"block": "singing_pumpkin_soprano", "display": "Singing Pumpkin (Soprano)", "middle": "F#5", "hz": 740.0,
                "formants": [[800, 1150, 2900], [350, 900, 2700]], "brightness": 0.9,
                "shape": [10.0, 11.0], "face": [3.0, 7.0, 2.3, 2.4, 3.6, 4.0, 3.4]},
}
CHOIR = {"notes": 25, "open_ticks": 24, "light": 12}

MESSAGES = {
    "container.jugcraft.farm_stand": "Farm Stand",
    "message.jugcraft.farm_stand.empty": "That crate is empty",
    "message.jugcraft.farm_stand.poor": "You need %s Jugs",
    "message.jugcraft.farm_stand.owner_full": "%s can't hold any more Jugs",
    "message.jugcraft.farm_stand.bought": "Bought %s for %s Jugs",
    "message.jugcraft.farm_stand.own": "It's your own stand",
    "message.jugcraft.farm_stand.locked": "Only %s can take this stand down",
    "gui.jugcraft.farm_stand.balance": "Your Jugs: %s",
    "gui.jugcraft.farm_stand.owner": "%s's stand",
    "gui.jugcraft.farm_stand.buy": "Buy",
    "gui.jugcraft.farm_stand.pick": "Price",
    "gui.jugcraft.farm_stand.pricing": "Crate %s: %s Jugs",
    "gui.jugcraft.farm_stand.hint": "One at a time; the Jugs go to the owner",
    "message.jugcraft.effigy.day": "It will only catch at night",
    "message.jugcraft.effigy.wet": "It's too wet to burn",
    "message.jugcraft.effigy.cheer": "The blaze warms you: Harvest Cheer!",
    "message.jugcraft.singing_pumpkin": "Note %s",
    "subtitles.jugcraft.singing_pumpkin.bass": "Pumpkin sings low",
    "subtitles.jugcraft.singing_pumpkin.tenor": "Pumpkin sings",
    "subtitles.jugcraft.singing_pumpkin.alto": "Pumpkin sings",
    "subtitles.jugcraft.singing_pumpkin.soprano": "Pumpkin sings high",
    "subtitles.jugcraft.effigy.burn": "Effigy roars in flame",
}


def blocks():
    return [FARM_STAND["block"], EFFIGY["block"], ASHES["block"]] + [v["block"] for v in VOICES.values()]


def items():
    return blocks() + [g["item"] for g in GARLANDS.values()] + [HEARTH_ASH["item"]]


def names():
    out = {FARM_STAND["block"]: FARM_STAND["display"], EFFIGY["block"]: EFFIGY["display"], ASHES["block"]: ASHES["display"]}
    out.update({v["block"]: v["display"] for v in VOICES.values()})
    return out


def item_names():
    out = {g["item"]: g["display"] for g in GARLANDS.values()}
    out[HEARTH_ASH["item"]] = HEARTH_ASH["display"]
    return out


SHAPED = [
    {"id": FARM_STAND["block"], "pattern": ["OWO", "PCP", "PHP"], "key": {"O": "minecraft:orange_wool", "W": "minecraft:white_wool",
                                                                         "P": "#minecraft:planks", "C": "minecraft:chest",
                                                                         "H": "minecraft:hay_block"},
     "result": FARM_STAND["block"], "count": 1, "category": "building"},
    {"id": EFFIGY["block"], "pattern": ["THT", "SHS", " H "], "key": {"T": "minecraft:string", "H": "minecraft:hay_block",
                                                                     "S": "minecraft:stick"},
     "result": EFFIGY["block"], "count": 1, "category": "building"},
]
SHAPELESS = [
    {"id": GARLANDS["pumpkin_vine"]["item"], "inputs": ["minecraft:vine", "minecraft:pumpkin", "minecraft:string"],
     "result": GARLANDS["pumpkin_vine"]["item"], "count": 1, "category": "building"},
    {"id": GARLANDS["autumn_leaves"]["item"], "inputs": ["jugcraft:maple_leaves", "jugcraft:aspen_leaves", "jugcraft:chestnut_leaves",
                                                          "minecraft:string"],
     "result": GARLANDS["autumn_leaves"]["item"], "count": 1, "category": "building"},
    {"id": VOICES["bass"]["block"], "inputs": ["minecraft:jack_o_lantern", "minecraft:note_block", "#minecraft:planks"],
     "result": VOICES["bass"]["block"], "count": 1, "category": "redstone"},
    {"id": VOICES["tenor"]["block"], "inputs": ["minecraft:jack_o_lantern", "minecraft:note_block", "minecraft:clay_ball"],
     "result": VOICES["tenor"]["block"], "count": 1, "category": "redstone"},
    {"id": VOICES["alto"]["block"], "inputs": ["minecraft:jack_o_lantern", "minecraft:note_block", "minecraft:gold_nugget"],
     "result": VOICES["alto"]["block"], "count": 1, "category": "redstone"},
    {"id": VOICES["soprano"]["block"], "inputs": ["minecraft:jack_o_lantern", "minecraft:note_block", "minecraft:amethyst_shard"],
     "result": VOICES["soprano"]["block"], "count": 1, "category": "redstone"},
]
