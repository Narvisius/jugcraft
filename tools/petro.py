"""Petrochemistry (the dieselpunk oil line of the Chemistry branch): single source of truth.

See docs/branches/CHEMISTRY.md for the plan. generate_material_data.py writes the fluid blocks, buckets and names
from here, petro_textures.py draws them, and check_mod_data.py keeps the Java registration in sync.
Amounts are millibuckets (mB); 1 bucket = 1,000 mB.
"""

# Fluids: display name, colours (dark, mid, light, sheen) for the textures, and how they flow.
# tick_delay: ticks between spreads (water 5, lava 30); slope: how far it looks for a way down (water 4);
# drop_off: level lost per block (water 1, lava 2). None of them makes new source blocks.
FLUIDS = {
    "crude_oil": {"display": "Crude Oil", "feature": "crude_oil",
                  "colors": [(14, 11, 9), (30, 23, 17), (48, 38, 28), (70, 62, 84)],
                  "tick_delay": 20, "slope": 2, "drop_off": 2},
}


def fluid_blocks():
    return list(FLUIDS)


def buckets():
    return [f"{fluid}_bucket" for fluid in FLUIDS]


def petro_items():
    """Items of the petrochemistry line that are not blocks."""
    return buckets()


# Fluid processing machines (MachineKind.fluidSpec() in Java mirrors this): input and output tank capacities in mB,
# item input and output slots, and their recipe type (data/jugcraft/recipe/<type>/).
FLUID_MACHINES = {
    # Pumps the conventional reservoir under its chunk (no recipes): 2 mB a tick at 32 JE/t.
    "pumpjack": {"inputs": [], "outputs": [16_000], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Hot-water extraction: oil sand or bitumen + water -> crude oil (+ sand). 32 JE/t.
    "oil_sand_extractor": {"inputs": [8_000], "outputs": [8_000], "item_inputs": 1, "item_outputs": 1,
                           "recipe_type": "oil_sand_extraction"},
}

# Fluid recipes per machine. Each: name, item ingredients [(item or #tag, count)], fluids in [(fluid, mB)],
# fluids out [(fluid, mB)], item results [(item, count)], ticks, feature switches.
# "source": mB of fluid the recipe releases from its items (the audit allows that much more fluid out than in).
FLUID_RECIPES = {
    "oil_sand_extractor": [
        # A whole oil sand block (silk touch) gives the most: two buckets of crude oil from four blocks.
        {"name": "oil_sand", "items": [("jugcraft:oil_sand", 1)], "fluids": [("minecraft:water", 250)],
         "fluid_results": [("jugcraft:crude_oil", 500)], "results": [("minecraft:sand", 1)], "source": 500,
         "ticks": 160, "features": ["crude_oil"]},
        # Bitumen (what oil sand drops, or crushes into, three to a block) gives less per block.
        {"name": "bitumen", "items": [("jugcraft:bitumen", 1)], "fluids": [("minecraft:water", 100)],
         "fluid_results": [("jugcraft:crude_oil", 150)], "source": 150, "ticks": 80, "features": ["crude_oil"]},
    ],
}


def fluid_recipe_files(condition):
    """(recipe type, file name, JSON) for every fluid recipe."""
    for machine, recipes in FLUID_RECIPES.items():
        kind = FLUID_MACHINES[machine]["recipe_type"]
        for recipe in recipes:
            data = {"fabric:load_conditions": [c for f in recipe["features"] for c in condition(f)],
                    "type": f"jugcraft:{kind}"}
            if recipe.get("items"):
                data["items"] = [{"ingredient": item, "count": count} for item, count in recipe["items"]]
            if recipe.get("fluids"):
                data["fluids"] = [{"fluid": fluid, "amount": mb} for fluid, mb in recipe["fluids"]]
            if recipe.get("fluid_results"):
                data["fluid_results"] = [{"fluid": fluid, "amount": mb} for fluid, mb in recipe["fluid_results"]]
            if recipe.get("results"):
                data["results"] = [{"id": item, "count": count} for item, count in recipe["results"]]
            data["time"] = recipe["ticks"]
            yield kind, recipe["name"], data
