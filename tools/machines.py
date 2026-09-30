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
}

# Plain crafting-component / structure blocks.
PARTS = {
    "machine_casing": "Machine Casing",
    "arc_furnace_casing": "Arc Furnace Casing",
}

# Cables: display name and JE per tick one push may send through the network.
CABLES = {"copper_cable": {"display": "Copper Cable", "rate": 256}}

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
}

# Fuel for the coal generator: burn ticks per item (coal matches the vanilla furnace's 1600).
GENERATOR_FUELS = {"minecraft:coal": 1600, "minecraft:charcoal": 1600, "minecraft:coal_block": 16000}
# The steam generator also burns bitumen from oil sand.
STEAM_FUELS = {**GENERATOR_FUELS, "jugcraft:bitumen": 800}

# One ore block yields this many raw items in the crusher (the central ore-processing bonus).
ORE_PROCESSING_MULTIPLIER = 2

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


def machine_recipes():
    return {"crusher": _crusher(), "arc_furnace": ARC_FURNACE, "alloy_smelter": ALLOY_SMELTER,
            "metal_press": _metal_press(), "wire_drawer": _wire_drawer(), "circuit_assembler": CIRCUIT_ASSEMBLER}


def machine_blocks():
    return list(MACHINES) + list(PARTS) + list(CABLES) + list(PIPES) + list(FLUID_BLOCKS)
