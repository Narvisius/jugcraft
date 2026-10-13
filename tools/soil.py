"""Soil, compost and storage (the fifth slice of the kitchen and cooking expansion in docs/branches/AGRICULTURE.md), in the
owner's own art (art/owner-library, imported by tools/owner_art.py): Organic Compost that ripens into Rich Soil, Rich Soil
and its farmland, which speed whatever grows on them, crates for the farm's produce, a bag of corn kernels, and the
owner's wooden and bamboo baskets. The numbers here are what agriculture/RichSoilBlock, RichFarmlandBlock,
OrganicCompostBlock, BasketBlock(Entity) and JugcraftAgriculture use; tools/check_mod_data.py compares them.
tools/soil_data.py writes the JSON.

As the owner chose (7 October 2026, "make the baskets be storage blocks and crates be seperate aswell"): the baskets are
storage blocks of their own, leaving the Foraging Basket as it is, and the produce crates stand beside the Halloween
Pumpkin Crate rather than replacing it.

Growth: Rich Soil and Rich Soil Farmland give the plant standing on them an extra random tick each time they get one
of their own (BOOST), so it grows about twice as fast on top of every other bonus. Rich Soil counts as dirt (saplings,
flowers, and rice in water over it); Rich Soil Farmland takes crops as farmland does, keeps its moisture as farmland
does, is never trampled, and dries back into Rich Soil, not dirt.

Storage: a crate or bag packs exactly nine of its item and unpacks into the same nine (UNPACKING, left out of the
recipe-loop check as the rice slice's are). A basket holds BASKET_SLOTS stacks and takes in items that land in its open
top.
"""
FEATURE = "agriculture"

# Rich soil: the plant standing on it gets BOOST extra random ticks for each of the soil's own.
RICH_SOIL = {"block": "rich_soil", "display": "Rich Soil", "texture": "rich_soil"}
RICH_FARMLAND = {"block": "rich_soil_farmland", "display": "Rich Soil Farmland",
                 "textures": {"dry": "rich_soil_farmland", "moist": "rich_soil_farmland_moist",
                              "moist_side": "rich_soil_farmland_moist_side"}}
BOOST = 1
# Farmland keeps moist within WATER_REACH blocks of water (and one up), as vanilla's does.
WATER_REACH = 4

# Organic compost: dirt, straw, bone meal and rotten flesh, left to rot. Each random tick turns it one stage (of
# STAGES) with a 1 in TURN_CHANCE chance, or surely while water touches it; after the last stage it is Rich Soil.
COMPOST = {"block": "organic_compost", "display": "Organic Compost", "stages": 4, "turn_chance": 2,
           "textures": ["organic_compost_stage0", "organic_compost_stage1", "organic_compost_stage2", "organic_compost_stage3"]}

# Crates: nine of a crop to a crate, in the owner's crate art (each crop's side and top, and one bottom for all).
CRATE_BOTTOM = "crate_bottom"
CRATES = {
    "beetroot_crate": {"display": "Beetroot Crate", "item": "minecraft:beetroot"},
    "cabbage_crate": {"display": "Cabbage Crate", "item": "jugcraft:cabbage"},
    "carrot_crate": {"display": "Carrot Crate", "item": "minecraft:carrot"},
    "corn_crate": {"display": "Corn Crate", "item": "jugcraft:corn"},
    "onion_crate": {"display": "Onion Crate", "item": "jugcraft:onion"},
    "potato_crate": {"display": "Potato Crate", "item": "minecraft:potato"},
    "tomato_crate": {"display": "Tomato Crate", "item": "jugcraft:tomato"},
}
for _crate, _info in CRATES.items():
    _info["unpack"] = f"{_info['item'].split(':')[1]}_from_crate"
    _info["textures"] = {"side": f"{_crate}_side", "top": f"{_crate}_top", "bottom": CRATE_BOTTOM}

# The Bag of Corn Kernels: the owner's kernel bag top on their rice sack (tools/rice.py), turned as the Bag of Rice is.
SACKS = {"corn_kernel_bag": {"display": "Bag of Corn Kernels", "item": "jugcraft:corn_kernels", "unpack": "corn_kernels_from_bag",
                             "top": "corn_kernel_bag_top"}}
SACK_TEXTURES = {"side": "rice_bag_side", "front": "rice_bag_side_tied", "bottom": "rice_bag_bottom"}
PACK = 9

# Baskets: the owner's woven baskets, open at the top. Each holds BASKET_SLOTS stacks (a 3 by 3 screen) and takes in
# items that come to rest inside it, at most one stack every PICKUP_TICKS ticks.
BASKET_SLOTS = 9
PICKUP_TICKS = 4
BASKETS = {
    "wooden_basket": {"display": "Wooden Basket", "pattern": ["S S", "P P", "PPP"], "key": {"S": "minecraft:stick", "P": "#minecraft:planks"},
                      "textures": {"side": "wooden_basket_side", "top": "wooden_basket_top", "bottom": "wooden_basket_bottom"}},
    "bamboo_basket": {"display": "Bamboo Basket", "pattern": ["B B", "B B", "BBB"], "key": {"B": "minecraft:bamboo"},
                      "textures": {"side": "bamboo_basket_side", "top": "bamboo_basket_top", "bottom": "bamboo_basket_bottom"}},
}

# Recipes.
SHAPELESS = [
    {"id": COMPOST["block"], "inputs": ["minecraft:dirt", "jugcraft:straw", "jugcraft:straw", "jugcraft:straw", "jugcraft:straw",
                                        "minecraft:bone_meal", "minecraft:bone_meal", "minecraft:rotten_flesh", "minecraft:rotten_flesh"],
     "result": COMPOST["block"], "count": 1, "category": "building"},
]
SHAPED = []
for _block, _info in list(CRATES.items()) + list(SACKS.items()):
    SHAPED.append({"id": _block, "pattern": ["###", "###", "###"], "key": {"#": _info["item"]}, "result": _block, "count": 1,
                   "category": "building"})
    SHAPELESS.append({"id": _info["unpack"], "inputs": [f"jugcraft:{_block}"], "result": _info["item"], "count": PACK,
                      "category": "misc"})
for _block, _info in BASKETS.items():
    SHAPED.append({"id": _block, "pattern": _info["pattern"], "key": _info["key"], "result": _block, "count": 1, "category": "misc"})
# Unpacking gives back exactly what packed it; the recipe-loop check leaves these out.
UNPACKING = [info["unpack"] for info in list(CRATES.values()) + list(SACKS.values())]

# The owner's textures (runtime path -> library name, tools/owner_art.py).
TEXTURES = {f"block/{name}": name for name in [RICH_SOIL["texture"], *RICH_FARMLAND["textures"].values(), *COMPOST["textures"],
                                                 CRATE_BOTTOM]}
for _crate in CRATES:
    TEXTURES.update({f"block/{_crate}_side": f"{_crate}_side", f"block/{_crate}_top": f"{_crate}_top"})
TEXTURES["block/corn_kernel_bag_top"] = "corn_kernal_bag_top"
for _info in BASKETS.values():
    TEXTURES.update({f"block/{name}": name for name in _info["textures"].values()})


def blocks():
    return ([RICH_SOIL["block"], RICH_FARMLAND["block"], COMPOST["block"]] + list(CRATES) + list(SACKS) + list(BASKETS))


def items():
    """The slice's block items (Rich Soil Farmland has none: a hoe makes it, and it drops Rich Soil)."""
    return [RICH_SOIL["block"], COMPOST["block"]] + list(CRATES) + list(SACKS) + list(BASKETS)


def itemless():
    return [RICH_FARMLAND["block"]]
