"""Acquisition adapters for the October 29 native collection.

Record actual recipe loaders and world interactions, never seed items merely
because an asset or registry entry exists. Quantities/energy remain runtime checks.
"""
import json


def extend(root, edge, evidence, items):
    res = root / 'src/main/resources'
    data = res / 'data/jugcraft'
    java = 'src/main/java/io/github/jimbozoomer/jugcraft/'
    def read(p): return json.loads(p.read_text(encoding='utf-8'))
    def j(n): return n if ':' in n else 'jugcraft:' + n
    def route(source, inputs, outputs, kind='native_interaction'):
        edge(java + source + '.java', kind, inputs, [j(o) for o in outputs])
    def found(names, source, note):
        for n in names:
            evidence[j(n)].append(dict(kind='native_acquisition', source=java+source+'.java', note=note))
    def ref(v):
        if isinstance(v, list): return [ref(x) for x in v]
        if isinstance(v, dict):
            return ('#'+v['tag']) if 'tag' in v else v.get('item', v.get('id', v.get('block', v.get('name'))))
        return v
    def block_ref(v):
        value=ref(v)
        if value in ('minecraft:water','minecraft:lava'): return value+'_bucket'
        if value.startswith('#jugcraft:'):
            tag=data/'tags/block'/(value.split(':',1)[1]+'.json')
            if tag.exists(): return [block_ref(x) for x in read(tag)['values']]
        return value
    def outs(v):
        if isinstance(v,list): return [ref(x) for x in v]
        return [ref(v)]
    colors = ('white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black').split()
    found(['jugtania_'+c+'_mystical_flower' for c in colors], 'botany/BotanicalFlowers',
          'Natural feature places mystical flowers in eligible Overworld biomes; Floral Fertilizer also grows them.')
    for c in colors:
        route('botany/BotanicalFlowers', ['jugcraft:jugtania_'+c+'_mystical_flower','minecraft:bone_meal'], ['jugtania_'+c+'_double_flower'], 'bonemeal_mystical_flower')
    stations = {'mana_infusion':'mana_pool', 'petal_apothecary':'apothecary_default',
                'pure_daisy':'pure_daisy', 'state_copying_pure_daisy':'pure_daisy',
                'runic_altar':'runic_altar', 'terra_plate':'terra_plate', 'elven_trade':'alfheim_portal',
                'orechid':'orechid', 'orechid_ignem':'orechid_ignem', 'marimorphosis':'marimorphosis'}
    for p in sorted((data/'jugtania_recipe').rglob('*.json')):
        d=read(p); typ=d['type']
        if typ == 'brew':
            for vessel in ('vial','flask'):
                edge(p.relative_to(root).as_posix(), 'botanical_brew',
                     [ref(x) for x in d['ingredients']]+['jugcraft:jugtania_'+vessel,'jugcraft:jugtania_brewery','jugcraft:jugtania_endoflame','jugcraft:jugtania_mana_spreader'],
                     ['jugcraft:jugtania_brew_'+vessel])
            continue
        if typ not in stations: raise ValueError('Unaudited botanical recipe '+typ)
        inputs=[ref(x) for x in d.get('ingredients',[])]
        if 'input' in d: inputs.append(ref(d['input']) if typ=='mana_infusion' else block_ref(d['input']))
        inputs.append('jugcraft:jugtania_'+stations[typ])
        if 'reagent' in d: inputs += [ref(d['reagent']), 'minecraft:water_bucket']
        if 'catalyst' in d: inputs.append(ref(d['catalyst']))
        if d.get('mana',0) or typ in ('elven_trade','orechid','orechid_ignem'):
            inputs += ['jugcraft:jugtania_endoflame','jugcraft:jugtania_mana_spreader']
        if typ == 'runic_altar': inputs.append('jugcraft:jugtania_livingrock')
        if typ == 'terra_plate': inputs += ['jugcraft:jugtania_livingrock','minecraft:lapis_block']
        if typ == 'elven_trade': inputs += ['jugcraft:jugtania_natura_pylon','jugcraft:jugtania_mana_pool','jugcraft:jugtania_glimmering_livingwood']
        # Block transformations can produce non-item states such as flowing water;
        # they are not collectible item sources in this graph.
        outputs=[o for o in outs(d.get('output',d.get('result'))) if o in items or o.startswith('minecraft:') and o not in ('minecraft:water','minecraft:lava','minecraft:air')]
        if outputs: edge(p.relative_to(root).as_posix(), 'botanical_'+typ, inputs, outputs)
    for p in sorted((data/'arcane_ritual').glob('*.json')):
        d=read(p); result=d['result'].get('result_item')
        outputs = outs(result) if result else ['jugcraft:forbidden_hephaestus_forge_tier_'+str(d['result']['result_tier'])]
        inputs=[ref(d['main_ingredient'])]+[ref(x['ingredient']) for x in d['inputs']]
        inputs += ['jugcraft:forbidden_hephaestus_forge_tier_'+str(d.get('forge_tier',1)), 'jugcraft:forbidden_darkstone_pedestal', '#jugcraft:forbidden_blacksmith_gavel']
        needs=d.get('enhancers',[])
        inputs += needs if isinstance(needs,list) else [needs]
        for essence, item in {'aureal':'forbidden_aureal_bottle','souls':'forbidden_soul','blood':'forbidden_blood_test_tube','experience':'minecraft:experience_bottle'}.items():
            if d.get('essences',{}).get(essence,0): inputs.append(j(item))
        edge(p.relative_to(root).as_posix(), 'arcane_ritual', inputs, outputs)
    for p in sorted((data/'arcane_combustion').glob('*.json')):
        d=read(p); ingredients=d['ingredients']
        inputs=[ref(ingredients['first']),ref(ingredients['second'])] if 'first' in ingredients else [ref(ingredients)]
        inputs += ['jugcraft:forbidden_clibano_core','jugcraft:forbidden_polished_darkstone','jugcraft:forbidden_polished_darkstone_bricks','jugcraft:forbidden_mundabitur_dust','minecraft:coal']
        if d.get('enhancer'): inputs.append(d['enhancer'])
        if d['fire_type'] in ('soul_fire','enchanted_fire'):
            inputs.append('#jugcraft:forbidden_clibano_creates_'+d['fire_type'])
        edge(p.relative_to(root).as_posix(), 'arcane_combustion', inputs, outs(d['result']))
    aquatic=read(res/'jugcraft/newest_aquatic.json')
    for fish in aquatic['fish']:
        name='aquatic_'+fish['name']
        route('aquatic/OwnerAquaculture', ['jugcraft:'+name,'minecraft:water_bucket'], [name+'_bucket'], 'bucket_live_naturally_spawning_fish')
        if fish.get('fillets',0) <= 0: continue
        route('aquatic/OwnerFilletRecipe', ['jugcraft:'+name,'jugcraft:aquatic_wooden_fillet_knife'], ['aquatic_fish_fillet_raw'], 'fillet_fish_with_knife')
    route('arcane/ArcaneDust', ['minecraft:smithing_table','jugcraft:forbidden_arcane_crystal_dust','jugcraft:forbidden_gilded_chiseled_polished_darkstone','jugcraft:forbidden_chiseled_arcane_polished_darkstone','jugcraft:forbidden_polished_darkstone'], ['forbidden_hephaestus_forge_tier_1'], 'form_complete_darkstone_forge_base')
    route('arcane/ArcaneEssences', ['minecraft:soul_sand','jugcraft:forbidden_soul_extractor'], ['forbidden_soul','forbidden_soulless_sand'], 'extract_soul_from_soul_sand')
    route('arcane/ArcaneEssences', ['jugcraft:forbidden_test_tube','minecraft:iron_sword'], ['forbidden_blood_test_tube'], 'collect_blood_from_valid_melee_target')
    route('arcane/ArcaneAurealRecipe', ['jugcraft:forbidden_arcane_crystal_dust','minecraft:potion','minecraft:ghast_tear','minecraft:glowstone_dust'], ['forbidden_aureal_bottle'], 'craft_with_strong_regeneration_potion')
    route('menagerie/OwnerAmendmentCauldrons', ['minecraft:cauldron','minecraft:water_bucket','minecraft:red_dye','minecraft:glass_bottle'], ['amends_dye_bottle'], 'bottle_dyed_cauldron_water')
    route('building/OwnerTwigsWorld', ['minecraft:oak_leaves'], ['twigworks_twig'], 'natural_leaf_stick_drop_conversion')
    route('building/OwnerTwigsWorld', ['minecraft:gravel'], ['twigworks_pebble'], 'gravel_bonus_drop')
    route('building/OwnerTwigsWorld', ['minecraft:bamboo'], ['twigworks_bamboo_leaves'], 'bamboo_bonus_drop')
    found(['twigworks_'+n+'_seashell' for n in ('opaline','tangerine','roseate','bronzed')], 'building/OwnerTwigsWorld', 'Registered natural seashell patches place weighted shell variants.')
    found(['ribbits_giant_lilypad'], 'ribbits/RibbitWorld', 'Registered swamp vegetation feature places giant lilypads on water.')
    route('ribbits/Ribbit', ['minecraft:emerald'], ['ribbits_mossy_oak_planks'], 'trade_with_naturally_generated_ribbit_merchant')
    route('arcane/ArcaneGrowth', ['jugcraft:forbidden_growing_edelwood','minecraft:bone_meal'], ['forbidden_edelwood_log','forbidden_carved_edelwood_log','forbidden_edelwood_branch'], 'grow_edelwood')
    route('arcane/ArcaneGrowth', ['jugcraft:forbidden_fungyss','minecraft:bone_meal'], ['forbidden_fungyss_stem','forbidden_fungyss_block'], 'grow_fungyss')
    route('arcane/EdelwoodBlocks', ['jugcraft:forbidden_edelwood_log','minecraft:glass_bottle'], ['forbidden_edelwood_oil'], 'bottle_regenerating_oil_from_edelwood')
    route('arcane/ArcaneJar', ['jugcraft:forbidden_utrem_jar','jugcraft:forbidden_aureal_bottle'], ['forbidden_essence_utrem_jar'], 'fill_jar_and_sneak_collect')
    route('aquatic/OwnerWormFarm', ['jugcraft:aquatic_worm_farm','minecraft:wheat_seeds'], ['aquatic_worm'], 'compost_and_harvest_bait')
    for name in ('infinite_fruit','king_key','flugel_eye','thor_ring','odin_ring','loki_ring'):
        route('botany/BotanicalRelics', ['jugcraft:jugtania_dice'], ['jugtania_'+name], 'roll_bound_dice_for_uncollected_relic')
    route('botany/BotanicalWaterBowl', ['minecraft:bowl','minecraft:water_bucket'], ['jugtania_water_bowl'], 'fill_bowl_from_water_source')
    route('botany/BotanicalEnderAir', ['minecraft:glass_bottle'], ['jugtania_ender_air_bottle'], 'fill_bottle_in_the_end')
    for name in ('dry','golden','vivid','scorched','infused','mutated'):
        route('botany/BotanicalLandscaping', ['jugcraft:jugtania_'+name+'_seeds','minecraft:grass_block'], ['jugtania_'+name+'_grass'], 'plant_pasture_seeds')
    route('botany/BotanicalCacophonium', ['jugcraft:jugtania_cacophonium','minecraft:note_block'], ['jugtania_cacophonium_block'], 'attach_mob_sound_instrument_to_note_block')
    route('botany/BotanicalEnchanter', ['minecraft:lapis_block','minecraft:obsidian','jugcraft:jugtania_mana_pylon','jugcraft:jugtania_white_mystical_flower','jugcraft:jugtania_twig_wand'], ['jugtania_enchanter'], 'form_complete_botanical_enchanter')
    route('ribbits/Ribbit', ['minecraft:emerald'], ['ribbits_maraca'], 'buy_from_ribbit_merchant')
    found(['wilds_elokosa_paw_'+phase for phase in ('full','gibbous','half','crescent','new')], 'wilds/WildElokosa', 'Elokosa death reward depends on moon phase; looting affects the drop chance.')
    found(['wilds_ice_crystal','wilds_music_disc_petiole'], 'wilds/WildFrostmaw', 'Defeat a crystal-carrying Frostmaw for its crystal, or one without its crystal during rain for the disc.')
    route('wilds/WildGrottol', ['minecraft:diamond_pickaxe','minecraft:enchanted_book','minecraft:anvil'], ['wilds_captured_grottol'], 'capture_natural_grottol_using_silk_touch')
    for sand in ('sand','red_sand'):
        route('wilds/WildRakedSand', ['minecraft:'+sand,'jugcraft:wilds_sand_rake'], ['wilds_raked_sand' if sand=='sand' else 'wilds_red_raked_sand'], 'rake_exposed_sand')
    route('arcane/ArcaneGrowth', ['jugcraft:forbidden_corrupt_soul','minecraft:oak_sapling'], ['forbidden_growing_edelwood'], 'corrupt_sapling_with_corrupt_soul')
    for wood in ('livingwood','livingwood_log','dreamwood','dreamwood_log'):
        route('botany/Jugtania', ['jugcraft:jugtania_'+wood,'minecraft:iron_axe'], ['jugtania_stripped_'+wood], 'strip_with_axe')
    for wood in ('aurum_log','aurum_wood'):
        route('arcane/ForbiddenArcane', ['jugcraft:forbidden_'+wood,'minecraft:iron_axe'], ['forbidden_stripped_'+wood], 'strip_with_axe')
    route('arcane/ArcaneBlackHole', ['jugcraft:forbidden_dark_matter','jugcraft:forbidden_corrupti_dust','minecraft:experience_bottle'], ['forbidden_xpetrified_orb'], 'form_black_hole_and_collect_91_experience')
    route('arcane/ArcaneDust', ['jugcraft:forbidden_mundabitur_dust','jugcraft:forbidden_arcane_polished_darkstone','jugcraft:forbidden_arcane_crystal_block'], ['forbidden_arcane_crystal_obelisk'], 'form_three_block_obelisk')
    route('arcane/ArcaneMixture', ['jugcraft:forbidden_darkstone_pedestal','jugcraft:forbidden_ferrognetic_mixture'], ['forbidden_magnetized_darkstone_pedestal'], 'magnetize_pedestal')
    for liquid in ('water','lava','milk','powder_snow'):
        route('arcane/ArcaneBuckets', ['jugcraft:forbidden_edelwood_bucket','minecraft:'+liquid+'_bucket'], ['forbidden_edelwood_'+liquid+'_bucket'], 'fill_edelwood_bucket_from_source_or_animal')
    route('botany/BotanicalThrownTools', ['jugcraft:jugtania_vine_ball'], ['jugtania_solid_vine'], 'throw_vine_ball_against_supported_wall')
    found(['supps_barnacles'], 'menagerie/OwnerBarnaclePatch', 'Registered ocean and beach features place barnacles.')
    route('menagerie/OwnerSupplementaries', ['minecraft:gravel','minecraft:iron_hoe'], ['supps_raked_gravel'], 'hoe_exposed_gravel')
    route('menagerie/OwnerSupplementTerrain', ['minecraft:basalt','jugcraft:supps_ash'], ['supps_ashen_basalt'], 'fall_ash_onto_basalt')
    route('arcane/ArcaneGrowth', ['jugcraft:forbidden_arcane_bone_meal','minecraft:farmland'], ['forbidden_magical_farmland'], 'enrich_farmland')
    route('aquatic/OwnerNeptunium', ['jugcraft:aquatic_neptunium_hoe','minecraft:dirt'], ['aquatic_farmland'], 'till_soil_with_neptunium_hoe')
    route('wilds/WildGroveTrades', ['minecraft:gold_ingot'], ['wilds_clawed_log'], 'trade_at_generated_grove')
    found(['supps_wild_flax'], 'menagerie/OwnerSupplementFlora', 'Registered wild flax patches generate in eligible sandy habitats.')
    route('menagerie/OwnerSupplementFlax', ['jugcraft:supps_flax_seeds','minecraft:farmland'], ['supps_flax'], 'grow_and_harvest_mature_flax')
    route('menagerie/OwnerRopeArrow', ['minecraft:arrow','jugcraft:supps_rope'], ['supps_rope_arrow'], 'craft_paid_rope_ammunition')
    found(['supps_music_disc_heave_ho','supps_dragon_banner_pattern'], 'menagerie/OwnerSupplementTreasures', 'Native loot injections add the disc to shipwreck treasure and the banner pattern to End city treasure.')
    found(['supps_enderman_head','supps_spider_head'], 'menagerie/OwnerMobHeads', 'Charged creeper kills of the matching mobs award their heads.')
    for before,after in (('obsidian','cracked_obsidian'),('cracked_obsidian','fragmented_obsidian'),('fragmented_obsidian','fading_obsidian')):
        route('arcane/ArcaneSkulls', ['jugcraft:forbidden_'+before+'_skull'], ['forbidden_'+after+'_skull'], 'wear_skull_until_fire_protection_degrades')
    route('botany/BotanicalTreasures', ['jugcraft:jugtania_overgrowth_seed','minecraft:grass_block'], ['jugtania_enchanted_soil'], 'transform_soil_in_world_not_a_collectible_drop')
    exclusions = {'jugcraft:wilds_grant_suns_blessing': 'Creative/operator grant token; ordinary survival blessing is awarded by the Umvuthi encounter, not this item.'}
    for name in ('chest','trapped_chest','barrel','shulker_box','copper_chest','exposed_copper_chest','weathered_copper_chest','oxidized_copper_chest','decorated_pot','suspicious_sand','suspicious_gravel','inventory','item_frame','minecart'):
        exclusions[j('personal_loot_'+name)] = 'Structure/operator personal-loot container; deliberately empty block drops and no survival loot-generator recipe (tools/newest_loot.py).'
    exclusions[j('jugtania_infrangible_platform')] = 'Creative-only unbreakable platform; BotanicalPlatforms requires infinite materials to edit it.'
    exclusions[j('packworks_survival_infinity_upgrade')] = 'No survival acquisition under the owner server economy policy. OwnerBackpackInfinity requires the disabled-by-default backpacks.survival_infinity option; no recipe is supplied or enabled by this repair.'
    for name in ('jugtania_creative_pool','jugtania_corporea_spark_creative','packworks_infinity_upgrade'):
        exclusions[j(name)] = 'Creative/operator-only resource; not a survival unlock.'
    return exclusions

