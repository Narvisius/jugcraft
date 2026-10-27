"""Explicit distinguishing parts for native workshop crafts.

Shared housings are intentional; identical crafting inputs with different results
are not. These are ordinary existing materials, not new recipe-selector items.
The spell forge uses the same refined bill as the crafting-table route.
"""
ADDITIONS = {
    'jimtafacts_industry/jimta_induction_furnace_housing':['jugcraft:copper_wire'],
    'jimtafacts_crops/mug': ['minecraft:stick'],
    'jimtafacts_utilities/rubber_boat': ['minecraft:wooden_shovel'],
    'jimtafacts_industry/jimta_advanced_crafting_table_housing': ['jugcraft:advanced_circuit'],
    'jimtafacts_industry/jimta_integration_table_housing': ['jugcraft:basic_circuit'],
    'jimtafacts_reactor/jimta_heat_exchanger': ['jugcraft:copper_plate'],
    'jimtafacts_reactor/jimta_core_heat_exchanger': ['minecraft:gold_ingot'],
    'jimtafacts_reactor/jimta_component_heat_exchanger': ['jugcraft:basic_circuit'],
    'jimtafacts_reactor/jimta_reactor_heat_vent': ['minecraft:iron_bars'],
    'jimtafacts_reactor/jimta_component_heat_vent': ['minecraft:redstone'],
    'jimtafacts_reactor/jimta_advanced_heat_exchanger': ['jugcraft:copper_plate'],
}
for name, part in {
    'ball_lightning':'slime_ball', 'electrocute':'redstone',
    'black_hole':'obsidian', 'blaze_storm':'gunpowder',
    'cone_of_cold':'quartz', 'divine_smite':'iron_ingot', 'evasion':'rabbit_foot',
    'flaming_barrage':'gunpowder', 'fire_breath':'coal', 'wall_of_fire':'brick',
    'frost_step':'leather', 'ice_block':'snowball',
    'ice_spikes':'pointed_dripstone', 'lightning_lance':'amethyst_shard',
    'poison_breath':'slime_ball', 'raise_hell':'bone',
}.items():
    ADDITIONS['jimta_spellbooks_scroll_'+name]=['minecraft:'+part]

for name, part in {
    'electric_kinetic_generator':'jugcraft:iron_gear',
    'fluid_heat_generator':'minecraft:bucket',
    'liquid_heat_exchanger':'jugcraft:copper_plate',
    'radioisotope_generator':'jugcraft:jimta_rtg_pellet',
    'radioisotope_heat_generator':'jugcraft:lead_plate',
    'steam_generator':'minecraft:cauldron',
    'steam_kinetic_generator':'jugcraft:jimta_turbine_blade',
    'stirling_generator':'minecraft:piston',
    'stirling_kinetic_generator':'minecraft:sticky_piston',
    'water_kinetic_generator':'minecraft:oak_boat',
    'wind_kinetic_generator':'jugcraft:jimta_wood_rotor',
}.items():
    ADDITIONS['jimtafacts_heat/'+name]=[part]

for flow in ('item','fluid','power'):
    ADDITIONS['jimtafacts_transport/jimta_pipe_diamond_wood_'+flow]=['minecraft:oak_planks']
ADDITIONS.update({
    'jimtafacts_transport/jimta_pipe_daizuli_item':['minecraft:lapis_lazuli'],
    'jimtafacts_transport/jimta_pipe_structure':['minecraft:iron_nugget'],
    'jimtafacts_transport/jimta_pipe_void_item':['minecraft:redstone'],
})

# These base bills already occupy all nine crafting slots, so specialize the
# final component instead of silently making an uncraftable ten-slot recipe.
FINAL_PART = {
    'jimtafacts_civil/builder':'minecraft:bricks',
    'jimtafacts_civil/filler':'minecraft:hopper',
    'jimtafacts_civil/flood_gate':'minecraft:bucket',
    'jimtafacts_civil/mining_well':'minecraft:iron_pickaxe',
    'jimtafacts_civil/replacer':'minecraft:piston',
    'jimtafacts_matter/jimta_solid_canner':'jugcraft:tin_ingot',
    'jimtafacts_matter/jimta_matter_replicator':'minecraft:dropper',
    'jimtafacts_matter/jimta_matter_scanner':'minecraft:observer',
    'jimtafacts_matter/jimta_pattern_storage':'minecraft:chest',
}

def distinct(name, inputs):
    result=list(inputs)
    if name in FINAL_PART:
        result[-1]=FINAL_PART[name]
    result.extend(ADDITIONS.get(name, ()))
    if not 1 <= len(result) <= 9:
        raise ValueError(f'{name}: crafting bill needs {len(result)} slots')
    return result
