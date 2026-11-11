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
from PIL import Image, ImageDraw, ImageFilter, ImageFont

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
    "reactor_control_console": ("Reactor Control Console", 3, 2, 3),
    # The pre-made plants' own reactors (tools/nuclear_plants.py).
    "epr_reactor_vessel": ("EPR Reactor Pressure Vessel", 8, 8, 22),
    "small_modular_reactor": ("Small Modular Reactor", 5, 5, 22),
    "small_turbine_generator": ("Small Turbine-Generator", 3, 5, 4),
    "circulating_water_pump": ("Circulating Water Pump", 3, 3, 6),
    # Plant equipment that only has to look right (the plants' buildings): drawn by the client, placed whole.
    "polar_crane": ("Polar Crane", 40, 5, 5),
    "refueling_machine": ("Refueling Machine", 15, 4, 8),
    "overhead_crane": ("Overhead Crane", 42, 6, 5),
    "moisture_separator_reheater": ("Moisture Separator Reheater", 5, 20, 5),
    "feedwater_heater": ("Feedwater Heater", 3, 12, 3),
    "emergency_diesel_generator": ("Emergency Diesel Generator", 10, 21, 7),
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
    # The control room's system boards (round 23, tools/control_boards.py), each its own layout.
    "esf_control_board": ("Safety Systems Control Board", 6, 2, 3),
    "rcs_control_board": ("Reactor Coolant Control Board", 6, 2, 3),
    "secondary_control_board": ("Steam and Feedwater Control Board", 6, 2, 3),
    "turbine_control_board": ("Turbine Generator Control Board", 6, 2, 3),
    "electrical_control_board": ("Electrical Control Board", 6, 2, 3),
    "radiation_control_board": ("Radiation and Ventilation Control Board", 6, 2, 3),
    # The control building's remote shutdown panel (round 24, tools/control_boards.py remote_shutdown_panel).
    "remote_shutdown_panel": ("Remote Shutdown Panel", 6, 1, 3),
    # The DC equipment rooms' 480 V motor control centres (round 26, 8a; tools/control_boards.py motor_control_center).
    "mcc_lineup": ("480 V Motor Control Center", 4, 1, 3),
    # The control room's ventilation plant (round 26, #11; tools/hvac_boards.py): they run as the room's HVAC control
    # panel has them (nuclear/HvacRoom.java), normal or emergency.
    "cr_filtration_unit": ("Control Room Emergency Filtration Unit", 3, 7, 3),
    "cr_air_handling_unit": ("Control Room Air Handling Unit", 3, 7, 3),
    "cr_chiller": ("Control Room Water Chiller", 3, 5, 3),
    "chw_pump": ("Chilled Water Pump", 1, 3, 2),
    # The diesel generator's starting air system (round 31; tools/diesel_set.py): each joins the nearest diesel
    # generator (nuclear/StartingAir.java), labelled by its anchor's variant.
    "edg_air_receiver": ("Starting Air Receiver", 2, 2, 5),
    "edg_air_compressor": ("Starting Air Compressor", 3, 2, 2),
    "edg_air_dryer": ("Starting Air Dryer", 2, 1, 2),
    # The diesel rooms' own plant (round 31 part 3; tools/diesel_room.py): each joins the nearest diesel generator, as
    # the starting air does, its gauges and lamps reading it; the CO2 bank discharges into its room
    # (nuclear/Co2System.java), the monorail's hoist travels and lifts from its pendant (nuclear/Monorail.java).
    "edg_jw_keepwarm": ("Jacket Water Keep-Warm Skid", 3, 2, 2),
    "edg_lo_keepwarm": ("Lube Oil Keep-Warm Skid", 3, 2, 2),
    "edg_expansion_tank": ("Jacket Water Expansion Tank", 2, 2, 5),
    "edg_jw_cooler": ("Jacket Water Cooler", 7, 2, 3),
    "edg_ngr": ("Neutral Grounding Resistor", 2, 2, 2),
    "edg_gen_cubicle": ("Generator Terminal and CT Cubicle", 3, 2, 3),
    "edg_co2_bank": ("CO2 Cylinder Bank", 3, 1, 2),
    "edg_monorail": ("Monorail Hoist", 20, 2, 1),
    # The turbine hall's feed train and the turbine's oil plant (turbine hall part 2, tools/feed_train.py; TurbineHall):
    # each joins the nearest turbine-generator, labelled by its anchor's variant; the condensate and main feed pumps
    # start and stop and the plant answers.
    "condenser_shell": ("Condenser Shell", 12, 6, 6),
    "condenser_hotwell": ("Condenser Hotwell", 12, 6, 6),
    "condensate_pump": ("Condensate Pump", 3, 2, 5),
    "main_feed_pump": ("Main Feed Pump", 4, 11, 5),
    "lube_oil_reservoir": ("Lube Oil Reservoir", 4, 8, 4),
    "ehc_unit": ("EHC Fluid Supply Unit", 3, 5, 3),
    "seal_oil_unit": ("Seal Oil Unit", 3, 5, 3),
    "h2_gas_panel": ("Generator Gas Control Panel", 4, 2, 3),
    # Turbine hall part 3 (tools/oil_room.py): the lube oil room's conditioner, storage tanks and transfer pumps.
    "lo_conditioner": ("Lube Oil Conditioner", 3, 2, 3),
    "lo_storage_tank": ("Lube Oil Storage Tank", 3, 3, 8),
    "lo_transfer_pumps": ("Lube Oil Transfer Pumps", 2, 2, 2),
}
# The cooling tower controller: one block, three tower kinds (CoolingTowerBlock.Tower).
TOWER_CONTROLLER = "cooling_tower_controller"
# The pump houses' controllers (towers and pump houses build: nuclear/WaterWorksBlock.java): the lake intake's and the
# circulating water pump house's.
WORKS_CONTROLLER = "water_works_controller"
WORKS = ["intake", "circ"]
# The transformer yard's and the switchyard's controllers (transformer yard and switchyard build: nuclear/YardBlock.java).
YARD_CONTROLLER = "yard_controller"
YARD_WORKS = ["yard", "switchyard"]
# The reactor building's controller (its inside a compiled site, tools/reactor_site.py: nuclear/ReactorBuildingBlock.java).
REACTOR_CONTROLLER = "reactor_building_controller"
TOWERS = ["natural", "large", "mechanical", "medium"]
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
# The compiled sites' blocks (towers and pump houses build, nuclear/SiteBlock.java): drawn and shaped by their site.
SITE_BLOCKS = {"site_structure": "Plant Structure", "site_ladder": "Plant Structure (Ladder)"}
import plant_interior
import storage_tank
import nuclear_storage
import structure_items

BLOCKS = ["shielding_concrete"] + list(EQUIPMENT) + list(PIPES) + [TOWER_CONTROLLER, WORKS_CONTROLLER, YARD_CONTROLLER, REACTOR_CONTROLLER] + list(COOLING_BLOCKS) + list(SITE_BLOCKS) + plant_interior.BLOCKS \
    + list(storage_tank.NAMES) + nuclear_storage.BLOCKS

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
    WORKS_CONTROLLER: "Water Works Controller",
    YARD_CONTROLLER: "Yard Controller",
    REACTOR_CONTROLLER: "Reactor Building Controller",
    **COOLING_BLOCKS,
    **SITE_BLOCKS,
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
    "tooltip.jugcraft.nuclear.turbine_generator": "Steam in, power out. Needs a condenser and a main transformer touching it. Manual trip on its front standard, turning gear by its last LP",
    "tooltip.jugcraft.nuclear.condenser": "Turns used steam back into water: 4,000 mB/t of cooling water at full power. Sneak and use with an empty hand to relabel it 1A, 1B or 1C",
    "tooltip.jugcraft.nuclear.feedwater_pump": "Sends the condensed water back to the steam generators (on the steam line)",
    "tooltip.jugcraft.nuclear.main_transformer": "The unit's power comes out here, into cables or a Unit Substation",
    "tooltip.jugcraft.nuclear.reactor_control_console": "Runs the reactor: its rods, nuclear instruments and trip switches work. Link it to the vessel with a Reactor Link Card",
    "tooltip.jugcraft.nuclear.epr_reactor_vessel": "The EPR's larger core: 241 assemblies and 1.6 times the output. Needs two turbine-generators to use it all",
    "tooltip.jugcraft.nuclear.small_modular_reactor": "Reactor, steam generator and pressurizer in one module: 37 assemblies, an eighth of a full unit. Steam line to a Small Turbine-Generator",
    "tooltip.jugcraft.nuclear.circulating_water_pump": "Moves the cooling water round the cooling towers: a tower only works with these on its conduit (each one carries half a unit's heat)",
    "tooltip.jugcraft.nuclear.polar_crane": "The containment's round-the-dome crane (for building your own plant: it only looks the part)",
    "tooltip.jugcraft.nuclear.refueling_machine": "The gantry that lifts fuel out of the open reactor (for show)",
    "tooltip.jugcraft.nuclear.overhead_crane": "The turbine hall's 160 / 25 ton bridge crane: place it on a runway; from a crane pendant station hold a button to run the bridge, trolley or either hook (the pendant's NORTH, SOUTH, WEST and EAST)",
    "tooltip.jugcraft.nuclear.moisture_separator_reheater": "Dries and reheats the steam between the turbine's HP and LP cylinders; its gauges read the nearest turbine",
    "tooltip.jugcraft.nuclear.feedwater_heater": "Warms the feedwater with steam bled from the turbine (for show)",
    "tooltip.jugcraft.nuclear.emergency_diesel_generator": "Backup power for the safety systems: right-click to start or stop it. It burns diesel from its day tank and starts on air from its receivers",
    "tooltip.jugcraft.nuclear.storage_tank": "A plant water tank: refuelling water, condensate, demineralised water (for show)",
    "tooltip.jugcraft.nuclear.heat_exchanger": "A component cooling heat exchanger (for show)",
    "tooltip.jugcraft.nuclear.pump_skid": "A plant pump and motor on its baseplate (for show)",
    "tooltip.jugcraft.nuclear.fuel_handling_machine": "The bridge that moves fuel round the spent fuel pool (for show)",
    "tooltip.jugcraft.nuclear.spent_fuel_rack": "Holds used fuel under water; the water glows blue over it (for show)",
    "tooltip.jugcraft.nuclear.dry_storage_cask": "A ventilated concrete cask for cooled spent fuel (for show)",
    "tooltip.jugcraft.nuclear.travelling_water_screen": "Strains fish and debris out of the cooling water intake (for show)",
    "tooltip.jugcraft.nuclear.plant_display_wall": "The control room's plant overview display: a wall of projection cubes showing the plant live, its safety functions, electrical lineup and trends, and the boards' alarms",
    "tooltip.jugcraft.nuclear.main_control_board": "A control room benchboard with its mimic, meters and annunciators (for show)",
    "tooltip.jugcraft.nuclear.switchgear_lineup": "A 480 V load center that holds 2,000,000 JE: cable into the back of its transformer section, take power out of its feeders' sections (upper breakers out of the top, lower ones down through the floor); right-click a breaker to open or close it",
    "tooltip.jugcraft.nuclear.air_handling_unit": "Filters and moves a building's air (for show)",
    "tooltip.jugcraft.nuclear.esf_control_board": "The safety injection, residual heat removal and containment spray trains (for show)",
    "tooltip.jugcraft.nuclear.rcs_control_board": "The coolant loops, coolant pumps, pressurizer, charging and letdown (for show)",
    "tooltip.jugcraft.nuclear.secondary_control_board": "The steam generators' levels, feedwater and main steam (for show)",
    "tooltip.jugcraft.nuclear.turbine_control_board": "The turbine's controls and the generator's meters and breakers (for show)",
    "tooltip.jugcraft.nuclear.electrical_control_board": "Offsite power, the station busses and the diesel generators (for show)",
    "tooltip.jugcraft.nuclear.radiation_control_board": "The radiation monitors, ventilation and fire detection (for show)",
    "tooltip.jugcraft.nuclear.remote_shutdown_panel": "Trips the nearest reactor and takes its controls from the control room (turn a transfer key; then its aux feed and ARV switches work)",
    "tooltip.jugcraft.nuclear.mcc_lineup": "A 480 V motor control center that holds 500,000 JE: cable in under its first section, take power out of its units' terminals (the chargers' down through the floor or out of the back); right-click a unit to switch its breaker",
    "tooltip.jugcraft.nuclear.cr_filtration_unit": "Charcoal and HEPA filters the control room's outside air when the HVAC panel goes to emergency (a reactor trip or high temperature); its booster fan starts on its own",
    "tooltip.jugcraft.nuclear.cr_air_handling_unit": "Cools and circulates the control room's air (its filters, chilled water coil and supply fan); runs as the HVAC panel has it",
    "tooltip.jugcraft.nuclear.cr_chiller": "Chills the water for the control room's air handlers; its panel shows its temperatures and load",
    "tooltip.jugcraft.nuclear.chw_pump": "Pumps chilled water from the air handlers' coils through its chiller",
    "tooltip.jugcraft.nuclear.edg_air_receiver": "Holds the nearest diesel generator's starting air: each start takes some; below 150 psig it fails to start",
    "tooltip.jugcraft.nuclear.edg_air_compressor": "Refills the nearest diesel generator's air receivers: the first cuts in below 225 psig, the second below 200, both stop at 250",
    "tooltip.jugcraft.nuclear.edg_air_dryer": "Dries the starting air between the compressors and the receivers (its POWER lamp lit while it is joined to a diesel generator)",
    # The diesel rooms' own plant (round 31 part 3, nuclear/DieselRoom.java).
    "tooltip.jugcraft.nuclear.edg_jw_keepwarm": "Keeps the nearest diesel generator's jacket water warm while it stands by: its heater and pump run, its gauges read the water",
    "tooltip.jugcraft.nuclear.edg_lo_keepwarm": "Keeps the nearest diesel generator's lube oil warm and its gallery primed while it stands by (30 psig prelube)",
    "tooltip.jugcraft.nuclear.edg_expansion_tank": "The nearest diesel generator's jacket water expansion tank: its sight glass rises as the water warms",
    "tooltip.jugcraft.nuclear.edg_jw_cooler": "Cools the nearest diesel generator's jacket water with service water while it runs: its temperature valve opens past 160 °F",
    "tooltip.jugcraft.nuclear.edg_ngr": "The diesel generator's neutral grounding resistor (for show)",
    "tooltip.jugcraft.nuclear.edg_gen_cubicle": "The diesel generator's terminal and CT cubicle: look in through its door windows (for show)",
    "tooltip.jugcraft.nuclear.edg_co2_bank": "Floods the nearest diesel generator's room with CO2 when its release station is pulled: get out before the 30 seconds are up",
    "tooltip.jugcraft.nuclear.edg_monorail": "A 5-ton monorail hoist beam: its trolley runs along it and its hook goes up and down from the hoist pendant station",
    # Turbine hall part 2 (tools/feed_train.py; nuclear/TurbineHall.java): the feed train and the oil plant.
    "tooltip.jugcraft.nuclear.condenser_shell": "Another condenser shell under the turbine's LPs, standing on its hotwell (for show). Sneak and use with an empty hand to relabel it 1A, 1B or 1C",
    "tooltip.jugcraft.nuclear.condenser_hotwell": "The hotwell under a condenser shell: its vacuum and hotwell gauges and level glass read the nearest turbine's unit",
    "tooltip.jugcraft.nuclear.condensate_pump": "A 50% condensate pump: START and STOP at its station. A stopped one stands by in AUTO and starts when the pumps running can't carry the feed",
    "tooltip.jugcraft.nuclear.main_feed_pump": "A turbine-driven 60% main feed pump: TRIP and RESET at its station, start and stop it from the secondary board. Losing one above 60% runs the plant back with the rods in AUTO",
    "tooltip.jugcraft.nuclear.lube_oil_reservoir": "The main turbine's lube oil reservoir: its pumps' lamps, gauges and level glass show the nearest turbine",
    "tooltip.jugcraft.nuclear.ehc_unit": "The turbine's EHC fluid supply unit: its pumps' lamps, gauges and level glass show the nearest turbine",
    "tooltip.jugcraft.nuclear.seal_oil_unit": "The generator's seal oil unit: its pumps' lamps, gauges and level glass show the nearest turbine",
    "tooltip.jugcraft.nuclear.h2_gas_panel": "The generator's gas control station: hydrogen purity, casing pressure and dew point, its hydrogen and CO2 cylinders",
    # Turbine hall part 3 (tools/oil_room.py; nuclear/TurbineHall.java): the lube oil room's plant.
    "tooltip.jugcraft.nuclear.lo_conditioner": "The lube oil conditioner: its heater, pump and coalescer lamps, filter dP gauge and level glass show the nearest turbine's oil",
    "tooltip.jugcraft.nuclear.lo_storage_tank": "A lube oil storage tank (clean or dirty: sneak and use with an empty hand to relabel it): its level gauge shows the nearest turbine's oil",
    "tooltip.jugcraft.nuclear.lo_transfer_pumps": "The lube oil transfer pumps between the reservoir and the storage tanks: their lamps and discharge gauge show the nearest turbine's oil",
    "tooltip.jugcraft.nuclear.small_turbine_generator": "One module's turbine, condenser and generator in one: cooling water in, power out into cables",
    "block.jugcraft.cooling_tower_controller": "Cooling Tower Controller",
    "message.jugcraft.nuclear.tower_status": "Cooling tower: %s%% busy (takes %s of a standard unit's heat)",
    "message.jugcraft.nuclear.tower_incomplete": "Cooling tower incomplete: build the whole tower around this controller",
    # The medium plant's towers (towers and pump houses build).
    "message.jugcraft.nuclear.mu1_open": "MU-1 open: make-up water to the basin",
    "message.jugcraft.nuclear.mu1_shut": "MU-1 shut: no make-up water to the basin",
    "message.jugcraft.nuclear.lp_ct1a": "LP-CT1A: basin level %s%%, MU-1 %s; alarms acknowledged",
    # The pump houses (towers and pump houses build: nuclear/WaterWorksBlockEntity.java).
    "message.jugcraft.nuclear.ww_pump_start": "%s started",
    "message.jugcraft.nuclear.ww_pump_stop": "%s stopped",
    "message.jugcraft.nuclear.ww_pump_locked": "%s: the 86 lockout is tripped. Press RESET on the panel first",
    "message.jugcraft.nuclear.ww_pump_suction": "%s: no suction, the forebay is too low (the towers' basins need make-up)",
    "message.jugcraft.nuclear.ww_mov_open": "%s opening",
    "message.jugcraft.nuclear.ww_mov_shut": "%s closing",
    "message.jugcraft.nuclear.ww_tws_start": "%s started",
    "message.jugcraft.nuclear.ww_tws_stop": "%s stopped",
    "message.jugcraft.nuclear.ww_ann_ack": "Alarms acknowledged",
    "message.jugcraft.nuclear.ww_ann_silence": "Horn silenced",
    "message.jugcraft.nuclear.ww_ann_reset": "Trips reset",
    "message.jugcraft.nuclear.ww_reset_refused": "Can't reset: the forebay is still low",
    "message.jugcraft.nuclear.ww_intake_status": "Lake intake: make-up %s gpm, service water train 1 %s gpm, train 2 %s gpm",
    "message.jugcraft.nuclear.ww_circ_status": "Circulating water: %s pumps delivering, %s gpm; forebay %s m",
    # The transformer yard and the switchyard (transformer yard and switchyard build: nuclear/YardBlockEntity.java).
    "message.jugcraft.nuclear.yard_status": "Transformer yard: %s%% of rated output out to the switchyard. MT-1A %s/%s C, MT-1B %s/%s C, MT-1C %s/%s C (top oil/winding); UAT-1A %s C, UAT-1B %s C; deluge zones flowing: %s%s",
    "message.jugcraft.nuclear.yard_lockout": ". A main transformer's lockout is in (too hot): the breaker stays open until it cools",
    "message.jugcraft.nuclear.reactor_building_status": "Reactor building: %s. Coolant pumps running: %s; pressurizer %s psig, steam generator 1 %s psig; operating floor %s mR/hr",
    "message.jugcraft.nuclear.reactor_building_power": "at power (head on)",
    "message.jugcraft.nuclear.reactor_building_refuelling": "refuelling (head on its stand, internals in the bay)",
    "message.jugcraft.nuclear.reactor_building_no_vessel": "No reactor vessel within 48 blocks of this reactor building",
    "message.jugcraft.nuclear.switchyard_status": "Switchyard: BKR 1-1 %s, 89-1 %s, earth switch %s; line 1 terminal holds %s JE; SAT-1 on tap %s, top oil %s C",
    "message.jugcraft.nuclear.yard_deluge": "%s manual release: the deluge is spraying %s (RESET on the releasing panel to stop it)",
    "message.jugcraft.nuclear.yard_deluge_reset": "Releasing panel reset: every zone armed again",
    "message.jugcraft.nuclear.yard_deluge_armed": "Every zone is armed: nothing to reset",
    "message.jugcraft.nuclear.yard_deluge_silence": "Gongs silenced (the water still flows until RESET)",
    "message.jugcraft.nuclear.yard_ds_opening": "89-1 opening",
    "message.jugcraft.nuclear.yard_ds_closing": "89-1 closing",
    "message.jugcraft.nuclear.yard_ds_already_open": "89-1 is already open",
    "message.jugcraft.nuclear.yard_ds_already_closed": "89-1 is already closed",
    "message.jugcraft.nuclear.yard_ds_earthed": "89-1 can't close: its earth switch is on (take it off first)",
    "message.jugcraft.nuclear.yard_es_interlock": "Earth switch interlocked: open 89-1 fully first",
    "message.jugcraft.nuclear.yard_es_on": "89-1E earthed",
    "message.jugcraft.nuclear.yard_es_off": "89-1E open",
    "message.jugcraft.nuclear.yard_bkr_info": "BKR 1-1 is %s. The switchyard's breakers follow the plant: closed while the generator runs, open when it stops or trips",
    "block.jugcraft.nuclear_equipment_frame": "Nuclear Equipment",
    "death.attack.jugcraft.radiation": "%1$s died of radiation sickness",
    "death.attack.jugcraft.radiation.player": "%1$s died of radiation sickness while fighting %2$s",
    "subtitles.jugcraft.geiger_counter": "Geiger counter clicks",
    "message.jugcraft.nuclear.no_room": "Not enough room: it needs %s wide, %s deep and %s tall, clear",
    "message.jugcraft.nuclear.console_unlinked": "Not linked: use a Reactor Link Card on the vessel, then on this console",
    "message.jugcraft.nuclear.edg_start": "Emergency diesel generator: air start... it fires and comes up to speed",
    "message.jugcraft.nuclear.edg_no_fuel": "No diesel: it cranks on air but won't fire. Its day tank (a storage tank of diesel within %s blocks) is empty or missing, or the tank's outlet valve is shut",
    "message.jugcraft.nuclear.edg_stop": "Emergency diesel generator: stopping, coasting down",
    "message.jugcraft.nuclear.edg_no_air": "Not enough starting air: it barely turns over and won't start (%s psig in the receivers; it needs %s). Its compressors refill them",
    # The diesel rooms' panels' controls (round 31 part 2, nuclear/DieselPanels.java).
    "message.jugcraft.nuclear.edg_status": "%s: %s; starting air %s psig; MODE %s%s",
    "message.jugcraft.nuclear.edg_mode": "%s MODE: %s",
    "message.jugcraft.nuclear.edg_mode_off": "%s: its MODE switch is at OFF (turn it to LOCAL or AUTO on its engine panel)",
    "message.jugcraft.nuclear.edg_not_auto": "%s is in %s: it answers the control room only with its MODE switch at AUTO",
    "message.jugcraft.nuclear.edg_not_local": "%s is in %s: turn its MODE switch to LOCAL to start or stop it here",
    "message.jugcraft.nuclear.edg_lockout": "%s: its 86G lockout is tripped (reset it at the generator protection panel)",
    "message.jugcraft.nuclear.edg_estop_in": "%s: its EMERG STOP is pushed in (pull it out at the engine panel)",
    "message.jugcraft.nuclear.edg_estop": "%s: EMERGENCY STOP (latched in; push it again to pull it out)",
    "message.jugcraft.nuclear.edg_estop_out": "%s: emergency stop pulled out",
    "message.jugcraft.nuclear.edg_stopped": "%s is already stopped",
    "message.jugcraft.nuclear.edg_bkr_tripped": "%s: generator breaker 52G TRIPPED: the loads drop, the engine runs on light",
    "message.jugcraft.nuclear.edg_bkr_open": "%s: generator breaker 52G is already open",
    "message.jugcraft.nuclear.edg_bkr_closed": "%s: generator breaker 52G CLOSED: the load sequencer starts the loads",
    "message.jugcraft.nuclear.edg_bkr_already": "%s: generator breaker 52G is already closed",
    "message.jugcraft.nuclear.edg_bkr_not_running": "%s: the engine isn't running (nothing to close the breaker on)",
    "message.jugcraft.nuclear.edg_bkr_not_ready": "%s: not up to speed and voltage yet",
    "message.jugcraft.nuclear.edg_relay_test": "%s: relay %s tested: its target dropped and it tripped the 86G lockout",
    "message.jugcraft.nuclear.edg_relay_reset": "%s: relay %s target reset",
    "message.jugcraft.nuclear.edg_relay_ok": "%s: relay %s: no target (its test switch tests it)",
    "message.jugcraft.nuclear.edg_lor_manual": "%s: 86G lockout TRIPPED: breaker open, engine stopped and locked out",
    "message.jugcraft.nuclear.edg_lor_reset": "%s: 86G lockout reset",
    "message.jugcraft.nuclear.edg_panel_no_edg": "This panel isn't linked to a diesel generator (none near)",
    "message.jugcraft.nuclear.air_receiver": "Starting air receiver %s: %s psig (%s)",
    "message.jugcraft.nuclear.air_compressor": "Starting air compressor %s: %s (%s; cuts in below %s psig, stops at %s)",
    "message.jugcraft.nuclear.air_dryer": "Starting air dryer %s: %s",
    "message.jugcraft.nuclear.air_unlinked": "not joined to a diesel generator (none within %s blocks)",
    "message.jugcraft.nuclear.air_linked": "joined to the diesel generator %s blocks away",
    "message.jugcraft.nuclear.air_running": "running",
    "message.jugcraft.nuclear.air_standby": "standing by",
    "message.jugcraft.nuclear.air_lead": "lead",
    "message.jugcraft.nuclear.air_lag": "lag",
    # The diesel rooms' own plant and CO2 system (round 31 part 3: nuclear/DieselRoom.java, Co2System.java, Monorail.java).
    "message.jugcraft.nuclear.room_piece": "%s %s: %s",
    "message.jugcraft.nuclear.room_co2": "%s %s: %s (%s)",
    "message.jugcraft.nuclear.room_hoist": "%s %s: trolley at %s of %s along the beam, hook %s blocks down (drive it from its pendant station)",
    "message.jugcraft.nuclear.co2_state_0": "CO2 system normal",
    "message.jugcraft.nuclear.co2_state_1": "CO2 countdown: GET OUT",
    "message.jugcraft.nuclear.co2_state_2": "CO2 DISCHARGED: reset it at the release station",
    "message.jugcraft.nuclear.co2_state_3": "CO2 purge: the exhaust fans are clearing the room",
    "message.jugcraft.nuclear.co2_station": "%s CO2 release station: %s",
    "message.jugcraft.nuclear.co2_no_bank": "%s's room has no CO2 cylinder bank: nothing to release",
    "message.jugcraft.nuclear.co2_pulled": "CO2 RELEASE PULLED: %s's room floods in %s seconds. Get out, or press ABORT",
    "message.jugcraft.nuclear.co2_counting": "CO2 discharges in %s seconds (ABORT stops it)",
    "message.jugcraft.nuclear.co2_already": "%s's CO2 has already discharged (RESET to purge the room)",
    "message.jugcraft.nuclear.co2_aborted": "%s: CO2 release aborted",
    "message.jugcraft.nuclear.co2_no_abort": "There is no CO2 countdown to abort",
    "message.jugcraft.nuclear.co2_purge": "CO2 system reset: the exhaust fans purge the room for %s seconds",
    "message.jugcraft.nuclear.co2_purging": "Purging: %s seconds left",
    "message.jugcraft.nuclear.co2_use_abort": "The countdown is running: ABORT stops it",
    "message.jugcraft.nuclear.co2_normal": "%s: CO2 system normal",
    "message.jugcraft.nuclear.edg_co2": "%s won't start: its room's CO2 has discharged (reset it at the release station)",
    "message.jugcraft.nuclear.hoist_no_rail": "No monorail within %s blocks",
    "message.jugcraft.nuclear.hoist_end": "The trolley is at the end of the beam",
    "message.jugcraft.nuclear.hoist_low": "The hook is all the way down",
    "message.jugcraft.nuclear.hoist_high": "The hook is all the way up",
    "death.attack.jugcraft.co2": "%1$s suffocated in a CO2 discharge",
    "death.attack.jugcraft.co2.player": "%1$s suffocated in a CO2 discharge while fighting %2$s",
    "message.jugcraft.nuclear.console_linked": "Console linked to the reactor",
    # The control room's live boards (round 27, nuclear/ControlBoard.java).
    "message.jugcraft.nuclear.board_no_reactor": "This board finds no reactor within %s blocks",
    "message.jugcraft.nuclear.board_info": "Reactor %s%%  Tavg %s °F  Pressurizer %s psig  Generator %s MW",
    "message.jugcraft.nuclear.display_wall": "Plant overview display: %s; %s alarms in, %s not acknowledged",
    # Round 34: the plant overview screens (nuclear/OverviewScreenBlockEntity.java click).
    "message.jugcraft.nuclear.overview_page": "Plant overview: %s for %s seconds (SITE MAP goes back to the map)",
    "message.jugcraft.nuclear.overview_map": "Plant overview: back to the site map",
    "message.jugcraft.nuclear.overview_kept": "Plant overview: this page stays up another %s seconds",
    "message.jugcraft.nuclear.overview_none": "Plant overview: the %s has no plant of its own on this screen",
    "message.jugcraft.nuclear.overview_nosignal": "Plant overview: no control room boards within %s blocks",
    "message.jugcraft.nuclear.overview_status": "Plant overview: %s; %s alarms in, %s not acknowledged",
    # The cable spreading room (punch list #15: nuclear/SpreadingRoomFire.java, TrainFurnitureBlock).
    "message.jugcraft.nuclear.csr_status": "Pre-action releasing panel: %s / %s",
    "message.jugcraft.nuclear.csr_nothing": "Nothing to do: the system is normal",
    "message.jugcraft.nuclear.csr_acked": "Alarm acknowledged",
    "message.jugcraft.nuclear.csr_silenced": "Horns silenced",
    "message.jugcraft.nuclear.csr_still_fire": "Can't reset: the detectors still see fire",
    "message.jugcraft.nuclear.csr_still_flowing": "Can't reset: water is still flowing. Wait until the fire has been out a while",
    "message.jugcraft.nuclear.csr_reset": "System reset: the air is coming back up and the sprinklers are armed",
    "message.jugcraft.nuclear.csr_normal": "The system is normal and armed",
    "message.jugcraft.nuclear.csr_drill": "Drill: lamps and horns for 10 seconds, no water",
    # The plant computer room (older rooms round 2: nuclear/ServerRoom.java).
    "message.jugcraft.nuclear.srv_status": "Server room: %s",
    "message.jugcraft.nuclear.srv_crac_on": "%s main isolator ON: the unit runs when it is fed",
    "message.jugcraft.nuclear.srv_crac_off": "%s main isolator OFF: the unit stops",
    "message.jugcraft.nuclear.srv_nothing": "Nothing to acknowledge or silence: the system is normal",
    "message.jugcraft.nuclear.srv_acked": "Alarm acknowledged",
    "message.jugcraft.nuclear.srv_silenced": "Horn silenced (the strobe keeps flashing)",
    "message.jugcraft.nuclear.srv_still_fire": "Cannot reset: the detectors still see a fire",
    "message.jugcraft.nuclear.srv_refilled": "Reset: new agent cylinders fitted, the system is armed",
    "message.jugcraft.nuclear.srv_reset": "Reset: the system is normal and armed",
    "message.jugcraft.nuclear.srv_normal": "The system is normal",
    "message.jugcraft.nuclear.srv_drill": "Drill: the alarm and the 30 second countdown, no discharge",
    "message.jugcraft.nuclear.srv_busy": "No drill while the system is in alarm or released",
    "message.jugcraft.nuclear.srv_no_panel": "No clean agent releasing panel in reach",
    "message.jugcraft.nuclear.srv_abort_manual": "A manual release cannot be aborted",
    "message.jugcraft.nuclear.srv_abort_held": "Abort held: the countdown waits (keep holding it)",
    "message.jugcraft.nuclear.srv_abort_idle": "Abort pressed: nothing is counting down",
    "message.jugcraft.nuclear.srv_already": "The agent has already been released: reset the panel",
    "message.jugcraft.nuclear.srv_already_counting": "The manual release is already counting down",
    "message.jugcraft.nuclear.srv_pulled": "Manual release: the agent discharges in 30 seconds. Leave the room",
    "message.jugcraft.nuclear.csr_busy": "Can't run a drill now: reset the system first",
    "message.jugcraft.nuclear.csr_no_panel": "This pull station finds no pre-action releasing panel nearby",
    "message.jugcraft.nuclear.csr_already": "The sprinkler system has already released",
    "message.jugcraft.nuclear.csr_no_valve": "Fire alarm! But the releasing panel finds no valve station: no water",
    "message.jugcraft.nuclear.csr_pulled": "Fire alarm! The pre-action valve has tripped: water is in the pipes",
    "message.jugcraft.nuclear.csr_train_0": "Train A (orange)%s",
    "message.jugcraft.nuclear.csr_train_1": "Train B (blue)%s",
    "message.jugcraft.nuclear.csr_train_2": "Non-safety (grey)%s",
    "message.jugcraft.nuclear.csr_hanger": ", with hanger and tag",
    "message.jugcraft.nuclear.csr_no_hanger": "",
    "message.jugcraft.nuclear.board_no_pumps": "This reactor has no coolant pumps: its coolant circulates by itself",
    "message.jugcraft.nuclear.board_rcp_none": "RCP %s: there is no coolant pump in loop %s",
    "message.jugcraft.nuclear.board_rcp_stop": "RCP %s stopped: its loop's flow coasts down",
    "message.jugcraft.nuclear.board_rcp_start": "RCP %s started",
    "message.jugcraft.nuclear.board_no_turbine": "This unit has no turbine generator",
    "message.jugcraft.nuclear.board_guard": "Turbine trip guard lifted: use the switch again within 5 seconds to trip the turbine",
    "message.jugcraft.nuclear.board_turbine_tripped": "Turbine tripped: the stop valves shut and the generator breaker opens",
    # Turbine hall part 1 (nuclear/TurbineHall.java): the turbine's manual trip, its turning gear, the reheaters.
    "message.jugcraft.nuclear.turbine_status": "Main turbine: %s rpm, %s, %s MW",
    "message.jugcraft.nuclear.turbine_online": "on line",
    "message.jugcraft.nuclear.turbine_latched": "latched",
    "message.jugcraft.nuclear.turbine_turning_gear": "tripped, on its turning gear",
    "message.jugcraft.nuclear.turbine_stopped": "tripped and stopped",
    "message.jugcraft.nuclear.turbine_tripped": "tripped, coasting down",
    "message.jugcraft.nuclear.turbine_no_reactor": "This turbine isn't part of a working reactor unit",
    "message.jugcraft.nuclear.turbine_guard": "Trip guard lifted: pull the handle within 5 seconds to trip the turbine",
    "message.jugcraft.nuclear.turbine_tripped_local": "Turbine tripped at the front standard: the stop valves shut and the generator breaker opens",
    "message.jugcraft.nuclear.turbine_already_tripped": "The turbine is already tripped",
    "message.jugcraft.nuclear.tg_engaged": "Turning gear engaged: its motor rolls the shaft at 3 rpm",
    "message.jugcraft.nuclear.tg_disengaged": "Turning gear disengaged",
    "message.jugcraft.nuclear.tg_rolling": "The turbine is still turning (%s rpm): the turning gear only goes in at zero speed",
    "message.jugcraft.nuclear.msr_status": "Moisture separator reheater %s: shell %s psig, reheat steam %s F",
    "message.jugcraft.nuclear.msr_idle": "Moisture separator reheater %s: no turbine running near it",
    # Turbine hall part 2 (nuclear/TurbineHall.java): the feed train's pumps, their stations and the board's MFP switches.
    "message.jugcraft.nuclear.feed_idle": "Not joined to a working turbine: there is none within %s blocks, or it has no reactor",
    "message.jugcraft.nuclear.cp_started": "Condensate pump %s started",
    "message.jugcraft.nuclear.cp_already": "Condensate pump %s is already running",
    "message.jugcraft.nuclear.cp_stopped_auto": "Condensate pump %s stopped: it stands by in AUTO (another standby pump starts first)",
    "message.jugcraft.nuclear.cp_auto_on": "Condensate pump %s in AUTO: it starts when the pumps running can't carry the feed",
    "message.jugcraft.nuclear.cp_auto_off": "Condensate pump %s out of AUTO: it won't start by itself",
    "message.jugcraft.nuclear.mfp_tripped": "Main feed pump %s tripped: its turbine's stop valves shut",
    "message.jugcraft.nuclear.mfp_already_tripped": "Main feed pump %s is already tripped",
    "message.jugcraft.nuclear.mfp_reset": "Main feed pump %s reset: start it from the secondary board",
    "message.jugcraft.nuclear.mfp_not_tripped": "Main feed pump %s isn't tripped",
    "message.jugcraft.nuclear.condenser_status": "Condenser %s: %s in Hg vacuum, hotwell %s in",
    "message.jugcraft.nuclear.cp_status": "Condensate pump %s: %s (%s of 3 running), %s A",
    "message.jugcraft.nuclear.mfp_status": "Main feed pump %s: %s, %s rpm, discharge %s psig",
    "message.jugcraft.nuclear.pump_running": "running",
    "message.jugcraft.nuclear.pump_standby": "standing by in AUTO",
    "message.jugcraft.nuclear.pump_stopped": "stopped",
    "message.jugcraft.nuclear.pump_tripped": "tripped",
    "message.jugcraft.nuclear.lo_status": "Main turbine lube oil: %s psig to the bearings at %s F, %s",
    "message.jugcraft.nuclear.lo_tg": "the turning gear oil pump running",
    "message.jugcraft.nuclear.lo_aux": "the AC auxiliary oil pump running",
    "message.jugcraft.nuclear.lo_mop": "the shaft-driven main oil pump supplying it",
    "message.jugcraft.nuclear.ehc_status": "EHC fluid: %s psig at %s F (pump A running, pump B standing by)",
    "message.jugcraft.nuclear.so_status": "Generator seal oil: %s psig, %s psid over the hydrogen",
    "message.jugcraft.nuclear.h2_status": "Generator hydrogen: %s%% pure at %s psig",
    "message.jugcraft.nuclear.lo_cond_status": "Lube oil conditioner: %s psid across its coalescer filter, %s",
    "message.jugcraft.nuclear.lo_cond_heating": "running with its heater on (the oil is cool while the turbine is below speed)",
    "message.jugcraft.nuclear.lo_cond_running": "running",
    "message.jugcraft.nuclear.lo_tank_status": "%s lube oil storage tank: %s%% full",
    "message.jugcraft.nuclear.lo_xfer_status": "Lube oil transfer pumps: both standing by (they run to fill or empty the main oil reservoir)",
    "message.jugcraft.nuclear.crane_status": "Overhead crane: %s. Bridge %s, trolley %s across, main hook %s down, aux hook %s down (blocks)",
    "message.jugcraft.nuclear.crane_on": "power on",
    "message.jugcraft.nuclear.crane_power_off": "power off (STOP pressed: press START on a pendant)",
    "message.jugcraft.nuclear.crane_stopped": "Crane STOP: its power is off until START",
    "message.jugcraft.nuclear.crane_power": "Crane power on: hold a button to run it",
    "message.jugcraft.nuclear.crane_off": "The crane's power is off: press START first",
    "message.jugcraft.nuclear.crane_end_bridge": "The bridge is at the end of its runway",
    "message.jugcraft.nuclear.crane_end_trolley": "The trolley is at the end of the bridge",
    "message.jugcraft.nuclear.crane_end_floor": "The hook is down on the floor",
    "message.jugcraft.nuclear.crane_end_top": "The hook is up at its top limit",
    "message.jugcraft.nuclear.crane_none": "There is no overhead crane within reach of this pendant",
    "message.jugcraft.nuclear.tcr_status": "%s: %s",
    "message.jugcraft.nuclear.board_no_mfp": "There is no main feed pump %s at this unit's turbine hall: its main feed runs by itself",
    "message.jugcraft.nuclear.board_mfp_tripped": "Main feed pump %s is tripped: reset it at its station first",
    "message.jugcraft.nuclear.board_mfp_start": "Main feed pump %s started",
    "message.jugcraft.nuclear.board_mfp_no_steam": "Main feed pump %s in service: it turns once the plant has steam (above 200 C)",
    "message.jugcraft.nuclear.board_mfp_stop": "Main feed pump %s stopped",
    "message.jugcraft.nuclear.board_turbine_already": "The turbine is already tripped",
    "message.jugcraft.nuclear.board_latched": "Turbine latched: it runs up to 1,800 rpm. Close the generator breaker on the electrical board to load it",
    "message.jugcraft.nuclear.board_latch_already": "The turbine is already latched",
    "message.jugcraft.nuclear.board_latch_blocked": "The turbine won't latch while the reactor is tripped: reset the reactor first",
    "message.jugcraft.nuclear.board_gen_open": "Generator breaker opened",
    "message.jugcraft.nuclear.board_gen_sync": "Generator breaker synchronizing: it closes when the synchroscope reaches 12 o'clock",
    "message.jugcraft.nuclear.board_gen_blocked": "Generator breaker won't close: the turbine isn't latched at speed (%s rpm)",
    "message.jugcraft.nuclear.board_gen_no_steam": "Generator breaker won't close: the reactor isn't making power",
    "message.jugcraft.nuclear.board_gen_closed": "The generator breaker is already closed",
    "message.jugcraft.nuclear.board_no_edg": "No diesel generator %s found within %s blocks",
    "message.jugcraft.nuclear.board_edg_running": "Diesel generator %s is already running",
    "message.jugcraft.nuclear.board_edg_stopped": "Diesel generator %s is already stopped",
    # Round 28: the safety systems, steam and feedwater boards.
    "message.jugcraft.nuclear.board_si_guard": "Safety injection train %s: guard lifted (click again to actuate)",
    "message.jugcraft.nuclear.board_si_actuated": "SAFETY INJECTION actuated from train %s: reactor and turbine tripped, diesels starting, ECCS pumps running, feedwater and containment isolated, aux feed starting",
    "message.jugcraft.nuclear.board_si_already": "Safety injection is already actuated",
    "message.jugcraft.nuclear.board_si_not": "Safety injection is not actuated",
    "message.jugcraft.nuclear.board_si_reset_wait": "Safety injection can be reset in %s s",
    "message.jugcraft.nuclear.board_si_reset": "Safety injection reset: feedwater and containment isolation cleared, SI and RHR pumps standing down (aux feed and diesels keep running)",
    "message.jugcraft.nuclear.board_no_sg": "This plant has no steam generator %s",
    "message.jugcraft.nuclear.board_msiv_closed": "MSIV %s closed: its steam goes out the relief valves",
    "message.jugcraft.nuclear.board_msiv_open": "MSIV %s open",
    "message.jugcraft.nuclear.board_no_afw": "This plant has no auxiliary feedwater (no steam generators)",
    "message.jugcraft.nuclear.board_afw_start": "Auxiliary feed pump %s started",
    "message.jugcraft.nuclear.board_afw_stop": "Auxiliary feed pump %s stopped",
    # Round 29: the remote shutdown panel, the valves and ARVs.
    "message.jugcraft.nuclear.board_rsp_not_local": "Turn a control transfer key to this panel first",
    "message.jugcraft.nuclear.board_rsp_guard": "Reactor trip %s: cover lifted (click again to trip)",
    "message.jugcraft.nuclear.board_rsp_already": "The reactor is already tripped",
    "message.jugcraft.nuclear.board_fcv_open": "Aux feed valve to SG %s open",
    "message.jugcraft.nuclear.board_fcv_shut": "Aux feed valve to SG %s shut: no aux feed to it",
    "message.jugcraft.nuclear.board_arv_open": "SG %s ARV open: steaming to atmosphere, the plant cools",
    "message.jugcraft.nuclear.board_arv_shut": "SG %s ARV closed",
    # Round 30: the reactor control console.
    "message.jugcraft.nuclear.board_rx_guard": "Reactor trip %s: guard lifted (click again to trip)",
    "message.jugcraft.nuclear.board_rx_guard_close": "Reactor trip %s: guard lifted (click again to turn it to CLOSE and reset the trip)",
    "message.jugcraft.nuclear.board_rx_tripped": "Reactor trip: the rods are going in",
    "message.jugcraft.nuclear.board_rx_closed": "Reactor trip breakers closed: the trip is reset, the shutdown banks are coming out",
    "message.jugcraft.nuclear.board_rods_out": "Rods out: bank %s at %s steps, power %s%%",
    "message.jugcraft.nuclear.board_rods_in": "Rods in: bank %s at %s steps, power %s%%",
    "message.jugcraft.nuclear.board_rods_in_auto": "Rod control is in AUTO: turn it to MAN to drive the rods",
    "message.jugcraft.nuclear.board_rods_tripped": "The reactor is tripped: reset it first (turn a reactor trip switch to CLOSE)",
    "message.jugcraft.nuclear.board_rods_refuel": "The reactor head is off for refuelling",
    "message.jugcraft.nuclear.board_rods_auto": "Rod control in AUTO: holding %s%% power",
    "message.jugcraft.nuclear.board_rods_man": "Rod control in MAN: the rods stay where they are until driven",
    "message.jugcraft.nuclear.board_block_wait": "The %s trip can only be blocked above %s",
    "message.jugcraft.nuclear.board_block_on": "%s trip blocked",
    "message.jugcraft.nuclear.board_block_reset": "%s trip block reset",
    "message.jugcraft.nuclear.board_rps_normal": "Reactor protection in NORMAL: its automatic trips are armed",
    "message.jugcraft.nuclear.board_rps_bypass": "Reactor protection BYPASSED: the reactor will not trip itself",
    "message.jugcraft.nuclear.panel_no_reactor": "No reactor within %s blocks: the remote shutdown panel runs the nearest one",
    "message.jugcraft.nuclear.panel_tripped": "Reactor trip: the rods are going in",
    "message.jugcraft.nuclear.panel_control_here": "Control transferred to the remote shutdown panel: the control room's console can only trip the reactor",
    "message.jugcraft.nuclear.panel_control_returned": "Control returned to the main control room",
    "message.jugcraft.nuclear.control_at_panel": "Control is at the remote shutdown panel: hand it back there first (you can still trip the reactor)",
    "message.jugcraft.nuclear.card_set": "Link card set to this reactor: now use it on a console",
    "message.jugcraft.nuclear.card_empty": "Use the card on a reactor vessel first",
    "message.jugcraft.nuclear.not_owner": "This reactor belongs to someone else",
    "message.jugcraft.nuclear.load_center": "Load center bus: %s of %s JE (%s%%), main breaker %s, %s JE/t coming in",
    "message.jugcraft.nuclear.mcc": "Motor control center bus: %s of %s JE (%s%%), main breaker %s, %s JE/t coming in",
    "message.jugcraft.nuclear.breaker": "%s breaker %s",
    "message.jugcraft.nuclear.breaker_open": "open",
    "message.jugcraft.nuclear.breaker_closed": "closed",
    "message.jugcraft.nuclear.breaker_not_owner": "This switchgear belongs to someone else",
    # The rebuilt load centres (older rooms round 1: nuclear/LoadCentre.java) and the switchgear room's laptop.
    "message.jugcraft.nuclear.lc_fans": "%s transformer fans: %s",
    "message.jugcraft.nuclear.lc_fans_manual": "MANUAL (running)",
    "message.jugcraft.nuclear.lc_fans_auto": "AUTO (on above 110 °C, off below 90 °C)",
    "message.jugcraft.nuclear.lc_no_power": "%s: no control power (racked out to DISCONNECT)",
    "message.jugcraft.nuclear.lc_tripped": "%s tripped open",
    "message.jugcraft.nuclear.lc_target": "%s will not close: its trip unit has tripped (press RESET on the trip unit)",
    "message.jugcraft.nuclear.lc_spring": "%s will not close: its closing spring is still charging",
    "message.jugcraft.nuclear.lc_closed": "%s closed, %s",
    "message.jugcraft.nuclear.lc_carrying": "carrying its load",
    "message.jugcraft.nuclear.lc_test_only": "in TEST (its load not connected)",
    "message.jugcraft.nuclear.lc_interlock": "%s is closed: open it before racking it (interlock)",
    "message.jugcraft.nuclear.lc_racked": "%s racked to %s",
    "message.jugcraft.nuclear.lc_reset": "%s trip unit reset",
    "message.jugcraft.nuclear.lc_reset_none": "%s trip unit: nothing to reset",
    "message.jugcraft.nuclear.sgr_laptop": "Trip unit viewer: %s",
    "message.jugcraft.nuclear.vital_breaker": "Circuit %s, %s: %s",
    "message.jugcraft.nuclear.vital_main": "Main breaker: %s",
    "message.jugcraft.nuclear.ground_status": "DC bus %s: no ground, each side %s V to ground; battery %s%% charged. Hold + TEST or - TEST to test",
    "message.jugcraft.nuclear.ground_test": "Ground test: %s side of DC bus %s grounded through the test resistor",
    "message.jugcraft.nuclear.ground_no_battery": "DC bus %s: no station battery in reach",
    "message.jugcraft.nuclear.battery_string": "Battery monitor %3$s: string %1$s of %2$s",
    # The DC rooms working (older rooms round 3: nuclear/DcRoom.java): the switchboard's breakers, the chargers' breakers
    # and MODE, the inverters' bypass switch and breaker, the annunciators' buttons, the laptop; what loses its DC.
    "message.jugcraft.nuclear.sb_main": "72-%s MAIN (the battery to the bus): %s",
    "message.jugcraft.nuclear.sb_feeder": "%s %s: %s",
    "message.jugcraft.nuclear.sb_status": "125 VDC switchboard %s: bus %s, battery %s%% charged, main %s, %s of 11 feeders closed, %s charger(s) feeding",
    "message.jugcraft.nuclear.charger_ac": "%s AC input breaker: %s",
    "message.jugcraft.nuclear.charger_dc": "%s DC output breaker: %s",
    "message.jugcraft.nuclear.charger_equalize": "%s MODE: EQUALIZE (the bus up to 139.8 V; back to FLOAT by itself in 24 hours)",
    "message.jugcraft.nuclear.charger_float": "%s MODE: FLOAT",
    "message.jugcraft.nuclear.charger_status": "%s: %s; %s alarm window(s) lit",
    "message.jugcraft.nuclear.inverter_dc": "%s DC input breaker: %s",
    "message.jugcraft.nuclear.inverter_bypass": "%s manual bypass: BYPASS (its load on its regulating transformer)",
    "message.jugcraft.nuclear.inverter_bypass_dead": "%s manual bypass: BYPASS, but its regulating transformer is dead: the vital panel has lost its power",
    "message.jugcraft.nuclear.inverter_normal": "%s manual bypass: NORMAL (its static switch carries the load)",
    "message.jugcraft.nuclear.inverter_status": "%s: %s; %s alarm window(s) lit",
    "message.jugcraft.nuclear.ann_ack": "%s annunciator: acknowledged (the windows go steady)",
    "message.jugcraft.nuclear.ann_ack_none": "%s annunciator: nothing new to acknowledge",
    "message.jugcraft.nuclear.ann_reset": "%s annunciator: reset, %s cleared window(s) dark",
    "message.jugcraft.nuclear.ann_reset_none": "%s annunciator: nothing to reset (a window stays lit while its alarm is in, and until it is acknowledged)",
    "message.jugcraft.nuclear.ann_test": "%s annunciator: lamp test",
    "message.jugcraft.nuclear.dc_laptop": "DC system monitor, page %s of %s: %s",
    "message.jugcraft.nuclear.lc_no_dc": "%s: no control power (its 125 VDC feeder %s is open or its bus dead)",
    "message.jugcraft.nuclear.edg_no_dc": "%s won't start: no 125 VDC control power (its switchboard feeder %s is open or the bus dead)",
    # The battery rooms (older rooms round 4: StationBatteryBlock, BatteryDisconnectBlock).
    "message.jugcraft.nuclear.battery_cell": "Cell %1$s of battery %2$s string %3$s: %4$s V, specific gravity %5$s, %6$s °C (%7$s °F)",
    "message.jugcraft.nuclear.bd_open": "%s OPEN: that string is off the bus (the other carries it)",
    "message.jugcraft.nuclear.bd_closed": "%s CLOSED: the string is back on the bus",
    "message.jugcraft.nuclear.bd_status": "%s: %s, %s (use the handle to switch it)",
    "message.jugcraft.nuclear.crc_page": "Screen: %s",
    "message.jugcraft.nuclear.crc_status": "%s (click a screen to change its page)",
    "message.jugcraft.nuclear.ac_hand": "Hand geometry: VERIFIED - %s",
    "message.jugcraft.nuclear.ac_duress_alarm": "DURESS ALARM at a security post (%s)",
    "message.jugcraft.nuclear.wd_alarm": "METAL DETECTED: %s - see the security officer",
    "message.jugcraft.nuclear.wd_pass": "Walk-through metal detector: PASS",
    "message.jugcraft.nuclear.wd_status": "Walk-through metal detector: %s passed through",
    "message.jugcraft.nuclear.xr_search": "X-ray: %s - SEARCH (set aside for the officer)",
    "message.jugcraft.nuclear.xr_clear": "X-ray: %s - CLEAR",
    "message.jugcraft.nuclear.xr_status": "X-ray baggage scanner: %s bags scanned (hold something and right-click to put it through)",
    "message.jugcraft.nuclear.sas_status": "Alarm station: %s, %s",
    "message.jugcraft.nuclear.fire_pull": "FIRE ALARM: a pull station was pulled by %s - leave the building by the nearest exit",
    "message.jugcraft.nuclear.chem_status": "Plant chemistry: %s in specification, %s at an action level (mode %s)",
    "message.jugcraft.nuclear.gs_start": "Gamma spectroscopy: counting sample %s (600 s live time)",
    "message.jugcraft.nuclear.gs_busy": "Gamma spectroscopy: counting, %s of %s s live time",
    "message.jugcraft.nuclear.ms_label": "Carriage %s: %s (turn the handwheel at its end to open the aisle beside it)",
    "message.jugcraft.nuclear.ms_moving": "The carriages are moving",
    "message.jugcraft.nuclear.ms_no_aisle": "No aisle to open: the bank is packed tight",
    "message.jugcraft.nuclear.ms_busy": "Someone is in the aisle: the carriages won't close on them",
    "message.jugcraft.nuclear.pf_open": "Plan file: %s pulled out",
    "message.jugcraft.nuclear.pf_shut": "Plan file: %s pushed shut",
    "message.jugcraft.nuclear.hotline": "Shift manager: %s",
    "message.jugcraft.nuclear.hotline_none": "The control room line rings out: no reactor in reach",
    "message.jugcraft.nuclear.ac_status": "Security post: %s inside, last passage %s",
    "message.jugcraft.nuclear.exhaust_fan": "Exhaust fan EF-%s: %s (right-click the selector to turn it)",
    "message.jugcraft.nuclear.relay_cabinet": "%s (sneak and right-click with an empty hand to change its label)",
    "message.jugcraft.nuclear.relay_no_reactor": "%s: no reactor in range to read",
    "message.jugcraft.nuclear.relay_relabel": "Label: %s",
    "message.jugcraft.nuclear.hvac_panel": "Control room HVAC, %s (right-click the mode selector to turn it, sneaking to turn it back)",
    "message.jugcraft.nuclear.hvac_mode": "CR HVAC mode: %s",
    "message.jugcraft.nuclear.hvac_damper": "%s (sneak and right-click with an empty hand to change its tag)",
    "message.jugcraft.nuclear.hvac_monitor": "%s",
    "message.jugcraft.nuclear.tsc": "%s",
    "message.jugcraft.nuclear.tsc_relabel": "Now showing: %s",
    "message.jugcraft.nuclear.hvac_running": "%s: running, as the HVAC panel has it (sneak and right-click with an empty hand for the other train's tag)",
    "message.jugcraft.nuclear.hvac_standby": "%s: on standby, as the HVAC panel has it (sneak and right-click with an empty hand for the other train's tag)",
    "message.jugcraft.nuclear.station_battery": "Station battery: %s%% charged (%s of %s JE), %s",
    "message.jugcraft.nuclear.battery_charging": "charging at %s JE/t",
    "message.jugcraft.nuclear.battery_discharging": "discharging at %s JE/t",
    "message.jugcraft.nuclear.battery_float": "full, on float",
    "message.jugcraft.nuclear.owner_only": "Only the owner can change who may use it",
    "message.jugcraft.nuclear.vessel_status": "Reactor: %s%% power, %s °C, %s of %s assemblies loaded",
    "message.jugcraft.nuclear.vessel_closed": "The vessel is closed: open it for refuelling from the console",
    "message.jugcraft.nuclear.loaded": "Assembly loaded (%s of %s)",
    "message.jugcraft.nuclear.unloaded": "Assembly taken out (%s of %s left)",
    "message.jugcraft.nuclear.core_full": "The core is full",
    "message.jugcraft.nuclear.spent_no": "Spent fuel can't go back in: store it behind shielding",
    "message.jugcraft.nuclear.core_empty": "The core is empty",
    "message.jugcraft.nuclear.cannot_reset": "Can't reset: the core must be below 350 °C (and its fuel undamaged)",
    "message.jugcraft.nuclear.cannot_reset_si": "Can't reset: safety injection is still in (reset it on the ESF board first)",
    "message.jugcraft.nuclear.refuel_not_shut_down": "Shut down first: rods fully in and the core below 120 °C",
    "message.jugcraft.nuclear.condenser_water": "Cooling water: %s of %s mB",
    "message.jugcraft.nuclear.showcase": "Built a running reactor unit (vessel at %s)",
    "message.jugcraft.nuclear.built": "Built %s: %s blocks, %s pieces of equipment, %s reactor(s), in %s s",
    "message.jugcraft.nuclear.plant_unknown": "No pre-made plant called %s: try small, medium or large",
    "message.jugcraft.nuclear.plant_creative": "Pre-made plants are built in creative mode (or with cheats on)",
}

R = "screen.jugcraft.reactor."
CONSOLE_LANG = {
    R + "title": "REACTOR CONTROL", R + "out_of_range": "Linked reactor at %s is out of range: come closer", R + "unlinked": "Not linked: use a Reactor Link Card on the vessel, then on this console",
    R + "header": "UNIT 1  ·  %s-LOOP PWR  ·  LEU",
    R + "header_epr": "UNIT 1  ·  EPR  ·  LEU", R + "header_smr": "MODULE  ·  SMALL MODULAR REACTOR  ·  LEU",
    R + "status.incomplete": "UNIT INCOMPLETE", R + "status.scram": "SCRAM", R + "status.refuelling": "REFUELLING",
    R + "status.operating": "OPERATING", R + "status.shut_down": "SHUT DOWN", R + "status.remote": "CONTROL AT REMOTE SHUTDOWN PANEL",
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
# The remote shutdown panel's screen (round 24, client/RemoteShutdownScreen.java).
P = "screen.jugcraft.remote_shutdown."
CONSOLE_LANG.update({
    P + "title": "REMOTE SHUTDOWN PANEL", P + "no_reactor": "No reactor within %s blocks: the panel runs the nearest one",
    P + "status.no_signal": "NO SIGNAL", P + "status.tripped": "REACTOR TRIPPED", P + "status.operating": "OPERATING",
    P + "status.shut_down": "SHUT DOWN", P + "reactor": "REACTOR", P + "found": "Nearest reactor: %s, %s, %s",
    P + "distance": "%s blocks from this panel", P + "design_pwr": "Pressurized water reactor", P + "design_epr": "EPR",
    P + "design_smr": "Small modular reactor", P + "power": "POWER", P + "rods": "RODS IN", P + "temperature": "CORE TEMP",
    P + "output": "OUTPUT", P + "rods_bar": "ROD TRAVEL (FULL: ALL IN)", P + "fuel_damaged": "FUEL DAMAGED",
    P + "control": "CONTROL", P + "at_mcr": "Main control room", P + "at_panel": "This panel (control room locked out)",
    P + "take": "TAKE CONTROL", P + "return": "RETURN CONTROL", P + "trip": "REACTOR TRIP",
    P + "trip_line": "Drives every rod in", P + "tripped_line": "Tripped: rods going in", P + "trip_line2": "Reset from the console",
    P + "mode_personal": "PERSONAL", P + "mode_party": "PARTY",
})

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
    # Its control rods (docs/features/boric-acid.md): boric acid's boron absorbs neutrons, in tungsten-clad steel.
    "control_rod_assembly": (["TBT", "TBT", "SBS"], {"T": "#c:plates/tungsten", "B": "jugcraft:boric_acid",
                                                    "S": "#c:plates/steel"}, 1),
    "reactor_pressure_vessel": (["TCT", "RFR", "BBB"], {"T": "#c:plates/tungsten", "C": "jugcraft:processor",
                                                       "R": "jugcraft:control_rod_assembly", "B": "#c:storage_blocks/steel",
                                                       "F": "jugcraft:leu_fuel_assembly"}, 1),
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
    "plant_display_wall": (["GGG", "CPC", "SSS"], {"G": "jugcraft:wall_display", "C": "jugcraft:advanced_circuit", "P": "jugcraft:processor", "S": "#c:plates/steel"}, 1),
    "main_control_board": (["RGR", "CCC", "SSS"], {"R": "minecraft:redstone_lamp", "G": "minecraft:glass_pane", "C": "jugcraft:advanced_circuit", "S": "#c:plates/steel"}, 1),
    "switchgear_lineup": (["SRS", "CAC", "SSS"], {"S": "#c:plates/steel", "R": "minecraft:redstone", "C": "minecraft:copper_ingot", "A": "jugcraft:aluminum_cable"}, 1),
    "air_handling_unit": (["SBS", "WMW", "SSS"], {"S": "#c:plates/steel", "B": "minecraft:iron_bars", "W": "minecraft:white_wool", "M": "jugcraft:electric_motor"}, 1),
    # The system boards: a control board's recipe with the system's colour in the middle.
    **{board: (["RGR", "CDC", "SSS"], {"R": "minecraft:redstone_lamp", "G": "minecraft:glass_pane", "C": "jugcraft:advanced_circuit",
                                         "D": f"minecraft:{dye}_dye", "S": "#c:plates/steel"}, 1)
       for board, dye in (("esf_control_board", "orange"), ("rcs_control_board", "red"), ("secondary_control_board", "yellow"),
                          ("turbine_control_board", "purple"), ("electrical_control_board", "black"),
                          ("radiation_control_board", "light_blue"))},
    "remote_shutdown_panel": (["RGR", "CKC", "SSS"], {"R": "minecraft:redstone_lamp", "G": "minecraft:glass_pane", "C": "jugcraft:advanced_circuit",
                                                     "K": "minecraft:tripwire_hook", "S": "#c:plates/steel"}, 1),
    "mcc_lineup": (["SLS", "CAC", "SSS"], {"S": "#c:plates/steel", "L": "minecraft:lever", "C": "minecraft:copper_ingot", "A": "jugcraft:aluminum_cable"}, 1),
    "cr_filtration_unit": (["SBS", "CMC", "SSS"], {"S": "#c:plates/steel", "B": "minecraft:iron_bars", "C": "minecraft:charcoal", "M": "jugcraft:electric_motor"}, 1),
    "cr_air_handling_unit": (["SBS", "WMW", "SPS"], {"S": "#c:plates/steel", "B": "minecraft:iron_bars", "W": "minecraft:white_wool", "M": "jugcraft:electric_motor",
                                                     "P": "jugcraft:steel_fluid_pipe"}, 1),
    "cr_chiller": (["SMS", "PCP", "SSS"], {"S": "#c:plates/steel", "M": "jugcraft:electric_motor", "P": "jugcraft:steel_fluid_pipe", "C": "minecraft:copper_block"}, 1),
    "chw_pump": (["PMP", " S "], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "M": "jugcraft:electric_motor"}, 1),
    "edg_air_receiver": (["SPS", "S S", "SGS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "G": "minecraft:redstone"}, 1),
    "edg_air_compressor": (["PMP", "SGS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "M": "jugcraft:electric_motor", "G": "#c:gears/steel"}, 1),
    "edg_air_dryer": (["P P", "SRS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "R": "minecraft:redstone"}, 1),
    "edg_jw_keepwarm": (["PHP", "SMS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "H": "minecraft:blast_furnace", "M": "jugcraft:electric_motor"}, 1),
    "edg_lo_keepwarm": (["PHP", "SMS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "H": "minecraft:furnace", "M": "jugcraft:electric_motor"}, 1),
    "edg_expansion_tank": (["SBS", "S S", "I I"], {"S": "#c:plates/steel", "B": "minecraft:bucket", "I": "minecraft:iron_bars"}, 1),
    "edg_jw_cooler": (["PSP", "CCC", "PSP"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "C": "minecraft:copper_ingot"}, 1),
    "edg_ngr": (["SBS", "CRC", "SSS"], {"S": "#c:plates/steel", "B": "minecraft:iron_bars", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, 1),
    "edg_gen_cubicle": (["SGS", "CCC", "SSS"], {"S": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "minecraft:copper_ingot"}, 1),
    "edg_co2_bank": (["PPP", "III", "III"], {"P": "jugcraft:steel_fluid_pipe", "I": "minecraft:iron_ingot"}, 1),
    "edg_monorail": (["SSS", "CHC"], {"S": "#c:plates/steel", "C": "minecraft:chain", "H": "jugcraft:electric_motor"}, 1),
    "condenser_shell": (["SPS", "CCC", "SPS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe", "C": "minecraft:copper_ingot"}, 1),
    "condenser_hotwell": (["S S", "S S", "SPS"], {"S": "#c:plates/steel", "P": "jugcraft:steel_fluid_pipe"}, 1),
    "condensate_pump": (["M", "P", "S"], {"M": "jugcraft:electric_motor", "P": "jugcraft:steel_fluid_pipe", "S": "#c:plates/steel"}, 1),
    "main_feed_pump": (["GPG", "SSS"], {"G": "#c:gears/steel", "P": "jugcraft:steel_fluid_pipe", "S": "#c:plates/steel"}, 1),
    "lube_oil_reservoir": (["MMM", "SBS", "SSS"], {"M": "jugcraft:electric_motor", "S": "#c:plates/steel", "B": "minecraft:bucket"}, 1),
    "ehc_unit": (["MPM", "SBS"], {"M": "jugcraft:electric_motor", "P": "jugcraft:steel_fluid_pipe", "S": "#c:plates/steel", "B": "minecraft:bucket"}, 1),
    "seal_oil_unit": (["MPM", "SBS"], {"M": "jugcraft:electric_motor", "P": "jugcraft:steel_fluid_pipe", "S": "#c:plates/steel", "B": "minecraft:bucket"}, 1),
    "h2_gas_panel": (["SGS", "III"], {"S": "#c:plates/steel", "G": "minecraft:glass_pane", "I": "minecraft:iron_ingot"}, 1),
    "lo_conditioner": (["MPM", "SBS", "SSS"], {"M": "jugcraft:electric_motor", "P": "jugcraft:steel_fluid_pipe", "S": "#c:plates/steel", "B": "minecraft:bucket"}, 1),
    "lo_storage_tank": (["SSS", "SBS", "SSS"], {"S": "#c:plates/steel", "B": "minecraft:bucket"}, 1),
    "lo_transfer_pumps": (["MPM", "SSS"], {"M": "jugcraft:electric_motor", "P": "jugcraft:steel_fluid_pipe", "S": "#c:plates/steel"}, 1),
    "cooling_tower_fill": (["PPP", "SSS", "PPP"], {"P": "jugcraft:plastic_sheet", "S": "minecraft:stick"}, 8),
    "conduit_trench": (["CCC", "PPP", "CCC"], {"C": "minecraft:smooth_stone", "P": "jugcraft:circulating_water_conduit"}, 3),
    "site_structure": (["CIC", "ISI", "CIC"], {"C": "minecraft:gray_concrete", "I": "minecraft:iron_ingot", "S": "#c:plates/steel"}, 8),
    "site_ladder": (["S", "L"], {"S": "jugcraft:site_structure", "L": "minecraft:ladder"}, 1),
    TOWER_CONTROLLER: (["CPC", "PAP", "CPC"], {"C": "minecraft:gray_concrete", "P": "jugcraft:steel_fluid_pipe",
                                              "A": "jugcraft:advanced_circuit"}, 1),
    WORKS_CONTROLLER: (["CPC", "PAP", "CEC"], {"C": "minecraft:gray_concrete", "P": "jugcraft:steel_fluid_pipe",
                                              "A": "jugcraft:advanced_circuit", "E": "jugcraft:electric_motor"}, 1),
    YARD_CONTROLLER: (["CPC", "WAW", "CPC"], {"C": "minecraft:gray_concrete", "P": "#c:plates/aluminum",
                                             "A": "jugcraft:advanced_circuit", "W": "jugcraft:aluminum_cable"}, 1),
    "reactor_coolant_pipe": (["SSS", "   ", "SSS"], {"S": "#c:plates/steel"}, 6),
    "insulated_steam_line": (["WWW", "SSS", "WWW"], {"W": "minecraft:white_wool", "S": "#c:plates/steel"}, 6),
    "circulating_water_conduit": (["CCC", "S S", "CCC"], {"C": "minecraft:gray_concrete", "S": "#c:plates/steel"}, 6),
    "isolated_phase_bus": (["AAA", "CCC", "AAA"], {"A": "#c:plates/aluminum", "C": "minecraft:copper_ingot"}, 6),
}
CRAFTING.update(plant_interior.CRAFTING)
CRAFTING.update(storage_tank.CRAFTING)
CRAFTING.update(nuclear_storage.CRAFTING)
ITEM_FEATURES = {"plastic_sheet": ["crude_oil"], "processor": ["silicon"], "advanced_circuit": ["silver", "nickel"],
                 "leu_fuel_assembly": ["uranium"], "aluminum_cable": ["aluminum"], "rubber": ["crude_oil"],
                 "tank_rubber_lining": ["crude_oil"], "ceramic_insulator": ["drones"], "cryogenic_vaporizer": ["aluminum"]}
BASE_METALS = ("copper", "iron", "gold", "steel")

# The textures made here at 64x64 (tools/check_mod_data.py allows these by name).
NU_TEXTURES = ["nu_centrifuge", "nu_cap", "nu_steel", "nu_floor", "nu_cabinet", "nu_glass", "nu_hazard", "nu_pipe_blue",
               "nu_pipe_orange"]
PIPE_TEXTURES = list(PIPES) + [p + "_end" for p in PIPES]
TOWER_TEXTURES = [f"{TOWER_CONTROLLER}_side", f"{TOWER_CONTROLLER}_top", f"{TOWER_CONTROLLER}_grate", "cooling_tower_fill",
                  "cooling_tower_fill_side", "conduit_trench"]
HI_RES = (set(ITEMS) - {"geiger_counter"}) | set(BLOCKS) | set(NU_TEXTURES) | set(PIPE_TEXTURES) | set(TOWER_TEXTURES) | {
    "geiger_case", "geiger_rubber", "geiger_steel", "geiger_grille", "geiger_label"} | plant_interior.HI_RES \
    | set(storage_tank.TEXTURES)


# Textures kept at 256 px so their lettering reads (tools/check_mod_data.py).
SHARP = {"geiger_screen"}


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
        a = 90 + k * 120  # ISO 361: one blade pointing down, two up
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


# The Geiger counter's dial (round 14): a real moving needle over a log scale from background to 100 Sv/h, in the
# same bands as the readout (client/RadiationMeter.java): SAFE (green) below 50 uSv/h, CAUTION (amber) up to 1 Sv/h,
# LETHAL (red) above. Kept in step with RadiationMeter.LOW/HIGH/SAFE/LETHAL.
DIAL_LOW, DIAL_HIGH = -4, 5             # log10 mSv/h: 0.1 uSv/h .. 100 Sv/h
DIAL_SAFE, DIAL_LETHAL = math.log10(0.05), 3.0
DIAL_SWEEP = 50                         # degrees either side of upright
DIAL_STEPS = 48                         # needle positions (item models)
SCREEN = ((4.6, 4.4), (11.4, 9.2))      # the screen face, model px (x, y): 6.8 x 4.8
DIAL_PIVOT = (170, 228)                 # in the 340 x 240 drawing
DIAL_R = 190


def dial_fraction(log_rate):
    return min(1.0, max(0.0, (log_rate - DIAL_LOW) / (DIAL_HIGH - DIAL_LOW)))


def geiger_dial():
    """The dial face: cream card, coloured band, decade ticks and figures, unit names; drawn at 340 x 240 (the face's
    6.8 x 4.8 model px) and squeezed into a 256 px square texture."""
    W, H, f = 340, 240, 4
    big = Image.new("RGBA", (W * f, H * f), (22, 24, 26, 255))
    d = ImageDraw.Draw(big)
    cx, cy, R = DIAL_PIVOT[0] * f, DIAL_PIVOT[1] * f, DIAL_R * f
    d.rounded_rectangle([6 * f, 6 * f, (W - 6) * f, (H - 6) * f], radius=10 * f, fill=(236, 232, 216, 255))

    def ang(frac):
        return math.radians(-90 - DIAL_SWEEP + 2 * DIAL_SWEEP * frac)   # PIL: 0 = east, clockwise

    def pt(frac, r):
        a = ang(frac)
        return cx + math.cos(a) * r, cy + math.sin(a) * r

    # The coloured band just inside the scale.
    bands = [(0, dial_fraction(DIAL_SAFE), (70, 176, 84)), (dial_fraction(DIAL_SAFE), dial_fraction(DIAL_LETHAL), (232, 172, 30)),
             (dial_fraction(DIAL_LETHAL), 1, (212, 52, 40))]
    for f0, f1, col in bands:
        a0, a1 = math.degrees(ang(f0)), math.degrees(ang(f1))
        d.arc([cx - R + 14 * f, cy - R + 14 * f, cx + R - 14 * f, cy + R - 14 * f], a0, a1, fill=col + (255,), width=16 * f)
    # The scale arc and ticks: a long tick each decade, short ones at 2 and 5.
    a0, a1 = math.degrees(ang(0)), math.degrees(ang(1))
    d.arc([cx - R, cy - R, cx + R, cy + R], a0, a1, fill=(20, 20, 22, 255), width=3 * f)
    font_n = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", 15 * f)
    labels = {-3: "1", -2: "10", -1: "100", 0: "1", 1: "10", 2: "100", 3: "1", 4: "10"}
    for dec in range(DIAL_LOW, DIAL_HIGH + 1):
        for m, long_ in ((1, True), (2, False), (5, False)):
            lr = dec + math.log10(m)
            if lr > DIAL_HIGH:
                continue
            fr = dial_fraction(lr)
            r0 = R - (16 if long_ else 9) * f
            d.line([pt(fr, R + 1 * f), pt(fr, r0)], fill=(20, 20, 22, 255), width=(3 if long_ else 2) * f)
        if dec in labels:
            x, y = pt(dial_fraction(dec), R + 17 * f)
            d.text((x, y), labels[dec], font=font_n, fill=(20, 20, 22, 255), anchor="mm")
    # Unit names under each third of the scale, and the zone words.
    font_u = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", 13 * f)
    for lo, hi, unit in ((-4, -1, "\u00b5Sv/h"), (-1, 2, "mSv/h"), (2, 5, "Sv/h")):
        x, y = pt(dial_fraction((lo + hi) / 2 + 0.5), R - 44 * f)
        d.text((x, y), unit, font=font_u, fill=(60, 60, 64, 255), anchor="mm")
    font_z = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", 12 * f)
    for frac, word, col in ((dial_fraction(DIAL_SAFE) / 2, "SAFE", (40, 120, 52)),
                            ((dial_fraction(DIAL_SAFE) + dial_fraction(DIAL_LETHAL)) / 2, "CAUTION", (150, 104, 10)),
                            ((dial_fraction(DIAL_LETHAL) + 1) / 2, "LETHAL", (176, 30, 24))):
        x, y = pt(frac, R - 74 * f)
        d.text((x, y), word, font=font_z, fill=col + (255,), anchor="mm")
    # Maker's line and a little trefoil at the bottom.
    font_s = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed.ttf", 10 * f)
    d.text((W * f // 2, (H - 18) * f), "DOSE RATE", font=font_s, fill=(70, 70, 74, 255), anchor="mm")
    return big.resize((256, 256), Image.LANCZOS)


def geiger_needle_texture():
    img = Image.new("RGBA", (16, 16), (24, 24, 26, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 5], fill=(214, 60, 30, 255))   # the tip end is painted orange (top of the needle)
    return img


def _geiger_textures():
    case = _surface((232, 190, 40), 50, fine=6, broad=10)
    case_img = _img(case)
    d = ImageDraw.Draw(case_img)
    for y in range(0, 64, 16):
        d.line([(0, y), (63, y)], fill=(196, 156, 26, 255))
    screen = geiger_dial()
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
            "geiger_grille": grille, "geiger_label": _supersample(label), "geiger_needle": geiger_needle_texture()}


def draw_item_textures():
    out = {"hazmat_hood": _icon(_hood), "hazmat_coverall": _icon(_coverall), "hazmat_trousers": _icon(_trousers),
           "hazmat_boots": _icon(_boots), "check_source": _icon(_check_source)}
    out.update(_geiger_textures())
    out["enriched_uranium_cylinder"] = _icon(_icon_cylinder)
    out["leu_fuel_assembly"] = _icon(_icon_assembly(False))
    out["spent_fuel_assembly"] = _icon(_icon_assembly(True))
    out["reactor_link_card"] = _icon(_icon_card)
    out.update(plant_interior.draw_item_textures())
    out["lead_door"] = nuclear_storage.door_item_icon()
    out.update(storage_tank.draw_item_textures())
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
    out.update(storage_tank.draw_block_textures())
    out.update(draw_tower_controller())
    grate = draw_cooling_blocks()
    out.update({k: v for k, v in grate.items() if k != "cooling_tower_controller_grate"})
    out["cooling_tower_controller_grate"] = grate["cooling_tower_controller_grate"]
    out.update(nuclear_storage.textures())
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
        _el([SCREEN[0][0], SCREEN[0][1], 10], [SCREEN[1][0], SCREEN[1][1], 10.4], "rubber", {"south": "screen"}, {"south": full}),  # dial
        _el([5, 2, 10], [11, 4.0, 10.3], "grille", uv={"south": full}),                  # speaker grille
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


def geiger_needle_model(rid, k):
    """The dial's needle at position k of DIAL_STEPS (0 = the left stop), turned about its pivot; it inherits the
    counter's display transforms (and replaces its elements), so it sits on the dial in every view."""
    sx = (SCREEN[1][0] - SCREEN[0][0]) / 340
    sy = (SCREEN[1][1] - SCREEN[0][1]) / 240
    px = SCREEN[0][0] + DIAL_PIVOT[0] * sx
    py = SCREEN[1][1] - DIAL_PIVOT[1] * sy
    length = (DIAL_R - 8) * sy
    angle = round(DIAL_SWEEP - 2 * DIAL_SWEEP * k / DIAL_STEPS, 4)
    n = {"texture": "#needle", "uv": [0, 0, 16, 16]}
    needle = {"from": [round(px - 0.09, 4), round(py, 4), 10.46], "to": [round(px + 0.09, 4), round(py + length, 4), 10.5],
              "rotation": {"origin": [round(px, 4), round(py, 4), 10.48], "axis": "z", "angle": angle},
              "faces": {"south": n, "north": n, "east": n, "west": n, "up": n}}
    hub = {"from": [round(px - 0.28, 4), round(py - 0.28, 4), 10.42], "to": [round(px + 0.28, 4), round(py + 0.28, 4), 10.56],
           "faces": {f: {"texture": "#hub"} for f in ("south", "east", "west", "up", "down")}}
    return {"parent": rid("item/geiger_counter"), "textures": {"needle": rid("item/geiger_needle"), "hub": rid("item/geiger_rubber")},
            "elements": [needle, hub]}


def geiger_item(rid):
    """The counter and its needle on top, the needle chosen by the meter's reading (client/RadiationNeedle.java)."""
    entries = [{"threshold": round((k - 0.5) / DIAL_STEPS, 6), "model": {"type": "minecraft:model", "model": rid(f"item/geiger_needle_{k}")}}
               for k in range(1, DIAL_STEPS + 1)]
    return {"model": {"type": "minecraft:composite", "models": [
        {"type": "minecraft:model", "model": rid("item/geiger_counter")},
        {"type": "minecraft:range_dispatch", "property": rid("radiation_needle"), "entries": entries,
         "fallback": {"type": "minecraft:model", "model": rid("item/geiger_needle_0")}}]}}


# ---------------------------------------------------------------- the equipment's material sheet (512 wide, 64 px cells)
# client/NuclearRenderer.java reads these regions by name, in this order (checked by tools/check_mod_data.py).
# Textures are drawn the right way up: the renderer puts the top of a cell at the top of every vertical face.
# Eight cells across; nine rows since round 24 (512 x 576: client/NuclearRenderer.java ROWS).
EQ_SHEET = 1024
EQ_CELL = 128          # round 26, 8e: 128 px cells (the lettered ones drawn at that size; the rest at 64, doubled)
EQ_ROWS = 11
EQ_MATERIALS = ["stainless", "stainless_dark", "carbon_steel", "paint_grey", "paint_green", "paint_blue", "paint_white",
                "concrete", "grating", "hazard", "insulation", "weld_band", "flange", "crdm", "copper", "tank_grey",
                "radiator", "glaze_brown", "glass", "screen", "desk", "rubber", "lamp_red", "lamp_green",
                "trefoil_sign", "generator", "fins", "handrail", "nameplate", "turbine_blue", "pipe_red", "water",
                "paint_yellow", "mimic", "display", "cherenkov", "fuel_rack", "louvre", "switchgear", "filter_bank",
                "sign_panel", "sign_edge",
                # The emergency diesel generator (round 12): its engine, piping by service, panel and signs.
                "engine_green", "rocker", "gunmetal", "red_iron", "pipe_green", "pipe_brown",
                "pipe_yellow", "pipe_lblue", "crankcase_door", "gauge_board", "sign_autostart", "stencil_edg", "turbo", "black",
                # The control room's boards (round 23, tools/control_boards.py): lamps, windows, meters, charts, swatches.
                "lamp_white", "lamp_amber", "annunciator", "meter", "dial", "chart", "drpi", "swatches",
                # The remote shutdown panel (round 24): engraved nameplates and titles, digital indicators, placards,
                # procedure binders (tools/control_boards.py TEXT_MATERIALS).
                "nameplates", "nameplates2", "nameplates_inst", "titles", "digital", "placards", "binders",
                # The 480 V load centre's legends (round 25), the DC rooms' motor control centres' (round 26).
                "nameplates3", "nameplates4",
                # The control room's ventilation plant (round 26, #11: tools/hvac_boards.py): its units' title plates by
                # train, the stage and part plates, the warnings and the gauges' faces.
                "hvac_titles_efu", "hvac_titles_ahu", "hvac_titles_chiller", "hvac_titles_pump", "nameplates5", "nameplates6",
                "hvac_placards",
                # The main turbine-generator (turbine hall part 1, tools/turbine_set.py): the HP's and valves' lagging
                # (smooth sheet with its laps), the LP hoods' plate, the generator frame's paint.
                "turbine_lagging", "lp_hood", "gen_frame", "cond_shell"]


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


def _font(size, bold=True):
    from PIL import ImageFont
    return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/" + ("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"), size)


def _rocker_cover():
    """A cast aluminium rocker cover: dull, with raised ribs across it and a bolt at each corner."""
    import nuclear_materials as M
    img = M.brushed((172, 176, 174), 143, "h", dirt=0.25)
    d = ImageDraw.Draw(img)
    for y in range(20, 128, 22):
        d.rectangle([10, y, 117, y + 5], fill=(198, 202, 200, 255))
        d.line([(10, y + 6), (117, y + 6)], fill=(110, 114, 112, 255), width=2)
    for x, y in ((8, 8), (119, 8), (8, 119), (119, 119)):
        d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=(80, 84, 86, 255))
    return img


def _crankcase_door():
    """A crankcase inspection door: an oval cover on the engine's green, a ring of bolts and the explosion relief
    valve's round cap in the middle."""
    import nuclear_materials as M
    img = M.painted((66, 104, 80), 150, chip_count=3, drip_count=2, dirt=0.5)
    d = ImageDraw.Draw(img)
    d.ellipse([10, 22, 117, 105], fill=(80, 122, 94, 255), outline=(40, 64, 48, 255), width=4)
    for k in range(12):
        a = k / 12 * math.tau
        x, y = 64 + 48 * math.cos(a), 64 + 36 * math.sin(a)
        d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(40, 44, 46, 255))
    d.ellipse([46, 46, 82, 82], fill=(150, 154, 152, 255), outline=(60, 64, 66, 255), width=3)
    d.ellipse([58, 58, 70, 70], fill=(90, 94, 96, 255))
    return img


def _gauge_board():
    """The engine's gauge board: four white dials on black (lube oil, jacket water, fuel and starting air), each with
    its scale and red zone and a label strip under it. The needles are drawn over them, moving (PlantDecor.live)."""
    img = Image.new("RGBA", (128, 128), (28, 30, 32, 255))
    d = ImageDraw.Draw(img)
    for k in range(4):
        cx, cy = 32 + (k % 2) * 64, 30 + (k // 2) * 64
        d.ellipse([cx - 24, cy - 24, cx + 24, cy + 24], fill=(150, 154, 156, 255))
        d.ellipse([cx - 20, cy - 20, cx + 20, cy + 20], fill=(238, 238, 232, 255))
        d.arc([cx - 16, cy - 16, cx + 16, cy + 16], 140, 400, fill=(30, 30, 30, 255), width=2)
        d.arc([cx - 16, cy - 16, cx + 16, cy + 16], 340, 400, fill=(200, 40, 34, 255), width=3)
        for t in range(11):
            a = math.radians(140 + 26 * t)
            d.line([(cx + 12 * math.cos(a), cy + 12 * math.sin(a)), (cx + 16 * math.cos(a), cy + 16 * math.sin(a))], fill=(30, 30, 30, 255))
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(40, 40, 42, 255))
        d.rectangle([cx - 18, cy + 26, cx + 18, cy + 31], fill=(220, 220, 214, 255))
    return img


def _sign_autostart():
    """The sign every emergency diesel carries: CAUTION on a yellow band, and under it that the engine may start on
    its own at any time."""
    img = Image.new("RGBA", (128, 128), (244, 244, 238, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 127], outline=(20, 20, 22, 255), width=4)
    d.rectangle([4, 4, 123, 38], fill=(240, 196, 30, 255))
    d.text((64, 22), "CAUTION", font=_font(22), fill=(16, 16, 18, 255), anchor="mm")
    for k, line in enumerate(("THIS ENGINE", "STARTS", "AUTOMATICALLY")):
        d.text((64, 56 + k * 22), line, font=_font((15, 18, 13)[k]), fill=(16, 16, 18, 255), anchor="mm")
    return img


def _stencil_edg():
    """The generator's stencil: EDG, white on the frame's green."""
    import nuclear_materials as M
    img = M.painted((58, 118, 88), 152, chip_count=2, drip_count=1, dirt=0.2)
    d = ImageDraw.Draw(img)
    d.text((64, 66), "EDG", font=_font(52), fill=(240, 240, 234, 255), anchor="mm")
    return img


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
        "engine_green": lambda: M.painted((66, 104, 80), 142, seams=(("h", 64),), chip_count=4, drip_count=4, dirt=0.6),
        "rocker": _rocker_cover, "gunmetal": lambda: M.painted((62, 66, 70), 144, chip_count=4, drip_count=0, dirt=0.3),
        "red_iron": lambda: M.painted((160, 34, 30), 145, chip_count=6, drip_count=0, dirt=0.2),
        "pipe_green": lambda: M.pipe_stripe((64, 132, 84), 146), "pipe_brown": lambda: M.pipe_stripe((128, 82, 40), 147),
        "pipe_yellow": lambda: M.pipe_stripe((214, 172, 40), 148), "pipe_lblue": lambda: M.pipe_stripe((120, 170, 212), 149),
        "crankcase_door": _crankcase_door, "gauge_board": _gauge_board, "sign_autostart": _sign_autostart,
        "stencil_edg": _stencil_edg, "turbo": lambda: M.painted((96, 98, 100), 153, seams=(("v", 64),), chip_count=3, drip_count=2, dirt=0.7),
        "black": lambda: M.rubber(154, colour=(26, 27, 30)),
        "turbine_lagging": lambda: M.lagging((96, 128, 152), 155),
        "lp_hood": lambda: M.painted((70, 104, 132), 156, chip_count=2, drip_count=2, dirt=0.3),
        "gen_frame": lambda: M.painted((58, 112, 84), 157, chip_count=2, drip_count=2, dirt=0.3),
        "cond_shell": lambda: M.painted((150, 160, 152), 158, chip_count=1, drip_count=1, dirt=0.15),
    }
    import control_boards
    for name, make in control_boards.MAKERS.items():
        makers[name] = (lambda make=make, seed=160 + len(makers): make(seed))
    sheet = Image.new("RGBA", (EQ_SHEET, EQ_CELL * EQ_ROWS), (0, 0, 0, 0))
    for name in EQ_MATERIALS:
        col, row = eq_slot(name)
        if name in control_boards.LETTERED_CELLS:
            # Drawn at the cell's full size, its letters kept sharp (a small palette, no resizing).
            img = makers[name]().convert("RGBA")
            rgb = img.convert("RGB").quantize(colors=control_boards.COLOURS[name], method=Image.Quantize.MEDIANCUT,
                                              dither=Image.Dither.NONE).convert("RGBA")
            sheet.paste(rgb, (col * EQ_CELL, row * EQ_CELL))
            continue
        if name in control_boards.MAKERS:
            small = M.stylize(makers[name](), control_boards.COLOURS[name])
        else:
            many = ("mimic", "display", "fuel_rack", "switchgear", "gauge_board", "sign_autostart", "stencil_edg")
            small = M.stylize(makers[name](), 24 if name in many else 12 if name in ("screen", "desk", "lamp_red", "lamp_green", "cherenkov") else 10)
        # The game's 64 px look, each texel doubled to the sheet's 128 px cells.
        sheet.paste(small.resize((EQ_CELL, EQ_CELL), Image.NEAREST), (col * EQ_CELL, row * EQ_CELL))
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


# ---------------------------------------------------------------- fuel and card item icons (64 px)

def _cyl_icon(d, f, cx, y0, y1, r, body, dark, light, top=None):
    """A shaded upright cylinder, seen from slightly above."""
    S = lambda v: v * f
    d.rectangle([S(cx - r), S(y0), S(cx + r), S(y1)], fill=body)
    d.rectangle([S(cx - r), S(y0), S(cx - r * 0.45), S(y1)], fill=light)
    d.rectangle([S(cx + r * 0.55), S(y0), S(cx + r), S(y1)], fill=dark)
    d.ellipse([S(cx - r), S(y1 - r * 0.35), S(cx + r), S(y1 + r * 0.35)], fill=dark)
    d.ellipse([S(cx - r), S(y0 - r * 0.35), S(cx + r), S(y0 + r * 0.35)], fill=top or light)


STEEL_D = (110, 116, 124, 255)


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
    import control_boards
    control_boards.write(assets / "control_boards.json.gz")
    plant_interior.write_assets(write, rid, assets, lang)
    nuclear_storage.write_assets(write, rid, assets, lang)
    storage_tank.write_assets(write, rid, assets, lang)
    for item in ITEMS:
        lang[f"item.{MOD}.{item}"] = NAMES[item]
        if item == "geiger_counter":
            write(assets / "models" / "item" / f"{item}.json", geiger_model(rid))
            for k in range(DIAL_STEPS + 1):
                write(assets / "models" / "item" / f"geiger_needle_{k}.json", geiger_needle_model(rid, k))
            write(assets / "items" / f"{item}.json", geiger_item(rid))
            continue
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
        # The item shows the equipment itself, in miniature (tools/structure_items.py).
        structure_items.equipment_item(write, rid, assets, kind)
    lang[f"block.{MOD}.{TOWER_CONTROLLER}"] = NAMES[TOWER_CONTROLLER]
    lang[f"block.{MOD}.{WORKS_CONTROLLER}"] = NAMES[WORKS_CONTROLLER]
    lang[f"block.{MOD}.{YARD_CONTROLLER}"] = NAMES[YARD_CONTROLLER]
    lang[f"block.{MOD}.{REACTOR_CONTROLLER}"] = NAMES[REACTOR_CONTROLLER]
    lang[f"block.{MOD}.cooling_tower_fill"] = NAMES["cooling_tower_fill"]
    write(assets / "models" / "block" / "cooling_tower_fill.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": rid("block/cooling_tower_fill_side"), "top": rid("block/cooling_tower_fill"), "bottom": rid("block/cooling_tower_fill")}})
    write(assets / "blockstates" / "cooling_tower_fill.json", {"variants": {"": {"model": rid("block/cooling_tower_fill")}}})
    write(assets / "items" / "cooling_tower_fill.json", {"model": {"type": "minecraft:model", "model": rid("block/cooling_tower_fill")}})
    lang[f"block.{MOD}.conduit_trench"] = NAMES["conduit_trench"]
    # Plant Structure: no model of its own (client/SiteModel.java draws its site's quads); the item a precast block.
    for block, name in SITE_BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = name
        write(assets / "models" / "block" / f"{block}.json", {"textures": {"particle": rid("block/site/ct_precast")}})
        write(assets / "blockstates" / f"{block}.json", {"variants": {f"waterlogged={w}": {"model": rid(f"block/{block}")}
                                                                      for w in ("false", "true")}})
        write(assets / "models" / "item" / f"{block}.json", {"parent": "minecraft:block/cube_all", "textures": {
            "all": rid("block/site/ct_precast" if block == "site_structure" else "block/site/ct_grating")}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})
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
    # The pump houses' controllers (buried under the grass where the lines leave): the tower controller's grate and sides.
    write(assets / "blockstates" / f"{WORKS_CONTROLLER}.json", {"variants": {
        f"facing={f},works={w}": dict({"model": rid(f"block/{TOWER_CONTROLLER}")}, **({"y": turn[f]} if turn[f] else {}))
        for f in turn for w in WORKS}})
    write(assets / "items" / f"{WORKS_CONTROLLER}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{TOWER_CONTROLLER}")}})
    # The yards' controllers (buried under the grass): the same.
    write(assets / "blockstates" / f"{YARD_CONTROLLER}.json", {"variants": {
        f"facing={f},works={w}": dict({"model": rid(f"block/{TOWER_CONTROLLER}")}, **({"y": turn[f]} if turn[f] else {}))
        for f in turn for w in YARD_WORKS}})
    write(assets / "items" / f"{YARD_CONTROLLER}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{TOWER_CONTROLLER}")}})
    # The reactor building's controller (under its loop floor): the same.
    write(assets / "blockstates" / f"{REACTOR_CONTROLLER}.json", {"variants": {
        f"facing={f}": dict({"model": rid(f"block/{TOWER_CONTROLLER}")}, **({"y": turn[f]} if turn[f] else {})) for f in turn}})
    write(assets / "items" / f"{REACTOR_CONTROLLER}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{TOWER_CONTROLLER}")}})
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
    import tv_speaker_data
    write(out / f"{tv_speaker_data.ID}.json", tv_speaker_data.loot(rid))   # the surround speaker: its lower half drops it
    for block in BLOCKS:
        if block in plant_interior.NO_DROP:
            write(out / f"{block}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{block}")})
        else:
            door = block in plant_interior.DOORS or block in plant_interior.restroom.DOORS
            if block in plant_interior.DOORS:
                # The steel and glass doors keep their hardware (exit, reader) on the item (doors round).
                write(out / f"{block}.json", plant_interior.plant_doors.door_loot(rid, block))
            else:
                table = plant_interior.restroom.toilet_loot(rid) if block == "restroom_toilet" else None
                write(out / f"{block}.json", table or (plant_interior.door_loot(rid, block) if door else self_drop(block)))
    # Breaking a frame block breaks the equipment, and its anchor drops the item.
    write(out / f"{FRAME}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{FRAME}")})
    storage_tank.write_loot(write, rid, out, self_drop)
    nuclear_storage.write_loot(write, rid, out, self_drop)


def write_data(write, data):
    write(data / MOD / "damage_type" / "radiation.json", {"message_id": "jugcraft.radiation", "exhaustion": 0.0, "scaling": "never"})
    # The diesel rooms' CO2 discharge (round 31 part 3, nuclear/Co2System.java): choking, armour no help.
    write(data / MOD / "damage_type" / "co2.json", {"message_id": "jugcraft.co2", "exhaustion": 0.0, "scaling": "never"})


def write_tags(tags, rid):
    for block in BLOCKS:
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
        tags.add("block", "minecraft:needs_stone_tool", rid(block))
    storage_tank.write_tags(tags, rid)
    tags.add("block", f"{MOD}:{SHIELDING_TAG}", rid("shielding_concrete"))
    tags.add("block", "minecraft:climbable", rid("site_ladder"))
    tags.add("block", f"{MOD}:{SHIELDING_TAG}", "#c:storage_blocks/lead")
    # (The hazmat suit's armor tags are written with the rest of the armor's, in tools/gear.py: HAZMAT_TAGS.)


# The hazmat suit is armor of its slots (enchants, equips): tools/gear.py adds these to its armor tags, because the
# shared tag writer replaces a whole file.
HAZMAT_TAGS = {"head_armor": "hazmat_hood", "chest_armor": "hazmat_coverall", "leg_armor": "hazmat_trousers",
               "foot_armor": "hazmat_boots"}


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
