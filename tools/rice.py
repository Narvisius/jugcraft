"""Rice and wet farming (the fourth slice of the kitchen and cooking expansion in docs/branches/AGRICULTURE.md), in the
owner's own art (art/owner-library, imported by tools/owner_art.py): rice grown in paddies, wild rice in swamps and along
rivers, rice and straw stored in bags and bales, tatami woven from the straw, and the rice dishes and rolls the menu (slice
3) left for this slice. The numbers here are what agriculture/TallCrop (RICE), PaddyCropBlock, WildRiceBlock, TatamiBlock,
TatamiMatBlock, RollMedleyBlock and JugcraftAgriculture use; tools/check_mod_data.py compares them. tools/rice_data.py
writes the JSON.

The rice plant is a paddy crop: one of TallCrop's crops (picked when ripe and left standing, like corn), rooted in a still
water source one block deep over bog soil (the cranberry bog's block tag) with open air above the water. It holds its
water, so breaking it leaves the paddy flooded. Its soil counts as moist farmland while water covers it, so a paddy grows
as fast as a watered field. The owner drew it in two parts: four stages in the water, then the stalks (rice_supporting)
with the panicles growing above them in four more; picking takes the panicles and the stalks grow new ones.

Rice is its own seed (it plants the crop in shallow water, as cranberries plant their bush). A Cutting Board cuts a
panicle into two rice and a straw; by hand a panicle gives one rice.

Balance (docs/BALANCE.md, tools/menu.py COOK_BONUS): a grain counts as the bread vanilla bakes from it, 5/3 hunger each
(three wheat make a loaf of 5), and rice counts as wheat does, so a dish of rice gives at most COOK_BONUS more than its
ingredients. The rice roll medley serves exactly the rolls it is made of.
"""
FEATURE = "agriculture"

# Rice: a TallCrop (tools/agriculture.py TALL_CROPS) that grows in a paddy. heights: blocks tall at each age; pick: what a
# ripe plant gives when picked, after which it is pick_reset again (the stalks, the panicles regrowing).
CROP = {
    "block": "rice_crop", "display": "Rice Plant", "seed": "rice", "paddy": True,
    "heights": [1, 1, 1, 1, 2, 2, 2, 2],
    "pick": {"item": "rice_panicle", "min": 2, "max": 3}, "pick_reset": 4, "growth_time": 1.25,
    "textures": [["rice_stage0"], ["rice_stage1"], ["rice_stage2"], ["rice_stage3"],
                 ["rice_supporting", "rice_panicles_stage0"], ["rice_supporting", "rice_panicles_stage1"],
                 ["rice_supporting", "rice_panicles_stage2"], ["rice_supporting", "rice_panicles_stage3"]],
}

# Wild rice: two blocks tall, its foot in shallow water over bog soil and its top in the air, found in swamps and along
# rivers. Broken, it gives 1-2 rice (shears take the plant itself). patch: as the cranberry bog's (tools/agriculture.py).
WILD_RICE = {"block": "wild_rice", "display": "Wild Rice", "drops": {"item": "rice", "min": 1, "max": 2},
             "biomes": ["IS_SWAMP", "IS_RIVER"], "patch": {"rarity": 4, "tries": 24, "spread_xz": 6},
             "textures": {"lower": "wild_rice_bottom", "upper": "wild_rice_top"}}

# Storage blocks: nine of an item packed into one block and back. A bale softens a fall as a hay bale does.
STORAGE = {
    "rice_bag": {"display": "Bag of Rice", "item": "rice", "kind": "bag", "unpack": "rice_from_bag"},
    "rice_bale": {"display": "Rice Bale", "item": "rice_panicle", "kind": "bale", "unpack": "rice_panicle_from_bale"},
    "straw_bale": {"display": "Straw Bale", "item": "straw", "kind": "bale", "unpack": "straw_from_bale"},
}
PACK = 9

# Tatami, woven from straw: the Tatami (a full block that pairs with a tatami placed against its side into one two-block
# mat, the owner's even and odd halves), the Full Tatami Mat (a thin mat two blocks long, placed like a bed) and the Half
# Tatami Mat (one block).
TATAMI = {"block": "tatami", "display": "Tatami"}
TATAMI_MATS = {"full_tatami_mat": {"display": "Full Tatami Mat", "length": 2},
               "half_tatami_mat": {"display": "Half Tatami Mat", "length": 1}}

# Items: rice is the seed; the panicle is what a ripe plant gives; straw comes from cutting panicles.
ITEMS = {
    "rice": {"display": "Rice", "plants": "rice_crop", "bog_seed": True, "compost": "low",
             "tags": ["c:seeds/rice", "c:crops/rice", "minecraft:chicken_food"]},
    "rice_panicle": {"display": "Rice Panicle", "compost": "medium", "tags": []},
    "straw": {"display": "Straw", "compost": "low", "tags": []},
}

# Dishes, in the menu's form (tools/menu.py DISHES): food [hunger, saturation modifier], kind and the model each sets down
# as (tools/menu_data.py templates).
DISHES = {
    "cooked_rice": {"display": "Cooked Rice", "food": [6, 0.5], "kind": "stew", "model": ["bowl", {"heap": 2}]},
    "fried_rice": {"display": "Fried Rice", "food": [10, 0.7], "kind": "stew", "model": ["bowl", {"heap": 2}]},
    "mushroom_rice": {"display": "Mushroom Rice", "food": [9, 0.6], "kind": "stew", "model": ["plate"]},
    "salmon_roll": {"display": "Salmon Roll", "food": [5, 0.6], "kind": "meal", "model": ["flat"]},
    "cod_roll": {"display": "Cod Roll", "food": [4, 0.6], "kind": "meal", "model": ["flat"]},
    "kelp_roll": {"display": "Kelp Roll", "food": [12, 0.6], "kind": "meal", "model": ["flat"]},
    "kelp_roll_slice": {"display": "Kelp Roll Slice", "food": [3, 0.6], "kind": "meal", "model": ["flat"]},
}
for _name, _info in DISHES.items():
    _entry = {"display": _info["display"], "food": list(_info["food"]), "tags": ["c:foods"]}
    if _info["kind"] == "stew":
        _entry["stew"] = True
    ITEMS[_name] = _entry

# The rice roll medley: the owner's platter of rolls, set down whole and served a roll at a time (the last ones first, as
# PIECES lists them from the top), then cleared for the platter it stood on. Made from the rolls it serves, so it is
# never a gain. Its block shares nothing with the feasts; a whole one picks up again.
MEDLEY = {"block": "rice_roll_medley", "display": "Rice Roll Medley", "platter": "platter",
          "pieces": ["kelp_roll_slice", "kelp_roll_slice", "cod_roll", "salmon_roll",
                     "kelp_roll_slice", "kelp_roll_slice", "cod_roll", "salmon_roll"]}

# Recipes. Cooking Pot (tools/agriculture.py POT_RECIPES).
POT_RECIPES = {
    "cooked_rice": {"inputs": {"minecraft:bowl": 1, "jugcraft:rice": 2}, "time": 200},
    "fried_rice": {"inputs": {"minecraft:bowl": 1, "jugcraft:rice": 2, "minecraft:egg": 1, "minecraft:carrot": 1,
                              "jugcraft:onion": 1}, "time": 200},
    "mushroom_rice": {"inputs": {"minecraft:bowl": 1, "jugcraft:rice": 2, "minecraft:brown_mushroom": 1,
                                 "minecraft:red_mushroom": 1, "minecraft:carrot": 1}, "time": 200},
}
# Cutting Board (tools/kitchen.py CUTTING).
CUTTING = {
    "rice": {"input": "jugcraft:rice_panicle", "results": [["rice", 2], ["straw", 1]]},
    "kelp_roll_slice": {"input": "jugcraft:kelp_roll", "results": [["kelp_roll_slice", 4]]},
}
# Crafting.
SHAPELESS = [
    {"id": "rice_from_panicle", "inputs": ["jugcraft:rice_panicle"], "result": "rice", "count": 1, "category": "misc"},
    {"id": "salmon_roll", "inputs": ["jugcraft:rice", "jugcraft:salmon_slice", "jugcraft:salmon_slice"], "result": "salmon_roll",
     "count": 2, "category": "misc"},
    {"id": "cod_roll", "inputs": ["jugcraft:rice", "jugcraft:cod_slice", "jugcraft:cod_slice"], "result": "cod_roll",
     "count": 2, "category": "misc"},
    {"id": "kelp_roll", "inputs": ["minecraft:dried_kelp", "minecraft:dried_kelp", "minecraft:dried_kelp", "jugcraft:rice",
                                   "jugcraft:rice", "minecraft:carrot"], "result": "kelp_roll", "count": 1, "category": "misc"},
    {"id": "rice_roll_medley", "inputs": ["jugcraft:platter", "jugcraft:kelp_roll", "jugcraft:salmon_roll", "jugcraft:salmon_roll",
                                          "jugcraft:cod_roll", "jugcraft:cod_roll"], "result": "rice_roll_medley", "count": 1,
     "category": "misc"},
    {"id": "full_tatami_mat", "inputs": ["jugcraft:tatami"], "result": "full_tatami_mat", "count": 1, "category": "building"},
    {"id": "half_tatami_mat", "inputs": ["jugcraft:full_tatami_mat"], "result": "half_tatami_mat", "count": 2, "category": "building"},
]
SHAPED = [{"id": "tatami", "pattern": ["SS", "SS"], "key": {"S": "jugcraft:straw"}, "result": "tatami", "count": 1, "category": "building"}]
for _block, _info in STORAGE.items():
    SHAPED.append({"id": _block, "pattern": ["###", "###", "###"], "key": {"#": f"jugcraft:{_info['item']}"}, "result": _block,
                   "count": 1, "category": "building"})
    SHAPELESS.append({"id": _info["unpack"], "inputs": [f"jugcraft:{_block}"], "result": _info["item"], "count": PACK,
                      "category": "misc"})
# Unpacking a storage block gives back exactly what packed it; the recipe-loop check leaves these pairs out.
UNPACKING = [info["unpack"] for info in STORAGE.values()]

# The owner's textures (runtime path -> library name, tools/owner_art.py), and every placed dish's icon again as its
# model's texture (block/menu/<dish>, as tools/menu.py does).
TEXTURES = {f"block/{name}": name for stage in CROP["textures"] for name in stage}
TEXTURES.update({f"block/{name}": name for name in WILD_RICE["textures"].values()})
TEXTURES.update({f"block/{name}": name for name in (
    "rice_bag_top", "rice_bag_side", "rice_bag_side_tied", "rice_bag_bottom", "rice_bale_top", "rice_bale_side", "rice_bale_bottom",
    "straw_bale_end", "straw_bale_side", "tatami", "tatami_even", "tatami_odd", "tatami_mat_even", "tatami_mat_odd",
    "tatami_mat_half", "tatami_mat_side", "rice_roll_medley")})
TEXTURES.update({f"item/{name}": name for name in ("rice", "rice_panicle", "straw", "full_tatami_mat", "half_tatami_mat")})
TEXTURES["item/rice_roll_medley"] = "rice_roll_medley_block"
TEXTURES.update({f"item/{name}": name for name in DISHES})
TEXTURES.update({f"block/menu/{name}": name for name, info in DISHES.items() if "model" in info})


def placed():
    """The rice dishes that set down, with their models (as tools/menu.py placed())."""
    return {name: info["model"] for name, info in DISHES.items() if "model" in info}


def blocks():
    return [WILD_RICE["block"]] + list(STORAGE) + [TATAMI["block"]] + list(TATAMI_MATS) + [MEDLEY["block"]] + list(placed())


def items():
    """The slice's block items (its plain items and foods join tools/agriculture.py ITEMS)."""
    return [WILD_RICE["block"]] + list(STORAGE) + [TATAMI["block"]] + list(TATAMI_MATS) + [MEDLEY["block"]]


def itemless():
    """The set-down dishes: blocks named as their food, with no item of their own."""
    return list(placed())
