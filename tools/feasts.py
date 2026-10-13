"""Feasts and food displays (the second slice of the kitchen and cooking expansion in docs/branches/AGRICULTURE.md), in the
owner's own farming and food textures (art/owner-library, imported by tools/owner_art.py). The numbers here are what
agriculture/PieBlock, PlacedPieBlock, FeastBlock, Feast and FoodDisplayBlock use; tools/check_mod_data.py compares them.
tools/feasts_data.py writes the JSON.

Pies. The owner chose (7 October 2026) to give the apple pie their own art, and to let vanilla's pumpkin pie be set down
as their pumpkin pie. Two more pies in their art join the Hearth Oven's: chocolate and the sweet berry cheesecake (their
fillings are in tools/agriculture.py PIES, marked "owner"). A pie in the owner's art wears their top and filling, a
shared crust side and bottom, their slice, and their whole-pie icon as its item.

A placed pie (PLACED_PIES) is a vanilla food item set down as a pie: sneak and use it on a block. It is eaten or cut
`slices` slices at a time like any pie; its slices add up to the whole item exactly, so setting it down is never a gain.
"""
FEATURE = "agriculture"

# Vanilla pies that can be set down as a pie: the block, the vanilla item, its slice, a slice's food (hunger, saturation
# modifier) and the owner's top and filling textures. Vanilla's pumpkin pie is 8 / 0.3, four slices of 2 / 0.3.
PLACED_PIES = {
    "pumpkin_pie": {"display": "Pumpkin Pie", "item": "minecraft:pumpkin_pie", "slice": "pumpkin_pie_slice",
                    "slice_display": "Slice of Pumpkin Pie", "food": [2, 0.3], "whole": [8, 0.3]},
}

# The crust every pie in the owner's art shares: their pie side and bottom.
PIE_SIDE = "baked_pie_side"
PIE_BOTTOM = "baked_pie_bottom"

# The owner's textures each block and item wears: textures/<path>.png from the library's "farming and food textures"
# folder (tools/owner_art.py copies them).
TEXTURES = {
    f"block/{PIE_SIDE}": "pie_side", f"block/{PIE_BOTTOM}": "pie_bottom",
    "block/apple_pie_top": "apple_pie_top", "block/apple_pie_inside": "apple_pie_inner",
    "block/chocolate_pie_top": "chocolate_pie_top", "block/chocolate_pie_inside": "chocolate_pie_inner",
    "block/sweet_berry_cheesecake_top": "sweet_berry_cheesecake_top", "block/sweet_berry_cheesecake_inside": "sweet_berry_cheesecake_inner",
    "block/pumpkin_pie_top": "pumpkin_pie_top", "block/pumpkin_pie_inside": "pumpkin_pie_inner",
    "item/apple_pie": "apple_pie", "item/apple_pie_slice": "apple_pie_slice",
    "item/chocolate_pie": "chocolate_pie", "item/chocolate_pie_slice": "chocolate_pie_slice",
    "item/sweet_berry_cheesecake": "sweet_berry_cheesecake", "item/sweet_berry_cheesecake_slice": "sweet_berry_cheesecake_slice",
    "item/pumpkin_pie_slice": "pumpkin_pie_slice",
}


def owner_pies():
    """The Hearth Oven pies in the owner's art: their block names."""
    import agriculture as ag
    return [ag.pie_name(f) for f, info in ag.PIES["fillings"].items() if info.get("owner")]




# A feast's servings give at most this much hunger over its ingredients (the cook's work); tools/check_mod_data.py checks.
COOK_BONUS = 1

# Feasts: placed whole, then served a serving at a time. Use a bowl on one to take a serving to go (the serving item, a
# bowl food that gives the bowl back), or use it with an empty hand while hungry to eat a serving there. The last serving
# leaves the leftovers, which an empty hand clears (dropping `leftovers`). Only a whole feast picks up again. Each serving
# gives `food` (hunger, saturation modifier); `servings` of them add up to about what the ingredients give, never far more
# (docs/BALANCE.md): roast chicken 19 hunger in, 20 out; ham 29 in, 28 out; shepherd's pie 19 in, 20 out; stuffed pumpkin
# 12 in (20 with the pumpkin cut into slices), 20 out; gleaming salad 13 in, 12 out. `light` is the block's light while
# any is left (glow berries). `textures` are the model's (texture key -> runtime texture); `particle` its particle.
FEASTS = {
    "roast_chicken": {"display": "Roast Chicken", "servings": 4, "food": [5, 0.7], "light": 0,
                      "serving": "bowl_of_roast_chicken", "serving_display": "Bowl of Roast Chicken",
                      "leftovers": [["minecraft:bone", 1]],
                      "inputs": ["minecraft:cooked_chicken", "minecraft:baked_potato", "minecraft:carrot", "jugcraft:onion", "minecraft:bread"],
                      "textures": {"platter": "platter", "leftovers": "roast_chicken_leftovers", "side_dish": "roast_chicken_side_dish",
                                   "chicken": "roast_chicken", "details": "roast_chicken_details"},
                      "particle": "roast_chicken"},
    "honey_glazed_ham": {"display": "Honey-Glazed Ham", "servings": 4, "food": [7, 0.8], "light": 0,
                         "serving": "bowl_of_honey_glazed_ham", "serving_display": "Bowl of Honey-Glazed Ham",
                         "leftovers": [["minecraft:bone", 1]],
                         "inputs": ["minecraft:cooked_porkchop", "minecraft:cooked_porkchop", "minecraft:honey_bottle",
                                    "minecraft:sweet_berries", "minecraft:baked_potato"],
                         "textures": {"platter": "platter", "leftovers": "honey_glazed_ham_leftovers", "side_dish": "honey_glazed_ham_side_dish",
                                      "ham": "honey_glazed_ham", "details": "honey_glazed_ham_details"},
                         "particle": "honey_glazed_ham"},
    "shepherds_pie": {"display": "Shepherd's Pie", "servings": 4, "food": [5, 0.7], "light": 0,
                      "serving": "bowl_of_shepherds_pie", "serving_display": "Bowl of Shepherd's Pie", "leftovers": [],
                      "inputs": ["jugcraft:cooked_mutton_chops", "jugcraft:cooked_mutton_chops", "minecraft:baked_potato",
                                 "minecraft:baked_potato", "minecraft:carrot", "jugcraft:onion"],
                      "textures": {"platter": "platter", "leftovers": "shepherds_pie_leftovers", "top": "shepherds_pie_top",
                                   "side": "shepherds_pie_side", "inside": "shepherds_pie_inside"},
                      "particle": "shepherds_pie_top"},
    "stuffed_pumpkin": {"display": "Stuffed Pumpkin", "servings": 4, "food": [5, 0.6], "light": 0,
                        "serving": "bowl_of_stuffed_pumpkin", "serving_display": "Bowl of Stuffed Pumpkin",
                        "leftovers": [["minecraft:pumpkin_seeds", 2]],
                        "inputs": ["minecraft:pumpkin", "minecraft:bread", "minecraft:baked_potato", "jugcraft:onion",
                                   "minecraft:brown_mushroom", "minecraft:sweet_berries"],
                        "textures": {"side": "stuffed_pumpkin_side", "top": "stuffed_pumpkin_top", "top_eaten": "stuffed_pumpkin_top_eaten",
                                     "bottom": "stuffed_pumpkin_bottom", "details": "stuffed_pumpkin_details"},
                        "particle": "stuffed_pumpkin_side"},
    "gleaming_salad": {"display": "Gleaming Salad", "servings": 4, "food": [3, 0.6], "light": 6,
                       "serving": "bowl_of_gleaming_salad", "serving_display": "Bowl of Gleaming Salad", "leftovers": [],
                       "inputs": ["minecraft:glow_berries", "minecraft:glow_berries", "minecraft:melon_slice", "minecraft:melon_slice",
                                  "jugcraft:cabbage", "minecraft:sweet_berries"],
                       "textures": {"bowl": "salad_bowl", "top": "gleaming_salad", "leftovers": "gleaming_salad_leftovers",
                                    "details": "gleaming_salad_details"},
                       "particle": "gleaming_salad"},
    # The menu (tools/menu.py) adds nachos: a tray of tortilla chips under beef, tomato and pepper, eaten a quarter at a
    # time down to the last bitten chip. 13 hunger in, 12 out.
    "nachos": {"display": "Nachos", "servings": 4, "food": [3, 0.6], "light": 0,
               "serving": "bowl_of_nachos", "serving_display": "Bowl of Nachos", "leftovers": [],
               "inputs": ["jugcraft:tortilla_chip", "jugcraft:tortilla_chip", "jugcraft:tortilla_chip", "jugcraft:tortilla_chip",
                          "jugcraft:beef_patty", "jugcraft:tomato", "jugcraft:pepper"],
               "textures": {"top": "nacho_top", "side": "nacho_side", "inside": "nacho_inner", "chip": "nacho_chip",
                            "eaten": "nacho_chip_eaten"},
               "particle": "nacho_top"},
}
# The bowl a serving is taken with; a serving gives it back when eaten.
SERVING_BOWL = "minecraft:bowl"

# Food displays: show what is set on them (anything; one of a thing to a place, chosen by where it is used), like the
# Witch's Workshop's displays (ShowcaseBlockEntity). `places` and where they lie ({x, z} pixels in a display facing north,
# at height `height`, `scale` of a whole item across) are what ShowcaseRenderer draws; tools/check_mod_data.py compares.
DISPLAYS = {
    "plate": {"display": "Plate", "places": 1, "height": 1.05, "scale": 0.5, "layout": [[8, 8]],
              "textures": {"plate": "plate"}, "particle": "plate",
              "recipe": {"pattern": ["TT"], "key": {"T": "minecraft:white_terracotta"}, "count": 4}},
    "platter": {"display": "Platter", "places": 4, "height": 1.05, "scale": 0.42, "layout": [[5, 5], [11, 5], [5, 11], [11, 11]],
                "textures": {"platter": "platter"}, "particle": "platter",
                "recipe": {"pattern": ["SSS"], "key": {"S": "#minecraft:wooden_slabs"}, "count": 1}},
    "serving_tray": {"display": "Serving Tray", "places": 4, "height": 1.05, "scale": 0.42, "layout": [[5, 5], [11, 5], [5, 11], [11, 11]],
                     "textures": {"rim": "serving_tray", "base": "serving_tray_bottom"}, "particle": "serving_tray",
                     "recipe": {"pattern": ["S S", "PPP"], "key": {"S": "minecraft:stick", "P": "#minecraft:wooden_slabs"}, "count": 1}},
}

# The feasts' and displays' textures from the owner's library (runtime path -> library name); the plate is drawn by code
# (tools/feasts_textures.py), as the library has no plate.
TEXTURES.update({
    "block/platter": "platter", "block/serving_tray": "tray", "block/serving_tray_bottom": "tray_bottom", "block/salad_bowl": "bowl",
    "block/roast_chicken": "roast_chicken", "block/roast_chicken_details": "roast_chicken_details",
    "block/roast_chicken_side_dish": "roast_chicken_side_dish", "block/roast_chicken_leftovers": "roast_chicken_leftovers",
    "block/honey_glazed_ham": "honey_glazed_ham", "block/honey_glazed_ham_details": "honey_glazed_ham_details",
    "block/honey_glazed_ham_side_dish": "honey_glazed_ham_side_dish", "block/honey_glazed_ham_leftovers": "honey_glazed_ham_leftovers",
    "block/shepherds_pie_top": "shepherds_pie_top", "block/shepherds_pie_side": "shepherds_pie_side",
    "block/shepherds_pie_inside": "shepherds_pie_inner", "block/shepherds_pie_leftovers": "shepherds_pie_leftovers",
    "block/stuffed_pumpkin_top": "stuffed_pumpkin_top", "block/stuffed_pumpkin_top_eaten": "stuffed_pumpkin_top_eaten",
    "block/stuffed_pumpkin_side": "stuffed_pumpkin_side", "block/stuffed_pumpkin_bottom": "stuffed_pumpkin_bottom",
    "block/stuffed_pumpkin_details": "stuffed_pumpkin_details",
    "block/gleaming_salad": "gleaming_salad", "block/gleaming_salad_details": "gleaming_salad_details",
    "block/gleaming_salad_leftovers": "gleaming_salad_leftovers",
    # the whole feasts' icons, and the two servings the owner drew in their bowls
    "item/roast_chicken": "roast_chicken_block", "item/honey_glazed_ham": "honey_glazed_ham_block",
    "item/shepherds_pie": "shepherds_pie_block", "item/stuffed_pumpkin": "stuffed_pumpkin_block", "item/gleaming_salad": "gleaming_salad_block",
    "item/bowl_of_shepherds_pie": "shepherds_pie", "item/bowl_of_stuffed_pumpkin": "stuffed_pumpkin",
    # the menu's nachos
    "block/nacho_top": "nacho_top", "block/nacho_side": "nacho_side", "block/nacho_inner": "nacho_inner",
    "block/nacho_chip": "nacho_chip", "block/nacho_chip_eaten": "nacho_chip_eaten",
    "item/nachos": "nachos_block", "item/bowl_of_nachos": "nachos_bowl",
})

# The servings the owner drew no icon for: their bowl (from their shepherd's pie serving: the rows below `bowl_from`)
# with the dish heaped in it (a window of their whole-feast icon, `crop` = left, top, right, bottom, set at `at`).
COMPOSED = {
    "item/bowl_of_roast_chicken": {"bowl": "shepherds_pie", "bowl_from": 9, "dish": "roast_chicken_block", "crop": [2, 1, 14, 10], "at": [2, 1]},
    "item/bowl_of_honey_glazed_ham": {"bowl": "shepherds_pie", "bowl_from": 9, "dish": "honey_glazed_ham_block", "crop": [1, 2, 15, 10], "at": [1, 1]},
    "item/bowl_of_gleaming_salad": {"bowl": "shepherds_pie", "bowl_from": 9, "dish": "gleaming_salad_block", "crop": [1, 0, 15, 9], "at": [1, 1]},
}

TEXT = {
    "message.jugcraft.feast.full": "You're full: use a bowl to take a serving away",
}

ITEMS = {
    "pumpkin_pie_slice": {"display": "Slice of Pumpkin Pie", "food": [2, 0.3], "compost": "medium_high", "tags": ["c:foods", "c:foods/pie"]},
}
for _name, _info in FEASTS.items():
    ITEMS[_info["serving"]] = {"display": _info["serving_display"], "food": list(_info["food"]), "stew": True, "tags": ["c:foods"]}

SHAPELESS = [{"id": name, "inputs": info["inputs"], "result": name, "count": 1, "category": "misc"} for name, info in FEASTS.items()]
SHAPED = [{"id": name, "pattern": info["recipe"]["pattern"], "key": info["recipe"]["key"], "result": name,
           "count": info["recipe"]["count"], "category": "misc"} for name, info in DISPLAYS.items()]


def feast_blocks():
    return list(FEASTS)


def blocks():
    return list(PLACED_PIES) + list(FEASTS) + list(DISPLAYS)


def items():
    """The slice's items besides its foods (which join tools/agriculture.py ITEMS): the feasts and displays. The placed
    pies have no item of their own; they are the vanilla item set down."""
    return list(FEASTS) + list(DISPLAYS)
