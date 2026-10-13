"""The Farmhouse Kitchen (the first slice of the kitchen and cooking expansion in docs/branches/AGRICULTURE.md): the
Kitchen Stove, the Skillet, the Cutting Board and its knives, and the kitchen cabinets, drawn in the owner's own farming
and food textures (art/owner-library, imported by tools/owner_art.py). The numbers here are what
agriculture/KitchenStoveBlock(Entity), SkilletBlock(Entity), CuttingBoardBlock(Entity), CuttingRecipe, KitchenKnifeItem
and KitchenCabinetBlock(Entity) use; tools/check_mod_data.py compares them. tools/kitchen_data.py writes the JSON.

The Kitchen Stove is a brick range. Lit with flint and steel or a fire charge (a shovel puts it out), it gives light
`light` and heats the block on top of it like a campfire (block tag jugcraft:heat_sources), so a Cooking Pot, Canning
Kettle, Candy Kettle, Wax Melting Pot or Skillet cooks on it. With nothing on top, its hob cooks up to `slots` foods at
once by vanilla's campfire recipes, `speed` times as fast as a campfire; each pops off when done. Only what a campfire
cooks goes on the hob; anything else (a pot, a skillet, a bucket) is placed or used on top as usual. It burns no fuel,
like a campfire, and anything standing on its hot top is burnt as by a magma block.

The Skillet is an iron pan set on any heat source. Use anything a campfire cooks on it to put up to `capacity` of one
food in; it fries them one after another by the campfire recipe that takes them, `speed` times as fast as a campfire (a
furnace's pace), into the pan beside them. Use it with an empty hand to take back everything in it.

The Cutting Board holds one thing set on it (anything, for show). Use a knife on it to cut what is on it by a cutting
recipe (type jugcraft:cutting: an ingredient, the tool that cuts it, and what it is cut into); each cut costs the knife
one durability. Use it with an empty hand to take back what is on it. Knives (item tag jugcraft:knives, which holds the
Carving Knife too) also cut slices from pies and the roast turkey, and from a vanilla cake (before the cake is eaten, so
a hungry cook still slices it).

A cut is a whole split into parts: the parts' hunger and saturation add up to no more than the whole's, so cutting is
never a gain (docs/BALANCE.md). Raw cuts are barely worth eating; cook them on the stove, in the skillet or in a
furnace, smoker or on a campfire.
"""
FEATURE = "agriculture"

STOVE = {"block": "kitchen_stove", "display": "Kitchen Stove", "slots": 6, "speed": 2, "light": 13, "burn": 1.0}
SKILLET = {"block": "skillet", "display": "Skillet", "capacity": 16, "speed": 3}
BOARD = {"block": "cutting_board", "display": "Cutting Board"}
KNIFE_TAG = "jugcraft:knives"
CUTTING_TYPE = "jugcraft:cutting"

# Kitchen knives: a light, quick blade (attack damage `damage` over the material's, speed `speed`) in each material.
# "material" is the crafting ingredient, set diagonally above a stick; the netherite knife is a smithing upgrade.
KNIFE_DAMAGE = 0.5
KNIFE_SPEED = -2.0
KNIVES = {
    "flint_knife": {"display": "Flint Knife", "tier": "STONE", "material": "minecraft:flint"},
    "iron_knife": {"display": "Iron Knife", "tier": "IRON", "material": "#c:ingots/iron"},
    "bronze_knife": {"display": "Bronze Knife", "tier": "BRONZE", "material": "#c:ingots/bronze", "features": ["tin"]},
    "golden_knife": {"display": "Golden Knife", "tier": "GOLD", "material": "minecraft:gold_ingot"},
    "steel_knife": {"display": "Steel Knife", "tier": "STEEL", "material": "#c:ingots/steel", "features": ["machines"]},
    "diamond_knife": {"display": "Diamond Knife", "tier": "DIAMOND", "material": "minecraft:diamond"},
    "netherite_knife": {"display": "Netherite Knife", "tier": "NETHERITE", "smithing": "diamond_knife"},
}
KNIFE_PATTERN = [" M", "S "]

# Kitchen cabinets: a cupboard of `slots` slots in each wood the owner drew, made from that wood's slabs and trapdoors.
CABINET_SLOTS = 27
CABINET_WOODS = {
    "oak": "Oak", "spruce": "Spruce", "birch": "Birch", "jungle": "Jungle", "acacia": "Acacia", "dark_oak": "Dark Oak",
    "mangrove": "Mangrove", "cherry": "Cherry", "bamboo": "Bamboo", "crimson": "Crimson", "warped": "Warped",
}
CABINET_PATTERN = ["SSS", "T T", "SSS"]


def cabinet(wood):
    return f"{wood}_cabinet"


# The cuts, their cooked forms and the other new foods. "food": [hunger, saturation modifier] as in tools/agriculture.py.
# Each whole's cuts add up to no more than the whole (vanilla's values in the comments: hunger / saturation modifier).
ITEMS = {
    # A raw porkchop (3 / 0.3) cuts into 2 bacon; a cooked porkchop is 8 / 0.8.
    "bacon": {"display": "Bacon", "food": [1, 0.3], "tags": ["c:foods/raw_meat", "minecraft:meat"]},
    "cooked_bacon": {"display": "Cooked Bacon", "food": [4, 0.8], "tags": ["c:foods", "c:foods/cooked_meat", "minecraft:meat"]},
    # Raw beef (3 / 0.3) into 2 minced beef; a steak is 8 / 0.8.
    "minced_beef": {"display": "Minced Beef", "food": [1, 0.3], "tags": ["c:foods/raw_meat", "minecraft:meat"]},
    "beef_patty": {"display": "Beef Patty", "food": [4, 0.8], "tags": ["c:foods", "c:foods/cooked_meat", "minecraft:meat"]},
    # Raw chicken (2 / 0.3) into 2 cuts; cooked chicken is 6 / 0.6.
    "chicken_cuts": {"display": "Chicken Cuts", "food": [1, 0.3], "tags": ["c:foods/raw_meat", "minecraft:meat"]},
    "cooked_chicken_cuts": {"display": "Cooked Chicken Cuts", "food": [3, 0.6],
                            "tags": ["c:foods", "c:foods/cooked_meat", "minecraft:meat"]},
    # Raw mutton (2 / 0.3) into 2 chops; cooked mutton is 6 / 0.8.
    "mutton_chops": {"display": "Mutton Chops", "food": [1, 0.3], "tags": ["c:foods/raw_meat", "minecraft:meat"]},
    "cooked_mutton_chops": {"display": "Cooked Mutton Chops", "food": [3, 0.8],
                            "tags": ["c:foods", "c:foods/cooked_meat", "minecraft:meat"]},
    # Raw cod (2 / 0.1) into 2 slices; cooked cod is 5 / 0.6, so its two cooked slices (2 / 0.6 each) lose a little.
    "cod_slice": {"display": "Cod Slice", "food": [1, 0.1], "tags": ["c:foods/raw_fish"]},
    "cooked_cod_slice": {"display": "Cooked Cod Slice", "food": [2, 0.6], "tags": ["c:foods", "c:foods/cooked_fish"]},
    # Raw salmon (2 / 0.1) into 2 slices; cooked salmon is 6 / 0.8.
    "salmon_slice": {"display": "Salmon Slice", "food": [1, 0.1], "tags": ["c:foods/raw_fish"]},
    "cooked_salmon_slice": {"display": "Cooked Salmon Slice", "food": [3, 0.8], "tags": ["c:foods", "c:foods/cooked_fish"]},
    # A cabbage (3 / 0.6) into 2 leaves, for wraps and rolls later.
    "cabbage_leaf": {"display": "Cabbage Leaf", "food": [1, 0.5], "compost": "medium", "tags": ["c:foods/vegetable"]},
    # A pumpkin (not food) into 4 slices, like a melon's slices.
    "pumpkin_slice": {"display": "Pumpkin Slice", "food": [2, 0.3], "compost": "medium", "tags": ["c:foods/vegetable"]},
    # A cake's slice is one of its seven bites (2 / 0.1), cut to carry away.
    "cake_slice": {"display": "Slice of Cake", "food": [2, 0.1], "compost": "medium_high", "tags": ["c:foods"]},
    # An egg (not food) fried.
    "fried_egg": {"display": "Fried Egg", "food": [3, 0.6], "tags": ["c:foods"]},
}

# Cooked in a furnace, smoker or on a campfire (and so on the stove's hob and in the skillet), vanilla's timings.
COOKING = {
    "cooked_bacon": {"input": "bacon", "xp": 0.35},
    "beef_patty": {"input": "minced_beef", "xp": 0.35},
    "cooked_chicken_cuts": {"input": "chicken_cuts", "xp": 0.35},
    "cooked_mutton_chops": {"input": "mutton_chops", "xp": 0.35},
    "cooked_cod_slice": {"input": "cod_slice", "xp": 0.35},
    "cooked_salmon_slice": {"input": "salmon_slice", "xp": 0.35},
    "fried_egg": {"input": "minecraft:egg", "xp": 0.35},
}

# Cutting recipes (type jugcraft:cutting): what is set on the board, and what a knife cuts it into.
CUTTING = {
    "bacon": {"input": "minecraft:porkchop", "results": [["bacon", 2]]},
    "minced_beef": {"input": "minecraft:beef", "results": [["minced_beef", 2]]},
    "chicken_cuts": {"input": "minecraft:chicken", "results": [["chicken_cuts", 2]]},
    "mutton_chops": {"input": "minecraft:mutton", "results": [["mutton_chops", 2]]},
    "cod_slice": {"input": "minecraft:cod", "results": [["cod_slice", 2], ["minecraft:bone_meal", 1]]},
    "salmon_slice": {"input": "minecraft:salmon", "results": [["salmon_slice", 2], ["minecraft:bone_meal", 1]]},
    "cabbage_leaf": {"input": "jugcraft:cabbage", "results": [["cabbage_leaf", 2]]},
    "pumpkin_slice": {"input": "minecraft:pumpkin", "results": [["pumpkin_slice", 4]]},
    "cake_slice": {"input": "minecraft:cake", "results": [["cake_slice", 7]]},
    # The menu (tools/menu.py): wheat dough cut into pasta, a tortilla into chips.
    "raw_pasta": {"input": "jugcraft:wheat_dough", "results": [["raw_pasta", 2]]},
    "tortilla_chip": {"input": "jugcraft:tortilla", "results": [["tortilla_chip", 2]]},
    # Rice (tools/rice.py): a panicle cut into two rice and its straw; a kelp roll into four slices.
    "rice": {"input": "jugcraft:rice_panicle", "results": [["rice", 2], ["straw", 1]]},
    "kelp_roll_slice": {"input": "jugcraft:kelp_roll", "results": [["kelp_roll_slice", 4]]},
}

# Crafting.
SHAPED = [
    {"id": STOVE["block"], "pattern": ["III", "BCB", "BBB"],
     "key": {"I": "#c:ingots/iron", "B": "minecraft:brick", "C": "minecraft:campfire"},
     "result": STOVE["block"], "count": 1, "category": "misc"},
    {"id": SKILLET["block"], "pattern": ["III", " S "], "key": {"I": "#c:ingots/iron", "S": "minecraft:stick"},
     "result": SKILLET["block"], "count": 1, "category": "misc"},
    {"id": BOARD["block"], "pattern": ["S  ", "PPP"], "key": {"S": "minecraft:stick", "P": "#minecraft:planks"},
     "result": BOARD["block"], "count": 1, "category": "misc"},
]
SHAPED += [{"id": knife, "pattern": KNIFE_PATTERN, "key": {"M": info["material"], "S": "minecraft:stick"}, "result": knife, "count": 1,
            "category": "equipment", "features": info.get("features", [])}
           for knife, info in KNIVES.items() if "material" in info]
SHAPED += [{"id": cabinet(wood), "pattern": CABINET_PATTERN, "key": {"S": f"minecraft:{wood}_slab", "T": f"minecraft:{wood}_trapdoor"},
            "result": cabinet(wood), "count": 1, "category": "building"}
           for wood in CABINET_WOODS]

# The owner's textures each block and item wears: textures/<path>.png from the library's "farming and food textures"
# folder (tools/owner_art.py copies them; the bronze and steel knives are the iron knife recoloured).
TEXTURES = {
    "block/kitchen_stove_front": "stove_front", "block/kitchen_stove_front_on": "stove_front_on",
    "block/kitchen_stove_side": "stove_side", "block/kitchen_stove_top": "stove_top", "block/kitchen_stove_top_on": "stove_top_on",
    "block/kitchen_stove_bottom": "stove_bottom",
    "block/skillet_top": "skillet_top", "block/skillet_side": "skillet_side", "block/skillet_bottom": "skillet_bottom",
    "block/cutting_board": "cutting_board",
}
for _wood in CABINET_WOODS:
    for _part in ("front", "front_open", "side", "top"):
        TEXTURES[f"block/{_wood}_cabinet_{_part}"] = f"{_wood}_cabinet_{_part}"
for _knife in KNIVES:
    if _knife not in ("bronze_knife", "steel_knife"):
        TEXTURES[f"item/{_knife}"] = _knife
for _item in ITEMS:
    TEXTURES[f"item/{_item}"] = _item
# The bronze and steel knives: the iron knife's blade recoloured in the approved bronze and steel (tools/arms_pixel.py).
RECOLOURED = {"item/bronze_knife": ("iron_knife", "BRONZE"), "item/steel_knife": ("iron_knife", "STEEL")}

TEXT = {
    "message.jugcraft.kitchen_stove.unlit": "The stove is out: light it with flint and steel to cook",
    "message.jugcraft.kitchen_stove.full": "The stove top is full",
    "message.jugcraft.skillet.cold": "The skillet needs heat below it",
    "message.jugcraft.skillet.other": "The skillet is busy with something else",
    "message.jugcraft.cutting_board.nothing": "Put something on the board to cut",
    "message.jugcraft.cutting_board.uncuttable": "That knife can't cut this",
    "container.jugcraft.kitchen_cabinet": "Cabinet",
}


def blocks():
    return [STOVE["block"], SKILLET["block"], BOARD["block"]] + [cabinet(wood) for wood in CABINET_WOODS]


def items():
    """The kitchen's items besides its foods (which are in tools/agriculture.py ITEMS): its blocks and the knives."""
    return blocks() + list(KNIVES)
