"""The menu (the third slice of the kitchen and cooking expansion in docs/branches/AGRICULTURE.md): dishes from the owner's
own food art (art/owner-library, imported by tools/owner_art.py), each a food that can be set down as a 3D model. The
numbers here are what agriculture/JugcraftAgriculture, PlacedDishBlock, MenuDishes and PetFoodItem use;
tools/check_mod_data.py compares them. tools/menu_data.py writes the JSON.

The owner chose (7 October 2026), seeing both looks side by side, that the onion, vegetable and pumpkin soups, the cabbage
rolls, the roasted corn (their grilled corn), the mulled cider (their cider mug) and the Cooking Pot wear their art, keeping
their IDs, recipes and food; and that popcorn set down becomes their striped popcorn box. The rice dishes wait for rice
(slice 4), and the owner's crop icons for the garden crops (slice 7).

Every dish is set down by a sneaking player using it on a block (as vanilla's pumpkin pie is, tools/feasts.py), and taken
back by using it with an empty hand. Its model is fitted to the owner's icon (MODEL: a template and where on the icon its
parts are drawn): a soup bowl, a wide plate, a stacked sandwich, the icon extruded lying flat, the icon extruded standing
(mugs and bottles), or the popcorn box.

Balance (docs/BALANCE.md): a dish gives at most COOK_BONUS more hunger than its ingredients, each counted as the most it
would give (as itself, as what a furnace makes of it, as what a board cuts it into, or, for an ingredient that is not
food, as what went into it); tools/check_mod_data.py checks. Cookies come four to a batch, not vanilla's eight, so a
batch is never a gain.
"""
FEATURE = "agriculture"

COOK_BONUS = 3

# The dishes: display, food [hunger, saturation modifier], how it is eaten ("kind"), its model and its owner texture
# (library name; the dish's ID when not given). Kinds: "stew" (a bowl food that gives the bowl back; "effect" adds a short
# effect), "drink" (a glass drunk even when full: "effect"), "treat" (on a stick, given back), "cob" (corn on the cob,
# the cob given back), "meal" (a cooked food composters refuse), "plain" (an ingredient), "milk" (clears effects like a
# bucket of milk), "pet" (fed to a tamed wolf or horse: tools/menu.py PETS).
DISHES = {
    # Drinks, in the owner's glass mugs and bottle.
    "hot_cocoa": {"display": "Hot Cocoa", "food": [3, 0.4], "kind": "drink", "effect": ["REGENERATION", 8], "model": ["stand"]},
    "creamy_corn_drink": {"display": "Creamy Corn Drink", "food": [4, 0.5], "kind": "drink", "effect": ["ABSORPTION", 30],
                          "model": ["stand"]},
    "melon_juice": {"display": "Melon Juice", "food": [4, 0.4], "kind": "drink", "effect": ["SPEED", 45], "model": ["stand"]},
    "glow_berry_custard": {"display": "Glow Berry Custard", "food": [6, 0.6], "kind": "drink", "effect": ["NIGHT_VISION", 30],
                           "model": ["stand"]},
    "milk_bottle": {"display": "Milk Bottle", "kind": "milk", "model": ["stand"]},
    # Soups and stews in the owner's small bowl.
    "beef_stew": {"display": "Beef Stew", "food": [11, 0.8], "kind": "stew", "model": ["bowl", {"heap": 1}]},
    "chicken_soup": {"display": "Chicken Soup", "food": [10, 0.8], "kind": "stew", "model": ["bowl"]},
    "baked_cod_stew": {"display": "Baked Cod Stew", "food": [10, 0.8], "kind": "stew", "model": ["bowl", {"heap": 1}]},
    "fish_stew": {"display": "Fish Stew", "food": [10, 0.8], "kind": "stew", "model": ["bowl", {"heap": 1}]},
    "bone_broth": {"display": "Bone Broth", "food": [6, 0.6], "kind": "stew", "model": ["bowl"]},
    "corn_soup": {"display": "Corn Soup", "food": [9, 0.6], "kind": "stew", "model": ["bowl"]},
    "noodle_soup": {"display": "Noodle Soup", "food": [9, 0.8], "kind": "stew", "model": ["bowl"]},
    "tomato_sauce": {"display": "Tomato Sauce", "food": [5, 0.5], "kind": "stew", "model": ["bowl"]},
    "fruit_salad": {"display": "Fruit Salad", "food": [8, 0.6], "kind": "stew", "model": ["bowl", {"heap": 2, "content": [3, 2, 13, 8]}]},
    "nether_salad": {"display": "Nether Salad", "food": [3, 0.6], "kind": "stew", "effect": ["NAUSEA", 4], "model": ["bowl", {"heap": 1}]},
    "creamed_corn": {"display": "Creamed Corn", "food": [8, 0.6], "kind": "stew", "model": ["flat"]},
    # Meals on the owner's wide plate.
    "bacon_and_eggs": {"display": "Bacon and Eggs", "food": [10, 0.8], "kind": "stew", "model": ["plate"]},
    "steak_and_potatoes": {"display": "Steak and Potatoes", "food": [13, 0.8], "kind": "stew", "model": ["plate"]},
    "roasted_mutton_chops": {"display": "Roasted Mutton Chops", "food": [11, 0.8], "kind": "stew", "model": ["plate"]},
    "grilled_salmon": {"display": "Grilled Salmon", "food": [10, 0.8], "kind": "stew", "model": ["plate"]},
    "ratatouille": {"display": "Ratatouille", "food": [8, 0.6], "kind": "stew", "model": ["plate"]},
    "pasta_with_meatballs": {"display": "Pasta with Meatballs", "food": [12, 0.8], "kind": "stew", "model": ["plate"]},
    "pasta_with_mutton_chop": {"display": "Pasta with Mutton Chop", "food": [12, 0.8], "kind": "stew", "model": ["plate"]},
    "squid_ink_pasta": {"display": "Squid Ink Pasta", "food": [10, 0.8], "kind": "stew", "model": ["plate"]},
    "vegetable_noodles": {"display": "Vegetable Noodles", "food": [9, 0.7], "kind": "stew", "model": ["plate"]},
    "cornbread_stuffing": {"display": "Cornbread Stuffing", "food": [9, 0.7], "kind": "stew", "model": ["plate"]},
    # Sandwiches and other food in hand.
    "hamburger": {"display": "Hamburger", "food": [11, 0.8], "kind": "meal",
                  "model": ["stack", {"lo": [3, 0, 4], "hi": [13, 6, 12], "top": [3, 2, 13, 6], "side": [1, 6, 15, 13]}]},
    "bacon_sandwich": {"display": "Bacon Sandwich", "food": [10, 0.8], "kind": "meal",
                       "model": ["stack", {"lo": [3, 0, 4], "hi": [13, 5, 12], "top": [3, 3, 13, 7], "side": [1, 7, 15, 12]}]},
    "chicken_sandwich": {"display": "Chicken Sandwich", "food": [10, 0.8], "kind": "meal",
                         "model": ["stack", {"lo": [3, 0, 4], "hi": [13, 5, 12], "top": [3, 3, 13, 7], "side": [1, 7, 15, 12]}]},
    "egg_sandwich": {"display": "Egg Sandwich", "food": [8, 0.8], "kind": "meal",
                     "model": ["stack", {"lo": [3, 0, 5], "hi": [13, 4, 11], "top": [3, 5, 13, 8], "side": [2, 8, 14, 12]}]},
    "mutton_wrap": {"display": "Mutton Wrap", "food": [8, 0.8], "kind": "meal", "model": ["flat"]},
    "taco": {"display": "Taco", "food": [9, 0.8], "kind": "meal", "model": ["flat"]},
    "stuffed_potato": {"display": "Stuffed Potato", "food": [10, 0.8], "kind": "meal", "model": ["flat"]},
    "dumplings": {"display": "Dumplings", "food": [4, 0.6], "kind": "meal", "model": ["flat"]},
    "ham": {"display": "Ham", "food": [5, 0.3], "kind": "meal", "model": ["flat"]},
    "smoked_ham": {"display": "Smoked Ham", "food": [14, 0.8], "kind": "meal", "model": ["flat"]},
    "barbecue_stick": {"display": "Barbecue Stick", "food": [7, 0.8], "kind": "treat", "model": ["flat"]},
    "corn_dog": {"display": "Corn Dog", "food": [8, 0.8], "kind": "treat", "model": ["flat"]},
    "classic_corn_dog": {"display": "Classic Corn Dog", "food": [10, 0.8], "kind": "treat", "model": ["flat"]},
    # Sweets.
    "honey_cookie": {"display": "Honey Cookie", "food": [2, 0.2], "kind": "meal", "model": ["flat"]},
    "sweet_berry_cookie": {"display": "Sweet Berry Cookie", "food": [2, 0.2], "kind": "meal", "model": ["flat"]},
    "caramel_popcorn": {"display": "Caramel Popcorn", "food": [6, 0.5], "kind": "meal", "model": ["flat"]},
    "corn_popsicle": {"display": "Corn Popsicle", "food": [3, 0.4], "kind": "treat", "model": ["flat"]},
    "melon_popsicle": {"display": "Melon Popsicle", "food": [3, 0.4], "kind": "treat", "model": ["flat"]},
    # Corn: on the cob, bread, tortillas and chips.
    "boiled_corn": {"display": "Boiled Corn", "food": [5, 0.6], "kind": "cob", "compost": "medium_high", "model": ["flat"]},
    "cornbread": {"display": "Cornbread", "food": [6, 0.6], "kind": "meal", "model": ["flat"]},
    "tortilla": {"display": "Tortilla", "food": [2, 0.4], "kind": "meal", "model": ["flat"]},
    "tortilla_chip": {"display": "Tortilla Chip", "food": [1, 0.3], "kind": "meal", "model": ["flat"]},
    # Ingredients.
    "wheat_dough": {"display": "Wheat Dough", "kind": "plain", "compost": "medium"},
    "raw_pasta": {"display": "Raw Pasta", "kind": "plain", "compost": "medium"},
    "cornbread_batter": {"display": "Cornbread Batter", "kind": "plain", "compost": "medium"},
    "tortilla_raw": {"display": "Raw Tortilla", "kind": "plain", "compost": "medium"},
    "corncob": {"display": "Corncob", "kind": "plain", "compost": "low"},
    # Feed for pets.
    "dog_food": {"display": "Dog Food", "kind": "pet", "model": ["bowl", {"heap": 1}]},
    "horse_feed": {"display": "Horse Feed", "kind": "pet", "model": ["flat"]},
}

# What pet food does, fed by its tamed animal's owner (wolf) or rider-tamer (horse): health restored and effects (seconds).
PETS = {
    "dog_food": {"animal": "wolf", "heal": 20, "effects": [["STRENGTH", 300], ["SPEED", 300]], "returns": "minecraft:bowl"},
    "horse_feed": {"animal": "horse", "heal": 10, "effects": [["SPEED", 120], ["JUMP_BOOST", 120]], "returns": None},
}

# Foods Jugcraft already had that now wear the owner's art (item texture -> library name), and are set down like the rest.
RESTYLED = {"onion_soup": "onion_soup", "vegetable_soup": "vegetable_soup", "pumpkin_soup": "pumpkin_soup",
            "cabbage_rolls": "cabbage_rolls", "roasted_corn": "grilled_corn", "mulled_cider": "apple_cider"}
RESTYLED_MODELS = {"onion_soup": ["bowl"], "vegetable_soup": ["bowl"], "pumpkin_soup": ["bowl"], "cabbage_rolls": ["flat"],
                   "roasted_corn": ["flat"], "mulled_cider": ["stand"], "popcorn": ["box"]}
# Roasted Corn now gives its cob back too.
COB_FOODS = ["roasted_corn", "boiled_corn"]
COB = "corncob"

# The owner's textures that are not a dish's icon: the popcorn box, and the Cooking Pot (block texture -> library name).
POPCORN_BOX = "popcorn"
COOKING_POT_TEXTURES = {"block/cooking_pot_side": "cooking_pot_side", "block/cooking_pot_top": "cooking_pot_top",
                        "block/cooking_pot_bottom": "cooking_pot_bottom", "block/cooking_pot_handle": "cooking_pot_handle",
                        "block/cooking_pot_parts": "cooking_pot_parts", "item/cooking_pot": "cooking_pot"}

# Recipes. Cooking Pot (tools/agriculture.py POT_RECIPES: any slot order, the vessel among the inputs; time: ticks).
POT_RECIPES = {
    "hot_cocoa": {"inputs": {"minecraft:glass_bottle": 1, "minecraft:cocoa_beans": 2, "minecraft:milk_bucket": 1, "minecraft:sugar": 1},
                  "time": 200},
    "creamy_corn_drink": {"inputs": {"minecraft:glass_bottle": 1, "jugcraft:corn": 1, "minecraft:milk_bucket": 1, "minecraft:sugar": 1},
                          "time": 200},
    "glow_berry_custard": {"inputs": {"minecraft:glass_bottle": 1, "minecraft:glow_berries": 2, "minecraft:milk_bucket": 1,
                                      "minecraft:egg": 1, "minecraft:sugar": 1}, "time": 200},
    "beef_stew": {"inputs": {"minecraft:bowl": 1, "jugcraft:minced_beef": 2, "minecraft:potato": 1, "minecraft:carrot": 1,
                             "jugcraft:onion": 1}, "time": 300},
    "chicken_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:chicken_cuts": 2, "minecraft:carrot": 1, "jugcraft:onion": 1,
                                "jugcraft:cabbage_leaf": 1}, "time": 300},
    "baked_cod_stew": {"inputs": {"minecraft:bowl": 1, "jugcraft:cod_slice": 2, "minecraft:potato": 1, "jugcraft:tomato": 1,
                                  "minecraft:egg": 1}, "time": 300},
    "fish_stew": {"inputs": {"minecraft:bowl": 1, "jugcraft:salmon_slice": 2, "jugcraft:tomato": 1, "jugcraft:onion": 1,
                             "jugcraft:garlic": 1}, "time": 300},
    "bone_broth": {"inputs": {"minecraft:bowl": 1, "minecraft:bone": 1, "minecraft:carrot": 1, "jugcraft:onion": 1,
                              "minecraft:brown_mushroom": 1}, "time": 300},
    "corn_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:corn": 2, "jugcraft:onion": 1, "minecraft:potato": 1}, "time": 200},
    "creamed_corn": {"inputs": {"minecraft:bowl": 1, "jugcraft:corn": 2, "minecraft:milk_bucket": 1, "minecraft:sugar": 1}, "time": 200},
    "noodle_soup": {"inputs": {"minecraft:bowl": 1, "jugcraft:raw_pasta": 1, "jugcraft:chicken_cuts": 1, "minecraft:carrot": 1,
                               "jugcraft:onion": 1}, "time": 200},
    "vegetable_noodles": {"inputs": {"minecraft:bowl": 1, "jugcraft:raw_pasta": 1, "jugcraft:cabbage_leaf": 1, "minecraft:carrot": 1,
                                     "jugcraft:pepper": 1, "jugcraft:onion": 1}, "time": 200},
    "tomato_sauce": {"inputs": {"minecraft:bowl": 1, "jugcraft:tomato": 2, "jugcraft:garlic": 1}, "time": 200},
    "pasta_with_meatballs": {"inputs": {"minecraft:bowl": 1, "jugcraft:raw_pasta": 1, "jugcraft:minced_beef": 2, "jugcraft:tomato": 1},
                             "time": 300},
    "pasta_with_mutton_chop": {"inputs": {"minecraft:bowl": 1, "jugcraft:raw_pasta": 1, "jugcraft:mutton_chops": 2,
                                          "jugcraft:tomato": 1}, "time": 300},
    "squid_ink_pasta": {"inputs": {"minecraft:bowl": 1, "jugcraft:raw_pasta": 1, "minecraft:ink_sac": 1, "jugcraft:cod_slice": 1,
                                   "jugcraft:salmon_slice": 1, "jugcraft:tomato": 1}, "time": 300},
    # Cooked from what a ratatouille is made of since the eggplant and zucchini grow (tools/vegetables.py RATATOUILLE).
    "ratatouille": {"inputs": {"minecraft:bowl": 1, "jugcraft:tomato": 1, "jugcraft:pepper": 1, "jugcraft:onion": 1,
                               "jugcraft:eggplant": 1, "jugcraft:zucchini": 1}, "time": 300},
    "cornbread_stuffing": {"inputs": {"minecraft:bowl": 1, "jugcraft:cornbread": 1, "jugcraft:onion": 1, "minecraft:brown_mushroom": 1,
                                      "minecraft:carrot": 1}, "time": 200},
    "dumplings": {"inputs": {"jugcraft:wheat_dough": 1, "jugcraft:minced_beef": 1, "jugcraft:cabbage_leaf": 1, "jugcraft:onion": 1},
                  "count": 2, "time": 200},
    "boiled_corn": {"inputs": {"jugcraft:corn": 1}, "time": 100},
}

# Crafting (shapeless): inputs and how many it makes.
SHAPELESS = {
    "melon_juice": [["minecraft:glass_bottle", "minecraft:melon_slice", "minecraft:melon_slice", "minecraft:melon_slice",
                     "minecraft:sugar"], 1],
    "milk_bottle": [["minecraft:milk_bucket", "minecraft:glass_bottle", "minecraft:glass_bottle", "minecraft:glass_bottle",
                     "minecraft:glass_bottle"], 4],
    "nether_salad": [["minecraft:bowl", "minecraft:crimson_fungus", "minecraft:warped_fungus"], 1],
    "fruit_salad": [["minecraft:bowl", "minecraft:apple", "minecraft:melon_slice", "minecraft:sweet_berries"], 1],
    "bacon_and_eggs": [["minecraft:bowl", "jugcraft:cooked_bacon", "jugcraft:fried_egg", "jugcraft:fried_egg"], 1],
    "steak_and_potatoes": [["minecraft:bowl", "minecraft:cooked_beef", "minecraft:baked_potato"], 1],
    "roasted_mutton_chops": [["minecraft:bowl", "jugcraft:cooked_mutton_chops", "jugcraft:cooked_mutton_chops", "minecraft:baked_potato"], 1],
    "grilled_salmon": [["minecraft:bowl", "jugcraft:cooked_salmon_slice", "jugcraft:cooked_salmon_slice", "minecraft:sweet_berries",
                        "jugcraft:cabbage_leaf"], 1],
    "hamburger": [["minecraft:bread", "jugcraft:beef_patty", "jugcraft:cabbage_leaf", "jugcraft:tomato", "jugcraft:onion"], 1],
    "bacon_sandwich": [["minecraft:bread", "jugcraft:cooked_bacon", "jugcraft:cabbage_leaf", "jugcraft:tomato"], 1],
    "chicken_sandwich": [["minecraft:bread", "jugcraft:cooked_chicken_cuts", "jugcraft:cabbage_leaf", "minecraft:carrot"], 1],
    "egg_sandwich": [["minecraft:bread", "jugcraft:fried_egg", "jugcraft:fried_egg"], 1],
    "mutton_wrap": [["jugcraft:tortilla", "jugcraft:cooked_mutton_chops", "jugcraft:cabbage_leaf", "jugcraft:onion"], 1],
    "taco": [["jugcraft:tortilla", "jugcraft:beef_patty", "jugcraft:cabbage_leaf", "jugcraft:tomato"], 1],
    "stuffed_potato": [["minecraft:baked_potato", "jugcraft:cooked_bacon", "jugcraft:cabbage_leaf"], 1],
    "ham": [["minecraft:porkchop", "minecraft:porkchop"], 1],
    "barbecue_stick": [["minecraft:stick", "jugcraft:cooked_chicken_cuts", "jugcraft:pepper", "jugcraft:onion"], 1],
    "corn_dog": [["minecraft:stick", "jugcraft:cornbread_batter", "minecraft:cooked_porkchop"], 1],
    "classic_corn_dog": [["jugcraft:corn_dog", "jugcraft:tomato"], 1],
    "honey_cookie": [["minecraft:wheat", "minecraft:wheat", "minecraft:honey_bottle"], 4],
    "sweet_berry_cookie": [["minecraft:wheat", "minecraft:wheat", "minecraft:sweet_berries", "minecraft:sweet_berries",
                            "minecraft:sweet_berries"], 4],
    "caramel_popcorn": [["jugcraft:popcorn", "jugcraft:popcorn", "jugcraft:caramel"], 1],
    "corn_popsicle": [["minecraft:stick", "jugcraft:corn", "minecraft:ice", "minecraft:sugar"], 1],
    "melon_popsicle": [["minecraft:stick", "minecraft:melon_slice", "minecraft:melon_slice", "minecraft:ice"], 1],
    "wheat_dough": [["minecraft:wheat", "minecraft:wheat", "minecraft:wheat", "minecraft:water_bucket"], 3],
    "cornbread_batter": [["jugcraft:corn", "jugcraft:corn", "minecraft:egg", "minecraft:milk_bucket"], 1],
    "tortilla_raw": [["jugcraft:corn", "jugcraft:corn"], 3],
    "dog_food": [["minecraft:bowl", "minecraft:rotten_flesh", "minecraft:bone_meal", "jugcraft:cooked_chicken_cuts"], 1],
    "horse_feed": [["minecraft:wheat", "minecraft:wheat", "minecraft:apple", "minecraft:carrot"], 1],
}

# Furnace, smoker and campfire (tools/agriculture.py COOKING).
COOKING = {
    "smoked_ham": {"input": "ham", "xp": 0.35},
    "cornbread": {"input": "cornbread_batter", "xp": 0.35},
    "tortilla": {"input": "tortilla_raw", "xp": 0.1},
}


def placed():
    """Every dish that can be set down, with its model: the menu's and the restyled foods'."""
    out = {name: info["model"] for name, info in DISHES.items() if "model" in info}
    out.update(RESTYLED_MODELS)
    return out


def owner(name):
    """The library texture a dish wears."""
    return RESTYLED.get(name) or DISHES.get(name, {}).get("owner", name)


# The owner's textures (runtime path -> library name): each new dish's icon, the restyled foods' icons, every placed dish's
# icon again as a block texture for its model, the popcorn box and the Cooking Pot.
TEXTURES = {f"item/{name}": owner(name) for name in DISHES}
TEXTURES.update({f"item/{name}": source for name, source in RESTYLED.items()})
TEXTURES.update({f"block/menu/{name}": (POPCORN_BOX if name == "popcorn" else owner(name)) for name in placed()})
TEXTURES.update(COOKING_POT_TEXTURES)

# Food entries for tools/agriculture.py ITEMS (item models, names, tags, compost).
ITEMS = {}
for _name, _info in DISHES.items():
    _entry = {"display": _info["display"], "tags": []}
    if "food" in _info:
        _entry["food"] = list(_info["food"])
        _entry["tags"] = ["c:foods"]
    if _info["kind"] == "stew":
        _entry["stew"] = True
        if "effect" in _info:
            _entry["stew_effect"] = list(_info["effect"])
    elif _info["kind"] == "drink":
        _entry["drink"] = list(_info["effect"])
    elif _info["kind"] == "treat":
        _entry["treat"] = True
    elif _info["kind"] in ("cob", "milk", "pet"):
        _entry[_info["kind"]] = True
    if "compost" in _info:
        _entry["compost"] = _info["compost"]
    ITEMS[_name] = _entry

SHAPELESS_RECIPES = [{"id": name, "inputs": inputs, "result": name, "count": count, "category": "misc"}
                     for name, (inputs, count) in SHAPELESS.items()]


def blocks():
    """The placed dishes: a block for each, named as its food (the block has no item of its own)."""
    return list(placed())


def items():
    return []


def all_placed():
    """Every dish that sets down: the menu's, then the rice slice's (tools/rice.py), then the orchards' juices
    (tools/orchard.py), then the owner's milkshakes (tools/milkshakes.py); MenuDishes.PLACED in this order."""
    import milkshakes
    import orchard
    import rice
    return {**placed(), **rice.placed(), **orchard.placed(), **milkshakes.placed()}
