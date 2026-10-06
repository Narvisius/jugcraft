"""Nuclear power, stage 1 (docs/features/nuclear.md). Radiation protection: the hazmat suit's material sheet
(client/HazmatLayer.java draws the suit in 3D from it), the Geiger counter's 3D item model, shielding concrete and the
radiation damage type. Fuel: the enriched uranium cylinder, fuel assemblies and spent fuel (the Gas Centrifuge Cascade
and Fuel Fabricator are machines: tools/machines.py, models here). The reactor unit: the working equipment (placed
whole from one item, drawn in 3D by client/NuclearRenderer.java from the material sheet made here), the three nuclear
pipes and the link card. Names, models, loot, recipes, tags; tools/check_mod_data.py checks the Java matches."""
import math
import os
import random
import subprocess

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

MOD = "jugcraft"
FEATURE = "machines"

HAZMAT = ["hazmat_hood", "hazmat_coverall", "hazmat_trousers", "hazmat_boots"]
FUEL_ITEMS = ["enriched_uranium_cylinder", "leu_fuel_assembly", "spent_fuel_assembly", "reactor_link_card"]
ITEMS = HAZMAT + ["geiger_counter", "check_source"] + FUEL_ITEMS
SHIELDING_TAG = "radiation_shielding"

# The reactor unit's working equipment: id: (name, wide, deep, tall). Same order as EquipmentKind.java.
EQUIPMENT = {
    "reactor_pressure_vessel": ("Reactor Pressure Vessel", 7, 7, 20),
    "reactor_steam_generator": ("Nuclear Steam Generator", 7, 7, 21),
    "reactor_coolant_pump": ("Reactor Coolant Pump", 5, 3, 10),
    "pressurizer": ("Pressurizer", 5, 3, 18),
    "turbine_generator": ("Turbine-Generator", 11, 50, 10),
    "condenser": ("Condenser", 12, 6, 6),
    "feedwater_pump": ("Feedwater Pump", 3, 2, 3),
    "main_transformer": ("Main Transformer", 5, 5, 8),
    "reactor_control_console": ("Reactor Control Console", 3, 2, 2),
    # The pre-made plants' own reactors (tools/nuclear_plants.py).
    "epr_reactor_vessel": ("EPR Reactor Pressure Vessel", 8, 8, 22),
    "small_modular_reactor": ("Small Modular Reactor", 5, 5, 22),
    "small_turbine_generator": ("Small Turbine-Generator", 3, 5, 4),
    "circulating_water_pump": ("Circulating Water Pump", 3, 3, 6),
    # Plant equipment that only has to look right (the plants' buildings): drawn by the client, placed whole.
    "polar_crane": ("Polar Crane", 40, 5, 5),
    "refueling_machine": ("Refueling Machine", 15, 4, 8),
    "overhead_crane": ("Overhead Crane", 40, 5, 4),
    "moisture_separator_reheater": ("Moisture Separator Reheater", 5, 20, 5),
    "feedwater_heater": ("Feedwater Heater", 3, 12, 3),
    "emergency_diesel_generator": ("Emergency Diesel Generator", 5, 14, 5),
    "storage_tank": ("Storage Tank", 5, 5, 9),
    "heat_exchanger": ("Heat Exchanger", 3, 9, 3),
    "pump_skid": ("Pump Skid", 3, 5, 3),
    "fuel_handling_machine": ("Fuel Handling Machine", 14, 5, 7),
    "spent_fuel_rack": ("Spent Fuel Rack", 4, 4, 4),
    "dry_storage_cask": ("Dry Storage Cask", 3, 3, 6),
    "travelling_water_screen": ("Travelling Water Screen", 6, 2, 9),
    "plant_display_wall": ("Plant Display Wall", 9, 1, 4),
    "main_control_board": ("Main Control Board", 6, 2, 3),
    "switchgear_lineup": ("Switchgear Lineup", 8, 2, 3),
    "air_handling_unit": ("Air Handling Unit", 4, 8, 3),
}
# The cooling tower controller: one block, three tower kinds (CoolingTowerBlock.Tower).
TOWER_CONTROLLER = "cooling_tower_controller"
TOWERS = ["natural", "large", "mechanical"]
# The nuclear pipes: id: (name, thickness in pixels). Same order as PipeKind.java.
PIPES = {
    "reactor_coolant_pipe": ("Reactor Coolant Pipe", 10),
    "insulated_steam_line": ("Insulated Steam Line", 10),
    "circulating_water_conduit": ("Circulating Water Conduit", 14),
    "isolated_phase_bus": ("Isolated Phase Bus", 12),
}
FRAME = "nuclear_equipment_frame"
# Plain blocks of the cooling system: the tower fill and the buried conduit (a full block that joins like a conduit).
COOLING_BLOCKS = {"cooling_tower_fill": "Cooling Tower Fill", "conduit_trench": "Conduit Trench"}
import plant_interior

BLOCKS = ["shielding_concrete"] + list(EQUIPMENT) + list(PIPES) + [TOWER_CONTROLLER] + list(COOLING_BLOCKS) + plant_interior.BLOCKS

NAMES = {
    "hazmat_hood": "Hazmat Hood",
    "hazmat_coverall": "Hazmat Coverall",
    "hazmat_trousers": "Hazmat Trousers",
    "hazmat_boots": "Hazmat Boots",
    "geiger_counter": "Geiger Counter",
    "check_source": "Check Source",
    "shielding_concrete": "Shielding Concrete",
    "enriched_uranium_cylinder": "Enriched Uranium Cylinder",
    "leu_fuel_assembly": "LEU Fuel Assembly",
    "spent_fuel_assembly": "Spent Fuel Assembly",
    "reactor_link_card": "Reactor Link Card",
    TOWER_CONTROLLER: "Cooling Tower Controller",
    **COOLING_BLOCKS,
}
NAMES.update({k: v[0] for k, v in EQUIPMENT.items()})
NAMES.update({k: v[0] for k, v in PIPES.items()})

LANG = {
    "tooltip.jugcraft.hazmat": "Each piece stops 22.5% of nuclear radiation (90% for the full suit)",
    "tooltip.jugcraft.geiger_counter": "Clicks faster near radiation; shows your dose rate and total dose",
    "tooltip.jugcraft.check_source": "A weak sealed source for testing a Geiger counter",
    "tooltip.jugcraft.enriched_uranium_cylinder": "Low-enriched uranium hexafluoride, ready for the Fuel Fabricator",
    "tooltip.jugcraft.leu_fuel_assembly": "Standard reactor fuel. A full core is 193 assemblies and runs about a day at full power",
    "tooltip.jugcraft.spent_fuel_assembly": "Deadly radioactive: carry it in a full hazmat suit, and store it behind shielding",
    "tooltip.jugcraft.reactor_link_card": "Use on a reactor vessel, then on a Reactor Control Console to link them",
    "tooltip.jugcraft.nuclear.size": "%s wide, %s deep, %s tall (placed to your right and away from you)",
    "tooltip.jugcraft.nuclear.reactor_pressure_vessel": "Holds the core. Joins its unit through Reactor Coolant Pipe",
    "tooltip.jugcraft.nuclear.reactor_steam_generator": "Boils the steam. Each one (with a coolant pump) carries a quarter of the heat",
    "tooltip.jugcraft.nuclear.reactor_coolant_pump": "Drives the primary coolant: one per steam generator",
    "tooltip.jugcraft.nuclear.pressurizer": "Keeps the primary loop at 15.5 MPa so the coolant can't boil",
    "tooltip.jugcraft.nuclear.turbine_generator": "Steam in, power out. Needs a condenser and a main transformer touching it",
    "tooltip.jugcraft.nuclear.condenser": "Turns used steam back into water: 4,000 mB/t of cooling water at full power",
    "tooltip.jugcraft.nuclear.feedwater_pump": "Sends the condensed water back to the steam generators (on the steam line)",
    "tooltip.jugcraft.nuclear.main_transformer": "The unit's power comes out here, into cables or a Unit Substation",
    "tooltip.jugcraft.nuclear.reactor_control_console": "Runs the reactor. Link it to the vessel with a Reactor Link Card",
    "tooltip.jugcraft.nuclear.epr_reactor_vessel": "The EPR's larger core: 241 assemblies and 1.6 times the output. Needs two turbine-generators to use it all",
    "tooltip.jugcraft.nuclear.small_modular_reactor": "Reactor, steam generator and pressurizer in one module: 37 assemblies, an eighth of a full unit. Steam line to a Small Turbine-Generator",
    "tooltip.jugcraft.nuclear.circulating_water_pump": "Moves the cooling water round the cooling towers: a tower only works with these on its conduit (each one carries half a unit's heat)",
    "tooltip.jugcraft.nuclear.polar_crane": "The containment's round-the-dome crane (for building your own plant: it only looks the part)",
    "tooltip.jugcraft.nuclear.refueling_machine": "The gantry that lifts fuel out of the open reactor (for show)",
    "tooltip.jugcraft.nuclear.overhead_crane": "The turbine hall's bridge crane (for show)",
    "tooltip.jugcraft.nuclear.moisture_separator_reheater": "Dries and reheats the steam between the turbine stages (for show)",
    "tooltip.jugcraft.nuclear.feedwater_heater": "Warms the feedwater with steam bled from the turbine (for show)",
    "tooltip.jugcraft.nuclear.emergency_diesel_generator": "Backup power for the safety systems if the grid is lost (for show)",
    "tooltip.jugcraft.nuclear.storage_tank": "A plant water tank: refuelling water, condensate, demineralised water (for show)",
    "tooltip.jugcraft.nuclear.heat_exchanger": "A component cooling heat exchanger (for show)",
    "tooltip.jugcraft.nuclear.pump_skid": "A plant pump and motor on its baseplate (for show)",
    "tooltip.jugcraft.nuclear.fuel_handling_machine": "The bridge that moves fuel round the spent fuel pool (for show)",
    "tooltip.jugcraft.nuclear.spent_fuel_rack": "Holds used fuel under water; the water glows blue over it (for show)",
    "tooltip.jugcraft.nuclear.dry_storage_cask": "A ventilated concrete cask for cooled spent fuel (for show)",
    "tooltip.jugcraft.nuclear.travelling_water_screen": "Strains fish and debris out of the cooling water intake (for show)",
    "tooltip.jugcraft.nuclear.plant_display_wall": "The control room's large plant overview display (for show)",
    "tooltip.jugcraft.nuclear.main_control_board": "A control room benchboard with its mimic, meters and annunciators (for show)",
    "tooltip.jugcraft.nuclear.switchgear_lineup": "A line of medium-voltage breakers (for show)",
    "tooltip.jugcraft.nuclear.air_handling_unit": "Filters and moves a building's air (for show)",
    "tooltip.jugcraft.nuclear.small_turbine_generator": "One module's turbine, condenser and generator in one: cooling water in, power out into cables",
    "block.jugcraft.cooling_tower_controller": "Cooling Tower Controller",
    "message.jugcraft.nuclear.tower_status": "Cooling tower: %s%% busy (takes %s of a standard unit's heat)",
    "message.jugcraft.nuclear.tower_incomplete": "Cooling tower incomplete: build the whole tower around this controller",
    "block.jugcraft.nuclear_equipment_frame": "Nuclear Equipment",
    "death.attack.jugcraft.radiation": "%1$s died of radiation sickness",
    "death.attack.jugcraft.radiation.player": "%1$s died of radiation sickness while fighting %2$s",
    "subtitles.jugcraft.geiger_counter": "Geiger counter clicks",
    "message.jugcraft.nuclear.no_room": "Not enough room: it needs %s wide, %s deep and %s tall, clear",
    "message.jugcraft.nuclear.console_unlinked": "Not linked: use a Reactor Link Card on the vessel, then on this console",
    "message.jugcraft.nuclear.console_linked": "Console linked to the reactor",
    "message.jugcraft.nuclear.card_set": "Link card set to this reactor: now use it on a console",
    "message.jugcraft.nuclear.card_empty": "Use the card on a reactor vessel first",
    "message.jugcraft.nuclear.not_owner": "This reactor belongs to someone else",
    "message.jugcraft.nuclear.owner_only": "Only the owner can change who may use it",
    "message.jugcraft.nuclear.vessel_status": "Reactor: %s%% power, %s °C, %s of %s assemblies loaded",
    "message.jugcraft.nuclear.vessel_closed": "The vessel is closed: open it for refuelling from the console",
    "message.jugcraft.nuclear.loaded": "Assembly loaded (%s of %s)",
    "message.jugcraft.nuclear.unloaded": "Assembly taken out (%s of %s left)",
    "message.jugcraft.nuclear.core_full": "The core is full",
    "message.jugcraft.nuclear.spent_no": "Spent fuel can't go back in: store it behind shielding",
    "message.jugcraft.nuclear.core_empty": "The core is empty",
    "message.jugcraft.nuclear.cannot_reset": "Can't reset: the core must be below 350 °C (and its fuel undamaged)",
    "message.jugcraft.nuclear.refuel_not_shut_down": "Shut down first: rods fully in and the core below 120 °C",
    "message.jugcraft.nuclear.condenser_water": "Cooling water: %s of %s mB",
    "message.jugcraft.nuclear.showcase": "Built a running reactor unit (vessel at %s)",
    "message.jugcraft.nuclear.built": "Built %s: %s blocks, %s pieces of equipment, %s reactor(s), in %s s",
}

R = "screen.jugcraft.reactor."
CONSOLE_LANG = {
    R + "title": "REACTOR CONTROL", R + "out_of_range": "Linked reactor at %s is out of range: come closer", R + "unlinked": "Not linked: use a Reactor Link Card on the vessel, then on this console",
    R + "header": "UNIT 1  ·  %s-LOOP PWR  ·  LEU",
    R + "header_epr": "UNIT 1  ·  EPR  ·  LEU", R + "header_smr": "MODULE  ·  SMALL MODULAR REACTOR  ·  LEU",
    R + "status.incomplete": "UNIT INCOMPLETE", R + "status.scram": "SCRAM", R + "status.refuelling": "REFUELLING",
    R + "status.operating": "OPERATING", R + "status.shut_down": "SHUT DOWN",
    R + "core_map": "CORE MAP  ·  ROD BANKS", R + "core_legend": "%s / %s assemblies",
    R + "rods": "RODS", R + "rods_in": "%s%% IN", R + "plant": "PLANT",
    R + "thermal": "THERMAL POWER", R + "electric": "ELECTRIC", R + "core_temp": "CORE TEMP", R + "coolant": "COOLANT",
    R + "steam": "STEAM", R + "steam_value": "%s/4 SG  ·  %s%%", R + "cooling_water": "COOLING WATER",
    R + "trend": "CORE TEMPERATURE  ·  10 MIN", R + "scram_line": "SCRAM %s", R + "annunciators": "ANNUNCIATORS",
    R + "ann.high_temp": "CORE TEMP HI", R + "ann.scram": "REACTOR TRIP", R + "ann.low_water": "CW FLOW LOW",
    R + "ann.loops": "LOOPS DOWN", R + "ann.no_power_out": "NO GEN OUTPUT", R + "ann.protection_off": "RPS BYPASSED",
    R + "ann.fuel_spent": "FUEL SPENT", R + "ann.fuel_damaged": "FUEL DAMAGE", R + "ann.not_formed": "UNIT INCOMPLETE",
    R + "ann.refuel": "HEAD REMOVED", R + "ann.no_fuel": "CORE EMPTY", R + "ann.rad_monitor": "RAD MONITOR",
    R + "protection": "PROTECTION", R + "rps": "REACTOR PROTECTION", R + "on": "ON", R + "off": "OFF",
    R + "reset": "RESET TRIP", R + "mode_personal": "PERSONAL", R + "mode_party": "PARTY",
    R + "fuel": "FUEL", R + "fuel_line": "LEU · %s assemblies · %s fresh · %s spent · burnup %s%%",
    R + "refuel": "REFUEL", R + "refuel_close": "CLOSE HEAD",
}

CRAFTING = {
    "hazmat_hood": (["WWW", "WGW", "PIP"], {"W": "minecraft:yellow_wool", "G": "minecraft:glass_pane",
                                             "P": "jugcraft:plastic_sheet", "I": "#c:ingots/iron"}, 1),
    "hazmat_coverall": (["W W", "PSP", "WWW"], {"W": "minecraft:yellow_wool", "P": "jugcraft:plastic_sheet",
                                                 "S": "#c:plates/steel"}, 1),
    "hazmat_trousers": (["WPW", "W W", "W W"], {"W": "minecraft:yellow_wool", "P": "jugcraft:plastic_sheet"}, 1),
    "hazmat_boots": (["P P", "W W"], {"W": "minecraft:yellow_wool", "P": "jugcraft:plastic_sheet"}, 1),
    "geiger_counter": (["ICI", "IRI", "IGI"], {"I": "#c:ingots/iron", "C": "minecraft:copper_ingot",
                                               "R": "minecraft:redstone", "G": "minecraft:glass_pane"}, 1),
    "shielding_concrete": (["CCC", "CLC", "CCC"], {"C": "minecraft:gray_concrete", "L": "#c:ingots/lead"}, 8),
    "reactor_link_card": ([" P ", "PCP", " P "], {"P": "jugcraft:plastic_sheet", "C": "jugcraft:advanced_circuit"}, 1),
    # The equipment: heavy steel, with the reactor's own parts needing tungsten and the electrics copper and circuits.
    "reactor_pressure_vessel": (["TCT", "BFB", "BBB"], {"T": "#c:plates/tungsten", "C": "jugcraft:processor",
                                                       "B": "#c:storage_blocks/steel", "F": "jugcraft:leu_fuel_assembly"}, 1),
    "reactor_steam_generator": (["SBS", "TCT", "SBS"], {"S": "#c:plates/steel", "B": "#c:storage_blocks/steel",
                                                       "T": "#c:plates/nickel", "C": "minecraft:copper_block"}, 1),
    "reactor_coolant_pump": (["SMS", "BPB", "SBS"], {"S": "#c:plates/steel", "M": "jugcraft:electric_motor",
                                                    "B": "#c:storage_blocks/steel", "P": "minecraft:piston"}, 1),
    "pressurizer": (["SBS", "SRS", "SBS"], {"S": "#c:plates/steel", "B": "#c:storage_blocks/steel",
                                           "R": "minecraft:redstone_block"}, 1),
    "turbine_generator": (["BBB", "GMG", "CCC"], {"B": "#c:storage_blocks/steel", "G": "#c:gears/steel",
                                                 "M": "jugcraft:electric_motor", "C": "minecraft:copper_block"}, 1),
    "condenser": (["SPS", "PBP", "SPS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe",
                                         "B": "#c:storage_blocks/steel"}, 1),
    "feedwater_pump": (["SMS", "P P", "SSS"], {"S": "#c:plates/steel", "M": "jugcraft:electric_motor",
                                              "P": "jugcraft:steel_fluid_pipe"}, 1),
    "main_transformer": (["IAI", "CBC", "SSS"], {"I": "minecraft:white_terracotta", "A": "jugcraft:aluminum_cable",
                                                "C": "minecraft:copper_block", "B": "#c:storage_blocks/steel",
                                                "S": "#c:plates/steel"}, 1),
    "reactor_control_console": (["GGG", "CPC", "SSS"], {"G": "minecraft:glass_pane", "C": "jugcraft:advanced_circuit",
                                                       "P": "jugcraft:processor", "S": "#c:plates/steel"}, 1),
    "epr_reactor_vessel": (["TCT", "BVB", "TBT"], {"T": "#c:plates/tungsten", "C": "jugcraft:processor",
                                                "B": "#c:storage_blocks/steel", "V": "jugcraft:reactor_pressure_vessel"}, 1),
    "small_modular_reactor": (["SCS", "TFT", "SBS"], {"S": "#c:plates/steel", "C": "jugcraft:processor", "T": "#c:plates/tungsten",
                                                    "F": "jugcraft:leu_fuel_assembly", "B": "#c:storage_blocks/steel"}, 1),
    "small_turbine_generator": (["SGS", "PMP", "SCS"], {"S": "#c:plates/steel", "G": "#c:gears/steel", "P": "jugcraft:steel_fluid_pipe",
                                                      "M": "jugcraft:electric_motor", "C": "minecraft:copper_block"}, 1),
    "circulating_water_pump": (["SMS", "PGP", "SBS"], {"S": "#c:plates/steel", "M": "jugcraft:electric_motor", "P": "jugcraft:steel_fluid_pipe",
                                                     "G": "#c:gears/steel", "B": "#c:storage_blocks/steel"}, 1),
    "polar_crane": (["SSS", "GMG", "C C"], {"S": "#c:storage_blocks/steel", "G": "#c:gears/steel", "M": "jugcraft:electric_motor", "C": "minecraft:iron_chain"}, 1),
    "refueling_machine": (["SMS", "S S", "SCS"], {"S": "#c:plates/steel", "M": "jugcraft:electric_motor", "C": "jugcraft:processor"}, 1),
    "overhead_crane": (["SSS", "GMG", " C "], {"S": "#c:storage_blocks/steel", "G": "#c:gears/steel", "M": "jugcraft:electric_motor", "C": "minecraft:iron_chain"}, 1),
    "moisture_separator_reheater": (["WSW", "PBP", "WSW"], {"W": "minecraft:white_wool", "S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "B": "#c:storage_blocks/steel"}, 1),
    "feedwater_heater": (["SPS", "PCP", "SPS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "C": "minecraft:copper_ingot"}, 1),
    "emergency_diesel_generator": (["SPS", "GMC", "SSS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "G": "#c:gears/steel", "M": "jugcraft:electric_motor", "C": "minecraft:copper_block"}, 1),
    "storage_tank": (["SSS", "S S", "SSS"], {"S": "#c:plates/steel"}, 1),
    "heat_exchanger": (["SPS", "CCC", "SPS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "C": "minecraft:copper_ingot"}, 1),
    "pump_skid": (["PMP", "SSS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "M": "jugcraft:electric_motor"}, 1),
    "fuel_handling_machine": (["SMS", "S S", "SCS"], {"S": "#c:plates/steel", "M": "jugcraft:electric_motor", "C": "jugcraft:advanced_circuit"}, 1),
    "spent_fuel_rack": (["SBS", "B B", "SBS"], {"S": "#c:plates/steel", "B": "minecraft:iron_bars"}, 1),
    "dry_storage_cask": (["CSC", "C C", "CSC"], {"C": "jugcraft:shielding_concrete", "S": "#c:plates/steel"}, 1),
    "travelling_water_screen": (["SMS", "BBB", "BBB"], {"S": "#c:plates/steel", "M": "jugcraft:electric_motor", "B": "minecraft:iron_bars"}, 1),
    "plant_display_wall": (["GGG", "CPC", "SSS"], {"G": "minecraft:glass_pane", "C": "jugcraft:advanced_circuit", "P": "jugcraft:processor", "S": "#c:plates/steel"}, 1),
    "main_control_board": (["RGR", "CCC", "SSS"], {"R": "minecraft:redstone_lamp", "G": "minecraft:glass_pane", "C": "jugcraft:advanced_circuit", "S": "#c:plates/steel"}, 1),
    "switchgear_lineup": (["SRS", "CAC", "SSS"], {"S": "#c:plates/steel", "R": "minecraft:redstone", "C": "minecraft:copper_ingot", "A": "jugcraft:aluminum_cable"}, 1),
    "air_handling_unit": (["SBS", "WMW", "SSS"], {"S": "#c:plates/steel", "B": "minecraft:iron_bars", "W": "minecraft:white_wool", "M": "jugcraft:electric_motor"}, 1),
    "cooling_tower_fill": (["PPP", "SSS", "PPP"], {"P": "jugcraft:plastic_sheet", "S": "minecraft:stick"}, 8),
    "conduit_trench": (["CCC", "PPP", "CCC"], {"C": "minecraft:smooth_stone", "P": "jugcraft:circulating_water_conduit"}, 3),
    TOWER_CONTROLLER: (["CPC", "PAP", "CPC"], {"C": "minecraft:gray_concrete", "P": "jugcraft:steel_fluid_pipe",
                                              "A": "jugcraft:advanced_circuit"}, 1),
    "reactor_coolant_pipe": (["SSS", "   ", "SSS"], {"S": "#c:plates/steel"}, 6),
    "insulated_steam_line": (["WWW", "SSS", "WWW"], {"W": "minecraft:white_wool", "S": "#c:plates/steel"}, 6),
    "circulating_water_conduit": (["CCC", "S S", "CCC"], {"C": "minecraft:gray_concrete", "S": "#c:plates/steel"}, 6),
    "isolated_phase_bus": (["AAA", "CCC", "AAA"], {"A": "#c:plates/aluminum", "C": "minecraft:copper_ingot"}, 6),
}
CRAFTING.update(plant_interior.CRAFTING)
ITEM_FEATURES = {"plastic_sheet": ["crude_oil"], "processor": ["silicon"], "advanced_circuit": ["silver", "nickel"],
                 "leu_fuel_assembly": ["uranium"], "aluminum_cable": ["aluminum"]}
BASE_METALS = ("copper", "iron", "gold", "steel")

# The textures made here at 64x64 (tools/check_mod_data.py allows these by name).
NU_TEXTURES = ["nu_centrifuge", "nu_cap", "nu_steel", "nu_floor", "nu_cabinet", "nu_glass", "nu_hazard", "nu_pipe_blue",
               "nu_pipe_orange"]
PIPE_TEXTURES = list(PIPES) + [p + "_end" for p in PIPES]
TOWER_TEXTURES = [f"{TOWER_CONTROLLER}_side", f"{TOWER_CONTROLLER}_top", f"{TOWER_CONTROLLER}_grate", "cooling_tower_fill",
                  "cooling_tower_fill_side", "conduit_trench"]
HI_RES = (set(ITEMS) - {"geiger_counter"}) | set(BLOCKS) | set(NU_TEXTURES) | set(PIPE_TEXTURES) | set(TOWER_TEXTURES) | {
    "geiger_case", "geiger_screen", "geiger_rubber", "geiger_steel", "geiger_grille", "geiger_label"} | plant_interior.HI_RES


def recipe_features(result, feature_of):
    found = set()
    for ref in CRAFTING[result][1].values():
        if ref.startswith("#c:") and ref.split("/")[0] in ("#c:plates", "#c:ingots", "#c:storage_blocks", "#c:gears"):
            metal = ref.split("/")[-1]
            if metal not in BASE_METALS:
                found.add(feature_of(f"{metal}_ingot"))
        elif ref.startswith(f"{MOD}:"):
            found.update(ITEM_FEATURES.get(ref.split(":")[1], []))
    return found - {FEATURE}


# ---------------------------------------------------------------- drawing helpers

def _rng(seed):
    return np.random.default_rng(seed)


def _smooth_noise(size, seed, scale):
    """Value noise: random values on a coarse grid, smoothly enlarged."""
    r = _rng(seed)
    small = max(2, size // scale)
    img = Image.fromarray((r.random((small, small)) * 255).astype(np.uint8), "L").resize((size, size), Image.BICUBIC)
    return np.asarray(img, dtype=np.float32) / 255 - 0.5


def _surface(base, seed, size=64, fine=8, broad=14, grain=None):
    """A realistic surface: broad mottling, fine speckle, and optional streaks ('h' or 'v')."""
    r = _rng(seed)
    a = np.zeros((size, size, 4), dtype=np.float32)
    a[..., :3] = base[:3]
    a[..., 3] = 255
    shade = _smooth_noise(size, seed, 8) * broad + (r.random((size, size)) - 0.5) * fine
    if grain == "h":
        shade += (r.random((size, 1)) - 0.5) * fine
    elif grain == "v":
        shade += (r.random((1, size)) - 0.5) * fine
    a[..., :3] += shade[..., None]
    return np.clip(a, 0, 255)


def _img(a):
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def _shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def trefoil(d, cx, cy, size, fg=(18, 18, 20, 255), bg=(244, 200, 36, 255), ring=True):
    """The radiation trefoil on a yellow disc."""
    if ring:
        d.ellipse([cx - size, cy - size, cx + size, cy + size], fill=bg, outline=fg, width=max(1, size // 9))
    r0, r1 = size * 0.2, size * 0.8
    for k in range(3):
        a = -90 + k * 120
        d.pieslice([cx - r1, cy - r1, cx + r1, cy + r1], a - 30, a + 30, fill=fg)
    d.ellipse([cx - r0 * 1.45, cy - r0 * 1.45, cx + r0 * 1.45, cy + r0 * 1.45], fill=bg)
    d.ellipse([cx - r0, cy - r0, cx + r0, cy + r0], fill=fg)


def _supersample(draw, size=64, factor=4):
    """Draw at factor x size and shrink, for smooth edges."""
    big = Image.new("RGBA", (size * factor, size * factor), (0, 0, 0, 0))
    draw(big, ImageDraw.Draw(big), factor)
    return big.resize((size, size), Image.LANCZOS)


# ---------------------------------------------------------------- the suit's material sheet (256x256, 64 px cells)

# The suit's material sheet: 64 px cells, four a row (tools/nuclear_materials.py draws them).
SHEET = 256
CELL = 64
# Same order as the regions in client/HazmatLayer.java (col, row).
HAZMAT_MATERIALS = ["fabric", "fabric_dark", "rubber", "tape",
                    "visor", "tank", "steel", "filter",
                    "trefoil", "badge", "sole", "zip",
                    "tank_band"]
SUIT = (226, 178, 40)


def slot(name):
    index = HAZMAT_MATERIALS.index(name)
    return index % 4, index // 4


def _fabric(base, seed):
    """Coated fabric: a fine weave, soft creases and a taped seam near one edge."""
    a = _surface(base, seed, fine=6, broad=16)
    y, x = np.mgrid[0:CELL, 0:CELL]
    a[..., :3] += (((x + y) % 2) * 3 - 1.5)[..., None]          # weave
    crease = np.sin((x * 0.35 + y * 0.9) / 3.0 + _smooth_noise(CELL, seed + 1, 6) * 6)
    a[..., :3] += (np.clip(crease, 0.75, 1) - 0.75)[..., None] * 60  # highlights on creases
    a[..., :3] -= (np.clip(-crease, 0.85, 1) - 0.85)[..., None] * 90
    img = _img(np.clip(a, 0, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 58, CELL, 61], fill=_shade(base, -26))      # seam tape
    d.line([(0, 57), (CELL, 57)], fill=_shade(base, -48))
    for x0 in range(2, CELL, 5):
        d.point((x0, 59), fill=_shade(base, -60))                # stitches
    return img


def _rubber(seed, base=(38, 40, 44)):
    a = _surface(base, seed, fine=5, broad=10)
    y, x = np.mgrid[0:CELL, 0:CELL]
    a[..., :3] += (np.exp(-((y - 14) / 5.0) ** 2) * 16)[..., None]  # soft sheen
    return _img(np.clip(a, 0, 255))


def _tape(seed):
    a = _surface((196, 198, 196), seed, fine=6, broad=8, grain="h")
    img = _img(a)
    d = ImageDraw.Draw(img)
    for y in (0, 21, 42, 63):                                    # overlapping wraps
        d.line([(0, y), (CELL, y + 3)], fill=(150, 152, 150, 255), width=1)
    return img


def _visor(seed):
    y, x = np.mgrid[0:CELL, 0:CELL]
    a = np.zeros((CELL, CELL, 4), dtype=np.float32)
    t = y / CELL
    a[..., 0] = 70 + 60 * (1 - t)
    a[..., 1] = 110 + 70 * (1 - t)
    a[..., 2] = 128 + 80 * (1 - t)
    a[..., 3] = 255
    band = np.exp(-(((x - y * 0.8) - 14) / 4.0) ** 2) + 0.6 * np.exp(-(((x - y * 0.8) - 26) / 2.0) ** 2)
    a[..., :3] += band[..., None] * 110                          # reflections
    a[..., :3] += _smooth_noise(CELL, seed, 4)[..., None] * 12
    return _img(np.clip(a, 0, 255))


def _tank(seed):
    """Painted aluminium cylinder. Plain and even in both directions: the renderer wraps it round the tank face by
    face, so any band or stripe would repeat on every face. The blue pressure band is its own ring (tank_band)."""
    a = _surface((200, 204, 208), seed, fine=4, broad=6)
    return _img(np.clip(a, 0, 255))


def _tank_band(seed):
    a = _surface((44, 112, 190), seed, fine=4, broad=6)
    return _img(np.clip(a, 0, 255))


def _steel(seed):
    a = _surface((160, 164, 170), seed, fine=10, broad=12, grain="h")
    return _img(a)


def _filter(seed):
    a = _surface((70, 72, 78), seed, fine=6, broad=8)
    img = _img(a)
    d = ImageDraw.Draw(img)
    for yy in range(4, CELL, 7):
        for xx in range(4 + (yy // 7 % 2) * 3, CELL, 7):
            d.ellipse([xx - 2, yy - 2, xx + 2, yy + 2], fill=(22, 22, 24, 255))
    d.ellipse([2, 2, CELL - 3, CELL - 3], outline=(190, 194, 200, 255), width=3)
    return img


def _trefoil_patch(seed):
    import nuclear_materials as M
    def draw(big, d, f):
        base = M.fabric(SUIT, seed).resize((CELL * f, CELL * f), Image.LANCZOS)
        big.paste(base)
        trefoil(d, CELL * f // 2, CELL * f // 2, int(CELL * f * 0.44))
    return _supersample(draw, size=CELL, factor=4)


def _badge(seed):
    img = Image.new("RGBA", (CELL, CELL), (232, 232, 228, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, CELL - 1, CELL - 1], outline=(150, 150, 146, 255), width=3)
    d.rectangle([8, 10, 40, 30], fill=(40, 60, 50, 255))
    d.rectangle([11, 13, 37, 27], fill=(90, 220, 120, 255))     # dose window
    d.rectangle([8, 38, 56, 42], fill=(60, 60, 64, 255))
    d.rectangle([8, 47, 44, 50], fill=(110, 110, 114, 255))
    d.ellipse([46, 10, 56, 20], fill=(200, 50, 40, 255))
    return img


def _sole(seed):
    a = _surface((30, 30, 32), seed, fine=6, broad=6)
    img = _img(a)
    d = ImageDraw.Draw(img)
    for yy in range(3, CELL, 8):                                 # tread lugs
        d.rectangle([2, yy, CELL - 3, yy + 3], fill=(16, 16, 18, 255))
    return img


def _zip(seed):
    img = _img(_surface((60, 62, 66), seed, fine=4, broad=4))
    d = ImageDraw.Draw(img)
    for yy in range(0, CELL, 4):
        d.rectangle([10, yy, 30, yy + 1], fill=(200, 204, 210, 255))
        d.rectangle([34, yy + 2, 54, yy + 3], fill=(200, 204, 210, 255))
    return img


def draw_sheet():
    import nuclear_materials as M
    makers = {
        "fabric": lambda: M.fabric(SUIT, 1), "fabric_dark": lambda: M.fabric(_shade(SUIT, -22), 2),
        "rubber": lambda: M.rubber(3), "tape": lambda: M.tape(4), "visor": lambda: M.visor(5), "tank": lambda: M.tank(6),
        "steel": lambda: M.brushed((160, 164, 170), 7, "h"), "filter": lambda: M.filter_face(8),
        "trefoil": lambda: _trefoil_patch(9), "badge": lambda: M.badge(10), "sole": lambda: M.sole(11), "zip": lambda: M.zip_tex(12),
        "tank_band": lambda: M.tank_band(13),
    }
    sheet = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
    for name in HAZMAT_MATERIALS:
        col, row = slot(name)
        sheet.paste(M.stylize(makers[name](), 12), (col * CELL, row * CELL))
    return sheet


# ---------------------------------------------------------------- item and block art (64 px)

def _outline(img, colour=(20, 20, 22, 255)):
    """A one-pixel dark edge round an icon, like the vanilla item style."""
    alpha = img.split()[3]
    grown = alpha.filter(ImageFilter.MaxFilter(3))
    edge = Image.new("RGBA", img.size, colour)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(edge, (0, 0), grown)
    out.alpha_composite(img)
    return out


def _shaded_poly(d, pts, colour, f):
    d.polygon([(x * f, y * f) for x, y in pts], fill=colour)


def _icon(draw):
    def wrapped(big, d, f):
        draw(big, d, f)
    return _outline(_supersample(wrapped))


def _hood(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rounded_rectangle(S(12, 6, 52, 54), radius=16 * f, fill=SUIT)
    d.rounded_rectangle(S(12, 6, 30, 54), radius=16 * f, fill=_shade(SUIT, 18))
    d.rectangle(S(10, 50, 54, 58), fill=(38, 40, 44, 255))
    d.rounded_rectangle(S(17, 16, 47, 34), radius=5 * f, fill=(36, 38, 42, 255))
    d.rounded_rectangle(S(19, 18, 45, 32), radius=4 * f, fill=(110, 160, 182, 255))
    d.polygon(S(22, 19, 30, 19, 24, 31, 20, 31), fill=(200, 230, 240, 255))
    d.rounded_rectangle(S(26, 34, 38, 46), radius=3 * f, fill=(40, 42, 46, 255))
    for cx in (21, 43):
        d.ellipse(S(cx - 6, 37, cx + 6, 49), fill=(150, 154, 160, 255))
        d.ellipse(S(cx - 4, 39, cx + 4, 47), fill=(70, 72, 78, 255))


def _coverall(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.polygon(S(16, 6, 26, 4, 32, 10, 38, 4, 48, 6, 60, 22, 52, 28, 48, 22, 48, 60, 16, 60, 16, 22, 12, 28, 4, 22), fill=SUIT)
    d.polygon(S(16, 6, 26, 4, 32, 10, 32, 60, 16, 60, 16, 22, 12, 28, 4, 22), fill=_shade(SUIT, 14))
    d.line(S(32, 10, 32, 58), fill=(70, 72, 76, 255), width=2 * f)
    d.rectangle(S(16, 38, 48, 43), fill=(38, 40, 44, 255))
    d.rectangle(S(29, 38, 35, 43), fill=(170, 174, 180, 255))
    for x0 in (22, 42):
        d.rectangle(S(x0 - 2, 6, x0 + 2, 38), fill=(38, 40, 44, 255))
    d.rectangle(S(36, 16, 44, 24), fill=(232, 232, 228, 255))
    d.rectangle(S(38, 18, 42, 21), fill=(90, 220, 120, 255))
    trefoil(d, 24 * f, 29 * f, int(5 * f))
    for x0, x1 in ((4, 12), (52, 60)):
        d.polygon(S(x0, 22, x0 + 4, 20, x1, 26, x1 - 4, 28), fill=(196, 198, 196, 255))


def _trousers(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.polygon(S(14, 6, 50, 6, 54, 58, 36, 58, 32, 24, 28, 58, 10, 58), fill=SUIT)
    d.polygon(S(14, 6, 32, 6, 32, 24, 28, 58, 10, 58), fill=_shade(SUIT, 14))
    d.rectangle(S(14, 6, 50, 11), fill=(38, 40, 44, 255))
    for x0, x1 in ((10, 28), (36, 54)):
        d.rectangle(S(x0, 50, x1, 55), fill=(196, 198, 196, 255))


def _boots(big, d, f):
    S = lambda *v: [c * f for c in v]
    for dx in (0, 22):
        d.polygon(S(8 + dx, 14, 24 + dx, 14, 24 + dx, 42, 36 + dx, 46, 36 + dx, 54, 8 + dx, 54), fill=(44, 46, 50, 255))
        d.polygon(S(8 + dx, 14, 14 + dx, 14, 14 + dx, 54, 8 + dx, 54), fill=(64, 66, 72, 255))
        d.rectangle(S(6 + dx, 54, 38 + dx, 58), fill=(22, 22, 24, 255))
        d.rectangle(S(7 + dx, 12, 25 + dx, 17), fill=(196, 198, 196, 255))
        d.polygon(S(24 + dx, 44, 36 + dx, 47, 36 + dx, 54, 24 + dx, 54), fill=(230, 196, 40, 255))


def _check_source(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.ellipse(S(8, 14, 56, 50), fill=(150, 154, 160, 255))
    d.ellipse(S(8, 10, 56, 46), fill=(196, 200, 206, 255))
    d.ellipse(S(14, 15, 50, 41), fill=(244, 200, 36, 255))
    trefoil(d, 32 * f, 28 * f, int(14 * f), ring=False, bg=(244, 200, 36, 255))
    d.ellipse(S(29, 25, 35, 31), fill=(255, 120, 60, 255))


def _concrete():
    """Dense shielding concrete: grey with dark aggregate (barite) and a few form-tie holes."""
    a = _surface((132, 134, 136), 40, fine=14, broad=14)
    img = _img(a)
    d = ImageDraw.Draw(img)
    r = random.Random(41)
    for _ in range(70):
        x, y, s = r.randint(0, 63), r.randint(0, 63), r.randint(1, 2)
        c = r.choice([(92, 90, 88, 255), (110, 106, 100, 255), (160, 160, 158, 255)])
        d.ellipse([x - s, y - s, x + s, y + s], fill=c)
    for x, y in ((12, 12), (52, 12), (12, 52), (52, 52)):
        d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(70, 72, 74, 255))
        d.ellipse([x - 2, y - 2, x + 1, y + 1], fill=(50, 52, 54, 255))
    d.rectangle([0, 0, 63, 63], outline=(112, 114, 116, 255))
    return img


def _geiger_textures():
    case = _surface((232, 190, 40), 50, fine=6, broad=10)
    case_img = _img(case)
    d = ImageDraw.Draw(case_img)
    for y in range(0, 64, 16):
        d.line([(0, y), (63, y)], fill=(196, 156, 26, 255))
    screen = Image.new("RGBA", (64, 64), (26, 32, 30, 255))
    d = ImageDraw.Draw(screen)
    d.rectangle([0, 0, 63, 63], outline=(60, 62, 66, 255), width=4)
    d.rectangle([7, 8, 56, 40], fill=(40, 70, 52, 255))
    # A needle meter over a green LCD.
    for k in range(9):
        a = math.radians(200 + k * 17.5)
        x0, y0 = 32 + math.cos(a) * 20, 38 + math.sin(a) * 20
        x1, y1 = 32 + math.cos(a) * 24, 38 + math.sin(a) * 24
        d.line([(x0, y0), (x1, y1)], fill=(150, 240, 170, 255), width=2 if k % 4 == 0 else 1)
    d.line([(32, 38), (32 + math.cos(math.radians(250)) * 22, 38 + math.sin(math.radians(250)) * 22)], fill=(255, 120, 60, 255), width=2)
    d.rectangle([7, 46, 56, 56], fill=(40, 70, 52, 255))
    for x in range(10, 54, 7):
        d.rectangle([x, 48, x + 4, 54], fill=(120, 230, 150, 255))
    rubber = _rubber(52, (34, 36, 40))
    steel = _steel(53)
    grille = Image.new("RGBA", (64, 64), (40, 40, 44, 255))
    d = ImageDraw.Draw(grille)
    for y in range(4, 64, 6):
        for x in range(4 + (y // 6 % 2) * 3, 64, 6):
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(14, 14, 16, 255))
    def label(big, d, f):
        big.paste(case_img.resize((64 * f, 64 * f), Image.NEAREST))
        d.rectangle([4 * f, 4 * f, 60 * f, 60 * f], fill=(244, 244, 238, 255), outline=(20, 20, 22, 255), width=2 * f)
        trefoil(d, 32 * f, 32 * f, 22 * f)
    return {"geiger_case": case_img, "geiger_screen": screen, "geiger_rubber": rubber, "geiger_steel": steel,
            "geiger_grille": grille, "geiger_label": _supersample(label)}


def draw_item_textures():
    out = {"hazmat_hood": _icon(_hood), "hazmat_coverall": _icon(_coverall), "hazmat_trousers": _icon(_trousers),
           "hazmat_boots": _icon(_boots), "check_source": _icon(_check_source)}
    out.update(_geiger_textures())
    for name, draw in EQUIPMENT_ICONS.items():
        out[name] = _icon(draw)
    out["enriched_uranium_cylinder"] = _icon(_icon_cylinder)
    out["leu_fuel_assembly"] = _icon(_icon_assembly(False))
    out["spent_fuel_assembly"] = _icon(_icon_assembly(True))
    out["reactor_link_card"] = _icon(_icon_card)
    out.update(plant_interior.draw_item_textures())
    return out


def _brushed(base, seed, grain="v", fine=10):
    return _img(_surface(base, seed, fine=fine, broad=8, grain=grain))


def _hazard_tex(seed):
    img = Image.new("RGBA", (64, 64), (236, 190, 30, 255))
    d = ImageDraw.Draw(img)
    for k in range(-64, 128, 16):
        d.polygon([(k, 64), (k + 8, 64), (k + 72, 0), (k + 64, 0)], fill=(24, 24, 26, 255))
    a = np.asarray(img).astype(np.float32)
    a[..., :3] += _smooth_noise(64, seed, 8)[..., None] * 18
    return _img(np.clip(a, 0, 255))


def _floor(seed):
    a = _surface((150, 150, 146), seed, fine=12, broad=14)
    img = _img(a)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 63, 63], outline=(120, 120, 116, 255))
    d.rectangle([0, 28, 63, 33], fill=(226, 184, 30, 255))
    return img


def _cabinet(seed):
    a = _surface((198, 202, 198), seed, fine=6, broad=8)
    img = _img(a)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 63, 63], outline=(140, 144, 140, 255), width=2)
    d.line([(32, 2), (32, 61)], fill=(150, 154, 150, 255), width=1)
    for y in (12, 50):
        d.rectangle([28, y, 30, y + 6], fill=(70, 72, 76, 255))
        d.rectangle([34, y, 36, y + 6], fill=(70, 72, 76, 255))
    for y in range(20, 44, 4):
        d.line([(8, y), (24, y)], fill=(120, 124, 120, 255))
    return img


def _glass_tex(seed):
    y, x = np.mgrid[0:64, 0:64]
    a = np.zeros((64, 64, 4), dtype=np.float32)
    a[..., 0], a[..., 1], a[..., 2], a[..., 3] = 120, 160, 176, 255
    a[..., :3] += (np.exp(-(((x - y * 0.7) - 18) / 5.0) ** 2) * 90)[..., None]
    img = _img(np.clip(a, 0, 255))
    ImageDraw.Draw(img).rectangle([0, 0, 63, 63], outline=(60, 64, 70, 255), width=3)
    return img


def _pipe_tex(base, seed):
    a = _surface(base, seed, fine=6, broad=8, grain="h")
    img = _img(a)
    d = ImageDraw.Draw(img)
    for x in (10, 40):
        d.polygon([(x, 26), (x + 12, 32), (x, 38)], fill=(240, 240, 236, 255))
    return img


def _shielding_concrete():
    """Dense shielding concrete as it comes out of the forms: a smooth, even warm grey with soft mottling and a
    little fine aggregate, the faint joints of the form panels at the block's edges (so a wall reads as cast panels)
    and the two plugged form-tie holes on the panel's middle line. Calm enough to cover a whole containment."""
    import nuclear_materials as M
    size = M.CELL
    a = M.base((168, 166, 160), size)
    M.shade(a, M.fbm(size, 701, 4, 64) * 12)
    M.shade(a, M.grain(size, 702, 5))
    img = M.to_img(a)
    d = ImageDraw.Draw(img)
    r = random.Random(703)
    for _ in range(40):
        x, y = r.randrange(size), r.randrange(size)
        d.point((x, y), fill=r.choice([(150, 148, 142, 255), (184, 182, 176, 255)]))
    # Form panel joints round the edge: a dark line with a light lip.
    d.rectangle([0, 0, size - 1, size - 1], outline=(146, 144, 138, 255), width=2)
    d.line([(2, 2), (size - 3, 2)], fill=(182, 180, 174, 255))
    d.line([(2, 2), (2, size - 3)], fill=(182, 180, 174, 255))
    for x in (size // 4, size * 3 // 4):
        y = size // 2
        d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(150, 148, 144, 255))
        d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(132, 130, 126, 255))
        d.arc([x - 4, y - 4, x + 4, y + 4], 200, 340, fill=(190, 188, 182, 255))
    return M.stylize(img, 10)


def draw_block_textures():
    """The fuel machines' and pipes' block textures, in the same stylized-realistic look as the equipment sheet."""
    import nuclear_materials as M
    floor = M.concrete(63, tie_holes=False)
    d = ImageDraw.Draw(floor)
    d.rectangle([0, 56, 127, 66], fill=(226, 184, 30, 255))
    d.rectangle([0, 66, 127, 68], fill=(150, 120, 20, 255))
    out = {"shielding_concrete": _shielding_concrete(),
           "nu_centrifuge": M.stylize(M.brushed((206, 210, 214), 60, "v", dirt=0.15), 10),
           "nu_cap": M.stylize(M.brushed((52, 56, 62), 61, "h", dirt=0.1), 8),
           "nu_steel": M.stylize(M.brushed((150, 156, 162), 62, "h"), 10),
           "nu_floor": M.stylize(floor, 12), "nu_cabinet": M.stylize(M.painted((198, 202, 198), 64, seams=(("v", 62),), bolts=[(12, 20), (116, 20), (12, 108), (116, 108)], chip_count=2, drip_count=2, dirt=0.2), 10),
           "nu_glass": M.stylize(M.glass(65), 10), "nu_hazard": M.stylize(M.hazard(66), 8),
           "nu_pipe_blue": M.stylize(M.pipe_stripe((40, 100, 190), 67), 10), "nu_pipe_orange": M.stylize(M.pipe_stripe((220, 120, 30), 68), 10)}
    out.update(draw_pipe_textures())
    out.update(plant_interior.draw_block_textures())
    out.update(draw_tower_controller())
    grate = draw_cooling_blocks()
    out.update({k: v for k, v in grate.items() if k != "cooling_tower_controller_grate"})
    out["cooling_tower_controller_grate"] = grate["cooling_tower_controller_grate"]
    return out


def draw_tower_controller():
    """The controller: a concrete valve pit with a steel hatch and a blue water valve wheel on top."""
    import nuclear_materials as M
    side = M.concrete(71, tie_holes=False)
    d = ImageDraw.Draw(side)
    d.rectangle([0, 100, 127, 108], fill=(226, 184, 30, 255))
    d.rectangle([40, 30, 88, 70], fill=(60, 66, 72, 255))
    d.rectangle([44, 34, 84, 66], fill=(18, 26, 34, 255))
    for i, c in enumerate([(60, 200, 90), (230, 180, 40), (220, 60, 50)]):
        d.ellipse([50 + i * 10, 44, 56 + i * 10, 50], fill=c + (255,))
    top = M.brushed((120, 126, 132), 72, "h", dirt=0.2)
    d = ImageDraw.Draw(top)
    d.rectangle([8, 8, 119, 119], outline=(70, 74, 80, 255), width=4)
    d.ellipse([34, 34, 94, 94], outline=(40, 90, 170, 255), width=7)
    d.line([64, 34, 64, 94], fill=(40, 90, 170, 255), width=5)
    d.line([34, 64, 94, 64], fill=(40, 90, 170, 255), width=5)
    d.ellipse([56, 56, 72, 72], fill=(40, 90, 170, 255))
    return {"cooling_tower_controller_side": M.stylize(side, 12), "cooling_tower_controller_top": M.stylize(top, 10)}


def draw_cooling_blocks():
    """The cooling tower fill (corrugated PVC packs, seen from above and the side), the conduit trench cover (precast
    concrete slabs with lifting eyes, flush with the ground) and the tower controller's drain grate."""
    import nuclear_materials as M
    S = 128
    fill = Image.new("RGBA", (S, S), (214, 220, 222, 255))
    d = ImageDraw.Draw(fill)
    r = np.random.default_rng(91)
    for x in range(0, S, 8):
        for y in range(S):
            wave = int(2.5 * math.sin(y / 6.0))
            d.line([(x + wave, y), (x + wave + 3, y)], fill=(176, 184, 188, 255))
    for y in (0, 63):
        d.rectangle([0, y, S - 1, y + 1], fill=(150, 158, 162, 255))
    a = np.array(fill).astype(float)
    a[..., :3] *= r.uniform(0.94, 1.04, (S, S, 1))
    fill = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    side = Image.new("RGBA", (S, S), (204, 210, 212, 255))
    d = ImageDraw.Draw(side)
    for y in range(0, S, 6):
        d.rectangle([0, y, S - 1, y + 2], fill=(170, 178, 182, 255))
    for x in range(0, S, 32):
        d.rectangle([x, 0, x + 1, S - 1], fill=(140, 148, 152, 255))
    trench = M.concrete(93, tie_holes=False)
    d = ImageDraw.Draw(trench)
    d.rectangle([0, 62, 127, 65], fill=(96, 96, 92, 255))
    for cx, cy in ((32, 30), (96, 30), (32, 96), (96, 96)):
        d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], outline=(84, 86, 88, 255), width=3)
    grate = Image.new("RGBA", (S, S), (70, 74, 78, 255))
    d = ImageDraw.Draw(grate)
    for x in range(6, S, 12):
        d.rectangle([x, 6, x + 6, S - 7], fill=(28, 30, 34, 255))
    d.rectangle([0, 0, S - 1, S - 1], outline=(110, 114, 118, 255), width=6)
    return {"cooling_tower_fill": M.stylize(fill, 10), "cooling_tower_fill_side": M.stylize(side, 8),
            "conduit_trench": M.stylize(trench, 12), "cooling_tower_controller_grate": M.stylize(grate, 8)}


def draw_plume_frames():
    """Four soft white puffs for the cooling tower plume particle (32 px)."""
    from PIL import ImageFilter
    frames = {}
    for k in range(4):
        r = np.random.default_rng(80 + k)
        img = Image.new("L", (32, 32), 0)
        dd = ImageDraw.Draw(img)
        for _ in range(9):
            cx, cy = r.uniform(9, 23, 2)
            rad = r.uniform(5, 9) - k * 0.4
            dd.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=int(r.uniform(170, 235)))
        img = img.filter(ImageFilter.GaussianBlur(2.2))
        a = np.array(img).astype(float)
        yy, xx = np.mgrid[0:32, 0:32]
        a *= np.clip(1.25 - np.hypot(xx - 15.5, yy - 15.5) / 15, 0, 1)
        rgba = np.zeros((32, 32, 4), np.uint8)
        rgba[..., :3] = 246
        rgba[..., 3] = np.clip(a, 0, 255).astype(np.uint8)
        frames[f"cooling_plume_{k}"] = Image.fromarray(rgba, "RGBA")
    return frames


def draw_scram_button():
    """The console's big red SCRAM button in its yellow guard ring (48 px)."""
    def draw(big, d, f):
        c = 24 * f
        for k in range(8):
            a0 = k * 45
            d.pieslice([c - 23 * f, c - 23 * f, c + 23 * f, c + 23 * f], a0, a0 + 28, fill=(236, 190, 30, 255))
        d.ellipse([c - 18 * f, c - 18 * f, c + 18 * f, c + 18 * f], fill=(120, 20, 16, 255))
        d.ellipse([c - 16 * f, c - 16 * f, c + 16 * f, c + 16 * f], fill=(230, 60, 46, 255))
        d.ellipse([c - 11 * f, c - 13 * f, c - 1 * f, c - 5 * f], fill=(255, 150, 140, 255))
        from PIL import ImageFont
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 9 * f)
        d.text((c, c), "SCRAM", font=font, fill=(255, 255, 255, 255), anchor="mm")
    return _supersample(draw, size=48, factor=8)



def draw_hud_trefoil():
    def draw(big, d, f):
        trefoil(d, 11 * f, 11 * f, int(10.5 * f))
    return _supersample(draw, size=22, factor=8)


# ---------------------------------------------------------------- the Geiger counter's 3D item model

def _el(frm, to, tex, faces=None, uv=None):
    """An element; every face uses tex unless faces overrides some ({'south': 'screen'}). uv gives explicit UVs."""
    all_faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        t = (faces or {}).get(face, tex)
        if t is None:
            continue
        entry = {"texture": f"#{t}"}
        if uv and face in uv:
            entry["uv"] = uv[face]
        all_faces[face] = entry
    return {"from": frm, "to": to, "faces": all_faces}


def geiger_model(rid):
    full = [0, 0, 16, 16]
    elements = [
        _el([4, 1.5, 5], [12, 9.5, 10], "case", {"south": "case"}),                      # body
        _el([3.5, 0.5, 4.5], [12.5, 1.5, 10.5], "rubber"),                               # bottom bumper
        _el([3.5, 9.5, 4.5], [12.5, 10.5, 10.5], "rubber"),                              # top bumper
        _el([5, 5, 10], [11, 9, 10.5], "rubber", {"south": "screen"}, {"south": full}),  # screen, standing out
        _el([5, 2, 10], [11, 4.5, 10.3], "grille", uv={"south": full}),                  # speaker grille
        _el([12, 3, 6], [12.2, 8, 9], None, {"east": "label"}, {"east": full}),          # trefoil label
        _el([9.6, 10.5, 6.6], [11, 11.6, 8], "steel"),                                   # range knob
        _el([5, 10.5, 7], [6, 13, 8], "steel"),                                          # handle posts
        _el([10, 10.5, 7], [11, 13, 8], "steel"),
        _el([4.5, 13, 6.7], [11.5, 14.2, 8.3], "rubber"),                                # grip
        _el([1, 2.5, 6.2], [3.4, 10, 8.6], "steel"),                                     # side probe
        _el([1.2, 10, 6.4], [3.2, 10.4, 8.4], "rubber", {"up": "grille"}),               # mica window
        _el([3.4, 6, 6.8], [4, 7.5, 8], "steel"),                                        # probe clip
        _el([1.6, 1.2, 7], [2.6, 2.5, 8], "rubber"),                                     # coiled cable
        _el([2.6, 0.9, 7], [4.2, 1.8, 8], "rubber"),
    ]
    for e in elements:
        for face in list(e["faces"]):
            if e["faces"][face]["texture"] == "#None":
                del e["faces"][face]
    return {
        "textures": {"particle": rid("item/geiger_case"), "case": rid("item/geiger_case"), "screen": rid("item/geiger_screen"),
                     "rubber": rid("item/geiger_rubber"), "steel": rid("item/geiger_steel"),
                     "grille": rid("item/geiger_grille"), "label": rid("item/geiger_label")},
        "elements": elements,
        "display": {
            "gui": {"rotation": [20, -35, 0], "translation": [0, 0.5, 0], "scale": [1.05, 1.05, 1.05]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
            "thirdperson_righthand": {"rotation": [10, 180, 0], "translation": [0, 1.5, 2], "scale": [0.6, 0.6, 0.6]},
            "thirdperson_lefthand": {"rotation": [10, 180, 0], "translation": [0, 1.5, 2], "scale": [0.6, 0.6, 0.6]},
            "firstperson_righthand": {"rotation": [10, -20, 0], "translation": [1.5, 1.5, -1], "scale": [0.45, 0.45, 0.45]},
            "firstperson_lefthand": {"rotation": [10, 20, 0], "translation": [1.5, 1.5, -1], "scale": [0.45, 0.45, 0.45]},
        },
    }


# ---------------------------------------------------------------- the equipment's material sheet (512x512, 64 px cells)
# client/NuclearRenderer.java reads these regions by name, in this order (checked by tools/check_mod_data.py).
# Textures are drawn the right way up: the renderer puts the top of a cell at the top of every vertical face.
EQ_SHEET = 512
EQ_CELL = 64
EQ_MATERIALS = ["stainless", "stainless_dark", "carbon_steel", "paint_grey", "paint_green", "paint_blue", "paint_white",
                "concrete", "grating", "hazard", "insulation", "weld_band", "flange", "crdm", "copper", "tank_grey",
                "radiator", "glaze_brown", "glass", "screen", "desk", "rubber", "lamp_red", "lamp_green",
                "trefoil_sign", "generator", "fins", "handrail", "nameplate", "turbine_blue", "pipe_red", "water",
                "paint_yellow", "mimic", "display", "cherenkov", "fuel_rack", "louvre", "switchgear", "filter_bank",
                "sign_panel", "sign_edge"]


def eq_slot(name):
    index = EQ_MATERIALS.index(name)
    return index % 8, index // 8


def _plate(base, seed, seams=(), bolts=False, grain=None, fine=8, broad=10):
    img = _img(_surface(base, seed, fine=fine, broad=broad, grain=grain))
    d = ImageDraw.Draw(img)
    for kind, pos in seams:
        if kind == "h":
            d.line([(0, pos), (63, pos)], fill=_shade(base, -36), width=1)
            d.line([(0, pos + 1), (63, pos + 1)], fill=_shade(base, 18), width=1)
        else:
            d.line([(pos, 0), (pos, 63)], fill=_shade(base, -36), width=1)
            d.line([(pos + 1, 0), (pos + 1, 63)], fill=_shade(base, 18), width=1)
    if bolts:
        for x in range(4, 64, 8):
            for y in (3, 60):
                d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=_shade(base, -50))
    return img


def _weld_band(seed):
    img = _plate((168, 172, 176), seed, grain="h")
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for x in range(0, 64, 2):
        y = 30 + r.randint(-1, 1)
        d.ellipse([x - 2, y - 3, x + 2, y + 3], fill=(120 + r.randint(-10, 10), 112, 104, 255))
    d.line([(0, 26), (63, 26)], fill=(90, 84, 80, 255))
    d.line([(0, 35), (63, 35)], fill=(90, 84, 80, 255))
    return img


def _flange(seed):
    img = _plate((140, 146, 152), seed, grain="h", fine=6)
    d = ImageDraw.Draw(img)
    for x in range(4, 64, 8):
        d.ellipse([x - 3, 26, x + 3, 38], fill=(70, 74, 80, 255))
        d.ellipse([x - 2, 28, x + 2, 34], fill=(180, 186, 192, 255))
    d.line([(0, 2), (63, 2)], fill=(90, 94, 100, 255))
    d.line([(0, 61), (63, 61)], fill=(90, 94, 100, 255))
    return img


def _crdm(seed):
    img = _plate((86, 92, 100), seed, grain="v", fine=6)
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 12):
        d.rectangle([0, y, 63, y + 2], fill=(150, 156, 162, 255))
    for y in range(6, 64, 12):
        d.rectangle([28, y, 35, y + 3], fill=(220, 180, 40, 255))
    return img


def _grating(seed):
    img = Image.new("RGBA", (64, 64), (60, 64, 68, 255))
    d = ImageDraw.Draw(img)
    for x in range(0, 64, 8):
        d.line([(x, 0), (x, 63)], fill=(150, 154, 158, 255), width=2)
    for y in range(0, 64, 16):
        d.line([(0, y), (63, y)], fill=(170, 174, 178, 255), width=2)
    return img


def _insulation(seed):
    img = _plate((196, 200, 202), seed, grain="h", fine=6)
    d = ImageDraw.Draw(img)
    for y in (0, 31, 32, 63):
        d.line([(0, y), (63, y)], fill=(140, 144, 146, 255))
    for x in range(6, 64, 16):
        d.ellipse([x - 1, 2, x + 1, 4], fill=(110, 112, 114, 255))
        d.ellipse([x - 1, 34, x + 1, 36], fill=(110, 112, 114, 255))
    return img


def _radiator_eq(seed):
    img = _img(_surface((120, 128, 130), seed, fine=4, broad=4))
    d = ImageDraw.Draw(img)
    for x in range(0, 64, 4):
        d.rectangle([x, 0, x + 1, 63], fill=(78, 84, 86, 255))
        d.line([(x + 2, 0), (x + 2, 63)], fill=(156, 164, 166, 255))
    return img


def _glaze(base, seed):
    a = _surface(base, seed, fine=4, broad=6)
    y, x = np.mgrid[0:64, 0:64]
    a[..., :3] += (np.exp(-((x - 20) / 6.0) ** 2) * 60)[..., None]
    return _img(np.clip(a, 0, 255))


def _screen_tex(seed):
    img = Image.new("RGBA", (64, 64), (14, 22, 30, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 63, 63], outline=(40, 50, 58, 255), width=2)
    for y in range(8, 56, 8):
        d.line([(6, y), (6 + random.Random(seed + y).randint(14, 50), y)], fill=(80, 200, 230, 255), width=2)
    d.rectangle([44, 40, 58, 56], outline=(240, 180, 40, 255))
    return img


def _desk(seed):
    img = _plate((150, 156, 160), seed, seams=(("h", 10),), fine=5)
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    for k in range(10):
        x, y = 6 + (k % 5) * 11, 20 + (k // 5) * 16
        d.rectangle([x, y, x + 6, y + 6], fill=(40, 42, 46, 255))
        d.rectangle([x + 2, y + 2, x + 4, y + 4], fill=r.choice([(220, 60, 50, 255), (70, 200, 90, 255), (240, 190, 40, 255)]))
    return img


def _lamp(color, seed):
    img = Image.new("RGBA", (64, 64), tuple(int(c * 0.35) for c in color) + (255,))
    d = ImageDraw.Draw(img)
    d.ellipse([8, 8, 56, 56], fill=color + (254,))
    d.ellipse([18, 14, 30, 26], fill=(255, 255, 255, 254))
    return img


def _trefoil_sign(seed):
    def draw(big, d, f):
        d.rectangle([0, 0, 64 * f, 64 * f], fill=(244, 200, 36, 255))
        d.rectangle([2 * f, 2 * f, 62 * f, 62 * f], outline=(20, 20, 22, 255), width=2 * f)
        trefoil(d, 32 * f, 30 * f, 22 * f, ring=False, bg=(244, 200, 36, 255))
    return _supersample(draw)


def _generator(seed):
    return _plate((60, 120, 90), seed, seams=(("v", 31),), fine=6)


def _fins(seed):
    img = _img(_surface((96, 102, 108), seed, fine=4, broad=4))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 4):
        d.rectangle([0, y, 63, y + 1], fill=(60, 64, 70, 255))
    return img


def _handrail(seed):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 2, 63, 7], fill=(236, 190, 30, 255))
    d.rectangle([0, 30, 63, 33], fill=(236, 190, 30, 255))
    for x in (2, 30, 58):
        d.rectangle([x, 0, x + 4, 63], fill=(236, 190, 30, 255))
    d.rectangle([0, 58, 63, 63], fill=(236, 190, 30, 255))
    return img


def _nameplate(seed):
    img = Image.new("RGBA", (64, 64), (230, 232, 228, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 63, 63], outline=(60, 62, 66, 255), width=3)
    for y in range(12, 56, 9):
        d.line([(8, y), (8 + random.Random(seed + y).randint(20, 46), y)], fill=(40, 42, 46, 255), width=3)
    return img


def _water_tex(seed):
    a = _surface((40, 90, 150), seed, fine=6, broad=20)
    return _img(np.clip(a, 0, 255))


def draw_equipment_sheet():
    import nuclear_materials as M
    makers = {
        "stainless": lambda: M.brushed((182, 186, 190), 100, "v", seams=(("h", 40),)),
        "stainless_dark": lambda: M.brushed((120, 126, 132), 101, "v"),
        "carbon_steel": lambda: M.painted((92, 96, 100), 102, seams=(("h", 62),), bolts=[(x, 10) for x in range(12, 128, 24)], chip_count=2, dirt=0.7),
        "paint_grey": lambda: M.painted((168, 172, 170), 103, seams=(("v", 62),), chip_count=3, drip_count=3),
        "paint_green": lambda: M.painted((92, 130, 104), 104, chip_count=4, drip_count=3),
        "paint_blue": lambda: M.painted((52, 92, 150), 105, chip_count=4, drip_count=3),
        "paint_white": lambda: M.painted((222, 224, 220), 106, seams=(("h", 62),), chip_count=2, drip_count=4, dirt=0.6),
        "concrete": lambda: M.concrete(107), "grating": lambda: M.grating(108), "hazard": lambda: M.hazard(109),
        "insulation": lambda: M.insulation(110), "weld_band": lambda: M.weld_band(111), "flange": lambda: M.flange(112),
        "crdm": lambda: M.crdm(113), "copper": lambda: M.copper(114), "tank_grey": lambda: M.tank_grey(115),
        "radiator": lambda: M.radiator(116), "glaze_brown": lambda: M.glaze((120, 66, 40), 117), "glass": lambda: M.glass(118),
        "screen": lambda: M.screen(119), "desk": lambda: M.desk(120), "rubber": lambda: M.rubber(121),
        "lamp_red": lambda: M.lamp((230, 60, 50), 122), "lamp_green": lambda: M.lamp((80, 220, 110), 123),
        "trefoil_sign": lambda: M.trefoil_sign(124), "generator": lambda: M.generator(125), "fins": lambda: M.fins(126),
        "handrail": lambda: M.handrail(127), "nameplate": lambda: M.nameplate(128), "turbine_blue": lambda: M.turbine_blue(129),
        "pipe_red": lambda: M.pipe_stripe((180, 60, 50), 130), "water": lambda: M.water(131),
        "paint_yellow": lambda: M.painted((222, 178, 40), 132, seams=(("h", 62),), chip_count=5, drip_count=3),
        "mimic": lambda: M.mimic(133), "display": lambda: M.display(134), "cherenkov": lambda: M.cherenkov(135),
        "fuel_rack": lambda: M.fuel_rack(136), "louvre": lambda: M.louvre(137), "switchgear": lambda: M.switchgear(138),
        "filter_bank": lambda: M.filter_bank(139),
        "sign_panel": lambda: M.brushed((44, 48, 54), 140, "h", dirt=0.05), "sign_edge": lambda: M.brushed((190, 194, 198), 141, "h", dirt=0.05),
    }
    sheet = Image.new("RGBA", (EQ_SHEET, EQ_SHEET), (0, 0, 0, 0))
    for name in EQ_MATERIALS:
        col, row = eq_slot(name)
        sheet.paste(M.stylize(makers[name](), 24 if name in ("mimic", "display", "fuel_rack", "switchgear") else 12 if name in ("screen", "desk", "lamp_red", "lamp_green", "cherenkov") else 10), (col * EQ_CELL, row * EQ_CELL))
    return sheet


# ---------------------------------------------------------------- pipes (block models) and their textures

def _pipe_side(name):
    import nuclear_materials as M
    if name == "reactor_coolant_pipe":
        img = M.brushed((176, 180, 184), 140, "h", dirt=0.2)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 56, 127, 72], fill=(200, 60, 50, 255))
        d.rectangle([0, 72, 127, 74], fill=(120, 36, 30, 255))
        for x in (20, 84):
            d.polygon([(x, 44), (x + 22, 64), (x, 84)], fill=(240, 240, 236, 255))
        return M.stylize(img, 10)
    if name == "insulated_steam_line":
        img = M.insulation(141)
        d = ImageDraw.Draw(img)
        d.rectangle([40, 20, 52, 30], fill=(240, 150, 40, 255))
        return M.stylize(img, 10)
    if name == "isolated_phase_bus":
        # The generator's bus duct: a ribbed aluminium enclosure, a flange band and a phase label.
        img = M.brushed((182, 188, 192), 143, "h", dirt=0.15)
        d = ImageDraw.Draw(img)
        for x in range(0, 128, 32):
            d.rectangle([x, 0, x + 3, 127], fill=(130, 136, 140, 255))
        d.rectangle([60, 52, 76, 76], fill=(220, 180, 40, 255))
        return M.stylize(img, 10)
    img = M.concrete(142, tie_holes=False, stains=True)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 7], fill=(110, 112, 110, 255))
    d.rectangle([0, 120, 127, 127], fill=(110, 112, 110, 255))
    d.rectangle([0, 56, 127, 72], fill=(40, 110, 170, 255))
    return M.stylize(img, 10)


def _pipe_end(name):
    import nuclear_materials as M
    base = {"reactor_coolant_pipe": (150, 156, 160), "insulated_steam_line": (196, 200, 202), "isolated_phase_bus": (160, 166, 170),
            "circulating_water_conduit": (140, 140, 134)}[name]
    img = M.brushed(base, 150, "h", dirt=0.1) if name != "circulating_water_conduit" else M.concrete(150, tie_holes=False, stains=False)
    d = ImageDraw.Draw(img)
    inner = (40, 90, 150, 255) if name == "circulating_water_conduit" else (60, 62, 66, 255)
    d.ellipse([24, 24, 104, 104], fill=inner)
    d.ellipse([24, 24, 104, 104], outline=tuple(max(0, c - 40) for c in base) + (255,), width=6)
    return M.stylize(img, 10)


def draw_pipe_textures():
    out = {}
    for name in PIPES:
        out[name] = _pipe_side(name)
        out[name + "_end"] = _pipe_end(name)
    return out


def pipe_models(rid, name):
    """A core and an arm per side (multipart), like the mod's other pipes."""
    size = PIPES[name][1]
    lo, hi = 8 - size / 2, 8 + size / 2
    side, end = rid(f"block/{name}"), rid(f"block/{name}_end")
    core = {"parent": "minecraft:block/block", "textures": {"particle": side, "side": side, "end": end},
            "elements": [{"from": [lo, lo, lo], "to": [hi, hi, hi],
                          "faces": {f: {"texture": "#side"} for f in ("north", "south", "east", "west", "up", "down")}}]}
    # The arm on the north side; the blockstate turns it for the others.
    arm = {"parent": "minecraft:block/block", "textures": {"particle": side, "side": side, "end": end},
           "elements": [{"from": [lo, lo, 0], "to": [hi, hi, lo],
                         "faces": {"east": {"texture": "#side"}, "west": {"texture": "#side"},
                                   "up": {"texture": "#side", "rotation": 90}, "down": {"texture": "#side", "rotation": 90},
                                   "north": {"texture": "#end"}}}]}
    item = {"parent": "minecraft:block/block", "textures": {"particle": side, "side": side, "end": end},
            "elements": [{"from": [0, lo, lo], "to": [16, hi, hi],
                          "faces": {"north": {"texture": "#side"}, "south": {"texture": "#side"},
                                    "up": {"texture": "#side"}, "down": {"texture": "#side"},
                                    "east": {"texture": "#end"}, "west": {"texture": "#end"}}}],
            "display": {"gui": {"rotation": [30, 45, 0], "scale": [0.625, 0.625, 0.625]}}}
    return core, arm, item


def pipe_blockstate(rid, name):
    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    parts = [{"apply": {"model": rid(f"block/{name}_core")}}]
    for face, r in rot.items():
        parts.append({"when": {face: "true"}, "apply": dict({"model": rid(f"block/{name}_arm"), "uvlock": False}, **r)})
    return {"multipart": parts}


# ---------------------------------------------------------------- equipment and fuel item icons (64 px)

def _cyl_icon(d, f, cx, y0, y1, r, body, dark, light, top=None):
    """A shaded upright cylinder, seen from slightly above."""
    S = lambda v: v * f
    d.rectangle([S(cx - r), S(y0), S(cx + r), S(y1)], fill=body)
    d.rectangle([S(cx - r), S(y0), S(cx - r * 0.45), S(y1)], fill=light)
    d.rectangle([S(cx + r * 0.55), S(y0), S(cx + r), S(y1)], fill=dark)
    d.ellipse([S(cx - r), S(y1 - r * 0.35), S(cx + r), S(y1 + r * 0.35)], fill=dark)
    d.ellipse([S(cx - r), S(y0 - r * 0.35), S(cx + r), S(y0 + r * 0.35)], fill=top or light)


STEEL = (176, 182, 188, 255)
STEEL_D = (110, 116, 124, 255)
STEEL_L = (220, 224, 228, 255)


def _icon_vessel(big, d, f):
    S = lambda *v: [c * f for c in v]
    _cyl_icon(d, f, 32, 22, 56, 15, STEEL, STEEL_D, STEEL_L)
    d.rectangle(S(17, 36, 47, 39), fill=(130, 136, 142, 255))
    for x in range(20, 46, 5):
        d.rectangle(S(x, 6, x + 3, 22), fill=(90, 96, 104, 255))
        d.rectangle(S(x, 6, x + 3, 8), fill=(230, 190, 40, 255))
    d.ellipse(S(17, 18, 47, 26), fill=STEEL_L)
    trefoil(d, 32 * f, 47 * f, int(5 * f))


def _icon_sg(big, d, f):
    _cyl_icon(d, f, 32, 26, 58, 10, STEEL, STEEL_D, STEEL_L)
    d.polygon([(22 * f, 26 * f), (42 * f, 26 * f), (46 * f, 18 * f), (18 * f, 18 * f)], fill=STEEL)
    _cyl_icon(d, f, 32, 4, 18, 14, STEEL, STEEL_D, STEEL_L)
    d.rectangle([18 * f, 30 * f, 46 * f, 33 * f], fill=(200, 60, 50, 255))


def _icon_pump(big, d, f):
    S = lambda *v: [c * f for c in v]
    _cyl_icon(d, f, 32, 8, 34, 9, (52, 92, 150, 255), (36, 64, 110, 255), (90, 130, 190, 255))
    d.rectangle(S(20, 34, 44, 40), fill=STEEL_D)
    _cyl_icon(d, f, 32, 40, 56, 13, STEEL, STEEL_D, STEEL_L)
    d.rectangle(S(6, 46, 20, 52), fill=STEEL)
    d.rectangle(S(44, 46, 58, 52), fill=STEEL)


def _icon_pressurizer(big, d, f):
    _cyl_icon(d, f, 32, 8, 54, 9, STEEL, STEEL_D, STEEL_L)
    d.ellipse([23 * f, 3 * f, 41 * f, 13 * f], fill=STEEL_L)
    d.rectangle([22 * f, 54 * f, 42 * f, 60 * f], fill=(90, 96, 100, 255))


def _icon_turbine(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(2, 44, 62, 56), fill=(150, 150, 146, 255))
    d.rectangle(S(4, 32, 14, 44), fill=(70, 110, 140, 255))
    for x in (16, 30):
        d.chord(S(x - 2, 20, x + 14, 52), 180, 360, fill=(70, 110, 140, 255))
        d.rectangle(S(x - 2, 36, x + 14, 44), fill=(70, 110, 140, 255))
    _cyl_icon(d, f, 52, 30, 44, 8, (60, 120, 90, 255), (40, 86, 64, 255), (96, 160, 124, 255))


def _icon_condenser(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(6, 18, 58, 54), fill=(126, 134, 136, 255))
    d.rectangle(S(6, 18, 18, 54), fill=(150, 158, 160, 255))
    d.rectangle(S(6, 14, 58, 18), fill=(100, 106, 110, 255))
    for y in (26, 38):
        d.rectangle(S(58, y, 62, y + 6), fill=(40, 110, 170, 255))
    d.rectangle(S(2, 40, 6, 48), fill=(40, 110, 170, 255))


def _icon_feedwater(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(4, 44, 60, 52), fill=(130, 136, 140, 255))
    _cyl_icon(d, f, 20, 26, 44, 9, (52, 92, 150, 255), (36, 64, 110, 255), (90, 130, 190, 255))
    _cyl_icon(d, f, 44, 30, 44, 7, STEEL, STEEL_D, STEEL_L)
    d.rectangle(S(29, 34, 37, 38), fill=STEEL_D)


def _icon_transformer(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(12, 26, 52, 56), fill=(126, 134, 136, 255))
    for x in range(14, 52, 4):
        d.rectangle(S(x, 30, x + 2, 52), fill=(96, 102, 106, 255))
    for x in (20, 32, 44):
        d.rectangle(S(x - 2, 8, x + 2, 26), fill=(120, 66, 40, 255))
        for y in range(10, 26, 4):
            d.rectangle(S(x - 4, y, x + 4, y + 2), fill=(140, 80, 50, 255))
    d.rectangle(S(4, 32, 12, 50), fill=(96, 102, 108, 255))


def _icon_console(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.polygon(S(4, 50, 60, 50, 54, 34, 10, 34), fill=(150, 156, 160, 255))
    d.rectangle(S(4, 50, 60, 58), fill=(120, 126, 130, 255))
    d.rectangle(S(10, 10, 54, 34), fill=(40, 44, 50, 255))
    d.rectangle(S(13, 13, 51, 31), fill=(14, 22, 30, 255))
    for y in (17, 22, 27):
        d.line(S(16, y, 40, y), fill=(80, 200, 230, 255), width=f)
    d.rectangle(S(44, 40, 50, 46), fill=(220, 50, 40, 255))
    for x in (14, 22, 30):
        d.rectangle(S(x, 40, x + 4, 44), fill=(70, 200, 90, 255))


def _icon_epr(big, d, f):
    """The EPR vessel: the standard vessel's icon, wider, with a blue band (the plant colour)."""
    S = lambda *v: [c * f for c in v]
    _cyl_icon(d, f, 32, 22, 58, 18, STEEL, STEEL_D, STEEL_L)
    d.rectangle(S(14, 34, 50, 38), fill=(60, 90, 160, 255))
    for x in range(17, 48, 4):
        d.rectangle(S(x, 4, x + 2, 22), fill=(90, 96, 104, 255))
        d.rectangle(S(x, 4, x + 2, 6), fill=(230, 190, 40, 255))
    d.ellipse(S(14, 17, 50, 27), fill=STEEL_L)
    trefoil(d, 32 * f, 48 * f, int(5 * f))


def _icon_smr(big, d, f):
    """A slim tall module with its flange and nozzles."""
    S = lambda *v: [c * f for c in v]
    _cyl_icon(d, f, 32, 8, 58, 10, STEEL, STEEL_D, STEEL_L)
    d.rectangle(S(20, 30, 44, 34), fill=(120, 126, 132, 255))
    for y in (14, 20):
        d.rectangle(S(14, y, 22, y + 3), fill=STEEL_D)
    for x in range(27, 38, 3):
        d.rectangle(S(x, 2, x + 1, 8), fill=(90, 96, 104, 255))
    trefoil(d, 32 * f, 46 * f, int(4 * f))


def _icon_small_turbine(big, d, f):
    """A small skid: blue turbine casing and green generator."""
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(4, 46, 60, 54), fill=(120, 126, 130, 255))
    _cyl_icon(d, f, 20, 26, 46, 11, (52, 92, 150, 255), (36, 64, 110, 255), (90, 130, 190, 255))
    d.rectangle(S(34, 24, 58, 46), fill=(60, 120, 90, 255))
    d.rectangle(S(52, 24, 58, 46), fill=(40, 86, 64, 255))
    d.rectangle(S(17, 10, 23, 24), fill=(220, 220, 214, 255))


def _icon_cw_pump(big, d, f):
    """A vertical pump: blue motor on top of a stand over the pump column, a discharge pipe out of the side."""
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(10, 52, 54, 58), fill=(110, 116, 120, 255))
    _cyl_icon(d, f, 32, 34, 52, 7, STEEL, STEEL_D, STEEL_L)
    d.rectangle(S(38, 40, 60, 46), fill=(40, 110, 170, 255))
    _cyl_icon(d, f, 32, 8, 30, 12, (52, 92, 150, 255), (36, 64, 110, 255), (90, 130, 190, 255))
    for y in range(12, 30, 4):
        d.rectangle(S(20, y, 44, y + 1), fill=(36, 64, 110, 255))


def _icon_cylinder(big, d, f):
    _cyl_icon(d, f, 32, 14, 54, 14, (220, 222, 216, 255), (160, 162, 156, 255), (240, 242, 238, 255))
    d.rectangle([18 * f, 30 * f, 46 * f, 36 * f], fill=(240, 190, 40, 255))
    d.rectangle([28 * f, 6 * f, 36 * f, 14 * f], fill=STEEL_D)


def _icon_assembly(spent):
    def draw(big, d, f):
        S = lambda *v: [c * f for c in v]
        d.rectangle(S(22, 4, 42, 8), fill=STEEL_D)
        d.rectangle(S(22, 56, 42, 60), fill=STEEL_D)
        rod = (180, 186, 190, 255) if not spent else (120, 110, 96, 255)
        for x in range(23, 42, 3):
            d.rectangle(S(x, 8, x + 2, 56), fill=rod)
        for y in (18, 30, 42):
            d.rectangle(S(21, y, 43, y + 2), fill=(200, 204, 208, 255) if not spent else (150, 140, 120, 255))
        if spent:
            d.rectangle(S(20, 4, 44, 60), outline=(90, 170, 255, 255), width=f)
    return draw


def _icon_card(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rounded_rectangle(S(6, 16, 58, 48), radius=4 * f, fill=(40, 60, 90, 255))
    d.rectangle(S(12, 22, 24, 32), fill=(220, 190, 60, 255))
    d.line(S(30, 26, 52, 26), fill=(200, 220, 240, 255), width=2 * f)
    d.line(S(30, 34, 48, 34), fill=(200, 220, 240, 255), width=2 * f)
    trefoil(d, 18 * f, 40 * f, int(5 * f))


EQUIPMENT_ICONS = {"reactor_pressure_vessel": _icon_vessel, "reactor_steam_generator": _icon_sg,
                   "reactor_coolant_pump": _icon_pump, "pressurizer": _icon_pressurizer, "turbine_generator": _icon_turbine,
                   "condenser": _icon_condenser, "feedwater_pump": _icon_feedwater, "main_transformer": _icon_transformer,
                   "epr_reactor_vessel": lambda big, d, f: _icon_epr(big, d, f), "small_modular_reactor": lambda big, d, f: _icon_smr(big, d, f),
                   "small_turbine_generator": lambda big, d, f: _icon_small_turbine(big, d, f),
                   "circulating_water_pump": lambda big, d, f: _icon_cw_pump(big, d, f),
                   "reactor_control_console": _icon_console}


# ---------------------------------------------------------------- the plant buildings' equipment: icons

YEL, YEL_D, YEL_L = (222, 178, 40, 255), (160, 124, 24, 255), (244, 210, 90, 255)
GRN, GRN_D, GRN_L = (92, 130, 104, 255), (60, 90, 70, 255), (130, 170, 140, 255)
WHT, WHT_D, WHT_L = (222, 224, 220, 255), (170, 172, 168, 255), (244, 246, 242, 255)
BLU, BLU_D, BLU_L = (52, 92, 150, 255), (36, 64, 110, 255), (90, 130, 190, 255)
DARK = (40, 42, 46, 255)


def _hcyl_icon(d, f, x0, x1, cy, r, body, dark, light):
    """A shaded horizontal cylinder with rounded ends."""
    S = lambda *v: [c * f for c in v]
    d.rounded_rectangle(S(x0, cy - r, x1, cy + r), radius=r * f, fill=body)
    d.rectangle(S(x0 + r * 0.6, cy - r, x1 - r * 0.6, cy - r * 0.45), fill=light)
    d.rectangle(S(x0 + r * 0.6, cy + r * 0.5, x1 - r * 0.6, cy + r), fill=dark)


def _icon_crane(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(4, 12, 60, 20), fill=YEL)
    d.rectangle(S(4, 18, 60, 20), fill=YEL_D)
    for x in (4, 52):
        d.rectangle(S(x, 8, x + 8, 22), fill=YEL_D)
        d.rectangle(S(x, 8, x + 8, 10), fill=(30, 30, 30, 255))
    d.rectangle(S(26, 6, 38, 12), fill=(90, 96, 104, 255))
    d.line(S(30, 20, 30, 42), fill=DARK, width=f)
    d.line(S(34, 20, 34, 42), fill=DARK, width=f)
    d.rectangle(S(26, 42, 38, 48), fill=YEL)
    d.arc(S(28, 48, 38, 58), 0, 200, fill=STEEL_D, width=2 * f)


def _icon_overhead_crane(big, d, f):
    _icon_crane(big, d, f)
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(6, 24, 14, 30), fill=YEL)
    d.rectangle(S(7, 25, 13, 28), fill=(110, 150, 170, 255))


def _icon_gantry(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(6, 10, 58, 16), fill=YEL)
    for x in (6, 52):
        d.rectangle(S(x, 10, x + 6, 50), fill=YEL)
        d.rectangle(S(x - 2, 50, x + 8, 54), fill=YEL_D)
    d.rectangle(S(26, 4, 38, 12), fill=STEEL)
    d.rectangle(S(30, 12, 34, 52), fill=STEEL_L)
    d.rectangle(S(28, 52, 36, 56), fill=(230, 180, 40, 255))
    d.rectangle(S(20, 6, 26, 10), fill=(60, 180, 220, 255))


def _icon_msr(big, d, f):
    _hcyl_icon(d, f, 4, 60, 34, 14, WHT, WHT_D, WHT_L)
    S = lambda *v: [c * f for c in v]
    for x in (18, 46):
        d.rectangle(S(x - 4, 12, x + 4, 22), fill=WHT_D)
        d.rectangle(S(x - 6, 10, x + 6, 13), fill=STEEL_D)
        d.rectangle(S(x - 6, 48, x + 6, 58), fill=(92, 96, 100, 255))


def _icon_fw_heater(big, d, f):
    _hcyl_icon(d, f, 12, 60, 32, 11, (168, 172, 170, 255), (120, 124, 122, 255), (200, 204, 202, 255))
    S = lambda *v: [c * f for c in v]
    d.ellipse(S(4, 20, 18, 44), fill=(130, 134, 132, 255))
    d.rectangle(S(12, 20, 16, 44), fill=STEEL_D)
    for x in (24, 48):
        d.rectangle(S(x - 5, 42, x + 5, 56), fill=(92, 96, 100, 255))


def _icon_diesel(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(2, 50, 62, 56), fill=(92, 96, 100, 255))
    d.rectangle(S(4, 24, 36, 50), fill=GRN)
    for x in range(6, 34, 6):
        d.rectangle(S(x, 18, x + 4, 24), fill=GRN_D)
    d.rectangle(S(8, 14, 32, 18), fill=WHT_D)
    d.rectangle(S(10, 2, 16, 14), fill=WHT_D)
    _hcyl_icon(d, f, 36, 62, 36, 13, (60, 120, 90, 255), (40, 84, 62, 255), (90, 160, 120, 255))


def _icon_tank(big, d, f):
    _cyl_icon(d, f, 32, 12, 56, 18, WHT, WHT_D, WHT_L)
    S = lambda *v: [c * f for c in v]
    for y in (24, 36, 48):
        d.rectangle(S(14, y, 50, y + 1), fill=WHT_D)
    d.rectangle(S(20, 10, 22, 56), fill=DARK)
    d.rectangle(S(26, 10, 28, 56), fill=DARK)


def _icon_hx(big, d, f):
    _hcyl_icon(d, f, 8, 56, 32, 12, GRN, GRN_D, GRN_L)
    S = lambda *v: [c * f for c in v]
    for x in (8, 52):
        d.rectangle(S(x, 18, x + 4, 46), fill=STEEL)
    d.rectangle(S(18, 8, 24, 20), fill=(180, 60, 50, 255))
    d.rectangle(S(40, 8, 46, 20), fill=BLU)
    for x in (18, 42):
        d.rectangle(S(x, 44, x + 6, 56), fill=(92, 96, 100, 255))


def _icon_pump_skid(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(4, 48, 60, 54), fill=(92, 96, 100, 255))
    d.ellipse(S(4, 24, 24, 48), fill=BLU)
    d.ellipse(S(8, 28, 20, 44), fill=BLU_L)
    d.rectangle(S(24, 30, 30, 42), fill=(230, 180, 40, 255))
    _hcyl_icon(d, f, 30, 60, 36, 11, (168, 172, 170, 255), (120, 124, 122, 255), (200, 204, 202, 255))
    d.rectangle(S(11, 8, 17, 26), fill=STEEL)


def _icon_fhm(big, d, f):
    _icon_gantry(big, d, f)
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(4, 56, 60, 62), fill=(70, 140, 220, 255))


def _icon_rack(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(6, 6, 58, 58), fill=(70, 150, 230, 255))
    for i in range(4):
        for j in range(4):
            x, y = 8 + i * 12.5, 8 + j * 12.5
            d.rectangle(S(x, y, x + 10, y + 10), fill=(200, 215, 230, 255), outline=(240, 245, 250, 255))
            d.rectangle(S(x + 3, y + 3, x + 7, y + 7), fill=(90, 160, 230, 255))


def _icon_cask(big, d, f):
    _cyl_icon(d, f, 32, 12, 58, 16, (176, 176, 170, 255), (130, 130, 124, 255), (206, 206, 200, 255), top=(110, 116, 124, 255))
    S = lambda *v: [c * f for c in v]
    for x in (20, 40):
        d.rectangle(S(x, 50, x + 5, 54), fill=DARK)
        d.rectangle(S(x, 14, x + 5, 18), fill=DARK)
    trefoil(d, 32 * f, 34 * f, int(6 * f))


def _icon_screen(big, d, f):
    S = lambda *v: [c * f for c in v]
    for x in (4, 54):
        d.rectangle(S(x, 4, x + 6, 60), fill=BLU)
    d.rectangle(S(10, 10, 54, 52), fill=(150, 154, 158, 255))
    for x in range(12, 54, 4):
        d.line(S(x, 10, x, 52), fill=(80, 84, 88, 255), width=f)
    for y in range(12, 52, 6):
        d.line(S(10, y, 54, y), fill=(60, 64, 68, 255), width=f)
    d.rectangle(S(4, 2, 60, 10), fill=BLU_D)


def _icon_display_wall(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(2, 12, 62, 50), fill=(60, 64, 70, 255))
    for i in range(3):
        for j in range(2):
            x, y = 4 + i * 19.5, 14 + j * 18
            d.rectangle(S(x, y, x + 18, y + 16), fill=(14, 30, 52, 255))
            d.line(S(x + 2, y + 10, x + 7, y + 6, x + 12, y + 9, x + 16, y + 4), fill=(110, 230, 140, 255), width=f)
    d.rectangle(S(24, 50, 40, 58), fill=(90, 94, 100, 255))


def _icon_board(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(4, 8, 60, 40), fill=(176, 182, 178, 255))
    d.line(S(8, 22, 30, 22, 30, 32, 56, 32), fill=(200, 50, 40, 255), width=2 * f)
    d.line(S(8, 28, 22, 28, 22, 16, 56, 16), fill=(50, 110, 200, 255), width=2 * f)
    for x in range(8, 58, 6):
        d.rectangle(S(x, 10, x + 3, 13), fill=(80, 220, 110, 255) if x % 18 else (230, 60, 50, 255))
    d.polygon(S(2, 40, 62, 40, 62, 48, 2, 54), fill=(150, 156, 160, 255))
    d.rectangle(S(2, 54, 62, 60), fill=(120, 124, 128, 255))


def _icon_switchgear(big, d, f):
    S = lambda *v: [c * f for c in v]
    for k in range(3):
        x = 4 + k * 19
        d.rectangle(S(x, 6, x + 18, 58), fill=(150, 158, 160, 255), outline=(80, 86, 90, 255))
        d.rectangle(S(x + 3, 12, x + 11, 20), fill=(240, 240, 230, 255))
        d.ellipse(S(x + 12, 13, x + 16, 17), fill=(230, 60, 50, 255) if k == 1 else (80, 220, 110, 255))
        d.rectangle(S(x + 2, 30, x + 4, 46), fill=(60, 62, 66, 255))


def _icon_ahu(big, d, f):
    S = lambda *v: [c * f for c in v]
    d.rectangle(S(4, 22, 60, 54), fill=WHT)
    d.rectangle(S(4, 22, 18, 54), fill=(150, 154, 152, 255))
    for y in range(25, 52, 4):
        d.line(S(6, y, 16, y), fill=(60, 64, 66, 255), width=f)
    d.rectangle(S(22, 26, 38, 50), outline=WHT_D, width=f)
    d.rectangle(S(42, 26, 58, 50), outline=WHT_D, width=f)
    d.rectangle(S(44, 8, 58, 22), fill=(110, 116, 124, 255))


EQUIPMENT_ICONS.update({
    "polar_crane": _icon_crane, "refueling_machine": _icon_gantry, "overhead_crane": _icon_overhead_crane,
    "moisture_separator_reheater": _icon_msr, "feedwater_heater": _icon_fw_heater, "emergency_diesel_generator": _icon_diesel,
    "storage_tank": _icon_tank, "heat_exchanger": _icon_hx, "pump_skid": _icon_pump_skid, "fuel_handling_machine": _icon_fhm,
    "spent_fuel_rack": _icon_rack, "dry_storage_cask": _icon_cask, "travelling_water_screen": _icon_screen,
    "plant_display_wall": _icon_display_wall, "main_control_board": _icon_board, "switchgear_lineup": _icon_switchgear,
    "air_handling_unit": _icon_ahu,
})


# ---------------------------------------------------------------- the fuel machines' models (MachineKind multi-blocks)
# Structure coordinates in pixels, facing north (front = -z), master block at 0..16 (tools/large_machines.py explains
# the format). Both style packs use these: the nuclear look is the same in each.

def gas_centrifuge_cascade_model():
    """Five wide, three tall, two deep: a concrete floor with a yellow walkway line, two rows of tall aluminium gas
    centrifuges (nine of them) with dark motor caps, three header pipes along the top (blue feed, orange product,
    grey tails) dropping a branch into every machine, and the control cabinet (the master block) at the front left."""
    from steampunk_models import box, cyl, dial
    m = [box((-64, 0, 0), (16, 1.5, 32), {"*": "nu_floor", "north": "nu_hazard", "south": "nu_hazard"})]
    m.append(box((1, 1.5, 0.5), (15, 30, 9), {"*": "nu_cabinet", "north": "#front", "up": "nu_steel"}))
    m.append(dial("north", (4, 26, 0.25), 2.5, texture="nu_glass", body="nu_steel"))
    m.append(dial("north", (12, 26, 0.25), 2.5, texture="nu_glass", body="nu_steel"))
    rows = [(x, 9) for x in (-8, -24, -40, -56)] + [(x, 24) for x in (8, -8, -24, -40, -56)]
    for cx, cz in rows:
        m += cyl("y", cx, cz, 5, 3.5, 40, "nu_centrifuge", "nu_cap")
        m += cyl("y", cx, cz, 5.5, 1.5, 3.5, "nu_steel")
        m += cyl("y", cx, cz, 4, 40, 43, "nu_cap")
        m += cyl("y", cx, cz, 1, 43, 45, "nu_steel")
    # Headers above the rows, and a short drop into each machine.
    for z, tex in ((15.5, "nu_pipe_blue"), (17.5, "nu_pipe_orange")):
        m.append(box((-62, 45, z - 0.75), (14, 46.5, z + 0.75), tex))
    m.append(box((-62, 45, 30), (14, 46.5, 31.5), "nu_steel"))
    for cx, cz in rows:
        z0, z1 = (cz + 0.5, 15) if cz < 16 else (18.5, cz - 0.5)
        m.append(box((cx - 0.5, 44.5, min(z0, z1)), (cx + 0.5, 45.5, max(z0, z1)), "nu_steel"))
    # Header supports.
    for x in (-62, -30, 12):
        m.append(box((x, 1.5, 15), (x + 2, 45, 17), "nu_steel"))
    return m


def fuel_fabricator_model():
    """Three wide, two tall, two deep: a pellet press with its powder hopper on the right, a glass-walled rod-loading
    glovebox in the middle with a fuel assembly standing in its frame, and the control cabinet (master) at the front
    left, all on a concrete floor with hazard edging."""
    from steampunk_models import box, cyl, dial
    m = [box((-32, 0, 0), (16, 1.5, 32), {"*": "nu_floor", "north": "nu_hazard", "south": "nu_hazard"})]
    m.append(box((1, 1.5, 0.5), (15, 24, 10), {"*": "nu_cabinet", "north": "#front", "up": "nu_steel"}))
    m.append(dial("north", (8, 20.5, 0.25), 3, texture="nu_glass", body="nu_steel"))
    # Glovebox: steel base, glass walls, a frame holding an assembly, two glove ports.
    m.append(box((-14, 1.5, 4), (-2, 10, 28), "nu_cabinet"))
    m.append(box((-14, 10, 4), (-13.5, 28, 28), "nu_glass"))
    m.append(box((-2.5, 10, 4), (-2, 28, 28), "nu_glass"))
    m.append(box((-14, 10, 4), (-2, 28, 4.5), "nu_glass"))
    m.append(box((-14, 28, 4), (-2, 29.5, 28), "nu_steel"))
    m.append(box((-9.5, 10, 14), (-6.5, 27, 17), "nu_centrifuge"))
    m.append(box((-10, 10, 13.5), (-6, 11, 17.5), "nu_steel"))
    m.append(box((-10, 26, 13.5), (-6, 27, 17.5), "nu_steel"))
    for x in (-12, -5):
        m += cyl("z", x, 17, 1.5, 3.5, 4.5, "nu_cap")
    # Pellet press and hopper.
    m.append(box((-30, 1.5, 6), (-17, 18, 26), {"*": "nu_cabinet", "up": "nu_steel"}))
    m += cyl("y", -23.5, 16, 3, 18, 26, "nu_steel")
    m.append(box((-29, 26, 10), (-18, 31, 22), {"*": "nu_steel", "up": "nu_cap"}))
    m.append(box((-24.5, 2, 5.5), (-22.5, 14, 6), "nu_hazard"))
    m.append(box((-31, 18, 15), (-30, 22, 17), "nu_pipe_blue"))
    return m


# ---------------------------------------------------------------- data

def write_assets(write, rid, assets, lang):
    plant_interior.write_assets(write, rid, assets, lang)
    for item in ITEMS:
        lang[f"item.{MOD}.{item}"] = NAMES[item]
        if item == "geiger_counter":
            write(assets / "models" / "item" / f"{item}.json", geiger_model(rid))
        else:
            write(assets / "models" / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    lang[f"block.{MOD}.shielding_concrete"] = NAMES["shielding_concrete"]
    write(assets / "models" / "block" / "shielding_concrete.json", {"parent": "minecraft:block/cube_all",
                                                                    "textures": {"all": rid("block/shielding_concrete")}})
    write(assets / "blockstates" / "shielding_concrete.json", {"variants": {"": {"model": rid("block/shielding_concrete")}}})
    write(assets / "items" / "shielding_concrete.json", {"model": {"type": "minecraft:model", "model": rid("block/shielding_concrete")}})
    # Equipment: drawn by client/NuclearRenderer.java; the block model only gives break particles.
    for kind in EQUIPMENT:
        lang[f"block.{MOD}.{kind}"] = NAMES[kind]
        write(assets / "models" / "block" / f"{kind}.json", {"textures": {"particle": "minecraft:block/iron_block"}})
        write(assets / "blockstates" / f"{kind}.json",
              {"variants": {f"facing={f}": {"model": rid(f"block/{kind}")} for f in ("north", "east", "south", "west")}})
        write(assets / "models" / "item" / f"{kind}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{kind}")}})
        write(assets / "items" / f"{kind}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{kind}")}})
    lang[f"block.{MOD}.{TOWER_CONTROLLER}"] = NAMES[TOWER_CONTROLLER]
    lang[f"block.{MOD}.cooling_tower_fill"] = NAMES["cooling_tower_fill"]
    write(assets / "models" / "block" / "cooling_tower_fill.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": rid("block/cooling_tower_fill_side"), "top": rid("block/cooling_tower_fill"), "bottom": rid("block/cooling_tower_fill")}})
    write(assets / "blockstates" / "cooling_tower_fill.json", {"variants": {"": {"model": rid("block/cooling_tower_fill")}}})
    write(assets / "items" / "cooling_tower_fill.json", {"model": {"type": "minecraft:model", "model": rid("block/cooling_tower_fill")}})
    lang[f"block.{MOD}.conduit_trench"] = NAMES["conduit_trench"]
    write(assets / "models" / "block" / "conduit_trench.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": rid("block/circulating_water_conduit"), "top": rid("block/conduit_trench"), "bottom": rid("block/conduit_trench")}})
    write(assets / "blockstates" / "conduit_trench.json", {"variants": {"": {"model": rid("block/conduit_trench")}}})
    write(assets / "items" / "conduit_trench.json", {"model": {"type": "minecraft:model", "model": rid("block/conduit_trench")}})
    write(assets / "models" / "block" / f"{TOWER_CONTROLLER}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": rid(f"block/{TOWER_CONTROLLER}_side"), "top": rid(f"block/{TOWER_CONTROLLER}_grate"),
        "bottom": rid(f"block/{TOWER_CONTROLLER}_side")}})
    turn = {"south": 0, "west": 90, "north": 180, "east": 270}
    write(assets / "blockstates" / f"{TOWER_CONTROLLER}.json", {"variants": {
        f"facing={f},tower={t}": dict({"model": rid(f"block/{TOWER_CONTROLLER}")}, **({"y": turn[f]} if turn[f] else {}))
        for f in turn for t in TOWERS}})
    write(assets / "items" / f"{TOWER_CONTROLLER}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{TOWER_CONTROLLER}")}})
    write(assets / "particles" / "cooling_plume.json", {"textures": [rid(f"cooling_plume_{k}") for k in range(4)]})
    write(assets / "models" / "block" / f"{FRAME}.json", {"textures": {"particle": "minecraft:block/iron_block"}})
    write(assets / "blockstates" / f"{FRAME}.json", {"variants": {"": {"model": rid(f"block/{FRAME}")}}})
    for pipe in PIPES:
        lang[f"block.{MOD}.{pipe}"] = NAMES[pipe]
        core, arm, item = pipe_models(rid, pipe)
        write(assets / "models" / "block" / f"{pipe}_core.json", core)
        write(assets / "models" / "block" / f"{pipe}_arm.json", arm)
        write(assets / "models" / "item" / f"{pipe}.json", item)
        write(assets / "blockstates" / f"{pipe}.json", pipe_blockstate(rid, pipe))
        write(assets / "items" / f"{pipe}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{pipe}")}})
    # The suit is drawn in 3D by client/HazmatLayer.java, so its equipment asset has no flat layers.
    write(assets / "equipment" / "hazmat.json", {"layers": {"humanoid": [{"texture": "jugcraft:blank"}]}})
    lang.update(LANG)
    lang.update(CONSOLE_LANG)


def write_loot(write, rid, out, self_drop):
    for block in BLOCKS:
        write(out / f"{block}.json", plant_interior.door_loot(rid, block) if block in plant_interior.DOORS else self_drop(block))
    # Breaking a frame block breaks the equipment, and its anchor drops the item.
    write(out / f"{FRAME}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{FRAME}")})


def write_data(write, data):
    write(data / MOD / "damage_type" / "radiation.json", {"message_id": "jugcraft.radiation", "exhaustion": 0.0, "scaling": "never"})


def write_tags(tags, rid):
    for block in BLOCKS:
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
        tags.add("block", "minecraft:needs_stone_tool", rid(block))
    tags.add("block", f"{MOD}:{SHIELDING_TAG}", rid("shielding_concrete"))
    tags.add("block", f"{MOD}:{SHIELDING_TAG}", "#c:storage_blocks/lead")
    tags.add("item", "minecraft:head_armor", rid("hazmat_hood"))
    tags.add("item", "minecraft:chest_armor", rid("hazmat_coverall"))
    tags.add("item", "minecraft:leg_armor", rid("hazmat_trousers"))
    tags.add("item", "minecraft:foot_armor", rid("hazmat_boots"))


# ---------------------------------------------------------------- sound
# The Geiger counter has no recording. client/GeigerSound.java makes its sound live, from the dose rate the server
# syncs: counts arrive at random (a Poisson process, with the tube's dead time), each one the tube's discharge pulse
# kicking a small speaker (a resonant cone and a little case thump). The click model here is the same as the Java's;
# preview() renders a sample so it can be listened to outside the game. The sound event is a Fabric stream
# placeholder: the game asks the sound instance for its audio, and the instance generates it.

RATE = 44100


def _resonance(x, freq, q):
    """A two-pole resonant filter (the speaker cone)."""
    w = 2 * math.pi * freq / RATE
    r = math.exp(-w / (2 * q))
    a1, a2 = -2 * r * math.cos(w), r * r
    y = np.zeros_like(x)
    for n in range(len(x)):
        y[n] = x[n] - a1 * (y[n - 1] if n > 0 else 0) - a2 * (y[n - 2] if n > 1 else 0)
    return y


def geiger_click(seed):
    """One count, about 12 ms (GeigerSound.click)."""
    r = np.random.default_rng(seed)
    n = int(RATE * 0.012)
    pulse = np.zeros(n)
    width = 3 + r.integers(0, 2)
    pulse[:width] = 1.0
    pulse[width:width * 2] = -0.55
    pulse += r.normal(0, 0.02, n) * np.exp(-np.arange(n) / (RATE * 0.0008))
    cone = _resonance(pulse, 3300 + r.uniform(-150, 150), 4.5)
    body = _resonance(pulse, 620 + r.uniform(-40, 40), 2.2) * 0.6
    out = (cone + body) * np.exp(-np.arange(n) / (RATE * 0.0025))
    return out / np.max(np.abs(out))


def sound_events():
    return {"geiger_counter": {"subtitle": "subtitles.jugcraft.geiger_counter",
                               "sounds": [{"name": "fabric-sound-api-v1:empty", "stream": True}]}}


def write_sounds():
    """sounds.json entry (and removes the old recorded clips)."""
    import json
    out = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jugcraft/sounds/nuclear")
    if os.path.isdir(out):
        for f in os.listdir(out):
            os.remove(os.path.join(out, f))
        os.rmdir(out)
    path = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jugcraft/sounds.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data = {k: v for k, v in data.items() if not k.startswith("geiger_")}
    data.update(sound_events())
    with open(path, "w", encoding="utf-8") as f:
        f.write(json.dumps(data, indent=2) + "\n")


if __name__ == "__main__":
    write_sounds()
