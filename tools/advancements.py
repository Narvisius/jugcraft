"""The Jugcraft advancement tree: a quest line from the first tin to rocket packs, written to
data/jugcraft/advancement/ by generate_material_data.py. Each step is earned by having the item;
the Engineer's Handbook's Milestones page lists them in order.

Each entry: id -> (parent or None, item that earns it, title, description, frame).
"""

TREE = {
    "root": (None, "raw_tin", "Jugcraft", "Dig up tin, the first metal of the workshop", "task"),
    "bronze": ("root", "bronze_ingot", "Bronze Age", "Alloy tin and copper into bronze", "task"),
    "pixel_shard": ("root", "pixel_shard", "Dead Pixels", "Find the Pixel Hollows and mine a pixel crystal", "task"),
    "prospector": ("bronze", "prospector", "Geo-Resonance", "Build a prospector and listen for ore underground", "task"),
    "coal_generator": ("bronze", "coal_generator", "Spark of Industry", "Build a coal generator", "task"),
    "crusher": ("coal_generator", "crusher", "Twice the Ore", "Build a crusher and double your ingots", "task"),
    "pulverizer": ("crusher", "pulverizer", "Down to Dust", "Build a pulverizer for byproducts", "task"),
    "battery_box": ("coal_generator", "battery_box", "Stored Lightning", "Build a battery box", "task"),
    "auto_crafter": ("coal_generator", "auto_crafter", "Hands Off", "Build an auto-crafter", "task"),
    "deposit_drill": ("coal_generator", "deposit_drill", "Strike It Rich", "Build a deposit drill on a surface deposit",
                      "goal"),
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
    # Batch 25: gear.
    # A Kaiser piece is steel armor too (docs/features/steampunk-and-kaiser-armor.md), so one got by trade counts.
    "steel_armor": ("steel", ["steel_helmet", "steel_chestplate", "steel_leggings", "steel_boots", "kaiser_helmet",
                              "kaiser_chestplate", "kaiser_leggings", "kaiser_boots"], "Suited Up",
                    "Make a piece of steel armor", "task"),
    # Steampunk and Kaiser Armor: bronze and steel armor smithed into their stylized looks.
    "steampunk_armor": ("bronze", ["steampunk_helmet", "steampunk_chestplate", "steampunk_leggings", "steampunk_boots"],
                        "Goggles On", "Smith a Steampunk Pattern onto a piece of bronze armor", "task"),
    "kaiser_armor": ("steel_armor", ["kaiser_helmet", "kaiser_chestplate", "kaiser_leggings", "kaiser_boots"],
                     "On Parade", "Smith a Kaiser Pattern onto a piece of steel armor", "task"),
    "paxel": ("steel", [f"{tier}_paxel" for tier in ("wood", "stone", "iron", "gold", "diamond", "netherite", "bronze",
                                                      "steel")], "Jack of All Trades", "Make a paxel", "task"),
    # Batch 42: the arms, in bronze from the bronze age on.
    "arms": ("bronze", [f"{metal}_{kind}" for metal in ("bronze", "steel") for kind in (
        "longsword", "greatsword", "rapier", "flanged_mace", "war_hammer", "glaive", "halberd", "spear", "lance")],
        "Man-at-Arms", "Forge a longsword, greatsword, rapier, mace, war hammer, glaive, halberd, spear or lance", "task"),
    # Batch 45: Arms II, from the bronze age on.
    "arms_ii": ("bronze", [f"{metal}_{kind}" for metal in ("bronze", "steel") for kind in (
        "dagger", "sabre", "estoc", "battle_axe", "flail", "scythe", "quarterstaff", "pike")],
        "Armory", "Forge a dagger, sabre, estoc, battle axe, flail, scythe, quarterstaff or pike", "task"),
    # Batch 46: Arms III, from the bronze age on.
    "arms_iii": ("bronze", [f"{metal}_{kind}" for metal in ("bronze", "steel") for kind in (
        "zweihander", "maul", "executioner", "bill")],
        "Two-Hander", "Forge a zweihander, maul, executioner's sword or bill", "task"),
    # Batch 47: Arms IV, from the bronze age on.
    "arms_iv": ("bronze", [f"{metal}_{kind}" for metal in ("bronze", "steel") for kind in (
        "labrys", "battleblade", "war_fork", "kama", "war_pick")],
        "Masterwork", "Forge a labrys, battleblade, war fork, kama or war pick", "task"),
    # Batch 48: Arms V, the arms with weapon arts, from the bronze age on.
    "arms_v": ("bronze", [f"{metal}_{kind}" for metal in ("bronze", "steel") for kind in (
        "twinblade", "nodachi", "earthbreaker", "katar", "moonblade", "kusarigama")],
        "Weapon Art", "Forge a twinblade, nodachi, earthbreaker, katar, moonblade or kusarigama", "task"),
    # Batch 55: Arms VI, the katana, brazier mace, bows, crossbows and shields, from the bronze age on.
    "arms_vi": ("bronze", [f"{metal}_{kind}" for metal in ("bronze", "steel") for kind in (
        "katana", "brazier_mace", "longbow", "arbalest", "heater_shield", "tower_shield")],
        "War Kit", "Forge a katana, brazier mace, longbow, arbalest, heater shield or tower shield", "task"),
    # Batch 59: Arms VIII, the thrown arms, from the bronze age on.
    "arms_viii": ("bronze", [f"{metal}_{kind}" for metal in ("bronze", "steel") for kind in (
        "javelin", "francisca", "chakram", "harpoon")],
        "Let Fly", "Forge a javelin, francisca, chakram or harpoon", "task"),
    # Batch 27: gear and plastic blocks.
    "scuba": ("rubber", ["scuba_mask", "scuba_tank"], "Deep Breath", "Make scuba gear and breathe under water on oxygen",
              "task"),
    "free_runners": ("rubber", "free_runners", "Light on Your Feet", "Make free runners and never take fall damage",
                     "task"),
    "power_weapon": ("charging_station", ["power_katana", "power_bow"], "Charged Up", "Make a power katana or power bow",
                     "goal"),
    "plastic_blocks": ("plastic", [f"{color}_plastic" for color in (
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
        "blue", "brown", "green", "red", "black")], "Lego My Ego", "Mould coloured plastic blocks", "task"),
    # Batch 28: the powered exosuit.
    "exosuit": ("charging_station", [f"exosuit_{piece}" for piece in ("helmet", "chestplate", "leggings", "boots")],
                "Steel Samurai", "Build a piece of the powered exosuit", "goal"),
    "ronin": ("exosuit", [f"ronin_exosuit_{piece}" for piece in ("helmet", "chestplate", "leggings", "boots")]
              + ["ronin_katana"], "Masterless", "Repaint your exosuit or katana in the Ronin livery", "task"),
    # The oil line (Chemistry branch).
    "crude_oil": ("steel", "crude_oil_bucket", "Black Gold", "Fill a bucket with crude oil", "task"),
    "pumpjack": ("crude_oil", "pumpjack", "Nodding Donkey", "Build a pumpjack over an oil reservoir", "task"),
    "distillation_tower": ("crude_oil", "distillation_tower", "Fractional Thinking",
                           "Build a distillation tower", "goal"),
    "fracking_rig": ("distillation_tower", "fracking_rig", "Tight Spot", "Build a fracking rig for shale oil", "goal"),
    "diesel_generator": ("distillation_tower", "diesel_generator", "Diesel Power", "Build a diesel generator", "task"),
    "gas_turbine": ("diesel_generator", "gas_turbine", "Spool Up", "Build a gas turbine", "goal"),
    # Batch 29: refinery upgrades.
    "premium_fuel": ("diesel_generator", ["premium_diesel_bucket", "premium_gasoline_bucket"], "Top Shelf",
                     "Make premium diesel or premium gasoline in the hydrotreater", "task"),
    "heat_recovery_unit": ("gas_turbine", "heat_recovery_unit", "Waste Not",
                           "Build a heat recovery unit to make power from a generator's exhaust", "goal"),
    "diesel_engine": ("diesel_generator", "diesel_engine", "Eight Cylinders", "Build a diesel engine", "goal"),
    "plastic": ("distillation_tower", "plastic_sheet", "Fantastic Plastic", "Press a plastic sheet", "task"),
    "rubber": ("plastic", "rubber", "Bouncing Back", "Make synthetic rubber", "task"),
    "pvc": ("plastic", "pvc_resin", "Vinyl Revival", "Make PVC resin from chlorine", "task"),
    "soap": ("electrolytic_cell", "soap", "Squeaky Clean", "Boil soap from lye", "task"),
    "optical_fibre": ("processor", "optical_fibre", "Light Speed", "Draw borosilicate glass into optical fibre", "task"),
    # Explosive weapons (batch 18).
    "grenade": ("nitric_acid", "grenade", "Pin Pulled", "Make a grenade", "task"),
    # Flow batteries (batch 17).
    "flow_battery": ("lithium_battery_bank", "flow_battery", "Going with the Flow", "Build a flow battery", "goal"),
    "asphalt": ("distillation_tower", "asphalt", "Hit the Road", "Lay asphalt, and walk faster on it", "task"),
    # Industrial chemistry (batch 5).
    "electrolytic_cell": ("steel", "electrolytic_cell", "Split Decision", "Build an electrolytic cell", "goal"),
    "sulfuric_acid": ("electrolytic_cell", "sulfuric_acid_bucket", "Oil of Vitriol", "Make sulfuric acid", "task"),
    "fertilizer": ("sulfuric_acid", "fertilizer", "Green Revolution", "Make fertilizer", "task"),
    # Batch 26: fuel from crops.
    "bioethanol": ("crop_harvester", "bioethanol_bucket", "Moonshine", "Ferment crops into bioethanol", "task"),
    "alumina": ("electrolytic_cell", "alumina", "The Bayer Way", "Digest bauxite into alumina", "task"),
    "fuel_cell": ("electrolytic_cell", "fuel_cell", "Clean Burn", "Build a hydrogen fuel cell", "goal"),
    # Nitrogen chemistry (batch 12).
    "air_separation_unit": ("electrolytic_cell", "air_separation_unit", "Thin Air",
                            "Build an air separation unit", "goal"),
    # Batch 30: the pneumatic grapple, on nitrogen from the air separation unit.
    "pneumatic_grapple": ("air_separation_unit", "pneumatic_grapple", "Reel Me In", "Make a pneumatic grapple", "task"),
    # Batch 31: field chemistry.
    "gas_mask": ("air_separation_unit", "gas_mask", "Filtered", "Make a gas mask", "task"),
    "chemical_grenade": ("grenade", ["chlorine_grenade", "smoke_grenade", "thermite_grenade", "flashbang"],
                         "Chemical Arsenal", "Make a chlorine, smoke or thermite grenade or a flashbang", "task"),
    # Batch 32: construction chemistry.
    "foam_sprayer": ("synthesis_converter", "foam_sprayer", "Expanding Foam", "Make a foam sprayer", "task"),
    "blastproof_concrete": ("steel", "blastproof_concrete", "Bunker Down", "Cast blast-proof concrete, as blast-proof as "
                            "obsidian", "task"),
    "first_aid_kit": ("air_separation_unit", "first_aid_kit", "Field Medic", "Make a first aid kit in the chemical reactor",
                      "task"),
    "synthesis_converter": ("air_separation_unit", "synthesis_converter", "Bread from Air",
                            "Build a synthesis converter, to make ammonia from air", "goal"),
    "nitric_acid": ("synthesis_converter", "nitric_acid_bucket", "Aqua Fortis", "Make nitric acid", "task"),
    # Advanced materials (batch 6).
    "titanium": ("electrolytic_cell", "titanium_ingot", "Kroll Call", "Chlorinate titanium and melt the sponge", "task"),
    "lithium_battery_bank": ("titanium", "lithium_battery_bank", "Power Wall", "Build a lithium battery bank", "goal"),
    "neodymium_magnet": ("sulfuric_acid", "neodymium_magnet", "Strong Attraction", "Make a neodymium magnet", "task"),
    "magnet_dynamo": ("neodymium_magnet", ["magnet_dynamo", "magnet_motor"], "Lossless (Almost)",
                      "Build a magnet dynamo or magnet motor", "goal"),
    # Electronics (batch 7).
    "silicon_boule": ("titanium", "silicon_boule", "Pulling Strings", "Pull a silicon boule in the arc furnace", "goal"),
    "microchip": ("silicon_boule", "microchip", "Etched in Light", "Etch microchips in the lithography station", "task"),
    "processor": ("microchip", "processor", "Central Processing", "Assemble a processor", "task"),
    "network_terminal": ("processor", "network_terminal", "Hello, World", "Build a network terminal", "goal"),
    "logic_controller": ("network_terminal", "logic_controller", "In Control", "Build a logic controller", "task"),
    "control_monitor": ("logic_controller", "control_monitor", "Mission Control", "Build control monitor panels", "task"),
    # Fluid logistics (batch 8).
    "gas_holder": ("electrolytic_cell", "gas_holder", "Under Pressure", "Build a gas holder", "goal"),
    "fluid_valve": ("crude_oil", "fluid_valve", "Shut-Off Valve", "Make a fluid valve", "task"),
    "fluid_filter": ("fluid_valve", "fluid_filter", "Strained Relations", "Make a fluid filter", "task"),
    # Advanced power (batch 10).
    "advanced_solar_panel": ("processor", "advanced_solar_panel", "Here Comes the Sun",
                             "Build an advanced solar panel", "goal"),
    "tank_gauge": ("crude_oil", "tank_gauge", "Full of It", "Make a tank gauge", "task"),
    "solar_receiver": ("advanced_solar_panel", "solar_receiver", "Concentrate", "Build a solar receiver for a heliostat field", "goal"),
    "turbocharger": ("advanced_engine", "turbocharger", "Spool Up", "Make a turbocharger for the advanced engine", "task"),
    "flywheel": ("steam_engine", "flywheel", "Keep It Spinning", "Build a flywheel", "task"),
    "advanced_engine": ("diesel_engine", "advanced_engine", "Four on the Floor",
                        "Build an advanced combustion engine", "goal"),
    # Farming (batch 9).
    "cotton": ("bronze", "cotton", "King Cotton", "Grow cotton", "task"),
    "sprinkler": ("cotton", "sprinkler", "Make It Rain", "Build a sprinkler", "task"),
    "electroplating_bath": ("electrolytic_cell", "electroplating_bath", "Silver Lining", "Build an electroplating bath",
                            "task"),
    "ammonia_chiller": ("synthesis_converter", "ammonia_chiller", "Ice Cold", "Build an ammonia chiller", "task"),
    "cryogenic_liquefier": ("rocket_workshop", "cryogenic_liquefier", "Minus 183", "Build a cryogenic liquefier", "task"),
    "rocket_workshop": ("processor", "rocket_workshop", "Rocket Science", "Build a rocket workshop", "goal"),
    "survey_rocket": ("rocket_workshop", "survey_rocket", "Eye in the Sky", "Assemble a survey rocket", "task"),
    "hydroponic_bay": ("crop_harvester", "hydroponic_bay", "Soil Optional", "Build a hydroponic bay", "task"),
    "crop_harvester": ("steel", "crop_harvester", "Reaping What You Sow", "Build a crop harvester", "goal"),
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
