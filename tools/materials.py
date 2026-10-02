"""Single source of truth for Jugcraft material data (IDs, recipes, tags, worldgen).

generate_material_data.py writes the JSON resources from this table, and
check_mod_data.py verifies them. Keep it in sync with materials/JugcraftMaterials.java.

Real-world processing (electrolysis, arc furnaces, solvent extraction, oil refining)
needs machines that do not exist yet. Until they do, the hard steps use the vanilla
blast furnace as a clearly marked stand-in, and liquid crude oil is not added.
"""

MOD = "jugcraft"

# Feature switches (config/jugcraft.properties). Order is the config file order.
FEATURES = ["tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium", "titanium", "aluminum",
            "salt", "phosphate", "lithium", "rare_earths", "sulfur", "silicon", "crude_oil", "machines", "deposits",
            "explosives"]

# Metals use the vanilla form set. "mined" adds ore, deepslate ore, raw item and raw block.
# cook: which vanilla cookers can smelt the ore/raw form. tool: minimum pickaxe tier.
METALS = {
    "tin": {"mined": True, "display": "Tin", "feature": "tin", "cook": ["smelting", "blasting"], "xp": 0.7,
            "tool": "stone", "gen": {"size": 9, "count": 8, "min_y": -32, "max_y": 96}},
    "zinc": {"mined": True, "display": "Zinc", "feature": "zinc", "cook": ["smelting", "blasting"], "xp": 0.7,
             "tool": "stone", "gen": {"size": 9, "count": 7, "min_y": -16, "max_y": 80}},
    "lead": {"mined": True, "display": "Lead", "feature": "lead", "cook": ["smelting", "blasting"], "xp": 0.7,
             "tool": "stone", "gen": {"size": 9, "count": 6, "min_y": -48, "max_y": 64}},
    "silver": {"mined": True, "display": "Silver", "feature": "silver", "cook": ["smelting", "blasting"], "xp": 1.0,
               "tool": "iron", "gen": {"size": 6, "count": 3, "min_y": -64, "max_y": 32}},
    "nickel": {"mined": True, "display": "Nickel", "feature": "nickel", "cook": ["blasting"], "xp": 0.7,
               "tool": "iron", "gen": {"size": 7, "count": 4, "min_y": -64, "max_y": 16}},
    "tungsten": {"mined": True, "display": "Tungsten", "feature": "tungsten", "cook": ["blasting"], "xp": 1.0,
                 "tool": "iron", "gen": {"size": 5, "count": 2, "min_y": -64, "max_y": 0}},
    "uranium": {"mined": True, "display": "Uranium", "feature": "uranium", "cook": ["blasting"], "xp": 1.0,
                "tool": "iron", "gen": {"size": 4, "count": 1, "min_y": -64, "max_y": -16}},
    # Rutile-bearing titanium ore: no furnace smelts it; only the Kroll process (chlorine, chemistry batch 6) does.
    "titanium": {"mined": True, "display": "Titanium", "feature": "titanium", "cook": [], "xp": 1.0,
                 "tool": "iron", "gen": {"size": 5, "count": 2, "min_y": -64, "max_y": -8}},
    "bronze": {"mined": False, "display": "Bronze", "feature": "tin", "extras": ["bronze_blend"]},
    "aluminum": {"mined": False, "display": "Aluminum", "feature": "aluminum"},
    # Alloys made only in the alloy smelter (tools/machines.py ALLOY_SMELTER).
    "brass": {"mined": False, "display": "Brass", "feature": "zinc"},
    "invar": {"mined": False, "display": "Invar", "feature": "nickel"},
    "solder": {"mined": False, "display": "Solder", "feature": "lead"},
    # Iron refined with coke in the steel foundry (tools/machines.py STEEL_FOUNDRY): the second tier.
    "steel": {"mined": False, "display": "Steel", "feature": "machines"},
}

# Non-metal ores: <name>_ore, deepslate_<name>_ore, item <name>, storage block <name>_block.
MINERALS = {
    "salt": {"display": "Salt", "ore_display": "Rock Salt", "feature": "salt", "drops": [2, 4], "tool": "stone",
             "gen": {"size": 12, "count": 4, "min_y": 0, "max_y": 64}},
    "phosphate": {"display": "Phosphate", "ore_display": "Phosphorite", "feature": "phosphate", "drops": [1, 3],
                  "tool": "stone", "gen": {"size": 10, "count": 4, "min_y": -16, "max_y": 48}},
    "lepidolite": {"display": "Lepidolite", "ore_display": "Lepidolite", "feature": "lithium", "drops": [1, 2],
                   "tool": "iron", "gen": {"size": 7, "count": 3, "min_y": -48, "max_y": 32}},
    "monazite": {"display": "Monazite", "ore_display": "Monazite", "feature": "rare_earths", "drops": [1, 1],
                 "tool": "iron", "gen": {"size": 5, "count": 2, "min_y": -64, "max_y": 16}},
}

# Surface rocks with their own blocks. copy: vanilla block whose properties are copied.
ROCKS = {
    "bauxite": {"display": "Bauxite", "feature": "aluminum", "copy": "GRANITE", "tool": "pickaxe", "drop": None,
                "gen": {"size": 33, "count": 4, "min_y": 50, "max_y": 100, "target": "minecraft:stone_ore_replaceables",
                        "biomes": ["IS_JUNGLE", "IS_SAVANNA", "IS_BADLANDS"]}},
    "oil_sand": {"display": "Oil Sand", "feature": "crude_oil", "copy": "SAND", "tool": "shovel",
                 "drop": {"item": "bitumen", "min": 1, "max": 2},
                 "gen": {"size": 24, "count": 3, "min_y": 50, "max_y": 90, "target": "minecraft:sand",
                         "biomes": ["IS_DESERT", "IS_BADLANDS"]}},
    # Glass chemistry (batch 16): tincal, natural borax, crusts the sand of dry deserts.
    "tincal": {"display": "Tincal", "feature": "silicon", "copy": "SANDSTONE", "tool": "pickaxe",
               "drop": {"item": "borax", "min": 1, "max": 3},
               "gen": {"size": 14, "count": 2, "min_y": 55, "max_y": 100, "target": "minecraft:sand",
                       "biomes": ["IS_DESERT", "IS_BADLANDS"]}},
}

# Plain items: display name, feature, optional c: tag path.
ITEMS = {
    "bitumen": {"display": "Bitumen", "feature": "crude_oil", "tag": None},
    "sulfur_dust": {"display": "Sulfur Dust", "feature": "sulfur", "tag": "dusts/sulfur"},
    "silicon": {"display": "Silicon", "feature": "silicon", "tag": "silicon"},
    "lithium_carbonate": {"display": "Lithium Carbonate", "feature": "lithium", "tag": "dusts/lithium_carbonate"},
    "rare_earth_oxide": {"display": "Rare Earth Oxide", "feature": "rare_earths", "tag": "dusts/rare_earth_oxide"},
    # Coal baked in the coke oven: a hotter fuel and the carbon for steel.
    "coke": {"display": "Coal Coke", "feature": "machines", "tag": "coal_coke"},
    # Glass chemistry (batch 16): borax, glass made with it, and fibre drawn from that glass.
    "borax": {"display": "Borax", "feature": "silicon", "tag": "dusts/borax"},
    "borosilicate_glass": {"display": "Borosilicate Glass", "feature": "silicon", "tag": None},
    "optical_fibre": {"display": "Optical Fibre", "feature": "silicon", "tag": None},
    "ferroboron": {"display": "Ferroboron", "feature": "silicon", "tag": None},
}

EXTRA_NAMES = {"bronze_blend": "Bronze Blend"}

# ---------------------------------------------------------------- mechanical parts
# Physical components made by the metal press and wire drawer (mechanical branch).
# Metal content per item in nugget units; the checker uses these to audit recipes.
PART_UNITS = {"plate": 9, "gear": 36, "wire": 3, "dust": 9}
COMPONENTS = {
    "plate": ["copper", "iron", "tin", "bronze", "brass", "invar", "aluminum", "nickel", "lead", "tungsten", "steel",
              "titanium"],
    "gear": ["iron", "bronze", "brass", "invar", "steel"],
    "wire": ["copper", "silver", "aluminum"],
    # Pulverizer output (see tools/machines.py); one dust smelts back into one ingot.
    "dust": ["copper", "iron", "gold", "tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium"],
}
# Ore washer output: washed_<metal>_ore, one ingot's worth each; the pulverizer grinds it into dust.
WASHED_ORES = list(COMPONENTS["dust"])
# Sawmill byproduct.
SAWDUST = "sawdust"
# Assembled electronics (non-metal outputs; their metal is consumed).
CIRCUITS = {"basic_circuit": "Basic Circuit", "advanced_circuit": "Advanced Circuit", "processor": "Processor"}
# Vanilla metals that get Jugcraft parts: nugget units per vanilla ingot.
VANILLA_METALS = {"copper", "iron", "gold"}


def part_items():
    return ([f"{metal}_{form}" for form, metals in COMPONENTS.items() for metal in metals] + list(CIRCUITS)
            + [f"washed_{metal}_ore" for metal in WASHED_ORES] + [SAWDUST])


def ore_ids(metal):
    """The stone and deepslate ore blocks of a mined metal (vanilla ones for copper, iron and gold)."""
    ns = "minecraft" if metal in VANILLA_METALS else MOD
    return [f"{ns}:{metal}_ore", f"{ns}:deepslate_{metal}_ore"]


def raw_id(metal):
    return f"minecraft:raw_{metal}" if metal in VANILLA_METALS else f"{MOD}:raw_{metal}"


def nugget_id(metal):
    return f"minecraft:{metal}_nugget" if metal in ("iron", "gold") else f"{MOD}:{metal}_nugget"


def ingot_id(metal):
    return f"minecraft:{metal}_ingot" if metal in VANILLA_METALS else f"{MOD}:{metal}_ingot"

# Extra c: item tags for mineral items.
MINERAL_TAGS = {"salt": "dusts/salt", "phosphate": "dusts/phosphate"}

# Processing recipes beyond the standard metal/mineral set.
# kind: shapeless | smelting | blasting. Blast-furnace entries marked stand_in replace
# machines that do not exist yet (electrolysis, arc furnace, separation).
PROCESSING = [
    {"id": "bronze_blend", "kind": "shapeless", "feature": "tin",
     "inputs": ["#c:ingots/copper", "#c:ingots/copper", "#c:ingots/copper", "#c:ingots/tin"], "result": "bronze_blend", "count": 4},
    {"id": "bronze_ingot_from_smelting_bronze_blend", "kind": "smelting", "feature": "tin",
     "input": "jugcraft:bronze_blend", "result": "bronze_ingot", "xp": 0.1},
    {"id": "bronze_ingot_from_blasting_bronze_blend", "kind": "blasting", "feature": "tin",
     "input": "jugcraft:bronze_blend", "result": "bronze_ingot", "xp": 0.1},
    {"id": "aluminum_nugget_from_blasting_bauxite", "kind": "blasting", "feature": "aluminum", "stand_in": True,
     "input": "jugcraft:bauxite", "result": "aluminum_nugget", "xp": 0.2},
    {"id": "sulfur_dust", "kind": "shapeless", "feature": "sulfur",
     "inputs": ["minecraft:sulfur"], "result": "sulfur_dust", "count": 4},
    {"id": "silicon_from_blasting_quartz", "kind": "blasting", "feature": "silicon", "stand_in": True,
     "input": "minecraft:quartz", "result": "silicon", "xp": 0.2},
    {"id": "lithium_carbonate", "kind": "blasting", "feature": "lithium", "stand_in": True,
     "input": "jugcraft:lepidolite", "result": "lithium_carbonate", "xp": 0.2},
    {"id": "rare_earth_oxide", "kind": "blasting", "feature": "rare_earths", "stand_in": True,
     "input": "jugcraft:monazite", "result": "rare_earth_oxide", "xp": 0.3},
]


def metal_blocks(metal):
    out = []
    if METALS[metal]["mined"]:
        out += [f"{metal}_ore", f"deepslate_{metal}_ore", f"raw_{metal}_block"]
    out.append(f"{metal}_block")
    return out


def metal_items(metal):
    info = METALS[metal]
    out = [f"raw_{metal}"] if info["mined"] else []
    out += info.get("extras", [])
    out += [f"{metal}_ingot", f"{metal}_nugget"]
    return out


def mineral_blocks(mineral):
    return [f"{mineral}_ore", f"deepslate_{mineral}_ore", f"{mineral}_block"]


def all_blocks():
    return ([b for m in METALS for b in metal_blocks(m)] + [b for m in MINERALS for b in mineral_blocks(m)]
            + list(ROCKS))


def all_items():
    return [i for m in METALS for i in metal_items(m)] + list(MINERALS) + list(ITEMS) + part_items()


def feature_of(entry_id):
    """The feature switch that owns a registered ID."""
    for metal, info in METALS.items():
        if entry_id in metal_blocks(metal) + metal_items(metal):
            return info["feature"]
    for mineral, info in MINERALS.items():
        if entry_id == mineral or entry_id in mineral_blocks(mineral):
            return info["feature"]
    if entry_id in ROCKS:
        return ROCKS[entry_id]["feature"]
    if entry_id in ITEMS:
        return ITEMS[entry_id]["feature"]
    if entry_id in part_items():
        return "machines"
    from machines import machine_blocks, machine_items, FEATURE
    if entry_id in machine_blocks() or entry_id in machine_items():
        return FEATURE
    import petro
    if entry_id in petro.petro_items() or entry_id in petro.petro_blocks():
        return FEATURE
    import tank_display
    if entry_id in tank_display.BLOCKS:
        return FEATURE
    raise KeyError(entry_id)
