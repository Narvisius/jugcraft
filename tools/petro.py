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
    # Distillation fractions (batch 2).
    "naphtha": {"display": "Naphtha", "feature": "crude_oil",
                "colors": [(150, 130, 70), (190, 170, 100), (220, 205, 140), (244, 236, 196)],
                "tick_delay": 5, "slope": 4, "drop_off": 1},
    "diesel": {"display": "Diesel", "feature": "crude_oil",
               "colors": [(120, 70, 10), (170, 110, 25), (210, 150, 50), (242, 204, 112)],
               "tick_delay": 8, "slope": 3, "drop_off": 1},
    "heavy_fuel_oil": {"display": "Heavy Fuel Oil", "feature": "crude_oil",
                       "colors": [(20, 16, 10), (38, 30, 18), (58, 46, 28), (96, 84, 62)],
                       "tick_delay": 30, "slope": 2, "drop_off": 2},
    "lubricant": {"display": "Lubricant", "feature": "crude_oil",
                  "colors": [(90, 80, 20), (140, 125, 40), (180, 165, 70), (222, 212, 134)],
                  "tick_delay": 25, "slope": 2, "drop_off": 2},
    "gasoline": {"display": "Gasoline", "feature": "crude_oil",
                 "colors": [(150, 60, 40), (200, 100, 70), (230, 150, 110), (250, 212, 184)],
                 "tick_delay": 4, "slope": 4, "drop_off": 1},
    # Batch 3: fracking.
    "fracking_fluid": {"display": "Fracking Fluid", "feature": "crude_oil",
                       "colors": [(90, 110, 120), (130, 150, 160), (170, 185, 190), (212, 222, 226)],
                       "tick_delay": 6, "slope": 3, "drop_off": 1},
    "flowback_water": {"display": "Flowback Water", "feature": "crude_oil",
                       "colors": [(70, 64, 50), (100, 92, 72), (130, 120, 96), (172, 162, 132)],
                       "tick_delay": 5, "slope": 4, "drop_off": 1},
}

# Gases: fluids that only live in tanks and pipes (no block, no bucket). Gauge colour in Java (PetroFluids.gas).
GASES = {
    "refinery_gas": {"display": "Refinery Gas", "feature": "crude_oil"},
}


# Plain items (chemistry/PetroItems.java): display name.
ITEMS = {
    # Bauxite (alumina) and sand (silica) with a little nickel: used up, one per bucket of heavy fuel oil cracked.
    "cracking_catalyst": "Cracking Catalyst",
    # The residue of vacuum distillation; asphalt roads come in batch 4.
    "asphalt_binder": "Asphalt Binder",
}


def fluid_ids():
    """Every fluid id this line registers (sources, flowing forms and gases), for tags and recipe checks."""
    return [f for fluid in FLUIDS for f in (fluid, f"flowing_{fluid}")] + list(GASES)


def fluid_blocks():
    return list(FLUIDS)


def buckets():
    return [f"{fluid}_bucket" for fluid in FLUIDS]


def petro_items():
    """Items of the petrochemistry line that are not blocks."""
    return buckets() + list(ITEMS)


# Fluid processing machines (MachineKind.fluidSpec() in Java mirrors this): input and output tank capacities in mB,
# item input and output slots, and their recipe type (data/jugcraft/recipe/<type>/).
FLUID_MACHINES = {
    # Pumps the conventional reservoir under its chunk (no recipes): 2 mB a tick at 32 JE/t.
    "pumpjack": {"inputs": [], "outputs": [16_000], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Hot-water extraction: oil sand or bitumen + water -> crude oil (+ sand). 32 JE/t.
    "oil_sand_extractor": {"inputs": [8_000], "outputs": [8_000], "item_inputs": 1, "item_outputs": 1,
                           "recipe_type": "oil_sand_extraction"},
    # Crude oil -> four fractions, each drawn off at its own height (Java: MachineKind.outputLayer). 128 JE/t.
    "distillation_tower": {"inputs": [16_000], "outputs": [8_000, 8_000, 8_000, 8_000], "item_inputs": 0,
                           "item_outputs": 0, "recipe_type": "distillation"},
    # Heavy fuel oil + water (steam) + catalyst -> diesel (base), naphtha (layer 2), refinery gas (top). 160 JE/t.
    "catalytic_cracker": {"inputs": [8_000, 8_000], "outputs": [8_000, 8_000, 8_000], "item_inputs": 1,
                          "item_outputs": 0, "recipe_type": "catalytic_cracking"},
    # Heavy fuel oil -> lubricant + asphalt binder. 96 JE/t.
    "vacuum_distillation_unit": {"inputs": [8_000], "outputs": [8_000], "item_inputs": 0, "item_outputs": 1,
                                 "recipe_type": "vacuum_distillation"},
    # Naphtha -> gasoline (base) + refinery gas (top). 120 JE/t.
    "catalytic_reformer": {"inputs": [8_000], "outputs": [8_000, 8_000], "item_inputs": 0, "item_outputs": 0,
                           "recipe_type": "reforming"},
    # Over shale (no recipes): 4 mB/t fracking fluid down; 8 mB/t freed (6 crude, 2 gas) and 3 mB/t flowback up.
    # 256 JE/t. Draw-offs: crude at the base, flowback one block up, gas at the top (MachineKind.outputLayer).
    "fracking_rig": {"inputs": [16_000], "outputs": [16_000, 8_000, 16_000], "item_inputs": 0, "item_outputs": 0,
                     "recipe_type": None},
    # Flowback water -> clean water + salt. 48 JE/t.
    "flowback_treatment_unit": {"inputs": [8_000], "outputs": [8_000], "item_inputs": 0, "item_outputs": 1,
                                "recipe_type": "water_treatment"},
    # Water + two powders/solids -> a mixture. 64 JE/t.
    "chemical_mixer": {"inputs": [8_000], "outputs": [8_000], "item_inputs": 2, "item_outputs": 0,
                       "recipe_type": "chemical_mixing"},
    # Burns diesel (256 JE/mB) or heavy fuel oil (128 JE/mB) from its tank at 256 JE/t (FLUID_FUELS).
    "diesel_generator": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Burns gasoline (384 JE/mB) or refinery gas (192 JE/mB) at 512 JE/t; the second tank takes lubricant,
    # 1 mB every 20 ticks of running (FluidFuels.LUBRICANT_TICKS), and it will not run without it.
    "gas_turbine": {"inputs": [16_000, 4_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
}

# JE per mB each fluid-burning generator gets from each fuel (Java: chemistry/FluidFuels).
FLUID_FUELS = {
    "diesel_generator": {"diesel": 256, "heavy_fuel_oil": 128},
    "gas_turbine": {"gasoline": 384, "refinery_gas": 192},
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
    # One bucket of crude oil splits into fractions that add up to one bucket, in the tower's output tank order.
    "distillation_tower": [
        {"name": "crude_oil", "fluids": [("jugcraft:crude_oil", 1000)],
         "fluid_results": [("jugcraft:refinery_gas", 100), ("jugcraft:naphtha", 250), ("jugcraft:diesel", 400),
                           ("jugcraft:heavy_fuel_oil", 250)], "ticks": 100, "features": ["crude_oil"]},
    ],
    # Cracking breaks heavy oil into lighter fuels; the steam's water is not counted as product.
    "catalytic_cracker": [
        {"name": "heavy_fuel_oil", "items": [("jugcraft:cracking_catalyst", 1)],
         "fluids": [("jugcraft:heavy_fuel_oil", 1000), ("minecraft:water", 250)],
         "fluid_results": [("jugcraft:diesel", 500), ("jugcraft:naphtha", 300), ("jugcraft:refinery_gas", 200)],
         "source": 0, "ticks": 160, "features": ["crude_oil"]},
    ],
    # The heaviest part of heavy fuel oil, boiled under vacuum: lubricant, and a residue of asphalt binder.
    "vacuum_distillation_unit": [
        {"name": "heavy_fuel_oil", "fluids": [("jugcraft:heavy_fuel_oil", 1000)],
         "fluid_results": [("jugcraft:lubricant", 400)], "results": [("jugcraft:asphalt_binder", 2)], "ticks": 120,
         "features": ["crude_oil"]},
    ],
    # Reforming rearranges naphtha into high-octane gasoline, giving off a little gas.
    # Fracking fluid: water carrying sand (to prop the cracks open) and a gelling agent (dried kelp, standing in for
    # guar gum) to carry the sand.
    "chemical_mixer": [
        {"name": "fracking_fluid", "items": [("minecraft:sand", 2), ("minecraft:dried_kelp", 1)],
         "fluids": [("minecraft:water", 1000)], "fluid_results": [("jugcraft:fracking_fluid", 1000)], "source": 0,
         "ticks": 80, "features": ["crude_oil"]},
    ],
    # Flowback water settles and is filtered: most of it comes back as clean water; the brine leaves salt. A quarter
    # is lost (sludge), so fracking water is never free.
    "flowback_treatment_unit": [
        {"name": "flowback_water", "fluids": [("jugcraft:flowback_water", 1000)],
         "fluid_results": [("minecraft:water", 750)], "results": [("jugcraft:salt", 1)], "ticks": 80,
         "features": ["crude_oil"]},
    ],
    "catalytic_reformer": [
        {"name": "naphtha", "fluids": [("jugcraft:naphtha", 1000)],
         "fluid_results": [("jugcraft:gasoline", 900), ("jugcraft:refinery_gas", 100)], "ticks": 120,
         "features": ["crude_oil"]},
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
