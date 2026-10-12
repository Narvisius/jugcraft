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
    # Flow batteries (batch 17): vanadium leached out of asphalt binder (heavy oil residue is rich in it) into
    # sulfuric acid, a deep blue.
    "vanadium_electrolyte": {"display": "Vanadium Electrolyte", "feature": "machines",
                             "colors": [(30, 40, 110), (44, 62, 150), (70, 92, 186), (130, 150, 224)],
                             "tick_delay": 6, "slope": 4, "drop_off": 1},
    # Batch 26: crops fermented into fuel alcohol, a pale straw colour and thin as gasoline.
    "bioethanol": {"display": "Bioethanol", "feature": "machines",
                   "colors": [(170, 150, 90), (204, 188, 128), (226, 214, 164), (246, 240, 212)],
                   "tick_delay": 4, "slope": 4, "drop_off": 1},
    # Batch 29: refinery upgrades. Hydrotreated diesel, its sulfur taken out with hydrogen: clear and pale gold.
    "premium_diesel": {"display": "Premium Diesel", "feature": "crude_oil",
                       "colors": [(150, 110, 30), (200, 160, 60), (228, 196, 100), (248, 230, 160)],
                       "tick_delay": 8, "slope": 3, "drop_off": 1},
    # Gasoline blended with a tenth of bioethanol: higher octane, so engines run leaner and hotter. Pink-orange.
    "premium_gasoline": {"display": "Premium Gasoline", "feature": "crude_oil",
                         "colors": [(170, 70, 60), (214, 110, 90), (236, 160, 130), (252, 214, 196)],
                         "tick_delay": 4, "slope": 4, "drop_off": 1},
    # Batch 33: hydroponics. Fertilizer dissolved in water, for the hydroponic bay: a cloudy green.
    "nutrient_solution": {"display": "Nutrient Solution", "feature": "machines",
                          "colors": [(40, 90, 50), (70, 130, 70), (110, 170, 100), (170, 214, 150)],
                          "tick_delay": 5, "slope": 4, "drop_off": 1},
    # Batch 43: liquid fuels (tools/rocketry.py). RP-1 rocket kerosene, hydrocracked from heavy fuel oil: water-clear
    # with a straw tint. Liquid oxygen, condensed in the cryogenic liquefier: pale sky blue, runs fast.
    "kerosene": {"display": "RP-1 Kerosene", "feature": "crude_oil",
                 "colors": [(150, 150, 120), (196, 196, 160), (224, 224, 196), (246, 246, 228)],
                 "tick_delay": 5, "slope": 4, "drop_off": 1},
    "liquid_oxygen": {"display": "Liquid Oxygen", "feature": "machines",
                      "colors": [(70, 120, 180), (110, 160, 214), (160, 200, 236), (214, 234, 250)],
                      "tick_delay": 3, "slope": 4, "drop_off": 1},
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
    # Batch 13: the scarce third part of air, a shielding gas.
    "argon": {"display": "Argon", "feature": "machines",
              "colors": [(170, 120, 200), (190, 150, 220), (212, 182, 236), (234, 216, 248)]},
    # Batch 14: cracked out of naphtha, polymerized into synthetic rubber.
    "butadiene": {"display": "Butadiene", "feature": "crude_oil",
                  "colors": [(150, 160, 120), (176, 186, 144), (200, 208, 170), (226, 232, 204)]},
    # Batch 15: chlorine joined to refinery gas (standing in for ethylene), polymerized into PVC.
    "vinyl_chloride": {"display": "Vinyl Chloride", "feature": "salt",
                       "colors": [(170, 180, 130), (194, 204, 156), (214, 222, 182), (236, 240, 214)]},
    # Batch 29: the sulfur hydrotreating strips out of diesel, carried off as hydrogen sulfide (sour gas). The
    # chemical reactor recovers it as sulfur (the Claus process). A sickly yellow-green.
    "hydrogen_sulfide": {"display": "Hydrogen Sulfide", "feature": "crude_oil",
                         "colors": [(150, 150, 70), (178, 178, 96), (204, 204, 126), (230, 230, 170)]},
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
    # Rubber and polymers (batch 14): synthetic rubber, and gaskets pressed from it with steel.
    "rubber": "Synthetic Rubber",
    "gasket": "Gasket",
    # Chlor-alkali (batch 15): PVC resin (pressed into plastic sheets) and soap (washes off status effects).
    "pvc_resin": "PVC Resin",
    "soap": "Soap",
    # Explosive weapons (batch 18): cotton nitrated into guncotton, packed into grenades that hurt living things but
    # never break blocks; the grenade launcher throws them further (weapons/).
    "guncotton": "Guncotton",
    "grenade": "Grenade",
    "grenade_launcher": "Grenade Launcher",
    # Power (batch 19): fitted in the advanced engine's slot; needs coolant water.
    "turbocharger": "Turbocharger",
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
    # Crude oil -> four fractions, each drawn off at its own height (Java: MachineKind.outputLayer). Batch 24: it
    # also takes heavy fuel oil and vacuum-distils it into lubricant (fifth tank, layer 1) and asphalt binder (its
    # item slot), replacing the vacuum distillation unit. 128 JE/t.
    "distillation_tower": {"inputs": [16_000], "outputs": [8_000, 8_000, 8_000, 8_000, 8_000], "item_inputs": 0,
                           "item_outputs": 1, "recipe_type": "distillation"},
    # Heavy fuel oil + water (steam) + catalyst -> diesel (base), naphtha (layer 2), refinery gas (top). Batch 24: it
    # also reforms naphtha over the same catalyst into gasoline (fourth tank, layer 1), replacing the catalytic
    # reformer. 160 JE/t.
    "catalytic_cracker": {"inputs": [8_000, 8_000], "outputs": [8_000, 8_000, 8_000, 8_000], "item_inputs": 1,
                          "item_outputs": 0, "recipe_type": "catalytic_cracking"},
    # Over shale (no recipes): 4 mB/t fracking fluid down; 8 mB/t freed (6 crude, 2 gas) and 3 mB/t flowback up.
    # 256 JE/t. Draw-offs: crude at the base, flowback one block up, gas at the top (MachineKind.outputLayer).
    "fracking_rig": {"inputs": [16_000], "outputs": [16_000, 8_000, 16_000], "item_inputs": 0, "item_outputs": 0,
                     "recipe_type": None},
    # Settling basins and a filter press (batch 24 widens the flowback treatment unit): flowback water -> clean water
    # + salt; oil sand or bitumen + hot water -> crude oil (+ sand), replacing the oil sand extractor; mud -> clay.
    # 48 JE/t.
    "flowback_treatment_unit": {"inputs": [8_000], "outputs": [8_000], "item_inputs": 1, "item_outputs": 1,
                                "recipe_type": "water_treatment"},
    # Burns diesel (256 JE/mB) or heavy fuel oil (128 JE/mB) from its tank at 256 JE/t (FLUID_FUELS).
    "diesel_generator": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Burns gasoline (384 JE/mB) or refinery gas (192 JE/mB) at 512 JE/t; the second tank takes lubricant,
    # 1 mB every 20 ticks of running (FluidFuels.LUBRICANT_TICKS), and it will not run without it.
    "gas_turbine": {"inputs": [16_000, 4_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Burns diesel or heavy fuel oil (FLUID_FUELS, KE per mB) to turn a shaft at up to 512 KE/t.
    "diesel_engine": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Brine -> chlorine (top), hydrogen (middle) and lye (base); alumina + coke -> aluminum (batch 5); water ->
    # hydrogen (middle) and oxygen (top) (batch 24). 256 JE/t.
    "electrolytic_cell": {"inputs": [8_000], "outputs": [8_000, 8_000, 8_000], "item_inputs": 2, "item_outputs": 1,
                          "recipe_type": "electrolysis"},
    # Burns hydrogen (128 JE/mB) at 128 JE/t; one block, electric look (batch 5).
    "fuel_cell": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 0, "recipe_type": None},
    # Burns gasoline (448 KE/mB) or diesel (320) to turn a shaft at up to 1,024 KE/t (batch 10).
    "advanced_engine": {"inputs": [8_000, 4_000], "outputs": [], "item_inputs": 1, "item_outputs": 0, "recipe_type": None},
    # Sulfur + water -> sulfuric acid; bauxite + lye -> alumina; phosphate + acid -> fertilizer (batch 5). Batch 24:
    # also the mixing jobs (brine, fracking fluid), replacing the chemical mixer. 96 JE/t.
    "chemical_reactor": {"inputs": [8_000], "outputs": [8_000], "item_inputs": 2, "item_outputs": 1,
                         "recipe_type": "chemical_reaction"},
    # Electronics (batch 7): a wafer and copper wire etched with sulfuric acid into microchips. 192 JE/t.
    "lithography_station": {"inputs": [4_000], "outputs": [], "item_inputs": 2, "item_outputs": 1,
                            "recipe_type": "lithography"},
    # Nitrogen chemistry (batch 12). Air separation (no recipes): from the air alone, 8 mB/t nitrogen drawn off the
    # top, 2 mB/t oxygen off the base and (batch 13) 1 mB of argon every 2 ticks off the middle (Java:
    # MachineKind.outputLayer) at 64 JE/t, like the pumpjack's oil.
    "air_separation_unit": {"inputs": [], "outputs": [16_000, 16_000, 16_000], "item_inputs": 0, "item_outputs": 0,
                            "recipe_type": None},
    # A high-pressure catalytic converter: hydrogen + nitrogen -> ammonia (Haber-Bosch); ammonia + oxygen + water
    # -> nitric acid (Ostwald). 128 JE/t.
    "synthesis_converter": {"inputs": [8_000, 8_000, 8_000], "outputs": [8_000], "item_inputs": 0,
                            "item_outputs": 0, "recipe_type": "gas_synthesis"},
    # Refinery gas -> plastic pellets. 96 JE/t.
    "polymerization_reactor": {"inputs": [8_000], "outputs": [], "item_inputs": 0, "item_outputs": 1,
                               "recipe_type": "polymerization"},
    # Batch 29. Fuel in tank 0, hydrogen or bioethanol in tank 1; the finished fuel is drawn off the base and the sour
    # gas off the top (Java: MachineKind.outputLayer). Its catalyst bed is built in, so it has no item slots. 128 JE/t.
    "hydrotreater": {"inputs": [8_000, 8_000], "outputs": [8_000, 8_000], "item_inputs": 0, "item_outputs": 0,
                     "recipe_type": "hydrotreating"},
    # Batch 29. Bolted to a running diesel generator or gas turbine, it boils water in that generator's exhaust and
    # makes JE from the steam (no recipes): water in tank 0 (or a spring below it), lubricant in tank 1.
    "heat_recovery_unit": {"inputs": [8_000, 4_000], "outputs": [], "item_inputs": 0, "item_outputs": 0,
                           "recipe_type": None},
}

# JE per mB each fluid-burning generator gets from each fuel (Java: chemistry/FluidFuels).
FLUID_FUELS = {
    "diesel_generator": {"diesel": 256, "heavy_fuel_oil": 128, "premium_diesel": 320},
    "gas_turbine": {"gasoline": 384, "refinery_gas": 192, "bioethanol": 192, "premium_gasoline": 448, "kerosene": 448},
    "diesel_engine": {"diesel": 256, "heavy_fuel_oil": 128, "premium_diesel": 320},
    "fuel_cell": {"hydrogen": 128},
    "advanced_engine": {"gasoline": 448, "diesel": 320, "bioethanol": 256, "premium_diesel": 400, "premium_gasoline": 512,
                        "kerosene": 480},
}

# Fluid recipes per machine. Each: name, item ingredients [(item or #tag, count)], fluids in [(fluid, mB)],
# fluids out [(fluid, mB)], item results [(item, count)], ticks, feature switches.
# "source": mB of fluid the recipe releases from its items (the audit allows that much more fluid out than in).
FLUID_RECIPES = {
    # One bucket of crude oil splits into fractions that add up to one bucket, in the tower's output tank order.
    "distillation_tower": [
        {"name": "crude_oil", "fluids": [("jugcraft:crude_oil", 1000)],
         "fluid_results": [("jugcraft:refinery_gas", 100), ("jugcraft:naphtha", 250), ("jugcraft:diesel", 400),
                           ("jugcraft:heavy_fuel_oil", 250)], "ticks": 100, "features": ["crude_oil"]},
        # Vacuum distillation (batch 24, from the old vacuum distillation unit): heavy fuel oil fed back in boils under
        # vacuum into lubricant (fifth tank) and a residue of asphalt binder.
        {"name": "heavy_fuel_oil", "fluids": [("jugcraft:heavy_fuel_oil", 1000)],
         "fluid_results": [("jugcraft:lubricant", 400, 4)], "results": [("jugcraft:asphalt_binder", 2)], "ticks": 120,
         "features": ["crude_oil"]},
    ],
    # Cracking breaks heavy oil into lighter fuels; the steam's water is not counted as product.
    "catalytic_cracker": [
        {"name": "heavy_fuel_oil", "items": [("jugcraft:cracking_catalyst", 1)],
         "fluids": [("jugcraft:heavy_fuel_oil", 1000), ("minecraft:water", 250)],
         "fluid_results": [("jugcraft:diesel", 500), ("jugcraft:naphtha", 300), ("jugcraft:refinery_gas", 200)],
         "source": 0, "ticks": 160, "features": ["crude_oil"]},
        # Catalytic reforming (batch 24, from the old catalytic reformer): naphtha rearranged over the catalyst into
        # high-octane gasoline (fourth tank), giving off a little gas (the gas tank).
        {"name": "naphtha", "items": [("jugcraft:cracking_catalyst", 1)], "fluids": [("jugcraft:naphtha", 1000)],
         "fluid_results": [("jugcraft:gasoline", 900, 3), ("jugcraft:refinery_gas", 100, 2)], "source": 0,
         "ticks": 120, "features": ["crude_oil"]},
    ],
    # Refinery gas polymerizes into plastic: a bucket of gas gives four pellets.
    "polymerization_reactor": [
        {"name": "refinery_gas", "fluids": [("jugcraft:refinery_gas", 1000)], "fluid_results": [],
         "results": [("jugcraft:plastic_pellets", 4)], "source": 0, "ticks": 100, "features": ["crude_oil"]},
        # PVC (batch 15): vinyl chloride polymerized into resin.
        {"name": "vinyl_chloride", "fluids": [("jugcraft:vinyl_chloride", 500)], "fluid_results": [],
         "results": [("jugcraft:pvc_resin", 4)], "source": 0, "ticks": 100, "features": ["crude_oil", "salt"]},
        # Synthetic rubber (batch 14): butadiene polymerized into crumbs of rubber.
        {"name": "butadiene", "fluids": [("jugcraft:butadiene", 500)], "fluid_results": [],
         "results": [("jugcraft:rubber", 4)], "source": 0, "ticks": 100, "features": ["crude_oil"]},
    ],
    # The chlor-alkali process: a bucket of brine splits into chlorine at the anode, hydrogen at the cathode and lye
    # left in the cell. Electricity-hungry: 256 JE/t for 400 ticks (102,400 JE), the same as the Electrolytic
    # Separator (the owner's starter baseline; it was 200 ticks before).
    "electrolytic_cell": [
        {"name": "brine", "fluids": [("jugcraft:brine", 1000)],
         "fluid_results": [("jugcraft:chlorine", 250), ("jugcraft:hydrogen", 250), ("jugcraft:lye", 500)],
         "ticks": 400, "features": ["salt"]},
        # The Hall-Heroult process: alumina dissolved in molten salt and split with a coke anode, which burns away.
        {"name": "aluminum", "items": [("jugcraft:alumina", 2), ("jugcraft:coke", 1)],
         "results": [("jugcraft:aluminum_ingot", 2)], "ticks": 160, "features": ["aluminum"]},
        # Water electrolysis (batch 24): hydrogen at the cathode (middle), oxygen at the anode (top), two to one. An
        # early hydrogen source for the fuel cell, deliberately dear: 204,800 JE for 500 mB of hydrogen (410 JE/mB)
        # against the fuel cell's 128 JE/mB, so even four efficiency cards (41%) leave it a loss.
        {"name": "water", "fluids": [("minecraft:water", 1000)],
         "fluid_results": [("jugcraft:oxygen", 250, 0), ("jugcraft:hydrogen", 500, 1)], "ticks": 800,
         "features": ["machines"]},
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
        # Saponification (batch 15): fat (rotten flesh, rendered) boiled in lye sets into soap.
        {"name": "soap", "items": [("minecraft:rotten_flesh", 2)], "fluids": [("jugcraft:lye", 250)],
         "results": [("jugcraft:soap", 4)], "ticks": 80, "features": ["salt"]},
        # Steam cracking (batch 14): naphtha broken down at high heat; the butadiene is kept, the rest is lost as
        # fuel for the cracking furnace.
        {"name": "butadiene", "fluids": [("jugcraft:naphtha", 1000)], "fluid_results": [("jugcraft:butadiene", 500)],
         "ticks": 100, "features": ["crude_oil"]},
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
        # Guncotton (batch 18): cotton nitrated in nitric acid (nitrocellulose), the grenade's charge.
        {"name": "guncotton", "items": [("jugcraft:cotton", 2)], "fluids": [("jugcraft:nitric_acid", 250)],
         "results": [("jugcraft:guncotton", 2)], "ticks": 100, "features": ["machines", "explosives"]},
        # Field chemistry (batch 31, tools/field_chemistry.py). Chlorine grenades: a steel case charged with chlorine.
        {"name": "chlorine_grenade", "items": [("#c:plates/steel", 1), ("minecraft:iron_nugget", 1)],
         "fluids": [("jugcraft:chlorine", 500)], "results": [("jugcraft:chlorine_grenade", 2)], "ticks": 80,
         "features": ["machines", "salt"]},
        # Smoke grenades: sugar to burn and an ammonium salt to make a thick white smoke.
        {"name": "smoke_grenade", "items": [("#c:plates/steel", 1), ("minecraft:sugar", 2)],
         "fluids": [("jugcraft:ammonia", 250)], "results": [("jugcraft:smoke_grenade", 2)], "ticks": 80,
         "features": ["machines"]},
        # Construction chemistry (batch 32, tools/construction.py). A foam canister: polyurethane-style foam from plastic
        # and ammonia, in a steel can.
        {"name": "foam_canister", "items": [("jugcraft:plastic_pellets", 2), ("minecraft:iron_nugget", 1)],
         "fluids": [("jugcraft:ammonia", 250)], "results": [("jugcraft:foam_canister", 1)], "ticks": 80,
         "features": ["machines", "crude_oil"]},
        # Hydroponics (batch 33): nutrient solution, a fertilizer dissolved in a bucket of water; enough for ten harvests.
        {"name": "nutrient_solution", "items": [("jugcraft:fertilizer", 1)], "fluids": [("minecraft:water", 1000)],
         "fluid_results": [("jugcraft:nutrient_solution", 1000)], "ticks": 40, "features": ["machines"]},
        # Medicines. A first aid kit: cotton dressings and soap, sterilised in bioethanol.
        {"name": "first_aid_kit", "items": [("jugcraft:cotton", 2), ("jugcraft:soap", 1)],
         "fluids": [("jugcraft:bioethanol", 250)], "results": [("jugcraft:first_aid_kit", 2)], "ticks": 100,
         "features": ["machines"]},
        # Antidote: charcoal activated in lye, in a bottle of water.
        {"name": "antidote", "items": [("minecraft:charcoal", 2), ("minecraft:glass_bottle", 1)],
         "fluids": [("jugcraft:lye", 250)], "results": [("jugcraft:antidote", 1)], "ticks": 100,
         "features": ["machines", "salt"]},
        # Stimulant: caffeine extracted from cocoa beans with bioethanol.
        {"name": "stimulant", "items": [("minecraft:cocoa_beans", 4), ("minecraft:glass_bottle", 1)],
         "fluids": [("jugcraft:bioethanol", 250)], "results": [("jugcraft:stimulant", 1)], "ticks": 100,
         "features": ["machines"]},
        # Vanadium electrolyte (batch 17): the vanadium in heavy oil residue leached into sulfuric acid, for the flow
        # battery. As much electrolyte as acid goes in.
        {"name": "vanadium_electrolyte", "items": [("jugcraft:asphalt_binder", 2)],
         "fluids": [("jugcraft:sulfuric_acid", 1000)], "fluid_results": [("jugcraft:vanadium_electrolyte", 1000)],
         "ticks": 160, "features": ["crude_oil", "sulfur"]},
        # Fermentation (batch 26, after Mekanism's bio-generator): eight crops (#jugcraft:fermentable) in a bucket of
        # water make 250 mB of bioethanol for the gas turbine or the advanced engine.
        {"name": "bioethanol", "items": [("#jugcraft:fermentable", 8)], "fluids": [("minecraft:water", 1000)],
         "fluid_results": [("jugcraft:bioethanol", 250)], "source": 0, "ticks": 100, "features": ["machines"]},
        # Sulfur recovery (batch 29, the Claus process): hydrogen sulfide from the hydrotreater partly burnt, and the
        # rest reacted over a catalyst into sulfur: a renewable source of sulfur for the acid recipes.
        {"name": "sulfur_recovery", "fluids": [("jugcraft:hydrogen_sulfide", 200)], "results": [("jugcraft:sulfur_dust", 1)],
         "ticks": 60, "features": ["crude_oil", "sulfur"]},
        # Mixing jobs (batch 24, from the old chemical mixer). Fracking fluid: water carrying sand (to prop the
        # cracks open) and a gelling agent (dried kelp, standing in for guar gum) to carry the sand.
        {"name": "fracking_fluid", "items": [("minecraft:sand", 2), ("minecraft:dried_kelp", 1)],
         "fluids": [("minecraft:water", 1000)], "fluid_results": [("jugcraft:fracking_fluid", 1000)], "source": 0,
         "ticks": 80, "features": ["crude_oil"]},
        # Brine for the electrolytic cell: two salt dissolved in a bucket of water.
        {"name": "brine", "items": [("jugcraft:salt", 2)], "fluids": [("minecraft:water", 1000)],
         "fluid_results": [("jugcraft:brine", 1000)], "source": 0, "ticks": 60, "features": ["salt"]},
    ],
    # Batch 29: refinery upgrades. Hydrotreating: diesel and hydrogen over the built-in catalyst bed. The sulfur
    # leaves as hydrogen sulfide (the top tank), and the diesel burns a quarter better (FLUID_FUELS).
    "hydrotreater": [
        {"name": "premium_diesel", "fluids": [("jugcraft:diesel", 1000), ("jugcraft:hydrogen", 100)],
         "fluid_results": [("jugcraft:premium_diesel", 1000), ("jugcraft:hydrogen_sulfide", 100)], "ticks": 120,
         "features": ["crude_oil", "salt"]},
        # Blending: a tenth of bioethanol raises gasoline's octane, worth more than the two fuels apart.
        {"name": "premium_gasoline", "fluids": [("jugcraft:gasoline", 900), ("jugcraft:bioethanol", 100)],
         "fluid_results": [("jugcraft:premium_gasoline", 1000)], "ticks": 40, "features": ["crude_oil", "machines"]},
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
        # Vinyl chloride (batch 15): chlorine added to refinery gas (its ethylene), at heat over a catalyst.
        {"name": "vinyl_chloride", "fluids": [("jugcraft:refinery_gas", 250), ("jugcraft:chlorine", 250)],
         "fluid_results": [("jugcraft:vinyl_chloride", 250)], "ticks": 40, "features": ["crude_oil", "salt"]},
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
        # Hot-water extraction (batch 24, from the old oil sand extractor): the oil floats off in the settling cells
        # and the sand sinks.
        # A whole oil sand block (silk touch) gives the most: two buckets of crude oil from four blocks.
        {"name": "oil_sand", "items": [("jugcraft:oil_sand", 1)], "fluids": [("minecraft:water", 250)],
         "fluid_results": [("jugcraft:crude_oil", 500)], "results": [("minecraft:sand", 1)], "source": 500,
         "ticks": 160, "features": ["crude_oil"]},
        # Bitumen (what oil sand drops, or crushes into, three to a block) gives less per block.
        {"name": "bitumen", "items": [("jugcraft:bitumen", 1)], "fluids": [("minecraft:water", 100)],
         "fluid_results": [("jugcraft:crude_oil", 150)], "source": 150, "ticks": 80, "features": ["crude_oil"]},
        # The filter press squeezes the water out of mud, leaving clay: four balls a block, as vanilla's dripstone
        # gives a clay block.
        {"name": "mud", "items": [("minecraft:mud", 1)], "fluid_results": [("minecraft:water", 250)],
         "results": [("minecraft:clay_ball", 4)], "source": 250, "ticks": 60, "features": ["machines"]},
    ],
}


def _leaching():
    """Batch 26: each ore dissolved in 250 mB of sulfuric acid gives four washed ores (ORE_LEACHING_MULTIPLIER), the
    best ore route, one better than the ore washer's three."""
    from machines import ORE_LEACHING_MULTIPLIER, _metal_features
    from materials import WASHED_ORES, ore_ids
    return [{"name": ore.split(":")[1], "items": [(ore, 1)], "fluids": [("jugcraft:sulfuric_acid", 250)],
             "results": [(f"jugcraft:washed_{metal}_ore", ORE_LEACHING_MULTIPLIER)], "ticks": 160,
             "ore_bonus": ORE_LEACHING_MULTIPLIER, "features": ["sulfur"] + _metal_features(metal)}
            for metal in WASHED_ORES for ore in ore_ids(metal)]


FLUID_RECIPES["chemical_reactor"] += _leaching()

# Gas storage (batch 35, tools/gas_storage.py): the ammonia chiller, ammonia first and water second.
import gas_storage as _gas_storage  # noqa: E402
FLUID_MACHINES["ammonia_chiller"] = _gas_storage.FLUID_MACHINE
FLUID_RECIPES["ammonia_chiller"] = _gas_storage.FLUID_RECIPES
# Rocketry (batch 38, tools/rocketry.py): iodine from kelp and ammonium perchlorate in the chemical reactor.
import rocketry as _rocketry  # noqa: E402
FLUID_RECIPES["chemical_reactor"] += _rocketry.REACTOR_RECIPES
# Liquid fuels (batch 43, tools/rocketry.py): hydrocracking to kerosene, the cryogenic liquefier, propellant tanks.
FLUID_RECIPES["catalytic_cracker"] += _rocketry.CRACKER_RECIPES
FLUID_MACHINES["cryogenic_liquefier"] = _rocketry.LIQUEFIER
FLUID_RECIPES["cryogenic_liquefier"] = _rocketry.LIQUEFIER_RECIPES
FLUID_RECIPES["chemical_reactor"] += _rocketry.TANK_RECIPES

# Crops that ferment into bioethanol (data/jugcraft/tags/item/fermentable.json).
FERMENTABLE = ["minecraft:wheat", "minecraft:sugar_cane", "minecraft:potato", "minecraft:carrot", "minecraft:beetroot",
               "minecraft:sweet_berries", "minecraft:melon_slice", "minecraft:apple"]


def result_tank(index, result):
    """The output tank a fluid result goes to: its own third value (batch 24), or else its position."""
    return result[2] if len(result) > 2 else index


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
                data["fluid_results"] = [{"fluid": r[0], "amount": r[1]} | ({"tank": r[2]} if len(r) > 2 else {})
                                         for r in recipe["fluid_results"]]
            if recipe.get("results"):
                data["results"] = [{"id": item, "count": count} for item, count in recipe["results"]]
            data["time"] = recipe["ticks"]
            yield kind, recipe["name"], data
