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
}

# Plain crafting-component / structure blocks.
PARTS = {
    "machine_casing": "Machine Casing",
    "arc_furnace_casing": "Arc Furnace Casing",
}

# Cables: display name and JE per tick one push may send through the network.
CABLES = {"copper_cable": {"display": "Copper Cable", "rate": 256}}

# Balance numbers shared with the Java code (MachineStats.java). Keep in sync.
STATS = {
    "coal_generator": {"capacity": 16_000, "output_per_tick": 64, "generation_per_tick": 32},
    "battery_box": {"capacity": 400_000, "io_per_tick": 256},
    "electric_furnace": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 10, "ticks": 100},
    "crusher": {"capacity": 10_000, "input_per_tick": 128, "use_per_tick": 16},
    "arc_furnace_controller": {"capacity": 50_000, "input_per_tick": 512, "use_per_tick": 64},
}

# Fuel for the coal generator: burn ticks per item (coal matches the vanilla furnace's 1600).
GENERATOR_FUELS = {"minecraft:coal": 1600, "minecraft:charcoal": 1600, "minecraft:coal_block": 16000}

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


def machine_recipes():
    return {"crusher": _crusher(), "arc_furnace": ARC_FURNACE}


def machine_blocks():
    return list(MACHINES) + list(PARTS) + list(CABLES)
