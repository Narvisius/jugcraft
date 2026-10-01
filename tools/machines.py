"""Machine, cable and multiblock definitions (single source of truth).

generate_material_data.py turns this into block models, loot, names, crafting recipes and
src/main/resources/jugcraft/machine_recipes.json; check_mod_data.py audits it.
Energy is measured in JE (Jugcraft Energy).
"""

# Orientable machine blocks with block entities. lit: has an "on" front texture.
MACHINES = {
    "coal_generator": {"display": "Coal Generator", "lit": True},
    "battery_box": {"display": "Battery Box", "lit": False},
    "electric_furnace": {"display": "Electric Furnace", "lit": True},
    "crusher": {"display": "Crusher", "lit": True},
    "arc_furnace_controller": {"display": "Arc Furnace Controller", "lit": True},
    "solar_panel": {"display": "Solar Panel", "lit": False, "top": "solar_panel_top", "front": "machine_side"},
    "steam_generator": {"display": "Steam Generator", "lit": True},
    "alloy_smelter": {"display": "Alloy Smelter", "lit": True},
    "metal_press": {"display": "Metal Press", "lit": True},
    "wire_drawer": {"display": "Wire Drawer", "lit": True},
    "circuit_assembler": {"display": "Circuit Assembler", "lit": True},
    # Processing depth: dusts, washing, sifting and sawing (docs/TECH_TREE.md#ore-processing).
    "pulverizer": {"display": "Pulverizer", "lit": True},
    "ore_washer": {"display": "Ore Washer", "lit": True},
    "sieve": {"display": "Sieve", "lit": True},
    "sawmill": {"display": "Sawmill", "lit": True},
    # Steel tier: unpowered brick multi-blocks (tools/large_machines.py).
    "coke_oven": {"display": "Coke Oven", "lit": True},
    "steel_foundry": {"display": "Steel Foundry", "lit": True},
    # Storage multi-blocks (2x2): a capacitor bank and a steel tank.
    "capacitor_bank": {"display": "Capacitor Bank", "lit": False},
    "steel_tank": {"display": "Steel Tank", "lit": False},
    # Mining: a 2-tall powered derrick that mines ores in a 9x9 column below it.
    "ore_drill": {"display": "Ore Drill", "lit": True},
    # A 3x3x2 rig that works the surface deposit blocks under and around it (tools/deposits.py).
    "deposit_drill": {"display": "Deposit Drill", "lit": True},
    # Renewables: cobblestone from water and lava, wood from saplings, power from flowing water.
    "cobblestone_generator": {"display": "Cobblestone Generator", "lit": True},
    "tree_farm": {"display": "Tree Farm", "lit": True},
    "water_wheel": {"display": "Water Wheel", "lit": False},
    # Crafts the recipe laid out in its 3x3 grid, keeping one of each item as the pattern.
    "auto_crafter": {"display": "Auto-Crafter", "lit": True},
    # Kinetic: a 2x2x2 steam engine, four times the small one (kinetic/SteamEngineBlock).
    "large_steam_engine": {"display": "Large Steam Engine", "lit": True},
    # Multi-block machines: models and footprints live in tools/large_machines.py.
    "geothermal_generator": {"display": "Geothermal Generator", "lit": True},
    # Petrochemistry (tools/petro.py): a 1x3x3 pumpjack over a conventional oil reservoir.
    "pumpjack": {"display": "Pumpjack", "lit": True},
    # A 2x2x2 hot-water extraction plant: oil sand or bitumen + water -> crude oil (+ sand).
    "oil_sand_extractor": {"display": "Oil Sand Extractor", "lit": True},
    # A 2x2 column seven blocks tall: crude oil -> refinery gas, naphtha, diesel and heavy fuel oil.
    "distillation_tower": {"display": "Distillation Tower", "lit": True},
    # A 2x2x4 fluid catalytic cracker: heavy fuel oil + steam + catalyst -> diesel, naphtha, refinery gas.
    "catalytic_cracker": {"display": "Catalytic Cracker", "lit": True},
    # A 2x2x3 vacuum column: heavy fuel oil -> lubricant + asphalt binder.
    "vacuum_distillation_unit": {"display": "Vacuum Distillation Unit", "lit": True},
    # A 3x2x2 row of reactors: naphtha -> gasoline + refinery gas.
    "catalytic_reformer": {"display": "Catalytic Reformer", "lit": True},
    # A 2x2x2 stirred mixing vessel: water + sand + dried kelp -> fracking fluid.
    "chemical_mixer": {"display": "Chemical Mixer", "lit": True},
    # A 3x3x5 derrick over shale oil: fracking fluid down; crude oil, refinery gas and flowback water up.
    "fracking_rig": {"display": "Fracking Rig", "lit": True},
    # 3x1x2 settling basins and a filter press: flowback water -> water + salt.
    "flowback_treatment_unit": {"display": "Flowback Treatment Unit", "lit": True},
    # A 3x2x2 inline six on a skid: burns diesel or heavy fuel oil from its tank.
    "diesel_generator": {"display": "Diesel Generator", "lit": True},
    # A 4x2x2 gas turbine: burns gasoline or refinery gas, with a lubricant tank.
    "gas_turbine": {"display": "Gas Turbine", "lit": True},
    # A 2x2x3 jacketed reactor: refinery gas -> plastic pellets.
    "polymerization_reactor": {"display": "Polymerization Reactor", "lit": True},
    # Industrial chemistry: a 3x3x2 electrolysis house (brine -> chlorine, hydrogen, lye; alumina -> aluminum).
    "electrolytic_cell": {"display": "Electrolytic Cell", "lit": True},
    # A 2x2x2 acid-proof reactor: sulfur + water -> sulfuric acid, and later digestion and fertilizer.
    "chemical_reactor": {"display": "Chemical Reactor", "lit": True},
    # Nitrogen chemistry (batch 12): a cold box splitting air, and the high-pressure synthesis converter.
    "air_separation_unit": {"display": "Air Separation Unit", "lit": True},
    "synthesis_converter": {"display": "Synthesis Converter", "lit": True},
    # A one-block hydrogen fuel cell in the electric look: hydrogen -> JE.
    "fuel_cell": {"display": "Fuel Cell", "lit": True},
    # Storage (batch 6): a 3x2 lithium battery bank in the electric look.
    "lithium_battery_bank": {"display": "Lithium Battery Bank", "lit": False},
    # Chemistry (batch 17): a 3x3x2 vanadium redox flow battery in the electric look.
    "flow_battery": {"display": "Flow Battery", "lit": False},
    # Electronics (batch 7, the cyan look).
    "crystal_grower": {"display": "Crystal Grower", "lit": True},
    "lithography_station": {"display": "Lithography Station", "lit": True},
    # Fluid logistics (batch 8): a Horton sphere for gases.
    "gas_holder": {"display": "Gas Holder", "lit": False},
    # Power (batch 10): a pedestal carrying a 3x3 array of solar cells.
    "advanced_solar_panel": {"display": "Advanced Solar Panel", "lit": False},
    # A two-block four-cylinder engine: gasoline or diesel into a shaft at up to 1,024 KE/t.
    "advanced_engine": {"display": "Advanced Combustion Engine", "lit": True},
    # Farming (batch 9): harvests and replants ripe crops in the 9x9 field in front of it.
    "crop_harvester": {"display": "Crop Harvester", "lit": True},
    # Kinetic: a 2x2x3 V8 diesel engine; its shaft leaves the back of the upper right back block.
    "diesel_engine": {"display": "Diesel Engine", "lit": True},
    "wind_turbine": {"display": "Wind Turbine", "lit": False},
}

# Plain crafting-component / structure blocks.
PARTS = {
    "machine_casing": "Machine Casing",
    "arc_furnace_casing": "Arc Furnace Casing",
}

# Cables: display name and JE per tick one push may send through the network.
# Tiers connect to each other; a network carries as much as its slowest cable (energy/EnergyNetworks).
# Electric look (tools/electric_textures.py): 6 pixels thick, graphite with a glowing green core; the collars at the
# ends show the tier's metal.
CABLES = {"copper_cable": {"display": "Copper Cable", "rate": 256, "size": 6},
          "silver_cable": {"display": "Silver Cable", "rate": 1_024, "size": 6},
          "aluminum_cable": {"display": "Aluminum Cable", "rate": 4_096, "size": 6}}

# Item logistics (see docs/TECH_TREE.md#item-logistics). The tube is a 6-pixel transmitter; the
# extractor and sorter face any of six directions. Models: tools/logistics_models.py.
ITEM_PIPES = {"brass_item_pipe": {"display": "Brass Item Pipe", "size": 6}}
LOGISTICS_BLOCKS = {
    "pneumatic_extractor": {"display": "Pneumatic Extractor"},
    # Steel tier: 32 items every 4 ticks (the brass one: 16 every 8).
    "high_pressure_extractor": {"display": "High-Pressure Extractor"},
    "item_sorter": {"display": "Item Sorter"},
}
# Electronics (batch 7): a beige retro computer that reads out its power network (electronics/NetworkTerminalBlock).
ELECTRONICS_BLOCKS = {"network_terminal": {"display": "Network Terminal"}}
# Farming (batch 9): a pipe-fed sprinkler with a fertilizer hopper (farming/SprinklerBlock); "wet" shows spray.
FARMING_BLOCKS = {"sprinkler": {"display": "Sprinkler", "states": "wet"}}
# Crops (farming/CottonCropBlock): the crop block (planted by its seeds item, no item of its own), its seeds and its
# product. Growth ages 0-7 show the block's four stage textures.
CROPS = {"cotton_crop": {"display": "Cotton", "seeds": "cotton_seeds", "seeds_display": "Cotton Seeds",
                         "product": "cotton", "product_display": "Cotton", "stages": [0, 0, 1, 1, 2, 2, 2, 3]}}
TOOLS = {"brass_wrench": "Brass Wrench", "engineers_handbook": "Engineer's Handbook",
         # Mining & prospecting: a vague 3x3-chunk ore survey (prospecting/OreSurvey).
         "prospector": "Geo-Resonance Prospector",
         # Kinetic: links two belt pulleys.
         "belt": "Leather Belt"}
# Kinetic power (kinetic/): rotation in KE per tick, carried by shafts and gearboxes. Models: tools/kinetic_models.py.
# "axis": placed like a log; "facing": six directions; "horizontal": four.
KINETIC_BLOCKS = {
    "iron_shaft": {"display": "Iron Shaft", "states": "axis"},
    "brass_gearbox": {"display": "Brass Gearbox", "states": "none"},
    "hand_crank": {"display": "Hand Crank", "states": "facing"},
    "steam_engine": {"display": "Steam Engine", "states": "horizontal"},
    "dynamo": {"display": "Dynamo", "states": "horizontal"},
    # A shaft with a grooved wheel; a belt links two of them (kinetic/BeltItem).
    "belt_pulley": {"display": "Belt Pulley", "states": "axis"},
    # JE -> KE at 75%; its shaft points the way the player looked when placing it.
    "electric_motor": {"display": "Electric Motor", "states": "facing"},
    # Rare-earth magnet versions (batch 6): faster and far less lossy.
    "magnet_dynamo": {"display": "Magnet Dynamo", "states": "horizontal"},
    "magnet_motor": {"display": "Magnet Motor", "states": "facing"},
    # Item conveyors (logistics/ConveyorBlock): driven by rotation, facing the way items travel.
    "conveyor": {"display": "Conveyor", "states": "horizontal"},
    "conveyor_splitter": {"display": "Conveyor Splitter", "states": "horizontal"},
}
# Conveyor slopes (logistics/ConveyorSlopeBlock): carry items one block up or down; use with an empty hand to flip.
SLOPE_BLOCKS = {"conveyor_slope": {"display": "Conveyor Slope"}}
# Powered tools (tools/), dieselpunk 3D item models in tools/tool_models.py: JE in an "energy" item component,
# charged at the charging station.
POWERED_TOOLS = {"mining_drill": "Mining Drill", "chainsaw": "Chainsaw", "rocket_pack": "Rocket Pack"}
# Upgrade modules for the powered tools, fitted at the charging station (tools/ToolUpgrades): display name, short
# name for tooltips, and what it does.
UPGRADE_MODULES = {
    "overclock_module": ("Overclock Module", "Overclock", "Mines 50% faster for twice the JE a block (up to 2)"),
    "range_module": ("Range Module", "Range", "The drill's area mode mines 5×5 (drill only)"),
    "capacity_module": ("Capacity Module", "Capacity", "Holds its base charge again (up to 2)"),
    "silk_touch_module": ("Silk Touch Module", "Silk Touch", "Blocks drop themselves (drill or chainsaw; not with fortune)"),
    "fortune_module": ("Fortune Module", "Fortune", "More ore drops, up to Fortune III (drill only; not with silk touch)"),
}
# Blocks of the powered-tools feature: the 2-tall charging station (lower and upper halves, one item).
TOOL_BLOCKS = {"charging_station": {"display": "Charging Station"}}
# Plain storage blocks with their own block entity (storage/). Models: tools/storage_models.py.
STORAGE_BLOCKS = {"item_crate": {"display": "Item Crate"}}
# Machine upgrades (docs/TECH_TREE.md#machine-control): go in a powered processor's two upgrade slots.
# At most 4 of each kind count. Numbers are in machine/MachineUpgrades.java.
UPGRADES = {"speed_upgrade": "Speed Upgrade", "efficiency_upgrade": "Efficiency Upgrade"}

# Fluid logistics (physical branch; see docs/TECH_TREE.md). Amounts are millibuckets (mB);
# 1 bucket = 1000 mB = 81000 Fabric droplets. Pipes are passive: pumps push through them.
PIPES = {"bronze_fluid_pipe": {"display": "Bronze Fluid Pipe", "rate": 250},
         # Steel tier, for refinery flows; a network carries as much as its slowest pipe.
         "steel_fluid_pipe": {"display": "Steel Fluid Pipe", "rate": 1_000},
         # Fluid logistics (batch 8): a steel pipe segment that redstone closes (tools/pipe_models.py draws its body).
         "fluid_valve": {"display": "Fluid Valve", "rate": 1_000, "texture": "steel_fluid_pipe"},
         # Lets only its chosen fluid out into the tanks and machines it touches (fluid/FluidFilterBlock).
         "fluid_filter": {"display": "Fluid Filter", "rate": 1_000, "texture": "steel_fluid_pipe"}}
# Fluid blocks with their own block entities. top/side/bottom name the textures.
FLUID_BLOCKS = {
    "fluid_tank": {"display": "Tinplate Tank"},
    "electric_pump": {"display": "Electric Pump"},
    "heavy_pump": {"display": "Heavy Pump"},
}
FLUID_STATS = {
    # Holds 16 buckets of one fluid; filled and emptied with buckets or by pumps.
    "fluid_tank": {"capacity_mb": 16_000},
    # Draws a water/lava source (or the tank) below it and pushes into pipes/storages on its other sides.
    "electric_pump": {"energy_capacity": 4_000, "input_per_tick": 64, "use_per_tick": 8,
                      "pump_per_tick": 100, "buffer_mb": 4_000},
    # Steel tier: ten times the pump, for refineries.
    "heavy_pump": {"energy_capacity": 32_000, "input_per_tick": 512, "use_per_tick": 40,
                   "pump_per_tick": 1_000, "buffer_mb": 16_000},
}

# Balance numbers shared with the Java code (MachineStats.java). Keep in sync.
STATS = {
    "coal_generator": {"capacity": 16_000, "output_per_tick": 64, "generation_per_tick": 32},
    "battery_box": {"capacity": 400_000, "io_per_tick": 256},
    "electric_furnace": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 10, "ticks": 100},
    "crusher": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 16},
    "arc_furnace_controller": {"capacity": 50_000, "input_per_tick": 512, "use_per_tick": 64},
    # Full sun under open sky; half in rain or thunder; nothing at night.
    "solar_panel": {"capacity": 4_000, "output_per_tick": 32, "generation_per_tick": 8},
    # Boils 10 mB of water per tick; the tank holds 8 buckets and a water source below refills 20 mB/t.
    "steam_generator": {"capacity": 40_000, "output_per_tick": 128, "generation_per_tick": 64,
                        "water_per_tick": 10, "tank": 8_000},
    "alloy_smelter": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 20},
    "metal_press": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 16},
    "wire_drawer": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 12},
    "circuit_assembler": {"capacity": 20_000, "input_per_tick": 256, "use_per_tick": 32},
    "pulverizer": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 20},
    # Uses 500 mB of water per operation; the tank holds 8 buckets and a water source below refills 20 mB/t.
    "ore_washer": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 16, "water_per_operation": 500,
                   "tank": 8_000},
    "sieve": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 8},
    "sawmill": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 12},
    # 2x2. Outputs from its front (all four blocks), charges from any other face.
    "capacitor_bank": {"capacity": 4_000_000, "io_per_tick": 4_096},
    # 2x2, one tall with a dome: 128 buckets of one fluid. No power.
    "steel_tank": {"capacity": 0, "tank": 128_000},
    # 3x3x3 sphere on legs: 1,024 buckets of one gas, and only gases. No power.
    "gas_holder": {"capacity": 0, "tank": 1_024_000},
    # A pedestal and a 3x3 layer of cells above it: 64 JE/t in full sun (eight solar panels), half in rain.
    "advanced_solar_panel": {"capacity": 400_000, "output_per_tick": 512, "generation_per_tick": 64},
    # 2x1x1. Gasoline 448 KE/mB, diesel 320; up to 1,024 KE/t out of the back of its master block. No JE.
    "advanced_engine": {"capacity": 0, "tank": 8_000},
    # 2 tall. One ripe crop per 20 ticks from the 9x9 field in front of it; replants with one of the seeds.
    "crop_harvester": {"capacity": 20_000, "input_per_tick": 256, "use_per_tick": 24, "ticks": 20, "radius": 4},
    # 2 tall. Mines one c:ores block per 40 ticks from a 9x9 column below it, down to the bottom of the world.
    "ore_drill": {"capacity": 20_000, "input_per_tick": 256, "use_per_tick": 32, "ticks": 40, "radius": 4},
    # 3x3x2. Every 300 ticks (15 s) takes 1 unit (item) from each kind of deposit under its base and 1 block round
    # it, 3 deep: a drill over coal and iron gives 1 coal and 1 raw iron.
    "deposit_drill": {"capacity": 20_000, "input_per_tick": 256, "use_per_tick": 16, "ticks": 300, "units": 1,
                      "reach": 1, "depth": 3},
    # Needs water and lava touching it (neither is used): 1 cobblestone per 20 ticks.
    "cobblestone_generator": {"capacity": 4_000, "input_per_tick": 64, "use_per_tick": 4, "ticks": 20},
    # Sapling -> logs in 400 ticks; the sapling comes back.
    "tree_farm": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 16},
    # 2 tall. 8 JE/t per block of flowing water on its right (12 if falling), both blocks: up to 24 JE/t.
    "water_wheel": {"capacity": 8_000, "output_per_tick": 64, "generation_per_tick": 24},
    # One craft per 40 ticks at 8 JE/t.
    "auto_crafter": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 8, "ticks": 40},
    # 2x2x2. 256 KE/t out of the back of its upper right block; 40 mB water per tick; no JE of its own.
    "large_steam_engine": {"capacity": 0, "use_per_tick": 0, "output_ke": 256, "water_per_tick": 40, "tank": 16_000},
    # Unpowered: heat comes from the charge itself. No battery, no cable connection.
    "coke_oven": {"capacity": 0, "use_per_tick": 0},
    "steel_foundry": {"capacity": 0, "use_per_tick": 0, "boost": "oxygen", "boost_per_tick": 2, "boost_tank": 8_000},
    # Two blocks wide. Burns 1 mB of lava per tick for 64 JE: a bucket lasts 1,000 ticks.
    "geothermal_generator": {"capacity": 30_000, "output_per_tick": 128, "generation_per_tick": 64,
                             "lava_per_tick": 1, "tank": 4_000},
    # Nine blocks tall with a 7-block rotor. 12 JE/t at sea level, +1 per 2 blocks higher, capped at 72;
    # x1.5 rain, x2 thunder. The 7x7 square the rotor sweeps must be clear.
    "wind_turbine": {"capacity": 48_000, "output_per_tick": 192, "generation_per_tick": 72},
    # 1x3x3. Pumps 2 mB of crude oil a tick (a bucket every 25 s) from the conventional reservoir under its chunk.
    "pumpjack": {"capacity": 20_000, "input_per_tick": 256, "use_per_tick": 32, "pump_per_tick": 2, "tank": 16_000},
    # 2x2x2. Recipes in tools/petro.py; 8,000 mB water tank and 8,000 mB crude oil tank.
    "oil_sand_extractor": {"capacity": 20_000, "input_per_tick": 256, "use_per_tick": 32, "tank": 8_000},
    # 2x2x7. 128 JE/t heats the reboiler; a bucket of crude oil per 100 ticks.
    "distillation_tower": {"capacity": 40_000, "input_per_tick": 512, "use_per_tick": 128, "tank": 16_000},
    # 2x2x4. 160 JE/t; a bucket of heavy fuel oil per 160 ticks.
    "catalytic_cracker": {"capacity": 40_000, "input_per_tick": 512, "use_per_tick": 160, "tank": 8_000},
    # 2x2x3. 96 JE/t; a bucket of heavy fuel oil per 120 ticks.
    "vacuum_distillation_unit": {"capacity": 30_000, "input_per_tick": 512, "use_per_tick": 96, "tank": 8_000},
    # 3x2x2. 120 JE/t; a bucket of naphtha per 120 ticks.
    "catalytic_reformer": {"capacity": 30_000, "input_per_tick": 512, "use_per_tick": 120, "tank": 8_000},
    # 2x2x2. 64 JE/t; a bucket of mixture per 80 ticks.
    "chemical_mixer": {"capacity": 20_000, "input_per_tick": 256, "use_per_tick": 64, "tank": 8_000},
    # 3x3x5. 256 JE/t; 4 mB/t fracking fluid down, 8 mB/t oil freed, 3 mB/t flowback.
    "fracking_rig": {"capacity": 80_000, "input_per_tick": 1_024, "use_per_tick": 256, "tank": 16_000},
    # 3x1x2. 48 JE/t; a bucket of flowback water per 80 ticks.
    "flowback_treatment_unit": {"capacity": 20_000, "input_per_tick": 256, "use_per_tick": 48, "tank": 8_000},
    # 3x2x2. 256 JE/t: 1 mB of diesel a tick (256 JE/mB) or 2 mB of heavy fuel oil (128 JE/mB).
    "diesel_generator": {"capacity": 60_000, "output_per_tick": 1_024, "generation_per_tick": 256, "tank": 8_000},
    # 4x2x2. 512 JE/t: 1.33 mB of gasoline a tick (384 JE/mB) or 2.67 mB of refinery gas (192 JE/mB);
    # 1 mB of lubricant every 20 ticks.
    "gas_turbine": {"capacity": 120_000, "output_per_tick": 2_048, "generation_per_tick": 512, "tank": 16_000},
    # 2x2x3. 96 JE/t; a bucket of refinery gas per 100 ticks.
    "polymerization_reactor": {"capacity": 30_000, "input_per_tick": 512, "use_per_tick": 96, "tank": 8_000},
    # 3x3x2. 256 JE/t; a bucket of brine per 200 ticks (51,200 JE).
    "electrolytic_cell": {"capacity": 60_000, "input_per_tick": 1_024, "use_per_tick": 256, "tank": 8_000},
    # 2x2x2. 96 JE/t; a bucket of sulfuric acid per 100 ticks.
    "chemical_reactor": {"capacity": 30_000, "input_per_tick": 512, "use_per_tick": 96, "tank": 8_000},
    # 2x2x6. From the air alone: 8 mB/t nitrogen (top) and 2 mB/t oxygen (base) at 64 JE/t.
    "air_separation_unit": {"capacity": 40_000, "input_per_tick": 512, "use_per_tick": 64, "tank": 16_000,
                            "nitrogen_per_tick": 8, "oxygen_per_tick": 2, "argon_interval": 2},
    # 3x4x2. Three input tanks and one output: ammonia (Haber-Bosch) and nitric acid (Ostwald). 128 JE/t.
    "synthesis_converter": {"capacity": 60_000, "input_per_tick": 1_024, "use_per_tick": 128, "tank": 8_000},
    # One block. 128 JE/t from 1 mB of hydrogen a tick (128 JE/mB).
    "fuel_cell": {"capacity": 40_000, "output_per_tick": 512, "generation_per_tick": 128, "tank": 8_000},
    # 3x2, one deep. Outputs from its front (all six blocks), charges from any other face.
    "lithium_battery_bank": {"capacity": 32_000_000, "io_per_tick": 16_384},
    # 3x3x2. Holds 1,000 JE per mB of vanadium electrolyte in its 64,000 mB tank, so 64,000,000 JE when full.
    # Outputs from its front, charges from any other face.
    "flow_battery": {"capacity": 64_000_000, "io_per_tick": 8_192, "tank": 64_000, "je_per_mb": 1_000},
    # 2 tall. 4 silicon + 1 phosphate (the dopant) -> a silicon boule.
    "crystal_grower": {"capacity": 60_000, "input_per_tick": 512, "use_per_tick": 128, "boost": "argon",
                       "boost_per_tick": 1, "boost_tank": 8_000},
    # 3x2x2. Wafer + 2 copper wire + 100 mB sulfuric acid -> 4 microchips.
    "lithography_station": {"capacity": 60_000, "input_per_tick": 1_024, "use_per_tick": 192, "tank": 4_000},
    # 2x2x3. Up to 512 KE/t: 2 mB of diesel a tick (256 KE/mB) or 4 mB of heavy fuel oil, only for what it delivers.
    "diesel_engine": {"capacity": 0, "use_per_tick": 0, "output_ke": 512, "tank": 8_000},
}

# Fuel for the coal generator: burn ticks per item (coal matches the vanilla furnace's 1600).
# Charcoal burns three quarters as long as coal here (vanilla furnaces are unchanged): tree farm charcoal is wood
# power, kept on purpose but a little weaker (owner decision, 1 October 2026; see docs/BALANCE.md).
GENERATOR_FUELS = {"minecraft:coal": 1600, "minecraft:charcoal": 1200, "minecraft:coal_block": 16000,
                   "jugcraft:coke": 3200}
# The steam generator also burns bitumen from oil sand.
STEAM_FUELS = {**GENERATOR_FUELS, "jugcraft:bitumen": 800}

# One ore block yields this many raw items in the crusher (the central ore-processing bonus).
ORE_PROCESSING_MULTIPLIER = 2
# The ore washer's better route: one ore -> three washed ores -> three dusts -> three ingots.
ORE_WASHING_MULTIPLIER = 3
# Pulverizer byproducts: grinding ore (or washed ore) of the first metal sometimes yields dust of the
# second, as the real ores occur together. Chance per operation.
BYPRODUCTS = {"copper": ("gold", 0.1), "iron": ("nickel", 0.1), "gold": ("silver", 0.1), "tin": ("tungsten", 0.05),
              "zinc": ("lead", 0.1), "lead": ("silver", 0.1), "silver": ("lead", 0.1), "nickel": ("iron", 0.1),
              "tungsten": ("tin", 0.1), "uranium": ("lead", 0.1)}
# Byproducts may not add more than this share of the input's metal (expected value), and a
# renewable recipe (no metal in) at most this many nugget units per operation.
BYPRODUCT_SHARE = 0.25
RENEWABLE_UNITS = 1
# Sawmill: log tag -> planks. Bamboo blocks make 2 planks by hand, logs 4.
WOODS = {"oak": "oak_logs", "spruce": "spruce_logs", "birch": "birch_logs", "jungle": "jungle_logs",
         "acacia": "acacia_logs", "dark_oak": "dark_oak_logs", "mangrove": "mangrove_logs", "cherry": "cherry_logs",
         "pale_oak": "pale_oak_logs", "crimson": "crimson_stems", "warped": "warped_stems", "bamboo": "bamboo_blocks"}

FEATURE = "machines"

# Shaped crafting recipes: id -> (pattern, key, result count). Result id equals the recipe id.
# Second recipes for items that already have one in CRAFTING: file name -> (result, pattern, key, count).
ALT_CRAFTING = {
    # Rubber (batch 14): belts from rubber instead of leather, and steel pipes sealed with gaskets.
    "belt_from_rubber": ("belt", ["RSR"], {"R": "jugcraft:rubber", "S": "minecraft:string"}, 2),
    "steel_fluid_pipe_from_gaskets": ("steel_fluid_pipe", ["PKP"], {"P": "#c:plates/steel", "K": "jugcraft:gasket"}, 4),
}

CRAFTING = {
    "machine_casing": (["BZB", "Z Z", "BZB"], {"B": "#c:ingots/bronze", "Z": "#c:ingots/zinc"}, 1),
    "copper_cable": (["CTC"], {"C": "#c:ingots/copper", "T": "#c:ingots/tin"}, 6),
    # Faster cables: silver wire (wire drawer), then aluminum wire armored with steel.
    "silver_cable": (["WWW", "RRR"], {"W": "#c:wires/silver", "R": "jugcraft:copper_cable"}, 3),
    "aluminum_cable": (["WPW", "RRR"], {"W": "#c:wires/aluminum", "P": "#c:plates/steel", "R": "jugcraft:silver_cable"}, 3),
    "coal_generator": (["BCB", "BFB", "BMB"],
                       {"B": "#c:ingots/bronze", "C": "jugcraft:copper_cable", "F": "minecraft:furnace",
                        "M": "jugcraft:machine_casing"}, 1),
    "battery_box": (["LCL", "LRL", "LML"],
                    {"L": "#c:ingots/lead", "C": "jugcraft:copper_cable", "R": "minecraft:redstone_block",
                     "M": "jugcraft:machine_casing"}, 1),
    "electric_furnace": (["BRB", "CFC", "BMB"],
                         {"B": "#c:ingots/bronze", "R": "minecraft:redstone", "C": "jugcraft:copper_cable",
                          "F": "minecraft:furnace", "M": "jugcraft:machine_casing"}, 1),
    "crusher": (["FFF", "CMC", "BRB"],
                {"F": "minecraft:flint", "C": "jugcraft:copper_cable", "M": "jugcraft:machine_casing",
                 "B": "#c:ingots/bronze", "R": "minecraft:redstone"}, 1),
    "solar_panel": (["GGG", "SSS", "BCB"],
                    {"G": "minecraft:glass", "S": "#c:silicon", "B": "#c:ingots/bronze", "C": "jugcraft:copper_cable"}, 1),
    "steam_generator": (["BUB", "CGC", "BMB"],
                        {"B": "#c:ingots/bronze", "U": "minecraft:bucket", "C": "jugcraft:copper_cable",
                         "G": "jugcraft:coal_generator", "M": "jugcraft:machine_casing"}, 1),
    "alloy_smelter": (["BCB", "FMF", "BRB"],
                      {"B": "#c:ingots/bronze", "C": "jugcraft:copper_cable", "F": "minecraft:furnace",
                       "M": "jugcraft:machine_casing", "R": "minecraft:redstone"}, 1),
    # Mechanical branch. Order matters for progression: press and drawer need only a casing;
    # the circuit assembler needs press-made plates and gears.
    "metal_press": (["BPB", "CMC", "BAB"],
                    {"B": "#c:ingots/bronze", "P": "minecraft:piston", "C": "jugcraft:copper_cable",
                     "M": "jugcraft:machine_casing", "A": "minecraft:anvil"}, 1),
    "wire_drawer": (["ZSZ", "CMC", "ZRZ"],
                    {"Z": "#c:ingots/brass", "S": "minecraft:shears", "C": "jugcraft:copper_cable",
                     "M": "jugcraft:machine_casing", "R": "minecraft:redstone"}, 1),
    "circuit_assembler": (["PGP", "CMC", "PRP"],
                          {"P": "#c:plates/tin", "G": "#c:gears/bronze", "C": "jugcraft:copper_cable",
                           "M": "jugcraft:machine_casing", "R": "minecraft:redstone"}, 1),
    # Fluid branch: pipes and tanks come from press-made plates; the pump adds gears and a casing.
    "bronze_fluid_pipe": (["PGP"], {"P": "#c:plates/bronze", "G": "minecraft:glass"}, 4),
    "fluid_tank": (["PPP", "PGP", "PPP"], {"P": "#c:plates/tin", "G": "minecraft:glass"}, 1),
    "steel_fluid_pipe": (["PGP"], {"P": "#c:plates/steel", "G": "jugcraft:bronze_fluid_pipe"}, 3),
    "heavy_pump": (["PCP", "GUG", "PMP"], {"P": "#c:plates/steel", "C": "jugcraft:silver_cable", "G": "#c:gears/steel",
                                          "U": "jugcraft:electric_pump", "M": "jugcraft:machine_casing"}, 1),
    "electric_pump": (["PUP", "GMG", "PCP"],
                      {"P": "#c:plates/bronze", "U": "minecraft:bucket", "G": "#c:gears/iron",
                       "M": "jugcraft:machine_casing", "C": "jugcraft:copper_cable"}, 1),
    # Multi-block power: built from plates, gears and circuits, so they come after the workshop tier.
    "geothermal_generator": (["PTP", "GMG", "PCP"],
                             {"P": "#c:plates/invar", "T": "jugcraft:fluid_tank", "G": "#c:gears/bronze",
                              "M": "jugcraft:machine_casing", "C": "jugcraft:basic_circuit"}, 1),
    "wind_turbine": (["AAA", "GMG", "BCB"],
                     {"A": "#c:plates/aluminum", "G": "#c:gears/bronze", "M": "jugcraft:machine_casing",
                      "B": "#c:plates/bronze", "C": "jugcraft:copper_cable"}, 1),
    # Item logistics.
    "brass_item_pipe": (["PGP"], {"P": "#c:plates/brass", "G": "minecraft:glass"}, 6),
    "pneumatic_extractor": (["PHP", "PTP"], {"P": "#c:plates/brass", "H": "minecraft:hopper",
                                             "T": "jugcraft:brass_item_pipe"}, 1),
    "high_pressure_extractor": (["PKP", "PEP"], {"P": "#c:plates/steel", "K": "minecraft:piston",
                                                 "E": "jugcraft:pneumatic_extractor"}, 1),
    "item_sorter": (["PCP", "THT", "PPP"], {"P": "#c:plates/brass", "C": "minecraft:comparator",
                                           "T": "jugcraft:brass_item_pipe", "H": "minecraft:hopper"}, 1),
    "brass_wrench": (["B B", " B ", " B "], {"B": "#c:ingots/brass"}, 1),
    # The in-game guide (tools/handbook.py): available from the start.
    "engineers_handbook": (["BC"], {"B": "minecraft:book", "C": "#c:ingots/copper"}, 1),
    # Prospecting: brass instrument with a glass screen; the circuit puts it after the workshop tier.
    "prospector": ([" W ", "PGP", "PCP"], {"W": "#c:wires/copper", "P": "#c:plates/brass", "G": "minecraft:glass_pane",
                                        "C": "jugcraft:basic_circuit"}, 1),
    # Processing depth. The pulverizer follows the metal press (plates, gears); the ore washer needs
    # invar and a circuit, so the three-fold route comes after the workshop tier.
    "pulverizer": (["FGF", "CMC", "PGP"], {"F": "minecraft:flint", "G": "#c:gears/iron", "C": "jugcraft:copper_cable",
                                           "M": "jugcraft:machine_casing", "P": "#c:plates/bronze"}, 1),
    "ore_washer": (["PUP", "GMG", "PCP"], {"P": "#c:plates/invar", "U": "minecraft:bucket", "G": "#c:gears/bronze",
                                           "M": "jugcraft:machine_casing", "C": "jugcraft:basic_circuit"}, 1),
    "sieve": (["PBP", "CMC", "PHP"], {"P": "#c:plates/iron", "B": "minecraft:iron_bars", "C": "jugcraft:copper_cable",
                                      "M": "jugcraft:machine_casing", "H": "minecraft:hopper"}, 1),
    "sawmill": (["IGI", "CMC", "PPP"], {"I": "#c:ingots/iron", "G": "#c:gears/iron", "C": "jugcraft:copper_cable",
                                        "M": "jugcraft:machine_casing", "P": "#c:plates/iron"}, 1),
    # Steel tier: brick ovens that need no power, reachable right after the metal press.
    "coke_oven": (["KIK", "KFK", "KIK"], {"K": "minecraft:bricks", "I": "#c:ingots/iron", "F": "minecraft:furnace"}, 1),
    "steel_foundry": (["KHK", "PFP", "KKK"], {"K": "minecraft:bricks", "H": "minecraft:hopper", "P": "#c:plates/iron",
                                              "F": "minecraft:blast_furnace"}, 1),
    # Upgrades: the first things built from steel.
    "speed_upgrade": (["PRP", "GCG", "PRP"], {"P": "#c:plates/steel", "R": "minecraft:redstone", "G": "#c:gears/steel",
                                              "C": "jugcraft:basic_circuit"}, 1),
    "efficiency_upgrade": (["PWP", "WCW", "PWP"], {"P": "#c:plates/steel", "W": "#c:wires/copper",
                                                   "C": "jugcraft:basic_circuit"}, 1),
    # Storage.
    "item_crate": (["PWP", "W W", "PWP"], {"P": "#c:plates/iron", "W": "#minecraft:planks"}, 1),
    "capacitor_bank": (["PBP", "BCB", "PBP"], {"P": "#c:plates/steel", "B": "jugcraft:battery_box",
                                               "C": "jugcraft:advanced_circuit"}, 1),
    "steel_tank": (["PPP", "PTP", "PPP"], {"P": "#c:plates/steel", "T": "jugcraft:fluid_tank"}, 1),
    "fluid_valve": ([" W ", "PSP", " R "], {"W": "minecraft:lever", "P": "#c:plates/steel",
                                            "S": "jugcraft:steel_fluid_pipe", "R": "minecraft:redstone"}, 2),
    "fluid_filter": ([" P ", "ISI", " P "], {"P": "#c:plates/steel", "I": "minecraft:iron_bars",
                                             "S": "jugcraft:steel_fluid_pipe"}, 1),
    "advanced_engine": (["TPT", "MDM", "TCT"], {"T": "#c:ingots/titanium", "P": "jugcraft:processor",
                                                "M": "jugcraft:neodymium_magnet", "D": "jugcraft:diesel_engine",
                                                "C": "jugcraft:machine_casing"}, 1),
    "advanced_solar_panel": (["SSS", "ACA", "PTP"], {"S": "jugcraft:solar_panel", "A": "#c:plates/aluminum",
                                                     "C": "jugcraft:processor", "P": "jugcraft:aluminum_cable",
                                                     "T": "#c:ingots/titanium"}, 1),
    "crop_harvester": (["GSG", "HMH", "PCP"], {"G": "#c:gears/steel", "S": "minecraft:shears",
                                               "H": "minecraft:hopper", "M": "jugcraft:machine_casing",
                                               "P": "#c:plates/steel", "C": "jugcraft:basic_circuit"}, 1),
    "sprinkler": ([" R ", "PTP", " S "], {"R": "jugcraft:bronze_fluid_pipe", "P": "#c:plates/steel",
                                          "T": "jugcraft:fluid_tank", "S": "minecraft:hopper"}, 1),
    "gas_holder": (["PTP", "TVT", "PTP"], {"P": "#c:plates/steel", "T": "jugcraft:steel_tank",
                                           "V": "jugcraft:steel_fluid_pipe"}, 1),
    # Automation: after the workshop tier (a circuit and brass plates).
    "auto_crafter": (["PCP", "TMT", "PHP"], {"P": "#c:plates/brass", "C": "jugcraft:basic_circuit",
                                           "T": "minecraft:crafting_table", "M": "jugcraft:machine_casing",
                                           "H": "minecraft:hopper"}, 1),
    "large_steam_engine": (["SPS", "PCP", "SPS"], {"S": "jugcraft:steam_engine", "P": "#c:plates/iron",
                                                 "C": "jugcraft:machine_casing"}, 1),
    # Kinetic power: all bronze-age, so it can come before electricity.
    "iron_shaft": (["I", "I"], {"I": "#c:ingots/iron"}, 4),
    "brass_gearbox": (["PGP", "GSG", "PGP"], {"P": "#c:plates/brass", "G": "#c:gears/bronze", "S": "jugcraft:iron_shaft"}, 1),
    "hand_crank": (["PS"], {"P": "#minecraft:planks", "S": "jugcraft:iron_shaft"}, 1),
    "steam_engine": (["BUB", "PFP", "BSB"], {"B": "#c:ingots/bronze", "U": "minecraft:bucket", "P": "minecraft:piston",
                                           "F": "minecraft:furnace", "S": "jugcraft:iron_shaft"}, 1),
    "belt_pulley": (["PSP"], {"P": "#minecraft:planks", "S": "jugcraft:iron_shaft"}, 1),
    "belt": (["LSL"], {"L": "minecraft:leather", "S": "minecraft:string"}, 1),
    "electric_motor": (["PWP", "WSW", "PCP"], {"P": "#c:plates/iron", "W": "#c:wires/copper", "S": "jugcraft:iron_shaft",
                                             "C": "jugcraft:copper_cable"}, 1),
    "magnet_dynamo": (["PMP", "MDM", "PWP"], {"P": "#c:plates/aluminum", "M": "jugcraft:neodymium_magnet",
                                              "D": "jugcraft:dynamo", "W": "jugcraft:aluminum_cable"}, 1),
    "magnet_motor": (["PMP", "MEM", "PWP"], {"P": "#c:plates/aluminum", "M": "jugcraft:neodymium_magnet",
                                             "E": "jugcraft:electric_motor", "W": "jugcraft:aluminum_cable"}, 1),
    # Conveyors: leather belts over iron plates and a shaft; the splitter adds bronze gears and a brass plate.
    "conveyor": (["BBB", "PSP"], {"B": "jugcraft:belt", "P": "#c:plates/iron", "S": "jugcraft:iron_shaft"}, 6),
    "conveyor_slope": ([" C", "CP"], {"C": "jugcraft:conveyor", "P": "#c:plates/iron"}, 2),
    "conveyor_splitter": ([" R ", "GCG"], {"R": "#c:plates/brass", "G": "#c:gears/bronze", "C": "jugcraft:conveyor"}, 1),
    # Powered tools (steel tier, dieselpunk): tungsten cutting edges, steel bodies, an advanced circuit each.
    "mining_drill": ([" T ", "SGS", "ALS"], {"T": "#c:plates/tungsten", "S": "#c:plates/steel", "G": "#c:gears/steel",
                                            "A": "jugcraft:advanced_circuit", "L": "#c:ingots/lead"}, 1),
    "chainsaw": (["TTT", "SGS", "ALS"], {"T": "#c:plates/tungsten", "S": "#c:plates/steel", "G": "#c:gears/steel",
                                        "A": "jugcraft:advanced_circuit", "L": "#c:ingots/lead"}, 1),
    "rocket_pack": (["SAS", "FLF", "N N"], {"S": "#c:plates/steel", "A": "jugcraft:advanced_circuit",
                                           "F": "jugcraft:fluid_tank", "L": "minecraft:leather",
                                           "N": "#c:plates/tungsten"}, 1),
    # Upgrade modules: an advanced circuit on a steel card, with what the upgrade is about.
    "overclock_module": (["SRS", "RAR", "SRS"], {"S": "#c:plates/steel", "R": "minecraft:redstone_block",
                                               "A": "jugcraft:advanced_circuit"}, 1),
    "range_module": (["STS", "TAT", "STS"], {"S": "#c:plates/steel", "T": "#c:plates/tungsten",
                                           "A": "jugcraft:advanced_circuit"}, 1),
    "capacity_module": (["SLS", "LAL", "SLS"], {"S": "#c:plates/steel", "L": "#c:ingots/lead",
                                              "A": "jugcraft:advanced_circuit"}, 1),
    "silk_touch_module": (["SES", "EAE", "SES"], {"S": "#c:plates/steel", "E": "minecraft:emerald",
                                                "A": "jugcraft:advanced_circuit"}, 1),
    "fortune_module": (["SLS", "GAG", "SLS"], {"S": "#c:plates/steel", "L": "minecraft:lapis_block",
                                             "G": "minecraft:gold_ingot", "A": "jugcraft:advanced_circuit"}, 1),
    # Petrochemistry: steel frame and gears around an electric pump; after the steel tier.
    "pumpjack": (["PPP", "GUG", "PMP"], {"P": "#c:plates/steel", "G": "#c:gears/steel", "U": "jugcraft:electric_pump",
                                         "M": "jugcraft:machine_casing"}, 1),
    "oil_sand_extractor": (["PHP", "TMT", "PGP"], {"P": "#c:plates/steel", "H": "minecraft:hopper",
                                                   "T": "jugcraft:fluid_tank", "M": "jugcraft:machine_casing",
                                                   "G": "#c:gears/steel"}, 1),
    "distillation_tower": (["PTP", "PCP", "PFP"], {"P": "#c:plates/steel", "T": "jugcraft:steel_tank",
                                                   "C": "jugcraft:advanced_circuit", "F": "minecraft:blast_furnace"}, 1),
    "catalytic_cracker": (["PCP", "TAT", "PMP"], {"P": "#c:plates/steel", "C": "jugcraft:advanced_circuit",
                                                  "T": "jugcraft:steel_tank", "A": "jugcraft:arc_furnace_casing",
                                                  "M": "jugcraft:machine_casing"}, 1),
    "vacuum_distillation_unit": (["PEP", "TCT", "PMP"], {"P": "#c:plates/steel", "E": "jugcraft:heavy_pump",
                                                         "T": "jugcraft:fluid_tank", "C": "jugcraft:advanced_circuit",
                                                         "M": "jugcraft:machine_casing"}, 1),
    "catalytic_reformer": (["PCP", "TFT", "PMP"], {"P": "#c:plates/steel", "C": "jugcraft:advanced_circuit",
                                                   "T": "jugcraft:fluid_tank", "F": "minecraft:blast_furnace",
                                                   "M": "jugcraft:machine_casing"}, 1),
    "chemical_mixer": (["PMP", "TCT", "PHP"], {"P": "#c:plates/steel", "M": "jugcraft:electric_motor",
                                               "T": "jugcraft:fluid_tank", "C": "jugcraft:machine_casing",
                                               "H": "minecraft:hopper"}, 1),
    "fracking_rig": (["SDS", "UCU", "SMS"], {"S": "#c:plates/steel", "D": "jugcraft:ore_drill",
                                             "U": "jugcraft:heavy_pump", "C": "jugcraft:advanced_circuit",
                                             "M": "jugcraft:machine_casing"}, 1),
    "flowback_treatment_unit": (["PFP", "TST", "PMP"], {"P": "#c:plates/steel", "F": "minecraft:iron_bars",
                                                        "T": "jugcraft:fluid_tank", "S": "jugcraft:sieve",
                                                        "M": "jugcraft:machine_casing"}, 1),
    "diesel_generator": (["PEP", "TMT", "PGP"], {"P": "#c:plates/steel", "E": "jugcraft:electric_motor",
                                                 "T": "jugcraft:fluid_tank", "M": "jugcraft:machine_casing",
                                                 "G": "#c:gears/steel"}, 1),
    "gas_turbine": (["PBP", "DCD", "PGP"], {"P": "#c:plates/steel", "B": "minecraft:iron_bars",
                                            "D": "jugcraft:diesel_generator", "C": "jugcraft:advanced_circuit",
                                            "G": "#c:gears/steel"}, 1),
    "electrolytic_cell": (["PWP", "TCT", "PMP"], {"P": "#c:plates/steel", "W": "jugcraft:aluminum_cable",
                                                  "T": "jugcraft:steel_tank", "C": "jugcraft:advanced_circuit",
                                                  "M": "jugcraft:machine_casing"}, 1),
    "air_separation_unit": (["SPS", "TMT", "ECE"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe",
                                                    "T": "jugcraft:fluid_tank", "M": "jugcraft:machine_casing",
                                                    "E": "jugcraft:electric_motor", "C": "jugcraft:advanced_circuit"}, 1),
    "synthesis_converter": (["STS", "PMP", "SCS"], {"S": "#c:plates/steel", "T": "#c:ingots/titanium",
                                                    "P": "jugcraft:steel_fluid_pipe", "M": "jugcraft:machine_casing",
                                                    "C": "jugcraft:advanced_circuit"}, 1),
    "chemical_reactor": (["PGP", "TCT", "PLP"], {"P": "#c:plates/steel", "G": "minecraft:glass",
                                                 "T": "jugcraft:fluid_tank", "C": "jugcraft:machine_casing",
                                                 "L": "#c:ingots/lead"}, 1),
    "fuel_cell": (["PWP", "SCS", "PTP"], {"P": "#c:plates/aluminum", "W": "jugcraft:aluminum_cable",
                                          "S": "#c:plates/steel", "C": "jugcraft:advanced_circuit",
                                          "T": "jugcraft:fluid_tank"}, 1),
    "crystal_grower": (["GTG", "GHG", "PCP"], {"G": "minecraft:glass", "T": "#c:ingots/titanium",
                                               "H": "jugcraft:arc_furnace_casing", "P": "#c:plates/aluminum",
                                               "C": "jugcraft:advanced_circuit"}, 1),
    "lithography_station": (["GSG", "TCT", "PMP"], {"G": "minecraft:glass", "S": "minecraft:redstone_lamp",
                                                    "T": "#c:ingots/titanium", "C": "jugcraft:advanced_circuit",
                                                    "P": "#c:plates/aluminum", "M": "jugcraft:machine_casing"}, 1),
    "network_terminal": (["GSG", "PXP", "WKW"], {"G": "minecraft:glass_pane", "S": "jugcraft:processor",
                                                 "P": "jugcraft:plastic_sheet", "X": "minecraft:redstone",
                                                 "W": "jugcraft:copper_cable", "K": "minecraft:stone_button"}, 1),
    "lithium_cell": (["PLP", "LWL", "PLP"], {"P": "#c:plates/aluminum", "L": "jugcraft:lithium_carbonate",
                                             "W": "#c:wires/copper"}, 2),
    "lithium_battery_bank": (["TCT", "CBC", "TCT"], {"T": "#c:ingots/titanium", "C": "jugcraft:lithium_cell",
                                                     "B": "jugcraft:capacitor_bank"}, 1),
    # Two electrolyte tanks of borosilicate glass and steel either side of a cell stack (batch 17).
    "flow_battery": (["GXG", "TBT", "GPG"], {"G": "jugcraft:borosilicate_glass", "X": "jugcraft:processor",
                                             "T": "jugcraft:steel_tank", "B": "jugcraft:capacitor_bank",
                                             "P": "#c:plates/steel"}, 1),
    "diesel_engine": (["PXP", "GCG", "PXP"], {"P": "#c:plates/steel", "X": "jugcraft:plastic_sheet",
                                              "G": "#c:gears/steel", "C": "jugcraft:machine_casing"}, 1),
    "polymerization_reactor": (["PCP", "TGT", "PMP"], {"P": "#c:plates/steel", "C": "jugcraft:cracking_catalyst",
                                                       "T": "jugcraft:steel_tank", "G": "minecraft:glass",
                                                       "M": "jugcraft:machine_casing"}, 1),
    # A steel plate faced with rubber, cut into four gaskets (batch 14).
    "gasket": (["RPR"], {"R": "jugcraft:rubber", "P": "#c:plates/steel"}, 4),
    "cracking_catalyst": (["BSB", "SNS", "BSB"], {"B": "jugcraft:bauxite", "S": "minecraft:sand",
                                                  "N": "#c:ingots/nickel"}, 4),
    "charging_station": (["SLS", "WAW", "SBS"], {"S": "#c:plates/steel", "L": "minecraft:redstone_lamp",
                                                "W": "jugcraft:copper_cable", "A": "jugcraft:advanced_circuit",
                                                "B": "jugcraft:battery_box"}, 1),
    "dynamo": (["CRC", "RSR", "CRC"], {"C": "#c:ingots/copper", "R": "minecraft:redstone", "S": "jugcraft:iron_shaft"}, 1),
    # Renewables: the water wheel and cobblestone generator are early (bronze); the tree farm needs a circuit.
    "cobblestone_generator": (["BWB", "CMC", "BLB"], {"B": "#c:ingots/bronze", "W": "minecraft:water_bucket",
                                                   "C": "jugcraft:copper_cable", "M": "jugcraft:machine_casing",
                                                   "L": "minecraft:lava_bucket"}, 1),
    "water_wheel": (["PSP", "SGS", "PCP"], {"P": "#minecraft:planks", "S": "minecraft:stick", "G": "#c:gears/bronze",
                                             "C": "jugcraft:copper_cable"}, 1),
    "tree_farm": (["GLG", "DMD", "BCB"], {"G": "minecraft:glass", "L": "minecraft:glowstone", "D": "minecraft:dirt",
                                         "M": "jugcraft:machine_casing", "B": "#c:ingots/bronze",
                                         "C": "jugcraft:basic_circuit"}, 1),
    # Mining: steel frame, a pulverizer-grade drill head and a circuit; after the steel tier.
    # Early: iron and bronze, a basic circuit and two iron pickaxes for the drill heads.
    "deposit_drill": (["PGP", "KMK", "PCP"], {"P": "#c:plates/iron", "G": "#c:gears/bronze",
                                              "K": "minecraft:iron_pickaxe", "M": "jugcraft:machine_casing",
                                              "C": "jugcraft:basic_circuit"}, 1),
    "ore_drill": (["SGS", "CMC", "SDS"], {"S": "#c:plates/steel", "G": "#c:gears/steel", "C": "jugcraft:basic_circuit",
                                         "M": "jugcraft:machine_casing", "D": "minecraft:diamond_pickaxe"}, 1),
    "arc_furnace_casing": (["KNK", "N N", "KNK"], {"K": "minecraft:bricks", "N": "#c:ingots/nickel"}, 8),
    "arc_furnace_controller": (["NCN", "RMR", "NFN"],
                               {"N": "#c:ingots/nickel", "C": "jugcraft:copper_cable", "R": "minecraft:redstone",
                                "M": "jugcraft:machine_casing", "F": "minecraft:blast_furnace"}, 1),
}


def _crusher():
    from materials import METALS, MINERALS
    recipes = []
    for metal, info in METALS.items():
        if info["mined"]:
            for ore in (f"jugcraft:{metal}_ore", f"jugcraft:deepslate_{metal}_ore"):
                recipes.append({"input": ore, "output": f"jugcraft:raw_{metal}", "count": ORE_PROCESSING_MULTIPLIER,
                                "ticks": 160, "features": [FEATURE, info["feature"]], "ore": True})
    for vanilla in ("iron", "gold"):
        for ore in (f"minecraft:{vanilla}_ore", f"minecraft:deepslate_{vanilla}_ore"):
            recipes.append({"input": ore, "output": f"minecraft:raw_{vanilla}", "count": ORE_PROCESSING_MULTIPLIER,
                            "ticks": 160, "features": [FEATURE], "ore": True})
    for mineral, info in MINERALS.items():
        for ore in (f"jugcraft:{mineral}_ore", f"jugcraft:deepslate_{mineral}_ore"):
            recipes.append({"input": ore, "output": f"jugcraft:{mineral}", "count": info["drops"][1] + 1,
                            "ticks": 160, "features": [FEATURE, info["feature"]], "ore": True})
    recipes += [
        {"input": "minecraft:sulfur", "output": "jugcraft:sulfur_dust", "count": 6, "ticks": 100,
         "features": [FEATURE, "sulfur"]},
        {"input": "jugcraft:oil_sand", "output": "jugcraft:bitumen", "count": 3, "ticks": 120,
         "features": [FEATURE, "crude_oil"]},
        {"input": "minecraft:cobblestone", "output": "minecraft:gravel", "count": 1, "ticks": 100,
         "features": [FEATURE]},
        {"input": "minecraft:gravel", "output": "minecraft:sand", "count": 1, "ticks": 100, "features": [FEATURE]},
    ]
    return recipes


# Arc furnace: the high-temperature processing the blast-furnace stand-ins approximate.
ARC_FURNACE = [
    {"input": "minecraft:quartz", "output": "jugcraft:silicon", "count": 2, "ticks": 200, "features": [FEATURE, "silicon"]},
    {"input": "jugcraft:bauxite", "output": "jugcraft:aluminum_ingot", "count": 1, "ticks": 200,
     "features": [FEATURE, "aluminum"]},
    {"input": "jugcraft:raw_tungsten", "output": "jugcraft:tungsten_ingot", "count": 1, "ticks": 120,
     "features": [FEATURE, "tungsten"]},
    {"input": "jugcraft:raw_nickel", "output": "jugcraft:nickel_ingot", "count": 1, "ticks": 120,
     "features": [FEATURE, "nickel"]},
    {"input": "jugcraft:raw_uranium", "output": "jugcraft:uranium_ingot", "count": 1, "ticks": 120,
     "features": [FEATURE, "uranium"]},
    # Kroll-process titanium sponge (chemical reactor) melts into ingots; raw titanium itself never does.
    {"input": "jugcraft:titanium_sponge", "output": "jugcraft:titanium_ingot", "count": 1, "ticks": 160,
     "features": [FEATURE, "titanium"]},
    {"input": "jugcraft:lepidolite", "output": "jugcraft:lithium_carbonate", "count": 2, "ticks": 160,
     "features": [FEATURE, "lithium"]},
    {"input": "jugcraft:monazite", "output": "jugcraft:rare_earth_oxide", "count": 2, "ticks": 200,
     "features": [FEATURE, "rare_earths"]},
]


# Alloy smelter: two ingredient stacks (any order) -> alloy. Ratios conserve metal exactly.
ALLOY_SMELTER = [
    {"inputs": [["minecraft:copper_ingot", 3], ["jugcraft:tin_ingot", 1]], "output": "jugcraft:bronze_ingot",
     "count": 4, "ticks": 200, "features": [FEATURE, "tin"]},
    {"inputs": [["minecraft:copper_ingot", 3], ["jugcraft:zinc_ingot", 1]], "output": "jugcraft:brass_ingot",
     "count": 4, "ticks": 200, "features": [FEATURE, "zinc"]},
    {"inputs": [["minecraft:iron_ingot", 2], ["jugcraft:nickel_ingot", 1]], "output": "jugcraft:invar_ingot",
     "count": 3, "ticks": 240, "features": [FEATURE, "nickel"]},
    {"inputs": [["jugcraft:tin_ingot", 1], ["jugcraft:lead_ingot", 1]], "output": "jugcraft:solder_ingot",
     "count": 2, "ticks": 120, "features": [FEATURE, "tin", "lead"]},
    # Batch 6: rare earths alloyed with iron (and boron, left out) make neodymium magnets.
    {"inputs": [["jugcraft:rare_earth_oxide", 1], ["minecraft:iron_ingot", 1]], "output": "jugcraft:neodymium_magnet",
     "count": 1, "ticks": 200, "features": [FEATURE, "rare_earths"]},
    # Batch 16: with the boron put back, NdFeB magnets come two to a rare earth. Iron and borax first make
    # ferroboron, the real master alloy.
    {"inputs": [["minecraft:iron_ingot", 1], ["jugcraft:borax", 1]], "output": "jugcraft:ferroboron",
     "count": 1, "ticks": 160, "features": [FEATURE, "silicon"]},
    {"name": "neodymium_magnet_from_ferroboron",
     "inputs": [["jugcraft:rare_earth_oxide", 1], ["jugcraft:ferroboron", 1]],
     "output": "jugcraft:neodymium_magnet", "count": 2, "ticks": 200, "features": [FEATURE, "rare_earths", "silicon"]},
    # Borosilicate glass: sand melted with borax, tougher than plain glass and clear for optics.
    {"inputs": [["minecraft:sand", 2], ["jugcraft:borax", 1]], "output": "jugcraft:borosilicate_glass",
     "count": 2, "ticks": 160, "features": [FEATURE, "silicon"]},
]


def _metal_press():
    from materials import COMPONENTS, ingot_id
    return [{"input": ingot_id(metal), "output": f"jugcraft:{metal}_plate", "count": 1, "ticks": 100,
             "features": [FEATURE]} for metal in COMPONENTS["plate"]] + [
        # Petrochemistry: plastic pellets from the polymerization reactor are pressed into sheets.
        {"input": "jugcraft:plastic_pellets", "output": "jugcraft:plastic_sheet", "count": 1, "ticks": 60,
         "features": [FEATURE, "crude_oil"]},
        # PVC resin (batch 15) presses into two sheets: chlorine stretches the plastic.
        {"input": "jugcraft:pvc_resin", "output": "jugcraft:plastic_sheet", "count": 2, "ticks": 60,
         "features": [FEATURE, "crude_oil", "salt"]}]


def _wire_drawer():
    from materials import COMPONENTS, ingot_id
    return [{"input": ingot_id(metal), "output": f"jugcraft:{metal}_wire", "count": 3, "ticks": 100,
             "features": [FEATURE]} for metal in COMPONENTS["wire"]] + [
        # Glass chemistry (batch 16): borosilicate glass drawn into fibre.
        {"input": "jugcraft:borosilicate_glass", "output": "jugcraft:optical_fibre", "count": 4, "ticks": 100,
         "features": [FEATURE, "silicon"]}]


# Circuit assembler: up to three ingredient stacks, any slot order.
CIRCUIT_ASSEMBLER = [
    {"inputs": [["jugcraft:silicon", 1], ["jugcraft:copper_wire", 3], ["jugcraft:solder_ingot", 1]],
     "output": "jugcraft:basic_circuit", "count": 1, "ticks": 200, "features": [FEATURE, "silicon", "lead"]},
    {"inputs": [["jugcraft:basic_circuit", 2], ["jugcraft:silver_wire", 3], ["jugcraft:invar_plate", 1]],
     "output": "jugcraft:advanced_circuit", "count": 1, "ticks": 300, "features": [FEATURE, "silver", "nickel"]},
    # Electronics (batch 7): four microchips bonded to an advanced circuit with gold.
    {"inputs": [["jugcraft:microchip", 4], ["jugcraft:advanced_circuit", 1], ["minecraft:gold_ingot", 1]],
     "output": "jugcraft:processor", "count": 1, "ticks": 400, "features": [FEATURE, "silicon", "sulfur", "silver", "nickel"]},
    # Batch 16: optical fibre carries the signals instead of gold bond wires.
    {"name": "processor_with_fibre",
     "inputs": [["jugcraft:microchip", 4], ["jugcraft:advanced_circuit", 1], ["jugcraft:optical_fibre", 2]],
     "output": "jugcraft:processor", "count": 1, "ticks": 400, "features": [FEATURE, "silicon", "sulfur", "silver", "nickel"]},
]


def _metal_features(*metals):
    from materials import METALS
    return sorted({METALS[m]["feature"] for m in metals if m in METALS})


def _byproduct(metal, washed=False):
    """[item, count, chance, feature or None] for grinding this metal's ore."""
    from materials import COMPONENTS, METALS
    other, chance = BYPRODUCTS[metal]
    assert other in COMPONENTS["dust"]
    return [f"jugcraft:{other}_dust", 1, chance, METALS[other]["feature"] if other in METALS else None]


def _pulverizer():
    """Ore -> 2 dust (like the crusher, plus a byproduct); washed ore, raw metal and ingots -> 1 dust."""
    from materials import COMPONENTS, METALS, ore_ids, raw_id, ingot_id
    recipes = []
    for metal in COMPONENTS["dust"]:
        features = [FEATURE] + _metal_features(metal)
        dust = f"jugcraft:{metal}_dust"
        for ore in ore_ids(metal):
            recipes.append({"input": ore, "output": dust, "count": ORE_PROCESSING_MULTIPLIER, "ticks": 200,
                            "features": features, "ore": True, "byproducts": [_byproduct(metal)]})
        recipes.append({"input": f"jugcraft:washed_{metal}_ore", "output": dust, "count": 1, "ticks": 100,
                        "features": features, "byproducts": [_byproduct(metal)]})
        recipes.append({"input": raw_id(metal), "output": dust, "count": 1, "ticks": 100, "features": features})
        recipes.append({"input": ingot_id(metal), "output": dust, "count": 1, "ticks": 100, "features": features})
    return recipes


def _ore_washer():
    """Ore + 500 mB water -> 3 washed ore."""
    from materials import WASHED_ORES, ore_ids
    return [{"input": ore, "output": f"jugcraft:washed_{metal}_ore", "count": ORE_WASHING_MULTIPLIER, "ticks": 200,
             "features": [FEATURE] + _metal_features(metal), "ore_bonus": ORE_WASHING_MULTIPLIER}
            for metal in WASHED_ORES for ore in ore_ids(metal)]


# Sieve: gravel and soul sand, with small renewable finds.
SIEVE = [
    # Farming (batch 9): sifting coarse dirt turns up cotton seeds now and then.
    {"input": "minecraft:coarse_dirt", "output": "minecraft:dirt", "count": 1, "ticks": 100, "features": [FEATURE],
     "renewable": True, "byproducts": [["jugcraft:cotton_seeds", 1, 0.15, None], ["minecraft:wheat_seeds", 1, 0.1, None]]},
    {"input": "minecraft:gravel", "output": "minecraft:flint", "count": 1, "ticks": 100, "features": [FEATURE],
     "renewable": True, "byproducts": [["minecraft:iron_nugget", 1, 0.12, None], ["jugcraft:tin_nugget", 1, 0.08, "tin"]]},
    {"input": "minecraft:soul_sand", "output": "minecraft:soul_soil", "count": 1, "ticks": 100, "features": [FEATURE],
     "renewable": True, "byproducts": [["minecraft:quartz", 1, 0.15, None], ["minecraft:gold_nugget", 1, 0.08, None]]},
]


def _sawmill():
    """Logs -> 6 planks (4 by hand) with sawdust; planks -> 3 sticks (2 by hand)."""
    recipes = []
    for wood, tag in WOODS.items():
        planks = 3 if wood == "bamboo" else 6
        recipes.append({"input": f"#minecraft:{tag}", "output": f"minecraft:{wood}_planks", "count": planks, "ticks": 100,
                        "features": [FEATURE], "byproducts": [["jugcraft:sawdust", 1, 0.5, None]]})
    recipes.append({"input": "#minecraft:planks", "output": "minecraft:stick", "count": 3, "ticks": 60,
                    "features": [FEATURE]})
    # Electronics (batch 7): a wire saw slices a silicon boule into wafers.
    recipes.append({"input": "jugcraft:silicon_boule", "output": "jugcraft:silicon_wafer", "count": 8, "ticks": 200,
                    "features": [FEATURE, "silicon"]})
    return recipes


# Crystal grower (batch 7): polysilicon melted with a phosphorus dopant and pulled into a single crystal.
CRYSTAL_GROWER = [
    {"inputs": [["jugcraft:silicon", 4], ["jugcraft:phosphate", 1]], "output": "jugcraft:silicon_boule",
     "count": 1, "ticks": 400, "features": [FEATURE, "silicon", "phosphate"]},
]


# Coke oven: coal baked slowly into coke (no power, no fuel).
COKE_OVEN = [
    {"input": "minecraft:coal", "output": "jugcraft:coke", "count": 1, "ticks": 600, "features": [FEATURE]},
]

# Steel foundry: one iron ingot and one coke make one steel ingot (iron's metal is kept; coke is carbon).
STEEL_FOUNDRY = [
    {"inputs": [["#c:ingots/iron", 1], ["jugcraft:coke", 1]], "output": "jugcraft:steel_ingot", "count": 1,
     "ticks": 400, "features": [FEATURE]},
]


# Tree farm: a sapling grows into logs in 400 ticks and comes back, with a chance of the tree's extras.
TREES = {"oak": ("oak_sapling", "minecraft:apple"), "spruce": ("spruce_sapling", "minecraft:stick"),
         "birch": ("birch_sapling", "minecraft:stick"), "jungle": ("jungle_sapling", "minecraft:cocoa_beans"),
         "acacia": ("acacia_sapling", "minecraft:stick"), "dark_oak": ("dark_oak_sapling", "minecraft:apple"),
         "cherry": ("cherry_sapling", "minecraft:pink_petals"), "mangrove": ("mangrove_propagule", "minecraft:stick"),
         "pale_oak": ("pale_oak_sapling", "minecraft:pale_moss_carpet")}


def _tree_farm():
    return [{"input": f"minecraft:{sapling}", "output": f"minecraft:{wood}_log", "count": 6, "ticks": 400,
             "features": [FEATURE], "renewable": True,
             "byproducts": [[f"minecraft:{sapling}", 1, 1.0, None], [extra, 1, 0.1, None]]}
            for wood, (sapling, extra) in TREES.items()]


def machine_recipes():
    return {"crusher": _crusher(), "arc_furnace": ARC_FURNACE + _arc_dusts(), "alloy_smelter": ALLOY_SMELTER,
            "metal_press": _metal_press(), "wire_drawer": _wire_drawer(), "circuit_assembler": CIRCUIT_ASSEMBLER,
            "pulverizer": _pulverizer(), "ore_washer": _ore_washer(), "sieve": SIEVE, "sawmill": _sawmill(),
            "coke_oven": COKE_OVEN, "steel_foundry": STEEL_FOUNDRY, "tree_farm": _tree_farm(),
            "crystal_grower": CRYSTAL_GROWER}


def _arc_dusts():
    """Dusts of metals a plain furnace cannot smelt (nickel, tungsten, uranium) melt in the arc furnace. (Titanium has
    no dust: only the Kroll process frees it.)"""
    from materials import COMPONENTS, METALS
    return [{"input": f"jugcraft:{metal}_dust", "output": f"jugcraft:{metal}_ingot", "count": 1, "ticks": 80,
             "features": [FEATURE, info["feature"]]}
            for metal, info in METALS.items()
            if info["mined"] and "smelting" not in info["cook"] and metal in COMPONENTS["dust"]]


def machine_blocks():
    from drones import drone_blocks
    from tower import tower_blocks
    return (list(MACHINES) + list(PARTS) + list(CABLES) + list(PIPES) + list(FLUID_BLOCKS)
            + list(ITEM_PIPES) + list(LOGISTICS_BLOCKS) + list(STORAGE_BLOCKS) + list(KINETIC_BLOCKS) + list(TOOL_BLOCKS)
            + list(SLOPE_BLOCKS) + list(ELECTRONICS_BLOCKS) + list(FARMING_BLOCKS) + list(CROPS)
            + drone_blocks() + tower_blocks() + list(__import__('blueprints').STAKE))


def machine_items():
    """Items of the machine feature that are not blocks (tools and upgrades)."""
    from drones import drone_items
    from tower import tower_items
    return (list(TOOLS) + list(UPGRADES) + list(POWERED_TOOLS) + list(UPGRADE_MODULES)
            + [info[key] for info in CROPS.values() for key in ("seeds", "product")]
            + drone_items() + tower_items() + list(__import__('blueprints').ITEMS))
