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

# Offsets (x east, y up, z south) of each part; part 0 is the master. Keep in sync with Java.
FOOTPRINTS = {
    # The lava tank stands to the right of the generator body, seen from the front.
    "geothermal_generator": [(0, 0, 0), (-1, 0, 0)],
    "wind_turbine": [(0, 0, 0), (0, 1, 0), (0, 2, 0)],
    # Furnace body, crucible base to its right, hoppers above the body, crucible tower above the base.
    "alloy_smelter": [(0, 0, 0), (-1, 0, 0), (0, 1, 0), (-1, 1, 0)],
}

# Machines that take power at one marked socket only: (part, face) for a north-facing machine.
# Keep in sync with MachineKind.powerPort(). Everything else takes power on any face.
POWER_PORTS = {
    "alloy_smelter": (1, "west"),
}

PIPE = "bronze_fluid_pipe"
STEEL = "machine_side"
TOP = "machine_top"

MODELS = {
    "geothermal_generator": [
        # Generator body (master block).
        ((0, 0, 0), (16, 2, 16), "geothermal_plinth"),
        ((1, 2, 1), (15, 11, 15), {"*": STEEL, "north": "#front", "up": TOP}),
        ((0, 11, 0), (16, 12, 16), "geothermal_plinth"),
        ((2, 12, 2), (14, 13, 14), TOP),
        ((9, 13, 9), (13, 16, 13), "geothermal_stack"),
        ((8.5, 16, 8.5), (13.5, 17, 13.5), "geothermal_plinth"),
        ((3, 13, 3), (6, 15, 6), PIPE),
        # Heat pipe from the tank into the body.
        ((-4, 6, 5), (1, 10, 9), PIPE),
        ((-2, 5, 4), (-1, 11, 10), "geothermal_plinth"),
        # Lava tank (the block to the right, x -16..0), an octagonal column.
        ((-16, 0, 0), (0, 2, 16), "geothermal_plinth"),
        ((-14, 2, 4), (-2, 15, 12), {"*": "geothermal_tank", "up": "fluid_tank_top"}),
        ((-12, 2, 2), (-4, 14.9, 14), {"*": "geothermal_tank", "up": "fluid_tank_top"}),
        ((-14.5, 4, 3.5), (-1.5, 5, 12.5), "geothermal_plinth"),
        ((-12.5, 4.1, 1.5), (-3.5, 4.9, 14.5), "geothermal_plinth"),
        ((-14.5, 12, 3.5), (-1.5, 13, 12.5), "geothermal_plinth"),
        ((-12.5, 12.1, 1.5), (-3.5, 12.9, 14.5), "geothermal_plinth"),
        ((-11, 15, 5), (-5, 16, 11), "geothermal_plinth"),
    ],
    "wind_turbine": [
        # Base housing (master block).
        ((0, 0, 0), (16, 3, 16), "wind_turbine_base"),
        ((2, 3, 2), (14, 14, 14), {"*": STEEL, "north": "#front", "up": TOP}),
        ((3, 14, 3), (13, 16, 13), "wind_turbine_base"),
        # Mast through the middle block and into the top one, with collars.
        ((6, 16, 6), (10, 38, 10), "wind_turbine_mast"),
        ((5, 16, 5), (11, 17, 11), "wind_turbine_base"),
        ((5, 31, 5), (11, 32, 11), "wind_turbine_base"),
        # Nacelle on top, hub at the front, tail fin behind.
        ((4, 38, 3), (12, 44, 16), "wind_turbine_nacelle"),
        ((5, 44, 6), (11, 45, 14), "wind_turbine_nacelle"),
        ((7.5, 38, 16), (8.5, 46, 24), "wind_turbine_blade"),
        ((6, 39, 1), (10, 43, 3), "wind_turbine_nacelle"),
        ((7, 40, 0), (9, 42, 1), "wind_turbine_base"),
        # Four blades in front of the hub (clear of the mast, which is at z 6..10), red-tipped.
        ((7, 43, 1.5), (9, 60, 2.5), "wind_turbine_blade"),
        ((7, 60, 1.5), (9, 64, 2.5), "wind_turbine_tip"),
        ((7, 22, 1.5), (9, 39, 2.5), "wind_turbine_blade"),
        ((7, 18, 1.5), (9, 22, 2.5), "wind_turbine_tip"),
        ((10, 40, 1.5), (27, 42, 2.5), "wind_turbine_blade"),
        ((27, 40, 1.5), (31, 42, 2.5), "wind_turbine_tip"),
        ((-11, 40, 1.5), (6, 42, 2.5), "wind_turbine_blade"),
        ((-15, 40, 1.5), (-11, 42, 2.5), "wind_turbine_tip"),
    ],
}

MODELS["alloy_smelter"] = [
    # Shared footing under both lower blocks.
    ((-16, 0, 0), (16, 2, 16), "heavy_plinth"),
    # Firebrick furnace body with the crucible window at the front (master block).
    ((1, 2, 1), (15, 14, 15), {"*": "alloy_smelter_brick", "north": "#front", "up": TOP}),
    ((0.5, 2, 0.5), (15.5, 3, 15.5), "bronze_block"),
    ((0.5, 12.5, 0.5), (15.5, 13.5, 15.5), "bronze_block"),
    # Two ingredient hoppers above the body, feeding down through spouts.
    ((1, 22, 1), (8, 28, 8), {"*": "alloy_hopper", "up": "alloy_hopper_top"}),
    ((3, 14, 3), (6, 22, 6), "alloy_hopper"),
    ((8, 22, 8), (15, 28, 15), {"*": "alloy_hopper", "up": "alloy_hopper_top"}),
    ((10, 14, 10), (13, 22, 13), "alloy_hopper"),
    # Crucible base with the copper power socket on its outer side (lower right block).
    ((-15, 2, 1), (-1, 12, 15), {"*": STEEL, "up": TOP}),
    ((-16, 4, 4), (-15, 12, 12), "power_port_frame"),
    ((-16.5, 6.5, 6.5), (-16, 9.5, 9.5), "power_port"),
    # Crucible tower: octagonal pot with bronze rim and a short stack (upper right block).
    ((-14, 12, 4), (-2, 28, 12), {"*": "alloy_crucible", "up": "alloy_crucible"}),
    ((-12, 12, 2), (-4, 27.9, 14), {"*": "alloy_crucible", "up": "alloy_crucible"}),
    ((-14.5, 28, 3.5), (-1.5, 29, 12.5), "bronze_block"),
    ((-12.5, 28, 1.5), (-3.5, 28.9, 14.5), "bronze_block"),
    ((-10, 29, 6), (-6, 32, 10), "geothermal_stack"),
    # Pour trough from the crucible into the body.
    ((-2, 17, 6), (3, 20, 10), PIPE),
]

# Textures the front face uses: "#front" in MODELS. Lit machines also get <front>_on.
FRONTS = {
    "geothermal_generator": "geothermal_generator_front",
    "wind_turbine": "wind_turbine_front",
    "alloy_smelter": "alloy_smelter_front",
}
