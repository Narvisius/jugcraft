"""What the Drone Tower costs, and the checks that keep it sensible.

Since October 2026 every step of the Tri-Pillar (the foundation, then tiers 1 to 9) costs the blocks it places, as the
items a player places them from (tower/TowerCosts.java; step_items() below follows the same rules); since 26 October
2026 only the CHARGED ones (Justin: "the main 12 blocks", plus the holo parts): the twelve blocks the tower is mostly
built of and the foundation's four holo pieces. The rest a step places comes free. check() (run
by check_mod_data.py) makes sure every block the tower is built from has a crafting recipe, that no two of those
recipes share a shape (only one of them could ever be crafted), and that every tier is worth more than the one below.

Blocks are valued in steel-ingot equivalents (SIE): a block's crafting recipe is followed down to base materials
(ingots, plates, coke, concrete...), each weighted by WEIGHTS.

The module figures (FAMILIES, BLOCK_VALUE, MODULE_VALUE, module_cost) are kept only for the generators of the earlier,
frozen tower designs (drone_tower.py, tower_industrial.py), whose saved data still lists module costs; the modules
themselves are gone from the game.
"""
import collections
import gzip
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RECIPES = ROOT / "src/main/resources/data/jugcraft/recipe"
TRIPILLAR = ROOT / "src/main/resources/data/jugcraft/drone_tower/tower_tripillar.json.gz"

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
                          "acoustic_wall_panel", "carpet_tile", "blast_glass", "tower_service_grating",
                          "tower_service_railing", "tower_vent", "tower_ladder"},
    "armor_module": {"steel_armor_plate", "hazard_plating", "depleted_uranium_armor", "silicon_carbide_armor"},
    "hangar_module": {"landing_platform", "landing_pad", "hangar_pad", "dock_plating", "hangar_bay_door", "supply_pickup",
                      "intake_funnel", "cargo_exchange", "tower_door_mechanism"},
}


def family(block):
    for module, blocks in FAMILIES.items():
        if block in blocks:
            return module
    return "avionics_module"


# SIE per block placed (from check()'s computation; keep in step with the recipes).
BLOCK_VALUE = {
    "access_floor_tile": 0.633, "acoustic_wall_panel": 0.275, "aluminum_cladding": 1.0, "armored_conduit": 0.75,
    "blast_glass": 0.267, "cable_tray": 0.287, "carbon_composite_panel": 0.5, "cargo_exchange_port": 10.3,
    "cargo_packager": 14.7, "carpet_tile": 0.2, "ceiling_light_panel": 0.525, "ceramic_insulator": 0.2,
    "chiseled_stone_bricks": 0.2, "concrete_column": 0.275, "console_desk": 8.9, "control_screen": 2.65, "cooling_fin": 1.75,
    "copper_busbar": 0.5, "depleted_uranium_armor": 4.75, "dock_plating": 0.35, "drone_depot_terminal": 16.333,
    "energy_exchange_port": 8.9, "equipment_rack": 11.25, "graphene_lattice": 1.45, "hangar_bay_door": 4.0,
    "hangar_pad": 1.113, "hazard_plating": 0.55, "holo_table": 2.683, "intake_funnel": 2.5, "landing_pad": 0.417,
    "landing_platform": 0.675, "operator_chair": 3.4, "red_light_strip": 0.108, "reinforced_concrete": 0.275,
    "silicon_carbide_armor": 1.55, "steel_armor_plate": 2.0, "steel_girder": 1.167, "superconducting_conduit": 3.375,
    "supply_pickup": 1.056, "transformer_casing": 2.8, "tungsten_steel_frame": 4.0, "warning_light": 0.5,
    # A station's one-item recipe value spread over its dummy parts; never charge for 64 complete stations.
    "energy_exchange": 25.3 / 64, "cargo_exchange": 21.8 / 60,
    "tower_service_grating": 0.875, "tower_service_railing": 1.333, "tower_vent": 2.438, "tower_ladder": 1.167,
    "tower_door_mechanism": 2.375,
    "wall_task_light": 0.525, "linear_ceiling_light": 0.525, "tower_master_console": 30.7 / 6,
}
STATION_PARTS = {"energy_exchange": 64, "cargo_exchange": 60, "tower_master_console": 6}
# SIE per module crafted.
MODULE_VALUE = {"structural_module": 47.88, "armor_module": 46.5, "hangar_module": 21.9, "avionics_module": 20.93}


def module_cost(tier, block_names, station_parts=None):
    """{module: count} for a tier that places {@code block_names} (block ids without namespace, one per block)."""
    value = collections.Counter()
    for name in block_names:
        unit = BLOCK_VALUE.get(name, 0.5)
        if station_parts and name in station_parts:
            unit *= STATION_PARTS[name] / station_parts[name]
        value[family(name)] += unit
    cost = {}
    for module in ("structural_module", "hangar_module", "armor_module", "avionics_module"):
        if value[module] > 0:
            cost[module] = max(1, -(-int(value[module] * 1000) // int(MODULE_VALUE[module] * 1000)))
    cost["avionics_module"] = max(cost.get("avionics_module", 0), tier)
    return cost


# ---------------------------------------------------------------- checking against the generated recipes

def _recipes():
    out = {}
    # In name order: where several recipes make an item and none is named after it, the first one counts, and the
    # file system's own order differs between Windows and Linux.
    for f in sorted(RECIPES.glob("**/*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        res = d.get("result")
        if not isinstance(res, dict) or d.get("type") not in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless",
                                                              "jugcraft:advanced_crafting"):
            continue
        item, count = res.get("id"), res.get("count", 1)
        # Prefer the recipe named after the item (not a module or slab that also yields it), at the top of the recipe
        # folder (an industry's alternative in a folder of its own may share the name: jimtafacts_industry/).
        if item in out and (f.stem != item.split(":")[1] or f.parent != RECIPES):
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


# ---------------------------------------------------------------- what a step costs (as tower/TowerCosts.java)

# Fittings one item places whole: only their first part (part=0) is paid for.
WHOLE_FROM_ONE_ITEM = {"jugcraft:energy_exchange", "jugcraft:cargo_exchange", "jugcraft:tower_master_console", "jugcraft:storage_rack",
                       "jugcraft:holo_command_table", "jugcraft:tower_control_console",
                       # the data hall's plant pieces (nuclear/MultiFurnitureBlock: one item places all its parts) and its sign
                       "jugcraft:server_rack", "jugcraft:network_rack", "jugcraft:san_rack", "jugcraft:crac_unit", "jugcraft:ups_battery",
                       "jugcraft:ups_module", "jugcraft:ups_bypass", "jugcraft:pdu_cabinet", "jugcraft:agent_cylinder",
                       "jugcraft:agent_release_panel", "jugcraft:agent_horn_strobe", "jugcraft:data_hall_sign",
                       # the map room's sign (layout 23)
                       "jugcraft:map_room_sign"}
# Parts with no item of their own (they come with another block).
NO_ITEM = {"jugcraft:speed_gate_pedestal", "jugcraft:curved_video_wall_part", "jugcraft:drone_display_case_part", "jugcraft:crystal_tree_part",
           "jugcraft:long_coffee_table_part", "jugcraft:drone_parts_shelving_part", "jugcraft:aisle_containment_door_part",
           "jugcraft:holo_projector_part", "jugcraft:cradle_struts_part", "jugcraft:big_map_table_part",
           "jugcraft:ember_oak_part", "jugcraft:drift_isle_part"}
DOORS = {"jugcraft:glass_door", "jugcraft:steel_door", "jugcraft:maintenance_door"}
KIT_MANIFEST = ROOT / "src/main/resources/jugcraft/room_kit.json"


def _kit_anchors(cache={}):
    """The upper floor's room kit pieces of more than one block (tools/room_kit.py): each one's anchor cell's part and
    half. One item places the whole piece, so only its anchor costs one (as tower/TowerCosts counts them)."""
    if not cache and KIT_MANIFEST.exists():
        for b in json.loads(KIT_MANIFEST.read_text(encoding="utf-8"))["blocks"]:
            if len(b["cells"]) > 1:
                cache[f"jugcraft:{b['id']}"] = {k: str(v) for k, v in b["cells"][0][3].items()}
    return cache


def _props(state):
    if "[" not in state:
        return {}
    return dict(kv.split("=") for kv in state[state.index("[") + 1:-1].split(","))


def items_for(state):
    """(item id, count) one placed block state costs."""
    block = state.split("[")[0]
    props = _props(state)
    if block in NO_ITEM:
        return block, 0
    anchor = _kit_anchors().get(block)
    if anchor is not None:
        return block, 1 if all(props.get(k) == v for k, v in anchor.items()) else 0
    if block in WHOLE_FROM_ONE_ITEM and props.get("part", "0") != "0":
        return block, 0
    if block in DOORS and props.get("half") == "upper":
        return block, 0
    if block.endswith("_slab") and props.get("type") == "double":
        return block, 2
    return block, 1


# What a step is paid for (tower/TowerCosts.CHARGED, the same list): the twelve blocks the tower uses most (about 93% of
# it). The holograms (tower/Holograms, 28 October 2026: no recipe, the tower's own) come free, as the rest does.
CHARGED = ("carbon_composite_panel", "reinforced_concrete", "steel_armor_plate", "ceiling_tile", "access_floor_tile", "dock_plating",
           "framed_glass", "epoxy_floor", "hangar_pad", "neon_tube", "ceiling_light_panel", "painted_wall")


def step_items(place, palette, charged_only=True):
    """{item id: count} for a step that places {@code place} ([x, y, z, palette index] each): what it is paid for, or
    with charged_only False every item it is placed from."""
    out = collections.Counter()
    for p in place:
        item, n = items_for(palette[p[3]])
        if n:
            out[item] += n
        if palette[p[3]].startswith("jugcraft:console_chair[") and "platform=true" in palette[p[3]]:
            out["jugcraft:raised_floor"] += 1      # the chair took the floor tile's cell: it is placed from both
    if charged_only:
        out = collections.Counter({i: n for i, n in out.items() if i.startswith("jugcraft:") and i.split(":")[1] in CHARGED})
    return dict(out.most_common())


def tripillar_costs(charged_only=True):
    """[(step name, {item: count})] for the foundation and tiers 1-9 of the generated Tri-Pillar data: what each is paid
    for (or with charged_only False everything it is placed from)."""
    with gzip.open(TRIPILLAR, "rt", encoding="utf-8") as f:
        data = json.load(f)
    steps = [("foundation", data["foundation"])] + [(f"tier {t['tier']}", t) for t in data["tiers"]]
    return [(name, step_items(t["place"], data["palette"], charged_only)) for name, t in steps]


def _shape(pattern, key):
    """A shaped recipe's grid with its ingredients, trimmed, the same whichever way round (recipes match mirrored)."""
    rows = list(pattern)
    width = max(len(r) for r in rows)
    rows = [r.ljust(width) for r in rows]
    rows = [r for r in rows if r.strip()]
    cols = [i for i in range(width) if any(r[i] != " " for r in rows)]
    rows = [r[min(cols):max(cols) + 1] for r in rows]

    def ing(c):
        v = key[c]
        if isinstance(v, list):
            return tuple(sorted(str(x) for x in v))
        if isinstance(v, dict):
            return v.get("item") or "#" + v.get("tag", "")
        return v
    grid = tuple(tuple(ing(c) if c != " " else None for c in r) for r in rows)
    return min(str(grid), str(tuple(tuple(reversed(r)) for r in grid)))


def check(err):
    recipes = _recipes()
    costs = tripillar_costs()
    java = (ROOT / "src/main/java/io/github/jimbozoomer/jugcraft/tower/TowerCosts.java").read_text(encoding="utf-8")
    start = java.find("CHARGED = Set.of(")
    listed = set(__import__("re").findall(r'"([a-z0-9_]+)"', java[start:java.find(");", start)])) if start >= 0 else set()
    if listed != set(CHARGED):
        err(f"tower_costs: TowerCosts.CHARGED {sorted(listed)} differs from tools/tower_costs.py CHARGED {sorted(CHARGED)}")
    for step, cost in costs:
        if not cost:
            err(f"tower_costs: the Tri-Pillar's {step} costs nothing")
    used = {item for _, cost in costs for item in cost}
    for item in sorted(used):
        if item.startswith("jugcraft:") and item not in recipes:
            err(f"tower_costs: the Drone Tower is built from {item}, which has no crafting recipe")
    # no two shaped recipes for the tower's items (or anything else) share a shape: on the crafting table, or on the
    # Advanced Crafting Table (tools/advanced_crafting.py; its 6 x 6s can never match a 3 x 3)
    shapes = collections.defaultdict(set)
    for f in RECIPES.glob("**/*.json"):
        d = json.loads(f.read_text(encoding="utf-8"))
        if d.get("type") in ("minecraft:crafting_shaped", "jugcraft:advanced_crafting") and isinstance(d.get("result"), dict):
            shapes[(d["type"], _shape(d["pattern"], d["key"]))].add(d["result"]["id"])
    for results in shapes.values():
        if len(results) > 1 and results & used:
            err(f"tower_costs: these recipes have the same shape and ingredients, so only one can be crafted: {sorted(results)}")
    # every tier is worth more than the one below
    last = 0.0
    for name, cost in costs[1:]:
        value = sum(_value(item, recipes) * n for item, n in cost.items())
        if value <= last:
            err(f"tower_costs: {name} is worth {value:.0f} SIE, not more than the tier below ({last:.0f})")
        last = value
