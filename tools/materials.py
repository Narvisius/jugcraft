"""Single source of truth for Jugcraft material data (IDs, recipes, tags, worldgen).

generate_material_data.py writes the JSON resources from this table, and
check_mod_data.py verifies them. Keep it in sync with materials/JugcraftMaterials.java.

Real-world processing (electrolysis, arc furnaces, solvent extraction, oil refining)
needs machines that do not exist yet. Until they do, the hard steps use the vanilla
blast furnace as a clearly marked stand-in, and liquid crude oil is not added.
"""

from mineral_sands import SANDS

MOD = "jugcraft"

# Feature switches (config/jugcraft.properties). Order is the config file order.
FEATURES = ["tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium", "titanium", "thallite", "aluminum",
            "salt", "phosphate", "lithium", "rare_earths", "fluorite", "mineral_sands", "vanadium", "sulfur", "silicon", "crude_oil", "machines", "deposits",
            "explosives", "agriculture", "parties", "drones",
            "pixel_hollows", "retro_trader", "alpine_spawn", "biomes", "town", "diagonal_connections", "raiders",
            "concordance", "guns", "adam_ondi_ahman"]

# Metals use the vanilla form set. "mined" adds ore, deepslate ore, raw item and raw block.
# cook: which vanilla cookers can smelt the ore/raw form. tool: minimum pickaxe tier.
# gen: the ore's veins in every Overworld biome (placed feature ore_<metal>). rich_gen: optional further veins, placed
# feature ore_<metal>_rich. Either may name "biomes" (biome ids or #biome tags): it is then placed only there, through
# the biome tag jugcraft:has_ore/<placed feature name> written from that list (ore_gens() below).
# lore: an optional grey italic line under the ingot's name (tooltip.jugcraft.<metal>_ingot; MetalFamily.Builder.lore).
# hand_plate: optional ingots that make one plate at a crafting table, a dearer route than the Metal Press's one.
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
    # Thallite, the Earth school's green metal (docs/features/thallite.md): everywhere between lead and silver, and rich
    # pockets where roots and water work through old rock, in Lush Caves and the Glowcap Grotto. No alloy, on purpose.
    "thallite": {"mined": True, "display": "Thallite", "feature": "thallite", "cook": ["smelting", "blasting"], "xp": 0.7,
                 "tool": "stone", "gen": {"size": 7, "count": 4, "min_y": -32, "max_y": 48},
                 "rich_gen": {"size": 9, "count": 6, "min_y": -32, "max_y": 48,
                              "biomes": ["minecraft:lush_caves", "jugcraft:glowcap_grotto"]},
                 "lore": "Green as a new shoot.", "hand_plate": 2},
    "bronze": {"mined": False, "display": "Bronze", "feature": "tin", "extras": ["bronze_blend"]},
    "aluminum": {"mined": False, "display": "Aluminum", "feature": "aluminum"},
    # Alloys made only in the alloy smelter (tools/machines.py ALLOY_SMELTER).
    "brass": {"mined": False, "display": "Brass", "feature": "zinc"},
    "invar": {"mined": False, "display": "Invar", "feature": "nickel"},
    "solder": {"mined": False, "display": "Solder", "feature": "lead"},
    # Iron refined with coke in the steel foundry (tools/machines.py STEEL_FOUNDRY): the second tier.
    "steel": {"mined": False, "display": "Steel", "feature": "machines"},
    "ferrochromium": {"mined": False, "display": "Ferrochromium", "feature": "machines"},
    "stainless_steel": {"mined": False, "display": "Stainless Steel", "feature": "nickel"},
    "aluminum_magnesium": {"mined": False, "display": "Aluminum-Magnesium Alloy", "feature": "aluminum"},
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
    "fluorite": {"display": "Fluorite", "ore_display": "Fluorite", "feature": "fluorite", "drops": [1, 1],
                 "tool": "iron", "gen": {"size": 48, "rarity": 8, "min_y": -32, "max_y": 48,
                 "outcrop_chance": 4, "surface_biomes": [
                     "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:ice_spikes", "minecraft:grove",
                     "minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:jagged_peaks",
                     "jugcraft:snowy_coniferous_forest", "jugcraft:snowy_forest", "jugcraft:muskeg",
                     "jugcraft:frostlight_garden", "jugcraft:snowpetal_grove"]}},
    "spodumene": {"display": "Spodumene", "ore_display": "Spodumene", "feature": "lithium", "drops": [1, 1],
                  "tool": "iron", "gen": {"size": 40, "rarity": 12, "min_y": -16, "max_y": 64,
                  "outcrop_chance": 4, "surface_biomes": [
                      "jugcraft:shield", "jugcraft:highland", "jugcraft:basin", "minecraft:windswept_hills",
                      "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:stony_peaks"]}},
    "vanadium_iron": {"display": "Raw Vanadium-bearing Iron", "ore_display": "Vanadium-bearing Iron",
                     "feature": "vanadium", "drops": [1, 1], "fortune": False, "tool": "iron",
                     "gen": {"size": 40, "rarity": 24, "min_y": -48, "max_y": 32,
                             "outcrop_chance": 3, "surface_biomes": [
                                 "jugcraft:highland", "jugcraft:basin", "minecraft:windswept_hills",
                                 "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:stony_peaks"]},
                     "rich_gen": {"size": 48, "rarity": 12, "min_y": -48, "max_y": 32,
                                  "outcrop_chance": 3, "surface_biomes": ["jugcraft:shield", "jugcraft:volcano"],
                                  "outcrop_surface_biomes": ["jugcraft:shield"]}},
}

# Chromite's host province is selected at the surface, so cave biomes do not cut holes
# out of the ore province. Existing chunks are never rewritten.
MINERALS['chromite'] = {
    'display': 'Chromite', 'ore_display': 'Chromite', 'feature': 'machines',
    'drops': [1, 1], 'tool': 'iron',
    'gen': {'size': 32, 'rarity': 10, 'min_y': -32, 'max_y': 64, 'outcrop_chance': 4,
            'surface_biomes': ['minecraft:windswept_hills', 'minecraft:windswept_gravelly_hills',
                              'minecraft:windswept_forest', 'minecraft:stony_peaks',
                              'minecraft:stony_shore', 'jugcraft:shield', 'jugcraft:highland']},
}

# Separate primary cobalt feed, available before synthetic fuel. Surface host province, new chunks only.
MINERALS['cobaltite'] = {
    'display':'Cobaltite', 'ore_display':'Cobaltite', 'feature':'machines', 'drops':[1,1], 'tool':'iron',
    'gen':{'size':24,'rarity':12,'min_y':-48,'max_y':32,'outcrop_chance':4,
           'surface_biomes':['minecraft:windswept_hills','minecraft:windswept_gravelly_hills',
                             'minecraft:windswept_forest','minecraft:stony_peaks','jugcraft:shield','jugcraft:highland']},
}

# Quartz shares the existing vanilla item and storage recipes; only the two host ores are new.
MINERALS['quartz'] = {
    'display':'Quartz', 'ore_display':'Quartz', 'feature':'silicon', 'drops':[1,1], 'tool':'stone',
    'vanilla_item':'minecraft:quartz',
    'gen':{'size':20,'rarity':4,'min_y':-16,'max_y':96,'outcrop_chance':3,'outcrop_max':6,
        'surface_biomes':['jugcraft:highland','jugcraft:shield','jugcraft:basin','minecraft:windswept_hills',
            'minecraft:windswept_gravelly_hills','minecraft:windswept_forest','minecraft:stony_peaks','minecraft:stony_shore']},
    'secondary_gen':{'size':12,'rarity':12,'min_y':-16,'max_y':96,'outcrop_chance':0,
        'surface_biomes':['minecraft:forest','minecraft:birch_forest','minecraft:taiga','minecraft:old_growth_pine_taiga',
            'minecraft:old_growth_spruce_taiga','jugcraft:woodland','jugcraft:coniferous_forest','jugcraft:maple_woods','jugcraft:aspen_glade']},
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

ROCKS.update({name: {"display": info["display"], "feature": "mineral_sands", "copy": "SAND",
                          "tool": "shovel", "drop": None} for name, info in SANDS.items()})

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
    "plate": ["copper", "iron", "tin", "zinc", "bronze", "brass", "invar", "aluminum", "nickel", "lead", "tungsten", "steel",
              "titanium", "thallite", "stainless_steel", "aluminum_magnesium"],
    "gear": ["iron", "bronze", "brass", "invar", "steel"],
    "wire": ["copper", "silver", "aluminum"],
    # Pulverizer output (see tools/machines.py); one dust smelts back into one ingot.
    "dust": ["copper", "iron", "gold", "tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium", "thallite"],
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


def ore_gens(name, info):
    """(placed feature name, worldgen entry) for a metal's or mineral's veins: "gen" as ore_<name>, then "rich_gen" as
    ore_<name>_rich. An entry with "biomes" is placed only in the biome tag jugcraft:has_ore/<placed feature name>."""
    out = []
    if "gen" in info:
        out.append((name, info["gen"]))
    if "rich_gen" in info:
        out.append((f"{name}_rich", info["rich_gen"]))
    if "secondary_gen" in info:
        out.append((f"{name}_secondary", info["secondary_gen"]))
    return out


def ore_gen_owners():
    """{placed feature name: (ore, feature switch)} for every metal's and mineral's veins."""
    return {placed: (name, info["feature"]) for name, info in list(METALS.items()) + list(MINERALS.items())
            for placed, _ in ore_gens(name, info)}

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
    return [f"{mineral}_ore", f"deepslate_{mineral}_ore"] + ([] if MINERALS[mineral].get("vanilla_item") else [f"{mineral}_block"])


def all_blocks():
    return ([b for m in METALS for b in metal_blocks(m)] + [b for m in MINERALS for b in mineral_blocks(m)]
            + list(ROCKS))


def all_items():
    return [i for m in METALS for i in metal_items(m)] + [m for m in MINERALS if not MINERALS[m].get("vanilla_item")] + list(ITEMS) + part_items()


def feature_of(entry_id):
    """The feature switch that owns a registered ID."""
    import jimtafacts
    if entry_id in jimtafacts.items():
        return "concordance"
    import jimtafacts_network
    import jimtafacts_robots
    import jimtafacts_gear
    import jimtafacts_crops
    if entry_id in jimtafacts_crops.items() or entry_id in jimtafacts_crops.blocks():
        return "agriculture"
    import jimtafacts_automation
    import jimtafacts_transport
    if entry_id in jimtafacts_transport.items():
        return 'machines'
    import jimtafacts_production
    import jimtafacts_reactor
    import jimtafacts_infrastructure
    import jimtafacts_upgrades
    if entry_id in jimtafacts_upgrades.items():
        return "jimtafacts"
    if entry_id in jimtafacts_infrastructure.items() or entry_id in jimtafacts_infrastructure.blocks():
        return "jimtafacts"
    import jimtafacts_civil
    if entry_id in jimtafacts_civil.items() or entry_id in jimtafacts_civil.blocks():
        return "jimtafacts"
    import jimtafacts_matter
    if entry_id in jimtafacts_matter.items() or entry_id in jimtafacts_matter.blocks():
        return 'machines'
    if entry_id in jimtafacts_reactor.items():
        return 'machines'
    if entry_id in jimtafacts_production.items():
        return 'machines'
    if entry_id in jimtafacts_automation.items():
        return 'machines'
    if entry_id in jimtafacts_gear.items():
        return 'machines'
    if entry_id in jimtafacts_robots.items():
        return 'machines'
    if entry_id in jimtafacts_network.ids():
        return "machines"
    import jimtafacts_industry
    if entry_id in jimtafacts_industry.items():
        return "machines"
    import jimtafacts_tables, jimtafacts_utilities, jimtafacts_services, jimtafacts_personal, jimtafacts_engines, jimtafacts_factory
    if entry_id in jimtafacts_tables.items() or entry_id in jimtafacts_utilities.items() or entry_id in jimtafacts_services.items() or entry_id in jimtafacts_personal.items() or entry_id in jimtafacts_engines.items() or entry_id in jimtafacts_factory.items():
        return "machines"
    import jimtafacts_cables
    if entry_id in jimtafacts_cables.items():
        return "machines"
    import jimtafacts_terraforming
    if entry_id in jimtafacts_terraforming.items():
        return "machines"
    import jimtafacts_computers
    if entry_id in jimtafacts_computers.items() or entry_id in jimtafacts_computers.blocks():
        return "machines"
    import jimtafacts_spellbooks
    if entry_id in jimtafacts_spellbooks.items():
        return "concordance"
    if entry_id == "deep_drilling_assembly":
        return "machines"
    import vehicle_bays
    if entry_id in vehicle_bays.BLOCKS:
        return "machines"
    import menagerie_registry
    if entry_id in menagerie_registry.entries():
        return menagerie_registry.feature()
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
    import guide_books  # the Drone Tower guide books go with the drones, part of the machines
    if entry_id in guide_books.BOOKS:
        return FEATURE
    import tank_display
    if entry_id in tank_display.BLOCKS:
        return FEATURE
    import agriculture
    if agriculture.owns(entry_id):
        return agriculture.switch_of(entry_id)
    import plastic
    if entry_id in plastic.blocks():
        return "crude_oil"
    import gear
    if entry_id in gear.items():
        return gear.feature(entry_id)
    import exosuit
    if entry_id in exosuit.items():
        return FEATURE
    import arms
    if entry_id in arms.items():
        return arms.feature(entry_id)
    import guns
    if entry_id in guns.items() or entry_id in guns.blocks():
        return "guns"
    import arms_variants
    if entry_id in arms_variants.items():
        return arms_variants.FEATURE
    import grapple
    if entry_id in grapple.items():
        return FEATURE
    import field_chemistry
    if entry_id in field_chemistry.items():
        return FEATURE
    import construction
    if entry_id in construction.items() or entry_id in construction.blocks():
        return FEATURE
    import gas_storage
    if entry_id in gas_storage.items():
        return "machines"
    import control_electronics
    if entry_id in control_electronics.blocks() or entry_id in control_electronics.items():
        return "machines"
    import rocketry
    if entry_id in rocketry.items() or entry_id in rocketry.blocks():
        return "machines"
    import dieselworks
    if entry_id in dieselworks.blocks():
        return "machines"
    import kaiserworks
    if entry_id in kaiserworks.blocks():
        return "machines"
    import trenchworks
    if entry_id in trenchworks.blocks():
        return "machines"
    import zeppelin
    if entry_id in zeppelin.ITEMS:
        return "machines"
    import exchange_poker  # the Exchange's poker tables go with the town
    if entry_id in exchange_poker.BLOCKS:
        return exchange_poker.FEATURE
    import exchange_casino
    if entry_id in exchange_casino.BLOCKS:
        return exchange_casino.FEATURE
    import mech
    if entry_id in mech.ITEMS:
        return "machines"
    import landship
    if entry_id in landship.ITEMS:
        return "machines"
    import trains
    if entry_id in trains.ITEMS:
        return "machines"
    import artillery
    if entry_id in artillery.ITEMS:
        return "machines"
    import tower_guns
    if entry_id in tower_guns.items():
        return "machines"
    import fortifications
    if entry_id in fortifications.blocks():
        return "machines"
    import bunkerworks
    if entry_id in bunkerworks.blocks():
        return "machines"
    import aged_wood
    # Aged pine and maple come from the biomes branch's trees.
    if entry_id in aged_wood.blocks():
        return "biomes"
    import fire_control
    if entry_id in fire_control.blocks() + fire_control.items():
        return "machines"
    import raiders
    if entry_id in raiders.ITEMS or entry_id in raiders.BLOCKS:
        return "raiders"
    import armoured_walker
    if entry_id in armoured_walker.ITEMS:
        return "machines"
    import howitzer_walker
    if entry_id in howitzer_walker.ITEMS:
        return "machines"
    import concordance
    if entry_id in concordance.items() or entry_id in concordance.blocks() or entry_id in concordance.itemless_blocks():
        return concordance.FEATURE
    import industrial_forms
    if entry_id in industrial_forms.blocks():
        return "machines"
    import pixel_hollows
    return pixel_hollows.feature_of(entry_id)
