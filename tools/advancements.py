"""The Jugcraft advancement tree: a quest line from the first tin to rocket packs, written to
data/jugcraft/advancement/ by generate_material_data.py. Each step is earned by having the item;
the Engineer's Handbook's Milestones page lists them in order.

Each entry: id -> (parent or None, item that earns it, title, description, frame).
"""

TREE = {
    "root": (None, "raw_tin", "Jugcraft", "Dig up tin, the first metal of the workshop", "task"),
    "bronze": ("root", "bronze_ingot", "Bronze Age", "Alloy tin and copper into bronze", "task"),
    "prospector": ("bronze", "prospector", "Geo-Resonance", "Build a prospector and listen for ore underground", "task"),
    "coal_generator": ("bronze", "coal_generator", "Spark of Industry", "Build a coal generator", "task"),
    "crusher": ("coal_generator", "crusher", "Twice the Ore", "Build a crusher and double your ingots", "task"),
    "pulverizer": ("crusher", "pulverizer", "Down to Dust", "Build a pulverizer for byproducts", "task"),
    "battery_box": ("coal_generator", "battery_box", "Stored Lightning", "Build a battery box", "task"),
    "auto_crafter": ("coal_generator", "auto_crafter", "Hands Off", "Build an auto-crafter", "task"),
    "hand_crank": ("bronze", "hand_crank", "Elbow Grease", "Make a hand crank: power by hand", "task"),
    "steam_engine": ("hand_crank", "steam_engine", "Full Steam Ahead", "Build a steam engine to turn your shafts", "task"),
    "large_steam_engine": ("steam_engine", "large_steam_engine", "Iron Horse", "Build the large steam engine", "goal"),
    "belt_pulley": ("hand_crank", "belt_pulley", "Belt Drive", "Carry rotation across a gap with belt pulleys", "task"),
    "conveyor": ("hand_crank", "conveyor", "Assembly Line", "Build a conveyor", "task"),
    "conveyor_splitter": ("conveyor", "conveyor_splitter", "Sorting It Out", "Split a conveyor line three ways", "task"),
    "steel": ("coal_generator", "steel_ingot", "Forged in Coke", "Make steel", "goal"),
    "steel_foundry": ("steel", "steel_foundry", "Blast Furnace", "Build the steel foundry", "goal"),
    "capacitor_bank": ("steel", "capacitor_bank", "Capacitance", "Build a capacitor bank", "task"),
    "charging_station": ("steel", "charging_station", "Plugged In", "Build a charging station", "task"),
    "mining_drill": ("charging_station", "mining_drill", "Power Drill", "Make a mining drill", "task"),
    "chainsaw": ("charging_station", "chainsaw", "Timber!", "Make a chainsaw", "task"),
    "upgrade": ("mining_drill", ["overclock_module", "range_module", "capacity_module", "silk_touch_module",
                                 "fortune_module"], "Tinkerer", "Make an upgrade module for a powered tool", "task"),
    "rocket_pack": ("charging_station", "rocket_pack", "Rocketeer", "Make a rocket pack and take to the air", "challenge"),
    # The oil line (Chemistry branch).
    "crude_oil": ("steel", "crude_oil_bucket", "Black Gold", "Fill a bucket with crude oil", "task"),
    "pumpjack": ("crude_oil", "pumpjack", "Nodding Donkey", "Build a pumpjack over an oil reservoir", "task"),
    "distillation_tower": ("crude_oil", "distillation_tower", "Fractional Thinking",
                           "Build a distillation tower", "goal"),
    "fracking_rig": ("distillation_tower", "fracking_rig", "Tight Spot", "Build a fracking rig for shale oil", "goal"),
    "diesel_generator": ("distillation_tower", "diesel_generator", "Diesel Power", "Build a diesel generator", "task"),
    "gas_turbine": ("diesel_generator", "gas_turbine", "Spool Up", "Build a gas turbine", "goal"),
    "diesel_engine": ("diesel_generator", "diesel_engine", "Eight Cylinders", "Build a diesel engine", "goal"),
    "plastic": ("distillation_tower", "plastic_sheet", "Fantastic Plastic", "Press a plastic sheet", "task"),
    "asphalt": ("distillation_tower", "asphalt", "Hit the Road", "Lay asphalt, and walk faster on it", "task"),
}
# Background of the tab (a block texture), shown behind the tree.
BACKGROUND = "jugcraft:block/dp_gunmetal"


def advancement(key, parent, items, title, description, frame, mod="jugcraft"):
    items = items if isinstance(items, list) else [items]
    display = {
        "icon": {"id": f"{mod}:{items[0]}"},
        "title": {"translate": f"advancements.{mod}.{key}.title"},
        "description": {"translate": f"advancements.{mod}.{key}.description"},
        "frame": frame,
        "show_toast": True,
        "announce_to_chat": frame != "task",
    }
    if parent is None:
        display["background"] = BACKGROUND
    out = {"display": display,
           "criteria": {"has_item": {"trigger": "minecraft:inventory_changed",
                                     "conditions": {"items": [{"items": [f"{mod}:{item}" for item in items]}]}}},
           "requirements": [["has_item"]]}
    if parent is not None:
        out["parent"] = f"{mod}:{parent}"
    return out


def generate(mod="jugcraft"):
    """(path, json) for every advancement, and the lang entries."""
    files, lang = {}, {}
    for key, (parent, items, title, description, frame) in TREE.items():
        files[key] = advancement(key, parent, items, title, description, frame, mod)
        lang[f"advancements.{mod}.{key}.title"] = title
        lang[f"advancements.{mod}.{key}.description"] = description
    return files, lang
