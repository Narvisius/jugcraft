"""Acquisition edges for the collection's native interactions, not crafting shortcuts.

The graph proves reachability only. JE costs, time, quantities, permissions and
output capacity remain enforced by the referenced native implementations.
"""
import json

DEFERRED_ACQUISITION = {}


def extend(root, edge, evidence, items):
    base = 'src/main/java/io/github/jimbozoomer/jugcraft/jimtafacts/'
    def j(n): return n if ':' in n or n.startswith('#') else 'jugcraft:' + n
    def route(file, ins, outs, kind='native_workshop'):
        edge(base+file+'.java', kind, [j(n) for n in ins], [j(n) for n in outs])
    def read(name):
        return json.loads((root/'src/main/resources/jugcraft/jimtafacts'/name).read_text(encoding='utf-8'))
    route('spellbooks/JimtaSpellbookLore', ['jimta_spellbooks_archevoker_logbook_untranslated', 'jimta_spellbooks_ancient_knowledge_fragment', 'minecraft:enchanting_table'], ['jimta_spellbooks_archevoker_logbook_translated'], 'translate_logbook_consuming_one_fragment')
    for name, crop in read('crops.json').items():
        parents=['jimta_seed_'+p for p in crop['breed']]
        route('industry/IndustrialCrops', parents+['jimta_crop_sticks'], ['jimta_seed_'+name], 'crossbreed_two_mature_crops')
        anchor=crop['anchor']
        inputs=['jimta_seed_'+name]
        if anchor: inputs += [('minecraft:' if anchor in ('iron','gold','copper') else 'jugcraft:')+anchor+'_block']
        route('industry/IndustrialCrops', inputs, [crop['product']], 'ripen_with_required_light_and_ore_anchor')
    for source, result in [('minecraft:compass','meteorite_compass'),('jugcraft:jimta_certus_quartz_crystal','charged_certus_quartz_crystal')]:
        route('network/DigitalWorkshops', ['jimta_charger',source], ['jimta_'+result], 'powered_charger_1000_je')
    for kind, material in [('logic','minecraft:gold_ingot'),('calculation','jugcraft:jimta_certus_quartz_crystal'),('engineering','minecraft:diamond')]:
        route('network/DigitalProduction', ['jimta_inscriber','jimta_'+kind+'_processor_press',material], ['jimta_printed_'+kind+'_processor'])
        route('network/DigitalProduction', ['jimta_inscriber','jimta_printed_'+kind+'_processor','minecraft:redstone','jimta_printed_silicon'], ['jimta_'+kind+'_processor'])
    route('network/DigitalProduction', ['jimta_inscriber','jimta_silicon_press','silicon'], ['jimta_printed_silicon'])
    for kind in ('crafting','processing','stonecutting','smithing_table'):
        route('network/DigitalPatterns', ['jimta_pattern_encoding_terminal','jimta_blank_pattern'], ['jimta_'+kind+'_pattern'], 'encode_valid_recipe_or_processing_pattern')
    route('network/DigitalPressMenu', ['jimta_certus_quartz_cutting_knife','minecraft:iron_ingot'], ['jimta_name_press'], 'engrave_ingot_with_knife')
    route('network/DigitalFacadeRecipe', ['jimta_certus_quartz_cutting_knife','minecraft:iron_ingot'], ['jimta_cable_anchor'], 'cut_ingot_consuming_knife_durability')
    route('network/DigitalFacadeRecipe', ['jimta_cable_anchor','minecraft:stone'], ['jimta_facade'])
    route('industry/TransportFacades', ['jimta_facade'], ['jimta_pipe_facade_solid'], 'convert_sampled_facade_preserving_material')
    route('industry/TransportFacades', ['jimta_pipe_facade_solid'], ['jimta_pipe_facade_hollow'], 'convert_sampled_facade_preserving_material')
    route('industry/FactoryWaterGel', ['jimta_water_gel_seed','minecraft:water_bucket'], ['jimta_gelled_water'], 'seed_source_water_then_break_gel')
    route('industry/IndustrialHeat', ['chemical_reactor','minecraft:water_bucket','minecraft:bucket'], ['jimta_hot_water_bucket'], 'heat_water_then_fill_bucket')
    route('industry/IndustrialHeat', ['chemical_reactor','jimta_cf_powder','minecraft:water_bucket','minecraft:bucket'], ['jimta_construction_foam_fluid_bucket'], 'mix_foam_then_fill_bucket')
    route('industry/IndustrialHeat', ['jimta_liquid_heat_exchanger','minecraft:lava_bucket','minecraft:bucket'], ['jimta_pahoehoe_lava_bucket'], 'recover_lava_heat_then_fill_bucket')
    for compact, full in [('cable_interface','interface'),('cable_energy_acceptor','energy_acceptor'),('cable_pattern_provider','pattern_provider')]:
        route('network/DigitalPartConversionRecipe',['jimta_'+full],['jimta_'+compact],'convert_compact_part_preserving_saved_components')
    route('network/DigitalCondenser', ['jimta_condenser','jimta_cell_component_64k','minecraft:cobblestone'], ['jimta_singularity'], 'condense_256000_material_units')
    for name in ('small_quartz_bud','medium_quartz_bud','large_quartz_bud','quartz_cluster','chipped_budding_quartz','damaged_budding_quartz'):
        route('network/DigitalMaterials', ['jimta_flawed_budding_quartz','minecraft:diamond_pickaxe'], ['jimta_'+name], 'budding_growth_or_degradation_and_silk_touch_harvest')
    route('industry/WorkshopEngines', ['jimta_stirling_engine','minecraft:coal'], ['jimta_ashes'], 'burn_fuel_byproduct')
    for form in ('page','pages','book'):
        ins=['jimta_computer_computer_normal','jimta_computer_printer','minecraft:paper','minecraft:ink_sac']
        if form!='page':ins+=['minecraft:string']
        if form=='book':ins+=['minecraft:leather']
        route('computers/ComputerNode', ins, ['jimta_printed_'+form], 'print_document_consuming_ink_paper_and_binding')
    for form in ('brew','youngster','beer','ale','dragonblood','black_stuff'):
        route('industry/BreweryBarrel', ['jimta_brewing_barrel','minecraft:water_bucket','minecraft:wheat','jimta_hops','jimta_hand_treetap','jimta_empty_mug'], ['jimta_barrel_'+form], 'age_then_tap_at_chosen_maturity')
    route('industry/BreweryBarrel', ['jimta_brewing_barrel','minecraft:sugar_cane','jimta_hand_treetap','jimta_empty_mug'], ['jimta_barrel_rum'])
    route('industry/MatterWorkshops', ['jimta_solid_canner','jimta_tin_can','minecraft:apple'], ['jimta_filled_tin_can'])
    route('industry/IndustrialBoats', ['jimta_rubber_boat'], ['jimta_broken_rubber_boat'], 'recover_hull_after_hard_crash')
    for name in ('thin_fluted_axle','turned_handle','turned_drill'):
        route('industry/IndustrialLathe', ['jimta_lathe','jimta_iron_turning_blank'], ['jimta_'+name], 'lathe_to_documented_five_segment_profile')
    route('industry/CompactReactor', ['jimta_compact_reactor','jimta_lithium_cell','jimta_uranium_rod_simple'], ['jimta_tritium_cell'], 'irradiate_cell_with_10000_neutron_units')
    for fuel in ('uranium','mox'):
        for size in ('simple','dual','quad'):
            route('industry/CompactReactor', ['jimta_compact_reactor','jimta_'+fuel+'_rod_'+size], ['jimta_'+fuel+'_rod_'+size+'_depleted'], 'exhaust_reactor_fuel')
    route('industry/IndustrialFluidServices', ['jimta_solar_distiller','minecraft:water_bucket','minecraft:bucket'], ['distilled_water_bucket'], 'solar_distillation_and_tank_fill')
    route('industry/IndustrialHeat', ['chemical_reactor','minecraft:water_bucket','minecraft:lapis_lazuli','minecraft:bucket'], ['jimta_coolant_bucket'], 'mix_and_fill_bucket')
    route('industry/CompactReactor', ['jimta_compact_reactor','jimta_uranium_rod_simple','jimta_coolant_bucket'], ['jimta_hot_coolant_bucket'], 'fluid_cooled_reactor_and_bucket_fill')
    route('industry/MatterWorkshops', ['jimta_mass_fabricator','minecraft:bucket'], ['jimta_uu_matter_bucket'], 'consume_power_and_collect_uu_matter')
    route('industry/IndustrialConstruction',['jimta_cf_sprayer','foam_canister'],['jimta_construction_foam'],'spray_world_foam_then_cure')
    route('industry/IndustrialConstruction',['jimta_cf_sprayer','foam_canister','jimta_iron_scaffold'],['jimta_reinforced_foam'],'coat_iron_scaffold_then_cure')
    evidence['jugcraft:jimta_mysterious_cube'].append({'kind':'native_acquisition','source':base+'network/DigitalMeteorites.java',
        'note':'The registered meteorite feature places this core at its centre; discover and mine it in newly generated terrain.'})
    # Cauldron fluids are graph intermediates, never invented inventory items.
    def native(n): return n.replace('irons_spellbooks:','jugcraft:jimta_spellbooks_')
    def liquid(row):
        potion=row.get('components',{}).get('minecraft:potion_contents',{}).get('potion')
        return 'fluid:potion:'+potion if potion else 'fluid:'+row['id']
    recipes=json.loads((root/'src/main/resources/jugcraft/jimtafacts_spellbooks/cauldron_recipes.json').read_text(encoding='utf-8'))['recipes']
    for row in recipes:
        part=row['input'];reagent=native(part['item']) if 'item' in part else '#'+part['tag']
        inputs=['jimta_spellbooks_alchemist_cauldron',reagent];outputs=[]
        kind=row['type'].rsplit('_',1)[-1]
        if kind=='fill':outputs.append(liquid(row['fluid']))
        elif kind=='empty':inputs.append(liquid(row['fluid']))
        elif kind=='brew':
            inputs.append(liquid(row['base_fluid']))
            outputs.extend(liquid(r) for r in row['results'])
        else:raise ValueError('Unmapped cauldron recipe '+row['type'])
        for field in ('result','byproduct'):
            if row.get(field):outputs.append(native(row[field]['id']))
        route('spellbooks/JimtaWorkshops',inputs,outputs,'finite_cauldron_'+row['source_recipe'])
        for field in ('base_fluid','fluid'):
            fluid=row.get(field,{})
            potion=fluid.get('components',{}).get('minecraft:potion_contents',{}).get('potion')
            if potion:
                # Vanilla potion availability is an explicit audit assumption.
                # Imported Focus potions also require their registered brewing reagent.
                ins=['minecraft:potion','jimta_spellbooks_alchemist_cauldron']
                if potion.startswith('irons_spellbooks:'):ins+=['jimta_spellbooks_arcane_essence','minecraft:amethyst_cluster']
                route('spellbooks/JimtaWorkshops',ins,[liquid(fluid)],'pour_brewed_'+potion)
    route('spellbooks/JimtaWorkshops',['jimta_spellbooks_blood_cauldron','minecraft:campfire'],['fluid:irons_spellbooks:blood'],'heat_cauldron_and_damage_permitted_living_target')
    route('spellbooks/JimtaWorkshops',['minecraft:glass_bottle','minecraft:trident'],['jimta_spellbooks_lightning_bottle'],'bottle_a_charged_creeper_once_per_lightning_charge')
    exclusions={'jugcraft:'+name:'Creative/admin-only collection entry; no survival acquisition intended.' for name in (
        'jimta_creative_engine','jimta_creative_storage_cell','jimta_computer_computer_command','jimta_spellbooks_gold_crown')}
    exclusions['jugcraft:jimta_mining_tip']='Deployed mining-column state; basic miner places it from a Mining Pipe, and breaking or retracting returns that pipe. No separate survival item.'
    exclusions['jugcraft:jimta_pipe_color_marker']='Render-only pipe-color model carrier; no recipe, creative entry or survival drop.'
    exclusions['jugcraft:jimta_paint']='World paint-splotch state, placed using a Color Applicator and consumed paint balls; no loose survival block item.'
    exclusions.update({'jugcraft:jimta_spellbooks_'+name:'Deferred acquisition design: '+reason+' See docs/features/jimtafacts-remaining.md.'
                       for name,reason in DEFERRED_ACQUISITION.items()})
    return exclusions
