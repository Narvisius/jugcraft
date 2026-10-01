"""What Drone Tower tiers cost in modules, so a tier's modules are worth what its blocks would cost to craft.

Every tower block is valued in steel-ingot equivalents (SIE): its crafting recipe is followed down to base
materials (ingots, plates, coke, concrete...), each weighted by WEIGHTS. Blocks fall into the four module
families (structural, armour, hangar, avionics), and a tier needs, per family, enough modules to cover the value
of the blocks it places: ceil(family value / module value). The avionics count is never below the tier number
(the electronics each tier adds).

BLOCK_VALUE and MODULE_VALUE are fixed numbers so the tower data can be written before the recipe files are;
check() (run by check_mod_data.py) recomputes them from the generated recipes and fails if they drift, so a
recipe change can't quietly make modules cheaper or dearer than the blocks.
"""
import collections
import json
from pathlib import Path

RECIPES = Path(__file__).resolve().parent.parent / "src/main/resources/data/jugcraft/recipe"

# Base materials in steel-ingot equivalents.
WEIGHTS = {
    "#c:ingots/steel": 1, "#c:plates/steel": 1, "#c:storage_blocks/steel": 9, "#c:gears/steel": 4,
    "#c:ingots/iron": 0.6, "#c:plates/iron": 0.6, "#c:ingots/copper": 0.5, "#c:plates/copper": 0.5, "#c:dusts/copper": 0.5,
    "#c:wires/copper": 0.25, "#c:wires/silver": 1, "#c:plates/aluminum": 1, "#c:ingots/tungsten": 3, "#c:ingots/uranium": 3,
    "#c:plates/titanium": 2.5, "#c:ingots/bronze": 0.6, "#c:ingots/zinc": 0.4, "#c:ingots/tin": 0.4,
    "jugcraft:silicon": 1, "jugcraft:coke": 0.3, "jugcraft:plastic_sheet": 0.5, "jugcraft:rare_earth_oxide": 2,
    "jugcraft:basic_circuit": 1.5, "jugcraft:advanced_circuit": 4, "jugcraft:processor": 8,
    "minecraft:gray_concrete": 0.15, "minecraft:smooth_stone": 0.1, "minecraft:glass_pane": 0.05, "minecraft:glass": 0.1,
    "minecraft:redstone": 0.2,
}


def weight(item):
    if item in WEIGHTS:
        return WEIGHTS[item]
    if item.endswith("_dye"):
        return 0.1
    return 0.2 if item.startswith("minecraft:") else 0.5


FAMILIES = {
    "structural_module": {"reinforced_concrete", "concrete_column", "steel_girder", "aluminum_cladding", "carbon_composite_panel",
                          "graphene_lattice", "tungsten_steel_frame", "chiseled_stone_bricks", "access_floor_tile",
                          "acoustic_wall_panel", "carpet_tile", "blast_glass"},
    "armor_module": {"steel_armor_plate", "hazard_plating", "depleted_uranium_armor", "silicon_carbide_armor"},
    "hangar_module": {"landing_platform", "landing_pad", "hangar_pad", "dock_plating", "hangar_bay_door", "supply_pickup",
                      "intake_funnel"},
}


def family(block):
    for module, blocks in FAMILIES.items():
        if block in blocks:
            return module
    return "avionics_module"


# SIE per block placed (from check()'s computation; keep in step with the recipes).
BLOCK_VALUE = {
    "access_floor_tile": 0.633, "acoustic_wall_panel": 0.275, "aluminum_cladding": 1.0, "armored_conduit": 0.75,
    "blast_glass": 0.267, "cable_tray": 0.287, "carbon_composite_panel": 0.875, "cargo_exchange_port": 6.3,
    "cargo_packager": 14.7, "carpet_tile": 0.2, "ceiling_light_panel": 0.525, "ceramic_insulator": 0.2,
    "chiseled_stone_bricks": 0.2, "concrete_column": 0.275, "console_desk": 4.9, "control_screen": 1.317, "cooling_fin": 1.75,
    "copper_busbar": 0.5, "depleted_uranium_armor": 4.75, "dock_plating": 0.35, "drone_depot_terminal": 16.333,
    "energy_exchange_port": 8.9, "equipment_rack": 5.25, "graphene_lattice": 1.45, "hangar_bay_door": 4.0,
    "hangar_pad": 1.113, "hazard_plating": 0.55, "holo_table": 1.35, "intake_funnel": 2.5, "landing_pad": 0.417,
    "landing_platform": 0.675, "operator_chair": 3.4, "red_light_strip": 0.108, "reinforced_concrete": 0.275,
    "silicon_carbide_armor": 1.55, "steel_armor_plate": 2.0, "steel_girder": 1.167, "superconducting_conduit": 3.375,
    "supply_pickup": 1.056, "transformer_casing": 2.8, "tungsten_steel_frame": 4.0, "warning_light": 0.5,
}
# SIE per module crafted.
MODULE_VALUE = {"structural_module": 47.88, "armor_module": 46.5, "hangar_module": 21.9, "avionics_module": 24.43}


def module_cost(tier, block_names):
    """{module: count} for a tier that places {@code block_names} (block ids without namespace, one per block)."""
    value = collections.Counter()
    for name in block_names:
        value[family(name)] += BLOCK_VALUE.get(name, 0.5)
    cost = {}
    for module in ("structural_module", "hangar_module", "armor_module", "avionics_module"):
        if value[module] > 0:
            cost[module] = max(1, -(-int(value[module] * 1000) // int(MODULE_VALUE[module] * 1000)))
    cost["avionics_module"] = max(cost.get("avionics_module", 0), tier)
    return cost


# ---------------------------------------------------------------- checking against the generated recipes

def _recipes():
    out = {}
    for f in RECIPES.glob("**/*.json"):
        d = json.loads(f.read_text(encoding="utf-8"))
        res = d.get("result")
        if not isinstance(res, dict) or d.get("type") not in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless"):
            continue
        item, count = res.get("id"), res.get("count", 1)
        # Prefer the recipe named after the item (not a module or slab that also yields it).
        if item in out and f.stem != item.split(":")[1]:
            continue
        ing = collections.Counter()
        keys = [d["key"][ch] for row in d["pattern"] for ch in row if ch != " "] if "pattern" in d else d["ingredients"]
        for k in keys:
            if isinstance(k, list):
                k = k[0]
            if isinstance(k, dict):
                k = k.get("item") or "#" + k.get("tag", "")
            ing[k] += 1
        out[item] = (ing, count)
    return out


def _value(item, recipes, depth=0):
    if item.startswith("#") or not item.startswith("jugcraft:") or item not in recipes or depth > 6:
        return weight(item)
    ing, count = recipes[item]
    return sum(_value(k, recipes, depth + 1) * n for k, n in ing.items()) / count


def check(err):
    recipes = _recipes()
    for name, fixed in BLOCK_VALUE.items():
        item = ("minecraft:" if name == "chiseled_stone_bricks" else "jugcraft:") + name
        actual = _value(item, recipes)
        if abs(actual - fixed) > max(0.02, fixed * 0.02):
            err(f"tower_costs.BLOCK_VALUE[{name}] is {fixed} but its recipe now works out at {actual:.3f} SIE")
    for module, fixed in MODULE_VALUE.items():
        actual = _value("jugcraft:" + module, recipes)
        if abs(actual - fixed) > fixed * 0.02:
            err(f"tower_costs.MODULE_VALUE[{module}] is {fixed} but its recipe now works out at {actual:.2f} SIE")
