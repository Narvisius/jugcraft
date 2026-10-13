"""Orchards (the sixth slice of the kitchen and cooking expansion in docs/branches/AGRICULTURE.md): pear, peach, lemon and
orange trees, which fruit every year without being cut down, as the apple tree does (tools/agriculture.py CIDER), and what
is made of their fruit: two juices, two pies and three preserves. The owner chose (7 October 2026) to build this slice in
Jugcraft's own art, the owner's library having no fruit trees ("Slice 6, Jugcraft art"), and to plant these four trees
of the roster (no grapes, hops or berries yet). Every texture is drawn by tools/orchard_textures.py. The numbers here are
what agriculture/OrchardTree, OrchardLeavesBlock and JugcraftAgriculture use; tools/check_mod_data.py compares them.
tools/orchard_data.py writes the JSON.

A tree grows from its seed (a pear's seeds, a peach's pit) planted as a sapling, into a tree of vanilla oak logs with a
crown of its own leaves. Leaves the tree grew itself, with air below them, blossom (fruit 1) and then hang with ripe fruit
(fruit 2), a stage in FRUIT_CHANCE random ticks each, about a Minecraft day in all; a right-click picks the ripe fruit,
which drops below, and the leaves start again. Broken, the leaves drop as the apple's do: now and then a seed, sticks, and
the ripe fruit. A fruit crafts into its seed, so a single fruit found or traded starts an orchard.

The trees grow wild: in the Orchard (pears and peaches), the Mediterranean Forest and the Subtropics (lemons and oranges),
among the trees those biomes already have (tools/biomes.py), and, outside Jugcraft's biomes, in the vanilla biomes each
tree's `biomes` tags name (a patch like the apple tree's, one tree in `rarity` chunks).

Balance (docs/BALANCE.md, tools/menu.py COOK_BONUS): a fruit gives what an apple gives (the lemon, too sour to eat whole,
half). A juice gives at most COOK_BONUS more than its fruit; a pie's four slices and a jar's four servings stay within
COOK_BONUS of what went into them (tools/check_mod_data.py checks every dish, pie and preserve).
"""
FEATURE = "agriculture"

# One fruit stage (bare -> blossom -> ripe) in this many random ticks, as the apple tree's (OrchardLeavesBlock.FRUIT_CHANCE).
FRUIT_CHANCE = 10

# The trees, in the order of the OrchardTree enum. fruit: [hunger, saturation modifier]; pick: fruit a ripe cluster gives
# [min, max]; trunk and foliage: the tree's shape (a straight trunk `base_height` tall plus up to `height_rand_a`, of oak
# logs or the tree's own `log`, and a blob of leaves `radius` across and `height` deep, or the foliage `type` vanilla
# places: the pear tall and pointed, the peach low and wide, the citrus small and round); biomes: the vanilla biome tags
# (ConventionalBiomeTags) a wild patch grows in, one tree in `rarity` chunks; regions: Jugcraft biomes whose trees include
# it, and how often (tools/biomes.py "picks").
TREES = {
    "pear": {"display": "Pear", "seed": "pear_seeds", "seed_display": "Pear Seeds", "food": [4, 0.3], "pick": [1, 3],
             "trunk": {"base_height": 5, "height_rand_a": 1}, "foliage": {"radius": 2, "height": 4},
             "biomes": ["IS_FOREST", "IS_BIRCH_FOREST"], "rarity": 16, "regions": {"orchard": 0.2}},
    "peach": {"display": "Peach", "seed": "peach_pit", "seed_display": "Peach Pit", "food": [4, 0.3], "pick": [1, 3],
              "trunk": {"base_height": 3, "height_rand_a": 1}, "foliage": {"radius": 3, "height": 2},
              "biomes": ["IS_PLAINS", "IS_SAVANNA"], "rarity": 16, "regions": {"orchard": 0.2}},
    "lemon": {"display": "Lemon", "seed": "lemon_seeds", "seed_display": "Lemon Seeds", "food": [2, 0.1], "pick": [1, 3],
              "trunk": {"base_height": 3, "height_rand_a": 1}, "foliage": {"radius": 2, "height": 3},
              "biomes": ["IS_SAVANNA"], "rarity": 16, "regions": {"mediterranean_forest": 0.1, "subtropics": 0.15}},
    "orange": {"display": "Orange", "seed": "orange_seeds", "seed_display": "Orange Seeds", "food": [4, 0.3], "pick": [1, 3],
               "trunk": {"base_height": 4, "height_rand_a": 1}, "foliage": {"radius": 2, "height": 3},
               "biomes": ["IS_SAVANNA", "IS_JUNGLE"], "rarity": 16, "regions": {"mediterranean_forest": 0.1, "subtropics": 0.15}},
    # The fruit crops (tools/fruit_crops.py, 8 October 2026): the plum, an oak-trunked tree as the others; and the banana,
    # a soft green-brown stem of its own (`log`, BANANA_STEM below) under a crown of long fronds that droop and hang
    # (vanilla's cherry foliage placer, `foliage` "type"), its fruit hanging in bunches among them.
    "plum": {"display": "Plum", "seed": "plum_pit", "seed_display": "Plum Pit", "food": [4, 0.3], "pick": [1, 3],
             "trunk": {"base_height": 4, "height_rand_a": 1}, "foliage": {"radius": 2, "height": 3},
             "biomes": ["IS_FOREST", "IS_TAIGA"], "rarity": 16, "regions": {"orchard": 0.15}},
    "banana": {"display": "Banana", "seed": "banana_pup", "seed_display": "Banana Pup", "food": [4, 0.4], "pick": [2, 4],
               "log": "jugcraft:banana_stem", "trunk": {"base_height": 4, "height_rand_a": 2},
               "foliage": {"type": "cherry", "radius": 3, "height": 4, "wide_bottom_layer_hole_chance": 0.25,
                           "corner_hole_chance": 0.4, "hanging_leaves_chance": 0.7, "hanging_leaves_extension_chance": 0.3},
               "biomes": ["IS_JUNGLE"], "rarity": 12, "regions": {"tropics": 0.15, "rainforest": 0.1}},
}
# The banana's stem: a log of its own (a pillar, in #minecraft:logs so its fronds count it as their tree, as the TREES.md
# roster asks; it does not burn as fuel).
BANANA_STEM = {"block": "banana_stem", "display": "Banana Stem"}
LEAF_STAGES = ["", "_blossom", "_ripe"]


def sapling(tree):
    return f"{tree}_sapling"


def leaves(tree):
    return f"{tree}_leaves"


def feature(tree):
    """The tree's configured feature (data/jugcraft/worldgen/feature/<tree>_tree.json), grown by its sapling."""
    return f"{tree}_tree"


def checked(tree):
    """The placed feature a biome's tree list names: the tree, where its sapling could stand (tools/biomes.py PLACED_TREES)."""
    return f"{tree}_checked"


# The juices: drinks in a glass bottle (drunk on a full stomach for a short effect, leaving the bottle; tools/menu.py "drink"),
# set down standing as the menu's drinks are.
DISHES = {
    "orange_juice": {"display": "Orange Juice", "food": [5, 0.5], "kind": "drink", "effect": ["HEALTH_BOOST", 60], "model": ["stand"]},
    "lemonade": {"display": "Lemonade", "food": [4, 0.4], "kind": "drink", "effect": ["SPEED", 30], "model": ["stand"]},
}

# The pies (tools/agriculture.py PIES "fillings", baked in the Hearth Oven; the PieFilling enum's PEACH and LEMON), each with
# the pastry and sugar every pie takes. The lemon meringue pie is topped with meringue, not a lattice.
PIES = {
    "peach": {"display": "Peach", "food": [4, 0.6], "color": 0xE8904A, "with": ["jugcraft:peach", "jugcraft:peach"]},
    "lemon": {"display": "Lemon", "pie": "lemon_meringue_pie", "pie_display": "Lemon Meringue Pie", "food": [4, 0.6],
              "color": 0xF0D040, "with": ["jugcraft:lemon", "jugcraft:lemon", "minecraft:egg", "minecraft:egg"], "top": "meringue"},
}

# The preserves (tools/agriculture.py PANTRY "preserves"), cooked into a Mason Jar in the Cooking Pot and sealed in the
# Canning Kettle as the others are.
PRESERVES = {
    "orange_marmalade": {"display": "Orange Marmalade", "food": [3, 0.4], "effect": None, "color": 0xE0761A, "kind": "sweet"},
    "peach_preserves": {"display": "Peach Preserves", "food": [3, 0.4], "effect": None, "color": 0xF09A50, "kind": "sweet"},
    "pear_butter": {"display": "Pear Butter", "food": [4, 0.5], "effect": None, "color": 0xB8923E, "kind": "sweet"},
}

# Items for tools/agriculture.py ITEMS: each fruit (a food), each seed (it plants its sapling) and the juices.
ITEMS = {}
for _tree, _info in TREES.items():
    ITEMS[_tree] = {"display": _info["display"], "food": list(_info["food"]), "compost": "medium_high",
                    "tags": ["c:foods", "c:foods/fruit", f"c:crops/{_tree}"]}
    ITEMS[_info["seed"]] = {"display": _info["seed_display"], "plants": sapling(_tree), "compost": "low", "tags": [f"c:seeds/{_tree}"]}
for _name, _info in DISHES.items():
    ITEMS[_name] = {"display": _info["display"], "food": list(_info["food"]), "drink": list(_info["effect"]), "tags": ["c:foods"]}

# Recipes. Cooking Pot (tools/agriculture.py POT_RECIPES): the preserves.
POT_RECIPES = {
    "orange_marmalade": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:orange": 3, "jugcraft:lemon": 1, "minecraft:sugar": 2}, "time": 300},
    "peach_preserves": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:peach": 3, "minecraft:sugar": 2}, "time": 300},
    "pear_butter": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:pear": 4, "minecraft:sugar": 1}, "time": 300},
}
# Crafting: a fruit into its seed; the juices.
SHAPELESS = [{"id": info["seed"], "inputs": [f"jugcraft:{tree}"], "result": info["seed"], "count": 1, "category": "misc"}
             for tree, info in TREES.items()]
SHAPELESS += [
    {"id": "orange_juice", "inputs": ["minecraft:glass_bottle", "jugcraft:orange", "jugcraft:orange"], "result": "orange_juice",
     "count": 1, "category": "misc"},
    {"id": "lemonade", "inputs": ["minecraft:glass_bottle", "jugcraft:lemon", "minecraft:sugar", "minecraft:sugar"], "result": "lemonade",
     "count": 1, "category": "misc"},
]


def placed():
    """The juices that set down, with their models (as tools/menu.py placed())."""
    return {name: info["model"] for name, info in DISHES.items() if "model" in info}


def blocks():
    """Each tree's sapling and leaves, the banana's stem, and the set-down juices."""
    return [b for tree in TREES for b in (sapling(tree), leaves(tree))] + [BANANA_STEM["block"]] + list(placed())


def items():
    """The slice's block items: the leaves and the banana's stem (the saplings are planted from their seeds; the fruit,
    seeds and juices join tools/agriculture.py ITEMS)."""
    return [leaves(tree) for tree in TREES] + [BANANA_STEM["block"]]


def log(tree):
    """The block a tree's trunk is made of."""
    return TREES[tree].get("log", "minecraft:oak_log")


def itemless():
    """The saplings (planted from their seeds, as the apple's) and the set-down juices: blocks with no item of their own."""
    return [sapling(tree) for tree in TREES] + list(placed())
