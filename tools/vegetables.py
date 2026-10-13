"""Garden crops, herbs and spices, part b (slice 7 of the kitchen and cooking expansion, docs/branches/AGRICULTURE.md; the
record: docs/features/vegetables-herbs-and-spices.md): the vegetables. On 9 October 2026 the owner chose the recommendations
("go with the recommendations"): new salad and summer vegetables drawn in Jugcraft's own style, their library having none of
them. Every texture is drawn by tools/vegetable_textures.py.

- **Lettuce, Spinach, Radishes and Peas:** low crops on farmland (tools/agriculture.py CROPS), eight ages and four stages
  each, as the onion and cabbage. Lettuce and spinach grow from their seeds and give their leaves and seeds; radishes and
  peas are planted from themselves, as the onion and the beans are. Peas fix nitrogen, as the beans do, so the crops round
  them grow faster.
- **Cucumbers** climb a trellis, as the tomato does; **eggplants** and **zucchini** are bushes a block tall, as the pepper
  is (tools/agriculture.py TALL_CROPS, agriculture/TallCrop). Ripe, a right-click picks them and they fruit again.
- Every vegetable is found wild (WILD) and its seeds come from breaking short grass (GRASS_SEEDS).
- **Dishes:** a Green Salad, Pea Soup, Grilled Zucchini and Roasted Eggplant; and the menu's Ratatouille is now cooked from
  what a ratatouille is made of, eggplant and zucchini among it (tools/menu.py POT_RECIPES). The cucumbers are pickled with
  dill and mustard seed (tools/herbs.py).

Balance (docs/BALANCE.md, tools/menu.py COOK_BONUS): a raw vegetable is worth what vanilla's are (a beetroot's 1 to a
carrot's 3); a dish gives at most COOK_BONUS more than its ingredients, and tools/check_mod_data.py checks every one.
"""

# Low crops (tools/agriculture.py CROPS): four stages over eight ages, as the onion's. "loot": "grain" gives the produce and
# seeds when ripe; "root" gives the produce, which is also what plants it.
CROPS = {
    "lettuce": {"block": "lettuce_crop", "display": "Lettuce", "seed": "lettuce_seeds", "produce": "lettuce",
                "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "grain"},
    "spinach": {"block": "spinach_crop", "display": "Spinach", "seed": "spinach_seeds", "produce": "spinach",
                "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "grain"},
    "radish": {"block": "radish_crop", "display": "Radishes", "seed": "radish", "produce": "radish",
               "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root"},
    "peas": {"block": "pea_crop", "display": "Peas", "seed": "peas", "produce": "peas",
             "legume": True, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root"},
}

# The cucumber on its trellis and the two bushes (tools/agriculture.py TALL_CROPS, in the TallCrop enum's order after the
# fruit crops'). The cucumber is a block tall for its first three ages and then two, the vine in the lower trellis and its
# top in the upper; the bushes are a block tall, six stages over eight ages, as the fruit bushes.
STAGES = 6
VINE_STAGES = 5


def stage_texture(crop, stage):
    return f"{crop}_stage{stage}"


def vine_textures(crop):
    """A climbing crop's textures by age: three one-block stages, then the vine and its top for each of the five two-block ages
    (young, leafy, flowering, fruit setting, ripe)."""
    return ([[stage_texture(crop, s)] for s in range(3)]
            + [[f"{crop}_vine_stage{s}", f"{crop}_top_stage{s}"] for s in range(VINE_STAGES)])


TALL_CROPS = {
    "cucumber": {"block": "cucumber_crop", "display": "Cucumber Vine", "seed": "cucumber_seeds",
                 "heights": [1, 1, 1, 2, 2, 2, 2, 2], "pick": {"item": "cucumber", "min": 2, "max": 4}, "pick_reset": 5,
                 "growth_time": 1.25, "trellis": True, "textures": vine_textures("cucumber")},
    "eggplant": {"block": "eggplant_crop", "display": "Eggplant", "seed": "eggplant_seeds",
                 "heights": [1, 1, 1, 1, 1, 1, 1, 1], "pick": {"item": "eggplant", "min": 1, "max": 3}, "pick_reset": 5,
                 "growth_time": 1.25, "textures": [[stage_texture("eggplant", s)] for s in (0, 0, 1, 1, 2, 3, 4, 5)]},
    "zucchini": {"block": "zucchini_crop", "display": "Zucchini", "seed": "zucchini_seeds",
                 "heights": [1, 1, 1, 1, 1, 1, 1, 1], "pick": {"item": "zucchini", "min": 1, "max": 3}, "pick_reset": 5,
                 "growth_time": 1.25, "textures": [[stage_texture("zucchini", s)] for s in (0, 0, 1, 1, 2, 3, 4, 5)]},
}

# Wild plants (tools/agriculture.py WILD_CROPS): found in patches on grass in these biomes (ConventionalBiomeTags), wearing
# their ripe stage (the cucumber a wild vine sprawled on the ground, drawn for it); broken, they give 1-2 seeds.
WILD = {
    "wild_lettuce": {"display": "Wild Lettuce", "crop": "lettuce", "texture": "lettuce_stage3", "biomes": ["IS_PLAINS", "IS_FLORAL"]},
    "wild_spinach": {"display": "Wild Spinach", "crop": "spinach", "texture": "spinach_stage3", "biomes": ["IS_FOREST", "IS_PLAINS"]},
    "wild_radishes": {"display": "Wild Radishes", "crop": "radish", "texture": "radish_stage3", "biomes": ["IS_PLAINS", "IS_HILL"]},
    "wild_peas": {"display": "Wild Peas", "crop": "peas", "texture": "pea_stage3", "biomes": ["IS_HILL", "IS_FOREST"]},
    "wild_cucumbers": {"display": "Wild Cucumbers", "crop": "cucumber", "texture": "wild_cucumbers",
                       "biomes": ["IS_JUNGLE", "IS_SAVANNA"]},
    "wild_eggplant": {"display": "Wild Eggplant", "crop": "eggplant", "texture": stage_texture("eggplant", 5),
                      "biomes": ["IS_SAVANNA", "IS_JUNGLE"]},
    "wild_zucchini": {"display": "Wild Zucchini", "crop": "zucchini", "texture": stage_texture("zucchini", 5),
                      "biomes": ["IS_PLAINS", "IS_SAVANNA"]},
}
GRASS_SEEDS = ["lettuce_seeds", "spinach_seeds", "radish", "peas", "cucumber_seeds", "eggplant_seeds", "zucchini_seeds"]

ITEMS = {
    "lettuce": {"display": "Lettuce", "food": [2, 0.3], "compost": "medium",
                "tags": ["c:crops/lettuce", "c:foods/vegetable", "minecraft:rabbit_food"]},
    "lettuce_seeds": {"display": "Lettuce Seeds", "plants": "lettuce_crop", "compost": "low",
                      "tags": ["c:seeds/lettuce", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "spinach": {"display": "Spinach", "food": [1, 0.6], "compost": "medium", "tags": ["c:crops/spinach", "c:foods/vegetable"]},
    "spinach_seeds": {"display": "Spinach Seeds", "plants": "spinach_crop", "compost": "low",
                      "tags": ["c:seeds/spinach", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "radish": {"display": "Radish", "plants": "radish_crop", "food": [1, 0.6], "compost": "medium",
               "tags": ["c:crops/radish", "c:foods/vegetable", "minecraft:pig_food"]},
    "peas": {"display": "Peas", "plants": "pea_crop", "food": [1, 0.3], "compost": "medium",
             "tags": ["c:seeds/peas", "c:crops/peas", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "cucumber": {"display": "Cucumber", "food": [2, 0.3], "compost": "medium", "tags": ["c:crops/cucumber", "c:foods/vegetable"]},
    "cucumber_seeds": {"display": "Cucumber Seeds", "plants": "cucumber_crop", "trellis_seed": True, "compost": "low",
                       "tags": ["c:seeds/cucumber", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "eggplant": {"display": "Eggplant", "food": [2, 0.3], "compost": "medium", "tags": ["c:crops/eggplant", "c:foods/vegetable"]},
    "eggplant_seeds": {"display": "Eggplant Seeds", "plants": "eggplant_crop", "compost": "low",
                       "tags": ["c:seeds/eggplant", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "zucchini": {"display": "Zucchini", "food": [2, 0.3], "compost": "medium", "tags": ["c:crops/zucchini", "c:foods/vegetable"]},
    "zucchini_seeds": {"display": "Zucchini Seeds", "plants": "zucchini_crop", "compost": "low",
                       "tags": ["c:seeds/zucchini", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "roasted_eggplant": {"display": "Roasted Eggplant", "food": [5, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "grilled_zucchini": {"display": "Grilled Zucchini", "food": [5, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "green_salad": {"display": "Green Salad", "food": [8, 0.6], "stew": True, "tags": ["c:foods"]},
    "pea_soup": {"display": "Pea Soup", "food": [7, 0.6], "stew": True, "tags": ["c:foods/soup"]},
}

# A cucumber, an eggplant and a zucchini craft into their seeds, as the fruit crops' fruit do.
SHAPELESS = [{"id": seed, "inputs": [f"jugcraft:{fruit}"], "result": seed, "count": 1, "category": "misc"}
             for fruit, seed in (("cucumber", "cucumber_seeds"), ("eggplant", "eggplant_seeds"), ("zucchini", "zucchini_seeds"))]
SHAPELESS.append({"id": "green_salad", "inputs": ["minecraft:bowl", "jugcraft:lettuce", "jugcraft:spinach", "jugcraft:cucumber",
                                                  "jugcraft:radish"], "result": "green_salad", "count": 1})
# Furnace, smoker and campfire (tools/agriculture.py COOKING).
COOKING = {"roasted_eggplant": {"input": "eggplant", "xp": 0.35}, "grilled_zucchini": {"input": "zucchini", "xp": 0.35}}
POT_RECIPES = {
    "pea_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:peas": 2, "jugcraft:onion": 1, "minecraft:carrot": 1}, "time": 200},
}
# The menu's Ratatouille (tools/menu.py, the owner's art): tomato, pepper and onion, now with the eggplant and zucchini a
# ratatouille is made of, in place of the beetroot and garlic it stood in for until they grew.
RATATOUILLE = {"minecraft:bowl": 1, "jugcraft:tomato": 1, "jugcraft:pepper": 1, "jugcraft:onion": 1, "jugcraft:eggplant": 1,
               "jugcraft:zucchini": 1}
