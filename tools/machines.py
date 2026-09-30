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
    # Multi-block machines: models and footprints live in tools/large_machines.py.
    "geothermal_generator": {"display": "Geothermal Generator", "lit": True},
    "wind_turbine": {"display": "Wind Turbine", "lit": False},
}

# Plain crafting-component / structure blocks.
PARTS = {
    "machine_casing": "Machine Casing",
    "arc_furnace_casing": "Arc Furnace Casing",
}

# Cables: display name and JE per tick one push may send through the network.
CABLES = {"copper_cable": {"display": "Copper Cable", "rate": 256}}

# Item logistics (see docs/TECH_TREE.md#item-logistics). The tube is a 6-pixel transmitter; the
# extractor and sorter face any of six directions. Models: tools/logistics_models.py.
ITEM_PIPES = {"brass_item_pipe": {"display": "Brass Item Pipe", "size": 6}}
LOGISTICS_BLOCKS = {
    "pneumatic_extractor": {"display": "Pneumatic Extractor"},
    "item_sorter": {"display": "Item Sorter"},
}
TOOLS = {"brass_wrench": "Brass Wrench", "engineers_handbook": "Engineer's Handbook"}
# Machine upgrades (docs/TECH_TREE.md#machine-control): go in a powered processor's two upgrade slots.
# At most 4 of each kind count. Numbers are in machine/MachineUpgrades.java.
UPGRADES = {"speed_upgrade": "Speed Upgrade", "efficiency_upgrade": "Efficiency Upgrade"}

# Fluid logistics (physical branch; see docs/TECH_TREE.md). Amounts are millibuckets (mB);
# 1 bucket = 1000 mB = 81000 Fabric droplets. Pipes are passive: pumps push through them.
PIPES = {"bronze_fluid_pipe": {"display": "Bronze Fluid Pipe", "rate": 250}}
# Fluid blocks with their own block entities. top/side/bottom name the textures.
FLUID_BLOCKS = {
    "fluid_tank": {"display": "Tinplate Tank"},
    "electric_pump": {"display": "Electric Pump"},
}
FLUID_STATS = {
    # Holds 16 buckets of one fluid; filled and emptied with buckets or by pumps.
    "fluid_tank": {"capacity_mb": 16_000},
    # Draws a water/lava source (or the tank) below it and pushes into pipes/storages on its other sides.
    "electric_pump": {"energy_capacity": 4_000, "input_per_tick": 64, "use_per_tick": 8,
                      "pump_per_tick": 100, "buffer_mb": 4_000},
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
    # Unpowered: heat comes from the charge itself. No battery, no cable connection.
    "coke_oven": {"capacity": 0, "use_per_tick": 0},
    "steel_foundry": {"capacity": 0, "use_per_tick": 0},
    # Two blocks wide. Burns 1 mB of lava per tick for 64 JE: a bucket lasts 1,000 ticks.
    "geothermal_generator": {"capacity": 30_000, "output_per_tick": 128, "generation_per_tick": 64,
                             "lava_per_tick": 1, "tank": 4_000},
    # Three blocks tall. 4 JE/t at sea level, +1 per 4 blocks higher, capped at 24; x1.5 rain, x2 thunder.
    "wind_turbine": {"capacity": 16_000, "output_per_tick": 64, "generation_per_tick": 24},
}

# Fuel for the coal generator: burn ticks per item (coal matches the vanilla furnace's 1600).
GENERATOR_FUELS = {"minecraft:coal": 1600, "minecraft:charcoal": 1600, "minecraft:coal_block": 16000,
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
CRAFTING = {
    "machine_casing": (["BZB", "Z Z", "BZB"], {"B": "#c:ingots/bronze", "Z": "#c:ingots/zinc"}, 1),
    "copper_cable": (["CTC"], {"C": "#c:ingots/copper", "T": "#c:ingots/tin"}, 6),
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
    "item_sorter": (["PCP", "THT", "PPP"], {"P": "#c:plates/brass", "C": "minecraft:comparator",
                                           "T": "jugcraft:brass_item_pipe", "H": "minecraft:hopper"}, 1),
    "brass_wrench": (["B B", " B ", " B "], {"B": "#c:ingots/brass"}, 1),
    # The in-game guide (tools/handbook.py): available from the start.
    "engineers_handbook": (["BC"], {"B": "minecraft:book", "C": "#c:ingots/copper"}, 1),
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
]


def _metal_press():
    from materials import COMPONENTS, ingot_id
    return [{"input": ingot_id(metal), "output": f"jugcraft:{metal}_plate", "count": 1, "ticks": 100,
             "features": [FEATURE]} for metal in COMPONENTS["plate"]]


def _wire_drawer():
    from materials import COMPONENTS, ingot_id
    return [{"input": ingot_id(metal), "output": f"jugcraft:{metal}_wire", "count": 3, "ticks": 100,
             "features": [FEATURE]} for metal in COMPONENTS["wire"]]


# Circuit assembler: up to three ingredient stacks, any slot order.
CIRCUIT_ASSEMBLER = [
    {"inputs": [["jugcraft:silicon", 1], ["jugcraft:copper_wire", 3], ["jugcraft:solder_ingot", 1]],
     "output": "jugcraft:basic_circuit", "count": 1, "ticks": 200, "features": [FEATURE, "silicon", "lead"]},
    {"inputs": [["jugcraft:basic_circuit", 2], ["jugcraft:silver_wire", 3], ["jugcraft:invar_plate", 1]],
     "output": "jugcraft:advanced_circuit", "count": 1, "ticks": 300, "features": [FEATURE, "silver", "nickel"]},
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
    return recipes


# Coke oven: coal baked slowly into coke (no power, no fuel).
COKE_OVEN = [
    {"input": "minecraft:coal", "output": "jugcraft:coke", "count": 1, "ticks": 600, "features": [FEATURE]},
]

# Steel foundry: one iron ingot and one coke make one steel ingot (iron's metal is kept; coke is carbon).
STEEL_FOUNDRY = [
    {"inputs": [["#c:ingots/iron", 1], ["jugcraft:coke", 1]], "output": "jugcraft:steel_ingot", "count": 1,
     "ticks": 400, "features": [FEATURE]},
]


def machine_recipes():
    return {"crusher": _crusher(), "arc_furnace": ARC_FURNACE + _arc_dusts(), "alloy_smelter": ALLOY_SMELTER,
            "metal_press": _metal_press(), "wire_drawer": _wire_drawer(), "circuit_assembler": CIRCUIT_ASSEMBLER,
            "pulverizer": _pulverizer(), "ore_washer": _ore_washer(), "sieve": SIEVE, "sawmill": _sawmill(),
            "coke_oven": COKE_OVEN, "steel_foundry": STEEL_FOUNDRY}


def _arc_dusts():
    """Dusts of metals a plain furnace cannot smelt (nickel, tungsten, uranium) melt in the arc furnace."""
    from materials import METALS
    return [{"input": f"jugcraft:{metal}_dust", "output": f"jugcraft:{metal}_ingot", "count": 1, "ticks": 80,
             "features": [FEATURE, info["feature"]]}
            for metal, info in METALS.items() if info["mined"] and "smelting" not in info["cook"]]


def machine_blocks():
    return (list(MACHINES) + list(PARTS) + list(CABLES) + list(PIPES) + list(FLUID_BLOCKS)
            + list(ITEM_PIPES) + list(LOGISTICS_BLOCKS))


def machine_items():
    """Items of the machine feature that are not blocks (tools and upgrades)."""
    return list(TOOLS) + list(UPGRADES)
