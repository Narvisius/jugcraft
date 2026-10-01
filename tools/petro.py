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
    # Industrial chemistry (batch 5): salt brine for the electrolytic cell, and the lye it makes.
    "brine": {"display": "Brine", "feature": "salt",
              "colors": [(110, 140, 160), (150, 178, 194), (190, 210, 220), (232, 240, 244)],
              "tick_delay": 5, "slope": 4, "drop_off": 1},
    "lye": {"display": "Lye", "feature": "salt",
            "colors": [(170, 170, 140), (200, 200, 170), (224, 224, 198), (246, 246, 228)],
            "tick_delay": 6, "slope": 4, "drop_off": 1},
    "sulfuric_acid": {"display": "Sulfuric Acid", "feature": "sulfur",
                      "colors": [(150, 140, 40), (190, 180, 70), (214, 206, 104), (240, 236, 170)],
                      "tick_delay": 6, "slope": 4, "drop_off": 1},
    # Nitrogen chemistry (batch 12): the Ostwald process's acid, a fuming pale yellow.
    "nitric_acid": {"display": "Nitric Acid", "feature": "machines",
                    "colors": [(170, 150, 70), (204, 186, 104), (226, 212, 140), (246, 238, 196)],
                    "tick_delay": 5, "slope": 4, "drop_off": 1},
}

# Gases: fluids that only live in tanks and pipes (no block, no bucket). Gauge colour in Java (PetroFluids.gas).
GASES = {
    # colors: the swirl drawn for recipe viewers and tank gauges (gases are never placed in the world).
    "refinery_gas": {"display": "Refinery Gas", "feature": "crude_oil",
                     "colors": [(150, 160, 172), (176, 188, 200), (200, 210, 220), (226, 232, 238)]},
    # From brine electrolysis (batch 5).
    "chlorine": {"display": "Chlorine", "feature": "salt",
                 "colors": [(130, 160, 60), (160, 190, 80), (186, 214, 104), (214, 236, 150)]},
    "hydrogen": {"display": "Hydrogen", "feature": "salt",
                 "colors": [(190, 200, 214), (210, 220, 232), (228, 236, 244), (246, 250, 254)]},
    # Nitrogen chemistry (batch 12): air separated into nitrogen and oxygen; ammonia from Haber-Bosch.
    "nitrogen": {"display": "Nitrogen", "feature": "machines",
                 "colors": [(120, 140, 190), (150, 168, 214), (182, 196, 232), (214, 224, 246)]},
    "oxygen": {"display": "Oxygen", "feature": "machines",
               "colors": [(90, 150, 200), (120, 180, 224), (160, 206, 238), (204, 232, 250)]},
    "ammonia": {"display": "Ammonia", "feature": "machines",
                "colors": [(150, 120, 190), (176, 150, 212), (202, 182, 230), (228, 216, 244)]},
}


# Plain items (chemistry/PetroItems.java): display name.
ITEMS = {
    # Bauxite (alumina) and sand (silica) with a little nickel: used up, one per bucket of heavy fuel oil cracked.
    "cracking_catalyst": "Cracking Catalyst",
    # The residue of vacuum distillation; asphalt roads come in batch 4.
    "asphalt_binder": "Asphalt Binder",
    # Polymerized refinery gas; the metal press flattens each into a plastic sheet.
    "plastic_pellets": "Plastic Pellets",
    "plastic_sheet": "Plastic Sheet",
    # Bayer-process alumina (batch 5): the electrolytic cell smelts it into aluminum.
    "alumina": "Alumina",
    # Superphosphate fertilizer (batch 5): ripens crops in a 5x5 area (chemistry/FertilizerItem).
    "fertilizer": "Fertilizer",
    # The Kroll process (batch 6): the arc furnace melts the sponge into titanium ingots.
    "titanium_sponge": "Titanium Sponge",
    # Lithium cells (batch 6): crafted from lithium carbonate, built into the lithium battery bank.
    "lithium_cell": "Lithium Cell",
    # Neodymium magnets (batch 6): the alloy smelter makes them; the magnet dynamo and motor use them.
    "neodymium_magnet": "Neodymium Magnet",
    # Electronics (batch 7): the crystal grower pulls doped silicon boules; the sawmill cuts them into wafers.
    "silicon_boule": "Silicon Boule",
    "silicon_wafer": "Silicon Wafer",
    # Etched in the lithography station (batch 7).
    "microchip": "Microchip",
}


# Blocks (chemistry/PetroBlocks.java): asphalt road, walked on at 1.3x speed. shape: cube, slab or line (a cube
# with a yellow centre line on top that turns to face the player placing it).
BLOCKS = {
    "asphalt": {"display": "Asphalt", "shape": "cube"},
    "asphalt_slab": {"display": "Asphalt Slab", "shape": "slab"},
    "asphalt_road_line": {"display": "Asphalt Road Line", "shape": "line"},
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


def petro_blocks():
    """Blocks of the petrochemistry line with block items (not fluids)."""
    return list(BLOCKS)


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
    # Burns diesel or heavy fuel oil (FLUID_FUELS, KE per mB) to turn a shaft at up to 512 KE/t.
    "diesel_engine": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Brine -> chlorine (top), hydrogen (middle) and lye (base); alumina + coke -> aluminum (batch 5). 256 JE/t.
    "electrolytic_cell": {"inputs": [8_000], "outputs": [8_000, 8_000, 8_000], "item_inputs": 2, "item_outputs": 1,
                          "recipe_type": "electrolysis"},
    # Burns hydrogen (128 JE/mB) at 128 JE/t; one block, electric look (batch 5).
    "fuel_cell": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Burns gasoline (448 KE/mB) or diesel (320) to turn a shaft at up to 1,024 KE/t (batch 10).
    "advanced_engine": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Sulfur + water -> sulfuric acid; bauxite + lye -> alumina; phosphate + acid -> fertilizer (batch 5). 96 JE/t.
    "chemical_reactor": {"inputs": [8_000], "outputs": [8_000], "item_inputs": 2, "item_outputs": 1,
                         "recipe_type": "chemical_reaction"},
    # Electronics (batch 7): a wafer and copper wire etched with sulfuric acid into microchips. 192 JE/t.
    "lithography_station": {"inputs": [4_000], "outputs": [], "item_inputs": 2, "item_outputs": 1,
                            "recipe_type": "lithography"},
    # Nitrogen chemistry (batch 12). Air separation (no recipes): from the air alone, 8 mB/t nitrogen drawn off the
    # top and 2 mB/t oxygen off the base (Java: MachineKind.outputLayer) at 64 JE/t, like the pumpjack's oil.
    "air_separation_unit": {"inputs": [], "outputs": [16_000, 16_000], "item_inputs": 0, "item_outputs": 0,
                            "recipe_type": None},
    # A high-pressure catalytic converter: hydrogen + nitrogen -> ammonia (Haber-Bosch); ammonia + oxygen + water
    # -> nitric acid (Ostwald). 128 JE/t.
    "synthesis_converter": {"inputs": [8_000, 8_000, 8_000], "outputs": [8_000], "item_inputs": 0,
                            "item_outputs": 0, "recipe_type": "gas_synthesis"},
    # Refinery gas -> plastic pellets. 96 JE/t.
    "polymerization_reactor": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 1,
                               "recipe_type": "polymerization"},
}

# JE per mB each fluid-burning generator gets from each fuel (Java: chemistry/FluidFuels).
FLUID_FUELS = {
    "diesel_generator": {"diesel": 256, "heavy_fuel_oil": 128},
    "gas_turbine": {"gasoline": 384, "refinery_gas": 192},
    "diesel_engine": {"diesel": 256, "heavy_fuel_oil": 128},
    "fuel_cell": {"hydrogen": 128},
    "advanced_engine": {"gasoline": 448, "diesel": 320},
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
    # Refinery gas polymerizes into plastic: a bucket of gas gives four pellets.
    "polymerization_reactor": [
        {"name": "refinery_gas", "fluids": [("jugcraft:refinery_gas", 1000)], "fluid_results": [],
         "results": [("jugcraft:plastic_pellets", 4)], "source": 0, "ticks": 100, "features": ["crude_oil"]},
    ],
    # Fracking fluid: water carrying sand (to prop the cracks open) and a gelling agent (dried kelp, standing in for
    # guar gum) to carry the sand.
    "chemical_mixer": [
        {"name": "fracking_fluid", "items": [("minecraft:sand", 2), ("minecraft:dried_kelp", 1)],
         "fluids": [("minecraft:water", 1000)], "fluid_results": [("jugcraft:fracking_fluid", 1000)], "source": 0,
         "ticks": 80, "features": ["crude_oil"]},
        # Brine for the electrolytic cell: two salt dissolved in a bucket of water.
        {"name": "brine", "items": [("jugcraft:salt", 2)], "fluids": [("minecraft:water", 1000)],
         "fluid_results": [("jugcraft:brine", 1000)], "source": 0, "ticks": 60, "features": ["salt"]},
    ],
    # The chlor-alkali process: a bucket of brine splits into chlorine at the anode, hydrogen at the cathode and lye
    # left in the cell. Electricity-hungry: 256 JE/t for 200 ticks.
    "electrolytic_cell": [
        {"name": "brine", "fluids": [("jugcraft:brine", 1000)],
         "fluid_results": [("jugcraft:chlorine", 250), ("jugcraft:hydrogen", 250), ("jugcraft:lye", 500)],
         "ticks": 200, "features": ["salt"]},
        # The Hall-Heroult process: alumina dissolved in molten salt and split with a coke anode, which burns away.
        {"name": "aluminum", "items": [("jugcraft:alumina", 2), ("jugcraft:coke", 1)],
         "results": [("jugcraft:aluminum_ingot", 2)], "ticks": 160, "features": ["aluminum"]},
    ],
    # Sulfur burnt to sulfur trioxide and absorbed in water (the contact process, simplified): two sulfur dust and a
    # bucket of water make a bucket of sulfuric acid.
    "chemical_reactor": [
        {"name": "sulfuric_acid", "items": [("jugcraft:sulfur_dust", 2)], "fluids": [("minecraft:water", 1000)],
         "fluid_results": [("jugcraft:sulfuric_acid", 1000)], "source": 0, "ticks": 100, "features": ["sulfur"]},
        # The Bayer process: bauxite digested in hot lye leaves alumina (each bauxite holds two ingots' worth).
        {"name": "alumina", "items": [("jugcraft:bauxite", 1)], "fluids": [("jugcraft:lye", 250)],
         "results": [("jugcraft:alumina", 2)], "ticks": 120, "features": ["aluminum", "salt"]},
        # Superphosphate: phosphate rock treated with sulfuric acid becomes a soluble fertilizer.
        {"name": "fertilizer", "items": [("jugcraft:phosphate", 2)], "fluids": [("jugcraft:sulfuric_acid", 250)],
         "results": [("jugcraft:fertilizer", 4)], "ticks": 80, "features": ["phosphate", "sulfur"]},
        # Ammonium phosphate (batch 12): phosphate rock with ammonia, a richer fertilizer than superphosphate.
        {"name": "ammonium_phosphate", "items": [("jugcraft:phosphate", 2)], "fluids": [("jugcraft:ammonia", 250)],
         "results": [("jugcraft:fertilizer", 6)], "ticks": 80, "features": ["phosphate", "salt", "machines"]},
        # The Kroll process, in one step: rutile chlorinated over hot coke to titanium tetrachloride, then reduced to
        # a porous titanium sponge. The chlorine is used up.
        {"name": "titanium_sponge", "items": [("jugcraft:raw_titanium", 1), ("jugcraft:coke", 1)],
         "fluids": [("jugcraft:chlorine", 250)], "results": [("jugcraft:titanium_sponge", 1)], "ticks": 160,
         "features": ["titanium", "salt"]},
        # Acid leaching (batch 6): lithium mica and monazite dissolved in sulfuric acid and precipitated, twice what
        # the blast-furnace stand-ins recover.
        {"name": "lithium_carbonate", "items": [("jugcraft:lepidolite", 1)], "fluids": [("jugcraft:sulfuric_acid", 250)],
         "results": [("jugcraft:lithium_carbonate", 2)], "ticks": 100, "features": ["lithium", "sulfur"]},
        {"name": "rare_earth_oxide", "items": [("jugcraft:monazite", 1)], "fluids": [("jugcraft:sulfuric_acid", 250)],
         "results": [("jugcraft:rare_earth_oxide", 2)], "ticks": 140, "features": ["rare_earths", "sulfur"]},
    ],
    # Photolithography (batch 7): a wafer patterned and etched with sulfuric acid, with copper wire for the bonds.
    "lithography_station": [
        {"name": "microchip", "items": [("jugcraft:silicon_wafer", 1), ("jugcraft:copper_wire", 2)],
         "fluids": [("jugcraft:sulfuric_acid", 100)], "results": [("jugcraft:microchip", 4)], "ticks": 200,
         "features": ["silicon", "sulfur"]},
        # Nitric acid etches as well, and twice as far (batch 12).
        {"name": "microchip_nitric", "items": [("jugcraft:silicon_wafer", 1), ("jugcraft:copper_wire", 2)],
         "fluids": [("jugcraft:nitric_acid", 50)], "results": [("jugcraft:microchip", 4)], "ticks": 200,
         "features": ["silicon", "machines"]},
    ],
    # Nitrogen chemistry (batch 12). Fluid volumes shrink: 400 mB of gas in gives 200 of ammonia, so no recipe
    # makes fluid from nothing.
    "synthesis_converter": [
        # Haber-Bosch: three parts hydrogen to one of nitrogen over an iron catalyst at high pressure.
        {"name": "ammonia", "fluids": [("jugcraft:hydrogen", 300), ("jugcraft:nitrogen", 100)],
         "fluid_results": [("jugcraft:ammonia", 200)], "ticks": 40, "features": ["salt", "machines"]},
        # Ostwald: ammonia burnt over platinum gauze in oxygen, the gases absorbed in water.
        {"name": "nitric_acid", "fluids": [("jugcraft:ammonia", 100), ("jugcraft:oxygen", 200),
                                           ("minecraft:water", 100)],
         "fluid_results": [("jugcraft:nitric_acid", 200)], "ticks": 40, "features": ["salt", "machines"]},
    ],
    # Flowback water settles and is filtered: most of it comes back as clean water; the brine leaves salt. A quarter
    # is lost (sludge), so fracking water is never free.
    "flowback_treatment_unit": [
        {"name": "flowback_water", "fluids": [("jugcraft:flowback_water", 1000)],
         "fluid_results": [("minecraft:water", 750)], "results": [("jugcraft:salt", 1)], "ticks": 80,
         "features": ["crude_oil"]},
    ],
    # Reforming rearranges naphtha into high-octane gasoline, giving off a little gas.
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
