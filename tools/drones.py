"""Drone Depot data (docs/features/drone-depot.md): blocks, drone tiers, parts and recipes.

generate_material_data.py writes the models, names, loot tables and recipes from these tables, and
check_mod_data.py checks them against drone/DroneTier.java and drone/JugcraftDrones.java.
All recipes are shaped crafting recipes, so the vanilla Crafter can automate drone production.
"""

FEATURE = "drones"

# Blocks: display name and textures (cube_all, side/top/bottom, or the pad plate).
PAD_SIZE = 5
PAD_PLATE_HEIGHT = 3
DRONE_BLOCKS = {
    # A console desk with one big display; its model is written by tools/tower.py (TERMINAL_MODEL).
    "drone_depot_terminal": {"display": "Drone Depot Terminal", "faces": "furniture"},
    "landing_platform": {"display": "Landing Platform", "faces": "all"},
    # Thin plates placed on top of the platform; 25 of them in a 5x5 form one pad (part 1-25).
    "landing_pad": {"display": "Landing Pad", "faces": "pad"},
    "cargo_packager": {"display": "Cargo Packager", "faces": "side_top_bottom"},
    # Thin plates on the platform; 9 in a 3x3 form the supply pickup with its lift hatch (part 1-9).
    "supply_pickup": {"display": "Supply Pickup Plate", "faces": "pickup"},
    # Control room wall panels; 6 in a 3x2 wall form one live screen (part 1-6), any facing.
    "control_screen": {"display": "Control Screen Panel", "faces": "screen"},
    # Hologram table sections; 9 in a 3x3 form one table with a projector (part 1-9).
    "holo_table": {"display": "Hologram Table", "faces": "holo"},
    # Control room decoration.
}
HOLO_SIZE = 3
HOLO_HEIGHT = 13
PICKUP_SIZE = 3
SCREEN_WIDTH = 3
SCREEN_HEIGHT = 2
SCREEN_THICKNESS = 3

# Tier stats, kept in sync with drone/DroneTier.java: capacity (blocks/trip), speed (blocks/s),
# upkeep (JE/t while working), size (landing slots: small 1, medium 2, large 4), available = craftable
# now (every tier is, since main has neodymium, plastics, hydrogen, titanium and processors).
DRONE_TIERS = {
    1: {"id": "drone_t1", "display": "Courier Quad Drone", "capacity": 1, "speed": 4, "upkeep": 8, "size": "small", "available": True},
    2: {"id": "drone_t2", "display": "Survey Hexacopter Drone", "capacity": 2, "speed": 4, "upkeep": 12, "size": "small", "available": True},
    3: {"id": "drone_t3", "display": "Lifter Octocopter Drone", "capacity": 4, "speed": 4, "upkeep": 20, "size": "small", "available": True},
    4: {"id": "drone_t4", "display": "Ducted-Fan Runner Drone", "capacity": 4, "speed": 8, "upkeep": 32, "size": "medium", "available": True},
    5: {"id": "drone_t5", "display": "Tiltrotor Carrier Drone", "capacity": 8, "speed": 8, "upkeep": 48, "size": "medium", "available": True},
    6: {"id": "drone_t6", "display": "Tandem Freighter Drone", "capacity": 16, "speed": 8, "upkeep": 64, "size": "medium", "available": True},
    7: {"id": "drone_t7", "display": "Hybrid Aerostat Drone", "capacity": 32, "speed": 8, "upkeep": 96, "size": "large", "available": True},
    8: {"id": "drone_t8", "display": "Ion-Wind Glider Drone", "capacity": 32, "speed": 16, "upkeep": 128, "size": "large", "available": True},
    9: {"id": "drone_t9", "display": "Superconducting Ring Lifter Drone", "capacity": 64, "speed": 20, "upkeep": 192, "size": "large", "available": True},
    # Creative only (no recipe): four times tier 9's load and speed, no power, small.
    10: {"id": "creative_drone", "display": "Creative Drone", "capacity": 256, "speed": 80, "upkeep": 0, "size": "small", "available": True},
}
MAX_DRONES = 100
SLOTS_PER_PAD = 4

# Plain crafting parts, in the order of JugcraftDrones.PARTS.
DRONE_PARTS = {
    "drone_motor": "Brushless Drone Motor",
    "wooden_propeller_set": "Wooden Propeller Set",
    "aluminum_propeller_set": "Aluminum Propeller Set",
    "rotor_assembly": "Rotor Assembly",
    "aluminum_rotor_assembly": "Aluminum Rotor Assembly",
    "ducted_fan": "Ducted Fan",
    "flight_controller": "Flight Controller",
    "lead_acid_pack": "Lead-Acid Pack",
    "cargo_winch": "Cargo Winch",
    # Tiers 5-9 (see docs/features/drone-depot.md for the real-world basis of each).
    "neodymium_drone_motor": "Neodymium Drone Motor",
    "tilt_rotor_nacelle": "Tilt-Rotor Nacelle",
    "composite_rotor_set": "Composite Rotor Blade Set",
    "composite_rotor_assembly": "Composite Rotor Assembly",
    "hydrogen_lift_cell": "Hydrogen Lift Cell",
    "ion_emitter": "High-Voltage Ion Emitter",
    "superconducting_tape": "Superconducting Tape",
    "stirling_cryocooler": "Stirling Cryocooler",
    "superconducting_motor": "Superconducting Drone Motor",
    "superconducting_lift_fan": "Superconducting Lift Fan",
}

# result: (pattern, key, count). Real-world basis in docs/features/drone-depot.md.
DRONE_CRAFTING = {
    "drone_motor": (["WPW", " I ", " R "], {"W": "#c:wires/copper", "P": "#c:plates/iron", "I": "#c:ingots/iron",
                                            "R": "minecraft:redstone"}, 1),
    "wooden_propeller_set": (["P P", " S ", "P P"], {"P": "#minecraft:planks", "S": "minecraft:stick"}, 1),
    "aluminum_propeller_set": (["P P", " S ", "P P"], {"P": "#c:plates/aluminum", "S": "minecraft:stick"}, 1),
    "rotor_assembly": (["P", "M", "B"], {"P": "jugcraft:wooden_propeller_set", "M": "jugcraft:drone_motor",
                                        "B": "#c:plates/bronze"}, 1),
    "aluminum_rotor_assembly": (["P", "M", "B"], {"P": "jugcraft:aluminum_propeller_set", "M": "jugcraft:drone_motor",
                                                 "B": "#c:plates/bronze"}, 1),
    "ducted_fan": ([" S ", "SRS", " S "], {"S": "#c:plates/steel", "R": "jugcraft:aluminum_rotor_assembly"}, 1),
    "flight_controller": (["WCW", " K "], {"W": "#c:wires/copper", "C": "jugcraft:basic_circuit", "K": "minecraft:compass"}, 1),
    "lead_acid_pack": (["LSL", " W "], {"L": "#c:plates/lead", "S": "jugcraft:sulfur_dust", "W": "#c:wires/copper"}, 1),
    "cargo_winch": (["G", "M", "C"], {"G": "#c:gears/steel", "M": "jugcraft:drone_motor", "C": "minecraft:iron_chain"}, 1),
    "drone_t1": (["RBR", "BFB", "RPR"], {"R": "jugcraft:rotor_assembly", "B": "#c:plates/bronze",
                                        "F": "jugcraft:flight_controller", "P": "jugcraft:lead_acid_pack"}, 1),
    "drone_t2": (["RZR", "IDC", "ZW "], {"R": "jugcraft:rotor_assembly", "Z": "#c:plates/brass", "I": "#c:plates/invar",
                                        "D": "jugcraft:drone_t1", "C": "jugcraft:basic_circuit", "W": "jugcraft:cargo_winch"}, 1),
    "drone_t3": (["RAR", "CDL", "RAR"], {"R": "jugcraft:aluminum_rotor_assembly", "A": "#c:plates/aluminum",
                                        "C": "jugcraft:advanced_circuit", "D": "jugcraft:drone_t2", "L": "jugcraft:lithium_cell"}, 1),
    "drone_t4": (["FLF", "CDU", "FLF"], {"F": "jugcraft:ducted_fan", "L": "jugcraft:lithium_cell",
                                        "C": "jugcraft:advanced_circuit", "D": "jugcraft:drone_t3", "U": "jugcraft:speed_upgrade"}, 1),
    # Neodymium magnets make a lighter, stronger motor (the tiltrotor's tilting nacelles need it).
    "neodymium_drone_motor": (["MWM", "PDP"], {"M": "jugcraft:neodymium_magnet", "W": "#c:wires/copper",
                                              "P": "#c:plates/titanium", "D": "jugcraft:drone_motor"}, 1),
    "tilt_rotor_nacelle": ([" R ", "GNG", " P "], {"R": "jugcraft:aluminum_rotor_assembly", "G": "#c:gears/steel",
                                                  "N": "jugcraft:neodymium_drone_motor", "P": "#c:plates/aluminum"}, 1),
    # Plastic-composite blades: light and stiff enough for the tandem freighter's big rotors.
    "composite_rotor_set": (["S S", " T ", "S S"], {"S": "jugcraft:plastic_sheet", "T": "#c:plates/titanium"}, 1),
    "composite_rotor_assembly": (["B", "M", "G"], {"B": "jugcraft:composite_rotor_set",
                                                  "M": "jugcraft:neodymium_drone_motor", "G": "#c:gears/steel"}, 1),
    # Ion-wind thrust: thin tungsten emitter edges at high voltage, driven by a processor.
    "ion_emitter": (["TTT", "WPW", "AAA"], {"T": "#c:plates/tungsten", "W": "#c:wires/silver", "P": "jugcraft:processor",
                                           "A": "#c:plates/aluminum"}, 1),
    # Rare-earth (ReBCO-style) superconductor deposited on silver tape, kept cold by a Stirling cryocooler.
    "superconducting_tape": (["RCR", "WWW"], {"R": "jugcraft:rare_earth_oxide", "C": "#c:dusts/copper",
                                             "W": "#c:wires/silver"}, 3),
    "stirling_cryocooler": (["APA", "GDG", "APA"], {"A": "#c:plates/aluminum", "P": "#c:plates/copper",
                                                   "G": "#c:gears/steel", "D": "jugcraft:drone_motor"}, 1),
    "superconducting_motor": (["TMT", "TCT", "PXP"], {"T": "jugcraft:superconducting_tape", "M": "jugcraft:neodymium_magnet",
                                                     "C": "jugcraft:stirling_cryocooler", "P": "#c:plates/titanium",
                                                     "X": "jugcraft:processor"}, 1),
    "superconducting_lift_fan": (["TFT", "TMT"], {"T": "#c:plates/titanium", "F": "jugcraft:composite_rotor_set",
                                                 "M": "jugcraft:superconducting_motor"}, 1),
    "drone_t5": (["NAN", "CDP", "LWL"], {"N": "jugcraft:tilt_rotor_nacelle", "A": "#c:plates/aluminum", "C": "jugcraft:processor",
                                        "D": "jugcraft:drone_t4", "P": "jugcraft:plastic_sheet", "L": "jugcraft:lithium_cell",
                                        "W": "jugcraft:cargo_winch"}, 1),
    "drone_t6": (["RTR", "CDC", "LWL"], {"R": "jugcraft:composite_rotor_assembly", "T": "#c:plates/titanium",
                                        "C": "jugcraft:processor", "D": "jugcraft:drone_t5", "L": "jugcraft:lithium_cell",
                                        "W": "jugcraft:cargo_winch"}, 1),
    "drone_t7": (["HHH", "FDF", "CLC"], {"H": "jugcraft:hydrogen_lift_cell", "F": "jugcraft:ducted_fan",
                                        "D": "jugcraft:drone_t6", "C": "jugcraft:processor", "L": "jugcraft:lithium_cell"}, 1),
    "drone_t8": (["EEE", "FDF", "TCT"], {"E": "jugcraft:ion_emitter", "F": "jugcraft:ducted_fan", "D": "jugcraft:drone_t7",
                                        "T": "#c:plates/titanium", "C": "jugcraft:processor"}, 1),
    # The Ring Lifter: six superconducting lift fans round the hull, one battery bank.
    "drone_t9": (["FCF", "FDF", "FBF"], {"F": "jugcraft:superconducting_lift_fan", "C": "jugcraft:processor",
                                        "D": "jugcraft:drone_t8", "B": "jugcraft:lithium_battery_bank"}, 1),
    "drone_depot_terminal": (["PGP", "AMA", "PCP"], {"P": "#c:plates/aluminum", "G": "minecraft:glass",
                                                    "A": "jugcraft:advanced_circuit", "M": "jugcraft:machine_casing",
                                                    "C": "jugcraft:copper_cable"}, 1),
    "control_screen": (["PGP", "PCP"], {"P": "#c:plates/iron", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit"}, 3),
    "holo_table": (["GGG", "PCP", "P P"], {"G": "minecraft:glass_pane", "P": "#c:plates/iron", "C": "jugcraft:basic_circuit"}, 3),
    # The landing field (the Drone Tower lays these itself; these are for repairs and extensions).
    "landing_platform": (["SCS", "CSC", "SCS"], {"S": "#c:plates/steel", "C": "minecraft:smooth_stone"}, 8),
    "landing_pad": (["YBY", "SRS"], {"Y": "minecraft:yellow_dye", "B": "minecraft:black_dye", "S": "#c:plates/steel",
                                     "R": "minecraft:redstone"}, 6),
    "supply_pickup": (["PTP", "SWS"], {"P": "#c:plates/steel", "T": "minecraft:iron_trapdoor", "S": "#c:plates/iron",
                                       "W": "jugcraft:cargo_winch"}, 9),
    "cargo_packager": (["PHP", "CMC", "PWP"], {"P": "#c:plates/steel", "H": "minecraft:hopper", "C": "minecraft:chest",
                                              "M": "jugcraft:machine_casing", "W": "jugcraft:cargo_winch"}, 1),
}

# Chat and tooltip text (drone/*.java).
DRONE_LANG = {
    "container.jugcraft.cargo_packager": "Cargo Packager",
    "tooltip.jugcraft.drone": "Tier %s · %s blocks/trip · %s blocks/s · %s JE/t · %s",
    "tooltip.jugcraft.drone.size.small": "small (4 per pad)",
    "tooltip.jugcraft.drone.size.medium": "medium (2 per pad)",
    "tooltip.jugcraft.drone.size.large": "large (1 per pad)",
    "message.jugcraft.drone.no_depot": "This pad is not part of a working drone depot",
    "message.jugcraft.drone.not_owner": "Only this depot's owner can do that",
    "message.jugcraft.drone.claimed": "You now own this Drone Depot Terminal",
    "message.jugcraft.drone.link.ok": "Drone linked (%s/%s drones, %s/%s pad slots)",
    "message.jugcraft.drone.link.not_available": "This drone tier can't be used yet",
    "message.jugcraft.drone.link.fleet_full": "Fleet full (%s/%s drones)",
    "message.jugcraft.drone.link.no_pad_room": "No pad room: %3$s/%4$s slots used. Add another 5x5 landing pad (at least one block from the others)",
    "message.jugcraft.drone.unlinked": "Drone returned (%s left)",
    "message.jugcraft.drone.none_docked": "No docked drone to return",
    "message.jugcraft.drone.mode.personal": "Depot set to PERSONAL: builds only your blueprints, from your storage",
    "message.jugcraft.drone.mode.party": "Depot set to PARTY: also builds party members' Party blueprints",
    "message.jugcraft.drone.mode_name.personal": "PERSONAL",
    "message.jugcraft.drone.mode_name.party": "PARTY",
    "message.jugcraft.drone.status.title": "▌DRONE DEPOT ▸ %s",
    "message.jugcraft.drone.status.platform.ok": "▸ Platform %sx%s · %s pads · %s/%s slots",
    "message.jugcraft.drone.status.platform.no_platform": "▸ Platform: none found below the terminal",
    "message.jugcraft.drone.status.platform.too_small": "▸ Platform %sx%s: too small (min 9x9)",
    "message.jugcraft.drone.status.platform.no_pad": "▸ Platform %sx%s: place a complete 5x5 of landing pads on top",
    "message.jugcraft.drone.status.platform.too_large": "▸ Platform: larger than 64x64 blocks",
    "message.jugcraft.drone.status.fleet": "▸ Fleet %s/%s drones · %s flying",
    "message.jugcraft.drone.status.tiers": "▸ Drones: %s",
    "message.jugcraft.drone.status.power": "▸ Power %s/%s JE · draw %s JE/t (working %s, standby %s)",
    "message.jugcraft.drone.status.power_low": "⚠ LOW POWER %s/%s JE · needs %s JE/t (working %s, standby %s)",
    "message.jugcraft.drone.status.packager": "▸ Cargo packager connected",
    "message.jugcraft.drone.status.no_packager": "⚠ No cargo packager next to the supply pickup",
    "message.jugcraft.drone.status.no_pickup": "⚠ No supply pickup: place a 3x3 of supply pickup plates on the platform",
    "message.jugcraft.drone.status.jobs": "▸ Open build positions %s · skipped under cover %s",
    "message.jugcraft.drone.status.missing": "⚠ Missing materials: %s",
}


# Hydrogen from the electrolytic cell, sealed into plastic gas cells in the chemical reactor.
DRONE_FLUID_RECIPES = {
    "chemical_reactor": [
        {"name": "hydrogen_lift_cell", "items": [("jugcraft:plastic_sheet", 2)], "fluids": [("jugcraft:hydrogen", 1000)],
         "results": [("jugcraft:hydrogen_lift_cell", 1)], "ticks": 120, "features": ["drones", "salt", "crude_oil"]},
    ],
}

# Feature switches behind Jugcraft items used in drone recipes that aren't plain metal forms (their own
# switches gate the recipe too); drone parts inherit the switches of what they're made from.
ITEM_FEATURES = {
    "neodymium_magnet": ["rare_earths"], "rare_earth_oxide": ["rare_earths"], "plastic_sheet": ["crude_oil"],
    "processor": ["silicon"], "lithium_cell": ["lithium"], "lithium_battery_bank": ["lithium", "titanium"],
    "hydrogen_lift_cell": ["salt", "crude_oil"], "sulfur_dust": ["sulfur"], "lithium_carbonate": ["lithium"],
}


def drone_blocks():
    return list(DRONE_BLOCKS)


def drone_items():
    return list(DRONE_PARTS) + [tier["id"] for tier in DRONE_TIERS.values()]
