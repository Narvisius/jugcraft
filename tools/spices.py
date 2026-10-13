"""Garden crops, herbs and spices, part b (slice 7 of the kitchen and cooking expansion, docs/branches/AGRICULTURE.md; the
record: docs/features/vegetables-herbs-and-spices.md): the spices, drawn in Jugcraft's own style. The numbers here are what
agriculture/TallCrop, CinnamonLogBlock, SpiceRackBlockEntity and JugcraftAgriculture use; tools/check_mod_data.py compares
them. tools/spice_data.py writes the JSON; tools/spice_textures.py draws them.

- **Black pepper and vanilla** climb a trellis, as the tomato and cucumber do (tools/agriculture.py TALL_CROPS): the pepper
  vine hangs clusters of peppercorns, green and then red, the vanilla vine flowers pale yellow and hangs long pods. Each is
  planted from what it gives: peppercorns, vanilla pods.
- **Ginger, mustard and saffron** are low crops (tools/agriculture.py CROPS): ginger planted from its root, mustard from its
  seeds (and flowering yellow), saffron from a Crocus Bulb, its purple crocus flowering with the red threads that are
  saffron.
- **Cinnamon** is the bark of the Cinnamon Tree: grown from a sapling, found wild in jungles; an axe stripping one of its
  logs peels off `bark` Cinnamon, as cinnamon is harvested (CINNAMON).
- **Paprika** is ground from the Kitchen Garden's chili pepper: dried in a smoker, furnace or on a campfire, then ground with
  the Mortar and Pestle the Concordance's alchemists already use (concordance/MortarItem): used with a Dried Chili in the
  other hand, it grinds it into `count` Paprika (GRIND). No second mortar is added.
- **Spice Rack:** a wall rack of two shelves, `slots` spices in all (item tag jugcraft:spices): used holding a spice it takes
  one; with an empty hand it gives the last back. A comparator reads how full it is.
- **Dishes:** Gingerbread Cookies, Chicken Curry, Saffron Rice and Vanilla Custard; cinnamon and ginger make Mulling Spices
  too.

Balance (docs/BALANCE.md, tools/menu.py COOK_BONUS): spices are not eaten alone; a dish gives at most COOK_BONUS more than its
ingredients (tools/check_mod_data.py checks), cookies four to a batch as the menu's.
"""
from vegetables import vine_textures

CROPS = {
    "ginger": {"block": "ginger_crop", "display": "Ginger", "seed": "ginger", "produce": "ginger",
               "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root"},
    "mustard": {"block": "mustard_crop", "display": "Mustard", "seed": "mustard_seeds", "produce": "mustard_seeds",
                "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root"},
    "saffron": {"block": "saffron_crop", "display": "Saffron Crocus", "seed": "crocus_bulb", "produce": "saffron",
                "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "grain"},
}
TALL_CROPS = {
    "peppercorn": {"block": "peppercorn_crop", "display": "Black Pepper Vine", "seed": "peppercorns",
                   "heights": [1, 1, 1, 2, 2, 2, 2, 2], "pick": {"item": "peppercorns", "min": 2, "max": 4}, "pick_reset": 5,
                   "growth_time": 1.5, "trellis": True, "textures": vine_textures("peppercorn")},
    "vanilla": {"block": "vanilla_crop", "display": "Vanilla Vine", "seed": "vanilla_pods",
                "heights": [1, 1, 1, 2, 2, 2, 2, 2], "pick": {"item": "vanilla_pods", "min": 1, "max": 2}, "pick_reset": 5,
                "growth_time": 1.5, "trellis": True, "textures": vine_textures("vanilla")},
}
WILD = {
    "wild_peppercorns": {"display": "Wild Black Pepper", "crop": "peppercorn", "texture": "wild_peppercorns", "biomes": ["IS_JUNGLE"]},
    "wild_vanilla": {"display": "Wild Vanilla", "crop": "vanilla", "texture": "wild_vanilla", "biomes": ["IS_JUNGLE"]},
    "wild_ginger": {"display": "Wild Ginger", "crop": "ginger", "texture": "ginger_stage3", "biomes": ["IS_JUNGLE", "IS_SWAMP"]},
    "wild_mustard": {"display": "Wild Mustard", "crop": "mustard", "texture": "mustard_stage3", "biomes": ["IS_PLAINS", "IS_FLORAL"]},
    "wild_saffron": {"display": "Wild Saffron Crocus", "crop": "saffron", "texture": "saffron_stage3", "biomes": ["IS_SAVANNA", "IS_HILL"]},
}

# The Cinnamon Tree, an evergreen laurel: a straight trunk `trunk` tall plus up to `trunk_extra` of cinnamon logs under a
# crown of its leaves (a blob `radius` across and `height` deep); its leaves drop its sapling as spruce leaves do theirs
# (tools/agriculture.py SAPLING_CHANCES), and sticks. Stripping a log with an axe peels `bark` Cinnamon from it (one to two),
# as cinnamon is the inner bark. It has its own log and stripped log, as the banana its stem, but no wood set: either log
# saws into `planks` jungle planks at a crafting table, the jungle wood it is grown among. It grows wild in jungles, one tree
# in `rarity` chunks.
CINNAMON = {"tree": "cinnamon", "sapling": "cinnamon_sapling", "leaves": "cinnamon_leaves", "log": "cinnamon_log",
            "stripped": "stripped_cinnamon_log", "bark_item": "cinnamon", "bark": [1, 2], "trunk": 4, "trunk_extra": 2,
            "radius": 2, "height": 3, "planks": 4, "biomes": ["IS_JUNGLE"], "rarity": 16,
            "displays": {"cinnamon_sapling": "Cinnamon Sapling", "cinnamon_leaves": "Cinnamon Leaves", "cinnamon_log": "Cinnamon Log",
                         "stripped_cinnamon_log": "Stripped Cinnamon Log"}}

RACK = {"block": "spice_rack", "display": "Spice Rack", "slots": 8, "tag": "jugcraft:spices"}
# Grinding with the Concordance's Mortar and Pestle (jugcraft:mortar): the dried chili in the other hand becomes paprika.
GRIND = {"tool": "mortar", "input": "dried_chili", "result": "paprika", "count": 2}

ITEMS = {
    "peppercorns": {"display": "Peppercorns", "plants": "peppercorn_crop", "trellis_seed": True, "compost": "low",
                    "tags": ["c:crops/black_pepper", "jugcraft:spices"]},
    "vanilla_pods": {"display": "Vanilla Pods", "plants": "vanilla_crop", "trellis_seed": True, "compost": "low",
                     "tags": ["c:crops/vanilla", "jugcraft:spices"]},
    "ginger": {"display": "Ginger", "plants": "ginger_crop", "compost": "medium", "tags": ["c:crops/ginger", "jugcraft:spices"]},
    "mustard_seeds": {"display": "Mustard Seeds", "plants": "mustard_crop", "compost": "low",
                      "tags": ["c:seeds/mustard", "c:crops/mustard", "jugcraft:spices", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "crocus_bulb": {"display": "Crocus Bulb", "plants": "saffron_crop", "compost": "medium", "tags": ["c:seeds/saffron"]},
    "saffron": {"display": "Saffron", "compost": "low", "tags": ["c:crops/saffron", "jugcraft:spices"]},
    "cinnamon": {"display": "Cinnamon", "compost": "low", "tags": ["jugcraft:spices"]},
    "dried_chili": {"display": "Dried Chili", "compost": "medium", "tags": []},
    "paprika": {"display": "Paprika", "compost": "low", "tags": ["jugcraft:spices"]},
    "gingerbread_cookie": {"display": "Gingerbread Cookie", "food": [2, 0.1], "compost": "medium_high", "tags": ["c:foods/cookie"]},
    "chicken_curry": {"display": "Chicken Curry", "food": [10, 0.8], "stew": True, "tags": ["c:foods"]},
    "saffron_rice": {"display": "Saffron Rice", "food": [6, 0.7], "stew": True, "tags": ["c:foods"]},
    "vanilla_custard": {"display": "Vanilla Custard", "food": [5, 0.6], "stew": True, "tags": ["c:foods"]},
}
GRASS_SEEDS = []
# Paprika: a chili pepper dried (furnace, smoker, campfire), then ground with the Mortar and Pestle, which is kept.
COOKING = {"dried_chili": {"input": "pepper", "xp": 0.1, "category": "misc"}}
SHAPED = [
    {"id": RACK["block"], "pattern": ["TTT", "SSS", "TTT"], "key": {"T": "minecraft:stick", "S": "#minecraft:wooden_slabs"},
     "result": RACK["block"], "count": 1, "category": "building"},
]
SHAPELESS = [
    {"id": "gingerbread_cookie", "inputs": ["minecraft:wheat", "minecraft:wheat", "jugcraft:ginger", "minecraft:sugar",
                                            "jugcraft:cinnamon"], "result": "gingerbread_cookie", "count": 4},
    {"id": "mulling_spices_from_cinnamon", "inputs": ["minecraft:sugar", "jugcraft:cinnamon", "jugcraft:ginger"],
     "result": "mulling_spices", "count": 1, "category": "misc"},
]
# Either cinnamon log into jungle planks (CINNAMON "planks"), as a log of its own wood would give its planks.
SHAPELESS += [{"id": f"jungle_planks_from_{log}", "inputs": [f"jugcraft:{log}"], "result": "minecraft:jungle_planks",
               "count": CINNAMON["planks"], "category": "building"} for log in (CINNAMON["log"], CINNAMON["stripped"])]
POT_RECIPES = {
    "chicken_curry": {"inputs": {"minecraft:bowl": 1, "jugcraft:rice": 1, "minecraft:chicken": 1, "jugcraft:ginger": 1,
                                 "jugcraft:paprika": 1, "jugcraft:onion": 1}, "time": 300},
    "saffron_rice": {"inputs": {"minecraft:bowl": 1, "jugcraft:rice": 2, "jugcraft:saffron": 1, "jugcraft:onion": 1}, "time": 200},
    "vanilla_custard": {"inputs": {"minecraft:bowl": 1, "minecraft:egg": 1, "minecraft:sugar": 1, "jugcraft:vanilla_pods": 1,
                                   "jugcraft:milk_bottle": 1}, "time": 200},
}


def blocks():
    """The cinnamon tree's blocks and the spice rack (the spice crops and their wild plants are among the crops and wild plants)."""
    return [CINNAMON["sapling"], CINNAMON["leaves"], CINNAMON["log"], CINNAMON["stripped"], RACK["block"]]


def items():
    return [CINNAMON["sapling"], CINNAMON["leaves"], CINNAMON["log"], CINNAMON["stripped"], RACK["block"]]


def textures():
    """The cinnamon tree's and the spice rack's block textures (the spice crops' stages are the crops')."""
    return [CINNAMON["sapling"], CINNAMON["leaves"], "cinnamon_log", "cinnamon_log_top", "stripped_cinnamon_log",
            "stripped_cinnamon_log_top", "spice_rack"]
