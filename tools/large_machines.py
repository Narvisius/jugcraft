"""Multi-block machine models (single source of truth for their shapes).

Each machine is authored as ONE model in structure coordinates, facing north (front = -z),
in pixels, with the master block at 0..16 on every axis. The footprint lists the other blocks
it fills, as block offsets, matching MachineKind.footprint() in Java.

generate_material_data.py slices the model into one model per block (like Immersive
Engineering's split models): every element is clipped to each block it overlaps. Elements that
reach outside the footprint (wind turbine blades, exhaust caps) are kept whole on the part that
holds their center, so a part model may reach up to one block past its own space.

Overlapping boxes must not share a visible face plane (it flickers in game): make one a
hair shorter, as the octagonal tanks do.

An element is (from, to, texture) where texture is one texture name for all faces or a dict
{face: texture} with "*" as the default. Texture names are jugcraft block textures.
"""

def cuboid(width, height, depth):
    """Footprint.cuboid in Java: width blocks to the right (-x), height up, depth back (+z); bottom layer first,
    front row first, right to left along each row. Part 0 is the front left bottom block."""
    return [(-x, y, z) for y in range(height) for z in range(depth) for x in range(width)]


# Offsets (x east, y up, z south) of each part; part 0 is the master. Keep in sync with Java.
FOOTPRINTS = {
    # The lava tank stands to the right of the generator body, seen from the front.
    "geothermal_generator": cuboid(2, 2, 2),
    "wind_turbine": [(0, y, 0) for y in range(9)],
    # Three wide, six tall, two deep: the furnace column (left) and a 2x2 crucible tank tower (right).
    "alloy_smelter": cuboid(3, 6, 2),
    # Steel tier: a two-block brick oven and a three-block foundry stack.
    # A 2x2 beehive two blocks high with its chimney in one block on top (part 8), and a 2x2x5 blast furnace.
    "coke_oven": cuboid(2, 2, 2) + [(0, 2, 0)],
    "steel_foundry": cuboid(2, 5, 2),
    # Storage: a 2x2 capacitor bank (two wide, two tall) and a squat 2x2 steel tank (two wide, two deep).
    "capacitor_bank": [(0, 0, 0), (-1, 0, 0), (0, 1, 0), (-1, 1, 0)],
    "steel_tank": [(0, 0, 0), (-1, 0, 0), (0, 0, 1), (-1, 0, 1)],
    # Mining: a two-block derrick over the drilled column.
    "ore_drill": [(0, 0, 0), (0, 1, 0)],
    # Renewables: a two-block water wheel house (the wheel turns in the water column on its right).
    "water_wheel": [(0, 0, 0), (0, 1, 0)],
    # Kinetic: a 2x2x2 steam engine; its shaft comes out of the back of part 7 (upper right back).
    "large_steam_engine": cuboid(2, 2, 2),
    # Petrochemistry: a pumpjack one wide, three tall, three long (wellhead at the front).
    "pumpjack": cuboid(1, 3, 3),
    "oil_sand_extractor": cuboid(2, 2, 2),
    "distillation_tower": cuboid(2, 7, 2),
    "catalytic_cracker": cuboid(2, 4, 2),
    "vacuum_distillation_unit": cuboid(2, 3, 2),
    "catalytic_reformer": cuboid(3, 2, 2),
    "chemical_mixer": cuboid(2, 2, 2),
    "fracking_rig": cuboid(3, 5, 3),
    "flowback_treatment_unit": cuboid(3, 1, 2),
    "diesel_generator": cuboid(3, 2, 2),
    "gas_turbine": cuboid(4, 2, 2),
    "polymerization_reactor": cuboid(2, 3, 2),
    "diesel_engine": cuboid(2, 2, 3),
    "electrolytic_cell": cuboid(3, 3, 2),
    "chemical_reactor": cuboid(2, 2, 2),
    # Storage: a lithium battery bank three wide, two tall and one deep.
    "lithium_battery_bank": cuboid(3, 2, 1),
    # Electronics: a two-block crystal grower.
    "crystal_grower": [(0, 0, 0), (0, 1, 0)],
    "lithography_station": cuboid(3, 2, 2),
    # Fluid logistics: a 3x3x3 gas holder.
    "gas_holder": cuboid(3, 3, 3),
    # Farming: a two-block crop harvester.
    "crop_harvester": [(0, 0, 0), (0, 1, 0)],
}

# Machines that take power at one marked socket only: (part, face) for a north-facing machine.
# Keep in sync with MachineKind.powerPort(). Everything else takes power on any face.
POWER_PORTS = {
    "alloy_smelter": (2, "west"),
}

PIPE = "bronze_fluid_pipe"
STEEL = "machine_side"
TOP = "machine_top"

MODELS = {
    "geothermal_generator": [
        # Generator house (left column, 2 deep, 2 tall) and twin lava tanks (right column).
        ((-16, 0, 0), (16, 2, 32), "geothermal_plinth"),
        ((1, 2, 1), (15, 22, 31), {"*": STEEL, "north": "#front", "up": TOP}),
        ((0, 22, 0), (16, 23, 32), "geothermal_plinth"),
        ((9, 23, 20), (13, 32, 24), "geothermal_stack"),
        ((-4, 12, 6), (1, 16, 10), PIPE),
        ((-4, 12, 22), (1, 16, 26), PIPE),
        ((-14, 2, 3), (-2, 28, 13), {"*": "geothermal_tank", "up": "fluid_tank_top"}),
        ((-14, 2, 19), (-2, 28, 29), {"*": "geothermal_tank", "up": "fluid_tank_top"}),
    ],
    "wind_turbine": [
        # Base housing (master block).
        ((0, 0, 0), (16, 3, 16), "wind_turbine_base"),
        ((2, 3, 2), (14, 14, 14), {"*": STEEL, "north": "#front", "up": TOP}),
        ((3, 14, 3), (13, 16, 13), "wind_turbine_base"),
        # A tall mast with collars every two blocks, and the nacelle and tail fin on top; the rotor is
        # drawn by the client renderer.
        ((6, 16, 6), (10, 132, 10), "wind_turbine_mast"),
        ((5, 48, 5), (11, 49, 11), "wind_turbine_base"),
        ((5, 80, 5), (11, 81, 11), "wind_turbine_base"),
        ((5, 112, 5), (11, 113, 11), "wind_turbine_base"),
        ((4, 132, 2), (12, 140, 16), "wind_turbine_nacelle"),
        ((6, 134, 0), (10, 138, 2), "wind_turbine_nacelle"),
        ((7.5, 130, 16), (8.5, 142, 26), "wind_turbine_blade"),
    ],
}

MODELS["alloy_smelter"] = [
    ((-32, 0, 0), (16, 2, 32), "heavy_plinth"),
    # Firebrick furnace with the crucible window at the front (master column), a funnel on top.
    ((1, 2, 1), (15, 26, 31), {"*": "alloy_smelter_brick", "north": "#front", "up": TOP}),
    ((3, 26, 11), (13, 32, 21), "alloy_hopper"),
    ((1, 32, 9), (15, 37, 23), {"*": "alloy_hopper", "up": "alloy_hopper_top"}),
    # The crucible tank (2x2, right) on a base with the copper power socket on its outer side.
    ((-31, 2, 1), (-1, 10, 31), {"*": STEEL, "up": TOP}),
    ((-32, 3, 12), (-31, 11, 20), "power_port_frame"),
    ((-32.5, 5.5, 14.5), (-32, 8.5, 17.5), "power_port"),
    ((-29, 10, 5), (-3, 80, 27), {"*": "alloy_crucible", "up": "alloy_crucible"}),
    ((-27, 10, 3), (-5, 79.9, 29), {"*": "alloy_crucible", "up": "alloy_crucible"}),
    ((-24, 80, 8), (-8, 88, 24), "bronze_block"),
    ((-18, 88, 14), (-14, 96, 18), "geothermal_stack"),
    # Pour pipe into the funnel.
    ((-4, 40, 14), (8, 43, 18), PIPE),
]

MODELS["coke_oven"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    # Brick oven body (2x2) with the fire door at the front, a stepped dome and the chimney block on top.
    ((-15, 2, 1), (15, 20, 31), {"*": "alloy_smelter_brick", "north": "#front", "up": "alloy_smelter_brick"}),
    ((-12, 20, 4), (12, 26, 28), "alloy_smelter_brick"),
    ((-8, 26, 8), (8, 31, 24), "alloy_smelter_brick"),
    ((-2.5, 31, 13.5), (2.5, 48, 18.5), "geothermal_stack"),
]

MODELS["steel_foundry"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    # Hearth with the tapping door (2x2), then a tapering brick shaft three blocks high and a charging hopper.
    ((-15, 2, 1), (15, 28, 31), {"*": "alloy_smelter_brick", "north": "#front", "up": "alloy_smelter_brick"}),
    ((-13, 28, 3), (13, 50, 29), "alloy_smelter_brick"),
    ((-11, 50, 5), (11, 70, 27), "alloy_smelter_brick"),
    ((-16, 26, 0), (16, 27, 32), "heavy_plinth"),
    ((-6, 70, 10), (6, 78, 22), {"*": "alloy_hopper", "up": "alloy_hopper_top"}),
]

MODELS["capacitor_bank"] = [
    ((-16, 0, 0), (16, 2, 16), "heavy_plinth"),
    ((-15, 2, 1), (15, 30, 15), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-16, 30, 0), (16, 32, 16), "heavy_plinth"),
    ((-14, 8, 0.5), (14, 9, 1), "power_port_frame"),
]

MODELS["steel_tank"] = [
    ((-16, 0, 0), (16, 1, 32), "heavy_plinth"),
    ((-15, 1, 1), (15, 15, 31), {"*": "fluid_tank_side", "north": "#front", "up": "fluid_tank_top"}),
    ((-12, 15, 4), (12, 17, 28), "fluid_tank_top"),
]

MODELS["ore_drill"] = [
    ((0, 0, 0), (16, 2, 16), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 15), {"*": STEEL, "north": "#front", "up": TOP}),
    ((3, 12, 3), (13, 26, 13), "wind_turbine_mast"),
    ((6, 26, 6), (10, 32, 10), "geothermal_stack"),
]

MODELS["water_wheel"] = [
    ((0, 0, 0), (16, 2, 16), "heavy_plinth"),
    ((1, 2, 1), (15, 20, 15), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-4, 4, -4), (-1, 28, 20), "wind_turbine_blade"),
]

MODELS["large_steam_engine"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 14, 31), {"*": STEEL, "north": "#front", "up": TOP}),
    ((2, 14, 2), (14, 26, 30), "fluid_tank_side"),
    ((-15, 2, 1), (-1, 7, 31), {"*": STEEL, "up": TOP}),
    ((-12, 8, 3), (-4, 14, 15), "geothermal_stack"),
    ((-15, 16, 21), (-1, 32, 23), "wind_turbine_blade"),
    ((-9.5, 22.5, 16), (-6.5, 25.5, 32), PIPE),
]

MODELS["pumpjack"] = [
    ((0, 0, 0), (16, 2, 48), "heavy_plinth"),
    ((3, 2, 2), (13, 12, 14), {"*": STEEL, "north": "#front", "up": TOP}),
    ((3, 2, 18), (13, 34, 30), "wind_turbine_mast"),
    ((5, 34, 2), (11, 40, 44), "geothermal_stack"),
    ((3, 2, 33), (13, 22, 46), {"*": STEEL, "up": TOP}),
    ((7, 12, 6), (9, 34, 8), PIPE),
]

MODELS["oil_sand_extractor"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((-12, 2, 4), (12, 26, 28), "fluid_tank_side"),
    ((-12, 26, 4), (12, 28, 28), "fluid_tank_top"),
    ((-13, 2, 1), (-3, 12, 3), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-15, 2, 25), (-5, 14, 31), {"*": STEEL, "up": TOP}),
    ((-9.5, 14, 27), (-6.5, 30, 30), "geothermal_stack"),
]

MODELS["distillation_tower"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 14, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-11, 2, 5), (11, 106, 27), "fluid_tank_side"),
    ((-11, 106, 5), (11, 108, 27), "fluid_tank_top"),
    ((-13, 7, 0), (-7, 11, 5), PIPE),
    ((-13, 38, 0), (-7, 42, 5), PIPE),
    ((-13, 70, 0), (-7, 74, 5), PIPE),
    ((-13, 102, 0), (-7, 106, 5), PIPE),
]

MODELS["catalytic_cracker"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 14, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((2, 14, 10), (14, 60, 26), "fluid_tank_side"),
    ((-14, 2, 6), (-2, 48, 28), "fluid_tank_side"),
    ((-12, 48, 10), (8, 52, 14), PIPE),
    ((-13, 7, 0), (-7, 11, 5), PIPE),
    ((-13, 39, 0), (-7, 43, 5), PIPE),
    ((-13, 55, 0), (-7, 59, 5), PIPE),
]

MODELS["vacuum_distillation_unit"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 14, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-14, 2, 6), (10, 40, 30), "fluid_tank_side"),
    ((-10, 40, 10), (6, 46, 26), "fluid_tank_top"),
    ((-13, 7, 0), (-7, 11, 5), PIPE),
]

MODELS["catalytic_reformer"] = [
    ((-32, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 14, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-14, 2, 10), (-2, 26, 22), "fluid_tank_side"),
    ((-30, 2, 10), (-18, 26, 22), "fluid_tank_side"),
    ((2, 14, 12), (14, 26, 22), "fluid_tank_side"),
    ((-28, 26, 14), (12, 29, 18), PIPE),
    ((-29, 7, 0), (-23, 11, 5), PIPE),
    ((-29, 21, 0), (-23, 25, 5), PIPE),
]

MODELS["chemical_mixer"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 14, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-12, 2, 6), (10, 24, 28), "fluid_tank_side"),
    ((-12, 24, 6), (10, 26, 28), "fluid_tank_top"),
    ((-5, 26, 13), (3, 31, 21), {"*": STEEL, "up": TOP}),
]

MODELS["fracking_rig"] = [
    ((-32, 0, 0), (16, 2, 48), "heavy_plinth"),
    ((2, 2, 1), (14, 12, 6), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-28, 14, 4), (12, 16, 44), "heavy_plinth"),
    ((-24, 16, 8), (8, 40, 40), "wind_turbine_mast"),
    ((-18, 40, 14), (2, 70, 34), "wind_turbine_mast"),
    ((-12, 70, 20), (-4, 78, 28), "geothermal_stack"),
    ((-11, 2, 22), (-5, 14, 26), PIPE),
]

MODELS["flowback_treatment_unit"] = [
    ((-32, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-31, 2, 2), (-1, 10, 30), {"*": STEEL, "up": "fluid_tank_top"}),
    ((1, 2, 12), (15, 14, 30), "fluid_tank_side"),
]

MODELS["diesel_generator"] = [
    ((-32, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-24, 2, 10), (0, 20, 26), STEEL),
    ((-31, 2, 4), (-26, 26, 28), "fluid_tank_side"),
    ((-20, 20, 14), (-16, 30, 18), "geothermal_stack"),
]

MODELS["gas_turbine"] = [
    ((-48, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-36, 4, 8), (0, 22, 26), STEEL),
    ((-47, 2, 2), (-37, 30, 30), "fluid_tank_side"),
    ((-10, 22, 14), (-4, 32, 20), "geothermal_stack"),
]

MODELS["polymerization_reactor"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-12, 2, 6), (12, 40, 28), {"*": "fluid_tank_side", "up": "fluid_tank_top"}),
    ((-4, 40, 14), (4, 46, 22), STEEL),
]

MODELS["diesel_engine"] = [
    ((-16, 0, 0), (16, 2, 48), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-12, 2, 9), (12, 22, 42), STEEL),
    ((-15, 2, 1), (-1, 24, 7), "fluid_tank_side"),
    ((-9.5, 22.5, 42), (-6.5, 25.5, 48), PIPE),
]

MODELS["electrolytic_cell"] = [
    ((-32, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-30, 2, 4), (-2, 24, 28), {"*": "fluid_tank_side", "up": "fluid_tank_top"}),
    ((-31, 36, 20), (15, 40, 24), PIPE),
    ((-31, 22, 24), (15, 26, 28), PIPE),
]

MODELS["chemical_reactor"] = [
    ((-16, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-14, 2, 8), (12, 26, 30), {"*": "fluid_tank_side", "up": "fluid_tank_top"}),
]

MODELS["lithium_battery_bank"] = [
    ((-32, 0, 0), (16, 2, 16), "heavy_plinth"),
    ((-31, 2, 1), (15, 30, 15), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-32, 30, 0), (16, 32, 16), "heavy_plinth"),
    ((-30, 8, 0.5), (14, 9, 1), "power_port_frame"),
]

MODELS["crystal_grower"] = [
    ((0, 0, 0), (16, 2, 16), "heavy_plinth"),
    ((1, 2, 1), (15, 14, 15), {"*": STEEL, "north": "#front", "up": TOP}),
    ((3, 14, 3), (13, 28, 13), "fluid_tank_side"),
    ((6, 28, 6), (10, 32, 10), "geothermal_stack"),
]

MODELS["lithography_station"] = [
    ((-32, 0, 0), (16, 2, 32), "heavy_plinth"),
    ((1, 2, 1), (15, 12, 9), {"*": STEEL, "north": "#front", "up": TOP}),
    ((-30, 2, 4), (-2, 28, 30), {"*": "fluid_tank_side", "up": "fluid_tank_top"}),
]

MODELS["gas_holder"] = [
    ((-32, 0, 0), (16, 2, 48), "heavy_plinth"),
    ((-30, 2, 2), (14, 46, 46), {"*": "fluid_tank_side", "north": "#front", "up": "fluid_tank_top"}),
]

MODELS["crop_harvester"] = [
    ((0, 0, 0), (16, 2, 16), "heavy_plinth"),
    ((1, 2, 1), (15, 14, 15), {"*": STEEL, "north": "#front", "up": TOP}),
    ((3, 14, 3), (13, 28, 13), "wind_turbine_mast"),
    ((0, 22, 1), (16, 26, 5), "sp_red_iron"),
]

# Textures the front face uses: "#front" in MODELS. Lit machines also get <front>_on.
FRONTS = {
    "geothermal_generator": "geothermal_generator_front",
    "wind_turbine": "wind_turbine_front",
    "alloy_smelter": "alloy_smelter_front",
    "coke_oven": "coke_oven_front",
    "steel_foundry": "steel_foundry_front",
    "capacitor_bank": "capacitor_bank_front",
    "steel_tank": "steel_tank_front",
    "ore_drill": "ore_drill_front",
    "water_wheel": "water_wheel_front",
    "large_steam_engine": "large_steam_engine_front",
    "pumpjack": "pumpjack_front",
    "oil_sand_extractor": "oil_sand_extractor_front",
    "distillation_tower": "distillation_tower_front",
    "catalytic_cracker": "catalytic_cracker_front",
    "vacuum_distillation_unit": "vacuum_distillation_unit_front",
    "catalytic_reformer": "catalytic_reformer_front",
    "chemical_mixer": "chemical_mixer_front",
    "fracking_rig": "fracking_rig_front",
    "flowback_treatment_unit": "flowback_treatment_unit_front",
    "diesel_generator": "diesel_generator_front",
    "gas_turbine": "gas_turbine_front",
    "polymerization_reactor": "polymerization_reactor_front",
    "diesel_engine": "diesel_engine_front",
    "electrolytic_cell": "electrolytic_cell_front",
    "chemical_reactor": "chemical_reactor_front",
    "lithium_battery_bank": "lithium_battery_bank_front",
    "crystal_grower": "crystal_grower_front",
    "lithography_station": "lithography_station_front",
    "gas_holder": "gas_holder_front",
    "crop_harvester": "crop_harvester_front",
}
