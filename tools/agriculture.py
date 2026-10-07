"""Single source of truth for the Agriculture branch: crops, seeds, foods, farm tools.

generate_material_data.py writes the JSON resources from these tables and
check_mod_data.py verifies them. Keep in sync with agriculture/TallCrop.java and
agriculture/JugcraftAgriculture.java; the checker compares the two.
See docs/branches/AGRICULTURE.md for the design.
"""

import plants
import graveyard
import midway
import ferris_wheel
import pinata
import hot_air_balloon
import leaf_blower
import decor15
import decor16
import decor17
import decor18
import decor19
import decor20

FEATURE = "agriculture"

# Tall crops grow through ages 0-7. heights[age] is how many blocks tall the plant is at
# that age; it only grows into air. At age 7 it is ripe: right-click (or a sickle) picks
# the produce and sets it back to pick_reset, which has the same height, so the plant
# keeps standing. That keeps cornfields and mazes intact through every harvest.
# textures[age] lists one texture per block, bottom first.
TALL_CROPS = {
    "corn": {
        "block": "corn_crop", "display": "Corn Crop", "seed": "corn_kernels",
        "heights": [1, 1, 1, 2, 2, 3, 3, 3],
        "pick": {"item": "corn", "min": 2, "max": 3}, "pick_reset": 5,
        # Grows this many times slower than wheat: a 3-block plant is a bigger investment.
        "growth_time": 1.5,
        "textures": [
            ["corn_sprout"], ["corn_seedling"], ["corn_young"],
            ["corn_stalk", "corn_young_top"],
            ["corn_stalk", "corn_leafy_top"],
            ["corn_stalk", "corn_stalk_middle", "corn_top"],
            ["corn_stalk", "corn_middle_silk", "corn_tassel"],
            ["corn_stalk_ripe", "corn_middle_ears", "corn_tassel_ripe"],
        ],
    },
    "sunflower": {
        "block": "sunflower_crop", "display": "Sunflower Crop", "seed": "sunflower_seeds",
        "heights": [1, 1, 1, 2, 2, 2, 2, 2],
        "pick": {"item": "sunflower_seeds", "min": 2, "max": 4}, "pick_reset": 5,
        "growth_time": 1.25,
        "textures": [
            ["sunflower_sprout"], ["sunflower_seedling"], ["sunflower_young"],
            ["sunflower_stem", "sunflower_young_top"],
            ["sunflower_stem", "sunflower_leafy_top"],
            ["sunflower_stem", "sunflower_bud"],
            ["sunflower_stem", "sunflower_opening"],
            ["sunflower_stem", "sunflower_bloom"],
        ],
    },
}
# Kitchen Garden (slice 2). Tomatoes climb a trellis ("trellis": True): the plant only grows into
# trellis blocks, not air, and every block of it drops its trellis again when broken. Peppers
# are a one-block bush that is picked like the tall crops.
TALL_CROPS["tomato"] = {
    "block": "tomato_crop", "display": "Tomato Plant", "seed": "tomato_seeds",
    "heights": [1, 1, 1, 2, 2, 2, 2, 2],
    "pick": {"item": "tomato", "min": 2, "max": 4}, "pick_reset": 5,
    "growth_time": 1.25, "trellis": True,
    "textures": [
        ["tomato_sprout"], ["tomato_seedling"], ["tomato_young"],
        ["tomato_vine", "tomato_climbing_top"],
        ["tomato_vine", "tomato_leafy_top"],
        ["tomato_vine_flowers", "tomato_flowering_top"],
        ["tomato_vine_green", "tomato_green_top"],
        ["tomato_vine_ripe", "tomato_ripe_top"],
    ],
}
TALL_CROPS["pepper"] = {
    "block": "pepper_crop", "display": "Pepper Plant", "seed": "pepper_seeds",
    "heights": [1, 1, 1, 1, 1, 1, 1, 1],
    "pick": {"item": "pepper", "min": 1, "max": 3}, "pick_reset": 5,
    "growth_time": 1.25,
    "textures": [["pepper_stage0"], ["pepper_stage0"], ["pepper_stage1"], ["pepper_stage1"],
                 ["pepper_stage2"], ["pepper_stage3"], ["pepper_stage4"], ["pepper_stage5"]],
}
# Halloween harvest. Ornamental (flint) corn grows like corn but ripens into multicoloured ears for
# decoration. Its kernels come from grass and, now and then, from breaking Wild Corn (WILD_BONUS).
TALL_CROPS["ornamental_corn"] = {
    "block": "ornamental_corn_crop", "display": "Ornamental Corn Crop", "seed": "ornamental_corn_kernels",
    "heights": [1, 1, 1, 2, 2, 3, 3, 3],
    "pick": {"item": "ornamental_corn", "min": 1, "max": 2}, "pick_reset": 5,
    "growth_time": 1.5,
    "textures": [
        ["corn_sprout"], ["corn_seedling"], ["corn_young"],
        ["corn_stalk", "corn_young_top"],
        ["corn_stalk", "corn_leafy_top"],
        ["corn_stalk", "corn_stalk_middle", "corn_top"],
        ["corn_stalk", "corn_middle_silk", "corn_tassel"],
        ["corn_stalk_ripe", "ornamental_corn_middle_ears", "corn_tassel_ripe"],
    ],
}
# Breaking a corn plant three blocks tall (age 5 or more, so also after picking) drops dry stalks for
# Corn Shocks, from its bottom block only.
STALKS = {"item": "corn_stalks", "min": 1, "max": 2, "from_age": 5, "crops": ["corn", "ornamental_corn"]}
TALL_SECTIONS = 3  # The shared "section" block state property runs 0..2.

# One-block crops follow vanilla crop rules (farmland, light, bone meal) plus the legume bonus.
# stages[age] -> texture index <block>_stage<n>. loot: "root" = the planted item drops, more when
# ripe (vanilla carrots); "grain" = produce when ripe else seed, plus bonus seeds (vanilla wheat).
CROPS = {
    "beans": {"block": "bean_crop", "display": "Bean Crop", "seed": "beans", "produce": "beans",
              "legume": True, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root"},
    "sweet_potato": {"block": "sweet_potato_crop", "display": "Sweet Potato Crop", "seed": "sweet_potato",
                     "produce": "sweet_potato", "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3],
                     "loot": "root"},
    "flax": {"block": "flax_crop", "display": "Flax Crop", "seed": "flax_seeds", "produce": "flax",
             "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "grain"},
    # Kitchen Garden.
    "onion": {"block": "onion_crop", "display": "Onion Crop", "seed": "onion", "produce": "onion",
              "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root"},
    "garlic": {"block": "garlic_crop", "display": "Garlic Crop", "seed": "garlic", "produce": "garlic",
               "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root"},
    "cabbage": {"block": "cabbage_crop", "display": "Cabbage Crop", "seed": "cabbage_seeds", "produce": "cabbage",
                "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "grain"},
    "oats": {"block": "oat_crop", "display": "Oat Crop", "seed": "oat_seeds", "produce": "oats",
             "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "grain"},
    "barley": {"block": "barley_crop", "display": "Barley Crop", "seed": "barley_seeds", "produce": "barley",
               "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "grain"},
    # Festival crops.
    "turnip": {"block": "turnip_crop", "display": "Turnip Crop", "seed": "turnip", "produce": "turnip",
               "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root"},
    # The graveyard flora: sculpted (tools/flora_models.py) rather than drawn as crossed pictures.
    "mandrake": {"block": "mandrake_crop", "display": "Mandrake", "seed": "mandrake_root", "produce": "mandrake_root",
                 "legume": False, "stages": [0, 0, 1, 1, 2, 2, 2, 3], "loot": "root", "sculpted": True},
}

# Wild plants: the natural entry point. Found in patches on grass in fitting biomes (new chunks
# only); breaking one gives 1-2 of its crop's planting item. Shears take the plant itself.
# texture: which crop texture the plant reuses. biomes: Fabric ConventionalBiomeTags fields.
WILD_CROPS = {
    "wild_corn": {"display": "Wild Corn", "crop": "corn", "texture": "corn_wild",
                  "biomes": ["IS_PLAINS", "IS_SAVANNA"]},
    "wild_sunflower": {"display": "Wild Sunflower", "crop": "sunflower", "texture": "sunflower_wild",
                       "biomes": ["IS_PLAINS"]},
    "wild_beans": {"display": "Wild Beans", "crop": "beans", "texture": "bean_stage3",
                   "biomes": ["IS_FOREST", "IS_JUNGLE"]},
    "wild_sweet_potato": {"display": "Wild Sweet Potato", "crop": "sweet_potato", "texture": "sweet_potato_stage3",
                          "biomes": ["IS_SAVANNA", "IS_JUNGLE"]},
    "wild_flax": {"display": "Wild Flax", "crop": "flax", "texture": "flax_stage2",
                  "biomes": ["IS_PLAINS", "IS_FLORAL"]},
    "wild_tomato": {"display": "Wild Tomato", "crop": "tomato", "texture": "tomato_wild",
                    "biomes": ["IS_JUNGLE", "IS_SAVANNA"]},
    "wild_pepper": {"display": "Wild Pepper", "crop": "pepper", "texture": "pepper_stage5",
                    "biomes": ["IS_SAVANNA", "IS_BADLANDS"]},
    "wild_onion": {"display": "Wild Onion", "crop": "onion", "texture": "onion_stage3",
                   "biomes": ["IS_PLAINS", "IS_HILL"]},
    "wild_garlic": {"display": "Wild Garlic", "crop": "garlic", "texture": "garlic_stage3",
                    "biomes": ["IS_FOREST", "IS_TAIGA"]},
    "wild_cabbage": {"display": "Wild Cabbage", "crop": "cabbage", "texture": "cabbage_stage2",
                     "biomes": ["IS_WINDSWEPT", "IS_HILL"]},
    "wild_oats": {"display": "Wild Oats", "crop": "oats", "texture": "oat_stage2",
                  "biomes": ["IS_PLAINS", "IS_TAIGA"]},
    "wild_barley": {"display": "Wild Barley", "crop": "barley", "texture": "barley_stage2",
                    "biomes": ["IS_SAVANNA", "IS_HILL"]},
    "wild_turnip": {"display": "Wild Turnip", "crop": "turnip", "texture": "turnip_stage3",
                    "biomes": ["IS_TAIGA", "IS_BIRCH_FOREST"]},
    "wild_mandrake": {"display": "Wild Mandrake", "crop": "mandrake", "texture": "mandrake", "biomes": ["IS_SPOOKY"], "sculpted": True},
}
# Extra drop when a wild plant is broken without shears: Wild Corn sometimes gives ornamental corn kernels.
WILD_BONUS = {"wild_corn": {"item": "ornamental_corn_kernels", "chance": 0.1}}
# One patch in about 1 of `rarity` chunks of a matching biome; `tries` placement attempts per patch.
WILD_PATCH = {"rarity": 24, "tries": 24, "spread_xz": 5, "spread_y": 2}
WILD_COMPOST = "medium"
# Breaking short grass has this chance to drop one Jugcraft seed, chosen evenly from GRASS_SEEDS: as
# often as vanilla wheat seeds (0.125), however many crops there are. Every crop is reachable in any
# biome and in worlds generated before this feature.
GRASS_SEEDS = ["corn_kernels", "sunflower_seeds", "beans", "sweet_potato", "flax_seeds",
               "tomato_seeds", "pepper_seeds", "onion", "garlic", "cabbage_seeds", "oat_seeds", "barley_seeds",
               "butternut_squash_seeds", "acorn_squash_seeds", "warty_gourd_seeds", "turnip", "cranberries", "chestnut",
               "giant_pumpkin_seeds", "white_pumpkin_seeds", "jarrahdale_pumpkin_seeds", "cinderella_pumpkin_seeds", "red_kuri_pumpkin_seeds",
               "kabocha_pumpkin_seeds", "bottle_gourd_seeds",
               "ornamental_corn_kernels", "mandrake_root"]
GRASS_SEED_CHANCE = 0.125

# Plain and food items. food: [nutrition, saturation modifier] (vanilla carrot is [3, 0.6],
# baked potato [5, 0.6], rabbit stew [10, 0.6]). plants: the crop block a seed item places.
# compost: composter chance tier, as vanilla's (low 30 %, medium 65 %, medium_high 85 %).
# tags: extra item tags.
ITEMS = {
    "corn": {"display": "Corn", "food": [3, 0.6], "compost": "medium", "tags": ["c:crops/corn", "c:foods/vegetable", "minecraft:pig_food"]},
    "corn_kernels": {"display": "Corn Kernels", "plants": "corn_crop", "compost": "low",
                     "tags": ["c:seeds/corn", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "roasted_corn": {"display": "Roasted Corn", "food": [5, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "popcorn": {"display": "Popcorn", "food": [2, 0.3], "compost": "medium_high", "tags": ["c:foods"]},
    "sunflower_seeds": {"display": "Sunflower Seeds", "plants": "sunflower_crop", "compost": "low",
                        "tags": ["c:seeds/sunflower", "c:crops/sunflower", "minecraft:chicken_food",
                                 "minecraft:parrot_food"]},
    "roasted_sunflower_seeds": {"display": "Roasted Sunflower Seeds", "food": [2, 0.3], "compost": "medium_high", "tags": ["c:foods"]},
    "roasted_pumpkin_seeds": {"display": "Roasted Pumpkin Seeds", "food": [2, 0.3], "compost": "medium_high", "tags": ["c:foods"]},
    "beans": {"display": "Beans", "plants": "bean_crop", "compost": "medium", "tags": ["c:seeds/beans", "c:crops/beans"]},
    "sweet_potato": {"display": "Sweet Potato", "plants": "sweet_potato_crop", "food": [2, 0.3], "compost": "medium",
                     "tags": ["c:crops/sweet_potato", "c:foods/vegetable", "minecraft:pig_food"]},
    "baked_sweet_potato": {"display": "Baked Sweet Potato", "food": [5, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "flax": {"display": "Flax", "compost": "medium", "tags": ["c:crops/flax"]},
    "flax_seeds": {"display": "Flax Seeds", "plants": "flax_crop", "compost": "low",
                   "tags": ["c:seeds/flax", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "three_sisters_stew": {"display": "Three Sisters Stew", "food": [10, 0.6], "stew": True,
                           "tags": ["c:foods/soup"]},
    # Kitchen Garden. "trellis_seed": planted on a trellis, not on bare farmland.
    "tomato": {"display": "Tomato", "food": [3, 0.3], "compost": "medium", "tags": ["c:crops/tomato", "c:foods/vegetable"]},
    "tomato_seeds": {"display": "Tomato Seeds", "plants": "tomato_crop", "trellis_seed": True, "compost": "low",
                     "tags": ["c:seeds/tomato", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "pepper": {"display": "Pepper", "food": [2, 0.3], "compost": "medium", "tags": ["c:crops/pepper", "c:foods/vegetable"]},
    "pepper_seeds": {"display": "Pepper Seeds", "plants": "pepper_crop", "compost": "low",
                     "tags": ["c:seeds/pepper", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "onion": {"display": "Onion", "plants": "onion_crop", "compost": "medium", "tags": ["c:crops/onion"]},
    "garlic": {"display": "Garlic", "plants": "garlic_crop", "compost": "medium", "tags": ["c:crops/garlic"]},
    "cabbage": {"display": "Cabbage", "food": [3, 0.6], "compost": "medium",
                "tags": ["c:crops/cabbage", "c:foods/vegetable", "minecraft:rabbit_food", "minecraft:pig_food"]},
    "cabbage_seeds": {"display": "Cabbage Seeds", "plants": "cabbage_crop", "compost": "low",
                      "tags": ["c:seeds/cabbage", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "oats": {"display": "Oats", "compost": "medium",
             "tags": ["c:crops/oat", "minecraft:horse_food", "minecraft:cow_food", "minecraft:sheep_food", "minecraft:goat_food"]},
    "oat_seeds": {"display": "Oat Seeds", "plants": "oat_crop", "compost": "low",
                  "tags": ["c:seeds/oat", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "barley": {"display": "Barley", "compost": "medium",
               "tags": ["c:crops/barley", "minecraft:cow_food", "minecraft:sheep_food", "minecraft:goat_food"]},
    "barley_seeds": {"display": "Barley Seeds", "plants": "barley_crop", "compost": "low",
                     "tags": ["c:seeds/barley", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "barley_bread": {"display": "Barley Bread", "food": [5, 0.6], "compost": "medium_high", "tags": ["c:foods/bread"]},
    "sauerkraut": {"display": "Sauerkraut", "food": [4, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "garden_salad": {"display": "Garden Salad", "food": [7, 0.6], "stew": True, "tags": ["c:foods"]},
    "tomato_soup": {"display": "Tomato Soup", "food": [8, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "onion_soup": {"display": "Onion Soup", "food": [8, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "vegetable_soup": {"display": "Vegetable Soup", "food": [10, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "mushroom_barley_soup": {"display": "Mushroom Barley Soup", "food": [8, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "oat_porridge": {"display": "Oat Porridge", "food": [6, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "chili": {"display": "Chili", "food": [10, 0.8], "stew": True, "tags": ["c:foods/soup"]},
    "cabbage_rolls": {"display": "Cabbage Rolls", "food": [6, 0.8], "tags": ["c:foods"]},
    # Fall additions 20: pan de muerto, the sweet bread of Día de Muertos, its top crossed with dough "bones".
    "pan_de_muerto_dough": {"display": "Pan de Muerto Dough", "compost": "medium", "tags": []},
    "pan_de_muerto": {"display": "Pan de Muerto", "food": [6, 0.7], "compost": "medium_high", "tags": ["c:foods", "c:foods/bread"]},
    # Fall additions 24: acorns roasted in a furnace, smoker or on a campfire.
    "roasted_acorns": {"display": "Roasted Acorns", "food": [3, 0.4], "compost": "medium", "tags": ["c:foods"]},
    # Fall additions 18: a wild turkey's meat, raw (it cooks into a whole roast turkey) and carved from the roast.
    "raw_turkey": {"display": "Raw Turkey", "food": [3, 0.3], "tags": ["c:foods", "c:foods/raw_meat"]},
    "turkey_slice": {"display": "Slice of Roast Turkey", "food": [3, 0.6], "tags": ["c:foods", "c:foods/cooked_meat"]},
    # Festival crops. Gourd seeds plant stems; a chestnut plants a chestnut tree sapling; cranberries are
    # planted in shallow water ("bog_seed").
    "butternut_squash_seeds": {"display": "Butternut Squash Seeds", "plants": "butternut_squash_stem", "compost": "low",
                               "tags": ["c:seeds/butternut_squash", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "acorn_squash_seeds": {"display": "Acorn Squash Seeds", "plants": "acorn_squash_stem", "compost": "low",
                           "tags": ["c:seeds/acorn_squash", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "warty_gourd_seeds": {"display": "Warty Gourd Seeds", "plants": "warty_gourd_stem", "compost": "low",
                          "tags": ["c:seeds/warty_gourd", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "turnip": {"display": "Turnip", "plants": "turnip_crop", "food": [3, 0.6], "compost": "medium",
               "tags": ["c:crops/turnip", "c:foods/vegetable", "minecraft:pig_food", "minecraft:rabbit_food"]},
    # The graveyard flora's mandrake: planted from its root; screams when pulled (MANDRAKE).
    "mandrake_root": {"display": "Mandrake Root", "plants": "mandrake_crop", "compost": "medium", "tags": ["c:crops/mandrake"]},
    "cranberries": {"display": "Cranberries", "plants": "cranberry_bush", "bog_seed": True, "food": [2, 0.1], "compost": "low",
                    "tags": ["c:crops/cranberry", "c:foods/berry", "minecraft:fox_food"]},
    "chestnut": {"display": "Chestnut", "plants": "chestnut_sapling", "compost": "low",
                 "tags": ["c:crops/chestnut", "minecraft:pig_food"]},
    "roasted_chestnuts": {"display": "Roasted Chestnuts", "food": [4, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "baked_acorn_squash": {"display": "Baked Acorn Squash", "food": [6, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "squash_pie": {"display": "Squash Pie", "food": [8, 0.3], "compost": "medium_high", "tags": ["c:foods"]},
    "candy_corn": {"display": "Candy Corn", "food": [2, 0.1], "compost": "medium_high", "tags": ["c:foods/candy"]},
    "butternut_squash_soup": {"display": "Butternut Squash Soup", "food": [8, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "harvest_stew": {"display": "Harvest Stew", "food": [10, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "cranberry_sauce": {"display": "Cranberry Sauce", "food": [5, 0.6], "stew": True, "tags": ["c:foods"]},
    # Halloween harvest. "treat": eaten off a stick, which is left in the hand.
    "giant_pumpkin_seeds": {"display": "Giant Pumpkin Seeds", "plants": "giant_pumpkin_vine", "compost": "low",
                            "tags": ["c:seeds/giant_pumpkin", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "pumpkin_guts": {"display": "Pumpkin Guts", "compost": "medium", "tags": []},
    "pumpkin_soup": {"display": "Pumpkin Soup", "food": [8, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "white_pumpkin_seeds": {"display": "White Pumpkin Seeds", "plants": "white_pumpkin_stem", "compost": "low",
                            "tags": ["c:seeds/white_pumpkin", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "jarrahdale_pumpkin_seeds": {"display": "Jarrahdale Pumpkin Seeds", "plants": "jarrahdale_pumpkin_stem", "compost": "low",
                                 "tags": ["c:seeds/jarrahdale_pumpkin", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "cinderella_pumpkin_seeds": {"display": "Cinderella Pumpkin Seeds", "plants": "cinderella_pumpkin_stem", "compost": "low",
                                 "tags": ["c:seeds/cinderella_pumpkin", "minecraft:chicken_food", "minecraft:parrot_food"]},
    # Pumpkin Night (tools/decor20.py): the Red Kuri and the Kabocha, and their dishes.
    "red_kuri_pumpkin_seeds": {"display": "Red Kuri Pumpkin Seeds", "plants": "red_kuri_pumpkin_stem", "compost": "low",
                               "tags": ["c:seeds/red_kuri_pumpkin", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "kabocha_pumpkin_seeds": {"display": "Kabocha Pumpkin Seeds", "plants": "kabocha_pumpkin_stem", "compost": "low",
                              "tags": ["c:seeds/kabocha_pumpkin", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "red_kuri_soup": {"display": "Red Kuri Soup", "food": [9, 0.7], "stew": True, "tags": ["c:foods/soup"]},
    "kabocha_tempura": {"display": "Kabocha Tempura", "food": [6, 0.8], "compost": "medium_high", "tags": ["c:foods"]},
    "bottle_gourd_seeds": {"display": "Bottle Gourd Seeds", "plants": "bottle_gourd_stem", "compost": "low",
                           "tags": ["c:seeds/bottle_gourd", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "dried_bottle_gourd": {"display": "Dried Bottle Gourd", "compost": "medium", "tags": []},
    "ornamental_corn": {"display": "Ornamental Corn", "compost": "medium", "tags": ["c:crops/ornamental_corn"]},
    "ornamental_corn_kernels": {"display": "Ornamental Corn Kernels", "plants": "ornamental_corn_crop", "compost": "low",
                                "tags": ["c:seeds/ornamental_corn", "minecraft:chicken_food", "minecraft:parrot_food"]},
    "corn_stalks": {"display": "Corn Stalks", "compost": "medium", "tags": []},
    "caramel": {"display": "Caramel", "food": [2, 0.1], "compost": "medium_high", "tags": ["c:foods/candy"]},
    "caramel_apple": {"display": "Caramel Apple", "food": [6, 0.6], "treat": True, "tags": ["c:foods/candy"]},
    "popcorn_ball": {"display": "Popcorn Ball", "food": [5, 0.6], "compost": "medium_high", "tags": ["c:foods/candy"]},
    # Marshmallows (Halloween batch 12): toasted on a stick over a bonfire or campfire (MarshmallowStickItem); eating a
    # toasted or burnt one leaves the stick.
    "marshmallow": {"display": "Marshmallow", "food": [1, 0.1], "compost": "medium_high", "tags": ["c:foods/candy"]},
    "toasted_marshmallow": {"display": "Toasted Marshmallow", "food": [4, 0.5], "treat": True, "tags": ["c:foods/candy"]},
    "burnt_marshmallow": {"display": "Burnt Marshmallow", "food": [2, 0.1], "treat": True, "tags": []},
    # Trick-or-treating's rare prize: only villagers hand it out (an optional seasonal treat, nothing needs it).
    "king_size_candy_bar": {"display": "King-Size Candy Bar", "food": [8, 0.4], "compost": "medium_high", "tags": ["c:foods/candy"]},
    # Halloween treats (batch 13). Soul cakes (given out to soulers on All Hallows' Eve) and pumpkin bread; spiderweb
    # cupcakes and bat-wing cookies count as candy; the pumpkin spice latte and the witch's brew punch are drinks
    # (`drink`: an effect, vanilla MobEffects field, and its seconds), leaving their glass bottle.
    "soul_cake": {"display": "Soul Cake", "food": [4, 0.4], "compost": "medium_high", "tags": ["c:foods"]},
    "pumpkin_bread": {"display": "Pumpkin Bread", "food": [6, 0.8], "compost": "medium_high", "tags": ["c:foods/bread"]},
    "spiderweb_cupcake": {"display": "Spiderweb Cupcake", "food": [3, 0.4], "compost": "medium_high", "tags": ["c:foods/candy"]},
    "bat_wing_cookie": {"display": "Bat-Wing Cookie", "food": [2, 0.1], "compost": "medium_high", "tags": ["c:foods/candy"]},
    "pumpkin_spice_latte": {"display": "Pumpkin Spice Latte", "food": [3, 0.3], "drink": ["SPEED", 30], "tags": []},
    "witchs_brew_punch": {"display": "Witch's Brew Punch", "food": [2, 0.2], "drink": ["GLOWING", 10], "tags": []},
    # Spooky sweets (Cooking Pot): eaten even on a full stomach for a moment of magic, `sweet`: the effect (vanilla
    # MobEffects field) and its seconds, level I. Not compostable, like cooked meals.
    "glow_gum": {"display": "Glow Gum", "food": [1, 0.1], "sweet": ["GLOWING", 30], "tags": ["c:foods/candy"]},
    "ghost_taffy": {"display": "Ghost Taffy", "food": [1, 0.1], "sweet": ["INVISIBILITY", 3], "tags": ["c:foods/candy"]},
    "fizz_rocks": {"display": "Fizz Rocks", "food": [1, 0.1], "sweet": ["JUMP_BOOST", 20], "tags": ["c:foods/candy"]},
    "witchs_licorice": {"display": "Witch's Licorice", "food": [1, 0.1], "sweet": ["NIGHT_VISION", 45], "tags": ["c:foods/candy"]},
    # Fall additions 2, the cider mill. Apple seeds plant an apple sapling (from the pomace the Cider Press knocks out, or
    # broken apple leaves); pomace feeds pigs and composts. Cider is drawn from the press (sweet) or the Cider Barrel as it
    # ages (sparkling, then aged); mulled cider is sparkling cider simmered with mulling spices in the Cooking Pot. Every
    # cider leaves its glass bottle when drunk; `bottle_back`: crafting with it gives the bottle back too (sweet cider, in the
    # donuts). Sparkling cider doesn't: the Cooking Pot hands remainders back, and mulled cider keeps that bottle.
    "apple_seeds": {"display": "Apple Seeds", "plants": "apple_sapling", "compost": "low", "tags": ["c:seeds/apple"]},
    "apple_pomace": {"display": "Apple Pomace", "compost": "medium", "tags": ["minecraft:pig_food"]},
    "sweet_cider": {"display": "Sweet Cider", "food": [3, 0.3], "drink": ["HASTE", 30], "bottle_back": True, "tags": []},
    "sparkling_cider": {"display": "Sparkling Cider", "food": [3, 0.4], "drink": ["JUMP_BOOST", 60], "tags": []},
    "aged_cider": {"display": "Aged Cider", "food": [4, 0.6], "drink": ["ABSORPTION", 120], "tags": []},
    "mulled_cider": {"display": "Mulled Cider", "food": [6, 0.8], "drink": ["REGENERATION", 15], "tags": []},
    "mulling_spices": {"display": "Mulling Spices", "compost": "medium", "tags": []},
    # Fall additions 12: wild mushrooms cooked on their own, and stewed together.
    "sauteed_chanterelles": {"display": "Sautéed Chanterelles", "food": [5, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "roasted_porcini": {"display": "Roasted Porcini", "food": [6, 0.6], "compost": "medium_high", "tags": ["c:foods"]},
    "fried_puffball": {"display": "Fried Puffball", "food": [4, 0.5], "compost": "medium_high", "tags": ["c:foods"]},
    "foragers_stew": {"display": "Forager's Stew", "food": [10, 0.8], "stew": True, "tags": ["c:foods"]},
    # Bunker interiors (batch 59, tools/bunkerworks.py TRENCH_STEW): beef, potato and carrot stewed in a Cooking Pot, best
    # on a Field Kitchen. A stew with a short effect (`stew_effect`: [effect, seconds]).
    "trench_stew": {"display": "Trench Stew", "food": [10, 0.8], "stew": True, "stew_effect": ["REGENERATION", 5],
                    "tags": ["c:foods/soup"]},
    "apple_cider_donut": {"display": "Apple Cider Donut", "food": [3, 0.4], "compost": "medium_high", "tags": ["c:foods/candy"]},
}

# ---------------------------------------------------------------- Festival crops (slice 3)

# Gourds grow from stems like vanilla pumpkins. A stem on farmland grows through ages 0-7, then puts its
# gourd on a free neighbouring block that supports vegetation (vanilla tag minecraft:supports_stem_fruit)
# and bends towards it; breaking the gourd lets the stem grow another. Stems grow by CropGrowth, so
# squash next to beans grows 1.5x as fast (the Three Sisters with corn). Gourds are blocks: decorations
# for fall displays, and food. growth_time: times wheat's time per stage.
GOURDS = {
    "butternut_squash": {"display": "Butternut Squash", "seed": "butternut_squash_seeds", "growth_time": 1.0,
                         "compost": "medium", "tags": ["c:crops/squash", "c:crops/butternut_squash", "c:foods/vegetable"]},
    "acorn_squash": {"display": "Acorn Squash", "seed": "acorn_squash_seeds", "growth_time": 1.0,
                     "compost": "medium", "tags": ["c:crops/squash", "c:crops/acorn_squash", "c:foods/vegetable"]},
    "warty_gourd": {"display": "Warty Gourd", "seed": "warty_gourd_seeds", "growth_time": 1.0,
                    "compost": "medium", "tags": ["c:crops/gourd", "c:crops/warty_gourd"]},
    # Halloween harvest: three heirloom pumpkins, whole blocks like vanilla's ("cube"), that the Carving Knife
    # carves like a pumpkin; and the bottle gourd, dried into canteens and birdhouses.
    "white_pumpkin": {"display": "White Pumpkin", "seed": "white_pumpkin_seeds", "growth_time": 1.0, "cube": True,
                      "compost": "medium", "tags": ["c:crops/pumpkin", "c:crops/white_pumpkin", "jugcraft:heirloom_pumpkins"]},
    "jarrahdale_pumpkin": {"display": "Jarrahdale Pumpkin", "seed": "jarrahdale_pumpkin_seeds", "growth_time": 1.0, "cube": True,
                           "compost": "medium", "tags": ["c:crops/pumpkin", "c:crops/jarrahdale_pumpkin", "jugcraft:heirloom_pumpkins"]},
    "cinderella_pumpkin": {"display": "Cinderella Pumpkin", "seed": "cinderella_pumpkin_seeds", "growth_time": 1.0, "cube": True,
                           "compost": "medium", "tags": ["c:crops/pumpkin", "c:crops/cinderella_pumpkin", "jugcraft:heirloom_pumpkins"]},
    # Pumpkin Night (tools/decor20.py): a teardrop red-orange Red Kuri and a squat, flecked dark green Kabocha.
    "red_kuri_pumpkin": {"display": "Red Kuri Pumpkin", "seed": "red_kuri_pumpkin_seeds", "growth_time": 1.0, "cube": True,
                         "compost": "medium", "tags": ["c:crops/pumpkin", "c:crops/red_kuri_pumpkin", "jugcraft:heirloom_pumpkins"]},
    "kabocha_pumpkin": {"display": "Kabocha Pumpkin", "seed": "kabocha_pumpkin_seeds", "growth_time": 1.0, "cube": True,
                        "compost": "medium", "tags": ["c:crops/pumpkin", "c:crops/kabocha_pumpkin", "jugcraft:heirloom_pumpkins"]},
    "bottle_gourd": {"display": "Bottle Gourd", "seed": "bottle_gourd_seeds", "growth_time": 1.0,
                     "compost": "medium", "tags": ["c:crops/gourd", "c:crops/bottle_gourd"]},
}
STEM_TEXTURES = ["gourd_stem", "gourd_stem_attached", "gourd_stalk"]


def stem(gourd):
    return f"{gourd}_stem"


def attached_stem(gourd):
    return f"attached_{gourd}_stem"


# Cranberries are a bog crop. The bush stands in a still water source one block deep, rooted in bog soil
# (block tag jugcraft:bog_soil), and grows only with open air above the water. Ripe bushes are picked
# with a right-click or a sickle and flower again, like vanilla sweet berries. Breaking one leaves its
# water. growth_chance: one stage in this many random ticks with light 9 or more (sweet berries: 5).
CRANBERRY = {"block": "cranberry_bush", "display": "Cranberry Bush", "seed": "cranberries",
             "pick": {"item": "cranberries", "min": 2, "max": 3}, "pick_reset": 1, "growth_chance": 5,
             "stages": ["cranberry_stage0", "cranberry_stage1", "cranberry_stage2", "cranberry_stage3"]}
BOG_SOIL_TAG = "jugcraft:bog_soil"
BOG_SOIL = ["#minecraft:dirt", "#minecraft:mud", "#minecraft:grass_blocks", "#minecraft:sand", "minecraft:clay", "minecraft:gravel"]

# The chestnut tree, the branch's first fruit tree (orchards in slice 4 follow the same rules). A chestnut
# is its seed: it plants a chestnut sapling on dirt or grass, which grows into a tree like vanilla
# saplings (bone meal works). Leaves the tree grew (not ones a player placed) with air under them grow
# spiny burs that ripen, fruit 0 -> 1 -> 2, one step in fruit_chance random ticks (about a Minecraft day
# in all); a right-click picks the ripe ones. The tree is never cut down to harvest it.
CHESTNUT = {"sapling": "chestnut_sapling", "leaves": "chestnut_leaves", "seed": "chestnut", "fruit_chance": 10,
            "pick": {"item": "chestnut", "min": 1, "max": 2},
            # worldgen/feature/chestnut.json: a broad crown on a straight trunk.
            "trunk": {"base_height": 5, "height_rand_a": 2}, "foliage": {"radius": 3, "height": 3}}
# Wood sets: each tree's log, wood, stripped log and wood, planks, stairs, slab, fence and fence gate, generated
# alike (festival_data.wood_assets) and registered alike (JugcraftAgriculture.registerWoodSet). Logs and wood strip
# with an axe; logs saw into planks (sawmill). The chestnut is the Festival Crops' fruit tree; the larch is Alpine
# Spawn's seasonal conifer; maple, aspen, fir and dead wood come from the biomes branch's seasonal forests, the
# jacaranda from its fields and meadows, the willow from its wetlands, palm and cypress from its warm, dry lands,
# redwood, eucalyptus and mahogany from its big forests and rainforests, and the cedar from the tree roster's batch 1
# (docs/branches/TREES.md), the swamp cedar of its wetlands.
WOOD_SETS = {"chestnut": "Chestnut", "larch": "Larch", "maple": "Maple", "aspen": "Aspen", "fir": "Fir", "dead": "Dead",
             "jacaranda": "Jacaranda", "willow": "Willow", "palm": "Palm", "cypress": "Cypress", "redwood": "Redwood",
             "eucalyptus": "Eucalyptus", "mahogany": "Mahogany", "cedar": "Cedar"}
# The feature switches each wood's hand and sawmill recipes, and its tree's leaves and sapling, follow: every switch
# that grows its logs, any one of which turns them on (TREES.md rule 4). The chestnut grows wild (agriculture) and in
# the Orchard (biomes); the larch in Alpine Spawn and, as the larch and the tamarack, in Jugcraft's biomes.
# check_mod_data checks that every switch placing a shape, fallen log or placed tree of a wood is in its list.
WOOD_SWITCHES = {"chestnut": [FEATURE, "biomes"], "larch": ["alpine_spawn", "biomes"], "maple": ["biomes"],
                 "aspen": ["biomes"], "fir": ["biomes"], "dead": ["biomes"], "jacaranda": ["biomes"], "willow": ["biomes"],
                 "palm": ["biomes"], "cypress": ["biomes"], "redwood": ["biomes"], "eucalyptus": ["biomes"],
                 "mahogany": ["biomes"], "cedar": ["biomes"]}


def wood_blocks(wood, display):
    """One wood set's blocks and display names, in a fixed order."""
    return {f"{wood}_log": f"{display} Log", f"{wood}_wood": f"{display} Wood", f"stripped_{wood}_log": f"Stripped {display} Log",
            f"stripped_{wood}_wood": f"Stripped {display} Wood", f"{wood}_planks": f"{display} Planks",
            f"{wood}_stairs": f"{display} Stairs", f"{wood}_slab": f"{display} Slab", f"{wood}_fence": f"{display} Fence",
            f"{wood}_fence_gate": f"{display} Fence Gate"}


WOOD = {block: name for wood, display in WOOD_SETS.items() for block, name in wood_blocks(wood, display).items()}
WOOD_TAGS = {wood: f"jugcraft:{wood}_logs" for wood in WOOD_SETS}
WOOD_TAG = WOOD_TAGS["chestnut"]
STRIPPED = {f"{wood}_{part}": f"stripped_{wood}_{part}" for wood in WOOD_SETS for part in ("log", "wood")}
# Trees with their own leaves and sapling (besides the chestnut, whose sapling is planted from a chestnut). Their
# wood is in WOOD_SETS under the same name; their shapes are in tools/trees.py. Leaves drop saplings and sticks like
# spruce leaves (SAPLING_CHANCES with Fortune); leaves and saplings follow the wood's switch.
# "season": deciduous leaves (agriculture/SeasonalLeavesBlock, state "season": green, gold, bare) follow the server's
# season day (season/JugcraftSeasons): green from the first day, their autumn colour ("gold") from the second, bare
# from the third until the first comes round again (northern calendar days; seasons.hemisphere shifts the south).
# Each block turns up to JITTER days early or late, fixed by its position, so a crown turns gradually and trees
# differ; blocks catch up on random ticks (a changed block brings up to SPREAD touching leaves along), and leaves
# placed or grown from a sapling start in today's state. With seasons off they stay green. "autumn": the gold
# state's looks, weighted (random by position, as vanilla picks grass models). None: evergreen.
TREES = {
    "larch": {"leaves": "larch_needles", "leaves_display": "Larch Needles", "season": [91, 268, 318], "autumn": {"gold": 1},
              "base": "spruce"},
    "maple": {"leaves": "maple_leaves", "leaves_display": "Maple Leaves", "season": [95, 265, 310],
              "autumn": {"red": 3, "orange": 2, "gold": 1}, "base": "oak"},
    "aspen": {"leaves": "aspen_leaves", "leaves_display": "Aspen Leaves", "season": [96, 258, 302], "autumn": {"gold": 1},
              "base": "birch"},
    "fir": {"leaves": "fir_needles", "leaves_display": "Fir Needles", "season": None, "base": "spruce"},
    # The biomes branch's later batches. The jacaranda flowers all year, a crown of violet blossom.
    "jacaranda": {"leaves": "jacaranda_leaves", "leaves_display": "Jacaranda Leaves", "season": None, "base": "cherry"},
    # The willow of the wetlands: leaves out early and holds them late, turning yellow before they fall.
    "willow": {"leaves": "willow_leaves", "leaves_display": "Willow Leaves", "season": [88, 283, 328], "autumn": {"gold": 1},
               "base": "oak"},
    # Warm and dry lands: the palm of oases (and later the tropics), and the tall, narrow Mediterranean cypress.
    "palm": {"leaves": "palm_fronds", "leaves_display": "Palm Fronds", "season": None, "base": "jungle"},
    "cypress": {"leaves": "cypress_leaves", "leaves_display": "Cypress Leaves", "season": None, "base": "spruce"},
    # Big trees and rainforests: the redwood, the eucalyptus and the mahogany, all evergreen. "giant": the shape (in
    # tools/trees.py) that four saplings planted in a square grow (agriculture/GiantSaplingBlock).
    "redwood": {"leaves": "redwood_needles", "leaves_display": "Redwood Needles", "season": None, "base": "spruce",
                "giant": "giant_redwood"},
    "eucalyptus": {"leaves": "eucalyptus_leaves", "leaves_display": "Eucalyptus Leaves", "season": None, "base": "jungle"},
    "mahogany": {"leaves": "mahogany_leaves", "leaves_display": "Mahogany Leaves", "season": None, "base": "jungle",
                 "giant": "giant_mahogany"},
    # The tree roster's batch 1: the swamp cedar, an evergreen conifer of flat sprays of scale-leaves.
    "cedar": {"leaves": "cedar_leaves", "leaves_display": "Cedar Leaves", "season": None, "base": "spruce"},
}
SEASON_STATES = ["green", "gold", "bare"]
JITTER = 7
SPREAD = 128
SAPLING_CHANCES = [0.05, 0.0625, 0.083333336, 0.1]


def sapling(tree):
    return f"{tree}_sapling"


def leaf_looks(tree):
    """Texture (and model) names for each state of a tree's leaves: {state: {texture: weight}}."""
    leaves = TREES[tree]["leaves"]
    if TREES[tree]["season"] is None:
        return {None: {leaves: 1}}
    return {"green": {leaves: 1}, "gold": {f"{leaves}_{look}": weight for look, weight in TREES[tree]["autumn"].items()},
            "bare": {f"{leaves}_bare": 1}}


TREE_BLOCKS = {"chestnut_sapling": "Chestnut Sapling", "chestnut_leaves": "Chestnut Leaves",
               **{block: name for tree in TREES for block, name in
                  ((sapling(tree), f"{WOOD_SETS[tree]} Sapling"), (TREES[tree]["leaves"], TREES[tree]["leaves_display"]))}}
TREE_TEXTURES = (["chestnut_log", "chestnut_log_top", "stripped_chestnut_log", "stripped_chestnut_log_top", "chestnut_planks",
                  "chestnut_leaves", "chestnut_leaves_burs", "chestnut_leaves_ripe", "chestnut_sapling"]
                 + [name for wood in WOOD_SETS if wood != "chestnut" for name in (
                     f"{wood}_log", f"{wood}_log_top", f"stripped_{wood}_log", f"stripped_{wood}_log_top", f"{wood}_planks")]
                 + [texture for tree in TREES for looks in leaf_looks(tree).values() for texture in looks]
                 + [sapling(tree) for tree in TREES])

# Decorations. The Turnip Lantern is the original jack-o'-lantern: a carved turnip with a candle inside.
DECOR = {"turnip_lantern": {"display": "Turnip Lantern", "light": 13}}
DECOR_TEXTURES = ["turnip_lantern_side", "turnip_lantern_face", "turnip_lantern_top"]

# Festival crops that also grow wild as themselves, in new chunks: gourds lie on grass like vanilla
# pumpkins, ripe cranberry bushes stand in swamp shallows, and chestnut trees grow in forests.
FOUND_WILD = {
    "butternut_squash": {"biomes": ["IS_PLAINS", "IS_SAVANNA"], "on": "grass"},
    "acorn_squash": {"biomes": ["IS_FOREST", "IS_TAIGA"], "on": "grass"},
    "warty_gourd": {"biomes": ["IS_SWAMP", "IS_SPOOKY"], "on": "grass"},
    "cranberry_bush": {"biomes": ["IS_SWAMP"], "on": "bog"},
    "white_pumpkin": {"biomes": ["IS_BIRCH_FOREST", "IS_SNOWY"], "on": "grass"},
    "jarrahdale_pumpkin": {"biomes": ["IS_SAVANNA", "IS_WINDSWEPT"], "on": "grass"},
    "cinderella_pumpkin": {"biomes": ["IS_PLAINS", "IS_FLORAL"], "on": "grass"},
    "red_kuri_pumpkin": {"biomes": ["IS_TAIGA", "IS_SPOOKY"], "on": "grass"},
    "kabocha_pumpkin": {"biomes": ["IS_FOREST", "IS_JUNGLE"], "on": "grass"},
    "bottle_gourd": {"biomes": ["IS_JUNGLE", "IS_SAVANNA"], "on": "grass"},
}
GOURD_PATCH = {"rarity": 32, "tries": 8, "spread_xz": 4, "spread_y": 2}
CRANBERRY_PATCH = {"rarity": 4, "tries": 32, "spread_xz": 6}
CHESTNUT_TREES = {"biomes": ["IS_FOREST"], "rarity": 3}

# Pumpkin carving: the Carving Knife opens a 16x16 carving screen for one side of a vanilla pumpkin;
# the server checks the finished face and turns the pumpkin into a hand-carved pumpkin (a block entity
# holds all four sides). A torch inside lights it: glow = 4 + holes / 3 + shaved / 12, up to 15.
# Java: agriculture/PumpkinCarving.java, PumpkinCarvings.java, CarvedPumpkinBlock.java.
CARVING = {
    "knife": "carving_knife", "knife_display": "Carving Knife", "durability": 238,
    "block": "hand_carved_pumpkin", "display": "Hand-Carved Pumpkin",
    "size": 16, "faces": 4, "session_ticks": 6000,
    "glow": {"base": 4, "per_holes": 3, "per_shaved": 12, "max": 15},
    "templates": ["classic", "cat", "ghost", "spooky"],
    "knife_pattern": ["I", "S"], "knife_key": {"I": "minecraft:iron_ingot", "S": "minecraft:stick"},
}
CARVING_TEXTURES = ["carving_knife", "hand_carved_pumpkin", "hand_carved_pumpkin_lit"]
# Heirloom pumpkins the knife carves: each becomes its own hand-carved block (same rules, its own skin),
# and its first cut drops 4 of its seeds (loot table carve/<pumpkin>), like vanilla's carve/pumpkin.
CARVED_VARIETIES = {"white_pumpkin": "hand_carved_white_pumpkin", "jarrahdale_pumpkin": "hand_carved_jarrahdale_pumpkin",
                    "cinderella_pumpkin": "hand_carved_cinderella_pumpkin", "red_kuri_pumpkin": "hand_carved_red_kuri_pumpkin",
                    "kabocha_pumpkin": "hand_carved_kabocha_pumpkin"}
CARVE_SEEDS = 4
# Item tag of the heirloom pumpkins, which bake into vanilla pumpkin pie like a pumpkin does.
HEIRLOOM_TAG = "jugcraft:heirloom_pumpkins"

# ---------------------------------------------------------------- Halloween harvest (slice 4)

# The giant pumpkin. Giant Pumpkin Seeds (scooped out of pumpkins, or dropped by grass) plant a vine on
# farmland that grows like a pumpkin stem but vine_growth_time times slower, then sets one small fruit on a
# free side. While the vine holds it, each random tick of the fruit's master block gives 1 growth point, +1
# if the vine's farmland is moist, +1 if watered (Gourd Canteen) in the last watered_ticks; bone meal gives
# bone_meal_points. Points count up from planting: at grow_to_two it swells to 2x2x2, at grow_to_three to
# 3x3x3, if there is room (air, grass, flowers; never water) on ground fruit can lie on. Full grown it weighs
# start_weight (+0-20) kg and puts on weight_per_point kg a point up to max_weight, until carved. Each side
# carves as one face_size x face_size face; a torch lights it with glow = base + holes / per_holes +
# shaved / per_shaved. Breaking any block breaks it all and drops `drops[size]` pumpkins (and giant seeds
# when full grown).
GIANT_PUMPKIN = {
    "block": "giant_pumpkin", "vine": "giant_pumpkin_vine", "attached_vine": "attached_giant_pumpkin_vine",
    "seed": "giant_pumpkin_seeds", "display": "Giant Pumpkin", "vine_growth_time": 1.5,
    "grow_to_two": 16, "grow_to_three": 48, "bone_meal_points": 4,
    "start_weight": 100, "weight_per_point": 2, "max_weight": 1000, "watered_ticks": 24000,
    "max_size": 3, "face_size": 48, "reach": 3.5,
    "glow": {"base": 4, "per_holes": 27, "per_shaved": 108, "max": 15},
    "drops": {1: 1, 2: 4, 3: 9}, "seeds": [1, 3], "chop_table": "gameplay/chop_giant_pumpkin",
}
# The first cut into any plain pumpkin scoops it out: besides its seeds, 1-2 Pumpkin Guts and sometimes a
# giant pumpkin seed (loot table gameplay/scoop_pumpkin).
SCOOP = {"table": "gameplay/scoop_pumpkin", "guts": [1, 2], "giant_seed_chance": 0.1}

# The Harvest Scale weighs a full-grown giant pumpkin beside it and keeps the heaviest `board` it has
# weighed. The first time a pumpkin places, the player weighing it gets that place's ribbon (a trophy
# only); the scale remembers the last `remembered` pumpkins it gave ribbons to. Comparators read the last
# weight: weight * 15 / max_weight, at least 1.
HARVEST_SCALE = {"block": "harvest_scale", "display": "Harvest Scale", "board": 3, "remembered": 64,
                 "ribbons": {"first_prize_ribbon": "First Prize Ribbon", "second_prize_ribbon": "Second Prize Ribbon",
                             "third_prize_ribbon": "Third Prize Ribbon"}}

# Stencils: a Blank Stencil used on a carved side of a hand-carved pumpkin traces it into a Pumpkin
# Stencil (component jugcraft:stencil); held in the other hand while carving, the screen offers it.
STENCILS = {"blank": "blank_stencil", "blank_display": "Blank Stencil", "stencil": "pumpkin_stencil",
            "stencil_display": "Pumpkin Stencil"}
# The Gourd Canteen holds `capacity` sips of water (component jugcraft:canteen_water).
CANTEEN = {"item": "gourd_canteen", "display": "Gourd Canteen", "capacity": 3}

# Decorations of the Halloween harvest, with their display names. The scarecrow's shirt takes any of the 16
# dye colours (block state "shirt", red when placed).
HALLOWEEN_DECOR = {"scarecrow": "Scarecrow", "corn_shock": "Corn Shock", "ornamental_corn_bundle": "Ornamental Corn Bundle",
                   "gourd_birdhouse": "Gourd Birdhouse"}
SCARECROW_SHIRT = "red"
# Item tag of what a scarecrow wears for a head (drawn on its shoulders the way an armor stand wears a pumpkin).
SCARECROW_HEADS = "jugcraft:scarecrow_heads"
DYE_COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
              "purple", "blue", "brown", "green", "red", "black"]

# Mums (garden chrysanthemums): small flowers that go in flower pots, make dye and suspicious stew. They
# grow wild in flower forests, meadows and forests (one patch mixes all four colours).
MUMS = {
    "yellow_mum": {"display": "Yellow Mum", "dye": "yellow", "effect": "SATURATION", "seconds": 0.35},
    "orange_mum": {"display": "Orange Mum", "dye": "orange", "effect": "FIRE_RESISTANCE", "seconds": 4.0},
    "red_mum": {"display": "Red Mum", "dye": "red", "effect": "REGENERATION", "seconds": 8.0},
    "purple_mum": {"display": "Purple Mum", "dye": "purple", "effect": "NIGHT_VISION", "seconds": 5.0},
    # Fall additions 20: the cempasúchil marigold, grown and found wild with the mums.
    "marigold": {"display": "Cempasúchil Marigold", "dye": "orange", "effect": "LUCK", "seconds": 6.0},
}
MUM_PATCH = {"biomes": ["IS_FLORAL", "IS_FOREST"], "rarity": 16, "tries": 32, "spread_xz": 5, "spread_y": 2}


# ---------------------------------------------------------------- the pumpkin regatta

# A giant pumpkin 2 or 3 blocks wide hollows out into a boat: sneak and use a Carving Knife on its top. The
# 3x3x3 one becomes a Pumpkin Barge (4 seats) keeping its weight, carving and torch; the 2x2x2 one a Pumpkin
# Racer (1 seat) weighing racer_base_weight + weight_per_point kg a growth point. Speed in water against a
# vanilla boat runs from `fastest` at the lightest weight to `slowest` at the heaviest (each tick in water
# the speed is multiplied by f, (1 - F) / (1 - F f) = ratio, F = water_friction). `hitbox`: width, height;
# `shell`: drawn width, height and the row of the old face it starts at (blocks from the top); `seat`: height.
# Hollowing drops the pumpkin without its usual drops; the hollowing table gives `guts` and `seeds` by size.
PUMPKIN_BOATS = {
    "pumpkin_barge": {"display": "Pumpkin Barge", "kind": "BARGE", "size": 3, "seats": 4, "hitbox": [2.75, 1.125],
                      "shell": [3.0, 1.875, 0.625], "floor": 0.9, "seat": 1.0, "fastest": 0.95, "slowest": 0.70},
    "pumpkin_racer": {"display": "Pumpkin Racer", "kind": "RACER", "size": 2, "seats": 1, "hitbox": [1.75, 0.9],
                      "shell": [2.0, 1.5, 0.5], "floor": 0.7, "seat": 0.7, "fastest": 1.30, "slowest": 1.15},
}
RACER_BASE_WEIGHT = 30
WATER_FRICTION = 0.9
HOLLOW = {"table": "gameplay/hollow_giant_pumpkin", "guts": {2: [2, 4], 3: [4, 8]}, "seeds": {3: [1, 3]}}

# The Regatta Flag starts and finishes a run; its course is the Regatta Buoys (numbered 1 to max_number,
# floating on still water) within course_range blocks across and course_height up or down, in number order.
# A run: a countdown_ticks countdown, then each buoy within mark_radius blocks in order and back within
# finish_radius of the flag; over max_race_ticks, or faster than max_speed blocks in a tick, voids it. The
# board keeps the best `board` times (each racer once); a racer's first place on it gives that place's
# ribbon (the Harvest Scale's), once per flag; the flag remembers `remembered` racers.
REGATTA = {"flag": "regatta_flag", "flag_display": "Regatta Flag", "buoy": "regatta_buoy", "buoy_display": "Regatta Buoy",
           "max_number": 16, "course_range": 64, "course_height": 16, "mark_radius": 5.0, "finish_radius": 5.0,
           "countdown_ticks": 60, "max_race_ticks": 12000, "max_speed": 2.0, "board": 3, "remembered": 64}

# Trick-or-treating, only while the Halloween event runs (config/jugcraft.properties: halloween.start/end as
# MM-DD, halloween.timezone, halloween.mode auto/on/off; defaults in `window`). A Candy Bag used on a wooden
# door knocks; answer_ticks later, between dusk and midnight, in costume (item tag costume_tag), with a porch
# light within porch_radius blocks (block tag porch_light_tag, or any lit hand-carved or giant pumpkin) and a
# villager whose home bed is within home_radius blocks, that home gives each player one treat a night (loot
# table `table`; a costume hat adds a second roll costume_bonus of the time) and a harmless trick after.
# full_bag homes in one night earn the advancement of that name.
TRICK_OR_TREAT = {"bag": "candy_bag", "bag_display": "Candy Bag", "table": "gameplay/trick_or_treat",
                  "dusk": 12000, "midnight": 18000, "answer_ticks": 30, "knock_cooldown": 40, "porch_radius": 4,
                  "home_radius": 12, "full_bag": 10, "costume_bonus": 0.25,
                  "window": {"halloween.start": "10-20", "halloween.end": "11-03", "halloween.timezone": "UTC", "halloween.mode": "auto"},
                  # (item, weight, count): one roll, so every treat is a single sweet or a small handful.
                  "treats": [("jugcraft:candy_corn", 30, [1, 3]), ("jugcraft:caramel", 20, [1, 2]), ("minecraft:cookie", 15, [1, 3]),
                             ("jugcraft:popcorn_ball", 15, [1, 1]), ("jugcraft:caramel_apple", 10, [1, 1]),
                             ("jugcraft:king_size_candy_bar", 2, [1, 1])]}
COSTUMES = {"witch_hat": "Witch Hat", "ghost_sheet": "Ghost Sheet", "scarecrow_hat": "Scarecrow Hat"}
COSTUME_TAG = "jugcraft:trick_or_treat_costumes"
COSTUME_HAT_TAG = "jugcraft:costume_hats"
PORCH_LIGHT_TAG = "jugcraft:porch_lights"
PORCH_LIGHTS = ["minecraft:jack_o_lantern", "jugcraft:turnip_lantern"]
# Advancements granted from code (criterion "done"), in vanilla's Husbandry tab.
HALLOWEEN_ADVANCEMENTS = {
    "full_bag": {"icon": "jugcraft:candy_bag", "title": "Full Bag", "description": "Trick-or-treat at ten homes in one Halloween night",
                 "frame": "goal"},
    "pumpkin_regatta": {"icon": "jugcraft:pumpkin_racer", "title": "Pumpkin Regatta",
                        "description": "Finish a regatta course in a hollowed-out giant pumpkin", "frame": "task"},
    "wisp_in_a_jar": {"icon": "jugcraft:wisp_in_a_jar", "title": "Bottled Light", "description": "Catch a will-o'-wisp in a glass bottle",
                      "frame": "task"},
    "pumpkin_chunkin": {"icon": "jugcraft:trebuchet", "title": "Pumpkin Chunkin'",
                        "description": "Throw a pumpkin 50 blocks or more with a trebuchet", "frame": "goal"},
    "headless_horseman": {"icon": "jugcraft:horseman_lantern", "title": "Lost His Head",
                          "description": "Send the Headless Horseman back into the night", "frame": "challenge"},
    "lantern_festival": {"icon": "jugcraft:sky_lantern", "title": "A Sky Full of Wishes",
                         "description": "Be there when eight sky lanterns are let go together", "frame": "goal"},
    "harvest_home": {"icon": "jugcraft:feast_table", "title": "Harvest Home",
                     "description": "Share a grand feast at a Harvest Feast Table", "frame": "challenge"},
    "amazing": {"icon": "jugcraft:corn_maze_gate", "title": "A-maze-ing", "description": "Find your way through a corn maze",
                "frame": "task"},
    "ghost_hunter": {"icon": "jugcraft:spirit_lantern", "title": "Ghost Hunter", "description": "Catch a restless spirit in a glass bottle",
                     "frame": "goal"},
    "face_painter": {"icon": "jugcraft:face_paint_kit", "title": "Face Painter", "description": "Paint another player's face",
                     "frame": "task"},
    "candy_maker": {"icon": "jugcraft:candy_kettle", "title": "Sweet Science", "description": "Pour a batch of candy from a Candy Kettle",
                    "frame": "task"},
    "night_shift": {"icon": "jugcraft:bat_house", "title": "Night Shift", "description": "Watch bats pour out of a Bat House at dusk",
                    "frame": "task"},
    "knit_one_purl_two": {"icon": "jugcraft:knitting_needles", "title": "Knit One, Purl Two", "description": "Knit a garment on Knitting Needles",
                          "frame": "task"},
    "snug_as_a_bug": {"icon": "jugcraft:knit_sweater", "title": "Snug as a Bug",
                      "description": "Warm yourself by a campfire in a knit beanie, sweater and wool socks", "frame": "goal"},
    "is_anybody_there": {"icon": "jugcraft:spirit_board", "title": "Is Anybody There?",
                         "description": "Hold a séance at a Spirit Board and have a restless spirit answer", "frame": "task"},
    "unfinished_business": {"icon": "jugcraft:ectoplasm", "title": "Unfinished Business",
                            "description": "Give a restless spirit the thing it wishes for, and lay it to rest", "frame": "goal"},
    "gobble_gobble": {"icon": "jugcraft:raw_turkey", "title": "Gobble Gobble", "description": "Breed two wild turkeys", "frame": "task"},
    "carving_the_bird": {"icon": "jugcraft:roast_turkey", "title": "Carving the Bird",
                         "description": "Carve a slice from a roast turkey with a Carving Knife", "frame": "task"},
    "good_vibrations": {"icon": "jugcraft:theremin", "title": "Good Vibrations", "description": "Play a theremin without touching it",
                        "frame": "task"},
    "remembered": {"icon": "jugcraft:marigold", "title": "Remembered", "description": "Welcome a spirit to a complete ofrenda on a quiet night",
                   "frame": "goal"},
    "as_easy_as_pie": {"icon": "jugcraft:apple_pie", "title": "As Easy as Pie", "description": "Take a perfectly baked pie out of a Hearth Oven",
                       "frame": "task"},
    "man_of_straw": {"icon": "minecraft:hay_block", "title": "Man of Straw", "description": "Build a Hay Golem from hay bales and a carved pumpkin",
                     "frame": "task"},
    "fairy_ring": {"icon": "jugcraft:fly_agaric", "title": "Away with the Fairies",
                   "description": "Stand in a fairy ring under a full moon", "frame": "goal"},
    "forager": {"icon": "jugcraft:foraging_basket", "title": "Forager", "description": "Carry all five wild mushrooms in a Foraging Basket",
                "frame": "task"},
    "taffy_puller": {"icon": "jugcraft:salt_water_taffy", "title": "Pulling Power", "description": "Pull a tray of warm taffy until it's done",
                     "frame": "task"},
}
# The graveyard pack (tools/graveyard.py).
HALLOWEEN_ADVANCEMENTS.update(graveyard.ADVANCEMENTS)
HALLOWEEN_ADVANCEMENTS.update({
    "drink_me": {"icon": "jugcraft:shrinking_draught", "title": "Drink Me", "description": "Drink a Shrinking Draught", "frame": "task"},
    "fee_fi_fo_fum": {"icon": "jugcraft:giants_draught", "title": "Fee-Fi-Fo-Fum", "description": "Drink a Giant's Draught", "frame": "task"},
    "up_and_away": {"icon": "jugcraft:flying_broomstick", "title": "Up and Away", "description": "Take off on a flying broomstick",
                    "frame": "task"},
    "over_the_moon": {"icon": "jugcraft:flying_broomstick", "title": "Over the Moon",
                      "description": "Fly a broomstick high across the sky on a full-moon night", "frame": "challenge"},
    "silver_lining": {"icon": "jugcraft:silver_dagger", "title": "Silver Lining", "description": "Slay a werewolf with silver",
                      "frame": "task"},
    "leader_of_the_pack": {"icon": "jugcraft:shadow_werewolf_pelt", "title": "Leader of the Pack", "description": "Slay a shadow werewolf",
                           "frame": "challenge"},
    "wolfsbane_ward": {"icon": "jugcraft:wolfsbane", "title": "Not Tonight", "description": "Keep a werewolf at bay with wolfsbane",
                       "frame": "task"},
    "nuts_about_squirrels": {"icon": "jugcraft:acorn", "title": "Nuts About Squirrels", "description": "Breed two squirrels with nuts",
                             "frame": "task"},
    "little_jack": {"icon": "jugcraft:hand_carved_pumpkin", "title": "Little Jack",
                    "description": "Wake a carved pumpkin with a wisp or ectoplasm", "frame": "task"},
})
# The fall fair midway (tools/midway.py).
HALLOWEEN_ADVANCEMENTS.update(midway.ADVANCEMENTS)
# The Ferris wheel (tools/ferris_wheel.py).
HALLOWEEN_ADVANCEMENTS.update(ferris_wheel.ADVANCEMENTS)
# The piñata party (tools/pinata.py).
HALLOWEEN_ADVANCEMENTS.update(pinata.ADVANCEMENTS)
# The hot-air balloon fiesta (tools/hot_air_balloon.py).
HALLOWEEN_ADVANCEMENTS.update(hot_air_balloon.ADVANCEMENTS)
# The leaf blower (tools/leaf_blower.py).
HALLOWEEN_ADVANCEMENTS.update(leaf_blower.ADVANCEMENTS)
# The graveyard flora's mandrake (agriculture/Mandrakes): pulling a ripe or wild one (not with shears) screams, and every
# player within `scream_radius` blocks with nothing on their head is sickened (nausea for `nausea_seconds`). It is the
# mandrake crop at its last age or a wild mandrake broken by a player. Pulled with your own ears covered: Mind Your Ears.
MANDRAKE = {"crop": "mandrake_crop", "wild": "wild_mandrake", "root": "mandrake_root", "scream_radius": 8, "nausea_seconds": 8,
            "advancement": "mind_your_ears"}
HALLOWEEN_ADVANCEMENTS.update({
    "mind_your_ears": {"icon": "jugcraft:mandrake_root", "title": "Mind Your Ears", "description": "Pull up a mandrake with your ears covered",
                       "frame": "task"},
})


# ---------------------------------------------------------------- Halloween festivities

# The carving contest (agriculture/CarvingContest.java): a hand-carved pumpkin on a Judging Stand is entered by
# its carver (or by anyone, once the pumpkin was moved and forgot its carver); using the stand votes for its
# entrant while the Halloween event runs: one vote per player per Halloween, never for oneself (voting again
# moves it). Sneak-use shows the standings. When the event ends, the `places` entrants with the most votes get
# the Harvest Scale's ribbons (offline winners on their next visit). The server checks every check_ticks; a
# contest counts at most max_voters voters and the world keeps the latest max_contests contests.
CONTEST = {"stand": "judging_stand", "stand_display": "Judging Stand", "places": 3, "check_ticks": 200, "max_voters": 4096,
           "max_contests": 8}

# Costumed mobs (agriculture/CostumedMobs.java): while the event runs, each mob of `mobs` rolls once when it
# first enters the world, and `chance` of them dress up in one of `costumes` (equal odds) if their head is bare.
# Killed by a player with mob loot on, a costumed mob (entity tag `tag`) also drops one roll of `candy` (gift
# loot table `table`, rolled for the killer). The costume drops like any mob equipment (vanilla's 8.5%).
COSTUMED_MOBS = {"mobs": ["zombie", "husk", "skeleton", "stray", "zombie_villager"], "chance": 0.15,
                 "costumes": ["jugcraft:witch_hat", "jugcraft:ghost_sheet", "jugcraft:scarecrow_hat", "minecraft:carved_pumpkin",
                              "jugcraft:vampire_cape", "jugcraft:mummy_wraps", "jugcraft:skeleton_suit", "jugcraft:werewolf_mask",
                              "jugcraft:cat_ears_and_tail", "jugcraft:bat_wings"],
                 "tag": "jugcraft.costumed", "table": "entities/costumed_mob_candy",
                 # (item, weight, count)
                 "candy": [("jugcraft:candy_corn", 40, [1, 2]), ("jugcraft:caramel", 20, [1, 1]), ("jugcraft:glow_gum", 10, [1, 1]),
                           ("jugcraft:ghost_taffy", 10, [1, 1]), ("jugcraft:fizz_rocks", 10, [1, 1]),
                           ("jugcraft:witchs_licorice", 10, [1, 1])]}

# The Halloween Peddler (agriculture/HalloweenPeddler.java): while the event runs, a wandering trader arrives
# in a Witch Hat and adds `amount` of these offers, without repeats, to its usual wares (data:
# villager_trade/halloween_peddler/<name>, the villager_trade tag and trade_set halloween_peddler). Each wants
# emeralds and gives (item, count) up to max_uses times; it buys nothing, so no trade can be run backwards. It
# is a second route to the heirloom and giant pumpkin seeds, the costumes and the sweets, never the only one.
PEDDLER = {"display": "Halloween Peddler", "trade_set": "halloween_peddler", "amount": 4, "trades": {
    "emerald_giant_pumpkin_seeds": {"wants": 4, "gives": ("jugcraft:giant_pumpkin_seeds", 1), "max_uses": 3},
    "emerald_white_pumpkin_seeds": {"wants": 1, "gives": ("jugcraft:white_pumpkin_seeds", 2), "max_uses": 8},
    "emerald_jarrahdale_pumpkin_seeds": {"wants": 1, "gives": ("jugcraft:jarrahdale_pumpkin_seeds", 2), "max_uses": 8},
    "emerald_cinderella_pumpkin_seeds": {"wants": 1, "gives": ("jugcraft:cinderella_pumpkin_seeds", 2), "max_uses": 8},
    "emerald_red_kuri_pumpkin_seeds": {"wants": 1, "gives": ("jugcraft:red_kuri_pumpkin_seeds", 2), "max_uses": 8},
    "emerald_kabocha_pumpkin_seeds": {"wants": 1, "gives": ("jugcraft:kabocha_pumpkin_seeds", 2), "max_uses": 8},
    "emerald_witch_hat": {"wants": 3, "gives": ("jugcraft:witch_hat", 1), "max_uses": 2},
    "emerald_ghost_sheet": {"wants": 3, "gives": ("jugcraft:ghost_sheet", 1), "max_uses": 2},
    "emerald_scarecrow_hat": {"wants": 3, "gives": ("jugcraft:scarecrow_hat", 1), "max_uses": 2},
    "emerald_candle_skull": {"wants": 2, "gives": ("jugcraft:candle_skull", 1), "max_uses": 4},
    "emerald_hanging_ghost": {"wants": 1, "gives": ("jugcraft:hanging_ghost", 2), "max_uses": 6},
    "emerald_glow_gum": {"wants": 1, "gives": ("jugcraft:glow_gum", 2), "max_uses": 8},
    "emerald_ghost_taffy": {"wants": 1, "gives": ("jugcraft:ghost_taffy", 2), "max_uses": 8},
    "emerald_fizz_rocks": {"wants": 1, "gives": ("jugcraft:fizz_rocks", 2), "max_uses": 8},
    "emerald_witchs_licorice": {"wants": 1, "gives": ("jugcraft:witchs_licorice", 2), "max_uses": 8},
}}

# Gravestones (agriculture/GravestoneBlock.java), cut from stone in a stonecutter. `boxes`: the model and shape
# facing north, pixels (x0, y0, z0, x1, y1, z1); `engraving`: how far in front of the centre the engraved face
# is, the height of the text's middle, its width (pixels), at most how many lines, and blocks per font pixel.
# A named Name Tag engraves its name (at most max_length characters); so does renaming the item in an anvil.
GRAVESTONES = {
    "rounded_gravestone": {"display": "Rounded Gravestone", "style": "ROUNDED",
                           "boxes": [(1, 0, 4, 15, 2, 12), (2, 2, 6, 14, 12, 10), (3, 12, 6, 13, 14, 10), (5, 14, 6, 11, 15, 10)],
                           "engraving": [2.0, 7.5, 10.0, 6, "1.0F / 96"]},
    "cross_gravestone": {"display": "Cross Gravestone", "style": "CROSS",
                         "boxes": [(2, 0, 4, 14, 4, 12), (6.5, 4, 6.5, 9.5, 16, 9.5), (2.5, 10, 6.5, 13.5, 13, 9.5)],
                         "engraving": [4.0, 2.0, 11.0, 3, "1.0F / 112"]},
    "obelisk_gravestone": {"display": "Obelisk Gravestone", "style": "OBELISK",
                           "boxes": [(2, 0, 2, 14, 3, 14), (4, 3, 4, 12, 13, 12), (5, 13, 5, 11, 15, 11), (6.5, 15, 6.5, 9.5, 16, 9.5)],
                           "engraving": [4.0, 8.0, 7.0, 6, "1.0F / 112"]},
}
ENGRAVING = {"max_length": 50, "stone": "minecraft:stone"}
# Decorations for any time of year: cobwebs that never slow anyone, a ghost hanging under a block, and a
# skull with a candle that is lit and snuffed like one (light `light` when lit).
FEST_DECOR = {"spun_cobweb": "Spun Cobweb", "hanging_ghost": "Hanging Ghost", "candle_skull": "Candle Skull"}
CANDLE_SKULL = {"light": 12}


# ---------------------------------------------------------------- Halloween nights

# Will-o'-wisps (agriculture/WillOWisp.java, Wisps.java): on Halloween nights (the event running, overworld time
# dusk to dawn), every spawn_ticks the server tries, spawn_chance of the time per player, one spot min..max
# blocks away, open to the sky, over a swamp or within corn_radius of corn; at most near_cap within near_range of a
# player and level_cap in the world. A wisp flees within flee (sneak_flee when sneaking); a glass bottle catches it.
WISPS = {"spawn_ticks": 100, "spawn_chance": 0.5, "min": 10, "max": 32, "corn_radius": 2, "near_cap": 4, "near_range": 48,
         "level_cap": 64, "flee": 6.0, "sneak_flee": 2.5, "jar": "wisp_in_a_jar", "jar_display": "Wisp in a Jar", "jar_light": 13}
# The Harvest Moon (agriculture/HarvestMoon.java): the nights (dusk to dawn) of `day` (halloween.harvest_moon) while
# the event runs; Jugcraft crops grow growth_bonus times as fast and giant pumpkins swell twice as fast.
HARVEST_MOON = {"day": "10-31", "growth_bonus": 2.0, "dusk": 13000, "dawn": 23000, "check_ticks": 100}
# The Pumpkin Chunkin' Trebuchet (agriculture/TrebuchetBlock.java, TrebuchetBlockEntity.java, FlyingPumpkin.java):
# throws one pumpkin (item tag ammo_tag) at base_speed blocks a tick times the pumpkin's factor, plus or minus a
# gust of up to `gust`, at an angle from min_angle to max_angle (default default_angle, steps of angle_step). The
# board keeps the `board` longest throws, ribbons once per player per trebuchet (remembered: the last 64).
TREBUCHET = {"block": "trebuchet", "display": "Pumpkin Chunkin' Trebuchet", "ammo_tag": "jugcraft:trebuchet_ammo", "base_speed": 1.5,
             "gust": 0.04, "min_angle": 30, "max_angle": 60, "angle_step": 5, "default_angle": 45, "board": 3, "remembered": 64,
             "reset_ticks": 30, "max_flight": 200, "advancement_distance": 50.0, "marker_ticks": 600,
             "factors": {"minecraft:pumpkin": 1.0, "minecraft:carved_pumpkin": 1.06, "minecraft:jack_o_lantern": 1.03,
                         "jugcraft:white_pumpkin": 1.02, "jugcraft:jarrahdale_pumpkin": 0.97, "jugcraft:cinderella_pumpkin": 0.95,
                         "jugcraft:hand_carved_pumpkin": 1.06, "jugcraft:hand_carved_white_pumpkin": 1.08,
                         "jugcraft:hand_carved_jarrahdale_pumpkin": 1.03, "jugcraft:hand_carved_cinderella_pumpkin": 1.01,
                         "jugcraft:red_kuri_pumpkin": 0.99, "jugcraft:kabocha_pumpkin": 0.96,
                         "jugcraft:hand_carved_red_kuri_pumpkin": 1.05, "jugcraft:hand_carved_kabocha_pumpkin": 1.02}}
# The Candy Bag holds treats (item tag treat_tag) like a bundle.
CANDY_BAG = {"treat_tag": "jugcraft:candy_bag_treats", "treats": ["#c:foods/candy", "minecraft:cookie", "jugcraft:soul_cake"]}
# The Headless Horseman (agriculture/HeadlessHorseman.java, HorsemanSummoning.java, FlamingPumpkin.java): summoned
# within hour_window ticks of midnight in the event with a lit pumpkin on a scarecrow; fights within arena_radius,
# rides off with nobody within leave_range for lonely_ticks, at dawn, or when the event ends.
HORSEMAN = {"health": 160, "arena_radius": 32, "leave_range": 48, "lonely_ticks": 600, "throw_cooldown": 60, "enraged_throw_cooldown": 30,
            "throw_range": 28.0, "midnight": 18000, "hour_window": 1000, "one_at_a_time": 128, "pumpkin_damage": 6.0, "splash": 2.0,
            "fire_seconds": 3.0, "lantern": "horseman_lantern", "lantern_display": "Horseman's Lantern", "cloak": "horseman_cloak",
            "cloak_display": "Horseman's Cloak", "table": "entities/headless_horseman",
            # (item, count range) dropped when a player defeats him
            "loot": [("jugcraft:horseman_lantern", [1, 1]), ("jugcraft:horseman_cloak", [1, 1]), ("jugcraft:king_size_candy_bar", [1, 2]),
                     ("jugcraft:candy_corn", [3, 6])]}


# ---------------------------------------------------------------- Halloween decorations, batch 1
# Jack-o'-Lantern String Lights (agriculture/StringLightHookBlock.java, StringLightHookBlockEntity.java,
# StringLightsItem.java): the strand item strings one hook to another at most max_length blocks away (one strand from
# each hook). A hook lights (light level `light`) from a redstone signal, or from the electric network at `use` JE a
# tick (buffer `capacity`, taking up to `input` a tick). A strand glows while either of its hooks is lit. Every
# check_ticks a hook drops a strand whose far hook is gone.
STRING_LIGHTS = {"hook": "string_light_hook", "hook_display": "String Light Hook", "strand": "jack_o_lantern_string_lights",
                 "strand_display": "Jack-o'-Lantern String Lights", "max_length": 16, "light": 10, "use": 1, "capacity": 200,
                 "input": 20, "check_ticks": 100}
# The Candy Bowl (CandyBowlBlock, CandyBowlBlockEntity): holds up to `capacity` treats (CANDY_BAG's treat tag). Anyone
# may add treats; each visitor takes one a night (the trick-or-treat night), its owner (whoever placed it) any time.
# It remembers the last `visitors` visitors. Its look shows how full it is: `fill` are the counts for levels 1-3.
CANDY_BOWL = {"block": "candy_bowl", "display": "Candy Bowl", "capacity": 64, "visitors": 256, "fill": [1, 16, 48]}
# The Coffin (CoffinBlock, a bed; CoffinBlockEntity on its head half): `slots` slots behind the lid; sneak-use to lie
# down and set your spawn, like a bed.
COFFIN = {"block": "coffin", "display": "Coffin", "slots": 27}
# The Haunted Portrait (HauntedPortraitBlock + client HauntedPortraitRenderer): a framed painting in four portraits
# whose pupils follow whoever looks at it. Each eye is (x, y, width, height) in the portrait's 16x16 texture.
HAUNTED_PORTRAIT = {"block": "haunted_portrait", "display": "Haunted Portrait",
                    "portraits": {"lady": [(6, 6, 2, 1), (9, 6, 2, 1)], "captain": [(5, 6, 2, 1), (9, 6, 2, 1)],
                                  "cat": [(5, 7, 2, 2), (9, 7, 2, 2)], "owl": [(4, 5, 3, 3), (9, 5, 3, 3)]}}
# The Fog Machine (FogMachineBlock, FogMachineBlockEntity, client FogParticle): switched on by hand or by redstone, it
# runs at `use` JE a tick (buffer `capacity`, taking up to `input` a tick) and rolls ground fog over a radius from
# `radii` (sneak-use to change). Fog is drawn only by clients near it: at most particles_per_tick a machine, and
# `budget` a tick for all machines together.
FOG_MACHINE = {"block": "fog_machine", "display": "Fog Machine", "use": 16, "capacity": 4000, "input": 64, "radii": [4, 8, 12, 16],
               "particles_per_tick": 6, "budget": 24, "view": 48}


def decor1_blocks():
    return [STRING_LIGHTS["hook"], CANDY_BOWL["block"], COFFIN["block"], HAUNTED_PORTRAIT["block"], FOG_MACHINE["block"]]


def decor1_items():
    return decor1_blocks() + [STRING_LIGHTS["strand"]]


# ---------------------------------------------------------------- Halloween decorations, batch 2
# The Luminaria (agriculture/LuminariaBlock.java): a paper bag weighted with sand round a candle, a jack-o'-lantern
# face cut in its sides. Lit like a candle (flint and steel or a fire charge) it gives `light`; an empty hand snuffs
# it. A dye colours the paper (any of DYE_COLORS); the bag keeps its colour when broken.
LUMINARIA = {"block": "luminaria", "display": "Luminaria", "light": 10}
# Floating Candles (FloatingCandleBlock, client FloatingCandleRenderer): 1 to `max` candles hanging in the air where
# they are placed, bobbing `bob` pixels every `bob_ticks` ticks; `light_per_candle` light each while lit, like
# vanilla candles. Each candle's place (x, z, bottom) and height in pixels: `candles` and `heights`.
FLOATING_CANDLE = {"block": "floating_candle", "display": "Floating Candle", "max": 4, "light_per_candle": 3, "bob": 1.0, "bob_ticks": 80,
                   "candles": [[(8, 8, 6)], [(6, 8, 7), (10, 8, 5)], [(8, 6, 7.5), (5.5, 10, 5), (10.5, 10, 6)],
                               [(6, 6, 6), (10, 6, 7.5), (6, 10, 8), (10, 10, 5)]],
                   "heights": [5, 4, 6, 4.5]}
# The Skeleton Hand Sconce (SkeletonHandSconceBlock): a torch held out from a wall by a bony hand; placed burning
# (light `light`); an empty hand snuffs it, flint and steel or a fire charge lights it again.
SCONCE = {"block": "skeleton_hand_sconce", "display": "Skeleton Hand Sconce", "light": 14}
# Soul-Flame Carvings: a soul torch lights any hand-carved or giant pumpkin with a blue glow, giving the carving's
# glow but at most `light` (a soul torch's own); taking it out gives the soul torch back.
SOUL_CARVING = {"light": 10}
# Bat Bunting (StringLightsItem with the BUNTING strand): strung between String Light Hooks like the string lights,
# by the same rules; orange and black pennants and paper bats on a twine cord. It gives no light.
BAT_BUNTING = {"item": "bat_bunting", "display": "Bat Bunting"}


def decor2_blocks():
    return [LUMINARIA["block"], FLOATING_CANDLE["block"], SCONCE["block"]]


def decor2_items():
    return decor2_blocks() + [BAT_BUNTING["item"]]


# ---------------------------------------------------------------- Halloween decorations, batch 3: the graveyard
# A wrought-iron cemetery fence and gate (vanilla FenceBlock and FenceGateBlock; tags minecraft:fences and
# fence_gates, so they join other non-wooden fences, gates and walls).
CEMETERY_FENCE = {"fence": "cemetery_fence", "fence_display": "Wrought-Iron Cemetery Fence", "gate": "cemetery_gate",
                  "gate_display": "Wrought-Iron Cemetery Gate"}
# The crypt set: crypt stone, its chiseled form and pillar (stonecutter), and a stone Crypt Door that opens by hand.
CRYPT = {"stone": "crypt_stone", "stone_display": "Crypt Stone", "chiseled": "chiseled_crypt_stone", "chiseled_display": "Chiseled Crypt Stone",
         "pillar": "crypt_stone_pillar", "pillar_display": "Crypt Stone Pillar", "door": "crypt_door", "door_display": "Crypt Door"}
# Scare props (agriculture/ScareProp.java, ScarePropBlockEntity.java): a player within `reach` blocks, not sneaking,
# sets one off for `up_ticks`; then it rests `cooldown_ticks` before it can go again. A redstone signal holds it up.
# The block entity looks every `period` ticks.
SCARE_PERIOD = 10
GRAVE_MOUND = {"block": "grave_mound", "display": "Grave Mound", "reach": 3.0, "up_ticks": 60, "cooldown_ticks": 100}
POP_UP_SKELETON = {"block": "pop_up_skeleton", "display": "Pop-Up Skeleton", "reach": 2.5, "up_ticks": 40, "cooldown_ticks": 100}
# The Mourning Angel (MourningAngelBlock, a two-block TallDecorationBlock): weeps at night (client particles).
MOURNING_ANGEL = {"block": "mourning_angel", "display": "Mourning Angel"}


def decor3_blocks():
    return [CEMETERY_FENCE["fence"], CEMETERY_FENCE["gate"], CRYPT["stone"], CRYPT["chiseled"], CRYPT["pillar"], CRYPT["door"],
            GRAVE_MOUND["block"], MOURNING_ANGEL["block"], POP_UP_SKELETON["block"]]


def decor3_items():
    return decor3_blocks()


# ---------------------------------------------------------------- Halloween decorations, batch 4: the witch's cottage
# The Bubbling Cauldron (BubblingCauldronBlock): water from a bucket, then a brew ingredient (item tags
# jugcraft:brew/<colour>) makes a brew that glows (`light`); over one of the Cooking Pot's heat sources it bubbles.
CAULDRON = {"block": "bubbling_cauldron", "display": "Bubbling Cauldron", "light": 7,
            "brews": {"green": ["minecraft:spider_eye", "minecraft:fermented_spider_eye", "minecraft:slime_ball"],
                      "purple": ["minecraft:nether_wart", "minecraft:chorus_fruit", "minecraft:amethyst_shard"],
                      "orange": ["minecraft:glowstone_dust", "minecraft:blaze_powder", "minecraft:magma_cream"]}}
# Hex brewing (fall addition 21; BubblingCauldronBlock, Hexes, HexDraughtItem): a brew bubbling over a heat source
# turns into a hex brew when its hex ingredient (item tags jugcraft:hex/<hex>) is stirred in; a glass bottle draws one
# of its `doses`. The Shrinking Draught and Giant's Draught scale the drinker (attribute minecraft:scale, `scale` added
# to 1 times the base) for `seconds`; the giant also steps higher and reaches further. A giant's draught is only drunk
# with room to grow; a shrunk player without room to grow back stays small `room_extend_ticks` longer, again and again,
# so nobody wakes up inside a wall. Flying Ointment, rubbed on, gives Slow Falling for `seconds` (and fuels a broom).
HEX = {"doses": 3, "room_extend_ticks": 100,
       "brews": {"shrinking": {"brew": "green", "ingredients": ["minecraft:brown_mushroom"], "item": "shrinking_draught",
                               "display": "Shrinking Draught", "effect": "shrunk", "effect_display": "Shrunk", "scale": -0.5, "seconds": 180},
                 "giant": {"brew": "orange", "ingredients": ["jugcraft:beans"], "item": "giants_draught", "display": "Giant's Draught",
                           "effect": "giant", "effect_display": "Giant", "scale": 0.6, "step": 0.5, "reach": 1.0, "seconds": 180},
                 "flying": {"brew": "purple", "ingredients": ["minecraft:phantom_membrane", "jugcraft:mandrake_root"], "item": "flying_ointment",
                            "display": "Flying Ointment", "seconds": 30}}}
# The flying broomstick (fall addition 22; Broomstick, FlyingBroomstickItem, client/BroomstickClient): a witch's broom
# anointed with Flying Ointment, ridden and steered by looking. Each ointment is `charge_per_ointment` ticks of flight,
# up to `max_charge`, burnt while ridden off the ground. Forward pushes `accel` a tick along the look, strafe `strafe`
# sideways, jump `climb` up; `drag` of the speed is kept each tick (`brake` of that when braking); at most `max_speed`
# blocks a tick, times `hat_bonus` in a witch hat. Dry, it sinks at `sink`. The server checks every `check_ticks` ticks
# that it went no further than its top speed allows (`tolerance` times, for lag) and, dry, climbed no more than
# `dry_climb`; getting off in the air gives `slow_fall_ticks` of slow falling.
BROOMSTICK = {"item": "flying_broomstick", "display": "Flying Broomstick", "charge_per_ointment": 2400, "max_charge": 7200,
              "accel": 0.05, "strafe": 0.025, "climb": 0.04, "drag": 0.91, "brake": 0.8, "max_speed": 0.6, "hat_bonus": 1.25,
              "sink": 0.08, "check_ticks": 20, "tolerance": 3.0, "dry_climb": 2.0, "low_charge": 400, "slow_fall_ticks": 100,
              "moon_height": 48, "inputs": ["jugcraft:witchs_broom", "jugcraft:flying_ointment", "minecraft:feather", "minecraft:feather"]}
# Full-moon werewolves (fall addition 23; Werewolf, Werewolves, client/WerewolfModel): a werewolf comes out of the woods
# (biome tag jugcraft:werewolf_haunts) only on full-moon nights and is gone at dawn. Every `spawn_ticks`, for each player,
# `spawn_chance` of the time a spot `min_distance` to `max_distance` blocks off is tried; fewer than `near_cap` near a
# player, `level_cap` in all. Its hide takes `hide_factor` of any damage but silver's, which does `silver_factor`; it heals
# a point every `regen_ticks` unless silver wounded it in the last `silver_wound_ticks`. Wolfsbane wards it off (held, or
# within `ward_reach` blocks); a warded target is left alone `shun_ticks`. Silver: a dagger and arrows; its pelt makes a rug.
WEREWOLF = {"entity": "werewolf", "display": "Werewolf", "size": [0.9, 2.4], "hide_factor": 0.5, "silver_factor": 2.5,
            "regen_ticks": 40, "silver_wound_ticks": 100, "shun_ticks": 200, "spawn_ticks": 200, "spawn_chance": 0.3,
            "min_distance": 24, "max_distance": 40, "near_cap": 2, "level_cap": 8, "ward_reach": 6,
            "haunts": ["#minecraft:is_forest", "#minecraft:is_taiga", "minecraft:grove"],
            # Which kind comes out: a snow werewolf in snowy biomes; a shadow werewolf `shadow_haunt_chance` of the time
            # in its haunts and `shadow_chance` elsewhere; otherwise a brown one.
            "snow_haunts": ["#c:is_snowy"],
            "shadow_haunts": ["minecraft:dark_forest", "minecraft:pale_garden", "jugcraft:gloomweald", "jugcraft:ghost_forest"],
            "shadow_chance": 0.08, "shadow_haunt_chance": 0.5,
            # Three kinds, three tiers of danger: their attributes, the experience they give and what they drop.
            "kinds": {
                "brown": {"tier": 1, "display": "Brown Werewolf", "health": 40.0, "damage": 6.0, "armor": 2.0, "speed": 0.33,
                          "scale": 1.0, "knockback": 0.3, "xp": 10, "pelt": "werewolf_pelt", "rug": "werewolf_rug"},
                "snow": {"tier": 2, "display": "Snow Werewolf", "health": 50.0, "damage": 8.0, "armor": 4.0, "speed": 0.32,
                         "scale": 1.05, "knockback": 0.4, "xp": 20, "pelt": "snow_werewolf_pelt", "rug": "snow_werewolf_rug"},
                "shadow": {"tier": 3, "display": "Shadow Werewolf", "health": 80.0, "damage": 11.0, "armor": 6.0, "speed": 0.36,
                           "scale": 1.15, "knockback": 0.6, "xp": 40, "pelt": "shadow_werewolf_pelt", "rug": "shadow_werewolf_rug"}},
            # The brown werewolf: hunts livestock too (`prey`); its howl calls brown werewolves within `pack_reach` to its
            # prey; below `flee_below` of its health it flees until healed to `flee_until`.
            "prey": ["minecraft:sheep", "minecraft:pig", "minecraft:cow", "minecraft:chicken", "minecraft:rabbit"],
            "pack_reach": 32, "flee_below": 0.25, "flee_until": 0.5,
            # The snow werewolf: its bite slows (Slowness II) for `frostbite_ticks` and chills by `frostbite_chill` frozen
            # ticks; it never freezes; on snow it is `snow_stride` faster.
            "frostbite_ticks": 60, "frostbite_chill": 80, "snow_stride": 0.25,
            # The shadow werewolf: steps out of the shadows behind prey at least `shadow_step_min` blocks off, every
            # `shadow_step_ticks`; its howl darkens the night for players within `alpha_reach` for `darkness_ticks` and
            # sends werewolves within `frenzy_reach` into a frenzy (Strength and Speed) for `frenzy_ticks`. A sprig in
            # hand doesn't ward it.
            "shadow_step_min": 6, "shadow_step_ticks": 200, "alpha_reach": 16, "darkness_ticks": 160, "frenzy_reach": 24,
            "frenzy_ticks": 200, "howl_cooldown": 600,
            "dagger": "silver_dagger", "arrow": "silver_arrow",
            "dagger_material": {"durability": 200, "speed": 6.0, "bonus": 1.5, "enchantability": 20, "damage": 2.5, "attack_speed": -1.8},
            "displays": {"werewolf_pelt": "Werewolf Pelt", "werewolf_rug": "Werewolf Rug", "snow_werewolf_pelt": "Snow Werewolf Pelt",
                         "snow_werewolf_rug": "Snow Werewolf Rug", "shadow_werewolf_pelt": "Shadow Werewolf Pelt",
                         "shadow_werewolf_rug": "Shadow Werewolf Rug", "silver_dagger": "Silver Dagger", "silver_arrow": "Silver Arrow"}}
WEREWOLF_PELTS = [k["pelt"] for k in WEREWOLF["kinds"].values()]
WEREWOLF_RUGS = [k["rug"] for k in WEREWOLF["kinds"].values()]
# Wolfsbane (fall addition 23): a wild flower of taiga and forest (new chunks), plantable and potted, that wards werewolves
# off; in suspicious stew it poisons for `seconds`.
WOLFSBANE = {"block": "wolfsbane", "display": "Wolfsbane", "effect": "POISON", "seconds": 8.0, "biomes": ["IS_TAIGA", "IS_FOREST"],
             "patch": {"rarity": 20, "tries": 24, "spread_xz": 5, "spread_y": 2}}
# Squirrels and acorns (fall addition 24; Squirrel, Squirrels, AcornItem, client/SquirrelModel): squirrels, red or grey,
# come to the woods (biome tag jugcraft:squirrel_habitat) by day; every `spawn_ticks`, for each player, `spawn_chance` of
# the time a spot `min_distance` to `max_distance` blocks off is tried; fewer than `near_cap` near a player, `level_cap`
# in all. They climb logs, are tempted and bred by nuts (jugcraft:squirrel_food), and gather acorns lying within
# `seek_range`, burying each after `bury_ticks`; `sapling_chance` of buried acorns sprout as oak saplings (none within
# `sapling_space` of another sapling or a log). Oak and dark oak leaves drop an acorn `acorn_chance` of the time.
SQUIRRELS = {"entity": "squirrel", "display": "Squirrel", "health": 6, "speed": 0.32, "size": [0.4, 0.5], "spawn_ticks": 400,
             "spawn_chance": 0.25, "min_distance": 20, "max_distance": 40, "near_cap": 6, "level_cap": 30, "seek_range": 10,
             "bury_ticks": 200, "sapling_chance": 0.25, "sapling_space": 3, "acorn_chance": 0.05,
             "acorn_leaves": ["oak_leaves", "dark_oak_leaves"], "acorn": "acorn", "acorn_display": "Acorn", "roasted": "roasted_acorns",
             "habitat": ["#minecraft:is_forest", "#minecraft:is_taiga"],
             "food": ["jugcraft:acorn", "jugcraft:chestnut", "jugcraft:roasted_chestnuts"]}
# The Pumpkling (Pumpkling): a hand-carved pumpkin woken by a spark into a pet; follows past `follow_start`, stops at
# `follow_stop`, comes to its owner past `teleport`; treats heal `treat_heal`.
PUMPKLING = {"entity": "pumpkling", "display": "Pumpkling", "health": 16, "speed": 0.3, "size": [0.6, 0.9],
             "follow_start": 6.0, "follow_stop": 2.5, "teleport": 16.0, "treat_heal": 4.0, "tempt_range": 10.0,
             "sparks": ["jugcraft:wisp_in_a_jar", "jugcraft:ectoplasm"],
             "treats": ["minecraft:pumpkin_seeds", "jugcraft:roasted_pumpkin_seeds", "minecraft:pumpkin_pie", "jugcraft:candy_corn"]}
# The Apothecary Shelf (ApothecaryShelfBlock): wall shelves of jars; sneak-use cycles `arrangements` ways to set them.
APOTHECARY_SHELF = {"block": "apothecary_shelf", "display": "Apothecary Shelf", "arrangements": 4}
# The Crystal Ball (CrystalBallBlock): glows `light`; gazing flares it to `gazing_light` for `gaze_ticks` and tells a
# fortune (one of these, in order: message.jugcraft.crystal_ball.fortune.<n>).
CRYSTAL_BALL = {"block": "crystal_ball", "display": "Crystal Ball", "light": 6, "gazing_light": 12, "gaze_ticks": 40,
                "fortunes": ["A pumpkin is in your future. Possibly several.",
                             "Beware of creepers bearing gifts.",
                             "Something will follow you home tonight. It is probably a cat.",
                             "The mists say: water your crops.",
                             "A great treasure lies beneath your feet. Dig carefully.",
                             "You will meet a tall, dark stranger. It will be an Enderman.",
                             "Your next knock on a door will bring a treat.",
                             "The Horseman rides closer than you think.",
                             "Bones will rattle where you least expect them.",
                             "The mists are cloudy. Ask again after supper."]}
# The Grimoire Stand (GrimoireStandBlock): an open spellbook (light `light`); using it turns to the next spread.
GRIMOIRE = {"block": "grimoire_stand", "display": "Grimoire Stand", "light": 3,
            "spreads": {"moons": "Almanac of Moons", "bats": "A Treatise on Bats", "brew": "Brew of the Seven Shadows",
                        "pumpkin": "Charm for a Lantern Pumpkin"}}
# The Witch's Broom (WitchsBroomBlock): a besom leaning on its bristles.
BROOM = {"block": "witchs_broom", "display": "Witch's Broom"}


def decor4_blocks():
    return [CAULDRON["block"], APOTHECARY_SHELF["block"], CRYSTAL_BALL["block"], GRIMOIRE["block"], BROOM["block"]]


def decor4_items():
    return decor4_blocks()


# ---------------------------------------------------------------- Halloween decorations, batch 5: the harvest party
# The Bobbing for Apples Tub (BobbingTubBlock): holds up to `max_apples` apples; one try in `chance` catches one, then
# the water splashes `splash_ticks` before the next try.
BOBBING_TUB = {"block": "bobbing_tub", "display": "Bobbing for Apples Tub", "max_apples": 4, "chance": 3, "splash_ticks": 20,
               "messages": {"empty": "There are no apples in the tub.", "caught": "Got one! You caught an apple in your teeth.",
                            "missed": "Splash! The apple bobs away."}}
# The Pumpkin Crate (PumpkinCrateBlock + PumpkinCrateBlockEntity): shows up to `capacity` pieces of produce (item tag
# jugcraft:crate_produce).
PUMPKIN_CRATE = {"block": "pumpkin_crate", "display": "Pumpkin Crate", "capacity": 4, "produce_tag": "jugcraft:crate_produce",
                 "produce": ["minecraft:pumpkin", "minecraft:melon", "#c:crops/pumpkin", "#c:crops/squash", "#c:crops/gourd"]}
# The Hay Bale Seat (HayBaleSeatBlock): sat on at `height` blocks; softens falls like a hay block.
HAY_BALE_SEAT = {"block": "hay_bale_seat", "display": "Hay Bale Seat", "height": 0.625, "fall_softening": 0.8, "entity": "chair_seat"}
# The Autumn Wreath (AutumnWreathBlock): chestnut leaves, ornamental corn and mums; a mum swaps its flowers.
AUTUMN_WREATH = {"block": "autumn_wreath", "display": "Autumn Wreath", "flowers": ["yellow", "orange", "red", "purple"],
                 "default": "orange"}
# Leaf Piles (LeafPileBlock): one block a colour, heaped `max_layers` layers of `layer_pixels`; each layer softens a
# fall by `softening_per_layer`.
LEAF_PILES = {"colours": {"red": "Red Leaf Pile", "orange": "Orange Leaf Pile", "yellow": "Yellow Leaf Pile"}, "max_layers": 4,
              "layer_pixels": 4, "softening_per_layer": 0.2, "scatter_chance": 4}


def leaf_piles():
    return [f"{colour}_leaf_pile" for colour in LEAF_PILES["colours"]]


def decor5_blocks():
    return [BOBBING_TUB["block"], PUMPKIN_CRATE["block"], HAY_BALE_SEAT["block"], AUTUMN_WREATH["block"]] + leaf_piles()


def decor5_items():
    return decor5_blocks()


# ---------------------------------------------------------------- Halloween decorations, batch 6: the haunted house and yard
# The Rocking Chair (RockingChairBlock): sat in at `seat_height`; rocks `sitter_rock` degrees under a sitter and
# `haunted_rock` on its own at night, one rock every `rock_period` ticks (drawn by the client).
ROCKING_CHAIR = {"block": "rocking_chair", "display": "Rocking Chair", "seat_height": 0.5625, "haunted_rock": 7.0, "sitter_rock": 4.0,
                 "rock_period": 50}
# Lurking Eyes (LurkingEyesBlock): show at night to viewers at least `hide_distance` away; blink every `blink_period`.
LURKING_EYES = {"block": "lurking_eyes", "display": "Lurking Eyes", "hide_distance": 4.0, "blink_period": 90, "blink_ticks": 4}
# The Silhouette Window (SilhouetteWindowBlock): a cut-out per design; glows on the side away from block light of at
# least `glow_light`.
SILHOUETTE_WINDOW = {"block": "silhouette_window", "display": "Silhouette Window", "designs": ["bat", "cat", "witch"], "glow_light": 8}
# The Spooky Music Box (MusicBoxBlock + MusicBoxBlockEntity): an original tune of `beats` beats of `ticks_per_beat`.
MUSIC_BOX = {"block": "music_box", "display": "Spooky Music Box", "ticks_per_beat": 6, "beats": 24}
# The Giant Fake Spider (GiantFakeSpiderBlock): dangles 1 to `max_drop` blocks, swaying `sway_degrees` every `sway_period`.
GIANT_FAKE_SPIDER = {"block": "giant_fake_spider", "display": "Giant Fake Spider", "max_drop": 4, "sway_period": 120, "sway_degrees": 6.0}


def decor6_blocks():
    return [ROCKING_CHAIR["block"], LURKING_EYES["block"], SILHOUETTE_WINDOW["block"], MUSIC_BOX["block"], GIANT_FAKE_SPIDER["block"]]


def decor6_items():
    return decor6_blocks()


# ---------------------------------------------------------------- Halloween decorations, batch 7: the haunted house inside
# The Haunted Chandelier (HauntedChandelierBlock): hangs under a block (or a chain) with `candles` candles, lit by flint
# and steel or a fire charge, giving 3 light for every two lit candles. At night a lit chandelier gets a random tick's
# one-in-`gust_chance` gust that snuffs every candle; they relight themselves one every `relight_ticks`. The client
# sways it `sway_degrees` every `sway_period` ticks.
HAUNTED_CHANDELIER = {"block": "haunted_chandelier", "display": "Haunted Chandelier", "candles": 8, "gust_chance": 3, "relight_ticks": 15,
                      "sway_degrees": 3.0, "sway_period": 160}
# The Phantom Pipe Organ (PipeOrganBlock + PipeOrganBlockEntity): `width` blocks wide and `height` tall, one prop. A
# click or a rising redstone signal plays the opening of Bach's Toccata and Fugue in D minor (public domain), arranged
# here for note-block sounds over `tune_ticks` ticks; the keys go down by themselves. At night a random tick plays it
# one time in `phantom_chance`.
PIPE_ORGAN = {"block": "phantom_pipe_organ", "display": "Phantom Pipe Organ", "width": 3, "height": 2, "tune_ticks": 200, "phantom_chance": 4}
# The Suit of Armor (SuitOfArmorBlock, a two-block TallDecorationBlock): its helmet turns, at most `turn_speed` degrees
# a tick and `max_turn` either way, toward the nearest player within `watch_range` blocks (drawn by the client).
SUIT_OF_ARMOR = {"block": "suit_of_armor", "display": "Suit of Armor", "watch_range": 10.0, "turn_speed": 4.0, "max_turn": 75.0}
# The Dust Sheet (DustSheetItem, DustSheetBlock + DustSheetBlockEntity): covers a block of the block tag `tag`, keeping
# it (and its contents) under the sheet; an empty hand pulls it off. One sheet in `breathe_chance` seems to breathe at
# night (drawn by the client).
DUST_SHEET = {"block": "dust_sheet", "item": "dust_sheet", "display": "Dust Sheet", "tag": "jugcraft:dust_sheet_coverable", "breathe_chance": 3,
              "coverable": ["#minecraft:stairs", "#minecraft:slabs", "minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel",
                            "minecraft:crafting_table", "minecraft:bookshelf", "minecraft:chiseled_bookshelf", "minecraft:loom",
                            "minecraft:cartography_table", "minecraft:fletching_table", "minecraft:smithing_table", "minecraft:note_block",
                            "jugcraft:rocking_chair", "jugcraft:hay_bale_seat", "jugcraft:crystal_ball", "jugcraft:grimoire_stand",
                            "jugcraft:creepy_doll", "jugcraft:spirit_mirror"]}
# The Spirit Mirror (SpiritMirrorBlock): at night a pale face shows in the glass for `visible` ticks in every `period`
# (fading in and out over `fade`), to viewers in front of it within `range` blocks.
SPIRIT_MIRROR = {"block": "spirit_mirror", "display": "Spirit Mirror", "period": 600, "visible": 80, "fade": 20, "range": 8.0}
# Tattered Curtains (TatteredCurtainsBlock): stack into a drape of up to `max_drop` blocks that opens and closes as one;
# closed, it sways `sway` pixels at the hem (`night_sway` at night) every `sway_period` ticks.
TATTERED_CURTAINS = {"block": "tattered_curtains", "display": "Tattered Curtains", "max_drop": 8, "sway": 1.0, "night_sway": 2.0,
                     "sway_period": 70}
# The Creepy Doll (CreepyDollBlock): each time a viewer looks back after `unseen_ticks` without seeing it, its head has
# turned: toward them, or one time in `elsewhere_chance` somewhere else (drawn by the client).
CREEPY_DOLL = {"block": "creepy_doll", "display": "Creepy Doll", "unseen_ticks": 10, "elsewhere_chance": 3}


def decor7_blocks():
    return [HAUNTED_CHANDELIER["block"], PIPE_ORGAN["block"], SUIT_OF_ARMOR["block"], DUST_SHEET["block"], SPIRIT_MIRROR["block"],
            TATTERED_CURTAINS["block"], CREEPY_DOLL["block"]]


def decor7_items():
    return decor7_blocks()


# ---------------------------------------------------------------- Halloween decorations, batch 8: the mad scientist and monsters
# The Tesla Coil (TeslaCoilBlock + TeslaCoilBlockEntity, two blocks tall): switched on and holding `use` JE (buffer
# `capacity`, `input` a tick in) it hums, lit `light`, using `use` JE a tick; every `arc_min` to `arc_min` + `arc_spread`
# ticks it throws a harmless arc to another running coil within `range` blocks (or into the air), drawn for `arc_ticks`.
TESLA_COIL = {"block": "tesla_coil", "display": "Tesla Coil", "use": 20, "capacity": 4000, "input": 64, "range": 8, "arc_min": 15,
              "arc_spread": 25, "arc_ticks": 6, "light": 8}
# The Lab Table (LabTableBlock, two blocks long): its sheeted patient sits up `sit_degrees` (at `sit_speed` a tick) while
# the table has a redstone signal, and twitches at night for `twitch_ticks` of every `twitch_period` (drawn by the client).
LAB_TABLE = {"block": "lab_table", "display": "Lab Table", "sit_degrees": 70.0, "sit_speed": 6.0, "twitch_period": 97, "twitch_ticks": 4}
# The Specimen Jar (SpecimenJarBlock): glowing green fluid (light `light`) with one of `specimens` floating in it,
# bobbing `bob` pixels every `bob_ticks`; sneak-use changes the specimen.
SPECIMEN_JAR = {"block": "specimen_jar", "display": "Specimen Jar", "specimens": ["eye", "tentacle", "pumpkin", "brain"], "light": 7,
                "bob": 0.75, "bob_ticks": 90}
# The Mummy Sarcophagus (MummySarcophagusBlock, two blocks tall): a click or a rising redstone signal opens it for
# `open_ticks`: the lid swings `lid_degrees` and the mummy lurches `lurch` pixels out (drawn by the client).
SARCOPHAGUS = {"block": "mummy_sarcophagus", "display": "Mummy Sarcophagus", "open_ticks": 80, "lid_degrees": 100.0, "lurch": 5.0}
# The Raven on a Perch (RavenPerchBlock): turns its head up to `max_turn` toward the nearest player within
# `watch_range`; ruffles its feathers for `ruffle_ticks` of every `ruffle_period`; caws and flaps when used.
RAVEN = {"block": "raven_perch", "display": "Raven on a Perch", "watch_range": 8.0, "max_turn": 90.0, "ruffle_period": 140, "ruffle_ticks": 12}
# The Black Cat Figure (BlackCatBlock + BlackCatBlockEntity): swishes its tail `swish_degrees` every `swish_period`;
# its eyes glow at night; a sprinting player within `reach` blocks makes it hiss, back arched, for `hiss_ticks`, then
# it rests `cooldown_ticks`.
BLACK_CAT = {"block": "black_cat_figure", "display": "Black Cat Figure", "reach": 3.0, "hiss_ticks": 30, "cooldown_ticks": 60,
             "swish_period": 50, "swish_degrees": 25.0}


def decor8_blocks():
    return [TESLA_COIL["block"], LAB_TABLE["block"], SPECIMEN_JAR["block"], SARCOPHAGUS["block"], RAVEN["block"], BLACK_CAT["block"]]


def decor8_items():
    return decor8_blocks()


# ---------------------------------------------------------------- Halloween decorations, batch 9: the yard and porch
# Yard Inflatables (InflatableBlock + DecorationBlockEntity, two blocks tall, one block per design): switched on by hand
# or by a redstone signal, the blower fills the figure over `inflate_ticks`, it glows from inside (light `light`) and
# wobbles `wobble_degrees`; switched off, it sags flat over `deflate_ticks` (drawn by the client).
INFLATABLES = {"designs": ["ghost", "cat", "pumpkin", "spider"], "display": {"ghost": "Inflatable Ghost", "cat": "Inflatable Black Cat",
                                                                               "pumpkin": "Inflatable Pumpkin Stack", "spider": "Inflatable Spider"},
               "inflate_ticks": 40, "deflate_ticks": 60, "light": 7, "wobble_degrees": 3.0, "wobble_period": 45}
# The Porch Witch (PorchWitchBlock + PorchWitchBlockEntity, two blocks tall): stirs her pot slowly; when someone walks up
# within `reach` blocks after nobody was there, she cackles and stirs hard for `cackle_ticks`, then won't again for
# `cooldown_ticks`. Her head follows the nearest player within `watch_range`.
PORCH_WITCH = {"block": "porch_witch", "display": "Animatronic Porch Witch", "reach": 4.0, "cackle_ticks": 40, "cooldown_ticks": 200,
               "stir_period": 60, "fast_stir_period": 16, "watch_range": 8.0}
# Grasping Hands (GraspingHandsBlock): something stepping on them (not sneaking) is grabbed by the ankle: Slowness
# `slowness_level` for `slow_ticks`; the hands stay up `grab_ticks`, then sink back and rest `rest_ticks`.
GRASPING_HANDS = {"block": "grasping_hands", "display": "Grasping Hands", "slow_ticks": 30, "slowness_level": 2, "grab_ticks": 15,
                  "rest_ticks": 40}
# The Poseable Skeleton (PoseableSkeletonBlock, two blocks tall): use it to pose it: sitting, waving, lounging, hanging.
POSEABLE_SKELETON = {"block": "poseable_skeleton", "display": "Poseable Skeleton", "poses": ["sitting", "waving", "lounging", "hanging"]}
# Bone Wind Chimes (BoneWindChimesBlock + DecorationBlockEntity): hang under a block; `bones` bones swing `calm_swing`
# degrees, up to `storm_swing` in a thunderstorm, and clack now and then (more often and louder in rain and storms).
WIND_CHIMES = {"block": "bone_wind_chimes", "display": "Bone Wind Chimes", "bones": 5, "calm_swing": 4.0, "rain_swing": 12.0,
               "storm_swing": 28.0, "calm_chance": 12, "rain_chance": 4, "storm_chance": 1}
# Weathervanes (WeathervaneBlock + DecorationBlockEntity, one block per design): the vane turns to the wind, the same in
# the whole world (Weathervane.wind), at most `turn_speed` degrees a tick; storms make it swing about.
WEATHERVANES = {"designs": ["bat", "witch"], "display": {"bat": "Bat Weathervane", "witch": "Witch Weathervane"}, "turn_speed": 3.0}
# The Spooky Sign (SpookySignBlock + SpookySignBlockEntity): painted `words` (use to change them) or, like a gravestone,
# your own words from a named Name Tag or an anvil, at most `max_length` characters.
SPOOKY_SIGN = {"block": "spooky_sign", "display": "Spooky Sign", "words": ["beware", "keep_out", "turn_back", "go_away", "no_trespassing",
                                                                            "abandon_hope"], "max_length": 50}
# The Haunted Archway (HauntedArchwayBlock, a multi-block MultiDecorationBlock): two stone pillars and an iron arch,
# `width` blocks wide and `height` tall, with a lantern hanging on each side (light `light`), lit or put out by hand.
HAUNTED_ARCHWAY = {"block": "haunted_archway", "display": "Haunted Archway", "width": 3, "height": 3, "light": 14}
# The Dead Hollow Tree (DeadHollowTreeBlock, a multi-block MultiDecorationBlock): a dead trunk `height` blocks tall with
# bare branches, lanterns hanging from two of them (light `light`), a hollow at its foot and a face in its bark.
DEAD_TREE = {"block": "dead_hollow_tree", "display": "Dead Hollow Tree", "height": 4, "light": 13}


def inflatable(design):
    return f"inflatable_{design}"


def weathervane(design):
    return f"{design}_weathervane"


def decor9_blocks():
    return ([inflatable(d) for d in INFLATABLES["designs"]] + [PORCH_WITCH["block"], GRASPING_HANDS["block"], POSEABLE_SKELETON["block"],
                                                              WIND_CHIMES["block"]]
            + [weathervane(d) for d in WEATHERVANES["designs"]] + [SPOOKY_SIGN["block"], HAUNTED_ARCHWAY["block"], DEAD_TREE["block"]])


def decor9_items():
    return decor9_blocks()


# ---------------------------------------------------------------- Halloween decorations, batch 10: lighting and glow
# The Black Light (BlackLightBlock + BlackLightBlockEntity, on a wall): switched on by hand or redstone it glows purple
# (light `light`) and lights up Glow Paint within `range` blocks (drawn by the client).
BLACK_LIGHT = {"block": "black_light", "display": "Black Light", "light": 6, "range": 6.0}
# Glow Paint (GlowPaintBlock + DecorationBlockEntity): a design painted on any face of a block; faint by itself, it
# glows bright under a black light. Use it to paint the next design.
GLOW_PAINT = {"block": "glow_paint", "display": "Glow Paint", "designs": ["skull", "bat", "spider", "web", "hand", "eye"]}
# The Witch Fire Brazier (WitchFireBrazierBlock + DecorationBlockEntity): an iron brazier lit by flint and steel (light
# `light`), its flames dyed `flames` with a dye; a shovel puts it out. It burns nothing.
BRAZIER = {"block": "witch_fire_brazier", "display": "Witch Fire Brazier", "light": 15,
           "flames": {"orange": "orange_dye", "green": "green_dye", "purple": "purple_dye", "blue": "blue_dye"}}
# The Shadow Puppet Lamp (ShadowPuppetLampBlock + DecorationBlockEntity): lit (light `light`), its paper shade turns
# once every `turn_ticks` round a candle, throwing a bat, a cat and a witch onto walls up to `range` blocks away.
SHADOW_LAMP = {"block": "shadow_puppet_lamp", "display": "Shadow Puppet Lamp", "light": 12, "turn_ticks": 240, "range": 6}
# The Mini Pumpkin Stack (MiniPumpkinStackBlock): three little jack o'lanterns with candles in them (light `light`).
MINI_PUMPKINS = {"block": "mini_pumpkin_stack", "display": "Mini Pumpkin Stack", "light": 12}
# Floating Witch Hats (FloatingWitchHatBlock + DecorationBlockEntity): a witch's hat with a candle in it (light
# `light`) floating where it is placed, bobbing `bob` pixels every `bob_ticks` and turning once every `turn_ticks`.
FLOATING_HAT = {"block": "floating_witch_hat", "display": "Floating Witch Hat", "light": 10, "bob": 1.5, "bob_ticks": 100,
                "turn_ticks": 600}


def decor10_blocks():
    return [BLACK_LIGHT["block"], GLOW_PAINT["block"], BRAZIER["block"], SHADOW_LAMP["block"], MINI_PUMPKINS["block"], FLOATING_HAT["block"]]


def decor10_items():
    return decor10_blocks()


# ---------------------------------------------------------------- Halloween decorations, batch 11: party games
# The Jump-Scare Trap (JumpScareTrapBlock + JumpScareTrapBlockEntity): a crate that springs open with a shriek and a
# ghost on a spring when someone walks up to its front (within `reach` blocks, not sneaking) or on a rising redstone
# signal (a tripwire); the ghost stays out `pop_ticks`, then it rests `reset_ticks`.
JUMP_SCARE = {"block": "jump_scare_trap", "display": "Jump-Scare Trap", "reach": 2.5, "pop_ticks": 40, "reset_ticks": 60}
# The Costume Contest: Runway carpet (CostumeRunwayBlock) and the Judges' Table (JudgesTableBlock +
# JudgesTableBlockEntity). Ringing the table's bell opens a round of `round_ticks`; players in costume (TrickOrTreat's
# costume tag on their head) who walk the runway within `range` blocks are contestants (at most `max_contestants`);
# anyone else votes by using the contestant they like (one vote each, never for themselves). The most votes win the
# `ribbon`.
COSTUME_CONTEST = {"runway": "costume_runway", "runway_display": "Costume Runway", "table": "judges_table", "table_display": "Judges' Table",
                   "ribbon": "best_costume_ribbon", "ribbon_display": "Best Costume Ribbon", "round_ticks": 1200, "range": 16,
                   "max_contestants": 16}
# Pumpkin Bowling: Skeleton Pins (SkeletonPinBlock) and the Bowling Pumpkin (BowlingPumpkinItem, rolled as a
# BowlingPumpkin entity at `speed` blocks a tick, slowing by `friction` a tick, knocking pins down as it rolls and the
# pins behind them one time in `domino_chance`), scored by the Bowling Scoreboard (BowlingScoreboardBlock + entity)
# for the pins within `lane_reach` blocks of it: `frames` frames of two rolls, strikes and spares as in ten-pin.
BOWLING = {"pin": "skeleton_pin", "pin_display": "Skeleton Pin", "pumpkin": "bowling_pumpkin", "pumpkin_display": "Bowling Pumpkin",
           "scoreboard": "bowling_scoreboard", "scoreboard_display": "Bowling Scoreboard", "speed": 0.55, "friction": 0.985,
           "domino_chance": 2, "lane_reach": 4, "frames": 10, "pins": 10}
# The Candy Cache (CandyCacheBlock, a hidden Candy Bowl): a hollow stump that keeps treats like a Candy Bowl does, one
# a night for each finder; it sparkles faintly.
CANDY_CACHE = {"block": "candy_cache", "display": "Candy Cache"}
# The Monster Mash Dance Floor (DanceFloorBlock + DecorationBlockEntity): tiles light up and pulse in colours while a
# jukebox with a disc in it or a redstone signal is next to one of them, passing it along up to `reach` tiles (light
# `light`); villagers on lit tiles hop and spin.
DANCE_FLOOR = {"block": "dance_floor", "display": "Monster Mash Dance Floor", "reach": 8, "light": 8}
# Ghost Tag (GhostBellBlock + GhostBellBlockEntity): ring the bell to start a round of `round_ticks` with everyone
# within `range` blocks (at least two); whoever is "it" glows; "it" tags someone by hitting them (no harm done), not
# the one who just tagged them back within `tag_back_ticks`.
GHOST_TAG = {"block": "ghost_bell", "display": "Ghost Bell", "round_ticks": 2400, "range": 16, "tag_back_ticks": 40, "max_players": 32}
# The Fortune Teller's Table (FortuneTellerTableBlock + DecorationBlockEntity): use it to turn a tarot card and have the
# planchette slide over the spirit board; one of `fortunes` silly fortunes, at most one a player every `cooldown_ticks`.
FORTUNE_TABLE = {"block": "fortune_teller_table", "display": "Fortune Teller's Table", "fortunes": 20, "cards": 6, "cooldown_ticks": 40}


def decor11_blocks():
    return [JUMP_SCARE["block"], COSTUME_CONTEST["runway"], COSTUME_CONTEST["table"], BOWLING["pin"], BOWLING["scoreboard"],
            CANDY_CACHE["block"], DANCE_FLOOR["block"], GHOST_TAG["block"], FORTUNE_TABLE["block"]]


def decor11_items():
    return decor11_blocks() + [COSTUME_CONTEST["ribbon"], BOWLING["pumpkin"]]

# ---------------------------------------------------------------- Halloween decorations, batch 12: night events
# Trick-or-treaters at your door (TrickOrTreaters, during the Halloween event, dusk to midnight): every `check_ticks`
# a Candy Bowl by a wooden door with a porch light, with a player within `player_range` blocks, has one chance in
# `chance` of a visit, at most `max_groups` a night. A group of `kids` village children in costume (`costumes`) comes
# from `spawn_distance` blocks away, walks to the bowl, knocks and waits `wait_ticks`; each takes a treat from the bowl
# and leaves a thank-you gift (loot table `gift_table`); if the bowl is empty they toilet-paper up to `streamers` spots
# within `prank_reach` blocks of the door instead. They give up after `give_up_ticks`, and go home `leave_ticks`.
TRICK_OR_TREATERS = {"check_ticks": 200, "chance": 4, "max_groups": 6, "kids": [1, 3], "spawn_distance": [12, 20], "player_range": 48,
                     "give_up_ticks": 1200, "wait_ticks": 60, "leave_ticks": 300, "prank_reach": 8, "streamers": 6,
                     "gift_table": "gameplay/trick_or_treater_thanks",
                     "costumes": ["minecraft:carved_pumpkin", "jugcraft:witch_hat", "jugcraft:scarecrow_hat", "jugcraft:ghost_sheet"],
                     "gifts": [("minecraft:poppy", 20), ("minecraft:dandelion", 20), ("minecraft:oxeye_daisy", 10),
                               ("minecraft:pumpkin_seeds", 15), ("minecraft:paper", 15), ("minecraft:feather", 10),
                               ("minecraft:candle", 5)]}
# Toilet Paper Rolls (ToiletPaperRollItem, thrown as a ToiletPaperRoll entity): where one lands it drapes up to
# `streamers` Toilet Paper Streamers (ToiletPaperStreamerBlock) within `reach` blocks, hanging from leaves and logs
# (up to `max_length` long) or draped over fences and walls. They drop nothing; rain washes off the ones it reaches.
TOILET_PAPER = {"item": "toilet_paper_roll", "display": "Toilet Paper Roll", "block": "toilet_paper_streamer",
                "block_display": "Toilet Paper Streamer", "streamers": 4, "reach": 2, "max_length": 3}
# The Haunted Hayride (HauntedHayride, a minecart): a hay wagon on rails with `seats` seats; at night, while it rolls
# with riders, something spooky is heard every `spook_ticks` ticks (between the two).
HAYRIDE = {"item": "haunted_hayride", "display": "Haunted Hayride", "seats": 4, "spook_ticks": [100, 240]}
# The Halloween Bonfire (HalloweenBonfireBlock + entity): a great log fire (light `light`) that cooks what a campfire
# cooks, `slots` at a time, `speed` times as fast, and burns what stands in it. A Marshmallow on a Stick
# (MarshmallowStickItem) held over a lit bonfire within `reach` blocks, or a campfire within `campfire_reach`, toasts in
# `toast_ticks` and burns at `burn_ticks`.
BONFIRE = {"block": "halloween_bonfire", "display": "Halloween Bonfire", "light": 15, "slots": 4, "speed": 2, "reach": 3.5,
           "campfire_reach": 2.0, "stick": "marshmallow_on_a_stick", "stick_display": "Marshmallow on a Stick", "toast_ticks": 60,
           "burn_ticks": 140}


def decor12_blocks():
    return [TOILET_PAPER["block"], BONFIRE["block"]]


def decor12_items():
    return decor12_blocks() + [TOILET_PAPER["item"], HAYRIDE["item"], BONFIRE["stick"]]


# ---------------------------------------------------------------- Halloween decorations, batch 13: treats
# The Witch's Brew Punch Bowl (PunchBowlBlock): a glass bowl of glowing green punch (light `light`) with dry-ice fog
# rolling over its rim; a berry (`ingredients`) brews `per_berry` servings, up to `servings`; a glass bottle ladles one
# Witch's Brew Punch.
PUNCH_BOWL = {"block": "witchs_brew_punch_bowl", "display": "Witch's Brew Punch Bowl", "servings": 12, "per_berry": 2, "light": 6,
              "punch": "witchs_brew_punch", "ingredients": ["minecraft:glow_berries", "minecraft:sweet_berries"]}
# The Barmbrack (BarmbrackBlock + entity): an Irish fruit loaf of `slices` slices, eaten like a cake (each `slice_food`);
# one slice, picked when it is placed, hides the `ring` (the gold nugget baked into it): whoever eats that slice finds
# it. Every other slice tells a smaller fortune (`fortunes`).
BARMBRACK = {"block": "barmbrack", "display": "Barmbrack", "slices": 6, "slice_food": [2, 0.4], "ring": "barmbrack_ring",
             "ring_display": "Barmbrack Ring", "fortunes": ["coin", "pea", "stick", "cloth", "crumbs"]}
# Giant Candy (GiantCandyBlock): a prop of a giant sweet, one of `designs` (an empty hand changes it).
GIANT_CANDY = {"block": "giant_candy", "display": "Giant Candy", "designs": ["candy_corn", "lollipop", "wrapped_candy", "gumdrop"]}


def decor13_blocks():
    return [PUNCH_BOWL["block"], BARMBRACK["block"], GIANT_CANDY["block"]]


def decor13_items():
    return decor13_blocks() + [BARMBRACK["ring"]]


# ---------------------------------------------------------------- Halloween decorations, batch 14: costumes
# Outfits (JugcraftAgriculture.OUTFITS): worn on the head like the Ghost Sheet, one at a time, and drawn over the whole
# body by the client's CostumeLayer from assets/jugcraft/costumes.json. Each is a trick-or-treat costume and a costume
# hat (COSTUME_TAG, COSTUME_HAT_TAG). The Skeleton Suit's bones glow in the dark (`glow`); the cape wraps round its
# wearer while they sneak, the tails sway, and the bat wings spread and flap while their wearer is off the ground.
OUTFITS = {"vampire_cape": {"display": "Vampire Cape"}, "mummy_wraps": {"display": "Mummy Wraps"},
           "skeleton_suit": {"display": "Skeleton Suit", "glow": True}, "werewolf_mask": {"display": "Werewolf Mask"},
           "cat_ears_and_tail": {"display": "Cat Ears and Tail"}, "bat_wings": {"display": "Bat Wings"}}
# The Costume Trunk (CostumeTrunkBlock + entity): holds up to `slots` costumes (item tag COSTUME_TAG). A costume in hand
# goes in; an empty hand changes into the next one (what you wore goes in at the back); sneaking with an empty hand takes
# the last one out. Its lid opens for `open_ticks` when used.
COSTUME_TRUNK = {"block": "costume_trunk", "display": "Costume Trunk", "slots": 9, "open_ticks": 20}


def decor14_blocks():
    return [COSTUME_TRUNK["block"]]


def decor14_items():
    return decor14_blocks() + list(OUTFITS)


# ---------------------------------------------------------------- Fall additions 1: the chandlery
# The Wax Melting Pot (WaxPotBlock + entity) holds up to `capacity` measures of one wax (`waxes`: what puts it in, as an
# item tag, how many measures an item gives, how long a candle burns for each layer of it, and its natural colour). Over
# a heat source (HEAT_TAG, as the Cooking Pot) it melts a measure every `melt_ticks`; without heat its molten wax sets
# again, a measure every `set_ticks`. Molten wax takes dyes (mixed as leather dye mixes), up to `max_scents` scents
# (`scents`: item tag, the aura, its colour), a brightener (stronger aura, burns `bright_burn` as long) and an extender
# (burns `long_burn` as long). String dipped in molten wax starts a candle; each later dip adds a layer (a measure of wax)
# once the last has cooled (`cool_ticks`; dipped too soon the layer slides off and is lost), up to `max_dips` layers.
# The Aura Candle burns for the sum of its layers. Lit, it pulses every `pulse_ticks` (as a beacon does) over `radius`
# blocks (by layers), giving players within it its scents' effects for `effect_ticks`. A candle layered with more than
# `max_scents` scents is muddled: it burns for light but has no aura. Harvest gives radius^2 / `harvest_divisor` random
# ticks to plants in the radius a pulse (twice as many when bright).
CHANDLERY = {"pot": "wax_melting_pot", "pot_display": "Wax Melting Pot", "candle": "aura_candle", "candle_display": "Aura Candle",
             "capacity": 8, "melt_ticks": 100, "set_ticks": 300, "cool_ticks": 40, "max_dips": 4, "max_scents": 2,
             "pulse_ticks": 80, "effect_ticks": 180, "radius": [5, 8, 12, 16], "light": [8, 10, 12, 14],
             "bright_burn": 0.5, "long_burn": 1.5, "harvest_divisor": 4,
             "brightener": {"tag": "jugcraft:candle_brighteners", "items": ["minecraft:glowstone_dust"]},
             "extender": {"tag": "jugcraft:candle_extenders", "items": ["minecraft:redstone"]},
             "waxes": {"beeswax": {"display": "Beeswax", "items": ["minecraft:honeycomb"], "measures": 2, "burn_per_dip": 4800,
                                   "color": 0xE8B84A},
                       "tallow": {"display": "Tallow", "items": ["minecraft:rotten_flesh"], "measures": 1, "burn_per_dip": 2400,
                                  "color": 0xEEE6D2}},
             # Scents: the vanilla MobEffects field given to players, or a special aura (warding: hostile mobs in the radius
             # are slowed and weakened; harvest: plants grow; revealing: other creatures glow).
             "scents": {"swiftness": {"display": "Swiftness", "items": ["minecraft:sugar"], "effect": "SPEED", "color": 0x7CC8F0},
                        "leaping": {"display": "Leaping", "items": ["minecraft:rabbit_foot"], "effect": "JUMP_BOOST", "color": 0x9CF26E},
                        "moonlight": {"display": "Moonlight", "items": ["minecraft:golden_carrot"], "effect": "NIGHT_VISION",
                                      "color": 0x5A78FF},
                        "featherfall": {"display": "Featherfall", "items": ["minecraft:feather"], "effect": "SLOW_FALLING",
                                        "color": 0xF2F2E8},
                        "tide": {"display": "Tide", "items": ["minecraft:pufferfish"], "effect": "WATER_BREATHING", "color": 0x3FA9C8},
                        "ember": {"display": "Ember", "items": ["minecraft:magma_cream"], "effect": "FIRE_RESISTANCE", "color": 0xFF6A1A},
                        "diligence": {"display": "Diligence", "items": ["minecraft:amethyst_shard"], "effect": "HASTE", "color": 0xE8C84A},
                        "mending": {"display": "Mending", "items": ["minecraft:ghast_tear"], "effect": "REGENERATION", "color": 0xF27ACB},
                        "warding": {"display": "Warding", "items": ["minecraft:fermented_spider_eye"], "effect": None, "color": 0x7A3FCF},
                        "harvest": {"display": "Harvest", "items": ["minecraft:bone_meal"], "effect": None, "color": 0x5FBF3A},
                        "revealing": {"display": "Revealing", "items": ["minecraft:glow_ink_sac"], "effect": None, "color": 0x9FFFE8},
                        # Ectoplasm, caught from restless spirits (fall additions 9): everyone near turns invisible.
                        "ghostly": {"display": "Ghostly", "items": ["jugcraft:ectoplasm"], "effect": "INVISIBILITY", "color": 0xB8FFD8}}}


def chandlery_blocks():
    return [CHANDLERY["pot"], CHANDLERY["candle"]]


def chandlery_items():
    return chandlery_blocks()


# ---------------------------------------------------------------- Fall additions 2: the cider mill
# The apple tree (AppleLeavesBlock, like the chestnut tree): apple seeds plant a sapling that grows an oak-trunked tree
# (worldgen/feature/apple_tree.json) whose leaves blossom and then hang with ripe apples, one stage in `fruit_chance`
# random ticks; a right-click picks 1-3 vanilla apples. Wild apple trees grow in plains and flower-rich places.
# The Cider Press (CiderPressBlock + entity) takes `capacity` apples (tag `apples`) in its hopper and basket together; an
# empty hand turns its crank, grinding one apple into pulp, then its screw, pressing the pulp in `turns` turns, each letting
# its share of the juice (a serving an apple) into a trough of `trough` servings; the last turn knocks out a pomace for
# every `apples_per_pomace` apples (rounded up). Crank and screw each move once every `work_ticks`. A glass bottle draws
# a serving: Sweet Cider. The Cider Barrel (CiderBarrelBlock + entity) holds `capacity` servings of one batch: fresh juice
# ferments into Sparkling Cider after `sparkling_ticks` and matures into Aged Cider after `aged_ticks`, counted from the
# last fresh serving poured in (a fermenting batch takes no more). A glass bottle draws whatever it has become.
CIDER = {"tree": {"sapling": "apple_sapling", "leaves": "apple_leaves", "seed": "apple_seeds", "fruit_chance": 10,
                  "pick": {"item": "minecraft:apple", "min": 1, "max": 3},
                  # worldgen/feature/apple_tree.json: a rounded crown on a short oak trunk.
                  "trunk": {"base_height": 4, "height_rand_a": 1}, "foliage": {"radius": 2, "height": 3},
                  "biomes": ["IS_PLAINS", "IS_FLORAL"], "rarity": 12, "display": {"apple_sapling": "Apple Sapling",
                                                                                    "apple_leaves": "Apple Leaves"}},
         "press": {"block": "cider_press", "display": "Cider Press", "apples": "jugcraft:cider_apples",
                   "apple_items": ["minecraft:apple"], "capacity": 8, "trough": 8, "turns": 4, "work_ticks": 8,
                   "apples_per_pomace": 2},
         "barrel": {"block": "cider_barrel", "display": "Cider Barrel", "capacity": 16, "sparkling_ticks": 24000,
                    "aged_ticks": 72000, "stages": ["sweet_cider", "sparkling_cider", "aged_cider"]}}


def cider_blocks():
    return [CIDER["tree"]["sapling"], CIDER["tree"]["leaves"], CIDER["press"]["block"], CIDER["barrel"]["block"]]


def cider_items():
    return [CIDER["tree"]["leaves"], CIDER["press"]["block"], CIDER["barrel"]["block"]]


# ---------------------------------------------------------------- Fall additions 3: the preserves pantry
# Preserves (PreserveJarItem) are cooked into a Mason Jar in the Cooking Pot (POT_RECIPES). A jar holds `servings`
# servings, eaten one at a time (`food` each, and `effect` [vanilla MobEffects field, seconds] if any); the last leaves the
# jar. Fresh from the pot a jar is unsealed and spoils `spoil_ticks` after it was cooked (a spoiled serving: 1 food,
# Hunger and Nausea). The Canning Kettle (CanningKettleBlock + entity) holds water and up to `kettle_jars` full jars; over
# heat its water boils after `boil_ticks`, and a jar in boiling water for `process_ticks` is sealed (component
# jugcraft:sealed): it keeps until opened, when its days start. The Pantry Shelf (PantryShelfBlock + entity) shows up to
# `shelf_slots` jars. `color`: what is in the jar, for the item textures and the renderers.
PANTRY = {"jar": "mason_jar", "jar_display": "Mason Jar", "vinegar": "cider_vinegar", "vinegar_display": "Cider Vinegar",
          "kettle": "canning_kettle", "kettle_display": "Canning Kettle", "shelf": "pantry_shelf", "shelf_display": "Pantry Shelf",
          "servings": 4, "spoil_ticks": 72000, "kettle_jars": 4, "boil_ticks": 200, "process_ticks": 400, "shelf_slots": 6,
          "preserves": {
              "sweet_berry_jam": {"display": "Sweet Berry Jam", "food": [3, 0.4], "effect": None, "color": 0x9A1E3A, "kind": "sweet"},
              "apple_butter": {"display": "Apple Butter", "food": [4, 0.5], "effect": None, "color": 0x7A3A14, "kind": "sweet"},
              "pumpkin_butter": {"display": "Pumpkin Butter", "food": [4, 0.5], "effect": None, "color": 0xC8701E, "kind": "sweet"},
              "cranberry_preserves": {"display": "Cranberry Preserves", "food": [3, 0.4], "effect": None, "color": 0xB0122E, "kind": "sweet"},
              "glow_berry_jelly": {"display": "Glow Berry Jelly", "food": [2, 0.3], "effect": ["NIGHT_VISION", 30], "color": 0xF0B030,
                                   "kind": "sweet"},
              "pickled_beets": {"display": "Pickled Beets", "food": [2, 0.4], "effect": None, "color": 0x7A1040, "kind": "pickle"},
              "pickled_peppers": {"display": "Pickled Peppers", "food": [2, 0.4], "effect": ["FIRE_RESISTANCE", 15], "color": 0x4A8A2A,
                                  "kind": "pickle"},
              "corn_relish": {"display": "Corn Relish", "food": [3, 0.5], "effect": None, "color": 0xE0B828, "kind": "pickle"}}}


# ---------------------------------------------------------------- Fall additions 4: crows and working scarecrows
# Crows (agriculture/Crow.java, Crows.java) come to fields by day: every `spawn_ticks`, for each overworld player,
# `spawn_chance` of the time a spot `min_distance` to `max_distance` blocks away is tried; if a ripe crop is within
# `field_radius` of it (`field_tries` spots sampled) a flock of `flock` crows arrives, while the spawn_mobs rule is on.
# At most `near_cap` near a player (within `near_range`), `level_cap` in the world; they leave at `day_end` on the
# overworld clock, climbing until `leave_height` over the ground or for `leave_ticks`. A crow searches
# `search_tries` spots within `raid_radius` for a ripe single-block crop, flies to it and pecks for `peck_ticks`,
# setting it back `setback` stages, then rests `raid_cooldown` ticks and up to `raid_cooldown_spread` more; only while
# the mob_griefing rule is on. It flies off from a player within `flee_radius` (sneaking: `sneak_flee_radius`), or from a
# scarecrow come to guard its crop (looking every `look_ticks`).
# Scarecrows (Scarecrows.java) guard crops within `guard` blocks across (bare, wearing a head, wearing a lit head) and
# `guard_height` up or down. A crow drops 0 to `feathers` feathers.
CROWS = {"entity": "crow", "display": "Crow", "health": 4.0, "flee_radius": 6.0, "sneak_flee_radius": 2.5, "raid_radius": 12,
         "search_tries": 24, "peck_ticks": 40, "setback": 3, "raid_cooldown": 600, "raid_cooldown_spread": 600,
         "spawn_ticks": 200, "spawn_chance": 0.3, "min_distance": 16, "max_distance": 40, "field_radius": 6, "field_tries": 16,
         "flock": [2, 3], "near_cap": 6, "near_range": 48, "level_cap": 32, "day_end": 12000,
         "leave_height": 24, "leave_ticks": 200, "look_ticks": 10,
         "guard": {"bare": 4, "headed": 8, "lit": 12}, "guard_height": 6, "feathers": 2, "table": "entities/crow"}

# ---------------------------------------------------------------- Fall additions 5: spooky fireworks
# Spooky fireworks (SpookyFireworkItem, SpookyRocket): rockets that burst into a picture made of sparks (FireworkShape:
# a bat, a jack o'lantern, a ghost or a skull), drawn by each client facing the player who watches. Crafted from paper,
# 1 to 3 gunpowder (the flight, as vanilla's) and the shape's ingredients, `per_craft` a craft; glowstone dust makes the
# sparks twinkle. A rocket flies `lifetime_base` x (flight + 1) ticks plus up to `lifetime_spread` more, climbing
# `climb` blocks/tick faster each tick, and bursts at the end or where it hits something. It hurts nothing and breaks
# nothing. The Show Launcher (ShowLauncherBlock) holds `tube_capacity` rockets (spooky or vanilla) in each of its
# `tubes` tubes; a rising redstone signal (or an empty hand) starts a show in its mode and a second stops it:
# sequence (one rocket every `sequence_ticks`), volley (a row of three every `volley_ticks`) or finale (one from every
# tube at once). Rockets leave its tubes fanned out by `lean` blocks/tick (vanilla rockets `vanilla_lean`).
FIREWORKS = {"shapes": {"bat": {"item": "bat_firework", "display": "Bat Burst Firework",
                                "ingredients": ["minecraft:feather", "minecraft:black_dye"], "colour": 0x9B59D0},
                        "pumpkin": {"item": "pumpkin_firework", "display": "Jack o'Lantern Burst Firework",
                                    "ingredients": ["minecraft:carved_pumpkin"], "colour": 0xFF8A1C},
                        "ghost": {"item": "ghost_firework", "display": "Ghost Burst Firework",
                                  "ingredients": ["minecraft:phantom_membrane"], "colour": 0xF2F4FF},
                        "skull": {"item": "skull_firework", "display": "Skull Burst Firework",
                                  "ingredients": ["minecraft:bone"], "colour": 0xEDE3C4}},
             "per_craft": 3, "flights": [1, 2, 3], "twinkle": "minecraft:glowstone_dust", "component": "twinkle",
             "lifetime_base": 10, "lifetime_spread": 12, "climb": 0.04, "entity": "spooky_rocket", "particle": "spooky_spark",
             "launcher": "show_launcher", "launcher_display": "Show Launcher", "tubes": 9, "tube_capacity": 16,
             "sequence_ticks": 10, "volley_ticks": 20, "lean": 0.1, "vanilla_lean": 0.003}

# ---------------------------------------------------------------- Fall additions 6: the sky lantern festival
# Sky lanterns (SkyLanternItem, SkyLantern): used, a lantern is let go in front of its holder; it rises `rise` blocks a
# tick and drifts with the wind (`wind` blocks a tick, its direction turning full circle every `wind_period` ticks, the
# same for every lantern), burns `lifetime` ticks plus up to `lifetime_spread` more, dimming over the last `fade_ticks`.
# Dyed in the crafting grid as Minecraft 26.3 dyes leather armour (a `dye_recipe` recipe taking any dye) and washed
# clean in a water cauldron (`wash_tag`); named in an anvil (its wish). `per_craft` a craft. When
# `festival_lanterns` are let go within `festival_radius` blocks of each other in `festival_window` ticks (SkyLanterns),
# players within the radius get Luck for `luck_ticks` and A Sky Full of Wishes; no second festival there for
# `festival_cooldown` ticks; the server remembers at most `memory` releases. Mooncakes (MooncakeItem), `mooncake_count`
# a batch in the Cooking Pot, give `mooncake_food`, and Luck for `mooncake_luck_ticks` when eaten outdoors on a
# full-moon night (`night` on the overworld clock, the first night of eight).
LANTERNS = {"item": "sky_lantern", "display": "Sky Lantern", "entity": "sky_lantern", "default_colour": 0xE8642A,
            "dye_recipe": "minecraft:crafting_dye", "dye_group": "dyed_sky_lantern", "wash_tag": "minecraft:cauldron_can_remove_dye",
            "rise": 0.035, "wind": 0.015, "wind_period": 72000, "lifetime": 2400, "lifetime_spread": 600, "fade_ticks": 100,
            "per_craft": 2, "festival_lanterns": 8, "festival_radius": 32, "festival_window": 2400, "festival_cooldown": 24000,
            "luck_ticks": 6000, "memory": 256,
            "mooncakes": {"red_bean_mooncake": {"display": "Red Bean Mooncake", "filling": {"jugcraft:beans": 2}},
                          "chestnut_mooncake": {"display": "Chestnut Mooncake", "filling": {"jugcraft:roasted_chestnuts": 2}},
                          "pumpkin_mooncake": {"display": "Pumpkin Mooncake", "filling": {"minecraft:pumpkin": 1}}},
            "mooncake_base": {"minecraft:wheat": 2, "minecraft:sugar": 1, "minecraft:egg": 1}, "mooncake_count": 4,
            "mooncake_time": 300, "mooncake_food": [3, 0.6], "mooncake_luck_ticks": 6000, "night": [13000, 23000]}

# ---------------------------------------------------------------- Fall additions 7: the harvest feast
# The Harvest Feast Table (FeastTableBlock, FeastTableBlockEntity, Feasts): lengths placed end to end along one axis join
# into a table of up to `max_length`; each length holds `dishes` dishes of up to `servings` servings of one food. Eating
# a serving there works out the feast: score = different foods on the table + players who ate there in the last
# `window` ticks; every recent diner within `reach` blocks gets the tier: `tiers` (good meal, feast, harvest feast, grand
# feast) at those scores, with Regeneration for `regeneration_ticks`, Absorption for `absorption_ticks`, Haste and Luck
# and (grand) Health Boost for `long_ticks`, and Harvest Home. `per_craft` a craft.
FEAST = {"block": "feast_table", "display": "Harvest Feast Table", "dishes": 2, "servings": 8, "window": 2400, "max_length": 8,
         "tiers": [3, 5, 8, 11], "regeneration_ticks": 200, "absorption_ticks": 2400, "long_ticks": 6000, "reach": 16,
         "per_craft": 2}

# ---------------------------------------------------------------- Fall additions 8: the corn maze
# The Corn Maze Gate (CornMazeGateBlock, CornMazeGateBlockEntity, CornMaze): sneak-use cycles the size (`cells` cells
# across, `sizes`); used holding corn kernels it carves a perfect maze from a new seed and plants maze corn (MazeCornBlock,
# three tall, solid, needing only solid ground) along its walls, `plant_per_tick` stalks a tick, one kernel a stalk, only
# where three blocks are clear; the finish post goes at the exit. A run starts through the gate and ends at the finish
# post; it is void if the runner flies, climbs on the corn, leaves the maze, takes more than `max_run` ticks, or walked
# less than `shortcut` of the shortest way through. The best of each runner, top `board`, go on the board; ribbons the
# first time a runner places; A-maze-ing for finishing. At most `max_runners` at once.
MAZE = {"gate": "corn_maze_gate", "gate_display": "Corn Maze Gate", "finish": "corn_maze_finish", "finish_display": "Corn Maze Finish Post",
        "corn": "maze_corn", "corn_display": "Maze Corn", "cells": [3, 5, 7, 9], "sizes": ["tiny", "small", "medium", "large"],
        "plant_per_tick": 32, "max_run": 12000, "shortcut": 0.8, "board": 3, "max_runners": 16, "kernel": "corn_kernels"}

# ---------------------------------------------------------------- Fall additions 9: ghost hunting
# Restless spirits (RestlessSpirit, Spirits) rise from graves at night: each random tick of a grave (the gravestones and
# the grave mound) raises one `stir_chance` of the time, at night on the overworld clock, while fewer than `near_cap`
# are within `near_range` blocks. A spirit drifts about its grave, `haunt_radius` blocks across and up to `haunt_height`
# above, at `drift_speed` blocks/tick. Hidden until revealed: by a player holding the Spirit Lantern within
# `reveal_radius` blocks (it looks every `look_ticks`, and a look lasts `reveal_ticks`), or by glowing (a Revealing
# candle). Revealed, it fades in over `fade_ticks`, shows to everyone, and shies away (`shy_speed`) from anyone within
# `shy_radius` (a sneaking player gets to `sneak_shy_radius`), never out of its haunt. A glass bottle catches a revealed
# spirit as Ectoplasm (giving the bottle back when stirred into wax), the Ghostly candle scent.
GHOSTS = {"lantern": "spirit_lantern", "lantern_display": "Spirit Lantern", "ectoplasm": "ectoplasm", "ectoplasm_display": "Ectoplasm",
          "entity": "restless_spirit", "display": "Restless Spirit",
          "graves": ["rounded_gravestone", "cross_gravestone", "obelisk_gravestone", "grave_mound"],
          "stir_chance": 0.25, "near_cap": 3, "near_range": 16, "reveal_radius": 12,
          "haunt_radius": 6, "haunt_height": 3.0, "shy_radius": 3.0, "sneak_shy_radius": 1.5, "drift_speed": 0.04, "shy_speed": 0.12,
          "reveal_ticks": 40, "look_ticks": 10, "fade_ticks": 10}

# ---------------------------------------------------------------- Fall additions 10: face paint
# A Face Paint Kit (FacePaintKitItem) paints one of `designs` on a player's face (FacePaint: a Fabric data attachment,
# saved and sent to every client that sees the player, drawn by the client's FacePaintLayer). Its dial (data component
# `component`) picks the design; it lasts `uses` faces; painting your own face takes `use_ticks`. The paint washes off
# when the player's head is under water (checked every `wash_ticks`) or at death. A painted face counts as a costume.
FACE_PAINT = {"kit": "face_paint_kit", "kit_display": "Face Paint Kit", "component": "face_paint_design", "attachment": "face_paint",
              "uses": 16, "use_ticks": 32, "wash_ticks": 20,
              "designs": {"skull": "Skull", "pumpkin": "Jack o'Lantern", "black_cat": "Black Cat", "vampire": "Vampire",
                          "witch": "Witch", "scarecrow": "Scarecrow"}}

# ---------------------------------------------------------------- Fall additions 11: the candy kitchen
# The Candy Kettle (CandyKettleBlock + entity) boils a batch: a base (a water bottle for syrup, a milk bucket for cream),
# up to `max_sugar` sugar (`pieces_per_sugar` pieces each), up to `max_flavours` flavours (item tags
# jugcraft:candy_flavours/<flavour>) and dyes, all added below `add_below` degrees. Over heat it warms a degree every
# `heat_ticks` up to the boil (`boil`), every `boil_ticks` while its water boils off (to `boiled`), then every `cook_ticks`
# up to `max_temp`; off the heat it cools a degree every `cool_ticks` to `room`. The hottest it has been sets its stage
# (`stages`: name and the degree it starts at); `makes` gives the candy each base sets into at each stage (None: too runny
# to pour). It pours onto a Candy Tray (CandyTrayItem, component `component`): most candy sets in `set_ticks`, rock candy
# grows for `crystal_ticks`, taffy must be pulled `pulls` times (`pull_ticks` each) within `warm_ticks` or it sets hard
# (as hard candy), candy corn takes up to `max_layers` layers, and hard candy with sticks makes lollipops. `candies`: the
# kettle's own candy items (food: nutrition, saturation; eaten in `eat_seconds`, even when full). Flavoured candy gives
# each flavour's effect (vanilla MobEffects field, seconds) when eaten.
CANDY = {"kettle": "candy_kettle", "kettle_display": "Candy Kettle", "tray": "candy_tray", "tray_display": "Candy Tray",
         "component": "candy_batch", "max_sugar": 4, "pieces_per_sugar": 2, "max_flavours": 2,
         "room": 20, "boil": 100, "boiled": 110, "max_temp": 190, "add_below": 100,
         "heat_ticks": 4, "boil_ticks": 12, "cook_ticks": 6, "cool_ticks": 8,
         "set_ticks": 100, "warm_ticks": 600, "pull_ticks": 20, "pulls": 4, "crystal_ticks": 24000, "max_layers": 3,
         "eat_seconds": 0.8,
         "stages": {"syrup": ("Syrup", 0), "thread": ("Thread", 110), "soft_ball": ("Soft Ball", 115), "firm_ball": ("Firm Ball", 120),
                    "hard_ball": ("Hard Ball", 125), "soft_crack": ("Soft Crack", 132), "hard_crack": ("Hard Crack", 145),
                    "caramel": ("Caramel", 155), "burnt": ("Burnt", 175)},
         "bases": {"syrup": "Sugar Syrup", "cream": "Cream"},
         "makes": {"syrup": [None, "rock_candy", "candy_corn", "candy_corn", "salt_water_taffy", "salt_water_taffy", "hard_candy", "caramel",
                             "burnt_sugar"],
                   "cream": [None, None, "fudge", "cream_caramel", "cream_caramel", "toffee", "toffee", "burnt_sugar", "burnt_sugar"]},
         "candies": {"rock_candy": {"display": "Rock Candy", "food": [2, 0.1]},
                     "salt_water_taffy": {"display": "Salt Water Taffy", "food": [2, 0.2]},
                     "hard_candy": {"display": "Hard Candy", "food": [1, 0.1]},
                     "lollipop": {"display": "Lollipop", "food": [2, 0.1]},
                     "fudge": {"display": "Fudge", "food": [3, 0.3]},
                     "cream_caramel": {"display": "Cream Caramel", "food": [2, 0.2]},
                     "toffee": {"display": "Toffee", "food": [2, 0.2]},
                     "burnt_sugar": {"display": "Burnt Sugar", "food": [1, 0.0]}},
         # The kinds, in CandyKind order, with the colour each sets to undyed and unflavoured.
         "kinds": {"rock_candy": 0xE6DEF6, "candy_corn": 0xF8F4EA, "salt_water_taffy": 0xF4E2C8, "hard_candy": 0xE8A838,
                   "lollipop": 0xE8A838, "caramel": 0xC07828, "fudge": 0x7A4A2A, "cream_caramel": 0xC8883A, "toffee": 0xB0702A,
                   "burnt_sugar": 0x2A1A10},
         # Candy corn's bands, tip to base, where a layer is undyed.
         "corn_bands": [0xF8F4EA, 0xF08A24, 0xF6C836],
         "flavours": {"chocolate": {"display": "Chocolate", "items": ["minecraft:cocoa_beans"], "effect": "SPEED", "seconds": 10,
                                    "color": 0x5A3218},
                      "berry": {"display": "Berry", "items": ["minecraft:sweet_berries"], "effect": "REGENERATION", "seconds": 4,
                                "color": 0xC8283C},
                      "glow_berry": {"display": "Glow Berry", "items": ["minecraft:glow_berries"], "effect": "NIGHT_VISION", "seconds": 30,
                                     "color": 0xF0B030},
                      "honey": {"display": "Honey", "items": ["minecraft:honey_bottle"], "effect": "ABSORPTION", "seconds": 10,
                                "color": 0xE8A020},
                      "cranberry": {"display": "Cranberry", "items": ["jugcraft:cranberries"], "effect": "RESISTANCE", "seconds": 10,
                                    "color": 0xB0122E},
                      "spiced": {"display": "Spiced", "items": ["jugcraft:mulling_spices"], "effect": "FIRE_RESISTANCE", "seconds": 15,
                                 "color": 0xA0522D},
                      "chestnut": {"display": "Chestnut", "items": ["jugcraft:roasted_chestnuts"], "effect": "HASTE", "seconds": 15,
                                   "color": 0x8A5A30}}}


def candy_blocks():
    return [CANDY["kettle"]]


def candy_items():
    return candy_blocks() + [CANDY["tray"]] + list(CANDY["candies"])

# ---------------------------------------------------------------- Fall additions 12: autumn foraging
# Wild mushrooms (WildMushroomBlock) grow on soil (block tag `soil_tag`, `soil`) in patches on forest floors (`biomes`,
# `patch`). One random tick in `spread_chance`, a mushroom with fewer than `spread_cap` of its kind within 4 blocks (and
# 1 up or down) puts out another within 2, where the light is below `spread_light`; bone meal makes it try at once, in any
# light. On a full-moon night one random tick in `ring_chance` sprouts a fairy ring of its kind instead: eight round a
# circle of radius three. A fairy ring (FairyRings) is `ring_mushrooms` or more wild mushrooms between `inner` and `outer`
# blocks from a centre; a player at its centre on a full-moon night is blessed once a night (Luck II for `luck_ticks`;
# players looked at every `check_ticks`). `light`: the light a mushroom gives. The Foraging Basket (ForagingBasketItem)
# holds forage (item tag `forage_tag`) like a bundle.
FORAGING = {"basket": "foraging_basket", "basket_display": "Foraging Basket", "forage_tag": "jugcraft:forage",
            "soil_tag": "jugcraft:mushroom_soil", "soil": ["minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
                                                            "minecraft:podzol", "minecraft:mycelium", "minecraft:moss_block"],
            "spread_chance": 25, "spread_cap": 5, "spread_light": 13, "ring_chance": 40,
            "ring_mushrooms": 8, "inner": 2.5, "outer": 3.6, "check_ticks": 20, "luck_ticks": 6000,
            "patch": {"rarity": 16, "tries": 12, "spread_xz": 4, "spread_y": 1},
            "mushrooms": {"chanterelle": {"display": "Chanterelle", "biomes": ["IS_FOREST", "IS_BIRCH_FOREST"], "light": 0},
                          "porcini": {"display": "Porcini", "biomes": ["IS_TAIGA", "IS_FOREST"], "light": 0},
                          "puffball": {"display": "Puffball", "biomes": ["IS_PLAINS", "IS_FOREST"], "light": 0},
                          "fly_agaric": {"display": "Fly Agaric", "biomes": ["IS_BIRCH_FOREST", "IS_TAIGA"], "light": 0},
                          "jack_o_lantern_mushroom": {"display": "Jack o'Lantern Mushroom", "biomes": ["IS_SPOOKY", "IS_FOREST"], "light": 9}},
            "forage": ["#jugcraft:wild_mushrooms", "minecraft:brown_mushroom", "minecraft:red_mushroom", "minecraft:sweet_berries",
                       "minecraft:glow_berries", "minecraft:apple", "minecraft:cocoa_beans", "jugcraft:chestnut", "jugcraft:cranberries"],
            "foods": ["sauteed_chanterelles", "roasted_porcini", "fried_puffball", "foragers_stew"]}


def foraging_blocks():
    return list(FORAGING["mushrooms"])


def foraging_items():
    return foraging_blocks() + [FORAGING["basket"]]

# ---------------------------------------------------------------- Fall additions 13: the Bat House
# The Bat House (BatHouseBlock + entity) holds up to `capacity` roosting bats. At dusk (the Overworld clock, looked at every
# `check_ticks`), while mobs spawn, a house with room gains a bat one time in 1/`move_in_chance`; then every roosting bat
# flies out (vanilla bats, tagged `tag`). At dawn the nearest bats within `return_range` blocks come in, up to the room,
# and each leaves a guano (up to `guano_cap` waiting). Bat Guano is a fertilizer (FertilizerItem): every crop within
# `guano_radius` gets `guano_doses` doses of bone meal; `guano_per_phosphate` guano make a phosphate.
BATS = {"house": "bat_house", "house_display": "Bat House", "guano": "bat_guano", "guano_display": "Bat Guano", "tag": "jugcraft.bat_house",
        "capacity": 4, "guano_cap": 16, "return_range": 32, "move_in_chance": 0.5, "check_ticks": 20,
        "guano_radius": 1, "guano_doses": 1, "guano_per_phosphate": 4}


# The Hay Golem (HayGolem, an entity): built from `hay_bales` hay bales (two stacked, an arm either side of the upper one)
# and a carved head from `heads_tag`. It keeps a post (where it was built or was last led with wheat, `lead_range`) and
# guards crops from crows as a scarecrow wearing its head would (CROWS["guard"]). Every `tend_ticks` it harvests the
# nearest ripe crop within `post_radius` (and `search_height` up or down) of its post, working `work_ticks` at it,
# replanting from the drops, pouching the rest (`pouch_slots` stacks); it carries `carry` items or more home, or what it has
# after `idle_ticks` without a harvest, giving up on a crop or the trip after `give_up_ticks`. Wheat heals `wheat_heal`;
# fire hurts `fire_factor` times; killed it drops `wheat` wheat (min, max), its head and pouch.
HAY_GOLEM = {"entity": "hay_golem", "display": "Hay Golem", "health": 20, "speed": 0.25, "size": [0.9, 2.5],
             "post_radius": 8, "search_height": 2, "tend_ticks": 100, "work_ticks": 10, "give_up_ticks": 300,
             "pouch_slots": 9, "carry": 32, "idle_ticks": 200, "wheat_heal": 4.0, "fire_factor": 2.0, "hay_bales": 4,
             "reach": 1.8, "lead_range": 8.0, "heads_tag": "jugcraft:hay_golem_heads",
             "heads": ["minecraft:carved_pumpkin", "minecraft:jack_o_lantern", "jugcraft:hand_carved_pumpkin",
                       "jugcraft:hand_carved_white_pumpkin", "jugcraft:hand_carved_jarrahdale_pumpkin",
                       "jugcraft:hand_carved_cinderella_pumpkin", "jugcraft:hand_carved_red_kuri_pumpkin",
                       "jugcraft:hand_carved_kabocha_pumpkin"],
             "wheat": [2, 5], "table": "entities/hay_golem"}


# Knitting (fall additions 15): the Spinning Wheel (SpinningWheelBlock + entity) spins a skein of wool in `turns` turns of
# the treadle (each turn runs the wheel `spin_ticks`) into `yarn_per_wool` balls of yarn in the wool's colour; knitwear
# used on it unravels into a ball a row less `unravel_loss`. Knitting Needles (`needles_durability` uses) knit a row a
# ball of yarn, holding use `row_ticks`; each garment takes its `rows` and is worn in its slot, dyed the blend of its
# yarns, with the equipment asset `asset`. Wearing `cozy.pieces` or more within `cozy.range` blocks of a lit campfire
# (looked at every `cozy.ticks`) gives Regeneration I for `cozy.effect_ticks`. Undyed yarn is `undyed`.
KNITTING = {"wheel": "spinning_wheel", "wheel_display": "Spinning Wheel", "yarn": "yarn", "yarn_display": "Ball of Yarn",
            "needles": "knitting_needles", "needles_display": "Knitting Needles", "needles_durability": 128, "row_ticks": 40,
            "turns": 4, "spin_ticks": 20, "yarn_per_wool": 4, "unravel_loss": 1, "undyed": 0xF0E6D2,
            "cozy": {"ticks": 40, "pieces": 2, "range": 4, "effect_ticks": 60}, "knitwear_tag": "jugcraft:knitwear",
            # Dyeing, as Minecraft 26.3 dyes leather armour: a recipe of this type for each dyeable item (yarn and the
            # garments), taking any dye; a water cauldron washes the dye out of anything in vanilla's tag.
            "dye_recipe": "minecraft:crafting_dye", "dye_group": "dyed_knitwear", "wash_tag": "minecraft:cauldron_can_remove_dye",
            # In the order of the Knitwear enum (the needles' cycle).
            "garments": {
                "knit_beanie": {"display": "Knit Beanie", "slot": "HEAD", "rows": 2, "asset": "knit"},
                "wool_socks": {"display": "Wool Socks", "slot": "FEET", "rows": 2, "asset": "knit"},
                "knit_sweater": {"display": "Knit Sweater", "slot": "CHEST", "rows": 5, "asset": "knit"},
                "striped_sweater": {"display": "Striped Sweater", "slot": "CHEST", "rows": 5, "asset": "knit_striped", "motif": "stripes"},
                "pumpkin_sweater": {"display": "Pumpkin Sweater", "slot": "CHEST", "rows": 5, "asset": "knit_pumpkin", "motif": "pumpkin"},
                "bat_sweater": {"display": "Bat Sweater", "slot": "CHEST", "rows": 5, "asset": "knit_bat", "motif": "bat"},
                "leaf_sweater": {"display": "Autumn Leaf Sweater", "slot": "CHEST", "rows": 5, "asset": "knit_leaf", "motif": "leaf"}}}


# Pie baking (fall additions 16): the Hearth Oven (HearthOvenBlock + entity) banks up to `max_burn` ticks of fuel (coal,
# charcoal and coke as long as generators burn them, GeneratorFuels; logs, item tag `wood_tag`, `wood_burn` ticks each);
# burning, it heats a degree every `heat_ticks` to `max_heat`, cooling a degree every `cool_ticks` when out. A pie bakes
# at `bake_heat` or hotter, a point a tick (two at full heat): baked at `baked` points, burnt at `burnt`. Its light when
# lit is `light`. Pies (PieBlock) have `slices` slices; each filling's slice gives its nutrition and
# saturation; a burnt pie's slice gives `burnt_nutrition` and Hunger one time in `burnt_sick_chance`.
PIES = {"oven": "hearth_oven", "oven_display": "Hearth Oven", "dough": "pastry_dough", "dough_display": "Pastry Dough",
        "burnt": "burnt_pie", "burnt_display": "Burnt Pie", "max_burn": 3200, "wood_burn": 300,
        "wood_tag": "jugcraft:hearth_oven_wood", "wood": ["#minecraft:logs_that_burn"], "max_heat": 100, "heat_ticks": 2, "cool_ticks": 4,
        "bake_heat": 50, "baked": 600, "burnt_points": 1200, "light": 13, "slices": 4, "burnt_nutrition": 1, "burnt_sick_chance": 3,
        # In the order of the PieFilling enum: display name, slice food, filling colour, and the raw pie's ingredients
        # (with the pastry and sugar).
        "fillings": {
            "apple": {"display": "Apple", "food": [4, 0.6], "color": 0xC89A48, "with": ["minecraft:apple", "minecraft:apple"]},
            "pumpkin_cream": {"display": "Pumpkin Cream", "food": [4, 0.6], "color": 0xE0822A,
                              "with": ["minecraft:pumpkin", "minecraft:milk_bucket"]},
            "cranberry": {"display": "Cranberry", "food": [3, 0.6], "color": 0xA01C34, "with": ["jugcraft:cranberries", "jugcraft:cranberries"]},
            "sweet_potato": {"display": "Sweet Potato", "food": [4, 0.7], "color": 0xD8682A,
                             "with": ["jugcraft:sweet_potato", "jugcraft:sweet_potato"]},
            "chestnut": {"display": "Chestnut", "food": [5, 0.7], "color": 0x6A3E1E,
                         "with": ["jugcraft:roasted_chestnuts", "jugcraft:roasted_chestnuts"]}}}


# The Spirit Board (fall additions 17): a séance (SpiritBoard, SpiritBoardBlock + entity) needs a lit candle (block tag
# `candles_tag`, anything in it with the vanilla `lit` property set) within `candle_range` blocks, and players' fingers on
# the planchette (up to `max_hands`, each within `hand_range` blocks; a hand lifts beyond `hand_range` + 1). The restless
# spirit nearest the board within `spirit_range` blocks answers: YES, its name and its wish, then GOODBYE, a stop every
# `letter_ticks` with one hand on the planchette or every `fast_letter_ticks` with two or more; no spirit, NO and GOODBYE.
# The board rests `cooldown_ticks` after; players within `watch_range` see the letters. Given what it wishes for (an item
# in `jugcraft:spirit_wishes/<wish>`), a revealed spirit is laid to rest: `rest_xp` experience and Luck for
# `rest_luck_ticks` ticks to the giver. Names and wishes are drawn at random the first time a séance asks a spirit.
SPIRIT_BOARD = {"block": "spirit_board", "display": "Spirit Board", "candle_range": 4, "spirit_range": 16, "hand_range": 3,
                "max_hands": 4, "letter_ticks": 20, "fast_letter_ticks": 12, "cooldown_ticks": 40, "watch_range": 8, "rest_xp": 20,
                "rest_luck_ticks": 6000, "candles_tag": "jugcraft:seance_candles",
                "candles": ["#minecraft:candles", "jugcraft:aura_candle", "jugcraft:floating_candle", "jugcraft:candle_skull"],
                "names": ["MABEL", "OTIS", "EZRA", "HATTIE", "JASPER", "AGNES", "SILAS", "WINNIE", "AMOS", "PRUDENCE", "ELIJAH", "OPAL",
                          "CORNELIUS", "BEATRIX", "HORACE", "LUELLA"],
                # In the order of SpiritBoard.Wish: what the message calls it, and the items that grant it.
                "wishes": {
                    "pie": {"display": "a pie, or a slice of one",
                            "items": [f"jugcraft:{f}_pie" for f in PIES["fillings"]] + [f"jugcraft:{f}_pie_slice" for f in PIES["fillings"]]},
                    "candle": {"display": "a candle", "items": ["#minecraft:candles", "jugcraft:aura_candle"]},
                    "cider": {"display": "a bottle of cider",
                              "items": ["jugcraft:sweet_cider", "jugcraft:sparkling_cider", "jugcraft:aged_cider", "jugcraft:mulled_cider"]},
                    "sweater": {"display": "a knitted sweater",
                                "items": [f"jugcraft:{g}" for g, i in KNITTING["garments"].items() if i["slot"] == "CHEST"]},
                    "candy": {"display": "a piece of candy", "items": [f"jugcraft:{c}" for c in CANDY["candies"]]},
                    "apple": {"display": "an apple", "items": ["minecraft:apple", "minecraft:golden_apple", "jugcraft:caramel_apple"]},
                    "rose": {"display": "a rose (a poppy, or a rose bush)", "items": ["minecraft:poppy", "minecraft:rose_bush"]},
                    "pumpkin": {"display": "a pumpkin", "items": ["minecraft:pumpkin", "minecraft:carved_pumpkin", "minecraft:jack_o_lantern"]}}}


# Wild turkeys (fall additions 18): Turkey (an Animal, `health` and `speed`; MobCategory.CREATURE, `size` wide and tall)
# eats and breeds on item tag `food_tag`; a grown tom with a player or hen within `strut_range` blocks starts to strut
# one tick in `strut_chance`, for `strut_ticks`; a hen lays a vanilla egg every `egg_min` to `egg_max` ticks. Turkeys
# (spawning): every `spawn_ticks`, for each overworld player, `spawn_chance` of the time, a spot `min_distance` to
# `max_distance` away on grass in biome tag `habitat_tag` gets a flock of `flock` (min, max), the first a tom; at most
# `near_cap` within `near_range` of a player and `level_cap` in the world, by day only. Each turkey drops a raw turkey
# and `feathers` (min, max) feathers. A raw turkey cooks (COOKING) into a roast turkey (RoastTurkeyBlock): `servings`
# servings of `serving` food (hunger, saturation), eaten at the table or carved with a Carving Knife into slices; the
# last leaves a bone.
TURKEYS = {"entity": "turkey", "display": "Wild Turkey", "health": 8, "speed": 0.25, "size": [0.6, 0.95], "food_tag": "jugcraft:turkey_food",
           "food": ["minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds", "minecraft:pumpkin_seeds",
                    "minecraft:sweet_berries", "jugcraft:corn_kernels", "jugcraft:sunflower_seeds", "jugcraft:oat_seeds", "jugcraft:barley_seeds"],
           "strut_range": 6, "strut_ticks": 80, "strut_chance": 200, "egg_min": 6000, "egg_max": 12000,
           "spawn_ticks": 400, "spawn_chance": 0.25, "min_distance": 24, "max_distance": 48, "flock": [3, 5], "near_cap": 10,
           "near_range": 64, "level_cap": 40, "habitat_tag": "jugcraft:turkey_habitat",
           "habitat": ["#minecraft:is_forest", "#minecraft:is_taiga", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow"],
           "table": "entities/turkey", "feathers": [1, 3], "raw": "raw_turkey", "raw_food": [3, 0.3], "roast": "roast_turkey",
           "roast_display": "Roast Turkey", "slice": "turkey_slice", "servings": 6, "serving": [3, 0.6]}


# The Theremin (fall additions 19; ThereminBlock + entity): every `sense_ticks` it finds the nearest player or mob within
# `range` blocks of its pitch antenna; comparators read 15 at the antenna down to 0 at `range`. Playing (switched on with
# an empty hand, or powered), it sounds a flute note each look, from `low` pitch at `range` to `high` at the antenna (two
# octaves), with a vibrato of `vibrato` at `vibrato_speed` radians a tick; a player within `player_range` earns Good
# Vibrations. Its magic eye glows (`light`) while it plays. Made with copper wire, so it needs the machines feature.
THEREMIN = {"block": "theremin", "display": "Theremin", "sense_ticks": 4, "range": 8.0, "player_range": 3.0, "low": 0.5, "high": 2.0,
            "vibrato": 0.03, "vibrato_speed": 0.7, "light": 7}


# The Día de Muertos ofrenda (fall additions 20; OfrendaBlock + entity): `slots` offerings, two to each of three tiers.
# Anything in `offerings_tag` may be offered; complete holds one of each `kinds` (item tags jugcraft:ofrenda/<kind>).
# Every `check_ticks` a complete ofrenda glows (`light`) and, at night, welcomes the restless spirits within
# `welcome_range` blocks: each makes the ofrenda its home and shows itself there, calm, until dawn; when one is within
# `arrived` blocks every player within `witness_range` earns Remembered. Marigold petals, papel picado and sugar skulls
# are decorations and offerings; pan de muerto is in ITEMS.
OFRENDA = {"block": "ofrenda", "display": "Ofrenda", "slots": 6, "check_ticks": 40, "welcome_range": 16, "arrived": 3.0, "witness_range": 8,
           "light": 8, "offerings_tag": "jugcraft:ofrenda/offerings",
           "kinds": {"flowers": ["jugcraft:marigold", "jugcraft:marigold_petals"],
                     "light": ["#minecraft:candles", "jugcraft:aura_candle"],
                     "bread": ["jugcraft:pan_de_muerto", "minecraft:bread"],
                     "sugar": ["jugcraft:sugar_skull"],
                     "drink": ["minecraft:potion", "minecraft:honey_bottle", "minecraft:milk_bucket", "jugcraft:sweet_cider", "jugcraft:mulled_cider"]},
           # Keepsakes: a picture, a likeness, a favourite food, the papel picado, any flower of the season.
           "keepsakes": ["minecraft:painting", "minecraft:player_head", "#c:foods", "jugcraft:papel_picado", "minecraft:poppy",
                         "jugcraft:orange_mum", "jugcraft:yellow_mum"],
           "decor": {"marigold_petals": "Marigold Petals", "papel_picado": "Papel Picado", "sugar_skull": "Sugar Skull"}}


def pie_blocks():
    return [PIES["oven"], PIES["burnt"]] + [f"{f}_pie" for f in PIES["fillings"]]


def pie_items():
    return (pie_blocks() + [PIES["dough"]] + [f"raw_{f}_pie" for f in PIES["fillings"]]
            + [f"{f}_pie_slice" for f in PIES["fillings"]])


def knitting_items():
    return [KNITTING["wheel"], KNITTING["yarn"], KNITTING["needles"]] + list(KNITTING["garments"])


def bat_blocks():
    return [BATS["house"]]


def bat_items():
    return bat_blocks() + [BATS["guano"]]


def pantry_blocks():
    return [PANTRY["kettle"], PANTRY["shelf"]]


def pantry_items():
    return pantry_blocks() + [PANTRY["jar"], PANTRY["vinegar"]] + list(PANTRY["preserves"])


def firework_blocks():
    return [FIREWORKS["launcher"]]


def firework_items():
    return firework_blocks() + [info["item"] for info in FIREWORKS["shapes"].values()]


def ghost_items():
    return [GHOSTS["lantern"], GHOSTS["ectoplasm"]]


def face_paint_items():
    return [FACE_PAINT["kit"]]


def lantern_items():
    return [LANTERNS["item"]] + list(LANTERNS["mooncakes"])


def feast_blocks():
    return [FEAST["block"]]


def maze_blocks():
    return [MAZE["gate"], MAZE["finish"], MAZE["corn"]]


def night_blocks():
    return [WISPS["jar"], TREBUCHET["block"], HORSEMAN["lantern"]]


def night_items():
    return night_blocks() + [HORSEMAN["cloak"]]


def festivity_blocks():
    return [CONTEST["stand"]] + list(GRAVESTONES) + list(FEST_DECOR)


def regatta_blocks():
    return [REGATTA["flag"], REGATTA["buoy"]]


def regatta_items():
    return list(PUMPKIN_BOATS) + regatta_blocks() + [TRICK_OR_TREAT["bag"]] + list(COSTUMES)


def potted(mum):
    return f"potted_{mum}"


def giant_tile(kind, size, a, b):
    """A 16x16 tile of a giant pumpkin's side (column a from the left, row b from the top) or top (a = x, b = z)."""
    return f"giant_pumpkin_{kind}_{size}_{a}_{b}"


def halloween_textures():
    """Every block texture of the Halloween harvest that its models reference (the gourds' come with GOURDS)."""
    out = ["giant_pumpkin_bottom"]
    for size in range(2, GIANT_PUMPKIN["max_size"] + 1):
        for a in range(size):
            for b in range(size):
                out += [giant_tile("side", size, a, b), giant_tile("top", size, a, b)]
    out += ["harvest_scale_side", "harvest_scale_top", "harvest_scale_dial", "scarecrow_post", "scarecrow_trousers", "scarecrow_straw"]
    out += [f"scarecrow_shirt_{color}" for color in DYE_COLORS]
    out += ["corn_shock_lower", "corn_shock_upper", "ornamental_corn_bundle", "gourd_birdhouse_front", "gourd_birdhouse_side",
            "gourd_birdhouse_top", "gourd_birdhouse_string"]
    return out + list(MUMS)

# Kitchen Garden equipment: blocks with an item of their own.
# Trellis: a square wooden lattice. It stands on farmland, on any sturdy top face or on another
# trellis; climbing crops are planted on it and grow up it. Tomatoes need two stacked.
# Cooking Pot: cooks multi-ingredient meals (POT_RECIPES) while a heat source is under it.
EQUIPMENT = {
    "trellis": {"display": "Trellis"},
    "cooking_pot": {"display": "Cooking Pot"},
}
# Block tag of what heats a Cooking Pot from directly below. Blocks with a "lit" property (campfires)
# only count while lit. Data packs and other branches can add their own heat sources.
HEAT_TAG = "jugcraft:heat_sources"
HEAT_SOURCES = ["#minecraft:campfires", "minecraft:fire", "minecraft:soul_fire", "minecraft:lava", "minecraft:magma_block"]
# Cooking Pot numbers, shared with Java (agriculture/CookingPotBlockEntity.java).
POT_INPUTS = 6       # ingredient slots
POT_OUTPUTS = 4      # result slots after them: soups do not stack, so a pot cooks up to four bowls in a row
POT_COOLING = 2      # progress lost per tick without heat, like a furnace going out

# Sickles: right-click harvests every ripe crop in a square of side 2 * radius + 1. Low crops are
# replanted from their own drops; tall crops are picked and stay standing. 1 durability per use.
SICKLES = {
    "flint_sickle": {"display": "Flint Sickle", "radius": 1, "durability": 131, "material": "minecraft:flint",
                     "features": []},
    "bronze_sickle": {"display": "Bronze Sickle", "radius": 2, "durability": 350, "material": "#c:ingots/bronze",
                      "features": ["tin"]},
}
SICKLE_PATTERN = [" M ", "  M", "SM "]

# Cooking: every cooked food works in the furnace, smoker and on a campfire (vanilla timings).
COOKING = {
    "pan_de_muerto": {"input": "pan_de_muerto_dough", "xp": 0.35},
    "roasted_acorns": {"input": "acorn", "xp": 0.2},
    "roast_turkey": {"input": "raw_turkey", "xp": 0.35},
    "roasted_corn": {"input": "corn", "xp": 0.35},
    "popcorn": {"input": "corn_kernels", "xp": 0.1},
    "baked_sweet_potato": {"input": "sweet_potato", "xp": 0.35},
    "roasted_sunflower_seeds": {"input": "sunflower_seeds", "xp": 0.1},
    "roasted_chestnuts": {"input": "chestnut", "xp": 0.35},
    "baked_acorn_squash": {"input": "acorn_squash", "xp": 0.35},
    "roasted_pumpkin_seeds": {"input": "minecraft:pumpkin_seeds", "xp": 0.1},
    # Halloween harvest. Sugar melts into caramel; a bottle gourd dries hard enough to hollow out.
    "caramel": {"input": "minecraft:sugar", "xp": 0.1},
    "dried_bottle_gourd": {"input": "bottle_gourd", "xp": 0.1, "category": "misc"},
    # Fall additions 12: wild mushrooms.
    "sauteed_chanterelles": {"input": "chanterelle", "xp": 0.35},
    "roasted_porcini": {"input": "porcini", "xp": 0.35},
    "fried_puffball": {"input": "puffball", "xp": 0.35},
}
COOK_TIMES = {"smelting": 200, "smoking": 100, "campfire_cooking": 600}

# Cooking Pot recipes (recipe type jugcraft:pot_cooking): any slot order, one serving per batch, and
# ingredients may be stacked for batch cooking. Every filled slot must belong to the recipe. No two
# recipes may share the same set of ingredient items. time: ticks with heat.
POT_RECIPES = {
    "tomato_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:tomato": 2, "jugcraft:onion": 1}, "time": 200},
    "onion_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:onion": 2, "jugcraft:garlic": 1, "minecraft:bread": 1}, "time": 200},
    "vegetable_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:cabbage": 1, "minecraft:carrot": 1, "minecraft:potato": 1,
                                  "jugcraft:onion": 1}, "time": 200},
    "mushroom_barley_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:barley": 1, "minecraft:brown_mushroom": 1,
                                        "jugcraft:onion": 1}, "time": 200},
    "oat_porridge": {"inputs": {"minecraft:bowl": 1, "jugcraft:oats": 2, "minecraft:sugar": 1}, "time": 200},
    "chili": {"inputs": {"minecraft:bowl": 1, "jugcraft:beans": 1, "jugcraft:tomato": 1, "jugcraft:pepper": 1,
                         "jugcraft:onion": 1, "minecraft:beef": 1}, "time": 300},
    "cabbage_rolls": {"inputs": {"jugcraft:cabbage": 1, "minecraft:beef": 1, "jugcraft:onion": 1, "jugcraft:garlic": 1},
                      "count": 2, "time": 300},
    # Festival crops.
    "butternut_squash_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:butternut_squash": 1, "jugcraft:onion": 1,
                                         "jugcraft:garlic": 1}, "time": 200},
    "harvest_stew": {"inputs": {"minecraft:bowl": 1, "jugcraft:turnip": 1, "minecraft:carrot": 1, "jugcraft:onion": 1,
                                "minecraft:mutton": 1}, "time": 300},
    "cranberry_sauce": {"inputs": {"minecraft:bowl": 1, "jugcraft:cranberries": 2, "minecraft:sugar": 1}, "time": 200},
    # Halloween harvest: the guts scooped from carved pumpkins make soup.
    "pumpkin_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:pumpkin_guts": 2, "jugcraft:onion": 1}, "time": 200},
    # Pumpkin Night (tools/decor20.py DISHES).
    "red_kuri_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:red_kuri_pumpkin": 1, "jugcraft:onion": 1, "jugcraft:garlic": 1},
                      "time": 200},
    "kabocha_tempura": {"inputs": {"jugcraft:kabocha_pumpkin": 1, "minecraft:wheat": 1, "minecraft:egg": 1}, "count": 2, "time": 200},
    # Spooky sweets: sugar boiled with a pinch of something odd, four pieces a batch.
    "glow_gum": {"inputs": {"minecraft:sugar": 2, "minecraft:glow_berries": 1, "minecraft:slime_ball": 1}, "count": 4, "time": 200},
    "ghost_taffy": {"inputs": {"minecraft:sugar": 2, "minecraft:phantom_membrane": 1}, "count": 4, "time": 200},
    "fizz_rocks": {"inputs": {"minecraft:sugar": 2, "minecraft:gunpowder": 1}, "count": 4, "time": 200},
    "witchs_licorice": {"inputs": {"minecraft:sugar": 2, "minecraft:wheat": 1, "minecraft:ink_sac": 1}, "count": 4, "time": 200},
    # Fall additions 2: sparkling cider simmered with mulling spices, a bottle a batch.
    "mulled_cider": {"inputs": {"jugcraft:sparkling_cider": 1, "jugcraft:mulling_spices": 1}, "time": 200},
    # Fall additions 3: cider vinegar (aged cider soured on pomace); preserves, each cooked down into a Mason Jar: jams and
    # fruit butters with sugar (apple butter cooked in sweet cider, pumpkin butter with mulling spices), and pickles and
    # relish in cider vinegar.
    "cider_vinegar": {"inputs": {"jugcraft:aged_cider": 1, "jugcraft:apple_pomace": 1}, "time": 200},
    "sweet_berry_jam": {"inputs": {"jugcraft:mason_jar": 1, "minecraft:sweet_berries": 6, "minecraft:sugar": 2}, "time": 300},
    "apple_butter": {"inputs": {"jugcraft:mason_jar": 1, "minecraft:apple": 3, "minecraft:sugar": 1, "jugcraft:sweet_cider": 1}, "time": 300},
    "pumpkin_butter": {"inputs": {"jugcraft:mason_jar": 1, "minecraft:pumpkin": 1, "minecraft:sugar": 2, "jugcraft:mulling_spices": 1},
                       "time": 300},
    "cranberry_preserves": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:cranberries": 6, "minecraft:sugar": 2}, "time": 300},
    "glow_berry_jelly": {"inputs": {"jugcraft:mason_jar": 1, "minecraft:glow_berries": 6, "minecraft:sugar": 2}, "time": 300},
    # Fall additions 12: the three edible wild mushrooms stewed with a potato.
    "foragers_stew": {"inputs": {"minecraft:bowl": 1, "jugcraft:chanterelle": 1, "jugcraft:porcini": 1, "jugcraft:puffball": 1,
                                 "minecraft:potato": 1}, "time": 200},
    # Bunker interiors (batch 59, tools/bunkerworks.py TRENCH_STEW).
    "trench_stew": {"inputs": {"minecraft:bowl": 1, "minecraft:beef": 1, "minecraft:potato": 1, "minecraft:carrot": 1}, "time": 300},
    "pickled_beets": {"inputs": {"jugcraft:mason_jar": 1, "minecraft:beetroot": 4, "jugcraft:cider_vinegar": 1}, "time": 300},
    "pickled_peppers": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:pepper": 4, "jugcraft:cider_vinegar": 1}, "time": 300},
    "corn_relish": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:corn": 2, "jugcraft:pepper": 1, "jugcraft:onion": 1,
                               "jugcraft:cider_vinegar": 1}, "time": 300},
}
# The mooncakes (the sky lantern festival, above) bake in the pot too.
for _cake, _info in LANTERNS["mooncakes"].items():
    POT_RECIPES[_cake] = {"inputs": {**LANTERNS["mooncake_base"], **_info["filling"]}, "time": LANTERNS["mooncake_time"],
                          "count": LANTERNS["mooncake_count"]}

# Crafting. result: an ID (jugcraft unless namespaced) and count. features: switches besides agriculture.
SHAPELESS = [
    # Fall additions 13: bat guano is rich in phosphate.
    {"id": "phosphate_from_bat_guano", "inputs": ["jugcraft:bat_guano"] * 4, "result": "phosphate", "count": 1, "category": "misc",
     "features": ["phosphate"]},
    # Fall additions 3: a canning kettle is a cauldron enamelled blue, with an iron-bar jar rack.
    {"id": "canning_kettle", "inputs": ["minecraft:cauldron", "minecraft:blue_dye", "minecraft:iron_bars"], "result": "canning_kettle",
     "count": 1, "category": "misc"},
    # Fall additions 2: mulling spices; apple seeds picked out of pomace; apple cider donuts (the cider's bottle comes back).
    {"id": "mulling_spices", "inputs": ["minecraft:sugar", "minecraft:sweet_berries", "minecraft:cocoa_beans"], "result": "mulling_spices",
     "count": 2, "category": "misc"},
    {"id": "apple_seeds", "inputs": ["jugcraft:apple_pomace"], "result": "apple_seeds", "count": 1, "category": "misc"},
    {"id": "apple_cider_donut", "inputs": ["minecraft:wheat", "minecraft:wheat", "minecraft:sugar", "#minecraft:eggs", "jugcraft:sweet_cider"],
     "result": "apple_cider_donut", "count": 4, "category": "misc"},
    # Decorations batch 14: cat ears on a headband with a tail of black wool.
    {"id": "cat_ears_and_tail", "inputs": ["minecraft:black_wool", "minecraft:black_wool", "minecraft:string", "minecraft:pink_dye"],
     "result": "cat_ears_and_tail", "count": 1, "category": "misc"},
    # Decorations batch 13: soul cakes with currants; pumpkin bread; chocolate cupcakes iced with a web; chocolate
    # bat-wing cookies; a pumpkin spice latte (the milk bucket is given back).
    {"id": "soul_cake", "inputs": ["minecraft:wheat", "minecraft:sugar", "#minecraft:eggs", "minecraft:sweet_berries"], "result": "soul_cake",
     "count": 3, "category": "misc"},
    {"id": "pumpkin_bread", "inputs": ["minecraft:wheat", "minecraft:wheat", "minecraft:pumpkin", "minecraft:sugar", "#minecraft:eggs"],
     "result": "pumpkin_bread", "count": 2, "category": "misc"},
    {"id": "spiderweb_cupcake", "inputs": ["minecraft:wheat", "minecraft:sugar", "#minecraft:eggs", "minecraft:cocoa_beans"],
     "result": "spiderweb_cupcake", "count": 4, "category": "misc"},
    {"id": "bat_wing_cookie", "inputs": ["minecraft:wheat", "minecraft:wheat", "minecraft:cocoa_beans", "minecraft:sugar"],
     "result": "bat_wing_cookie", "count": 8, "category": "misc"},
    {"id": "pumpkin_spice_latte", "inputs": ["minecraft:milk_bucket", "minecraft:pumpkin", "minecraft:sugar", "minecraft:cocoa_beans",
                                              "minecraft:glass_bottle"],
     "result": "pumpkin_spice_latte", "count": 1, "category": "misc"},
    # Decorations batch 12: three paper round a stick make toilet paper; sugar whipped with an egg makes marshmallows,
    # one goes on a stick to toast.
    {"id": "toilet_paper_roll", "inputs": ["minecraft:paper", "minecraft:paper", "minecraft:paper", "minecraft:stick"],
     "result": "toilet_paper_roll", "count": 4, "category": "misc"},
    {"id": "marshmallow", "inputs": ["minecraft:sugar", "minecraft:sugar", "#minecraft:eggs"], "result": "marshmallow", "count": 4,
     "category": "misc"},
    {"id": "marshmallow_on_a_stick", "inputs": ["jugcraft:marshmallow", "minecraft:stick"], "result": "marshmallow_on_a_stick", "count": 1,
     "category": "misc"},
    # Decorations batch 11: a pumpkin weighted with an iron nugget.
    {"id": "bowling_pumpkin", "inputs": ["minecraft:pumpkin", "minecraft:iron_nugget"], "result": "bowling_pumpkin", "count": 1,
     "category": "misc"},
    # Decorations batch 10: glow ink thinned with bone meal.
    {"id": "glow_paint", "inputs": ["minecraft:glow_ink_sac", "minecraft:bone_meal"], "result": "glow_paint", "count": 4,
     "category": "building"},
    {"id": "corn_kernels", "inputs": ["jugcraft:corn"], "result": "corn_kernels", "count": 2},
    {"id": "three_sisters_stew", "inputs": ["minecraft:bowl", "jugcraft:corn", "jugcraft:beans", "minecraft:pumpkin"],
     "result": "three_sisters_stew", "count": 1},
    {"id": "string_from_flax", "inputs": ["jugcraft:flax", "jugcraft:flax"], "result": "minecraft:string", "count": 1},
    {"id": "tomato_seeds", "inputs": ["jugcraft:tomato"], "result": "tomato_seeds", "count": 2},
    {"id": "pepper_seeds", "inputs": ["jugcraft:pepper"], "result": "pepper_seeds", "count": 2},
    {"id": "garden_salad", "inputs": ["minecraft:bowl", "jugcraft:cabbage", "jugcraft:tomato", "jugcraft:pepper"],
     "result": "garden_salad", "count": 1},
    # Jugcraft's salt (the mining branch) pickles cabbage.
    {"id": "sauerkraut", "inputs": ["jugcraft:cabbage", "jugcraft:cabbage", "#c:dusts/salt"], "result": "sauerkraut", "count": 2,
     "features": ["salt"]},
    # Festival crops. A gourd gives four seeds, like a vanilla pumpkin.
    {"id": "butternut_squash_seeds", "inputs": ["jugcraft:butternut_squash"], "result": "butternut_squash_seeds", "count": 4},
    {"id": "acorn_squash_seeds", "inputs": ["jugcraft:acorn_squash"], "result": "acorn_squash_seeds", "count": 4},
    {"id": "warty_gourd_seeds", "inputs": ["jugcraft:warty_gourd"], "result": "warty_gourd_seeds", "count": 4},
    {"id": "squash_pie", "inputs": ["jugcraft:butternut_squash", "minecraft:sugar", "#minecraft:eggs"], "result": "squash_pie",
     "count": 1},
    {"id": "candy_corn", "inputs": ["jugcraft:corn", "minecraft:sugar", "minecraft:honey_bottle"], "result": "candy_corn",
     "count": 4},
    *[{"id": f"{wood}_planks", "inputs": [f"#{WOOD_TAGS[wood]}"], "result": f"{wood}_planks", "count": 4,
       "category": "building", "group": "planks", "switch": WOOD_SWITCHES[wood]} for wood in WOOD_SETS],
    # Halloween harvest.
    {"id": "white_pumpkin_seeds", "inputs": ["jugcraft:white_pumpkin"], "result": "white_pumpkin_seeds", "count": 4},
    {"id": "jarrahdale_pumpkin_seeds", "inputs": ["jugcraft:jarrahdale_pumpkin"], "result": "jarrahdale_pumpkin_seeds", "count": 4},
    {"id": "cinderella_pumpkin_seeds", "inputs": ["jugcraft:cinderella_pumpkin"], "result": "cinderella_pumpkin_seeds", "count": 4},
    {"id": "red_kuri_pumpkin_seeds", "inputs": ["jugcraft:red_kuri_pumpkin"], "result": "red_kuri_pumpkin_seeds", "count": 4},
    {"id": "kabocha_pumpkin_seeds", "inputs": ["jugcraft:kabocha_pumpkin"], "result": "kabocha_pumpkin_seeds", "count": 4},
    {"id": "bottle_gourd_seeds", "inputs": ["jugcraft:bottle_gourd"], "result": "bottle_gourd_seeds", "count": 4},
    {"id": "pumpkin_pie_from_heirloom_pumpkins", "inputs": ["#jugcraft:heirloom_pumpkins", "minecraft:sugar", "#minecraft:eggs"],
     "result": "minecraft:pumpkin_pie", "count": 1},
    {"id": "ornamental_corn_kernels", "inputs": ["jugcraft:ornamental_corn"], "result": "ornamental_corn_kernels", "count": 2},
    {"id": "ornamental_corn_bundle", "inputs": ["jugcraft:ornamental_corn", "jugcraft:ornamental_corn", "jugcraft:ornamental_corn",
                                                "minecraft:string"], "result": "ornamental_corn_bundle", "count": 1, "category": "building"},
    {"id": "caramel_apple", "inputs": ["minecraft:apple", "jugcraft:caramel", "minecraft:stick"], "result": "caramel_apple", "count": 1},
    {"id": "popcorn_ball", "inputs": ["jugcraft:popcorn", "jugcraft:popcorn", "jugcraft:caramel"], "result": "popcorn_ball", "count": 1},
    {"id": "gourd_canteen", "inputs": ["jugcraft:dried_bottle_gourd", "minecraft:leather"], "result": "gourd_canteen", "count": 1,
     "category": "equipment"},
    {"id": "blank_stencil", "inputs": ["minecraft:paper", "minecraft:paper"], "result": "blank_stencil", "count": 1},
    # Trick-or-treating: a paper bag with a string handle, dyed orange.
    {"id": "candy_bag", "inputs": ["minecraft:paper", "minecraft:paper", "minecraft:string", "minecraft:orange_dye"],
     "result": "candy_bag", "count": 1, "category": "equipment"},
    {"id": "yellow_dye_from_yellow_mum", "inputs": ["jugcraft:yellow_mum"], "result": "minecraft:yellow_dye", "count": 1, "group": "yellow_dye"},
    {"id": "orange_dye_from_orange_mum", "inputs": ["jugcraft:orange_mum"], "result": "minecraft:orange_dye", "count": 1, "group": "orange_dye"},
    {"id": "red_dye_from_red_mum", "inputs": ["jugcraft:red_mum"], "result": "minecraft:red_dye", "count": 1, "group": "red_dye"},
    {"id": "orange_dye_from_marigold", "inputs": ["jugcraft:marigold"], "result": "minecraft:orange_dye", "count": 1, "group": "orange_dye"},
    {"id": "purple_dye_from_purple_mum", "inputs": ["jugcraft:purple_mum"], "result": "minecraft:purple_dye", "count": 1,
     "group": "purple_dye"},
    # Halloween festivities: a sheet ghost on a string, and a candle melted onto a bone skull.
    {"id": "hanging_ghost", "inputs": ["minecraft:white_wool", "minecraft:string", "minecraft:black_dye"], "result": "hanging_ghost",
     "count": 2, "category": "building"},
    {"id": "candle_skull", "inputs": ["minecraft:bone_block", "minecraft:candle"], "result": "candle_skull", "count": 1,
     "category": "building"},
    # Halloween decorations: a strand of tiny pumpkin bulbs, and a painting whose eyes are a spider's.
    {"id": "jack_o_lantern_string_lights", "inputs": ["minecraft:string", "minecraft:string", "minecraft:glowstone_dust", "minecraft:orange_dye"],
     "result": "jack_o_lantern_string_lights", "count": 1, "category": "building"},
    {"id": "haunted_portrait", "inputs": ["minecraft:painting", "minecraft:gold_nugget", "minecraft:spider_eye"], "result": "haunted_portrait",
     "count": 1, "category": "building"},
    # Decorations batch 2: a paper bag with sand and a candle; a candle with a feather to float it; a torch in a bony
    # hand; pennants and paper bats on a string.
    {"id": "luminaria", "inputs": ["minecraft:paper", "minecraft:paper", "minecraft:sand", "minecraft:candle"], "result": "luminaria",
     "count": 1, "category": "building"},
    {"id": "floating_candle", "inputs": ["minecraft:candle", "minecraft:feather"], "result": "floating_candle", "count": 1,
     "category": "building"},
    {"id": "skeleton_hand_sconce", "inputs": ["minecraft:torch", "minecraft:bone", "minecraft:bone"], "result": "skeleton_hand_sconce",
     "count": 1, "category": "building"},
    {"id": "bat_bunting", "inputs": ["minecraft:string", "minecraft:paper", "minecraft:paper", "minecraft:orange_dye", "minecraft:black_dye"],
     "result": "bat_bunting", "count": 1, "category": "building"},
    # Decorations batch 3: stone bricks aged with bone meal into crypt stone; earth heaped over rotten flesh.
    {"id": "crypt_stone", "inputs": ["minecraft:stone_bricks", "minecraft:stone_bricks", "minecraft:stone_bricks", "minecraft:stone_bricks",
                                     "minecraft:bone_meal"], "result": "crypt_stone", "count": 4, "category": "building"},
    {"id": "grave_mound", "inputs": ["minecraft:dirt", "minecraft:dirt", "minecraft:rotten_flesh"], "result": "grave_mound", "count": 1,
     "category": "building"},
    # Decorations batch 5: four leaves (any) and a dye heap into four leaf piles of that colour.
    {"id": "red_leaf_pile", "inputs": ["#minecraft:leaves", "#minecraft:leaves", "#minecraft:leaves", "#minecraft:leaves", "minecraft:red_dye"],
     "result": "red_leaf_pile", "count": 4, "category": "building"},
    {"id": "orange_leaf_pile", "inputs": ["#minecraft:leaves", "#minecraft:leaves", "#minecraft:leaves", "#minecraft:leaves", "minecraft:orange_dye"],
     "result": "orange_leaf_pile", "count": 4, "category": "building"},
    {"id": "yellow_leaf_pile", "inputs": ["#minecraft:leaves", "#minecraft:leaves", "#minecraft:leaves", "#minecraft:leaves", "minecraft:yellow_dye"],
     "result": "yellow_leaf_pile", "count": 4, "category": "building"},
    # A hay block and string tie two bale seats.
    {"id": "hay_bale_seat", "inputs": ["minecraft:hay_block", "minecraft:string"], "result": "hay_bale_seat", "count": 2,
     "category": "building"},
    # Decorations batch 6: two spider eyes and a glow ink sac make two pairs of lurking eyes; a pane, paper and dyes a
    # silhouette window.
    {"id": "lurking_eyes", "inputs": ["minecraft:spider_eye", "minecraft:spider_eye", "minecraft:glow_ink_sac"], "result": "lurking_eyes",
     "count": 2, "category": "building"},
    {"id": "silhouette_window", "inputs": ["minecraft:glass_pane", "minecraft:paper", "minecraft:orange_dye", "minecraft:black_dye"],
     "result": "silhouette_window", "count": 1, "category": "building"},
    # The biomes branch's wild flowers make dye (tools/plants.py).
    *plants.dye_recipes(),
]
SHAPED = [
    {"id": "barley_bread", "pattern": ["BBB"], "key": {"B": "jugcraft:barley"}, "result": "barley_bread", "count": 1,
     "category": "misc"},
    {"id": "trellis", "pattern": ["S S", " S ", "S S"], "key": {"S": "minecraft:stick"}, "result": "trellis", "count": 2,
     "category": "misc"},
    {"id": "cooking_pot", "pattern": ["S S", "I I", "III"], "key": {"S": "minecraft:stick", "I": "#c:ingots/iron"},
     "result": "cooking_pot", "count": 1, "category": "misc"},
    # Festival crops: the lantern (like vanilla's jack o'lantern recipe) and the chestnut wood set (like oak's).
    {"id": "turnip_lantern", "pattern": ["T", "B"], "key": {"T": "jugcraft:turnip", "B": "minecraft:torch"},
     "result": "turnip_lantern", "count": 1, "category": "building"},
    *[dict(recipe, switch=WOOD_SWITCHES[wood]) for wood in WOOD_SETS for recipe in (
        {"id": f"{wood}_wood", "pattern": ["##", "##"], "key": {"#": f"jugcraft:{wood}_log"}, "result": f"{wood}_wood",
         "count": 3, "category": "building", "group": "bark"},
        {"id": f"stripped_{wood}_wood", "pattern": ["##", "##"], "key": {"#": f"jugcraft:stripped_{wood}_log"},
         "result": f"stripped_{wood}_wood", "count": 3, "category": "building", "group": "bark"},
        {"id": f"{wood}_stairs", "pattern": ["#  ", "## ", "###"], "key": {"#": f"jugcraft:{wood}_planks"}, "result": f"{wood}_stairs",
         "count": 4, "category": "building", "group": "wooden_stairs"},
        {"id": f"{wood}_slab", "pattern": ["###"], "key": {"#": f"jugcraft:{wood}_planks"}, "result": f"{wood}_slab",
         "count": 6, "category": "building", "group": "wooden_slab"},
        {"id": f"{wood}_fence", "pattern": ["W#W", "W#W"], "key": {"W": f"jugcraft:{wood}_planks", "#": "minecraft:stick"},
         "result": f"{wood}_fence", "count": 3, "category": "misc", "group": "wooden_fence"},
        {"id": f"{wood}_fence_gate", "pattern": ["#W#", "#W#"], "key": {"W": f"jugcraft:{wood}_planks", "#": "minecraft:stick"},
         "result": f"{wood}_fence_gate", "count": 1, "category": "redstone", "group": "wooden_fence_gate"})],
    # Halloween harvest: a platform scale with a clock for a dial, a straw man on a post, a stook of stalks
    # and a hollowed gourd on a string.
    {"id": "harvest_scale", "pattern": [" C ", "III", "PPP"], "key": {"C": "minecraft:clock", "I": "#c:ingots/iron",
                                                                      "P": "#minecraft:planks"},
     "result": "harvest_scale", "count": 1, "category": "misc"},
    {"id": "scarecrow", "pattern": [" W ", "SHS", " S "], "key": {"W": "#minecraft:wool", "S": "minecraft:stick", "H": "minecraft:hay_block"},
     "result": "scarecrow", "count": 1, "category": "building"},
    {"id": "corn_shock", "pattern": [" T ", "SSS", "S S"], "key": {"T": "minecraft:string", "S": "jugcraft:corn_stalks"},
     "result": "corn_shock", "count": 1, "category": "building"},
    {"id": "gourd_birdhouse", "pattern": ["T", "G"], "key": {"T": "minecraft:string", "G": "jugcraft:dried_bottle_gourd"},
     "result": "gourd_birdhouse", "count": 1, "category": "building"},
    # The regatta: a chequered flag on a pole, and red-and-white buoys of planks.
    {"id": "regatta_flag", "pattern": ["SWB", "SBW", "S  "], "key": {"S": "minecraft:stick", "W": "minecraft:white_wool",
                                                                    "B": "minecraft:black_wool"},
     "result": "regatta_flag", "count": 1, "category": "misc"},
    {"id": "regatta_buoy", "pattern": [" R ", "PWP"], "key": {"R": "minecraft:red_dye", "W": "minecraft:white_wool", "P": "#minecraft:planks"},
     "result": "regatta_buoy", "count": 4, "category": "misc"},
    # Costumes: a pointed witch's hat with a purple band, a sheet with eye holes, a straw hat.
    {"id": "witch_hat", "pattern": [" W ", " W ", "WDW"], "key": {"W": "minecraft:black_wool", "D": "minecraft:purple_dye"},
     "result": "witch_hat", "count": 1, "category": "equipment"},
    {"id": "ghost_sheet", "pattern": ["WWW", "WBW"], "key": {"W": "minecraft:white_wool", "B": "minecraft:black_dye"},
     "result": "ghost_sheet", "count": 1, "category": "equipment"},
    {"id": "scarecrow_hat", "pattern": [" W ", "WWW"], "key": {"W": "minecraft:wheat"},
     "result": "scarecrow_hat", "count": 1, "category": "equipment"},
    # Halloween festivities: a pedestal draped in purple for the carving contest, and cobwebs spun from string.
    {"id": "judging_stand", "pattern": ["WWW", " P ", "PPP"], "key": {"W": "minecraft:purple_wool", "P": "#minecraft:planks"},
     "result": "judging_stand", "count": 1, "category": "misc"},
    {"id": "spun_cobweb", "pattern": ["S S", " S ", "S S"], "key": {"S": "minecraft:string"}, "result": "spun_cobweb", "count": 2,
     "category": "building"},
    # Halloween nights: a log arm with a string sling and an iron counterweight on a plank frame.
    {"id": "trebuchet", "pattern": ["  S", "LLI", "PPP"], "key": {"S": "minecraft:string", "L": "#minecraft:logs", "I": "#c:ingots/iron",
                                                                 "P": "#minecraft:planks"},
     "result": "trebuchet", "count": 1, "category": "misc"},
    # Halloween decorations: an iron hook for string lights; a bowl of orange terracotta; a plank coffin lined in red
    # around a chest; a fog machine of iron with a bucket for its tank, copper heaters and a grille nozzle.
    {"id": "string_light_hook", "pattern": ["N", "I"], "key": {"N": "minecraft:iron_nugget", "I": "#c:ingots/iron"},
     "result": "string_light_hook", "count": 4, "category": "building"},
    {"id": "candy_bowl", "pattern": ["T T", " T "], "key": {"T": "minecraft:orange_terracotta"}, "result": "candy_bowl", "count": 1,
     "category": "misc"},
    {"id": "coffin", "pattern": ["PWP", "PCP"], "key": {"P": "#minecraft:planks", "W": "minecraft:red_wool", "C": "minecraft:chest"},
     "result": "coffin", "count": 1, "category": "misc"},
    {"id": "fog_machine", "pattern": ["IGI", "CBC", "III"], "key": {"I": "#c:ingots/iron", "G": "minecraft:iron_bars",
                                                                   "C": "jugcraft:copper_cable", "B": "minecraft:bucket"},
     "result": "fog_machine", "count": 1, "category": "redstone"},
    # Decorations batch 3: iron bars and ingots for the cemetery fence and gate; crypt stone stood up as pillars and
    # hung as a door; a calcite angel with a feather's wings on a stone plinth; a skeleton of bones on a slime-ball
    # spring in a plank crate.
    {"id": "cemetery_fence", "pattern": ["BIB", "BIB"], "key": {"B": "minecraft:iron_bars", "I": "#c:ingots/iron"}, "result": "cemetery_fence",
     "count": 6, "category": "building"},
    {"id": "cemetery_gate", "pattern": ["IBI", "IBI"], "key": {"B": "minecraft:iron_bars", "I": "#c:ingots/iron"}, "result": "cemetery_gate",
     "count": 1, "category": "redstone"},
    {"id": "crypt_stone_pillar", "pattern": ["C", "C"], "key": {"C": "jugcraft:crypt_stone"}, "result": "crypt_stone_pillar", "count": 2,
     "category": "building"},
    {"id": "crypt_door", "pattern": ["CC", "CC", "CC"], "key": {"C": "jugcraft:crypt_stone"}, "result": "crypt_door", "count": 3,
     "category": "redstone"},
    {"id": "mourning_angel", "pattern": [" C ", "CFC", "SSS"], "key": {"C": "minecraft:calcite", "F": "minecraft:feather",
                                                                    "S": "minecraft:stone_bricks"},
     "result": "mourning_angel", "count": 1, "category": "building"},
    {"id": "pop_up_skeleton", "pattern": ["PBP", "BSB", "PPP"], "key": {"P": "#minecraft:planks", "B": "minecraft:bone",
                                                                     "S": "minecraft:slime_ball"},
     "result": "pop_up_skeleton", "count": 1, "category": "redstone"},
    # Decorations batch 4: a cauldron on iron legs; glass bottles on slab shelves; an amethyst-hearted glass orb on a
    # gold stand; a feathered book on a slab stand; a wheat besom on a stick.
    {"id": "bubbling_cauldron", "pattern": [" C ", "I I"], "key": {"C": "minecraft:cauldron", "I": "#c:ingots/iron"},
     "result": "bubbling_cauldron", "count": 1, "category": "misc"},
    {"id": "apothecary_shelf", "pattern": ["BBB", "SSS"], "key": {"B": "minecraft:glass_bottle", "S": "#minecraft:wooden_slabs"},
     "result": "apothecary_shelf", "count": 1, "category": "building"},
    {"id": "crystal_ball", "pattern": [" G ", "GAG", " N "], "key": {"G": "minecraft:glass", "A": "minecraft:amethyst_shard",
                                                                  "N": "minecraft:gold_ingot"},
     "result": "crystal_ball", "count": 1, "category": "misc"},
    {"id": "grimoire_stand", "pattern": ["FBF", " S ", "SSS"], "key": {"F": "minecraft:feather", "B": "minecraft:book",
                                                                    "S": "#minecraft:wooden_slabs"},
     "result": "grimoire_stand", "count": 1, "category": "misc"},
    {"id": "witchs_broom", "pattern": ["  S", " S ", "W  "], "key": {"S": "minecraft:stick", "W": "minecraft:wheat"}, "result": "witchs_broom",
     "count": 1, "category": "misc"},
    # Decorations batch 5: a plank tub of water (the bucket comes back); a crate of slabs; a wreath of chestnut leaves
    # round an ear of ornamental corn, with a mum that sets its flowers.
    {"id": "bobbing_tub", "pattern": ["PWP", "PPP"], "key": {"P": "#minecraft:planks", "W": "minecraft:water_bucket"},
     "result": "bobbing_tub", "count": 1, "category": "misc"},
    {"id": "pumpkin_crate", "pattern": ["S S", "SSS"], "key": {"S": "#minecraft:wooden_slabs"}, "result": "pumpkin_crate", "count": 1,
     "category": "building"},
    {"id": "autumn_wreath_yellow", "pattern": ["LCL", "L L", "LML"], "key": {"L": "jugcraft:chestnut_leaves", "C": "jugcraft:ornamental_corn",
                                                                          "M": "jugcraft:yellow_mum"},
     "result": "autumn_wreath", "count": 1, "category": "building", "group": "autumn_wreath",
     "components": {"minecraft:block_state": {"flowers": "yellow"}}},
    {"id": "autumn_wreath_orange", "pattern": ["LCL", "L L", "LML"], "key": {"L": "jugcraft:chestnut_leaves", "C": "jugcraft:ornamental_corn",
                                                                          "M": "jugcraft:orange_mum"},
     "result": "autumn_wreath", "count": 1, "category": "building", "group": "autumn_wreath",
     "components": {"minecraft:block_state": {"flowers": "orange"}}},
    {"id": "autumn_wreath_red", "pattern": ["LCL", "L L", "LML"], "key": {"L": "jugcraft:chestnut_leaves", "C": "jugcraft:ornamental_corn",
                                                                          "M": "jugcraft:red_mum"},
     "result": "autumn_wreath", "count": 1, "category": "building", "group": "autumn_wreath",
     "components": {"minecraft:block_state": {"flowers": "red"}}},
    {"id": "autumn_wreath_purple", "pattern": ["LCL", "L L", "LML"], "key": {"L": "jugcraft:chestnut_leaves", "C": "jugcraft:ornamental_corn",
                                                                          "M": "jugcraft:purple_mum"},
     "result": "autumn_wreath", "count": 1, "category": "building", "group": "autumn_wreath",
     "components": {"minecraft:block_state": {"flowers": "purple"}}},
    # Decorations batch 6: a planked chair with a wool cushion on stick legs; a slab box round a note block with a
    # gold catch; a black-wool spider on string legs with a spider eye.
    {"id": "rocking_chair", "pattern": ["P  ", "PWP", "S S"], "key": {"P": "#minecraft:planks", "W": "#minecraft:wool", "S": "minecraft:stick"},
     "result": "rocking_chair", "count": 1, "category": "building"},
    {"id": "music_box", "pattern": ["SGS", "SNS"], "key": {"S": "#minecraft:wooden_slabs", "G": "minecraft:gold_nugget", "N": "minecraft:note_block"},
     "result": "music_box", "count": 1, "category": "redstone"},
    {"id": "giant_fake_spider", "pattern": ["S S", "WEW", "S S"], "key": {"S": "minecraft:string", "W": "minecraft:black_wool",
                                                                       "E": "minecraft:spider_eye"},
     "result": "giant_fake_spider", "count": 1, "category": "building"},
    # Decorations batch 7: candles on an iron ring; iron pipes over a note block and bone keys in a plank case; iron
    # plates on an armor stand; white carpet for a sheet; a gold-framed pane over soul sand; string cheesecloth on an
    # iron-nugget rod; a clay-headed doll in a wool dress.
    {"id": "haunted_chandelier", "pattern": ["NIN", "CCC"], "key": {"N": "minecraft:iron_nugget", "I": "#c:ingots/iron", "C": "#minecraft:candles"},
     "result": "haunted_chandelier", "count": 1, "category": "building"},
    {"id": "phantom_pipe_organ", "pattern": ["III", "BNB", "PPP"], "key": {"I": "#c:ingots/iron", "B": "minecraft:bone", "N": "minecraft:note_block",
                                                                        "P": "#minecraft:planks"},
     "result": "phantom_pipe_organ", "count": 1, "category": "redstone"},
    {"id": "suit_of_armor", "pattern": [" I ", "IAI", " I "], "key": {"I": "#c:ingots/iron", "A": "minecraft:armor_stand"},
     "result": "suit_of_armor", "count": 1, "category": "building"},
    {"id": "dust_sheet", "pattern": ["CCC"], "key": {"C": "minecraft:white_carpet"}, "result": "dust_sheet", "count": 1, "category": "building"},
    {"id": "spirit_mirror", "pattern": ["NNN", "NPN", "NSN"], "key": {"N": "minecraft:gold_nugget", "P": "minecraft:glass_pane",
                                                                    "S": "minecraft:soul_sand"},
     "result": "spirit_mirror", "count": 1, "category": "building"},
    {"id": "tattered_curtains", "pattern": ["NNN", "SSS", "SSS"], "key": {"N": "minecraft:iron_nugget", "S": "minecraft:string"},
     "result": "tattered_curtains", "count": 3, "category": "building"},
    {"id": "creepy_doll", "pattern": [" C ", "WSW"], "key": {"C": "minecraft:clay_ball", "W": "#minecraft:wool", "S": "minecraft:string"},
     "result": "creepy_doll", "count": 1, "category": "building"},
    # Decorations batch 8: a lightning rod over copper coils and cable on an iron base; a wool-sheeted patient on an
    # iron table; slime in a glass jar under an iron lid; a sandstone case round a paper-wrapped mummy; a feathered raven
    # on a stick perch; a black terracotta cat with glowing eyes.
    {"id": "tesla_coil", "pattern": [" R ", "CWC", "III"], "key": {"R": "minecraft:lightning_rod", "C": "#c:ingots/copper",
                                                                "W": "jugcraft:copper_cable", "I": "#c:ingots/iron"},
     "result": "tesla_coil", "count": 1, "category": "redstone"},
    {"id": "lab_table", "pattern": ["WRW", "III", "I I"], "key": {"W": "minecraft:white_wool", "R": "minecraft:rotten_flesh", "I": "#c:ingots/iron"},
     "result": "lab_table", "count": 1, "category": "redstone"},
    {"id": "specimen_jar", "pattern": [" N ", "GSG", " G "], "key": {"N": "minecraft:iron_nugget", "G": "minecraft:glass",
                                                                  "S": "minecraft:slime_ball"},
     "result": "specimen_jar", "count": 1, "category": "building"},
    {"id": "mummy_sarcophagus", "pattern": ["GSG", "SPS", "SRS"], "key": {"G": "minecraft:gold_nugget", "S": "minecraft:sandstone",
                                                                       "P": "minecraft:paper", "R": "minecraft:rotten_flesh"},
     "result": "mummy_sarcophagus", "count": 1, "category": "redstone"},
    {"id": "raven_perch", "pattern": ["FBF", " S ", "SSS"], "key": {"F": "minecraft:feather", "B": "minecraft:black_dye", "S": "minecraft:stick"},
     "result": "raven_perch", "count": 1, "category": "building"},
    {"id": "black_cat_figure", "pattern": ["BGB", "BBB"], "key": {"B": "minecraft:black_terracotta", "G": "minecraft:glowstone_dust"},
     "result": "black_cat_figure", "count": 1, "category": "building"},
    # Decorations batch 9: wool figures over a redstone blower; a witch of wool round a cauldron with redstone inside;
    # rotten hands of bone in dirt; a skeleton of bones; bones hung on string from an iron hook; iron vanes painted
    # black or purple on an iron pole; a painted plank sign on a stick; mossy pillars under an iron arch with lanterns;
    # a dead tree of logs with lanterns and a jack o'lantern's face.
    {"id": "inflatable_ghost", "pattern": ["WDW", "WWW", "NRN"], "key": {"W": "minecraft:white_wool", "D": "minecraft:black_dye",
                                                                      "N": "minecraft:iron_nugget", "R": "minecraft:redstone"},
     "result": "inflatable_ghost", "count": 1, "category": "redstone"},
    {"id": "inflatable_cat", "pattern": ["WDW", "WWW", "NRN"], "key": {"W": "minecraft:black_wool", "D": "minecraft:yellow_dye",
                                                                    "N": "minecraft:iron_nugget", "R": "minecraft:redstone"},
     "result": "inflatable_cat", "count": 1, "category": "redstone"},
    {"id": "inflatable_pumpkin", "pattern": ["WDW", "WWW", "NRN"], "key": {"W": "minecraft:orange_wool", "D": "minecraft:purple_dye",
                                                                        "N": "minecraft:iron_nugget", "R": "minecraft:redstone"},
     "result": "inflatable_pumpkin", "count": 1, "category": "redstone"},
    {"id": "inflatable_spider", "pattern": ["WDW", "WWW", "NRN"], "key": {"W": "minecraft:black_wool", "D": "minecraft:spider_eye",
                                                                       "N": "minecraft:iron_nugget", "R": "minecraft:redstone"},
     "result": "inflatable_spider", "count": 1, "category": "redstone"},
    {"id": "porch_witch", "pattern": [" P ", "BRB", " C "], "key": {"P": "minecraft:purple_wool", "B": "minecraft:black_wool",
                                                                 "R": "minecraft:redstone", "C": "minecraft:cauldron"},
     "result": "porch_witch", "count": 1, "category": "redstone"},
    {"id": "grasping_hands", "pattern": ["BFB", " D "], "key": {"B": "minecraft:bone", "F": "minecraft:rotten_flesh", "D": "minecraft:dirt"},
     "result": "grasping_hands", "count": 1, "category": "building"},
    {"id": "poseable_skeleton", "pattern": [" B ", "BBB", "B B"], "key": {"B": "minecraft:bone"},
     "result": "poseable_skeleton", "count": 1, "category": "building"},
    {"id": "bone_wind_chimes", "pattern": [" N ", "SPS", "BBB"], "key": {"N": "minecraft:iron_nugget", "S": "minecraft:string",
                                                                      "P": "#minecraft:wooden_slabs", "B": "minecraft:bone"},
     "result": "bone_wind_chimes", "count": 1, "category": "building"},
    {"id": "bat_weathervane", "pattern": ["NDN", " I ", " I "], "key": {"N": "minecraft:iron_nugget", "D": "minecraft:black_dye",
                                                                     "I": "#c:ingots/iron"},
     "result": "bat_weathervane", "count": 1, "category": "building"},
    {"id": "witch_weathervane", "pattern": ["NDN", " I ", " I "], "key": {"N": "minecraft:iron_nugget", "D": "minecraft:purple_dye",
                                                                       "I": "#c:ingots/iron"},
     "result": "witch_weathervane", "count": 1, "category": "building"},
    {"id": "spooky_sign", "pattern": ["PPP", "PDP", " S "], "key": {"P": "#minecraft:planks", "D": "minecraft:red_dye", "S": "minecraft:stick"},
     "result": "spooky_sign", "count": 2, "category": "building"},
    {"id": "haunted_archway", "pattern": ["LIL", "CIC", "C C"], "key": {"L": "minecraft:lantern", "I": "#c:ingots/iron",
                                                                     "C": "minecraft:mossy_cobblestone"},
     "result": "haunted_archway", "count": 1, "category": "building"},
    {"id": "dead_hollow_tree", "pattern": ["LSL", "TJT", " T "], "key": {"L": "minecraft:lantern", "S": "minecraft:stick",
                                                                      "T": "#minecraft:logs", "J": "minecraft:jack_o_lantern"},
     "result": "dead_hollow_tree", "count": 1, "category": "building"},
    # Decorations batch 13: a glass bowl round glow berries; a fruit loaf of wheat, sugar and berries with a gold nugget
    # (the ring) baked in; sugar round a red dye.
    # Fall additions 20: an altar of three tiers, planks under a white cloth.
    {"id": "ofrenda", "pattern": ["WWW", "PPP", "PPP"], "key": {"W": "minecraft:white_carpet", "P": "#minecraft:planks"},
     "result": "ofrenda", "count": 1, "category": "building"},
    # Fall additions 19: two copper-wire antennas over a note block in a wooden cabinet on legs (copper wire needs machines).
    {"id": "theremin", "pattern": ["W W", "PNP", "S S"], "key": {"W": "jugcraft:copper_wire", "P": "#minecraft:planks",
                                                               "N": "minecraft:note_block", "S": "minecraft:stick"},
     "result": "theremin", "count": 1, "category": "redstone", "features": ["machines"]},
    # Fall additions 17: a birch board lettered in ink under a glass-lensed planchette.
    {"id": "spirit_board", "pattern": [" G ", "SIS"], "key": {"G": "minecraft:glass_pane", "S": "minecraft:birch_slab", "I": "minecraft:ink_sac"},
     "result": "spirit_board", "count": 1, "category": "building"},
    # Fall additions 16: a brick oven over a furnace's fire.
    {"id": "hearth_oven", "pattern": ["BBB", "B B", "BFB"], "key": {"B": "minecraft:brick", "F": "minecraft:furnace"},
     "result": "hearth_oven", "count": 1, "category": "building"},
    # Fall additions 15: a spoked wheel of planks and sticks with a string drive band; two sticks tipped with iron.
    {"id": "spinning_wheel", "pattern": [" P ", "PSP", "STS"], "key": {"P": "#minecraft:planks", "S": "minecraft:stick",
                                                                     "T": "minecraft:string"},
     "result": "spinning_wheel", "count": 1, "category": "building"},
    {"id": "knitting_needles", "pattern": ["N N", "S S"], "key": {"N": "minecraft:iron_nugget", "S": "minecraft:stick"},
     "result": "knitting_needles", "count": 1, "category": "equipment"},
    # Fall additions 13: a slatted roost of planks.
    {"id": "bat_house", "pattern": ["PPP", "PSP", "P P"], "key": {"P": "#minecraft:planks", "S": "minecraft:stick"}, "result": "bat_house",
     "count": 1, "category": "building"},
    # Fall additions 12: a wicker basket of sugar cane with a stick handle.
    {"id": "foraging_basket", "pattern": [" S ", "C C", "CCC"], "key": {"S": "minecraft:stick", "C": "minecraft:sugar_cane"},
     "result": "foraging_basket", "count": 1, "category": "equipment"},
    # Fall additions 11: a copper sugar pot with a glass thermometer on its rim; a tin tray of iron nuggets.
    {"id": "candy_kettle", "pattern": ["CGC", "C C", "CCC"], "key": {"C": "minecraft:copper_ingot", "G": "minecraft:glass_pane"},
     "result": "candy_kettle", "count": 1, "category": "misc"},
    {"id": "candy_tray", "pattern": ["N N", "NNN"], "key": {"N": "minecraft:iron_nugget"}, "result": "candy_tray", "count": 1,
     "category": "misc"},
    # Fall additions 1: a copper pot for melting wax.
    {"id": "wax_melting_pot", "pattern": ["C C", "C C", "CCC"], "key": {"C": "minecraft:copper_ingot"},
     "result": "wax_melting_pot", "count": 1, "category": "misc"},
    # Fall additions 2: a press of planks round an iron screw over a grindstone (the grinder) and a trough of slabs; a
    # barrel in a cradle of sticks with iron hoops and a gold tap.
    {"id": "cider_press", "pattern": ["PIP", "PGP", "SSS"], "key": {"P": "#minecraft:planks", "I": "minecraft:iron_ingot",
                                                                   "G": "minecraft:grindstone", "S": "#minecraft:wooden_slabs"},
     "result": "cider_press", "count": 1, "category": "misc"},
    # Fall additions 3: Mason Jars of glass with an iron lid; a pantry shelf of planks and slabs.
    {"id": "mason_jar", "pattern": [" N ", "G G", "GGG"], "key": {"N": "minecraft:iron_nugget", "G": "minecraft:glass"},
     "result": "mason_jar", "count": 3, "category": "misc"},
    {"id": "pantry_shelf", "pattern": ["SSS", "P P", "SSS"], "key": {"S": "#minecraft:wooden_slabs", "P": "#minecraft:planks"},
     "result": "pantry_shelf", "count": 1, "category": "misc"},
    {"id": "cider_barrel", "pattern": [" G ", "NBN", "S S"], "key": {"G": "minecraft:gold_nugget", "N": "minecraft:iron_nugget",
                                                                    "B": "minecraft:barrel", "S": "minecraft:stick"},
     "result": "cider_barrel", "count": 1, "category": "misc"},
    # Decorations batch 14: a black cape lined with red; linen strips and string; a black suit with bones and glowstone
    # for their glow; brown fur with leather and bone fangs; leather wings on sticks; a trunk of planks round a chest.
    {"id": "vampire_cape", "pattern": ["BRB", "BBB", "B B"], "key": {"B": "minecraft:black_wool", "R": "minecraft:red_wool"},
     "result": "vampire_cape", "count": 1, "category": "misc"},
    {"id": "mummy_wraps", "pattern": ["PSP", "SWS", "PSP"], "key": {"P": "minecraft:paper", "S": "minecraft:string", "W": "minecraft:white_wool"},
     "result": "mummy_wraps", "count": 1, "category": "misc"},
    {"id": "skeleton_suit", "pattern": ["BWB", "WGW", "BWB"], "key": {"B": "minecraft:bone", "W": "minecraft:black_wool",
                                                                   "G": "minecraft:glowstone_dust"},
     "result": "skeleton_suit", "count": 1, "category": "misc"},
    {"id": "werewolf_mask", "pattern": ["WLW", "WBW", "W W"], "key": {"W": "minecraft:brown_wool", "L": "minecraft:leather",
                                                                   "B": "minecraft:bone"},
     "result": "werewolf_mask", "count": 1, "category": "misc"},
    {"id": "bat_wings", "pattern": ["S S", "LDL", "L L"], "key": {"S": "minecraft:stick", "L": "minecraft:leather", "D": "minecraft:black_dye"},
     "result": "bat_wings", "count": 1, "category": "misc"},
    {"id": "costume_trunk", "pattern": ["PWP", "PCP", "PPP"], "key": {"P": "#minecraft:planks", "W": "minecraft:purple_wool",
                                                                   "C": "minecraft:chest"},
     "result": "costume_trunk", "count": 1, "category": "building"},
    {"id": "witchs_brew_punch_bowl", "pattern": ["G G", "GBG"], "key": {"G": "minecraft:glass", "B": "minecraft:glow_berries"},
     "result": "witchs_brew_punch_bowl", "count": 1, "category": "building"},
    {"id": "barmbrack", "pattern": ["WSW", "BGB"], "key": {"W": "minecraft:wheat", "S": "minecraft:sugar", "B": "minecraft:sweet_berries",
                                                        "G": "minecraft:gold_nugget"},
     "result": "barmbrack", "count": 1, "category": "misc"},
    {"id": "giant_candy", "pattern": ["SSS", "SDS", "SSS"], "key": {"S": "minecraft:sugar", "D": "minecraft:red_dye"},
     "result": "giant_candy", "count": 2, "category": "building"},
    # Decorations batch 12: a minecart under two hay bales with a jack o'lantern on a post; logs over coal in a ring of
    # cobblestone.
    {"id": "haunted_hayride", "pattern": [" J ", "HMH"], "key": {"J": "minecraft:jack_o_lantern", "H": "minecraft:hay_block",
                                                              "M": "minecraft:minecart"},
     "result": "haunted_hayride", "count": 1, "category": "misc"},
    {"id": "halloween_bonfire", "pattern": [" L ", "LCL", "SSS"], "key": {"L": "#minecraft:logs", "C": "#minecraft:coals",
                                                                       "S": "minecraft:cobblestone"},
     "result": "halloween_bonfire", "count": 1, "category": "building"},
    # Decorations batch 11: a plank crate round a wool ghost on an iron spring; red carpet edged with gold; a red-draped
    # table with a gold bell; bone pins; a chalkboard on legs; a hollow log round a chest; black glass tiles round a
    # redstone lamp; a gold bell with a wool ghost; a purple-draped table with a book for a spirit board.
    {"id": "jump_scare_trap", "pattern": ["PWP", "PIP", "PPP"], "key": {"P": "#minecraft:planks", "W": "minecraft:white_wool",
                                                                     "I": "#c:ingots/iron"},
     "result": "jump_scare_trap", "count": 1, "category": "redstone"},
    {"id": "costume_runway", "pattern": ["CCC", "G G"], "key": {"C": "minecraft:red_carpet", "G": "minecraft:gold_nugget"},
     "result": "costume_runway", "count": 3, "category": "building"},
    {"id": "judges_table", "pattern": ["CGC", "P P"], "key": {"C": "minecraft:red_wool", "G": "minecraft:gold_ingot", "P": "#minecraft:planks"},
     "result": "judges_table", "count": 1, "category": "building"},
    {"id": "skeleton_pin", "pattern": ["B", "B"], "key": {"B": "minecraft:bone"}, "result": "skeleton_pin", "count": 2, "category": "building"},
    {"id": "bowling_scoreboard", "pattern": ["PPP", "PKP", "S S"], "key": {"P": "#minecraft:planks", "K": "minecraft:black_dye",
                                                                        "S": "minecraft:stick"},
     "result": "bowling_scoreboard", "count": 1, "category": "building"},
    {"id": "candy_cache", "pattern": ["L L", "LCL"], "key": {"L": "#minecraft:logs", "C": "minecraft:chest"},
     "result": "candy_cache", "count": 1, "category": "building"},
    {"id": "dance_floor", "pattern": ["GDG", "DLD", "GDG"], "key": {"G": "minecraft:black_stained_glass", "D": "minecraft:glowstone_dust",
                                                                 "L": "minecraft:redstone_lamp"},
     "result": "dance_floor", "count": 8, "category": "redstone"},
    {"id": "ghost_bell", "pattern": [" S ", "GWG", " G "], "key": {"S": "minecraft:stick", "G": "minecraft:gold_ingot", "W": "minecraft:white_wool"},
     "result": "ghost_bell", "count": 1, "category": "building"},
    {"id": "fortune_teller_table", "pattern": ["CBC", "PPP", "P P"], "key": {"C": "minecraft:purple_carpet", "B": "minecraft:book",
                                                                          "P": "#minecraft:planks"},
     "result": "fortune_teller_table", "count": 1, "category": "building"},
    # Decorations batch 10: a purple-dyed glass tube in an iron fixture with glowstone; glow ink with bone meal; an iron
    # brazier on a campfire; paper round a candle on a plank base; three pumpkins and torches; a hat of wool round a candle.
    {"id": "black_light", "pattern": ["III", "PGP"], "key": {"I": "minecraft:iron_nugget", "P": "minecraft:purple_stained_glass",
                                                          "G": "minecraft:glowstone_dust"},
     "result": "black_light", "count": 1, "category": "redstone"},
    {"id": "witch_fire_brazier", "pattern": ["I I", "ICI", " I "], "key": {"I": "#c:ingots/iron", "C": "minecraft:campfire"},
     "result": "witch_fire_brazier", "count": 1, "category": "building"},
    {"id": "shadow_puppet_lamp", "pattern": ["PPP", "PCP", " S "], "key": {"P": "minecraft:paper", "C": "#minecraft:candles",
                                                                        "S": "#minecraft:wooden_slabs"},
     "result": "shadow_puppet_lamp", "count": 1, "category": "building"},
    {"id": "mini_pumpkin_stack", "pattern": [" P ", "PTP"], "key": {"P": "minecraft:carved_pumpkin", "T": "minecraft:torch"},
     "result": "mini_pumpkin_stack", "count": 2, "category": "building"},
    {"id": "floating_witch_hat", "pattern": [" W ", "WCW", "WWW"], "key": {"W": "minecraft:black_wool", "C": "#minecraft:candles"},
     "result": "floating_witch_hat", "count": 2, "category": "building"},
]

# Growth rules shared with Java (agriculture/CropGrowth.java): non-legume crops next to a
# legume (block tag jugcraft:nitrogen_fixing_crops, 8 neighbours) grow this much faster.
LEGUME_BONUS = 1.5
LEGUME_TAG = "jugcraft:nitrogen_fixing_crops"


def tall_blocks():
    return [info["block"] for info in TALL_CROPS.values()]


def crop_blocks():
    return tall_blocks() + [info["block"] for info in CROPS.values()]


def trellis_crops():
    return [info["block"] for info in TALL_CROPS.values() if info.get("trellis")]


def stem_blocks():
    return [block for gourd in GOURDS for block in (stem(gourd), attached_stem(gourd))]


def giant_blocks():
    return [GIANT_PUMPKIN["block"], GIANT_PUMPKIN["vine"], GIANT_PUMPKIN["attached_vine"]]


def planted_blocks():
    """Blocks that a seed item places: crops, gourd stems and vines, the cranberry bush and the fruit trees' saplings."""
    return (crop_blocks() + [stem(gourd) for gourd in GOURDS]
            + [CRANBERRY["block"], CHESTNUT["sapling"], CIDER["tree"]["sapling"], GIANT_PUMPKIN["vine"]])


def itemless_blocks():
    """Blocks without an item of their own: the item that plants them (or the pumpkins they drop) stands in for them."""
    return (crop_blocks() + stem_blocks() + [CRANBERRY["block"], CHESTNUT["sapling"], CIDER["tree"]["sapling"]] + giant_blocks()
            + [potted(m) for m in MUMS] + [potted(WOLFSBANE["block"])] + [MAZE["finish"], MAZE["corn"]] + plants.itemless())


def all_blocks():
    """Every registered agriculture block. Crops have no block item (seeds place them); wild plants and equipment do."""
    return (crop_blocks() + stem_blocks() + [CRANBERRY["block"]] + list(WILD_CROPS) + list(EQUIPMENT) + list(GOURDS)
            + list(TREE_BLOCKS) + list(WOOD) + list(DECOR) + [CARVING["block"]] + list(CARVED_VARIETIES.values())
            + giant_blocks() + [HARVEST_SCALE["block"]] + list(HALLOWEEN_DECOR) + list(MUMS) + [potted(m) for m in MUMS]
            + regatta_blocks() + festivity_blocks() + night_blocks() + decor1_blocks() + decor2_blocks() + decor3_blocks()
            + decor4_blocks() + decor5_blocks() + decor6_blocks() + decor7_blocks() + decor8_blocks() + decor9_blocks() + decor10_blocks()
            + decor11_blocks() + decor12_blocks() + decor13_blocks() + decor14_blocks() + chandlery_blocks() + cider_blocks() + pantry_blocks()
            + firework_blocks() + feast_blocks() + maze_blocks() + candy_blocks() + foraging_blocks() + bat_blocks() + [KNITTING["wheel"]] + pie_blocks() + [SPIRIT_BOARD["block"], TURKEYS["roast"], THEREMIN["block"], OFRENDA["block"]] + list(OFRENDA["decor"]) + graveyard.blocks() + [WOLFSBANE["block"], potted(WOLFSBANE["block"])] + WEREWOLF_RUGS + midway.blocks() + ferris_wheel.blocks() + hot_air_balloon.blocks() + plants.blocks() + decor15.blocks() + decor16.blocks() + decor17.blocks() + decor18.blocks() + decor19.blocks() + decor20.blocks())


def all_items():
    return (list(ITEMS) + list(SICKLES) + list(WILD_CROPS) + list(EQUIPMENT) + list(GOURDS) + [CHESTNUT["leaves"]]
            + [block for tree in TREES for block in (sapling(tree), TREES[tree]["leaves"])] + list(WOOD) + list(DECOR) + [CARVING["block"], CARVING["knife"]] + list(CARVED_VARIETIES.values())
            + [HARVEST_SCALE["block"]] + list(HARVEST_SCALE["ribbons"]) + [STENCILS["blank"], STENCILS["stencil"], CANTEEN["item"]]
            + list(HALLOWEEN_DECOR) + list(MUMS) + regatta_items() + festivity_blocks() + night_items() + decor1_items()
            + decor2_items() + decor3_items() + decor4_items() + decor5_items() + decor6_items() + decor7_items() + decor8_items()
            + decor9_items() + decor10_items() + decor11_items() + decor12_items() + decor13_items() + decor14_items()
            + chandlery_items() + cider_items() + pantry_items() + firework_items()
            + lantern_items() + feast_blocks() + [MAZE["gate"]] + ghost_items() + face_paint_items() + candy_items() + foraging_items() + bat_items() + knitting_items() + pie_items() + [SPIRIT_BOARD["block"], TURKEYS["roast"], THEREMIN["block"], OFRENDA["block"]] + list(OFRENDA["decor"]) + graveyard.items() + [h["item"] for h in HEX["brews"].values()] + [BROOMSTICK["item"]] + [WOLFSBANE["block"], WEREWOLF["dagger"], WEREWOLF["arrow"]] + WEREWOLF_PELTS + WEREWOLF_RUGS + [SQUIRRELS["acorn"]] + midway.items() + ferris_wheel.items() + pinata.items() + hot_air_balloon.items() + leaf_blower.items() + plants.items() + decor15.items() + decor16.items() + decor17.items() + decor18.items() + decor19.items() + decor20.items())


def owns(entry_id):
    return entry_id in all_blocks() or entry_id in all_items()


def switch_of(entry_id):
    """The feature switch that owns one of this branch's IDs: for a wood set and for a tree's sapling and leaves, its
    wood's WOOD_SWITCHES list (any of which turns it on), otherwise FEATURE."""
    for wood, switch in WOOD_SWITCHES.items():
        if entry_id in wood_blocks(wood, WOOD_SETS[wood]):
            return switch
    for tree in TREES:
        if entry_id in (sapling(tree), TREES[tree]["leaves"]):
            return WOOD_SWITCHES[tree]
    if entry_id in plants.blocks():
        return plants.FEATURE
    return FEATURE


def textures():
    """Every crop block texture the models reference."""
    out = []
    for info in TALL_CROPS.values():
        for names in info["textures"]:
            out += [n for n in names if n not in out]
    for crop, info in CROPS.items():
        if info.get("sculpted"):
            out += [crop] if crop not in out else []
            continue
        out += [f"{info['block'].removesuffix('_crop')}_stage{n}" for n in sorted(set(info["stages"]))]
    out += [w["texture"] for w in WILD_CROPS.values() if w["texture"] not in out]
    out += [f"{gourd}_{part}" for gourd in GOURDS for part in ("side", "top")] + STEM_TEXTURES + CRANBERRY["stages"]
    return out + EQUIPMENT_TEXTURES + TREE_TEXTURES + DECOR_TEXTURES + halloween_textures() + graveyard.textures() + plants.textures()


EQUIPMENT_TEXTURES = ["trellis", "trellis_post", "cooking_pot_side", "cooking_pot_rim", "cooking_pot_empty", "cooking_pot_soup"]

# Fall additions 16: pastry from wheat and an egg; a raw pie of pastry, its filling and sugar (a milk bucket leaves its
# bucket, as crafting with one does).
SHAPELESS += [{"id": PIES["dough"], "inputs": ["minecraft:wheat", "minecraft:wheat", "minecraft:egg"], "result": PIES["dough"], "count": 2,
               "category": "misc"}]
SHAPELESS += [{"id": f"raw_{filling}_pie", "inputs": [f"jugcraft:{PIES['dough']}"] + info["with"] + ["minecraft:sugar"],
               "result": f"raw_{filling}_pie", "count": 1, "category": "misc"} for filling, info in PIES["fillings"].items()]

# Fall additions 20: marigold petals strewn from a flower; papel picado cut from paper on a string; sugar skulls moulded
# from sugar and an egg white, iced; pan de muerto dough (baked in COOKING).
SHAPELESS += [{"id": "marigold_petals", "inputs": ["jugcraft:marigold"], "result": "marigold_petals", "count": 4, "category": "building"},
              {"id": "papel_picado", "inputs": ["minecraft:paper", "minecraft:paper", "minecraft:paper", "minecraft:string",
                                                "minecraft:pink_dye", "minecraft:orange_dye"], "result": "papel_picado", "count": 6,
               "category": "building"},
              {"id": "sugar_skull", "inputs": ["minecraft:sugar", "minecraft:sugar", "minecraft:sugar", "minecraft:egg",
                                               "minecraft:light_blue_dye"], "result": "sugar_skull", "count": 2, "category": "building"},
              {"id": "pan_de_muerto_dough", "inputs": ["minecraft:wheat", "minecraft:wheat", "minecraft:wheat", "minecraft:egg",
                                                       "minecraft:sugar"], "result": "pan_de_muerto_dough", "count": 2, "category": "misc"}]

# Fall additions 22: a flying broomstick is a witch's broom anointed with Flying Ointment and fletched with feathers.
SHAPELESS += [{"id": BROOMSTICK["item"], "inputs": BROOMSTICK["inputs"], "result": BROOMSTICK["item"], "count": 1, "category": "misc"}]

# Fall additions 23: silver against werewolves. A silver dagger; silver-tipped arrows; a werewolf's pelt laid out as a rug
# or cut into leather.
SHAPED += [{"id": WEREWOLF["dagger"], "pattern": ["S", "T"], "key": {"S": "#c:ingots/silver", "T": "minecraft:stick"},
            "result": WEREWOLF["dagger"], "count": 1, "category": "equipment", "features": ["silver"]},
           ] + [{"id": k["rug"], "pattern": ["PP"], "key": {"P": f"jugcraft:{k['pelt']}"}, "result": k["rug"], "count": 1,
                 "category": "building"} for k in WEREWOLF["kinds"].values()]
SHAPELESS += [{"id": WEREWOLF["arrow"], "inputs": ["#c:nuggets/silver"] + ["minecraft:arrow"] * 4, "result": WEREWOLF["arrow"], "count": 4,
               "category": "equipment", "features": ["silver"]},
              ] + [{"id": f"leather_from_{k['pelt']}", "inputs": [f"jugcraft:{k['pelt']}"], "result": "minecraft:leather", "count": 2,
                    "category": "misc"} for k in WEREWOLF["kinds"].values()]
# The fall fair midway's recipes (tools/midway.py).
SHAPED += midway.SHAPED
SHAPELESS += midway.SHAPELESS
# The Ferris wheel's recipe (tools/ferris_wheel.py).
SHAPED += ferris_wheel.SHAPED
# The piñata party's recipes (tools/pinata.py).
SHAPED += pinata.SHAPED
SHAPELESS += pinata.SHAPELESS
# The hot-air balloon fiesta's recipes (tools/hot_air_balloon.py).
SHAPED += hot_air_balloon.SHAPED
SHAPELESS += hot_air_balloon.SHAPELESS
# The leaf blower's recipe (tools/leaf_blower.py).
SHAPED += leaf_blower.SHAPED
# Halloween decorations batch 15, the churchyard's ornaments (tools/decor15.py).
SHAPED += decor15.SHAPED
SHAPELESS += decor15.SHAPELESS
# Halloween decorations batch 16, the haunted house's props (tools/decor16.py).
SHAPED += decor16.SHAPED
SHAPELESS += decor16.SHAPELESS
# Halloween decorations batch 17, the Witch's Workshop (tools/decor17.py).
SHAPED += decor17.SHAPED
SHAPELESS += decor17.SHAPELESS
# Halloween decorations batch 18, the Crypt and the Ossuary (tools/decor18.py).
SHAPED += decor18.SHAPED
SHAPELESS += decor18.SHAPELESS
# Halloween decorations batch 19, the Laboratory, the Larder and the Dining Room (tools/decor19.py).
SHAPED += decor19.SHAPED
SHAPELESS += decor19.SHAPELESS
# Halloween decorations batch 20, Pumpkin Night (tools/decor20.py); the heirlooms are with the others above.
SHAPED += decor20.SHAPED
SHAPELESS += decor20.SHAPELESS
