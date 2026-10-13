"""Garden crops, herbs and spices, part b (slice 7 of the kitchen and cooking expansion, docs/branches/AGRICULTURE.md; the
record: docs/features/vegetables-herbs-and-spices.md): the kitchen herbs, drawn in Jugcraft's own style. The numbers here are
what agriculture/TallCrop, PlanterBoxBlock, HerbBundleBlock and JugcraftAgriculture use; tools/check_mod_data.py compares
them. tools/herb_data.py writes the JSON; tools/herb_textures.py draws them.

- **Eight herbs:** basil, mint, rosemary, thyme, parsley, sage, dill and chives. Each is planted from a sprig of itself, on
  farmland or in a Planter Box, and grows a block tall in four stages (tools/agriculture.py TALL_CROPS, agriculture/TallCrop,
  as the pepper). Grown, a right-click cuts a few sprigs and it grows back from its cut-back stage, as herbs do.
- **Pots:** a sprig used on a flower pot plants the herb in it, grown (vanilla's flower pot, POTTED), for a windowsill.
- **Planter Box:** a wooden box of soil that holds its own water: crops take it as moist farmland wherever it stands, on a
  balcony or indoors, and it is never trampled. It takes the place of farmland and a water source, no more.
- **Drying:** four sprigs tied with string make a herb bundle, hung under a block (a beam, a ceiling). It dries in time
  (a 1 in `dry_chance` chance on each random tick, about six minutes on average) into its dried look; taken down dried, it
  gives `dried` Dried Herbs. Taken down fresh, it gives the bundle back.
- **Dishes:** herbs season pesto pasta, sage and onion stuffing, herb-roasted mutton, garden herb soup and mint tea; Dried
  Herbs season herb-roasted potatoes; dill (with the vegetables' cucumbers and the spices' mustard seed) pickles cucumbers.

Balance (docs/BALANCE.md, tools/menu.py COOK_BONUS): herbs are not eaten alone; a dish gives at most COOK_BONUS more than its
ingredients (tools/check_mod_data.py checks). A bundle is four sprigs and gives four Dried Herbs: drying changes the herbs,
not how many.
"""
HERBS = {
    "basil": {"display": "Basil", "biomes": ["IS_JUNGLE", "IS_SAVANNA"]},
    "mint": {"display": "Mint", "biomes": ["IS_RIVER", "IS_SWAMP"]},
    "rosemary": {"display": "Rosemary", "biomes": ["IS_HILL", "IS_SAVANNA"]},
    "thyme": {"display": "Thyme", "biomes": ["IS_HILL", "IS_PLAINS"]},
    "parsley": {"display": "Parsley", "biomes": ["IS_PLAINS", "IS_FOREST"]},
    "sage": {"display": "Sage", "biomes": ["IS_SAVANNA", "IS_PLAINS"]},
    "dill": {"display": "Dill", "biomes": ["IS_PLAINS", "IS_FLORAL"]},
    "chives": {"display": "Chives", "biomes": ["IS_FOREST", "IS_TAIGA"]},
}
STAGES = 4


def crop(herb):
    return f"{herb}_crop"


def stage_texture(herb, stage):
    return f"{herb}_stage{stage}"


def bundle(herb):
    return f"{herb}_bundle"


def potted(herb):
    return f"potted_{herb}"


def wild(herb):
    return f"wild_{herb}"


# The herbs as tools/agriculture.py TALL_CROPS entries (after the vegetables'): a block tall, sprouting (ages 0-1), young
# (2-3), bushy (4-5) and grown (6-7); grown, `pick` sprigs are cut and the herb goes back to age 4. Planted from a sprig.
TALL_CROPS = {
    herb: {"block": crop(herb), "display": info["display"], "seed": herb, "heights": [1] * 8,
           "pick": {"item": herb, "min": 1, "max": 3}, "pick_reset": 4, "growth_time": 1.0,
           "textures": [[stage_texture(herb, s)] for s in (0, 0, 1, 1, 2, 2, 3, 3)]}
    for herb, info in HERBS.items()
}
# Wild herbs, in patches on grass (tools/agriculture.py WILD_CROPS), wearing their grown stage; broken, a sprig or two.
WILD = {wild(herb): {"display": f"Wild {info['display']}", "crop": herb, "texture": stage_texture(herb, 3), "biomes": info["biomes"]}
        for herb, info in HERBS.items()}

PLANTER = {"block": "planter_box", "display": "Planter Box", "textures": {"side": "planter_box_side", "top": "planter_box_top",
                                                                          "bottom": "planter_box_bottom"}}
BUNDLE = {"dry_chance": 5, "sprigs": 4, "dried": "dried_herbs", "dried_display": "Dried Herbs"}

ITEMS = {herb: {"display": info["display"], "plants": crop(herb), "compost": "low", "tags": [f"c:crops/{herb}", "jugcraft:herbs"]}
         for herb, info in HERBS.items()}
ITEMS.update({
    BUNDLE["dried"]: {"display": BUNDLE["dried_display"], "compost": "low", "tags": ["jugcraft:spices"]},
    "pesto_pasta": {"display": "Pesto Pasta", "food": [6, 0.7], "stew": True, "tags": ["c:foods"]},
    "sage_and_onion_stuffing": {"display": "Sage and Onion Stuffing", "food": [7, 0.6], "stew": True, "tags": ["c:foods"]},
    "herb_roasted_mutton": {"display": "Herb-Roasted Mutton", "food": [10, 0.8], "stew": True, "tags": ["c:foods"]},
    "garden_herb_soup": {"display": "Garden Herb Soup", "food": [6, 0.6], "stew": True, "tags": ["c:foods/soup"]},
    "herb_roasted_potatoes": {"display": "Herb-Roasted Potatoes", "food": [12, 0.8], "stew": True, "tags": ["c:foods"]},
    "mint_tea": {"display": "Mint Tea", "food": [2, 0.3], "drink": ["SPEED", 30], "tags": ["c:drinks"]},
})
# The bundles and the planter box are blocks with items of their own.
BUNDLES = {bundle(herb): f"{info['display']} Bundle" for herb, info in HERBS.items()}

SHAPED = [{"id": PLANTER["block"], "pattern": ["PWP", "PDP", "PPP"],
           "key": {"P": "#minecraft:planks", "W": "minecraft:water_bucket", "D": "minecraft:dirt"},
           "result": PLANTER["block"], "count": 1, "category": "building"}]
SHAPELESS = [{"id": bundle(herb), "inputs": [f"jugcraft:{herb}"] * BUNDLE["sprigs"] + ["minecraft:string"], "result": bundle(herb),
              "count": 1, "category": "building"} for herb in HERBS]
# Cooking Pot dishes (tools/agriculture.py POT_RECIPES). Mint tea is brewed in a glass bottle, which it leaves when drunk.
POT_RECIPES = {
    "pesto_pasta": {"inputs": {"minecraft:bowl": 1, "jugcraft:raw_pasta": 1, "jugcraft:basil": 2, "jugcraft:garlic": 1,
                                "jugcraft:tomato": 1}, "time": 200},
    "sage_and_onion_stuffing": {"inputs": {"minecraft:bowl": 1, "minecraft:bread": 1, "jugcraft:onion": 1, "jugcraft:sage": 1},
                                "time": 200},
    "herb_roasted_mutton": {"inputs": {"minecraft:bowl": 1, "minecraft:mutton": 1, "minecraft:potato": 1, "jugcraft:rosemary": 1,
                                       "jugcraft:thyme": 1}, "time": 300},
    "garden_herb_soup": {"inputs": {"minecraft:bowl": 1, "minecraft:carrot": 1, "minecraft:potato": 1, "jugcraft:parsley": 1,
                                    "jugcraft:chives": 1, "jugcraft:dill": 1}, "time": 200},
    "herb_roasted_potatoes": {"inputs": {"minecraft:bowl": 1, "minecraft:baked_potato": 2, f"jugcraft:{BUNDLE['dried']}": 1},
                              "time": 200},
    "mint_tea": {"inputs": {"minecraft:glass_bottle": 1, "jugcraft:mint": 2, "minecraft:sugar": 1}, "time": 200},
}
# Dill pickles (tools/agriculture.py PANTRY "preserves"): cucumbers with dill and mustard seed in cider vinegar.
PRESERVES = {"dill_pickles": {"display": "Dill Pickles", "food": [2, 0.4], "effect": None, "color": 0x5A8A2A, "kind": "pickle"}}
PRESERVE_RECIPES = {
    "dill_pickles": {"inputs": {"jugcraft:mason_jar": 1, "jugcraft:cucumber": 4, "jugcraft:dill": 1, "jugcraft:mustard_seeds": 1,
                                "jugcraft:cider_vinegar": 1}, "time": 300},
}


def blocks():
    """The potted herbs, bundles and planter box (the herbs and their wild plants are among the tall crops and wild plants)."""
    return [potted(herb) for herb in HERBS] + list(BUNDLES) + [PLANTER["block"]]


def items():
    return list(BUNDLES) + [PLANTER["block"]]


def itemless():
    """The potted herbs (a sprig plants one, as vanilla's potted plants have no item)."""
    return [potted(herb) for herb in HERBS]


def textures():
    """The bundles' and planter box's block textures (the herbs' stages are the tall crops')."""
    return [f"{bundle(herb)}{suffix}" for herb in HERBS for suffix in ("", "_dried")] + list(PLANTER["textures"].values())
