"""Industrial machine forms the game registers (docs/features/industrial-machine-foundation.md): the starter factory's
Electrolytic Separator (docs/features/industrial-electrolytic-separator.md), Gas Burning Generator
(docs/features/industrial-gas-burning-generator.md), Chemical Infuser (docs/features/industrial-chemical-infuser.md) and
Chemical Oxidizer (docs/features/industrial-chemical-oxidizer.md), Industrial Fermenter
(docs/features/industrial-fermenter.md), Bulk Chemical Reactor (docs/features/industrial-bulk-chemical-reactor.md),
Anaerobic Digester (docs/features/industrial-anaerobic-digester.md), Coal Gasifier and Gas Cleanup and Separation Rack
(docs/features/industrial-coal-gasification.md) and Bio Generator (docs/features/industrial-bio-generator.md), of the
owner's factory plan.

Each form is described here for its art, recipes and data, and in IndustrialForms.java for the game;
tools/check_mod_data.py checks that the two agree. Layers are given bottom first, each as rows from the front to the
back, each row's characters from the viewer's left to right, standing in front of the machine: C the controller,
# another structural block, ~ clearance for a moving part and . open access space.

Models are authored in structure space like tools/large_machines.py: pixels, facing north with the front at z = 0,
the controller's block at 0..16 on every axis and the viewer's right towards -x. model_writer.split_model slices
them into one model per part, in the form's part order: the controller first, then every other structural block
from the bottom layer up, the front row first, left to right (MachineForm in Java).
"""
import math

import model_writer
# steampunk_models first: it imports dieselpunk_models' finished models at its end.
from steampunk_models import GLOW, box, cyl, dial, q, span
from dieselpunk_models import CHROME, EXHAUST, GAUGE, GRILLE, GUNMETAL, HAZARD, LAMP, OLIVE, RUBBER, STENCIL

MOD = "jugcraft"

# Splitting water and ordinary brine: what the Separator offers its family's recipes.
AQUEOUS_ELECTROLYSIS = f"{MOD}:aqueous_electrolysis"
# Joining two gases with no catalyst bed (hydrogen burnt in chlorine): what the Infuser offers its family's recipes.
GAS_COMBINATION = f"{MOD}:gas_combination"
# Burning a solid feed in a metered oxidant gas (sulfur in oxygen): what the Oxidizer offers its family's recipes.
SOLID_OXIDATION = f"{MOD}:solid_oxidation"
# Fermenting crops in a closed, stirred, heated vessel: what the Fermenter offers its family's recipes.
CONTROLLED_FERMENTATION = f"{MOD}:controlled_fermentation"
# Dissolving a gas in a liquid (hydrogen chloride in water) and leaching ores in the acid that makes: what the Bulk
# Chemical Reactor offers its family's recipes.
ACID_ABSORPTION = f"{MOD}:acid_absorption"
CHLORIDE_LEACHING = f"{MOD}:chloride_leaching"
# Digesting organic waste in a sealed, stirred tank into raw biogas: what the Anaerobic Digester offers, and the item tag
# of what it takes.
ANAEROBIC_DIGESTION = f"{MOD}:anaerobic_digestion"
DIGESTIBLE = "digestible"
# Gasifying a carbon feed with steam into a dirty carbon monoxide and hydrogen mixture (the Coal Gasifier), and cleaning
# and dividing that mixture into its gases (the Gas Cleanup and Separation Rack).
COAL_GASIFICATION = f"{MOD}:coal_gasification"
GAS_SEPARATION = f"{MOD}:gas_separation"
# Joining a carbon oxide and hydrogen into methane over the nickel bed, and the item tag the Infuser's bed socket holds.
NICKEL_METHANATION = f"{MOD}:nickel_methanation"
# What the Infuser's bed socket holds: the nickel bed and the vanadium contact bed.
CATALYST_BEDS = "catalyst_beds"
# Oxidizing sulfur dioxide over the vanadium contact bed (the Infuser), and roasting the bed's vanadium oxide out of
# vanadium-bearing concentrate with lye (the Bulk Chemical Reactor; washed iron is a legacy fallback).
CONTACT_CONVERSION = f"{MOD}:contact_conversion"
VANADIUM_RECOVERY = f"{MOD}:vanadium_recovery"
# Neutralizing nitric acid with ammonia into solid ammonium nitrate (the Bulk Chemical Reactor).
NITRATE_NEUTRALIZATION = f"{MOD}:nitrate_neutralization"

# The owner's white tank family (the tank restyle) for the collection towers, with its checker band.
TANK_BODY, TANK_TOP, TANK_CHECKER = "tk_body", "tk_top", "tk_checker"
# Match the silver-grey process-machine fittings, without glossy insulator stripes.
INSULATOR, BUSBAR, COPPER = "dw_steel_plate", "copper_busbar", "sp_copper"
# Amber level strips that light while it works (steampunk_models.GLOW).
STRIP = "dr_amber"
# The Gas Burning Generator's materials (the art brief): graphite ribs, olive combustion covers, a pale heat shield,
# a ribbed generator housing, and mint lamps at the electrical output that light while it runs.
SHIELD, RIBBED, MINT = "el_ribbed", "dw_ribbed_steel", "el_lamp"
# The Chemical Infuser's materials (the art brief): a pale ceramic collar where the chamber steps in, grey bolted
# flanges where pipes meet it, a ringed inspection port and a red-painted handwheel on each inlet valve.
CERAMIC, FLANGE, PORTHOLE, HANDWHEEL = "sp_ceramic", "dr_flange", "dw_porthole_steel", "dr_red_paint"
# The Chemical Oxidizer's materials (the art brief): a cream refractory lining round its hatch, warm copper fittings,
# olive hopper guards and the hopper's dark mouth.
REFRACTORY, MOUTH = "sp_ceramic", "sp_hopper_inside"
# What lights while a form works: steampunk_models.GLOW and the mint lamp.
FORM_GLOW = dict(GLOW, el_lamp="el_lamp_on")
# What the generator burns (tools/petro.py FLUID_FUELS keeps the Fuel Cell's value): (fluid, JE per mB, JE a tick).
# Hydrogen at the Fuel Cell's 128 JE/mB and the owner's selected 128 JE/t; methane at the plan's 448 JE/mB and the
# owner's selected 256 JE/t.
GAS_GENERATOR_FUELS = [(f"{MOD}:hydrogen", 128, 128), (f"{MOD}:methane", 448, 256)]
# What the Bio Generator burns: bioethanol at the gas turbine's 192 JE/mB and the plan's proposed 96 JE/t, below the gas
# generator's output; and the Anaerobic Digester's biogas at the plan's 64 JE/t, its 60% methane share worth 268 JE/mB
# (60% of methane's 448, rounded down: the carbon dioxide burns for nothing).
BIO_GENERATOR_FUELS = [(f"{MOD}:bioethanol", 192, 96), (f"{MOD}:biogas", 268, 64)]
# The Bio Generator's materials (the art brief): pale ceramic heat shields over the combustion body.
HEAT_SHIELD, FLANGE_FACE = "sp_ceramic", "dr_flange"

FORMS = {
    "electrolytic_separator": {
        "display": "Electrolytic Separator",
        "family": "electrolytic_cell",
        "layers": [["C#", "##"], ["##", "##"], ["##", "##"]],
        # OperatingProfile.ENTRY: one batch at a time in 2,000 mB tanks, at the family's full draw.
        "profile": "ENTRY",
        "tank_mb": 2_000,
        "input_tanks": ["feed"],
        # Oxygen from water or chlorine from brine share one destination; the lye tank stays empty for water.
        "output_tanks": ["anode_gas", "hydrogen", "lye"],
        "capabilities": [AQUEOUS_ELECTROLYSIS],
        # Two upgrade slots (speed and efficiency cards), and a warm-up label for the first two seconds after a cold
        # start, inside the paid work (the owner's choice: heating included in the processing power).
        "upgrades": True,
        "warmup": 40,
        # (name, kind, target, column, row, layer, side), as FormPort in Java.
        "ports": [
            ("feed_in", "FLUID_IN", 0, 0, 1, 0, "BACK"),
            ("anode_gas_out", "FLUID_OUT", 0, 1, 0, 2, "FRONT"),
            ("hydrogen_out", "FLUID_OUT", 1, 0, 0, 2, "FRONT"),
            ("lye_out", "FLUID_OUT", 2, 1, 0, 1, "FRONT"),
            ("power", "ENERGY_IN", 0, 1, 0, 0, "FRONT"),
        ],
        # The owner's selected construction: 4 steel plates and two Steel Tanks (8 plates each), 20 plates in all,
        # with copper cable and a basic circuit where the Electrolytic Cell needs aluminum cable and an advanced one.
        "recipe": (["PWP", "TCT", "PMP"], {"P": "#c:plates/steel", "W": f"{MOD}:copper_cable", "T": f"{MOD}:steel_tank",
                                           "C": f"{MOD}:basic_circuit", "M": f"{MOD}:machine_casing"}),
        "features": ["machines"],
    },
    "gas_burning_generator": {
        "display": "Gas Burning Generator",
        # The combustion generators' family (the Gas Turbine's energy figures and screen); the form's own fuel list,
        # not the family's, says what it burns, so the turbine never takes hydrogen.
        "family": "gas_turbine",
        # Every block the model reaches: the skid fills the ground; the engine-generator train the middle columns;
        # the housing and the cabinet the back rows; the cabinet's top two blocks of the fourth layer.
        "layers": [["C###", "####", "####", "####", "####"],
                   [".##.", ".##.", ".##.", "####", "####"],
                   [".##.", ".##.", ".##.", "####", "####"],
                   ["....", "....", "....", "....", "..##"]],
        "profile": "ENTRY",
        "tank_mb": 2_000,
        "input_tanks": ["fuel"],
        "output_tanks": [],
        "capabilities": [],
        "fuels": GAS_GENERATOR_FUELS,
        "upgrades": False,
        "warmup": 0,
        "ports": [
            ("fuel_in", "FLUID_IN", 0, 3, 1, 0, "RIGHT"),
            ("power_out", "ENERGY_OUT", 0, 2, 4, 1, "BACK"),
        ],
        # The plan's starter bill (docs/features/industrial-starter-gas-and-acid-factory.md): 4 steel plates, an
        # electric motor, a steel gear, a Tinplate Tank, a basic circuit and a Machine Casing.
        "recipe": (["PGP", "MCT", "PKP"], {"P": "#c:plates/steel", "G": f"{MOD}:steel_gear", "M": f"{MOD}:electric_motor",
                                           "C": f"{MOD}:basic_circuit", "T": f"{MOD}:fluid_tank",
                                           "K": f"{MOD}:machine_casing"}),
        "features": ["machines"],
    },
    "chemical_infuser": {
        "display": "Chemical Infuser",
        # The synthesis family (the Synthesis Converter's energy figures: 128 JE/t, a 60,000 JE store); it runs only the
        # recipes that ask for its capability, and the converter never runs those.
        "family": "synthesis_converter",
        "layers": [["C#", "##", "##"], ["##", "##", "##"], ["##", "##", "##"]],
        "profile": "ENTRY",
        "tank_mb": 2_000,
        # Hydrogen always comes in on the left; the gas it joins (chlorine now, carbon oxides with the nickel bed later)
        # on the right. The product gas and the water a reaction gives off leave by the cooling jacket at the back.
        "input_tanks": ["hydrogen", "reactant"],
        "output_tanks": ["product", "water"],
        # The bed socket holds the nickel catalyst bed that methanation needs; hydrogen chloride needs none.
        "sockets": [("bed", CATALYST_BEDS)],
        "capabilities": [GAS_COMBINATION, NICKEL_METHANATION, CONTACT_CONVERSION],
        # Speed and efficiency cards, and the warm-up label for the first two seconds after a cold start, inside the
        # paid work (the owner's choice: heating included in the processing power).
        "upgrades": True,
        "warmup": 40,
        "ports": [
            ("hydrogen_in", "FLUID_IN", 0, 0, 1, 1, "LEFT"),
            ("reactant_in", "FLUID_IN", 1, 1, 1, 1, "RIGHT"),
            ("product_out", "FLUID_OUT", 0, 1, 2, 1, "BACK"),
            ("water_out", "FLUID_OUT", 1, 0, 2, 0, "BACK"),
            ("power", "ENERGY_IN", 0, 1, 2, 0, "RIGHT"),
        ],
        # The plan's starter bill (docs/features/industrial-starter-gas-and-acid-factory.md): 4 steel plates, 2 steel
        # fluid pipes, a Tinplate Tank, copper cable and a basic circuit, with no titanium or advanced circuit, unlike the
        # Synthesis Converter.
        "recipe": (["PIP", "WTC", "PIP"], {"P": "#c:plates/steel", "I": f"{MOD}:steel_fluid_pipe", "W": f"{MOD}:copper_cable",
                                           "T": f"{MOD}:fluid_tank", "C": f"{MOD}:basic_circuit"}),
        "features": ["machines"],
    },
    "chemical_oxidizer": {
        "display": "Chemical Oxidizer",
        # The chemical reactor family (the Chemical Reactor's energy figures: 96 JE/t, a 30,000 JE store); it runs only
        # the recipes that ask for its capability, and the reactor never runs those.
        "family": "chemical_reactor",
        "layers": [["C#", "##"], ["##", "##"], ["##", "##"]],
        "profile": "ENTRY",
        "tank_mb": 2_000,
        # One solid feed slot (the hopper), the oxidant gas and the gas it makes.
        "input_tanks": ["oxidant"],
        "output_tanks": ["product"],
        "item_inputs": 1,
        "capabilities": [SOLID_OXIDATION],
        "upgrades": True,
        "warmup": 40,
        "ports": [
            ("feed_in", "ITEM_IN", 0, 0, 1, 2, "TOP"),
            ("oxidant_in", "FLUID_IN", 0, 1, 0, 0, "RIGHT"),
            ("product_out", "FLUID_OUT", 0, 1, 0, 2, "TOP"),
            ("power", "ENERGY_IN", 0, 0, 1, 0, "LEFT"),
        ],
        # The plan's starter bill (docs/features/industrial-starter-gas-and-acid-factory.md): 4 steel plates, a furnace, an
        # electric motor, glass, a basic circuit and a Machine Casing.
        "recipe": (["PFP", "MCG", "PKP"], {"P": "#c:plates/steel", "F": "minecraft:furnace", "M": f"{MOD}:electric_motor",
                                           "C": f"{MOD}:basic_circuit", "G": "minecraft:glass",
                                           "K": f"{MOD}:machine_casing"}),
        "features": ["machines"],
    },
    "industrial_fermenter": {
        "display": "Industrial Fermenter",
        # The chemical reactor family (the Chemical Reactor's energy figures, 96 JE/t), like the reactor's own fermentation.
        "family": "chemical_reactor",
        "layers": [["C##", "###", "###"], ["###", "###", "###"], ["###", "###", "###"], ["###", "###", "###"]],
        "profile": "ENTRY",
        "tank_mb": 2_000,
        # Crops in the hatch slot and water; the alcohol and the carbon dioxide, which vents what it cannot hold
        # (MachineForm.vents: the owner's selected excess-vent default, so fermentation never waits on the gas).
        "input_tanks": ["water"],
        "output_tanks": ["product", "co2"],
        "vents": ["co2"],
        "item_inputs": 1,
        "capabilities": [CONTROLLED_FERMENTATION],
        "upgrades": True,
        "warmup": 40,
        "ports": [
            ("feed_in", "ITEM_IN", 0, 0, 1, 3, "TOP"),
            ("water_in", "FLUID_IN", 0, 0, 2, 1, "BACK"),
            ("product_out", "FLUID_OUT", 0, 1, 0, 0, "FRONT"),
            ("co2_out", "FLUID_OUT", 1, 1, 2, 3, "BACK"),
            ("power", "ENERGY_IN", 0, 2, 2, 0, "BACK"),
        ],
        # The plan's bill is 8 steel plates, a Steel Tank, a motor, 2 glass, a basic circuit and a casing: 14 items, more
        # than a crafting grid holds. This takes 4 plates, the Steel Tank, glass, the motor, the circuit and the casing,
        # 12 plates in all (open for review).
        "recipe": (["PGP", "PTP", "MCK"], {"P": "#c:plates/steel", "G": "minecraft:glass", "T": f"{MOD}:steel_tank",
                                           "M": f"{MOD}:electric_motor", "C": f"{MOD}:basic_circuit",
                                           "K": f"{MOD}:machine_casing"}),
        "features": ["machines"],
    },
    "bulk_chemical_reactor": {
        "display": "Bulk Chemical Reactor",
        # The chemical reactor family (96 JE/t), in the expanded profile: two batches side by side, 8,000 mB tanks and
        # 90% of the draw a batch.
        "family": "chemical_reactor",
        "layers": [["C###", "####", "####", "####"]] + [["####", "####", "####", "####"]] * 3,
        "profile": "EXPANDED",
        "tank_mb": 8_000,
        # The liquid (water or acid) at the side, the gas at the short absorption inlet in the crown; ores in the hatch
        # and washed ores out at the front.
        "input_tanks": ["liquid", "gas"],
        "output_tanks": ["product"],
        "item_inputs": 1,
        "item_outputs": 1,
        "capabilities": [ACID_ABSORPTION, CHLORIDE_LEACHING, VANADIUM_RECOVERY, NITRATE_NEUTRALIZATION],
        "upgrades": True,
        "warmup": 40,
        "ports": [
            ("feed_in", "ITEM_IN", 0, 1, 1, 3, "TOP"),
            ("liquid_in", "FLUID_IN", 0, 0, 2, 0, "LEFT"),
            ("gas_in", "FLUID_IN", 1, 2, 2, 3, "TOP"),
            ("product_out", "FLUID_OUT", 0, 2, 0, 0, "FRONT"),
            ("items_out", "ITEM_OUT", "FormPort.ALL", 1, 0, 0, "FRONT"),
            ("power", "ENERGY_IN", 0, 3, 3, 0, "RIGHT"),
        ],
        # The enlarged Chemical Reactor (no bill in the plan; this change's choice, open for review): the reactor itself,
        # a Steel Tank, a steel fluid pipe, an electric motor, a basic circuit and 4 steel plates.
        "recipe": (["PIP", "MRC", "PTP"], {"P": "#c:plates/steel", "I": f"{MOD}:steel_fluid_pipe",
                                           "M": f"{MOD}:electric_motor", "R": f"{MOD}:chemical_reactor",
                                           "C": f"{MOD}:basic_circuit", "T": f"{MOD}:steel_tank"}),
        "features": ["machines"],
    },
    "anaerobic_digester": {
        "display": "Anaerobic Digester",
        # The chemical reactor family (96 JE/t) in the bulk profile: four batches side by side, 16,000 mB tanks and 80%
        # of the draw a batch. The owner's selected substantial first digester, six blocks every way.
        "family": "chemical_reactor",
        "layers": [["C#####"] + ["######"] * 5] + [["######"] * 6] * 5,
        "profile": "BULK",
        "tank_mb": 16_000,
        # Waste at the feed auger and water; raw biogas at the top header, digestate (bone meal) at the low chute.
        "input_tanks": ["water"],
        "output_tanks": ["biogas"],
        "item_inputs": 1,
        "item_outputs": 1,
        "capabilities": [ANAEROBIC_DIGESTION],
        "upgrades": True,
        "warmup": 40,
        "ports": [
            ("feed_in", "ITEM_IN", 0, 0, 2, 1, "LEFT"),
            ("water_in", "FLUID_IN", 0, 0, 4, 0, "LEFT"),
            ("biogas_out", "FLUID_OUT", 0, 5, 4, 5, "RIGHT"),
            ("digestate_out", "ITEM_OUT", "FormPort.ALL", 5, 2, 0, "RIGHT"),
            ("power", "ENERGY_IN", 0, 3, 5, 0, "BACK"),
        ],
        # No bill in the plan (this change's choice, open for review): four Steel Tanks, two electric motors, a steel
        # plate, a basic circuit and a Machine Casing.
        "recipe": (["TPT", "MCM", "TKT"], {"T": f"{MOD}:steel_tank", "P": "#c:plates/steel", "M": f"{MOD}:electric_motor",
                                           "C": f"{MOD}:basic_circuit", "K": f"{MOD}:machine_casing"}),
        "features": ["machines"],
    },
    "coal_gasifier": {
        "display": "Coal Gasifier",
        # The chemical reactor family (96 JE/t: the plan's paid automatic electrical heat), in the expanded profile: two
        # batches side by side, 8,000 mB tanks and 90% of the draw a batch.
        "family": "chemical_reactor",
        "layers": [["C###", "####", "####", "####"]] + [["####", "####", "####", "####"]] * 5,
        "profile": "EXPANDED",
        "tank_mb": 8_000,
        # The carbon feed at the side charging chute, water low on the left; the raw gas at the upper header and the ash
        # chamber's residue at the chute low on the right.
        "input_tanks": ["water"],
        "output_tanks": ["product"],
        "item_inputs": 1,
        # The ash chamber's residue leaves at the chute low on the right.
        "item_outputs": 1,
        "capabilities": [COAL_GASIFICATION],
        "upgrades": True,
        "warmup": 40,
        "ports": [
            ("feed_in", "ITEM_IN", 0, 0, 1, 2, "LEFT"),
            ("water_in", "FLUID_IN", 0, 0, 3, 0, "LEFT"),
            ("syngas_out", "FLUID_OUT", 0, 3, 2, 5, "RIGHT"),
            ("residue_out", "ITEM_OUT", "FormPort.ALL", 3, 1, 0, "RIGHT"),
            ("power", "ENERGY_IN", 0, 2, 3, 0, "BACK"),
        ],
        # The plan's bill (docs/features/industrial-starter-gas-and-acid-factory.md): 4 steel plates, 2 bricks, a furnace,
        # a steel fluid pipe and a Machine Casing.
        "recipe": (["PBP", "FKI", "PBP"], {"P": "#c:plates/steel", "B": "minecraft:brick", "F": "minecraft:furnace",
                                           "K": f"{MOD}:machine_casing", "I": f"{MOD}:steel_fluid_pipe"}),
        "features": ["machines"],
    },
    "gas_cleanup_rack": {
        "display": "Gas Cleanup and Separation Rack",
        # The synthesis family (128 JE/t), like the Infuser.
        "family": "synthesis_converter",
        "layers": [["C##", "###", "###", "###", "###"]] + [["###", "###", "###", "###", "###"]] * 3,
        "profile": "ENTRY",
        "tank_mb": 2_000,
        # The raw gas in low on the left; carbon monoxide and hydrogen out at the tops of their towers at the back.
        "input_tanks": ["feed"],
        "output_tanks": ["product", "hydrogen"],
        "capabilities": [GAS_SEPARATION],
        "upgrades": True,
        "warmup": 40,
        "ports": [
            ("feed_in", "FLUID_IN", 0, 0, 2, 0, "LEFT"),
            ("product_out", "FLUID_OUT", 0, 2, 4, 3, "TOP"),
            ("hydrogen_out", "FLUID_OUT", 1, 0, 4, 3, "TOP"),
            ("power", "ENERGY_IN", 0, 1, 4, 0, "BACK"),
        ],
        # The plan's bill: 4 steel plates, a Steel Tank, 2 Tinplate Tanks, an electric motor and a basic circuit.
        "recipe": (["PTP", "SMS", "PCP"], {"P": "#c:plates/steel", "T": f"{MOD}:steel_tank", "S": f"{MOD}:fluid_tank",
                                           "M": f"{MOD}:electric_motor", "C": f"{MOD}:basic_circuit"}),
        "features": ["machines"],
    },
    "bio_generator": {
        "display": "Bio Generator",
        # The diesel generator's family (its energy figures: a 60,000 JE store, 1,024 JE/t out); the form's fuel list says
        # what it burns, so the Diesel Generator itself is unchanged.
        "family": "diesel_generator",
        # The skid and the train fill the two lower layers; above them the exhaust along the middle and the radiator.
        "layers": [["C##", "###", "###", "###"], ["###", "###", "###", "###"], [".#.", ".#.", ".#.", "###"]],
        "profile": "ENTRY",
        "tank_mb": 2_000,
        "input_tanks": ["fuel"],
        "output_tanks": [],
        "capabilities": [],
        "fuels": BIO_GENERATOR_FUELS,
        "upgrades": False,
        "warmup": 0,
        "ports": [
            ("fuel_in", "FLUID_IN", 0, 2, 2, 0, "RIGHT"),
            ("power_out", "ENERGY_OUT", 0, 0, 2, 0, "LEFT"),
        ],
        # The plan's bill (docs/features/industrial-machine-construction-and-operation.md): 8 steel plates, a motor, a steel
        # gear, a Tinplate Tank, a basic circuit and a casing. Eight plates round a Tinplate Tank are a Steel Tank, so the
        # grid takes one: the same materials in one crafting.
        "recipe": (["MGC", " T ", " K "], {"M": f"{MOD}:electric_motor", "G": f"{MOD}:steel_gear", "C": f"{MOD}:basic_circuit",
                                           "T": f"{MOD}:steel_tank", "K": f"{MOD}:machine_casing"}),
        "features": ["machines"],
    },
}

# Each form's Engineer's Handbook page (tools/handbook.py form_page), after its family's machine.
HANDBOOK = {
    "electrolytic_separator": [
        "Two wide, two deep and three tall: the first of the big industrial machines, built from steel, copper cable "
        "and a basic circuit, before the Electrolytic Cell's aluminum cable and advanced circuit. It is placed from "
        "the front; every block it fills must be free, or it says what is in the way and where.",
        "Pipe water or brine into the inlet low at the back. A bucket of water gives 500 mB of hydrogen (the top "
        "left collar) and 250 mB of oxygen (top right) in 40 seconds; a bucket of brine gives 250 mB of hydrogen, "
        "250 mB of chlorine (top right) and 500 mB of lye (the return on the right) in 20 seconds. It draws 256 JE/t "
        "through the socket at the foot of the right tower.",
        "It keeps its lye: when the lye tank is full, brine stops until the lye is piped away. Its screen says what "
        "it is waiting for, and upgrade cards fit its terminal.",
    ],
    "gas_burning_generator": [
        "Four wide, five deep and four tall: a gas engine turning a generator on a steel skid, built from steel "
        "plates, an electric motor, a steel gear, a Tinplate Tank and a basic circuit. Every block it fills must be "
        "free when it is placed; the corners above the skid at the front stay open.",
        "Pipe hydrogen into the capped inlet low on its right side. It burns a millibucket a tick, 128 JE each, and "
        "sends 128 JE/t out of the socket on the terminal cabinet at the back: a bucket lasts 50 seconds. Its coupling "
        "and intake fan spin up as it starts.",
        "It makes only as much as its store has room for, so no fuel is wasted when nothing draws its power, and its "
        "screen says when the fuel tank is empty or the store is full. It also burns methane from the Chemical Infuser: "
        "448 JE a millibucket at 256 JE/t, a bucket lasting 87.5 seconds.",
    ],
    "chemical_infuser": [
        "Two wide, three deep and three tall: a stepped reaction chamber with an inlet on each shoulder, a catalyst-bed "
        "drawer at its foot and a cooling jacket behind, built from steel plates, two steel fluid pipes, a Tinplate "
        "Tank, copper cable and a basic circuit. Every block it fills must be free when it is placed.",
        "Pipe hydrogen into the left inlet and chlorine into the right. 250 mB of each burn together into 500 mB of "
        "hydrogen chloride in under four seconds, which leaves at the top collar at the back. It draws 128 JE/t "
        "through the socket low on the right side at the back.",
        "With a Nickel Catalyst Bed in its bed socket it makes methane: 400 mB of hydrogen and 100 mB of carbon "
        "dioxide (the right inlet) give 100 mB of methane at the top collar and 400 mB of water at the drain below it "
        "in ten seconds. The bed is never used up. Its screen says what it is waiting for, and upgrade cards fit its "
        "terminal.",
    ],
    "industrial_fermenter": [
        "Three wide, three deep and four tall: a tall stirred vessel on four legs, built from steel plates, a Steel Tank, "
        "glass, an electric motor, a basic circuit and a Machine Casing. Every block it fills must be free when it is "
        "placed.",
        "Drop eight crops (wheat, sugar cane, potatoes, carrots, beetroot, berries, melon or apples) into the hatch on its "
        "crown and pipe water into the inlet low at the back left. With a bucket of water they ferment into 250 mB of "
        "bioethanol at the broth outlet low at the front, and 250 mB of carbon dioxide at the flange high at the back, in "
        "five seconds at 96 JE/t.",
        "The carbon dioxide feeds the Chemical Infuser's methane. Collecting it is optional: when its tank is full the "
        "rest is vented and fermentation goes on.",
    ],
    "chemical_oxidizer": [
        "Two wide, two deep and three tall: a lined reaction throat fed by a hopper and a feed screw at the back, with a "
        "gas column and collector hood on top, built from steel plates, a furnace, an electric motor, glass, a basic "
        "circuit and a Machine Casing. Every block it fills must be free when it is placed.",
        "Drop sulfur dust into the hopper on top at the back and pipe oxygen into the capped inlet low on the right. "
        "Each dust burns with 250 mB of oxygen into 250 mB of sulfur dioxide in five seconds, which leaves at the "
        "collar on top of the hood. It draws 96 JE/t through the socket low on the left side at the back.",
        "Coal or charcoal burns the same way into 250 mB of carbon dioxide, the Chemical Infuser's feed for methane. "
        "Its screen says what it is waiting for, and upgrade cards fit its terminal.",
    ],
}

HANDBOOK["bulk_chemical_reactor"] = [
    "Four wide, four deep and four tall: the Chemical Reactor enlarged into a jacketed vessel with a stirring drive on "
    "its lid, built from a Chemical Reactor, a Steel Tank, a steel fluid pipe, an electric motor, a basic circuit and "
    "steel plates. Every block it fills must be free when it is placed. It runs two batches at once, from 8,000 mB "
    "tanks, at 90% of the reactor's draw.",
    "Pipe water into the inlet on its left side and hydrogen chloride from the Chemical Infuser into the short "
    "absorption inlet on its lid: 500 mB of the gas dissolves in a bucket of water into a bucket of hydrochloric acid "
    "in five seconds, which leaves at the outlet low at the front.",
    "Hydrochloric acid leaches ore like sulfuric acid: drop an ore into the hatch on its lid, pipe 250 mB of the acid "
    "into the side inlet, and four washed ores come out of the chute at the front, ready for the furnace.",
    "Nitric acid in the side inlet and ammonia in the lid inlet neutralize each other: 250 mB of the acid and 125 mB of "
    "ammonia give four ammonium nitrate at the chute. It is a strong nitrogen fertilizer, and it packs grenades and "
    "high-explosive rockets in place of guncotton.",
]

HANDBOOK["anaerobic_digester"] = [
    "Six wide, six deep and six tall: a huge sealed tank on four foundation shoes, with a feed auger on its left side, a "
    "digestate chute low on its right, a gas header over its dome and a mixer gearbox at the back, built from four "
    "Steel Tanks, two electric motors, a steel plate, a basic circuit and a Machine Casing. Every block it fills must be "
    "free when it is placed. It runs four batches at once, from 16,000 mB tanks, at 80% of the reactor's draw.",
    "Feed it organic waste at the auger: crops, rotten flesh, poisonous potatoes, kelp, pumpkins or seeds. Pipe water "
    "into the inlet low on the left. Sixteen items and a bucket of water digest over twenty seconds into 1,000 mB of raw "
    "biogas (60% methane, 40% carbon dioxide) at the header flange high on the right, and two bone meal at the chute.",
    "Biogas upgrades into pure methane in the Chemical Infuser with its Nickel Catalyst Bed: 250 mB of biogas and 400 "
    "mB of hydrogen give 250 mB of methane and 400 mB of water.",
]

HANDBOOK["coal_gasifier"] = [
    "Four wide, four deep and six tall: a tall lined reaction column over a broad ash chamber, with a charging chute on "
    "its left side and a gas header high on its right, built from steel plates, bricks, a furnace, a steel fluid pipe "
    "and a Machine Casing. Every block it fills must be free when it is placed. It runs two batches at once.",
    "Drop coal or coal coke into the charging chute and pipe water into the inlet low on its left side. Each piece and "
    "500 mB of water (as steam) become 500 mB of raw syngas (half carbon monoxide, half hydrogen) in ten seconds, which leaves at "
    "the header flange high on its right. Charcoal gives 400 mB from 400 mB of water.",
    "Coal and coke each leave a Gasification Residue at the chute low on its right side. Bound with cement it makes "
    "concrete, and with asphalt binder road asphalt, in place of gravel.",
    "Raw syngas is dirty: the Gas Cleanup and Separation Rack divides it into carbon monoxide and hydrogen.",
]
HANDBOOK["gas_cleanup_rack"] = [
    "Three wide, five deep and four tall: a scrubber column and two separation towers on a steel skid, built from "
    "steel plates, a Steel Tank, two Tinplate Tanks, an electric motor and a basic circuit. Every block it fills must "
    "be free when it is placed.",
    "Pipe raw syngas into the inlet low on its left side. 500 mB of it is cleaned and divided into 250 mB of carbon "
    "monoxide (the tower at the back on the right) and 250 mB of hydrogen (the tower at the back on the left) in five "
    "seconds at 128 JE/t.",
    "Carbon monoxide becomes methane in the Chemical Infuser with its Nickel Catalyst Bed: 100 mB of it and 300 mB of "
    "hydrogen give 100 mB of methane and 200 mB of water.",
]

HANDBOOK["bio_generator"] = [
        "Three wide, four deep and three tall: a compact engine-generator on a thick skid, built from a Steel Tank, an "
        "electric motor, a steel gear, a basic circuit and a Machine Casing. Every block it fills must be free when it is "
        "placed.",
        "Pipe bioethanol into the low liquid inlet on its right side. It burns 192 JE a millibucket at 96 JE/t, a bucket "
        "lasting 100 seconds, and sends the power out of the socket on the terminal box low on its left side. Its "
        "coupling and radiator fan turn while it runs.",
        "It makes only as much as its store has room for. It also burns biogas from the Anaerobic Digester, piped into "
        "the same inlet: 268 JE a millibucket at 64 JE/t, a bucket lasting about 210 seconds.",
    ]

# Tank labels on the screen and in its reasons (MachineStatus.tankLabel), by role.
TANK_NAMES = {
    "fuel": "Fuel tank",
    "feed": "Feed tank",
    "anode_gas": "Oxygen/chlorine tank",
    "hydrogen": "Hydrogen tank",
    "lye": "Lye tank",
    "reactant": "Reactant gas tank",
    "product": "Product gas tank",
    "water": "Water tank",
    "oxidant": "Oxidant tank",
    "co2": "Carbon dioxide tank",
    "liquid": "Liquid tank",
    "gas": "Gas tank",
    "biogas": "Biogas tank",
}

# Recipes only forms run: each names the capability a form must offer. Fluid results go to output tanks by their
# third value. The quantities and times are the owner's selected starter baseline
# (docs/features/industrial-starter-gas-and-acid-factory.md, eighth batch, question 9).
FORM_RECIPES = {
    "electrolytic_separator": [
        # Ordinary brine: chlorine at the anode, hydrogen at the cathode, lye left behind. 400 ticks at 256 JE/t,
        # 102,400 JE, the same as the Electrolytic Cell's own brine recipe.
        {"name": "separator_brine", "capability": AQUEOUS_ELECTROLYSIS, "fluids": [(f"{MOD}:brine", 1000)],
         "fluid_results": [(f"{MOD}:chlorine", 250, 0), (f"{MOD}:hydrogen", 250, 1), (f"{MOD}:lye", 500, 2)],
         "ticks": 400, "features": ["salt"]},
        # Water: two hydrogen to one oxygen, as in the cell. 800 ticks at 256 JE/t, 204,800 JE.
        {"name": "separator_water", "capability": AQUEOUS_ELECTROLYSIS, "fluids": [("minecraft:water", 1000)],
         "fluid_results": [(f"{MOD}:oxygen", 250, 0), (f"{MOD}:hydrogen", 500, 1)],
         "ticks": 800, "features": ["machines"]},
    ],
    "chemical_infuser": [
        # Hydrogen chloride: hydrogen burnt in chlorine, equal gas amounts making twice as much
        # (docs/features/industrial-starter-gas-and-acid-factory.md). The plan's budget, 9,600 JE a batch, at the
        # synthesis family's 128 JE/t: 75 ticks.
        # Direct chlorination (the plan's detailed PVC route): refinery ethylene and chlorine join into ethylene dichloride,
        # the liquid at 2 mB a reaction amount. 60 ticks at 128 JE/t.
        {"name": "infuser_ethylene_dichloride", "capability": GAS_COMBINATION,
         "fluids": [(f"{MOD}:refinery_gas", 250), (f"{MOD}:chlorine", 250)],
         "fluid_results": [(f"{MOD}:ethylene_dichloride", 500, 0)], "ticks": 60, "features": ["salt", "machines"]},
        {"name": "infuser_hydrogen_chloride", "capability": GAS_COMBINATION,
         "fluids": [(f"{MOD}:hydrogen", 250), (f"{MOD}:chlorine", 250)],
         "fluid_results": [(f"{MOD}:hydrogen_chloride", 500, 0)], "ticks": 75, "features": ["salt"]},
        # CO2 methanation over the reusable nickel bed (the plan's proposal): CO2 + 4 H2 -> CH4 + 2 H2O, the water at the
        # liquid anchor of 2 mB a reaction amount. 200 ticks at 128 JE/t: the plan's 25,600 JE.
        {"name": "infuser_carbon_dioxide_methanation", "capability": NICKEL_METHANATION,
         "tool": f"{MOD}:nickel_catalyst_bed",
         "fluids": [(f"{MOD}:hydrogen", 400), (f"{MOD}:carbon_dioxide", 100)],
         "fluid_results": [(f"{MOD}:methane", 100, 0), ("minecraft:water", 400, 1)], "ticks": 200, "features": ["machines"]},
        # Contact conversion (docs/features/industrial-starter-gas-and-acid-factory.md, the plan's row): 250 mB of sulfur
        # dioxide and 125 mB of oxygen over the vanadium contact bed give 250 mB of sulfur trioxide. 100 ticks at 128 JE/t,
        # the plan's 12,800 JE.
        {"name": "infuser_sulfur_trioxide", "capability": CONTACT_CONVERSION, "tool": f"{MOD}:vanadium_contact_bed",
         "fluids": [(f"{MOD}:oxygen", 125), (f"{MOD}:sulfur_dioxide", 250)],
         "fluid_results": [(f"{MOD}:sulfur_trioxide", 250, 0)], "ticks": 100, "features": ["sulfur", "machines"]},
        # CO methanation over the same bed (the plan's distinct CO + 3 H2 recipe): CO + 3 H2 -> CH4 + H2O, the water at the
        # liquid anchor of 2 mB a reaction amount. 150 ticks at 128 JE/t, 19,200 JE.
        {"name": "infuser_carbon_monoxide_methanation", "capability": NICKEL_METHANATION,
         "tool": f"{MOD}:nickel_catalyst_bed", "fluids": [(f"{MOD}:hydrogen", 300), (f"{MOD}:carbon_monoxide", 100)],
         "fluid_results": [(f"{MOD}:methane", 100, 0), ("minecraft:water", 200, 1)], "ticks": 150, "features": ["machines"]},
        # Biogas upgrading over the same bed (docs/features/industrial-starter-gas-and-acid-factory.md, the 60/40
        # mapping): 250 mB of biogas holds 150 of methane and 100 of carbon dioxide; the carbon dioxide takes 400 mB of
        # hydrogen as in methanation, giving 250 mB of methane in all and 400 mB of water. 200 ticks at 128 JE/t.
        {"name": "infuser_biogas_upgrading", "capability": NICKEL_METHANATION, "tool": f"{MOD}:nickel_catalyst_bed",
         "fluids": [(f"{MOD}:hydrogen", 400), (f"{MOD}:biogas", 250)],
         "fluid_results": [(f"{MOD}:methane", 250, 0), ("minecraft:water", 400, 1)], "ticks": 200, "features": ["machines"]},
    ],
    "anaerobic_digester": [
        # Organic waste digested (this change's proposal): sixteen items and a bucket of water make a bucket of raw biogas
        # in the selected 60/40 methane/carbon dioxide mixture, and two bone meal of digestate, about what a composter
        # gives for as many items. 400 ticks at the reactor family's 96 JE/t, 38,400 JE (30,800 in the bulk profile).
        {"name": "digester_biogas", "capability": ANAEROBIC_DIGESTION, "items": [(f"#{MOD}:{DIGESTIBLE}", 16)],
         "fluids": [("minecraft:water", 1000)], "fluid_results": [(f"{MOD}:biogas", 1000, 0)],
         "results": [("minecraft:bone_meal", 2)], "ticks": 400, "features": ["machines"]},
    ],
    "industrial_fermenter": [
        # The Chemical Reactor's fermentation (eight fermentable crops and a bucket of water, 250 mB of bioethanol, 100 ticks)
        # with its carbon dioxide collected: one reaction amount of gas for each of alcohol, 250 mB. Vented when not taken.
        {"name": "fermenter_bioethanol", "capability": CONTROLLED_FERMENTATION, "items": [("#jugcraft:fermentable", 8)],
         "fluids": [("minecraft:water", 1000)],
         "fluid_results": [(f"{MOD}:bioethanol", 250, 0), (f"{MOD}:carbon_dioxide", 250, 1)], "ticks": 100,
         "features": ["machines"]},
    ],
    "bulk_chemical_reactor": [
        # Hydrogen chloride absorbed in water (docs/features/industrial-starter-gas-and-acid-factory.md, aqueous HCl
        # preparation): 500 mB of the gas and a bucket of water make a bucket of ordinary hydrochloric acid, 100 ticks at
        # the reactor family's 96 JE/t, the plan's 9,600 JE.
        {"name": "bulk_hydrochloric_acid", "capability": ACID_ABSORPTION,
         "fluids": [("minecraft:water", 1000), (f"{MOD}:hydrogen_chloride", 500)],
         "fluid_results": [(f"{MOD}:hydrochloric_acid", 1000, 0)], "ticks": 100, "features": ["salt"]},
        # Ordinary sulfuric-acid absorption (the plan's row): 250 mB of sulfur trioxide in 500 mB of water make 500 mB of
        # acid, 100 ticks at 96 JE/t, the plan's 9,600 JE.
        {"name": "bulk_sulfuric_acid", "capability": ACID_ABSORPTION,
         "fluids": [("minecraft:water", 500), (f"{MOD}:sulfur_trioxide", 250)],
         "fluid_results": [(f"{MOD}:sulfuric_acid", 500, 0)], "ticks": 100, "features": ["sulfur", "machines"]},
        # Four vanadium-bearing concentrates roasted with lye give pentoxide without an acid gate.
        # Preserve the ordinary washed-iron recipe only behind the explicit legacy fallback.
        {"name": "bulk_vanadium_pentoxide_from_concentrate", "capability": VANADIUM_RECOVERY,
         "items": [(f"{MOD}:vanadium_iron_concentrate", 4)],
         "fluids": [(f"{MOD}:lye", 500)], "results": [(f"{MOD}:vanadium_pentoxide", 1)], "ticks": 200,
         "features": ["salt", "machines"]},
        {"name": "bulk_vanadium_pentoxide", "legacy_vanadium": True, "capability": VANADIUM_RECOVERY, "items": [(f"{MOD}:washed_iron_ore", 4)],
         "fluids": [(f"{MOD}:lye", 500)], "results": [(f"{MOD}:vanadium_pentoxide", 1)], "ticks": 200,
         "features": ["salt", "machines"]},
        # Ammonium nitrate (this change's proposal): HNO3 + NH3 -> NH4NO3, 250 mB of nitric acid (125 reaction amounts at
        # the liquid's 2 mB) neutralized by 125 mB of ammonia gas, crystallized into four ammonium nitrate. 100 ticks at
        # 96 JE/t.
        {"name": "bulk_ammonium_nitrate", "capability": NITRATE_NEUTRALIZATION,
         "fluids": [(f"{MOD}:nitric_acid", 250), (f"{MOD}:ammonia", 125)],
         "results": [(f"{MOD}:ammonium_nitrate", 4)], "ticks": 100, "features": ["salt", "machines"]},
    ],
    "coal_gasifier": [
        # Steam gasification (this change's proposal for the pending carbon ledger): a coal or coke counts as 250 reaction
        # amounts of carbon, like a sulfur dust, C + H2O -> CO + H2: 250 mB of each gas, 500 mB of the mixture. The steam
        # is fed in excess, 500 mB of water, as a steam gasifier runs; no fluid comes from nothing. 200 ticks at 96 JE/t,
        # 19,200 JE of paid heat. The ash chamber keeps the mineral matter as one mixed residue a piece (the plan's single
        # initial cleaning residue, this change's quantity); charcoal, nearly ash-free, leaves none.
        {"name": "gasifier_coal", "capability": COAL_GASIFICATION, "items": [("minecraft:coal", 1)],
         "fluids": [("minecraft:water", 500)], "fluid_results": [(f"{MOD}:raw_syngas", 500, 0)],
         "results": [(f"{MOD}:gasification_residue", 1)], "ticks": 200,
         "features": ["machines"]},
        {"name": "gasifier_coke", "capability": COAL_GASIFICATION, "items": [(f"{MOD}:coke", 1)],
         "fluids": [("minecraft:water", 500)], "fluid_results": [(f"{MOD}:raw_syngas", 500, 0)],
         "results": [(f"{MOD}:gasification_residue", 1)], "ticks": 200,
         "features": ["machines"]},
        # Charcoal: the owner's selected lower-yield alternative, 200 reaction amounts of carbon.
        {"name": "gasifier_charcoal", "capability": COAL_GASIFICATION, "items": [("minecraft:charcoal", 1)],
         "fluids": [("minecraft:water", 400)], "fluid_results": [(f"{MOD}:raw_syngas", 400, 0)], "ticks": 200,
         "features": ["machines"]},
    ],
    "gas_cleanup_rack": [
        # Cleaning and dividing the mixture: each gas once, never more than went in. 100 ticks at 128 JE/t, 12,800 JE.
        {"name": "rack_raw_syngas", "capability": GAS_SEPARATION, "fluids": [(f"{MOD}:raw_syngas", 500)],
         "fluid_results": [(f"{MOD}:carbon_monoxide", 250, 0), (f"{MOD}:hydrogen", 250, 1)], "ticks": 100,
         "features": ["machines"]},
    ],
    "chemical_oxidizer": [
        # Sulfur oxidation (docs/features/industrial-starter-gas-and-acid-factory.md): a dust counts as 250 reaction
        # amounts of sulfur, burning with as much oxygen into as much sulfur dioxide. 100 ticks at the reactor family's
        # 96 JE/t: the plan's 9,600 JE.
        {"name": "oxidizer_sulfur_dioxide", "capability": SOLID_OXIDATION, "items": [(f"{MOD}:sulfur_dust", 1)],
         "fluids": [(f"{MOD}:oxygen", 250)], "fluid_results": [(f"{MOD}:sulfur_dioxide", 250, 0)], "ticks": 100,
         "features": ["sulfur", "machines"]},
        # Carbon burnt in oxygen: a coal or charcoal counts as 250 reaction amounts of carbon, like a sulfur dust, giving
        # as much carbon dioxide. Its heat is not recovered. 100 ticks at 96 JE/t, 9,600 JE.
        {"name": "oxidizer_carbon_dioxide", "capability": SOLID_OXIDATION, "items": [("#minecraft:coals", 1)],
         "fluids": [(f"{MOD}:oxygen", 250)], "fluid_results": [(f"{MOD}:carbon_dioxide", 250, 0)], "ticks": 100,
         "features": ["machines"]},
    ],
}


def _chloride_leaching():
    """Each ore dissolved in 250 mB of hydrochloric acid gives four washed ores, as sulfuric acid does in the Chemical
    Reactor (tools/petro.py _leaching): a second, sulfur-free route to the best ore yield, never a better one."""
    from machines import ORE_LEACHING_MULTIPLIER, _metal_features
    from materials import WASHED_ORES, ore_ids
    return [{"name": f"bulk_chloride_{ore.split(':')[1]}", "capability": CHLORIDE_LEACHING, "items": [(ore, 1)],
             "fluids": [(f"{MOD}:hydrochloric_acid", 250)],
             "results": [(f"{MOD}:washed_{metal}_ore", ORE_LEACHING_MULTIPLIER)], "ticks": 160,
             "ore_bonus": ORE_LEACHING_MULTIPLIER, "features": ["salt"] + _metal_features(metal)}
            for metal in WASHED_ORES for ore in ore_ids(metal)]


FORM_RECIPES["bulk_chemical_reactor"] += _chloride_leaching()


def blocks():
    return list(FORMS)


def footprint(layers):
    """The parts' offsets from the controller, in part order (MachineForm in Java): x towards the viewer's left,
    y up, z towards the back."""
    width, depth = len(layers[0][0]), len(layers[0])
    cells = [(column, row, layer) for layer in range(len(layers)) for row in range(depth) for column in range(width)]
    controller = next(c for c in cells if layers[c[2]][c[1]][c[0]] == "C")
    parts = [controller] + [c for c in cells if layers[c[2]][c[1]][c[0]] == "#"]
    return [(controller[0] - column, layer - controller[2], row - controller[1]) for column, row, layer in parts]


def electrolytic_separator():
    """Two wide, two deep, three tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a tall
    olive cell housing with a stepped door stands between two narrower round collection towers, white with checker
    shoulders, on a shared gunmetal plinth. At the front: the left-side control box with two gauges and an amber lamp
    (the controller), an insulated bus connection with its power socket at the foot of the right tower, the lye
    return recessed above it, and an outlet pipe forward from each tower to a collar high on the front, hydrogen on
    the left and oxygen or chlorine on the right, banded in their colours. The feed comes into the cell low at the
    back. Each port sits at the middle of its block's face, where pipes and cables meet it."""
    m = [box((-16, 0, 0), (16, 2, 32), GUNMETAL)]
    # The cell housing: a gunmetal skirt, the olive body and a crown with four electrode feedthroughs on ceramic
    # insulators and copper caps, joined in pairs by bus bars.
    m.append(box((-6.5, 2, 2.5), (6.5, 4, 26.5), GUNMETAL))
    m.append(box((-6, 4, 3), (6, 44, 26), {"*": OLIVE, "up": GUNMETAL}))
    for y in (15.25, 31.25):
        m.append(box((-6.5, y, 2.5), (6.5, y + 1.5, 26.5), GUNMETAL))
    m.append(box((-7, 44, 2), (7, 46, 27), GUNMETAL))
    for x in (-3, 3):
        for z in (8, 20):
            m += cyl("y", x, z, 1.25, 46, 47.25, INSULATOR)
            m.append(box((x - 0.75, 47.25, z - 0.75), (x + 0.75, 47.75, z + 0.75), COPPER))
    for z in (8, 20):
        m.append(box((-2.25, 46.5, z - 0.5), (2.25, 47, z + 0.5), BUSBAR))
    # The stepped door: a gunmetal frame, an olive leaf, a stencilled upper panel, a handle on the right and two
    # hinges on the left.
    m.append(box((-4.5, 6, 2.25), (4.5, 36, 3), GUNMETAL))
    m.append(box((-3.5, 7, 1.5), (3.5, 35, 2.25), OLIVE))
    m.append(box((-2.5, 22, 1), (2.5, 33, 1.5), {"*": OLIVE, "north": STENCIL}))
    m.append(box((-3.25, 13, 0.75), (-2.5, 18, 1.5), CHROME))
    for y in (17, 30):
        m.append(box((3.75, y, 1.5), (4.75, y + 2, 2.25), CHROME))
    # The collection towers, round and white, either side of the housing: a checker shoulder, a gunmetal dome with
    # a chrome vent, and an amber level strip on the outer side.
    for sign in (1, -1):
        cx = sign * 10.5
        m += cyl("y", cx, 14, 4, 2, 41, TANK_BODY, TANK_TOP)
        m += cyl("y", cx, 14, 4.4, 33, 35, TANK_CHECKER)
        m += cyl("y", cx, 14, 3, 41, 42.5, GUNMETAL)
        m += cyl("y", cx, 14, 1.25, 42.5, 44, CHROME)
        strip_lo, strip_hi = sorted((sign * 14.5, sign * 14.75))
        m.append(box((strip_lo, 8, 13), (strip_hi, 30, 15), STRIP))
    # An outlet pipe forward from each tower to a collar at the middle of its upper front face: charcoal, banded in
    # its gas's colours, with a chrome flange where pipes meet it.
    for x, bands in ((8, ["hydrogen_still"]), (-8, ["oxygen_still", "chlorine_still"])):
        m.append(box((x - 1.5, 38.5, 1), (x + 1.5, 41.5, 12), RUBBER))
        for number, band in enumerate(bands):
            z = 3 + number * 1.5
            m.append(box((x - 2, 38, z), (x + 2, 42, z + 1), band))
        m.append(box((x - 3, 37, 0.25), (x + 3, 43, 1), CHROME))
    # The control box (the controller), standing on the plinth before the left tower: two gauges, the running lamp
    # and a level strip.
    m.append(box((5.25, 2, 0.5), (15, 15, 7), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (12.5, 10.5, 0.25), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (8, 10.5, 0.25), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (10.25, 5, 0.25), 2.5, texture=LAMP, body=GUNMETAL))
    # The insulated bus connection at the foot of the right tower: a gunmetal junction box with a hazard band at
    # the live end, its power socket, two ceramic insulators on top and a copper bus bar into the cell housing.
    m.append(box((-14, 2, 1.25), (-6, 13, 7), GUNMETAL))
    m.append(box((-14, 13, 1.25), (-6, 14, 7), HAZARD))
    m.append(box((-11, 5, 0.25), (-5, 11, 1.25), {"*": CHROME, "north": "power_port!"}))
    for x in (-12, -9):
        m += cyl("y", x, 3.5, 1, 14, 17.5, INSULATOR)
    m.append(box((-12.75, 17.5, 2.75), (-6, 18.5, 4.25), BUSBAR))
    # The lye return from the cell, recessed in a gunmetal frame bolted to the housing: a dark well, a pipe stub in
    # lye colour and a chrome flange.
    m.append(box((-12.5, 19.5, 1.5), (-4.75, 28.5, 7), GUNMETAL))
    m.append(box((-11.5, 20, 1.25), (-5, 28, 1.5), "sp_hopper_inside"))
    m.append(box((-9.5, 22.5, 0.75), (-6.5, 25.5, 1.25), "lye_still"))
    m.append(box((-10.5, 21.5, 0.25), (-5.5, 26.5, 0.75), CHROME))
    # The feed inlet into the cell, low at the back: a charcoal pipe banded in brine colour and a chrome flange.
    m.append(box((5.5, 6.5, 24), (9.5, 9.5, 31), RUBBER))
    m.append(box((5.25, 6, 27.5), (10, 10, 28.5), "brine_still"))
    m.append(box((5, 5, 31), (11, 11, 31.75), CHROME))
    # A gunmetal access panel on the back of the cell housing.
    m.append(box((-4, 8, 26), (4, 36, 26.75), {"*": GUNMETAL, "south": STENCIL}))
    return m


def octagon_ring(axis, cu, cv, r_out, r_in, a0, a1, texture):
    """A ring of eight bars round (cu, cv) along `axis`: an octagon r_out across its flats with a hole r_in across."""
    side = q(r_out * math.tan(math.radians(22.5)))
    out = []
    for lo, hi in ((r_in, r_out), (-r_out, -r_in)):
        out.append(box(*span(axis, a0, a1, q(cu - side), q(cu + side), q(cv + lo), q(cv + hi)), texture))
        out.append(box(*span(axis, a0, a1, q(cu + lo), q(cu + hi), q(cv - side), q(cv + side)), texture))
    # The four corners, a little shorter along the axis so their caps never share a plane with the bars'.
    middle = (r_in + r_out) / 2 / math.sqrt(2)
    half = (r_out - r_in) / 2
    for su in (1, -1):
        for sv in (1, -1):
            u, v = cu + su * middle, cv + sv * middle
            out.append(box(*span(axis, a0 + 0.125, a1 - 0.125, q(u - half), q(u + half), q(v - half), q(v + half)), texture))
    return out


def arch(cu, cv, r, thickness, z0, z1, texture):
    """An arched hood over a cylinder along z: a flat crown, stepped shoulders and straight sides down to the axis,
    the outside an octagon r across its flats, `thickness` thick."""
    side = q(r * math.tan(math.radians(22.5)))
    out = [box((q(cu - side), q(cv + r - thickness), z0), (q(cu + side), q(cv + r), z1), texture)]
    steps = 3
    for sign in (1, -1):
        for step in range(steps):
            u0 = side + (r - side) * step / steps
            u1 = side + (r - side) * (step + 1) / steps
            top = cv + r + side - u0
            bottom = cv + r + side - u1 - thickness
            lo, hi = sorted((cu + sign * u0, cu + sign * u1))
            out.append(box((q(lo), q(bottom), z0), (q(hi), q(top), z1), texture))
        lo, hi = sorted((cu + sign * (r - thickness), cu + sign * r))
        out.append(box((q(lo), q(cv), z0), (q(hi), q(cv + side - thickness), z1), texture))
    return out


def gas_burning_generator():
    """Four wide, five deep and four tall (the art brief, docs/features/industrial-machine-models-and-textures.md): an
    engine-generator train along the middle of a gunmetal skid. At the front, the round intake with its fan (drawn
    turning by MachineRotors) and, at the front left, the stepped ignition box with its gauges and amber lamp (the
    controller). Behind them the olive combustion section with graphite ribs and amber sight glasses, under an arched
    pale heat shield on four posts; the guarded coupling (turning, behind hazard-banded hoops); then the large ribbed
    generator housing with a cooling grille at its back. At the rear right the tall terminal cabinet: an olive door,
    mint lamps, the power socket and copper terminals on ceramic insulators on top. The capped gas inlet enters the
    right side above the skid and runs through a regulator to the burner; the exhaust leaves the burner's belly, runs
    back low along the skid on the left and turns out at the back."""
    cu, cv = -16, 26
    m = [box((-47, 0, 1), (15, 3, 79), GUNMETAL)]
    # The combustion section on two saddles: olive covers, graphite ribs, a dark intake throat at its front.
    for z in (9, 31):
        m.append(box((cu - 9, 3, z), (cu + 9, 14, z + 4), GUNMETAL))
    m += cyl("z", cu, cv, 13, 6, 40, OLIVE, EXHAUST)
    for z in (11, 19, 27, 35):
        m += cyl("z", cu, cv, 13.75, z, z + 1.5, GUNMETAL)
    # Amber sight glasses on either side of the burner, in gunmetal frames.
    for sign in (1, -1):
        frame_lo, frame_hi = sorted((cu + sign * 13, cu + sign * 13.4))
        glass_lo, glass_hi = sorted((cu + sign * 13, cu + sign * 13.6))
        m.append(box((frame_lo, 20.5, 8.5), (frame_hi, 25.5, 15.5), GUNMETAL))
        m.append(box((glass_lo, 21, 9), (glass_hi, 25, 15), STRIP))
    # The round intake: a chrome rim round the throat and a cross of guard bars in front of the fan, with a hub cap.
    m += octagon_ring("z", cu, cv, 14, 11.5, 3, 6.5, CHROME)
    m.append(box((cu - 12, cv - 0.75, 2.25), (cu + 12, cv + 0.75, 3), GUNMETAL))
    m.append(box((cu - 0.75, cv - 12, 2.5), (cu + 0.75, cv + 12, 2.75), GUNMETAL))
    m += cyl("z", cu, cv, 2, 1.75, 2.25, CHROME)
    # The arched heat shield over the combustion section, on four posts down to the skid.
    m += arch(cu, cv, 16, 1.5, 4, 42, SHIELD)
    for sign in (1, -1):
        lo, hi = sorted((cu + sign * 14.5, cu + sign * 16))
        for z in (4, 40):
            m.append(box((lo, 3, z), (hi, cv, z + 2), GUNMETAL))
    # The stepped ignition box (the controller) at the front left: gauges and the amber running lamp, a stencilled
    # upper step, and a conduit under the burner.
    m.append(box((2, 3, 1), (14, 12, 12), {"*": OLIVE, "up": STENCIL}))
    m.append(box((4, 12, 3), (12, 15.5, 10), GUNMETAL))
    m.append(dial("north", (11, 8.5, 0.75), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (6.5, 8.5, 0.75), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (8.75, 4.75, 0.75), 2.25, texture=LAMP, body=GUNMETAL))
    m.append(box((-8.5, 13.5, 6.5), (2, 15.5, 8.5), RUBBER))
    # The guarded coupling between the combustion section and the generator: hazard-banded hoops joined by four bars,
    # standing on a gunmetal leg.
    m += octagon_ring("z", cu, cv, 10.5, 9.5, 40.5, 41.5, HAZARD)
    m += octagon_ring("z", cu, cv, 10.5, 9.5, 46.5, 47.5, HAZARD)
    for su in (1, -1):
        for sv in (1, -1):
            u, v = cu + su * 7, cv + sv * 7
            m.append(box((u - 0.75, v - 0.75, 41.5), (u + 0.75, v + 0.75, 46.5), GUNMETAL))
    m.append(box((cu - 1, 3, 43), (cu + 1, cv - 10.5, 45), GUNMETAL))
    # The large ribbed generator housing on two cradles, a bearing boss at its front and a cooling grille at its back.
    for z in (51, 65):
        m.append(box((cu - 12, 3, z), (cu + 12, 8, z + 4), GUNMETAL))
    m += cyl("z", cu, cv, 20, 48, 72, OLIVE, GUNMETAL)
    for z in (49.5, 53.5, 57.5, 61.5, 65.5, 69.5):
        m += cyl("z", cu, cv, 21, z, z + 1.5, RIBBED)
    m += cyl("z", cu, cv, 8, 46.5, 48, GUNMETAL)
    m += cyl("z", cu, cv, 13, 72, 72.75, GRILLE)
    # The terminal cabinet at the rear right: gunmetal, an olive door with a stencil, a hazard band at the top, two
    # mint lamps, the power socket, and three ceramic insulators with copper caps and a bus bar on top. Black cables
    # come over from the generator housing.
    m.append(box((-47, 3, 72.5), (-21, 54, 79), GUNMETAL))
    m.append(box((-44.5, 26, 79), (-30.5, 48, 79.5), {"*": OLIVE, "south": STENCIL}))
    m.append(box((-47.25, 50, 72.25), (-20.75, 52, 79.25), HAZARD))
    for y in (45.5, 40):
        m.append(dial("south", (-25.5, y, 79.25), 2.5, texture=MINT, body=CHROME))
    m.append(box((-27, 21, 79), (-21, 27, 79.75), {"*": CHROME, "south": "power_port!"}))
    for x in (-42, -34, -26):
        m += cyl("y", x, 76, 1.5, 54, 57.5, INSULATOR)
        m.append(box((x - 1, 57.5, 75), (x + 1, 58, 77), COPPER))
    m.append(box((-42.75, 58, 75.25), (-25.25, 59, 76.75), BUSBAR))
    for x in (-30, -24):
        m.append(box((x - 1.5, 43, 60), (x + 1.5, 46, 72.5), RUBBER))
    # The exhaust: out of the combustion section's belly, across to the left, back along the skid and turned out at the
    # back.
    m.append(box((-12, 12, 23.5), (-8, 14, 28.5), EXHAUST))
    m.append(box((-10, 8, 23), (5, 12, 29), EXHAUST))
    m.append(box((5, 7, 23), (11, 12, 76), EXHAUST))
    for z in (44, 56, 68):
        m.append(box((4.75, 6.75, z), (11.25, 12.25, z + 1.5), CHROME))
    m.append(box((5, 7, 76), (11, 16, 79), EXHAUST))
    m.append(box((4.5, 10, 79), (11.5, 16.5, 79.75), {"*": CHROME, "south": EXHAUST}))
    # The capped gas inlet on the right side above the skid: a chrome flange, a black hose banded in hydrogen colour,
    # a regulator, and the line into the burner's belly.
    m.append(box((-47.75, 5, 21), (-47, 11, 27), CHROME))
    m += cyl("x", 8, 24, 2, -47, -35, RUBBER)
    m += cyl("x", 8, 24, 2.3, -44, -43, "hydrogen_still")
    m.append(box((-37.5, 10, 21.5), (-32.5, 15, 26.5), GUNMETAL))
    m += cyl("x", 13.25, 24, 1.25, -32.5, -23, RUBBER)
    return m


def gas_burning_generator_rotor():
    """The Gas Burning Generator's turning parts, about the train's axis (client/MachineRotors draws them): the intake
    fan's hub and six blades behind the guard bars, and the coupling's shaft and two bolted flanges inside the guard."""
    import machine_rotors as rotors
    cu, cv = -16, 26
    quads = rotors.disc("z", 3.25, 5.75, cu, cv, 2.5, CHROME, CHROME)
    for blade in range(6):
        angle = math.radians(60 * blade + 15)
        c, s = math.cos(angle), math.sin(angle)

        def at(r, w):
            return (cu + r * c - w * s, cv + r * s + w * c)
        quads += rotors.prism("z", 4, 5, [at(2.25, -1.25), at(10.25, -2), at(10.25, 2), at(2.25, 1.25)], GUNMETAL, GUNMETAL)
    quads += rotors.disc("z", 38, 50, cu, cv, 2.5, CHROME, CHROME)
    for z0, z1, bolts in ((42.5, 44, (42, 42.5)), (44.5, 46, (46, 46.5))):
        quads += rotors.disc("z", z0, z1, cu, cv, 7, CHROME, "dr_flange")
        for bolt in range(4):
            angle = math.radians(90 * bolt + 45)
            quads += rotors.disc("z", bolts[0], bolts[1], cu + 5 * math.cos(angle), cv + 5 * math.sin(angle), 0.75,
                                 GUNMETAL, GUNMETAL, sides=6)
    return quads


def gas_burning_generator_still():
    """The turning parts standing still, for the item model only (the block models leave them to MachineRotors)."""
    cu, cv = -16, 26
    return (cyl("z", cu, cv, 10.25, 4, 5, GUNMETAL, "dr_fan") + cyl("z", cu, cv, 2.5, 3.25, 5.75, CHROME)
            + cyl("z", cu, cv, 2.5, 38, 50, CHROME) + cyl("z", cu, cv, 7, 42.5, 44, CHROME, "dr_flange")
            + cyl("z", cu, cv, 7, 44.5, 46, CHROME, "dr_flange"))


# The Infuser's cooling fan, on the jacket's axis at the back, turning about y: its hub's centre (x, y, z).
INFUSER_FAN = (0, 35.5, 40)


def chemical_infuser():
    """Two wide, three deep and three tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a
    central stepped reaction chamber of cool gunmetal on a shared plinth, its lower body carrying olive service plates
    and stepping in at a pale ceramic collar. A substantial inlet branch enters each shoulder, with a valve and a
    red handwheel: hydrogen on the left, the other gas on the right, banded in their colours. At the front, the thick
    catalyst-bed drawer with a raised handle and its bed-face marker; above it the ringed inspection port, the heat
    gauge and the amber process band; at the front left the control box with its gauge and amber lamp (the controller).
    Behind the chamber a closed loop runs into the short ribbed cooling jacket, with its fan (drawn turning by
    MachineRotors) under a guard on top, the product collar high at the back and the water drain low beside it. The
    power socket sits on a junction box low on the right side at the back. Each port is at the middle of its block's
    face, where pipes and cables meet it."""
    m = [box((-16, 0, 0), (16, 2, 48), GUNMETAL)]
    # The reaction chamber: a skirt, the lower body with an olive service plate on either side, the ceramic collar
    # where it steps in, the middle body with its lip, and the crown with a relief valve.
    m.append(box((-13, 2, 3), (13, 4, 29), GUNMETAL))
    m.append(box((-12, 4, 4), (12, 22, 28), {"*": OLIVE, "up": GUNMETAL}))
    for sign, face in ((1, "east"), (-1, "west")):
        lo, hi = sorted((sign * 12, sign * 12.75))
        m.append(box((lo, 6, 7), (hi, 18, 25), {"*": GUNMETAL, face: STENCIL}))
    m.append(box((-12.5, 22, 3.5), (12.5, 24, 28.5), CERAMIC))
    m.append(box((-10, 24, 6), (10, 36, 27.5), GUNMETAL))
    m.append(box((-10.5, 36, 5.5), (10.5, 37.5, 28), GUNMETAL))
    m.append(box((-7, 37.5, 9), (7, 42, 23), {"*": GUNMETAL, "up": OLIVE}))
    m += cyl("y", 0, 16, 1.5, 42, 44.5, CHROME)
    m += cyl("y", 0, 16, 2.5, 44.5, 46, GUNMETAL)
    # The thick catalyst-bed drawer at the foot of the front: a frame, the olive drawer, a raised chrome handle on two
    # posts and the small plate marking the bed it holds.
    m.append(box((-7.5, 4.5, 3.25), (7.5, 15.5, 4), GUNMETAL))
    m.append(box((-6.5, 5.5, 1.75), (6.5, 14.5, 3.25), {"*": GUNMETAL, "north": OLIVE}))
    for x in (-4, 4):
        m.append(box((x - 0.5, 8.75, 0.75), (x + 0.5, 10.25, 1.75), CHROME))
    m.append(box((-4.5, 9, 0.25), (4.5, 10, 0.75), CHROME))
    m.append(box((-2, 11.5, 1.5), (2, 13.25, 1.75), CHROME))
    # The reaction zone: the ringed inspection port and the heat gauge on the middle body, and the amber process band
    # round it above the collar, which lights while it works.
    m.append(box((-7.5, 25.75, 5.5), (2.5, 35.25, 6), FLANGE))
    m.append(dial("north", (-2.5, 30.5, 4.75), 7.5, texture=PORTHOLE, body=CHROME))
    m.append(dial("north", (6, 31, 5.25), 4, texture=GAUGE, body=CHROME))
    m.append(box((-9.5, 24.25, 5.5), (9.5, 25.25, 6), STRIP))
    for sign in (1, -1):
        lo, hi = sorted((sign * 10, sign * 10.5))
        m.append(box((lo, 24.25, 6.5), (hi, 25.25, 18), STRIP))
    # The control box (the controller) at the front left, on the plinth: a gauge and the amber running lamp.
    m.append(box((8.5, 2, 0.75), (15.5, 14, 4), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (12, 10, 0.5), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (12, 5, 0.5), 2.5, texture=LAMP, body=GUNMETAL))
    # An inlet branch on each shoulder, from a flange at the middle of the side face into the chamber: a charcoal pipe
    # banded in its gas's colour, a valve with a red handwheel and a chrome collar where it meets the chamber.
    for sign, face, band in ((1, "east", "hydrogen_still"), (-1, "west", "chlorine_still")):
        lo, hi = sorted((sign * 10, sign * 15))
        m += cyl("x", 24, 24, 2.75, lo, hi, RUBBER)
        lo, hi = sorted((sign * 10, sign * 11))
        m += cyl("x", 24, 24, 3.5, lo, hi, CHROME)
        lo, hi = sorted((sign * 13.25, sign * 14.25))
        m += cyl("x", 24, 24, 3.1, lo, hi, band)
        lo, hi = sorted((sign * 11.5, sign * 13))
        m.append(box((lo, 26.5, 22), (hi, 28.5, 26), GUNMETAL))
        m += cyl("y", sign * 12.25, 24, 0.5, 28.5, 30, CHROME)
        m += cyl("y", sign * 12.25, 24, 2.25, 30, 30.75, HANDWHEEL)
        lo, hi = sorted((sign * 15, sign * 15.75))
        m.append(box((lo, 19.5, 19.5), (hi, 28.5, 28.5), {"*": CHROME, face: f"{FLANGE}!"}))
    # The closed loop from the chamber's back into the cooling jacket: hot gas out high, the cooled return low.
    m += cyl("z", 0, 31, 2, 27, 34, RUBBER)
    m += cyl("z", 0, 10, 2, 28, 34, RUBBER)
    for y in (31, 10):
        m += cyl("z", 0, y, 2.5, 32, 33, CHROME)
    # The short cooling jacket: a ribbed drum on the plinth, gunmetal banded, a cap and the fan shroud on top, its fan
    # under two guard bars.
    m += cyl("y", 0, 40, 6.5, 2, 30, RIBBED, GUNMETAL)
    for y in (8, 17, 26):
        m += cyl("y", 0, 40, 7, y, y + 1.5, GUNMETAL)
    m += cyl("y", 0, 40, 7, 30, 33, GUNMETAL)
    m += octagon_ring("y", 0, 40, 7, 5.5, 33, 38.5, GUNMETAL)
    m.append(box((-5.75, 37.5, 39.5), (5.75, 38.25, 40.5), CHROME))
    m.append(box((-0.5, 37.75, 34.25), (0.5, 38, 45.75), CHROME))
    # The product collar high at the back (on the right) and the water drain low (on the left): charcoal pipes out of
    # the jacket, banded hydrogen chloride and water, with chrome collars.
    m += cyl("z", -8, 24, 2.25, 42, 47, RUBBER)
    m += cyl("z", -8, 24, 2.6, 44.5, 45.5, "hydrogen_chloride_still")
    m.append(box((-11, 21, 47), (-5, 27, 47.75), {"*": CHROME, "south": f"{FLANGE}!"}))
    m += cyl("z", 8, 8, 1.75, 38, 47, RUBBER)
    m += cyl("z", 8, 8, 2.1, 44.5, 45.5, "sp_water")
    m.append(box((5, 5, 47), (11, 11, 47.75), {"*": CHROME, "south": f"{FLANGE}!"}))
    # The junction box low on the right side at the back: its power socket, a hazard band, two insulators and a
    # conduit into the jacket.
    m.append(box((-15, 2, 34), (-9, 14, 46), GUNMETAL))
    m.append(box((-15.25, 14, 33.75), (-8.75, 15, 46.25), HAZARD))
    m.append(box((-15.75, 5, 37), (-15, 11, 43), {"*": CHROME, "west": "power_port!"}))
    for z in (37, 43):
        m += cyl("y", -12, z, 1, 15, 18, INSULATOR)
        m.append(box((-12.5, 18, z - 0.5), (-11.5, 18.5, z + 0.5), COPPER))
    m += cyl("x", 9, 40, 1.25, -9, -6, RUBBER)
    return m


def chemical_infuser_rotor():
    """The Infuser's cooling fan inside the shroud on top of the jacket, turning about y (client/MachineRotors draws
    it): a hub and five blades."""
    import machine_rotors as rotors
    cx, y, cz = INFUSER_FAN
    quads = rotors.disc("y", y - 1.25, y + 1.25, cx, cz, 1.25, CHROME, CHROME)
    for blade in range(5):
        angle = math.radians(72 * blade)
        c, s = math.cos(angle), math.sin(angle)

        def at(r, w):
            return (cx + r * c - w * s, cz + r * s + w * c)
        quads += rotors.prism("y", y - 0.5, y + 0.5, [at(1.6, -0.55), at(4.75, -1.6), at(4.75, 1.6), at(1.6, 0.55)], GUNMETAL, GUNMETAL)
    return quads


def chemical_infuser_still():
    """The fan standing still, for the item model only (the block models leave it to MachineRotors)."""
    cx, y, cz = INFUSER_FAN
    return cyl("y", cx, cz, 4.75, y - 0.5, y + 0.5, GUNMETAL, "dr_fan") + cyl("y", cx, cz, 1.25, y - 1.25, y + 1.25, CHROME)


# The Oxidizer's feed screw, under the hopper at the back, turning about z: its axis's centre (x, y, z).
OXIDIZER_SCREW = (6, 7.5, 27)


def chemical_oxidizer():
    """Two wide, two deep and three tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a
    squat dark-steel reaction throat on a plinth, a cream refractory ring round its circular bolted hatch, the orange
    reaction slit above it and the deep residue drawer below; a short gas column and a ringed collector hood on top,
    its outlet rising to a collar at the top on the right. At the back, a broad olive hopper with a hazard-striped feed
    lip drops through a chute into a guarded trough, where the feed screw (drawn turning by MachineRotors) carries the
    dust forward into the throat. The capped oxidant inlet enters low on the right; the power socket sits on a junction
    box low on the left at the back; the control panel (the controller) stands at the front left. Each port is at the
    middle of its block's face."""
    m = [box((-16, 0, 0), (16, 2, 32), GUNMETAL)]
    # The reaction throat, its front plate and the refractory ring round the bolted hatch.
    m.append(box((-12, 2, 3), (12, 24, 22), GUNMETAL))
    m.append(box((-12.5, 22, 2.5), (12.5, 24.5, 22.5), GUNMETAL))
    m += cyl("z", 0, 15, 6.5, 2.25, 3, REFRACTORY)
    m += cyl("z", 0, 15, 5, 1.5, 2.25, GUNMETAL, FLANGE)
    m.append(box((-1, 13.5, 0.75), (1, 16.5, 1.5), CHROME))
    # The reaction slit above the hatch, which glows while it burns, and the deep residue drawer below it.
    m.append(box((-4.5, 21, 2.5), (4.5, 22, 3), STRIP))
    m.append(box((-7, 3, 1.5), (7, 8.5, 3), {"*": GUNMETAL, "north": OLIVE}))
    m.append(box((-3, 5, 0.75), (3, 6.5, 1.5), CHROME))
    # The short gas column and the ringed collector hood, with the outlet rising to its collar at the top right.
    m += cyl("y", 0, 10, 6, 24.5, 36, GUNMETAL)
    m += cyl("y", 0, 10, 6.5, 30, 31.5, COPPER)
    m += cyl("y", 0, 10, 9, 36, 39, GUNMETAL)
    m += cyl("y", 0, 10, 9.5, 39, 40.5, REFRACTORY)
    m += cyl("y", 0, 10, 7, 40.5, 42, GUNMETAL)
    m += cyl("y", -8, 8, 2, 40.5, 47, RUBBER)
    m += cyl("y", -8, 8, 2.35, 43.5, 44.5, "sulfur_dioxide_still")
    m.append(box((-11, 47, 5), (-5, 47.75, 11), {"*": CHROME, "up": f"{FLANGE}!"}))
    # The control panel (the controller) at the front left: a gauge and the amber running lamp.
    m.append(box((9.5, 2, 1), (15.5, 13, 3), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (12.5, 9.5, 0.75), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (12.5, 4.5, 0.75), 2.5, texture=LAMP, body=GUNMETAL))
    # The capped oxidant inlet low on the right: a charcoal pipe banded in oxygen colour, a copper valve and a flange.
    m += cyl("x", 8, 8, 2.25, -15, -12, RUBBER)
    m += cyl("x", 8, 8, 2.6, -14.25, -13.25, "oxygen_still")
    m.append(box((-14.25, 10.25, 6.5), (-12.75, 12.25, 9.5), COPPER))
    m.append(box((-15.75, 3.5, 3.5), (-15, 12.5, 12.5), {"*": CHROME, "west": f"{FLANGE}!"}))
    # The broad hopper at the back, olive guards narrowing to a chute, its mouth under a hazard-striped feed lip.
    m.append(box((1, 41, 17), (15, 46, 31), OLIVE))
    m.append(box((3, 34, 19), (13, 41, 29), OLIVE))
    m.append(box((4, 27, 21), (11, 34, 28), OLIVE))
    m.append(box((3.5, 12, 22.5), (8.5, 27, 27.5), GUNMETAL))
    for x0, x1 in ((1.25, 2.75), (13.25, 14.75)):
        m.append(box((x0, 12, 29), (x1, 41, 30.5), GUNMETAL))
    m.append(box((2.75, 30, 29.25), (13.25, 31, 30.25), GUNMETAL))
    for lo, hi in (((0.5, 46, 16.5), (15.5, 47.75, 18)), ((0.5, 46, 30), (15.5, 47.75, 31.5)),
                   ((0.5, 46, 18), (2, 47.75, 30)), ((14, 46, 18), (15.5, 47.75, 30))):
        m.append(box(lo, hi, HAZARD))
    m.append(box((2, 46, 18), (14, 46.5, 30), MOUTH))
    # The guarded trough the feed screw turns in, running forward into the throat.
    m.append(box((1, 2, 22), (11, 3.5, 31.5), GUNMETAL))
    for x0, x1 in ((1, 2), (10, 11)):
        m.append(box((x0, 3.5, 22), (x1, 12, 31.5), GUNMETAL))
    m.append(box((2, 3.5, 30.75), (10, 12, 31.5), GUNMETAL))
    for z in (24, 27.5):
        m.append(box((2, 11.5, z), (10, 12, z + 1), CHROME))
    # The junction box low on the left at the back, its power socket and a hazard band.
    m.append(box((12, 2, 18), (15, 13, 30), GUNMETAL))
    m.append(box((11.75, 13, 17.75), (15.25, 14, 30.25), HAZARD))
    m.append(box((15, 5, 21), (15.75, 11, 27), {"*": CHROME, "east": "power_port!"}))
    return m


def chemical_oxidizer_rotor():
    """The Oxidizer's feed screw in its trough (client/MachineRotors draws it): a shaft and a run of flights, each a
    paddle turned a little further than the last, so it reads as a screw while it turns."""
    import machine_rotors as rotors
    cx, cy, cz = OXIDIZER_SCREW
    quads = rotors.disc("z", 22.5, 30.5, cx, cy, 0.75, CHROME, CHROME)
    for flight in range(6):
        z0 = 23 + flight * 1.25
        angle = math.radians(60 * flight)
        c, s_ = math.cos(angle), math.sin(angle)

        def at(r, w):
            return (cx + r * c - w * s_, cy + r * s_ + w * c)
        quads += rotors.prism("z", z0, z0 + 0.75, [at(0.9, -1.1), at(3.5, -1.4), at(3.5, 1.4), at(0.9, 1.1)], COPPER, COPPER)
    return quads


def chemical_oxidizer_still():
    """The feed screw standing still, for the item model only."""
    cx, cy, cz = OXIDIZER_SCREW
    return cyl("z", cx, cy, 0.75, 22.5, 30.5, CHROME) + cyl("z", cx, cy, 3, 24, 29.5, COPPER)


# The Fermenter's agitator drive pulley on top of the vessel, turning about y: its centre (x, y, z).
FERMENTER_DRIVE = (-8, 62.75, 24)
# The Fermenter's materials (the art brief): dull stainless steel for the vessel, a cream insulated jacket.
STAINLESS, JACKET = "dw_steel_plate", "sp_ceramic"


def industrial_fermenter():
    """Three wide, three deep and four tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a
    tall stepped vessel of dull stainless steel on four stout legs, a sloping bottom cone leading to the low broth
    outlet at the front, its pipe banded in bioethanol with a red-wheeled valve. The front carries the short cream
    insulated jacket between charcoal bands, the narrow amber level strip (which lights while it works) and the ringed
    manway on the upper body; at the front left, under the vessel, the instrument box (the controller) with its gauge
    and the green batch lamp. A shallow domed crown carries the enclosed agitator drive, its pulley (drawn turning by
    MachineRotors) on a gearbox driven by the motor lying towards the back. The feed hatch rises through the crown's
    shoulder at the top on the left; the closed carbon dioxide outlet leaves the upper body at the back to its flange,
    the water inlet enters lower down on the left, and the compact heat-services box at the back right, with copper
    risers into the jacket, carries the power socket. Each port is at the middle of its block's face."""
    cx, cz = -8, 24
    m = []
    # Four stout legs on feet under a support ring, and the sloping bottom cone.
    for lx in (cx - 14, cx + 14):
        for lz in (cz - 14, cz + 14):
            m.append(box((lx - 2.5, 0, lz - 2.5), (lx + 2.5, 1, lz + 2.5), GUNMETAL))
            m.append(box((lx - 1.5, 1, lz - 1.5), (lx + 1.5, 14, lz + 1.5), GUNMETAL))
    m += cyl("y", cx, cz, 10, 6, 10, GUNMETAL)
    m += cyl("y", cx, cz, 15, 10, 14, GUNMETAL)
    # The vessel: the lower body with its support ring, the cream jacket between charcoal bands, the shoulder ring, the
    # stepped upper body with a charcoal band, and the shallow domed crown.
    m += cyl("y", cx, cz, 19, 14, 40, STAINLESS, GUNMETAL)
    m += cyl("y", cx, cz, 19.75, 14, 15.5, GUNMETAL)
    m += cyl("y", cx, cz, 19.5, 18, 32, JACKET)
    for y in (17, 32):
        m += cyl("y", cx, cz, 19.75, y, y + 1, GUNMETAL)
    m += cyl("y", cx, cz, 19.25, 39, 41, GUNMETAL)
    m += cyl("y", cx, cz, 17, 41, 56, STAINLESS, GUNMETAL)
    m += cyl("y", cx, cz, 17.5, 47, 48.5, GUNMETAL)
    m += cyl("y", cx, cz, 14, 56, 58, STAINLESS)
    m += cyl("y", cx, cz, 11, 58, 60, STAINLESS)
    # The narrow level strip on the jacket's front, in a charcoal frame, and the ringed manway on the upper body.
    m.append(box((cx - 1.75, 18.5, 3.75), (cx + 1.75, 31.5, 4.5), GUNMETAL))
    m.append(box((cx - 1, 19.5, 3.5), (cx + 1, 30.5, 3.75), STRIP))
    m += cyl("z", cx, 48, 5.5, 6.25, 7, GUNMETAL, FLANGE)
    m.append(dial("north", (cx, 48, 5.5), 6, texture=PORTHOLE, body=CHROME))
    # The instrument box (the controller) at the front left under the vessel: the amber gauge and the green batch lamp.
    m.append(box((4, 0, 1), (15.5, 13.5, 7), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (11.5, 9.5, 0.75), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (7, 9.5, 0.75), 2, texture=MINT, body=GUNMETAL))
    # The low broth outlet from the cone to its flange at the front: a charcoal pipe banded in bioethanol, a valve with
    # a red handwheel.
    m += cyl("z", cx, 8, 2.25, 0.75, 22, RUBBER)
    m += cyl("z", cx, 8, 2.6, 3, 4, "bioethanol_still")
    m.append(box((cx - 3, 5, 0), (cx + 3, 11, 0.75), {"*": CHROME, "north": f"{FLANGE}!"}))
    m.append(box((cx - 1.5, 10, 7), (cx + 1.5, 11.5, 9), GUNMETAL))
    m += cyl("y", cx, 8, 0.5, 11.5, 12.5, CHROME)
    m += cyl("y", cx, 8, 2, 12.5, 13.25, HANDWHEEL)
    # The enclosed agitator drive on the crown: a gearbox under the pulley and the motor lying towards the back,
    # charcoal ribbed with its fan cover.
    m.append(box((cx - 4, 59, cz - 4), (cx + 4, 62, cz + 4), GUNMETAL))
    m += cyl("z", cx, 60.5, 2.25, cz + 4, cz + 13, OLIVE, GUNMETAL)
    for z in (cz + 6, cz + 9):
        m += cyl("z", cx, 60.5, 2.6, z, z + 1, GUNMETAL)
    m += cyl("z", cx, 60.5, 2.5, cz + 13, cz + 14, GUNMETAL, GRILLE)
    # The feed hatch rising through the crown's shoulder at the top on the left, its hazard-banded rim round the mouth.
    m += cyl("y", 8, cz, 3, 50, 62.5, GUNMETAL)
    m.append(box((3.5, 62.5, cz - 4.5), (12.5, 63.5, cz + 4.5), HAZARD))
    m.append(box((5, 63.5, cz - 3), (11, 64, cz + 3), {"*": CHROME, "up": MOUTH}))
    # The closed carbon dioxide outlet from the upper body to its flange high at the back.
    m += cyl("z", cx, 56, 2.25, 38, 47.25, RUBBER)
    m += cyl("z", cx, 56, 2.6, 44, 45, "carbon_dioxide_still")
    m.append(box((cx - 3, 53, 47.25), (cx + 3, 59, 48), {"*": CHROME, "south": f"{FLANGE}!"}))
    # The water inlet into the lower body at the back on the left.
    m += cyl("z", 8, 24, 1.75, 32, 47.25, RUBBER)
    m += cyl("z", 8, 24, 2.1, 44, 45, "sp_water")
    m.append(box((5, 21, 47.25), (11, 27, 48), {"*": CHROME, "south": f"{FLANGE}!"}))
    # The compact heat-services box at the back right: the power socket, a hazard band and copper risers into the
    # jacket.
    m.append(box((-31, 1.5, 41), (-17, 15, 47.25), GUNMETAL))
    m.append(box((-31.25, 15, 40.75), (-16.75, 16, 47.5), HAZARD))
    m.append(box((-27, 5, 47.25), (-21, 11, 48), {"*": CHROME, "south": "power_port!"}))
    for x in (-27, -21):
        m += cyl("y", x, 44, 1, 16, 22.5, COPPER)
        m += cyl("z", x, 22, 1, 38, 45, COPPER)
    return m


def industrial_fermenter_rotor():
    """The Fermenter's agitator drive pulley on its gearbox (client/MachineRotors draws it): a spoked pulley and a
    drive key across it, so its slow turn reads from the floor."""
    import machine_rotors as rotors
    cx, y, cz = FERMENTER_DRIVE
    quads = rotors.disc("y", y - 0.75, y + 0.5, cx, cz, 3.25, COPPER, "dr_pulley")
    quads += rotors.prism("y", y + 0.5, y + 1, [(cx - 3, cz - 0.5), (cx + 3, cz - 0.5), (cx + 3, cz + 0.5), (cx - 3, cz + 0.5)],
                          CHROME, CHROME)
    return quads


def industrial_fermenter_still():
    """The pulley standing still, for the item model only."""
    cx, y, cz = FERMENTER_DRIVE
    return cyl("y", cx, cz, 3.25, y - 0.75, y + 0.5, COPPER, "dr_pulley")


# The Bulk Chemical Reactor's stirring drive on its lid, turning about y: its centre (x, y, z).
BULK_REACTOR_DRIVE = (-16, 59.25, 34)


def bulk_chemical_reactor():
    """Four wide, four deep and four tall (the art brief, docs/features/industrial-machine-models-and-textures.md): the
    Chemical Reactor's jacketed vessel enlarged on a hazard-edged plinth, steel plate with olive bands and a cream lining
    edge under its lid. The front carries the deep bolted access plate with the restrained activity window and its amber
    strip; below it the lower product manifold (the acid outlet with its red-wheeled valve, banded in hydrochloric acid)
    and the washed-ore chute under a hazard hood; at the front left the control panel (the controller) with its gauge,
    amber lamp and green batch lamp. The lid carries the stirring drive (its pulley drawn turning by MachineRotors) on a
    gearbox driven by a motor lying towards the back, the ore hatch beside it and the short closed absorption inlet,
    banded in hydrogen chloride. The water inlet enters the left side; an acid sight glass stands beside it; the power
    socket sits on a junction box at the back right. Each port is at the middle of its block's face."""
    cx, cz = -16, 34
    m = [box((-47, 0, 1), (15, 3, 63), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # The vessel on its skirt: the lead-grey body with olive bands, the cream lining edge, the lid and its crown.
    m += cyl("y", cx, cz, 23, 3, 5, GUNMETAL)
    m += cyl("y", cx, cz, 22, 5, 44, STAINLESS, CHROME)
    for y in (11, 24, 37):
        m += cyl("y", cx, cz, 22.5, y, y + 2, OLIVE)
    m += cyl("y", cx, cz, 22.75, 43, 45, CERAMIC)
    m += cyl("y", cx, cz, 20, 45, 49, STAINLESS, CHROME)
    m += cyl("y", cx, cz, 13, 49, 51, GUNMETAL)
    # The deep bolted access plate with the activity window and the amber strip under it.
    m.append(box((cx - 9, 15, 10.5), (cx + 9, 35, 12.25), {"*": GUNMETAL, "north": STENCIL}))
    m.append(dial("north", (cx, 27, 9.75), 8, texture=PORTHOLE, body=CHROME))
    m.append(box((cx - 5, 18, 10), (cx + 5, 19, 10.5), STRIP))
    for x in (cx - 7.5, cx + 7.5):
        for y in (16.5, 33.5):
            m.append(box((x - 0.5, y - 0.5, 10), (x + 0.5, y + 0.5, 10.5), CHROME))
    # The control panel (the controller) at the front left: the gauge, the amber running lamp and the green batch lamp.
    m.append(box((3, 3, 1), (15.5, 16, 8), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (12, 11.5, 0.75), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (6.5, 11.5, 0.75), 2.5, texture=LAMP, body=GUNMETAL))
    m.append(dial("north", (6.5, 6, 0.75), 2, texture=MINT, body=GUNMETAL))
    # The lower product manifold: the acid outlet from the vessel's foot to its flange, banded in hydrochloric acid, with a
    # valve and a red handwheel.
    m += cyl("z", -24, 8, 2.25, 0.75, 16, RUBBER)
    m += cyl("z", -24, 8, 2.6, 3, 4, "hydrochloric_acid_still")
    m.append(box((-27, 5, 0), (-21, 11, 0.75), {"*": CHROME, "north": f"{FLANGE}!"}))
    m.append(box((-25.5, 10, 6), (-22.5, 11.5, 8), GUNMETAL))
    m += cyl("y", -24, 7, 0.5, 11.5, 12.5, CHROME)
    m += cyl("y", -24, 7, 2, 12.5, 13.25, HANDWHEEL)
    # The washed-ore chute from the vessel to the front, under a hazard hood.
    m.append(box((-11, 4, 1), (-5, 11, 13), GUNMETAL))
    m.append(box((-12, 4, 0), (-4, 11, 1), {"*": GUNMETAL, "north": MOUTH}))
    m.append(box((-12.5, 11, 0), (-3.5, 12, 6), HAZARD))
    # The stirring drive on the lid: a gearbox under the pulley and the motor lying towards the back.
    m.append(box((cx - 5, 51, cz - 5), (cx + 5, 58, cz + 5), GUNMETAL))
    m += cyl("z", cx, 54.5, 2.75, cz + 5, cz + 15, OLIVE, GUNMETAL)
    for z in (cz + 7, cz + 11):
        m += cyl("z", cx, 54.5, 3.1, z, z + 1, GUNMETAL)
    m += cyl("z", cx, 54.5, 3, cz + 15, cz + 16, GUNMETAL, GRILLE)
    # The ore hatch on the lid, its hazard-banded rim round the mouth.
    m += cyl("y", -8, 24, 3, 47, 62.5, GUNMETAL)
    m.append(box((-12.5, 62.5, 19.5), (-3.5, 63.5, 28.5), HAZARD))
    m.append(box((-11, 63.5, 21), (-5, 64, 27), {"*": CHROME, "up": MOUTH}))
    # The short closed absorption inlet on the lid, banded in hydrogen chloride, to its flange on top.
    m += cyl("y", -24, 40, 2.5, 47, 63.25, RUBBER)
    m += cyl("y", -24, 40, 2.85, 57, 58, "hydrogen_chloride_still")
    m.append(box((-27, 63.25, 37), (-21, 64, 43), {"*": CHROME, "up": f"{FLANGE}!"}))
    # The water inlet into the left side, banded in water, and the acid sight glass beside it.
    m += cyl("x", 8, 40, 2, 4, 15.25, RUBBER)
    m += cyl("x", 8, 40, 2.4, 11, 12, "sp_water")
    m.append(box((15.25, 5, 37), (16, 11, 43), {"*": CHROME, "east": f"{FLANGE}!"}))
    m.append(box((5.5, 8, 28.5), (7, 40, 33.5), GUNMETAL))
    m.append(box((7, 10, 29.5), (7.5, 38, 32.5), {"*": CHROME, "east": "hydrochloric_acid_still"}))
    # The junction box at the back right: its power socket, a hazard band and a conduit into the vessel.
    m.append(box((-47, 3, 50), (-38, 15, 62), GUNMETAL))
    m.append(box((-47.25, 15, 49.75), (-37.75, 16, 62.25), HAZARD))
    m.append(box((-47.75, 5, 53), (-47, 11, 59), {"*": CHROME, "west": "power_port!"}))
    m += cyl("x", 10, 51, 1.25, -38, -34, RUBBER)
    return m


def bulk_chemical_reactor_rotor():
    """The Bulk Chemical Reactor's stirring drive pulley on its gearbox (client/MachineRotors draws it): a spoked pulley
    and a drive key across it."""
    import machine_rotors as rotors
    cx, y, cz = BULK_REACTOR_DRIVE
    quads = rotors.disc("y", y - 1.25, y + 0.75, cx, cz, 3.75, COPPER, "dr_pulley")
    quads += rotors.prism("y", y + 0.75, y + 1.25, [(cx - 3.5, cz - 0.5), (cx + 3.5, cz - 0.5), (cx + 3.5, cz + 0.5),
                                                     (cx - 3.5, cz + 0.5)], CHROME, CHROME)
    return quads


def bulk_chemical_reactor_still():
    """The pulley standing still, for the item model only."""
    cx, y, cz = BULK_REACTOR_DRIVE
    return cyl("y", cx, cz, 3.75, y - 1.25, y + 0.75, COPPER, "dr_pulley")


# The Anaerobic Digester's mixer coupling on the back of its gearbox, turning about z: its centre (x, y, z).
DIGESTER_COUPLING = (-24, 16, 95.25)


def anaerobic_digester():
    """Six wide, six deep and six tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a factory
    landmark, a huge closed cylindrical tank of the owner's white tank panels on four broad foundation shoes, ringed by
    thick graphite compression rings, under a faceted domed cap with a raised upper ring. A ladder climbs the front to the
    ring. Across the front runs the sheltered feed gallery: a canopy on posts over the control panel (the controller,
    with its amber and green lamps), the digestate sight windows and the gas-status lamp. The capped feed auger enters
    the left side with its hopper, the water inlet low beside it; the low digestate chute leaves the right side; the dark
    gas header slopes from the dome to its flange high on the right. At the back, the heavy mixer gearbox with its motor
    and coupling (drawn turning by MachineRotors) and the junction box with the power socket. Each port is at the middle
    of its block's face."""
    cx, cz = -32, 48
    m = []
    # Four broad foundation shoes under the tank.
    for sx in (cx - 27, cx + 27):
        for sz in (cz - 27, cz + 27):
            m.append(box((sx - 7, 0, sz - 7), (sx + 7, 6, sz + 7), {"*": GUNMETAL, "up": STENCIL}))
    # The tank: white panels, graphite compression rings, the raised upper ring and the faceted dome.
    m += cyl("y", cx, cz, 42, 6, 76, TANK_BODY, TANK_TOP)
    for y in (12, 28, 44, 60):
        m += cyl("y", cx, cz, 42.75, y, y + 2.5, GUNMETAL)
    m += cyl("y", cx, cz, 43.5, 74, 77, GUNMETAL)
    m += cyl("y", cx, cz, 38, 76, 82, TANK_BODY, TANK_TOP)
    m += cyl("y", cx, cz, 30, 82, 86, TANK_BODY, TANK_TOP)
    m += cyl("y", cx, cz, 20, 86, 89, TANK_BODY, TANK_TOP)
    m += cyl("y", cx, cz, 8, 89, 91, GUNMETAL, CHROME)
    # The ladder up the front to the upper ring.
    for x in (-13.5, -8):
        m.append(box((x - 0.5, 6, 3.5), (x + 0.5, 74, 4.5), GUNMETAL))
    for y in range(10, 74, 5):
        m.append(box((-13, y, 3.75), (-8.5, y + 0.75, 4.25), CHROME))
    # The sheltered feed gallery: a canopy on posts across the front, the control panel under it at the left, the
    # digestate sight windows and the gas-status lamp on the tank.
    m.append(box((-60, 34, 0.5), (15, 35.5, 10), {"*": GUNMETAL, "north": HAZARD}))
    for x in (-58, 12):
        m.append(box((x, 0, 1), (x + 2, 34, 3), GUNMETAL))
    m.append(box((2, 0, 1), (11, 18, 8), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (6.5, 13.5, 0.75), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (4.5, 7, 0.75), 2.5, texture=LAMP, body=GUNMETAL))
    m.append(dial("north", (8.5, 7, 0.75), 2.5, texture=MINT, body=GUNMETAL))
    for y in (14, 24):
        m.append(dial("north", (cx - 6, y, 5.25), 6, texture=PORTHOLE, body=CHROME))
    m.append(box((cx + 1, 10, 5.25), (cx + 3, 30, 6.25), STRIP))
    m.append(dial("north", (cx, 52, 5.25), 3, texture=MINT, body=GUNMETAL))
    # The capped feed auger into the left side, its hopper on top and its cap at the port.
    m += cyl("x", 24, 40, 4, 6, 15.25, OLIVE, GUNMETAL)
    for x in (8, 12):
        m += cyl("x", 24, 40, 4.4, x, x + 1, GUNMETAL)
    m.append(box((7, 28, 35), (14, 33, 45), OLIVE))
    m.append(box((6.5, 33, 34.5), (14.5, 34, 45.5), HAZARD))
    m.append(box((7.5, 33.25, 35.5), (13.5, 33.5, 44.5), MOUTH))
    m.append(box((15.25, 19, 35), (16, 29, 45), {"*": CHROME, "east": MOUTH}))
    # The water inlet low on the left.
    m += cyl("x", 8, 72, 2, 6, 15.25, RUBBER)
    m += cyl("x", 8, 72, 2.4, 11, 12, "sp_water")
    m.append(box((15.25, 5, 69), (16, 11, 75), {"*": CHROME, "east": f"{FLANGE}!"}))
    # The low digestate chute out of the right side.
    m.append(box((-79.25, 3, 36), (-73, 13, 44), GUNMETAL))
    m.append(box((-80, 3, 35), (-79.25, 13, 45), {"*": GUNMETAL, "west": MOUTH}))
    m.append(box((-80, 13, 34.5), (-72, 14, 45.5), HAZARD))
    # The dark gas header from the dome to its flange high on the right, banded in biogas.
    m += cyl("x", 88, 72, 3, -79.25, -40, EXHAUST, GUNMETAL)
    m += cyl("x", 88, 72, 3.4, -72, -71, "biogas_still")
    m += cyl("y", -40, 72, 3, 80, 88, EXHAUST, GUNMETAL)
    m.append(box((-80, 84, 68), (-79.25, 92, 76), {"*": CHROME, "west": f"{FLANGE}!"}))
    # The heavy mixer gearbox at the back, its motor on top and the coupling's guard ring.
    m.append(box((-34, 6, 86), (-14, 26, 94.5), GUNMETAL))
    m.append(box((-34.25, 26, 85.75), (-13.75, 27, 94.75), HAZARD))
    m += cyl("z", -24, 33, 4, 80, 92, OLIVE, GUNMETAL)
    for z in (83, 87):
        m += cyl("z", -24, 33, 4.4, z, z + 1, GUNMETAL)
    m += cyl("z", -24, 33, 4.2, 92, 93, GUNMETAL, GRILLE)
    m += octagon_ring("z", -24, 16, 6, 5, 94.5, 95.75, CHROME)
    # The junction box at the back right: its power socket and a hazard band.
    m.append(box((-48, 6, 88), (-34, 18, 95.25), GUNMETAL))
    m.append(box((-48.25, 18, 87.75), (-33.75, 19, 95.5), HAZARD))
    m.append(box((-43, 5, 95.25), (-37, 11, 96), {"*": CHROME, "south": "power_port!"}))
    return m


def anaerobic_digester_rotor():
    """The Digester's mixer coupling on the back of its gearbox (client/MachineRotors draws it): a hub and four arms, so
    its slow turn reads from behind."""
    import machine_rotors as rotors
    cx, cy, z = DIGESTER_COUPLING
    quads = rotors.disc("z", z - 0.75, z + 0.5, cx, cy, 1.75, CHROME, CHROME)
    for arm in range(4):
        angle = math.radians(90 * arm)
        c, s_ = math.cos(angle), math.sin(angle)

        def at(r, w):
            return (cx + r * c - w * s_, cy + r * s_ + w * c)
        quads += rotors.prism("z", z - 0.5, z + 0.25, [at(1.9, -0.9), at(4.5, -0.9), at(4.5, 0.9), at(1.9, 0.9)], COPPER, COPPER)
    return quads


def anaerobic_digester_still():
    """The coupling standing still, for the item model only."""
    cx, cy, z = DIGESTER_COUPLING
    return cyl("z", cx, cy, 4.5, z - 0.5, z + 0.25, COPPER) + cyl("z", cx, cy, 1.75, z - 0.75, z + 0.5, CHROME)


# The Coal Gasifier's feed-drive pulley on its cabinet, turning about x: its centre (x, y, z).
GASIFIER_DRIVE = (15.5, 18, 43)
# The Gas Cleanup and Separation Rack's blower fan on the scrubber's crown, turning about y: its centre (x, y, z).
RACK_FAN = (-8, 53.5, 24)


def coal_gasifier():
    """Four wide, four deep and six tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a tall
    reaction column of blackened steel with cream refractory borders and copper pipe rings rises above a broad lower ash
    chamber on four large braced feet. The ash chamber's front carries the thick closed fire door, which glows while it
    works, the heat throat's slit beneath it and the control panel (the controller) at the front left. A sloped charging
    chute meets the column halfway up on the left, with the dark feed-drive cabinet below it (its pulley drawn turning by
    MachineRotors) and the water inlet low behind it. The capped top stack carries the short gas header to its flange high
    on the right, banded in raw syngas; the residue chute sits low on the ash chamber's right side and the power socket on a
    junction box at the back. Each port is at the middle of its block's face."""
    cx, cz = -16, 32
    m = []
    # Four large braced feet and the broad ash chamber, its refractory border at the top.
    for fx in (-40, 4):
        for fz in (8, 52):
            m.append(box((fx - 5, 0, fz - 5), (fx + 5, 6, fz + 5), {"*": GUNMETAL, "up": STENCIL}))
    m.append(box((-44, 6, 6), (12, 28, 58), GUNMETAL))
    m.append(box((-45, 28, 5), (13, 31, 59), REFRACTORY))
    # The reaction column, its refractory borders and copper pipe rings, and the capped stack.
    m += cyl("y", cx, cz, 16, 31, 84, GUNMETAL, CHROME)
    for y in (44, 64):
        m += cyl("y", cx, cz, 16.6, y, y + 2, REFRACTORY)
    for y in (38, 56, 74):
        m += cyl("y", cx, cz, 16.4, y, y + 1, COPPER)
    m += cyl("y", cx, cz, 12, 84, 88, GUNMETAL)
    m += cyl("y", cx, cz, 8, 88, 92, GUNMETAL, CHROME)
    m += cyl("y", cx, cz, 10, 92, 94, GUNMETAL, "dr_soot")
    # The residue chute low on the ash chamber's right side, its olive hood and hazard-banded mouth.
    m.append(box((-47.25, 4, 18), (-44, 15, 30), OLIVE))
    m.append(box((-47.75, 15, 17.5), (-43.5, 16, 30.5), HAZARD))
    m.append(box((-48, 5, 19), (-47.25, 13, 29), {"*": CHROME, "west": MOUTH}))
    # The thick closed fire door on the ash chamber, the heat throat's slit under it and a soot seam above.
    m.append(box((-28, 9, 4.5), (-4, 25, 6), {"*": GUNMETAL, "north": "dr_fire_door"}))
    m.append(box((-24, 7, 5.25), (-8, 8, 6), STRIP))
    m.append(box((-28, 26, 5.25), (-4, 27, 6), "dr_soot"))
    # The control panel (the controller) at the front left.
    m.append(box((2, 6, 1), (15.5, 20, 6), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (11.5, 15, 0.75), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (6, 15, 0.75), 2.5, texture=LAMP, body=GUNMETAL))
    m.append(dial("north", (6, 10, 0.75), 2, texture=MINT, body=GUNMETAL))
    # The sloped charging chute meeting the column halfway up on the left, its olive guard and hazard-banded mouth.
    m.append(box((-1, 34, 19), (8, 42, 29), GUNMETAL))
    m.append(box((8, 36, 18), (15.25, 46, 30), OLIVE))
    m.append(box((7.5, 46, 17.5), (15.75, 47, 30.5), HAZARD))
    m.append(box((15.25, 35, 19), (16, 45, 29), {"*": CHROME, "east": MOUTH}))
    # The dark feed-drive cabinet below the chute (its pulley turns while it feeds).
    m.append(box((4, 6, 36), (15, 26, 50), {"*": "dr_soot", "east": GUNMETAL}))
    # The water inlet low on the left.
    m += cyl("x", 8, 56, 2, 12, 15.25, RUBBER)
    m += cyl("x", 8, 56, 2.4, 13, 14, "sp_water")
    m.append(box((15.25, 5, 53), (16, 11, 59), {"*": CHROME, "east": f"{FLANGE}!"}))
    # The short gas header from the stack to its flange high on the right, banded in raw syngas.
    m += cyl("x", 88, 40, 3, -47.25, -20, EXHAUST, GUNMETAL)
    m += cyl("x", 88, 40, 3.4, -42, -41, "raw_syngas_still")
    for x in (-36, -28):
        m += cyl("x", 88, 40, 3.4, x, x + 1, COPPER)
    m.append(box((-48, 84, 36), (-47.25, 92, 44), {"*": CHROME, "west": f"{FLANGE}!"}))
    # The junction box at the back: its power socket and a hazard band.
    m.append(box((-31, 6, 58), (-17, 18, 63.25), GUNMETAL))
    m.append(box((-31.25, 18, 57.75), (-16.75, 19, 63.5), HAZARD))
    m.append(box((-27, 5, 63.25), (-21, 11, 64), {"*": CHROME, "south": "power_port!"}))
    return m


def coal_gasifier_rotor():
    """The Gasifier's feed-drive pulley on the side of its cabinet (client/MachineRotors draws it)."""
    import machine_rotors as rotors
    x, cy, cz = GASIFIER_DRIVE
    quads = rotors.disc("x", x - 0.5, x + 0.25, cy, cz, 4, COPPER, "dr_pulley")
    quads += rotors.prism("x", x + 0.25, x + 0.5, [(cy - 0.5, cz - 3.5), (cy + 0.5, cz - 3.5), (cy + 0.5, cz + 3.5),
                                                   (cy - 0.5, cz + 3.5)], CHROME, CHROME)
    return quads


def coal_gasifier_still():
    """The pulley standing still, for the item model only."""
    x, cy, cz = GASIFIER_DRIVE
    return cyl("x", cy, cz, 4, x - 0.5, x + 0.25, COPPER, "dr_pulley")


def gas_cleanup_rack():
    """Three wide, five deep and four tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a
    steel skid carrying a scrubber column at the front, its blower fan (drawn turning by MachineRotors) under a guard on
    its crown, and two separation towers at the back, joined by a pipe bridge: carbon monoxide on the right, hydrogen on
    the left, each banded in its gas's colour and rising to a flange on top. The raw gas enters low on the left through a
    valve; a residue drawer sits at the scrubber's foot; the control panel (the controller) stands at the front left and
    the power socket on a junction box at the back. Each port is at the middle of its block's face."""
    m = [box((-31, 0, 1), (15, 3, 79), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD})]
    # Frame posts at the corners with a top rail, olive guards.
    for x in (-30, 12):
        for z in (2, 76):
            m.append(box((x, 3, z), (x + 2, 52, z + 2), OLIVE))
    for z in (2, 76):
        m.append(box((-28, 50, z), (12, 52, z + 2), OLIVE))
    # The scrubber column at the front middle, its cream lining bands, crown and the blower fan's guard ring.
    m += cyl("y", -8, 24, 9, 3, 48, STAINLESS, CHROME)
    for y in (14, 32):
        m += cyl("y", -8, 24, 9.5, y, y + 2, CERAMIC)
    m += cyl("y", -8, 24, 6, 48, 52, GUNMETAL)
    m += octagon_ring("y", -8, 24, 6, 5, 52, 55, CHROME)
    # The residue drawer at the scrubber's foot.
    m.append(box((-13, 4, 13.5), (-3, 10, 15.5), {"*": GUNMETAL, "north": OLIVE}))
    m.append(box((-10, 6.5, 12.75), (-6, 7.5, 13.5), CHROME))
    # The two separation towers at the back: carbon monoxide on the right, hydrogen on the left.
    for x, gas in ((-24, "carbon_monoxide_still"), (8, "hydrogen_still")):
        m += cyl("y", x, 64, 6.5, 3, 58, STAINLESS, CHROME)
        for y in (12, 30, 48):
            m += cyl("y", x, 64, 7, y, y + 1.5, GUNMETAL)
        m += cyl("y", x, 64, 7, 40, 42, gas)
        m += cyl("y", x, 72, 2.5, 50, 63.25, RUBBER)
        m.append(box((x - 3, 63.25, 69), (x + 3, 64, 75), {"*": CHROME, "up": f"{FLANGE}!"}))
        m.append(box((x - 2.5, 50, 64), (x + 2.5, 54, 72), GUNMETAL))
    # The pipe bridge from the scrubber to both towers.
    m += cyl("z", -8, 44, 2, 30, 64, RUBBER)
    m += cyl("x", 44, 64, 2, -24, 8, RUBBER)
    # The raw gas inlet low on the left, its valve with a red handwheel.
    m += cyl("x", 8, 40, 2.25, -2, 15.25, RUBBER)
    m += cyl("x", 8, 40, 2.6, 11, 12, "raw_syngas_still")
    m.append(box((4, 10, 38.5), (7, 11.5, 41.5), GUNMETAL))
    m += cyl("y", 5.5, 40, 0.5, 11.5, 12.5, CHROME)
    m += cyl("y", 5.5, 40, 2, 12.5, 13.25, HANDWHEEL)
    m.append(box((15.25, 5, 37), (16, 11, 43), {"*": CHROME, "east": f"{FLANGE}!"}))
    m += cyl("z", -2, 8, 1.5, 24, 40, RUBBER)
    # The control panel (the controller) at the front left.
    m.append(box((3, 3, 1), (11, 16, 7), {"*": OLIVE, "north": GUNMETAL, "up": STENCIL}))
    m.append(dial("north", (7, 12, 0.75), 3.5, texture=GAUGE, body=CHROME))
    m.append(dial("north", (5, 6.5, 0.75), 2, texture=LAMP, body=GUNMETAL))
    m.append(dial("north", (9, 6.5, 0.75), 2, texture=MINT, body=GUNMETAL))
    # The junction box at the back: its power socket and a hazard band.
    m.append(box((-14, 3, 73), (-2, 14, 79.25), GUNMETAL))
    m.append(box((-14.25, 14, 72.75), (-1.75, 15, 79.5), HAZARD))
    m.append(box((-11, 5, 79.25), (-5, 11, 80), {"*": CHROME, "south": "power_port!"}))
    return m


def gas_cleanup_rack_rotor():
    """The rack's blower fan on the scrubber's crown (client/MachineRotors draws it): a hub and five blades."""
    import machine_rotors as rotors
    cx, y, cz = RACK_FAN
    quads = rotors.disc("y", y - 1, y + 1, cx, cz, 1.25, CHROME, CHROME)
    for blade in range(5):
        angle = math.radians(72 * blade)
        c, s_ = math.cos(angle), math.sin(angle)

        def at(r, w):
            return (cx + r * c - w * s_, cz + r * s_ + w * c)
        quads += rotors.prism("y", y - 0.5, y + 0.5, [at(1.6, -0.55), at(4.5, -1.5), at(4.5, 1.5), at(1.6, 0.55)], GUNMETAL, GUNMETAL)
    return quads


def gas_cleanup_rack_still():
    """The fan standing still, for the item model only."""
    cx, y, cz = RACK_FAN
    return cyl("y", cx, cz, 4.5, y - 0.5, y + 0.5, GUNMETAL, "dr_fan") + cyl("y", cx, cz, 1.25, y - 1, y + 1, CHROME)


# The Bio Generator's train axis (x, y) and where its turning parts sit along z: the coupling and the radiator fan.
BIO_AXIS = (-8, 19)
BIO_COUPLING_Z = (27, 32)
BIO_FAN_Z = 58.5


def bio_generator():
    """Three wide, four deep and three tall (the art brief, docs/features/industrial-machine-models-and-textures.md): a
    compact engine-generator on a thick gunmetal skid. At the front the ringed olive combustion body with a round bolted
    inspection cover, thick mounting bolts and pale ceramic heat shields on its flanks; at the front left the sheltered
    instrument panel (the controller) with its gauges and warm amber lamp. Behind it the guarded coupling (drawn turning
    by MachineRotors) and the ribbed generator housing, and at the back the radiator with its fan (also turning). Beside
    the train on the right the low fuel-handling box, with the liquid inlet low on its side and the capped gas inlet above
    it; on the left the terminal box with its power socket, mint lamp and copper terminals. A short black exhaust rises
    from the combustion body and folds back along the top to a rain cap."""
    cu, cv = BIO_AXIS
    m = [box((-32, 0, 0), (16, 4, 64), GUNMETAL)]
    for z in (6, 18, 36, 50):
        m.append(box((cu - 10, 4, z), (cu + 10, 8, z + 4), GUNMETAL))
    # The combustion body: olive, ringed in gunmetal, with ceramic heat shields on either flank.
    m += cyl("z", cu, cv, 11, 4, 26, OLIVE, GUNMETAL)
    for z in (8, 14, 20):
        m += cyl("z", cu, cv, 11.75, z, z + 1.5, GUNMETAL)
    for sign in (1, -1):
        lo, hi = sorted((cu + sign * 11.5, cu + sign * 12.5))
        m.append(box((lo, 14, 6), (hi, 25, 24), HEAT_SHIELD))
    # The round bolted inspection cover on its front, and four thick mounting bolts on the saddles.
    m += cyl("z", cu, cv, 7.5, 3, 4, CHROME, FLANGE_FACE)
    m += cyl("z", cu, cv, 3, 2.25, 3, GUNMETAL)
    for x in (cu - 8, cu + 8):
        for z in (7, 19):
            m.append(box((x - 1, 8, z), (x + 1, 9.5, z + 2), CHROME))
    # The guarded coupling: hazard-banded hoops on bars, the shaft turning inside (MachineRotors).
    z0, z1 = BIO_COUPLING_Z
    m += octagon_ring("z", cu, cv, 7.5, 6.5, z0 - 1, z0, HAZARD)
    m += octagon_ring("z", cu, cv, 7.5, 6.5, z1, z1 + 1, HAZARD)
    for su in (1, -1):
        for sv in (1, -1):
            m.append(box((cu + su * 5 - 0.5, cv + sv * 5 - 0.5, z0), (cu + su * 5 + 0.5, cv + sv * 5 + 0.5, z1), GUNMETAL))
    # The ribbed generator housing, a bearing boss at its front.
    m += cyl("z", cu, cv, 12, 33, 54, OLIVE, GUNMETAL)
    for z in (35, 39.5, 44, 48.5):
        m += cyl("z", cu, cv, 12.75, z, z + 1.5, RIBBED)
    m += cyl("z", cu, cv, 5, 32, 33, GUNMETAL)
    # The radiator at the back: a gunmetal frame with a grille, its fan turning behind a guard (MachineRotors).
    m.append(box((cu - 13, 4, 54), (cu + 13, 44, 57), {"*": GUNMETAL, "north": GRILLE, "south": GRILLE}))
    for lo, hi in (((cu - 13, 4, 61), (cu - 10.5, 44, 63)), ((cu + 10.5, 4, 61), (cu + 13, 44, 63)),
                   ((cu - 10.5, 4, 61), (cu + 10.5, 13.5, 63)), ((cu - 10.5, 36.5, 61), (cu + 10.5, 44, 63))):
        m.append(box(lo, hi, GUNMETAL))
    m.append(box((cu - 12, 6, 57), (cu + 12, 8, 61), GUNMETAL))
    m.append(box((cu - 12, 41, 57), (cu + 12, 43, 61), GUNMETAL))
    m += octagon_ring("z", cu, 25, 10, 8.5, 62.5, 63.5, CHROME)
    m.append(box((cu - 8.5, 24.5, 63.25), (cu + 8.5, 25.5, 63.75), CHROME))
    # The sheltered instrument panel (the controller) at the front left: gauges and the amber lamp under a hood.
    m.append(box((4, 4, 2), (15, 16, 9), {"*": OLIVE, "north": GUNMETAL}))
    m.append(box((3.5, 16, 0.75), (15.5, 17, 9.5), GUNMETAL))
    m.append(dial("north", (12, 12, 1.75), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (7.25, 12, 1.75), 3, texture=GAUGE, body=CHROME))
    m.append(dial("north", (9.5, 7, 1.75), 2.5, texture=LAMP, body=GUNMETAL))
    # The low fuel-handling box on the right: the liquid inlet low on its side (the port) banded in bioethanol colour, the
    # capped gas inlet above it for biogas, and a hose into the combustion body.
    m.append(box((-31, 4, 33), (-22, 18, 51), {"*": OLIVE, "up": STENCIL}))
    m.append(box((-31.75, 3.5, 36.5), (-31, 12.5, 45.5), {"*": CHROME, "west": f"{FLANGE_FACE}!"}))
    m += cyl("x", 8, 41, 2.5, -31, -30, "bioethanol_still")
    m += cyl("x", 15, 47.5, 1.75, -32, -31, RUBBER)
    m.append(box((-32.25, 13, 45.5), (-31.75, 17, 49.5), CHROME))
    m += cyl("z", -26.5, 14, 1.5, 22, 33, RUBBER)
    m.append(box((-27.75, 12.5, 20), (-19, 15.5, 23), RUBBER))
    # The terminal box on the left: the power socket on its side (the port), a mint lamp and copper terminals on ceramic.
    m.append(box((7, 4, 33), (15, 20, 47), GUNMETAL))
    m.append(box((6.75, 20, 32.75), (15.25, 21, 47.25), HAZARD))
    m.append(box((15, 5, 37), (15.75, 11, 43), {"*": CHROME, "east": "power_port!"}))
    m.append(dial("east", (15.75, 15.5, 40), 2.5, texture=MINT, body=CHROME))
    for z in (36, 44):
        m += cyl("y", 11, z, 1, 21, 24, INSULATOR)
        m.append(box((10.5, 24, z - 0.5), (11.5, 24.5, z + 0.5), COPPER))
    m.append(box((4, 13, 39), (7, 15, 41), RUBBER))
    # The short black exhaust: up out of the combustion body, folded back along the top to a rain cap.
    m.append(box((cu - 2, 29, 9), (cu + 2, 38, 13), EXHAUST))
    m.append(box((cu - 2, 34, 13), (cu + 2, 38, 44), EXHAUST))
    for z in (20, 32):
        m.append(box((cu - 2.25, 33.75, z), (cu + 2.25, 38.25, z + 1), CHROME))
    m.append(box((cu - 2.5, 38, 42), (cu + 2.5, 39, 46), {"*": CHROME, "up": EXHAUST}))
    return m


def bio_generator_coupling():
    """The Bio Generator's coupling shaft and flanges (client/MachineRotors)."""
    import machine_rotors as rotors
    cu, cv = BIO_AXIS
    z0, z1 = BIO_COUPLING_Z
    quads = rotors.disc("z", z0 - 1, z1 + 1, cu, cv, 1.75, CHROME, CHROME)
    quads += rotors.disc("z", z0 + 0.5, z0 + 2, cu, cv, 4.5, CHROME, "dr_flange")
    quads += rotors.disc("z", z1 - 2, z1 - 0.5, cu, cv, 4.5, CHROME, "dr_flange")
    return quads


def bio_generator_fan():
    """The radiator fan behind its guard (client/MachineRotors): a hub and six blades."""
    import machine_rotors as rotors
    cu = BIO_AXIS[0]
    cv, z = 25, BIO_FAN_Z
    quads = rotors.disc("z", z - 1.25, z + 1.25, cu, cv, 1.5, CHROME, CHROME)
    for blade in range(6):
        angle = math.radians(60 * blade + 10)
        c, s_ = math.cos(angle), math.sin(angle)

        def at(r, w):
            return (cu + r * c - w * s_, cv + r * s_ + w * c)
        quads += rotors.prism("z", z - 0.5, z + 0.5, [at(2, -0.75), at(7.5, -1.75), at(7.5, 1.75), at(2, 0.75)], GUNMETAL, GUNMETAL)
    return quads


def bio_generator_still():
    """The turning parts standing still, for the item model only."""
    cu, cv = BIO_AXIS
    z0, z1 = BIO_COUPLING_Z
    return (cyl("z", cu, cv, 1.75, z0 - 1, z1 + 1, CHROME) + cyl("z", cu, cv, 4.5, z0 + 0.5, z0 + 2, CHROME, "dr_flange")
            + cyl("z", cu, cv, 4.5, z1 - 2, z1 - 0.5, CHROME, "dr_flange")
            + cyl("z", cu, 25, 7.5, BIO_FAN_Z - 0.5, BIO_FAN_Z + 0.5, GUNMETAL, "dr_fan"))


# Turning parts (tools/machine_rotors.py writes them for client/MachineRotors): on the train's axis, spinning up over
# two seconds when the machine starts and running down as long when it stops, standing still while idle.
ROTORS = {
    "gas_burning_generator_train": {
        "block": "gas_burning_generator", "axis": "z", "center": [-16, 26, 24], "property": "lit", "speed": 12,
        "ease": 40, "always": True, "quads": gas_burning_generator_rotor(),
    },
    # The Infuser's cooling fan, running while it works.
    "chemical_infuser_fan": {
        "block": "chemical_infuser", "axis": "y", "center": list(INFUSER_FAN), "property": "lit", "speed": 18,
        "ease": 30, "always": True, "quads": chemical_infuser_rotor(),
    },
    # The Oxidizer's feed screw, turning while it burns.
    "chemical_oxidizer_screw": {
        "block": "chemical_oxidizer", "axis": "z", "center": list(OXIDIZER_SCREW), "property": "lit", "speed": 9,
        "ease": 20, "always": True, "quads": chemical_oxidizer_rotor(),
    },
    # The Fermenter's agitator drive, turning slowly while a batch works.
    "industrial_fermenter_drive": {
        "block": "industrial_fermenter", "axis": "y", "center": list(FERMENTER_DRIVE), "property": "lit", "speed": 4,
        "ease": 30, "always": True, "quads": industrial_fermenter_rotor(),
    },
    # The Bulk Chemical Reactor's stirring drive, turning while a batch works.
    "bulk_chemical_reactor_drive": {
        "block": "bulk_chemical_reactor", "axis": "y", "center": list(BULK_REACTOR_DRIVE), "property": "lit", "speed": 6,
        "ease": 30, "always": True, "quads": bulk_chemical_reactor_rotor(),
    },
    # The Digester's mixer coupling, turning slowly while it digests.
    "anaerobic_digester_coupling": {
        "block": "anaerobic_digester", "axis": "z", "center": list(DIGESTER_COUPLING), "property": "lit", "speed": 3,
        "ease": 40, "always": True, "quads": anaerobic_digester_rotor(),
    },
    # The Gasifier's feed drive, turning while it feeds.
    "coal_gasifier_drive": {
        "block": "coal_gasifier", "axis": "x", "center": list(GASIFIER_DRIVE), "property": "lit", "speed": 6,
        "ease": 30, "always": True, "quads": coal_gasifier_rotor(),
    },
    # The rack's blower fan, running while it cleans.
    "gas_cleanup_rack_fan": {
        "block": "gas_cleanup_rack", "axis": "y", "center": list(RACK_FAN), "property": "lit", "speed": 18,
        "ease": 30, "always": True, "quads": gas_cleanup_rack_rotor(),
    },
    # The Bio Generator's coupling and radiator fan, on two axes along z, spinning up as it starts.
    "bio_generator_coupling": {
        "block": "bio_generator", "axis": "z", "center": [BIO_AXIS[0], BIO_AXIS[1], 29.5], "property": "lit", "speed": 12,
        "ease": 40, "always": True, "quads": bio_generator_coupling(),
    },
    "bio_generator_fan": {
        "block": "bio_generator", "axis": "z", "center": [BIO_AXIS[0], 25, BIO_FAN_Z], "property": "lit", "speed": 18,
        "ease": 40, "always": True, "quads": bio_generator_fan(),
    },
}

MODELS = {"electrolytic_separator": electrolytic_separator(), "gas_burning_generator": gas_burning_generator(),
          "chemical_infuser": chemical_infuser(), "chemical_oxidizer": chemical_oxidizer(),
          "industrial_fermenter": industrial_fermenter(), "bulk_chemical_reactor": bulk_chemical_reactor(),
          "anaerobic_digester": anaerobic_digester(), "coal_gasifier": coal_gasifier(),
          "gas_cleanup_rack": gas_cleanup_rack(), "bio_generator": bio_generator()}
# Extra elements for a form's item model alone: its turning parts, standing still.
ITEM_EXTRAS = {"gas_burning_generator": gas_burning_generator_still(), "chemical_infuser": chemical_infuser_still(),
               "chemical_oxidizer": chemical_oxidizer_still(), "industrial_fermenter": industrial_fermenter_still(),
               "bulk_chemical_reactor": bulk_chemical_reactor_still(), "anaerobic_digester": anaerobic_digester_still(),
               "coal_gasifier": coal_gasifier_still(), "gas_cleanup_rack": gas_cleanup_rack_still(),
               "bio_generator": bio_generator_still()}
PARTICLE = "dp_olive"


def write_all(write, assets, data, lang, condition, self_drop):
    """Blockstates, sliced part models, item models, names, loot, the construction recipe and the form recipes."""
    out = model_writer.StyleWriter(assets)
    for role, name in TANK_NAMES.items():
        lang[f"container.{MOD}.form.tank.{role}"] = name
    lang[f"container.{MOD}.form.socket.bed"] = "Catalyst bed"
    # The Infuser's bed socket holds the nickel catalyst bed: the owner's selected one nickel ingot on two ordinary fired
    # bricks (docs/features/industrial-starter-gas-and-acid-factory.md), made once and never used up.
    write(data / "tags" / "item" / f"{CATALYST_BEDS}.json", {"values": [f"{MOD}:nickel_catalyst_bed", f"{MOD}:vanadium_contact_bed"]})
    # The vanadium contact bed: the owner's selected reusable vanadium-oxide/ceramic bed, its oxide on two fired bricks
    # like the nickel bed, made once and never used up.
    write(data / "recipe" / "vanadium_contact_bed.json", {
        "fabric:load_conditions": condition("machines"),
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["BVB"],
        "key": {"B": "minecraft:brick", "V": f"{MOD}:vanadium_pentoxide"}, "result": {"id": f"{MOD}:vanadium_contact_bed", "count": 1}})
    # The Coal Gasifier's mixed residue stabilized into road and filler stock with a paid binder (the plan's selected
    # modest product; this change's quantities): in place of gravel, with cement and water into concrete, or with
    # asphalt binder into road asphalt, the same yields as the gravel recipes.
    write(data / "recipe" / "concrete_from_gasification_residue.json", {
        "fabric:load_conditions": condition("machines"),
        "type": "minecraft:crafting_shaped", "category": "building", "pattern": ["CRC", "RWR", "CRC"],
        "key": {"C": f"{MOD}:cement", "R": f"{MOD}:gasification_residue", "W": "minecraft:water_bucket"},
        "result": {"id": f"{MOD}:concrete", "count": 8}})
    write(data / "recipe" / "asphalt_from_gasification_residue.json", {
        "fabric:load_conditions": condition("machines") + condition("crude_oil"),
        "type": "minecraft:crafting_shaped", "category": "building", "pattern": ["RRR", "RBR", "RRR"],
        "key": {"R": f"{MOD}:gasification_residue", "B": f"{MOD}:asphalt_binder"},
        "result": {"id": f"{MOD}:asphalt", "count": 8}})
    # What the Anaerobic Digester takes: the fermentable crops and farm and kitchen waste.
    write(data / "tags" / "item" / f"{DIGESTIBLE}.json", {"values": [
        f"#{MOD}:fermentable", "minecraft:rotten_flesh", "minecraft:poisonous_potato", "minecraft:kelp",
        "minecraft:pumpkin", "minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds",
        "minecraft:pumpkin_seeds"]})
    write(data / "recipe" / "nickel_catalyst_bed.json", {
        "fabric:load_conditions": condition("machines"),
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["BNB"],
        "key": {"B": "minecraft:brick", "N": "#c:ingots/nickel"}, "result": {"id": f"{MOD}:nickel_catalyst_bed", "count": 1}})
    for form, info in FORMS.items():
        lang[f"block.{MOD}.{form}"] = info["display"]
        from machine_connections import with_sockets, form_ports
        elements = with_sockets(MODELS[form], form_ports(info))
        item_elements = elements + ITEM_EXTRAS.get(form, [])
        textures = {name: f"{MOD}:block/{name}" for name in model_writer.texture_names(item_elements)}
        textures["particle"] = f"{MOD}:block/{PARTICLE}"
        glow = {name: f"{MOD}:block/{FORM_GLOW[name]}" for name in model_writer.texture_names(elements) if name in FORM_GLOW}
        offsets = footprint(info["layers"])
        for index, part in enumerate(model_writer.split_model(form, elements, offsets)):
            # A block wholly inside a vessel keeps none of its faces; Minecraft refuses a faceless element.
            part = [element for element in part if element["faces"]]
            out.model(f"{form}_part{index}", {"ambientocclusion": False, "textures": textures, "elements": part})
            out.model(f"{form}_part{index}_on", {"parent": f"{MOD}:block/{form}_part{index}", "textures": glow})
        variants = {}
        for facing, y in model_writer.FACING_Y.items():
            rotation = {"y": y} if y else {}
            for lit in ("false", "true"):
                for part in range(len(offsets)):
                    on = "_on" if lit == "true" else ""
                    variants[f"facing={facing},lit={lit},part={part}"] = {"model": f"{MOD}:block/{form}_part{part}{on}",
                                                                          **rotation}
        out.blockstate(form, {"variants": variants})
        out.item_model(form, {"parent": "minecraft:block/block", "textures": textures,
                              "elements": model_writer.scaled_elements(item_elements)})
        out.item(form, f"{MOD}:item/{form}")
        write(data / "loot_table" / "blocks" / f"{form}.json", self_drop(form))
        pattern, key = info["recipe"]
        write(data / "recipe" / f"{form}.json", {
            "fabric:load_conditions": [c for f in info["features"] for c in condition(f)],
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
            "result": {"id": f"{MOD}:{form}", "count": 1}})
    for name, info in FORMS.items():
        if "alternate_recipe" in info and info["alternate_recipe"] != info["recipe"]:
            pattern, key = info["alternate_recipe"]
            write(data / "recipe" / f"expanded_{name}.json", {
                "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped",
                "category": "misc", "pattern": pattern, "key": key,
                "result": {"id": f"{MOD}:{name}", "count": 1}})
    machine_consolidation.write_labels(lang, FORMS)
    import starter_chemistry
    starter_chemistry.write_extras(write, assets, data, lang, condition)
    import anaerobic
    anaerobic.write_extras(write, assets, data, lang, condition)
    biorefinery.write_extras(write, assets, data, lang, condition)
    coastal_metals.write_extras(write, assets, data, lang, condition)
    polymers.write_extras(write, assets, data, lang, condition)
    fluorochemistry.write_extras(write, assets, data, lang, condition)
    for kind, name, recipe in form_recipe_files(condition):
        write(data / "recipe" / kind / f"{name}.json", recipe)


def form_recipe_files(condition):
    """(recipe type, file name, JSON) for every form recipe: a family fluid recipe with its capability."""
    import petro
    for form, recipes in FORM_RECIPES.items():
        kind = petro.FLUID_MACHINES[FORMS[form]["family"]]["recipe_type"]
        for recipe in recipes:
            data = {"fabric:load_conditions": [c for f in recipe["features"] for c in condition(f)],
                    "type": f"{MOD}:{kind}", "capability": recipe["capability"]}
            if recipe.get("legacy_vanadium"):
                data["fabric:load_conditions"].append({"condition": "jugcraft:vanadium_legacy_iron"})
            if recipe.get("items"):
                data["items"] = [{"ingredient": item, "count": count} for item, count in recipe["items"]]
            if recipe.get("fluids"):
                data["fluids"] = [{"fluid": fluid, "amount": mb} for fluid, mb in recipe["fluids"]]
            if recipe.get("fluid_results"):
                data["fluid_results"] = [{"fluid": r[0], "amount": r[1]} | ({"tank": r[2]} if len(r) > 2 else {})
                                         for r in recipe["fluid_results"]]
            if recipe.get("results"):
                data["results"] = [{"id": item, "count": count} for item, count in recipe["results"]]
            for field in ("tool", "energy_per_tick", "emissions", "priority"):
                if field in recipe:
                    data[field] = recipe[field]
            data["time"] = recipe["ticks"]
            yield kind, recipe["name"], data


def recipes_for_form(form):
    """Resolve shared family/capability receipts without writing duplicate recipe IDs."""
    info=FORMS[form]
    return [recipe for owner,recipes in FORM_RECIPES.items() if FORMS[owner]['family']==info['family']
            for recipe in recipes if recipe['capability'] in info['capabilities']]


def form_energy(form, power):
    import machines
    power = power or machines.STATS[FORMS[form]['family']]['use_per_tick']
    percent={'ENTRY':100,'EXPANDED':90,'BULK':80}[FORMS[form]['profile']]
    return max(1,(power*percent+99)//100) if power else 0


def recipe_view():
    """The forms' JEI categories (tools/recipe_view.py): one per form that runs recipes, its recipes and the form as the
    station. A generator form runs none (its fuels are in the Engineer's Handbook, like every generator's)."""
    return [{"block": f"{MOD}:{form}", "type": form,
             "recipes": [{"items": [[ref, count] for ref, count in recipe.get("items", [])],
                          "fluids": [[fluid, mb] for fluid, mb in recipe.get("fluids", [])],
                          "fluid_results": [[r[0], r[1]] for r in recipe.get("fluid_results", [])],
                          "results": [[item, count] for item, count in recipe.get("results", [])],
                          "ticks": recipe["ticks"], "tool": recipe.get("tool", ""), "energy_per_tick": form_energy(form,recipe.get("energy_per_tick", 0))} for recipe in recipes_for_form(form)]}
            for form in FORMS if recipes_for_form(form)]


# Preserve released forms while adding the owner-tested expansion below.
from copy import deepcopy
_RELEASED = {key: deepcopy(value) for key, value in dict(forms=FORMS, recipes=FORM_RECIPES,
    handbook=HANDBOOK, models=MODELS, rotors=ROTORS, extras=ITEM_EXTRAS).items()}

# Package 2's four further entry forms. Keep each physical machine in the common controller/renderer pipeline.
import starter_chemistry
import starter_chemistry_models
FORMS.update(starter_chemistry.FORMS)
FORM_RECIPES.update(starter_chemistry.RECIPES)
HANDBOOK.update(starter_chemistry.HANDBOOK)
TANK_NAMES.update(starter_chemistry.TANK_NAMES)
MODELS.update(starter_chemistry_models.MODELS)
ROTORS.update(starter_chemistry_models.ROTORS)
ITEM_EXTRAS.update(starter_chemistry_models.ITEM_EXTRAS)
HANDBOOK["gas_burning_generator"][-1] = "Methane from the Infuser now burns here too: 448 JE per mB, up to 256 JE/t. Fractional fuel credit is saved, so switching loads or restarting loses and duplicates no fuel. This generator has no upgrade slots."

import anaerobic
import anaerobic_models
FORMS.update(anaerobic.FORMS)
FORM_RECIPES.update(anaerobic.RECIPES)
FORM_RECIPES["chemical_infuser"].append(anaerobic.UPGRADING)
HANDBOOK.update(anaerobic.HANDBOOK)
TANK_NAMES.update(anaerobic.TANK_NAMES)
MODELS.update(anaerobic_models.MODELS)
ROTORS.update(anaerobic_models.ROTORS)
ITEM_EXTRAS.update(anaerobic_models.ITEM_EXTRAS)

import biorefinery
import biorefinery_models
FORMS.update(biorefinery.FORMS)
FORM_RECIPES.update(biorefinery.RECIPES)
for name, recipes in biorefinery.EXTRA_RECIPES.items(): FORM_RECIPES[name].extend(recipes)
HANDBOOK.update(biorefinery.HANDBOOK)
TANK_NAMES.update(biorefinery.TANK_NAMES)
MODELS.update(biorefinery_models.MODELS)
ROTORS.update(biorefinery_models.ROTORS)
ITEM_EXTRAS.update(biorefinery_models.ITEM_EXTRAS)

import coastal_metals
import coastal_metals_models
FORMS.update(coastal_metals.FORMS)
FORM_RECIPES.update({name:list(recipes) for name,recipes in coastal_metals.RECIPES.items()})
for name, recipes in coastal_metals.EXTRA_RECIPES.items(): FORM_RECIPES[name].extend(recipes)
HANDBOOK.update(coastal_metals.HANDBOOK)
TANK_NAMES.update(coastal_metals.TANK_NAMES)
MODELS.update(coastal_metals_models.MODELS)
ROTORS.update(coastal_metals_models.ROTORS)
ITEM_EXTRAS.update(coastal_metals_models.ITEM_EXTRAS)

HANDBOOK['biogas_cleaner'].append('Also cleans wet fermentation CO2: 250 mB gas + 250 mB wash water -> 250 mB usable CO2 + 250 mB recovered water, 100 ticks at 24 JE/t. Drain both previous products before switching between biogas and fermentation gas. No new disposable filter or separate gas-cleaning machine is needed.')

import polymers
import polymer_models
FORMS.update(polymers.FORMS)
FORM_RECIPES.update({name:list(recipes) for name,recipes in polymers.RECIPES.items()})
HANDBOOK.update(polymers.HANDBOOK)
TANK_NAMES.update(polymers.TANK_NAMES)
MODELS.update(polymer_models.MODELS)
ROTORS.update(polymer_models.ROTORS)
ITEM_EXTRAS.update(polymer_models.ITEM_EXTRAS)

import fluorochemistry
import fluorochemistry_models
FORMS.update(fluorochemistry.FORMS)
FORM_RECIPES.update(fluorochemistry.RECIPES)
for machine, recipes in fluorochemistry.EXTRA_RECIPES.items():
    FORM_RECIPES[machine].extend(recipes)
HANDBOOK.update(fluorochemistry.HANDBOOK)
TANK_NAMES.update(fluorochemistry.TANK_NAMES)
MODELS.update(fluorochemistry_models.MODELS)
ROTORS.update(fluorochemistry_models.ROTORS)
ITEM_EXTRAS.update(fluorochemistry_models.ITEM_EXTRAS)

# Owner-requested consolidation: retain IDs/recipes and extend shared capabilities.
import machine_consolidation
machine_consolidation.apply(FORMS, HANDBOOK)

# Shared IDs keep main's footprint, slots, port positions and tank roles. The expansion
# adds recipes and explicitly aliased tanks rather than replacing existing factories.
import machine_expansion_compat
machine_expansion_compat.apply(_RELEASED, FORMS, FORM_RECIPES, HANDBOOK, MODELS, ROTORS, ITEM_EXTRAS)
