"""Fruit crops (asked for on 8 October 2026, before the owner's pies, tarts and milkshakes that need them): strawberries,
blueberries and coffee grow as bushes on farmland, picked when ripe and fruiting again, as the pepper does
(tools/agriculture.py TALL_CROPS, agriculture/TallCrop); plums and bananas grow on trees, as the orchards' fruit does
(tools/orchard.py TREES). The owner chose to add them as crops in Jugcraft's own art ("Add as crops, Jugcraft art"), all
also found wild; their library has no art for any of them. Every texture is drawn by tools/fruit_crop_textures.py.

- **Strawberry Plant, Blueberry Bush and Coffee Plant:** planted from their seeds on farmland, they grow through eight
  ages and flower; ripe (age 7), a right-click picks their fruit and they drop back to age `pick_reset` and ripen again.
  Each is found wild in its biomes (WILD), its seeds come from breaking short grass (GRASS_SEEDS) and a fruit crafts into
  its seeds, so one fruit traded starts a patch.
- **Coffee:** a coffee plant gives Coffee Cherries; roasted (furnace, smoker or campfire) they are Coffee Beans, which the
  Coffee Cake takes (tools/cakes.py), and the owner's coffee tart will.
- **Jams:** Strawberry, Blueberry and Plum Jam, cooked into a Mason Jar in the Cooking Pot and sealed in the Canning Kettle
  as the pantry's other preserves are (tools/agriculture.py PANTRY).

Balance (docs/BALANCE.md): a strawberry or a handful of blueberries is a berry's worth (vanilla's sweet berries give 2);
a jar's four servings give no more than the fruit and sugar that went in (tools/check_mod_data.py checks every preserve).
"""

# The bushes, as tools/agriculture.py TALL_CROPS entries (in the TallCrop enum's order after the others): a block tall at
# every age; pick: what a ripe bush gives (min, max); pick_reset: the age picking sets it back to; growth_time: as the
# pepper's. textures: each age's texture (tools/fruit_crop_textures.py: six stages, the last two green, then ripe).
BUSHES = {
    "strawberry": {"block": "strawberry_crop", "display": "Strawberry Plant", "seed": "strawberry_seeds",
                   "heights": [1, 1, 1, 1, 1, 1, 1, 1], "pick": {"item": "strawberry", "min": 1, "max": 3}, "pick_reset": 5,
                   "growth_time": 1.25},
    "blueberry": {"block": "blueberry_crop", "display": "Blueberry Bush", "seed": "blueberry_seeds",
                  "heights": [1, 1, 1, 1, 1, 1, 1, 1], "pick": {"item": "blueberries", "min": 2, "max": 4}, "pick_reset": 5,
                  "growth_time": 1.25},
    "coffee": {"block": "coffee_crop", "display": "Coffee Plant", "seed": "coffee_seeds",
               "heights": [1, 1, 1, 1, 1, 1, 1, 1], "pick": {"item": "coffee_cherries", "min": 1, "max": 3}, "pick_reset": 5,
               "growth_time": 1.5},
}
STAGES = 6


def stage_texture(bush, stage):
    return f"{bush}_stage{stage}"


for _bush, _info in BUSHES.items():
    _info["textures"] = [[stage_texture(_bush, s)] for s in (0, 0, 1, 1, 2, 3, 4, 5)]

# Wild plants (tools/agriculture.py WILD_CROPS): found in patches in these biomes (ConventionalBiomeTags), wearing the ripe
# bush's texture; broken, they give 1-2 seeds.
WILD = {
    "wild_strawberries": {"display": "Wild Strawberries", "crop": "strawberry", "texture": stage_texture("strawberry", 5),
                          "biomes": ["IS_FOREST", "IS_FLORAL"]},
    "wild_blueberries": {"display": "Wild Blueberries", "crop": "blueberry", "texture": stage_texture("blueberry", 5),
                         "biomes": ["IS_TAIGA", "IS_HILL"]},
    "wild_coffee": {"display": "Wild Coffee", "crop": "coffee", "texture": stage_texture("coffee", 5),
                    "biomes": ["IS_JUNGLE"]},
}
GRASS_SEEDS = ["strawberry_seeds", "blueberry_seeds", "coffee_seeds"]

ITEMS = {
    "strawberry": {"display": "Strawberry", "food": [2, 0.3], "compost": "medium",
                   "tags": ["c:foods", "c:foods/berry", "c:crops/strawberry"]},
    "strawberry_seeds": {"display": "Strawberry Seeds", "plants": "strawberry_crop", "compost": "low", "tags": ["c:seeds/strawberry"]},
    "blueberries": {"display": "Blueberries", "food": [2, 0.2], "compost": "medium",
                    "tags": ["c:foods", "c:foods/berry", "c:crops/blueberry"]},
    "blueberry_seeds": {"display": "Blueberry Seeds", "plants": "blueberry_crop", "compost": "low", "tags": ["c:seeds/blueberry"]},
    "coffee_cherries": {"display": "Coffee Cherries", "compost": "medium", "tags": ["c:crops/coffee"]},
    "coffee_seeds": {"display": "Coffee Seeds", "plants": "coffee_crop", "compost": "low", "tags": ["c:seeds/coffee"]},
    "coffee_beans": {"display": "Coffee Beans", "compost": "medium", "tags": []},
}

# A fruit crafts into its seeds.
SHAPELESS = [{"id": seed, "inputs": [f"jugcraft:{fruit}"], "result": seed, "count": 1, "category": "misc"}
             for fruit, seed in (("strawberry", "strawberry_seeds"), ("blueberries", "blueberry_seeds"), ("coffee_cherries", "coffee_seeds"))]
# Coffee cherries roast into coffee beans (tools/agriculture.py COOKING: furnace, smoker and campfire).
COOKING = {"coffee_beans": {"input": "coffee_cherries", "xp": 0.2, "category": "misc"}}

# The jams (tools/agriculture.py PANTRY "preserves") and their Cooking Pot recipes, as the orchards' preserves: six berries
# (as the sweet berry jam takes) or three plums, and two sugar, for four servings of 3.
PRESERVES = {
    "strawberry_jam": {"display": "Strawberry Jam", "food": [3, 0.4], "effect": None, "color": 0xD0203A, "kind": "sweet"},
    "blueberry_jam": {"display": "Blueberry Jam", "food": [3, 0.4], "effect": None, "color": 0x3A2E7A, "kind": "sweet"},
    "plum_jam": {"display": "Plum Jam", "food": [3, 0.4], "effect": None, "color": 0x6A1E4A, "kind": "sweet"},
}
POT_RECIPES = {
    "strawberry_jam": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:strawberry": 6, "minecraft:sugar": 2}, "time": 300},
    "blueberry_jam": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:blueberries": 6, "minecraft:sugar": 2}, "time": 300},
    "plum_jam": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:plum": 3, "minecraft:sugar": 2}, "time": 300},
}


def blocks():
    """The bushes and their wild plants."""
    return [info["block"] for info in BUSHES.values()] + list(WILD)
