"""Thanksgiving decorations (docs/features/thanksgiving-decor.md): the harvest market and garden (batch D), the feast
table (E), the warm town and its interiors (F) and the playful statement pieces (G) of the owner's approved list, less
what Jugcraft already had (the record lists every entry and what covers it). Models and textures:
tools/thanksgiving_decor_data.py, built from boxes at one texel to a model pixel in the Harvest Festival Grounds' manner
(tools/harvest_festival_data.py), whose willow, produce and leaf painters they share.

Every piece is decoration, craftable from the start and kept all year: nothing here asks for research, and none of it
follows the calendar. Each is registered by JugcraftAgriculture.registerThanksgivingDecor() (the Java must match
KINDS and the shapes below: tools/check_mod_data.py check_thanksgiving_decor):
- "floor": a FacingDecorationBlock standing on the floor or a table, facing whoever places it, its outline `shape`
  ({x0, y0, z0, x1, y1, z1} in pixels, facing north);
- "candles": a CandleCenterpieceBlock, a FacingDecorationBlock whose candles flint and steel or a fire charge lights and
  an empty hand snuffs (CandleLighting), giving `light` while lit;
- "wall": a WallDecorationBlock hung on the side of a solid block (`depth` out from it, `low` to `high` across and up);
- "garland": a LeafGarlandBlock, a WallDecorationBlock that joins the same garland beside it on the same wall, so a row
  of them hangs as one garland in swags with tied ends only at its two ends;
- "lantern": vanilla's LanternBlock (standing, or hanging under a block), giving `light`;
- "hanging": a HangingGhostBlock, hung from the underside of a block and dropping when that goes, its outline `shape`.
The Autumn Leaf Table Runner is a cloth for the Harvest Feast Table (FeastTableBlock.Runner LEAVES), laid and taken
off like the Bat Table Runner, so it follows tables of any length.
Every recipe follows the agriculture feature switch.
"""

FEATURE = "agriculture"

# Each prop: its batch, kind, name, outline and how it sounds, breaks and shows on maps. Shapes are in pixels facing
# north (the prop's front to the north, towards whoever placed it); strength is the hardness of the block.
PROPS = {
    # D. The harvest market and garden.
    "pumpkin_and_gourd_stack": {"batch": "D", "kind": "floor", "display": "Pumpkin and Gourd Stack",
                                "shape": [0.5, 0.0, 0.5, 15.5, 12.5, 15.5], "color": "COLOR_ORANGE", "sound": "WOOD", "strength": 0.8},
    "wicker_apple_basket": {"batch": "D", "kind": "floor", "display": "Wicker Apple Basket",
                            "shape": [2.0, 0.0, 3.0, 14.0, 13.0, 13.0], "color": "WOOD", "sound": "WOOD", "strength": 0.5},
    "pear_basket": {"batch": "D", "kind": "floor", "display": "Pear Basket",
                    "shape": [1.0, 0.0, 0.5, 15.0, 10.0, 12.5], "color": "WOOD", "sound": "WOOD", "strength": 0.5},
    "root_vegetable_hamper": {"batch": "D", "kind": "floor", "display": "Root Vegetable Hamper",
                              "shape": [1.5, 0.0, 2.5, 14.5, 16.0, 15.0], "color": "WOOD", "sound": "WOOD", "strength": 0.6},
    # E. The feast table (the runner is an item: RUNNER).
    "harvest_place_setting": {"batch": "E", "kind": "floor", "display": "Harvest Place Setting",
                              "shape": [1.5, 0.0, 2.5, 15.0, 5.5, 14.5], "color": "TERRACOTTA_WHITE", "sound": "DECORATED_POT", "strength": 0.3},
    "napkin_and_cutlery_caddy": {"batch": "E", "kind": "floor", "display": "Napkin and Cutlery Caddy",
                                 "shape": [3.5, 0.0, 5.5, 12.5, 9.0, 10.5], "color": "WOOD", "sound": "WOOD", "strength": 0.5},
    "bread_basket": {"batch": "E", "kind": "floor", "display": "Bread Basket",
                     "shape": [1.5, 0.0, 3.0, 14.5, 6.5, 13.0], "color": "WOOD", "sound": "WOOD", "strength": 0.5},
    "gravy_boat": {"batch": "E", "kind": "floor", "display": "Gravy Boat",
                   "shape": [4.0, 0.0, 2.5, 12.0, 5.0, 13.5], "color": "TERRACOTTA_WHITE", "sound": "DECORATED_POT", "strength": 0.3},
    "covered_butter_dish": {"batch": "E", "kind": "floor", "display": "Covered Butter Dish",
                            "shape": [3.5, 0.0, 2.5, 12.5, 5.5, 11.5], "color": "TERRACOTTA_WHITE", "sound": "DECORATED_POT", "strength": 0.3},
    "pie_stand": {"batch": "E", "kind": "floor", "display": "Pie Stand",
                  "shape": [1.5, 0.0, 1.5, 14.5, 14.0, 14.5], "color": "WOOD", "sound": "WOOD", "strength": 0.6},
    # F. The warm town and its interiors.
    "autumn_leaf_wall_garland": {"batch": "F", "kind": "garland", "display": "Autumn Leaf Wall Garland",
                                 "depth": 3.5, "low": 0.0, "high": 16.0, "color": "COLOR_ORANGE", "sound": "GRASS", "strength": 0.2},
    "amber_jar_lantern": {"batch": "F", "kind": "lantern", "display": "Amber Jar Lantern", "light": 13,
                          "color": "COLOR_ORANGE", "sound": "GLASS", "strength": 0.4},
    "pumpkin_candle_centerpiece": {"batch": "F", "kind": "candles", "display": "Pumpkin Candle Centerpiece", "light": 12,
                                   "shape": [1.0, 0.0, 4.5, 15.0, 10.0, 13.5], "color": "WOOD", "sound": "WOOD", "strength": 0.5},
    "harvest_market_sign": {"batch": "F", "kind": "wall", "display": "Harvest Market Sign",
                            "depth": 5.5, "low": 1.0, "high": 15.0, "color": "WOOD", "sound": "WOOD", "strength": 0.8},
    "copper_pan_rack": {"batch": "F", "kind": "wall", "display": "Copper Pan Rack",
                        "depth": 4.0, "low": 1.0, "high": 15.0, "color": "COLOR_ORANGE", "sound": "COPPER", "strength": 1.0},
    # G. The playful statement pieces.
    "turkey_statuette": {"batch": "G", "kind": "floor", "display": "Turkey Statuette",
                         "shape": [1.0, 0.0, 3.0, 15.0, 15.0, 14.0], "color": "COLOR_BROWN", "sound": "WOOD", "strength": 0.8},
    "acorn_leaf_mobile": {"batch": "G", "kind": "hanging", "display": "Acorn and Leaf Mobile",
                          "shape": [2.0, 0.5, 2.0, 15.0, 16.0, 14.0], "color": "COLOR_BROWN", "sound": "WOOD", "strength": 0.2},
    "squirrel_acorn_figurine": {"batch": "G", "kind": "floor", "display": "Squirrel with Acorn Figurine",
                                "shape": [4.5, 0.0, 5.0, 11.5, 11.0, 15.0], "color": "COLOR_ORANGE", "sound": "WOOD", "strength": 0.5},
    "table_cornucopia": {"batch": "G", "kind": "floor", "display": "Table Cornucopia",
                         "shape": [0.0, 0.0, 2.0, 14.5, 8.5, 13.5], "color": "WOOD", "sound": "WOOD", "strength": 0.5},
}
# What breaks each quickest (block tag minecraft:mineable/...): copper, tin and china a pickaxe, the rest (wood, willow,
# leaves) an axe. None needs the tool to drop.
PICKAXE = ("copper_pan_rack", "amber_jar_lantern", "harvest_place_setting", "gravy_boat", "covered_butter_dish")
KINDS = {"floor": "FacingDecorationBlock", "candles": "CandleCenterpieceBlock", "wall": "WallDecorationBlock",
         "garland": "LeafGarlandBlock", "lantern": "LanternBlock", "hanging": "HangingGhostBlock"}

# The Autumn Leaf Table Runner: the Feast Table's `runner` value it lays, and how far it hangs over a table's end.
RUNNER = {"item": "autumn_leaf_table_runner", "display": "Autumn Leaf Table Runner", "runner": "leaves", "width": 6.0, "drape": 5.0}

STICK, WHEAT = "minecraft:stick", "minecraft:wheat"
SHAPELESS = [
    # D: willow woven from sticks round wheat (the Giant Cornucopia's), and what fills it.
    {"id": "pumpkin_and_gourd_stack", "inputs": ["minecraft:pumpkin", "jugcraft:white_pumpkin", "jugcraft:warty_gourd",
                                                 "jugcraft:acorn_squash", "jugcraft:butternut_squash", WHEAT]},
    {"id": "wicker_apple_basket", "inputs": [STICK, STICK, WHEAT, "minecraft:apple", "minecraft:apple", "minecraft:apple"]},
    {"id": "pear_basket", "inputs": [STICK, STICK, WHEAT, "jugcraft:pear", "jugcraft:pear", "jugcraft:pear"]},
    {"id": "root_vegetable_hamper", "inputs": [STICK, STICK, WHEAT, "minecraft:carrot", "minecraft:potato", "minecraft:beetroot",
                                               "jugcraft:turnip"]},
    # E: china is clay, cutlery iron, linen the farm's flax.
    {"id": RUNNER["item"], "inputs": ["jugcraft:flax", "jugcraft:flax", "#minecraft:leaves", "#minecraft:leaves", "minecraft:orange_dye",
                                      "minecraft:red_dye"]},
    {"id": "harvest_place_setting", "inputs": ["minecraft:clay_ball", "minecraft:iron_nugget", "minecraft:iron_nugget", "jugcraft:flax",
                                               "minecraft:orange_dye", "minecraft:glass_bottle"]},
    {"id": "napkin_and_cutlery_caddy", "inputs": ["#minecraft:wooden_slabs", "minecraft:iron_nugget", "minecraft:iron_nugget",
                                                  "minecraft:iron_nugget", "jugcraft:flax"]},
    {"id": "bread_basket", "inputs": [STICK, STICK, WHEAT, "minecraft:bread", "minecraft:bread", "minecraft:red_dye"]},
    {"id": "gravy_boat", "inputs": ["minecraft:clay_ball", "minecraft:clay_ball", "minecraft:bone_meal", "minecraft:brown_mushroom"]},
    {"id": "covered_butter_dish", "inputs": ["minecraft:clay_ball", "minecraft:clay_ball", "minecraft:bone_meal", "jugcraft:butter"]},
    {"id": "pie_stand", "inputs": ["#minecraft:wooden_slabs", "#minecraft:wooden_slabs", STICK, "minecraft:pumpkin_pie", "jugcraft:apple_pie"]},
    # F.
    {"id": "autumn_leaf_wall_garland", "inputs": ["minecraft:string", "#minecraft:leaves", "#minecraft:leaves", "#minecraft:leaves",
                                                  "minecraft:orange_dye", "minecraft:red_dye"], "count": 3},
    {"id": "amber_jar_lantern", "inputs": ["jugcraft:mason_jar", "minecraft:candle", "minecraft:orange_dye", "minecraft:iron_nugget"]},
    {"id": "pumpkin_candle_centerpiece", "inputs": ["#minecraft:wooden_slabs", "minecraft:candle", "minecraft:candle", "minecraft:candle",
                                                    "minecraft:pumpkin", "#minecraft:leaves"]},
    {"id": "harvest_market_sign", "inputs": ["#minecraft:planks", "minecraft:iron_nugget", "minecraft:iron_nugget", "minecraft:orange_dye",
                                             "minecraft:green_dye"]},
    {"id": "copper_pan_rack", "inputs": ["#minecraft:wooden_slabs", "minecraft:copper_ingot", "minecraft:copper_ingot",
                                         "minecraft:copper_ingot", "minecraft:iron_nugget"]},
    # G.
    {"id": "turkey_statuette", "inputs": ["#minecraft:logs", "minecraft:feather", "minecraft:feather", "minecraft:red_dye",
                                          "minecraft:brown_dye"]},
    {"id": "acorn_leaf_mobile", "inputs": [STICK, STICK, "minecraft:string", "jugcraft:acorn", "jugcraft:acorn", "#minecraft:leaves"]},
    {"id": "squirrel_acorn_figurine", "inputs": ["#minecraft:logs", "jugcraft:acorn", "minecraft:orange_dye"]},
    {"id": "table_cornucopia", "inputs": [STICK, STICK, WHEAT, "minecraft:apple", "jugcraft:corn", "minecraft:sweet_berries"]},
]
for _recipe in SHAPELESS:
    _recipe.setdefault("result", _recipe["id"])
    _recipe.setdefault("count", 1)
    _recipe.setdefault("category", "building")
SHAPED = []


def blocks(batch=None):
    return [block for block, spec in PROPS.items() if batch is None or spec["batch"] == batch]


def items():
    return blocks() + [RUNNER["item"]]


def names():
    out = {f"block.jugcraft.{block}": spec["display"] for block, spec in PROPS.items()}
    out[f"item.jugcraft.{RUNNER['item']}"] = RUNNER["display"]
    return out


def java_box(values):
    """A shape as JugcraftAgriculture writes it: new double[] {x0, y0, z0, x1, y1, z1}."""
    return "new double[] {" + ", ".join(f"{float(v)}" for v in values) + "}"
